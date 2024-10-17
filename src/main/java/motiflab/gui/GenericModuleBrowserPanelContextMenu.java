
package motiflab.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import motiflab.engine.MotifLabEngine;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import motiflab.engine.data.Data;
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.ModuleCollection;
import motiflab.engine.data.ModulePartition;



/**
 *
 * @author kjetikl
 */
public class GenericModuleBrowserPanelContextMenu extends JPopupMenu  {
    private static final String DISPLAY_MODULE="Display";
    private static final String SHOW="Show";
    private static final String HIDE="Hide";
    private static final String SHOW_ONLY_SELECTED="Show Only Selected";
    private static final String SHOW_ALL="Show All";
    private static final String HIDE_ALL="Hide All";
    private static final String COLOR_SUBMENU_HEADER="Set Color";
    private static final String SELECT_SHOWN_MODULES="Select Shown Modules";
    private static final String SELECT_ONLY_SHOWN_MODULES="Select Only Shown Modules";
    private static final String SELECT_MODULES_FROM="Select Modules From";
    private static final String SELECT_ONLY_MODULES_FROM="Select Only Modules From";
    private static final String CREATE_MODULE_COLLECTION="Create Module Collection";


    protected MotifLabEngine engine;
    protected MotifLabGUI gui;
    protected String[] selectedModuleNames=null;
    protected JMenuItem displayModule;
    protected GenericModuleBrowserPanel panel;
    private ArrayList<JMenuItem> limitedToOne=new ArrayList<JMenuItem>();
    private boolean extraAdded=false;
    private SelectFromCollectionListener selectFromCollectionListener;
    private ClearAndSelectFromCollectionListener clearAndselectFromCollectionListener;
    private JMenu selectModulesFromMenu;
    private JMenu selectOnlyModulesFromMenu;

    /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param panel
     * @param selection The selected row in the table
     */
    public GenericModuleBrowserPanelContextMenu(final GenericModuleBrowserPanel panel) {
        this(panel,true);
    }
        /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param panel
     * @param selection The selected row in the table
     * @param allowCreateCollections set to TRUE if "Create Collection" option should be added to the menu
     */
    public GenericModuleBrowserPanelContextMenu(final GenericModuleBrowserPanel panel, boolean allowCreateCollection) {
        this.gui=panel.getGUI();
        this.panel=panel;
        this.engine=gui.getEngine();

        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        JMenuItem showItem=new JMenuItem(SHOW);
        JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED);
        JMenuItem showAllItem=new JMenuItem(SHOW_ALL);
        JMenuItem hideItem=new JMenuItem(HIDE);
        JMenuItem hideAllItem=new JMenuItem(HIDE_ALL);
        JMenuItem selectShownModulesItem=new JMenuItem(SELECT_SHOWN_MODULES);
        JMenuItem selectOnlyShownModulesItem=new JMenuItem(SELECT_ONLY_SHOWN_MODULES);
        JMenuItem createCollectionItem=new JMenuItem(CREATE_MODULE_COLLECTION);

        selectModulesFromMenu=new JMenu(SELECT_MODULES_FROM);
        selectOnlyModulesFromMenu=new JMenu(SELECT_ONLY_MODULES_FROM);
        selectFromCollectionListener=new SelectFromCollectionListener();
        clearAndselectFromCollectionListener=new ClearAndSelectFromCollectionListener();

        showItem.addActionListener(menuItemListener);
        showOnlySelectedItem.addActionListener(menuItemListener);
        showAllItem.addActionListener(menuItemListener);
        hideItem.addActionListener(menuItemListener);
        hideAllItem.addActionListener(menuItemListener);
        selectShownModulesItem.addActionListener(menuItemListener);
        selectOnlyShownModulesItem.addActionListener(menuItemListener);
        if (allowCreateCollection) createCollectionItem.addActionListener(menuItemListener);
        ColorMenuListener colormenulistener=new ColorMenuListener() {
              public void newColorSelected(Color color) {
                if (color==null) return;
                gui.getVisualizationSettings().setFeatureColor(selectedModuleNames, color, true);
                panel.repaint();
             }
        };
        ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER,colormenulistener,panel);

        displayModule=new JMenuItem(DISPLAY_MODULE);
        displayModule.addActionListener(menuItemListener);
        this.add(displayModule);
        limitedToOne.add(displayModule);

        this.add(showItem);
        this.add(showOnlySelectedItem);
        this.add(showAllItem);
        this.add(hideItem);
        this.add(hideAllItem);
        this.add(colorMenu);
        this.add(new JSeparator());        
        this.add(selectShownModulesItem);
        this.add(selectOnlyShownModulesItem);
        this.add(selectModulesFromMenu);
        this.add(selectOnlyModulesFromMenu);
        if (allowCreateCollection) {        
            this.add(new JSeparator());        
            this.add(createCollectionItem);
        }
    }

    /**
     * Updates the menu to fit a specific selection
     * This should be called every time before the menu is shown
     */
    public boolean updateMenu() {
        selectedModuleNames=panel.getSelectedModuleNames();
        if (selectedModuleNames==null) return false;
        for (JMenuItem item:limitedToOne) {
            item.setEnabled(selectedModuleNames.length==1);
        }
        selectModulesFromMenu.removeAll();
        selectOnlyModulesFromMenu.removeAll();
        for (String collectionName:engine.getNamesForAllDataItemsOfType(ModuleCollection.class)) {
            JMenuItem subitem=new JMenuItem(collectionName);
            subitem.addActionListener(selectFromCollectionListener);
            selectModulesFromMenu.add(subitem);
            JMenuItem subitem2=new JMenuItem(collectionName);
            subitem2.addActionListener(clearAndselectFromCollectionListener);
            selectOnlyModulesFromMenu.add(subitem2);
        }
        for (String partitionName:engine.getNamesForAllDataItemsOfType(ModulePartition.class)) {
            Data data=engine.getDataItem(partitionName);
            if (data instanceof ModulePartition) {
                JMenu selectModulesFromMenuCluster=new JMenu(data.getName());               
                JMenu selectOnlyModulesFromMenuCluster=new JMenu(data.getName());               
                for (String cluster:((ModulePartition)data).getClusterNames()) {                    
                    JMenuItem subitem=new JMenuItem(cluster);
                    subitem.setActionCommand(partitionName+"."+cluster);
                    subitem.addActionListener(selectFromCollectionListener);
                    selectModulesFromMenuCluster.add(subitem);
                    JMenuItem subitem2=new JMenuItem(cluster);
                    subitem2.setActionCommand(partitionName+"."+cluster);
                    subitem2.addActionListener(clearAndselectFromCollectionListener);
                    selectOnlyModulesFromMenuCluster.add(subitem2);
                }
                selectModulesFromMenu.add(selectModulesFromMenuCluster);
                selectOnlyModulesFromMenu.add(selectOnlyModulesFromMenuCluster);
            }
        }
        selectModulesFromMenu.setEnabled(selectModulesFromMenu.getMenuComponentCount()>0);
        selectOnlyModulesFromMenu.setEnabled(selectOnlyModulesFromMenu.getMenuComponentCount()>0);
        if (selectedModuleNames.length==1) {
            displayModule.setText(DISPLAY_MODULE+" "+selectedModuleNames[0]);
            displayModule.setVisible(true);
        } else {            
            displayModule.setVisible(false);
        }          
        return true;
    }


   /**
     * Adds an additional menu item to the end of this
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one module is selected
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
           if (e.getActionCommand().startsWith(DISPLAY_MODULE)) {
                if (selectedModuleNames==null || selectedModuleNames.length<1) return;               
                Data cisRegModule=gui.getEngine().getDataItem(selectedModuleNames[0]);
                if (cisRegModule instanceof ModuleCRM) gui.showPrompt((ModuleCRM)cisRegModule, false, panel.isPanelModal());
           } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                panel.setVisibilityOnAllModules(false,false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedModuleNames!=null) {
                    for (String modulename:selectedModuleNames) {
                       settings.setRegionTypeVisible(modulename, true, false);
                    }
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(SHOW_ALL)) {
                panel.setVisibilityOnAllModules(true,true);
           } else if (e.getActionCommand().equals(HIDE_ALL)) {
                panel.setVisibilityOnAllModules(false,true);
           } else if (e.getActionCommand().equals(SHOW)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedModuleNames!=null) {
                    for (String modulename:selectedModuleNames) {
                       settings.setRegionTypeVisible(modulename, true, false);
                    }
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(HIDE)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedModuleNames!=null) {
                    for (String modulename:selectedModuleNames) {
                       settings.setRegionTypeVisible(modulename, false, false);
                    }
                }
                //panel.repaint();
                gui.redraw();
           } else if (e.getActionCommand().equals(CREATE_MODULE_COLLECTION)) {
                if (selectedModuleNames==null) return;
                ArrayList<String> list=new ArrayList<String>(selectedModuleNames.length);
                list.addAll(Arrays.asList(selectedModuleNames));
                gui.promptAndCreateModuleCollection(list);
           } else if (e.getActionCommand().equals(SELECT_SHOWN_MODULES)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allModules=engine.getNamesForAllDataItemsOfType(ModuleCRM.class);
                HashSet<String> shownModules=new HashSet<String>(allModules.size());
                for (String modulename:allModules) {
                   if (settings.isRegionTypeVisible(modulename)) shownModules.add(modulename);
                }
                selectRowsForModules(shownModules, false);
           } else if (e.getActionCommand().equals(SELECT_ONLY_SHOWN_MODULES)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allModules=engine.getNamesForAllDataItemsOfType(ModuleCRM.class);
                HashSet<String> shownModules=new HashSet<String>(allModules.size());
                for (String modulename:allModules) {
                   if (settings.isRegionTypeVisible(modulename)) shownModules.add(modulename);
                }
                selectRowsForModules(shownModules, true);
           }
       }

   }

    private void selectRowsForModules(HashSet<String> modules, boolean clearSelection) {
        JTable table=panel.getTable();
        if (clearSelection) table.clearSelection();
        int modulecolumn=panel.getModuleColumn();
        if (modulecolumn<0) return;
        ListSelectionModel selection=table.getSelectionModel();
        for (int i=0;i<table.getRowCount();i++) {
            Object value=table.getValueAt(i, modulecolumn);
            String modulename=((ModuleCRM)value).getName();
            if (modules.contains(modulename)) {
                selection.addSelectionInterval(i,i);
            }
        }
    }


   private void selectCollection(String collectionName, boolean clearCurrentSelection) {
       HashSet<String> modulenames=new HashSet<String>();
       int point=collectionName.indexOf('.');
       if (point>0) { // partition
           String clusterName=collectionName.substring(point+1);
           collectionName=collectionName.substring(0,point);
           Data col=engine.getDataItem(collectionName);
           if (col instanceof ModulePartition) modulenames.addAll( ((ModulePartition)col).getAllModuleNamesInCluster(clusterName) );

       } else { // collection
           Data col=engine.getDataItem(collectionName);
           if (col instanceof ModuleCollection) modulenames.addAll( ((ModuleCollection)col).getAllModuleNames());
       }
       selectRowsForModules(modulenames, clearCurrentSelection);
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
