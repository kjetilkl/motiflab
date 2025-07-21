/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class Operation_score extends FeatureTransformOperation {
    private static final int ABSOLUTE=0;
    private static final int RELATIVE=1;
    private static final int SCORE_RAW=0;
    private static final int SCORE_LOGLIKELIHOOD=1;
    private static final int BOTH_STRANDS=0;
    private static final int DIRECT_STRAND=1;      
    private static final int REVERSE_STRAND=2;      
    private static final int RELATIVE_STRAND=3;      
    private static final int OPPOSITE_STRAND=4;      
    
    public static final String MOTIFNAME="MotifName";
    public static final String RAW_OR_LOGLIKELIHOOD="RawOrLoglikelihood";
    public static final String SCORE="Score";
    public static final String SCORE_ABSOLUTE="absolute";
    public static final String SCORE_RELATIVE="relative";    
    public static final String STRAND="Strand";
    public static final String STRAND_SINGLE="single";
    public static final String STRAND_RELATIVE="relative";
    public static final String STRAND_DIRECT="direct";
    public static final String STRAND_REVERSE="reverse";
    public static final String STRAND_OPPOSITE="opposite";
    public static final String STRAND_BOTH="both";    
    public static final String RAW="raw";
    public static final String LOGLIKELIHOOD="log-likelihood";
    public static final String BACKGROUNDMODEL="Background";
    private static final String name="score";
    
    private static final String description="Compares a single motif (or collection of motifs) agains a DNA sequence and returns a numeric dataset containing the match score for each position";
    private Class[] datasourcePreferences=new Class[]{DNASequenceDataset.class};

    @Override
    public String getOperationGroup() {
        return "Motif";
    }

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public boolean canUseAsSourceProxy(Data object) {
        return (object instanceof Motif || object instanceof MotifCollection);
    }     
    
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {return name;}
    
    @Override
    public boolean isSubrangeApplicable() {return true;}

    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        // this is not called here since I override execute
    }

    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        // this is not called here since I override execute
    }

    @Override
    public boolean assignToProxy(Object proxysource, OperationTask operationtask) {
        Data proxy=null;
        if (proxysource instanceof Data) proxy=(Data)proxysource;
        else if (proxysource instanceof Data[] && ((Data[])proxysource).length>0) proxy=((Data[])proxysource)[0];
        if (proxy instanceof Motif || proxy instanceof MotifCollection) {
          operationtask.setParameter(MOTIFNAME,proxy.getName());
          return true;
        } else return false;
    }    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String sourceDatasetName=task.getSourceDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());          
        String targetDatasetName=task.getTargetDataName();
        FeatureDataset sourceDataset=(FeatureDataset)engine.getDataItem(sourceDatasetName);
        if (sourceDataset==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceDataset)) throw new ExecutionError(sourceDatasetName+"("+sourceDataset.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());
        FeatureDataset targetDataset=new NumericDataset(targetDatasetName);
        ArrayList<Data> sequenceList=engine.getAllDataItemsOfType(Sequence.class);
        for (Data seq:sequenceList) {
            NumericSequenceData numericSeq=new NumericSequenceData((Sequence)seq, 0);
            targetDataset.addSequence(numericSeq);
        }        
        
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) condition.resolve(engine, task);   

        Condition_within within=(Condition_within)task.getParameter("within");
        if (within!=null) within.resolve(engine, task);           
        
        String motifName=(String)task.getParameter(MOTIFNAME);
        if (motifName==null || motifName.isEmpty()) throw new ExecutionError("Missing motif specification");     
        Object targetmotifs=engine.getDataItem(motifName);
        if (targetmotifs==null) throw new ExecutionError("No such data object: "+motifName);
        if (!(targetmotifs instanceof Motif || targetmotifs instanceof MotifCollection)) throw new ExecutionError(motifName+" is not a Motif or Motif Collection");            
        String raworlogString=(String)task.getParameter(RAW_OR_LOGLIKELIHOOD);
        int raworlog=SCORE_RAW;
        if (raworlogString.equals(LOGLIKELIHOOD)) raworlog=SCORE_LOGLIKELIHOOD;
        HashMap<String,double[][]> matrixTable=new HashMap<String, double[][]>();
        double pseudo=0.01;
        if (targetmotifs instanceof Motif) {
            Motif motif=(Motif)targetmotifs;
            double[][] matrix=motif.getMatrixAsFrequencyMatrixWithPseudo(pseudo);  
            if (raworlog==SCORE_LOGLIKELIHOOD) logMatrix(matrix);
            matrixTable.put(motif.getName(),matrix);           
        } else if (targetmotifs instanceof MotifCollection) {
            ArrayList<Motif> allMotifs=((MotifCollection)targetmotifs).getAllMotifs(engine);          
            for (Motif motif:allMotifs) { // search with each motif in turn
                double[][] matrix=motif.getMatrixAsFrequencyMatrixWithPseudo(pseudo);  
                if (raworlog==SCORE_LOGLIKELIHOOD) logMatrix(matrix);
                matrixTable.put(motif.getName(),matrix);
            }             
        }
        BackgroundModel background=null;       
        String backgroundModelname=(String)task.getParameter(BACKGROUNDMODEL);
        if (backgroundModelname!=null) {
            Data bg=engine.getDataItem(backgroundModelname);
            if (bg==null) throw new ExecutionError("Unknown data object '"+backgroundModelname+"'",task.getLineNumber());
            if (!(bg instanceof BackgroundModel)) throw new ExecutionError("'"+backgroundModelname+"' is not a Background Model",task.getLineNumber());
            background=(BackgroundModel)bg;  
        }
        int scoremode=ABSOLUTE;
        String scoremodestring=(String)task.getParameter(SCORE);        
             if (scoremodestring.equalsIgnoreCase(SCORE_ABSOLUTE)) scoremode=ABSOLUTE;
        else if (scoremodestring.equalsIgnoreCase(SCORE_RELATIVE)) scoremode=RELATIVE;
        int strand=BOTH_STRANDS;
        String strandstring=(String)task.getParameter(STRAND);        
             if (strandstring.equalsIgnoreCase(STRAND_BOTH)) strand=BOTH_STRANDS;
        else if (strandstring.equalsIgnoreCase(STRAND_RELATIVE) || strandstring.equalsIgnoreCase(STRAND_SINGLE)) strand=RELATIVE_STRAND;
        else if (strandstring.equalsIgnoreCase(STRAND_OPPOSITE)) strand=OPPOSITE_STRAND;
        else if (strandstring.equalsIgnoreCase(STRAND_DIRECT)) strand=DIRECT_STRAND;
        else if (strandstring.equalsIgnoreCase(STRAND_REVERSE)) strand=REVERSE_STRAND;
        double[] snf=(background!=null)?background.getSNF():new double[]{0.25f,0.25f,0.25f,0.25f};
        // log-transform the background
        snf[0]=Math.log(snf[0]);
        snf[1]=Math.log(snf[1]);
        snf[2]=Math.log(snf[2]);
        snf[3]=Math.log(snf[3]);
        
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        if (isSubrangeApplicable() && within!=null) { // remove sequences with no selection windows (if within-condition is used)
            Iterator iter=sequences.iterator();
            while (iter.hasNext()) {
                Sequence seq = (Sequence) iter.next();
                if (!within.existsSelectionWithinSequence(seq.getName(), task)) iter.remove();
            }           
        }        
        
//        int size=sequenceCollection.getNumberofSequences();
//        int i=0;
//        for (Sequence sequence:sequences) { // for each sequence in the collection
//            task.checkExecutionLock(); // checks to see if this task should suspend execution
//            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
//            String sequenceName=sequence.getName();
//            DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequenceName);
//            NumericSequenceData targetSequence=new NumericSequenceData(sequence,(raworlog==SCORE_LOGLIKELIHOOD)?Double.NEGATIVE_INFINITY:0);
//            if (raworlog==SCORE_LOGLIKELIHOOD) scoreSequenceLogLikelihood(sourceSequence,targetSequence, matrixTable, scoremode, strand, background, task);
//            else scoreSequenceRaw(sourceSequence,targetSequence, matrixTable, scoremode, strand, task);
//            targetDataset.addSequence(targetSequence);                    
//            task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
//            task.setProgress(i+1, size);
//            i++;
//            Thread.yield();
//        }   
        
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(sourceDataset, targetDataset, sequence.getName(), raworlog, scoremode, strand, matrixTable, background, task, counters));
        List<Future<FeatureSequenceData>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<FeatureSequenceData> future:futures) {
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
            throw new ExecutionError("Some mysterious error occurred while performing operation: "+getName());
        }           
        
        ((NumericDataset)targetDataset).updateAllowedMinMaxValuesFromData();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.setIsDerived(true);
        try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }    
    

    
    
    public void scoreSequenceLogLikelihood(DNASequenceData sourceSequence, NumericSequenceData targetSequence, HashMap<String,double[][]> matrixTable, int scoremode, int strand, BackgroundModel background, OperationTask task) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        double[] snf=(background!=null)?background.getSNF():new double[]{0.25f,0.25f,0.25f,0.25f};
        // log-transform the background
        snf[0]=Math.log(snf[0]);
        snf[1]=Math.log(snf[1]);
        snf[2]=Math.log(snf[2]);
        snf[3]=Math.log(snf[3]);
        int scorestrand=sourceSequence.getStrandOrientation();
        int counter=0;
        for (String motifname:matrixTable.keySet()) { // search with each motif in turn
            counter++;          
            double[][] matrix=matrixTable.get(motifname);
            double[] minmax=getMinMaxScores(matrix, snf);
            if (counter%50==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
                Thread.yield();
            }
            //if (counter%100==0) Thread.yield();
            int motifsize=matrix.length;
      
            for (int i=0;i<=sequence.length-motifsize;i++) {
                int pos=i;
                if (scorestrand==Sequence.REVERSE) pos+=(motifsize-1); // note that this is the position that will receive the score. It will correspond to the start or end of the motif depending on the sequence orientation
                if (!positionSatisfiesCondition(seqname,startOffset+pos,task)) continue;
                double[] scores=matrixMatcherLogLikelihood(matrix,sequence,i,snf);
                double relativeScoreDirect=(scores[0]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double relativeScoreReverse=(scores[1]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double comparisonScoreDirect=(scoremode==ABSOLUTE)?scores[0]:relativeScoreDirect;
                double comparisonScoreReverse=(scoremode==ABSOLUTE)?scores[1]:relativeScoreReverse;
                double score=(comparisonScoreDirect>comparisonScoreReverse)?comparisonScoreDirect:comparisonScoreReverse;
                     if (strand==RELATIVE_STRAND) score=(scorestrand==Sequence.REVERSE)?comparisonScoreReverse:comparisonScoreDirect; // if only single strand, use either direct or reverse depending on strand choice and sequence orientation
                else if (strand==OPPOSITE_STRAND) score=(scorestrand==Sequence.REVERSE)?comparisonScoreDirect:comparisonScoreReverse;
                else if (strand==DIRECT_STRAND)   score=comparisonScoreDirect; 
                else if (strand==REVERSE_STRAND)  score=comparisonScoreReverse;                            
                double currentscore=((NumericSequenceData)targetSequence).getValueAtRelativePosition(pos);
                if (score>currentscore) ((NumericSequenceData)targetSequence).setValueAtRelativePosition(pos,score); // this would apply if we test many motifs. Always keep highest score
            }
        }
        // remove -Inf values (replace them by 0)
        for (int i=0;i<sequence.length;i++) {
            if (((NumericSequenceData)targetSequence).getValueAtRelativePosition(i)==Double.NEGATIVE_INFINITY) ((NumericSequenceData)targetSequence).setValueAtRelativePosition(i,0);
        }

    }    
    
    public void scoreSequenceRaw(DNASequenceData sourceSequence, NumericSequenceData targetSequence, HashMap<String,double[][]> matrixTable, int scoremode, int strand, OperationTask task) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        int scorestrand=sourceSequence.getStrandOrientation();   
        int counter=0;      
        for (String motifname:matrixTable.keySet()) { // search with each motif in turn
            counter++;           
            double[][] matrix=matrixTable.get(motifname);
            double[] minmax=getMinMaxScores(matrix);
            if (counter%50==0) {
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
                Thread.yield(); 
            }
            int motifsize=matrix.length;
      
            for (int i=0;i<=sequence.length-motifsize;i++) {
                int pos=i;
                if (scorestrand==Sequence.REVERSE) pos+=(motifsize-1); // note that this is the position that will receive the score. It will correspond to the start or end of the motif depending on the sequence orientation
                if (!positionSatisfiesCondition(seqname,startOffset+pos,task)) continue;
                double[] scores=matrixMatcherRaw(matrix,sequence,i); // match both orientation of the motif to the sequence    
                double relativeScoreDirect=(scores[0]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double relativeScoreReverse=(scores[1]-minmax[0])/(minmax[1]-minmax[0]); // convert to relative scores
                double comparisonScoreDirect=(scoremode==ABSOLUTE)?scores[0]:relativeScoreDirect;
                double comparisonScoreReverse=(scoremode==ABSOLUTE)?scores[1]:relativeScoreReverse;
                double score=(comparisonScoreDirect>comparisonScoreReverse)?comparisonScoreDirect:comparisonScoreReverse; // best of direct and reverse match
                     if (strand==RELATIVE_STRAND) score=(scorestrand==Sequence.REVERSE)?comparisonScoreReverse:comparisonScoreDirect; // if only single strand, use either direct or reverse depending on strand choice and sequence orientation
                else if (strand==OPPOSITE_STRAND) score=(scorestrand==Sequence.REVERSE)?comparisonScoreDirect:comparisonScoreReverse;
                else if (strand==DIRECT_STRAND)   score=comparisonScoreDirect; 
                else if (strand==REVERSE_STRAND)  score=comparisonScoreReverse; 
                //System.err.println("strand="+strand+"   direct="+comparisonScoreDirect+"   reverse="+comparisonScoreReverse+"   score="+score+" scores=["+scores[0]+","+scores[1]+"]  relative=["+relativeScoreDirect+","+relativeScoreReverse+"]  minmax=["+minmax[0]+","+minmax[1]+"]");
                double currentscore=((NumericSequenceData)targetSequence).getValueAtRelativePosition(pos);
                if (score>currentscore) ((NumericSequenceData)targetSequence).setValueAtRelativePosition(pos,score);  // this would apply if we test many motifs. Always keep highest score
            }
        }              
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
  
   
    protected class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final FeatureDataset targetDataset;
        final FeatureDataset sourceDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final int raworlog;
        final int scoremode;
        final int strand;
        final HashMap<String,double[][]> matrixTable;
        final BackgroundModel background;
        final OperationTask task;  
        
        public ProcessSequenceTask(FeatureDataset sourceDataset, FeatureDataset targetDataset, String sequencename, int raworlog, int scoremode, int strand, HashMap<String,double[][]> matrixTable, BackgroundModel background, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset; 
           this.counters=counters;
           this.raworlog=raworlog;
           this.scoremode=scoremode;
           this.strand=strand;     
           this.matrixTable=matrixTable;
           this.background=background;
           this.task=task;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public FeatureSequenceData call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

            DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequencename);
            NumericSequenceData targetSequence=(NumericSequenceData)targetDataset.getSequenceByName(sequencename);
            if (raworlog==SCORE_LOGLIKELIHOOD) scoreSequenceLogLikelihood(sourceSequence,targetSequence, matrixTable, scoremode, strand, background, task);
            else scoreSequenceRaw(sourceSequence,targetSequence, matrixTable, scoremode, strand, task);                
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                task.setStatusMessage("Executing "+task.getOperationName()+":  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return targetSequence;
        }   
    }       
}
