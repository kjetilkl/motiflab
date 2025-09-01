/*
 
 
 */

package org.motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Condition;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_merge;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_merge extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String distanceString=(String)task.getParameter(Operation_merge.DISTANCE_STRING);    
        String similarString=(String)task.getParameter(Operation_merge.SIMILAR);  
        if (similarString==null) similarString="";
        else similarString=similarString+" ";
        String msg="";
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="merge "+similarString+sourceName;
        if (distanceString!=null) msg+=" closer than "+distanceString;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="merge";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String similarString=null;
           String distanceString=null;
           String whereString=null;           
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?merge( similar| any)? ([a-zA-Z_0-9-]+)?( closer than ([a-zA-Z_0-9-]+))?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(4);
               similarString=matcher.group(3);
               distanceString=matcher.group(6);
               whereString=matcher.group(8);
               String unknown = matcher.group(9);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for merge operation");
           if (whereString!=null) parseWhereRegionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           lookup.register(targetName, RegionDataset.class);
           task.addAffectedDataObject(targetName, RegionDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           if (distanceString!=null && !distanceString.isEmpty()) task.setParameter(Operation_merge.DISTANCE_STRING, distanceString);
           if (similarString!=null && similarString.equals(" similar")) task.setParameter(Operation_merge.SIMILAR, "similar");
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }

}


