/*
 
 
 */

package motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Motif;
import motiflab.engine.data.Sequence;

/**
 * A Generic panel containing a table for displaying GO terms (accessions) and other information related to these terms
 * @author kjetikl
 */
public class GenericGOBrowserPanel extends GenericBrowserPanel {
    private JTable table;
    private TableModel model;
    private MotifLabGUI gui;
    private JPopupMenu contextMenu=null;
    private JPanel controlsPanel=null;
    private JSearchTextField searchfield;
    private boolean isModal=false;
    private int GOTermColumn=-1;
    private VisualizationSettings settings;
    private JScrollPane scrollPane=null;
    private Class datatype=null; // Used if the panel is attached to a specific data type such as Sequence or Motif. A value of NULL means no specific type.
                                 // If a panel is attached to a type, the S/H/O keys can be used to show/hide data elements of that type that have the selected GO terms
    private JLabel datatypeLabel=null;
    private JComboBox<String> datatypeCombobox=null;
    private JPopupMenu dataTypeMenu=null;
    

    public GenericGOBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, int goColumn, boolean modal) {
        this(GUI,tablemodel,goColumn, true, modal);
    }

    public GenericGOBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, int goColumn, boolean showControlsPanel, boolean modal) {
        super();
        this.gui=GUI;
        this.isModal=modal;
        this.GOTermColumn=goColumn;
        this.settings=gui.getVisualizationSettings();
        model=tablemodel;
        table=new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        ToolTipHeader header = new ToolTipHeader(table.getColumnModel());
        table.setTableHeader(header);      
        table.getTableHeader().setReorderingAllowed(false);        
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
               if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown()) {   
                    if (datatype==null) return;
                    int[] rows=table.getSelectedRows();
                    boolean doShow=(e.getKeyCode()==KeyEvent.VK_S);
                    for (int row:rows) {
                        String GOterm=(String)model.getValueAt(table.convertRowIndexToModel(row),GOTermColumn);
                        setVisibilityOnDataItemsWithGOterm(GOterm,datatype,doShow);                    
                    }
                    if (datatype==Sequence.class) settings.notifySequenceVisibilityUpdated(); 
                    else settings.redraw();
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown()) {        
                    if (datatype==null) return;
                    boolean doShow=(e.getKeyCode()==KeyEvent.VK_S);
                    setVisibilityOnAllDataItems(datatype,doShow);
                    if (datatype==Sequence.class) settings.notifySequenceVisibilityUpdated(); 
                    else settings.redraw();
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    if (datatype==null) return;
                    setVisibilityOnAllDataItems(datatype,false);
                    int[] rows=table.getSelectedRows();
                    for (int row:rows) {
                        String GOterm=(String)model.getValueAt(table.convertRowIndexToModel(row),GOTermColumn);
                        setVisibilityOnDataItemsWithGOterm(GOterm,datatype,true);                    
                    }                
                    if (datatype==Sequence.class) settings.notifySequenceVisibilityUpdated(); 
                    else settings.redraw();
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder regionstring=new StringBuilder();
                     int[] rows=table.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';                     
                     boolean first=true;
                     for (int row:rows) {
                         int modelrow=table.convertRowIndexToModel(row);
                         String regionType=(String)model.getValueAt(modelrow, GOTermColumn);                   
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
//        for (int i=0;i<table.getColumnCount();i++) {
//            Class columnclass=model.getColumnClass(i);
//            if (columnclass==Color.class) {
//                if (colorCellRenderer==null) colorCellRenderer=new ColorCellRenderer();
//                TableColumn column=table.getColumnModel().getColumn(i);
//                column.setCellRenderer(colorCellRenderer);
//                column.setPreferredWidth(22);
//                column.setMinWidth(22);
//                column.setMaxWidth(22);
//            }
//        }
        if (showControlsPanel) {            
            if (controlsPanel==null) {
                controlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
                this.add(controlsPanel,BorderLayout.SOUTH);
            }
            if (searchfield==null) setupSearchField();            
            controlsPanel.add(searchfield);         
            datatypeLabel=new JLabel("   Associated type ");
            controlsPanel.add(datatypeLabel);      
            datatypeCombobox=new JComboBox<>(new String[]{"","Sequences","Motifs"});
            datatypeCombobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selected=(String)datatypeCombobox.getSelectedItem();
                    if (selected==null || selected.equals("") || selected.equals("No type") ) setDataTypeInternal(null);
                    else if (selected.equals("Sequences")) setDataTypeInternal(Sequence.class);
                    else if (selected.equals("Motifs")) setDataTypeInternal(Motif.class);
                }
            });
            controlsPanel.add(datatypeCombobox);
        } 
        contextMenu=new GenericGOBrowserPanelContextMenu(this, datatype);
    } // end setup

    
    public void allowDataTypeChange(boolean allow) {
        datatypeLabel.setVisible(allow);
        datatypeCombobox.setVisible(allow);
    }
    
    public void redrawDataItems() {
        if (datatype==Sequence.class) settings.notifySequenceVisibilityUpdated(); 
        else settings.redraw();        
    }
    
    public void setVisibilityOnDataItemsWithGOterms(String[] GOterms, Class datatype, boolean doShow) {
        for (String term:GOterms) {
            setVisibilityOnDataItemsWithGOterm(term, datatype, doShow);
        }
        if (datatype==Sequence.class) settings.notifySequenceVisibilityUpdated(); 
        else settings.redraw();
    }
    
    public void setVisibilityOnDataItemsWithGOterm(String GOterm, Class datatype, boolean doShow) {
        if (datatype==Sequence.class) setVisibilityOnSequencesWithGOterm(GOterm, doShow);
        else if (datatype==Motif.class) setVisibilityOnMotifsWithGOterm(GOterm, doShow);
    }
    
    
    public void setVisibilityOnSequencesWithGOterm(String GOterm, boolean doshow) {
         int accession=0;
         if (GOterm.startsWith("GO:") || GOterm.startsWith("go:") ) GOterm=GOterm.substring(3);
         try {
             accession=Integer.parseInt(GOterm);
             if (accession<=0 || accession>9999999) return; // not a valid GO term
         } catch (NumberFormatException e) {return;}
         for (Sequence sequence:gui.getEngine().getDefaultSequenceCollection().getAllSequences(gui.getEngine())) {
             if (sequence.hasGOterm(accession)) settings.setSequenceVisible(sequence.getSequenceName(), doshow, false);
         }         
    }
      
    public void setVisibilityOnMotifsWithGOterm(String GOterm, boolean doshow) {
         int accession=0;
         if (GOterm.startsWith("GO:") || GOterm.startsWith("go:") ) GOterm=GOterm.substring(3);
         try {
             accession=Integer.parseInt(GOterm);
             if (accession<=0 || accession>9999999) return; // not a valid GO term
         } catch (NumberFormatException e) {return;}
         for (Data motif:gui.getEngine().getAllDataItemsOfType(Motif.class)) {
             if (((Motif)motif).hasGOterm(accession)) settings.setRegionTypeVisible(motif.getName(), doshow, false);
         }         
    }    
    
    public void setVisibilityOnAllDataItems(Class datatype, boolean doShow) {
        if (datatype==Sequence.class) setVisibilityOnAllSequences(doShow);
        else if (datatype==Motif.class) setVisibilityOnAllMotifs(doShow);
    }    
    
    public void setVisibilityOnAllSequences(boolean doshow) {
         for (Sequence sequence:gui.getEngine().getDefaultSequenceCollection().getAllSequences(gui.getEngine())) {
              settings.setSequenceVisible(sequence.getSequenceName(), doshow, false);
         }        
    }    
    
    public void setVisibilityOnAllMotifs(boolean doshow) {
         for (Data motif:gui.getEngine().getAllDataItemsOfType(Motif.class)) {
              settings.setRegionTypeVisible(motif.getName(), doshow, false);
         }        
    }     

    public void setColorOnDataItemsWithGOterms(String[] GOterms, Class datatype, Color color) {
        if (datatype==null) return;
        for (Data data:gui.getEngine().getAllDataItemsOfType(datatype)) {
             if (data instanceof Motif && ((Motif)data).hasAnyGOterm(GOterms)) settings.setFeatureColor(data.getName(), color, false);
             else if (data instanceof Sequence && ((Sequence)data).hasAnyGOterm(GOterms)) settings.setSequenceLabelColor(data.getName(), color);
        } 
        settings.redraw();
    }
    
    public ArrayList<String> getNamesOfDataItemsWithGOterms(String[] GOterms, Class datatype) {
        ArrayList<String> result=new ArrayList<>();
        if (datatype==null) return result;
        for (Data data:gui.getEngine().getAllDataItemsOfType(datatype)) {
             if ( (data instanceof Motif && ((Motif)data).hasAnyGOterm(GOterms))
               || (data instanceof Sequence && ((Sequence)data).hasAnyGOterm(GOterms))      
             ) result.add(data.getName());
        }         
        return result;
    }
      
    
    
    /** Attaches a specific data type to this panel. Attaching a type such a e.g. Sequence.class
     *  allows users to press the S/H/O keys on selected GO-terms to show or hide data elements 
     *  that are annotated with those terms
     */
    public void setDataType(Class type) {
        if (type==Sequence.class) datatypeCombobox.setSelectedItem("Sequences");
        else if (type==Motif.class) datatypeCombobox.setSelectedItem("Motifs");
        else datatypeCombobox.setSelectedItem("");
        // the change in selected item will result in a call to the setDataTypeInternal() method below
    }
    
    private void setDataTypeInternal(Class type) {
        datatype=type;
        contextMenu=new GenericGOBrowserPanelContextMenu(this, datatype);
    }    
    
    public Class getDataType() {
        return datatype;
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
    public int getGOtermColumn() {
       return GOTermColumn;
    }

    private void showContextMenu(MouseEvent evt) {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return;
        if (contextMenu instanceof GenericGOBrowserPanelContextMenu) ((GenericGOBrowserPanelContextMenu)contextMenu).updateMenu();
        contextMenu.show(evt.getComponent(), evt.getX(),evt.getY());       
    }
        
    
    /** Returns the names of the regions in currently selected rows of the table
     *  or NULL if no (viable) rows are currently selected
     */
    public String[] getSelectedGOterms() {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        int gocolumn=getGOtermColumn();
        ArrayList<String> selected=new ArrayList<String>(selection.length);
        for (int i=0;i<selection.length;i++) {
            String regionType=(String)table.getValueAt(selection[i], gocolumn);
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
     * Adds an additional menu item to the end of this 
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one region is selected
     * @param callback 
     */ 
   public void addExtraContextMenuItem(String name, boolean limitOne, ActionListener callback) {
       if (contextMenu instanceof GenericGOBrowserPanelContextMenu) ((GenericGOBrowserPanelContextMenu)contextMenu).addExtraMenuItem(name, limitOne, callback);
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
    
}
