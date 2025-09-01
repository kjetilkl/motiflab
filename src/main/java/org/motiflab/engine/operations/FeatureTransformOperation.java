/*
 
 
 */

package org.motiflab.engine.operations;

import org.motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.gui.VisualizationSettings;

/**
 * FeatureTransformOperation functions as a superclass for operations that implement simple transforms
 * of FeatureDataset objects. The superclass implements some functionality that is common for these transforms
 * and introduces a "inversion-of-control" paradigm whereby subclasses only need to implement the abstract
 * method {@code transformSequence} which performs the transformation of a single sequence. This method is
 * then called in turn for all applicable sequences in the chosen sequence collection.
 * @author kjetikl
 */
public abstract class FeatureTransformOperation extends Operation {

    
    /**
     * Override this function to implement transform of a single FeatureSequenceData object
     * The transformSequence method should call the boolean function {@code isConditionSatisfied} for each position along the sequence to verify
     * that the position should indeed be transformed according to the 'where-clause' of the operation task.
     * (if the isConditionSatisfied(i) function returns false then that position should be skipped, if true it should be transformed)
     * 
     * @param sourceSequence The original sequence to transformed. <b>NOTE:</b> This sequence should NOT be altered by the transform!!!
     * @param targetSequence The result of the transformation should be stored in this object
     * @return
     */   
    public abstract void transformSequence(final FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception;   

    /**
     * This method is called automatically before any transformation are executed
     * Subclasses should use this method to parse and resolve their own parameters
     */ 
    public abstract void resolveParameters(OperationTask task) throws Exception;   
       
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String sourceDatasetName=task.getSourceDataName();
        String targetDatasetName=task.getTargetDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());    
        FeatureDataset sourceDataset=(FeatureDataset)engine.getDataItem(sourceDatasetName);
        if (sourceDataset==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceDataset)) throw new ExecutionError(sourceDatasetName+"("+sourceDataset.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());
        FeatureDataset targetDataset=(FeatureDataset)sourceDataset.clone(); // Double-buffer.
        targetDataset.setName(targetDatasetName);
        
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) condition.resolve(engine, task);  
        
        Condition_within within=(Condition_within)task.getParameter("within");
        if (within!=null) within.resolve(engine, task);        
        
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SOURCE, sourceDataset);
        task.setParameter(OperationTask.TARGET, targetDataset);
        task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
        resolveParameters(task);
        
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        if (isSubrangeApplicable() && within!=null) { // remove sequences with no selection windows (if within-condition is used)
            Iterator iter=sequences.iterator();
            while (iter.hasNext()) {
                Sequence seq = (Sequence) iter.next();
                if (!within.existsSelectionWithinSequence(seq.getName(), task)) iter.remove();
            }           
        }
        
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(sourceDataset,targetDataset, sequence.getName(), task, counters));
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
        
        if (targetDataset instanceof NumericDataset) ((NumericDataset)targetDataset).updateAllowedMinMaxValuesFromData();
        if (targetDataset instanceof RegionDataset) ((RegionDataset)targetDataset).updateMaxScoreValueFromData();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.setIsDerived(true);
        if (engine.getClient() instanceof org.motiflab.gui.MotifLabGUI && !targetDatasetName.equals(sourceDatasetName) && !engine.dataExists(targetDatasetName, null)) { // a small hack to copy visualization settings from source when creating a new target
            boolean hasFG=engine.getClient().getVisualizationSettings().hasSetting(VisualizationSettings.FOREGROUND_COLOR, targetDatasetName);
            boolean hasVisibility=engine.getClient().getVisualizationSettings().hasSetting(VisualizationSettings.TRACK_VISIBLE, targetDatasetName);
            engine.getClient().getVisualizationSettings().copySettings(sourceDatasetName, targetDatasetName, false);    
            if (!hasFG) engine.getClient().getVisualizationSettings().setForeGroundColor(targetDatasetName,null); // clear copied color in order to assign a new
            if (!hasVisibility) engine.getClient().getVisualizationSettings().setTrackVisible(targetDatasetName,true); // always show new track (unless it is already specified to be hidden)
        }          
        try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
       
    
    
    protected boolean positionSatisfiesCondition(String sequencename, int pos, OperationTask task) throws Exception {
        Condition_position condition=(Condition_position)task.getParameter("where");
        Condition_within within=(Condition_within)task.getParameter("within");  
        if (within!=null) {
            if (!within.isConditionSatisfied(sequencename, pos, task)) return false;
        }
        if (condition==null) return true; // no where condition is set, so the transform should be applied everywhere
        else return condition.isConditionSatisfied(sequencename, pos, task);
    }
    
   
    protected boolean regionSatisfiesCondition(String sequencename, Region region, OperationTask task) throws Exception {
        Condition_region condition=(Condition_region)task.getParameter("where");
        Condition_within within=(Condition_within)task.getParameter("within");  
        if (within!=null) {
            if (!within.isConditionSatisfied(sequencename, region.getGenomicStart(),region.getGenomicEnd(), task)) return false;
        }
        if (condition==null) return true; // no where condition is set, so the transform should be applied everywhere
        else return condition.isConditionSatisfied(sequencename, region, task);
    }
          
    
    
    protected class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final FeatureDataset targetDataset;
        final FeatureDataset sourceDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;  
        
        public ProcessSequenceTask(FeatureDataset sourceDataset, FeatureDataset targetDataset, String sequencename, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset; 
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
            FeatureSequenceData targetSequence=(FeatureSequenceData)targetDataset.getSequenceByName(sequencename);
            
            if (sourceSequence==null) throw new SystemError("Unexpected error: No source sequence found named '"+sequencename+"'");
            if (targetSequence==null) throw new SystemError("Unexpected error: No target sequence found named '"+sequencename+"'");            

            transformSequence(sourceSequence, targetSequence, task);
            
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

