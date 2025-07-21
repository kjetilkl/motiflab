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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
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
import javax.swing.JToolTip;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import org.motiflab.engine.DataListener;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModulePartition;

/**
 * This is the browser panel used by the ModuleBrowser dialog
 * @author kjetikl
 */
public class ModuleBrowserPanel extends JPanel implements DataListener {
    static Boolean showOnlyMembersDefault=Boolean.TRUE;    
    
    private JTable manualSelectionTable;
    private DefaultTableColumnModel columnModel;
    private JTextField filterTextfield;
    private JComboBox filterOperator;
    private JLabel numberOfModulesShowingLabel;
    private Filter filter;
    private JCheckBox showOnlyCollectionMembersCheckbox;    
    private ManualSelectionTableModel model;
    private ManualSelectionContextMenu manualSelectionContextMenu;
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private ModuleCollection modulecollection=null;
    private String initialFilterChoice="Cardinality";
    private String[] columnHeaders=new String[] {" ","Module","Filter","Logo"}; // the name of the Filter column will be changed later on 
    private String[] filterColumnHeaders=new String[]{"ID","Motifs","Motif names","Motif IDs","Cardinality","Ordered","Oriented","Max length"};
    private HashMap<String,Class> filterPropertyClass=null;
    private SingleMotifTooltip customTooltip;
    private boolean isModal=false;

    private static final int SELECTED_COLUMN=0;
    private static final int MODULE_COLUMN=1;
    private static final int FILTER_COLUMN=2;    
    private static final int LOGO_COLUMN=3;    

    private static ImageIcon blackbullet;
    private static ImageIcon greenbullet;    
    
    public ModuleBrowserPanel(MotifLabGUI gui, ModuleCollection modulecollection, boolean modal, boolean allowCreateCollection) {
        super();
        this.gui=gui;
        this.engine=gui.getEngine();
        this.modulecollection=modulecollection;
        this.isModal=modal;
        if (ModuleBrowserPanel.blackbullet==null) {
            java.net.URL greenBulletURL=getClass().getResource("resources/icons/greenbullet.png");
            java.net.URL blackBulletURL=getClass().getResource("resources/icons/blackbullet.png");
            ModuleBrowserPanel.blackbullet=new ImageIcon(blackBulletURL); 
            ModuleBrowserPanel.greenbullet=new ImageIcon(greenBulletURL);     
        }        
        setupManualEntryPanel(allowCreateCollection);
        if (modulecollection==null) {
              columnModel.getColumn(SELECTED_COLUMN).setCellRenderer(new FeatureColorCellRenderer(gui));
        } else ((javax.swing.JComponent)manualSelectionTable.getDefaultRenderer(Boolean.class)).setOpaque(true); // fixes alternating background bug for checkbox renderers in Nimbus
    }
    private void setupFilterColumns() {
        filterPropertyClass=new HashMap<String,Class>(filterColumnHeaders.length);        
        filterPropertyClass.put("ID",String.class);
        filterPropertyClass.put("Motifs",String.class);
        filterPropertyClass.put("Motif names",String.class);
        filterPropertyClass.put("Motif IDs",String.class);
        filterPropertyClass.put("Cardinality",Integer.class);
        filterPropertyClass.put("Ordered",String.class); // I will convert this boolean to a YES/NO string
        filterPropertyClass.put("Oriented",String.class);  // I will convert this boolean to a YES/NO string
        filterPropertyClass.put("Max length",Integer.class);        
    }
    
    
    @SuppressWarnings("unchecked")
    private void setupManualEntryPanel(boolean allowCreateCollection) {
        setupFilterColumns();
        ArrayList<Data> modules=engine.getAllDataItemsOfType(ModuleCRM.class);
        model=new ManualSelectionTableModel(modules,columnHeaders);
        columnModel = new DefaultTableColumnModel();
        for (int i=0;i<columnHeaders.length;i++) {
            TableColumn col = new TableColumn(i);
            col.setHeaderValue(model.getColumnName(i));
            columnModel.addColumn(col);
        }
        manualSelectionTable=new JTable(model,columnModel) {
            @Override
            public JToolTip createToolTip() {
                if (customTooltip==null) {
                    customTooltip=new SingleMotifTooltip(gui.getVisualizationSettings());
                    customTooltip.setComponent(this);
                }
                return customTooltip;
            }
        };
        manualSelectionTable.setAutoCreateRowSorter(true);
        manualSelectionTable.getTableHeader().setReorderingAllowed(false);
        manualSelectionTable.setRowHeight(32);
        columnModel.getColumn(SELECTED_COLUMN).setMinWidth(22);
        columnModel.getColumn(SELECTED_COLUMN).setMaxWidth(22);
        columnModel.getColumn(MODULE_COLUMN).setPreferredWidth(100);
        columnModel.getColumn(LOGO_COLUMN).setPreferredWidth(200);
        JScrollPane manualSelectionScrollPane=new JScrollPane(manualSelectionTable);
        this.setLayout(new BorderLayout());
        this.add(manualSelectionScrollPane,BorderLayout.CENTER);
        JPanel filterPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        final JComboBox filterCombobox=new JComboBox(filterColumnHeaders);
        filterTextfield=new JTextField(12);                  
        filterOperator = new JComboBox(new String[]{"~","!~","=","<>","<","<=",">=",">"}); // matching, not matching, equals, not equals, smaller than...    
        numberOfModulesShowingLabel = new JLabel();
        
        if (this.modulecollection!=null) {
            showOnlyCollectionMembersCheckbox=new JCheckBox();  
            showOnlyCollectionMembersCheckbox.setSelectedIcon(greenbullet);
            showOnlyCollectionMembersCheckbox.setIcon(blackbullet);     
            showOnlyCollectionMembersCheckbox.setRolloverEnabled(false); // colors should not change on rollover                     
            showOnlyCollectionMembersCheckbox.setToolTipText("Click to toggle between showing only modules included in the collection (green) or all modules (black)");
            filterPanel.add(showOnlyCollectionMembersCheckbox);
        }       
        filterPanel.add(new JLabel("  Filter "));        
        filterPanel.add(filterCombobox);
        filterPanel.add(filterOperator);
        filterPanel.add(filterTextfield);
        filterPanel.add(numberOfModulesShowingLabel);
        this.add(filterPanel,BorderLayout.SOUTH);
        final VisualizationSettings settings=gui.getVisualizationSettings();
        columnModel.getColumn(MODULE_COLUMN).setCellRenderer(new CellRenderer_Module());
        columnModel.getColumn(FILTER_COLUMN).setCellRenderer(new CellRenderer_FilterColumn());
        columnModel.getColumn(LOGO_COLUMN).setCellRenderer(new ModuleLogo(settings));
        ((TableRowSorter)manualSelectionTable.getRowSorter()).setComparator(FILTER_COLUMN, new Sorter());        
        ((TableRowSorter)manualSelectionTable.getRowSorter()).setComparator(LOGO_COLUMN, new ModuleLogoComparator());        
        manualSelectionTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE && modulecollection!=null) {
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
                } else if ((e.getKeyCode()==KeyEvent.VK_SPACE && modulecollection==null) || e.getKeyCode()==KeyEvent.VK_V) {
                    int[] rows=manualSelectionTable.getSelectedRows();
                    int visibleRows=0;
                    for (int row:rows) {
                        ModuleCRM cisRegModule=(ModuleCRM)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MODULE_COLUMN);
                        if (settings.isRegionTypeVisible(cisRegModule.getName())) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    for (int row:rows) {
                        ModuleCRM cisRegModule=(ModuleCRM)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MODULE_COLUMN);
                        settings.setRegionTypeVisible(cisRegModule.getName(),doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown()) {
                    int[] rows=manualSelectionTable.getSelectedRows();
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    for (int row:rows) {
                        ModuleCRM cisRegModule=(ModuleCRM)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MODULE_COLUMN);
                        settings.setRegionTypeVisible(cisRegModule.getName(),doShow,false);                        
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown()) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    setVisibilityOnAllModules(doShow);
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    setVisibilityOnAllModules(false);                    
                    int[] rows=manualSelectionTable.getSelectedRows();
                    for (int row:rows) {
                        ModuleCRM cisRegModule=(ModuleCRM)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MODULE_COLUMN);
                        settings.setRegionTypeVisible(cisRegModule.getName(),true,false);
                    }
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder string=new StringBuilder();
                     int[] rows=manualSelectionTable.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';                     
                     boolean first=true;
                     for (int row:rows) {
                         if (first) first=false; else string.append(separator);
                         int modelrow=manualSelectionTable.convertRowIndexToModel(row);
                         String name=((ModuleCRM)model.getValueAt(modelrow, 1)).getName();
                         string.append(name);
                     }
                     String modulestring=string.toString();
                     gui.logMessage(modulestring);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(modulestring);
                     java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                     clipboard.setContents(stringSelection, null);
                 }
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
        filterOperator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filter.updateFilter();
                ((TableRowSorter)manualSelectionTable.getRowSorter()).sort();
                updateCountLabelText();
            }
        });          
        filterCombobox.setSelectedIndex(0);
        filterCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newFilterColumn=(String)filterCombobox.getSelectedItem();
                model.setFilterColumn(newFilterColumn);
                columnModel.getColumn(FILTER_COLUMN).setHeaderValue(newFilterColumn);     
                manualSelectionTable.getTableHeader().repaint();  
                manualSelectionTable.repaint();                 
                filter.updateFilter();                
                // ((TableRowSorter)manualSelectionTable.getRowSorter()).sort();
            }
        });            
        if (showOnlyCollectionMembersCheckbox!=null) {
            showOnlyCollectionMembersCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean showOnlyMembers=showOnlyCollectionMembersCheckbox.isSelected(); 
                    filter.showOnlyCollectionMembers=showOnlyMembers;
                    showOnlyMembersDefault=showOnlyMembers;
                    filter.updateFilter();
                    ((TableRowSorter)manualSelectionTable.getRowSorter()).sort();
                    updateCountLabelText();           
                }
            });
        }        
        manualSelectionContextMenu=new ManualSelectionContextMenu(modulecollection!=null, allowCreateCollection);
        manualSelectionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount()==2) {
                    int column = manualSelectionTable.columnAtPoint(evt.getPoint()); //
                    if (column==0) return; // clicked the checkbox
                    int modelrow=manualSelectionTable.convertRowIndexToModel(manualSelectionTable.getSelectedRow());
                    ModuleCRM cisRegModule=(ModuleCRM)model.getValueAt(modelrow, 1);
                    if (cisRegModule!=null) {
                        gui.showPrompt(cisRegModule, false, isModal);
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
        if (modules.size()>0) { // set selected on modules in collection
            manualSelectionTable.getRowSorter().toggleSortOrder(MODULE_COLUMN);
            if (modulecollection!=null) {
               for (int i=0;i<model.getRowCount();i++) {
                   ModuleCRM cisRegModule=(ModuleCRM)model.getValueAt(i, 1);
                   model.setValueAt(modulecollection.contains(cisRegModule), i, 0);
               }
               manualSelectionTable.getRowSorter().toggleSortOrder(SELECTED_COLUMN); // show checked first
               manualSelectionTable.getRowSorter().toggleSortOrder(SELECTED_COLUMN); // show checked first - must sort twice to get checked on top
            }
        }          

        filterCombobox.setSelectedItem(initialFilterChoice);   
        if (showOnlyCollectionMembersCheckbox!=null) {
            boolean defaultValue=(modulecollection.isEmpty())?false:showOnlyMembersDefault;
            if (defaultValue==true) showOnlyCollectionMembersCheckbox.doClick(); // the doClick is necessary to update the filter, setSelected() is not enough       
        }   
        updateCountLabelText();        
    } // end setupManualEntryPanel()


    public JTable getTable() {
        return manualSelectionTable;
    }

    /** Updates the current ModuleCRM Collection (if set) to reflect the new selection */
    public void updateModuleCollection() {
        if (modulecollection==null) return;
        modulecollection.clearAll(engine);
        for (int i=0;i<model.getRowCount();i++) {
            if ((Boolean)model.getValueAt(i,0)) {
                ModuleCRM cisRegModule=(ModuleCRM)model.getValueAt(i, 1);
                modulecollection.addModule((ModuleCRM)cisRegModule);
            }
        }
    }

    @Override
    public void dataAdded(Data data) {
        if (!(data instanceof ModuleCRM)) return;
        model.addModule((ModuleCRM)data);
    }

    @Override
    public void dataRemoved(Data data) {
        if (!(data instanceof ModuleCRM)) return;
        model.removeModule((ModuleCRM)data);
    }

    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {}
    @Override
    public void dataUpdated(Data data) {
         if (!(data instanceof ModuleCRM)) return;
         model.fireTableDataChanged();
    }

    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {}

    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) { }

    /** Shows or hides all module depending on the argument, but does not update visualization */
    public void setVisibilityOnAllModules(boolean visible) {
         VisualizationSettings settings=gui.getVisualizationSettings();
         for (int i=0;i<model.getRowCount();i++) {
             settings.setRegionTypeVisible(((ModuleCRM)model.getValueAt(i,1)).getName(), visible, false);
         }
    }

    /** Returns the number of modules currently selected under Manual Entry tab*/
    private int countSelectedModules() {
         int selected=0;
         for (int i=0;i<model.getRowCount();i++) {
             if ((Boolean)model.getValueAt(i,0)) selected++;
         }
         return selected;
    }
    /** Updates the label displaying counts (shown, selected and total) in the manual selection tab*/
    private void updateCountLabelText() {
        int total=model.getRowCount();
        int included = countSelectedModules();
        int matchingAgainst = (showOnlyMembersDefault)?included:total;
        if (modulecollection!=null) numberOfModulesShowingLabel.setText("<html>Matching: "+manualSelectionTable.getRowCount()+" of "+matchingAgainst+"<br>Included: "+included+" of "+total+"</html>");
        else numberOfModulesShowingLabel.setText("<html>Matching: "+manualSelectionTable.getRowCount()+" of "+total+"</html>");
    }


     private void showContextMenu(MouseEvent evt) {
        int selectedCount=manualSelectionTable.getSelectedRowCount();
        int firstSelected=manualSelectionTable.getSelectedRow();
        if (firstSelected>=0 && selectedCount==1) {
            int modelrow=manualSelectionTable.convertRowIndexToModel(firstSelected);
            manualSelectionContextMenu.setSingleModuleSelected(((ModuleCRM)model.getValueAt(modelrow,1)).getName());
        } else {          
            manualSelectionContextMenu.setSingleModuleSelected(null);
        }
        manualSelectionContextMenu.updateMenu();
        manualSelectionContextMenu.show(evt.getComponent(), evt.getX(),evt.getY());  
     }


private class ManualSelectionTableModel extends AbstractTableModel { 
    private ArrayList<Boolean> checked=null;
    private ArrayList<Data> modules=null;
    private String[] columnNames=null;
    private String filterColumn="Cardinality";
    
    public ManualSelectionTableModel(ArrayList<Data> modules, String[] columnNames) {
        this.modules=modules;
        checked=new ArrayList<Boolean>(modules.size());
        for (int i=0;i<modules.size();i++) checked.add(Boolean.FALSE);
        this.columnNames=columnNames;
    }
    
    @Override
    public Class getColumnClass(int c) {
        if (c==SELECTED_COLUMN) return Boolean.class; // first column
        else if (c==MODULE_COLUMN || c==LOGO_COLUMN) return ModuleCRM.class; // 
        else { // c==FILTER_COLUMN
            return filterPropertyClass.get(filterColumn);
        }
    }
    
    @Override    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return (columnIndex==0 && modulecollection!=null);
    }
    
    private String getFilterColumn() {return filterColumn;}
    private void setFilterColumn(String filterColumn) {this.filterColumn=filterColumn;}
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        checked.set(rowIndex,(Boolean)aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void addModule(ModuleCRM newmodule) {
        int index=modules.size();
        modules.add(newmodule);
        checked.add(Boolean.FALSE);
        try {
           fireTableRowsInserted(index, index);
        } catch (ArrayIndexOutOfBoundsException e) {}        
    }

    public void removeModule(ModuleCRM cisRegModule) {
        int index=modules.indexOf(cisRegModule);
        if (index>=0) {
            modules.remove(index);
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
        return modules.size();
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex==SELECTED_COLUMN) return checked.get(rowIndex);
        else if (columnIndex==MODULE_COLUMN || columnIndex==LOGO_COLUMN) return (ModuleCRM)modules.get(rowIndex);
        else { // value from filter column
                 if (filterColumn.equals("ID")) return ((ModuleCRM)modules.get(rowIndex)).getName();
            else if (filterColumn.equals("Motifs")) return ((ModuleCRM)modules.get(rowIndex)).getSingleMotifNamesAsString(",");
            else if (filterColumn.equals("Motif names")) return ((ModuleCRM)modules.get(rowIndex)).getSingleMotifShortNamesAsString(engine);
            else if (filterColumn.equals("Motif IDs")) return ((ModuleCRM)modules.get(rowIndex)).getSingleMotifIDsAsString();
            else if (filterColumn.equals("Cardinality")) return ((ModuleCRM)modules.get(rowIndex)).getCardinality();
            else if (filterColumn.equals("Ordered")) return convertBooleanToString(((ModuleCRM)modules.get(rowIndex)).isOrdered());
            else if (filterColumn.equals("Oriented")) return convertBooleanToString(((ModuleCRM)modules.get(rowIndex)).isOriented());
            else if (filterColumn.equals("Max length")) return ((ModuleCRM)modules.get(rowIndex)).getMaxLength();
            else return "error";
        }
    }

    @Override
    public String getColumnName(int column) {
        if (column==FILTER_COLUMN) return filterColumn;
        else return columnNames[column];
    }
    
    private String convertBooleanToString(Boolean value) {
        if (value!=null && value.booleanValue()) return "Yes";
        else return "No";
    } 
    
}    

private class CellRenderer_Module extends DefaultTableCellRenderer {
    public CellRenderer_Module() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
    }
    @Override
    public void setValue(Object value) {
       if (value!=null && value instanceof ModuleCRM) {
           setText(((ModuleCRM)value).getName());
       }
       else setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        ModuleCRM cisRegModule=(ModuleCRM)table.getValueAt(row, MODULE_COLUMN);
        if (modulecollection==null) { // if used as MotifBrowser (and not to create a motif collection)
           if (!gui.getVisualizationSettings().isRegionTypeVisible(cisRegModule.getName())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        if (customTooltip!=null) setToolTipText(cisRegModule.getName());
        return c;
    }
    }// end class CellRenderer_Module


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
        ModuleCRM cisRegModule=(ModuleCRM)table.getValueAt(row, MODULE_COLUMN);
        if (modulecollection==null) { // if used as ModuleBrowser (and not to create a module collection)
           if (!gui.getVisualizationSettings().isRegionTypeVisible(cisRegModule.getName())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        return c;
    }

}// end class CellRenderer_FilterColumn  


/** This private class implements the context menu */
private class ManualSelectionContextMenu extends JPopupMenu implements ActionListener {
     private final String DISPLAY_MODULE="Display";
     private final String SHOW="Show";
     private final String HIDE="Hide";
     private final String SHOW_ONLY_SELECTED="Show Only Selected";   
     private final String SHOW_ALL="Show All";  
     private final String HIDE_ALL="Hide All";      
     private final String SELECT_SHOWN_MODULES="Select Shown Modules";
     private final String SELECT_ONLY_SHOWN_MODULES="Select Only Shown Modules";
     private final String SELECT_MODULES_FROM="Select Modules From";
     private final String SELECT_ONLY_MODULES_FROM="Select Only Modules From";
     private final String CREATE_MODULE_COLLECTION="Create Module Collection";    
     private final String COLOR_SUBMENU_HEADER="Set Color"; 
     
     JMenuItem includeSelectedItem=new JMenuItem("Include Selected Modules");
     JMenuItem excludeSelectedItem=new JMenuItem("Exclude Selected Modules");
     JMenuItem includeListedItem=new JMenuItem("Include Matching Modules");
     JMenuItem includeNotListedItem=new JMenuItem("Include Not Matching Modules");
     JMenuItem excludeListedItem=new JMenuItem("Exclude Matching Modules");
     JMenuItem excludeNotListedItem=new JMenuItem("Exclude Not Matching Modules");
     JMenuItem includeAllItem=new JMenuItem("Include All Modules");
     JMenuItem excludeAllItem=new JMenuItem("Exclude All Modules");
     JMenuItem invertCollectionItem=new JMenuItem("Invert Collection");
     JMenuItem displayItem=new JMenuItem(DISPLAY_MODULE);
     JMenu selectModulesFromMenu=new JMenu(SELECT_MODULES_FROM);
     JMenu selectOnlyModulesFromMenu=new JMenu(SELECT_ONLY_MODULES_FROM);
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
         ColorMenuListener colormenulistener=new ColorMenuListener() {
             public void newColorSelected(Color color) {
                if (color==null) return;
                int[] selectedRows = manualSelectionTable.getSelectedRows();
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (int i=0;i<selectedRows.length;i++) {
                    int modelrow=manualSelectionTable.convertRowIndexToModel(selectedRows[i]);
                    settings.setFeatureColor(((ModuleCRM)model.getValueAt(modelrow, 1)).getName(), color, false);
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
         JMenuItem selectShownModulesItem=new JMenuItem(SELECT_SHOWN_MODULES);
         JMenuItem selectOnlyShownModulesItem=new JMenuItem(SELECT_ONLY_SHOWN_MODULES);
         JMenuItem createCollectionItem=new JMenuItem(CREATE_MODULE_COLLECTION);        

         updateMenu();
                  
         displayItem.addActionListener(this);
         showItem.addActionListener(this);
         showOnlySelectedItem.addActionListener(this);
         showAllItem.addActionListener(this);
         hideItem.addActionListener(this);
         hideAllItem.addActionListener(this);     
         selectShownModulesItem.addActionListener(this);     
         selectOnlyShownModulesItem.addActionListener(this);
         if (allowCreateCollection) createCollectionItem.addActionListener(this);
        
         setColorMenu=new ColorMenu(COLOR_SUBMENU_HEADER, colormenulistener, ModuleBrowserPanel.this);
         this.add(displayItem);         
         this.add(showItem);
         this.add(showOnlySelectedItem); 
         this.add(showAllItem);            
         this.add(hideItem);   
         this.add(hideAllItem);              
         this.add(setColorMenu);
         this.add(new JSeparator());
         this.add(selectShownModulesItem);
         this.add(selectOnlyShownModulesItem);
         this.add(selectModulesFromMenu);
         this.add(selectOnlyModulesFromMenu);
         if (allowCreateCollection) {
             this.add(new JSeparator());
             this.add(createCollectionItem);
         }
       
     }

    public final void updateMenu() {
        selectModulesFromMenu.removeAll();
        selectOnlyModulesFromMenu.removeAll();
        for (String collectionName:engine.getNamesForAllDataItemsOfType(ModuleCollection.class)) {
            JMenuItem subitem=new JMenuItem(collectionName);
            subitem.addActionListener(selectFromCollectionListener);
            selectModulesFromMenu.add(subitem);
            JMenuItem subitem2=new JMenuItem(collectionName);
            subitem2.addActionListener(clearAndselectFromCollectionListener);
            selectOnlyModulesFromMenu.add(subitem2);
        }
        for (String partitionName:engine.getNamesForAllDataItemsOfType(ModulePartition.class)) {
            Data data=engine.getDataItem(partitionName);
            if (data instanceof ModulePartition) {
                JMenu selectModulesFromMenuCluster=new JMenu(data.getName());               
                JMenu selectOnlyModulesFromMenuCluster=new JMenu(data.getName());               
                for (String cluster:((ModulePartition)data).getClusterNames()) {                    
                    JMenuItem subitem=new JMenuItem(cluster);
                    subitem.setActionCommand(partitionName+"."+cluster);
                    subitem.addActionListener(selectFromCollectionListener);
                    selectModulesFromMenuCluster.add(subitem);
                    JMenuItem subitem2=new JMenuItem(cluster);
                    subitem2.setActionCommand(partitionName+"."+cluster);
                    subitem2.addActionListener(clearAndselectFromCollectionListener);
                    selectOnlyModulesFromMenuCluster.add(subitem2);
                }
                selectModulesFromMenu.add(selectModulesFromMenuCluster);
                selectOnlyModulesFromMenu.add(selectOnlyModulesFromMenuCluster);
            }
        }
        selectModulesFromMenu.setEnabled(selectModulesFromMenu.getMenuComponentCount()>0);
        selectOnlyModulesFromMenu.setEnabled(selectOnlyModulesFromMenu.getMenuComponentCount()>0);
    }    
    
    /** This should be called when the context menu is invoked with only one selected module */
    public void setSingleModuleSelected(String targetModuleName) {
        if (targetModuleName==null) {
             displayItem.setVisible(false);
        } else {
            displayItem.setText(DISPLAY_MODULE+" "+targetModuleName);
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
         } else if (e.getActionCommand().startsWith(DISPLAY_MODULE)) {
                String[] selectedModuleNames=getSelectedModuleNames();
                Data cisRegModule=gui.getEngine().getDataItem(selectedModuleNames[0]);
                if (cisRegModule instanceof ModuleCRM) gui.showPrompt((ModuleCRM)cisRegModule, false, isModal);
           } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                setVisibilityOnAllModules(false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String modulename:getSelectedModuleNames()) {
                   settings.setRegionTypeVisible(modulename, true, false);   
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(SHOW_ALL)) {
                setVisibilityOnAllModules(true);
                gui.redraw();                        
           } else if (e.getActionCommand().equals(HIDE_ALL)) {
                setVisibilityOnAllModules(false);
                gui.redraw(); 
           } else if (e.getActionCommand().equals(SHOW)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String modulename:getSelectedModuleNames()) {
                   settings.setRegionTypeVisible(modulename, true, false);   
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(HIDE)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String modulename:getSelectedModuleNames()) {
                   settings.setRegionTypeVisible(modulename, false, false);   
                }
                gui.redraw();
           } else if (e.getActionCommand().equals(CREATE_MODULE_COLLECTION)) {
                String[] selectedModuleNames=getSelectedModuleNames();
                ArrayList<String> list=new ArrayList<String>(selectedModuleNames.length);
                list.addAll(Arrays.asList(selectedModuleNames));
                gui.promptAndCreateModuleCollection(list);
           } else if (e.getActionCommand().equals(SELECT_SHOWN_MODULES)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allModules=engine.getNamesForAllDataItemsOfType(ModuleCRM.class);
                HashSet<String> shownModules=new HashSet<String>(allModules.size());
                for (String modulename:allModules) {
                   if (settings.isRegionTypeVisible(modulename)) shownModules.add(modulename);
                }
                selectRowsForModules(shownModules,false);
           } else if (e.getActionCommand().equals(SELECT_ONLY_SHOWN_MODULES)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allModules=engine.getNamesForAllDataItemsOfType(ModuleCRM.class);
                HashSet<String> shownModules=new HashSet<String>(allModules.size());
                for (String modulename:allModules) {
                   if (settings.isRegionTypeVisible(modulename)) shownModules.add(modulename);
                }
                selectRowsForModules(shownModules,true);
           }
         manualSelectionTable.repaint();
         updateCountLabelText();
     }

     
         /** Returns the names of the modules in currently selected rows of the tabel
     *  or NULL if no rows are currently selected
     */
    private String[] getSelectedModuleNames() {
        int[] selection=manualSelectionTable.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        String[] selectedModuleNames=new String[selection.length];
        for (int i=0;i<selection.length;i++) {
            Object cisRegModule=manualSelectionTable.getValueAt(selection[i], MODULE_COLUMN);
            if (cisRegModule instanceof ModuleCRM) selectedModuleNames[i]=((ModuleCRM)cisRegModule).getName();   
            else return null;
        }        
        return selectedModuleNames;
    }
    
} // END private class ManualSelectionContextMenu

  private class SelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectModulesFromCollection(collectionName,false);
       }
   }
  private class ClearAndSelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectModulesFromCollection(collectionName,true);
       }
   }

    private void selectModulesFromCollection(String collectionName, boolean clearCurrentSelection) {
           HashSet<String> modulenames=new HashSet<String>();
           int point=collectionName.indexOf('.');
           if (point>0) { // partition
               String clusterName=collectionName.substring(point+1);
               collectionName=collectionName.substring(0,point);
               Data col=engine.getDataItem(collectionName);
               if (col instanceof ModulePartition) modulenames.addAll( ((ModulePartition)col).getAllModuleNamesInCluster(clusterName) );
                                 
           } else { // collection
               Data col=engine.getDataItem(collectionName);
               if (col instanceof ModuleCollection) modulenames.addAll( ((ModuleCollection)col).getAllModuleNames());                         
           }
           selectRowsForModules(modulenames,clearCurrentSelection);          
    }

    private void selectRowsForModules(HashSet<String> modules, boolean clearCurrentSelection) {
        if (clearCurrentSelection) manualSelectionTable.clearSelection();
        ListSelectionModel selection=manualSelectionTable.getSelectionModel();        
        int modulecolumn=MODULE_COLUMN;
        for (int i=0;i<manualSelectionTable.getRowCount();i++) {
            Object value=manualSelectionTable.getValueAt(i, modulecolumn);      
            String modulename=((ModuleCRM)value).getName();
            if (modules.contains(modulename)) {
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
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); // this is necessary in order to get correct alternating row rendering 
        ModuleCRM cisRegModule=(ModuleCRM)table.getValueAt(row, column+1);
        String modulename=cisRegModule.getName();
        selectedicon.setForegroundColor(settings.getFeatureColor(modulename));
        selectedicon.setBorderColor((settings.isRegionTypeVisible(modulename))?Color.BLACK:Color.LIGHT_GRAY);
        if (customTooltip!=null) setToolTipText(cisRegModule.getName());
        setText(null); 
        return this;
    }           
    }   

    /** A class to sort module logos based on the names of ModuleMotifs in order */
    private class ModuleLogoComparator implements java.util.Comparator<ModuleCRM> {
        @Override
        public int compare(ModuleCRM mod1, ModuleCRM mod2) {
            int size1=mod1.getCardinality();
            int size2=mod2.getCardinality();
            int size=(size1<size2)?size1:size2;
            for (int i=0;i<size;i++) {
                String name1=mod1.getSingleMotifName(i);
                String name2=mod2.getSingleMotifName(i);
                int compare=name1.compareTo(name2);
                if (compare!=0) return compare;
            }
            return size1-size2;
        }  
    }
    
private class Filter extends RowFilter<Object,Object> {
    String filterString;
    String[][] structuredStringFilter=null;
    boolean showOnlyCollectionMembers=false;    
        
    @Override
    public boolean include(Entry<? extends Object, ? extends Object> entry) {
     if (filterString==null && !showOnlyCollectionMembers) return true; // no filtering at all applied        
     ManualSelectionTableModel model= (ManualSelectionTableModel) entry.getModel();
     int row = ((Integer)entry.getIdentifier()).intValue();
     if (showOnlyCollectionMembers) {
         Object value=model.getValueAt(row,SELECTED_COLUMN);
         if (value instanceof Boolean && !((Boolean)value)) return false; // do not show if not in collection
     } 
     if (filterString==null) return true; // no property filtering applied     
     String comparator=(String)filterOperator.getSelectedItem();
     Object value=model.getValueAt(row,FILTER_COLUMN);
     String filterColumn=model.getFilterColumn();
     Class classtype=filterPropertyClass.get(filterColumn);
     if (Number.class.isAssignableFrom(classtype)) { // should I allow multiple numbers?
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
     } else { // other textual properties
         boolean showContains=(comparator.equals("=") || comparator.equals("~") || comparator.equals("<=") || comparator.equals(">="));
         if (value==null) return !showContains; // the motif has no value for this property
         String textvalue;
         if (value instanceof List) textvalue=MotifLabEngine.splice((List)value, ",");
         else textvalue=value.toString().toLowerCase();
         if (structuredStringFilter!=null && (comparator.equals("!~") || comparator.equals("<>"))) { 
             // we have a boolean expression with a negating comparator. Instead of doing a complex deMorgan transform of the expression, we switch the comparator and negate the result instead 
             return !advancedStringMatches(textvalue,(comparator.equals("!~")?"~":"=")); // change comparator to the non-negated counterpart
         } 
         return advancedStringMatches(textvalue,comparator);    
     }
     return false;
  }   
    
    public void updateFilter() {
       filterString=filterTextfield.getText();   
       if (filterString!=null) filterString=filterString.trim().toLowerCase();
       if (filterString.isEmpty()) {
           filterString=null;
           structuredStringFilter=null;
       }
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
    * returned if the value satisfies the filters according to the chosen comparator 
    * The filter (which is global to this method) is a 2D list of Strings, e.g: [ [X and X and X] or [X] or [X and X] ]
    * To return TRUE, the 'value' must match either of the filters at the outer level (OR).
    * To match a filter at the outer level the 'value' must match all of the filters at the inner level (AND)
    */   
    private boolean advancedStringMatches(String value, String comparator) {         
        if (structuredStringFilter==null) return (filterString==null)?true:stringMatches(value,filterString,comparator);
        for (int i=0;i<structuredStringFilter.length;i++) { // for each OR-level
            String[] ands=structuredStringFilter[i]; // must match all entries in this         
            if (ands!=null && ands.length>0) {
               int matches=0;
               for (String string:ands) if (string.isEmpty() || stringMatches(value,string,comparator)) matches++;
               if (matches==ands.length) return true; // matching all AND entries 
            }
        }
        return false;
    }     
   
   /**
    * Returns TRUE if the relationship between the value and the target matches according to the comparator
    * Both value and target are expected to be in lowercase (unless they are NULL)
    * @param value
    * @param target
    * @param operator
    * @return 
    */
   private boolean stringMatches(String value, String target, String comparator) {
       if (value==null || target==null) return false; // ?!
       switch(comparator) {
           case "=" : return value.equalsIgnoreCase(target);
           case "<>": return !value.equalsIgnoreCase(target);
           case "~" : return value.contains(target);
           case "!~": return !value.contains(target);
           case "<" : return value.compareTo(target)<0;
           case ">" : return value.compareTo(target)>0;
           case "<=": return value.compareTo(target)<=0;
           case ">=": return value.compareTo(target)>=0;                          
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
