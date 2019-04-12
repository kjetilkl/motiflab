
package motiflab.engine.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class SequenceTextMap extends TextMap {
    private static String typedescription="Sequence Map";
    private String propertyName=null;
    
    public SequenceTextMap(String name, String defaultvalue) {
        this.name=name;
        this.defaultvalue=defaultvalue;
    }
    
    public SequenceTextMap(String name, HashMap<String,String>values, String defaultvalue) {
        this.name=name;
        this.values=values;
        this.defaultvalue=defaultvalue;
    }
    
    public SequenceTextMap() {
        this.name="tmp";
        this.defaultvalue="";
    }    

    @Override
    public ArrayList<String> getAllKeys(MotifLabEngine engine) {
        return engine.getNamesForAllDataItemsOfType(Sequence.class);
    }

    @Override
    public Class getMembersClass() {
        return Sequence.class;
    }

    @Override
    public SequenceTextMap clone() {
        HashMap<String,String> newvalues=new HashMap<String,String>();
        for (String key:values.keySet()) {
            newvalues.put(key, values.get(key));
        }        
        SequenceTextMap newdata=new SequenceTextMap(name, newvalues, defaultvalue);
        newdata.propertyName=this.propertyName;
        newdata.cloneConstructor(this);
        return newdata;
    }   

    /** Returns the name of the sequence property this SequenceTextMap object is based on
     * (Or NULL if the object is not based on a property)
     * @return The name of the property
     */
    public String getFromPropertyName() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }

    /**
     * Sets the name of the sequence property this SequenceTextMap object is based on
     * @param property
     */
    public void setFromPropertyName(String property) {
       setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,property);
    }    
    
    public boolean isFromProperty() {
        return hasConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }    
    
    /** If this map is based on a (non-resolved) list of references, this method will return a string describing the settings used for initialization */
    public String getFromListString() {
        return getConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** Sets a string used for initialization of this map (which includes references to basic data objects, collections, and partition clusters) */
    public void setFromListString(String liststring) {
        setConstructorString(Operation_new.FROM_LIST_PREFIX, liststring);        
    }
    /** Returns TRUE if this map is based a (non-resolved) list of references (which could include references to collections and partition-clusters) */
    public boolean isFromList() {
        return hasConstructorString(Operation_new.FROM_LIST_PREFIX);
    }       
    
    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}
    
    @Override
    public void importData(Data source) throws ClassCastException {
        SequenceTextMap datasource=(SequenceTextMap)source;
        super.importData(source);
        this.propertyName=datasource.propertyName;
        this.cloneConstructor(datasource);
    }

    @Override
    public String getValueAsParameterString() {
        if (hasConstructorString()) return getFullConstructorString();
        else return super.getValueAsParameterString();
    }

    /**
     * Returns true if this SequenceTextMap equals the other given SequenceTextMap
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof SequenceTextMap)) return false;
        SequenceTextMap other=(SequenceTextMap)data;
        if (!this.hasSameConstructor(other)) return false;
        return super.containsSameData(data);
    }     
    
    
   @Override
    public String[] getResultVariables() {
        return new String[]{DEFAULT_KEY,"assigned entries","unassigned entries","rank ascending", "rank descending"}; //
    }

    @Override
    public boolean hasResult(String variablename) {
        return true; // what?! It could happen :P    This is checked during run-time!
    }    
    
    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
         if (variablename.equals(DEFAULT_KEY)) {
             return new TextVariable("temp",defaultvalue);
         } else if (variablename.equals("assigned entries")) {
             Set<String> assignedKeys=values.keySet();
             ArrayList<String> keys=new ArrayList<String>(assignedKeys);
             SequenceCollection newcol= new SequenceCollection("temp");
             newcol.addSequenceNames(keys);
             return newcol;
         } else if (variablename.equals("unassigned entries")) {
             Set<String> assignedKeys=values.keySet();            
             ArrayList<String> keys=getAllKeys(engine);
             keys.removeAll(assignedKeys);
             SequenceCollection newcol= new SequenceCollection("temp");
             newcol.addSequenceNames(keys);
             return newcol;
         } else if (variablename.startsWith("rank ")) {
             boolean ascending=variablename.endsWith("ascending");
             HashMap<String,Double> ranks=getRankOrder(ascending);
             return new SequenceNumericMap("temp",ranks, ranks.size()+1);             
         } else {
             Data data=engine.getDataItem(variablename);
             if (data instanceof Sequence) return new TextVariable("temp",getValue(variablename));             
         } 
         throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (variablename.equals("assigned entries") || variablename.equals("unassigned entries")) return SequenceCollection.class;
       else if (variablename.equals("rank ascending") || variablename.equals("rank descending")) return SequenceNumericMap.class;
       else return TextVariable.class; // all other exported values in this analysis are text based
    }
    

    /**
     * This method can be used to create new SequenceTextMap objects from a parameterString
     * @param parameterString
     * @param engine
     * @return
     * @throws ExecutionError
     */
    public static SequenceTextMap createSequenceTextMapFromParameterString(String targetName, String parameterString, ArrayList<String> notfound, MotifLabEngine engine) throws ExecutionError {
        boolean silentMode=false;
        if (notfound!=null) {silentMode=true;notfound.clear();}
        SequenceTextMap data=new SequenceTextMap(targetName,"");
        if (parameterString==null || parameterString.isEmpty()) return data;
        if (parameterString.startsWith(Operation_new.FROM_PROPERTY_PREFIX)) {
            String property=parameterString.substring(Operation_new.FROM_PROPERTY_PREFIX.length()).trim();
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data sequence:sequences) {
                Object value=((Sequence)sequence).getPropertyValue(property,engine);
                String valueString="";
                if (value instanceof List) valueString=MotifLabEngine.splice((List)value, ","); // un-nest lists
                else if (value!=null) valueString=value.toString();
                data.setValue(sequence.getName(),valueString);                
            }
            data.setFromPropertyName(property);
        } else {
            boolean keepConfig=false; // set this flag to true if the original config-string should be retained
            if (parameterString.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               parameterString=parameterString.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
            }             
            // normal list-format    
            ArrayList<String> elements;
            try {
                elements=MotifLabEngine.splitOnCharacter(parameterString,','); // previously, elements were separated by semicolons since the values themselves could contain commas. But now values are comma-separated but can be double-quoted
            } catch (ParseError pe) {
                throw new ExecutionError(pe.getMessage(),pe);
            }  
            for (String element:elements) {
                String[] parts=element.split("\\s*=\\s*",2);
                String entry=parts[0];
                if (parts.length!=2) {
                    if (parts.length==1) { // see if this is a single value that can be used as default
                        if (entry!=null) entry=entry.trim();
                        data.setDefaultValue(entry);
                    }
                    if (silentMode) {notfound.add(element+" : Not a 'sequence = value' pair");continue;} else throw new ExecutionError("The parameter for a SequenceTextMap should be a comma-separated list of 'sequencename = value' pairs");
                }
                String value=parts[1];
                if (value==null) value="";
                value=value.trim();
                Data dataobject=null;
                if (entry.equals(DEFAULT_KEY)) {data.setDefaultValue(value); continue;}
                else if (entry.contains("->")) { // entry refers to a cluster within a Sequence Partition
                   String[] partitionelements=entry.split("->");
                   if (partitionelements.length!=2) {
                       if (silentMode) {notfound.add(entry+" : Syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                   }
                   String entrypartition=partitionelements[0];
                   String cluster=partitionelements[1];
                   dataobject=engine.getDataItem(entrypartition);
                        if (dataobject==null) {if (silentMode) {notfound.add(entrypartition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+entrypartition);}
                   else if (!(dataobject instanceof SequencePartition)) {if (silentMode) {notfound.add(entrypartition+" : Not a Sequence Partition"); continue;} else throw new ExecutionError("Data item '"+entrypartition+"' is not a Sequence Partition");}
                   else if (!((SequencePartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Sequence Partition '"+entrypartition+"' does not contain a cluster with the name '"+cluster+"'");}
                   else dataobject=((SequencePartition)dataobject).getClusterAsSequenceCollection(cluster, engine);
                } else if (entry.contains(":")) { // a range of sequences
                   String[] rangeElements=entry.split(":");
                   if (rangeElements[0]==null || rangeElements[0].isEmpty() || rangeElements[1]==null || rangeElements[1].isEmpty()) {if (silentMode) {notfound.add(entry+" : Problem with range specification"); continue;} else throw new ExecutionError("Problem with range specification: "+entry);}
                   String[] range1=MotifLabEngine.splitOnNumericPart(rangeElements[0]);
                   String[] range2=MotifLabEngine.splitOnNumericPart(rangeElements[1]);
                   if (   range1==null || range2==null
                       || range1[0].isEmpty() && !range2[0].isEmpty()
                       || range2[0].isEmpty() && !range1[0].isEmpty()
                       || range1[2].isEmpty() && !range2[2].isEmpty()
                       || range2[2].isEmpty() && !range1[2].isEmpty()
                       || !range1[0].equals(range2[0])
                       || !range1[2].equals(range2[2])
                   ) {if (silentMode) {notfound.add(entry+" : Problem with range specification"); continue;} else throw new ExecutionError("Problem with range specification: "+entry);}                         
                   int start=0;
                   int end=0;
                   try {
                      start=Integer.parseInt(range1[1]);  
                      end=Integer.parseInt(range2[1]);  
                   } catch(NumberFormatException nf) {
                      if (silentMode) {notfound.add(entry+" : Problem with range specification"); continue;} else throw new ExecutionError("Problem with range specification: "+entry); 
                   }
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpressionInNumericRange(range1[0], range1[2], start, end, Sequence.class);
                   SequenceCollection tempCollection=new SequenceCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addSequence((Sequence)object);
                   }
                   addToSequenceMapValues(data,tempCollection,value);
                   continue; 
               } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                   if (entry.contains(".")) entry=entry.replace(".", "\\."); // escape dot (since it is allowed in sequence names)
                   if (entry.contains("-")) entry=entry.replace("-", "\\-"); // escape - (since it is allowed in sequence names)
                   if (entry.contains("+")) entry=entry.replace("+", "\\+"); // escape + (since it is allowed in sequence names)   
                   if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, Sequence.class);
                   for (Data object:regexmatches) {
                       addToSequenceMapValues(data,object,value);
                   }
                   continue;
                } else {
                   dataobject=engine.getDataItem(entry);
                }
                     if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
                else if (!(dataobject instanceof Sequence || dataobject instanceof SequenceCollection)) {if (silentMode) notfound.add(entry+" : Not a Sequence or Sequence Collection)"); else throw new ExecutionError("Data item '"+entry+"' is not a Sequence or Sequence Collection");}
                else {
                   addToSequenceMapValues(data,dataobject,value);
                }           
            }
            if (keepConfig) data.setFromListString(parameterString); // Store original config-string in data object
        }
        return data;
    }    
    
     private static void addToSequenceMapValues(SequenceTextMap target, Object other, String value) {
        if (other==null) return;
        if (other instanceof Sequence) {
            target.setValue(((Sequence)other).getName(), value);
        } else if (other instanceof SequenceCollection) {
            for (String sequence:((SequenceCollection)other).getAllSequenceNames()) {
                target.setValue(sequence, value);
            }
        } else {
            System.err.println("SYSTEM ERROR: In SequenceTextMap.addToSequenceMapValues. Parameter is neither Sequence nor Sequence Collection but rather: "+other.getClass().getSimpleName());
        }
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
         
         // migrate legacy "constructor" fields to new common constructor                
         if (this.propertyName!=null) this.setConstructorString(Operation_new.FROM_PROPERTY_PREFIX, this.propertyName);   

         // clear legacy fields
         this.propertyName=null;          
    }    
    
}
