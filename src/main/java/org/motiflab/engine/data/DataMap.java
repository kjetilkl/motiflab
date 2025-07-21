package org.motiflab.engine.data;

import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public abstract class DataMap extends Data {

    public static final String DEFAULT_KEY="_DEFAULT_";
    private String[] constructor=null;

    /**
     * Returns the Class type of the members that are used in this map.
     * This could be e.g. Sequence.class for SequenceNumericMaps or Motif.class for MotifNumericMaps
     */
    public abstract Class getMembersClass();
    
    /**
     * Returns TRUE if this map can contain a mapping for the given data item.
     * E.g. if this map is a SequenceTextMap or SequenceNumeric map, it will return TRUE
     * if the data item is a Sequence and FALSE in other cases
     * @param item
     * @return 
     */
    public boolean canContain(Data item) {
        if (item==null) return false;
        return (item.getClass()==getMembersClass());
    }


    /** Removes all explicitly assigned entries from this map
     *  (The default value is kept)
     */
    public abstract void clear();
    
    /** Sets the default value to the factory default
     */
    public abstract void clearDefault();
    
    /**
     * Returns the default value of this map
     * @return 
     */
    @Override
    public abstract Object getValue(); 
    
    /**
     * Returns the value associated with the given data object
     * or a default value if no value is registered for the data object
     * @return 
     */
    public abstract Object getValue(String dataname);     
    
    /**
     * Sets the value for the given data object based on a string which should
     * be converted to an internal value of the correct type
     * @param dataname The name of the dataobject that maps to the value
     * @param value The value for the dataobject represented in the form of a string
     * @throws ParseError if the value could not be converted to the proper type
     */
    public abstract void setValueFromString(String dataname, String value) throws ParseError;
    
    /**
     * Sets the default value for this map based on a string which should
     * be converted to an internal value of the correct type
     * @param value The value for the dataobject represented in the form of a string
     * @throws ParseError if the value could not be converted to the proper type
     */
    public abstract void setDefaultValueFromString(String value) throws ParseError;    
    
    /**
     * Returns true if this Map contains an explicitly set
     * value for the given data object (that is, when asking for the value for
     * the given data object with getValue(dataname) it will not just return
     * the default value
     */
    public abstract boolean contains(String dataname);

   
    
    /** 
     * Returns the number of entries that have specifically assigned values in
     * the map (and do not rely on the default value)
     * @return 
     */
    public abstract int getNumberOfSpecificallyAssignedEntries();
        
  
   /**
    * Returns a HashMap wherein the value for each entry corresponds to the rank
    *  that the entry has in this Map. I.e. the entry with the lowest value
    *  is given a rank value of "1" and the second lowest entry is given the value "2" etc.
    *  (if 'ascending' parameter is false the entry with the highest value is given
    *  rank "1". Tied entries with similar values are assigned the same rank, 
    *  and the next rank value will then be set to the the number of entries that 
    *  have lower values. For instance, if the values in the map are:
    *  13,13,24,32,32,32,58 the corresponding ranks will be 1,1,3,4,4,4,7  
    *  @return 
    */
    public abstract HashMap<String,Double> getRankOrder(boolean ascending);
    
    /**
     * This method will sort the provided list (which should contain entries in the map)
     * according to the ascending order in the map
     * @param list
     * @return 
     */
    public abstract void sortAccordingToMap(ArrayList<String> list);
    
    /**
     * Returns a set of names for which values have been explicitly assigned in this Map
     * @return
     */
    public abstract ArrayList<String> getRegisteredKeys();

    /**
     * Returns an array containing all applicable keys for this Map
     * Subclasses should override this method to return
     * keys depending on what kind of objects they apply to
     * @param engine
     * @return
     */
    public abstract ArrayList<String> getAllKeys(MotifLabEngine engine);

    /**
     * Returns an array containing the keys of all entries in this Map whose corresponding
     * values pass a filter test. The filter is provided with the map value as an Object argument.
     * @param filter This should return 1 (or positive value above 0) if the entry passes the test, else 0 (or negative value).
     * @param engine
     * @return 
     */
    public ArrayList<String> getMatchingEntries(Comparable<Object> filter, MotifLabEngine engine) {
        ArrayList<String> keys=getAllKeys(engine);
        ArrayList<String> matching=new ArrayList();
        if (filter==null) return matching;
        for (String key:keys) {
            if (filter.compareTo(getValue(key))>0) matching.add(key);
        }
        return matching;
    }
    
    /** This is use by operation collate */
    public abstract HashMap<String,Object> getColumnData();    
    

    private static final long serialVersionUID = -5906307498325056582L;
    
    /** Returns the constructor parameter string for this data object corresponding to the given constructor-prefix     
     *  or NULL if this data object was not constructed in this way
     * @param constructorPrefix
     * @return 
     */
    public String getConstructorString(String constructorPrefix) {
        if (constructor==null || constructor.length<2) return null;
        if (constructor[0].equals(constructorPrefix)) return constructor[1];
        else return null;
    }
    
    /** Returns the full constructor string for this data object.
     *  The constructor string is a direct concatenation of the prefix and the parameters
     *  If the data object does not have an associated constructor an empty string
     *  will be returned (not NULL)
     * @param constructorPrefix
     * @return 
     */
    public String getFullConstructorString() {
        if (constructor==null || constructor.length<2 || constructor[0]==null || constructor[1]==null) return "";
        else return constructor[0]+constructor[1];
    }    
    
    /** Sets the constructor property-prefix and parameter string for this data object.
     *  If one of the parameters are NULL or empty, the constructor will be cleared
     * @param constructorPrefix
     * @param parameters
     * @return 
     */ 
    public void setConstructorString(String constructorPrefix, String parameters) {
        if (constructorPrefix==null || constructorPrefix.isEmpty() || parameters==null || parameters.isEmpty()) constructor=null;
        else constructor=new String[]{constructorPrefix,parameters};
    }   
    
    /**
     * Returns TRUE if this data object was based on the given constructor type
     * @param constructorPrefix
     * @return 
     */
    public boolean hasConstructorString(String constructorPrefix) {
        if (constructor==null || constructor.length<2 || constructor[0]==null) return false;
        return constructor[0].equals(constructorPrefix);
    }
    
    /**
     * Returns TRUE if this data object has an associated constructor string
     * @return 
     */
    public boolean hasConstructorString() {
        return (constructor!=null && constructor.length==2 && constructor[0]!=null && constructor[1]!=null);
    }    
    
    /**
     * Returns the prefix for the constructor used for this data object
     * or NULL if this object was not based on a particular constructor
     * @return 
     */    
    public String getConstructorTypePrefix() {
       if (constructor==null || constructor.length<2) return null;
       else return constructor[0];
    }
    
    /**
     * Sets the constructor of this data object to the same value as the other object
     * @param other 
     */    
    public void cloneConstructor(DataMap other) {
        String otherPrefix=other.getConstructorTypePrefix();
        String otherParameter=other.getConstructorString(otherPrefix);
        setConstructorString(otherPrefix, otherParameter);
    }
    
    /**
     * Clears the constructor string associated with this data object
     */
    public void clearConstructorString() {
        constructor=null;
    }
    
    /**
     * Returns TRUE if this object has the same constructor type and parameters as the other object
     * The method will also return TRUE if neither objects have associated constructors (both are NULL)
     * @param other
     * @return 
     */    
    public boolean hasSameConstructor(DataMap other) {
        String thisPrefix=this.getConstructorTypePrefix();
        String otherPrefix=other.getConstructorTypePrefix();
        if (thisPrefix==null && otherPrefix==null) return true;
        if (! (otherPrefix!=null && thisPrefix!=null && thisPrefix.equals(otherPrefix))) return false;
        // here the prefixes are equal (and not NULL)
        String thisParameter=this.getConstructorString(thisPrefix);         
        String otherParameter=other.getConstructorString(otherPrefix);   
        return (thisParameter!=null && otherParameter!=null && thisParameter.equals(otherParameter));
    }        
    
    /**
     * Returns TRUE if this map contains the same keys with the same values as the other map
     * but other properties of the map (such as how they were constructed) need not be the same.
     * @param other
     * @return 
     */
    public abstract boolean containsSameMappings(DataMap other);
    
}
