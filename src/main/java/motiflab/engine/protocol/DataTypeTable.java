/*
 
 
 */

package motiflab.engine.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Sequence;

/**
 * This class represents a look-up table that maps data object names to their data types (class)
 * Such a table should contain all references to all "known" and (hitherto) "unknown" data objects
 * in the system. The table is populated from data items registered in the engine or by parsing protocol
 * scripts (that have not necessarily been executed yet) looking for potential data types. 
 * DataTypeTables can be used by protocol editors for beautification (coloring Data items) or by operation 
 * edit dialogs to correctly populate drop-down menus with Data items of different types.
 * 
 * @author kjetikl
 */
public class DataTypeTable {

    private HashMap<String,Class> map; // contains the name-class pairs
    private HashMap<String,Class> collatedTypeMap; // contains the collated type (Motif,Module,Sequence) for CollatedAnalysis objects
    private MotifLabEngine engine;
    private boolean defaultSequenceCollectionAssigned=false;
    
    public DataTypeTable(MotifLabEngine engine) {
        this.engine=engine;
        map=new HashMap<String, Class>();
        collatedTypeMap=new HashMap<String, Class>();
        defaultSequenceCollectionAssigned=false;
    }
    
    /**
     * Returns the class for the data object with the given name (or null if unknown)
     * @param dataName
     */
    public Class getClassFor(String dataName) {
        if (map.containsKey(dataName)) return map.get(dataName);
        else return null;
    }
    
    /**
     * Returns true if the table contains the given name
     * @param dataName
     */
    public boolean contains(String dataName) {
        return map.containsKey(dataName);
    }
    
    /** Returns TRUE if the default Sequence Collection has been explicitly registered in the protocol 
     *  which would hopefully mean that sequences have been loaded from file
     */
    public boolean isDefaultSequenceCollectionAssigned() {
        return defaultSequenceCollectionAssigned;
    }

    /**
     * Registers the class for the data object with the given name
     * @param dataName The name of the 
     * @param type The class type of the dataobject. If this is NULL the data object will be removed from the table
     * @throws ParseError If the entered name is not a valid data object name according to the rules
     */
    public void register(String dataName, Class type) throws ParseError {
        String nameError=(type==Sequence.class)?engine.checkSequenceNameValidity(dataName, false):engine.checkNameValidity(dataName, false);
        if (nameError!=null) throw new ParseError("\""+dataName+"\" is not a legal name for the data object. "+nameError);
        if (type==null) map.remove(dataName);
        else map.put(dataName, type);
        if (dataName.equals(engine.getDefaultSequenceCollectionName())) defaultSequenceCollectionAssigned=true;
    }

    /**
     * Registers the classes for several data objects
     * @param mappings A Map containing Name-Class pairs for Data objects.
     * Data objects whose class is NULL will be removed from the table
     */
    public void register(HashMap<String,Class> mappings) {
        for (String dataname:mappings.keySet()) {
            Class type=mappings.get(dataname);
            if (type==null) map.remove(dataname);
            else map.put(dataname, type);
            if (dataname.equals(engine.getDefaultSequenceCollectionName())) defaultSequenceCollectionAssigned=true;
        }
    }
    
    /**
     * Registers the collated type class for the analysis object with the given name
     * @param dataName The name of the analysis
     * @param type The collated type class. If this is NULL the entry will be removed from the table
     */
    public void registerCollatedType(String dataName, Class type) {
        if (type==null) collatedTypeMap.remove(dataName);
        else collatedTypeMap.put(dataName, type);
    }   
    
    /**
     * Returns the collated type class for the analysis object with the given name (or null if no type is registered)
     * @param dataName
     */
    public Class getCollatedTypeFor(String dataName) {
        if (collatedTypeMap.containsKey(dataName)) return collatedTypeMap.get(dataName);
        else return null;
    }   
    
    /**
     * Returns a list containing the names of all data items of the given type
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllDataItemsOfType(Class type) {
        ArrayList<String> list=new ArrayList<String>();
        for (String key:map.keySet()) {
            if (type.isAssignableFrom(map.get(key))) list.add(key);
        }
        return list;
    }
    
    /**
     * Returns a list containing the names of all data items of the given types
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllDataItemsOfType(Class[] types) {
        ArrayList<String> list=new ArrayList<String>();
        for (Class type:types) {
          for (String key:map.keySet()) {
            if (type.isAssignableFrom(map.get(key))) list.add(key);
          }
        }
        return list;
    }

    /**
     * Returns TRUE if this DataTypeTable currently contains data items of the given type
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean hasDataItemsOfType(Class type) {
        for (String key:map.keySet()) {
            if (type.isAssignableFrom(map.get(key))) return true;
        }
        return false;
    }

    /**
     * Populates the list with entries from the engine
     */
    public void populateFromEngine() {
        if (engine==null) return;
        ArrayList<Data> list=engine.getAllDataItemsOfType(Data.class);
        for (Data item:list) {
            map.put(item.getName(),item.getClass());
        }
    }

    /**
     * Returns the number of entries registered in this DataTypeTable
     * @return
     */
    public int size() {
        return map.size();
    }
    
    /**
     * Clears all registered entries from the table
     */
    public void clear() {
        map.clear();
        collatedTypeMap.clear();
        defaultSequenceCollectionAssigned=false;
    }
    
    public void debug() {
        System.err.println("====== ( "+this+" ) Size:"+map.size()+"  ======");
        for (String key:map.keySet()) {
            System.err.println("    "+key+" => "+map.get(key).toString());
        }
        System.err.println("----------------------------------------\n");
    }
}
