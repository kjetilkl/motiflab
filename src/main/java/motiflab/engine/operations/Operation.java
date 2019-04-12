package motiflab.engine.operations;

import motiflab.engine.task.OperationTask;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.protocol.*;

/**
 * An operation is a language independent abstraction of 
 * @author kjetikl
 */
public abstract class Operation {
    protected MotifLabEngine engine;
    
    public Operation(MotifLabEngine engine) {
        this.engine=engine;
    }

    public Operation() { }

    /** 
     * Sets the engine for this operation. This method should always be called 
     * directly after instantiation of new operations with a zero-parameters constructor
     */
    public void setEngine(MotifLabEngine engine) {
        this.engine=engine;
    }
    /** 
     * Gets the engine for this operation.
     */
    public MotifLabEngine getEngine() {
       return engine;
    }
    
    /**
     * Returns a descriptive name or command for the operation
     * 
     * @return the name of the operation (command)
     */
    public abstract String getName();
    
    /**
     * Returns a short description of the command to be used for Tool Tips or similar
     * @return 
     */
    public abstract String getDescription();

    /** Returns the assigned operation group for this operation.
     *  E.g. "Transform","Derive","Motif","Module","Combine" etc.
     *  The default implementation returns NULL so subclasses should
     *  override to return the actual group where applicable
     */
    public String getOperationGroup() {
        return null;
    }

    /**
     * Returns an URL referring to a 'help page' document for this operation
     * or a String containing the 'help page' document itself.
     * The help-page describes the purpose of the operations and its various parameters
     * @return
     */

    public Object getHelp(MotifLabEngine engine) {
        if (engine!=null) {
            try {
               return new java.net.URL(engine.getWebSiteURL()+"getHelp.php?type=Operation&topic="+getName());
            } catch (Exception e) {}            
        }
        return null;
    }
    
     /**
     * Returns an array of classes that this operation can work on
     * @return 
     */  
    public abstract Class[] getDataSourcePreferences();
    
    /**
     * Returns an array of words that this operation needs to 'reserve'.
     * Reserved words can not be used as names for datatracks or variables in protocol scripts
     * (note that names in protocol-scripts are case-sensitive)
     * @return a list of reserved words
     */  
    public String[] getReservedWords() {return null;}
    
    
    /** Operations that could affect sequence order (including deleting sequences) 
     *  should override this method to return TRUE so that the original order can
     *  be stored and then restored on UNDO
     */
    public boolean affectsSequenceOrder() {
        return false;
    }
    
    /**
     * This function should return true if the supplied data argument is of
     * a type that the operation can work on, or false if the operation can
     * not be performed on this type of data
     * @param object
     * @return true if this operation can work on data of the specified type
     */
    public final boolean canUseAsSource(Data data) {
        if (data==null) return false;
        Class[] sources=getDataSourcePreferences();
        if (sources==null) return true;
        for (Class source:sources) {
            if (source.isInstance(data)) return true;
        }
        return false;
    }
    
    /** This method returns TRUE if the given list is non-empty and 
     *  every data item in the list is of a type which can be used as 
     *  source for this operation.
     */
    public final boolean canUseAsSource(Data[] datalist) {
        if (datalist==null || datalist.length==0) return false;
        for (Data data:datalist) {
            if (!canUseAsSource(data)) return false;
        }
        return true;
    }    
    
    /**
     * This function should return true if the supplied class argument represents
     * a type that the operation can work on, or false if the operation can
     * not be performed on this class type
     * @param object
     * @return true if this operation can work on data of the specified class
     */
    @SuppressWarnings("unchecked")
    public final boolean canUseAsSource(Class type) {
        if (type==null) return false;
        Class[] sources=getDataSourcePreferences();
        if (sources==null) return true;
        for (Class source:sources) {
            if (source.isAssignableFrom(type)) return true;
        }
        return false;
    }
      
    /**
     * This function should return true if the given data object can be used,
     * not as the primary source object, but as source for one of the other 
     * parameters the operation requires, and the operation wants to make this
     * fact known, so that for instance the "perform operation" popup-menu can
     * include this operation among its choices.
     * @param object
     * @return 
     */  
    public boolean canUseAsSourceProxy(Data data) {
        return false;
    }    

    /** Returns true if the given list is non-empty and any of the data
     *  items therein can be used as a proxy source for this operation
     */
    public boolean canUseAnyAsSourceProxy(Data[] datalist) {
        if (datalist==null || datalist.length==0) return false;
        for (Data data:datalist) {
            if (canUseAsSourceProxy(data)) return true;
        }
        return false;
    }     
    
    /**
     * This function can be called to assign the given data object to a parameter
     * in the task parameter (which parameter the data object will be assigned to 
     * can be chosen at the discretion of the operation, but it should not be 
     * the primary source)
     * 
     * @param proxysource This should be a single Data object or a Data[] array
     * @param operationtask
     * @return true if the dataobject was successfully assigned as proxy source
     */
    public boolean assignToProxy(Object proxysource, OperationTask operationtask) {
        return false;
    }
        
    /** 
     * This method should return TRUE if the operation can be applied to
     * subregions (windows) within a sequence. For instance, transform operations that
     * replace values at each position with a new value can usually be applied
     * to portions of a sequence, while the "convert" operation which converts
     * numeric data to regions data or vice versa must be applied to the whole
     * dataset or not at all
     */
    public boolean isSubrangeApplicable() {
        return false; 
    }
    
    /** 
     * This method should return TRUE if the operation can have multiple datasets
     * as input such as the combine_X operations
     */
    public boolean canHaveMultipleInput() {
        return false; 
    }
    
    
    /**
     * This function executes the operation according to the parameters specified
     * in the supplied object (containing information about source and target datasets,
     * as well as operation specific parameters)
     * 
     * @param parameters 
     * @return true if the execution was successful
     */
    public abstract boolean execute(OperationTask operationtask) throws Exception;
    
    /**
     * This function returns a textual representation of an operation (with arguments)
     * in the format of the specified Protocol
     * 
     * @param parameters 
     * @return A string that can be included in protocol scripts
     */
    public final String getCommandString(OperationTask task, Protocol protocol) {
        return protocol.getCommandString(task);
    }

    
  
}
