/*
 
 
 */

package motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.operations.Operation_convert;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_convert extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        String targetType=(String)task.getParameter(Operation_convert.TARGET_TYPE);
        String newValue=(String)task.getParameter(Operation_convert.NEW_VALUE_STRING);
        String newValueOperator=(String)task.getParameter(Operation_convert.NEW_VALUE_OPERATOR);
           
        String msg="";
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="convert "+sourceName+" to "+targetType;

        if (targetType.equalsIgnoreCase(Operation_convert.REGION)) { // numeric->region
            msg+=" with region.score=";
            if (newValueOperator!=null)  msg+=newValueOperator+" ";
            msg+=newValue;
        } else {  // region->numeric
            msg+=" with value="+newValue;  
        } 
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="convert";
           String sourceName=null;
           String targetName=null;
           String targetType=null;
           String newValue=null;
           String newValueOperator=null;
           String whereString=null;
           String[] splitWhere=command.split(" where ");
           if (splitWhere.length==2 && !splitWhere[1].isEmpty()) {
               if (splitWhere[1]!=null) whereString=splitWhere[1].trim();
               command=splitWhere[0].trim();
           }       
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?convert ([a-zA-Z_0-9-]+)? to (region|Region)( with region\\.score\\s*=\\s*((min|minimum|max|maximum|sum|avg|average|median) )?(\\S+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {  // Numeric -> Region ?
               //System.err.println("\nConvert numeric->region");
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               targetType=matcher.group(4).toLowerCase();
               newValueOperator=matcher.group(7);               
               newValue=matcher.group(8);
               String unknown = matcher.group(9);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else {
               pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?convert ([a-zA-Z_0-9-]+)? to (numeric|Numeric)( with value\\s*=\\s*(\\S+))?(\\s*\\S.*)?");
               matcher=pattern.matcher(command);
               if (matcher.find()) { // Region -> Numeric ? 
                   //System.err.println("\nConvert region->numeric");
                   //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                   targetName=matcher.group(2);
                   sourceName=matcher.group(3);
                   targetType=matcher.group(4).toLowerCase();
                   newValue=matcher.group(6);
                   String unknown = matcher.group(7);
                   if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           }
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for weight operation");
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;           

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           if (sourceclass==NumericDataset.class && targetType.equals("numeric")) throw new ParseError("'"+sourceName+"' is already a Numeric Dataset (no conversion necessary)");
           if (sourceclass==RegionDataset.class && targetType.equals("region")) throw new ParseError("'"+sourceName+"' is already a Region Dataset (no conversion necessary)");
           
           if (whereString!=null && !whereString.isEmpty()) {
               if (sourceclass==NumericDataset.class) parseWherePositionCondition(whereString,task);
               else if (sourceclass==RegionDataset.class) parseWhereRegionCondition(whereString,task);
           }

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           task.setParameter(Operation_convert.NEW_VALUE_STRING, newValue);           
           task.setParameter(Operation_convert.NEW_VALUE_OPERATOR, newValueOperator);               
           task.setParameter(Operation_convert.TARGET_TYPE, targetType);               

           if (sourceclass==NumericDataset.class) {
               lookup.register(targetName, RegionDataset.class);
               task.addAffectedDataObject(targetName, RegionDataset.class);
           }  else if (sourceclass==RegionDataset.class) {
               lookup.register(targetName, NumericDataset.class);
               task.addAffectedDataObject(targetName, NumericDataset.class);
           }
            
           return task;
    }

}
