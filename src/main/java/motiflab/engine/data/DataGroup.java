package motiflab.engine.data;

import motiflab.engine.MotifLabEngine;

/**
 * This interface serves as a common reference to Collections and Partitions
 * @author kjetikl
 */
public interface DataGroup {

    /**
     * Returns the Class type of the members that are used in this group.
     * This could be e.g. Sequence.class for SequencePartitions or Motif.class for MotifCollections etc
     */    
    public abstract Class getMembersClass();    
      
   /**
     * Removes all entries from this collection or partition, and also any associations 
     * with tracks, maps or predefined collections etc.
     * @param engine This parameter is not required (can be null) but if provided
     *        any data listeners will be notified that data has been removed from
     *        this collection
     */
    public abstract void clearAll(MotifLabEngine engine);    
    
    /** Returns the constructor parameter string for this data object corresponding to the given constructor-prefix     
     *  or NULL if this data object was not constructed in this way
     * @param constructorPrefix
     * @return 
     */
    public abstract String getConstructorString(String constructorPrefix);
    
    /** Returns the full constructor string for this data object.
     *  The constructor string is a direct concatenation of the prefix and the parameters
     *  If the data object does not have an associated constructor an empty string
     *  will be returned (not NULL)
     * @param constructorPrefix
     * @return 
     */
    public abstract String getFullConstructorString();    
    
    /** Sets the constructor property-prefix and parameter string for this data object.
     *  If the constructorPrefix is NULL the constructor will be cleared
     * @param constructorPrefix
     * @param parameters
     * @return 
     */
    public abstract void setConstructorString(String constructorPrefix, String parameters);
    
    /**
     * Returns TRUE if this data object has an associated constructor string
     * @return 
     */
    public abstract boolean hasConstructorString();    
    
    /**
     * Returns TRUE if this data object was based on the given constructor type
     * @param constructorPrefix
     * @return 
     */
    public abstract boolean hasConstructorString(String constructorPrefix);
    
    /**
     * Clears the constructor string associated with this data object
     */
    public abstract void clearConstructorString();  
    
    /**
     * Returns the prefix for the constructor used for this data object
     * or NULL if this object was not based on a particular constructor
     * @return 
     */
    public abstract String getConstructorTypePrefix();
    
    /**
     * Sets the constructor of this data object to the same value as the other object
     * @param other 
     */
    public abstract void cloneConstructor(DataGroup other);
    
    /**
     * Returns TRUE if this object has the same constructor type and parameters as the other object
     * The method will also return TRUE if neither objects have associated constructors (both are NULL)
     * @param other
     * @return 
     */ 
    public abstract boolean hasSameConstructor(DataGroup other);    
    
}
