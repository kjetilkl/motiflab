/*
 
 
 */

package motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Operation_extract;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_extract extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        String variableName=(String)task.getParameter(Operation_extract.RESULT_VARIABLE_NAME);
        Class variableType=(Class)task.getParameter(Operation_extract.RESULT_VARIABLE_TYPE);
        String msg="";
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="extract \""+variableName+"\" from "+sourceName+" as "+engine.getTypeNameForDataClass(variableType);
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="extract";
           String sourceName=null;
           String targetName=null;
           String variableName=null;
           String variableType=null;
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?extract\\s+\"(.+?)\"\\s+from\\s+([a-zA-Z_0-9.+-]+)\\s+as\\s+(\\S.+)");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               variableName=matcher.group(3);
               sourceName=matcher.group(4);
               variableType=matcher.group(5);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for extract operation");           
           if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing target data name for extract operation");           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           if (variableName==null || variableName.isEmpty()) throw new ParseError("Missing result variable specification");           
           if (variableType==null || variableType.isEmpty()) throw new ParseError("Missing data type specification of result variable");           

           Class variableclass=engine.getDataClassForTypeName(variableType);
           if (variableclass==null) throw new ParseError("Unknown data type: "+variableType);
           lookup.register(targetName, variableclass);
           task.addAffectedDataObject(targetName, variableclass);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_extract.RESULT_VARIABLE_NAME, variableName);
           task.setParameter(Operation_extract.RESULT_VARIABLE_TYPE, variableclass);
           task.setParameter(command, task);
           return task;
    }

}


