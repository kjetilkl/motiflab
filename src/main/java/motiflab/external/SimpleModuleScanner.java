/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.Data;
import motiflab.engine.data.Module;
import motiflab.engine.data.ModuleCollection;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;

/**
 * SimpleModuleScanner is a basic module scanning program that can locate sets of regions
 * in a motif track that correspond to predefined modules
 * @author kjetikl
 */
public class SimpleModuleScanner extends ModuleScanning {

    public SimpleModuleScanner() {
        this.name="SimpleModuleScanner";
        this.programclass="ModuleScanning";
        this.serviceType="bundled";

        addSourceParameter("Motif track", RegionDataset.class, null, null, "input track");
        addParameter("Modules",ModuleCollection.class, null,new Class[]{ModuleCollection.class},null,true,false);
        addParameter("Overlap",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Allow overlapping motif sites (for unordered modules only!)",true,false);
        addResultParameter("Result", RegionDataset.class, null, null, "output track");
    }

    private void processAdditionalParameters(OperationTask task) throws Exception {
        ModuleCollection modules=(ModuleCollection)task.getParameter("Modules");
        HashMap<String,HashSet<String>> motifnames=new HashMap<String,HashSet<String>>();
        for (Module module:modules.getAllModules(engine)) {
            HashSet<String> names=new HashSet<String>();
            int size=module.getCardinality();
            for (int i=0;i<size;i++) {
               MotifCollection col=module.getMotifAsCollection(i);
               for (String mname:col.getAllMotifNames()) names.add(mname);
            }
            motifnames.put(module.getName(), names);
        }
        task.setParameter("Motifnames",motifnames); // these names are used to filter out regions for motifs that are not used in the modules we search for
    }


    @Override
    public void execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String targetDatasetName=task.getTargetDataName();
        Data[] sources=(Data[])task.getParameter(SOURCES);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for module scanning with SimpleModuleScanner");
        if (!(sources[0] instanceof RegionDataset)) throw new ExecutionError("SYSTEM ERROR: SimpleModuleScanner requires a Motif track as source input");
        if (!((RegionDataset)sources[0]).isMotifTrack()) throw new ExecutionError("SYSTEM ERROR: SimpleModuleScanner requires a Motif track as source input");
        processAdditionalParameters(task);
        RegionDataset sourceDataset=(RegionDataset)sources[0];
        RegionDataset targetDataset=new RegionDataset(targetDatasetName);
        ArrayList<Data> allsequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data sequence:allsequences) {
             RegionSequenceData regionsequence=new RegionSequenceData((Sequence)sequence);
             targetDataset.addSequence(regionsequence);
        }
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        int size=sequenceCollection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        
         int threads=engine.getConcurrentThreads();
        if (threads<=0) threads=1;
        ExecutorService threadpool=Executors.newFixedThreadPool(threads);
        int[] counters=new int[]{0,0,sequences.size()}; // counters[0]=#downloads started, [1]=#downloads completed, [2]=#total number of sequences
        ArrayList<ScanSequenceTask<RegionSequenceData>> scantasks=new ArrayList<ScanSequenceTask<RegionSequenceData>>(sequences.size());
        for (Sequence sequence:sequences) scantasks.add(new ScanSequenceTask<RegionSequenceData>(sourceDataset,targetDataset, sequence.getName(), task, counters));
        List<Future<RegionSequenceData>> futures=null;
        int countOK=0;            
        try {
            futures=threadpool.invokeAll(scantasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<RegionSequenceData> future:futures) {
                if (future.isDone() && !future.isCancelled()) {
                    future.get(); // this blocks until completion but the return value is not used
                    countOK++;
                }
            }
        } catch (Throwable e) {  
           threadpool.shutdownNow();
           if (e instanceof java.util.concurrent.ExecutionException) throw (Exception)e.getCause(); 
           else if (!(e instanceof Exception)) throw new ExecutionError(e.getMessage(),e); 
           else throw e; 
        } 
        if (threadpool!=null) threadpool.shutdownNow();        
        if (countOK!=sequences.size()) {
            throw new ExecutionError("Some mysterious error occurred while scanning");
        }             
                          
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);
    }


    @SuppressWarnings("unchecked")
    private void searchInSequence(RegionSequenceData sourceSequence, RegionSequenceData targetSequence, OperationTask task) throws Exception {
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        String seqname=sourceSequence.getName();
        HashMap<String,HashSet<String>> motifnames=(HashMap<String,HashSet<String>>)task.getParameter("Motifnames");        
        ModuleCollection modules=(ModuleCollection)task.getParameter("Modules");
        
        for (Module module:modules.getAllModules(engine)) {
            boolean allowoverlap=(Boolean)task.getParameter("Overlap");
            int size=module.getCardinality();
            if (size==0) return;
            int[] motifcounts=new int[size]; // count the number of occurrences for each modulemotif
            int totalmotifcounts=0;

            ArrayList<Region> candidateRegions=sourceSequence.getAllRegions(motifnames.get(module.getName())); // these are the regions corresponding to motifs in the module
            // check that all module motifs are indeed present in the sequence. If not, continue with next module
            for (Region region:candidateRegions) {
                for (int i=0;i<motifcounts.length;i++) {
                    if (module.isMotifCandidate(i,region.getType())) {motifcounts[i]++;totalmotifcounts++;}
                }               
            }            
            for (int i=0;i<motifcounts.length;i++) {
                if (motifcounts[i]==0) continue;
            }
            // now search for motifs within the candidate regions
            if (module.isOrdered()) { // motifs have to appear in order
                ArrayList<Region> included=new ArrayList<Region>(size);
                for (int i=0;i<=candidateRegions.size()-size;i++) {
                    //System.err.println("search "+i+"/"+(candidateRegions.size()-size)+"   candidates="+candidateRegions.size()+" size="+size+" included="+included.size());
                    searchModuleRecursivelyDirectStrand(candidateRegions, i, 0, targetSequence, module, included); // this will search for applicable combinations and add them
                }
                Collections.sort(candidateRegions,new ReverseOrderComparator()); // sort in reverse order
                included.clear();
                for (int i=0;i<=candidateRegions.size()-size;i++) {
                    //System.err.println("search "+i+"/"+(candidateRegions.size()-size)+"   candidates="+candidateRegions.size()+" size="+size+" included="+included.size());
                    searchModuleRecursivelyReverseStrand(candidateRegions, i, 0, targetSequence, module, included); // this will search for applicable combinations and add them
                }
            } else { // // motifs do not have to appear in order, but orientation constraints should apply!
                ArrayList<Region> included=new ArrayList<Region>(size);
                for (int i=0;i<=candidateRegions.size()-size;i++) {
                    //System.err.println("search "+i+"/"+(candidateRegions.size()-size)+"   candidates="+candidateRegions.size()+" size="+size+" included="+included.size());
                    searchUnorderedModuleRecursivelyDirectStrand(candidateRegions, i, 0, targetSequence, module, included, allowoverlap); // this will search for applicable combinations and add them
                }
            } // 
        } // end: for each Module   
    }

    /**
     * This method is used to find ordered modules on the direct strand
     *
     * @param regionslist The list of candidate regions from the source sequence
     * @param regionIndex The index of the region currently searched in regionslist
     * @param prevRegionEnd The position that the previous region in the module ended
     * @param singlemotifindex The index of the motif in the
     * @param module The template module to search for
     * @param targetsequence The new module track where applicable module occurrences should be stored
     * @param moduleregion The module region which is tested recursively
     * @return a flag indicating status. 0=everything OK, 1=motif type or region not applicable, 2=did not satisfy local space constraint, 3=did not satisfy global space constrainst
     */
    private int searchModuleRecursivelyDirectStrand(ArrayList<Region> sequenceregionslist, int regionIndex, int prevRegionEnd, RegionSequenceData targetsequence, Module module,  ArrayList<Region> included) {
         int singlemotifindex=included.size();
         int size=module.getCardinality();
         Region nextRegion=sequenceregionslist.get(regionIndex); // the next candidate to potentially add to the list
         if (!module.isMotifCandidate(singlemotifindex, nextRegion.getType(), nextRegion.getOrientation())) return 1; // region is not of correct motif type or orientation
         if (singlemotifindex>0) {
             int[] distanceConstraint=module.getDistance(singlemotifindex-1, singlemotifindex);
             if (module.getMaxLength()>0) {
                 int start=included.get(0).getRelativeStart();
                 int nextStart=nextRegion.getRelativeStart();
                 int end=nextRegion.getRelativeEnd();
                 if (nextStart-start+1>module.getMaxLength()) return 2; // this candidate did not satisfy the global and neither can anyone coming after
                 if (end-start+1>module.getMaxLength()) return 3; // this candidate did not satisfy the global constraint
             }
             if (distanceConstraint!=null) {
                 int distance=nextRegion.getRelativeStart()-(prevRegionEnd+1);
                 if (distance<distanceConstraint[0]) return 4; // region did not satisfy distance constraint. distance was too short
                 else if (distance>distanceConstraint[1]) return 5; // distance was too long
             }
         }
         // 'nextRegion' is an applicable candidate for motif with index 'singlemotifindex'. Add it to the list
         included.add(nextRegion);
         if (singlemotifindex<size-1) { // list is not complete yet. Add this motif and fill the remaining slots recursively
             // check next region as next candidate. if that does not hold check the ones after that until end.
             int result=0;
             for (int j=regionIndex+1;j<=sequenceregionslist.size()-(size-included.size());j++) {
                result=searchModuleRecursivelyDirectStrand(sequenceregionslist, j, nextRegion.getRelativeEnd(), targetsequence, module, included); // this will search for applicable combinations and add them
                if (result==2 || result==5) break;  // the distance constraint is not satisfied. No further candidates can be build from this base. Break out and backtrack!
             }
             result=0; // clear the mark since it only applies to one level
             included.remove(included.size()-1); // pop the last motif when backtracking
             return result;
         } else { // all motifs are found and applicable. create module region and add it!
               Region moduleregion=new Region(targetsequence, included.get(0).getRelativeStart(), nextRegion.getRelativeEnd());
               moduleregion.setType(module.getName());
               moduleregion.setOrientation(Region.DIRECT);
               double scoreSum=0;
               double scoreMax=0;
               for (int i=0;i<included.size();i++) {
                   Region singlemotif=included.get(i).clone();
                   scoreSum+=singlemotif.getScore();
                   if (singlemotif.getScore()>scoreMax) scoreMax=singlemotif.getScore();
                   singlemotif.setParent(targetsequence);
                   moduleregion.setProperty(module.getSingleMotifName(i), singlemotif);
               }
               moduleregion.setScore(scoreMax); // or use scoreSum?
               targetsequence.addRegion(moduleregion); // if (!targetsequence.containsRegion(moduleregion)) ...

               included.remove(included.size()-1); // pop the last motif and try to find others
               return 0;
         }        
    }


    private int searchModuleRecursivelyReverseStrand(ArrayList<Region> sequenceregionslist, int regionIndex, int prevRegionEnd, RegionSequenceData targetsequence, Module module,  ArrayList<Region> included) {
         int singlemotifindex=included.size();
         int size=module.getCardinality();
         Region nextRegion=sequenceregionslist.get(regionIndex); // the next candidate to potentially add to the list
         if (!module.isMotifCandidate(singlemotifindex, nextRegion.getType(), nextRegion.getOrientation()*(-1))) return 1; // NOTE: orientation flip *-1
         if (singlemotifindex>0) {
             int[] distanceConstraint=module.getDistance(singlemotifindex-1, singlemotifindex);
             if (module.getMaxLength()>0) {
                 int start=included.get(0).getRelativeEnd(); // using End instead
                 int nextStart=nextRegion.getRelativeEnd();
                 int end=nextRegion.getRelativeStart();
                 if (start-nextStart+1>module.getMaxLength()) return 2; // this candidate did not satisfy the global and neither can anyone coming after
                 if (start-end+1>module.getMaxLength()) return 3; // this candidate did not satisfy the global constraint
             }
             if (distanceConstraint!=null) {
                 int distance=prevRegionEnd-(nextRegion.getRelativeEnd()+1);
                 if (distance<distanceConstraint[0]) return 4; // region did not satisfy distance constraint. distance was too short
                 else if (distance>distanceConstraint[1]) return 5; // distance was too long
             }
         }
         // 'nextRegion' is an applicable candidate for motif with index 'singlemotifindex'. Add it to the list
         included.add(nextRegion);
         if (singlemotifindex<size-1) { // list is not complete yet. Fill the remaining slots recursively
             // check next region as next candidate. Ff that does not hold, check the ones after that until end.
             int result=0;
             for (int j=regionIndex+1;j<=sequenceregionslist.size()-(size-included.size());j++) {
                result=searchModuleRecursivelyReverseStrand(sequenceregionslist, j, nextRegion.getRelativeStart(), targetsequence, module, included); // this will search for applicable combinations and add them
                if (result==2 || result==5) break; // the distance constraint is not satisfied. No further candidates can be build from this base. Break out and backtrack!
             }
             result=0; // clear the mark since it only applies to one level
             included.remove(included.size()-1); // pop the last motif when backtracking
             return result;
         } else { // all motifs are found and applicable. create module region and add it!
               Region moduleregion=new Region(targetsequence, nextRegion.getRelativeStart(), included.get(0).getRelativeEnd());
               moduleregion.setType(module.getName());
               moduleregion.setOrientation(Region.REVERSE); 
               double score=0;
               for (int i=0;i<included.size();i++) {
                   Region singlemotif=included.get(i).clone();
                   score+=singlemotif.getScore();
                   singlemotif.setParent(targetsequence);
                   moduleregion.setProperty(module.getSingleMotifName(i), singlemotif);
               }
               moduleregion.setScore(score);
               targetsequence.addRegion(moduleregion); // if (!targetsequence.containsRegion(moduleregion)) ...
               included.remove(included.size()-1); // pop the last motif and try to find others
               return 0;
         }
    }

    
    /**
     * This method is used to find unordered modules on the direct strand
     *
     * @param regionslist The list of candidate regions from the source sequence
     * @param regionIndex The index of the region currently searched in regionslist
     * @param prevRegionEnd The position that the previous region in the module ended
     * @param singlemotifindex The index of the motif in the
     * @param module The template module to search for
     * @param targetsequence The new module track where applicable module occurrences should be stored
     * @param moduleregion The module region which is tested recursively
     * @return a flag indicating status. 0=everything OK, 1=motif type or region not applicable, 2=did not satisfy local space constraint, 3=did not satisfy global space constrainst
     */
    private int searchUnorderedModuleRecursivelyDirectStrand(ArrayList<Region> sequenceregionslist, int regionIndex, int prevRegionEnd, RegionSequenceData targetsequence, Module module,  ArrayList<Region> included, boolean allowoverlap) {
         int singlemotifindex=included.size();
         int size=module.getCardinality();
         Region nextRegion=sequenceregionslist.get(regionIndex); // the next candidate to potentially add to the list
         if (singlemotifindex>0) { // check if we the current TFBS set is still within the max distance
             if (!allowoverlap) {
                 int prevEnd=included.get(included.size()-1).getRelativeEnd();
                 int nextStart=nextRegion.getRelativeStart();   
                 if (nextStart<prevEnd) return 4; // signal overlap
             }
             if (module.getMaxLength()>0) {
                 int start=included.get(0).getRelativeStart();
                 int nextStart=nextRegion.getRelativeStart();
                 int end=nextRegion.getRelativeEnd();
                 if (nextStart-start+1>module.getMaxLength()) return 2; // this candidate did not satisfy the global and neither can anyone coming after
                 if (end-start+1>module.getMaxLength()) return 3; // this candidate did not satisfy the global constraint
             }
         }
         included.add(nextRegion);
         if (singlemotifindex<size-1) { // list is not complete yet. Fill the remaining slots recursively
             // check next region as next candidate. if that does not hold check the ones after that until end.
             int result=0;
             for (int j=regionIndex+1;j<=sequenceregionslist.size()-size;j++) {
                result=searchUnorderedModuleRecursivelyDirectStrand(sequenceregionslist, j, nextRegion.getRelativeEnd(), targetsequence, module, included, allowoverlap); // this will search for applicable combinations and add them
                if (result==2) break;  // the distance constraint is not satisfied. No further candidates can be build from this base. Break out and backtrack!
            }
            result=0; // clear the mark since it only applies to one level
            included.remove(included.size()-1);
            return result;
         } else { // all motifs are found and so far applicable. Check if the motif set fits the module template, and, if so, create module region and add it!
               int regionOrientation=Region.DIRECT;
               HashMap<String,Region> match=isModuleMatch(module, included, true); // true means direct orientation
               if (match==null && module.motifsHaveOpposingOrientations()) {
                   match=isModuleMatch(module, included, false); // false means reverse orientation
                   if (match!=null) regionOrientation=Region.REVERSE;
               }
               if (match!=null) {
                   // determine the span of the region
                   int moduleStart=Integer.MAX_VALUE;
                   int moduleEnd=Integer.MIN_VALUE;
                   for (String modulemotifname:match.keySet()) {
                       Region singlemotif=match.get(modulemotifname).clone();
                       if (singlemotif.getRelativeStart()<moduleStart) moduleStart=singlemotif.getRelativeStart();
                       if (singlemotif.getRelativeEnd()>moduleEnd) moduleEnd=singlemotif.getRelativeEnd();                       
                   }
                   // now create module
                   Region moduleregion=new Region(targetsequence, moduleStart, moduleEnd); 
                   moduleregion.setType(module.getName());
                   moduleregion.setOrientation(regionOrientation);
                   double score=0;
                   for (String modulemotifname:match.keySet()) {
                       Region singlemotif=match.get(modulemotifname).clone();
                       score+=singlemotif.getScore();
                       singlemotif.setParent(targetsequence);
                       moduleregion.setProperty(modulemotifname, singlemotif);
                   }
                   moduleregion.setScore(score);
                   targetsequence.addRegion(moduleregion); // if (!targetsequence.containsRegion(moduleregion)) ...
               } 
               included.remove(included.size()-1); // pop the last motif and try to find others
               return 0;
         }        
    }    

    /** This is used by 'unordered module search' to check that a candidate set of motif regions 
     *  is indeed a match to the given module template.
     *  To match the module, the candidate motifs must each match one of the modulemotifs and
     *  their orientations must satisfy any given constraints
     *  The candidates should have already been verified to be within the maximum module span allowed 
     */
    private HashMap<String,Region> isModuleMatch(Module module, ArrayList<Region> candidates, boolean directorientation) {
        // the method works by creating a MxN matrix for M modulemotifs and N region candidates (M and N will usually be the same value).
        // Next, for each modulemotif we check if each of the regions can be a match for this modulemotif
        boolean[][] matchmatrix=new boolean[module.getCardinality()][candidates.size()]; // each cell is TRUE if the given Region (by index) is a match for the module motif (by index)
        int modulesize=module.getCardinality();
        int regionssize=candidates.size(); // these two sizes should be equal !!!
        for (int i=0;i<modulesize;i++) {
            for (int j=0;j<regionssize;j++) {
                Region candidate=candidates.get(j);
                int orientation=candidate.getOrientation();
                if (!directorientation) orientation=orientation*(-1);
                matchmatrix[i][j]=module.isMotifCandidate(i, candidate.getType(), orientation);
            }
        }
        boolean[] allpresent=new boolean[modulesize];
        boolean multiplesolutions=false;
        for (int i=0;i<modulesize;i++) {
            for (int j=0;j<regionssize;j++) {
                boolean match=matchmatrix[i][j];
                if (allpresent[i] && match) multiplesolutions=true;
                allpresent[i]=allpresent[i]||match;
            }
        }
        for (int i=0;i<modulesize;i++) {
            if (!allpresent[i]) return null; // not all modulemotifs had matching region. This group of candidates is not a match to the module!
        }
        // we have now established that there is a matching combination.
        if (!multiplesolutions) { // only one solution possible
            HashMap<String,Region> map=new HashMap<String,Region>();
            for (int i=0;i<modulesize;i++) {
                int col=getMatchingColumn(matchmatrix, i);
                String mmname=module.getSingleMotifName(i);
                map.put(mmname, candidates.get(col));
            }
            return map;
        } else { // there are potentially multiple possible solutions
            int[] solution=new int[modulesize];
            for (int i=0;i<solution.length;i++) solution[i]=-1; // initialize to all -1
            boolean found=findSolution(matchmatrix, solution, 0);
            if (found) {
                HashMap<String,Region> map=new HashMap<String,Region>();
                for (int i=0;i<modulesize;i++) {
                    String mmname=module.getSingleMotifName(i);
                    map.put(mmname, candidates.get(solution[i]));
                }
                return map;
            } else {
                return null;
            }
        }     
    }

    /** Finds a 1-to-1 assignment of motifs and regions given a matrix which marks potential candidate mappings
     *  Returns TRUE if such an assignment exists (in which case the mapping will be present in the solution array)
     */
    private boolean findSolution(boolean matrix[][], int[] solution, int currentMotif) {
        int lastRegion=solution[currentMotif];
        for (int i=lastRegion+1;i<matrix.length;i++) { // search row of current motif to find potential candidate regions
            if (matrix[currentMotif][i]) { // possible match, but check to see if it is taken!
                boolean istaken=false;
                for (int s=0;s<currentMotif;s++) {if (solution[s]==i) {istaken=true;break;} }
                if (!istaken) { // this motif-region mapping is valid
                    solution[currentMotif]=i;
                    if (currentMotif==solution.length-1) return true; // we have found a complete solution!!
                    boolean solved=findSolution(matrix, solution, currentMotif+1); // found partial solution. Now search next position
                    if (solved) return true;
                }
            }
        } // end try each region candiate for the current motif
        solution[currentMotif]=-1; // reset this position and backtrack
        return false;
    }

    /** Returns the index of the first column with a TRUE value in the given row in the matrix */
    private int getMatchingColumn(boolean[][] matrix, int row) {
        for (int j=0;j<matrix[row].length;j++) {
            if (matrix[row][j]) return j;
        }
        return -1; // this should not happen
    }


    /** A Comparator used to reverse the order of Regions in a list based on the end-point of each region instead of start-point */
    private class ReverseOrderComparator implements Comparator<Region> {
            @Override
            public int compare(Region region1, Region region2) { //
                int end1=region1.getRelativeEnd();
                int end2=region2.getRelativeEnd();
                if (end1>end2) return -1;
                else if (end2>end1) return 1;
                else {
                    int start1=region1.getRelativeStart();
                    int start2=region2.getRelativeStart();
                    if (start1>start2) return -1;
                    else if (start2>start1) return 1;
                    else return 0;
                }
            }
    }
    
    
  private class ScanSequenceTask<RegionSequenceData> implements Callable<RegionSequenceData> {
        final RegionDataset targetDataset;
        final RegionDataset sourceDataset;
        final int[] counters;
        final String sequencename;
        final OperationTask task;
     
        
        public ScanSequenceTask(RegionDataset sourceDataset, RegionDataset targetDataset, String sequencename, OperationTask task, int[] counters) {;
           this.counters=counters;
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset;    
           this.task=task;           
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public RegionSequenceData call() throws Exception {
            synchronized(counters) {
                counters[0]++; // number of downloads started
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            motiflab.engine.data.RegionSequenceData sourceSequence=(motiflab.engine.data.RegionSequenceData )sourceDataset.getSequenceByName(sequencename);
            motiflab.engine.data.RegionSequenceData targetSequence=(motiflab.engine.data.RegionSequenceData )targetDataset.getSequenceByName(sequencename);
            searchInSequence(sourceSequence,targetSequence, task);
            synchronized(counters) {
                counters[1]++; // number of downloads finished
                task.setProgress(counters[1], counters[2]);   
                task.setStatusMessage("Executing SimpleModuleScanner:  ("+counters[1]+"/"+counters[2]+")");
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return (RegionSequenceData)targetSequence;
        }   
    }       

}