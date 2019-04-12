package motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.Module;
import motiflab.engine.data.Motif;
import motiflab.engine.data.Sequence;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_delete extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();

        String msg="delete "+sourceName;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="delete";
           String sourceName=null;
           String targetName=null;

           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?delete ([a-zA-Z_0-9-,\\s]+)?(\\s*\\S+)?");
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
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("No data items specified to be deleted");
           sourceName=sourceName.trim();
           if (targetName!=null && !targetName.isEmpty()) throw new ParseError("Assignment not allowed for operation 'delete'");
           String[] sourceNames=sourceName.split("\\s*,\\s*");
           checkSourceObjects(sourceNames,lookup); // checks the list of sourcenames and throws ParseErrors if appropriate
           for (String source:sourceNames) {
              task.addAffectedDataObject(source, null);
              lookup.register(source, null);
           }
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           return task;
    }


    private void checkSourceObjects(String[] sourceNames, DataTypeTable lookup) throws ParseError {
        for (int i=0;i<sourceNames.length;i++) {
           String sourceName=sourceNames[i];
           if (sourceName.isEmpty()) throw new ParseError("Empty name for data object");
           if (sourceName.equals(engine.getDefaultSequenceCollectionName())) throw new ParseError("'"+sourceName+"' can not be deleted with the 'delete' operation");
           Class sourceclass=lookup.getClassFor(sourceName);
           // if (sourceclass==null) throw new ParseError("Unknown data object: "+sourceName);
           if (sourceclass==null) return; // this could mean that the command has been executed and the object has already been deleted. We should not complain about this?           
           if (sourceclass==Sequence.class) throw new ParseError("Sequences can not be deleted with the 'delete' operation");
           if (sourceclass==Motif.class) throw new ParseError("Motifs can not be deleted with the 'delete' operation");
           if (sourceclass==Module.class) throw new ParseError("Modules can not be deleted with the 'delete' operation");
        }
    }

}
