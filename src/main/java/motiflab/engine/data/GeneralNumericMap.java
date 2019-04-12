/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class GeneralNumericMap extends NumericMap {
    private static String typedescription="Numeric Map";
 
    @Override
    public Class getMembersClass() {
        return String.class;
    }

    public GeneralNumericMap(String name, double defaultvalue) {
        this.name=name;
        this.defaultvalue=new Double(defaultvalue);
    }

    public GeneralNumericMap(String name, HashMap<String,Double>values, double defaultvalue) {
        this.name=name;
        this.values=values;
        this.defaultvalue=new Double(defaultvalue);
    }

    public GeneralNumericMap() {
        this.name="temp";
        this.defaultvalue=0.0;
    }


    @Override
    public ArrayList<String> getAllKeys(MotifLabEngine engine) {
        return getRegisteredKeys();
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        GeneralNumericMap datasource=(GeneralNumericMap)source;
        super.importData(source);
        //notifyListenersOfDataUpdate();
    }

    @Override
    public String getValueAsParameterString() {
        return super.getValueAsParameterString();
    }

    /**
     * Returns true if this GeneralNumericMap equals the other given GeneralNumericMap
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof GeneralNumericMap)) return false;
        if (!super.containsSameData(data)) return false;
        // should I check the constructor here?
        return true;
    }

    @Override
    public GeneralNumericMap clone() {
        HashMap<String,Double> newvalues=new HashMap<String,Double>();
        for (String key:values.keySet()) {
            newvalues.put(key, values.get(key));
        }
        GeneralNumericMap newdata=new GeneralNumericMap(name, newvalues, defaultvalue);
        return newdata;
    }

    @Override
    public String[] getResultVariables() {
        return new String[]{DEFAULT_KEY,"top:10","bottom:10","rank ascending","rank descending"}; // the 10 in top:10 is just an example for the user
    }

    @Override
    public boolean hasResult(String variablename) {
        return true; // what?! It could happen :P    This is checked during run-time!
    }    
    
    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
         if (variablename.startsWith("top:") || variablename.startsWith("bottom:")) {
             Object[] params=parseTopBottomParameters(variablename, engine);
             boolean isTop=(Boolean)params[0];
             int number=(Integer)params[1];
             boolean isPercentage=(Boolean)params[2];
             DataCollection collection=(DataCollection)params[3];
             if (collection!=null && !(collection instanceof DataCollection)) throw new ExecutionError("'"+collection.getName()+"' is not a Data Collection");
             ArrayList<String> keys=(collection!=null)?((ArrayList<String>)collection.getValues().clone()):getAllKeys(engine);
             if (isPercentage) number=(int)(((double)keys.size()*number)/100.0);
             ArrayList<String> includeEntries=getTopOrBottomEntries(number,isTop, keys);
             TextVariable col=new TextVariable("temp",includeEntries);
             return col;
         } else if (variablename.startsWith("top value") || variablename.startsWith("bottom value")) {
             ArrayList<String> keys;
             boolean isTop=variablename.startsWith("top");
             String collectionName=null;            
             if (variablename.startsWith("top value in ")) collectionName=variablename.substring("top value in".length()).trim();
             else if (variablename.startsWith("bottom value in ")) collectionName=variablename.substring("bottom value in".length()).trim();
             if (collectionName!=null) {
                 Data item=engine.getDataItem(collectionName);
                 if (item==null) throw new ExecutionError("Unrecognized data item: "+collectionName);
                 else if (item instanceof DataCollection) keys=(ArrayList<String>)((DataCollection)item).getValues().clone();
                 else if (item instanceof TextVariable) keys=(ArrayList<String>)((TextVariable)item).getAllStrings().clone();
                 else throw new ExecutionError("'"+collectionName+"' is not a Sequence Collection");
             } else keys=getAllKeys(engine);
             ArrayList<String> topEntry=getTopOrBottomEntries(1, isTop, keys);
             if (topEntry.size()!=1) return new NumericVariable("temp",defaultvalue);
             return new NumericVariable("temp",getValue(topEntry.get(0)));
         } else if (variablename.equals(DEFAULT_KEY)) {
             return new NumericVariable("temp",defaultvalue);
         } else if (variablename.startsWith("rank ")) {
             boolean ascending=variablename.endsWith("ascending");
             HashMap<String,Double> ranks=getRankOrder(ascending);
             return new GeneralNumericMap("temp",ranks, ranks.size()+1);
         } else {
             return new NumericVariable("temp",getValue(variablename));             
         } 
    }

    @Override
    public Class getResultType(String variablename) {
       if (variablename.startsWith("top:") || variablename.startsWith("bottom:")) return TextVariable.class;
       else if (variablename.equals("rank ascending") || variablename.equals("rank descending")) return GeneralNumericMap.class;
       else return NumericVariable.class; // all other exported values in this analysis are numerical
    }    
    
    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

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
            } // end: matcher.matches()
        } // end: for each input line
    } // end: inputFromPlain



    /**
     * This method can be used to create new GeneralNumericMap objects from a parameterString
     * @param parameterString
     * @param engine
     * @return
     * @throws ExecutionError
     */
    public static GeneralNumericMap createGeneralNumericMapFromParameterString(String targetName, String parameterString, ArrayList<String> notfound, MotifLabEngine engine) throws ExecutionError {
        boolean silentMode=false;
        if (notfound!=null) {silentMode=true;notfound.clear();}
        GeneralNumericMap data=new GeneralNumericMap(targetName,0);
        if (parameterString==null || parameterString.isEmpty()) return data;

        // normal list-format           
        String[] elements=parameterString.split("\\s*,\\s*");
        for (String element:elements) {
            String[] parts=element.split("\\s*=\\s*");
            String entry=parts[0];
            if (parts.length!=2) {
                if (silentMode) {notfound.add(element+" : Not a 'key = value' pair");continue;} else throw new ExecutionError("The parameter for a Numeric Map should be a comma-separated list of 'key = value' pairs");
            }
            double value=0;
            try {
               value=Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {if (silentMode) {notfound.add(parts[0]+" : Unable to parse expected numeric value");continue;} else throw new ExecutionError("Unable to parse expected numeric value for key '"+parts[0]+"': "+parts[1]);}
            if (entry.equals(DEFAULT_KEY)) {data.setDefaultValue(value); continue;}
            else {
               data.setValue(entry, value);
            }        
        }        
        return data;
    }

   
    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }

}
