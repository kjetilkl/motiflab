/*
 
 
 */

package motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Operation_apply;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_apply extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
         
        String windowTypeString=(String)task.getParameter(Operation_apply.WINDOW_TYPE);
        String windowSizeString=(String)task.getParameter(Operation_apply.WINDOW_SIZE);
        String windowAnchorString=(String)task.getParameter(Operation_apply.ANCHOR);
        if (windowAnchorString==null || windowAnchorString.isEmpty()) windowAnchorString=Operation_apply.CENTER;
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String msg="";
        String parstring=windowTypeString+" window of size "+windowSizeString+" with anchor at "+windowAnchorString;
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="apply "+parstring+" to "+sourceName;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="apply";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String windowTypeString=null;
           String windowSizeString=null;
           String windowAnchorString=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?apply (\\S+) window of size (\\S+)( with anchor at (\\S+?))? to ([a-zA-Z_0-9-]+)( where (.+))?(\\s*\\S.*)?");
           
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               windowTypeString=matcher.group(3);
               windowSizeString=matcher.group(4);
               windowAnchorString=matcher.group(6);
               sourceName=matcher.group(7);               
               whereString=matcher.group(9);
               String unknown = matcher.group(10);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (windowTypeString==null || windowTypeString.isEmpty()) throw new ParseError("Missing window type specification");
           if (windowSizeString==null || windowSizeString.isEmpty()) throw new ParseError("Missing window size specification");
           if (windowAnchorString==null || windowAnchorString.isEmpty()) windowAnchorString=Operation_apply.CENTER;
           if (!Operation_apply.isRecognizedAnchor(windowAnchorString)) throw new ParseError("Unknown anchor point: "+windowAnchorString);
           if (whereString!=null && !windowTypeString.isEmpty()) parseWherePositionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;           

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=NumericDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object has wrong type!");
           lookup.register(targetName, NumericDataset.class);
           task.addAffectedDataObject(targetName, NumericDataset.class); 

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_apply.WINDOW_TYPE, windowTypeString);
           task.setParameter(Operation_apply.WINDOW_SIZE, windowSizeString);
           task.setParameter(Operation_apply.ANCHOR, windowAnchorString);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }

}
