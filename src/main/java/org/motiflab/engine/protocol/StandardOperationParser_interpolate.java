/*
 
 
 */

package org.motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.operations.Condition;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_interpolate;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_interpolate extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
         
        String method=(String)task.getParameter(Operation_interpolate.METHOD);        
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String msg="";
        String periodString="";
        if (task.getParameter(Operation_interpolate.PERIOD)!=null) {
            periodString=" with period "+(String)task.getParameter(Operation_interpolate.PERIOD);
        }
        if (task.getParameter(Operation_interpolate.MAX_DISTANCE)!=null) {
            periodString=" and max distance "+(String)task.getParameter(Operation_interpolate.MAX_DISTANCE);
        }        
        String parstring=" using \""+method+"\"";
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="interpolate "+sourceName;
        msg+=parstring+periodString;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="interpolate";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String methodString=null;
           String periodString=null;
           String maxDistanceString=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?interpolate ([a-zA-Z_0-9-]+)?( using \"(.+?)\")?( with period (\\S+))?( and max distance (\\S+))?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               methodString=matcher.group(5);
               periodString=matcher.group(7);
               maxDistanceString=matcher.group(9);
               whereString=matcher.group(11);
               String unknown = matcher.group(12);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));              
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (methodString==null || methodString.isEmpty()) throw new ParseError("Missing specification of interpolation method");
           if (whereString!=null && !methodString.isEmpty()) parseWherePositionCondition(whereString,task);
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
           task.setParameter(Operation_interpolate.METHOD, methodString);
           task.setParameter(OperationTask.TARGET_NAME, targetName);   
           if (periodString!=null && !periodString.isEmpty()) task.setParameter(Operation_interpolate.PERIOD, periodString);
           if (maxDistanceString!=null && !maxDistanceString.isEmpty()) task.setParameter(Operation_interpolate.MAX_DISTANCE, maxDistanceString);
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }

}
