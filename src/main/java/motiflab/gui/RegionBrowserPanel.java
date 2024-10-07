
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
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
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
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Module;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.MotifPartition;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;

/**
 * This class implements a panel that will display a table of Regions
 * with 3 or 4 columns containing a region type, parent sequence, a selected (filter) property and a perhaps a motif/module logo if applicable.
 * The panel is used by RegionBrowser
 * @author kjetikl
 */
public class RegionBrowserPanel extends JPanel {    
    private RegionDataset regiontrack;
    private boolean isMotifTrack=false;
    private boolean isModuleTrack=false;
    private JTable regionTable;
    private DefaultTableColumnModel columnModel;
    private JTextField filterTextfield;
    private JComboBox filterOperator;
    private JLabel numberOfRegionsShowingLabel;
    private Filter filter;
    private JComboBox filterCombobox;
    private RegionTableModel model;    
    private RegionTableContextMenu regionTableContextMenu;
    private JScrollPane tableScrollPane;
    private MotifLogo motifLogorenderer;
    private ModuleLogo moduleLogorenderer;    
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private String initialFilterChoice="Score";
    private String[] columnHeadersWithLogo=new String[] {" ","Region","Sequence name","Location","Filter","Logo"}; // the name of the Filter column will be changed later on 
    private String[] columnHeadersWithoutLogo=new String[] {" ","Region","Sequence name","Location","Filter"}; //
    private String[] columnHeaders;
    private String[] standardFilterColumnHeaders=new String[]{"Chromosome","End","Genomic end","Genomic start","Length","Location","Orientation","Relative end","Relative start","Score","Sequence name","Sequence","Size","Start","TES[0]-relative end","TES[0]-relative start","TES[1]-relative end","TES[1]-relative start","TSS[0]-relative end","TSS[0]-relative start","TSS[1]-relative end","TSS[1]-relative start","Type"};
    private String[] filterColumnHeaders=new String[]{}; // this will be the standardFilterColumnHeaders + non-standard (user-defined) properties
    private HashMap<String,Class> filterPropertyClass=null;
    private MiscIcons markedSequenceIcon;    
    private RegionBrowserFilter regionBrowserFilter;
    private boolean manualFilterSelectionMode=false;
    private VisualizationSettings settings=null;
    private PropertyChangeEvent filterChangeEvent; 
    

    private static final int SELECTED_COLUMN=0;
    private static final int REGION_COLUMN=1;
    private static final int SEQUENCE_COLUMN=2;  
    private static final int LOCATION_COLUMN=3;      
    private static final int FILTER_COLUMN=4;    
    private static final int LOGO_COLUMN=5;

    private boolean isModal=false;

    public RegionBrowserPanel(MotifLabGUI gui, boolean modal, boolean allowCreateCollection) {        
        super();
        this.gui=gui;
        this.engine=gui.getEngine();
        this.isModal=modal;    
        settings=gui.getVisualizationSettings();
        markedSequenceIcon=new MiscIcons(MiscIcons.BULLET_MARK, Color.RED);
        setupComponents();
    }
    
    public void setRegionTrack(RegionDataset regiontrack) {
        this.regiontrack=regiontrack;
        isMotifTrack=(regiontrack!=null && regiontrack.isMotifTrack());
        isModuleTrack=(regiontrack!=null && regiontrack.isModuleTrack());  
        setupFilterColumns();  
        updateTable();        
        filter=new Filter();
        ((TableRowSorter)regionTable.getRowSorter()).setRowFilter(filter);        
        if (filterCombobox!=null) {
            filterCombobox.setModel(new DefaultComboBoxModel(filterColumnHeaders));
            filterCombobox.setSelectedItem(initialFilterChoice);            
        }     
        getRegionBrowserFilter().setTrack(regiontrack);
        filterChangeEvent=new PropertyChangeEvent(getRegionBrowserFilter(),"update",null,null);
        updateCountLabelText();
    }
    
    private void setupComponents() {
        this.setLayout(new BorderLayout());
        tableScrollPane=new JScrollPane();
        this.add(tableScrollPane,BorderLayout.CENTER);
        JPanel filterPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        filterPanel.add(new JLabel("Filter "));
        filterCombobox=new JComboBox(filterColumnHeaders);
        filterTextfield=new JTextField(12);                  
        filterOperator = new JComboBox(new String[]{"=","<>","<","<=",">=",">"});   
        numberOfRegionsShowingLabel = new JLabel();
        filterPanel.add(filterCombobox);
        filterPanel.add(filterOperator);
        filterPanel.add(filterTextfield);
        filterPanel.add(numberOfRegionsShowingLabel);
        this.add(filterPanel,BorderLayout.SOUTH);          
        filterCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { // replace filter column
                if (model==null) return;
                String newFilterColumn=(String)filterCombobox.getSelectedItem();
                model.setFilterColumn(newFilterColumn);
                columnModel.getColumn(FILTER_COLUMN).setHeaderValue(newFilterColumn);
                regionTable.getTableHeader().repaint();
                regionTable.repaint();
            }
        });
        filterTextfield.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (filter==null) return;
                filter.updateFilter();
                ((TableRowSorter)regionTable.getRowSorter()).sort();
                updateCountLabelText();
            }
        }); 
        filterTextfield.addKeyListener(new java.awt.event.KeyAdapter() { // note that this is a direct copy of the one above
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (filter==null) return;                
                filter.updateFilter();
                ((TableRowSorter)regionTable.getRowSorter()).sort();
                updateCountLabelText();
            }
        });        
        filterOperator.setSelectedIndex(0);             
        filterCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {   
                if (filter==null) return;                
                filter.updateFilter();
                ((TableRowSorter)regionTable.getRowSorter()).sort();
            }
        });  
        updateCountLabelText();             
    }
    
    
    
    private void setupFilterColumns() {
        HashMap<String,Class> userdefinedproperties=(regiontrack!=null)?regiontrack.getUserDefinedProperties(null):null;
        // remove "Region" type properties (i.e. nested regions)
        String regionClassName=Region.class.getSimpleName();
        for(Iterator<Map.Entry<String, Class>> it = userdefinedproperties.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry<String, Class> entry = it.next();
          if (entry.getValue().getSimpleName().equals(regionClassName)) {
             it.remove();
          }
        }
        if (isMotifTrack) {
            if (userdefinedproperties==null) userdefinedproperties=new HashMap<>();
            userdefinedproperties.put("Motif name",String.class);
        }
        filterPropertyClass=new HashMap<String,Class>(standardFilterColumnHeaders.length+((userdefinedproperties!=null)?userdefinedproperties.size():0));               
        filterPropertyClass.put("Chromosome",String.class);
        filterPropertyClass.put("End",Integer.class);
        filterPropertyClass.put("Genomic end",Integer.class);
        filterPropertyClass.put("Genomic start",Integer.class);
        filterPropertyClass.put("Length",Integer.class);
        filterPropertyClass.put("Location",String.class);        
        filterPropertyClass.put("Orientation",Integer.class);
        filterPropertyClass.put("Relative end",Integer.class);
        filterPropertyClass.put("Relative start",Integer.class);
        filterPropertyClass.put("Score",Double.class);
        filterPropertyClass.put("Sequence name",String.class);
        filterPropertyClass.put("Sequence",String.class);
        filterPropertyClass.put("Size",Integer.class);
        filterPropertyClass.put("Start",Integer.class);
        filterPropertyClass.put("TES[0]-relative end",Integer.class);
        filterPropertyClass.put("TES[0]-relative start",Integer.class);
        filterPropertyClass.put("TES[1]-relative end",Integer.class);
        filterPropertyClass.put("TES[1]-relative start",Integer.class);
        filterPropertyClass.put("TSS[0]-relative end",Integer.class);
        filterPropertyClass.put("TSS[0]-relative start",Integer.class);
        filterPropertyClass.put("TSS[1]-relative end",Integer.class);
        filterPropertyClass.put("TSS[1]-relative start",Integer.class);
        filterPropertyClass.put("Type",String.class);       
               
        if (userdefinedproperties!=null && !userdefinedproperties.isEmpty()) {
            for (String key:userdefinedproperties.keySet()) {
                Class propclass=userdefinedproperties.get(key);               
                if (propclass.equals(ArrayList.class)) filterPropertyClass.put(key,String.class); // this will 'magically' convert lists to strings 
                else if (propclass.equals(Number.class)) filterPropertyClass.put(key,Double.class); // treat 'Numbers' as 'Double'
                else filterPropertyClass.put(key,propclass);
            }       
            ArrayList<String> temp=new ArrayList<>();
            temp.addAll(userdefinedproperties.keySet());
            temp.addAll(Arrays.asList(standardFilterColumnHeaders));
            filterColumnHeaders=new String[temp.size()];
            filterColumnHeaders=temp.toArray(filterColumnHeaders);
            Arrays.sort(filterColumnHeaders);
        } else filterColumnHeaders=standardFilterColumnHeaders;
       
    }
        
    @SuppressWarnings("unchecked")
    private void updateTable() { 
        columnHeaders=(isMotifTrack || isModuleTrack)?columnHeadersWithLogo:columnHeadersWithoutLogo;        
        model=new RegionTableModel(columnHeaders);        
        columnModel = new DefaultTableColumnModel();
        for (int i=0;i<columnHeaders.length;i++) {
            TableColumn col = new TableColumn(i);
            col.setHeaderValue(model.getColumnName(i));
            columnModel.addColumn(col);
        }        
        regionTable=new JTable(model,columnModel);
        tableScrollPane.setViewportView(regionTable);
        regionTable.setAutoCreateRowSorter(true);
        regionTable.getTableHeader().setReorderingAllowed(false);
        ((javax.swing.JComponent)regionTable.getDefaultRenderer(Boolean.class)).setOpaque(true); // fixes alternating background bug for checkbox renderers in Nimbus
        columnModel.getColumn(SELECTED_COLUMN).setMinWidth(22);
        columnModel.getColumn(SELECTED_COLUMN).setMaxWidth(22);

        columnModel.getColumn(REGION_COLUMN).setCellRenderer(new CellRenderer_Region());
        columnModel.getColumn(SEQUENCE_COLUMN).setCellRenderer(new CellRenderer_Sequence());        
        columnModel.getColumn(FILTER_COLUMN).setCellRenderer(new CellRenderer_FilterColumn());
        columnModel.getColumn(SELECTED_COLUMN).setCellRenderer(new FeatureColorCellRenderer(gui)); // render as colored boxes instead of checkboxes         
        Color [] basecolors=new Color[]{settings.getBaseColor('A'),settings.getBaseColor('C'),settings.getBaseColor('G'),settings.getBaseColor('T')};   
        int rowheight=regionTable.getRowHeight();
        motifLogorenderer=new MotifLogo(basecolors,(int)(rowheight*1.25));
        motifLogorenderer.setUseAntialias(gui.getVisualizationSettings().useMotifAntialiasing());
        moduleLogorenderer=new ModuleLogo(settings);       
        if (isModuleTrack) columnModel.getColumn(LOGO_COLUMN).setCellRenderer(moduleLogorenderer);
        else if (isMotifTrack) columnModel.getColumn(LOGO_COLUMN).setCellRenderer(motifLogorenderer);
        if (isModuleTrack) regionTable.setRowHeight(32);
        Sorter sorter=new Sorter();
        ((TableRowSorter)regionTable.getRowSorter()).setComparator(FILTER_COLUMN, sorter);              
        ((TableRowSorter)regionTable.getRowSorter()).setComparator(REGION_COLUMN, sorter);      
        ((TableRowSorter)regionTable.getRowSorter()).setComparator(SEQUENCE_COLUMN, sorter); 
        ((TableRowSorter)regionTable.getRowSorter()).setComparator(LOCATION_COLUMN, sorter);         
        if (isModuleTrack || isMotifTrack) ((TableRowSorter)regionTable.getRowSorter()).setComparator(LOGO_COLUMN, new RegionLogoComparator());              
        regionTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {                
                if ((e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_V) && !manualFilterSelectionMode) {
                    regionBrowserFilter.disable();
                    int[] rows=regionTable.getSelectedRows();
                    int visibleRows=0;
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        if (settings.isRegionTypeVisible(region.getType())) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        settings.setRegionTypeVisible(region.getType(),doShow,false);                        
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown() && !manualFilterSelectionMode) {
                    int[] rows=regionTable.getSelectedRows();
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        settings.setRegionTypeVisible(region.getType(),doShow,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown() && !manualFilterSelectionMode) {
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;                      
                    setVisibilityOnAllFeatureTypes(doShow);
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_O  && !manualFilterSelectionMode) {               
                    setVisibilityOnAllFeatureTypes(false);                    
                    int[] rows=regionTable.getSelectedRows();
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        settings.setRegionTypeVisible(region.getType(),true,false);
                    }
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_V) && manualFilterSelectionMode) {
                    int[] rows=regionTable.getSelectedRows();
                    int visibleRows=0;
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        if (regionBrowserFilter.containsRegion(region)) visibleRows++;
                    }
                    boolean doShow=Boolean.TRUE;
                    if (visibleRows==rows.length) doShow=Boolean.FALSE;
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        if (doShow) regionBrowserFilter.addRegion(region);
                        else regionBrowserFilter.removeRegion(region);
                    }
                    regionBrowserFilter.fireFilterEvent(filterChangeEvent);
                    settings.redraw();                    
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown() && manualFilterSelectionMode) {
                    int[] rows=regionTable.getSelectedRows();
                    boolean doShow=e.getKeyCode()==KeyEvent.VK_S;
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        if (doShow) regionBrowserFilter.addRegion(region);
                        else regionBrowserFilter.removeRegion(region);
                    }
                    regionBrowserFilter.fireFilterEvent(filterChangeEvent);
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_S  && e.isShiftDown() && manualFilterSelectionMode) {  
                    for (int i=0;i<model.getRowCount();i++) {
                       Region region=(Region)model.getValueAt(i,REGION_COLUMN);
                       regionBrowserFilter.addRegion(region); 
                    }            
                    regionBrowserFilter.fireFilterEvent(filterChangeEvent);
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_H && e.isShiftDown() && manualFilterSelectionMode) { 
                    regionBrowserFilter.clearAll();                   
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_O && manualFilterSelectionMode) {
                    regionBrowserFilter.clearAll();
                    int[] rows=regionTable.getSelectedRows();
                    for (int row:rows) {
                        Region region=(Region)model.getValueAt(regionTable.convertRowIndexToModel(row),REGION_COLUMN);
                        regionBrowserFilter.addRegion(region);
                    }          
                    regionBrowserFilter.fireFilterEvent(filterChangeEvent);                    
                    settings.redraw();                    
                } else if (e.getKeyCode()==KeyEvent.VK_C && !e.isControlDown()) {
                     StringBuilder motifstring=new StringBuilder();
                     int[] rows=regionTable.getSelectedRows();
                     char separator=(e.isShiftDown())?'\n':',';
                     boolean first=true;
                     for (int row:rows) {
                         if (first) first=false; else motifstring.append(separator);
                         int modelrow=regionTable.convertRowIndexToModel(row);
                         String name=((Motif)model.getValueAt(modelrow, 1)).getName();
                         motifstring.append(name);
                     }
                     String motifs=motifstring.toString();
                     gui.logMessage(motifs);
                     java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(motifs);
                     java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                     clipboard.setContents(stringSelection, null);
               } else if (e.getKeyCode()==KeyEvent.VK_PLUS || e.getKeyCode()==KeyEvent.VK_ADD) {
                    int newheight=regionTable.getRowHeight()+1;
                    if (newheight>80) return;
                    motifLogorenderer.setFontHeight((int)(newheight*1.25));
                    regionTable.setRowHeight(newheight);
                } else if (e.getKeyCode()==KeyEvent.VK_MINUS || e.getKeyCode()==KeyEvent.VK_SUBTRACT) {
                    int newheight=regionTable.getRowHeight()-1;
                    if (newheight<8) return;
                    motifLogorenderer.setFontHeight((int)(newheight*1.25));
                    regionTable.setRowHeight(newheight); 
                } else if (e.getKeyCode()==KeyEvent.VK_L) {
                    motifLogorenderer.setScaleByIC(!motifLogorenderer.getScaleByIC());
                    regionTable.repaint();
                } else if (e.getKeyCode()==KeyEvent.VK_G) {
                    Region[] selectedRegions=getSelectedRegions();
                    if (selectedRegions!=null && selectedRegions.length>0) {
                        Region region=selectedRegions[0];
                        gotoRegion(region, region.getParent().getSequenceName());
                    }                 
                } 
            }
        });
 
        // ------- add context menu -------
        regionTableContextMenu=new RegionTableContextMenu(false, true); //
        regionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount()==2) {
                    int column = regionTable.columnAtPoint(evt.getPoint()); //
                    if (column==0) return; // clicked the checkbox                    
                    int modelrow=regionTable.convertRowIndexToModel(regionTable.getSelectedRow());
                    Region region=(Region)model.getValueAt(modelrow, REGION_COLUMN);
                    String sequenceName=(String)model.getValueAt(modelrow, SEQUENCE_COLUMN);
                    if (region!=null) {
                        editRegionProperties(region, regiontrack.getName(), sequenceName);                  
                    }
                }                  
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    int row = regionTable.rowAtPoint(evt.getPoint()); //
                    if (row>=0 && !regionTable.isRowSelected(row)) regionTable.getSelectionModel().setSelectionInterval(row, row);                                          
                    if (regionTable.getSelectedRowCount()>0) showContextMenu(evt);
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                updateCountLabelText();
                if (evt.isPopupTrigger()) {
                    int row = regionTable.rowAtPoint(evt.getPoint()); //
                    if (row>=0 && !regionTable.isRowSelected(row)) regionTable.getSelectionModel().setSelectionInterval(row, row);
                    if (regionTable.getSelectedRowCount()>0) showContextMenu(evt);
                }
            }
        });
       
    } // end setupManualEntryPanel()

    /** Shows a dialog to edit the properties of a region and commits the changes
     *  if the OK button is pressed and the properties have been changes
     */
    protected void editRegionProperties(Region oldregion, String featureName, String sequenceName) {
        boolean updateregion=true;
        Region regionclone=oldregion.clone();
        EditRegionPropertiesDialog propertiesdialog=new EditRegionPropertiesDialog(regionclone, gui);
        propertiesdialog.setLocation(gui.getFrame().getWidth()/2-propertiesdialog.getWidth()/2, gui.getFrame().getHeight()/2-propertiesdialog.getHeight()/2);
        propertiesdialog.setVisible(true);
        if (!propertiesdialog.okPressed()) updateregion=false;
        propertiesdialog.dispose();
        if (updateregion && (oldregion!=null && oldregion.isIdenticalTo(regionclone))) updateregion=false; // no changes have been made
        if (updateregion) {
            gui.updatePartialDataItem(featureName, sequenceName, oldregion, regionclone); // update the registered dataset with the new data from the buffer (this will update the whole dataset not just the single sequence).
        }
    }
    
    /** Zooms in on the sequence so that the region is displayed in the center at 20% of the width of the sequence
     *  The sequence is shown at the top of the sequence window (if possible)
     */
    protected void gotoRegion(Region region, String sequenceName) {
        int start = region.getGenomicStart();
        int end = region.getGenomicEnd();
        int size = end-start+1;
        int newstart = start - (size*2);
        int newend = end + (size*2);
        if (newstart<1) newstart=1;
        Data sequence = gui.getEngine().getDataItem(sequenceName, Sequence.class);
        if (sequence instanceof Sequence) {
            int sequenceStart = ((Sequence)sequence).getRegionStart();
            int sequenceEnd = ((Sequence)sequence).getRegionEnd();
            if (newstart<sequenceStart) newstart = sequenceStart;
            if (newend>sequenceEnd) newend = sequenceEnd;
        }
        settings.setSequenceViewPort(sequenceName, newstart, newend);
        settings.setSequenceVisible(sequenceName, true);
        settings.setRegionTypeVisible(region.getType(),true,false);
        gui.getVisualizationPanel().goToSequence(sequenceName);
        gui.redraw();
    }    
    
    public JTable getTable() {
        return regionTable;
    }
    
    public RegionBrowserFilter getRegionBrowserFilter() {
        if (regionBrowserFilter==null) regionBrowserFilter=new RegionBrowserFilter();
        return regionBrowserFilter;
    }
    
    public void setManualFilterSelectionModeEnabled(boolean enabled) {
        manualFilterSelectionMode=enabled;  
    }
    
    /** Shows or hides all motifs depending on the argument, but does not update visualization */
    public void setVisibilityOnAllFeatureTypes(boolean visible) {
         if (isMotifTrack || isModuleTrack) {
             for (Data data:engine.getAllDataItemsOfType((isMotifTrack)?Motif.class:Module.class)) {
                settings.setRegionTypeVisible(data.getName(), visible, false); 
             }
         } else {
             for (int i=0;i<model.getRowCount();i++) {
                 settings.setRegionTypeVisible(((Region)model.getValueAt(i,REGION_COLUMN)).getType(), visible, false);
             }
        }
    }

    /** Returns the names of the regions in currently selected rows of the table
     *  or NULL if no rows are currently selected
     */
    public HashSet<String> getSelectedRegionTypes() {
        int[] selection=regionTable.getSelectedRows();
        if (selection==null || selection.length==0) return null;
        HashSet<String> selectedRegionNames=new HashSet<String>();
        for (int i=0;i<selection.length;i++) {
            Object region=regionTable.getValueAt(selection[i], REGION_COLUMN);
            if (region instanceof Region) selectedRegionNames.add(((Region)region).getType());   
            else return null;
        }        
        return selectedRegionNames;
    }
    
    /** Returns the regions in currently selected rows of the table
     *  or NULL if no rows are currently selected
     */    
    public Region[] getSelectedRegions() {
        int[] selection=regionTable.getSelectedRows();
        if (selection==null || selection.length==0) return new Region[0];
        Region[] selectedRegions=new Region[selection.length];
        for (int i=0;i<selection.length;i++) {
            Object region=regionTable.getValueAt(selection[i], REGION_COLUMN);
            if (region instanceof Region) selectedRegions[i]=((Region)region);   
            else return null;
        }        
        return selectedRegions;        
    }   
    
    /** Returns the regions currently shown in the table (not hidden by a filter)
     */    
    public Region[] getRegionsShownInTable() {
        int rows=regionTable.getRowCount();
        if (rows==0) return new Region[0];
        Region[] allShownRegions=new Region[rows];
        for (int i=0;i<rows;i++) {
            Object region=regionTable.getValueAt(i, REGION_COLUMN);
            if (region instanceof Region) allShownRegions[i]=((Region)region);   
            else return null;
        }        
        return allShownRegions;        
    }       
    
    /** Updates the label displaying counts (shown, selected and total) in the manual selection tab*/
    private void updateCountLabelText() {
        int total=(model==null)?0:model.getRowCount();
        int matching=(regionTable==null)?0:regionTable.getRowCount();
        numberOfRegionsShowingLabel.setText("<html>Matching: "+matching+" of "+total+"</html>");
    }


     private void showContextMenu(MouseEvent evt) {
        int selectedCount=regionTable.getSelectedRowCount();
        int firstSelected=regionTable.getSelectedRow();
        if (firstSelected>=0 && selectedCount==1) {
            int modelrow=regionTable.convertRowIndexToModel(firstSelected);
            Object value=model.getValueAt(modelrow,1);
            String regionType=null;
            if (value instanceof String) regionType=(String)value;
            else if (value instanceof Region) regionType=((Region)value).getType();
            regionTableContextMenu.setSingleRegionSelected(regionType);
        } else {          
            regionTableContextMenu.setSingleRegionSelected(null);
        }
        regionTableContextMenu.updateMenu();
        regionTableContextMenu.show(evt.getComponent(), evt.getX(),evt.getY());     
     }


    
private class RegionTableModel extends AbstractTableModel { 
    private ArrayList<Region> regions=null;
    private String[] columnNames=null;
    private String filterColumn="Score";
    
    public RegionTableModel(String[] columnNames) {
        if (regions!=null) regions.clear(); else regions=new ArrayList<>();
        if (regiontrack!=null) {
            for (FeatureSequenceData seq:regiontrack.getAllSequences()) {
                regions.addAll(((RegionSequenceData)seq).getOriginalRegions());
            }
        }
        this.columnNames=columnNames;
    }
    
    @Override
    public Class getColumnClass(int c) {
        if (c==SELECTED_COLUMN) return Region.class; // first column
        else if (c==REGION_COLUMN) return Region.class; // 
        else if (c==SEQUENCE_COLUMN) return String.class; // 
        else if (c==LOCATION_COLUMN) return String.class; //         
        else if (c==LOGO_COLUMN) {
            if (isMotifTrack) return Motif.class;
            else if (isModuleTrack) return Module.class;
            else return Region.class;
        }
        else { // c==FILTER_COLUMN
            return filterPropertyClass.get(filterColumn);
        }
    }
    
    @Override    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }
    
    private String getFilterColumn() {return filterColumn;}
    private void setFilterColumn(String filterColumn) {this.filterColumn=filterColumn;}
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // not allowed here
    }

    @Override    
    public int getColumnCount() {
        return columnHeaders.length;
    }
    
    @Override
    public int getRowCount() {
        return regions.size();
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex==SELECTED_COLUMN) return (Region)regions.get(rowIndex);
        else if (columnIndex==REGION_COLUMN) return (Region)regions.get(rowIndex);
        else if (columnIndex==LOGO_COLUMN) {
            Region region=(Region)regions.get(rowIndex);
            if (isMotifTrack) return (Motif)engine.getDataItem(region.getType(), Motif.class);           
            else if (isModuleTrack) return (Module)engine.getDataItem(region.getType(), Module.class);            
            else return region;
        }
        else if (columnIndex==SEQUENCE_COLUMN) return (String)regions.get(rowIndex).getParent().getSequenceName();
        else if (columnIndex==LOCATION_COLUMN) return (String)regions.get(rowIndex).getChromosomalLocationAndOrientationAsString();
        else { // value from filter column
            Region region=regions.get(rowIndex);
            if (filterColumn.equalsIgnoreCase("Motif name")) {
                Motif motif=(Motif)engine.getDataItem(region.getType(), Motif.class);
                if (motif!=null) return motif.getPresentationName();
            }
            return getPropertyValueAsType(region, filterColumn, filterPropertyClass.get(filterColumn)); // standard or user-defined property             
       }
    }

    @Override
    public String getColumnName(int column) {
        if (column==FILTER_COLUMN) return filterColumn;
        else return columnNames[column];
    }
   
}    
        

    /** Returns the specified user-defined property as an object of the given class
     *  if the property is defined for this motif and can be "converted" into an object
     *  of the given class, or NULL if the property is not defined or can not be converted
     *  All properties can be converted to String.class if defined
     */
    public Object getPropertyValueAsType(Region region, String propertyName, Class type) {
        Object value=region.getProperty(propertyName);
        if (value==null) return null;
        if (value.getClass().equals(type)) return value; // no conversion necessary
        if (type.equals(Double.class)) {
            if (value instanceof Number) return new Double(((Number)value).doubleValue());
        }
        if (type.equals(String.class)) {
            if (value instanceof ArrayList) {
                return MotifLabEngine.splice((ArrayList)value,",");
            } else return value.toString(); 
        } 
        return null; // no conversion possible (apparently)
    }


private class CellRenderer_Region extends DefaultTableCellRenderer {
    public CellRenderer_Region() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);              
    }
    @Override
    public void setValue(Object value) {
       if (value!=null && value instanceof Region) {
           String type=((Region)value).getType();
           if (isMotifTrack) {
               Motif motif=(Motif)engine.getDataItem(type, Motif.class);
               if (motif!=null) type=motif.getPresentationName();
           }
           setText(type);
       } else if (value instanceof String) setText((String)value);
       else setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        if (value instanceof Region) { //
           if (!gui.getVisualizationSettings().isRegionTypeVisible(((Region)value).getType())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        return c;
    }
}// end class CellRenderer_Region 

private class CellRenderer_Sequence extends DefaultTableCellRenderer {
    public CellRenderer_Sequence() {
         super();
    }
 
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel comp=(JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String sequencename=(value instanceof Sequence)?((Sequence)value).getName():value.toString();
        VisualizationSettings settings=gui.getVisualizationSettings();
        Color labelColor=settings.getSequenceLabelColor(sequencename);
        boolean isVisible=settings.isSequenceVisible(sequencename);           
        comp.setText(sequencename);
        comp.setForeground((isVisible)?labelColor:Color.LIGHT_GRAY);
        comp.setHorizontalTextPosition(SwingConstants.LEFT);
        comp.setIconTextGap(12);
        if (settings.isSequenceMarked(sequencename)) comp.setIcon(markedSequenceIcon);
        else comp.setIcon(null);     
        return comp;
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
           if (value!=null) {
               String text=value.toString();
               setText(text);          
           }  else setText("");                 
       }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        if (value instanceof Region) { // 
           if (!gui.getVisualizationSettings().isRegionTypeVisible(((Region)value).getType())) this.setForeground((isSelected)?Color.LIGHT_GRAY:Color.GRAY);
           else this.setForeground((isSelected)?Color.WHITE:Color.BLACK);
        }
        return c;
    }

}// end class CellRenderer_FilterColumn    


private class Filter extends RowFilter<Object,Object> {
    String filterString;
    String[][] structuredStringFilter=null;        
    
    @Override
    public boolean include(Entry<? extends Object, ? extends Object> entry) {
     if (filterString==null) return true;
     RegionTableModel model= (RegionTableModel)entry.getModel();
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
             // System.err.println(e.getClass().getSimpleName()+":"+e.getMessage());
             return false;
         }
     } else { // other textual properties
         boolean showContains=(comparator.equals("=") || comparator.equals("<=") || comparator.equals(">="));
         if (value==null) return !showContains;
         String textvalue=value.toString().toLowerCase(); //
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


/** This private class implements the context menu */
private class RegionTableContextMenu extends JPopupMenu implements ActionListener {
     private final String EDIT_REGION="Edit this region";
     private final String GOTO_REGION="Go to region";     
     private final String SHOW="Show Region Type";
     private final String HIDE="Hide Region Type";
     private final String SHOW_ONLY_SELECTED="Show Only Selected Region Type";   
     private final String SHOW_ALL="Show All Region Types";  
     private final String HIDE_ALL="Hide All Region Types";      
     private final String SELECT_SHOWN_REGION_TYPES="Select Shown Region Types";
     private final String SELECT_ONLY_SHOWN_REGION_TYPES="Select Only Shown Region Types";
     private final String SELECT_REGION_TYPES_FROM="Select Region Types From";
     private final String SELECT_ONLY_REGION_TYPES_FROM="Select Only Region Types From";
     private final String CREATE_REGION_TYPE_COLLECTION="Create Collection";   
     private final String SAVE_MOTIF_LOGO="Save Motif Logo";
     private final String COLOR_SUBMENU_HEADER="Set Color"; 
     
     JMenuItem includeSelectedItem=new JMenuItem("Include Selected Region Types"); 
     JMenuItem excludeSelectedItem=new JMenuItem("Exclude Selected Region Types");
     JMenuItem includeListedItem=new JMenuItem("Include Matching Region Types");
     JMenuItem includeNotListedItem=new JMenuItem("Include Not Matching Region Types");
     JMenuItem excludeListedItem=new JMenuItem("Exclude Matching Region Types");
     JMenuItem excludeNotListedItem=new JMenuItem("Exclude Not Matching Region Types");
     JMenuItem includeAllItem=new JMenuItem("Include All Region Types");
     JMenuItem excludeAllItem=new JMenuItem("Exclude All Region Types");
     JMenuItem invertCollectionItem=new JMenuItem("Invert Collection");
     JMenuItem displayItem=new JMenuItem(EDIT_REGION);
     JMenuItem gotoRegionItem=new JMenuItem(GOTO_REGION);     
     JMenuItem compareMotifToOthers=new JMenuItem("Compare"); // this text will be changed dynamically
     JMenuItem saveMotifLogoItem=new JMenuItem(SAVE_MOTIF_LOGO);
     JMenu selectRegionsFromMenu=new JMenu(SELECT_REGION_TYPES_FROM);
     JMenu selectOnlyRegionsFromMenu=new JMenu(SELECT_ONLY_REGION_TYPES_FROM);
     JMenu includeRegionsFromMenu=new JMenu("Include Region Types From");   
     ExternalDBLinkMenu dbmenu=null;   
     SelectFromCollectionListener selectFromCollectionListener=new SelectFromCollectionListener();
     ClearAndSelectFromCollectionListener clearAndselectFromCollectionListener=new ClearAndSelectFromCollectionListener();
     IncludeFromCollectionListener includeFromCollectionListener=new IncludeFromCollectionListener();
              
     
     ColorMenu setColorMenu;
     
    public RegionTableContextMenu(boolean isSelectionMenu, boolean allowCreateCollection) {

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
             includeMenu.add(includeRegionsFromMenu);
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
                int[] selectedRows = regionTable.getSelectedRows();
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (int i=0;i<selectedRows.length;i++) {
                    int modelrow=regionTable.convertRowIndexToModel(selectedRows[i]);
                    Object entry=model.getValueAt(modelrow, REGION_COLUMN);
                    if (entry instanceof Region) settings.setFeatureColor(((Region)entry).getType(), color, false);
                }
                regionTable.repaint();
                gui.redraw();
             }
         };
         JMenuItem showItem=new JMenuItem(SHOW);
         JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED);   
         JMenuItem showAllItem=new JMenuItem(SHOW_ALL);               
         JMenuItem hideItem=new JMenuItem(HIDE);
         JMenuItem hideAllItem=new JMenuItem(HIDE_ALL);
//         JMenuItem selectShownRegionsItem=new JMenuItem(SELECT_SHOWN_REGION_TYPES);
//         JMenuItem selectOnlyShownRegionsItem=new JMenuItem(SELECT_ONLY_SHOWN_REGION_TYPES);
         JMenuItem createCollectionItem=new JMenuItem(CREATE_REGION_TYPE_COLLECTION);  
         updateMenu();
         
         displayItem.addActionListener(this);
         gotoRegionItem.addActionListener(this);
         showItem.addActionListener(this);
         showOnlySelectedItem.addActionListener(this);
         showAllItem.addActionListener(this);
         hideItem.addActionListener(this);
         hideAllItem.addActionListener(this);     
//         selectShownRegionsItem.addActionListener(this);     
//         selectOnlyShownRegionsItem.addActionListener(this);
         if (allowCreateCollection) createCollectionItem.addActionListener(this);
         compareMotifToOthers.addActionListener(this);
         saveMotifLogoItem.addActionListener(this);
         setColorMenu=new ColorMenu(COLOR_SUBMENU_HEADER, colormenulistener, RegionBrowserPanel.this);
         this.add(displayItem);
         this.add(gotoRegionItem);         
         this.add(showItem);
         this.add(showOnlySelectedItem); 
         this.add(showAllItem);            
         this.add(hideItem);   
         this.add(hideAllItem);              
         this.add(setColorMenu);
         this.add(new JSeparator());
//         this.add(selectShownRegionsItem);
//         this.add(selectOnlyShownRegionsItem);
         this.add(selectRegionsFromMenu);
         this.add(selectOnlyRegionsFromMenu);
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
        selectRegionsFromMenu.removeAll();
        selectOnlyRegionsFromMenu.removeAll();        
        includeRegionsFromMenu.removeAll();  
        for (String collectionName:engine.getNamesForAllDataItemsOfType(MotifCollection.class)) {
            JMenuItem subitem=new JMenuItem(collectionName);
            subitem.addActionListener(selectFromCollectionListener);
            selectRegionsFromMenu.add(subitem);        
            JMenuItem subitem2=new JMenuItem(collectionName);
            subitem2.addActionListener(clearAndselectFromCollectionListener);
            selectOnlyRegionsFromMenu.add(subitem2);
            JMenuItem subitem3=new JMenuItem(collectionName);
            subitem3.addActionListener(includeFromCollectionListener);
            includeRegionsFromMenu.add(subitem3);            
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
                selectRegionsFromMenu.add(selectMotifsFromMenuCluster);
                selectOnlyRegionsFromMenu.add(selectOnlyMotifsFromMenuCluster);
                includeRegionsFromMenu.add(includeMotifsFromMenuCluster);                 
            }
        }         
        selectRegionsFromMenu.setEnabled(selectRegionsFromMenu.getMenuComponentCount()>0);
        selectOnlyRegionsFromMenu.setEnabled(selectOnlyRegionsFromMenu.getMenuComponentCount()>0);
    }    
    
    /** This should be called when the context menu is invoked with only one selected region 
     */
    public void setSingleRegionSelected(String targetRegionName) {
        if (isMotifTrack || isModuleTrack) dbmenu.updateMenu(targetRegionName,true);
        else dbmenu.updateMenu(targetRegionName,regiontrack.getName(),true);
        if (targetRegionName==null) { // not a single region
             if (compareMotifToOthers!=null) compareMotifToOthers.setVisible(false);
             if (saveMotifLogoItem!=null) saveMotifLogoItem.setVisible(false);
             displayItem.setVisible(false);
             gotoRegionItem.setVisible(false);
        } else if (isMotifTrack) {
            String command="Compare "+targetRegionName+" To Other Motifs";
            compareMotifToOthers.setText(command);
            compareMotifToOthers.setVisible(true);
            saveMotifLogoItem.setVisible(true);
            displayItem.setText(EDIT_REGION+" ("+targetRegionName+")");
            displayItem.setVisible(true);  
            gotoRegionItem.setVisible(true);
        } else {
            compareMotifToOthers.setVisible(false);
            saveMotifLogoItem.setVisible(false);
            displayItem.setVisible(true);  
            gotoRegionItem.setVisible(true);            
        }
    }

     @Override
     public void actionPerformed(ActionEvent e) {
         String cmd=e.getActionCommand();
         if (cmd.equals(includeSelectedItem.getActionCommand())) {
             int[] rows=regionTable.getSelectedRows();
             for (int row:rows) {
                 int modelrow=regionTable.convertRowIndexToModel(row);
                 model.setValueAt(true, modelrow, 0);
             }
         } else if (cmd.equals(excludeSelectedItem.getActionCommand())) {
             int[] rows=regionTable.getSelectedRows();
             for (int row:rows) {
                 int modelrow=regionTable.convertRowIndexToModel(row);
                 model.setValueAt(false, modelrow, 0);
             }             
         } else if (cmd.equals(includeListedItem.getActionCommand())) {
             for (int i=0;i<regionTable.getRowCount();i++) {
                 int modelrow=regionTable.convertRowIndexToModel(i);
                 model.setValueAt(true, modelrow, 0);
             }            
         } else if (cmd.equals(includeNotListedItem.getActionCommand())) {             
             for (int i=0;i<model.getRowCount();i++) {
                 int viewrow=regionTable.convertRowIndexToView(i);
                 if (viewrow==-1) model.setValueAt(true, i, 0);
             }                  
         } else if (cmd.equals(excludeListedItem.getActionCommand())) {
             for (int i=0;i<regionTable.getRowCount();i++) {
                 int modelrow=regionTable.convertRowIndexToModel(i);
                 model.setValueAt(false, modelrow, 0);
             }
         } else if (cmd.equals(excludeNotListedItem.getActionCommand())) {
             for (int i=0;i<model.getRowCount();i++) {
                 int viewrow=regionTable.convertRowIndexToView(i);
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
         } else if (e.getActionCommand().startsWith(EDIT_REGION)) {
                Region[] selectedRegions=getSelectedRegions();
                if (selectedRegions!=null && selectedRegions.length>0) {
                    Region region=selectedRegions[0];
                    editRegionProperties(region, regiontrack.getName(), region.getParent().getSequenceName());
                }
         } else if (e.getActionCommand().startsWith(GOTO_REGION)) {
                Region[] selectedRegions=getSelectedRegions();
                if (selectedRegions!=null && selectedRegions.length>0) {
                    Region region=selectedRegions[0];
                    gotoRegion(region, region.getParent().getSequenceName());
                }                              
         } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                setVisibilityOnAllFeatureTypes(false);
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String regionType:getSelectedRegionTypes()) {
                   settings.setRegionTypeVisible(regionType, true, false);   
                }
                gui.redraw();
         } else if (e.getActionCommand().equals(SHOW_ALL)) {
                setVisibilityOnAllFeatureTypes(true);
                gui.redraw();                        
         } else if (e.getActionCommand().equals(HIDE_ALL)) {
                setVisibilityOnAllFeatureTypes(false);
                gui.redraw(); 
         } else if (e.getActionCommand().equals(SHOW)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String regionType:getSelectedRegionTypes()) {
                   settings.setRegionTypeVisible(regionType, true, false);   
                }
                gui.redraw();
         } else if (e.getActionCommand().equals(HIDE)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                for (String regionType:getSelectedRegionTypes()) {
                   settings.setRegionTypeVisible(regionType, false, false);   
                }
                gui.redraw();
         } else if (e.getActionCommand().equals(CREATE_REGION_TYPE_COLLECTION)) {
                ArrayList<String> list=new ArrayList<String>(getSelectedRegionTypes());
                if (isMotifTrack) gui.promptAndCreateMotifCollection(list);
                else if (isModuleTrack) gui.promptAndCreateMotifCollection(list);
                else gui.promptAndCreateTextVariable(list);
         } else if (e.getActionCommand().equals(SELECT_SHOWN_REGION_TYPES)) {
                VisualizationSettings settings=gui.getVisualizationSettings();
                ArrayList<String> allMotifs=engine.getNamesForAllDataItemsOfType(Motif.class);
                HashSet<String> shownMotifs=new HashSet<String>(allMotifs.size());
                for (String motifname:allMotifs) {
                   if (settings.isRegionTypeVisible(motifname)) shownMotifs.add(motifname);
                }
                selectRowsForMotifs(shownMotifs,false);
//         } else if (e.getActionCommand().equals(SELECT_ONLY_SHOWN_REGION_TYPES)) {
//                VisualizationSettings settings=gui.getVisualizationSettings();
//                ArrayList<String> allMotifs=engine.getNamesForAllDataItemsOfType(Motif.class);
//                HashSet<String> shownMotifs=new HashSet<String>(allMotifs.size());
//                for (String motifname:allMotifs) {
//                   if (settings.isRegionTypeVisible(motifname)) shownMotifs.add(motifname);
//                }
//                selectRowsForRegions(shownMotifs,true);
         } else if (cmd.startsWith("Compare")) {
            int firstrow=regionTable.getSelectedRow();
            if (firstrow>=0) {
                int modelrow=regionTable.convertRowIndexToModel(firstrow);
                Region region=(Region)model.getValueAt(modelrow, REGION_COLUMN);
                Motif target=(region==null)?null:(Motif)engine.getDataItem(region.getType(), Motif.class);
                if (target!=null) {
                    MotifComparisonDialog motifcomparisonPanel=new MotifComparisonDialog(gui,target,isModal);
                    int x=gui.getGUIFrame().getWidth()/2-motifcomparisonPanel.getWidth()/2; if (x<0) x=0;
                    int y=gui.getGUIFrame().getHeight()/2-motifcomparisonPanel.getHeight()/2; if (y<0) y=0;
                    motifcomparisonPanel.setLocation(x, y);                 
                    motifcomparisonPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    motifcomparisonPanel.setVisible(true);
                }
            }
         } else if (e.getActionCommand().equals(SAVE_MOTIF_LOGO))  {
            int firstrow=regionTable.getSelectedRow();
            if (firstrow>=0) {
                int modelrow=regionTable.convertRowIndexToModel(firstrow);
                Region region=(Region)model.getValueAt(modelrow, REGION_COLUMN);
                Motif target=(region==null)?null:(Motif)engine.getDataItem(region.getType(), Motif.class);
                if (target!=null) {          
                    SaveMotifLogoImageDialog saveLogoPanel=new SaveMotifLogoImageDialog(gui, target, isModal);
                    int x=gui.getGUIFrame().getWidth()/2-saveLogoPanel.getWidth()/2; if (x<0) x=0;
                    int y=gui.getGUIFrame().getHeight()/2-saveLogoPanel.getHeight()/2; if (y<0) y=0;
                    saveLogoPanel.setLocation(x, y);                 
                    saveLogoPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    saveLogoPanel.setVisible(true);   
                }
            }
         }
         regionTable.repaint();
         updateCountLabelText();
     }

        
} // END private class RegionTableContextMenu


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
        if (clearCurrentSelection) regionTable.clearSelection();
        ListSelectionModel selection=regionTable.getSelectionModel();        
        int motifcolumn=REGION_COLUMN;
        for (int i=0;i<regionTable.getRowCount();i++) {
            Object value=regionTable.getValueAt(i, motifcolumn);      
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
            Object value=model.getValueAt(i, REGION_COLUMN);      
            String motifname=((Motif)value).getName();
            if (motifs.contains(motifname)) {
                model.setValueAt(Boolean.TRUE, i, SELECTED_COLUMN);
            }
        }
        updateCountLabelText();
    }     

    /** A class to sort logos based on the length of the region */
    private class RegionLogoComparator implements java.util.Comparator<Region> {
        @Override
        public int compare(Region region1, Region region2) {
            return region1.getLength()-region2.getLength();
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
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); // necessary to get correct alternating row coloring in Nimbus
            String featurename="";
            if (value instanceof Region) featurename=((Region)value).getType();
            else if (value!=null) featurename=value.toString();
            selectedicon.setForegroundColor(settings.getFeatureColor(featurename));
            if (manualFilterSelectionMode && value instanceof Region) selectedicon.setBorderColor(regionBrowserFilter.containsRegion((Region)value)?Color.BLACK:Color.LIGHT_GRAY);
            else selectedicon.setBorderColor((settings.isRegionTypeVisible(featurename))?Color.BLACK:Color.LIGHT_GRAY);
            return this;
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
            } else if (o1 instanceof Region && o2 instanceof Region) {
                return MotifLabEngine.compareNaturalOrder(((Region)o1).getType(),((Region)o2).getType());
            } else {
                return MotifLabEngine.compareNaturalOrder(o1.toString(),o2.toString());
            }
        }      
    }
    
   
    
   

}
