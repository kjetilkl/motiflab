/*
 
 
 */

package org.motiflab.engine.task;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.protocol.Protocol;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.operations.Condition_basic;
import org.motiflab.engine.protocol.OperationParser;
import org.motiflab.engine.protocol.StandardProtocol;

/**
 * This class represents an conditional task that can execute differently depending on conditions
 * 
 * 
 * @author kjetikl
 */
public class ConditionalTask extends ExecutableTask implements PropertyChangeListener {

    protected ArrayList<Object[]> tasklist; // each element is a pair consisting of a condition (Condition_basic) and an Executable task (actually a CompoundTask)
    
    public ConditionalTask(String taskName) {
        super(taskName);
        tasklist=new ArrayList<Object[]>();
    }
        
    /**
     * Adds a new conditional subtask to this compound. New tasks can only be added before the compound task has started execution 
     * Each new task should represent an "if", "else if" or "else" block and only the first task in the list whose condition is satisfied will be executed.
     * if the condition is NULL it will be considered trivially satisfied and the corresponding task will always be executed (unless a previous task has been). This thus represents an "else" block
     * @return true if the task was successfully added or false if not
     */
    public boolean addConditionalTask(Condition_basic condition, ExecutableTask task) {
        if (getStatus().equals(PENDING) && task!=null) {
            boolean ok=tasklist.add(new Object[]{condition,task});
            if (ok) task.setParentTask(this);
            return ok;
        } else return false;
    }
    
    /** Adds a new executable task to the last condition block (provided that this is a CompoundTask) */
    public boolean addTaskToLastConditionBlock(ExecutableTask task) {
        if (tasklist!=null && !tasklist.isEmpty()) {
            Object lastTask=tasklist.get(tasklist.size()-1)[1];
            if (lastTask instanceof CompoundTask) {
                boolean ok=((CompoundTask)lastTask).addTask(task);
                return ok;
            }
        } 
        return false;   
    }
    
    /** Returns true if this ConditionalTask has a subtask with empty condition (corresponding to an else-clause) */
    public boolean hasEmptyCondition() {
        if (tasklist==null || tasklist.isEmpty()) return false;
        for (Object[] sub:tasklist) {
            if (sub[0]==null) return true;
        }
        return false;
    }
    
    @Override
    public HashMap<String,Class> getAffectedDataObjects() {
        HashMap<String,Class> allAffected=new HashMap<String, Class>();
        HashMap<String,Class> thisAffected=super.getAffectedDataObjects();
        if (thisAffected!=null) allAffected.putAll(thisAffected);
        for (Object[] sub:tasklist) {
           ExecutableTask subtask=(ExecutableTask)sub[1];
           HashMap<String,Class> subtaskAffected=subtask.getAffectedDataObjects();
           if (subtaskAffected!=null) allAffected.putAll(subtaskAffected); // this could overwrite results by earlier tasks in the list
        }
        return allAffected;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        /* The CompoundTask registers and listens for propertyChanges reported by subtasks while they are running 
         * so that these updates can be reflected in the parent (and propagated to listeners registered in this task) 
         */
             if (evt.getPropertyName().equals(STATUS_MESSAGE)) setStatusMessage((String)evt.getNewValue());
        else if (evt.getPropertyName().equals(STATUS)) {
            if (((String)evt.getNewValue()).equals(ABORTED) && !getStatus().equals(ABORTED)) setStatus(ABORTED); // setStatus will also propage to children!
            if (((String)evt.getNewValue()).equals(WAITING) && !getStatus().equals(WAITING)) setStatus(WAITING);
        }
        else if (evt.getPropertyName().equals(PROGRESS)) {
            setProgress(((Integer)evt.getNewValue()).intValue()); // propagate directly from the running (sub)task (which can be a CompoundTask)           
        } else if (evt.getPropertyName().equals(FINISHED_EXECUTION) || evt.getPropertyName().equals(STARTED_EXECUTION)) { // all other propertyChanges reported by subtasks are propagated directly to listener of this task
           // these notifications from subtasks will just be consumed here
        } else if (evt.getPropertyName().equals(STARTED_EXECUTION_OF_LINE) || evt.getPropertyName().equals(FINISHED_EXECUTION_OF_LINE)) { //
            int line=(Integer)evt.getNewValue();
            if (line==getLineNumber()) return; // this means that the first if-block (CompoundTask) is executed, but this has the same line number as the whole ConditionalTask so we will not report this twice (The ConditionalTask is already handled by its parent task)          
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
            for (Object[] sub:tasklist) {
                ExecutableTask subtask=(ExecutableTask)sub[1];
                subtask.setStatus(status);
            }
        }
     }    
    
    @Override
    public void run() throws InterruptedException, Exception { // this method will throw all exception back to the caller since none are caught here
        setStatus(RUNNING); 
        setProgress(0);
        if (undoMonitor!=null) undoMonitor.register();
        int size=tasklist.size();
        ExecutableTask runTask=null; // the task that satisfied the condition and should be run
        try {
            if (Thread.interrupted() || isAborted()) throw new InterruptedException();               
            checkExecutionLock();             
            for (int i=0;i<size;i++) { // check each of the tasks to see which one (if any) should be executed          
                Object[] subtask=tasklist.get(i);
                Condition_basic condition=(Condition_basic)subtask[0];
                if (condition==null || condition.isConditionSatisfied(ConditionalTask.this)) { //
                    runTask=(ExecutableTask)subtask[1];
                    break; // only execute the first task whose condition is satisfied
                }
            }   
            if (runTask!=null) {
                runTask.addPropertyChangeListener(this);
                runTask.setExecutionLock(getExecutionLock());
                runTask.setMotifLabClient(getMotifLabClient());
                notifySubTaskStarted(runTask); 
                runTask.run();
                notifySubTaskFinished(runTask, true); // last boolean value is exit status           
                runTask.setMotifLabClient(null);
                runTask.setExecutionLock(null);
                runTask.removePropertyChangeListener(this);   
                runTask=null;
            }
//        } catch (Exception e) { // other errors
//              if (shouldRetry(e)) {   // this will notify the user               
//                  setRetrying(true);
//              } else {
//                  setStatus(ERROR);
//                  throw e;
//              }        
        } finally { // uncaught exceptions are thrown back to the caller, but first we clean up!
            if (undoMonitor!=null) undoMonitor.deregister(true); // true=> register final state to allow redo of successfully executed subtasks
            if (runTask!=null) {
                notifySubTaskFinished(runTask, false); // last boolean value is exit status                  
                runTask.setMotifLabClient(null);
                runTask.removePropertyChangeListener(this); 
                runTask.setExecutionLock(null);
            }
        }   
        setProgress(100);
        setStatus(DONE);
    }


    private void notifySubTaskStarted(ExecutableTask subtask) {
        // Note: The execution of the line number of this ConditionalTask has already been reported by its parent task. So to avoid reporting this line twice we check if it is the same
        if (getLineNumber()!=subtask.getLineNumber()) firePropertyChange(STARTED_EXECUTION_OF_LINE, getTaskName(), subtask.getLineNumber()); 
        firePropertyChange(SUBTASK_STARTED, subtask, true);
    } 
    
    private void notifySubTaskFinished(ExecutableTask subtask, boolean finished) {
        // Note: The execution of the line number of this ConditionalTask has already been reported by its parent task. So to avoid reporting this line twice we check if it is the same        
        if (finished && getLineNumber()!=subtask.getLineNumber()) firePropertyChange(FINISHED_EXECUTION_OF_LINE, getTaskName(), subtask.getLineNumber()); 
        firePropertyChange(SUBTASK_ENDED, subtask, finished);        
    }    
      
    
    @Override
    public String getCommandString(Protocol protocol) { // this would return a multi-line string...
        if (protocol==null || tasklist==null || tasklist.isEmpty()) return null;
        else {
            StringBuilder builder=new StringBuilder();
            for (int i=0;i<tasklist.size();i++) {
                Object[] sub=tasklist.get(i);
                Condition_basic condition=(Condition_basic)sub[0];
                ExecutableTask task=(ExecutableTask)sub[1];
                if (condition!=null) {
                    if (i==0) builder.append("if "); else builder.append("else if ");
                    if (protocol instanceof org.motiflab.engine.protocol.StandardProtocol)
                    builder.append(getConditionString(protocol, condition));
                    builder.append("\n");
                } else {
                    builder.append("else\n");
                }
                builder.append(protocol.getCommandString(task));                
            }
            builder.append("end if"); // no ending newline here
            return builder.toString();
        } 
    }    
    
    private String getConditionString(Protocol protocol, Condition_basic condition) {
        if (!(protocol instanceof StandardProtocol)) return "<ERROR: Unable to format condition>";
        OperationParser parser=protocol.getOperationParser("filter");
        if (!(parser instanceof org.motiflab.engine.protocol.StandardOperationParser)) return "<ERROR: Unable to format condition>";
        return ((org.motiflab.engine.protocol.StandardOperationParser)parser).getCommandString_condition(condition);        
    }
    
    @Override
    public void purgeReferences() {
        for (Object[] sub:tasklist) {
            ExecutableTask task=(ExecutableTask)sub[1];
            task.purgeReferences();
        }
    }
      
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[ConditionalTask] ===== "+getTaskName()+" =====  (Size: "+((tasklist!=null)?tasklist.size():0)+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);        
        if (tasklist!=null) {
            int i=0;
            for (Object[] subtask:tasklist) {
                String condition=(String)subtask[0];
                ExecutableTask task=(ExecutableTask)subtask[1];
                if (condition!=null && i==0) MotifLabEngine.debugOutput("  IF ("+condition+") :",indentLevel);
                else if (condition!=null) MotifLabEngine.debugOutput("  ELSE IF ("+condition+") :",indentLevel);
                else  MotifLabEngine.debugOutput("  ELSE :",indentLevel);                
                task.debug(verbosity, indentLevel+1);
                i++;
            }        
        }        
        MotifLabEngine.debugOutput("-------------------------------------------[End ConditionalTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }      
    
}


