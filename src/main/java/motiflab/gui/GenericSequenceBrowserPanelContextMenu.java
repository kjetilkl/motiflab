
package motiflab.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.SequencePartition;

/**
 *
 * @author kjetikl
 */
public class GenericSequenceBrowserPanelContextMenu extends JPopupMenu  {
    private static final String DISPLAY_SEQUENCE="Display";    
    private static final String SHOW="Show Sequences";
    private static final String HIDE="Hide Sequences";
    private static final String SHOW_ONLY_SELECTED="Show Only Selected";
    private static final String SHOW_ALL="Show All Sequences";
    private static final String HIDE_ALL="Hide All Sequences";
    private static final String COLOR_SUBMENU_HEADER="Set Label Color";
    private static final String MARK_SEQUENCES="Mark Sequences";
    private static final String SELECT_SHOWN_SEQUENCES="Select Shown Sequences";
    private static final String SELECT_ONLY_SHOWN_SEQUENCES="Select Only Shown Sequences";    
    private static final String SELECT_MARKED_SEQUENCES="Select Marked Sequences";
    private static final String SELECT_ONLY_MARKED_SEQUENCES="Select Only Marked Sequences";    
    private static final String SELECT_SEQUENCES_FROM="Select Sequences From";      
    private static final String SELECT_ONLY_SEQUENCES_FROM="Select Only Sequences From";     
    private static final String CREATE_SEQUENCE_COLLECTION="Create Sequence Collection";

    protected String[] selectedSequenceNames=null;
    protected JMenu selectSequencesFromMenu;
    protected JMenu selectOnlySequencesFromMenu;    
    protected JMenuItem displaySequence;    
    private SelectFromCollectionListener selectFromCollectionListener;
    private ClearAndSelectFromCollectionListener clearAndselectFromCollectionListener;    
    protected MotifLabGUI gui;
    protected MotifLabEngine engine;
    protected GenericSequenceBrowserPanel panel;
    private ArrayList<JMenuItem> limitedToOne=new ArrayList<JMenuItem>();    
    private boolean extraAdded=false;    
    private ExternalDBLinkMenu dbmenu;
    
   /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param client
     * @param table
     * @param sequenceColumn the index of the column in the table that contains the sequence name
     */
    public GenericSequenceBrowserPanelContextMenu(GenericSequenceBrowserPanel panel) {
        this(panel,true);
    }
  /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param client
     * @param table
     * @param sequenceColumn the index of the column in the table that contains the sequence name
     * @param allowCreateCollections set to TRUE if "Create Collection" option should be added to the menu
     */
    public GenericSequenceBrowserPanelContextMenu(GenericSequenceBrowserPanel panel, boolean allowCreateCollections) {
        this.panel=panel;
        this.gui=panel.getGUI();  
        this.engine=gui.getEngine();
        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        JMenuItem showItem=new JMenuItem(SHOW);
        JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED);
        JMenuItem showAllItem=new JMenuItem(SHOW_ALL);
        JMenuItem hideItem=new JMenuItem(HIDE);
        JMenuItem hideAllItem=new JMenuItem(HIDE_ALL);
        JMenuItem selectShownSequencesItem=new JMenuItem(SELECT_SHOWN_SEQUENCES);
        JMenuItem selectOnlyShownSequencesItem=new JMenuItem(SELECT_ONLY_SHOWN_SEQUENCES);
        JMenuItem selectMarkedSequencesItem=new JMenuItem(SELECT_MARKED_SEQUENCES);
        JMenuItem selectOnlyMarkedSequencesItem=new JMenuItem(SELECT_ONLY_MARKED_SEQUENCES);
        selectSequencesFromMenu=new JMenu(SELECT_SEQUENCES_FROM);
        selectOnlySequencesFromMenu=new JMenu(SELECT_ONLY_SEQUENCES_FROM);        
        JMenuItem markSequencesItem=new JMenuItem(MARK_SEQUENCES);
        JMenuItem createCollectionItem=new JMenuItem(CREATE_SEQUENCE_COLLECTION);
        dbmenu=new ExternalDBLinkMenu(null, gui);        

        selectFromCollectionListener=new SelectFromCollectionListener(); 
        clearAndselectFromCollectionListener=new ClearAndSelectFromCollectionListener();         
        
        showItem.addActionListener(menuItemListener);
        showOnlySelectedItem.addActionListener(menuItemListener);
        showAllItem.addActionListener(menuItemListener);
        hideItem.addActionListener(menuItemListener);
        hideAllItem.addActionListener(menuItemListener);
        selectShownSequencesItem.addActionListener(menuItemListener);
        selectOnlyShownSequencesItem.addActionListener(menuItemListener);
        selectMarkedSequencesItem.addActionListener(menuItemListener);  
        selectOnlyMarkedSequencesItem.addActionListener(menuItemListener);  
        markSequencesItem.addActionListener(menuItemListener);
        if (allowCreateCollections) createCollectionItem.addActionListener(menuItemListener);        
        ColorMenuListener colormenulistener=new ColorMenuListener() {
              @Override
              public void newColorSelected(Color color) {
                if (color==null) return;
                gui.getVisualizationSettings().setSequenceLabelColor(selectedSequenceNames, color);
                gui.redraw();
             }
        };
        ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER,colormenulistener,panel);

        displaySequence=new JMenuItem(DISPLAY_SEQUENCE);     
        displaySequence.addActionListener(menuItemListener);
        this.add(displaySequence);                                       
        limitedToOne.add(displaySequence);
        
        this.add(showItem);
        this.add(showOnlySelectedItem);
        this.add(showAllItem);
        this.add(hideItem);
        this.add(hideAllItem);
        this.add(markSequencesItem);        
        this.add(colorMenu);
        this.add(new JSeparator());
        this.add(selectShownSequencesItem);
        this.add(selectOnlyShownSequencesItem);        
        this.add(selectMarkedSequencesItem);
        this.add(selectOnlyMarkedSequencesItem);
        this.add(selectSequencesFromMenu);
        this.add(selectOnlySequencesFromMenu);  
        this.add(new JSeparator());        
        if (allowCreateCollections) {
            this.add(createCollectionItem);
        }
        this.add(dbmenu);                                       
        limitedToOne.add(dbmenu);        
    }

    /**
     * Updates the menu to fit a specific selection
     * This should be called every time before the menu is shown
     */
    public boolean updateMenu() {
        selectedSequenceNames=panel.getSelectedSequenceNames();
        if (selectedSequenceNames==null) return false;
        for (JMenuItem item:limitedToOne) {
            item.setEnabled(selectedSequenceNames.length==1);
        }
        selectSequencesFromMenu.removeAll();
        selectOnlySequencesFromMenu.removeAll();        
        for (String collectionName:engine.getNamesForAllDataItemsOfType(SequenceCollection.class)) {
            JMenuItem subitem=new JMenuItem(collectionName);
            subitem.addActionListener(selectFromCollectionListener);
            selectSequencesFromMenu.add(subitem);
            JMenuItem subitem2=new JMenuItem(collectionName);
            subitem2.addActionListener(clearAndselectFromCollectionListener);
            selectOnlySequencesFromMenu.add(subitem2);
        }      
        for (String partitionName:engine.getNamesForAllDataItemsOfType(SequencePartition.class)) {
            Data data=engine.getDataItem(partitionName);
            if (data instanceof SequencePartition) {
                JMenu selectSequencesFromMenuCluster=new JMenu(data.getName());               
                JMenu selectOnlySequencesFromMenuCluster=new JMenu(data.getName());               
                for (String cluster:((SequencePartition)data).getClusterNames()) {                    
                    JMenuItem subitem=new JMenuItem(cluster);
                    subitem.setActionCommand(partitionName+"."+cluster);
                    subitem.addActionListener(selectFromCollectionListener);
                    selectSequencesFromMenuCluster.add(subitem);
                    JMenuItem subitem2=new JMenuItem(cluster);
                    subitem2.setActionCommand(partitionName+"."+cluster);
                    subitem2.addActionListener(clearAndselectFromCollectionListener);
                    selectOnlySequencesFromMenuCluster.add(subitem2);
                }
                selectSequencesFromMenu.add(selectSequencesFromMenuCluster);
                selectOnlySequencesFromMenu.add(selectOnlySequencesFromMenuCluster);
            }
        }    
        selectSequencesFromMenu.setEnabled(selectSequencesFromMenu.getMenuComponentCount()>0);
        selectOnlySequencesFromMenu.setEnabled(selectOnlySequencesFromMenu.getMenuComponentCount()>0);
        dbmenu.updateMenu((selectedSequenceNames.length==1)?selectedSequenceNames[0]:null,true);
        if (selectedSequenceNames.length==1) {
            displaySequence.setText(DISPLAY_SEQUENCE+" "+selectedSequenceNames[0]);
            displaySequence.setVisible(true);
            dbmenu.setVisible(true);
        } else {            
            displaySequence.setVisible(false);
            dbmenu.setVisible(false);
        }        
        return true;
    }

    
   /**
     * Adds an additional menu item to the end of this 
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one sequence is selected
     * @param callback 
     */ 
   public void addExtraMenuItem(String name, boolean limitOne, ActionListener callback) {
       JMenuItem item=new JMenuItem(name);
       item.addActionListener(callback);
       //if (!extraAdded) this.add(new javax.swing.JSeparator());
       this.add(item);   
       if (limitOne) limitedToOne.add(item);
       extraAdded=true;
   }


    private void selectRowsForSequences(HashSet<String> sequencenames, boolean clearSelection) {
        JTable table=panel.getTable();        
        if (clearSelection) table.clearSelection();
        int sequenceColumn=panel.getSequenceColumn();
        if (sequenceColumn<0) return;
        ListSelectionModel selection=table.getSelectionModel();        
        for (int i=0;i<table.getRowCount();i++) {
            Object value=table.getValueAt(i, sequenceColumn);      
            String sequencename=((Sequence)value).getName();                 
            if (sequencenames.contains(sequencename)) {
                selection.addSelectionInterval(i,i); 
            }
        }
    }     
    
   private void selectCollection(String collectionName, boolean clearCurrentSelection) {
       HashSet<String> sequencenames=new HashSet<String>();
       int point=collectionName.indexOf('.');
       if (point>0) { // partition
           String clusterName=collectionName.substring(point+1);
           collectionName=collectionName.substring(0,point);
           Data col=engine.getDataItem(collectionName);
           if (col instanceof SequencePartition) sequencenames.addAll( ((SequencePartition)col).getAllSequenceNamesInCluster(clusterName) );

       } else { // collection
           Data col=engine.getDataItem(collectionName);
           if (col instanceof SequenceCollection) sequencenames.addAll( ((SequenceCollection)col).getAllSequenceNames());
       }
       selectRowsForSequences(sequencenames, clearCurrentSelection);
   }

   private class SelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectCollection(collectionName,false);
       }             
   }
   private class ClearAndSelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectCollection(collectionName,true);
       }
   }  
   
     /**
     * An inner class that listens to popup-menu events NOT related to the operations submenu and notifies the gui events
     */
    private class DisplayMenuItemListener implements ActionListener {
       @Override
       public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().startsWith(DISPLAY_SEQUENCE)) {
                if (selectedSequenceNames==null || selectedSequenceNames.length<1) return;
                Data sequence=gui.getEngine().getDataItem(selectedSequenceNames[0]);
                if (sequence instanceof Sequence) gui.showPrompt((Sequence)sequence, false, panel.isPanelModal());
           } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) { // this needs some work
                if (selectedSequenceNames==null) return;
                panel.setVisibilityOnAllSequences(false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.setSequenceVisible(selectedSequenceNames, true);
           } else if (e.getActionCommand().equals(SHOW_ALL)) {
                panel.setVisibilityOnAllSequences(true);
           } else if (e.getActionCommand().equals(HIDE_ALL)) {
                panel.setVisibilityOnAllSequences(false);
           } else if (e.getActionCommand().equals(SHOW)) {
               if (selectedSequenceNames==null) return;
               VisualizationSettings settings=gui.getVisualizationSettings();
               settings.setSequenceVisible(selectedSequenceNames, true);
           } else if (e.getActionCommand().equals(HIDE)) {
               if (selectedSequenceNames==null) return;               
               VisualizationSettings settings=gui.getVisualizationSettings();
               settings.setSequenceVisible(selectedSequenceNames, false);
           } else if (e.getActionCommand().equals(MARK_SEQUENCES)) {
                if (selectedSequenceNames==null) return;               
                VisualizationSettings settings=gui.getVisualizationSettings();
                //settings.clearSequenceMarks();
                for (String seq:selectedSequenceNames) settings.setSequenceMarked(seq, true);
                gui.redraw();
           } else if (e.getActionCommand().equals(CREATE_SEQUENCE_COLLECTION)) {
                if (selectedSequenceNames==null) return;               
                ArrayList<String> list=new ArrayList<String>(selectedSequenceNames.length);
                list.addAll(Arrays.asList(selectedSequenceNames));
                gui.promptAndCreateSequenceCollection(list);
           } else if (e.getActionCommand().equals(SELECT_SHOWN_SEQUENCES)) {
                VisualizationSettings settings=gui.getVisualizationSettings(); 
                ArrayList<String> allSequences=engine.getNamesForAllDataItemsOfType(Sequence.class);
                HashSet<String> shownSequences=new HashSet<String>(allSequences.size());
                for (String sequencename:allSequences) {
                   if (settings.isSequenceVisible(sequencename)) shownSequences.add(sequencename);
                }     
                selectRowsForSequences(shownSequences,false); 
           } else if (e.getActionCommand().equals(SELECT_ONLY_SHOWN_SEQUENCES)) {
                VisualizationSettings settings=gui.getVisualizationSettings(); 
                ArrayList<String> allSequences=engine.getNamesForAllDataItemsOfType(Sequence.class);
                HashSet<String> shownSequences=new HashSet<String>(allSequences.size());
                for (String sequencename:allSequences) {
                   if (settings.isSequenceVisible(sequencename)) shownSequences.add(sequencename);
                }     
                selectRowsForSequences(shownSequences,true); 
           } else if (e.getActionCommand().equals(SELECT_MARKED_SEQUENCES)) {
                HashSet<String> marked=gui.getVisualizationSettings().getMarkedSequences();
                if (marked!=null) selectRowsForSequences(marked,false); 
           } else if (e.getActionCommand().equals(SELECT_ONLY_MARKED_SEQUENCES)) {
                HashSet<String> marked=gui.getVisualizationSettings().getMarkedSequences();
                if (marked!=null) selectRowsForSequences(marked,true); 
           }
       }

    }

}
