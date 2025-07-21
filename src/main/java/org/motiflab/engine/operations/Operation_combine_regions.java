/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class Operation_combine_regions extends FeatureTransformOperation {
    public static final String SOURCE_DATA="sourceData"; // reference to an array with RegionDatasets
    public static final String OPERATOR="operator"; // 
    private static final String name="combine_regions";
    private static final String description="Combines several Region Datasets into one";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};

    @Override
    public String getOperationGroup() {
        return "Combine";
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

    @Override
    public boolean canHaveMultipleInput() {
        return true; 
    }    
    
    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        String sourceDataString=(String)task.getSourceDataName(); // this could now potentially be a comma-separated list!
        if (sourceDataString==null || sourceDataString.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());
        String[] sourceNames=sourceDataString.split(",");
        RegionDataset[] sources=new RegionDataset[sourceNames.length];
        for (int i=0;i<sourceNames.length;i++) {
           String sourcename=sourceNames[i].trim();
           if (sourcename.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());
           Data data=engine.getDataItem(sourcename);
           if (data==null) throw new ExecutionError("Unknown data object '"+sourcename+"'",task.getLineNumber());
           if (!(data instanceof RegionDataset)) throw new ExecutionError("Data object '"+sourcename+"' is not of required type (Region Dataset)",task.getLineNumber());
           sources[i]=(RegionDataset)data;
        }
        task.setParameter(SOURCE_DATA, sources);
    }
    
  
    public void transformSequence(RegionSequenceData[] sourceSequences, RegionSequenceData targetSequence, OperationTask task) throws Exception {        
        String seqname=targetSequence.getName();
        for (int i=0;i<sourceSequences.length;i++) {
             for (Region region:sourceSequences[i].getAllRegions()) {
                if (regionSatisfiesCondition(seqname, region, task)) {
                   targetSequence.addRegion(region.clone());
                }
             }
        } 
    }
    
  @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        // this method is not used anymore since I override the execute() method in FeatureTransformOperation and use my own transformSequence (below)
    }   
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        resolveParameters(task);
        RegionDataset[] sources=(RegionDataset[])task.getParameter(SOURCE_DATA);
        if (sources.length==0) throw new ExecutionError("Missing source data objects",task.getLineNumber());
        String targetDatasetName=task.getTargetDataName();
        RegionDataset targetDataset=sources[0].clone(); // Double-buffer. Clone first in list!
        targetDataset.setName(targetDatasetName);   
        targetDataset.clearRegions();
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) condition.resolve(engine, task);        
        
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(sources, targetDataset, sequence.getName(), task, counters));
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
            throw new ExecutionError("Some mysterious error occurred while performing the operation: "+getName());
        }         
        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.updateMaxScoreValueFromData();
        targetDataset.setIsDerived(true);
        boolean isAllMotifTracks=true;
        boolean isAllModuleTracks=true;
        boolean isAllNestedTracks=true;
        for (RegionDataset source:sources) {
            isAllMotifTracks=isAllMotifTracks&&source.isMotifTrack();
            isAllModuleTracks=isAllModuleTracks&&source.isModuleTrack();
            isAllNestedTracks=isAllNestedTracks&&source.isNestedTrack();            
        }
        targetDataset.setMotifTrack(isAllMotifTracks);
        targetDataset.setModuleTrack(isAllModuleTracks);
        targetDataset.setNestedTrack(isAllNestedTracks);        
        try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
    
    
    
   private class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final RegionDataset targetDataset;
        final RegionDataset[] sourceDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;  
        
        public ProcessSequenceTask(RegionDataset[] sourceDataset, RegionDataset targetDataset, String sequencename, OperationTask task, long[] counters) {
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
            RegionSequenceData targetSequence=(RegionSequenceData)targetDataset.getSequenceByName(sequencename);
            RegionSequenceData[] sourceSequences=new RegionSequenceData[sourceDataset.length];
            for (int j=0;j<sourceDataset.length;j++) {
                sourceSequences[j]=(RegionSequenceData)sourceDataset[j].getSequenceByName(sequencename);
            }            
                       
            transformSequence(sourceSequences, targetSequence, task);  
                        
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
    