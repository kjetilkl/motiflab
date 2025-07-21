/*
 
 
 */

package org.motiflab.engine.data;

import java.util.ArrayList;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;

/**
 * DataCollection is superclass for SequenceCollection, MotifCollection ...
 * @author kjetikl
 */
public abstract class DataCollection extends Data implements DataGroup {

    static final long serialVersionUID = 1509898011654039338L;
    private String[] constructor=null; // 2 part array. First is constructor prefix
 
    /**
     * Returns the names of all the Data objects in this collection
     * Note that this list could represent the original data in the collection
     * (instead of being a copy) and it should never be manipulated directly.
     *
     * @return A list with the names of Data objects contained in this collection
     */
    public abstract ArrayList<String> getValues();
     
    
    /**
     * Returns TRUE if this collection contains an entry with the given name
     * @param name
     * @return
     */
    public abstract boolean contains(String name);


    /**
     * Returns true if this collection contains the exact same entries as the other collection (which must be of the same type)
     * @param other
     * @return 
     */
    public boolean containsSameEntries(DataCollection other) {
        if (!this.getClass().equals(other.getClass())) return false;
        if (size()!=other.size()) return false;
        for (String name:getValues()) {
            if (!other.contains(name)) return false;
        }
        return true;
    }    
    
    /** Returns TRUE if this collection is a subset of the other collection (or the same) */
    public boolean isSubsetOf(DataCollection other) {
        if (!this.getClass().equals(other.getClass())) return false;
        if (size()>other.size()) return false; // this set is larger so it cannot be a subset!
        for (String name:getValues()) {
            if (!other.contains(name)) return false;
        }
        return true;       
    }
    
    /** Returns TRUE if this collection is a proper subset of the other collection.
     *  i.e. all the elements in this collection are also in the other collection 
     *  but the other collection also contains additional elements not in this collection
     */
    public boolean isProperSubsetOf(DataCollection other) {
        return (isSubsetOf(other) && other.size()>this.size());
    }    
    
    /** Returns TRUE if this collection is a superset of the other collection (or the same) */
    public boolean isSupersetOf(DataCollection other) {
        return other.isSubsetOf(this);
    }  
    
    /** Returns TRUE if this collection is a proper superset of the other collection.
     *  i.e. all the elements in the other collection are also in this collection 
     *  but the this collection also contains additional elements not in the other collection
     */
    public boolean isProperSupersetOf(DataCollection other) {
        return other.isProperSubsetOf(this);
    }     
    
    /** Returns TRUE if this collection has at least one entry in common with the other collection
     * (based on entry names only, not on type. Two collections of different types can be compared with this method).
     */
    public boolean overlaps(DataCollection other) {
        if (this.isEmpty() || other.isEmpty()) return false;
        for (String entry:getValues()) {
            if (other.contains(entry)) return true;
        }
        return false;
    }     
       
    /**
     * Returns the size of this DataCollection
     * @return
     */
    public abstract int size();

    /** Returns TRUE is this Data collection contains no elements */
    public abstract boolean isEmpty();

    public abstract void initializeFromPayload(MotifLabEngine engine) throws ExecutionError;

    public abstract boolean hasPayload();
    
    @Override
    public String getConstructorString(String constructorPrefix) {
        if (constructor==null || constructor.length<2) return null;
        if (constructor[0].equals(constructorPrefix)) return constructor[1];
        else return null;
    }
    
    @Override
    public String getFullConstructorString() {
        if (constructor==null || constructor.length<2 || constructor[0]==null || constructor[1]==null) return "";
        else return constructor[0]+constructor[1];
    }     
    
    @Override
    public void setConstructorString(String constructorPrefix, String parameters) {
        if (constructorPrefix==null || constructorPrefix.isEmpty() || parameters==null || parameters.isEmpty()) constructor=null;
        else constructor=new String[]{constructorPrefix,parameters};
    }   
    
    @Override
    public boolean hasConstructorString() {
        return (constructor!=null && constructor.length==2 && constructor[0]!=null && constructor[1]!=null);
    }       
    
    @Override
    public boolean hasConstructorString(String constructorPrefix) {
        if (constructor==null || constructor.length<2 || constructor[0]==null) return false;
        return constructor[0].equals(constructorPrefix);
    }
    
    @Override
    public void clearConstructorString() {
        constructor=null;
    }    
    
    @Override
    public String getConstructorTypePrefix() {
       if (constructor==null || constructor.length<2) return null;
       else return constructor[0];
    }
    
    @Override   
    public void cloneConstructor(DataGroup other) {
        String otherPrefix=other.getConstructorTypePrefix();
        String otherParameter=other.getConstructorString(otherPrefix);
        setConstructorString(otherPrefix, otherParameter);
    }
    
    @Override
    public boolean hasSameConstructor(DataGroup other) {
        String thisPrefix=this.getConstructorTypePrefix();
        String otherPrefix=other.getConstructorTypePrefix();
        if (thisPrefix==null && otherPrefix==null) return true;        
        if (! (otherPrefix!=null && thisPrefix!=null && thisPrefix.equals(otherPrefix))) return false;
        // here the prefixes are equal (and not NULL)
        String thisParameter=this.getConstructorString(thisPrefix);         
        String otherParameter=other.getConstructorString(otherPrefix);   
        return (thisParameter!=null && otherParameter!=null && thisParameter.equals(otherParameter));
    }        
    
    
            
}
