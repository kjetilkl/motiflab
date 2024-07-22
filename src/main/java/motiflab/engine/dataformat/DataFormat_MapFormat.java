/*
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Collections;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.DataMap;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.TextMap;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_MapFormat extends DataFormat {
    private String name="MapFormat";
    private Class[] supportedTypes=new Class[]{DataMap.class};


    public DataFormat_MapFormat() {
        addOptionalParameter("Entry separator","Newline", new String[]{"Newline","Space","Tab","Comma","Semicolon","Colon"},"The character that separates each key-value pair");
        addOptionalParameter("Key-value separator","Equals", new String[]{"Space","Tab","Comma","Semicolon","Colon","Equals"},"The character that separates the value from the key in a key-value pair");
        addOptionalParameter("Include entries",null, new Class[]{DataCollection.class},"Specifies which entries to include in the output. The default is to include all entries from the map.");
        addOptionalParameter("Include default",Boolean.TRUE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Include an entry in the output for the default value.");
        addOptionalParameter("Reverse order",Boolean.FALSE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"If the order is reversed, the value comes before the key");
        addOptionalParameter("Allow duplicates",Boolean.FALSE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"If TRUE, duplicate entries for a single non-numeric data item will be added as a comma-separated list. If FALSE, duplicate entries will overwrite previous values for the same data item.");
        setParameterFilter("Include entries","output");   
        setParameterFilter("Allow duplicates","input");   
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
        String entryseparator;
        String keyvalueseparator;
        DataCollection includeEntries;
        boolean includeDefault=false;
        boolean reverseOrder=false;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             entryseparator=(String)settings.getResolvedParameter("Entry separator",defaults,engine);
             keyvalueseparator=(String)settings.getResolvedParameter("Key-value separator",defaults,engine);
             includeEntries=(DataCollection)settings.getResolvedParameter("Include entries",defaults,engine);
             includeDefault=(Boolean)settings.getResolvedParameter("Include default",defaults,engine);   
             reverseOrder=(Boolean)settings.getResolvedParameter("Reverse order",defaults,engine);              
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           entryseparator=(String)getDefaultValueForParameter("Entry separator");
           keyvalueseparator=(String)getDefaultValueForParameter("Key-value separator");
           includeEntries=(DataCollection)getDefaultValueForParameter("Include entries");
           includeDefault=(Boolean)getDefaultValueForParameter("Include default");      
           reverseOrder=(Boolean)getDefaultValueForParameter("Reverse order");             
        }
        entryseparator=getSeparatorCharacter(entryseparator);
        keyvalueseparator=getSeparatorCharacter(keyvalueseparator);
        DataMap map=(DataMap)dataobject;        
        if (includeEntries!=null && !includeEntries.getMembersClass().equals(map.getMembersClass())) includeEntries=null;

        StringBuilder outputString=new StringBuilder();
        ArrayList<String> keys=(includeEntries==null)?map.getAllKeys(engine):includeEntries.getValues();
        MotifLabEngine.sortNaturalOrder(keys, true);
        //Collections.sort(keys);
        int size=keys.size();
        int i=0;
        for (String key:keys) { // for each entry
              if (i>0) outputString.append(entryseparator);
              Object value=map.getValue(key);
              outputString.append((reverseOrder)?value:key);
              outputString.append(keyvalueseparator);
              outputString.append((reverseOrder)?key:value);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%100==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        if (includeDefault) {
           if (i>0) outputString.append(entryseparator);
           Object value=map.getValue();
           outputString.append((reverseOrder)?value:DataMap.DEFAULT_KEY);
           outputString.append(keyvalueseparator);
           outputString.append((reverseOrder)?DataMap.DEFAULT_KEY:value);           
        }
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }

    private String getSeparatorCharacter(String separatorString) {
             if (separatorString==null) return "";
        else if (separatorString.equalsIgnoreCase("Tab")) return "\t";
        else if (separatorString.equalsIgnoreCase("NewLine")) return "\n";
        else if (separatorString.equalsIgnoreCase("Space")) return " ";
        else if (separatorString.equalsIgnoreCase("Comma")) return ",";
        else if (separatorString.equalsIgnoreCase("Colon")) return ":";
        else if (separatorString.equalsIgnoreCase("Semicolon")) return ";";
        else if (separatorString.equalsIgnoreCase("Equals")) return "=";    
        else return separatorString;
    }



    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_MapFormat.parseInput(ArrayList<String> input, Data target)");
        else if (!(target instanceof DataMap)) throw new ParseError("Unable to parse input to target data of type '"+target.getTypeDescription()+"' using data format "+getName());
        ((DataMap)target).clear();
        ((DataMap)target).clearDefault();
        String entryseparator;
        String keyvalueseparator;
        boolean allowDuplicates=false;
        boolean reverseOrder=false;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             entryseparator=(String)settings.getResolvedParameter("Entry separator",defaults,engine);
             keyvalueseparator=(String)settings.getResolvedParameter("Key-value separator",defaults,engine);
             allowDuplicates=(Boolean)settings.getResolvedParameter("Allow duplicates",defaults,engine);     
             reverseOrder=(Boolean)settings.getResolvedParameter("Reverse order",defaults,engine);              
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
           entryseparator=(String)getDefaultValueForParameter("Entry separator");
           keyvalueseparator=(String)getDefaultValueForParameter("Key-value separator");
           allowDuplicates=(Boolean)getDefaultValueForParameter("Allow duplicates");  
           reverseOrder=(Boolean)getDefaultValueForParameter("Reverse order");              
        }
        entryseparator=getSeparatorCharacter(entryseparator);
        keyvalueseparator=getSeparatorCharacter(keyvalueseparator);
        if (entryseparator.equals(keyvalueseparator)) throw new ParseError("Unable to parse entries when entryseparator is the same as key-value separator '"+entryseparator+"'");
        if (!entryseparator.equals(" ")) entryseparator="\\x20*"+entryseparator+"\\x20*"; // regex with 0 or more spaces on each side of separator 
        if (!keyvalueseparator.equals(" ")) keyvalueseparator="\\x20*"+keyvalueseparator+"\\x20*"; // regex with 0 or more spaces on each side of separator 
        int lineNumber=0;
        for (String line:input) {
            lineNumber++;
            if (line.startsWith("#")) continue; // skip comment lines
            line=line.trim();
            String[] entries=line.split(entryseparator);
            for (String entry:entries) {
                String[] keyvalue=entry.split(keyvalueseparator,2);
                if (keyvalue.length!=2) continue; //not a key-value entry?
                String key=keyvalue[(reverseOrder)?1:0];
                String valueString=keyvalue[(reverseOrder)?0:1];
                if (key.equals(DataMap.DEFAULT_KEY)) {
                     if (allowDuplicates && target instanceof TextMap) {
                        Object currentValue=((DataMap)target).getValue();
                        String currentString=(currentValue!=null)?currentValue.toString():"";
                        String newValue=(currentString.trim().isEmpty())?valueString:(currentString+","+valueString);
                        ((DataMap)target).setDefaultValueFromString(newValue);
                     }                    
                     else ((DataMap)target).setDefaultValueFromString(valueString);
                }
                else {
                     if (((DataMap)target).getMembersClass().equals(Sequence.class)) {
                        key=convertIllegalSequenceNamesIfNecessary(key, false);
                        String error=engine.checkSequenceNameValidity(key, false);
                        if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+key+"' : "+error, lineNumber);                              
                     }
                     if (allowDuplicates && target instanceof TextMap) {
                        Object currentValue=((DataMap)target).getValue(key);
                        String currentString=(currentValue!=null)?currentValue.toString():"";
                        String newValue=(currentString.trim().isEmpty())?valueString:(currentString+","+valueString);
                        ((DataMap)target).setValueFromString(key,newValue);
                     }
                     else ((DataMap)target).setValueFromString(key,valueString);
                }             
            }
        }
        return target;
    }

}
