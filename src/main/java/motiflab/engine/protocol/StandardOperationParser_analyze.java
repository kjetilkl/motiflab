/*
 
 
 */

package motiflab.engine.protocol;

import motiflab.engine.data.analysis.Analysis;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.operations.Operation_analyze;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_analyze extends StandardOperationParser {
    

    
    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String analysisName=(String)task.getParameter(Operation_analyze.ANALYSIS);
        Analysis analysis=engine.getAnalysis(analysisName);
        if (analysis==null) return "ERROR: Unknown analysis '"+analysisName+"'";
        String parameters=getParameterSettingsAsString(analysis,task);      
        String msg=targetName+" = analyze "+analysisName;   
        if (parameters!=null && !parameters.isEmpty()) msg+=" {"+parameters+"}";
        return msg;
    }
    

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="analyze";
           String targetName=null;
           String analysisName=null;
           String parameters=null;
           String[] list = engine.getAnalysisNames();
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?analyze ([a-zA-Z_0-9- ]+)?(\\s*\\{(.*)\\})?(\\s*\\S.*)?"); // note that spaces are allowed in analysis names
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               targetName=matcher.group(2);
               analysisName=matcher.group(3);
               if (analysisName!=null) analysisName=analysisName.trim();
               parameters=matcher.group(5);
               String unknown=matcher.group(6);
               if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing target data object for operation 'analyze'");
               if (analysisName==null || analysisName.isEmpty()) throw new ParseError("Missing analysis name");
               else if (!inStringArray(list, analysisName)) throw new ParseError("Unknown analysis: "+analysisName);
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");

           Analysis analysis=engine.getAnalysis(analysisName);
           if (analysis==null) throw new ParseError("Unrecognized analysis '"+analysisName+"'");
     
           lookup.register(targetName, engine.getClassForAnalysis(analysisName));    
           task.addAffectedDataObject(targetName, engine.getClassForAnalysis(analysisName));
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           //task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_analyze.ANALYSIS, analysisName);           
           if (analysis!=null && parameters!=null && !parameters.isEmpty()) {               
               parseParameterSettings(parameters,analysis,task);
           }              
           return task;
    }

    
    private boolean inStringArray(String[] list,String element) {
        for (int i=0;i<list.length;i++) {
            if (list[i].equals(element)) return true;
        }
        return false;
    }    


    /** Parses and sets the analysis parameter settings from a string with name-value pairs*/
    private void parseParameterSettings(String text, Analysis analysis, OperationTask task) throws ParseError {
        ParametersParser parametersParser=protocol.getParametersParser();
        try {
           ParameterSettings settings=parametersParser.parse(text, analysis.getParameters());
           task.setParameter(Operation_analyze.PARAMETERS, settings);
        } catch (ParseError parseError) {
          parseError.setLineNumber(task.getLineNumber()); 
          throw parseError;
        }       
    } 
    
    private String getParameterSettingsAsString(Analysis analysis, OperationTask task) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(Operation_analyze.PARAMETERS);
        ParametersParser parametersParser=protocol.getParametersParser();
        if (settings!=null) {
            return parametersParser.getCommandString(analysis.getParameters(), settings);
        }
        else return "";        
    }    
    
}
