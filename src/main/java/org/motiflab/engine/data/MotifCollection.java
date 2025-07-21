/*
 
 
 */

package org.motiflab.engine.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.ParseError;


/**
 * MotifCollections can be used to refer to collections of Transcription Factor Binding Motifs
 * Although most get/set methods work directly with Motif
 * objects rather than Motif names (Strings), the internal mechanism of the MotifCollection 
 * itself revolves around a plain list of names for the Motifs in the collection. 
 * The methods that returns Motif objects or lists do this by dynamically obtaining these Motifs
 * from the Engine (on-the-fly) based on the internal list of names
 * 
 * @author kjetikl
 */
public class MotifCollection extends DataCollection {
    private static String typedescription="Motif Collection"; 
    protected String datasetName;
    protected ArrayList<String> storage; // this is a list of Motif names
    private ArrayList<Motif> payload=null; // this is used to temporarily store Motifs that should be registered with the engine
    
    // LEGACY FIELDS: the following constructor strings are deprecated. The fields are kept for backwards compatibility with objects in saved sessions, but they are replaced by the superclass "constructor string" property in newer versions.
    private String predefined=null;   // LEGACY FIELD: name of predefined model
    private String fromTrack=null;    // LEGACY FIELD: a configuration string used when this collection is based on a track. Format example: "<trackname>,support>=80%" 
    private String fromMap=null;      // LEGACY FIELD: a configuration string used when this collection is based on a map. Format example: "<mapname>,value>=0" or "mapname,value in [0,1]"
    private String fromProperty=null; // LEGACY FIELD: a configuration string used used when this collection is based on a property. Format example: "<mapname>,value>=0" or "mapname,value in [0,1]"
    private String fromList=null;     // LEGACY FIELD: a configuration string used when this collection is based on a (non-resolved) list of entries (that could include references to collections and partition-clusters)   

    private static String[] transforms = new String[]{"complement:","reverse:","inverse:","flank:","shuffle:","trim:","trim flanks:","round:","combine:"};
         
    /**
     * Constructs a new initially "empty" Motif collection with the given name
     * 
     * @param datasetName A name for this dataset
     */
   public MotifCollection(String datasetName) {
       this.datasetName=datasetName;
       storage=new ArrayList<String>(20);
   }
   

   
   
    /**
     * Specifies a new name for this dataset
     * 
     * @param name the name for this dataset
     */
    public void setName(String name) {
        this.datasetName=name;
    }
    
    @Override
    public void rename(String name) {
        setName(name);
    }   
    
   /**
    * Returns the name of this dataset
    * 
    * @return dataset name
    */
    @Override
    public String getName() {
        return datasetName;
    }
    
    @Override
    public Object getValue() {return this;} // should maybe change later
  
    @Override
    public String getValueAsParameterString() {
        if (hasConstructorString()) return getFullConstructorString();
        else {
            StringBuilder string=new StringBuilder();
            for (int i=0;i<storage.size();i++) {
                if (i==storage.size()-1) string.append(storage.get(i));
                else {
                    string.append(storage.get(i));
                    string.append(",");
                }
            }
            return string.toString();
        }
    }  

    
    
    /**
     * Returns the names of all the Motifs in this dataset
     * 
     * @return A list of Motif names
     */
    public ArrayList<String> getAllMotifNames() {
        return storage;
    }    

    @Override
    public ArrayList<String> getValues() {
        return storage;
    }

    @Override
    public String[] getResultVariables() {
        return new String[]{"size","random X","random X%",
          "complement; name_suffix=_RC",
          "reverse; name_suffix=_RC",
          "inverse; name_suffix=_I",
          "flank:AAA,TTT; name_suffix=_F",
          "shuffle; name_suffix=_S",
          "trim:1,1; name_suffix=_T", 
          "trim flanks:0.5; name_suffix=_TF",
          "round; name_suffix=_R",
          "combine: <motifname>,0; name_suffix=_COMBINED",          
          "motif:<property>"
        };
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (!hasResult(variablename)) throw new ExecutionError("'" + getName() + "' does not have a result for '" + variablename + "'");
        else if (variablename.equalsIgnoreCase("size")) {
            NumericVariable result=new NumericVariable("size",size());
            return result;           
        } else if (variablename.startsWith("random")) {
            String[] parts=variablename.split("\\s+",2);
            if (parts.length==1) return new MotifCollection("randomCollection");
            String configString=Operation_new.RANDOM_PREFIX+parts[1]+" from "+getName();
            try {
              MotifCollection randomCollection=MotifCollection.parseMotifCollectionParameters(configString, "randomCollection", null, engine, null);
              return randomCollection;
            } catch (Exception e) {
                if (e instanceof ExecutionError) throw (ExecutionError)e;
                else return null;
            }  
        } else if (variablename.startsWith("motif:")) {
            String propertyName=variablename.substring("motif:".length());
            if (propertyName.equals("<property>")) throw new ExecutionError("You must replace <property> with the actual name of a motif property");
            Class propclass=Motif.getPropertyClass(propertyName, engine);
            if (propclass==null) throw new ExecutionError("'"+propertyName+"' is not a recognized motif property");
            DataMap map=null;
            if (Number.class.isAssignableFrom(propclass)) map=new MotifNumericMap("map_"+propertyName, 0);
            else map=new MotifTextMap("map_"+propertyName, "");
            for (String motifname:storage) {
                Motif motif=(Motif)engine.getDataItem(motifname, Motif.class);
                if (motif==null) continue;
                Object value=motif.getPropertyValue(propertyName, engine);
                if (value instanceof Number && map instanceof MotifNumericMap) ((MotifNumericMap)map).setValue(motifname, ((Number)value).doubleValue());
                else if (value!=null && map instanceof MotifTextMap) {
                    if (value instanceof List) ((MotifTextMap)map).setValue(motifname,MotifLabEngine.splice((List)value, ","));
                    else ((MotifTextMap)map).setValue(motifname,value.toString());
                }
            }
            return map;
        } else {
           String name_prefix="";
           String name_suffix="";
           if (variablename.contains(";")) { // possibly name prefix and suffix
               String parts[]=variablename.trim().split("\\s*;\\s*");
               variablename=parts[0];
               if (parts.length>1) {
                   String[] parts2=parts[1].split("\\s*,\\s*");
                   for (String pair:parts2) {
                       if (!pair.contains("=")) continue;
                       String[] keyvalue=pair.trim().split("\\s*=\\s*");
                       if (keyvalue.length<2) continue;
                       if (keyvalue[0].equalsIgnoreCase("name_prefix") || keyvalue[0].equalsIgnoreCase("prefix")) name_prefix=keyvalue[1];
                       else if (keyvalue[0].equalsIgnoreCase("name_suffix") || keyvalue[0].equalsIgnoreCase("suffix")) name_suffix=keyvalue[1];
                   }
               }
           }
           String transform=(variablename.contains(":"))?variablename.substring(0,variablename.indexOf(':')):variablename;
           String parameters=(variablename.contains(":"))?variablename.substring(variablename.indexOf(':')+1):"";
           String targetName="tempCollection";
           MotifCollection data=new MotifCollection(targetName);             
           for (String motifname:storage) {
              String transformString=transform+":"+motifname+((parameters.isEmpty())?"":(","+parameters));
              String newmotifname=name_prefix+motifname+name_suffix;
              Motif motif=Motif.parseMotifParameters(transformString, newmotifname, engine); // this can throw an ExecutionError if the syntax is wrong
              data.addMotifToPayload(motif);
           }   
           return data;                 
        }
    }

    @Override
    public Class getResultType(String variablename) {
        if (!hasResult(variablename)) {
            return null;
        } else if (variablename.equalsIgnoreCase("size") ) {
            return NumericVariable.class;
        } else if (variablename.startsWith("motif:") ) {
            String propertyName=variablename.substring("motif:".length());
            if (propertyName.equals("<property>")) return MotifTextMap.class; // just in case
            Class propclass=Motif.getPropertyClass(propertyName, MotifLabEngine.getEngine());
            if (propclass==null) return MotifTextMap.class; // just in case
            else if (Number.class.isAssignableFrom(propclass)) return MotifNumericMap.class;
            else return MotifTextMap.class; 
        } else return MotifCollection.class; // all other results are Motif Collections

    }

    @Override
    public boolean hasResult(String variablename) {
        variablename=variablename.toLowerCase();
        if ( variablename.equals("size")) return true;
        else if ( variablename.startsWith("random")) return true;
        else if ( variablename.startsWith("complement")) return true;
        else if ( variablename.startsWith("reverse")) return true;
        else if ( variablename.startsWith("inverse")) return true;
        else if ( variablename.startsWith("flank:")) return true;
        else if ( variablename.startsWith("shuffle")) return true;
        else if ( variablename.startsWith("trim:")) return true;
        else if ( variablename.startsWith("trim flanks:")) return true;
        else if ( variablename.startsWith("combine:")) return true;        
        else if ( variablename.startsWith("round")) return true;
        else if ( variablename.startsWith("motif:")) return true;
        return false;
    }  
    
    @Override
    public Class getMembersClass() {
        return Motif.class;
    }    
    
    /**
     * Returns the presentation names of all the Motifs in this dataset
     * 
     * @return A list of Motif presentation names
     */
    public ArrayList<String> getAllMotifPresentationNames(MotifLabEngine engine) {
        ArrayList<String> list = new ArrayList<String>(storage.size());
        for (String name:storage) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Motif) list.add( ((Motif)item).getPresentationName() );
        }
        return list;
    }
    
    /**
     * Returns the short names of all the Motifs in this dataset
     * 
     * @return A list of Motif presentation names
     */
    public ArrayList<String> getAllMotifShortNames(MotifLabEngine engine) {
        ArrayList<String> list = new ArrayList<String>(storage.size());
        for (String name:storage) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Motif) list.add( ((Motif)item).getShortName() );
        }
        return list;
    }    

    /**
     * Returns all the Motif objects in this dataset
     * (if they are currently registered with the engine)
     * 
     * @return A list of Motif objects
     */
    public ArrayList<Motif> getAllMotifs(MotifLabEngine engine) {
        ArrayList<Motif> list = new ArrayList<Motif>(storage.size());
        for (String name:storage) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Motif) list.add((Motif)item);
        }
        return list;
    }    
    
    
            
    /**
     * Returns the Motif corresponding to the given name.
     * 
     * @param name The name of the Motif
     * @return the specified Motif (if found) or null
     */
    public Motif getMotifByName(String name, MotifLabEngine engine) {
        for (String motifname : storage) {
            if (motifname.equals(name)) {
                Data item=engine.getDataItem(name); 
                if (item!=null && item instanceof Motif) return (Motif)item;
            }
        }
        return null;
    }
    
    /**
     * Returns the name of the Motif corresponding to the given index.
     * This method could be used if you want to iterate through all Motifs
     * in a dataset. 
     * 
     * @param index The index of the Motif
     * @return the name of the Motif at the specified index (if exists) or null
     */
    public String getMotifNameByIndex(int index) {
        return storage.get(index);
    }
    /**
     * Returns the Motif corresponding to the given index.
     * This method could be used if you want to iterate through all Motifs
     * in a dataset. 
     * 
     * @param index The index of the Motif
     * @return the specified Motif (if exists) or null
     */
    public Motif getMotifByIndex(int index, MotifLabEngine engine) {
        String name=storage.get(index);
        Motif motif=null;
        if (name!=null) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Motif) return (Motif)item;            
        }
        return motif;
    }
    
    /**
     * Returns the index (order within the dataset) of the Motif with the given name 
     * If no Motif with the given name exists within the dataset the value -1 is returned.
     * 
     * @param name The name of the Motif
     * @return index of the Motif (between 0 and size-1) or -1
     */
    public int getIndexForMotif(String name) {
        for (int i=0;i<storage.size();i++) {
            String motifName=storage.get(i);
            if (motifName.equals(name)) return i;
        }
        return -1;
    }
    
    /** 
     * Returns true if the specified Motif is in this collection 
     */
    public boolean contains(Motif motif) {
        if (motif==null) return false;
        String motifName=motif.getName();
        return contains(motifName);
    }
    
    /** 
     * Returns true if a Motif with the given name is in this collection
     */
    @Override
    public boolean contains(String motifName) {
        return storage.contains(motifName);
    }
    
    /**
     * Returns true if this MotifCollection objects contains the same Motifs
     * as the given collection and they are derived in the same way 
     * @param collection
     * @return
     */
    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof MotifCollection)) return false;
        MotifCollection collection=(MotifCollection)other;
        if (size()!=collection.size()) return false;
        
        if (!hasSameConstructor((MotifCollection)other)) return false;        
        for (String name:storage) {
            if (!collection.contains(name)) return false;
        }
        return true;
    }
               
   /**
     * Adds a new Motif object to this collection (if it is not already present)
     * 
     * @param motif The Motif to be added
     */
    public void addMotif(Motif motif) {
        if (motif==null || storage.contains(motif.getName())) return;
        storage.add(motif.getName()); // add to local storage
        notifyListenersOfDataAddition(motif);
    }

   /**
     * Adds the name of a motif to this collection (if it is not already present)
     * Note that this method is not the preferred way of adding motifs.
     * It is requested that one uses addMotif(Motif motif) instead since that method
     * will also notify any listeners that the collection has been updated
     * @param name The name of the motif to be added
     */
    public void addMotifName(String name) {
        if (name!=null && !name.isEmpty() && !storage.contains(name)) storage.add(name); //
    }
    
   /**
     * Adds the names of motifs to this collection (if they are not already present)
     * Note that this method is not the preferred way of adding motifs.
     * It is requested that one uses addMotif(Motif motif) instead since that method
     * will also notify any listeners that the collection has been updated
     * @param names The names of the motifs to be added
     */
    public void addMotifNames(Collection<String> names) {
        if (names==null || names.isEmpty()) return;
        for (String name:names) {
          if (!storage.contains(name)) storage.add(name);      
        }
    }    
    
   /**
     * Removes a Motif object from this collection (if present)
     * 
     * @param motif The Motif to be remvoed
     */
    public void removeMotif(Motif motif) {
        boolean hasremoved=storage.remove(motif.getName()); // remove from local storage
        if (hasremoved) notifyListenersOfDataRemoval(motif);
    }
    
    /** Renames a reference to a motif in this collection with the given oldname
     *  to reference a motif with the given newname
     *  (Don't do this unless you know what you are doing)
     */
    public void renameMotifReference(String oldname, String newname) {
        int index=storage.indexOf(oldname);
        if (index>0) storage.set(index, newname);
    }

     /** Renames reference to motifs in this collection based on the given mapping
      *  from old names to new names
      *  (Don't do this unless you know what you are doing)
      */
    public void renameMotifReferences(HashMap<String, String> namesmapping) {
        for (int i=0;i<storage.size();i++) {
            String oldname=storage.get(i);
            String newname=namesmapping.get(oldname);
            if (newname!=null) storage.set(i, newname);
        }
    }

    @Override
    public void clearAll(MotifLabEngine engine) {
        String[] list=new String[storage.size()];
        for (int i=0;i<list.length;i++) {list[i]=storage.get(i);}
        for (String name:list) {
            storage.remove(name);
            Data item=null;
            if (engine!=null) item=engine.getDataItem(name);
            if (item!=null && item instanceof Motif) notifyListenersOfDataRemoval((Motif)item);
        }
        predefined=null;
        fromTrack=null;
        fromMap=null;
        fromList=null;
        fromProperty=null;
        clearConstructorString();
    }
    
    
    /*
     * Reorders the list of motifs in the dataset by moving the motif
     * at the specified current position to the new position.
     * 
     * @param oldposition Position of the motif to be moved
     * @param newposition New position to move motif to
     *
    public void reorderMotifs(int currentposition, int newposition) {
        String temp=storage.remove(currentposition);
        storage.add(newposition,temp);
        notifyListenersOfDataUpdate();
    }   
    */
    
    /**
     * Returns the number of Motifs in this collection
     * 
     * @return number of motifs
     */
    public int getNumberofMotifs() {
        return storage.size();
    }
    
    /**
     * Returns the number of motifs in this collection (same as getNumberofMotifs)
     *
     * @return number of motifs
     */
    @Override
    public int size() {
        return storage.size();
    }
    
   /**
     * Returns TRUE if this MotifCollection contains no motifs
     */
    @Override
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    

    
    @Override
    public String output() {
        StringBuilder string=new StringBuilder();
        for (int i=0;i<storage.size();i++) {
           string.append(storage.get(i));
           string.append("\n");
        }
        return string.toString();
    }

    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        clearAll(engine);
        for (String line:input) {
            line=line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            Data data=engine.getDataItem(line);
            if (data==null) engine.logMessage("No such motif: "+line); // should this throw ParseError?
            else if (!(data instanceof Motif)) engine.logMessage("'"+line+"' is not a Motif object"); // should this throw ParseError?
            else addMotif((Motif)data);
        }
    }     
    
    /**
     * This method takes all motifs in the specified dataset and adds them
     * to this dataset
     * @param dataset The dataset to be incorporated into this dataset
     */
    public void merge(MotifCollection dataset) {
        int size=dataset.size();
        for (int i=0;i<size;i++) {
            String motifname=dataset.getMotifNameByIndex(i);
            if (!contains(motifname)) storage.add(motifname); // I use this directly instead of calling addMotif() to limit the number of notifications sent 
        }              
        notifyListenersOfDataAddition(dataset);
    }
    
    /**
     * This method removes all motifs from this collection that are also present
     * in the argument dataset
     * @param dataset A collection of motifs to remove from this collection
     */
    public void subtract(MotifCollection dataset) {
        int size=dataset.size();
        for (int i=0;i<size;i++) {
            String motifname=dataset.getMotifNameByIndex(i);
            if (contains(motifname)) storage.remove(motifname); // I use this directly instead of calling removeMotif() to limit the number of notifications sent 
        }              
        notifyListenersOfDataRemoval(dataset);
    }
    
    /**
     * This method compares the current contents of this collection with the argument collection
     * and removes any motifs from this collection that are not present in both
     * @param dataset A collection of motifs to keep in this collection (if the already exists). 
     * Remove all the rest
     */
    public void intersect(MotifCollection dataset) {
        MotifCollection removed=new MotifCollection("removed");
        int size=storage.size();
        for (int i=0;i<size;i++) { // first find which motifs to remove
            String motifname=storage.get(i);
            if (!dataset.contains(motifname)) {
                removed.storage.add(motifname);
            } 
        } 
        // then remove them
        size=removed.size();
        for (int i=0;i<size;i++) { // first find which motifs to remove
            String motifname=removed.getMotifNameByIndex(i);
            this.storage.remove(motifname);
        }                 
        notifyListenersOfDataRemoval(removed);
    }



    /**
     * This method returns a new MotifCollection that contains all motifs that
     * are not members of this MotifCollection
     * @return The complement MotifCollection
     */
    public MotifCollection complement(MotifLabEngine engine) {
        MotifCollection complement=new MotifCollection("complement");
        ArrayList<Data> allmotifs=engine.getAllDataItemsOfType(Motif.class);
        for (Data motif:allmotifs) {
            String motifname=motif.getName();
            if (!this.contains(motifname)) {
                complement.storage.add(motifname);
            }
        }
        return complement;
    }
    
    
    
    
    @Override
    public void importData(Data source) throws ClassCastException {
        if (source==this) return; // no need to import, the source and target are the same
        MotifCollection datasource=(MotifCollection)source;
        this.datasetName=datasource.datasetName;
        this.predefined=datasource.predefined;
        this.fromTrack=datasource.fromTrack;
        this.fromMap=datasource.fromMap;
        this.fromList=datasource.fromList;
        this.fromProperty=datasource.fromProperty;
        cloneConstructor(datasource);
        this.storage=datasource.storage; // do not clone, just import!
        this.payload=datasource.payload; // in case...
        //notifyListenersOfDataUpdate(); 
    }


    
    /**
     * This should only be used for testing
     */
    @Deprecated
    public void importMotifList(String[] list) {
        storage.clear();
        storage.addAll(Arrays.asList(list));
        //notifyListenersOfDataUpdate();
    }


    @Override
    public MotifCollection clone() {
        MotifCollection newcollection=new MotifCollection(datasetName);
        int size=size();
        for (int i=0;i<size;i++) {
            String motifName=getMotifNameByIndex(i);
            newcollection.storage.add(motifName); // I use this directly instead of calling addMotif() to limit the number of notifications sent 
        }  
        newcollection.predefined=this.predefined;
        newcollection.fromTrack=this.fromTrack;
        newcollection.fromMap=this.fromMap;
        newcollection.fromList=this.fromList;
        newcollection.fromProperty=this.fromProperty;   
        newcollection.cloneConstructor(this);
        return newcollection;
    }
       
    public void setPredefinedCollectionName(String collectionname) {
        setConstructorString(Operation_new.COLLECTION_PREFIX, collectionname);       
    }
    
    public String getPredefinedCollectionName() {
        return getConstructorString(Operation_new.COLLECTION_PREFIX);
    }    
    /** Returns TRUE if this MotifCollection returns a system predefined collection (like Transfac or JASPAR) */
    public boolean isPredefined() {
        return hasConstructorString(Operation_new.COLLECTION_PREFIX);
    }
    
    
    /** If this collection is based on which motifs are present in a motif track, this method will return a string describing the settings used for initialization */
    public String getFromTrackString() {
        return getConstructorString(Operation_new.FROM_TRACK_PREFIX);
    }    
    
    public void setFromTrackString(String fromTrackString) {
        setConstructorString(Operation_new.FROM_TRACK_PREFIX, fromTrackString);       
    }
    
    /** Returns TRUE if this MotifCollection is based on which motifs are present in a motif track */
    public boolean isFromTrack() {
        return hasConstructorString(Operation_new.FROM_TRACK_PREFIX);
    }
    
    /** If this collection is based on which Motifs satisfy a condition in a MotifNumericMap, this method will return a string describing the settings used for initialization */
    public String getFromMapString() {
         return getConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    /** Returns TRUE if this MotifCollection is based on which Motifs satisfy a condition in a MotifNumericMap */
    public boolean isFromMap() {
        return hasConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    
    public void setFromMapString(String fromMapString) {
        setConstructorString(Operation_new.FROM_MAP_PREFIX, fromMapString);        
    }
    
    /** If this collection is based on a (non-resolved) list of references, this method will return a string describing the settings used for initialization */
    public String getFromListString() {
        return getConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** Sets a string used for initialization of this collection (which includes references to motifs, collections, and partition clusters) */
    public void setFromListString(String liststring) {
        setConstructorString(Operation_new.FROM_LIST_PREFIX, liststring);         
    }
    /** Returns TRUE if this collection is based a (non-resolved) list of references (which could include references to collections and partition-clusters) */
    public boolean isFromList() {
        return hasConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** If this collection is based on a property condition, this method will return a string describing the settings used for initialization */
    public String getFromPropertyString() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }
    /** Returns TRUE if this MotifCollection is based on a property condition */
    public boolean isFromProperty() {
        return hasConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }    
    
    public void setFromPropertyString(String fromPropertyString) {
        setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,fromPropertyString);      
    }
    
    public static String getType() {return typedescription;}
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription+" : "+storage.size();}

   
    /** This method adds a motif to temporary storage within this collection.
     * (These temporarily stored motifs should be registered with the engine afterwards
     * using the initializeFromPayload() method, however, in recent version this
     * is done automatically in MotifLabEngine.storeDataItem(data) which checks if
     * the data item is a MotifCollection and calls initializeFromPayload())
     */
    public void addMotifToPayload(Motif motif) {
        if (payload==null) payload=new ArrayList<Motif>();
        payload.add(motif);
    }
    
    /** This method removes a motif from temporary storage within this collection.
     */
    public void removeMotifFromPayload(Motif motif) {
        if (payload!=null) payload.remove(motif);
    }

    /** This method removes a motif from temporary storage within this collection.
     */
    public void removeMotifFromPayloadAtIndex(int index) {
        if (payload==null || index<0 || index>=payload.size()) return;
        else payload.remove(index);
    }
    
    /** This method renames a motif existing in the temporary storage within this collection.
     *  @return returns the old name of the motif at that index
     */
    public String renameMotifInPayload(int index,String newname) {
        if (payload==null || index<0 || index>=payload.size()) return null;  
        Motif motif=payload.get(index);
        String oldname=motif.getName();
        motif.rename(newname);
        return oldname;
    }
    
    /** This method returns the List of Motifs temporarily stored in the payload of this collection
     */
    public List<Motif> getPayload() {
        return payload;
    }
    
    /** This method returns the number of Motifs temporarily stored in the payload of this collection
     */
    public int getPayloadSize() {
        if (payload==null) return 0;
        else return payload.size();
    }
    
    /** This registers all Motifs temporarily stored in the payload and deletes the payload storage */
    @Override
    public void initializeFromPayload(MotifLabEngine engine) throws ExecutionError {
        //System.err.println("Initializing MotifCollection from payload. Payload="+(payload!=null));
        if (payload==null) return;
        for (Motif motif:payload) {
            engine.updateDataItem(motif);
            this.addMotif(motif);
        }
        payload=null; // clear payload after initialization!
    }

    @Override
    public boolean hasPayload() {
        if (payload==null) return false;
        else return !payload.isEmpty();
    }

    public boolean isImportedCollection() {
        return (hasPayload() && predefined==null);
    }
    

/** Given the first line of a file, this method returns the name of the 
 *  dataformat believed to be used for the file, or null if the format is
 *  not recognized
 */
public static String determineDataFormatFromHeader(String firstline) {
         if (firstline.startsWith("#INCLUSive Motif")) return "INCLUSive_Motif_Model";
    else if (firstline.startsWith("#MotifLabMotif")) return "MotifLabMotif";
    else return null;
}


/**
 * Creates and returns a new MotifCollection based on a parameter string
 * @param text The parameter string to be parsed
 * @param targetName The new name of the MotifCollection (only used if a new collection is created)
 * @param notfound If this parameter is NULL the method will throw an ExecutionError if any entries in the parameter list is not found (the first error will be reported)
 *                 If this parameter is an (empty) ArrayList, the method will be run in 'silent-mode' and not throw exceptions upon encountering errors.
 *                 The ArrayList will be filled with the names that could not be properly resolved (with the reason in parenthesis),
 *                 and the returned collection will only contain those sequences that could successfully be resolved.
 * @param engine
 * @return
 * @throws ExecutionError
 * @throws InterruptedException
 */
public static MotifCollection parseMotifCollectionParameters(String text, String targetName,  ArrayList<String> notfound, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        boolean silentMode=false;       
        if (notfound!=null) {silentMode=true;notfound.clear();}
        if (text.startsWith(Operation_new.COLLECTION_PREFIX)) { // predefined collection
            String collectionName=text.substring(Operation_new.COLLECTION_PREFIX.length());
            String filename=engine.getFilenameForMotifCollection(collectionName);
            if (filename==null) throw new ExecutionError("Unknown Motif Collection: "+collectionName);
            BufferedReader inputStream=null;
            ArrayList<String> input=new ArrayList<String>();
            try {
                //inputStream=new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/org/motiflab/engine/resources/"+filename)));
                inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)))); // these are locally installed files, not repository files
                String line;
                while((line=inputStream.readLine())!=null) {input.add(line);}
            } catch (IOException e) {
                throw new ExecutionError("An error occurred when loading predefined Motif Collection: ["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
            } finally {
                try {
                    if (inputStream!=null) inputStream.close();
                } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader Operation_new.parseMotifCollectionParameters(): "+ioe.getMessage());}
            }
            DataFormat format = engine.getDataFormat("MotifLabMotif");
            if (format==null) throw new ExecutionError("Unknown Dataformat: MotifLabMotif");
            MotifCollection data=new MotifCollection(targetName);
            try {data=(MotifCollection)format.parseInput(input, data, null, task);}
            catch (InterruptedException ie) {throw ie;}
            catch (Exception e) {throw new ExecutionError(e.getMessage());}
            data.setPredefinedCollectionName(collectionName);
            return data;
        } else if (text.startsWith(Operation_new.RANDOM_PREFIX)) {
            String configstring=text.substring(Operation_new.RANDOM_PREFIX.length()).trim();
            String[] parts=configstring.split("\\s+from\\s+|\\*s,\\s*");                        
            if (parts[0].isEmpty()) throw new ExecutionError("Missing size specification for random collection");
            double number=0;
            boolean percentage=false;
            if (parts[0].endsWith("%")) {parts[0]=parts[0].substring(0,parts[0].length()-1).trim();percentage=true;}
            Data data=engine.getDataItem(parts[0]);
            if (data!=null) {
                if (data instanceof NumericVariable) number=((NumericVariable)data).getValue();
                else throw new ExecutionError("'"+parts[0]+"' is not a valid integer number or Numeric Variable");
            } else {
                try {
                    number=Double.parseDouble(parts[0]);
                } catch (NumberFormatException e) {throw new ExecutionError("'"+parts[0]+"' is not a valid literal number or Numeric Variable");}
            }
            ArrayList<String> names=null;
            if (parts.length>1) {
                Data sourceCollection=engine.getDataItem(parts[1]);
                if (sourceCollection instanceof MotifCollection) names=((MotifCollection)sourceCollection).getAllMotifNames();
                else throw new ExecutionError("'"+parts[1]+"' is not a Motif Collection");
            } else {
                names=engine.getNamesForAllDataItemsOfType(Motif.class);
            }
            if (percentage) number=number*(double)names.size()/100.0;
            number=Math.round(number);
            MotifCollection collection=new MotifCollection(targetName);           
            if (number<names.size()) {
                Collections.shuffle(names);
                for (int i=0;i<number;i++) collection.addMotifName(names.get(i));
            } else {
                collection.addMotifNames(names);
            }
            return collection;
        } else if (isTransformConstructor(text)) {
           String name_prefix="";
           String name_suffix="";
           if (text.contains(";")) { // possibly name prefix and suffix
               String parts[]=text.trim().split("\\s*;\\s*");
               text=parts[0];
               if (parts.length>1) {
                   String[] parts2=parts[1].split("\\s*,\\s*");
                   for (String pair:parts2) {
                       if (!pair.contains("=")) continue;
                       String[] keyvalue=pair.trim().split("\\s*=\\s*");
                       if (keyvalue.length<2) continue;
                       if (keyvalue[0].equalsIgnoreCase("name_prefix") || keyvalue[0].equalsIgnoreCase("prefix")) name_prefix=keyvalue[1];
                       else if (keyvalue[0].equalsIgnoreCase("name_suffix") || keyvalue[0].equalsIgnoreCase("suffix")) name_suffix=keyvalue[1];
                   }
               }
           }
           if (text.endsWith(":") || text.endsWith(",")) throw new ExecutionError("Missing required parameters for '"+text+"'");
           String transform=text.substring(0,text.indexOf(':'));
           String parametersString=text.substring(text.indexOf(':')+1);
           String collectionName="";
           String parameters="";
           if (parametersString.contains(",")) {
               collectionName=parametersString.substring(0,parametersString.indexOf(',')); // the first parameter is the target collection
               parameters=parametersString.substring(parametersString.indexOf(',')+1); // the rest are specific to the chosen transform
           } else collectionName=parametersString;
           Data collection=engine.getDataItem(collectionName);
           if (collection==null) throw new ExecutionError("Unknown Motif Collection: "+collectionName);
           if (!(collection instanceof MotifCollection)) throw new ExecutionError("'"+collectionName+"' is not a Motif Collection");
           MotifCollection data=new MotifCollection(targetName);             
           for (String motifname:((MotifCollection)collection).getAllMotifNames()) {
              String transformString=transform+":"+motifname+((parameters.isEmpty())?"":(","+parameters));
              String newmotifname=name_prefix+motifname+name_suffix;
              Motif motif=Motif.parseMotifParameters(transformString, newmotifname, engine); // this can throw an ExecutionError if the syntax is wrong
              data.addMotifToPayload(motif);
           }   
           return data;               
        } else if (text.startsWith(Operation_new.FROM_MAP_PREFIX)) {
           String configstring=text.substring(Operation_new.FROM_MAP_PREFIX.length());
           String mapName="";
           String operator="";
           String firstOperand=null; 
           String secondOperand=null; 
           try {
               String[] parseElements=parseMapConfigurationString(configstring,engine);
               mapName=(String)parseElements[0];
               operator=(String)parseElements[1];
               firstOperand=(String)parseElements[2];
               if (parseElements[3]!=null) secondOperand=(String)parseElements[3];
           } catch (ParseError e) {
               throw new ExecutionError(e.getMessage());
           }
           Data numericMap=engine.getDataItem(mapName);
           if (numericMap==null) throw new ExecutionError("Unknown data object '"+mapName+"'");
           if (!(numericMap instanceof MotifNumericMap)) throw new ExecutionError("'"+mapName+"' is not a Motif Numeric Map");
           MotifCollection data=new MotifCollection(targetName);
           data.createCollectionFromMap((MotifNumericMap)numericMap, operator, firstOperand, secondOperand, engine);
           return data;
        } else if (text.startsWith(Operation_new.FROM_PROPERTY_PREFIX)) {
           String configstring=text.substring(Operation_new.FROM_PROPERTY_PREFIX.length());
           String propertyName="";
           String operator="";
           String[] operands=null;
           try {
               Object[] parseElements=parsePropertyConfigurationString(configstring,engine);
               propertyName=(String)parseElements[0];
               operator=(String)parseElements[1];
               operands=(String[])parseElements[2];
           } catch (ParseError e) {
               throw new ExecutionError(e.getMessage());
           }
           MotifCollection data=new MotifCollection(targetName);
           data.createCollectionFromProperty(propertyName, operator, operands, engine);
           return data;
        } else if (text.startsWith(Operation_new.FROM_TRACK_PREFIX)) {
           MotifCollection newDataItem=new MotifCollection(targetName); // empty collection
           String configstring=text.substring(Operation_new.FROM_TRACK_PREFIX.length());
           boolean percentage=false;
           String trackName="";
           String quorum=null;
           String quorum2=null;           
           String operator=null;
           String sequenceCollectionName=null;
           try {
               String[] parseElements=parseTrackConfigurationString(configstring, engine);
               trackName=parseElements[0];
               operator=parseElements[1];
               quorum=parseElements[2];
               quorum2=parseElements[3];
               if (parseElements[4].equals(Boolean.TRUE.toString())) percentage=true;
               sequenceCollectionName=parseElements[5];
               
           } catch (ParseError e) {
               throw new ExecutionError(e.getMessage());
           }           
           Data trackitem=engine.getDataItem(trackName);
           if (trackitem==null) throw new ExecutionError("Unknown data object '"+trackName+"'");
           if (!(trackitem instanceof RegionDataset)) throw new ExecutionError("'"+trackName+"' is not a Region Dataset");
           SequenceCollection sequenceCollection=engine.getDefaultSequenceCollection();
           if (sequenceCollectionName!=null) {
               Data colitem=engine.getDataItem(sequenceCollectionName);
               if (colitem==null) throw new ExecutionError("Unknown data object '"+sequenceCollectionName+"'");
               if (colitem instanceof SequenceCollection) sequenceCollection=(SequenceCollection)colitem;
               else throw new ExecutionError("'"+sequenceCollectionName+"' is not a Sequence Collection");               
           }
           newDataItem.createCollectionFromTrack((RegionDataset)trackitem, quorum, quorum2, operator, percentage, sequenceCollection, engine, task);
           return newDataItem;
        } else { // initialize from a list of data names
           boolean keepConfig=false; // set this flag to true if the original config-string should be retained
           if (text.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               text=text.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
           }
           MotifCollection newDataItem=new MotifCollection(targetName); // empty collection

           String[] list=text.split("\\s*,\\s*");
           for (String entry:list) {
               int mode=Operation_new.UNION;
               if (entry.equals("+") || entry.equals("-") || entry.equals("&") || entry.equals("!")) if (silentMode) {notfound.add("Operator '"+entry+"' must be immediately followed by identifier (no space inbetween)");continue;} else throw new ExecutionError("Operator '"+entry+"' must be immediately followed by identifier (no space inbetween)");
               if (entry.startsWith("+") && entry.length()>1) {mode=Operation_new.UNION;entry=entry.substring(1);}
               if (entry.startsWith("-") && entry.length()>1) {mode=Operation_new.SUBTRACTION;entry=entry.substring(1);}
               if (entry.startsWith("&") && entry.length()>1) {mode=Operation_new.INTERSECTION;entry=entry.substring(1);}
               if (entry.startsWith("!") && entry.length()>1) {mode=Operation_new.COMPLEMENT;entry=entry.substring(1);}
               Data dataobject=null;
               if (entry.trim().isEmpty()) continue;
               if (entry.contains("->")) { // entry refers to a cluster within a Motif Partition
                   String[] elements=entry.split("->");
                   if (elements.length!=2) {
                       if (silentMode) {notfound.add(entry+" : syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                   }
                   String partition=elements[0];
                   String cluster=elements[1];
                   dataobject=engine.getDataItem(partition);
                        if (dataobject==null) {if (silentMode) {notfound.add(partition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+partition);}
                   else if (!(dataobject instanceof MotifPartition)) {if (silentMode) {notfound.add(partition+" : Not a Motif Partition"); continue;} else throw new ExecutionError("Data item '"+partition+"' is not a Motif Partition");}
                   else if (!((MotifPartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Motif Partition '"+partition+"' does not contain a cluster with the name '"+cluster+"'");}
                   else dataobject=((MotifPartition)dataobject).getClusterAsMotifCollection(cluster, engine);
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
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpressionInNumericRange(range1[0], range1[2], start, end, Motif.class);
                   MotifCollection tempCollection=new MotifCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addMotif((Motif)object);
                   }
                   combineWithCollection(newDataItem,tempCollection,mode,engine);
                   continue; 
               } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                   if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, Motif.class);
                   MotifCollection tempCollection=new MotifCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addMotif((Motif)object);
                   }
                   combineWithCollection(newDataItem,tempCollection,mode,engine);
                   continue; 
               } else { // entry refers to one motif or a motif collection
                   dataobject=engine.getDataItem(entry);
               }
                    if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
               else if (dataobject instanceof MotifPartition) {if (silentMode) notfound.add(entry+" : Missing cluster for Motif Partition"); else throw new ExecutionError("Missing specification of cluster for Motif Partition '"+entry+"'. (use format: Partition.Cluster)");}
               else if (!(dataobject instanceof Motif || dataobject instanceof MotifCollection)) {if (silentMode) notfound.add(entry+" : Not a Motif or Motif Collection"); else throw new ExecutionError("Data item '"+entry+"' is not a Motif or Motif Collection");}
               else combineWithCollection(newDataItem,dataobject,mode,engine);
           }
           if (keepConfig) newDataItem.setFromListString(text); // Store original config-string in data object
           return newDataItem;
        }
    }

    private static void combineWithCollection(MotifCollection col, Data dataobject, int mode, MotifLabEngine engine) {
            if (mode==Operation_new.SUBTRACTION) subtractFromMotifCollection(col,dataobject,engine);
       else if (mode==Operation_new.INTERSECTION) intersectWithMotifCollection(col,dataobject,engine);
       else if (mode==Operation_new.COMPLEMENT) addComplementToMotifCollection(col,dataobject,engine);
       else addToMotifCollection(col,dataobject,engine);
    }

 /** Adds a single Motif or Motif collection (other) to a target collection */
    private static void addToMotifCollection(MotifCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Motif) {
            target.addMotif((Motif)other);
        } else if (other instanceof MotifCollection) {
            for (Motif motif:((MotifCollection)other).getAllMotifs(engine)) {
                target.addMotif(motif);
            }
        } else {
            System.err.println("SYSTEM ERROR: In MotifCollection.addToMotifCollection. Parameter is neither Motif nor Motif collection but rather: "+other.getClass().getSimpleName());
        }
    }

    /** Removes all motifs from the target collection which is not present in the other Motif collection (or single motif) */
    private static void intersectWithMotifCollection(MotifCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Motif) {
            if (target.contains((Motif)other)) {
                target.clearAll(engine);
                target.addMotif((Motif)other);
            } else target.clearAll(engine);
        } else if (other instanceof MotifCollection) {
            for (Motif motif:target.getAllMotifs(engine)) {
                if (!((MotifCollection)other).contains(motif)) target.removeMotif(motif);
            }
        } else {
            System.err.println("SYSTEM ERROR: In MotifCollection.intersectWithMotifCollection. Parameter is neither Motif nor Motif collection but rather: "+other.getClass().getSimpleName());
        }

    }
     /** Subtracts a single Motif or Motif collection (other) from a target collection */
    private static void subtractFromMotifCollection(MotifCollection target, Object other, MotifLabEngine engine) {
         if (other==null) return;
        if (other instanceof Motif) {
            target.removeMotif((Motif)other);
        } else if (other instanceof MotifCollection) {
            for (Motif motif:((MotifCollection)other).getAllMotifs(engine)) {
                target.removeMotif(motif);
            }
        } else {
            System.err.println("SYSTEM ERROR: In MotifCollection.subtractFromMotifCollection. Parameter is neither Motif nor Motif collection but rather: "+other.getClass().getSimpleName());
        }
    }

 /** Adds the complement of a single Motif or Motif collection (other) to a target collection */
    private static void addComplementToMotifCollection(MotifCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Motif) {
            for (Data motif:engine.getAllDataItemsOfType(Motif.class)) {
                if (motif!=other) target.addMotif((Motif)motif);
            }
        } else if (other instanceof MotifCollection) {
            for (Data motif:engine.getAllDataItemsOfType(Motif.class)) {
                if (!((MotifCollection)other).contains((Motif)motif)) target.addMotif((Motif)motif);
            }
        } else {
            System.err.println("SYSTEM ERROR: In MotifCollection.addComplementToMotifCollection. Parameter is neither Motif nor Motif collection but rather: "+other.getClass().getSimpleName());
        }
    }

    
     /**
     * Initialize this collection based on which motifs that satisfy a condition with respect to a specified property
     *
     * @param property The name of the property 
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in, not in, equals, not equals, matches, not matches )
     * @param operands A list of possible values for the property
      */
   public void createCollectionFromProperty(String property, String operator, String[] operands, MotifLabEngine engine) throws ExecutionError {
       Class typeclass=Motif.getPropertyClass(property,engine);
       if (typeclass==null) throw new ExecutionError("Unknown motif property: "+property);
       if (operands==null || operands.length==0) throw new ExecutionError("Missing property value");       
       storage.clear();
       Object[] resolvedoperands=null;
       if (Number.class.isAssignableFrom(typeclass)) { // either a single operand or two for ranges
           resolvedoperands=new Object[operands.length]; // resolve all operands, irrespective of the operator used
           for (int i=0;i<operands.length;i++) {
               resolvedoperands[i]=engine.getNumericDataForString(operands[i]);     
               if (resolvedoperands[i]==null) throw new ExecutionError("Property value '"+operands[i]+"' is not a numeric constant or applicable numeric data object");           
               if (resolvedoperands[i] instanceof NumericMap && !(resolvedoperands[i] instanceof MotifNumericMap)) throw new ExecutionError("Property value '"+operands[i]+"' is of a type not applicable in this context");               
           }
           if (operator.equals("in") || operator.equals("not in")) {
               if (operands.length!=2) throw new ExecutionError("The operator '"+operator+"' requires exactly two operand values (for lower and upper range limits) when applied to numeric properties");
           } else if (!operator.equals("=") && operands.length!=1) throw new ExecutionError("The operator '"+operator+"' requires a single operand value when applied to numeric properties");
       } else if (typeclass.equals(Boolean.class)) {   
           if (!(operator.equals("=") || operator.equals("<>"))) throw new ExecutionError("The operator should be either '=' or '<>' for boolean properties");
           if (operands.length!=1 || getBooleanValue(operands[0])==null) throw new ExecutionError("The property value should be a single YES/NO or TRUE/FALSE value for boolean properties");          
           resolvedoperands=new Object[]{getBooleanValue(operands[0])};
       } else if (typeclass.equals(String.class) || typeclass.equals(ArrayList.class)) {
           if (!(operator.equals("equals") || operator.equals("not equals") || operator.equals("matches")|| operator.equals("not matches")|| operator.equals("in")|| operator.equals("not in"))) throw new ExecutionError("The operator should be either '(not) equals', '(not) matches' or '(not) in' for text based properties");
           if (operator.equals("in") || operator.equals("not in")) { // the operands should be names of Text Variables
               resolvedoperands=new Object[operands.length];
               for (int i=0;i<operands.length;i++) {
                   String operand=operands[i];
                   Data data=engine.getDataItem(operand);
                   if (data instanceof TextVariable) resolvedoperands[i]=data;
                   else throw new ExecutionError("When using the '"+operator+"' operator with text based properties, the property values should refer to Text Variables");
               }
           } else {
               resolvedoperands=operands;
               for (int i=0;i<resolvedoperands.length;i++) {
                   resolvedoperands[i]=MotifLabEngine.addQuotes((String)resolvedoperands[i]); // add double quotes around strings
               } 
           } // use literal string values
       } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: class '"+typeclass+"' not recognized by MotifCollection.createCollectionFromProperty()");
       // configure 
       String spliced=MotifLabEngine.splice(operands, ",");
       StringBuilder builder=new StringBuilder();
       builder.append(property);
       if (operator.matches("[\\w\\s]+")) builder.append(" "+operator+" ");
       else builder.append(operator);
       builder.append(spliced);
       setFromPropertyString(builder.toString());// sets the 'config' string

       ArrayList<Data> allMotifs=engine.getAllDataItemsOfType(Motif.class);
       for (Data motif:allMotifs) {
           Object value=null;
           String motifName=motif.getName();
           try {
              value=((Motif)motif).getPropertyValue(property,engine); 
              if (value==null) continue; // do not include motifs which do not have a value for the property (no matter which operator is used)
           } catch (Exception e) {continue;}
           boolean satisfies=false;
           if (Boolean.class.equals(typeclass)) satisfies=motifSatisfiesBooleanPropertyCondition(value,operator,(Boolean)resolvedoperands[0]);
           else if (Number.class.isAssignableFrom(typeclass)) satisfies=motifSatisfiesNumericPropertyCondition(value,motifName,operator,resolvedoperands);
           else satisfies=motifSatisfiesStringPropertyCondition(value,operator,resolvedoperands);
           if (satisfies) storage.add(motifName);
       }   
   }
   
   private double resolveNumericValue(Object value, String motifname) throws ExecutionError{
            if (value instanceof Integer) return ((Integer)value).doubleValue();
       else if (value instanceof Double) return ((Double)value).doubleValue();
       else if (value instanceof NumericVariable) return ((NumericVariable)value).getValue();
       else if (value instanceof MotifNumericMap) return ((MotifNumericMap)value).getValue(motifname);
       else if (value instanceof Data) throw new ExecutionError("Data object '"+value+"' can not be used in this context");
       else throw new ExecutionError("'"+value+"' is not a numeric constant or known data object");
   }
   
   private boolean motifSatisfiesBooleanPropertyCondition(Object motifPropertyValue, String operator, boolean targetValue) {
       if (motifPropertyValue instanceof Boolean) {
          if (operator.equals("=")) return ((Boolean)motifPropertyValue)==targetValue;
          else return ((Boolean)motifPropertyValue)!=targetValue;
       } else return false;
   }   
   
   private boolean motifSatisfiesNumericPropertyCondition(Object motifPropertyValue, String motifname, String operator, Object[] operands) throws ExecutionError {
       double motifvalue=0;
            if (motifPropertyValue instanceof Integer) motifvalue=((Integer)motifPropertyValue).doubleValue();
       else if (motifPropertyValue instanceof Double)  motifvalue=((Double)motifPropertyValue).doubleValue();
       else return false;
       double firstOperandValue=resolveNumericValue(operands[0],motifname);
       double secondOperandValue=(operands.length>1 && operands[1]!=null)?resolveNumericValue(operands[1],motifname):0;
            if (operator.equals("=") && operands.length==1) return motifvalue==firstOperandValue;
       else if (operator.equals("=")) return motifNumericValueInList(motifvalue,operands,motifname);            
       else if (operator.equals("<")) return motifvalue<firstOperandValue;
       else if (operator.equals("<=")) return motifvalue<=firstOperandValue;
       else if (operator.equals(">")) return motifvalue>firstOperandValue;
       else if (operator.equals(">=")) return motifvalue>=firstOperandValue;
       else if (operator.equals("<>")) return motifvalue!=firstOperandValue;
       else if (operator.equals("in")) return (motifvalue>=firstOperandValue && motifvalue<=secondOperandValue);
       else if (operator.equals("not in")) return !(motifvalue>=firstOperandValue && motifvalue<=secondOperandValue);
       else throw new ExecutionError("Unknown operator for numeric comparison '"+operator+"'");
   } 
   
   private boolean motifNumericValueInList(double motifValue, Object[] operands, String motifname) throws ExecutionError {
       for (Object operand:operands) {
           double operandValue=(operand!=null)?resolveNumericValue(operand,motifname):0;
           if (motifValue==operandValue) return true;
       }
       return false;
   }
   
   @SuppressWarnings("unchecked")
   private boolean motifSatisfiesStringPropertyCondition(Object motifPropertyValue, String operator, Object[] operands) throws ExecutionError {
       if (motifPropertyValue instanceof String) {
           String value=(String)motifPropertyValue;
           boolean isMatch=(operator.equals("matches") || operator.equals("not matches"))?stringPropertyMatches(value,operands):stringPropertyEquals(value, operands);
           if (operator.startsWith("not")) return !isMatch;
           else return isMatch; // 
       } else if (motifPropertyValue instanceof ArrayList) {
           boolean isMatch=false;
           for (String value:(ArrayList<String>)motifPropertyValue) {
              isMatch=(operator.equals("matches") || operator.equals("not matches"))?stringPropertyMatches(value,operands):stringPropertyEquals(value, operands);
              if (isMatch) break;
           }
           if (operator.startsWith("not")) return !isMatch;
           else return isMatch; //         
       } else return false;
   }   
   
   /** Returns TRUE if the motif value is found among the operand values else false */
   private boolean stringPropertyMatches(String motifvalue, Object[] operands) {
        for (Object operand:operands) {
           if (operand instanceof String) {
               String target=( ((String)operand).startsWith("\"") && ((String)operand).endsWith("\"") && ((String)operand).length()>2)?((String)operand).substring(1,((String)operand).length()-1):(String)operand;
               if (motifvalue.matches("(?i)"+target)) return true; // case insensitive match         
           }                
           else if (operand instanceof TextVariable && ((TextVariable)operand).matches(motifvalue)) return true;
        }
        return false;
   }
   /** Returns TRUE if the motifvalue is found among the operand values else false */
   private boolean stringPropertyEquals(String motifvalue, Object[] operands) {
        for (Object operand:operands) {
           if (operand instanceof String) {
               String target=( ((String)operand).startsWith("\"") && ((String)operand).endsWith("\"") && ((String)operand).length()>2)?((String)operand).substring(1,((String)operand).length()-1):(String)operand;
               if (motifvalue.equalsIgnoreCase(target)) return true;         
           } 
           else if (operand instanceof TextVariable && ((TextVariable)operand).contains(motifvalue)) return true;
        }
        return false;
   }   
   
   private Boolean getBooleanValue(String string) {
            if (string.equalsIgnoreCase("TRUE") || string.equalsIgnoreCase("YES")) return Boolean.TRUE;
       else if (string.equalsIgnoreCase("FALSE") || string.equalsIgnoreCase("NO")) return Boolean.FALSE;
       else return null;
   }

   
     /**
     * Initialize this collection based on which motifs satisfy a condition in a MotifNumericMap
     *
     * @param map The MotifNumericMap used as basis for the condition
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in, not in )
     * @param firstOperand A string representing a number used for comparisons. This could be a literal numeric constant or the name of a data object
     * @param secondOperand A string representing a second number used as upper limit if operator is "in". This could be a literal numeric constant or the name of a data object
     */
   public void createCollectionFromMap(MotifNumericMap map, String operator, String firstOperand, String secondOperand, MotifLabEngine engine) throws ExecutionError {
       Object firstOperandData=null;
       Object secondOperandData=null;
       if (firstOperand==null || firstOperand.isEmpty()) throw new ExecutionError("Missing numeric operand for comparison"); 
       firstOperandData=engine.getNumericDataForString(firstOperand);
       if (firstOperandData==null) throw new ExecutionError("'"+firstOperand+"' is not a numeric constant or known numeric data object");
       if ((firstOperandData instanceof Data) && !(firstOperandData instanceof NumericConstant || firstOperandData instanceof NumericVariable || firstOperandData instanceof MotifNumericMap)) throw new ExecutionError("'"+firstOperand+"' is of a type not applicable in this context");
       if (operator.equals("in") || operator.equals("not in")) {
           if (secondOperand==null) throw new ExecutionError("Missing upper limit for numeric range");
           secondOperandData=engine.getNumericDataForString(secondOperand);
           if (secondOperandData==null) throw new ExecutionError("'"+secondOperand+"' is not a numeric constant or known numeric data object");
           if ((secondOperandData instanceof Data) && !(secondOperandData instanceof NumericConstant || secondOperandData instanceof NumericVariable || secondOperandData instanceof MotifNumericMap)) throw new ExecutionError("'"+secondOperand+"' is of a type not applicable in this context");
       }     
       storage.clear();
       // configure
       String fromMapString;
       if (operator.equals("in") || operator.equals("not in")) {
          fromMapString=map.getName()+" "+operator+" ["+firstOperand+","+secondOperand+"]"; // sets the 'config' string
       } else {
          fromMapString=map.getName()+operator+firstOperand; // sets the 'config' string
       }
       setFromMapString(fromMapString);
       ArrayList<Data> allMotifs=engine.getAllDataItemsOfType(Motif.class);
       for (Data motif:allMotifs) {
           String motifName=motif.getName();
           if (motifSatisfiesMapCondition(motifName, map, operator, firstOperandData, secondOperandData)) storage.add(motifName);
       }
   }

   /** Returns TRUE if the motif with the given name satisfies the condition in the map */
   private boolean motifSatisfiesMapCondition(String motifName, MotifNumericMap map, String operator, Object firstOperandData, Object secondOperandData) {
       double firstOperand=0;
       double secondOperand=0;
            if (firstOperandData instanceof Integer) firstOperand=((Integer)firstOperandData).intValue();
       else if (firstOperandData instanceof Double) firstOperand=((Double)firstOperandData).doubleValue();
       else if (firstOperandData instanceof NumericVariable) firstOperand=((NumericVariable)firstOperandData).getValue();
       else if (firstOperandData instanceof NumericConstant) firstOperand=((NumericConstant)firstOperandData).getValue();
       else if (firstOperandData instanceof MotifNumericMap) firstOperand=((MotifNumericMap)firstOperandData).getValue(motifName);
            if (secondOperandData instanceof Integer) secondOperand=((Integer)secondOperandData).intValue();
       else if (secondOperandData instanceof Double) secondOperand=((Double)secondOperandData).doubleValue();
       else if (secondOperandData instanceof NumericVariable) secondOperand=((NumericVariable)secondOperandData).getValue();
       else if (secondOperandData instanceof NumericConstant) secondOperand=((NumericConstant)secondOperandData).getValue();
       else if (secondOperandData instanceof MotifNumericMap) secondOperand=((MotifNumericMap)secondOperandData).getValue(motifName);  
       double motifValue=map.getValue(motifName);
            if (operator.equals("="))  return (motifValue==firstOperand);
       else if (operator.equals(">=")) return (motifValue>=firstOperand);
       else if (operator.equals(">"))  return (motifValue>firstOperand);
       else if (operator.equals("<=")) return (motifValue<=firstOperand);
       else if (operator.equals("<"))  return (motifValue<firstOperand);
       else if (operator.equals("<>")) return (motifValue!=firstOperand);
       else if (operator.equals("in")) return (motifValue>=firstOperand && motifValue<=secondOperand);
       else if (operator.equals("not in")) return (!(motifValue>=firstOperand && motifValue<=secondOperand));
       else return false;
   }
  
/**
 * Parses a configuration string for Motif Collection creation from a Numeric map
 * and returns an String[] containing parsed elements:
 * [0] Name of MotifNumericMap 
 * [1] Operator
 * [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 * @param configString. Format example: "MotifNumericMap >= 0.4" or "MotifNumericMap in [0,10]" or "MotifNumericMap <> NumericVariable1"
 * @return
 */
   public static String[] parseMapConfigurationString(String configstring, MotifLabEngine engine) throws ParseError {
       Pattern pattern=Pattern.compile("\\s*([a-zA-Z_0-9]+)\\s*(>=|<=|<>|<|=|>| in | not in )\\s*\\[?\\s*([\\w\\d\\.\\-]+)(\\s*,\\s*([\\w\\d\\.\\-]+))?\\s*\\]?");
       Matcher matcher=pattern.matcher(configstring);
       String mapName=null;
       String operator=null;
       String firstOperand=null;
       String secondOperand=null;
       if (matcher.find()) {
           mapName=matcher.group(1);
           operator=matcher.group(2).trim();
           firstOperand=matcher.group(3);
           if (matcher.group(5)!=null && !matcher.group(5).isEmpty()) { // second operand
               secondOperand=matcher.group(5);
           }
           if ((operator.equals("in") || operator.equals("not in")) && secondOperand==null) throw new ParseError("Missing upper limit for numeric range");
       } else throw new ParseError("Unable to parse 'Map' parameter for new Motif Collection");
       return new String[]{mapName,operator,firstOperand,secondOperand};
   }
   
/**
 * Parses a configuration string for Motif Collection creation from a Property
 * and returns an Object[] containing parsed elements:
 * [0] Name of Property 
 * [1] Operator
 * [2] List of operands, as a String[]
 * @param configString. Format example: "Short name = Ap1,STAT1" or "GC-content in 0.3,0.6"
 * @return
 */
   public static Object[] parsePropertyConfigurationString(String configstring, MotifLabEngine engine) throws ParseError {
       Pattern pattern=Pattern.compile("\\s*(\\S.*?)\\s*( not in | in | not equals | equals | not matches | matches |>=|<=|<>|>|=|<)(.+)");
       Matcher matcher=pattern.matcher(configstring);
       String propertyName=null;
       String operator=null;
       String[] operands=null;
       if (matcher.find()) {
           propertyName=matcher.group(1).trim();
           operator=matcher.group(2).trim();
           String rest=matcher.group(3);
           if (rest==null || rest.trim().isEmpty()) throw new ParseError("Missing value for 'Property' parameter");
           operands=MotifLabEngine.splitOnCommaToArray(rest); // rest.trim().split("\\s*,\\s*");         
       } else throw new ParseError("Unable to parse 'Property' parameter for new Motif Collection");
       if (propertyName.startsWith("\"") && propertyName.endsWith("\"")) propertyName=propertyName.substring(1,propertyName.length()-1);
       for (int i=0;i<operands.length;i++) { // remove quotes
           if (operands[i].startsWith("\"") && operands[i].endsWith("\"")) operands[i]=operands[i].substring(1,operands[i].length()-1);
       }
       return new Object[]{propertyName,operator,operands};
   }   
    
/**
 * Parses a configuration string for Motif Collection creation from a Motif Track 
 * and returns an String[] containing parsed elements:
 * [0] Name of Motif Track 
 * [1] Operator
 * [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [4] "TRUE" or "FALSE". TRUE if numbers should be treated as percentages or FALSE if they are absolute numbers
 * [5] Name of sequence collection (or NULL if no collection is specified)
 * @param configString. Format example: "TFBS, support >= 20" or "TFBS, support in [0,10]" or "TFBS, support < 10, collection=Downregulated"
 * @return
 */
   public static String[] parseTrackConfigurationString(String configstring, MotifLabEngine engine) throws ParseError {
      Pattern pattern=Pattern.compile("support\\s*(>=|<=|<>|<|=|>| in | not in )\\s*(\\S.*)");      
      String trackName=null;
      String operator=null;
      String firstOperand=null;
      String secondOperand=null;
      String seqCollectionName=null;
      String percentage=Boolean.FALSE.toString();
      ArrayList<String> parts = MotifLabEngine.splitOnCharacter(configstring, ',' , '[', ']'); // split configstring on comma, but ignore commas within brackets
      trackName=parts.remove(0); // first element should always be track name
      trackName=trackName.trim();
      for (String part:parts) { // parse remaining arguments
          part=part.trim();
          if (part.isEmpty()) continue;
          if (part.startsWith("support")) {
              if (part.endsWith("%")) {part=part.substring(0,part.length()-1);percentage=Boolean.TRUE.toString();}
              Matcher matcher=pattern.matcher(part);      
              if (matcher.find()) {
                   operator=matcher.group(1).trim();
                   String operand=matcher.group(2).trim();  
                   if (operand.startsWith("[") && operand.endsWith("]")) {
                       if (!(operator.equals("in") || operator.equals("not in"))) throw new ParseError("Numeric ranges can only be used in combination with the 'in' comparator");
                       operand=operand.substring(1,operand.length()-1).trim();
                       String[] operandparts=operand.split("\\s*,\\s*");
                       if (operandparts.length!=2) throw new ParseError("Expected two comma-separated values within brackets for numeric range. Found "+operandparts.length+" value"+((operandparts.length!=1)?"s":""));
                       firstOperand=operandparts[0];
                       secondOperand=operandparts[1];                       
                   } else {
                       if (operator.equals("in") || operator.equals("not in")) throw new ParseError("Expected two comma-separated values within brackets when using 'in' comparator");
                       firstOperand=operand;
                   }
              } else throw new ParseError("Unable to parse 'support' argument");
          } else if (part.startsWith("collection")) {
              int pos=part.indexOf("=");
              if (pos<0) throw new ParseError("Error in collection argument. Missing '=' ");
              if (pos==part.length()-1) throw new ParseError("Missing collection name"); // '=' is last character
              seqCollectionName=part.substring(pos+1).trim();
              if (seqCollectionName.isEmpty())  throw new ParseError("Missing collection name");
          } else throw new ParseError("Unrecognized argument: "+part);
      }
      if (trackName.isEmpty()) throw new ParseError("Missing name of motif track");      
      if (operator==null || firstOperand==null) throw new ParseError("Missing support specification");      
      return new String[]{trackName,operator,firstOperand,secondOperand,percentage,seqCollectionName};
   }

     /**
     * Initialize this collection based on which motifs (known to the engine)
     * are found in in a selected number of sequences in the motif track (region dataset) provided as argument
     * (note this it does not return a collection but populates THIS collection). 
     *
     * @param dataset The motif track (Region dataset) used as basis
     * @param quorum The quorum states the number of sequences the motif should be present in (comparison operand). This is a string which can be a literal number or the name of a data object
     * @param quorum2 The quorum2 states the upper limit on the number of sequences the motif should be present in (when operator 'in' is used). This is a string which can be a literal number or the name of a data object
     * @param operator Used for comparison (should be <, <=, =, >, >=,<>, in, not in)
     * @param percentage If this flag is TRUE the quorum number should be between 0 and 100 and will be interpreted as a percentage of the sequences
     *                   If the flag is FALSE the quorum number will be interpreted as the specific number of sequences
     * @param seqCollection Only consider sequences from this collection. Any percentage numbers are relative to the size of this collection (can be NULL)
     */
   public void createCollectionFromTrack(RegionDataset dataset, String quorum, String quorum2, String operator, boolean percentage, SequenceCollection seqCollection, MotifLabEngine engine, OperationTask task) throws ExecutionError,InterruptedException {
       if (seqCollection==null) seqCollection=engine.getDefaultSequenceCollection();
       String seqCollectionName=seqCollection.getName();
       Object quorumData=engine.getNumericDataForString(quorum);
       if (quorumData==null) throw new ExecutionError("'"+quorum+"' is not a numeric constant or known numeric data object");
       if ((quorumData instanceof Data) && !(quorumData instanceof NumericConstant || quorumData instanceof NumericVariable || quorumData instanceof MotifNumericMap)) throw new ExecutionError("'"+quorum+"' is of a type not applicable in this context");
       Object quorumData2=null;
       if (operator.equals("in") || operator.equals("not in")) {
           if (quorum2==null || quorum2.isEmpty()) throw new ExecutionError("Missing upper limit for numeric range");
           quorumData2=engine.getNumericDataForString(quorum2);
           if (quorumData2==null) throw new ExecutionError("'"+quorum2+"' is not a numeric constant or known numeric data object");
           if ((quorumData2 instanceof Data) && !(quorumData2 instanceof NumericConstant || quorumData2 instanceof NumericVariable || quorumData2 instanceof MotifNumericMap)) throw new ExecutionError("'"+quorum2+"' is of a type not applicable in this context");
       }

       MotifNumericMap support=new MotifNumericMap("support",0); // the number of sequences each motif appears in
       ArrayList<Data> allMotifs=engine.getAllDataItemsOfType(Motif.class);
       storage.clear();
       // configure
       setPredefinedCollectionName(null);
       String fromTrackString=null;
       if (operator.equals("in") || operator.equals("not in")) fromTrackString=dataset.getName()+",support "+operator+" ["+quorum+","+quorum2+"]"+((percentage)?"%":""); // sets the 'config' string
       else fromTrackString = dataset.getName() + ",support" + operator + quorum + ((percentage) ? "%" : ""); // sets the 'config' string
       if (!seqCollectionName.equals(engine.getDefaultSequenceCollectionName())) {
           fromTrackString+=",collection="+seqCollectionName;
       }      
       setFromTrackString(fromTrackString);
       int seqsize=seqCollection.getNumberofSequences();
       if (seqsize==0) {
           engine.logMessage("Warning: Sequence Collection '"+seqCollection.getName()+"' is empty",20);
           return; 
       }
       for (Data motif:allMotifs) {
           support.setValue(motif.getName(), new Integer(0)); // initialize the map
       }
       // count the number of sequences each motif occurs in
       ArrayList<FeatureSequenceData> sequences=dataset.getSequencesFromCollection(seqCollection);
       int size=sequences.size();
       int s=0;
       for (FeatureSequenceData sequence:sequences) {
           ArrayList<Region> allRegions=((RegionSequenceData)sequence).getOriginalRegions();
           HashSet<String> present=new HashSet<String>();
           for (Region region:allRegions) { // make a set of present region types (motifs)
               present.add(region.getType());
           }
           for (String motiftype:present) { // update motif counts by one if present in current sequence
               if (engine.dataExists(motiftype, Motif.class)) {
                   support.setValue(motiftype, support.getValue(motiftype)+1);
               }
           }
           s++;
           if (task!=null) {
               task.setProgress((int)(s*0.9),size); // scaling by 90% to leave some room for later
               if (Thread.interrupted() || task.isAborted()) throw new InterruptedException();
           }
       }
       for (Data motif:allMotifs) {
           String motifName=motif.getName();
           double firstOperand=0;
           double secondOperand=0;
                if (quorumData instanceof Integer) firstOperand=((Integer)quorumData).intValue();
           else if (quorumData instanceof Double) firstOperand=((Double)quorumData).doubleValue();
           else if (quorumData instanceof NumericVariable) firstOperand=((NumericVariable)quorumData).getValue();
           else if (quorumData instanceof NumericConstant) firstOperand=((NumericConstant)quorumData).getValue();
           else if (quorumData instanceof MotifNumericMap) firstOperand=((MotifNumericMap)quorumData).getValue(motifName);
                if (quorumData2 instanceof Integer) secondOperand=((Integer)quorumData2).intValue();
           else if (quorumData2 instanceof Double) secondOperand=((Double)quorumData2).doubleValue();
           else if (quorumData2 instanceof NumericVariable) secondOperand=((NumericVariable)quorumData2).getValue();
           else if (quorumData2 instanceof NumericConstant) secondOperand=((NumericConstant)quorumData2).getValue();
           else if (quorumData2 instanceof MotifNumericMap) secondOperand=((MotifNumericMap)quorumData2).getValue(motifName);
           if (percentage) {
               firstOperand=(int)Math.round((double)firstOperand/100.0*seqsize);
               secondOperand=(int)Math.round((double)secondOperand/100.0*seqsize);
           }
           if (motifSatisfiesMapCondition(motifName, support, operator, firstOperand, secondOperand)) storage.add(motifName);
       }
       if (task!=null) {
           task.setProgress(99); // 
       }       
   }
   
   /** Returns TRUE if the given constructor string is a special motif transform (such as "reverse", "trim", "flank" etc) */
   private static boolean isTransformConstructor(String string) {
       for (String cons:transforms) {
           if (MotifLabEngine.startsWithIgnoreCase(string,cons)) return true;
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
              if (this.predefined!=null) this.setConstructorString(Operation_new.COLLECTION_PREFIX, this.predefined);
         else if (this.fromTrack!=null) this.setConstructorString(Operation_new.FROM_TRACK_PREFIX, this.fromTrack);
         else if (this.fromMap!=null) this.setConstructorString(Operation_new.FROM_MAP_PREFIX, this.fromMap);
         else if (this.fromProperty!=null) this.setConstructorString(Operation_new.FROM_PROPERTY_PREFIX, this.fromProperty);
         else if (this.fromList!=null) this.setConstructorString(Operation_new.FROM_LIST_PREFIX, this.fromList);

         // clear legacy fields
         this.predefined=null;
         this.fromTrack=null; 
         this.fromMap=null; 
         this.fromProperty=null; 
         this.fromList=null;          
    }

}

