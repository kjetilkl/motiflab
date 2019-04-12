/*
 
 
 */

package motiflab.engine.task;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import motiflab.engine.MotifLabEngine;

/**
 * This class represents an executable task corresponding to a Protocol script.
 * 
 * @author kjetikl
 */
public class ProtocolTask extends CompoundTask implements PropertyChangeListener {

    private String protocolName;
    private boolean partial=false;
    private int executingLineNumber=-1;
  
    public ProtocolTask(String protocolName) {
        super("execution of "+protocolName); // this will be the "task name" returned by getTaskName();
        this.protocolName=protocolName;
    }
    
    public String getProtocolName() {
        return protocolName;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) { // this method listens for callbacks from sub tasks in this protocol
             if (evt.getPropertyName().equals(STATUS_MESSAGE)) setStatusMessage((String)evt.getNewValue());
        else if (evt.getPropertyName().equals(STATUS)) {
            if (((String)evt.getNewValue()).equals(ABORTED) && !getStatus().equals(ABORTED)) setStatus(ABORTED); // setStatus will also propage to children!
            if (((String)evt.getNewValue()).equals(WAITING) && !getStatus().equals(WAITING)) setStatus(WAITING);
        }
        else if (evt.getPropertyName().equals(PROGRESS)) {
            double segment=100.0f/(tasklist.size()*1.0f);
            int completed=currentTaskNumber-1;
            double operationProgress=(double)((Integer)evt.getNewValue()).intValue();
            overallProgress=(segment*completed)+(segment*operationProgress/100.0f);
            setProgress((int)overallProgress); 
        } else if (evt.getPropertyName().equals(STARTED_EXECUTION_OF_LINE) || evt.getPropertyName().equals(FINISHED_EXECUTION_OF_LINE)) { //
            if (evt.getPropertyName().equals(STARTED_EXECUTION_OF_LINE)) setExecutingLineNumber((Integer)evt.getNewValue()); // this way the counter can only increment
            PropertyChangeEvent newEvt=new PropertyChangeEvent(evt.getSource(), evt.getPropertyName(), protocolName, evt.getNewValue()); // the 'old' value in this case is the name of the task. We replace it here with the name of the protocol (this is important for reporting progress in the protocol editor)
            firePropertyChange(newEvt);
        } else { 
            firePropertyChange(evt); // all other propertyChanges reported by subtasks are propagated directly to listener of this task
        }
    }  

    /** Convenience method to notify listeners that this task started executing */    
    private void notifyExecutionStarted() {
        firePropertyChange(STARTED_EXECUTION, protocolName, protocolName);
    }
    
    /** Convenience method to notify listeners that this task has finished executing */    
    private void notifyExecutionFinished() {
        firePropertyChange(FINISHED_EXECUTION, protocolName, protocolName);
    }   
    
    /** Convenience method to notify listeners that this task has started executing a subtask
     * @param subtask  The subtask that has started executing     
     */
    private void notifySubTaskStarted(ExecutableTask subtask) {
        int lineNumber=subtask.getLineNumber();
        setExecutingLineNumber(lineNumber);
        firePropertyChange(STARTED_EXECUTION_OF_LINE, protocolName, lineNumber); // the executing line number should already have been set by the subtask already
        firePropertyChange(SUBTASK_STARTED, subtask, true);
    } 
    
    /** Convenience method to notify listeners that this task has finished executed of a subtask
     * @param subtask  The subtask that has finished executing
     * @param finished set this to TRUE if the subtask completed its full execution or FALSE if it was ended abruptly (e.g. because of an exception)
     */    
    private void notifySubTaskFinished(ExecutableTask subtask, boolean finished) {
        int lineNumber=subtask.getLineNumber();   
        if (finished) firePropertyChange(FINISHED_EXECUTION_OF_LINE, protocolName, lineNumber); // the executing line number should already have been set by the subtask already
        firePropertyChange(SUBTASK_ENDED, subtask, finished);        
    }
    
    /** This can be called from the outside (e.g. by a Task Scheduler) to notify the running task that it has been aborted by the user */
    public void notifyExecutionAbortedByUser() {
        firePropertyChange(EXECUTION_OF_LINE_ABORTED, protocolName, new Integer(getExecutingLineNumber()));
    }    
    
    /** This can be called from the outside (e.g. by a Task Scheduler) to notify the running task that it has been stopped because of an error */    
    public void notifyExecutionStoppedByError() {
        firePropertyChange(EXECUTION_OF_LINE_ERROR, protocolName, new Integer(getExecutingLineNumber()));
    }        
    
    
    /**
     * Returns the line number for the operation that is currently executing (when part of a protocol script)
     * (or 0 if the operation is not associated with a script)
     */
    private int getExecutingLineNumber() {
        return (executingLineNumber>=0)?executingLineNumber:getLineNumber(); // default to 'lineNumber' if executingLineNumber has not been explicitly set (to something other than -1)
    }
    
    /**
     * Updates the line number of the line that is currently executing
     */
    private void setExecutingLineNumber(int line) {
         executingLineNumber=line;
    }  
    
    
    @Override
    public void run() throws InterruptedException, Exception { // this method will throw all exception back to the caller since none are caught here
        setStatus(RUNNING);
        notifyExecutionStarted();
        overallProgress=0;
        setProgress((int)overallProgress);
        if (undoMonitor!=null) {
            undoMonitor.register();
            undoMonitor.storeSequenceOrder(); //
        }
        int size=tasklist.size();
        ExecutableTask nextTask=null;
        try {
          for (int i=0;i<size;i++) {  
            if (Thread.interrupted() || isAborted()) throw new InterruptedException();
            checkExecutionLock();
            nextTask=tasklist.get(i);
            currentTaskNumber=i+1;
            nextTask.addPropertyChangeListener(this);
            nextTask.setExecutionLock(getExecutionLock());
            nextTask.setMotifLabClient(getMotifLabClient());
            notifySubTaskStarted(nextTask);            
            nextTask.run();
            notifySubTaskFinished(nextTask,true);
            nextTask.setMotifLabClient(null);
            nextTask.setExecutionLock(null);
            nextTask.removePropertyChangeListener(this);
            nextTask=null;            
          }
        } finally { // uncaught exception are thrown back to the caller, but first we clean up!
            if (undoMonitor!=null) undoMonitor.deregister(true); // true=> always register final state to allow redo of successful subtasks (even if an error occurred at one point in the protocol)
            if (nextTask!=null) {
                notifySubTaskFinished(nextTask,false);                
                nextTask.setMotifLabClient(null);
                nextTask.removePropertyChangeListener(this); 
                nextTask.setExecutionLock(null);
            }
        }       
        setProgress(100);
        setStatus(DONE);
        setStatusMessage("Protocol execution finished");
        notifyExecutionFinished();
    }

    /** 
     * Returns TRUE if this ProtocolTask represents a subselection of a protocol script rather than the full protocol
     */    
    public boolean isPartial() {
        return partial;
    }
    
    /**
     * Sets the isPartial flag which signals whether this ProtocolTask 
     * represents a subselection of a protocol script rather than the full protocol
     * @param ispartial 
     */
    public void setIsPartial(boolean ispartial) {
        partial=ispartial;
    }

     
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[ProtocolTask] ===== "+protocolName+" =====  (Size: "+((tasklist!=null)?tasklist.size():0)+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);        
        if (tasklist!=null) {
            for (ExecutableTask task:tasklist) {
                task.debug(verbosity, indentLevel+1);
            }        
        }        
        MotifLabEngine.debugOutput("-------------------------------------------[End ProtocolTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }    
    
}


