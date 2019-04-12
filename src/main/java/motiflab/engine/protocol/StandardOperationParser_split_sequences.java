package motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.SequencePartition;
import motiflab.engine.operations.Operation_split_sequences;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_split_sequences extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();     
        String msg="";
        if (targetName!=null && !targetName.isEmpty()) {
            msg+=targetName+" = ";
        }
        msg+="split_sequences based on "+sourceName;
        String sequenceCollection=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        if (sequenceCollection!=null && !sequenceCollection.isEmpty()) msg+=" in collection "+sequenceCollection;
        Boolean deleteOriginals=(Boolean)task.getParameter(Operation_split_sequences.DELETE_ORIGINAL_SEQUENCES);
        if (deleteOriginals!=null && deleteOriginals) msg+=". Delete original sequences";
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="split_sequences";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String deleteString=null;

           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?split_sequences based on ([a-zA-Z_0-9-]+)?( in collection ([a-zA-Z_0-9-]+))?(\\.?\\s*[Dd]elete original sequences)?(\\s*\\S+)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               sequenceCollection = matcher.group(5);
               deleteString = matcher.group(6);
               String unknown = matcher.group(7);               
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing target name ( target = split_sequences ...)");
           task.addAffectedDataObject(sourceName, null);       
           lookup.register(targetName, SequencePartition.class);
           task.addAffectedDataObject(targetName, SequencePartition.class);        
           
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);  
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           boolean deleteSequences=(deleteString!=null && !deleteString.isEmpty());
           task.setParameter(Operation_split_sequences.DELETE_ORIGINAL_SEQUENCES, deleteSequences);
           task.setBlockGUI(true); // to prevent problems 
           task.setTurnOffGUInotifications(true);
           return task;
    }


}
