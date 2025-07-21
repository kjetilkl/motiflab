/*
 
 
 */

package org.motiflab.engine.task;

import java.util.ArrayList;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.protocol.Protocol;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class ExtendSequencesTask extends OperationTask {
    
    private MotifLabEngine engine;
    private Object extendStartObject=0;
    private Object extendEndObject=0;
    private boolean useRelativeOrientation=false;

    public ExtendSequencesTask(MotifLabEngine engine, Object extendStart, Object extendEnd, boolean relativeorientation) {
        super("Extend sequences");
        this.engine=engine;
        this.extendStartObject=extendStart;
        this.extendEndObject=extendEnd;
        this.useRelativeOrientation=relativeorientation;
        
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
                ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
                ArrayList<Sequence> clonedsequences=new ArrayList<Sequence>(sequences.size());
                for (int i=0;i<sequences.size();i++) {
                   clonedsequences.add((Sequence)sequences.get(i).clone()); 
                }
                int extended=0;
                int i=0;
                for (Data data:clonedsequences) {  
                    setProgress(i,sequences.size()*2); // *2 is so that this part will end at 50%
                    i++;
                    Sequence sequence=(Sequence)data;
                    int[] newGenomicRange=getNewGenomicCoordinates(sequence);
                    if (newGenomicRange==null) {
                        engine.logMessage("Unable to extend "+sequence.getName());                        
                        continue;
                    }
                    if (newGenomicRange[0]==sequence.getRegionStart() && newGenomicRange[1]==sequence.getRegionEnd()) continue; // same location as before. Do nothing
                    sequence.setRegionStart(newGenomicRange[0]);
                    sequence.setRegionEnd(newGenomicRange[1]);
                    extended++;                                                 
                }    
                int totaldata=sequences.size();
                int counter=0;
                if (extended>0) {
                    for (i=0;i<clonedsequences.size();i++) {
                        if (i%100==0) {
                            checkExecutionLock(); // checks to see if this task should suspend execution
                            if (Thread.interrupted() || getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            Thread.yield();
                            setProgress(50+(int)((double)counter/(double)totaldata*50.0));
                        }
                        engine.updateDataItem(clonedsequences.get(i));
                        counter++;
                    }   
                } // end: if (extended>0)
        }  catch (ClassCastException c) {
            throw new ExecutionError(c.getMessage());
        } catch (ExecutionError e) {
            throw e;
        }
    }

    @Override
    public String getCommandString(Protocol protocol) {
       if (protocol==null) return null;
       return protocol.getCommandString(this);
    }

    @Override
    public void purgeReferences() {
        super.purgeReferences();
        extendStartObject=null;
        extendEndObject=null;        
    }

    private int[] getNewGenomicCoordinates(Sequence sequence) {
        int[] result=new int[]{sequence.getRegionStart(),sequence.getRegionEnd()};
            int extendStart=0;
            int extendEnd=0;
            if (extendStartObject instanceof SequenceNumericMap) extendStart=((SequenceNumericMap)extendStartObject).getValue(sequence.getSequenceName()).intValue();
            else if (extendStartObject instanceof NumericVariable) extendStart=((NumericVariable)extendStartObject).getValue().intValue();
            else if (extendStartObject instanceof Number) extendStart=((Number)extendStartObject).intValue();
            if (extendEndObject instanceof SequenceNumericMap) extendEnd=((SequenceNumericMap)extendEndObject).getValue(sequence.getSequenceName()).intValue();
            else if (extendEndObject instanceof NumericVariable) extendEnd=((NumericVariable)extendEndObject).getValue().intValue();
            else if (extendEndObject instanceof Number) extendEnd=((Number)extendEndObject).intValue();
            
            if (extendStart<0) extendStart=0;
            if (extendEnd<0) extendEnd=0;
            if (useRelativeOrientation && sequence.getStrandOrientation()==Sequence.REVERSE) {
               result[0]=result[0]-extendEnd;
               result[1]=result[1]+extendStart;                
            } else { // Direct
               result[0]=result[0]-extendStart;
               result[1]=result[1]+extendEnd;
            }
            return result;        
    }    
    
    @Override
    public boolean shouldBlockGUI() {
        return true;
    }
    
    @Override
    public boolean turnOffGUInotifications() {
        return true;
    }    
    
    // -- The two methods below are actually not used anywhere since I don't override
    //    shouldRunPreprocessing() and shouldRunPostprocessing to return TRUE.
    //    But it seems that the visualization works just fine anyway
    
    @Override
    public void guiClientPreprocess(){
        ArrayList<String> names=engine.getNamesForAllDataItemsOfType(Sequence.class);
        String[] nameslist=new String[names.size()];
        nameslist=names.toArray(nameslist);      
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();                 
        settings.setSequenceVisible(nameslist, false);         
    }
    
    @Override
    public void guiClientPostprocess(){
        ArrayList<String> names=engine.getNamesForAllDataItemsOfType(Sequence.class);
        String[] nameslist=new String[names.size()];
        nameslist=names.toArray(nameslist);      
        ((MotifLabGUI)engine.getClient()).getVisualizationPanel().clearCachedVisualizers();
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();                 
        settings.setSequenceVisible(nameslist, true);          
    }    
    
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[ExtendSequencesTask] ===== "+getTaskName()+" =====  (Line: "+getLineNumber()+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);
        if (verbosity>=3) {
            org.motiflab.engine.protocol.StandardProtocol protocol=new org.motiflab.engine.protocol.StandardProtocol(MotifLabEngine.getEngine());
            MotifLabEngine.debugOutput(" Command: "+getCommandString(protocol),indentLevel);
        }
        if (verbosity>1) MotifLabEngine.debugOutput("-------------------------------------------[End ExtendSequencesTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    } 
    
}
