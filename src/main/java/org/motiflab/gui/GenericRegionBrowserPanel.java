/*
 
 
 */

package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.motiflab.engine.MotifLabEngine;

/**
 * This class is meant as a generic class for displaying tables where each
 * row corresponds to a region type (e.g. a repeatmasker type or ChIP-Seq TF)
 * @author kjetikl
 */
public class GenericRegionBrowserPanel extends GenericBrowserPanel {
    private JTable table;
    private TableModel model;
    private MotifLabGUI gui;
    private JPopupMenu contextMenu=null;
    private JPanel controlsPanel=null;
    private JSearchTextField searchfield;
    private ColorCellRenderer colorCellRenderer=null;
    private boolean isModal=false;
    private int regionTypeColumn=-1;
    private VisualizationSettings settings;
    private JScrollPane scrollPane=null;

    public GenericRegionBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, int regionColumn, boolean modal) {
        this(GUI,tablemodel,regionColumn, true, modal);
    }

    public GenericRegionBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, int regionColumn, boolean showControlsPanel, boolean modal) {
        super();
        this.gui=GUI;
        this.isModal=modal;
        this.regionTypeColumn=regionColumn;
        this.settings=gui.getVisualizationSettings();
        model=tablemodel;
        table=new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        ToolTipHeader header = new ToolTipHeader(table.getColumnModel());
        table.setTableHeader(header);      
        table.getTableHeader().setReorderingAllowed(false);       
        ((javax.swing.JComponent)table.getDefaultRenderer(Boolean.class)).setOpaque(true); // fixes alternating background bug for checkbox renderers in Nimbus        
        scrollPane=new JScrollPane(table);
        this.setLayout(new BorderLayout());
        this.add(scrollPane,BorderLayout.CENTER);
        for (int i=0;i<table.getColumnCount();i++) {
            Class columnclass=model.getColumnClass(i);            
            if (columnclass==String.class) {
                ((TableRowSorter)table.getRowSorter()).setComparator(i, MotifLabEngine.getNaturalSortOrderComparator(true));
            }            
        }           
        table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
               if (table.isCellEditable(table.getSelectionModel().getLeadSelectionIndex(), table.getColumnModel().getSelectionModel().getLeadSelectionIndex())) return; // do nothing if cell can be edited, so as to not interfere with editing process                
               if (e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_V) {
                    int[] rows=table.getSelectedRows();
                    int visibleRows=0;
                    for (int row:rows) {
                        String regionType=(String)model.getValueAt(table.convertRowIndexToModel(row),regionTypeColumn);
                        if (settings.isRegionTypeVisible(regionType)) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    for (int row:rows) {
                        String regionType=(String)model.getValueAt(table.convertRowIndexToModel(row),regionTypeColumn);
                        settings.setRegionTypeVisible(regionType,doShow,false);                        
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown()) {
                    int[] rows=table.getSelectedRows();
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    for (int row:rows) {
                        String regionType=(String)model.getValueAt(table.convertRowIndexToModel(row),regionTypeColumn);
                        settings.setRegionTypeVisible(regionType,doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown()) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    setVisibilityOnAllRows(doShow, true);
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    setVisibilityOnAllRows(false, false);
                    int[] rows=table.getSelectedRows();
                    for (int row:rows) {
                        String regionType=(String)model.getValueAt(table.convertRowIndexToModel(row),regionTypeColumn);
                        settings.setRegionTypeVisible(regionType,true,false);
                    }                   
                    settings.redraw();
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder regionstring=new StringBuilder();
                     int[] rows=table.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';                     
                     boolean first=true;
                     for (int row:rows) {
                         int modelrow=table.convertRowIndexToModel(row);
                         String regionType=(String)model.getValueAt(modelrow, regionTypeColumn);                   
                         if (first) first=false; else regionstring.append(separator);
                         regionstring.append(regionType);
                     }
                     String regions=regionstring.toString();
                     gui.logMessage(regions);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(regions);
                     java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                     clipboard.setContents(stringSelection, null);
                }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {

            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger() && contextMenu!=null) {
                   int row = table.rowAtPoint(evt.getPoint()); //
                   if (row>=0 && !table.isRowSelected(row)) table.getSelectionModel().setSelectionInterval(row, row);                 
                   showContextMenu(evt);
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger() && contextMenu!=null) {
                   int row = table.rowAtPoint(evt.getPoint()); //
                   if (row>=0 && !table.isRowSelected(row)) table.getSelectionModel().setSelectionInterval(row, row);                 
                   showContextMenu(evt);
                }
            }
        });
        // install renderers
        for (int i=0;i<table.getColumnCount();i++) {
            Class columnclass=model.getColumnClass(i);
            if (columnclass==Color.class) {
                if (colorCellRenderer==null) colorCellRenderer=new ColorCellRenderer();
                TableColumn column=table.getColumnModel().getColumn(i);
                column.setCellRenderer(colorCellRenderer);
                column.setPreferredWidth(22);
                column.setMinWidth(22);
                column.setMaxWidth(22);
            }
        }
        
        if (showControlsPanel) {            
            if (controlsPanel==null) {
                controlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
                this.add(controlsPanel,BorderLayout.SOUTH);
            }
            if (searchfield==null) setupSearchField();            
            controlsPanel.add(searchfield);
        } 
        contextMenu=new GenericRegionBrowserPanelContextMenu(this);

    } // end setup

    @Override
    public JScrollPane getTableScrollPane() {
        return scrollPane;
    }
    
    @Override
    public JPanel getControlsPanel() {
        if (controlsPanel==null) {
            controlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
            this.add(controlsPanel,BorderLayout.SOUTH);
        }
        return controlsPanel;
    }
    
    @Override
    public JSearchTextField getSearchField() {
        if (searchfield==null) setupSearchField();
        return searchfield;
    }    
    
    private void setupSearchField() {
        searchfield=new JSearchTextField();
        searchfield.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchForText();
            }
        });   
        searchfield.enableRowFiltering(table);
    }
    
    @Override
    public JTable getTable() {
        return table;
    }
    
    @Override
    public MotifLabGUI getGUI() {
        return gui;
    }

    @Override
    public boolean isPanelModal() {
        return isModal;
    }
    
    /** Returns the index of the first column containing Region types */
    public int getRegionTypeColumn() {
       return regionTypeColumn;
    }

    private void showContextMenu(MouseEvent evt) {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return;
        if (contextMenu instanceof GenericRegionBrowserPanelContextMenu) ((GenericRegionBrowserPanelContextMenu)contextMenu).updateMenu();
        contextMenu.show(evt.getComponent(), evt.getX(),evt.getY());       
    }

    /** Returns the names of the regions in currently selected rows of the table
     *  or NULL if no (viable) rows are currently selected
     */
    public String[] getSelectedRegionNames() {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        int regioncolumn=getRegionTypeColumn();
        ArrayList<String> selected=new ArrayList<String>(selection.length);
        for (int i=0;i<selection.length;i++) {
            String regionType=(String)table.getValueAt(selection[i], regioncolumn);
            selected.add(regionType);
        }
        if (selected.isEmpty()) return null;
        String[] selectedRegionNames=new String[selected.size()];
        return selected.toArray(selectedRegionNames);
    }
    
    public JPopupMenu getContextMenu() {
        return contextMenu;
    }
    
    public void setContextMenu(JPopupMenu menu) {
        contextMenu=menu;
    }    
    
    public void disableContextMenu() {
        contextMenu=null;
    }

   /**
     * Adds an additional menu item to the end of the context menu for this panel
     * (assuming the default GenericRegionBrowserPanelContextMenu is still being used)
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one region is selected
     * @param callback 
     */ 
   public void addExtraContextMenuItem(String name, boolean limitOne, ActionListener callback) {
       if (contextMenu instanceof GenericRegionBrowserPanelContextMenu) ((GenericRegionBrowserPanelContextMenu)contextMenu).addExtraMenuItem(name, limitOne, callback);
   }    
    

    /** Searches for the next row in the table that matches the text string
     *  and marks it as selected
     */
    private void searchForText() {
        String searchfieldText=searchfield.getText();
        if (searchfieldText==null || searchfieldText.isEmpty()) return; // no need to search    
        int currentRow=table.getSelectedRow();
        int startRow=currentRow+1;
        int rowsCount=table.getRowCount();
        int colCount=table.getColumnCount();
        int matchAt=-1;
        for (int i=0;i<rowsCount;i++) {
            int nextRow=(startRow+i)%rowsCount; // this will search all rows starting at the current and wrapping around
            if (nextRow==currentRow) {
                gui.statusMessage("No match found for '"+searchfieldText+"'");
                break;
            }
            for (int j=0;j<colCount;j++) {
                Object value=table.getValueAt(nextRow, j);
                if (value!=null && !(value instanceof Color)) {
                    String valueString=value.toString().toLowerCase();
                    if (searchfield.isSearchMatch(valueString)) { // previously:  if (valueString.indexOf(text)>=0) { 
                        matchAt=nextRow;
                        break;
                    }
                }
            }
            if (matchAt>=0) break;
        }
        if (matchAt>=0) {
            table.setRowSelectionInterval(matchAt, matchAt);
            table.scrollRectToVisible(table.getCellRect(matchAt,0,true));
        }
    }
    
    protected void setVisibilityOnAllRows(boolean visible, boolean redraw) {
        for (int i=0;i<table.getModel().getRowCount();i++) {
            String regionType=(String)model.getValueAt(i,regionTypeColumn);
            settings.setRegionTypeVisible(regionType,visible,false);
        }
        if (redraw) settings.redraw();          
    }

private class ColorCellRenderer extends DefaultTableCellRenderer {
    
    SimpleDataPanelIcon selectedicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
    
    public ColorCellRenderer() {
         super();
         setIcon(selectedicon);  
         setText(null);           
         setHorizontalTextPosition(SwingConstants.RIGHT);
    }
 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String regionType=(String)table.getValueAt(row, regionTypeColumn);
        selectedicon.setForegroundColor(settings.getFeatureColor(regionType));
        selectedicon.setBorderColor((settings.isRegionTypeVisible(regionType))?Color.BLACK:Color.LIGHT_GRAY);
        setText(null);  
        return this;
    }    
}  
    
}
