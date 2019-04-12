/*
 
 
 */

package motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_new;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_new extends StandardOperationParser {
     
    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String datatype=(String)task.getParameter(Operation_new.DATA_TYPE);
        String parameters=(String)task.getParameter(Operation_new.PARAMETERS);
        String msg=targetName+" = new "+datatype;  
        if (parameters!=null && (parameters.startsWith(Operation_new.FILE_PREFIX) || parameters.startsWith(Operation_new.INPUT_FROM_DATAOBJECT_PREFIX))) {
            if (parameters.startsWith(Operation_new.FILE_PREFIX)) {
                String filename=(String)task.getParameter(Operation_new.FILENAME);
                parameters=Operation_new.FILE_PREFIX+"\""+filename+"\"";
            } else {
                String dataobjectname=(String)task.getParameter(Operation_new.INPUT_DATA_NAME);
                parameters=Operation_new.INPUT_FROM_DATAOBJECT_PREFIX+dataobjectname;
            }
            
            String dataformatname=(String)task.getParameter(Operation_new.DATA_FORMAT);
            if (dataformatname!=null) {
                parameters+=", format="+dataformatname;
                DataFormat formatter=engine.getDataFormat(dataformatname);
                StandardParametersParser spp=(StandardParametersParser)protocol.getParametersParser();
                String settings=spp.getParameterSettingsAsString(formatter,task,Operation_new.DATA_FORMAT_SETTINGS, "input");
                if (!settings.isEmpty()) parameters+=" {"+settings+"}";
            }   
        }
        if (parameters!=null && !parameters.isEmpty()) msg+="("+parameters+")";
        return msg;
    }
    
    public static String getImportFromFileParameter(OperationTask task, MotifLabEngine engine) {
        String filename=(String)task.getParameter(Operation_new.FILENAME);    
        String parameters=Operation_new.FILE_PREFIX+"\""+filename+"\"";
        String dataformatname=(String)task.getParameter(Operation_new.DATA_FORMAT);
        if (dataformatname!=null) {
            parameters+=", format="+dataformatname;
            DataFormat formatter=engine.getDataFormat(dataformatname);
            StandardParametersParser spp=new StandardParametersParser(engine);
            String settings=spp.getParameterSettingsAsString(formatter,task,Operation_new.DATA_FORMAT_SETTINGS, "input");
            if (!settings.isEmpty()) parameters+=" {"+settings+"}";
        }   
        return parameters;
    }    
    
    public static String getImportFromTextVariableParameter(OperationTask task, MotifLabEngine engine) {
        String filename=(String)task.getParameter(Operation_new.FILENAME);    
        String parameters=Operation_new.INPUT_FROM_DATAOBJECT_PREFIX+filename;
        String dataformatname=(String)task.getParameter(Operation_new.DATA_FORMAT);
        if (dataformatname!=null) {
            parameters+=", format="+dataformatname;
            DataFormat formatter=engine.getDataFormat(dataformatname);
            StandardParametersParser spp=new StandardParametersParser(engine);
            String settings=spp.getParameterSettingsAsString(formatter,task,Operation_new.DATA_FORMAT_SETTINGS, "input");
            if (!settings.isEmpty()) parameters+=" {"+settings+"}";
        }   
        return parameters;
    }        

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="new";
           String targetName=null;
           String datatype=null;
           String parameters=null;
           String datatypenames="";
           String[] list = Operation_new.getAvailableTypes();
           for (String typename:list) datatypenames+="(?i)"+typename+"|";
           datatypenames=datatypenames.substring(0, datatypenames.length()-1);
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9.+-]+)\\s*=\\s*)?new ("+datatypenames+")?\\s*(\\((.*)\\))?(\\s*\\S.*)?");  
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               datatype=matcher.group(3);
               parameters=matcher.group(5);
               String unknown=matcher.group(6);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) {
                   if (datatype==null || datatype.isEmpty()) throw new ParseError("Unknown data type: "+unknown);
                   else throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing name for target data object");
           if (datatype==null || datatype.isEmpty()) throw new ParseError("Missing data type specification for 'new' operation");
           if (datatype.equals(Sequence.getType())) {
               String namefail=engine.checkSequenceNameValidity(targetName, false);
               if (namefail!=null) throw new ParseError(namefail+": "+targetName);
           } else {
               String namefail=engine.checkNameValidity(targetName, false);
               if (namefail!=null) throw new ParseError(namefail+": "+targetName);
           }
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(OperationTask.ENGINE, engine);
           task.setParameter(Operation_new.DATA_TYPE, datatype);
           Class dataclass=engine.getDataClassForTypeName(datatype);
           if (dataclass==null) throw new ParseError("Unknown data type: "+datatype);
           lookup.register(targetName, dataclass);
           task.addAffectedDataObject(targetName, dataclass);
           if (parameters!=null) {
               parameters=parameters.trim();
               if (!parameters.isEmpty()) task.setParameter(Operation_new.PARAMETERS, parameters);
               if (parameters.startsWith(Operation_new.FILE_PREFIX) || parameters.startsWith(Operation_new.INPUT_FROM_DATAOBJECT_PREFIX)) {
                  StandardParametersParser spp=(StandardParametersParser)protocol.getParametersParser();
                  parseAndSetImportFromFileParameters(task, parameters, dataclass, spp, engine);
                  if (dataclass==SequenceCollection.class) task.setTurnOffGUInotifications(true); // in case new sequences are added
               }
           }
           return task;
    }
    
    /**
     * Parses a parameter-string which contains an 'import from file' directive
     * and sets the required fields (filename, dataformat and dataformat-settings)
     * in the task based on the specified settings
     * @param task Should be an Operation_new task
     * @param parameters The parameterString (Starting with "FILE-prefix")
     * @param targetClass The class of the target Data object
     * @throws ParseError
     */
    public static void parseAndSetImportFromFileParameters(OperationTask task, String parameters, Class targetClass, StandardParametersParser spp, MotifLabEngine engine) throws ParseError {
       String[] elements=null; 
       boolean fromFile=true;
       String directive="import from file";
        if (parameters.startsWith(Operation_new.FILE_PREFIX)) {
            elements=parseLoadFromFileParameter(parameters.substring(Operation_new.FILE_PREFIX.length()));            
        }
       else if (parameters.startsWith(Operation_new.INPUT_FROM_DATAOBJECT_PREFIX)) {
           elements=parseInputFromDataObjectParameter(parameters.substring(Operation_new.INPUT_FROM_DATAOBJECT_PREFIX.length()));
           directive="input from data object";
           fromFile=false;
       }
       else throw new ParseError("Missing prefix for directive"); 
       String filename=elements[0];
       String dataformatname=elements[1];
       String dataformatsettings=elements[2];
       if (filename==null || filename.isEmpty()) throw new ParseError("Missing "+((fromFile)?"filename":"data object")+" specification for '"+directive+"' directive");
       DataFormat formatter=null;
       if (dataformatname==null || dataformatname.isEmpty()) {
           formatter=engine.getDefaultDataFormat(targetClass);
           if (formatter==null) throw new ParseError("Missing data format specification for '"+directive+"' directive. No default found.");
           else dataformatname=formatter.getName();
       } else formatter=engine.getDataFormat(dataformatname);
       if (formatter==null) throw new ParseError("Unrecognized data format '"+dataformatname+"'");
       if (fromFile) task.setParameter(Operation_new.FILENAME, filename);
       else task.setParameter(Operation_new.INPUT_DATA_NAME, filename);
       task.setParameter(Operation_new.DATA_FORMAT, dataformatname);
       if (spp==null) spp=new StandardParametersParser(engine);
       if (dataformatsettings!=null && !dataformatsettings.isEmpty()) {
           spp.parseFormatParameterSettings(dataformatsettings,formatter,task,Operation_new.DATA_FORMAT_SETTINGS);
       }
    }

    
     /** The returned String array consists of 3 elements:
      * 1) Name of file (String)
      * 2) Name of data format (String)
      * 3) Parameter settings for data format (String) -optional- (could be NULL)
      */
     public static String[] parseLoadFromFileParameter(String text) throws ParseError {
         if (!text.startsWith("\"")) throw new ParseError("Filename must be enclosed in double quotes");
           Pattern pattern=Pattern.compile("^\"(.+?)\"(\\s*,\\s*format\\s*=\\s*(\\S+)(\\s*\\{(.+)\\})?)?");
           Matcher matcher=pattern.matcher(text);
           String filename=null;
           String dataformatname=null;
           String formatsettings=null;
           if (matcher.find()) {
               filename=matcher.group(1);
               dataformatname=matcher.group(3);
               formatsettings=matcher.group(5);
           } else throw new ParseError("Unable to parse parameters for 'import from file' directive: "+text);
           return new String[]{filename,dataformatname,formatsettings};
    }

     /** The returned String array consists of 3 elements:
      * 1) Name of data object (String)
      * 2) Name of data format (String)
      * 3) Parameter settings for data format (String) -optional- (could be NULL)
      */
     public static String[] parseInputFromDataObjectParameter(String text) throws ParseError {
           Pattern pattern=Pattern.compile("^(\\w+)(\\s*,\\s*format\\s*=\\s*(\\S+)(\\s*\\{(.+)\\})?)?");
           Matcher matcher=pattern.matcher(text);
           String filename=null;
           String dataformatname=null;
           String formatsettings=null;
           if (matcher.find()) {
               filename=matcher.group(1);
               dataformatname=matcher.group(3);
               formatsettings=matcher.group(5);
           } else throw new ParseError("Unable to parse parameters for 'input from data object' directive: "+text);
           return new String[]{filename,dataformatname,formatsettings};
    }     
     
}
