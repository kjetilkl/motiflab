/*
 
 
 */

package org.motiflab.engine.protocol;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Operation_moduleDiscovery;
import org.motiflab.external.ExternalProgram;
import org.motiflab.external.ModuleDiscovery;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_moduleDiscovery extends StandardOperationParser {



    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String sourceName=task.getSourceDataName();
        String moduleCollectionName=(String)task.getParameter(Operation_moduleDiscovery.MODULECOLLECTION);
        String moduleprefix=(String)task.getParameter(Operation_moduleDiscovery.MODULEPREFIX);
        String algorithmName=(String)task.getParameter(Operation_moduleDiscovery.ALGORITHM);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        String additionalResults=(String)task.getParameter(Operation_moduleDiscovery.ADDITIONAL_RESULTS);        

        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        if (algorithm==null) return "ERROR: Unknown algorithm '"+algorithmName+"'";
        if (!(algorithm instanceof ModuleDiscovery)) return "ERROR: '"+algorithmName+"' is not a Module Discovery algorithm";
        String parameters=getParameterSettingsAsString((ModuleDiscovery)algorithm,task);
        boolean hasTFBS=((ModuleDiscovery)algorithm).returnsSiteResults();
        boolean hasModules=((ModuleDiscovery)algorithm).returnsModuleResults();       
        String msg="[";
        if (hasTFBS) msg+=targetName;
        if (hasTFBS && hasModules) msg+=",";
        if (hasModules) msg+=moduleCollectionName;
        if (additionalResults!=null && !additionalResults.isEmpty()) msg+=","+additionalResults;
        msg+="] = moduleDiscovery in "+sourceName+" with "+algorithmName;  
        if (parameters!=null && !parameters.isEmpty()) msg+=" {"+parameters+"}";
        if (moduleprefix!=null) msg+=" module-prefix=\""+moduleprefix+"\"";
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }


    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="moduleDiscovery";
           String sourceName=null;
           String targetName=null;
           String moduleCollectionName=null;
           String[] results=null;           
           String algorithmName=null;
           String parameters=null;
           String[] list = engine.getAvailableModuleDiscoveryAlgorithms();
           String sequenceCollection=null;
           ModuleDiscovery algorithm=null;           
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           String moduleprefix=null;
           String[] splitOnPrexif=splitOn[0].split(" module-prefix\\s*=");
           if (splitOnPrexif.length==2 && !splitOnPrexif[1].isEmpty()) moduleprefix=splitOnPrexif[1].trim();
           if (moduleprefix!=null) {
             String[] splitOn3=moduleprefix.split(" ");
             if (splitOn3.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn3[1]);
           }
           Pattern pattern=Pattern.compile("^(\\[\\s*([a-zA-Z_0-9-,\\s]+?)\\s*\\]\\s*=\\s*)?moduleDiscovery (?:on|in) ([a-zA-Z_0-9-]+)?( with ([a-zA-Z_0-9-]+))?(\\s*\\{(.*)\\})?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOnPrexif[0]);
           if (matcher.find()) {
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               String targets=matcher.group(2);
               sourceName=matcher.group(3);
               algorithmName=matcher.group(5);
               parameters=matcher.group(7);
               String unknown=matcher.group(8);
               
               if (algorithmName==null || algorithmName.isEmpty()) throw new ParseError("Missing specification of module discovery algorithm");
               else if (!inStringArray(list, algorithmName)) throw new ParseError("Unknown module discovery algorithm: "+algorithmName);               
               
               if (algorithmName!=null) {
                   ExternalProgram program=engine.getExternalProgram(algorithmName);
                   if (program==null) throw new ParseError("Unrecognized Module Discovery algorithm '"+algorithmName+"'");
                   else if (!(program instanceof ModuleDiscovery)) throw new ParseError("'"+algorithmName+"' is not a Module Discovery algorithm");
                   else algorithm=(ModuleDiscovery)program;
               } 
               if (targets!=null) {
                   results=targets.trim().split("\\s*,\\s*");
                   int count=0;
                   if (results.length>count && algorithm.returnsSiteResults()) {targetName=results[count].trim();count++;}
                   if (results.length>count && algorithm.returnsModuleResults()) moduleCollectionName=results[count].trim();
               }  
               if ((targetName==null || targetName.isEmpty()) && algorithm.returnsSiteResults()) throw new ParseError("Missing target dataset for operation 'moduleDiscovery'");
               if ((moduleCollectionName==null || moduleCollectionName.isEmpty()) && algorithm.returnsModuleResults()) throw new ParseError("Missing module collection target dataname for operation 'moduleDiscovery'");
               if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source dataset for operation 'moduleDiscovery'");
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");

           if (moduleprefix==null) moduleprefix=algorithmName;
           else { // strip quotes
               if (moduleprefix.startsWith("\"")) moduleprefix=moduleprefix.substring(1);
               if (moduleprefix.endsWith("\"")) moduleprefix=moduleprefix.substring(0,moduleprefix.length()-1);
           }
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is not a Sequence dataset");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=RegionDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object exists and is not a Region Dataset!");
           if (targetName!=null) {
               lookup.register(targetName, RegionDataset.class);
               task.addAffectedDataObject(targetName, RegionDataset.class);
           }
           if (moduleCollectionName!=null) {           
               lookup.register(moduleCollectionName, ModuleCollection.class);
               task.addAffectedDataObject(moduleCollectionName, ModuleCollection.class);
           }

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_moduleDiscovery.MODULECOLLECTION, moduleCollectionName);
           task.setParameter(Operation_moduleDiscovery.ALGORITHM, algorithmName);
           task.setParameter(Operation_moduleDiscovery.MODULEPREFIX, moduleprefix);
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);

           if (algorithm!=null && parameters!=null && !parameters.isEmpty()) {
               parseParameterSettings(parameters,algorithm,task);
           }
           if (algorithm!=null) {
               int regularResults=algorithm.getNumberOfRegularResults();
               int additionalResults=algorithm.getNumberOfAdditionalResults();
               if (results.length>regularResults) { // user wants additional results also 
                   if (results.length!=(regularResults+additionalResults)) throw new ParseError("'"+algorithmName+"' requires specification of either "+regularResults+" or "+(regularResults+additionalResults)+" result data objects");
                   String additionalResultsNames="";
                   ArrayList<Parameter> additional=algorithm.getAdditionalResultsParameters();
                   for (int i=0;i<additional.size();i++) {
                       String paramName=results[regularResults+i].trim();
                       Class paramtype=additional.get(i).getType();
                       lookup.register(paramName, paramtype);
                       task.addAffectedDataObject(paramName, paramtype); 
                       if (i>0) additionalResultsNames+=",";
                       additionalResultsNames+=paramName;
                   }
                   task.setParameter(Operation_moduleDiscovery.ADDITIONAL_RESULTS, additionalResultsNames);
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

    /** Parses and sets the Module Discovery program parameter settings from a string with name-value pairs*/
    private void parseParameterSettings(String text, ModuleDiscovery algorithm, OperationTask task) throws ParseError {
        ParametersParser parametersParser=protocol.getParametersParser();
        try {
           ParameterSettings settings=parametersParser.parse(text, algorithm.getParameters());
           task.setParameter(Operation_moduleDiscovery.PARAMETERS, settings);
        } catch (ParseError parseError) {
          parseError.setLineNumber(task.getLineNumber());
          throw parseError;
        }
    }

    private String getParameterSettingsAsString(ModuleDiscovery algorithm, OperationTask task) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(Operation_moduleDiscovery.PARAMETERS);
        ParametersParser parametersParser=protocol.getParametersParser();
        if (settings!=null) {
            return parametersParser.getCommandString(algorithm.getParameters(), settings);
        }
        else return "";
    }

}
