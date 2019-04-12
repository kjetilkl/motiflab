/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class MotifNumericMap extends NumericMap {
    private static String typedescription="Motif Numeric Map";
    private String trackName=null; // note that this is not only the trackname but could be the whole settings string (including property and sequence collection etc.)
    private String propertyName=null;

    private static String[] trackPropertyNames=new String[]{"Frequency","Total count","Sequence support","Max occurrences"}; // properties recognized by "from Track" property

    public static String[] getFromTrackPropertyNames() {
        return trackPropertyNames;
    }
    
    public static int getFromTrackPropertyIndexFromName(String propertyName) {
        for (int i=0;i<trackPropertyNames.length;i++) {
            if (trackPropertyNames[i].equalsIgnoreCase(propertyName)) return i;
        }
        return -1;
    }
    
    
    @Override
    public Class getMembersClass() {
        return Motif.class;
    }

    public MotifNumericMap(String name, double defaultvalue) {
        this.name=name;
        this.defaultvalue=new Double(defaultvalue);
    }

    public MotifNumericMap(String name, HashMap<String,Double>values, double defaultvalue) {
        this.name=name;
        this.values=values;
        this.defaultvalue=new Double(defaultvalue);
    }

    public MotifNumericMap() {
        this.name="temp";
        this.defaultvalue=0.0;
    }

    /** Returns the configuration string if this MotifNumericMap object is based on a track
     * (Or NULL if the object is not based on a track)
     * @return The name of the track
     */
    public String getFromTrackString() {
        return getConstructorString(Operation_new.FROM_TRACK_PREFIX);
    }

    /**
     * Sets the track configuration string if this MotifNumericMap object is based on a track
     * @param track
     */
    public void setFromTrackString(String track) {
        setConstructorString(Operation_new.FROM_TRACK_PREFIX,track);
    }

    /** Returns the name of the motif property this MotifNumericMap object is based on
     * (Or NULL if the object is not based on a property)
     * @return The name of the property
     */
    public String getFromPropertyName() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }

    /**
     * Sets the name of the motif property this MotifNumericMap object is based on
     * @param track
     */
    public void setFromPropertyName(String property) {
         setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,property);
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
        return engine.getNamesForAllDataItemsOfType(Motif.class);
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        MotifNumericMap datasource=(MotifNumericMap)source;
        super.importData(source);
        this.trackName=datasource.trackName;
        this.propertyName=datasource.propertyName;
        this.cloneConstructor(datasource);
        //notifyListenersOfDataUpdate();
    }

    @Override
    public String getValueAsParameterString() {
        if (hasConstructorString()) return getFullConstructorString();
        else return super.getValueAsParameterString();
    }

    /**
     * Returns true if this MotifNumericMap equals the other given MotifNumericMap
     * (This means that they have the same mappings but also was constructed in the same way!)
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof MotifNumericMap)) return false;
        if (!super.containsSameData(data)) return false;
        MotifNumericMap other=(MotifNumericMap)data;
        if (!this.hasSameConstructor(other)) return false;
        return true;
    }

    @Override
    public MotifNumericMap clone() {
        HashMap<String,Double> newvalues=new HashMap<String,Double>();
        for (String key:values.keySet()) {
            newvalues.put(key, values.get(key));
        }
        MotifNumericMap newdata=new MotifNumericMap(name, newvalues, defaultvalue);
        newdata.trackName=this.trackName;
        newdata.propertyName=this.propertyName;
        newdata.cloneConstructor(this);
        return newdata;
    }

    @Override
    public String[] getResultVariables() {
        return new String[]{DEFAULT_KEY,"top value","top value in Collection","top:10","top:10 in Collection","top:10%","top:10% in Collection","bottom value","bottom value in Collection","bottom:10","bottom:10 in Collection","bottom:10%","bottom:10% in Collection","rank ascending","rank descending","assigned entries","unassigned entries","positive entries","negative entries","zero-valued entries","value:X"}; // the 10 in top:10 is just an example for the user
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
             Data collection=(Data)params[3];
             ArrayList<String> keys;
             if (collection!=null) {
                 if (collection instanceof MotifCollection) keys=(ArrayList<String>)((MotifCollection)collection).getValues().clone();
                 else if (collection instanceof TextVariable) keys=(ArrayList<String>)((TextVariable)collection).getAllStrings().clone();
                 else throw new ExecutionError("'"+collection.getName()+"' is not a Motif Collection or Text Variable");
             } else keys=getAllKeys(engine);             
             if (isPercentage) number=(int)(((double)keys.size()*number)/100.0);
             ArrayList<String> includeEntries=getTopOrBottomEntries(number,isTop, keys);
             MotifCollection col=new MotifCollection("temp");
             col.addMotifNames(includeEntries);
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
                 else if (item instanceof MotifCollection) keys=(ArrayList<String>)((MotifCollection)item).getValues().clone();
                 else if (item instanceof TextVariable) keys=(ArrayList<String>)((TextVariable)item).getAllStrings().clone();                 
                 else throw new ExecutionError("'"+collectionName+"' is not a Motif Collection");
             } else keys=getAllKeys(engine);
             ArrayList<String> topEntry=getTopOrBottomEntries(1, isTop, keys);
             if (topEntry.size()!=1) return new NumericVariable("temp",defaultvalue);
             return new NumericVariable("temp",getValue(topEntry.get(0)));
         } else if (variablename.equals(DEFAULT_KEY)) {
             return new NumericVariable("temp",defaultvalue);
         } else if (variablename.startsWith("rank ")) {
             boolean ascending=variablename.endsWith("ascending");
             HashMap<String,Double> ranks=getRankOrder(ascending);
             return new MotifNumericMap("temp",ranks, ranks.size()+1);
         } else if (variablename.equals("assigned entries")) {
             Set<String> assignedKeys=values.keySet();
             ArrayList<String> keys=new ArrayList<String>(assignedKeys);
             MotifCollection newcol= new MotifCollection("temp");
             newcol.addMotifNames(keys);
             return newcol;
         } else if (variablename.equals("unassigned entries")) {
             Set<String> assignedKeys=values.keySet();            
             ArrayList<String> keys=getAllKeys(engine);
             keys.removeAll(assignedKeys);
             MotifCollection newcol= new MotifCollection("temp");
             newcol.addMotifNames(keys);
             return newcol;
         } else if (variablename.equals("positive entries") || variablename.equals("negative entries") || variablename.equals("zero-valued entries")) {
             Comparable filter=null;
             if (variablename.equals("positive entries"))    filter=new Comparable() {public int compareTo(Object o) {if (o instanceof Double && ((Double)o).doubleValue()>0) return 1; else return 0; }};
             if (variablename.equals("negative entries"))    filter=new Comparable() {public int compareTo(Object o) {if (o instanceof Double && ((Double)o).doubleValue()<0) return 1; else return 0; }};
             if (variablename.equals("zero-valued entries")) filter=new Comparable() {public int compareTo(Object o) {if (o instanceof Double && ((Double)o).doubleValue()==0) return 1; else return 0; }};
             ArrayList<String> keys=getMatchingEntries(filter, engine);
             MotifCollection newcol= new MotifCollection("temp");
             newcol.addMotifNames(keys);
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
             MotifCollection newcol= new MotifCollection("temp");
             newcol.addMotifNames(keys);
             return newcol;
         } else {
             Data data=engine.getDataItem(variablename);
             if (data instanceof Motif) return new NumericVariable("temp",getValue(variablename));             
         } 
         throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (variablename.startsWith("top:") || variablename.startsWith("bottom:") || variablename.equals("assigned entries") || variablename.equals("unassigned entries")) return MotifCollection.class;
       else if (variablename.equals("positive entries") || variablename.equals("negative entries") || variablename.equals("zero-valued entries") || variablename.startsWith("value:")) return MotifCollection.class;
       else if (variablename.startsWith("top value") || variablename.startsWith("bottom value")) return NumericVariable.class;
       else if (variablename.equals("rank ascending") || variablename.equals("rank descending")) return MotifNumericMap.class;
       else return NumericVariable.class; // all other exported values in this analysis are numerical
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
     * according to the ascending order in the map
     * @param list
     * @return 
     */
    public void sortDataAccordingToMap(ArrayList<Motif> list) {
        Collections.sort(list,new SortOrderComparatorData(true));
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
                if (dataName.equals(DEFAULT_KEY) || engine.dataExists(dataName, Motif.class)) { // just ignore unknown entries
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



    /**
     * This method can be used to create new MotifNumericMap objects from a parameterString
     * @param parameterString
     * @param engine
     * @return
     * @throws ExecutionError
     */
    public static MotifNumericMap createMotifNumericMapFromParameterString(String targetName, String parameterString, ArrayList<String> notfound, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        boolean silentMode=false;
        if (notfound!=null) {silentMode=true;notfound.clear();}
        MotifNumericMap data=new MotifNumericMap(targetName,0);
        if (parameterString==null || parameterString.isEmpty()) return data;
        if (parameterString.startsWith(Operation_new.FROM_TRACK_PREFIX)) {
            int property=0;
            String fromTrackString=parameterString.substring(Operation_new.FROM_TRACK_PREFIX.length());
            String[] parameters=fromTrackString.split("\\s*,\\s*");
            String trackName=parameters[0].trim();
            if (trackName.isEmpty()) throw new ExecutionError("Missing track name");
            Data trackData=engine.getDataItem(trackName);
            if (trackData==null) throw new ExecutionError("Unknown data item: "+trackName);
            else if (!(trackData instanceof RegionDataset)) throw new ExecutionError(trackName+" is not a Region Dataset");
            if (!((RegionDataset)trackData).isMotifTrack()) throw new ExecutionError(trackName+" is not a Motif track");
            RegionDataset source=(RegionDataset)trackData;
            SequenceCollection collection=null;
            RegionDataset withinSegments=null;
            if (parameters.length>1) {
                for (int j=0;j<parameters.length;j++) {
                    String[] split1=parameters[j].trim().split("\\s*(:|=)\\s*");
                    if (split1[0].equalsIgnoreCase("Sequence Collection")) {
                        String dataName=split1[1].trim();
                        Data col=engine.getDataItem(dataName);
                        if (col==null) throw new ExecutionError("Unknown data item: "+dataName);
                        else if (!(col instanceof SequenceCollection)) throw new ExecutionError(dataName+" is not a Sequence Collection");
                        else collection=(SequenceCollection)col;
                    } else if (split1[0].equalsIgnoreCase("within")) {
                        String dataName=split1[1].trim();
                        Data within=engine.getDataItem(dataName);
                        if (within==null) throw new ExecutionError("Unknown data item: "+dataName);
                        else if (!(within instanceof RegionDataset)) throw new ExecutionError(dataName+" is not a Region Dataset");
                        else withinSegments=(RegionDataset)within;
                    } else if (split1[0].equalsIgnoreCase("property")) {
                        String propertyNamePart=split1[1].trim();
                        property=getFromTrackPropertyIndexFromName(propertyNamePart);
                        if (property<0) throw new ExecutionError("Unknown property: "+propertyNamePart);
                    }
                }
            }
            if (collection==null) collection=engine.getDefaultSequenceCollection();
            if (withinSegments!=null) data=createMapFromTrack(targetName, source, collection, withinSegments, property, engine, task);
            else data=createMapFromTrack(targetName, source, collection, property, engine, task);
        } else if (parameterString.startsWith(Operation_new.FROM_PROPERTY_PREFIX)) {
            String property=parameterString.substring(Operation_new.FROM_PROPERTY_PREFIX.length()).trim();          
            Class type=Motif.getPropertyClass(property, engine);
            if (type==null) throw new ExecutionError("Unknown motif property: "+property); 
            if (!(type==Integer.class || type==Double.class)) throw new ExecutionError("'"+property+"' is not a numeric property");
            ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
            for (Data motif:motifs) {
                Object value=((Motif)motif).getPropertyValue(property, engine);
                if (value!=null) {
                    if (value instanceof Integer) data.setValue(motif.getName(), ((Integer)value).doubleValue());
                    else if (value instanceof Double) data.setValue(motif.getName(), ((Double)value).doubleValue());
                    else throw new ExecutionError("'"+property+"' is not a numeric property");
                }
            }
            data.setFromPropertyName(property);
        } else {
            boolean keepConfig=false; // set this flag to true if the original config-string should be retained
            if (parameterString.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               parameterString=parameterString.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
            }             
            // normal list-format           
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
                    if (silentMode) {notfound.add(element+" : Not a 'motif = value' pair");continue;} else throw new ExecutionError("The parameter for a MotifNumericMap should be a comma-separated list of 'motifname = value' pairs");
                }
                double value=0;
                try {
                   value=Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {if (silentMode) {notfound.add(parts[0]+" : Unable to parse expected numeric value");continue;} else throw new ExecutionError("Unable to parse expected numeric value for '"+parts[0]+"': "+parts[1]);}
                Data dataobject=null;
                if (entry.equals(DEFAULT_KEY)) {data.setDefaultValue(value); continue;}
                else if (entry.contains("->")) { // entry refers to a cluster within a Motif Partition
                   String[] partitionelements=entry.split("->");
                   if (partitionelements.length!=2) {
                       if (silentMode) {notfound.add(entry+" : Syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                   }
                   String entrypartition=partitionelements[0];
                   String cluster=partitionelements[1];
                   dataobject=engine.getDataItem(entrypartition);
                        if (dataobject==null) {if (silentMode) {notfound.add(entrypartition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+entrypartition);}
                   else if (!(dataobject instanceof MotifPartition)) {if (silentMode) {notfound.add(entrypartition+" : Not a Motif Partition"); continue;} else throw new ExecutionError("Data item '"+entrypartition+"' is not a Motif Partition");}
                   else if (!((MotifPartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Motif Partition '"+entrypartition+"' does not contain a cluster with the name '"+cluster+"'");}
                   else dataobject=((MotifPartition)dataobject).getClusterAsMotifCollection(cluster, engine);
                } else if (entry.contains(":")) { // a range of motif
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
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpressionInNumericRange(range1[0], range1[2], start, end, Motif.class);
                   MotifCollection tempCollection=new MotifCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addMotif((Motif)object);
                   }
                   addToMotifMapValues(data,tempCollection,value);
                   continue; 
               } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                   if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, Motif.class);
                   for (Data object:regexmatches) {
                       addToMotifMapValues(data,object,value);
                   }
                   continue;
                } else {
                   dataobject=engine.getDataItem(entry);
                }
                     if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
                else if (!(dataobject instanceof Motif || dataobject instanceof MotifCollection)) {if (silentMode) notfound.add(entry+" : Not a Motif or Motif Collection)"); else throw new ExecutionError("Data item '"+entry+"' is not a Motif or Motif Collection");}
                else {
                   addToMotifMapValues(data,dataobject,value);
                }           
            }
            if (keepConfig) data.setFromListString(parameterString); // Store original config-string in data object
        }
        return data;
    }

    private static void addToMotifMapValues(MotifNumericMap target, Object other, double value) {
        if (other==null) return;
        if (other instanceof Motif) {
            target.setValue(((Motif)other).getName(), value);
        } else if (other instanceof MotifCollection) {
            for (String motif:((MotifCollection)other).getAllMotifNames()) {
                target.setValue(motif, value);
            }
        } else {
            System.err.println("SYSTEM ERROR: In MotifNumericMap.addToMotifMapValues. Parameter is neither Motif nor Motif Collection but rather: "+other.getClass().getSimpleName());
        }
    }

    /** Creates a new MotifNumericMap based on the sequence support, total count or frequency of motif occurrences within a given collection of sequences
     * @param targetName
     * @param source
     * @param collection
     * @param property (this integer is index into trackPropertyNames[]) 
     * @param engine
     * @return
     * @throws ExecutionError
     */
    public static MotifNumericMap createMapFromTrack(String targetName, RegionDataset source, SequenceCollection collection, int property, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        HashMap<String,Integer> motiflengths=new HashMap<String,Integer>();
        HashMap<String,Double> counts=new HashMap<String,Double>();
        int[] maxoccurrences=new int[40];
        if (task!=null) {
            task.setProgress(1);
        }        
        for (int i=0;i<maxoccurrences.length;i++) maxoccurrences[i]=maxOccurrences(i, collection, engine, task); // prepare LUT for max occurrences <40 bp
        int s=0,c=0;
        if (property<3) { // applies if property!=Max occurrences     
            ArrayList<String> sequenceNames=collection.getAllSequenceNames();
            int size=sequenceNames.size();
            for (String sequenceName:sequenceNames) {
                s++;
                HashSet<String> inSequence=(property==2)?(new HashSet<String>()):null; // if property==Sequence support then create a list of the sequences
                RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
                for (Region r:seq.getOriginalRegions()) {
                  c++;if (c%100==0) {if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();}
                  String type=r.getType();
                  if (!motiflengths.containsKey(type)) motiflengths.put(type, r.getLength());
                  if (property!=2) { // property is not sequence support
                      if (counts.containsKey(type)) {
                          counts.put(type, new Double(counts.get(type)+1f));
                      } else counts.put(type, new Double(1f));
                  } else inSequence.add(type);
                }
                if (property==2) { // property is sequence support
                    for (String type:inSequence) {
                        if (counts.containsKey(type)) {
                          counts.put(type, new Double(counts.get(type)+1f));
                       } else counts.put(type, new Double(1f));                  
                    }
                }
                if (task!=null) {
                   task.setProgress(s, size);
                   if (Thread.interrupted() || task.isAborted()) throw new InterruptedException();
                }
            } // end for each sequence
        }
        if (property==0) {// convert counts to frequencies by dividing by the maximum number of possible occurrences for each motif
            for (String motif:counts.keySet()) {
                int motiflength=motiflengths.get(motif);
                int max=(motiflength>=maxoccurrences.length)?maxOccurrences(motiflength, collection, engine, task):maxoccurrences[motiflength]; // use LUT if size is smaller than 40
                double freq=counts.get(motif)/(double)max;
                counts.put(motif,new Double(freq));
            }
        }   
        if (property==3) {// return max occurrences
            for (Data motifasdata:engine.getAllDataItemsOfType(Motif.class)) {
                Motif motif=(Motif)motifasdata;
                int motiflength=motif.getLength();
                int max=(motiflength>=maxoccurrences.length)?maxOccurrences(motiflength, collection, engine, task):maxoccurrences[motiflength]; // use LUT if size is smaller than 40
                counts.put(motif.getName(),(double)max);
            }
        }    
        if (task!=null) {
            task.setProgress(100);
            if (Thread.interrupted() || task.isAborted()) throw new InterruptedException();
        }
        MotifNumericMap data=new MotifNumericMap(targetName, counts, 0);
        String fromTrackString=source.getName()+",property="+trackPropertyNames[property];
        if (collection!=null && !collection.getName().equals(engine.getDefaultSequenceCollectionName())) fromTrackString+=",Sequence Collection="+collection.getName();
        data.setFromTrackString(fromTrackString);
        if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
        return data;
    }

    /**
     * Creates a new MotifNumericMap based on the sequence support, total count or frequency of motif occurrences within a given collection of sequences
     * This method is similar to the other createMapFromTrack() method except that this method does not consider the full sequence but only considers
     * occurrences within the given segments (specified by a second RegionDataset). 
     * (The frequency is thus calculated as the number of times a motif occurs within these segments 
     * divided by the maximum number of times it could possibly have occurred within these subsegments (both strands considered))
     * @param targetName
     * @param source
     * @param collection
     * @param withinSegments
     * @param property (this integer is index into trackPropertyNames[]) 
     * @param engine
     * @return
     * @throws ExecutionError
     */
    public static MotifNumericMap createMapFromTrack(String targetName, RegionDataset source, SequenceCollection collection, RegionDataset withinSegments, int property, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        HashMap<String,Integer> motiflengths=new HashMap<String,Integer>();
        HashMap<String,Double> counts=new HashMap<String,Double>(); // first this contains motif counts, then motif frequencies
        int[] maxoccurrences=maxOccurrences(collection, withinSegments, engine, 40, task); // prepare a LUT for max occurrences up to length 40
        int s=0, c=0;
        ArrayList<String> sequenceNames=collection.getAllSequenceNames();
        int size=sequenceNames.size();
        if (property<3) { // applies if property!=Max occurrences
            for (String sequenceName:sequenceNames) {
                s++;                
                HashSet<String> inSequence=(property==2)?(new HashSet<String>()):null;
                RegionSequenceData seq=(RegionSequenceData)source.getSequenceByName(sequenceName);
                RegionSequenceData segmentssequence=(RegionSequenceData)withinSegments.getSequenceByName(sequenceName);
                ArrayList<Region> segments=flattenRegions(segmentssequence.getOriginalRegions(), segmentssequence.getSize());
                for (Region r:seq.getOriginalRegions()) {
                  c++;if (c%100==0) {if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();}
                  String type=r.getType();
                  if (!motiflengths.containsKey(type)) motiflengths.put(type, r.getLength());
                  if (!isRegionWithinOtherRegions(r,segments)) continue;
                  if (property!=2) { // property is not sequence support             
                      if (counts.containsKey(type)) {
                          counts.put(type, new Double(counts.get(type)+1f));
                      } else counts.put(type, new Double(1f));
                  } else inSequence.add(type);
                }
                if (property==2) { // property is sequence support
                    for (String type:inSequence) {
                        if (counts.containsKey(type)) {
                          counts.put(type, new Double(counts.get(type)+1f));
                       } else counts.put(type, new Double(1f));                  
                    }
                }       
                if (task!=null) {
                    task.setProgress(s,size);
                    if (Thread.interrupted() || task.isAborted()) throw new InterruptedException();
                }
            } // end for each sequence
        }
        if (property==0) {// convert counts to frequencies by dividing by the maximum number of possible occurrences for each motif
            for (String motif:counts.keySet()) {
                int motiflength=motiflengths.get(motif);
                int max=(motiflength>=maxoccurrences.length)?maxOccurrences(motiflength, collection, withinSegments, engine, task):maxoccurrences[motiflength]; // use LUT if size is smaller than 40
                double freq=counts.get(motif)/(double)max;
                counts.put(motif,new Double(freq));
            }
        }
        if (property==3) {// return max occurrences         
            for (Data motifasdata:engine.getAllDataItemsOfType(Motif.class)) {
                Motif motif=(Motif)motifasdata;
                int motiflength=motif.getLength();
                int max=(motiflength>=maxoccurrences.length)?maxOccurrences(motiflength, collection, withinSegments, engine, task):maxoccurrences[motiflength]; // use LUT if size is smaller than 40
                counts.put(motif.getName(),(double)max);
            }
        }        
        MotifNumericMap data=new MotifNumericMap(targetName, counts, 0);
        String fromTrackString=source.getName()+",property="+trackPropertyNames[property];
        if (collection!=null && !collection.getName().equals(engine.getDefaultSequenceCollectionName())) fromTrackString+=",Sequence Collection="+collection.getName();
        fromTrackString+=",within="+withinSegments.getName();
        data.setFromTrackString(fromTrackString);    
        if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
        return data;
    }

    /** Returns the maximal number of times a motif with the given length could match in the sequence collection (both strands considered)*/
    private static int maxOccurrences(int motiflength, SequenceCollection collection, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        int total=0;
        if (motiflength==0) return 0;
        for (int i=0;i<collection.size();i++) {
            if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
            String seqname=collection.getSequenceNameByIndex(i);
            Data data=engine.getDataItem(seqname);
            if (data==null || !(data instanceof Sequence)) throw new ExecutionError(seqname+" is not a sequence");
            Sequence sequence=(Sequence)data;
            int sequenceLength=sequence.getSize();
            total+=(sequenceLength<motiflength)?0:((sequence.getSize()-motiflength+1)*2); // avoiding negative values
        }
        return total;
    }

    /** Returns the maximal number of times a motif with the given length could match within the regions of the given track in the sequences in the collection (both strands considered)*/
    private static int maxOccurrences(int motiflength, SequenceCollection collection, RegionDataset withinTrack, MotifLabEngine engine, OperationTask task) throws ExecutionError,InterruptedException {
        int total=0; // total number of times a motif of the given length can occur within regions
        if (motiflength==0) return 0;
        for (int i=0;i<collection.size();i++) {
            if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
            String seqname=collection.getSequenceNameByIndex(i);
            Data data=engine.getDataItem(seqname);
            if (data==null || !(data instanceof Sequence)) throw new ExecutionError(seqname+" is not a sequence");
            //Sequence sequence=(Sequence)data;
            RegionSequenceData sequence=(RegionSequenceData)withinTrack.getSequenceByName(seqname);
            int sequenceLength=sequence.getSize();
            ArrayList<Region> flattenedRegions=flattenRegions(sequence.getOriginalRegions(),sequenceLength);
            for (Region r:flattenedRegions) {
                int regionLength=r.getLength();
                total+=(regionLength<motiflength)?0:((regionLength-motiflength+1)*2); // avoiding negative values
            }
        }
        return total;
    }

    /**
     * Returns the maximal number of times a motif with different lengths could match within the regions of the given track in the sequences in the collection (both strands)
     * The returned array contains these max numbers for lengths from 0 up to and including maxmotiflength
     */
    private static int[] maxOccurrences(SequenceCollection collection, RegionDataset withinTrack, MotifLabEngine engine, int maxmotiflength, OperationTask task) throws ExecutionError,InterruptedException {
        int[] maxocc=new int[maxmotiflength+1];
        for (int i=0;i<collection.size();i++) {
            if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
            String seqname=collection.getSequenceNameByIndex(i);
            Data data=engine.getDataItem(seqname);
            if (data==null || !(data instanceof Sequence)) throw new ExecutionError(seqname+" is not a sequence");
            //Sequence sequence=(Sequence)data;
            RegionSequenceData sequence=(RegionSequenceData)withinTrack.getSequenceByName(seqname);
            int sequenceLength=sequence.getSize();
            ArrayList<Region> flattenedRegions=flattenRegions(sequence.getOriginalRegions(),sequenceLength);
            for (Region r:flattenedRegions) {
                int regionLength=r.getLength();
                for (int motiflength=1;motiflength<=maxmotiflength;motiflength++) {
                   maxocc[motiflength]+=(regionLength<motiflength)?0:((regionLength-motiflength+1)*2);
                }
            }

        }
        return maxocc;
    }

    /** Takes a list of regions (ordered by start position) and returns a new list of regions where overlapping regions in the original list have been merged into longer regions spanning the overlapping segments */
    private static ArrayList<Region> flattenRegions(ArrayList<Region> list, int sequenceLength) {
        ArrayList<Region> flattened=new ArrayList<Region>();
        if (list.isEmpty()) return flattened;
        for (int i=0;i<list.size();i++) {
            Region region=list.get(i);
            int regionStart=region.getRelativeStart();
            int regionEnd=region.getRelativeEnd();
            if (regionEnd<0 || regionStart>=sequenceLength) continue; // region is fully outside sequence. Cant this happen?
            if (regionStart<0) regionStart=0; // partially outside left end
            if (regionEnd>=sequenceLength) regionEnd=sequenceLength-1; // partially outside right end
            if (flattened.isEmpty()) flattened.add(new Region(null,regionStart,regionEnd));
            else {
                Region lastflattened=flattened.get(flattened.size()-1);
                if (regionStart<=lastflattened.getRelativeEnd()+1) {// overlapping or back-to-back => extend old region
                   if (regionEnd>lastflattened.getRelativeEnd()) lastflattened.setRelativeEnd(regionEnd);
                } else { // not overlapping => add new region
                   flattened.add(new Region(null,regionStart,regionEnd));
                }
            }
        }
        return flattened;
    }

    /** Returns TRUE if the selected region is fully within any one of the regions in the provided list */
    private static boolean isRegionWithinOtherRegions(Region region, ArrayList<Region> list) {
        for (Region other:list) {
            if (region.getRelativeStart()>=other.getRelativeStart() && region.getRelativeEnd()<=other.getRelativeEnd()) return true;
        }
        return false;
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
              if (this.trackName!=null) this.setConstructorString(Operation_new.FROM_TRACK_PREFIX, this.trackName);         
         else if (this.propertyName!=null) this.setConstructorString(Operation_new.FROM_PROPERTY_PREFIX, this.propertyName);

         // clear legacy fields
         this.trackName=null;
         this.propertyName=null;               
    }

}
