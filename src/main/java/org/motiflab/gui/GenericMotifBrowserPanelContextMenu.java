
package org.motiflab.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import org.motiflab.engine.MotifLabEngine;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifPartition;



/**
 *
 * @author kjetikl
 */
public class GenericMotifBrowserPanelContextMenu extends JPopupMenu  {
    private static final String DISPLAY_MOTIF="Display";
    private static final String SHOW="Show";
    private static final String HIDE="Hide";
    private static final String SHOW_ONLY_SELECTED="Show Only Selected";   
    private static final String SHOW_ALL="Show All";  
    private static final String HIDE_ALL="Hide All";      
    private static final String COLOR_SUBMENU_HEADER="Set Color";
    private static final String SELECT_SHOWN_MOTIFS="Select Shown Motifs";
    private static final String SELECT_ONLY_SHOWN_MOTIFS="Select Only Shown Motifs";
    private static final String SELECT_MOTIFS_FROM="Select Motifs From"; 
    private static final String SELECT_ONLY_MOTIFS_FROM="Select Only Motifs From";
    private static final String CREATE_MOTIF_COLLECTION="Create Motif Collection";
    private static final String SAVE_MOTIF_LOGO="Save Motif Logo";
     

    protected MotifLabEngine engine;
    protected MotifLabGUI gui;
    protected String[] selectedMotifNames=null;
    protected JMenuItem displayMotif;
    protected JMenuItem compareMotifToOthers;   
    protected JMenuItem saveMotifLogo;
    protected GenericMotifBrowserPanel panel;
    private ArrayList<JMenuItem> limitedToOne=new ArrayList<JMenuItem>();
    private boolean extraAdded=false;
    private SelectFromCollectionListener selectFromCollectionListener;
    private ClearAndSelectFromCollectionListener clearAndselectFromCollectionListener;
    private JMenu selectMotifsFromMenu;   
    private JMenu selectOnlyMotifsFromMenu;   
    private ExternalDBLinkMenu dbmenu;
    
    /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param panel
     * @param selection The selected row in the table
     */
    public GenericMotifBrowserPanelContextMenu(final GenericMotifBrowserPanel panel) {
        this(panel,true);
    }     
    /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param panel
     * @param selection The selected row in the table
     * @param allowCreateCollections set to TRUE if "Create Collection" option should be added to the menu     * 
     */  
    public GenericMotifBrowserPanelContextMenu(final GenericMotifBrowserPanel panel, boolean allowCreateCollection) {
        this.gui=panel.getGUI();  
        this.panel=panel;
        this.engine=gui.getEngine();
         
        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        JMenuItem showItem=new JMenuItem(SHOW);
        JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED);   
        JMenuItem showAllItem=new JMenuItem(SHOW_ALL);               
        JMenuItem hideItem=new JMenuItem(HIDE);
        JMenuItem hideAllItem=new JMenuItem(HIDE_ALL);
        JMenuItem selectShownMotifsItem=new JMenuItem(SELECT_SHOWN_MOTIFS);
        JMenuItem selectOnlyShownMotifsItem=new JMenuItem(SELECT_ONLY_SHOWN_MOTIFS);
        JMenuItem createCollectionItem=new JMenuItem(CREATE_MOTIF_COLLECTION);    

        dbmenu=new ExternalDBLinkMenu(null, gui);
        selectMotifsFromMenu=new JMenu(SELECT_MOTIFS_FROM);
        selectOnlyMotifsFromMenu=new JMenu(SELECT_ONLY_MOTIFS_FROM);
        selectFromCollectionListener=new SelectFromCollectionListener();
        clearAndselectFromCollectionListener=new ClearAndSelectFromCollectionListener();
        

        showItem.addActionListener(menuItemListener);
        showOnlySelectedItem.addActionListener(menuItemListener);
        showAllItem.addActionListener(menuItemListener);
        hideItem.addActionListener(menuItemListener);
        hideAllItem.addActionListener(menuItemListener);     
        selectShownMotifsItem.addActionListener(menuItemListener);
        selectOnlyShownMotifsItem.addActionListener(menuItemListener);
        if (allowCreateCollection) createCollectionItem.addActionListener(menuItemListener);     
        ColorMenuListener colormenulistener=new ColorMenuListener() {
              public void newColorSelected(Color color) {
                if (color==null) return;
                gui.getVisualizationSettings().setFeatureColor(selectedMotifNames, color, true);
                panel.repaint();
             }
        };
        ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER,colormenulistener,panel);

        displayMotif=new JMenuItem(DISPLAY_MOTIF);     
        displayMotif.addActionListener(menuItemListener);
        this.add(displayMotif);                                       
        limitedToOne.add(displayMotif);
        
        this.add(showItem);
        this.add(showOnlySelectedItem); 
        this.add(showAllItem);            
        this.add(hideItem);   
        this.add(hideAllItem);              
        this.add(colorMenu);
        this.add(new JSeparator());        
        this.add(selectShownMotifsItem);
        this.add(selectOnlyShownMotifsItem);
        this.add(selectMotifsFromMenu);
        this.add(selectOnlyMotifsFromMenu);
        this.add(new JSeparator());        
        if (allowCreateCollection) {
            this.add(createCollectionItem);
        }
        compareMotifToOthers=new JMenuItem("Compare");     
        compareMotifToOthers.addActionListener(menuItemListener);
        this.add(compareMotifToOthers);     
        limitedToOne.add(compareMotifToOthers);   
        
        saveMotifLogo=new JMenuItem(SAVE_MOTIF_LOGO);
        saveMotifLogo.addActionListener(menuItemListener);        
        this.add(saveMotifLogo);      
        limitedToOne.add(saveMotifLogo);
        
        this.add(dbmenu);
        limitedToOne.add(dbmenu);
    }
    
    /**
     * Updates the menu to fit a specific selection
     * This should be called every time before the menu is shown
     */
    public boolean updateMenu() {
        selectedMotifNames=panel.getSelectedMotifNames();
        if (selectedMotifNames==null) return false;
        for (JMenuItem item:limitedToOne) {
            item.setEnabled(selectedMotifNames.length==1);
        }
        selectMotifsFromMenu.removeAll();
        selectOnlyMotifsFromMenu.removeAll();  
        for (String collectionName:engine.getNamesForAllDataItemsOfType(MotifCollection.class)) {
            JMenuItem subitem=new JMenuItem(collectionName);
            subitem.addActionListener(selectFromCollectionListener);
            selectMotifsFromMenu.add(subitem);
            JMenuItem subitem2=new JMenuItem(collectionName);
            subitem2.addActionListener(clearAndselectFromCollectionListener);
            selectOnlyMotifsFromMenu.add(subitem2);
        }      
        for (String partitionName:engine.getNamesForAllDataItemsOfType(MotifPartition.class)) {
            Data data=engine.getDataItem(partitionName);
            if (data instanceof MotifPartition) {
                JMenu selectMotifsFromMenuCluster=new JMenu(data.getName());               
                JMenu selectOnlyMotifsFromMenuCluster=new JMenu(data.getName());               
                for (String cluster:((MotifPartition)data).getClusterNames()) {                    
                    JMenuItem subitem=new JMenuItem(cluster);
                    subitem.setActionCommand(partitionName+"."+cluster);
                    subitem.addActionListener(selectFromCollectionListener);
                    selectMotifsFromMenuCluster.add(subitem);
                    JMenuItem subitem2=new JMenuItem(cluster);
                    subitem2.setActionCommand(partitionName+"."+cluster);
                    subitem2.addActionListener(clearAndselectFromCollectionListener);
                    selectOnlyMotifsFromMenuCluster.add(subitem2);
                }
                selectMotifsFromMenu.add(selectMotifsFromMenuCluster);
                selectOnlyMotifsFromMenu.add(selectOnlyMotifsFromMenuCluster);
            }
        }    
        selectMotifsFromMenu.setEnabled(selectMotifsFromMenu.getMenuComponentCount()>0);
        selectOnlyMotifsFromMenu.setEnabled(selectOnlyMotifsFromMenu.getMenuComponentCount()>0);
        dbmenu.updateMenu((selectedMotifNames.length==1)?selectedMotifNames[0]:null,true);
        if (selectedMotifNames.length==1) {
            displayMotif.setText(DISPLAY_MOTIF+" "+selectedMotifNames[0]);
            displayMotif.setVisible(true);
            compareMotifToOthers.setText("Compare "+selectedMotifNames[0]+" To Other Motifs");
            compareMotifToOthers.setVisible(true);
            saveMotifLogo.setVisible(true);
            dbmenu.setVisible(true);
        } else {            
            displayMotif.setVisible(false);
            compareMotifToOthers.setVisible(true);
            saveMotifLogo.setVisible(true);
            dbmenu.setVisible(false);
        }
        return true;
    }
    
    
   /**
     * Adds an additional menu item to the end of this 
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one motif is selected
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
    
       
     /**
     * An inner class that listens to popup-menu events NOT related to the operations submenu and notifies the gui events
     */       
    private class DisplayMenuItemListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           if (e.getActionCommand().startsWith(DISPLAY_MOTIF)) {
                if (selectedMotifNames==null || selectedMotifNames.length<1) return;
                Data motif=gui.getEngine().getDataItem(selectedMotifNames[0]);
                if (motif instanceof Motif) gui.showPrompt((Motif)motif, false, panel.isPanelModal());
           } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                panel.setVisibilityOnAllMotifs(false,false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedMotifNames!=null) {
                    for (String motifname:selectedMotifNames) {
                       settings.setRegionTypeVisible(motifname, true, false);   
                    }
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(SHOW_ALL)) {
                panel.setVisibilityOnAllMotifs(true,true);                       
           } else if (e.getActionCommand().equals(HIDE_ALL)) {
                panel.setVisibilityOnAllMotifs(false,true);
           } else if (e.getActionCommand().equals(SHOW)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedMotifNames!=null) {
                    for (String motifname:selectedMotifNames) {
                       settings.setRegionTypeVisible(motifname, true, false);   
                    }
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(HIDE)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedMotifNames!=null) {                
                    for (String motifname:selectedMotifNames) {
                       settings.setRegionTypeVisible(motifname, false, false);   
                    }
                }
                //panel.repaint();
                gui.redraw();
           } else if (e.getActionCommand().equals(CREATE_MOTIF_COLLECTION)) {
                if (selectedMotifNames==null) return;               
                ArrayList<String> list=new ArrayList<String>(selectedMotifNames.length);
                list.addAll(Arrays.asList(selectedMotifNames));
                gui.promptAndCreateMotifCollection(list);
           } else if (e.getActionCommand().equals(SELECT_SHOWN_MOTIFS)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allMotifs=engine.getNamesForAllDataItemsOfType(Motif.class);
                HashSet<String> shownMotifs=new HashSet<String>(allMotifs.size());
                for (String motifname:allMotifs) {
                   if (settings.isRegionTypeVisible(motifname)) shownMotifs.add(motifname);
                }
                selectRowsForMotifs(shownMotifs, false);
           } else if (e.getActionCommand().equals(SELECT_ONLY_SHOWN_MOTIFS)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allMotifs=engine.getNamesForAllDataItemsOfType(Motif.class);
                HashSet<String> shownMotifs=new HashSet<String>(allMotifs.size());
                for (String motifname:allMotifs) {
                   if (settings.isRegionTypeVisible(motifname)) shownMotifs.add(motifname);
                }
                selectRowsForMotifs(shownMotifs, true);
           } else if (e.getActionCommand().equals(SAVE_MOTIF_LOGO)) {
                if (selectedMotifNames==null || selectedMotifNames.length<1) return;
                Data motif=gui.getEngine().getDataItem(selectedMotifNames[0]);
                if (motif instanceof Motif) {
                    SaveMotifLogoImageDialog saveLogoPanel=new SaveMotifLogoImageDialog(gui, (Motif)motif, panel.isPanelModal());
                    int x=gui.getGUIFrame().getWidth()/2-saveLogoPanel.getWidth()/2; if (x<0) x=0;
                    int y=gui.getGUIFrame().getHeight()/2-saveLogoPanel.getHeight()/2; if (y<0) y=0;
                    saveLogoPanel.setLocation(x, y);
                    saveLogoPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    saveLogoPanel.setVisible(true);                    
                }
           } else if (e.getActionCommand().startsWith("Compare")) {
                if (selectedMotifNames==null || selectedMotifNames.length<1) return;
                Data motif=gui.getEngine().getDataItem(selectedMotifNames[0]);
                if (motif instanceof Motif) {
                    MotifComparisonDialog motifcomparisonPanel=new MotifComparisonDialog(gui, (Motif)motif, panel.isPanelModal());
                    int x=gui.getGUIFrame().getWidth()/2-motifcomparisonPanel.getWidth()/2; if (x<0) x=0;
                    int y=gui.getGUIFrame().getHeight()/2-motifcomparisonPanel.getHeight()/2; if (y<0) y=0;
                    motifcomparisonPanel.setLocation(x, y);                    
                    motifcomparisonPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    motifcomparisonPanel.setVisible(true);                   
                }               
           }
       }           

   }
    
    private void selectRowsForMotifs(HashSet<String> motifs, boolean clearSelection) {
        JTable table=panel.getTable();
        if (clearSelection) table.clearSelection();
        int motifcolumn=panel.getMotifColumn();
        if (motifcolumn<0) return;
        ListSelectionModel selection=table.getSelectionModel();        
        for (int i=0;i<table.getRowCount();i++) {
            Object value=table.getValueAt(i, motifcolumn);
            if (value==null) continue; // this can happen if for some reason there are rows for motifs that do not exist
            String motifname=((Motif)value).getName();
            if (motifs.contains(motifname)) {
                selection.addSelectionInterval(i,i); 
            }
        }
    }  
    
    
   private void selectCollection(String collectionName, boolean clearCurrentSelection) {
       HashSet<String> motifnames=new HashSet<String>();
       int point=collectionName.indexOf('.');
       if (point>0) { // partition
           String clusterName=collectionName.substring(point+1);
           collectionName=collectionName.substring(0,point);
           Data col=engine.getDataItem(collectionName);
           if (col instanceof MotifPartition) motifnames.addAll( ((MotifPartition)col).getAllMotifNamesInCluster(clusterName) );

       } else { // collection
           Data col=engine.getDataItem(collectionName);
           if (col instanceof MotifCollection) motifnames.addAll( ((MotifCollection)col).getAllMotifNames());
       }
       selectRowsForMotifs(motifnames, clearCurrentSelection);
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

        
}
