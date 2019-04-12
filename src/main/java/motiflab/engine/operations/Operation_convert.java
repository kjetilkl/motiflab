package motiflab.engine.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.TaskRunner;
import motiflab.engine.data.*;
import motiflab.engine.data.FeatureSequenceData;

/**
 *
 * @author kjetikl
 */
public class Operation_convert extends Operation {
    private static final String name="convert";
    private static final String description="Converts Numeric Datasets to Region Datasets or vice versa";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class,RegionDataset.class};
    
    public static final String NEW_VALUE="newValue";
    public static final String NEW_VALUE_STRING="newValueString";
    public static final String NEW_VALUE_OPERATOR="newValueOperator";
    public static final String TARGET_TYPE="targetType";
    public static final String REGION="region";
    public static final String NUMERIC="numeric";
  
    @Override
    public String getOperationGroup() {
        return "Derive";
    }
    
    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
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

    public void resolveParameters(OperationTask task) throws Exception {
        String newValueString=(String)task.getParameter(NEW_VALUE_STRING);
        if (newValueString==null) return;
        String newValueOperatorString=(String)task.getParameter(NEW_VALUE_OPERATOR);
        if (newValueOperatorString!=null) { // convert to canonical values
                 if (newValueOperatorString.equals("avg")) task.setParameter(NEW_VALUE_OPERATOR,"average");
            else if (newValueOperatorString.equals("minimum")) task.setParameter(NEW_VALUE_OPERATOR,"min");
            else if (newValueOperatorString.equals("maximum")) task.setParameter(NEW_VALUE_OPERATOR,"max");
        }
        Object newValueObject;
        newValueObject=engine.getDataItem(newValueString);
        if (newValueObject==null) {
            if (newValueString.startsWith("region.")) newValueObject=newValueString;
            else try {
              double value=Double.parseDouble(newValueString);
              newValueObject=new NumericConstant(newValueString, (double)value);
           } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+newValueString+"' does not represent a numerical value",task.getLineNumber());}         
        } else if (!(newValueObject instanceof NumericVariable || newValueObject instanceof SequenceNumericMap || newValueObject instanceof NumericDataset)) throw new ExecutionError("Unrecognized token '"+newValueString+"' does not represent a numerical value",task.getLineNumber());         
        task.setParameter(NEW_VALUE, newValueObject);
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
        FeatureDataset targetDataset=null; // Double-buffer.
        if (sourceDataset instanceof NumericDataset) targetDataset=new RegionDataset(targetDatasetName);
        else if (sourceDataset instanceof RegionDataset) targetDataset=new NumericDataset(targetDatasetName);
        
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) condition.resolve(engine, task);        
        resolveParameters(task);       
        SequenceCollection sequenceCollection=engine.getDefaultSequenceCollection();
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);        
                
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(sourceDataset, targetDataset, sequence.getName(), task, counters));
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
            throw new ExecutionError("Some mysterious error occurred while scanning");
        }           
        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        if (targetDataset instanceof RegionDataset) ((RegionDataset)targetDataset).updateMaxScoreValueFromData();
        targetDataset.setIsDerived(true);
        try {engine.storeDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
       
    
    
    private boolean positionSatisfiesCondition(String sequencename, int pos, OperationTask task) throws Exception {
        Condition_position condition=(Condition_position)task.getParameter("where");
        if (condition==null) return true; // no condition is set, so the transform should be applied everywhere
        else return condition.isConditionSatisfied(sequencename, pos, task);
    }
    
   
    private boolean regionSatisfiesCondition(String sequencename, Region region, OperationTask task) throws Exception {
        Condition_region condition=(Condition_region)task.getParameter("where");
        if (condition==null) return true; // no condition is set, so the transform should be applied everywhere
        else return condition.isConditionSatisfied(sequencename, region, task);
    }
    
   

    public void convertNumericToRegion(NumericSequenceData sourceSequence, RegionSequenceData targetSequence, OperationTask task, Object newValueObject, String newValueOperator) throws Exception {
        String seqname=sourceSequence.getName();
        boolean inside=false;
        int regionStart=0;
        int regionEnd=0;
        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
            if (positionSatisfiesCondition(seqname,i,task)) {
                if (!inside) {regionStart=i; inside=true;  } 
            } else { // condition not satisfied
                if (inside) {
                 regionEnd=i-1; inside=false;
                 Region newRegion=new Region(targetSequence, sourceSequence.getRelativePositionFromGenomic(regionStart), sourceSequence.getRelativePositionFromGenomic(regionEnd));
                 if (newValueObject!=null) newRegion.setScore(getRegionScore(newRegion.getRelativeStart(),newRegion.getRelativeEnd(),newValueObject,newValueOperator,seqname));
                 targetSequence.addRegion(newRegion);
              }               
           }
        } // end: for each position
        if (inside) { // last region did not close
             regionEnd=sourceSequence.getRegionEnd();
             Region newRegion=new Region(targetSequence, sourceSequence.getRelativePositionFromGenomic(regionStart), sourceSequence.getRelativePositionFromGenomic(regionEnd));
             if (newValueObject!=null) newRegion.setScore(getRegionScore(newRegion.getRelativeStart(),newRegion.getRelativeEnd(),newValueObject,newValueOperator,seqname));
             targetSequence.addRegion(newRegion);          
        }
    }          
            
    public void convertRegionToNumeric(RegionSequenceData sourceSequence, NumericSequenceData targetSequence, OperationTask task, Object newValueObject) throws Exception {
        ArrayList<Region> list = ((RegionSequenceData)sourceSequence).getAllRegions();
        String seqName=targetSequence.getName();
        double newValue=1;
        boolean setValueToRegionCount=false;
        boolean setValueToRegionScoreSum=false;
        
        NumericSequenceData wigseq=null;
             if (newValueObject instanceof NumericConstant) newValue=((NumericConstant)newValueObject).getValue().doubleValue();
        else if (newValueObject instanceof NumericVariable) newValue=((NumericVariable)newValueObject).getValue().doubleValue();
        else if (newValueObject instanceof SequenceNumericMap) newValue=((SequenceNumericMap)newValueObject).getValue(seqName).doubleValue();
        else if (newValueObject instanceof NumericDataset) wigseq=(NumericSequenceData)((NumericDataset)newValueObject).getSequenceByName(seqName);
        else if ((newValueObject instanceof String) && (((String)newValueObject).equals("region.count"))) {
            for (int i=0;i<targetSequence.getSize();i++) {targetSequence.setValueAtRelativePosition(i, 0);} // clear current sequence           
            setValueToRegionCount=true;
        } else if ((newValueObject instanceof String) && (((String)newValueObject).equals("region.sumscore"))) {
            for (int i=0;i<targetSequence.getSize();i++) {targetSequence.setValueAtRelativePosition(i, 0);} // clear current sequence           
            setValueToRegionScoreSum=true;
        }
        for (Region region:list) {
            if (regionSatisfiesCondition(seqName, region, task)) {
                if (newValueObject instanceof String) {
                    if (((String)newValueObject).equals("region.score") || ((String)newValueObject).equals("region.sumscore") || ((String)newValueObject).equals("region.highestscore")) newValue=region.getScore();
                    else if (((String)newValueObject).equals("region.length")) newValue=region.getLength();
                    else if (!((String)newValueObject).equals("region.count")) throw new ExecutionError("Unrecognized property: "+newValueObject.toString());
                } 
                int start=region.getRelativeStart();
                int end=region.getRelativeEnd();
                if (start<0) start=0;
                if (end>=sourceSequence.getSize()) end=sourceSequence.getSize()-1;
                if (wigseq==null) {
                    for (int i=start;i<=end;i++) {
                        if (setValueToRegionCount) {
                            double oldValue=targetSequence.getValueAtRelativePosition(i);
                            targetSequence.setValueAtRelativePosition(i, oldValue+1); // 
                        } else if (setValueToRegionScoreSum) {
                            double oldValue=targetSequence.getValueAtRelativePosition(i);
                            targetSequence.setValueAtRelativePosition(i, oldValue+newValue); //
                        } else {
                            double oldValue=targetSequence.getValueAtRelativePosition(i);
                            if (newValue>oldValue) targetSequence.setValueAtRelativePosition(i, newValue);
                        } // use same value all over
                    }
                } else {
                    for (int i=start;i<=end;i++) {
                        Double v=wigseq.getValueAtRelativePosition(i);
                        if (v!=null) targetSequence.setValueAtRelativePosition(i, v); // "copy" values from other Numeric dataset
                    }           
                }
            }            
        } // end: for each region
    }          
 
    
    /** Convenience function to retrieve the score function to use for a new region */
    private double getRegionScore(int start, int end, Object newValueObject, String newValueOperator, String seqName) {
        Double newValue=null;
             if (newValueObject instanceof NumericConstant) newValue=((NumericConstant)newValueObject).getValue().doubleValue();
        else if (newValueObject instanceof NumericVariable) newValue=((NumericVariable)newValueObject).getValue().doubleValue();
        else if (newValueObject instanceof SequenceNumericMap) newValue=((SequenceNumericMap)newValueObject).getValue(seqName).doubleValue();
        else if (newValueObject instanceof NumericDataset) {            
            NumericSequenceData wigseq=(NumericSequenceData)((NumericDataset)newValueObject).getSequenceByName(seqName);
            if (newValueOperator==null) newValueOperator="sum";
                 if (newValueOperator.equals("min")) newValue=wigseq.getMinValueInInterval(start, end);
            else if (newValueOperator.equals("max")) newValue=wigseq.getMaxValueInInterval(start, end);
            else if (newValueOperator.equals("average")) newValue=wigseq.getAverageValueInInterval(start, end);
            else if (newValueOperator.equals("median")) newValue=wigseq.getMedianValueInInterval(start, end);
            else if (newValueOperator.equals("sum")) newValue=wigseq.getSumValueInInterval(start, end);
        }
        if (newValue==null) return 0;
        else return newValue;
    }
    
    
    private class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final FeatureDataset targetDataset;
        final FeatureDataset sourceDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final Object newValueObject;
        final String newValueOperator;
        final OperationTask task;  
        
        public ProcessSequenceTask(FeatureDataset sourceDataset, FeatureDataset targetDataset, String sequencename, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset; 
           this.newValueObject=task.getParameter(NEW_VALUE);        
           this.newValueOperator=(String)task.getParameter(NEW_VALUE_OPERATOR); 
           this.counters=counters;
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
            FeatureSequenceData sourceSequence=(FeatureSequenceData)sourceDataset.getSequenceByName(sequencename);
            FeatureSequenceData targetSequence=null; // just in case...
            Sequence sequence=(Sequence)engine.getDataItem(sequencename, Sequence.class);
            if (sourceSequence instanceof NumericSequenceData) {
                targetSequence=new RegionSequenceData(sequence);
                convertNumericToRegion((NumericSequenceData)sourceSequence,(RegionSequenceData)targetSequence, task, newValueObject, newValueOperator);
            } else {
                targetSequence=new NumericSequenceData(sequence,0);
                convertRegionToNumeric((RegionSequenceData)sourceSequence,(NumericSequenceData)targetSequence, task, newValueObject);
            }
            targetDataset.addSequence(targetSequence);  // this operation is always applied to the DefaultSequenceCollection, so all sequences will be added eventually 
            
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


