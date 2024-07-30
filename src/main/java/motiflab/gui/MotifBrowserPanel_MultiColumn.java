
package motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import motiflab.engine.DataListener;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifClassification;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.MotifPartition;

/**
 * This class implements a panel that will display a table of Motifs
 * with 3 columns containing a name, a selected (filter) property and a sequence logo.
 * The panel is used by MotifBrowser and Prompt_MotifCollection
 * @author kjetikl
 */
public class MotifBrowserPanel_MultiColumn extends JPanel implements DataListener {
    static Boolean showOnlyMembersDefault=Boolean.TRUE;
    
    private JTable manualSelectionTable;
    private DefaultTableColumnModel columnModel;
    private JTextField filterTextfield;
    private JComboBox filterOperator;
    private JLabel numberOfMotifsShowingLabel;
    private Filter filter;
    private JCheckBox showOnlyCollectionMembersCheckbox;
    private ManualSelectionTableModel model;    
    private ManualSelectionContextMenu manualSelectionContextMenu;
    private MotifLogo logorenderer;
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private MotifCollection motifcollection=null;
    private String initialFilterChoice="Names + Factors";
    private String[] columnHeaders;
    private String[] standardFilterColumnHeaders=new String[]{"ID","Classification","Consensus","Description","Expression","Factors","GC-content","Information Content","Long Name","Names","Names + Factors","Organisms","Part","PWM match","Quality","Size","Short Name"};
    private String[] filterColumnHeaders=null; // this will be the standardFilterColumnHeaders + non-standard (user-defined) properties
    private HashMap<String,Double> PWMsimilarityCache=new HashMap<String,Double>(); // caches PWM similarity values
    private HashMap<String,Class> filterPropertyClass=null;
    private String oldPWMtarget="";

    private JPanel filterBoxesPanel;
    
    private static final int SELECTED_COLUMN=0; // this should always be the first column
    private static final int MOTIF_COLUMN=1; // this should always be the first column
    private static final int FILTER_COLUMN=2; // this should always be the first column
    private static final int LOGO_COLUMN=3; // this should always be the first column

    private boolean isModal=false;

    public MotifBrowserPanel_MultiColumn(MotifLabGUI gui, MotifCollection motifcollection, boolean modal, boolean allowCreateCollection) {        
        super();
        this.gui=gui;
        this.engine=gui.getEngine();
        this.motifcollection=motifcollection;
        this.isModal=modal;
        setupManualEntryPanel(allowCreateCollection);
        if (motifcollection==null) {   
              columnModel.getColumn(SELECTED_COLUMN).setCellRenderer(new FeatureColorCellRenderer(gui)); // render as colored boxes instead of checkboxes
        } else ((javax.swing.JComponent)manualSelectionTable.getDefaultRenderer(Boolean.class)).setOpaque(true); // fixes alternating background bug for checkbox renderers in Nimbus
    }
    
    private void setupFilterColumns() { // initialize filterColumnHeaders[] and the filterPropertyClass LUT
        String[] userdefined=Motif.getAllUserDefinedProperties(engine);
        filterPropertyClass=new HashMap<String,Class>(standardFilterColumnHeaders.length+userdefined.length);        
        filterPropertyClass.put("ID",String.class);
        filterPropertyClass.put("Classification",String.class);
        filterPropertyClass.put("Consensus",String.class);
        filterPropertyClass.put("Description",String.class);
        filterPropertyClass.put("Expression",String.class);
        filterPropertyClass.put("Factors",String.class);
        filterPropertyClass.put("GC-content",Double.class);
        filterPropertyClass.put("Information Content",Double.class);
        filterPropertyClass.put("Names",String.class);
        filterPropertyClass.put("Names + Factors",String.class);
        filterPropertyClass.put("Short Name",String.class);
        filterPropertyClass.put("Long Name",String.class);        
        filterPropertyClass.put("Organisms",String.class);
        filterPropertyClass.put("Part",String.class);
        filterPropertyClass.put("PWM match",Double.class);
        filterPropertyClass.put("Quality",Integer.class);
        filterPropertyClass.put("Size",Integer.class);
        if (userdefined!=null && userdefined.length>0) {
            for (String key:userdefined) {
                Class propclass=Motif.getClassForUserDefinedProperty(key, engine);
                if (propclass.equals(ArrayList.class)) filterPropertyClass.put(key,String.class); // this will 'magically' convert lists to strings 
                else filterPropertyClass.put(key,propclass);
            }       
            String[] temp=new String[standardFilterColumnHeaders.length+userdefined.length];
            System.arraycopy(standardFilterColumnHeaders, 0, temp, 0, standardFilterColumnHeaders.length);
            System.arraycopy(userdefined, 0, temp, standardFilterColumnHeaders.length, userdefined.length);
            Arrays.sort(temp);
            filterColumnHeaders=temp;
        } else filterColumnHeaders=standardFilterColumnHeaders;
    }
        
    @SuppressWarnings("unchecked")
    private void setupManualEntryPanel(boolean allowCreateCollection) { 
        setupFilterColumns();
        columnHeaders=new String[filterColumnHeaders.length+3]; // Include all property columns in the table model. The 3 extra columns are the "selection column" (first column), "Motif" (aka Presentation Name) and Logo
        columnHeaders[0]="";
        columnHeaders[1]="Motif"; // this will always contain a reference to the Motif data object
        columnHeaders[columnHeaders.length-1]="Logo";
        for (int i=0;i<filterColumnHeaders.length;i++) {columnHeaders[i+2]=filterColumnHeaders[i];} // fill in the rest of the property names      
        ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
        model=new ManualSelectionTableModel(motifs,columnHeaders);
        columnModel = new DefaultTableColumnModel();
        TableColumn col = new TableColumn(0);
        col.setHeaderValue(model.getColumnName(0));
        col.setMinWidth(22);
        col.setMaxWidth(22);
        columnModel.addColumn(col);        
//        for (int i=1;i<columnHeaders.length;i++) { // add the rest of the columns
//            TableColumn col = new TableColumn(i);
//            col.setHeaderValue(model.getColumnName(i));
//            columnModel.addColumn(col);
//        }
        manualSelectionTable=new JTable(model,columnModel);
        manualSelectionTable.setAutoCreateRowSorter(true);
        manualSelectionTable.getTableHeader().setReorderingAllowed(true);

        JScrollPane manualSelectionScrollPane=new JScrollPane(manualSelectionTable);
        this.setLayout(new BorderLayout());
        this.add(manualSelectionScrollPane,BorderLayout.CENTER);
        
        JPanel filterPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        final JComboBox filterCombobox=new JComboBox(filterColumnHeaders);
        filterTextfield=new JTextField(12);                  
        filterOperator = new JComboBox(new String[]{"=","<>","<","<=",">=",">"});   
        numberOfMotifsShowingLabel = new JLabel();
        if (this.motifcollection!=null) {
            showOnlyCollectionMembersCheckbox=new JCheckBox();  
            showOnlyCollectionMembersCheckbox.setToolTipText("Show only motifs included in the collection");
            filterPanel.add(showOnlyCollectionMembersCheckbox);
        }
        filterPanel.add(new JLabel("Filter "));        
        filterPanel.add(filterCombobox);
        filterPanel.add(filterOperator);
        filterPanel.add(filterTextfield);
        filterPanel.add(numberOfMotifsShowingLabel);
        // -- testing stuff --
        //filterBoxesPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        filterBoxesPanel=new JPanel();
        filterBoxesPanel.setLayout(new BoxLayout(filterBoxesPanel, BoxLayout.X_AXIS));          
        JPanel controlsPanel=new JPanel(new BorderLayout());     
        controlsPanel.add(filterBoxesPanel,BorderLayout.NORTH);
        controlsPanel.add(filterPanel,BorderLayout.SOUTH);
        // 
        //this.add(filterPanel,BorderLayout.SOUTH);
        this.add(controlsPanel,BorderLayout.SOUTH);
       
        final VisualizationSettings settings=gui.getVisualizationSettings();
        Color [] basecolors=new Color[]{settings.getBaseColor('A'),settings.getBaseColor('C'),settings.getBaseColor('G'),settings.getBaseColor('T')};   
        int rowheight=manualSelectionTable.getRowHeight();
        logorenderer=new MotifLogo(basecolors,(int)(rowheight*1.25));
        logorenderer.setUseAntialias(gui.getVisualizationSettings().useMotifAntialiasing());
        
        manualSelectionTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE && motifcollection!=null) {
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
                } else if ((e.getKeyCode()==KeyEvent.VK_SPACE && motifcollection==null) || e.getKeyCode()==KeyEvent.VK_V) {
                    int[] rows=manualSelectionTable.getSelectedRows();
                    int visibleRows=0;
                    for (int row:rows) {
                        Motif motif=(Motif)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MOTIF_COLUMN);
                        if (settings.isRegionTypeVisible(motif.getName())) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    for (int row:rows) {
                        Motif motif=(Motif)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MOTIF_COLUMN);
                        settings.setRegionTypeVisible(motif.getName(),doShow,false);                        
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown()) {
                    int[] rows=manualSelectionTable.getSelectedRows();
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    for (int row:rows) {
                        Motif motif=(Motif)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MOTIF_COLUMN);
                        settings.setRegionTypeVisible(motif.getName(),doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown()) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;                    
                    setVisibilityOnAllMotifs(doShow);
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_O) {
                    setVisibilityOnAllMotifs(false);
                    int[] rows=manualSelectionTable.getSelectedRows();
                    for (int row:rows) {
                        Motif motif=(Motif)model.getValueAt(manualSelectionTable.convertRowIndexToModel(row),MOTIF_COLUMN);
                        settings.setRegionTypeVisible(motif.getName(),true,false);
                    }
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_PLUS || e.getKeyCode()==KeyEvent.VK_ADD) {
                    int newheight=manualSelectionTable.getRowHeight()+1;
                    if (newheight>80) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    manualSelectionTable.setRowHeight(newheight);
                } else if (e.getKeyCode()==KeyEvent.VK_MINUS || e.getKeyCode()==KeyEvent.VK_SUBTRACT) {
                    int newheight=manualSelectionTable.getRowHeight()-1;
                    if (newheight<8) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    manualSelectionTable.setRowHeight(newheight); 
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder motifstring=new StringBuilder();
                     int[] rows=manualSelectionTable.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';
                     boolean first=true;
                     for (int row:rows) {
                         if (first) first=false; else motifstring.append(separator);
                         int modelrow=manualSelectionTable.convertRowIndexToModel(row);
                         String name=((Motif)model.getValueAt(modelrow, 1)).getName();
                         motifstring.append(name);
                     }
                     String motifs=motifstring.toString();
                     gui.logMessage(motifs);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(motifs);
                     java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                     clipboard.setContents(stringSelection, null);
                 } else if (e.getKeyCode()==KeyEvent.VK_L) {
                    logorenderer.setScaleByIC(!logorenderer.getScaleByIC());
                    manualSelectionTable.repaint();
                 } 
            }
        });
        // check if the supplied motifcollection has motifs that are not registered with the engine
        if (motifcollection!=null) {
              for (String motifname:motifcollection.getAllMotifNames()) {
                  if (engine.dataExists(motifname, Motif.class)) continue;
                  Motif dummy=new Motif(motifname);
                  dummy.setMatrix(new double[][]{{0.25f,0.25f,0.25f,0.25f}});
                  model.addMotif(dummy);
              }
        }   
        
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
        
        manualSelectionContextMenu=new ManualSelectionContextMenu(motifcollection!=null,allowCreateCollection);
        manualSelectionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount()==2) {
                    int column = manualSelectionTable.columnAtPoint(evt.getPoint()); //
                    if (column==0) return; // clicked the checkbox                    
                    int modelrow=manualSelectionTable.convertRowIndexToModel(manualSelectionTable.getSelectedRow());
                    Motif motif=(Motif)model.getValueAt(modelrow, 1);
                    if (motif!=null) {
                        gui.showPrompt(motif, false, isModal);                    
                    }
                }                  
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    int row = manualSelectionTable.rowAtPoint(evt.getPoint()); //
                    if (row>=0 && !manualSelectionTable.isRowSelected(row)) manualSelectionTable.getSelectionModel().setSelectionInterval(row, row);                                          
                    if (manualSelectionTable.getSelectedRowCount()>0) showContextMenu(evt);
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                updateCountLabelText();
                if (evt.isPopupTrigger()) {
                    int row = manualSelectionTable.rowAtPoint(evt.getPoint()); //
                    if (row>=0 && !manualSelectionTable.isRowSelected(row)) manualSelectionTable.getSelectionModel().setSelectionInterval(row, row);
                    if (manualSelectionTable.getSelectedRowCount()>0) showContextMenu(evt);
                }
            }
        });
        if (motifs.size()>0) { // set selected on motifs in collection
            manualSelectionTable.getRowSorter().toggleSortOrder(MOTIF_COLUMN);
            if (motifcollection!=null) {
               for (int i=0;i<model.getRowCount();i++) {
                   Motif motif=(Motif)model.getValueAt(i, 1);
                   model.setValueAt(motifcollection.contains(motif), i, 0);
               }
            manualSelectionTable.getRowSorter().toggleSortOrder(SELECTED_COLUMN); // show checked first
            manualSelectionTable.getRowSorter().toggleSortOrder(SELECTED_COLUMN); // show checked first - must sort twice to get checked on top               
            }
        }             
        filterCombobox.setSelectedItem(initialFilterChoice);
        if (showOnlyCollectionMembersCheckbox!=null) {
            boolean defaultValue=(motifcollection.isEmpty())?false:showOnlyMembersDefault;
            if (defaultValue==true) showOnlyCollectionMembersCheckbox.doClick(); // the doClick is necessary to update the filter, setSelected() is not enough       
        }         
        updateCountLabelText();
         
        manualSelectionTable.getColumnModel().addColumnModelListener(new TableColumnWidthListener());
        manualSelectionTable.getTableHeader().addMouseListener(new TableHeaderMouseListener());                
        setupFilterBoxes();
    } // end setupManualEntryPanel()

    
    private void addPropertyColumnToTable(String propertyName) { // adds a column to the table (
        int row=0;
        manualSelectionTable.convertColumnIndexToModel(row);
        int columnIndex=0;        
        TableColumn column = new TableColumn(columnIndex);
        column.setHeaderValue(model.getColumnName(columnIndex));
        columnModel.addColumn(column);
        if (propertyName.equals("Motif")) column.setCellRenderer(new CellRenderer_Motif());
        else if (propertyName.equals("Logo")) column.setCellRenderer(logorenderer);
        else column.setCellRenderer(new CellRenderer_FilterColumn());
                
        Comparator comparator=(propertyName.equals("Logo"))?new MotifLogoComparator():new Sorter();
        ((TableRowSorter)manualSelectionTable.getRowSorter()).setComparator(columnIndex,comparator);
    }
    
    public JTable getTable() {
        return manualSelectionTable;
    }
    
    /** Updates the current Motif Collection (if set) to reflect the new selection */    
    public void updateMotifCollection() {
        if (motifcollection==null) return;
        // first check if the current collection is different 
        int collectionSize=motifcollection.size();
        boolean containsAllChecked=true;
        int checkcount=0;
        for (int i=0;i<model.getRowCount();i++) {
            if ((Boolean)model.getValueAt(i,0)) {
                checkcount++;
                Motif motif=(Motif)model.getValueAt(i, 1);
                if (!motifcollection.contains((Motif)motif)) {containsAllChecked=false;break;}
            }
        }         
        if (containsAllChecked && collectionSize==checkcount) return; // collection has not been updated
        motifcollection.clearAll(engine); // this will empty the collection and also clear previous 'associations' the collection might have
        for (int i=0;i<model.getRowCount();i++) {
            if ((Boolean)model.getValueAt(i,0)) {
                Motif motif=(Motif)model.getValueAt(i, 1);
                motifcollection.addMotif((Motif)motif);
            }
        }      
    }

    @Override
    public void dataAdded(Data data) {
        if (!(data instanceof Motif)) return;
        model.addMotif((Motif)data);
    }

    @Override
    public void dataRemoved(Data data) {
        if (!(data instanceof Motif)) return;
        model.removeMotif((Motif)data); 
    }

    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {}
    @Override
    public void dataUpdated(Data data) {
         if (!(data instanceof Motif)) return;
         model.fireTableDataChanged();
    }

    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {}

    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) { }

    /** Shows or hides all motifs depending on the argument, but does not update visualization */
    public void setVisibilityOnAllMotifs(boolean visible) {
         VisualizationSettings settings=gui.getVisualizationSettings();
         for (int i=0;i<model.getRowCount();i++) {
             settings.setRegionTypeVisible(((Motif)model.getValueAt(i,1)).getName(), visible, false);
         }
    }

    /** Returns the number of motifs currently selected under Manual Entry tab*/
    private int countSelectedMotifs() {
         int selected=0;
         for (int i=0;i<model.getRowCount();i++) {
             if ((Boolean)model.getValueAt(i,0)) selected++;
         }           
         return selected;
    }
    /** Updates the label displaying counts (shown, selected and total) in the manual selection tab*/
    private void updateCountLabelText() {
        int total=model.getRowCount();
        if (motifcollection!=null) numberOfMotifsShowingLabel.setText("<html>Matching: "+manualSelectionTable.getRowCount()+" of "+total+"<br>Included: "+countSelectedMotifs()+" of "+total+"</html>");
        else numberOfMotifsShowingLabel.setText("<html>Matching: "+manualSelectionTable.getRowCount()+" of "+total+"</html>");
    }


     private void showContextMenu(MouseEvent evt) {
        int selectedCount=manualSelectionTable.getSelectedRowCount();
        int firstSelected=manualSelectionTable.getSelectedRow();
        if (firstSelected>=0 && selectedCount==1) {
            int modelrow=manualSelectionTable.convertRowIndexToModel(firstSelected);
            manualSelectionContextMenu.setSingleMotifSelected(((Motif)model.getValueAt(modelrow,1)).getName());
        } else {          
            manualSelectionContextMenu.setSingleMotifSelected(null);
        }
        manualSelectionContextMenu.updateMenu();
        manualSelectionContextMenu.show(evt.getComponent(), evt.getX(),evt.getY());     
     }

     /**
      *  This method compares the given matrix to the current filter-string (which should be a DNA oligo)
      *  and returns a score value reflecting the similarity of the matrix to the score.
      *  All possible alignments in both orientations are tried, and the best score found is returned.
      */
     private double getPWMSimilarityToFilter(Motif motif) {
         String dnasequence=filterTextfield.getText().trim();
         if (dnasequence.isEmpty()) return 0;
         if (!oldPWMtarget.equals(dnasequence)) {
             PWMsimilarityCache.clear(); // the cache is made invalid since the current filterstring is different from the one used by the cache
             oldPWMtarget=dnasequence;
         }
         String motifname=motif.getName();
         Double cached=PWMsimilarityCache.get(motifname);
         if (cached!=null) return cached.doubleValue();
         double scorevalue=0;
         double[][] matrix=motif.getMatrixAsFrequencyMatrix();
         int orientation=0; // 0=use both orientations. 1=only DIRECT, -1=only REVERSE
              if (dnasequence.startsWith("+")) {dnasequence=dnasequence.substring(1);orientation=1;}
         else if (dnasequence.startsWith("-")) {dnasequence=dnasequence.substring(1);orientation=-1;}
         if (dnasequence.length()>=motif.getLength()) {
             double directbest=0;
             double reversebest=0;
             if (orientation>=0) directbest=scorePWMstringAgainstDNA(dnasequence, matrix);
             if (orientation<=0) reversebest=scorePWMstringAgainstDNA(MotifLabEngine.reverseSequence(dnasequence), matrix);
             scorevalue=Math.max(directbest, reversebest);
         } else {
             double directbest=0;
             double reversebest=0;
             if (orientation>=0) directbest=scoreDNAstringAgainstPWM(dnasequence, matrix);
             if (orientation<=0) reversebest=scoreDNAstringAgainstPWM(MotifLabEngine.reverseSequence(dnasequence), matrix);
             scorevalue=Math.max(directbest, reversebest);
         }
         PWMsimilarityCache.put(motifname,scorevalue);
         return scorevalue;
     }
    
private class ManualSelectionTableModel extends AbstractTableModel { 
    private ArrayList<Boolean> checked=null;
    private ArrayList<Data> motifs=null;
    private String[] columnNames=null;
    private String filterColumn="Factors";
    
    public ManualSelectionTableModel(ArrayList<Data> motifs, String[] columnNames) {
        this.motifs=motifs;
        checked=new ArrayList<Boolean>(motifs.size());
        for (int i=0;i<motifs.size();i++) checked.add(Boolean.FALSE);
        this.columnNames=columnNames;
    }
    
    @Override
    public Class getColumnClass(int c) {
        if (c==SELECTED_COLUMN) return Boolean.class; // first column
        else if (c==MOTIF_COLUMN || c==LOGO_COLUMN) return Motif.class; // 
        else { // c==FILTER_COLUMN
            return filterPropertyClass.get(filterColumn);
        }
    }
    
    @Override    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return (columnIndex==0 && motifcollection!=null);
    }
    
    private String getFilterColumn() {return filterColumn;}
    private void setFilterColumn(String filterColumn) {this.filterColumn=filterColumn;}
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        checked.set(rowIndex,(Boolean)aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void addMotif(Motif newmotif) {
        int index=motifs.size();
        motifs.add(newmotif);
        checked.add(Boolean.FALSE);
        try {
           fireTableRowsInserted(index, index);
        } catch (ArrayIndexOutOfBoundsException e) {}        
    }

    public void removeMotif(Motif motif) {
        int index=motifs.indexOf(motif);
        if (index>=0) {
            motifs.remove(index);
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
        return motifs.size();
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex==SELECTED_COLUMN) return checked.get(rowIndex);
        else if (columnIndex==MOTIF_COLUMN || columnIndex==LOGO_COLUMN) return (Motif)motifs.get(rowIndex);
        else { // value from filter column
                 if (filterColumn.equals("ID")) return ((Motif)motifs.get(rowIndex)).getName();
            else if (filterColumn.equals("Classification")) return ((Motif)motifs.get(rowIndex)).getClassification();
            else if (filterColumn.equals("Consensus")) return ((Motif)motifs.get(rowIndex)).getConsensusMotif();
            else if (filterColumn.equals("Description")) return ((Motif)motifs.get(rowIndex)).getDescription();
            else if (filterColumn.equals("Expression")) return ((Motif)motifs.get(rowIndex)).getTissueExpressionAsString();
            else if (filterColumn.equals("Factors")) return ((Motif)motifs.get(rowIndex)).getBindingFactors();
            else if (filterColumn.equals("GC-content")) return ((Motif)motifs.get(rowIndex)).getGCcontent();
            else if (filterColumn.equals("Information Content")) return ((Motif)motifs.get(rowIndex)).getICcontent();
            else if (filterColumn.equals("Names")) return getMotifDisplayName((Motif)motifs.get(rowIndex));
            else if (filterColumn.equals("Names + Factors")) return getMotifNameAndFactors((Motif)motifs.get(rowIndex)); 
            else if (filterColumn.equals("Short Name")) return ((Motif)motifs.get(rowIndex)).getShortName();
            else if (filterColumn.equals("Long Name")) return ((Motif)motifs.get(rowIndex)).getLongName();           
            else if (filterColumn.equals("Organisms")) return ((Motif)motifs.get(rowIndex)).getOrganisms();
            else if (filterColumn.equals("Part")) return ((Motif)motifs.get(rowIndex)).getPart();
            else if (filterColumn.equals("PWM match")) return getPWMSimilarityToFilter((Motif)motifs.get(rowIndex));
            else if (filterColumn.equals("Quality")) return ((Motif)motifs.get(rowIndex)).getQuality();
            else if (filterColumn.equals("Size")) return ((Motif)motifs.get(rowIndex)).getLength();
            else return ((Motif)motifs.get(rowIndex)).getUserDefinedPropertyValueAsType(filterColumn,filterPropertyClass.get(filterColumn)); // user-defined property             
       }
    }

    @Override
    public String getColumnName(int column) {
        if (column==FILTER_COLUMN) return filterColumn;
        else return columnNames[column];
    }

    private String getMotifDisplayName(Motif motif) {
        String shortName=motif.getShortName();
        String longName=motif.getLongName();
        if (shortName!=null && !shortName.isEmpty() && longName!=null && !longName.isEmpty()) return shortName+", "+longName;
        else if (shortName!=null && !shortName.isEmpty() && (longName==null || longName.isEmpty())) return shortName;
        else if (longName!=null && !longName.isEmpty() && (shortName==null || shortName.isEmpty())) return longName;
        else return "";
    }
    
    private String getMotifNameAndFactors(Motif motif) {
        String shortName=motif.getShortName();
        String longName=motif.getLongName();
        String factors=motif.getBindingFactors();
        if (factors==null) factors=""; 
        String string;
        if (shortName!=null && !shortName.isEmpty() && longName!=null && !longName.isEmpty()) string=shortName+", "+longName;
        else if (shortName!=null && !shortName.isEmpty() && (longName==null || longName.isEmpty())) string=shortName;
        else if (longName!=null && !longName.isEmpty() && (shortName==null || shortName.isEmpty())) string=longName;
        else string="";
        if (!factors.isEmpty()) {
            if (string.isEmpty()) string=factors;
            else string+=", "+factors;
        }
        return string;
    }    
    
}    
        
   
private class CellRenderer_RightAlign extends DefaultTableCellRenderer {
    public CellRenderer_RightAlign() {
           super();
           this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);              
    }
    @Override
    public void setValue(Object value) {
           if (value!=null)setText(value.toString());
           else setText("");
       }
}// end class CellRenderer_RightAlign    

private class CellRenderer_Motif extends DefaultTableCellRenderer {
    public CellRenderer_Motif() {
           super();
           this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);              
    }
    @Override
    public void setValue(Object value) {
           if (value!=null && value instanceof Motif) {
               setText(((Motif)value).getPresentationName());
           }
           else setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        Motif motif=(Motif)table.getValueAt(row, MOTIF_COLUMN);
        if (motifcollection==null) { // if used as MotifBrowser (and not to create a motif collection)
           if (!gui.getVisualizationSettings().isRegionTypeVisible(motif.getName())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        return c;
    }
}// end class CellRenderer_Motif 




private class CellRenderer_FilterColumn extends DefaultTableCellRenderer {
    private NumberFormat percentageFormatter;
    public CellRenderer_FilterColumn() {
           super();
           percentageFormatter=NumberFormat.getPercentInstance();
    }
    @Override
    public void setValue(Object value) {
           String filterColumn=model.getFilterColumn();
           Class classtype=filterPropertyClass.get(filterColumn);
           if (Number.class.isAssignableFrom(classtype)) this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
           else this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
           if (filterColumn.equals("Classification")) {
               if (value==null) {setText("Unknown");setToolTipText(null);}
               else {
                  String name=MotifClassification.getNameForClass((String)value);
                  String fullPath=MotifClassification.getFullLevelsString((String)value);
                  fullPath=fullPath.replaceAll("\t", ":");
                  fullPath=fullPath.replaceAll("\n", " > ");
                  setText(((String)value)+" : "+name+"  ("+fullPath+")");
                  setToolTipText(MotifClassification.getFullLevelsStringAsHTML((String)value));
               }
           } else if (filterColumn.equals("GC-content")) {
               setText(percentageFormatter.format(((Double)value).doubleValue()));
           } else if (value!=null) {
               String text=value.toString();
               setText(text);
               String tooltiptext="";
               if (filterColumn.equals("Description")) tooltiptext=MotifLabGUI.formatTooltipString(text, 100);
               else tooltiptext="<html>"+text.replaceAll("\\s*,\\s*", "<br>")+"</html>"; // Note: Sometimes the value can contain commas that are not meant as list-separators, in which case it will be somewhat weird to split on comma (even more so if the elements are sorted alphabetically afterwards).  
               setToolTipText(tooltiptext);            
           } else {
             setText("");
             setToolTipText(null); 
           }                 
       }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        Motif motif=(Motif)table.getValueAt(row, MOTIF_COLUMN);
        if (motifcollection==null) { // if used as MotifBrowser (and not to create a motif collection)
           if (!gui.getVisualizationSettings().isRegionTypeVisible(motif.getName())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        return c;
    }

}// end class CellRenderer_FilterColumn    


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
     if (Number.class.isAssignableFrom(classtype)) {
         if (filterColumn.equals("PWM match")) return true; // always show all motifs for PWM match
         try {
            double numeric=0;
            if (value instanceof Integer) numeric=(double)((Integer)value).intValue();
            else numeric=((Double)value).doubleValue();
            if (filterColumn.equals("GC-content")) numeric=Math.round(numeric*100); // using percentages for GC-content
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
     } else if (filterColumn.equals("Classification")) { 
         boolean showContains=(comparator.equals("=") || comparator.equals("<=") || comparator.equals(">="));
         String classification;
         if (value==null) classification="unknown";
         else {
             classification=MotifClassification.getFullLevelsString(value.toString()).toLowerCase();
             classification=classification.replaceAll("\n", " ");
             classification=classification.replaceAll("\t", " ");
             classification=value.toString()+" "+classification;
         }
         boolean contains=advancedStringContains(classification);
         return ((showContains && contains) || (!showContains && !contains));  
         
     } else if (filterColumn.equals("Consensus")) {
         boolean convertIUPAC=false;
         boolean showContains=!(comparator.equals("<>"));
         boolean sameOrMore=false; 
         if (comparator.equals(">") || comparator.equals(">=")) {
             sameOrMore=true; convertIUPAC=true;
         } else if (comparator.equals("=") || comparator.equals("<") || comparator.equals("<=")) {
             sameOrMore=false; convertIUPAC=true;
         }        
         if (value==null) return !showContains;
         String consensusString=value.toString().toLowerCase();
         boolean contains=false;         
         String[] ors=filterString.split("\\s*(,|\\|)\\s*"); // match either of these
         for (String orclause:ors) {
             String[] ands=orclause.split("\\s*&\\s*"); // match all of these
             int matches=0;
             for (String filterStringCopy:ands) {
                 if (filterStringCopy.isEmpty()) {contains=true;break;}
                 boolean containsPart=false;
                 boolean anchorStart=false;
                 boolean anchorEnd=false;
                 int compareOrientation=0; // 0=both orientations, 1=only direct, -1=only reverse
                 if (filterStringCopy.startsWith("+")) {filterStringCopy=filterStringCopy.substring(1);compareOrientation=1;}
                 else if (filterStringCopy.startsWith("-")) {filterStringCopy=filterStringCopy.substring(1);compareOrientation=-1;}
                 if (filterStringCopy.startsWith("_") || filterStringCopy.startsWith("^")) {filterStringCopy=filterStringCopy.substring(1);anchorStart=true;}
                 if (filterStringCopy.endsWith("_") || filterStringCopy.endsWith("$")) {filterStringCopy=filterStringCopy.substring(0,filterStringCopy.length()-1);anchorEnd=true;}
                 String filterStringCopyReverse=reverseComplementIUPACString(filterStringCopy); // reverse the other strings in stead. So we can use regex?
                 if (!anchorStart) {
                     filterStringCopy=".*"+filterStringCopy;
                     filterStringCopyReverse=filterStringCopyReverse+".*";
                 }
                 if (!anchorEnd) {
                     filterStringCopy=filterStringCopy+".*";
                     filterStringCopyReverse=".*"+filterStringCopyReverse;
                 }
                 if (convertIUPAC) {
                     filterStringCopy=convertIUPACStringtoRegex(filterStringCopy,sameOrMore);
                     filterStringCopyReverse=convertIUPACStringtoRegex(filterStringCopyReverse,sameOrMore);
                 }
                 try {
                     //System.err.println("["+compareOrientation+"] compare '"+filterStringCopy+"' to "+value.toString());
                          if (compareOrientation==0) containsPart=(consensusString.matches(filterStringCopy) || (consensusString.matches(filterStringCopyReverse)));
                     else if (compareOrientation>0)  containsPart=consensusString.matches(filterStringCopy);
                     else if (compareOrientation<0)  containsPart=consensusString.matches(filterStringCopyReverse);                      
                 } catch (Exception e){} 
                 if (containsPart) matches++;
             } // end for each AND-clause
             if (matches==ands.length) {contains=true;break;}
         } // end for each OR-clause
         return ((showContains && contains) || (!showContains && !contains));    
         
     } else if (filterColumn.equals("Names")) { // Name
         boolean showContains=(comparator.equals("=") || comparator.equals("<=") || comparator.equals(">="));
         if (value==null) return !showContains;
         Motif motif=(Motif)model.getValueAt(row,1);
         String motifname=(motif.getShortName()+", "+motif.getLongName()).toLowerCase();
         boolean contains=advancedStringContains(motifname);
         return ((showContains && contains) || (!showContains && !contains));
         
     } else { // other textual properties
         boolean showContains=(comparator.equals("=") || comparator.equals("<=") || comparator.equals(">="));
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
       if (structuredStringFilter==null) return (filterString==null)?true:value.contains(filterString);
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
    
    private String convertIUPACStringtoRegex(String filterString,boolean sameOrMore) {
        String result="";
        for (int i=0;i<filterString.length();i++) {
            result+=(sameOrMore)?convertIUPACLettertoRegexMatchSameorMore(filterString.charAt(i)):convertIUPACLettertoRegexMatchSameorLess(filterString.charAt(i));
        }
        return result;
    }  

    private String reverseComplementIUPACString(String string) {
        String result="";
        for (int i=0;i<string.length();i++) {
            char c=string.charAt(i);
            result=reverseComplementIUPACletter(c)+result;
        }
        return result;
    }
    
    private char reverseComplementIUPACletter(char c) {
        switch (c) {
            case 'n':return 'n';
            case 'a':return 't';
            case 'c':return 'g';
            case 'g':return 'c';
            case 't':return 'a';
            case 'r':return 'y';
            case 'y':return 'r';
            case 'm':return 'k';
            case 'k':return 'm';
            case 'w':return 'w';
            case 's':return 's';
            case 'b':return 'v';
            case 'd':return 'h';
            case 'h':return 'd';
            case 'v':return 'b';
            default: return c;
        }
    } 
     
    private String convertIUPACLettertoRegexMatchSameorMore(char c) {
        switch (c) {
            case 'n':return ".";
            case 'a':return "[armwdhvn]";
            case 'c':return "[cymsbhvn]";
            case 'g':return "[grksbdvn]";
            case 't':return "[tykwbdhn]";
            case 'r':return "[ragdvn]";
            case 'y':return "[yctbhn]";
            case 'm':return "[machvn]";
            case 'k':return "[kgtbdn]";
            case 'w':return "[watdhn]";
            case 's':return "[scgbvn]";
            case 'b':return "[bcgtskyn]";
            case 'd':return "[dagtrkwn]";
            case 'h':return "[hactmywn]";
            case 'v':return "[vacgmrsn]";
            default: return ""+c;
        }
    }  

    private String convertIUPACLettertoRegexMatchSameorLess(char c) {
        switch (c) {
            case 'n':return ".";
            case 'a':return "a";
            case 'c':return "c";
            case 'g':return "g";
            case 't':return "t";
            case 'r':return "[rag]";
            case 'y':return "[yct]";
            case 'm':return "[mac]";
            case 'k':return "[kgt]";
            case 'w':return "[wat]";
            case 's':return "[scg]";
            case 'b':return "[bcgtsky]";
            case 'd':return "[dagtrkw]";
            case 'h':return "[hactmyw]";
            case 'v':return "[vacgmrs]";
            default: return ""+c;
        }

    }     
    
} // end class Filter


/** This private class implements the context menu */
private class ManualSelectionContextMenu extends JPopupMenu implements ActionListener {
     private final String DISPLAY_MOTIF="Display";
     private final String SHOW="Show";
     private final String HIDE="Hide";
     private final String SHOW_ONLY_SELECTED="Show Only Selected";   
     private final String SHOW_ALL="Show All";  
     private final String HIDE_ALL="Hide All";      
     private final String SELECT_SHOWN_MOTIFS="Select Shown Motifs";
     private final String SELECT_ONLY_SHOWN_MOTIFS="Select Only Shown Motifs";
     private final String SELECT_MOTIFS_FROM="Select Motifs From";
     private final String SELECT_ONLY_MOTIFS_FROM="Select Only Motifs From";
     private final String CREATE_MOTIF_COLLECTION="Create Motif Collection";   
     private final String SAVE_MOTIF_LOGO="Save Motif Logo";
     private final String COLOR_SUBMENU_HEADER="Set Color"; 
     
     JMenuItem includeSelectedItem=new JMenuItem("Include Selected Motifs"); 
     JMenuItem excludeSelectedItem=new JMenuItem("Exclude Selected Motifs");
     JMenuItem includeListedItem=new JMenuItem("Include Matching Motifs");
     JMenuItem includeNotListedItem=new JMenuItem("Include Not Matching Motifs");
     JMenuItem excludeListedItem=new JMenuItem("Exclude Matching Motifs");
     JMenuItem excludeNotListedItem=new JMenuItem("Exclude Not Matching Motifs");
     JMenuItem includeAllItem=new JMenuItem("Include All Motifs");
     JMenuItem excludeAllItem=new JMenuItem("Exclude All Motifs");
     JMenuItem invertCollectionItem=new JMenuItem("Invert Collection");
     JMenuItem displayItem=new JMenuItem(DISPLAY_MOTIF);
     JMenuItem compareMotifToOthers=new JMenuItem("Compare"); // this text will be changed dynamically
     JMenuItem saveMotifLogoItem=new JMenuItem(SAVE_MOTIF_LOGO);
     JMenu selectMotifsFromMenu=new JMenu(SELECT_MOTIFS_FROM);
     JMenu selectOnlyMotifsFromMenu=new JMenu(SELECT_ONLY_MOTIFS_FROM);
     JMenu includeMotifsFromMenu=new JMenu("Include Motifs From");   
     ExternalDBLinkMenu dbmenu=null;   
     SelectFromCollectionListener selectFromCollectionListener=new SelectFromCollectionListener();
     ClearAndSelectFromCollectionListener clearAndselectFromCollectionListener=new ClearAndSelectFromCollectionListener();
     IncludeFromCollectionListener includeFromCollectionListener=new IncludeFromCollectionListener();
              
     
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
             includeMenu.add(includeMotifsFromMenu);
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
                    settings.setFeatureColor(((Motif)model.getValueAt(modelrow, 1)).getName(), color, false);
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
         JMenuItem selectShownMotifsItem=new JMenuItem(SELECT_SHOWN_MOTIFS);
         JMenuItem selectOnlyShownMotifsItem=new JMenuItem(SELECT_ONLY_SHOWN_MOTIFS);
         JMenuItem createCollectionItem=new JMenuItem(CREATE_MOTIF_COLLECTION);  
         // ----- testing 1-2-3 -------------
//         if (selectMotifsFromMenu.getItemCount()>MenuScroller.DEFAULT_MENU_SIZE) MenuScroller.setScrollerFor(selectMotifsFromMenu);
//         if (selectOnlyMotifsFromMenu.getItemCount()>MenuScroller.DEFAULT_MENU_SIZE) MenuScroller.setScrollerFor(selectOnlyMotifsFromMenu);
//         if (includeMotifsFromMenu.getItemCount()>MenuScroller.DEFAULT_MENU_SIZE) MenuScroller.setScrollerFor(includeMotifsFromMenu);   
         // ----- end of testing ------------
         updateMenu();
         
         displayItem.addActionListener(this);
         showItem.addActionListener(this);
         showOnlySelectedItem.addActionListener(this);
         showAllItem.addActionListener(this);
         hideItem.addActionListener(this);
         hideAllItem.addActionListener(this);     
         selectShownMotifsItem.addActionListener(this);     
         selectOnlyShownMotifsItem.addActionListener(this);
         if (allowCreateCollection) createCollectionItem.addActionListener(this);
         compareMotifToOthers.addActionListener(this);
         saveMotifLogoItem.addActionListener(this);
         //setColorMenu=new ColorMenu(COLOR_SUBMENU_HEADER, colormenulistener, MotifBrowserPanel.this);
         this.add(displayItem);         
         this.add(showItem);
         this.add(showOnlySelectedItem); 
         this.add(showAllItem);            
         this.add(hideItem);   
         this.add(hideAllItem);              
         this.add(new JMenuItem("* Color Menu Error *")); // this.add(setColorMenu);
         this.add(new JSeparator());
         this.add(selectShownMotifsItem);
         this.add(selectOnlyShownMotifsItem);
         this.add(selectMotifsFromMenu);
         this.add(selectOnlyMotifsFromMenu);
         this.add(new JSeparator());
         if (allowCreateCollection) this.add(createCollectionItem);        
         this.add(compareMotifToOthers); // NOTE that this item can be shown/hidden depending on how many motifs are selected
         this.add(saveMotifLogoItem); // NOTE that this item can be shown/hidden depending on how many motifs are selected
         this.add(dbmenu);
     }
              
    /**
     * Updates the menu to fit a specific selection
     * This should be called every time before the menu is shown
     */
    public final void updateMenu() {
        selectMotifsFromMenu.removeAll();
        selectOnlyMotifsFromMenu.removeAll();        
        includeMotifsFromMenu.removeAll();  
        for (String collectionName:engine.getNamesForAllDataItemsOfType(MotifCollection.class)) {
            JMenuItem subitem=new JMenuItem(collectionName);
            subitem.addActionListener(selectFromCollectionListener);
            selectMotifsFromMenu.add(subitem);        
            JMenuItem subitem2=new JMenuItem(collectionName);
            subitem2.addActionListener(clearAndselectFromCollectionListener);
            selectOnlyMotifsFromMenu.add(subitem2);
            JMenuItem subitem3=new JMenuItem(collectionName);
            subitem3.addActionListener(includeFromCollectionListener);
            includeMotifsFromMenu.add(subitem3);            
        }      
        for (String partitionName:engine.getNamesForAllDataItemsOfType(MotifPartition.class)) {
            Data data=engine.getDataItem(partitionName);
            if (data instanceof MotifPartition) {
                JMenu selectMotifsFromMenuCluster=new JMenu(data.getName());               
                JMenu selectOnlyMotifsFromMenuCluster=new JMenu(data.getName());    
                JMenu includeMotifsFromMenuCluster=new JMenu(data.getName());                  
                for (String cluster:((MotifPartition)data).getClusterNames()) {                    
                    JMenuItem subitem=new JMenuItem(cluster);
                    subitem.setActionCommand(partitionName+"."+cluster);
                    subitem.addActionListener(selectFromCollectionListener);
                    selectMotifsFromMenuCluster.add(subitem);
                    JMenuItem subitem2=new JMenuItem(cluster);
                    subitem2.setActionCommand(partitionName+"."+cluster);
                    subitem2.addActionListener(clearAndselectFromCollectionListener);
                    selectOnlyMotifsFromMenuCluster.add(subitem2);
                    JMenuItem subitem3=new JMenuItem(cluster);
                    subitem3.setActionCommand(partitionName+"."+cluster);
                    subitem3.addActionListener(includeFromCollectionListener);
                    includeMotifsFromMenuCluster.add(subitem3);                    
                }
                // ----- testing 1-2-3 -------------                
//                if (selectMotifsFromMenuCluster.getItemCount()>MenuScroller.DEFAULT_MENU_SIZE) MenuScroller.setScrollerFor(selectMotifsFromMenuCluster);
//                if (selectOnlyMotifsFromMenuCluster.getItemCount()>MenuScroller.DEFAULT_MENU_SIZE) MenuScroller.setScrollerFor(selectOnlyMotifsFromMenuCluster);
//                if (includeMotifsFromMenuCluster.getItemCount()>MenuScroller.DEFAULT_MENU_SIZE) MenuScroller.setScrollerFor(includeMotifsFromMenuCluster);   
                // ----- end of testing ------------
                selectMotifsFromMenu.add(selectMotifsFromMenuCluster);
                selectOnlyMotifsFromMenu.add(selectOnlyMotifsFromMenuCluster);
                includeMotifsFromMenu.add(includeMotifsFromMenuCluster);                 
            }
        }         
        selectMotifsFromMenu.setEnabled(selectMotifsFromMenu.getMenuComponentCount()>0);
        selectOnlyMotifsFromMenu.setEnabled(selectOnlyMotifsFromMenu.getMenuComponentCount()>0);
    }    
    
    /** This should be called when the context menu is invoked with only one selected motif
     */
    public void setSingleMotifSelected(String targetMotifName) {
        dbmenu.updateMenu(targetMotifName,true);
        if (targetMotifName==null) {
             if (compareMotifToOthers!=null) compareMotifToOthers.setVisible(false);
             if (saveMotifLogoItem!=null) saveMotifLogoItem.setVisible(false);
             displayItem.setVisible(false);
        } else {
            String command="Compare "+targetMotifName+" To Other Motifs";
            compareMotifToOthers.setText(command);
            compareMotifToOthers.setVisible(true);
            saveMotifLogoItem.setVisible(true);
            displayItem.setText(DISPLAY_MOTIF+" "+targetMotifName);
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
         } else if (e.getActionCommand().startsWith(DISPLAY_MOTIF)) {
                String[] selectedMotifNames=getSelectedMotifNames();
                Data motif=gui.getEngine().getDataItem(selectedMotifNames[0]);
                if (motif instanceof Motif) gui.showPrompt((Motif)motif, false, isModal);
         } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                setVisibilityOnAllMotifs(false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String motifname:getSelectedMotifNames()) {
                   settings.setRegionTypeVisible(motifname, true, false);   
                }
                gui.redraw();
         } else if (e.getActionCommand().equals(SHOW_ALL)) {
                setVisibilityOnAllMotifs(true);
                gui.redraw();                        
         } else if (e.getActionCommand().equals(HIDE_ALL)) {
                setVisibilityOnAllMotifs(false);
                gui.redraw(); 
         } else if (e.getActionCommand().equals(SHOW)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String motifname:getSelectedMotifNames()) {
                   settings.setRegionTypeVisible(motifname, true, false);   
                }
                gui.redraw();
         } else if (e.getActionCommand().equals(HIDE)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String motifname:getSelectedMotifNames()) {
                   settings.setRegionTypeVisible(motifname, false, false);   
                }
                gui.redraw();
         } else if (e.getActionCommand().equals(CREATE_MOTIF_COLLECTION)) {
                String[] selectedMotifNames=getSelectedMotifNames();
                ArrayList<String> list=new ArrayList<String>(selectedMotifNames.length);
                list.addAll(Arrays.asList(selectedMotifNames));
                gui.promptAndCreateMotifCollection(list);
         } else if (e.getActionCommand().equals(SELECT_SHOWN_MOTIFS)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allMotifs=engine.getNamesForAllDataItemsOfType(Motif.class);
                HashSet<String> shownMotifs=new HashSet<String>(allMotifs.size());
                for (String motifname:allMotifs) {
                   if (settings.isRegionTypeVisible(motifname)) shownMotifs.add(motifname);
                }
                selectRowsForMotifs(shownMotifs,false);
         } else if (e.getActionCommand().equals(SELECT_ONLY_SHOWN_MOTIFS)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allMotifs=engine.getNamesForAllDataItemsOfType(Motif.class);
                HashSet<String> shownMotifs=new HashSet<String>(allMotifs.size());
                for (String motifname:allMotifs) {
                   if (settings.isRegionTypeVisible(motifname)) shownMotifs.add(motifname);
                }
                selectRowsForMotifs(shownMotifs,true);
         } else if (cmd.startsWith("Compare")) {
            int firstrow=manualSelectionTable.getSelectedRow();
            if (firstrow>=0) {
                int modelrow=manualSelectionTable.convertRowIndexToModel(firstrow);
                Motif target=(Motif)model.getValueAt(modelrow, 1);
                MotifComparisonDialog motifcomparisonPanel=new MotifComparisonDialog(gui,target,isModal);
                int x=gui.getGUIFrame().getWidth()/2-motifcomparisonPanel.getWidth()/2; if (x<0) x=0;
                int y=gui.getGUIFrame().getHeight()/2-motifcomparisonPanel.getHeight()/2; if (y<0) y=0;
                motifcomparisonPanel.setLocation(x, y);                 
                motifcomparisonPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                motifcomparisonPanel.setVisible(true);
            }
         } else if (e.getActionCommand().equals(SAVE_MOTIF_LOGO))  {
            int firstrow=manualSelectionTable.getSelectedRow();
            if (firstrow>=0) {
                int modelrow=manualSelectionTable.convertRowIndexToModel(firstrow);
                Motif target=(Motif)model.getValueAt(modelrow, 1);             
                SaveMotifLogoImageDialog saveLogoPanel=new SaveMotifLogoImageDialog(gui, target, isModal);
                int x=gui.getGUIFrame().getWidth()/2-saveLogoPanel.getWidth()/2; if (x<0) x=0;
                int y=gui.getGUIFrame().getHeight()/2-saveLogoPanel.getHeight()/2; if (y<0) y=0;
                saveLogoPanel.setLocation(x, y);                 
                saveLogoPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                saveLogoPanel.setVisible(true);     
            }
         }
         manualSelectionTable.repaint();
         updateCountLabelText();
     }

     
         /** Returns the names of the motifs in currently selected rows of the tabel
     *  or NULL if no rows are currently selected
     */
    private String[] getSelectedMotifNames() {
        int[] selection=manualSelectionTable.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        String[] selectedMotifNames=new String[selection.length];
        for (int i=0;i<selection.length;i++) {
            Object motif=manualSelectionTable.getValueAt(selection[i], MOTIF_COLUMN);
            if (motif instanceof Motif) selectedMotifNames[i]=((Motif)motif).getName();   
            else return null;
        }        
        return selectedMotifNames;
    }
    
} // END private class ManualSelectionContextMenu


   private class SelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectMotifsFromCollection(collectionName,false,false);
       }
   }
   private class ClearAndSelectFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectMotifsFromCollection(collectionName,true,false);
       }
   }
   private class IncludeFromCollectionListener implements ActionListener {
       public void actionPerformed(ActionEvent e) {
           String collectionName=e.getActionCommand();
           selectMotifsFromCollection(collectionName,false,true);
       }
   }   

    private void selectMotifsFromCollection(String collectionName, boolean clearCurrentSelection, boolean include) {
           HashSet<String> motifnames=new HashSet<String>();
           int point=collectionName.indexOf('.');
           if (point>0) { // partition
               String clusterName=collectionName.substring(point+1);
               collectionName=collectionName.substring(0,point);
               Data col=engine.getDataItem(collectionName);
               if (col instanceof MotifPartition) motifnames.addAll( ((MotifPartition)col).getAllMotifNamesInCluster(clusterName) );
                                 
           } else { // collection
               Data col=engine.getDataItem(collectionName);
               if (col instanceof MotifCollection) motifnames.addAll( ((MotifCollection)col).getAllMotifNames());                         
           }
           if (include) includeRowsForMotifs(motifnames,clearCurrentSelection);          
           else selectRowsForMotifs(motifnames,clearCurrentSelection);          
    }

    private void selectRowsForMotifs(HashSet<String> motifs, boolean clearCurrentSelection) {
        if (clearCurrentSelection) manualSelectionTable.clearSelection();
        ListSelectionModel selection=manualSelectionTable.getSelectionModel();        
        int motifcolumn=MOTIF_COLUMN;
        for (int i=0;i<manualSelectionTable.getRowCount();i++) {
            Object value=manualSelectionTable.getValueAt(i, motifcolumn);      
            String motifname=((Motif)value).getName();
            if (motifs.contains(motifname)) {
                selection.addSelectionInterval(i,i); 
            }
        }
    }  
    
    private void includeRowsForMotifs(HashSet<String> motifs, boolean clearCurrentSelection) {       
        if (clearCurrentSelection) {
            for (int i=0;i<model.getRowCount();i++) {
                model.setValueAt(Boolean.FALSE, i, SELECTED_COLUMN);
            }
        }     
        for (int i=0;i<model.getRowCount();i++) {
            Object value=model.getValueAt(i, MOTIF_COLUMN);      
            String motifname=((Motif)value).getName();
            if (motifs.contains(motifname)) {
                model.setValueAt(Boolean.TRUE, i, SELECTED_COLUMN);
            }
        }
        updateCountLabelText();
    }     

    /** A class to sort motif logos based on the length of the motif */
    private class MotifLogoComparator implements java.util.Comparator<Motif> {
        @Override
        public int compare(Motif motif1, Motif motif2) {
            return motif1.getLength()-motif2.getLength();
        }  
    }       
    
private class FeatureColorCellRenderer extends DefaultTableCellRenderer {
    
    SimpleDataPanelIcon selectedicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
    VisualizationSettings settings;

    public FeatureColorCellRenderer(MotifLabGUI gui) {
         super();
         settings=gui.getVisualizationSettings();
         setIcon(selectedicon);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); // this is necessary in order to get correct alternating row rendering 
        Motif motif=(Motif)table.getValueAt(row, MOTIF_COLUMN);
        String motifname=motif.getName();
        selectedicon.setForegroundColor(settings.getFeatureColor(motifname));
        selectedicon.setBorderColor((settings.isRegionTypeVisible(motifname))?Color.BLACK:Color.LIGHT_GRAY);
        setIcon(selectedicon);
        setText(null);
        return this;
    }           
}    
    
    /** scores a short DNA string against a PWM and returns the best matching score. The string should be shorter than the PWM (else use scorePWMstringAgainstDNA() below). */
    private double scoreDNAstringAgainstPWM(String dnasequence, double[][] pwm) {
        double currentbest=0;
        for (int i=0;i<=pwm.length-dnasequence.length();i++) { // i is position in the PWM
           double positionscore=0;
           for (int j=0;j<dnasequence.length();j++) {  // j is position in the sequence
               char base=dnasequence.charAt(j);
               if (base=='A' || base=='a') positionscore+=pwm[i+j][0];
               else if (base=='C' || base=='c') positionscore+=pwm[i+j][1];
               else if (base=='G' || base=='g') positionscore+=pwm[i+j][2];
               else if (base=='T' || base=='t') positionscore+=pwm[i+j][3];
           }
           if (positionscore>currentbest) currentbest=positionscore;
        }
        return currentbest;
    }

    /** scores a PWM against a DNA string and returns the score for the best matching position. The length of the DNA sequence must be at least that of the PWM (else use scoreDNAstringAgainstPWM() above). */
    private double scorePWMstringAgainstDNA(String dnasequence, double[][] pwm) {
        double currentbest=0;
        for (int i=0;i<=dnasequence.length()-pwm.length;i++) { // i is position in the sequence
           double positionscore=0;
           for (int j=0;j<pwm.length;j++) {  // j is position in the PWM
               char base=dnasequence.charAt(i+j);
               if (base=='A' || base=='a') positionscore+=pwm[j][0];
               else if (base=='C' || base=='c') positionscore+=pwm[j][1];
               else if (base=='G' || base=='g') positionscore+=pwm[j][2];
               else if (base=='T' || base=='t') positionscore+=pwm[j][3];
           }
           if (positionscore>currentbest) currentbest=positionscore;
        }
        return currentbest;
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
    
    private class TableColumnWidthListener implements TableColumnModelListener {
        @Override
        public void columnMarginChanged(ChangeEvent e) {
             resizeFilterBoxes();
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) { }

        @Override
        public void columnAdded(TableColumnModelEvent e) { }

        @Override
        public void columnRemoved(TableColumnModelEvent e) { }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) { }
    }    
    
    private class TableHeaderMouseListener extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            gui.logMessage(e.toString());
        }
    }    
    
    private void setupFilterBoxes() {
        filterBoxesPanel.removeAll();
        filterBoxesPanel.add(Box.createRigidArea(new Dimension(22+4,0))); // Add some space first: 22 pixel in colorbox column + 8px border
        for (int i=0;i<manualSelectionTable.getColumnCount()-1;i++) { // no filter should be applied to the first column, so we lopp to "count-1"
             JTextField filterColumn=new JTextField(2);
             filterBoxesPanel.add(filterColumn);            
        }             
    }
    
    private void resizeFilterBoxes() {
         int[] colWidths=new int[manualSelectionTable.getColumnCount()];
         for (int i=0;i<manualSelectionTable.getColumnCount();i++) {
             String colName=manualSelectionTable.getColumnName(i);
             TableColumn col=manualSelectionTable.getColumn(colName);
             colWidths[i]=col.getWidth();
         }
         int j=1; //
         for (int i=0;i<filterBoxesPanel.getComponentCount();i++) {
             Component comp=filterBoxesPanel.getComponent(i);
             if (comp instanceof JTextField && colWidths.length>j) {
                 int bordersize=0;// 
                 Dimension dim=new Dimension(colWidths[j]-bordersize, 27); 
                 comp.setSize(dim);
                 comp.setPreferredSize(dim);
                 comp.setMinimumSize(dim);             
                 comp.setMaximumSize(dim);    
                 j++;
             }
         }         
         filterBoxesPanel.revalidate();
    }

}
