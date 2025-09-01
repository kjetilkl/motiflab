
package org.motiflab.gui;

import java.awt.event.KeyEvent;
import javax.swing.JPopupMenu;
import org.motiflab.engine.data.Data;

/**
 * This interface can be used to respond to events in some GUI panels (e.g. data panels and visualization panel)
 * such as key pressed events and also to add extra menu items to context menus
 */
public interface PanelListener {
       
    /**
     * This method is called when a context menu is about to be displayed in the panel.
     * It allows the PanelListeners to add additional elements to the menu
     * @param panel The panel that triggered the event
     * @param data An array containing the selected data elements in the panel (note that the list can contain heterogeneous elements)
     * @param menu The context menu to be shown. New elements can be added to this menu
     */
    public void showContextMenu(Object panel, Data[] data, JPopupMenu menu);    
    
    /**
     * This method is called when a key is pressed in the panel
     * @param panel The panel that triggered the event
     * @param data An array containing the selected data elements in the panel (note that the list can contain heterogeneous elements)
     * @param e The KeyEvent containing information about which keys were pressed
     */
    public void keyPressed(Object panel, Data[] data, KeyEvent e);
    
    
}
