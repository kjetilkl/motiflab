package org.motiflab.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.motiflab.engine.MotifLabEngine;
import java.util.HashMap;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEdit;
import org.motiflab.engine.DataListener;
import org.motiflab.engine.data.*;
import org.motiflab.engine.data.analysis.Analysis;

/**
 * UndoMonitors are used by the GUI to monitor executions of operations and scripts.
 * They register as DataListeners for the duration of a task execution and keep
 * track of which data items that change and how (if they are updated, deleted or created anew)
 * For data items that update during execution the UndoMonitor is responsible for caching 
 * the original data as it was before the execution started so that all the changes that has
 * been done to it can later be undone (since only the original state is stored there is no
 * need for step-wise rollback to undo multiple operations that have affected the same data).
 * The UndoMonitors should also cache the resulting data after successful execution to facilitate 
 * efficient REDO functionality after UNDO (rather than executing the whole script again).
 * 
 * The UNDO/REDO information is stored in two HashMaps. Any object that is affected during
 * execution should be registered by name in these maps (at least in the originalData map).
 * 
 * 
 * @author kjetikl
 */
public class UndoMonitor implements DataListener, UndoableEdit {
    protected GUIUndoManager undoManager=null; // provides functionality for caching UNDO-data
    
    protected MotifLabEngine engine=null;
    private HashMap<String,UndoInformation> originalData=new HashMap<String, UndoInformation>(); // stores UNDO information
    private HashMap<String,UndoInformation> finalData=new HashMap<String, UndoInformation>();  // stores REDO information
    protected boolean redoInformationStored=false;
    protected boolean isDone=false;
    protected String presentationName;    
    private String sequenceOrderTicket=null; // if set. this will point to a GUIUndoManager ticket with an ArrayList<String> holding a sequence order which will be restored on rollback
    private static DataSorter sorter=new DataSorter();

    
    /**
     * Creates a new UndoManager
     */        
    public UndoMonitor(GUIUndoManager undoManager, String presentationName) {
        this.undoManager=undoManager;
        this.presentationName=presentationName;
        this.engine=undoManager.getEngine();
    }
    
    /**
     * Registers the UndoMonitor with the engine. Once the monitor is registered
     * it will listen for data updates and track information needed to undo 
     * all changes until it is deregistered.
     */
    public void register() {
        cleanup();        
        engine.addDataListener(this); // Commented out for testing purposes
    }
    
    /**
     * Deregisters the UndoMonitor with the engine. Once the monitor is deregistered,
     * it will no longer listen for data updates. If the execution of the operation/script
     * was a success, the boolean argument should be set to <tt>true</tt> to signal that
     * the final state resulting from the operation should be stored to facilitate efficient
     * REDO functionality if the user ever chooses to UNDO and then REDO (caching this data
     * will perhaps be more efficient than executing the whole script anew). 
     * However, if the execution was aborted by the user or otherwise stopped because of 
     * unforseen errors there will be an automatic rollback. In this case there will be
     * no reason to REDO the same execution again; the final state needs not to be 
     * stored and the argument should be set to <tt>false</tt>
     * @param storeFinalState set this to <tt>true</tt> if the execution of the script was
     * successful and the final state should be cached to facilitate speedy REDO functionality later.
     */
    public void deregister(boolean storeFinalState) {
        // System.err.println("UndoMonitor DEregistered => "+presentationName+"  EDT="+javax.swing.SwingUtilities.isEventDispatchThread());
        engine.removeDataListener(this);
        isDone=true;
        if (storeFinalState) {
            // go through the originalData HashMap to see which data items have been updated, then store the final results of these
            Set<String> updatedDataitems = originalData.keySet();
            for (String name:updatedDataitems) {
               //engine.logMessage("Deregister: Storing REDO info about:" +name);
               Data data=engine.getDataItem(name);
               if (data==null) { // data has been removed (deleted by the task)
                   Class dataclass=originalData.get(name).getDataClass(); // get dataclass from the original entry
                   finalData.put(name, new UndoInformation(dataclass));  
               } 
               else if (shouldBeBundled(data)) {
                   finalData.put(name, new UndoInformation(data.clone()));  
               } else {
                   String undoID=undoManager.getUniqueUndoID();
                   if (!undoManager.storeObjectInCache(undoID, data)) engine.logMessage("Problems occurred while caching data for " +name);;
                   finalData.put(name, new UndoInformation(undoID,data.getClass())); 
               }               
            }
            if (mapContainsBundledTypes(finalData)) { // bundle up all motifs, modules and sequences in a new HashMap (unless they have NULL values)
               String bundledRedoID=undoManager.getUniqueUndoID();
               HashMap<String, Data> redoWrapper=new HashMap<String,Data>();
               for (String dataname:finalData.keySet()) {
                    UndoInformation info=finalData.get(dataname);
                    if (shouldBeBundled(info.getDataClass()) && !info.isEmpty()) {
                        redoWrapper.put(dataname, info.getStoredData()); // the data should already be a clone
                        info.replaceDataWithTicket(bundledRedoID);
                    } 
                }
                if (!undoManager.storeObjectInCache(bundledRedoID, redoWrapper, false)) engine.logMessage("Problems occurred while storing REDO information");                        
            }              
            redoInformationStored=true;
        }
        
        // now store undo information for bundled data types (since these have not been stored already)
        if (mapContainsBundledTypes(originalData)) { // bundle up all motifs, modules and sequences in a new HashMap (unless they have NULL values)
           String bundledUndoID=undoManager.getUniqueUndoID();
           HashMap<String, Data> undoWrapper=new HashMap<String,Data>();
           for (String dataname:originalData.keySet()) {
                UndoInformation info=originalData.get(dataname);
                if (shouldBeBundled(info.getDataClass()) && !info.isEmpty()) {                
                    undoWrapper.put(dataname, info.getStoredData()); // the data should already be a clone
                    info.replaceDataWithTicket(bundledUndoID);
                } 
            }
            if (!undoManager.storeObjectInCache(bundledUndoID, undoWrapper, false)) engine.logMessage("Problems occurred while storing UNDO information");                        
        }   
    }
      
    private boolean mapContainsBundledTypes(HashMap<String, UndoInformation> map) {
        for (String key:map.keySet()) {
            UndoInformation info=map.get(key);
            if (shouldBeBundled(info.getDataClass())) return true;
        }
        return false;
    }
    
    /**
     * Undos all the data changes traced by this UndoMonitor while it was registered with the engine
     * This method is used by the undo() method to reset changes and should only be called directly 
     * to immediately do rollback after abnormal termination of unsuccessful tasks (for instance due 
     * to errors/exceptions during execution or abortion by the user) in which case the normal undo/redo
     * functions are disabled
     */
    public void rollback() {
        //System.err.println("- Rollback -");
        HashMap<String, Data> bundledData=null;
        Set<String> updatedDataitems = originalData.keySet();
        ArrayList<Data> recovery = new ArrayList<Data>();
        for (String name:updatedDataitems) {
            // engine.logMessage("RB: Updating " +name);
            UndoInformation undoInfo=originalData.get(name);
            if (undoInfo==null || undoInfo.isEmpty()) { // the data item was created during execution. Just remove it and all will be as it was before
                try {engine.removeDataItem(name);} catch (Exception e) {engine.errorMessage("WARNING: an error occurred on rollback: "+e.toString(), 0);e.printStackTrace(System.err);} 
            } 
            else if (shouldBeBundled(undoInfo.getDataClass())) { // data has been bundled up
                if (bundledData==null) bundledData=(HashMap<String, Data>)undoManager.getObjectFromCache(undoInfo.getUndoTicket());
                Data storedItem=bundledData.get(name);
                recovery.add(storedItem);
            } 
            else { // data is stored individually
                Data storedItem=null;
                String undoID=undoInfo.undoTicket;
                if (undoID==null) storedItem=undoInfo.dataitem;
                else storedItem=(Data)undoManager.getObjectFromCache(undoID);
                recovery.add(storedItem);
            }
        }
        Collections.sort(recovery, sorter);
        for (Data item:recovery) {
            try {
               // engine.logMessage("RB: Recover " +item.getName()+"   type="+item.getTypeDescription());
               if (item instanceof Sequence && !engine.dataExists(item.getName(), Sequence.class)) engine.storeDataItem_useBackdoor((Sequence)item); // a "hack" to bypass the limitation that sequences cannot be added after feature data
               else engine.updateDataItem(item);
            } 
            catch (Exception e) {
                engine.errorMessage("WARNING: an error occurred in UndoMonitor.rollback(): "+e.toString(), 0);
            }
        } // end data recovery

        
    }
 
   /**
     * Redos changes that has been undone by reestablishing the state of data as it
     * was at the point when the monitor was deregistered
     */
    public void rollforward() {       
        HashMap<String, Data> bundledData=null;
        Set<String> updatedDataitems = finalData.keySet();
        ArrayList<Data> recovery = new ArrayList<Data>();
        for (String name:updatedDataitems) {
            UndoInformation undoInfo=finalData.get(name);
            if (undoInfo==null || undoInfo.isEmpty()) { // the data item was removed during execution. So remove it again for redo
                 try {engine.removeDataItem(name);} catch (Exception e) {engine.errorMessage("An error occurred in UndoMonitor.rollforward()",0);} 
            }
            else if (shouldBeBundled(undoInfo.getDataClass())) { // data has been bundled up. Extract from bundle)
                if (bundledData==null) bundledData=(HashMap<String, Data>)undoManager.getObjectFromCache(undoInfo.getUndoTicket());
                Data storedItem=bundledData.get(name);
                recovery.add(storedItem);
            }             
            else {
                Data storedItem=null;
                String undoID=undoInfo.undoTicket;
                if (undoID==null) storedItem=undoInfo.dataitem;
                else storedItem=(Data)undoManager.getObjectFromCache(undoID);  
                recovery.add(storedItem);
            }
        }
        Collections.sort(recovery, sorter);
        for (Data item:recovery) {
            try { 
                // engine.logMessage("RF: Recover " +item.getName()+"     ["+System.identityHashCode(item)+"]");
               if (item instanceof Sequence && !engine.dataExists(item.getName(), Sequence.class)) engine.storeDataItem_useBackdoor((Sequence)item); // a "hack" to bypass the limitation that sequences cannot be added after feature data
               else engine.updateDataItem(item);
            }
            catch (Exception e) {engine.errorMessage("An error occurred in UndoMonitor.rollforward(): "+e.toString(),0);}
        }
    }
 
    
    
    
    
    @Override
    public void dataAdded(Data data) {
        // System.err.println("dataAdded!! data="+data.getName());
        if (!originalData.containsKey(data.getName())) originalData.put(data.getName(), new UndoInformation(data.getClass())); // no need to store the original data, so we just make a note of the class
    }

    @Override
    public void dataRemoved(Data data) {
        if (originalData.containsKey(data.getName())) return; // the original data is already stored for this object. Further notificiations of updates can be ignored                                                                
        else if (shouldBeBundled(data)) { // delay storage of this object until deregistration of UndoMonitor
            originalData.put(data.getName(), new UndoInformation(data.clone()));
        } else { // store object in undocache right away
           String undoID=undoManager.getUniqueUndoID();
           undoManager.storeObjectInCache(undoID, data);
           originalData.put(data.getName(), new UndoInformation(undoID,data.getClass()));
        }             
    }    

    // this method is invoked by the engine just before a data value is updated/overwritten in memory (or before new data items are added)
    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {
        if (oldvalue==engine.getDefaultSequenceCollection()) return; // do not store/recover DefaultSequenceCollection
        else if (originalData.containsKey(oldvalue.getName())) return; // the original data is already stored for this object. Further notificiations of updates can be ignored
        else if (shouldBeBundled(oldvalue)) { // delay storage of this object until deregistration of UndoMonitor
           originalData.put(oldvalue.getName(), new UndoInformation(oldvalue.clone()));
        } else { // store object in undocache right away
           String undoID=undoManager.getUniqueUndoID();
           undoManager.storeObjectInCache(undoID, oldvalue);
           originalData.put(oldvalue.getName(), new UndoInformation(undoID, oldvalue.getClass()));
        }      
    }
    
    
    @Override
    public void dataUpdated(Data data) { } // Not used. We catch updates with dataUpdate(old,new) instead to get access to the original value
    @Override
    public void dataAddedToSet(Data parentDataset, Data child) { }
    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) { }

    @Override
    public boolean addEdit(UndoableEdit anEdit) { return false; }
    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {return false;}
       
    
    /**
     * This method can be called to register data updates when UndoMonitors are used but not
     * registered as data listeners with the engine.
     * @param name The name of the data object
     * @param oldvalue  A data object representing the data before it is updated (can be NULL if the data is added anew)
     * @param newvalue  A data object representing the data after it is updated (can be NULL if the data is removed)
     */
    public void setDataUpdateManually(String name, Data oldvalue, Data newvalue) {
        if (oldvalue==null) {
           originalData.put(name, new UndoInformation(newvalue.getClass())); // We store the class but not the data (newvalue should at least be non-null if the oldvalue is null)
        }
        else if (oldvalue==engine.getDefaultSequenceCollection()) {
             // do nothing...
        }                    
        else if (shouldBeBundled(oldvalue)) {
           originalData.put(oldvalue.getName(), new UndoInformation(oldvalue.clone()));
        } else {
           String undoID=undoManager.getUniqueUndoID();
           if (!undoManager.storeObjectInCache(undoID, oldvalue)) engine.logMessage("Problems occurred while caching data for " +name);
           originalData.put(oldvalue.getName(), new UndoInformation(undoID, oldvalue.getClass()));
        }           
             
        // process new value
        if (newvalue==null) { // data has been removed?
           finalData.put(name, new UndoInformation(oldvalue.getClass())); // We store the class but not the data (oldvalue should at least be non-null if the newvalue is null)
        } else if (shouldBeBundled(newvalue)) {
           finalData.put(name, new UndoInformation(newvalue.clone())); 
        } else {
           String undoID=undoManager.getUniqueUndoID();
           if (!undoManager.storeObjectInCache(undoID, newvalue)) engine.logMessage("Problems occurred while caching data for " +name);
           finalData.put(name, new UndoInformation(undoID, newvalue.getClass())); 
        }                
    }
    
    /** If this method is called, the UndoMonitor will store the current sequence order and restore it again if UNDO is called */
    public void storeSequenceOrder() {
        SequenceCollection collection=engine.getDefaultSequenceCollection();
        if (collection!=null) {
            ArrayList<String> order=(ArrayList<String>)collection.getAllSequenceNames().clone();
            sequenceOrderTicket=undoManager.getUniqueUndoID();
            if (!undoManager.storeObjectInCache(sequenceOrderTicket, order, false)) engine.logMessage("Problems occurred while caching sequence order for UNDO");
        }
    }
    
    
    @Override
    public boolean canRedo() {
        return redoInformationStored && !isDone;
    }
    
    @Override
    public boolean canUndo() {
        // return isDone;
        return true;
    }
    
    @Override
    public void die() { // performs cleanup. This can be useful to do to recycle resources if objects have been cached in memory rather than local disc
        //engine.logMessage("Undo DIE: "+presentationName);
        if (SwingUtilities.isEventDispatchThread()) {
           Runnable runner=new Runnable() {
                @Override
                public void run() {
                    cleanup();
                }
            }; 
           new Thread(runner).start();
        } else cleanup();
    }
    
    /**
     * Clears all undo/redo information registered by this UndoMonitor 
     */
    private void cleanup() {
        Set<String> updatedDataitems = originalData.keySet();
        for (String name:updatedDataitems) {
            UndoInformation undoInfo=originalData.get(name);
            if (undoInfo==null) continue;
            String undoID=undoInfo.undoTicket;
            if (undoID!=null) undoManager.clearCachedObject(undoID);
        }
        originalData.clear();
        updatedDataitems = finalData.keySet();
        for (String name:updatedDataitems) {
            UndoInformation undoInfo=finalData.get(name);
            if (undoInfo==null) continue;
            String undoID=undoInfo.undoTicket;
            if (undoID!=null) undoManager.clearCachedObject(undoID);
        }
        finalData.clear();        
    }
    
    
    @Override
    public String getPresentationName() {
        return presentationName;
    }
    
    @Override
    public String getRedoPresentationName() {
        return "Redo "+presentationName;
    }
    
    @Override
    public String getUndoPresentationName() {
        return "Undo "+presentationName;
    }

    @Override
    public boolean isSignificant() { return true; }
    

    private boolean shouldBeBundled(Data item) {
        return (item instanceof Motif || item instanceof ModuleCRM || item instanceof Sequence);
    }
    
    private boolean shouldBeBundled(Class dataclass) {
        return (dataclass==Motif.class || dataclass==ModuleCRM.class || dataclass==Sequence.class);
    }    
 
    @Override
    public void undo() throws CannotUndoException {
        rollback();
        isDone=false;
        
        if (sequenceOrderTicket!=null) { // in case sequence order is changed
            Object order=undoManager.getObjectFromCache(sequenceOrderTicket);
            if (order instanceof ArrayList) {
                int size=((ArrayList<String>)order).size();
                if (engine.getDefaultSequenceCollection().size()==size) {
                    engine.getDefaultSequenceCollection().setSequenceOrder((ArrayList<String>)order);
                    engine.getClient().getVisualizationSettings().redraw();
                }
            }
        }
    }
    
    @Override
    public void redo() throws CannotRedoException {
      if (!redoInformationStored || isDone)  throw new CannotRedoException();
        else { //engine.logMessage("Performing rollforward");
           rollforward();
           isDone=true;
        }   
    }    
    
    
    
    
    /**
     * This private class is just an information wrapper that is inserted into the HashMaps of the outer class
     * The class of the data object should always be stored. The data itself could either be stored directly
     * (as a clone of the dataitem) or by storing the object in the undo-cache provided by GUIUndoManager
     * and then providing an UndoTicket instead (which can be used to retrieve the object from the undo-cache).
     * If the dataitem is NULL (and no undoticket is provided), it is assumed that the data itself is not required
     */     
    private class UndoInformation {
        private Class thisclass=null;
        private String undoTicket=null;
        private Data dataitem=null;
        
        /** Stores the given data item directly in the UndoInformation object. The data item can not be NULL! */
        public UndoInformation(Data dataitem) {
            this.undoTicket=null;
            this.dataitem=dataitem;
            this.thisclass=dataitem.getClass();
        }
        
        /** Stores a NULL value for this data object to signify that the data itself is not needed (but the class should be noted) */
        public UndoInformation(Class dataclass) {
            this.undoTicket=null;
            this.dataitem=null;
            this.thisclass=dataclass;
        }        
        /** Stores an 'undoticket' which can be used to obtain the data from the data-cache provided by GUIUndoManager */
        public UndoInformation(String undoTicket, Class dataclass) {
            this.undoTicket=undoTicket;
            this.dataitem=null;
            this.thisclass=dataclass;            
        }        
             
        public boolean isEmpty() {
            return (dataitem==null && undoTicket==null);
        }
        
        public boolean dataStoredDirectly() {
            return (dataitem!=null);
        }
        
        public boolean dataStoredInCache() {
            return (undoTicket!=null);
        }
        
        public Class getDataClass() {
            return thisclass;
        }
        
        public String getUndoTicket() {
            return undoTicket;
        }        
        public Data getStoredData() {
            return dataitem;
        }
        
        public void replaceDataWithTicket(String ticket) {
           this.undoTicket=ticket;
           this.dataitem=null;
        }
        
        @Override
        public String toString() {
            return "UndoInformation(class="+thisclass+", data="+((dataitem!=null)?"YES":"NO")+", ticket="+undoTicket+")";
        }
        
    }

    /**
     * This class is used to sort the data items so that they are recovered in the correct order
     * (so that data items that are dependent on other data items are recovered AFTER their
     * dependencies have been recovered)
     */
    private static class DataSorter implements Comparator<Data> {
        private HashMap<Class,Integer> order=new HashMap<Class,Integer>();
        public DataSorter() {
            order.put(Sequence.class, new Integer(6));            
            order.put(DNASequenceDataset.class, new Integer(7));
            order.put(NumericDataset.class, new Integer(7));
            order.put(RegionDataset.class, new Integer(7));
            order.put(SequenceCollection.class, new Integer(10));
            order.put(SequencePartition.class, new Integer(11));
            order.put(Motif.class, new Integer(12));
            order.put(MotifCollection.class, new Integer(13));
            order.put(MotifPartition.class, new Integer(13));
            order.put(ModuleCRM.class, new Integer(14));
            order.put(ModuleCollection.class, new Integer(15));
            order.put(ModulePartition.class, new Integer(15));
            order.put(SequenceNumericMap.class, new Integer(16));
            order.put(SequenceTextMap.class, new Integer(16));    
            order.put(ExpressionProfile.class, new Integer(16));              
            order.put(MotifNumericMap.class, new Integer(17));
            order.put(MotifTextMap.class, new Integer(17));            
            order.put(ModuleNumericMap.class, new Integer(17));
            order.put(ModuleTextMap.class, new Integer(17));   
            order.put(PriorsGenerator.class, new Integer(17));
            order.put(Analysis.class, new Integer(18));
        }
        @Override
        public int compare(Data o1, Data o2) {
            if (o1==null && o2==null) return 0;
            else if (o1!=null && o2==null) return -1;
            else if (o1==null && o2!=null) return 1;
            Integer val1=order.get(o1.getClass());
            Integer val2=order.get(o2.getClass());
            if (val1==null) return -1; // recover those with NULL order first (in any order)
            else if (val2==null) return 1; // recover those with NULL order first
            return val1.compareTo(val2);
        }

    }


 }
