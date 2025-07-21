/*
 
 
 */

package org.motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.operations.Operation_output;
import org.motiflab.engine.dataformat.DataFormat;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_output extends StandardOperationParser {
    

    
    @Override
    public String getCommandString(OperationTask task) {
        String directOutput=(String)task.getParameter(Operation_output.DIRECT_OUTPUT);
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        
        if (directOutput!=null) {
            String msg="";
            directOutput=Operation_output.processDirectOutput(directOutput, false);
            if (targetName!=null && !targetName.equals(sourceName) && !targetName.equals(engine.getDefaultOutputObjectName())) msg+=targetName+" = ";
            msg+="output \""+directOutput+"\"";  
            String directOutputReferences=(String)task.getParameter(Operation_output.DIRECT_OUTPUT_REFERENCES);
            if (directOutputReferences!=null) msg+=","+directOutputReferences;
            return msg;
        } else {
            String outputformatName=(String)task.getParameter(Operation_output.OUTPUT_FORMAT);
            // Data sourceData=engine.getDataItem(sourceName); 
            String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);       
            DataFormat formatter=engine.getDataFormat(outputformatName);
            if (formatter==null) return ("ERROR: Unknown output format '"+outputformatName+"'");
            //if (!formatter.canFormat(sourceData)) return ("Unsupported output format '"+outputformatName+"' for data object '"+sourceName+"'");
            Class sourceClass=protocol.getDataTypeLookupTable().getClassFor(sourceName);
            Analysis analysis=null;
            if (Analysis.class.isAssignableFrom(sourceClass)) analysis=engine.getAnalysisForClass(sourceClass);
            String msg="";
            if (targetName!=null && !targetName.equals(sourceName) && !targetName.equals(engine.getDefaultOutputObjectName())) msg+=targetName+" = ";
            msg+="output "+sourceName+" in "+outputformatName+" format";        
            StandardParametersParser spp=(StandardParametersParser)protocol.getParametersParser();
            String settings="";
            if (analysis!=null) settings=spp.getParameterSettingsAsString(analysis.getOutputParameters(outputformatName),task,Operation_output.OUTPUT_FORMAT_PARAMETERS, "output");
            else settings=spp.getParameterSettingsAsString(formatter,task,Operation_output.OUTPUT_FORMAT_PARAMETERS, "output");
            if (!settings.isEmpty()) msg+=" {"+settings+"}";
            if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;        
            return msg;
        }
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="output";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String outputFormatName=null;
           String outputFormatSettings=null;
           if (command.matches("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?output\\s+\".*")) { // output "...
               return parseDirectOutput(command);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?output ([a-zA-Z_0-9.+-]+)?( in (\\w+) format(\\s*\\{(.*)\\})?)?( in collection ([a-zA-Z_0-9-]+))?( \\S.*)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               outputFormatName=matcher.group(5);
               outputFormatSettings=matcher.group(7);
               sequenceCollection=matcher.group(9);
               String unknown=matcher.group(10);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for output operation");
           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");

           DataFormat formatter=null;
           if (outputFormatName!=null) formatter=engine.getDataFormat(outputFormatName);
           if (outputFormatName!=null && formatter==null) throw new ParseError("Unrecognized output format '"+outputFormatName+"'");
           if (outputFormatSettings!=null && !outputFormatSettings.isEmpty()) {   
               StandardParametersParser spp=(StandardParametersParser)protocol.getParametersParser();
               if (Analysis.class.isAssignableFrom(sourceclass)) {
                   Parameter[] parameters=engine.getAnalysisForClass(sourceclass).getOutputParameters(outputFormatName);
                   spp.parseFormatParameterSettings(outputFormatSettings,parameters,task,Operation_output.OUTPUT_FORMAT_PARAMETERS);
               } 
               else spp.parseFormatParameterSettings(outputFormatSettings,formatter,task,Operation_output.OUTPUT_FORMAT_PARAMETERS);
           }
      
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_output.OUTPUT_FORMAT, outputFormatName);
           if (targetName!=null && !targetName.isEmpty()) {
               task.setParameter(OperationTask.TARGET_NAME, targetName);
               Class oldclass=lookup.getClassFor(targetName);
               if (oldclass!=null && oldclass!=OutputData.class) throw new ParseError("Unable to output to "+targetName+". Target data object has wrong type!");
               lookup.register(targetName, OutputData.class);
               task.addAffectedDataObject(targetName, OutputData.class);
           } else {
               lookup.register(engine.getDefaultOutputObjectName(), OutputData.class);
               task.addAffectedDataObject(engine.getDefaultOutputObjectName(), OutputData.class);
           }
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }
    

    private OperationTask parseDirectOutput(String command) throws ParseError {
       String operationName="output";
       String targetName=null;    
       String directOutputString=null;
       String references=null;
       OperationTask task=new OperationTask(operationName);
       Operation operation=engine.getOperation(operationName);
       if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
       Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?output\\s+\"((?:\\\\\\\\|\\\\\\w|\\\\\"|[^\"\\\\])*?)\"\\s*(\\S.*)?");
       Matcher matcher=pattern.matcher(command);
       if (matcher.find()) {
           targetName=matcher.group(2);
           directOutputString=matcher.group(3);
           references=matcher.group(4);
           if (references!=null && !references.isEmpty()) {
               if (references.startsWith(",")) references=references.substring(1).trim();
               // else throw new ParseError("Unrecognized clause (or wrong order): "+references);
           }
           // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
       } else throw new ParseError("Unable to parse "+operationName+" command. Perhaps the text is not properly enclosed in double qoutes.");       
       directOutputString=Operation_output.processDirectOutput(directOutputString,true);
       task.setParameter(OperationTask.OPERATION, operation);
       task.setParameter(OperationTask.OPERATION_NAME, operationName);
       task.setParameter(Operation_output.DIRECT_OUTPUT, directOutputString);       
       DataTypeTable lookup=protocol.getDataTypeLookupTable();       
       if (targetName!=null && !targetName.isEmpty()) {
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           Class oldclass=lookup.getClassFor(targetName);
           if (oldclass!=null && oldclass!=OutputData.class) throw new ParseError("Unable to output to "+targetName+". Target data object has wrong type!");
           lookup.register(targetName, OutputData.class);
           task.addAffectedDataObject(targetName, OutputData.class);
       } else {
           lookup.register(engine.getDefaultOutputObjectName(), OutputData.class);
           task.addAffectedDataObject(engine.getDefaultOutputObjectName(), OutputData.class);
       }
       if (references!=null && !references.isEmpty()) {
           String[] referencelist=references.split("\\s*,\\s*");
           for (String ref:referencelist) {
              Class refclass=lookup.getClassFor(ref); 
              if (refclass==null) throw new ParseError("Unknown data object: "+ref);
              else if (!(refclass==NumericVariable.class || refclass==TextVariable.class || DataCollection.class.isAssignableFrom(refclass))) {
                  throw new ParseError("Unable to include value for \""+ref+"\" in direct output (data type not supported: "+engine.getTypeNameForDataClass(refclass)+")");
              }
           }
           task.setParameter(Operation_output.DIRECT_OUTPUT_REFERENCES, references);   
       }
       return task;
           
    }

}
