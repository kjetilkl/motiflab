/*
 * UCSC_API_trackDialog.java
 *
 */

package org.motiflab.gui;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.motiflab.engine.MotifLabEngine;
import org.jdesktop.application.Action;
import org.motiflab.engine.GeneIDResolver;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Organism;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.datasource.DataTrack;
import org.motiflab.engine.datasource.DataSource;
import org.motiflab.engine.datasource.DataSource_UCSC;
import org.motiflab.engine.protocol.ParseError;


/**
 *
 * @author  kjetikl
 */
public class UCSC_API_trackDialog extends javax.swing.JDialog {

    private static final String apiEndPoint="https://api.genome.ucsc.edu";
    private boolean OKpressed=false;
    private boolean everythingOK=true;
    private boolean interruptFlag=false;

    private DataTrack parentDataTrack=null;
    private MotifLabGUI gui=null;
    private MotifLabEngine engine;
    
    private final String TABLECOLUMN_NAME="Name";
    private final String TABLECOLUMN_TYPE="Type";
    private final String TABLECOLUMN_DESCRIPTION="Description";
    private final String TABLECOLUMN_GROUP="Group";
    private JTable ucscTrackTable;
    private DefaultTableModel ucscTrackTableModel;
    

    private RowFilter<Object,Object> expressionRowFilter=null;  
    
    private GeneIDResolver idResolver;

    private JTextField trackNameField;
    private JComboBox organismCombobox;
    private JComboBox genomeBuildCombobox;
    private JTextField filterTextfield;    
    private JScrollPane scrollPane;
    private JButton okButton;
    private JButton cancelButton;
    private JProgressBar progressBar;
    
    private Object[] selectedTrack; //  [0]=UCSC track name, [1]=Dataset class, [2] = Description
    private HashMap<String,String> regionAttributes=null;

    

    /** Creates new UCSCtrackDialog
     * @parent A parent frame
     * @datatrack A parent datatrack (optional). If this is set then the the trackname will not be editable but be based on this track. Also if the selected source is incompatible with the track, a warning message will be displayed
     */
    public UCSC_API_trackDialog(MotifLabGUI gui, DataTrack datatrack) {
        super(gui.getFrame(), true);
        this.gui=gui;
        this.engine=gui.getEngine();
        parentDataTrack=datatrack;
        idResolver=gui.getEngine().getGeneIDResolver();
        initComponents();
        setupTracksTable();
        if (parentDataTrack==null) setTitle("Select Track from UCSC Genome Browser");
        else setTitle("Add source for "+parentDataTrack.getName()+" from UCSC Genome Browser");
        
        if (idResolver.isOrganismSupported(9606)) { // select human as default. This can be changed later if settings have been saved
           organismCombobox.setSelectedItem(Organism.getCommonName(9606));
        } else organismCombobox.setSelectedIndex(0);         
        // updateTable((String)genomeBuildCombobox.getSelectedItem(), parentDataTrack);
    }

   
    private void initComponents() {
        JPanel mainPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();  
        
        trackNameField=new JTextField();
        trackNameField.setColumns(22);
        trackNameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackNameTextfieldActionPerformed(evt);
            }
        });
        JPanel jPanel2 = new JPanel();
        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 5, 6, 10));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5)); 
        jPanel2.add(new JLabel("Track name"));
        jPanel2.add(trackNameField);

        JPanel topPanel = new JPanel();
        topPanel.setMinimumSize(new java.awt.Dimension(100, 46));
        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(100, 46));
        topPanel.setLayout(new java.awt.BorderLayout());        
        topPanel.add(jPanel2, java.awt.BorderLayout.WEST);
                            
        genomeBuildCombobox = new javax.swing.JComboBox();
        genomeBuildCombobox.addActionListener((ActionEvent e) -> {
            String genomebuild=(String)genomeBuildCombobox.getSelectedItem();
            updateTable(genomebuild,parentDataTrack);
        });
        
        int[] supportedOrganism=idResolver.getSupportedOrganisms();
        String[] organismNames=new String[supportedOrganism.length];
        for (int i=0;i<supportedOrganism.length;i++) {
            organismNames[i]=Organism.getCommonName(supportedOrganism[i]);
        }
        Arrays.sort(organismNames); 
        organismCombobox = new javax.swing.JComboBox();
        organismCombobox.setModel(new DefaultComboBoxModel(organismNames));
        organismCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String organismname=(String)organismCombobox.getSelectedItem();
                int organismID=Organism.getTaxonomyID(organismname);
                String[] supportedBuilds=Organism.getSupportedGenomeBuilds(organismID);
                if (supportedBuilds!=null && supportedBuilds.length>0) {
                    genomeBuildCombobox.setModel(new DefaultComboBoxModel(supportedBuilds));
                    genomeBuildCombobox.setEnabled(true);  
                    genomeBuildCombobox.setSelectedIndex(0);
                } else {                 
                    genomeBuildCombobox.setEnabled(false);
                }
            }
        });
        
        JPanel genomePanel = new JPanel();
        genomePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 5, 6, 10));
        genomePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5)); 
        genomePanel.add(new JLabel("Organism "));  
        genomePanel.add(organismCombobox); 
        genomePanel.add(new JLabel("Genome build "));
        genomePanel.add(genomeBuildCombobox); 
        topPanel.add(genomePanel, java.awt.BorderLayout.CENTER);        
                
        JPanel buttonsPanel = new JPanel();                
        buttonsPanel.setMinimumSize(new java.awt.Dimension(100, 90));
        buttonsPanel.setPreferredSize(new java.awt.Dimension(100, 42));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        JLabel filterLabel=new JLabel("Filter");
        filterLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 0));
        jPanel3.add(filterLabel);

        filterTextfield = new JTextField();
        filterTextfield.setColumns(18);
        filterTextfield.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                keyReleasedInFilterTextfield(evt);
            }
        });
        jPanel3.add(filterTextfield);
        
        buttonsPanel.add(jPanel3, java.awt.BorderLayout.WEST);

        JPanel jPanel5 = new JPanel();
        jPanel5.setLayout(new BorderLayout());   
        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 20, 8, 20));        
        progressBar = new JProgressBar(0,100);
        jPanel5.add(progressBar, java.awt.BorderLayout.CENTER);
        buttonsPanel.add(jPanel5, java.awt.BorderLayout.CENTER);
                
        JPanel jPanel4 = new JPanel();
        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getActionMap(UCSC_API_trackDialog.class, this);
        okButton = new JButton();
        okButton.setMaximumSize(new java.awt.Dimension(75, 27));
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton");
        okButton.setAction(actionMap.get("OKAction")); // NOI18N
        okButton.setText("OK");
        okButton.setEnabled(false);
        jPanel4.add(okButton);

        cancelButton = new JButton();
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setAction(actionMap.get("cancelAction")); // NOI18N        
        cancelButton.setText("Cancel");
        jPanel4.add(cancelButton);

        buttonsPanel.add(jPanel4, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        mainPanel.setMinimumSize(new java.awt.Dimension(800, 450));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(800, 450));
        mainPanel.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);        
        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);
        getRootPane().setDefaultButton(okButton);       
        pack();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);    
        if (parentDataTrack!=null) {
            trackNameField.setText(parentDataTrack.getName());
            trackNameField.setEditable(false);
            trackNameField.setCaretPosition(0);
        }
    }
    
    private void setupTracksTable() {
        ucscTrackTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_TYPE, TABLECOLUMN_DESCRIPTION},0);
        ucscTrackTable=new JTable(ucscTrackTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };

        ucscTrackTable.setFillsViewportHeight(true);
        ucscTrackTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ucscTrackTable.setRowSelectionAllowed(true);
        ucscTrackTable.getTableHeader().setReorderingAllowed(false);
        ucscTrackTable.getColumn(TABLECOLUMN_TYPE).setMinWidth(50);
        ucscTrackTable.getColumn(TABLECOLUMN_TYPE).setMaxWidth(50);
        ucscTrackTable.getColumn(TABLECOLUMN_TYPE).setCellRenderer(new DataTypeRenderer());
        ucscTrackTable.getColumn(TABLECOLUMN_DESCRIPTION).setCellRenderer(new DescriptionRenderer());        
        ucscTrackTable.setRowHeight(18);

        ucscTrackTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int selectedRow=ucscTrackTable.getSelectedRow();
                if (selectedRow>=0) {
                    if (trackNameField.isEditable()) {
                        int modelRow=ucscTrackTable.convertRowIndexToModel(selectedRow);
                        String trackName=(String)ucscTrackTable.getModel().getValueAt(modelRow, 0);
                        trackNameField.setText(trackName);
                        trackNameField.setCaretPosition(0);
                    }
                    okButton.setEnabled(true);  
                } else {
                   okButton.setEnabled(false);  
                }
            }
        });
        ucscTrackTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
                   okButton.doClick();
               }
            }
        });        
        
        scrollPane.setViewportView(ucscTrackTable);

        // Disable OK-button until a valid selection is made
        okButton.setEnabled(false);        
        ucscTrackTable.setAutoCreateRowSorter(true);
        ucscTrackTable.getRowSorter().toggleSortOrder(ucscTrackTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        filterTextfield.requestFocusInWindow();
        ActionListener escapeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interruptFlag=true;
            }
        };
        rootPane.registerKeyboardAction(
                escapeListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );        
    }
                             

    @SuppressWarnings("unchecked")
    private void keyReleasedInFilterTextfield(java.awt.event.KeyEvent evt) {                                              
        String text=filterTextfield.getText();                                             
        int column=ucscTrackTableModel.findColumn(TABLECOLUMN_DESCRIPTION);
        if (text!=null && text.isEmpty()) expressionRowFilter=null;
        else {
            text=text.replaceAll("\\W", ""); // to avoid problems with regex characters
            expressionRowFilter=RowFilter.regexFilter("(?i)"+text,column);
        }
        ((TableRowSorter)ucscTrackTable.getRowSorter()).setRowFilter(expressionRowFilter);
    } 
    
    private void trackNameTextfieldActionPerformed(java.awt.event.ActionEvent evt) {                                                  
    // TODO add your handling code here:
    }         
    
    
    @SuppressWarnings("unchecked")
    private void updateTable(String genomebuild, DataTrack track) {
        if (genomebuild==null) return;     
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        Class filter=(track!=null)?track.getDataType():null;
        SwingWorker worker=new SwingWorker<Void, Void>() {
            Exception ex=null;
            @Override
            public Void doInBackground() {
                try {
                    getTracksFromUCSC(genomebuild, filter);
                } catch (InterruptedException ie) {
                    //
                } catch (Exception e) {
                    ex=e;
                }
                return null;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressBar.setIndeterminate(false);
                if (ex!=null) {
                    String message=ex.getMessage();
                    progressBar.setString(message);
                    everythingOK=false;
                    okButton.setEnabled(false);
                } else {
                    // progressBar.setVisible(false);
                    progressBar.setValue(100);
                    int count = ucscTrackTableModel.getRowCount();
                    progressBar.setString("Found "+count+" track"+((count!=1)?"s":""));
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }
    
    
    @Action
    public void OKAction() {   
        if (progressBar.isIndeterminate()) interruptFlag=true;
        String trackname=trackNameField.getText().trim();
        if (trackname.isBlank()) {
            reportError("You must specify a name for the Data Track");
            return;
        }
        if (!trackname.matches("[\\w\\-]+")) {
            reportError("Track name contains illegal characters (only letters, numbers, underscores and hyphens allowed)");
            return;
        }
        int selectedRow=ucscTrackTable.getSelectedRow();
        if (selectedRow>=0) {
            int modelRow=ucscTrackTable.convertRowIndexToModel(selectedRow);
            String trackName=(String)ucscTrackTable.getModel().getValueAt(modelRow, 0);
            Class trackType=(Class)ucscTrackTable.getModel().getValueAt(modelRow, 1);  
            String description=(String)ucscTrackTable.getModel().getValueAt(modelRow, 2);             
            selectedTrack=new Object[]{trackName,trackType,description};           
            if (trackType==RegionDataset.class) {
                configureAttributes((String)genomeBuildCombobox.getSelectedItem(), trackName); // For region datasets we need to know the names of the attributes to use for start/end, type, score and strand
            }
            OKpressed=true;
        } else {
           OKpressed=false;
           everythingOK=false;
           selectedTrack=null;
        }        
        setVisible(false);
    }

    @Action
    public void cancelAction() {
        OKpressed=false;
        interruptFlag=true;
        setVisible(false);
    }

    public boolean isOKPressed() {
        return OKpressed && everythingOK;
    }

    private void reportError(String message) {
       JOptionPane.showMessageDialog(this, message, "Error",JOptionPane.ERROR_MESSAGE);
    }
    
    public void configureAttributes(String genomeBuild, String trackname) {
        UCSC_API_RegionAttributesDialog attributesDialog = new UCSC_API_RegionAttributesDialog(gui, genomeBuild, trackname);
        attributesDialog.setLocation(gui.getFrame().getWidth()/2-attributesDialog.getWidth()/2, gui.getFrame().getHeight()/2-attributesDialog.getHeight()/3);
        attributesDialog.setVisible(true);
        if (attributesDialog.isOKPressed()) {
            regionAttributes=attributesDialog.getSelectedAttributes();
            attributesDialog.dispose();
        } else {
           attributesDialog.dispose();
        }                
    }

    /**
     * This method is used to retrieve a new DataTrack object based on the selections made in the dialog
     * the returned DataTrack object contains all necessary information to register a new track
     * @return DataTrack
     */
    public DataTrack getDataTrack() {
        if (!everythingOK || selectedTrack==null) return null;
        Class type=(Class)selectedTrack[1];

        String trackname=trackNameField.getText().trim();
        String build=(String)genomeBuildCombobox.getSelectedItem(); 
        int organismID=Organism.getOrganismForGenomeBuild(build);
        DataTrack datatrack=new DataTrack(trackname, type, "UCSC Genome Browser", (String)selectedTrack[2]);
        DataSource_UCSC source=new DataSource_UCSC(datatrack, organismID, build);
        if (type==RegionDataset.class || type==NumericDataset.class) {
           source.setParameter("track", (String)selectedTrack[0]);
        }        
        if (type==RegionDataset.class) {
            if (regionAttributes==null) {
                reportError("Missing attribute configuration for region track");
                return null;
            }
            for (String attribute:regionAttributes.keySet()) {
                source.setParameter(attribute, regionAttributes.get(attribute));
            }
        }
        datatrack.addDataSource(source);
        return datatrack;
    }

    /**
     * This method is used to retrieve a new DataSource object based on the selections made in the dialog
     * The source should be added to a preexisting datatrack (it is not automatically added to the argument track)
     * @param datatrack The parent datatrack for this source
     * @return DataSource
     */
    public DataSource getDataSource(DataTrack datatrack) {
        if (!everythingOK || selectedTrack==null || datatrack==null) return null;
        Class type=datatrack.getDataType();
        if (type==null) return null;
        String build=(String)genomeBuildCombobox.getSelectedItem(); 
        int organismID=Organism.getOrganismForGenomeBuild(build);
        DataSource_UCSC source=new DataSource_UCSC(datatrack, organismID, build);
        if (type==RegionDataset.class || type==NumericDataset.class) {
           source.setParameter("track", (String)selectedTrack[0]);
        }        
        if (type==RegionDataset.class) {
            if (regionAttributes==null) {
                reportError("Missing attribute configuration for region track");
                return null;
            }
            for (String attribute:regionAttributes.keySet()) {
                source.setParameter(attribute, regionAttributes.get(attribute));
            }
        }        
        return source;
    }
           

    /**
     * Retrieve tracks from the UCSC Genome Browser API. 
     * Tracks will be added to the table as they are parsed
     * @param genome
     * @param trackType
     * @throws InterruptedException 
     */
    private void getTracksFromUCSC(String genome, Class trackType) throws InterruptedException, IOException, ParseError { 
        ucscTrackTableModel.setRowCount(0); // clear current contents of the table
        URL url = null; 
        try {
            url = new URL(apiEndPoint+"/list/tracks?genome="+genome+";trackLeavesOnly=1");        
        } catch (MalformedURLException ex) {}
        try (InputStream in = url.openStream()) {
            JsonFactory factory = new JsonFactory();           
            JsonParser parser = factory.createParser(in);
            int progress=0;
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) break;
               
                if (JsonToken.FIELD_NAME.equals(token) && genome.equals(parser.currentName())) {
                    // this genome is supported by UCSC. Add a DNA track manually (unless filtered)
                    if (trackType==null || trackType==DNASequenceDataset.class) {
                        Object[] dnaTrack=new Object[]{"DNA",DNASequenceDataset.class,"Genomic DNA sequence"};
                        addTrackToTable(dnaTrack); 
                        if (trackType==DNASequenceDataset.class) { // no need to look for more tracks. We have all we need!
                            parser.close();
                            return; 
                        }
                    }
                    token=parser.nextToken(); // move to START_OBJECT
                    if (!JsonToken.START_OBJECT.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected start of object but got "+token);
                    token=parser.nextToken(); // this should now be FIELD_NAME or END_OBJECT
                    while (token != JsonToken.END_OBJECT) {                            
                        Object[] trackData = parseTrack(token, parser, trackType);
                        if (trackData!=null) addTrackToTable(trackData);                          
                        token=parser.nextToken(); // this should now be the name of a new track (FIELD) or END_OBJECT of the whole thing
                        progress++;
                        if (progress%100==0) {
                            if (Thread.interrupted() || interruptFlag) {interruptFlag=false;throw new InterruptedException();}
                            if (progress>=500) {
                                Thread.yield();
                                progress=0;
                            }
                        }                         
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }
    
    private void addTrackToTable(Object[] trackData) throws InterruptedException{
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    ucscTrackTableModel.addRow(trackData);
                    int count=ucscTrackTableModel.getRowCount();
                    progressBar.setString("Found "+count+" tracks...");
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            interruptFlag=false;throw new InterruptedException();
        }          
    }
    
    
    private Object[] parseTrack(JsonToken token, JsonParser parser, Class filter) throws ParseError, IOException {
        if (!JsonToken.FIELD_NAME.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected Field name but got "+token);
        String trackName = parser.getValueAsString();
        token=parser.nextToken(); // this should be START_OBJECT
        if (!JsonToken.START_OBJECT.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected start of object but got "+token);
        token=parser.nextToken(); // this should now be FIELD_NAME or END_OBJECT
        String shortLabel="", longLabel="", group="";
        Class type=null;
        while (!JsonToken.END_OBJECT.equals(token)) {
            if (JsonToken.FIELD_NAME.equals(token)) {
                String field=parser.getValueAsString();
                token=parser.nextToken(); // this is now the corresponding value
                if (field.equals("type")) {
                   if (!JsonToken.VALUE_STRING.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected string value for field but got "+token);
                   String typeField=parser.getValueAsString();
                   type=getTrackTypeFromFormat(typeField);
                } else if (field.equals("shortLabel")) {
                   if (!JsonToken.VALUE_STRING.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected string value for field but got "+token);
                   shortLabel=parser.getValueAsString();                                 
                } else if (field.equals("longLabel")) {
                   longLabel=parser.getValueAsString();                                  
                } else if (field.equals("group")) {
                   if (!JsonToken.VALUE_STRING.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected string value for field but got "+token);
                   group=parser.getValueAsString();
                }  
            } else throw new ParseError("Unexpected token in JSON file. Expected name of field but got "+token);
            token=parser.nextToken(); // this should now either be a new field or the end of the object
        }
        if (type==null || (filter!=null && type!=filter)) return null;
        // System.err.println("Track ["+trackName+"] = '"+shortLabel+"' ("+longLabel+") type=["+type+"] group=["+group+"] ");
        return new Object[]{trackName,type,longLabel};
    } 
    
    private Class getTrackTypeFromFormat(String format) {
        if (format.startsWith("bed ") || format.startsWith("bigBed") || format.equals("rmsk")) return RegionDataset.class;
        else if (format.startsWith("wig ") || format.startsWith("bigWig")) return NumericDataset.class;
        else return null;
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
        String tooltip=null;
        if (value instanceof String) {
            String val=(String)value;
                 if (val.equalsIgnoreCase(NumericDataset.getType())) {setIcon(numericIcon);tooltip="Numeric Dataset";}
            else if (val.equalsIgnoreCase(RegionDataset.getType())) {setIcon(regionIcon);tooltip="Region Dataset";}
            else if (val.equalsIgnoreCase(DNASequenceDataset.getType())) {setIcon(DNAIcon);tooltip="DNA Sequence Dataset";}
        } else if (value instanceof Class) {
            Class cl=(Class)value;
                 if (cl==NumericDataset.class) {setIcon(numericIcon);tooltip="Numeric Dataset";}
            else if (cl==RegionDataset.class) {setIcon(regionIcon);tooltip="Region Dataset";}
            else if (cl==DNASequenceDataset.class) {setIcon(DNAIcon);tooltip="DNA Sequence Dataset";}
        }
        setToolTipText(tooltip);
    }

}

private class DescriptionRenderer extends DefaultTableCellRenderer {
    public DescriptionRenderer() {
       super();
    }
    @Override
    public void setValue(Object value) {
        super.setValue(value);
        String tooltip=(String)value;
        setToolTipText((tooltip==null)?null:MotifLabGUI.formatTooltipString((String)tooltip,80));
    }
}

}
