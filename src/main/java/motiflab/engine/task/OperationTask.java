/*
 
 
 */

package motiflab.engine.task;

import motiflab.engine.task.ExecutableTask;
import motiflab.engine.operations.*;
import motiflab.engine.data.Data;
import java.util.HashMap;
import java.util.Set;

import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.Protocol;

/**
 * Whereas the Operation classes represents singleton objects that only hold the
 * code necessary to perform different operations and contain no field variables
 * to hold instance specific data or parameters, the OperationTasks objects are
 * used to store the values for all parameters needed to perform an operation on
 * a specified set of data objects.   
 * 
 * OperationTasks also contain functions to obtain and update information about
 * the progress and status of an operation execution.
 * 
 * @author kjetikl
 */
public class OperationTask extends ExecutableTask {
    public static final String SOURCE="source";
    public static final String SOURCE_NAME="sourceName";
    public static final String TARGET="target";
    public static final String TARGET_NAME="targetName";
    public static final String OPERATION="operation";
    public static final String OPERATION_NAME="operationName";
    public static final String ENGINE="engine";
    public static final String SEQUENCE_COLLECTION_NAME="sequenceCollectionName"; // this field should hold the NAME (String) of the SequenceCollection
    public static final String SEQUENCE_COLLECTION="sequenceCollection"; // this field should hold the SequenceCollection itself
    private static final String RESERVED_NAMES="_RESERVED_NAMES";
   
    
    public OperationTask(String taskName) {
        super(taskName);
    }
   

    /**
     * Returns the Operation object associated with this task
     */    
    public Operation getOperation() {
       Operation operation=(Operation)getParameter(OPERATION);
       if (operation!=null) return operation;
       else {
            String name=(String)getParameter(OPERATION_NAME);
            if (name==null) return null;
            MotifLabEngine engine=(MotifLabEngine)getParameter(ENGINE);
            if (engine!=null) return engine.getOperation(name);
       }
       return null;
    }
    /**
     * Returns the name of the Operation object associated with this task
     */    
    public String getOperationName() {
       String name=(String)getParameter(OPERATION_NAME);
       if (name!=null) return name;
       else {
          Operation operation=(Operation)getParameter(OPERATION);
          if (operation!=null) return operation.getName();
       }
       return null;
    }
    
    
    /**
     * Returns a reference to the engine (if set)
     */    
    public MotifLabEngine getEngine() {
        MotifLabEngine engine=(MotifLabEngine)getParameter(ENGINE);
        if (engine==null) {
          Operation operation=(Operation)getParameter(OPERATION);
          if (operation!=null) {
              engine=operation.getEngine();
              setParameter(ENGINE, engine);
          }      
        }
        return engine;
    }

    
    /**
     * Returns the Target dataobject associated with this task (if it exists)
     */    
    public Data getTargetData() {
       Data data=(Data)getParameter(TARGET);
       return data;
    }  
    
    /**
     * Returns the name of the Target dataobject associated with this task
     */    
    public String getTargetDataName () {
       return (String)getParameter(TARGET_NAME);
    }
    
    /**
     * Returns the Source dataobject associated with this task
     */    
    public Data getSourceData() {
       Data data=(Data)getParameter(SOURCE);
       return data;
    }
    
    /**
     * Returns the name of the Source dataobject associated with this task
     */    
    public String getSourceDataName() {
       return (String)getParameter(SOURCE_NAME);
    }
    
    /**
     * Returns a textual representation of this command (with parameters) to be included in protocol scripts
     */   
    @Override
    public String getCommandString(Protocol protocol) {
       if (protocol==null) return null;
       return protocol.getCommandString(this);
    }

    /**
     * This method can be used to 'reserve' names for new data objects that will be created by this task
     * The names of "default target(s)" will automatically be marked as reserved so there is no need to explicitly reserve these
     * but other names might be necessary to reserve (for instance the names of motif collections for motif discovery operations)
     */
    public void reserveDataName(String name) {
        String reserved=(String)getParameter(RESERVED_NAMES);
        if (reserved==null) reserved=name; else reserved=reserved+","+name;
        setParameter(RESERVED_NAMES, reserved);
    }

    /** Returns a comma-separated list of names that this task wishes to reserve for its output data
     *  The string can be null or empty
     */
    public String getReserveDataNames() {
        String reserved=(String)getParameter(RESERVED_NAMES);
        String targetDataNames=getTargetDataName();
        if (reserved==null) return getTargetDataName();
        else {
            if (targetDataNames==null || targetDataNames.isEmpty()) return reserved;
            else return targetDataNames+","+reserved;
        }
    }
        
    /**
     * Executes the operation associated with this task
     * Implements the Runnable interface
     */
    @Override
    public void run() throws InterruptedException, Exception {
        //System.err.println("Executing operation");debug(); 
        boolean done=false;
        Operation operation=getOperation();  
        if (operation==null) return;
        setProgress(1);
        setStatus(RUNNING);  
        logMessage("Executing operation: "+operation.getName(), 10);
        if (undoMonitor!=null) {
            undoMonitor.register();
            if (operation.affectsSequenceOrder()) undoMonitor.storeSequenceOrder(); // this is required in order to restore sequence order on UNDO
        }
        while (!done){
          try {      
            operation.execute(this);
            done=true;
          } catch (InterruptedException e) { // task aborted by the user
              if (undoMonitor!=null) undoMonitor.deregister(false);
              setStatus(ABORTED);
              throw e;
          } catch (Exception e) { // other errors
              if (shouldRetry(e)) {                  
                  setRetrying(true);
              } else {
                  if (undoMonitor!=null) undoMonitor.deregister(false);
                  setStatus(ERROR);
                  throw e;
              }
          }            
        }
        setProgress(100);
        if (undoMonitor!=null) undoMonitor.deregister(true);
        setStatus(DONE);
        setStatusMessage(null);
    }

    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[OperationTask] ===== "+getOperationName()+" =====  (Line: "+getLineNumber()+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);
        if (verbosity>=3) {
            motiflab.engine.protocol.StandardProtocol protocol=new motiflab.engine.protocol.StandardProtocol(MotifLabEngine.getEngine());
            MotifLabEngine.debugOutput(" Command: "+getCommandString(protocol),indentLevel);
        }
        if (verbosity>=3) {
            for (String paramName:storage.keySet()) {
                Object value=getParameter(paramName);
                if (value==null) MotifLabEngine.debugOutput(" > "+paramName+" is NULL",indentLevel);
                else if (value instanceof ParameterSettings) {
                    MotifLabEngine.debugOutput(" > "+paramName+" [ParameterSettings]:",indentLevel+1);
                    ((ParameterSettings)value).debug(indentLevel);
                } else MotifLabEngine.debugOutput(" > "+paramName+" = "+value.toString(),indentLevel);
            }
        }
        if (verbosity>1) MotifLabEngine.debugOutput("-------------------------------------------[End OperationTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }
}
