/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public abstract class NumericMap extends DataMap {
    protected String name;    
    protected HashMap<String,Double> values=new HashMap<String,Double>();
    protected Double defaultvalue=new Double(0);


    @Override
    public String getName() {return name;}

    @Override
    public void rename(String newname) {
        this.name=newname;
    }    
    
    public void setDefaultValue(double defaultvalue) {
        this.defaultvalue=new Double(defaultvalue);
    }


    @Override
    public Double getValue() {return defaultvalue;}

    @Override
    public String getValueAsParameterString() {
        StringBuilder string=new StringBuilder();
        for (String seq:values.keySet()) {
            string.append(seq);
            string.append('=');
            string.append(values.get(seq).toString());
            string.append(',');
        }
        string.append(DEFAULT_KEY);
        string.append('=');
        string.append(defaultvalue);
        return string.toString();
    }

    @Override
    public String output() {
        StringBuilder string=new StringBuilder();
        for (String seq:values.keySet()) {
            string.append(seq);
            string.append('=');
            string.append(values.get(seq).toString());
            string.append('\n');
        }
        string.append(DEFAULT_KEY);
        string.append('=');
        string.append(defaultvalue);
        string.append('\n');
        return string.toString();
    }

    /**
     * Sets a scalar numeric value to be associated with the given data object
     * @param dataname
     * @param value
     */
    public void setValue(String dataname, double value) {
        values.put(dataname, value);
    }

    @Override
    public void setValueFromString(String dataname, String value) throws ParseError {
        try {
           double numericvalue=Double.parseDouble(value);  
           setValue(dataname, numericvalue);
        } catch (NumberFormatException e) {
            throw new ParseError("Not a valid numeric map-value for '"+dataname+"' => "+value);
        }
    }    

   @Override
    public void setDefaultValueFromString(String value) throws ParseError  {
        try {
           double numericvalue=Double.parseDouble(value);  
           setDefaultValue(numericvalue);
        } catch (NumberFormatException e) {
            throw new ParseError("Not a valid default value for Numeric Map => "+value);
        }
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
        defaultvalue=new Double(0);
    }    
    
       
    @Override
    public Double getValue(String dataname) {
        Double val=values.get(dataname);
        if (val==null) return defaultvalue;
        else return val;
    }

    @Override
    public boolean contains(String dataname) {
        Double val=values.get(dataname);
        return (val!=null);
    }

    /**
     * Returns the smallest value in the map
     * (or the default value if this is smaller)
     * @return
     */
    public double getMinValue() {
        double min=Double.MAX_VALUE;
        for (double value:values.values()) {
            if (value<min) min=value;
        }
        if (defaultvalue<min) return defaultvalue;
        else return min;
    }

    /**
     * Returns the largest value in the map
     * (or the default if this is largest)
     * @return
     */
    public double getMaxValue() {
        double max=-Double.MAX_VALUE;
        for (double value:values.values()) {
            if (value>max) max=value;
        }
        if (defaultvalue>max) return defaultvalue;
        else return max;
    }

    /**
     * Returns the largest value among the keys in the list
     * @param list
     * @return
     */
    public double getMaxValue(ArrayList<String> list) {
        double max=-Double.MAX_VALUE;
        for (String key:list) {
            double value=0;
            if (values.containsKey(key)) value=values.get(key);
            else value = defaultvalue;
            if (value>max) max=value;
        }
        return max;
    }

    /**
     * Returns the smallest value among the keys in the list
     * @param list
     * @return
     */
    public double getMinValue(ArrayList<String> list) {
        double min=Double.MAX_VALUE;
        for (String key:list) {
            double value=0;
            if (values.containsKey(key)) value=values.get(key);
            else value = defaultvalue;
            if (value<min) min=value;
        }
        return min;
    }
    
    /**
     * Returns the (original) HashMap containing the key-value pairs registered
     * in this NumericMap
     * @return
     */
    public HashMap<String,Double> getValues() {
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
    public double[] getValues(ArrayList<String> list) {
        double[] result=new double[list.size()];
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
    
    
    protected Object[] parseTopBottomParameters(String paramString, MotifLabEngine engine) throws ExecutionError {
        boolean isTop=true;
        boolean isPercentage=false;
        int number=0;
        Data collection=null;
        Pattern pattern=Pattern.compile("^(top:|bottom:)\\s*(\\w+)(%)?(\\s+(in|from)\\s+(\\S+))?");
        Matcher matcher=pattern.matcher(paramString);
        String numberString="";
        String collectionName="";
        if (matcher.find()) {
           if (matcher.group(1)!=null && matcher.group(1).equals("bottom:")) isTop=false;
           if (matcher.group(3)!=null && matcher.group(3).equals("%")) isPercentage=true;
           collectionName=matcher.group(6);
           numberString=matcher.group(2);           
        } else throw new ExecutionError("Expected parameter in the format: 'top|bottom:number[%][ in <collection>]'");        
        number=getNumberFromString(numberString,engine);
        if (collectionName!=null && !collectionName.isEmpty()) {
            Data item=engine.getDataItem(collectionName);
            if (item==null) throw new ExecutionError("Unrecognized data object: "+collectionName);
            if (item instanceof DataCollection || item instanceof TextVariable) collection=item;
            else throw new ExecutionError("'"+collectionName+"' is not a Collection or Text Variable");          
        }
        return new Object[]{isTop,number,isPercentage,collection};
    }
    
    /**
     * Returns an integer number represented by the given text string which could be either
     * a literal integer constant or the name of a Numeric Variable
     * @param text
     * @param engine
     * @return
     * @throws ExecutionError 
     */
    protected int getNumberFromString(String text, MotifLabEngine engine) throws ExecutionError {
        try {
            int x=Integer.parseInt(text);
            return x;
        } catch (NumberFormatException e) {
            Data data=engine.getDataItem(text);
            if (data instanceof NumericVariable) return ((NumericVariable)data).getValue().intValue();
        }    
        throw new ExecutionError("'"+text+"' is not a valid number");
    }      
    
    /**
     * Returns the names of the selected number of entries that have the highest (or lowest) values in the map
     * @param entries Number of top scoring entries to return (note that a smaller set could be returned)
     * @param returnTop if TRUE the top entries will be returned, else the bottom entries will be returned
     * @param list A list containing the entries that should be considered. Note that the order in this list will be changed by a call to this method!
     *             If this list is smaller than the number of top entries requested, the whole list will be returned 
     * @return 
     */
    public ArrayList<String> getTopOrBottomEntries(int entries, boolean returnTop, ArrayList<String> list) {
        if (list.size()<=entries) return list;
        Collections.sort(list,new SortOrderComparator(!returnTop));
        ArrayList<String> result=new ArrayList<String>();
        for (int i=0;i<entries;i++) result.add(list.get(i));
        return result;
    }
         
   @Override
    public HashMap<String,Double> getRankOrder(boolean ascending) {
         ArrayList<String> entries=getRegisteredKeys();
         Collections.sort(entries,new SortOrderComparator(ascending));
         HashMap<String,Double> result=new HashMap<String,Double>(entries.size());
         if (entries.size()==1) result.put(entries.get(0), 1d);
         if (entries.size()<=1) return result; // the map is either empty or has just one value
         int lastend=0;
         double lastvalue=getValue(entries.get(0));
         for (int i=1;i<entries.size();i++) {
             String entry=entries.get(i);
             double value=getValue(entry);
             if (value==lastvalue || (Double.isNaN(value) && Double.isNaN(lastvalue))) continue;
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
        Collections.sort(list,new SortOrderComparator(true));
    }
    
    @Override
    public ArrayList<String> getRegisteredKeys() {
        ArrayList<String> list=new ArrayList<String>();
        list.addAll(values.keySet());
        return list;
    }


    @Override
    public void importData(Data source) throws ClassCastException {
        NumericMap datasource=(NumericMap)source;
        this.name=datasource.name;
        this.defaultvalue=datasource.defaultvalue;
        this.values=datasource.values; 
    }

    /**
     * Returns true if this NumericMap equals the other given NumericMap
     * (i.e. it contains the same mappings and they were constructed in a similar way)
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof NumericMap)) return false;
        NumericMap other=(NumericMap)data;
        if (((double)this.defaultvalue)!=((double)other.defaultvalue)) return false;
        if (other.values.size()!=this.values.size()) return false;
        for (String key:values.keySet()) {
            if (!other.contains(key)) return false;
            if (((double)values.get(key))!=((double)other.values.get(key))) return false;
        }
        return true;
    }
    
    @Override 
    public final boolean containsSameMappings(DataMap data) {
        if (data==null || !(data instanceof NumericMap)) return false;
        NumericMap other=(NumericMap)data;
        if (((double)this.defaultvalue)!=((double)other.defaultvalue)) return false;
        if (other.values.size()!=this.values.size()) return false;
        for (String key:values.keySet()) {
            if (!other.contains(key)) return false;
            System.err.println(((double)values.get(key))+" <=> "+((double)other.values.get(key))+"    : "+(((double)values.get(key))!=((double)other.values.get(key))));
            if (((double)values.get(key))!=((double)other.values.get(key))) return false;
        }
        return true;        
    }


    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        values.clear();
        java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("(\\S+)\\s*[=\\t]\\s*(\\S+)");
        for (String line:input) {
            line=line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            java.util.regex.Matcher matcher=pattern.matcher(line);
            if (matcher.matches()) {
                String dataName=matcher.group(1);
                String value=matcher.group(2);
                try {
                    Double newvalue=Double.parseDouble(value);
                    if (dataName.equals(DEFAULT_KEY)) setDefaultValue(newvalue);
                    else setValue(dataName, newvalue);
                } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numerical input in PLAIN format for "+getName()+": "+nfe.getMessage());}                
            
            } else { // Line is not in standard "key = value" format. Check if it is just a single value which can be used as default
                 try {
                    Double newvalue=Double.parseDouble(line);
                    setDefaultValue(newvalue);
                } catch (NumberFormatException nfe) {}                             
            } // end: matcher.matches() else
        } // end: for each input line
    } // end: inputFromPlain
      
    private class SortOrderComparator implements Comparator<String> {
        boolean ascending=true;

        public SortOrderComparator(boolean ascending) {
            this.ascending=ascending;
        }

        @Override
        public int compare(String entry1, String entry2) { // these are two entry names
            double value1=getValue(entry1);
            double value2=getValue(entry2);
            // always sort NaNs at the "bottom" irrespective of 
            if (!Double.isNaN(value1) && Double.isNaN(value2)) return -1;
            else if (Double.isNaN(value1) && !Double.isNaN(value2)) return 1;
            else if (Double.isNaN(value1) && Double.isNaN(value2)) {
                int val=entry1.compareTo(entry2); // both are NaN: sort by name
                return (ascending)?val:-val;
            }        
            // both map entries are non-null and non-NaN            
                 if (value1<value2) return (ascending)?-1:1;
            else if (value1>value2) return (ascending)?1:-1;
            else { // same map value. sort by name.
                int val=entry1.compareTo(entry2);
                return (ascending)?val:-val;
            }
        }    
    }
    
    protected class SortOrderComparatorData implements Comparator<Data> {
        boolean ascending=true;

        public SortOrderComparatorData(boolean ascending) {
            this.ascending=ascending;
        }

        @Override
        public int compare(Data entry1, Data entry2) { // these are two entry names
            if (entry2==null) return (entry1==null)?0:-1;
            if (entry1==null) return (entry2==null)?0:1;
            double value1=getValue(entry1.getName());
            double value2=getValue(entry2.getName());
            // always sort NaNs at the "bottom"
            if (!Double.isNaN(value1) && Double.isNaN(value2)) return -1;
            else if (Double.isNaN(value1) && !Double.isNaN(value2)) return 1;
            else if (Double.isNaN(value1) && Double.isNaN(value2)) {
                int val=entry1.getName().compareTo(entry2.getName());
                return (ascending)?val:-val;                
            }        
            // both map entries are non-null and non-NaN
                 if (value1<value2) return (ascending)?-1:1;
            else if (value1>value2) return (ascending)?1:1;
            else { // same map value. sort by data name.
                int val=entry1.getName().compareTo(entry2.getName());
                return (ascending)?val:-val;
            }
        }    
    } 
    
    /** Creates and returns a subclass of NumericMap corresponding to the given targetType
     *  E.g. If the target type is 'Motif.class' the method will return an instance of MotifNumericMap.
     *  @return A NumericMap corresponding the the target class or NULL if no NumericMap exists for that type
     */
    public static NumericMap createMapForType(String name, Class targetType, double defaultValue) {
             if (targetType == Motif.class) return new MotifNumericMap(name,defaultValue); 
        else if (targetType == ModuleCRM.class) return new ModuleNumericMap(name, defaultValue);
        else if (targetType == Sequence.class) return new SequenceNumericMap(name, defaultValue); 
        else return null;
    }

    /** Given a Data type as input, this will return the class of the Numeric Map type
     *  which is associated with that type, i.e. if the input parameter is Motif.class
     *  the method will return MotifNumericMap.class
     */
    public static Class getMapTypeForDataType(Class targetType) {
             if (targetType == Motif.class) return MotifNumericMap.class;
        else if (targetType == ModuleCRM.class) return ModuleNumericMap.class;
        else if (targetType == Sequence.class) return SequenceNumericMap.class;
        else return null;
    }

    /** Given a Numeric Map subclass as input, this will return the class of the data type
     *  that is associated with the Map, i.e. if the input parameter is MotifNumericMap.class
     *  the method will return Motif.class
     */
    public static Class getDataTypeForMapType(Class mapType) {
             if (mapType == MotifNumericMap.class) return Motif.class;
        else if (mapType == ModuleNumericMap.class) return ModuleCRM.class;
        else if (mapType == SequenceNumericMap.class) return Sequence.class;
        else return null;
    }

    private static final long serialVersionUID = -5906307498325056582L;
    
}
