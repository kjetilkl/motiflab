/*
 * MinimalGUI.java
 */

package motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;

import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.text.StyledDocument;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.LocalStorage;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.GeneIDResolver;
import motiflab.engine.GeneIDmapping;
import motiflab.engine.MotifLab;
import motiflab.engine.MotifLabClient;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Plugin;
import motiflab.engine.task.ProtocolTask;
import motiflab.engine.SystemError;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.datasource.DataRepositoryFile;
import motiflab.engine.operations.PromptConstraints;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.SerializedStandardProtocol;
import motiflab.engine.protocol.StandardProtocol;
import motiflab.engine.task.AddSequencesTask;



/**
 * This class represents a "minimal GUI" alternative to the full GUI.
 * This client is like a graphical version of the CLI-client
 */
public final class MinimalGUI extends FrameView implements MotifLabClient, java.beans.PropertyChangeListener {
    
    private MotifLabEngine engine=null;
    private MotifLab motiflabclient=null;
    private ProgressMonitor progressMonitor=null;
    private VisualizationSettings visualizationSettings=null;
    private StandardProtocol currentProtocol=null;
    private AddSequencesTask addSequencesFromDialogTask=null;       
    
    private int loggerLevel=3; // 1=show only errors, 2=show normal messages, 3=show execution log, 4=show every status update 
    
    private static final String lastusedlocationfilename="lastdir.ser"; // copied from MotifLabGUI
    private File lastUsedDirectory=null;   

    private String outputdirname;
    private String saveSessionFilename;
    private String inputSessionFilename;
    private String sequencesFilename;    
    private boolean saveOutput=true;
    private boolean assignNewNames=true;
    private String sequenceFormat="Auto";
    private String defaultGenomeBuild="hg18";    
    private int splitGroupSize=0;
    private HashMap<String,String> renameOutput=new HashMap<String, String>();
    
    private ProtocolTask protocolTask=null; // the currently executing task
    private boolean isAborted=false;
    private int[] taskprogress=null; // this is used to hand proper progress-reporting when splitting tasks into subtasks (e.g. with WGA)
    private long startTime=0L; // set at the start of protocol execution to time the event 
    private SingleFrameApplication thisApp=null;
    /**
     * Creates a new instance of MotifLab
     * @param app
     */
    public MinimalGUI(SingleFrameApplication app, MotifLabEngine motiflabEngine) {
        super(app); // initialize some GUI framework
        thisApp=app;
        try {
            ArrayList<Image>iconlist=new ArrayList<Image>(3);
            iconlist.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/MotifLabIcon_16.png")));
            iconlist.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/DNAhelix24.png")));
            iconlist.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/DNAhelix32.png")));
            this.getFrame().setIconImages(iconlist);
        } catch (Exception e) {}        
        setEngine(motiflabEngine); // initialize the MotifLabEngine   
        visualizationSettings=VisualizationSettings.getInstance(this);
        engine.executeStartupConfigurationProtocol();
        ApplicationContext context=getApplication().getContext();
        LocalStorage localStorage=context.getLocalStorage();
        try {
           Object lastused=localStorage.load(lastusedlocationfilename);
           if (lastused instanceof String) {
               File file=engine.getFile((String)lastused);
               if (file instanceof DataRepositoryFile && ((DataRepositoryFile)file).getRepository().isProtected()) {
                   // do not start up in a protected data repository since that could prompt the user to enter credentials before the GUI is up and running
               } 
               else setLastUsedDirectory(file);                            
           }
        } catch (Exception e) {}                  
        initComponents(); // lay out the components

        progressMonitor=new ProgressMonitor();
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);
//        this.setSize(700,500);
//        pack();
        organismCombobox.setSelectedIndex(0);
        organismSelectedinCombobox(null); // just in case
        dataList.setCellRenderer(new DataPanelCellRenderer(visualizationSettings));
        dataList.setModel(new DataObjectsPanelListModel(engine, new Class[]{Data.class}));
        dataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                mousePressedInDataObjectsPanel(evt);
            }
        });        
        dataInjectionsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        macrosTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        outputDataTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        dataInjectionsTable.getTableHeader().setReorderingAllowed(false);
        macrosTable.getTableHeader().setReorderingAllowed(false);
        outputDataTable.getTableHeader().setReorderingAllowed(false);
        
        dataInjectionsTable.getColumn("Value").setCellEditor(new DataInjectionsCellEditor());      
        File lastUsedDir=getLastUsedDirectory();
        if (lastUsedDir!=null) outputDirectoryTextfield.setText(lastUsedDir.getAbsolutePath());

        saveSessionCheckbox.doClick(); // this will deselect the option (since it is selected by design)   
        initializePluginsFromClient();        

    }
    
    public void setupReady() {
        restoreGUIsettings(); // initialize GUI elements with values used in previous session
        MGtabPanel.setSelectedComponent(protocolTab); 
        protocolEditorPane.setCaretPosition(0);
    }
    
    
    /**
     * This method should be called when the system exits.
     * It is called by MotifLabApp on shutdown
     */
    public void cleanup() {                        
        storeGUIsettings(); // initialize some of the GUI elements with the same values next time
        LocalStorage localStorage=getApplication().getContext().getLocalStorage();
        try {
           localStorage.save(lastUsedDirectory.getAbsolutePath(), lastusedlocationfilename);
        } catch (Exception e) {}
    }


    /**
     * Specifies an instance of MotifLab Engine to use by this GUI
     *
     * @param engine
     */
    private void setEngine(MotifLabEngine engine) {
        this.engine=engine;
        engine.setClient(this);
    }

    /**
     * Returns and instance of MotifLabEngine which is used by this GUI
     * @return an instance of MotifLabEngine
     */
    @Override
    public MotifLabEngine getEngine() {
        return engine;
    }

    /**
     * Returns an instance of the VisualizationSettings used by this GUI
     * @return the VisualizationSettings
     */
    @Override
    public final VisualizationSettings getVisualizationSettings() {
        return visualizationSettings;
    }
      
    @Override
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
      if (evt.getPropertyName().equals(ExecutableTask.STATUS_MESSAGE)) {
         String text = (String)(evt.getNewValue());
         if (text!=null && !text.isEmpty()) statusMessage(text);        
      } else if (evt.getPropertyName().equals(ExecutableTask.PROGRESS)) {
         Integer value = (Integer)(evt.getNewValue());
         if (taskprogress!=null) setProgress(taskprogress,value); 
         else setProgress(value);
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

        MGtopPanel = new javax.swing.JPanel();
        MGmainPanel = new javax.swing.JPanel();
        MGtabPanel = new javax.swing.JTabbedPane();
        protocolTab = new javax.swing.JPanel();
        jPanel26 = new javax.swing.JPanel();
        jpanelx2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        protocolFilenameTextfield = new javax.swing.JTextField();
        protocolBrowse = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        protocolEditorPane = new javax.swing.JTextPane();
        inputTab = new javax.swing.JPanel();
        inputBottomPanel = new javax.swing.JPanel();
        promptInjectionPanel = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        dataInjectionsTable = new EditOnFocusTable();
        jPanel17 = new javax.swing.JPanel();
        addToDataInjections = new javax.swing.JButton();
        removeFromDataInjections = new javax.swing.JButton();
        macrosPanel = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        macrosTable = new EditOnFocusTable();
        jPanel18 = new javax.swing.JPanel();
        addToMacros = new javax.swing.JButton();
        removeFromMacros = new javax.swing.JButton();
        jPanel25 = new javax.swing.JPanel();
        inputTopPanel = new javax.swing.JPanel();
        inputSequencePanel = new javax.swing.JPanel();
        wgaRadioButton = new javax.swing.JRadioButton();
        noSequencesRadioButton = new javax.swing.JRadioButton();
        sequencesFilenameTextfield = new javax.swing.JTextField();
        sequencesBrowseButton = new javax.swing.JButton();
        sequencesRadioButton = new javax.swing.JRadioButton();
        dataformatLabel = new javax.swing.JLabel();
        dataformatCombobox = new javax.swing.JComboBox();
        customFormatTextfield = new javax.swing.JTextField();
        groupsizeLabel = new javax.swing.JLabel();
        splitGroupsSpinner = new javax.swing.JSpinner();
        assignNewNamesCheckbox = new javax.swing.JCheckBox();
        wgaRegionTextfield = new javax.swing.JTextField();
        wgaInnerPanel = new javax.swing.JPanel();
        wgaSegmentSizeLabel = new javax.swing.JLabel();
        wgaSegmentSizeSpinner = new javax.swing.JSpinner();
        wgaCollectionSizeLabel = new javax.swing.JLabel();
        wgaCollectionSizeSpinner = new javax.swing.JSpinner();
        wgaOverlapLabel = new javax.swing.JLabel();
        wgaOverlapSpinner = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        sequencesDialogRadioButton = new javax.swing.JRadioButton();
        launchInputSequencesDialogButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        inputSessionLabel = new javax.swing.JLabel();
        inputSessionTextfield = new javax.swing.JTextField();
        inputSessionBrowseButton = new javax.swing.JButton();
        genomebuildCombobox = new javax.swing.JComboBox();
        organismCombobox = new javax.swing.JComboBox();
        defaultOrganismLabel = new javax.swing.JLabel();
        defaultGenomeBuildLabel = new javax.swing.JLabel();
        outputTab = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        outputDataTablePanel = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        outputDataTable = new EditOnFocusTable();
        jPanel19 = new javax.swing.JPanel();
        addToOutputData = new javax.swing.JButton();
        removeFromOutputData = new javax.swing.JButton();
        jPanel21 = new javax.swing.JPanel();
        jPanel24 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        outputDirectoryTextfield = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        saveSessionTextfield = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        saveOutputCheckbox = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        saveSessionCheckbox = new javax.swing.JCheckBox();
        logTab = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        logPanel = new javax.swing.JTextPane();
        jPanel2 = new javax.swing.JPanel();
        clearLogButton = new javax.swing.JButton();
        resultsTab = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        dataListPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        showInPlainFormatCheckbox = new javax.swing.JCheckBox();
        jPanel22 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        dataList = new javax.swing.JList();
        resultsPanelOuter = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        shownDataObjectNameLabel = new javax.swing.JLabel();
        resultsPanel = new javax.swing.JPanel();
        jPanel23 = new javax.swing.JPanel();
        saveShownDataButton = new javax.swing.JButton();
        MGbuttonsPanel = new javax.swing.JPanel();
        controlsPanel = new javax.swing.JPanel();
        executeButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        exitButtonPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem fileExitMenuItem = new javax.swing.JMenuItem();
        MGstatusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        inputButtonGroup = new javax.swing.ButtonGroup();

        MGtopPanel.setName("MGtopPanel"); // NOI18N
        MGtopPanel.setLayout(new java.awt.BorderLayout());

        MGmainPanel.setName("MGmainPanel"); // NOI18N
        MGmainPanel.setLayout(new java.awt.BorderLayout());

        MGtabPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        MGtabPanel.setName("MGtabPanel"); // NOI18N

        protocolTab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        protocolTab.setName("protocolTab"); // NOI18N
        protocolTab.setLayout(new java.awt.BorderLayout());

        jPanel26.setName("jPanel26"); // NOI18N
        jPanel26.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jpanelx2.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 4, 2, 4));
        jpanelx2.setMaximumSize(new java.awt.Dimension(800, 2147483647));
        jpanelx2.setName("jpanelx2"); // NOI18N
        jpanelx2.setLayout(new java.awt.GridBagLayout());

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(MinimalGUI.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jpanelx2.add(jLabel1, gridBagConstraints);

        protocolFilenameTextfield.setColumns(50);
        protocolFilenameTextfield.setText(resourceMap.getString("protocolFilenameTextfield.text")); // NOI18N
        protocolFilenameTextfield.setMaximumSize(new java.awt.Dimension(800, 30));
        protocolFilenameTextfield.setName("protocolFilenameTextfield"); // NOI18N
        protocolFilenameTextfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolFileNameUpdated(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        jpanelx2.add(protocolFilenameTextfield, gridBagConstraints);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MinimalGUI.class, this);
        protocolBrowse.setAction(actionMap.get("protocolBrowseButtonPressed")); // NOI18N
        protocolBrowse.setText(resourceMap.getString("protocolBrowse.text")); // NOI18N
        protocolBrowse.setName("protocolBrowse"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jpanelx2.add(protocolBrowse, gridBagConstraints);

        jPanel26.add(jpanelx2);

        protocolTab.add(jPanel26, java.awt.BorderLayout.NORTH);

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 8, 8));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        protocolEditorPane.setName("protocolEditorPane"); // NOI18N
        jScrollPane2.setViewportView(protocolEditorPane);

        jPanel1.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel5.add(jPanel1, java.awt.BorderLayout.CENTER);

        protocolTab.add(jPanel5, java.awt.BorderLayout.CENTER);

        MGtabPanel.addTab(resourceMap.getString("protocolTab.TabConstraints.tabTitle"), protocolTab); // NOI18N

        inputTab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        inputTab.setName("inputTab"); // NOI18N
        inputTab.setLayout(new java.awt.BorderLayout());

        inputBottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 16, 4, 16));
        inputBottomPanel.setName("inputBottomPanel"); // NOI18N
        inputBottomPanel.setLayout(new javax.swing.BoxLayout(inputBottomPanel, javax.swing.BoxLayout.X_AXIS));

        promptInjectionPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 16), javax.swing.BorderFactory.createTitledBorder("")));
        promptInjectionPanel.setAlignmentY(0.0F);
        promptInjectionPanel.setMaximumSize(new java.awt.Dimension(600, 400));
        promptInjectionPanel.setName("promptInjectionPanel"); // NOI18N
        promptInjectionPanel.setPreferredSize(new java.awt.Dimension(600, 400));
        promptInjectionPanel.setLayout(new java.awt.BorderLayout());

        jPanel13.setName("jPanel13"); // NOI18N
        jPanel13.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel6.setFont(resourceMap.getFont("jLabel6.font")); // NOI18N
        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N
        jPanel13.add(jLabel6);

        promptInjectionPanel.add(jPanel13, java.awt.BorderLayout.PAGE_START);

        jPanel12.setName("jPanel12"); // NOI18N
        jPanel12.setLayout(new java.awt.BorderLayout());

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        dataInjectionsTable.setModel(getDataInjectionTableModel());
        dataInjectionsTable.setName("dataInjectionsTable"); // NOI18N
        dataInjectionsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane4.setViewportView(dataInjectionsTable);

        jPanel12.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        promptInjectionPanel.add(jPanel12, java.awt.BorderLayout.CENTER);

        jPanel17.setName("jPanel17"); // NOI18N

        addToDataInjections.setText(resourceMap.getString("addToDataInjections.text")); // NOI18N
        addToDataInjections.setName("addToDataInjections"); // NOI18N
        addToDataInjections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrRemoveFromTable(evt);
            }
        });
        jPanel17.add(addToDataInjections);

        removeFromDataInjections.setText(resourceMap.getString("removeFromDataInjections.text")); // NOI18N
        removeFromDataInjections.setName("removeFromDataInjections"); // NOI18N
        removeFromDataInjections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrRemoveFromTable(evt);
            }
        });
        jPanel17.add(removeFromDataInjections);

        promptInjectionPanel.add(jPanel17, java.awt.BorderLayout.SOUTH);

        inputBottomPanel.add(promptInjectionPanel);

        macrosPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 8), javax.swing.BorderFactory.createTitledBorder("")));
        macrosPanel.setAlignmentY(0.0F);
        macrosPanel.setMaximumSize(new java.awt.Dimension(600, 400));
        macrosPanel.setName("macrosPanel"); // NOI18N
        macrosPanel.setPreferredSize(new java.awt.Dimension(600, 400));
        macrosPanel.setLayout(new java.awt.BorderLayout());

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel7.setFont(resourceMap.getFont("jLabel7.font")); // NOI18N
        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N
        jPanel7.add(jLabel7);

        macrosPanel.add(jPanel7, java.awt.BorderLayout.PAGE_START);

        jPanel11.setName("jPanel11"); // NOI18N
        jPanel11.setLayout(new java.awt.BorderLayout());

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        macrosTable.setModel(getMacroTableModel());
        macrosTable.setName("macrosTable"); // NOI18N
        macrosTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane5.setViewportView(macrosTable);

        jPanel11.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        macrosPanel.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel18.setName("jPanel18"); // NOI18N

        addToMacros.setText(resourceMap.getString("addToMacros.text")); // NOI18N
        addToMacros.setName("addToMacros"); // NOI18N
        addToMacros.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrRemoveFromTable(evt);
            }
        });
        jPanel18.add(addToMacros);

        removeFromMacros.setText(resourceMap.getString("removeFromMacros.text")); // NOI18N
        removeFromMacros.setName("removeFromMacros"); // NOI18N
        removeFromMacros.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrRemoveFromTable(evt);
            }
        });
        jPanel18.add(removeFromMacros);

        macrosPanel.add(jPanel18, java.awt.BorderLayout.SOUTH);

        inputBottomPanel.add(macrosPanel);

        inputTab.add(inputBottomPanel, java.awt.BorderLayout.CENTER);

        jPanel25.setName("jPanel25"); // NOI18N
        jPanel25.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        inputTopPanel.setName("inputTopPanel"); // NOI18N
        inputTopPanel.setLayout(new java.awt.BorderLayout());

        inputSequencePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputSequencePanel.setName("inputSequencePanel"); // NOI18N
        inputSequencePanel.setLayout(new java.awt.GridBagLayout());

        inputButtonGroup.add(wgaRadioButton);
        wgaRadioButton.setText(resourceMap.getString("wgaRadioButton.text")); // NOI18N
        wgaRadioButton.setName("wgaRadioButton"); // NOI18N
        wgaRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chromosomeRadioButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(32, 8, 8, 8);
        inputSequencePanel.add(wgaRadioButton, gridBagConstraints);

        inputButtonGroup.add(noSequencesRadioButton);
        noSequencesRadioButton.setSelected(true);
        noSequencesRadioButton.setText(resourceMap.getString("noSequencesRadioButton.text")); // NOI18N
        noSequencesRadioButton.setName("noSequencesRadioButton"); // NOI18N
        noSequencesRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noSequencesRadioButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 24, 8);
        inputSequencePanel.add(noSequencesRadioButton, gridBagConstraints);

        sequencesFilenameTextfield.setText(resourceMap.getString("sequencesFilenameTextfield.text")); // NOI18N
        sequencesFilenameTextfield.setName("sequencesFilenameTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        inputSequencePanel.add(sequencesFilenameTextfield, gridBagConstraints);

        sequencesBrowseButton.setAction(actionMap.get("browseSequencesFile")); // NOI18N
        sequencesBrowseButton.setText(resourceMap.getString("sequencesBrowseButton.text")); // NOI18N
        sequencesBrowseButton.setName("sequencesBrowseButton"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        inputSequencePanel.add(sequencesBrowseButton, gridBagConstraints);

        inputButtonGroup.add(sequencesRadioButton);
        sequencesRadioButton.setText(resourceMap.getString("sequencesRadioButton.text")); // NOI18N
        sequencesRadioButton.setName("sequencesRadioButton"); // NOI18N
        sequencesRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sequencesFileRadioButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        inputSequencePanel.add(sequencesRadioButton, gridBagConstraints);

        dataformatLabel.setText(resourceMap.getString("dataformatLabel.text")); // NOI18N
        dataformatLabel.setName("dataformatLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 8);
        inputSequencePanel.add(dataformatLabel, gridBagConstraints);

        dataformatCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Auto", "Gene ID", "Manual 4", "Manual 8", "Manual 10", "Custom" }));
        dataformatCombobox.setName("dataformatCombobox"); // NOI18N
        dataformatCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newDataFormatSelected(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 8);
        inputSequencePanel.add(dataformatCombobox, gridBagConstraints);

        customFormatTextfield.setColumns(20);
        customFormatTextfield.setText(resourceMap.getString("customFormatTextfield.text")); // NOI18N
        customFormatTextfield.setEnabled(false);
        customFormatTextfield.setName("customFormatTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 8);
        inputSequencePanel.add(customFormatTextfield, gridBagConstraints);

        groupsizeLabel.setText(resourceMap.getString("groupsizeLabel.text")); // NOI18N
        groupsizeLabel.setName("groupsizeLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 32, 8, 8);
        inputSequencePanel.add(groupsizeLabel, gridBagConstraints);

        splitGroupsSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), null, Integer.valueOf(100000), Integer.valueOf(1)));
        splitGroupsSpinner.setToolTipText(resourceMap.getString("splitGroupsSpinner.toolTipText")); // NOI18N
        splitGroupsSpinner.setName("splitGroupsSpinner"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 0);
        inputSequencePanel.add(splitGroupsSpinner, gridBagConstraints);

        assignNewNamesCheckbox.setText(resourceMap.getString("assignNewNamesCheckbox.text")); // NOI18N
        assignNewNamesCheckbox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        assignNewNamesCheckbox.setName("assignNewNamesCheckbox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 32, 8, 8);
        inputSequencePanel.add(assignNewNamesCheckbox, gridBagConstraints);

        wgaRegionTextfield.setColumns(16);
        wgaRegionTextfield.setText(resourceMap.getString("wgaRegionTextfield.text")); // NOI18N
        wgaRegionTextfield.setName("wgaRegionTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(32, 0, 8, 0);
        inputSequencePanel.add(wgaRegionTextfield, gridBagConstraints);

        wgaInnerPanel.setName("wgaInnerPanel"); // NOI18N
        wgaInnerPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        wgaSegmentSizeLabel.setText(resourceMap.getString("wgaSegmentSizeLabel.text")); // NOI18N
        wgaSegmentSizeLabel.setName("wgaSegmentSizeLabel"); // NOI18N
        wgaInnerPanel.add(wgaSegmentSizeLabel);

        wgaSegmentSizeSpinner.setModel(new javax.swing.SpinnerNumberModel(10000, 10, 10000000, 1));
        wgaSegmentSizeSpinner.setName("wgaSegmentSizeSpinner"); // NOI18N
        wgaInnerPanel.add(wgaSegmentSizeSpinner);

        wgaCollectionSizeLabel.setText(resourceMap.getString("wgaCollectionSizeLabel.text")); // NOI18N
        wgaCollectionSizeLabel.setName("wgaCollectionSizeLabel"); // NOI18N
        wgaInnerPanel.add(wgaCollectionSizeLabel);

        wgaCollectionSizeSpinner.setModel(new javax.swing.SpinnerNumberModel(100, 1, 100000, 1));
        wgaCollectionSizeSpinner.setName("wgaCollectionSizeSpinner"); // NOI18N
        wgaInnerPanel.add(wgaCollectionSizeSpinner);

        wgaOverlapLabel.setText(resourceMap.getString("wgaOverlapLabel.text")); // NOI18N
        wgaOverlapLabel.setName("wgaOverlapLabel"); // NOI18N
        wgaInnerPanel.add(wgaOverlapLabel);

        wgaOverlapSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, 10000000, 1));
        wgaOverlapSpinner.setName("wgaOverlapSpinner"); // NOI18N
        wgaInnerPanel.add(wgaOverlapSpinner);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(24, 0, 0, 0);
        inputSequencePanel.add(wgaInnerPanel, gridBagConstraints);

        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 24, 8);
        inputSequencePanel.add(jLabel9, gridBagConstraints);

        inputButtonGroup.add(sequencesDialogRadioButton);
        sequencesDialogRadioButton.setText(resourceMap.getString("sequencesDialogRadioButton.text")); // NOI18N
        sequencesDialogRadioButton.setName("sequencesDialogRadioButton"); // NOI18N
        sequencesDialogRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sequencesDialogRadioButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 24, 8);
        inputSequencePanel.add(sequencesDialogRadioButton, gridBagConstraints);

        launchInputSequencesDialogButton.setText(resourceMap.getString("launchInputSequencesDialogButton.text")); // NOI18N
        launchInputSequencesDialogButton.setName("launchInputSequencesDialogButton"); // NOI18N
        launchInputSequencesDialogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launchInputSequencesDialogButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 24, 0);
        inputSequencePanel.add(launchInputSequencesDialogButton, gridBagConstraints);

        inputTopPanel.add(inputSequencePanel, java.awt.BorderLayout.NORTH);

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(32, 16, 32, 8));
        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.GridBagLayout());

        inputSessionLabel.setText(resourceMap.getString("inputSessionLabel.text")); // NOI18N
        inputSessionLabel.setName("inputSessionLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jPanel6.add(inputSessionLabel, gridBagConstraints);

        inputSessionTextfield.setText(resourceMap.getString("inputSessionTextfield.text")); // NOI18N
        inputSessionTextfield.setName("inputSessionTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        jPanel6.add(inputSessionTextfield, gridBagConstraints);

        inputSessionBrowseButton.setAction(actionMap.get("browseInputSessionFile")); // NOI18N
        inputSessionBrowseButton.setText(resourceMap.getString("inputSessionBrowseButton.text")); // NOI18N
        inputSessionBrowseButton.setName("inputSessionBrowseButton"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jPanel6.add(inputSessionBrowseButton, gridBagConstraints);

        genomebuildCombobox.setName("genomebuildCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 8);
        jPanel6.add(genomebuildCombobox, gridBagConstraints);

        organismCombobox.setModel(getOrganismsModel());
        organismCombobox.setName("organismCombobox"); // NOI18N
        organismCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                organismSelectedinCombobox(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 8);
        jPanel6.add(organismCombobox, gridBagConstraints);

        defaultOrganismLabel.setText(resourceMap.getString("defaultOrganismLabel.text")); // NOI18N
        defaultOrganismLabel.setName("defaultOrganismLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jPanel6.add(defaultOrganismLabel, gridBagConstraints);

        defaultGenomeBuildLabel.setText(resourceMap.getString("defaultGenomeBuildLabel.text")); // NOI18N
        defaultGenomeBuildLabel.setName("defaultGenomeBuildLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 22, 8, 8);
        jPanel6.add(defaultGenomeBuildLabel, gridBagConstraints);

        inputTopPanel.add(jPanel6, java.awt.BorderLayout.SOUTH);

        jPanel25.add(inputTopPanel);

        inputTab.add(jPanel25, java.awt.BorderLayout.NORTH);

        MGtabPanel.addTab(resourceMap.getString("inputTab.TabConstraints.tabTitle"), inputTab); // NOI18N

        outputTab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        outputTab.setName("outputTab"); // NOI18N
        outputTab.setLayout(new java.awt.BorderLayout());

        jPanel9.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 8, 8));
        jPanel9.setName("jPanel9"); // NOI18N
        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.LINE_AXIS));

        outputDataTablePanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8), javax.swing.BorderFactory.createTitledBorder("")));
        outputDataTablePanel.setAlignmentY(0.0F);
        outputDataTablePanel.setMaximumSize(new java.awt.Dimension(600, 400));
        outputDataTablePanel.setName("outputDataTablePanel"); // NOI18N
        outputDataTablePanel.setPreferredSize(new java.awt.Dimension(600, 400));
        outputDataTablePanel.setLayout(new java.awt.BorderLayout());

        jPanel15.setName("jPanel15"); // NOI18N
        jPanel15.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel8.setFont(resourceMap.getFont("jLabel8.font")); // NOI18N
        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N
        jPanel15.add(jLabel8);

        outputDataTablePanel.add(jPanel15, java.awt.BorderLayout.PAGE_START);

        jPanel16.setName("jPanel16"); // NOI18N
        jPanel16.setLayout(new java.awt.BorderLayout());

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        outputDataTable.setModel(getOutputDataTableModel());
        outputDataTable.setName("outputDataTable"); // NOI18N
        jScrollPane6.setViewportView(outputDataTable);

        jPanel16.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        outputDataTablePanel.add(jPanel16, java.awt.BorderLayout.CENTER);

        jPanel19.setName("jPanel19"); // NOI18N

        addToOutputData.setText(resourceMap.getString("addToOutputData.text")); // NOI18N
        addToOutputData.setName("addToOutputData"); // NOI18N
        addToOutputData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrRemoveFromTable(evt);
            }
        });
        jPanel19.add(addToOutputData);

        removeFromOutputData.setText(resourceMap.getString("removeFromOutputData.text")); // NOI18N
        removeFromOutputData.setName("removeFromOutputData"); // NOI18N
        removeFromOutputData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrRemoveFromTable(evt);
            }
        });
        jPanel19.add(removeFromOutputData);

        outputDataTablePanel.add(jPanel19, java.awt.BorderLayout.SOUTH);

        jPanel9.add(outputDataTablePanel);

        jPanel21.setName("jPanel21"); // NOI18N
        jPanel21.setLayout(new java.awt.BorderLayout());
        jPanel9.add(jPanel21);

        outputTab.add(jPanel9, java.awt.BorderLayout.CENTER);

        jPanel24.setName("jPanel24"); // NOI18N
        jPanel24.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jPanel10.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 32, 8));
        jPanel10.setName("jPanel10"); // NOI18N
        jPanel10.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jPanel10.add(jLabel3, gridBagConstraints);

        outputDirectoryTextfield.setColumns(50);
        outputDirectoryTextfield.setText(resourceMap.getString("outputDirectoryTextfield.text")); // NOI18N
        outputDirectoryTextfield.setName("outputDirectoryTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.8;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        jPanel10.add(outputDirectoryTextfield, gridBagConstraints);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jPanel10.add(jLabel4, gridBagConstraints);

        saveSessionTextfield.setColumns(30);
        saveSessionTextfield.setText(resourceMap.getString("saveSessionTextfield.text")); // NOI18N
        saveSessionTextfield.setName("saveSessionTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        jPanel10.add(saveSessionTextfield, gridBagConstraints);

        jButton2.setAction(actionMap.get("browseOutputDirectory")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jPanel10.add(jButton2, gridBagConstraints);

        saveOutputCheckbox.setSelected(true);
        saveOutputCheckbox.setText(resourceMap.getString("saveOutputCheckbox.text")); // NOI18N
        saveOutputCheckbox.setName("saveOutputCheckbox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        jPanel10.add(saveOutputCheckbox, gridBagConstraints);

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        jPanel10.add(jLabel5, gridBagConstraints);

        saveSessionCheckbox.setSelected(true);
        saveSessionCheckbox.setText(resourceMap.getString("saveSessionCheckbox.text")); // NOI18N
        saveSessionCheckbox.setName("saveSessionCheckbox"); // NOI18N
        saveSessionCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputSessionCheckboxPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 9);
        jPanel10.add(saveSessionCheckbox, gridBagConstraints);

        jPanel24.add(jPanel10);

        outputTab.add(jPanel24, java.awt.BorderLayout.NORTH);

        MGtabPanel.addTab(resourceMap.getString("outputTab.TabConstraints.tabTitle"), outputTab); // NOI18N

        logTab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        logTab.setName("logTab"); // NOI18N
        logTab.setLayout(new java.awt.BorderLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 2, 8));
        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        logPanel.setEditable(false);
        logPanel.setName("logPanel"); // NOI18N
        jScrollPane1.setViewportView(logPanel);

        jPanel4.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        logTab.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        clearLogButton.setText(resourceMap.getString("clearLogButton.text")); // NOI18N
        clearLogButton.setName("clearLogButton"); // NOI18N
        clearLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearLogButtonPressed(evt);
            }
        });
        jPanel2.add(clearLogButton);

        logTab.add(jPanel2, java.awt.BorderLayout.SOUTH);

        MGtabPanel.addTab(resourceMap.getString("logTab.TabConstraints.tabTitle"), logTab); // NOI18N

        resultsTab.setName("resultsTab"); // NOI18N
        resultsTab.setLayout(new java.awt.BorderLayout());

        jSplitPane1.setName("jSplitPane1"); // NOI18N

        dataListPanel.setName("dataListPanel"); // NOI18N
        dataListPanel.setPreferredSize(new java.awt.Dimension(250, 100));
        dataListPanel.setLayout(new java.awt.BorderLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 0, 0));
        jPanel3.setName("jPanel3"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel3.add(jLabel2);

        dataListPanel.add(jPanel3, java.awt.BorderLayout.PAGE_START);

        jPanel20.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 8, 8));
        jPanel20.setName("jPanel20"); // NOI18N
        jPanel20.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        showInPlainFormatCheckbox.setText(resourceMap.getString("showInPlainFormatCheckbox.text")); // NOI18N
        showInPlainFormatCheckbox.setName("showInPlainFormatCheckbox"); // NOI18N
        showInPlainFormatCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showInPlainFormatPressed(evt);
            }
        });
        jPanel20.add(showInPlainFormatCheckbox);

        dataListPanel.add(jPanel20, java.awt.BorderLayout.SOUTH);

        jPanel22.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 6));
        jPanel22.setName("jPanel22"); // NOI18N
        jPanel22.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        dataList.setName("dataList"); // NOI18N
        jScrollPane3.setViewportView(dataList);

        jPanel22.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        dataListPanel.add(jPanel22, java.awt.BorderLayout.CENTER);

        jSplitPane1.setLeftComponent(dataListPanel);

        resultsPanelOuter.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 8, 8));
        resultsPanelOuter.setName("resultsPanelOuter"); // NOI18N
        resultsPanelOuter.setLayout(new java.awt.BorderLayout());

        jPanel8.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 0, 0));
        jPanel8.setName("jPanel8"); // NOI18N

        shownDataObjectNameLabel.setText(resourceMap.getString("shownDataObjectNameLabel.text")); // NOI18N
        shownDataObjectNameLabel.setName("shownDataObjectNameLabel"); // NOI18N
        jPanel8.add(shownDataObjectNameLabel);

        resultsPanelOuter.add(jPanel8, java.awt.BorderLayout.PAGE_START);

        resultsPanel.setName("resultsPanel"); // NOI18N
        resultsPanel.setLayout(new java.awt.BorderLayout());
        resultsPanelOuter.add(resultsPanel, java.awt.BorderLayout.CENTER);

        jPanel23.setName("jPanel23"); // NOI18N
        jPanel23.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        saveShownDataButton.setAction(actionMap.get("saveShownDataObject")); // NOI18N
        saveShownDataButton.setText(resourceMap.getString("saveShownDataButton.text")); // NOI18N
        saveShownDataButton.setName("saveShownDataButton"); // NOI18N
        jPanel23.add(saveShownDataButton);

        resultsPanelOuter.add(jPanel23, java.awt.BorderLayout.PAGE_END);

        jSplitPane1.setRightComponent(resultsPanelOuter);

        resultsTab.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        MGtabPanel.addTab(resourceMap.getString("resultsTab.TabConstraints.tabTitle"), resultsTab); // NOI18N

        MGmainPanel.add(MGtabPanel, java.awt.BorderLayout.CENTER);
        MGtabPanel.getAccessibleContext().setAccessibleName(resourceMap.getString("jTabbedPane1.AccessibleContext.accessibleName")); // NOI18N

        MGtopPanel.add(MGmainPanel, java.awt.BorderLayout.CENTER);

        MGbuttonsPanel.setName("MGbuttonsPanel"); // NOI18N
        MGbuttonsPanel.setPreferredSize(new java.awt.Dimension(1293, 46));
        MGbuttonsPanel.setLayout(new java.awt.BorderLayout());

        controlsPanel.setName("controlsPanel"); // NOI18N
        controlsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 16, 5));

        executeButton.setAction(actionMap.get("executeProtocol")); // NOI18N
        executeButton.setText(resourceMap.getString("executeButton.text")); // NOI18N
        executeButton.setName("executeButton"); // NOI18N
        controlsPanel.add(executeButton);

        stopButton.setAction(actionMap.get("stopAction")); // NOI18N
        stopButton.setText(resourceMap.getString("stopButton.text")); // NOI18N
        stopButton.setName("stopButton"); // NOI18N
        controlsPanel.add(stopButton);

        MGbuttonsPanel.add(controlsPanel, java.awt.BorderLayout.CENTER);

        exitButtonPanel.setName("exitButtonPanel"); // NOI18N
        exitButtonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        MGbuttonsPanel.add(exitButtonPanel, java.awt.BorderLayout.EAST);

        MGtopPanel.add(MGbuttonsPanel, java.awt.BorderLayout.SOUTH);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        fileExitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        fileExitMenuItem.setName("fileExitMenuItem"); // NOI18N
        fileMenu.add(fileExitMenuItem);

        menuBar.add(fileMenu);

        MGstatusPanel.setMaximumSize(new java.awt.Dimension(32767, 30));
        MGstatusPanel.setMinimumSize(new java.awt.Dimension(0, 30));
        MGstatusPanel.setName("MGstatusPanel"); // NOI18N
        MGstatusPanel.setPreferredSize(new java.awt.Dimension(2, 30));

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout MGstatusPanelLayout = new javax.swing.GroupLayout(MGstatusPanel);
        MGstatusPanel.setLayout(MGstatusPanelLayout);
        MGstatusPanelLayout.setHorizontalGroup(
            MGstatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1293, Short.MAX_VALUE)
            .addGroup(MGstatusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1119, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        MGstatusPanelLayout.setVerticalGroup(
            MGstatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MGstatusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addGroup(MGstatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(MGtopPanel);
        setMenuBar(menuBar);
        setStatusBar(MGstatusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void protocolFileNameUpdated(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolFileNameUpdated
        loadProtocolFile(protocolFilenameTextfield.getText().trim());
    }//GEN-LAST:event_protocolFileNameUpdated

    private void clearLogButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearLogButtonPressed
        logPanel.setText("");
    }//GEN-LAST:event_clearLogButtonPressed

    private void addOrRemoveFromTable(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addOrRemoveFromTable
        if (evt.getSource()==addToDataInjections) {
            DefaultTableModel model=(DefaultTableModel)dataInjectionsTable.getModel();
            model.addRow(new String[]{"",""});
            dataInjectionsTable.revalidate();
            dataInjectionsTable.repaint();            
        } else if (evt.getSource()==addToMacros) {
            DefaultTableModel model=(DefaultTableModel)macrosTable.getModel();
            model.addRow(new String[]{"",""});    
            macrosTable.revalidate();
            macrosTable.repaint();
        } else if (evt.getSource()==addToOutputData) {
            DefaultTableModel model=(DefaultTableModel)outputDataTable.getModel();
            model.addRow(new String[]{"",""});    
            outputDataTable.revalidate();
            outputDataTable.repaint();
        } else if (evt.getSource()==removeFromDataInjections) {
            int[] selected=dataInjectionsTable.getSelectedRows();
            Arrays.sort(selected); // just in case
            for (int i=0;i<selected.length;i++) {
                int index=selected[selected.length-(i+1)];
                DefaultTableModel model=(DefaultTableModel)dataInjectionsTable.getModel();    
                model.removeRow(dataInjectionsTable.convertRowIndexToModel(index));
            }
            dataInjectionsTable.clearSelection();       
            dataInjectionsTable.revalidate();
            dataInjectionsTable.repaint();
        } else if (evt.getSource()==removeFromMacros) {
            int[] selected=macrosTable.getSelectedRows();
            Arrays.sort(selected); // just in case
            for (int i=0;i<selected.length;i++) {
                int index=selected[selected.length-(i+1)];
                DefaultTableModel model=(DefaultTableModel)macrosTable.getModel();
                model.removeRow(macrosTable.convertRowIndexToModel(index));
            }
            macrosTable.clearSelection();
            macrosTable.revalidate();
            macrosTable.repaint();
        } else if (evt.getSource()==removeFromOutputData) {
            int[] selected=outputDataTable.getSelectedRows();
            Arrays.sort(selected); // just in case
            for (int i=0;i<selected.length;i++) {
                int index=selected[selected.length-(i+1)];
                DefaultTableModel model=(DefaultTableModel)outputDataTable.getModel();
                model.removeRow(outputDataTable.convertRowIndexToModel(index));
            }
            outputDataTable.clearSelection();
            outputDataTable.revalidate();
            macrosTable.repaint();
        } 
    }//GEN-LAST:event_addOrRemoveFromTable

    private void outputSessionCheckboxPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputSessionCheckboxPressed
        saveSessionTextfield.setEnabled(saveSessionCheckbox.isSelected());
    }//GEN-LAST:event_outputSessionCheckboxPressed

    private void organismSelectedinCombobox(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_organismSelectedinCombobox
        String selectedOrganism=(String)organismCombobox.getSelectedItem();
        int organism=Organism.getTaxonomyID(selectedOrganism);
        String[] builds=Organism.getSupportedGenomeBuilds(organism);
        genomebuildCombobox.setModel(new DefaultComboBoxModel(builds));
    }//GEN-LAST:event_organismSelectedinCombobox

    private void newDataFormatSelected(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newDataFormatSelected
        String dataformat=(String)dataformatCombobox.getSelectedItem();
        customFormatTextfield.setEnabled(dataformat.equals("Custom"));
    }//GEN-LAST:event_newDataFormatSelected

    private void showInPlainFormatPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showInPlainFormatPressed
        Object selected=dataList.getSelectedValue();
        if (selected instanceof Data) showData((Data)selected);
    }//GEN-LAST:event_showInPlainFormatPressed

    private void noSequencesRadioButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noSequencesRadioButtonPressed
        setSequencesFileEnabled(false);
        setChromosomeInputEnabled(false);
        setInputSequencesDialogEnabled(false);
        setInputSessionEnabled(true);
        setDefaultOrganismAndGenomeBuildEnabled(false);
    }//GEN-LAST:event_noSequencesRadioButtonPressed

    private void sequencesFileRadioButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequencesFileRadioButtonPressed
        setSequencesFileEnabled(true);
        setChromosomeInputEnabled(false);
        setInputSequencesDialogEnabled(false);        
        setInputSessionEnabled(true);
        setDefaultOrganismAndGenomeBuildEnabled(true);
    }//GEN-LAST:event_sequencesFileRadioButtonPressed

    private void chromosomeRadioButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chromosomeRadioButtonPressed
        setSequencesFileEnabled(false);
        setChromosomeInputEnabled(true);
        setInputSequencesDialogEnabled(false);        
        setInputSessionEnabled(false);
        setDefaultOrganismAndGenomeBuildEnabled(true);
    }//GEN-LAST:event_chromosomeRadioButtonPressed

    private void sequencesDialogRadioButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sequencesDialogRadioButtonPressed
        setSequencesFileEnabled(false);
        setChromosomeInputEnabled(false);
        setInputSequencesDialogEnabled(true);        
        setInputSessionEnabled(true);
        setDefaultOrganismAndGenomeBuildEnabled(false);
    }//GEN-LAST:event_sequencesDialogRadioButtonPressed

    private void launchInputSequencesDialogButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launchInputSequencesDialogButtonPressed
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SequenceInputDialog dialog=new SequenceInputDialog(engine);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.disableAutoFetchDNA();
        dialog.setVisible(true);
        addSequencesFromDialogTask=dialog.getAddSequencesTask();
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());             
    }//GEN-LAST:event_launchInputSequencesDialogButtonPressed
   

    private void setSequencesFileEnabled(boolean enable) {
        sequencesFilenameTextfield.setEnabled(enable);
        sequencesBrowseButton.setEnabled(enable);
        dataformatLabel.setEnabled(enable);
        dataformatCombobox.setEnabled(enable);
        groupsizeLabel.setEnabled(enable);
        splitGroupsSpinner.setEnabled(enable);
        assignNewNamesCheckbox.setEnabled(enable);
        String dataformat=(String)dataformatCombobox.getSelectedItem();
        customFormatTextfield.setEnabled((enable)?dataformat.equals("Custom"):false);
    }
    
    private void setChromosomeInputEnabled(boolean enable) {
        wgaRegionTextfield.setEnabled(enable);
        wgaSegmentSizeLabel.setEnabled(enable);
        wgaSegmentSizeSpinner.setEnabled(enable);
        wgaCollectionSizeLabel.setEnabled(enable);
        wgaCollectionSizeSpinner.setEnabled(enable);
        wgaOverlapLabel.setEnabled(enable);
        wgaOverlapSpinner.setEnabled(enable);
    }    
    
    private void setInputSequencesDialogEnabled(boolean enable) {
        launchInputSequencesDialogButton.setEnabled(enable);
    }     
    
    private void setInputSessionEnabled(boolean enable) {
        inputSessionLabel.setEnabled(enable);
        inputSessionTextfield.setEnabled(enable);
        inputSessionBrowseButton.setEnabled(enable);
    }     
    
    private void setDefaultOrganismAndGenomeBuildEnabled(boolean enable) {
        defaultOrganismLabel.setEnabled(enable);
        defaultGenomeBuildLabel.setEnabled(enable);
        organismCombobox.setEnabled(enable);
        genomebuildCombobox.setEnabled(enable);
    }      
    
    
    private DefaultComboBoxModel getOrganismsModel() {
        String[] list=Organism.getSupportedOrganisms();
        Arrays.sort(list);
        return new DefaultComboBoxModel(list);
    }
    
    private DefaultTableModel getMacroTableModel() {
        return new DefaultTableModel(new String[]{"Macro","Definition"}, 0);
    }
    
    private DefaultTableModel getDataInjectionTableModel() {
        return new DefaultTableModel(new String[]{"Data Object","Value"}, 0);
    }    
    
    private DefaultTableModel getOutputDataTableModel() {
        return new DefaultTableModel(new String[]{"Data Object","New filename"}, 0);
    }       
    
    // Implementation of MessageListener interface method */
    @Override
    public void errorMessage(final String msg, final int errortype) {
          Runnable runnable = new Runnable() {
              @Override
              public void run() {
                       try {
                            StyledDocument log=logPanel.getStyledDocument();
                            log.insertString(log.getLength(), msg+"\n", null);
                       } catch (Exception e) {System.err.println("ERROR: Unable to write message in LogPanel: '"+msg+"'    (=> "+e.toString()+")");}
              } // end run()
          }; // end Runnable
          if (SwingUtilities.isEventDispatchThread()) runnable.run(); // invoke directly on EDT
          else {SwingUtilities.invokeLater(runnable); // queue on EDT
        }
    }

    // Implementation of MessageListener interface method */
    @Override
    public void logMessage(final String msg, int level) {
          if (level<10 && loggerLevel<4) return; // do not log "status" reports
          if (level<20 && loggerLevel<3) return; // do not log "trivial progress" reports
          if (level<30 && loggerLevel<2) return; // do not log anything other than errors and really important messages          
          Runnable runnable = new Runnable() {
             @Override
             public void run() {
                   try {
                        StyledDocument log=logPanel.getStyledDocument();
                        log.insertString(log.getLength(), msg+"\n", null);
                   } catch (Exception e) {System.err.println("ERROR: Unable to write message in LogPanel: '"+msg+"'    (=> "+e.toString()+")");}
             } // end run()
          }; // end Runnable
          if (SwingUtilities.isEventDispatchThread()) runnable.run(); // invoke directly on EDT
          else {SwingUtilities.invokeLater(runnable); // queue on EDT
        }
    }
    
    @Override
    public void logMessage(final String msg) {
        logMessage(msg, 20);
    }
    
    
    // abandoned interface method
    public void outputMessage(final String msg) {
        logMessage(msg);
    }

    // Implementation of MessageListener interface method */
    @Override
    public void statusMessage(final String msg) {
        PropertyChangeEvent event=new PropertyChangeEvent(engine, ExecutableTask.STATUS_MESSAGE, msg, msg);
        if (progressMonitor!=null) progressMonitor.propertyChange(event);
        if (loggerLevel==4) logMessage(msg, 0);
    }

    // Implementation of MessageListener interface method   
    @Override
    public void progressReportMessage(int progress) { 
        setProgress(progress);
    }     
    
    /** Sets the value of the progressbar by adding the update to the EDT queue
     *  the value should be between 0 and 100
     *  If provided value is outside this range, the progress bar will be hidden
     *  However, if the progress is Integer.MAX_INT the progress will be set to "indetermined"
     */
    public void setProgress(final int progress) {
        String property=((progress>=0 && progress<=100)||progress==Integer.MAX_VALUE)?ExecutableTask.PROGRESS:"HIDE";
        PropertyChangeEvent event=new PropertyChangeEvent(engine, property, progress, progress);
        if (progressMonitor!=null) progressMonitor.propertyChange(event);
    }
    
    /** Sets the value of the progressbar by adding the update to the EDT queue
     *  the value should be between 0 and 100
     *  If provided value is outside this range, the progress bar will be hidden
     *  However, if the progress is Integer.MAX_INT the progress will be set to "indetermined"
     */
    public void setProgress(final int[] totalprogress, int progress) {
        int i=totalprogress[0];
        int n=totalprogress[1];
        int subprogress=progress;
        double subtaskSegment=100.0/(double)n;
        if (progress>100) progress=100;
        else if (progress<0) progress=0;
        if (i>=n) subprogress=100;
        else {
            if (i<0) i=0;
            else subprogress=(int)((i*subtaskSegment)+((double)subprogress/100)*subtaskSegment);
        }        
        String property=ExecutableTask.PROGRESS; // String property=((progress>=0 && progress<=100)||progress==Integer.MAX_VALUE)?ExecutableTask.PROGRESS:"HIDE";
        PropertyChangeEvent event=new PropertyChangeEvent(engine, property, subprogress, subprogress);
        if (progressMonitor!=null) progressMonitor.propertyChange(event);
    }    
   
    
    private void setTaskProgress(int tasknumber, int total) {
        if (total<=1) taskprogress=null; // no subtasks
        else taskprogress=new int[]{tasknumber,total};
    }
    

    @Action
    public void executeProtocol() {        
        startTime=System.nanoTime();
        // Create the protocol object
	SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                setTaskProgress(0,0);   
                try {        
                    MinimalGUI.this.setProgress(Integer.MAX_VALUE); // Don't know how long it will take to restore input sessions session
                    if (isAborted) throw new InterruptedException();
                    engine.clearAllData();
                    StandardProtocol protocol=parseProtocolFromEditor();
                    if (isAborted) throw new InterruptedException();
                    currentProtocol=protocol;                    
                    if (protocol==null) throw new ParseError("The protocol is empty");                    
                                                         
                    if (sequencesRadioButton.isSelected()) { // load sequences from file before executing protocol
                        if (sequencesFilename==null) throw new ExecutionError("No sequences file selected");
                        if (splitGroupSize>0) executeProtocolOnSequenceSubset(protocol); // all other "parameters" are global
                        else { // regular protocol execution (with input sequences)
                            if (!inputSessionFilename.isEmpty()) { //
                                try {
                                    restoreSession(inputSessionFilename);
                                } catch (Exception e) {throw new ExecutionError("Input session error: "+e.getMessage(),e);}
                            }  
                            if (isAborted) throw new InterruptedException();
                            MotifLab.getSequences(sequencesFilename,sequenceFormat,defaultGenomeBuild,assignNewNames,false,engine); // reuse functionality from the CLI-client (which is called "MotifLab" because that makes more sense on the command-line)                                                        
                            protocolTask = protocol.parse();
                            if (isAborted) throw new InterruptedException();
                            engine.executeProtocolTask(protocolTask, false);
                            if (isAborted) throw new InterruptedException();                            
                            if (saveOutput) MotifLab.saveOutputData(null, outputdirname, renameOutput, engine);
                        }                                      
                    } if (sequencesDialogRadioButton.isSelected()) { // load sequences from file before executing protocol
                        if (addSequencesFromDialogTask==null || addSequencesFromDialogTask.getNumberofSequencesToAdd()==0) throw new ExecutionError("No sequences selected");
                        if (!inputSessionFilename.isEmpty()) { //
                            try {
                                restoreSession(inputSessionFilename);
                            } catch (Exception e) {throw new ExecutionError("Input session error: "+e.getMessage(),e);}
                        }  
                        if (isAborted) throw new InterruptedException();
                        engine.executeTask(addSequencesFromDialogTask);                                                       
                        protocolTask = protocol.parse();
                        if (isAborted) throw new InterruptedException();
                        engine.executeProtocolTask(protocolTask, false);
                        if (isAborted) throw new InterruptedException();                            
                        if (saveOutput) MotifLab.saveOutputData(null, outputdirname, renameOutput, engine);                                                              
                    } else if (wgaRadioButton.isSelected()) {
                        String build=(String)genomebuildCombobox.getSelectedItem();
                        String region=wgaRegionTextfield.getText().trim();
                        if (region.contains(",")) throw new ParseError("Chromosome region specification should not contain commas!"); // just to avoid confusing 
                        int segmentSize=(Integer)wgaSegmentSizeSpinner.getValue();
                        int collectionSize=(Integer)wgaCollectionSizeSpinner.getValue();
                        int overlap=(Integer)wgaOverlapSpinner.getValue();
                        String specification=build+","+region+","+segmentSize+","+collectionSize+","+overlap;
                        performWholeGenomeAnalysis(specification, protocol);
                    } else {
                        // do not load any sequences. Either the protocol contains the sequences itself or no sequences need to be loaded
                        if (!inputSessionFilename.isEmpty()) { //
                            try {
                                restoreSession(inputSessionFilename);
                            } catch (Exception e) {throw new ExecutionError("Input session error: "+e.getMessage(),e);}
                        }                        
                        if (isAborted) throw new InterruptedException();                        
                        protocolTask = protocol.parse();
                        if (isAborted) throw new InterruptedException();
                        engine.executeProtocolTask(protocolTask, false); 
                        if (isAborted) throw new InterruptedException();                        
                        if (saveOutput) MotifLab.saveOutputData(null, outputdirname, renameOutput, engine);
                    }
                    if (isAborted) throw new InterruptedException();                    
                    // save session        
                    if (saveSessionCheckbox.isSelected() && !saveSessionFilename.isEmpty()) {
                         MinimalGUI.this.statusMessage("Saving session");
                         MinimalGUI.this.setProgress(99);                         
                         saveSession(saveSessionFilename);
                    }                                           
                    MinimalGUI.this.statusMessage("Done!");
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                } 
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
               protocolTask=null; // 
               MinimalGUI.this.setProgress(101); // hide progress bar
               String timeMessage=(ex==null)?"Protocol execution completed in: ":"Protocol execution halted after: ";
               if (ex instanceof InterruptedException) {
                   timeMessage="Protocol execution aborted after: ";
               } else if (ex instanceof ParseError) {
                   JOptionPane.showMessageDialog(getFrame(), ex.getMessage(),"Parse Error", JOptionPane.ERROR_MESSAGE);
               } else if (ex instanceof  ExecutionError) {
                  JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), "Execution Error", JOptionPane.ERROR_MESSAGE);  
               } else if (ex instanceof  Exception) {
                  JOptionPane.showMessageDialog(getFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);                   
                  ex.printStackTrace(System.err);
               } else { // execution was successful
                   MGtabPanel.setSelectedComponent(resultsTab);                   
               }
               long nanoseconds=System.nanoTime()-startTime;
               long seconds=nanoseconds/1000000000; // total seconds (not seconds passed the minute)
               long minutes=seconds/60; // total minutes (not minutes passed the hour)
               long hours=minutes/60;
               logMessage("");
               String timestamp="";
               if (hours>0) {
                   minutes=minutes%60; // minutes passed hour 
                   seconds=seconds%60; // seconds passed minute 
                   timestamp=hours+"h "+minutes+"m "+seconds+"s";
               } else if (minutes>0) {
                   seconds=seconds%60; // seconds passed minute                   
                   timestamp=minutes+"min "+seconds+"sec";
               }
               else timestamp=seconds+" seconds";
               logMessage(timeMessage+timestamp);               
               //if (ex!=null) ex.printStackTrace(System.err);
               executeButton.setEnabled(true);
               stopButton.setEnabled(false);
            }
        }; // end of SwingWorker class
        
        executeButton.setEnabled(false); 
        isAborted=false;        
        clearResults(); // remove current result output
        clearLogPanel();
        updateMacros();       
        outputdirname=outputDirectoryTextfield.getText().trim();
        if (outputdirname.isEmpty()) outputdirname=null;
        saveSessionFilename=saveSessionTextfield.getText().trim();        
        inputSessionFilename=inputSessionTextfield.getText().trim();
        sequencesFilename=sequencesFilenameTextfield.getText().trim();
        if (sequencesFilename.isEmpty()) sequencesFilename=null;   
        saveOutput=saveOutputCheckbox.isSelected();
        assignNewNames=assignNewNamesCheckbox.isSelected();
        splitGroupSize=(Integer)splitGroupsSpinner.getValue();
        defaultGenomeBuild=(String)genomebuildCombobox.getSelectedItem();;
        if (defaultGenomeBuild.isEmpty()) defaultGenomeBuild=null;
        sequenceFormat=(String)dataformatCombobox.getSelectedItem();
             if (sequenceFormat.equals("Auto")) sequenceFormat=null;
        else if (sequenceFormat.equals("BED")) sequenceFormat="BED"; // NB: I have removed the BED option from the "Data Format" menu in the GUI because there are some problems with the BED format import function here (coordinates are off)
        else if (sequenceFormat.equals("Gene ID")) sequenceFormat="geneID";
        else if (sequenceFormat.equals("Manual 4")) sequenceFormat="manual4";
        else if (sequenceFormat.equals("Manual 8")) sequenceFormat="manual8";
        else if (sequenceFormat.equals("Manual 10")) sequenceFormat="manual10";
        else if (sequenceFormat.equals("Custom")) {
            String customFormat=customFormatTextfield.getText();
            if (customFormat.isEmpty()) sequenceFormat=null; else sequenceFormat=customFormat;
        } else sequenceFormat=null; // i.e. "auto detect"
        renameOutput.clear();
        for (int i=0;i<outputDataTable.getRowCount();i++) {
             String key=(String)outputDataTable.getValueAt(i, 0);
             String value=(String)outputDataTable.getValueAt(i, 1); 
             if (key!=null && value!=null && !(key.trim().isEmpty() || value.trim().isEmpty())) {
                 renameOutput.put(key.trim(), value.trim());
             }
        }
        MGtabPanel.setSelectedComponent(logTab); 
        stopButton.setEnabled(true);     
        worker.execute();        
    }
    
    /** Returns a string on the format "hhh:mm:ss" which tells the time between the current time and the globally set startTime */
    private String getElapsedTime() {
       long nanoseconds=System.nanoTime()-startTime;
       long seconds=nanoseconds/1000000000; // total seconds (not seconds passed the minute)
       long minutes=seconds/60; // total minutes (not minutes passed the hour)
       long hours=minutes/60;
       
       minutes=minutes%60; // minutes passed hour 
       seconds=seconds%60; // seconds passed minute        
       return ((hours>9)?hours:("0"+hours))+":"+((minutes>9)?minutes:("0"+minutes))+":"+((seconds>9)?seconds:("0"+seconds));
    }
    
    @Action
    public void stopAction() {
        stopButton.setEnabled(false);
        isAborted=true;
        ExecutableTask task=protocolTask; // copy the reference to make sure it doesn't change
        if (task!=null) task.setStatus(ExecutableTask.ABORTED); // signal abortion        
    }
    
    
    private void updateMacros() {
        HashMap<String, String> macros=new HashMap<String, String>();
        for (int i=0;i<macrosTable.getRowCount();i++) {
            String key=(String)macrosTable.getValueAt(i, 0);
            String value=(String)macrosTable.getValueAt(i, 1);
            if (key!=null && !key.trim().isEmpty()) {
                if (value==null) value="";
                macros.put(key, value);
            }
        }
        engine.setMacros(macros);
    }
        
    private void executeProtocolOnSequenceSubset(StandardProtocol protocol) throws Exception {
        int groupIndex=0;
        int startIndex=0;
             
        boolean finished=false;
        while (!finished) {  
            if (isAborted) throw new InterruptedException();
            groupIndex++;
            int endIndex=startIndex+splitGroupSize-1; // 
            if (inputSessionFilename!=null && !inputSessionFilename.isEmpty()) { // reload input session for every group
                try {
                    restoreSession(inputSessionFilename);
                } catch (Exception e) {throw new ExecutionError("Input session error: "+e.getMessage(),e);}
            }  
            if (isAborted) throw new InterruptedException();            
            int size=MotifLab.getSequences(sequencesFilename, startIndex, endIndex, sequenceFormat, defaultGenomeBuild, false, false, engine);
            if (size==0) break; // no more sequences
            if (isAborted) throw new InterruptedException();            
            protocolTask = protocol.parse();
            logMessage("-----------   Analyzing sequence group "+groupIndex+" (Sequences "+(startIndex+1)+" to "+(startIndex+size)+")   ----------- ["+getElapsedTime()+"]");                
            protocolTask.setStatus(ExecutableTask.WAITING);
            engine.executeProtocolTask(protocolTask,false); // parses and executes the protocol
            if (isAborted) throw new InterruptedException();             
            // now save all "OutputData" objects to files           
            if (saveOutput) MotifLab.saveOutputData("_"+groupIndex, outputdirname, renameOutput, engine);
            if (isAborted) throw new InterruptedException();            
            engine.clearAllData();
            if (isAborted) throw new InterruptedException();            
            if (size<splitGroupSize) finished=true; // this is the last group
            startIndex+=splitGroupSize;
        }        
    }
    
    /* This whole method (except for some minor additions and changes) has been copied verbatim from the CLI-client in MotifLab.class */
    private void performWholeGenomeAnalysis(String specifications, StandardProtocol protocol) throws Exception {
        String[] parts=specifications.split(",");
        String build="";
        int start=1;
        int end=1;
        int segmentSize=10000;
        int overlap=0;
        boolean extendToOverlap=(overlap>0); // means that the segment size should be extended by the overlap size (so that the start of each segment is on the form: start+k*(segment size))
        int sequenceCollectionSize=100;
        int organism=Organism.UNKNOWN;
        String chromosome="";
        String startString="1";
        String endString="";
        int numberOfSegments=0;
        if (parts.length>0) {
            build=parts[0];
            GeneIDResolver idResolver=engine.getGeneIDResolver();
            if (!idResolver.isGenomeBuildSupported(build)) throw new ExecutionError("Unrecognized genome build: "+build);
            organism=Organism.getOrganismForGenomeBuild(build);
            if (organism==Organism.UNKNOWN) throw new ExecutionError("Unable to determine organism based on genome build");
        }
        if (parts.length>=2) {
            String regionString=parts[1];
            if (regionString.indexOf(':')<=0) throw new ExecutionError("The genomic region to analyse should be specified in the format 'chr:[start-]end'.");
            String[] regParts=regionString.split(":");
            chromosome=regParts[0].trim();
            if (chromosome.isEmpty()) throw new ExecutionError("Missing specification of chromosome for genomic region to analyse");
            if (chromosome.startsWith("chr")) chromosome=chromosome.substring("chr".length());
            if (regParts[1].indexOf('-')>=0) {
                String[] tmp=regParts[1].split("-");
                startString=tmp[0].trim();
                endString=tmp[1].trim();
             } else endString=regParts[1];
             if (startString.isEmpty()) throw new ExecutionError("Missing start coordinate of genomic region to analyse");
             if (endString.isEmpty()) throw new ExecutionError("Missing end coordinate of genomic region to analyse");
             try {
                start=Integer.parseInt(startString);
             } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse expected integer number for start coordinate of genomic region: "+startString);}
             try {
                end=Integer.parseInt(endString);
             } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse expected integer number for end coordinate of genomic region: "+endString);}           
        } else throw new ExecutionError("Missing specification of genomic region to analyse");
        if (parts.length>=3) {
             try {
                segmentSize=Integer.parseInt(parts[2]);
             } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse expected integer number for segment size: "+parts[2]);}           
        }  
        if (parts.length>=4) {
             try {
                sequenceCollectionSize=Integer.parseInt(parts[3]);
             } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse number of segments to analyse at a time: "+parts[3]);}           
        }         
        if (parts.length>=5) {
             try {
                overlap=Integer.parseInt(parts[4]);
                extendToOverlap=(overlap>0);
                overlap=Math.abs(overlap);                
             } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse expected integer number for segment overlap: "+parts[4]);}           
        }   
        if (start>end) throw new ExecutionError("The start coordinate of the genomic region ("+start+") must be greater than the end coordinate ("+end+")");
        if (overlap>segmentSize || (overlap==segmentSize && !extendToOverlap)) throw new ExecutionError("The segment overlap ("+overlap+") can not be larger than the segment size ("+segmentSize+")");
        if (isAborted) throw new InterruptedException();
        int totalSegmentLength=end-start+1;
        int seqLength=segmentSize;
        if (extendToOverlap) {
            numberOfSegments=(int)Math.ceil((double)totalSegmentLength/(double)segmentSize);
            seqLength+=overlap;
        } else {
            int position=start;
            while (position<=end) {
               numberOfSegments++;
               position+=segmentSize-overlap;
            }         
        }        
        int numberOfCollections=(int)Math.ceil((double)numberOfSegments/(double)sequenceCollectionSize);
        int segmentsToGo=numberOfSegments;
        int position=start;
        int sequenceIndex=0;
        //System.out.println("Number of segments: "+numberOfSegments+", collections="+numberOfCollections);
        for (int i=1;i<=numberOfCollections;i++) {
            setTaskProgress(i-1, numberOfCollections);
            if (isAborted) throw new InterruptedException();            
            int segmentsInThisCollection=(segmentsToGo>=sequenceCollectionSize)?sequenceCollectionSize:segmentsToGo;            
            logMessage("-----------   Analyzing sequence group "+i+" of "+numberOfCollections+"   ("+segmentsInThisCollection+" sequences @ "+seqLength+" bp)   -------- ["+getElapsedTime()+"]");
            // setup the sequence collection
            Sequence[] sequences=new Sequence[segmentsInThisCollection];
            for (int j=0;j<segmentsInThisCollection;j++) {
               sequenceIndex++;
               String sequenceName="seq"+sequenceIndex;
               int sequenceStart=position;
               int sequenceEnd=sequenceStart+segmentSize-1;
               if (extendToOverlap) {
                   sequenceEnd+=overlap;
                   position+=segmentSize;
               } else {
                   position+=(segmentSize-overlap); 
               }
               if (sequenceEnd>end) sequenceEnd=end;
               sequences[j]=new Sequence(sequenceName, organism, build, chromosome, sequenceStart, sequenceEnd, sequenceName, null, null, Sequence.DIRECT); 
               // logMessage("   ["+(j+1)+"] "+sequences[j].getValueAsParameterString());               
            }
            if (isAborted) throw new InterruptedException();
            // register sequences with the engine
            AddSequencesTask addSequencesTask=new AddSequencesTask(engine, sequences);
            addSequencesTask.setMotifLabClient(this);  
            addSequencesTask.addPropertyChangeListener(this);
            addSequencesTask.run();

            if (isAborted) throw new InterruptedException();
            // run the protocol and save the results to file
            protocolTask = protocol.parse();
            engine.executeProtocolTask(protocolTask, true);
            if (isAborted) throw new InterruptedException();
            MotifLab.saveOutputData("_"+i, outputdirname, renameOutput, engine);
            if (isAborted) throw new InterruptedException();
            // prepare for next run
            engine.clearAllData();
            segmentsToGo-=segmentsInThisCollection;
        } 
         
    }    
    
    private class EditOnFocusTable extends JTable {
        
        // This method override will activate editing mode immediately when a cell receives focus (rather than having to click the cell or start typing)
        // (This is necessary to work flawlessly with the custom editor in the "value" column for the data injections table)
        @Override
        public void changeSelection(final int row, final int column, boolean toggle, boolean extend) {
            super.changeSelection(row, column, toggle, extend);
            this.editCellAt(row, column);
            this.transferFocus();
        }
}
    
    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel MGbuttonsPanel;
    private javax.swing.JPanel MGmainPanel;
    private javax.swing.JPanel MGstatusPanel;
    private javax.swing.JTabbedPane MGtabPanel;
    private javax.swing.JPanel MGtopPanel;
    private javax.swing.JButton addToDataInjections;
    private javax.swing.JButton addToMacros;
    private javax.swing.JButton addToOutputData;
    private javax.swing.JCheckBox assignNewNamesCheckbox;
    private javax.swing.JButton clearLogButton;
    private javax.swing.JPanel controlsPanel;
    private javax.swing.JTextField customFormatTextfield;
    private javax.swing.JTable dataInjectionsTable;
    private javax.swing.JList dataList;
    private javax.swing.JPanel dataListPanel;
    private javax.swing.JComboBox dataformatCombobox;
    private javax.swing.JLabel dataformatLabel;
    private javax.swing.JLabel defaultGenomeBuildLabel;
    private javax.swing.JLabel defaultOrganismLabel;
    private javax.swing.JButton executeButton;
    private javax.swing.JPanel exitButtonPanel;
    private javax.swing.JComboBox genomebuildCombobox;
    private javax.swing.JLabel groupsizeLabel;
    private javax.swing.JPanel inputBottomPanel;
    private javax.swing.ButtonGroup inputButtonGroup;
    private javax.swing.JPanel inputSequencePanel;
    private javax.swing.JButton inputSessionBrowseButton;
    private javax.swing.JLabel inputSessionLabel;
    private javax.swing.JTextField inputSessionTextfield;
    private javax.swing.JPanel inputTab;
    private javax.swing.JPanel inputTopPanel;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JPanel jpanelx2;
    private javax.swing.JButton launchInputSequencesDialogButton;
    private javax.swing.JTextPane logPanel;
    private javax.swing.JPanel logTab;
    private javax.swing.JPanel macrosPanel;
    private javax.swing.JTable macrosTable;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JRadioButton noSequencesRadioButton;
    private javax.swing.JComboBox organismCombobox;
    private javax.swing.JTable outputDataTable;
    private javax.swing.JPanel outputDataTablePanel;
    private javax.swing.JTextField outputDirectoryTextfield;
    private javax.swing.JPanel outputTab;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel promptInjectionPanel;
    private javax.swing.JButton protocolBrowse;
    private javax.swing.JTextPane protocolEditorPane;
    private javax.swing.JTextField protocolFilenameTextfield;
    private javax.swing.JPanel protocolTab;
    private javax.swing.JButton removeFromDataInjections;
    private javax.swing.JButton removeFromMacros;
    private javax.swing.JButton removeFromOutputData;
    private javax.swing.JPanel resultsPanel;
    private javax.swing.JPanel resultsPanelOuter;
    private javax.swing.JPanel resultsTab;
    private javax.swing.JCheckBox saveOutputCheckbox;
    private javax.swing.JCheckBox saveSessionCheckbox;
    private javax.swing.JTextField saveSessionTextfield;
    private javax.swing.JButton saveShownDataButton;
    private javax.swing.JButton sequencesBrowseButton;
    private javax.swing.JRadioButton sequencesDialogRadioButton;
    private javax.swing.JTextField sequencesFilenameTextfield;
    private javax.swing.JRadioButton sequencesRadioButton;
    private javax.swing.JCheckBox showInPlainFormatCheckbox;
    private javax.swing.JLabel shownDataObjectNameLabel;
    private javax.swing.JSpinner splitGroupsSpinner;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JButton stopButton;
    private javax.swing.JLabel wgaCollectionSizeLabel;
    private javax.swing.JSpinner wgaCollectionSizeSpinner;
    private javax.swing.JPanel wgaInnerPanel;
    private javax.swing.JLabel wgaOverlapLabel;
    private javax.swing.JSpinner wgaOverlapSpinner;
    private javax.swing.JRadioButton wgaRadioButton;
    private javax.swing.JTextField wgaRegionTextfield;
    private javax.swing.JLabel wgaSegmentSizeLabel;
    private javax.swing.JSpinner wgaSegmentSizeSpinner;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    
    private void clearLogPanel() {
        logPanel.setText(""); //        
    }
    
    private void clearResults() {
        resultsPanel.removeAll();
        shownDataObjectNameLabel.setText("No data objects shown");
        resultsPanelOuter.revalidate();
        resultsPanelOuter.repaint();
        saveShownDataButton.setEnabled(false);        
    }
    
    
  private void mousePressedInDataObjectsPanel(java.awt.event.MouseEvent evt) {                                             
    int x=evt.getX(); 
    int y=evt.getY();
    JList list=(JList)evt.getComponent();    
    int index=getElementIndexAtPointInList(list,x,y);
    if (index==-1) {list.clearSelection();return;} 
    if (evt.getButton()==java.awt.event.MouseEvent.BUTTON1) {
         //gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         javax.swing.DefaultListModel model=(javax.swing.DefaultListModel)list.getModel();
         Data selectedData=(Data)model.getElementAt(index);
         showData(selectedData);
    }
  }  
  
   private int getElementIndexAtPointInList(JList list, int x, int y) {
     int index=list.locationToIndex(new java.awt.Point(x,y));
     if (index>=0) {
        javax.swing.DefaultListModel model=(javax.swing.DefaultListModel)list.getModel();
        java.awt.Rectangle bounds = list.getCellBounds(0, model.getSize()-1);
        if (y>bounds.getHeight() || y<0 || x>bounds.getWidth() || x<0) index=-1; // 
     }
     return index;
   }  
    
   private SwingWorker previousWorker=null;
   private final Boolean lock=Boolean.TRUE;
   
   private void showData(final Data data) {
        final boolean showPlain=showInPlainFormatCheckbox.isSelected();
	SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            OutputPanel outputpanel=null;
            String formatName="Plain";
            @Override
            public Boolean doInBackground() {
                try {                    
                   if (data instanceof OutputData) {
                        outputpanel=new OutputPanel((OutputData)data, null);
                        formatName=((OutputData)data).getDataFormat();
                   } else {          
                       DataFormat format=(showPlain)?engine.getDataFormat("Plain"):engine.getDefaultDataFormat(data);
                       if (data instanceof Sequence && !showPlain) format=engine.getDataFormat("Location");
                       OutputData output=new OutputData("output");
                       if (format!=null) {
                           try {
                               formatName=format.getName();
                               format.format(data, output, null, null);
                           } catch (Exception e) {
                               output.append("ERROR: Unable to display data object", null);
                           }
                       } else output.append(data.getValueAsParameterString(),null);
                       outputpanel=new OutputPanel(output, null);                       
                   }
                } catch (Exception e) {
                    ex=e;
                    OutputData output=new OutputData("output");
                    output.append("ERROR: Unable to display data object\n\n"+e.getClass().getSimpleName()+" : "+e.getMessage(), null);
                    outputpanel=new OutputPanel(output, null); 
                    return Boolean.FALSE;                 
                } 
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
               if (isCancelled()) return;
               if (outputpanel!=null) {
                   resultsPanel.add(outputpanel);
                   shownDataObjectNameLabel.setText("<html>Showing data object: <b>"+data.getName()+"</b>&nbsp;&nbsp;&nbsp;&nbsp;(format = "+formatName+")</html>");
                   saveShownDataButton.setEnabled(true); 
               }
               MinimalGUI.this.statusMessage("");
               MinimalGUI.this.setProgress(101);               
               resultsPanelOuter.revalidate();
               resultsPanelOuter.repaint();               
            }
     }; // end of SwingWorker class       
       resultsPanel.removeAll();
       saveShownDataButton.setEnabled(false); 
       MinimalGUI.this.statusMessage("Formatting output. Please wait...");
       MinimalGUI.this.setProgress(Integer.MAX_VALUE); // Indetermined 
       synchronized (lock) {
           if (previousWorker!=null) previousWorker.cancel(true); // if another worker is running, cancel it so the results will not be shown
           previousWorker=worker;
       }
       worker.execute();
   }
   
    /**
     * This method is used by Operation_prompt
     * @param item
     * @param message
     * @param editable
     * @return
     * @throws ExecutionError 
     */
    public Data promptValue(final Data item, final String message, final PromptConstraints constraints) throws ExecutionError {
        // this is called from the protocol (OFF the EDT!). This method can not return until the Data object is valid
        HashMap<String,String> promptInjections=getPromptInjections();
        if (promptInjections!=null && !promptInjections.isEmpty()) {
            // use proxy CLI-client to retrieve injected value (either from file or parsed directly)
            MotifLab proxy = new MotifLab(engine,null,false);
            proxy.setPromptInjections(promptInjections);
            Data value=proxy.promptValue(item.clone(), message, constraints); // the cloning is necessary (not always, but sometimes)
            return value;
        }
        // If no prompt-injections are provided, prompt interactively with a proxy GUI (reusing all the regular GUI prompts)      
        final MotifLabGUI gui=new MotifLabGUI(thisApp,engine,getVisualizationSettings(),getFrame(),messageTimer,busyIconTimer);     
        final ExecutionError[] exception=new ExecutionError[1];
        final Data[] buffer=new Data[]{item};         
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    buffer[0]=gui.promptValue(item, message,constraints);
                }
                catch (ExecutionError e) {
                    exception[0]=e;
                }                                 
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else try {
            SwingUtilities.invokeAndWait(runner); // this will block until the runner is finished!
        } catch (Exception ie) {engine.logMessage("Warning: "+ie.getMessage());}
        if (exception[0]!=null) throw exception[0];
        return buffer[0];
    }

    private HashMap<String,String> getPromptInjections() {
        HashMap<String, String> injections=new HashMap<String, String>();
        for (int i=0;i<dataInjectionsTable.getRowCount();i++) {
            String key=(String)dataInjectionsTable.getValueAt(i, 0);
            String value=(String)dataInjectionsTable.getValueAt(i, 1);
            if (key!=null && !key.trim().isEmpty()) {
                if (value==null) value="";
                injections.put(key, value);
            }
        }    
        return injections;
    }
    
  /**
   * This class listens to PropertyChanges and updates progress-bar and status messages
   * in the status field accordingly. It is used as a replacement for TaskMonitor which
   * didn't quite have the functionality I was looking for
   * The class uses references to widgets defined as fields in the outer class
   */
  private class ProgressMonitor implements PropertyChangeListener {

      @Override
      public void propertyChange(java.beans.PropertyChangeEvent evt) {
          ProgressRunner runner=new ProgressRunner(evt);
          SwingUtilities.invokeLater(runner); // this will queue the propertyChanges correctly so that they are executed in order even if some calls to this propertyChange() method are from the EDT and some are not. (Do not check "isEDT" and run directly if TRUE!)
      }

      private class ProgressRunner implements Runnable {
          private PropertyChangeEvent evt=null;
          public ProgressRunner(PropertyChangeEvent event) {
              evt=event;
          }
          @Override
          public void run() {
                String propertyName = evt.getPropertyName();
                if (propertyName.equals(ExecutableTask.STATUS)) {
                   String newstatus=(String)evt.getNewValue();
                   if (newstatus.equals(ExecutableTask.RUNNING)) {
                       if (!busyIconTimer.isRunning()) {
                          statusAnimationLabel.setIcon(busyIcons[0]);
                          busyIconIndex = 0;
                          busyIconTimer.start();
                       }
                       progressBar.setVisible(true);
                  } else if (newstatus.equals(ExecutableTask.WAITING) || newstatus.equals(ExecutableTask.PENDING)) {
                       progressBar.setVisible(true);
                       progressBar.setIndeterminate(true);
                  } else {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                    progressBar.setString("");
                  }
                } else if (propertyName.equals(ExecutableTask.STATUS_MESSAGE)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if (propertyName.equals(ExecutableTask.PROGRESS)) { // this is used for "direct" setting of progress by GUI-initiated functions, not feedback from scheduled tasks
                    if (!busyIconTimer.isRunning()) {
                      statusAnimationLabel.setIcon(busyIcons[0]);
                      busyIconIndex = 0;
                      busyIconTimer.start();
                    }
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(value==Integer.MAX_VALUE);
                    if (value!=Integer.MAX_VALUE) progressBar.setValue(value);
                    //int thisNumber=scheduler.getCurrentTaskQueueNumber();
                    //int totalNumber=scheduler.getLastQueueNumber();
                    progressBar.setString("");
                } else if (propertyName.equals(ExecutableTask.SCHEDULING_EVENT)) { // a task has been scheduled for execution or aborted
                    progressBar.setString("");
                } else if (propertyName.equals(ProtocolTask.STARTED_EXECUTION)) {
                    String protocolName = (String)(evt.getOldValue());
                } else if (propertyName.equals("HIDE")) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                    progressBar.setString("");
                }
        }
      } // of class ProgressRunner
  }  // end of class ProgressMonitor

    

    @Override 
    public void saveSession(String filename) throws Exception {
        File file=engine.getFile((outputdirname==null || outputdirname.isEmpty())?null:outputdirname,filename);
        ObjectOutputStream outputStream = null;
        try {
            OutputStream os=MotifLabEngine.getOutputStreamForFile(file);
            outputStream = new ObjectOutputStream(new BufferedOutputStream(os));                      
            HashMap<String,Object> info=new HashMap<String, Object>();
            VisualizationSettings vizSettings=getVisualizationSettings();           
            ArrayList<String> tabs=new ArrayList<String>();
            tabs.add("Visualization");
            tabs.add("Protocol");
            ArrayList<Data> outputData=engine.getAllDataItemsOfType(OutputData.class);
            for (Data output:outputData) tabs.add(output.getName());
            String[] tabNames=new String[tabs.size()];
            tabNames=tabs.toArray(tabNames);         
            info.put("data", engine.getAllDataItemsOfType(Data.class));
            info.put("visualizationsettings", vizSettings);
            info.put("protocols",new SerializedStandardProtocol(currentProtocol));
            info.put("tabnames",tabNames);
            info.put("selectedtab","Visualization");
            engine.saveSession(outputStream, info, null);
            logMessage("Session saved to \""+file.getAbsolutePath()+"\"");
        } catch (Exception e) {
            throw e;
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (Exception x) {}
        }
    }
    
    @Override 
    public void restoreSession(String filename) throws Exception { 
        logMessage("Restoring input session: "+filename);
        Object input=null;
        if (filename.startsWith("http://") || filename.startsWith("https://") || filename.startsWith("ftp://") || filename.startsWith("ftps://")) {
            input=new URL(filename); 
        } else {
            input=engine.getFile(filename);            
        }         
        ObjectInputStream inputStream = null;
        ArrayList<Data> datalist=null;
        VisualizationSettings settings=null;
        Object restoredProtocols=null; // this can be a SerializedProtocolManager or StandardProtocol
        SequenceCollection defaultCollectionCopy=null;
        String[] tabNames;
        String selectedTabName=null;
        String[] requirements=null;     
        try {
            InputStream is=MotifLabEngine.getInputStreamForDataSource(input);
            HashMap<String,Object> restored=engine.restoreSession(is, null);
            requirements=(String[])restored.get("requirements");            
            if (restored.containsKey("exception")) throw (Exception)restored.get("exception");    
            datalist=(ArrayList<Data>)restored.get("data");
            settings=(VisualizationSettings)restored.get("visualizationsettings");
            restoredProtocols=restored.get("protocols");
            tabNames=(String[])restored.get("tabnames");
            selectedTabName=(String)restored.get("selectedtab");
            defaultCollectionCopy=(SequenceCollection)restored.get("defaultsequencecollection");
            
                 if (datalist==null) throw new ExecutionError("Unable to recover data from session file");
            else if (settings==null) throw new ExecutionError("Unable to recover display settings from file");
            else if (restoredProtocols==null) throw new ExecutionError("Unable to recover protocol information from file");
            else if (tabNames==null) throw new ExecutionError("Unable to recover correct output tab names from file");
            else if (selectedTabName==null) throw new ExecutionError("Unable to recover selectedTabName file");
            else { // restore went OK
                engine.clearAllData();
                getVisualizationSettings().importSettings(settings);            
                for (Data element:datalist) { // restore all sequences and motifs first, since these are referred to by collections!           
                    if (element instanceof Sequence || element instanceof Motif || element instanceof Module) {
                        engine.storeDataItem(element);
                    }
                }
                for (String outputDataName:tabNames){ // restore output data objects (same as tabnames)
                    Data element=getDataByNameFromList(datalist,outputDataName);
                    if (element==null) continue;              
                    engine.storeDataItem(element); // this will show the tab
                }
                for (Data element:datalist) {
                    if (!(element instanceof Sequence || element instanceof OutputData || element instanceof Motif || element instanceof Module)) engine.storeDataItem(element);
                }
                SequenceCollection col=engine.getDefaultSequenceCollection();
                col.setSequenceOrder(defaultCollectionCopy.getAllSequenceNames());   
                // Any protocols saved with the session can safely be ignored!                
            }
            inputStream.close();
        } catch (Exception e) {
            //e.printStackTrace(System.err);
            // if (e instanceof StreamCorruptedException) throw new ExecutionError("The session file requires a different version of MotifLab (probably newer) or some required plugins might be missing");
            if (e instanceof InvalidClassException || e instanceof ClassCastException) throw new ExecutionError("The saved session is not compatible with this version of MotifLab",e);          
            if (e instanceof ClassNotFoundException || e instanceof StreamCorruptedException) {
                if (e.getMessage().equals("Newer version")) throw new ClassNotFoundException("The saved session requires a newer version of MotifLab",e);
                StringBuilder builder=new StringBuilder();
                builder.append("The saved session is not compatible with the current version/setup of MotifLab. ");
                if (requirements!=null && requirements.length>0) {
                    ArrayList<String> missingPlugins=new ArrayList<String>();
                    ArrayList<String> additionRequirements=new ArrayList<String>();
                    for (String req:requirements) {
                        if (req.startsWith("Plugin:")) {
                            String pluginName=req.substring("Plugin:".length()).trim();
                            if (engine.getPlugin(pluginName)==null) missingPlugins.add(pluginName);                                    
                        } 
                        else additionRequirements.add(req);
                    }      
                    if (!missingPlugins.isEmpty()) {
                        builder.append("Missing required plugins: ");
                        builder.append(MotifLabEngine.splice(missingPlugins, ","));
                        builder.append(". ");
                    }
                    if (!additionRequirements.isEmpty()) {
                        builder.append("Additional requirements: ");
                        builder.append(MotifLabEngine.splice(additionRequirements, ","));
                        builder.append(". ");
                    }                                                       
                }
                throw new ClassNotFoundException(builder.toString(),e);
            }            
            else throw e;
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (Exception x) {}
        }      
    }
    
    private Data getDataByNameFromList(ArrayList<Data>list, String name) {
        for (Data element:list) {if (element.getName().equals(name)) return element;}
        return null;
    }       

    /** Returns the directory last used to load or save files. This can be used to
     *  initialize new FileChoosers with the same location
     */
    public File getLastUsedDirectory() {
        return lastUsedDirectory;
    }

    /** Sets the directory last used to load or save files. This directory can be used to
     *  initialize new FileChoosers with the same location
     */
    public void setLastUsedDirectory(File dir) {
        if (dir==null) return; // ignore
        if (!(dir instanceof DataRepositoryFile)) { // not currently a repository file, but check if it could be (i.e. the path matches repository file)
            DataRepositoryFile repfile=DataRepositoryFile.getRepositoryFileFromString(dir.getPath(), engine);
            if (repfile!=null) dir=repfile;
        }
        lastUsedDirectory=dir;
    }





     @Override
     public void initializeClient(MotifLabEngine engine) {
         // empty 
     }
     
     private void initializePluginsFromClient() {
        // initialize plugins from client
        for (Plugin plugin:engine.getPlugins()) {
            try {
                plugin.initializePluginFromClient(this);
            } catch (Exception e) {
                logMessage("Unable to initialize plugin \""+plugin.getPluginName()+"\" from client: "+e.getMessage());
            }
        }
     }     

    /** The next two methods implement parts of the MotifLabClient interface */
      final boolean[] answer=new boolean[]{false,false}; // just a quick cheat to get around "final" :-)
      @Override
      public boolean shouldRetry(final ExecutableTask t, final Exception e) {            
            // answer[0]=true if should retry. If answer[0]==false then answer[1] should be set to true if rollback should be performed
            int linenumber=t.getLineNumber();
            String exceptionMessage=e.getMessage();
            if (e instanceof ExecutionError || e instanceof ParseError || e instanceof SystemError || e instanceof java.util.concurrent.ExecutionException) {
                if (exceptionMessage==null || exceptionMessage.isEmpty()) exceptionMessage=e.getClass().getSimpleName();
            } else {               
                exceptionMessage=e.getClass().getSimpleName()+":"+e.getMessage();
            }
            final String msg=(linenumber==0)?exceptionMessage:("An error occurred during execution in line "+linenumber+":\n"+exceptionMessage);
            if (1<2) { // Remove this block to prompt the user interactively (I don't bother)
                logMessage(msg);
                answer[0]=false;answer[1]=true; // Abort and rollback
                return false;
            }            
            Runnable runner = new Runnable() {
            @Override
                public void run() {
                    int n=showAbortRetryDialog(MinimalGUI.this,msg);
                    if (n==0) {answer[0]=true;answer[1]=false;}
                    else if (n==1) {answer[0]=false;answer[1]=false;}
                    else if (n==2) {answer[0]=false;answer[1]=true;}
                }
            };
            if (!(e instanceof ExecutionError)) { // ExecutionErrors are caused by the user. Other errors are caused by sloppy programming on my part
              logMessage("An error occurred: "+e.getClass().toString()+" => "+e.getMessage());
              //e.printStackTrace(System.err);
            }
            if (SwingUtilities.isEventDispatchThread()) runner.run();
            else try {SwingUtilities.invokeAndWait(runner);} catch (Exception ex) {return false;}
            return answer[0];
     }

     @Override
     public boolean shouldRollback() {
        return answer[1];
     }

    public static int showAbortRetryDialog(MinimalGUI gui, String message) {
        Object[] options={"Retry","Abort","Abort and Rollback"};
        final int[] countdowntimer=new int[]{31}; // 30 seconds
        final JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null,options, options[0]);
        pane.setComponentOrientation(gui.getRootPane().getComponentOrientation());
        pane.selectInitialValue();       
        final JDialog dialog = new JDialog(gui.getFrame(), "Error", true);
        dialog.setComponentOrientation(pane.getComponentOrientation());
        java.awt.Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(pane, BorderLayout.CENTER);
        dialog.setResizable(false);
        WindowAdapter adapter = new WindowAdapter() {
            private boolean gotFocus = false;
            public void windowClosing(java.awt.event.WindowEvent we) {
                pane.setValue(null);
            }
            public void windowGainedFocus(java.awt.event.WindowEvent we) {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    pane.selectInitialValue();
                    gotFocus = true;
                }
            }
        };
        dialog.addWindowListener(adapter);
        dialog.addWindowFocusListener(adapter);
        pane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if (dialog.isVisible() && event.getSource() == pane &&
                  (event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) &&
                  event.getNewValue() != null &&
                  event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
                    dialog.setVisible(false);
                }
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(gui.getFrame());
        Timer retryTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdowntimer[0]--;
                if (countdowntimer[0]==0) {
                   pane.setValue("Retry");
                   dialog.setVisible(false);
                } else {
                   dialog.setTitle("Error     (Automatic retry in "+countdowntimer[0]+" second"+((countdowntimer[0]==1)?"":"s")+")");
                }
            }
        });
        retryTimer.setInitialDelay(15000); // 15 sec delay before automatic retry countdown starts
        retryTimer.setRepeats(true);
        retryTimer.start();
        dialog.setVisible(true);
        dialog.dispose();
        Object selectedValue = pane.getValue();
        if(selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        if(options == null) {
            if(selectedValue instanceof Integer)
                return ((Integer)selectedValue).intValue();
            return JOptionPane.CLOSED_OPTION;
        }
        for(int counter = 0, maxCounter = options.length;
            counter < maxCounter; counter++) {
            if(options[counter].equals(selectedValue))
                return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    
    @Override
    public boolean handleUncaughtException(final Throwable e) {
        if (e instanceof IllegalStateException) return true; // do nothing for this type of exception
        //e.printStackTrace(System.err);
        Runnable runner=new Runnable() {
            @Override
            public void run() { // 
                if (e instanceof OutOfMemoryError) {
                    JOptionPane.showMessageDialog(getFrame(),"There was not enough available memory left to perform the requested task.\nPlease run MotifLab with more memory to continue with this analysis.", "Out of memory", JOptionPane.ERROR_MESSAGE);
                } else if (e instanceof ConcurrentModificationException) { // aka: Bad Programming Error
                    JOptionPane.showMessageDialog(getFrame(),"A problem of type 2904 has occurred!\n\nThis is not a serious error, but you might want to try that last thing you did one more time.", "Oops :-|", JOptionPane.ERROR_MESSAGE);
                } else {
                    int option=JOptionPane.showOptionDialog(getFrame(), e.getClass().getSimpleName()+":\n"+e.getMessage(), "ERROR: Uncaught Exception", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE,null, new String[]{"OK","Send Error Report"},"OK");
                    if (option==1) {
                        engine.reportError(e);
                        //logMessage(e.getClass().getSimpleName()+": "+e.getMessage());
                        e.printStackTrace(System.err);
                    }
                }
                // scheduler.abortEmergency(); // in case the error occurred while some task was executing
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
        return true; // this is to implement the interface
    }

    @Override
    /** This method implements shutdown() from MotifLab Client
      *  Note this method is only called by the engine if serious error happens which makes the system go down. 
     *   The engine will then call client.shutdown to allow the client to perform cleanup
      *  This method is not called on normal program exit, so any common clean-up code 
     *   should be put in the cleanup() method in this class rather than here. 
     */
    public void shutdown() {
        engine.shutdown(); // the engine calls shutdown in the client but does not call shutdown on itself in case the client needs the engine a bit more. So we must shutdown the engine afterwards
        getApplication().exit(); // this will call gui.cleanup()
    }


    /**
     * Returns a File chooser dialog that supports Data Repositories as well as the regular file systems
     * The dialog also allows the user to delete selected files or folders by pressing the DELETE key
     * @param directory
     * @return 
     */
    public JFileChooser getFileChooser(File directory) {
        if (directory==null) directory=getLastUsedDirectory();
        MotifLabFileChooser filechooser=new MotifLabFileChooser(directory, engine);
        return filechooser;
    }

    public File getNewOpenFileSelection(String oldfilename, boolean directoriesOnly, FileNameExtensionFilter[] filters) {        
        File file=(oldfilename!=null)?engine.getFile(oldfilename):null;
        File directory=null;
        if (file!=null) {
            directory=(file.isDirectory())?file:file.getParentFile();
        }
        JFileChooser chooser = getFileChooser(directory);
        if (directoriesOnly) chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
        if (filters!=null && filters.length>0) {
            for (FileNameExtensionFilter filter:filters) {chooser.addChoosableFileFilter(filter);}
            chooser.setFileFilter(filters[0]);
        }      
        if (file!=null) chooser.setSelectedFile(file);
        int selection=chooser.showOpenDialog(getFrame());
        if (selection==JFileChooser.APPROVE_OPTION) {
             File selectedFile=chooser.getSelectedFile(); 
             setLastUsedDirectory((selectedFile.isDirectory())?selectedFile:selectedFile.getParentFile());
             return selectedFile;
        } else return null;
    }
    
    public File getNewSaveFileSelection(String oldfilename, boolean directoriesOnly) {        
        File file=(oldfilename!=null)?engine.getFile(oldfilename):null;
        File directory=null;
        if (file!=null) {
            directory=(file.isDirectory())?file:file.getParentFile();
        }
        final JFileChooser chooser = getFileChooser(directory);
        if (directoriesOnly) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);   
        } 
        if (file!=null) {
            // if the file is a dir, setSelectedFile(dirfile) will change the dialog to show the parent folder. 
            // However, if the dir-file is "top level" (i.e. root), the parent will be null and then a default "home" folder will be displayed instead, which is probably not what we want (since it is not the correct parent)
            if (!(file.isDirectory() && file.getParentFile()==null)) chooser.setSelectedFile(file);
        } 
        int selection=chooser.showSaveDialog(getFrame());
        if (selection==JFileChooser.APPROVE_OPTION) {            
             File selectedFile=chooser.getSelectedFile(); 
             if (selectedFile instanceof DataRepositoryFile) { // This is a hack to fix a peculiar bug. Not really sure how to fix it otherwise...
                 String repositoryPrefix=((DataRepositoryFile)selectedFile).getRepositoryName()+":";
                 String path=selectedFile.getAbsolutePath();
                 if (path.startsWith(repositoryPrefix+repositoryPrefix)) { // If the "root" directory of a datarepository is selected, the returned file will be "repname:repname:" (prefix repeated twice)
                     path=path.substring(repositoryPrefix.length());
                     selectedFile=engine.getFile(path);
                 }
             }    
             setLastUsedDirectory((selectedFile.isDirectory())?selectedFile:selectedFile.getParentFile());
             return selectedFile;
        } else return null;
    }    
    
    @Action
    public void protocolBrowseButtonPressed() {
        String currentFile=protocolFilenameTextfield.getText();
        File test=(currentFile!=null)?engine.getFile(currentFile):null;
        if (test==null || !test.exists()) currentFile=null;
        File newFile=getNewOpenFileSelection(currentFile,false,null);
        if (newFile!=null && newFile.exists()) {
            protocolFilenameTextfield.setText(newFile.getAbsolutePath());
            loadProtocolFile(newFile.getAbsolutePath());
        }
    }
    
    @Action
    public void browseSequencesFile() {
        String currentFile=sequencesFilenameTextfield.getText();
        File test=(currentFile!=null)?engine.getFile(currentFile):null;
        if (test==null || !test.exists()) currentFile=null;
        File newFile=getNewOpenFileSelection(currentFile,false,null);
        if (newFile!=null && newFile.exists()) {
            sequencesFilenameTextfield.setText(newFile.getAbsolutePath());
        }        
    }  
    
    @Action
    public void browseInputSessionFile() {
        String currentFile=inputSessionTextfield.getText();
        File test=(currentFile!=null)?engine.getFile(currentFile):null;
        if (test==null || !test.exists()) currentFile=null;
        FileNameExtensionFilter[] filters=new FileNameExtensionFilter[]{new FileNameExtensionFilter("MotifLab Session (*.mls)", "mls")};
        File newFile=getNewOpenFileSelection(currentFile,false,filters);
        if (newFile!=null && newFile.exists()) {
            inputSessionTextfield.setText(newFile.getAbsolutePath());
        }        
    }  
    
    @Action
    public void browseOutputDirectory() {
        String currentFile=outputDirectoryTextfield.getText();
        File test=(currentFile!=null)?engine.getFile(currentFile):null;
        if (test==null || !test.exists() || !test.isDirectory()) currentFile=null;
        File newFile=getNewSaveFileSelection(currentFile,true);
        if (newFile!=null && newFile.exists() && newFile.isDirectory()) {
            outputDirectoryTextfield.setText(newFile.getAbsolutePath());
        }
    }    
    
    private void debugFile(Object file) {
        if (file==null) System.err.println("File=null");
        else if (file instanceof File) {
            System.err.println("(File)["+(  (((File)file).isDirectory())?"dir":"file")+"]["+((file instanceof DataRepositoryFile)?"R":"*")+"]" +((File)file).getAbsolutePath()+"  =>  "+file.toString());
        } else if (file instanceof String) {
            file=engine.getFile((String)file);
            System.err.println("(String)["+(  (((File)file).isDirectory())?"dir":"file")+"]["+((file instanceof DataRepositoryFile)?"R":"*")+"]" +((File)file).getAbsolutePath());
        } else System.err.println("(File) MISTAKE");
    }
    
    private void loadProtocolFile(String filenameOrURL) {
        try  {
            ArrayList<String> contents=engine.readFileContents(filenameOrURL);
            StringBuilder builder=new StringBuilder();
            for (String string:contents) {builder.append(string); builder.append("\n");}
            protocolEditorPane.setText(builder.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(protocolTab, e.getClass().getSimpleName()+":\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }        
    }
    
    private StandardProtocol parseProtocolFromEditor() {       
        String text=protocolEditorPane.getText();
        if (text.trim().isEmpty()) return null;
        StandardProtocol protocol=new StandardProtocol(engine,text); 
        String protocolFileName=protocolFilenameTextfield.getText();
        if (protocolFileName!=null && !protocolFileName.isEmpty()) {
            File file = engine.getFile(protocolFileName);
            if (file!=null) {
                protocol.setName(file.getName());
                protocol.setFileName(file.getAbsolutePath());
            }
        } else protocol.setName("Protocol-1");       
        return protocol;
    }
    
    @Override
    public ArrayList<GeneIDmapping> selectCorrectMappings(ArrayList<GeneIDmapping> list, String id) {
        ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>();
        String[] displayNames=new String[list.size()];
        for (int i=0;i<list.size();i++) {
            GeneIDmapping mapping = list.get(i);
            int start=(mapping.TSS<mapping.TES)?mapping.TSS:mapping.TES;
            int end  =(mapping.TSS<mapping.TES)?mapping.TES:mapping.TSS;
            String chromosome=(mapping.chromosome.startsWith("chr")?"":"chr")+mapping.chromosome+":"+start+"-"+end;
            String strand=((mapping.strand==Sequence.DIRECT)?"(Direct)":"(Reverse)"); 
            String string=mapping.geneName+"; "+chromosome+" "+strand;
            displayNames[i]=string;
        }
        JList displaylist = new JList(displayNames);
        displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);  
        JPanel internal=new JPanel(new BorderLayout());
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("<html>Found several matches for identifier: <b>"+id+"</b><br>Please select the one(s) you would like to use</html>"));
        JPanel center=new JPanel(new BorderLayout());
        center.add(new JScrollPane(displaylist));
        internal.add(top,BorderLayout.NORTH);
        internal.add(center,BorderLayout.CENTER);
        displaylist.setSelectedIndex(0);
        JOptionPane.showMessageDialog(getFrame(), internal, "Select genes", JOptionPane.QUESTION_MESSAGE);        
        int[] selected=displaylist.getSelectedIndices();
        if (selected!=null && selected.length>0) {
            for (Integer index:selected) {
                GeneIDmapping mapping=list.get(index);
                mapping.geneID=mapping.geneID+"_"+(index+1);
                result.add(mapping);
            }        
        }
        return result;
    }

    @Action
    public void saveShownDataObject() {
        if (resultsPanel.getComponentCount()==0) return;
        Component comp=resultsPanel.getComponent(0);
        if (comp instanceof OutputPanel) {
            OutputData data=((OutputPanel)comp).getOutputData();
            if (data!=null) {
                String outputDataName=data.getName();
                String suffix=data.getPreferredFileSuffix();
                String filename=outputDataName+"."+suffix;
                try {
                    if (outputdirname!=null && !outputdirname.isEmpty()) filename=engine.getFile(outputdirname, filename).getCanonicalPath();                   
                    File file=getNewSaveFileSelection(filename, false);
                    if (file==null) return;
                    filename=file.getCanonicalPath();
                    statusMessage("Saving file: "+filename);
                    data.saveToFile(file, true, progressMonitor, engine);
                } catch (Exception io) {
                    JOptionPane.showMessageDialog(getFrame(),"ERROR: Unable to save file '"+filename+"'\n"+io.getMessage(), "Save Error",JOptionPane.ERROR_MESSAGE);                    
                } finally {
                    setProgress(101);
                }
            }
        }
    }

    @Override
    public ParameterSettings promptForValues(final Parameter[] parameters, final ParameterSettings defaultsettings, final String message, final String title) {
        // this method could be called from a background thread off the EDT so we must run it on the EDT and block the other thread until finished
        final java.awt.Frame frame=this.getFrame();
        final ParameterSettings[] values=new ParameterSettings[]{null}; // this is also used as a monitor for synchronization
        Runnable runner=new Runnable() {          
            @Override
            public void run() {
                synchronized (values) {          
                    ParametersPanelPrompt dialog = new ParametersPanelPrompt(frame, getEngine(), message, title, parameters, defaultsettings);
                    dialog.enableDefaultOKButton();
                    int height=dialog.getHeight();
                    int width=dialog.getWidth();
                    java.awt.Dimension size=frame.getSize();
                    int x=(int)((size.width-width)/2);
                    int y=(int)((size.height-height)/2);
                    if (x<0) x=0;
                    if (y<0) y=0;
                    dialog.setLocation(x,y);
                    dialog.setVisible(true); // note that this will not block other threads...                                    
                    values[0]=(dialog.isOKPressed())?dialog.getParameterSettings():null; // use NULL to signal that the dialog was cancelled
                    dialog.dispose();                       
                    values.notifyAll(); // release synchronization lock to allow caller to access the return value                 
                }
            }
        };
        // Show the prompt and block until the prompt is closed                  
        if (SwingUtilities.isEventDispatchThread()) {
            runner.run(); // since the runner will be executed in the same thread as this (EDT), this method will "block" until the runner is completed (synchronous execution)
        }
        else {
            synchronized (values) {
                SwingUtilities.invokeLater(runner); // The runner will be executed on the EDT which is a different thread, so this method will not be blocked (asynchronous execution)
                try {values.wait();} catch (Exception e){} // we must therefore use a monitor to block this method and wait until the prompt dialog has been closed and the monitor is released                 
            }
        }           
        if (values[0]!=null) return values[0];
        else return new ParameterSettings();
    }  
    
    
    
private class DataPanelCellRenderer extends JPanel implements ListCellRenderer<Object>  {
    JLabel typeiconlabel; // a JLabel which is a placeholder for the typeicon
    JLabel name; // a JLabel which displays the dataset name
    SimpleDataPanelIcon typeicon; // the icon to the left of the dataset name which shows the type of dataset
    VisualizationSettings visualizationsettings;
    Font font;
    Font boldfont;
    Font bolditalicfont;
            
    public DataPanelCellRenderer(VisualizationSettings visualizationsettings) {
        super();
        this.visualizationsettings=visualizationsettings;
        setOpaque(false);
        setLayout(new BorderLayout());
        typeicon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.UNKNOWN_ICON,visualizationsettings);
        typeicon.drawBorder(false);
        typeicon.setBackgroundColor(this.getBackground());
        typeiconlabel=new JLabel(typeicon);
        name=new JLabel("");
        name.setMinimumSize(new Dimension(30,20));

        add(typeiconlabel,BorderLayout.WEST);
        add(name,BorderLayout.CENTER);
        setPreferredSize(new Dimension(40,21));
        setMinimumSize(new Dimension(40,21));
        name.setOpaque(true);
        typeiconlabel.setBorder(BorderFactory.createRaisedBevelBorder());
        font=this.getFont();
        boldfont=font.deriveFont(java.awt.Font.BOLD);
        bolditalicfont=font.deriveFont(java.awt.Font.BOLD+java.awt.Font.ITALIC);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Data dataobject=(Data)value;
        if (isSelected) {
            name.setBackground(list.getSelectionBackground());
            name.setForeground(list.getSelectionForeground());
        } else {
            name.setBackground(Color.WHITE);// list.getBackground());
            name.setForeground(list.getForeground());      
        }
        if (dataobject==null) { // this really should not happen
             typeicon.setIconType(SimpleDataPanelIcon.HIDDEN_ICON);
             name.setText("   ");
             return this; 
        }
        
        if (dataobject instanceof NumericDataset || dataobject instanceof RegionDataset) { 
          typeicon.setForegroundColor(visualizationsettings.getForeGroundColor(dataobject.getName()));
          typeicon.setBackgroundColor(visualizationsettings.getBackGroundColor(dataobject.getName()));
          typeicon.setSecondaryColor(visualizationsettings.getSecondaryColor(dataobject.getName()));          
          typeicon.setBaselineColor(visualizationsettings.getBaselineColor(dataobject.getName()));   
          if (dataobject instanceof NumericDataset) {
              typeicon.setGradient(visualizationsettings.getColorGradient(dataobject.getName()));
              typeicon.setSecondaryGradient(visualizationsettings.getSecondaryGradient(dataobject.getName()));
          }
        } else {
          typeicon.setForegroundColor(Color.black);
          typeicon.setBackgroundColor(Color.white);            
        }
//             if (dataobject instanceof RegionDataset) typeicon.setIconType(SimpleDataPanelIcon.REGION_ICON);
//        else if (dataobject instanceof NumericDataset) typeicon.setIconType(SimpleDataPanelIcon.NUMERIC_TRACK_GRAPH_ICON);                
        typeicon.setIconType(SimpleDataPanelIcon.getIconTypeForData(dataobject));
        String typedescription=dataobject.getTypeDescription();
        if (dataobject instanceof RegionDataset && ((RegionDataset)dataobject).isModuleTrack()) {
            typedescription+=", Module track";
            name.setFont(bolditalicfont);
        }
        else if (dataobject instanceof RegionDataset && ((RegionDataset)dataobject).isMotifTrack()) {
            typedescription+=", Motif track";
            name.setFont(boldfont);
        } else if (dataobject instanceof RegionDataset && ((RegionDataset)dataobject).isNestedTrack()) {
            typedescription+=", Linked track";
        } else name.setFont(font);
        this.setToolTipText(dataobject.getName()+"    [ "+typedescription+" ]");
        name.setText("   "+dataobject.getName());
        return this;
    }    
    
} // end of internal class DataPanelCellRenderer


private class DataInjectionsCellEditor extends AbstractCellEditor implements TableCellEditor {   
        JPanel component;
        JTextField textField;
        JLabel browseButton;
    
        public DataInjectionsCellEditor() {             
             textField=new JTextField();
             textField.setBorder(BorderFactory.createEmptyBorder());
             browseButton=new JLabel(new MiscIcons(MiscIcons.ELLIPSIS)); // horizontal ellipsis
             browseButton.setVerticalTextPosition(SwingConstants.TOP);
             browseButton.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
             component=new JPanel(new BorderLayout());
             component.add(textField,BorderLayout.CENTER);
             component.add(browseButton,BorderLayout.EAST);         
             browseButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    File selectedFile=null;
                    String text=textField.getText();
                    if (text.startsWith("file:")) text=text.substring("file:".length());
                    if (text!=null && !text.isEmpty()) {
                        File file=engine.getFile(text);
                        if (file.exists()) selectedFile=file;
                    }
                    JFileChooser chooser=getFileChooser(lastUsedDirectory);
                    if (selectedFile!=null) chooser.setSelectedFile(selectedFile);
                    int selection=chooser.showOpenDialog(getFrame());
                    if (selection==JFileChooser.APPROVE_OPTION) {
                         selectedFile=chooser.getSelectedFile(); 
                         setLastUsedDirectory((selectedFile.isDirectory())?selectedFile:selectedFile.getParentFile());
                         textField.setText("file:"+selectedFile.getAbsolutePath());
                    }
                }
                 
             });
        }
      
        
        public Object getCellEditorValue() {
            String value=textField.getText();
            return (value!=null)?value:"";
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setText((value!=null)?value.toString():"");
            textField.requestFocusInWindow();
            return component;
        }   
        
        
    }

  private void restoreGUIsettings() {
         // initialize with settings used on previous occasdion
        try {
            Object settingsObject=engine.loadSystemObject("SequenceDialogSettings.ser");
            if (settingsObject instanceof String[]) {
                String[] settings=(String[])settingsObject;
                //if (settings.length>0 && settings[0]!=null) identifierTypeCombobox.setSelectedItem(settings[0]);
                if (settings.length>1 && settings[1]!=null) organismCombobox.setSelectedItem(settings[1]);
                if (settings.length>2 && settings[2]!=null) genomebuildCombobox.setSelectedItem(settings[2]);
            }
            Object otherSettingsObject=engine.loadSystemObject("minimalGUIsettings.ser");
            String input="none";
            if (otherSettingsObject instanceof Object[]) {
                Object[] settings=(Object[])otherSettingsObject;
                if (settings.length>0 && settings[0]!=null) outputDirectoryTextfield.setText(settings[0].toString());
                if (settings.length>1 && settings[1]!=null) input=settings[1].toString();
                if (settings.length>2 && settings[2]!=null) wgaRegionTextfield.setText(settings[2].toString());     
                if (settings.length>3 && settings[3]!=null) wgaSegmentSizeSpinner.setValue((Integer)settings[3]);  
                if (settings.length>4 && settings[4]!=null) wgaCollectionSizeSpinner.setValue((Integer)settings[4]);  
                if (settings.length>5 && settings[5]!=null) wgaOverlapSpinner.setValue((Integer)settings[5]);                  
            }
            if (input.equals("sequences")) sequencesRadioButton.doClick();
            else if (input.equals("chromosome")) wgaRadioButton.doClick();
            else if (input.equals("dialog")) sequencesDialogRadioButton.doClick();
            else noSequencesRadioButton.doClick();
        } catch (Exception e) {}       
  }

  private void storeGUIsettings(){
    // save current settings to initialize dialog with the same selections next time
    try {
        String[] settings=null;
        Object settingsObject=engine.loadSystemObject("SequenceDialogSettings.ser");
        if (settingsObject instanceof String[]) {
            settings=(String[])settingsObject; // reuse existing settings. Just replace a few of them
        } else { 
           settings=new String[5]; // no previously stored settings. Create a new one           
        }      
        if (settings.length>1) settings[1]=(String)organismCombobox.getSelectedItem();
        if (settings.length>2) settings[2]=(String)genomebuildCombobox.getSelectedItem();        
        engine.storeSystemObject(settings, "SequenceDialogSettings.ser");   
        String input="none"; // "none", "sequences", "dialog" or "chromosome"
        if (wgaRadioButton.isSelected()) input="chromosome";
        else if (sequencesRadioButton.isSelected()) input="sequences";
        else if (sequencesDialogRadioButton.isSelected()) input="dialog";
        else input="none";
        Object[] otherSettings=new Object[]{
            outputDirectoryTextfield.getText(),
            input,
            wgaRegionTextfield.getText(),
            wgaSegmentSizeSpinner.getValue(),
            wgaCollectionSizeSpinner.getValue(),
            wgaOverlapSpinner.getValue()
        };
        engine.storeSystemObject(otherSettings, "minimalGUIsettings.ser");
    } catch (Exception e) {}       
  }

}

