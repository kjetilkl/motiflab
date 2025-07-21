/*
 
 
 */

package org.motiflab.engine.task;

import org.motiflab.engine.task.ExecutableTask;
import java.util.ArrayList;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.protocol.Protocol;

/**
 *
 * @author kjetikl
 */
public class UpdateMotifsTask extends ExecutableTask {
    
    private ArrayList<Motif> changeList;
    private MotifLabEngine engine;

    public UpdateMotifsTask(MotifLabEngine engine) {
        super("Update motifs");
        this.engine=engine;
        changeList=new ArrayList<Motif>();
    }


    public UpdateMotifsTask(MotifLabEngine engine,ArrayList<Motif> updates) {
        super("Update motifs");
        this.engine=engine;
        changeList=updates;
    }

    /** adds a motif */
    public void addMotif(Motif motif) {
        changeList.add(motif);
    }


    @Override
    public void run() throws InterruptedException, Exception {
        boolean done=false;
        setProgress(1);
        setStatus(RUNNING);
        if (undoMonitor!=null) undoMonitor.register();
        while (!done){
          try {
            execute();
            done=true;
          } catch (InterruptedException e) { // task aborted by the user
              if (undoMonitor!=null) undoMonitor.deregister(false);
              setStatus(ABORTED);
              throw e;
          } catch (Exception e) { // other errors
              if (undoMonitor!=null) undoMonitor.deregister(false);
              setStatus(ERROR);
              throw e;
          }
        }
        setProgress(100);
        if (undoMonitor!=null) undoMonitor.deregister(true);
        setStatus(DONE);
        setStatusMessage(null);
    }

    private void execute() throws InterruptedException, ExecutionError {
        try {
            for (int i=0;i<changeList.size();i++) {
                if (i%20==0) {
                    checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                    Thread.yield();
                    setStatusMessage("Updating motifs ("+(i+1)+"/"+changeList.size()+")");
                    setProgress(i+1, changeList.size());
                }
                Motif motif=changeList.get(i);
                engine.updateDataItem(motif);
            }
        }  catch (ClassCastException c) {
            throw new ExecutionError(c.getMessage());
        } catch (ExecutionError e) {
            throw e;
        }
    }

    @Override
    public String getCommandString(Protocol protocol) {
        return "#update";
    }

    @Override
    public void purgeReferences() {
        changeList.clear();
        changeList=null;
    }
    
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[UpdateMotifsTask] ===== "+getTaskName()+" =====  (Line: "+getLineNumber()+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage()+",  Number of motifs: "+((changeList!=null)?changeList.size():0),indentLevel);
        if (verbosity>=3) {
            org.motiflab.engine.protocol.StandardProtocol protocol=new org.motiflab.engine.protocol.StandardProtocol(MotifLabEngine.getEngine());
            MotifLabEngine.debugOutput(" Command: "+getCommandString(protocol),indentLevel);
        }
        if (verbosity>1) MotifLabEngine.debugOutput("-------------------------------------------[End UpdateMotifsTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }    

}
