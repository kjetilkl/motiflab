package org.motiflab.gui;

import java.io.*;
import java.lang.management.ManagementFactory;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.*;
import javax.swing.event.UndoableEditEvent;
import java.util.HashSet;
import java.util.HashMap;
import javax.swing.SwingWorker;
import org.jdesktop.application.ApplicationContext;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Region;

/**
 * The GUIUndoManager is responsible for handling undo/redo events initiated by the GUI.
 * It handles restoration of previous states by caching old data objects as these are
 * updated or deleted. The cache is maintained on disc to avoid using up main memory,
 * however, for efficiency purposes, data objects are first stored in an "in memory"
 * cache while a background thread is spawned which saves the data to disc. After data
 * has been saved it is deleted from the "in memory" cache. When restoring objects,
 * the "in memory" cache is first searched and if the requested data is not there,
 * it is obtained from disc. This strategy was chosen to avoid freezing up the GUI
 * since the undo/redo methods are called on the EDT and storing large datasets on
 * disc in the EDT is not recommended. Note that restoration of objects is still
 * done by reading the data back on the EDT which will make the GUI unresponsive
 * while performing undo redo, but it will no longer use up extra resources
 * after operations finish.
 *
 * @author kjetikl
 */
public class GUIUndoManager extends UndoManager implements UndoableEditListener {
    private ExtendedUndoableEditSupport support;
    //private Object source;
    private HashSet<String> usedCacheKeys;
    private HashMap<String,Object> virtualMemoryStorage; //  this is used to temporary store Data objects while they are written to disc, or as a last resort if disc-caching fails
    private MotifLabGUI gui;
    private static int counter=0; // would it be better to use a long or is that just overkill? If you have extremely large datasets you would probably not want to use the GUI-client anyway...
    private UndoableEditListener forwardListener=null;
    private String cacheDirectoryName=null; // the cacheDirectory is unique for this MotifLab process
    private String cacheDirectoryFullPath=null; // the cacheDirectory is unique for this MotifLab process
    private HashSet<java.io.Closeable> openfiles=null;

    /** Creates a new UndoManager to be used with the GUI */
    public GUIUndoManager(ApplicationContext context, MotifLabGUI gui) {
        super();
        this.gui=gui;
        virtualMemoryStorage=new HashMap<String, Object>();
        cacheDirectoryName="UndoCache_"+ManagementFactory.getRuntimeMXBean().getName();
        openfiles=new HashSet<java.io.Closeable>();
        try {
           File localStorage=context.getLocalStorage().getDirectory();
           if (localStorage!=null) cacheDirectoryFullPath=localStorage.getAbsolutePath()+File.separator+"currentsession"+File.separator+cacheDirectoryName;
           if (cacheDirectoryFullPath!=null) {
               File dir=new File(cacheDirectoryFullPath);
               if (!dir.mkdirs()) throw new IOException("Unable to create cache directory on local disc => "+cacheDirectoryFullPath);
               dir.deleteOnExit(); // tells the VM to delete the cache directory automatically on exit
           } else throw new IOException("Unable to create cache directory on local disc: Problems with obtaining path to directory");
        } catch (Exception e) {gui.logMessage("UNDO cache error: "+e.getMessage());}
        support=new ExtendedUndoableEditSupport();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
               clearCache();
            }
        });

    }
    
    
    /**
     * If the parameter recipient is set to something different than NULL,
     * incoming undoableEditEvents will not be added to the undo queue but
     * forwarded to the specified recipient instead. If recipient is NULL, 
     * undoEvents will be added to the queue as normal
     * This can be used to intercept events coming in to the GUIUndoManager.
     * 
     * @param recipient 
     */
    public void forwardUndoEvents(UndoableEditListener recipient) {
        forwardListener=recipient;
    }
    
    /** A convenience method to get to the engine */
    public MotifLabEngine getEngine() {
       return gui.getEngine();
    }
    
    @Override
    public synchronized boolean addEdit(UndoableEdit edit) {
        if (forwardListener!=null) {
            forwardListener.undoableEditHappened(new UndoableEditEvent(this,edit));
            return true;
        }
        boolean ok=super.addEdit(edit);
        if (ok) support.postEdit(edit);
        //gui.debugMessage("added Edit to undoManager => "+edit.toString()+"   Queue:"+edits.size()+"  ok="+ok);
        return ok;
    }
        
    @Override
    public synchronized void undoableEditHappened(UndoableEditEvent e) {
        if (forwardListener!=null) {
            forwardListener.undoableEditHappened(e);
            return;
        }
        UndoableEdit edit=e.getEdit();
        //source=e.getSource();
        addEdit(edit);       
    }
    
    public synchronized void addUndoableEditListener(UndoableEditListener listener) {
        support.addUndoableEditListener(listener);    
    }
    
    public synchronized void removeUndoableEditListener(UndoableEditListener listener) {
        support.removeUndoableEditListener(listener);    
    }
       
    /**
     * Returns a unique ID that can be used to identify an UndoableEdit
     * @return
     */
    public synchronized String getUniqueUndoID() {
        counter++;
        return "Undo_"+counter;
    }
    
    
    public void debug() {
        gui.debugMessage("Undoables left="+edits.size());
        gui.debugMessage("InProgress="+isInProgress());
        UndoableEdit nextundo=editToBeUndone();
        gui.debugMessage("editToBeUndone="+nextundo);
        gui.debugMessage("Manager status: undo="+canUndo()+"   redo="+canRedo()+"   both="+canUndoOrRedo());
        for (UndoableEdit edit:edits) {
            gui.debugMessage("  "+edit.getPresentationName()+"["+edit.getClass().toString()+"]  undo=>"+edit.canUndo()+"  redo=>"+edit.canRedo()+"  significant="+edit.isSignificant());
        }
    }
    
    @Override
    public void discardAllEdits(){
        super.discardAllEdits();
        gui.updateUndoRedoStates();
    }
    
    /**
     * Temporarily stores an object so that it can be retrieved later  
     * (usually on local disc but if that does not work then it stores it directly in virtual memory).
     * This is an easy way of implementing undos (by storing previous version of the data)
     * @param key A unique name to associate with the object. This name is used to retrieve it later
     * @param object The object to be stored (this should be Data, Region or HashMap<String,Data>).
     * @param doClone. If TRUE the object in question will be deep-cloned so that it can be safely be deleted from the outside environment
     * @return Always TRUE. (Used to be: True if the object could be stored on disc, false if it had to be stored in memory)
     */
    public boolean storeObjectInCache(final String key, Object object, boolean doClone) {
        if (usedCacheKeys==null) usedCacheKeys=new HashSet<String>();
        Object clone=null;
        if (doClone) {
            if (object instanceof Data) clone=((Data)object).clone();
            else if (object instanceof Region) clone=((Region)object).clone();                
            else if (object instanceof HashMap) clone=cloneMap((HashMap<String,Data>)object);            
            else throw new NullPointerException("Unable to clone object in storeObjectInCache():"+((object==null)?null:object.getClass()));
        } else clone=object;
        final Object value=clone;

        final String filename=cacheDirectoryFullPath+File.separator+key+".ser";   
        final File cachefile=new File(filename);
        cachefile.deleteOnExit();
        virtualMemoryStorage.put(key, value); // cache it in memory while it is saved to disc in a background thread. Remove from memory-cache afterwards       
        //gui.getEngine().logMessage(key+" stored in virtual cache. "+value.getClass().toString());
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override 
            public Boolean doInBackground() {
                ObjectOutputStream stream=null;
                try {
                     stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cachefile)));
                     stream.flush(); // force write header?
                     openfiles.add(stream);
                     stream.writeObject(value);
                     stream.close();
                     openfiles.remove(stream);
                     usedCacheKeys.add(key);    
                     return Boolean.TRUE;
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (stream!=null) stream.close();
                    } catch (Exception x){}
                    openfiles.remove(stream);
                }
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                    gui.logMessage("Note: Unable to cache '"+value+"' to local disc. Using virtual memory for storage instead (Exception message:"+ex.toString()+")");
                    ex.printStackTrace(System.err);
                } else {
                    virtualMemoryStorage.remove(key);
                }
            }
        }; // end of SwingWorker class
       worker.execute();
       return true;
    }

    public boolean storeObjectInCache(final String key, Object object) {
        return storeObjectInCache(key, object, true);
    }
    
    /** Returns a deep clone of a HashMap<String,Data> data structure */
    private HashMap<String,Data> cloneMap(HashMap<String,Data> object) {
        HashMap<String,Data> clone=new HashMap<String,Data>(object.size());
        for (String key:object.keySet()) {
            Data dataclone=object.get(key);
            if (dataclone!=null) dataclone=dataclone.clone();
            clone.put(key, dataclone);
        }
        return clone;
    }
    
    /**
     * Fetches an object that has been temporarily stored
     * This is an easy way of implementing undos (by retrieving previous versions of the data that has been stored)
     * @param key The unique name of the object
     */
    public Object getObjectFromCache(String key) {
        Object memorycachedobject=virtualMemoryStorage.get(key);
        if (memorycachedobject!=null) return memorycachedobject; // check temp-cache. Maybe it hasn't been saved yet
        ObjectInputStream stream=null;
        try { // read object from disc-cache
             String filename=cacheDirectoryFullPath+File.separator+key+".ser";  
             stream=new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
             openfiles.add(stream);
             Object value=stream.readObject();
             stream.close();
             openfiles.remove(stream);
             return value;        
        } catch (Exception e) { // unable to read it from disc. try virtual memory cache.
            if (stream!=null) try {stream.close();} catch(Exception ex){}
            openfiles.remove(stream);
            if (memorycachedobject!=null) return memorycachedobject;
            else {
                gui.logMessage("Undo Error: Unable to load cached object from local disc: "+e.toString());
                return null;
            }
        }

    }
    

    /**
     * Clears all data that has been stored in local disc-cache (as well as the cache itself)
     * This method should only be called on system shutdown to remove any 
     * temporary files that have been created
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    private void clearCache() {
        if (cacheDirectoryFullPath!=null) {
            HashSet<java.io.Closeable> stillopen=(HashSet<java.io.Closeable>)openfiles.clone(); // make a clone to avoid concurrent modification errors
            for (java.io.Closeable stream:stillopen) {
                try {stream.close();} catch (Exception e) {} // force close any open streams that have not finished writing/reading
            }
            // -- NOTE: all the cachefiles are now marked for "deleteOnExit" an should be removed automatically by the JVM.
            // ---      so the next line (which deletes the temp-dir manually) is no longer needed
            // if (!deleteTempFile(new File(cacheDirectoryFullPath))) { gui.logMessage("WARNING:Unable to delete UNDO directory");} // delete cachedir recursively
        } else gui.logMessage("WARNING: unable to delete UNDO directory!");
     }
    
    
    /**
     * Clears a cached object associated with the given key
     */
    public void clearCachedObject(String key) {
        //gui.debugMessage("Clearing cached object: "+key);
        if (usedCacheKeys==null) return;
        if (virtualMemoryStorage!=null && virtualMemoryStorage.containsKey(key)) virtualMemoryStorage.remove(key);
        else {
            if (cacheDirectoryFullPath!=null) {
               File file=new File(cacheDirectoryFullPath+File.separator+key+".ser");
               if (file.exists() && !file.delete()) gui.logMessage("Unable to delete cached file '"+file.getAbsolutePath()+"'");
            } 
        }
        usedCacheKeys.remove(key);
    }
    
    /** Returns the next significant edit to be undone if undo is invoked. It is not removed from the queue */
    public UndoableEdit peek() {
        return editToBeUndone();
    }
    
    /** Removes the last UndoableEdit that was added to the queue and returns it */
    public UndoableEdit pop() {
        int size=edits.size();
        UndoableEdit lastedit=edits.elementAt(size-1);
        edits.removeElementAt(size-1);
        gui.updateUndoRedoStates();
        gui.getFrame().repaint(); // This seems necessary but it shouldn't be
        return lastedit;
    }
    
    @Override
    public synchronized void undo() throws CannotUndoException {
        super.undo();
        gui.updateUndoRedoStates();
        gui.getFrame().repaint(); // This seems necessary but it shouldn't be    
    }
   
    @Override
    public synchronized void redo() throws CannotUndoException {
        super.redo();
        gui.updateUndoRedoStates();
        gui.getFrame().repaint(); // This seems necessary but it shouldn't be    
    }
    
    /** Returns an UndoMonitor that can be used to track changes.
     *  The monitor has already been registered with this UndoManager
     *  (but has not been added to the undo-queue)
     */
    public UndoMonitor getUndoMonitor(String presentationName) {
        return new UndoMonitor(this, presentationName);
    }
    
    /**
     * Returns true if the specified edit is already added to the undo-queue
     * @param edit
     * @return
     */
    public boolean isInUndoQueue(UndoableEdit edit) {
        return edits.contains(edit); // "edits" is the undo-queue
    }
    
    
    /** */
    private class ExtendedUndoableEditSupport extends UndoableEditSupport { // I honestly am not sure what this class does...
        public synchronized void postEdit(UndoableEdit edit) {
            //if (source!=null) realSource=source;
            super.postEdit(edit);
        }
    }
    
    
    /** Deletes the given file or directory (and subdirectories) */
    private boolean deleteTempFile(File tempdir) {
        if (tempdir==null || !tempdir.exists()) return true;
        if (tempdir.isDirectory()) {
            boolean ok=true;
            File[] files=tempdir.listFiles();
            if (files==null) return ok;
            for (File file:files) {
                if (file.isDirectory()) ok = ok && deleteTempFile(file);
                else ok = ok && file.delete();
            }
            ok = ok && tempdir.delete();
            return ok;
        } else return tempdir.delete();
    }    
    
}
