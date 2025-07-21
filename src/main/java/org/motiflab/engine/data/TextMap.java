/*
 
 
 */

package org.motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public abstract class TextMap extends DataMap {
    protected String name;    
    protected HashMap<String,String> values=new HashMap<String,String>();
    protected String defaultvalue="";

    
    @Override
    public String getName() {return name;}

    @Override
    public void rename(String newname) {
        this.name=newname;
    }    

    public void setDefaultValue(String defaultvalue) {
        if (defaultvalue!=null && defaultvalue.startsWith("\"") && defaultvalue.endsWith("\"")) defaultvalue=defaultvalue.substring(1,defaultvalue.length()-1);        
        this.defaultvalue=(defaultvalue==null)?"":defaultvalue;
    }


    @Override
    public String getValue() {
        return defaultvalue;
    }

    @Override
    public String getValueAsParameterString() { // escape values with double quotes
        StringBuilder string=new StringBuilder();
        for (String key:values.keySet()) {
            string.append(key);
            string.append('=');
            string.append('"');            
            string.append(getSafeValue(values.get(key)));
            string.append('"');   
            string.append(',');
        }
        if (defaultvalue!=null && !defaultvalue.isEmpty()) {
            string.append(DEFAULT_KEY);
            string.append('=');
            string.append('"');              
            string.append(getSafeValue(defaultvalue));
            string.append('"');  
        }
        return string.toString();
    }
    
    private String getSafeValue(String value) { // return a value that can safely be included in a comma-separated list string
       if (value==null || value.isEmpty()) return "";
       if (value.contains("\"")) value=value.replace("\"", "\\\""); // escape double quotes
       if (value.contains(",") || value.contains("\"")) value="\""+value+"\""; // enclose in double quotes if value contains commas or quotes
       return value;
    }

    @Override
    public String output() { // output one entry on each line. No need to escape values
        StringBuilder string=new StringBuilder();
        for (String key:values.keySet()) {
            string.append(key);
            string.append('=');            
            string.append(values.get(key));
            string.append('\n');
        }
        if (defaultvalue!=null && !defaultvalue.isEmpty()) {        
            string.append(DEFAULT_KEY);
            string.append('=');
            string.append(defaultvalue);
            string.append('\n');
        }
        return string.toString();
    }

    /**
     * Sets a text value to be associated with the given data object
     * @param dataname
     * @param value
     */
    public void setValue(String dataname, String value) {
        if (value!=null && value.startsWith("\"") && value.endsWith("\"")) value=value.substring(1,value.length()-1);
        if (value==null) value="";
        values.put(dataname, value);
    }
    
    @Override
    public void setValueFromString(String dataname, String value) throws ParseError {
        setValue(dataname, value);
    }    

   @Override
    public void setDefaultValueFromString(String value) throws ParseError {
        if (value!=null && value.startsWith("\"") && value.endsWith("\"")) value=value.substring(1,value.length()-1);       
        setDefaultValue(value);
    }      
    
    /**
     * Removes the current mapping for the given data object
     * if getValue is called for this data object in the future, the default value will be returned
     * @param dataname
     * @param value
     */
    public void removeValue(String dataname) {
        values.remove(dataname);
    }

    @Override
    public void clear() {
        values.clear();
        clearConstructorString();
    }
    
    @Override
    public void clearDefault() {
        defaultvalue="";
    }
    
    @Override
    public String getValue(String dataname) {
        String val=values.get(dataname);
        if (val==null) return defaultvalue;
        else return val;
    }

    @Override
    public boolean contains(String dataname) {
        String val=values.get(dataname);
        return (val!=null);
    }


    
    /**
     * Returns the (original) HashMap containing the key-value pairs registered
     * in this TextMap
     * @return
     */
    public HashMap<String,String> getValues() {
        return values;
    }

    @Override
    public HashMap<String,Object> getColumnData() {
        HashMap<String,Object> map=new HashMap<String,Object>();
        for (String key:values.keySet()) map.put(key,values.get(key));
        return map;
    }
    /**
     * Returns the values (just the values) for the keys in the list
     * @return
     */
    public String[] getValues(ArrayList<String> list) {
        String[] result=new String[list.size()];
        for (int i=0;i<list.size();i++) {
            String key=list.get(i);
            if (values.containsKey(key)) result[i]=values.get(key);
            else result[i] = defaultvalue;
        }
        return result;
    }
    
    @Override
    public int getNumberOfSpecificallyAssignedEntries() {
        return values.size();
    }

    @Override
    public ArrayList<String> getRegisteredKeys() {
        ArrayList<String> list=new ArrayList<String>();
        list.addAll(values.keySet());
        return list;
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        TextMap datasource=(TextMap)source;
        this.name=datasource.name;
        this.defaultvalue=datasource.defaultvalue;
        this.values=datasource.values; 
    }

    /**
     * Returns true if this TextMap equals the other given TextMap
     * (i.e. it contains the same mappings and other properties such as the constructor is also the same (checked in subclasses)
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof TextMap)) return false;
        TextMap other=(TextMap)data;
        if ((this.defaultvalue==null && other.defaultvalue!=null) || (this.defaultvalue!=null && other.defaultvalue==null)) return false;
        if (this.defaultvalue!=null && other.defaultvalue!=null && !this.defaultvalue.equals(other.defaultvalue)) return false;
        if (((String)this.defaultvalue) == null ? ((String)other.defaultvalue) != null : !((String)this.defaultvalue).equals((String)other.defaultvalue)) return false;
        if (other.values.size()!=this.values.size()) return false;
        for (String key:values.keySet()) {
            if (!other.contains(key)) return false;
            String thisValue=values.get(key);
            String otherValue=other.values.get(key);
            if ((thisValue==null && otherValue!=null) || (thisValue!=null && otherValue==null)) return false;
            if (thisValue!=null && otherValue!=null && !thisValue.equals(otherValue)) return false;
        }
        return true;
    }

    @Override 
    public final boolean containsSameMappings(DataMap data) {
        if (data==null || !(data instanceof TextMap)) return false;
        TextMap other=(TextMap)data;
        if ((this.defaultvalue==null && other.defaultvalue!=null) || (this.defaultvalue!=null && other.defaultvalue==null)) return false;
        if (this.defaultvalue!=null && other.defaultvalue!=null && !this.defaultvalue.equals(other.defaultvalue)) return false;
        if (((String)this.defaultvalue) == null ? ((String)other.defaultvalue) != null : !((String)this.defaultvalue).equals((String)other.defaultvalue)) return false;
        if (other.values.size()!=this.values.size()) return false;
        for (String key:values.keySet()) {
            if (!other.contains(key)) return false;
            String thisValue=values.get(key);
            String otherValue=other.values.get(key);
            if ((thisValue==null && otherValue!=null) || (thisValue!=null && otherValue==null)) return false;
            if (thisValue!=null && otherValue!=null && !thisValue.equals(otherValue)) return false;
        }
        return true;       
    }    
    

    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        values.clear();
        java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("(\\S+)\\s*[=\\t]\\s*(\\S.*)");
        for (String line:input) {
            line=line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            java.util.regex.Matcher matcher=pattern.matcher(line);
            if (matcher.matches()) {
                String dataName=matcher.group(1);
                String value=matcher.group(2);
                if (dataName.equals(DEFAULT_KEY)) setDefaultValue(value);
                else setValue(dataName, value);           
            } else { // Line is not in standard "key = value" format. Check if it is just a single value which can be used as default
                 setDefaultValue(line);                       
            } // end: matcher.matches() else
        } // end: for each input line
    } // end: inputFromPlain
      
    
   @Override
    public HashMap<String,Double> getRankOrder(boolean ascending) {
         ArrayList<String> entries=getRegisteredKeys();
         Collections.sort(entries,MotifLabEngine.getNaturalSortOrderComparator(ascending));
         HashMap<String,Double> result=new HashMap<String,Double>(entries.size());
         if (entries.size()==1) result.put(entries.get(0), 1d);
         if (entries.size()<=1) return result; // the map is either empty or has just one value
         int lastend=0;
         String lastvalue=getValue(entries.get(0));
         for (int i=1;i<entries.size();i++) {
             String entry=entries.get(i);
             String value=getValue(entry);
             if (value.equals(lastvalue)) continue;
             // new value
             double rank=result.size()+1;
             for (int j=lastend;j<i;j++) result.put(entries.get(j),rank);
             lastvalue=value;
             lastend=i;
         }
         double rank=result.size()+1;
         for (int j=lastend;j<entries.size();j++) result.put(entries.get(j),rank);
         return result;
    }    
   
    @Override
    public void sortAccordingToMap(ArrayList<String> list) {
        Collections.sort(list,MotifLabEngine.getNaturalSortOrderComparator(true));
    }   

    
    /** Creates and returns a subclass of TextMap corresponding to the given targetType
     *  E.g. If the target type is 'Motif.class' the method will return an instance of MotifTextMap.
     *  @return A TextMap corresponding the the target class or NULL if no TextMap exists for that type
     */
    public static TextMap createMapForType(String name, Class targetType, String defaultValue) {
             if (targetType == Motif.class) return new MotifTextMap(name,defaultValue); 
        else if (targetType == ModuleCRM.class) return new ModuleTextMap(name, defaultValue);
        else if (targetType == Sequence.class) return new SequenceTextMap(name, defaultValue); 
        else return null;
    }

    /** Given a Data type as input, this will return the class of the TextMap type
     *  which is associated with that type, i.e. if the input parameter is Motif.class
     *  the method will return MotifTextMap.class
     */
    public static Class getMapTypeForDataType(Class targetType) {
             if (targetType == Motif.class) return MotifTextMap.class;
        else if (targetType == ModuleCRM.class) return ModuleTextMap.class;
        else if (targetType == Sequence.class) return SequenceTextMap.class;
        else return null;
    }

    /** Given a TextMap subclass as input, this will return the class of the data type
     *  that is associated with the Map, i.e. if the input parameter is MotifTextMap.class
     *  the method will return Motif.class
     */
    public static Class getDataTypeForMapType(Class mapType) {
             if (mapType == MotifTextMap.class) return Motif.class;
        else if (mapType == ModuleTextMap.class) return ModuleCRM.class;
        else if (mapType == SequenceTextMap.class) return Sequence.class;
        else return null;
    }

    private static final long serialVersionUID = 1L;
    
}
