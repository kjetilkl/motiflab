/*
 
 
 */

package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.BlockingQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabClient;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.task.CompoundTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.ProtocolTask;



/**
 * This class schedules the execution of tasks that need to be sequentially executed 
 * in the order they were submitted. 
 * 
 * @author kjetikl
 */
public class GuiScheduler implements PropertyChangeListener {
    private final BlockingQueue<ExecutableTask> queue = new LinkedBlockingQueue<ExecutableTask>(); // tasks are added to this queue in the order submitted
    private ScheduledExecutor executor=null;
    private MotifLabEngine engine;
    private MotifLabGUI gui;
    private HashSet<PropertyChangeListener> propertyChangeListeners;
    private int queueNumber=0;
    private Object executionLock=new Object();
    private ModalProgressDialog progressdialog=null;
    
    
    
    /**
     * 
     */
    public GuiScheduler(MotifLabGUI gui) {
       this.gui=gui;
       this.engine=gui.getEngine();
       propertyChangeListeners = new HashSet<PropertyChangeListener>();
    }
    
    
    
    /**
     * Submits a task to be scheduled for execution as soon as possible
     * @return
     */
    public synchronized void submit(ExecutableTask task) {
        //engine.logMessage("Submitting task for execution in GuiScheduler");
        if (executor==null || !executor.isAlive()) {
            executor=new ScheduledExecutor(this);
            executor.start(); // just start it right away. It will block anyway until tasks are submitted
        }
        if (queueNumber==0) {
            queueNumber=1;
            task.setQueueNumber(1);    
        } else {
          queueNumber++;
          task.setQueueNumber(queueNumber);          
        }
        queue.offer(task);
        reportSchedulingEvent();
    }
    

    /**
     * Aborts the specified task. If the task is still pending it will simply 
     * be removed from the queue without further ado. If the task is currently being
     * executed the Executor will be asked to shut it down 
     * @param task
     * @return true if the task was currently executing and had to be aborted or
     * false otherwise 
     */
    public boolean abort(ExecutableTask task) {
         synchronized(queue) {
            if (executor==null) return false;
            ExecutableTask currentTask=executor.getCurrentTask();
            if (currentTask==null) return false;
            if (task==currentTask) {
                executor.interrupt();
                try {
                    executor.join(); // wait until the executor thread finishes
                } catch (Exception e) {}
                reportSchedulingEvent();
                return true;
            } else {
                queue.remove(task);
                reportSchedulingEvent();
                return false;
            }
        }
    }
    
    /** Aborts execution of all tasks */
    public void abort() {
       if (executor!=null) executor.abort();
       try {
            executor.join(); // wait until the executor thread finishes
       } catch (Exception e) {}
    }
    
    /** Cancels the current execution if an error/exception has made it impossible to cancel
     *  executions the normal way (i.e. using abort() ).
     *  This method is called by the GUI's default UncaughtExceptionHandler in order to clean
     *  up any executing tasks that might be "stuck"
     */
    public void abortEmergency() {
       if (executor!=null) executor.abortEmergency();
       try {
            executor.join(); // wait until the executor thread finishes
       } catch (Exception e) {}
       executor=null;
    }
    
    /**
     * Returns a lock object that can be used to temporarily suspend execution of tasks
     * by synchronizing on the lock. Note that the running task might not block right away
     * (tasks are only required to check this lock sporadically).
     */
    public Object getExecutionLock() {
        return executionLock;
    }
    
    /**
     * Returns the number of tasks in queue still waiting to be executed
     * (not counting the one currently executing)
     * @param waitForIt
     */
    public int pendingTasks() {
        return queue.size();
    }
    
    /** Returns true if the execution thread is not currently executing tasks */
    public boolean isIdle() {
        return (executor==null || executor.isIdle());
    }

    /** Returns true if the execution thread is currently executing a Protocol task */
    public boolean isRunningProtocol() {
        return (executor!=null && executor.isRunningProtocol());
    }
    
    /**
     * Returns the queue number for the task currently being executed
     * The queue number for tasks increases every time a task is added to a
     * non-empty queue but resets when the queue empties
     * @returns The queue number of the task currently executing (1+) or 0 if there is no such task
     */
    public int getCurrentTaskQueueNumber() {
        if (executor==null) return 0;
        ExecutableTask ct=executor.getCurrentTask();
        if (ct!=null) return ct.getQueueNumber();
        else return 0;
    }
    /**
     * Returns the queue number for the last task added to the "current queue"
     * The queue number for tasks increases every time a task is added to a
     * non-empty queue but resets when the queue empties
     */
    public int getLastQueueNumber() {
        return queueNumber;
    }
    
    
    /**
     * This method tries to shutdown the currently running execution thread
     * Pending tasks will be flushed
     * @param waitForIt If this is set to true the method will wait until the 
     * Executor thread is finished aborting. If false the method will return
     * right away. 
     */
    public void shutdown(boolean waitForIt) {
        if (executor==null) return;
        executor.shutdown();        
        if (waitForIt) 
        try {
            executor.join(); // wait until the executor thread finishes
        } catch (Exception e) {}
    }
    

    
    /** Registers a new propertyChangeListener which will receive notifications of important updates */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.add(listener);
    }
    
    /** Removes a previously registered propertyChangeListener */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeListeners.remove(listener);
    }

    // The scheduler registers itself as PropertyChangeListener with ScheduledTasks to receive notifications about progress
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // just propagate the event for now. Maybe I will add more functionality later
        // I should probably check here if errors have occurred?
        if (evt.getPropertyName().equals(CompoundTask.SUBTASK_STARTED)) {
            ExecutableTask task=(ExecutableTask)evt.getOldValue();
            if (task.turnOffGUInotifications()) gui.getVisualizationSettings().enableVisualizationSettingsNotifications(false);
        } else if (evt.getPropertyName().equals(CompoundTask.SUBTASK_ENDED)) {
            ExecutableTask task=(ExecutableTask)evt.getOldValue();
            if (task.turnOffGUInotifications()) reenableGUInotifications();   
        } 
        for (PropertyChangeListener listener:propertyChangeListeners) listener.propertyChange(evt);
    }

    private void reportSchedulingEvent() {
          PropertyChangeEvent event=new PropertyChangeEvent(this, ExecutableTask.SCHEDULING_EVENT, "void", "void"); // the value is not important
          propertyChange(event); // notifies listeners
    }
    
    private void reenableGUInotifications() {
        boolean enabled=gui.getVisualizationSettings().isNotificationsEnabled();        
        if (!enabled) {
            gui.getVisualizationSettings().enableVisualizationSettingsNotifications(true); // reenable                    
            gui.getVisualizationPanel().clearCachedVisualizers(); // probably safe to do this here in order to ensure that everything is updated properly
            gui.getVisualizationSettings().notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REORDERED, engine.getDefaultSequenceCollection());  
            gui.redraw();                
        }           
    }    
    
    /**
     * Returns an ordered list of tasks containing the currently executing task followed by
     * all other tasks that have been submitted and are pending execution
     * @return
     */
    public ArrayList<ExecutableTask> getTasks() {
      // it might be a bit tricky to avoid concurrency problems here, but I will give it a try
      ArrayList<ExecutableTask> tasklist=new ArrayList<ExecutableTask>();
      if (executor!=null) {
          executor.setWaitToProceed(true);
          Iterator<ExecutableTask> iterator=queue.iterator();
          while(iterator.hasNext()) {
              tasklist.add(iterator.next());
          }
          ExecutableTask currentTask=executor.getCurrentTask();
          if (currentTask!=null && !tasklist.contains(currentTask)) tasklist.add(0, currentTask);
          executor.setWaitToProceed(false);
      }
      return tasklist;
    }

    /**
     * This method returns a Map containing the names of all data objects that will
     * be affected by the execution of the currently submitted tasks (the task currently executing
     * and the ones pending execution). The Map also contains the class-types that these data objects
     * will have after all the tasks have finished executing.
     * If the class for an object is NULL it means that the data object will be deleted at some point.
     * Note that the list only contains the names of data objects that the tasks know will be affected ahead of time,
     * and so there might be some data objects that could be affected that are not mentioned.
     * This usually applies to tasks affecting many objects, where the number of objects is determined 
     * dynamically during the actual executing of the task. For instance if a task loads a MotifCollection
     * from a file, the names of the Motifs in that collection can not be known ahead of time, and the
     * individual motifs are thus not included in the returned Map, only the name of the MotifCollection itself.
     * @return
     */
    public HashMap<String,Class> getDataObjectsAffectedBySubmittedTasks() {
        HashMap<String,Class> affected=new HashMap<String,Class>();
        ArrayList<ExecutableTask> tasklist=getTasks();
        for (ExecutableTask task:tasklist) {
            HashMap<String,Class> affectedbytask=task.getAffectedDataObjects();
            if (affectedbytask!=null) affected.putAll(affectedbytask); // this could overwrite entries made by earlier tasks
        }
        return affected;
    }

    // ------------------------------------------------------------------------------------------

    /**
     * The following private class represents the worker-thread that executes tasks.
     * All it does is to remove scheduled tasks from the queue and execute them.
     * When the queue is empty the ScheduledExecutor blocks until additional tasks are added,
     * in which case the thread wakes up and continues its work.
     */
    private class ScheduledExecutor extends Thread {
        private GuiScheduler scheduler;
        private boolean exitThread=false;
        ExecutableTask task=null; // the currently executing task
        private boolean waitforit=false; // used to prevent
        
        public ScheduledExecutor(GuiScheduler scheduler) {
            super(); 
            this.scheduler=scheduler;
            setDaemon(true);
        }
        
        /** By setting this to TRUE the scheduler will not start to process any new tasks until 
         *  wait is set to FALSE again. It will, however, not halt execution of the currently running task.
         *  Note that the scheduler will not be blocked, but will rather be actively waiting. 
         *  Hence, this functionality should only be used to block the scheduler from altering the task-queue 
         *  for a short time period (e.g. while inspecting the queue with an Iterator)
         */
        public void setWaitToProceed(boolean wait) {
            waitforit=wait;
        }

        /** Cancels the currently used execution thread. (new ones can be created later !) */
        public void shutdown() {
            exitThread=true;
            this.interrupt();
        }
      
        /** Cancels the currently used execution thread. (new ones can be created later !) */
        public void abort() {          
            this.interrupt(); // 
        }
        
        /** Cancels the current execution if an error/exception has made it impossible to cancel
         *  executions the normal way (i.e. using abort() ).
         */
        public void abortEmergency() {  
            if (task!=null) {
                MotifLabClient arbiter=task.getMotifLabClient();                
                if ((!(task instanceof ProtocolTask) || arbiter.shouldRollback()) && gui.getUndoManager().canUndo()) {
                   gui.getUndoManager().undo(); // always rollback "atomic" tasks (they have probably not been executed properly anyway)
                   gui.getUndoManager().pop(); // remove the undo (after it is undone) so that it can't be redone
                }
                task.setStatus(ExecutableTask.ERROR);  // this will notify listeners, i.e. The scheduler
                task.setMotifLabClient(null);
                task.setExecutionLock(null);
                task.removePropertyChangeListener(scheduler);
                gui.updateUndoRedoStates(); 
            }                  
            task=null;
            flushQueue();     
            engine.removeTemporaryDataItems();              
            gui.reportExecutionEnded();          
        }
        
      
        /** Returns true if the execution thread is not currently executing tasks */
        public boolean isIdle() {
            return (queue.size()==0 && task==null);
        }

        /** Returns TRUE if the execution thread is currently executing a Protocol task*/
        public boolean isRunningProtocol() {
            return (task instanceof ProtocolTask);
        }
        
        public ExecutableTask getCurrentTask() {
            return task;
        }
        
        /** Retrieves tasks from the queue one by one (blocking if the queue is empty) and executes them */ 
        @Override
        public void run() {  
            boolean shouldAbort=false;
            while(!shouldAbort && !exitThread) { // infinite loop?
                if (queue.size()==0) queueNumber=0;
                while (waitforit) {try {Thread.sleep(100);} catch (Exception e) {}}
                try {
                    long executionTime=0;
                    task=queue.take(); // this will block the thread until a new task is available in the queue                 
                    gui.reportExecutionStarted();
                    task.addPropertyChangeListener(scheduler);
                    task.setExecutionLock(executionLock);
                    task.setMotifLabClient(gui);   
                    if (task.turnOffGUInotifications()) { // turn off visualization updates to avoid too many redundant updates
                        gui.getVisualizationSettings().enableVisualizationSettingsNotifications(false); // disable
                    }
                    if (task.shouldRunPreprocessing()) runPreprocessingOnEDT(task); 
                    if (task.shouldBlockGUI()) { // this is used by e.g. Extend/Crop sequences tasks
                        progressdialog=new ModalProgressDialog(gui, task);
                        progressdialog.runTask(); // this will call task.run();
                    } else {
                       progressdialog=null; 
                       executionTime=System.currentTimeMillis();
                       task.run(); // <-- THIS IS IT. HERE WE GO !!! 
                       executionTime=System.currentTimeMillis()-executionTime;
                    }                  
                    task.setMotifLabClient(null);
                    task.setExecutionLock(null);
                    task.purgeReferences(); // the task is no longer needed but it could still be referenced by UndoManagers, so keep the task but purge its data
                    task.removePropertyChangeListener(scheduler);
                    if (task instanceof ProtocolTask) gui.logMessage("Protocol execution finished in "+MotifLabEngine.formatTime(executionTime));
                    if (task.shouldRunPostprocessing()) runPostprocessingOnEDT(task); 
                    if (task.turnOffGUInotifications()) {
                        SequenceCollection defaultSequenceCollection=engine.getDefaultSequenceCollection();
                        int size=defaultSequenceCollection.size();
                        String message="Preparing sequence visualization"+((size>300)?". This can take some time...":"");
                        gui.statusMessage(message);
                        gui.setProgress(Integer.MAX_VALUE);
                        Thread.yield();
                        gui.getVisualizationPanel().clearCachedVisualizers(); // probably safe to do this here in order to ensure that everything is updated properly
                        gui.getVisualizationSettings().enableVisualizationSettingsNotifications(true); // reenable
                        gui.getVisualizationSettings().notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REORDERED, defaultSequenceCollection);
                        gui.redraw();
                        gui.setProgress(-1);
                        Thread.yield();
                    }                             
                    engine.removeTemporaryDataItems();
                    task=null; // clear before retrieving next task   
                    progressdialog=null;
                } catch (NullPointerException e) { // this should not happen
                    if (progressdialog!=null) progressdialog.setVisibilityOnEDT(false);
                    if (task.shouldRunPostprocessing()) runPostprocessingOnEDT(task);    
                    reenableGUInotifications(); // 
                    gui.debugMessage("SYSTEM ERROR: Caught unexpected NullPointerException");   
                    gui.logError(e);
                    if (task!=null) {
                        task.setStatus(ExecutableTask.ABORTED); // this will notify listeners, i.e. The scheduler
                        task.setExecutionLock(null);
                        task.removePropertyChangeListener(scheduler);
                        task.purgeReferences();
                        gui.updateUndoRedoStates(); 
                    }   
                    shouldAbort=true;                    
                    task=null;
                    flushQueue(); // flush queue
                    throw e; // throws the nullpointer Exception to be handled by the defaultExceptionHandler
                    //e.printStackTrace(System.err);
                    //break;  
                } catch (InterruptedException e) { // Execution aborted interactively by the user
                    if (progressdialog!=null) {
                        progressdialog.doAbort(); // this will undo the task and also take down the dialog
                    }
                    if (task!=null && task.shouldRunPostprocessing()) runPostprocessingOnEDT(task);                   
                    reenableGUInotifications(); // 
                     
                    if (task instanceof ProtocolTask) {
                        ((ProtocolTask)task).notifyExecutionAbortedByUser();
                        gui.logMessage("Protocol script execution aborted");
                    } 
                    else gui.logMessage("Execution aborted");
                    if (task!=null) {
                        task.setStatusMessage("Aborted!");
                        task.setStatus(ExecutableTask.ABORTED); // this will notify listeners, i.e. The scheduler
                        task.setExecutionLock(null);
                        task.removePropertyChangeListener(scheduler);
                        task.purgeReferences();
                        gui.updateUndoRedoStates(); 
                    } 
                    shouldAbort=true;
                    task=null;
                    flushQueue(); // flush queue
                    //break;  
                    
                } catch (Exception e) { // Execution aborted because of unforseen errors
                    gui.logError(e);
                    if (progressdialog!=null) progressdialog.setVisibilityOnEDT(false);
                    reenableGUInotifications(); // reenable
                    if (task!=null) {
                        MotifLabClient arbiter=task.getMotifLabClient();
                        if ((!(task instanceof ProtocolTask) || arbiter.shouldRollback()) && gui.getUndoManager().canUndo()) {
                           gui.getUndoManager().undo(); // always rollback "atomic" tasks (they have probably not been executed properly anyway)
                           gui.getUndoManager().pop(); // remove the undo (after it is undone) so that it can't be redone
                        }
                        if (task.shouldRunPostprocessing()) runPostprocessingOnEDT(task);                       
                        task.setStatus(ExecutableTask.ERROR);  // this will notify listeners, i.e. The scheduler
                        if (task instanceof ProtocolTask) {
                            ((ProtocolTask)task).notifyExecutionStoppedByError();
                        }
                        task.setMotifLabClient(null);
                        task.setExecutionLock(null);
                        task.purgeReferences();
                        task.removePropertyChangeListener(scheduler);
                        gui.updateUndoRedoStates(); 
                    } 
                    if (!(e instanceof ExecutionError || e instanceof SystemError || e instanceof ParseError)) e.printStackTrace(System.err);
                    shouldAbort=true;                    
                    task=null;
                    flushQueue();     
                    gui.getProtocolEditor().setException(e);
                } finally  {
                    reenableGUInotifications(); // 
                    engine.removeTemporaryDataItems();
                    gui.reportExecutionEnded();
                }
            }
            flushQueue(); // flushes the remaining scheduled tasks upon abortion/errors.
            //gui.debugMessage("ScheduledExecutor thread finished");
            task=null;
            queueNumber=0;
        } // --- end run() ---
      
        
        private void flushQueue() {
            queue.clear();
            queueNumber=0;
        }
        
                
        private void runPreprocessingOnEDT(final ExecutableTask task) {
            Runnable runner=new Runnable() {
                @Override
                public void run() {
                    task.guiClientPreprocess();
                }
            };
            try {
                SwingUtilities.invokeAndWait(runner);
            } catch (Exception e) {gui.logMessage(e.getMessage(),30);}
        }
        
        private void runPostprocessingOnEDT(final ExecutableTask task) {
            Runnable runner=new Runnable() {
                @Override
                public void run() {
                    task.guiClientPostprocess();
                }
            };
            SwingUtilities.invokeLater(runner);
        }       
        
    } // ******** end class ScheduledExecutor **********
    
    private class ModalProgressDialog extends JDialog implements PropertyChangeListener {
        JProgressBar progressbar;
        JButton cancelButton;
        ExecutableTask task;
        
        public ModalProgressDialog(MotifLabGUI gui, final ExecutableTask task) {
            super(gui.getFrame(),task.getPresentationName(),true);
            this.task=task;
            setLayout(new BorderLayout());
            JPanel top=new JPanel(new FlowLayout());
            JPanel bottom=new JPanel(new FlowLayout());
            this.add(top,BorderLayout.NORTH);
            this.add(bottom,BorderLayout.SOUTH);
            progressbar=new JProgressBar(0, 100); 
            top.add(progressbar);
            cancelButton=new JButton("Cancel");
            bottom.add(cancelButton);
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    task.setStatus(ExecutableTask.ABORTED);
                }
            });
            pack();
//            Dimension dim=new Dimension(300,120);
//            this.setMinimumSize(dim);
//            this.setMaximumSize(dim);
//            this.setPreferredSize(dim);
//            this.setSize(dim);
            this.setResizable(false);
        }
        
        public void runTask() throws Exception {
           this.setLocation(getOwner().getWidth()/2-this.getWidth()/2, getOwner().getHeight()/2-this.getHeight()/2);            
           task.addPropertyChangeListener(this);
           this.setVisibilityOnEDT(true);
           task.run(); 
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {          
           if (evt.getPropertyName().equals(ExecutableTask.PROGRESS)) {
               int progressValue=(Integer)evt.getNewValue();
               setProgressOnEDT(progressValue);
               if (progressValue>=100) setVisibilityOnEDT(false); // this concludes our work               
           }
           else if (evt.getPropertyName().equals(ExecutableTask.STATUS)) {
               String status=(String)evt.getNewValue();
               // if (status.equals(ExecutableTask.ABORTED)) doAbort();
               if (status.equals(ExecutableTask.DONE)) setVisibilityOnEDT(false); // this concludes our work     
           }        
        }   
        
        public void setVisibilityOnEDT(final boolean visible) {
            Runnable runner=new Runnable() {
                @Override
                public void run() {
                    ModalProgressDialog.this.setVisible(visible);
                }
            };
             SwingUtilities.invokeLater(runner);
        }
        private void setProgressOnEDT(final int progress) {
            Runnable runner=new Runnable() {
                @Override
                public void run() {
                    if (progress<0) progressbar.setIndeterminate(true);
                    else progressbar.setValue(progress);                   
                    progressbar.repaint();
                }
            };
             SwingUtilities.invokeLater(runner);
        }   
        
        private void doAbort() {
            setProgressOnEDT(-1);
            if (gui.getUndoManager().canUndo()) {
               gui.getUndoManager().undo(); // 
               gui.getUndoManager().pop(); //
            }
            setVisibilityOnEDT(false);
        }
        
    } // end class ModalProgressDialog   

}
