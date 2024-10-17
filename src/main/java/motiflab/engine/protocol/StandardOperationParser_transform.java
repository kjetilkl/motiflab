/*
 
 
 */

package motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.Motif;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.operations.Operation_transform;
import motiflab.engine.operations.ArithmeticOperation;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_transform extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();

        String transformArgumentString=(String)task.getParameter(Operation_transform.TRANSFORM_ARGUMENT_STRING);
        String transformNameString=(String)task.getParameter(Operation_transform.TRANSFORM_NAME);
        String subset=(String)task.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        String propertyName=(String)task.getParameter(Operation_transform.PROPERTY_NAME);
        StringBuilder msg=new StringBuilder();
        if (!sourceName.equalsIgnoreCase(targetName))  msg.append(targetName+" = ");
        msg.append("transform ");
        msg.append(sourceName);
        if (propertyName!=null) {
            msg.append('[');
            msg.append(propertyName);
            msg.append(']');
        }        
        msg.append(" with ");
        msg.append(transformNameString);
        if (Operation_transform.takesArgument(transformNameString) && transformArgumentString!=null && !transformArgumentString.isEmpty()) msg.append("("+transformArgumentString+")");
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg.append(" where "+getCommandString_condition(condition));
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg.append(" in collection "+subset);
        return msg.toString();
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="transform";
           String sourceName=null;
           String targetName=null;
           String propertyName=null;
           String dataCollectionName=null;
           String transformNameString=null;
           String transformArgumentString=null;
           String whereString=null;           
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) dataCollectionName=splitOn[1].trim();
           if (dataCollectionName!=null) {
             String[] splitOn2=dataCollectionName.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?transform (([a-zA-Z_0-9-]+)(\\[(.+?)\\]|\\s+property\\s+\"(.+?)\")?)?( with ([a-zA-Z_0-9-]+)(\\(([a-zA-Z_0-9-\\.]+|\".*?\")\\))?)?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(4);
               propertyName=matcher.group(6);
               if (propertyName==null) propertyName=matcher.group(7); // either way is fine 
               if (propertyName!=null) {
                   propertyName=propertyName.trim();
                   if (propertyName.startsWith("\"") || propertyName.startsWith("\'")) propertyName=propertyName.substring(1);
                   if (propertyName.endsWith("\"") || propertyName.endsWith("\'")) propertyName=propertyName.substring(0,propertyName.length()-1);                  
               }
               transformNameString=matcher.group(9);
               transformArgumentString=matcher.group(11);
               whereString=matcher.group(13);
               String unknown = matcher.group(14);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (transformNameString==null || transformNameString.isEmpty()) throw new ParseError("Missing transform specification");
           if (!Operation_transform.isTransform(transformNameString)) throw new ParseError("Unknown transform '"+transformNameString+"'");
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           if (whereString!=null) {
               if (sourceclass==RegionDataset.class) parseWhereRegionCondition(whereString, task);
               else if (sourceclass==NumericDataset.class) parseWherePositionCondition(whereString,task);
           }
           if ((propertyName!=null && !propertyName.isEmpty()) && !(sourceclass==RegionDataset.class || sourceclass==Motif.class || sourceclass==ModuleCRM.class || sourceclass==Sequence.class || DataCollection.class.isAssignableFrom(sourceclass))) throw new ParseError(engine.getTypeNameForDataClass(sourceclass)+" '"+sourceName+"' can not have a property named '"+propertyName+"'");
                      
           
           lookup.register(targetName, sourceclass);
           task.addAffectedDataObject(targetName, sourceclass);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_transform.TRANSFORM_NAME, transformNameString);
           task.setParameter(Operation_transform.TRANSFORM_ARGUMENT_STRING, transformArgumentString);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_transform.PROPERTY_NAME, propertyName);           
           if (dataCollectionName!=null && !dataCollectionName.isEmpty()) task.setParameter(ArithmeticOperation.DATA_COLLECTION_NAME, dataCollectionName);
           return task;
    }

}
