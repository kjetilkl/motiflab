/*
 
 
 */

package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;

/**
 * This class is meant as a generic class for displaying tables where each
 * row corresponds to a Motif.
 * If any of the columns in the tablemodel has class=Motif.class they will
 * be drawn as sequence logos. Also, if any such column exists, the user will
 * be able to double-click on a row to display a Motif prompt
 * @author kjetikl
 */
public class GenericMotifBrowserPanel extends GenericBrowserPanel {
    private JTable table;
    private TableModel model;
    private MotifLogo logorenderer;
    private MotifLabGUI gui;
    private JPopupMenu contextMenu=null;
    private JPanel controlsPanel=null;
    private JSearchTextField searchfield;
    private ColorCellRenderer colorCellRenderer=null;
    private boolean isModal=false;
    private VisualizationSettings settings=null;
    private JScrollPane scrollPane=null;
    private HashMap<Integer,JToolTip> customtooltips=null;
    
    public GenericMotifBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, boolean modal) {
        this(GUI,tablemodel,true, modal);
    }

    public GenericMotifBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, boolean showControlsPanel, boolean modal) {
        super();
        this.gui=GUI;
        this.isModal=modal;
        this.settings=gui.getVisualizationSettings();
        model=tablemodel;
        table=new JTable(model) {
            @Override
            public JToolTip createToolTip() {
                if (customtooltips!=null) {
                    Point p=table.getMousePosition();
                    if (p!=null) {
                        int tablecolumn=table.columnAtPoint(p);
                        int modelcolumn=table.convertColumnIndexToModel(tablecolumn);
                        JToolTip custom=customtooltips.get(modelcolumn);
                        if (custom!=null) {
                            custom.setComponent(table);
                            return custom;
                        }
                    }
                }
                return super.createToolTip();
            }              
        };
        table.setAutoCreateRowSorter(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        ToolTipHeader header = new ToolTipHeader(table.getColumnModel());
        table.setTableHeader(header);
        table.getTableHeader().setReorderingAllowed(false);        
        scrollPane=new JScrollPane(table);
        this.setLayout(new BorderLayout());
        this.add(scrollPane,BorderLayout.CENTER);
        Color [] basecolors=settings.getBaseColors();
        int rowheight=table.getRowHeight();
        logorenderer=new MotifLogo(basecolors,(int)(rowheight*1.25));
        logorenderer.setUseAntialias(settings.useMotifAntialiasing());
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
               if (table.isCellEditable(table.getSelectionModel().getLeadSelectionIndex(), table.getColumnModel().getSelectionModel().getLeadSelectionIndex())) return; // do nothing if cell can be edited, so as to not interfere with editing process               
               if (e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_V) {
                    int[] rows=table.getSelectedRows();
                    int visibleRows=0;
                    int motifcolumn=getMotifColumn();
                    if (motifcolumn<0) return; // nothing to do here
                    for (int row:rows) {
                        Object motif=model.getValueAt(table.convertRowIndexToModel(row),motifcolumn);
                        if (motif instanceof Motif && settings.isRegionTypeVisible(((Motif)motif).getName())) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    for (int row:rows) {
                        Object motif=model.getValueAt(table.convertRowIndexToModel(row),motifcolumn);
                        if (motif instanceof Motif) settings.setRegionTypeVisible(((Motif)motif).getName(),doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown()) {
                    int[] rows=table.getSelectedRows();
                    int motifcolumn=getMotifColumn();
                    if (motifcolumn<0) return;
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    for (int row:rows) {
                        Object motif=model.getValueAt(table.convertRowIndexToModel(row),motifcolumn);
                        if (motif instanceof Motif) settings.setRegionTypeVisible(((Motif)motif).getName(),doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown()) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    setVisibilityOnAllMotifs(doShow,true);
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    setVisibilityOnAllMotifs(false,false);
                    int[] rows=table.getSelectedRows();
                    int motifcolumn=getMotifColumn();
                    if (motifcolumn<0) return;
                    for (int row:rows) {
                        Object motif=model.getValueAt(table.convertRowIndexToModel(row),motifcolumn);
                        if (motif instanceof Motif) settings.setRegionTypeVisible(((Motif)motif).getName(),true,false);
                    }                  
                    settings.redraw();
                } else if (e.getKeyCode()==KeyEvent.VK_PLUS || e.getKeyCode()==KeyEvent.VK_ADD) {
                    int newheight=table.getRowHeight()+1;
                    if (newheight>80) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    table.setRowHeight(newheight);
                } else if (e.getKeyCode()==KeyEvent.VK_MINUS || e.getKeyCode()==KeyEvent.VK_SUBTRACT) {
                    int newheight=table.getRowHeight()-1;
                    if (newheight<8) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    table.setRowHeight(newheight);
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder motifstring=new StringBuilder();
                     int[] rows=table.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';                     
                     boolean first=true;
                     int motifcolumn=getMotifColumn();
                     if (motifcolumn<0) return;
                     for (int row:rows) {
                         Object motif=model.getValueAt(table.convertRowIndexToModel(row),motifcolumn);
                         if (!(motif instanceof Motif)) continue;
                         if (first) first=false; else motifstring.append(separator);
                         motifstring.append(((Motif)motif).getName());
                     }
                     String motifs=motifstring.toString();
                     gui.logMessage(motifs);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(motifs);
                     java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                     clipboard.setContents(stringSelection, null);
                } else if (e.getKeyCode()==KeyEvent.VK_L) {
                    logorenderer.setScaleByIC(!logorenderer.getScaleByIC());
                    table.repaint();
                 } 
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount()==2) {
                    int modelrow=table.convertRowIndexToModel(table.getSelectedRow());
                    int motifcolumn=getMotifColumn();
                    if (motifcolumn<0) return;
                    Object motif=model.getValueAt(modelrow, motifcolumn);
                    if (motif instanceof Motif) {
                        gui.showPrompt((Motif)motif, false, isModal);
                    }
                }
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
        // install renderers and sorters
        MotifLogoComparator logocomparator=new MotifLogoComparator();        
        for (int i=0;i<table.getColumnCount();i++) {
            Class columnclass=model.getColumnClass(i);
            if (columnclass==Motif.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(logorenderer);
                ((TableRowSorter)table.getRowSorter()).setComparator(i, logocomparator);                
            } else if (columnclass==Color.class) {
                if (colorCellRenderer==null) colorCellRenderer=new ColorCellRenderer(getMotifColumn());
                TableColumn column=table.getColumnModel().getColumn(i);
                column.setCellRenderer(colorCellRenderer);
                column.setPreferredWidth(22);
                column.setMinWidth(22);
                column.setMaxWidth(22);
            } else if (columnclass==String.class) {
                ((TableRowSorter)table.getRowSorter()).setComparator(i, MotifLabEngine.getNaturalSortOrderComparator(true));
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
        contextMenu=new GenericMotifBrowserPanelContextMenu(this);
    } // end setup

    
    public void setCustomTooltipForModelColumn(JToolTip tooltip,int column) {
        if (customtooltips==null) customtooltips=new HashMap<Integer, JToolTip>(1);
        customtooltips.put(column, tooltip);
    }
    
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
                searchForText(); // the text to search for is taken directly from the searchField
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
    
    /** Returns the index of the first column containing Motif objects, or -1 if no such column was found */
    public final int getMotifColumn() {
       for (int i=0;i<table.getColumnCount();i++) {
            if (model.getColumnClass(i)==Motif.class) return i;
       }
       return -1;
    }

    private void showContextMenu(MouseEvent evt) {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return;
        if (contextMenu instanceof GenericMotifBrowserPanelContextMenu) ((GenericMotifBrowserPanelContextMenu)contextMenu).updateMenu();
        contextMenu.show(evt.getComponent(), evt.getX(),evt.getY());       
    }

    /** Returns the names of the motifs in currently selected rows of the table
     *  or NULL if no (viable) rows are currently selected
     */
    public String[] getSelectedMotifNames() {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        int motifcolumn=getMotifColumn();
        if (motifcolumn<0) return null;
        ArrayList<String> selected=new ArrayList<String>(selection.length);
        for (int i=0;i<selection.length;i++) {
            Object motif=table.getValueAt(selection[i], motifcolumn);
            if (motif instanceof Motif) selected.add(((Motif)motif).getName());
        }
        if (selected.isEmpty()) return null;
        String[] selectedMotifNames=new String[selected.size()];
        return selected.toArray(selectedMotifNames);
    }
    
    public JPopupMenu getContextMenu() {
        return contextMenu;
    }
    
    /**
     * Specifies a new context menu to use for this GenericMotifBrowserPanel
     * @param newMenu 
     */
    public void setContextMenu(JPopupMenu newMenu) {
         contextMenu=newMenu;
    }    
    
    public void disableContextMenu() {
        contextMenu=null;
    }

   /**
     * Adds an additional menu item to the end of this panels context menu
     * (assuming it is associated with a GenericMotifBrowserPanelContextMenu)
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one motif is selected
     * @param callback 
     */ 
   public void addExtraContextMenuItem(String name, boolean limitOne, ActionListener callback) {
       if (contextMenu instanceof GenericMotifBrowserPanelContextMenu) ((GenericMotifBrowserPanelContextMenu)contextMenu).addExtraMenuItem(name, limitOne, callback);
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
        int matchAt=-1;
        for (int i=0;i<rowsCount;i++) {
            int nextRow=(startRow+i)%rowsCount; // this will search all rows starting at the current and wrapping around
            if (isRowMatching(table,nextRow)) {
                matchAt=nextRow;
                break;
            }
            if (nextRow==currentRow) break;  // search wrapped around without any matches
        }
        if (matchAt>=0) {
            table.setRowSelectionInterval(matchAt, matchAt);
            table.scrollRectToVisible(table.getCellRect(matchAt,0,true));
        } else {
            gui.statusMessage("No match found for '"+searchfieldText+"'");
            table.clearSelection();
        }
    }
    
    private boolean isRowMatching(JTable table, int row) {
        int colCount=table.getColumnCount();        
        for (int j=0;j<colCount;j++) {
            Object value=table.getValueAt(row, j);
            if (value!=null && !(value instanceof Color)) {
                String valueString=(value instanceof Motif)?(((Motif)value).getPresentationName()):value.toString();
                valueString=valueString.toLowerCase();
                if (searchfield.isSearchMatch(valueString)) {
                    return true;
                }
            }
        }
        return false;
    }

   protected void setVisibilityOnAllMotifs(boolean show, boolean redraw) {
       for (Data data:gui.getEngine().getAllDataItemsOfType(Motif.class)) {
           settings.setRegionTypeVisible(data.getName(), show, false);             
       }
       if (redraw) gui.redraw();
   }    
   
   public Comparator<Motif> getMotifLogoComparator() {
       return new MotifLogoComparator();
   }
    
/** A class to sort motif logos based on the length of the motif */
private class MotifLogoComparator implements java.util.Comparator<Motif> {
    @Override
    public int compare(Motif motif1, Motif motif2) {
        return motif1.getLength()-motif2.getLength();
    }  
}   
   
private class ColorCellRenderer extends DefaultTableCellRenderer {
    
    SimpleDataPanelIcon selectedicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
    int motifcolumn=-1;
    
    public ColorCellRenderer(int motifcolumn) {
         super();
         this.motifcolumn=motifcolumn;
         setIcon(selectedicon);  
         setText(null);         
         setHorizontalTextPosition(SwingConstants.RIGHT);
    }
 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component thiscomponent=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (motifcolumn<0) return thiscomponent;
        Motif motif=(Motif)table.getValueAt(row, motifcolumn);
        if (motif!=null) {
            String motifname=motif.getName();
            boolean isVisible=settings.isRegionTypeVisible(motifname);
            selectedicon.setForegroundColor((Color)value); //  selectedicon.setForegroundColor(settings.getFeatureColor(motifname));
            selectedicon.setBorderColor((isVisible)?Color.BLACK:Color.LIGHT_GRAY);            
            thiscomponent.setForeground((isVisible)?Color.BLACK:Color.LIGHT_GRAY);            
        } else {
            selectedicon.setForegroundColor(Color.WHITE); //
            selectedicon.setBorderColor(Color.WHITE); //
        }
        setText(null);          
        return thiscomponent;
    }    
}    
   
}
