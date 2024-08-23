/*
 
 
 */

package motiflab.engine.protocol;

import java.util.ArrayList;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.NumericMap;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.dataformat.DataFormat;


/**
 *
 * @author kjetikl
 */
public class StandardParametersParser extends ParametersParser {
    private DataTypeTable datatypetable=null;
    
    public StandardParametersParser(MotifLabEngine engine) {
        setEngine(engine);
        setProtocol(null);
    }    
    
    public StandardParametersParser(MotifLabEngine engine, Protocol protocol) {
        setEngine(engine);
        setProtocol(protocol);
    }
    
    public StandardParametersParser(MotifLabEngine engine, DataTypeTable datatypetable) {
        setEngine(engine);
        setProtocol(null);
        this.datatypetable=datatypetable;
    }    
    
    public StandardParametersParser() {

    }
    
    /**
     * Given a text string containing parameter settings (for instance a portion from a protocol script)
     * formatted as as "name=value" pairs separated by commas, the method will update the specified parameters 
     * with the given values. ParameterSettings that are not mentioned in the text will be assigned default values
     * 
     * @param text
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public ParameterSettings parse(String text, Parameter[] parameterFormats) throws ParseError {
        ParameterSettings settings=new ParameterSettings();
        if (text==null || text.trim().isEmpty()) return settings;
        if (parameterFormats==null) return settings;
        String[] parts=splitOnComma(text);
        for (String part:parts) {
            if (part.isEmpty()) throw new ParseError("Empty parameter (maybe misplaced comma)");
            String[] fields=part.split("=",2);
            if (fields.length!=2) throw new ParseError("Error in parameters. Not a key=value pair: '"+part+"'");
            String parname=fields[0].trim();
            String parvalueString=fields[1].trim();
            if (!parameterExists(parname, parameterFormats)) throw new ParseError("Unrecognized parameter '"+parname+"'");
            Class type=getTypeForParameter(parname, parameterFormats);            
            Object parvalue=null;
            if (type==null) {
                parvalue=parvalueString;
            } else if (type.equals(Double.class)) {
                  try {
                      double value=Double.parseDouble(parvalueString);
                      parvalue=new Double(value);
                  } catch(NumberFormatException e) {
                      Class currentType=null;
                      if (protocol!=null) currentType=protocol.getDataTypeLookupTable().getClassFor(parvalueString);
                      else if (datatypetable!=null) currentType=datatypetable.getClassFor(parvalueString);
                      else if (engine!=null) currentType=engine.getClassForDataItem(parvalueString);
                      if (currentType==null) throw new ParseError("Unknown data item: "+parvalueString);
                      else if (currentType.equals(NumericVariable.class)) parvalue=parvalueString;
                      else throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is not a valid number or Numeric Variable");
                  }
            } else if (type.equals(Integer.class)) {
                  try {
                      int value=Integer.parseInt(parvalueString);
                      parvalue=new Integer(value);
                  } catch(NumberFormatException e) {
                      Class currentType=null;
                      if (protocol!=null) currentType=protocol.getDataTypeLookupTable().getClassFor(parvalueString);
                      else if (datatypetable!=null) currentType=datatypetable.getClassFor(parvalueString);
                      else if (engine!=null) currentType=engine.getClassForDataItem(parvalueString);
                      if (currentType==null) throw new ParseError("Unknown data item: "+parvalueString);
                      else if (currentType.equals(NumericVariable.class)) parvalue=parvalueString;
                      else throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is not a valid integer or Numeric Variable");
                  }
            } else if (type.equals(Boolean.class)) {
                    if (parvalueString.equalsIgnoreCase("true")) parvalue=Boolean.TRUE;
                    else if (parvalueString.equalsIgnoreCase("false")) parvalue=Boolean.FALSE;
                    else throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is not a valid boolean value");
            } else if (NumericMap.class.isAssignableFrom(type)) {
                  boolean allowNumeric=false;
                  Object allowedValues=getAllowedValuesForParameter(parname, parameterFormats);
                  if (allowedValues instanceof Class[]) {
                      for (Class c:(Class[])allowedValues) {
                          if (c.equals(NumericVariable.class)) {allowNumeric=true;break;}
                      }
                  }
                  if (allowNumeric) {
                      try {
                          double value=Double.parseDouble(parvalueString); // this is just a check
                          parvalue=parvalueString; // the value will be resolved later
                      } catch (NumberFormatException e) {
                          Class currentType=null;
                          if (protocol!=null) currentType=protocol.getDataTypeLookupTable().getClassFor(parvalueString);
                          else if (datatypetable!=null) currentType=datatypetable.getClassFor(parvalueString);
                          else if (engine!=null) currentType=engine.getClassForDataItem(parvalueString);
                          if (currentType==null) throw new ParseError("Unknown data item: "+parvalueString);
                          if (currentType.equals(NumericVariable.class) || type.isAssignableFrom(currentType)) parvalue=parvalueString;
                          else if (NumericMap.class.isAssignableFrom(currentType) && !type.isAssignableFrom(currentType)) throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is a Numeric Map of a type not applicable in this context");
                          else throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is not a valid Numeric Map, Numeric Variable or constant number");
                      }
                  } else { // only maps are allowed
                      Class currentType=null;
                      if (protocol!=null) currentType=protocol.getDataTypeLookupTable().getClassFor(parvalueString);
                      else if (datatypetable!=null) currentType=datatypetable.getClassFor(parvalueString);
                      else if (engine!=null) currentType=engine.getClassForDataItem(parvalueString);
                      if (currentType==null) throw new ParseError("Unknown data item: "+parvalueString);
                      if (type.isAssignableFrom(currentType)) parvalue=parvalueString;
                      else if (NumericMap.class.isAssignableFrom(currentType)) throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is a Numeric Map of a type not applicable in this context");
                      else throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is not a valid Numeric Map");
                  } 
            } else if (Data.class.isAssignableFrom(type)) {
                    Class currentType=null;
                    if (protocol!=null) currentType=protocol.getDataTypeLookupTable().getClassFor(parvalueString);
                    else if (datatypetable!=null) currentType=datatypetable.getClassFor(parvalueString);
                    else if (engine!=null) currentType=engine.getClassForDataItem(parvalueString);
                    if (currentType==null) throw new ParseError("Unknown data item: "+parvalueString);
                    Object av=getAllowedValuesForParameter(parname, parameterFormats);
                    if (!(av instanceof Class || av instanceof Class[])) throw new ParseError("SYSTEM ERROR: 'allowed values' for parameter is neither Class nor Class[]");
                    else if (av instanceof Class) {
                        Class allowedType=(Class)av;
                        if (allowedType.isAssignableFrom(currentType)) parvalue=parvalueString;
                        else throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is not of valid type");
                    } else {
                        if (testAll(currentType,(Class[])av)) parvalue=parvalueString;
                        else throw new ParseError("Value for parameter '"+parname+"="+parvalueString+"' is not of valid type");
                    } 
            } else if (type.equals(String.class)){
                if (parvalueString.startsWith("\"") && parvalueString.endsWith("\"")) {
                    if (parvalueString.length()>2) parvalue=parvalueString.substring(1, parvalueString.length()-1);
                    else parvalue="";
                }
                else parvalue = parvalueString;
            } else {
                parvalue=parvalueString;
            }
            //System.err.println("Parser DataFormat setting: "+parname+"="+parvalue);
            settings.setParameter(parname, parvalue);
            // assign default values to missing parameters
            for(Parameter item:parameterFormats) {
                String pname=item.getName();
                if (settings.getParameterAsString(pname, parameterFormats)==null)  settings.setParameter(pname,item.getDefaultValue());
            }
        }
        return settings;
    }    
    

    
    /**
     * This method returns a string containing a concatenation of all the explicitly set parameters and their values. 
     * The string can be included in protocol scripts and should be parsable by the {@code parseParameterSettings(String)} method
     * @param list A list of parameters to output. If this parameter is not null only the parameters included in the list will be output and in the order given
     * @param settings The values for the parameters
     * @param filter An optional string specifying whether only "input" or only "output" parameters should be included (a null value will include both)
     * @return
     */
    @Override
    public String getCommandString(Parameter[] list, ParameterSettings settings, String filter) {
        StringBuilder string=new StringBuilder();
        boolean isFirst=true;
        if (list==null) { // no list of parameters, just output all settings
          String[] keys=settings.getParameters();
          for (int i=0;i<keys.length;i++) {
              String parametername=keys[i];
              String value=settings.getParameterAsString(parametername,list);
              if (value==null) continue;
              if (!isFirst) string.append(",");
              isFirst=false;
              string.append(parametername);
              string.append("=");       
              string.append(value);
          }
        } else { // output settings of the parameters in the list
          for (int i=0;i<list.length;i++) {
              Parameter parameter=list[i];
              if (parameter.excludeFromProtocol()) continue; // this parameter should never be included
              String parametername=parameter.getName();
              String value=settings.getParameterAsString(parametername,list);
              if (value==null) continue;
              if (isAdvancedAndDefaults(list, settings, parametername, value)) continue; // no need to output this parameter
              //if (isHiddenAndDefaults(list, settings, parametername, value)) continue; // no need to output this parameter
              if (settings.isHidden(parametername, list)) continue; // no need to output this hidden parameter
              if (filter!=null && list[i].getFilterType()!=null && !list[i].getFilterType().equalsIgnoreCase(filter)) continue; // no need to output this parameter
              if (list[i].hasAttributeValue("ui", "password")) continue; // newer include passwords or other secret text attributes in the command line
              if (!isFirst) string.append(",");
              isFirst=false;
              string.append(parametername);
              string.append("=");   
              Class type=settings.getTypeForParameter(parametername, list);              
              if (type==String.class) {
                  string.append("\"");
                  string.append(value);
                  string.append("\"");
              }
              else string.append(value);
          }  
        }
        return string.toString();
    }                
    
    private boolean isHiddenAndDefaults(Parameter[] list, ParameterSettings settings, String parameterName, String stringvalue) {
        if (!settings.isHidden(parameterName, list)) return false;
        Object def=settings.getDefaultValueForParameter(parameterName, list);
        if (def==null) return (stringvalue==null || stringvalue.trim().isEmpty());
        else if (stringvalue==null || stringvalue.isEmpty()) return def.toString().isEmpty();
        else return def.toString().equals(stringvalue);
    }
    
    private boolean isAdvancedAndDefaults(Parameter[] list, ParameterSettings settings, String parameterName, String stringvalue) {
        if (!settings.isAdvanced(parameterName, list)) return false;
        Object def=settings.getDefaultValueForParameter(parameterName, list);
        if (def==null) return (stringvalue==null || stringvalue.trim().isEmpty());
        else if (stringvalue==null || stringvalue.isEmpty()) return def.toString().isEmpty();
        else return def.toString().equals(stringvalue);
    }
     
    /**
     * Returns an object representing the allowed values for the parameter with the given name,
     * or null if no there is no such parameter
     * @param parameterName
     * @param parameterFormats
     * @return
     */
    private Object getAllowedValuesForParameter(String parameterName, Parameter[] parameterFormats) {
        Object def=null;
        if (parameterFormats==null) return null;
        for(Parameter item:parameterFormats) {
            if (item.getName().equals(parameterName)) {def=item.getAllowedValues();break;}
        }
        return def;
    }
    
    /**
     * Returns a Class object representing the type for the parameter with the given name,
     * or null if no there is no such parameter
     * @param parameterName
     * @param parameterFormats
     * @return
     */
    private Class getTypeForParameter(String parameterName, Parameter[] parameterFormats) {
        Class def=null;
        if (parameterFormats==null) return null;
        for(Parameter item:parameterFormats) {
            if (item.getName().equals(parameterName)) {def=item.getType();break;}
        }
        return def;
    }   
    
    /** Returns true if the list of Parameters contains a parameter corresponding to the given name */
    private boolean parameterExists(String parameterName, Parameter[] parameterFormats) {
        if (parameterFormats==null) return false;
        for(Parameter item:parameterFormats) {
            if (item.getName().equals(parameterName)) return true;
        }
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private boolean testAll(Class type, Class[] list) {
        for (Class c:list) {
            if (c.isAssignableFrom(type)) return true;
        }
        return false;
    }
    
    
    
    /** Parses and sets the Data Format parameter settings from a string with name-value pairs
     *  The ParameterSettings which results from this parse is placed among the parameters
     *  of the task using the name provided with the 'taskParameterName' argument
     *  (using: task.setParameter(taskParameterName,parametersettings);)
     *  
     *  @param taskParameterName The name of the task parameter the settings should be stored as 
     *
     */
    protected void parseFormatParameterSettings(String text, DataFormat formatter, OperationTask task, String taskParameterName) throws ParseError {
        ParametersParser parametersParser=(protocol!=null)?protocol.getParametersParser():this; // I am not sure what this is good for (don't remember)
        try {
           ParameterSettings settings=parametersParser.parse(text, formatter.getParameters());
           task.setParameter(taskParameterName, settings);
        } catch (ParseError parseError) {
          parseError.setLineNumber(task.getLineNumber()); 
          throw parseError;
        }       
    } 
    
    protected void parseFormatParameterSettings(String text, Parameter[] parameters, OperationTask task, String taskParameterName) throws ParseError {
        ParametersParser parametersParser=(protocol!=null)?protocol.getParametersParser():this; // I am not sure what this is good for (don't remember)
        try {
           ParameterSettings settings=parametersParser.parse(text, parameters);
           task.setParameter(taskParameterName, settings);
        } catch (ParseError parseError) {
          parseError.setLineNumber(task.getLineNumber()); 
          throw parseError;
        }       
    } 
    
    /** Returns a parameterString for the DataFormat settings which are store in the OperationTask
     *  as a parameter under the name 'taskParameterName'
     */
    protected String getParameterSettingsAsString(DataFormat formatter, OperationTask task, String taskParameterName, String filter) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(taskParameterName);
        if (settings!=null) {
            return getCommandString(formatter.getParameters(), settings, filter);
        }
        else return "";        
    }    
    
    protected String getParameterSettingsAsString(Parameter[] parameters, OperationTask task, String taskParameterName, String filter) {
        ParameterSettings settings=(ParameterSettings)task.getParameter(taskParameterName);
        if (settings!=null) {
            return getCommandString(parameters, settings, filter);
        }
        else return "";        
    }    
    
    /** Splits a text string on commas, except for commas within quotes (") */
    private String[] splitOnComma(String text) throws ParseError {
        if (text.indexOf('\"')<0) return text.split(","); // no quotes in the text
        boolean insidequote=false;
        ArrayList<Integer> unquotedcommapositions=new ArrayList<Integer>();
        for (int i=0;i<text.length();i++) {
            if (text.charAt(i)==',' && !insidequote) {
               unquotedcommapositions.add(i);
            } else if (text.charAt(i)=='\"') {
                insidequote=!insidequote; // toogle inside-outside quotes
            }
        }
        if (insidequote) throw new ParseError("Unclosed quote in parameters");
        // now split up the text on unquoted commas
        if (unquotedcommapositions.isEmpty()) return new String[]{text};
        int substrStart=0;
        String[] result=new String[unquotedcommapositions.size()+1];
        for (int i=0;i<unquotedcommapositions.size();i++) {
            int commapos=unquotedcommapositions.get(i);
            result[i]=text.substring(substrStart,commapos);
            substrStart=commapos+1;
        }
        result[result.length-1]=text.substring(substrStart,text.length());
        return result;
    }

}
