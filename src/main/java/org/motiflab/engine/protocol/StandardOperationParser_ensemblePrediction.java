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
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Operation_ensemblePrediction;
import org.motiflab.external.ExternalProgram;
import org.motiflab.external.EnsemblePredictionMethod;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_ensemblePrediction extends StandardOperationParser {



    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String sourceName=task.getSourceDataName(); // this can be a comma-separated list
        String motifCollectionName=(String)task.getParameter(Operation_ensemblePrediction.MOTIFCOLLECTION);
        String motifprefix=(String)task.getParameter(Operation_ensemblePrediction.MOTIFPREFIX);
        String algorithmName=(String)task.getParameter(Operation_ensemblePrediction.ALGORITHM);
        String dnaTrack=(String)task.getParameter(Operation_ensemblePrediction.DNA_TRACK_NAME);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        String additionalResults=(String)task.getParameter(Operation_ensemblePrediction.ADDITIONAL_RESULTS);        

        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        if (algorithm==null) return "ERROR: Unknown algorithm '"+algorithmName+"'";
        if (!(algorithm instanceof EnsemblePredictionMethod)) return "ERROR: '"+algorithmName+"' is not an Ensemble Prediction algorithm";
        String parameters=getParameterSettingsAsString((EnsemblePredictionMethod)algorithm,task);        
        boolean hasTFBS=((EnsemblePredictionMethod)algorithm).returnsSiteResults();
        boolean hasMotifs=((EnsemblePredictionMethod)algorithm).returnsMotifResults();       
        String msg="[";
        if (hasTFBS) msg+=targetName;
        if (hasTFBS && hasMotifs) msg+=",";
        if (hasMotifs) msg+=motifCollectionName;
        if (additionalResults!=null && !additionalResults.isEmpty()) msg+=","+additionalResults;
        msg+="] = ensemblePrediction in "+sourceName+" with "+algorithmName;          
        if (parameters!=null && !parameters.isEmpty()) msg+=" {"+parameters+"}";
        if (dnaTrack!=null) msg+=" DNA-track="+dnaTrack;
        if (motifprefix!=null) msg+=" motif-prefix=\""+motifprefix+"\"";
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }


    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="ensemblePrediction";
           String sourceName=null;
           String targetName=null;
           String motifCollectionName=null;
           String algorithmName=null;
           String parameters=null;
           String[] results=null;          
           String[] list = engine.getAvailableEnsemblePredictionAlgorithms();
           String sequenceCollection=null;
           EnsemblePredictionMethod algorithm=null;           
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           String motifprefix=null;
           String[] splitOnPrexif=splitOn[0].split(" motif-prefix\\s*=");
           if (splitOnPrexif.length==2 && !splitOnPrexif[1].isEmpty()) motifprefix=splitOnPrexif[1].trim();
           if (motifprefix!=null) {
             String[] splitOn3=motifprefix.split(" ");
             if (splitOn3.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn3[1]);
           }

           String dnaTrackName=null;
           String[] splitOnDNATrack=splitOnPrexif[0].split(" DNA-track\\s*=");
           if (splitOnDNATrack.length==2 && !splitOnDNATrack[1].isEmpty()) dnaTrackName=splitOnDNATrack[1].trim();
           if (dnaTrackName!=null) {
             String[] splitOn3=dnaTrackName.split(" ");
             if (splitOn3.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn3[1]);
           }

           String remaining=splitOnDNATrack[0];
           Pattern pattern=Pattern.compile("^(\\[\\s*([a-zA-Z_0-9-,\\s]+?)\\s*\\]\\s*=\\s*)?ensemblePrediction (?:on|in) ([a-zA-Z_0-9-,\\s]+)( with ([a-zA-Z_0-9-]+))(\\s*\\{(.*)\\})?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(remaining);
           if (matcher.find()) {
               // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               String targets=matcher.group(2);
               sourceName=matcher.group(3);
               algorithmName=matcher.group(5);
               parameters=matcher.group(7);
               String unknown=matcher.group(8);
               
               if (algorithmName==null || algorithmName.isEmpty()) throw new ParseError("Missing specification of ensemble prediction algorithm");
               else if (!inStringArray(list, algorithmName)) throw new ParseError("Unknown ensemble prediction algorithm: "+algorithmName);               
               
               if (algorithmName!=null) {
                   ExternalProgram program=engine.getExternalProgram(algorithmName);
                   if (program==null) throw new ParseError("Unrecognized Ensemble Prediction algorithm '"+algorithmName+"'");
                   else if (!(program instanceof EnsemblePredictionMethod)) throw new ParseError("'"+algorithmName+"' is not an Ensemble Prediction algorithm");
                   else algorithm=(EnsemblePredictionMethod)program;
               } 
               if (targets!=null) {
                   results=targets.trim().split("\\s*,\\s*");
                   int count=0;
                   if (results.length>count && algorithm.returnsSiteResults()) {targetName=results[count].trim();count++;}
                   if (results.length>count && algorithm.returnsMotifResults()) motifCollectionName=results[count].trim();
               }               
               
               if ((targetName==null || targetName.isEmpty()) && algorithm.returnsSiteResults()) throw new ParseError("Missing target dataset for operation 'ensemblePrediction'");
               if ((motifCollectionName==null || motifCollectionName.isEmpty()) && algorithm.returnsMotifResults()) throw new ParseError("Missing motif collection target dataname for operation 'ensemblePrediction'");              
               if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source dataset for operation '"+operationName+"'");
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");

           if (motifprefix==null) motifprefix=algorithmName;
           else { // strip quotes
               if (motifprefix.startsWith("\"")) motifprefix=motifprefix.substring(1);
               if (motifprefix.endsWith("\"")) motifprefix=motifprefix.substring(0,motifprefix.length()-1);
           }
           checkSourceObjects(sourceName,lookup,operation); // checks the list of sourcenames and throws ParseErrors if appropriate
           if (dnaTrackName==null || dnaTrackName.isEmpty()) {
              throw new ParseError("Missing DNA-track for ensemble prediction");
           } else {
               Class dnaclass=lookup.getClassFor(dnaTrackName);
               if (dnaclass==null) throw new ParseError("Unrecognized DNA-track: "+dnaTrackName);
               if (!(DNASequenceDataset.class.isAssignableFrom(dnaclass))) throw new ParseError("'"+dnaTrackName+"' is not a DNA Sequence Dataset");
           }
           if (targetName!=null) {           
               lookup.register(targetName, RegionDataset.class);
               task.addAffectedDataObject(targetName, RegionDataset.class);
           }
           if (motifCollectionName!=null) {           
               lookup.register(motifCollectionName, MotifCollection.class);
               task.addAffectedDataObject(motifCollectionName, MotifCollection.class);
           }

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_ensemblePrediction.MOTIFCOLLECTION, motifCollectionName);
           task.setParameter(Operation_ensemblePrediction.ALGORITHM, algorithmName);
           task.setParameter(Operation_ensemblePrediction.MOTIFPREFIX, motifprefix);
           task.setParameter(Operation_ensemblePrediction.DNA_TRACK_NAME, dnaTrackName);           
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
                   task.setParameter(Operation_ensemblePrediction.ADDITIONAL_RESULTS, additionalResultsNames);
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

    /** Parses and sets the Ensemble Prediction program parameter settings from a string with name-value pairs*/
    private void parseParameterSettings(String text, EnsemblePredictionMethod algorithm, OperationTask task) throws ParseError {
        ParametersParser parametersParser=protocol.getParametersParser();
        try {
           ParameterSettings settings=parametersParser.parse(text, algorithm.getParameters());
           task.setParameter(Operation_ensemblePrediction.PARAMETERS, settings);
        } catch (ParseError parseError) {
          parseError.setLineNumber(task.getLineNumber());
          throw parseError;
        }
    }

    private String getParameterSettingsAsString(EnsemblePredictionMethod algorithm, OperationTask task) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(Operation_ensemblePrediction.PARAMETERS);
        ParametersParser parametersParser=protocol.getParametersParser();
        if (settings!=null) {
            return parametersParser.getCommandString(algorithm.getParameters(), settings);
        }
        else return "";
    }

    private void checkSourceObjects(String sourceNameString, DataTypeTable lookup, Operation operation) throws ParseError {
        String[] sourceNames=sourceNameString.split(",");
        for (int i=0;i<sourceNames.length;i++) {
           String sourceName=sourceNames[i].trim();
           if (sourceName.isEmpty()) throw new ParseError("Missing name for source data object");
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operation.getName()+"'");
        }
    }

}
