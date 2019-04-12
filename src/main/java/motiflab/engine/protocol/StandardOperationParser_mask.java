/*
 
 
 */

package motiflab.engine.protocol;


import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Operation_mask;
import motiflab.engine.operations.Operation;
import motiflab.engine.data.DNASequenceDataset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Condition;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_mask extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
         
        String maskString=(String)task.getParameter(Operation_mask.MASK_STRING);        
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String strandString=(String)task.getParameter(Operation_mask.STRAND);        
        String msg="";
        String parstring="with "+maskString;
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="mask "+sourceName;
        if (strandString!=null && !strandString.isEmpty()) msg+=" on "+strandString+" strand";
        msg+=" "+parstring;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="mask";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String maskString=null;
           String strandString=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?mask ([a-zA-Z_0-9-]+)?((?: on)? (\\w+) strand)?( with (\\S+))?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               strandString=matcher.group(5);               
               maskString=matcher.group(7);
               whereString=matcher.group(9);
               String unknown = matcher.group(10);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for mask operation");
           if (maskString==null || maskString.isEmpty()) throw new ParseError("Missing mask type specification");
           if (whereString!=null && !maskString.isEmpty()) parseWherePositionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           if (strandString!=null && !strandString.isEmpty()) {
             if (strandString.equalsIgnoreCase("relative") || strandString.equalsIgnoreCase("sequence")||strandString.equalsIgnoreCase("gene")) strandString="relative";  
             else if (!strandString.equalsIgnoreCase("direct")) throw new ParseError("Unknown strand specification '"+strandString+"'");
           } else strandString="relative";
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=DNASequenceDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object has wrong type!");
           lookup.register(targetName, DNASequenceDataset.class);
           task.addAffectedDataObject(targetName, DNASequenceDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_mask.MASK_STRING, maskString);
           task.setParameter(Operation_mask.STRAND, strandString);
           task.setParameter(OperationTask.TARGET_NAME, targetName);          
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }


}