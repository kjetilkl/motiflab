/*
 
 
 */

package motiflab.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import org.jdesktop.application.LocalStorage;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.Protocol;
import motiflab.engine.protocol.SerializedStandardProtocol;
import motiflab.engine.protocol.StandardProtocol;


/**
 * Used to manage multiple open protocols
 * 
 * @author kjetikl
 */
public class ProtocolManager extends DefaultListModel implements DocumentListener {
    private transient ProtocolEditor protocolEditor=null;
    String currentProtocolName=null;
    private int protocolCount=0; // a number used as suffix for the protocol name when creating new protocols
    private int recentListMaxSize=6; // number of recent protocols to display in menu
    private static final String recentProtocolsFileName="recentProtocols.ser";
    HashMap<String,Protocol> storage; // used to keep track of "closed" protocols. Since the Document part of the Protocol is kept in memory anyway (through references in DocumentEditEvents that are stored in the undo queue) we might as well keep the full Protocols (which are only a few more fields) 
    private HashMap<PlainDocument,Protocol> documentProtocolMapping; // maps document names to protocol names
    HashMap<Protocol,Integer> caretposition; // stores the caret position within each protocol document
    private transient ArrayList<String> recentprotocols;
    private transient OpenRecentMenuListener openrecentmenulistener;
    
    
    public ProtocolManager(ProtocolEditor protocolEditor) {
        super();
        this.protocolEditor=protocolEditor;
        storage=new HashMap<String, Protocol>();
        documentProtocolMapping=new HashMap<PlainDocument,Protocol>();
        caretposition=new HashMap<Protocol,Integer>();
        recentprotocols=new ArrayList<String>();
        openrecentmenulistener=new OpenRecentMenuListener();
        updateRecentMenu(true);
    }

    /** Imports state settings from a Protocol Manager that has been serialized */
    public void importSettings(SerializedProtocolManager manager) {
        clear(); // clears protocols stored in the ListModel
        storage.clear();
        documentProtocolMapping.clear();
        caretposition.clear();
        this.currentProtocolName=manager.currentProtocolName;
        for (SerializedStandardProtocol serprot:manager.protocolList) {
            StandardProtocol protocol=serprot.getProtocol(protocolEditor.getGUI().getEngine());
            addProtocol(protocol);
            caretposition.put(protocol,manager.caretposition.get(protocol.getName()));
        }
        protocolEditor.changeProtocol(currentProtocolName);
        protocolCount=getSize();
    }
    
    /** Clears all protocols currently open and installs a new one
     *  This method is only used to restore sessions that do not have SerializedProtocolManagers
     */
    public void clearAndInstallProtocol(StandardProtocol protocol) {
        clear(); // clears protocols stored in the ListModel
        storage.clear();
        documentProtocolMapping.clear();
        caretposition.clear();
        this.currentProtocolName=protocol.getName();
        addProtocol(protocol);
        caretposition.put(protocol,0);
        protocolEditor.changeProtocol(currentProtocolName);
        protocolCount=getSize();        
    }
    
    /**
     * Creates a new empty Protocol object with a generic name (it is not added to the pool)
     * @param 
     */
    public Protocol createNewProtocol() {
        Protocol newprotocol=new StandardProtocol(protocolEditor.getGUI().getEngine());
        newprotocol.setName(getDefaultProtocolName());
        return newprotocol;
    }    
    
    
    /** Returns a new unique name that can be used for newly created protocols */
    public String getDefaultProtocolName() {
        protocolCount++;
        String protocolName="Protocol-"+protocolCount;
        while (getProtocol(protocolName)!=null || getProtocol(protocolName+".txt")!=null) { // for some reason the name is in use already
           protocolCount++;
           protocolName="Protocol-"+protocolCount;  
        }
        return protocolName;
    }
    
    
    
    
    /**
     * Adds a new Protocol to be managed by this ProtocolManager
     * @param protocol
     */
    public void addProtocol(Protocol protocol) {
        PlainDocument doc=(PlainDocument)protocol.getDocument();
        doc.addDocumentListener(this);
        documentProtocolMapping.put(doc,protocol);
        addElement(protocol);
    }
    
    /**
     * Registering a protocol as "recent" which will make it show up in the
     * "open recent protocol" menu (maybe)
     * 
     * @param protocol
     */
    public void registerRecent(Protocol protocol) {
        String filename=((StandardProtocol)protocol).getFileName();  // this is the full file path
        if (recentprotocols.contains(filename)) recentprotocols.remove(filename);
        if (recentprotocols.size()>=recentListMaxSize) recentprotocols.remove(recentListMaxSize-1);
        recentprotocols.add(0,filename);
        updateRecentMenu(false);
    }
    
    @SuppressWarnings("unchecked")
    private void updateRecentMenu(boolean readFromDisc) {
        JMenu menu=protocolEditor.getGUI().getFileOpenRecentProtocolsSubmenu();
        menu.removeAll();       
        if (readFromDisc) {  
            LocalStorage localStorage=protocolEditor.getGUI().getApplication().getContext().getLocalStorage();
            try {
               Object list=localStorage.load(recentProtocolsFileName);
               if (list instanceof ArrayList) recentprotocols=(ArrayList<String>)list;
            } catch (Exception e) {}
        } 
        for(String pathname:recentprotocols) {
            String shortname;
            if (pathname.startsWith("http://") || pathname.startsWith("https://") || pathname.startsWith("ftp://") || pathname.startsWith("ftps://")) {
                int endpos=pathname.indexOf('?');
                if (endpos<0) endpos=pathname.length();
                int startpos=pathname.lastIndexOf('/',endpos-1);
                if (startpos<0) startpos=0; else startpos++;
                shortname=pathname.substring(startpos,endpos);
            } else {
                File file=protocolEditor.getGUI().getEngine().getFile(pathname);
                shortname=file.getName();
            }
            JMenuItem menuItem=new JMenuItem(shortname);
            menuItem.setActionCommand(pathname);
            menuItem.setToolTipText(pathname);
            menuItem.addActionListener(openrecentmenulistener);
            menu.add(menuItem);
        }
        if (recentprotocols.isEmpty()) menu.setEnabled(false);
        else menu.setEnabled(true);
    }
    
    
    /**
     * Return the Protocol with the given name (if it is currently registered)
     */
    public Protocol getProtocol (String protocolName) {
        if (protocolName==null) return null;
        for (int i=0;i<getSize();i++) {
            Protocol prot=(Protocol)getElementAt(i);
            if (prot.getName().equals(protocolName)) return prot;
        }
        return null;
    }
    
    /**
     * Return the last Protocol registered
     */
    public Protocol getLastProtocol () {
        if (getSize()==0) return null;
        else return (Protocol)elementAt(getSize()-1);
    }
    
    /** Returns a list of all open protocols */
    public List<Protocol> getAllProtocols() {
        ArrayList<Protocol> list=new ArrayList<>(getSize());
        for (int i=0;i<getSize();i++) {
            Protocol prot=(Protocol)getElementAt(i);
            list.add(prot);
        }    
        return list;
    }

    /**
     * Returns the name of the Protocol currently being edited in the ProtocolEditor
     * @return
     */
    public String getCurrentProtocolName() {
        return currentProtocolName;
    }
    
    /**
     * Returns TRUE if a Protocol with the given name is currently present in the collection of 
     * open protocols (i.e. although not necessarily being edited at the moment)
     * @param name The name of the protocol
     * @return
     */
    public boolean isProtocolOpen(String protocolName) {
        for (int i=0;i<getSize();i++) {
            Protocol prot=(Protocol)getElementAt(i);
            if (prot.getName().equals(protocolName)) return true;
        }        
        return false;
    }
    
    /**
     * Sets the name of the current protocol. This method should be called whenever
     * the Protocol being edited changes
     */
    public void setCurrent(String name) {
        currentProtocolName=name;
        int index=-1;
        if (name!=null) {
            for (int i=0;i<getSize();i++) {
                Protocol prot=(Protocol)getElementAt(i);
                if (prot.getName().equals(name)) {index=i;break;}
            }
        }
        fireContentsChanged(this, index, index);
    }
    
    /** Returns the registered caret position for the given protocol */
    public int getCaretPosition(Protocol protocol) {
        Integer i=caretposition.get(protocol);
        if (i!=null) return i.intValue();
        else return 0;
    }
    
    /** Stores the caret position for the given protocol */
    public void setCaretPosition(Protocol protocol, int position) {
        caretposition.put(protocol, position);
    }
 
    
   /** 
    * Saves the given Protocol script to file.
    * If the supplied boolean argument is true, a dialog will be displayed to allow the user to
    * select a filename for the protocol. If the argument is false, the filename currently associated
    * with the script will be used (if the protocol has not been save before a dialog will be displayed)
    * @return FALSE if the user canceled the save interactively or TRUE if the data was saved (or at least attempted)
    */    
    public boolean saveProtocolFile(StandardProtocol protocol, boolean askForNewName) {
        String currentFileName=protocol.getFileName();
        boolean isURL=(currentFileName!=null && (currentFileName.startsWith("http://") || currentFileName.startsWith("https://") || currentFileName.startsWith("ftp://")|| currentFileName.startsWith("ftps://")));
        if (!askForNewName && !protocol.isDirty() && !isURL) return true; // we didn't really save because there was no need to...
        File file=null;
        File parentDir=protocolEditor.getGUI().getLastUsedDirectory();
        if (askForNewName || currentFileName==null || isURL) {            
            final JFileChooser fc = protocolEditor.getGUI().getFileChooser(parentDir);// new JFileChooser(protocolEditor.getGUI().getLastUsedDirectory());
            fc.setDialogTitle("Save protocol: "+protocol.getName()); 
            if (currentFileName!=null && !isURL) {
                File preselected=protocolEditor.getGUI().getEngine().getFile(currentFileName);
                fc.setSelectedFile(preselected);
            } else {
                String newfilename=protocol.getName();
                if (!newfilename.endsWith(".txt")) newfilename+=".txt";
                File preselected=MotifLabEngine.getFile(parentDir,newfilename);
                fc.setSelectedFile(preselected);
            }
            int returnValue=fc.showSaveDialog(protocolEditor);
            if (returnValue==JFileChooser.APPROVE_OPTION) {
                file=fc.getSelectedFile();            
            } else return false;
        } else {          
           //String filename=((StandardProtocol)protocol).getFileName();
           file=protocolEditor.getGUI().getEngine().getFile(currentFileName);
        } 
        if (protocolNameInUse(protocol, file.getName())) {
            JOptionPane.showMessageDialog(protocolEditor.getGUI().getFrame(), "A protocol with the same name is already open", "Protocol exists", JOptionPane.ERROR_MESSAGE);
            return false;
        }        
        if ((askForNewName || currentFileName==null || isURL) && file.exists()) {
            int choice=JOptionPane.showConfirmDialog(protocolEditor.getGUI().getFrame(), "Overwrite existing file \""+file.getName()+"\" ?","Save Protocol",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return false;
        }
        String oldprotocolname=protocol.getName();
        protocolEditor.getGUI().setLastUsedDirectory(file.getParentFile());
        saveProtocolToFile(protocol, file); // this also sets the name of the protocol equal to the new filename
        String newprotocolname=file.getName();
        protocol.setName(newprotocolname);
        if (currentProtocolName.equals(oldprotocolname) && !oldprotocolname.equals(newprotocolname)) {
            currentProtocolName=newprotocolname; // update currentProtocolName field if the current protocol was saved under a different name
            int index=0;
            for (int i=0;i<getSize();i++) {
                StandardProtocol p=(StandardProtocol)getElementAt(i);
                if (p==protocol) {index=i; break;}
            }
            fireContentsChanged(this, index, index);
            
        } 
        return true;
    }   
    
    
    /**
     * This method saves the selected protocol to the selected file
     * This should be called on the EDT because it might display some error messages
     * @param protocol
     * @param file
     */
    public void saveProtocolToFile(final Protocol protocol, final File file) {
        final String filename=file.getName();
        final ProtocolEditor parent=protocolEditor;
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override 
            public Boolean doInBackground() {
                PlainDocument document=(PlainDocument)protocol.getDocument();
                Element root=document.getDefaultRootElement();
                int linecount=root.getElementCount();
                PrintWriter outputStream=null; // PrintWriter is buffered!
                try {
                    OutputStream stream=MotifLabEngine.getOutputStreamForFile(file);
                    outputStream=new PrintWriter(stream);
                    for (int i=0;i<linecount;i++) { 
                          Element e=root.getElement(i);  
                          String line=document.getText(e.getStartOffset(),e.getEndOffset()-e.getStartOffset());
                          line=line.trim();
                          outputStream.println(line);
                    }                    
                } catch (Exception e) { 
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (outputStream!=null) outputStream.close();} catch (Exception ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing PrintWriter in ProtocolManager: "+ioe.getMessage());}
                }       
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     JOptionPane.showMessageDialog(protocolEditor.getGUI().getFrame(), ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                     protocolEditor.getGUI().logMessage("Unable to save protocol script \""+filename+"\"  ("+ex.getMessage()+")");
                     return;
                } else { // save went OK
                    if (protocol instanceof StandardProtocol) {
                        ((StandardProtocol)protocol).setDirtyFlag(false);
                        try {
                            ((StandardProtocol)protocol).setFileName(file.getCanonicalPath());
                            protocol.setName(filename);
                        } catch (Exception e) {System.err.println("SYSTEM ERROR: Unable to set canonical path when saving Protocol");}
                    }                    
                    registerRecent(protocol);
                    protocolEditor.updateProtocolRendering();
                    protocolEditor.getGUI().logMessage("Saved protocol script \""+filename+"\"");
                }
            }
        }; // end of SwingWorker class
        worker.execute();              
    }
    
    
    /** Saves all files */
    public void saveAll() {
        for (int i=0;i<getSize();i++) {
            StandardProtocol protocol=(StandardProtocol)getElementAt(i);
            saveProtocolFile(protocol, false);
        }
    }
    
    /** Checks if the new protocol name is in use already by another protocol (not the argument protocol) */
    private boolean protocolNameInUse(Protocol protocol, String newname) {
        for (int i=0;i<getSize();i++) {
            StandardProtocol otherprotocol=(StandardProtocol)getElementAt(i);
            if (otherprotocol==protocol) continue;
            if (otherprotocol.getName().equals(newname)) return true;
        }
        return false;
    }
    
    /**
     * This method should be called when the system exits. 
     * It saves the "state" of the ProtocolManager (e.g. recently opened scripts etc.)
     */
    public void shutdown() {
        LocalStorage localStorage=protocolEditor.getGUI().getApplication().getContext().getLocalStorage();
        try {
           localStorage.save(recentprotocols, recentProtocolsFileName);
        } catch (Exception e) {}
    }
    
    /** This method closes the given protocol */
    public void closeProtocol(String protocolName) {
        Protocol protocol=getProtocol(protocolName);
        if (protocol==null) return; 
        CloseProtocolUndoableEvent event=new CloseProtocolUndoableEvent((StandardProtocol)protocol, protocolEditor.getGUI().getEngine());     
        protocolEditor.getGUI().getUndoManager().addEdit(event);
    }
    
    public void closeAllWithoutUndo() {
        clear();
        protocolEditor.changeProtocol(null);
    }
    
    
    /** Removes the protocol with the specified name from the list */
    private void removeProtocolFromList(String protocolName) {
        Protocol prot=getProtocol(protocolName);
        removeElement(prot);
    }
    
    /** Returns the number of protocols currently open */
    public int getNumberOfOpenProtocols() {
        return size();
    }

    /** Returns true if any of the currently open protocols are dirty */
    public boolean isAnyDirty() {
        for (int i=0;i<getSize();i++) {
            StandardProtocol protocol=(StandardProtocol)getElementAt(i);
            if (protocol.isDirty()) return true;
        }
        return false;
    }

    
    
// ------------------   DocumentListener    -----------------------------------     

    @Override
    public void changedUpdate(DocumentEvent e) {
        respondToUpdate(e);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        respondToUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        respondToUpdate(e);
    }        
    
    /* this will trigger a change in the installed protocol if the change occurs in another */
    private void respondToUpdate(DocumentEvent e) { 
        Document doc=e.getDocument();
        if (protocolEditor==null || doc==null || protocolEditor.getProtocol()==null || protocolEditor.getProtocol().getDocument()==null || protocolEditor.getProtocol().getDocument()==doc) return;               
        Protocol prot=documentProtocolMapping.get(doc);
        protocolEditor.changeProtocol(prot.getName());                            
    } 
            
// ------------------------------------------------------------------------------    
    
private class CloseProtocolUndoableEvent extends AbstractUndoableEdit {
        private String protocolName;
        private String cacheTicket=null;
        private MotifLabEngine engine;
                      
        public CloseProtocolUndoableEvent(StandardProtocol protocol, MotifLabEngine engine) {
            protocolName=protocol.getName();
            this.engine=engine;
            GUIUndoManager undoManager=protocolEditor.getGUI().getUndoManager();
            cacheTicket=undoManager.getUniqueUndoID();
            storage.put(cacheTicket, protocol);
            removeProtocolFromList(protocolName);
            Protocol other=getLastProtocol(); // 
            String otherName=null;
            if (other!=null) otherName=other.getName();
            protocolEditor.changeInstalledProtocol(protocolName,otherName);            
        }        
    
        @Override
        public String getPresentationName() {
            return "close "+protocolName;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            StandardProtocol newProtocol=(StandardProtocol)storage.get(cacheTicket);
            addProtocol(newProtocol);
            //protocolEditor.changeProtocol(protocolName, null, false); // change back to this protocol, but do not queue new undoable
            protocolEditor.changeProtocol(protocolName); // change back to this protocol, but do not queue new undoable
        }

        
        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            removeProtocolFromList(protocolName);
            Protocol other=getLastProtocol(); 
            String otherName=null;
            if (other!=null) otherName=other.getName();
            protocolEditor.changeInstalledProtocol(protocolName,otherName);
        }
        
        @Override
        public void die() { 
            // this is called when the "close" undoable can no longer be undone or redone
            // which is when it is undone and then some other undoable event is created before this is redone
            // in which case the Protocol script that was closed will still be the one which is open!         
            Protocol prot=storage.remove(cacheTicket); // since the close can no longer be undone we can remove the reference.
            if (prot!=null) {
                documentProtocolMapping.remove(prot.getDocument());
                caretposition.remove(prot);
            }
        }

        
} // end private class CloseUndoableEvent


private class OpenRecentMenuListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
        String fullpath=e.getActionCommand();
        Object input=null;
        if (fullpath.startsWith("http://") || fullpath.startsWith("https://") || fullpath.startsWith("ftp://")|| fullpath.startsWith("ftps://")) {
            try {input=new URL(fullpath);} catch (MalformedURLException mue) {protocolEditor.getGUI().logMessage("Trying to open Malformed URL: "+fullpath);}
        } else input=protocolEditor.getGUI().getEngine().getFile(fullpath);
        //String shortname=file.getName();
        protocolEditor.openProtocolFileButAskToRevert(input);
    }        
} // end private class OpenRecentMenuListener
    
/** 
 * Returns a Serializable version of the ProtocolManager and its state (including serialized Protocols)
 * The state can be restored from this object using the importSettings() method
 */
public SerializedProtocolManager getSerializedProtocolManager() {
    return new SerializedProtocolManager(this);
}




}

