/*
 
 
 */

package org.motiflab.external;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataGroup;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class SimpleModuleSearcher extends ModuleDiscovery {
    //private HashMap<MotifTupleKey,int[]> counts=new HashMap<MotifTupleKey,int[]>(); // key is motif tuple. Value is int[]{total count,sequence support}
    private RegionDataset motifsTrack; // holds the remaining motifs after filtering
    private RegionDataset moduleTrack; // holds the modules
    private ModuleCollection moduleCollection;
    private transient HashMap<String,Short> motifNumber; // a LUT that maps motifnames (all applicable ones) to short numbers
    private transient HashMap<Short,String> motifNumberToName; // a LUT that maps short numbers to motifnames (all applicable ones). This is the inverse map of the one above
    private HashMap<MotifTupleKey,ModuleCRM> modules;
    private HashMap<String,Double> motifIC;
    private Utilities utilities;

    private static final int COUNT_MOTIF=0;
    private static final int MARK_MOTIF=1;
    private static final int ADD_MODULE=2;


    public SimpleModuleSearcher() {
        this.name="SimpleModuleSearcher";
        this.programclass="ModuleDiscovery";
        this.serviceType="bundled";

        addSourceParameter("Motif track", RegionDataset.class, null, null, "input track");

        addParameter("Motifs",MotifCollection.class, null,new Class[]{MotifCollection.class},null,true,false);
        addParameter("Size",Integer.class, new Integer(3),new Integer[]{1,10},"Number of motifs in the module",true,false);
        addParameter("Max span",Integer.class, new Integer(80),new Integer[]{1,1000},"Maximum allowed length of module",true,false);
        addParameter("Sequence support",Integer.class, new Integer(25),new Integer[]{1,1000},"Minimum number of sequences the module must appear in",true,false);
        addParameter("Report",Integer.class, new Integer(5),new Integer[]{1,100},"Number of top modules to report",true,false);
        addParameter("Sort results by",String.class, "IC-content",new String[]{"Support","IC-content"},"How to determine which modules are better when selecting the top modules",true,false);
        addParameter("Only known interactions",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Keep only tuples with known physical interactions (pairwise)",true,false);
        addParameter("Force heterogeneous",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, the same motif can only appear once in a module",true,false);
        addParameter("Required motifs",DataGroup.class, null,new Class[]{MotifPartition.class,MotifCollection.class},"If a Motif Collection is specified here, at least one of the motifs in the collection must be present in each module site. If a Motif Partition is specified here, at least one motif from each cluster in the partition must be present in each module site.",false,false);
        addParameter("Cluster motifs",MotifPartition.class, null,new Class[]{MotifPartition.class},"If provided, motifs that are grouped together in the same cluster will be considered as instances of the same 'meta-motif' with respect to module discovery",false,false);
        addResultParameter("Result", RegionDataset.class, null, null, "output track");
        addResultParameter("Modules", ModuleCollection.class, null, null, "Module collection");

    }


    @Override
    public void execute(OperationTask task) throws Exception {
        Data[] sources=(Data[])task.getParameter(SOURCES);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for module discovery with SimpleModuleSearcher");
        if (!(sources[0] instanceof RegionDataset)) throw new ExecutionError("SYSTEM ERROR: SimpleModuleSearcher requires a Motif track as source input");
        if (!((RegionDataset)sources[0]).isMotifTrack()) throw new ExecutionError("SYSTEM ERROR: SimpleModuleSearcher requires a Motif track as source input");        
        RegionDataset source=(RegionDataset)sources[0];
        motifsTrack=source.clone();
        motifsTrack.rename("tuples");
        motifsTrack.setMotifTrack(true);
        boolean onlyInteracting=(Boolean)task.getParameter("Only known interactions");
        boolean forceHeterogeneous=(Boolean)task.getParameter("Force heterogeneous");
        DataGroup requiredMotifs=(DataGroup)task.getParameter("Required motifs");
        if (requiredMotifs!=null && !(requiredMotifs instanceof MotifCollection || requiredMotifs instanceof MotifPartition)) throw new ExecutionError("The 'Required motifs' parameter should specify either a Motif Collection or a Motif Partition");
        MotifPartition motifclusters=(MotifPartition)task.getParameter("Cluster motifs");  
        int modulesize=((Integer)task.getParameter("Size")).intValue();
        if (modulesize<2) throw new ExecutionError("Module size should be at least 2");
        int minSupport=((Integer)task.getParameter("Sequence support")).intValue();
        if (minSupport<2) throw new ExecutionError("Minimum sequence support should be at least 2");
        MotifCollection motifcollection=(MotifCollection)task.getParameter("Motifs");
        if (((String)task.getParameter("Sort results by")).equalsIgnoreCase("IC-content")) motifIC=getICforAllMotifs(engine);
        else motifIC=null;
        utilities=new Utilities();
        utilities.setMotifsToUse((motifclusters!=null)?motifclusters:motifcollection);
        motifNumber=new HashMap<String,Short>(); // a LUT that maps motifnames (or clusternames) to short numbers
        motifNumberToName=new HashMap<Short,String>(); // a LUT that maps motifnames (or clusternames) to short numbers
        modules=new HashMap<MotifTupleKey,ModuleCRM>(); // stores the final modules
        short i=0;
        for (String motifname:motifcollection.getAllMotifNames()) { // create LUTs to convert between motifs and short numbers representing them
            String usemotifname=utilities.getMotifType(motifname);
            motifNumber.put(usemotifname, i);
            motifNumberToName.put(i,usemotifname);
            i++;
        }
        utilities.setInteractingAndForceHeterogeneous(onlyInteracting,forceHeterogeneous); // this must be called AFTER the motifName<->motifNumber LUTs have been setup        
        SequenceCollection sequencecollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        ArrayList<Sequence> sequences=sequencecollection.getAllSequences(engine);
        int numberofsequences=sequences.size();
        if (numberofsequences<minSupport) throw new ExecutionError("The number of sequences in the collection '"+sequencecollection.getName()+"' ("+numberofsequences+") is less than the requested sequence support ("+minSupport+")");
        HashMap<String,Integer> motifsupport=new HashMap<String,Integer>(); // contains counts for all sequences. int[0] is total occurrences, int[1] is sequence support
        for (int s=0;s<numberofsequences;s++) {
            RegionSequenceData seq=(RegionSequenceData)motifsTrack.getSequenceByName(sequences.get(s).getName());
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing operation: moduleDiscovery : counting motifs ("+(int)(s+1)+"/"+numberofsequences+")");
            task.setProgress(s, numberofsequences); //
            Thread.yield();
            HashSet<String> present=new HashSet<String>();
            for (Region r:seq.getOriginalRegions()) {
              if (!motifNumber.containsKey(utilities.getRegionType(r))) continue; // this motif type is not present in the collection we are searching for
              present.add(utilities.getRegionType(r));
            }
             // determine sequence support for each motif (of applicable types)
            for (String key:present) {
                if (!motifsupport.containsKey(key)) motifsupport.put(key, new Integer(1));
                else {
                    motifsupport.put(key, new Integer(motifsupport.get(key)+1));
                }
            }
        }
        // filter unadmissible single motifs (those that appear in less than the required number of sequences)
        i=0;
        int presentsize=motifsupport.size();
        Iterator<String> motifiterator=motifsupport.keySet().iterator();
        while (motifiterator.hasNext()) {
            String motifname=motifiterator.next();
            int seqcount=motifsupport.get(motifname);
            if (i%100==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                task.setStatusMessage("Executing operation: moduleDiscovery : pre-filtering ("+(int)(i+1)+"/"+presentsize+")");
                task.setProgress(i, presentsize);
                Thread.yield();
            }
            if (seqcount<minSupport) motifiterator.remove();
        }
        
        for (int s=0;s<numberofsequences;s++) {
            RegionSequenceData seq=(RegionSequenceData)motifsTrack.getSequenceByName(sequences.get(s).getName());       
            ArrayList<Region> regions=((RegionSequenceData)seq).getOriginalRegions(); // this is a clone anyway
            Iterator iterator=regions.iterator();
            while (iterator.hasNext()) {
                Region r=(Region)iterator.next();
                if (!motifsupport.containsKey(utilities.getRegionType(r))) iterator.remove();
            }
        }
        // end single-motif filtering
        engine.logMessage("Found "+motifsupport.size()+" motifs occurring in at least "+minSupport+" sequences");
        //for (String s:motifsupport.keySet()) System.err.println(s);
        motifsupport=null;

        moduleTrack=new RegionDataset("tempModuleTrack");
        moduleTrack.setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));
        moduleCollection=new ModuleCollection("tempcollection"); // it will be renamed in Operation_moduleDiscovery

        // find admissible tuples of the correct size and create Modules based on the tuples 
        int maxdistance=((Integer)task.getParameter("Max span")).intValue();
                
        for (int k=2;k<=modulesize;k++) {
            boolean lastrun=(k==modulesize);
            HashMap<MotifTupleKey,int[]> result=findTuplesAndFilterUnadmissible(motifsTrack, sequences, k, maxdistance, minSupport, onlyInteracting, forceHeterogeneous, task, lastrun);
            if (lastrun) {  // Create modules for tuples that have not been filtered
                ArrayList<MotifTupleKey> sortedlist=new ArrayList<MotifTupleKey>(result.keySet());
                Collections.sort(sortedlist, new SortOrderComparator(result,(motifIC!=null)));                
                for (MotifTupleKey key:sortedlist) {
                    ModuleCRM cisRegModule=createModuleForTuple(key, maxdistance); // this will create a new module and add it to the 'modules' hashtable                    
                    moduleCollection.addModuleToPayload(cisRegModule);
                }
            }
            Runtime.getRuntime().gc(); // this algorithm uses a lot of memory so try to clean up after each run!        
        }
        // now search the remaining motifs for tuples that match modules
        findModuleSites(motifsTrack, sequences, modulesize, maxdistance, onlyInteracting, forceHeterogeneous, task);

        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", moduleTrack);
        task.setParameter("Modules", moduleCollection);
    }

    private HashMap<MotifTupleKey,int[]> findTuplesAndFilterUnadmissible(RegionDataset dataset, ArrayList<Sequence> sequences, int modulesize, int maxdist, int minSupport, boolean interactionsOnly, boolean forceHeterogeneous, OperationTask task, boolean lastrun) throws Exception {
        HashMap<MotifTupleKey,int[]> totalcounts=new HashMap<MotifTupleKey,int[]>(); // contains counts for all sequences. int[0] is total occurrences, int[1] is sequence support
        for (int s=0;s<sequences.size();s++) {
            RegionSequenceData seq=(RegionSequenceData)dataset.getSequenceByName(sequences.get(s).getName());
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing operation: moduleDiscovery : searching for tuples of size "+modulesize+" ("+(int)(s+1)+"/"+sequences.size()+")");
            task.setProgress(s, sequences.size()); // the +20 is just to not reach 100% in this loop
            Thread.yield();
            // find admissible n-tuples in a single sequenec
            HashMap<MotifTupleKey,int[]> seqcounts=new HashMap<MotifTupleKey,int[]>();
            ArrayList<Region> included=new ArrayList<Region>();
            ArrayList<Region> candidateRegions=seq.getOriginalRegions();
            for (int i=0;i<=candidateRegions.size()-modulesize;i++) {
               searchTuplesRecursively(candidateRegions, i, modulesize, maxdist, included, interactionsOnly, forceHeterogeneous, seqcounts, COUNT_MOTIF, engine, seq.getName()); // this will fill up 'seqcounts'
            }
            // add to total counts
            for (MotifTupleKey key:seqcounts.keySet()) {
                int[] occurrences=seqcounts.get(key);
                if (!totalcounts.containsKey(key)) totalcounts.put(key, new int[]{occurrences[0],1});
                else {
                    int[] tuplecounts=totalcounts.get(key);
                    tuplecounts[0]+=occurrences[0];
                    tuplecounts[1]++;
                }
            }
        }
        // remove unadmissible tuples from totalcounts
        //System.err.println(modulesize+"-tuples counted="+totalcounts.size());
        Set<MotifTupleKey> keyset=totalcounts.keySet();
        Iterator<MotifTupleKey> iterator=keyset.iterator();
        while (iterator.hasNext()) {
            MotifTupleKey key=iterator.next();
            int[] tuplecounts=totalcounts.get(key);
            if (tuplecounts[1]<minSupport) iterator.remove();
        }
        engine.logMessage("Found "+totalcounts.size()+" admissible "+((interactionsOnly)?"interacting ":"")+"tuples of size "+modulesize+" occurring in at least "+minSupport+" sequences (maximum module length = "+maxdist+" bp)");
        if (lastrun) {
            int report=((Integer)task.getParameter("Report")).intValue();
           String sortby=((String)task.getParameter("Sort results by"));
            DataGroup requiredMotifs=(DataGroup)task.getParameter("Required motifs"); 
            MotifPartition motifclusters=(MotifPartition)task.getParameter("Cluster motifs");             
            if (requiredMotifs!=null) { // filter out tuples that do not contain the required motifs
                filterTuplesNotContainingRequiredMotifs(totalcounts,requiredMotifs,motifclusters);
                engine.logMessage(totalcounts.size()+" tuples remain after filtering those that do not contain required motifs");
            } 
            if (totalcounts.size()>report) totalcounts=keepTopX(totalcounts, report, sortby); // filter the rest
        }
        // Do the search again and mark those regions that are still admissible
        for (int s=0;s<sequences.size();s++) {
            RegionSequenceData seq=(RegionSequenceData)dataset.getSequenceByName(sequences.get(s).getName());
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing operation: moduleDiscovery : filtering tuples of size "+modulesize+" ("+(int)(s+1)+"/"+sequences.size()+")");
            task.setProgress(s, sequences.size());
            Thread.yield();
            ArrayList<Region> included=new ArrayList<Region>();
            ArrayList<Region> candidateRegions=seq.getOriginalRegions();
            for (int i=0;i<=candidateRegions.size()-modulesize;i++) {
               searchTuplesRecursively(candidateRegions, i, modulesize, maxdist, included, interactionsOnly, forceHeterogeneous, totalcounts, MARK_MOTIF, engine, seq.getName()); // this will fill up 'seqcounts'
            }
            
            // now remove unmarked regions
            Iterator<Region> filterIterator=candidateRegions.iterator();
            int removeCounter=0;
            while (filterIterator.hasNext()) {
                Region r=filterIterator.next();
                 if (r.getProperty("FMTmarked")==null) {filterIterator.remove();removeCounter++;}
            }
            removeMarks(candidateRegions);
        }
        return totalcounts;
    }

    /** Find the remaining tuples and creates modules based on these tuples. Module regions are added to the moduleTrack */
    private void findModuleSites(RegionDataset dataset, ArrayList<Sequence> sequences, int modulesize, int maxdist, boolean interactionsOnly, boolean forceHeterogeneous, OperationTask task) throws Exception {
        for (int s=0;s<sequences.size();s++) {
            RegionSequenceData seq=(RegionSequenceData)dataset.getSequenceByName(sequences.get(s).getName());          
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing operation: moduleDiscovery : generating modules "+modulesize+" ("+(int)(s+1)+"/"+sequences.size()+")");
            task.setProgress(s, sequences.size()); 
            Thread.yield();
            // find admissible n-tuples
            HashMap<MotifTupleKey,int[]> seqcounts=new HashMap<MotifTupleKey,int[]>();
            ArrayList<Region> included=new ArrayList<Region>();
            ArrayList<Region> candidateRegions=seq.getOriginalRegions();
            for (int i=0;i<=candidateRegions.size()-modulesize;i++) {
               searchTuplesRecursively(candidateRegions, i, modulesize, maxdist, included, interactionsOnly,forceHeterogeneous, seqcounts, ADD_MODULE, engine,seq.getName()); // this will fill up 'seqcounts'
            }
        }
    }

    private void filterTuplesNotContainingRequiredMotifs(HashMap<MotifTupleKey,int[]> tuples, DataGroup requiredMotifs, MotifPartition clustered) {
        Iterator<MotifTupleKey> iterator=tuples.keySet().iterator();
        if (requiredMotifs instanceof MotifCollection) {
            ArrayList<String> required=((MotifCollection)requiredMotifs).getAllMotifNames();            
            while (iterator.hasNext()) {
                MotifTupleKey tuple=iterator.next();
                if (clustered!=null) {
                   if (!tuple.containsAtLeastOne(required,clustered)) iterator.remove(); 
                } else {
                   if (!tuple.containsAtLeastOne(required)) iterator.remove();
                }
            }           
        } else { // required motifs is a partition
            MotifPartition requiredPartition=(MotifPartition)requiredMotifs;
            ArrayList<ArrayList<String>> clusterMotifNames=new ArrayList<ArrayList<String>>();            
            for (String clusterName:requiredPartition.getClusterNames()) {
               clusterMotifNames.add(requiredPartition.getAllMembersInCluster(clusterName)); 
            }
            while (iterator.hasNext()) {
                MotifTupleKey tuple=iterator.next();
                for (ArrayList<String> required:clusterMotifNames) {
                    if (clustered!=null) {
                       if (!tuple.containsAtLeastOne(required,clustered)) {iterator.remove();break;}
                    } else {
                       if (!tuple.containsAtLeastOne(required)) {iterator.remove();break;}
                    }                        
                }                    
            } // end for each tuple                       
        } // end MotifPartition of required motifs
    }

    private void removeMarks(ArrayList<Region> regions) {
        for (Region r: regions) {
            r.setProperty("FMTmarked", null);
        }
    }

    /**
     * This method is used to find modules on the direct strand
     *
     * @param regionslist The list of candidate regions from the source sequence
     * @param regionIndex The index of the region currently searched in regionslist
     * @param size The target size of a tuple
     * @param maxdist The maximum allowed distance between pairs of motifs
     * @param included A list containing the regions included in the tuple so far
     * @param interactionsOnly A flag specifying whether a new motif should only be included if the motif interacts with the previous motif in the included list
     * @param forceHeterogeneous A flag specifying whether a new motif must be different from all motifs in the included list
     * @param counts A hash containing occurrence counts for tuples (of correct size) in this sequence
     * @param stage ...
     * @return a flag indicating status. 0=OK (region added), 1=distance too short (proceed with next region), 2=Region not applicable (e.g. not interacting with previous), 3=distance too far from previous region
     */
    private int searchTuplesRecursively(ArrayList<Region> sequenceregionslist, int regionIndex, int size, int maxdist, ArrayList<Region> included, boolean interactionsOnly, boolean forceHeterogeneous, HashMap<MotifTupleKey,int[]>counts, int stage, MotifLabEngine engine, String sequenceName) throws ExecutionError {
        int currentsize=included.size();
        Region nextRegion=sequenceregionslist.get(regionIndex); // the next candidate to potentially add to the list
        if (currentsize>0) { //
             Region previousRegion=included.get(included.size()-1);
             int span=nextRegion.getRelativeEnd()-included.get(0).getRelativeStart()+1; // current span
             int distance=nextRegion.getRelativeStart()-(previousRegion.getRelativeEnd()+1);
             if (distance<0) return 1; // do not allow overlapping motifs
             if (span>maxdist) return 3; // distance was too long and regions coming after will also be too far away
             if (interactionsOnly && !utilities.interacts(previousRegion,nextRegion)) return 2; // region violates additional constraint
             if (forceHeterogeneous && utilities.isSimilarToList(included, nextRegion)) return 2;
         }
         // 'nextRegion' is an applicable candidate for motif with index 'singlemotifindex'. Add it to the list
         included.add(nextRegion);
         currentsize=included.size();
         if (currentsize<size) { // list is not complete yet. Add this motif and fill the remaining slots recursively (note that "currentsize+1" is now actually the current size since "cu
             // check next region as next candidate. if that does not hold check the ones after that until end.
             int result=0;
             for (int j=regionIndex+1;j<=sequenceregionslist.size()-(size-included.size());j++) {
                result=searchTuplesRecursively(sequenceregionslist, j, size, maxdist, included, interactionsOnly, forceHeterogeneous, counts, stage, engine, sequenceName); // this will search for applicable combinations and add them
                if (result==3 || result==6) break; // the distance was too long. No further candidates can be build from this base. Backtrack!
            }
            included.remove(included.size()-1); // pop the last motif before backtracking
            return 0; // signal "fresh start"
         } else { // all motifs are found and applicable. create module region and count, mark or add it!
               MotifTupleKey key=new MotifTupleKey(included, true); // create a MotifTupleKey for this tuple
               //engine.logMessage("Trying tuple: "+key.toString());
               if (stage==MARK_MOTIF) { // mark the regions in this tuple as admissible
                 if (counts.containsKey(key)) {
                    for (Region r:included) {
                       r.setProperty("FMTmarked", Boolean.TRUE);
                    }
                 } 
               } else if (stage==COUNT_MOTIF) { // count this tuple
                   if (!counts.containsKey(key)) {
                       counts.put(key, new int[]{1});
                   } else {
                       int[] tuplevalue=counts.get(key);
                       tuplevalue[0]++;
                   }
               } else if (stage==ADD_MODULE) { // add module for this tuple
                   ModuleCRM cisRegModule=modules.get(key);
                   if (cisRegModule!=null) { // is this tuple corresponding to an applicable module?
                       // create region for module and add it to the track
                       // note that if the module contains duplicate motifs they will have to be renamed!
                       int[] mapping=key.getMotifIndexForRegions(included);
                       double score=0;
                       double maxmotifscore=0;
                       int start=included.get(0).getRelativeStart(); // start of module
                       int end=included.get(included.size()-1).getRelativeEnd(); // end of module
                       RegionSequenceData sequence=(RegionSequenceData)moduleTrack.getSequenceByName(sequenceName);
                       Region moduleRegion=new Region(sequence, start, end, cisRegModule.getName(), score, ModuleCRM.INDETERMINED);
                       // add motif sites to the module
                       for (int i=0;i<included.size();i++) {
                           Region singlemotif=included.get(i).clone();
                           score+=singlemotif.getScore();
                           if (singlemotif.getScore()>maxmotifscore) maxmotifscore=singlemotif.getScore();
                           singlemotif.setParent(sequence);
                           String newsinglemotifname=cisRegModule.getSingleMotifName(mapping[i]);
                           moduleRegion.setProperty(newsinglemotifname, singlemotif);
                       }
                       //moduleRegion.setScore(score);
                       moduleRegion.setScore(maxmotifscore);
                       sequence.addRegion(moduleRegion);
                   }
               } 
               included.remove(included.size()-1); // pop the last motif and try to find others
               //if (regionIndex==sequenceregionslist.size()-1) return 6; // end of sequence
               return 0;
         }
    }

    /**
     * Keeps only the 'report' best scoring tuples and discards the rest
     * 'Best scoring' means either having the largest (minimum) IC-content or
     * occurring in more sequences (or more in total)
     * @param counts
     * @param report
     * @return
     */
    private HashMap<MotifTupleKey,int[]> keepTopX(HashMap<MotifTupleKey,int[]> counts, int report, String sortResultsBy) {
        HashMap<MotifTupleKey,int[]> keep=new HashMap<MotifTupleKey,int[]>();
        ArrayList<MotifTupleKey> sortedlist=new ArrayList<MotifTupleKey>(counts.keySet());
        Collections.sort(sortedlist, new SortOrderComparator(counts,(motifIC!=null)));
        for (int i=0;i<report;i++) keep.put(sortedlist.get(i), counts.get(sortedlist.get(i)));
        return keep;
    }

    /** Returns a canonical ModuleCRM corresponding to the Motif tuple
  If the tuple contains duplicate motifs these will be renamed
     */
    @SuppressWarnings("unchecked")
    private ModuleCRM createModuleForTuple(MotifTupleKey tuple, int maxlength) throws ExecutionError {
        if (modules.containsKey(tuple)) return modules.get(tuple); // this one has already been created and stored
        else {
            int index=modules.size()+1;
            ModuleCRM newModule=new ModuleCRM("M"+index);
            String[] names=tuple.getMotifNames();
            HashMap<String,String> motifnamemap=null; // maps motifmodule names to motif names
            if (tuple.hasDuplicateMotifs()) { // rename similar motifs
                Object[] tmp=renameDuplicateMotifs(names);
                names=(String[])tmp[0];
                motifnamemap=(HashMap<String,String>)tmp[1];                                      
            } // end duplicate motifs
            for (int i=0;i<names.length;i++) {
                String modulemotifname=names[i];
                String motifname=(motifnamemap==null)?modulemotifname:motifnamemap.get(modulemotifname);
                if (utilities.baseModulesOnMotifClusters()) { // ModuleMotifs based on motif clusters
                    ArrayList<String> motifnames=utilities.getMotifNamesFromCluster(motifname);
                    newModule.addModuleMotif(modulemotifname, motifnames, ModuleCRM.INDETERMINED);   
                } else { // ModuleMotifs based on single motifs
                    if (!engine.dataExists(motifname, Motif.class)) throw new ExecutionError("'"+motifname+"' is not a Motif");
                    Motif motif=(Motif)engine.getDataItem(motifname);
                    newModule.addModuleMotif(modulemotifname, motif, ModuleCRM.INDETERMINED);                    
                }
            }
            newModule.setMaxLength(maxlength);
            modules.put(tuple, newModule);
            return newModule;
        }
    }
    
    
    private Object[] renameDuplicateMotifs(String[] names) {
            HashMap<String,String> motifnamemap=new HashMap<String,String>(names.length);
            HashMap<String,Integer> occurrences=new HashMap<String, Integer>();
            for (String mname:names) {
                if (occurrences.containsKey(mname)) occurrences.put(mname, occurrences.get(mname)+1);
                else occurrences.put(mname, 1);
            }
            HashMap<String,Integer> usecount=new HashMap<String, Integer>();
            String[] newnames=new String[names.length];
            for (int i=0;i<names.length;i++) {
                if (occurrences.get(names[i])==1) {
                    newnames[i]=names[i];
                } else {
                    int used=0;
                    if (usecount.containsKey(names[i])) used=usecount.get(names[i]);
                    used++;
                    newnames[i]=names[i]+"_"+used;
                    usecount.put(names[i],used);
                }
                motifnamemap.put(newnames[i],names[i]);
            }
            return new Object[]{newnames,motifnamemap};        
    }
    
    @SuppressWarnings("unchecked")
    private double getModuleICfromTuple(MotifTupleKey tuple) {
        double totalIC=0;
        String[] names=tuple.getMotifNames();
        for (int i=0;i<names.length;i++) {
            String modulemotifname=names[i];
            if (utilities.baseModulesOnMotifClusters()) { // ModuleMotifs based on motif clusters
                ArrayList<String> motifnames=utilities.getMotifNamesFromCluster(modulemotifname);
                double lowIC=Double.MAX_VALUE;
                for (String motifname:motifnames) {
                    double IC=motifIC.get(motifname);  
                    if (IC<lowIC) lowIC=IC;
                }
                totalIC+=lowIC;
            } else { // ModuleMotifs based on single motifs
                totalIC+=motifIC.get(modulemotifname);                   
            }
        }   
        return totalIC;
    }    
    
    private HashMap<String,Double> getICforAllMotifs(MotifLabEngine engine) {
        HashMap<String,Double> IC=new HashMap<String, Double>();
        for (Data data:engine.getAllDataItemsOfType(Motif.class)) {
            IC.put(data.getName(),((Motif)data).getICcontent());
        }
        return IC;
    } 

    // this is used sometimes for debugging
    private String outputRegionList(ArrayList<Region> list) {
        if (list.isEmpty()) return "";
        StringBuilder builder=new StringBuilder("("+list.get(0).getRelativeStart()+")"+list.get(0).getType());
        for (int i=1;i<list.size();i++) {
            builder.append(",");
            builder.append(list.get(i).getType());
            builder.append("(");
            builder.append(list.get(i).getRelativeStart());
            builder.append(")");           
        }
        return builder.toString();
    }
    
    /** MotifTupleKey is a short representation of a motif tuple that can be easily compared to other tuples
     *  to see if two different tuples represents the same set of motifs. The motifs can be ordered or unordered
     *  (if they are unordered they are sorted canonically)
     */
    private class MotifTupleKey implements Serializable {
        private short[] tuplekey;

        /** Creates a new MotifTupleKey from a list of motifnames */
        public MotifTupleKey(String[] motifnames, boolean sort) {
            tuplekey=new short[motifnames.length];
            for (int i=0;i<motifnames.length;i++) {
                tuplekey[i]=motifNumber.get(motifnames[i]);
            }
            if (sort) {
                if (tuplekey.length==2) {
                   if (tuplekey[1]<tuplekey[0]) {short a=tuplekey[1];tuplekey[1]=tuplekey[0];tuplekey[0]=a;} // just swap
                } else Arrays.sort(tuplekey);
            }
        }

        public MotifTupleKey(ArrayList<Region> list, boolean sort) {
            tuplekey=new short[list.size()];
            for (int i=0;i<tuplekey.length;i++) {
                tuplekey[i]=motifNumber.get(utilities.getRegionType(list.get(i)));
            }
            if (sort) {
                if (tuplekey.length==2) {
                   if (tuplekey[1]<tuplekey[0]) {short a=tuplekey[1];tuplekey[1]=tuplekey[0];tuplekey[0]=a;}
                } else Arrays.sort(tuplekey);
            }
        }

        public boolean contains(String motifname) {
            Short num=motifNumber.get(motifname);
            for (short i:tuplekey) if (num!=null && i==num) return true;
            return false;
        }
        public boolean contains(String[] motifnames) {
            for (String m:motifnames) if (!contains(m)) return false;
            return true;
        }        
        public boolean containsInOrder(String[] motifnames) {
            if (motifnames.length!=tuplekey.length) return false;
            for (int i=0;i<motifnames.length;i++) {
               Short num=motifNumber.get(motifnames[i]); 
               if (num==null || tuplekey[i]!=num) return false;
            }
            return true;
        }          

        public boolean containsAtLeastOne(ArrayList<String> motifnames) {
            for (int i=0;i<tuplekey.length;i++) {
                String name=motifNumberToName.get(tuplekey[i]);
                if (motifnames.contains(name)) return true;
            }
            return false;
        }
        public boolean containsAtLeastOne(ArrayList<String> motifnames, MotifPartition clusters) {
            for (int i=0;i<tuplekey.length;i++) {
                String clustername=motifNumberToName.get(tuplekey[i]);
                ArrayList<String> motifsInCluster=clusters.getAllMembersInCluster(clustername);
                if (listsOverlaps(motifnames,motifsInCluster)) return true;
            }
            return false;
        }        
        
        private boolean listsOverlaps(ArrayList<String> list1, ArrayList<String> list2) {
            if (list1==null || list2==null) return false;
            ArrayList<String> first=(list1.size()<list2.size())?list1:list2;
            ArrayList<String> second=(list1.size()<list2.size())?list2:list1;
            for (String s:first) {
                if (second.contains(s)) return true;
            }
            return false;
            
        }
        
        /** Returns TRUE if the MotifTupleKey contains multiple instances of the same motif */
        public boolean hasDuplicateMotifs() {
            for (int i=0;i<tuplekey.length-1;i++) {
                for (int j=i+1;j<tuplekey.length;j++) {
                    if (tuplekey[i]==tuplekey[j]) return true;
                }
            }
            return false;
        }

        /** This method will return a mapping from the regions in the list to the corresponding
         *  number for the constituent motif in the module. Note that if a module has multiple motifs
         *  of the same type, the mapping can be (somewhat) arbitrary for these motifs.
         */
        public int[] getMotifIndexForRegions(ArrayList<Region> list) {
            short[] tuplekeySorted=new short[list.size()];
            short[] tuplekeyUnsorted=new short[list.size()];
            for (int i=0;i<tuplekeyUnsorted.length;i++) {
                tuplekeyUnsorted[i]=motifNumber.get(utilities.getRegionType(list.get(i)));
                tuplekeySorted[i]=motifNumber.get(utilities.getRegionType(list.get(i)));
            }
            // sort keys in tuplekeySorted
            if (tuplekey.length==2) {
               if (tuplekey[1]<tuplekey[0]) {short a=tuplekey[1];tuplekey[1]=tuplekey[0];tuplekey[0]=a;}
            } else Arrays.sort(tuplekeySorted);
            
            int[] result=new int[list.size()];
            for (int i=0;i<tuplekeyUnsorted.length;i++) {
                int target=tuplekeyUnsorted[i];
                for (int j=0;j<tuplekeySorted.length;j++) {
                    if (tuplekeySorted[j]==target) {result[i]=j;tuplekeySorted[j]=-1;break;}
                }
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o==null || !(o instanceof MotifTupleKey) || ( ((MotifTupleKey)o).tuplekey.length!=tuplekey.length) )  return false;
            short[] other=((MotifTupleKey)o).tuplekey;
            for (int i=0;i<tuplekey.length;i++) if (tuplekey[i]!=other[i]) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Arrays.hashCode(this.tuplekey);
            return hash;
        }

        @Override
        public String toString() {
            String result=motifNumberToName.get(tuplekey[0]);
            for (int i=1;i<tuplekey.length;i++) {
                result+=","+motifNumberToName.get(tuplekey[i]);
            }
            return result;        
        }

        public String[] getMotifNames() {
            String[] names=new String[tuplekey.length];
            for (int i=0;i<tuplekey.length;i++) names[i]=motifNumberToName.get(tuplekey[i]);
            return names;
        }

    }


    private void debug(String header,ArrayList<Region>list) {
        StringBuilder builder=new StringBuilder("  "+header);
        for (Region region:list) {
            builder.append(region.getType());
            builder.append("[");
            builder.append(region.getRelativeStart());
            builder.append("],");
        }
        String string=builder.toString();
        engine.logMessage(string.substring(0, string.length()-1));

    }

    private class SortOrderComparator implements Comparator<MotifTupleKey> {
            private HashMap<MotifTupleKey,int[]> counts;
            private HashMap<MotifTupleKey,Double> icContent=null;

            public SortOrderComparator(HashMap<MotifTupleKey,int[]> counts, boolean byIC) {
               this.counts=counts;
               if (byIC) {
                   icContent=new HashMap<MotifTupleKey, Double>();
                   for (MotifTupleKey key:counts.keySet()) {
                       icContent.put(key,getModuleICfromTuple(key));
                   }
               }
            }
            @Override
            public int compare(MotifTupleKey tuple1, MotifTupleKey tuple2) { //
                if (icContent!=null) {
                    double ic1=icContent.get(tuple1);
                    double ic2=icContent.get(tuple2);
                    if (ic1>ic2) return -1;
                    else if (ic2>ic1) return 1;
                    else { // same IC? sort by support
                        int[] counts1=counts.get(tuple1);
                        int[] counts2=counts.get(tuple2);
                        if (counts1[1]>counts2[1]) return -1;
                        else if (counts2[1]>counts1[1]) return 1;
                        else {
                            if (counts1[0]>counts2[0]) return -1;
                            else if (counts2[0]>counts1[0]) return 1;
                            else return 0;
                        }
                    }                    
                } else { // sort by support
                    int[] counts1=counts.get(tuple1);
                    int[] counts2=counts.get(tuple2);
                    if (counts1[1]>counts2[1]) return -1;
                    else if (counts2[1]>counts1[1]) return 1;
                    else {
                        if (counts1[0]>counts2[0]) return -1;
                        else if (counts2[0]>counts1[0]) return 1;
                        else return 0;
                    }
                }
            }
    }
    
    /** A utility class to redirect/translate certain calls when motif clustering is used */
    private class Utilities {
        HashSet<String> motifs=null; // names of motifs or clusters that are eligible
        MotifPartition partition=null;
        boolean forceheterogeneous=false;
        boolean interacting=false;
        HashSet<String> interactions=null; // each key is i_j where i and j are motifNumbers and i<=j
        HashSet<String> similar=null; // each key is i_j where i and j are motifNumbers and i<=j
        
        public Utilities() {}
        
        public void setMotifsToUse(DataGroup usemotifs) {
            motifs=new HashSet<String>();
            if (usemotifs instanceof MotifPartition) {
                this.partition=(MotifPartition)usemotifs;
                motifs.addAll(partition.getClusterNames());
            } else { // usemotifs = Motif Collection
                motifs.addAll(((MotifCollection)usemotifs).getAllMotifNames());
            }
        }   
         
        public void setInteractingAndForceHeterogeneous(boolean interacting, boolean heterogeneous) {
           forceheterogeneous=heterogeneous;
           this.interacting=interacting;
           if (interacting) {
               interactions=getInteractions();
           } else interactions=null;
           if (heterogeneous) {
               similar=getSimilarPairs();
           } else similar=null;
           // debug();
        }
        
        public boolean baseModulesOnMotifClusters() {
            return (partition!=null);
        }
        
        private ArrayList<String> getMotifNamesFromCluster(String clustername) {
            if (partition==null) return null;
            else return partition.getAllMotifNamesInCluster(clustername);
        }
        
        public String getRegionType(Region region) {
            String type=region.getType();
            if (partition!=null) {
                String motifclustername=partition.getClusterForMotif(type);
                if (motifclustername!=null) return motifclustername;
            }
            return type;
        }
        
        public String getMotifType(String motifname) {
            if (partition!=null) {
                String motifclustername=partition.getClusterForMotif(motifname);
                if (motifclustername!=null) return motifclustername;
            }
            return motifname;           
        }
        
        public boolean interacts(Region region1, Region region2) {
            String name1=getRegionType(region1);
            String name2=getRegionType(region2);
            Short num1=motifNumber.get(name1);
            Short num2=motifNumber.get(name2);
            if (num1==null || num2==null) return false;
            String key=(num1<num2)?(num1+"_"+num2):(num2+"_"+num1);
            return interactions.contains(key);
        }

        /** Returns true if any of the regions in the given list has a type which is identical
         *  or similar to (i.e. an alternative to) the type of the target region
         */
        public boolean isSimilarToList(ArrayList<Region> list, Region targetRegion) {
            String targetType=getRegionType(targetRegion);
            Short targetNumber=motifNumber.get(targetType); 
            if (targetNumber==null) return false;
            for (Region region:list) { // return true if any of these are similar to the target
                String regionType=getRegionType(region);
                if (regionType.equals(targetType)) return true; // this is true if they are both part of the same equivalence cluster or they are the exact same motif
                else if (partition==null) { // compare motif <-> motif
                    Short regionNumber=motifNumber.get(regionType);
                    if (regionNumber==null) continue;
                    String key=(targetNumber<regionNumber)?(targetNumber+"_"+regionNumber):(regionNumber+"_"+targetNumber);
                    if (similar.contains(key)) return true;                   
                }
            }
            return false;
        }
 
        /** Returns a set of interacting motif pairs (or cluster pairs) 
         *  in the format "i_j" where i and j are motif numbers in the 'motifNumber' LUT
         *  (and i is smaller than or equal to j)
         */
        private HashSet<String> getInteractions() {
            if (partition!=null) { // use cluster-cluster interactions
                HashMap<String,HashSet<String>> clusters=partition.getClusters(); 
                HashSet<String> motifnamesfilter=new HashSet<String>(partition.getAllAssignedMembers()); 
                ArrayList<String> clusterNames=new ArrayList<String>(clusters.size()); // this is just to get an ordered and indexable list of the clusters 
                clusterNames.addAll(clusters.keySet()); 
                HashMap<String,HashSet<String>> interactionClusters=new HashMap<String,HashSet<String>>(); 
                for (String clusterName:clusterNames) {
                   interactionClusters.put(clusterName, getInteractionsForCluster(clusters.get(clusterName),motifnamesfilter,engine)); 
                }
                HashSet<String> interactionsset=new HashSet<String>(); // this will store interactions as strings of the format "X_Y" where X and Y are cluster indices (integers) and X<=Y
                int skip=(forceheterogeneous)?1:0; // set to 1 to exclude self-interactions
                int numclusters=clusterNames.size();
                for (int i=0;i<numclusters-1;i++) {
                   for (int j=i+skip;j<numclusters;j++) { // go from j=i to include self-interactions or j=i+1 to exclude self-interactions
                       boolean hasInteraction=false;
                       String clustername1=clusterNames.get(i);
                       String clustername2=clusterNames.get(j);              
                       HashSet<String> cluster1=clusters.get(clustername1);
                       HashSet<String> cluster2_interactions=interactionClusters.get(clustername2);
                       if (clustersOverlap(cluster1,cluster2_interactions)) hasInteraction=true;
                       else { // try the other way around just in case (this will be redundant if all interaction links are bidirectional, but that might not be the case
                          HashSet<String> cluster2=clusters.get(clustername2);
                          HashSet<String> cluster1_interactions=interactionClusters.get(clustername1);
                          hasInteraction=clustersOverlap(cluster1_interactions,cluster2);                  
                       }
                       if (hasInteraction) {
                           Short clusterIndex1=motifNumber.get(clustername1);
                           Short clusterIndex2=motifNumber.get(clustername2);
                           if (clusterIndex1==null || clusterIndex2==null) continue;
                           if (clusterIndex1<clusterIndex2) interactionsset.add(clusterIndex1+"_"+clusterIndex2);
                           else interactionsset.add(clusterIndex2+"_"+clusterIndex1);
                       }
                   }           
                }
                return interactionsset;
            } else { // partition==null. Use motif-motif interactions
                HashSet<String> interactionsset=new HashSet<String>();
                for (String motifname:motifs) {
                    Data motif=engine.getDataItem(motifname);
                    if (motif instanceof Motif) {
                        Short motif1=motifNumber.get(motifname);
                        for (String partner:((Motif)motif).getInteractionPartnerNames()) {
                           Short motif2=motifNumber.get(partner);
                           if (motif2==null) continue; // annotated partner not among currently considered motifs
                           if (motif1<motif2) interactionsset.add(motif1+"_"+motif2);
                           else interactionsset.add(motif2+"_"+motif1);                           
                        }
                    }
                }                
                return interactionsset;
            }
        }
        
        private  HashSet<String> getInteractionsForCluster(HashSet<String> motifnames, HashSet<String> motifnamesfilter, MotifLabEngine engine) {
            HashSet<String> interactingmotifs=new HashSet<String>();
            for (String motifname:motifnames) {
                Data dataitem=engine.getDataItem(motifname);
                if (dataitem instanceof Motif) {
                    interactingmotifs.addAll(((Motif)dataitem).getInteractionPartnerNames());
                }
            }
            if (motifnamesfilter!=null) filterMotifNames(interactingmotifs,motifnamesfilter);
            return interactingmotifs;
        }

        /** Removes all Strings from the target Set which is not in the filter
         *  The method alters the target parameter
         */
        private void filterMotifNames(HashSet<String> target, HashSet<String> filter) {
            Iterator<String> iterator=target.iterator();
            while (iterator.hasNext()) {
                String value=iterator.next();
                if (!filter.contains(value)) iterator.remove();
            }
        }

        /** This method returns true if any of the strings in cluster1 is also in cluster2 */
        private boolean clustersOverlap(HashSet<String> cluster1,HashSet<String> cluster2) {
            for (String string:cluster1) {
                if (cluster2.contains(string)) return true;
            }
            return false;
        } 
        
        private HashSet<String> getSimilarPairs() { // this is only used for simple motif <-> motif pairs (not clusters)
            HashSet<String> similarpairs=new HashSet<String>();
            for (String motifname:motifs) {
                Data motif=engine.getDataItem(motifname);
                if (motif instanceof Motif) {
                    Short motifnumber=motifNumber.get(motifname);
                    if (motifnumber==null) continue;
                    for (String alternative:((Motif)motif).getKnownDuplicatesNames()) {
                        Short altNumber=motifNumber.get(alternative);
                        if (altNumber!=null) {
                            if (motifnumber<altNumber) similarpairs.add(motifnumber+"_"+altNumber);
                            else similarpairs.add(altNumber+"_"+motifnumber);
                        }
                    }
                }
            }
            return similarpairs;
        }
        
        
        private void debug() {
            System.err.println("Recorded interactions: "+((interactions==null)?"null":interactions.size()));            
            if (interactions!=null) {
                for (String key:interactions) {
                    String[] pair=key.split("_");
                    try {
                        short first=Short.parseShort(pair[0]);
                        short second=Short.parseShort(pair[1]);
                        String name1=motifNumberToName.get(first);
                        String name2=motifNumberToName.get(second);
                        System.err.println("[I] "+key+" => "+name1+" <-> "+name2);
                    } catch (Exception e) {}
                }
            }
            System.err.println("Recorded similarity pairs: "+((similar==null)?"null":similar.size()));            
            if (similar!=null) {
                for (String key:similar) {
                    String[] pair=key.split("_");
                    try {
                        short first=Short.parseShort(pair[0]);
                        short second=Short.parseShort(pair[1]);
                        String name1=motifNumberToName.get(first);
                        String name2=motifNumberToName.get(second);
                        System.err.println("[S] "+key+" => "+name1+" <-> "+name2);
                    } catch (Exception e) {}
                }
            }            
        }
    
    } // end class Utilities

}
