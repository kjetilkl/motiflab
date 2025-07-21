/*
 * A superclass for all GenericBrowserPanels such as 
 * GenericMotifBrowserpanel, GenericModuleBrowserpanel and GenericSequenceBrowserpanel
 */
package org.motiflab.gui;

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author kjetikl
 */
public abstract class GenericBrowserPanel extends JPanel {
 
    public abstract JTable getTable();
    
    /** Returns the controls panel for this GenericBrowserPanel.
     *  The controls panel is an extra small panel below the table
     *  that can contain additional buttons and controls, for instance
     *  a search field. The panel has a FlowLayout and other controls
     *  can be added to it. If the GenericBrowserPanel does not presently 
     *  have a controls panel a new one will be installed when this method 
     *  is called
     */
    public abstract JPanel getControlsPanel();
    
    public abstract JSearchTextField getSearchField();
        
    public abstract MotifLabGUI getGUI();

    public abstract boolean isPanelModal();

    public abstract JScrollPane getTableScrollPane();

    protected class RowSelectionTooltip implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int selectedrows=getTable().getSelectedRowCount();
            String text="     "+selectedrows+" row"+((selectedrows!=1?"s":""))+" selected     ";
            JPopupMenu pm = new JPopupMenu();
            pm.setInvoker (getTable());
            pm.add(new JLabel(text));
            pm.setBackground(Color.yellow);
            Point p=MouseInfo.getPointerInfo().getLocation();
            p.x+=20;
            p.y+=20;
            pm.setLocation(p);
            pm.setVisible(true);
        }
    }


}
