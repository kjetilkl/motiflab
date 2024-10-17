/*
 
 
 */

package motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.data.*;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.operations.Operation_plant;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_plant extends StandardOperationParser {



    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String sourceName=task.getSourceDataName();
        String motifCollectionName=(String)task.getParameter(Operation_plant.MOTIFS_NAME);
        String targetSitesTrackName=(String)task.getParameter(Operation_plant.TARGET_SITES_TRACK);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        Operation_plant plantoperation=(Operation_plant)engine.getOperation("plant");
        String parameters=getParameterSettingsAsString(task,plantoperation);
        String msg="["+targetName+","+targetSitesTrackName+"] = plant "+motifCollectionName+" in "+sourceName;
        if (parameters!=null && !parameters.isEmpty()) msg+=" {"+parameters+"}";
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }


    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="plant";
           Operation_plant plantoperation=(Operation_plant)engine.getOperation(operationName);
           String sourceName=null;
           String targetName=null;
           String targetSitesTrackName=null;
           String plantObjectName=null;

           String parameters=null;
           String sequenceCollection=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(\\[\\s*([a-zA-Z_0-9-]+)\\s*,\\s*([a-zA-Z_0-9-]+)\\s*\\]\\s*=\\s*)?plant ([a-zA-Z_0-9-]+) in ([a-zA-Z_0-9-]+)(\\s*\\{(.*)\\})?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               targetName=matcher.group(2);
               targetSitesTrackName=matcher.group(3);
               plantObjectName=matcher.group(4);
               sourceName=matcher.group(5);
               parameters=matcher.group(7);
               String unknown=matcher.group(8);
               if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing target DNA dataset for operation 'plant'");
               if (targetSitesTrackName==null || targetSitesTrackName.isEmpty()) throw new ParseError("Missing target Region Dataset for operation 'plant'");
               if (plantObjectName==null || plantObjectName.isEmpty()) throw new ParseError("Missing motif collection target dataname for operation 'plant'");
               if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source dataset for operation 'plant'");
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is not a DNA Sequence dataset");

           Class plantObjectclass=lookup.getClassFor(plantObjectName);
           //if (plantObjectclass==null) throw new ParseError("Unrecognized object: "+plantObjectName);
           if (plantObjectclass!=null && !(plantObjectclass==Motif.class || plantObjectclass==MotifCollection.class || plantObjectclass==ModuleCRM.class)) throw new ParseError("'"+plantObjectName+"' is not a Motif, Motif Collection or Module");

           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=RegionDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object exists and is not a Region Dataset!");
           lookup.register(targetName, DNASequenceDataset.class);
           task.addAffectedDataObject(targetName, DNASequenceDataset.class);
           lookup.register(targetSitesTrackName, RegionDataset.class);
           task.addAffectedDataObject(targetSitesTrackName, RegionDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_plant.TARGET_SITES_TRACK, targetSitesTrackName);
           task.setParameter(Operation_plant.MOTIFS_NAME, plantObjectName);

           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);

           parseParameterSettings(parameters,task,plantoperation);
           return task;
    }

    /** Parses and sets specific parameter settings from a string with name-value pairs*/
    private void parseParameterSettings(String text, OperationTask task, Operation_plant plantoperation) throws ParseError {
        ParametersParser parametersParser=protocol.getParametersParser();
        try {
           ParameterSettings settings=parametersParser.parse(text, plantoperation.getParameters());
           task.setParameter(Operation_plant.PARAMETERS, settings);
        } catch (ParseError parseError) {
          parseError.setLineNumber(task.getLineNumber());
          throw parseError;
        }
    }

    private String getParameterSettingsAsString(OperationTask task, Operation_plant plantoperation) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(Operation_plant.PARAMETERS);
        ParametersParser parametersParser=protocol.getParametersParser();
        if (settings!=null) {
            return parametersParser.getCommandString(plantoperation.getParameters(), settings);
        }
        else return "";
    }

}
