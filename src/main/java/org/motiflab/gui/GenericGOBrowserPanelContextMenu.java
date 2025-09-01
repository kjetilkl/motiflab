
package org.motiflab.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import org.motiflab.engine.MotifLabEngine;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JSeparator;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.Sequence;



/**
 *
 * @author kjetikl
 */
public class GenericGOBrowserPanelContextMenu extends JPopupMenu  {
    private static final String SHOW="Show";
    private static final String SHOW_ONLY_SELECTED="Show Only";
    private static final String SHOW_ALL="Show All";    
    private static final String HIDE="Hide";    
    private static final String HIDE_ALL="Hide All";    
    private static final String COLOR_SUBMENU_HEADER="Set Color";
    private static final String CREATE_COLLECTION="Create Collection";


    protected MotifLabEngine engine;
    protected MotifLabGUI gui;
    protected String[] selectedGOterms=null;
    protected GenericGOBrowserPanel panel;
    private ArrayList<JMenuItem> limitedToOne=new ArrayList<JMenuItem>();
    private boolean extraAdded=false;
    private Class datatype=null;
 
    /**
     * Returns a context menu applicable for the selected rows (this should not be NULL!)
     * @param panel
     * @param selection The selected row in the table
     */
    public GenericGOBrowserPanelContextMenu(final GenericGOBrowserPanel panel, Class datatype) {
        this.gui=panel.getGUI();  
        this.panel=panel;
        this.engine=gui.getEngine();
        this.datatype=datatype;
        String typename="";
        if (datatype==Sequence.class) typename="Sequences";
        else if (datatype==Motif.class) typename="Motifs";
        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        String suffix=(" "+typename+" with selected GO terms");
        if (datatype!=null) {
            JMenuItem showItem=new JMenuItem(SHOW+suffix);
            JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED+suffix);   
            JMenuItem showAllItem=new JMenuItem(SHOW_ALL+" "+typename);               
            JMenuItem hideItem=new JMenuItem(HIDE+suffix);
            JMenuItem hideAllItem=new JMenuItem(HIDE_ALL+" "+typename);     
            showItem.addActionListener(menuItemListener);
            showOnlySelectedItem.addActionListener(menuItemListener);
            showAllItem.addActionListener(menuItemListener);
            hideItem.addActionListener(menuItemListener);
            hideAllItem.addActionListener(menuItemListener);  
            ColorMenuListener colormenulistener=new ColorMenuListener() {
                  public void newColorSelected(Color color) {
                    if (color==null) return;             
                    panel.setColorOnDataItemsWithGOterms(selectedGOterms, panel.getDataType(), color);
                    panel.repaint();
                 }
            };
            ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER+" on "+suffix,colormenulistener,panel);     
            this.add(showItem);
            this.add(showOnlySelectedItem); 
            this.add(showAllItem);            
            this.add(hideItem);   
            this.add(hideAllItem);             
            this.add(colorMenu);
            this.add(new JSeparator());             
        }
        JMenuItem createCollectionItem=new JMenuItem((datatype!=null)?(CREATE_COLLECTION+" of "+suffix):CREATE_COLLECTION);
        createCollectionItem.addActionListener(menuItemListener);     
        this.add(createCollectionItem);
    }
    
    /**
     * Updates the menu to fit a specific selection
     * This should be called every time before the menu is shown
     */
    public boolean updateMenu() {
        selectedGOterms=panel.getSelectedGOterms();
        if (selectedGOterms==null) return false;
        for (JMenuItem item:limitedToOne) {
            item.setEnabled(selectedGOterms.length==1);
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
     * An inner class that listens to popup-menu events
     */       
    private class DisplayMenuItemListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           if (e.getActionCommand().startsWith(SHOW_ONLY_SELECTED) && datatype!=null) {
                panel.setVisibilityOnAllDataItems(datatype,false);
                if (selectedGOterms!=null) panel.setVisibilityOnDataItemsWithGOterms(selectedGOterms, datatype, true);
                panel.redrawDataItems();
           } else if (e.getActionCommand().startsWith(SHOW_ALL) && datatype!=null) {
                panel.setVisibilityOnAllDataItems(datatype,true); 
                panel.redrawDataItems();
           } else if (e.getActionCommand().startsWith(SHOW) && datatype!=null) {
                if (selectedGOterms!=null) panel.setVisibilityOnDataItemsWithGOterms(selectedGOterms, datatype, true);         
                panel.redrawDataItems();
           } else if (e.getActionCommand().startsWith(HIDE_ALL) && datatype!=null) {
                panel.setVisibilityOnAllDataItems(datatype,false); 
                panel.redrawDataItems();
           } else if (e.getActionCommand().startsWith(HIDE) && datatype!=null) {
                if (selectedGOterms!=null) panel.setVisibilityOnDataItemsWithGOterms(selectedGOterms, datatype, false);           
                panel.redrawDataItems();
           } else if (e.getActionCommand().startsWith(CREATE_COLLECTION)) {
                if (selectedGOterms==null) return;               
                if (datatype==Sequence.class) gui.promptAndCreateSequenceCollection(panel.getNamesOfDataItemsWithGOterms(selectedGOterms, datatype));
                else if (datatype==Motif.class) gui.promptAndCreateMotifCollection(panel.getNamesOfDataItemsWithGOterms(selectedGOterms, datatype));
                else {
                    ArrayList<String> list=new ArrayList<String>(selectedGOterms.length);
                    list.addAll(Arrays.asList(selectedGOterms));                    
                    gui.promptAndCreateTextVariable(list);
                }
           } 
       }           

   }
    
        
}
