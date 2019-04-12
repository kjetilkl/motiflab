/*
 
 
 */

package motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.operations.Operation_execute;
import motiflab.external.ExternalProgram;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_execute extends StandardOperationParser {
    

    
    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String algorithmName=(String)task.getParameter(Operation_execute.ALGORITHM);
        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME); 
        if (algorithm==null) return "ERROR: Unknown algorithm '"+algorithmName+"'";
        String parameters=getParameterSettingsAsString(algorithm,task);
        String msg=(algorithm.getNumberOfResultParameters()>1)?"["+targetName+"]":targetName;
        msg+=" = execute "+algorithmName;
        if (parameters!=null && !parameters.isEmpty()) msg+=" {"+parameters+"}";
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }
    

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="execute";
           String targetName=null;
           String algorithmName=null;
           String parameters=null;
           String sequenceCollection=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           String[] list = engine.getOtherExternalPrograms();
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+?|\\[([a-zA-Z_0-9-,\\s]+?)\\])\\s*=\\s*)?execute ([a-zA-Z_0-9-]+)?(\\s*\\{(.*)\\})?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               String singleTargetName=matcher.group(2);
               String multipleTargetName=matcher.group(3);
               if (multipleTargetName!=null && !multipleTargetName.isEmpty()) targetName=multipleTargetName; else targetName=singleTargetName;
               algorithmName=matcher.group(4);
               parameters=matcher.group(6);
               String unknown=matcher.group(7);
               if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing target data object for operation 'execute'");
               if (algorithmName==null || algorithmName.isEmpty()) throw new ParseError("Missing external program name");
               else if (!inStringArray(list, algorithmName)) throw new ParseError("Unknown external program: "+algorithmName);
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");

           ExternalProgram algorithm=null;
           if (algorithmName!=null) {
               ExternalProgram program=engine.getExternalProgram(algorithmName);
               if (program==null) throw new ParseError("Unrecognized external program '"+algorithmName+"'");
               else algorithm=program;
           }
           String[] targetNames=targetName.split("\\s*,\\s*");
           int numberOfResultParameters=algorithm.getNumberOfResultParameters();
           if (targetNames.length!=numberOfResultParameters) throw new ParseError(targetNames.length+" target name"+((targetNames.length==1)?" is":"s are")+" specified, but external program '"+algorithm.getName()+"' returns "+numberOfResultParameters+" data object"+((numberOfResultParameters==1)?"":"s")+".");
           for (int i=0;i<targetNames.length;i++) {
               String nextTarget=targetNames[i].trim();
               if (nextTarget.isEmpty()) throw new ParseError("Missing name for result data object");
               Class type=algorithm.getTypeForResultParameter(i);
               lookup.register(nextTarget, type); 
               task.addAffectedDataObject(nextTarget, type);
           }
           
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           //task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_execute.ALGORITHM, algorithmName);
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           
           if (algorithm!=null && parameters!=null && !parameters.isEmpty()) {               
               parseParameterSettings(parameters,algorithm,task);
           }    
           
           return task;
    }

    
    private boolean inStringArray(String[] list,String element) {
        for (int i=0;i<list.length;i++) {
            if (list[i].equals(element)) return true;
        }
        return false;
    }    


    /** Parses and sets the external program parameter settings from a string with name-value pairs*/
    private void parseParameterSettings(String text, ExternalProgram algorithm, OperationTask task) throws ParseError {
        ParametersParser parametersParser=protocol.getParametersParser();
        try {
            ParameterSettings settings=parametersParser.parse(text, algorithm.getParameters());
            task.setParameter(Operation_execute.PARAMETERS, settings);
        } catch (ParseError parseError) {
            parseError.setLineNumber(task.getLineNumber()); 
          throw parseError;
        }       
    } 
    
    private String getParameterSettingsAsString(ExternalProgram algorithm, OperationTask task) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(Operation_execute.PARAMETERS);
        ParametersParser parametersParser=protocol.getParametersParser();
        if (settings!=null) {
            return parametersParser.getCommandString(algorithm.getParameters(), settings);
        }
        else return "";        
    }    
    
}
