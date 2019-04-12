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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import motiflab.engine.DataListener;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.SequencePartition;

/**
 * This is the browser panel used by the SequenceBrowser dialog (but not Prompt_SequenceCollection)
 * @author kjetikl
 */
public class SequenceBrowserPanel extends JPanel implements DataListener {
    private JTable manualSelectionTable;
    private DefaultTableColumnModel columnModel;
    private JTextField filterTextfield;
    private JComboBox filterOperator;
    private JLabel numberOfSequencesShowingLabel;
    private Filter filter;
    private ManualSelectionTableModel model;
    private ManualSelectionContextMenu manualSelectionContextMenu;
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private SequenceCollection sequencecollection=null;
    private String initialFilterChoice="location";
    private String[] columnHeaders=new String[] {" ","Sequence","Filter"}; // the name of the Filter column will be changed later on 
    private String[] filterColumnHeaders=null; //n
    private HashMap<String,Class> filterPropertyClass=null;
    private boolean isModal=false;
    private MiscIcons markedSequenceIcon;

    private static final int SELECTED_COLUMN=0;
    private static final int SEQUENCE_COLUMN=1;
    private static final int FILTER_COLUMN=2;    
    // private static final int LOGO_COLUMN=3;    

    public SequenceBrowserPanel(MotifLabGUI gui, SequenceCollection sequencecollection, boolean modal, boolean allowCreateCollection) {
        super();
        this.gui=gui;
        this.engine=gui.getEngine();
        this.sequencecollection=sequencecollection;
        this.isModal=modal;
        markedSequenceIcon=new MiscIcons(MiscIcons.BULLET_MARK, Color.RED);
        setupManualEntryPanel(allowCreateCollection);
        if (sequencecollection==null) {
              columnModel.getColumn(SELECTED_COLUMN).setCellRenderer(new FeatureColorCellRenderer(gui));
        } else ((javax.swing.JComponent)manualSelectionTable.getDefaultRenderer(Boolean.class)).setOpaque(true); // fixes alternating background bug for checkbox renderers in Nimbus
    }
    
    private void setupFilterColumns() {
        String[] properties=Sequence.getAllProperties(engine);
        filterPropertyClass=new HashMap<String,Class>(properties.length+1);      
        filterPropertyClass.put("marked",Boolean.class);        
        for (String key:properties) {
            Class propclass=Sequence.getPropertyClass(key, engine);
            if (propclass.equals(ArrayList.class)) filterPropertyClass.put(key,String.class); // this will 'magically' convert lists to strings 
            else filterPropertyClass.put(key,propclass);
        }       
        ArrayList<String> sorted=new ArrayList<String>(filterPropertyClass.keySet());
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);        
        filterColumnHeaders=new String[sorted.size()];
        filterColumnHeaders=sorted.toArray(filterColumnHeaders);
    }
    
    
    @SuppressWarnings("unchecked")
    private void setupManualEntryPanel(boolean allowCreateCollection) {
        setupFilterColumns();
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        model=new ManualSelectionTableModel(sequences,columnHeaders);
        columnModel = new DefaultTableColumnModel();
        for (int i=0;i<columnHeaders.length;i++) {
            TableColumn col = new TableColumn(i);
            col.setHeaderValue(model.getColumnName(i));
            columnModel.addColumn(col);
        }
        manualSelectionTable=new JTable(model,columnModel) {
//            @Override
//            public JToolTip createToolTip() {
//                if (customTooltip==null) {
//                    customTooltip=new SingleMotifTooltip(gui.getVisualizationSettings());
//                    customTooltip.setComponent(this);
//                }
//                return customTooltip;
//            }
        };
        manualSelectionTable.setAutoCreateRowSorter(true);
        manualSelectionTable.getTableHeader().setReorderingAllowed(false);
        manualSelectionTable.setRowHeight(18);
        columnModel.getColumn(SELECTED_COLUMN).setMinWidth(22);
        columnModel.getColumn(SELECTED_COLUMN).setMaxWidth(22);
        columnModel.getColumn(SEQUENCE_COLUMN).setPreferredWidth(100);
        JScrollPane manualSelectionScrollPane=new JScrollPane(manualSelectionTable);
        this.setLayout(new BorderLayout());
        this.add(manualSelectionScrollPane,BorderLayout.CENTER);
        JPanel filterPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        filterPanel.add(new JLabel("Filter "));
        final JComboBox filterCombobox=new JComboBox(filterColumnHeaders);
        filterTextfield=new JTextField(12);                  
        filterOperator = new JComboBox(new String[]{"=","<>","<","<=",">=",">"});   
        numberOfSequencesShowingLabel = new JLabel();
        filterPanel.add(filterCombobox);
        filterPanel.add(filterOperator);
        filterPanel.add(filterTextfield);
        filterPanel.add(numberOfSequencesShowingLabel);
        this.add(filterPanel,BorderLayout.SOUTH);
        final VisualizationSettings settings=gui.getVisualizationSettings();
        columnModel.getColumn(SEQUENCE_COLUMN).setCellRenderer(new CellRenderer_Sequence());
        columnModel.getColumn(FILTER_COLUMN).setCellRenderer(new CellRenderer_FilterColumn());
        Sorter sorter=new Sorter();
        ((TableRowSorter)manualSelectionTable.getRowSorter()).setComparator(FILTER_COLUMN, sorter);        
        ((TableRowSorter)manualSelectionTable.getRowSorter()).setComparator(SEQUENCE_COLUMN, sorter);
        manualSelectionTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE && sequencecollection!=null) {
                    int[] rows=manualSelectionTable.getSelectedRows();
                    int checkedRows=0;
                    for (int row:rows) {
                        if ((Boolean)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),0)) checkedRows++;
                    }
                    boolean doCheck=Boolean.TRUE;
                    if (checkedRows==rows.length) doCheck=Boolean.FALSE;
                    for (int row:rows) {
                        model.setValueAt(doCheck,manualSelectionTable.convertRowIndexToModel(row),0);
                    }
                    updateCountLabelText();
                } else if ((e.getKeyCode()==KeyEvent.VK_SPACE && sequencecollection==null) || e.getKeyCode()==KeyEvent.VK_V) {
                    int[] rows=manualSelectionTable.getSelectedRows();
                    int visibleRows=0;
                    for (int row:rows) {
                        Sequence sequence=(Sequence)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),SEQUENCE_COLUMN);
                        if (settings.isSequenceVisible(sequence.getName())) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    String[] list=new String[rows.length];
                    int i=0;
                    for (int row:rows) {
                        Sequence sequence=(Sequence)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),SEQUENCE_COLUMN);
                        list[i]=sequence.getName();
                        i++;
                    }
                    settings.setSequenceVisible(list,doShow);                   
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown()) {
                    int[] rows=manualSelectionTable.getSelectedRows();
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    String[] list=new String[rows.length];
                    int i=0;
                    for (int row:rows) {
                        Sequence sequence=(Sequence)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),SEQUENCE_COLUMN);
                        list[i]=sequence.getName();
                        i++;
                    }
                    settings.setSequenceVisible(list,doShow);                   
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown()) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    setVisibilityOnAllSequences(doShow);
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    setVisibilityOnAllSequences(false);                    
                    int[] rows=manualSelectionTable.getSelectedRows();
                    String[] list=new String[rows.length];
                    int i=0;
                    for (int row:rows) {
                        Sequence sequence=(Sequence)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),SEQUENCE_COLUMN);
                        list[i]=sequence.getName();
                        i++;
                    }
                    settings.setSequenceVisible(list,true);                    
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder string=new StringBuilder();
                     int[] rows=manualSelectionTable.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';                     
                     boolean first=true;
                     for (int row:rows) {
                         if (first) first=false; else string.append(separator);
                         int modelrow=manualSelectionTable.convertRowIndexToModel(row);
                         String name=((Sequence)model.getValueAt(modelrow, 1)).getName();
                         string.append(name);
                     }
                     String sequencestring=string.toString();
                     gui.logMessage(sequencestring);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(sequencestring);
                     java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                     clipboard.setContents(stringSelection, null);
                 } else if ((e.getKeyCode()==KeyEvent.VK_M) && !e.isControlDown()) {                                   
                    if (e.isShiftDown()) settings.clearMarkedSequences(getSelectedSequenceNamesAsSet());
                    else settings.addMarkedSequences(getSelectedSequenceNamesAsSet());
                    settings.redraw();
                } 
            }
        });
        filterCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String newFilterColumn=(String)filterCombobox.getSelectedItem();
                model.setFilterColumn(newFilterColumn);
                columnModel.getColumn(FILTER_COLUMN).setHeaderValue(newFilterColumn);
                manualSelectionTable.getTableHeader().repaint();
                manualSelectionTable.repaint();
            }
        });        
        filter=new Filter();
        ((TableRowSorter)manualSelectionTable.getRowSorter()).setRowFilter(filter);
        filterTextfield.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filter.updateFilter();                
                ((TableRowSorter)manualSelectionTable.getRowSorter()).sort();
                updateCountLabelText();
            }
        }); 
        filterTextfield.addKeyListener(new java.awt.event.KeyAdapter() { // note that this is a direct copy of the one above
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                filter.updateFilter();                
                ((TableRowSorter)manualSelectionTable.getRowSorter()).sort();
                updateCountLabelText();
            }
        });        
        filterOperator.setSelectedIndex(0);         
        filterCombobox.setSelectedIndex(0);
        filterCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filter.updateFilter();                
                ((TableRowSorter)manualSelectionTable.getRowSorter()).sort();
            }
        });        
        manualSelectionContextMenu=new ManualSelectionContextMenu(sequencecollection!=null, allowCreateCollection);
        manualSelectionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount()==2) {
                    int column = manualSelectionTable.columnAtPoint(evt.getPoint()); //
                    if (column==0) return; // clicked the checkbox
                    int modelrow=manualSelectionTable.convertRowIndexToModel(manualSelectionTable.getSelectedRow());
                    Sequence sequence=(Sequence)model.getValueAt(modelrow, 1);
                    if (sequence!=null) {
                        gui.showPrompt(sequence, false, isModal);
                    }
                }
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    int row = manualSelectionTable.rowAtPoint(evt.getPoint()); //
                    if (row>=0) {
                        if (!manualSelectionTable.isRowSelected(row)) {
                            manualSelectionTable.getSelectionModel().setSelectionInterval(row, row);
                        }
                    }
                    if (manualSelectionTable.getSelectedRowCount()>0) showContextMenu(evt);
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                updateCountLabelText();
                if (evt.isPopupTrigger()) {
                    int row = manualSelectionTable.rowAtPoint(evt.getPoint()); //
                    if (row>=0) {
                        if (!manualSelectionTable.isRowSelected(row)) {
                            manualSelectionTable.getSelectionModel().setSelectionInterval(row, row);
                        }
                    }
                    if (manualSelectionTable.getSelectedRowCount()>0) showContextMenu(evt);
                }
            }
        });
        if (sequences.size()>0) { // set selected on sequences in collection
            manualSelectionTable.getRowSorter().toggleSortOrder(SEQUENCE_COLUMN);
            if (sequencecollection!=null) {
               for (int i=0;i<model.getRowCount();i++) {
                   Sequence sequence=(Sequence)model.getValueAt(i, 1);
                   model.setValueAt(sequencecollection.contains(sequence), i, 0);
               }
               manualSelectionTable.getRowSorter().toggleSortOrder(SELECTED_COLUMN); // show checked first
               manualSelectionTable.getRowSorter().toggleSortOrder(SELECTED_COLUMN); // show checked first - must sort twice to get checked on top
            }
        }          
        updateCountLabelText();
        filterCombobox.setSelectedItem(initialFilterChoice);       
    } // end setupManualEntryPanel()


    public JTable getTable() {
        return manualSelectionTable;
    }

    /** Updates the current Sequence Collection (if set) to reflect the new selection */
    public void updateSequenceCollection() {
        if (sequencecollection==null) return;
        sequencecollection.clearAll(engine);
        for (int i=0;i<model.getRowCount();i++) {
            if ((Boolean)model.getValueAt(i,0)) {
                Sequence sequence=(Sequence)model.getValueAt(i, 1);
                sequencecollection.addSequence((Sequence)sequence);
            }
        }
    }

    @Override
    public void dataAdded(Data data) {
        if (!(data instanceof Sequence)) return;
        model.addSequence((Sequence)data);
    }

    @Override
    public void dataRemoved(Data data) {
        if (!(data instanceof Sequence)) return;
        model.removeSequence((Sequence)data);
    }

    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {}
    @Override
    public void dataUpdated(Data data) {
         if (!(data instanceof Sequence)) return;
         model.fireTableDataChanged();
    }

    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {}

    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) { }

    /** Shows or hides all sequence depending on the argument, but does not update visualization */
    public void setVisibilityOnAllSequences(boolean visible) {
         VisualizationSettings settings=gui.getVisualizationSettings();
         String[] list=new String[model.getRowCount()];
         for (int i=0;i<model.getRowCount();i++) {
             list[i]=((Sequence)model.getValueAt(i,1)).getName();
         }
         settings.setSequenceVisible(list, visible);
    }   

    /** Returns the number of sequences currently selected under Manual Entry tab*/
    private int countSelectedSequences() {
         int selected=0;
         for (int i=0;i<model.getRowCount();i++) {
             if ((Boolean)model.getValueAt(i,0)) selected++;
         }
         return selected;
    }
    /** Updates the label displaying counts (shown, selected and total) in the manual selection tab*/
    private void updateCountLabelText() {
        int total=model.getRowCount();
        if (sequencecollection!=null) numberOfSequencesShowingLabel.setText("<html>Matching: "+manualSelectionTable.getRowCount()+" of "+total+"<br>Included: "+countSelectedSequences()+" of "+total+"</html>");
        else numberOfSequencesShowingLabel.setText("<html>Matching: "+manualSelectionTable.getRowCount()+" of "+total+"</html>");
    }


     private void showContextMenu(MouseEvent evt) {
        int selectedCount=manualSelectionTable.getSelectedRowCount();
        int firstSelected=manualSelectionTable.getSelectedRow();
        if (firstSelected>=0 && selectedCount==1) {
            int modelrow=manualSelectionTable.convertRowIndexToModel(firstSelected);
            manualSelectionContextMenu.setSingleSequenceSelected(((Sequence)model.getValueAt(modelrow,1)).getName());
        } else {          
            manualSelectionContextMenu.setSingleSequenceSelected(null);
        }
        manualSelectionContextMenu.updateMenu();
        manualSelectionContextMenu.show(evt.getComponent(), evt.getX(),evt.getY());  
     }


    private HashSet<String> getSelectedSequenceNamesAsSet() {
        int[] selection=manualSelectionTable.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        HashSet<String> selectedSequenceNames=new HashSet<String>(selection.length);
        for (int i=0;i<selection.length;i++) {
            Object sequence=manualSelectionTable.getValueAt(selection[i], SEQUENCE_COLUMN);
            if (sequence instanceof Sequence) selectedSequenceNames.add(((Sequence)sequence).getName());   
            else return null;
        }        
        return selectedSequenceNames;
    }        
     

private class ManualSelectionTableModel extends AbstractTableModel { 
    private ArrayList<Boolean> checked=null;
    private ArrayList<Data> sequences=null;
    private String[] columnNames=null;
    private String filterColumn="location";
    
    public ManualSelectionTableModel(ArrayList<Data> sequences, String[] columnNames) {
        this.sequences=sequences;
        checked=new ArrayList<Boolean>(sequences.size());
        for (int i=0;i<sequences.size();i++) checked.add(Boolean.FALSE);
        this.columnNames=columnNames;
    }
    
    @Override
    public Class getColumnClass(int c) {
        if (c==SELECTED_COLUMN) return Boolean.class; // first column
        else if (c==SEQUENCE_COLUMN) return Sequence.class; // 
        else { // c==FILTER_COLUMN
            return filterPropertyClass.get(filterColumn);
        }
    }
    
    @Override    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return (columnIndex==0 && sequencecollection!=null);
    }
    
    private String getFilterColumn() {return filterColumn;}
    private void setFilterColumn(String filterColumn) {this.filterColumn=filterColumn;}
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        checked.set(rowIndex,(Boolean)aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void addSequence(Sequence newsequence) {
        int index=sequences.size();
        sequences.add(newsequence);
        checked.add(Boolean.FALSE);
        try {
           fireTableRowsInserted(index, index);
        } catch (ArrayIndexOutOfBoundsException e) {}        
    }

    public void removeSequence(Sequence sequence) {
        int index=sequences.indexOf(sequence);
        if (index>=0) {
            sequences.remove(index);
            checked.remove(index);
        }
        try {
            fireTableRowsDeleted(index, index);
        } catch (ArrayIndexOutOfBoundsException e) {}
    }

    @Override    
    public int getColumnCount() {
        return columnHeaders.length;
    }
    
    @Override
    public int getRowCount() {
        return sequences.size();
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex==SELECTED_COLUMN) return checked.get(rowIndex);
        else if (columnIndex==SEQUENCE_COLUMN) return (Sequence)sequences.get(rowIndex);
        else { // value from filter column
           if (filterColumn.equals("marked")) return (gui.getVisualizationSettings().isSequenceMarked( ((Sequence)sequences.get(rowIndex)).getName() ))?Boolean.TRUE:Boolean.FALSE;
           try {
               Object value=((Sequence)sequences.get(rowIndex)).getPropertyValue(filterColumn, engine);
               return Sequence.convertPropertyValueToType(value,filterPropertyClass.get(filterColumn));
           } catch (ExecutionError e) {
               return null;
           }
                             
        }
    }
  
    @Override
    public String getColumnName(int column) {
        if (column==FILTER_COLUMN) return filterColumn;
        else return columnNames[column];
    }
      
}    

private class CellRenderer_Sequence extends DefaultTableCellRenderer {
    public CellRenderer_Sequence() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
    }
    @Override
    public void setValue(Object value) {
       if (value!=null && value instanceof Sequence) {
           String seqName=((Sequence)value).getName();         
           setText(seqName);
       }
       else setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        Sequence sequence=(Sequence)table.getValueAt(row, SEQUENCE_COLUMN);
        if (sequencecollection==null) { // if used as SequenceBrowser (and not to create a sequence collection)
           if (!gui.getVisualizationSettings().isSequenceVisible(sequence.getName())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        if (c instanceof JLabel) {
           ((JLabel)c).setHorizontalTextPosition(SwingConstants.LEFT);
           ((JLabel)c).setIconTextGap(12);
           if (gui.getVisualizationSettings().isSequenceMarked(sequence.getName())) ((JLabel)c).setIcon(markedSequenceIcon);
           else ((JLabel)c).setIcon(null);
        }       
        return c;
    }
}// end class CellRenderer_Sequence


private class CellRenderer_FilterColumn extends DefaultTableCellRenderer {

    public CellRenderer_FilterColumn() {
           super();
    }
    @Override
    public void setValue(Object value) {
           String filterColumn=model.getFilterColumn();
           Class classtype=filterPropertyClass.get(filterColumn);
           if (Number.class.isAssignableFrom(classtype)) this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
           else this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
           if (classtype==Boolean.class) { //
               setText((value!=null)?value.toString():""); 
               setToolTipText(null);  
           } else if (value!=null) {
               String text=value.toString();
               setText(text);
               String split=text.replaceAll("\\s*,\\s*", "<br>");   
               setToolTipText("<html>"+split+"</html>");            
           } else {
             setText("");
             setToolTipText(null); 
           }                 
       }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        Sequence sequence=(Sequence)table.getValueAt(row, SEQUENCE_COLUMN);
        if (sequencecollection==null) { // if used as SequenceBrowser (and not to create a sequence collection)
           if (!gui.getVisualizationSettings().isSequenceVisible(sequence.getName())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        return c;
    }

}// end class CellRenderer_FilterColumn  


/** This private class implements the context menu */
private class ManualSelectionContextMenu extends JPopupMenu implements ActionListener {
     private final String DISPLAY_SEQUENCE="Display";
     private final String SHOW="Show";
     private final String HIDE="Hide";
     private final String SHOW_ONLY_SELECTED="Show Only Selected";   
     private final String SHOW_ALL="Show All";  
     private final String HIDE_ALL="Hide All";      
     private final String SELECT_SHOWN_SEQUENCES="Select Shown Sequences";
     private final String SELECT_ONLY_SHOWN_SEQUENCES="Select Only Shown Sequences";
     private final String SELECT_SEQUENCES_FROM="Select Sequences From";
     private final String SELECT_ONLY_SEQUENCES_FROM="Select Only Sequences From";
     private final String CREATE_SEQUENCE_COLLECTION="Create Sequence Collection";    
     private final String COLOR_SUBMENU_HEADER="Set Label Color"; 
     
     private final String MARK="Mark";
     private final String UNMARK="Unmark";
     private final String MARK_ONLY_SELECTED="Mark Only Selected";   
     private final String MARK_ALL="Mark All";  
     private final String UNMARK_ALL="Unmark All";    
     
     JMenuItem includeSelectedItem=new JMenuItem("Include Selected Sequences");
     JMenuItem excludeSelectedItem=new JMenuItem("Exclude Selected Sequences");
     JMenuItem includeListedItem=new JMenuItem("Include Matching Sequences");
     JMenuItem includeNotListedItem=new JMenuItem("Include Not Matching Sequences");
     JMenuItem excludeListedItem=new JMenuItem("Exclude Matching Sequences");
     JMenuItem excludeNotListedItem=new JMenuItem("Exclude Not Matching Sequences");
     JMenuItem includeAllItem=new JMenuItem("Include All Sequences");
     JMenuItem excludeAllItem=new JMenuItem("Exclude All Sequences");
     JMenuItem invertCollectionItem=new JMenuItem("Invert Collection");
     JMenuItem displayItem=new JMenuItem(DISPLAY_SEQUENCE);
     JMenu selectSequencesFromMenu=new JMenu(SELECT_SEQUENCES_FROM);
     JMenu selectOnlySequencesFromMenu=new JMenu(SELECT_ONLY_SEQUENCES_FROM);
     ExternalDBLinkMenu dbmenu=null;        
     SelectFromCollectionListener selectFromCollectionListener=new SelectFromCollectionListener();
     ClearAndSelectFromCollectionListener clearAndselectFromCollectionListener=new ClearAndSelectFromCollectionListener();
     
     ColorMenu setColorMenu;
     
    public ManualSelectionContextMenu(boolean isSelectionMenu, boolean allowCreateCollection) {
         if (isSelectionMenu) {
             includeSelectedItem.addActionListener(this);
             excludeSelectedItem.addActionListener(this);
             includeListedItem.addActionListener(this);
             excludeListedItem.addActionListener(this);
             includeNotListedItem.addActionListener(this);
             excludeNotListedItem.addActionListener(this);
             includeAllItem.addActionListener(this);
             excludeAllItem.addActionListener(this);
             invertCollectionItem.addActionListener(this);
             JMenu includeMenu=new JMenu("Include");
             JMenu excludeMenu=new JMenu("Exclude");
             includeMenu.add(includeSelectedItem);
             includeMenu.add(includeListedItem);
             includeMenu.add(includeNotListedItem);
             includeMenu.add(includeAllItem);
             excludeMenu.add(excludeSelectedItem);
             excludeMenu.add(excludeListedItem);
             excludeMenu.add(excludeNotListedItem);
             excludeMenu.add(excludeAllItem);             
             add(includeMenu);
             add(excludeMenu);
             add(invertCollectionItem);             
             add(new JSeparator());
         }
         dbmenu=new ExternalDBLinkMenu(null, gui);  
         ColorMenuListener colormenulistener=new ColorMenuListener() {
             public void newColorSelected(Color color) {
                if (color==null) return;
                int[] selectedRows = manualSelectionTable.getSelectedRows();
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (int i=0;i<selectedRows.length;i++) {
                    int modelrow=manualSelectionTable.convertRowIndexToModel(selectedRows[i]);
                    settings.setSequenceLabelColor(((Sequence)model.getValueAt(modelrow, 1)).getName(), color);
                }
                manualSelectionTable.repaint();
                gui.redraw();
             }
         };
         JMenuItem showItem=new JMenuItem(SHOW);
         JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED);   
         JMenuItem showAllItem=new JMenuItem(SHOW_ALL);               
         JMenuItem hideItem=new JMenuItem(HIDE);
         JMenuItem hideAllItem=new JMenuItem(HIDE_ALL);
         
         JMenuItem markItem=new JMenuItem(MARK);
         JMenuItem markOnlySelectedItem=new JMenuItem(MARK_ONLY_SELECTED);   
         JMenuItem markAllItem=new JMenuItem(MARK_ALL);               
         JMenuItem unmarkItem=new JMenuItem(UNMARK);
         JMenuItem unmarkAllItem=new JMenuItem(UNMARK_ALL);
         
         JMenuItem selectShownSequencesItem=new JMenuItem(SELECT_SHOWN_SEQUENCES);
         JMenuItem selectOnlyShownSequencesItem=new JMenuItem(SELECT_ONLY_SHOWN_SEQUENCES);
         JMenuItem createCollectionItem=new JMenuItem(CREATE_SEQUENCE_COLLECTION);        

         updateMenu();
                  
         displayItem.addActionListener(this);
         showItem.addActionListener(this);
         showOnlySelectedItem.addActionListener(this);
         showAllItem.addActionListener(this);
         hideItem.addActionListener(this);
         hideAllItem.addActionListener(this);  
         markItem.addActionListener(this);
         markOnlySelectedItem.addActionListener(this);
         markAllItem.addActionListener(this);
         unmarkItem.addActionListener(this);
         unmarkAllItem.addActionListener(this);           
         selectShownSequencesItem.addActionListener(this);     
         selectOnlyShownSequencesItem.addActionListener(this);
         if (allowCreateCollection) createCollectionItem.addActionListener(this);
        
         setColorMenu=new ColorMenu(COLOR_SUBMENU_HEADER, colormenulistener, SequenceBrowserPanel.this);
         this.add(displayItem);         
         this.add(showItem);
         this.add(showOnlySelectedItem); 
         this.add(showAllItem);            
         this.add(hideItem);   
         this.add(hideAllItem);              
         this.add(setColorMenu);
         this.add(new JSeparator());
         this.add(markItem);
         this.add(markOnlySelectedItem); 
         this.add(markAllItem);            
         this.add(unmarkItem);   
         this.add(unmarkAllItem);              
         this.add(new JSeparator());         
         this.add(selectShownSequencesItem);
         this.add(selectOnlyShownSequencesItem);
         this.add(selectSequencesFromMenu);
         this.add(selectOnlySequencesFromMenu);
         if (allowCreateCollection) {
             this.add(new JSeparator());
             this.add(createCollectionItem);
         }
         this.add(dbmenu);         
       
     }

    public final void updateMenu() {
        selectSequencesFromMenu.removeAll();
        selectOnlySequencesFromMenu.removeAll();
        for (String collectionName:engine.getNamesForAllDataItemsOfType(SequenceCollection.class)) {
            JMenuItem subitem=new JMenuItem(collectionName);
            subitem.addActionListener(selectFromCollectionListener);
            selectSequencesFromMenu.add(subitem);
            JMenuItem subitem2=new JMenuItem(collectionName);
            subitem2.addActionListener(clearAndselectFromCollectionListener);
            selectOnlySequencesFromMenu.add(subitem2);
        }
        for (String partitionName:engine.getNamesForAllDataItemsOfType(SequencePartition.class)) {
            Data data=engine.getDataItem(partitionName);
            if (data instanceof SequencePartition) {
                JMenu selectSequencesFromMenuCluster=new JMenu(data.getName());               
                JMenu selectOnlySequencesFromMenuCluster=new JMenu(data.getName());               
                for (String cluster:((SequencePartition)data).getClusterNames()) {                    
                    JMenuItem subitem=new JMenuItem(cluster);
                    subitem.setActionCommand(partitionName+"."+cluster);
                    subitem.addActionListener(selectFromCollectionListener);
                    selectSequencesFromMenuCluster.add(subitem);
                    JMenuItem subitem2=new JMenuItem(cluster);
                    subitem2.setActionCommand(partitionName+"."+cluster);
                    subitem2.addActionListener(clearAndselectFromCollectionListener);
                    selectOnlySequencesFromMenuCluster.add(subitem2);
                }
                selectSequencesFromMenu.add(selectSequencesFromMenuCluster);
                selectOnlySequencesFromMenu.add(selectOnlySequencesFromMenuCluster);
            }
        }
        selectSequencesFromMenu.setEnabled(selectSequencesFromMenu.getMenuComponentCount()>0);
        selectOnlySequencesFromMenu.setEnabled(selectOnlySequencesFromMenu.getMenuComponentCount()>0);
    }    
    
    /** This should be called when the context menu is invoked with only one selected sequence */
    public void setSingleSequenceSelected(String targetSequenceName) {
        dbmenu.updateMenu(targetSequenceName,true);        
        if (targetSequenceName==null) {
             displayItem.setVisible(false);
        } else {
            displayItem.setText(DISPLAY_SEQUENCE+" "+targetSequenceName);
            displayItem.setVisible(true);
        }

    }

     @Override
     public void actionPerformed(ActionEvent e) {
         String cmd=e.getActionCommand();
         if (cmd.equals(includeSelectedItem.getActionCommand())) {
             int[] rows=manualSelectionTable.getSelectedRows();
             for (int row:rows) {
                 int modelrow=manualSelectionTable.convertRowIndexToModel(row);
                 model.setValueAt(true, modelrow, 0);
             }
         } else if (cmd.equals(excludeSelectedItem.getActionCommand())) {
             int[] rows=manualSelectionTable.getSelectedRows();
             for (int row:rows) {
                 int modelrow=manualSelectionTable.convertRowIndexToModel(row);
                 model.setValueAt(false, modelrow, 0);
             }             
         } else if (cmd.equals(includeListedItem.getActionCommand())) {
             for (int i=0;i<manualSelectionTable.getRowCount();i++) {
                 int modelrow=manualSelectionTable.convertRowIndexToModel(i);
                 model.setValueAt(true, modelrow, 0);
             }            
         } else if (cmd.equals(includeNotListedItem.getActionCommand())) {             
             for (int i=0;i<model.getRowCount();i++) {
                 int viewrow=manualSelectionTable.convertRowIndexToView(i);
                 if (viewrow==-1) model.setValueAt(true, i, 0);
             }                  
         } else if (cmd.equals(excludeListedItem.getActionCommand())) {
             for (int i=0;i<manualSelectionTable.getRowCount();i++) {
                 int modelrow=manualSelectionTable.convertRowIndexToModel(i);
                 model.setValueAt(false, modelrow, 0);
             }
         } else if (cmd.equals(excludeNotListedItem.getActionCommand())) {
             for (int i=0;i<model.getRowCount();i++) {
                 int viewrow=manualSelectionTable.convertRowIndexToView(i);
                 if (viewrow==-1) model.setValueAt(false, i, 0);
             }
         } else if (cmd.equals(includeAllItem.getActionCommand())) {
             for (int i=0;i<model.getRowCount();i++) {
                 model.setValueAt(true, i, 0);
             }
         } else if (cmd.equals(excludeAllItem.getActionCommand())) {
             for (int i=0;i<model.getRowCount();i++) {
                 model.setValueAt(false, i, 0);
             }             
         } else if (cmd.equals(invertCollectionItem.getActionCommand())) {
             for (int i=0;i<model.getRowCount();i++) {
                 boolean checked=(Boolean)model.getValueAt(i,0);
                 model.setValueAt(!checked, i, 0);
             }
         } else if (e.getActionCommand().startsWith(DISPLAY_SEQUENCE)) {
                String[] selectedSequenceNames=getSelectedSequenceNames();
                Data sequence=gui.getEngine().getDataItem(selectedSequenceNames[0]);
                if (sequence instanceof Sequence) gui.showPrompt((Sequence)sequence, false, isModal);
           } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                setVisibilityOnAllSequences(false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.setSequenceVisible(getSelectedSequenceNames(), true);
                gui.redraw();
           } else if (e.getActionCommand().equals(SHOW_ALL)) {
                setVisibilityOnAllSequences(true);
                gui.redraw();                        
           } else if (e.getActionCommand().equals(HIDE_ALL)) {
                setVisibilityOnAllSequences(false);
                gui.redraw(); 
           } else if (e.getActionCommand().equals(SHOW)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.setSequenceVisible(getSelectedSequenceNames(), true);                 
                gui.redraw();
           } else if (e.getActionCommand().equals(HIDE)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.setSequenceVisible(getSelectedSequenceNames(), false);   
                gui.redraw();
           } else if (e.getActionCommand().equals(MARK_ONLY_SELECTED)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.setMarkedSequences(getSelectedSequenceNamesAsSet());
                gui.redraw();
           } else if (e.getActionCommand().equals(MARK_ALL)) {
                HashSet<String> all=new HashSet<String>(engine.getNamesForAllDataItemsOfType(Sequence.class));
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.setMarkedSequences(all);                
                gui.redraw();                        
           } else if (e.getActionCommand().equals(UNMARK_ALL)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.clearSequenceMarks();
                gui.redraw(); 
           } else if (e.getActionCommand().equals(MARK)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.addMarkedSequences(getSelectedSequenceNamesAsSet());               
                gui.redraw();
           } else if (e.getActionCommand().equals(UNMARK)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                settings.clearMarkedSequences(getSelectedSequenceNamesAsSet());
                gui.redraw();
           } else if (e.getActionCommand().equals(CREATE_SEQUENCE_COLLECTION)) {
                String[] selectedSequenceNames=getSelectedSequenceNames();
                ArrayList<String> list=new ArrayList<String>(selectedSequenceNames.length);
                list.addAll(Arrays.asList(selectedSequenceNames));
                gui.promptAndCreateSequenceCollection(list);
           } else if (e.getActionCommand().equals(SELECT_SHOWN_SEQUENCES)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allSequences=engine.getNamesForAllDataItemsOfType(Sequence.class);
                HashSet<String> shownSequences=new HashSet<String>(allSequences.size());
                for (String sequencename:allSequences) {
                   if (settings.isSequenceVisible(sequencename)) shownSequences.add(sequencename);
                }
                selectRowsForSequences(shownSequences,false);
           } else if (e.getActionCommand().equals(SELECT_ONLY_SHOWN_SEQUENCES)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allSequences=engine.getNamesForAllDataItemsOfType(Sequence.class);
                HashSet<String> shownSequences=new HashSet<String>(allSequences.size());
                for (String sequencename:allSequences) {
                   if (settings.isSequenceVisible(sequencename)) shownSequences.add(sequencename);
                }
                selectRowsForSequences(shownSequences,true);
           }
         manualSelectionTable.repaint();
         updateCountLabelText();
     }

     
    /** Returns the names of the sequences in currently selected rows of the table
     *  or NULL if no rows are currently selected
     */
    private String[] getSelectedSequenceNames() {
        int[] selection=manualSelectionTable.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        String[] selectedSequenceNames=new String[selection.length];
        for (int i=0;i<selection.length;i++) {
            Object sequence=manualSelectionTable.getValueAt(selection[i], SEQUENCE_COLUMN);
            if (sequence instanceof Sequence) selectedSequenceNames[i]=((Sequence)sequence).getName();   
            else return null;
        }        
        return selectedSequenceNames;
    }
    
     
} // END private class ManualSelectionContextMenu

  private class SelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectSequencesFromCollection(collectionName,false);
       }
   }
  private class ClearAndSelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectSequencesFromCollection(collectionName,true);
       }
   }

    private void selectSequencesFromCollection(String collectionName, boolean clearCurrentSelection) {
           HashSet<String> sequencenames=new HashSet<String>();
           int point=collectionName.indexOf('.');
           if (point>0) { // partition
               String clusterName=collectionName.substring(point+1);
               collectionName=collectionName.substring(0,point);
               Data col=engine.getDataItem(collectionName);
               if (col instanceof SequencePartition) sequencenames.addAll( ((SequencePartition)col).getAllSequenceNamesInCluster(clusterName) );
                                 
           } else { // collection
               Data col=engine.getDataItem(collectionName);
               if (col instanceof SequenceCollection) sequencenames.addAll( ((SequenceCollection)col).getAllSequenceNames());                         
           }
           selectRowsForSequences(sequencenames,clearCurrentSelection);          
    }

    private void selectRowsForSequences(HashSet<String> sequences, boolean clearCurrentSelection) {
        if (clearCurrentSelection) manualSelectionTable.clearSelection();
        ListSelectionModel selection=manualSelectionTable.getSelectionModel();        
        int sequencecolumn=SEQUENCE_COLUMN;
        for (int i=0;i<manualSelectionTable.getRowCount();i++) {
            Object value=manualSelectionTable.getValueAt(i, sequencecolumn);      
            String sequencename=((Sequence)value).getName();
            if (sequences.contains(sequencename)) {
                selection.addSelectionInterval(i,i); 
            }
        }
    }   


private class FeatureColorCellRenderer extends DefaultTableCellRenderer {
    
    SimpleDataPanelIcon selectedicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
    VisualizationSettings settings;

    public FeatureColorCellRenderer(MotifLabGUI gui) {
         super();
         settings=gui.getVisualizationSettings();
         setIcon(selectedicon);
         setText(null);           
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); // necessary to get correct alternating row rendering in Nimbus
        Sequence sequence=(Sequence)table.getValueAt(row, column+1);
        String sequencename=sequence.getName();
        selectedicon.setForegroundColor(settings.getSequenceLabelColor(sequencename));
        selectedicon.setBorderColor((settings.isSequenceVisible(sequencename))?Color.BLACK:Color.LIGHT_GRAY);
        // if (customTooltip!=null) setToolTipText(sequence.getName());
        setText(null);  
        return this;
    }
}

    
private class Filter extends RowFilter<Object,Object> {
    String filterString;
    String[][] structuredStringFilter=null;
        
    @Override
    public boolean include(Entry<? extends Object, ? extends Object> entry) {
     if (filterString==null) return true;
     ManualSelectionTableModel model= (ManualSelectionTableModel) entry.getModel();
     int row = ((Integer)entry.getIdentifier()).intValue();
     String comparator=(String)filterOperator.getSelectedItem();
     Object value=model.getValueAt(row,FILTER_COLUMN);
     String filterColumn=model.getFilterColumn();
     Class classtype=filterPropertyClass.get(filterColumn);
     if (Number.class.isAssignableFrom(classtype)) {
         try {
            double numeric=0;
            if (value instanceof Integer) numeric=(double)((Integer)value).intValue();
            else numeric=((Double)value).doubleValue();
            Double filterNumber = Double.parseDouble(filterString);
                 if (comparator.equals("="))  return numeric==filterNumber;
            else if (comparator.equals("<>")) return numeric!=filterNumber;
            else if (comparator.equals("<"))  return numeric<filterNumber;
            else if (comparator.equals("<=")) return numeric<=filterNumber;
            else if (comparator.equals(">"))  return numeric>filterNumber;
            else if (comparator.equals(">=")) return numeric>=filterNumber;
         } catch (Exception e) {
             //System.err.println(e.getClass().getSimpleName()+":"+e.getMessage());
             return false;
         }
    } else if (filterColumn.equals("somespecial value")) { // This is just an example if you need to expand later for specific properties
         boolean showContains=(comparator.equals("=") || comparator.equals("<=") || comparator.equals(">="));
//         if (value==null) return !showContains;
//         Sequence sequence=(Sequence)model.getValueAt(row,1);
//         String motifname=(sequence.getShortName()+", "+sequence.getLongName()).toLowerCase();
         boolean contains=false;
//         if (filterString.contains("|") || filterString.contains(",")) {
//             String[] parts=filterString.split(",|\\|");
//             for (String part:parts) {
//                if (part.isEmpty()) continue;
//                if (motifname.contains(part)) {contains=true;break;}
//             }
//         } else contains=motifname.contains(filterString);
         return ((showContains && contains) || (!showContains && !contains));         
     } else { // other textual properties. These are only compared with "equals" or "not equals", not by alphabetical sorting
         boolean showContains=(comparator.equals("=") || comparator.equals("<=") || comparator.equals(">=")); // Use "equals" comparison if operator contains the "=" sign
         if (value==null) return !showContains;
         String textvalue=value.toString().toLowerCase();
         boolean contains=advancedStringContains(textvalue);
         return ((showContains && contains) || (!showContains && !contains));       
     }
     return false;
  }   
    
    public void updateFilter() {
       filterString=filterTextfield.getText();   
       if (filterString!=null) filterString=filterString.trim().toLowerCase();
       if (filterString.isEmpty()) filterString=null;
       else {
          if (filterString.indexOf('|')<0 && filterString.indexOf(',')<0 && filterString.indexOf('&')<0) {structuredStringFilter=null;return;} // not a boolean search
          else {
              String[] ors=filterString.split("\\s*(,|\\|)\\s*"); 
              structuredStringFilter=new String[ors.length][];
              for (int i=0;i<ors.length;i++) {
                 String[] ands=ors[i].split("\\s*&\\s*"); 
                 structuredStringFilter[i]=ands;
              }
          }
       }
    }
    
   /** The 'value' is checked against a set of filters and a value of TRUE is
     * returned if the value satisfies the filters. 
     * The filter (which is global) is a 2D list of Strings. 
     * To return TRUE, the 'value' must match either of the filters at the outer level (OR).
     * To match a filter at the outer level the 'value' must match all of the filters 
     * at the inner level (AND)
     */
   private boolean advancedStringContains(String value) {       
       if (structuredStringFilter==null) return value.contains(filterString);
       for (int i=0;i<structuredStringFilter.length;i++) { // for each OR-level
           String[] ands=structuredStringFilter[i]; // must match all entries in this         
           if (ands!=null && ands.length>0) {
              int matches=0;
              for (String string:ands) if (string.isEmpty() || value.contains(string)) matches++;
              if (matches==ands.length) return true; // matching all AND entries 
           }
       }
       return false;
   }    
    
} // end class Filter    

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
