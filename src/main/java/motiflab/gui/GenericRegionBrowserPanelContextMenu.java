
package motiflab.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import motiflab.engine.MotifLabEngine;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JSeparator;



/**
 *
 * @author kjetikl
 */
public class GenericRegionBrowserPanelContextMenu extends JPopupMenu  {
    private static final String SHOW="Show";
    private static final String SHOW_ONLY_SELECTED="Show Only Selected";
    private static final String SHOW_ALL="Show All";    
    private static final String HIDE="Hide";    
    private static final String HIDE_ALL="Hide All";    
    private static final String COLOR_SUBMENU_HEADER="Set Color";
    private static final String CREATE_COLLECTION="Create Collection";


    protected MotifLabEngine engine;
    protected MotifLabGUI gui;
    protected String[] selectedRegionNames=null;
    protected GenericRegionBrowserPanel panel;
    private ArrayList<JMenuItem> limitedToOne=new ArrayList<JMenuItem>();
    private boolean extraAdded=false;
 
    /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param panel
     * @param selection The selected row in the table
     */
    public GenericRegionBrowserPanelContextMenu(final GenericRegionBrowserPanel panel) {
        this.gui=panel.getGUI();  
        this.panel=panel;
        this.engine=gui.getEngine();
         
        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        JMenuItem showItem=new JMenuItem(SHOW);
        JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED);   
        JMenuItem showAllItem=new JMenuItem(SHOW_ALL);               
        JMenuItem hideItem=new JMenuItem(HIDE);
        JMenuItem hideAllItem=new JMenuItem(HIDE_ALL);
        JMenuItem createCollectionItem=new JMenuItem(CREATE_COLLECTION);

        showItem.addActionListener(menuItemListener);
        showOnlySelectedItem.addActionListener(menuItemListener);
        showAllItem.addActionListener(menuItemListener);
        hideItem.addActionListener(menuItemListener);
        hideAllItem.addActionListener(menuItemListener);  
        createCollectionItem.addActionListener(menuItemListener);     
        ColorMenuListener colormenulistener=new ColorMenuListener() {
              public void newColorSelected(Color color) {
                if (color==null) return;
                gui.getVisualizationSettings().setFeatureColor(selectedRegionNames, color, true);
                panel.repaint();
             }
        };
        ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER,colormenulistener,panel);

        
        this.add(showItem);
        this.add(showOnlySelectedItem); 
        this.add(showAllItem);            
        this.add(hideItem);   
        this.add(hideAllItem);             
        this.add(colorMenu);
        this.add(new JSeparator());        
        this.add(createCollectionItem);
    }
    
    /**
     * Updates the menu to fit a specific selection
     * This should be called every time before the menu is shown
     */
    public boolean updateMenu() {
        selectedRegionNames=panel.getSelectedRegionNames();
        if (selectedRegionNames==null) return false;
        for (JMenuItem item:limitedToOne) {
            item.setEnabled(selectedRegionNames.length==1);
        }
        return true;
    }
    
    
   /**
     * Adds an additional menu item to the end of this 
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one region is selected
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
           if (e.getActionCommand().equals(SHOW)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedRegionNames!=null) {
                    for (String regionname:selectedRegionNames) {
                       settings.setRegionTypeVisible(regionname, true, false);   
                    }
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(HIDE)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedRegionNames!=null) {                
                    for (String regionname:selectedRegionNames) {
                       settings.setRegionTypeVisible(regionname, false, false);   
                    }
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                panel.setVisibilityOnAllRows(false,false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                if (selectedRegionNames!=null) {
                    for (String regionname:selectedRegionNames) {
                       settings.setRegionTypeVisible(regionname, true, false);   
                    }
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(SHOW_ALL)) {
                panel.setVisibilityOnAllRows(true,true);                       
           } else if (e.getActionCommand().equals(HIDE_ALL)) {
                panel.setVisibilityOnAllRows(false,true);
           } else if (e.getActionCommand().equals(CREATE_COLLECTION)) {
                if (selectedRegionNames==null) return;               
                ArrayList<String> list=new ArrayList<String>(selectedRegionNames.length);
                list.addAll(Arrays.asList(selectedRegionNames));
                gui.promptAndCreateTextVariable(list);
           } 
       }           

   }
    
        
}
