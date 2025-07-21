/*
 
 
 */

package org.motiflab.gui;

import javax.swing.JMenuItem;
import java.awt.Color;
/**
 * This class implements a simple unselectable JMenuItem that can be used
 * for headers in popup-menus or similar
 * 
 * @author kjetikl
 */
public class HeaderMenuItem extends JMenuItem {
    
    public HeaderMenuItem(String itemname) {
        super("<html><b>"+itemname+"</b></html>");
        setEnabled(false);  
    }
    
   public HeaderMenuItem(String itemname, Color fgColor, Color bgColor) {
        super("<html><b>"+itemname+"</b></html>");
        setEnabled(false);  
        setBackground(bgColor);
        setForeground(fgColor);
        setOpaque(true);
    }
}
