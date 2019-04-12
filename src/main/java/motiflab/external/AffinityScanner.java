/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.BackgroundModel;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.Data;
import motiflab.engine.data.Motif;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class AffinityScanner extends MotifScanning {

    public AffinityScanner() {
        this.name="AffinityScanner";
        this.programclass="MotifScanning";
        this.serviceType="bundled";

        addSourceParameter("Sequence", DNASequenceDataset.class, null, null, "input sequences");
        addParameter("Motif Collection",MotifCollection.class, null,new Class[]{MotifCollection.class},null,true,false);
        addParameter("Background", BackgroundModel.class, null, new Class[]{BackgroundModel.class},"<html>A background model which specifies expected single nucleotide frequencies.<br>This is only used when log-likelihoods are used as score.<br>Note that AffinityScanner is not able to use higher-order models</html>",false,false);
        addParameter("Score", String.class, "Raw", new String[]{"Raw","Log-likelihood"},"",false,false);
        addParameter("Use relative scores",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, the match score for each position will be normalized to [0,1]",false,false);
        addParameter("Skip negative scores",Boolean.class, Boolean.FALSE,new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, only positions with positive motif match scores will be included in the sum.",false,false);
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
        String scoreMode=(String)task.getParameter("Score");
        boolean useloglikelihood=(scoreMode!=null && scoreMode.equalsIgnoreCase("Log-likelihood"));
        Boolean useRelativeValues=(Boolean)task.getParameter("Use relative scores");
        if (useRelativeValues==null) useRelativeValues=Boolean.FALSE;  
        Boolean skipNegative=(Boolean)task.getParameter("Skip negative scores");
        if (skipNegative==null) skipNegative=Boolean.FALSE;       
        BackgroundModel background=(BackgroundModel)task.getParameter("Background");
        double[] snf=(background!=null)?background.getSNF():new double[]{0.25f,0.25f,0.25f,0.25f};
        // log-transform the background
        snf[0]=Math.log(snf[0]);
        snf[1]=Math.log(snf[1]);
        snf[2]=Math.log(snf[2]);
        snf[3]=Math.log(snf[3]);
        HashMap<String,double[][]> matrixTable=new HashMap<String, double[][]>();
        MotifCollection motifs=(MotifCollection)task.getParameter("Motif Collection");
        ArrayList<Motif> allMotifs=motifs.getAllMotifs(engine);
        double pseudo=0.01;
        for (Motif motif:allMotifs) { 
            double[][] matrix=motif.getMatrixAsFrequencyMatrixWithPseudo(pseudo);  
            if (useloglikelihood) logMatrix(matrix); // NB: logMatrix(x) changes the argument!
            matrixTable.put(motif.getName(),matrix);
        }     
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        int i=0;        
        for (Sequence sequence:sequences) { // for each sequence
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            String sequenceName=sequence.getName();
            DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequenceName);
            RegionSequenceData targetSequence=(RegionSequenceData)targetDataset.getSequenceByName(sequenceName);
            searchInSequence(sourceSequence,targetSequence, matrixTable, snf, useloglikelihood, skipNegative.booleanValue(), useRelativeValues.booleanValue(), task);
            task.setStatusMessage("Executing AffinityScanner:  ("+(i+1)+"/"+size+")");
            task.setProgress(i+1, size);
            i++;
            Thread.yield();
        }   
        //((NumericDataset)targetDataset).updateMinMax();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);   
    }        
    
    
    
    public void searchInSequence(DNASequenceData sourceSequence, RegionSequenceData targetSequence, HashMap<String,double[][]> matrixTable, double[] snf, boolean useloglikelihood, boolean skipNegative, boolean useRelativeValues, OperationTask task) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        int counter=0;
        for (String motifname:matrixTable.keySet()) { // search with each motif in turn
            counter++;
            //String consensus=((Motif)engine.getDataItem(motifname)).getConsensusMotif();            
            double[][] matrix=matrixTable.get(motifname);
            if (counter%50==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
            }
            //if (counter%100==0) Thread.yield();
            int motifsize=matrix.length;
            double affinityscore=0;
            int bestpos=0;
            int bestOrientation=Region.DIRECT;
            double bestposScore=-Double.MAX_VALUE;
            double[] minmax=null;
            if (useRelativeValues) {
                minmax=(useloglikelihood)?getMinMaxScores(matrix, snf):getMinMaxScores(matrix);
            }
            for (int i=0;i<=sequence.length-motifsize;i++) {                                
                double[] scores=(useloglikelihood)?matrixMatcherLogLikelihood(matrix,sequence,i,snf):matrixMatcherRaw(matrix,sequence,i); // scores on direct and reverse strand
                if (skipNegative && !useRelativeValues) {
                    if (scores[0]<0) scores[0]=0;
                    if (scores[1]<0) scores[1]=0;
                }
                double directscore=(useRelativeValues)?((scores[0]-minmax[0])/(minmax[1]-minmax[0])):scores[0];
                double reversescore=(useRelativeValues)?((scores[1]-minmax[0])/(minmax[1]-minmax[0])):scores[1];               
                affinityscore+=directscore;
                affinityscore+=reversescore;   
                if (directscore>bestposScore) {bestposScore=directscore;bestpos=i;bestOrientation=Region.DIRECT;}
                if (reversescore>bestposScore) {bestposScore=reversescore;bestpos=i;bestOrientation=Region.REVERSE;}          
            }
            // add site at best position
            Region region=new Region(targetSequence,bestpos,bestpos+motifsize-1,motifname,affinityscore,bestOrientation);
            //region.setProperty("consensus motif", consensus);
            targetSequence.addRegionWithoutSorting(region);                      
        }       
        targetSequence.updateRegionSortOrder(); // this must be called since regions were added unsorted
        Thread.yield();        
    }
    
     
    private double[] matrixMatcherLogLikelihood(double[][] matrix, char[] sequence, int offset, double[] snf) {
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
    
    private double[] matrixMatcherRaw(double[][] matrix, char[] sequence, int offset) {
         double directscore=0, reversescore=0;        
         for (int i=0;i<matrix.length;i++) {
             char base=sequence[i+offset];
             double basefreq=0;
             double basefreqReverse=0;          
             switch (base) {
                 case 'A': case 'a': basefreq=matrix[i][0];basefreqReverse=matrix[matrix.length-(i+1)][3];break;
                 case 'C': case 'c': basefreq=matrix[i][1];basefreqReverse=matrix[matrix.length-(i+1)][2];break;
                 case 'G': case 'g': basefreq=matrix[i][2];basefreqReverse=matrix[matrix.length-(i+1)][1];break;
                 case 'T': case 't': basefreq=matrix[i][3];basefreqReverse=matrix[matrix.length-(i+1)][0];break;
             }
             directscore+=basefreq; 
             reversescore+=basefreqReverse;
         }
         return new double[]{directscore,reversescore};
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
    * [0] lowest obtainable score for a motif in a position
    * [1] highest obtainable score  for a motif in a position
    */
   private double[] getMinMaxScores(double[][] matrix) {
      double[] minmax=new double[2];
      for (int i=0;i<matrix.length;i++) {
         double maxColumn=getMax(matrix,i);
         double minColumn=getMin(matrix,i);
         minmax[0]+=minColumn;
         minmax[1]+=maxColumn;
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
}