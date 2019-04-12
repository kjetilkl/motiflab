/*
 
 
 */

package motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.TaskRunner;
import motiflab.engine.data.BasicDataType;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.DataMap;
import motiflab.engine.data.ExpressionProfile;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Module;
import motiflab.engine.data.ModuleCollection;
import motiflab.engine.data.ModuleNumericMap;
import motiflab.engine.data.ModuleTextMap;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericMap;
import motiflab.engine.data.MotifNumericMap;
import motiflab.engine.data.MotifTextMap;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.TextVariable;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;


/**
 *
 * @author kjetikl
 */
public class Operation_transform extends FeatureTransformOperation {
    public static final String TRANSFORM_ARGUMENT_STRING="transformArgumentString"; // textual representation of the transform argument
    private static final String TRANSFORM_ARGUMENT_DATA="transformArgumentData"; // a data object representation of the transform argument
    public static final String TRANSFORM_NAME="transformName"; // a data object representation of the transform argument
    public static final String PROPERTY_NAME="propertyName"; // the name of the property that the operation should be applied to (if applied to Region Datasets or Sequences/Motifs/Modules). Defaults to "score" if null
    
    private static final String name="transform";
    private static final String description="Transforms each numeric value in a data object according to a chosen function";
    
    private static final int LOG_TRANSFORM=0;
    private static final int SQUARE_ROOT_TRANSFORM=2;
    private static final int CUBIC_ROOT_TRANSFORM=3;
    private static final int POWER_TRANSFORM=4;
    private static final int WAVE_TRANSFORM=5;
    private static final int ABSOLUTE_TRANSFORM=6;
    private static final int SIGNUM_TRANSFORM=7;
    private static final int RECIPROCAL_TRANSFORM=8;
    private static final int SIGMOID_TRANSFORM=9;
    private static final int MODULO_TRANSFORM=10;
    private static final int LOGX_TRANSFORM=11;
    private static final int RANDOM_TRANSFORM=12;
    private static final int GAUSSIAN_RANDOM_TRANSFORM=13;
    private static final int FLOOR_TRANSFORM=14;
    private static final int CEIL_TRANSFORM=15;
    private static final int ROUND_TRANSFORM=16;    
    private static final int LOGIT_TRANSFORM=17;    
    private static final int ODDS_TRANSFORM=18;  
    private static final int TYPE_REPLACE_TRANSFORM=19;  
    private static final int REVERSE_TRANSFORM=20;        
    public static final String TYPE_REPLACE="type-replace";
    

    private Class[] datasourcePreferences=new Class[]{NumericDataset.class, NumericVariable.class, NumericMap.class, RegionDataset.class, ExpressionProfile.class, Motif.class, Module.class, Sequence.class, MotifCollection.class, ModuleCollection.class, SequenceCollection.class};

    @Override
    public String getOperationGroup() {
        return "Transform";
    }

    public static String[] getAvailableTransforms() {
        return new String[]{"log","logX","square-root","cubic-root","power","wave","absolute","signum","reciprocal","reverse","sigmoid","modulo","random","gaussian","floor","ceil","round","logit","odds",TYPE_REPLACE};
    }

    /** Returns TRUE if the argument names an available transform */
    public static boolean isTransform(String tname) {
        for (String s:getAvailableTransforms()) {
            if (s.equals(tname)) return true;
        }
        return false;
     }

    /** Returns TRUE if the given transform takes an argument */
    public static boolean takesArgument(String tname) {
        return (tname.equals("logX") || tname.equals("power") || tname.equals("wave") || tname.equals("modulo") || tname.equals("random")|| tname.equals(TYPE_REPLACE));
    }

    private int getIndexForTransform(String tname) {
             if (tname.equals("log")) return LOG_TRANSFORM;
        else if (tname.equals("logX")) return LOGX_TRANSFORM;
        else if (tname.equals("square-root")) return SQUARE_ROOT_TRANSFORM;
        else if (tname.equals("cubic-root")) return CUBIC_ROOT_TRANSFORM;
        else if (tname.equals("power")) return POWER_TRANSFORM;
        else if (tname.equals("wave")) return WAVE_TRANSFORM;
        else if (tname.equals("absolute")) return ABSOLUTE_TRANSFORM;
        else if (tname.equals("signum")) return SIGNUM_TRANSFORM;
        else if (tname.equals("reciprocal")) return RECIPROCAL_TRANSFORM;
        else if (tname.equals("sigmoid")) return SIGMOID_TRANSFORM;
        else if (tname.equals("modulo")) return MODULO_TRANSFORM;
        else if (tname.equals("random")) return RANDOM_TRANSFORM;
        else if (tname.equals("gaussian")) return GAUSSIAN_RANDOM_TRANSFORM;
        else if (tname.equals("floor")) return FLOOR_TRANSFORM;
        else if (tname.equals("ceil")) return CEIL_TRANSFORM;
        else if (tname.equals("round")) return ROUND_TRANSFORM;        
        else if (tname.equals("logit")) return LOGIT_TRANSFORM;        
        else if (tname.equals("odds")) return ODDS_TRANSFORM;    
        else if (tname.equals(TYPE_REPLACE)) return TYPE_REPLACE_TRANSFORM;          
        else if (tname.equals("reverse")) return REVERSE_TRANSFORM;          
        else return -1;
    }

    /** Returns a default argument value for the given transform*/
    public static double getDefaultArgument(String name) {
        if (name.equals("logX") || name.equals("power")) return 2.0;
        else if (name.equals("wave")) return 10.3;
        else if (name.equals("modulo")) return 10.0;
        else if (name.equals("random")) return 1.0;
        else return 0.0;
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
    public void resolveParameters(OperationTask task) throws Exception {
        String transformname=(String)task.getParameter(TRANSFORM_NAME);        
        String transformArgumentString=(String)task.getParameter(TRANSFORM_ARGUMENT_STRING);
        Data argumentData=null;
        if (transformname.equals(TYPE_REPLACE)) {
             if (transformArgumentString!=null) {
                if (transformArgumentString.startsWith("\"") && transformArgumentString.endsWith("\"")) { // transform rules given as literal string
                    String arg=transformArgumentString.substring(1,transformArgumentString.length()-1);
                    try {
                        task.setParameter("transform_replace_map", parseReplaceMap((String)arg));
                     } catch (ExecutionError e) {
                        String msg=e.getMessage();
                        if (msg.length()>20) msg=msg.substring(0,20)+"...";
                        throw new ExecutionError("Unable to parse replacement-directive line in '"+TYPE_REPLACE+"'. Expected two expressions separated by =>, but found '"+msg+"'",task.getLineNumber());
                     }                      
                } else { // transform rules given as TextVariable?
                     argumentData=engine.getDataItem(transformArgumentString);
                     if (argumentData==null) throw new ExecutionError("Unknown data object '"+transformArgumentString+"'",task.getLineNumber());
                     else if (argumentData instanceof TextVariable) {
                         try {
                            task.setParameter("transform_replace_map", parseReplaceMap((TextVariable)argumentData));
                         } catch (ExecutionError e) {
                            String msg=e.getMessage();
                            if (msg.length()>20) msg=msg.substring(0,20)+"...";
                            throw new ExecutionError("Unable to parse replacement-directive line in '"+TYPE_REPLACE+"'. Expected two expressions separated by TAB or =>, but found '"+msg+"'",task.getLineNumber());
                         }   
                     } else if (argumentData instanceof MotifTextMap || argumentData instanceof ModuleTextMap) {
                         task.setParameter("transform_replace_map", parseReplaceMap((DataMap)argumentData,task.getEngine()));
                     } else throw new ExecutionError("Argument for '"+TYPE_REPLACE+"' transform must be a Text Variable, Motif Map or Module Map",task.getLineNumber());
                }
            } else throw new ExecutionError("Missing required argument for '"+TYPE_REPLACE+"' transform",task.getLineNumber());
        } else { // numeric transform argument (numeric constant, numeric variable or numeric map)
            if (transformArgumentString!=null) {
                argumentData=engine.getDataItem(transformArgumentString);
                if (argumentData==null) {
                    try {
                      double value=Double.parseDouble(transformArgumentString);
                      argumentData=new NumericVariable(transformArgumentString, (double)value);
                   } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+transformArgumentString+"' neither data nor numeric constant",task.getLineNumber());}
                }
                if (!(argumentData instanceof NumericVariable || argumentData instanceof NumericMap)) throw new ExecutionError("'"+transformArgumentString+"' is not a valid numeric value or numeric data object",task.getLineNumber());
            } else argumentData=new NumericVariable("argument",getDefaultArgument((String)task.getParameter(TRANSFORM_NAME)));
        }
        task.setParameter(TRANSFORM_ARGUMENT_DATA, argumentData);

        String subsetName=(String)task.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        Data subsetCollection=null;
        if (subsetName!=null && !subsetName.isEmpty()) {
            Data sourceData=task.getSourceData(); // this should have been set before resolveParameters is called
            Data col=engine.getDataItem(subsetName);
            if (col==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
            if ((sourceData instanceof SequenceNumericMap || sourceData instanceof NumericDataset || sourceData instanceof ExpressionProfile) && !(col instanceof SequenceCollection)) throw new ExecutionError("'"+subsetName+"' is not a Sequence Collection",task.getLineNumber());
            if (sourceData instanceof MotifNumericMap && !(col instanceof MotifCollection)) throw new ExecutionError("'"+subsetName+"' is not a Motif Collection",task.getLineNumber());
            if (sourceData instanceof ModuleNumericMap && !(col instanceof ModuleCollection)) throw new ExecutionError("'"+subsetName+"' is not a Module Collection",task.getLineNumber());
            if (!(col instanceof DataCollection)) throw new ExecutionError(subsetName+" is not a data collection",task.getLineNumber());
            subsetCollection=(DataCollection)col;
        }
        task.setParameter(ArithmeticOperation.DATA_COLLECTION, subsetCollection);

    }

 
    @Override
    public boolean execute(OperationTask task) throws Exception {
        String sourceDatasetName=task.getSourceDataName();
        String targetDatasetName=task.getTargetDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());          
        Data sourceData=engine.getDataItem(sourceDatasetName);
        String transformname=(String)task.getParameter(TRANSFORM_NAME);
        int transformindex=getIndexForTransform(transformname);
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError(sourceDatasetName+"("+sourceData.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());
        if (transformname.equals(TYPE_REPLACE) && !(sourceData instanceof RegionDataset)) throw new ExecutionError("The '"+TYPE_REPLACE+"' transform can only be applied to Region Datasets",task.getLineNumber());
        if (transformname.equals("reverse") && !(sourceData instanceof RegionDataset)) throw new ExecutionError("The 'reverse' transform can only be applied to Region Datasets",task.getLineNumber());
        
        if (sourceData instanceof NumericVariable) {
            task.setStatusMessage("Executing operation: "+task.getOperationName());
            NumericVariable targetData=(NumericVariable)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);
            Data argumentData=(Data)task.getParameter(TRANSFORM_ARGUMENT_DATA);
            if (!(argumentData instanceof NumericVariable)) throw new ExecutionError((String)task.getParameter(TRANSFORM_ARGUMENT_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            double argumentValue=((NumericVariable)argumentData).getValue();
            //else if (argumentData instanceof NumericMap) argumentValue=((NumericMap)argumentData).getValue(); // use default value from NumericMap
            double oldvalue=targetData.getValue(); // the target is currently a clone of source
            try {
                double newvalue=transformValue(oldvalue, argumentValue, transformindex);
                targetData.setValue(newvalue);
            } catch (Exception e) {
                //throw new ExecutionError(transformname+" can not transform values"+e.getMessage()+". "+sourceData.getName()+"="+oldvalue,task.getLineNumber());
                engine.logMessage("WARNING: "+transformname+" can not transform values: "+e.getMessage());
            }
            task.setProgress(100);
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        } else if (sourceData instanceof NumericMap) {
            NumericMap targetData=(NumericMap)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);

            DataCollection subsetCollection=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            Data argumentData=(Data)task.getParameter(TRANSFORM_ARGUMENT_DATA);
            if (!((argumentData instanceof NumericMap && argumentData.getClass()==sourceData.getClass()) || argumentData instanceof NumericVariable)) throw new ExecutionError((String)task.getParameter(TRANSFORM_ARGUMENT_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            
            ArrayList<String> keys;
            if (subsetCollection!=null) keys=subsetCollection.getValues(); // use explicit keys from collection
            else keys=targetData.getRegisteredKeys(); // or use only registered key-value pairs in this map
            int size=keys.size();
            int i=0;
            int skipped=0;
            for (String key:keys) { // for each key with explicitly assigned value in the map
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                try {
                   double argumentValue=0;
                   if (argumentData instanceof NumericVariable) argumentValue=((NumericVariable)argumentData).getValue(); 
                   else if (argumentData instanceof NumericMap) argumentValue=((NumericMap)argumentData).getValue(key);
                   double newvalue=transformValue(targetData.getValue(key), argumentValue, transformindex);
                   targetData.setValue(key,newvalue);
                } catch (Exception e) {
                    skipped++;
                }
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
            }
            if (skipped>0) {
                engine.logMessage("WARNING: "+sourceDatasetName+" contains values that can not be transformed by '"+transformname+"'. "+skipped+((skipped==1)?" entry was skipped":" entries were skipped"));
            }
            // transform defaultvalue of the NumericMap also if no subsets are specified
            if (subsetCollection==null || subsetCollection==engine.getDefaultSequenceCollection()) {
                try {
                   double argumentValue=0;
                   if (argumentData instanceof NumericVariable) argumentValue=((NumericVariable)argumentData).getValue(); 
                   else if (argumentData instanceof NumericMap) argumentValue=((NumericMap)argumentData).getValue();                    
                   double newvalue=transformValue(targetData.getValue(), argumentValue, transformindex);
                   targetData.setDefaultValue(newvalue);
                } catch (Exception e) {
                   engine.logMessage("WARNING: "+transformname+" can not transform values: "+e.getMessage()+". Skipped transform of default value");
               }
            }
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        }  else if (sourceData instanceof ExpressionProfile) {
            ExpressionProfile targetData=(ExpressionProfile)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);

            DataCollection subsetCollection=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            Data argumentData=(Data)task.getParameter(TRANSFORM_ARGUMENT_DATA);
            if (!(argumentData instanceof SequenceNumericMap || argumentData instanceof NumericVariable)) throw new ExecutionError((String)task.getParameter(TRANSFORM_ARGUMENT_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            
            ArrayList<String> sequences;
            if (subsetCollection!=null) sequences=subsetCollection.getValues(); // use explicit keys from collection
            else sequences=engine.getDefaultSequenceCollection().getAllSequenceNames();
            int size=sequences.size();
            int i=0;
            int skipped=0;
            int experiments=targetData.getNumberOfConditions();
            for (String sequence:sequences) { // 
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                double argumentValue=0;
                if (argumentData instanceof NumericVariable) argumentValue=((NumericVariable)argumentData).getValue(); 
                else if (argumentData instanceof SequenceNumericMap) argumentValue=((SequenceNumericMap)argumentData).getValue(sequence);
                for (int j=0;j<experiments;j++) {
                   try {
                     double newvalue=transformValue(targetData.getValue(sequence,j), argumentValue, transformindex);
                     targetData.setValue(sequence,j,newvalue);
                   } catch (Exception e) {
                      skipped++;
                   }
                }
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
            }
            if (skipped>0) {
                engine.logMessage("WARNING: "+sourceDatasetName+" contains values that can not be transformed by '"+transformname+"'. "+skipped+((skipped==1)?" entry was skipped":" entries were skipped"));
            }
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        } else if (sourceData instanceof BasicDataType) {
            Data targetData=(Data)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);            
            resolveParameters(task);
            String property=(String)task.getParameter(PROPERTY_NAME);
            if (property==null || property.isEmpty()) throw new ExecutionError("Missing property-name specification for operation '"+task.getOperationName()+"'");            
            Object operandData=task.getParameter(TRANSFORM_ARGUMENT_DATA);
            if (!(operandData instanceof NumericVariable || operandData instanceof NumericMap)) throw new ExecutionError((String)task.getParameter(TRANSFORM_ARGUMENT_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            if (operandData instanceof NumericMap && !((NumericMap)operandData).canContain(sourceData)) throw new ExecutionError((String)task.getParameter(TRANSFORM_ARGUMENT_STRING)+" is of a map type not applicable in this context",task.getLineNumber());
            
            Object operandValue=null;
                 if (operandData instanceof NumericMap) operandValue=((DataMap)operandData).getValue(sourceData.getName());
            else if (operandData instanceof NumericVariable) operandValue=((NumericVariable)operandData).getValue();
            else operandValue=operandData;           
            Object oldvalue=null;            
            try {oldvalue=((BasicDataType)targetData).getPropertyValue(property, engine);} catch (ExecutionError e) {}
            if (oldvalue==null) throw new ExecutionError(sourceData.getName()+" does not have a value for '"+property+"'",task.getLineNumber());
            if (!(oldvalue instanceof Number)) throw new ExecutionError("'"+property+"' is not a numeric property",task.getLineNumber());
            if (!(operandValue instanceof Number)) throw new ExecutionError("'"+property+"' is not a numeric argument",task.getLineNumber());
            double argumentValue=((Number)operandValue).doubleValue();
            double oldValue=((Number)oldvalue).doubleValue();           
            try {
                double newvalue=transformValue(oldValue, argumentValue, transformindex);
                boolean ok=((BasicDataType)targetData).setPropertyValue(property, newvalue);
                if (!ok) engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"'. Unable to change value of property '"+property+"' for '"+targetDatasetName+"'");          
            } catch (Exception e) {
                //throw new ExecutionError(transformname+" can not transform values: "+e.getMessage()+". "+sourceData.getName()+"="+oldvalue,task.getLineNumber());
                engine.logMessage("WARNING: "+transformname+" can not transform values: "+e.getMessage());
            }                               
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}            
        } else if (sourceData instanceof DataCollection) {
            task.setParameter(OperationTask.SOURCE, sourceData);
            //task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);
            String property=(String)task.getParameter(PROPERTY_NAME);  
            if (property==null || property.isEmpty()) throw new ExecutionError("Missing property-name specification for operation '"+task.getOperationName()+"'");
            ArrayList<String> keys=((DataCollection)sourceData).getValues();
            int size=keys.size();
            int i=0;
            int skipped=0;
            Object operandData=task.getParameter(TRANSFORM_ARGUMENT_DATA);   
            if (!(operandData instanceof NumericVariable || operandData instanceof NumericMap)) throw new ExecutionError((String)task.getParameter(TRANSFORM_ARGUMENT_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            if (operandData instanceof NumericMap && ((NumericMap)operandData).getMembersClass()!=((DataCollection)sourceData).getMembersClass()) throw new ExecutionError((String)task.getParameter(TRANSFORM_ARGUMENT_STRING)+" is of a map type not applicable in this context",task.getLineNumber());

            for (String key:keys) { // for each key with explicitly assigned value in the map
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Data targetData = engine.getDataItem(key).clone();
                task.addAffectedDataObject(key, targetData.getClass());
                Object operandValue=null;
                     if (operandData instanceof NumericMap) operandValue=((NumericMap)operandData).getValue(key);
                else if (operandData instanceof NumericVariable) operandValue=((NumericVariable)operandData).getValue();
                else operandValue=operandData;           
                Object oldvalue=null;            
                try {oldvalue=((BasicDataType)targetData).getPropertyValue(property, engine);} catch (ExecutionError e) {}
                if (oldvalue==null) {skipped++;}
                else {
                    if (!(oldvalue instanceof Number)) throw new ExecutionError("'"+property+"' is not a numeric property",task.getLineNumber());
                    if (!(operandValue instanceof Number)) throw new ExecutionError("'"+task.getParameter(TRANSFORM_ARGUMENT_STRING)+"' is not a numeric argument",task.getLineNumber());
                    double argumentValue=((Number)operandValue).doubleValue();
                    double oldValue=((Number)oldvalue).doubleValue();                                                      
                    try {
                       double newvalue=transformValue(oldValue, argumentValue, transformindex);
                       boolean ok=((BasicDataType)targetData).setPropertyValue(property, newvalue);
                       if (!ok) skipped++;
                    } catch (Exception e) {
                        skipped++;
                    }
                    try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}                               
                }
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
            } // end for each map entry
            if (skipped>0) {
                engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"', "+skipped+((skipped==1)?" collection entry was skipped":" collection entries were skipped")+" in "+sourceDatasetName+" because the property '"+property+"' could not be updated");
            }                
        } else if (sourceData instanceof FeatureDataset) {
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

            if (engine.getClient() instanceof motiflab.gui.MotifLabGUI && !targetDatasetName.equals(sourceDatasetName) && !engine.dataExists(targetDatasetName, null)) { // a small hack to copy visualization settings from source when creating a new target
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
        if (sourceSequence instanceof NumericSequenceData) transformNumericSequence((NumericSequenceData)sourceSequence, (NumericSequenceData)targetSequence, task);
        else if (sourceSequence instanceof RegionSequenceData) transformRegionSequence((RegionSequenceData)sourceSequence, (RegionSequenceData)targetSequence, task);
        else throw new ExecutionError("Source sequence not from Numeric or Region Dataset");
    }


    private void transformNumericSequence(NumericSequenceData sourceSequence, NumericSequenceData targetSequence, OperationTask task) throws Exception {
        String transformName=(String)task.getParameter(TRANSFORM_NAME);
        int transformindex=getIndexForTransform(transformName);
        Data argumentData=(Data)task.getParameter(TRANSFORM_ARGUMENT_DATA);
        String seqname=sourceSequence.getName();
        int skipped=0; String cause=null;
        int sequenceStart=sourceSequence.getRegionStart();
        int sequenceEnd=sourceSequence.getRegionEnd();
        double argumentValue=0;
        if (argumentData instanceof NumericVariable) {
           argumentValue=((NumericVariable)argumentData).getValue();
        } else if (argumentData instanceof SequenceNumericMap) {                 
          argumentValue=((SequenceNumericMap)argumentData).getValue(seqname);
        } else {
           throw new ExecutionError(argumentData.getName()+" is of a type not applicable in this context",task.getLineNumber());
        }
        for (int i=sequenceStart;i<=sequenceEnd;i++) {
            if (positionSatisfiesCondition(seqname,i,task)) {
               double oldvalue=sourceSequence.getValueAtGenomicPosition(i);
               try {
                  double newvalue=transformValue(oldvalue,argumentValue,transformindex);
                  targetSequence.setValueAtGenomicPosition(i, newvalue);
               } catch (ExecutionError e) {skipped++;if (cause==null) cause=e.getMessage();}                              
           } // satisfies 'where'-condition
        }
        if (skipped>0) engine.logMessage("WARNING: "+seqname+" contains 'illegal' values ("+cause+"). "+skipped+((skipped==1)?" position was skipped":" positions were skipped"));

    }

    @SuppressWarnings("unchecked")
    public void transformRegionSequence(RegionSequenceData sourceSequence, RegionSequenceData targetSequence, OperationTask task) throws Exception {
        String transformName=(String)task.getParameter(TRANSFORM_NAME);
        int transformindex=getIndexForTransform(transformName);
        Data argumentData=(Data)task.getParameter(TRANSFORM_ARGUMENT_DATA);
        String propertyName=(String)task.getParameter(PROPERTY_NAME);
        if (propertyName==null || propertyName.isEmpty()) propertyName="score";
        ArrayList<String[]> replaceMap=(ArrayList<String[]>)task.getParameter("transform_replace_map");
        String seqname=sourceSequence.getName();
        int skipped=0; String cause=null;
        double argumentValue=0;
        boolean replaceType=false;
        boolean reverseRegion=false;
        if (transformName.equals(TYPE_REPLACE)) {
           replaceType=true;
        } else if (transformName.equals("reverse")) {
           reverseRegion=true;
        } else if (argumentData instanceof NumericVariable) {
           argumentValue=((NumericVariable)argumentData).getValue();
        } else if (argumentData instanceof SequenceNumericMap) {
           argumentValue=((SequenceNumericMap)argumentData).getValue(seqname);
        } else if (!(argumentData instanceof NumericMap)) {
           throw new ExecutionError(argumentData.getName()+" is of a type not applicable in this context",task.getLineNumber());
        } 
        ArrayList<Region> list = ((RegionSequenceData)targetSequence).getAllRegions();
        for (Region region:list) {
            if (regionSatisfiesCondition(seqname, region, task)) {
               if (replaceType) { // region type-replacement
                   String type=region.getType();
                   String newtype=replaceString(type, replaceMap);
                   region.setType(newtype);
               } else if (reverseRegion) { // reverse orientation of region (and binding sequence)
                   int orientation=region.getOrientation();
                   if (orientation==Region.INDETERMINED) continue;
                   else if (orientation==Region.DIRECT) region.setOrientation(Region.REVERSE);
                   else if (orientation==Region.REVERSE) region.setOrientation(Region.DIRECT);
                   String bindingsequence=region.getSequence();
                   if (bindingsequence!=null && !bindingsequence.isEmpty()) {
                       region.setSequence(MotifLabEngine.reverseSequence(bindingsequence));  // binding sequence is the actual relative sequence, not the direct strand sequence
                   }                   
               } else { // numeric transform on region score or other property
                    if (argumentData instanceof MotifNumericMap || argumentData instanceof ModuleNumericMap) {
                      String type=region.getType();
                      if (type!=null) argumentValue=((NumericMap)argumentData).getValue(type);
                      else argumentValue=((NumericMap)argumentData).getValue();
                   } 
                   double oldvalue=0;
                   Object propertyValue=region.getProperty(propertyName);
                   if (propertyValue==null) continue; // just ignore this
                   if (propertyValue instanceof Number) oldvalue=((Number)propertyValue).doubleValue();
                   else throw new ExecutionError("'"+propertyName+"' is not a numeric property",task.getLineNumber());
                   try {
                      double newvalue=transformValue(oldvalue,argumentValue,transformindex);
                      region.setProperty(propertyName,newvalue);
                   } catch (ExecutionError e) {skipped++;if (cause==null) cause=e.getMessage();}
               }
             }
        } // end: for each region
        if (skipped>0) {
            engine.logMessage("WARNING: Some regions in "+seqname+" contain 'illegal' values ("+cause+"). "+skipped+((skipped==1)?" region was skipped":" regions were skipped"));
        }
    }


    private double transformValue(double value, double argument, int transform) throws ExecutionError {
        switch (transform) {
            case LOG_TRANSFORM:  if (value>0) return Math.log(value); else throw new ExecutionError("<=0");
            case LOGX_TRANSFORM: if (value>0) return (Math.log(value)/Math.log(argument)); else throw new ExecutionError("<=0");
            case SQUARE_ROOT_TRANSFORM: if (value>=0) return Math.sqrt(value); else throw new ExecutionError("<0");
            case CUBIC_ROOT_TRANSFORM:  if (value>=0) return Math.cbrt(value); else throw new ExecutionError("<0");
            case POWER_TRANSFORM:    return Math.pow(value, argument);
            case WAVE_TRANSFORM:     return Math.cos((2*Math.PI*value)/argument);
            case ABSOLUTE_TRANSFORM: return Math.abs(value);
            case SIGNUM_TRANSFORM:   return Math.signum(value);
            case MODULO_TRANSFORM:   return value%argument;
            case RECIPROCAL_TRANSFORM: if (value!=0) return 1.0/value; else throw new ExecutionError("=0");
            case SIGMOID_TRANSFORM:  return 1.0/(double)(1+Math.exp(-value));
            case RANDOM_TRANSFORM:   return Math.random()*argument;
            case GAUSSIAN_RANDOM_TRANSFORM: return MotifLabEngine.getRandomNumberGenerator().nextGaussian();
            case FLOOR_TRANSFORM: return Math.floor(value);
            case CEIL_TRANSFORM: return Math.ceil(value);
            case ROUND_TRANSFORM: return Math.round(value);
            case LOGIT_TRANSFORM: if (value!=1) return Math.log(value/(1.0-value)); else throw new ExecutionError("=1");
            case ODDS_TRANSFORM: if (value!=1) return (value/(1.0-value)); else throw new ExecutionError("=1");             
            default:return value; // no transform
        }
    }
    
    // code for nRoot by Erik Price. Retrieved from http://www.dreamincode.net/code/snippet4611.htm
//    private double nRoot(int n, double num, double epsilon) {
//        //if you weren't sure, epsilon is the precision
//        int ctr = 0;
//        double root = 1;
//        if(n <= 0)
//                return Double.longBitsToDouble(0x7ff8000000000000L);
//        //0x7ff8000000000000L is the Java constant for NaN (Not-a-Number)
//        if(num == 0) return 0; //this step is just to reduce the needed iterations              
//        while((Math.abs(Math.pow(root, n) - num) > epsilon) && (ctr++ < 1000)) //checks if the number is good enough
//        {
//                root = ((1.0/n)*(((n-1.0)*root)+(num/Math.pow(root, n-1.0))));
//        }
//        return root;
//    }

    private ArrayList<String[]> parseReplaceMap(TextVariable text) throws ExecutionError {
        ArrayList<String[]> map = new ArrayList<String[]>(text.size());
        for (String line:text.getAllStrings()) {
            if (line.trim().isEmpty()) continue;
            String[] mapline=line.split("\t");
            if (mapline.length!=2) { // no TAB. Try to split on => instead 
                mapline=line.split("=>");
                if (mapline.length!=2) throw new ExecutionError(line); // still no success...
            }
            map.add(mapline);
        }
        return map;
    }
    private ArrayList<String[]> parseReplaceMap(DataMap datamap, MotifLabEngine engine) throws ExecutionError {
        ArrayList<String> keys=datamap.getAllKeys(engine);
        ArrayList<String[]> map = new ArrayList<String[]>(keys.size());
        for (String key:keys) {
            map.add(new String[]{key,datamap.getValue(key).toString()});
        }
        return map;
    }    
    private ArrayList<String[]> parseReplaceMap(String text) throws ExecutionError {
        String[] lines=text.trim().split("\\s*,\\s*");
        ArrayList<String[]> map = new ArrayList<String[]>(lines.length);
        for (String line:lines) {
            if (line.trim().isEmpty()) continue;
            String[] mapline=line.split("=>");
            if (mapline.length==2) map.add(mapline);
            else throw new ExecutionError(line);           
        }
        return map;
    }    
    
    
    private String replaceString(String key,ArrayList<String[]>map) {
        for (String[] mapline:map) {
            if (key.matches(mapline[0])) return key.replaceAll(mapline[0], mapline[1]);
        }
        return key; // no applicable transform found
    }
}



