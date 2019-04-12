package motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Iterator;
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
public class Operation_count extends FeatureTransformOperation {
    public static final String WINDOW_SIZE="windowSize"; 
    private static final String WINDOW_SIZE_VALUE="windowSizeValue"; 
    public static final String ANCHOR="anchor"; //
    public static final String UPSTREAM="start"; //
    public static final String DOWNSTREAM="end"; //
    public static final String CENTER="center"; //    
    public static final String COUNT_PROPERTY="countProperty"; //    
    public static final String REGION_NUMBER_COUNT="number"; //
    public static final String REGION_SCORES_COUNT="score"; //
    public static final String REGION_SCORES_IC_CONTENT="IC-content"; //    
    public static final String OVERLAPPING_OR_WITHIN="overlappingORwithin"; //
    private static final String name="count";
    private static final String description="Counts the number of regions (or sums the scores of regions) that overlap with or lie within a sliding window";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};
   
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
    
    @Override
    public boolean isSubrangeApplicable() {return true;}
    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String sourceDatasetName=task.getSourceDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());                
        String targetDatasetName=task.getTargetDataName();
        FeatureDataset sourceDataset=(FeatureDataset)engine.getDataItem(sourceDatasetName);
        if (sourceDataset==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceDataset)) throw new ExecutionError(sourceDatasetName+"("+sourceDataset.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());
        NumericDataset targetDataset=new NumericDataset(targetDatasetName);
        ArrayList<Data> sequenceList=engine.getAllDataItemsOfType(Sequence.class);
        for (Data seq:sequenceList) {
            NumericSequenceData numericSeq=new NumericSequenceData((Sequence)seq, 0);
            targetDataset.addSequence(numericSeq);
        }
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
            throw new ExecutionError("Some mysterious error occurred while performing the operation: "+getName());
        }         

        if (targetDataset instanceof NumericDataset) ((NumericDataset)targetDataset).updateAllowedMinMaxValuesFromData();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.setIsDerived(true);
        try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
    
    
    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        String windowSizeString=(String)task.getParameter(WINDOW_SIZE);
        Data windowSizeData=null;
        windowSizeData=engine.getDataItem(windowSizeString);
        if (windowSizeData==null) {
            try {
              double value=Double.parseDouble(windowSizeString);
              windowSizeData=new NumericConstant(windowSizeString, (double)value);
           } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+windowSizeString+"' neither data nor numeric constant",task.getLineNumber());}         
        }
        task.setParameter(WINDOW_SIZE_VALUE, windowSizeData);
    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String countProperty=(String)task.getParameter(COUNT_PROPERTY);
        String overlapping=(String)task.getParameter(OVERLAPPING_OR_WITHIN);
        Boolean strictlyWithin=true;
        if (overlapping.equalsIgnoreCase("overlapping")) strictlyWithin=false;
        Data windowSizeData=(Data)task.getParameter(WINDOW_SIZE_VALUE);
        String anchor=(String)task.getParameter(ANCHOR);
        int windowsize=5;   
        String seqname=sourceSequence.getName();
        if (windowSizeData instanceof SequenceNumericMap) windowsize=(int)((SequenceNumericMap)windowSizeData).getValue(seqname).doubleValue();
        else if (windowSizeData instanceof NumericVariable) windowsize=(int)((NumericVariable)windowSizeData).getValue().doubleValue();
        else if (windowSizeData instanceof NumericConstant) windowsize=(int)((NumericConstant)windowSizeData).getValue().doubleValue();
        if (windowsize==0) throw new ExecutionError("Window size can not be zero");
                      
        int orientation=sourceSequence.getStrandOrientation();
        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
            int start=0, end=0;
            if (anchor.equals(UPSTREAM)) {
                start=(orientation==Sequence.DIRECT)?i:i-windowsize+1;
                end=(orientation==Sequence.DIRECT)?i+windowsize-1:i;
            } else if (anchor.equals(DOWNSTREAM)) {
                start=(orientation==Sequence.DIRECT)?i-windowsize+1:i;                
                end=(orientation==Sequence.DIRECT)?i:i+windowsize-1;
            } else {
               if (windowsize%2==0) { // window has even number of bases - anchor left of center
                   int flanksize=(int)(windowsize/2);
                   start=(orientation==Sequence.DIRECT)?i-flanksize+1:i-flanksize;
                   end=(orientation==Sequence.DIRECT)?i+flanksize:i+flanksize-1;                   
               } else { // window has odd number of bases
                   int flanksize=(int)(windowsize/2);
                   start=i-flanksize;
                   end=i+flanksize;
               }
            }
            if (positionSatisfiesCondition(seqname,i,task)) {
              double newvalue=count((RegionSequenceData)sourceSequence,start,end,countProperty,strictlyWithin);
              ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i, newvalue);
           } // satisfies 'where'-condition
        }
    }
    
    private double count(RegionSequenceData sourceSequence, int start, int end, String countProperty, boolean strictlyWithin) throws ExecutionError {
        ArrayList<Region> list=null;
        if (strictlyWithin) list=sourceSequence.getRegionsWithinGenomicInterval(start, end);
        else list=sourceSequence.getRegionsOverlappingGenomicInterval(start, end);  
        //System.err.println(start+"-"+end+" => "+list.size());
        if (countProperty.equals(REGION_NUMBER_COUNT)) {
            return list.size();
        } else if (countProperty.equals(REGION_SCORES_COUNT)) {
            double scores=0;
            for (Region r:list) scores+=r.getScore();
            return scores;
        } else if (countProperty.equals(REGION_SCORES_IC_CONTENT)) {
            // Only count IC within the window from start to end!
            double sum=0;
            for (Region r:list) {
                Motif motif=(Motif)engine.getDataItem(r.getType(), Motif.class);
                if (motif==null) continue;
                // traverse the region fra region start to region end. 
                // for each position i. If position is outside window [start,end] continue to next
                // find the motif position which corresponds to i
                // get the IC for that position and add to sum.
                int offset=r.getGenomicStart();
                int length=r.getLength();
                for (int i=start;i<=end;i++) {
                    int motifoffset=start-offset; // offset into motif on direct strand
                    if (motifoffset<0 || motifoffset>=length) continue;
                    if (r.getOrientation()==Region.REVERSE) motifoffset=length-(motifoffset+1);
                    sum+=motif.getICcontentForColumn(motifoffset);                          
                }
            }
            return sum;             
        } else { // assume user-defined numeric property
            double sum=0;
            for (Region r:list) {
                Object value=r.getProperty(countProperty);
                if (value instanceof Number) sum+=((Number)value).doubleValue();
            }
            return sum;            
        }
    }
    
    
    private class ProcessSequenceTask implements Callable<FeatureSequenceData> {
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
