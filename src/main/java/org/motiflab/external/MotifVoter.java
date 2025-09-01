/*
 
 
 */

package org.motiflab.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.operations.Operation_ensemblePrediction;

/**
 *
 * @author Kjetil
 */
public class MotifVoter extends EnsemblePredictionMethod {
    private String[] motifDiscoveryPrograms=null; // the names of the motif discovery programs (or rather the tracks containing their predicted sites);
    private ArrayList<String> motifnames=null; // contains all motifnames
    private HashMap<String,Integer> motifToProgramMap=null; // maps the name of a motif to the index of the program predicting it
    private double[][] motifSimilarities=null; // stores similarities between motifs
    private RegionDataset[] sources=null;
    private SequenceCollection sequenceCollection=null;

    public MotifVoter() {
        this.name="MotifVoter";
        this.programclass="EnsemblePrediction";
        this.serviceType="bundled";

        addResultParameter("Result", RegionDataset.class, null, null, "output track");
        addResultParameter("Motifs", MotifCollection.class, null, null, "Motif collection");
    }



    @Override
    public void execute(OperationTask task) throws Exception {
        System.err.println("Executing MotifVoter");
        int totalsubtasks=3;
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        sources=(RegionDataset[])task.getParameter(Operation_ensemblePrediction.SOURCE_DATA);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for ensemble motif prediction with MotifVoter");

        sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        if (sequenceCollection==null) sequenceCollection=task.getEngine().getDefaultSequenceCollection();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing ensemblePrediction:  (1/"+totalsubtasks+")");
        task.setProgress(1, totalsubtasks);
        motifDiscoveryPrograms=new String[sources.length]; // names of motif discovery programs (actually region track names)
        motifnames=new ArrayList<String>(); // names of all motifs predicted by all programs together
        motifToProgramMap=new HashMap<String, Integer>(); // maps motif names to their corresponding MD program index in the motifDiscoveryPrograms array
        for (int i=0;i<sources.length;i++) { // for each motif discovery program
            RegionDataset predictedSitesTrack=sources[i];
            String programName=predictedSitesTrack.getName();
            motifDiscoveryPrograms[i]=programName;
            for (FeatureSequenceData seq:predictedSitesTrack.getAllSequences()) {
                ArrayList<Region> regions=((RegionSequenceData)seq).getOriginalRegions();
                for (Region r:regions) {
                    String type=r.getType();
                    if (!motifnames.contains(type)) motifnames.add(type);
                    if (!motifToProgramMap.containsKey(type)) motifToProgramMap.put(type, i);
                }
            }        
        }
        motifnames.trimToSize();
        for (int i=0;i<motifnames.size();i++) {
            System.err.println("Motif["+i+"] => "+motifnames.get(i));
        }
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing ensemblePrediction with MotifVoter:  (2/"+totalsubtasks+")");
        task.setProgress(2, totalsubtasks);
        Thread.yield();
        
        // calculate the similarity between all pairs of motifs. This covers steps 1-3 in the paper
        calculateSimilarityMatrix();
        for (int i=0;i<motifnames.size()-1;i++) {
            for (int j=i;j<motifnames.size();j++) {
                System.err.println("Sim["+i+"]["+j+"] = "+motifSimilarities[i][j]);
            }
        }


        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing ensemblePrediction:  (3/"+totalsubtasks+")");
        task.setProgress(3, totalsubtasks);
        Thread.yield();

        // find a subset of motifs X that maximizes the A(X) score using a heuristic search. This covers steps 4-9 in the paper (but I leave out step 8 until it is needed)
        int size=motifnames.size();
        SimilaritySorter sorter=new SimilaritySorter();
        double bestAscore=0;
        HashSet<int[]> bestSets=new HashSet<int[]>();
        for (int z=0;z<size;z++) {
             System.err.print("\n\nTARGET z="+z);
             Integer[] others=getComplementMotifSetAsIntegers(new int[]{z});
             sorter.setTarget(z);
             Arrays.sort(others,sorter);
             System.err.println(".   Complement set (sorted) = "+debugArray(others));
             for (int j=0;j<others.length;j++) {
                 int[] candidateSet=new int[j+2];
                 candidateSet[0]=z;
                 for (int k=0;k<=j;k++) {candidateSet[1+k]=others[k];}
                 System.err.println("------------\nCandidate set => "+debugArray(candidateSet));
                 double candidateAscore=Ascore(candidateSet);
                 System.err.println("   A-score="+candidateAscore+"  => "+debugArray(candidateSet));
                 if (candidateAscore>bestAscore) {
                     bestSets.clear();
                     bestSets.add(candidateSet);
                     bestAscore=candidateAscore;
                 } else if (candidateAscore==bestAscore) bestSets.add(candidateSet);
             }
        }
        System.err.println("best A-score="+bestAscore+"  obtained by "+bestSets.size()+" subsets");
        int bestQ=0;
        int countBestQ=0;
        for (int[] candidate:bestSets) {
            int qscore=Qscore(candidate);
            System.err.println("Candidate with "+candidate.length+" motifs from "+qscore+" programs");
            if (qscore>bestQ) {countBestQ=1;bestQ=qscore;}
            else if (qscore==bestQ) countBestQ++;
        }
        System.err.println(countBestQ+" set(s) obtained the best Q-score which was "+bestQ);
        int[] bestCandidate=null;
        int random=(int)(Math.random()*countBestQ)+1;
        int found=0;
        for (int[] candidate:bestSets) {
            if (Qscore(candidate)==bestQ) {
                found++;
                if (found==random) {bestCandidate=candidate;break;}
            }
        }
        System.err.println("The best candidate selected had "+bestCandidate.length+" motifs");
        for (int i=0;i<bestCandidate.length;i++) {
            System.err.println("    Motif#"+i+":"+motifnames.get(bestCandidate[i]));
        }
        // 10: set X=X(z,j) with the maximum A( X(z,j) ) score. If there are several such X(z,j) pick the one with the largest Q( X(z,j,) )
        // 11: Extract and align sites of motifs in X
        // 12: construct PWMs based on the aligned motifs (and remap sites?)
        RegionDataset targetDataset=sources[0].clone();
        targetDataset.setName("MotifVoterSites");
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);
        task.setParameter("Motifs", new MotifCollection("MotifVoterMotifs"));
        cleanUp();
    }

    /** Removes all local field variables to free up resources (this is done since MotifVoter is meant to be a singleton object */
    private void cleanUp() {
        motifDiscoveryPrograms=null;
        motifToProgramMap=null;
        motifSimilarities=null;
        sources=null;
        motifnames=null;
        sequenceCollection=null;
    }

    /** calculates the similarities between all pairs of motifs */
    private void calculateSimilarityMatrix() {
        int size=motifnames.size();
        motifSimilarities=new double[size][size];
        for (int i=0;i<size-1;i++) {
            for (int j=i;j<size;j++) {
                if (i==j) {motifSimilarities[i][j]=1;continue;}
                double sim=calculateMotifSimilarity(i,j);
                motifSimilarities[i][j]=sim;
                motifSimilarities[j][i]=sim; // symmetrical matrix
            }
        }
    }



    /** Given two motifs this method will calculate the similarity between the two motifs
     *  @param motif1 the index number of the first motif. The index refers to the 'motifnames' field variable array
     *  @param motif2 the index number of the second motif . The index refers to the 'motifnames' field variable array
     */
    private double calculateMotifSimilarity(int motif1, int motif2) {
        String motif1Name=motifnames.get(motif1);
        String motif2Name=motifnames.get(motif2);
        int program1index=motifToProgramMap.get(motif1Name);
        int program2index=motifToProgramMap.get(motif2Name);
        RegionDataset sourceProgram1=sources[program1index];
        RegionDataset sourceProgram2=sources[program2index];
        int union=0;
        int intersect=0;
        for (String sequence:sequenceCollection.getAllSequenceNames()) {
            RegionSequenceData seqProgram1=(RegionSequenceData)sourceProgram1.getSequenceByName(sequence);
            RegionSequenceData seqProgram2=(RegionSequenceData)sourceProgram2.getSequenceByName(sequence);
            int sequenceSize=seqProgram1.getSize();
            ArrayList<Region> regionsMotif1=seqProgram1.getAllRegions(motif1Name);
            ArrayList<Region> regionsMotif2=seqProgram2.getAllRegions(motif2Name);
            int firstPos=0;
            if (regionsMotif1.isEmpty() && regionsMotif2.isEmpty()) continue; // no contributions from this sequence
            if (!regionsMotif1.isEmpty()) firstPos=regionsMotif1.get(0).getRelativeStart();
            if (!regionsMotif2.isEmpty() && regionsMotif2.get(0).getRelativeStart()<firstPos) firstPos=regionsMotif2.get(0).getRelativeStart();
            int[] p1flags=new int[]{0,0};
            int[] p2flags=new int[]{0,0};
            for (int i=firstPos;i<sequenceSize;i++) { // compare the two
                boolean coveredIn1=positionCovered(regionsMotif1, i, p1flags);
                boolean coveredIn2=positionCovered(regionsMotif2, i, p2flags);
                if (coveredIn1 || coveredIn2) union++;
                if (coveredIn1 && coveredIn2) intersect++;
                if (p1flags[1]==1 && p2flags[1]==2) break; // passed last region in both tracks
            }
        }
        return (double)intersect/(double)union;
    }
  
    /** This method returns TRUE if the given position is covered by a region in the list
     *  the flags array contains two values which can be updated by this method and used
     *  to communicate back. The first value is the current index pointer into the list
     *  The second is a flag which should be set to 1 if there are no more regions left
     *  in the list (i.e. no higher positions can return a TRUE value)
     */
    private boolean positionCovered(ArrayList<Region> list, int pos, int[] flags) {
        if (flags[1]==1) return false;
        int currentIndex=flags[0];
        if (currentIndex>=list.size()) {flags[1]=1;return false;}
        Region r=list.get(currentIndex);
        if (pos<r.getRelativeStart()) return false; // not reached first region
        else if (pos>=r.getRelativeStart() && pos<=r.getRelativeEnd()) return true; // pos within current region
        else { // pos not within current region, try the next one
           currentIndex++;
           flags[0]=currentIndex;
           if (currentIndex>=list.size()) {flags[1]=1;return false;}
           boolean ok=false;
           while (!ok) {
              r=list.get(currentIndex);
              if (r.getRelativeEnd()<pos) {// this region is fully within the previous region
                 currentIndex++;
                 flags[0]=currentIndex;
                 if (currentIndex>=list.size()) {flags[1]=1;return false;}
              } else {
                 ok=true;
              }
           }
           // next applicable region found
           if (pos<r.getRelativeStart()) return false; // not reached next region
           else if (pos>=r.getRelativeStart() && pos<=r.getRelativeEnd()) return true; // pos within current region
           else return false; // I don't think this is really an option here...
        }
    }

    /**
     * Returns the w-score of a motif set as described in the MotifVoter paper
     * @param set
     * @return
     */
    private double wScore(int[] set) {
        double simX=meanSimilarityOfSet(set);
        double sum=0;
        for (int i=0;i<set.length;i++) {
            for (int j=0;j<set.length;j++) {
                double square=(motifSimilarities[set[i]][set[j]]-simX)*(motifSimilarities[set[i]][set[j]]-simX);
                sum+=square;
            }
        }
        double denominator=Math.sqrt(sum);
        //System.err.println("   wScore mean similarity="+simX+"  denominator="+denominator+"");
        //if (denominator==0) return 0;
        if (denominator==0) return simX;
        else return simX/denominator;
    }

    /**
     * Returns the A-score of the motif set as described in the MotifVoter paper
     * This is the ratio " w(X)/w(P-X) ". That is, the similarity w-score of
     * a set divided by the w-score of the complement set (P-X)
     * @param set A set of motif indices
     * @return
     */
    private double Ascore(int[] set) {
        int[] complementSet=getComplementMotifSet(set);
        double wscore=wScore(set);
        double wcomplementScore=wScore(complementSet);
        System.err.println("    wScore="+wscore+"  complement="+wcomplementScore);
        //if (wcomplementScore==0) return 0;
        if (wcomplementScore==0) return wscore;
        else return wscore/wcomplementScore;
    }

    /**
     * Returns the Q-score of the motif set as described in the MotifVoter paper
     * The Q-score is the number of motif discovery programs that has contributed
     * to detecting the motifs in the set (which is the same as the number of
     * different tracks that the motifs originate from).
     * @param set A set of motif indices
     * @return
     */
    private int Qscore(int[] set) {
        ArrayList<Integer> programs=new ArrayList<Integer>(motifDiscoveryPrograms.length);
        for (int motifindex:set) {
            int programIndex=motifToProgramMap.get(motifnames.get(motifindex));
            if (!programs.contains(programIndex)) programs.add(programIndex);
        }
        return programs.size();
    }

    /** Returns the mean similarity between motifs in a set of motif indices */
    private double meanSimilarityOfSet(int[] set) {
        int size=set.length;
        if (size==0) return 0;
        double sum=0;
        for (int i=0;i<set.length;i++) {
            for (int j=0;j<set.length;j++) {
                sum+=motifSimilarities[set[i]][set[j]];
                //System.err.println("       *["+set[i]+"]["+set[j]+"]="+motifSimilarities[set[i]][set[j]]);
            }
        }
        //System.err.println("meanSimilarityOfSet:  Size="+size+"   sum="+sum+"  X^2="+(size*size)+"  mean="+(sum/(double)(size*size)));
        return sum/(double)(size*size);
    }

    /** for a given set of motifs this will return the complement set
     *  The set is represented by an integer array containing motif indices
     *  (which point into the motifnames list)
     */
    private int[] getComplementMotifSet(int[] set) {
        int[] complement=new int[motifnames.size()-set.length];
        int index=0;
        for (int i=0;i<motifnames.size();i++) {
            if (!arrayContainsNumber(set, i)) {
                complement[index]=i;
                index++;
            }
        }
        return complement;
    }

    /** for a given set of motifs this will return the complement set
     *  The set is represented by an integer array containing motif indices
     *  (which point into the motifnames list)
     */
    private Integer[] getComplementMotifSetAsIntegers(int[] set) {
        Integer[] complement=new Integer[motifnames.size()-set.length];
        int index=0;
        for (int i=0;i<motifnames.size();i++) {
            if (!arrayContainsNumber(set, i)) {
                complement[index]=i;
                index++;
            }
        }
        return complement;
    }

    /** Returns TRUE if the given array contains the given number */
    private boolean arrayContainsNumber(int[] array, int number) {
        for (int i=0;i<array.length;i++) {
            if (array[i]==number) return true;
        }
        return false;
    }


    private class SimilaritySorter implements Comparator<Integer> {
        private int target=0;       
        
        public void setTarget(int target) {this.target=target;}       
        
        @Override
        public int compare(Integer motif1, Integer motif2) {
            double sim1=motifSimilarities[target][motif1];
            double sim2=motifSimilarities[target][motif2];
            if (sim1==sim2) return 0;
            else if (sim1>sim2) return -1;
            else return 1;
        }
        
    }

    private String debugArray(Object obj) {
        if (obj instanceof Integer[]) {
            String res="("+((Integer[])obj).length+")[";
            for (Integer i:(Integer[])obj) {
                res+=""+i+" ";
            }
            res+="]";
            return res;
        } else if (obj instanceof int[]) {
             String res="("+((int[])obj).length+")[";
            for (Integer i:(int[])obj) {
                res+=""+i+" ";
            }
            res+="]";
            return res;
        } else return "";
    }
}
