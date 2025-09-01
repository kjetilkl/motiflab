/*
 
 
 */

package org.motiflab.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class ConsensusScanner extends MotifScanning {
 
    private static final int ABSOLUTE=0;
    private static final int RELATIVE=1;
    
    public ConsensusScanner() {
        this.name="ConsensusScanner";
        this.programclass="MotifScanning";
        this.serviceType="bundled";

        addSourceParameter("Sequence", DNASequenceDataset.class, null, null, "input sequences");
        addParameter("Motif Collection",MotifCollection.class, null,new Class[]{MotifCollection.class},null,true,false);
        addParameter("Score", String.class, "Absolute", new String[]{"Relative","Absolute"},"<html>Specifies whether the scores assigned to the returned sites should be absolute scores<br>or relative scores (the absolute score divided by the highest achievable score for the motif)</html>",true,false);
        //addParameter("Threshold type", String.class, "Percentage", new String[]{"Percentage","Absolute"},"<html>Specifies whether the threshold value defined below should be interpreted as a relative threshold (% match) or absolute score threshold.</html>",true,false);
        //addParameter("Threshold", Double.class, 95, new Double[]{0.0,100.0},"Only sites that scores above or equal to this threshold value will be returned",true,false);
        addResultParameter("Result", RegionDataset.class, null, null, "output track");
    }     
    
    @Override
    public void execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String targetDatasetName=task.getTargetDataName();
        Data[] sources=(Data[])task.getParameter(SOURCES);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for motif scanning with ConsensusScanner");
        DNASequenceDataset sourceDataset=(DNASequenceDataset)sources[0];
        RegionDataset targetDataset=new RegionDataset(targetDatasetName);
        ArrayList<Data> allsequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data sequence:allsequences) {
             RegionSequenceData regionsequence=new RegionSequenceData((Sequence)sequence);
             targetDataset.addSequence(regionsequence);
        }
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        int size=sequenceCollection.getNumberofSequences();
        //String thresholdtypestring=(String)task.getParameter("Threshold type");
        String scoremodestring=(String)task.getParameter("Score");
        int scoremode=ABSOLUTE;
        int thresholdtype=RELATIVE;
//             if (thresholdtypestring.equalsIgnoreCase("Absolute")) thresholdtype=ABSOLUTE;
//        else if (thresholdtypestring.equalsIgnoreCase("Percentage") || thresholdtypestring.equalsIgnoreCase("Relative")) thresholdtype=RELATIVE;
             if (scoremodestring.equalsIgnoreCase("Absolute")) scoremode=ABSOLUTE;
        else if (scoremodestring.equalsIgnoreCase("Relative")) scoremode=RELATIVE;
//        double threshold=(Double)task.getParameter("Threshold");
        double threshold=1.0; // exact fit for consensus
//        if (thresholdtype==RELATIVE && threshold>1.0) { // threshold is given as a percentage number between 0 and 100, so divide by 100 to get a number between 0 and 1
//            threshold=threshold/100.0;
//            if (threshold>1.0) threshold=1.0;
//        }
//        if (thresholdtype==ABSOLUTE && threshold>=18) engine.logMessage("Note: the selected absolute score threshold '"+threshold+"' might be a bit high. Suggested range: 0-15");
        HashMap<String,char[]> consensusTable=new HashMap<String, char[]>();
        MotifCollection motifs=(MotifCollection)task.getParameter("Motif Collection");
        ArrayList<Motif> allMotifs=motifs.getAllMotifs(engine);
        for (Motif motif:allMotifs) { // search with each motif in turn
            String consensusmotif=motif.getConsensusMotif().toUpperCase();            
            consensusTable.put(motif.getName(),consensusmotif.toCharArray());
        }     
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        int threads=engine.getConcurrentThreads();
        if (threads<=0) threads=1;
        ExecutorService threadpool=Executors.newFixedThreadPool(threads);
        if (threads==1) { // run all in single thread        
            int i=0;        
            for (Sequence sequence:sequences) { // for each sequence
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                String sequenceName=sequence.getName();
                DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequenceName);
                RegionSequenceData targetSequence=(RegionSequenceData)targetDataset.getSequenceByName(sequenceName);
                searchInSequence(sourceSequence,targetSequence, consensusTable, scoremode, thresholdtype, threshold, task);
                task.setStatusMessage("Executing ConsensusScanner:  ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
                if (i%3==0) Thread.yield();
            } 
        } else { // multiple threads
            int[] counters=new int[]{0,0,sequences.size()}; // counters[0]=#downloads started, [1]=#downloads completed, [2]=#total number of sequences
            ArrayList<ScanSequenceTask<RegionSequenceData>> scantasks=new ArrayList<ScanSequenceTask<RegionSequenceData>>(sequences.size());
            for (Sequence sequence:sequences) scantasks.add(new ScanSequenceTask<RegionSequenceData>(sourceDataset,targetDataset, sequence.getName(), thresholdtype, threshold, scoremode, consensusTable, task, counters));
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
            } catch (Exception e) {  
               threadpool.shutdownNow();
               if (e instanceof java.util.concurrent.ExecutionException) throw (Exception)e.getCause(); 
               else throw e; 
            } 
            if (threadpool!=null) threadpool.shutdownNow();        
            if (countOK!=sequences.size()) {
                throw new ExecutionError("Some mysterious error occurred while scanning");
            }             
        }        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);   
    }        
    
    
    
    public void searchInSequence(DNASequenceData sourceSequence, RegionSequenceData targetSequence, HashMap<String,char[]> consensusTable, int scoremode, int thresholdtype, double threshold, OperationTask task) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        int counter=0;
        for (String motifname:consensusTable.keySet()) { // search with each motif in turn
            counter++;  
            if (counter%50==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
            }
            char[] consensus=consensusTable.get(motifname);
            int motifsize=consensus.length;
            double[] minmax=getMinMaxScores(consensus);            
      
            for (int i=0;i<=sequence.length-motifsize;i++) {                                
                double[] scores=consensusMatcher(consensus,sequence,i);
                //double[] scores=matrixMatcher(matrix,sequence,i,directProb,reverseProb);
                double relativeScoreDirect=(scores[0]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double comparisonScoreDirect=(thresholdtype==ABSOLUTE)?scores[0]:relativeScoreDirect;
                double relativeScoreReverse=(scores[1]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double comparisonScoreReverse=(thresholdtype==ABSOLUTE)?scores[1]:relativeScoreReverse;
                if (comparisonScoreDirect>=threshold) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,(scoremode==ABSOLUTE)?scores[0]:relativeScoreDirect,Region.DIRECT);
                     targetSequence.addRegionWithoutSorting(region);
                }
                if (comparisonScoreReverse>=threshold) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,(scoremode==ABSOLUTE)?scores[1]:relativeScoreReverse,Region.REVERSE);
                     targetSequence.addRegionWithoutSorting(region);
                }
            }
            targetSequence.updateRegionSortOrder(); // this must be called since regions were added unsorted
            Thread.yield();
        }       
    }
    
    /** Returns an array containing (in order)
    * [0] lowest obtainable score for a motif in a position
    * [1] highest obtainable score  for a motif in a position
    */
   private double[] getMinMaxScores(char[] consensusMotif) {       
      double[] minmax=new double[2]; // min-score will always be 0
      for (int i=0;i<consensusMotif.length;i++) {
         switch(consensusMotif[i]) {
             case 'A':case 'C': case 'G': case 'T':minmax[1]+=1;break;
             case 'R':case 'Y': case 'K': case 'M':case 'W':case 'S':minmax[1]+=0.5;break;
             case 'B':case 'D': case 'H': case 'V':minmax[1]+=0.3;break;
         }
      }
      return minmax;
   }
   
    /** Calculates an absolute match score for the consensus the sequence segment
     *  The match is calculated for both strands and returned as an array: double[]{matchdirect, matchreverse}
     *  @param motif The consensus motif to be matched (IUPAC).
     *  @param reversemotif The reverse complement of the consensus motif to be matched (IUPAC).
     *  @param sequence The full sequence which is to be searched
     *  @param offset The offset into the sequence where the motif is to be matched. The comparison is made at [offset, offset+motif.length-1] in the sequence
     *  @return an array with two match scores for direct and reverse strand respectively
     */   
    private double[] consensusMatcher(char[] motif, char[] sequence, int offset) {
         double directscore=0, reversescore=0;        
         for (int i=0;i<motif.length;i++) {
             char base=Character.toUpperCase(sequence[i+offset]);
             char reversebase=base;
             switch (base) {
               case 'A': reversebase='T';break;                
               case 'C': reversebase='G';break;                
               case 'G': reversebase='C';break;                
               case 'T': reversebase='A';break;                
             }
             directscore+=matchConsensusCharToSequence(motif[i],base);
             reversescore+=matchConsensusCharToSequence(motif[motif.length-(i+1)],reversebase);
         }
         return new double[]{directscore,reversescore};
    }  
    
    /** Matches a IUPAC consensus character to a specific base and returns a match score */
    private double  matchConsensusCharToSequence(char consensus, char base) {
        if (base=='A') {
            switch (consensus) {
               case 'A': return 1.0;
               case 'R': case 'M': case 'W': return 0.5;
               case 'D': case 'H': case 'V': return 1.0/3.0;
            }
        } else if (base=='C') {
            switch (consensus) {
               case 'C': return 1.0;
               case 'Y': case 'M': case 'S': return 0.5;
               case 'B': case 'H': case 'V': return 1.0/3.0;
            }            
        } else if (base=='G') {
            switch (consensus) {
               case 'G': return 1.0;
               case 'R': case 'K': case 'S': return 0.5;
               case 'B': case 'D': case 'V': return 1.0/3.0;
            }            
        } else if (base=='T') {
             switch (consensus) {
               case 'T': return 1.0;
               case 'Y': case 'K': case 'W': return 0.5;
               case 'B': case 'D': case 'H': return 1.0/3.0;
            }           
        }
        return 0;
    }
    
    private class ScanSequenceTask<RegionSequenceData> implements Callable<RegionSequenceData> {
        final RegionDataset targetDataset;
        final DNASequenceDataset sourceDataset;
        final int[] counters;
        final String sequencename;
        final OperationTask task;
        final HashMap<String,char[]> consensusTable;
        final int scoremode;
        final int thresholdtype;
        final double threshold;       
        
        public ScanSequenceTask(DNASequenceDataset sourceDataset, RegionDataset targetDataset, String sequencename, int thresholdtype, double threshold, int scoremode, HashMap<String,char[]> consensusTable, OperationTask task, int[] counters) {;
           this.counters=counters;
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset;    
           this.thresholdtype=thresholdtype;
           this.consensusTable=consensusTable;
           this.scoremode=scoremode;
           this.threshold=threshold;
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
            DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequencename);
            org.motiflab.engine.data.RegionSequenceData targetSequence=(org.motiflab.engine.data.RegionSequenceData)targetDataset.getSequenceByName(sequencename);
            //motiflab.engine.data.RegionSequenceData targetSequence=(motiflab.engine.data.RegionSequenceData)targetDataset.getSequenceByName(sequencename);
            searchInSequence(sourceSequence, targetSequence, consensusTable, scoremode, thresholdtype, threshold, task);
            synchronized(counters) {
                counters[1]++; // number of downloads finished
                task.setProgress(counters[1], counters[2]);   
                task.setStatusMessage("Executing ConsensusScanner:  ("+counters[1]+"/"+counters[2]+")");
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException(); 
            return (RegionSequenceData)targetSequence;
        }   
    }     
  
}