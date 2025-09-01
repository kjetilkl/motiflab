/*
 
 
 */

package org.motiflab.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class SimpleScanner extends MotifScanning {
    private static final int ABSOLUTE=0;
    private static final int RELATIVE=1;
    private static final int MATCH=2;

    private static final int reportSpanLength=50000;

    public SimpleScanner() {
        this.name="SimpleScanner";
        this.programclass="MotifScanning";
        this.serviceType="bundled";

        addSourceParameter("Sequence", DNASequenceDataset.class, null, null, "input sequences");
        addParameter("Motif Collection",MotifCollection.class, null,new Class[]{MotifCollection.class},null,true,false);
        addParameter("Background", BackgroundModel.class, null, new Class[]{BackgroundModel.class},"<html>A background model which specifies expected single nucleotide frequencies.<br>Note that SimpleScanner is not able to use higher-order models</html>",false,false);
        addParameter("Threshold type", String.class, "Percentage", new String[]{"Percentage","Absolute","MATCH"},"<html>Specifies whether the threshold value defined below should be interpreted as a relative threshold (% match) or absolute score threshold (log-odds score).<br>Absolute thresholds should preferably be equal to or higher than 0.<br>MATCH is a special relative threshold which uses the same scoring function as the MATCH algorithm.</html>",true,false);
        addParameter("Threshold", Double.class, 95, new Double[]{0.0,100.0},"Only sites that scores above or equal to this threshold value will be returned",true,false);
        addParameter("Score", String.class, "Absolute", new String[]{"Relative","Absolute"},"<html>Specifies whether the scores assigned to the returned sites should be absolute scores (log-odds score)<br>or relative scores (the absolute score divided by the highest achievable score for the motif)</html>",true,false);        
        addParameter("Pseudo", Double.class, 0.01, new Double[]{0.0,1000.0},"A pseudo-count which is added to each entry of the frequency matrix before the columns are renormalized",false,false,true);
        addResultParameter("Result", RegionDataset.class, null, null, "output track");
    }     
    
    @Override
    public void execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String targetDatasetName=task.getTargetDataName();
        Data[] sources=(Data[])task.getParameter(SOURCES);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for motif scanning with SimpleScanner");
        DNASequenceDataset sourceDataset=(DNASequenceDataset)sources[0];
        RegionDataset targetDataset=new RegionDataset(targetDatasetName);
        ArrayList<Data> allsequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data sequence:allsequences) {
             RegionSequenceData regionsequence=new RegionSequenceData((Sequence)sequence);
             targetDataset.addSequence(regionsequence);
        }
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        int size=sequenceCollection.getNumberofSequences();
        BackgroundModel background=(BackgroundModel)task.getParameter("Background");
        String thresholdtypestring=(String)task.getParameter("Threshold type");
        String scoremodestring=(String)task.getParameter("Score");
        double pseudo=0.01;        
        Double pseudocount=(Double)task.getParameter("Pseudo");
        if (pseudocount!=null && pseudocount>=0) pseudo=pseudocount;
        int scoremode=ABSOLUTE;
        int thresholdtype=ABSOLUTE;
             if (thresholdtypestring.equalsIgnoreCase("Absolute")) thresholdtype=ABSOLUTE;
        else if (thresholdtypestring.equalsIgnoreCase("MATCH")) thresholdtype = MATCH;
        else if (thresholdtypestring.equalsIgnoreCase("Percentage") || thresholdtypestring.equalsIgnoreCase("Relative")) thresholdtype=RELATIVE;
             if (scoremodestring.equalsIgnoreCase("Absolute")) scoremode=ABSOLUTE;
        else if (scoremodestring.equalsIgnoreCase("Relative")) scoremode=RELATIVE;
        double[] snf=(background!=null)?background.getSNF():new double[]{0.25f,0.25f,0.25f,0.25f};
        // log-transform the background
        snf[0]=Math.log(snf[0]);
        snf[1]=Math.log(snf[1]);
        snf[2]=Math.log(snf[2]);
        snf[3]=Math.log(snf[3]);
        double threshold=(Double)task.getParameter("Threshold");
        if ((thresholdtype==RELATIVE || thresholdtype==MATCH) && threshold>1.0) { // threshold is given as a percentage number between 0 and 100, so divide by 100 to get a number between 0 and 1
            threshold=threshold/100.0;
            if (threshold>1.0) threshold=1.0;
        }
        if (thresholdtype==ABSOLUTE && threshold>=18) engine.logMessage("Note: the selected absolute score threshold '"+threshold+"' might be a bit high. Suggested range: 0-15");
        HashMap<String,double[][]> matrixTable=new HashMap<String, double[][]>();
        MotifCollection motifs=(MotifCollection)task.getParameter("Motif Collection");
        ArrayList<Motif> allMotifs=motifs.getAllMotifs(engine);
        for (Motif motif:allMotifs) { // search with each motif in turn
            double[][] matrix=motif.getMatrixAsFrequencyMatrixWithPseudo(pseudo);  
            if (thresholdtype != MATCH) logMatrix(matrix); // NB: logMatrix(x) changes the argument!
            matrixTable.put(motif.getName(),matrix);
        }     
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        long totalsize=0;
        for (Sequence sequence:sequences) {totalsize+=sequence.getSize();}
        totalsize=totalsize*motifs.size(); // total number of bp to search by all motifs combined

        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,totalsize);
        long[] counters=new long[]{0,0,sequences.size(),0,totalsize}; // counters[0]=#sequences started, [1]=#sequences completed, [2]=#total number of sequences, [3]=total bp processed so far (by all motifs), [4]=total number of bp to process (combined sequence lengths * number of motifs)       

        ArrayList<ScanSequenceTask<RegionSequenceData>> scantasks=new ArrayList<ScanSequenceTask<RegionSequenceData>>(sequences.size());
        for (Sequence sequence:sequences) scantasks.add(new ScanSequenceTask<RegionSequenceData>(sourceDataset,targetDataset, sequence.getName(), thresholdtype, threshold, scoremode, snf, matrixTable, task, counters));
        List<Future<RegionSequenceData>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(scantasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<RegionSequenceData> future:futures) {
                if (future.isDone() && !future.isCancelled()) {
                    future.get(); // this blocks until completion but the return value is not used
                    countOK++;
                }
            }
        } catch (Exception e) {  
           taskRunner.shutdownNow(); // Note: this will abort all executing tasks (even those that did not cause the exception), but that is OK. 
           if (e instanceof java.util.concurrent.ExecutionException) throw (Exception)e.getCause(); 
           else throw e; 
        }        
        if (countOK!=sequences.size()) {
            throw new ExecutionError("Some mysterious error occurred while scanning");
        }             

        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);   
    }        
    
    
    
    public void searchInSequence(DNASequenceData sourceSequence, RegionSequenceData targetSequence, HashMap<String,double[][]> matrixTable, int scoremode, int thresholdtype, double threshold, double[] snf, OperationTask task, long[] counters) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        if (sequence==null) {
            if (sourceSequence.getRegionEnd()<startOffset) throw new ExecutionError("Something is wrong with the sequence \""+seqname+"\". Genomic end coordinate ("+sourceSequence.getRegionEnd()+") is before start coordinate ("+startOffset+").");
            else throw new ExecutionError("Empty sequence segment: "+seqname+":"+startOffset+"-"+sourceSequence.getRegionEnd());
        }
        int reportEvery=(sequence.length>reportSpanLength)?reportSpanLength:Integer.MAX_VALUE; // for long sequences, update progress every 10Kbp
        
        int counter=0;
        int bpProcessedSinceLastUpdated=0;
        for (String motifname:matrixTable.keySet()) { // search with each motif in turn
            counter++;     
            double[][] matrix=matrixTable.get(motifname);
            double[] minmax=getMinMaxScores(matrix, snf); // these can probably be pre-calculated once for all sequences
            int motifsize=matrix.length;
            
            for (int i=0;i<=sequence.length-motifsize;i++) {
                bpProcessedSinceLastUpdated++;
                double[] scores=matrixMatcher(matrix,sequence,i,snf);
                double relativeScoreDirect=(scores[0]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double comparisonScoreDirect=(thresholdtype==ABSOLUTE)?scores[0]:relativeScoreDirect;
                double relativeScoreReverse=(scores[1]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double comparisonScoreReverse=(thresholdtype==ABSOLUTE)?scores[1]:relativeScoreReverse;
                if (comparisonScoreDirect>=threshold) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,(scoremode==ABSOLUTE)?scores[0]:relativeScoreDirect,Region.DIRECT);
                     if (scoremode==ABSOLUTE) region.setProperty("relative score", relativeScoreDirect); // include other type of score as well
                     else region.setProperty("absolute score", scores[0]); // include other type of score as well
                     targetSequence.addRegionWithoutSorting(region);
                }
                if (comparisonScoreReverse>=threshold) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,(scoremode==ABSOLUTE)?scores[1]:relativeScoreReverse,Region.REVERSE);
                     if (scoremode==ABSOLUTE) region.setProperty("relative score", relativeScoreReverse); // include other type of score as well
                     else region.setProperty("absolute score", scores[1]); // include other type of score as well
                     targetSequence.addRegionWithoutSorting(region);
                }
                if (bpProcessedSinceLastUpdated==reportEvery) {
                    synchronized(counters) { 
                        counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                        bpProcessedSinceLastUpdated=0;
                        task.setProgress(counters[3],counters[4]);
                    }
                    task.checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                                   
                    Thread.yield();
                }
            } // end: for each bp in sequence
            synchronized(counters) { 
                counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                bpProcessedSinceLastUpdated=0;
                task.setProgress(counters[3],counters[4]);
            }         
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                            
            Thread.yield();
        } // end: for each motif          
        targetSequence.updateRegionSortOrder(); // this must be called since regions were added unsorted       
    }
    
    public void searchWithMATCHinSequence(DNASequenceData sourceSequence, RegionSequenceData targetSequence, HashMap<String,double[][]> matrixTable, int scoremode, double threshold, OperationTask task, long[] counters) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        int reportEvery=(sequence.length>reportSpanLength)?reportSpanLength:Integer.MAX_VALUE; // for long sequences, update progress every 10Kbp
        
        int counter=0;          
        int bpProcessedSinceLastUpdated=0;
       
        for (String motifname:matrixTable.keySet()) { // search with each motif in turn
            counter++;
            //String consensus=((Motif)engine.getDataItem(motifname)).getConsensusMotif();            
            double[][] matrix=matrixTable.get(motifname);
            double[] minmax=getMATCHMinMaxScores(matrix);
            double[] IC=getICforColumns(matrix);

            //if (counter%100==0) Thread.yield();
            int motifsize=matrix.length;
      
            for (int i=0;i<=sequence.length-motifsize;i++) {  
                bpProcessedSinceLastUpdated++;
                double[] scores=MATCHmatrixMatcher(matrix,sequence,i,IC);                
                double relativeScoreDirect=(scores[0]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double relativeScoreReverse=(scores[1]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                if (relativeScoreDirect>=threshold) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,(scoremode==ABSOLUTE)?scores[0]:relativeScoreDirect,Region.DIRECT);
                     if (scoremode==ABSOLUTE) region.setProperty("relative score", relativeScoreDirect); // include other type of score as well
                     else region.setProperty("absolute score", scores[0]); // include other type of score as well
                     targetSequence.addRegionWithoutSorting(region);
                }
                if (relativeScoreReverse>=threshold) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,(scoremode==ABSOLUTE)?scores[1]:relativeScoreReverse,Region.REVERSE);
                     if (scoremode==ABSOLUTE) region.setProperty("relative score", relativeScoreReverse); // include other type of score as well
                     else region.setProperty("absolute score", scores[1]); // include other type of score as well
                     targetSequence.addRegionWithoutSorting(region);
                }
                if (bpProcessedSinceLastUpdated==reportEvery) {
                    synchronized(counters) { 
                        counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                        bpProcessedSinceLastUpdated=0;
                        task.setProgress(counters[3],counters[4]);
                    }
                    task.checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                                   
                    Thread.yield();
                }                
            } // end: for each bp in sequence
            synchronized(counters) { 
                counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                bpProcessedSinceLastUpdated=0;
                task.setProgress(counters[3],counters[4]);
            }
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
            Thread.yield();            
        } // end: for each motif          
        targetSequence.updateRegionSortOrder(); // this must be called since regions were added unsorted          
    }    
      
    
    /** Calculates an absolute match score according to the "usual equation" 
     *  The match is calculated for both strands and returned as an array: double[]{matchdirect, matchreverse}
     *  @param The matrix to be matched. This must have been log-transformed
     *  @param sequence The full sequence which is to be searched
     *  @param offset The offset into the sequence where the matrix is to be matched. The comparison is made at [offset, offset+matrix.length-1] in the sequence
     *  @param snf A background distribution that has been log-transformed (single nucleotide frequency)
     *  @return an array with two match scores for direct and reverse strand respectively
     */
    private double[] matrixMatcher(double[][] matrix, char[] sequence, int offset, double[] snf) {
         double directscore=0, reversescore=0;        
         for (int i=0;i<matrix.length;i++) {
             char base=sequence[i+offset];
             double basefreq=0;
             double backgroundFreq=1;
             double basefreqReverse=0;
             double backgroundFreqReverse=1;             
             switch (base) {
                 case 'A': case 'a': backgroundFreq=snf[0];backgroundFreqReverse=snf[3];basefreq=matrix[i][0];basefreqReverse=matrix[matrix.length-(i+1)][3];break;
                 case 'C': case 'c': backgroundFreq=snf[1];backgroundFreqReverse=snf[2];basefreq=matrix[i][1];basefreqReverse=matrix[matrix.length-(i+1)][2];break;
                 case 'G': case 'g': backgroundFreq=snf[2];backgroundFreqReverse=snf[1];basefreq=matrix[i][2];basefreqReverse=matrix[matrix.length-(i+1)][1];break;
                 case 'T': case 't': backgroundFreq=snf[3];backgroundFreqReverse=snf[0];basefreq=matrix[i][3];basefreqReverse=matrix[matrix.length-(i+1)][0];break;
             }
             directscore+=(basefreq-backgroundFreq); // since both basefreq and backgroundFreq has been log-transformed previously, this equates to score+=Math.log(baseFreq/bgFreq)
             reversescore+=(basefreqReverse-backgroundFreqReverse);
         }
         return new double[]{directscore,reversescore};
    }

    private double[] matrixMatcher(double[][] matrix, char[] sequence, int offset, double[] directProb, double[] reverseProb) {
         double directscore=0, reversescore=0;
         for (int i=0;i<matrix.length;i++) {
             char base=sequence[offset+i];
             double basefreq=0;
             double backgroundFreq=directProb[i];
             double basefreqReverse=0;
             double backgroundFreqReverse=reverseProb[i];
             switch (base) {
                 case 'A': case 'a': basefreq=matrix[i][0];basefreqReverse=matrix[matrix.length-(i+1)][3];break;
                 case 'C': case 'c': basefreq=matrix[i][1];basefreqReverse=matrix[matrix.length-(i+1)][2];break;
                 case 'G': case 'g': basefreq=matrix[i][2];basefreqReverse=matrix[matrix.length-(i+1)][1];break;
                 case 'T': case 't': basefreq=matrix[i][3];basefreqReverse=matrix[matrix.length-(i+1)][0];break;
             }
             directscore+=(basefreq-backgroundFreq); // since both basefreq and backgroundFreq has been log-transformed previously, this equates to score+=Math.log(baseFreq/bgFreq)
             reversescore+=(basefreqReverse-backgroundFreqReverse);
         }
         return new double[]{directscore,reversescore};
    }


    /** Calculates a match score according to the equations used by the MATCH algorithm
     *  The match is calculated for both strands and returned as an array: double[]{matchdirect, matchreverse}
     *  @param The matrix to be matched
     *  @param sequence The full sequence which is to be searched
     *  @param offset The offset into the sequence where the matrix is to be matched. The comparison is made at [offset, offset+matrix.length-1] in the sequence     * 
     *  @param precalculated IC content for each column in the matrix 
     *  @return an array with two match scores for direct and reverse strand respectively
     */
    private double[] MATCHmatrixMatcher(double[][] matrix, char[] sequence, int offset, double[] IC) {
         double directscore=0, reversescore=0;  
         for (int i=0;i<matrix.length;i++) {
             char base=sequence[i+offset];
             double basefreqDirect=0;
             double basefreqReverse=0;
             switch (base) {
                 case 'A': case 'a': basefreqDirect=matrix[i][0];basefreqReverse=matrix[matrix.length-(i+1)][3];break;
                 case 'C': case 'c': basefreqDirect=matrix[i][1];basefreqReverse=matrix[matrix.length-(i+1)][2];break;
                 case 'G': case 'g': basefreqDirect=matrix[i][2];basefreqReverse=matrix[matrix.length-(i+1)][1];break;
                 case 'T': case 't': basefreqDirect=matrix[i][3];basefreqReverse=matrix[matrix.length-(i+1)][0];break;
             }
             directscore+=(IC[i]*basefreqDirect);
             reversescore+=(IC[matrix.length-(i+1)]*basefreqReverse);
             //System.err.println("["+(i+1)+"/"+subseq.length+"]  logodds="+logodds+"  score="+score+"   basefreq="+basefreq+"   backgroundFreq="+backgroundFreq);
         }
         return new double[]{directscore,reversescore};
    }

    
    private double[] getICforColumns(double[][] matrix) {
         double[] IC=new double[matrix.length];
         for (int i=0;i<matrix.length;i++) { 
             IC[i]=matrix[i][0]*Math.log(4*matrix[i][0])+matrix[i][1]*Math.log(4*matrix[i][1])+matrix[i][2]*Math.log(4*matrix[i][2])+matrix[i][3]*Math.log(4*matrix[i][3]); 
         }
         return IC;
    }
    
    /**
     * Log-transforms a matrix
     */
    private void logMatrix(double[][] matrix) {
        for (int i=0;i<matrix.length;i++) {
            for (int j=0;j<matrix[i].length;j++) {
                matrix[i][j]=Math.log(matrix[i][j]);
            }
        }
    }
    
   /** Returns an array containing (in order)
    * [0] lowest obtainable score for a motif in a position given the 0-order background frequencies
    * [1] highest obtainable score  for a motif in a position given the 0-order background frequencies
    */
   private double[] getMinMaxScores(double[][] matrix, double[] snf) {
      double[] minmax=new double[2];
      double minBackground=getMin(snf);
      double maxBackground=getMax(snf);
      for (int i=0;i<matrix.length;i++) {
         double maxColumn=getMax(matrix,i);
         double minColumn=getMin(matrix,i);
         minmax[0]+=(minColumn-maxBackground);
         minmax[1]+=(maxColumn-minBackground);
      }
      return minmax;
   }

   
   
    /** Returns an array containing (in order)
    * [0] lowest obtainable score for a motif in a position given the 0-order background frequencies
    * [1] highest obtainable score  for a motif in a position given the 0-order background frequencies
    */
   private double[] getMATCHMinMaxScores(double[][] matrix) {
      double[] minmax=new double[2];
      for (int i=0;i<matrix.length;i++) {
         double maxColumn=getMax(matrix,i);
         double minColumn=getMin(matrix,i);
         double IC=matrix[i][0]*Math.log(4*matrix[i][0])+matrix[i][1]*Math.log(4*matrix[i][1])+matrix[i][2]*Math.log(4*matrix[i][2])+matrix[i][3]*Math.log(4*matrix[i][3]);
         double min=IC*minColumn;
         double max=IC*maxColumn;
         minmax[0]+=min;
         minmax[1]+=max;
      }
      return minmax;
   }

   private double getMax(double[] values) {
       double max=-Double.MAX_VALUE;
       for (int i=0;i<values.length;i++) if (values[i]>max) max=values[i];
       return max;
   }

   private double getMin(double[] values) {
       double min=Double.MAX_VALUE;
       for (int i=0;i<values.length;i++) if (values[i]<min) min=values[i];
       return min;
   }

   /** Returns the maximum value in the given base pos */
   private double getMax(double[][] values, int pos) {
       double max=-Double.MAX_VALUE;
       for (int i=0;i<4;i++) if (values[pos][i]>max) max=values[pos][i];
       return max;
   }

   /** Returns the maximum value in the given column */
   private double getMin(double[][] values, int pos) {
       double min=Double.MAX_VALUE;
       for (int i=0;i<4;i++) if (values[pos][i]<min) min=values[pos][i];
       return min;
   }

   /**
    * Returns (log transformed) probabilities of observing the given base at each position according to the background model 
    * @param sequence
    * @param model
    * @param reverse
    * @return
    */
   private double[] deriveBackgroundProbabilities(char[] sequence, BackgroundModel model, boolean reverse) throws ExecutionError {
      double[] prob=new double[sequence.length];
      if (prob.length==0) return prob;
      int order=(model!=null)?model.getOrder():0;
      if (order==0) { // just use SNFs
            double[] snf=(model!=null)?model.getSNF():new double[]{0.25,0.25,0.25,0.25};
            for (int i=0;i<snf.length;i++) snf[i]=Math.log(snf[i]); // use log-transforms!
            if (reverse) snf=new double[]{snf[3],snf[2],snf[1],snf[0]}; // reverse the snf
            for (int i=0;i<sequence.length;i++) prob[i]=getSNF(sequence[i],snf);
      } else if (!reverse) { // higher order model direct strand
           prob[0]=model.getTransitionProbability("", sequence[0]);
            for (int i=1;i<sequence.length;i++) {
                char base=sequence[i];
                int prefixStart=i-order;
                int prefixEnd=i-1;
                if (prefixStart<0) prefixStart=0;
                String prefix=new String(sequence, prefixStart, prefixEnd-prefixStart+1);
                prob[i]=model.getTransitionProbability(prefix, base);
                prob[i]=Math.log(prob[i]);
            }
      } else { // higher order model reverse strand
            int lastbase=sequence.length-1;
            prob[lastbase]=model.getTransitionProbability("", sequence[lastbase]); // last base
            for (int i=sequence.length-2;i>=0;i--) {
                char base=sequence[i];
                int prefixStart=i+1;
                int prefixEnd=i+order;
                if (prefixEnd>lastbase) prefixEnd=lastbase;
                String prefix=new String(sequence, prefixStart, prefixEnd-prefixStart+1);
                prob[i]=model.getTransitionProbability(MotifLabEngine.reverseSequence(prefix), base);
                prob[i]=Math.log(prob[i]);
            }
      }
      return prob;
   }

    private double getSNF(char base, double[] snf) {
         switch(base) {
          case 'A': case 'a': return snf[0];
          case 'C': case 'c':  return snf[1];
          case 'G': case 'g':  return snf[2];
          case 'T': case 't':  return snf[3];
          default: return 0;
        }
    }
    
    private class ScanSequenceTask<RegionSequenceData> implements Callable<RegionSequenceData> {
        final RegionDataset targetDataset;
        final DNASequenceDataset sourceDataset;
        final long[] counters; // NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;
        final HashMap<String,double[][]> matrixTable;
        final int scoremode;
        final int thresholdtype;
        final double threshold;
        final double[] background;   
        
        public ScanSequenceTask(DNASequenceDataset sourceDataset, RegionDataset targetDataset, String sequencename, int thresholdtype, double threshold, int scoremode, double[] background, HashMap<String,double[][]> matrixTable, OperationTask task, long[] counters) {
           this.counters=counters;
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset;    
           this.thresholdtype=thresholdtype;
           this.matrixTable=matrixTable;
           this.scoremode=scoremode;
           this.threshold=threshold;
           this.background=background;
           this.task=task;     
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public RegionSequenceData call() throws Exception {
            synchronized(counters) {
                counters[0]++; // number of sequences started
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequencename);
            org.motiflab.engine.data.RegionSequenceData targetSequence=(org.motiflab.engine.data.RegionSequenceData)targetDataset.getSequenceByName(sequencename);
            //motiflab.engine.data.RegionSequenceData targetSequence=(motiflab.engine.data.RegionSequenceData)targetDataset.getSequenceByName(sequencename);
            if (thresholdtype == MATCH) searchWithMATCHinSequence(sourceSequence,targetSequence, matrixTable, scoremode, threshold, task, counters);                              
            else searchInSequence(sourceSequence,targetSequence, matrixTable, scoremode, thresholdtype, threshold, background, task, counters);
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                task.setStatusMessage("Executing SimpleScanner:  ("+counters[1]+"/"+counters[2]+")");
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return (RegionSequenceData)targetSequence;
        }   
    }    
  
}