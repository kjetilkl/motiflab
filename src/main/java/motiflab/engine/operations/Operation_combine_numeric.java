/*
 
 
 */

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
public class Operation_combine_numeric extends FeatureTransformOperation {
    public static final String SOURCE_DATA="sourceData"; // reference to an array with data objects
    public static final String OPERATOR="operator"; // 
    private static final String name="combine_numeric";
    private static final String description="Combines several Numeric Datasets, Numeric Maps or Numeric Variables into one data object using either the sum, minimum, maximum or average values at each position or for each entry";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class, SequenceNumericMap.class, MotifNumericMap.class, ModuleNumericMap.class, NumericVariable.class};
    public static final int USE_SUM=0;
    public static final int USE_MIN=1;
    public static final int USE_MAX=2;
    public static final int USE_AVERAGE=3;
    public static final int USE_PRODUCT=4;
  

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
        if (sourceDataString==null || sourceDataString.isEmpty())  throw new ExecutionError("Missing name for source data object",task.getLineNumber()); 
        String[] sourceNames=sourceDataString.split(",");
        Data[] sources=new Data[sourceNames.length];
        for (int i=0;i<sourceNames.length;i++) {
           String sourcename=sourceNames[i].trim();
           if (sourcename.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());
           Data data=engine.getDataItem(sourcename);
           if (data==null) throw new ExecutionError("Unknown data object '"+sourcename+"'",task.getLineNumber());
           if (!(data instanceof NumericDataset || data instanceof NumericMap || data instanceof NumericVariable)) throw new ExecutionError("'"+sourcename+"' is not a numeric data object (Numeric Dataset, Numeric Map or Numeric Variable)",task.getLineNumber());
           if (i>0 && data.getClass()!=sources[0].getClass()) throw new ExecutionError("All the data objects must be of the same type");
           sources[i]=data;
        }
        String operator=(String)task.getParameter(OPERATOR);
        if (operator.equalsIgnoreCase("minimum")) task.setParameter(OPERATOR, "min");
        else if (operator.equalsIgnoreCase("maximum")) task.setParameter(OPERATOR, "max");
        else if (operator.equalsIgnoreCase("avg")) task.setParameter(OPERATOR, "average");
        task.setParameter(SOURCE_DATA, sources);
    }
    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        // this method is not used anymore since I override the execute() method in FeatureTransformOperation and use my own transformSequence (below)
    }   
    
    public void transformSequence(NumericSequenceData[] sourceSequences, NumericSequenceData targetSequence, OperationTask task, int operator) throws Exception {
        String seqname=targetSequence.getName();
        double[] values=new double[sourceSequences.length];
        for (int i=targetSequence.getRegionStart();i<=targetSequence.getRegionEnd();i++) {
            if (positionSatisfiesCondition(seqname,i,task)) {
              double newvalue=0;     
              for (int j=0;j<sourceSequences.length;j++) {
                  values[j]=sourceSequences[j].getValueAtGenomicPosition(i);
              }
              switch (operator) {
                  case USE_SUM: newvalue=calculateSum(values); break;
                  case USE_PRODUCT: newvalue=calculateProduct(values); break;
                  case USE_MIN: newvalue=findMinimum(values); break;
                  case USE_MAX: newvalue=findMaximum(values); break;
                  case USE_AVERAGE: newvalue=calculateSum(values)/(double)sourceSequences.length; break;
              }
              targetSequence.setValueAtGenomicPosition(i, newvalue);
           } // end: satisfies 'where'-condition
        }
    }
    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {       
        resolveParameters(task);              
        Data[] sources=(Data[])task.getParameter(SOURCE_DATA);
        if (sources.length==0) throw new ExecutionError("Missing source data objects",task.getLineNumber());
        String targetDatasetName=task.getTargetDataName();
        int operator=USE_SUM;
        String operatorString=(String)task.getParameter(OPERATOR);
             if (operatorString.equalsIgnoreCase("sum")) operator=USE_SUM;
        else if (operatorString.equalsIgnoreCase("product")) operator=USE_PRODUCT;
        else if (operatorString.equalsIgnoreCase("average")) operator=USE_AVERAGE;
        else if (operatorString.equalsIgnoreCase("min") || operatorString.equalsIgnoreCase("minimum")) operator=USE_MIN;
        else if (operatorString.equalsIgnoreCase("max") || operatorString.equalsIgnoreCase("maximum")) operator=USE_MAX;
        else throw new ExecutionError("Unknown operator '"+operatorString+"'",task.getLineNumber());        
        
        Class typeclass=sources[0].getClass();
        if (typeclass==NumericDataset.class) {
            if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
            NumericDataset targetDataset=(NumericDataset)sources[0].clone(); // Double-buffer. Clone first in list!
            targetDataset.setName(targetDatasetName);        
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
            NumericDataset[] datasetsources=new NumericDataset[sources.length];
            for (int i=0;i<sources.length;i++) datasetsources[i]=(NumericDataset)sources[i];
            ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
            for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(datasetsources, targetDataset, sequence.getName(), operator, task, counters));
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

            targetDataset.setIsDerived(true);
            try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
            return true;
        } else if (NumericMap.class.isAssignableFrom(typeclass)) {
            NumericMap targetDataMap=NumericMap.createMapForType(targetDatasetName, NumericMap.getDataTypeForMapType(typeclass), 0);
            // combine values for each entry in the map
            for (String key:targetDataMap.getAllKeys(engine)) {
                double[] values=new double[sources.length];
                for (int j=0;j<sources.length;j++) {
                    values[j]=((NumericMap)sources[j]).getValue(key);
                }
                double newvalue=0;
                switch (operator) {
                      case USE_SUM: newvalue=calculateSum(values); break;
                      case USE_PRODUCT: newvalue=calculateProduct(values); break;
                      case USE_MIN: newvalue=findMinimum(values); break;
                      case USE_MAX: newvalue=findMaximum(values); break;
                      case USE_AVERAGE: newvalue=calculateSum(values)/(double)sources.length; break;
                }  
                targetDataMap.setValue(key, newvalue);
            }   
            // now combine the default values also
            double[] values=new double[sources.length];
            for (int j=0;j<sources.length;j++) {
                values[j]=((NumericMap)sources[j]).getValue();
            }
            double newvalue=0;
            switch (operator) {
                  case USE_SUM: newvalue=calculateSum(values); break;
                  case USE_PRODUCT: newvalue=calculateProduct(values); break;
                  case USE_MIN: newvalue=findMinimum(values); break;
                  case USE_MAX: newvalue=findMaximum(values); break;
                  case USE_AVERAGE: newvalue=calculateSum(values)/(double)sources.length; break;
            }  
            targetDataMap.setDefaultValue(newvalue);
                          
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            try {engine.updateDataItem(targetDataMap);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
            return true;           
        } else if (typeclass==NumericVariable.class) {
            double[] values=new double[sources.length];
            for (int j=0;j<sources.length;j++) {
                values[j]=((NumericVariable)sources[j]).getValue();
            }
            double newvalue=0;
            switch (operator) {
                  case USE_SUM: newvalue=calculateSum(values); break;
                  case USE_PRODUCT: newvalue=calculateProduct(values); break;
                  case USE_MIN: newvalue=findMinimum(values); break;
                  case USE_MAX: newvalue=findMaximum(values); break;
                  case USE_AVERAGE: newvalue=calculateSum(values)/(double)sources.length; break;
            }
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            NumericVariable targetData=new NumericVariable(targetDatasetName, newvalue);
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
            return true;
        } else throw new ExecutionError("SystemError: data type not supported for operation 'combine_numeric'");
    }
    
    /** Calculates the sum of an array of double values */
    private double calculateSum(double[] values) {
        double sum=0;
        for (double val:values) sum+=val;
        return sum;
    }
    
    /** Calculates the product of an array of double values */
    private double calculateProduct(double[] values) {
        double product=1f;
        for (double val:values) product*=val;
        return product;
    }
    
    /** Finds the smallest value in an array of values */
    private double findMinimum(double[] values) {
        double min=Double.MAX_VALUE;
        for (double val:values) {
            if (val<min) min=val;
        }
        return min;
    }
    /** Finds the largest value in an array of values */
    private double findMaximum(double[] values) {
        double max=-Double.MAX_VALUE;
        for (double val:values) {
            if (val>max) max=val;
        }
        return max;
    }

    private class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final NumericDataset targetDataset;
        final NumericDataset[] sourceDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final int operator;
        final OperationTask task;  
        
        public ProcessSequenceTask(NumericDataset[] sourceDataset, NumericDataset targetDataset, String sequencename, int operator, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset;
           this.operator=operator;
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
            NumericSequenceData targetSequence=(NumericSequenceData)targetDataset.getSequenceByName(sequencename);
            NumericSequenceData[] sourceSequences=new NumericSequenceData[sourceDataset.length];
            for (int j=0;j<sourceDataset.length;j++) {
                sourceSequences[j]=(NumericSequenceData)sourceDataset[j].getSequenceByName(sequencename);
            }            
                       
            transformSequence(sourceSequences, targetSequence, task, operator);  
                        
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
    