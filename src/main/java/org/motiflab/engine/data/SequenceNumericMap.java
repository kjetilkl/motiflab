/*
 
 
 */

package org.motiflab.engine.data;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.operations.Operation_statistic;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.protocol.StandardOperationParser_statistic;
import org.motiflab.engine.task.ExecutableTask;

/**
 * This class represents 'vector' objects that can hold a single and separate 
 * numeric value for each sequence. To get a specific value the user supplies the
 * name of the sequence in question. If the user do not supply a sequence name or
 * if no value is registered for that sequence the value returned will be a preset 
 * 'default' value.
 * @author kjetikl
 */
public class SequenceNumericMap extends NumericMap {

    private static String typedescription="Sequence Numeric Map";

    private String propertyName=null;
    private String fromStatistic=null;

    
    @Override
    public Class getMembersClass() {
        return Sequence.class;
    }

    public SequenceNumericMap(String name, double defaultvalue) {
        this.name=name;
        this.defaultvalue=new Double(defaultvalue);
    }
    
    public SequenceNumericMap(String name, HashMap<String,Double>values, double defaultvalue) {
        this.name=name;
        this.values=values;
        this.defaultvalue=new Double(defaultvalue);
    }

    public SequenceNumericMap() {
        this.name="temp";
        this.defaultvalue=0.0;
    }

    /** Returns the name of the sequence property this SequenceNumericMap object is based on
     * (Or NULL if the object is not based on a property)
     * @return The name of the property
     */
    public String getFromPropertyName() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }

    /**
     * Sets the name of the sequence property this SequenceNumericMap object is based on
     * @param property
     */
    public void setFromPropertyName(String property) {
        setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,property);
    }    
    
    public boolean isFromProperty() {
        return hasConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }
    
    /** Returns the statistical expression this SequenceNumericMap object is based on
     * (Or NULL if the object is not based on a statistic)
     * @return The statistical expression
     */
    public String getFromStatisticString() {
        return getConstructorString(Operation_new.FROM_STATISTIC_PREFIX);
    }

    /**
     * Sets the statistical expression this SequenceNumericMap object is based on
     * @param expression
     */
    public void setFromStatisticString(String expression) {
        setConstructorString(Operation_new.FROM_STATISTIC_PREFIX,expression);
    }    
    
    public boolean isFromStatistic() {
        return hasConstructorString(Operation_new.FROM_STATISTIC_PREFIX);
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
    
    @Override
    public ArrayList<String> getAllKeys(MotifLabEngine engine) {
        return engine.getNamesForAllDataItemsOfType(Sequence.class);
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        SequenceNumericMap datasource=(SequenceNumericMap)source;
        super.importData(source);
        this.propertyName=datasource.propertyName;
        this.fromStatistic=datasource.fromStatistic;
        this.cloneConstructor(datasource);
    }

    @Override
    public String getValueAsParameterString() {
        if (hasConstructorString()) return getFullConstructorString();
        else return super.getValueAsParameterString();
    }

    /**
     * Returns true if this SequenceNumericMap equals the other given SequenceNumericMap
     * (This means that they have the same mappings but also was constructed in the same way!)     * 
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof SequenceNumericMap)) return false;
        SequenceNumericMap other=(SequenceNumericMap)data;
        if (!this.hasSameConstructor(other)) return false;
        return super.containsSameData(data);
    }    
    
    @Override
    public SequenceNumericMap clone() {
        HashMap<String,Double> newvalues=new HashMap<String,Double>();
        for (String key:values.keySet()) {
            newvalues.put(key, values.get(key));
        }        
        SequenceNumericMap newdata=new SequenceNumericMap(name, newvalues, defaultvalue);
        newdata.propertyName=this.propertyName;
        newdata.fromStatistic=this.fromStatistic;
        newdata.cloneConstructor(this);
        return newdata;
    }      
    

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    /**
     * This method will sort the provided list (which should contain entries in the map)
     * according to the ascending order in the map. If some entries are tied (have same map value)
     * they will be sorted according to sequence name, so that all valid entries (non-null and non-NaN)
     * should have a defined order
     * @param list
     * @return 
     */
    public void sortDataAccordingToMap(ArrayList<Sequence> list) {
        Collections.sort(list,new SortOrderComparatorData(true));
    }     
    

    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        values.clear();
        this.propertyName=null;
        this.fromStatistic=null;
        this.clearConstructorString();
        java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("(\\S+)\\s*[=\\t]\\s*(\\S+)");
        for (String line:input) {
            line=line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            java.util.regex.Matcher matcher=pattern.matcher(line);
            if (matcher.matches()) {
                String dataName=matcher.group(1);
                String value=matcher.group(2);
                if (dataName.equals(DEFAULT_KEY) || engine.dataExists(dataName, Sequence.class)) { // just ignore unknown entries
                      try {
                        Double newvalue=Double.parseDouble(value);
                        if (dataName.equals(DEFAULT_KEY)) setDefaultValue(newvalue);
                        else setValue(dataName, newvalue);
                    } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numerical input in PLAIN format for "+getName()+": "+nfe.getMessage());}
                }
            } else { // Line is not in standard "key = value" format. Check if it is just a single value which can be used as default
                 try {
                    Double newvalue=Double.parseDouble(line);
                    setDefaultValue(newvalue);
                } catch (NumberFormatException nfe) {}                             
            } // end: matcher.matches() else
        } // end: for each input line
    } // end: inputFromPlain

    
    @Override
    public String[] getResultVariables() {
        return new String[]{DEFAULT_KEY,"top value","top value in Collection","top:10","top:10 in Collection","top:10%","top:10% in Collection","bottom value","bottom value in Collection","bottom:10","bottom:10 in Collection","bottom:10%","bottom:10% in Collection","rank ascending","rank descending","assigned entries","unassigned entries","positive entries","negative entries","zero-valued entries","value:X"}; // the 10 in top:10 is just an example for the user
    }

    @Override
    public boolean hasResult(String variablename) {
        return true; // what?! It could happen :P
    }    
    
    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
         if (variablename.startsWith("top:") || variablename.startsWith("bottom:")) {
             Object[] params=parseTopBottomParameters(variablename, engine);
             boolean isTop=(Boolean)params[0];
             int number=(Integer)params[1];
             boolean isPercentage=(Boolean)params[2];
             Data collection=(Data)params[3];
             ArrayList<String> keys;
             if (collection!=null) {
                 if (collection instanceof SequenceCollection) keys=(ArrayList<String>)((SequenceCollection)collection).getValues().clone();
                 else if (collection instanceof TextVariable) keys=(ArrayList<String>)((TextVariable)collection).getAllStrings().clone();
                 else throw new ExecutionError("'"+collection.getName()+"' is not a Sequence Collection or Text Variable");
             } else keys=getAllKeys(engine);             
             if (isPercentage) number=(int)(((double)keys.size()*number)/100.0);
             ArrayList<String> includeEntries=getTopOrBottomEntries(number,isTop, keys);
             SequenceCollection col=new SequenceCollection("temp");
             col.addSequenceNames(includeEntries);
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
                 else if (item instanceof SequenceCollection) keys=(ArrayList<String>)((SequenceCollection)item).getValues().clone();
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
             return new SequenceNumericMap("temp",ranks, ranks.size()+1);
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
         } else if (variablename.equals("positive entries") || variablename.equals("negative entries") || variablename.equals("zero-valued entries")) {
             Comparable filter=null;
             if (variablename.equals("positive entries"))    filter=new Comparable() {public int compareTo(Object o) {if (o instanceof Double && ((Double)o).doubleValue()>0) return 1; else return 0; }};
             if (variablename.equals("negative entries"))    filter=new Comparable() {public int compareTo(Object o) {if (o instanceof Double && ((Double)o).doubleValue()<0) return 1; else return 0; }};
             if (variablename.equals("zero-valued entries")) filter=new Comparable() {public int compareTo(Object o) {if (o instanceof Double && ((Double)o).doubleValue()==0) return 1; else return 0; }};
             ArrayList<String> keys=getMatchingEntries(filter, engine);
             SequenceCollection newcol= new SequenceCollection("temp");
             newcol.addSequenceNames(keys);
             return newcol;
         } else if (variablename.startsWith("value:")) {
             String valueAsString=variablename.substring("value:".length());
             final Double[] value=new Double[1];
             Object valueObject=engine.getNumericDataForString(valueAsString);
                  if (valueObject instanceof Double) value[0]=((Double)valueObject);
             else if (valueObject instanceof NumericMap) value[0]=((NumericMap)valueObject).getValue();
             else if (valueObject instanceof NumericVariable) value[0]=((NumericVariable)valueObject).getValue();
             else if (valueObject instanceof NumericConstant) value[0]=((NumericConstant)valueObject).getValue();  
             else throw new ExecutionError("'"+valueAsString+"' is not a numeric value or recognized numeric data object");
             Comparable filter=new Comparable() {public int compareTo(Object o) {if (o instanceof Double && ((Double)o).doubleValue()==value[0].doubleValue()) return 1; else return 0; }};
             ArrayList<String> keys=getMatchingEntries(filter, engine);
             SequenceCollection newcol= new SequenceCollection("temp");
             newcol.addSequenceNames(keys);
             return newcol;
         } else {
             Data data=engine.getDataItem(variablename);
             if (data instanceof Sequence) return new NumericVariable("temp",getValue(variablename));             
         } 
         throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (variablename.startsWith("top:") || variablename.startsWith("bottom:") || variablename.equals("assigned entries") || variablename.equals("unassigned entries")) return SequenceCollection.class;
       else if (variablename.equals("positive entries") || variablename.equals("negative entries") || variablename.equals("zero-valued entries") || variablename.startsWith("value:")) return SequenceCollection.class;
       else if (variablename.startsWith("top value") || variablename.startsWith("bottom value")) return NumericVariable.class; 
       else if (variablename.startsWith("rank ascending") || variablename.startsWith("rank descending")) return SequenceNumericMap.class;
       else return NumericVariable.class; // all other exported values in this analysis are numerical
    }    
     
    
    /**
     * This method can be used to create new SequenceNumericMap objects from a parameterString
     * @param parameterString
     * @param engine
     * @return
     * @throws ExecutionError
     */
    public static SequenceNumericMap createSequenceNumericMapFromParameterString(String targetName, String parameterString, ArrayList<String> notfound, MotifLabEngine engine, OperationTask task) throws ExecutionError {
        boolean silentMode=false;
        if (notfound!=null) {silentMode=true;notfound.clear();}
        SequenceNumericMap data=new SequenceNumericMap(targetName,0);
        if (parameterString==null || parameterString.isEmpty()) return data;
        if (parameterString.startsWith(Operation_new.FROM_PROPERTY_PREFIX)) {
            String property=parameterString.substring(Operation_new.FROM_PROPERTY_PREFIX.length()).trim();
            if (property.equalsIgnoreCase("sort order")) {
                SequenceCollection ordered=engine.getDefaultSequenceCollection();
                ArrayList<String> names=ordered.getAllSequenceNames();
                int index=1;
                for (String name:names) {                   
                    data.setValue(name, index);
                    index++;
                }
                data.setDefaultValue(index);
            } else {
                ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
                Class propertyclass=Sequence.getPropertyClass(property,engine);
                if (propertyclass==null) throw new ExecutionError("Unknown sequence property: "+property);
                if (!(Number.class.isAssignableFrom(propertyclass))) throw new ExecutionError("Sequence property '"+property+"' is not numeric");
                for (Data sequence:sequences) {
                    Object value=((Sequence)sequence).getPropertyValue(property,engine);
                    if (value!=null && Number.class.isAssignableFrom(value.getClass())) {
                       double size=((Number)value).doubleValue();
                       data.setValue(sequence.getName(), size);                   
                    } 
                }
            }
            data.setFromPropertyName(property);
        } else if (parameterString.startsWith(Operation_new.FROM_STATISTIC_PREFIX)) {
           String statisticString=parameterString.substring(Operation_new.FROM_STATISTIC_PREFIX.length());
           data.createMapFromStatistic(targetName,statisticString, engine, task);
           return data;
        } else { // parse from "Plain" format  
            boolean keepConfig=false; // set this flag to true if the original config-string should be retained
            if (parameterString.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               parameterString=parameterString.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
            }            
            String[] elements=parameterString.split("\\s*,\\s*");
            for (String element:elements) {
                String[] parts=element.split("\\s*=\\s*");
                String entry=parts[0];
                if (parts.length!=2) {
                    if (parts.length==1) { // see if this is a single value that can be used as default
                         try {
                           double value=Double.parseDouble(entry);
                           data.setDefaultValue(value); 
                           continue;
                        } catch (NumberFormatException e) {} // I will use the error reporting on the next line...                      
                    }                    
                    if (silentMode) {notfound.add(element+" : Not a 'sequence = value' pair");continue;} else throw new ExecutionError("The parameter for a SequenceNumericMap should be a comma-separated list of 'sequencename = value' pairs");
                }
                double value=0;
                try {
                   value=Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {if (silentMode) {notfound.add(parts[0]+" : Unable to parse expected numeric value");continue;} else throw new ExecutionError("Unable to parse expected numeric value for sequence '"+parts[0]+"': "+parts[1]);}
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
                   if (entry.contains("+")) entry=entry.replace("+", "\\+"); // escape + (since it is allowed in sequence names )            
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
                else if (dataobject instanceof SequencePartition) {if (silentMode) notfound.add(entry+" : Missing cluster for Sequence Partition)"); else throw new ExecutionError("Missing specification of cluster for Sequence Partition '"+entry+"'. (use format: Partition.Cluster)");}
                else if (!(dataobject instanceof Sequence || dataobject instanceof SequenceCollection)) {if (silentMode) notfound.add(entry+" : Not a Sequence or Sequence Collection)"); else throw new ExecutionError("Data item '"+entry+"' is not a Sequence or Sequence Collection");}
                else {
                   addToSequenceMapValues(data,dataobject,value);
                }
            }
            if (keepConfig) data.setFromListString(parameterString); // Store original config-string in data object
        }
        return data;
    }

    private static void addToSequenceMapValues(SequenceNumericMap target, Object other, double value) {
        if (other==null) return;
        if (other instanceof Sequence) {
            target.setValue(((Sequence)other).getName(), value);
        } else if (other instanceof SequenceCollection) {
            for (String seq:((SequenceCollection)other).getAllSequenceNames()) {
                target.setValue(seq, value);
            }
        } else {
            System.err.println("SYSTEM ERROR: In SequenceNumericMap.addToSequenceMapValues. Parameter is neither Sequence nor Sequence Collection but rather: "+other.getClass().getSimpleName());
        }
    }
    
 
    /**
     * Initialize this map based on a statistic
     *
     * @param statisticalString A string describing the statistical function. The "statistic" operation will be used to create a SequenceNumericMap based on this function
     */
   public void createMapFromStatistic(String mapname,String statisticalString, MotifLabEngine engine, final OperationTask monitorTask) throws ExecutionError {
       // configure   
       Operation_statistic statop=(Operation_statistic)engine.getOperation("statistic");
       StandardOperationParser_statistic parser=new StandardOperationParser_statistic();
       parser.setEngine(engine);
       OperationTask task=null;
       try {
           task=parser.parseInternal(statisticalString);
       } catch (ParseError e) {throw new ExecutionError("Unable to parse statistic argument: "+e.getMessage(),e);}
       task.setParameter(OperationTask.ENGINE, engine);
       task.setParameter(OperationTask.OPERATION, statop);
       task.setParameter(Operation_statistic.INITIATE_ALL_TO_ZERO, Boolean.TRUE);
       task.setParameter(Operation_statistic.FOR_INTERNAL_USE, Boolean.TRUE);
       if (monitorTask!=null) {
            final OperationTask innerTask=task;
            innerTask.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(ExecutableTask.PROGRESS)) {
                    Object value=evt.getNewValue();
                    if (value instanceof Integer) monitorTask.setProgress((Integer)value);
                    if (Thread.interrupted() || innerTask.isAborted()) monitorTask.setStatus(ExecutableTask.ABORTED);                   
                }
            }
        });
       }
       try {
           statop.execute(task);
       } catch (Exception e) {throw new ExecutionError("An exception occurred while evaluating statistical function", e);}
       SequenceNumericMap map=(SequenceNumericMap)task.getParameter(Operation_statistic.RESULT);
       this.importData(map);
       this.setFromStatisticString(statisticalString); // sets the 'config' string     
       this.rename(mapname);
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
              if (this.fromStatistic!=null) this.setConstructorString(Operation_new.FROM_STATISTIC_PREFIX, this.fromStatistic);         
         else if (this.propertyName!=null) this.setConstructorString(Operation_new.FROM_PROPERTY_PREFIX, this.propertyName);  

         // clear legacy fields
         this.fromStatistic=null;
         this.propertyName=null;             
    }

}
