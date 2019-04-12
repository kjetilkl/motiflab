/*
 
 
 */

package motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Condition;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.operations.ArithmeticOperation;
import motiflab.engine.operations.Operation;
import motiflab.engine.operations.Operation_threshold;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_threshold extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();

        String cutoffString=(String)task.getParameter(Operation_threshold.CUTOFF_THRESHOLD_STRING);
        String aboveString=(String)task.getParameter(Operation_threshold.ABOVE_OR_EQUAL_STRING);
        String belowString=(String)task.getParameter(Operation_threshold.BELOW_STRING);
        String subset=(String)task.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        String msg="";
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="threshold "+sourceName;
        msg+=" with cutoff="+cutoffString+" set values above cutoff to "+aboveString+" and values below cutoff to "+belowString;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);

        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="threshold";
           String sourceName=null;
           String targetName=null;
           String dataCollectionName=null;
           String cutoffString=null;
           String rangeString1=null;
           String rangeString2=null;
           String rangeName1=null;
           String rangeName2=null;
           String aboveString=null;
           String belowString=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) dataCollectionName=splitOn[1].trim();
           if (dataCollectionName!=null) {
             String[] splitOn2=dataCollectionName.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?threshold ([a-zA-Z_0-9-]+)?( with cutoff\\s*=\\s*(\\S+)\\s+(above\\s*=|below\\s*=|set values above cutoff to|set values below cutoff to)\\s*(\\S+)\\s+(above\\s*=|below\\s*=|and values above cutoff to|and values below cutoff to)\\s*(\\S+))?(\\s+where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               cutoffString=matcher.group(5);
               rangeName1=matcher.group(6);
               rangeString1=matcher.group(7);
               rangeName2=matcher.group(8);
               rangeString2=matcher.group(9);
               whereString=matcher.group(11);
               String unknown = matcher.group(12);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (rangeName1.contains("above")) aboveString=rangeString1; else belowString=rangeString1;
           if (rangeName2.contains("above")) aboveString=rangeString2; else belowString=rangeString2;

           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for threshold operation");
           if (aboveString==null) throw new ParseError("Missing specification of value to use above cutoff-threshold");
           if (belowString==null) throw new ParseError("Missing specification of value to use below cutoff-threshold");
           if (whereString!=null) parseWherePositionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");

           if (whereString!=null && !whereString.isEmpty()) {
               if (sourceclass==NumericDataset.class) parseWherePositionCondition(whereString,task);
               else if (sourceclass==RegionDataset.class) parseWhereRegionCondition(whereString,task);
           }

           if (sourceclass==NumericDataset.class) {
               lookup.register(targetName, NumericDataset.class);
               task.addAffectedDataObject(targetName, NumericDataset.class);
           }  else if (sourceclass==RegionDataset.class) {
               lookup.register(targetName, RegionDataset.class);
               task.addAffectedDataObject(targetName, RegionDataset.class);
           }

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_threshold.CUTOFF_THRESHOLD_STRING, cutoffString);
           task.setParameter(Operation_threshold.ABOVE_OR_EQUAL_STRING, aboveString);
           task.setParameter(Operation_threshold.BELOW_STRING, belowString);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           if (dataCollectionName!=null && !dataCollectionName.isEmpty()) task.setParameter(ArithmeticOperation.DATA_COLLECTION_NAME, dataCollectionName);
           return task;
    }

}
