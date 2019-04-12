/*
 
 
 */

package motiflab.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import motiflab.engine.DataListener;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.protocol.Protocol;
import motiflab.engine.protocol.StandardProtocol;

/**
 * This class represents the Tabbed Pane that contains tabs for "Visualization", "Protocol" and
 * all Output objects. It provides functionality to add new tabs (for output objects) and close
 * output tabs and also contains an OutputDataManager to keep track of open OutputData objects
 * with respect to saving (e.g. if the user wants to "Save All" open objects).
 * 
 * @author kjetikl
 * 
 * ACKNOWLEDGEMENT: Portions of the code in this class was taken from an example by Halil Karakose
 */
public class MainPanel extends JTabbedPane implements DataListener {
        private MotifLabEngine engine;
        private MotifLabGUI gui;
        private JMenu viewMenu=null;
        private final MainPanel mainpanel=this;
        private String closingDownNow=null; 
        
	public MainPanel(MotifLabGUI gui, JMenu viewMenu) {
            this.gui=gui;
            this.engine=gui.getEngine();
            this.viewMenu=viewMenu;
            this.addChangeListener(new SelectedTabChangedListener());
            engine.addDataListener(this);         
	}
       
        
        /** Changes the current tab to the one with the given name (if exists) */
        public void setSelectedTab(String tabName) {
            for (int i=0;i<getTabCount();i++) {
               String tabTitle=getTitleAt(i);
               if (tabTitle.equalsIgnoreCase(tabName)) {
                   setSelectedIndex(i); 
                   return;
               }
            }
        }
        
        /** returns the tab index for tab with the given name (or -1 if no such tab exists) */
        public int getTabIndexFor(String tabName) {
            for (int i=0;i<getTabCount();i++) {
               String tabTitle=getTitleAt(i);
               if (tabTitle.equalsIgnoreCase(tabName)) {
                   return i;
               }
            }
            return -1;
        }
        
        
        
        /** Returns the name of the currently selected tab */
        public String getSelectedTabName() {
            return getTitleAt(getSelectedIndex());  
       }
        
       /** Returns the tab-names in the order listed */ 
        public String[] getTabNames() {
            String[] names=new String[getTabCount()];
            for (int i=0;i<getTabCount();i++) {
                names[i]=getTitleAt(i);
            }
            return names;
        }
        
        /** Returns TRUE if the main panel contains a tab with the given name */
        public boolean hasTab(String name) {
            return (getTabIndexFor(name)>=0);
        }
        
        /** Returns TRUE if the main panel contains a tab with the given name
         *  and that tab contains an OutputData object formatted in the given format
         *  @param name The name of the output data object (same as tab name)
         *  @param formatsuffix A suffix which should match any of the allowed suffixes of a dataformat
         */
        public boolean hasTab(String name, String formatsuffix) {
            int index=getTabIndexFor(name);
            if (index<0) return false;           
            Component tab=((CloseButtonTab)getTabComponentAt(index)).getContentComponent();
            if (tab instanceof OutputPanel) {
                OutputData data=((OutputPanel)tab).getOutputData();
                String formatname=data.getDataFormat();
                DataFormat format=engine.getDataFormat(formatname);
                if (format!=null) {
                    for (String suffix:format.getSuffixAlternatives()) {
                        if (suffix.equalsIgnoreCase(formatsuffix)) return true;
                    }
                }
            }                    
            return false;
        }
        
       /** Closes the current open output file tab (not Visualization or protocol) */
       public void closeCurrentTab() {
           int i=getSelectedIndex();
           if (getTitleAt(i).equals("Visualization") || getTitleAt(i).equals("Protocol")) return;
           else {
              Component tab=getTabComponentAt(i); 
              ((CloseButtonTab)tab).closeTab();                          
           }
           i=getSelectedIndex();
       } 
        
       public void showVisualizationPanel() {
           setSelectedTab("Visualization");
       }
       
       public void showProtocolEditor() {
           setSelectedTab("Protocol");
       }
                  
       /** 
        * Saves the contents of the currently open tab to file (only Protocol scripts or Output objects)
        * @param askForName 
        */
       public void saveCurrentTabToFile(boolean askForName) {
         String currentTab=getSelectedTabName();
         if (currentTab.equals("Visualization") || currentTab.equals("Protocol")) {
             int index=getTabIndexFor("Protocol");
             if (index<0) return;
             ProtocolEditor protocolEditor=(ProtocolEditor)((CloseButtonTab)getTabComponentAt(index)).getContentComponent();
             protocolEditor.saveProtocolFile(askForName);
         } else {
             OutputPanel panel=(OutputPanel)getSelectedComponent();
             OutputData data=panel.getOutputData();
             saveOutputDataToFile(data,askForName);
         }          
       }

    /** 
     * Saves the contents out the given OutputData object to its associated file
     * If the OutputData object has no associated file or if the flag asForNewName is TRUE
     * then a FileChooser dialog will be displayed which allows the user to select a new file
     * @return FALSE if the user cancelled the save interactively or TRUE if the data was saved  (or attempted)
     */
    public boolean saveOutputDataToFile(OutputData outputdata, boolean askForNewName) {
        if (!askForNewName && !outputdata.isDirty()) return true; // we didn't really save because there was no need to...
        File file=null;
        String currentFileName=outputdata.getFileName();
        File parentDir=gui.getLastUsedDirectory();
        if (askForNewName || currentFileName==null) {          
            final JFileChooser fc = gui.getFileChooser(parentDir);// new JFileChooser(gui.getLastUsedDirectory());
            fc.setDialogTitle("Save "+outputdata.getName()); 
            if (currentFileName!=null) {
                File preselected=MotifLabEngine.getFile(parentDir,currentFileName);
                fc.setSelectedFile(preselected);
            }
            else {
                String suffix=outputdata.getPreferredFileSuffix();
                File preselected=MotifLabEngine.getFile(parentDir,outputdata.getName()+"."+suffix);
                fc.setSelectedFile(preselected);                
            }
            int returnValue=fc.showSaveDialog(this);
            if (returnValue==JFileChooser.APPROVE_OPTION) {
                file=fc.getSelectedFile();            
            } else return false; // user has cancelled the save
        } else {
           String filename=outputdata.getFileName();
           file=MotifLabEngine.getFile(parentDir,filename);
        }        
        if ((askForNewName || currentFileName==null) && file.exists()) {
            int choice=JOptionPane.showConfirmDialog(mainpanel, "Overwrite existing file \""+file.getName()+"\" ?","Save Output",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return false;
        }
        gui.setLastUsedDirectory(file.getParentFile());
        saveOutputDataToFile(outputdata, file);
        return true;
    }        
       

    /**
     * This method saves the selected protocol to the selected file
     * This should be called on the EDT because it might display some error messages
     * @param protocol
     * @param file
     * @return TRUE if the file was saved successfully or FALSE if an error occurred
     */
    private void saveOutputDataToFile(final OutputData outputdata, final File file) {
        final String filename=file.getName();
        final MainPanel parent=this;
        gui.statusMessage("Saving output file \""+filename+"\"");
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override 
            public Boolean doInBackground() {
                try {
                   outputdata.saveToFile(file,true,gui.getProgressMonitor(),engine);                
                } catch (Exception e) { 
                   ex=e;
                   return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                gui.setProgress(101); // just to hide the progressbar
                if (ex!=null) {
                    JOptionPane.showMessageDialog(parent, ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                    gui.logMessage("Unable to save output file \""+filename+"\"  ("+ex.getMessage()+")");
                } else {
                    parent.repaint();
                    setEnabledActions();
                    gui.logMessage("Saved output file \""+filename+"\"");
                }
            }
        }; // end of SwingWorker class
        worker.execute();              
    }    
    
    
       /** Saves the contents of all currently open tabs (but only Protocol scripts or Output objects) */
       public void saveAllTabs() {
            for (int i=0;i<getTabCount();i++) {
              Component tab=((CloseButtonTab)getTabComponentAt(i)).getContentComponent();
              if (tab instanceof ProtocolEditor) ((ProtocolEditor)tab).getProtocolManager().saveAll();
              else if (tab instanceof OutputPanel) saveOutputDataToFile(((OutputPanel)tab).getOutputData(),false);
            }
       }

    /** Returns true if any of the currently open protocols or output tabs are dirty */
    public boolean isAnyDirty() {
           for (int i=0;i<getTabCount();i++) {
              Component tab=((CloseButtonTab)getTabComponentAt(i)).getContentComponent();
              if (tab instanceof ProtocolEditor) {
                  if (((ProtocolEditor)tab).getProtocolManager().isAnyDirty()) return true;
              }
              else if (tab instanceof OutputPanel) {
                  if (((OutputPanel)tab).getOutputData().isDirty()) return true;
              }
            }
           return false;          
    }       
      
    
// -----------------------------------------------------------------------        
    public void dataAdded(Data data) {
        if (!(data instanceof OutputData)) return;
        if (data.isTemporary()) return; // do not show temporary data objects in tabs
        OutputPanel newpanel=new OutputPanel((OutputData)data,gui);
        addTab(data.getName(), newpanel, true);
        setupViewMenu();
        this.setSelectedComponent(newpanel);
    }



    public void dataRemoved(Data data) {
        if (!(data instanceof OutputData)) return;
        if (data.isTemporary()) return; // temporary data objects are not shown
        int tabs=this.getTabCount();
        for (int i=0;i<tabs;i++) {
           String tabname=this.getTitleAt(i);
           if (tabname.equals(data.getName())) {
               try {
                 this.removeTabAt(i);
               } catch (Exception e) {} // to avoid ClassCastException
               break;
           }
        }   
        setupViewMenu();
    }
    
    public void dataUpdated(Data data) {
       if (data instanceof OutputData && !data.isTemporary()) {
           setEnabledActions();
           repaint(); // updates dirtyFlag marks
       }
    } 
    
    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {} // empty interface implementation
    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) {} // empty interface implementation
    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {} // empty interface implementation 
// -----------------------------------------------------------------------           
        
        
/**
 * This function should be called whenever the number of tabs changes.
 * The purpose of this method is to populate the "View" menu in the top-level application menu with entries
 * corresponding to the current tabs collection.
 */ 
private void setupViewMenu() {   
    if (viewMenu==null) return;
    viewMenu.removeAll();
    int[] mnemonics = new int[]{KeyEvent.VK_1,KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4,KeyEvent.VK_5,KeyEvent.VK_6,KeyEvent.VK_7,KeyEvent.VK_8,KeyEvent.VK_9,KeyEvent.VK_0};
    int tabs=this.getTabCount();
    for (int i=0;i<tabs;i++) {
        String tabname=this.getTitleAt(i);
        JMenuItem menuitem;
        if (i<10) {        
           menuitem=new JMenuItem(tabname,mnemonics[i]);
           menuitem.setAccelerator(KeyStroke.getKeyStroke(mnemonics[i], InputEvent.ALT_DOWN_MASK));
           this.setMnemonicAt(i, mnemonics[i]);
        } else {
          menuitem=new JMenuItem(tabname);
        }
        final int index=i;
        menuitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               mainpanel.setSelectedIndex(index);
            }
        });
        viewMenu.add(menuitem);
    }
}        


private void setClosingDownTab(String closingdowntab) {
    closingDownNow=closingdowntab;
}
private String getClosingDownTab() {
    return closingDownNow;
}
        
  // -------------------------------------- FUNCTIONALITY RELATED TO CLOSE BUTTONS BELOW --------------      
	public void addTab(String title, Icon icon, Component component, String tip, boolean closeable) {
            int index=getOutputPanelTabIndex(component);          
            if (index>=2) insertTab(title, icon, component, tip, index); // restore previous position              
            else super.addTab(title, icon, component, tip);
            index = (index>=2)?index:(this.getTabCount() - 1);
            setTabComponentAt(index, new CloseButtonTab(component, title, icon,closeable));        
            setupViewMenu();
	}
	public void addTab(String title, Icon icon, Component component, boolean closeable) {
            addTab(title, icon, component, null, closeable);
	}

	public void addTab(String title, Component component, boolean closeable) {
            addTab(title, null, component,closeable);
	}
        @Override
	public void addTab(String title, Icon icon, Component component, String tip) {
             addTab(title, icon, component, tip,false);
	}
        @Override
	public void addTab(String title, Icon icon, Component component) {
            addTab(title, icon, component, false);
	}
        @Override
	public void addTab(String title, Component component) {
            addTab(title, component,false);
	}
        
        public void closeAllTabs() {
            RecordingCompoundEdit compoundEdit=new RecordingCompoundEdit("close all output panels");
            gui.getUndoManager().forwardUndoEvents(compoundEdit);
            ArrayList<CloseButtonTab> list=new ArrayList<CloseButtonTab>();
            for (int i=getTabCount()-1;i>=0;i--) { // remove in reverse order so they will be added back in correct order
               CloseButtonTab tab=(CloseButtonTab)getTabComponentAt(i);
               if (!(tab.getContentComponent() instanceof OutputPanel)) continue; // don't close Visualization and Protocol panels
               setOutputPanelTabIndex(tab.getContentComponent(), -1);
               list.add(tab);
            }
            for (CloseButtonTab tab:list) tab.closeTab();
            compoundEdit.end();
            gui.getUndoManager().forwardUndoEvents(null);
            gui.getUndoManager().addEdit(compoundEdit);             
       }
        
       private void setOutputPanelTabIndex(Component component, int index) {
           if (component instanceof OutputPanel) {
               ((OutputPanel)component).getOutputData().tabIndex=index;
           }
       }
       
       private int getOutputPanelTabIndex(Component component) {
           if (component instanceof OutputPanel) {
               return ((OutputPanel)component).getOutputData().tabIndex;
           } else return -1;
       }       

private class CloseButtonTab extends JPanel {
    private Component tab;
    private String mytitle;
    private OutputData outputdataobject=null;

    public CloseButtonTab(final Component tab, String title, Icon icon, boolean closeable) {
        this.tab = tab;
        this.mytitle=title;
        setOpaque(false);
        FlowLayout flowLayout = new FlowLayout(FlowLayout.CENTER, 3, 3);
        setLayout(flowLayout);
        setVisible(true);
        JLabel jLabel;
        if (tab instanceof OutputPanel) {
           outputdataobject=((OutputPanel)tab).getOutputData();
           jLabel = new DirtyLabel(title);
        } else {
           jLabel = new JLabel(title);
        }
        jLabel.setIcon(icon);
        add(jLabel);
        JButton button = new JButton(new MiscIcons(MiscIcons.CLOSE_ICON));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(null);
        button.setRolloverEnabled(false);
        button.addMouseListener(new MouseAdapter() {				
             @Override public void mouseClicked(MouseEvent e) {
                        closeTab();
                }
        });
        if (closeable) {
            add(new JLabel("  ")); // Just to get some margin between title and close button 
            add(button);
        }
    }

    public Component getContentComponent() {return tab;}

    private void closeTab() {
        String closingtab=mainpanel.getClosingDownTab();
        if (closingtab!=null) return; // we are already in the process of closing a tab. Do not create additional events
        if (outputdataobject.isDirty() && gui.doPromptBeforeDiscard()) {
            gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int response=JOptionPane.showConfirmDialog(mainpanel, "Save changes to "+outputdataobject.getName()+"?","Close output tab",JOptionPane.YES_NO_CANCEL_OPTION);
            if (response==JOptionPane.CANCEL_OPTION) {
                gui.getFrame().setCursor(Cursor.getDefaultCursor());
                return;
            }
            else if (response==JOptionPane.OK_OPTION) {
                if (!saveOutputDataToFile(outputdataobject, false)) {
                    gui.getFrame().setCursor(Cursor.getDefaultCursor());
                    return;
                }
            }
        }
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        CloseTabEvent closeEvent=new CloseTabEvent(mytitle);
        gui.getFrame().setCursor(Cursor.getDefaultCursor());
        gui.getUndoManager().addEdit(closeEvent);
    }

    /** A subclass of JLabel used for Tab-titles. Dirty output tabs are marked with an asterisk */
    private class DirtyLabel extends JLabel {
        public DirtyLabel(String title) {super(title);}
        @Override
        public void paintComponent(Graphics g) {
            if (outputdataobject.isDirty()) setText(mytitle+" *");
            else setText(mytitle);
            super.paintComponent(g);
        }
    }
                
} // end private class CloseButtonTab
        
        
 private class CloseTabEvent extends AbstractUndoableEdit {
        private String tabName;
        private int tabnr=2;
        private String cacheTicket=null;
        private boolean cacheOK=false;

        public CloseTabEvent(String tabname) {
            tabName=tabname;
            GUIUndoManager undoManager=gui.getUndoManager();
            Data data=engine.getDataItem(tabname);
            cacheTicket=undoManager.getUniqueUndoID();
            cacheOK=undoManager.storeObjectInCache(cacheTicket, data);
            int tabs=mainpanel.getTabCount();
            for (int i=0;i<tabs;i++) {
                String tabtitle=mainpanel.getTitleAt(i);
                if (tabtitle.equals(tabname)) {tabnr=i;break;}
            }
            Component comp=mainpanel.getTabComponentAt(tabnr);
            if (comp instanceof CloseButtonTab &&  ((CloseButtonTab)comp).getContentComponent() instanceof OutputPanel) {
                setOutputPanelTabIndex(((CloseButtonTab)comp).getContentComponent(),tabnr);
            }             
            mainpanel.setClosingDownTab(tabName); // we make a note that this tab is closing so that the next line will not create cyclic events
            try {engine.removeDataItem(tabName);} catch (ExecutionError e) {} // note !!! this will also remove the tab, which will again create a new event 
            mainpanel.setClosingDownTab(null);
        }        

        @Override
        public String getPresentationName() {
            return "close "+tabName;
        }

        @Override
        public void undo() throws CannotUndoException {
            if (!cacheOK) throw new CannotUndoException();
            super.undo();
            Data data=(Data)gui.getUndoManager().getObjectFromCache(cacheTicket);
            if (data instanceof OutputData) {
                ((OutputData)data).tabIndex=tabnr;
            }
            try {engine.storeDataItem(data); } catch (ExecutionError e) {JOptionPane.showMessageDialog(gui.getFrame(), "An error occurred during session restore:\n"+e.getMessage(),"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);}
            // this will also create a new tab through the callback event: dataAdded(Data data)
        }

        @Override
        public void redo() throws CannotRedoException {
            if (!cacheOK) throw new CannotRedoException();
            super.redo();
            mainpanel.setClosingDownTab(tabName);
            try {engine.removeDataItem(tabName);} catch (ExecutionError e) {} //
            mainpanel.setClosingDownTab(null);
        }

        @Override
        public boolean canRedo() {
            return super.canRedo();
            // return false; // if we always return false here we say that it is OK to undo tab closing (if one regrets) but not to redo it with the redo button
        }
      
        
 } // end private class CloseTabEvent

    /** Updates the 'enabled' status of save and close actions */
    private void setEnabledActions() {
       int i=getSelectedIndex();
       if (getTitleAt(i).equals("Visualization")) {
          gui.setCloseEnabled(false);  
          gui.setSaveEnabled(false);                  
          gui.setSaveAsEnabled(false);                  
       } else if (getTitleAt(i).equals("Protocol")) {
          Component tab=((CloseButtonTab)getTabComponentAt(i)).getContentComponent();
          Protocol protocol=null;
          if (tab instanceof ProtocolEditor) protocol=((ProtocolEditor)tab).getProtocol();
          if (protocol==null) {
              gui.setCloseEnabled(false);
              gui.setSaveEnabled(false); 
              gui.setSaveAsEnabled(false);                   
          } else {
              gui.setCloseEnabled(true);
              gui.setSaveEnabled(((StandardProtocol)protocol).isDirty()); 
              gui.setSaveAsEnabled(true);
              gui.setSearchTargetComponent(tab);
          }                              
       } else {
          Component tab=((CloseButtonTab)getTabComponentAt(i)).getContentComponent();
          OutputData data=null;
          if (tab instanceof OutputPanel) data=((OutputPanel)tab).getOutputData();
          if (data==null) {
              gui.setCloseEnabled(false);
              gui.setSaveEnabled(false); 
              gui.setSaveAsEnabled(false);                   
          } else {
              gui.setCloseEnabled(true);
              gui.setSaveEnabled(data.isDirty()); 
              gui.setSaveAsEnabled(true);
              gui.setSearchTargetComponent(tab);
          }                 
       }    
    }

    private class SelectedTabChangedListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            setEnabledActions();
        }    
    }

} // end class MainPanel


