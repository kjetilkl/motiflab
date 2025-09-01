/*
 
 
 */

package org.motiflab.engine.data;

import java.awt.Color;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.protocol.StandardParametersParser;
import org.motiflab.engine.util.BronKerboschCliqueFinder;
import org.motiflab.engine.util.ClusterGraph;
import org.motiflab.gui.VisualizationSettings;


/**
 * ModuleCollections can be used to refer to collections of Modules
 * Although most get/set methods work directly with Module
 * objects rather than Module names (Strings), the internal mechanism of the ModuleCollection
 * itself revolves around a plain list of names for the Modules in the collection.
 * The methods that returns Module objects or lists do this by dynamically obtaining these Modules
 * from the Engine (on-the-fly) based on the internal list of names
 *
 * @author kjetikl
 */
public class ModuleCollection extends DataCollection {
    private static String typedescription="Module Collection";
    protected String modulecollectionName;
    protected ArrayList<String> storage; // this is a list of ModuleCRM names.  (The collection stores names rather than actual Module objects)
    private ArrayList<Data> payload=null; // this is used to temporarily store Modules (and maybe constituent Motifs) that should be registered with the engine
 
    private String fromList=null; // LEGACY FIELD: a configuration string used if this collection is based on a (non-resolved) list of references
    private String fromInteractions=null; // LEGACY FIELD: a configuration string used if this collection is created from known motif interactions
    private String fromTrack=null; // LEGACY FIELD: a configuration string used when this collection is based on a track. Format example: "<trackname>,support>=80%"     
    private String fromMap=null;   // LEGACY FIELD: a configuration string used when this collection is based on a map. Format example: "<mapname>,value>=0" or "mapname,value in [0,1]"
    /**
     * Constructs a new initially "empty" ModuleCRM collection with the given name
     *
     * @param modulecollectionName A name for this dataset
     */
   public ModuleCollection(String collectionName) {
       this.modulecollectionName=collectionName;
       storage=new ArrayList<String>(20);
   }

    /**
     * Specifies a new name for this dataset
     *
     * @param name the name for this dataset
     */
    public void setName(String name) {
        this.modulecollectionName=name;
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
        return modulecollectionName;
    }

    @Override
    public Object getValue() {return this;} // should maybe change later

    
    @Override
    public ArrayList<String> getValues() {
        return storage;
    }
    
    @Override
    public String[] getResultVariables() {
        return new String[]{"size","random X","random X%","module:<property>"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (!hasResult(variablename)) throw new ExecutionError("'" + getName() + "' does not have a result for '" + variablename + "'");
        else if (variablename.equalsIgnoreCase("size")) {
            NumericVariable result=new NumericVariable("size",size());
            return result;           
        } else if (variablename.startsWith("random")) {
            String[] parts=variablename.split("\\s+",2);
            if (parts.length==1) return new ModuleCollection("randomCollection");
            String configString=Operation_new.RANDOM_PREFIX+parts[1]+" from "+getName();
            try {
              ModuleCollection randomCollection=ModuleCollection.parseModuleCollectionParameters(configString, "randomCollection", null, engine, null);
              return randomCollection;
            } catch (Exception e) {
                if (e instanceof ExecutionError) throw (ExecutionError)e;
                else return null;
            }  
        } else if (variablename.startsWith("module:")) {
            String propertyName=variablename.substring("module:".length());
            if (propertyName.equals("<property>")) throw new ExecutionError("You must replace <property> with the actual name of a module property");
            Class propclass=ModuleCRM.getPropertyClass(propertyName, engine);
            if (propclass==null) throw new ExecutionError("'"+propertyName+"' is not a recognized module property");
            DataMap map=null;
            if (Number.class.isAssignableFrom(propclass)) map=new ModuleNumericMap("map_"+propertyName, 0);
            else map=new ModuleTextMap("map_"+propertyName, "");
            for (String modulename:storage) {
                ModuleCRM cisRegModule=(ModuleCRM)engine.getDataItem(modulename, ModuleCRM.class);
                if (cisRegModule==null) continue;
                Object value=cisRegModule.getPropertyValue(propertyName, engine);
                if (value instanceof Number && map instanceof ModuleNumericMap) ((ModuleNumericMap)map).setValue(modulename, ((Number)value).doubleValue());
                else if (value!=null && map instanceof ModuleTextMap) {
                    if (value instanceof List) ((ModuleTextMap)map).setValue(modulename,MotifLabEngine.splice((List)value, ","));
                    else ((ModuleTextMap)map).setValue(modulename,value.toString());
                }
            }
            return map;
        } else return null;
    }

    @Override
    public Class getResultType(String variablename) {
        if (!hasResult(variablename)) {
            return null;
        } else if (variablename.equalsIgnoreCase("size") ) {
            return NumericVariable.class;
        } else if (variablename.startsWith("random") ) {
            return ModuleCollection.class;            
        } else if (variablename.startsWith("module:") ) {
            String propertyName=variablename.substring("module:".length());
            if (propertyName.equals("<property>")) return ModuleTextMap.class; // just in case
            Class propclass=ModuleCRM.getPropertyClass(propertyName, MotifLabEngine.getEngine());
            if (propclass==null) return ModuleTextMap.class; // just in case
            else if (Number.class.isAssignableFrom(propclass)) return ModuleNumericMap.class;
            else return ModuleTextMap.class;              
        } else return null;
    }

    @Override
    public boolean hasResult(String variablename) {
        if ( variablename.equalsIgnoreCase("size")) return true;
        else if ( variablename.startsWith("random")) return true;        
        else if ( variablename.startsWith("module:")) return true;        
        return false;
    }      

    @Override
    public Class getMembersClass() {
        return ModuleCRM.class;
    }    
    
    @Override
    public String getValueAsParameterString() {       
        if (hasConstructorString()) return getFullConstructorString();
        else { // list names of all modules included in the collection
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
     * Returns the names of all the Modules in this dataset
     *
     * @return A list of ModuleCRM names
     */
    public ArrayList<String> getAllModuleNames() {
        return storage;
    }

    /**
     * Returns all the ModuleCRM objects in this dataset
 (if they are currently registered with the engine)
     *
     * @return A list of ModuleCRM objects
     */
    public ArrayList<ModuleCRM> getAllModules(MotifLabEngine engine) {
        ArrayList<ModuleCRM> list = new ArrayList<ModuleCRM>(storage.size());
        for (String name:storage) {
            Data item=engine.getDataItem(name);
            if (item!=null && item instanceof ModuleCRM) list.add((ModuleCRM)item);
        }
        return list;
    }

    /**
     * Returns the ModuleCRM corresponding to the given name.
     *
     * @param name The name of the ModuleCRM
     * @return the specified ModuleCRM (if found) or null
     */
    public ModuleCRM getModuleByName(String name, MotifLabEngine engine) {
        for (String modulename : storage) {
            if (modulename.equals(name)) {
                Data item=engine.getDataItem(name);
                if (item!=null && item instanceof ModuleCRM) return (ModuleCRM)item;
            }
        }
        return null;
    }

    /**
     * Returns the name of the Module corresponding to the given index.
     * This method could be used if you want to iterate through all Modules
     * in a dataset.
     *
     * @param index The index of the ModuleCRM
     * @return the name of the ModuleCRM at the specified index (if exists) or null
     */
    public String getModuleNameByIndex(int index) {
        return storage.get(index);
    }
    /**
     * Returns the Module corresponding to the given index.
     * This method could be used if you want to iterate through all Modules
     * in a dataset.
     *
     * @param index The index of the ModuleCRM
     * @return the specified ModuleCRM (if exists) or null
     */
    public ModuleCRM getModuleByIndex(int index, MotifLabEngine engine) {
        String name=storage.get(index);
        ModuleCRM cisRegModule=null;
        if (name!=null) {
            Data item=engine.getDataItem(name);
            if (item!=null && item instanceof ModuleCRM) return (ModuleCRM)item;
        }
        return cisRegModule;
    }

    /**
     * Returns the index (order within the dataset) of the ModuleCRM with the given name
 If no ModuleCRM with the given name exists within the dataset the value -1 is returned.
     *
     * @param name The name of the ModuleCRM
     * @return index of the ModuleCRM (between 0 and size-1) or -1
     */
    public int getIndexForModule(String name) {
        for (int i=0;i<storage.size();i++) {
            String moduleName=storage.get(i);
            if (moduleName.equals(name)) return i;
        }
        return -1;
    }

    /**
     * Returns true if the specified ModuleCRM is in this collection
     */
    public boolean contains(ModuleCRM cisRegModule) {
        if (cisRegModule==null) return false;
        String moduleName=cisRegModule.getName();
        return contains(moduleName);
    }

    /**
     * Returns true if a ModuleCRM with the given name is in this collection
     */
    @Override
    public boolean contains(String moduleName) {
        return storage.contains(moduleName);
    }

    /**
     * Returns true if this ModuleCollection objects contains the same Modules
     * as the given collection (the source (e.g. module track or predefined)
     * of these modules are not considered, only if the lists of modules are the same)
     * @param collection
     * @return
     */
    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof ModuleCollection)) return false;
        ModuleCollection collection=(ModuleCollection)other;   
        if (!this.hasSameConstructor(collection)) return false;
        if (size()!=collection.size()) return false;
        for (String name:storage) {
            if (!collection.contains(name)) return false;
        }
        return true;
    }

   /**
     * Adds a new ModuleCRM object to this collection (if it is not already present)
     *
     * @param module The ModuleCRM to be added
     */
    public void addModule(ModuleCRM cisRegModule) {
        if (storage.contains(cisRegModule.getName())) return;
        storage.add(cisRegModule.getName()); // add to local storage
        notifyListenersOfDataAddition(cisRegModule);
    }

   /**
     * Adds the name of a module to this collection (if it is not already present)
     * Note that this method is not the preferred way of adding modules.
     * It is requested that one uses addModule(Module module) instead since that method
     *  will also notify any listeners that the collection has been updated
     * @param modulename The name of the module to be added
     */
    public void addModuleName(String name) {
        if (storage.contains(name)) return;
        storage.add(name); //
    }

   /**
     * Adds the names of modules to this collection (if they are not already present)
     * Note that this method is not the preferred way of adding modules.
     * It is requested that one uses addModule(ModuleCR module) instead since that method
     * will also notify any listeners that the collection has been updated
     * @param names The names of the modules to be added
     */
    public void addModuleNames(ArrayList<String> names) {
        for (String name:names) {
          if (!storage.contains(name)) storage.add(name);
        }
    }
   /**
     * Removes a ModuleCRM object from this collection (if present)
     *
     * @param module The ModuleCRM to be removed
     */
    public void removeModule(ModuleCRM cisRegModule) {
        boolean hasremoved=storage.remove(cisRegModule.getName()); // remove from local storage
        if (hasremoved) notifyListenersOfDataRemoval(cisRegModule);
    }


    @Override
    public void clearAll(MotifLabEngine engine) {
        String[] list=new String[storage.size()];
        for (int i=0;i<list.length;i++) {list[i]=storage.get(i);}
        for (String name:list) {
            storage.remove(name);
            Data item=null;
            if (engine!=null) item=engine.getDataItem(name);
            if (item!=null && item instanceof ModuleCRM) notifyListenersOfDataRemoval((ModuleCRM)item);
        }
        this.fromList=null;
        this.fromInteractions=null;
        this.fromTrack=null;
        this.fromMap=null;
        this.clearConstructorString();
    }

    /**
     * Returns the number of Modules in this collection
     *
     * @return number of modules
     */
    public int getNumberofModules() {
        return storage.size();
    }

    /**
     * Returns the number of modules in this collection (same as getNumberofModules)
     *
     * @return number of modules
     */
    @Override
    public int size() {
        return storage.size();
    }

   /**
     * Returns TRUE if this ModuleCollection contains no modules
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
            if (data==null) engine.logMessage("No such module: "+line); // should this throw ParseError?
            else if (!(data instanceof ModuleCRM)) engine.logMessage("'"+line+"' is not a Module object"); // should this throw ParseError?
            else addModule((ModuleCRM)data);
        }
    }      

    /**
     * This method takes all modules in the specified dataset and adds them
     * to this dataset
     * @param dataset The dataset to be incorporated into this dataset
     */
    public void merge(ModuleCollection dataset) {
        int size=dataset.size();
        for (int i=0;i<size;i++) {
            String modulename=dataset.getModuleNameByIndex(i);
            if (!contains(modulename)) storage.add(modulename); // I use this directly instead of calling addModule() to limit the number of notifications sent
        }
        notifyListenersOfDataAddition(dataset);
    }

    /**
     * This method removes all modules from this collection that are also present
     * in the argument dataset
     * @param dataset A collection of modules to remove from this collection
     */
    public void subtract(ModuleCollection dataset) {
        int size=dataset.size();
        for (int i=0;i<size;i++) {
            String modulename=dataset.getModuleNameByIndex(i);
            if (contains(modulename)) storage.remove(modulename); // I use this directly instead of calling removeModule() to limit the number of notifications sent
        }
        notifyListenersOfDataRemoval(dataset);
    }

    /**
     * This method compares the current contents of this collection with the argument collection
     * and removes any modules from this collection that are not present in both
     * @param dataset A collection of modules to keep in this collection (if the already exists).
     * Remove all the rest
     */
    public void intersect(ModuleCollection dataset) {
        ModuleCollection removed=new ModuleCollection("removed");
        int size=storage.size();
        for (int i=0;i<size;i++) { // first find which modules to remove
            String modulename=storage.get(i);
            if (!dataset.contains(modulename)) {
                removed.storage.add(modulename);
            }
        }
        // then remove them
        size=removed.size();
        for (int i=0;i<size;i++) { // first find which modules to remove
            String modulename=removed.getModuleNameByIndex(i);
            this.storage.remove(modulename);
        }
        notifyListenersOfDataRemoval(removed);
    }

     /**
     * This method returns a new ModuleCollection that contains all modules that
     * are not members of this ModuleCollection
     * @return The complement ModuleCollection
     */
    public ModuleCollection complement(MotifLabEngine engine) {
        ModuleCollection complement=new ModuleCollection("complement");
        ArrayList<Data> allmodules=engine.getAllDataItemsOfType(ModuleCRM.class);
        for (Data cisRegModule:allmodules) { //
            String modulename=cisRegModule.getName();
            if (!this.contains(modulename)) {
                complement.storage.add(modulename);
            }
        }
        return complement;
    }


    @Override
    public void importData(Data source) throws ClassCastException {
        if (source==this) return; // no need to import, the source and target are the same
        ModuleCollection datasource=(ModuleCollection)source;
        this.modulecollectionName=datasource.modulecollectionName;
        storage.clear();
        for (String moduleName:datasource.storage) {
            storage.add(moduleName);
        }
        this.fromList=datasource.fromList;
        this.fromInteractions=datasource.fromInteractions;
        this.fromTrack=datasource.fromTrack;        
        this.fromMap=datasource.fromMap;  
        this.cloneConstructor(datasource);
        this.payload=datasource.payload;
        //notifyListenersOfDataUpdate();
    }

    @Deprecated
    /**
     * This should only be used for testing
     */
    public void importModuleList(String[] list) {
        storage.clear();
        storage.addAll(Arrays.asList(list));
    }


    @Override
    public ModuleCollection clone() {
        ModuleCollection newcollection= new ModuleCollection(modulecollectionName);
        int size=size();
        for (int i=0;i<size;i++) {
            String moduleName=getModuleNameByIndex(i);
            newcollection.storage.add(moduleName); // I use this directly instead of calling addModule() to limit the number of notifications sent
        }
        newcollection.fromList=this.fromList;
        newcollection.fromInteractions=this.fromInteractions;
        newcollection.fromTrack=this.fromTrack;
        newcollection.fromMap=this.fromMap;     
        newcollection.cloneConstructor(this);
        return newcollection;
    }

    public void setPredefinedCollectionName(String collectionname) {
        setConstructorString(Operation_new.COLLECTION_PREFIX, collectionname);       
    }
    
    public String getPredefinedCollectionName() {
        return getConstructorString(Operation_new.COLLECTION_PREFIX);
    }    
    /** Returns TRUE if this ModuleCollection returns a system predefined collection  */
    public boolean isPredefined() {
        return hasConstructorString(Operation_new.COLLECTION_PREFIX);
    }
    
    
    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }
    
    @Override
    public String getTypeDescription() {return typedescription+" : "+storage.size();}


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

    /** If this collection is created from known motif interactions, this method will return a string describing the settings used for initialization (Parsable by StandardParametersParser) */
    public String getFromInteractionsConfiguration() {
        return getConstructorString(Operation_new.INTERACTIONS_PREFIX);
    }
   /** Sets a configuration String (parsable by StandardParametersParser) used for creating this collection from known motif interactions */
    public void setFromInteractionsConfiguration(String settings) {
        setConstructorString(Operation_new.INTERACTIONS_PREFIX, settings);       
    }
    /** Returns TRUE if this collection is created from known motif interactions */
    public boolean isFromInteractions() {
        return hasConstructorString(Operation_new.INTERACTIONS_PREFIX);
    }
    
    /** If this collection is based on which modules are present in a module track, this method will return a string describing the settings used for initialization */
    public String getFromTrackString() {
        return getConstructorString(Operation_new.FROM_TRACK_PREFIX);
    }    
    
    public void setFromTrackString(String fromTrackString) {
        setConstructorString(Operation_new.FROM_TRACK_PREFIX,fromTrackString);      
    }
    
    /** Returns TRUE if this ModuleCollection is based on which modules are present in a modules track */
    public boolean isFromTrack() {
        return hasConstructorString(Operation_new.FROM_TRACK_PREFIX);
    }    
    
    /** If this collection is based on which Modules satisfy a condition in a ModuleNumericMap, this method will return a string describing the settings used for initialization */
    public String getFromMapString() {
        return getConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    /** Returns TRUE if this ModuleCollection is based on which Modules satisfy a condition in a ModuleNumericMap */
    public boolean isFromMap() {
        return hasConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    
    public void setFromMapString(String fromMapString) {
        setConstructorString(Operation_new.FROM_MAP_PREFIX, fromMapString);       
    }    
    
    /** If this collection is based on a property condition, this method will return a string describing the settings used for initialization */
    public String getFromPropertyString() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }
    
    /** Returns TRUE if this ModuleCollection is based on a property condition */
    public boolean isFromProperty() {
        return hasConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }    
    
    public void setFromPropertyString(String fromPropertyString) {
        setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,fromPropertyString);      
    }    

    /** This method adds a module to temporary storage within this collection.
     * (These temporarily stored modules should be registered with the engine afterwards
     * using the initializeFromPayload() method, however, in recent version this
     * is done automatically in MotifLabEngine.storeDataItem(data) which checks if
     * the data item is a ModuleCollection and calls initializeFromPayload())
     */
    public void addModuleToPayload(ModuleCRM cisRegModule) {
        if (payload==null) payload=new ArrayList<Data>();
        payload.add(cisRegModule);
    }

    /** This method adds a constituent single motif object to temporary storage within this collection.
     * (These temporarily stored objects should be registered with the engine afterwards
     * using the initializeFromPayload() method, however, in recent version this
     * is done automatically in MotifLabEngine.storeDataItem(data) which checks if
     * the data item is a ModuleCollection and calls initializeFromPayload())
     */
    public void addMotifToPayload(Motif motif) {
        if (payload==null) payload=new ArrayList<Data>();
        payload.add(motif);
    }
    
    public void addDataToPayload(Data data) {
        if (payload==null) payload=new ArrayList<Data>();
        payload.add(data);
    }    

    /** This method removes a module from temporary storage within this collection.
     */
    public void removeModuleFromPayload(ModuleCRM cisRegModule) {
        if (payload==null) return;
        else payload.remove(cisRegModule);
    }

    /** This method removes a constituent single motif from temporary storage within this collection.
     */
    public void removeMotifFromPayload(Motif motif) {
        if (payload==null) return;
        else payload.remove(motif);
    }

    /** Returns the ModuleCRM with the given name if it exists in the payload, or else NULL */
    public ModuleCRM getModuleFromPayload(String modulename) {
        if (payload==null) return null;
        for (Data data:payload) {
            if (data instanceof ModuleCRM && data.getName().equals(modulename)) return (ModuleCRM)data;
        }
        return null;
    }

    /** This method removes a module from temporary storage within this collection.
     */
    public void removeObjectFromPayloadAtIndex(int index) {
        if (payload==null || index<0 || index>=payload.size()) return;
        else payload.remove(index);
    }

    /** This method renames a module existing in the temporary storage within this collection.
     *  @param the relative index of the module (Note that while the payload can contain a mix of
     *         both Modules and Motifs, this index applies only to Modules. Thus if index=3 this means
     *         the 4th module in the payload (numbering starts at 0).
     *  @return returns the old name of the module at that index (or NULL if no such module was found)
     */
    public String renameModuleInPayload(int index, String newname) {
        if (payload==null || index<0 || index>=payload.size()) return null;
        int moduleindex=-1;
        for (int i=0;i<payload.size();i++) {
            if (payload.get(i) instanceof ModuleCRM) {
                moduleindex++;
                if (index==moduleindex) {
                    ModuleCRM cisRegModule=(ModuleCRM)payload.get(i);
                    String oldname=cisRegModule.getName();
                    cisRegModule.rename(newname);
                    return oldname;
                }
            }
        }
        return null;
    }

    /** This method renames a motif existing in the temporary storage within this collection.
     *  @param the relative index of the Motif (Note that while the payload can contain a mix of
     *         both Modules and Motifs, this index applies only to Motifs. Thus if index=3 this means
     *         the 4th Motif in the payload (numbering starts at 0).
     *  @return returns the old name of the Motif at that index (or NULL if no such Motif was found)
     */
    public String renameMotifInPayload(int index, String newname) {
        if (payload==null || index<0 || index>=payload.size()) return null;
        int motifindex=-1;
        for (int i=0;i<payload.size();i++) {
            if (payload.get(i) instanceof Motif) {
                motifindex++;
                if (index==motifindex) {
                    Motif motif=(Motif)payload.get(i);
                    String oldname=motif.getName();
                    motif.rename(newname);
                    return oldname;
                }
            }
        }
        return null;
    }

    /** Goes through the Modules in the payload and renames any references to single motifs
     *  based on the given mapping from old names to new names
     */
    public void renameSingleMotifsInPayloadModules(HashMap<String, String> newnames) {
         if (payload==null || payload.isEmpty()) return;
         for (Data data:payload) {
             if (data instanceof ModuleCRM) {
                 for (ModuleMotif mm:((ModuleCRM)data).getModuleMotifs()) {
                     mm.renameSingleMotifReferences(newnames);
                 }
             }
         }
    }

    /** This method returns the List of Modules temporarily stored in the payload of this collection
     */
    public List<Data> getPayload() {
        return payload;
    }

    /** This method returns the number of Modules temporarily stored in the payload of this collection
     */
    public int getNumberofModulesInPayload() {
        if (payload==null || payload.isEmpty()) return 0;
        else {
            int count=0;
            for (Data data:payload) {
                if (data instanceof ModuleCRM) count++;
            }
            return count;
        }
    }

    /** This method returns the number of constituent single Motifs temporarily stored in the payload of this collection
     */
    public int getNumberofMotifsInPayload() {
        if (payload==null || payload.isEmpty()) return 0;
        else {
            int count=0;
            for (Data data:payload) {
                if (data instanceof Motif) count++;
            }
            return count;
        }
    }

    /** This initializes the ModuleCollection by registering all Modules and single Motifs
     *  temporarily stored in the payload.
     *  The collection is then filled with the modules from the payload before the payload is deleted */
    @Override
    public void initializeFromPayload(MotifLabEngine engine) throws ExecutionError {
        //System.err.println("Initializing ModuleCollection from payload. Payload="+(payload!=null));
        if (payload==null) return;
        for (Data moduleormotif:payload) {
            if (!(moduleormotif instanceof ModuleCRM || moduleormotif instanceof Motif)) continue;
            engine.updateDataItem(moduleormotif);
            if (moduleormotif instanceof ModuleCRM) this.addModule((ModuleCRM)moduleormotif);
        }
        payload=null; // clear payload after initialization!
    }

    @Override
    public boolean hasPayload() {
        if (payload==null) return false;
        else return !payload.isEmpty();
    }
 
    
    
/**
 * Creates and returns a new ModuleCollection based on a parameter string
 * @param text The parameter string to be parsed
 * @param targetName The new name of the ModuleCollection (only used if a new collection is created)
 * @param notfound If this parameter is NULL the method will throw an ExecutionError if any entries in the parameter list is not found (the first error will be reported)
 *                 If this parameter is an (empty) ArrayList, the method will be run in 'silent-mode' and not throw exceptions upon encountering errors.
 *                 The ArrayList will be filled with the names that could not be properly resolved (with the reason in parenthesis),
 *                 and the returned collection will only contain those modules that could successfully be resolved.
 * @param engine
 * @return
 * @throws ExecutionError
 * @throws InterruptedException
 */
public static ModuleCollection parseModuleCollectionParameters(String text, String targetName, ArrayList<String> notfound, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
       boolean silentMode=false;
       if (notfound!=null) {silentMode=true;notfound.clear();}
       if (text.startsWith(Operation_new.COLLECTION_PREFIX)) { // predefined collection
            String collectionName=text.substring(Operation_new.COLLECTION_PREFIX.length());
            String filename=engine.getFilenameForModuleCollection(collectionName);
            if (filename==null) throw new ExecutionError("Unknown Motif Collection: "+collectionName);
            BufferedReader inputStream=null;
            ArrayList<String> input=new ArrayList<String>();
            try {
                inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename)))); // these are locally installed files, not repository files
                String line;
                while((line=inputStream.readLine())!=null) {input.add(line);}
            } catch (IOException e) {
                throw new ExecutionError("An error occurred when loading predefined Module Collection: ["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
            } finally {
                try {
                    if (inputStream!=null) inputStream.close();
                } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader Operation_new.parseModuleCollectionParameters(): "+ioe.getMessage());}
            }
            DataFormat format = engine.getDataFormat("MotifLabModule");
            if (format==null) throw new ExecutionError("Unknown Dataformat: MotifLabModule");
            ModuleCollection data=new ModuleCollection(targetName);
            try {data=(ModuleCollection)format.parseInput(input, data, null, task);}
            catch (InterruptedException ie) {throw ie;}
            catch (Exception e) {throw new ExecutionError(e.getMessage());}
            data.setPredefinedCollectionName(collectionName);
            return data;
        } else if (text.startsWith(Operation_new.RANDOM_PREFIX)) { // predefined collection
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
                if (sourceCollection instanceof ModuleCollection) names=((ModuleCollection)sourceCollection).getAllModuleNames();
                else throw new ExecutionError("'"+parts[1]+"' is not a Module Collection");
            } else {
                names=engine.getNamesForAllDataItemsOfType(Motif.class);
            }
            if (percentage) number=number*(double)names.size()/100.0;
            number=Math.round(number);            
            ModuleCollection collection=new ModuleCollection(targetName);
            if (number<names.size()) {
                Collections.shuffle(names);
                for (int i=0;i<number;i++) collection.addModuleName(names.get(i));
            } else {
                collection.addModuleNames(names);
            }
            return collection;            
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
           if (!(numericMap instanceof ModuleNumericMap)) throw new ExecutionError("'"+mapName+"' is not a Module Numeric Map");
           ModuleCollection data=new ModuleCollection(targetName);
           data.createCollectionFromMap((ModuleNumericMap)numericMap, operator, firstOperand, secondOperand, engine);
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
           ModuleCollection collection=new ModuleCollection(targetName);           
           collection.createCollectionFromProperty(propertyName, operator, operands, engine);
           return collection;
        } else if (text.startsWith(Operation_new.FROM_TRACK_PREFIX)) {
           ModuleCollection newDataItem=new ModuleCollection(targetName); // empty collection
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
        } else if (text.startsWith(Operation_new.INTERACTIONS_PREFIX)) {
            String configstring=text.substring(Operation_new.INTERACTIONS_PREFIX.length()).trim();
            StandardParametersParser parser=new StandardParametersParser(engine);
            ParameterSettings parametersettings=null;
            try {
                parametersettings=parser.parse(configstring, getCreateFromInteractionsParameters());
            } catch (ParseError e) {throw new ExecutionError(e.getMessage(), e);}
            ModuleCollection collection=createModuleCollectionFromInteractions(targetName,parametersettings,engine, task);
            return collection;
       } else {
           boolean keepConfig=false; // set this flag to true if the original config-string should be retained
           if (text.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               text=text.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
           }
           ModuleCollection newDataItem=new ModuleCollection(targetName); // empty collection
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
               if (entry.contains("->")) { // entry refers to a cluster within a ModuleCRM Partition
                   String[] elements=entry.split("->");
                   if (elements.length!=2) {
                       if (silentMode) {notfound.add(entry+" : syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                   }
                   String partition=elements[0];
                   String cluster=elements[1];
                   dataobject=engine.getDataItem(partition);
                        if (dataobject==null) {if (silentMode) {notfound.add(partition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+partition);}
                   else if (!(dataobject instanceof ModulePartition)) {if (silentMode) {notfound.add(partition+" : Not a Module Partition"); continue;} else throw new ExecutionError("Data item '"+partition+"' is not a Module Partition");}
                   else if (!((ModulePartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Module Partition '"+partition+"' does not contain a cluster with the name '"+cluster+"'");}
                   else dataobject=((ModulePartition)dataobject).getClusterAsModuleCollection(cluster, engine);
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
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpressionInNumericRange(range1[0], range1[2], start, end, ModuleCRM.class);
                   ModuleCollection tempCollection=new ModuleCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addModule((ModuleCRM)object);
                   }
                   combineWithCollection(newDataItem,tempCollection,mode,engine);
                   continue; 
               } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                   if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, ModuleCRM.class);
                   ModuleCollection tempCollection=new ModuleCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addModule((ModuleCRM)object);
                   }
                   combineWithCollection(newDataItem,tempCollection,mode,engine);
                   continue; 
               } else { // entry refers to one module or a module collection
                   dataobject=engine.getDataItem(entry);
               }
               
                    if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
               else if (dataobject instanceof ModulePartition) {if (silentMode) notfound.add(entry+" : Missing cluster for Module Partition"); else throw new ExecutionError("Missing specification of cluster for Module Partition '"+entry+"'. (use format: Partition.Cluster)");}
               else if (!(dataobject instanceof ModuleCRM || dataobject instanceof ModuleCollection)) {if (silentMode) notfound.add(entry+" : Not a Module or Module Collection"); else throw new ExecutionError("Data item '"+entry+"' is not a Module or Module Collection");}
               else combineWithCollection(newDataItem,dataobject,mode,engine);
           }
           if (keepConfig) newDataItem.setFromListString(text); // Store original config-string in data object
           return newDataItem;
         }

    }

    private static void combineWithCollection(ModuleCollection col, Data dataobject, int mode, MotifLabEngine engine) {
            if (mode==Operation_new.SUBTRACTION) subtractFromModuleCollection(col,dataobject,engine);
       else if (mode==Operation_new.INTERSECTION) intersectWithModuleCollection(col,dataobject,engine);
       else if (mode==Operation_new.COMPLEMENT) addComplementToModuleCollection(col,dataobject,engine);
       else addToModuleCollection(col,dataobject,engine);
    }

 /** Adds a single ModuleCRM or ModuleCRM collection (other) to a target collection */
    private static void addToModuleCollection(ModuleCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof ModuleCRM) {
            target.addModule((ModuleCRM)other);
        } else if (other instanceof ModuleCollection) {
            for (ModuleCRM cisRegModule:((ModuleCollection)other).getAllModules(engine)) {
                target.addModule(cisRegModule);
            }
        } else {
            System.err.println("SYSTEM ERROR: In ModuleCollection.addToModuleCollection. Parameter is neither Module nor Module collection but rather: "+other.getClass().getSimpleName());
        }
    }

    /** Removes all modules from the target collection which is not present in the other ModuleCRM collection (or single module) */
    private static void intersectWithModuleCollection(ModuleCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof ModuleCRM) {
            if (target.contains((ModuleCRM)other)) {
                target.clearAll(engine);
                target.addModule((ModuleCRM)other);
            } else target.clearAll(engine);
        } else if (other instanceof ModuleCollection) {
            for (ModuleCRM cisRegModule:target.getAllModules(engine)) {
                if (!((ModuleCollection)other).contains(cisRegModule)) target.removeModule(cisRegModule);
            }
        } else {
            System.err.println("SYSTEM ERROR: In ModuleCollection.intersectWithModuleCollection. Parameter is neither Module nor Module collection but rather: "+other.getClass().getSimpleName());
        }
    }
    /** Subtracts a single ModuleCRM or ModuleCRM collection (other) from a target collection */
    private static void subtractFromModuleCollection(ModuleCollection target, Object other, MotifLabEngine engine) {
         if (other==null) return;
        if (other instanceof ModuleCRM) {
            target.removeModule((ModuleCRM)other);
        } else if (other instanceof ModuleCollection) {
            for (ModuleCRM cisRegModule:((ModuleCollection)other).getAllModules(engine)) {
                target.removeModule(cisRegModule);
            }
        } else {
            System.err.println("SYSTEM ERROR: In ModuleCollection.subtractFromModuleCollection. Parameter is neither Module nor Module collection but rather: "+other.getClass().getSimpleName());
        }
    }

 /** Adds the complement of a single ModuleCRM or ModuleCRM collection (other) to a target collection */
    private static void addComplementToModuleCollection(ModuleCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof ModuleCRM) {
            for (Data cisRegModule:engine.getAllDataItemsOfType(ModuleCRM.class)) {
                if (cisRegModule!=other) target.addModule((ModuleCRM)cisRegModule);
            }
        } else if (other instanceof ModuleCollection) {
            for (Data cisRegModule:engine.getAllDataItemsOfType(ModuleCRM.class)) {
                if (!((ModuleCollection)other).contains((ModuleCRM)cisRegModule)) target.addModule((ModuleCRM)cisRegModule);
            }
        } else {
            System.err.println("SYSTEM ERROR: In ModuleCollection.addComplementToModuleCollection. Parameter is neither Module nor Module collection but rather: "+other.getClass().getSimpleName());
        }
    }

    /** Creates a new ModuleCollection with new Modules based on the known interactions among motifs 
     * @param targetName A name for this ModuleCollection
     * @return 
     */
    public static ModuleCollection createModuleCollectionFromInteractions(String targetName, ParameterSettings settings, MotifLabEngine engine, OperationTask task) throws ExecutionError {
        MotifCollection motifs=null;
        MotifPartition partition=null;
        int limit=0;
        boolean justTesting=false;
        String configuration=null;
        String widthLimitString=null;
        int widthLimit=0;
        String cardinalityLimitString=null;
        int cardinalityLimit=0;
        boolean includeSelfInteractions=true;
        if (task!=null) {
            task.setProgress(1);
        }
        
        Parameter[] parameters=getCreateFromInteractionsParameters();
        if (settings!=null) {
          try{
             configuration=(String)settings.getResolvedParameter("Configurations",parameters,engine);
             motifs=(MotifCollection)settings.getResolvedParameter("Motifs",parameters,engine);
             partition=(MotifPartition)settings.getResolvedParameter("Groups",parameters,engine);
             widthLimitString=(String)settings.getResolvedParameter("Width limit",parameters,engine);
             widthLimit=(Integer)settings.getResolvedParameter("Width",parameters,engine);
             cardinalityLimitString=(String)settings.getResolvedParameter("Cardinality limit",parameters,engine);
             cardinalityLimit=(Integer)settings.getResolvedParameter("Cardinality", parameters, engine);
             limit=(Integer)settings.getResolvedParameter("Collection limit",parameters,engine);
             includeSelfInteractions=(Boolean)settings.getResolvedParameter("Include self-interactions",parameters,engine);
             justTesting=(Boolean)settings.getResolvedParameter("Testing",parameters,engine);
          }
          catch (ExecutionError e) {throw new ExecutionError("An error occurred while resolving parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ExecutionError("An error occurred while resolving parameters: "+ex.getMessage());}
        } else {
             configuration=(String)getDefaultValueForParameter("Configurations",parameters);
             motifs=(MotifCollection)getDefaultValueForParameter("Motifs",parameters);            
             partition=(MotifPartition)getDefaultValueForParameter("Groups",parameters);
             widthLimitString=(String)getDefaultValueForParameter("Width limit",parameters);
             widthLimit=(Integer)getDefaultValueForParameter("Width",parameters);
             cardinalityLimitString=(String)getDefaultValueForParameter("Cardinality limit",parameters);
             cardinalityLimit=(Integer)getDefaultValueForParameter("Cardinality",parameters);
             limit=(Integer)getDefaultValueForParameter("Collection limit",parameters);
             includeSelfInteractions=(Boolean)getDefaultValueForParameter("Include self-interactions",parameters);
             justTesting=(Boolean)getDefaultValueForParameter("Testing",parameters);
        }
        if (configuration==null) throw new ExecutionError("Missing value for required parameter 'configurations'");
        if (widthLimitString.equalsIgnoreCase("No limit")) widthLimit=0;
        boolean widthPerMotif=(widthLimitString.equalsIgnoreCase("Width per motif"));
        ModuleCollection collection=new ModuleCollection(targetName);
        StandardParametersParser parser=new StandardParametersParser();
        String commandString=parser.getCommandString(parameters, settings);
        collection.setFromInteractionsConfiguration(commandString);
        // first I will create a partition werein all eligible motifs have been clustered together with their known alternatives
        //MotifPartition partition=MotifPartition.createPartitionBasedOnAlternatives("clusters", motifs, engine);
        if (partition==null) partition=MotifPartition.createPartitionWithIndividualClusters("clusters", motifs, engine);
        if (partition.getNumberOfClusters()==0) { // no interacting clusters to construct modules from            
            if (justTesting) collection.addDataToPayload(new NumericVariable("modules",0));
            return collection;
        }
        HashMap<String,HashSet<String>> clusters=partition.getClusters(); 
        HashSet<String> motifnamesfilter=new HashSet<String>(partition.getAllAssignedMembers()); 
        ArrayList<String> clusterNames=new ArrayList<String>(clusters.size()); // this is just to get an ordered and indexable list of the clusters 
        clusterNames.addAll(clusters.keySet()); 
        HashMap<String,HashSet<String>> interactionClusters=new HashMap<String, HashSet<String>>(); // for each cluster (key) the value is a set containing the names of all motifs that interact with the first cluster
        for (String clusterName:clusterNames) {
           interactionClusters.put(clusterName, getInteractionsForCluster(clusters.get(clusterName),motifnamesfilter,engine)); 
        }
        HashSet<String> interactions=new HashSet<String>(); // this will store interactions as strings of the format "X_Y" where X and Y are cluster indices (integers) and X<=Y
        int numclusters=partition.getNumberOfClusters();
        int skip=(includeSelfInteractions)?0:1;
        for (int i=0;i<numclusters-1;i++) {
           for (int j=i+skip;j<numclusters;j++) { // go from j=i to include self-interactions or j=i+1 to exclude self-interactions
               boolean hasInteraction=false;
               String clustername1=clusterNames.get(i);
               String clustername2=clusterNames.get(j);              
               HashSet<String> cluster1=clusters.get(clustername1);
               HashSet<String> cluster2_interactions=interactionClusters.get(clustername2);
               if (clustersOverlap(cluster1,cluster2_interactions)) hasInteraction=true;
               else { // try the other way around just in case (this will be redundant if all interaction links are bidirectional, but that might not be the case
                  HashSet<String> cluster2=clusters.get(clustername2);
                  HashSet<String> cluster1_interactions=interactionClusters.get(clustername1);
                  hasInteraction=clustersOverlap(cluster1_interactions,cluster2);                  
               }
               if (hasInteraction) {
                   interactions.add(i+"_"+j);
               }
           }           
        }
        interactionClusters=null; // just to free up some memory
        Color[] assignedColors=new Color[clusterNames.size()]; // the 'key' is the cluster index
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        if (configuration.equalsIgnoreCase("pairwise")) {
            if (justTesting) collection.addDataToPayload(new NumericVariable("modules",interactions.size()));
            else {
                int total=(limit>0 && limit<interactions.size())?limit:interactions.size();
                int counter=0;
                String[] modulenames=engine.getNextAvailableDataNames("MOD", 4, total);
                for (String interaction:interactions) {
                   int[] pair=getInteractionPair(interaction);
                   String name1=clusterNames.get(pair[0]);
                   String name2=clusterNames.get(pair[1]);
//                   if (name1.compareTo(name2)>0) {int pairswap=pair[0]; pair[0]=pair[1];pair[1]=pairswap;String nameswap=name1; name1=name2; name2=nameswap;} // swap the clusters to sort alphabetically
                   ArrayList<HashSet<String>> mmcluster=new ArrayList<HashSet<String>>(2);
                   mmcluster.add(clusters.get(name1));
                   mmcluster.add(clusters.get(name2));
                   if (name2.equals(name1)) name2=name2+"_2"; // now rename second motif if both are the same
                   String[] mmnames=new String[]{name1,name2};
                   if (assignedColors[pair[0]]==null) assignedColors[pair[0]]=vizSettings.getFeatureColor(modulenames[counter]+"."+name1);
                   else vizSettings.setFeatureColor(modulenames[counter]+"."+name1, assignedColors[pair[0]], false);
                   if (assignedColors[pair[1]]==null) assignedColors[pair[1]]=vizSettings.getFeatureColor(modulenames[counter]+"."+name2);
                   else vizSettings.setFeatureColor(modulenames[counter]+"."+name2, assignedColors[pair[1]], false);
                   int factor=(widthPerMotif)?mmnames.length:1;
                   collection.addDataToPayload(createModuleFromClusters(modulenames[counter], mmnames, mmcluster, widthLimit*factor));
                   counter++;
                   if (counter>=total) break;
                }
            }            
        } else if (configuration.equalsIgnoreCase("maximum cliques") || configuration.equalsIgnoreCase("maximal cliques")) {
            boolean maximum=configuration.equalsIgnoreCase("maximum cliques");
            ClusterGraph clustergraph=new ClusterGraph(interactions,true); // 
            BronKerboschCliqueFinder cliquefinder=new BronKerboschCliqueFinder(clustergraph);  
            Collection<Set<Integer>> results=(maximum)?cliquefinder.getBiggestMaximalCliques():cliquefinder.getAllMaximalCliques();
            // process the results wrt self-interactions (clusters of size 1)
            if (!includeSelfInteractions) { // remove clusters of size 1
                Iterator iterator=results.iterator();
                while (iterator.hasNext()) {
                    Set<Integer> entry=(Set<Integer>)iterator.next();
                    if (entry.size()==1) iterator.remove();
                }                
            }
            // now filter the results based on cardinality constraints            
            if (cardinalityLimitString.equalsIgnoreCase("exactly")) {
                Iterator iterator=results.iterator();
                while (iterator.hasNext()) {
                    Set<Integer> entry=(Set<Integer>)iterator.next();
                    int entrysize=entry.size();
                    if (entrysize==1) entrysize=2; // Homogeneous pair. These will be expanded on later 
                    if (entrysize!=cardinalityLimit) iterator.remove();
                }
            } else if (cardinalityLimitString.equalsIgnoreCase("at most")) {
                Iterator iterator=results.iterator();
                while (iterator.hasNext()) {
                    Set<Integer> entry=(Set<Integer>)iterator.next();
                    int entrysize=entry.size();
                    if (entrysize==1) entrysize=2; // Homogeneous pair. These will be expanded on later 
                    if (entrysize>cardinalityLimit) iterator.remove();
                }               
            } else if (cardinalityLimitString.equalsIgnoreCase("at least")) {
                Iterator iterator=results.iterator();
                while (iterator.hasNext()) {
                    Set<Integer> entry=(Set<Integer>)iterator.next();
                    int entrysize=entry.size();
                    if (entrysize==1) entrysize=2; // Homogeneous pair. These will be expanded on later 
                    if (entrysize<cardinalityLimit) iterator.remove();
                }                
            }
            if (justTesting) collection.addDataToPayload(new NumericVariable("modules",results.size()));
            else {
                int total=(limit>0 && limit<interactions.size())?limit:interactions.size();
                int counter=0;
                String[] modulenames=engine.getNextAvailableDataNames("MOD", 4, total);
                for (Set<Integer> moduleset:results) {
                   int cardinality=moduleset.size();
                   if (cardinality==1) cardinality=2; // homogeneous pair
                   String[] mmnames=new String[cardinality];
                   ArrayList<HashSet<String>> mmcluster=new ArrayList<HashSet<String>>(cardinality);
                   int mmCounter=0;
                   int lastClusterIndex=-1;
                   for (Integer clusterIndex:moduleset) {
                       String name=clusterNames.get(clusterIndex);
                       mmcluster.add(clusters.get(name));                       
                       mmnames[mmCounter]=getFirstUniqueName(name,mmnames); // this should be unique in the module by now
                       if (assignedColors[clusterIndex]==null) assignedColors[clusterIndex]=vizSettings.getFeatureColor(modulenames[counter]+"."+name);
                       else vizSettings.setFeatureColor(modulenames[counter]+"."+name, assignedColors[clusterIndex], false);
                       mmCounter++;
                       lastClusterIndex=clusterIndex;                                              
                   }
                   if (moduleset.size()==1) { // add the same cluster a second time for homogeneous pairs
                       String name=clusterNames.get(lastClusterIndex);
                       mmcluster.add(clusters.get(name)); 
                       name=name+"_2";
                       mmnames[1]=name;
                       vizSettings.setFeatureColor(modulenames[counter]+"."+name, assignedColors[lastClusterIndex], false);
                   }
                   int factor=(widthPerMotif)?mmnames.length:1;
                   collection.addDataToPayload(createModuleFromClusters(modulenames[counter], mmnames, mmcluster, widthLimit*factor));
                   counter++;
                   if (counter>=total) break;
                }
            }                
        } else throw new ExecutionError("Unrecognized configuration: "+configuration);
        if (task!=null) {
            task.setProgress(99);
        }
        return collection;
    }
    
    private static String getFirstUniqueName(String name, String[] taken) {
        for (int i=0;i<taken.length;i++) {
            if (taken[i]==null) return name; 
            else if (taken[i].equals(name)) name=name+"_2"; //
        }
        return name;
    }    
    /** breaks up an X_Y string into [X,Y] */
    private static int[] getInteractionPair(String interactionstring) {
        String first=interactionstring.substring(0,interactionstring.indexOf('_'));
        String second=interactionstring.substring(interactionstring.indexOf('_')+1);
        try {
            int firstCluster=Integer.parseInt(first);
            int secondCluster=Integer.parseInt(second);
            return new int[]{firstCluster,secondCluster};
        } catch (NumberFormatException e) {return null;}
    }
    
    private static ModuleCRM createModuleFromClusters(String modulename, String[] motifClusterNames, ArrayList<HashSet<String>> clusters, int width) {
        ModuleCRM cisRegModule=new ModuleCRM(modulename);
        for (int i=0;i<motifClusterNames.length;i++) {
            String motifClusterName=motifClusterNames[i]; // all these names should be unique!!
            HashSet<String> motifs=clusters.get(i);
            cisRegModule.addModuleMotif(motifClusterName, motifs, ModuleCRM.INDETERMINED);
        }
        if (width>0) cisRegModule.setMaxLength(width);
        return cisRegModule;
    }
    
    /** This method returns a set containing the names of all motifs that are known to interact with
     *  any of the motifs mentioned by name in the motifnames set. 
     *  Also, if the motifnamesfilter parameter is non-null, the name must also be present in this
     *  set in order to be included in the result
     */
    private static HashSet<String> getInteractionsForCluster(HashSet<String> motifnames, HashSet<String> motifnamesfilter, MotifLabEngine engine) {
        HashSet<String> interacting=new HashSet<String>();
        for (String motifname:motifnames) {
            Data dataitem=engine.getDataItem(motifname);
            if (dataitem instanceof Motif) {
                interacting.addAll(((Motif)dataitem).getInteractionPartnerNames());
            }
        }
        if (motifnamesfilter!=null) filterMotifNames(interacting,motifnamesfilter);
        return interacting;
    }
    
    /** Removes all Strings from the target Set which is not in the filter
     *  The method alters the target parameter
     */
    private static void filterMotifNames(HashSet<String> target, HashSet<String> filter) {
        Iterator<String> iterator=target.iterator();
        while (iterator.hasNext()) {
            String value=iterator.next();
            if (!filter.contains(value)) iterator.remove();
        }
    }
    
    /** This method returns true if any of the strings in cluster1 is also in cluster2 */
    private static boolean clustersOverlap(HashSet<String> cluster1,HashSet<String> cluster2) {
        for (String string:cluster1) {
            if (cluster2.contains(string)) return true;
        }
        return false;
    }   

    private static Object getDefaultValueForParameter(String parameterName, Parameter[] parameters) {
        for (Parameter par:parameters) {
            if (parameterName.equals(par.getName())) return par.getDefaultValue();
        }
        return null;
    }

    public static Parameter[] getCreateFromInteractionsParameters() {
        Parameter[] parameters=new Parameter[]{
           new Parameter("Configurations", String.class, "Pairwise", new String[]{"Pairwise","Maximum cliques","Maximal cliques"}, "<html>The 'configurations' parameter specifies what type of modules to return from the motif interactions graph.<br><ul><li>'Pairwise' modules will consist of pairs of motifs that are known to interact.</li><li>'Maximal cliques' are groups of motifs where each motif is known to interact with every other motif in the group<br>and no new motif can be added to the group without violating the clique property.</li><li>'Maximum cliques' are the largest clique modules that can be found in the motif interactions graph.</li></ul></html>", true, false),
           new Parameter("Motifs", MotifCollection.class, null, new Class[]{MotifCollection.class}, "If specified, only motifs in this collection will be considered", false, false),
           new Parameter("Groups", MotifPartition.class, null, new Class[]{MotifPartition.class}, "If specified, the motifs will be grouped together into equivalence clusters based on this partition", false, false),
           new Parameter("Width limit", String.class, "Total width", new String[]{"No limit","Total width","Width per motif"}, "<html><ul><li>'No limit' means that no limits are imposed on the widths of the modules.</li><li>'Total width' means that the total width of each module will be limited to the value of the 'width' parameter below.</li><li>'Width per motif' means that the total width of each module will be limited to the value of the 'width' parameter below times the number of motifs in the module</li></ul></html>", true, false),
           new Parameter("Width", Integer.class, 200, new Integer[]{0,10000}, "A width constraint value (number of base pairs). This is required unless the 'Width limit' parameter above is set to 'No limit'", false, false),
           new Parameter("Cardinality limit", String.class, "No limit", new String[]{"No limit","At most","At least","Exactly"}, "<html>This parameter only applies to <i>maximum</i> and <i>maximal clique</i> configurations.<br><ul><li>'No limit' means that no limits are imposed on the number of motifs in the modules.</li><li>'At most' means that each module should contain no more motifs than the specified cardinality.</li><li>'At least' means that each module should have no fewer motifs than the specified cardinality.</li><li>'Exactly' means that each module should have exactly the number of motifs specified by the cardinality.</li></ul></html>", true, false),
           new Parameter("Cardinality", Integer.class, 3, new Integer[]{0,100}, "<html>This parameter only applies to <i>maximum</i> and <i>maximal clique</i> configurations.<br>If the 'cardinality limit' above is set to anything other than 'No limit',<br>this parameter specifies an upper and/or lower bound on the cardinality of the module (the number of motifs)</html>", false, false),
           new Parameter("Include self-interactions", Boolean.class, Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE}, "Allow inclusion of the same motif twice if it can interact with itself", false, false),
           new Parameter("Collection limit", Integer.class, 0, new Integer[]{0,10000}, "Specifying a limit greater than 0 will limit the number of returned modules to this number. A value of 0 is interpreted as 'no limit'", false, false),
           new Parameter("Testing", Boolean.class, Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE}, null, false, true),
        };
        return parameters;
    }
    
    
/**
 * Parses a configuration string for Module Collection creation from a Module Track 
 * and returns an String[] containing parsed elements:
 * [0] Name of Module Track 
 * [1] Operator
 * [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [4] "TRUE" or "FALSE". TRUE if numbers should be treated as percentages or FALSE if they are absolute numbers
 * [5] Name of sequence collection (or NULL if no collection is specified)
 * @param configString. Format example: "CRM1, support >= 20" or "CRM1, support in [0,10]" or "CRM1, support < 10, collection=Downregulated"
 * @return
 */
   public static String[] parseTrackConfigurationString(String configstring, MotifLabEngine engine) throws ParseError {
      Pattern pattern=Pattern.compile("support\\s*(>=|<=|<>|<|=|>| in )\\s*(\\S.*)");      
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
      if (trackName.isEmpty()) throw new ParseError("Missing name of module track");      
      if (operator==null || firstOperand==null) throw new ParseError("Missing support specification");      
      return new String[]{trackName,operator,firstOperand,secondOperand,percentage,seqCollectionName};
   }
    
     /**
     * Initialize this collection based on which modules (known to the engine)
     * are found in in a selected number of sequences in the module track (region dataset)
     * provided as argument
     *
     * @param dataset The module track (Region dataset) used as basis
     * @param quorum The quorum states the number of sequences the module should be present in (comparison operand). This is a string which can be a literal number or the name of a data object
     * @param quorum2 The quorum2 states the upper limit on the number of sequences the module should be present in (when operator 'in' is used). This is a string which can be a literal number or the name of a data object
     * @param operator Used for comparison (should be <, <=, =, >, >=,<> or in)
     * @param percentage If this flag is TRUE the quorum number should be between 0 and 100 and will be interpreted as a percentage of the sequences
     *                   If the flag is FALSE the quorum number will be interpreted as the specific number of sequences
     * @param seqCollection Only consider sequences from this collection. Any percentage numbers are relative to the size of this collection (can be NULL)
     */
   public void createCollectionFromTrack(RegionDataset dataset, String quorum, String quorum2, String operator, boolean percentage, SequenceCollection seqCollection, MotifLabEngine engine, OperationTask task) throws ExecutionError,InterruptedException {
       if (seqCollection==null) seqCollection=engine.getDefaultSequenceCollection();
       String seqCollectionName=seqCollection.getName();
       Object quorumData=engine.getNumericDataForString(quorum);
       if (quorumData==null) throw new ExecutionError("'"+quorum+"' is not a numeric constant or known numeric data object");
       if ((quorumData instanceof Data) && !(quorumData instanceof NumericConstant || quorumData instanceof NumericVariable || quorumData instanceof ModuleNumericMap)) throw new ExecutionError("'"+quorum+"' is of a type not applicable in this context");
       Object quorumData2=null;
       if (operator.equals("in") || operator.equals("not in")) {
           if (quorum2==null || quorum2.isEmpty()) throw new ExecutionError("Missing upper limit for numeric range");
           quorumData2=engine.getNumericDataForString(quorum2);
           if (quorumData2==null) throw new ExecutionError("'"+quorum2+"' is not a numeric constant or known numeric data object");
           if ((quorumData2 instanceof Data) && !(quorumData2 instanceof NumericConstant || quorumData2 instanceof NumericVariable || quorumData2 instanceof ModuleNumericMap)) throw new ExecutionError("'"+quorum2+"' is of a type not applicable in this context");
       }

       ModuleNumericMap support=new ModuleNumericMap("support",0); // the number of sequences each module appears in
       ArrayList<Data> allModules=engine.getAllDataItemsOfType(ModuleCRM.class);
       storage.clear();
       // configure
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
       for (Data cisRegModule:allModules) {
           support.setValue(cisRegModule.getName(), new Integer(0));
       }
       // count the number of sequences each module occurs in
       ArrayList<FeatureSequenceData> sequences=dataset.getSequencesFromCollection(seqCollection);
       int size=sequences.size();
       int s=0;
       for (FeatureSequenceData sequence:sequences) {
           ArrayList<Region> allRegions=((RegionSequenceData)sequence).getOriginalRegions();
           HashSet<String> present=new HashSet<String>();
           for (Region region:allRegions) { // make a set of present region types (modules)
               present.add(region.getType());
           }
           for (String moduletype:present) { // update module counts by one if present in current sequence
               if (engine.dataExists(moduletype, ModuleCRM.class)) {
                   support.setValue(moduletype, support.getValue(moduletype)+1);
               }
           }
           s++;
           if (task!=null) {
               task.setProgress((int)(s*0.9),size); // scaling by 90% to leave some room for later
               if (Thread.interrupted() || task.isAborted()) throw new InterruptedException();
           }
       }
       for (Data cisRegModule:allModules) {
           String moduleName=cisRegModule.getName();
           double firstOperand=0;
           double secondOperand=0;
                if (quorumData instanceof Integer) firstOperand=((Integer)quorumData).intValue();
           else if (quorumData instanceof Double) firstOperand=((Double)quorumData).doubleValue();
           else if (quorumData instanceof NumericVariable) firstOperand=((NumericVariable)quorumData).getValue();
           else if (quorumData instanceof NumericConstant) firstOperand=((NumericConstant)quorumData).getValue();
           else if (quorumData instanceof ModuleNumericMap) firstOperand=((ModuleNumericMap)quorumData).getValue(moduleName);
                if (quorumData2 instanceof Integer) secondOperand=((Integer)quorumData2).intValue();
           else if (quorumData2 instanceof Double) secondOperand=((Double)quorumData2).doubleValue();
           else if (quorumData2 instanceof NumericVariable) secondOperand=((NumericVariable)quorumData2).getValue();
           else if (quorumData2 instanceof NumericConstant) secondOperand=((NumericConstant)quorumData2).getValue();
           else if (quorumData2 instanceof ModuleNumericMap) secondOperand=((ModuleNumericMap)quorumData2).getValue(moduleName);
           if (percentage) {
               firstOperand=(int)Math.round((double)firstOperand/100.0*seqsize);
               secondOperand=(int)Math.round((double)secondOperand/100.0*seqsize);
           }
           if (moduleSatisfiesMapCondition(moduleName, support, operator, firstOperand, secondOperand)) storage.add(moduleName);
       }
       if (task!=null) {
           task.setProgress(99); // 
       }       
   }    
    
   /** Returns TRUE if the module with the given name satisfies the condition in the map */
   private boolean moduleSatisfiesMapCondition(String moduleName, ModuleNumericMap map, String operator, Object firstOperandData, Object secondOperandData) {
       double firstOperand=0;
       double secondOperand=0;
            if (firstOperandData instanceof Integer) firstOperand=((Integer)firstOperandData).intValue();
       else if (firstOperandData instanceof Double) firstOperand=((Double)firstOperandData).doubleValue();
       else if (firstOperandData instanceof NumericVariable) firstOperand=((NumericVariable)firstOperandData).getValue();
       else if (firstOperandData instanceof NumericConstant) firstOperand=((NumericConstant)firstOperandData).getValue();
       else if (firstOperandData instanceof ModuleNumericMap) firstOperand=((ModuleNumericMap)firstOperandData).getValue(moduleName);
            if (secondOperandData instanceof Integer) secondOperand=((Integer)secondOperandData).intValue();
       else if (secondOperandData instanceof Double) secondOperand=((Double)secondOperandData).doubleValue();
       else if (secondOperandData instanceof NumericVariable) secondOperand=((NumericVariable)secondOperandData).getValue();
       else if (secondOperandData instanceof NumericConstant) secondOperand=((NumericConstant)secondOperandData).getValue();
       else if (secondOperandData instanceof ModuleNumericMap) secondOperand=((ModuleNumericMap)secondOperandData).getValue(moduleName);  
       double moduleValue=map.getValue(moduleName);
            if (operator.equals("="))  return (moduleValue==firstOperand);
       else if (operator.equals(">=")) return (moduleValue>=firstOperand);
       else if (operator.equals(">"))  return (moduleValue>firstOperand);
       else if (operator.equals("<=")) return (moduleValue<=firstOperand);
       else if (operator.equals("<"))  return (moduleValue<firstOperand);
       else if (operator.equals("<>")) return (moduleValue!=firstOperand);
       else if (operator.equals("in")) return (moduleValue>=firstOperand && moduleValue<=secondOperand);
       else if (operator.equals("not in")) return (!(moduleValue>=firstOperand && moduleValue<=secondOperand));
       else return false;
   }   
   
   

  /**
   * Parses a configuration string for ModuleCRM Collection creation from a Numeric map
 and returns an String[] containing parsed elements:
 [0] Name of ModuleNumericMap 
 [1] Operator
 [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
   * @param configString. Format example: "ModuleNumericMap >= 0.4" or "ModuleNumericMap in [0,10]" or "ModuleNumericMap <> NumericVariable1"
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
       } else throw new ParseError("Unable to parse 'Map' parameter for new Module Collection");
       return new String[]{mapName,operator,firstOperand,secondOperand};
   }   
   
     /**
     * Initialize this collection based on which modules satisfy a condition in a ModuleNumericMap
     *
     * @param map The ModuleNumericMap used as basis for the condition
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in )
     * @param firstOperand A string representing a number used for comparisons. This could be a literal numeric constant or the name of a data object
     * @param secondOperand A string representing a second number used as upper limit if operator is "in". This could be a literal numeric constant or the name of a data object
     */
   public void createCollectionFromMap(ModuleNumericMap map, String operator, String firstOperand, String secondOperand, MotifLabEngine engine) throws ExecutionError {
       Object firstOperandData=null;
       Object secondOperandData=null;
       if (firstOperand==null || firstOperand.isEmpty()) throw new ExecutionError("Missing numeric operand for comparison"); 
       firstOperandData=engine.getNumericDataForString(firstOperand);
       if (firstOperandData==null) throw new ExecutionError("'"+firstOperand+"' is not a numeric constant or known numeric data object");
       if ((firstOperandData instanceof Data) && !(firstOperandData instanceof NumericConstant || firstOperandData instanceof NumericVariable || firstOperandData instanceof ModuleNumericMap)) throw new ExecutionError("'"+firstOperand+"' is of a type not applicable in this context");
       if (operator.equals("in") || operator.equals("not in")) {
           if (secondOperand==null) throw new ExecutionError("Missing upper limit for numeric range");
           secondOperandData=engine.getNumericDataForString(secondOperand);
           if (secondOperandData==null) throw new ExecutionError("'"+secondOperand+"' is not a numeric constant or known numeric data object");
           if ((secondOperandData instanceof Data) && !(secondOperandData instanceof NumericConstant || secondOperandData instanceof NumericVariable || secondOperandData instanceof ModuleNumericMap)) throw new ExecutionError("'"+secondOperand+"' is of a type not applicable in this context");
       }     
       storage.clear();
       // configure
       String fromMapString=null;
       if (operator.equals("in") || operator.equals("not in")) {
          fromMapString=map.getName()+" "+operator+" ["+firstOperand+","+secondOperand+"]"; // sets the 'config' string
       } else {
          fromMapString=map.getName()+operator+firstOperand; // sets the 'config' string
       }
       setFromMapString(fromMapString);
       ArrayList<Data> allModules=engine.getAllDataItemsOfType(ModuleCRM.class);
       for (Data cisRegModule:allModules) {
           String moduleName=cisRegModule.getName();
           if (moduleSatisfiesMapCondition(moduleName, map, operator, firstOperandData, secondOperandData)) storage.add(moduleName);
       }
   }   
   
/**
 * Parses a configuration string for ModuleCRM Collection creation from a Property
 and returns an Object[] containing parsed elements:
 [0] Name of Property 
 [1] Operator
 [2] List of operands, as a String[]
 * @param configString. Format example: "Cardinality = 2"
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
       } else throw new ParseError("Unable to parse 'Property' parameter for new Module Collection");
       if (propertyName.startsWith("\"") && propertyName.endsWith("\"")) propertyName=propertyName.substring(1,propertyName.length()-1);
       for (int i=0;i<operands.length;i++) { // remove quotes
           if (operands[i].startsWith("\"") && operands[i].endsWith("\"")) operands[i]=operands[i].substring(1,operands[i].length()-1);
       }
       return new Object[]{propertyName,operator,operands};
   }    
   
     /**
     * Initialize this collection based on which modules that satisfy a condition with respect to a specified property
     *
     * @param property The name of the property 
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in, not in, equals, not equals, matches, not matches )
     * @param operands A list of possible values for the property
      */
   public void createCollectionFromProperty(String property, String operator, String[] operands, MotifLabEngine engine) throws ExecutionError {
       Class typeclass=ModuleCRM.getPropertyClass(property,engine);
       if (typeclass==null) throw new ExecutionError("Unknown module property: "+property);
       if (operands==null || operands.length==0) throw new ExecutionError("Missing property value");       
       storage.clear();
       Object[] resolvedoperands=null;
       if (Number.class.isAssignableFrom(typeclass)) { // either a single operand or two for ranges
           resolvedoperands=new Object[operands.length]; // resolve all operands, irrespective of the operator used
           for (int i=0;i<operands.length;i++) {
               resolvedoperands[i]=engine.getNumericDataForString(operands[i]);     
               if (resolvedoperands[i]==null) throw new ExecutionError("Property value '"+operands[i]+"' is not a numeric constant or applicable numeric data object");           
               if (resolvedoperands[i] instanceof NumericMap && !(resolvedoperands[i] instanceof ModuleNumericMap)) throw new ExecutionError("Property value '"+operands[i]+"' is of a type not applicable in this context");               
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
       } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: class '"+typeclass+"' not recognized by ModuleCollection.createCollectionFromProperty()");
       // configure 
       String spliced=MotifLabEngine.splice(operands, ",");
       StringBuilder builder=new StringBuilder();
       builder.append(property);
       if (operator.matches("[\\w\\s]+")) builder.append(" "+operator+" ");
       else builder.append(operator);
       builder.append(spliced);
       setFromPropertyString(builder.toString());// sets the 'config' string

       ArrayList<Data> allModules=engine.getAllDataItemsOfType(ModuleCRM.class);
       for (Data cisRegModule:allModules) {
           Object value=null;
           String moduleName=cisRegModule.getName();
           try {
               value=((ModuleCRM)cisRegModule).getPropertyValue(property,engine); 
               if (value==null) continue; // do not include modules which do not have a value for the property (no matter which operator is used)
           } catch (Exception e) {continue;}
           boolean satisfies=false;
           if (Boolean.class.equals(typeclass)) satisfies=moduleSatisfiesBooleanPropertyCondition(value,operator,(Boolean)resolvedoperands[0]);
           else if (Number.class.isAssignableFrom(typeclass)) satisfies=moduleSatisfiesNumericPropertyCondition(value,moduleName,operator,resolvedoperands);
           else satisfies=moduleSatisfiesStringPropertyCondition(value,operator,resolvedoperands);
           if (satisfies) storage.add(moduleName);
       }   
   }    
   
   private double resolveNumericValue(Object value, String modulename) throws ExecutionError{
            if (value instanceof Integer) return ((Integer)value).doubleValue();
       else if (value instanceof Double) return ((Double)value).doubleValue();
       else if (value instanceof NumericVariable) return ((NumericVariable)value).getValue();
       else if (value instanceof ModuleNumericMap) return ((ModuleNumericMap)value).getValue(modulename);
       else if (value instanceof Data) throw new ExecutionError("Data object '"+value+"' can not be used in this context");
       else throw new ExecutionError("'"+value+"' is not a numeric constant or known data object");
   }
   
   private boolean moduleSatisfiesBooleanPropertyCondition(Object modulePropertyValue, String operator, boolean targetValue) {
       if (modulePropertyValue instanceof Boolean) {
          if (operator.equals("=")) return ((Boolean)modulePropertyValue)==targetValue;
          else return ((Boolean)modulePropertyValue)!=targetValue;
       } else return false;
   }   
   
   private boolean moduleSatisfiesNumericPropertyCondition(Object modulePropertyValue, String modulename, String operator, Object[] operands) throws ExecutionError {
       double modulevalue=0;
            if (modulePropertyValue instanceof Integer) modulevalue=((Integer)modulePropertyValue).doubleValue();
       else if (modulePropertyValue instanceof Double)  modulevalue=((Double)modulePropertyValue).doubleValue();
       else return false;
       double firstOperandValue=resolveNumericValue(operands[0],modulename);
       double secondOperandValue=(operands.length>1 && operands[1]!=null)?resolveNumericValue(operands[1],modulename):0;
            if (operator.equals("=") && operands.length==1) return modulevalue==firstOperandValue;
       else if (operator.equals("=")) return moduleNumericValueInList(modulevalue,operands,modulename);            
       else if (operator.equals("<")) return modulevalue<firstOperandValue;
       else if (operator.equals("<=")) return modulevalue<=firstOperandValue;
       else if (operator.equals(">")) return modulevalue>firstOperandValue;
       else if (operator.equals(">=")) return modulevalue>=firstOperandValue;
       else if (operator.equals("<>")) return modulevalue!=firstOperandValue;
       else if (operator.equals("in")) return (modulevalue>=firstOperandValue && modulevalue<=secondOperandValue);
       else if (operator.equals("not in")) return !(modulevalue>=firstOperandValue && modulevalue<=secondOperandValue);
       else throw new ExecutionError("Unknown operator for numeric comparison '"+operator+"'");
   } 
   
   private boolean moduleNumericValueInList(double moduleValue, Object[] operands, String modulename) throws ExecutionError {
       for (Object operand:operands) {
           double operandValue=(operand!=null)?resolveNumericValue(operand,modulename):0;
           if (moduleValue==operandValue) return true;
       }
       return false;
   }
   
   @SuppressWarnings("unchecked")
   private boolean moduleSatisfiesStringPropertyCondition(Object modulePropertyValue, String operator, Object[] operands) throws ExecutionError {
       if (modulePropertyValue instanceof String) {
           String value=(String)modulePropertyValue;
           boolean isMatch=(operator.equals("matches") || operator.equals("not matches"))?stringPropertyMatches(value,operands):stringPropertyEquals(value, operands);
           if (operator.startsWith("not")) return !isMatch;
           else return isMatch; // 
       } else if (modulePropertyValue instanceof ArrayList) {
           boolean isMatch=false;
           for (String value:(ArrayList<String>)modulePropertyValue) {
              isMatch=(operator.equals("matches") || operator.equals("not matches"))?stringPropertyMatches(value,operands):stringPropertyEquals(value, operands);
              if (isMatch) break;
           }
           if (operator.startsWith("not")) return !isMatch;
           else return isMatch; //         
       } else return false;
   }    
   
   /** Returns TRUE if the module value is found among the operand values else false */
   private boolean stringPropertyMatches(String modulevalue, Object[] operands) {
        for (Object operand:operands) {
           if (operand instanceof String) {
               String target=( ((String)operand).startsWith("\"") && ((String)operand).endsWith("\"") && ((String)operand).length()>2)?((String)operand).substring(1,((String)operand).length()-1):(String)operand;
               if (modulevalue.matches("(?i)"+target)) return true; // case insensitive match         
           }                
           else if (operand instanceof TextVariable && ((TextVariable)operand).matches(modulevalue)) return true;
        }
        return false;
   }
   /** Returns TRUE if the module value is found among the operand values else false */
   private boolean stringPropertyEquals(String modulevalue, Object[] operands) {
        for (Object operand:operands) {
           if (operand instanceof String) {
               String target=( ((String)operand).startsWith("\"") && ((String)operand).endsWith("\"") && ((String)operand).length()>2)?((String)operand).substring(1,((String)operand).length()-1):(String)operand;
               if (modulevalue.equalsIgnoreCase(target)) return true;         
           } 
           else if (operand instanceof TextVariable && ((TextVariable)operand).contains(modulevalue)) return true;
        }
        return false;
   }   
   
   private Boolean getBooleanValue(String string) {
            if (string.equalsIgnoreCase("TRUE") || string.equalsIgnoreCase("YES")) return Boolean.TRUE;
       else if (string.equalsIgnoreCase("FALSE") || string.equalsIgnoreCase("NO")) return Boolean.FALSE;
       else return null;
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
              if (this.fromMap!=null) this.setConstructorString(Operation_new.FROM_MAP_PREFIX, this.fromMap);
         else if (this.fromInteractions!=null) this.setConstructorString(Operation_new.INTERACTIONS_PREFIX, this.fromInteractions);
         else if (this.fromList!=null) this.setConstructorString(Operation_new.FROM_LIST_PREFIX, this.fromList);
         else if (this.fromTrack!=null) this.setConstructorString(Operation_new.FROM_TRACK_PREFIX, this.fromTrack);

         // clear legacy fields
         this.fromInteractions=null;
         this.fromTrack=null; 
         this.fromMap=null; 
         this.fromList=null;       
    }
    
}

