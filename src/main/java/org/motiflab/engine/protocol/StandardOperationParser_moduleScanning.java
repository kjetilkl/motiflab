/*
 
 
 */

package org.motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Operation_moduleScanning;
import org.motiflab.external.ExternalProgram;
import org.motiflab.external.ModuleScanning;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_moduleScanning extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String sourceName=task.getSourceDataName();
        String algorithmName=(String)task.getParameter(Operation_moduleScanning.ALGORITHM);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        String additionalResults=(String)task.getParameter(Operation_moduleScanning.ADDITIONAL_RESULTS);        
        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        if (algorithm==null) return "ERROR: Unknown algorithm '"+algorithmName+"'";
        if (!(algorithm instanceof ModuleScanning)) return "ERROR: '"+algorithmName+"' is not a Module Scanning algorithm";
        String parameters=getParameterSettingsAsString((ModuleScanning)algorithm,task);
        String msg=(additionalResults!=null && !additionalResults.isEmpty())?("["+targetName+","+additionalResults+"]"):targetName;
        msg+=" = moduleScanning in "+sourceName+" with "+algorithmName;   
        if (parameters!=null && !parameters.isEmpty()) msg+=" {"+parameters+"}";
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }


    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="moduleScanning";
           String sourceName=null;
           String targetName=null;
           String algorithmName=null;
           String parameters=null;
           String sequenceCollection=null;
           String[] results=null;               
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           String[] list = engine.getAvailableModuleScanningAlgorithms();
           //for (String typename:list) algorithmNames+="(?i)"+typename+"|";
           //algorithmNames=algorithmNames.substring(0, algorithmNames.length()-1);
           //Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?moduleScanning ([a-zA-Z_0-9-]+)?( with ("+algorithmNames+"))?(\\s*\\{(.*)\\})?(\\s*\\S.*)?");
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+?|\\[([a-zA-Z_0-9-,\\s]+?)\\])\\s*=\\s*)?moduleScanning (?:on|in) ([a-zA-Z_0-9-]+)?( with ([a-zA-Z_0-9-]+))?(\\s*\\{(.*)\\})?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               String targets=null;
               String singleTargetName=matcher.group(2);
               String multipleTargetName=matcher.group(3);
               if (multipleTargetName!=null && !multipleTargetName.isEmpty()) targets=multipleTargetName; else targets=singleTargetName;               
               if (targets!=null) {
                   results=targets.trim().split("\\s*,\\s*");
                   if (results.length>=1) targetName=results[0].trim();
               }                 
               sourceName=matcher.group(4);
               algorithmName=matcher.group(6);
               parameters=matcher.group(8);
               String unknown=matcher.group(9);
               if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing target dataset for operation 'moduleScanning'");
               if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source dataset for operation 'moduleScanning'");
               if (algorithmName==null || algorithmName.isEmpty()) throw new ParseError("Missing specification of module scanning algorithm");
               else if (!inStringArray(list, algorithmName)) throw new ParseError("Unknown module scanning algorithm: "+algorithmName);
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");

           ModuleScanning algorithm=null;
           if (algorithmName!=null) {
               ExternalProgram program=engine.getExternalProgram(algorithmName);
               if (program==null) throw new ParseError("Unrecognized Module Scanning algorithm '"+algorithmName+"'");
               else if (!(program instanceof ModuleScanning)) throw new ParseError("'"+algorithmName+"' is not a Module Scanning algorithm");
               else algorithm=(ModuleScanning)program;
           }

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is can not be used as input for Module Scanning");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=RegionDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object exists and is not a Region Dataset!");
           lookup.register(targetName, RegionDataset.class);
           task.addAffectedDataObject(targetName, RegionDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_moduleScanning.ALGORITHM, algorithmName);
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);

           if (algorithm!=null && parameters!=null && !parameters.isEmpty()) {
               parseParameterSettings(parameters,algorithm,task);
           }
           if (algorithm!=null) {
               int resultsCardinality=algorithm.getNumberOfResultParameters();
               if (results.length>1) {
                   if (results.length!=resultsCardinality) throw new ParseError("'"+algorithmName+"' requires specification of either 1 or "+resultsCardinality+" result data objects");
                   String additionalResults="";
                   for (int i=1;i<results.length;i++) {
                       String paramName=results[i].trim();
                       Class paramtype=algorithm.getTypeForResultParameter(i);
                       lookup.register(paramName, paramtype);
                       task.addAffectedDataObject(paramName, paramtype); 
                       if (i>1) additionalResults+=",";
                       additionalResults+=paramName;
                   }
                   task.setParameter(Operation_moduleScanning.ADDITIONAL_RESULTS, additionalResults);
               }            
           }             
           return task;
    }


    private boolean inStringArray(String[] list,String element) {
        for (int i=0;i<list.length;i++) {
            if (list[i].equals(element)) return true;
        }
        return false;
    }


    /** Parses and sets the Module Scanning program parameter settings from a string with OPERATION_NAME-value pairs*/
    private void parseParameterSettings(String text, ModuleScanning algorithm, OperationTask task) throws ParseError {
        ParametersParser parametersParser=protocol.getParametersParser();
        try {
           ParameterSettings settings=parametersParser.parse(text, algorithm.getParameters());
           task.setParameter(Operation_moduleScanning.PARAMETERS, settings);
        } catch (ParseError parseError) {
          parseError.setLineNumber(task.getLineNumber());
          throw parseError;
        }
    }

    private String getParameterSettingsAsString(ModuleScanning algorithm, OperationTask task) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(Operation_moduleScanning.PARAMETERS);
        ParametersParser parametersParser=protocol.getParametersParser();
        if (settings!=null) {
            return parametersParser.getCommandString(algorithm.getParameters(), settings);
        }
        else return "";
    }

}
