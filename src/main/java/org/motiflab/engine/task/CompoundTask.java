/*
 
 
 */

package org.motiflab.engine.task;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.protocol.Protocol;
import org.motiflab.engine.MotifLabEngine;

/**
 * This class represents an compound task wrapping several Executable tasks that should be executed in order
 * 
 * @author kjetikl
 */
public class CompoundTask extends ExecutableTask implements PropertyChangeListener {

    protected ArrayList<ExecutableTask> tasklist;
    protected double overallProgress=0;
    protected int currentTaskNumber=1; // the number (in order) of the OperationTask currently executing (starting at 1)
        
    public CompoundTask(String taskName) {
        super(taskName);
        tasklist=new ArrayList<ExecutableTask>();
    }
        
    /**
     * Adds a new task to this compound. New tasks can only be added before the
     * compound task has started execution 
     * @return true if the OperationTask was successfully added or false if not
     */
    public boolean addTask(ExecutableTask task) {
        if (getStatus().equals(PENDING)) {
            boolean ok=tasklist.add(task);
            if (ok) task.setParentTask(this);
            return ok;
        } else return false;
    }

    @Override
    public HashMap<String,Class> getAffectedDataObjects() {
        HashMap<String,Class> allAffected=new HashMap<String, Class>();
        HashMap<String,Class> thisAffected=super.getAffectedDataObjects();
        if (thisAffected!=null) allAffected.putAll(thisAffected);
        for (ExecutableTask subtask:tasklist) {
           HashMap<String,Class> subtaskAffected=subtask.getAffectedDataObjects();
           if (subtaskAffected!=null) allAffected.putAll(subtaskAffected); // this could overwrite results by earlier tasks in the list
        }
        return allAffected;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        /* The CompoundTask registers and listens for propertyChanges reported by subtasks while they are running 
         * so that these updates can be reflected in this object as well (and propagated to listeners registered in this task) 
         */
             if (evt.getPropertyName().equals(STATUS_MESSAGE)) setStatusMessage((String)evt.getNewValue());
        else if (evt.getPropertyName().equals(STATUS)) {
            if (((String)evt.getNewValue()).equals(ABORTED) && !getStatus().equals(ABORTED)) setStatus(ABORTED); // setStatus will also propage to children!
            if (((String)evt.getNewValue()).equals(WAITING) && !getStatus().equals(WAITING)) setStatus(WAITING);
        }
        else if (evt.getPropertyName().equals(PROGRESS)) {
            double segment=100.0f/(tasklist.size()*1.0f);
            int completed=currentTaskNumber-1; // 'currentTaskNumber' is incremented for each finished subtask
            double operationProgress=(double)((Integer)evt.getNewValue()).intValue(); // this is the progress reported by the subtask
            overallProgress=(segment*completed)+(segment*operationProgress/100.0f);   // this is the overall progress in the compound task
            setProgress((int)overallProgress);            
        } else if (evt.getPropertyName().equals(FINISHED_EXECUTION) || evt.getPropertyName().equals(STARTED_EXECUTION)) { // all other propertyChanges reported by subtasks are propagated directly to listener of this task
           // these notifications from subtasks will just be consumed here
        } else if (evt.getPropertyName().equals(STARTED_EXECUTION_OF_LINE) || evt.getPropertyName().equals(FINISHED_EXECUTION_OF_LINE)) { //
            //setExecutingLineNumber((Integer)evt.getNewValue());            
            PropertyChangeEvent newEvt=new PropertyChangeEvent(evt.getSource(), evt.getPropertyName(), getTaskName(), evt.getNewValue()); // the 'old' value in this case is the name of the task. We replace it here with the parent
            firePropertyChange(newEvt);
        } else { 
            firePropertyChange(evt); // all other propertyChanges reported by subtasks are propagated directly to listener of this task
        }
    }   
    
     @Override
     public void setStatus(String status) {         
        super.setStatus(status);
        if (ExecutableTask.ABORTED.equals(status)) { // propagate abort-signal to all subtasks so that the signal is available to them during their execution
            for (ExecutableTask subtask:tasklist) subtask.setStatus(status);
        }
     }    
     
     
    @Override
    public void run() throws InterruptedException, Exception { // this method will throw all exception back to the caller since none are caught here
        setStatus(RUNNING); 
        overallProgress=0;
        setProgress((int)overallProgress);
        if (undoMonitor!=null) undoMonitor.register();
        int size=tasklist.size();
        ExecutableTask nextTask=null;
        try {
           for (int i=0;i<size;i++) {
              if (Thread.interrupted() || isAborted()) throw new InterruptedException();               
              checkExecutionLock();           
              nextTask=tasklist.get(i);
              currentTaskNumber=i+1;
              //setExecutingLineNumber(nextTask.getLineNumber());
              nextTask.addPropertyChangeListener(this);
              nextTask.setExecutionLock(getExecutionLock());
              nextTask.setMotifLabClient(getMotifLabClient());
              notifySubTaskStarted(nextTask);
              nextTask.run();
              notifySubTaskFinished(nextTask, true); // last boolean value is exit status           
              nextTask.setMotifLabClient(null);
              nextTask.setExecutionLock(null);
              nextTask.removePropertyChangeListener(this);   
              nextTask=null;
          }
        } finally { // uncaught exceptions are thrown back to the caller, but first we clean up!
            if (undoMonitor!=null) undoMonitor.deregister(true); // true=> register final state to allow redo of successfully executed subtasks
            if (nextTask!=null) {
                notifySubTaskFinished(nextTask, false); // last boolean value is exit status                  
                nextTask.setMotifLabClient(null);
                nextTask.removePropertyChangeListener(this); 
                nextTask.setExecutionLock(null);
            }
        }       
        setStatus(DONE);
    }

    
    private void notifySubTaskStarted(ExecutableTask subtask) {
        firePropertyChange(STARTED_EXECUTION_OF_LINE, getTaskName(), subtask.getLineNumber()); // the executing line number should already have been set by the subtask already
        firePropertyChange(SUBTASK_STARTED, subtask, true);
    } 
    
    /** Convenience method to notify listeners that this task has finished executed of a subtask
     * @param subtask  The subtask that has finished executing
     * @param finished set this to TRUE if the subtask completed its full execution or FALSE if it was ended abruptly (e.g. because of an exception)
     */    
    private void notifySubTaskFinished(ExecutableTask subtask, boolean finished) {
        if (finished) firePropertyChange(FINISHED_EXECUTION_OF_LINE, getTaskName(), subtask.getLineNumber()); // the executing line number should already have been set by the subtask already
        firePropertyChange(SUBTASK_ENDED, subtask, finished);        
    }
    
    


    @Override
    public String getCommandString(Protocol protocol) {
        if (protocol==null) return null;
        else return protocol.getCommandString(this);
    }    
    
    /** Returns the list of tasks wrapped by this CompoundTask */
    public ArrayList<ExecutableTask> getTaskList() {
        return tasklist;
    }

    @Override
    public void purgeReferences() {
        for (ExecutableTask task:tasklist) task.purgeReferences();
    }
    
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[CompoundTask] ===== "+getTaskName()+" =====  (Size: "+((tasklist!=null)?tasklist.size():0)+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);        
        if (tasklist!=null) {
            for (ExecutableTask task:tasklist) {
                task.debug(verbosity, indentLevel+1);
            }        
        }        
        MotifLabEngine.debugOutput("-------------------------------------------[End CompoundTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }    
      
}


