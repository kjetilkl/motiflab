/*
 * MotifLabGUI.java
 */

package motiflab.gui;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import motiflab.engine.data.analysis.Analysis;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.EventObject;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.UndoableEditEvent;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.DataListener;
import motiflab.engine.data.*;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.MenuElement;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoableEdit;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.LocalStorage;
import motiflab.engine.task.AddSequencesTask;
import motiflab.engine.task.ClearDataTask;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.GeneIDmapping;
import motiflab.engine.ImportantNotification;
import motiflab.engine.MotifLabClient;
import motiflab.engine.MotifLabResource;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Plugin;
import motiflab.engine.ProgressListener;
import motiflab.engine.task.ProtocolTask;
import motiflab.engine.SystemError;
import motiflab.engine.datasource.DataRepositoryFile;
import motiflab.engine.operations.Condition_within;
import motiflab.engine.operations.Operation_analyze;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.operations.PromptConstraints;
import motiflab.engine.protocol.DataTypeTable;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.Protocol;
import motiflab.engine.protocol.SerializedStandardProtocol;
import motiflab.engine.protocol.StandardProtocol;
import motiflab.gui.operationdialog.*;
import motiflab.gui.prompt.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.core.util.TypeUtil;



/**
 * The application's main frame.
 */
public final class MotifLabGUI extends FrameView implements MotifLabClient, DataListener {

    public static final String PREFERENCES_PROMPT_BEFORE_DISCARD="promptBeforeDiscard";
    public static final String PREFERENCES_NOTIFICATIONS_MINIMUM_LEVEL="notificationsMinimumLevel";
    public static final String AUTOSAVE_SESSION_ON_EXIT="autoSaveSessionOnExit";
    public static final String PREFERENCES_SKIP_POSITION_0="skipPositionZero";
    public static final String SHOW_SIDE_PANEL_AS_TABS="showSidePanelAsTabs";
    public static final String PREFERENCES_ANTIALIAS_MODE="antialiasMode";

    public static final String SELECTION_TOOL="Selection tool";
    public static final String MOVE_TOOL="Move tool";
    public static final String ZOOM_TOOL="Zoom tool";
    public static final String DRAW_TOOL="Draw tool";

    public static Object ANTIALIAS_MODE=RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;

    private static final int tooltipLineLength=100; // break tooltips after 100 chars

    private MotifLabEngine engine=null;
    private VisualizationSettings visualizationSettings=null;
    private RegionVisualizationFilter regionVisualizationFilter=null;
    private String selectedToolName=null;
    private VisualizationPanel visualizationPanel;
    private FeaturesPanel featuresPanel;
    private DataObjectsPanel dataObjectsPanel;
    private MotifsPanel motifsPanel;
    private ProtocolEditor protocolEditor;
    private GUIUndoManager undoManager;
    private GuiScheduler scheduler=null;
    private MainPanel mainWindow;
    private UndoRedoDocumentListener undoRedoDocumentListener;
    private ProgressMonitor progressMonitor;
    private ArrayList<String> recentsessions=new ArrayList<String>(); // can contain filename or URL-path
    private OpenRecentSessionMenuListener openrecentsessionmenulistener;
    private static final String recentsessionsfilename="recentsession.ser";
    private static final String lastusedlocationfilename="lastdir.ser";
    private static final String autosavedsessionfilename="autosavedsession.mls";
    private int maxRecentSessions=8; // max number of recent sessions to show in menu. This should probably be configurable somehow!
    private boolean promptBeforeDiscard=true;
    private boolean skip0=true;
    private JMenu viewInSidePanelMenu;  // lists open tabs in main window in the view menu
    private JCheckBoxMenuItem showfeatures;
    private JCheckBoxMenuItem showmotifs;
    private JCheckBoxMenuItem showdata;
    private JCheckBoxMenuItem showSidePanelAsTabs;
    private JFrame clientFrame=null;

    private DecimalFormat motifcounterformat = new DecimalFormat("0000");
    private Preferences preferences;
    private MemoryMonitor memorymonitor;
    private File lastUsedDirectory=null;
    private HashSet<String> reservedDataNames=new HashSet<String>(); // this is used by GUI-dialogs to reserve names of new data items until they are either stored properly or the task is aborted
    private JCheckBoxMenuItem enableCloseupViewCheckBoxMenuItem;
    private JCheckBoxMenuItem showLogoInCloseupViewCheckBoxMenuItem;
    private JCheckBoxMenuItem shownamesCheckBoxMenuItem;
    private MotifBrowser motifbrowser=null;
    private ModuleBrowser modulebrowser=null;
    private SequenceBrowser sequencebrowser=null;
    private RegionBrowser regionbrowser=null;
    private JSearchTextField searchTextfield;
    private JButton newNotificationsButton;
    private ArrayList<RedrawListener> redrawlisteners=null;
    private HashMap<String,String> externalProgramDefaults=null; // stores the name of the program which is selected as 'first choice' for motifDiscovery, motifscanning etc. The key is the program type and the value is the name of a program
    private Favorites favorites;
    private boolean saveSessionOnExit=false;
    private String autoSaveSessionSetting="Never";
    private int loggerLevel=2; // 1=show only errors, 2=show normal messages, 3=show execution log, 4=show every status update
    private int stackDumpLevel=0; // 0=no stack dumps, 1=stack dump critical errors, 2=stack dump all errors
    private boolean useColorsInLog=true;

    private final static NumberFormat normalNumberFormat=NumberFormat.getInstance();
    private final static NumberFormat scientificNumberFormat=new DecimalFormat("0.#####E0");
    private ProgressListener progressListener = new ProgressListener() {
        @Override
        public void processProgressEvent(Object source, int progress) {
            MotifLabGUI.this.setProgress(progress);
        }
    };


    static {
        normalNumberFormat.setMaximumFractionDigits(3);
        normalNumberFormat.setGroupingUsed(false);
        scientificNumberFormat.setMaximumFractionDigits(3);
        scientificNumberFormat.setGroupingUsed(false);
    }
    /**
     * Creates a new instance of MotifLab
     * @param app
     */
    public MotifLabGUI(SingleFrameApplication app, MotifLabEngine motiflabEngine) {
        super(app); // initialize some GUI framework
        try {
            ArrayList<Image>iconlist=new ArrayList<Image>(3);
            iconlist.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/MotifLabIcon_16.png")));
            iconlist.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/DNAhelix24.png")));
            iconlist.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/DNAhelix32.png")));
            this.getFrame().setIconImages(iconlist);
        } catch (Exception e) {}
        setEngine(motiflabEngine); // initialize the MotifLabEngine
        engine.addDataListener(this);
        visualizationSettings=VisualizationSettings.getInstance(this);
        registerDataTrackVisualizers();
        engine.executeStartupConfigurationProtocol();
        preferences = Preferences.userNodeForPackage(this.getClass());
        setPromptBeforeDiscard(preferences.getBoolean(PREFERENCES_PROMPT_BEFORE_DISCARD, promptBeforeDiscard));
        setSkipPosition0(preferences.getBoolean(PREFERENCES_SKIP_POSITION_0, skip0));
        setNumericTrackOptimizationSettings(
            preferences.getInt(VisualizationSettings.NUMERIC_TRACK_DISPLAY_VALUE, 0),
            preferences.getInt(VisualizationSettings.NUMERIC_TRACK_SAMPLING_CUTOFF, 100),
            preferences.getInt(VisualizationSettings.NUMERIC_TRACK_SAMPLING_NUMBER, 20)
        );
        setAntialiasMode(preferences.get(PREFERENCES_ANTIALIAS_MODE, "On"));
        setAutoSaveSessionSetting(preferences.get(AUTOSAVE_SESSION_ON_EXIT, autoSaveSessionSetting));
        openrecentsessionmenulistener=new OpenRecentSessionMenuListener();
        getApplication().addExitListener(new ConfirmExit());
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
        undoManager=new GUIUndoManager(context,this);
        scheduler=new GuiScheduler(this);
        initComponents(); // lay out the components
        newNotificationsButton=new JButton(); // note that this is not added to the current toolbar here
        newNotificationsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayNotifications(false, true);
            }
        });
        newNotificationsButton.setFocusable(true);
        newNotificationsButton.setContentAreaFilled(true);
        newNotificationsButton.setOpaque(true);
        newNotificationsButton.setBorderPainted(true);
        newNotificationsButton.setRolloverEnabled(false);
        newNotificationsButton.setBackground(Color.RED);
        newNotificationsButton.setForeground(Color.WHITE);
        newNotificationsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newNotificationsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        viewInMainWindowMenu=new JMenu("Main Panel");
        viewMenu.add(viewInMainWindowMenu);
        //viewMenu.add(new JSeparator());
        viewInSidePanelMenu=new JMenu("Data Panels");
        showSidePanelAsTabs = new JCheckBoxMenuItem("Show Tabbed",preferences.getBoolean(SHOW_SIDE_PANEL_AS_TABS, false));
        showSidePanelAsTabs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSidePanelSetup();
            }
        });
        viewInSidePanelMenu.add(showSidePanelAsTabs);
        viewInSidePanelMenu.add(new JSeparator());
        viewMenu.add(viewInSidePanelMenu);
        final JMenuItem closeTabsitem=new JMenuItem("Close All Output Panels");
        closeTabsitem.setToolTipText("Close all output panels currently displayed in tabs in the main panel");
        closeTabsitem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getMainWindow().closeAllTabs();
            }
        });
        viewMenu.add(closeTabsitem);
        viewMenu.add(new JSeparator());
        setupCondensedModeMenuItem();
        setupBaseCursorMenuItem();
        setupShowTrackNamesMenuItem();
        setupExampleSessionsMenu();
        setupOperationsMenu();

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
        scheduler.addPropertyChangeListener(progressMonitor);
        searchTextfield = new JSearchTextField();
        searchTextfield.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findAction();
            }
        });
        toolbarSearchPanel.add(searchTextfield, BorderLayout.EAST);
        searchTextfield.setToolTipText("<html>Search the selected panel for matching text or data objects.<br><br>"
                + "If the text matches a data object or part of a document,<br>"
                + "the matching object or segment will be highlighted.<br>"
                + "Press <tt><b>CONTROL+F</b></tt> to search for additional matches.<br><br>"
                + "Note that the search function will only search within a single panel.<br>"
                + "To select the panel to search simply press the mouse within that panel<br>"
                + "(e.g. a data panel, the protocol editor or an output panel).<br>"
                +" The target panel is reported in the status bar along with the status for the search."
                + "</html>"
        );

        undoManager.addUndoableEditListener(new UndoRedoButtonUpdater());
        setSelectedTool(SELECTION_TOOL);
        selectionToolButton.setSelected(true);
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("stopAction").setEnabled(false);
        mainWindow=new MainPanel(this,viewInMainWindowMenu);
        mainWindow.setMinimumSize(new java.awt.Dimension(200,200));
        mainWindow.setPreferredSize(new java.awt.Dimension(800,600));
        verticalSplitPane.setRightComponent(mainWindow);
        featuresPanel=new FeaturesPanel(this);
        dataObjectsPanel=new DataObjectsPanel(this);
        motifsPanel=new MotifsPanel(this);

        visualizationPanel=new VisualizationPanel(getVisualizationSettings());
        protocolEditor = new ProtocolEditor(this);
        mainWindow.addTab("Visualization",visualizationPanel,false);
        mainWindow.addTab("Protocol",protocolEditor,false);
        ProtocolManagerListener pml=new ProtocolManagerListener();
        protocolEditor.getProtocolManager().addListDataListener(pml);
        pml.populateProtocolList();
        updateRecentSessionsMenu(true);
        favorites=new Favorites(new File(engine.getMotifLabDirectory()+File.separator+"favorites.data"));
        memorymonitor=new MemoryMonitor(this,8);
        logLeftPanel.setLayout(new BorderLayout());
        logLeftPanel.setBorder(BorderFactory.createEmptyBorder(8,5,8,5));
        logLeftPanel.add(memorymonitor);
        ToolTipManager tpm=ToolTipManager.sharedInstance();
        tpm.setDismissDelay(Integer.MAX_VALUE);
        //  tpm.setInitialDelay(0);
        clearLogLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearLog();
            }
        });
        try {
           Object loggerLevelString=engine.loadSystemObject("loggerLevel.ser");
           if (loggerLevelString instanceof String) {
               String string=(String)loggerLevelString;
               if (string.contains(":")) {
                   String[] parts=string.split(":");
                   Integer level=Integer.parseInt(parts[0]);
                   Integer dump=Integer.parseInt(parts[1]);
                   setLoggerLevel(level, dump, false);
               }
           } else setLoggerLevel(2,0, true);
        } catch (Exception e) {
            setLoggerLevel(2,0, true);
        }
        logLevelIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // incrementLogLevel();
                ActionListener listener=new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int newLevel=loggerLevel;
                        int newDump=stackDumpLevel;
                             if (e.getActionCommand().equals("Logging: Very High")) newLevel=4;
                        else if (e.getActionCommand().equals("Logging: High")) newLevel=3;
                        else if (e.getActionCommand().equals("Logging: Regular")) newLevel=2;
                        else if (e.getActionCommand().equals("Logging: Low")) newLevel=1;
                        else if (e.getActionCommand().equals("Debugging: High")) newDump=2;
                        else if (e.getActionCommand().equals("Debugging: Low")) newDump=1;
                        else if (e.getActionCommand().equals("Debugging: None")) newDump=0;
                        setLoggerLevel(newLevel, newDump, true);
                    }
                };
                JPopupMenu menu=new JPopupMenu();
                JCheckBoxMenuItem level4=new JCheckBoxMenuItem("Logging: Very High", loggerLevel==4);
                JCheckBoxMenuItem level3=new JCheckBoxMenuItem("Logging: High", loggerLevel==3);
                JCheckBoxMenuItem level2=new JCheckBoxMenuItem("Logging: Regular", loggerLevel==2);
                JCheckBoxMenuItem level1=new JCheckBoxMenuItem("Logging: Low", loggerLevel==1);
                level4.addActionListener(listener);
                level3.addActionListener(listener);
                level2.addActionListener(listener);
                level1.addActionListener(listener);
                menu.add(level4);
                menu.add(level3);
                menu.add(level2);
                menu.add(level1);
                menu.add(new JSeparator());
                JCheckBoxMenuItem debug3=new JCheckBoxMenuItem("Debugging: High",stackDumpLevel==2);
                JCheckBoxMenuItem debug2=new JCheckBoxMenuItem("Debugging: Low",stackDumpLevel==1);
                JCheckBoxMenuItem debug1=new JCheckBoxMenuItem("Debugging: None",stackDumpLevel==0);
                debug3.addActionListener(listener);
                debug2.addActionListener(listener);
                debug1.addActionListener(listener);
                menu.add(debug3);
                menu.add(debug2);
                menu.add(debug1);
                menu.show(e.getComponent(), e.getX(),e.getY());
                return;
            }
        });
        // force JVM to load the Prompt_Motif class (just so it will appear faster later)
        Prompt_Motif forceload=new Prompt_Motif(MotifLabGUI.this, null, null, visualizationSettings,true);
        forceload.dispose();
        JFileChooser fc = getFileChooser(getLastUsedDirectory());

        visualizationSettings.resetPaletteIndex();
        setupSidePanelOnStartup(showSidePanelAsTabs.isSelected());

        String autosavesession=engine.getMotifLabDirectory()+File.separator+autosavedsessionfilename;;
        if (engine.systemObjectFileExists("lastSavedSessionName")) {
            try {
                Object autosavesessionObject=engine.loadSystemObject("lastSavedSessionName");
                if (autosavesessionObject!=null && autosavesessionObject instanceof String) autosavesession=(String)autosavesessionObject;
            } catch (Exception e) {}
        }
        File file=new File(autosavesession);
        if (autosavesession!=null && file.exists()) {
            restoreSessionInBackground(file);
        }
        clearLogLabel.requestFocusInWindow(); // just to move focus away from search-textfield
        initializePluginsFromClient();
    }


    /** This method should only be used to create skeleton MotifLabGUI objects that will not be used as the main client, but that could be used for other things,
     *  such as e.g. a proxy for calling promptValue()
     */
    public MotifLabGUI(SingleFrameApplication app, MotifLabEngine engine, VisualizationSettings settings, JFrame frame, Timer messageTimer, Timer busyIconTimer) {
        super(app);
        this.engine=engine; // note that setEngine(engine) will also set the client to this GUI, so we will not use it
        engine.addDataListener(this);
        setClientFrame(frame);
        visualizationSettings=settings;
        this.messageTimer = messageTimer;
        this.busyIconTimer=busyIconTimer;
        idleIcon=null;
    }

    private void registerDataTrackVisualizers() {
        Class[] allDTVs= new Class[]{
                DataTrackVisualizer_Numeric_BarGraph.class,
                DataTrackVisualizer_Numeric_LineGraph.class,
                DataTrackVisualizer_Numeric_OutlinedGraph.class,
                DataTrackVisualizer_Numeric_GradientBarGraph.class,
                DataTrackVisualizer_Numeric_HeatMap1Color.class,
                DataTrackVisualizer_Numeric_HeatMap2Colors.class,
                DataTrackVisualizer_Numeric_HeatMapRainbow.class,
                DataTrackVisualizer_Numeric_DNA_Graph.class,
                DataTrackVisualizer_Sequence_DNA.class,
                DataTrackVisualizer_Sequence_DNA_Colored.class,
                DataTrackVisualizer_Sequence_DNA_Monochrome.class,
                DataTrackVisualizer_Sequence_DNA_Big.class,
                DataTrackVisualizer_Region_Default.class             
        };

        for (Class type:allDTVs) {
            try {
               DataTrackVisualizer dtv=(DataTrackVisualizer)type.newInstance();
               engine.registerResource((MotifLabResource)dtv); // all DataTrackVisualizers implement the MotifLabResource interface
            } catch (Exception e) {
                logError(e);                  
            }
        }
    }

    /* Returns names of all registered DataTrackVisualizers (graph types) for the given class type (can be FeatureDataset class or DTV class) */
    public ArrayList<String> getRegisteredDataTrackVisualizers(Class classtype) throws NullPointerException {
        // Convert the classtype to a DTV class if it is a FeatureDataset class
             if (DNASequenceDataset.class.isAssignableFrom(classtype) || DataTrackVisualizer_Sequence.class.isAssignableFrom(classtype)) classtype=DataTrackVisualizer_Sequence.class;
        else if (NumericDataset.class.isAssignableFrom(classtype) || DataTrackVisualizer_Numeric.class.isAssignableFrom(classtype)) classtype=DataTrackVisualizer_Numeric.class;
        else if (RegionDataset.class.isAssignableFrom(classtype) || DataTrackVisualizer_Region.class.isAssignableFrom(classtype)) classtype=DataTrackVisualizer_Region.class;
        ArrayList<String> list=engine.getResourceNames(classtype);
        if (list==null) return new ArrayList<>(0);
        Collections.sort(list);
        return list;
    }


    /** Given the name of a registered Graph type (DataTrackVisualizer) this method will return
     *  return the next (or previous if the second argument is TRUE) DTV in the registered list
     *  whose type matches that of the named DTV type
     *  @param name The name of the "current" graph type
     *  @param previous If TRUE, the method will return the name of the previous graph type in the list rather than the next
     *  @return The name of the next (or previous) graph type in the (circular) list of all graph types for the same FeatureDataset type
     */
    public String getNextGraphType(String name, boolean previous) {
        Class type=engine.getResourceClass(name,"DTV");
        if (type==null || !(DataTrackVisualizer.class.isAssignableFrom(type))) return null; // provided graph type is not in the list
        ArrayList<String> list=getRegisteredDataTrackVisualizers(type); // get list of all compatible visualizers
        int index=list.indexOf(name);
        int size=list.size();
        if (size==0 || index<0) return null;
        else if (size==1) return list.get(0); // only a single entry
        int newindex=(previous)?(index-1):(index+1);
        if (newindex>=size) newindex-=size;
        else if (newindex<0) newindex+=size;
        return list.get(newindex);
    }

    /** Returns the Favorites repository */
    public Favorites getFavorites() {return favorites;}


    /** This is called when the GUI initializes to setup the SidePanel
     * and also the corresponding entries in the View data
     */
    private void setupSidePanelOnStartup(boolean showinTabs) {
        sidePanel.removeAll();
        boolean featuresPanelVisible=preferences.getBoolean("featuresPanelVisible", true);
        boolean motifsPanelVisible=preferences.getBoolean("motifsPanelVisible", true);
        boolean dataPanelsVisible=preferences.getBoolean("dataPanelsVisible", true);
        showfeatures=new JCheckBoxMenuItem("Features Panel",featuresPanelVisible);
        showmotifs=new JCheckBoxMenuItem("Motifs Panel",motifsPanelVisible);
        showdata=new JCheckBoxMenuItem("Data Panel",dataPanelsVisible);
        ActionListener showpanellistener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSidePanelSetup();
            }
        };
        showfeatures.addActionListener(showpanellistener);
        viewInSidePanelMenu.add(showfeatures);
        showmotifs.addActionListener(showpanellistener);
        viewInSidePanelMenu.add(showmotifs);
        showdata.addActionListener(showpanellistener);
        viewInSidePanelMenu.add(showdata);
        // setup side panel
        int count=0;
        JPanel[] panels=new JPanel[3];
        if (featuresPanelVisible) {panels[count]=featuresPanel; count++;}
        if (motifsPanelVisible) {panels[count]=motifsPanel; count++;}
        if (dataPanelsVisible) {panels[count]=dataObjectsPanel; count++;}
        int splitLocation1=preferences.getInt("splitLocation1", -1);
        int splitLocation2=preferences.getInt("splitLocation2", -1);

        if (showinTabs) { // show as tabbed panel
            JTabbedPane sideTabs=new JTabbedPane();
            if (featuresPanelVisible) {sideTabs.addTab("Features", featuresPanel);}
            if (motifsPanelVisible) {sideTabs.addTab("Motifs", motifsPanel);}
            if (dataPanelsVisible) {sideTabs.addTab("Data", dataObjectsPanel);}
            sidePanel.add(sideTabs);
        } else { // show as vertically split panels
            JPanel top=new JPanel();
            Dimension dim=new Dimension(2,29);
            top.setMinimumSize(dim);
            top.setMaximumSize(dim);
            top.setPreferredSize(dim);
            JPanel panelsPanel=new JPanel();
            panelsPanel.setName("_panelsPanel_");
            panelsPanel.setLayout(new BorderLayout());
            if (count==1) {
               panelsPanel.add(panels[0]);
            } else if (count==2) {
               JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
               split.setTopComponent(panels[0]);
               split.setBottomComponent(panels[1]);
               split.setDividerLocation(splitLocation1);
               panelsPanel.add(split);
            } else if (count==3) {
               JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
               JSplitPane split2=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
               split.setTopComponent(panels[0]);
               split.setBottomComponent(split2);
               split2.setTopComponent(panels[1]);
               split2.setBottomComponent(panels[2]);
               split.setDividerLocation(splitLocation1);
               split2.setDividerLocation(splitLocation2);
               panelsPanel.add(split);
            }
            sidePanel.add(top,BorderLayout.NORTH);
            sidePanel.add(panelsPanel,BorderLayout.CENTER);
        }
        sidePanel.revalidate();
        redraw();
    }

    private void storeSidePanelSettings() {
        preferences.putBoolean("featuresPanelVisible", showfeatures.isSelected());
        preferences.putBoolean("motifsPanelVisible", showmotifs.isSelected());
        preferences.putBoolean("dataPanelsVisible", showdata.isSelected());
        boolean showAsTabs=showSidePanelAsTabs.isSelected();
        preferences.putBoolean(SHOW_SIDE_PANEL_AS_TABS,showAsTabs);
        if (!showAsTabs) {
           JPanel panelspanel=null;
           for (int i=0;i<sidePanel.getComponentCount();i++) {
               Component comp=sidePanel.getComponent(i);
               String compname=comp.getName();
               if (compname!=null && compname.equals("_panelsPanel_")) {panelspanel=(JPanel)comp;break;}
           }
           int index=1;
           if (panelspanel!=null) {
               Component comp=panelspanel.getComponent(0);
               while (comp instanceof JSplitPane) {
                   int loc=((JSplitPane)comp).getDividerLocation();
                   preferences.putInt("splitLocation"+index,loc);
                   index++;
                   comp=((JSplitPane)comp).getBottomComponent(); // the bottom panel could be divided into further splitpanels
               }
           }
        } // end !showAsTabs
    }


    private void updateSidePanelSetup() {
        sidePanel.removeAll();
        boolean showinTabs=showSidePanelAsTabs.isSelected();
        if (showinTabs) { // show as tabbed panel
            JTabbedPane sideTabs=new JTabbedPane();
            if (showfeatures.isSelected()) {sideTabs.addTab("Features", featuresPanel);}
            if (showmotifs.isSelected()) {sideTabs.addTab("Motifs", motifsPanel);}
            if (showdata.isSelected()) {sideTabs.addTab("Data", dataObjectsPanel);}
            sidePanel.add(sideTabs);
        } else { // show as vertically split panels
            JPanel top=new JPanel();
            Dimension dim=new Dimension(2,29);
            top.setMinimumSize(dim);
            top.setMaximumSize(dim);
            top.setPreferredSize(dim);
            JPanel panelsPanel=new JPanel();
            panelsPanel.setLayout(new BorderLayout());
            int splitLocation1=preferences.getInt("splitLocation1", -1);
            int splitLocation2=preferences.getInt("splitLocation2", -1);
            int count=0;
            JPanel[] panels=new JPanel[3];
            if (showfeatures.isSelected()) {panels[count]=featuresPanel; count++;}
            if (showmotifs.isSelected()) {panels[count]=motifsPanel; count++;}
            if (showdata.isSelected()) {panels[count]=dataObjectsPanel; count++;}
            if (count==1) {
               panelsPanel.add(panels[0]);
            } else if (count==2) {
               JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
               split.setTopComponent(panels[0]);
               split.setBottomComponent(panels[1]);
               panelsPanel.add(split);
               split.setDividerLocation(splitLocation1);
            } else if (count==3) {
               JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
               JSplitPane split2=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
               split.setTopComponent(panels[0]);
               split.setBottomComponent(split2);
               split2.setTopComponent(panels[1]);
               split2.setBottomComponent(panels[2]);
               split.setDividerLocation(splitLocation1);
               split2.setDividerLocation(splitLocation2);
               panelsPanel.add(split);
            }
            sidePanel.add(top,BorderLayout.NORTH);
            sidePanel.add(panelsPanel,BorderLayout.CENTER);
        }
        storeSidePanelSettings();
        sidePanel.revalidate();
        redraw();
    }


    /**
     * This method should be called when the system exits.
     * It is called by MotifLabApp on shutdown
     */
    public void cleanup() {
        // most of the cleanup will be performed in a background worker thread to avoid freezing up the GUI
        // if the cleanup takes a long time (this way it will appear that the application exists sooner)
        logMessage("MotifLab is shutting down. Please wait!");
        // check if the session should be saved
        String sessionFilename=engine.getMotifLabDirectory()+File.separator+autosavedsessionfilename;
        if (!saveSessionOnExit) { // this flag is set to TRUE if the user selected "Save session and exit" from the FILE menu. If we are exiting in the normal way, we should consult the configured settings
            if (autoSaveSessionSetting.equalsIgnoreCase("Always")) {
                saveSessionOnExit=true;
                try {engine.storeSystemObject(sessionFilename, "lastSavedSessionName");} catch (Exception e) {}
            } else if (autoSaveSessionSetting.equalsIgnoreCase("Ask")) {
                if (engine.isDataStorageEmpty()) saveSessionOnExit=false;
                else {
                    int option=JOptionPane.showConfirmDialog(getFrame(),"Would you like to save the current session\nand restore it automatically next time MotifLab is started","Save session?",JOptionPane.YES_NO_OPTION);
                    saveSessionOnExit=(option==JOptionPane.OK_OPTION);
                }
                try {engine.storeSystemObject((saveSessionOnExit)?sessionFilename:null, "lastSavedSessionName");} catch (Exception e) {}
            } else if (autoSaveSessionSetting.equalsIgnoreCase("Ask for File")) {
                if (engine.isDataStorageEmpty()) saveSessionOnExit=false;
                else {
                    int option=JOptionPane.showConfirmDialog(getFrame(),"Would you like to save the current session to file\nand restore it automatically next time MotifLab is started","Save session to file?",JOptionPane.YES_NO_OPTION);
                    saveSessionOnExit=(option==JOptionPane.OK_OPTION);
                }
                if (saveSessionOnExit) {
                    boolean done=false;
                    while(!done) {
                        File file=null;
                        File parentDir=getLastUsedDirectory();
                        final JFileChooser fc = getFileChooser(parentDir);
                        fc.setDialogTitle("Save Session");
                        FileNameExtensionFilter sessionFilter=new FileNameExtensionFilter("MotifLab Session (*.mls)", "mls");
                        fc.addChoosableFileFilter(sessionFilter);
                        fc.setFileFilter(sessionFilter);
                        File preselected=MotifLabEngine.getFile(parentDir,"MotifLabSession.mls");
                        fc.setSelectedFile(preselected);
                        int returnValue=fc.showSaveDialog(getFrame());
                        if (returnValue==JFileChooser.APPROVE_OPTION) {
                            file=fc.getSelectedFile();
                        } else {
                            // User did not select a file. Just save to hidden system file to avoid loosing data
                            break;
                        }
                        if (file.exists()) {
                            int choice=JOptionPane.showConfirmDialog(getFrame(), "Overwrite existing file \""+file.getName()+"\" ?","Save Session",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
                            if (choice==JOptionPane.OK_OPTION) {
                                done=true;
                            } // if (choice!=JOptionPane.OK_OPTION) the user will be prompted for a different filename
                        } else done=true;
                        if (done) sessionFilename=file.getAbsolutePath();
                    } // end of while(!done)
                    try {engine.storeSystemObject(sessionFilename, "lastSavedSessionName");} catch (Exception e) {} // if the user "aborted" the sessionFilename will be the name of the hidden system file
                }
            }
        }
        deleteHiddenSessionFile();
        if (saveSessionOnExit) {
            logMessage("Saving current session...");
            try { // the session will be saved on the EDT and not in a background thread to make absolutely sure that it completes before shutdown
                MotifLabGUI.this.setProgress(Integer.MAX_VALUE);
                progressBar.paintImmediately(progressBar.getBounds());
                getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                saveSession(sessionFilename);
            } catch (Exception e) {
                logMessage(e.toString());
            }
        }
        if (favorites.isDirty()) try {favorites.saveUpdates();} catch (Exception e) {}
        LocalStorage localStorage=getApplication().getContext().getLocalStorage();
        try {
           localStorage.save(recentsessions, recentsessionsfilename);
           localStorage.save(lastUsedDirectory.getAbsolutePath(), lastusedlocationfilename);
        } catch (Exception e) {}
        storeSidePanelSettings();
        SwingWorker worker=new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                if (favorites.isDirty()) try {favorites.saveUpdates();} catch (Exception e) {}
                scheduler.shutdown(true);
                protocolEditor.shutdown();
                return null;
            }
        };
        worker.execute();
    }

    private void deleteHiddenSessionFile() {
        String filename=engine.getMotifLabDirectory()+File.separator+autosavedsessionfilename;
        File file=new File(filename);
        file.delete(); // delete previous auto-saved session
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
     * Returns an a reference to the ProgressMonitor in the GUI
     * as a PropertyChangeListener
     */
    public PropertyChangeListener getProgressMonitor() {
        return progressMonitor;
    }

    /**
     * Returns an instance of the top-level panel in the GUI (area between the menu and the statusbar)
     * @return the top level graphics panel
     */
    public final JPanel getTopPanel() {
        return topPanel;
    }

    /**
     * Returns an instance of the main panel in the GUI (the tabbed pane that shows Visualization, Protocols and outputs)
     * @return the main graphics panel
     */
    public final MainPanel getMainWindow() {
        return mainWindow;
    }

    /**
     * Returns the current ProtocolEditor
     * @return the current ProtocolEditor
     */
    public final ProtocolEditor getProtocolEditor() {
        return protocolEditor;
    }

    /** Returns the Motif Panel*/
    public final MotifsPanel getMotifsPanel() {
        return motifsPanel;
    }

    /**
     * Returns an instance of the VisualizationSettings used by this GUI
     * @return the VisualizationSettings
     */
    @Override
    public final VisualizationSettings getVisualizationSettings() {
        return visualizationSettings;
    }

    public DocumentListener getUndoRedoDocumentListener() {
        if (undoRedoDocumentListener==null) undoRedoDocumentListener=new UndoRedoDocumentListener();
        return undoRedoDocumentListener;
    }

    /**
     * Fetches the ListModel underlying the FeaturesPanel (containing Feature Datasets) and returns it
     * @return the ListModel for the DataPanel
     */
    public FeaturesPanelListModel getFeaturesPanelListModel() {
        if (featuresPanel==null || featuresPanel.getFeaturesList()==null) return null;
        return (FeaturesPanelListModel)featuresPanel.getFeaturesList().getModel();
    }

  /**
   * Given an list containing names of feature dataset, this method will return these names
   * (or a subset of them) according to the order they appear in the FeaturesPanel
   * @param list A list of names of feature datasets
   * @param skiphidden If TRUE only currently visible feature datasets will be included in the resulting list
   * @param aggregated If TRUE the given list of feature names can represent aggregated clusters where tracks with similar prefix and ending in _X (where X is some number) will be represented by the prefix.
   * @return A list of names of feature datasets in sorted order
   */
  public ArrayList<String> sortFeaturesAccordingToPanelOrder(ArrayList<String> list, boolean skiphidden, boolean aggregated) {
      if (featuresPanel==null || featuresPanel.getFeaturesList()==null) return new ArrayList<String>();
      else return featuresPanel.sortFeaturesAccordingToPanelOrder(list, skiphidden, aggregated);
  }

      /**
     * Returns the FeaturesPanel (containing feature dataset tracks)
     * @return FeaturesPanel
     */
    public FeaturesPanel getFeaturesPanel() {
        return featuresPanel;
    }

    /**
     * Returns the DataObjectsPanel (containing non-feature data)
     * @return DataObjectsPanel
     */
    public DataObjectsPanel getDataObjectsPanel() {
        return dataObjectsPanel;
    }

    /**
     * Fetches the ListModel underlying the DataObjectsPanel (containing non-feature data) and returns it
     * @return the ListModel for the DataObjectsPanel
     */
    public DataObjectsPanelListModel getDataObjectsPanelListModel() {
        if (dataObjectsPanel==null || dataObjectsPanel.getDataObjectsList()==null) return null;
        return (DataObjectsPanelListModel)dataObjectsPanel.getDataObjectsList().getModel();
    }
    /**
     * Sets the name of the currently selected GUI tool (usually selected by pressing a radio-button on the toolbar)
     * @param toolname The name of the current tool. This class defines useful constants such as SELECTION_TOOL, MOVE_TOOL and ZOOM_TOOL
     */
    public void setSelectedTool(String toolname) {
        selectedToolName=toolname;
        //debugMessage("Selected "+toolname);
    }

    /**
     * Returns the name of the currently selected GUI tool (usually selected by pressing a radio-button on the toolbar)
     * @return The name of the currently selected GUI tool. This class defines useful constants such as SELECTION_TOOL, MOVE_TOOL and ZOOM_TOOL
     */
    public String getSelectedTool() {
        return selectedToolName;
    }

    /**
     * Sets the 'autosave session on exit' setting.
     * The setting should be set to either "Never", "Always" or "Ask".
     *
     */
    public void setAutoSaveSessionSetting(String setting) {
        autoSaveSessionSetting=setting;
    }

    /**
     * Sets the 'logger level', updates the icon in the GUI.
     * The setting can be stored persistently in the system directory
     *
     */
    public void setLoggerLevel(int level, int stackDump, boolean store) {
        if (level<0) level=1;
        if (level>4) level=4;
        if (stackDump<0) stackDump=0;
        if (stackDump>2) stackDump=2;
        loggerLevel=level;
        stackDumpLevel=stackDump;
        MiscIcons icon=new MiscIcons(MiscIcons.LOGGER_LEVEL);
        icon.setProperty((Integer)loggerLevel);
        if (stackDumpLevel==0) icon.setForegroundColor(new Color(0,210,0));
        else if (stackDumpLevel==1) icon.setForegroundColor(new Color(230,180,0));
        else if (stackDumpLevel==2) icon.setForegroundColor(new Color(210,0,0));
        logLevelIcon.setIcon(icon);
        logLevelIcon.repaint();
        if (store) {
           String string=loggerLevel+":"+stackDumpLevel;
           try {engine.storeSystemObject(string, "loggerLevel.ser");} catch (Exception e) {}
        }
        logLevelIcon.setToolTipText(getLoggerIconToolTipText());
    }

    private void incrementLogLevel() {
        int newLevel=loggerLevel;
        int newStack=stackDumpLevel;
        if (newLevel<4) newLevel++;
        else {
            newLevel=1;
            if (newStack<2) newStack++;
            else newStack=0;
        }
        setLoggerLevel(newLevel, newStack, true);
    }

    private String getLoggerIconToolTipText() {
        String logging="";
        String debugging="";
        switch (loggerLevel) {
            case 1: logging="Low";break;
            case 2: logging="Regular";break;
            case 3: logging="High";break;
            case 4: logging="Very High";break;
        }
        switch (stackDumpLevel) {
            case 0: debugging="None";break;
            case 1: debugging="Low";break;
            case 2: debugging="High";break;
        }
        StringBuilder builder=new StringBuilder();
        builder.append("<html>Logging level: ");
        builder.append(logging);
        builder.append("<br>Debugging: ");
        builder.append(debugging);
        builder.append("<br><br><font size=-2>Click to change</font></html>");
        return builder.toString();
    }

    /**
     * Sets the 'Prompt before discard' flag. If this flag is TRUE the GUI will always prompt the user
     * for confirmation before discarding data that is not saved
     */
    public void setPromptBeforeDiscard(boolean doPrompt) {
        promptBeforeDiscard=doPrompt;
    }

    /**
     * If this method returns TRUE the GUI should always prompt the user
     * for confirmation before discarding data that is not saved
     */
    public boolean doPromptBeforeDiscard() {
        return promptBeforeDiscard;
    }



    /** Adds the "Show base cursor" menu item to the View menu*/
    private void setupBaseCursorMenuItem() {
        Object value=visualizationSettings.getPersistentSetting("showBaseCursor");
        boolean show=false; // default: don't show the base cursor (used to be TRUE before)
        if (value instanceof Boolean) show=(Boolean)value;
        SequenceVisualizer.setVisualizeBaseCursor(show);
        final JCheckBoxMenuItem item=new JCheckBoxMenuItem("Show Base Cursor");
        item.setToolTipText("Draw a box around the base position currently pointed at by the mouse");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean show=item.isSelected();
                SequenceVisualizer.setVisualizeBaseCursor(show);
                visualizationSettings.storePersistentSetting("showBaseCursor", show);
            }
        });
        item.setSelected(show);
        viewMenu.add(item);
    }

    /** Adds the "Show condensed" menu item to the View menu*/
    private void setupCondensedModeMenuItem() {
        boolean useCondensedmode=SequenceVisualizer.getCondensedMode();
        final JCheckBoxMenuItem item=new JCheckBoxMenuItem("Show Condensed");
        item.setToolTipText("Show the sequences in a condensed mode to fit more sequences in one screen");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SequenceVisualizer.setCondensedMode(item.isSelected());
                getVisualizationPanel().condensedModeChanged(item.isSelected()); // revalidates and repaints
            }
        });
        item.setSelected(useCondensedmode);
        viewMenu.add(item);
    }



    /** Adds the "Show Track Names" menu item to the View menu*/
    private void setupShowTrackNamesMenuItem() {
        JMenu trackLabelsSubmenu = new JMenu("Track Labels");
        int current_style = (Integer)visualizationSettings.getSettingAsType(VisualizationSettings.TRACK_LABEL_STYLE, 2);  
        int current_size  = (Integer)visualizationSettings.getSettingAsType(VisualizationSettings.TRACK_LABEL_SIZE, 10);  
        double current_align= (Double)visualizationSettings.getSettingAsType(VisualizationSettings.TRACK_LABEL_ALIGNMENT, 0.0);              
        ActionListener trackLabelsListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                 String cmd=e.getActionCommand();
                 String[] parts=cmd.split(":");
                 if (parts[0].equals("style")) {
                     Integer style=(Integer)MotifLabEngine.getBasicValueForStringAsObject(parts[1]);
                     visualizationSettings.storeSetting(VisualizationSettings.TRACK_LABEL_STYLE, style);    
                 } else if (parts[0].equals("size")) {
                     Integer size=(Integer)MotifLabEngine.getBasicValueForStringAsObject(parts[1]);
                     visualizationSettings.storeSetting(VisualizationSettings.TRACK_LABEL_SIZE, size);  
                 } else if (parts[0].equals("align")) {
                     Double align=(Double)MotifLabEngine.getBasicValueForStringAsObject(parts[1]);
                     visualizationSettings.storeSetting(VisualizationSettings.TRACK_LABEL_ALIGNMENT, align);
                 }
                 redraw();
             };
        };        
        JCheckBoxMenuItem styleNone=new JCheckBoxMenuItem("None",current_style<0);        
        JCheckBoxMenuItem style0=new JCheckBoxMenuItem("Black on White",current_style==0);
        JCheckBoxMenuItem style1=new JCheckBoxMenuItem("Color on White",current_style==1);
        JCheckBoxMenuItem style2=new JCheckBoxMenuItem("Black on Color",current_style==2);
        JCheckBoxMenuItem style3=new JCheckBoxMenuItem("White on Black",current_style==3); 
        JCheckBoxMenuItem style4=new JCheckBoxMenuItem("Color on Black",current_style==4);      
        JCheckBoxMenuItem style5=new JCheckBoxMenuItem("Just Text",current_style==5);  
        styleNone.setActionCommand("style:-1");        
        style0.setActionCommand("style:0");
        style1.setActionCommand("style:1");
        style2.setActionCommand("style:2");
        style3.setActionCommand("style:3");
        style4.setActionCommand("style:4");
        style5.setActionCommand("style:5");
        styleNone.addActionListener(trackLabelsListener);
        style0.addActionListener(trackLabelsListener);
        style1.addActionListener(trackLabelsListener);
        style2.addActionListener(trackLabelsListener);
        style3.addActionListener(trackLabelsListener);
        style4.addActionListener(trackLabelsListener);
        style5.addActionListener(trackLabelsListener);
        trackLabelsSubmenu.add(styleNone); 
        trackLabelsSubmenu.add(style2);    
        trackLabelsSubmenu.add(style0);        
        trackLabelsSubmenu.add(style3); // this has been rearranged            
        trackLabelsSubmenu.add(style1);                       
        trackLabelsSubmenu.add(style4);        
        trackLabelsSubmenu.add(style5);
        ButtonGroup styleGroup=new ButtonGroup();     
        styleGroup.add(styleNone);          
        styleGroup.add(style0);        
        styleGroup.add(style1);        
        styleGroup.add(style2);        
        styleGroup.add(style3);        
        styleGroup.add(style4);        
        styleGroup.add(style5);        
        trackLabelsSubmenu.add(new JSeparator());
        JCheckBoxMenuItem alignLeft=new JCheckBoxMenuItem("Align Left",current_align==0.0);
        JCheckBoxMenuItem alignMiddle=new JCheckBoxMenuItem("Align Middle",current_align==0.5);
        JCheckBoxMenuItem alignRight=new JCheckBoxMenuItem("Align Right",current_align==1.0);        
        alignLeft.setActionCommand("align:0.0");
        alignMiddle.setActionCommand("align:0.5");
        alignRight.setActionCommand("align:1.0");        
        alignLeft.addActionListener(trackLabelsListener);
        alignMiddle.addActionListener(trackLabelsListener);
        alignRight.addActionListener(trackLabelsListener);
        trackLabelsSubmenu.add(alignLeft);        
        trackLabelsSubmenu.add(alignMiddle);        
        trackLabelsSubmenu.add(alignRight);  
        ButtonGroup alignGroup=new ButtonGroup();
        alignGroup.add(alignLeft);        
        alignGroup.add(alignMiddle);        
        alignGroup.add(alignRight);          
        trackLabelsSubmenu.add(new JSeparator());        
        //JCheckBoxMenuItem sizeSmall=new JCheckBoxMenuItem("Small",current_size==8);
        JCheckBoxMenuItem sizeMedium=new JCheckBoxMenuItem("Small",current_size==10);
        JCheckBoxMenuItem sizeLarge=new JCheckBoxMenuItem("Large",current_size==12);        
        //sizeSmall.setActionCommand("size:8");
        sizeMedium.setActionCommand("size:10");
        sizeLarge.setActionCommand("size:12");        
        //sizeSmall.addActionListener(trackLabelsListener);
        sizeMedium.addActionListener(trackLabelsListener);
        sizeLarge.addActionListener(trackLabelsListener);
        //trackLabelsSubmenu.add(sizeSmall);        
        trackLabelsSubmenu.add(sizeMedium);        
        trackLabelsSubmenu.add(sizeLarge);           
        ButtonGroup sizeGroup=new ButtonGroup();     
        //sizeGroup.add(sizeSmall);        
        sizeGroup.add(sizeMedium);        
        sizeGroup.add(sizeLarge);          
        int index=-1;
        for (int i=0;i<viewMenu.getMenuComponentCount();i++) {
            Component comp=viewMenu.getMenuComponent(i);
            if (comp instanceof JMenu && ((JMenu)comp).getText().equals("Track Labels")) {index=i;break;}
        }
        if (index>=0) viewMenu.remove(index);
        viewMenu.add(trackLabelsSubmenu,index);        
    }






    /**
     * A call to this method forces the GUI to be redrawn
     * This is usually called after visualization settings have been altered
     */
    public void redraw() {
       if (SwingUtilities.isEventDispatchThread()) {
         doredraw();
       } else {
           Runnable runner = new Runnable() {
               @Override
               public void run() {
                  doredraw();
               }
           };
           SwingUtilities.invokeLater(runner);
       }
    }

    private void doredraw() {
       if (topPanel!=null) topPanel.repaint();
       if (redrawlisteners!=null) {
           for (RedrawListener listener:redrawlisteners) {
               listener.redrawEvent();
           }
       }
       // redraw dialogs also
       Window[] windows=Window.getWindows();
       for (Window w:windows) {
           if (w instanceof JDialog) ((JDialog)w).repaint();
       }
    }

    /**
     * A call to this method launches the modal operations dialog where the
     * user can choose parameters for an operation
     */
    public void launchOperationEditor(OperationTask operationtask) {
        String opstring=null;
        String sourceDatastring=null;
        String targetDatastring=null;
        Object operation=operationtask.getParameter(OperationTask.OPERATION);
        Object source=operationtask.getParameter(OperationTask.SOURCE);
        Object target=operationtask.getParameter(OperationTask.TARGET);
        opstring=operationtask.getOperationName();
        if (source!=null) sourceDatastring=((Data)source).getName();
        if (target!=null) targetDatastring=((Data)target).getName();
        if (source!=null) operationtask.setParameter(OperationTask.SOURCE_NAME,((Data)source).getName());
        if (target!=null) operationtask.setParameter(OperationTask.TARGET_NAME,((Data)target).getName());
        operationtask.setParameter(OperationTask.ENGINE, getEngine());
        //logMessage("Launcing Operations Editor: Operation="+opstring+", source="+sourceDatastring+", target="+targetDatastring);
        ArrayList<SelectionWindow>selections=visualizationSettings.getSelectionWindows();
        if (selections!=null && ((Operation)operation).isSubrangeApplicable()) {
            Condition_within within=new Condition_within(selections);
            operationtask.setParameter("within", within);
        }
        if (source!=null && !((Operation)operation).canUseAsSource((Data)source)) {
            String message=sourceDatastring+" is not a valid datatype for operation "+opstring;
            JOptionPane.showMessageDialog(this.getFrame(), message,"Operations editor", JOptionPane.ERROR_MESSAGE);
            return;
        } else {
            try {
               DataTypeTable datatypetable=new DataTypeTable(engine);
               HashMap<String,Class> affectedByTasks=scheduler.getDataObjectsAffectedBySubmittedTasks();
               datatypetable.populateFromEngine();
               datatypetable.register(affectedByTasks);
               Class dialogclass = Class.forName("motiflab.gui.operationdialog.OperationDialog_"+opstring);
               OperationDialog dialog=(OperationDialog)dialogclass.newInstance();
               dialog.initialize(operationtask,datatypetable,this);
               dialog.setLocation(getFrame().getWidth()/2-dialog.getWidth()/2, getFrame().getHeight()/2-dialog.getHeight()/2);
               dialog.setVisible(true);
               boolean okpressed=dialog.okPressed();
               dialog.dispose();
               if (okpressed) launchOperationTask(operationtask,isRecording());

            } catch (Exception e) {
                 JOptionPane.showMessageDialog(this.getFrame(), e.getClass().toString()+"\n"+e.getMessage(),"Operations editor" ,JOptionPane.ERROR_MESSAGE);
                 if (!(e instanceof ExecutionError || e instanceof ParseError || e instanceof SystemError)) {
                     engine.reportError(e);
                     e.printStackTrace(System.err);
                 }
            }

        }
  }

    /**
     * Launches an OperationTask as an undoable event and makes a record in the protocol script (if in record mode)
     */
    public void launchOperationTask(ExecutableTask task, boolean recordInProtocol) {
        if (task instanceof OperationTask) {
            reserveDataName(((OperationTask)task).getReserveDataNames());
        }
        UndoMonitor monitor=undoManager.getUndoMonitor(task.getPresentationName());
        task.setUndoMonitor(monitor);
        RecordingCompoundEdit protocolEdit=new RecordingCompoundEdit();
        undoManager.forwardUndoEvents(protocolEdit);

        if (recordInProtocol) {
           int pos=protocolEditor.getCaretPosition();
           protocolEditor.insertOperationAt(task,pos);
        }

        protocolEdit.end();
        undoManager.forwardUndoEvents(null);
        task.setProtocolEdit(protocolEdit);
        undoManager.addEdit(task);
        task.addPropertyChangeListener(new singleTaskListener());
        scheduler.submit(task);
    }

    /**
     * Launches an AddSequencesTask as an undoable event
     */
    public void launchAddSequencesTask(AddSequencesTask task, boolean recordInProtocol) {
        if (task.getNumberofSequencesToAdd()==0) return; // no sequences specified
        UndoMonitor monitor=undoManager.getUndoMonitor(task.getPresentationName());
        task.setUndoMonitor(monitor);
        if (recordInProtocol) {
            RecordingCompoundEdit protocolEdit=new RecordingCompoundEdit();
            undoManager.forwardUndoEvents(protocolEdit);
            int pos=protocolEditor.getCaretPosition();
            protocolEditor.insertOperationAt(task,pos);
            protocolEdit.end();
            undoManager.forwardUndoEvents(null);
            task.setProtocolEdit(protocolEdit);
        }
        undoManager.addEdit(task);
        scheduler.submit(task);
    }

    /**
     * Launches an ExecutableTask (which is not undoable and not registered to protocol)
     * If the task is an OperationTask or AddSequencesTask it would be better
     * to use the other launch methods available
     */
    public void launchTask(ExecutableTask task) {
        scheduler.submit(task);
    }

    /**
     * Launches an undoable ExecutableTask (nothing is recorded in the protocol!)
     */
    public void launchUndoableTask(ExecutableTask task) {
        UndoMonitor monitor=undoManager.getUndoMonitor(task.getPresentationName());
        task.setUndoMonitor(monitor);
        undoManager.addEdit(task);
        scheduler.submit(task);
    }


    /**
     * Returns a reference to the GUIUndoManager used by this GUI
     * @return
     */
    public GUIUndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Returns a reference to the Scheduler used by this gui
     * @return
     */
    public GuiScheduler getScheduler() {
        return scheduler;
    }



    /**
     * This method returns the VisualizationPanel
     */
    public VisualizationPanel getVisualizationPanel() {
        return visualizationPanel;
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = MotifLabApp.getApplication().getMainFrame();
            aboutBox = new MotifLabAboutBox(MotifLabGUI.this);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        MotifLabApp.getApplication().show(aboutBox);
    }

    /** Clears the text in the log-panel */
    private void clearLog() {
        logPanel.setText("");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        Toolbar = new javax.swing.JToolBar();
        newButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        printButton = new javax.swing.JButton();
        toolbarSeparator1 = new javax.swing.JToolBar.Separator();
        cutbutton = new javax.swing.JButton();
        copybutton = new javax.swing.JButton();
        pasteButton = new javax.swing.JButton();
        undoButton = new javax.swing.JButton();
        redoButton = new javax.swing.JButton();
        toolbarSeparator2 = new javax.swing.JToolBar.Separator();
        sequenceButton = new javax.swing.JButton();
        databaseButton = new javax.swing.JButton();
        toolbarSeparator3 = new javax.swing.JToolBar.Separator();
        selectionToolButton = new javax.swing.JToggleButton();
        dragToolButton = new javax.swing.JToggleButton();
        drawToolButton = new javax.swing.JToggleButton();
        zoomToolButton = new javax.swing.JToggleButton();
        zoomLevelCombobox = new javax.swing.JComboBox();
        toolbarSeparator4 = new javax.swing.JToolBar.Separator();
        showSequenceBrowserButton = new javax.swing.JButton();
        showMotifBrowserButton = new javax.swing.JButton();
        showModuleBrowserButton = new javax.swing.JButton();
        toolbarSeparator5 = new javax.swing.JToolBar.Separator();
        recordButton = new javax.swing.JToggleButton();
        executeButton = new javax.swing.JToggleButton();
        stopButton = new javax.swing.JToggleButton();
        toolbarSeparator6 = new javax.swing.JToolBar.Separator();
        toolbarSearchPanel = new javax.swing.JPanel();
        centerArea = new javax.swing.JPanel();
        horisontalSplitPane = new javax.swing.JSplitPane();
        verticalSplitPane = new javax.swing.JSplitPane();
        sidePanel = new javax.swing.JPanel();
        logTopPanel = new javax.swing.JPanel();
        logLeftPanel = new javax.swing.JPanel();
        logRightPanel = new javax.swing.JPanel();
        logLevelIcon = new javax.swing.JLabel();
        clearLogLabel = new javax.swing.JLabel();
        logScrollPane = new javax.swing.JScrollPane();
        logPanel = new javax.swing.JTextPane();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        fileNewMenuItem = new javax.swing.JMenuItem();
        fileOpenMenuItem = new javax.swing.JMenuItem();
        fileOpenFromURLMenuItem = new javax.swing.JMenuItem();
        fileOpenRecentSubmenu = new javax.swing.JMenu();
        fileCloseMenuItem = new javax.swing.JMenuItem();
        fileMenuSeparator1 = new javax.swing.JSeparator();
        fileSaveMenuItem = new javax.swing.JMenuItem();
        fileSaveAsMenuItem = new javax.swing.JMenuItem();
        fileSaveAllMenuItem = new javax.swing.JMenuItem();
        fileMenuSeparator2 = new javax.swing.JSeparator();
        fileSaveSessionMenuItem = new javax.swing.JMenuItem();
        fileRestoreSessionMenuItem = new javax.swing.JMenuItem();
        fileRestoreSessionFromURLMenuItem = new javax.swing.JMenuItem();
        fileRestoreRecentSessionSubmenu = new javax.swing.JMenu();
        fileMenuSeparator3 = new javax.swing.JSeparator();
        filePrintMenuItem = new javax.swing.JMenuItem();
        fileSaveAsImageMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        fileMenuSeparator4 = new javax.swing.JSeparator();
        fileSaveAndExitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem fileExitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        editMenuSeparator1 = new javax.swing.JSeparator();
        undoMenuItem = new javax.swing.JMenuItem();
        redoMenuItem = new javax.swing.JMenuItem();
        editMenuSeparator2 = new javax.swing.JPopupMenu.Separator();
        findMenuItem = new javax.swing.JMenuItem();
        replaceMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        dataMenu = new javax.swing.JMenu();
        addSequencesMenuItem = new javax.swing.JMenuItem();
        addSequencesFromFileMenuItem = new javax.swing.JMenuItem();
        addDataTracksMenuItem = new javax.swing.JMenuItem();
        addNewDataItemMenu = new javax.swing.JMenu();
        addBackgroundMenuItem = new javax.swing.JMenuItem();
        addExpressionProfileMenuItem = new javax.swing.JMenuItem();
        addMotifMenuItem = new javax.swing.JMenuItem();
        addMotifCollectionMenuItem = new javax.swing.JMenuItem();
        addMotifPartitionMenuItem = new javax.swing.JMenuItem();
        addMotifMapMenuItem = new javax.swing.JMenuItem();
        addBackgroundFrequenciesMenuItem = new javax.swing.JMenuItem();
        addModuleMenuItem = new javax.swing.JMenuItem();
        addModuleCollectionMenuItem = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        addModuleMapMenuItem = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        addNumericVariableMenuItem = new javax.swing.JMenuItem();
        addPriorsGeneratorMenuItem = new javax.swing.JMenuItem();
        addSequenceCollectionMenuItem = new javax.swing.JMenuItem();
        addSequencePartitionMenuItem = new javax.swing.JMenuItem();
        addSequenceMapMenuItem = new javax.swing.JMenuItem();
        addNumericMapVariableMenuItem = new javax.swing.JMenuItem();
        addTextVariableMenuItem = new javax.swing.JMenuItem();
        importFromFileMenuItem = new javax.swing.JMenuItem();
        favoritesMenuItem = new javax.swing.JMenuItem();
        dataMenuSeparator1 = new javax.swing.JSeparator();
        clearDataSubmenu = new javax.swing.JMenu();
        clearSequencesAndFeaturesMenuItem = new javax.swing.JMenuItem();
        clearFeatureDataMenuItem = new javax.swing.JMenuItem();
        clearMotifsAndModulesMenuItem = new javax.swing.JMenuItem();
        clearModulesMenuItem = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        clearOtherDataMenuItem = new javax.swing.JMenuItem();
        clearAllDataMenuItem = new javax.swing.JMenuItem();
        operationsMenu = new javax.swing.JMenu();
        protocolMenu = new javax.swing.JMenu();
        recordMenuItem = new javax.swing.JMenuItem();
        executeMenuItem = new javax.swing.JMenuItem();
        stopMenuItem = new javax.swing.JMenuItem();
        protocolMenuSeparator1 = new javax.swing.JSeparator();
        executeCurrentLineMenuItem = new javax.swing.JMenuItem();
        executeFromCurrentLineMenuItem = new javax.swing.JMenuItem();
        executeCurrentSelectionMenuItem = new javax.swing.JMenuItem();
        commentSelectedLines = new javax.swing.JMenuItem();
        uncommentSelectedLines = new javax.swing.JMenuItem();
        colorKeywordsMenuItem = new javax.swing.JCheckBoxMenuItem();
        protocolMenuSeparator2 = new javax.swing.JPopupMenu.Separator();
        macroEditorMenuItem = new javax.swing.JMenuItem();
        expandMacrosMenuItem = new javax.swing.JMenuItem();
        protocolMenuSeparator3 = new javax.swing.JPopupMenu.Separator();
        changeProtocolSubmenu = new javax.swing.JMenu();
        toolsMenu = new javax.swing.JMenu();
        selectionToolMenuItem = new javax.swing.JMenuItem();
        handToolMenuItem = new javax.swing.JMenuItem();
        drawToolMenuItem = new javax.swing.JMenuItem();
        zoomToolMenuItem = new javax.swing.JMenuItem();
        toolsMenuSeparator1 = new javax.swing.JSeparator();
        motifBrowserMenuItem = new javax.swing.JMenuItem();
        moduleBrowserMenuItem = new javax.swing.JMenuItem();
        sequenceBrowserMenuItem = new javax.swing.JMenuItem();
        regionBrowserMenuItem = new javax.swing.JMenuItem();
        toolsMenuSeparator2 = new javax.swing.JPopupMenu.Separator();
        interactionFilterMenuItem = new javax.swing.JMenuItem();
        PositionalDistributionViewerMenuItem = new javax.swing.JMenuItem();
        motifScoreFilterMenuItem = new javax.swing.JMenuItem();
        toolsMenuSeparator3 = new javax.swing.JPopupMenu.Separator();
        cropSequencesMenuItem = new javax.swing.JMenuItem();
        extendSequencesMenuItem = new javax.swing.JMenuItem();
        sortSequencesToolMenuItem = new javax.swing.JMenuItem();
        updateMotifsMenuItem = new javax.swing.JMenuItem();
        configureMenu = new javax.swing.JMenu();
        configureDatatracksMenuItem = new javax.swing.JMenuItem();
        configureOrganismsAndIdentifiersMenuItem = new javax.swing.JMenuItem();
        externalProgramMenuItem = new javax.swing.JMenuItem();
        datarepositoriesMenuItem = new javax.swing.JMenuItem();
        pluginsMenuItem = new javax.swing.JMenuItem();
        optionsMenuItem = new javax.swing.JMenuItem();
        configureMenuSeparator1 = new javax.swing.JPopupMenu.Separator();
        installConfigFileMenuItem = new javax.swing.JMenuItem();
        backupConfigurationMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        welcomeMenuItem = new javax.swing.JMenuItem();
        quickTutorialMenuItem = new javax.swing.JMenuItem();
        keyboardshortcutsMenuItem = new javax.swing.JMenuItem();
        exampleSessionsMenu = new javax.swing.JMenu();
        notificationsMenuItem = new javax.swing.JMenuItem();
        citationsMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        selectedToolButtonGroup = new javax.swing.ButtonGroup();
        recplaystopButtonGroup = new javax.swing.ButtonGroup();

        topPanel.setName("topPanel"); // NOI18N
        topPanel.setLayout(new java.awt.BorderLayout());

        Toolbar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        Toolbar.setFloatable(false);
        Toolbar.setRollover(true);
        Toolbar.setMinimumSize(new java.awt.Dimension(1224, 39));
        Toolbar.setName("Toolbar"); // NOI18N
        Toolbar.setPreferredSize(new java.awt.Dimension(15, 39));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        newButton.setAction(actionMap.get("newProtocol")); // NOI18N
        newButton.setFocusable(false);
        newButton.setHideActionText(true);
        newButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newButton.setName("newButton"); // NOI18N
        newButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(newButton);

        openButton.setAction(actionMap.get("openProtocol")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(MotifLabGUI.class);
        openButton.setText(resourceMap.getString("openButton.text")); // NOI18N
        openButton.setFocusable(false);
        openButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openButton.setName("openButton"); // NOI18N
        openButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(openButton);

        saveButton.setAction(actionMap.get("saveFile")); // NOI18N
        saveButton.setText(resourceMap.getString("saveButton.text")); // NOI18N
        saveButton.setFocusable(false);
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setName("saveButton"); // NOI18N
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(saveButton);

        printButton.setAction(actionMap.get("printActionMethod")); // NOI18N
        printButton.setText(resourceMap.getString("printButton.text")); // NOI18N
        printButton.setFocusable(false);
        printButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        printButton.setName("printButton"); // NOI18N
        printButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(printButton);

        toolbarSeparator1.setName("toolbarSeparator1"); // NOI18N
        Toolbar.add(toolbarSeparator1);

        cutbutton.setAction(getApplication().getContext().getActionMap().get("cut"));
        cutbutton.setIcon(resourceMap.getIcon("cutbutton.icon")); // NOI18N
        cutbutton.setFocusable(false);
        cutbutton.setHideActionText(true);
        cutbutton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cutbutton.setName("cutbutton"); // NOI18N
        cutbutton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(cutbutton);

        copybutton.setAction(getApplication().getContext().getActionMap().get("copy"));
        copybutton.setIcon(resourceMap.getIcon("copybutton.icon")); // NOI18N
        copybutton.setFocusable(false);
        copybutton.setHideActionText(true);
        copybutton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        copybutton.setName("copybutton"); // NOI18N
        copybutton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(copybutton);

        pasteButton.setAction(getApplication().getContext().getActionMap().get("paste"));
        pasteButton.setIcon(resourceMap.getIcon("pasteButton.icon")); // NOI18N
        pasteButton.setFocusable(false);
        pasteButton.setHideActionText(true);
        pasteButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        pasteButton.setName("pasteButton"); // NOI18N
        pasteButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(pasteButton);

        undoButton.setAction(actionMap.get("undo")); // NOI18N
        undoButton.setFocusable(false);
        undoButton.setHideActionText(true);
        undoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        undoButton.setName("undoButton"); // NOI18N
        undoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(undoButton);

        redoButton.setAction(actionMap.get("redo")); // NOI18N
        redoButton.setFocusable(false);
        redoButton.setHideActionText(true);
        redoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        redoButton.setName("redoButton"); // NOI18N
        redoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(redoButton);

        toolbarSeparator2.setName("toolbarSeparator2"); // NOI18N
        Toolbar.add(toolbarSeparator2);

        sequenceButton.setAction(actionMap.get("showSequenceInputDialog")); // NOI18N
        sequenceButton.setText(resourceMap.getString("")); // NOI18N
        sequenceButton.setFocusable(false);
        sequenceButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        sequenceButton.setName("sequenceButton"); // NOI18N
        sequenceButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(sequenceButton);

        databaseButton.setAction(actionMap.get("showDatatrackDialog")); // NOI18N
        databaseButton.setText(resourceMap.getString("")); // NOI18N
        databaseButton.setFocusable(false);
        databaseButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        databaseButton.setName("databaseButton"); // NOI18N
        databaseButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(databaseButton);

        toolbarSeparator3.setName("toolbarSeparator3"); // NOI18N
        Toolbar.add(toolbarSeparator3);

        selectionToolButton.setAction(actionMap.get("selectionToolAction")); // NOI18N
        selectedToolButtonGroup.add(selectionToolButton);
        selectionToolButton.setFocusable(false);
        selectionToolButton.setHideActionText(true);
        selectionToolButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        selectionToolButton.setName("selectionToolButton"); // NOI18N
        selectionToolButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(selectionToolButton);

        dragToolButton.setAction(actionMap.get("handToolAction")); // NOI18N
        selectedToolButtonGroup.add(dragToolButton);
        dragToolButton.setToolTipText(resourceMap.getString("dragToolButton.toolTipText")); // NOI18N
        dragToolButton.setFocusable(false);
        dragToolButton.setHideActionText(true);
        dragToolButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        dragToolButton.setName("dragToolButton"); // NOI18N
        dragToolButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(dragToolButton);

        drawToolButton.setAction(actionMap.get("drawToolAction")); // NOI18N
        selectedToolButtonGroup.add(drawToolButton);
        drawToolButton.setText(resourceMap.getString("drawToolButton.text")); // NOI18N
        drawToolButton.setFocusable(false);
        drawToolButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        drawToolButton.setName("drawToolButton"); // NOI18N
        drawToolButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(drawToolButton);

        zoomToolButton.setAction(actionMap.get("zoomToolAction")); // NOI18N
        selectedToolButtonGroup.add(zoomToolButton);
        zoomToolButton.setFocusable(false);
        zoomToolButton.setHideActionText(true);
        zoomToolButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomToolButton.setName("zoomToolButton"); // NOI18N
        zoomToolButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(zoomToolButton);

        zoomLevelCombobox.setModel(getModelForZoomLevelCombobox());
        zoomLevelCombobox.setFocusable(false);
        zoomLevelCombobox.setName("zoomLevelCombobox"); // NOI18N
        zoomLevelCombobox.setRenderer(new ZoomLevelComboboxCellRenderer());
        Toolbar.add(zoomLevelCombobox);
        zoomLevelCombobox.addActionListener(new ZoomLevelComboboxActionListener());

        toolbarSeparator4.setName("toolbarSeparator4"); // NOI18N
        Toolbar.add(toolbarSeparator4);

        showSequenceBrowserButton.setAction(actionMap.get("showSequenceBrowser")); // NOI18N
        showSequenceBrowserButton.setText(resourceMap.getString("showSequenceBrowserButton.text")); // NOI18N
        showSequenceBrowserButton.setFocusable(false);
        showSequenceBrowserButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showSequenceBrowserButton.setName("showSequenceBrowserButton"); // NOI18N
        showSequenceBrowserButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(showSequenceBrowserButton);

        showMotifBrowserButton.setAction(actionMap.get("showMotifBrowser")); // NOI18N
        showMotifBrowserButton.setText(resourceMap.getString("showMotifBrowserButton.text")); // NOI18N
        showMotifBrowserButton.setFocusable(false);
        showMotifBrowserButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showMotifBrowserButton.setName("showMotifBrowserButton"); // NOI18N
        showMotifBrowserButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(showMotifBrowserButton);

        showModuleBrowserButton.setAction(actionMap.get("showModuleBrowser")); // NOI18N
        showModuleBrowserButton.setText(resourceMap.getString("showModuleBrowserButton.text")); // NOI18N
        showModuleBrowserButton.setFocusable(false);
        showModuleBrowserButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showModuleBrowserButton.setName("showModuleBrowserButton"); // NOI18N
        showModuleBrowserButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(showModuleBrowserButton);

        toolbarSeparator5.setName("toolbarSeparator5"); // NOI18N
        Toolbar.add(toolbarSeparator5);

        recordButton.setAction(actionMap.get("recordMode")); // NOI18N
        recplaystopButtonGroup.add(recordButton);
        recordButton.setFocusPainted(false);
        recordButton.setFocusable(false);
        recordButton.setHideActionText(true);
        recordButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        recordButton.setName("recordButton"); // NOI18N
        recordButton.setRolloverEnabled(true);
        recordButton.setSelectedIcon(resourceMap.getIcon("recordButton.selectedIcon")); // NOI18N
        recordButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(recordButton);

        executeButton.setAction(actionMap.get("executeProtocol")); // NOI18N
        recplaystopButtonGroup.add(executeButton);
        executeButton.setFocusPainted(false);
        executeButton.setFocusable(false);
        executeButton.setHideActionText(true);
        executeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        executeButton.setName("executeButton"); // NOI18N
        executeButton.setRolloverEnabled(true);
        executeButton.setSelectedIcon(resourceMap.getIcon("executeButton.selectedIcon")); // NOI18N
        executeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(executeButton);

        stopButton.setAction(actionMap.get("stopAction")); // NOI18N
        recplaystopButtonGroup.add(stopButton);
        stopButton.setFocusPainted(false);
        stopButton.setFocusable(false);
        stopButton.setHideActionText(true);
        stopButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        stopButton.setName("stopButton"); // NOI18N
        stopButton.setRolloverEnabled(true);
        stopButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Toolbar.add(stopButton);

        toolbarSeparator6.setName("toolbarSeparator6"); // NOI18N
        Toolbar.add(toolbarSeparator6);

        toolbarSearchPanel.setName("toolbarSearchPanel"); // NOI18N
        toolbarSearchPanel.setLayout(new java.awt.BorderLayout());
        Toolbar.add(toolbarSearchPanel);

        topPanel.add(Toolbar, java.awt.BorderLayout.PAGE_START);

        centerArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));
        centerArea.setName("centerArea"); // NOI18N
        centerArea.setLayout(new java.awt.BorderLayout());

        horisontalSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        horisontalSplitPane.setName("horisontalSplitPane"); // NOI18N
        horisontalSplitPane.setOneTouchExpandable(true);

        verticalSplitPane.setName("verticalSplitPane"); // NOI18N
        verticalSplitPane.setOneTouchExpandable(true);

        sidePanel.setName("sidePanel"); // NOI18N
        sidePanel.setLayout(new java.awt.BorderLayout());
        verticalSplitPane.setLeftComponent(sidePanel);

        horisontalSplitPane.setLeftComponent(verticalSplitPane);

        logTopPanel.setName("logTopPanel"); // NOI18N
        logTopPanel.setPreferredSize(new java.awt.Dimension(70, 52));
        logTopPanel.setLayout(new java.awt.BorderLayout());

        logLeftPanel.setMaximumSize(new java.awt.Dimension(30, 50));
        logLeftPanel.setMinimumSize(new java.awt.Dimension(30, 50));
        logLeftPanel.setName("logLeftPanel"); // NOI18N
        logLeftPanel.setPreferredSize(new java.awt.Dimension(30, 50));

        javax.swing.GroupLayout logLeftPanelLayout = new javax.swing.GroupLayout(logLeftPanel);
        logLeftPanel.setLayout(logLeftPanelLayout);
        logLeftPanelLayout.setHorizontalGroup(
            logLeftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );
        logLeftPanelLayout.setVerticalGroup(
            logLeftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 279, Short.MAX_VALUE)
        );

        logTopPanel.add(logLeftPanel, java.awt.BorderLayout.LINE_START);

        logRightPanel.setMaximumSize(new java.awt.Dimension(26, 50));
        logRightPanel.setMinimumSize(new java.awt.Dimension(26, 50));
        logRightPanel.setName("logRightPanel"); // NOI18N
        logRightPanel.setPreferredSize(new java.awt.Dimension(26, 50));

        logLevelIcon.setIcon(new MiscIcons(MiscIcons.LOGGER_LEVEL));
        logLevelIcon.setText(resourceMap.getString("logLevelIcon.text")); // NOI18N
        logLevelIcon.setToolTipText(getLoggerIconToolTipText());
        logLevelIcon.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        logLevelIcon.setName("logLevelIcon"); // NOI18N
        logRightPanel.add(logLevelIcon);

        clearLogLabel.setIcon(resourceMap.getIcon("clearLogLabel.icon")); // NOI18N
        clearLogLabel.setText(resourceMap.getString("clearLogLabel.text")); // NOI18N
        clearLogLabel.setToolTipText(resourceMap.getString("clearLogLabel.toolTipText")); // NOI18N
        clearLogLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        clearLogLabel.setMaximumSize(new java.awt.Dimension(20, 20));
        clearLogLabel.setMinimumSize(new java.awt.Dimension(20, 20));
        clearLogLabel.setName("clearLogLabel"); // NOI18N
        clearLogLabel.setPreferredSize(new java.awt.Dimension(20, 20));
        clearLogLabel.setRequestFocusEnabled(false);
        logRightPanel.add(clearLogLabel);

        logTopPanel.add(logRightPanel, java.awt.BorderLayout.LINE_END);

        logScrollPane.setName("logScrollPane"); // NOI18N

        logPanel.setEditable(false);
        logPanel.setName("logPanel"); // NOI18N
        logPanel.setPreferredSize(new java.awt.Dimension(6, 30));
        logScrollPane.setViewportView(logPanel);

        logTopPanel.add(logScrollPane, java.awt.BorderLayout.CENTER);

        horisontalSplitPane.setRightComponent(logTopPanel);

        centerArea.add(horisontalSplitPane, java.awt.BorderLayout.CENTER);

        topPanel.add(centerArea, java.awt.BorderLayout.CENTER);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        fileNewMenuItem.setAction(actionMap.get("newProtocol")); // NOI18N
        fileNewMenuItem.setName("fileNewMenuItem"); // NOI18N
        fileMenu.add(fileNewMenuItem);

        fileOpenMenuItem.setAction(actionMap.get("openProtocol")); // NOI18N
        fileOpenMenuItem.setName("fileOpenMenuItem"); // NOI18N
        fileMenu.add(fileOpenMenuItem);

        fileOpenFromURLMenuItem.setAction(actionMap.get("openProtocolFromURL")); // NOI18N
        fileOpenFromURLMenuItem.setText(resourceMap.getString("fileOpenFromURLMenuItem.text")); // NOI18N
        fileOpenFromURLMenuItem.setName("fileOpenFromURLMenuItem"); // NOI18N
        fileMenu.add(fileOpenFromURLMenuItem);

        fileOpenRecentSubmenu.setText(resourceMap.getString("fileOpenRecentSubmenu.text")); // NOI18N
        fileOpenRecentSubmenu.setEnabled(false);
        fileOpenRecentSubmenu.setName("fileOpenRecentSubmenu"); // NOI18N
        fileMenu.add(fileOpenRecentSubmenu);

        fileCloseMenuItem.setAction(actionMap.get("closefileActionMethod")); // NOI18N
        fileCloseMenuItem.setName("fileCloseMenuItem"); // NOI18N
        fileMenu.add(fileCloseMenuItem);

        fileMenuSeparator1.setName("fileMenuSeparator1"); // NOI18N
        fileMenu.add(fileMenuSeparator1);

        fileSaveMenuItem.setAction(actionMap.get("saveFile")); // NOI18N
        fileSaveMenuItem.setName("fileSaveMenuItem"); // NOI18N
        fileMenu.add(fileSaveMenuItem);

        fileSaveAsMenuItem.setAction(actionMap.get("saveAsActionMethod")); // NOI18N
        fileSaveAsMenuItem.setName("fileSaveAsMenuItem"); // NOI18N
        fileMenu.add(fileSaveAsMenuItem);

        fileSaveAllMenuItem.setAction(actionMap.get("saveAllActionMethod")); // NOI18N
        fileSaveAllMenuItem.setName("fileSaveAllMenuItem"); // NOI18N
        fileMenu.add(fileSaveAllMenuItem);

        fileMenuSeparator2.setName("fileMenuSeparator2"); // NOI18N
        fileMenu.add(fileMenuSeparator2);

        fileSaveSessionMenuItem.setAction(actionMap.get("saveSessionActionMethod")); // NOI18N
        fileSaveSessionMenuItem.setName("fileSaveSessionMenuItem"); // NOI18N
        fileMenu.add(fileSaveSessionMenuItem);

        fileRestoreSessionMenuItem.setAction(actionMap.get("restoreSessionActionMethod")); // NOI18N
        fileRestoreSessionMenuItem.setName("fileRestoreSessionMenuItem"); // NOI18N
        fileMenu.add(fileRestoreSessionMenuItem);

        fileRestoreSessionFromURLMenuItem.setAction(actionMap.get("restoreSessionFromURLActionMethod")); // NOI18N
        fileRestoreSessionFromURLMenuItem.setText(resourceMap.getString("fileRestoreSessionFromURLMenuItem.text")); // NOI18N
        fileRestoreSessionFromURLMenuItem.setName("fileRestoreSessionFromURLMenuItem"); // NOI18N
        fileMenu.add(fileRestoreSessionFromURLMenuItem);

        fileRestoreRecentSessionSubmenu.setText(resourceMap.getString("fileRestoreRecentSessionSubmenu.text")); // NOI18N
        fileRestoreRecentSessionSubmenu.setEnabled(false);
        fileRestoreRecentSessionSubmenu.setName("fileRestoreRecentSessionSubmenu"); // NOI18N
        fileMenu.add(fileRestoreRecentSessionSubmenu);

        fileMenuSeparator3.setName("fileMenuSeparator3"); // NOI18N
        fileMenu.add(fileMenuSeparator3);

        filePrintMenuItem.setAction(actionMap.get("printActionMethod")); // NOI18N
        filePrintMenuItem.setName("filePrintMenuItem"); // NOI18N
        fileMenu.add(filePrintMenuItem);

        fileSaveAsImageMenuItem.setAction(actionMap.get("saveAsImageMethod")); // NOI18N
        fileSaveAsImageMenuItem.setName("fileSaveAsImageMenuItem"); // NOI18N
        fileMenu.add(fileSaveAsImageMenuItem);

        jMenuItem1.setAction(actionMap.get("saveAsIndividualImagesMethod")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        fileMenu.add(jMenuItem1);

        fileMenuSeparator4.setName("fileMenuSeparator4"); // NOI18N
        fileMenu.add(fileMenuSeparator4);

        fileSaveAndExitMenuItem.setAction(actionMap.get("saveSessionAndExit")); // NOI18N
        fileSaveAndExitMenuItem.setText(resourceMap.getString("fileSaveAndExitMenuItem.text")); // NOI18N
        fileSaveAndExitMenuItem.setName("fileSaveAndExitMenuItem"); // NOI18N
        fileMenu.add(fileSaveAndExitMenuItem);

        fileExitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        fileExitMenuItem.setName("fileExitMenuItem"); // NOI18N
        fileMenu.add(fileExitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText(resourceMap.getString("editMenu.text")); // NOI18N
        editMenu.setActionCommand(resourceMap.getString("editMenu.actionCommand")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        cutMenuItem.setAction(getApplication().getContext().getActionMap().get("cut"));
        cutMenuItem.setName("cutMenuItem"); // NOI18N
        editMenu.add(cutMenuItem);

        copyMenuItem.setAction(getApplication().getContext().getActionMap().get("copy"));
        copyMenuItem.setName("copyMenuItem"); // NOI18N
        editMenu.add(copyMenuItem);

        pasteMenuItem.setAction(getApplication().getContext().getActionMap().get("paste"));
        pasteMenuItem.setName("pasteMenuItem"); // NOI18N
        editMenu.add(pasteMenuItem);

        editMenuSeparator1.setName("editMenuSeparator1"); // NOI18N
        editMenu.add(editMenuSeparator1);

        undoMenuItem.setAction(actionMap.get("undo")); // NOI18N
        undoMenuItem.setName("undoMenuItem"); // NOI18N
        editMenu.add(undoMenuItem);

        redoMenuItem.setAction(actionMap.get("redo")); // NOI18N
        redoMenuItem.setName("redoMenuItem"); // NOI18N
        editMenu.add(redoMenuItem);

        editMenuSeparator2.setName("editMenuSeparator2"); // NOI18N
        editMenu.add(editMenuSeparator2);

        findMenuItem.setAction(actionMap.get("findAction")); // NOI18N
        findMenuItem.setText(resourceMap.getString("findMenuItem.text")); // NOI18N
        findMenuItem.setName("findMenuItem"); // NOI18N
        editMenu.add(findMenuItem);

        replaceMenuItem.setAction(actionMap.get("replaceAction")); // NOI18N
        replaceMenuItem.setText(resourceMap.getString("replaceMenuItem.text")); // NOI18N
        replaceMenuItem.setName("replaceMenuItem"); // NOI18N
        editMenu.add(replaceMenuItem);

        menuBar.add(editMenu);

        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N
        menuBar.add(viewMenu);

        dataMenu.setText(resourceMap.getString("dataMenu.text")); // NOI18N
        dataMenu.setName("dataMenu"); // NOI18N

        addSequencesMenuItem.setAction(actionMap.get("showSequenceInputDialog")); // NOI18N
        addSequencesMenuItem.setName("addSequencesMenuItem"); // NOI18N
        dataMenu.add(addSequencesMenuItem);

        addSequencesFromFileMenuItem.setAction(actionMap.get("showAddSequencesFromFileDialog")); // NOI18N
        addSequencesFromFileMenuItem.setName("addSequencesFromFileMenuItem"); // NOI18N
        dataMenu.add(addSequencesFromFileMenuItem);

        addDataTracksMenuItem.setAction(actionMap.get("showDatatrackDialog")); // NOI18N
        addDataTracksMenuItem.setName("addDataTracksMenuItem"); // NOI18N
        dataMenu.add(addDataTracksMenuItem);

        addNewDataItemMenu.setText(resourceMap.getString("addNewDataItemMenu.text")); // NOI18N
        addNewDataItemMenu.setName("addNewDataItemMenu"); // NOI18N

        addBackgroundMenuItem.setText(resourceMap.getString("addBackgroundMenuItem.text")); // NOI18N
        addBackgroundMenuItem.setName("addBackgroundMenuItem"); // NOI18N
        addBackgroundMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addBackgroundMenuItem);

        addExpressionProfileMenuItem.setText(resourceMap.getString("addExpressionProfileMenuItem.text")); // NOI18N
        addExpressionProfileMenuItem.setName("addExpressionProfileMenuItem"); // NOI18N
        addExpressionProfileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addExpressionProfileMenuItem);

        addMotifMenuItem.setText(resourceMap.getString("addMotifMenuItem.text")); // NOI18N
        addMotifMenuItem.setName("addMotifMenuItem"); // NOI18N
        addMotifMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addMotifMenuItem);

        addMotifCollectionMenuItem.setText(resourceMap.getString("addMotifCollectionMenuItem.text")); // NOI18N
        addMotifCollectionMenuItem.setName("addMotifCollectionMenuItem"); // NOI18N
        addMotifCollectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addMotifCollectionMenuItem);

        addMotifPartitionMenuItem.setText(resourceMap.getString("addMotifPartitionMenuItem.text")); // NOI18N
        addMotifPartitionMenuItem.setName("addMotifPartitionMenuItem"); // NOI18N
        addMotifPartitionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addMotifPartitionMenuItem);

        addMotifMapMenuItem.setText(resourceMap.getString("addMotifMapMenuItem.text")); // NOI18N
        addMotifMapMenuItem.setName("addMotifMapMenuItem"); // NOI18N
        addMotifMapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addMotifMapMenuItem);

        addBackgroundFrequenciesMenuItem.setText(resourceMap.getString("addBackgroundFrequenciesMenuItem.text")); // NOI18N
        addBackgroundFrequenciesMenuItem.setName("addBackgroundFrequenciesMenuItem"); // NOI18N
        addBackgroundFrequenciesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addBackgroundFrequenciesMenuItem);

        addModuleMenuItem.setText(resourceMap.getString("addModuleMenuItem.text")); // NOI18N
        addModuleMenuItem.setName("addModuleMenuItem"); // NOI18N
        addModuleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addModuleMenuItem);

        addModuleCollectionMenuItem.setText(resourceMap.getString("addModuleCollectionMenuItem.text")); // NOI18N
        addModuleCollectionMenuItem.setName("addModuleCollectionMenuItem"); // NOI18N
        addModuleCollectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addModuleCollectionMenuItem);

        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(jMenuItem2);

        addModuleMapMenuItem.setText(resourceMap.getString("addModuleMapMenuItem.text")); // NOI18N
        addModuleMapMenuItem.setName("addModuleMapMenuItem"); // NOI18N
        addModuleMapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addModuleMapMenuItem);

        jMenuItem3.setText(resourceMap.getString("jMenuItem3.text")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(jMenuItem3);

        addNumericVariableMenuItem.setText(resourceMap.getString("addNumericVariableMenuItem.text")); // NOI18N
        addNumericVariableMenuItem.setName("addNumericVariableMenuItem"); // NOI18N
        addNumericVariableMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addNumericVariableMenuItem);

        addPriorsGeneratorMenuItem.setText(resourceMap.getString("addPriorsGeneratorMenuItem.text")); // NOI18N
        addPriorsGeneratorMenuItem.setName("addPriorsGeneratorMenuItem"); // NOI18N
        addPriorsGeneratorMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addPriorsGeneratorMenuItem);

        addSequenceCollectionMenuItem.setText(resourceMap.getString("addSequenceCollectionMenuItem.text")); // NOI18N
        addSequenceCollectionMenuItem.setName("addSequenceCollectionMenuItem"); // NOI18N
        addSequenceCollectionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addSequenceCollectionMenuItem);

        addSequencePartitionMenuItem.setText(resourceMap.getString("addSequencePartitionMenuItem.text")); // NOI18N
        addSequencePartitionMenuItem.setName("addSequencePartitionMenuItem"); // NOI18N
        addSequencePartitionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addSequencePartitionMenuItem);

        addSequenceMapMenuItem.setText(resourceMap.getString("addSequenceMapMenuItem.text")); // NOI18N
        addSequenceMapMenuItem.setName("addSequenceMapMenuItem"); // NOI18N
        addSequenceMapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addSequenceMapMenuItem);

        addNumericMapVariableMenuItem.setText(resourceMap.getString("addNumericMapVariableMenuItem.text")); // NOI18N
        addNumericMapVariableMenuItem.setName("addNumericMapVariableMenuItem"); // NOI18N
        addNumericMapVariableMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addNumericMapVariableMenuItem);

        addTextVariableMenuItem.setText(resourceMap.getString("addTextVariableMenuItem.text")); // NOI18N
        addTextVariableMenuItem.setName("addTextVariableMenuItem"); // NOI18N
        addTextVariableMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDataItemMenuHandler(evt);
            }
        });
        addNewDataItemMenu.add(addTextVariableMenuItem);

        dataMenu.add(addNewDataItemMenu);

        importFromFileMenuItem.setAction(actionMap.get("showLoadFromFileDialog")); // NOI18N
        importFromFileMenuItem.setToolTipText(resourceMap.getString("importFromFileMenuItem.toolTipText")); // NOI18N
        importFromFileMenuItem.setName("importFromFileMenuItem"); // NOI18N
        dataMenu.add(importFromFileMenuItem);

        favoritesMenuItem.setAction(actionMap.get("showFavoritesDialog")); // NOI18N
        favoritesMenuItem.setText(resourceMap.getString("favoritesMenuItem.text")); // NOI18N
        favoritesMenuItem.setName("favoritesMenuItem"); // NOI18N
        dataMenu.add(favoritesMenuItem);

        dataMenuSeparator1.setName("dataMenuSeparator1"); // NOI18N
        dataMenu.add(dataMenuSeparator1);

        clearDataSubmenu.setText(resourceMap.getString("clearDataSubmenu.text")); // NOI18N
        clearDataSubmenu.setName("clearDataSubmenu"); // NOI18N

        clearSequencesAndFeaturesMenuItem.setAction(actionMap.get("clearAllSequencesActionMethod")); // NOI18N
        clearSequencesAndFeaturesMenuItem.setText(resourceMap.getString("clearSequencesAndFeaturesMenuItem.text")); // NOI18N
        clearSequencesAndFeaturesMenuItem.setToolTipText(resourceMap.getString("clearSequencesAndFeaturesMenuItem.toolTipText")); // NOI18N
        clearSequencesAndFeaturesMenuItem.setName("clearSequencesAndFeaturesMenuItem"); // NOI18N
        clearDataSubmenu.add(clearSequencesAndFeaturesMenuItem);

        clearFeatureDataMenuItem.setAction(actionMap.get("clearFeatureDataActionMethod")); // NOI18N
        clearFeatureDataMenuItem.setText(resourceMap.getString("clearFeatureDataMenuItem.text")); // NOI18N
        clearFeatureDataMenuItem.setToolTipText(resourceMap.getString("clearFeatureDataMenuItem.toolTipText")); // NOI18N
        clearFeatureDataMenuItem.setName("clearFeatureDataMenuItem"); // NOI18N
        clearDataSubmenu.add(clearFeatureDataMenuItem);

        clearMotifsAndModulesMenuItem.setAction(actionMap.get("clearAllMotifsAndModulesActionMethod")); // NOI18N
        clearMotifsAndModulesMenuItem.setText(resourceMap.getString("clearMotifsAndModulesMenuItem.text")); // NOI18N
        clearMotifsAndModulesMenuItem.setToolTipText(resourceMap.getString("clearMotifsAndModulesMenuItem.toolTipText")); // NOI18N
        clearMotifsAndModulesMenuItem.setName("clearMotifsAndModulesMenuItem"); // NOI18N
        clearDataSubmenu.add(clearMotifsAndModulesMenuItem);

        clearModulesMenuItem.setAction(actionMap.get("clearAllModulesActionMethod")); // NOI18N
        clearModulesMenuItem.setText(resourceMap.getString("clearModulesMenuItem.text")); // NOI18N
        clearModulesMenuItem.setToolTipText(resourceMap.getString("clearModulesMenuItem.toolTipText")); // NOI18N
        clearModulesMenuItem.setName("clearModulesMenuItem"); // NOI18N
        clearDataSubmenu.add(clearModulesMenuItem);

        jMenuItem4.setAction(actionMap.get("clearSequenceRelatedDataMethod")); // NOI18N
        jMenuItem4.setText(resourceMap.getString("jMenuItem4.text")); // NOI18N
        jMenuItem4.setToolTipText(resourceMap.getString("jMenuItem4.toolTipText")); // NOI18N
        jMenuItem4.setName("jMenuItem4"); // NOI18N
        clearDataSubmenu.add(jMenuItem4);

        clearOtherDataMenuItem.setAction(actionMap.get("clearOtherDataActionMethod")); // NOI18N
        clearOtherDataMenuItem.setText(resourceMap.getString("clearOtherDataMenuItem.text")); // NOI18N
        clearOtherDataMenuItem.setToolTipText(resourceMap.getString("clearOtherDataMenuItem.toolTipText")); // NOI18N
        clearOtherDataMenuItem.setName("clearOtherDataMenuItem"); // NOI18N
        clearDataSubmenu.add(clearOtherDataMenuItem);

        dataMenu.add(clearDataSubmenu);

        clearAllDataMenuItem.setAction(actionMap.get("clearAllDataActionMethod")); // NOI18N
        clearAllDataMenuItem.setText(resourceMap.getString("clearAllDataMenuItem.text")); // NOI18N
        clearAllDataMenuItem.setName("clearAllDataMenuItem"); // NOI18N
        dataMenu.add(clearAllDataMenuItem);

        menuBar.add(dataMenu);

        operationsMenu.setText(resourceMap.getString("operationsMenu.text")); // NOI18N
        operationsMenu.setName("operationsMenu"); // NOI18N
        menuBar.add(operationsMenu);

        protocolMenu.setText(resourceMap.getString("protocolMenu.text")); // NOI18N
        protocolMenu.setName("protocolMenu"); // NOI18N

        recordMenuItem.setAction(actionMap.get("recordMode")); // NOI18N
        recordMenuItem.setName("recordMenuItem"); // NOI18N
        recordMenuItem.setSelectedIcon(resourceMap.getIcon("recordMenuItem.selectedIcon")); // NOI18N
        protocolMenu.add(recordMenuItem);

        executeMenuItem.setAction(actionMap.get("executeProtocol")); // NOI18N
        executeMenuItem.setName("executeMenuItem"); // NOI18N
        protocolMenu.add(executeMenuItem);

        stopMenuItem.setAction(actionMap.get("stopAction")); // NOI18N
        stopMenuItem.setName("stopMenuItem"); // NOI18N
        protocolMenu.add(stopMenuItem);

        protocolMenuSeparator1.setName("protocolMenuSeparator1"); // NOI18N
        protocolMenu.add(protocolMenuSeparator1);

        executeCurrentLineMenuItem.setAction(actionMap.get("executeCurrentProtocolLine")); // NOI18N
        executeCurrentLineMenuItem.setText(resourceMap.getString("executeCurrentLineMenuItem.text")); // NOI18N
        executeCurrentLineMenuItem.setName("executeCurrentLineMenuItem"); // NOI18N
        protocolMenu.add(executeCurrentLineMenuItem);

        executeFromCurrentLineMenuItem.setAction(actionMap.get("executeProtocolFromCurrentLine")); // NOI18N
        executeFromCurrentLineMenuItem.setText(resourceMap.getString("executeFromCurrentLineMenuItem.text")); // NOI18N
        executeFromCurrentLineMenuItem.setName("executeFromCurrentLineMenuItem"); // NOI18N
        protocolMenu.add(executeFromCurrentLineMenuItem);

        executeCurrentSelectionMenuItem.setAction(actionMap.get("executeCurrentProtocolSelection")); // NOI18N
        executeCurrentSelectionMenuItem.setText(resourceMap.getString("executeCurrentSelectionMenuItem.text")); // NOI18N
        executeCurrentSelectionMenuItem.setName("executeCurrentSelectionMenuItem"); // NOI18N
        protocolMenu.add(executeCurrentSelectionMenuItem);

        commentSelectedLines.setAction(actionMap.get("commentSelectedLinesInProtocol")); // NOI18N
        commentSelectedLines.setText(resourceMap.getString("commentSelectedLines.text")); // NOI18N
        commentSelectedLines.setName("commentSelectedLines"); // NOI18N
        protocolMenu.add(commentSelectedLines);

        uncommentSelectedLines.setAction(actionMap.get("uncommentSelectedLinesInProtocol")); // NOI18N
        uncommentSelectedLines.setText(resourceMap.getString("uncommentSelectedLines.text")); // NOI18N
        uncommentSelectedLines.setToolTipText(resourceMap.getString("uncommentSelectedLines.toolTipText")); // NOI18N
        uncommentSelectedLines.setName("uncommentSelectedLines"); // NOI18N
        protocolMenu.add(uncommentSelectedLines);

        colorKeywordsMenuItem.setAction(actionMap.get("selectColorProtocol")); // NOI18N
        colorKeywordsMenuItem.setSelected(true);
        colorKeywordsMenuItem.setName("colorKeywordsMenuItem"); // NOI18N
        protocolMenu.add(colorKeywordsMenuItem);

        protocolMenuSeparator2.setName("protocolMenuSeparator2"); // NOI18N
        protocolMenu.add(protocolMenuSeparator2);

        macroEditorMenuItem.setAction(actionMap.get("showMacroEditor")); // NOI18N
        macroEditorMenuItem.setText(resourceMap.getString("macroEditorMenuItem.text")); // NOI18N
        macroEditorMenuItem.setName("macroEditorMenuItem"); // NOI18N
        protocolMenu.add(macroEditorMenuItem);

        expandMacrosMenuItem.setAction(actionMap.get("expandMacrosInCurrentProtocol")); // NOI18N
        expandMacrosMenuItem.setText(resourceMap.getString("expandMacrosMenuItem.text")); // NOI18N
        expandMacrosMenuItem.setName("expandMacrosMenuItem"); // NOI18N
        protocolMenu.add(expandMacrosMenuItem);

        protocolMenuSeparator3.setName("protocolMenuSeparator3"); // NOI18N
        protocolMenu.add(protocolMenuSeparator3);

        changeProtocolSubmenu.setText(resourceMap.getString("changeProtocolSubmenu.text")); // NOI18N
        changeProtocolSubmenu.setName("changeProtocolSubmenu"); // NOI18N
        protocolMenu.add(changeProtocolSubmenu);

        menuBar.add(protocolMenu);

        toolsMenu.setText(resourceMap.getString("toolsMenu.text")); // NOI18N
        toolsMenu.setName("toolsMenu"); // NOI18N

        selectionToolMenuItem.setAction(actionMap.get("selectionToolAction")); // NOI18N
        selectionToolMenuItem.setText(resourceMap.getString("selectionToolMenuItem.text")); // NOI18N
        selectionToolMenuItem.setName("selectionToolMenuItem"); // NOI18N
        toolsMenu.add(selectionToolMenuItem);

        handToolMenuItem.setAction(actionMap.get("handToolAction")); // NOI18N
        handToolMenuItem.setText(resourceMap.getString("handToolMenuItem.text")); // NOI18N
        handToolMenuItem.setName("handToolMenuItem"); // NOI18N
        toolsMenu.add(handToolMenuItem);

        drawToolMenuItem.setAction(actionMap.get("drawToolAction")); // NOI18N
        drawToolMenuItem.setName("drawToolMenuItem"); // NOI18N
        toolsMenu.add(drawToolMenuItem);

        zoomToolMenuItem.setAction(actionMap.get("zoomToolAction")); // NOI18N
        zoomToolMenuItem.setText(resourceMap.getString("zoomToolMenuItem.text")); // NOI18N
        zoomToolMenuItem.setName("zoomToolMenuItem"); // NOI18N
        toolsMenu.add(zoomToolMenuItem);

        toolsMenuSeparator1.setName("toolsMenuSeparator1"); // NOI18N
        toolsMenu.add(toolsMenuSeparator1);

        motifBrowserMenuItem.setAction(actionMap.get("showMotifBrowser")); // NOI18N
        motifBrowserMenuItem.setText(resourceMap.getString("motifBrowserMenuItem.text")); // NOI18N
        motifBrowserMenuItem.setName("motifBrowserMenuItem"); // NOI18N
        toolsMenu.add(motifBrowserMenuItem);

        moduleBrowserMenuItem.setAction(actionMap.get("showModuleBrowser")); // NOI18N
        moduleBrowserMenuItem.setText(resourceMap.getString("moduleBrowserMenuItem.text")); // NOI18N
        moduleBrowserMenuItem.setName("moduleBrowserMenuItem"); // NOI18N
        toolsMenu.add(moduleBrowserMenuItem);

        sequenceBrowserMenuItem.setAction(actionMap.get("showSequenceBrowser")); // NOI18N
        sequenceBrowserMenuItem.setText(resourceMap.getString("sequenceBrowserMenuItem.text")); // NOI18N
        sequenceBrowserMenuItem.setName("sequenceBrowserMenuItem"); // NOI18N
        toolsMenu.add(sequenceBrowserMenuItem);

        regionBrowserMenuItem.setAction(actionMap.get("showRegionBrowser")); // NOI18N
        regionBrowserMenuItem.setText(resourceMap.getString("regionBrowserMenuItem.text")); // NOI18N
        regionBrowserMenuItem.setName("regionBrowserMenuItem"); // NOI18N
        toolsMenu.add(regionBrowserMenuItem);

        toolsMenuSeparator2.setName("toolsMenuSeparator2"); // NOI18N
        toolsMenu.add(toolsMenuSeparator2);

        interactionFilterMenuItem.setAction(actionMap.get("showInteractionsFilterDialog")); // NOI18N
        interactionFilterMenuItem.setText(resourceMap.getString("interactionFilterMenuItem.text")); // NOI18N
        interactionFilterMenuItem.setName("interactionFilterMenuItem"); // NOI18N
        toolsMenu.add(interactionFilterMenuItem);

        PositionalDistributionViewerMenuItem.setAction(actionMap.get("showPositionalDistributionViewerDialog")); // NOI18N
        PositionalDistributionViewerMenuItem.setText(resourceMap.getString("PositionalDistributionViewerMenuItem.text")); // NOI18N
        PositionalDistributionViewerMenuItem.setToolTipText(resourceMap.getString("PositionalDistributionViewerMenuItem.toolTipText")); // NOI18N
        PositionalDistributionViewerMenuItem.setName("PositionalDistributionViewerMenuItem"); // NOI18N
        toolsMenu.add(PositionalDistributionViewerMenuItem);

        motifScoreFilterMenuItem.setAction(actionMap.get("showMotifScoreFilterDialog")); // NOI18N
        motifScoreFilterMenuItem.setText(resourceMap.getString("motifScoreFilterMenuItem.text")); // NOI18N
        motifScoreFilterMenuItem.setName("motifScoreFilterMenuItem"); // NOI18N
        toolsMenu.add(motifScoreFilterMenuItem);

        toolsMenuSeparator3.setName("toolsMenuSeparator3"); // NOI18N
        toolsMenu.add(toolsMenuSeparator3);

        cropSequencesMenuItem.setAction(actionMap.get("showCropSequencesDialog")); // NOI18N
        cropSequencesMenuItem.setText(resourceMap.getString("cropSequencesMenuItem.text")); // NOI18N
        cropSequencesMenuItem.setName("cropSequencesMenuItem"); // NOI18N
        toolsMenu.add(cropSequencesMenuItem);

        extendSequencesMenuItem.setAction(actionMap.get("showExtendSequencesDialog")); // NOI18N
        extendSequencesMenuItem.setText(resourceMap.getString("extendSequencesMenuItem.text")); // NOI18N
        extendSequencesMenuItem.setName("extendSequencesMenuItem"); // NOI18N
        toolsMenu.add(extendSequencesMenuItem);

        sortSequencesToolMenuItem.setAction(actionMap.get("showSortSequencesDialog")); // NOI18N
        sortSequencesToolMenuItem.setText(resourceMap.getString("sortSequencesToolMenuItem.text")); // NOI18N
        sortSequencesToolMenuItem.setName("sortSequencesToolMenuItem"); // NOI18N
        toolsMenu.add(sortSequencesToolMenuItem);

        updateMotifsMenuItem.setAction(actionMap.get("showAnnotateMotifsDialog")); // NOI18N
        updateMotifsMenuItem.setToolTipText(resourceMap.getString("updateMotifsMenuItem.toolTipText")); // NOI18N
        updateMotifsMenuItem.setName("updateMotifsMenuItem"); // NOI18N
        toolsMenu.add(updateMotifsMenuItem);

        menuBar.add(toolsMenu);

        configureMenu.setText(resourceMap.getString("configureMenu.text")); // NOI18N
        configureMenu.setName("configureMenu"); // NOI18N

        configureDatatracksMenuItem.setAction(actionMap.get("showConfigureDatatracksDialog")); // NOI18N
        configureDatatracksMenuItem.setName("configureDatatracksMenuItem"); // NOI18N
        configureMenu.add(configureDatatracksMenuItem);

        configureOrganismsAndIdentifiersMenuItem.setAction(actionMap.get("showConfigureOrganismsAndIdentifiersDialog")); // NOI18N
        configureOrganismsAndIdentifiersMenuItem.setText(resourceMap.getString("configureOrganismsAndIdentifiersMenuItem.text")); // NOI18N
        configureOrganismsAndIdentifiersMenuItem.setToolTipText(resourceMap.getString("configureOrganismsAndIdentifiersMenuItem.toolTipText")); // NOI18N
        configureOrganismsAndIdentifiersMenuItem.setName("configureOrganismsAndIdentifiersMenuItem"); // NOI18N
        configureMenu.add(configureOrganismsAndIdentifiersMenuItem);

        externalProgramMenuItem.setAction(actionMap.get("showConfigureExternalProgramsDialog")); // NOI18N
        externalProgramMenuItem.setToolTipText(resourceMap.getString("externalProgramMenuItem.toolTipText")); // NOI18N
        externalProgramMenuItem.setName("externalProgramMenuItem"); // NOI18N
        configureMenu.add(externalProgramMenuItem);

        datarepositoriesMenuItem.setAction(actionMap.get("showDataRepositoriesDialog")); // NOI18N
        datarepositoriesMenuItem.setName("datarepositoriesMenuItem"); // NOI18N
        configureMenu.add(datarepositoriesMenuItem);

        pluginsMenuItem.setAction(actionMap.get("showPluginsDialog")); // NOI18N
        pluginsMenuItem.setText(resourceMap.getString("pluginsMenuItem.text")); // NOI18N
        pluginsMenuItem.setName("pluginsMenuItem"); // NOI18N
        configureMenu.add(pluginsMenuItem);

        optionsMenuItem.setAction(actionMap.get("showOptionsDialog")); // NOI18N
        optionsMenuItem.setName("optionsMenuItem"); // NOI18N
        configureMenu.add(optionsMenuItem);

        configureMenuSeparator1.setName("configureMenuSeparator1"); // NOI18N
        configureMenu.add(configureMenuSeparator1);

        installConfigFileMenuItem.setAction(actionMap.get("installConfigurationFile")); // NOI18N
        installConfigFileMenuItem.setText(resourceMap.getString("installConfigFileMenuItem.text")); // NOI18N
        installConfigFileMenuItem.setName("installConfigFileMenuItem"); // NOI18N
        configureMenu.add(installConfigFileMenuItem);

        backupConfigurationMenuItem.setAction(actionMap.get("backupCurrentConfiguration")); // NOI18N
        backupConfigurationMenuItem.setText(resourceMap.getString("backupConfigurationMenuItem.text")); // NOI18N
        backupConfigurationMenuItem.setName("backupConfigurationMenuItem"); // NOI18N
        configureMenu.add(backupConfigurationMenuItem);

        menuBar.add(configureMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        helpMenuItem.setAction(actionMap.get("helpAction")); // NOI18N
        helpMenuItem.setName("helpMenuItem"); // NOI18N
        helpMenu.add(helpMenuItem);

        welcomeMenuItem.setAction(actionMap.get("showWelcomeScreen")); // NOI18N
        welcomeMenuItem.setText(resourceMap.getString("welcomeMenuItem.text")); // NOI18N
        welcomeMenuItem.setName("welcomeMenuItem"); // NOI18N
        helpMenu.add(welcomeMenuItem);

        quickTutorialMenuItem.setAction(actionMap.get("quickstartTutorialAction")); // NOI18N
        quickTutorialMenuItem.setText(resourceMap.getString("quickTutorialMenuItem.text")); // NOI18N
        quickTutorialMenuItem.setName("quickTutorialMenuItem"); // NOI18N
        helpMenu.add(quickTutorialMenuItem);

        keyboardshortcutsMenuItem.setAction(actionMap.get("showKeyboardShortcutsAction")); // NOI18N
        keyboardshortcutsMenuItem.setText(resourceMap.getString("keyboardshortcutsMenuItem.text")); // NOI18N
        keyboardshortcutsMenuItem.setName("keyboardshortcutsMenuItem"); // NOI18N
        helpMenu.add(keyboardshortcutsMenuItem);

        exampleSessionsMenu.setText(resourceMap.getString("exampleSessionsMenu.text")); // NOI18N
        exampleSessionsMenu.setEnabled(false);
        exampleSessionsMenu.setName("exampleSessionsMenu"); // NOI18N
        helpMenu.add(exampleSessionsMenu);

        notificationsMenuItem.setAction(actionMap.get("showNotificationsAction")); // NOI18N
        notificationsMenuItem.setText(resourceMap.getString("notificationsMenuItem.text")); // NOI18N
        notificationsMenuItem.setName("notificationsMenuItem"); // NOI18N
        helpMenu.add(notificationsMenuItem);

        citationsMenuItem.setAction(actionMap.get("showCitationDialog")); // NOI18N
        citationsMenuItem.setName("citationsMenuItem"); // NOI18N
        helpMenu.add(citationsMenuItem);

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setMaximumSize(new java.awt.Dimension(32767, 30));
        statusPanel.setMinimumSize(new java.awt.Dimension(0, 30));
        statusPanel.setName("statusPanel"); // NOI18N
        statusPanel.setPreferredSize(new java.awt.Dimension(2, 30));

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1293, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1119, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(topPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
        setToolBar(Toolbar);
    }// </editor-fold>//GEN-END:initComponents

private void addDataItemMenuHandler(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDataItemMenuHandler
         this.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         Prompt prompt=null;
         String type=evt.getActionCommand();//GEN-LAST:event_addDataItemMenuHandler
              if (type.equals(SequenceCollection.getType())) prompt=new Prompt_SequenceCollection(this, null, null, true);
         else if (type.equals(SequencePartition.getType())) prompt=new Prompt_SequencePartition(this, null, null, true);
         else if (type.equals(NumericVariable.getType())) prompt=new Prompt_NumericVariable(this, null, null, true);
         else if (type.equals(SequenceNumericMap.getType())) prompt=new Prompt_SequenceNumericMap(this, null, null, true);
         else if (type.equals(MotifNumericMap.getType())) prompt=new Prompt_MotifNumericMap(this, null, null, true);
         else if (type.equals(ModuleNumericMap.getType())) prompt=new Prompt_ModuleNumericMap(this, null, null, true);
         else if (type.equals(SequenceTextMap.getType())) prompt=new Prompt_SequenceTextMap(this, null, null, true);
         else if (type.equals(MotifTextMap.getType())) prompt=new Prompt_MotifTextMap(this, null, null, true);
         else if (type.equals(ModuleTextMap.getType())) prompt=new Prompt_ModuleTextMap(this, null, null, true);
         else if (type.equals(BackgroundModel.getType())) prompt=new Prompt_BackgroundModel(this, null, null, true);
         else if (type.equals(TextVariable.getType())) prompt=new Prompt_TextVariable(this, null, null, true);
         else if (type.equals(Motif.getType())) prompt=new Prompt_Motif(this, null, null,visualizationSettings, true);
         else if (type.equals(ModuleCRM.getType())) prompt=new Prompt_Module(this, null, null, true);
         else if (type.equals(MotifCollection.getType())) prompt=new Prompt_MotifCollection(this, null, null, true);
         else if (type.equals(MotifPartition.getType())) prompt=new Prompt_MotifPartition(this, null, null, true);
         else if (type.equals(ModuleCollection.getType())) prompt=new Prompt_ModuleCollection(this, null, null, true);
         else if (type.equals(ModulePartition.getType())) prompt=new Prompt_ModulePartition(this, null, null, true);
         else if (type.equals(ExpressionProfile.getType())) prompt=new Prompt_ExpressionProfile(this, null, null, true);
         else if (type.equals(PriorsGenerator.getType())) prompt=new Prompt_PriorsGenerator(this, null, null, true);
         if (prompt==null) {logMessage("Unrecognized data type:"+type,50);return;}
         prompt.setLocation(this.getFrame().getWidth()/2-prompt.getWidth()/2, this.getFrame().getHeight()/2-prompt.getHeight()/2);
         prompt.setVisible(true);
         this.getFrame().setCursor(Cursor.getDefaultCursor());
         if (prompt.isOKPressed()) {
            Data variable=prompt.getData();
            boolean silent=true; // silent=prompt.isSilentOK();
            if (variable==null) {
                prompt.dispose();
                return;
            }
            OperationTask task=new OperationTask("new "+type);
            task.setParameter(Operation_new.DATA_TYPE, type);
            task.setParameter(OperationTask.TARGET_NAME,variable.getName());
            String parameterString=variable.getValueAsParameterString();
            task.setParameter(OperationTask.OPERATION_NAME, "new");
            task.setParameter(Operation_new.PARAMETERS, parameterString);
            task.setParameter(OperationTask.ENGINE,this.getEngine());
            if (silent) task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
            if (prompt.isDataImportedFromFile()) {
                prompt.setImportFromFileSettingsInTask(task); // sets FILENAME, DATA_FORMAT and DATA_FORMAT_SETTINGS parameters
            }
            variable.setAdditionalOperationNewTaskParameters(task);
            task.addAffectedDataObject(variable.getName(), engine.getDataClassForTypeName(type));
            prompt.dispose();
            this.launchOperationTask(task,this.isRecording());
            if (motifsPanel.holdsType(type)) motifsPanel.setContentType(type);
         } else {
             prompt.dispose();
         }
}

    /** This should be called when a new plugin has been installed or uninstalled
      * from the "Configure plugins dialog" in order to update the GUI properly
      * (in case the plugin is unable to do so itself)
      */
    public void pluginInstallationUpdated() {
        setupOperationsMenu(); // refreshes the operations and analyses menu
    }

    /** Sets up the menu containing all available operations (in the top-level menu bar) */
    private void setupOperationsMenu() {
        operationsMenu.removeAll(); // start the update from scratch to make it idempotent
        HashMap<String,String> operationsGroups=new HashMap<String,String>();
        for (Operation operation:engine.getAllOperations()) {
            String operationGroup=operation.getOperationGroup();
            if (operationGroup!=null) operationsGroups.put(operation.getName(), operationGroup);
        }
        ArrayList<Operation> operations=engine.getAllOperations();
        OperationsComparator comparator=new OperationsComparator();
        Collections.sort(operations,comparator);
        String[] analyses=engine.getAnalysisNames();
        OperationsMenuListener menuListener = new OperationsMenuListener();
        JMenu analyzemenu=new JMenu("Analyze");
        for (String analysisname:analyses) {
            JMenuItem analysisItem=new JMenuItem(analysisname);
            Analysis analysis=engine.getAnalysis(analysisname);
            analysisItem.setToolTipText(MotifLabGUI.formatTooltipString(analysis.getDescription(), tooltipLineLength));
            analysisItem.addActionListener(menuListener);
            analyzemenu.add(analysisItem);
        }
        operationsMenu.add(analyzemenu);
        HashMap<String,JMenu> submenus=new HashMap<String,JMenu>();
        ArrayList<String> submenunames=new ArrayList<String>();
        for (String submenuname:operationsGroups.values()) {
            if(!submenunames.contains(submenuname)) submenunames.add(submenuname);
        }
        Collections.sort(submenunames);
        int mIndex=submenunames.indexOf("Motif");
        int MIndex=submenunames.indexOf("Module");
        if (mIndex>=0 && MIndex>=0) {
            submenunames.remove("Motif");
            submenunames.add(MIndex, "Motif"); // sort Motif before ModuleCRM if both are present
        }
        for (String submenuname:submenunames) {
            JMenu submenu=new JMenu(submenuname);
            submenus.put(submenuname,submenu);
            operationsMenu.add(submenu);
        }
        for (Operation operation:operations) { // operations should be listed in alphabetical order
            if (operation instanceof Operation_analyze) continue;
            String operationName=operation.getName();
            JMenuItem item=new JMenuItem(operationName);
            item.setToolTipText(MotifLabGUI.formatTooltipString(operation.getDescription(),tooltipLineLength));
            item.addActionListener(menuListener);
            JMenu submenu=submenus.get(operationsGroups.get(operationName));
            if (submenu!=null) submenu.add(item);
            else operationsMenu.add(item);
        }
    }

    /** Returns a menu containing all applicable operations. The menu can be added to a context menu
     *  such as the "Perform operation" submenu included in context menus for Data Panels
     *
     * @param menuname A name to be given to the menu
     * @param sources The data item(s) that the operation should be applied to.
     *                If this is specified (not null) only the operations that can be applied to this
     *                source object will be included in the context menu.
     *                This object can be either a single Data object, a single data Class type or an array
     *                of data objects (Data[]).
     * @param menuListener   The listener which will be notified when an item is selected in the menu
     *                       The actionCommand in the ActionEvent can refer to either the name of an
     *                       operation or the name of an analysis (when the operation is "analyze").
     * @return
     */
    public JMenu getOperationContextMenu(String menuname, Object sources, ActionListener menuListener) {
        JMenu contextMenu=new JMenu(menuname);
        ArrayList<Operation> operations=engine.getAllOperations();
        OperationsComparator comparator=new OperationsComparator();
        Collections.sort(operations,comparator);
        String[] analyses=engine.getAnalysisNames();
        JMenu analyzemenu=new JMenu("Analyze");
        for (String analysisname:analyses) {
            JMenuItem analysisItem=new JMenuItem(analysisname);
            Analysis analysis=engine.getAnalysis(analysisname);
            if (sources!=null) { // check if the analysis can be applied to the given sources
                if (sources instanceof Data && !analysis.canUseAsSourceProxy((Data)sources)) continue;
                else if (sources instanceof Class && !analysis.canUseAsSourceProxy((Class)sources)) continue;
                else if (sources instanceof Data[] && !canUseAsAnalysisSources(analysis,(Data[])sources)) continue;
                else if (!(sources instanceof Data || sources instanceof Data[] || sources instanceof Class)) continue; // unrecognized source
            }
            analysisItem.setToolTipText(MotifLabGUI.formatTooltipString(analysis.getDescription(),tooltipLineLength));
            analysisItem.addActionListener(menuListener);
            analyzemenu.add(analysisItem);
        }
        HashMap<String,JMenu> submenus=new HashMap<String,JMenu>();
        if (analyzemenu.getMenuComponentCount()>0) submenus.put("Analyze",analyzemenu);
        for (Operation operation:operations) { // operations should be listed in alphabetical order
            if (operation instanceof Operation_analyze) continue; // this has been handled already
            String operationName=operation.getName();
            boolean canUseAsSource=false;
            boolean canUseAsSourceProxy=false;
            if (sources!=null) { // check if the analysis can be applied to the given sources, else skip it
                     if (sources instanceof Data && operation.canUseAsSource((Data)sources)) canUseAsSource=true;
                else if (sources instanceof Data && operation.canUseAsSourceProxy((Data)sources)) canUseAsSourceProxy=true;
                else if (sources instanceof Class && operation.canUseAsSource((Class)sources)) canUseAsSource=true;
                else if (sources instanceof Data[] && canUseAsSources(operation,(Data[])sources)) canUseAsSource=true;
                else if (sources instanceof Data[] && canUseAsProxySources(operation,(Data[])sources)) canUseAsSourceProxy=true;
                if (!(canUseAsSource||canUseAsSourceProxy)) continue; // this operation can not be applied to the sources, so skip it
            }
            String operationGroup=operation.getOperationGroup();
            JMenuItem item=new JMenuItem(operationName);
            item.setToolTipText(MotifLabGUI.formatTooltipString(operation.getDescription(),tooltipLineLength));
            item.addActionListener(menuListener);
            if (operationGroup==null) {contextMenu.add(item);}
            else {
                if (!submenus.containsKey(operationGroup)) submenus.put(operationGroup, new JMenu(operationGroup));
                JMenu submenu=submenus.get(operationGroup);
                submenu.add(item);
            }
        }
        String[] submenunames=submenus.keySet().toArray(new String[submenus.size()]);
        Arrays.sort(submenunames);
        int mIndex=-1;int MIndex=-1;
        for (int i=0;i<submenunames.length;i++) {
            if (submenunames[i].equals("Motif")) mIndex=i;
            else if (submenunames[i].equals("Module")) MIndex=i;
        }
        if (mIndex>=0 && MIndex>=0) { // sort Motif before ModuleCRM if both are present
            submenunames[MIndex]="Motif"; // swap these two labels
            submenunames[mIndex]="Module";// swap these two labels
        }
        for (int i=0;i<submenunames.length;i++) {
            contextMenu.add(submenus.get(submenunames[i]),i);
        }
        return contextMenu;
    }

    private boolean canUseAsSources(Operation operation, Data[] sources) {
        if (!operation.canHaveMultipleInput()) return false;
        for (Data source:sources) {
            if (!operation.canUseAsSource(source)) return false;
        }
        return true;
    }
    private boolean canUseAsAnalysisSources(Analysis analysis, Data[] sources) {
        for (Data source:sources) {
            if (!analysis.canUseAsSourceProxy(source)) return false;
        }
        return true;
    }
    /** returns true of the given operation can use any of data objects as source proxy */
    private boolean canUseAsProxySources(Operation operation, Data[] sources) {
        for (Data source:sources) {
            if (operation.canUseAsSourceProxy(source)) return true;
        }
        return false;
    }

    private class OperationsComparator implements Comparator<Operation> {
            @Override
            public int compare(Operation o1, Operation o2) {
                return o1.getName().compareTo(o2.getName());
            }
    }
    /**
     * An inner class that listens to popup-menu events related to the operations submenu and notifies the gui events
     * This is used by the Operations-menu in the top-level menu bar.
     * Other operations menus in context menus must provide their own callback listeners
     */
    private class OperationsMenuListener implements ActionListener {
       @Override
       public void actionPerformed(ActionEvent e) {
            Operation operation=engine.getOperation(e.getActionCommand());
            if (operation!=null) {
                OperationTask parameters=new OperationTask(operation.getName());
                parameters.setParameter(OperationTask.OPERATION, operation);
                MotifLabGUI.this.launchOperationEditor(parameters);
            } else if (engine.getAnalysis(e.getActionCommand())!=null) {
                operation=engine.getOperation("analyze");
                OperationTask parameters=new OperationTask(operation.getName());
                parameters.setParameter(OperationTask.OPERATION, operation);
                parameters.setParameter(Operation_analyze.ANALYSIS, e.getActionCommand());
                MotifLabGUI.this.launchOperationEditor(parameters);
            }
       }
    }

/**
 * This method searches the engine for the data item with the given name and deletes it if found.
 * An undoableEdit event is registered with the undoManager so that the data item can be recovered again
 */
public void deleteDataItem(String name) {
    if (engine.getDataItem(name)==null || !engine.dataUpdatesIsAllowed()) return;
    getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    UndoMonitor undomonitor=undoManager.getUndoMonitor("Delete "+name);
    undomonitor.register();
    try {
        engine.removeDataItem(name);
    }
    catch (ExecutionError e) {
        getFrame().setCursor(Cursor.getDefaultCursor());
        JOptionPane.showMessageDialog(getFrame(), e.getMessage(),"Data Error" ,JOptionPane.ERROR_MESSAGE);
    }
    undomonitor.deregister(true);
    undoManager.addEdit(undomonitor);
    getFrame().setCursor(Cursor.getDefaultCursor());
}

/**
 * This method searches the engine for the data items with the given names and deletes them if found.
 * An undoableEdit event is registered with the undoManager so that the data can be recovered again
 */
public void deleteDataItems(String[] names) {
    if (!engine.dataUpdatesIsAllowed()) return;
    getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    UndoMonitor undomonitor=undoManager.getUndoMonitor("Delete data");
    undomonitor.register();
    for (String name:names) {
        if (name==null) continue;
        if (engine.getDataItem(name)!=null) {
            try {
                engine.removeDataItem(name);
            }
            catch (ExecutionError e) {
                getFrame().setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(getFrame(), e.getMessage(),"Data Error" ,JOptionPane.ERROR_MESSAGE);
                break;
            }
        }
    }
    undomonitor.deregister(true);
    undoManager.addEdit(undomonitor);
    getFrame().setCursor(Cursor.getDefaultCursor());
}

/**
 * Given a (non-registered) copy of a Data item with the same name as a registered data item,
 * this method will update the registered data item of the same name by importing the values
 * from the provided copy. If there are no registered data items with the same name as the copy
 * the copy will be stored directly as a new data item.
 * An undoableEdit event is registered with the undoManager so that the data update can be undone
 */
public void updateDataItem(Data data) {
    String undoName;
    boolean dataExists=false;
    if (engine.getDataItem(data.getName())!=null) dataExists=true;
    if (dataExists) undoName="Update "+data.getName();
    else undoName="Add data: "+data.getName();
    UndoMonitor undomonitor=undoManager.getUndoMonitor(undoName);
    undomonitor.register();
    try {
        if (dataExists) engine.updateDataItem(data);
        else engine.storeDataItem(data);
    } catch (ExecutionError e) {JOptionPane.showMessageDialog(getFrame(), "An error occurred during data update:\n"+e.getMessage(),"Data Error" ,JOptionPane.ERROR_MESSAGE);}
    undomonitor.deregister(true);
    undoManager.addEdit(undomonitor);
}

/**
 * Given a reference to a registered Data item and an object representing a partial update
 * to the parent data item (for instance a new or updated region, or updated sequence)
 * this method will update the registered data item by importing the values from the provided update object.
 * An undoableEdit event is registered with the undoManager so that the data update can be undone
 * @param featurename The name of the FeatureDataset which should be updated. This should be an existing registered data item
 * @param sequencename The name of the affected sequence within the FeatureDataset
 * @param old The part of the parent which should be updated (old original value). This should be either null (for FeatureSequenceData updates) or a clone of the original Region which is to be updated
 * @param updated The part of the parent which should be updated (new updated value). This could be a FeatureSequenceData or Region
 */
public void updatePartialDataItem(String featurename, String sequencename, Object old, Object updated) {
    FeatureDataset parent=(FeatureDataset)engine.getDataItem(featurename);
    if (parent==null) {
        JOptionPane.showMessageDialog(getFrame(), "An error occurred: unable to locate track","Data Error" ,JOptionPane.ERROR_MESSAGE);
        return;
    }
    parent=(FeatureDataset)parent.clone(); // make a completely new clone (this is currently required by the engine, since the engine can not do partial updates
    FeatureSequenceData originalsequenceclone=parent.getSequenceByName(sequencename); // this is now a clone of the original (currently stored) sequence
    PartialUndo partialundo=null;
    if (updated instanceof FeatureSequenceData) {
        String undoName="update "+featurename;
        partialundo=new PartialUndo(undoManager, undoName, featurename, sequencename, (Serializable)originalsequenceclone, (Serializable)updated);
        parent.replaceSequence((FeatureSequenceData)updated); //
    } else if (old instanceof Region || updated instanceof Region) {
         String undoName;
              if (old==null && updated!=null) undoName="add region";
         else if (old!=null && updated==null) undoName="delete region";
         else undoName="update region";
         if (old!=null) ((RegionSequenceData)originalsequenceclone).removeSimilarRegion((Region)old);
         if (updated!=null) ((RegionSequenceData)originalsequenceclone).addRegion((Region)updated);
         partialundo=new PartialUndo(undoManager, undoName, featurename, sequencename, (old==null)?null:(((Region)old).clone()), (updated==null)?null:(((Region)updated).clone()));
    } else {
        if (old!=null || updated!=null) logMessage("SYSTEM ERROR: wrong types in updatePartialDataItem(old="+((old==null)?"null":old.getClass().getSimpleName())+", updated="+((updated==null)?"null":updated.getClass().getSimpleName())+")");
        return; //
    }
    if (parent instanceof NumericDataset) ((NumericDataset)parent).updateAllowedMinMaxValuesFromData();
    else if (parent instanceof RegionDataset) ((RegionDataset)parent).updateMaxScoreValueFromData();
    try {
        engine.updateDataItem(parent);
        undoManager.addEdit(partialundo);
    } catch (ExecutionError e) {JOptionPane.showMessageDialog(getFrame(), "An error occurred during data update:\n"+e.getMessage(),"Data Error" ,JOptionPane.ERROR_MESSAGE);}
}


/**
  * Given a reference to a JList and a point (x,y), this function returns the index
  * of the element corresponding to that point (between 0 and model.getSize()-1).
  * If the coordinate does not correspond to an element in the list, the method returns the value -1;
  * @param list The JList (fitted with a DefaultListModel)
  * @param x
  * @param y
  * @return
  */
 private int getElementIndexAtPointInList(JList list, int x, int y) {
         int index=list.locationToIndex(new java.awt.Point(x,y));
         if (index>=0) {
            javax.swing.DefaultListModel model=(javax.swing.DefaultListModel)list.getModel();
            java.awt.Rectangle bounds = list.getCellBounds(0, model.getSize()-1);
            if (y>bounds.getHeight() || y<0 || x>bounds.getWidth() || x<0) index=-1; //
         }
         return index;
    }


    private Component findTargetcomponent=null;
    /** This method can be called by other components/panels to request that the next invocation
     *  of the "find" function should be redirected to them. If set to NULL the search will be
     *  redirected to the current tab in the main panel
     */
    public void setSearchTargetComponent(Component component) {
        findTargetcomponent=component;
        if (featuresPanel!=findTargetcomponent) featuresPanel.clearSelection();
        if (motifsPanel!=findTargetcomponent) motifsPanel.clearSelection();
        if (dataObjectsPanel!=findTargetcomponent) dataObjectsPanel.clearSelection();
    }


    @Action
    public void findAction() {
       String searchstring=searchTextfield.getText();
       if (searchstring==null) searchstring="";
       else searchstring=searchstring.trim();
       if (findTargetcomponent instanceof Searchable) {
           if (searchstring.isEmpty()) {
               searchstring=((Searchable)findTargetcomponent).getSelectedTextForSearch();
               if (searchstring==null) {
                   searchTextfield.requestFocusInWindow();
                   return;
               } else searchTextfield.setText(searchstring);
           }
          ((Searchable)findTargetcomponent).find(searchstring);
       } else {
           Component currentWindow=mainWindow.getSelectedComponent();
           if (currentWindow instanceof Searchable) {
                if (searchstring.isEmpty()) {
                   searchstring=((Searchable)currentWindow).getSelectedTextForSearch();
                   if (searchstring==null) {
                       searchTextfield.requestFocusInWindow();
                       return;
                   } else searchTextfield.setText(searchstring);
               }
               ((Searchable)currentWindow).find(searchstring);
           }
       }
    }

    @Action
    public void replaceAction() {
       Component currentWindow=mainWindow.getSelectedComponent();
       if (currentWindow instanceof Searchable) {
           if (!((Searchable)currentWindow).supportsReplace()) {
               statusMessage("Replace function is not applicable in this context");
           }
           else ((Searchable)currentWindow).searchAndReplace();
       }
    }

    /**
     * This method is called whenever a print action is invoked
     * (it is the "actionPerformed" method for the print action listener)
     */
    @Action
    public void printActionMethod() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Component currentWindow=mainWindow.getSelectedComponent();
        if (currentWindow instanceof ProtocolEditor && getProtocolEditor().getProtocol()!=null) {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop=java.awt.Desktop.getDesktop();
                    try {
                        File tempfile=File.createTempFile(getProtocolEditor().getProtocol().getName(), ".txt");
                        savePlainDocumentToFile((javax.swing.text.PlainDocument)getProtocolEditor().getProtocol().getDocument(),tempfile);
                        desktop.print(tempfile);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this.getFrame(), e.getClass().getSimpleName()+":\n\n"+e.getMessage(),"Print Error" ,JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    try {
                        ((ProtocolEditor)currentWindow).print();
                    } catch (PrinterException e) {
                        JOptionPane.showMessageDialog(this.getFrame(), e.getMessage(),"Print Error" ,JOptionPane.ERROR_MESSAGE);
                    }
                }
        } else if (currentWindow instanceof OutputPanel) {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop desktop=java.awt.Desktop.getDesktop();
                    try {
                        OutputData outputdata=((OutputPanel)currentWindow).getOutputData();
                        String suffix=outputdata.getPreferredFileSuffix();
                        File tempfile=File.createTempFile(outputdata.getName(), "."+suffix);
                        outputdata.saveToFile(tempfile, false, null, engine);
                        desktop.print(tempfile);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this.getFrame(), e.getClass().getSimpleName()+":\n\n"+e.getMessage(),"Print Error" ,JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    try {
                        ((OutputPanel)currentWindow).print();
                    } catch (PrinterException e) {
                        JOptionPane.showMessageDialog(this.getFrame(), e.getMessage(),"Print Error" ,JOptionPane.ERROR_MESSAGE);
                    }
                }
        } else if (currentWindow instanceof VisualizationPanel) {
                try {
                    ((VisualizationPanel)currentWindow).print(null, null, true, null, null, true);
                } catch (PrinterException e) {
                    JOptionPane.showMessageDialog(this.getFrame(), e.getMessage(),"Print Error" ,JOptionPane.ERROR_MESSAGE);
                }
        } else logMessage("Unable to print...");
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    /** Saves a document to file in a quick and dirty way... */
    private void savePlainDocumentToFile(javax.swing.text.PlainDocument document, File file) throws Exception {
        javax.swing.text.Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        java.io.PrintWriter outputStream=new java.io.PrintWriter(new java.io.FileWriter(file));
        for (int i=0;i<linecount;i++) {
              javax.swing.text.Element e=root.getElement(i);
              String line=document.getText(e.getStartOffset(),e.getEndOffset()-e.getStartOffset());
              line=line.trim();
              outputStream.println(line);
        }
        outputStream.close();
    }






    @Action(enabledProperty = "undoEnabled")
    public void undo() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        visualizationSettings.enableVisualizationSettingsNotifications(false); // disable to avoid redundant visualization updates
        UndoableEdit edit=undoManager.peek();
        if (edit instanceof ExecutableTask) {
            scheduler.abort((ExecutableTask)edit);
        }
        try {undoManager.undo();}
        catch (Exception e) {
            logMessage("Undo failed: "+e.getMessage()+" ("+e.getClass().toString()+")");
            //e.printStackTrace(System.err);
        }
        visualizationSettings.enableVisualizationSettingsNotifications(true); // reenable
        visualizationSettings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.VISIBILITY_CHANGED, null);
        visualizationSettings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REORDERED, engine.getDefaultSequenceCollection());
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action(enabledProperty = "redoEnabled")
    public void redo() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        visualizationSettings.enableVisualizationSettingsNotifications(false); // disable to avoid redundant visualization updates
        try {undoManager.redo();}
        catch (Exception e) {
            logMessage("Redo failed: "+e.getMessage());
            //e.printStackTrace(System.err)
        }
        visualizationSettings.enableVisualizationSettingsNotifications(true); // reenable
        visualizationSettings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REORDERED, engine.getDefaultSequenceCollection());
        getFrame().setCursor(Cursor.getDefaultCursor());
    }



    @Action
    public void helpAction() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String manualhomepage=engine.getWebSiteURL()+"documentation";
        try {
           java.awt.Desktop.getDesktop().browse(java.net.URI.create(manualhomepage));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getFrame(), "Unable to display help page in default web browser","Help", JOptionPane.ERROR_MESSAGE, null);
        }
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void goToWebSite() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String homepage=engine.getWebSiteURL();
        try {
           java.awt.Desktop.getDesktop().browse(java.net.URI.create(homepage));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getFrame(), "Unable to display web site in default web browser","Web Site Error", JOptionPane.ERROR_MESSAGE, null);
        }
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action
    public void quickstartTutorialAction() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String quickstart=engine.getWebSiteURL()+"tutorial";
        try {
           java.awt.Desktop.getDesktop().browse(java.net.URI.create(quickstart));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getFrame(), "Unable to display tutorial in default web browser","Web Site Error", JOptionPane.ERROR_MESSAGE, null);
        }
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action
    public void showKeyboardShortcutsAction() {
        String document=null;
        try {
            InputStream stream=this.getClass().getResourceAsStream("/motiflab/gui/resources/quickreference.html");
            document=MotifLabEngine.readTextFile(stream);
        } catch (Exception e) {
            document="<html><head></head><body>An error occurred while reading the keyboard shortcuts file: "+e.getMessage()+"</body></html>";
        }
        try {
            InfoDialog dialog=new InfoDialog(MotifLabGUI.this, "Keyboard shortcuts", document, 770,550);
            dialog.setVisible(true);
            dialog.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getFrame(), "Unable to display keyboard shortcuts","Web Site Error", JOptionPane.ERROR_MESSAGE, null);
        }

    }

    @Action
    public void showNotificationsAction() {
        displayNotifications(true,true);
    }

    public void showHelpDialog(String type, String topic) {
        String helpString="type="+type+"&topic="+topic;
        showHelpDialog(helpString);
    }

    public void showHelpDialog(String helpstring) {
        String urlstring=engine.getWebSiteURL()+"getHelp.php?"+helpstring;
        String errorString="No documentation available";
        URL url=null;
        try {url=new URL(urlstring);} catch (MalformedURLException e) {errorString=e.getMessage();}
        InfoDialog infodialog=null;
        if (url!=null) infodialog=new InfoDialog(this, "Help", url, 700, 450);
        else infodialog=new InfoDialog(this, "Help", errorString, 700, 450);
        infodialog.setErrorMessage(errorString);
        infodialog.setVisible(true);
        infodialog.dispose();
    }

    /** Displays a message in a popup dialog */
    public void alert(String message, int type) {
        String header="";
        if (type==JOptionPane.ERROR_MESSAGE) header="Error";
        else if (type==JOptionPane.WARNING_MESSAGE) header="Warning";
        JOptionPane.showMessageDialog(getFrame(), message, header, type);
    }

    // Implementation of MessageListener interface method */
    @Override
    public void errorMessage(final String msg, final int errortype) {
          Runnable runnable = new Runnable() {
              @Override
              public void run() {
                   try {
                        StyledDocument log=logPanel.getStyledDocument();
                        int pos=log.getLength();
                        log.insertString(pos, msg+"\n", null);
                        if (useColorsInLog) {
                            javax.swing.text.StyleContext sc = javax.swing.text.StyleContext.getDefaultStyleContext();
                            javax.swing.text.AttributeSet aset = sc.addAttribute(javax.swing.text.SimpleAttributeSet.EMPTY, javax.swing.text.StyleConstants.Foreground, Color.RED);
                            log.setCharacterAttributes(pos,msg.length(),aset, false);                                
                        }
                   } catch (Exception e) {System.err.println("ERROR: Unable to write message in LogPanel: '"+msg+"'");}
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
                        int pos=log.getLength();
                        log.insertString(pos, msg+"\n", null);
                        if (useColorsInLog && (msg.startsWith("NOTE:") || msg.contains("WARNING:") || msg.contains("Warning:") || msg.contains("ERROR:") || msg.contains("Error:"))) {
                            Color usecolor=Color.RED;
                            if (msg.startsWith("NOTE:")) usecolor=Color.BLUE;
                            else if (msg.contains("WARNING:") || msg.contains("Warning:")) usecolor=Color.MAGENTA;
                            javax.swing.text.StyleContext sc = javax.swing.text.StyleContext.getDefaultStyleContext();
                            javax.swing.text.AttributeSet aset = sc.addAttribute(javax.swing.text.SimpleAttributeSet.EMPTY, javax.swing.text.StyleConstants.Foreground, usecolor);
                            log.setCharacterAttributes(pos,msg.length(),aset, false);       
                        }                                            
                   } catch (Exception e) {System.err.println("ERROR: Unable to write message in LogPanel: '"+msg+"'");}
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

    public void logError(Throwable error) {
        if (stackDumpLevel==0) return; // do not stackdump errors at all at the lowest debugging level
        if ((error instanceof ExecutionError || error instanceof ParseError || error instanceof SystemError) && stackDumpLevel==1) return; // do not stackdump internal errors except at the highest debugging level
        logMessage("\n--------------- ERROR REPORT ----------------",50);
        logMessage(error.toString(),50);
        for (StackTraceElement line:error.getStackTrace()) {
            logMessage("     "+line.toString(),50);
        }
        Throwable cause=error.getCause();
        while (cause!=null) {
            logMessage("Caused by: "+cause.toString(),50);
            for (StackTraceElement line:cause.getStackTrace()) {
                logMessage("     "+line.toString(),50);
            }
            cause=cause.getCause();
        }
    }

    // abandoned interface methog
    public void outputMessage(final String msg) {
        logMessage(msg);
    }

    // Implementation of MessageListener interface method
    @Override
    public void statusMessage(final String msg) {
        PropertyChangeEvent event=new PropertyChangeEvent(engine, ExecutableTask.STATUS_MESSAGE, msg, msg);
        progressMonitor.propertyChange(event);
        if (loggerLevel==4 && !msg.isEmpty()) logMessage(msg, 0);
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
     *  (Implementation of MotifLabClient interface method)
     */
    public void setProgress(final int progress) {
        String property=((progress>=0 && progress<=100)||progress==Integer.MAX_VALUE)?ExecutableTask.PROGRESS:"HIDE";
        PropertyChangeEvent event=new PropertyChangeEvent(engine, property, progress, progress);
        progressMonitor.propertyChange(event);
    }



    /** Sets the value of the progressbar directly. This method should only be called from the EDT
     *  when it is essential to update the progressbar right away
     *  The value should be between 0 and 100
     *  If provided value is outside this range, the progress bar will be hidden
     */
    public void setProgressDirectly(final int progress, String text) {
        if (progress>=0 && progress<=100) {
            progressBar.setVisible(true);
            progressBar.setValue(progress);
            progressBar.setString(text);
            java.awt.Rectangle bounds=progressBar.getBounds();
            bounds.height=20;
            bounds.width=200;
            progressBar.repaint(progressBar.getBounds());
        } else {
            //progressBar.setVisible(false);
            progressBar.setValue(0);
            progressBar.setString("");
            java.awt.Rectangle bounds=progressBar.getBounds();
            bounds.height=20;
            bounds.width=200;
            progressBar.repaint(progressBar.getBounds());
        }
    }


    /** Implementation of MessageListener interface method */
    public void debugMessage(final String msg) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
               try {
                  StyledDocument log=logPanel.getStyledDocument();
                  log.insertString(log.getLength(), msg+"\n", null);
               } catch (Exception e) {System.err.println("ERROR: Unable to write message in LogPanel: '"+msg+"'");}
            } // end run()
          }; // end Runnable
          if (SwingUtilities.isEventDispatchThread()) runnable.run(); // invoke directly on EDT
          else {SwingUtilities.invokeLater(runnable);} // queue on EDT
    }


    @Action
    public void debugAction() {
        getVisualizationSettings().debug();
        undoManager.debug();
    }


    @Action
    public void selectionToolAction() {
        setSelectedTool(SELECTION_TOOL);
        selectionToolButton.setSelected(true);
    }

    @Action
    public void handToolAction() {
        setSelectedTool(MOVE_TOOL);
        dragToolButton.setSelected(true);
    }

    @Action
    public void zoomToolAction() {
        setSelectedTool(ZOOM_TOOL);
        zoomToolButton.setSelected(true);
    }

    @Action
    public void drawToolAction() {
        setSelectedTool(DRAW_TOOL);
        drawToolButton.setSelected(true);
    }




    private DefaultComboBoxModel getModelForZoomLevelCombobox() {
        double[] zoomlevels=getVisualizationSettings().getZoomChoices();
        Object[] elements=new Object[zoomlevels.length+2];
        Object selected=null;
        for (int i=0;i<zoomlevels.length;i++) {
            Double val=new Double(zoomlevels[i]);
            elements[i]=val;
            if (val==100) selected=val;

        }
        elements[elements.length-2]="To Fit";
        elements[elements.length-1]="Custom...";
        DefaultComboBoxModel model=new DefaultComboBoxModel(elements);
        model.setSelectedItem(selected);
        return model;

    }

    /** Returns the Action with the given name (if it exists) */
    public javax.swing.Action getAction(String actionname) {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        return actionMap.get(actionname);
    }

    public void setRecordEnabled(boolean enabled) {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("recordMode").setEnabled(enabled);
    }
    public void setExecuteEnabled(boolean enabled) {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("executeProtocol").setEnabled(enabled);
        actionMap.get("executeCurrentProtocolLine").setEnabled(enabled);
        actionMap.get("executeCurrentProtocolSelection").setEnabled(enabled);
        actionMap.get("executeProtocolFromCurrentLine").setEnabled(enabled);
    }
    public void setStopEnabled(boolean enabled) {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("stopAction").setEnabled(enabled);
    }


    @Action
    public void recordMode() {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("executeProtocol").setEnabled(false);
        actionMap.get("executeCurrentProtocolLine").setEnabled(false);
        actionMap.get("executeCurrentProtocolSelection").setEnabled(false);
        actionMap.get("executeProtocolFromCurrentLine").setEnabled(false);
        actionMap.get("stopAction").setEnabled(true);
        recordButton.setSelected(true);
        setRecordMode(true);

    }

    @Action
    public void executeProtocol() {
        getProtocolEditor().setException(null);
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        Protocol protocol=protocolEditor.getProtocol();
        if (protocol.getDocument().getLength()==0) {
            recplaystopButtonGroup.clearSelection();
            actionMap.get("executeProtocol").setEnabled(true);
            actionMap.get("executeCurrentProtocolLine").setEnabled(true);
            actionMap.get("executeCurrentProtocolSelection").setEnabled(true);
            actionMap.get("executeProtocolFromCurrentLine").setEnabled(true);
            actionMap.get("recordMode").setEnabled(true);
            actionMap.get("stopAction").setEnabled(false);
            statusMessage("Protocol script is emtpy");
            return;
        }
        actionMap.get("recordMode").setEnabled(false);
        actionMap.get("executeProtocol").setEnabled(false);
        actionMap.get("executeCurrentProtocolLine").setEnabled(false);
        actionMap.get("executeCurrentProtocolSelection").setEnabled(false);
        actionMap.get("executeProtocolFromCurrentLine").setEnabled(false);
        actionMap.get("stopAction").setEnabled(true);
        executeButton.setSelected(true);
        try {
            statusMessage("Parsing protocol script... ");
            ProtocolTask task=protocol.parse();
            statusMessage("Parsing protocol script... Completed!");
            boolean usesFeatureData=protocol.getDataTypeLookupTable().hasDataItemsOfType(FeatureDataset.class);
            boolean sequencesAvailable=(engine.getDefaultSequenceCollection().size()>0 || protocol.getDataTypeLookupTable().hasDataItemsOfType(Sequence.class) || protocol.getDataTypeLookupTable().isDefaultSequenceCollectionAssigned()); // sequences already available or defined in Protocol
            if (usesFeatureData && !sequencesAvailable) { // prompt for sequences if the protocol needs them and none are defined
               showSequenceInputDialog(); // ask the user to specify sequences if there is none and launch an AddSequencesTask
            }
            UndoMonitor monitor=undoManager.getUndoMonitor(task.getPresentationName());
            task.setUndoMonitor(monitor);
            undoManager.addEdit(task);
            task.addPropertyChangeListener(new protocolTaskListener(actionMap));
            scheduler.submit(task);
        } catch (ParseError e) {
            String errorpanemessage=e.getMessage();
            if (e.getLineNumber()!=0) errorpanemessage="Line "+e.getLineNumber()+":\n"+errorpanemessage;
            JOptionPane.showMessageDialog(this.getFrame(), errorpanemessage,"Parse Error" ,JOptionPane.ERROR_MESSAGE);
            String errormessage;
            if (e.getLineNumber()!=0) errormessage="Parse Error [line "+e.getLineNumber()+"]:"+e.getMessage();
            else errormessage="Parse Error :"+e.getMessage();
            errorMessage(errormessage, 0);
            recplaystopButtonGroup.clearSelection();
            actionMap.get("executeProtocol").setEnabled(true);
            actionMap.get("executeCurrentProtocolLine").setEnabled(true);
            actionMap.get("executeCurrentProtocolSelection").setEnabled(true);
            actionMap.get("executeProtocolFromCurrentLine").setEnabled(true);
            actionMap.get("recordMode").setEnabled(true);
            actionMap.get("stopAction").setEnabled(false);
        }
    }

    @Action
    public void executeCurrentProtocolLine() {
        protocolEditor.executeCurrentLine();
    }

    @Action
    public void executeProtocolFromCurrentLine() {
        int currentLine = protocolEditor.getCurrentLine()-1;
        int lastLine = protocolEditor.getNumberOfLines()-1;
        executeLinesFromCurrentProtocol(new int[]{currentLine,lastLine});
    }

    @Action
    public void executeCurrentProtocolSelection() {
        int[] selectedLines=protocolEditor.getSelectedLines();
        if (selectedLines==null) {
            logMessage("No protocol lines selected");
            return;
        }
        executeLinesFromCurrentProtocol(selectedLines);
    }

    private void executeLinesFromCurrentProtocol(int[] selectedLines) {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        Protocol protocol=protocolEditor.getProtocol();
        if (protocol.getDocument().getLength()==0) {
            recplaystopButtonGroup.clearSelection();
            actionMap.get("executeProtocol").setEnabled(true);
            actionMap.get("executeCurrentProtocolLine").setEnabled(true);
            actionMap.get("executeCurrentProtocolSelection").setEnabled(true);
            actionMap.get("executeProtocolFromCurrentLine").setEnabled(true);
            actionMap.get("recordMode").setEnabled(true);
            actionMap.get("stopAction").setEnabled(false);
            statusMessage("Protocol script is emtpy");
            return;
        }
        actionMap.get("recordMode").setEnabled(false);
        actionMap.get("executeProtocol").setEnabled(false);
        actionMap.get("executeCurrentProtocolLine").setEnabled(false);
        actionMap.get("executeCurrentProtocolSelection").setEnabled(false);
        actionMap.get("executeProtocolFromCurrentLine").setEnabled(false);
        actionMap.get("stopAction").setEnabled(true);
        executeButton.setSelected(true);
        try {
            statusMessage("Parsing protocol script... ");
            ProtocolTask task=protocol.parse(selectedLines[0],selectedLines[1]);
            statusMessage("Parsing protocol script... Completed!");
            boolean usesFeatureData=protocol.getDataTypeLookupTable().hasDataItemsOfType(FeatureDataset.class);
            boolean sequencesAvailable=(engine.getDefaultSequenceCollection().size()>0 || protocol.getDataTypeLookupTable().hasDataItemsOfType(Sequence.class) || protocol.getDataTypeLookupTable().isDefaultSequenceCollectionAssigned()); // sequences already available or defined in Protocol
            if (usesFeatureData && !sequencesAvailable) { // prompt for sequences if the protocol needs them and none are defined
               showSequenceInputDialog(); // ask the user to specify sequences if there is none and launch an AddSequencesTask
            }
            UndoMonitor monitor=undoManager.getUndoMonitor(task.getPresentationName());
            task.setUndoMonitor(monitor);
            undoManager.addEdit(task);
            task.addPropertyChangeListener(new protocolTaskListener(actionMap));
            scheduler.submit(task);
        } catch (ParseError e) {
            String errorpanemessage=e.getMessage();
            if (e.getLineNumber()!=0) errorpanemessage="Line "+e.getLineNumber()+":\n"+errorpanemessage;
            JOptionPane.showMessageDialog(this.getFrame(), errorpanemessage,"Parse Error",JOptionPane.ERROR_MESSAGE);
            String errormessage;
            if (e.getLineNumber()!=0) errormessage="Parse Error [line "+e.getLineNumber()+"]:"+e.getMessage();
            else errormessage="Parse Error :"+e.getMessage();
            errorMessage(errormessage, 0);
            recplaystopButtonGroup.clearSelection();
            actionMap.get("executeProtocol").setEnabled(true);
            actionMap.get("executeCurrentProtocolLine").setEnabled(true);
            actionMap.get("executeCurrentProtocolSelection").setEnabled(true);
            actionMap.get("executeProtocolFromCurrentLine").setEnabled(true);
            actionMap.get("recordMode").setEnabled(true);
            actionMap.get("stopAction").setEnabled(false);
        }
    }

    @Action
    public void commentSelectedLinesInProtocol() {
        protocolEditor.commentSelectedLines();
    }

    @Action
    public void uncommentSelectedLinesInProtocol() {
        protocolEditor.uncommentSelectedLines();
    }

    @Action
    public void expandMacrosInCurrentProtocol() {
        mainWindow.showProtocolEditor();
        protocolEditor.expandMacrosInCurrentProtocol();
    }

    public void enableCommentSelectedLinesInProtocol(boolean enabled) {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("commentSelectedLinesInProtocol").setEnabled(enabled);
        actionMap.get("uncommentSelectedLinesInProtocol").setEnabled(enabled);
    }

    /** Returns TRUE if the scheduler is currently in the process of executing a Protocol task */
    public boolean isExecutingProtocol() {
        return scheduler.isRunningProtocol();
    }

    /** Sets the selected status of the Record, Play and Stop buttons to false */
    public void unselectRecordPlayStop() {
        recplaystopButtonGroup.clearSelection();
    }

    /** This class tracks the progress of Protocol tasks to reenable some disabled actions when the task is done*/
    private class protocolTaskListener implements PropertyChangeListener {
        javax.swing.ActionMap actionMap;
        public protocolTaskListener(javax.swing.ActionMap actionMap) {this.actionMap=actionMap;}
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!evt.getPropertyName().equals(ExecutableTask.STATUS)) return;
            String value=(String)evt.getNewValue();
            if (value.equals(ExecutableTask.DONE) || value.equals(ExecutableTask.ABORTED) || value.equals(ExecutableTask.ERROR)) {
                recplaystopButtonGroup.clearSelection();
                actionMap.get("executeProtocol").setEnabled(true);
                actionMap.get("executeCurrentProtocolLine").setEnabled(true);
                actionMap.get("executeCurrentProtocolSelection").setEnabled(true);
                actionMap.get("executeProtocolFromCurrentLine").setEnabled(true);
                actionMap.get("recordMode").setEnabled(true);
                actionMap.get("stopAction").setEnabled(false);
            }
        }
    }

    /** This class tracks the progress of a single task which is executed in the GUI.
     */
    private class singleTaskListener implements PropertyChangeListener {
        public singleTaskListener() {}
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!evt.getPropertyName().equals(ExecutableTask.STATUS)) return;
            String value=(String)evt.getNewValue();
            if (value.equals(ExecutableTask.DONE)) {
                Object source=evt.getSource();
                if (source instanceof OperationTask) {
                    OperationTask task=(OperationTask)source;
                    Object show=task.getParameter("_SHOW_RESULTS");
                    if (show!=null && show instanceof Boolean && !((Boolean)show).booleanValue()) return;
                    String targetName=task.getTargetDataName();
                    final Data target=engine.getDataItem(targetName);
                    if (target!=null) {
                        if (motifsPanel.holdsType(target)) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    motifsPanel.showPrompt(target,false,false);
                                }
                            });
                        } else if (dataObjectsPanel.holdsType(target)) {
                             SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    dataObjectsPanel.showPrompt(target,false,false);
                                }
                            });
                        } else if (target instanceof Sequence) {
                             SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    showPrompt(target,false,false);
                                }
                            });
                        }
                    } // end: (target!=null)
                } // end: (source instanceof OperationTask)
            } // end: task==DONE
        }

    }

    @Action
    public void stopAction() {
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("stopAction").setEnabled(false);
        actionMap.get("executeProtocol").setEnabled(false);
        actionMap.get("executeCurrentProtocolLine").setEnabled(false);
        actionMap.get("executeCurrentProtocolSelection").setEnabled(false);
        actionMap.get("executeProtocolFromCurrentLine").setEnabled(false);
        actionMap.get("stopAction").setEnabled(false);
        if (recording) { // record mode
            recplaystopButtonGroup.clearSelection();
            actionMap.get("executeProtocol").setEnabled(true);
            actionMap.get("executeCurrentProtocolLine").setEnabled(true);
            actionMap.get("executeCurrentProtocolSelection").setEnabled(true);
            actionMap.get("executeProtocolFromCurrentLine").setEnabled(true);
            actionMap.get("recordMode").setEnabled(true);
            actionMap.get("stopAction").setEnabled(false);
            setRecordMode(false);
        }
        else { // play mode (executing script)
            boolean aborted=abortExecution();
            if (!aborted) {
                recplaystopButtonGroup.clearSelection();
                actionMap.get("executeProtocol").setEnabled(false);
                actionMap.get("executeCurrentProtocolLine").setEnabled(false);
                actionMap.get("executeCurrentProtocolSelection").setEnabled(false);
                actionMap.get("executeProtocolFromCurrentLine").setEnabled(false);
                executeButton.setSelected(true);
                actionMap.get("recordMode").setEnabled(false);
                actionMap.get("stopAction").setEnabled(true);
            } else {
                recplaystopButtonGroup.clearSelection();
                actionMap.get("executeProtocol").setEnabled(true);
                actionMap.get("executeCurrentProtocolLine").setEnabled(true);
                actionMap.get("executeCurrentProtocolSelection").setEnabled(true);
                actionMap.get("executeProtocolFromCurrentLine").setEnabled(true);
                actionMap.get("recordMode").setEnabled(true);
                actionMap.get("stopAction").setEnabled(false);
            }
        }

    }


    @Action
    public void showMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapmemory= memoryBean.getHeapMemoryUsage();
        MemoryUsage nonheapmemory= memoryBean.getNonHeapMemoryUsage();
        debugMessage("Memory usage\n===========\n\nHEAP\n  "+heapmemory.toString()+"\n\nNON-HEAP\n  "+nonheapmemory.toString());
    }

    private boolean recording = false;
    /**
     * Returns true if the system is currently in "RECORD" mode,
     * meaning that any operations executed should be logged in the protocol script
     * @return
     */
    public boolean isRecording() {
        return recording;
    }
    /**
     * Sets the value of the "recording" flag
     * @return
     */
    public void setRecordMode(boolean isRecording) {
        recording=isRecording;
    }

    private boolean saveEnabled = false;
    public boolean isSaveEnabled() {
        return saveEnabled;
    }
    public void setSaveEnabled(boolean b) {
        boolean old = isSaveEnabled();
        this.saveEnabled = b;
        firePropertyChange("saveEnabled", old, isSaveEnabled());
    }

    private boolean saveAsEnabled = false;
    public boolean isSaveAsEnabled() {
        return saveAsEnabled;
    }
    public void setSaveAsEnabled(boolean b) {
        boolean old = isSaveAsEnabled();
        this.saveAsEnabled = b;
        firePropertyChange("saveAsEnabled", old, isSaveAsEnabled());
    }
    private boolean saveAllEnabled = false;
    public boolean isSaveAllEnabled() {
        return saveAllEnabled;
    }
    public void setSaveAllEnabled(boolean b) {
        boolean old = isSaveAllEnabled();
        this.saveAllEnabled = b;
        firePropertyChange("saveAllEnabled", old, isSaveAllEnabled());
    }

    private boolean closeEnabled = false;
    public boolean isCloseEnabled() {
        return closeEnabled;
    }
    public void setCloseEnabled(boolean b) {
        boolean old = isCloseEnabled();
        this.closeEnabled = b;
        firePropertyChange("closeEnabled", old, isCloseEnabled());
    }



    private boolean redoEnabled = false;
    public boolean isRedoEnabled() {
        return redoEnabled;
    }

    public void setRedoEnabled(boolean b) {
        boolean old = isRedoEnabled();
        this.redoEnabled = b;
        firePropertyChange("redoEnabled", old, isRedoEnabled());
    }



    private boolean undoEnabled = false;
    public boolean isUndoEnabled() {
        return undoEnabled;
    }

    public void setUndoEnabled(boolean b) {
        boolean old = isUndoEnabled();
        this.undoEnabled = b;
        firePropertyChange("undoEnabled", old, isUndoEnabled());
    }

    /** This should be called whenever undos/redos are made so that actions are updated properly*/
    public void updateUndoRedoStates() {
        //undoManager.debug();
        if (undoManager.canUndo()) {// debugMessage("Can undo");
                setUndoEnabled(true);
                undoButton.setToolTipText(undoManager.getUndoPresentationName());
            } else {// debugMessage("Can not undo");
                setUndoEnabled(false);
                undoButton.setToolTipText(null);
            }

            if (undoManager.canRedo()) { //System.err.println("Can redo");
                setRedoEnabled(true);
                redoButton.setToolTipText(undoManager.getRedoPresentationName());
            } else { //System.err.println("Can not redo");
                setRedoEnabled(false);
                redoButton.setToolTipText(null);
            }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem PositionalDistributionViewerMenuItem;
    private javax.swing.JToolBar Toolbar;
    private javax.swing.JMenuItem addBackgroundFrequenciesMenuItem;
    private javax.swing.JMenuItem addBackgroundMenuItem;
    private javax.swing.JMenuItem addDataTracksMenuItem;
    private javax.swing.JMenuItem addExpressionProfileMenuItem;
    private javax.swing.JMenuItem addModuleCollectionMenuItem;
    private javax.swing.JMenuItem addModuleMapMenuItem;
    private javax.swing.JMenuItem addModuleMenuItem;
    private javax.swing.JMenuItem addMotifCollectionMenuItem;
    private javax.swing.JMenuItem addMotifMapMenuItem;
    private javax.swing.JMenuItem addMotifMenuItem;
    private javax.swing.JMenuItem addMotifPartitionMenuItem;
    private javax.swing.JMenu addNewDataItemMenu;
    private javax.swing.JMenuItem addNumericMapVariableMenuItem;
    private javax.swing.JMenuItem addNumericVariableMenuItem;
    private javax.swing.JMenuItem addPriorsGeneratorMenuItem;
    private javax.swing.JMenuItem addSequenceCollectionMenuItem;
    private javax.swing.JMenuItem addSequenceMapMenuItem;
    private javax.swing.JMenuItem addSequencePartitionMenuItem;
    private javax.swing.JMenuItem addSequencesFromFileMenuItem;
    private javax.swing.JMenuItem addSequencesMenuItem;
    private javax.swing.JMenuItem addTextVariableMenuItem;
    private javax.swing.JMenuItem backupConfigurationMenuItem;
    private javax.swing.JPanel centerArea;
    private javax.swing.JMenu changeProtocolSubmenu;
    private javax.swing.JMenuItem citationsMenuItem;
    private javax.swing.JMenuItem notificationsMenuItem;
    private javax.swing.JMenuItem clearAllDataMenuItem;
    private javax.swing.JMenu clearDataSubmenu;
    private javax.swing.JMenuItem clearFeatureDataMenuItem;
    private javax.swing.JLabel clearLogLabel;
    private javax.swing.JMenuItem clearModulesMenuItem;
    private javax.swing.JMenuItem clearMotifsAndModulesMenuItem;
    private javax.swing.JMenuItem clearOtherDataMenuItem;
    private javax.swing.JMenuItem clearSequencesAndFeaturesMenuItem;
    private javax.swing.JCheckBoxMenuItem colorKeywordsMenuItem;
    private javax.swing.JMenuItem commentSelectedLines;
    private javax.swing.JMenuItem configureDatatracksMenuItem;
    private javax.swing.JMenu configureMenu;
    private javax.swing.JPopupMenu.Separator configureMenuSeparator1;
    private javax.swing.JMenuItem configureOrganismsAndIdentifiersMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JButton copybutton;
    private javax.swing.JMenuItem cropSequencesMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JButton cutbutton;
    private javax.swing.JMenu dataMenu;
    private javax.swing.JSeparator dataMenuSeparator1;
    private javax.swing.JButton databaseButton;
    private javax.swing.JMenuItem datarepositoriesMenuItem;
    private javax.swing.JToggleButton dragToolButton;
    private javax.swing.JToggleButton drawToolButton;
    private javax.swing.JMenuItem drawToolMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JSeparator editMenuSeparator1;
    private javax.swing.JPopupMenu.Separator editMenuSeparator2;
    private javax.swing.JMenu exampleSessionsMenu;
    private javax.swing.JToggleButton executeButton;
    private javax.swing.JMenuItem executeCurrentLineMenuItem;
    private javax.swing.JMenuItem executeCurrentSelectionMenuItem;
    private javax.swing.JMenuItem executeFromCurrentLineMenuItem;
    private javax.swing.JMenuItem executeMenuItem;
    private javax.swing.JMenuItem expandMacrosMenuItem;
    private javax.swing.JMenuItem extendSequencesMenuItem;
    private javax.swing.JMenuItem externalProgramMenuItem;
    private javax.swing.JMenuItem favoritesMenuItem;
    private javax.swing.JMenuItem fileCloseMenuItem;
    private javax.swing.JSeparator fileMenuSeparator1;
    private javax.swing.JSeparator fileMenuSeparator2;
    private javax.swing.JSeparator fileMenuSeparator3;
    private javax.swing.JSeparator fileMenuSeparator4;
    private javax.swing.JMenuItem fileNewMenuItem;
    private javax.swing.JMenuItem fileOpenFromURLMenuItem;
    private javax.swing.JMenuItem fileOpenMenuItem;
    private javax.swing.JMenu fileOpenRecentSubmenu;
    private javax.swing.JMenuItem filePrintMenuItem;
    private javax.swing.JMenu fileRestoreRecentSessionSubmenu;
    private javax.swing.JMenuItem fileRestoreSessionFromURLMenuItem;
    private javax.swing.JMenuItem fileRestoreSessionMenuItem;
    private javax.swing.JMenuItem fileSaveAllMenuItem;
    private javax.swing.JMenuItem fileSaveAndExitMenuItem;
    private javax.swing.JMenuItem fileSaveAsImageMenuItem;
    private javax.swing.JMenuItem fileSaveAsMenuItem;
    private javax.swing.JMenuItem fileSaveMenuItem;
    private javax.swing.JMenuItem fileSaveSessionMenuItem;
    private javax.swing.JMenuItem findMenuItem;
    private javax.swing.JMenuItem handToolMenuItem;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JSplitPane horisontalSplitPane;
    private javax.swing.JMenuItem importFromFileMenuItem;
    private javax.swing.JMenuItem installConfigFileMenuItem;
    private javax.swing.JMenuItem interactionFilterMenuItem;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem keyboardshortcutsMenuItem;
    private javax.swing.JPanel logLeftPanel;
    private javax.swing.JLabel logLevelIcon;
    private javax.swing.JTextPane logPanel;
    private javax.swing.JPanel logRightPanel;
    private javax.swing.JScrollPane logScrollPane;
    private javax.swing.JPanel logTopPanel;
    private javax.swing.JMenuItem macroEditorMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem moduleBrowserMenuItem;
    private javax.swing.JMenuItem motifBrowserMenuItem;
    private javax.swing.JMenuItem motifScoreFilterMenuItem;
    private javax.swing.JButton newButton;
    private javax.swing.JButton openButton;
    private javax.swing.JMenu operationsMenu;
    private javax.swing.JMenuItem optionsMenuItem;
    private javax.swing.JButton pasteButton;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JMenuItem pluginsMenuItem;
    private javax.swing.JButton printButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenu protocolMenu;
    private javax.swing.JSeparator protocolMenuSeparator1;
    private javax.swing.JPopupMenu.Separator protocolMenuSeparator2;
    private javax.swing.JPopupMenu.Separator protocolMenuSeparator3;
    private javax.swing.JMenuItem quickTutorialMenuItem;
    private javax.swing.JToggleButton recordButton;
    private javax.swing.JMenuItem recordMenuItem;
    private javax.swing.ButtonGroup recplaystopButtonGroup;
    private javax.swing.JButton redoButton;
    private javax.swing.JMenuItem redoMenuItem;
    private javax.swing.JMenuItem regionBrowserMenuItem;
    private javax.swing.JMenuItem replaceMenuItem;
    private javax.swing.JButton saveButton;
    private javax.swing.ButtonGroup selectedToolButtonGroup;
    private javax.swing.JToggleButton selectionToolButton;
    private javax.swing.JMenuItem selectionToolMenuItem;
    private javax.swing.JMenuItem sequenceBrowserMenuItem;
    private javax.swing.JButton sequenceButton;
    private javax.swing.JButton showModuleBrowserButton;
    private javax.swing.JButton showMotifBrowserButton;
    private javax.swing.JButton showSequenceBrowserButton;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JMenuItem sortSequencesToolMenuItem;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JToggleButton stopButton;
    private javax.swing.JMenuItem stopMenuItem;
    private javax.swing.JPanel toolbarSearchPanel;
    private javax.swing.JToolBar.Separator toolbarSeparator1;
    private javax.swing.JToolBar.Separator toolbarSeparator2;
    private javax.swing.JToolBar.Separator toolbarSeparator3;
    private javax.swing.JToolBar.Separator toolbarSeparator4;
    private javax.swing.JToolBar.Separator toolbarSeparator5;
    private javax.swing.JToolBar.Separator toolbarSeparator6;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JSeparator toolsMenuSeparator1;
    private javax.swing.JPopupMenu.Separator toolsMenuSeparator2;
    private javax.swing.JPopupMenu.Separator toolsMenuSeparator3;
    private javax.swing.JPanel topPanel;
    private javax.swing.JMenuItem uncommentSelectedLines;
    private javax.swing.JButton undoButton;
    private javax.swing.JMenuItem undoMenuItem;
    private javax.swing.JMenuItem updateMotifsMenuItem;
    private javax.swing.JSplitPane verticalSplitPane;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JMenuItem welcomeMenuItem;
    private javax.swing.JComboBox zoomLevelCombobox;
    private javax.swing.JToggleButton zoomToolButton;
    private javax.swing.JMenuItem zoomToolMenuItem;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    private javax.swing.JMenu viewInMainWindowMenu; // lists open tabs in main window in the view menu



    /**
     * This method is used by Operation_prompt (via the other promptValue method)
     * @param item
     * @param message
     * @param editable
     * @return
     * @throws ExecutionError
     */
    public Data promptValue(final Data item, final String message, final boolean editable, PromptConstraints constraint) throws ExecutionError {
        final Data[] buffer=new Data[1];
        if (item instanceof Analysis) buffer[0]=item; // analyses can not be edited anyway, so we do not have to clone it
        else buffer[0]=item.clone();
        final Prompt prompt=getPrompt(buffer[0], message, true, constraint);
        if (prompt==null) throw new ExecutionError("Cannot prompt for value for data of type "+item.getTypeDescription());
        if (!editable) prompt.setDataEditable(false);
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                int x=getGUIFrame().getWidth()/2-prompt.getWidth()/2; if (x<0) x=0;
                int y=getGUIFrame().getHeight()/2-prompt.getHeight()/2; if (y<0) y=0;
                prompt.setLocation(x,y);
                prompt.disableNameEdit();
                prompt.setVisible(true); // these prompts are always modal (last argument in constructors is TRUE)
                if (editable && prompt.isOKPressed()) {
                  buffer[0]=prompt.getData();
                }
                prompt.dispose();
                redraw(); // updates other panels that might reference data that has been altered
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else try {
            SwingUtilities.invokeAndWait(runner); // this will block until the runner is finished!
        } catch (Exception ie) {engine.logMessage("Warning: "+ie.getMessage());}
        return buffer[0];
    }

    @Override
    public Data promptValue(final Data item, final String message, final PromptConstraints constraint) throws ExecutionError {
        boolean editable=true;
        if (item instanceof Analysis) editable=false;
        return promptValue(item, message,editable,constraint);
    }


    private int lastPrompt_X=-1, lastPrompt_Y=-1;

    /** Shows a prompt to edit the selected data object */
    public void showPrompt(final Data item, boolean editable, boolean modal) {
         final Data[] buffer=new Data[1];
         if (item instanceof Analysis) buffer[0]=item; // analyses can not be edited anyway, so we do not have to clone it
         else buffer[0]=item.clone();
         Prompt prompt=getPrompt(buffer[0],null,modal, null);
         if (prompt==null) {logMessage("Unknown data type "+item.getTypeDescription()); return;}

         int promptX=getGUIFrame().getWidth()/2-prompt.getWidth()/2;
         int promptY=getGUIFrame().getHeight()/2-prompt.getHeight()/2;
         if (promptX==lastPrompt_X && promptY==lastPrompt_Y) { // select a different location
           promptX+=16;
           promptY+=16;
         }
         prompt.setLocation(promptX, promptY);
         lastPrompt_X=promptX;
         lastPrompt_Y=promptY;
         prompt.disableNameEdit();
         prompt.setDataEditable(editable);
         if (editable) {
             prompt.setCallbackOnOKPressed(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  Prompt prompt=(Prompt)e.getSource();
                  if (prompt.isOKPressed()) {
                     Data data=prompt.getData();
                     prompt.dispose();
                     if (data!=null && !data.containsSameData(item)) updateDataItem(data);
                  }
                }
             });
         }
         prompt.setVisible(true);
    }


    private Prompt getPrompt(Data item, String message, boolean modal, PromptConstraints constraint) {
        Prompt prompt=null;
             if (item instanceof SequenceCollection) prompt=new Prompt_SequenceCollection(this, message, (SequenceCollection)item, modal);
        else if (item instanceof SequencePartition) prompt=new Prompt_SequencePartition(this, message, (SequencePartition)item, modal);
        else if (item instanceof NumericVariable) prompt=new Prompt_NumericVariable(this, message, (NumericVariable)item, modal);
        else if (item instanceof SequenceNumericMap) prompt=new Prompt_SequenceNumericMap(this, message, (SequenceNumericMap)item, modal);
        else if (item instanceof MotifNumericMap) prompt=new Prompt_MotifNumericMap(this, message, (MotifNumericMap)item, modal);
        else if (item instanceof ModuleNumericMap) prompt=new Prompt_ModuleNumericMap(this, message, (ModuleNumericMap)item, modal);
        else if (item instanceof SequenceTextMap) prompt=new Prompt_SequenceTextMap(this, message, (SequenceTextMap)item, modal);
        else if (item instanceof MotifTextMap) prompt=new Prompt_MotifTextMap(this, message, (MotifTextMap)item, modal);
        else if (item instanceof ModuleTextMap) prompt=new Prompt_ModuleTextMap(this, message, (ModuleTextMap)item, modal);
        else if (item instanceof BackgroundModel) prompt=new Prompt_BackgroundModel(this, message, (BackgroundModel)item, modal);
        else if (item instanceof TextVariable) prompt=new Prompt_TextVariable(this, message, (TextVariable)item, modal);
        else if (item instanceof Sequence) prompt=new Prompt_Sequence(this, message, (Sequence)item,visualizationSettings, modal);
        else if (item instanceof Motif) prompt=new Prompt_Motif(this, message, (Motif)item,visualizationSettings, modal);
        else if (item instanceof ModuleCRM) prompt=new Prompt_Module(this, message, (ModuleCRM)item, modal);
        else if (item instanceof MotifCollection) prompt=new Prompt_MotifCollection(this, message, (MotifCollection)item, modal);
        else if (item instanceof MotifPartition) prompt=new Prompt_MotifPartition(this, message, (MotifPartition)item, modal);
        else if (item instanceof ModuleCollection) prompt=new Prompt_ModuleCollection(this, message, (ModuleCollection)item, modal);
        else if (item instanceof ModulePartition) prompt=new Prompt_ModulePartition(this, message, (ModulePartition)item, modal);
        else if (item instanceof ExpressionProfile) prompt=new Prompt_ExpressionProfile(this, message, (ExpressionProfile)item, modal);
        else if (item instanceof PriorsGenerator) prompt=new Prompt_PriorsGenerator(this, message, (PriorsGenerator)item, modal);
        else if (item instanceof RegionDataset) prompt=new Prompt_RegionDataset(this, message, ((RegionDataset)item), modal);
        else if (item instanceof NumericDataset) prompt=new Prompt_NumericDataset(this, message, (NumericDataset)item, modal);
        else if (item instanceof DNASequenceDataset) prompt=new Prompt_DNASequenceDataset(this, message, (DNASequenceDataset)item, modal);
        else if (item instanceof Analysis) prompt=((Analysis)item).getPrompt(this, modal);
        if (prompt!=null && constraint!=null) prompt.setConstraints(constraint);
        return prompt;
    }

    // ------------------------------------- PRIVATE CLASSES ---------------------------------------  
    
    /**
     * Renderer for values in the Zoom Combobox in the toolbar
     */
    private class ZoomLevelComboboxCellRenderer extends JLabel implements ListCellRenderer {

        public ZoomLevelComboboxCellRenderer() {
            setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        }
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setOpaque(true);
            if (isSelected) {
                setForeground(list.getSelectionForeground());
                setBackground(list.getSelectionBackground());
            }
            else {
                setForeground(list.getForeground());
                setBackground(java.awt.Color.WHITE);
            }

            if (value instanceof Double) {
                int val=(int)(((Double)value).doubleValue());
                setText(""+val+" %");
                setHorizontalAlignment(JLabel.RIGHT);
            } else {
                setText(value.toString());
                setHorizontalAlignment(JLabel.LEFT);
            }
            return this;
        } // end getListCellRendererComponent
    }


    /**
     * ActionListener the Zoom Combobox in the toolbar
     */
    private class ZoomLevelComboboxActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object selection=zoomLevelCombobox.getSelectedItem();
            VisualizationSettings settings=getVisualizationSettings();
            if (selection instanceof Double) {
                 settings.setGlobalZoomLevel((Double)selection);
                 visualizationPanel.zoomToLevelOnAllSequences((Double)selection);
            } else if (selection.toString().equals("To Fit")) {
                 visualizationPanel.zoomToFitOnAllSequences();
            } else if (selection.toString().equals("Custom...")) {
              String newZoomString=(String)JOptionPane.showInputDialog(MotifLabGUI.this.getFrame(),"Enter new zoom level","Zoom to custom level",JOptionPane.PLAIN_MESSAGE,null,null,100.0);
              try {
                 double zoomlevel=Double.parseDouble(newZoomString);
                 if (zoomlevel>getVisualizationSettings().getSequenceWindowSize()*100) zoomlevel=settings.getSequenceWindowSize()*100;
                 settings.setGlobalZoomLevel(zoomlevel);
                 visualizationPanel.zoomToLevelOnAllSequences(zoomlevel);
              } catch (Exception ex) {}
            }
        }
    }

    /**
     * Updates tooltips and enabled status of toolbar undo/redo buttons when edit events happen
     */
    private class UndoRedoButtonUpdater implements UndoableEditListener {
        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            updateUndoRedoStates();
        }
    }

    /**
     * Listens to Document updates and sets undo/redo action states
     */
   private class UndoRedoDocumentListener implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {updateUndoRedoStates();}
        @Override
        public void insertUpdate(DocumentEvent e)  {updateUndoRedoStates();}
        @Override
        public void removeUpdate(DocumentEvent e)  {updateUndoRedoStates();}
    }

    /**
     * Listens to changes in the list of currently open Protocols
     */
   private class ProtocolManagerListener implements ListDataListener {
        ProtocolListMenuListener listener;
        int lastSize=1;

        public ProtocolManagerListener() {
            listener = new ProtocolListMenuListener();
        }
        @Override
        public void contentsChanged(ListDataEvent e) {populateProtocolList();}
        @Override
        public void intervalAdded(ListDataEvent e) {populateProtocolList();}
        @Override
        public void intervalRemoved(ListDataEvent e) {populateProtocolList();}

        public void populateProtocolList() {
            ProtocolManager manager=protocolEditor.getProtocolManager();
            changeProtocolSubmenu.removeAll();
            //System.err.println("protocolListChanges: "+manager.getSize()+" protocols");
            if (manager.getSize()==0) { // no open protocol scripts. Disable play/rec-buttons
                changeProtocolSubmenu.setEnabled(false);
                setRecordEnabled(false);
                setExecuteEnabled(false);
                setStopEnabled(false);
            } else { // protocol scripts are present
               if (lastSize==0) { // going from zero to one protocol scripts in list. Enable play/rec-buttons
                   setRecordEnabled(true);
                   setExecuteEnabled(true);
                   setStopEnabled(false);
               }
               changeProtocolSubmenu.setEnabled(true);
               Protocol currentProtocol=protocolEditor.getProtocol();
               String currentProtocolName="";
               if (currentProtocol!=null) currentProtocolName=currentProtocol.getName();
               for (int i=0;i<manager.getSize();i++) {
                   Protocol prot=(Protocol)manager.getElementAt(i);
                   JCheckBoxMenuItem item=new JCheckBoxMenuItem(prot.getName());
                   if (currentProtocolName.equals(prot.getName())) item.setSelected(true);
                   else item.setSelected(false);
                   item.addActionListener(listener);
                   changeProtocolSubmenu.add(item);
               }
            }
            lastSize=manager.getSize();
        }

        private class ProtocolListMenuListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isRecording()) {// notify user that recording is on
                    JOptionPane.showMessageDialog(getFrame(), "You are currently in \"record mode\".\nSubsequent operations will be recorded in the new protocol", "Notification", JOptionPane.INFORMATION_MESSAGE);
                }
                //protocolEditor.changeProtocol(e.getActionCommand(),null,true); // queue undoable event
                mainWindow.showProtocolEditor();
                protocolEditor.changeProtocol(e.getActionCommand());
            }
        }
   } // end class ProtocolManagerListener





    @Action
    public void changeDisplaySettings() {
        java.util.ArrayList test=engine.getAllDataItemsOfType(FeatureDataset.class);
        if (test.isEmpty()) {
            JOptionPane.showMessageDialog(getFrame(), "You can not change display settings for datasets, since no datasets have been loaded", "Error",JOptionPane.ERROR_MESSAGE,null);
            return;
        }
        DatasetDisplaySettingsDialog dialog=new DatasetDisplaySettingsDialog(getFrame(), null, getVisualizationSettings());
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
    }


    @Action
    public void showOptionsDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        PreferencesDialog dialog=new PreferencesDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action
    @Deprecated
    public void activateTransfacPROaction() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ActivateTransfacProDialog dialog=new ActivateTransfacProDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }




    /** Returns a filter that screens regions and selects which should be displayed and not */
    public RegionVisualizationFilter getRegionVisualizationFilter() {
        return regionVisualizationFilter;
    }

     /** Adds a RegionVisualizationFilter that screens regions and selects which should be displayed and not
      *  Multiple filters can be used simultaneously (they are then added to a RegionVisualizationFilterGroup),
      *  (but the same filter can only be added once)
      */
    public void addRegionVisualizationFilter(RegionVisualizationFilter filter) {
        if (isRegionFilterActive(filter)) return; // check if it has been added already
        if (regionVisualizationFilter==null) { // no filters are installed yet
            if (filter instanceof RegionVisualizationFilterGroup) { // the filter to be added is a group. To avoid confusion I will create a new "base" group and add the filter to that
                RegionVisualizationFilterGroup filtergroup=new RegionVisualizationFilterGroup("GUI");
                filtergroup.addFilter(filter); // add current filter
                regionVisualizationFilter=filtergroup;
            }
            else regionVisualizationFilter=filter;
        }
        else if (regionVisualizationFilter instanceof RegionVisualizationFilterGroup) { // current filter is a group. Add the new filter to this group
            ((RegionVisualizationFilterGroup)regionVisualizationFilter).addFilter(filter);
        } else { // current filter is a single filter. Setup a new group and add the current filter to it
            RegionVisualizationFilterGroup filtergroup=new RegionVisualizationFilterGroup("GUI");
            filtergroup.addFilter(regionVisualizationFilter); // add current filter to new group
            filtergroup.addFilter(filter); // add new filter to same group
            regionVisualizationFilter=filtergroup; // replace current filter with group
        }
        redraw();
    }


    /** Returns TRUE if the given filter is currently active */
    public boolean isRegionFilterActive(RegionVisualizationFilter filter) {
        if (regionVisualizationFilter==null) return false;
        else if (regionVisualizationFilter instanceof RegionVisualizationFilterGroup) { // current filter is a group. Add the new filter to this group
           return ((RegionVisualizationFilterGroup)regionVisualizationFilter).containsFilter(filter);
        } else { // current filter is a single filter.
            return (regionVisualizationFilter==filter);
        }
    }


     /** Removes a RegionVisualizationFilter
      *  Multiple filters can be used simultaneously
      */
    public void removeRegionVisualizationFilter(RegionVisualizationFilter filter) {
        if (filter!=null) filter.shutdown(); // notify filter of forced shutdown
        if (regionVisualizationFilter instanceof RegionVisualizationFilterGroup) {
            int count=((RegionVisualizationFilterGroup)regionVisualizationFilter).removeFilter(filter);
            if (count==0) regionVisualizationFilter=null; // no more filters in the group. Just remove the filter group
            else if (count==1) regionVisualizationFilter=((RegionVisualizationFilterGroup)regionVisualizationFilter).getFirstFilter(); // just one filter left in the group. Take it out of the group and replace the current filter with this (less complexity to worry about)
        } else if (regionVisualizationFilter==filter) {
            regionVisualizationFilter=null; // just remove the single filter
        } else {
            // this should probably not happen (but it can if no filters are installed)
        }
        redraw();
    }



    /** Returns a unique generic data item name that can be used as default name in dialogs etc.
     * @param type The class of the new data item. The class will dictate the returned name, however
     * it can also be NULL, in which case a generic type name will be returned
     * @param datatypetable (can be null) The method will return a name that is not already in use by other registered data items
     * if a DataTypeTable is provided the method will also avoid names present in the this table
     */
    public String getGenericDataitemName(Class type, DataTypeTable datatypetable) {
        int counter=1;
        String prefix="Data";
        //     if (FeatureDataset.class.isAssignableFrom(type)) {prefix="Feature";}
             if (type==DNASequenceDataset.class) {prefix="DNA";}
        else if (type==NumericDataset.class) {prefix="NumericDataset";}
        else if (type==RegionDataset.class) {prefix="RegionDataset";}
        else if (type==Sequence.class) {prefix="Sequence";}
        else if (type==SequenceCollection.class) {prefix="SequenceCollection";}
        else if (type==SequencePartition.class) {prefix="SequencePartition";}
        else if (type==Motif.class) {prefix="MM";}
        else if (type==ModuleCRM.class) {prefix="MOD";}
        else if (type==MotifCollection.class) {prefix="MotifCollection";}
        else if (type==MotifPartition.class) {prefix="MotifPartition";}
        else if (type==ModuleCollection.class) {prefix="ModuleCollection";}
        else if (type==ModulePartition.class) {prefix="ModulePartition";}
        else if (type==NumericVariable.class) {prefix="Value";}
        else if (type==SequenceNumericMap.class) {prefix="SequenceNumericMap";}
        else if (type==MotifNumericMap.class) {prefix="MotifNumericMap";}
        else if (type==ModuleNumericMap.class) {prefix="ModuleNumericMap";}
        else if (type==SequenceTextMap.class) {prefix="SequenceMap";}
        else if (type==MotifTextMap.class) {prefix="MotifMap";}
        else if (type==ModuleTextMap.class) {prefix="ModuleMap";}
        else if (type==TextVariable.class) {prefix="TextVariable";}
        else if (type==BackgroundModel.class) {prefix="Background";}
        else if (type==ExpressionProfile.class) {prefix="ExpressionProfile";}
        else if (type==PriorsGenerator.class) {prefix="PriorsGenerator";}
        else if (type==OutputData.class) {prefix="Output";}
        else if (Analysis.class.isAssignableFrom(type)) {prefix="Analysis";}
        if (type!=null) {
            boolean foundNumbers = false;
            ArrayList<Data> datalist=engine.getAllDataItemsOfTypeMatchingExpression("^"+prefix+"\\d.*",type);
            if (!datalist.isEmpty()) {
                ArrayList<String> namelist=new ArrayList<>(datalist.size());
                for (Data data:datalist) {namelist.add(data.getName());}
                MotifLabEngine.sortNaturalOrder(namelist, false);
                String lastName = namelist.get(0);
                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(lastName);
                if (matcher.find()) {
                    String numbers = matcher.group(0);
                    try {
                        counter=Integer.parseInt(numbers);
                        foundNumbers=true;} 
                    catch (NumberFormatException ne) {}
                    counter++;
                }   
            }
            if (datalist.isEmpty() || !foundNumbers) {
               counter=engine.countDataItemsOfType(type);
               if (type!=SequenceCollection.class) counter++; // this will start the SequenceCollection counter at 1 even thought there is already a (default) SequenceCollection registered               
            }            
        }
        String name;
        if (type==Motif.class || type==ModuleCRM.class) name=prefix+motifcounterformat.format(counter);
        else name=prefix+counter;
        while (engine.getDataItem(name)!=null || (datatypetable!=null && datatypetable.contains(name)) || reservedDataNames.contains(name)) {
            counter++; // adjust counter if name is taken already
            if (type==Motif.class || type==ModuleCRM.class) name=prefix+motifcounterformat.format(counter);
            else name=prefix+counter;
        }
        return name;
    }

    /** Returns a list of suggested unique names to be used for a set of new data objects of the given types
     *  The size of the list returned corresponds to the size of the type-array
     */
    public String[] getGenericDataitemNames(Class[] types, DataTypeTable datatypetable) {
        if (types==null || types.length==0) return new String[0];
        String[] resultnames=new String[types.length];
        for (int index=0;index<types.length;index++) {
            Class type=types[index];
            int counter=1;
            String prefix="Data";
            //     if (FeatureDataset.class.isAssignableFrom(type)) {prefix="Feature";}
                 if (type==DNASequenceDataset.class) {prefix="DNA";}
            else if (type==NumericDataset.class) {prefix="NumericDataset";}
            else if (type==RegionDataset.class) {prefix="RegionDataset";}
            else if (type==Sequence.class) {prefix="Sequence";}
            else if (type==SequenceCollection.class) {prefix="SequenceCollection";}
            else if (type==SequencePartition.class) {prefix="SequencePartition";}
            else if (type==Motif.class) {prefix="MM";}
            else if (type==ModuleCRM.class) {prefix="MOD";}
            else if (type==MotifCollection.class) {prefix="MotifCollection";}
            else if (type==MotifPartition.class) {prefix="MotifPartition";}
            else if (type==ModuleCollection.class) {prefix="ModuleCollection";}
            else if (type==ModulePartition.class) {prefix="ModulePartition";}
            else if (type==NumericVariable.class) {prefix="Value";}
            else if (type==SequenceNumericMap.class) {prefix="SequenceNumericMap";}
            else if (type==MotifNumericMap.class) {prefix="MotifNumericMap";}
            else if (type==ModuleNumericMap.class) {prefix="ModuleNumericMap";}
            else if (type==TextVariable.class) {prefix="TextVariable";}
            else if (type==BackgroundModel.class) {prefix="Background";}
            else if (type==ExpressionProfile.class) {prefix="ExpressionProfile";}
            else if (type==PriorsGenerator.class) {prefix="PriorsGenerator";}
            else if (type==OutputData.class) {prefix="Output";}
            else if (Analysis.class.isAssignableFrom(type)) {prefix="Analysis";}
            if (type!=null) {
                counter=engine.countDataItemsOfType(type);
                if (type!=SequenceCollection.class) counter++; // this will start the SequenceCollection counter at 1 even thought there is already a (default) SequenceCollection registered
            }
            String name;
            if (type==Motif.class || type==ModuleCRM.class) name=prefix+motifcounterformat.format(counter);
            else name=prefix+counter;
            while (engine.getDataItem(name)!=null || arrayContainsString(resultnames,name) || (datatypetable!=null && datatypetable.contains(name)) || reservedDataNames.contains(name)) {
                counter++; // adjust counter if name is taken already
                if (type==Motif.class || type==ModuleCRM.class) name=prefix+motifcounterformat.format(counter);
                else name=prefix+counter;
            }
            resultnames[index]=name;
        }
        return resultnames;
    }
    private boolean arrayContainsString(String[] list, String target) {
        for (String element:list) {
            if (element!=null && element.equals(target)) return true;
        }
        return false;
    }

    /** Returns a suggested unique name to be used for a new data object
     *  A prefix can be suggested
     */
    public String getGenericDataitemName(String prefix, DataTypeTable datatypetable) {
        int counter=1;
        if (prefix==null || prefix.isEmpty()) prefix="Data";
        String name=(prefix.equals("Data"))?prefix+counter:prefix; // if a prefix is provided, try that first before starting to add numbers to it
        while (engine.getDataItem(name)!=null || (datatypetable!=null && datatypetable.contains(name)) || reservedDataNames.contains(name)) {
            counter++; // adjust counter if name is taken already
            name=prefix+counter;
        }
        return name;
    }

    /**
     * This method can be used to reserve names that might be used in the future by tasks that have been scheduled already
     * If a name is reserved it will not be suggested as a "generic name" by the getGenericDataitemName() methods
     * @param name A single name or a comma-separated list of names to be reserved
     */
    public void reserveDataName(String name) {
        if (name==null || name.isEmpty()) return;
        String[] reserve=name.split("\\s*,\\s*");
        reservedDataNames.addAll(Arrays.asList(reserve));
    }

    /**
     * This method is used to release names that have been marked as 'reserved' with the reserveDataName() method
      * @param name A single name or a comma-separated list of names to be reserved
     */
    public void releaseReservedDataName(String name) {
        if (name==null || name.isEmpty()) return;
        String[] reserve=name.split("\\s*,\\s*");
        for (String rname:reserve) reservedDataNames.remove(rname);
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
                    if (text!=null && text.contains("\t")) text=text.replace("\t", "   "); // the status bar does not handle TABs, so just replace them with spaces
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
                    int pending=scheduler.pendingTasks();
                    if (pending>1) progressBar.setString("+"+pending+" tasks pending");
                    else if (pending==1) progressBar.setString("+"+pending+" task pending");
                    else progressBar.setString("");
                } else if (propertyName.equals(ExecutableTask.SCHEDULING_EVENT)) { // a task has been scheduled for execution or aborted
                    int pending=scheduler.pendingTasks();
                    if (pending>1) progressBar.setString("+"+pending+" tasks pending");
                    else if (pending==1) progressBar.setString("+"+pending+" task pending");
                    else progressBar.setString("");
                } else if (propertyName.equals(ProtocolTask.STARTED_EXECUTION)) {
                    String protocolName = (String)(evt.getOldValue());
                    getProtocolEditor().protocolExecutionStarted(protocolName);
                } else if (propertyName.equals(ProtocolTask.FINISHED_EXECUTION)) {
                    // nothing yet...
                } else if (propertyName.equals(ProtocolTask.STARTED_EXECUTION_OF_LINE)) {
                    String protocolName = (String)(evt.getOldValue());
                    int linenumber = (Integer)(evt.getNewValue());
                    getProtocolEditor().protocolLineExecutionStarted(protocolName, linenumber);
                } else if (propertyName.equals(ProtocolTask.FINISHED_EXECUTION_OF_LINE)) {
                    String protocolName = (String)(evt.getOldValue());
                    int linenumber = (Integer)(evt.getNewValue());
                    getProtocolEditor().protocolLineExecutionFinished(protocolName, linenumber);
                } else if (propertyName.equals(ProtocolTask.EXECUTION_OF_LINE_ERROR)) {
                    String protocolName = (String)(evt.getOldValue());
                    int linenumber = (Integer)(evt.getNewValue());
                    getProtocolEditor().protocolLineExecutionError(protocolName, linenumber);
                } else if (propertyName.equals(ProtocolTask.EXECUTION_OF_LINE_ABORTED)) {
                    String protocolName = (String)(evt.getOldValue());
                    int linenumber = (Integer)(evt.getNewValue());
                    getProtocolEditor().protocolLineExecutionAborted(protocolName, linenumber);
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


    /**
     * Attempts to abort the execution of the currently running protocol script
     * @return true if the execution was aborted or false it is still running
     */
    public boolean abortExecution() {
        if (1<2) {scheduler.abort(); return true;} // this line will always abort and return without questions. Remove this line to show a confirmation dialog which allows the user to reconsider
        int n=-1;
        synchronized (scheduler.getExecutionLock()) { // this will temporarily suspend task execution as soon as possible
           Object[] options={"OK","Cancel"};
           n=JOptionPane.showOptionDialog(getFrame(), "Abort execution?", "Abort execution", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,options,options[1]);
        }
        // I will release the execution-lock first before calling abort (because abort() can remove the execution lock from the task and cause deadlock)
        if (n==0) {scheduler.abort(); return true;}
        else return false;
        // scheduler.shutdown(false);
    }

    @Action
    public void showSequenceInputDialog() {
        if (engine.hasDataItemsOfType(FeatureDataset.class)) {
            JOptionPane.showMessageDialog(getFrame(), "New sequences can not be added after feature data has been loaded", "Add Sequences", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SequenceInputDialog dialog=new SequenceInputDialog(engine);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        if (executeButton.isSelected()) dialog.disableAutoFetchDNA();
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showDatatrackDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DataTrackDialog dialog=new DataTrackDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showFavoritesDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        FavoritesDialog dialog=new FavoritesDialog(this,favorites);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showLoadFromFileDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        LoadDataFromFileDialog dialog=new LoadDataFromFileDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showAddSequencesFromFileDialog() {
        if (engine.hasDataItemsOfType(FeatureDataset.class)) {
            JOptionPane.showMessageDialog(getFrame(), "New sequences can not be added after feature data has been loaded", "Add Sequences From File", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        AddSequencesFromFileDialog dialog=new AddSequencesFromFileDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }



    @Action
    public void newProtocol() {
        if (isRecording()) {// notify user that recording is on
             JOptionPane.showMessageDialog(getFrame(), "You are currently in \"record mode\".\nSubsequent operations will be recorded in the new protocol", "New Protocol", JOptionPane.INFORMATION_MESSAGE);
        }
        protocolEditor.newProtocol(null);
        mainWindow.showProtocolEditor();
    }


    @Action
    public void openProtocol() {
        if (isRecording()) {// notify user that recording is on
             JOptionPane.showMessageDialog(getFrame(), "You are currently in \"record mode\".\nSubsequent operations will be recorded in the new protocol", "Open Protocol", JOptionPane.INFORMATION_MESSAGE);
        }
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        protocolEditor.openProtocolFile();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void openProtocolFromURL() {
        if (isRecording()) {// notify user that recording is on
             JOptionPane.showMessageDialog(getFrame(), "You are currently in \"record mode\".\nSubsequent operations will be recorded in the new protocol", "Open Protocol", JOptionPane.INFORMATION_MESSAGE);
        }
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        protocolEditor.openProtocolFileFromURL();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action(enabledProperty = "saveEnabled")
    public void saveFile() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        mainWindow.saveCurrentTabToFile(false); // checks which tab is currently used and saves the contents to file
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action
    public void saveAllActionMethod() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        mainWindow.saveAllTabs(); //
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action(enabledProperty = "saveAsEnabled")
    public void saveAsActionMethod() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        mainWindow.saveCurrentTabToFile(true); // checks which tab is currently used and saves the contents to file
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action(enabledProperty = "closeEnabled")
    public void closefileActionMethod() {
        String currentTab=mainWindow.getSelectedTabName();
        if (currentTab.equals("Visualization") || currentTab.equals("Protocol")) {
            if (isRecording()) stopButton.doClick(); // stop recording when closing protocol
            protocolEditor.closeProtocolFile();
        }
        else mainWindow.closeCurrentTab();
    }


    @Action
    public void saveSessionActionMethod() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File file=null;
        File parentDir=getLastUsedDirectory();
        final JFileChooser fc = getFileChooser(parentDir);
        fc.setDialogTitle("Save Session");
        FileNameExtensionFilter sessionFilter=new FileNameExtensionFilter("MotifLab Session (*.mls)", "mls");
        fc.addChoosableFileFilter(sessionFilter);
        fc.setFileFilter(sessionFilter);
        File preselected=MotifLabEngine.getFile(parentDir,"MotifLabSession.mls");
        fc.setSelectedFile(preselected);
        int returnValue=fc.showSaveDialog(getFrame());
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            file=fc.getSelectedFile();
        } else {
            getFrame().setCursor(Cursor.getDefaultCursor());
            return;
        }
        if (!file.getAbsolutePath().endsWith(".mls")) {
            if (file instanceof DataRepositoryFile) file=new DataRepositoryFile(file.getAbsolutePath()+".mls", ((DataRepositoryFile)file).getRepository());
            else file=new File(file.getAbsolutePath()+".mls");
        }
        if (file.exists()) {
            int choice=JOptionPane.showConfirmDialog(getFrame(), "Overwrite existing file \""+file.getName()+"\" ?","Save Session",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) {
                getFrame().setCursor(Cursor.getDefaultCursor());
                return;
            }
        }
        saveSessionInBackground(file);
        setLastUsedDirectory(file.getParentFile());
    }

  /** A convenience method to save session off the EDT */
  private void saveSessionInBackground(final File file) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                ObjectOutputStream outputStream = null;
                try {
                    OutputStream os=MotifLabEngine.getOutputStreamForFile(file);
                    outputStream = new ObjectOutputStream(new BufferedOutputStream(os));
                    HashMap<String,Object> info=new HashMap<String, Object>();
                    VisualizationSettings vizSettings=getVisualizationSettings();
                    vizSettings.storeSetting("systemColor.trackBorderColor", SequenceVisualizer.getTrackBorderColor()); // these are not normally stored in VizSettings, but we add them so we can retrieve them later
                    vizSettings.storeSetting("systemColor.boundingBoxColor", SequenceVisualizer.getBoundingBoxColor());
                    info.put("data", engine.getAllDataItemsOfType(Data.class));
                    info.put("visualizationsettings", vizSettings);
                    info.put("protocols",protocolEditor.getProtocolManager().getSerializedProtocolManager());
                    info.put("tabnames",mainWindow.getTabNames());
                    info.put("selectedtab",mainWindow.getSelectedTabName());
                    engine.saveSession(outputStream, info, progressListener);
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (outputStream!=null) outputStream.close();} catch (Exception x) {}
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     JOptionPane.showMessageDialog(getFrame(), ex.getClass().toString()+":\n"+ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                     logMessage("Unable to save session to \""+file.getAbsolutePath()+"\"  ("+ex.getClass().toString()+":"+ex.getMessage()+")");
                } else { // save went OK
                     logMessage("Saved session to \""+file.getAbsolutePath()+"\"");
                     registerRecentSession(file.getAbsolutePath());
                }
                MotifLabGUI.this.setProgress(100);
                getFrame().setCursor(Cursor.getDefaultCursor());
                MotifLabGUI.this.setProgress(101); // this "illegal" value will hide the progressbar
                engine.setAllowDataUpdates(true); // enable data updates and undo/redo again
                setUndoEnabled(true); //
                setRedoEnabled(true);
                System.gc();
            }
        }; // end of SwingWorker class
        setUndoEnabled(false); // disable undo/redo while saving session
        setRedoEnabled(false);
        engine.setAllowDataUpdates(false); // Do not allow changes to be made to the data while saving the session
        worker.execute();
    }

   /* This method is required by the MotifLab-client interface
   *  It is not used by the GUI itself (interactively), but is used when 'saveSession' is called
   *  from a protocol script or when sessions are saved on exit
   */
    @Override
    public void saveSession(String filename) throws Exception {
       File file=engine.getFile(filename);
       if (!(file instanceof DataRepositoryFile || file.isAbsolute())) file=MotifLabEngine.getFile(getLastUsedDirectory(),filename);
       ObjectOutputStream outputStream = null;
        try {
            OutputStream os=MotifLabEngine.getOutputStreamForFile(file);
            outputStream = new ObjectOutputStream(new BufferedOutputStream(os));
            HashMap<String,Object> info=new HashMap<String, Object>();
            VisualizationSettings vizSettings=getVisualizationSettings();
            vizSettings.storeSetting("systemColor.trackBorderColor", SequenceVisualizer.getTrackBorderColor()); // these are not normally stored in VizSettings, but we add them so we can retrieve them later
            vizSettings.storeSetting("systemColor.boundingBoxColor", SequenceVisualizer.getBoundingBoxColor());
            info.put("data", engine.getAllDataItemsOfType(Data.class));
            info.put("visualizationsettings", vizSettings);
            info.put("protocols",protocolEditor.getProtocolManager().getSerializedProtocolManager());
            info.put("tabnames",mainWindow.getTabNames());
            info.put("selectedtab",mainWindow.getSelectedTabName());
            engine.saveSession(outputStream, info, progressListener);
            if (!file.getAbsolutePath().endsWith(autosavedsessionfilename)) logMessage("Session saved to \""+file.getAbsolutePath()+"\"");
        } catch (Exception e) {
            throw e;
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (Exception x) {}
        }
    }

  /*  This method is required by the MotifLab-client interface
   *  It is not used by the GUI itself, but is used when 'restoreSession' is called
   *  from a protocol script
   */
    @Override
    public void restoreSession(String filename) throws Exception {
        Object input=null;
        if (filename.startsWith("http://") || filename.startsWith("https://") || filename.startsWith("ftp://") || filename.startsWith("ftps://")) {
            input=new URL(filename);
        } else {
            input=engine.getFile(filename);
            if (!(input instanceof DataRepositoryFile) && !((File)input).isAbsolute()) input=new File(getLastUsedDirectory(),filename);
        }
        ObjectInputStream inputStream = null;
        ArrayList<Data> datalist=null;
        VisualizationSettings settings=null;
        Object restoredProtocols=null; // this can be a SerializedProtocolManager or StandardProtocol
        SequenceCollection defaultCollectionCopy=null;
        String[] tabNames;
        String selectedTabName=null;
        String[] requirements=null;
        HashMap<String,int[]> VPregion=new HashMap<String,int[]>();
        try {
            InputStream is=MotifLabEngine.getInputStreamForDataSource(input);
            HashMap<String,Object> restored=engine.restoreSession(is, progressListener);
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
                visualizationSettings.enableVisualizationSettingsNotifications(false); // disable to avoid redundant updates
                engine.clearAllData();
                protocolEditor.getProtocolManager().closeAllWithoutUndo();
                for (Data element:datalist) { // restore all sequences and motifs first, since these are referred to by collections!
                    if (element instanceof Sequence) {
                        int vpstart=settings.getSequenceViewPortStart(element.getName());
                        int vpend=settings.getSequenceViewPortEnd(element.getName());
                        if (vpstart>=0) VPregion.put(element.getName(), new int[]{vpstart,vpend}); // save VP to correctly restore it later...
                        engine.storeDataItem(element);
                    } else if (element instanceof Motif || element instanceof ModuleCRM) {
                        engine.storeDataItem(element);
                    }
                }
                visualizationSettings.importSettings(settings);
                PreferencesDialog.restoreVisualizationSettings(MotifLabGUI.this); // override with settings chosen in Preferences
                SequenceVisualizer.setTrackBorderColor((Color)visualizationSettings.getSettingAsType("systemColor.trackBorderColor", Color.BLACK));
                SequenceVisualizer.setBoundingBoxColor((Color)visualizationSettings.getSettingAsType("systemColor.boundingBoxColor", Color.BLACK));
                visualizationSettings.enableVisualizationSettingsNotifications(true); // reenable
                for (String outputDataName:tabNames){ // restore output data objects (same as tabnames)
                    Data element=getDataByNameFromList(datalist,outputDataName);
                    if (element==null) continue;
                    engine.storeDataItem(element); // this will show the tab
                }
                for (Data element:datalist) {
                    if (!(element instanceof Sequence || element instanceof OutputData || element instanceof Motif || element instanceof ModuleCRM)) try {engine.storeDataItem(element);} catch (ExecutionError e) {JOptionPane.showMessageDialog(getFrame(), "An error occurred during session restore:\n"+e.getMessage(),"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);}
                }
                SequenceCollection col=engine.getDefaultSequenceCollection();
                col.setSequenceOrder(defaultCollectionCopy.getAllSequenceNames());
                visualizationPanel.clearCachedVisualizers();
                visualizationPanel.sequencesLayoutEvent(VisualizationSettingsListener.FORCE_MAJURE,null); // updates sequence order
                mainWindow.invalidate();
                mainWindow.repaint();
                for (String name:VPregion.keySet()) {
                    int[] region=VPregion.get(name);
                    visualizationSettings.setSequenceViewPort(name, region[0], region[1]);
                }
                if (restoredProtocols instanceof SerializedProtocolManager) {
                    protocolEditor.getProtocolManager().importSettings((SerializedProtocolManager)restoredProtocols);
                } else if (restoredProtocols instanceof SerializedStandardProtocol) {
                   StandardProtocol stdprot=((SerializedStandardProtocol)restoredProtocols).getProtocol(engine);
                   protocolEditor.getProtocolManager().clearAndInstallProtocol(stdprot);
                }
                for (Protocol protocol:protocolEditor.getProtocolManager().getAllProtocols()) {
                    if (protocol instanceof StandardProtocol) ((StandardProtocol)protocol).setDirtyFlag(true); // mark all recently opened protocols as dirty to force user to acknowledge overwrite when saving
                }
                for (Data output:engine.getAllDataItemsOfType(OutputData.class)) {
                    ((OutputData)output).setDirty(true); // mark all output data objects as dirty so that it is possible to save them again
                }                
                undoManager.discardAllEdits(); // reset the undo queue
                if (selectedTabName!=null) mainWindow.setSelectedTab(selectedTabName);
                recplaystopButtonGroup.clearSelection();
                if (isRecording()) {
                    stopAction();
                }
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

    @Action
    public void restoreSessionActionMethod() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File file=null;
        final JFileChooser fc = getFileChooser(getLastUsedDirectory());
        fc.setDialogTitle("Restore Session");
        javax.swing.filechooser.FileFilter sessionfilter=new FileNameExtensionFilter("MotifLab Session (*.mls)", "mls");
        fc.addChoosableFileFilter(sessionfilter);
        fc.setFileFilter(sessionfilter);
        int returnValue=fc.showOpenDialog(getFrame());
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            file=fc.getSelectedFile();
        } else {
            getFrame().setCursor(Cursor.getDefaultCursor());
            return;
        }
        //if (!file.getAbsolutePath().endsWith(".mls")) file=new File(file.getAbsolutePath()+".mls");
        restoreSessionInBackground(file);
        setLastUsedDirectory(file.getParentFile());
    }

    @Action
    public void restoreSessionFromURLActionMethod() {
        String input=JOptionPane.showInputDialog(getFrame(), "Enter URL address","Restore Session From URL",JOptionPane.QUESTION_MESSAGE);
        if (input==null || input.isEmpty()) return;
        if (!(input.startsWith("http://") || input.startsWith("https://") || input.startsWith("ftp://")|| input.startsWith("ftps://"))) input="http://"+input;
        URL url=null;
        try {
            url=new URL(input);
            restoreSessionInBackground(url);
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(getFrame(), "Not a proper URL:\n\n"+input, "URL Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *
     * @param input should be either URL or FILE
     */
    @SuppressWarnings("unchecked")
    private void restoreSessionInBackground(final Object input) {
        MotifLabGUI.this.setProgress(0);
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusMessage("Restoring session...");
        final String fullpath=(input instanceof File)?((File)input).getAbsolutePath():((URL)input).toString();
        if (fullpath.endsWith(autosavedsessionfilename)) logMessage("Restoring previous session...");
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            ArrayList<Data> datalist=null;
            VisualizationSettings settings=null;
            Object restoredProtocols=null; // this can be a SerializedProtocolManager or StandardProtocol
            SequenceCollection defaultCollectionCopy=null;
            String[] tabNames;
            String selectedTabName=null;
            String[] requirements=null;
            int sessionFormatVersion=2;
            HashMap<String,int[]> VPregion=new HashMap<String,int[]>();
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                ObjectInputStream inputStream = null;
                try {
                    InputStream is=MotifLabEngine.getInputStreamForDataSource(input);
                    HashMap<String,Object> restored=engine.restoreSession(is, progressListener);
                    requirements=(String[])restored.get("requirements");
                    datalist=(ArrayList<Data>)restored.get("data");
                    settings=(VisualizationSettings)restored.get("visualizationsettings");
                    restoredProtocols=restored.get("protocols");
                    tabNames=(String[])restored.get("tabnames");
                    selectedTabName=(String)restored.get("selectedtab");
                    defaultCollectionCopy=(SequenceCollection)restored.get("defaultsequencecollection");
                    if (restored.containsKey("sessionFormatVersion")) {
                        sessionFormatVersion=Integer.parseInt((String)restored.get("sessionFormatVersion"));
                    }
                    if (restored.containsKey("exception")) throw (Exception)restored.get("exception");
                } catch (Exception e) {
                    // logMessage("Exception was thrown");
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (inputStream!=null) inputStream.close();} catch (Exception x) {}
                }
                statusMessage("Preparing session... (this might take some time)");
                MotifLabGUI.this.setProgress(Integer.MAX_VALUE);// sets progress bar to "indetermined"
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT! after doInBackground() has finished
                if (ex!=null) {
                     //ex.printStackTrace(System.err);
                     if (ex instanceof InvalidClassException || ex instanceof ClassCastException) {
                        JOptionPane.showMessageDialog(getFrame(), "An error occurred during session restore:\n\nThe saved session is not compatible with this version of MotifLab","Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                        System.err.println(ex.toString());
                        removeSessionFromRecentMenu(fullpath);
                     }
                     else if (ex instanceof ClassNotFoundException || ex instanceof StreamCorruptedException) {
                        StringBuilder message=new StringBuilder();
                        if (ex.getMessage().equals("Newer version")) message.append("The saved session requires a newer version of MotifLab.");
                        else message.append("The saved session is not compatible with the current version/setup of MotifLab.");
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
                                message.append("\n\nMissing required plugins:\n");
                                for (String req:missingPlugins) message.append("   \u2022 "+req+"\n");
                            }
                            if (!additionRequirements.isEmpty()) {
                                message.append("\nAdditional requirements:\n");
                                for (String req:additionRequirements) message.append("   \u2022 "+req+"\n");
                            }
                        }
                        JOptionPane.showMessageDialog(getFrame(), "An error occurred during session restore:\n\n"+message.toString(),"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                        removeSessionFromRecentMenu(fullpath);
                     } else if (ex instanceof ExecutionError && (((ExecutionError)ex).getCause() instanceof OutOfMemoryError)) {
                         JOptionPane.showMessageDialog(getFrame(), "Out of memory error:\n\nYou do not have enough memory allocated to MotifLab to restore this session.\nPlease consult the on-line user manual for information on running MotifLab with more memory.","Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                         logMessage("Unable to restore session: Not enough memory");
                     } else if (ex instanceof EOFException) {
                         JOptionPane.showMessageDialog(getFrame(), "Premature end of file error:\n\nSession file appears to be corrupted","Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                         logMessage("Unable to restore session: Premature end of file");
                     }
                     else {
                         JOptionPane.showMessageDialog(getFrame(), ex.getClass().getSimpleName().toString()+":\n\n"+ex.getMessage(),"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                         logMessage("Unable to restore session: "+ex.getClass().getSimpleName().toString()+" - "+ex.getMessage());
                         ex.printStackTrace();
                     }
                } else if (datalist==null || settings==null || restoredProtocols==null || tabNames==null || selectedTabName==null) {
                        String msg="";
                             if (datalist==null) msg="Unable to recover data from session file";
                        else if (settings==null) msg="Unable to recover display settings from file";
                        else if (restoredProtocols==null) msg="Unable to recover protocol information from file";
                        else if (tabNames==null) msg="Unable to recover correct output tab names from file";
                        else if (selectedTabName==null) msg="Unable to recover selectedTabName file";
                        JOptionPane.showMessageDialog(getFrame(), "An error occurred during session restore:\n\n"+msg,"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                        logMessage("Unable to restore session");
                } else { // restore went OK
                    visualizationSettings.enableVisualizationSettingsNotifications(false); // disable to avoid redundant updates
                    engine.clearAllData();
                    protocolEditor.getProtocolManager().closeAllWithoutUndo();
                    // restore all sequences and motifs first, since these are referred to by collections, partitions and maps
                    for (Data element:datalist) {
                      try {
                        if (element instanceof Sequence) {
                            int vpstart=settings.getSequenceViewPortStart(element.getName());
                            int vpend=settings.getSequenceViewPortEnd(element.getName());
                            if (vpstart>=0) VPregion.put(element.getName(), new int[]{vpstart,vpend}); // save VP to correctly restore it later...
                            engine.storeDataItem(element);
                        } else if (element instanceof Motif || element instanceof ModuleCRM) {
                            engine.storeDataItem(element);
                        }
                      } catch (ExecutionError e) {
                          e.printStackTrace(System.err);
                          JOptionPane.showMessageDialog(getFrame(), "An error occurred during session restore:\n\n"+e.getMessage(),"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                      }
                    }
                    visualizationSettings.importSettings(settings);
                    if (sessionFormatVersion<4) visualizationSettings.updateGradientsFromOlderSession(); // convert old style solid gradients to new transparent style
                    PreferencesDialog.restoreVisualizationSettings(MotifLabGUI.this); // override with settings chosen in Preferences
                    SequenceVisualizer.setTrackBorderColor((Color)visualizationSettings.getSettingAsType("systemColor.trackBorderColor", Color.BLACK));
                    SequenceVisualizer.setBoundingBoxColor((Color)visualizationSettings.getSettingAsType("systemColor.boundingBoxColor", Color.BLACK));
                    visualizationSettings.enableVisualizationSettingsNotifications(true); // reenable
                    //visualizationSettings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REORDERED, engine.getDefaultSequenceCollection());
                    // restore output data objects (same as tabnames)
                    for (String outputDataName:tabNames){
                        Data element=getDataByNameFromList(datalist,outputDataName);
                        if (element==null) continue;
                        try {
                            engine.storeDataItem(element); // this will show the tab
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                            JOptionPane.showMessageDialog(getFrame(), "An error occurred during session restore:\n\n"+e.getMessage(),"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    // restore the remaining data objects
                    for (Data element:datalist) {
                        if (!(element instanceof Sequence || element instanceof OutputData || element instanceof Motif || element instanceof ModuleCRM)) {
                            try {
                                engine.storeDataItem(element);
                            } catch (ExecutionError e) {
                                e.printStackTrace(System.err);
                                JOptionPane.showMessageDialog(getFrame(), "An error occurred during session restore:\n"+e.getMessage(),"Restore Session Error" ,JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    SequenceCollection col=engine.getDefaultSequenceCollection();
                    col.setSequenceOrder(defaultCollectionCopy.getAllSequenceNames());
                    visualizationPanel.clearCachedVisualizers();
                    visualizationPanel.sequencesLayoutEvent(0,null); // updates sequence order
                    mainWindow.invalidate();
                    mainWindow.repaint();
                    for (String name:VPregion.keySet()) { // restore VP regions
                        int[] region=VPregion.get(name);
                        visualizationSettings.setSequenceViewPort(name, region[0], region[1]);
                    }
                    if (restoredProtocols instanceof SerializedProtocolManager) {
                        protocolEditor.getProtocolManager().importSettings((SerializedProtocolManager)restoredProtocols);
                    } else if (restoredProtocols instanceof SerializedStandardProtocol) {
                       StandardProtocol stdprot=((SerializedStandardProtocol)restoredProtocols).getProtocol(engine);
                       protocolEditor.getProtocolManager().clearAndInstallProtocol(stdprot);
                    }
                    for (Protocol protocol:protocolEditor.getProtocolManager().getAllProtocols()) {
                        if (protocol instanceof StandardProtocol) ((StandardProtocol)protocol).setDirtyFlag(true); // mark all recently opened protocols as dirty to force user to acknowledge overwrite when saving
                    }
                    for (Data output:engine.getAllDataItemsOfType(OutputData.class)) {
                        ((OutputData)output).setDirty(true); // mark all output data as dirty so that it is possible to save them again
                    }                      
                    undoManager.discardAllEdits(); // reset the undo queue
                    if (selectedTabName!=null) mainWindow.setSelectedTab(selectedTabName);
                    registerRecentSession(fullpath);
                    recplaystopButtonGroup.clearSelection();
                    if (isRecording()) {
                        stopAction();
                    }
                    statusMessage("Restoring session...OK");
                    if (fullpath.endsWith(autosavedsessionfilename)) {} // logMessage("Restored previous session...");
                    else logMessage("Restored session from \""+fullpath+"\"");
                    synchronizeGUIwithVisualizationSettings();
                }
                MotifLabGUI.this.setProgress(100);//
                getFrame().setCursor(Cursor.getDefaultCursor());
                MotifLabGUI.this.setProgress(101); // illegal value will hide the progressbar
                System.gc();
            }
        }; // end of SwingWorker class
        worker.execute();
    }  

    private void synchronizeGUIwithVisualizationSettings() {
         setupShowTrackNamesMenuItem(); // this will update the track label menu with the correct selections
    }
    
    private Data getDataByNameFromList(ArrayList<Data>list, String name) {
        for (Data element:list) {if (element.getName().equals(name)) return element;}
        return null;
    }

    private void registerRecentSession(String filename) {
        if (filename.endsWith(autosavedsessionfilename)) return; // do not register autosaved sessions
        if (recentsessions.contains(filename)) recentsessions.remove(filename);
        if (recentsessions.size()>=maxRecentSessions) recentsessions.remove(maxRecentSessions-1);
        recentsessions.add(0,filename);
        updateRecentSessionsMenu(false);
    }

    private void removeSessionFromRecentMenu(String filename) {
        if (!recentsessions.contains(filename)) return;
        recentsessions.remove(filename);
        updateRecentSessionsMenu(false);
    }



    @SuppressWarnings("unchecked")
    private void updateRecentSessionsMenu(boolean readFromDisc) {
        JMenu menu=fileRestoreRecentSessionSubmenu;
        menu.removeAll();
        if (readFromDisc) {
            LocalStorage localStorage=getApplication().getContext().getLocalStorage();
            try {
               Object list=localStorage.load(recentsessionsfilename);
               if (list instanceof ArrayList) recentsessions=(ArrayList<String>)list;
            } catch (Exception e) {}
        }
        for(String pathname:recentsessions) {
            String shortname;
            if (pathname.startsWith("http://") || pathname.startsWith("https://") || pathname.startsWith("ftp://")|| pathname.startsWith("ftps://")) {
                int endpos=pathname.indexOf('?');
                if (endpos<0) endpos=pathname.length();
                int startpos=pathname.lastIndexOf('/',endpos-1);
                if (startpos<0) startpos=0; else startpos++;
                shortname=pathname.substring(startpos,endpos);
            } else {
                File file=engine.getFile(pathname);
                shortname=file.getName();
            }
            JMenuItem menuItem=new JMenuItem(shortname);
            menuItem.setActionCommand(pathname);
            menuItem.setToolTipText(pathname);
            menuItem.addActionListener(openrecentsessionmenulistener);
            menu.add(menuItem);
        }
        if (recentsessions.isEmpty()) menu.setEnabled(false);
        else menu.setEnabled(true);
    }

    private class OpenRecentSessionMenuListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String fullpath=e.getActionCommand();
            Object input=null;
            if (fullpath.startsWith("http://") || fullpath.startsWith("https://") || fullpath.startsWith("ftp://")|| fullpath.startsWith("ftps://")) {
                try {input=new URL(fullpath);} catch (MalformedURLException mue) {logMessage("Trying to open Malformed URL: "+fullpath);}
            } else input=engine.getFile(fullpath);
            restoreSessionInBackground(input);
        }
    } // end private class OpenRecentSessionMenuListener


    @Action
    public void saveAsIndividualImagesMethod() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File dir=null;
        File parentDir=getLastUsedDirectory();
        final JFileChooser fc = getFileChooser(parentDir);
        fc.setDialogTitle("Save Sequences as Individual Images");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JPanel subsetPanel=new JPanel(new BorderLayout());
        subsetPanel.setLayout(new FlowLayout());
        subsetPanel.setBorder(BorderFactory.createTitledBorder("Sequences"));
        ArrayList<String> subset=new ArrayList<String>();
        ArrayList<String> collection=engine.getNamesForAllDataItemsOfType(SequenceCollection.class);
        collection.remove(engine.getDefaultSequenceCollectionName());
        Collections.sort(collection,MotifLabEngine.getNaturalSortOrderComparator(true));
        subset.add(engine.getDefaultSequenceCollectionName());
        subset.addAll(collection);
        collection.clear();
        collection=engine.getNamesForAllDataItemsOfType(Sequence.class);
        Collections.sort(collection,MotifLabEngine.getNaturalSortOrderComparator(true));
        subset.addAll(collection);
        String[] array=new String[0];
        array=subset.toArray(array);
        JComboBox subsetCombobox=new JComboBox<>(array);
        subsetCombobox.setEditable(true);
        subsetPanel.add(subsetCombobox);
        fc.setAccessory(subsetPanel);
        File preselected=getLastUsedDirectory();
        fc.setSelectedFile(preselected);
        int returnValue=fc.showSaveDialog(getFrame());
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            dir=fc.getSelectedFile();
        } else {
            getFrame().setCursor(Cursor.getDefaultCursor());
            return;
        }
        String suffix="png";
        String subsetName=(String)subsetCombobox.getSelectedItem();
        saveSequencesAsIndividualImagesInBackground(dir,suffix,subsetName);
        setLastUsedDirectory(dir);
    }


    @Action
    public void saveAsImageMethod() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File file=null;
        File parentDir=getLastUsedDirectory();
        final JFileChooser fc = getFileChooser(parentDir);
        fc.setDialogTitle("Save visualization");
        fc.addPropertyChangeListener(new PropertyChangeListener() {
            private File oldSelectedFile=null;
            @Override
            public void propertyChange(PropertyChangeEvent evt) {  // property change listener will update the file-suffix when the filter is changed
                if (evt.getPropertyName().equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
                    if (!(evt.getNewValue() instanceof FileNameExtensionFilter)) return;
                    FileNameExtensionFilter filter=(FileNameExtensionFilter)evt.getNewValue();
                    String extension=filter.getExtensions()[0];
                    File newFile=fc.getSelectedFile();
                    if (newFile==null && oldSelectedFile!=null) {
                        String oldName=oldSelectedFile.getName();
                        String newName=oldName.substring(0,oldName.lastIndexOf('.')+1)+extension.toLowerCase();
                        File newSelectedFile=new File(oldSelectedFile.getParent(), newName);
                        fc.setSelectedFile(newSelectedFile);
                    }
                } else if (evt.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    if (evt.getNewValue()==null) oldSelectedFile=(File)evt.getOldValue();
                }
            }
        });
//        // Allow the user to set a scaling factor for the image
        JPanel scalePanel=new JPanel(new BorderLayout());
        scalePanel.setBorder(BorderFactory.createTitledBorder("Image Scale"));
        JPanel intScalePanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner scaleSpinner=new JSpinner(new SpinnerNumberModel(100, 2, 2000, 1));
        intScalePanel.add(scaleSpinner);
        intScalePanel.add(new JLabel("%"));
        scalePanel.add(intScalePanel);
        fc.setAccessory(scalePanel);

        FileNameExtensionFilter GIFfilter=new FileNameExtensionFilter("GIF image", "gif","GIF");
        FileNameExtensionFilter JPGfilter=new FileNameExtensionFilter("JPG image", "jpg","JPG");
        FileNameExtensionFilter PNGfilter=new FileNameExtensionFilter("PNG image", "png","PNG");
//        FileNameExtensionFilter SVGfilter=new FileNameExtensionFilter("SVG image", "svg","SVG");
//        FileNameExtensionFilter PDFfilter=new FileNameExtensionFilter("PDF file", "PDF","PDF");
//        FileNameExtensionFilter EPSfilter=new FileNameExtensionFilter("EPS image", "eps","EPS");

        fc.addChoosableFileFilter(GIFfilter);
        fc.addChoosableFileFilter(JPGfilter);
        fc.addChoosableFileFilter(PNGfilter);
//        fc.addChoosableFileFilter(SVGfilter);
//        fc.addChoosableFileFilter(PDFfilter);
//        fc.addChoosableFileFilter(EPSfilter);

        File preselected=MotifLabEngine.getFile(parentDir, "sequences.png");
        fc.setSelectedFile(preselected);
        int returnValue=fc.showSaveDialog(getFrame());
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            file=fc.getSelectedFile();
        } else {
            getFrame().setCursor(Cursor.getDefaultCursor());
            return;
        }
        String suffix="";
        String filename=file.getName();
             if (filename.endsWith(".png") || filename.endsWith(".PNG")) suffix="png";
        else if (filename.endsWith(".gif") || filename.endsWith(".GIF")) suffix="gif";
        else if (filename.endsWith(".jpg") || filename.endsWith(".JPG")) suffix="jpg";
        else if (filename.endsWith(".svg") || filename.endsWith(".svg")) suffix="svg";
        else if (filename.endsWith(".pdf") || filename.endsWith(".pdf")) suffix="pdf";
        else if (filename.endsWith(".eps") || filename.endsWith(".eps")) suffix="eps";
        else if (fc.getFileFilter()==GIFfilter) {suffix="gif";file=new File(file.getPath()+".gif");}
        else if (fc.getFileFilter()==JPGfilter) {suffix="jpg";file=new File(file.getPath()+".jpg");}
        else if (fc.getFileFilter()==PNGfilter) {suffix="png";file=new File(file.getPath()+".png");}
//        else if (fc.getFileFilter()==SVGfilter) {suffix="svg";file=new File(file.getPath()+".svg");}
//        else if (fc.getFileFilter()==PDFfilter) {suffix="pdf";file=new File(file.getPath()+".pdf");}
//        else if (fc.getFileFilter()==EPSfilter) {suffix="eps";file=new File(file.getPath()+".eps");}
        else {
            JOptionPane.showMessageDialog(getFrame(), "Unknown image format.\nPlease enter a filename that ends with '.png', '.gif', or '.jpg'", "Save Image", JOptionPane.WARNING_MESSAGE);
            getFrame().setCursor(Cursor.getDefaultCursor());
            return;
        }
        if (file.exists()) {
            int choice=JOptionPane.showConfirmDialog(getFrame(), "Overwrite existing file \""+file.getName()+"\" ?","Save Image",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) {
                getFrame().setCursor(Cursor.getDefaultCursor());
                return;
            }
        }
        int scalePercentage=(Integer)scaleSpinner.getValue();
        double scale=((double)scalePercentage)/100.0;

//        double scale=1.0; // the scaling does not produce very good images (probably AWT's fault)
        saveImageInBackground(file,suffix, scale);
        setLastUsedDirectory(file.getParentFile());
    }


  /** A convenience method to save images off the EDT */
  private void saveImageInBackground(final File file, final String suffix, final double scale) {
        //statusMessage("Saving image to \""+file.getName()+"\"");
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                JPanel panel=visualizationPanel.getMainVisualizationPanel();
                int width=visualizationSettings.getSequenceLabelWidth()+visualizationSettings.getSequenceWindowSize()+2*VisualizationSettings.DATATRACK_BORDER_SIZE;
                Font trackLabelsFont=SequenceVisualizer.getTrackLabelsFont();
                int maxtracklabelwidth=0;
                ArrayList<String> trackorder=visualizationSettings.getDatatrackOrder();
                for (String trackname:trackorder) {
                   if (!visualizationSettings.isTrackVisible(trackname)) continue;
                   int tracklabelwidth=visualizationPanel.getFontMetrics(trackLabelsFont).stringWidth(trackname);
                   if (tracklabelwidth>maxtracklabelwidth) maxtracklabelwidth=tracklabelwidth;
                }
                width+=maxtracklabelwidth+16; // 16 is just a padding value
                int height=panel.getHeight();
                if (suffix.equals("png") || suffix.equals("jpg") || suffix.equals("gif")) {
                    //BufferedImage image=new BufferedImage((int)Math.round(width*scale),(int)Math.round(height*scale), BufferedImage.TYPE_INT_RGB);
                    BufferedImage image=new BufferedImage((int)(width*scale), (int)(height*scale), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g=image.createGraphics();
                    g.scale(scale, scale); // logMessage("Saving with scale = "+scale);
                    g.setColor(java.awt.Color.WHITE);
                    g.fillRect(0, 0, width, height);
                    panel.print(g);
                    g.dispose();
                    try {
                        OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
                        ImageIO.write(image, suffix, output);
                        output.close();
                        return Boolean.TRUE;
                    } catch (Exception e) {
                        ex=e;
                        return Boolean.FALSE;
                    }
                } else { // NOTE: This does not work yet!!!!!
                    VectorGraphics2D g=null;
                         if (suffix.equals("svg")) g = new SVGGraphics2D(0, 0, width, height);
                    else if (suffix.equals("pdf")) g = new PDFGraphics2D(0, 0, width, height);
                    else if (suffix.equals("eps")) g = new EPSGraphics2D(0, 0, width, height);
                    else {ex=new IOException("Unknown image format: "+suffix); return false;}
                    //g.setClip(0, 0, width,height);
                    g.setColor(java.awt.Color.WHITE);
                    g.fillRect(0, 0, width, height);
                    panel.print(g);
                    OutputStream output=null;
                    try {
                        output=MotifLabEngine.getOutputStreamForFile(file);
                        output.write(g.getBytes());
                        return Boolean.TRUE;
                    } catch (Exception e) {
                        ex=e;
                        return Boolean.FALSE;
                    } finally {
                        g.dispose();
                        try {output.close();} catch (Exception ee) {}
                    }
                }

            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     JOptionPane.showMessageDialog(getFrame(), ex.getClass().getSimpleName().toString()+":\n"+ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                     logMessage("Unable to save to \""+file.getName()+"\"  ("+ex.getMessage()+")");
                } else { // save went OK
                     logMessage("Saved image to \""+file.getName()+"\"");
                }
                getFrame().setCursor(Cursor.getDefaultCursor());
            }
        }; // end of SwingWorker class
        worker.execute();
    }

  /** A convenience method to save images off the EDT */
  private void saveSequencesAsIndividualImagesInBackground(final File dir, final String suffix, final String subset) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            int count=0;
            @Override
            public Boolean doInBackground() {
                if (!(suffix.equals("png") || suffix.equals("jpg") || suffix.equals("gif"))) {ex=new ExecutionError("Unknown image format: "+suffix);return Boolean.FALSE;}
                //int width=visualizationSettings.getSequenceWindowSize()+2*VisualizationSettings.DATATRACK_BORDER_SIZE;
                BufferedImage testimage=new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
                Graphics2D testgraphics=testimage.createGraphics();
                ArrayList<String> sequenceNames;
                Data seqData=engine.getDataItem(subset);
                if (seqData instanceof SequenceCollection) {
                    sequenceNames=((SequenceCollection)seqData).getAllSequenceNames();
                } else if (seqData instanceof Sequence) {
                    sequenceNames=new ArrayList<String>(1);
                    sequenceNames.add(subset);
                } else sequenceNames=new ArrayList<String>(0);

                for (String sequenceName:sequenceNames) {
                    SequenceVisualizer seqViz=visualizationPanel.getSequenceVisualizer(sequenceName);
                    if (seqViz==null) {ex=new ExecutionError("Found no visualization for sequence '"+sequenceName+"'");return Boolean.FALSE;}
                    try {
                        if (seqViz.isVisualizerDirty()) {seqViz.paint(testgraphics);} // force initialization by painting in a different image
                        JPanel panel=seqViz.getTracksPanel();
                        java.awt.Rectangle rect=seqViz.getTrackPanelBounds();
                        if (rect.width==0 || rect.height==0) logMessage("Skipping image for sequence '"+sequenceName+"' (size="+rect.width+","+rect.height+")");
                        BufferedImage image=new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g=image.createGraphics();
                        g.setColor(java.awt.Color.WHITE);
                        g.fillRect(0, 0, rect.width, rect.height);
                        g.translate(-rect.x, -rect.y); // the panel contains other things besides the tracks. The rectangle holds the bounds for the tracks.
                        seqViz.paint(g); // I paint the whole thing but only the tracks will fit inside the image now
                        g.dispose();
                        File newfile=MotifLabEngine.getFile(dir, sequenceName+"."+suffix);
                        OutputStream output=MotifLabEngine.getOutputStreamForFile(newfile);
                        ImageIO.write(image, suffix, output);
                        output.close();
                        count++;
                    } catch (Exception e) {
                        ex=e;
                        return Boolean.FALSE;
                    }
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     JOptionPane.showMessageDialog(getFrame(), ex.getClass().getSimpleName().toString()+":\n"+ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                     logMessage("Save error: "+ex.getMessage());
                } else { // save went OK
                     logMessage("Saved "+count+" sequence image"+((count==1)?"":"s")+" to "+dir.getAbsolutePath());
                }
                getFrame().setCursor(Cursor.getDefaultCursor());
            }
        }; // end of SwingWorker class
        worker.execute();
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
        if (!(dir instanceof DataRepositoryFile)) { // not a repository file but check if it could be!
            DataRepositoryFile repfile=DataRepositoryFile.getRepositoryFileFromString(dir.getPath(), engine);
            if (repfile!=null) dir=repfile;
        }
        if (dir.exists() && !dir.isDirectory()) dir=dir.getParentFile(); // a regular file is submitted. Use its parent directory
        lastUsedDirectory=dir;
    }


    @Override
    public void initializeClient(MotifLabEngine engine) {

    }

    private void initializePluginsFromClient() {
       for (Plugin plugin:engine.getPlugins()) {
            try {
                plugin.initializePluginFromClient(this);
            } catch (Throwable e) {
                logMessage("Unable to initialize plugin \""+plugin.getPluginName()+"\" from client: "+e.getMessage());
                logError(e);
            }
       }
    }


    /** Returns a reference to the menu containing recent protocols*/
    public JMenu getFileOpenRecentProtocolsSubmenu() {
        return  fileOpenRecentSubmenu;
    }

    /**
     * This class is registered as a listener that is notified when the user tries to exit the application
     * It can veto the exit by returning FALSE in the canExit method
     */
    private class ConfirmExit implements Application.ExitListener {
        @Override
        public boolean canExit(EventObject arg0) {
            if (mainWindow.isAnyDirty() && doPromptBeforeDiscard()) {
                int option=JOptionPane.showConfirmDialog(getFrame(),"Save changes before exit?","Exit MotifLab",JOptionPane.YES_NO_CANCEL_OPTION);
                if (option==JOptionPane.CANCEL_OPTION) return false;
                else if (option==JOptionPane.YES_OPTION) mainWindow.saveAllTabs();
            }
            return true;
        }
        @Override
        public void willExit(EventObject arg0) {}

    }

    /** The next two methods implement parts of the MotifLabClient interface */
      final boolean[] answer=new boolean[]{false,false}; // just a quick cheat to get around "final" :-)
      @Override
      public boolean shouldRetry(final ExecutableTask t, final Exception e) {
            // answer[0]=true if should retry. If answer[0]==false then answer[1] should be set to true if rollback should be performed
            //e.printStackTrace(System.err);
            int linenumber=t.getLineNumber();
            String exceptionMessage=e.getMessage();
            if (e instanceof ExecutionError || e instanceof ParseError || e instanceof SystemError || e instanceof java.util.concurrent.ExecutionException) {
                if (exceptionMessage==null || exceptionMessage.isEmpty()) exceptionMessage=e.getClass().getSimpleName();
            } else {
                exceptionMessage=e.getClass().getSimpleName()+":"+e.getMessage();
            }
            final String msg=(linenumber==0)?exceptionMessage:("An error occurred during execution in line "+linenumber+": \n"+exceptionMessage);
            Runnable runner = new Runnable() {
            @Override
                public void run() {
                    int n=showAbortRetryDialog(MotifLabGUI.this,msg);
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

    public static int showAbortRetryDialog(MotifLabGUI gui, String message) {
        message=formatErrorString(message,120); //
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


// ---------------- Implementation of the DataListener interface -------------
    @Override
    public void dataAdded(Data data) {
        releaseReservedDataName(data.getName());
        if (data instanceof FeatureDataset) {
          updateActionButtons(Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this));
        }
    }
    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {
        releaseReservedDataName(child.getName());
        if (parentDataset==engine.getDefaultSequenceCollection()) {
          updateActionButtons(Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this));
        }
    }
    @Override
    public void dataRemoved(Data data) {
        if (data instanceof FeatureDataset) {
          updateActionButtons(Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this));
        }
    }
    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) {
        if (parentDataset==engine.getDefaultSequenceCollection()) {
          updateActionButtons(Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this));
        }
    }
    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) { }
    @Override
    public void dataUpdated(Data data) { }

    private void updateActionButtons(final ActionMap actionMap) {
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                if (engine.hasDataItemsOfType(FeatureDataset.class)) {
                    actionMap.get("showSequenceInputDialog").setEnabled(false);
                    actionMap.get("showAddSequencesFromFileDialog").setEnabled(false);
                }
                else { // no feature data present
                    actionMap.get("showSequenceInputDialog").setEnabled(true);
                    boolean empty=engine.getDefaultSequenceCollection().isEmpty();
                    actionMap.get("showAddSequencesFromFileDialog").setEnabled(empty);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }


    /** Adds the new menu item to the menu with the given name
     * @param menuName The name of the menu that the new item should be added to (e.g. "Tools")
     * @param item The menu item to add
     * @param anchor If this parameter is provided, the new menu item will be placed either before
     *        or after the element with the given text (or internal name) given by this parameter.
     *        If the anchor is NULL, the new item will be added to the end of the menu
     * @param before This parameter is only applicable if the 'position' parameter is not NULL
     *        It controls whether the new item should be placed before (TRUE) or after (FALSE) the
     *        item named in the anchor parameter
     * @return TRUE if the menu was found and the item was added. FALSE if no such menu was found
     */
    public boolean addToMenu(String menuName, JMenuItem item, String anchor, boolean before) {
        for (int i=0;i<getMenuBar().getMenuCount();i++) {
            JMenu menu=getMenuBar().getMenu(i);
            if (menu.getText().equals(menuName)) {
                if (anchor!=null) { // place the new menu item at a specific location
                     for (int j=0;j<menu.getMenuComponentCount();j++) {
                        Component comp=menu.getMenuComponent(j);
                        if ((comp instanceof JMenuItem && anchor.equals(((JMenuItem)comp).getText())) || (!(comp instanceof JMenuItem) && anchor.equals(comp.getName()))) {
                            menu.add(item, (before)?j:j+1);
                            return true;
                        }
                    }
                }
                // just add to end
                menu.add(item);
                return true;
            }
        }
        return false;
    }

    /** Adds the new menu item to the menu with the given name
     *  The new item will be added between two named components. If other menu items
     *  are present between these two components, the new menu item will be added in sorted order.
     *  If the anchor components are not found, the menu item will just be added to the end of the menu
     * @param menuName The name of the menu that the new item should be added to (e.g. "Tools")
     * @param item The menu item to add
     * @param anchor1 The first anchor.  This is usually the name of a Separator component (e.g. "toolMenuSeparator1")
     * @param anchor2 The second anchor. This is usually the name of a Separator component (e.g. "toolMenuSeparator2")
     * @return TRUE if the menu was found and the item was added. FALSE if the menu was not found.
     */
    public boolean addToMenu(String menuName, JMenuItem item, String anchor1, String anchor2) {
        for (int i=0;i<getMenuBar().getMenuCount();i++) {
            JMenu menu=getMenuBar().getMenu(i);
            if (menu.getText().equals(menuName)) {
                if (anchor1!=null && anchor2!=null && !anchor1.equals(anchor2)) { // place the new menu item at a specific location
                     int index1=-1,index2=-1;
                     for (int j=0;j<menu.getMenuComponentCount();j++) {
                        Component comp=menu.getMenuComponent(j);
                        if ((comp instanceof JMenuItem && anchor1.equals(((JMenuItem)comp).getText())) || (!(comp instanceof JMenuItem) && anchor1.equals(comp.getName()))) {
                            index1=j;
                            break;
                        }
                     }
                     for (int j=0;j<menu.getMenuComponentCount();j++) {
                        Component comp=menu.getMenuComponent(j);
                        if ((comp instanceof JMenuItem && anchor2.equals(((JMenuItem)comp).getText())) || (!(comp instanceof JMenuItem) && anchor2.equals(comp.getName()))) {
                            index2=j;
                            break;
                        }
                     }
                     if (index1>=0 && index2>=0) {
                         if (index2<index1) {int swap=index2;index2=index1;index1=swap;} // make sure index1<index2
                         for (int j=index1+1;j<index2;j++) {
                             Component comp=menu.getMenuComponent(j);
                             if ((comp instanceof JMenuItem && item.getText().compareToIgnoreCase(((JMenuItem)comp).getText())<0) || (!(comp instanceof JMenuItem) && item.getText().compareToIgnoreCase(comp.getName())<0)) {
                                menu.add(item, j);
                                return true;
                             }
                         }
                         menu.add(item, index2); // add to the end just before second anchor
                         return true;
                     }
                }
                // just add to end
                menu.add(item);
                return true;
            }
        }
        return false;
    }

    /** Removes the menu item from the menu with the given text or internal name
     * @return TRUE if the menu item was found and the item was removed. FALSE if no such menu and item was found
     */
    public boolean removeFromMenu(String menuName, String itemName) {
        for (int i=0;i<getMenuBar().getMenuCount();i++) {
            JMenu menu=getMenuBar().getMenu(i);
            if (menu.getText().equals(menuName)) {
                for (int j=0;j<menu.getMenuComponentCount();j++) {
                    Component comp=menu.getMenuComponent(j);
                    if ((comp instanceof JMenuItem && itemName.equals(((JMenuItem)comp).getText())) || itemName.equals(comp.getName())) {
                        menu.remove(comp);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * A convenience method to add a new menu item to the third section of the Tools menu
     * @return
     */
    public boolean addToToolsMenu(JMenuItem item) {
        return addToMenu("Tools", item, "toolsMenuSeparator2", "toolsMenuSeparator3");
    }

    /** Returns the menu bar for this GUI */
    public JMenuBar getTopMenuBar() {
        return getMenuBar();
    }

    /** Returns the tool bar for this GUI */
    public JToolBar getMainToolBar() {
        return getToolBar();
    }

    public JFrame getGUIFrame() {
        return (clientFrame!=null)?clientFrame:getFrame();
    }

    /** This method should only be used when this GUI is used as a proxy and not as the main client */
    public void setClientFrame(JFrame clientFrame) {
        this.clientFrame=clientFrame;
    }

    @Action
    public void showMotifBrowser() {
        if (motifbrowser!=null && motifbrowser.isVisible()) return;
        getGUIFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        motifbrowser=new MotifBrowser(this);
        //addRedrawListener(motifbrowser);
        int x=getGUIFrame().getWidth()/2-motifbrowser.getWidth()/2; if (x<0) x=0;
        int y=getGUIFrame().getHeight()/2-motifbrowser.getHeight()/2; if (y<0) y=0;
        motifbrowser.setLocation(x,y);
        motifbrowser.setVisible(true);
        getGUIFrame().setCursor(Cursor.getDefaultCursor());
        if (!engine.hasDataItemsOfType(Motif.class)) {
            JOptionPane.showMessageDialog(getGUIFrame(), "<html>To obtain motifs, go to the <b>Data</b> menu and select<br><br>Add New &rarr; Motif Collection<br><br>to choose from a list of predefined collections,<br>or go to the <b>Operations</b> menu and select<br><br>Motif &rarr; motifDiscovery<br><br>to perform <i>de novo</i> motif discovery in a set of sequences</html>","No motifs", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    @Action
    public void showModuleBrowser() {
        if (modulebrowser!=null && modulebrowser.isVisible()) return;
        getGUIFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        modulebrowser=new ModuleBrowser(this);
        //addRedrawListener(modulebrowser);
        int x=getGUIFrame().getWidth()/2-modulebrowser.getWidth()/2; if (x<0) x=0;
        int y=getGUIFrame().getHeight()/2-modulebrowser.getHeight()/2; if (y<0) y=0;
        modulebrowser.setLocation(x,y);
        modulebrowser.setVisible(true);
        getGUIFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showSequenceBrowser() {
        if (sequencebrowser!=null && sequencebrowser.isVisible()) return;
        getGUIFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        sequencebrowser=new SequenceBrowser(this);
        //addRedrawListener(sequencebrowser);
        int x=getGUIFrame().getWidth()/2-sequencebrowser.getWidth()/2; if (x<0) x=0;
        int y=getGUIFrame().getHeight()/2-sequencebrowser.getHeight()/2; if (y<0) y=0;
        sequencebrowser.setLocation(x,y);
        sequencebrowser.setVisible(true);
        getGUIFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action
    public void showRegionBrowser() {
        showRegionBrowser(null);
    }

    public void showRegionBrowser(RegionDataset dataset) {
        if (regionbrowser!=null && regionbrowser.isVisible()) return;
        getGUIFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        regionbrowser=new RegionBrowser(this, dataset);
        //addRedrawListener(sequencebrowser);
        int x=getGUIFrame().getWidth()/2-regionbrowser.getWidth()/2; if (x<0) x=0;
        int y=getGUIFrame().getHeight()/2-regionbrowser.getHeight()/2; if (y<0) y=0;
        regionbrowser.setLocation(x,y);
        regionbrowser.setVisible(true);
        getGUIFrame().setCursor(Cursor.getDefaultCursor());
    }



    @Action
    public void showMotifScoreFilterDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final MotifScoreFilterDialog scoredialog=new MotifScoreFilterDialog(this);
        scoredialog.setLocation(getFrame().getWidth()/2-scoredialog.getWidth()/2, getFrame().getHeight()/2-scoredialog.getHeight()/2);
        scoredialog.setVisible(true);
        // Do not dispose! The dialog is non-modal an will dispose itself on close!
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showInteractionsFilterDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final InteractionsFilterDialog interactionsdialog=new InteractionsFilterDialog(this);
        interactionsdialog.setLocation(getFrame().getWidth()/2-interactionsdialog.getWidth()/2, getFrame().getHeight()/2-interactionsdialog.getHeight()/2);
        interactionsdialog.setVisible(true);
        // Do not dispose! The dialog is non-modal an will dispose itself on close!
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showPositionalDistributionViewerDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final PositionalDistributionViewer dialog=new PositionalDistributionViewer(this);
        dialog.setLocation(getFrame().getWidth()/2-dialog.getWidth()/2, getFrame().getHeight()/2-dialog.getHeight()/2);
        dialog.setVisible(true);
        // Do not dispose! The dialog is non-modal an will dispose itself on close!
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showSortSequencesDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final SortSequencesDialog sortdialog=new SortSequencesDialog(this);
        sortdialog.setLocation(getFrame().getWidth()/2-sortdialog.getWidth()/2, getFrame().getHeight()/2-sortdialog.getHeight()/2);
        sortdialog.setVisible(true);
        sortdialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showCropSequencesDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final CropSequencesDialog cropdialog=new CropSequencesDialog(this);
        cropdialog.setLocation(getFrame().getWidth()/2-cropdialog.getWidth()/2, getFrame().getHeight()/2-cropdialog.getHeight()/2);
        cropdialog.setVisible(true);
        cropdialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showExtendSequencesDialog() {
        if (engine.hasDataItemsOfType(FeatureDataset.class)) {
            JOptionPane.showMessageDialog(getFrame(), "Sequences can not be extended after feature data has been loaded", "Extend Sequences", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final ExtendSequencesDialog extenddialog=new ExtendSequencesDialog(this);
        extenddialog.setLocation(getFrame().getWidth()/2-extenddialog.getWidth()/2, getFrame().getHeight()/2-extenddialog.getHeight()/2);
        extenddialog.setVisible(true);
        extenddialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showGeneExpressionAtlasDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final GeneExpressionAtlasDialog dialog=new GeneExpressionAtlasDialog(this);
        dialog.setLocation(getFrame().getWidth()/2-dialog.getWidth()/2, getFrame().getHeight()/2-dialog.getHeight()/2);
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showAnnotateMotifsDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final AnnotateMotifsDialog annotatedialog=new AnnotateMotifsDialog(this);
        annotatedialog.setLocation(getFrame().getWidth()/2-annotatedialog.getWidth()/2, getFrame().getHeight()/2-annotatedialog.getHeight()/2);
        annotatedialog.setVisible(true);
        annotatedialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action
    public void showConfigureDatatracksDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DataTrackConfigurationDialog dialog=new DataTrackConfigurationDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showMacroEditor() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        MacrosDialog dialog=new MacrosDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
        protocolEditor.updateProtocolRendering(); // this will reparse the current protocol and report if there are any errors
    }


    @Action
    public void editGUIstartupScript() {
        String filename=engine.getMotifLabDirectory()+File.separator+"startup.config";
        File file=new File(filename);
        if (!file.exists()) { // try installing it once more
            try {
                InputStream stream=this.getClass().getResourceAsStream("/motiflab/engine/resources/startup.config");
                engine.installConfigFileFromStream("startup.config",stream);
            } catch (Exception e) {} // don't bother with the error.
        }
        if (file.exists()) {
            protocolEditor.openProtocolFile(file);
        } else {
            JOptionPane.showMessageDialog(getFrame(), "Unable to open \"startup.config\"", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Action
    public void showConfigureOrganismsAndIdentifiersDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ConfigureOrganismsAndIdentifiersDialog dialog=new ConfigureOrganismsAndIdentifiersDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }


    @Action
    public void showPluginsDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ConfigurePluginsDialog dialog=new ConfigurePluginsDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void showDataRepositoriesDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ConfigureDataRepositoriesDialog dialog=new ConfigureDataRepositoriesDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void clearAllDataActionMethod() {
        if (engine.isDataStorageEmpty()) return;
        if (!scheduler.isIdle()) {logMessage("Unable to delete data while executing tasks");return;}
        if (doPromptBeforeDiscard()) {
           int option=JOptionPane.showConfirmDialog(getFrame(), "Do you really want to clear all data?", "Delete Data", JOptionPane.OK_CANCEL_OPTION);
           if (option==JOptionPane.CANCEL_OPTION) return;
        }
        if (motifbrowser!=null) {
            motifbrowser.setVisible(false);
            motifbrowser.dispose();
            motifbrowser=null;
        }
        if (modulebrowser!=null) {
            modulebrowser.setVisible(false);
            modulebrowser.dispose();
            modulebrowser=null;
        }
        if (sequencebrowser!=null) {
            sequencebrowser.setVisible(false);
            sequencebrowser.dispose();
            sequencebrowser=null;
        }
        visualizationSettings.enableVisualizationSettingsNotifications(false); // turn off to avoid redundant updates
        clearDataInBackground("Clear All Data");
    }

    @Action
    public void clearAllModulesActionMethod() {
        if (engine.isDataStorageEmpty()) return;
        if (!scheduler.isIdle()) {logMessage("Unable to delete data while executing tasks");return;}
        if (doPromptBeforeDiscard()) {
           int option=JOptionPane.showConfirmDialog(getFrame(), "Do you really want to clear all modules?", "Delete Data", JOptionPane.OK_CANCEL_OPTION);
           if (option==JOptionPane.CANCEL_OPTION) return;
        }
        if (modulebrowser!=null) {
            modulebrowser.setVisible(false);
            modulebrowser.dispose();
            modulebrowser=null;
        }
        visualizationSettings.enableVisualizationSettingsNotifications(false); // turn off to avoid redundant updates
        clearDataInBackground("Clear Modules Data");
    }

    @Action
    public void clearAllMotifsAndModulesActionMethod() {
        if (engine.isDataStorageEmpty()) return;
        if (!scheduler.isIdle()) {logMessage("Unable to delete data while executing tasks");return;}
        if (doPromptBeforeDiscard()) {
           int option=JOptionPane.showConfirmDialog(getFrame(), "Do you really want to clear all modules and motifs?", "Delete Data", JOptionPane.OK_CANCEL_OPTION);
           if (option==JOptionPane.CANCEL_OPTION) return;
        }
        if (motifbrowser!=null) {
            motifbrowser.setVisible(false);
            motifbrowser.dispose();
            motifbrowser=null;
        }
        if (modulebrowser!=null) {
            modulebrowser.setVisible(false);
            modulebrowser.dispose();
            modulebrowser=null;
        }
        visualizationSettings.enableVisualizationSettingsNotifications(false); // turn off to avoid redundant updates
        clearDataInBackground("Clear Motifs And Modules Data");
    }

    @Action
    public void clearAllSequencesActionMethod() {
        if (engine.getDefaultSequenceCollection().isEmpty()) return;
        if (!scheduler.isIdle()) {logMessage("Unable to delete data while executing tasks");return;}
        if (doPromptBeforeDiscard()) {
           int option=JOptionPane.showConfirmDialog(getFrame(), "Do you really want to clear all sequences and feature data?", "Delete Data", JOptionPane.OK_CANCEL_OPTION);
           if (option==JOptionPane.CANCEL_OPTION) return;
        }
        if (motifbrowser!=null) {
            motifbrowser.setVisible(false);
            motifbrowser.dispose();
            motifbrowser=null;
        }
        if (modulebrowser!=null) {
            modulebrowser.setVisible(false);
            modulebrowser.dispose();
            modulebrowser=null;
        }
        if (sequencebrowser!=null) {
            sequencebrowser.setVisible(false);
            sequencebrowser.dispose();
            sequencebrowser=null;
        }
        visualizationSettings.enableVisualizationSettingsNotifications(false); // turn off to avoid redundant updates
        clearDataInBackground("Clear Sequence Data");
    }

    @Action
    public void clearSequenceRelatedDataMethod() {
        if (engine.getDefaultSequenceCollection().isEmpty()) return;
        if (!scheduler.isIdle()) {logMessage("Unable to delete data while executing tasks");return;}
        if (doPromptBeforeDiscard()) {
           int option=JOptionPane.showConfirmDialog(getFrame(), "Do you really want to clear all sequence related data?", "Delete Data", JOptionPane.OK_CANCEL_OPTION);
           if (option==JOptionPane.CANCEL_OPTION) return;
        }
        visualizationSettings.enableVisualizationSettingsNotifications(false); // turn off to avoid redundant updates
        clearDataInBackground("Clear Sequence Related Data");
    }

    @Action
    public void clearFeatureDataActionMethod() {
        if (engine.isDataStorageEmpty()) return;
        if (!scheduler.isIdle()) {logMessage("Unable to delete data while executing tasks");return;}
        if (doPromptBeforeDiscard()) {
           int option=JOptionPane.showConfirmDialog(getFrame(), "Do you really want to clear all feature data?", "Delete Data", JOptionPane.OK_CANCEL_OPTION);
           if (option==JOptionPane.CANCEL_OPTION) return;
        }
        visualizationSettings.enableVisualizationSettingsNotifications(false); // turn off to avoid redundant updates
        clearDataInBackground("Clear Feature Data");
    }

    @Action
    public void clearOtherDataActionMethod() {
        if (engine.isDataStorageEmpty()) return;
        if (!scheduler.isIdle()) {logMessage("Unable to delete data while executing tasks");return;}
        if (doPromptBeforeDiscard()) {
           int option=JOptionPane.showConfirmDialog(getFrame(), "Do you really want to clear data?", "Delete Data", JOptionPane.OK_CANCEL_OPTION);
           if (option==JOptionPane.CANCEL_OPTION) return;
        }
        clearDataInBackground("Clear Other Data");
    }

    private void clearDataInBackground(final String what) {
        ClearDataTask task=new ClearDataTask(engine,what);
        launchUndoableTask(task);
    }

 /** Creates a new TextVariable based on the given list
  *  and prompts the user to keep or discard it
  */
  public void promptAndCreateTextVariable(ArrayList<String> list) {
     getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     TextVariable collection=new TextVariable(getGenericDataitemName(TextVariable.class,null), list);
     Prompt_TextVariable prompt=new Prompt_TextVariable(this, null, collection, true);
     prompt.setLocation(getFrame().getWidth()/2-prompt.getWidth()/2, getFrame().getHeight()/2-prompt.getHeight()/2);
     prompt.setVisible(true);
     getFrame().setCursor(Cursor.getDefaultCursor());
     if (prompt.isOKPressed()) {
        Data variable=prompt.getData();
        if (variable==null) return;
        String type=TextVariable.getType();
        OperationTask task=new OperationTask("new "+type);
        task.setParameter(OperationTask.OPERATION_NAME, "new");
        task.setParameter(Operation_new.DATA_TYPE, type);
        task.setParameter(OperationTask.TARGET_NAME,variable.getName());
        task.setParameter(OperationTask.ENGINE,getEngine());
        task.setParameter(Operation_new.PARAMETERS, variable.getValueAsParameterString());
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
        variable.setAdditionalOperationNewTaskParameters(task);
        Class datatype=getEngine().getDataClassForTypeName(type);
        task.addAffectedDataObject(variable.getName(), datatype);
        launchOperationTask(task,isRecording());
     }
   }

 /** Creates a new MotifCollection with the motifs in the given list
  *  and prompts the user to keep or discard it
  */
  public void promptAndCreateMotifCollection(ArrayList<String> list) {
     getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     MotifCollection collection=new MotifCollection(getGenericDataitemName(MotifCollection.class, null));
     collection.addMotifNames(list);
     Prompt_MotifCollection prompt=new Prompt_MotifCollection(this, null, collection, true, "Motif Collection", true);
     prompt.setLocation(getFrame().getWidth()/2-prompt.getWidth()/2, getFrame().getHeight()/2-prompt.getHeight()/2);
     prompt.setVisible(true);
     getFrame().setCursor(Cursor.getDefaultCursor());
     if (prompt.isOKPressed()) {
        Data variable=prompt.getData();
        if (variable==null) return;
        String type=MotifCollection.getType();
        OperationTask task=new OperationTask("new "+type);
        task.setParameter(OperationTask.OPERATION_NAME, "new");
        task.setParameter(Operation_new.DATA_TYPE, type);
        task.setParameter(OperationTask.TARGET_NAME,variable.getName());
        task.setParameter(OperationTask.ENGINE,getEngine());
        task.setParameter(Operation_new.PARAMETERS, variable.getValueAsParameterString());
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
        variable.setAdditionalOperationNewTaskParameters(task);
        Class datatype=getEngine().getDataClassForTypeName(type);
        task.addAffectedDataObject(variable.getName(), datatype);
        launchOperationTask(task,isRecording());
        getMotifsPanel().showPanel(MotifsPanel.SHOW_MOTIF_COLLECTIONS);
     }
   }

 /** Creates a new ModuleCollection with the modules in the given list
  *  and prompts the user to keep or discard it
  */
  public void promptAndCreateModuleCollection(ArrayList<String> list) {
     getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     ModuleCollection collection=new ModuleCollection(getGenericDataitemName(ModuleCollection.class, null));
     collection.addModuleNames(list);
     Prompt_ModuleCollection prompt=new Prompt_ModuleCollection(this, null, collection, true);
     prompt.setLocation(getFrame().getWidth()/2-prompt.getWidth()/2, getFrame().getHeight()/2-prompt.getHeight()/2);
     prompt.setVisible(true);
     getFrame().setCursor(Cursor.getDefaultCursor());
     if (prompt.isOKPressed()) {
        Data variable=prompt.getData();
        if (variable==null) return;
        String type=ModuleCollection.getType();
        OperationTask task=new OperationTask("new "+type);
        task.setParameter(OperationTask.OPERATION_NAME, "new");
        task.setParameter(Operation_new.DATA_TYPE, type);
        task.setParameter(OperationTask.TARGET_NAME,variable.getName());
        task.setParameter(OperationTask.ENGINE,getEngine());
        task.setParameter(Operation_new.PARAMETERS, variable.getValueAsParameterString());
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
        variable.setAdditionalOperationNewTaskParameters(task);
        Class datatype=getEngine().getDataClassForTypeName(type);
        task.addAffectedDataObject(variable.getName(), datatype);
        launchOperationTask(task,isRecording());
        getMotifsPanel().showPanel(MotifsPanel.SHOW_MODULE_COLLECTIONS);
     }
   }

     /** Creates a new SequenceCollection with the sequences in the given list
     *  and prompts the user to keep or discard it
     */
   public void promptAndCreateSequenceCollection(ArrayList<String> list) {
     getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     SequenceCollection collection=new SequenceCollection(getGenericDataitemName(SequenceCollection.class, null));
     collection.addSequenceNames(list);
     Prompt_SequenceCollection prompt=new Prompt_SequenceCollection(this, null, collection, true);
     prompt.setLocation(getFrame().getWidth()/2-prompt.getWidth()/2, getFrame().getHeight()/2-prompt.getHeight()/2);
     prompt.setVisible(true);
     getFrame().setCursor(Cursor.getDefaultCursor());
     if (prompt.isOKPressed()) {
        Data variable=prompt.getData();
        if (variable==null) return;
        String type=SequenceCollection.getType();
        OperationTask task=new OperationTask("new "+type);
        task.setParameter(OperationTask.OPERATION_NAME, "new");
        task.setParameter(Operation_new.DATA_TYPE, type);
        task.setParameter(OperationTask.TARGET_NAME,variable.getName());
        task.setParameter(OperationTask.ENGINE,getEngine());
        task.setParameter(Operation_new.PARAMETERS, variable.getValueAsParameterString());
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
        variable.setAdditionalOperationNewTaskParameters(task);
        Class datatype=getEngine().getDataClassForTypeName(type);
        task.addAffectedDataObject(variable.getName(), datatype);
        launchOperationTask(task,isRecording());
     }
   }

 /** Creates a new MotifNumericMap based on values from the given HashMap
  *  and prompts the user to keep or discard it
  */
  public void promptAndCreateMotifNumericMap(HashMap<String,Double> values) {
     getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     MotifNumericMap map=new MotifNumericMap(getGenericDataitemName(MotifNumericMap.class,null), values, 0);
     Prompt_MotifNumericMap prompt=new Prompt_MotifNumericMap(this, null, map, true);
     prompt.setLocation(getFrame().getWidth()/2-prompt.getWidth()/2, getFrame().getHeight()/2-prompt.getHeight()/2);
     prompt.setVisible(true);
     getFrame().setCursor(Cursor.getDefaultCursor());
     if (prompt.isOKPressed()) {
        Data variable=prompt.getData();
        if (variable==null) return;
        String type=MotifNumericMap.getType();
        OperationTask task=new OperationTask("new "+type);
        task.setParameter(OperationTask.OPERATION_NAME, "new");
        task.setParameter(Operation_new.DATA_TYPE, type);
        task.setParameter(OperationTask.TARGET_NAME,variable.getName());
        task.setParameter(OperationTask.ENGINE,getEngine());
        task.setParameter(Operation_new.PARAMETERS, variable.getValueAsParameterString());
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
        variable.setAdditionalOperationNewTaskParameters(task);
        Class datatype=getEngine().getDataClassForTypeName(type);
        task.addAffectedDataObject(variable.getName(), datatype);
        launchOperationTask(task,isRecording());
     }
   }

    /** This is a callback method invoked by GuiScheduler to report that Execution of a task or protocol script has started
     *  Note that this method is NOT called on the EDT!
     */
    public void reportExecutionStarted() {
        final javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        // the actionMap can not be retrieved within the runner below, because then the GUI will freeze :-|
        Runnable runner=new Runnable() {
            @Override
            public void run() {
                actionMap.get("clearAllDataActionMethod").setEnabled(false);
                actionMap.get("clearAllModulesActionMethod").setEnabled(false);
                actionMap.get("clearAllMotifsAndModulesActionMethod").setEnabled(false);
                actionMap.get("clearAllSequencesActionMethod").setEnabled(false);
                actionMap.get("restoreSessionActionMethod").setEnabled(false);
                actionMap.get("restoreSessionFromURLActionMethod").setEnabled(false);
                actionMap.get("showSequenceInputDialog").setEnabled(false);
                fileRestoreRecentSessionSubmenu.setEnabled(false);
            }
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) runner.run();
            else  SwingUtilities.invokeLater(runner);
        } catch (Exception ie) {}
    }

    /**
     * This is a callback method invoked by GuiScheduler to report that Execution of a task or protocol script has ended
     */
    public void reportExecutionEnded() {
        reservedDataNames.clear();
        final javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        // the actionMap can not be retrieved within the runner below, because then the GUI will freeze :-|
        Runnable runner=new Runnable() {
            @Override
            public void run() {
                actionMap.get("clearAllDataActionMethod").setEnabled(true);
                actionMap.get("clearAllModulesActionMethod").setEnabled(true);
                actionMap.get("clearAllMotifsAndModulesActionMethod").setEnabled(true);
                actionMap.get("clearAllSequencesActionMethod").setEnabled(true);
                actionMap.get("restoreSessionActionMethod").setEnabled(true);
                actionMap.get("restoreSessionFromURLActionMethod").setEnabled(true);
                if (!engine.hasDataItemsOfType(FeatureDataset.class)) {
                   actionMap.get("showSequenceInputDialog").setEnabled(true);
                }
                if (recentsessions.isEmpty()) fileRestoreRecentSessionSubmenu.setEnabled(false);
                else fileRestoreRecentSessionSubmenu.setEnabled(true);
            }
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) runner.run();
            else  SwingUtilities.invokeLater(runner);
        } catch (Exception ie) {}

    }

    @Action
    public void selectColorProtocol() {
       protocolEditor.setUseColors(colorKeywordsMenuItem.isSelected());
    }

    @Action
    public void showConfigureExternalProgramsDialog() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ExternalProgramsDialog dialog=new ExternalProgramsDialog(this);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        getFrame().setCursor(Cursor.getDefaultCursor());
    }

    public void addRedrawListener (RedrawListener listener) {
        if (redrawlisteners==null) redrawlisteners=new ArrayList<RedrawListener>();
        redrawlisteners.add(listener);
    }

    public void removeRedrawListener (RedrawListener listener) {
        if (redrawlisteners!=null) {
            redrawlisteners.remove(listener);
        }
    }

    /** Executes an internal command in the GUI. This can be used to programmatically invoke some action in the GUI
     *  for example in response to clicking a link on the Welcome page
     * @param command A recognized internal command which invokes some action
     *        This should be on the format: <command>=<target>
     *        Recognized command include:
     *        - protocol = <URL>     Opens up a protocol from the given URL into the editor
     *        - session = <URL>      Loads a session from the given URL
     *        - action = <command>   Invokes an action command stored in the action map of the GUI (can e.g. be used to display some tool dialogs)
     *        - showTab = <tabname>  shows the tab with the given name in the main panel
     *        - operation = <name>   Launches the "execute Operation" dialog for the operation with the given name
     *        - addData = <type>     Launches the "add data" dialog for the given data type
     *        - show = <name>        Shows the data object with the given name in a prompt, or shows the output tab (for outputdata ) or visualization panel (for feature data and sequences)
     *        - flash = <panel>      Highlights the GUI panel with the given name by flashing a yellow translucent box over it for a short time
     *        - click = <target>     Sends a "doClick" signal to one of a few registered components
     *        - clickMenu = <path>   Clicks the menu, submenu or menu item with the given name to either expand the (sub)menu or invoke the action of a menu item.
     *                               The path of a menu item can be specific on the form "topmenu.submenu1.submenu2.menuitem".
     *                               If a menu or item name contains periods, these must be specified as stars instead, e.g. "Configure.Options***"
     *        - showMenu = <path>    Shows the menu item as selected but without invoking the action behind it (as clickMenu would).
     *                               If the menu item is a top-level menu or submenu (but not a "leaf" menu item), you should use clickMenu instead to make sure the menu is expanded
     *        - help = <helpstring>  Displays the Help dialog for the given string
     */
    public void executeInternalCommand(String command) {
        if (command.contains("=")) {
            String[] split=command.split("=", 2);
            if (split.length<2) {logMessage("Missing parameter for command: "+command);return;}
            command=split[0];
            if (command.equals("protocol")) {
                String protocolURL=split[1];
                try {
                    URL url=new URL(protocolURL);
                    protocolEditor.openProtocolFile(url);
                } catch (MalformedURLException e) {
                    logMessage("URL error:"+protocolURL);
                }
            } else if (command.equals("session")) {
                String sessionURL=split[1];
                try {
                    URL url=new URL(sessionURL);
                    restoreSessionInBackground(url);
                } catch (MalformedURLException e) {
                    logMessage("URL error:"+sessionURL);
                }
            } else if (command.equals("action")) {
                String actioncommand=split[1];
                javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
                javax.swing.Action action=actionMap.get(actioncommand);
                if (action!=null) action.actionPerformed(new ActionEvent(this,0,actioncommand));
                else logMessage("Unrecognized action command: "+actioncommand);
            } else if (command.equals("showTab")) {
                String tabname=split[1];
                mainWindow.setSelectedTab(tabname);
            } else if (command.equals("help")) {
                String helpstring=split[1];
                showHelpDialog(helpstring);
            } else if (command.equals("operation")) {
                String[] parts=split[1].split(",");
                String operationName=parts[0];
                Operation operation=engine.getOperation(operationName);
                OperationTask parameters=new OperationTask(operationName);
                parameters.setParameter(OperationTask.OPERATION, operation);
                if (parts.length>1) {
                    for (int i=1;i<parts.length;i++) {
                        String[] pair=parts[i].split("=");
                        parameters.setParameter(pair[0], pair[1]);
                    }
                }
                MotifLabGUI.this.launchOperationEditor(parameters);
            } else if (command.equals("addData")) {
                try {
                   String dataType=split[1];
                   addDataItemMenuHandler(new ActionEvent(this, 0, dataType));
                } catch (Exception e) {logMessage(e.toString());e.printStackTrace(System.err);}
            } else if (command.equals("clickMenu")) {
                String[] menuparts=getMenuParts(split[1]);
                MenuElement target=menuBar;
                disarmRecursively(menuBar);
                for (String menuname:menuparts) {
                    MenuElement found=getMenuSubElement(target,menuname);
                    if (found==null) {logMessage("Unrecognized menu entry:"+menuname);return;}
                    else target=found;
                }
                Component comp=target.getComponent();
                if (comp instanceof JMenuItem) ((JMenuItem)comp).doClick();
            } else if (command.equals("showMenu")) {
                String[] menuparts=getMenuParts(split[1]);
                MenuElement target=menuBar;
                disarmRecursively(menuBar);
                for (String menuname:menuparts) {
                    MenuElement found=getMenuSubElement(target,menuname);
                    if (found==null) {logMessage("Unrecognized menu entry:"+menuname);return;}
                    else {
                        target=found;
                        if (!menuname.equals(menuparts[menuparts.length-1])) {
                            Component comp=target.getComponent();
                            if (comp instanceof JMenuItem) ((JMenuItem)comp).doClick(); // click on the parent submenus to expand them
                        }
                    }
                }
                Component comp=target.getComponent();
                if (comp instanceof JMenuItem) {
                    if (menuparts.length>1) ((JMenuItem)comp).setArmed(true); else ((JMenuItem)comp).doClick(); // "click" for top-level menus else "arm"
                }
            } else if (command.equals("click")) {
                String componentname=split[1];
                Component comp=null;
                if (componentname.equalsIgnoreCase("MotifsPanelMenu")) comp=getMotifsPanel().getContentCombobox();
                if (comp==null) {logMessage("Unrecognized component:"+componentname);return;}
                if (comp instanceof JButton) ((JButton)comp).doClick();
                else if (comp instanceof JComboBox) ((JComboBox)comp).setPopupVisible(true);
            } else if (command.equals("show")) {
                String dataname=split[1];
                Data data=engine.getDataItem(dataname);
                if (data instanceof OutputData) mainWindow.setSelectedTab(dataname);
                else if (data instanceof FeatureDataset || data instanceof Sequence) mainWindow.showVisualizationPanel();
                else showPrompt(data, false, false);
            } else if (command.equals("flash")) {
                String componentname=split[1];
                final javax.swing.JComponent flashpanel;
                if (componentname.equalsIgnoreCase("FeaturesPanel")) flashpanel=featuresPanel;
                else if (componentname.equalsIgnoreCase("MotifsPanel")) flashpanel=getMotifsPanel();
                else if (componentname.equalsIgnoreCase("MotifsPanelMenu")) flashpanel=getMotifsPanel().getContentCombobox();
                else if (componentname.equalsIgnoreCase("DataObjectsPanel")) flashpanel=getDataObjectsPanel();
                else if (componentname.equalsIgnoreCase("MainPanelTabs")) flashpanel=getMainWindow();
                else if (componentname.equalsIgnoreCase("MainPanelCurrentTab")) flashpanel=(JComponent)getMainWindow().getSelectedComponent();
                else if (componentname.equalsIgnoreCase("SidePanel")) flashpanel=sidePanel;
                else if (componentname.equalsIgnoreCase("LogPanel")) flashpanel=logScrollPane;
                else if (componentname.equalsIgnoreCase("recordButton")) flashpanel=recordButton;
                else if (componentname.equalsIgnoreCase("stopButton")) flashpanel=stopButton;
                else if (componentname.equalsIgnoreCase("executeButton")) flashpanel=executeButton;
                else if (componentname.equalsIgnoreCase("sequencesButton")) flashpanel=sequenceButton;
                else if (componentname.equalsIgnoreCase("datatracksButton")) flashpanel=databaseButton;
                else {logMessage("Unrecognized panel:"+componentname);return;}
                PanelFlasher flasher=new PanelFlasher(flashpanel, new java.awt.Color(255,200,0));
                final Timer timer = new Timer(20, flasher);
                flasher.timer=timer;
                timer.start();
            } else logMessage("Unrecognized internal command: "+command);
        } else { // command does not contain '='
            logMessage("Unrecognized internal command: "+command);
        }
    }

    private String[] getMenuParts(String path) {
        String[] menuparts=path.split("\\.");
        for (int i=0;i<menuparts.length;i++) {
            menuparts[i]=menuparts[i].replace('*', '.');
        }
        return menuparts;
    }
    /** Searching a menu to see if it can find the element with the given text among its direct children */
    private MenuElement getMenuSubElement(MenuElement parent, String childName) {
        MenuElement[] children=parent.getSubElements();
        if (children.length>0) {
            for (MenuElement element:children) {
                Component comp=element.getComponent();
                if (comp instanceof JMenuItem) {
                    String text=((JMenuItem)comp).getText();
                    if (text!=null && text.equals(childName)) return element;
                } else if (comp instanceof JPopupMenu) {
                    MenuElement found=getMenuSubElement(element,childName);
                    if (found!=null) return found;
                }
            }
        }
        return null;
    }

    private void disarmRecursively(MenuElement thisElement) {
        Component comp=thisElement.getComponent();
        if (comp instanceof JMenuItem) ((JMenuItem)comp).setArmed(false);
        MenuElement[] children=thisElement.getSubElements();
        if (children.length>0) {
            for (MenuElement element:children) {
                disarmRecursively(element);
            }
        }
    }

    public java.awt.Frame getParentFrame() {
        return this.getFrame();
    }

    @Override
    public boolean handleUncaughtException(final Throwable e) {
        if (e instanceof IllegalStateException) return true; // do nothing for this type of exception
        if (e instanceof ClassCastException && e.getMessage().endsWith("cannot be cast to javax.swing.Painter")) {redraw();return true;} // this is a known Nimbus bug. Perhaps it will go away if I ignore it?
        logError(e); // prints stacktrace if debugging is enabled
        Runnable runner=new Runnable() {
            @Override
            public void run() { //
                if (e instanceof OutOfMemoryError) {
                    JOptionPane.showMessageDialog(getFrame(),"There was not enough available memory left to perform the requested task.\nPlease run MotifLab with more memory to continue with this analysis.", "Out of memory", JOptionPane.ERROR_MESSAGE);
                } else if (e instanceof ConcurrentModificationException) { // aka: Bad Programming Error
                    JOptionPane.showMessageDialog(getFrame(),"A problem of type 2904 has occurred!\n\nThis is not necessarily a serious error, but you might want to try that last thing you did one more time.", "Oops :-|", JOptionPane.ERROR_MESSAGE);
                } else if (e instanceof NullPointerException) { // aka: Bad Programming Error
                    engine.reportError(e);
                } else {
                    int option=JOptionPane.showOptionDialog(getFrame(), e.getClass().getSimpleName()+":\n"+e.getMessage(), "ERROR: Uncaught Exception", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE,null, new String[]{"OK","Send Error Report"},"OK");
                    if (option==1) {
                        engine.reportError(e);
                    }
                }
                if (scheduler!=null) scheduler.abortEmergency(); // in case the error occurred while some task was executing
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
        return true; // this is to implement the interface
    }

    @Override
    /** This method implements shutdown() from MotifLab Client
     *  Note: this method is only called by the engine if a serious error happens which makes the system go down.
     *  The engine will then call client.shutdown(), i.e. this method here, to allow the client to perform cleanup.
     *  This method is not called on normal program exit, so any common clean-up code
     *  should be put in the cleanup() method in this class rather than here.
     */
    public void shutdown() {
        engine.shutdown(); // the engine calls shutdown in the client but does not call shutdown on itself in case the client needs the engine a bit more. So we must shutdown the engine afterwards
        getApplication().exit(); // this will call gui.cleanup()
    }

    @Action
    public void showCitationDialog() {
        String document="<html><body>" +
                "<h2><u>Citations</u></h2>" +
                "If you use MotifLab for your own research please cite the following publication:<br><br><br>" +
                "Klepper, K. and Drabløs, F. (2013)<br>\"MotifLab: a tools and data integration workbench for motif discovery and regulatory sequence analysis\"<br><i>BMC Bioinformatics</i> <b>14:9</b>" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://www.biomedcentral.com/1471-2105/14/9\">Go to paper</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/23323883\">PubMed:23323883</a>"+
                "<br><br>MotifLab is an extension of an earlier program published under the name <b>PriorsEditor</b><br><br>"+
                "Klepper, K. and Drabløs, F. (2010)<br>\"PriorsEditor: a tool for the construction and use of positional priors in motif discovery\"<br><i>Bioinformatics</i> <b>26</b>(17) : 2195-97" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://dx.doi.org/10.1093/bioinformatics/btq357\">Go to paper</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"http://www.ncbi.nlm.nih.gov/pubmed/20628076\">PubMed:20628076</a>"+
                "<br><br><br><br>" +
                "To cite external programs used within MotifLab, please see citation " +
                "information for individual programs under \"Configure\" &rarr; \"External Programs...\"" +
                "</body><html>";
        InfoDialog dialog=new InfoDialog(MotifLabGUI.this, "Citations", document,620,450);
        dialog.setVisible(true);
        dialog.dispose();
    }

    @Action
    public void showWelcomeScreen() {
        try {
            String welcomepage=engine.getWebSiteURL()+"welcomepage.html";
            java.net.URL url=new java.net.URL(welcomepage);
            OutputData welcome=new OutputData("Welcome",url);
            engine.storeDataItem(welcome);
        } catch (Exception e) {logMessage(e.toString());}
    }

    /**
     * Sets the program which should be considered 'first choice' to use for a particular task
     * such as motifscanning. The default program should be preselected in the algorithm combobox
     * in corresponding operation dialogs.
     * @param programtype An external program type, e.g. 'motifscanning', or 'modulediscovery' (case-insensitive)
     * @param programname
     * @return
     */
    public void setDefaultExternalProgram(String programtype, String programname) {
        if (externalProgramDefaults==null) externalProgramDefaults=new HashMap<String, String>(4);
        externalProgramDefaults.put(programtype.toLowerCase(),programname);
        LocalStorage localStorage=getApplication().getContext().getLocalStorage();
        try {
           localStorage.save(externalProgramDefaults, "defaultprograms.ser");
        } catch (Exception e) {System.err.println("Error when saving default program settings:"+e.toString());}
    }

    /**
     * Returns the name of a program which is set as 'first choice' to use for a particular task
     * such as motifscanning, or NULL if no program is set as default for the specified program type.
     * The default program should be preselected in the algorithm combobox in corresponding operation dialogs.
     * @param programtype An external program type, e.g. 'motifscanning', or 'modulediscovery' (case-insensitive)
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getDefaultExternalProgram(String programtype) {
        if (externalProgramDefaults==null) {
            LocalStorage localStorage=getApplication().getContext().getLocalStorage();
            try {
               externalProgramDefaults=(HashMap<String,String>)localStorage.load("defaultprograms.ser");
            } catch (Exception e) {}
            if (externalProgramDefaults==null) return null;
        }
        return externalProgramDefaults.get(programtype.toLowerCase());
    }

    /**
     * If this method returns TRUE, the relative position of TSS (or TES)
     * should be "1" while the base immediately upstream of TSS should be "-1"
     * (i.e. a ruler would show "... -3, -2, -1, +1 [TSS], +2, +3..."
     * If the method returns FALSE, TSS should be at position "0"
     * @return
     */
    public boolean skipPosition0() {
        return skip0; //
    }

    /**
     * Specifies whether MotifLab should skip position "0" when dealing with
     * positions relative to TSS (or TES). If skip is set to TRUE, TSS will be
     * located at "+1" while the base immediately upstream of TSS will be "-1"
     * (i.e. a ruler would show "... -3, -2, -1, +1 [TSS], +2, +3..."
     * If skip is FALSE the TSS will be at position "+0"
     * @skip
     */
    public void setSkipPosition0(boolean skip) {
        skip0=skip;
    }

    private void setNumericTrackOptimizationSettings(int displayValueSetting, int samplingcutoff, int samplingnumber) {
        DataTrackVisualizer_Numeric.display_value_setting=displayValueSetting;
        DataTrackVisualizer_Numeric.sampling_length_cutoff=samplingcutoff;
        DataTrackVisualizer_Numeric.sampling_size=samplingnumber;
    }


    /**
     * This method checks to see if this is the first time the is user
     * starts MotifLab (signaled by the absence of a special flag-file in
     * the work directory). On startup the first time there might be some
     * installations needed and license acceptance and registrations that must
     * be performed. If the method detects that the user has previously started
     * MotifLab it just returns prematurely
     */
    public void firstTimeStartup() {
        if (!engine.isFirstTimeStartup()) return; // is this the first time the user runs MotifLab?
        setProgress(10); // just to have some start value
        String document="<html><body>" +
                "<h1>Welcome to MotifLab</h1>" +
                "<br>" +
                "MotifLab is a workbench for motif discovery and regulatory sequence analysis that can integrate various tools and data sources." +
                "<br><br>" +
                "The software was developed by Kjetil Klepper (kjetil.klepper@ntnu.no) under the supervision of professor Finn Drabløs (finn.drablos@ntnu.no)<br><br>" +
                "The project was supported by the National Programme for Research in Functional Genomics in Norway (FUGE) " +
                "in the Research Council of Norway." +
                "<br><br>MotifLab is open source and free to use \"as is\" for academic and commercial purposes. " +
                "However, no parts of the source code may be reused in its original or modified form as part of other commercial software projects without the consent of the authors. " +
                "Also, it is not permitted to sell or otherwise redistribute MotifLab for profit (directly or indirectly) without the authors' consent.<br>"+
                "Note that MotifLab can link to other external programs whose use might be subject to other license restrictions." +
                "<br><br>" +
                "<center>"+
                "Bioinformatics and Gene Regulation Group (BiGR)<br>"+
                "Department of Clinical and Molecular Medicine<br>"+
                "Norwegian University of Science and Technology (NTNU)" +
                "<br></center><br>"+
                "</body></html>";
        InfoDialog dialog=new InfoDialog(MotifLabGUI.this, "MotifLab installation", document, 500,400);
        dialog.setVisible(true);
        dialog.dispose();
        MotifLabGUI.this.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Perform installation in the background
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                try {
                    engine.installResourcesFirstTime(); // this will install resources and import them.
                } catch (Exception e) {
                    ex=e;
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                MotifLabGUI.this.getFrame().setCursor(Cursor.getDefaultCursor());
                //MotifLabGUI.this.setProgress(-1); // hides progressbar
                if (ex!=null) { // this should return FALSE now if everything is registered OK
                    JOptionPane.showMessageDialog(MotifLabGUI.this.getFrame(), "It seems that something went wrong during installation\nPlease try to restart MotifLab","Install error",JOptionPane.ERROR_MESSAGE);
                } else {
                    setupOperationsMenu(); // the engine.importResources() method that sets up the operations may have been called after setupOperationsMenu() was called the first time. So we need to do it again, just to be sure
                    engine.executeStartupConfigurationProtocol();
                    setDefaultExternalProgram("motifscanning", "SimpleScanner");
                    showWelcomeScreen(); // show welcome screen after installation is completed
                    JOptionPane.showMessageDialog(MotifLabGUI.this.getFrame(), "Installation completed successfully.","Installation completed",JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }


    /*
     * Lets the user select a list of (configuration-) files from the MotifLab directory
     * and creates a backup of these files in a ZIP-file that can be saved to a selected location
     */
   @Action
    public void backupCurrentConfiguration() {
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File file=null;
        File parentDir=getLastUsedDirectory();
        final JFileChooser fc = getFileChooser(parentDir);
        fc.setDialogTitle("Select file to save configuration backup to");
        FileNameExtensionFilter zipFilter=new FileNameExtensionFilter("ZIP archive (*.zip)", "zip");
        fc.addChoosableFileFilter(zipFilter);
        fc.setFileFilter(zipFilter);
        File preselected=MotifLabEngine.getFile(parentDir,"MotifLabConfiguration.zip");
        fc.setSelectedFile(preselected);
        int returnValue=fc.showSaveDialog(getFrame());
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            file=fc.getSelectedFile();
        } else {
            getFrame().setCursor(Cursor.getDefaultCursor());
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".zip")) {
            if (file instanceof DataRepositoryFile) file=new DataRepositoryFile(file.getAbsolutePath()+".zip", ((DataRepositoryFile)file).getRepository());
            else file=new File(file.getAbsolutePath()+".zip");
        }
        if (file.exists()) {
            int choice=JOptionPane.showConfirmDialog(getFrame(), "Overwrite existing file \""+file.getName()+"\" ?","Backup configuration files",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) {
                getFrame().setCursor(Cursor.getDefaultCursor());
                return;
            }
        }
        getFrame().setCursor(Cursor.getDefaultCursor());
        File configDir=new File(engine.getMotifLabDirectory());
        File[] files=configDir.listFiles();
        ArrayList<File> configfiles=new ArrayList<File>();
        for (File cfile:files) {
            if (cfile.getName().endsWith(".config")) configfiles.add(cfile);
        }
        backupConfigurationInBackground(file,configfiles);
        setLastUsedDirectory(file.getParentFile());
    }

 /** A convenience method to save files off the EDT */
  private void backupConfigurationInBackground(final File file, final ArrayList<File> configfiles) {
        //MotifLabGUI.this.setProgress(0);
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                OutputStream outputStream = null;
                ZipOutputStream zout=null;
                FileInputStream fin=null;
                try {
                     outputStream=MotifLabEngine.getOutputStreamForFile(file);
                     byte[] buffer = new byte[1024];
                     zout = new ZipOutputStream(outputStream);
                     for (File configFile:configfiles) {
                        fin = new FileInputStream(configFile);
                        zout.putNextEntry(new ZipEntry(configFile.getName()));
                        int length;
                        while((length = fin.read(buffer)) > 0) {
                           zout.write(buffer, 0, length);
                        }
                        zout.closeEntry();
                        fin.close();
                     }
                     zout.flush();
                     zout.close();
                     outputStream.close();
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {
                        if (zout!=null) zout.close();
                        if (outputStream!=null) outputStream.close();
                        if (fin!=null) fin.close();
                    } catch (Exception x) {}
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     JOptionPane.showMessageDialog(getFrame(), ex.getClass().toString()+":\n"+ex.getMessage(),"File error" ,JOptionPane.ERROR_MESSAGE);
                     logMessage("Unable to save configuration backup to \""+file.getAbsolutePath()+"\"  ("+ex.getClass().toString()+":"+ex.getMessage()+")");
                } else { // save went OK
                     logMessage("Saved configuration backup to \""+file.getAbsolutePath()+"\"");
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }





   /** Returns a String representation of a number with 3 decimal digits. The number is formatted in scientific notation if is is very small or very large */
    public static String formatNumber(double number) {
        if (number==0) return normalNumberFormat.format(number);
        else if (Math.abs(number) < 1E-5 || Math.abs(number) > 1E5) return scientificNumberFormat.format(number);
        else return normalNumberFormat.format(number);
    }

    /** Creates a string which can be used for tooltips based on the given input string
     *  If the length of this string is longer than the given width argument, the string
     *  will be split over multiple lines. The string will be split on spaces or commas
     */
    public static String formatTooltipString(String string, int width, boolean addOpenTag, boolean addCloseTag) {
        int size=string.length();
        if (size<=width) return string;
        StringBuilder newstring=new StringBuilder();
        if (addOpenTag) newstring.append("<html>");
        int currentLineStart=0;
        while (currentLineStart<size) {
           boolean didSplitOnComma=false;
           int spacepos=string.indexOf(' ', currentLineStart+width);
           if (spacepos<0) {
               spacepos=string.indexOf(',', currentLineStart+width);
               didSplitOnComma=(spacepos>=0);
           } // no more spaces. Try a comma
           if (spacepos<0) spacepos=size; // if no more spaces or commas: use rest of string
           if (currentLineStart>0) newstring.append((didSplitOnComma)?",<br>":"<br>");
           newstring.append(string.substring(currentLineStart, spacepos));
           currentLineStart=spacepos+1;
        }
        if (addCloseTag) newstring.append("</html>");
        return newstring.toString();
    }
     public static String formatTooltipString(String string, int width) {
         return formatTooltipString(string, width, true, true);
     }


    public static String formatErrorString(String string, int width) {
        int size=string.length();
        if (size<=width) return string;
        StringBuilder newstring=new StringBuilder();
        if (string.contains("\n")) { // reuse the linebreaks that are present already
            String[] lines=string.split("\n");
            for (String line:lines) {
                breakString(line, width, newstring);
            }
        }
        else breakString(string, width, newstring);
        return newstring.toString().trim();
    }

    private static void breakString(String string, int width, StringBuilder target) {
        int size=string.length();
        int currentLineStart=0;
        while (currentLineStart<size) { // break up the line
           int spacepos=string.indexOf(' ', currentLineStart+width);
           if (spacepos<0) spacepos=size; // if no more spaces: use rest of string
           if (currentLineStart>0) target.append("\n");
           target.append(string.substring(currentLineStart, spacepos));
           currentLineStart=spacepos+1;
        }
        target.append("\n");
    }


    private class PanelFlasher implements ActionListener {
        private int count=0;
        public Timer timer;
        private Color currentColor;
        private JPanel rect;
        private boolean ok=true;
        private final JComponent flashpanel;
        
        public PanelFlasher(JComponent flashpanel, Color startcolor) {
            this.currentColor=startcolor;
            this.flashpanel=flashpanel;
            JPanel glasspane=(JPanel)getFrame().getGlassPane();
            if (glasspane.getLayout()!=null) glasspane.setLayout(null); // use absolute layout on glasspane
            rect=new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Color c=g.getColor();
                    g.setColor(currentColor);
                    g.fillRect(0, 0, rect.getWidth(), rect.getHeight());
                    g.setColor(c);
                }
            };
            try {
                glasspane.add(rect);
                java.awt.Point point=flashpanel.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(point, glasspane);
                rect.setBounds(flashpanel.getBounds());
                rect.setLocation(point);
                rect.setPreferredSize(flashpanel.getSize());
            } catch (java.awt.IllegalComponentStateException e) {
                 ok=false;
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JPanel glasspane=(JPanel)getFrame().getGlassPane();
            glasspane.setVisible(true);
            try { // update flashing rectangle, just in case
                java.awt.Point point=flashpanel.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(point, glasspane);
                rect.setBounds(flashpanel.getBounds());
                rect.setLocation(point);
                rect.setPreferredSize(flashpanel.getSize());
            } catch (java.awt.IllegalComponentStateException ex) {}            
            int alpha=currentColor.getAlpha()-20;
            if (alpha<0) alpha=0;
            currentColor=new Color(currentColor.getRed(),currentColor.getGreen(),currentColor.getBlue(),alpha);
            getFrame().repaint();
            count++;
            if (alpha==0 || !ok) {
                timer.stop();
                glasspane.setVisible(false);
                glasspane.remove(rect);
            }
        }
    }

    public ImageIcon getIcon(String name) {
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/"+name)));
    }

    @Action
    public void saveSessionAndExit() {
        saveSessionOnExit=true;
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(MotifLabGUI.class, this);
        actionMap.get("quit").actionPerformed(null); // invoke "quit" action
    }

    @Action
    public void installConfigurationFile() {
        final JFileChooser fc = getFileChooser(getLastUsedDirectory());
        fc.setDialogTitle("Select configuration file");
        int returnValue=fc.showOpenDialog(this.getFrame());
        if (returnValue!=JFileChooser.APPROVE_OPTION) return;
        File file=fc.getSelectedFile();
        if (file==null) return;
        String filename=file.getName();
        File oldfile=new File(engine.getMotifLabDirectory()+File.separator+filename);
        boolean isZIPfile=filename.toLowerCase().endsWith(".zip");
        if (isZIPfile) {
            int option=JOptionPane.showConfirmDialog(this.getFrame(), "Do you want to replace existing configuration files?", "Update configuration file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (option!=JOptionPane.OK_OPTION) return;
        } else if (oldfile.exists()) {
            int option=JOptionPane.showConfirmDialog(this.getFrame(), "Are you sure you want to replace the existing \""+filename+"\"", "Update configuration file", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (option!=JOptionPane.OK_OPTION) return;
        } else {
            int option=JOptionPane.showConfirmDialog(this.getFrame(), "This is not a recognized configuration file.\nWould you like to install this file anyway?", "Unrecognized Configuration File", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (option!=JOptionPane.OK_OPTION) return;
        }
        try {
            if (isZIPfile) {
                engine.installConfigFilesFromZip(file);
            } else {
                engine.installConfigFile(file);
            }
            JOptionPane.showMessageDialog(this.getFrame(), "Configuration files installed successfully", "Configure", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this.getFrame(), "An error occurred during installation:\n"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        setLastUsedDirectory(file.getParentFile());
    }

    /** Contacts the MotifLab web site to obtain registered notifications and
     *  displays them to the user in a popup dialog
     *  @param getAll if TRUE all the registered notifications will be obtained. If FALSE only new notifications will be fetched
     *  @param display if TRUE, the obtained notifications will be displayed in a dialog. 
     *                 If FALSE, a button will be added to the toolbar if there are any new messages. Clicking on the button will display the dialog
     */
    public void displayNotifications(final boolean getAll, final boolean display) {
        SwingWorker worker=new SwingWorker() { // do this in the background. Do not block user interactions right off the bat
            ArrayList<ImportantNotification> messages=null;
            @Override
            protected Object doInBackground() throws Exception {
                if (preferences==null) preferences = Preferences.userNodeForPackage(this.getClass());
                int level=(getAll)?ImportantNotification.MINOR:preferences.getInt(PREFERENCES_NOTIFICATIONS_MINIMUM_LEVEL, 0);
                if (level<ImportantNotification.MINOR) level=ImportantNotification.MINOR;
                if (level>ImportantNotification.CRITICAL) level=ImportantNotification.CRITICAL;
                messages=engine.getNewNotifications(level, getAll, display);
                return null;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                if (messages==null) messages=new ArrayList<>(0); // just in case
                if (!display) { // just show a button
                    int number=messages.size();
                    if (number>0) {
                         String msg=" new notification"+((number==1)?"":"s");
                         newNotificationsButton.setText(number+msg);
                         newNotificationsButton.setToolTipText(number+msg);
                         Toolbar.add(newNotificationsButton);    
                         Toolbar.revalidate();
                         Toolbar.repaint();
                    }
                    return;
                } 
                Toolbar.remove(newNotificationsButton); // hide the notification button if the dialog is displayed
                Toolbar.revalidate();    
                Toolbar.repaint();                
                String header=(getAll)?"All Notifications":"New notifications";
                StringBuilder builder=new StringBuilder();
                builder.append("<html>");
                builder.append("<head>");
                builder.append("<style type=\"text/css\">\n");
                builder.append("</style>");
                builder.append("</head>");
                builder.append("<body><center><h1>");
                builder.append(header);
                builder.append("</h1><table border=\"1\" width=90%>");
                builder.append("<tr><th bgcolor=\"#8080F0\"><font color=\"#FFFFFF\">Date</font></th><th bgcolor=\"#8080F0\"><font color=\"#FFFFFF\">Significance</font></th><th bgcolor=\"#8080F0\"><font color=\"#FFFFFF\">Message</font></th></tr>");
                SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
                formatter.setTimeZone(TimeZone.getDefault());
                String bg="#FFFFFF";
                int counter=0;
                for (ImportantNotification message:messages) {
                    bg=(counter%2==0)?"#F0F0F0":"#E0E0E8";
                    builder.append("<tr><td bgcolor=\""+bg+"\">");
                    builder.append(formatter.format(message.getDate()));
                    String colorString=VisualizationSettings.convertColorToHTMLrepresentation(message.getLevelAsColor());
                    builder.append("</td><td bgcolor=\""+bg+"\"><font color=\""+colorString+"\">");
                    builder.append(message.getLevelAsString());
                    builder.append("</font></td><td bgcolor=\""+bg+"\">");
                    String msg=message.getMessage();
                    msg=StringEscapeUtils.unescapeHtml(msg);
                    builder.append(msg);
                    builder.append("</td></tr>");
                    counter++;
                }
                builder.append("</table>");
                if (!getAll) builder.append("<br><font size=3>To see all notifications, select \"Notifications\" from the Help menu</font>");
                builder.append("</center></body></html>");
                InfoDialog dialog=new InfoDialog(MotifLabGUI.this, "Notification", builder.toString(),800,400);
                dialog.setVisible(true);
                dialog.dispose();
            }
        };
        worker.execute();
    }


    private void setupExampleSessionsMenu() {
        // delegate this to the background
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            ArrayList<String[]> examples=null;
            @Override
            public Boolean doInBackground() {
                try {
                    URL url=new URL(engine.getWebSiteURL()+"example_sessions.php?"+engine.getVersion());
	   	    ArrayList<String> lines=MotifLabEngine.getPageAsList(url);
                    for (String line:lines) {
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        String[] parts=(line.contains("|"))?line.split("\\|"):line.split("\t");  // split on vertical bar if present, else TAB
                        if (parts.length==3) {
                            if (examples==null) examples=new ArrayList<String[]>();
                            examples.add(parts);
                        }
                    }
                } catch (Exception e) {
                    logMessage(e.toString());
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                if (examples!=null) {
                    ActionListener menuItemListener=new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            boolean noData=engine.isDataStorageEmpty();
                            int option=(noData)?JOptionPane.OK_OPTION:JOptionPane.showConfirmDialog(getFrame(),"Would you like to restore this example session?\nAll current data will be discarded!","Import Example Session",JOptionPane.YES_NO_OPTION);
                            if (option==JOptionPane.OK_OPTION) {
                                try {
                                    URL sessionurl=new URL(e.getActionCommand());
                                    restoreSessionInBackground(sessionurl);
                                } catch (Exception ex) {
                                    logMessage("Unable to restore example session: "+ex.toString());
                                }
                            }
                        }
                    };
                    boolean haveExamples=false;
                    for (String[] example:examples) {
                        JMenuItem item=new JMenuItem(example[0]);
                        item.setToolTipText(example[2]);
                        item.setActionCommand(example[1]);
                        item.setText(example[0]);
                        item.addActionListener(menuItemListener);
                        exampleSessionsMenu.add(item);
                        haveExamples=true;
                    }
                    exampleSessionsMenu.setEnabled(haveExamples);
                    if (!haveExamples) exampleSessionsMenu.setToolTipText("Example sessions are currently unavailable");
                } else {
                    exampleSessionsMenu.setToolTipText("Example sessions are currently unavailable");
                }
            }
        }; // end of SwingWorker class
        worker.execute();
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



    public void setAntialiasMode(Object mode) {
        if (mode instanceof String) mode=getAntialiasMode((String)mode);
        MotifLabGUI.ANTIALIAS_MODE=mode;
        redraw();
    }

    public static Object getAntialiasMode(String mode) {
             if (mode==null || mode.equalsIgnoreCase("DEFAULT")) return RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        else if (mode.equalsIgnoreCase("ON")) return RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
        else if (mode.equalsIgnoreCase("GASP")) return RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
        else if (mode.equalsIgnoreCase("LCD_HRGB")) return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
        else if (mode.equalsIgnoreCase("LCD_HBGR")) return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR;
        else if (mode.equalsIgnoreCase("LCD_VRGB")) return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB;
        else if (mode.equalsIgnoreCase("LCD_VBGR")) return RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR;
        else if (mode.equalsIgnoreCase("OFF")) return RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        else return RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
    }

    public static String getAntialiasModeName(Object renderingHintsValue) {
             if (renderingHintsValue==null) return "Default";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_ON) return "On";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT) return "Default";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_OFF) return "Off";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB) return "LCD_HRGB";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR) return "LCD_HBGR";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB) return "LCD_VRGB";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR) return "LCD_VBGR";
        else if (renderingHintsValue==RenderingHints.VALUE_TEXT_ANTIALIAS_GASP) return "GASP";
        else return "Default";
    }

    public static String[] getAntialiasModes() {
        return new String[]{"On","Off","Default","LCD_HRGB","LCD_HBGR","LCD_VRGB","LCD_VBGR","GASP"};
    }

    /** Just an unused MotifLabClient interface method. This functionality is handled by SequenceInputDialog */
    @Override
    public ArrayList<GeneIDmapping> selectCorrectMappings(ArrayList<GeneIDmapping> list, String id) {
        return null;
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





}

