/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.DataMap;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_MapExpression extends DataFormat {
    private String name="MapExpression";
    private Class[] supportedTypes=new Class[]{DataMap.class};
    private static String KEY_GROUP="Key group";
    private static String VALUE_GROUP="Value group";
    private static String defaultExpression="{KEY}={VALUE}";


    public DataFormat_MapExpression() {
        addParameter("Expression",defaultExpression, null,"<html>When outputting a Map in MapExpression format the expression parameter<br>should be a string which contains two special field codes: {KEY} and {VALUE}.<br>(The braces must be included and the letters must be in uppercase).<br>The KEY field will be replaced by the name (identifier) of the data object<br>(motif, module or sequence) and the VALUE field will be replaced by the value.<br>E.g. the expression \"{KEY}={VALUE}\" will output the name of the data object<br>and the value separated by an equals-sign, whereas the expression \"ENTRY\\t{VALUE}\\t{KEY}\"<br>will output three TAB-separated columns on each row where the first column always<br>has the text \"ENTRY\", the second column is the value and the last column is<br>the name of the data object.<br><br>When importing a file in MapExpression format, the expression should be <br>a regular expression string (formatted according to JAVA regex rules)<br>containing at least two \"capturing groups\" enclosed in parenthesis.<br>The two capturing groups should match the data name (KEY) and value respectively.<br>The integer parameters 'Key group' and 'Value group' are used to tell<br>MotifLab which of the groups are associated with each of these fields.<br>E.g. if the entries in the file correspond to the (output) expression \"{KEY}={VALUE}\",<br>then the input expression could be \"(\\S+?)=(\\S+)\" with the value of 'Key group'<br>set to 1 and 'Value group' set to 2. <br>If the file is in the format \"ENTRY\\t{VALUE}\\t{KEY}\", then the input expression<br>\"ENTRY\\t(\\S+)\\t(\\S+)\" can be used with 'Key group' set to 2 and 'Value group'<br>set to 1 (since the key now occurs after the value in each line).<br>It is possible to use more than two capturing groups, and the 'Key group'<br>and 'Value group' parameters must then be adjusted accordingly.<br><br>Note that double quotes should preferably be avoided in the expression string.</html>");
        addOptionalParameter(KEY_GROUP, new Integer(1), new Integer[]{1,Integer.MAX_VALUE},"The number of the capture group which will capture the KEY (name of entry)");
        addOptionalParameter(VALUE_GROUP, new Integer(2), new Integer[]{1,Integer.MAX_VALUE},"The number of the capture group which will capture the VALUE");
        addOptionalParameter("Include entries",null, new Class[]{DataCollection.class},"Specifies which entries to include in the output. The default is to include all entries from the map.");
        addOptionalParameter("Include default",Boolean.TRUE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Include an entry in the output for the default value ");
       
        setParameterFilter(KEY_GROUP,"input");   
        setParameterFilter(VALUE_GROUP,"input");
        setParameterFilter("Include entries","output");        
        //setParameterFilter("Include default","output");         
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof DataMap); 
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (DataMap.class.isAssignableFrom(dataclass));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof DataMap);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (DataMap.class.isAssignableFrom(dataclass));
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "txt";
    }

    @Override
    public String[] getSuffixAlternatives() {return new String[]{"txt","csv","tsv"};}
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        String expression="";
        DataCollection includeEntries;
        boolean includeDefault=false;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             expression=(String)settings.getResolvedParameter("Expression",defaults,engine);
             includeEntries=(DataCollection)settings.getResolvedParameter("Include entries",defaults,engine);
             includeDefault=(Boolean)settings.getResolvedParameter("Include default",defaults,engine);             
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           expression=(String)getDefaultValueForParameter("Expression");
           includeEntries=(DataCollection)getDefaultValueForParameter("Include entries");
           includeDefault=(Boolean)getDefaultValueForParameter("Include default");           
        }
        if (expression!=null) { // replace escape characters
            expression=expression.replace("\\\\", "\\"); // escaped \
            expression=expression.replace("\\t", "\t"); // escaped TAB
            expression=expression.replace("\\n", "\n"); // escaped newline           
        }         
        
        DataMap map=(DataMap)dataobject;
        if (includeEntries!=null && !includeEntries.getMembersClass().equals(map.getMembersClass())) includeEntries=null;
        
        StringBuilder outputString=new StringBuilder();
        ArrayList<String> keys=(includeEntries==null)?map.getAllKeys(engine):includeEntries.getValues();
        Collections.sort(keys);
        int size=keys.size();
        int i=0;
        for (String key:keys) { // for each entry
              String line=expression.replace("{KEY}", key);
              line=line.replace("{VALUE}", map.getValue(key).toString());
              outputString.append(line);
              outputString.append("\n");
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%100==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        if (includeDefault) {        
              String line=expression.replace("{KEY}", DataMap.DEFAULT_KEY);
              line=line.replace("{VALUE}", map.getValue().toString());
              outputString.append(line);
              outputString.append("\n");                
        }
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_MapFormat.parseInput(ArrayList<String> input, Data target)");
        else if (!(target instanceof DataMap)) throw new ParseError("Unable to parse input to target data of type '"+target.getTypeDescription()+"' using data format "+getName());
        ((DataMap)target).clear();
        ((DataMap)target).clearDefault();
        String expression="";
        int key_group=1;
        int value_group=2;
        setProgress(5);
        if (settings!=null) {
          try {
             Parameter[] defaults=getParameters();
             expression=(String)settings.getResolvedParameter("Expression",defaults,engine);
             key_group=(Integer)settings.getResolvedParameter(KEY_GROUP,defaults,engine);             
             value_group=(Integer)settings.getResolvedParameter(VALUE_GROUP,defaults,engine);
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
           expression=(String)getDefaultValueForParameter("Expression");
           key_group=(Integer)getDefaultValueForParameter(KEY_GROUP);
           value_group=(Integer)getDefaultValueForParameter(VALUE_GROUP);
        }
        if (expression!=null) { // replace escape characters
            expression=expression.replace("\\\\", "\\"); // escaped \
            expression=expression.replace("\\t", "\t"); // escaped TAB
            expression=expression.replace("\\n", "\n"); // escaped newline           
        }          
        if (!expression.contains("(")) throw new ParseError("Expression lacks capturing groups for parsing input. Please consult the user manual.");
        if (key_group==value_group) throw new ParseError("The '"+KEY_GROUP+"' parameter can not have the same value as the '"+VALUE_GROUP+"' parameter");
        
        Pattern pattern=Pattern.compile(expression);
        int lineNumber=0;
        for (String line:input) {
            lineNumber++;
            //if (line.startsWith("#")) continue; // skip comment lines
            line=line.trim();
            Matcher matcher=pattern.matcher(line);
            if (matcher.find()) {
                String key=(key_group>matcher.groupCount())?null:matcher.group(key_group);
                String valueString=(value_group>matcher.groupCount())?null:matcher.group(value_group);
                if (key==null || valueString==null) continue; // not a good match. Should I throw an exception?               
                if (key.equals(DataMap.DEFAULT_KEY)) ((DataMap)target).setDefaultValueFromString(valueString);
                else {
                     if (target instanceof SequenceNumericMap) {
                        key=convertIllegalSequenceNamesIfNecessary(key, false);
                        String error=engine.checkSequenceNameValidity(key, false);
                        if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+key+"' : "+error, lineNumber);                              
                     }                                                
                     ((DataMap)target).setValueFromString(key,valueString);
                }
             
            } 
        }
        return target;
    }



}
