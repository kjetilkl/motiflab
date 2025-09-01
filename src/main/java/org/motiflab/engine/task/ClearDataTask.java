/*
 
 
 */

package org.motiflab.engine.task;

import java.util.ArrayList;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataGroup;
import org.motiflab.engine.data.DataMap;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.ModulePartition;
import org.motiflab.engine.data.ModuleTextMap;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.MotifTextMap;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.data.SequenceTextMap;
import org.motiflab.engine.protocol.Protocol;

/**
 *
 * @author kjetikl
 */
public class ClearDataTask extends ExecutableTask {
    private boolean clearSequences=false;
    private boolean clearFeatures=false;
    private boolean clearModules=false;
    private boolean clearMotifs=false;
    private boolean clearOther=false;    
    private boolean clearSequenceRelated=false;  // sequence collections, partitions, maps and Expression Profiles      
    private boolean clearCache=false;
    
    private MotifLabEngine engine;
    private ArrayList<String> names; // name of data objects to be deleted

    public ClearDataTask(MotifLabEngine engine, String what) {
        super(what);
        this.engine=engine;
        this.setBlockGUI(true);
        this.setTurnOffGUInotifications(true);        
        clearMotifs=(what.equals("Clear All Data") || what.equals("Clear Motifs And Modules Data"));
        clearModules=(what.equals("Clear All Data") || what.equals("Clear Modules Data") || what.equals("Clear Motifs And Modules Data"));
        clearSequences=(what.equals("Clear All Data") || what.equals("Clear Sequence Data"));
        clearSequenceRelated=(what.equals("Clear All Data") || what.equals("Clear Sequence Data") || what.equals("Clear Sequence Related Data"));
        clearFeatures=(what.equals("Clear All Data") || what.equals("Clear Feature Data") || what.equals("Clear Sequence Data"));
        clearOther=(what.equals("Clear All Data") || what.equals("Clear Other Data"));
        clearCache=(what.equalsIgnoreCase("Clear Cache") || what.equalsIgnoreCase("Cache"));
    }

    @Override
    public void run() throws InterruptedException, Exception {
        boolean done=false;
        setProgress(1);
        setStatus(RUNNING);
        if (undoMonitor!=null) {
            undoMonitor.register();
            if (clearSequences) undoMonitor.storeSequenceOrder(); // the ensures the sequence order will be restored again on undo            
        }       
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

    public void execute() throws InterruptedException, ExecutionError {
        this.setProgress(2);      
        setStatusMessage("Deleting data"); 
        names=new ArrayList<String>();
        setupDeletionList();             
        int size=names.size();
        for (int i=0;i<names.size();i++) {
            engine.removeDataItem(names.get(i));
            this.setProgress(i,size);
            if (i%50==0) {
                checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                               
            }
            if (i%100==0) Thread.yield(); 
        }
        checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        Thread.yield();                
        this.setProgress(100);
        this.setStatus(DONE);
        if (clearSequences) engine.getClient().getVisualizationSettings().clearSelectionWindows();
        if (clearCache) {
            engine.getDataLoader().clearCache();
        }
    }
    

    @Override
    public String getCommandString(Protocol protocol) {
        return "#delete";
    }

    @Override
    public void purgeReferences() {
        //
    }
    
    private void setupDeletionList() {
       if (clearOther) clearOtherData();
       Thread.yield();            
       if (clearMotifs) clearMotifAndModuleDataData(false);         
       else if (clearModules) clearMotifAndModuleDataData(true);
       Thread.yield();            
       if (clearFeatures) clearFeatureData();    
       Thread.yield();            
       if (clearSequenceRelated) clearAllSequenceRelated();         
       Thread.yield();            
       if (clearSequences) clearAllSequences();    
       Thread.yield();     
    }
    
      
    
    private void clearMotifAndModuleDataData(boolean justModules) {     
        ArrayList<Data> list=null;
        list=engine.getAllDataItemsOfType(ModuleNumericMap.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=engine.getAllDataItemsOfType(ModuleTextMap.class);
        for (Data item:list) {
            names.add(item.getName());
        }        
        list=engine.getAllDataItemsOfType(ModuleCollection.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=engine.getAllDataItemsOfType(ModulePartition.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=engine.getAllDataItemsOfType(ModuleCRM.class);
        for (Data item:list) {
            names.add(item.getName()); 
        }
        if (!justModules) {
            list=engine.getAllDataItemsOfType(MotifNumericMap.class);
            for (Data item:list) {
                names.add(item.getName());
            }        
            list=engine.getAllDataItemsOfType(MotifTextMap.class);
            for (Data item:list) {
                names.add(item.getName());
            }              
            list=engine.getAllDataItemsOfType(MotifCollection.class);
            for (Data item:list) {
                names.add(item.getName()); 
            }
            list=engine.getAllDataItemsOfType(MotifPartition.class);
            for (Data item:list) {
                names.add(item.getName());
            }
            list=engine.getAllDataItemsOfType(Motif.class);
            for (Data item:list) { 
                names.add(item.getName());
            }            
        }      
    }
    
    
    private void clearOtherData() { // clear data that is not related to sequences, motifs or modules
        ArrayList<Data> list=engine.getAllDataItemsOfType(Data.class);
        for (Data item:list) { // First add all non-Sequence data items
              if (    item instanceof Sequence 
                   || item instanceof Motif
                   || item instanceof ModuleCRM
                   || item instanceof FeatureDataset
                   || item instanceof DataGroup  // covers all collections and partitions
                   || item instanceof DataMap    // covers all numeric and text maps
                   || item instanceof ExpressionProfile
//                   || item instanceof ModuleCollection
//                   || item instanceof ModulePartition
//                   || item instanceof ModuleNumericMap
//                   || item instanceof MotifCollection
//                   || item instanceof MotifPartition
//                   || item instanceof MotifNumericMap
//                   || item instanceof SequenceCollection
//                   || item instanceof SequencePartition
//                   || item instanceof SequenceNumericMap
              ) continue;            
              else names.add(item.getName());
        }        
    }
    
    
    
    /**
     * Removes all data items related to or referring to sequences such as Expression Profiles, Sequence Maps, Sequence Partitions and Sequence Collections (except the default Sequence Collection)
     */
    private void clearAllSequenceRelated() {
        ArrayList<Data> list=engine.getAllDataItemsOfType(SequenceCollection.class);
        for (Data item:list) {
            if (item!=engine.getDefaultSequenceCollection()) names.add(item.getName()); // note that the default sequence collection should never be 'removed' 
        }
        list=engine.getAllDataItemsOfType(SequencePartition.class);
        for (Data item:list) { 
            names.add(item.getName()); 
        }
        list=engine.getAllDataItemsOfType(ExpressionProfile.class);
        for (Data item:list) { 
            names.add(item.getName());
        }
        list=engine.getAllDataItemsOfType(SequenceNumericMap.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=engine.getAllDataItemsOfType(SequenceTextMap.class);
        for (Data item:list) { 
            names.add(item.getName());
        }                    
    }    
    
    private void clearAllSequences() {
        ArrayList<Data> list=engine.getAllDataItemsOfType(Sequence.class);
        for (Data item:list) {
            names.add(item.getName());
        }              
    }        
    /**
     * Removes all data items related to or referring to sequences
     * Including Sequences, FeatureData, Sequence Collections, Sequence Partitions and Expression Profiles from engine (except the default Sequence Collection)
     */
    private void clearFeatureData() {
        ArrayList<Data> list=engine.getAllDataItemsOfType(FeatureDataset.class);
        for (Data item:list) { // First add all non-Sequence data items
            names.add(item.getName()); 
        }            
    }     
    
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[ClearDataTask] ===== "+getTaskName()+" =====  (Line: "+getLineNumber()+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);
        if (verbosity>=3) {
            org.motiflab.engine.protocol.StandardProtocol protocol=new org.motiflab.engine.protocol.StandardProtocol(MotifLabEngine.getEngine());
            MotifLabEngine.debugOutput(" Command: "+getCommandString(protocol),indentLevel);
        }
        if (verbosity>1) MotifLabEngine.debugOutput("-------------------------------------------[End ClearDataTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }      
    
  
}
