/*
 * SequenceInputDialog.java
 *
 * Created on 3. april 2009, 10:21
 */

package org.motiflab.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.AbstractCellEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import org.motiflab.engine.task.AddSequencesTask;
import org.motiflab.engine.GeneIDResolver;
import org.motiflab.engine.data.Organism;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.GeneIDmapping;
import org.motiflab.engine.GeneIdentifier;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author  kjetikl
 */
public class SequenceInputDialog extends javax.swing.JDialog {
    
    private static String lastSelectedGenomeBuild=null;
    private static String lastSelectedOrganism=null;
    
    private JTable advancedInputTable;
//    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private DefaultTableModel advancedTableModel;
    private int MAX_SEQUENCE_LENGTH=10000;
    private GeneIDResolver idResolver;
    private int exampleShown=-1;
    private boolean smdDialogCancelled=false; // set when the user selects CANCEL in the selectMappingDialog. This should cancel all resolves
    private boolean autoCorrectNames=false;
    private AddSequencesTask addSequencesTask=null;

    /** Creates new form SequenceInputDialog */
    public SequenceInputDialog(MotifLabEngine engine) {
        super((engine.getClient() instanceof MinimalGUI)?((MinimalGUI)engine.getClient()).getFrame():((MotifLabGUI)engine.getClient()).getFrame(), true);
        this.engine=engine;      
        this.idResolver=engine.getGeneIDResolver();
        this.MAX_SEQUENCE_LENGTH=engine.getMaxSequenceLength();
        autoCorrectNames=engine.autoCorrectSequenceNames();        
        initComponents();       
        if (engine.getClient() instanceof MinimalGUI) { // remove some GUI elements when used by the minimalGUI client
            bottomPanel.remove(loadDNAtrackPanel);
            additionalOptionsPanel.remove(includeSequenceInProtocolCheckbox);
            bottomPanel.setPreferredSize(new Dimension(118, 56));
        }
        smdList.addMouseListener(new MouseAdapter() { // I have to add this here because of a bug in NetBeans which will f*ck up the code if I add it to the 'design'
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) selectMappingDialog.setVisible(false);
            }
        });
        smdCancelButton.addActionListener(new ActionListener() { // I have to add this here because of a bug in NetBeans which will f*ck up the code if I add it to the 'design'
            @Override
            public void actionPerformed(ActionEvent e) {
                smdDialogCancelled=true;
                selectMappingDialog.setVisible(false);
            }
        });
        importButton.addActionListener(new ActionListener() { // I have to add this here because of a bug in NetBeans which will f*ck up the code if I add it to the 'design'
            @Override
            public void actionPerformed(ActionEvent e) {
                importManualEntriesFromFile();
            }
        });
        progressbar.setVisible(false);
        Object[] columnNames=new Object[]{"Name","Organism","Build","Chr","Start","End","Gene name","Gene TSS","Gene TES","Orientation"};
        advancedTableModel=new DefaultTableModel(columnNames,1);
        advancedInputTable=new JTable(advancedTableModel);
        ExcelAdapter adapter=new ExcelAdapter(advancedInputTable,true, ExcelAdapter.CONVERT_TO_DOUBLE_OR_INTEGER); // enables copy/paste capabilities in the table
        advancedInputTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        advancedInputTable.setFillsViewportHeight(true);
        advancedInputTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        advancedInputTable.setCellSelectionEnabled(true);
        advancedInputTable.getTableHeader().setReorderingAllowed(false);
        tableScrollPane.setViewportView(advancedInputTable);
        Object[] dnaSourceList=engine.getDataLoader().getAvailableDatatracks(DNASequenceDataset.class);
        dnaSequenceSourceCombobox.setModel(new DefaultComboBoxModel(dnaSourceList));
        DefaultTableCellRenderer organismRenderer=new SequenceTableRenderer_Organism();
        DefaultTableCellRenderer orientationRenderer=new SequenceTableRenderer_Orientation();
        DefaultTableCellRenderer rightRenderer=new SequenceTableRenderer_RightAlign();
        DefaultTableCellRenderer numberRenderer=new SequenceTableRenderer_number();
        advancedInputTable.getColumn("Organism").setCellRenderer(organismRenderer);
        advancedInputTable.getColumn("Orientation").setCellRenderer(orientationRenderer);
        advancedInputTable.getColumn("Chr").setCellRenderer(rightRenderer);
        advancedInputTable.getColumn("Start").setCellRenderer(numberRenderer);
        advancedInputTable.getColumn("End").setCellRenderer(numberRenderer);
        advancedInputTable.getColumn("Gene TSS").setCellRenderer(numberRenderer);
        advancedInputTable.getColumn("Gene TES").setCellRenderer(numberRenderer);
        advancedInputTable.getColumn("Organism").setCellEditor(new CellEditor_Organism());
        advancedInputTable.getColumn("Orientation").setCellEditor(new CellEditor_Orientation());
        advancedInputTable.getModel().addTableModelListener(new TableChangeListener());
        identifierTypeCombobox.setModel(new DefaultComboBoxModel(idResolver.getSupportedIDFormats()));
        if (idResolver.isIDFormatSupported("Ensembl Gene")) identifierTypeCombobox.setSelectedItem("Ensembl Gene");
        int[] supportedOrganism=idResolver.getSupportedOrganisms();
        String[] organismNames=new String[supportedOrganism.length];
        for (int i=0;i<supportedOrganism.length;i++) {
            organismNames[i]=Organism.getCommonName(supportedOrganism[i]);
        }
        Arrays.sort(organismNames);
        organismCombobox.setModel(new DefaultComboBoxModel(organismNames));
        organismCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String organismname=(String)organismCombobox.getSelectedItem();
                int organismID=Organism.getTaxonomyID(organismname);
                String[] supportedBuilds=Organism.getSupportedGenomeBuilds(organismID);
                if (supportedBuilds!=null) {
                    genomeBuildCombobox.setModel(new DefaultComboBoxModel(supportedBuilds));
                    genomeBuildCombobox.setEnabled(true);
                    lastSelectedOrganism=organismname;                    
                } else {                 
                    genomeBuildCombobox.setEnabled(false);
                    lastSelectedOrganism=null;
                }
            }
        });
        if (idResolver.isOrganismSupported(9606)) { // select human as default. This can be changed later if settings have been saved
           organismCombobox.setSelectedItem(Organism.getCommonName(9606));
        } else organismCombobox.setSelectedIndex(0);
        genomeBuildCombobox.addActionListener((ActionEvent e) -> {
            lastSelectedGenomeBuild=(String)genomeBuildCombobox.getSelectedItem();
        });     
        
        organismComboboxBED.setModel(new DefaultComboBoxModel(organismNames));
        organismComboboxBED.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String organismname=(String)organismComboboxBED.getSelectedItem();
                int organismID=Organism.getTaxonomyID(organismname);
                String[] supportedBuilds=Organism.getSupportedGenomeBuilds(organismID);
                if (supportedBuilds!=null) {
                    genomeBuildComboboxBED.setModel(new DefaultComboBoxModel(supportedBuilds));
                    genomeBuildComboboxBED.setEnabled(true);
                    lastSelectedOrganism=organismname;                    
                } else {
                    genomeBuildComboboxBED.setEnabled(false);
                    lastSelectedOrganism=null;                    
                }
            }
        });

        if (idResolver.isOrganismSupported(9606)) { // select human as default. This can be changed later if settings have been saved
           organismComboboxBED.setSelectedItem(Organism.getCommonName(9606));
        } else organismComboboxBED.setSelectedIndex(0);
              
        genomeBuildComboboxBED.addActionListener((ActionEvent e) -> {
            lastSelectedGenomeBuild=(String)genomeBuildComboboxBED.getSelectedItem();
        });
        
        exampleBEDbutton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showBEDExample();
            }
        });
        importBEDbutton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                importBEDbuttonPressed();
            }
        });
        genomeBuildCombobox.setToolTipText("<html><img src=\""+getClass().getResource("/org/motiflab/gui/resources/icons/warning16.png")+"\"><br> <font color=red><b>NOTE:</b></font> MotifLab queries various Ensembl BioMart services<br> to determine the genomic locations of genes. By default the most<br> recent BioMart is queried, but some older genome builds might require<br> the use of older archived BioMart services. Be sure to use the correct<br> version of BioMart for each genome build! These can be set by selecting<br>\"Configure Organisms and Identifiers\" from the \"Configure\" menu. </html>");

        // initialize with settings used on previous occasion
        try {
            Object settingsObject=engine.loadSystemObject("SequenceDialogSettings.ser");
            if (settingsObject instanceof String[]) {
                String[] settings=(String[])settingsObject;
                if (settings.length>0 && settings[0]!=null) identifierTypeCombobox.setSelectedItem(settings[0]);
                if (settings.length>1 && settings[1]!=null) organismCombobox.setSelectedItem(settings[1]);
                if (settings.length>2 && settings[2]!=null) genomeBuildCombobox.setSelectedItem(settings[2]);
                if (settings.length>3 && settings[3]!=null) organismComboboxBED.setSelectedItem(settings[3]);
                if (settings.length>4 && settings[4]!=null) genomeBuildComboboxBED.setSelectedItem(settings[4]);
                if (settings.length>5 && settings[5]!=null) {
                    if (settings[5].equals("BEDtab")) tabPanel.setSelectedComponent(BEDtab);
                    else if (settings[5].equals("GeneIDsTab")) tabPanel.setSelectedComponent(GeneIDsTab);
                    else if (settings[5].equals("CoordinatesTab")) tabPanel.setSelectedComponent(CoordinatesTab);
 
                }                
            }
        } catch (Exception e) {}
    }


    private ComboBoxModel getIDtypeModel() {
        return new DefaultComboBoxModel(new String[]{"Ensembl Gene"}); // this is just a default which is not used!
    }

    public void disableAutoFetchDNA() {
        loadDNAtrackCheckbox.setSelected(false);
        loadDNAtrackCheckbox.setVisible(false);
        includeSequenceInProtocolCheckbox.setSelected(false);
        includeSequenceInProtocolCheckbox.setVisible(false);
        dnaSequenceSourceCombobox.setVisible(false);
        dnaTrackNameTextfield.setVisible(false);
        fromLabel.setVisible(false);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {

        fileOrInput = new javax.swing.ButtonGroup();
        selectMappingDialog = new javax.swing.JDialog();
        smdTopPanel = new javax.swing.JPanel();
        smdMessageLabel = new javax.swing.JLabel();
        smdButtonsPanel = new javax.swing.JPanel();
        smdOkButton = new javax.swing.JButton();
        smdCancelButton = new javax.swing.JButton();
        smdMiddlePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        smdList = new javax.swing.JList();
        bottomPanel = new javax.swing.JPanel();
        loadDNAtrackPanel = new javax.swing.JPanel();
        loadDNAtrackCheckbox = new javax.swing.JCheckBox();
        dnaTrackNameTextfield = new javax.swing.JTextField();
        fromLabel = new javax.swing.JLabel();
        dnaSequenceSourceCombobox = new javax.swing.JComboBox();
        additionalOptionsPanel = new javax.swing.JPanel();
        includeSequenceInProtocolCheckbox = new javax.swing.JCheckBox();
        skipUnrecognizedCheckbox = new javax.swing.JCheckBox();
        helpIconLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        errorMessageLabel = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        topPanel = new javax.swing.JPanel();
        tabPanel = new javax.swing.JTabbedPane();
        GeneIDsTab = new javax.swing.JPanel();
        geneIDsTopPanel = new javax.swing.JPanel();
        topBoxPanel2 = new javax.swing.JPanel();
        importIDsPanel = new javax.swing.JPanel();
        enteridslabel = new javax.swing.JLabel();
        helpPanel = new javax.swing.JPanel();
        browseButton = new javax.swing.JButton();
        helpIconButton = new javax.swing.JButton();
        topBoxPanel1 = new javax.swing.JPanel();
        geneIDtypePanel = new javax.swing.JPanel();
        geneIDtypeLabel = new javax.swing.JLabel();
        identifierTypeCombobox = new javax.swing.JComboBox();
        genomeBuildPanel = new javax.swing.JPanel();
        organismLabel = new javax.swing.JLabel();
        organismCombobox = new javax.swing.JComboBox();
        genomeBuildLabel = new javax.swing.JLabel();
        genomeBuildCombobox = new javax.swing.JComboBox();
        textAreaPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        geneIdentifiersTextarea = new javax.swing.JTextArea();
        sequenceLocationPanel = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        includeGOcheckbox = new javax.swing.JCheckBox();
        jPanel14 = new javax.swing.JPanel();
        anchorLabel = new javax.swing.JLabel();
        anchorPointComboBox = new javax.swing.JComboBox();
        upstreamLabel = new javax.swing.JLabel();
        upstreamSpinner = new javax.swing.JSpinner();
        downstreamLabel = new javax.swing.JLabel();
        downstreamSpinner = new javax.swing.JSpinner();
        CoordinatesTab = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        tableScrollPane = new javax.swing.JScrollPane();
        jPanel17 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        importButton = new javax.swing.JButton();
        advancedExampleButton = new javax.swing.JButton();
        jPanel18 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        BEDtab = new javax.swing.JPanel();
        BEDtabControlsPanel = new javax.swing.JPanel();
        BEDtop1 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        organismComboboxBED = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        genomeBuildComboboxBED = new javax.swing.JComboBox();
        jPanel15 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        BEDcoordinateSystemCombobox = new javax.swing.JComboBox();
        assignNewNamesToBEDregionsCheckbox = new javax.swing.JCheckBox();
        BEDtop2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        importBEDbutton = new javax.swing.JButton();
        exampleBEDbutton = new javax.swing.JButton();
        jPanel16 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        BEDtabMainPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        BEDTextArea = new javax.swing.JTextArea();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(SequenceInputDialog.class);
        selectMappingDialog.setTitle(resourceMap.getString("selectMappingDialog.title")); // NOI18N
        selectMappingDialog.setModal(true);
        selectMappingDialog.setName("selectMappingDialog"); // NOI18N

        smdTopPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 1, 1));
        smdTopPanel.setName("smdTopPanel"); // NOI18N
        smdTopPanel.setPreferredSize(new java.awt.Dimension(100, 60));
        smdTopPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        smdMessageLabel.setText(resourceMap.getString("smdMessageLabel.text")); // NOI18N
        smdMessageLabel.setName("smdMessageLabel"); // NOI18N
        smdTopPanel.add(smdMessageLabel);

        selectMappingDialog.getContentPane().add(smdTopPanel, java.awt.BorderLayout.PAGE_START);

        smdButtonsPanel.setMinimumSize(new java.awt.Dimension(100, 46));
        smdButtonsPanel.setName("smdButtonsPanel"); // NOI18N
        smdButtonsPanel.setPreferredSize(new java.awt.Dimension(100, 46));

        smdOkButton.setText(resourceMap.getString("smdOkButton.text")); // NOI18N
        smdOkButton.setName("smdOkButton"); // NOI18N
        smdOkButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                smdOKclicked(evt);
            }
        });

        smdCancelButton.setText(resourceMap.getString("smdCancelButton.text")); // NOI18N
        smdCancelButton.setName("smdCancelButton"); // NOI18N

        javax.swing.GroupLayout smdButtonsPanelLayout = new javax.swing.GroupLayout(smdButtonsPanel);
        smdButtonsPanel.setLayout(smdButtonsPanelLayout);
        smdButtonsPanelLayout.setHorizontalGroup(
            smdButtonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, smdButtonsPanelLayout.createSequentialGroup()
                .addContainerGap(127, Short.MAX_VALUE)
                .addComponent(smdOkButton, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(smdCancelButton)
                .addContainerGap())
        );
        smdButtonsPanelLayout.setVerticalGroup(
            smdButtonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, smdButtonsPanelLayout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addGroup(smdButtonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(smdCancelButton)
                    .addComponent(smdOkButton))
                .addContainerGap())
        );

        selectMappingDialog.getContentPane().add(smdButtonsPanel, java.awt.BorderLayout.PAGE_END);

        smdMiddlePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        smdMiddlePanel.setName("smdMiddlePanel"); // NOI18N
        smdMiddlePanel.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        smdList.setFont(resourceMap.getFont("smdList.font")); // NOI18N
        smdList.setForeground(resourceMap.getColor("smdList.foreground")); // NOI18N
        smdList.setName("smdList"); // NOI18N
        jScrollPane1.setViewportView(smdList);

        smdMiddlePanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        selectMappingDialog.getContentPane().add(smdMiddlePanel, java.awt.BorderLayout.CENTER);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(750, 450));
        setName("Form"); // NOI18N

        bottomPanel.setMinimumSize(new java.awt.Dimension(118, 90));
        bottomPanel.setName("bottomPanel"); // NOI18N
        bottomPanel.setPreferredSize(new java.awt.Dimension(118, 90));
        bottomPanel.setRequestFocusEnabled(false);
        bottomPanel.setLayout(new javax.swing.BoxLayout(bottomPanel, javax.swing.BoxLayout.Y_AXIS));

        loadDNAtrackPanel.setName("loadDNAtrackPanel"); // NOI18N
        loadDNAtrackPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        loadDNAtrackCheckbox.setSelected(true);
        loadDNAtrackCheckbox.setText(resourceMap.getString("loadDNAtrackCheckbox.text")); // NOI18N
        loadDNAtrackCheckbox.setName("loadDNAtrackCheckbox"); // NOI18N
        loadDNAtrackPanel.add(loadDNAtrackCheckbox);

        dnaTrackNameTextfield.setColumns(12);
        dnaTrackNameTextfield.setText(resourceMap.getString("dnaTrackNameTextfield.text")); // NOI18N
        dnaTrackNameTextfield.setName("dnaTrackNameTextfield"); // NOI18N
        loadDNAtrackPanel.add(dnaTrackNameTextfield);

        fromLabel.setText(resourceMap.getString("fromLabel.text")); // NOI18N
        fromLabel.setName("fromLabel"); // NOI18N
        loadDNAtrackPanel.add(fromLabel);

        dnaSequenceSourceCombobox.setName("dnaSequenceSourceCombobox"); // NOI18N
        loadDNAtrackPanel.add(dnaSequenceSourceCombobox);

        bottomPanel.add(loadDNAtrackPanel);

        additionalOptionsPanel.setName("additionalOptionsPanel"); // NOI18N
        additionalOptionsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));

        includeSequenceInProtocolCheckbox.setText(resourceMap.getString("includeSequenceInProtocolCheckbox.text")); // NOI18N
        includeSequenceInProtocolCheckbox.setName("includeSequenceInProtocolCheckbox"); // NOI18N
        additionalOptionsPanel.add(includeSequenceInProtocolCheckbox);

        skipUnrecognizedCheckbox.setText(resourceMap.getString("skipUnrecognizedCheckbox.text")); // NOI18N
        skipUnrecognizedCheckbox.setName("skipUnrecognizedCheckbox"); // NOI18N
        additionalOptionsPanel.add(skipUnrecognizedCheckbox);

        helpIconLabel.setText(resourceMap.getString("helpIconLabel.text")); // NOI18N
        helpIconLabel.setToolTipText(resourceMap.getString("helpIconLabel.toolTipText")); // NOI18N
        helpIconLabel.setName("helpIconLabel"); // NOI18N
        helpIconLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showExample(evt);
            }
        });
        additionalOptionsPanel.add(helpIconLabel);

        bottomPanel.add(additionalOptionsPanel);

        jPanel2.setMinimumSize(new java.awt.Dimension(0, 38));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setOpaque(false);
        jPanel2.setPreferredSize(new java.awt.Dimension(400, 30));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 8));

        progressbar.setName("progressbar"); // NOI18N
        jPanel1.add(progressbar);

        errorMessageLabel.setFont(resourceMap.getFont("errorMessageLabel.font")); // NOI18N
        errorMessageLabel.setForeground(resourceMap.getColor("errorMessageLabel.foreground")); // NOI18N
        errorMessageLabel.setText(resourceMap.getString("errorMessageLabel.text")); // NOI18N
        errorMessageLabel.setMaximumSize(new java.awt.Dimension(600, 16));
        errorMessageLabel.setMinimumSize(new java.awt.Dimension(100, 16));
        errorMessageLabel.setName("errorMessageLabel"); // NOI18N
        jPanel1.add(errorMessageLabel);

        jPanel2.add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMaximumSize(new java.awt.Dimension(75, 27));
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.setPreferredSize(new java.awt.Dimension(75, 27));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onOKClicked(evt);
            }
        });
        jPanel5.add(okButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setPreferredSize(new java.awt.Dimension(75, 27));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCancelClicked(evt);
            }
        });
        jPanel5.add(cancelButton);

        jPanel2.add(jPanel5, java.awt.BorderLayout.EAST);

        bottomPanel.add(jPanel2);

        getContentPane().add(bottomPanel, java.awt.BorderLayout.PAGE_END);

        topPanel.setMinimumSize(new java.awt.Dimension(100, 30));
        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(100, 8));

        javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 732, Short.MAX_VALUE)
        );
        topPanelLayout.setVerticalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 8, Short.MAX_VALUE)
        );

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        tabPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 5));
        tabPanel.setMinimumSize(new java.awt.Dimension(700, 400));
        tabPanel.setName("tabPanel"); // NOI18N
        tabPanel.setPreferredSize(new java.awt.Dimension(700, 400));

        GeneIDsTab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        GeneIDsTab.setToolTipText(resourceMap.getString("GeneIDsTab.toolTipText")); // NOI18N
        GeneIDsTab.setName("GeneIDsTab"); // NOI18N
        GeneIDsTab.setLayout(new java.awt.BorderLayout());

        geneIDsTopPanel.setName("geneIDsTopPanel"); // NOI18N
        geneIDsTopPanel.setLayout(new java.awt.BorderLayout());

        topBoxPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 3, 0));
        topBoxPanel2.setName("topBoxPanel2"); // NOI18N
        topBoxPanel2.setPreferredSize(new java.awt.Dimension(790, 38));
        topBoxPanel2.setLayout(new java.awt.BorderLayout());

        importIDsPanel.setName("importIDsPanel"); // NOI18N
        importIDsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 14));

        enteridslabel.setText(resourceMap.getString("enteridslabel.text")); // NOI18N
        enteridslabel.setName("enteridslabel"); // NOI18N
        importIDsPanel.add(enteridslabel);

        topBoxPanel2.add(importIDsPanel, java.awt.BorderLayout.CENTER);

        helpPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 0, 0, 0));
        helpPanel.setName("helpPanel"); // NOI18N
        helpPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        browseButton.setText(resourceMap.getString("browseButton.text")); // NOI18N
        browseButton.setMaximumSize(new java.awt.Dimension(70, 27));
        browseButton.setMinimumSize(new java.awt.Dimension(70, 27));
        browseButton.setName("browseButton"); // NOI18N
        browseButton.setPreferredSize(new java.awt.Dimension(70, 27));
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onBrowseButtonPressed(evt);
            }
        });
        helpPanel.add(browseButton);

        helpIconButton.setIcon(resourceMap.getIcon("helpIconButton.icon")); // NOI18N
        helpIconButton.setText(resourceMap.getString("helpIconButton.text")); // NOI18N
        helpIconButton.setToolTipText(resourceMap.getString("helpIconButton.toolTipText")); // NOI18N
        helpIconButton.setMaximumSize(new java.awt.Dimension(98, 27));
        helpIconButton.setMinimumSize(new java.awt.Dimension(98, 27));
        helpIconButton.setName("helpIconButton"); // NOI18N
        helpIconButton.setPreferredSize(new java.awt.Dimension(98, 27));
        helpIconButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                showExample(evt);
            }
        });
        helpPanel.add(helpIconButton);

        topBoxPanel2.add(helpPanel, java.awt.BorderLayout.EAST);

        geneIDsTopPanel.add(topBoxPanel2, java.awt.BorderLayout.NORTH);

        topBoxPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 2, 2));
        topBoxPanel1.setName("topBoxPanel1"); // NOI18N
        topBoxPanel1.setLayout(new java.awt.BorderLayout());

        geneIDtypePanel.setName("geneIDtypePanel"); // NOI18N
        geneIDtypePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        geneIDtypeLabel.setText(resourceMap.getString("geneIDtypeLabel.text")); // NOI18N
        geneIDtypeLabel.setName("geneIDtypeLabel"); // NOI18N
        geneIDtypePanel.add(geneIDtypeLabel);

        identifierTypeCombobox.setModel(getIDtypeModel());
        identifierTypeCombobox.setName("identifierTypeCombobox"); // NOI18N
        geneIDtypePanel.add(identifierTypeCombobox);

        topBoxPanel1.add(geneIDtypePanel, java.awt.BorderLayout.WEST);

        genomeBuildPanel.setName("genomeBuildPanel"); // NOI18N
        genomeBuildPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));

        organismLabel.setText(resourceMap.getString("organismLabel.text")); // NOI18N
        organismLabel.setName("organismLabel"); // NOI18N
        genomeBuildPanel.add(organismLabel);

        organismCombobox.setName("organismCombobox"); // NOI18N
        genomeBuildPanel.add(organismCombobox);

        genomeBuildLabel.setText(resourceMap.getString("genomeBuildLabel.text")); // NOI18N
        genomeBuildLabel.setName("genomeBuildLabel"); // NOI18N
        genomeBuildPanel.add(genomeBuildLabel);

        genomeBuildCombobox.setName("genomeBuildCombobox"); // NOI18N
        genomeBuildPanel.add(genomeBuildCombobox);

        topBoxPanel1.add(genomeBuildPanel, java.awt.BorderLayout.CENTER);

        geneIDsTopPanel.add(topBoxPanel1, java.awt.BorderLayout.SOUTH);

        GeneIDsTab.add(geneIDsTopPanel, java.awt.BorderLayout.NORTH);

        textAreaPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 6));
        textAreaPanel.setName("textAreaPanel"); // NOI18N
        textAreaPanel.setLayout(new java.awt.BorderLayout());

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        geneIdentifiersTextarea.setColumns(20);
        geneIdentifiersTextarea.setRows(5);
        geneIdentifiersTextarea.setName("geneIdentifiersTextarea"); // NOI18N
        geneIdentifiersTextarea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                idTextAreaFocusGained(evt);
            }
        });
        jScrollPane2.setViewportView(geneIdentifiersTextarea);

        textAreaPanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        GeneIDsTab.add(textAreaPanel, java.awt.BorderLayout.CENTER);

        sequenceLocationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        sequenceLocationPanel.setName("sequenceLocationPanel"); // NOI18N
        sequenceLocationPanel.setLayout(new java.awt.BorderLayout());

        jPanel13.setName("jPanel13"); // NOI18N
        jPanel13.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        includeGOcheckbox.setText(resourceMap.getString("includeGOcheckbox.text")); // NOI18N
        includeGOcheckbox.setToolTipText(resourceMap.getString("includeGOcheckbox.toolTipText")); // NOI18N
        includeGOcheckbox.setName("includeGOcheckbox"); // NOI18N
        jPanel13.add(includeGOcheckbox);

        sequenceLocationPanel.add(jPanel13, java.awt.BorderLayout.WEST);

        jPanel14.setName("jPanel14"); // NOI18N
        jPanel14.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        anchorLabel.setText(resourceMap.getString("anchorLabel.text")); // NOI18N
        anchorLabel.setName("anchorLabel"); // NOI18N
        jPanel14.add(anchorLabel);

        anchorPointComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Transcription Start Site", "Transcription End Site", "Full Gene" }));
        anchorPointComboBox.setLightWeightPopupEnabled(false);
        anchorPointComboBox.setMinimumSize(new java.awt.Dimension(180, 22));
        anchorPointComboBox.setName("anchorCombobox"); // NOI18N
        jPanel14.add(anchorPointComboBox);

        upstreamLabel.setText(resourceMap.getString("upstreamLabel.text")); // NOI18N
        upstreamLabel.setName("upstreamLabel"); // NOI18N
        jPanel14.add(upstreamLabel);

        upstreamSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(-2000), null, null, Integer.valueOf(1)));
        upstreamSpinner.setMaximumSize(new java.awt.Dimension(98, 27));
        upstreamSpinner.setMinimumSize(new java.awt.Dimension(98, 27));
        upstreamSpinner.setName("upstreamSpinner"); // NOI18N
        upstreamSpinner.setPreferredSize(new java.awt.Dimension(98, 27));
        jPanel14.add(upstreamSpinner);

        downstreamLabel.setText(resourceMap.getString("downstreamLabel.text")); // NOI18N
        downstreamLabel.setName("downstreamLabel"); // NOI18N
        jPanel14.add(downstreamLabel);

        downstreamSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(200), null, null, Integer.valueOf(1)));
        downstreamSpinner.setMaximumSize(new java.awt.Dimension(98, 27));
        downstreamSpinner.setMinimumSize(new java.awt.Dimension(98, 27));
        downstreamSpinner.setName("downstreamSpinner"); // NOI18N
        downstreamSpinner.setOpaque(false);
        downstreamSpinner.setPreferredSize(new java.awt.Dimension(98, 27));
        jPanel14.add(downstreamSpinner);

        sequenceLocationPanel.add(jPanel14, java.awt.BorderLayout.EAST);

        GeneIDsTab.add(sequenceLocationPanel, java.awt.BorderLayout.SOUTH);

        tabPanel.addTab(resourceMap.getString("GeneIDsTab.TabConstraints.tabTitle"), GeneIDsTab); // NOI18N

        CoordinatesTab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        CoordinatesTab.setName("CoordinatesTab"); // NOI18N
        CoordinatesTab.setLayout(new java.awt.BorderLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 6, 6));
        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.BorderLayout());

        tableScrollPane.setName("tableScrollPane"); // NOI18N
        jPanel4.add(tableScrollPane, java.awt.BorderLayout.CENTER);

        CoordinatesTab.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel17.setName("jPanel17"); // NOI18N
        jPanel17.setLayout(new java.awt.BorderLayout());

        jPanel3.setMinimumSize(new java.awt.Dimension(100, 38));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(10, 38));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel11.setMinimumSize(new java.awt.Dimension(488, 38));
        jPanel11.setName("jPanel11"); // NOI18N
        jPanel11.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 15));

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setMinimumSize(new java.awt.Dimension(500, 14));
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setPreferredSize(new java.awt.Dimension(520, 14));
        jPanel11.add(jLabel2);

        jPanel3.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel10.setMinimumSize(new java.awt.Dimension(190, 38));
        jPanel10.setName("jPanel10"); // NOI18N
        jPanel10.setPreferredSize(new java.awt.Dimension(190, 0));
        jPanel10.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 8));

        importButton.setText(resourceMap.getString("importButton.text")); // NOI18N
        importButton.setMaximumSize(new java.awt.Dimension(70, 27));
        importButton.setMinimumSize(new java.awt.Dimension(70, 27));
        importButton.setName("importButton"); // NOI18N
        importButton.setPreferredSize(new java.awt.Dimension(70, 27));
        jPanel10.add(importButton);

        advancedExampleButton.setIcon(resourceMap.getIcon("advancedExampleButton.icon")); // NOI18N
        advancedExampleButton.setText(resourceMap.getString("advancedExampleButton.text")); // NOI18N
        advancedExampleButton.setMaximumSize(new java.awt.Dimension(98, 27));
        advancedExampleButton.setMinimumSize(new java.awt.Dimension(98, 27));
        advancedExampleButton.setName("advancedExampleButton"); // NOI18N
        advancedExampleButton.setPreferredSize(new java.awt.Dimension(98, 27));
        advancedExampleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAdvancedExample(evt);
            }
        });
        jPanel10.add(advancedExampleButton);

        jPanel3.add(jPanel10, java.awt.BorderLayout.EAST);

        jPanel17.add(jPanel3, java.awt.BorderLayout.PAGE_START);

        jPanel18.setMinimumSize(new java.awt.Dimension(16, 38));
        jPanel18.setName("jPanel18"); // NOI18N
        jPanel18.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 5, 3));

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N
        jPanel18.add(jLabel7);

        jPanel17.add(jPanel18, java.awt.BorderLayout.SOUTH);

        CoordinatesTab.add(jPanel17, java.awt.BorderLayout.NORTH);

        tabPanel.addTab(resourceMap.getString("CoordinatesTab.TabConstraints.tabTitle"), CoordinatesTab); // NOI18N

        BEDtab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        BEDtab.setName("BEDtab"); // NOI18N
        BEDtab.setLayout(new java.awt.BorderLayout());

        BEDtabControlsPanel.setName("BEDtabControlsPanel"); // NOI18N
        BEDtabControlsPanel.setLayout(new java.awt.BorderLayout());

        BEDtop1.setName("BEDtop1"); // NOI18N
        BEDtop1.setLayout(new java.awt.BorderLayout());

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 2, 2));
        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setPreferredSize(new java.awt.Dimension(10, 38));
        jPanel6.setLayout(new java.awt.BorderLayout());

        jPanel12.setName("jPanel12"); // NOI18N
        jPanel12.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel12.add(jLabel1);

        organismComboboxBED.setName("organismComboboxBED"); // NOI18N
        jPanel12.add(organismComboboxBED);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jPanel12.add(jLabel3);

        genomeBuildComboboxBED.setName("genomeBuildComboboxBED"); // NOI18N
        jPanel12.add(genomeBuildComboboxBED);

        jPanel6.add(jPanel12, java.awt.BorderLayout.CENTER);

        jPanel15.setName("jPanel15"); // NOI18N
        jPanel15.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setIcon(resourceMap.getIcon("jLabel4.icon"));   
        jLabel4.setHorizontalTextPosition(SwingConstants.LEADING);
        jLabel4.setToolTipText(resourceMap.getString("jLabel4.toolTipText")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        jPanel15.add(jLabel4);

        BEDcoordinateSystemCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "BED", "GFF" }));
        BEDcoordinateSystemCombobox.setToolTipText(resourceMap.getString("BEDcoordinateSystemCombobox.toolTipText")); // NOI18N
        BEDcoordinateSystemCombobox.setName("BEDcoordinateSystemCombobox"); // NOI18N
        jPanel15.add(BEDcoordinateSystemCombobox);

        assignNewNamesToBEDregionsCheckbox.setSelected(true);
        assignNewNamesToBEDregionsCheckbox.setText(resourceMap.getString("assignNewNamesToBEDregionsCheckbox.text")); // NOI18N
        assignNewNamesToBEDregionsCheckbox.setToolTipText(resourceMap.getString("assignNewNamesToBEDregionsCheckbox.toolTipText")); // NOI18N
        assignNewNamesToBEDregionsCheckbox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        assignNewNamesToBEDregionsCheckbox.setName("assignNewNamesToBEDregionsCheckbox"); // NOI18N
        jPanel15.add(assignNewNamesToBEDregionsCheckbox);

        jPanel6.add(jPanel15, java.awt.BorderLayout.WEST);

        BEDtop1.add(jPanel6, java.awt.BorderLayout.CENTER);

        BEDtabControlsPanel.add(BEDtop1, java.awt.BorderLayout.SOUTH);

        BEDtop2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        BEDtop2.setMinimumSize(new java.awt.Dimension(506, 38));
        BEDtop2.setName("BEDtop2"); // NOI18N
        BEDtop2.setLayout(new java.awt.BorderLayout());

        jPanel7.setMinimumSize(new java.awt.Dimension(178, 38));
        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setPreferredSize(new java.awt.Dimension(178, 38));
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 8));

        importBEDbutton.setText(resourceMap.getString("importBEDbutton.text")); // NOI18N
        importBEDbutton.setMaximumSize(new java.awt.Dimension(70, 27));
        importBEDbutton.setMinimumSize(new java.awt.Dimension(70, 27));
        importBEDbutton.setName("importBEDbutton"); // NOI18N
        importBEDbutton.setPreferredSize(new java.awt.Dimension(70, 27));
        jPanel7.add(importBEDbutton);

        exampleBEDbutton.setIcon(resourceMap.getIcon("exampleBEDbutton.icon")); // NOI18N
        exampleBEDbutton.setText(resourceMap.getString("exampleBEDbutton.text")); // NOI18N
        exampleBEDbutton.setMaximumSize(new java.awt.Dimension(98, 27));
        exampleBEDbutton.setMinimumSize(new java.awt.Dimension(98, 27));
        exampleBEDbutton.setName("exampleBEDbutton"); // NOI18N
        exampleBEDbutton.setPreferredSize(new java.awt.Dimension(98, 27));
        jPanel7.add(exampleBEDbutton);

        BEDtop2.add(jPanel7, java.awt.BorderLayout.EAST);

        jPanel16.setName("jPanel16"); // NOI18N
        jPanel16.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 14));

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N
        jPanel16.add(jLabel6);

        BEDtop2.add(jPanel16, java.awt.BorderLayout.WEST);

        BEDtabControlsPanel.add(BEDtop2, java.awt.BorderLayout.NORTH);

        BEDtab.add(BEDtabControlsPanel, java.awt.BorderLayout.NORTH);

        BEDtabMainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 6, 6));
        BEDtabMainPanel.setName("BEDtabMainPanel"); // NOI18N
        BEDtabMainPanel.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        BEDTextArea.setColumns(20);
        BEDTextArea.setRows(5);
        BEDTextArea.setName("BEDTextArea"); // NOI18N
        jScrollPane3.setViewportView(BEDTextArea);

        BEDtabMainPanel.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        BEDtab.add(BEDtabMainPanel, java.awt.BorderLayout.CENTER);

        tabPanel.addTab(resourceMap.getString("BEDtab.TabConstraints.tabTitle"), BEDtab); // NOI18N
        tabPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Component selectedTab=tabPanel.getSelectedComponent();
                if (selectedTab==BEDtab) {
                    if (lastSelectedOrganism!=null) organismComboboxBED.setSelectedItem(lastSelectedOrganism);
                    if (lastSelectedGenomeBuild!=null) genomeBuildComboboxBED.setSelectedItem(lastSelectedGenomeBuild);
                } else if (selectedTab==GeneIDsTab) {
                    if (lastSelectedOrganism!=null) organismCombobox.setSelectedItem(lastSelectedOrganism);
                    if (lastSelectedGenomeBuild!=null) genomeBuildCombobox.setSelectedItem(lastSelectedGenomeBuild);                   
                }
            }
        });

        getContentPane().add(tabPanel, java.awt.BorderLayout.CENTER);

        pack();
    }

private void storeDialogSettings() {
    // save current settings to initialize dialog with the same selections next time
    String[] settings=new String[]{
       (String)identifierTypeCombobox.getSelectedItem(),
       (String)organismCombobox.getSelectedItem(),
       (String)genomeBuildCombobox.getSelectedItem(),
       (String)organismComboboxBED.getSelectedItem(),
       (String)genomeBuildComboboxBED.getSelectedItem(),
       (String)tabPanel.getSelectedComponent().getName()
    };
    if ( (settings[1]!=null && !settings[1].equals(settings[3])) || (settings[2]!=null && !settings[2].equals(settings[4])) ) {
        // make sure settings in the GeneIDs panel match those in the BED panel
        if (tabPanel.getSelectedComponent()==BEDtab) {
            settings[1]=settings[3];
            settings[2]=settings[4];
        } else {
            settings[3]=settings[1];
            settings[4]=settings[2];           
        }
    }
    try {
        engine.storeSystemObject(settings, "SequenceDialogSettings.ser");
    } catch (Exception e) {
       // not really important
    }    
}    
    
private void onOKClicked(java.awt.event.ActionEvent evt) {
    okButton.setEnabled(false);
    errorMessageLabel.setText("");
    storeDialogSettings();
    
    if (loadDNAtrackCheckbox.isSelected()) {
        String targetName=dnaTrackNameTextfield.getText().trim();
        String nameError=engine.checkNameValidity(targetName,false);
        if (nameError!=null) {
           JOptionPane.showMessageDialog(this, nameError, "Illegal DNA datatrack name: "+targetName, JOptionPane.ERROR_MESSAGE);
           okButton.setEnabled(true);
           return;
        }
    }

    // now create the sequence objects
    AddSequencesTask task=null;
    if (tabPanel.getSelectedComponent()==GeneIDsTab) {
        String problem=checkGeneIDsTabEntry();
        if (problem!=null) {
            errorMessageLabel.setText(problem);
            okButton.setEnabled(true);
            return;
        }
        parseGeneIDEntries(); //
    } else if (tabPanel.getSelectedComponent()==BEDtab) {
        String problem=checkBEDTabEntry();
        if (problem!=null) {
            errorMessageLabel.setText(problem);
            okButton.setEnabled(true);
            return;
        }
        Sequence[] sequences=parseBEDEntries(); //
        task=new AddSequencesTask(engine,sequences);
        exitAndLoad(task);
    } else { // Advanced Dialog -> Manual entry of all information
        removeBlankRows();
        String problem=checkAdvancedTabEntry();
        if (problem!=null) {
            errorMessageLabel.setText(problem);
            okButton.setEnabled(true);
            return;
        }
        Object[][] data=parseManualEntryTable(); // add data directly
        Sequence[] sequences=new Sequence[data.length];
        for (int i=0;i<sequences.length;i++) {
            sequences[i]=new Sequence((data[i][0]!=null)?data[i][0].toString():null, ((Integer)data[i][1]).intValue(), (data[i][2]!=null)?data[i][2].toString():null, (data[i][3]!=null)?data[i][3].toString():null, ((Integer)data[i][4]).intValue(), ((Integer)data[i][5]).intValue(), (data[i][6]!=null)?data[i][6].toString():null, ((Integer)data[i][7]), ((Integer)data[i][8]), ((Integer)data[i][9]).intValue());
        }
        task=new AddSequencesTask(engine,sequences);
        exitAndLoad(task);
    }
}

/** This method can be used by the MinimalGUI client to retrieve the AddSequencesTask prepared by this dialog */
public AddSequencesTask getAddSequencesTask() {
    return addSequencesTask;
}

private void exitAndLoad(AddSequencesTask task) {
    this.setVisible(false);
    MotifLabGUI gui=null;
    if (engine.getClient() instanceof MotifLabGUI) gui=(MotifLabGUI)engine.getClient();
    if (gui!=null) {
        addSequencesTask=null; // this is not needed
        gui.launchAddSequencesTask(task, (includeSequenceInProtocolCheckbox.isSelected() && gui.isRecording()));
        if (loadDNAtrackCheckbox.isSelected()) launchLoadDNATrack(gui);
    } else {
        addSequencesTask=task; // allows the task to be retrieved later by the client
    }
}

private void launchLoadDNATrack(MotifLabGUI gui) {
    Object s=dnaSequenceSourceCombobox.getSelectedItem();
    String dnaTrackSource=null;
    if (s!=null) dnaTrackSource=s.toString();
    if (dnaTrackSource==null || dnaTrackSource.isEmpty()) return;
    Operation operation=engine.getOperation("new");
    OperationTask newDNATrackTask=new OperationTask(operation.getName());
    String targetName=dnaTrackNameTextfield.getText().trim();
    newDNATrackTask.setParameter(OperationTask.OPERATION, operation);
    newDNATrackTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
    newDNATrackTask.setParameter(OperationTask.TARGET_NAME, targetName);
    newDNATrackTask.setParameter(OperationTask.SOURCE_NAME, targetName);
    newDNATrackTask.setParameter(Operation_new.DATA_TYPE, DNASequenceDataset.getType());
    newDNATrackTask.setParameter(Operation_new.PARAMETERS,Operation_new.DATA_TRACK_PREFIX+dnaTrackSource);
    gui.launchOperationTask(newDNATrackTask, gui.isRecording());
}


private void importManualEntriesFromFile() {
    JFileChooser filechooser = (engine.getClient() instanceof MinimalGUI)?((MinimalGUI)engine.getClient()).getFileChooser(null):((MotifLabGUI)engine.getClient()).getFileChooser(null);//  JFileChooser(gui.getLastUsedDirectory());
    filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int status=filechooser.showOpenDialog(null);
    if (status==JFileChooser.APPROVE_OPTION) {
        File selected=filechooser.getSelectedFile();
        if (engine.getClient() instanceof MinimalGUI) ((MinimalGUI)engine.getClient()).setLastUsedDirectory(selected); else ((MotifLabGUI)engine.getClient()).setLastUsedDirectory(selected);
        try {
           loadManualEntryFileInBackground(selected);
        } catch (Exception e) {}
    }
}



private void onCancelClicked(java.awt.event.ActionEvent evt) {
    storeDialogSettings();
    this.setVisible(false);
}

private void onBrowseButtonPressed(java.awt.event.ActionEvent evt) {

    JFileChooser filechooser = (engine.getClient() instanceof MinimalGUI)?((MinimalGUI)engine.getClient()).getFileChooser(null):((MotifLabGUI)engine.getClient()).getFileChooser(null);//  JFileChooser(gui.getLastUsedDirectory());
    filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int status=filechooser.showOpenDialog(null);
    if (status==JFileChooser.APPROVE_OPTION) {
        File selected=filechooser.getSelectedFile();
        if (engine.getClient() instanceof MinimalGUI) ((MinimalGUI)engine.getClient()).setLastUsedDirectory(selected); else ((MotifLabGUI)engine.getClient()).setLastUsedDirectory(selected);
        try {
           loadFileInBackground(selected);
        } catch (Exception e) {}
    }
}

private void importBEDbuttonPressed() {
    JFileChooser filechooser = (engine.getClient() instanceof MinimalGUI)?((MinimalGUI)engine.getClient()).getFileChooser(null):((MotifLabGUI)engine.getClient()).getFileChooser(null);//  JFileChooser(gui.getLastUsedDirectory());
    filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int status=filechooser.showOpenDialog(null);
    if (status==JFileChooser.APPROVE_OPTION) {
        File selected=filechooser.getSelectedFile();
        if (engine.getClient() instanceof MinimalGUI) ((MinimalGUI)engine.getClient()).setLastUsedDirectory(selected); else ((MotifLabGUI)engine.getClient()).setLastUsedDirectory(selected);
        try {
           loadBEDFileInBackground(selected);
        } catch (Exception e) {} // the error is reported elsewhere
    }
}

private void idTextAreaFocusGained(java.awt.event.FocusEvent evt) {
    // fromListRadioButton.setSelected(true);
}

private void showExample(java.awt.event.MouseEvent evt) {
    exampleShown++;
    String example=exampleSequences[exampleShown%exampleSequences.length];
    geneIdentifiersTextarea.setText(example);
}

private void smdOKclicked(java.awt.event.MouseEvent evt) {
      selectMappingDialog.setVisible(false);
}

private void showAdvancedExample(java.awt.event.ActionEvent evt) {
// TODO add your handling code here:
         Object[][] list=new Object[][]{
                    {"NTNG1",Organism.HUMAN,"hg38","1",new Integer(107482152),new Integer(107484352),"NTNG1",new Integer(107484152),new Integer(107827603),new Integer(1)},
                    {"RPRM",Organism.HUMAN,"hg38","2",new Integer(154043368),new Integer(154045568),"RPRM",new Integer(154043568),new Integer(154042098),new Integer(-1)},
                    {"CUX2",Organism.HUMAN,"hg38","12",new Integer(109954212),new Integer(109956412),"CUX2",new Integer(109956212),new Integer(110272739),new Integer(1)},
                    {"HAPLN4",Organism.HUMAN,"hg38","19",new Integer(19244978),new Integer(19247178),"HAPLN4",new Integer(19245178),new Integer(19226557),new Integer(-1)},
                    {"NTNG2",Organism.HUMAN,"hg38","9",new Integer(134025155),new Integer(134027355),"NTNG2",new Integer(134027155),new Integer(134109742),new Integer(1)},
                    };
         advancedInputTable.setModel(advancedTableModel);
         while (advancedTableModel.getRowCount()>0) {
             advancedTableModel.removeRow(0);
         }
         for (int i=0;i<list.length;i++) {
             advancedTableModel.addRow(list[i]);
         }
}

private void showBEDExample() {
    final String example=
              "# Enter sequence coordinates in three columns separated by TABs or commas.\n"
            + "# If TABs are used for column separation, all commas will be removed (they can optionally be used for digit grouping in large numbers).\n"
            + "# The first column is the chromosome (optionally prefixed with \"chr\").\n"
            + "# The second and third columns are genomic start and end coordinates within the chromosome.\n"
            + "# Note that in the BED-format, the first coordinate in the genome starts at 0 and the end-coordinate is excluded,\n"
            + "# but in GFF-format the coordinates begin at 1 and both the start and end-coordinates are included.\n"
            + "# If TABs are used (or only one column is entered), the location can alternatively be provided on the format 'chr:start-end'.\n"
            + "# If the coordinate in the second column is larger than the third column, the sequence is assumed to be on the reverse strand.\n"
            + "# Three more columns can optionally be added specifying a name for the sequence, a score (not used) and the orientation to use.\n"
            +  "\n"
            +  "1\t107482152\t107484352\n"
            +  "2\t154,043,368\t154,045,568\n"
            +  "12\t109954212\t109956412\n"
            +  "chr19\t   19244978\t  19247178\n"
            +  "chr9\t134025155\t134027355\n"
            +  "chr2:154,143,368-154,145,568\n"
            ;
    BEDTextArea.setText(example);
}

private void removeBlankRows() {
    int rows=advancedTableModel.getRowCount();
    int columns=advancedTableModel.getColumnCount();
    for (int i=0;i<rows;i++) {
        boolean empty=true;
        for (int j=0;j<columns;j++) {
            Object val=advancedTableModel.getValueAt(i, j);
            if (val!=null && !val.toString().isEmpty()) {empty=false;break;}
        }
        if (empty) {advancedTableModel.removeRow(i);i--;rows--;}
    }
}

/**
 * Checks if the input in the Advanced tab table is correctly formatted
 * If errors are encountered a message string describing the problem is returned.
 * If everything is OK a null value is returned
 * Note that some "errors" might be fixed, for instance of the sequence has
 * no associated TSS or TES, these will be set to the start and end positions respectively
 * If the sequence has no name but has a gene, the name of the gene will be used as the name
 * of the sequence, etc.
 */
private String checkAdvancedTabEntry() {
    int rows=advancedTableModel.getRowCount();
    HashSet<String> usedNames=new HashSet<String>();
    int nameColumn=0;
    int organismColumn=1;
    int buildColumn=2;
    int chromosomeColumn=3;
    int startColumn=4;
    int endColumn=5;
    int geneColumn=6;
    int TSSColumn=7;
    int TESColumn=8;
    int orientationColumn=9;

    for (int i=0;i<rows;i++) {
        Object nameValue=advancedTableModel.getValueAt(i, nameColumn);
        Object organismValue=advancedTableModel.getValueAt(i, organismColumn);
        Object buildValue=advancedTableModel.getValueAt(i, buildColumn);
        Object chrValue=advancedTableModel.getValueAt(i, chromosomeColumn);
        Object startValue=advancedTableModel.getValueAt(i, startColumn);
        Object endValue=advancedTableModel.getValueAt(i, endColumn);
        Object geneValue=advancedTableModel.getValueAt(i, geneColumn);
        Object TSSValue=advancedTableModel.getValueAt(i, TSSColumn);
        Object TESValue=advancedTableModel.getValueAt(i, TESColumn);
        Object orientationValue=advancedTableModel.getValueAt(i, orientationColumn);
        if ((nameValue==null || nameValue.toString().isEmpty()) && (geneValue==null || geneValue.toString().isEmpty())) return "Row "+(i+1)+": Missing both sequence and gene name";
        else if (usedNames.contains(nameValue.toString())) return "Row "+(i+1)+": Sequence name '"+nameValue.toString()+"' has been used before";
        else if (geneValue==null || geneValue.toString().isEmpty()) {} // {advancedTableModel.setValueAt(nameValue,i, geneColumn);geneValue=nameValue;}
        else if (nameValue==null || nameValue.toString().isEmpty()) {advancedTableModel.setValueAt(geneValue.toString(),i, nameColumn);nameValue=geneValue.toString();}
        String namecheck=engine.checkSequenceNameValidity(nameValue.toString(), false);
        if (namecheck!=null) return ("Row "+(i+1)+": Invalid sequence name '"+nameValue.toString()+"': "+namecheck);
        usedNames.add(nameValue.toString());
        if (orientationValue==null || !(orientationValue instanceof Integer)) {orientationValue=new Integer(1);advancedTableModel.setValueAt(orientationValue,i, orientationColumn);}
        if (startValue==null || !(startValue instanceof Integer)) return "Row "+(i+1)+": Start is not an integer value";
        if (endValue==null || !(endValue instanceof Integer)) return "Row "+(i+1)+": End is not an integer value";
        if (TSSValue==null || TSSValue.toString().trim().isEmpty()) { // use start of sequence as TSS if not explicitly set=
//            Object newvalue=(((Integer)orientationValue).intValue()==1)?startValue:endValue;
//            advancedTableModel.setValueAt(newvalue,i, TSSColumn);
//            TSSValue=newvalue;
              TSSValue=null;
              advancedTableModel.setValueAt(TSSValue,i, TSSColumn);
        }
        if (TSSValue!=null && !(TSSValue instanceof Integer)) return "Row "+(i+1)+": TSS is not an integer value";
        if (TESValue==null || TESValue.toString().trim().isEmpty()) {
//            Object newvalue=(((Integer)orientationValue).intValue()==1)?endValue:startValue;
//            advancedTableModel.setValueAt(newvalue,i, TESColumn);
//            TESValue=newvalue;
              TESValue=null;
              advancedTableModel.setValueAt(TESValue,i, TESColumn);
        }
        if (TESValue!=null && !(TESValue instanceof Integer)) return "Row "+(i+1)+": TES is not an integer value";
        if (chrValue==null) return "Row "+(i+1)+": Missing chromosome specification";
        if (organismValue==null || !(organismValue instanceof Integer)) return "Row "+(i+1)+": Organism is not a valid Taxonomy ID value";
        if (buildValue==null  || buildValue.toString().isEmpty()) return "Row "+(i+1)+": Missing genome build specification";
        if ( ((Integer)startValue).intValue()>((Integer)endValue).intValue())  return "Row "+(i+1)+": Start coordinate should be smaller than End coordinate";
        if ( ((Integer)endValue).intValue()-((Integer)startValue).intValue()+1>MAX_SEQUENCE_LENGTH)  return "Row "+(i+1)+": Sequence exceeds (selfimposed) maximum length of "+MAX_SEQUENCE_LENGTH+" bp";
    }
    return null;
}

/**
 * Checks if the input in the GeneIDs Tab dialog is correctly formatted
 * If errors are encountered a message string describing the problem is returned.
 * If everything is OK a null value is returned
 */
private String checkGeneIDsTabEntry() {
    String text=geneIdentifiersTextarea.getText().trim();
    text=text.replaceAll("\\s*,\\s*", "\n");
    geneIdentifiersTextarea.setText(text);
    String[] lines=text.split("\n");
    for (String line:lines) {
        if (line.startsWith("#")) continue; // regard as comment
        String[] elements=line.split("\t");
        if (elements.length>6) return "The number of columns in a line must be between 1 and 6";
        if (elements.length>=2) {
            String formatString=elements[1];
            if (!idResolver.isIDFormatSupported(formatString)) return "Unknown ID format: "+formatString;
        }
        if (elements.length>=3) {
            String buildString=elements[2];
            if (!idResolver.isGenomeBuildSupported(buildString)) return "Unsupported genome build: "+buildString;
        }
        int startpos=0;
        if (elements.length>=4) {
            String positionString=elements[3];
            try {
                startpos=Integer.parseInt(positionString);
            } catch (NumberFormatException e) {
                return "Not a valid start position: "+positionString;
            }
        }
        int endpos=0;
        if (elements.length>=5) {
            String positionString=elements[4];
            try {
                endpos=Integer.parseInt(positionString);
            } catch (NumberFormatException e) {
                return "Not a valid end position: "+positionString;
            }
        }
        if (endpos<startpos) return "Start position ("+startpos+") must be located before End position ("+endpos+")";
        if (elements.length>=6) {
            String anchorString=elements[5];
            if (! (    anchorString.equalsIgnoreCase("TSS")
                    || anchorString.equalsIgnoreCase("Transcription Start Site")
                    || anchorString.equalsIgnoreCase("TES")
                    || anchorString.equalsIgnoreCase("Transcription End Site")
                    || anchorString.equalsIgnoreCase("gene")
                    || anchorString.equalsIgnoreCase("full gene")
             )) return "Unsupported anchor site: "+anchorString;

        }
    }
    // check default settings also
    int fromPos=((Integer)upstreamSpinner.getValue()).intValue();
    int toPos=((Integer)downstreamSpinner.getValue()).intValue();
    if (toPos<fromPos) return "Default 'from' position ("+fromPos+") must be located before 'to' position ("+toPos+")";
    return null;
}

/**
 * Checks if the input in the BED Tab dialog is correctly formatted
 * If errors are encountered a message string describing the problem is returned.
 * If everything is OK a null value is returned
 */
private String checkBEDTabEntry() {
    String text=BEDTextArea.getText().trim();
    String[] lines=text.split("\n");
    for (String line:lines) {
        line=line.trim();
        if (line.startsWith("#") || line.isEmpty()) continue; // regard as comment
        if (line.contains("\t") || line.matches("^(\\w+)\\s*:\\s*([0-9,]+)\\s*\\-\\s*([0-9,]+)$")) line=line.replaceAll(",",""); // if columns are TAB-separated or the entry only consists of "chr:start-end" allow commas to be used for grouping
        else line=line.replaceAll(",","\t"); //
        if (line.matches("^(\\w+)\\s*:\\s*(\\d+)\\s*\\-\\s*(\\d+).*")) { // the location is on the format chr:start-end. Replace the : and - with TABs to create three columns instead of one
            line=line.replaceFirst("\\s*:\\s*","\t");
            line=line.replaceFirst("\\s*\\-\\s*","\t");
        }
        String[] elements=line.split("\\s+");
        if (elements.length<3) return "There should be at least 3 columns on each line (found "+elements.length+")";
        int startpos=0;
        String positionString=elements[1];
        try {
            startpos=Integer.parseInt(positionString);
            if (startpos<0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return "Not a valid start position: "+positionString;
        }
        int endpos=0;
        positionString=elements[2];
        try {
            endpos=Integer.parseInt(positionString);
            if (endpos<0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return "Not a valid end position: "+positionString;
        }
        int length=(endpos>=startpos)?(endpos-startpos+1):(startpos-endpos+1);
        if (length>MAX_SEQUENCE_LENGTH)  return "\""+elements[0]+":"+elements[1]+"-"+elements[2]+"\": Sequence exceeds (selfimposed) maximum length of "+MAX_SEQUENCE_LENGTH+" bp";
    }
    return null;
}

/**
 * Parses the entries in the textarea under the "BED" tab,
 */
private Sequence[] parseBEDEntries() {
    String organismName=(String)organismComboboxBED.getSelectedItem();
    int defaultOrganism=Organism.getTaxonomyID(organismName);
    String defaultBuild=(String)genomeBuildComboboxBED.getSelectedItem();
    String coordinateSystem=(String)BEDcoordinateSystemCombobox.getSelectedItem();
    boolean zeroIndexed=coordinateSystem.equals("BED");
    boolean exclusiveEnd=coordinateSystem.equals("BED");
    String text=BEDTextArea.getText();
    String[] lines=text.split("\n");
    int digits=(""+lines.length).length(); // how many digits does it take to number all lines
    ArrayList<Sequence> sequences=new ArrayList<Sequence>();
    int count=0;
    for (String line:lines) {
        line=line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        if (line.contains("\t") || line.matches("^(\\w+)\\s*:\\s*([0-9,]+)\\s*\\-\\s*([0-9,]+)$")) line=line.replaceAll(",",""); // if columns are TAB-separated or the entry only consists of "chr:start-end" allow commas to be used for grouping
        else line=line.replaceAll(",","\t"); //

        if (line.matches("^(\\w+)\\s*:\\s*(\\d+)\\s*\\-\\s*(\\d+).*")) { // the location is on the format chr:start-end. Replace the : and - with TABs to create three columns instead of one
            line=line.replaceFirst("\\s*:\\s*","\t");
            line=line.replaceFirst("\\s*\\-\\s*","\t");
        }
        count++;
        String suffix=""+count;
        while (suffix.length()<digits) suffix="0"+suffix;
        String sequenceName="Sequence"+suffix;
        String[] elements=line.split("\\s+");
        int start=0,end=0;
        String chr=elements[0];
        if (chr.toLowerCase().startsWith("chr")) chr=elements[0].substring(3);
        try {
            start=Integer.parseInt(elements[1]);
        } catch(NumberFormatException e) {engine.logMessage("ERROR: Invalid 'Start' position value : "+elements[1]);}
        try {
            end=Integer.parseInt(elements[2]);
        } catch(NumberFormatException e) {engine.logMessage("ERROR: Invalid 'End' position value : "+elements[2]);}
        if (zeroIndexed) {
            start++;
            end++;
        }
        if (exclusiveEnd) {
            end--;
        }

        int orientation=Sequence.DIRECT;
        if (start>end) {
            int swap=start;
            start=end;
            end=swap;
            orientation=Sequence.REVERSE;
        } else if (elements.length>=6) {
            if (elements[5].startsWith("-")) orientation=Sequence.REVERSE;
        }

        String geneName=sequenceName;
        if (elements.length>=4) { // if a fourth column is present, use this for both sequence name and gene name
            geneName=elements[3];
            sequenceName=geneName;
        }
        if (!assignNewNamesToBEDregionsCheckbox.isSelected()) sequenceName="chr"+chr+"_"+elements[1]+"_"+elements[2];
        sequences.add(new Sequence(sequenceName, defaultOrganism, defaultBuild, chr, start, end, geneName, null, null, orientation));
    }
    Sequence[] seqarray=new Sequence[sequences.size()];
    seqarray=sequences.toArray(seqarray);
    return seqarray;
}

/**
 * Parses the gene ID entries in the textarea under the "GeneIDs" tab,
 * obtains information about TSS, TES etc. for each Gene ID.
 * The method returns "right away" but spawns an asynchroneous background
 * task to connect to a web service which can resolve the Gene IDs.
 */
private void parseGeneIDEntries() {
    String organismName=(String)organismCombobox.getSelectedItem();
    int defaultOrganism=Organism.getTaxonomyID(organismName);
    String defaultFormat=(String)identifierTypeCombobox.getSelectedItem();
    String defaultBuild=(String)genomeBuildCombobox.getSelectedItem();
    String text=geneIdentifiersTextarea.getText();
    text=text.replaceAll(",", "\n");
    String[] lines=text.split("\n");
    HashSet<String> usedNames=new HashSet<String>();
    ArrayList<GeneIdentifier> list=new ArrayList<GeneIdentifier>();
    HashMap<GeneIdentifier,Object[]> posInfoMap=new HashMap<GeneIdentifier,Object[]>();
    for (String line:lines) {
        line=line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        String[] elements=line.split("\t");
        if (usedNames.contains(elements[0])) {
            engine.logMessage("Skipping duplicate entry for: "+elements[0]);
            continue;
        } else usedNames.add(elements[0]);
        GeneIdentifier geneidentifier=new GeneIdentifier(elements[0], defaultFormat, defaultOrganism, defaultBuild);
        if (elements.length>=2) {
            geneidentifier.format=elements[1];
        }
        if (elements.length>=3) {
            String buildString=elements[2];
            int organism=Organism.getOrganismForGenomeBuild(buildString); // this should not fail (return 0) if checkGeneIDsTabEntry() returns OK
            geneidentifier.build=buildString;
            geneidentifier.organism=organism;
        }
        Object[] posInfo=(elements.length>=4)?new Object[]{null,null,null}:null;
        if (elements.length>=4) {
            try {
                int position=Integer.parseInt(elements[3]);
                posInfo[0]=new Integer(position);
            } catch(NumberFormatException e) {engine.logMessage("ERROR: Invalid integer number : "+elements[3]);}
        }
        if (elements.length>=5) {
            try {
                int position=Integer.parseInt(elements[4]);
                posInfo[1]=new Integer(position);
            } catch(NumberFormatException e) {engine.logMessage("ERROR: Invalid integer number : "+elements[4]);}
        }
        if (elements.length>=6) {
            posInfo[2]=elements[5]; // anchor
        }
        list.add(geneidentifier);
        if (posInfo!=null) posInfoMap.put(geneidentifier, posInfo);

    }
    resolveIDsInBackground(list, posInfoMap, includeGOcheckbox.isSelected());
}

/**
 * This method is called on the EDT after gene IDs have been successfully resolved and a list
 * of GeneIDMappings is returned. This list must be processed to ensure that all genes are
 * present and the user must decide how to handle possible duplicate matches.
 * Once these issues have been dealt with, an AddSequencesTask can be scheduled for execution
 * @param idlist The list of Gene identifier that the user has specified
 * @param resolvedlist Information about those specified gene identifiers, as obtained from e.g. BioMart
 * @param positionInfo Information about which sequence segment relative to the gene should be used (e.g. -1000 to +200 around TSS) 
 */
private void processResolvedGeneIDs(ArrayList<GeneIdentifier> idlist, ArrayList<GeneIDmapping> resolvedlist, HashMap<GeneIdentifier,Object[]> positionInfo, boolean includeGO) throws ParseError {
    // for (GeneIDmapping mapping:resolvedlist) {gui.debugMessage("Resolved:  "+mapping.geneID+" => "+mapping.geneName+"   "+mapping.chromosome+":"+mapping.TSS+"-"+mapping.TES+" ("+mapping.strand+")");}
    int missing=0;
    ArrayList<Object[]> result=new ArrayList<Object[]>(idlist.size());
    // check that all gene IDs have indeed been resolved
    for (GeneIdentifier geneid:idlist) {
       ArrayList<GeneIDmapping> list=getEntriesForID(resolvedlist,geneid.identifier);
       if (list.isEmpty()) {
           engine.logMessage("Unable to find information about "+Organism.getCommonName(geneid.organism).toLowerCase()+" "+geneid.format+" identifier: "+geneid.identifier);
           missing++;
       }
    }
    if (missing>0 && !skipUnrecognizedCheckbox.isSelected()) {
        if (missing==idlist.size()) throw new ParseError("Unable to resolve gene identifiers. Make sure the correct ID type is selected");
        else throw new ParseError("Missing information about "+missing+" gene identifier"+((missing==1)?"":"s")+" (see log)");
    }
    for (GeneIdentifier geneid:idlist) {
       ArrayList<GeneIDmapping> list=getEntriesForID(resolvedlist,geneid.identifier);
       if (list.size()>1) { // multiple candidate mappings for a single gene ID
           list=selectCorrectMappings(list,geneid.identifier);
           if (list==null && smdDialogCancelled) {smdDialogCancelled=false; throw new ParseError("Aborted!");} // abort resolve
       }
       //gui.debugMessage("Size of resolvedlist="+list.size());
       for (GeneIDmapping mapping:list) { // add all those mappings that are left for this ID
           //gui.debugMessage("AFTER:"+mapping.toString());
           Object[] sequenceInfo=new Object[12];
           Object[] posInfo=positionInfo.get(geneid);
           sequenceInfo[0]=(autoCorrectNames)?MotifLabEngine.convertToLegalSequenceName(mapping.geneID):mapping.geneID; // the gene ID will be used as sequence name
           sequenceInfo[1]=new Integer(geneid.organism);
           sequenceInfo[2]=geneid.build;
           sequenceInfo[3]=mapping.chromosome;
           // sequenceInfo[4]= sequence start // these will be filled in later based on the user's region selections
           // sequenceInfo[5]= sequence end   // these will be filled in later based on the user's region selections
           sequenceInfo[6]=mapping.geneName;
           sequenceInfo[7]=new Integer(mapping.TSS);
           sequenceInfo[8]=new Integer(mapping.TES);
           sequenceInfo[9]=new Integer(mapping.strand);
           sequenceInfo[10]=geneid.format;
           sequenceInfo[11]=(includeGO)?mapping.GOterms:null;
           fillInStartAndEndPositions(sequenceInfo,posInfo);
           result.add(sequenceInfo);
           //gui.debugMessage("SequenceInfo:   "+sequenceInfo[0].toString()+" "+sequenceInfo[1].toString()+" "+sequenceInfo[2].toString()+" "+sequenceInfo[3].toString()+" "+sequenceInfo[4].toString()+" "+sequenceInfo[5].toString()+" "+sequenceInfo[6].toString()+" "+sequenceInfo[7].toString()+" "+sequenceInfo[8].toString());
       }
    } // end for each geneID
    Object[][] data=new Object[result.size()][12];
    data=result.toArray(data);
    Sequence[] sequences=new Sequence[result.size()];
    for (int i=0;i<sequences.length;i++) {
        if (((Integer)data[i][4]).intValue()>((Integer)data[i][5]).intValue()) throw new ParseError("End position located prior to start position in sequence '"+(String)data[i][0]+"'");
        sequences[i]=new Sequence((String)data[i][0], ((Integer)data[i][1]).intValue(), (String)data[i][2], (String)data[i][3], ((Integer)data[i][4]).intValue(), ((Integer)data[i][5]).intValue(), (String)data[i][6], ((Integer)data[i][7]), ((Integer)data[i][8]), ((Integer)data[i][9]).intValue());
        sequences[i].setUserDefinedPropertyValue((String)data[i][10], (String)data[i][0]); // set "geneID type => sequence Name"
        if (data[i][11]!=null) {
           Collection<String> GOterms=(Collection<String>)data[i][11];
           if (!GOterms.isEmpty()) {
               try {sequences[i].setGOterms(GOterms);} catch (ParseError e) {} // The terms should have been checked many times already, so just ignore errors at this point
           }
        }
    }
    AddSequencesTask task=new AddSequencesTask(engine,sequences);
    exitAndLoad(task);
}



/** Goes through a list of GeneIDmapping and returns only those entries that correspond to the given gene id */
private ArrayList<GeneIDmapping> getEntriesForID(ArrayList<GeneIDmapping> list, String id) {
    ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>();
    for (GeneIDmapping entry:list) {
        if (entry.geneID.equalsIgnoreCase(id)) result.add(entry);
    }
    return result;
}

/** Fills in upstream and downstream coordinates based on user selections and gene orientation
 *  @param sequenceInfo
 *  @param position This can be either NULL or an array with 3 entries (some or all of which can be null)
 *  unless null, the first and second entries should be Integers and the third should be a String
 */
private void fillInStartAndEndPositions(Object[] sequenceInfo, Object[] position) throws ParseError {
    if (position==null) position=new Object[]{null,null,null};
    int upstream=(position[0] instanceof Integer)?(Integer)position[0]:((Integer)upstreamSpinner.getValue()).intValue();
    int downstream=(position[1] instanceof Integer)?(Integer)position[1]:((Integer)downstreamSpinner.getValue()).intValue();
    if (upstream>0) upstream--; // to account for direct transition from -1 to +1 at TSS
    if (downstream>0) downstream--; // to account for direct transition from -1 to +1 at TSS
    String anchor=(position[2] instanceof String)?(String)position[2]:(String)anchorPointComboBox.getSelectedItem();
    int tss=((Integer)sequenceInfo[7]).intValue();  // actual TSS relative to orientation
    int tes=((Integer)sequenceInfo[8]).intValue();  // actual TES
    if (anchor.equalsIgnoreCase("Transcription Start Site") || anchor.equalsIgnoreCase("TSS")) {
        if (((Integer)sequenceInfo[9]).intValue()==Sequence.DIRECT) {
           sequenceInfo[4]=tss+upstream;
           sequenceInfo[5]=tss+downstream;
        } else { // Reverse Strand
           sequenceInfo[4]=tss-downstream;
           sequenceInfo[5]=tss-upstream;
        }
    } else if (anchor.equalsIgnoreCase("Transcription End Site") || anchor.equalsIgnoreCase("TES")) {
        if (((Integer)sequenceInfo[9]).intValue()==Sequence.DIRECT) {
           sequenceInfo[4]=tes+upstream;
           sequenceInfo[5]=tes+downstream;
        } else { // Reverse Strand
           sequenceInfo[4]=tes-downstream;
           sequenceInfo[5]=tes-upstream;
        }
    } else if (anchor.equalsIgnoreCase("gene") || anchor.equalsIgnoreCase("full gene") || anchor.equalsIgnoreCase("transcript")) {
        if (((Integer)sequenceInfo[9]).intValue()==Sequence.DIRECT) {
           sequenceInfo[4]=tss+upstream;
           sequenceInfo[5]=tes+downstream;
        } else { // Reverse Strand
           sequenceInfo[4]=tes-downstream;
           sequenceInfo[5]=tss-upstream;
        }
    } else {
        throw new ParseError("Unsupported anchor site: "+anchor);
    }
    if ((Integer)sequenceInfo[4]<=0) sequenceInfo[4]=1; // do not let the sequence coordinates be negative
    if ((Integer)sequenceInfo[5]<=0) sequenceInfo[5]=1; // do not let the sequence coordinates be negative
    int seqlength=(Integer)sequenceInfo[5]-(Integer)sequenceInfo[4]+1;
    if (seqlength>MAX_SEQUENCE_LENGTH)  throw new ParseError("Sequence '"+sequenceInfo[0]+"' exceeds (selfimposed) maximum length of "+MAX_SEQUENCE_LENGTH+" bp");
}


/** This method is used whenever multiple mappings are returned for a single gene ID
 * (I am not sure if this is possible but I will account for it anyway). A dialog is
 * displayed so that the user can select the mapping wanted (or even several mappings)
 */
private ArrayList<GeneIDmapping> selectCorrectMappings(ArrayList<GeneIDmapping> list, String id) {
    ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>();
    DefaultListModel model = new DefaultListModel();
    smdList.setModel(model);
    smdMessageLabel.setText("<html>Found several matches for identifier <b>"+id+"</b><br>Please select the correct one (or select several by holding down SHIFT or CRTL)</html>");
    for (int i=0;i<list.size();i++) {
        GeneIDmapping mapping = list.get(i);
        int start=(mapping.TSS<mapping.TES)?mapping.TSS:mapping.TES;
        int end  =(mapping.TSS<mapping.TES)?mapping.TES:mapping.TSS;
        String string=id+"_"+(i+1);
        string=padToLength(string, 20, false);
        string+=mapping.geneName;
        string=padToLength(string, 30, false);
        String chromosome=(mapping.chromosome.startsWith("chr")?"":"chr")+mapping.chromosome+":"+start+"-"+end;
        chromosome=padToLength(chromosome, 30, true);
        string+=chromosome+"       ";
        string+=((mapping.strand==Sequence.DIRECT)?"Direct          ":"Reverse          ");
        model.addElement(string);
    }
    smdList.setSelectedIndex(0);
    selectMappingDialog.pack();
    int height=selectMappingDialog.getHeight();
    int width=selectMappingDialog.getWidth();
    java.awt.Dimension size=new Dimension(width,height);
    if (engine.getClient() instanceof MotifLabGUI) size=((MotifLabGUI)engine.getClient()).getFrame().getSize();
    else if (engine.getClient() instanceof MinimalGUI) size=((MinimalGUI)engine.getClient()).getFrame().getSize();
    selectMappingDialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
    selectMappingDialog.setVisible(true);
    // proceeds here once the user clicks OK or Cancel (or double-clicks on an entry) ! (selectMappingDialog is a modal dialog)
    if (smdDialogCancelled) return null; // user pressed CANCEL button
    int[] indices=smdList.getSelectedIndices();
    for (int index:indices) {
        GeneIDmapping mapping=list.get(index);
        if (indices.length>1) mapping.geneID=mapping.geneID+"_"+(index+1); // if multiple mappings are selected for the same gene ID, add a suffix (eg ENSG00023423_2) to discriminate the sequences (the mapping.geneID will be used as sequence name later on)
        result.add(mapping);
    }
    return result;
}


private String padToLength(String string, int size, boolean front) {
    while (string.length()<size) {
        if (front) string=" "+string;
        else string+=" ";
    }
    return string;
}



/**
 * Parses the manual entries in the table under the "coordinates" tab and returns
 * a 2D Object matrix containing the data needed to specify sequences
 */
private Object[][] parseManualEntryTable() {
    int rows=advancedTableModel.getRowCount();
    int columns=advancedTableModel.getColumnCount();
    Object[][] result=new Object[rows][columns];
    for (int i=0;i<rows;i++) {
       for (int j=0;j<columns;j++) {
        result[i][j]=advancedTableModel.getValueAt(i, j);
       }
    }
    return result;
}

/** Loads and parses a file containing location information for sequences  */
private void loadManualEntryFileInBackground(final File file) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            StringBuilder text=new StringBuilder();
            ArrayList<Object[]> entries=new ArrayList<Object[]>();
            @Override
            public Boolean doInBackground() {
                progressbar.setVisible(true);
                progressbar.setIndeterminate(true);
                BufferedReader inputStream=null;
                try {
                    InputStream stream=MotifLabEngine.getInputStreamForFile(file);
                    inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
                    String line;
                    while((line=inputStream.readLine())!=null) {
                       // parse line here
                      line=line.trim();
                      if (!(line.isEmpty() || line.startsWith("#"))) {
                           line=line.replace("\t", ",");
                           String[] fields=line.split("\\s*,+\\s*");
                           if (fields.length==10) {
                               Object[] lineEntries=new Object[10];
                               lineEntries[0]=fields[0];
                               if (fields[1].startsWith("NCBI:")) fields[1]=fields[1].substring(5);
                               int taxid=Organism.getTaxonomyID(fields[1]);
                               if (taxid!=0) lineEntries[1]=new Integer(taxid);
                               else try {
                                   int val=Integer.parseInt(fields[1]);
                                   lineEntries[1]=new Integer(val);
                               } catch (NumberFormatException ne) {lineEntries[1]=null;}
                               lineEntries[2]=fields[2]; // build
                               if (fields[3].toLowerCase().startsWith("chr")) fields[3]=fields[3].substring(3);
                               lineEntries[3]=fields[3]; // chromosome
                               try {int start=Integer.parseInt(fields[4]);lineEntries[4]=new Integer(start);} catch (NumberFormatException e){}
                               try {int end=Integer.parseInt(fields[5]);lineEntries[5]=new Integer(end);} catch (NumberFormatException e){}
                               lineEntries[6]=fields[6]; // gene name
                               try {int tss=Integer.parseInt(fields[7]);lineEntries[7]=new Integer(tss);} catch (NumberFormatException e){}
                               try {int tes=Integer.parseInt(fields[8]);lineEntries[8]=new Integer(tes);} catch (NumberFormatException e){}
                               if (fields[9].equalsIgnoreCase("Reverse") || fields[9].equalsIgnoreCase("-") || fields[9].equalsIgnoreCase("-1")) lineEntries[9]=new Integer(-1); else lineEntries[9]=new Integer(1);
                               entries.add(lineEntries);
                           } else if (fields.length==8) {
                               Object[] lineEntries=new Object[10];
                               lineEntries[0]=fields[0];
                               lineEntries[2]=fields[1]; // build
                               lineEntries[1]=Organism.getOrganismForGenomeBuild(fields[1]); // derive organism from build
                               if (fields[2].toLowerCase().startsWith("chr")) fields[2]=fields[2].substring(3);
                               lineEntries[3]=fields[2]; // chromosome
                               try {int start=Integer.parseInt(fields[3]);lineEntries[4]=new Integer(start);} catch (NumberFormatException e){}
                               try {int end=Integer.parseInt(fields[4]);lineEntries[5]=new Integer(end);} catch (NumberFormatException e){}
                               lineEntries[6]=fields[0]; // gene name
                               try {int tss=Integer.parseInt(fields[5]);lineEntries[7]=new Integer(tss);} catch (NumberFormatException e){}
                               try {int tes=Integer.parseInt(fields[6]);lineEntries[8]=new Integer(tes);} catch (NumberFormatException e){}
                               if (fields[7].equalsIgnoreCase("Reverse") || fields[7].equalsIgnoreCase("-") || fields[7].equalsIgnoreCase("-1")) lineEntries[9]=new Integer(-1); else lineEntries[9]=new Integer(1);
                               entries.add(lineEntries);
                           } else throw new ParseError(errMsg("Expected 8 or 10 fields separated by TAB or comma on each line, but found",line));
                      }
                    }
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader in SequenceInputDialog.loadFileInBackground: "+ioe.getMessage());}
                }
                return Boolean.TRUE;
            }

            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false);
                progressbar.setVisible(false);
                if (ex instanceof ParseError) {
                    String message=ex.getMessage();
                    JOptionPane.showMessageDialog(SequenceInputDialog.this, message, "Import Error", JOptionPane.ERROR_MESSAGE);
                }
                else if (ex!=null) errorMessageLabel.setText(ex.getMessage());
                else {
                    try { // remove current contents
                       while (advancedTableModel.getRowCount()>0) advancedTableModel.removeRow(0);
                    } catch (Exception e) {}
                    for (Object[] entry:entries) {
                        advancedTableModel.addRow(entry);
                    }
                }
            }
        }; // end of SwingWorker class

        worker.execute();
    }

    private String errMsg(String message, String line) {
        if (line.length()>80) line=line.substring(0,80)+" [...]";
        return message+":\n\n"+line;
    }

    /** Loads a file (hopefully containing gene IDs) and displays it in the textarea */
    private void loadFileInBackground(final File file) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            StringBuilder text=new StringBuilder();
            @Override
            public Boolean doInBackground() {
                progressbar.setVisible(true);
                progressbar.setIndeterminate(true);
                BufferedReader inputStream=null;
                try {
                    InputStream stream=MotifLabEngine.getInputStreamForFile(file);
                    inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
                    String line;
                    while((line=inputStream.readLine())!=null) {text.append(line.trim());text.append("\n");}
                } catch (IOException e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader in SequenceInputDialog.loadFileInBackground: "+ioe.getMessage());}
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false);
                progressbar.setVisible(false);
                if (ex!=null) errorMessageLabel.setText(ex.getMessage());
                else geneIdentifiersTextarea.setText(text.toString());
            }
        }; // end of SwingWorker class
        worker.execute();
    }

    /** Loads a file (hopefully containing BED coordinates) and displays it in the textarea in the BED tab*/
    private void loadBEDFileInBackground(final File file) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            StringBuilder text=new StringBuilder();
            @Override
            public Boolean doInBackground() {
                progressbar.setVisible(true);
                progressbar.setIndeterminate(true);
                BufferedReader inputStream=null;
                try {
                    InputStream stream=MotifLabEngine.getInputStreamForFile(file);
                    inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
                    String line;
                    while((line=inputStream.readLine())!=null) {text.append(line.trim());text.append("\n");}
                } catch (IOException e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader in SequenceInputDialog.loadFileInBackground: "+ioe.getMessage());}
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false);
                progressbar.setVisible(false);
                if (ex!=null) errorMessageLabel.setText(ex.getMessage());
                else BEDTextArea.setText(text.toString());
            }
        }; // end of SwingWorker class
        worker.execute();
    }


    private void resolveIDsInBackground(final ArrayList<GeneIdentifier> list, final HashMap<GeneIdentifier, Object[]> posInfoMap, final boolean includeGO) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            ArrayList<GeneIDmapping> resolvedList=null;
            @Override
            public Boolean doInBackground() {
                progressbar.setVisible(true);
                progressbar.setIndeterminate(true);
                try {
                    resolvedList=idResolver.resolveIDs(list, includeGO);
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false);
                progressbar.setVisible(false);
                if (ex!=null) {
                    String errorMessage=ex.getMessage();
                    if (errorMessage!=null && errorMessage.startsWith("Query ERROR: caught BioMart::Exception::Usage:")) errorMessage=errorMessage.replace("Query ERROR: caught BioMart::Exception::Usage:","BioMart Query ERROR:");
                    if (errorMessage!=null && errorMessage.startsWith("Server returned HTTP response code: ")) {
                        engine.logMessage(errorMessage);
                        int errorCode=0;
                        int pos="Server returned HTTP response code: ".length();
                        String responseCodeString=errorMessage.substring(pos,errorMessage.indexOf(' ', pos));
                        String url = errorMessage.substring(errorMessage.indexOf("for URL: ", pos)+"for URL: ".length());
                        try {errorCode=Integer.parseInt(responseCodeString);}catch(NumberFormatException nfe) {}
                        // rephrase errorMessage based on HTTP response code
                        //if (errorCode==500) errorMessage="ERROR: BioMart server is temporary unavailable: "+url;
                    } else {
                        engine.logMessage(errorMessage);
                    }
                    if (ex instanceof java.io.FileNotFoundException) errorMessage="Biomart service not found: "+errorMessage;
                    errorMessageLabel.setText(errorMessage);
                    okButton.setEnabled(true);
                    // ex.printStackTrace(System.err);
                }
                else if (resolvedList==null) {
                    errorMessageLabel.setText("Unable to resolve gene IDs");
                    okButton.setEnabled(true);
                }
                else {
                    try {
                        processResolvedGeneIDs(list, resolvedList, posInfoMap, includeGO);
                    } catch (Exception e) {
                        errorMessageLabel.setText(e.getMessage());
                        okButton.setEnabled(true);
                    }
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }


    // Variables declaration - do not modify
    private javax.swing.JTextArea BEDTextArea;
    private javax.swing.JComboBox BEDcoordinateSystemCombobox;
    private javax.swing.JPanel BEDtab;
    private javax.swing.JPanel BEDtabControlsPanel;
    private javax.swing.JPanel BEDtabMainPanel;
    private javax.swing.JPanel BEDtop1;
    private javax.swing.JPanel BEDtop2;
    private javax.swing.JPanel CoordinatesTab;
    private javax.swing.JPanel GeneIDsTab;
    private javax.swing.JPanel additionalOptionsPanel;
    private javax.swing.JButton advancedExampleButton;
    private javax.swing.JLabel anchorLabel;
    private javax.swing.JComboBox anchorPointComboBox;
    private javax.swing.JCheckBox assignNewNamesToBEDregionsCheckbox;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton browseButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox dnaSequenceSourceCombobox;
    private javax.swing.JTextField dnaTrackNameTextfield;
    private javax.swing.JLabel downstreamLabel;
    private javax.swing.JSpinner downstreamSpinner;
    private javax.swing.JLabel enteridslabel;
    private javax.swing.JLabel errorMessageLabel;
    private javax.swing.JButton exampleBEDbutton;
    private javax.swing.ButtonGroup fileOrInput;
    private javax.swing.JLabel fromLabel;
    private javax.swing.JPanel geneIDsTopPanel;
    private javax.swing.JLabel geneIDtypeLabel;
    private javax.swing.JPanel geneIDtypePanel;
    private javax.swing.JTextArea geneIdentifiersTextarea;
    private javax.swing.JComboBox genomeBuildCombobox;
    private javax.swing.JComboBox genomeBuildComboboxBED;
    private javax.swing.JLabel genomeBuildLabel;
    private javax.swing.JPanel genomeBuildPanel;
    private javax.swing.JButton helpIconButton;
    private javax.swing.JLabel helpIconLabel;
    private javax.swing.JPanel helpPanel;
    private javax.swing.JComboBox identifierTypeCombobox;
    private javax.swing.JButton importBEDbutton;
    private javax.swing.JButton importButton;
    private javax.swing.JPanel importIDsPanel;
    private javax.swing.JCheckBox includeGOcheckbox;
    private javax.swing.JCheckBox includeSequenceInProtocolCheckbox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JCheckBox loadDNAtrackCheckbox;
    private javax.swing.JPanel loadDNAtrackPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox organismCombobox;
    private javax.swing.JComboBox organismComboboxBED;
    private javax.swing.JLabel organismLabel;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JDialog selectMappingDialog;
    private javax.swing.JPanel sequenceLocationPanel;
    private javax.swing.JCheckBox skipUnrecognizedCheckbox;
    private javax.swing.JPanel smdButtonsPanel;
    private javax.swing.JButton smdCancelButton;
    private javax.swing.JList smdList;
    private javax.swing.JLabel smdMessageLabel;
    private javax.swing.JPanel smdMiddlePanel;
    private javax.swing.JButton smdOkButton;
    private javax.swing.JPanel smdTopPanel;
    private javax.swing.JTabbedPane tabPanel;
    private javax.swing.JScrollPane tableScrollPane;
    private javax.swing.JPanel textAreaPanel;
    private javax.swing.JPanel topBoxPanel1;
    private javax.swing.JPanel topBoxPanel2;
    private javax.swing.JPanel topPanel;
    private javax.swing.JLabel upstreamLabel;
    private javax.swing.JSpinner upstreamSpinner;
    // End of variables declaration



private class SequenceTableRenderer_Organism extends DefaultTableCellRenderer {

    public SequenceTableRenderer_Organism() {
           super();
           this.setHorizontalAlignment(this.LEFT);
       }
    public void setValue(Object value) {
           if (value==null) {setText("");}
           else if (value instanceof Integer) {
               if (((Integer)value).intValue()==0) setForeground(java.awt.Color.RED);
               else setForeground(java.awt.Color.BLACK);
               setText(Organism.getCommonName(((Integer)value).intValue()));
           }  else {
               setForeground(java.awt.Color.RED);
               setText("* * *");
           }
       }
}// end class SequenceTableRenderer_Organism

private class SequenceTableRenderer_RightAlign extends DefaultTableCellRenderer {
    public SequenceTableRenderer_RightAlign() {
           super();
           this.setHorizontalAlignment(this.RIGHT);
       }
    public void setValue(Object value) {
           if (value!=null)setText(value.toString());
           else setText("");
       }
}// end class SequenceTableRenderer_RightAlign

private class SequenceTableRenderer_number extends DefaultTableCellRenderer {
    public SequenceTableRenderer_number() {
           super();
           this.setHorizontalAlignment(this.RIGHT);
       }
    public void setValue(Object value) {
           if (value==null) {setText("");}
           else if (value instanceof Integer) {
                    setForeground(java.awt.Color.BLACK);
                    setText(value.toString());
               }  else {
                    setForeground(java.awt.Color.RED);
                    setText("* * *");
               }

       }
}// end class SequenceTableRenderer_number

private class SequenceTableRenderer_Orientation extends DefaultTableCellRenderer {
    public SequenceTableRenderer_Orientation() {
           super();
           this.setHorizontalAlignment(this.LEFT);
       }
    public void setValue(Object value) {
           if (value==null || value.toString().isEmpty()) {setText("");}
           else if (value instanceof Integer) {
               setForeground(java.awt.Color.BLACK);
               if (((Integer)value).intValue()==Sequence.REVERSE) setText("Reverse");
               else setText("Direct");
           } else {
               setForeground(java.awt.Color.RED);
               setText("* * *");
           }
       }
}// end class SequenceTableRenderer_RightAlign


private class TableChangeListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            if (e.getType()==TableModelEvent.INSERT || e.getType()==TableModelEvent.DELETE) return;
            int col=e.getColumn();
            int row=e.getFirstRow();
            DefaultTableModel model=(DefaultTableModel)e.getSource();
            Object value=model.getValueAt(row, col);
            String colName=model.getColumnName(col);
            //System.err.println("Inserting value at "+row+":"+colName+" = "+value);
            if (colName.equals("Orientation")) {
                if (value==null) {}
                else if (value instanceof Integer) {
                   int intval=((Integer)value).intValue();
                   if (intval==1 || intval==-1) return;
                   else if (intval>=0) model.setValueAt(new Integer(1), row, col);
                   else model.setValueAt(new Integer(-1), row, col);
                } else if (value instanceof String) {
                    String strval=(String)value;
                    if (strval.equalsIgnoreCase("Reverse") || strval.equals("-") || strval.equals("-1")) model.setValueAt(new Integer(-1), row, col);
                    else model.setValueAt(new Integer(1), row, col);
                } else model.setValueAt(new Integer(1), row, col);
            } else if (colName.equals("Organism")) {
                if (value instanceof String) {
                    String name=(String)value;
                    if (name.startsWith("NCBI:")) name=name.substring(5);
                    int taxid=Organism.getTaxonomyID(name);
                    if (taxid!=0) model.setValueAt(new Integer(taxid), row, col);
                    else try {
                        int val=Integer.parseInt(name);
                        model.setValueAt(new Integer(val), row, col);
                    } catch (NumberFormatException ne) {model.setValueAt(new Integer(0), row, col);}
                }
            } else if (colName.equals("Chr")) {
                if (value!=null && !(value instanceof String)) {
                    model.setValueAt(value.toString(), row, col);
                }
            } else if (colName.equals("Gene name")) {
                if (value instanceof String && ((String)value).equals("null")) {
                    model.setValueAt(null, row, col);
                }
            } else if (colName.equals("Start") || colName.equals("End") || colName.equals("Gene TSS") || colName.equals("Gene TES")) {
                //if (value==null) System.err.println(colName+" : "+null);
                //else System.err.println(colName+" : "+value.getClass().toString()+" : "+value.toString());
                if (value instanceof String) {
                    String strval=((String)value).trim();
                    try {
                        int val=Integer.parseInt(strval);
                        model.setValueAt(new Integer(val), row, col);
                    } catch (NumberFormatException ne) {model.setValueAt(null, row, col);}
                } // else model.setValueAt("", row, col);
            }
        }

}

private class CellEditor_Orientation extends AbstractCellEditor implements TableCellEditor {
        JComboBox component=new JComboBox(new String[]{"Direct","Reverse"});
        public Object getCellEditorValue() {
            int selectedIndex=component.getSelectedIndex();
            if (selectedIndex==-1) return null;
            else if (selectedIndex==1) return new Integer(-1);
            else return new Integer(1);
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value!=null) {
                if (value.toString().equals("-1")) component.setSelectedIndex(1);
                else component.setSelectedIndex(0);
            }
            return component;
        }

}

private class CellEditor_Organism extends AbstractCellEditor implements TableCellEditor {
        JComboBox component;

        public CellEditor_Organism() {
             component=new JComboBox(Organism.getSupportedOrganisms());
             component.setEditable(true);
        }

        public Object getCellEditorValue() {
            String name=(String)component.getSelectedItem();
            if (name.startsWith("NCBI:")) name=name.substring(5);
            int taxid=Organism.getTaxonomyID(name);
            if (taxid!=0) return new Integer(taxid);
            try {
                int val=Integer.parseInt(name);
                return new Integer(val);
            } catch(NumberFormatException e) {}
            return new Integer(0);
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value==null) component.setSelectedIndex(0);
            else {
                if (value instanceof Integer) component.setSelectedItem(Organism.getCommonName(((Integer)value).intValue()));
                else {
                    try {
                        int organism=Integer.parseInt((value.toString()));
                        component.setSelectedItem(Organism.getCommonName(organism));
                    }
                    catch (NumberFormatException ne) {component.setSelectedIndex(0);}
                }
            }
            return component;
        }
}

/*
 *
 */
private static String[]exampleSequences=new String[] {
    "NTNG1\tHGNC Symbol\thg38\n"
        + "SPIB\tHGNC Symbol\thg38\n"
        + "NM_001258406\tRefSeq mRNA\thg38\n"
        + "NM_003998\tRefSeq mRNA\thg38\n"
        + "P35579\tUniProtKB Gene ID\thg38\n"
        + "TSPAN6\tUniProtKB Gene Symbol\thg38\n"   
        + "ENSG00000111249\tEnsembl Gene\thg38\n"
        + "ENSG00000187664\tEnsembl Gene\thg38\n"
        + "ENSG00000196358\tEnsembl Gene\thg38\n",
    "#Human smooth muscle genes markers\nENSG00000035403\nENSG00000100345\nENSG00000107796\nENSG00000130176\nENSG00000133026\nENSG00000149591\nENSG00000154553\nENSG00000163017\nENSG00000163431\nENSG00000175084",
    "#Human genes expressed in liver tissue\nENSG00000017427\nENSG00000115718\nENSG00000116833\nENSG00000118271\nENSG00000129965\nENSG00000131482\nENSG00000163631\nENSG00000167910\nENSG00000173531"
};
}



