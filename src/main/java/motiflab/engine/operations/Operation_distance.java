package motiflab.engine.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.TaskRunner;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.SequenceNumericMap;

/**
 *
 * @author kjetikl
 */
public class Operation_distance extends Operation {
    public static final String ANCHOR_POINT="anchorPoint"; // 
    public static final String RELATIVE_ANCHOR_POINT="relativeAnchorPoint"; // this is used as 'offset' if the anchor is a Numeric value
    public static final String DIRECTION="DIRECTION"; // 
    public static final String UPSTREAM="upstream"; // 
    public static final String DOWNSTREAM="downstream"; // 
    public static final String BOTH="both"; // 
    private static final String name="distance";
    private static final String description="Creates a new Numeric Dataset where the value at each base is determined by its distance from a selected feature";
      
    private Class[] datasourcePreferences=new Class[]{};

    @Override
    public String getOperationGroup() {
        return "Derive";
    }

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public boolean canUseAsSourceProxy(Data object) {
        return (object instanceof FeatureDataset || object instanceof SequenceNumericMap || object instanceof NumericVariable);
    }       
    @Override
    public boolean assignToProxy(Object proxysource, OperationTask operationtask) {
        Data proxy=null;
        if (proxysource instanceof Data) proxy=(Data)proxysource;
        else if (proxysource instanceof Data[] && ((Data[])proxysource).length>0) {
            for (Data data:(Data[])proxysource) {
                if (data instanceof FeatureDataset || data instanceof SequenceNumericMap || data instanceof NumericVariable) {proxy=data;break;}
            }
        }
        if (proxy instanceof RegionDataset || proxy instanceof SequenceNumericMap || proxy instanceof NumericVariable) {
          operationtask.setParameter(ANCHOR_POINT,proxy.getName());
          return true;
        } else if (proxy instanceof FeatureDataset) {
          operationtask.setParameter(ANCHOR_POINT,"transcription start site");
          return true;
        } else return false;
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
    public boolean execute(OperationTask task) throws Exception {            
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String targetDatasetName=task.getTargetDataName();
        if (targetDatasetName==null || targetDatasetName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        NumericDataset targetDataset=new NumericDataset(targetDatasetName);
        targetDataset.setName(targetDatasetName);
        Object anchor=null;
        String direction=(String)task.getParameter(Operation_distance.DIRECTION);
        if (direction==null || direction.isEmpty()) direction=Operation_distance.BOTH;
        String anchorPoint=(String)task.getParameter(Operation_distance.ANCHOR_POINT);
        String relativeAnchor=(String)task.getParameter(Operation_distance.RELATIVE_ANCHOR_POINT);
        if (anchorPoint==null || anchorPoint.isEmpty()) throw new ExecutionError("Missing anchor specification");
        if (relativeAnchor==null || relativeAnchor.isEmpty()) relativeAnchor="transcription start site";
             if (anchorPoint.equalsIgnoreCase("transcription start site")) anchor="TSS";
        else if (anchorPoint.equalsIgnoreCase("transcription end site")) anchor="TES";
        else if (anchorPoint.equalsIgnoreCase("sequence upstream end")) anchor="upstream";
        else if (anchorPoint.equalsIgnoreCase("sequence downstream end")) anchor="downstream";
        else {
            try {
                int pos=Integer.parseInt(anchorPoint);
                anchor=new NumericVariable(""+pos, pos);
            } catch (NumberFormatException e) {
                Object dataobject=engine.getDataItem(anchorPoint);
                if (dataobject==null) throw new ExecutionError("No such data object: "+anchorPoint,task.getLineNumber());
                if (!(dataobject instanceof RegionDataset || dataobject instanceof SequenceNumericMap || dataobject instanceof NumericVariable)) throw new ExecutionError("Anchor point for distance function is not a Region Dataset, Sequence Numeric Map or Numeric Variable",task.getLineNumber());
                anchor=dataobject;
            }
        }            
        ArrayList<Sequence> sequences=engine.getDefaultSequenceCollection().getAllSequences(engine);

        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(targetDataset, sequence.getName(), anchor, direction, relativeAnchor, task, counters));
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
             
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.setIsDerived(true);
        try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }

    
    public void transformSequence(NumericSequenceData targetSequence, Object anchor, String direction, String relativeAnchor, OperationTask task) throws Exception {
         int orientation=targetSequence.getStrandOrientation();
         int usedirection=0;
         if (direction.equals(Operation_distance.DOWNSTREAM)) usedirection=1;
         else if (direction.equals(Operation_distance.UPSTREAM)) usedirection=-1;    
         
         if (anchor instanceof String && ((String)anchor).equalsIgnoreCase("TSS")) {
             Integer TSSint=targetSequence.getTSS();
             if (TSSint==null) return;
             transformSequenceAroundAnchor(targetSequence, TSSint, usedirection, orientation, task);
         } else if (anchor instanceof String && ((String)anchor).equalsIgnoreCase("TES")) {
             Integer TESint=targetSequence.getTES();
             if (TESint==null) return;
             transformSequenceAroundAnchor(targetSequence, TESint, usedirection, orientation, task);
         } else if (anchor instanceof String && ((String)anchor).equalsIgnoreCase("upstream")) {
             int upstream=(orientation==Sequence.DIRECT)?targetSequence.getRegionStart():targetSequence.getRegionEnd();
             transformSequenceAroundAnchor(targetSequence, upstream, usedirection, orientation, task);
         } else if (anchor instanceof String && ((String)anchor).equalsIgnoreCase("downstream")) {
             int downstream=(orientation==Sequence.DIRECT)?targetSequence.getRegionEnd():targetSequence.getRegionStart();
             transformSequenceAroundAnchor(targetSequence, downstream, usedirection, orientation, task);
         } else if (anchor instanceof NumericVariable || anchor instanceof SequenceNumericMap) {
             int anchorpos=(anchor instanceof NumericVariable)?((NumericVariable)anchor).getValue().intValue():((SequenceNumericMap)anchor).getValue(targetSequence.getName()).intValue();
             if (relativeAnchor.equalsIgnoreCase("transcription start site")) {
                 Integer TSSint=targetSequence.getTSS();
                 if (TSSint==null) return;  
                 if (orientation==Sequence.DIRECT) anchorpos+=TSSint; else anchorpos=TSSint-anchorpos;
             } else if (relativeAnchor.equalsIgnoreCase("transcription end site")) {
                 Integer TESint=targetSequence.getTSS();
                 if (TESint==null) return;         
                 if (orientation==Sequence.DIRECT) anchorpos+=TESint; else anchorpos=TESint-anchorpos;
             } else if (relativeAnchor.equalsIgnoreCase("sequence upstream end")) {
                 int upstream=(orientation==Sequence.DIRECT)?targetSequence.getRegionStart():targetSequence.getRegionEnd();
                 if (orientation==Sequence.DIRECT) anchorpos+=upstream; else anchorpos=upstream-anchorpos;          
             } else if (relativeAnchor.equalsIgnoreCase("sequence downstream end")) {
                 int downstream=(orientation==Sequence.DIRECT)?targetSequence.getRegionEnd():targetSequence.getRegionStart();                
                 if (orientation==Sequence.DIRECT) anchorpos+=downstream; else anchorpos=downstream-anchorpos;
             } else if (relativeAnchor.equalsIgnoreCase("chromosome start")) {
                 // do nothing to the anchor
             } 
             transformSequenceAroundAnchor(targetSequence, anchorpos, usedirection, orientation, task);
         } else if (anchor instanceof RegionDataset) { // anchor is a region dataset
             RegionDataset dataset=(RegionDataset)anchor;
             RegionSequenceData regionSequence=(RegionSequenceData)dataset.getSequenceByName(targetSequence.getName());
             ArrayList<Region> regions=regionSequence.getAllRegions();
             if (regions.isEmpty()) return;
             int pos1=targetSequence.getRegionStart();
             int pos2=regions.get(0).getGenomicStart();
             for (int p=pos1;p<=pos2-1;p++) { // between start and first region
                 double value=0;
                      if (usedirection>0) value=(orientation==Sequence.DIRECT)?0:pos2-p;
                 else if (usedirection<0) value=(orientation==Sequence.DIRECT)?pos2-p:0;
                 else value=pos2-p;
                 targetSequence.setValueAtGenomicPosition(p, value);
             }             
             for (int i=1;i<regions.size();i++) { // between regions
                 int newpos1=regions.get(i-1).getGenomicEnd();
                 if (newpos1>pos1) pos1=newpos1; 
                 pos2=regions.get(i).getGenomicStart();               
                 if (pos2-pos1<1) continue; // no 'space' between regions
                 for (int p=pos1+1;p<=pos2-1;p++) {
                     double value=0;
                          if (usedirection>0) value=(orientation==Sequence.DIRECT)?p-pos1:pos2-p;
                     else if (usedirection<0) value=(orientation==Sequence.DIRECT)?pos2-p:p-pos1;
                     else value=Math.min(p-pos1, pos2-p);
                     targetSequence.setValueAtGenomicPosition(p, value);
                 }
             }
             pos1=regions.get(regions.size()-1).getGenomicEnd();
             pos2=targetSequence.getRegionEnd();
             for (int p=pos1+1;p<=pos2;p++) { // between end and last region
                 double value=0;
                      if (usedirection>0) value=(orientation==Sequence.DIRECT)?p-pos1:0;
                 else if (usedirection<0) value=(orientation==Sequence.DIRECT)?0:p-pos1;
                 else value=p-pos1;
                 targetSequence.setValueAtGenomicPosition(p, value);
             }               
         } else if (anchor==null) throw new ExecutionError("SYSTEM ERROR: Anchor is NULL");
         else throw new ExecutionError("SYSTEM ERROR: Anchor is neither String nor Region Dataset but "+anchor.getClass().getSimpleName()+". Anchor="+anchor.toString());                 
    }

    /**
     * 
     * @param targetSequence
     * @param anchor
     * @param direction 0=both, 
     * @param orientation
     * @param task
     * @throws Exception 
     */
    private void transformSequenceAroundAnchor(NumericSequenceData targetSequence, int anchor, int direction, int orientation, OperationTask task) throws Exception {
         for (int i=targetSequence.getRegionStart();i<=targetSequence.getRegionEnd();i++) {
            double value=0;
            if (direction>0) value=(i-anchor)*orientation;
            else if (direction<0) value=(anchor-i)*orientation;
            else value=Math.abs(i-anchor);
            targetSequence.setValueAtGenomicPosition(i, value); 
         }        
    }    
    
    
    private class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final NumericDataset targetDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final Object anchor;
        final String direction;
        final String relativeAnchor;
        final OperationTask task;  
        
        public ProcessSequenceTask(NumericDataset targetDataset, String sequencename, Object anchor, String direction, String relativeAnchor, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.targetDataset=targetDataset;
           this.anchor=anchor;
           this.direction=direction;
           this.relativeAnchor=relativeAnchor;
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

            Sequence sequence=(Sequence)engine.getDataItem(sequencename,Sequence.class);
            NumericSequenceData numericSequence=new NumericSequenceData(sequence,0);
            transformSequence(numericSequence,anchor, direction, relativeAnchor, task);    
            targetDataset.addSequence(numericSequence); // this operation is always applied to the DefaultSequenceCollection, so all sequences will be added eventually 
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                task.setStatusMessage("Executing "+task.getOperationName()+":  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return numericSequence;
        }   
    }      
    
}
