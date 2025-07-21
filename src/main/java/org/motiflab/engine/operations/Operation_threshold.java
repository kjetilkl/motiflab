/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class Operation_threshold extends FeatureTransformOperation {
    public static final String ABOVE_OR_EQUAL_STRING="aboveOrEqualString"; //
    public static final String BELOW_STRING="belowString"; //
    private static final String ABOVE_OR_EQUAL_VALUE="aboveOrEqualValue"; //
    private static final String BELOW_VALUE="belowValue"; //
//    private static final String SEQUENCE_MIN="sequenceMin";
//    private static final String SEQUENCE_MAX="sequenceMax";
    private static final String DATASET_MIN="datasetMin";
    private static final String DATASET_MAX="datasetMax";
    private static final String COLLECTION_MIN="collectionMin";
    private static final String COLLECTION_MAX="collectionMax";
    public static final String CUTOFF_THRESHOLD_STRING="cutoffThresholdString";
    private static final String CUTOFF_THRESHOLD="cutoffThreshold";
    public static final String THRESHOLD_TYPE="thresholdType";
    public static final String THRESHOLD_TYPE_NORMAL="thresholdTypeNormal";
    public static final String THRESHOLD_TYPE_FRACTION_SEQUENCE_RANGE="thresholdTypeFractionSequence";
    public static final String THRESHOLD_TYPE_FRACTION_DATASET_RANGE="thresholdTypeFractionDataset";    
    public static final String THRESHOLD_TYPE_FRACTION_COLLECTION_RANGE="thresholdTypeFractionCollection";
    private static final String name="threshold";
    private static final String description="Transforms all numeric values in a data object currently above or equal to the threshold to a selected upper value and those below to a second lower value";
    private static String[] specialvalues=new String[]{"dataset.min","dataset.max","sequence.min","sequence.max","collection.min","collection.max"};

    private Class[] datasourcePreferences=new Class[]{NumericDataset.class,RegionDataset.class,NumericMap.class,ExpressionProfile.class};

    @Override
    public String getOperationGroup() {
        return "Transform";
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

    /** Returns a list of special value settings for the normalization operation */
    public static String[] getSpecialValues() {
        return (String[])specialvalues.clone();
    }

    private boolean isSpecialValue(String string) {
        for (String special:specialvalues) {
            if (special.equals(string)) return true;
        }
        return false;
    }
    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        Data sourceData=(Data)task.getSourceData();
        String aboveString=(String)task.getParameter(ABOVE_OR_EQUAL_STRING);
        if (isSpecialValue(aboveString)) task.setParameter(ABOVE_OR_EQUAL_VALUE, aboveString);
        else {
            Data aboveData=null;
            aboveData=engine.getDataItem(aboveString);
            if (aboveData==null) {
                try {
                  double value=Double.parseDouble(aboveString);
                  aboveData=new NumericVariable(aboveString, (double)value);
               } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+aboveString+"' neither data nor numeric constant",task.getLineNumber());}
            }
            if (!(aboveData instanceof NumericVariable || aboveData instanceof NumericMap)) throw new ExecutionError("'"+aboveString+"' is not a valid numeric data object",task.getLineNumber());
            if (sourceData instanceof NumericMap && aboveData instanceof NumericMap && !(((NumericMap)sourceData).getMembersClass().equals(((NumericMap)aboveData).getMembersClass())) ) throw new ExecutionError("'"+aboveString+"' is a Numeric Map of a type not compatible with the source data",task.getLineNumber());
            task.setParameter(ABOVE_OR_EQUAL_VALUE, aboveData);
        }

        String belowString=(String)task.getParameter(BELOW_STRING);
        if (isSpecialValue(belowString)) task.setParameter(BELOW_VALUE, belowString);
        else {
            Data belowData=null;
            belowData=engine.getDataItem(belowString);
            if (belowData==null) {
                try {
                  double value=Double.parseDouble(belowString);
                  belowData=new NumericVariable(belowString, (double)value);
               } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+belowString+"' neither data nor numeric constant",task.getLineNumber());}
            }
            if (!(belowData instanceof NumericVariable || belowData instanceof NumericMap)) throw new ExecutionError("'"+belowString+"' is not a valid numeric data object",task.getLineNumber());
            if (sourceData instanceof NumericMap && belowData instanceof NumericMap && !(((NumericMap)sourceData).getMembersClass().equals(((NumericMap)belowData).getMembersClass())) ) throw new ExecutionError("'"+belowString+"' is a Numeric Map of a type not compatible with the source data",task.getLineNumber());
            task.setParameter(BELOW_VALUE, belowData);
        }

        String cutoffthresholdString=(String)task.getParameter(CUTOFF_THRESHOLD_STRING);
        if (cutoffthresholdString.endsWith("%") || cutoffthresholdString.endsWith("%C")) {
            cutoffthresholdString=cutoffthresholdString.substring(0,cutoffthresholdString.indexOf('%'));
            task.setParameter(THRESHOLD_TYPE,THRESHOLD_TYPE_FRACTION_COLLECTION_RANGE);            
        } else if (cutoffthresholdString.endsWith("%D")) {
            cutoffthresholdString=cutoffthresholdString.substring(0,cutoffthresholdString.indexOf('%'));
            task.setParameter(THRESHOLD_TYPE,THRESHOLD_TYPE_FRACTION_DATASET_RANGE);
        } else if (cutoffthresholdString.endsWith("%S")) {
            cutoffthresholdString=cutoffthresholdString.substring(0,cutoffthresholdString.indexOf('%'));
            task.setParameter(THRESHOLD_TYPE,THRESHOLD_TYPE_FRACTION_SEQUENCE_RANGE);
        } else task.setParameter(THRESHOLD_TYPE,THRESHOLD_TYPE_NORMAL);
        Data cutoffData=null;
        cutoffData=engine.getDataItem(cutoffthresholdString);
        if (cutoffData==null) {
            try {
              double value=Double.parseDouble(cutoffthresholdString);
              cutoffData=new NumericVariable(cutoffthresholdString, (double)value);
           } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+cutoffthresholdString+"' neither data nor numeric constant",task.getLineNumber());}
        }
        if (!(cutoffData instanceof NumericVariable || cutoffData instanceof SequenceNumericMap)) throw new ExecutionError("'"+cutoffthresholdString+"' is not a valid numeric data object in this context",task.getLineNumber());
        task.setParameter(CUTOFF_THRESHOLD, cutoffData);

        String subsetName=(String)task.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        Data subsetCollection=null;
        if (subsetName!=null && !subsetName.isEmpty()) {
            Data col=engine.getDataItem(subsetName);
            if (col==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
            if ((sourceData instanceof SequenceNumericMap || sourceData instanceof NumericDataset || sourceData instanceof ExpressionProfile) && !(col instanceof SequenceCollection)) throw new ExecutionError("'"+subsetName+"' is not a Sequence Collection",task.getLineNumber());
            if (sourceData instanceof MotifNumericMap && !(col instanceof MotifCollection)) throw new ExecutionError("'"+subsetName+"' is not a Motif Collection",task.getLineNumber());
            if (sourceData instanceof ModuleNumericMap && !(col instanceof ModuleCollection)) throw new ExecutionError("'"+subsetName+"' is not a Module Collection",task.getLineNumber());
            if (!(col instanceof DataCollection)) throw new ExecutionError(subsetName+" is not a data collection",task.getLineNumber());
            subsetCollection=(DataCollection)col;
        }
        task.setParameter(ArithmeticOperation.DATA_COLLECTION, subsetCollection);

        if ( isSpecialValue(aboveString) || isSpecialValue(belowString) || ((String)task.getParameter(CUTOFF_THRESHOLD_STRING)).contains("%"))  { // find min and max for dataset and collection (sequence min/max will be dynamically calculated)
            int numberOfSequences=engine.getDefaultSequenceCollection().size();
            double datasetMin=(numberOfSequences==0)?0:Double.MAX_VALUE;
            double datasetMax=(numberOfSequences==0)?0:-Double.MAX_VALUE;
            double collectionMin=(subsetCollection==null || ((DataCollection)subsetCollection).isEmpty())?0:Double.MAX_VALUE;
            double collectionMax=(subsetCollection==null || ((DataCollection)subsetCollection).isEmpty())?0:-Double.MAX_VALUE;
            Data sourcedata=(Data)task.getSourceData();
            if (sourcedata instanceof FeatureDataset) {
                FeatureDataset sourcedataset=(FeatureDataset)sourcedata;
                for (int i=0;i<sourcedataset.getSize();i++) {
                    FeatureSequenceData sequenceData = (FeatureSequenceData)sourcedataset.getSequenceByIndex(i);
                    String sequenceName=sequenceData.getName();
                    double[] values=new double[]{0,0};
                    if (sequenceData instanceof NumericSequenceData) values=((NumericSequenceData)sequenceData).getMinMaxFromData();
                    else if (sequenceData instanceof RegionSequenceData) values=((RegionSequenceData)sequenceData).getMinMaxFromData();
                    if (values[0]<datasetMin) datasetMin=values[0];
                    if (values[1]>datasetMax) datasetMax=values[1];
                    if (subsetCollection!=null && ((DataCollection)subsetCollection).contains(sequenceName)) {
                        if (values[0]<collectionMin) collectionMin=values[0];
                        if (values[1]>collectionMax) collectionMax=values[1];
                    }
                }
                if (subsetCollection==null) { 
                    collectionMin=datasetMin;
                    collectionMax=datasetMax;
                }                
            } else if (sourcedata instanceof ExpressionProfile) {
                // sequenceMin&Max will be dynamically decided later on...  
                datasetMin=((ExpressionProfile)sourcedata).getMinValue();
                datasetMax=((ExpressionProfile)sourcedata).getMaxValue();
                if (subsetCollection!=null) {
                   for (String seqname:((DataCollection)subsetCollection).getValues()) {
                      double min=((ExpressionProfile)sourcedata).getMinValue(seqname);
                      double max=((ExpressionProfile)sourcedata).getMaxValue(seqname);
                      if (min<collectionMin) collectionMin=min;
                      if (max>collectionMax) collectionMax=max;
                   }
                } else {
                    collectionMin=datasetMin;
                    collectionMax=datasetMax;
                }
            } else if (sourcedata instanceof NumericMap) {
                ArrayList<String> keys=((NumericMap)sourcedata).getAllKeys(engine);
                datasetMin=((NumericMap)sourcedata).getMinValue(keys); // this will not consider the default value if all keys are assigned explicit values
                datasetMax=((NumericMap)sourcedata).getMaxValue(keys); // this will not consider the default value if all keys are assigned explicit values
                if (subsetCollection!=null) {
                   keys=((DataCollection)subsetCollection).getValues();
                   collectionMin=((NumericMap)sourcedata).getMinValue(keys); 
                   collectionMax=((NumericMap)sourcedata).getMaxValue(keys);
                } else {
                    collectionMin=datasetMin;
                    collectionMax=datasetMax;
                }
            }
            task.setParameter(DATASET_MIN, datasetMin);
            task.setParameter(DATASET_MAX, datasetMax);
            task.setParameter(COLLECTION_MIN, collectionMin);
            task.setParameter(COLLECTION_MAX, collectionMax);
        }
        
           
    }


   @Override
    public boolean execute(OperationTask task) throws Exception {
        String sourceDatasetName=task.getSourceDataName();
        String targetDatasetName=task.getTargetDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());          
        Data sourceData=engine.getDataItem(sourceDatasetName);
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError(sourceDatasetName+"("+sourceData.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());

        if (sourceData instanceof NumericMap) {
            NumericMap targetData=(NumericMap)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);
            DataCollection subsetCollection=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            
            ArrayList<String> keys;
            if (subsetCollection!=null) keys=subsetCollection.getValues(); // use explicit keys from collection
            else keys=targetData.getRegisteredKeys(); // or use only registered key-value pairs in this map
            int size=keys.size();
            int i=0;
            double cutoff=getCutoffValue(null, task);
            for (String key:keys) { // for each key with explicitly assigned value in the map
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                double oldValue=((NumericMap)sourceData).getValue(key);
                double aboveOrEqualValue=getAboveValue(key, task);
                double belowValue=getBelowValue(key, task);               
                targetData.setValue(key,(oldValue>=cutoff)?aboveOrEqualValue:belowValue);
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
            }    
            // threshold default value also
            double oldValue=((NumericMap)sourceData).getValue();
            double aboveOrEqualValue=getAboveValue(null, task);
            double belowValue=getBelowValue(null, task);             
            targetData.setDefaultValue((oldValue>=cutoff)?aboveOrEqualValue:belowValue);
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        }  else if (sourceData instanceof ExpressionProfile) {
            ExpressionProfile targetData=(ExpressionProfile)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);
            DataCollection seqcol=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            if (seqcol==null) seqcol=engine.getDefaultSequenceCollection();
            if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(seqcol.getName()+" is not a sequence collection",task.getLineNumber());
            SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
            int i=0;
            ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
            int size=sequences.size();
            int numExperiments=targetData.getNumberOfConditions();
            for (Sequence sequence:sequences) { // for each sequence
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                String sequenceName=sequence.getName();
                // transform one sequence in ExpressionProfile here...
                double aboveOrEqualValue=getAboveValue(sequenceName, task);
                double belowValue=getBelowValue(sequenceName, task);
                double cutoff=getCutoffValue(sequenceName, task);
                for (int j=0;j<numExperiments;j++) {
                    double oldValue=targetData.getValue(sequenceName, j);
                    targetData.setValue(sequenceName, j, (oldValue>=cutoff)?aboveOrEqualValue:belowValue);
                }
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
                Thread.yield();
            }
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        }  else if (sourceData instanceof FeatureDataset) {
            FeatureDataset sourceDataset=(FeatureDataset)sourceData;
            FeatureDataset targetDataset=(FeatureDataset)sourceDataset.clone(); // Double-buffer.
            targetDataset.setName(targetDatasetName);

            if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");

            Condition condition=(Condition)task.getParameter("where");
            if (condition!=null) condition.resolve(engine, task);

            Condition_within within=(Condition_within)task.getParameter("within");
            if (within!=null) within.resolve(engine, task);

            task.setParameter(OperationTask.SOURCE, sourceDataset);
            task.setParameter(OperationTask.TARGET, targetDataset);
            resolveParameters(task);

            DataCollection seqcol=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            if (seqcol==null) seqcol=engine.getDefaultSequenceCollection();
            if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(seqcol.getName()+" is not a sequence collection",task.getLineNumber());
            SequenceCollection sequenceCollection=(SequenceCollection)seqcol;

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
        }
        return true;
    }
   
    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String seqname=sourceSequence.getName();
        double aboveOrEqualValue=getAboveValue(seqname, task);
        double belowValue=getBelowValue(seqname, task);
        double cutoff=getCutoffValue(seqname, task);
        if (targetSequence instanceof NumericSequenceData) {
            for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                if (positionSatisfiesCondition(seqname,i,task)) {
                    double oldValue=((NumericSequenceData)targetSequence).getValueAtGenomicPosition(i);
                    ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i, (oldValue>=cutoff)?aboveOrEqualValue:belowValue);
               } // satisfies 'where'-condition
            }
        } else if (targetSequence instanceof RegionSequenceData) {
            ArrayList<Region> regions=((RegionSequenceData)targetSequence).getAllRegions();
            for (Region region:regions) {
                if (regionSatisfiesCondition(seqname,region,task)) {
                    double oldValue=region.getScore();
                    region.setScore((oldValue>=cutoff)?aboveOrEqualValue:belowValue);
               } // satisfies 'where'-condition
            }
        }

      } // end transformSequence


     private double getAboveValue(String sequencename, OperationTask task) {
        return getValueForObject(task.getParameter(ABOVE_OR_EQUAL_VALUE),sequencename,task);
     }
         
     private double getBelowValue(String sequencename, OperationTask task) {
        return getValueForObject(task.getParameter(BELOW_VALUE),sequencename,task);
     }
     
     private double getValueForObject(Object valueObject, String sequencename, OperationTask task) {
        double value=0;
        if (valueObject instanceof String) {
                 if (((String)valueObject).equals("sequence.min")) value=getSequenceMin(sequencename, task);
            else if (((String)valueObject).equals("sequence.max")) value=getSequenceMax(sequencename, task);
            else if (((String)valueObject).equals("dataset.min")) value=(Double)task.getParameter(DATASET_MIN);
            else if (((String)valueObject).equals("dataset.max")) value=(Double)task.getParameter(DATASET_MAX);
            else if (((String)valueObject).equals("collection.min")) value=(Double)task.getParameter(COLLECTION_MIN);
            else if (((String)valueObject).equals("collection.max")) value=(Double)task.getParameter(COLLECTION_MAX);
        }
        else if (valueObject instanceof NumericMap) value=(double)((NumericMap)valueObject).getValue(sequencename).doubleValue();
        else if (valueObject instanceof NumericVariable) value=(double)((NumericVariable)valueObject).getValue().doubleValue();
        return value;       
     }
     
     private double getCutoffValue(String sequencename, OperationTask task) {
        double cutoff=0;
        Object cutoffObject=task.getParameter(CUTOFF_THRESHOLD);
             if (cutoffObject instanceof SequenceNumericMap) cutoff=(double)((SequenceNumericMap)cutoffObject).getValue(sequencename).doubleValue();
        else if (cutoffObject instanceof NumericVariable) cutoff=(double)((NumericVariable)cutoffObject).getValue().doubleValue();
        String thresholdType=(String)task.getParameter(THRESHOLD_TYPE);
        if (thresholdType!=null && thresholdType.equals(THRESHOLD_TYPE_FRACTION_DATASET_RANGE)) {
            if (cutoff<0) cutoff=0;
            else if (cutoff>100) cutoff=100;
            double min=(Double)task.getParameter(DATASET_MIN);
            double max=(Double)task.getParameter(DATASET_MAX);
            double range=max-min;
            cutoff=min+range*(cutoff/100.0);
        } else if (thresholdType!=null && thresholdType.equals(THRESHOLD_TYPE_FRACTION_COLLECTION_RANGE)) {
            if (cutoff<0) cutoff=0;
            else if (cutoff>100) cutoff=100;
            double min=(Double)task.getParameter(COLLECTION_MIN);
            double max=(Double)task.getParameter(COLLECTION_MAX);
            double range=max-min;
            cutoff=min+range*(cutoff/100.0);
        } else if (thresholdType!=null && thresholdType.equals(THRESHOLD_TYPE_FRACTION_SEQUENCE_RANGE)) {
            if (cutoff<0) cutoff=0;
            else if (cutoff>100) cutoff=100;
            double min=getSequenceMin(sequencename, task);
            double max=getSequenceMax(sequencename, task);
            double range=max-min;
            cutoff=min+range*(cutoff/100.0);
        }
        return cutoff;
     }

     /** Returns the minimum value for the given sequence in the source object of the task */
     private double getSequenceMin(String sequencename, OperationTask task) {
          Data source=task.getSourceData();
          if (source instanceof FeatureDataset) {
              FeatureSequenceData dataseq=((FeatureDataset)source).getSequenceByName(sequencename);
              double[] minmax=null;
              if (dataseq instanceof NumericSequenceData) minmax=((NumericSequenceData)dataseq).getMinMaxFromData();
              else if (dataseq instanceof RegionSequenceData) minmax=((RegionSequenceData)dataseq).getMinMaxFromData();
              return minmax[0]; // minimum value
          } else if (source instanceof ExpressionProfile) {
              return ((ExpressionProfile)source).getMinValue(sequencename);
          } else if (source instanceof NumericMap) { // this should not happen, I think...
              return ((NumericMap)source).getValue(sequencename);
          } else return Double.MAX_VALUE; // this should not happen either, I think...  
     }
     
     /** Returns the maximum value for the given sequence in the source object of the task */
     private double getSequenceMax(String sequencename, OperationTask task) {
          Data source=task.getSourceData();
          if (source instanceof FeatureDataset) {
              FeatureSequenceData dataseq=((FeatureDataset)source).getSequenceByName(sequencename);
              double[] minmax=null;
              if (dataseq instanceof NumericSequenceData) minmax=((NumericSequenceData)dataseq).getMinMaxFromData();
              else if (dataseq instanceof RegionSequenceData) minmax=((RegionSequenceData)dataseq).getMinMaxFromData();
              return minmax[1]; // maximum value
          } else if (source instanceof ExpressionProfile) {
              return ((ExpressionProfile)source).getMaxValue(sequencename);
          } else if (source instanceof NumericMap) { // this should not happen, I think...
              return ((NumericMap)source).getValue(sequencename);
          } else return -Double.MAX_VALUE; // this should not happen either, I think...     
     }     
 }
