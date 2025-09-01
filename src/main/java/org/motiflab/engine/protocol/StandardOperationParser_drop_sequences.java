package org.motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.SequenceCollection;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_drop_sequences extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();

        String msg="drop_sequences "+sourceName;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="drop_sequences";
           String sourceName=null;
           String targetName=null;

           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?drop_sequences ([a-zA-Z_0-9-]+)?(\\s*\\S+)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               String unknown = matcher.group(4);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing name of Sequence Collection to be dropped");
           sourceName=sourceName.trim();
           if (targetName!=null && !targetName.isEmpty()) throw new ParseError("Assignment not allowed for operation 'drop_sequences'");
           checkSourceObjects(sourceName,lookup); // checks the list of sourcenames and throws ParseErrors if appropriate
           task.addAffectedDataObject(sourceName, null);
           lookup.register(sourceName, null);
           
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setBlockGUI(true);
           task.setTurnOffGUInotifications(true);
           return task;
    }


    private void checkSourceObjects(String sourceName, DataTypeTable lookup) throws ParseError {
       if (sourceName.isEmpty()) throw new ParseError("Empty name for data object");
       if (sourceName.equals(engine.getDefaultSequenceCollectionName())) throw new ParseError("'"+sourceName+"' can not be deleted with the 'drop_sequences' operation");
       Class sourceclass=lookup.getClassFor(sourceName);
       // if (sourceclass==null) throw new ParseError("Unknown data object: "+sourceName);
       if (sourceclass==null) return; // this could mean that the command has been executed and the object has already been deleted. We should not complain about this?
       if (sourceclass!=SequenceCollection.class) throw new ParseError("Only Sequence Collections can be used as argument for the 'drop_sequences' operation");        
    }

}
