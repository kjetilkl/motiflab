/*
 
 
 */

package org.motiflab.engine.data;
import java.io.Serializable;
import java.util.ArrayList;
import org.motiflab.engine.DataListener;
import java.util.HashSet;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ExtendedDataListener;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.protocol.ParseError;
/**
 * A top level object for all types of data that could possibly be
 * manipulated by the motiflab, including constants, variables and
 * full genomic datasets
 * 
 * @author kjetikl
 */
public abstract class Data implements Serializable, Cloneable, Comparable {
    private transient HashSet<DataListener> datalisteners=null; 
    
    /**
     * Returns a name for this data object
     * @return name
     */
    public abstract String getName();
    
     /**
     * Returns a value for this data object
     * @return value
     */
    public abstract Object getValue();
    
    
    /**
     * Returns a String representation of the value of this data object
     * The string representation should be such that if it was used as a parameter
     * string to create a new Data object of compatible type using the the NEW operation 
     * in a Protocol script, the new Data object would have the same value as this object
     * @return
     */
    public abstract String getValueAsParameterString();
    
     /**
     * Renames the data object. Note that no checks are performed to verify that
     * the name is valid, so this must be done before calling this method
     * @param newName
     */
    public abstract void rename(String newName);
   
    
    /**
     * Returns a string describing what type of data this object represents (e.g. Sequence, Numeric etc.)
     * This should be the same for all objects of the same class
     * @return name
     */
    public static String getType() {return "<void>";}
       
    /**
     * This method should return the same String as the static getType() of the dynamic class
     * of this object
     * @return 
     */
    public abstract String getDynamicType();
    
    /**
     * Returns a string describing what type of data this object represents (e.g. Sequence, Numeric etc.)
     * Unlike the static getType() method, this method can return slightly different information for
     * different data objects of the same basic type
     * @return name
     */
    public abstract String getTypeDescription();
       
    
     /**
     * Registers a new DataListener that will be notified when changes occur to this Data item
     * @param listener 
     */
    public void addDataListener(DataListener listener) {
        if (datalisteners==null) datalisteners=new HashSet<DataListener>();
        datalisteners.add(listener);
    }
    
     /**
     * Registers a new DataListener that will be notified when changes occur to this Data item
     * @param listener 
     */
    public void removeDataListener(DataListener listener) {
        if (datalisteners==null) return;
        datalisteners.remove(listener);
    }
   
     /**
     * Notifies registered DataListeners that an update has occurred on this Data item
     */
    public void notifyListenersOfDataUpdate() {
        if (datalisteners==null) return;
        for (DataListener listener:datalisteners) {
            listener.dataUpdated(this);
        }
    }
    
    /**
     * Notifies registered DataListeners that an addition has occurred to this Data item
     * This is used when this data item represents a compound data object (a dataset) which
     * has child data items added to it (note that the child data item can be an "atomic" 
     * data object or a dataset of its own)
     */
    public void notifyListenersOfDataAddition(Data child) {
        if (datalisteners==null) return;
        for (DataListener listener:datalisteners) {
            listener.dataAddedToSet(this, child);
        }
    }
    
    /**
     * Notifies registered DataListeners that a removal has occurred on this Data item
     * This is used when this data item represents a compound data object (a dataset) which
     * has child data items removed from it (note that the child data item can be an "atomic" 
     * data object or a dataset of its own)
     */
    public void notifyListenersOfDataRemoval(Data child) {
        if (datalisteners==null) return;
        for (DataListener listener:datalisteners) {
            listener.dataRemovedFromSet(this,child);
        }
    }
      
     /**
     * Notifies registered DataListeners that an update has occurred on this Data item that changed the order of its children
     */
    public void notifyListenersOfDataReorder(Integer oldposition, Integer newposition) {
        if (datalisteners==null) return;
        for (DataListener listener:datalisteners) {
            if (listener instanceof ExtendedDataListener) ((ExtendedDataListener)listener).dataOrderChanged(this,oldposition,newposition);
        }
    }  
    
    
    /**
     * Imports important data from the given source object (which should be of the same type)
     * essentially making this object a duplicate copy of the source
     * @param source
     */
    public abstract void importData(Data source) throws ClassCastException;
   
    /**
     * This functions is used to compare this data object to another object to see if they contain the same information
     * Overriding methods should compare all fields and attributes of the data object except the "name" attribute which
     * is allowed to be different 
     * @return
     */
    public abstract boolean containsSameData(Data otherdataobject);
    
    /** Returns TRUE if this data object is a 'temporary' object
     *  Temporary objects are recognized by names beginning with an underscore
     *  (objects with NULL names also return TRUE!)
     */
    public boolean isTemporary() {
        String name=getName();
        return (name==null || name.startsWith("_"));
    }
    
    @Override
    public abstract Data clone();
    
    @Override
    public final String toString() {
        return getName();
    }

    /**
     *  This method should be called when an OperationTask (used to perform a "new"-operation) uses
     *  an existing (but not registered) data object as a template to obtain the required task settings.
     *  This method can be used by the Data template object to supply additional needed parameters (besides the common getParametersAsString() etc)
     *  that are required by Operation_new and are usually set when parsing the parameter string, but not set when this
     *  string is not parsed (which would be the case if the Data object was created by a dialog and not by a protocol command).
     *  The default implementation does nothing but it can be overriden in subclasses in order to set
     *  less commonly used settings such as Operation_new.FILENAME and Operation_new.DATA_FORMAT
     */
    public void setAdditionalOperationNewTaskParameters(OperationTask task) {}
    
    /**
     * Outputs the contents of the Data object in a plain format
     * This function should only be used when no DataFormats object are available to
     * provide a better formatted output of the data in a standardized format
     * @return
     */
    public String output() {
        Object value=getValue();
        if (value==null) return "";
        else return value.toString();
    }


    /**
     * Imports data from a document (represented as a list of strings) formatted in "plain" format
     * The current contents of this data object is replaced by the content specified in the document
     * @param input
     * @throws org.motiflab.engine.protocol.ParseError
     */
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        throw new ParseError("Unable to parse input in PLAIN format for "+getName());
    }

    /** Returns a list of names for result variables that are available in this data object */
    public String[] getResultVariables() {
        return new String[0];
    }

    /** Returns true if this data object has a result variable with the given name*/
    public boolean hasResult(String variablename) {
        String[] list=getResultVariables();
        for (String variable:list) {
            if (variable.equals(variablename)) return true;
        }
        return false;
    }

    /** Returns the value (Data object) of the result variable with the given name */
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    /** Returns the Class of the Data object for the result variable with the given name or NULL if no such variable exists*/
    public Class getResultType(String variablename) {
        return null;
    }

    @Override
    public int compareTo(Object o) {
       if (o instanceof Data) {
           return getName().compareTo(((Data)o).getName());
       } else return -1;
    }
    
    
    private static final long serialVersionUID = 8760014983879248219L;
    
}
