/*

 */

package org.motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_prior extends StandardOperationParser {
    

    
    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String sourceName=task.getSourceDataName();
        String msg=targetName+" = prior "+sourceName;   
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="prior";
           String sourceName=null;
           String targetName=null;           
          
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?prior ([a-zA-Z_0-9-]+)?(\\s*\\S.*)?");  
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               String unknown=matcher.group(4);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           task.setParameter(OperationTask.ENGINE, engine);
           Class dataclass=lookup.getClassFor(sourceName);
           if (dataclass==null) throw new ParseError("Unknown data item: "+sourceName);
           lookup.register(targetName, dataclass);
           task.addAffectedDataObject(targetName, dataclass);
           return task;
    }
    
}