/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.BasicDataType;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.*;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public abstract class ArithmeticOperation extends FeatureTransformOperation {
    public static final String OPERAND_STRING="operandString"; // textual representation of the operand-value parameter
    public static final String OPERAND_DATA="operandData"; // an object representation of the operand-value parameter
    public static final String REGION_OPERATOR="regionOperator"; // textual representation of the operator used when applying the operation to Region Datasets and Numeric datasets are the second operand. The operator specifies which value to used based on the values falling within the region in the numeric dataset.
    public static final String DATA_COLLECTION_NAME="dataCollectionName"; // the name of a data collection (sequence, motif or module) that the operation should be limited to (when applied to Numeric Maps)
    public static final String DATA_COLLECTION="dataCollectionData"; // the data collection (sequence, motif or module) that the operation should be limited to (when applied to Numeric Maps)
    public static final String PROPERTY_NAME="propertyName"; // the name of the property that the operation should be applied to (if applied to Region Datasets or Sequences/Motifs/Modules). Defaults to "score" if null

    @Override
    public String getOperationGroup() {
        return "Transform";
    }

    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        String sourceName=task.getSourceDataName();
        Data sourceData=task.getSourceData(); // this should have been set before resolveParameters is called
        String operandDataString=(String)task.getParameter(OPERAND_STRING);
        
        Object operandData=null;
             if (operandDataString.equalsIgnoreCase("TRUE") || operandDataString.equalsIgnoreCase("YES")) operandData=Boolean.TRUE;
        else if (operandDataString.equalsIgnoreCase("FALSE") || operandDataString.equalsIgnoreCase("NO")) operandData=Boolean.FALSE;
        else if (operandDataString.startsWith("\"")) {
              String stripped=operandDataString.substring(1,operandDataString.length()-1);
              if (stripped.contains(",")) {
                String[] parts=stripped.trim().split("\\s*,\\s*");
                operandData=new ArrayList<String>(parts.length);
                ((ArrayList)operandData).addAll(Arrays.asList(parts));
              } else operandData=stripped.trim();
        } else if (operandDataString.startsWith("region[") && operandDataString.endsWith("]")) {
            String propertyName=operandDataString.substring("region[".length(),operandDataString.length()-1);
            if (propertyName.startsWith("\"") && propertyName.endsWith("\"")) propertyName=propertyName.substring(1,propertyName.length()-1);
            if (!(sourceData instanceof RegionDataset)) throw new ExecutionError(sourceData.getName()+" has no region property named '"+propertyName+"'");            
            operandData=new Object[]{propertyName}; // this is just a tagging array         
        } else { // the value should now be a data item or numeric literal
            operandData=engine.getDataItem(operandDataString);
            if (operandData==null) {
                try {
                  double value=Double.parseDouble(operandDataString);
                  operandData=new NumericVariable(operandDataString, (double)value);
               } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandDataString+"' is neither a data object nor a numeric constant or text enclosed in double quotes",task.getLineNumber());}
            }
        }
        if (sourceData instanceof RegionDataset) { // allow numeric types or Text Variable
             if (!(operandData instanceof Object[] || operandData instanceof NumericVariable || operandData instanceof DataMap || operandData instanceof NumericDataset || operandData instanceof String || operandData instanceof TextVariable || operandData instanceof Boolean || operandData instanceof ArrayList)) throw new ExecutionError("'"+operandDataString+"' is not a valid numeric, text or boolean value",task.getLineNumber());
        } else if (sourceData instanceof DataCollection || sourceData instanceof Motif || sourceData instanceof ModuleCRM || sourceData instanceof Sequence) { //`
             if (!(operandData instanceof NumericVariable || operandData instanceof DataMap || operandData instanceof String || operandData instanceof TextVariable || operandData instanceof Boolean || operandData instanceof ArrayList)) throw new ExecutionError("'"+operandDataString+"' is not a valid numeric, text or boolean value",task.getLineNumber());
        } else if (sourceData instanceof NumericMap) { //`
             if (!(operandData instanceof NumericVariable || operandData instanceof NumericMap)) throw new ExecutionError("'"+operandDataString+"' is not a valid numeric value",task.getLineNumber());
        } else if (sourceData instanceof TextMap) { //`
             if (!(operandData instanceof NumericVariable || operandData instanceof DataMap || operandData instanceof String || operandData instanceof TextVariable || operandData instanceof Boolean || operandData instanceof ArrayList)) throw new ExecutionError("'"+operandDataString+"' is not a valid numeric, text or boolean value",task.getLineNumber());
        } else { // allow only numeric types and (cardinality of) Collections
             if (!(operandData instanceof NumericVariable || operandData instanceof NumericMap || operandData instanceof NumericDataset || operandData instanceof DataCollection)) throw new ExecutionError("'"+operandDataString+"' is not a valid numeric value",task.getLineNumber());
        }
        if (operandData instanceof TextVariable) {// if the operandData is a TextVariable, convert it to a single String (if just one line) or to list (if multiple lines).
            ArrayList<String> strings=((TextVariable)operandData).getAllStrings();
            if (strings.size()==1) operandData=strings.get(0);
            else operandData=strings.clone();
        }
        task.setParameter(OPERAND_DATA, operandData);

        String subsetName=(String)task.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        Data subsetCollection=null;
        if (subsetName!=null && !subsetName.isEmpty()) {
            Data col=engine.getDataItem(subsetName);
            if (col==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
            if ((sourceData instanceof SequenceNumericMap || sourceData instanceof SequenceTextMap || sourceData instanceof FeatureDataset || sourceData instanceof ExpressionProfile) && !(col instanceof SequenceCollection)) throw new ExecutionError("'"+subsetName+"' is not a Sequence Collection",task.getLineNumber());
            if ((sourceData instanceof MotifNumericMap || sourceData instanceof MotifTextMap)  && !(col instanceof MotifCollection))  throw new ExecutionError("'"+subsetName+"' is not a Motif Collection",task.getLineNumber());
            if ((sourceData instanceof ModuleNumericMap|| sourceData instanceof ModuleTextMap) && !(col instanceof ModuleCollection)) throw new ExecutionError("'"+subsetName+"' is not a Module Collection",task.getLineNumber());
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
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError(sourceDatasetName+"("+sourceData.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());

        if (sourceData instanceof NumericVariable) {
            task.setStatusMessage("Executing operation: "+task.getOperationName());
            NumericVariable targetData=(NumericVariable)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);
            Object operandData=task.getParameter(OPERAND_DATA);
            if (!(operandData instanceof NumericVariable || operandData instanceof NumericMap || operandData instanceof DataCollection)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            double operandValue=0;
                 if (operandData instanceof NumericVariable) operandValue=((NumericVariable)operandData).getValue();
            else if (operandData instanceof NumericMap) operandValue=((NumericMap)operandData).getValue(); // use the default value in NumericMap
            else if (operandData instanceof DataCollection) operandValue=((DataCollection)operandData).size(); // set cardinality
            try {
               double newvalue=calculateNewValue(targetData.getValue(), operandValue);
               targetData.setValue(newvalue);
            } catch (Exception e) {
                 engine.logMessage("WARNING: Unable to perform operation '"+task.getOperationName()+"' on '"+sourceDatasetName+"' because of : "+e.getMessage());            
            }
            task.setProgress(100);
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        } else if (sourceData instanceof NumericMap) {
            NumericMap targetData=(NumericMap)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);

            Object operandData=(Data)task.getParameter(OPERAND_DATA);
            DataCollection subsetCollection=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            if (!(operandData instanceof NumericVariable || operandData instanceof NumericMap || operandData instanceof DataCollection)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            
            ArrayList<String> keys;
            if (subsetCollection!=null) keys=subsetCollection.getValues(); // use explicit keys from collection
            else keys=targetData.getAllKeys(engine); // or use all keys!
            // else keys=targetData.getRegisteredKeys(); // or use only registered key-value pairs in this map
            int size=keys.size();
            int i=0;
            int skipped=0;
            String exceptionMsg=null;
            for (String key:keys) { // for each key with explicitly assigned value in the map
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                double operandValue=0;
                if (operandData instanceof NumericMap) operandValue=((NumericMap)operandData).getValue(key);
                else if (operandData instanceof DataCollection) operandValue=((DataCollection)operandData).size();
                else operandValue=((NumericVariable)operandData).getValue();
                try {
                   double newvalue=calculateNewValue(targetData.getValue(key), operandValue);
                   targetData.setValue(key,newvalue);
                } catch (Exception e) {
                    skipped++;
                    if (exceptionMsg==null) exceptionMsg=e.getMessage();
                }
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
            }
            if (skipped>0) {
                engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"', "+skipped+((skipped==1)?" map entry was skipped":" map entries were skipped")+" in "+sourceDatasetName+" because of : "+exceptionMsg);
            }
            // transform defaultvalue of the NumericMap also if no subsets are specified
            if (subsetCollection==null || subsetCollection==engine.getDefaultSequenceCollection()) {
                double operandValue=0;
                if (operandData instanceof NumericMap) operandValue=((NumericMap)operandData).getValue();
                else if (operandData instanceof DataCollection) operandValue=((DataCollection)operandData).size();
                else operandValue=((NumericVariable)operandData).getValue();
                try {
                   double newvalue=calculateNewValue(targetData.getValue(), operandValue);
                   targetData.setDefaultValue(newvalue);
                } catch (Exception e) {
                  engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"', new default value could not be calculated because of : "+e.getMessage());
                }
            }
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        } else if (sourceData instanceof TextMap) {
            TextMap targetData=(TextMap)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);

            Object operandData=task.getParameter(OPERAND_DATA);
            DataCollection subsetCollection=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            if (!(operandData instanceof NumericVariable || operandData instanceof TextVariable || operandData instanceof DataMap || operandData instanceof String || operandData instanceof Boolean || operandData instanceof ArrayList)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            
            ArrayList<String> keys;
            if (subsetCollection!=null) keys=subsetCollection.getValues(); // use explicit keys from collection
            else keys=targetData.getAllKeys(engine); // or use all keys!
            // else keys=targetData.getRegisteredKeys(); // or use only registered key-value pairs in this map
            int size=keys.size();
            int i=0;
            int skipped=0;
            String exceptionMsg=null;
            for (String key:keys) { // for each key with explicitly assigned value in the map
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Object operandValue=null;
                if (operandData instanceof DataMap) operandValue=((DataMap)operandData).getValue(key);
                else if (operandData instanceof DataCollection) operandValue=((DataCollection)operandData).size();
                else if (operandData instanceof TextVariable) operandValue=((TextVariable)operandData).getValue();
                else if (operandData instanceof NumericVariable) operandValue=((NumericVariable)operandData).getValue();
                else operandValue=(operandData!=null)?operandData.toString():null; //
                try {
                   Object newvalue=calculateNewPropertyValue(targetData.getValue(key), operandValue);
                   if (newvalue instanceof ArrayList) newvalue=MotifLabEngine.splice((ArrayList)newvalue, ",");
                   targetData.setValue(key,newvalue.toString());
                } catch (Exception e) {
                    skipped++;
                    if (exceptionMsg==null) exceptionMsg=e.getMessage();
                }
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
            }
            if (skipped>0) {
                engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"', "+skipped+((skipped==1)?" map entry was skipped":" map entries were skipped")+" in "+sourceDatasetName+" because of : "+exceptionMsg);
            }
            // transform defaultvalue of the NumericMap also if no subsets are specified
            if (subsetCollection==null || subsetCollection==engine.getDefaultSequenceCollection()) {
                Object operandValue=null;
                if (operandData instanceof DataMap) operandValue=((DataMap)operandData).getValue();
                else if (operandData instanceof DataCollection) operandValue=((DataCollection)operandData).size();
                else if (operandData instanceof TextVariable) operandValue=((TextVariable)operandData).getValue();
                else if (operandData instanceof NumericVariable) operandValue=((NumericVariable)operandData).getValue();
                else operandValue=(operandData!=null)?operandData.toString():null;
                try {
                   Object newvalue=calculateNewPropertyValue(targetData.getValue(), operandValue);
                   if (newvalue instanceof ArrayList) newvalue=MotifLabEngine.splice((ArrayList)newvalue, ",");
                   targetData.setDefaultValue(newvalue.toString());
                } catch (Exception e) {
                  engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"', new default value could not be calculated because of : "+e.getMessage());
                }
            }
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        } else if (sourceData instanceof ExpressionProfile) {
            ExpressionProfile targetData=(ExpressionProfile)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);

            Object operandData=(Data)task.getParameter(OPERAND_DATA);
            DataCollection subsetCollection=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            if (subsetCollection!=null && !(subsetCollection instanceof SequenceCollection)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is not a Sequence Collection",task.getLineNumber());
            if (subsetCollection==null) subsetCollection=engine.getDefaultSequenceCollection();
            ArrayList<String> sequenceNames=((SequenceCollection)subsetCollection).getAllSequenceNames();
            if (!(operandData instanceof NumericVariable || operandData instanceof SequenceNumericMap || operandData instanceof DataCollection)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            
            int size=targetData.getNumberOfConditions();
            int skipped=0;
            String exceptionMsg=null;
            for (int i=0;i<size;i++) { // for each experiment
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                for (String sequenceName:sequenceNames) {
                    Double oldValue=targetData.getValue(sequenceName, i);
                    if (oldValue==null) continue;
                    double operandValue=0;
                    if (operandData instanceof SequenceNumericMap) operandValue=((SequenceNumericMap)operandData).getValue(sequenceName);
                    else if (operandData instanceof DataCollection) operandValue=((DataCollection)operandData).size();
                    else operandValue=((NumericVariable)operandData).getValue();
                    try {
                       double newvalue=calculateNewValue(oldValue.doubleValue(), operandValue);
                       targetData.setValue(sequenceName,i,newvalue);
                    } catch (Exception e) {
                        skipped++;
                        if (exceptionMsg==null) exceptionMsg=e.getMessage();
                    }                   
                }
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
            }
            if (skipped>0) {
                  engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"', "+skipped+((skipped==1)?" map entry was skipped":" map entries were skipped")+" in "+sourceDatasetName+" because of : "+exceptionMsg);
            }
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        } else if (sourceData instanceof BasicDataType) { // apply arithmetic operation to a property of Motif, ModuleCRM or Sequence
            Data targetData=(Data)sourceData.clone();
            targetData.rename(targetDatasetName);
            task.setParameter(OperationTask.SOURCE, sourceData);
            task.setParameter(OperationTask.TARGET, targetData);            
            resolveParameters(task);
            String property=(String)task.getParameter(PROPERTY_NAME);
            if (property==null || property.isEmpty()) throw new ExecutionError("Missing property-name specification for operation '"+task.getOperationName()+"'");            
            Object operandData=task.getParameter(OPERAND_DATA);
            if (!(operandData instanceof NumericVariable || operandData instanceof TextVariable || operandData instanceof TextMap || operandData instanceof NumericMap || operandData instanceof String || operandData instanceof Boolean || operandData instanceof ArrayList)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            if (operandData instanceof DataMap && !((DataMap)operandData).canContain(sourceData)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a map type not applicable in this context",task.getLineNumber());
            Object operandValue=null;
                 if (operandData instanceof DataMap) operandValue=((DataMap)operandData).getValue(sourceData.getName());
            else if (operandData instanceof NumericVariable) operandValue=((NumericVariable)operandData).getValue();
            else if (operandData instanceof TextVariable) operandValue=((TextVariable)operandData).getValue();
            else operandValue=operandData;           
            Object oldvalue=null;
            try {oldvalue=((BasicDataType)targetData).getPropertyValue(property, engine);} catch (ExecutionError e) {}
            Object newvalue=calculateNewPropertyValue(oldvalue, operandValue);                                  
            boolean ok=((BasicDataType)targetData).setPropertyValue(property, newvalue);
            if (!ok) engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"'. Unable to change value of property '"+property+"' for '"+targetDatasetName+"'");
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}            
        } else if (sourceData instanceof DataCollection) { // apply arithmetic operation to a property of Motif, ModuleCRM or Sequence for every entry in collection (this does not update the collection itself, only its members)
            task.setParameter(OperationTask.SOURCE, sourceData);
            //task.setParameter(OperationTask.TARGET, targetData);
            resolveParameters(task);
            String property=(String)task.getParameter(PROPERTY_NAME);  
            if (property==null || property.isEmpty()) throw new ExecutionError("Missing property-name specification for operation '"+task.getOperationName()+"'");
            ArrayList<String> keys=((DataCollection)sourceData).getValues();
            int size=keys.size();
            int i=0;
            int skipped=0;
            Object operandData=task.getParameter(OPERAND_DATA);   
            if (!(operandData instanceof NumericVariable || operandData instanceof TextVariable || operandData instanceof TextMap || operandData instanceof NumericMap || operandData instanceof String || operandData instanceof Boolean || operandData instanceof ArrayList)) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a type not applicable in this context",task.getLineNumber());
            if (operandData instanceof DataMap && ((DataMap)operandData).getMembersClass()!=((DataCollection)sourceData).getMembersClass()) throw new ExecutionError((String)task.getParameter(OPERAND_STRING)+" is of a map type not applicable in this context",task.getLineNumber());

            for (String key:keys) { // for each key with explicitly assigned value in the map
                task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                Data targetData = engine.getDataItem(key).clone();
                task.addAffectedDataObject(key, targetData.getClass());
                Object operandValue=null;
                     if (operandData instanceof DataMap) operandValue=((DataMap)operandData).getValue(key);
                else if (operandData instanceof NumericVariable) operandValue=((NumericVariable)operandData).getValue();
                else if (operandData instanceof TextVariable) operandValue=((TextVariable)operandData).getValue();
                else operandValue=operandData;                     
                Object oldvalue=null;
                try {oldvalue=((BasicDataType)targetData).getPropertyValue(property, engine);} catch (ExecutionError e) {}
                Object newvalue=calculateNewPropertyValue(oldvalue, operandValue);                                                     
                try {
                   boolean ok=((BasicDataType)targetData).setPropertyValue(property, newvalue);
                   if (!ok) skipped++;
                } catch (Exception e) {
                    skipped++;
                }
                try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}                               
                task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
                task.setProgress(i+1, size);
                i++;
            } // end for each map entry
            if (skipped>0) {
                engine.logMessage("WARNING: While performing operation '"+task.getOperationName()+"', "+skipped+((skipped==1)?" collection entry was skipped":" collection entries were skipped")+" in "+sourceDatasetName+" because the property '"+property+"' could not be updated");
            }                
        } else if (sourceData instanceof FeatureDataset) {
            FeatureDataset sourceDataset=(FeatureDataset)sourceData;
            if (!(sourceDataset instanceof NumericDataset || sourceData instanceof RegionDataset)) throw new ExecutionError("Source dataset not Numeric or Region Dataset");
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
            String propertyName=(sourceDataset instanceof RegionDataset)?(String)task.getParameter(ArithmeticOperation.PROPERTY_NAME):null;
            if (sourceDataset instanceof RegionDataset && propertyName==null) {
                propertyName="score";
                task.setParameter(ArithmeticOperation.PROPERTY_NAME,propertyName);
            }
            DataCollection seqcol=(DataCollection)task.getParameter(ArithmeticOperation.DATA_COLLECTION);
            if (seqcol==null) seqcol=engine.getDefaultSequenceCollection();
            if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(seqcol.getName()+" is not a sequence collection",task.getLineNumber());
            SequenceCollection sequenceCollection=(SequenceCollection)seqcol;

            int i=0;
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
            // if (threadpool!=null) threadpool.shutdownNow();        
            if (countOK!=sequences.size()) {
                throw new ExecutionError("Some mysterious error occurred while scanning");
            }            

            if (targetDataset instanceof NumericDataset) ((NumericDataset)targetDataset).updateAllowedMinMaxValuesFromData();
            if (targetDataset instanceof RegionDataset && propertyName.equals("score")) ((RegionDataset)targetDataset).updateMaxScoreValueFromData();
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
             if (sourceSequence instanceof NumericSequenceData) transformNumericSequence((NumericSequenceData)sourceSequence, (NumericSequenceData)targetSequence, task);
        else if (sourceSequence instanceof RegionSequenceData)  transformRegionSequence((RegionSequenceData)sourceSequence, (RegionSequenceData)targetSequence, task);
    }


    private void transformNumericSequence(NumericSequenceData sourceSequence, NumericSequenceData targetSequence, OperationTask task) throws Exception {
        Data operandData=(Data)task.getParameter(OPERAND_DATA);
        Data operand;
        if (operandData instanceof NumericDataset) operand=((NumericDataset)operandData).getSequenceByName(sourceSequence.getName());
        else operand=operandData;
        String seqname=sourceSequence.getName();
        String exceptionMsg=null;
        int skipped=0;
        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
            if (positionSatisfiesCondition(seqname,i,task)) {
              double operandValue=0;
              if (operand instanceof NumericConstant) {
                  operandValue=((NumericConstant)operand).getValue();
              } else if (operand instanceof SequenceNumericMap) {
                  operandValue=((SequenceNumericMap)operand).getValue(seqname);
              } else if (operand instanceof NumericVariable) {
                  operandValue=((NumericVariable)operand).getValue();
              } else if (operand instanceof NumericSequenceData) {
                  operandValue=((NumericSequenceData)operand).getValueAtGenomicPosition(i);
              } else {
                  throw new ExecutionError(operand.getName()+" is of a type not applicable in this context");
              }
              //engine.logMessage("operandValue="+operandValue+"  operandData="+operandData.toString()+" (class)="+operandData.getClass().toString());
              double oldvalue=sourceSequence.getValueAtGenomicPosition(i);
              try {
                  double newvalue=calculateNewValue(oldvalue,operandValue);
                  targetSequence.setValueAtGenomicPosition(i, newvalue);
              } catch (ExecutionError e) {
                  skipped++;
                  if (exceptionMsg==null) exceptionMsg=e.getMessage();
              }
           } // satisfies 'where'-condition
        }
        if (skipped>0) {
            engine.logMessage("WARNING: "+skipped+((skipped==1)?" position was skipped":" positions were skipped")+" in "+seqname+" because of : "+exceptionMsg);
        }
    }

    private void transformRegionSequence(RegionSequenceData sourceSequence, RegionSequenceData targetSequence, OperationTask task) throws Exception {
        Object operandData=task.getParameter(OPERAND_DATA); // The object containing the "target value" (which will be assigned to the specified property). This can be a data object, a boolean, a string, an ArrayList<String> or an Object[] (the last one is a region property marker)
        String operator=(String)task.getParameter(REGION_OPERATOR);
        String property=(String)task.getParameter(PROPERTY_NAME);
        Object operand;
        if (operandData instanceof NumericDataset) operand=((NumericDataset)operandData).getSequenceByName(sourceSequence.getName());
        else operand=operandData;
        String seqname=sourceSequence.getName();
        String exceptionMsg=null;
        int skipped=0;
        ArrayList<Region> list = ((RegionSequenceData)targetSequence).getAllRegions();
        for (Region region:list) {
            if (regionSatisfiesCondition(seqname, region, task)) {
              Object operandValue=null; // this is the 'new value' to use in the arithmetic operation
              if (operand instanceof NumericConstant) {
                  operandValue=((NumericConstant)operand).getValue();
              } else if (operand instanceof NumericVariable) {
                  operandValue=((NumericVariable)operand).getValue();
              } else if (operand instanceof SequenceNumericMap) {
                  operandValue=((SequenceNumericMap)operand).getValue(seqname);
              } else if (operand instanceof SequenceTextMap) {
                  operandValue=((SequenceTextMap)operand).getValue(seqname);
              } else if (operand instanceof MotifNumericMap) {
                  operandValue=((MotifNumericMap)operand).getValue(region.getType());
              } else if (operand instanceof MotifTextMap) {
                  operandValue=((MotifTextMap)operand).getValue(region.getType());
              } else if (operand instanceof ModuleNumericMap) {
                  operandValue=((ModuleNumericMap)operand).getValue(region.getType());
              } else if (operand instanceof ModuleTextMap) {
                  operandValue=((ModuleTextMap)operand).getValue(region.getType());
              } else if (operand instanceof NumericSequenceData) {
                  int relativeStart=region.getRelativeStart();
                  int relativeEnd=region.getRelativeEnd();
                       if (operator==null) throw new ExecutionError("Missing operator for Numeric Dataset value applied to region property");
                  else if (operator.equals("min") || operator.equals("minimum")) operandValue = ((NumericSequenceData) operand).getMinValueInInterval(relativeStart, relativeEnd);
                  else if (operator.equals("max") || operator.equals("maximum")) operandValue=((NumericSequenceData)operand).getMaxValueInInterval(relativeStart, relativeEnd);
                  else if (operator.equals("avg") || operator.equals("average")) operandValue=((NumericSequenceData)operand).getAverageValueInInterval(relativeStart, relativeEnd);
                  else if (operator.equals("median")) operandValue=((NumericSequenceData)operand).getMedianValueInInterval(relativeStart, relativeEnd);
                  else if (operator.equals("sum")) operandValue=((NumericSequenceData)operand).getSumValueInInterval(relativeStart, relativeEnd);
                  else if (operator.equals("weighted avg") || operator.equals("weighted average")) operandValue= getWeightedValue(operator, region, (NumericSequenceData)operand, task.getEngine());
                  else if (operator.equals("weighted sum")) operandValue=getWeightedValue(operator, region, (NumericSequenceData)operand, task.getEngine());
                  else if (operator.equals("startValue")) operandValue = ((NumericSequenceData)operand).getValueAtRelativePosition(region.getRelativeStart());
                  else if (operator.equals("endValue")) operandValue = ((NumericSequenceData)operand).getValueAtRelativePosition(region.getRelativeEnd());
                  else if (operator.equals("relativeStartValue")) operandValue=((NumericSequenceData)operand).getValueAtRelativePosition((((NumericSequenceData)operand).getStrandOrientation()==Sequence.DIRECT)?region.getRelativeStart():region.getRelativeEnd());
                  else if (operator.equals("relativeEndValue")) operandValue=((NumericSequenceData)operand).getValueAtRelativePosition((((NumericSequenceData)operand).getStrandOrientation()==Sequence.DIRECT)?region.getRelativeEnd():region.getRelativeStart());
                  else if (operator.equals("regionStartValue")) operandValue=((NumericSequenceData)operand).getValueAtRelativePosition((region.getOrientation()!=Region.REVERSE)?region.getRelativeStart():region.getRelativeEnd());
                  else if (operator.equals("regionEndValue")) operandValue=((NumericSequenceData)operand).getValueAtRelativePosition((region.getOrientation()!=Region.REVERSE)?region.getRelativeEnd():region.getRelativeStart());
                  else if (operator.equals("centerValue")) operandValue=((NumericSequenceData)operand).getValueAtRelativePosition((int)((region.getRelativeStart()+region.getRelativeEnd())/2.0));
                  else if (operator.equals("weighted min") || operator.equals("weighted minimum")) operandValue = getWeightedValue(operator, region, (NumericSequenceData)operand, task.getEngine());
                  else if (operator.equals("weighted max") || operator.equals("weighted maximum")) operandValue= getWeightedValue(operator, region, (NumericSequenceData)operand, task.getEngine());
                  else if (operator.equals("weighted median")) operandValue=getWeightedValue(operator, region, (NumericSequenceData)operand, task.getEngine());
                  else throw new ExecutionError("Unrecognized operator for Numeric Dataset value applied to region property: "+operator);
              } else if (operand instanceof Object[]) { // refers to a different object                 
                  String propertyName=(String)((Object[])operand)[0];
                  operandValue=region.getProperty(propertyName);
              } else operandValue=operand;
              //engine.logMessage("operandValue="+operandValue+"  operandData="+operandData.toString()+" (class)="+operandData.getClass().toString());
              Object oldvalue=region.getProperty(property);
              try {
                  Object newvalue=calculateNewPropertyValue(oldvalue,operandValue);
                  region.setProperty(property, newvalue);
              } catch (ExecutionError e) {
                  skipped++;
                  if (exceptionMsg==null) exceptionMsg=e.getMessage();
              }
             }
        } // end: for each region
        if (skipped>0) {
            engine.logMessage("WARNING: "+skipped+((skipped==1)?" region was skipped":" regions were skipped")+" in "+seqname+" because of : "+exceptionMsg);
        }
    }

    private double getWeightedValue(String compareProperty, Region region, NumericSequenceData numericSequence, MotifLabEngine engine) {
        String type=region.getType();
        int regionStart=region.getRelativeStart();
        int regionEnd=region.getRelativeEnd();
        Data motif=(type==null)?null:engine.getDataItem(type);
        if (motif instanceof Motif) {
           double[] weights=((Motif)motif).getICcontentForColumns(region.getOrientation()==Region.REVERSE, true);
           if (weights!=null) {
                    if (compareProperty.equals("weighted average") || compareProperty.equals("weighted avg")) return numericSequence.getWeightedAverageValueInInterval(regionStart, regionEnd, weights);
               else if (compareProperty.equals("weighted sum")) return numericSequence.getWeightedSumValueInInterval(regionStart, regionEnd, weights);
               else if (compareProperty.equals("weighted min") || compareProperty.equals("weighted minimum")) return numericSequence.getWeightedMinValueInInterval(regionStart, regionEnd, weights);
               else if (compareProperty.equals("weighted max") || compareProperty.equals("weighted maximum")) return numericSequence.getWeightedMaxValueInInterval(regionStart, regionEnd, weights);
               else if (compareProperty.equals("weighted median")) return numericSequence.getWeightedMedianValueInInterval(regionStart, regionEnd, weights);
           }
        }
        // If we have not returned yet something wrong has happened. Use non-weighted defaults
             if (compareProperty.equals("weighted average") || compareProperty.equals("weighted avg")) return numericSequence.getAverageValueInInterval(regionStart, regionEnd);
        else if (compareProperty.equals("weighted sum")) return numericSequence.getSumValueInInterval(regionStart, regionEnd);     
        else if (compareProperty.equals("weighted min") || compareProperty.equals("weighted minimum")) return numericSequence.getMinValueInInterval(regionStart, regionEnd);
        else if (compareProperty.equals("weighted max") || compareProperty.equals("weighted maximum")) return numericSequence.getMaxValueInInterval(regionStart, regionEnd);
        else if (compareProperty.equals("weighted median")) return numericSequence.getMedianValueInInterval(regionStart, regionEnd);
        else return 0;
    }      
    
    /** Calculates a new numeric value based on the present value and an additional argument value */
    public abstract double calculateNewValue(double oldvalue, double argument) throws ExecutionError;

    /** Calculates a new value based on the present value and an additional argument value. The values could be numeric, boolean or text values (or ArrayList of Strings) */
    public abstract Object calculateNewPropertyValue(Object oldvalue, Object argument) throws ExecutionError;


    /**
     * Takes an existing string containing a possibly comma-separated list of entries and merges
     * additional entries (which can be given as a single string or an ArrayList) into the list
     * The new entries are only added if they are not already in the current list.
     * The method returns a String with a comma-separated list
     * @param current
     * @param toadd
     * @return
     */
    @SuppressWarnings("unchecked")
    public String stringListAdd(Object currentValue, Object toAdd) {
        String current=null;
        if (currentValue instanceof ArrayList) current=MotifLabEngine.splice((ArrayList<String>)currentValue, ",");
        else if (currentValue!=null) current=currentValue.toString();
        
        if (toAdd==null || (toAdd instanceof String && ((String)toAdd).isEmpty())) return current;
        // the current value is now a comma-separated string. Add all these elements to a list...
        ArrayList<String> list=new ArrayList<String>();
        if (current!=null && !current.isEmpty()) {
            if (((String)current).indexOf(',')>=0) {
                String[] parts=current.split("\\s*,\\s*");
                list.addAll(Arrays.asList(parts));
            } else list.add((String)current);
        }
        // ...then add the new elements to a separate list...
        ArrayList<String> additionals=new ArrayList<String>();
        if (toAdd instanceof ArrayList) { // even though it is a list already, it can still contain comma-separated elements that should be added individually
            for (String tA:(ArrayList<String>)toAdd) {
                if (tA.indexOf(',')>=0) {
                    String[] toaddparts=tA.split("\\s*,\\s*");
                    additionals.addAll(Arrays.asList(toaddparts));
                } else additionals.add(tA);                
            }
        }
        else {
            toAdd=toAdd.toString();
            if (((String)toAdd).indexOf(',')>=0) {
                String[] toaddparts=((String)toAdd).split("\\s*,\\s*");
                additionals.addAll(Arrays.asList(toaddparts));
            } else additionals.add((String)toAdd);
        }
        // ... and merge the two lists (avoiding duplicates). Return a comma-separated string version of the list
        for (String a:additionals) {
            if (!list.contains(a)) list.add(a);
        }
        return MotifLabEngine.splice(list,",");
    }


    /**
     * Takes an existing string containing a possibly comma-separated list of entries and removes
     * given entries (which can be given as a single string or an ArrayList) from the list.
     * The method returns a String with a comma-separated list
     * @param current
     * @param toadd
     * @return
     */
    @SuppressWarnings("unchecked")
    public String stringListRemove(Object currentValue, Object toRemove) {
        String current=null;
        if (currentValue instanceof ArrayList) current=MotifLabEngine.splice((ArrayList<String>)currentValue,",");
        else if (currentValue!=null) current=currentValue.toString();
        ArrayList<String> list=new ArrayList<String>();
        if (current!=null) {
            if (((String)current).indexOf(',')>=0) {
                String[] parts=current.split("\\s*,\\s*");
                list.addAll(Arrays.asList(parts));
            } else list.add((String)current);
        }
        ArrayList<String> removeset=new ArrayList<String>();
        if (toRemove instanceof ArrayList) {
            for (String tR:(ArrayList<String>)toRemove) {            
                if (tR.indexOf(',')>=0) {
                    String[] toremoveparts=tR.split("\\s*,\\s*");
                    removeset.addAll(Arrays.asList(toremoveparts));
                } else removeset.add(tR);
            }
        }
        else {
            toRemove=toRemove.toString();
            if (((String)toRemove).indexOf(',')>=0) {
                String[] toremoveparts=((String)toRemove).split("\\s*,\\s*");
                removeset.addAll(Arrays.asList(toremoveparts));
            } else removeset.add((String)toRemove);
        }
        list.removeAll(removeset);
        return MotifLabEngine.splice(list,",");
    }
    
    
    
    private class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final FeatureDataset targetDataset;
        final FeatureDataset sourceDataset;
        final long[] counters; // NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;;   
        
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

                 if (sourceSequence instanceof NumericSequenceData) transformNumericSequence((NumericSequenceData)sourceSequence, (NumericSequenceData)targetSequence, task);
            else if (sourceSequence instanceof RegionSequenceData)  transformRegionSequence((RegionSequenceData)sourceSequence, (RegionSequenceData)targetSequence, task);
            
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
