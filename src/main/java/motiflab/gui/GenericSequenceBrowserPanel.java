/*
 
 
 */

package motiflab.gui;

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
import java.util.Comparator;
import java.util.HashSet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Sequence;

/**
 * This class is meant as a generic class for displaying tables where each
 * row corresponds to a Sequence.
 * @author kjetikl
 */
public class GenericSequenceBrowserPanel extends GenericBrowserPanel {
    private JTable table;
    private TableModel model;
    private MotifLabGUI gui;
    private JPopupMenu contextMenu=null;
    private JPanel controlsPanel=null;
    private JSearchTextField searchfield;    
    private boolean isModal=false;
    private VisualizationSettings settings=null;
    private JScrollPane scrollPane=null;
    private MiscIcons markedSequenceIcon;
    
    public GenericSequenceBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, boolean modal) {
        this(GUI,tablemodel,true, modal);
    }

    public GenericSequenceBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, boolean showControlsPanel, boolean modal) {
        super();
        this.gui=GUI;
        this.isModal=modal;
        this.settings=gui.getVisualizationSettings();
        markedSequenceIcon=new MiscIcons(MiscIcons.BULLET_MARK,Color.RED);
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
        Sorter sorter=new Sorter();
        for (int i=0;i<table.getColumnCount();i++) { // install natural sort ordering for strings
            Class columnclass=model.getColumnClass(i);          
            if (columnclass==String.class || columnclass==Sequence.class) {             
                ((TableRowSorter)table.getRowSorter()).setComparator(i,sorter);
            }         
        }         
        table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
              if (table.isCellEditable(table.getSelectionModel().getLeadSelectionIndex(), table.getColumnModel().getSelectionModel().getLeadSelectionIndex())) return; // do nothing if cell can be edited, so as to not interfere with editing process
              if (e.getKeyCode()==KeyEvent.VK_V && !e.isControlDown()) {
                    int sequencecolumn=getSequenceColumn();
                    if (sequencecolumn<0) return;
                    int[] rows=table.getSelectedRows();
                    int visibleRows=0;
                    for (int row:rows) {
                        Object sequence=model.getValueAt(table.convertRowIndexToModel(row),sequencecolumn);
                        if (sequence instanceof Sequence && settings.isSequenceVisible(((Sequence)sequence).getName())) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    String[] sequences=new String[rows.length];
                    int i=0;
                    for (int row:rows) {
                        Object sequence=model.getValueAt(table.convertRowIndexToModel(row),sequencecolumn);
                        sequences[i]=(sequence instanceof Sequence)?((Sequence)sequence).getName():null;
                        i++;
                    }
                    settings.setSequenceVisible(sequences,doShow);
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown() && !e.isControlDown()) {
                    int sequencecolumn=getSequenceColumn();
                    if (sequencecolumn<0) return;
                    int[] rows=table.getSelectedRows();
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    String[] sequences=new String[rows.length];
                    int i=0;                    
                    for (int row:rows) {
                        Object sequence=model.getValueAt(table.convertRowIndexToModel(row),sequencecolumn);
                        sequences[i]=(sequence instanceof Sequence)?((Sequence)sequence).getName():null;
                        i++;
                    }
                    settings.setSequenceVisible(sequences,doShow);
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown() && !e.isControlDown()) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    setVisibilityOnAllSequences(doShow);
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    setVisibilityOnAllSequences(false);
                    int sequencecolumn=getSequenceColumn();
                    if (sequencecolumn<0) return;
                    int[] rows=table.getSelectedRows();
                    String[] sequences=new String[rows.length];
                    int i=0;                    
                    for (int row:rows) {
                        Object sequence=model.getValueAt(table.convertRowIndexToModel(row),sequencecolumn);
                        sequences[i]=(sequence instanceof Sequence)?((Sequence)sequence).getName():null;
                        i++;
                    }
                    settings.setSequenceVisible(sequences,true);                    
                } else if ((e.getKeyCode()==KeyEvent.VK_M) && !e.isControlDown()) {
                    int sequencecolumn=getSequenceColumn();
                    if (sequencecolumn<0) return;
                    int[] rows=table.getSelectedRows();
                    HashSet<String> sequences=new HashSet<String>();              
                    for (int row:rows) {
                        Object sequence=model.getValueAt(table.convertRowIndexToModel(row),sequencecolumn);
                        String seqname=(sequence instanceof Sequence)?((Sequence)sequence).getName():null;
                        if (seqname!=null) sequences.add(seqname);
                    }                    
                    if (e.isShiftDown()) settings.clearMarkedSequences(sequences);
                    else settings.addMarkedSequences(sequences);
                    settings.redraw();
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder sequencestring=new StringBuilder();
                     int[] rows=table.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';                     
                     boolean first=true;
                     int sequencecolumn=getSequenceColumn();
                     if (sequencecolumn<0) return;
                     for (int row:rows) {
                         int modelrow=table.convertRowIndexToModel(row);
                         Object sequence=model.getValueAt(modelrow, sequencecolumn);
                         if (!(sequence instanceof Sequence)) continue;
                         if (first) first=false; else sequencestring.append(separator);
                         sequencestring.append(((Sequence)sequence).getName());
                     }
                     String sequences=sequencestring.toString();
                     gui.logMessage(sequences);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(sequences);
                     java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                     clipboard.setContents(stringSelection, null);
                }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount()==2) {
                    int modelrow=table.convertRowIndexToModel(table.getSelectedRow());
                    int sequencecolumn=getSequenceColumn();
                    if (sequencecolumn<0) return;
                    Object sequence=model.getValueAt(modelrow, sequencecolumn);
                    if (sequence instanceof Sequence) {
                        gui.showPrompt((Sequence)sequence, false, isModal);
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
        // install renderers
        int seqcol=getSequenceColumn();
        if (seqcol>=0) {
            table.getColumn(table.getColumnName(seqcol)).setCellRenderer(new SequenceColumnRenderer());
        }
        if (showControlsPanel) {            
            if (controlsPanel==null) {
                controlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
                this.add(controlsPanel,BorderLayout.SOUTH);
            }
            if (searchfield==null) setupSearchField();           
            controlsPanel.add(searchfield);
        } 
        contextMenu=new GenericSequenceBrowserPanelContextMenu(this);

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
    
    /** Returns the index of the first column containing Sequence objects, or -1 if no such column was found */
    public final int getSequenceColumn() {
       for (int i=0;i<table.getColumnCount();i++) {
            if (model.getColumnClass(i)==Sequence.class) return i;
       }
       return -1;
    }

    private void showContextMenu(MouseEvent evt) {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return;
        if (contextMenu instanceof GenericSequenceBrowserPanelContextMenu) ((GenericSequenceBrowserPanelContextMenu)contextMenu).updateMenu();
        contextMenu.show(evt.getComponent(), evt.getX(),evt.getY());       
    }

    /** Returns the names of the sequence in currently selected rows of the table
     *  or NULL if no rows are currently selected
     */
    public String[] getSelectedSequenceNames() {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        int sequencecolumn=getSequenceColumn();
        if (sequencecolumn<0) return null;
        String[] selectedSequenceNames=new String[selection.length];
        for (int i=0;i<selection.length;i++) {
            Object seq=table.getValueAt(selection[i], sequencecolumn);
            if (seq instanceof Sequence) selectedSequenceNames[i]=((Sequence)seq).getName();   
            else return null;
        }        
        return selectedSequenceNames;
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
     * (assuming the default GenericSequenceBrowserPanelContextMenu is still being used)
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one sequence is selected
     * @param callback 
     */ 
   public void addExtraContextMenuItem(String name, boolean limitOne, ActionListener callback) {
       if (contextMenu instanceof GenericSequenceBrowserPanelContextMenu) ((GenericSequenceBrowserPanelContextMenu)contextMenu).addExtraMenuItem(name, limitOne, callback);
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
                if (value!=null && !(value instanceof java.awt.Color)) {
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

   protected void setVisibilityOnAllSequences(boolean show) {
       ArrayList<Data> allSequences=gui.getEngine().getAllDataItemsOfType(Sequence.class);
       String[] sequenceNames=new String[allSequences.size()];
       for (int i=0;i<sequenceNames.length;i++) sequenceNames[i]=allSequences.get(i).getName();
       settings.setSequenceVisible(sequenceNames,show);
   }             
    
private class SequenceColumnRenderer extends DefaultTableCellRenderer {
    
   public SequenceColumnRenderer() {
         super();
         setHorizontalTextPosition(SwingConstants.LEFT);
    }
 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel comp=(JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof Sequence) {
            String sequencename=((Sequence)value).getName();
            Color labelColor=settings.getSequenceLabelColor(sequencename);
            boolean isVisible=settings.isSequenceVisible(sequencename);           
            comp.setText(sequencename);
            comp.setForeground((isVisible)?labelColor:Color.LIGHT_GRAY);
            comp.setHorizontalTextPosition(SwingConstants.LEFT);
            comp.setIconTextGap(12);
            if (settings.isSequenceMarked(sequencename)) comp.setIcon(markedSequenceIcon);
            else comp.setIcon(null);
        }
        return comp;
    }    
} 


    private class Sorter implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1==null && o2==null) return 0;
            if (o1!=null && o2==null) return -1;
            if (o1==null && o2!=null) return 1;
            if (o1 instanceof Number && o2 instanceof Number) {
                if (o1 instanceof Integer && o2 instanceof Integer) return ((Integer)o1).compareTo((Integer)o2);
                if (o1 instanceof Double && o2 instanceof Double) return ((Double)o1).compareTo((Double)o2);
                return Double.compare(((Number)o1).doubleValue(),((Number)o2).doubleValue());
            } else {               
                return MotifLabEngine.compareNaturalOrder(o1.toString(),o2.toString());
            }
        }      
    }

}
