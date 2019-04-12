/*
 
 
 */

package motiflab.engine.task;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import motiflab.engine.MotifLabClient;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.Protocol;
import motiflab.gui.UndoMonitor;
/**
 *
 * @author kjetikl
 */
public abstract class ExecutableTask extends AbstractUndoableEdit {
    public static final String SCHEDULING_EVENT="SchedulingEvent"; // Used to notify scheduling events for tasks (schedule or abort/unschedule)
    public static final String PENDING="Pending"; // Task has not yet been started. This is the default for newly created tasks.
    public static final String RUNNING="Running"; // Task has been started and is currently running
    public static final String DONE="Done";       // Task has completed execution
    public static final String ABORTED="Aborted"; // Task has been aborted by user
    public static final String WAITING="Waiting"; // Task is running but must wait for an indeterminate time for another process to finish before continuing
    public static final String ERROR="Error";     // Task has ended because of an error

    public static final String STATUS="status";
    public static final String PROGRESS="progress";
    public static final String STATUS_MESSAGE="statusMessage";
    
    public static final String STARTED_EXECUTION="_startedEx";
    public static final String FINISHED_EXECUTION="_finishedEx";
    public static final String STARTED_EXECUTION_OF_LINE="_startedLineEx";
    public static final String FINISHED_EXECUTION_OF_LINE="_finishedLineEx";
    public static final String EXECUTION_OF_LINE_ABORTED="_abortedLineEx";
    public static final String EXECUTION_OF_LINE_ERROR="_errorLineEx";
    
    public static final String SUBTASK_STARTED="_subtaskStart";
    public static final String SUBTASK_ENDED="_subtaskEnd";    

    private int progress=0;
    private String statusMessage="";
    private String status=PENDING;    
    private HashSet<PropertyChangeListener> propertyChangeListeners=new HashSet<PropertyChangeListener>();
    private String taskName="task";
    private int queueNumber=0; 
    private Object executionLock=null; //
    private MotifLabClient client=null;
    private int lineNumber=0; // the line in the protocol where this executable task starts (this is static)
    private boolean retrying=false;
    protected UndoMonitor undoMonitor=null;
    private UndoableEdit protocolEdit=null;
    private ExecutableTask parentTask=null;
    private HashMap<String,Class> affected=null; // stores the names and types of data objects that will be affected in some way by this task
    private boolean guiBlocking=false; // if set to true, a modal progress dialog will be shown if the task is run in MotifLabGUI
    private boolean turnOffGuiNotifications=false; // If true, no notifications about Visualization Settings updated should be reported during execution
    
    protected HashMap<String,Object> storage=null; // used for storing additional parameters    
    
    /**
     * 
     * @param name
     */
    public ExecutableTask(String name) {
        taskName=name;
    }
    
    public void setTaskName(String name) {
        taskName=name;
    }    
    
    public String getTaskName() {
        return taskName;
    }
    
    /**
     * Sets a specific parameter to be used for execution of an Operation
     * @param key
     * @param value
     */
    public void setParameter(String key, Object value) {
        if (storage==null) storage=new HashMap<>();
        storage.put(key, value);
    }

   /**
     * Removes a specific parameter setting
     * @param key
     */
    public void removeParameter(String key) {
        if (storage!=null) storage.remove(key);
    }

    /**
     * Gets a specific parameter to be used for execution of an Operation
     * @param key
     */    
    public Object getParameter(String key) {
        return (storage!=null)?storage.get(key):null;
    }
    
    public String[] getParameters() {
        if (storage==null || storage.isEmpty()) return new String[0];
        Set<String> keys=storage.keySet();
        String[] res=new String[keys.size()];
        int i=0;
        for (String k:keys) {res[i]=k;i++;}
        return res;
        
    }
    
    /** Removes any parameter references from this task */
    public void clearParameters () {
        if (storage!=null) storage.clear();
    }    
    
    
    /** 
     * This method is called automatically by some Executor when it is time to 
     * execute the task.
     * Subclasses should override this method to do the work real work
     */
    public abstract void run() throws InterruptedException, Exception;
                          
    
    /**
     * Provides an object which can be used as an execution lock to temporarily suspend execution 
     * of a task. Running tasks should regularly check this lock by calling the method
     * {@code checkExecutionLock}. If this lock has been requested by some outside object, the execution
     * of the task (and its thread) will block until the lock is released 
     * 
     * @param lock
     */
    public void setExecutionLock(Object lock) {
        executionLock=lock;
    }
    
    /**
     * Returns the executionLock for this object
     */
    public Object getExecutionLock() {
        return executionLock;
    }
    
    /**
     * Running tasks should call this method on a regular basis to check whether some other thread has
     * requested the task executing thread to suspend temporarily.
     */
    public void checkExecutionLock() {
        if (executionLock==null) return;
        synchronized(executionLock) {} // we do not need to do anything, really. This command will block and wait for the lock to be released (unless it is already free)
    }

    /**
     * Returns a map containing the names of data objects that will be created, updated (with possible change of datatype)
     * or deleted as a result of running this task. For each affected data object the resulting class-type is also registered.
     * If the class-value is NULL, it means that the object will be deleted.
     * (Use the method addAffectedDataObject() to register affected data objects)
     * @return
     */
    public HashMap<String,Class> getAffectedDataObjects() {
        if (affected!=null) return affected;
        else return new HashMap<String, Class>();
    }

    /**
     * This method can be used to specify that a named data object will be affected by the execution 
     * of this task (either being created, updated or deleted). The resulting datatype of the object
     * must also be specified. If the datatype is set to NULL it signals that the object will be deleted.
     * @param name The name of a data object which will be affected by this task
     * @param datatype The class type this data object will have after the task has finished
     * @return
     */
    public void addAffectedDataObject(String name, Class datatype) {
        if (affected==null) affected=new HashMap<String, Class>();
        affected.put(name,datatype);
    }

    /** 
     * Returns an integer value in the range 0 to 100 that represents the progress 
     * made so far to complete this task
     * @return A value between 0 and 100
     */
    public int getProgress() {
        return progress;
    }
    
    /** 
     * Sets the progress for this task to a number between 0 and 100
     * 
     * @param i A value between 0 and 100
     */
    public void setProgress(int i) {
        int oldvalue=progress;
        if (i>100) progress=100;
        else if (i<0) progress=0;
        else progress=i;
        firePropertyChange(PROGRESS, oldvalue, progress);
    }
    
    /** 
     * Sets the progress for this task according to the relative number
     * of "subtasks" that have been completed so far
     * 
     * @param i The number of subtasks currently completed
     * @param n The total number of subtasks that must be completed
     */
    public void setProgress(int i, int n) {
        int oldvalue=progress;
        if (i>n) progress=100;
        else if (i<0) progress=0;
        else progress=(int)((i*1.0/n)*100);
        firePropertyChange(PROGRESS, oldvalue, progress);
    }
    
    public void setProgress(long i, long n) {
        int oldvalue=progress;
        if (i>n) progress=100;
        else if (i<0) progress=0;
        else progress=(int)((i*1.0/n)*100);
        firePropertyChange(PROGRESS, oldvalue, progress);
    }    

    /**
     * Sets the progress for this task according to the progress within a numbered subtask.
     * This method can thus be used to set the progress at a finer level than setProgress(i,n)
     * since the progress within the subtask can also be specified (not just how many subtasks
     * have been completed)
     *
     * @param p The progress within the current subtask (a number between 0 and 100)
     * @param i The number of the current subtask (starting at 0 at ending at n-1)
     * @param n The total number of subtasks that must be completed
     */
    public void setProgress(int p, int i, int n) {
        int oldvalue=progress;
        double subtaskSegment=100.0/(double)n;
        if (p>100) p=100;
        else if (p<0) p=0;
        if (i>=n) progress=100;
        else {
            if (i<0) i=0;
            else progress=(int)((i*subtaskSegment)+((double)p/100.0)*subtaskSegment);
        }
        firePropertyChange(PROGRESS, oldvalue, progress); // progress should be a number between 0 and 100
    }
    
    
    /**
     * Returns TRUE if the task was attempted executed before but was aborted due
     * to some error or exception and is now (by user request) attempted executed
     * again. If a task is "retrying" some of the actions performed the last time
     * might still be valid and need not be repeated from scratch.
     * @return
     */
    public boolean isRetrying() {
        return retrying;
    }
    
    /**
     * Sets the "retrying" flag for this task
     * @return
     */
    protected void setRetrying (boolean isretrying) {
        retrying=isretrying;
    }
    
    /** Returns a status message for this task execution 
     * @return a status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }
    
    /** Sets a new status message for this task execution
     * @param newstatus The new status message
     */
    public void setStatusMessage(String newstatusMessage) {
        String oldstatusMessage=statusMessage;
        statusMessage=newstatusMessage;
        firePropertyChange(STATUS_MESSAGE, oldstatusMessage, statusMessage);
    }
        
    /**
     * Sets a new status for this ExecutableTask
     * The class ExecutableTask defines static string constants that can be 
     * used to specify the status 
     * @param status
     */
     public void setStatus(String status) {
        String oldstatus=this.status;
        this.status=status;
        firePropertyChange(STATUS, oldstatus, status);
     }

    /**
     * Returns the status for this ExecutableTask
     * The class ExecutableTask defines static integer constants for different status
     * @param status
     */
     public String getStatus() {
       return status;
     }
     
     /** Returns TRUE if the status of the task is set to ABORTED */
     public boolean isAborted() {
        return ExecutableTask.ABORTED.equals(status);         
     }
     
     public void logMessage(String message, int level) {
         if (client!=null) client.logMessage(message, level);
     }

     /** 
      * This function should return true if an executing task should attempt to
      * redo tasks that have failed because of unforseen errors (for instance 
      * network timeouts) or false if the error should be propagated by throwing
      * an exception. The default implementation first tries to ask an installed
      * MotifLabClient or returns false if no MotifLabClient is installed.
      * 
      */
     public boolean shouldRetry(Exception e) {
         if (client==null) return false;
         else return client.shouldRetry(this, e);
     }
     
     /** 
      * Sets a MotifLabClient to use for this task. MotifLabClient decides whether
      * tasks should attempt to retry executions which have failed due to unforseen
      * errors (such as network timeouts). If set to null the default behaviour 
      * would be to never retry execution and instead propagate the error by
      * throwing an Exception
      */
     public void setMotifLabClient(MotifLabClient client) {
         this.client=client;
     }
     
     /** 
      * Returns the MotifLabClient which is installed for this task
      */
     public MotifLabClient getMotifLabClient() {
         return client;
     }
     
     /** 
      * Returns the MotifLabEngine which is installed for this task
      */
     public MotifLabEngine getMotifLabEngine() {
         if (client!=null) return client.getEngine(); 
         else return null;
     }     
     
     
    /** Registers a new propertyChangeListener that will receive notifications of important updates from this task (such as status and progress updates) */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.add(listener);
    }
    
    /** Removes a previously registered propertyChangeListener */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.remove(listener);
    }
    
    /** Notifies interested listeners of a change in a property value */
    public void firePropertyChange(String name, Object oldvalue, Object newvalue) {
        PropertyChangeEvent event=new PropertyChangeEvent(this, name, oldvalue, newvalue);
        for (PropertyChangeListener listener:propertyChangeListeners) {
            listener.propertyChange(event);
        }
    }

    /** Notifies interested listeners of a change in a property value */
    public void firePropertyChange(PropertyChangeEvent event) {
        for (PropertyChangeListener listener:propertyChangeListeners) {
            listener.propertyChange(event);
        }
    }    
      

     /**
     * Provides an undoMonitor to be used by this Task to trace information about 
     * updates that needs to be undone. Without such a monitor the changes made by 
     * this task cannot be undone or redone (unless monitored at a higher level)  
     * @param flag
     */
    public void setUndoMonitor(UndoMonitor monitor) {
        undoMonitor=monitor;
    }   
   
    @Override
    public boolean canRedo() {
        return (super.canRedo() && undoMonitor!=null && undoMonitor.canRedo());
    }
    
    @Override
    public boolean canUndo() {
        return (super.canUndo() && undoMonitor!=null && undoMonitor.canUndo());
    }
    
   
    @Override
    public void undo() throws CannotUndoException {
        if (undoMonitor==null || !undoMonitor.canUndo()) throw new CannotUndoException();
        super.undo();        
        undoMonitor.undo();
        if (protocolEdit!=null) {
            //System.err.println("Undoing protocol edit:"+protocolEdit.getPresentationName());
            protocolEdit.undo();
        }
    }
    
    @Override
    public void redo() throws CannotRedoException {
        if (undoMonitor==null || !undoMonitor.canRedo()) throw new CannotRedoException();
        super.redo();        
        undoMonitor.redo();
        if (protocolEdit!=null) protocolEdit.redo();
    }

    
    public void setProtocolEdit(UndoableEdit edit) {
        protocolEdit=edit;
    }

    @Override
    public void die() {
        super.die();
        if (undoMonitor!=null) undoMonitor.die();
    }
    
    @Override
    public String getPresentationName() {
        if (status.equals(ABORTED) || status.equals(ERROR)) return "aborted "+taskName;
        else return taskName;
    }

    /**
     * Sets the parent task for this task
     * The parent task could for instance be a compound task or Protocol task
     * that this task is part of
     * @return
     */
    public void setParentTask(ExecutableTask parent) {
        parentTask=parent;
    }
    
    /**
     * Returns the parent task for this task (or null if no parent exists)
     * The parent task could for instance be a compound task or Protocol task
     * that this task is part of
     * @return
     */
    public ExecutableTask getParentTask() {
        return parentTask;
    }

    /**
     * Returns the topmost parent task for this task (or null if no parent exists)
     * The parent task could for instance be a compound task or Protocol task
     * that this task is part of
     * @return
     */
    public ExecutableTask getTopParentTask() {
        if (parentTask==null) return null;
        else {
            ExecutableTask task=parentTask;
            while (task.parentTask!=null) task=parentTask.parentTask;
            return task;
        }
    }

    /**
     * Sets a queue number for this task
     * @param number
     */
    public void setQueueNumber(int number) {
        queueNumber=number;
    }
    /**
     * Returns a queueNumber for this task. The number should represents the 
     * order assigned to the task when it first arrived in a queue, not necessarily
     * its current placement.
     */
    public int getQueueNumber() {
        return queueNumber;
    }

    /**
     * Returns the (starting) line number for this operation or block in a protocol script
     * (or 0 if the operation/block is not associated with a script)
     */
    public int getLineNumber() {
        return lineNumber;
    }
           
    
    /**
     * Specifies whether this task should block any activity in the GUI while running
     * If set to TRUE, a modal progress dialog will be displayed while this task is running
     */
    public void setBlockGUI(boolean blocking) {
        guiBlocking=blocking;
    }

    /**
     */
    public boolean shouldBlockGUI() {
        return guiBlocking;
    }
    
    /**
     * Specifies whether to turn off (possibly duplicate) GUI notifications while the task is running
     * and rather wait until the task is completed to give only one collective notification. 
     * If notifications are turned off, the single notification given afterwards will also give 
     * out a stronger update signal to the GUI that substantial changes might have been made. 
     * (which is useful for tasks that make significant changes to the data, such as extending or cropping sequences).
     */
    public void setTurnOffGUInotifications(boolean turnOff) {
        turnOffGuiNotifications=turnOff;
    }

    /**
     */
    public boolean turnOffGUInotifications() {
        return turnOffGuiNotifications;
    }    
    
    /**
     * Sets a line number for this operation 
     */
    public void setLineNumber(int line) {
        lineNumber=line;
    }    
    
   /**
     * Returns a textual representation of this task's command (with parameters) to be included in protocol scripts
     */    
    public abstract String getCommandString(Protocol protocol);

    /** Tells the task to get rid of any lingering references it might have to other objects if these references are no longer needed
     */
    public void purgeReferences() {
        if (storage!=null) storage.clear();
        storage=null;
    }
    
    /** If the task is executed by the GUI client, and the method 'shouldRunPreprocessing()' returns true
     *  this method will be run on the EDT before the rest of the task is executed. 
     *  Subclassing tasks can override this method (and 'shouldRunPreprocessing') to implement special requirements
     */
    public void guiClientPreprocess(){}
    
    /** If the task is executed by the GUI client, and the method 'shouldRunPostprocessing()' returns true
     *  this method will be run on the EDT after the rest of the task has executed. 
     *  Subclassing tasks can override this method (and 'shouldRunPostprocessing') to implement special requirements
     */
    public void guiClientPostprocess(){} 
    
    /** If this method returns true (default is false) and the task is executed by the GUI client,
     *  the method 'guiClientPreprocess()' will be run on the EDT before the rest of the task is executed
     */
    public boolean shouldRunPreprocessing() {return false;}
    
    /** If this method returns true (default is false) and the task is executed by the GUI client,
     *  the method 'guiClientPostprocess()' will be run on the EDT after the rest of the task is executed
     */    
    public boolean shouldRunPostprocessing() {return false;}
    

    /**
     * Dumps information about this task (and its subtasks) to STDERR
     * @param verbosity Level of detail that should be provided (1=low, 2=medium, 3=high)
     * @param indentLevel used for nested output
     */
    public abstract void debug(int verbosity, int indentLevel);

}
