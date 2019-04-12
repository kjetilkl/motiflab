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
import javax.swing.JComponent;
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
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Module;

/**
 * This class is meant as a generic class for displaying tables where each
 * row corresponds to a Module.
 * If any of the columns in the tablemodel has class=Module.class they will
 * be drawn as module logos. Also, if any such column exists, the user will
 * be able to double-click on a row to display a Module prompt
 * @author kjetikl
 */
public class GenericModuleBrowserPanel extends GenericBrowserPanel {
    private JTable table;
    private TableModel model;
    private ModuleLogo logorenderer;
    private MotifLabGUI gui;
    private JPopupMenu contextMenu=null;
    private JPanel controlsPanel=null;
    private JSearchTextField searchfield;
    private ColorCellRenderer colorCellRenderer=null;
    private boolean isModal=false;
    private VisualizationSettings settings=null;
    private JScrollPane scrollPane=null;
    private SingleMotifTooltip customTooltip;    

    
    public GenericModuleBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, boolean modal) {
        this(GUI,tablemodel,true, modal);
    }

    public GenericModuleBrowserPanel(MotifLabGUI GUI, TableModel tablemodel, boolean showControlsPanel, boolean modal) {
        super();
        this.gui=GUI;
        this.isModal=modal;
        model=tablemodel;
        table=new JTable(model) {
            @Override
            public JToolTip createToolTip() {
                if (customTooltip==null) {
                    customTooltip=new SingleMotifTooltip(gui.getVisualizationSettings());
                    customTooltip.setComponent(this);
                }
                return customTooltip;
            }            
        };
        ToolTipHeader header = new ToolTipHeader(table.getColumnModel());
        table.setTableHeader(header);        
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(32);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        scrollPane=new JScrollPane(table);
        this.setLayout(new BorderLayout());
        this.add(scrollPane,BorderLayout.CENTER);
        this.settings=gui.getVisualizationSettings();
        logorenderer=new ModuleLogo(settings);
        logorenderer.showToolTip(true);
        table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
               if (table.isCellEditable(table.getSelectionModel().getLeadSelectionIndex(), table.getColumnModel().getSelectionModel().getLeadSelectionIndex())) return; // do nothing if cell can be edited, so as to not interfere with editing process               
               if (e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_V) {
                    int[] rows=table.getSelectedRows();
                    int visibleRows=0;
                    int modulecolumn=getModuleColumn();
                    if (modulecolumn<0) return;
                    for (int row:rows) {
                        Object module=model.getValueAt(table.convertRowIndexToModel(row),modulecolumn);
                        if (module instanceof Module && settings.isRegionTypeVisible(((Module)module).getName())) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    for (int row:rows) {
                        Object module=model.getValueAt(table.convertRowIndexToModel(row),modulecolumn);
                        if (module instanceof Module) settings.setRegionTypeVisible(((Module)module).getName(),doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown()) {
                    int[] rows=table.getSelectedRows();
                    int modulecolumn=getModuleColumn();
                    if (modulecolumn<0) return;
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    for (int row:rows) {
                        Object module=model.getValueAt(table.convertRowIndexToModel(row),modulecolumn);
                        if (module instanceof Module) settings.setRegionTypeVisible(((Module)module).getName(),doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown()) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    setVisibilityOnAllModules(doShow,true);
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    setVisibilityOnAllModules(false,false);
                    int[] rows=table.getSelectedRows();
                    int modulecolumn=getModuleColumn();
                    if (modulecolumn<0) return;
                    for (int row:rows) {
                        Object module=model.getValueAt(table.convertRowIndexToModel(row),modulecolumn);
                        if (module instanceof Module) settings.setRegionTypeVisible(((Module)module).getName(),true,false);
                    }                  
                    settings.redraw();
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder modulestring=new StringBuilder();
                     int[] rows=table.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';                     
                     boolean first=true;
                     int modulecolumn=getModuleColumn();
                     if (modulecolumn<0) return;
                     for (int row:rows) {
                         int modelrow=table.convertRowIndexToModel(row);
                         Object module=model.getValueAt(modelrow, modulecolumn);
                         if (!(module instanceof Module)) continue;
                         if (first) first=false; else modulestring.append(separator);
                         modulestring.append(((Module)module).getName());
                     }
                     String modules=modulestring.toString();
                     gui.logMessage(modules);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(modules);
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
                    int modulecolumn=getModuleColumn();
                    if (modulecolumn<0) return;
                    Object module=model.getValueAt(modelrow, modulecolumn);
                    if (module instanceof Module) {
                        gui.showPrompt((Module)module, false, isModal);
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
        CellRenderer defaultRenderer=new CellRenderer();
        ModuleLogoComparator logocomparator=new ModuleLogoComparator();
        for (int i=0;i<table.getColumnCount();i++) {
            Class columnclass=model.getColumnClass(i);
            if (columnclass==Module.class) {
                table.getColumnModel().getColumn(i).setCellRenderer(logorenderer);
                ((TableRowSorter)table.getRowSorter()).setComparator(i, logocomparator);
            }
            else if (columnclass==Color.class) {
                if (colorCellRenderer==null) colorCellRenderer=new ColorCellRenderer(getModuleColumn());
                TableColumn column=table.getColumnModel().getColumn(i);
                column.setCellRenderer(colorCellRenderer);
                column.setPreferredWidth(22);
                column.setMinWidth(22);
                column.setMaxWidth(22);
            } else table.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
            if (columnclass==String.class) {
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
        contextMenu=new GenericModuleBrowserPanelContextMenu(this);

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
    
    /** Returns the index of the first column containing Module objects, or -1 if no such column was found */
    public final int getModuleColumn() {
       for (int i=0;i<table.getColumnCount();i++) {
            if (model.getColumnClass(i)==Module.class) return i;
       }
       return -1;
    }

    private void showContextMenu(MouseEvent evt) {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return;
        if (contextMenu instanceof GenericModuleBrowserPanelContextMenu) ((GenericModuleBrowserPanelContextMenu)contextMenu).updateMenu();
        contextMenu.show(evt.getComponent(), evt.getX(),evt.getY());
    }

    /** Returns the names of the modules in currently selected rows of the tabel
     *  or NULL if no rows are currently selected
     */
    public String[] getSelectedModuleNames() {
        int[] selection=table.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        int modulecolumn=getModuleColumn();
        if (modulecolumn<0) return null;
        String[] selectedModuleNames=new String[selection.length];
        for (int i=0;i<selection.length;i++) {
            Object module=table.getValueAt(selection[i], modulecolumn);
            if (module instanceof Module) selectedModuleNames[i]=((Module)module).getName();
            else return null;
        }
        return selectedModuleNames;
    }

    public JPopupMenu getContextMenu() {
        return contextMenu;
    }
    
    public void setContextMenu(JPopupMenu menu) {
        this.contextMenu=menu;
    }
    
    public void disableContextMenu() {
        contextMenu=null;
    }

   /**
     * Adds an additional menu item to the end of the context menu
     * (assuming the default GenericModuleBrowserPanelContextMenu is still used). 
     * @param name
     * @param limitOne If TRUE the item will be disabled if more than one module is selected
     * @param callback
     */
   public void addExtraContextMenuItem(String name, boolean limitOne, ActionListener callback) {
       if (contextMenu instanceof GenericModuleBrowserPanelContextMenu) ((GenericModuleBrowserPanelContextMenu)contextMenu).addExtraMenuItem(name, limitOne, callback);
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
                    String valueString=(value instanceof Module)?(((Module)value).getNamePlusSingleMotifNames()):value.toString();
                    valueString=valueString.toLowerCase();
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

   protected void setVisibilityOnAllModules(boolean show, boolean redraw) {
       for (Data data:gui.getEngine().getAllDataItemsOfType(Module.class)) {
           settings.setRegionTypeVisible(data.getName(), show, false);
       }
       if (redraw) gui.redraw();       
   }    
    
private class ColorCellRenderer extends DefaultTableCellRenderer {
    
    SimpleDataPanelIcon selectedicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
    int modulecolumn=-1;
    
    public ColorCellRenderer(int modulecolumn) {
         super();
         this.modulecolumn=modulecolumn;
         setIcon(selectedicon);  
         setText(null);           
         setHorizontalTextPosition(SwingConstants.RIGHT);
    }
 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component thiscomponent=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (modulecolumn<0) return thiscomponent;
        Module module=(Module)table.getValueAt(row, modulecolumn);
        if (module!=null) {
            String modulename=module.getName();
            boolean isVisible=settings.isRegionTypeVisible(modulename);            
            selectedicon.setForegroundColor(settings.getFeatureColor(modulename));
            selectedicon.setBorderColor((isVisible)?Color.BLACK:Color.LIGHT_GRAY);
            thiscomponent.setForeground((isVisible)?Color.BLACK:Color.LIGHT_GRAY);
        } else {
            selectedicon.setForegroundColor(Color.WHITE);
            selectedicon.setBorderColor(Color.WHITE);
        }
        setText(null);         
        return thiscomponent;
    }    
} 


private class CellRenderer extends DefaultTableCellRenderer {    
    public CellRenderer() {
         super();
    } 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component thiscomponent=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        table.setToolTipText(null);
        return thiscomponent;
    }
}

    /** A class to sort module logos based on the names of ModuleMotifs in order */
    private class ModuleLogoComparator implements java.util.Comparator<Module> {
        @Override
        public int compare(Module mod1, Module mod2) {
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
}
