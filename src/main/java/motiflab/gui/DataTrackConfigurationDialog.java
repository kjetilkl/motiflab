/*
 * DataTrackConfigurationDialog.java
 *
 * Created on 18. juni 2009, 11:00
 */

package motiflab.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.activation.DataHandler;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdesktop.application.Action;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Organism;
import motiflab.engine.datasource.DataTrack;
import motiflab.engine.datasource.DataSource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;
import motiflab.engine.ConfigurablePlugin;
import motiflab.engine.ExecutionError;
import motiflab.engine.datasource.DataConfiguration;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.MotifLabResource;
import motiflab.engine.Plugin;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.dataformat.DataFormat_2bit;
import motiflab.engine.dataformat.DataFormat_BED;
import motiflab.engine.dataformat.DataFormat_BigBed;
import motiflab.engine.dataformat.DataFormat_BigWig;
import motiflab.engine.dataformat.DataFormat_FASTA;
import motiflab.engine.dataformat.DataFormat_WIG;
import motiflab.engine.datasource.DBfield;
import motiflab.engine.datasource.DataSource_DAS;
import motiflab.engine.datasource.DataSource_FileServer;
import motiflab.engine.datasource.DataSource_SQL;
import motiflab.engine.datasource.DataSource_VOID;
import motiflab.engine.datasource.DataSource_http_GET;
import motiflab.engine.datasource.Server;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author  kjetikl
 */
public class DataTrackConfigurationDialog extends javax.swing.JDialog {
    private final String TABLECOLUMN_NAME="Name";    
    private final String TABLECOLUMN_TYPE="Type";
    private final String TABLECOLUMN_SOURCE="Provider";
    private final String TABLECOLUMN_ORGANISM="Organism";
    private final String TABLECOLUMN_BUILD="Build";
    private final String TABLECOLUMN_SUPPORTED_ORGANISMS="Supported organism";
    private final String TABLECOLUMN_PROTOCOL="Protocol";
    private final String TABLECOLUMN_DATAFORMAT="Format";
    private final String TABLECOLUMN_SERVER="Server";
    private final String TABLECOLUMN_SERVERADDRESS="Server address";
    private final String TABLECOLUMN_DELAY="Delay (ms)";
    private final String TABLECOLUMN_MAXSPAN="Max span (bp)";    
    private final String TABLECOLUMN_MIRRORS="Mirror sites";
    
    private final int SERVER_COLUMN_SERVERADDRESS=0;
    private final int SERVER_COLUMN_DELAY=1;
    private final int SERVER_COLUMN_MAXSPAN=2;
    private final int SERVER_COLUMN_MIRRORS=3;

    private MotifLabGUI gui;
    private MotifLabEngine engine;
    
    private DefaultTableModel datatrackTableModel;
    private DefaultTableModel datasourceTableModel;
    private DefaultTableModel serverTableModel;
    private JTable datatrackTable;
    private JTable datasourceTable;
    private JTable serverTable;
    private JTable SQLsourceTable;
    private ParametersPanel dataFormatParametersPanel=null; // reference to the panel containing settings specific for the selected data format
    private SupportedOrganismsRendered organismRenderer=new SupportedOrganismsRendered();
    private DataConfiguration dataconfiguration=null;
    private HashMap<String,DataTrack> availableTracks=null;
    private HashMap<String,Server> availableServers=null;
    private DataTrack currentDataTrack=null;
    private DataSource currentDataSource=null;
    private boolean addDataTrack=false; // TRUE when adding, FALSE when editing
    private boolean addDataSource=false;// TRUE when adding, FALSE when editing
    
    private RowFilter<Object,Object> expressionRowFilter=null;    
    
    /** Creates new form DataTrackDialog */
    @SuppressWarnings("unchecked")
    public DataTrackConfigurationDialog(MotifLabGUI gui) {
        super(gui.getFrame(), true);
        this.gui=gui;
        engine=gui.getEngine();
        initComponents();
        // Hack since Netbeans no longer allows be to edit the components in initComponents()
        
        // -----------------
        cardpaneEditSourceMainCardPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(), 
                BorderFactory.createEmptyBorder(1, 10, 1, 10)
        ));
        // first make a copy (clone) of the current data configuration
        dataconfiguration=(DataConfiguration)gui.getEngine().getDataLoader().getCurrentDataConfiguration().clone();
        availableTracks=dataconfiguration.getAvailableTracks(); // reference to the actual object in the dataconfiguration
        availableServers=dataconfiguration.getServers();        // reference to the actual object in the dataconfiguration
        
        datatrackTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_TYPE,TABLECOLUMN_SOURCE,TABLECOLUMN_SUPPORTED_ORGANISMS},0);
        datatrackTable=new JTable(datatrackTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };

        datatrackTable.setFillsViewportHeight(true);
        datatrackTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        datatrackTable.setRowSelectionAllowed(true);
        datatrackTable.getTableHeader().setReorderingAllowed(false);
        datatrackTable.getColumn(TABLECOLUMN_TYPE).setMaxWidth(50);
        datatrackTable.getColumn(TABLECOLUMN_TYPE).setMaxWidth(50);
        datatrackTable.getColumn(TABLECOLUMN_TYPE).setCellRenderer(new DataTypeRenderer());
        datatrackTable.getColumn(TABLECOLUMN_SUPPORTED_ORGANISMS).setCellRenderer(organismRenderer);               
        datatrackTable.setRowHeight(18);
        cardpaneAllTracksScrollPane.setViewportView(datatrackTable);  
      
        datatrackTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
                   allTracksEditButtonClicked();
               }
            }           
        });
        datatrackTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                boolean selected=(datatrackTable.getSelectedRowCount()>0);            
                allTracksEditButton.setEnabled(selected);
                allTracksRemoveButton.setEnabled(selected);
            }
        });
        datatrackTable.setAutoCreateRowSorter(true);
        datatrackTable.getRowSorter().toggleSortOrder(datatrackTable.getColumn(TABLECOLUMN_NAME).getModelIndex());

        datasourceTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_ORGANISM,TABLECOLUMN_BUILD,TABLECOLUMN_PROTOCOL,TABLECOLUMN_DATAFORMAT,TABLECOLUMN_SERVER},0);      
        datasourceTable=new JTable(datasourceTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };          
        datasourceTable.setFillsViewportHeight(true);
        datasourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        datasourceTable.setRowSelectionAllowed(true);
        datasourceTable.getTableHeader().setReorderingAllowed(false);
        datasourceTable.setRowHeight(18);
        datasourceTable.getColumn(TABLECOLUMN_ORGANISM).setCellRenderer(organismRenderer);
        datasourceTable.getColumn(TABLECOLUMN_BUILD).setPreferredWidth(50);      // does not work properly
        datasourceTable.getColumn(TABLECOLUMN_PROTOCOL).setPreferredWidth(50);   // does not work properly
        datasourceTable.getColumn(TABLECOLUMN_DATAFORMAT).setPreferredWidth(120);// does not work properly
        datasourceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
                   editDataSource();
               }
            }
            
        });
        datasourceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {              
                boolean selected=(datatrackTable.getSelectedRowCount()>0 || datasourceTable.getSelectedRows().length>0); // getSelectedRowCount() sometimes return 0 even if rows are selected, which is wrong!           
                editTrackEditButton.setEnabled(selected);
                editTrackRemoveButton.setEnabled(selected);
            }
        });        
        datasourceTable.setDragEnabled(true);
        datasourceTable.setDropMode(DropMode.INSERT_ROWS);
        datasourceTable.setTransferHandler(new TableRowTransferHandler(datasourceTable)); 
        cardpaneEditTracksScrollPane.setViewportView(datasourceTable);      
        
        serverTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_SERVERADDRESS,TABLECOLUMN_DELAY,TABLECOLUMN_MAXSPAN,TABLECOLUMN_MIRRORS},0);      
        serverTable=new JTable(serverTableModel); 
        ExcelAdapter adapter=new ExcelAdapter(serverTable,true, ExcelAdapter.CONVERT_TO_INTEGER); // enables copy/paste capabilities in the table
        serverTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        serverTable.setFillsViewportHeight(true);
        serverTable.getTableHeader().setReorderingAllowed(false);
        serverTable.setRowHeight(18);
        serversScrollPane.setViewportView(serverTable);      
        serverTable.setAutoCreateRowSorter(true);
        serverTable.setCellSelectionEnabled(true);
        serverTable.getRowSorter().toggleSortOrder(serverTable.getColumn(TABLECOLUMN_SERVERADDRESS).getModelIndex());
        
        setupTablesFromConfiguration();      
        editSourceDataFormatCombobox.setModel(getSupportedDataFormats(null, null)); // show all by default
        Integer[] organismsList=Organism.getSupportedOrganismIDs();
        Arrays.sort(organismsList, new Comparator<Integer>(){
            @Override 
            public int compare(Integer val1, Integer val2) {
               String name1=Organism.getCommonName((val1==null)?0:val1.intValue());
               String name2=Organism.getCommonName((val2==null)?0:val2.intValue());
               return name1.compareTo(name2);
             }
        });
        editSourceOrganismCombobox.setModel(new DefaultComboBoxModel(organismsList));
        OrganismComboboxRenderer organismrenderer=new OrganismComboboxRenderer();
        editSourceOrganismCombobox.setRenderer(organismrenderer);
        
        editSourceProtocolCombobox.setModel(new DefaultComboBoxModel(getAllDataSourceProtocols()));
        editSourceProtocolCombobox.setRenderer(new DisableableComboRenderer());
        editSourceProtocolCombobox.setMinimumSize(new Dimension(50, 20));
        editSourceProtocolCombobox.setSelectedItem(DataSource_http_GET.PROTOCOL_NAME); // this is safe as it applies to all types of tracks and hence is never disabled
        editSourceProtocolCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { // protocol is changed
                String selected=editSourceProtocolCombobox.getSelectedItem().toString();              
                Object renderer=editSourceProtocolCombobox.getRenderer();
                if (renderer instanceof DisableableComboRenderer) {
                    if (((DisableableComboRenderer)renderer).isItemEnabled(selected)) {
                       ((DisableableComboRenderer)renderer).setPreviouslySelected(selected);
                    } else { // invalid selection
                        String previous=((DisableableComboRenderer)renderer).getPreviouslySelected();
                        if (previous!=null) selected=previous; //
                        else selected=DataSource_http_GET.PROTOCOL_NAME; // select one that is always applicable     
                        editSourceProtocolCombobox.setSelectedItem(selected);
                    }
                }                 
                // selection is OK here   
                editSourceDataFormatCombobox.setModel(getSupportedDataFormats(null, null));       
                try {editSourceDataFormatCombobox.setSelectedIndex(0);} catch (IllegalArgumentException ie) {} // force selection of first applicable data format
                boolean useDataFormat=usesStandardDataFormat(selected);
                setSourceDataFormatEnabled(useDataFormat);
            }
        });    
        dataTrackPropertiesTypeCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String typename=(String)dataTrackPropertiesTypeCombobox.getSelectedItem();
                Class newtype=null;
                     if (typename.equals("Sequence")) newtype=DNASequenceDataset.class;
                else if (typename.equals("Numeric")) newtype=NumericDataset.class;
                else if (typename.equals("Region")) newtype=RegionDataset.class;
                String[] notsupported=getDataSourceProtocolsNotSupportingFeatureDataType(newtype);
                Object renderer=editSourceProtocolCombobox.getRenderer();
                if (renderer instanceof DisableableComboRenderer) {
                    ((DisableableComboRenderer)renderer).setDisabledItems(notsupported);
                    String selectedProtocol=(String)editSourceProtocolCombobox.getSelectedItem();
                    if (!((DisableableComboRenderer)renderer).isItemEnabled(selectedProtocol)) editSourceProtocolCombobox.setSelectedItem(DataSource_http_GET.PROTOCOL_NAME); // to be safe
                }
                // Give a warning if the user attempts to change the data type of a track that already has data sources, since this will surely lead to problems later
                if (currentDataTrack!=null && currentDataTrack.getDataType()!=newtype) {
                    ArrayList<DataSource> sources=currentDataTrack.getDatasources();
                    if (sources!=null && !sources.isEmpty()) JOptionPane.showMessageDialog(dataTrackPropertiesTypeCombobox, "Changing the track type after data sources have been added will almost certainly lead to problems!", "WARNING", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        editSourceDataFormatCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String protocol=(String)editSourceProtocolCombobox.getSelectedItem();
                Object selected=editSourceDataFormatCombobox.getSelectedItem();                
                if (selected!=null) showDataFormatSettingsPanel(protocol,getDataFormatSettingsPanel((String)selected)); 
                else showDataFormatSettingsPanel(protocol,null);               
                pack();
                // the following is a hack which (if allowed by the user) changes "hgta_doTopSubmit=1" to "hgta_doGetBed=get+BED" in the UCSC url when format is changed to Interactions
                if (selected!=null && selected.toString().equalsIgnoreCase("Interactions")) {
                    String paramString=editGETsourceParametersTextfield.getText();
                    if (paramString!=null && paramString.contains("hgta_doTopSubmit=1")) {
                        int answer=JOptionPane.showConfirmDialog(editSourcePropertiesPanel, "When switching to the 'Interactions' data format,\nsome changes to the 'Parameters' setting are recommended.\nWould you like to implement them?", "", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (answer==JOptionPane.YES_OPTION) {
                            paramString=paramString.replace("hgta_doTopSubmit=1", "hgta_doGetBed=get+BED");
                            editGETsourceParametersTextfield.setText(paramString);
                        }
                    }
                }
            }
        });       
        SQLsourceTable=getSQLsourceTable();
        editSQLsourceTableScrollPane.setViewportView(SQLsourceTable);
        SQLsourceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                boolean selected=(SQLsourceTable.getSelectedRowCount()>0);            
                editSQLsourceRemovePropertyButton.setEnabled(selected);
            }
        });    
        editSQLsourceAddPropertyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addRowToSQLsourceTable();
            }
        }); 
        editSQLsourceRemovePropertyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeSelectedRowsFromSQLsourceTable();
            }
        }); 
//        // disable the SegmentSize widget for all eternity since we do not allow "legacy" file servers to be configured
//        editFILEsourceSegmentsizeLabel.setVisible(false);
//        editFILEsourceSegmentsizeSpinner.setVisible(false);
        dataTrackPropertiesDisplaySettingsLabel.setIcon(datasourcesLabel.getIcon());
        dataTrackPropertiesDisplaySettingsLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        String displaySettingsHelp = "<html>A semicolon-separated list of <i>display settings</i> (protocol directives)<br>that can be used to control how the track will be displayed.<br>"
                + "Use the placeholder \" <b>?</b> \" to refer to the track name (i.e. the \"target\").<br><br>Example:<br><tt>$multicolor(?)=true;$height(?)=100;</tt></html>";
        dataTrackPropertiesDisplaySettingsLabel.setToolTipText(displaySettingsHelp);      
        dataTrackPropertiesNameLabel.setIcon(new SimpleDataPanelIcon(16, 16, SimpleDataPanelIcon.BLANK_ICON, null)); // I have to add an icon to this label as well, or else it will be misaligned for some reason
        dataTrackPropertiesNameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);  
        
        showTopMenuPanel();
        pack();
        gui.getFrame().setCursor(Cursor.getDefaultCursor());
    }
    
    /**
     * Returns the names of all the different data source protocols that are being used
     * @return 
     */
    private String[] getAllDataSourceProtocols() {
        ArrayList<MotifLabResource> datasources=engine.getResources("DataSource");
        ArrayList<String> protocolnames=new ArrayList<>();
        for (MotifLabResource resource:datasources) {
            Object datasourceTemplate=resource.getResourceInstance();
            if (datasourceTemplate instanceof DataSource) {
                String protocolName=((DataSource)datasourceTemplate).getProtocol();
                protocolnames.add(protocolName);
            } else engine.logMessage("WARNING: Registered Data Source template is NOT a valid Data Source, but a "+((datasourceTemplate==null)?"NULL":datasourceTemplate.toString()));
        }
        String[] datasourceprotocols=new String[protocolnames.size()];
        Collections.sort(protocolnames);
        return protocolnames.toArray(datasourceprotocols);
    }
    
    /**
     * Returns a template instance of a DataSource with the specified protocol
     * @param protocol
     * @return 
     */
    private DataSource getDataSourceInstance(String protocol) {
       DataSource datasource=(DataSource)engine.getResource(protocol, "DataSource");
       return datasource;
    }
    
     /** Returns the names of DataSource types (protocols) that support the given feature dataset type */
     public String[] getDataSourceProtocolsSupportingFeatureDataType(Class type) {
         ArrayList<String> supported=new ArrayList<String>();
         ArrayList<MotifLabResource> datasources=engine.getResources("DataSource");
         for (MotifLabResource resource:datasources) {
            Object datasourceTemplate=resource.getResourceInstance();
            if (datasourceTemplate instanceof DataSource) {
                String protocolName=((DataSource)datasourceTemplate).getProtocol();
                if (((DataSource)datasourceTemplate).supportsFeatureDataType(type)) supported.add(protocolName);
            } else engine.logMessage("WARNING: Registered Data Source template is NOT a valid Data Source, but a "+((datasourceTemplate==null)?"NULL":datasourceTemplate.toString()));
         }         
         String[] result=new String[supported.size()];
         return supported.toArray(result);
     }
     
     /** Returns the names of DataSource types (protocols) that do NOT support the given feature dataset type */
     public String[] getDataSourceProtocolsNotSupportingFeatureDataType(Class type) {        
         ArrayList<String> collection=new ArrayList<String>();
         collection.addAll(Arrays.asList(getAllDataSourceProtocols()));
         String[] supported=getDataSourceProtocolsSupportingFeatureDataType(type);
         collection.removeAll(Arrays.asList(supported));
         String[] result=new String[collection.size()];
         return collection.toArray(result);
     }      
    
    private DefaultComboBoxModel getAddDataTrackOptions() {
        ArrayList<String> options=new ArrayList<>();
        options.add("UCSC Genome Browser");
        options.add("DAS Registry");
        options.add("Configuration file");
        options.add("Manual entry");
        // add plugin options        
        ArrayList<MotifLabResource> resources=engine.getResources("DataTrackConfigurationDialog"); // Note that the resource type is different from the one in the method below
        for (MotifLabResource r:resources) {
            options.add(r.getResourceName());
        }       
        String[] list=new String[options.size()];
        return new DefaultComboBoxModel(options.toArray(list));  // 
    }
    
    private DefaultComboBoxModel getAddDataSourceOptions() {
        ArrayList<String> options=new ArrayList<>();
        options.add("Manual entry");
        options.add("UCSC Genome Browser");
        options.add("DAS Registry");        
        // add plugin options
        ArrayList<MotifLabResource> resources=engine.getResources("DataSourceConfigurationDialog"); // Note that the resource type is different from the one in the method above
        for (MotifLabResource r:resources) {
            options.add(r.getResourceName());
        }           
        String[] list=new String[options.size()];
        return new DefaultComboBoxModel(options.toArray(list));  //         
    }
    
    /** Returns a comboboxmodel containing the data formats supported by the specified FeatureDataset type and DataSource protocol
     *  If any of the parameters provided are NULL, they will instead be inferred from the current selections in the GUI
     */
    private DefaultComboBoxModel getSupportedDataFormats(Class type, String protocol) {
        if (type==null) {
            String typeString=(String)dataTrackPropertiesTypeCombobox.getSelectedItem();
            if (typeString!=null && typeString.equals("Sequence")) type=DNASequenceDataset.class;
            else if (typeString!=null && typeString.equals("Numeric")) type=NumericDataset.class;
            else if (typeString!=null && typeString.equals("Region")) type=RegionDataset.class;
        }
        ArrayList<DataFormat> inputformatsList=(type!=null)?gui.getEngine().getDataInputFormats(type):gui.getEngine().getFeatureDataInputFormats();
        Collections.sort(inputformatsList);
        if (protocol==null) protocol=(String)editSourceProtocolCombobox.getSelectedItem();
        // the inputformatsList now contains all dataformats supported by the FeatureDataset type. Next, we also filter based on those supported by the DataSource protocol
        if (protocol!=null) {
            DataSource datasource = getDataSourceInstance(protocol);
            inputformatsList = datasource.filterProtocolSupportedDataFormats(inputformatsList);
        }
        
        String[] list=new String[inputformatsList.size()];
        int i=0;
        for (DataFormat formatter:inputformatsList) {
            list[i]=formatter.getName();
            i++;
        }
        return new DefaultComboBoxModel(list);        
    }
  
// --- The functionality of these 3 methods have been replaced by methods in other classes    
    
//    private ArrayList<DataFormat> filterDataFormatsThatOnlyParseLocalFiles(ArrayList<DataFormat> list) {
//        Iterator<DataFormat> iter=list.iterator();
//        while (iter.hasNext()) {
//            DataFormat format=iter.next();
//            if (format.canOnlyParseDirectlyFromLocalFile()) iter.remove();
//        }
//        return list;
//    }    
//    
//    /** Returns the intersections between the two provided lists */
//    private ArrayList<DataFormat> filterDataFormats(ArrayList<DataFormat> list, Class[] filter) {
//        if (filter==null || filter.length==0) return list;
//        ArrayList<DataFormat> result=new ArrayList<DataFormat>();
//        for (DataFormat format:list) {
//            if (inClassFilter(format, filter)) result.add(format);
//        }
//        return result;
//    }
//    
//    private boolean inClassFilter(Object o, Class[] filter) {
//        for (Class c:filter) {
//            if (o.getClass()==c) return true;
//        }
//        return false;
//    }
    
    /** */
    private void showDataFormatSettingsPanel(String protocol, JPanel panel) {
        
        // The GET and FILE protocol are the only ones that support standard DataFormats thay may have additional Data format settings
        // whereas SQL, DAS and VOID protocols do not support standard DataFormats.
        // Note, however, that future DataSource plugins may also support standard DataFormats
        
        // The two panels below: "additionalDataFormatSettingsPanel_GET" and "additionalDataFormatSettingsPanel_FILE"
        // are just containers that are added to the higher-level panels for each of these DataSource types (in different "cards")

        // The panel to define the dataformat-specific settings is provided by the second parameter to this method,
        // but this is also a "singleton" which is referenced by the global variable "dataFormatParametersPanel".
        // This (apparently) makes it easier to parse the settings afterwards.
        
        // the settings panel (and hence also the global "dataFormatParametersPanel" variable) is created anew every time
        // the DataFormat is changed (by the user selecting a different one in a combobox), since each DataFormat will have a new set of format-specific parameters. 
        // (The settings panel is automatically built from the parameters exported by the data format)
        
        // The purpose of this particular method is to add this new singleton panel to the 
        // correct parent container, which can be either the GET or FILE panel cards, but may in the future also be plugin cards?
        // So, this hardcoding is maybe not be best way to deal with things.
        
        additionalDataFormatSettingsPanel_GET.removeAll();
        additionalDataFormatSettingsPanel_FILE.removeAll();
        if (panel!=null) {
            if (protocol.equals(DataSource_http_GET.PROTOCOL_NAME)) {
                additionalDataFormatSettingsPanel_GET.add(panel);
                additionalDataFormatSettingsPanel_GET.invalidate();
                additionalDataFormatSettingsPanel_GET.repaint();          
            } else if (protocol.equals(DataSource_FileServer.PROTOCOL_NAME)) {
                additionalDataFormatSettingsPanel_FILE.add(panel);
                additionalDataFormatSettingsPanel_FILE.invalidate();
                additionalDataFormatSettingsPanel_FILE.repaint();       
            }   
        }
    } 
    
    /** */
    private JPanel getDataFormatSettingsPanel(String outputformatName) {
        DataFormat outputFormat=engine.getDataFormat(outputformatName);
        if (outputFormat==null) {
            dataFormatParametersPanel=null;
            return new JPanel();
        }
        //ParametersPanel panel=new ParametersPanel(outputFormat.getParameters(),parameterSettings, this);
        ParameterSettings settings=null;
        if (currentDataSource!=null) settings=currentDataSource.getDataFormatSettings();
        ParametersPanel panel=new ParametersPanel(outputFormat.getParameters(),settings, null, null, engine);
        dataFormatParametersPanel=panel;
        // the following is just a little hack to show a widget for the Segment Size parameter used by legacy File Servers
//        boolean showFileSegmentSizeWidget=(outputFormat instanceof DataFormat_BED || outputFormat instanceof DataFormat_FASTA || outputFormat instanceof DataFormat_WIG);
//        editFILEsourceSegmentsizeLabel.setEnabled(showFileSegmentSizeWidget);
//        editFILEsourceSegmentsizeSpinner.setEnabled(showFileSegmentSizeWidget);
        return panel;
    }
    
    
    /** Fills in data in the DataTracks and DataSources JTables based on the current config */
    private void setupTablesFromConfiguration() {  
         int tracknumber=datatrackTableModel.getRowCount();
         if (tracknumber>0) {
             for (int i=0;i<tracknumber;i++) datatrackTableModel.removeRow(0);
         }
         int servernumber=serverTableModel.getRowCount();
         if (servernumber>0) {
             for (int i=0;i<servernumber;i++) serverTableModel.removeRow(0);
         }
         for (DataTrack track:availableTracks.values()) {
            int[] supportedOrganisms=track.getSupportedOrganisms();
            Object[] values=new Object[]{track.getName(),track.getDataType(),track.getSourceSite(),supportedOrganisms};
            datatrackTableModel.addRow(values);
        }           
        for (Server server:availableServers.values()) {
            String mirrors="";
            ArrayList<String> mirrorsites=server.getMirrorSites();
            if (mirrorsites!=null) {
              for (int i=0;i<mirrorsites.size();i++) {
                if (i<mirrorsites.size()-1) mirrors+=mirrorsites.get(i)+",";
                else mirrors+=mirrorsites.get(i);
              }
            }
            Object[] values=new Object[]{server.getServerAddress(), new Integer(server.getServerDelay()), new Integer(server.getMaxSequenceLength()), mirrors};
            serverTableModel.addRow(values);
        }          
    }
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        cardpaneTopMenu = new javax.swing.JPanel();
        cardpaneTopMenuTopPanel = new javax.swing.JPanel();
        cardpaneTopMenuMainPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        configureDatatracksButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        configureServerSettingsButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        importSettingsButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        exportSettingsButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        revertSettingsButton = new javax.swing.JButton();
        cardpaneTopMenuButtonPanel = new javax.swing.JPanel();
        saveChangesButton = new javax.swing.JButton();
        exitWithoutSaveButton = new javax.swing.JButton();
        cardpaneAllTracks = new javax.swing.JPanel();
        cardpaneAllTracksMainPanel = new javax.swing.JPanel();
        cardpaneAllTracksScrollPane = new javax.swing.JScrollPane();
        jPanel10 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        datatracksLabel = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        trackFilterTextField = new javax.swing.JTextField();
        cardpaneAllTracksTopPanel = new javax.swing.JPanel();
        cardpaneAllTracksButtonPanel = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        allTracksAddButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        addTrackSourceCombobox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        allTracksEditButton = new javax.swing.JButton();
        allTracksRemoveButton = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        allTracksOKButton = new javax.swing.JButton();
        cardpaneEditTrack = new javax.swing.JPanel();
        cardpaneEditTrackTopPanel = new javax.swing.JPanel();
        editTrackPropertiesPanel = new javax.swing.JPanel();
        dataTrackPropertiesNameLabel = new javax.swing.JLabel();
        dataTrackPropertiesNameTextfield = new javax.swing.JTextField();
        dataTrackPropertiesSourceLabel = new javax.swing.JLabel();
        dataTrackPropertiesSourceTextfield = new javax.swing.JTextField();
        dataTrackPropertiesDescriptionLabel = new javax.swing.JLabel();
        dataTrackPropertiesDescriptionTextfield = new javax.swing.JTextField();
        dataTrackPropertiesTypeLabel = new javax.swing.JLabel();
        dataTrackPropertiesTypeCombobox = new javax.swing.JComboBox();
        dataTrackPropertiesDisplaySettingsLabel = new javax.swing.JLabel();
        dataTrackPropertiesDisplaySettingsTextfield = new javax.swing.JTextField();
        cardpaneEditTrackMainPanel = new javax.swing.JPanel();
        cardpaneEditTracksScrollPane = new javax.swing.JScrollPane();
        datasourcesLabel = new javax.swing.JLabel();
        cardpaneEditTrackButtonPanel = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        editTrackOKButton = new javax.swing.JButton();
        editTrackCancelButton = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        editTrackAddButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        editTrackAddSourceCombobox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        editTrackEditButton = new javax.swing.JButton();
        editTrackRemoveButton = new javax.swing.JButton();
        cardpaneEditSource = new javax.swing.JPanel();
        cardpaneEditSourceTopPanel = new javax.swing.JPanel();
        editSourcePropertiesPanel = new javax.swing.JPanel();
        editSourceTrackNameLabel = new javax.swing.JLabel();
        editSourceTrackNameTextfield = new javax.swing.JTextField();
        editSourceOrganismLabel = new javax.swing.JLabel();
        editSourceOrganismCombobox = new javax.swing.JComboBox();
        editSourceBuildLabel = new javax.swing.JLabel();
        editSourceBuildCombobox = new javax.swing.JComboBox();
        editSourceProtocoLabel = new javax.swing.JLabel();
        editSourceProtocolCombobox = new javax.swing.JComboBox();
        editSourceDataFormatLabel = new javax.swing.JLabel();
        editSourceDataFormatCombobox = new javax.swing.JComboBox();
        protocolHelpButton = new javax.swing.JButton();
        cardpaneEditSourceMainCard = new javax.swing.JPanel();
        internalWrapper1 = new javax.swing.JPanel();
        cardpaneEditSourceMainCardPanel = new javax.swing.JPanel();
        editGETsourcePanel = new javax.swing.JPanel();
        editGETsourcePanelInner = new javax.swing.JPanel();
        editGETsourceBaseURLlabel = new javax.swing.JLabel();
        editGETsourceBaseURLtextfield = new javax.swing.JTextField();
        editGETsourceParametersLabel = new javax.swing.JLabel();
        editGETsourceParametersTextfield = new javax.swing.JTextField();
        DataFormatSettingsTopPanel = new javax.swing.JPanel();
        additionalDataFormatSettingsPanel_GET = new javax.swing.JPanel();
        editDASsourcePanel = new javax.swing.JPanel();
        editDASsourcePanelInner = new javax.swing.JPanel();
        editDASsourceBaseURLlabel = new javax.swing.JLabel();
        editDASsourceBaseURLtextfield = new javax.swing.JTextField();
        editDASsourceFeatureLabel = new javax.swing.JLabel();
        editDASsourceFeatureTextfield = new javax.swing.JTextField();
        editFILEsourcePanel = new javax.swing.JPanel();
        editFILEsourcePanelInner = new javax.swing.JPanel();
        editFILEsourceFilenameLabel = new javax.swing.JLabel();
        editFILEsourceFilenameTextfield = new javax.swing.JTextField();
        editFILEsourceSegmentsizeLabel = new javax.swing.JLabel();
        editFILEsourceSegmentsizeSpinner = new javax.swing.JSpinner();
        editFILEsourceBrowseButton = new javax.swing.JButton();
        DataFormatSettingsTopPanel_FILE = new javax.swing.JPanel();
        additionalDataFormatSettingsPanel_FILE = new javax.swing.JPanel();
        editSQLsourcePanel = new javax.swing.JPanel();
        editSQLsourceTopPanelOuter = new javax.swing.JPanel();
        editSQLsourceTopPanel = new javax.swing.JPanel();
        editSQLsourceServerURLField = new javax.swing.JTextField();
        editSQLsourceServerURLLabel = new javax.swing.JLabel();
        editSQLsourceDatabaseNameLabel = new javax.swing.JLabel();
        editSQLsourceDatabaseNameField = new javax.swing.JTextField();
        editSQLsourcePortLabel = new javax.swing.JLabel();
        editSQLsourcePortField = new javax.swing.JTextField();
        editSQLsourceUsernameLabel = new javax.swing.JLabel();
        editSQLsourceUsernameField = new javax.swing.JTextField();
        editSQLsourcePasswordLabel = new javax.swing.JLabel();
        editSQLsourcePasswordField = new javax.swing.JPasswordField();
        editSQLsourceTableNameLabel = new javax.swing.JLabel();
        editSQLsourceTableNameField = new javax.swing.JTextField();
        editSQLsourceTablePanel = new javax.swing.JPanel();
        editSQLsourceTableMainPanel = new javax.swing.JPanel();
        editSQLsourceTableScrollPane = new javax.swing.JScrollPane();
        editSQLsourceTableControlsPanel = new javax.swing.JPanel();
        editSQLsourceAddPropertyButton = new javax.swing.JButton();
        editSQLsourceRemovePropertyButton = new javax.swing.JButton();
        editVOIDsourcePanel = new javax.swing.JPanel();
        cardpaneEditSourceButtonsPanel = new javax.swing.JPanel();
        editSourceOKButton = new javax.swing.JButton();
        editSourceCancelButton = new javax.swing.JButton();
        cardpaneServers = new javax.swing.JPanel();
        cardpaneServersTopPanel = new javax.swing.JPanel();
        cardpaneServersMainPanel = new javax.swing.JPanel();
        serversLabel = new javax.swing.JLabel();
        serversScrollPane = new javax.swing.JScrollPane();
        cardpaneServersButtonPanel = new javax.swing.JPanel();
        addServerButton = new javax.swing.JButton();
        serversOKButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DataTrackConfigurationDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N
        getContentPane().setLayout(new java.awt.CardLayout());

        cardpaneTopMenu.setName("cardpaneTopMenu"); // NOI18N
        cardpaneTopMenu.setLayout(new java.awt.BorderLayout());

        cardpaneTopMenuTopPanel.setName("cardpaneTopMenuTopPanel"); // NOI18N
        cardpaneTopMenuTopPanel.setPreferredSize(new java.awt.Dimension(100, 10));

        javax.swing.GroupLayout cardpaneTopMenuTopPanelLayout = new javax.swing.GroupLayout(cardpaneTopMenuTopPanel);
        cardpaneTopMenuTopPanel.setLayout(cardpaneTopMenuTopPanelLayout);
        cardpaneTopMenuTopPanelLayout.setHorizontalGroup(
            cardpaneTopMenuTopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 585, Short.MAX_VALUE)
        );
        cardpaneTopMenuTopPanelLayout.setVerticalGroup(
            cardpaneTopMenuTopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 10, Short.MAX_VALUE)
        );

        cardpaneTopMenu.add(cardpaneTopMenuTopPanel, java.awt.BorderLayout.PAGE_START);

        cardpaneTopMenuMainPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10), javax.swing.BorderFactory.createEtchedBorder()));
        cardpaneTopMenuMainPanel.setName("cardpaneTopMenuMainPanel"); // NOI18N
        cardpaneTopMenuMainPanel.setLayout(new javax.swing.BoxLayout(cardpaneTopMenuMainPanel, javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 25));

        configureDatatracksButton.setText(resourceMap.getString("configureDatatracksButton.text")); // NOI18N
        configureDatatracksButton.setMaximumSize(new java.awt.Dimension(250, 31));
        configureDatatracksButton.setMinimumSize(new java.awt.Dimension(250, 31));
        configureDatatracksButton.setName("configureDatatracksButton"); // NOI18N
        configureDatatracksButton.setPreferredSize(new java.awt.Dimension(250, 31));
        configureDatatracksButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                configureDatatracksButtonClicked(evt);
            }
        });
        jPanel1.add(configureDatatracksButton);

        cardpaneTopMenuMainPanel.add(jPanel1);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 25));

        configureServerSettingsButton.setText(resourceMap.getString("configureServerSettingsButton.text")); // NOI18N
        configureServerSettingsButton.setMaximumSize(new java.awt.Dimension(250, 31));
        configureServerSettingsButton.setMinimumSize(new java.awt.Dimension(250, 31));
        configureServerSettingsButton.setName("configureServerSettingsButton"); // NOI18N
        configureServerSettingsButton.setPreferredSize(new java.awt.Dimension(250, 31));
        configureServerSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                configureServersSettingsButtonClicked(evt);
            }
        });
        jPanel2.add(configureServerSettingsButton);

        cardpaneTopMenuMainPanel.add(jPanel2);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 25));

        importSettingsButton.setText(resourceMap.getString("importSettingsButton.text")); // NOI18N
        importSettingsButton.setMaximumSize(new java.awt.Dimension(250, 31));
        importSettingsButton.setMinimumSize(new java.awt.Dimension(250, 31));
        importSettingsButton.setName("importSettingsButton"); // NOI18N
        importSettingsButton.setPreferredSize(new java.awt.Dimension(250, 31));
        importSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                importSettings(evt);
            }
        });
        jPanel3.add(importSettingsButton);

        cardpaneTopMenuMainPanel.add(jPanel3);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 25));

        exportSettingsButton.setText(resourceMap.getString("exportSettingsButton.text")); // NOI18N
        exportSettingsButton.setMaximumSize(new java.awt.Dimension(250, 31));
        exportSettingsButton.setMinimumSize(new java.awt.Dimension(250, 31));
        exportSettingsButton.setName("exportSettingsButton"); // NOI18N
        exportSettingsButton.setPreferredSize(new java.awt.Dimension(250, 31));
        exportSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                exportSettings(evt);
            }
        });
        jPanel4.add(exportSettingsButton);

        cardpaneTopMenuMainPanel.add(jPanel4);

        jPanel5.setMinimumSize(new java.awt.Dimension(235, 41));
        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setPreferredSize(new java.awt.Dimension(235, 91));
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 25));

        revertSettingsButton.setText(resourceMap.getString("revertSettingsButton.text")); // NOI18N
        revertSettingsButton.setMaximumSize(new java.awt.Dimension(250, 31));
        revertSettingsButton.setMinimumSize(new java.awt.Dimension(250, 31));
        revertSettingsButton.setName("revertSettingsButton"); // NOI18N
        revertSettingsButton.setPreferredSize(new java.awt.Dimension(250, 31));
        revertSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                revertSettings(evt);
            }
        });
        jPanel5.add(revertSettingsButton);

        cardpaneTopMenuMainPanel.add(jPanel5);

        cardpaneTopMenu.add(cardpaneTopMenuMainPanel, java.awt.BorderLayout.CENTER);

        cardpaneTopMenuButtonPanel.setMinimumSize(new java.awt.Dimension(0, 46));
        cardpaneTopMenuButtonPanel.setName("cardpaneTopMenuButtonPanel"); // NOI18N
        cardpaneTopMenuButtonPanel.setPreferredSize(new java.awt.Dimension(100, 46));
        cardpaneTopMenuButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 25, 5));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(DataTrackConfigurationDialog.class, this);
        saveChangesButton.setAction(actionMap.get("saveChangesAndExitAction")); // NOI18N
        saveChangesButton.setMaximumSize(new java.awt.Dimension(190, 29));
        saveChangesButton.setMinimumSize(new java.awt.Dimension(190, 29));
        saveChangesButton.setName("saveChangesButton"); // NOI18N
        saveChangesButton.setPreferredSize(new java.awt.Dimension(190, 29));
        cardpaneTopMenuButtonPanel.add(saveChangesButton);

        exitWithoutSaveButton.setText(resourceMap.getString("exitWithoutSaveButton.text")); // NOI18N
        exitWithoutSaveButton.setMaximumSize(new java.awt.Dimension(170, 29));
        exitWithoutSaveButton.setMinimumSize(new java.awt.Dimension(170, 29));
        exitWithoutSaveButton.setName("exitWithoutSaveButton"); // NOI18N
        exitWithoutSaveButton.setPreferredSize(new java.awt.Dimension(170, 29));
        exitWithoutSaveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                exitWithoutSaving(evt);
            }
        });
        cardpaneTopMenuButtonPanel.add(exitWithoutSaveButton);

        cardpaneTopMenu.add(cardpaneTopMenuButtonPanel, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(cardpaneTopMenu, "cardpaneTopMenu");

        cardpaneAllTracks.setName("cardpaneAllTracks"); // NOI18N
        cardpaneAllTracks.setLayout(new java.awt.BorderLayout());

        cardpaneAllTracksMainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        cardpaneAllTracksMainPanel.setName("cardpaneAllTracksMainPanel"); // NOI18N
        cardpaneAllTracksMainPanel.setLayout(new java.awt.BorderLayout());

        cardpaneAllTracksScrollPane.setName("cardpaneAllTracksScrollPane"); // NOI18N
        cardpaneAllTracksMainPanel.add(cardpaneAllTracksScrollPane, java.awt.BorderLayout.CENTER);

        jPanel10.setName("jPanel10"); // NOI18N
        jPanel10.setLayout(new java.awt.BorderLayout());

        jPanel11.setName("jPanel11"); // NOI18N
        jPanel11.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 2));

        datatracksLabel.setFont(resourceMap.getFont("datatracksLabel.font")); // NOI18N
        datatracksLabel.setText(resourceMap.getString("datatracksLabel.text")); // NOI18N
        datatracksLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 5, 1));
        datatracksLabel.setName("datatracksLabel"); // NOI18N
        jPanel11.add(datatracksLabel);

        jPanel10.add(jPanel11, java.awt.BorderLayout.WEST);

        jPanel12.setName("jPanel12"); // NOI18N
        jPanel12.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 2));

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jPanel12.add(jLabel5);

        trackFilterTextField.setColumns(18);
        trackFilterTextField.setText(resourceMap.getString("trackFilterTextField.text")); // NOI18N
        trackFilterTextField.setName("trackFilterTextField"); // NOI18N
        trackFilterTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                keyReleasedInFilterTextfield(evt);
            }
        });
        jPanel12.add(trackFilterTextField);

        jPanel10.add(jPanel12, java.awt.BorderLayout.EAST);

        cardpaneAllTracksMainPanel.add(jPanel10, java.awt.BorderLayout.NORTH);

        cardpaneAllTracks.add(cardpaneAllTracksMainPanel, java.awt.BorderLayout.CENTER);

        cardpaneAllTracksTopPanel.setMaximumSize(new java.awt.Dimension(32767, 40));
        cardpaneAllTracksTopPanel.setName("cardpaneAllTracksTopPanel"); // NOI18N
        cardpaneAllTracksTopPanel.setPreferredSize(new java.awt.Dimension(100, 4));

        javax.swing.GroupLayout cardpaneAllTracksTopPanelLayout = new javax.swing.GroupLayout(cardpaneAllTracksTopPanel);
        cardpaneAllTracksTopPanel.setLayout(cardpaneAllTracksTopPanelLayout);
        cardpaneAllTracksTopPanelLayout.setHorizontalGroup(
            cardpaneAllTracksTopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 585, Short.MAX_VALUE)
        );
        cardpaneAllTracksTopPanelLayout.setVerticalGroup(
            cardpaneAllTracksTopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );

        cardpaneAllTracks.add(cardpaneAllTracksTopPanel, java.awt.BorderLayout.PAGE_START);

        cardpaneAllTracksButtonPanel.setMaximumSize(new java.awt.Dimension(32767, 90));
        cardpaneAllTracksButtonPanel.setName("cardpaneAllTracksButtonPanel"); // NOI18N
        cardpaneAllTracksButtonPanel.setLayout(new java.awt.BorderLayout());

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        allTracksAddButton.setAction(actionMap.get("allTrackAddButtonClicked")); // NOI18N
        allTracksAddButton.setText(resourceMap.getString("allTracksAddButton.text")); // NOI18N
        allTracksAddButton.setMaximumSize(null);
        allTracksAddButton.setMinimumSize(null);
        allTracksAddButton.setName("allTracksAddButton"); // NOI18N
        allTracksAddButton.setPreferredSize(null);
        jPanel6.add(allTracksAddButton);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel6.add(jLabel1);

        addTrackSourceCombobox.setModel(getAddDataTrackOptions());
        addTrackSourceCombobox.setName("addTrackSourceCombobox"); // NOI18N
        jPanel6.add(addTrackSourceCombobox);

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel6.add(jLabel2);

        allTracksEditButton.setAction(actionMap.get("allTracksEditButtonClicked")); // NOI18N
        allTracksEditButton.setText(resourceMap.getString("allTracksEditButton.text")); // NOI18N
        allTracksEditButton.setMaximumSize(null);
        allTracksEditButton.setMinimumSize(null);
        allTracksEditButton.setName("allTracksEditButton"); // NOI18N
        allTracksEditButton.setPreferredSize(null);
        jPanel6.add(allTracksEditButton);

        allTracksRemoveButton.setAction(actionMap.get("allTracksRemoveButtonClicked")); // NOI18N
        allTracksRemoveButton.setText(resourceMap.getString("allTracksRemoveButton.text")); // NOI18N
        allTracksRemoveButton.setMaximumSize(null);
        allTracksRemoveButton.setMinimumSize(null);
        allTracksRemoveButton.setName("allTracksRemoveButton"); // NOI18N
        allTracksRemoveButton.setPreferredSize(null);
        jPanel6.add(allTracksRemoveButton);

        cardpaneAllTracksButtonPanel.add(jPanel6, java.awt.BorderLayout.WEST);

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        allTracksOKButton.setAction(actionMap.get("allTracksOKButtonClickedAction")); // NOI18N
        allTracksOKButton.setText(resourceMap.getString("allTracksOKButton.text")); // NOI18N
        allTracksOKButton.setMaximumSize(null);
        allTracksOKButton.setMinimumSize(null);
        allTracksOKButton.setName("allTracksOKButton"); // NOI18N
        allTracksOKButton.setPreferredSize(null);
        jPanel7.add(allTracksOKButton);

        cardpaneAllTracksButtonPanel.add(jPanel7, java.awt.BorderLayout.EAST);

        cardpaneAllTracks.add(cardpaneAllTracksButtonPanel, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(cardpaneAllTracks, "cardpaneAllTracks");

        cardpaneEditTrack.setName("cardpaneEditTrack"); // NOI18N
        cardpaneEditTrack.setLayout(new java.awt.BorderLayout());

        cardpaneEditTrackTopPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        cardpaneEditTrackTopPanel.setMinimumSize(new java.awt.Dimension(20, 150));
        cardpaneEditTrackTopPanel.setName("cardpaneEditTrackTopPanel"); // NOI18N
        cardpaneEditTrackTopPanel.setPreferredSize(new java.awt.Dimension(100, 186));
        cardpaneEditTrackTopPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        editTrackPropertiesPanel.setMinimumSize(new java.awt.Dimension(400, 20));
        editTrackPropertiesPanel.setName("editTrackPropertiesPanel"); // NOI18N
        editTrackPropertiesPanel.setLayout(new java.awt.GridBagLayout());

        dataTrackPropertiesNameLabel.setText(resourceMap.getString("dataTrackPropertiesNameLabel.text")); // NOI18N
        dataTrackPropertiesNameLabel.setName("dataTrackPropertiesNameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesNameLabel, gridBagConstraints);

        dataTrackPropertiesNameTextfield.setColumns(20);
        dataTrackPropertiesNameTextfield.setText(resourceMap.getString("dataTrackPropertiesNameTextfield.text")); // NOI18N
        dataTrackPropertiesNameTextfield.setName("dataTrackPropertiesNameTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesNameTextfield, gridBagConstraints);

        dataTrackPropertiesSourceLabel.setText(resourceMap.getString("dataTrackPropertiesSourceLabel.text")); // NOI18N
        dataTrackPropertiesSourceLabel.setName("dataTrackPropertiesSourceLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesSourceLabel, gridBagConstraints);

        dataTrackPropertiesSourceTextfield.setColumns(20);
        dataTrackPropertiesSourceTextfield.setText(resourceMap.getString("dataTrackPropertiesSourceTextfield.text")); // NOI18N
        dataTrackPropertiesSourceTextfield.setName("dataTrackPropertiesSourceTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesSourceTextfield, gridBagConstraints);

        dataTrackPropertiesDescriptionLabel.setText(resourceMap.getString("dataTrackPropertiesDescriptionLabel.text")); // NOI18N
        dataTrackPropertiesDescriptionLabel.setName("dataTrackPropertiesDescriptionLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesDescriptionLabel, gridBagConstraints);

        dataTrackPropertiesDescriptionTextfield.setColumns(50);
        dataTrackPropertiesDescriptionTextfield.setText(resourceMap.getString("dataTrackPropertiesDescriptionTextfield.text")); // NOI18N
        dataTrackPropertiesDescriptionTextfield.setName("dataTrackPropertiesDescriptionTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesDescriptionTextfield, gridBagConstraints);

        dataTrackPropertiesTypeLabel.setText(resourceMap.getString("dataTrackPropertiesTypeLabel.text")); // NOI18N
        dataTrackPropertiesTypeLabel.setName("dataTrackPropertiesTypeLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesTypeLabel, gridBagConstraints);

        dataTrackPropertiesTypeCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Sequence", "Region", "Numeric" }));
        dataTrackPropertiesTypeCombobox.setName("dataTrackPropertiesTypeCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesTypeCombobox, gridBagConstraints);

        dataTrackPropertiesDisplaySettingsLabel.setText(resourceMap.getString("dataTrackPropertiesDisplaySettingsLabel.text")); // NOI18N
        dataTrackPropertiesDisplaySettingsLabel.setName("dataTrackPropertiesDisplaySettingsLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesDisplaySettingsLabel, gridBagConstraints);

        dataTrackPropertiesDisplaySettingsTextfield.setColumns(40);
        dataTrackPropertiesDisplaySettingsTextfield.setText(resourceMap.getString("dataTrackPropertiesDisplaySettingsTextfield.text")); // NOI18N
        dataTrackPropertiesDisplaySettingsTextfield.setName("dataTrackPropertiesDisplaySettingsTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editTrackPropertiesPanel.add(dataTrackPropertiesDisplaySettingsTextfield, gridBagConstraints);

        cardpaneEditTrackTopPanel.add(editTrackPropertiesPanel);

        cardpaneEditTrack.add(cardpaneEditTrackTopPanel, java.awt.BorderLayout.PAGE_START);

        cardpaneEditTrackMainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        cardpaneEditTrackMainPanel.setName("cardpaneEditTrackMainPanel"); // NOI18N
        cardpaneEditTrackMainPanel.setLayout(new java.awt.BorderLayout());

        cardpaneEditTracksScrollPane.setName("cardpaneEditTracksScrollPane"); // NOI18N
        cardpaneEditTracksScrollPane.setPreferredSize(new java.awt.Dimension(20, 100));
        cardpaneEditTrackMainPanel.add(cardpaneEditTracksScrollPane, java.awt.BorderLayout.CENTER);

        datasourcesLabel.setFont(resourceMap.getFont("datasourcesLabel.font")); // NOI18N
        datasourcesLabel.setIcon(resourceMap.getIcon("datasourcesLabel.icon")); // NOI18N
        datasourcesLabel.setText(resourceMap.getString("datasourcesLabel.text")); // NOI18N
        datasourcesLabel.setToolTipText(resourceMap.getString("datasourcesLabel.toolTipText")); // NOI18N
        datasourcesLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 1, 5, 1));
        datasourcesLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        datasourcesLabel.setName("datasourcesLabel"); // NOI18N
        cardpaneEditTrackMainPanel.add(datasourcesLabel, java.awt.BorderLayout.PAGE_START);

        cardpaneEditTrack.add(cardpaneEditTrackMainPanel, java.awt.BorderLayout.CENTER);

        cardpaneEditTrackButtonPanel.setName("cardpaneEditTrackButtonPanel"); // NOI18N
        cardpaneEditTrackButtonPanel.setLayout(new java.awt.BorderLayout());

        jPanel8.setName("jPanel8"); // NOI18N
        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        editTrackOKButton.setAction(actionMap.get("editTrackOKButtonClickedAction")); // NOI18N
        editTrackOKButton.setText(resourceMap.getString("editTrackOKButton.text")); // NOI18N
        editTrackOKButton.setMaximumSize(null);
        editTrackOKButton.setMinimumSize(null);
        editTrackOKButton.setName("editTrackOKButton"); // NOI18N
        editTrackOKButton.setPreferredSize(null);
        jPanel8.add(editTrackOKButton);

        editTrackCancelButton.setText(resourceMap.getString("editTrackCancelButton.text")); // NOI18N
        editTrackCancelButton.setMaximumSize(null);
        editTrackCancelButton.setMinimumSize(null);
        editTrackCancelButton.setName("editTrackCancelButton"); // NOI18N
        editTrackCancelButton.setPreferredSize(null);
        editTrackCancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                editTrackCancelButtonPressed(evt);
            }
        });
        jPanel8.add(editTrackCancelButton);

        cardpaneEditTrackButtonPanel.add(jPanel8, java.awt.BorderLayout.EAST);

        jPanel9.setName("jPanel9"); // NOI18N
        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        editTrackAddButton.setAction(actionMap.get("addDataSource")); // NOI18N
        editTrackAddButton.setText(resourceMap.getString("editTrackAddButton.text")); // NOI18N
        editTrackAddButton.setMaximumSize(null);
        editTrackAddButton.setMinimumSize(null);
        editTrackAddButton.setName("editTrackAddButton"); // NOI18N
        editTrackAddButton.setPreferredSize(null);
        jPanel9.add(editTrackAddButton);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jPanel9.add(jLabel3);

        editTrackAddSourceCombobox.setModel(getAddDataSourceOptions());
        editTrackAddSourceCombobox.setName("editTrackAddSourceCombobox"); // NOI18N
        jPanel9.add(editTrackAddSourceCombobox);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        jPanel9.add(jLabel4);

        editTrackEditButton.setAction(actionMap.get("editDataSource")); // NOI18N
        editTrackEditButton.setText(resourceMap.getString("editTrackEditButton.text")); // NOI18N
        editTrackEditButton.setMaximumSize(null);
        editTrackEditButton.setMinimumSize(null);
        editTrackEditButton.setName("editTrackEditButton"); // NOI18N
        editTrackEditButton.setOpaque(false);
        editTrackEditButton.setPreferredSize(null);
        jPanel9.add(editTrackEditButton);

        editTrackRemoveButton.setAction(actionMap.get("removeDataSource")); // NOI18N
        editTrackRemoveButton.setText(resourceMap.getString("editTrackRemoveButton.text")); // NOI18N
        editTrackRemoveButton.setMaximumSize(null);
        editTrackRemoveButton.setMinimumSize(null);
        editTrackRemoveButton.setName("editTrackRemoveButton"); // NOI18N
        editTrackRemoveButton.setPreferredSize(null);
        jPanel9.add(editTrackRemoveButton);

        cardpaneEditTrackButtonPanel.add(jPanel9, java.awt.BorderLayout.CENTER);

        cardpaneEditTrack.add(cardpaneEditTrackButtonPanel, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(cardpaneEditTrack, "cardpaneEditTrack");

        cardpaneEditSource.setName("cardpaneEditSource"); // NOI18N
        cardpaneEditSource.setLayout(new java.awt.BorderLayout());

        cardpaneEditSourceTopPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        cardpaneEditSourceTopPanel.setName("cardpaneEditSourceTopPanel"); // NOI18N
        cardpaneEditSourceTopPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        editSourcePropertiesPanel.setName("editSourcePropertiesPanel"); // NOI18N
        editSourcePropertiesPanel.setLayout(new java.awt.GridBagLayout());

        editSourceTrackNameLabel.setText(resourceMap.getString("editSourceTrackNameLabel.text")); // NOI18N
        editSourceTrackNameLabel.setName("editSourceTrackNameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editSourcePropertiesPanel.add(editSourceTrackNameLabel, gridBagConstraints);

        editSourceTrackNameTextfield.setColumns(20);
        editSourceTrackNameTextfield.setEditable(false);
        editSourceTrackNameTextfield.setText(resourceMap.getString("editSourceTrackNameTextfield.text")); // NOI18N
        editSourceTrackNameTextfield.setName("editSourceTrackNameTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editSourcePropertiesPanel.add(editSourceTrackNameTextfield, gridBagConstraints);

        editSourceOrganismLabel.setText(resourceMap.getString("editSourceOrganismLabel.text")); // NOI18N
        editSourceOrganismLabel.setName("editSourceOrganismLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editSourcePropertiesPanel.add(editSourceOrganismLabel, gridBagConstraints);

        editSourceOrganismCombobox.setName("editSourceOrganismCombobox"); // NOI18N
        editSourceOrganismCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sourceOrganismSelected(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editSourcePropertiesPanel.add(editSourceOrganismCombobox, gridBagConstraints);

        editSourceBuildLabel.setText(resourceMap.getString("editSourceBuildLabel.text")); // NOI18N
        editSourceBuildLabel.setName("editSourceBuildLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(11, 24, 11, 0);
        editSourcePropertiesPanel.add(editSourceBuildLabel, gridBagConstraints);

        editSourceBuildCombobox.setEditable(true);
        editSourceBuildCombobox.setModel(new DefaultComboBoxModel<String>(new String[0]));
        editSourceBuildCombobox.setName("editSourceBuildCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editSourcePropertiesPanel.add(editSourceBuildCombobox, gridBagConstraints);

        editSourceProtocoLabel.setText(resourceMap.getString("editSourceProtocoLabel.text")); // NOI18N
        editSourceProtocoLabel.setName("editSourceProtocoLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editSourcePropertiesPanel.add(editSourceProtocoLabel, gridBagConstraints);

        editSourceProtocolCombobox.setName("editSourceProtocolCombobox"); // NOI18N
        editSourceProtocolCombobox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                editSourceProtocolChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editSourcePropertiesPanel.add(editSourceProtocolCombobox, gridBagConstraints);

        editSourceDataFormatLabel.setText(resourceMap.getString("editSourceDataFormatLabel.text")); // NOI18N
        editSourceDataFormatLabel.setName("editSourceDataFormatLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 24, 0, 0);
        editSourcePropertiesPanel.add(editSourceDataFormatLabel, gridBagConstraints);

        editSourceDataFormatCombobox.setName("editSourceDataFormatCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editSourcePropertiesPanel.add(editSourceDataFormatCombobox, gridBagConstraints);

        protocolHelpButton.setIcon(resourceMap.getIcon("protocolHelpButton.icon")); // NOI18N
        protocolHelpButton.setText(resourceMap.getString("protocolHelpButton.text")); // NOI18N
        protocolHelpButton.setName("protocolHelpButton"); // NOI18N
        protocolHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolHelpButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        editSourcePropertiesPanel.add(protocolHelpButton, gridBagConstraints);

        cardpaneEditSourceTopPanel.add(editSourcePropertiesPanel);

        cardpaneEditSource.add(cardpaneEditSourceTopPanel, java.awt.BorderLayout.PAGE_START);

        cardpaneEditSourceMainCard.setName("cardpaneEditSourceMainCard"); // NOI18N
        cardpaneEditSourceMainCard.setLayout(new java.awt.BorderLayout());

        internalWrapper1.setName("internalWrapper1"); // NOI18N
        internalWrapper1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        cardpaneEditSourceMainCardPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        cardpaneEditSourceMainCardPanel.setName("cardpaneEditSourceMainCardPanel"); // NOI18N
        cardpaneEditSourceMainCardPanel.setLayout(new java.awt.CardLayout());

        editGETsourcePanel.setName("editGETsourcePanel"); // NOI18N
        editGETsourcePanel.setLayout(new java.awt.BorderLayout());

        editGETsourcePanelInner.setName("editGETsourcePanelInner"); // NOI18N
        editGETsourcePanelInner.setLayout(new java.awt.GridBagLayout());

        editGETsourceBaseURLlabel.setText(resourceMap.getString("editGETsourceBaseURLlabel.text")); // NOI18N
        editGETsourceBaseURLlabel.setName("editGETsourceBaseURLlabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editGETsourcePanelInner.add(editGETsourceBaseURLlabel, gridBagConstraints);

        editGETsourceBaseURLtextfield.setColumns(60);
        editGETsourceBaseURLtextfield.setText(resourceMap.getString("editGETsourceBaseURLtextfield.text")); // NOI18N
        editGETsourceBaseURLtextfield.setName("editGETsourceBaseURLtextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editGETsourcePanelInner.add(editGETsourceBaseURLtextfield, gridBagConstraints);

        editGETsourceParametersLabel.setText(resourceMap.getString("editGETsourceParametersLabel.text")); // NOI18N
        editGETsourceParametersLabel.setName("editGETsourceParametersLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editGETsourcePanelInner.add(editGETsourceParametersLabel, gridBagConstraints);

        editGETsourceParametersTextfield.setColumns(60);
        editGETsourceParametersTextfield.setText(resourceMap.getString("editGETsourceParametersTextfield.text")); // NOI18N
        editGETsourceParametersTextfield.setName("editGETsourceParametersTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editGETsourcePanelInner.add(editGETsourceParametersTextfield, gridBagConstraints);

        editGETsourcePanel.add(editGETsourcePanelInner, java.awt.BorderLayout.NORTH);

        DataFormatSettingsTopPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 8, 5, 10));
        DataFormatSettingsTopPanel.setName("DataFormatSettingsTopPanel"); // NOI18N
        DataFormatSettingsTopPanel.setLayout(new java.awt.BorderLayout());

        additionalDataFormatSettingsPanel_GET.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("additionalDataFormatSettingsPanel_GET.border.title"))); // NOI18N
        additionalDataFormatSettingsPanel_GET.setName("additionalDataFormatSettingsPanel_GET"); // NOI18N
        additionalDataFormatSettingsPanel_GET.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        DataFormatSettingsTopPanel.add(additionalDataFormatSettingsPanel_GET, java.awt.BorderLayout.CENTER);

        editGETsourcePanel.add(DataFormatSettingsTopPanel, java.awt.BorderLayout.CENTER);

        cardpaneEditSourceMainCardPanel.add(editGETsourcePanel, "GETprotocol");

        editDASsourcePanel.setName("editDASsourcePanel"); // NOI18N
        editDASsourcePanel.setLayout(new java.awt.BorderLayout());

        editDASsourcePanelInner.setName("editDASsourcePanelInner"); // NOI18N
        editDASsourcePanelInner.setLayout(new java.awt.GridBagLayout());

        editDASsourceBaseURLlabel.setText(resourceMap.getString("editDASsourceBaseURLlabel.text")); // NOI18N
        editDASsourceBaseURLlabel.setName("editDASsourceBaseURLlabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editDASsourcePanelInner.add(editDASsourceBaseURLlabel, gridBagConstraints);

        editDASsourceBaseURLtextfield.setColumns(60);
        editDASsourceBaseURLtextfield.setText(resourceMap.getString("editDASsourceBaseURLtextfield.text")); // NOI18N
        editDASsourceBaseURLtextfield.setName("editDASsourceBaseURLtextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editDASsourcePanelInner.add(editDASsourceBaseURLtextfield, gridBagConstraints);

        editDASsourceFeatureLabel.setText(resourceMap.getString("editDASsourceFeatureLabel.text")); // NOI18N
        editDASsourceFeatureLabel.setName("editDASsourceFeatureLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editDASsourcePanelInner.add(editDASsourceFeatureLabel, gridBagConstraints);

        editDASsourceFeatureTextfield.setColumns(15);
        editDASsourceFeatureTextfield.setText(resourceMap.getString("editDASsourceFeatureTextfield.text")); // NOI18N
        editDASsourceFeatureTextfield.setName("editDASsourceFeatureTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editDASsourcePanelInner.add(editDASsourceFeatureTextfield, gridBagConstraints);

        editDASsourcePanel.add(editDASsourcePanelInner, java.awt.BorderLayout.PAGE_START);

        cardpaneEditSourceMainCardPanel.add(editDASsourcePanel, "DASprotocol");

        editFILEsourcePanel.setName("editFILEsourcePanel"); // NOI18N
        editFILEsourcePanel.setLayout(new java.awt.BorderLayout());

        editFILEsourcePanelInner.setName("editFILEsourcePanelInner"); // NOI18N
        editFILEsourcePanelInner.setLayout(new java.awt.GridBagLayout());

        editFILEsourceFilenameLabel.setText(resourceMap.getString("editFILEsourceFilenameLabel.text")); // NOI18N
        editFILEsourceFilenameLabel.setName("editFILEsourceFilenameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editFILEsourcePanelInner.add(editFILEsourceFilenameLabel, gridBagConstraints);

        editFILEsourceFilenameTextfield.setColumns(50);
        editFILEsourceFilenameTextfield.setText(resourceMap.getString("editFILEsourceFilenameTextfield.text")); // NOI18N
        editFILEsourceFilenameTextfield.setName("editFILEsourceFilenameTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editFILEsourcePanelInner.add(editFILEsourceFilenameTextfield, gridBagConstraints);

        editFILEsourceSegmentsizeLabel.setText(resourceMap.getString("editFILEsourceSegmentsizeLabel.text")); // NOI18N
        editFILEsourceSegmentsizeLabel.setName("editFILEsourceSegmentsizeLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        editFILEsourcePanelInner.add(editFILEsourceSegmentsizeLabel, gridBagConstraints);

        editFILEsourceSegmentsizeSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(200000), Integer.valueOf(1), null, Integer.valueOf(1)));
        editFILEsourceSegmentsizeSpinner.setMinimumSize(new java.awt.Dimension(100, 26));
        editFILEsourceSegmentsizeSpinner.setName("editFILEsourceSegmentsizeSpinner"); // NOI18N
        editFILEsourceSegmentsizeSpinner.setPreferredSize(new java.awt.Dimension(100, 26));
        editFILEsourceSegmentsizeSpinner.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        editFILEsourcePanelInner.add(editFILEsourceSegmentsizeSpinner, gridBagConstraints);

        editFILEsourceBrowseButton.setText(resourceMap.getString("editFILEsourceBrowseButton.text")); // NOI18N
        editFILEsourceBrowseButton.setName("editFILEsourceBrowseButton"); // NOI18N
        editFILEsourceBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileServerBrowseButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 0);
        editFILEsourcePanelInner.add(editFILEsourceBrowseButton, gridBagConstraints);

        editFILEsourcePanel.add(editFILEsourcePanelInner, java.awt.BorderLayout.PAGE_START);

        DataFormatSettingsTopPanel_FILE.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 8, 5, 10));
        DataFormatSettingsTopPanel_FILE.setName("DataFormatSettingsTopPanel_FILE"); // NOI18N
        DataFormatSettingsTopPanel_FILE.setLayout(new java.awt.BorderLayout());

        additionalDataFormatSettingsPanel_FILE.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("additionalDataFormatSettingsPanel_FILE.border.title"))); // NOI18N
        additionalDataFormatSettingsPanel_FILE.setName("additionalDataFormatSettingsPanel_FILE"); // NOI18N
        additionalDataFormatSettingsPanel_FILE.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        DataFormatSettingsTopPanel_FILE.add(additionalDataFormatSettingsPanel_FILE, java.awt.BorderLayout.CENTER);

        editFILEsourcePanel.add(DataFormatSettingsTopPanel_FILE, java.awt.BorderLayout.CENTER);

        cardpaneEditSourceMainCardPanel.add(editFILEsourcePanel, "FILEprotocol");

        editSQLsourcePanel.setName("editSQLsourcePanel"); // NOI18N
        editSQLsourcePanel.setLayout(new java.awt.BorderLayout());

        editSQLsourceTopPanelOuter.setName("editSQLsourceTopPanelOuter"); // NOI18N
        editSQLsourceTopPanelOuter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        editSQLsourceTopPanel.setName("editSQLsourceTopPanel"); // NOI18N
        editSQLsourceTopPanel.setLayout(new java.awt.GridBagLayout());

        editSQLsourceServerURLField.setColumns(16);
        editSQLsourceServerURLField.setText(resourceMap.getString("editSQLsourceServerURLField.text")); // NOI18N
        editSQLsourceServerURLField.setName("editSQLsourceServerURLField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        editSQLsourceTopPanel.add(editSQLsourceServerURLField, gridBagConstraints);

        editSQLsourceServerURLLabel.setText(resourceMap.getString("editSQLsourceServerURLLabel.text")); // NOI18N
        editSQLsourceServerURLLabel.setName("editSQLsourceServerURLLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 8);
        editSQLsourceTopPanel.add(editSQLsourceServerURLLabel, gridBagConstraints);

        editSQLsourceDatabaseNameLabel.setText(resourceMap.getString("editSQLsourceDatabaseNameLabel.text")); // NOI18N
        editSQLsourceDatabaseNameLabel.setName("editSQLsourceDatabaseNameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 16, 6, 8);
        editSQLsourceTopPanel.add(editSQLsourceDatabaseNameLabel, gridBagConstraints);

        editSQLsourceDatabaseNameField.setColumns(14);
        editSQLsourceDatabaseNameField.setText(resourceMap.getString("editSQLsourceDatabaseNameField.text")); // NOI18N
        editSQLsourceDatabaseNameField.setName("editSQLsourceDatabaseNameField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        editSQLsourceTopPanel.add(editSQLsourceDatabaseNameField, gridBagConstraints);

        editSQLsourcePortLabel.setText(resourceMap.getString("editSQLsourcePortLabel.text")); // NOI18N
        editSQLsourcePortLabel.setName("editSQLsourcePortLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 16, 6, 8);
        editSQLsourceTopPanel.add(editSQLsourcePortLabel, gridBagConstraints);

        editSQLsourcePortField.setColumns(5);
        editSQLsourcePortField.setText(resourceMap.getString("editSQLsourcePortField.text")); // NOI18N
        editSQLsourcePortField.setName("editSQLsourcePortField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        editSQLsourceTopPanel.add(editSQLsourcePortField, gridBagConstraints);

        editSQLsourceUsernameLabel.setText(resourceMap.getString("editSQLsourceUsernameLabel.text")); // NOI18N
        editSQLsourceUsernameLabel.setName("editSQLsourceUsernameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 8);
        editSQLsourceTopPanel.add(editSQLsourceUsernameLabel, gridBagConstraints);

        editSQLsourceUsernameField.setColumns(16);
        editSQLsourceUsernameField.setText(resourceMap.getString("editSQLsourceUsernameField.text")); // NOI18N
        editSQLsourceUsernameField.setName("editSQLsourceUsernameField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        editSQLsourceTopPanel.add(editSQLsourceUsernameField, gridBagConstraints);

        editSQLsourcePasswordLabel.setText(resourceMap.getString("editSQLsourcePasswordLabel.text")); // NOI18N
        editSQLsourcePasswordLabel.setName("editSQLsourcePasswordLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 16, 6, 8);
        editSQLsourceTopPanel.add(editSQLsourcePasswordLabel, gridBagConstraints);

        editSQLsourcePasswordField.setColumns(8);
        editSQLsourcePasswordField.setText(resourceMap.getString("editSQLsourcePasswordField.text")); // NOI18N
        editSQLsourcePasswordField.setName("editSQLsourcePasswordField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        editSQLsourceTopPanel.add(editSQLsourcePasswordField, gridBagConstraints);

        editSQLsourceTableNameLabel.setText(resourceMap.getString("editSQLsourceTableNameLabel.text")); // NOI18N
        editSQLsourceTableNameLabel.setName("editSQLsourceTableNameLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 16, 6, 8);
        editSQLsourceTopPanel.add(editSQLsourceTableNameLabel, gridBagConstraints);

        editSQLsourceTableNameField.setColumns(14);
        editSQLsourceTableNameField.setText(resourceMap.getString("editSQLsourceTableNameField.text")); // NOI18N
        editSQLsourceTableNameField.setName("editSQLsourceTableNameField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        editSQLsourceTopPanel.add(editSQLsourceTableNameField, gridBagConstraints);

        editSQLsourceTopPanelOuter.add(editSQLsourceTopPanel);

        editSQLsourcePanel.add(editSQLsourceTopPanelOuter, java.awt.BorderLayout.PAGE_START);

        editSQLsourceTablePanel.setName("editSQLsourceTablePanel"); // NOI18N
        editSQLsourceTablePanel.setLayout(new java.awt.BorderLayout());

        editSQLsourceTableMainPanel.setName("editSQLsourceTableMainPanel"); // NOI18N
        editSQLsourceTableMainPanel.setPreferredSize(new java.awt.Dimension(564, 200));
        editSQLsourceTableMainPanel.setLayout(new java.awt.BorderLayout());

        editSQLsourceTableScrollPane.setName("editSQLsourceTableScrollPane"); // NOI18N
        editSQLsourceTableScrollPane.setPreferredSize(new java.awt.Dimension(454, 200));
        editSQLsourceTableMainPanel.add(editSQLsourceTableScrollPane, java.awt.BorderLayout.CENTER);

        editSQLsourceTablePanel.add(editSQLsourceTableMainPanel, java.awt.BorderLayout.CENTER);

        editSQLsourceTableControlsPanel.setName("editSQLsourceTableControlsPanel"); // NOI18N
        editSQLsourceTableControlsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        editSQLsourceAddPropertyButton.setText(resourceMap.getString("editSQLsourceAddPropertyButton.text")); // NOI18N
        editSQLsourceAddPropertyButton.setName("editSQLsourceAddPropertyButton"); // NOI18N
        editSQLsourceTableControlsPanel.add(editSQLsourceAddPropertyButton);

        editSQLsourceRemovePropertyButton.setText(resourceMap.getString("editSQLsourceRemovePropertyButton.text")); // NOI18N
        editSQLsourceRemovePropertyButton.setEnabled(false);
        editSQLsourceRemovePropertyButton.setName("editSQLsourceRemovePropertyButton"); // NOI18N
        editSQLsourceTableControlsPanel.add(editSQLsourceRemovePropertyButton);

        editSQLsourceTablePanel.add(editSQLsourceTableControlsPanel, java.awt.BorderLayout.PAGE_END);

        editSQLsourcePanel.add(editSQLsourceTablePanel, java.awt.BorderLayout.CENTER);

        cardpaneEditSourceMainCardPanel.add(editSQLsourcePanel, "SQLprotocol");

        editVOIDsourcePanel.setName("editVOIDsourcePanel"); // NOI18N
        editVOIDsourcePanel.setLayout(new java.awt.BorderLayout());
        cardpaneEditSourceMainCardPanel.add(editVOIDsourcePanel, "VOIDprotocol");

        internalWrapper1.add(cardpaneEditSourceMainCardPanel);

        cardpaneEditSourceMainCard.add(internalWrapper1, java.awt.BorderLayout.NORTH);

        cardpaneEditSource.add(cardpaneEditSourceMainCard, java.awt.BorderLayout.CENTER);

        cardpaneEditSourceButtonsPanel.setMaximumSize(null);
        cardpaneEditSourceButtonsPanel.setName("cardpaneEditSourceButtonsPanel"); // NOI18N
        cardpaneEditSourceButtonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        editSourceOKButton.setAction(actionMap.get("editSourceOKButtonClickedAction")); // NOI18N
        editSourceOKButton.setText(resourceMap.getString("editSourceOKButton.text")); // NOI18N
        editSourceOKButton.setMaximumSize(null);
        editSourceOKButton.setMinimumSize(null);
        editSourceOKButton.setName("editSourceOKButton"); // NOI18N
        editSourceOKButton.setPreferredSize(null);
        cardpaneEditSourceButtonsPanel.add(editSourceOKButton);

        editSourceCancelButton.setAction(actionMap.get("editSourceCancelButtonClickedAction")); // NOI18N
        editSourceCancelButton.setText(resourceMap.getString("editSourceCancelButton.text")); // NOI18N
        editSourceCancelButton.setMaximumSize(null);
        editSourceCancelButton.setMinimumSize(null);
        editSourceCancelButton.setName("editSourceCancelButton"); // NOI18N
        editSourceCancelButton.setOpaque(false);
        editSourceCancelButton.setPreferredSize(null);
        cardpaneEditSourceButtonsPanel.add(editSourceCancelButton);

        cardpaneEditSource.add(cardpaneEditSourceButtonsPanel, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(cardpaneEditSource, "cardpaneEditSource");

        cardpaneServers.setName("cardpaneServers"); // NOI18N
        cardpaneServers.setLayout(new java.awt.BorderLayout());

        cardpaneServersTopPanel.setMaximumSize(new java.awt.Dimension(32767, 40));
        cardpaneServersTopPanel.setName("cardpaneServersTopPanel"); // NOI18N
        cardpaneServersTopPanel.setPreferredSize(new java.awt.Dimension(100, 10));
        cardpaneServersTopPanel.setRequestFocusEnabled(false);

        javax.swing.GroupLayout cardpaneServersTopPanelLayout = new javax.swing.GroupLayout(cardpaneServersTopPanel);
        cardpaneServersTopPanel.setLayout(cardpaneServersTopPanelLayout);
        cardpaneServersTopPanelLayout.setHorizontalGroup(
            cardpaneServersTopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 585, Short.MAX_VALUE)
        );
        cardpaneServersTopPanelLayout.setVerticalGroup(
            cardpaneServersTopPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 10, Short.MAX_VALUE)
        );

        cardpaneServers.add(cardpaneServersTopPanel, java.awt.BorderLayout.PAGE_START);

        cardpaneServersMainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        cardpaneServersMainPanel.setName("cardpaneServersMainPanel"); // NOI18N
        cardpaneServersMainPanel.setLayout(new java.awt.BorderLayout());

        serversLabel.setFont(resourceMap.getFont("serversLabel.font")); // NOI18N
        serversLabel.setIcon(resourceMap.getIcon("serversLabel.icon")); // NOI18N
        serversLabel.setText(resourceMap.getString("serversLabel.text")); // NOI18N
        serversLabel.setToolTipText(resourceMap.getString("serversLabel.toolTipText")); // NOI18N
        serversLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        serversLabel.setMaximumSize(new java.awt.Dimension(75, 23));
        serversLabel.setMinimumSize(new java.awt.Dimension(75, 23));
        serversLabel.setName("serversLabel"); // NOI18N
        serversLabel.setPreferredSize(new java.awt.Dimension(75, 23));
        cardpaneServersMainPanel.add(serversLabel, java.awt.BorderLayout.PAGE_START);

        serversScrollPane.setName("serversScrollPane"); // NOI18N
        cardpaneServersMainPanel.add(serversScrollPane, java.awt.BorderLayout.CENTER);

        cardpaneServers.add(cardpaneServersMainPanel, java.awt.BorderLayout.CENTER);

        cardpaneServersButtonPanel.setName("cardpaneServersButtonPanel"); // NOI18N
        cardpaneServersButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 5));

        addServerButton.setText(resourceMap.getString("addServerButton.text")); // NOI18N
        addServerButton.setMaximumSize(null);
        addServerButton.setMinimumSize(null);
        addServerButton.setName("addServerButton"); // NOI18N
        addServerButton.setPreferredSize(null);
        addServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addServerButtonPressed(evt);
            }
        });
        cardpaneServersButtonPanel.add(addServerButton);

        serversOKButton.setAction(actionMap.get("serversOKButtonClickedAction")); // NOI18N
        serversOKButton.setText(resourceMap.getString("serversOKButton.text")); // NOI18N
        serversOKButton.setMaximumSize(null);
        serversOKButton.setMinimumSize(null);
        serversOKButton.setName("serversOKButton"); // NOI18N
        serversOKButton.setOpaque(false);
        serversOKButton.setPreferredSize(null);
        cardpaneServersButtonPanel.add(serversOKButton);

        cardpaneServers.add(cardpaneServersButtonPanel, java.awt.BorderLayout.PAGE_END);

        getContentPane().add(cardpaneServers, "cardpaneServers");

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void exitWithoutSaving(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exitWithoutSaving
     
     setVisible(false);
}//GEN-LAST:event_exitWithoutSaving

private void configureDatatracksButtonClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_configureDatatracksButtonClicked
   showAllTracksPanel();
}//GEN-LAST:event_configureDatatracksButtonClicked

private void configureServersSettingsButtonClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_configureServersSettingsButtonClicked
    showServersPanel();
}//GEN-LAST:event_configureServersSettingsButtonClicked

private void importSettings(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_importSettings
        File file=null;//GEN-LAST:event_importSettings
 
      DataTrackConfigurationDialog_importDialog dialog=new DataTrackConfigurationDialog_importDialog(this, gui);
      dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
      dialog.setVisible(true);
      dialog.dispose();         
}

private void exportSettings(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportSettings
        File file=null;//GEN-LAST:event_exportSettings

      DataTrackConfigurationDialog_exportDialog dialog=new DataTrackConfigurationDialog_exportDialog(this, gui);
      dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
      dialog.setVisible(true);
      dialog.dispose();  
}

private void revertSettings(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_revertSettings
     int choice=JOptionPane.showConfirmDialog(this, "Do you really want to revert to default settings?\nThis will discard any alterations you have made to the configuration", "Revert Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
     if (choice==JOptionPane.CANCEL_OPTION) return;//GEN-LAST:event_revertSettings
     SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override 
            public Boolean doInBackground() {
                configureDatatracksButton.setEnabled(false);
                configureServerSettingsButton.setEnabled(false);
                importSettingsButton.setEnabled(false);
                exportSettingsButton.setEnabled(false);
                revertSettingsButton.setEnabled(false);
                exitWithoutSaveButton.setEnabled(false);
                saveChangesButton.setEnabled(false);
                try {
                    dataconfiguration.loadDefaultConfiguration();
                } catch (Exception e) {ex=e;}
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                configureDatatracksButton.setEnabled(true);
                configureServerSettingsButton.setEnabled(true);
                importSettingsButton.setEnabled(true);
                exportSettingsButton.setEnabled(true);
                revertSettingsButton.setEnabled(true);
                exitWithoutSaveButton.setEnabled(true);
                saveChangesButton.setEnabled(true);
                if (ex==null) {
                  availableTracks=dataconfiguration.getAvailableTracks();
                  availableServers=dataconfiguration.getServers();
                  setupTablesFromConfiguration();
                } else {
                    JOptionPane.showMessageDialog(gui.getFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }; // end of SwingWorker class
        worker.execute();      
}

private void editTrackCancelButtonPressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_editTrackCancelButtonPressed
       showAllTracksPanel();
}//GEN-LAST:event_editTrackCancelButtonPressed

private void editSourceProtocolChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_editSourceProtocolChanged
      // This method will be called twice in succession. First to DESELECT the currently selected item, and then again to SELECT the newly chosen item
      // I only need to respond to the second case and return immediately for the first case.
      if (evt.getStateChange()==java.awt.event.ItemEvent.DESELECTED) return; //    
  
      String protocol=(String)editSourceProtocolCombobox.getSelectedItem();  
      // The first five Data Source types here are included in the standard MotifLab distribution, but other types can be added as plugins
           if (protocol.equals(DataSource_DAS.PROTOCOL_NAME)) ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "DASprotocol");
      else if (protocol.equals(DataSource_http_GET.PROTOCOL_NAME)) ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "GETprotocol");
      else if (protocol.equals(DataSource_FileServer.PROTOCOL_NAME)) ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "FILEprotocol");
      else if (protocol.equals(DataSource_SQL.PROTOCOL_NAME)) ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "SQLprotocol");
      else if (protocol.equals(DataSource_VOID.PROTOCOL_NAME)) ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "VOIDprotocol");
      else { // plugin protocol
            JPanel datasourcepanel=null;
            Object resource=engine.getResource(protocol, "DataSource");
            if (resource instanceof DataSource) {
                if (currentDataSource==null || !((DataSource)resource).getClass().isAssignableFrom(currentDataSource.getClass())) {                 
                    try {
                        DataSource newDataSource=((DataSource)resource).getClass().newInstance();
                        int organism=Organism.HUMAN; // this default is replaced below with the value from the editor panel
                        Object organismObject=editSourceOrganismCombobox.getSelectedItem();
                        if (organismObject!=null) organism=((Integer)organismObject).intValue();
                        String build=(String)editSourceBuildCombobox.getSelectedItem();
                        build=(build==null)?"":build.trim();
                        String dataformat=(String)editSourceDataFormatCombobox.getSelectedItem();                      
                        newDataSource.initializeDataSource(currentDataTrack, organism, build, dataformat);
                        datasourcepanel=newDataSource.getConfigurationPanel();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(gui.getFrame(), e.getClass().getSimpleName()+"\n"+e.getMessage(),"Error" ,JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    datasourcepanel=currentDataSource.getConfigurationPanel();
                }
                
            }
            if (datasourcepanel==null) datasourcepanel=newMessagePanel("No configuration panel found for DataSource protocol: "+protocol);
            // ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).addLayoutComponent(datasourcepanel, protocol); // not working?!
            datasourcepanel.setName(protocol); // this is necessary so that we can search for it later by name
            addUniqueComponent(cardpaneEditSourceMainCardPanel, datasourcepanel, protocol);
            ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, protocol);
    }          
}//GEN-LAST:event_editSourceProtocolChanged

    /** Returns a very simple JPanel containing a single JLabel with a message */
    private JPanel newMessagePanel(String message) {
        JPanel panel=new JPanel();
        JLabel label=new JLabel(message);
        panel.add(label);
        return panel;
    }
    
    private JPanel getDataSourceConfigurationPanel(JPanel parent, String protocol) {
        Component[] components=parent.getComponents();                  
        for (Component c:components) {
           if (protocol.equals(c.getName()) && c instanceof JPanel) {
               return (JPanel)c;
           }
        }        
        return null;
    }
    
    /**
    * This method adds a panel with the specific name (constraint) to the parent
    * container while also removing any other child components that have the same name
    */
    private void addUniqueComponent(JPanel parent, JPanel panel, String name) {
        ArrayList<Component> removeList=new ArrayList<>();
        Component[] components=parent.getComponents();
        for (Component c:components) {
           if (name.equals(c.getName())) {
               removeList.add(c);
           }
        }  
        for (Component r:removeList) {
            parent.remove(r);
        }
        parent.add(panel,name);
    }


    private void addServerButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addServerButtonPressed
            Object[] values=new Object[]{"", new Integer(0),""};
            serverTableModel.addRow(values);
               int newrow=getFirstEmptyRowInTable(serverTable);
               if (newrow>=0) {
                   boolean canedit=serverTable.editCellAt(newrow, 0);
                   if (canedit) {
                       serverTable.changeSelection(newrow, 0, false, false);
                       serverTable.requestFocus();
                   }
               }
    }//GEN-LAST:event_addServerButtonPressed

    private void FileServerBrowseButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileServerBrowseButtonPressed
        JFileChooser filechooser = new JFileChooser(gui.getLastUsedDirectory()); // Using the regular JFileChooser here instead of "gui.getFileChooser(null)" since File Servers can only use local files, not data repository files
        boolean directFile=true; // TRUE = single compressed binary file server, FALSE = legacy split files
        String dataformatName=(String)editSourceDataFormatCombobox.getSelectedItem();
        DataFormat format=engine.getDataFormat(dataformatName);
        if (format instanceof DataFormat_BED || format instanceof DataFormat_FASTA || format instanceof DataFormat_WIG) {
            directFile=false; // to support legacy split files
        }
        if (directFile) {
            filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            filechooser.setDialogTitle("Select fileserver file");
        } else {
            filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            filechooser.setDialogTitle("Select fileserver directory");
        }
      
        int status=filechooser.showOpenDialog(this);
        if (status==JFileChooser.APPROVE_OPTION) {
            File selected=filechooser.getSelectedFile();
            // gui.setLastUsedDirectory(selected);
            editFILEsourceFilenameTextfield.setText(selected.getAbsolutePath());
        }  
    }//GEN-LAST:event_FileServerBrowseButtonPressed

    private void protocolHelpButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolHelpButtonPressed
        String protocol=(String)editSourceProtocolCombobox.getSelectedItem();
        StringBuilder builder = new StringBuilder();
        if (MotifLabEngine.inArray(protocol, new String[]{DataSource_http_GET.PROTOCOL_NAME, DataSource_DAS.PROTOCOL_NAME, DataSource_FileServer.PROTOCOL_NAME, DataSource_SQL.PROTOCOL_NAME, DataSource_VOID.PROTOCOL_NAME}, true)) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/motiflab/gui/resources/helpFor"+protocol+"protocol.html"), "UTF-8"));
                for (int c = br.read(); c != -1; c = br.read()) builder.append((char)c);        
            } catch (Exception e) {
                builder.append(e.toString());
                // e.printStackTrace(System.err);
            }
        } else { // plugin data source
            Object resource=engine.getResource(protocol, "DataSource");
            if (resource instanceof DataSource) {
                String helpString=((DataSource)resource).getHelp();
                if (helpString==null) helpString="<html><h1>"+protocol+" Data Source</h1>No documentation found for this data source protocol!</html>"; 
                builder.append(helpString);
            }
        }
        InfoDialog infodialog=new InfoDialog(gui, "Help", builder.toString(),600,500);
        infodialog.setVisible(true);
        infodialog.dispose();
    }//GEN-LAST:event_protocolHelpButtonPressed

    private void keyReleasedInFilterTextfield(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyReleasedInFilterTextfield
        String text=trackFilterTextField.getText();                                             
        int namecol=datatrackTableModel.findColumn(TABLECOLUMN_NAME);
        if (text!=null && text.isEmpty()) expressionRowFilter=null;
        else {
            text=text.replaceAll("\\W", ""); // to avoid problems with regex characters
            expressionRowFilter=RowFilter.regexFilter("(?i)"+text,namecol);
        }
        installFilters();    
    }//GEN-LAST:event_keyReleasedInFilterTextfield

    private void sourceOrganismSelected(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceOrganismSelected
        Integer org=(Integer)editSourceOrganismCombobox.getSelectedItem();
        int organism=0;
        if (org!=null) organism=org.intValue();
        String[] builds=Organism.getSupportedGenomeBuilds(organism);
        if (builds==null) builds=new String[0];
        DefaultComboBoxModel<String> buildsModel=new DefaultComboBoxModel<String>(builds);
        editSourceBuildCombobox.setModel(buildsModel);
    }//GEN-LAST:event_sourceOrganismSelected
    
    @SuppressWarnings("unchecked")
    private void installFilters() {
       ((TableRowSorter)datatrackTable.getRowSorter()).setRowFilter(expressionRowFilter);
    }    
    
    private int getFirstEmptyRowInTable(JTable table) {
        for (int i=0;i<table.getRowCount();i++) {
            Object val=table.getValueAt(i, 0);
            if (val==null || val.toString().isEmpty()) return i;
        }
        return -1;
    }

    protected void saveSettingsToFile(final File configurationfile, final DataConfiguration config) {
       SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override 
            public Boolean doInBackground() {
                try {
                    config.saveConfigurationToFile(configurationfile);
                } catch (Exception e) {
                    ex=e;
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     JOptionPane.showMessageDialog(gui.getFrame(), ex.getClass().getSimpleName()+"\n"+ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                     // ex.printStackTrace(System.err);
                     return;
                }
            }
        }; // end of SwingWorker class
        worker.execute();       
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel DataFormatSettingsTopPanel;
    private javax.swing.JPanel DataFormatSettingsTopPanel_FILE;
    private javax.swing.JButton addServerButton;
    private javax.swing.JComboBox addTrackSourceCombobox;
    private javax.swing.JPanel additionalDataFormatSettingsPanel_FILE;
    private javax.swing.JPanel additionalDataFormatSettingsPanel_GET;
    private javax.swing.JButton allTracksAddButton;
    private javax.swing.JButton allTracksEditButton;
    private javax.swing.JButton allTracksOKButton;
    private javax.swing.JButton allTracksRemoveButton;
    private javax.swing.JPanel cardpaneAllTracks;
    private javax.swing.JPanel cardpaneAllTracksButtonPanel;
    private javax.swing.JPanel cardpaneAllTracksMainPanel;
    private javax.swing.JScrollPane cardpaneAllTracksScrollPane;
    private javax.swing.JPanel cardpaneAllTracksTopPanel;
    private javax.swing.JPanel cardpaneEditSource;
    private javax.swing.JPanel cardpaneEditSourceButtonsPanel;
    private javax.swing.JPanel cardpaneEditSourceMainCard;
    private javax.swing.JPanel cardpaneEditSourceMainCardPanel;
    private javax.swing.JPanel cardpaneEditSourceTopPanel;
    private javax.swing.JPanel cardpaneEditTrack;
    private javax.swing.JPanel cardpaneEditTrackButtonPanel;
    private javax.swing.JPanel cardpaneEditTrackMainPanel;
    private javax.swing.JPanel cardpaneEditTrackTopPanel;
    private javax.swing.JScrollPane cardpaneEditTracksScrollPane;
    private javax.swing.JPanel cardpaneServers;
    private javax.swing.JPanel cardpaneServersButtonPanel;
    private javax.swing.JPanel cardpaneServersMainPanel;
    private javax.swing.JPanel cardpaneServersTopPanel;
    private javax.swing.JPanel cardpaneTopMenu;
    private javax.swing.JPanel cardpaneTopMenuButtonPanel;
    private javax.swing.JPanel cardpaneTopMenuMainPanel;
    private javax.swing.JPanel cardpaneTopMenuTopPanel;
    private javax.swing.JButton configureDatatracksButton;
    private javax.swing.JButton configureServerSettingsButton;
    private javax.swing.JLabel dataTrackPropertiesDescriptionLabel;
    private javax.swing.JTextField dataTrackPropertiesDescriptionTextfield;
    private javax.swing.JLabel dataTrackPropertiesDisplaySettingsLabel;
    private javax.swing.JTextField dataTrackPropertiesDisplaySettingsTextfield;
    private javax.swing.JLabel dataTrackPropertiesNameLabel;
    private javax.swing.JTextField dataTrackPropertiesNameTextfield;
    private javax.swing.JLabel dataTrackPropertiesSourceLabel;
    private javax.swing.JTextField dataTrackPropertiesSourceTextfield;
    private javax.swing.JComboBox dataTrackPropertiesTypeCombobox;
    private javax.swing.JLabel dataTrackPropertiesTypeLabel;
    private javax.swing.JLabel datasourcesLabel;
    private javax.swing.JLabel datatracksLabel;
    private javax.swing.JLabel editDASsourceBaseURLlabel;
    private javax.swing.JTextField editDASsourceBaseURLtextfield;
    private javax.swing.JLabel editDASsourceFeatureLabel;
    private javax.swing.JTextField editDASsourceFeatureTextfield;
    private javax.swing.JPanel editDASsourcePanel;
    private javax.swing.JPanel editDASsourcePanelInner;
    private javax.swing.JButton editFILEsourceBrowseButton;
    private javax.swing.JLabel editFILEsourceFilenameLabel;
    private javax.swing.JTextField editFILEsourceFilenameTextfield;
    private javax.swing.JPanel editFILEsourcePanel;
    private javax.swing.JPanel editFILEsourcePanelInner;
    private javax.swing.JLabel editFILEsourceSegmentsizeLabel;
    private javax.swing.JSpinner editFILEsourceSegmentsizeSpinner;
    private javax.swing.JLabel editGETsourceBaseURLlabel;
    private javax.swing.JTextField editGETsourceBaseURLtextfield;
    private javax.swing.JPanel editGETsourcePanel;
    private javax.swing.JPanel editGETsourcePanelInner;
    private javax.swing.JLabel editGETsourceParametersLabel;
    private javax.swing.JTextField editGETsourceParametersTextfield;
    private javax.swing.JButton editSQLsourceAddPropertyButton;
    private javax.swing.JTextField editSQLsourceDatabaseNameField;
    private javax.swing.JLabel editSQLsourceDatabaseNameLabel;
    private javax.swing.JPanel editSQLsourcePanel;
    private javax.swing.JPasswordField editSQLsourcePasswordField;
    private javax.swing.JLabel editSQLsourcePasswordLabel;
    private javax.swing.JTextField editSQLsourcePortField;
    private javax.swing.JLabel editSQLsourcePortLabel;
    private javax.swing.JButton editSQLsourceRemovePropertyButton;
    private javax.swing.JTextField editSQLsourceServerURLField;
    private javax.swing.JLabel editSQLsourceServerURLLabel;
    private javax.swing.JPanel editSQLsourceTableControlsPanel;
    private javax.swing.JPanel editSQLsourceTableMainPanel;
    private javax.swing.JTextField editSQLsourceTableNameField;
    private javax.swing.JLabel editSQLsourceTableNameLabel;
    private javax.swing.JPanel editSQLsourceTablePanel;
    private javax.swing.JScrollPane editSQLsourceTableScrollPane;
    private javax.swing.JPanel editSQLsourceTopPanel;
    private javax.swing.JPanel editSQLsourceTopPanelOuter;
    private javax.swing.JTextField editSQLsourceUsernameField;
    private javax.swing.JLabel editSQLsourceUsernameLabel;
    private javax.swing.JComboBox editSourceBuildCombobox;
    private javax.swing.JLabel editSourceBuildLabel;
    private javax.swing.JButton editSourceCancelButton;
    private javax.swing.JComboBox editSourceDataFormatCombobox;
    private javax.swing.JLabel editSourceDataFormatLabel;
    private javax.swing.JButton editSourceOKButton;
    private javax.swing.JComboBox editSourceOrganismCombobox;
    private javax.swing.JLabel editSourceOrganismLabel;
    private javax.swing.JPanel editSourcePropertiesPanel;
    private javax.swing.JLabel editSourceProtocoLabel;
    private javax.swing.JComboBox editSourceProtocolCombobox;
    private javax.swing.JLabel editSourceTrackNameLabel;
    private javax.swing.JTextField editSourceTrackNameTextfield;
    private javax.swing.JButton editTrackAddButton;
    private javax.swing.JComboBox editTrackAddSourceCombobox;
    private javax.swing.JButton editTrackCancelButton;
    private javax.swing.JButton editTrackEditButton;
    private javax.swing.JButton editTrackOKButton;
    private javax.swing.JPanel editTrackPropertiesPanel;
    private javax.swing.JButton editTrackRemoveButton;
    private javax.swing.JPanel editVOIDsourcePanel;
    private javax.swing.JButton exitWithoutSaveButton;
    private javax.swing.JButton exportSettingsButton;
    private javax.swing.JButton importSettingsButton;
    private javax.swing.JPanel internalWrapper1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton protocolHelpButton;
    private javax.swing.JButton revertSettingsButton;
    private javax.swing.JButton saveChangesButton;
    private javax.swing.JLabel serversLabel;
    private javax.swing.JButton serversOKButton;
    private javax.swing.JScrollPane serversScrollPane;
    private javax.swing.JTextField trackFilterTextField;
    // End of variables declaration//GEN-END:variables


    private void showTopMenuPanel() {
         currentDataTrack=null;
        ((CardLayout)getContentPane().getLayout()).show(getContentPane(), "cardpaneTopMenu");
         getRootPane().setDefaultButton(saveChangesButton);       
    }  
    private void showServersPanel() {
        ((CardLayout)getContentPane().getLayout()).show(getContentPane(), "cardpaneServers");
         getRootPane().setDefaultButton(serversOKButton);       
    }  

    private void showAllTracksPanel() {
        setupTablesFromConfiguration();
         currentDataTrack=null;
        ((CardLayout)getContentPane().getLayout()).show(getContentPane(), "cardpaneAllTracks");
         getRootPane().setDefaultButton(allTracksOKButton);       
    }    
    
    private void showEditDataTrackPanel(DataTrack datatrack) {      
         if (datatrack==null) addDataTrack=true; else addDataTrack=false;
         currentDataTrack=datatrack;
         String datatrackName=(addDataTrack)?"":datatrack.getName();
         String description=(addDataTrack)?"":datatrack.getDescription();
         String sourceSite=(addDataTrack)?"":datatrack.getSourceSite();
         String displayProtocol=(addDataTrack)?"":datatrack.getDisplayDirectivesProtocol();
         if (displayProtocol==null) displayProtocol="";
         Class datatype=(addDataTrack)?null:datatrack.getDataType();
         dataTrackPropertiesNameTextfield.setText(datatrackName);
         dataTrackPropertiesDescriptionTextfield.setText(description);
         dataTrackPropertiesDescriptionTextfield.setCaretPosition(0);
         dataTrackPropertiesDisplaySettingsTextfield.setText(displayProtocol);
         dataTrackPropertiesDisplaySettingsTextfield.setCaretPosition(0);         
         dataTrackPropertiesSourceTextfield.setText(sourceSite);
              if (datatype==null) dataTrackPropertiesTypeCombobox.setSelectedItem("Numeric");
         else if (datatype==DNASequenceDataset.class) dataTrackPropertiesTypeCombobox.setSelectedItem("Sequence");
         else if (datatype==NumericDataset.class) dataTrackPropertiesTypeCombobox.setSelectedItem("Numeric");
         else if (datatype==RegionDataset.class) dataTrackPropertiesTypeCombobox.setSelectedItem("Region");            
         ArrayList<DataSource> sources=(addDataTrack)?new ArrayList<DataSource>():datatrack.getDatasources();
         String[] columnNames=new String[]{TABLECOLUMN_ORGANISM,TABLECOLUMN_BUILD,TABLECOLUMN_PROTOCOL,TABLECOLUMN_DATAFORMAT,TABLECOLUMN_SERVER};
         Object[][] data=new Object[sources.size()][columnNames.length];
         for (int i=0;i<sources.size();i++) {          
             DataSource source=sources.get(i);
             data[i]=new Object[]{new Integer(source.getOrganism()),source.getGenomeBuild(),source.getProtocol(),source.getDataFormat(),source.getServerAddress()};
         }
         datasourceTableModel=new DefaultTableModel(data,columnNames);
         datasourceTable.setModel(datasourceTableModel);
         datasourceTable.getColumn(TABLECOLUMN_ORGANISM).setCellRenderer(organismRenderer);
         ((CardLayout)getContentPane().getLayout()).show(getContentPane(), "cardpaneEditTrack");        
         getRootPane().setDefaultButton(editTrackOKButton);
         editTrackEditButton.setEnabled(false); // this will be enabled when selecting a source
         editTrackRemoveButton.setEnabled(false);      
    }    
    
    private void showEditDataSourcePanel(DataSource datasource) {
         if (datasource==null) addDataSource=true; else addDataSource=false; 
         currentDataSource=datasource;         
         String datatrackName=currentDataTrack.getName();
         int organism=(addDataSource)?Organism.HUMAN:datasource.getOrganism(); // default to HUMAN hg38 when adding new source
         String build=(addDataSource)?"hg38":datasource.getGenomeBuild();
         if (build==null) build="";         
         String protocol=(addDataSource)?DataSource_http_GET.PROTOCOL_NAME:datasource.getProtocol();
         String dataformat=(addDataSource)?null:datasource.getDataFormat();
         if (dataformat!=null) {
            showDataFormatSettingsPanel(protocol, getDataFormatSettingsPanel(dataformat)); 
         }
         // clear current field values
         editSourceTrackNameTextfield.setText(datatrackName);
         editSourceOrganismCombobox.setSelectedItem(organism);         
         editSourceBuildCombobox.setSelectedItem(build);
         editDASsourceBaseURLtextfield.setText("");
         editDASsourceFeatureTextfield.setText("");
         editGETsourceBaseURLtextfield.setText("");
         editGETsourceParametersTextfield.setText("");
         editFILEsourceFilenameTextfield.setText("");
         editFILEsourceSegmentsizeSpinner.setValue(new Integer(0));
         editSQLsourceServerURLField.setText("");
         editSQLsourcePortField.setText("");
         editSQLsourceUsernameField.setText("");
         editSQLsourcePasswordField.setText("");
         editSQLsourceDatabaseNameField.setText("");          
         editSQLsourceTableNameField.setText("");      
                            
         if (datasource==null) {
             ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "GETprotocol"); // default to GET protocol if no datasource is provided
         } else if (datasource instanceof DataSource_DAS) {
             editDASsourceBaseURLtextfield.setText(((DataSource_DAS)datasource).getBaseURL());
             editDASsourceFeatureTextfield.setText(((DataSource_DAS)datasource).getFeature());     
             ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "DASprotocol");
         } else if (datasource instanceof DataSource_http_GET) {
             editGETsourceBaseURLtextfield.setText(((DataSource_http_GET)datasource).getBaseURL());
             editGETsourceParametersTextfield.setText(((DataSource_http_GET)datasource).getParameter());
             editGETsourceParametersTextfield.setCaretPosition(0);
             ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "GETprotocol");
         } else if (datasource instanceof DataSource_FileServer) {
             editFILEsourceFilenameTextfield.setText(((DataSource_FileServer)datasource).getFilepath());
             editFILEsourceSegmentsizeSpinner.setValue(((DataSource_FileServer)datasource).getSegmentsize());             
             editFILEsourceFilenameTextfield.setCaretPosition(0);
             ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "FILEprotocol");
         } else if (datasource instanceof DataSource_SQL) {
             editSQLsourceServerURLField.setText(((DataSource_SQL)datasource).getServerAddress());
             int portnumber=((DataSource_SQL)datasource).getPortNumber();
             editSQLsourcePortField.setText((portnumber>0)?(""+portnumber):"");
             editSQLsourceUsernameField.setText(((DataSource_SQL)datasource).getUsername());
             editSQLsourcePasswordField.setText(((DataSource_SQL)datasource).getPassword());
             editSQLsourceDatabaseNameField.setText(((DataSource_SQL)datasource).getDatabaseName());          
             editSQLsourceTableNameField.setText(((DataSource_SQL)datasource).getTablename());
             initializeSQLtableFromSource((DataSource_SQL)datasource);             
             ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "SQLprotocol");
         } else if (datasource instanceof DataSource_VOID) {
             ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, "VOIDprotocol");
         } else if (datasource instanceof Plugin) {          
             JPanel datasourcepanel=datasource.getConfigurationPanel();                  
             if (datasourcepanel==null) datasourcepanel=newMessagePanel("No configuration panel found for DataSource protocol: "+protocol);
             datasourcepanel.setName(protocol); // this is necessary so that we can search for it later by name
             addUniqueComponent(cardpaneEditSourceMainCardPanel, datasourcepanel, protocol);         
             ((CardLayout)cardpaneEditSourceMainCardPanel.getLayout()).show(cardpaneEditSourceMainCardPanel, protocol);             
         } 
         if (addDataSource) editSourceProtocolCombobox.setSelectedItem("GET"); // Use GET as the "default" protocol 
         else editSourceProtocolCombobox.setSelectedItem(protocol);
         if (addDataSource) editSourceDataFormatCombobox.setSelectedIndex(0);
         else editSourceDataFormatCombobox.setSelectedItem(dataformat);            
         ((CardLayout)getContentPane().getLayout()).show(getContentPane(), "cardpaneEditSource"); 
         getRootPane().setDefaultButton(editSourceOKButton);
    }    
    
    private void setSourceDataFormatEnabled(boolean enabled) {
        editSourceDataFormatLabel.setVisible(enabled);
        editSourceDataFormatCombobox.setVisible(enabled);
    }
    
    /** Returns TRUE if the given DataSource protocol uses standard Data Formats to parse track data */
    private boolean usesStandardDataFormat(String protocol) {
        DataSource ds=getDataSourceTemplate(protocol);
        if (ds!=null) {
            return ds.usesStandardDataFormat();
        } else return false;
    }

    private DataSource getDataSourceTemplate(String protocol) {
       Object resource=engine.getResource(protocol, "DataSource");
       if (resource instanceof DataSource) return ((DataSource)resource);
       else return null;
    }
    
    
    @Action
    public void allTracksEditButtonClicked() {
       int row=datatrackTable.getSelectedRow();
       if (row<0) return;
       row=datatrackTable.convertRowIndexToModel(row);
       String trackname=(String)datatrackTable.getModel().getValueAt(row, 0);
       DataTrack track=availableTracks.get(trackname);
       showEditDataTrackPanel(track.clone());
    }

    @Action
    public void allTracksRemoveButtonClicked() {
       int row=datatrackTable.getSelectedRow();
       if (row<0) return;
       row=datatrackTable.convertRowIndexToModel(row);
       String trackname=(String)datatrackTable.getModel().getValueAt(row, 0);
       availableTracks.remove(trackname);
       datatrackTableModel.removeRow(row);
    }

    @Action
    public void editDataSource() {
       int row=datasourceTable.getSelectedRow();
       if (row<0) return;
       row=datasourceTable.convertRowIndexToModel(row);
       DataSource source=currentDataTrack.getDatasources().get(row);
       showEditDataSourcePanel(source);
    }
    
    @Action
    public void addDataSource() {
        if (currentDataTrack==null) { // setup a temp track if no track has been configured just yet
            String editname=dataTrackPropertiesNameTextfield.getText().trim();
            if (editname.isEmpty()) {
                JOptionPane.showMessageDialog(gui.getFrame(), "You must supply a name for the Datatrack","Missing track name error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String typename=(String)dataTrackPropertiesTypeCombobox.getSelectedItem();
            Class newtype=null;
                 if (typename.equals("Sequence")) newtype=DNASequenceDataset.class;
            else if (typename.equals("Numeric")) newtype=NumericDataset.class;
            else if (typename.equals("Region")) newtype=RegionDataset.class;
            String sourcesite=dataTrackPropertiesSourceTextfield.getText().trim();
            String description=dataTrackPropertiesDescriptionTextfield.getText().trim(); 
            String dsp=dataTrackPropertiesDisplaySettingsTextfield.getText().trim(); 
            currentDataTrack=new DataTrack(editname, newtype, sourcesite, description);       
            currentDataTrack.setDisplayDirectivesProtocol(dsp.isEmpty()?null:dsp);
        }
        String selectedSource=(String)editTrackAddSourceCombobox.getSelectedItem();
             if (selectedSource.equalsIgnoreCase("UCSC Genome Browser")) addDataSourceFromUCSC();
        else if (selectedSource.equalsIgnoreCase("DAS Registry")) addDataSourceFromDAS();
        else if (selectedSource.equalsIgnoreCase("Manual entry")) showEditDataSourcePanel(null);   
        else { // this could be a plugin 
            Object object=engine.getResource(selectedSource, "DataSourceConfigurationDialog");
            if (object instanceof Plugin) addDataSourceFromPlugin((Plugin)object);
            else JOptionPane.showMessageDialog(gui.getFrame(), "Unable to show configuration dialog for '"+selectedSource+"'","Error", JOptionPane.ERROR_MESSAGE);
        }                 
    }
    
    private void addDataSourceFromUCSC() {
        UCSCtrackDialog ucscdialog = new UCSCtrackDialog(gui,currentDataTrack);
        ucscdialog.setLocation(gui.getFrame().getWidth()/2-ucscdialog.getWidth()/2, gui.getFrame().getHeight()/2-ucscdialog.getHeight()/2);
        ucscdialog.setVisible(true);
        if (ucscdialog.isOKPressed()) {
            DataSource newsource=ucscdialog.getDataSource(currentDataTrack);
            ucscdialog.dispose();
            if (newsource!=null) {
               currentDataTrack.addDataSource(newsource);
               showEditDataTrackPanel(currentDataTrack);
            }            
        } else {
           ucscdialog.dispose(); 
        }                        
    }
    
    private void addDataSourceFromDAS() {     
        DASRegistryDialog dasdialog = new DASRegistryDialog(gui, currentDataTrack);
        dasdialog.setLocation(gui.getFrame().getWidth()/2-dasdialog.getWidth()/2, gui.getFrame().getHeight()/2-dasdialog.getHeight()/2);
        dasdialog.setVisible(true);
        if (dasdialog.isOKPressed()) {
            DataSource newsource=dasdialog.getDataSource(currentDataTrack);
            dasdialog.dispose();
            if (newsource!=null) {
               currentDataTrack.addDataSource(newsource);
               showEditDataTrackPanel(currentDataTrack);
            }           
        } else {
           dasdialog.dispose(); 
        }              
    }    

    /**
     * This method can be used to display a special Data Source dialog implemented as a (configurable) plugin.
     * The plugin should have been registered as a MotifLabResource instance with type "DataSourceConfigurationDialog".
     * The plugin is provided with two values via the setPluginParameterValue() method.
     * The first is "gui" and the second is "currentDataTrack". The plugin should return a JDialog if the plugin
     * parameter value "datasource_dialog" is requested via the getPluginParameterValue() method. 
     * (Note that the same plugin could be registered as both a "DataTrackConfigurationDialog" resource and a
     * "DataSourceConfigurationDialog" resource and should then be able to return both a "datatrack_dialog" and a "dataource_dialog".)
     * The dialog will be shown in a modal fashion. The dialog should contain "OK" and "Cancel" buttons
     * that can be used to close the dialog (the dialog is responsible for calling "setVisible(false)" if either
     * of these buttons are pressed. If the OK button is pressed, the dialog must also make the selected DataSource
     * object available through a call to getPluginParameterValue("datasource"). If the Cancel button was pressed
     * the request for the datasource parameter value should return NULL.
     * @param plugin A plugin which should also implement ConfigurablePlugin
     */
    private void addDataSourceFromPlugin(Plugin plugin) {
        try {
           if (!(plugin instanceof ConfigurablePlugin)) throw new ExecutionError("Unable to obtain configuration dialog for "+plugin.getPluginName());
           if (currentDataTrack==null) throw new ExecutionError("Missing data track to configure data source for");
           ((ConfigurablePlugin)plugin).setPluginParameterValue("gui",gui); // just in case this is not properly initialized from client
           ((ConfigurablePlugin)plugin).setPluginParameterValue("currentDataTrack",currentDataTrack);           
           Object d = ((ConfigurablePlugin)plugin).getPluginParameterValue("datasource_dialog");           
           if (d instanceof javax.swing.JDialog) {
               javax.swing.JDialog dialog=(javax.swing.JDialog)d;
               dialog.setModal(true);
               dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
               dialog.setVisible(true);  // this should lock until the dialog closes itself
               Object newsource=((ConfigurablePlugin)plugin).getPluginParameterValue("datasource"); // the plugin should set this parameter if the user has pressed OK button to close the dialog
               dialog.dispose();
               if (newsource instanceof DataSource) {
                  currentDataTrack.addDataSource((DataSource)newsource);
                  showEditDataTrackPanel(currentDataTrack);                   
               }
           } else throw new ExecutionError("Missing configuration dialog for "+plugin.getPluginName());
        } catch (ExecutionError e) {
               JOptionPane.showMessageDialog(gui.getFrame(), e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    @Action
    public void removeDataSource() {
       int row=datasourceTable.getSelectedRow();
       if (row<0) return;
       row=datasourceTable.convertRowIndexToModel(row);
       DataSource source=currentDataTrack.getDatasources().remove(row);
       datasourceTableModel.removeRow(row);
       for (int i=0;i<datatrackTableModel.getRowCount();i++) {
           if (datatrackTableModel.getValueAt(i, 0).equals(currentDataTrack.getName())) {
               int[] supported=currentDataTrack.getSupportedOrganisms();
               datatrackTableModel.setValueAt(supported, i, 3);break;
           }
       }
    }

    @Action
    public void allTracksOKButtonClickedAction() {
        showTopMenuPanel();
    }    
    @Action
    public void allTrackAddButtonClicked() {
         String selectedSource=(String)addTrackSourceCombobox.getSelectedItem();
              if (selectedSource.equalsIgnoreCase("UCSC Genome Browser")) allTracksAddFromUCSCGenomeBrowser();
         else if (selectedSource.equalsIgnoreCase("DAS Registry")) allTracksAddFromDASRegistry();
         else if (selectedSource.equalsIgnoreCase("Configuration file")) allTracksAddFromConfigFile();          
         else if (selectedSource.equalsIgnoreCase("Manual entry")) showEditDataTrackPanel(null);      
         else { // this could be a plugin 
            Object object=engine.getResource(selectedSource, "DataTrackConfigurationDialog");
            if (object instanceof Plugin) allTracksAddFromPlugin((Plugin)object);
            else JOptionPane.showMessageDialog(gui.getFrame(), "Unable to show configuration dialog for '"+selectedSource+"'","Error", JOptionPane.ERROR_MESSAGE);
         }
    }


    
    private void allTracksAddFromUCSCGenomeBrowser() {
        UCSCtrackDialog ucscdialog = new UCSCtrackDialog(gui,null);
        ucscdialog.setLocation(gui.getFrame().getWidth()/2-ucscdialog.getWidth()/2, gui.getFrame().getHeight()/2-ucscdialog.getHeight()/2);
        ucscdialog.setVisible(true);
        if (ucscdialog.isOKPressed()) {
            DataTrack newtrack=ucscdialog.getDataTrack();
            ucscdialog.dispose();
            if (newtrack!=null) {
                String newtrackname=newtrack.getName();
                if (availableTracks.containsKey(newtrackname)) {
                    int choice=JOptionPane.showConfirmDialog(this, "A data track named \""+newtrackname+"\" already exists.\nWould you like to replace this track?","Replace data track",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
                    if (choice!=JOptionPane.OK_OPTION) return;                       
                }
                availableTracks.put(newtrack.getName(), newtrack);
                showAllTracksPanel(); // this will setup the table from the configuration
            }            
        } else {
           ucscdialog.dispose(); 
        }     
    }

    private void allTracksAddFromDASRegistry() {
        DASRegistryDialog dasdialog = new DASRegistryDialog(gui);
        dasdialog.setLocation(gui.getFrame().getWidth()/2-dasdialog.getWidth()/2, gui.getFrame().getHeight()/2-dasdialog.getHeight()/2);
        dasdialog.setVisible(true);
        if (dasdialog.isOKPressed()) {
            DataTrack newtrack=dasdialog.getDataTrack();
            dasdialog.dispose();
            if (newtrack!=null) {
                String newtrackname=newtrack.getName();
                if (availableTracks.containsKey(newtrackname)) {
                    int choice=JOptionPane.showConfirmDialog(this, "A data track named \""+newtrackname+"\" already exists.\nWould you like to replace this track?","Replace data track",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
                    if (choice!=JOptionPane.OK_OPTION) return;                       
                }
                availableTracks.put(newtrack.getName(), newtrack);
                showAllTracksPanel(); // this will setup the table from the configuration
            }            
        } else {
           dasdialog.dispose(); 
        }           
    }  
    
    private void allTracksAddFromConfigFile() {
        DataTrackConfiguration_AddFromConfigFileDialog dialog = new DataTrackConfiguration_AddFromConfigFileDialog(this,gui);
        dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
        dialog.setVisible(true);
        DataConfiguration newconfig=dialog.getConfiguration();
        int mode=dialog.getTreatDuplicatesMode();
        dialog.dispose();
        
        if (newconfig!=null) { // user pressed OK and no parse problems encountered  
            StringBuilder builder=new StringBuilder();            
            builder.append("<table border=0>");
            HashMap<String,DataTrack> datatracks=newconfig.getAvailableTracks();
            for (String trackname:datatracks.keySet()) {
                DataTrack newtrack=datatracks.get(trackname); 
                if (!availableTracks.containsKey(trackname)) {
                    availableTracks.put(trackname,newtrack); // no such track from before. Just add it                   
                    builder.append("<tr><td>");
                    builder.append(trackname);
                    builder.append("</td><td><font color=\"#00BB00\">&nbsp;&nbsp;&nbsp;NEW TRACK&nbsp;&nbsp;&nbsp;</font></td><td>");  
                    int size=newtrack.getDatasources().size();
                    builder.append(size);
                    builder.append("&nbsp;source"+((size!=1)?"s":""));
                    builder.append("</td></tr>");
                } else { // a track with the same name already exists. Now we must either replace the track or merge the sources (after first checking that it is actually compatible)
                    DataTrack oldTrack=availableTracks.get(trackname);
                    builder.append("<tr><td>");
                    builder.append(trackname);                    
                    if (mode==DataTrackConfiguration_AddFromConfigFileDialog.REPLACE_TRACK) { // replace the whole track
                        availableTracks.put(trackname, newtrack);
                        builder.append("</td><td colspan=\"2\"><font color=\"#FF0000\">&nbsp;&nbsp;&nbsp;REPLACED TRACK&nbsp;&nbsp;&nbsp;</font></td></tr>");                            
                        continue;
                    }                 
                    if (!oldTrack.getDataType().equals(newtrack.getDataType())) { // type conflict with existing track
                        builder.append("</td><td colspan=\"2\"><font color=\"#FF0000\">&nbsp;&nbsp;&nbsp;***&nbsp;INCOMPATIBLE&nbsp;***&nbsp;&nbsp;&nbsp;</font></td></tr>");                                            
                        continue;
                    }
                    boolean doMerge=(mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_PREFERRED || mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_MIRRORS);
                    String mergeString=(doMerge)?"MERGED":"REPLACED";
                    builder.append("</td><td><font color=\"#FFA500\">&nbsp;&nbsp;&nbsp;"+mergeString+"&nbsp;&nbsp;&nbsp;</font></td><td>");   
                    int size=newtrack.getDatasources().size();
                    builder.append(size);
                    builder.append("&nbsp;new source"+((size!=1)?"s":""));
                    builder.append("</td></tr>");                 
                    ArrayList<DataSource> newsources=newtrack.getDatasources();
                    if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_PREFERRED) {
                        oldTrack.addPreferredDataSources(newsources);
                    } else if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_MIRRORS) {
                        oldTrack.addDataSources(newsources);
                    } else if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_REMOVE_OLD) {
                        oldTrack.replaceDataSources(newsources);     
                    } else if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_REPLACE_ALL_SOURCES) {
                        oldTrack.replaceAllDataSources(newsources);     
                    }                    
                }
            }
            builder.append("</table>");
            // short a summary report
            InfoDialog infodialog=new InfoDialog(gui, "Added data tracks and sources", builder.toString(),500,300);
            infodialog.setMonospacedFont(12);
            infodialog.setVisible(true);
            infodialog.dispose();
            showAllTracksPanel(); // this will setup the table from the configuration
        }
        
    } 
    
    /**
     * This method can be used to display a special Data Track dialog implemented as a (configurable) plugin.
     * The plugin should have been registered as a MotifLabResource instance with type "DataTrackConfigurationDialog".
     * The plugin is provided with a reference to the GUI via the setPluginParameterValue("gui") method.
     * The plugin should return a JDialog if the plugin parameter value "datatrack_dialog" is requested via the getPluginParameterValue() method. 
     * (Note that the same plugin could be registered as both a "DataTrackConfigurationDialog" resource and a
     * "DataSourceConfigurationDialog" resource and should then be able to return both a "datatrack_dialog" and a "dataource_dialog".)
     * The dialog will be shown in a modal fashion. The dialog should contain "OK" and "Cancel" buttons
     * that can be used to close the dialog (the dialog is responsible for calling "setVisible(false)" if either
     * of these buttons are pressed. If the OK button is pressed, the dialog must also make the selected DataTrack
     * object available through a call to getPluginParameterValue("datatrack"). If the Cancel button was pressed
     * the request for the datatrack parameter value should return NULL.
     * @param plugin A plugin which should also implement ConfigurablePlugin
     */
    private void allTracksAddFromPlugin(Plugin plugin) {       
        try {
           if (!(plugin instanceof ConfigurablePlugin)) throw new ExecutionError("Unable to obtain configuration dialog for "+plugin.getPluginName());
           ((ConfigurablePlugin)plugin).setPluginParameterValue("gui",gui); // just in case this is not properly initialized from client    
           ((ConfigurablePlugin)plugin).setPluginParameterValue("currentDataTrack",null); // clear the currentDataTrack in the plugin (just in case)             
           Object d = ((ConfigurablePlugin)plugin).getPluginParameterValue("datatrack_dialog");
           if (d instanceof javax.swing.JDialog) {
               javax.swing.JDialog dialog=(javax.swing.JDialog)d;
               dialog.setModal(true);
               dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
               dialog.setVisible(true);  // this should block until the dialog closes itself
               Object newtrack=((ConfigurablePlugin)plugin).getPluginParameterValue("datatrack"); // the plugin should set this parameter if the user has pressed OK button to close the dialog
               dialog.dispose();
               if (newtrack instanceof DataTrack) { // if newtrack is NULL, the user has clicked the CANCEL button in the dialog
                    String newtrackname=((DataTrack)newtrack).getName();
                    if (availableTracks.containsKey(newtrackname)) {
                        int choice=JOptionPane.showConfirmDialog(this, "A data track named \""+newtrackname+"\" already exists.\nWould you like to replace this track?","Replace data track",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
                        if (choice!=JOptionPane.OK_OPTION) return;                       
                    }
                    availableTracks.put(((DataTrack)newtrack).getName(), (DataTrack)newtrack);
                    showAllTracksPanel(); // this will setup the table from the configuration                 
               }
           } else throw new ExecutionError("Missing configuration dialog for "+plugin.getPluginName());
        } catch (ExecutionError e) {
               JOptionPane.showMessageDialog(gui.getFrame(), e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
        }            
    }      

    @Action
    public void editTrackOKButtonClickedAction() {
         // store new values in track!!
        if (addDataTrack) { // adding new data track
            String editname=dataTrackPropertiesNameTextfield.getText().trim();
            if (editname.isEmpty()) {
                JOptionPane.showMessageDialog(gui.getFrame(), "You must supply a name for the Datatrack","Missing track name error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String typename=(String)dataTrackPropertiesTypeCombobox.getSelectedItem();
            Class newtype=null;
                 if (typename.equals("Sequence")) newtype=DNASequenceDataset.class;
            else if (typename.equals("Numeric")) newtype=NumericDataset.class;
            else if (typename.equals("Region")) newtype=RegionDataset.class;
            String sourcesite=dataTrackPropertiesSourceTextfield.getText().trim();
            String description=dataTrackPropertiesDescriptionTextfield.getText().trim();
            String dsp=dataTrackPropertiesDisplaySettingsTextfield.getText().trim();
            if (availableTracks.containsKey(editname)) {
                int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "There is already a track named '"+editname+"'\nWould you like to replace it?","Replace existing track", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice==JOptionPane.CANCEL_OPTION) return;
            }  
            currentDataTrack=new DataTrack(editname, newtype, sourcesite, description);
            currentDataTrack.setDisplayDirectivesProtocol(dsp.isEmpty()?null:dsp);             
            availableTracks.put(currentDataTrack.getName(), currentDataTrack); 
        } else { // editing existing track       
            String currenttrackname=currentDataTrack.getName();
            String editname=dataTrackPropertiesNameTextfield.getText().trim();
            String typename=(String)dataTrackPropertiesTypeCombobox.getSelectedItem();
            Class newtype=null;
                 if (typename.equals("Sequence")) newtype=DNASequenceDataset.class;
            else if (typename.equals("Numeric")) newtype=NumericDataset.class;
            else if (typename.equals("Region")) newtype=RegionDataset.class;
            if (!editname.equals(currenttrackname) && availableTracks.containsKey(editname)) {
                int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "There is already a track named '"+editname+"'\nWould you like to replace it?","Replace existing track", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice==JOptionPane.CANCEL_OPTION) return;
            } 
            currentDataTrack.setName(editname);
            currentDataTrack.setDataType(newtype);
            currentDataTrack.setSourceSite(dataTrackPropertiesSourceTextfield.getText().trim());
            currentDataTrack.setDescription(dataTrackPropertiesDescriptionTextfield.getText().trim()); 
            String dsp=dataTrackPropertiesDisplaySettingsTextfield.getText().trim();
            currentDataTrack.setDisplayDirectivesProtocol(dsp.isEmpty()?null:dsp);            
            if (!editname.equals(currenttrackname)) {
                availableTracks.remove(currenttrackname);
            }  
            availableTracks.put(editname, currentDataTrack);
        }
        //setupTablesFromConfiguration();
        showAllTracksPanel();
    }

    /**
     * Returns a reference to the working copy of the data configuration employed by the dialog
     * (this can include updates compared to the installed configuration)
     * @return 
     */
    public DataConfiguration getWorkingDataConfiguration() {
        return dataconfiguration;
    }

    @Action
    public void saveChangesAndExitAction() {
        File configurationfile=gui.getEngine().getDataLoader().getDataConfigurationFile();
        saveSettingsToFile(configurationfile,dataconfiguration);  
        gui.getEngine().getDataLoader().installConfiguration(dataconfiguration);
        setVisible(false);
    }

    @Action
    public void serversOKButtonClickedAction() {
        availableServers.clear();
        int rows=serverTableModel.getRowCount();
        for (int i=0;i<rows;i++) {
            Object addressobject=serverTableModel.getValueAt(i, SERVER_COLUMN_SERVERADDRESS);
            if (addressobject==null) continue;            
            String address=addressobject.toString();
            Object delayObject=serverTableModel.getValueAt(i, SERVER_COLUMN_DELAY);
            String delayasstring=(delayObject!=null)?delayObject.toString():"";
            Object maxspanObject=serverTableModel.getValueAt(i, SERVER_COLUMN_MAXSPAN);
            String maxspanstring=(maxspanObject!=null)?maxspanObject.toString():"";            
            Object mirrorbject=serverTableModel.getValueAt(i, SERVER_COLUMN_MIRRORS);  
            String mirrorstring=(mirrorbject==null)?"":mirrorbject.toString().trim();

            address=address.trim();
            if (address.isEmpty()) continue;
            if (availableServers.containsKey(address)) {
                int viewrow=serverTable.convertRowIndexToView(i);
                JOptionPane.showMessageDialog(cardpaneTopMenu, "The address '"+address+"' at row="+(viewrow+1)+",col="+(SERVER_COLUMN_SERVERADDRESS+1)+" has been used before", "Duplicate server error", JOptionPane.ERROR_MESSAGE);
                return;                
            }
            mirrorstring=mirrorstring.trim();
            int delay=0;
            if (!delayasstring.isEmpty()) {
              try {delay=Integer.parseInt(delayasstring);} 
              catch (NumberFormatException e) {
                int viewrow=serverTable.convertRowIndexToView(i);
                JOptionPane.showMessageDialog(cardpaneTopMenu, "The value '"+delayasstring+"' at row="+(viewrow+1)+",col="+(SERVER_COLUMN_DELAY+1)+" is not an integer number", "Number format error", JOptionPane.ERROR_MESSAGE);
                return;
              }
            }
            int maxspan=0;
            if (!maxspanstring.isEmpty()) {
              try {maxspan=Integer.parseInt(maxspanstring);} 
              catch (NumberFormatException e) {
                int viewrow=serverTable.convertRowIndexToView(i);
                JOptionPane.showMessageDialog(cardpaneTopMenu, "The value '"+maxspanstring+"' at row="+(viewrow+1)+",col="+(SERVER_COLUMN_MAXSPAN+1)+" is not an integer number", "Number format error", JOptionPane.ERROR_MESSAGE);
                return;
              }
            }            
            Server newserver=new Server(address,delay,maxspan);
            if (!mirrorstring.isEmpty()) {
                String[] mirrors=mirrorstring.split(",");
                for (String mirroraddress:mirrors) newserver.addMirror(mirroraddress);
            }    
            availableServers.put(address, newserver);
        }
        serverTable.clearSelection();
        setupTablesFromConfiguration();
        showTopMenuPanel();
    }

    @Action
    public void editSourceOKButtonClickedAction() { // this method is called when the user clicks the OK button in the "edit data source" panel (which is used both for adding a new data source and editing an existing one)
        int organism=Organism.HUMAN; // default
        Object organismObject=editSourceOrganismCombobox.getSelectedItem();
        if (organismObject!=null) organism=((Integer)organismObject);
        String build=(String)editSourceBuildCombobox.getSelectedItem();
        build=(build==null)?"":build.trim();
        String dataformat=(String)editSourceDataFormatCombobox.getSelectedItem();
        ParameterSettings dataformatSettings=null;
        if (dataFormatParametersPanel!=null) {
            dataFormatParametersPanel.setParameters();
            dataformatSettings=dataFormatParametersPanel.getParameterSettings();
        }           
        String protocol=(String)editSourceProtocolCombobox.getSelectedItem();  
        
        if (addDataSource) { // adding new data source for a track           
            if (protocol.equals(DataSource_DAS.PROTOCOL_NAME)) {
                String baseURL=editDASsourceBaseURLtextfield.getText().trim();
                String feature=editDASsourceFeatureTextfield.getText().trim();
                currentDataSource=new DataSource_DAS(currentDataTrack,organism,build,baseURL,null,feature);
            }
            else if (protocol.equals(DataSource_http_GET.PROTOCOL_NAME)) {
                String baseURL=editGETsourceBaseURLtextfield.getText().trim();
                String parameters=editGETsourceParametersTextfield.getText().trim();
                currentDataSource=new DataSource_http_GET(currentDataTrack,organism,build,baseURL,dataformat,parameters);
                currentDataSource.setDataFormatSettings(dataformatSettings);
            }
            else if (protocol.equals(DataSource_FileServer.PROTOCOL_NAME)) {
                String filepath=editFILEsourceFilenameTextfield.getText().trim();
                int segmentsize=((Integer)editFILEsourceSegmentsizeSpinner.getValue()).intValue();
                currentDataSource=new DataSource_FileServer(currentDataTrack,organism,build, filepath, segmentsize, dataformat);
                currentDataSource.setDataFormatSettings(dataformatSettings);
            }    
            else if (protocol.equals(DataSource_SQL.PROTOCOL_NAME)) {
                String baseURL=editSQLsourceServerURLField.getText().trim();
                String portString=editSQLsourcePortField.getText().trim();
                String username=editSQLsourceUsernameField.getText().trim();
                char[] password=editSQLsourcePasswordField.getPassword();
                String databasename=editSQLsourceDatabaseNameField.getText().trim();
                String tablename=editSQLsourceTableNameField.getText().trim();
                int port=-1;
                try {
                    if (!portString.isEmpty()) port=Integer.parseInt(portString); 
                } catch (NumberFormatException ne) {
                    JOptionPane.showMessageDialog(editSQLsourcePanel, "The port number must be an integer", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String passwordString=new String(password);
                ArrayList<DBfield> fields=null;
                try {
                    fields=parseSQLsourceTable();
                } catch (ParseError pe) {
                    JOptionPane.showMessageDialog(editSQLsourcePanel, pe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;                   
                }
                currentDataSource=new DataSource_SQL(currentDataTrack,organism,build, baseURL, port, databasename, tablename, username, passwordString, fields);
                initializeSQLtableFromSource(null); // clear the table for next time
            }             
            else if (protocol.equals(DataSource_VOID.PROTOCOL_NAME)) {
                currentDataSource=new DataSource_VOID(currentDataTrack,organism,build);
            }
            else { // add new source for plugin data source protocol
                Object resource=engine.getResource(protocol, "DataSource");
                if (resource instanceof DataSource) {
                    try {
                        currentDataSource=((DataSource)resource).getClass().newInstance(); // create new empty data source
                        currentDataSource.initializeDataSource(currentDataTrack, organism, build, dataformat);
                        JPanel configPanel=getDataSourceConfigurationPanel(cardpaneEditSourceMainCardPanel,protocol); // the configuration panel for this protocol should be stored as a child of cardpaneEditSourceMainCardPanel
                        if (configPanel!=null) currentDataSource.updateConfigurationFromPanel(configPanel);
                    } catch (Exception re) {
                        JOptionPane.showMessageDialog(cardpaneEditSourceMainCardPanel, re.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;   
                    }
                }
            }
            if (currentDataSource!=null) currentDataTrack.addDataSource(currentDataSource);          
        } else { // editing existing data source for a track
            String currentprotocol=currentDataSource.getProtocol();            
            if (protocol.equals(currentprotocol)) { // protocol is the same as before. Just replace the current values in the datasource object
                 currentDataSource.setDataFormat(dataformat);
                 currentDataSource.setGenomeBuild(build);
                 currentDataSource.setOrganism(organism);
                 currentDataSource.setDataFormatSettings(dataformatSettings);
                 if (currentDataSource instanceof DataSource_DAS) {
                    String baseURL=editDASsourceBaseURLtextfield.getText().trim();
                    String feature=editDASsourceFeatureTextfield.getText().trim();
                    ((DataSource_DAS)currentDataSource).setBaseURL(baseURL);
                    ((DataSource_DAS)currentDataSource).setFeature(feature);
                 } else if (currentDataSource instanceof DataSource_http_GET) {
                    String baseURL=editGETsourceBaseURLtextfield.getText().trim();
                    String parameter=editGETsourceParametersTextfield.getText().trim();
                    ((DataSource_http_GET)currentDataSource).setBaseURL(baseURL);
                    ((DataSource_http_GET)currentDataSource).setParameter(parameter);
                 } else if (currentDataSource instanceof DataSource_FileServer) {
                     String filepath=editFILEsourceFilenameTextfield.getText().trim();
                     int segmentsize=((Integer)editFILEsourceSegmentsizeSpinner.getValue()).intValue();
                    ((DataSource_FileServer)currentDataSource).setFilepath(filepath);
                    ((DataSource_FileServer)currentDataSource).setSegmentsize(segmentsize);
                 } else if (currentDataSource instanceof DataSource_SQL) {
                    String baseURL=editSQLsourceServerURLField.getText().trim();
                    String portString=editSQLsourcePortField.getText().trim();
                    String username=editSQLsourceUsernameField.getText().trim();
                    char[] password=editSQLsourcePasswordField.getPassword();
                    String databasename=editSQLsourceDatabaseNameField.getText().trim();
                    String tablename=editSQLsourceTableNameField.getText().trim();
                    int port=-1;
                    try {
                        if (!portString.isEmpty()) port=Integer.parseInt(portString); 
                    } catch (NumberFormatException ne) {
                        JOptionPane.showMessageDialog(editSQLsourcePanel, "The port number must be an integer", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String passwordString=new String(password);
                    ArrayList<DBfield> fields=null;
                    try {
                        fields=parseSQLsourceTable();
                    } catch (ParseError pe) {
                         JOptionPane.showMessageDialog(editSQLsourcePanel, pe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;                   
                    }
                    ((DataSource_SQL)currentDataSource).setServerAddress(baseURL);                    
                    ((DataSource_SQL)currentDataSource).setPortNumber(port);                    
                    ((DataSource_SQL)currentDataSource).setDatabaseName(databasename);
                    ((DataSource_SQL)currentDataSource).setTablename(tablename);
                    ((DataSource_SQL)currentDataSource).setUsername(username);
                    ((DataSource_SQL)currentDataSource).setPassword(passwordString);
                    ((DataSource_SQL)currentDataSource).setDBfields(fields); 
                    initializeSQLtableFromSource(null); // clear the table for next time                    
                 } else { // plugin data source
                      try {
                           JPanel configPanel=getDataSourceConfigurationPanel(cardpaneEditSourceMainCardPanel,protocol); // the configuration panel for this protocol should be stored as a child of cardpaneEditSourceMainCardPanel
                           if (configPanel!=null) currentDataSource.updateConfigurationFromPanel(configPanel);
                       } catch (Exception re) {
                           JOptionPane.showMessageDialog(cardpaneEditSourceMainCardPanel, re.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                           return;   
                       }                       
                 }                
            } else { // protocol has been changed. We need to replace the whole datasource object 
                 DataSource newsource=null;
                 if (protocol.equals(DataSource_DAS.PROTOCOL_NAME)) {
                     String baseURL=editDASsourceBaseURLtextfield.getText().trim();
                     String feature=editDASsourceFeatureTextfield.getText().trim();
                     newsource=new DataSource_DAS(currentDataTrack,organism,build,baseURL,dataformat,feature);
                 }
                 else if (protocol.equals(DataSource_http_GET.PROTOCOL_NAME)) {
                     String baseURL=editGETsourceBaseURLtextfield.getText().trim();
                     String parameter=editGETsourceParametersTextfield.getText().trim();
                     newsource=new DataSource_http_GET(currentDataTrack,organism,build,baseURL,dataformat,parameter);
                 }  
                 else if (protocol.equals(DataSource_FileServer.PROTOCOL_NAME)) {
                     String filepath=editFILEsourceFilenameTextfield.getText().trim();
                     int segmentsize=((Integer)editFILEsourceSegmentsizeSpinner.getValue()).intValue();
                     newsource=new DataSource_FileServer(currentDataTrack,organism, build, filepath, segmentsize, dataformat);                     
                 }    
                 else if (protocol.equals(DataSource_SQL.PROTOCOL_NAME)) {
                    String baseURL=editSQLsourceServerURLField.getText().trim();
                    String portString=editSQLsourcePortField.getText().trim();
                    String username=editSQLsourceUsernameField.getText().trim();
                    char[] password=editSQLsourcePasswordField.getPassword();
                    String databasename=editSQLsourceDatabaseNameField.getText().trim();
                    String tablename=editSQLsourceTableNameField.getText().trim();
                    int port=-1;
                    try {
                      if (!portString.isEmpty()) port=Integer.parseInt(portString); 
                    } catch (NumberFormatException ne) {
                        JOptionPane.showMessageDialog(editSQLsourcePanel, "The port number must be an integer", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String passwordString=new String(password);
                    ArrayList<DBfield> fields=null;
                    try {
                        fields=parseSQLsourceTable();
                    } catch (ParseError pe) {
                         JOptionPane.showMessageDialog(editSQLsourcePanel, pe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;                   
                    }
                    newsource=new DataSource_SQL(currentDataTrack,organism,build, baseURL, port, databasename, tablename, username, passwordString, fields);
                    initializeSQLtableFromSource(null); // clear the table for next time                    
                 }                   
                 else if (protocol.equals(DataSource_VOID.PROTOCOL_NAME)) {
                     newsource=new DataSource_VOID(currentDataTrack,organism,build);
                 } else {
                     Object resource=engine.getResource(protocol, "DataSource");
                     if (resource instanceof DataSource) {
                         try {
                             newsource=((DataSource)resource).getClass().newInstance();
                             newsource.initializeDataSource(currentDataTrack, organism, build, dataformat);
                             JPanel configPanel=getDataSourceConfigurationPanel(cardpaneEditSourceMainCardPanel,protocol); // the configuration panel for this protocol should be stored as a child of cardpaneEditSourceMainCardPanel
                             if (configPanel!=null) newsource.updateConfigurationFromPanel(configPanel);
                         } catch (Exception re) {
                             JOptionPane.showMessageDialog(cardpaneEditSourceMainCardPanel, re.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                             return;   
                         }
                     }                     
                 }
                 if (newsource!=null && newsource.usesStandardDataFormat()) newsource.setDataFormatSettings(dataformatSettings);
                 currentDataTrack.replaceDataSource(currentDataSource, newsource);
                 currentDataSource=newsource;                    
            }
        }
        showEditDataTrackPanel(currentDataTrack);
    }

    @Action
    public void editSourceCancelButtonClickedAction() {
        showEditDataTrackPanel(currentDataTrack);
    }
        
// ---------------------   PRIVATE CLASSES BELOW THIS LINE   ---------------------------
    
  class DisableableComboRenderer extends DefaultListCellRenderer {
        private HashSet<String> disabledItems=new HashSet<String>();
        private String previouslySelected=null;
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component c=super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
          } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
          }
          if (value!=null && disabledItems.contains(value.toString())) {
            setBackground(list.getBackground());
            setForeground(Color.LIGHT_GRAY);
          }
          setFont(list.getFont());
          setText((value == null) ? "" : value.toString());
          return c;
        }
        
        public void setItemEnabled(String item, boolean enabled) {
            if (enabled) disabledItems.add(item);
            else disabledItems.remove(item);
        }      
        
        public boolean isItemEnabled(String item) {
            return !disabledItems.contains(item);
        }
        
        public void setDisabledItems(String[] items) {
            disabledItems.clear();
            disabledItems.addAll(Arrays.asList(items));
        }
        
        public String getPreviouslySelected() {
            return previouslySelected;
        }
        public void setPreviouslySelected(String item) {
            previouslySelected=item;
        }        
        
  }  
   
    
      
private class DataTypeRenderer extends DefaultTableCellRenderer {
    private SimpleDataPanelIcon numericIcon;
    private SimpleDataPanelIcon regionIcon;
    private SimpleDataPanelIcon DNAIcon;
    public DataTypeRenderer() {
           super();
           this.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);   
           numericIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.NUMERIC_TRACK_GRAPH_ICON,null);
           numericIcon.setForegroundColor(java.awt.Color.BLUE);
           numericIcon.setBackgroundColor(null);
           regionIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.REGION_ICON,null);
           regionIcon.setForegroundColor(java.awt.Color.GREEN);
           regionIcon.setBackgroundColor(null);
           DNAIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.SEQUENCE_ICON_BASES,null);
           DNAIcon.setForegroundColor(java.awt.Color.BLACK);
           
           setOpaque(true);
    }
    
   @Override
   public void setValue(Object value) {
       if (value instanceof String) {
           String val=(String)value;
                if (val.equalsIgnoreCase(NumericDataset.getType())) {setIcon(numericIcon);setToolTipText("Numeric");}
           else if (val.equalsIgnoreCase(RegionDataset.getType())) {setIcon(regionIcon);setToolTipText("Region");}
           else if (val.equalsIgnoreCase(DNASequenceDataset.getType())) {setIcon(DNAIcon);setToolTipText("Sequence");}
       } else if (value instanceof Class) {
           Class cl=(Class)value;
                if (cl==NumericDataset.class) {setIcon(numericIcon);setToolTipText("Numeric");}
           else if (cl==RegionDataset.class) {setIcon(regionIcon);setToolTipText("Region");}
           else if (cl==DNASequenceDataset.class) {setIcon(DNAIcon);setToolTipText("Sequence");}
       }

   }
    } // end class DataTypeRenderer

private class SupportedOrganismsRendered extends DefaultTableCellRenderer {    
       public SupportedOrganismsRendered() {
           super();
       }
       @Override
       public void setValue(Object value) {
           if (value instanceof Integer) {
               setText(Organism.getCommonName(((Integer)value).intValue()));
           } else if (value instanceof int[]) {
               int[] organisms=(int[]) value;
               String text="";
               for (int i=0;i<organisms.length;i++) {
                   text+=Organism.getCommonName(organisms[i]);
                   if (i<organisms.length-1) text+=",";
               }
               setText(text); 
           } else setText("<error>");
       }
}

private class OrganismComboboxRenderer extends JLabel implements ListCellRenderer {
        public OrganismComboboxRenderer () {
            super();
            setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(java.awt.Color.WHITE);
                setForeground(list.getForeground());                
            }
            if (value==null) setText("***"); 
            if (value instanceof String) setText((String)value); 
            else {
               int organism=((Integer)value).intValue();
               String name=Organism.getCommonName(organism);
               setText(name);
            }
            return this;
        }
    
}

//    @Action
//    public void allTracksAddFromUCSCButtonClicked() {
//        UCSCtrackDialog ucscdialog = new UCSCtrackDialog(gui,null);
//        ucscdialog.setLocation(gui.getFrame().getWidth()/2-ucscdialog.getWidth()/2, gui.getFrame().getHeight()/2-ucscdialog.getHeight()/2);
//        ucscdialog.setVisible(true);
//        if (ucscdialog.isOKPressed()) {
//            DataTrack newtrack=ucscdialog.getDataTrack();
//            ucscdialog.dispose();
//            if (newtrack!=null) {
//                String newtrackname=newtrack.getName();
//                if (availableTracks.containsKey(newtrackname)) {
//                    int choice=JOptionPane.showConfirmDialog(this, "A data track named \""+newtrackname+"\" already exists.\nWould you like to replace this track?","Replace data track",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
//                    if (choice!=JOptionPane.OK_OPTION) return;                       
//                }
//                availableTracks.put(newtrack.getName(), newtrack);
//                showAllTracksPanel(); // this will setup the table from the configuration
//            }            
//        } else {
//           ucscdialog.dispose(); 
//        }     
//    }
//
//    @Action
//    public void allTracksAddFromDASRegistryClicked() {
//        DASRegistryDialog dasdialog = new DASRegistryDialog(gui);
//        dasdialog.setLocation(gui.getFrame().getWidth()/2-dasdialog.getWidth()/2, gui.getFrame().getHeight()/2-dasdialog.getHeight()/2);
//        dasdialog.setVisible(true);
//        if (dasdialog.isOKPressed()) {
//            DataTrack newtrack=dasdialog.getDataTrack();
//            dasdialog.dispose();
//            if (newtrack!=null) {
//                String newtrackname=newtrack.getName();
//                if (availableTracks.containsKey(newtrackname)) {
//                    int choice=JOptionPane.showConfirmDialog(this, "A data track named \""+newtrackname+"\" already exists.\nWould you like to replace this track?","Replace data track",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
//                    if (choice!=JOptionPane.OK_OPTION) return;                       
//                }
//                availableTracks.put(newtrack.getName(), newtrack);
//                showAllTracksPanel(); // this will setup the table from the configuration
//            }            
//        } else {
//           dasdialog.dispose(); 
//        }           
//    }

    
   private JTable getSQLsourceTable() {
       DefaultTableModel model=new DefaultTableModel(new String[]{"Property","Type","Database column","Value","Transform"}, 0);
       JTable table=new JTable(model);           
       JComboBox typeEditorCombobox = new JComboBox(new String[]{"Text","Integer","Double","Boolean"});
       table.getColumn("Type").setCellEditor(new DefaultCellEditor(typeEditorCombobox));
       table.getColumn("Transform").setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
               if (value instanceof String[]) setText(MotifLabEngine.splice((String[])value, ","));
               else setText((value!=null)?value.toString():"");
            }           
       });
       table.setFillsViewportHeight(true);
       table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
       table.setRowSelectionAllowed(true);
       table.getTableHeader().setReorderingAllowed(false);
       table.setRowHeight(18);
       table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
       addDefaultPropertiesToSQLtable(model);
       return table;
   } 
   
   private void addRowToSQLsourceTable() {
       ((DefaultTableModel)SQLsourceTable.getModel()).addRow(new Object[]{"newProperty",convertSQLclassToString(String.class),"newProperty",null,null});
        int lastrow=SQLsourceTable.getRowCount()-1;
        SQLsourceTable.editCellAt(lastrow, 0);
        TableCellEditor editor=SQLsourceTable.getCellEditor(lastrow, 0);
        editor.shouldSelectCell(new ListSelectionEvent(SQLsourceTable,lastrow,lastrow,true));
        SQLsourceTable.setRowSelectionInterval(lastrow, lastrow);
        Object value=SQLsourceTable.getValueAt(lastrow, 0);
        Component comp=editor.getTableCellEditorComponent(SQLsourceTable, value, true, lastrow, 0);
        SQLsourceTable.scrollRectToVisible(comp.getBounds());        
        if (comp instanceof JTextField) ((JTextField)comp).requestFocus();
   }
   
   private void removeSelectedRowsFromSQLsourceTable() {
       int row=SQLsourceTable.getSelectedRow();
       if (row<0) return;
       row=SQLsourceTable.convertRowIndexToModel(row);
       ((DefaultTableModel)SQLsourceTable.getModel()).removeRow(row);
   }   
   
   private void initializeSQLtableFromSource(DataSource_SQL source) {
       DefaultTableModel model=(DefaultTableModel)SQLsourceTable.getModel();
       int rows = model.getRowCount(); // remove current entries
       for(int i=rows-1;i>=0;i--){
           model.removeRow(i); 
       }       
       if (source!=null && source.getDBfields()!=null) {            
            for (DBfield field:source.getDBfields()) {
               String property=field.getPropertyName();
               Object transform=field.getTransformParameter();
               String transformString="";
               if (property.equalsIgnoreCase("chromosome") && transform instanceof Boolean && ((Boolean)transform).booleanValue()) transformString="chr-prefix";
               else if (transform instanceof String[][]) transformString=DBfield.getMapAsKeyValuePairs((String[][])transform);
               else if (transform!=null) transformString=transform.toString();
               Object explicitValue=field.getExplicitValue();    
               String explicitValueString=(explicitValue!=null)?explicitValue.toString():"";
               model.addRow(new Object[]{property,convertSQLclassToString(field.getFieldType()),field.getDBfieldName(),explicitValueString,transformString}); 
            }
       } else addDefaultPropertiesToSQLtable(model);         
   }
   
   private void addDefaultPropertiesToSQLtable(DefaultTableModel model) {     
       model.addRow(new Object[]{"chromosome",convertSQLclassToString(String.class),"chr",null,null});
       model.addRow(new Object[]{"start",convertSQLclassToString(Integer.class),"start",null,null});
       model.addRow(new Object[]{"end",convertSQLclassToString(Integer.class),"end",null,null});
       model.addRow(new Object[]{"type",convertSQLclassToString(String.class),null,"unknown",null});                 
   }
    
   private ArrayList<DBfield> parseSQLsourceTable() throws ParseError {
       ArrayList<DBfield> fields=new ArrayList<DBfield>();
       DefaultTableModel model=(DefaultTableModel)SQLsourceTable.getModel();
       int rows = model.getRowCount(); 
       boolean hasChromosome=false;
       boolean hasStart=false;
       boolean hasEnd=false;
       boolean hasType=false;
       for (int i=0;i<rows;i++){           
           String propertyName=(model.getValueAt(i,0)!=null)?model.getValueAt(i,0).toString().trim():"";
           String type=(model.getValueAt(i,1)!=null)?model.getValueAt(i,1).toString().trim():"";
           String dbColumnName=(model.getValueAt(i,2)!=null)?model.getValueAt(i,2).toString().trim():"";
           String explicitValueString=(model.getValueAt(i,3)!=null)?model.getValueAt(i,3).toString().trim():"";
           String transformString=(model.getValueAt(i,4)!=null)?model.getValueAt(i,4).toString().trim():"";
           Object explicitValue=null;
           Object transformValue=null;
           if (propertyName.isEmpty()) {
               if (type.isEmpty() && dbColumnName.isEmpty() && explicitValueString.isEmpty() && transformString.isEmpty()) continue; // just an empty line
               else throw new ParseError("Missing property name in first column for row "+(i+1));
           }
           if (propertyName.equalsIgnoreCase("chromosome") || propertyName.equalsIgnoreCase("chr")) {
               propertyName="chromosome";
               hasChromosome=true;
               if (!type.equals("Text")) throw new ParseError("Property 'chromosome' must have type 'Text'");
               if (dbColumnName.isEmpty()) throw new ParseError("'Database column' is required for property 'chromosome'");
               if (!transformString.isEmpty()) {
                   if (transformString.contains("=")) {
                       transformValue=DBfield.parseTransformMap(transformString);
                   } else {
                       transformValue=Boolean.TRUE; // any non-null value which is not a key=value map is interpreted as TRUE (i.e. "add chr-prefix")
                   }                                                        
               } 
           } else if (propertyName.equalsIgnoreCase("start")) {
               propertyName=propertyName.toLowerCase();
               hasStart=true;
               if (!type.equals("Integer")) throw new ParseError("Property 'start' must have type 'Integer'");
               if (dbColumnName.isEmpty()) throw new ParseError("'Database column' is required for property 'start'");               
           } else if (propertyName.equalsIgnoreCase("end")) {
               propertyName=propertyName.toLowerCase();
               hasEnd=true;
               if (!type.equals("Integer")) throw new ParseError("Property 'end' must have type 'Integer'");
               if (dbColumnName.isEmpty()) throw new ParseError("'Database column' is required for property 'end'");               
           } else if (propertyName.equalsIgnoreCase("type")) {
               propertyName=propertyName.toLowerCase();
               hasType=true;
               if (!type.equals("Text")) throw new ParseError("Property 'type' must have type 'Text'");
           } else if (propertyName.equalsIgnoreCase("score")) {
               propertyName=propertyName.toLowerCase();
               if (!type.equals("Double")) throw new ParseError("Property 'score' must have type 'Double'");
           } else if (propertyName.equalsIgnoreCase("strand") || propertyName.equalsIgnoreCase("orientation")) {
               propertyName="strand";
               if (!transformString.isEmpty()) {
                   if (transformString.contains("=")) {
                       transformValue=DBfield.parseTransformMap(transformString);
                   } else {
                       String[] parts=transformString.split("\\s*,\\s*");
                       if (parts.length!=2) throw new ParseError("The 'transform' for the strand-orientation property must either be two comma-separated values or a comma-separated list of key=value pairs");
                       transformValue=new String[][]{{"direct",parts[0]},{"reverse",parts[1]}};
                   }
               }
           } 
           if (dbColumnName.isEmpty() && explicitValueString.isEmpty()) throw new ParseError("Either the 'Database column' or 'Value' must be specified for property '"+propertyName+"'");                              
           if (!dbColumnName.isEmpty() && !explicitValueString.isEmpty()) throw new ParseError("'Database column' and 'Value' cannot both be specified for property '"+propertyName+"'");                              
           if (type.equals("Double")) {
               if (!explicitValueString.isEmpty()) {
                   try {explicitValue=new Double(Double.parseDouble(explicitValueString));} catch (NumberFormatException ne) {throw new ParseError("'Value' should be a number for property '"+propertyName+"'");}
               }
               if (!transformString.isEmpty()) {
                   try {transformValue=new Double(Double.parseDouble(transformString));} catch (NumberFormatException ne) {throw new ParseError("'Transform' should be a number for property '"+propertyName+"'");}
               }                           
           }
           else if (type.equals("Integer")) {
               if (!explicitValueString.isEmpty()) {
                   try {explicitValue=new Integer(Integer.parseInt(explicitValueString));} catch (NumberFormatException ne) {throw new ParseError("'Value' should be an integer number for property '"+propertyName+"'");}
               }
               if (!transformString.isEmpty()) {
                   try {transformValue=new Integer(Integer.parseInt(transformString));} catch (NumberFormatException ne) {throw new ParseError("'Transform' should be an integer number for property '"+propertyName+"'");}
               }                           
           }
           else if (type.equals("Boolean")) {
               if (!explicitValueString.isEmpty()) {
                   explicitValue=explicitValueString.equalsIgnoreCase("true") || explicitValueString.equalsIgnoreCase("yes") || explicitValueString.equalsIgnoreCase("on");
               }                          
           } else { // type equals text
               if (!explicitValueString.isEmpty()) explicitValue=explicitValueString;
               if (!(propertyName.equalsIgnoreCase("chromosome") || propertyName.equalsIgnoreCase("chr") || propertyName.equalsIgnoreCase("strand") || propertyName.equalsIgnoreCase("orientation"))) {
                   if (transformString.contains("=")) {
                       transformValue=DBfield.parseTransformMap(transformString);
                   } else {
                       if (!transformString.isEmpty()) throw new ParseError("'Transform' should be a comma-separated list of <property-value = database-value> pairs for property '"+propertyName+"'");                          
                   }    
               }
           }           
           Class classtype=convertStringToSQLclass(type);
           if (classtype==null) throw new ParseError("Unrecognized type '"+type+"' for property '"+propertyName+"'");
           DBfield field = new DBfield(propertyName, (dbColumnName.isEmpty())?null:dbColumnName, classtype, explicitValue, transformValue);
           // check if the field already exists
           for (DBfield f:fields) {
               if (f.getPropertyName().equalsIgnoreCase(field.getPropertyName())) throw new ParseError("Property '"+propertyName+"' occurs multiple times in the table");
           }
           fields.add(field);
       } 
       if (!hasChromosome) throw new ParseError("Missing required property 'chromosome' in the table");
       if (!hasStart) throw new ParseError("Missing required property 'start' in the table");
       if (!hasEnd) throw new ParseError("Missing required property 'end' in the table");
       // if (!hasType) throw new ParseError("Missing required property 'type' in the table");
       return fields;
   }
   
   private String convertSQLclassToString(Class type) {
       if (type==Integer.class) return "Integer";
       else if (type==Double.class) return "Double";
       else if (type==Boolean.class) return "Boolean";    
       else return "Text";
   }   
   private Class convertStringToSQLclass(String string) {
            if (string.equalsIgnoreCase("Integer")) return Integer.class;
       else if (string.equalsIgnoreCase("Double")) return Double.class;
       else if (string.equalsIgnoreCase("Boolean")) return Boolean.class;
       else if (string.equalsIgnoreCase("Text") || string.equalsIgnoreCase("String")) return String.class;
       else return null;
   }     
          
  /** This class handles reordering of rows in the DataSources table
    * The code was found at: http://stackoverflow.com/questions/638807/how-do-i-drag-and-drop-a-row-in-a-jtable
    */
   public class TableRowTransferHandler extends TransferHandler {
       private final DataFlavor localObjectFlavor = new DataFlavor(Integer.class, "Integer Row Index");
       private JTable table = null;

       public TableRowTransferHandler(JTable table) {
          this.table = table;
       }

       @Override
       protected Transferable createTransferable(JComponent c) {
          assert (c == table);
          return new DataHandler(new Integer(table.getSelectedRow()), localObjectFlavor.getMimeType());
       }

       @Override
       public boolean canImport(TransferHandler.TransferSupport info) {
          boolean b = info.getComponent() == table && info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
          table.setCursor(b?DragSource.DefaultMoveDrop:DragSource.DefaultMoveNoDrop);
          return b;
       }

       @Override
       public int getSourceActions(JComponent c) {
          return TransferHandler.COPY_OR_MOVE;
       }

       @Override
       public boolean importData(TransferHandler.TransferSupport info) {
          JTable target = (JTable) info.getComponent();
          JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
          int index = dl.getRow();
          int max = table.getModel().getRowCount();
          if (index < 0 || index > max)
             index = max;
          target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          try {
             Integer rowFrom = (Integer) info.getTransferable().getTransferData(localObjectFlavor);
             if (rowFrom != -1 && rowFrom != index) {
                if (index > rowFrom) index--;
                ((DefaultTableModel)table.getModel()).moveRow(rowFrom,rowFrom,index);       
                target.getSelectionModel().addSelectionInterval(index, index);
                currentDataTrack.moveDataSource(rowFrom, index);
                return true;
             }
          } catch (Exception e) {
              JOptionPane.showMessageDialog(DataTrackConfigurationDialog.this, e.getClass().getName()+":"+e.getMessage(), "Drag 'n' drop error", JOptionPane.ERROR_MESSAGE);              
              //e.printStackTrace(System.err);
          }
          return false;
       }

       @Override
       protected void exportDone(JComponent c, Transferable t, int act) {
          if (act == TransferHandler.MOVE) {
             table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
       }
   }
    
   
}
