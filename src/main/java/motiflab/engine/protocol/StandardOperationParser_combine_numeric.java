/*
 
 
 */

package motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Operation_combine_numeric;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_combine_numeric extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
         
        String operatorString=(String)task.getParameter(Operation_combine_numeric.OPERATOR);              
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String msg="";
        String parstring="using "+operatorString;
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="combine_numeric "+sourceName;
        msg+=" "+parstring;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="combine_numeric";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String operatorString=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split("\\s+");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?combine_numeric ([a-zA-Z_0-9-,\\s]+)?\\s*using (sum|product|average|min|max|minimum|maximum)?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               operatorString=matcher.group(4);
               whereString=matcher.group(6);
               String unknown = matcher.group(7);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           sourceName=sourceName.trim();
           if (operatorString==null || operatorString.isEmpty()) throw new ParseError("Missing combine operator");
           if (whereString!=null && !operatorString.isEmpty()) parseWherePositionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing required target data object for combine operation");         
           Class classtype=checkSourceObjects(sourceName,lookup,operation); // checks the list of sourcenames and throws ParseErrors if appropriate
           lookup.register(targetName, classtype);
           task.addAffectedDataObject(targetName, classtype);
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_combine_numeric.OPERATOR, operatorString);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }
    
    
    private Class checkSourceObjects(String sourceNameString, DataTypeTable lookup, Operation operation) throws ParseError {
        String[] sourceNames=sourceNameString.split(",");
        Class classtype=null;
        for (int i=0;i<sourceNames.length;i++) {
           String sourceName=sourceNames[i].trim();
           if (sourceName.isEmpty()) throw new ParseError("Missing name for source data object");
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operation.getName()+"'");
           if (i==0) classtype=sourceclass; 
           else if (sourceclass!=classtype) throw new ParseError("All the data objects to combine must be of the same type! ('"+sourceName+"' does not have the same type as the preceeding objects)");
        } 
        return classtype;
    }

}
