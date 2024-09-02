package motiflab.engine;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import motiflab.engine.datasource.DataRepository;
import motiflab.engine.datasource.DataLoader;
import motiflab.engine.task.ProtocolTask;
import motiflab.engine.task.ExecutableTask;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import motiflab.engine.data.analysis.Analysis;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import motiflab.engine.data.*;
import motiflab.engine.operations.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import motiflab.engine.data.analysis.CollatedAnalysis;
import motiflab.engine.dataformat.*;
import motiflab.engine.datasource.DataRepositoryFile;
import motiflab.engine.datasource.DataRepository_LocalDirectory;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.Protocol;
import motiflab.engine.protocol.StandardProtocol;
import motiflab.engine.util.ImportantNotificationParser;
import motiflab.engine.util.MotifComparator;
import motiflab.engine.util.NaturalOrderComparator;
import motiflab.external.ExternalProgram;
import motiflab.gui.PreferencesDialog;
import motiflab.gui.VisualizationSettings;
import org.apache.commons.io.IOUtils;



/**
 *
 * @author kjetikl
 */
public final class MotifLabEngine implements MessageListener, ExtendedDataListener, Thread.UncaughtExceptionHandler {
 
    public static final String PREFERENCES_USE_CACHE_FEATUREDATA="useFeatureDataCache";
    public static final String PREFERENCES_USE_CACHE_GENE_ID_MAPPING="useGeneIDMappingCache";    
    public static final String PREFERENCES_NETWORK_TIMEOUT="networkTimeout";    
    public static final String MAX_SEQUENCE_LENGTH="maxSequenceLength";   
    public static final String MAX_CONCURRENT_DOWNLOADS="maxConcurrentDownloads";         
    public static final String CONCURRENT_THREADS="concurrentThreadCount"; 
    public static final String PREFERENCES_AUTO_CORRECT_SEQUENCE_NAMES="autocorrectSequenceNames";  

    private static String version="2.0.0.-7"; // 
    private static Date releaseDate=getCorrectDate(2024, 7, 30); // Note: Official release date for v2.0.0 has not been determined yet...
    
    private DataStorage storage;
    private HashSet<MessageListener> messagelisteners=new HashSet<MessageListener>();
    private final HashSet<DataListener> datalisteners=new HashSet<DataListener>();
    private HashMap<String,Operation> operationslist=new HashMap<String,Operation>();
    private HashMap<String,DataFormat> dataformatslist=new HashMap<String,DataFormat>();
    private HashMap<Class,String> defaultdataformats=new HashMap<Class,String>();
    private HashMap<String,Object[]> predefinedMotifCollections=new HashMap<String,Object[]>();// (String)Object[0]=filename, (Integer)Object[1]=collection size
    private HashMap<String,DataRepository> datarepositories=null;
    private HashMap<String,MotifLabResource> motiflabresources=null;
    private HashMap<String,Class> datarepositoryTypes=null;
    private HashMap<String,HashMap<String,Object>> plugins=null; // plugin configurations. Each plugin is represented by a "key=value" hashmap, where the key "_plugin" refers to the Plugin object itself
    private String defaultSequenceCollectionName="AllSequences";
    private String defaultOutputStreamName="Result";
    private Object executionLock=new Object(); // this is used to synchronize execution threads with other threads
    private String[] operations=new String[]{"analyze","apply","collate","combine_numeric","combine_regions","convert","copy","count","crop_sequences","decrease","delete","difference","discriminate","distance","divide","drop_sequences","ensemblePrediction","execute","extend","extend_sequences","extract","filter","increase","interpolate","mask","merge","moduleDiscovery","moduleScanning","motifDiscovery","motifScanning","multiply","new","normalize","output","physical","plant","predict","prompt","prune","rank","replace","score","search","set","split_sequences","statistic","threshold","transform"};
    private MotifLabClient client=null;
    private DataLoader dataLoader;
    private GeneIDResolver geneIDResolver;
    private GOengine geneOntologyEngine;
    private HashMap<String,ExternalProgram> externalPrograms=new HashMap<String,ExternalProgram>();
    private HashMap<String,Analysis> analyses=new HashMap<String,Analysis>();
    private HashSet<String> reservedwords=new HashSet<String>(); // list of Protocol-specific words that can not be used as variable names
    private Preferences preferences;
    private int networkTimeoutDelay=25000; // milliseconds until the DataLoader should call a network timeout and proceed to the next mirror in the list (or throw Exception if there are no more mirrors)
    private String motiflabDirectoryPath="";
    private String MotifCollectionDirectory="MLab_MotifCollections";
    private int maxSequenceLength=0;
    private int concurrentThreadCount=4;
    private boolean dataUpdatesAllowed=true;
    private boolean autocorrectSequenceNames=false;
    private HashMap<String,OutputDataDependency> sharedOutputDependencies=null; // key is sharedID
    private ArrayList<MotifComparator> motifcomparators=null;
    private int uniqueCounter=0; // used to create work directories
    private ArrayList<PropertyChangeListener> clientListeners=null; // components can register to know if the client changes
    private WeakHashMap<URLClassLoader,Boolean> pluginClassLoaders=null; // this is used to retrieve classloaders    
    private TaskRunner taskRunner=null;
    
    private static Random randomNumberGenerator = new Random();
    private static NaturalOrderComparator naturalordercomparator=new NaturalOrderComparator();     
    private static MotifLabEngine engine=null;       
    
    public static MotifLabEngine getEngine() {     
        if (engine==null) engine=new MotifLabEngine();      
        return engine;
    }
    
    /** This constructor is private. Use the static method getEngine() to get a reference to a singleton engine */              
    private MotifLabEngine() {
        try {
            System.setSecurityManager(null); // disable any security managers to avoid problems with plugins when running via Web Start
        } catch (SecurityException e) {
            System.err.println("Warning: Unable to disable the installed security manager. Some functionality could be restricted (especially for plugins).");
        }
        storage=new DataStorage();
        SequenceCollection defaultSequenceCollection=new SequenceCollection(defaultSequenceCollectionName);
        try {storeDataItem(defaultSequenceCollection);} catch (Exception e) {}
        preferences = Preferences.userNodeForPackage(this.getClass());
        Thread.setDefaultUncaughtExceptionHandler(this);
        Locale.setDefault(Locale.ENGLISH);
    }
    
    /**
     * Initializes the engine and sets up necessary resources
     */
    public void initialize() {
        logMessage("Initializing MotifLab");
        importPlugins(new String[]{"type:DataSource","load:early"}, null);  //   
        initializeDataSourceTypes();
        setupDataLoader();
        geneIDResolver=new GeneIDResolver(this);        
        geneIDResolver.setUseCache(preferences.getBoolean(PREFERENCES_USE_CACHE_GENE_ID_MAPPING, true));
        setAutoCorrectSequenceNames(preferences.getBoolean(PREFERENCES_AUTO_CORRECT_SEQUENCE_NAMES, autocorrectSequenceNames));
        setNetworkTimeout(preferences.getInt(PREFERENCES_NETWORK_TIMEOUT, networkTimeoutDelay));
        setConcurrentThreads(preferences.getInt(CONCURRENT_THREADS, concurrentThreadCount));
        setMaxSequenceLength(preferences.getInt(MAX_SEQUENCE_LENGTH, 0)); // 0 means no maximum      
        setDataCacheDirectory(getMotifLabDirectory()+java.io.File.separator+"cache");
        setGeneIDCacheDirectory(getMotifLabDirectory());
        if (!setupTempDirectory()) System.exit(1); // if this fails there is nothing more we can do...
        if (!isFirstTimeStartup()) importResources(); // if this is the first time, we will install first and then import the resources later on

        // Register reserved words that can not be used as names for data objects (some of these words are reserved to avoid problems in the GUI and protocols)
        String[] reservedWords=new String[]{"Visualization","Protocol","within","Collection","Partition","Region","equals","matches","matching","is","in","NOT","OR","AND","TRUE","FALSE","True","False","YES","NO","Yes","No","_DEFAULT_"};
        for (String reserved:reservedWords) {
            registerReservedWord(reserved);
            registerReservedWord(reserved.toLowerCase());            
        }
        for (String datatype:Operation_new.getAvailableTypes()) {
            registerReservedWord(datatype);
            registerReservedWord(datatype.toLowerCase());
        }       
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
               shutdown();
            }
        });         
    }
    
    /**
     * Registers templates for all standard Data Source types (i.e. data source protocols) as MotifLab Resources
     */
    private void initializeDataSourceTypes() {
        registerResource(motiflab.engine.datasource.DataSource_http_GET.getTemplateInstance());
        registerResource(motiflab.engine.datasource.DataSource_DAS.getTemplateInstance());
        registerResource(motiflab.engine.datasource.DataSource_FileServer.getTemplateInstance());
        registerResource(motiflab.engine.datasource.DataSource_SQL.getTemplateInstance());
        registerResource(motiflab.engine.datasource.DataSource_VOID.getTemplateInstance());     
    }
    
    /**
     * Sets up the Data Loader an imports the current track configuration
     */
    private void setupDataLoader() {
        dataLoader=new DataLoader(this);  
        try {
            dataLoader.setup();
        }
        catch (Exception e) {
            errorMessage("DataTracks: "+e.getMessage(), 0);
        }     
        dataLoader.setUseCache(preferences.getBoolean(PREFERENCES_USE_CACHE_FEATUREDATA, true));
        try {
            dataLoader.setConcurrentDownloads(preferences.getInt(MAX_CONCURRENT_DOWNLOADS, 1));
        } catch (SystemError e) {
            errorMessage(e.getMessage(),0);
        }        
    }
    
    private void setupGOengine() {
        geneOntologyEngine=new GOengine(this);
    }
    
    private void importResources() {
        importOrganisms();
        importGeneIDResolverConfig();
        importMotifClassifications();           
        setupGOengine();         
        registerDataRepositoryType(new DataRepository_LocalDirectory());
        importOperations(); // import known operations
        importAnalyses(); // import Analyses programs
        importDataFormats(); // import known data formats     
        importPredefinedMotifCollections(); // import information about predefined motif collections   
        importExternalPrograms(); // import external programs         
        importDataRepositories();         
        importPlugins(null, new String[]{"type:DataSource", "load:early"});  // Data Sources and "early" plugins should already have been imported, so skip them here...         
    }

    private static Date getCorrectDate(int year, int month, int day) {
        return new Date(year-1900,month-1,day);
    }
    
    /** Returns all preference settings recognized by the engine (but not potential clients)     
     *  with their current values (which should not be null)
     */
    public HashMap<String,Object> getConfigurations() {
        HashMap<String,Object> settings=new HashMap<String, Object>();
        settings.put(PREFERENCES_USE_CACHE_FEATUREDATA, preferences.getBoolean(PREFERENCES_USE_CACHE_FEATUREDATA, true));
        settings.put(PREFERENCES_USE_CACHE_GENE_ID_MAPPING, preferences.getBoolean(PREFERENCES_USE_CACHE_GENE_ID_MAPPING, true));
        settings.put(PREFERENCES_NETWORK_TIMEOUT, preferences.getInt(PREFERENCES_NETWORK_TIMEOUT, networkTimeoutDelay));
        settings.put(MAX_SEQUENCE_LENGTH, preferences.getInt(MAX_SEQUENCE_LENGTH, maxSequenceLength));       
        settings.put(MAX_CONCURRENT_DOWNLOADS, preferences.getInt(MAX_CONCURRENT_DOWNLOADS, 1));       
        settings.put(CONCURRENT_THREADS, preferences.getInt(CONCURRENT_THREADS, 4));              
        return settings;      
    }
    
    /** Sets new values for configurations. Throws an exception if one of the specified settings
     *  is not known or the value given is illegal for that setting
     */
    public void setConfigurations(HashMap<String,Object> settings) throws ExecutionError {
        for (String key:settings.keySet()) {
            PreferencesDialog.setOption(key, settings.get(key), this);          
        }
        try {preferences.flush();}
        catch (Exception e) {throw new ExecutionError(e.getMessage());}
    }    
    
    /** Sets parameters for plugins. Throws an exception if one of the specified settings
     *  is not known or the value given is illegal for that setting or the plugin is not recognized
     */
    public void setPluginConfigurations(HashMap<String,String[]> settings) throws ExecutionError {
        for (String pluginName:settings.keySet()) {
            String[] pair=settings.get(pluginName);
            Plugin plugin = getPlugin(pluginName);
            if (plugin==null) throw new ExecutionError("Unrecognized plugin: "+pluginName);
            if (!(plugin instanceof ConfigurablePlugin)) throw new ExecutionError("The plugin  '"+pluginName+"' can not be configured");
            try {
                ((ConfigurablePlugin)plugin).setPluginParameterValue(pair[0], pair[1]);
            } catch (ExecutionError e) {
                throw new ExecutionError("Plugin configuration error ["+pluginName+"]: "+e.getMessage());
            }
        }
    }      
    
    /**
     * Returns the version number MotifLab as a string
     * @return
     */
    public static String getVersion() {
        return version;
    }
    
    /**
     * Returns the release date for this version of MotifLab
     * @return
     */
    public static Date getReleaseDate() {
        return releaseDate;
    }   
    
    /**
     * Returns the release date for this version of MotifLab as a string on the form YYYY-MM-DD
     * @return
     */
    public static String getReleaseDateAsString() {
        return new SimpleDateFormat("yyyy-MM-dd").format(releaseDate);
    }       
    
    /** Compares to "version strings". The strings can contain numbers separated by dots.  E.g. "2", "2.1" or "2.0.12". 
     *  Negative numbers are allowed and these will be considered to be smaller (i.e. "older") than positive numbers. E.g. "2.1" is a newer version than "2.-1".
     *  If one version has more dot-separated numbers than the other, the second is assumed to have the value 0 for these numbers. 
     *  @param version1 The first version string 
     *  @param version2 The second version string
     *  @return
     *  Returns 0 if the version strings are identical
     *  Returns 1 if the first version string refers to a "newer version" compared to the second version string
     *  Returns -1 if the second version string refers to a "newer version" compared to the first version string
     *  @throws NumberFormatException if the version number (this or provided) is not in a proper format
     *  i.e. [ddd(.ddd)*] where 'ddd' is an integer.
     */
    public static int compareVersions(String version1, String version2) throws NumberFormatException {
        if (version1==null || version2==null) throw new NumberFormatException("Version string is NULL");
        if (version1.equals(version2)) return 0;
        String[] thisVersionString=version1.split("\\.");
        String[] otherVersionString=version2.split("\\.");
        int size=Math.max(thisVersionString.length, otherVersionString.length);
        int[] thisVersion=new int[size];
        int[] otherVersion=new int[size];
        for (int i=0;i<thisVersionString.length;i++) {
            thisVersion[i]=Integer.parseInt(thisVersionString[i]);
        }
        for (int i=0;i<otherVersionString.length;i++) {
            otherVersion[i]=Integer.parseInt(otherVersionString[i]);
        }             
        for (int i=0;i<size;i++) {
            if (thisVersion[i]>otherVersion[i]) return 1;
            if (thisVersion[i]<otherVersion[i]) return -1;
        }
        return 0;
    }    
    
    /** Compares this version of MotifLab to the given versionString
     *  Returns 0 if the versions are identical
     *  Returns 1 if this MotifLab version is newer compared to the given version
     *  Returns -1 if the given version is newer compared to this version of MotifLab
     *  @throws NumberFormatException if the version number (this or provided) is not in a proper format
     *  i.e. [ddd(.ddd)*] where 'ddd' is an integer.
     */
    public static int compareVersions(String versionString) throws NumberFormatException {
        return compareVersions(version, versionString);
    }       
    
    

    /**
     * This method executes a protocol script and is called by the command-line client.
     * Note that the graphical user interface has its own protocol execution class called GuiScheduler
     * and does not use this method
     * @param protocol
     * @throws motiflab.engine.ExecutionError
     */
    public void executeProtocolTask(ProtocolTask task, boolean silent) throws ParseError, ExecutionError, InterruptedException {
        task.setMotifLabClient(client);          
        if (client instanceof java.beans.PropertyChangeListener) task.addPropertyChangeListener((java.beans.PropertyChangeListener)client);                
        try {      
            if (!silent) logMessage("Executing protocol");
            long timeBefore=System.currentTimeMillis();
            task.run(); 
            long executionTime=System.currentTimeMillis()-timeBefore;
            if (!silent) logMessage("Execution finished in "+formatTime(executionTime));
        } catch (ExecutionError e) { 
            task.setStatus(ExecutableTask.ERROR);  // this will notify listeners  
            throw e;
        } catch (NullPointerException e) { // this should not happen
            if (!silent) errorMessage("SYSTEM ERROR: Caught unexpected NullPointerException",0);                   
            task.setStatus(ExecutableTask.ABORTED); // this will notify listeners          
            //e.printStackTrace();
            throw new ExecutionError(e.getClass().getSimpleName()+":"+e.getMessage());
        } catch (InterruptedException e) { // Execution aborted interactively by the user
            //gui.debugMessage("ABORT!!!   ScheduledExecutor thread interrupted"); 
            if (!silent) logMessage("Protocol script execution aborted");
            if (!silent) task.setStatusMessage("Aborted!");
            task.setStatus(ExecutableTask.ABORTED); // this will notify listeners
            throw e; // propagete InterruptedException
        } catch (Exception e) { // Execution aborted because of unforseen errors 
            task.setStatus(ExecutableTask.ERROR);  // this will notify listeners
            throw new ExecutionError(e.getClass().getSimpleName()+":"+e.getMessage());
        } finally {           
            task.setMotifLabClient(null);
            if (client instanceof java.beans.PropertyChangeListener) task.removePropertyChangeListener((java.beans.PropertyChangeListener)client);
        }       
    }
    
    public void executeProtocol(Protocol protocol, boolean silent) throws ParseError, ExecutionError, InterruptedException {
        ProtocolTask task = protocol.parse();
        executeProtocolTask(task, silent);
        // removeTemporaryDataItems();  // removing temporary data here could cause problems since the engine sometimes executes auxiliary protocols in the middle of other protocols (e.g. for display settings)      
     }
    
    /**
     * This method executes an ExecutableTask and is called by the command-line client
     * Note that the graphical user interface has its own execution class called GuiScheduler
     * and does not use this method
     * @param task
     * @throws motiflab.engine.ExecutionError
     */
    public void executeTask(ExecutableTask task) throws ExecutionError {
        task.setMotifLabClient(client);
        if (client instanceof java.beans.PropertyChangeListener) task.addPropertyChangeListener((java.beans.PropertyChangeListener)client);        
        try {      
            task.run(); 
        } catch (ExecutionError e) { 
            task.setStatus(ExecutableTask.ERROR);  // this will notify listeners  
            throw e;
        } catch (NullPointerException e) { // this should not happen
            errorMessage("SYSTEM ERROR: Caught unexpected NullPointerException",0);                   
            task.setStatus(ExecutableTask.ABORTED); // this will notify listeners                    
        } catch (InterruptedException e) { // Execution aborted interactively by the user
            //gui.debugMessage("ABORT!!!   ScheduledExecutor thread interrupted"); 
            logMessage("Protocol script execution aborted");
            task.setStatusMessage("Aborted!");
            task.setStatus(ExecutableTask.ABORTED); // this will notify listeners              
        } catch (Exception e) { // Execution aborted because of unforseen errors 
            task.setStatus(ExecutableTask.ERROR);  // this will notify listeners
            throw new ExecutionError(e.getClass().getSimpleName()+":"+e.getMessage());         
        } finally {           
            task.setMotifLabClient(null);
            if (client instanceof java.beans.PropertyChangeListener) task.removePropertyChangeListener((java.beans.PropertyChangeListener)client);
        }   
    }
    
    
    
    /**
     * Sets a client to use for this session
     * It will also call "initializeClient" in the client
     * @param client
     */
    public void setClient(MotifLabClient client) {
        MotifLabClient oldclient=this.client;
        this.client=client;
        addMessageListener(client);
        client.initializeClient(this);
        if (clientListeners!=null) {
            PropertyChangeEvent event=new PropertyChangeEvent(this, "client", oldclient, client);
            for (PropertyChangeListener listener:clientListeners) {
                listener.propertyChange(event);
            }
        }
    }
    
    
    /**
     * Adds a new macro definition to the current set of macros
     */
    public void addMacro(String macroname, String macrodefinition) {
        if (client==null) return;
        client.getVisualizationSettings().addMacro(macroname, macrodefinition);
    }
    
    /**
     * Replaces the current set of macros with a new set
     */    
    public void setMacros(HashMap<String, String> macros) {
        if (client==null) return;
        client.getVisualizationSettings().setMacros(macros);
    }    
    
    public boolean isMacroDefined(String macroname) {
        if (client==null) return false;
        return client.getVisualizationSettings().isMacroDefined(macroname);
    }
    
    /**
     * Returns the set of currently defined macros as list of
     * [macroterm,macrodefinition] pairs
     * @return 
     */
    public ArrayList<String[]> getMacros() {
        if (client==null) return null;
        HashMap<String,String> macros=client.getVisualizationSettings().getMacros();        
        if (macros==null || macros.isEmpty()) return null;
        ArrayList<String[]> list=new ArrayList<String[]>(macros.size());
        Iterator<String> iter=macros.keySet().iterator();
        while (iter.hasNext()) {
            String macro=iter.next();
            list.add(new String[]{macro,macros.get(macro)});
        }
        return list;
    }
    
    /**
     * Returns a set of all defined macro terms
     * @return 
     */
    public Set<String> getMacroTerms() {
        if (client==null) return null;
        HashMap<String,String> macros=client.getVisualizationSettings().getMacros();           
        if (macros==null || macros.isEmpty()) return null;
        return macros.keySet();
    }    

    /**
     * Deletes all currently defined macros
     */
    public void clearMacros() {
        if (client==null) return;
        client.getVisualizationSettings().clearMacros();
    }     
    
    /**
     * Returns a reference to the current client
     * @param client
     */
    public MotifLabClient getClient() {
        return client;
    }
    
    public void addClientListener(PropertyChangeListener listener) {
        if (clientListeners==null) clientListeners=new ArrayList<PropertyChangeListener>();
        clientListeners.add(listener);
    }
    
    public void removeClientListener(PropertyChangeListener listener) {
        if (clientListeners==null) return;
        clientListeners.remove(listener);
    }
    
    /** Sets a maximum sequence length permitted by the engine
     *  A value of 0 should be interpreted as "no limit" 
     *  (as should negative values)
     */
    public void setMaxSequenceLength(int newmax) {
         maxSequenceLength=newmax;
    }

    /** Returns the maximum sequence length permitted by the engine
     *  If the maximum sequence length has been set to 0 or a negative
     *  value, this will be interpreted as "no limit" and this method
     *  will then return Integer.MAX_VALUE
     */
    public int getMaxSequenceLength() {        
        return (maxSequenceLength>0)?maxSequenceLength:Integer.MAX_VALUE;
    }
    
    /** Sets the number of concurrent threads the engine should use for parallel computations */
    public void setConcurrentThreads(int threads) {
         concurrentThreadCount=threads;
    }

    /** Returns the number of concurrent threads the engine should use for parallel computations */
    public int getConcurrentThreads() {
        return concurrentThreadCount;
    }    
    
    /** Returns a task runner/executor service that can be used to run tasks concurrently */
    public TaskRunner getTaskRunner() {
        if (taskRunner==null) taskRunner=new TaskRunner(this);
        return taskRunner;
    }
    
    /** 
     * Returns a string denoting the path of the program directory for this MotifLab installation
     * The program directory contains such information as GUI-settings, Datatrack configuration settings,
     * data caches, undo caches etc.
     */
    public String getMotifLabDirectory() {
        return motiflabDirectoryPath;
    }
    
  /**
    * Returns the path of the main plugins directory.
    * Individual plugins will be installed into subdirectories of this directory
    * @return 
    */
    public String getPluginsDirectory() {
        String dir=getMotifLabDirectory()+java.io.File.separator+"plugins";
        File dirFile=new File(dir);
        if (!dirFile.exists()) dirFile.mkdir();
        return dir;
    }   
    
  /**  Returns the directory where the given plugin is installed (or will be installed)
    *  Plugins are free to use their own directories to store additional files
    *  The plugin directory is part of the metadata annotation for the plugin
    */
    public String getPluginDirectory(Plugin plugin) {
        Object dir=getPluginProperty(plugin.getPluginName(),"pluginDirectory");
        if (dir!=null) return dir.toString();
        else { // meta-data may not have been registered yet. Try to infer directory from plugin name
            String dirName=plugin.getPluginName().replaceAll("\\W", "_");
            File pluginDir=new File(engine.getPluginsDirectory(), dirName);
            return pluginDir.getAbsolutePath();
        }
    }        
    
  /** 
    * Sets the path of the program directory to use for this MotifLab installation
    * The program directory contains such information as GUI-settings, Datatrack configuration settings,
    * data caches, undo caches etc.
    */
    public void setMotifLabDirectory(String motiflabDirectoryPath) {
        File file=new File(motiflabDirectoryPath);
        boolean success=true;
        if (!file.exists()) success=file.mkdirs();
        if (!success) errorMessage("Unable to create installation-directory: "+motiflabDirectoryPath,0);
        this.motiflabDirectoryPath=motiflabDirectoryPath;
    }
    
    /** Creates a temp directory for this MotifLab session */
    private boolean setupTempDirectory() {
       File dir=new File(getTempDirectory());
       if (!dir.mkdirs()) {logMessage("Unable to create temp directory on local disc => "+getTempDirectory()); return false;}
       dir.deleteOnExit(); // tells the VM to delete the temp directory automatically on exit
       return true;
    }
    
    /** 
     * Returns a string denoting the path of the temp directory for this MotifLab session.
     * The temp directory is located beneath the work directory for the MotifLab installation
     * but is different for each MotifLab session. Work directories are emptied and deleted 
     * when the process exits.
     */
    public String getTempDirectory() {
        return motiflabDirectoryPath+File.separator+"currentsession"+File.separator+ManagementFactory.getRuntimeMXBean().getName();
    }

    /** 
     * Returns a string denoting the path of a new work directory that can be used for 
     * temporary storage of files. This directory and its contents will be deleted
     * when MotifLab exits
     */
    public synchronized String getNewWorkDirectory() {
       String temp=getTempDirectory();
       uniqueCounter++; 
       String newdir=temp+File.separator+"tmp"+uniqueCounter;
       File dir=new File(newdir);
       if (!dir.mkdirs()) {logMessage("Unable to create temp directory on local disc => "+newdir); return null;}
       dir.deleteOnExit(); // tells the VM to delete the temp directory automatically on exit
       return newdir;        
    }
    
    
    /** 
     * Creates and returns a reference to a temporary file (with unique filename).
     * Temp files are automatically deleted when the MotifLab session is over
     * @return A File object which holds the pathname for the temp-file, or NULL if the file could not be created
     */
    public File createTempFile() {
        return createTempFile(".tmp", null);
    }    
    /** 
     * Creates and returns a reference to a temporary file (with unique filename).
     * Temp files are automatically deleted when the MotifLab session is over
     * @String workdir The path to a subdirectory under the session TempDir
     *                 This can be null
     * @return A File object which holds the pathname for the temp-file, or NULL if the file could not be created
     */
    public File createTempFile(String workdir) {
        return createTempFile(".tmp", workdir);
    }
    
    public File createTempFileWithSuffix(String suffix) {
        return createTempFile(suffix,null);
    }    
    /** 
     * Creates and returns a reference to a temporary file (with unique filename).
     * Temp files are automatically deleted when the MotifLab session is over
     * @return A File object which holds the pathname for the temp-file, or NULL if the file could not be created
     */
    public File createTempFile(String suffix, String workdir) {
        if (workdir==null) workdir=getTempDirectory();
        File temp=null;
        try {
            temp=File.createTempFile("MotifLab", suffix, new File(workdir));
            temp.deleteOnExit();
        } catch (Exception e) {errorMessage("Unable to create temp file", 0);}
        return temp;
    }
    
    /** 
     * Returns a reference to an OutputDataDependency object which contains the name
     * of a shared temporary file (with unique filename based on the identifier).
     * If the file with the given unique identifier already exists, a reference to an 
     * existing shared OutputDataDependency is returned, else a new file is created and returned and further
     * calls to this method with the same ID will always return that OutputDataDependency object
     * Temp files are automatically deleted when the MotifLab session is over
     * @return A OutputDataDependency object which holds the pathname for the temp-file, or NULL if the file could not be created
     */
    public OutputDataDependency createSharedOutputDependency(String identifier, String suffix) {
        if (sharedOutputDependencies==null) sharedOutputDependencies=new HashMap<String, OutputDataDependency>();
        if (sharedOutputDependencies.containsKey(identifier)) {
            return sharedOutputDependencies.get(identifier);
        } else {
            OutputDataDependency dependency=null;
            try {
                File temp=File.createTempFile("MotifLab", suffix, new File(getTempDirectory()));
                temp.deleteOnExit();
                String filename=temp.getAbsolutePath();
                dependency=new OutputDataDependency(filename,identifier); 
                sharedOutputDependencies.put(identifier,dependency);                  
            } catch (Exception e) {errorMessage("Unable to create temp file", 0);}
            return dependency;            
        }
    }  
    
    /** Tags the provided existing OutputDataDependency as a "shared" dependency with the given ID.
     *  @param dependency The OutputDataDependency that should be shared
     *  @param the identifier to use for the shared dependency
     *  @return If the dependency is already shared the method will return FALSE, else TRUE
     */
    public boolean shareOutputDependency(OutputDataDependency dependency, String sharedID) {
        if (dependency.isShared()) return false;
        if (sharedOutputDependencies==null) sharedOutputDependencies=new HashMap<String, OutputDataDependency>();
        dependency.setSharedID(sharedID);
        sharedOutputDependencies.put(sharedID,dependency);                               
        return true;
    }
    
    /** Returns the shared OutputDataDependency object corresponding to the given identifier 
     *  or NULL if no such OutputDataDependencye currently exists
     */
    public OutputDataDependency getSharedOutputDependency(String identifier) {
         if (sharedOutputDependencies==null) return null;
         else return sharedOutputDependencies.get(identifier);
    }
    
    public void resetOutputDataDependencyProcessedFlags() {
        if (sharedOutputDependencies!=null) {
            for (OutputDataDependency dependency:sharedOutputDependencies.values()) {
                dependency.setHasBeenProcessed(false);
            }
        }
    }
    
 
    
    /**
     * Returns a data item with the specified name that has been registered with the engine
     * Registered data items are usually datasets or scalar variables. Individual sequences 
     * within datasets should not be stored individually but rather as part of the datasets 
     * Thus to obtain a single sequence, use method to obtain the dataset, then use methods
     * in the dataset to obtain the sequence.
     * @param key
     */
    public Data getDataItem(String key) {
        return storage.getDataItem(key);
    }
    
    /**
     * Returns a data item with the specified name that has been registered with the engine
     * but only if the item has the specified class type. If no data item with that name exists
     * or if a data item exists but has a different type, the value NULL will be returned
     * @param key
     */
    public Data getDataItem(String key, Class classtype) {
        Data item=storage.getDataItem(key);
        if (item!=null && classtype!=null && classtype.isAssignableFrom(item.getClass())) return item;
        else return null;
     }    
    
    /**
     * Returns the class of the data item with the specified name 
     * if it has been registered with the engine, or null if no data item
     * by that name has been registered
     * @param key
     */
    public Class getClassForDataItem(String key) {
        Data item=storage.getDataItem(key);
        if (item!=null) return item.getClass();
        else return null;
    }    
    
    /**
     * Registers a new dataitem with the engine or replaces an old Data object of the same name
     * If a Data item of the same name already exists in storage, the previous Data object will be removed
     * from storage before the new Data item is added
     *
     * @param dataitem The new dataitem to be added 
     */
    @SuppressWarnings("unchecked")
    public void storeDataItem(Data dataitem) throws ExecutionError {
        if (!dataUpdatesAllowed) throw new ExecutionError("Data objects can not be created, updated or deleted at this time");
        if (dataitem.getName().equals(getDefaultSequenceCollectionName()) && getDefaultSequenceCollection()!=null) {
            if (dataitem instanceof SequenceCollection) ((SequenceCollection)dataitem).initializeFromPayload(this); // add additional sequences
            else throw new ExecutionError("The name '"+dataitem.getName()+"' is reserved");
            return;
        }
        String namecheck=checkNameValidity(dataitem.getName(),false);
        if (!(dataitem instanceof Sequence) && namecheck!=null) throw new ExecutionError("Invalid data name '"+dataitem.getName()+"': "+namecheck);
        if (dataitem instanceof Sequence && hasDataItemsOfType(FeatureDataset.class)) throw new ExecutionError("New sequences can not be added after feature data has been loaded");
        Data olditem=storage.getDataItem(dataitem.getName()); // is there a previous Data object with the same name?
        if (olditem!=null && dataitem.getClass()!=olditem.getClass()) {
           if (olditem.getClass()==Sequence.class) throw new ExecutionError("Cannot store new data object with name '"+dataitem.getName()+"' since a Sequence with that name already exists");
           else if (olditem.getClass()==Motif.class) throw new ExecutionError("Cannot store new data object with name '"+dataitem.getName()+"' since a Motif with that name already exists");
           else if (olditem.getClass()==Module.class) throw new ExecutionError("Cannot store new data object with name '"+dataitem.getName()+"' since a Module with that name already exists");
           else if (olditem.getClass()==OutputData.class) throw new ExecutionError("Cannot store new data object with name '"+dataitem.getName()+"' since an Output object with that name already exists");
        }
        if (olditem!=null) removeDataItem(olditem.getName());
        dataitem.addDataListener(this);
        if (dataitem instanceof DataCollection) {((DataCollection)dataitem).initializeFromPayload(this);} // add individual data items within the collection that has not been stored before but currently resides in the collections payload
        storage.storeDataItem(dataitem);
        HashSet<DataListener> listenerList;
        synchronized(datalisteners) {   
            listenerList=(HashSet<DataListener>)datalisteners.clone();
        }         
        if (dataitem instanceof Sequence) {
           SequenceCollection defaultSC=getDefaultSequenceCollection();
           for (DataListener listener:listenerList) listener.dataUpdate(defaultSC,null); // Notify all datalisteners that the DefaultSequenceCollection has been updated                                      
           defaultSC.addSequence((Sequence)dataitem); 
        }
        if (dataitem instanceof Motif) Motif.clearUserDefinedPropertyClassesLookupTable(); // just in case
        if (dataitem instanceof Module) Module.clearUserDefinedPropertyClassesLookupTable(); // just in case
        if (dataitem instanceof Sequence) Sequence.clearUserDefinedPropertyClassesLookupTable(); // just in case    
        if (dataitem instanceof FeatureDataset && ((FeatureDataset)dataitem).displayDirectives!=null) {
            executeDisplayDirectives(((FeatureDataset)dataitem).displayDirectives,dataitem.getName()); 
            ((FeatureDataset)dataitem).displayDirectives=null; // remove the directives since they are no longer needed.
        }
        // Note: the new Sequence must be added to defaultSequenceCollection before notifications 
        // (as done above) because some parts of the system use the DefaultSequenceCollection in response to the notification given below
        for (DataListener listener:listenerList) listener.dataAdded(dataitem); // Notify all datalisteners that a new dataitem has been added          
     }
    
   
    /**
     * Note: this method is a "hack" used to bypass regular security checks.
     * Sequences can normally not be added after FeatureData has been created, but this method will bypass
     * that limitation. This method should ONLY be used during an UNDO task to reintroduce sequences that
     * have previously been deleted (i.e. by "drop_sequences"), or if you really, really know what you are
     * doing and are also able to produce all the necessary feature data for the new sequences
     */
    @SuppressWarnings("unchecked")
    public void storeDataItem_useBackdoor(Sequence dataitem) throws ExecutionError {
        if (!dataUpdatesAllowed) throw new ExecutionError("Data objects can not be created, updated or deleted at this time");
        Data olditem=storage.getDataItem(dataitem.getName()); // is there a previous Data object with the same name?
        if (olditem!=null) throw new ExecutionError("System Error: storeDataItem backdoor: sequence already exists");
        dataitem.addDataListener(this);
        storage.storeDataItem(dataitem);
        HashSet<DataListener> listenerList;
        synchronized(datalisteners) {   
           listenerList=(HashSet<DataListener>)datalisteners.clone();
        }         
        SequenceCollection defaultSC=getDefaultSequenceCollection();
        for (DataListener listener:listenerList) listener.dataUpdate(defaultSC,null); // Notify all datalisteners that the DefaultSequenceCollection has been updated                                      
        defaultSC.addSequence((Sequence)dataitem);      
        Sequence.clearUserDefinedPropertyClassesLookupTable(); // just in case                
        // Note: the new Sequence must be added to defaultSequenceCollection before notifications 
        // (as done above) because some parts of the system use the DefaultSequenceCollection in response to the notification given below
        for (DataListener listener:listenerList) listener.dataAdded(dataitem); // Notify all datalisteners that a new dataitem has been added      
     }    


    /**
     * Updates a data item already registered with the engine by providing an argument Dataitem whose
     * values will be imported into the existing Data object.
     * If the two objects are of incompatible types a ClassCastException will be thrown 
     * If two items are compatible registered DataListeners will be notified through the callback 
     * interface dataUpdate(old, new) before the actual data import occurs. If no previous Data 
     * items with the same name exists, the method will add the Data item as a new object by passing 
     * on the same argument to storeDataItem(dataitem)
     * exec
     * NOTE: The procedure might fail if the dataitem provided is already stored in the engine
     * (i.e. if one attempts to update the dataitem itself using this method).
     * This applies for instance to Collection objects where the target (stored in the engine) 
     * is cleared of all entries before copying the entries from object provided as argument.
     * Thus, if the target and source are the same object (not just having the same names), the data
     * in the target (and hence the source) will be cleared and there will be nothing left to
     * import into the target.
     *
     * @param dataitem The new dataitem to be added (or an object holding values to be imported in an old data item)
     */
    @SuppressWarnings("unchecked")
    public void updateDataItem(Data dataitem) throws ExecutionError, ClassCastException {
        if (!dataUpdatesAllowed) throw new ExecutionError("Data objects can not be created, updated or deleted at this time");
        Data olditem=storage.getDataItem(dataitem.getName()); // is there a previous Data object with the same name?
        if (olditem == null || olditem==getDefaultSequenceCollection()) {
            storeDataItem(dataitem);
        } else { 
            if (dataitem.getClass()!=olditem.getClass()) { // incompatible types?
                if (olditem.getClass()==Sequence.class || dataitem.getClass()==Sequence.class) throw new ClassCastException("Illegal type change from/to Sequence");
                else if (olditem.getClass()==Motif.class || dataitem.getClass()==Motif.class) throw new ClassCastException("Illegal type change from/to Motif");
                else if (olditem.getClass()==Module.class || dataitem.getClass()==Module.class) throw new ClassCastException("Illegal type change from/to Module");
                else if (olditem.getClass()==OutputData.class || dataitem.getClass()==OutputData.class) throw new ClassCastException("Illegal type change from/to Output object");
                HashSet<DataListener> listenerList;
                synchronized(datalisteners) {   
                   listenerList=(HashSet<DataListener>)datalisteners.clone();
                }                   
                for (DataListener listener:listenerList) listener.dataUpdate(olditem,dataitem); // Notify all datalisteners that an update is about to occur                 
                storeDataItem(dataitem); // different type but conversion is allowed. Just store the new data item
            } else { //
                HashSet<DataListener> listenerList;
                synchronized(datalisteners) {   
                   listenerList=(HashSet<DataListener>)datalisteners.clone();
                }                
                for (DataListener listener:listenerList) listener.dataUpdate(olditem,dataitem); // Notify all datalisteners that an update is about to occur                 
                if (dataitem instanceof DataCollection) {((DataCollection)dataitem).initializeFromPayload(this);}
                olditem.importData(dataitem);
                if (dataitem instanceof Motif) Motif.clearUserDefinedPropertyClassesLookupTable(); // just in case
                if (dataitem instanceof Module) Module.clearUserDefinedPropertyClassesLookupTable(); // just in case                
                if (dataitem instanceof Sequence) Sequence.clearUserDefinedPropertyClassesLookupTable(); // just in case
                olditem.notifyListenersOfDataUpdate();
            }
        }
    }

    
    
    /**
     * Removes a specified dataitem from storage and notifies any listeners.
     * @param key The name of the dataitem
     * @return the dataitem that was removed or null if it was not found
     */
    @SuppressWarnings("unchecked")
    public Data removeDataItem(String key) throws ExecutionError {
        if (!dataUpdatesAllowed) throw new ExecutionError("Data objects can not be created, updated or deleted at this time");        
        Data dataitem=storage.removeDataItem(key);
        if (dataitem==null) return null;
        dataitem.removeDataListener(this);
        // notify all datalisteners      
        if (dataitem instanceof Sequence) getDefaultSequenceCollection().removeSequence((Sequence)dataitem);
        // this should be removed from all collections!!!
        HashSet<DataListener> listenerList;
        synchronized(datalisteners) {   
           listenerList=(HashSet<DataListener>)datalisteners.clone();
        }  
        for (DataListener listener:listenerList) listener.dataRemoved(dataitem);       
        return dataitem;
    }
    
    /**
     * Renames the specified data item. The rename() method in individual Data items
     * should not be called directly, rather Data items should be renamed with this function
     * so that the hash-keys in the DataStorage are updated accordingly.
     * Listeners are not notified of the name change by the Engine but perhaps from some other source 
     * @param oldname 
     * @param newname 
     * @return the dataitem that was renamed or null if it was not found
     */
    public Data renameDataItem(String oldname, String newname) throws ExecutionError {
        if (!dataUpdatesAllowed) throw new ExecutionError("Data objects can not be created, updated or deleted at this time");                
        if (oldname.equals(defaultSequenceCollectionName)) {defaultSequenceCollectionName=newname;}
        Data dataitem=storage.renameDataItem(oldname,newname);
        if (dataitem==null) return null;
        dataitem.rename(newname);
        return dataitem;
    }
        
    /** Returns TRUE if the engine allows data to be added, updated, deleted or renamed at this time */
    public boolean dataUpdatesIsAllowed() {
        return dataUpdatesAllowed;
    }
    
    /**  This method can be used to set whether or not the engine will allow data to be added, updated, deleted or renamed
     *   Attempts to add,update,delete or rename data at times when this is now allowed will results in ExecutionErrors being thrown
     */
    public void setAllowDataUpdates(boolean allow) {
         dataUpdatesAllowed=allow;
    }    
    
    /** 
     * Returns true if the specified dataitem is designated as a temporary data item
     * Currently temporary data items are those whose names begin with an underscore character
     */
    public static boolean isTemporary(Data item) {
        if (item!=null && item.getName().startsWith("_")) return true;
        else return false;
    }
    
    /** 
     * Returns true if there exists a data item with the specified name and of the specified type.
     * If the type parameter is null, the function will return true if a data item with the given
     * name exists irrespective of the data item's class type.
     */
    public boolean dataExists(String name, Class type) {
        Data item=storage.getDataItem(name);
        if (type==null) return (item!=null);
        if (item==null || !(type.isInstance(item))) return false;
        return true;
    }
    
    
    
    /**
     * Removes all temporary data items currently registered with the engine
     */
    public void removeTemporaryDataItems() {
        ArrayList<Data> list=storage.getAllDataItemsOfType(Data.class);
        ArrayList<String> names=new ArrayList<String>();
        for (Data item:list) {
            if (isTemporary(item)) names.add(item.getName());
        }
        for (String name:names) {
            try {removeDataItem(name);} catch (ExecutionError e) {} // we don't have to report this?
        }               
    }
    
    
    /**
     * Removes every data item from storage (except the default Sequence Collection which will be empty anyway)
     */
    public void clearAllData() {
        if (!dataUpdatesIsAllowed()) return; // data is not allowed to be touched at this time
        ArrayList<String> names=new ArrayList<String>();
        ArrayList<Data> list=storage.getAllDataItemsOfType(Data.class);
        for (Data item:list) { // First add all non-Sequence data items
            if (!(item instanceof Sequence || item instanceof Motif || item instanceof Module) && item!=getDefaultSequenceCollection()) names.add(item.getName());
        }
        list=storage.getAllDataItemsOfType(Module.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=storage.getAllDataItemsOfType(Motif.class);
        for (Data item:list) { 
            names.add(item.getName());
        }          
        list=storage.getAllDataItemsOfType(Sequence.class);
        for (Data item:list) { // Sequences  must be deleted last because other data objects might contain references to them
            names.add(item.getName());
        }      
        for (String name:names) {
            try {removeDataItem(name);} catch (ExecutionError e) {}
        }     
        Motif.clearUserDefinedPropertyClassesLookupTable();
        Module.clearUserDefinedPropertyClassesLookupTable();        
        Sequence.clearUserDefinedPropertyClassesLookupTable();
    }
    
   /**
     * Removes every data item from storage of type Motif, MotifCollection, MotifPartition and MotifNumericMap
    *  as well as Module, ModuleCollection, ModulePartition and ModuleNumericMap
     */
    public void clearMotifAndModuleDataData(boolean justModules) {
        if (!dataUpdatesIsAllowed()) return; // data is not allowed to be touched at this time        
        ArrayList<String> names=new ArrayList<String>();       
        ArrayList<Data> list=null;
        list=storage.getAllDataItemsOfType(ModuleTextMap.class);
        for (Data item:list) {
            names.add(item.getName());
        }        
        list=storage.getAllDataItemsOfType(ModuleNumericMap.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=storage.getAllDataItemsOfType(ModuleCollection.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=storage.getAllDataItemsOfType(ModulePartition.class);
        for (Data item:list) {
            names.add(item.getName());
        }
        list=storage.getAllDataItemsOfType(Module.class);
        for (Data item:list) {
            names.add(item.getName()); 
        }
        if (!justModules) {
            list=storage.getAllDataItemsOfType(MotifNumericMap.class);
            for (Data item:list) {
                names.add(item.getName());
            }   
            list=storage.getAllDataItemsOfType(MotifTextMap.class);
            for (Data item:list) {
                names.add(item.getName());
            }             
            list=storage.getAllDataItemsOfType(MotifCollection.class);
            for (Data item:list) {
                names.add(item.getName()); 
            }
            list=storage.getAllDataItemsOfType(MotifPartition.class);
            for (Data item:list) {
                names.add(item.getName());
            }
            list=storage.getAllDataItemsOfType(Motif.class);
            for (Data item:list) { 
                names.add(item.getName());
            } 
            Motif.clearUserDefinedPropertyClassesLookupTable();
        }
        Module.clearUserDefinedPropertyClassesLookupTable();        
        for (String name:names) {
            try {removeDataItem(name);} catch (ExecutionError e) {}
        }          
    }
    
    
    
    
    
    
    /**
     * Removes all data items related to or referring to sequences
     * Including Sequences, FeatureData, Sequence Collections, Sequence Partitions and Expression Profiles from storage (except the default Sequence Collection)
     */
    public void clearAllSequences() {
        if (!dataUpdatesIsAllowed()) return; // data is not allowed to be touched at this time        
        ArrayList<String> names=new ArrayList<String>();
        ArrayList<Data> list=storage.getAllDataItemsOfType(FeatureDataset.class);
        for (Data item:list) { // First add all non-Sequence data items
            names.add(item.getName()); 
        }   
        list=storage.getAllDataItemsOfType(SequenceCollection.class);
        for (Data item:list) { // Next add sequence collections (note that the default sequence collection should never be 'removed')
            if (item!=getDefaultSequenceCollection()) names.add(item.getName()); 
        }
        list=storage.getAllDataItemsOfType(SequencePartition.class);
        for (Data item:list) { 
            names.add(item.getName()); 
        }
        list=storage.getAllDataItemsOfType(ExpressionProfile.class);
        for (Data item:list) { 
            names.add(item.getName());
        }
        list=storage.getAllDataItemsOfType(SequenceNumericMap.class);
        for (Data item:list) { 
            names.add(item.getName());
        }
        list=storage.getAllDataItemsOfType(SequenceTextMap.class);
        for (Data item:list) { 
            names.add(item.getName());
        }        
        list=storage.getAllDataItemsOfType(Sequence.class);
        for (Data item:list) { 
            names.add(item.getName());
        }      
        for (String name:names) {
            try {removeDataItem(name);} catch (ExecutionError e) {}
        }   
        Sequence.clearUserDefinedPropertyClassesLookupTable();        
    }
    
    
    /**
     * Executes the provided display directives (which could contain the placeholder token '?')
     * @param displayDirectives
     * @param targetName The name referred to by the ?-token
     */
    public void executeDisplayDirectives(String displayDirectives, String targetName) {      
        if (displayDirectives!=null && !displayDirectives.isEmpty()) {
             displayDirectives=displayDirectives.replace("?", targetName); // '?' is a placeholder token referring to the new track     
             try {
                  ArrayList<String> lines=MotifLabEngine.splitOnCharacter(displayDirectives,';');
                 StandardProtocol displayProtocol=new StandardProtocol(engine,lines);
                 engine.executeProtocol(displayProtocol, true);
             } 
             catch (Exception ex) {}  // silently execute the display directives protocol (ignoring all errors)
        }          
    }     
    
    
    /**
     * Returns a new name that can be used for data objects. 
     * The name consists of the given prefix followed by an assigned number which
     * together makes the name unique. For instance, if the engine has registered 
     * a set of motifs with names MM0001,MM0002,MM0003,MM0004,MM0005 a call to
     * getNextAvailableDataName("MM",4) will return "MM0006". The name returned 
     * is the lowest available name, so if there exists motifs MM0001,MM0002,MM0004
     * and MM0005 (but not MM0003) the method will return MM0003 as the suggested name.
     * @param prefix 
     * @param digits the number of digits to use to format the number after the prefix 
     *        (If the assigned number is shorter than the number of digits it will be preceeded by 0's)
     *        If this argument is 0 then the number of digits will vary depending on the assigned number
     * @return
     */
    public String getNextAvailableDataName(String prefix, int digits) {
        int counter=1;      
        boolean done=false;
        while (!done) {
            String name=""+counter;
            while (name.length()<digits) name="0"+name;
            name=prefix+name;
            if (!dataExists(name, null)) return name;
            else counter++;
        }
        return "x";
    }

    /** 
     * This method works the same way as getNextAvailableDataName() but returns 
     * a list of available names
     */
    public String[] getNextAvailableDataNames(String prefix, int digits, int numberofnames) {
        int counter=0;      
        String[] list=new String[numberofnames];
        for (int i=0;i<numberofnames;i++) {
           while(1<2) { // the loop will exit with a break
              String name=""+(++counter);
              while (name.length()<digits) name="0"+name;
              name=prefix+name;
              if (!dataExists(name, null)) {list[i]=name;break;}
           }
        }
        return list;
    }
    
    /**
     * Returns an ArrayList containing all Data of the specified class type
     * currently available in storage
     *
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList of Data objects
     */
    public ArrayList<Data> getAllDataItemsOfType(Class classtype) {
        return storage.getAllDataItemsOfType(classtype);
    }

    /**
     * Returns an ArrayList containing all Data of the specified class type
     * currently available in storage whose names match the given regular expression string
     *
     * @param expression A regular expression string that data items should match
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList of Data objects
     */
    public ArrayList<Data> getAllDataItemsOfTypeMatchingExpression(String expression, Class classtype) {
        return storage.getAllDataItemsOfTypeMatchingExpression(expression, classtype);
    }
    
    /**
     * Returns an ArrayList containing all Data of the specified classtype
     * currently available in storage whose names contain numbers and matches 
     * a specific name expression string consisting of an optional prefix followed
     * by the number and an optional suffix. However, only data items whose number
     * component falls within a given range will be returned.
     * For example if the prefix is "sequence" the suffix is "" and the start and end
     * parameters are 100 and 130 respectively, all data items with names starting with 
     * the prefix "sequence" and followed by a number between 100 and 130 will be returned
     * The prefix and suffix strings could be regular expressions but should not contain capturing groups.
     * 
     * @param prefix A prefix (optionally empty) that the names of target data items should contain 
     * @param suffix A suffix (optionally empty) that the names of target data items should contain 
     * @param start The min value in the numeric range of data items to return
     * @param end The max value in the numeric range of data items to return
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList of Data objects
     */    
    public ArrayList<Data> getAllDataItemsOfTypeMatchingExpressionInNumericRange(String prefix, String suffix, int start, int end, Class classtype) {
        return storage.getAllDataItemsOfTypeMatchingExpressionInNumericRange(prefix, suffix, start, end, classtype);
    }    

    
    /**
     * Returns an ArrayList containing the names of all Data objects of the
     * specified class type currently available in storage
     *
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList containing names of Data object
     */
    public ArrayList<String> getNamesForAllDataItemsOfType(Class classtype) {
        return storage.getNamesForAllDataItemsOfType(classtype);
    }
    
    /**
     * Returns an ArrayList containing the names of all Data objects of the
     * specified class types currently available in storage
     *
     * @param classtypees the classes of data to return (subclass of Data)
     * @return An ArrayList containing names of Data object
     */
    public ArrayList<String> getNamesForAllDataItemsOfTypes(Class[] classtypes) {
        return storage.getNamesForAllDataItemsOfTypes(classtypes);
    }

    /** Takes a list of Strings as input and removes all the strings that do not start with the given prefix*/
    public ArrayList<String> filterNamesWithoutPrefix(ArrayList<String> list, String prefix) {
        Iterator<String> iterator=list.iterator();
        while (iterator.hasNext()) {
            String item=iterator.next();
            if (!item.startsWith(prefix)) iterator.remove();
        }
        return list;
    }
    /** Takes a list of Strings as input and removes all the strings that do not end with the given prefix*/
    public ArrayList<String> filterNamesWithoutSuffix(ArrayList<String> list, String suffix) {
        Iterator<String> iterator=list.iterator();
        while (iterator.hasNext()) {
            String item=iterator.next();
            if (!item.endsWith(suffix)) iterator.remove();
        }
        return list;
    }
    
    /**
     * Returns TRUE if there is currently registered any Data items of the specified classtype
     * 
     * @param classtype the class of data (subclass of Data)
     * @return 
     */
    public boolean hasDataItemsOfType(Class classtype) {
        return storage.hasDataItemsOfType(classtype);
    }
    
    /**
     * Returns the number of currently registered Data items of the specified classtype
     * 
     * @param classtype the class of data (subclass of Data)
     * @return 
     */
    public int countDataItemsOfType(Class classtype) {
        return storage.countDataItemsOfType(classtype);
    }

    /**
     * Returns the number of Sequences currently registered
     * It is a shorthand for countDataItemsOfType(Sequence.class)
     */
    public int getNumberOfSequences() {
        return ((SequenceCollection)getDataItem(defaultSequenceCollectionName)).size();
    }

    /**
     * Returns the default SequenceCollection which contains all sequences registered
     */
    public SequenceCollection getDefaultSequenceCollection() {
        return (SequenceCollection)getDataItem(defaultSequenceCollectionName);
    }
    
    /**
     * Returns the name of the default SequenceCollection which contains all sequences registered
     */
    public String getDefaultSequenceCollectionName() {
        return defaultSequenceCollectionName;
    }
    
    /**
     * Returns the default DataOutput object to use for output if no other destination is specified
     */
    public OutputData getDefaultOutputObject() {
        OutputData defaultout=(OutputData)getDataItem(defaultOutputStreamName);
        if (defaultout==null) defaultout=new OutputData(defaultOutputStreamName);           
        return defaultout;
    }
    
    /**
     * Returns the name of the default DataOutput object to use for output if no other destination is specified
     */
    public String getDefaultOutputObjectName() {
        return defaultOutputStreamName;
    }
    
    
    
    
    /**
     * Returns a list of all operations known to the engine.
     * These operations are implemented and available to be used in protocol scripts.
     * 
     * @return An ArrayList of Operation objects
     */
    public ArrayList<Operation> getAllOperations() {
        ArrayList<Operation> list=new ArrayList<Operation>();
        Set<String> keys = operationslist.keySet();
        for (String key:keys) {
            list.add(operationslist.get(key));
        }
        return list;
    }
    
    /**
     * Returns an instance of the operation with the specified name
     * 
     * @param name The name (command) of the operation
     * @return an Operation instance 
     */
    public Operation getOperation(String name) {
        return operationslist.get(name);
    }
  
    
    /**
     * Returns a list of all data formats known to the engine.
     * 
     * @return An ArrayList of Data Format objects
     */
    public ArrayList<DataFormat> getAllDataFormats() {
        ArrayList<DataFormat> list=new ArrayList<DataFormat>();
        Set<String> keys = dataformatslist.keySet();
        for (String key:keys) {
            list.add(dataformatslist.get(key));
        }
        return list;
    }
    
    /**
     * Returns a list of all data format object that can format output for the given data object
     * @param data A data item used as template
     * @return An ArrayList of Data Format objects
     */
    public ArrayList<DataFormat> getDataOutputFormats(Data data) {
        ArrayList<DataFormat> list=new ArrayList<DataFormat>();
        Set<String> keys = dataformatslist.keySet();
        for (String key:keys) {
            DataFormat formatter=dataformatslist.get(key);
            if (formatter.canFormatOutput(data)) list.add(formatter);
        }
        return list;
    }
    
    /**
     * Returns a list of all data format object that can format output for data objects of the given class
     * @param data A data item used as template
     * @return An ArrayList of Data Format objects
     */
    public ArrayList<DataFormat> getDataOutputFormats(Class dataclass) {
        ArrayList<DataFormat> list=new ArrayList<DataFormat>();
        Set<String> keys = dataformatslist.keySet();
        for (String key:keys) {
            DataFormat formatter=dataformatslist.get(key);
            if (formatter.canFormatOutput(dataclass)) list.add(formatter);
        }
        return list;
    }
    
     /**
     * Returns a list of all data format object that can parse input for the given data object
     * @param data A data item used as template
     * @return An ArrayList of Data Format objects
     */
    public ArrayList<DataFormat> getDataInputFormats(Data data) {
        ArrayList<DataFormat> list=new ArrayList<DataFormat>();
        Set<String> keys = dataformatslist.keySet();
        for (String key:keys) {
            DataFormat formatter=dataformatslist.get(key);
            if (formatter.canParseInput(data)) list.add(formatter);
        }
        return list;
    }   
    
    /**
     * Returns a list of all data format object that can parse input for data objects of the given class
     * @param data A data item used as template
     * @return An ArrayList of Data Format objects
     */
    public ArrayList<DataFormat> getDataInputFormats(Class dataclass) {
        ArrayList<DataFormat> list=new ArrayList<DataFormat>();
        Set<String> keys = dataformatslist.keySet();
        for (String key:keys) {
            DataFormat formatter=dataformatslist.get(key);
            if (formatter.canParseInput(dataclass)) list.add(formatter);
        }
        return list;
    }
    
    /**
     * Returns a list of all data format objects that can parse input for some kind of Feature Data
     * @return An ArrayList of Data Format objects
     */
    public ArrayList<DataFormat> getFeatureDataInputFormats() {
        ArrayList<DataFormat> list=new ArrayList<DataFormat>();
        Set<String> keys = dataformatslist.keySet();
        for (String key:keys) {
            DataFormat formatter=dataformatslist.get(key);
            if (formatter.canParseInput(DNASequenceData.class) || formatter.canParseInput(NumericSequenceData.class) || formatter.canParseInput(RegionSequenceData.class)) list.add(formatter);
        }
        return list;
    }
    
    
    /**
     * Returns an instance of the data format with the specified name
     * 
     * @param name The name (command) of the data format
     * @return an DataFormat instance 
     */
    public DataFormat getDataFormat(String name) {
        return dataformatslist.get(name);
    }
      
    /**
     * Sets the name of the default data format for Data items of the given class
     * 
     */
    public void setDefaultDataFormat(Class dataclass, String dataformatname) {
         defaultdataformats.put(dataclass, dataformatname);
    }
    
    
    /**
     * Returns an instance of the default data format for Data items of the given type
     * 
     * @param data The data 
     * @return a DataFormat instance which is registered as default for this data type 
     */
    public DataFormat getDefaultDataFormat(Data data) {
        String name=defaultdataformats.get(data.getClass());
        if (name==null) name="Plain"; // this can always be used for every data type 
        return getDataFormat(name);
    }
    
    /**
     * Returns an instance of the default data format for Data items of the given type
     * 
     * @param data The data 
     * @return anDataFormat instance which is registered as default for this data type 
     */
    public DataFormat getDefaultDataFormat(Class dataclass) {
        String name=(Analysis.class.isAssignableFrom(dataclass))?defaultdataformats.get(Analysis.class):defaultdataformats.get(dataclass);
        if (name==null) name="Plain"; // this can always be used for every data type 
        return getDataFormat(name);
    }
      
    /**
     * Returns the DataLoader used by the system. 
     * @return
     */
    public DataLoader getDataLoader() {
        return dataLoader;
    }
    /**
     * Returns the Gene ID Resolver used by the system. 
     * @return
     */
    public GeneIDResolver getGeneIDResolver() {
        return geneIDResolver;
    }
     /**
     * Returns the Gene Ontology Engine used by the system. 
     * @return
     */   
    public GOengine getGeneOntologyEngine() {
        return geneOntologyEngine;
    }    
    
    /** Returns the number of milliseconds to wait for network connection before calling timeout */
    public int getNetworkTimeout() {
        return networkTimeoutDelay;
    }
    
    /** Specifies the number of milliseconds to wait for network connection before calling timeout */
    public void setNetworkTimeout(int milliseconds) {
        networkTimeoutDelay=milliseconds;
    }
    
    /** Returns the "auto correct sequence names" setting. 
     *  If this is on, the system should try to automatically convert encountered sequence names
     *  with 'illegal characters' such as e.g. - or + to legal names 
     */
    public boolean autoCorrectSequenceNames() {
        return autocorrectSequenceNames;
    }
    
    /** Sets the "auto correct sequence names" setting. 
     *  If this is on, the system should try to automatically convert encountered sequence names
     *  with 'illegal characters' such as e.g. - or + to legal names 
     */
    public void setAutoCorrectSequenceNames(boolean correct) {
        autocorrectSequenceNames=correct;
    }    
        
    /** Specifies a directory location for the data cache */
    public void setDataCacheDirectory(String cacheDirectory) {
        if (dataLoader!=null) dataLoader.setCacheDirectory(cacheDirectory);
    }
    
    /** Specifies a cache file for the gene id mapping cache */
    public void setGeneIDCacheDirectory(String cacheDirectory) {
        if (geneIDResolver!=null) geneIDResolver.setCacheDirectory(cacheDirectory);
    }
    
    /** Returns a list of names for available analyses (sorted alphabetically) */
    public String[] getAnalysisNames() {
        ArrayList<String> list=new ArrayList<String>();
        for (Analysis analysis:analyses.values()) {
            if (analysis instanceof CollatedAnalysis) continue; // we don't want to add this special type of analysis
            list.add(analysis.getAnalysisName());
        }
        String[] result=new String[list.size()];
        result=list.toArray(result);
        Arrays.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }
    
    /** Returns a list of names for available motif discovery algorithms */
    public String[] getAvailableMotifDiscoveryAlgorithms() {
        ArrayList<String> list=new ArrayList<String>();
        for (ExternalProgram program:externalPrograms.values()) {
            if (program.getProgramClass().equalsIgnoreCase("MotifDiscovery")) list.add(program.getName());
        }
        String[] result=new String[list.size()];
        result=list.toArray(result);
        Arrays.sort(result);
        return result;
    }
    
    /** Returns a list of names for available motif scanning algorithms */
    public String[] getAvailableMotifScanningAlgorithms() {
        ArrayList<String> list=new ArrayList<String>();
        for (ExternalProgram program:externalPrograms.values()) {
            if (program.getProgramClass().equalsIgnoreCase("MotifScanning")) list.add(program.getName());
        }
        String[] result=new String[list.size()];
        result=list.toArray(result);
        Arrays.sort(result);
        return result;
    }

    /** Returns a list of names for available module discovery algorithms */
    public String[] getAvailableModuleDiscoveryAlgorithms() {
        ArrayList<String> list=new ArrayList<String>();
        for (ExternalProgram program:externalPrograms.values()) {
            if (program.getProgramClass().equalsIgnoreCase("ModuleDiscovery")) list.add(program.getName());
        }
        String[] result=new String[list.size()];
        result=list.toArray(result);
        Arrays.sort(result);
        return result;
    }

    /** Returns a list of names for available module scanning algorithms */
    public String[] getAvailableModuleScanningAlgorithms() {
        ArrayList<String> list=new ArrayList<String>();
        for (ExternalProgram program:externalPrograms.values()) {
            if (program.getProgramClass().equalsIgnoreCase("ModuleScanning")) list.add(program.getName());
        }
        String[] result=new String[list.size()];
        result=list.toArray(result);
        Arrays.sort(result);
        return result;
    }

    /** Returns a list of names for available ensemble prediction algorithms */
    public String[] getAvailableEnsemblePredictionAlgorithms() {
        ArrayList<String> list=new ArrayList<String>();
        for (ExternalProgram program:externalPrograms.values()) {
            if (program.getProgramClass().equalsIgnoreCase("EnsemblePrediction")) list.add(program.getName());
        }
        String[] result=new String[list.size()];
        result=list.toArray(result);
        Arrays.sort(result);
        return result;
    }


    /** Returns a list of names for available external programs that are NOT motif scanning or discovery programs */
    public String[] getOtherExternalPrograms() {
        ArrayList<String> list=new ArrayList<String>();
        for (ExternalProgram program:externalPrograms.values()) {
            String programClass=program.getProgramClass();
            if (!programClass.equalsIgnoreCase("MotifScanning") && !programClass.equalsIgnoreCase("MotifDiscovery") && !programClass.equalsIgnoreCase("ModuleScanning") && !programClass.equalsIgnoreCase("ModuleDiscovery") && !programClass.equalsIgnoreCase("EnsemblePrediction")) list.add(program.getName());
        }
        String[] result=new String[list.size()];
        result=list.toArray(result);
        Arrays.sort(result);
        return result;
    }  
    
    /** Returns a registered external program with the given name (or null if not found) */
    public ExternalProgram getExternalProgram(String programname) {
        return externalPrograms.get(programname);
    }

    
    /** Returns a collection containing all External Programs registered with the engine */
    public Collection<ExternalProgram> getAllExternalPrograms() {
        return externalPrograms.values();
    }
    

    /** Returns a default registered analysis with the given name (or null if not found)
     *  This analysis should only be used as a template for probing parameters. 
     *  It should not be used to run the analysis and store the data
     *  Use getNewAnalysis() to create a new analysis object for this purpose
     */
    public Analysis getAnalysis(String analysisName) {
        return analyses.get(analysisName);
    }    
    
    /** Returns an "empty" analysis object with the given analysisname (or null if not found)
     *  Analysis objects returned by this method could be used to run and store analyses
     */
    public Analysis getNewAnalysis(String analysisName) {
        Analysis template=analyses.get(analysisName);
        if (template==null || template instanceof CollatedAnalysis) return null;
        try {
           Analysis newAnalysis=(Analysis)template.getClass().newInstance();
           return newAnalysis;
        } catch (Exception e) {
            System.err.println(e);
            return null;}        
    }    

    /** Returns a list of registered MotifComparators
     */
    @SuppressWarnings("unchecked")
    public ArrayList<MotifComparator> getAllMotifComparators() {
        if (motifcomparators==null) {
            motifcomparators=new ArrayList<MotifComparator>(6);
            motifcomparators.add(new motiflab.engine.util.MotifComparator_ALLR());         
            motifcomparators.add(new motiflab.engine.util.MotifComparator_Chi2());          
            motifcomparators.add(new motiflab.engine.util.MotifComparator_KLD());          
            motifcomparators.add(new motiflab.engine.util.MotifComparator_PearsonsCorrelation()); 
            motifcomparators.add(new motiflab.engine.util.MotifComparator_PearsonsCorrelationICWeighted());            
            motifcomparators.add(new motiflab.engine.util.MotifComparator_SSD()); 
            //motifcomparators.add(new motiflab.engine.util.MotifComparator_WIC()); // this comparator was terrible, so I exclude it                     
        }
        return (ArrayList<MotifComparator>)motifcomparators.clone();
    }
    
    /** Registers a new MotifComparator with the engine */
    public void registerMotifComparator(MotifComparator motifcomparator) {
        if (motifcomparators==null) getAllMotifComparators(); // initialize
        motifcomparators.add(motifcomparator);
    }
    
    /** Removes a registered MotifComparator from the engine */
    public void uninstallMotifComparator(MotifComparator motifcomparator) {
        if (motifcomparators==null) return; // nothing to remove
        motifcomparators.remove(motifcomparator);
    }    
    
    /** Returns a list of different MotifComparators */
    public String[] getAllMotifComparatorNames(boolean abbreviations) {
        ArrayList<MotifComparator> comparators=getAllMotifComparators();
        String[] result=new String[comparators.size()];
        for (int i=0;i<comparators.size();i++) result[i]=(abbreviations)?comparators.get(i).getAbbreviatedName():comparators.get(i).getName();
        return result;
    } 
    
    /** Returns the MotifComparator with the given name or abbreviation (or null if not such comparator is known) */
    public MotifComparator getMotifComparator(String name) {
        ArrayList<MotifComparator> comparators=getAllMotifComparators();
        for (int i=0;i<comparators.size();i++) {
            if (comparators.get(i).getName().equals(name) || comparators.get(i).getAbbreviatedName().equals(name)) return comparators.get(i);
        }
        return null;
    }     


    /** Returns true if the argument data object is the default sequence collection */
    public boolean isDefaultSequenceCollection(Data data) {
        return (data!=null && data==getDataItem(defaultSequenceCollectionName));
    }
    
    /** 
     * Checks if the given word is marked as 'reserved', which means that it
     * can not be used as the name of a datatrack or variable in a protocol script
     */
    public boolean isReservedWord(String word) {
        return reservedwords.contains(word);
    }
    
    /** 
     * Returns all reserved words as a set
     */
    @SuppressWarnings("unchecked")
    public HashSet<String> getReservedWords() {
        return (HashSet<String>)reservedwords.clone();
    }    
    /** 
     * Marks the given word as 'reserved', which means that it
     * can not be used as the name of a datatrack or variable in a protocol script
     */
    public void registerReservedWord(String word) {
        reservedwords.add(word);
    }
    
    public void unregisterReservedWord(String word) {
        reservedwords.remove(word);
    }    
    
    /**
     * Returns TRUE if there are currently no Data items in storage (except perhaps the empty DefaultSequenceCollection)
     * @return
     */
    public boolean isDataStorageEmpty() {
        if (storage.getSize()==0) return true;
        if (storage.getSize()==1) {
            return getDefaultSequenceCollection()!=null; // if DefaultSequenceCollection is the only object in storage, consider it empty
        } else return false;
    }
      
    /** 
     * Returns a numeric dataobject corresponding to the given string.
     * If the string represents a known numeric data object (NumericVariable, NumericConstant or NumericMap)
     * this object will be returned. If the string represents a literal numeric value,
     * a new Double object holding this value will be returned.
     * If the string does not represent a numeric value (referring to a non-numeric data object
     * or a non-numeric literal string) a value of NULL will be returned.
     * @param string
     * @return 
     */
    public Object getNumericDataForString(String string) {
        if (string==null) return null;
        Data data = getDataItem(string);
        if (data instanceof NumericMap || data instanceof NumericVariable || data instanceof NumericConstant) return data;
        else if (data!=null) return null; // The string refers to a data object but not of a numeric type
        else {
            try {
                Double value=Double.parseDouble(string);
                return value;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
    
    /**
     * If the string has value TRUE/YES or FALSE/NO, a boolean object will be returned.
     * If the string contains an integer value an Integer will be returned
     * If the string contains a double value a Double will be returned
     * Else the string itself will be returned
     * @param string
     * @return 
     */
    public static Object getBasicValueForStringAsObject(String string) {
        if (string==null) return null;
        if (string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes")) return Boolean.TRUE;
        if (string.equalsIgnoreCase("false") || string.equalsIgnoreCase("no")) return Boolean.FALSE;
        try {
           int value=Integer.parseInt(string);
           return value;
        } catch (NumberFormatException e) {}
        try {
           double value=Double.parseDouble(string);
           return value;
        } catch (NumberFormatException e) {} 
        return string;
    }
    
    
    /** Given the class of a Data object (e.g. MotifCollection.class) the method will 
     *  return a String representation of the data type (e.g. "Motif Collection").
     *  If the class is not recognized as a Data type the method will return the String "unknown type"
     */
    public String getTypeNameForDataClass(Class dataclass) {
             if (dataclass==BackgroundModel.class) return BackgroundModel.getType();
        else if (dataclass==DNASequenceDataset.class) return DNASequenceData.getType();
        else if (dataclass==NumericDataset.class) return NumericDataset.getType();
        else if (dataclass==RegionDataset.class) return RegionDataset.getType();
        else if (dataclass==DNASequenceData.class) return DNASequenceData.getType();
        else if (dataclass==NumericSequenceData.class) return NumericSequenceData.getType();
        else if (dataclass==RegionSequenceData.class) return RegionSequenceData.getType();
        else if (dataclass==Motif.class) return Motif.getType();
        else if (dataclass==MotifCollection.class) return MotifCollection.getType();
        else if (dataclass==MotifPartition.class) return MotifPartition.getType();
        else if (dataclass==Module.class) return Module.getType();
        else if (dataclass==ModuleCollection.class) return ModuleCollection.getType();
        else if (dataclass==ModulePartition.class) return ModulePartition.getType();
        else if (dataclass==NumericConstant.class) return NumericConstant.getType();
        else if (dataclass==NumericVariable.class) return NumericVariable.getType();
        else if (dataclass==GeneralNumericMap.class) return GeneralNumericMap.getType();
        else if (dataclass==SequenceNumericMap.class) return SequenceNumericMap.getType();
        else if (dataclass==MotifNumericMap.class) return MotifNumericMap.getType();
        else if (dataclass==ModuleNumericMap.class) return ModuleNumericMap.getType();
        else if (dataclass==SequenceTextMap.class) return SequenceTextMap.getType();
        else if (dataclass==MotifTextMap.class) return MotifTextMap.getType();
        else if (dataclass==ModuleTextMap.class) return ModuleTextMap.getType();        
        else if (dataclass==OutputData.class) return OutputData.getType();
        else if (dataclass==Sequence.class) return Sequence.getType();
        else if (dataclass==SequenceCollection.class) return SequenceCollection.getType();
        else if (dataclass==SequencePartition.class) return SequencePartition.getType();
        else if (dataclass==TextVariable.class) return TextVariable.getType();
        else if (dataclass==ExpressionProfile.class) return ExpressionProfile.getType();
        else if (dataclass==PriorsGenerator.class) return PriorsGenerator.getType();
        else if (dataclass!=null && Analysis.class.isAssignableFrom(dataclass)) {
            Analysis analysis=getAnalysisForClass(dataclass);
            if (analysis!=null) return analysis.getTypeDescription(); // returns "Analysis: <analysis type name>"
            else return Analysis.getType(); // this will only return "Analysis"
        }                 
        else return "unknown type: "+((dataclass!=null)?dataclass.getSimpleName():"null");
    }
    
    public String getTypeNameForDataOrBasicClass(Class dataclass) {
        if (dataclass==null) return null;
        if (Data.class.isAssignableFrom(dataclass)) return getTypeNameForDataClass(dataclass);
        else if (dataclass==String.class) return "String";
        else if (dataclass==Double.class || dataclass==Float.class) return "Double";
        else if (dataclass==Integer.class) return "Integer";
        else if (dataclass==Boolean.class) return "Boolean";
        else if (List.class.isAssignableFrom(dataclass)) return "List";
        else return null;        
    }
    
    /** Given a string representing a data type (e.g. "Motif Collection") the method will 
     *  return the corresponding class (e.g. MotifCollection.class) or NULL if the data type is not known
     *  @param typeName the name of the data type, which should correspond to a value returned by the X.getType()
     *                  static method (where X is a subclass of Data)
     *                  For analyses, this name could be either "Analysis" which will return the supertype Analysis.class 
     *                  or a name on the form "Analysis: analysis name" which will return the corresponding Analysis subclass
     */
    public Class getDataClassForTypeName(String typeName) {
             if (typeName==null) return null;
        else if (typeName.equalsIgnoreCase(BackgroundModel.getType())) return BackgroundModel.class;
        else if (typeName.equalsIgnoreCase(DNASequenceDataset.getType())) return DNASequenceDataset.class;
        else if (typeName.equalsIgnoreCase(NumericDataset.getType())) return NumericDataset.class;
        else if (typeName.equalsIgnoreCase(RegionDataset.getType())) return RegionDataset.class;
        else if (typeName.equalsIgnoreCase(DNASequenceData.getType())) return DNASequenceData.class;
        else if (typeName.equalsIgnoreCase(NumericSequenceData.getType())) return NumericSequenceData.class;
        else if (typeName.equalsIgnoreCase(RegionSequenceData.getType())) return RegionSequenceData.class;
        else if (typeName.equalsIgnoreCase(Motif.getType())) return Motif.class;
        else if (typeName.equalsIgnoreCase(Module.getType())) return Module.class;
        else if (typeName.equalsIgnoreCase(ModuleCollection.getType())) return ModuleCollection.class;
        else if (typeName.equalsIgnoreCase(MotifCollection.getType())) return MotifCollection.class;
        else if (typeName.equalsIgnoreCase(MotifPartition.getType())) return MotifPartition.class;
        else if (typeName.equalsIgnoreCase(ModulePartition.getType())) return ModulePartition.class;
        else if (typeName.equalsIgnoreCase(NumericVariable.getType())) return NumericVariable.class;
        else if (typeName.equalsIgnoreCase(GeneralNumericMap.getType())) return GeneralNumericMap.class;
        else if (typeName.equalsIgnoreCase(SequenceNumericMap.getType())) return SequenceNumericMap.class;
        else if (typeName.equalsIgnoreCase(MotifNumericMap.getType())) return MotifNumericMap.class;
        else if (typeName.equalsIgnoreCase(ModuleNumericMap.getType())) return ModuleNumericMap.class;
        else if (typeName.equalsIgnoreCase(SequenceTextMap.getType())) return SequenceTextMap.class;
        else if (typeName.equalsIgnoreCase(MotifTextMap.getType())) return MotifTextMap.class;
        else if (typeName.equalsIgnoreCase(ModuleTextMap.getType())) return ModuleTextMap.class;        
        else if (typeName.equalsIgnoreCase(OutputData.getType())) return OutputData.class;
        else if (typeName.equalsIgnoreCase(Sequence.getType())) return Sequence.class;
        else if (typeName.equalsIgnoreCase(SequenceCollection.getType())) return SequenceCollection.class;
        else if (typeName.equalsIgnoreCase(SequencePartition.getType())) return SequencePartition.class;
        else if (typeName.equalsIgnoreCase(TextVariable.getType())) return TextVariable.class;
        else if (typeName.equalsIgnoreCase(ExpressionProfile.getType())) return ExpressionProfile.class;
        else if (typeName.equalsIgnoreCase(PriorsGenerator.getType())) return PriorsGenerator.class;
        else if (typeName.equalsIgnoreCase(Analysis.getType())) return Analysis.class;
        else if (typeName.toLowerCase().startsWith("analysis:")) {
            String analysisName=typeName.substring("analysis:".length()).trim();
            return getClassForAnalysis(analysisName);
        }     
        else return null;
    }    
        
    /** 
     * Checks if the given name can be used as the name for a NEW datatrack or variable
     * The method checks whether the name is syntactically correct, if it is a reserved
     * word or if it is already in use by another data object.
     * If the name can be used the method returns NULL, otherwise it returns a string
     * containing the reason why the name can not be used
     * @param name The name to be checked (this should not contain flanking whitepace)
     * @param checkInUse If this flag is set to TRUE an error message will be reported if
     * the name is already used by another data object. If FALSE then this check will be
     * be performed
     */    
    public String checkNameValidity(String name, boolean checkInUse) {
            if (name.isEmpty()) return "The name is empty";        
       else if (name.matches(".*[^a-zA-Z_0-9].*")) return "The name contains illegal characters";        
       else if (!name.matches("^[a-zA-Z_].*")) return "The name must start with a letter";        
       else if (isReservedWord(name)) return "This word is reserved and can not be used";
       else if (checkInUse && getDataItem(name)!=null) return "The name entered is already in use";     
       else return null; // Name is OK
    }    

    /**
     * Checks if the given name can be used as the name for a sequence.
     * The syntactic rules for sequence names are the same as for other data objects, except that 
     * sequences are allowed to have names starting with numbers (so that Entrez IDs can
     * be used as sequence names).
     * In MotifLab version 2.0+ sequence names can also contain the following: . - + () []
     * The method checks whether the name is syntactically correct, if it is a reserved
     * word or if it is already in use by another data object.
     * If the name can be used the method returns NULL, otherwise it returns a string
     * containing the reason why the name can not be used
     * @param name The name to be checked (this should not contain flanking whitespace)
     * @param checkInUse If this flag is set to TRUE an error message will be reported if
     * the name is already used by another data object. If FALSE then this check will be
     * be performed
     */
    public String checkSequenceNameValidity(String name, boolean checkInUse) {
            if (name.isEmpty()) return "The name is empty";
       else if (!name.matches("[\\w\\.\\-\\+\\[\\]\\(\\)]+")) return "The name contains illegal characters"; // allow . - + [] and ()
       else if (!name.matches("^[a-zA-Z0-9_].*")) return "The name must start with a letter or number"; 
       else if (isReservedWord(name)) return "This word is reserved and can not be used";
       else if (checkInUse && getDataItem(name)!=null) return "The name entered is already in use";
       else return null; // Name is OK
    }
    
    /**
     * Takes a string which is illegal to use as a data name (because it contains
     * characters other than normal letters, numbers and underscores) and converts
     * it into a legal name.
     * @param name
     * @return 
     */
    public static String convertToLegalDataName(String name) {
        boolean startsWithUnderscore=(!name.isEmpty() && name.charAt(0)=='_');
        if (name.endsWith("+")) name=name.substring(0,name.length()-1)+"D"; // convert suffix "+" to "D" and suffix "-" to "R"
        else if (name.endsWith("-")) name=name.substring(0,name.length()-1)+"R"; // convert suffix "+" to "D" and suffix "-" to "R"
        name=name.replaceAll("\\W", "_"); // replace all illegal characters with underscores
        name=name.replaceAll("__+", "_"); // collapse runs of underscores
        if (!startsWithUnderscore) name=name.replaceAll("^_", ""); // remove leading underscore introduced by replacements
        name=name.replaceAll("_$", ""); // remove trailing underscore  
        char firstChar=name.charAt(0);
        if (!(Character.isLetter(firstChar) || firstChar=='_')) name="Data"+name;       
        return name;
    }
    
    /**
     * Takes a string which is illegal to use as a sequence name (because it contains
     * characters other than normal letters, numbers and underscores or . + - () and [] ) and converts
     * it into a legal name. If the name does not start with a letter or number, the prefix "Seq" is added.
     * @param name
     * @return 
     */
    public static String convertToLegalSequenceName(String name) {
        boolean startsWithUnderscore=(!name.isEmpty() && name.charAt(0)=='_');
        name=name.replaceAll("[^\\w\\.\\-\\+\\[\\]\\(\\)]", "_"); // replace all illegal characters with underscores
        name=name.replaceAll("__+", "_"); // collapse runs of underscores
        if (!startsWithUnderscore) name=name.replaceAll("^_", ""); // remove leading underscore introduced by replacements
        name=name.replaceAll("_$", ""); // remove trailing underscore  
        char firstChar=name.charAt(0);
        if (!(Character.isLetter(firstChar) || Character.isDigit(firstChar) || firstChar=='_')) name="Seq"+name;
        return name;
    }    
    
    
    /**
     * Returns a list of requirements for the current session, that should be included at the start of the session file "header".
     * These requirements could e.g. be names of plugins that are required to restore certain data types in the stream
     * @return A list of requirements, which could be String[] but not NULL
     */
    public String[] getSessionRequirements(List list) {
        HashSet<String> pluginNames=new HashSet<String>();
        for (Object obj:list) {
            if (obj instanceof Plugin) {
                String text="Plugin: "+((Plugin)obj).getPluginName();
                pluginNames.add(text);
            }
        }
        if (pluginNames.isEmpty()) return new String[0];
        String[] req=new String[pluginNames.size()];
        int i=0;
        Iterator<String> iter=pluginNames.iterator();
        while (iter.hasNext()) {
            req[i]=iter.next();
            i++;
        }
        return req;
    }
    
    
    // *********************************    MESSAGE LISTENING    ***************************************

        
    /**
     * Registers a new message listener with the engine
     *  
     * @param listener The object that wants to receive messages from the engine
     */
    public void addMessageListener(MessageListener listener) {
        synchronized(messagelisteners) {
            messagelisteners.add(listener);
        }
    }
    
     /**
     * Removes a previously registered message listener
     *  
     * @param listener The object that wants to receive messages from the engine
     */   
    public void removeMessageListener(MessageListener listener) {
        synchronized(messagelisteners) {        
            messagelisteners.remove(listener);
        }
    }
    
    
    /**
     * Takes an incoming error message and distributes it to all registered listeners
     * @param msg The error message itself
     * @param errortype An errorcode specifying the type of error
     */
    @Override    
    public void errorMessage(String msg, int errortype) {
        synchronized(messagelisteners) {            
            if (messagelisteners.isEmpty()) System.err.println(msg);
            for (MessageListener listener:messagelisteners) {
                listener.errorMessage(msg, errortype);
            }
        }
    }
    
    /**
     * Takes an incoming log message and distributes it to all registered listeners
     * @param msg The log message itself
     */
    @Override    
    public void logMessage(String msg) {
        logMessage(msg, 20);   
    }
    
    @Override    
    public void logMessage(String msg, int level) {
        synchronized(messagelisteners) {            
            if (messagelisteners.isEmpty()) System.err.println(msg);
            for (MessageListener listener:messagelisteners) {
                listener.logMessage(msg, level);
            }   
        }
    }    

    
    /**
     * Takes an incoming status message and distributes it to all registered listeners
     * @param msg The status message itself
     */
    @Override
    public void statusMessage(String msg) {
        synchronized(messagelisteners) {           
            for (MessageListener listener:messagelisteners) {
                listener.statusMessage(msg);
            } 
        }
    }

    /**
     * Takes an incoming progress message and distributes it to all registered listeners
     * @param progress A number indicating progress (usually between 0 and 100)
     */
    @Override
    public void progressReportMessage(int progress) {
        synchronized(messagelisteners) {           
            for (MessageListener listener:messagelisteners) {
                listener.progressReportMessage(progress);
            } 
        }
    }    
    
    // *********************************    DATA LISTENING    ***************************************
    
    /**
     * Registers a new data listener with the engine
     * Although interested parties can register as DataListeners directly with Data objects
     * it is preferable that they rather register as listener with the Engine. 
     * The engine will automatically track any changes to all Data Items stored (being itself
     * a registered listener with all Data items) and will propagate important messages to
     * all listeners that have registered with the Engine.
     *  
     * @param listener The object that wants to receive data event messages from the engine
     */
    public void addDataListener(DataListener listener) {
        synchronized (datalisteners) {
            datalisteners.add(listener);
        }
    }
    
     /**
     * Removes a previously registered data listener
     *  
     * @param listener The object that wants to receive data event  messages from the engine
     */   
    public void removeDataListener(DataListener listener) {
        synchronized (datalisteners) {
            datalisteners.remove(listener);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void dataAdded(Data data) {
        HashSet<DataListener> listenercopy;
        synchronized (datalisteners) {
            listenercopy=(HashSet<DataListener>)datalisteners.clone();
        }        
        for (DataListener listener:listenercopy) {
            listener.dataAdded(data);
        }
    }
    @Override
    @SuppressWarnings("unchecked")
    public void dataAddedToSet(Data parent,Data child) {
        HashSet<DataListener> listenercopy;
        synchronized (datalisteners) {
            listenercopy=(HashSet<DataListener>)datalisteners.clone();
        }         
        for (DataListener listener:listenercopy) {
            listener.dataAddedToSet(parent,child);
        }
    }
    @Override
    @SuppressWarnings("unchecked")
    public void dataRemoved(Data data) {
        HashSet<DataListener> listenercopy;
        synchronized (datalisteners) {
            listenercopy=(HashSet<DataListener>)datalisteners.clone();
        }         
        for (DataListener listener:listenercopy) {
            listener.dataRemoved(data);
        }
    }
    @Override
    @SuppressWarnings("unchecked")
    public void dataRemovedFromSet(Data parent,Data child) {
        HashSet<DataListener> listenercopy;
        synchronized (datalisteners) {
            listenercopy=(HashSet<DataListener>)datalisteners.clone();
        }         
        for (DataListener listener:listenercopy) {
            listener.dataRemovedFromSet(parent,child);
        }
    }
       
    /**
     * Propagates notification of Data updates to any registered DataListeners
     * Parties interested in Data updates should preferentially register as listeners with 
     * the Engine rather than registering directly with the dataitem
     * @param data
     */
    @Override
    @SuppressWarnings("unchecked")
    public void dataUpdated(Data data) {
        HashSet<DataListener> listenercopy;
        synchronized (datalisteners) {
            listenercopy=(HashSet<DataListener>)datalisteners.clone();
        }         
        for (DataListener listener:listenercopy) {
            listener.dataUpdated(data);
        }
    }
    
    @Override
    public void dataUpdate(Data olddata, Data newdata) {
       // we do not need to implement this since it is only invoked by the engine anyway
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    public void dataOrderChanged(Data data, Integer oldpos, Integer newpos) {
        HashSet<DataListener> listenercopy;
        synchronized (datalisteners) {
            listenercopy=(HashSet<DataListener>)datalisteners.clone();
        }         
        for (DataListener listener:listenercopy) {
            if (listener instanceof ExtendedDataListener) ((ExtendedDataListener)listener).dataOrderChanged(data,oldpos,newpos);
        }
    }    


    /**
     * Returns an object which can be used as a lock for synchronization purposes
     * @return
     */
    public Object getExecutionLock() {
       return executionLock;
    }
    
    
    /** This will convert a region track to a motif track 
     * @param dataset The region dataset to convert
     * @param namemap An optional map containing mapping from alternative names to correct motif IDs
     * @param dna A DNA track to use to fill in the 'sequence' property of motif sites (if they do not have this already)
     * @param force If force is TRUE then the track will be converted if the type of at least one region matches a motif identifier (or short name). 
     *              If force is FALSE then the method will first check a few regions and only convert the rest if at least half of these are motif regions (with match both to name and length) 
     * @return TRUE if the dataset is now a motif track (or was already), or FALSE if the track could not be converted to a motif track
     * 
     */
   public boolean convertRegionTrackToMotifTrack(RegionDataset dataset, HashMap<String,String>namemap, DNASequenceDataset dna, boolean force) {
      if (dataset.isMotifTrack()) return true; // track is already a motif track
      if (namemap==null) { // create new mapping
            namemap=new HashMap<String,String>();
            ArrayList<Data> motifs=getAllDataItemsOfType(Motif.class);
            for (Data motif:motifs) {
                namemap.put(((Motif)motif).getShortName(), motif.getName()); // note that there might be a 1-to-many mapping between shortname and motif ID but I ignore this here and just overwrite previous entries
            }         
      }
      if (!force) {
          int count=0; // number of regions processed
          int tfbs=0; //  number of regions that are TFBS
          int check=20; // check the first 20 regions. If fewer than half of these are motifs then do not convert
          outer:
          for (int i=0;i<dataset.getNumberofSequences();i++) {
              RegionSequenceData sequence=(RegionSequenceData)dataset.getSequenceByIndex(i);
              for (Region region:sequence.getOriginalRegions()) {
                  count++;
                  String type=region.getType();
                  Motif motif=(Motif)getDataItem(type, Motif.class);
                  if (motif==null && namemap!=null && namemap.containsKey(type)) {
                      motif=(Motif)getDataItem(namemap.get(type), Motif.class);                  
                  }
                  if (motif!=null && motif.getLength()==region.getLength()) tfbs++; // we have a potential motif region!    
                  if (count==check) break outer;
              } // end for each region
          }
          if ((double)tfbs/(double)count<0.5) return false; // abort conversion if too few regions are motifs
      }
      int count=0;
      for (int i=0;i<dataset.getNumberofSequences();i++) {
          RegionSequenceData sequence=(RegionSequenceData)dataset.getSequenceByIndex(i);
          DNASequenceData dnasequence=null;
          if (dna!=null) dnasequence=(DNASequenceData) dna.getSequenceByName(sequence.getName());
          for (Region region:sequence.getOriginalRegions()) {
              String type=region.getType();
              if (dataExists(type, Motif.class)) {} // region type is already a recognized motif name
              else if (namemap!=null && namemap.containsKey(type)) { // region type is not a recognized motif name, but check the namemap if the type can be mapped to a motif name
                  region.setType(namemap.get(type));
              } else continue; // this is not a recognized motif
              Object bindingsite=region.getProperty("sequence");
              if (bindingsite==null && dnasequence!=null) {
                  char[] interval=(char[])dnasequence.getValueInGenomicInterval(region.getGenomicStart(), region.getGenomicEnd());
                  String siteseq=new String((interval!=null)?interval:new char[0]);
                  if (region.getOrientation()==Region.REVERSE) siteseq=MotifLabEngine.reverseSequence(siteseq);
                  region.setProperty("sequence", siteseq);
              }
              count++;
          } // end for each region
      }
      boolean success=(count>0); // if we managed to find at least some known motifs
      dataset.setMotifTrack(success);
      return success;
   }

    /** This will convert a region track to a module track
     * @param dataset The region dataset to convert
     * @param dna A DNA track to use to fill in the 'sequence' property of motif sites (if they do not have this already)
     * @param force If force is TRUE then the track will be converted if the type of at least one region matches a module identifier 
     *              If force is FALSE then the method will first check a few regions and only convert the rest if at least half of these are potentially module regions
     * @return TRUE if the dataset is now a module track (or was already), or FALSE if the track could not be converted to a module track
     */
   public boolean convertRegionTrackToModuleTrack(RegionDataset dataset, DNASequenceDataset dna, boolean force) {
      if (dataset.isModuleTrack()) return true; // track is already a module track
      if (!force) {
          int count=0; // number of regions processed
          int modules=0; //  number of regions that are potentially modules
          int check=10; // check the first 10 regions. If fewer than half of these are modules then do not convert
          outer:
          for (int i=0;i<dataset.getNumberofSequences();i++) {
              RegionSequenceData sequence=(RegionSequenceData)dataset.getSequenceByIndex(i);
              for (Region region:sequence.getOriginalRegions()) {
                  count++;
                  String type=region.getType();
                  Module module=(Module)getDataItem(type, Module.class);     
                  if (module!=null ) modules++; // we have a potential module region!    
                  if (count==check) break outer;
              } // end for each region
          }
          if ((double)modules/(double)count<0.5) return false; // abort conversion if too few regions are modules
      }          
      int count=0; // number of modules converted
      for (int i=0;i<dataset.getNumberofSequences();i++) {
          RegionSequenceData sequence=(RegionSequenceData)dataset.getSequenceByIndex(i);
          DNASequenceData dnasequence=null;
          if (dna!=null) dnasequence=(DNASequenceData) dna.getSequenceByName(sequence.getName());
          for (Region region:sequence.getOriginalRegions()) {
              String type=region.getType();
              if (!dataExists(type, Module.class)) continue; // not a recognized module
              ArrayList<Region> modulemotifs=region.getNestedRegions(false);
              for (Region motifsite:modulemotifs) {
                  String motiftype=motifsite.getType();
                  if (!dataExists(motiftype, Motif.class)) continue; // not a recognized motif
                  Object bindingsite=motifsite.getProperty("sequence");
                  if (bindingsite==null && dnasequence!=null) {
                      char[] interval=(char[])dnasequence.getValueInGenomicInterval(motifsite.getGenomicStart(), motifsite.getGenomicEnd());
                      String siteseq=new String(interval);
                      if (motifsite.getOrientation()==Region.REVERSE) siteseq=MotifLabEngine.reverseSequence(siteseq);
                      motifsite.setProperty("sequence", siteseq);
                  }
              }
              count++;
          } // end for each region
      }
      boolean success=(count>0); // if we managed to find at least some known modules
      dataset.setModuleTrack(success);
      return success;
   }
   
   public boolean convertRegionTrackToNestedTrack(RegionDataset dataset) {
      if (dataset.isNestedTrack()) return true; // track is already a nested track
      boolean hasNestedRegions=false;
      outer:
      for (int i=0;i<dataset.getNumberofSequences();i++) { // check if any of the regions are nested
          RegionSequenceData sequence=(RegionSequenceData)dataset.getSequenceByIndex(i);
          for (Region region:sequence.getOriginalRegions()) {
              if (region.hasNestedRegions()) {hasNestedRegions=true;break outer;}
          } 
      }
      dataset.setNestedTrack(hasNestedRegions);
      return hasNestedRegions;        
   }
   
    // *********************************    ADD RESOURCES    ***************************************

    public void importGeneIDResolverConfig() {
        if (geneIDResolver==null) {errorMessage("System Error: No Gene ID Resolver created",0);return;}
        logMessage("Importing Gene ID configurations");
        File configFile=new File(getMotifLabDirectory()+java.io.File.separator+"GeneIDResolver.config");
        if (configFile.exists()) {
            try {
                geneIDResolver.initializeFromFile(configFile,true);
            } catch (SystemError e) {
                errorMessage("Unable to read gene ID resolver configuration from file because the file is badly formatted:\n"+e.getMessage(),0);
            } catch (IOException e) {
                errorMessage("Unable to read gene ID resolver configuration from file because of IO-error: "+e.getMessage(),0);
            }
        }
    }

    public void importOrganisms() {
        logMessage("Importing Organisms");
        File organismFile=new File(getMotifLabDirectory()+java.io.File.separator+"Organisms.config");
        if (organismFile.exists()) {
            try {
                Organism.initializeFromFile(organismFile);
            } catch (SystemError e) {
                errorMessage("Unable to read Organism definitions from file because the file is badly formatted:\n"+e.getMessage(),0);
            } catch (IOException e) {
                errorMessage("Unable to read Organism definitions from file because of IO-error: "+e.getMessage(),0);
            }
        }
    }

    public void importMotifClassifications() {
        logMessage("Importing Motif classifications");       
        try {
            File tfclassFile=new File(getMotifLabDirectory()+java.io.File.separator+"TFclass.config");
            if (tfclassFile.exists()) {
                MotifClassification.initializeFromFile(tfclassFile);
            } else {
                InputStream stream=this.getClass().getResourceAsStream("/motiflab/engine/resources/TFclass.config");
                MotifClassification.initializeFromStream(stream);
            }      
        } catch (SystemError e) {
            errorMessage("Unable to read motif class definitions from file because the file is badly formatted:\n"+e.getMessage(),0);
        } catch (IOException e) {
            errorMessage("Unable to read motif class definitions from file because of IO-error: "+e.getMessage(),0);
        }       
    }    

    /** Imports known operations and registers them with the engine. Also installs standard parsers */
    private void importOperations() {
        logMessage("Importing Operations");
        for (String opname:operations) {
           //System.err.println("Installing operation: "+opname);
           try { 
              Class operationClass=Class.forName("motiflab.engine.operations.Operation_"+opname);
              Operation operation=(Operation)operationClass.newInstance();
              operation.setEngine(this);
              registerReservedWord(opname);
              String[] words=operation.getReservedWords();
              if (words!=null) {
                  for (String word:words) registerReservedWord(word);
              }
              operationslist.put(opname,operation);
           } catch(ClassNotFoundException cnfe) {
               errorMessage("WARNING: No class found for operation: "+opname,0);
           } catch(InstantiationException ie) {
               errorMessage("WARNING: InstantiationException for operation: "+opname,0);
           } catch(IllegalAccessException iae) {
               errorMessage("WARNING: IllegalAccessException for operation: "+opname,0);
           }                                   
        }        
    }

    /** Imports known analyses and registers them with the engine. */
    private void importAnalyses() {
        logMessage("Importing Analyses");
        registerAnalysis(new motiflab.engine.data.analysis.BenchmarkAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.BindingSequenceOccurrences());
        registerAnalysis(new motiflab.engine.data.analysis.CompareMotifOccurrencesAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.NumericMapDistributionAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.DistributionAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.GCcontentAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.CompareRegionDatasetsAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.EvaluatePriorAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.MotifOccurrenceAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.ModuleOccurrenceAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.MotifCollectionStatisticsAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.MotifRegressionAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.SingleMotifRegressionAnalysis());      
        registerAnalysis(new motiflab.engine.data.analysis.MapCorrelationAnalysis());   
        registerAnalysis(new motiflab.engine.data.analysis.CompareMotifsToNumericDatasetAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.MotifPositionDistributionAnalysis());      
        registerAnalysis(new motiflab.engine.data.analysis.CompareCollectionsAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.CompareClustersToCollectionAnalysis());
        //registerAnalysis(new motiflab.engine.data.analysis.MotifSequenceScoreAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.RegionOccurrenceAnalysis());      
        registerAnalysis(new motiflab.engine.data.analysis.CompareRegionOccurrencesAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.RegionDatasetCoverageAnalysis());
        registerAnalysis(new motiflab.engine.data.analysis.MotifComparisonAnalysis());      
        registerAnalysis(new motiflab.engine.data.analysis.ROCAnalysis());         
        registerAnalysis(new motiflab.engine.data.analysis.CollatedAnalysis("dummy"));
        //   
    }    
    
    /** Imports known data formats and registers them with the engine
     * (For now, this is hard-coded, but I guess it should not be...)
     */
    private void importDataFormats() {
        logMessage("Importing Data formats");
        // the second parameter in registerDataFormat() specifies which data types the format should be used as defaults for
        registerDataFormat(DataFormat_Plain.class, null);

        registerDataFormat(DataFormat_Location.class, null);
        // Feature data formats
        registerDataFormat(DataFormat_FASTA.class, new Class[]{DNASequenceData.class,DNASequenceDataset.class});
        registerDataFormat(DataFormat_MFASTA.class, null); // this is MultiFASTA which is used by ChIPMunk
        registerDataFormat(DataFormat_2bit.class, null); //
        //registerDataFormat(DataFormat_Consensus.class, null);
        
        registerDataFormat(DataFormat_GFF.class, new Class[]{RegionSequenceData.class,RegionDataset.class});
        registerDataFormat(DataFormat_GTF.class, null);
        registerDataFormat(DataFormat_EvidenceGFF.class, null);  
        registerDataFormat(DataFormat_RegionProperties.class, null);           
        registerDataFormat(DataFormat_CisML.class, null);        
        registerDataFormat(DataFormat_BED.class, null);
        registerDataFormat(DataFormat_BigBed.class, null);
        registerDataFormat(DataFormat_Interactions.class, null);
        
        registerDataFormat(DataFormat_WIG.class, null);
        registerDataFormat(DataFormat_Priority.class, new Class[]{NumericSequenceData.class,NumericDataset.class});
        registerDataFormat(DataFormat_PSP.class, null);
        registerDataFormat(DataFormat_BedGraph.class,null);
        registerDataFormat(DataFormat_BigWig.class,null);
        
        // Background model formats
        registerDataFormat(DataFormat_PriorityBackground.class, null);
        registerDataFormat(DataFormat_MEME_Background.class, null);
        registerDataFormat(DataFormat_INCLUSive_Background.class, new Class[]{BackgroundModel.class});
        
        // Motif/Module formats
        registerDataFormat(DataFormat_INCLUSive_Motif.class, null);
        registerDataFormat(DataFormat_RawPSSM.class, null);
        registerDataFormat(DataFormat_TRANSFAC.class, null);
        registerDataFormat(DataFormat_Jaspar.class, null);
        registerDataFormat(DataFormat_BindingSequences.class, null);        
        registerDataFormat(DataFormat_XMS.class, null);
        registerDataFormat(DataFormat_MEME_Minimal_Motif.class, null);
        registerDataFormat(DataFormat_HTML_MotifTable.class, null);
        registerDataFormat(DataFormat_HTML_Matrix.class, null);
        registerDataFormat(DataFormat_HTML_ModuleTable.class, null);
        registerDataFormat(DataFormat_MotifProperties.class, null);
        registerDataFormat(DataFormat_ModuleProperties.class, null);        
        registerDataFormat(DataFormat_TAMO.class, null);        
        registerDataFormat(DataFormat_MotifLabMotif.class, new Class[]{MotifCollection.class,Motif.class});
        registerDataFormat(DataFormat_MotifLabModule.class, new Class[]{ModuleCollection.class,Module.class});
        registerDataFormat(DataFormat_Properties.class, null);         

        // Analysis formats
        registerDataFormat(DataFormat_HTML.class, new Class[]{Analysis.class});
        registerDataFormat(DataFormat_RawData.class, null);
        registerDataFormat(DataFormat_Excel.class, null);        

        // Other data types
        registerDataFormat(DataFormat_ExpressionProfile.class, new Class[]{ExpressionProfile.class});
        registerDataFormat(DataFormat_PriorsGeneratorFormat.class, new Class[]{PriorsGenerator.class});
        registerDataFormat(DataFormat_MapFormat.class, new Class[]{MotifNumericMap.class,ModuleNumericMap.class,SequenceNumericMap.class,MotifTextMap.class,ModuleTextMap.class,SequenceTextMap.class});
        registerDataFormat(DataFormat_MapExpression.class, null);        
        registerDataFormat(DataFormat_ExcelMap.class, null);        
        registerDataFormat(DataFormat_TransfacProfile.class, null);  
        registerDataFormat(DataFormat_ExcelProfile.class, null);
        registerDataFormat(DataFormat_SequenceProperties.class, null);     
        registerDataFormat(DataFormat_TemplateHTML.class, null);
        registerDataFormat(DataFormat_Template.class, null);        
        
        // Formats for parsing output from external programs
        registerDataFormat(DataFormat_Weeder.class, null);
        registerDataFormat(DataFormat_MDscan.class, null);
        registerDataFormat(DataFormat_BioProspector.class, null);
        registerDataFormat(DataFormat_MEME.class, null);
        registerDataFormat(DataFormat_AlignAce.class, null);
        registerDataFormat(DataFormat_SeSiMCMC.class, null);
        registerDataFormat(DataFormat_ModuleSearcher.class, null);
        registerDataFormat(DataFormat_ClusterBuster.class, null);
        registerDataFormat(DataFormat_FIMO.class, null);
        registerDataFormat(DataFormat_PSP_binned.class, null);
        registerDataFormat(DataFormat_Clover.class, null);   
        registerDataFormat(DataFormat_MATCH.class, null);   
        registerDataFormat(DataFormat_ChIPMunk.class, null);
        registerDataFormat(DataFormat_MatrixREDUCE.class, null);         
        // registerDataFormat(DataFormat_DRIMust.class, null);   
        //registerDataFormat(DataFormat_ConsensusResults.class, null);        
        registerDataFormat(DataFormat_VOID.class, null); 
        registerDataFormat(DataFormat_Graph.class, null);
        
          
     }


    /** Returns the base-URL for the Web Site (including forward slash at the end) */
    public String getWebSiteURL() {
        // this should be a configurable property!!!
        return "http://tare.medisin.ntnu.no/motiflab/";
    }

    /** Returns the base-URL for the External Programs Repository (including forward slash at the end) */
    public String getRepositoryURL() {
        return getWebSiteURL()+"ExternalProgramsRepository/";
    }
    
    /** Returns the base-URL for the External Programs Repository (including forward slash at the end) */
    public String getPluginsRepositoryURL() {
        return getWebSiteURL()+"PluginsRepository/";
    }    

    /** Creates a serialized representation of the data repository configurations
     *  and stores it persistently in a system file
     */
    public void storeDataRepositoryConfigurations() {
        DataRepository[] reps=getDataRepositories();
        Object[][] ser=new Object[reps.length][3]; // contains name, class-type and ParameterSettings
        for (int i=0;i<reps.length;i++) {
            ser[i][0]=reps[i].getRepositoryName();
            ser[i][1]=reps[i].getClass().getName();
            ser[i][2]=reps[i].getConfigurationParameterSettings();        
        }
        try {
            storeSystemObject(ser, "datarepositories.ser");
        } catch (Exception e) {
            logMessage("An error occurred while saving data repository configurations: "+e.getMessage(), 30);
        }      
    }     
    
    /** Imports data repositories that have been registered with the engine.
     *  These are read from a serialized configuration
     */
    public void importDataRepositories() {
        logMessage("Importing Data Repositories");
        if (!systemObjectFileExists("datarepositories.ser")) return; // nothing to import
        Object ser=null;
        try {
            ser=loadSystemObject("datarepositories.ser",true);
            if (!(ser instanceof Object[][])) throw new ExecutionError("Warning: MotifLab's data repositories file is badly formatted. (Wrong object serialization)");
        } catch (Exception e) {
            logMessage("An error occurred while importing data repository configurations: "+e.getMessage(), 30);
        } 
        Object[][] reps=(Object[][])ser;
        for (int i=0;i<reps.length;i++) {
            String name=(String)reps[i][0];
            String classtypeName=(String)reps[i][1];
            ParameterSettings settings=(ParameterSettings)reps[i][2];   
            try {
                Class classtype=getDataRepositoryTypeFromClassName(classtypeName);
                if (classtype==null) throw new ExecutionError("Unknown data repository class: "+classtypeName);
                DataRepository newRepository=(DataRepository)classtype.newInstance();
                newRepository.setRepositoryName(name);
                newRepository.setConfigurationParameters(settings);    
                registerDataRepository(newRepository);
            } catch (Exception e) {
                 logMessage("An error occurred while importing data repository \""+name+"\": "+e.getMessage(), 30);
            }
        }                
    }    
    
    /** Imports known external programs and registers them with the engine
     *  The list of external programs is based on XML-files found in the 
     *  'external' subdirectory under the main workdirectory
     */
    public void importExternalPrograms() {
        logMessage("Importing External Programs");  
        ArrayList<String> imported=new ArrayList<String>(); // holds the names of imported programs
        File externaldir = new File(getMotifLabDirectory()+java.io.File.separator+"external");
        if (externaldir.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return (name.endsWith(".xml") || name.endsWith(".XML"));
                }
            };
            File[] programfiles=externaldir.listFiles(filter);
            for (File programfile:programfiles) {
                try {
                    ExternalProgram program=ExternalProgram.initializeExternalProgramFromFile(programfile,false);                      
                    if (program.getServiceType().equals("bundled") || program.getServiceType().equals("plugin")) { // If the external program is its own Java class (bundled) then change the object to that class instead of the generic superclass
                        String classname=program.getLocation();
                        if (classname==null || classname.isEmpty()) throw new SystemError("Missing class specification for "+program.getServiceType()+" program: "+program.getName());
                        program=(ExternalProgram)Class.forName(program.getLocation()).newInstance();
                    }                  
                    program.setConfigurationFile(programfile.getAbsolutePath());
                    registerExternalProgram(program); 
                    imported.add(program.getName()); // logMessage("Importing External Program: "+program.getName());
                } catch (Exception e) {errorMessage("ExternalProgram: "+e.getMessage(),0);}                    
            } 
            Collections.sort(imported);
            int group=3; // how many to output on each line
            for (int i=0;i<imported.size();i+=group) {
                int end=Math.min(i+group,imported.size());
                List<String> sublist=imported.subList(i, end);
                logMessage(" - "+MotifLabEngine.splice(sublist,", "));
            }
        } else {
            boolean ok=externaldir.mkdirs();
            if (!ok) errorMessage("Unable to create directory for external programs", 0);
        }
        
    }
    
    /** Imports all plugins */
    public void importPlugins() {
        importPlugins(null,null);
    }
    
    /** Imports plugins
     * This method can be called multiple times, but the same plugins should not be imported more than once.
     * Two different lists can optionally be provided as arguments. 
     * The first is a whitelist. If it is defined, only plugins in this list will be imported. 
     * The second is a blacklist. If this is defined, then all (whitelisted) plugins except the ones in this list will be imported. 
     * The entries in the lists can refer to plugins on the form "meta-attribute:value".
     * For instance "type:Tool", "type:DataSource" or "load:early" (if meta-attribute is omitted, it will default to "type". E.g. "Operation" equals "type:Operation")
     * @param processPlugins If defined (not null), plugins will only be imported if their "type" is in this list
     * @param skipPlugins    If defined (not null), plugins will NOT be imported if their "type" is in this list
     */
    public void importPlugins(String[] processPlugins, String[] skipPlugins) {    
        File plugindir=new File(getPluginsDirectory());
        File[] allFiles=plugindir.listFiles();
        if (allFiles.length>0) logMessage("Importing Plugins"); 
        ArrayList<String> pluginsOK=new ArrayList<String>(); // just to report which plugins were correctly imported        
        for (File dir:allFiles) { // go through all subdirectories in plugins directory
            if (!dir.isDirectory()) continue;
            HashMap<String, Object> metadata=null;
            Plugin plugin=null;
            try {
                metadata=readPluginMetaDataFromDirectory(dir); 
                // Skip plugin unless it satisfies the inclusion/exclusion criteria
                if (processPlugins!=null && !matchesPluginCritera(processPlugins, metadata)) continue; // this plugin is not in the "process" list
                if (skipPlugins!=null && matchesPluginCritera(skipPlugins, metadata)) continue; // this plugin is on the "skip" list  
                // if (metadata.containsKey("client")) { } // I thought it would be nice to have a "client" property (e.g. "client=motiflab.gui.MotifLabGUI") which can specify which clients a plugin is meant to be used with so as to avoid loading unnecessary plugins, but sadly the client is not yet defined at this point...                
                if (metadata.containsKey("requires")) checkPluginRequirements((String)metadata.get("requires")); // check if the plugin requires dependencies that do not exist          
                plugin=instantiatePluginFromDirectory(dir);
            } catch (SystemError e) {
                logMessage("  -> Plugin \""+dir.getAbsolutePath()+"\" : CRITICAL ERROR => "+e.getMessage()+". The plugin will not be activated!");  
                continue; // the directory did not contain correct metadata
            }  catch (Throwable tr) {
                logMessage("  -> Plugin \""+dir.getAbsolutePath()+"\" : CRITICAL ERROR => "+tr.toString()+". The plugin will not be activated!");  
                tr.printStackTrace(System.err);
                continue; // the directory did not contain correct metadata
            }        
            // The plugin object could be instantiated. Now try to initialize it and register it with the engine. Initialization from client will be performed later
            try {               
                plugin.initializePlugin(this);
                registerPlugin(plugin,metadata);              
                pluginsOK.add(plugin.getPluginName()); // logMessage("  - Plugin \""+plugin.getPluginName()+"\" : OK!");
            } catch (ExecutionError e) {
                logMessage("  -> Plugin \""+plugin.getPluginName()+"\" : ERROR => "+e.getMessage());                
                registerPlugin(plugin,metadata);                    
            } catch (SystemError se) {
                logMessage("  -> Plugin \""+plugin.getPluginName()+"\" : CRITICAL ERROR => "+se.getMessage()+". The plugin will not be activated!");
            } catch (Throwable tr) {
                logMessage("  -> Plugin \""+plugin.getPluginName()+"\" : CRITICAL ERROR => "+tr.toString()+". The plugin will not be activated!");
                tr.printStackTrace(System.err);
            }                                         
        }  
        if (!pluginsOK.isEmpty()) { // show which plugins have been loaded to the log
            int group=3; // how many to output on each line
            for (int i=0;i<pluginsOK.size();i+=group) {
                int end=Math.min(i+group,pluginsOK.size());
                List<String> sublist=pluginsOK.subList(i, end);
                logMessage(" - "+MotifLabEngine.splice(sublist,", "));
            }            
        }           
                 
    }     

    /** Returns TRUE if the plugin metadata matches at least one of the given criteria
     *  @param critera A non-null list of critera on the form "attribute:value" (if only the value is provided, the attribute will default to "type"
     *  @param pluginMetaData
     */
    private boolean matchesPluginCritera(String[] criteria, HashMap<String, Object> pluginMetaData) {
        for (String criterion:criteria) {
            String attribute="type";
            String value="Tool";
            if (criterion.contains(":")) {String[] parts=criterion.split(":"); attribute=parts[0];value=parts[1];}
            else {value=criterion;}
            String pluginAttributeValue=(pluginMetaData.containsKey(attribute))?pluginMetaData.get(attribute).toString():"";
            if (value.equalsIgnoreCase(pluginAttributeValue)) return true;
        }
        return false;
    }

    /**
     * Check the configured requirements for a Plugin and throw a SystemError for the first requirement that could not be satisfied
     * @param requires A comma-separated list of requirements for the plugin
     * @throws SystemError 
     */
    public void checkPluginRequirements(String requires) throws SystemError {
        String[] requirements=requires.split(",");
        for (String requirement:requirements) {
            requirement=requirement.trim();
            if (requirement.startsWith("class:")) {
                String requiredClass=requirement.substring("class:".length());
                try {
                    Class.forName(requiredClass);
                } catch (ClassNotFoundException c) {
                    throw new SystemError("Missing required class \""+requiredClass+"\". (Perhaps it is implemented in different plugin?)");
                }
            }  else if (requirement.startsWith("resource:")) {
                String requiredResource=requirement.substring("resource:".length());                
                String[] typeAndName=(requiredResource.contains(":"))?requiredResource.split(":",2):new String[]{null,requiredResource};
                Object resource=getResource(typeAndName[1], typeAndName[0]);
                if (resource==null) throw new SystemError("Missing required resource \""+requiredResource+"\". (Perhaps it is implemented in a different plugin?)");             
            }
        }        
    }    
    
    /** Load all the classes within the provided JAR file and return the (first and hopefully only) class which implements the Plugin interface 
     *  The method will also add all JAR-files residing beneath a lib/ directory to the class loader, so that they can be loaded when required
     */
    private Class loadClassesFromJar(File jarfile) throws Exception {
        Class pluginclass = null;
        // NOTE: The ! sign has special meaning in JAR paths as the separator between the path of the JAR file itself and the path to a file inside the JAR
        // jar:<URL for JAR file>!/<path within the JAR file>
        ArrayList<URL> allJars=new ArrayList<>(); // My first line of code written in Java 7 :-)
        allJars.add(new URL("jar:file:" + jarfile.getAbsolutePath()+"!/"));
        File libdir=new File(jarfile.getParentFile().getAbsolutePath()+"/lib/");
        if (libdir.exists() && libdir.isDirectory()) {
            File[] libJarFiles=libdir.listFiles(new FilenameFilter() {public boolean accept(File dir, String name) { return name.endsWith(".jar");}});
            for (File libJar:libJarFiles) {
                allJars.add(new URL("jar:file:" + libJar.getAbsolutePath()+"!/"));
            }
        }
        URL[] urls = new URL[allJars.size()];
        urls = allJars.toArray(urls);
        URLClassLoader loader = URLClassLoader.newInstance(urls, this.getClass().getClassLoader()); // Note: the second argument is necessary in order to work when MotifLab is run via Web Start
        if (pluginClassLoaders==null) pluginClassLoaders=new WeakHashMap<URLClassLoader,Boolean>();
        pluginClassLoaders.put(loader,Boolean.TRUE);
        JarFile jarFile = new JarFile(jarfile.getAbsolutePath());
        Enumeration e = jarFile.entries();        
        while (e.hasMoreElements()) {
            JarEntry je = (JarEntry) e.nextElement();
            if(je.isDirectory() || !je.getName().endsWith(".class")) continue;                     
            String className = je.getName().substring(0,je.getName().length()-".class".length());//
            className = className.replace('/', '.');
            Class newclass = loader.loadClass(className);
            if (Plugin.class.isAssignableFrom(newclass)) {
                if (!Modifier.isAbstract(newclass.getModifiers())) pluginclass = newclass;
            }
        }   
        return pluginclass;
    }
    
   /** Reads a plugin-directory and returns the plugin metadata contained in the file "plugin.config"
     * It also adds a couple of inferred properties, such as "documentation" and "pluginDirectory"
     * @param plugindir The directory that contains the plugin files
     * @return
     * @throws SystemError If a critical error occurred
     */
    public HashMap<String,Object> readPluginMetaDataFromDirectory(File plugindir) throws SystemError {
        File configfile=new File(plugindir,"plugin.ini");
        if (!configfile.exists()) configfile=new File(plugindir,"plugin.conf");
        if (!configfile.exists()) configfile=new File(plugindir,"plugin.config");
        if (!configfile.exists()) throw new SystemError("Missing plugin configuration file: plugin.[ini|conf|config]");  

        // Read the configuration file for the plugin
        HashMap<String,Object> metadata=null;
        try {
             ArrayList<String> iniFile=readFileContents(configfile.getAbsolutePath());
             metadata=parseINIfile(iniFile);
             if (!metadata.containsKey("documentation")) { // if no explicit documentation property is provided, but the plugin directory contains a "docs/index.html" file, then add this file as the documentation URL
                 File documentationFile=new File(plugindir,"docs/index.html");
                 if (documentationFile.exists()) {
                     String url=documentationFile.toURI().toURL().toExternalForm();
                     metadata.put("documentation",url);
                 }
             }
        } catch (Exception e) {
             throw new SystemError("Plugin meta data file error: "+e.toString());      
        }
        if (!metadata.containsKey("name")) throw new SystemError("Plugin configuration file is missing the required property \"name\"");            
        metadata.put("pluginDirectory",plugindir.getAbsolutePath()); // set plugin-dir as part of the metadata!
        return metadata;
    }
    
    public HashMap<String,Object> readPluginMetaDataFromZIP(File zipFile) throws SystemError {
        InputStream configFileStream=null;
        HashMap<String,Object> metadata=null;         
        try {
             ZipFile zip = new ZipFile(zipFile);
             for (Enumeration e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                     if (entry.getName().equals("plugin.ini")) {configFileStream=zip.getInputStream(entry);break;}
                else if (entry.getName().equals("plugin.conf")) {configFileStream=zip.getInputStream(entry);break;}
                else if (entry.getName().equals("plugin.config")) {configFileStream=zip.getInputStream(entry);break;}
             }        
             if (configFileStream==null) throw new SystemError("Missing plugin configuration file: plugin.[ini|conf|config]"); 
             ArrayList<String> iniFile=readStreamContents(configFileStream);
             metadata=parseINIfile(iniFile);
        } catch (SystemError se) {
             throw se;               
        } catch (ZipException e) {
             throw new SystemError("Invalid Plugin file");                
        } catch (IOException e) {
             throw new SystemError("Plugin meta data file error: "+e.getMessage());                
        } catch (Exception e) {
             throw new SystemError("Meta data error: "+e.toString());                
        } finally {
            if (configFileStream!=null) try {configFileStream.close();} catch (Exception ce){}
        }
        if (!metadata.containsKey("name")) throw new SystemError("Plugin configuration file is missing the required property \"name\"");            
        return metadata;
    }    
    
    /** Searches through all JAR files in the given plugin directory and returns an object instance of the first Plugin class found
     * 
     * @param plugindir The directory containing the plugin files
     * @return An instance of the plugin (this will have been created not have been initialized)
     * @throws SystemError If the plugin could not be instantiated
     */
    public Plugin instantiatePluginFromDirectory(File plugindir) throws SystemError {    
        // search the JAR-files to locate the actual plugin class
        File[] jarFiles=plugindir.listFiles(new FilenameFilter() {public boolean accept(File dir, String name) { return name.endsWith(".jar");}});      
        if (jarFiles==null || jarFiles.length==0) throw new SystemError("No JAR in plugin file");
        Plugin plugin=null;        
        for (File file:jarFiles) {            
            try {
               Class pluginclass=loadClassesFromJar(file);
               if (pluginclass==null) throw new SystemError("Missing plugin class");
               plugin=(Plugin)pluginclass.newInstance();
               return plugin;
            } catch (SystemError e) {
                throw e;
            } catch (Exception e) {
                throw new SystemError(e.getMessage());
            } 
        }
        if (plugin==null) throw new SystemError("Unable to instantiate plugin");
        return plugin;
    }
    
    /** Returns the plugin classloader associated with the given plugin directory */
    public ClassLoader getPluginClassLoader(File pluginDir) {
        if (pluginClassLoaders==null) return null;
        ClassLoader pluginloader=null;
        String pluginDirString=pluginDir.toString();
        for (URLClassLoader loader:pluginClassLoaders.keySet()) {
            URL[] urls=loader.getURLs();
            for (URL url:urls) {
                String urlString=url.toString();
                if (urlString.startsWith("jar:")) urlString=urlString.substring("jar:".length());
                if (urlString.startsWith("file:")) urlString=urlString.substring("file:".length());
                boolean match=urlString.startsWith(pluginDirString);              
                if (match) {pluginloader=loader;break;}
            }
        }
        return pluginloader;
    }
    
    /** Returns the plugin classloader associated with the given plugin class (or associated class) */
    public ClassLoader getPluginClassLoaderFromClassName(String pluginClassName) {
        if (pluginClassLoaders==null) return null;
        for (URLClassLoader loader:pluginClassLoaders.keySet()) {
            try {
                Class clazz=Class.forName(pluginClassName, false, loader);
                if (clazz!=null) return loader;                
            } catch (ClassNotFoundException e) {}
        }
        return null;
    }   
    
    /** Returns a class associated with a plugin (and respective ClassLoader), if such a class with the given name exists */
    public Class getPluginClassForName(String className) {
        if (pluginClassLoaders==null) return null;
        for (URLClassLoader loader:pluginClassLoaders.keySet()) {
            try {
                Class clazz=Class.forName(className, false, loader);
                if (clazz!=null) return clazz;                
            } catch (ClassNotFoundException e) {}
        }
        return null;        
    }
    
    /**
     * Registers a new plugin with the Engine. The plugin object must have been created but does not have to be "initialized" at this point
     * @param plugin The plugin object itself
     * @param metadata A map containing metadata for the plugin. 
     *                 This could include, e.g. type, version, author, contact, description etc.
     */    
    public void registerPlugin(Plugin plugin, HashMap<String,Object> metadata) {
        if (plugins==null) plugins=new HashMap<String, HashMap<String,Object>>();
        metadata.put("_plugin",plugin);
        plugins.put(plugin.getPluginName(), metadata);           
    }
    
    /** 
     * Uninstalls the plugin and removes its files from the system so that it will not be loaded the next time.
     * @returns TRUE if the plugin's ClassLoader was successfully discarded and all the plugin files were deleted, or FALSE if some of the files could not be deleted right away (they will hopefully be deleted when the VM exits).
     */
    public boolean uninstallPlugin(Plugin plugin) throws SystemError {
        ClassLoader classLoader=plugin.getClass().getClassLoader();
        Object dir=getPluginProperty(plugin.getPluginName(), "pluginDirectory");
        if (dir==null) throw new SystemError("Unable to determine location of installed plugin");
        String pluginDir=dir.toString();
        plugins.remove(plugin.getPluginName()); // unregister the plugin with the engine
        plugin.uninstallPlugin(this); // signal to the plugin to clean up after itself and detach from the engine and client
        plugin=null; // release the reference
        if (classLoader instanceof URLClassLoader) { // Note: Removing the classes completely could cause problems if they still are referenced somewhere. Is it really necessary?
            try {
                ((URLClassLoader)classLoader).close(); // necessary to release the file lock on the JAR-files
                classLoader=null;
            } catch (IOException io) {} 
        }
        System.gc(); // this is necessary in order to garbage collect the ClassLoader (which is necessary in order to delete the JAR-file). However, it is not guaranteed to work...     
        File directory=new File(pluginDir);
        boolean ok=deleteTempFile(directory); // delete the plugin files right away
        if (!ok) deleteOnExit(directory); // If that fails (maybe because of locked files), mark the files for deletion when the VM exits
        return ok;
    }    
    
    /** Returns all plugin objects that have been registered */
    public Plugin[] getPlugins() {
        if (plugins==null) return new Plugin[0];
        else {
            ArrayList<Plugin> allPlugins=new ArrayList<Plugin>();
            for (String name:plugins.keySet()) {
                Plugin plugin=getPlugin(name);
                if (plugin!=null) allPlugins.add(plugin);
            }          
            Plugin[] set=new Plugin[allPlugins.size()];
            set=allPlugins.toArray(set);
            return set;
        }
    }
    
    /**
     * @param pluginname
     * @return The plugin with the given name (or NULL if no such plugin exists)
     */
    public Plugin getPlugin(String pluginname) {
        if (plugins==null) return null;
        else {
            HashMap<String,Object> map=plugins.get(pluginname);
            if (map==null) return null;
            if (map.get("_plugin") instanceof Plugin) return (Plugin)map.get("_plugin");
            else return null;
        }
    }
    
    /**
     * Returns a map containing metadata (in key=value pairs) for the plugin 
     * with the given name (or NULL) if no plugin with that name is registered.
     * Common metadata entries include "type", "version", "author", "version",
     * "description" and "documentation". The special key "_plugin" will return
     * a reference to the plugin object itself.
     * @param pluginName
     * @return 
     */
    public HashMap<String,Object> getPluginMetaData(String pluginName) {
        if (plugins==null) return null;
        else return plugins.get(pluginName);
    }
    
    /** Returns the value for the given property of the named plugin
     *  @returns a plugin property value or NULL if the plugin does not exist or does not have a value for the requested property
     */
    public Object getPluginProperty(String pluginName, String propertyName) {
        if (plugins==null) return null;
        HashMap<String,Object> meta=getPluginMetaData(pluginName);
        if (meta==null) return null;
        return meta.get(propertyName);
    }    
    
    /**
     * Registers a new DataFormat from the given class
     * If a list of data types (classes) are given, the new DataFormat will be registered as default output
     * format for these data types
     * @param dataformatClass
     * @param useasdefaultfordatatypes
     */
    public void registerDataFormat(Class dataformatClass, Class[] useasdefaultfordatatypes) {
        try {
            DataFormat newformat=(DataFormat)dataformatClass.newInstance();
            newformat.setEngine(this);        
            dataformatslist.put(newformat.getName(), newformat); 
            if (useasdefaultfordatatypes!=null) {
                for (Class dataclass:useasdefaultfordatatypes) {
                    setDefaultDataFormat(dataclass,newformat.getName());
                }
            }   
        } catch (Exception e) {           
            errorMessage("Unable to register dataformat '"+dataformatClass+"' => "+e.toString(), 0);
            e.printStackTrace(System.err);
        }
    }
    
    public void registerDataFormat(DataFormat newformat) {
        newformat.setEngine(this);        
        dataformatslist.put(newformat.getName(), newformat); 
    }    
    
    public void unregisterDataFormat(DataFormat format) {
        format.setEngine(null);
        dataformatslist.remove(format.getName());
    }
    
        
    /** Registers the analysis template with the engine */
    public void registerAnalysis(Analysis analysis) {
        analyses.put(analysis.getAnalysisName(), analysis);
        registerReservedWord(analysis.getAnalysisName());
    }   
    
    public void unregisterAnalysis(Analysis analysis) {
        analyses.remove(analysis.getAnalysisName());
        unregisterReservedWord(analysis.getAnalysisName());
    }    
    
    /** Returns the class for the analysis with the given name*/
    public Class getClassForAnalysis(String analysisname) {
       Analysis analysis=analyses.get(analysisname);
       if (analysis==null) return null;
       else return analysis.getClass();
    }
    
    /** Returns a template Analysis object corresponding to the given class (if recognized, else NULL)  */
    public Analysis getAnalysisForClass(Class analysisclass) {
       if (analysisclass==null) return null;
       for (Analysis analysis:analyses.values()) {
           if (analysis.getClass()==analysisclass) return analysis;
       }
       return null;
    }
    
    /** Registers the external program with the engine */
    public void registerExternalProgram(ExternalProgram program) {
        program.setEngine(this);
        externalPrograms.put(program.getName(),program);      
    }  
    
    /** Unregisters the external program with the engine */
    public void unregisterExternalProgram(ExternalProgram program) {
        program.setEngine(null);
        externalPrograms.remove(program.getName());      
    }       
    
    /** Registers a new type of data repository with the engine. The provided repository only serves as a class template and can be blank (but not null) */
    public void registerDataRepositoryType(DataRepository repository) {
        if (datarepositoryTypes==null) datarepositoryTypes=new HashMap<String, Class>();
        String typename=repository.getRepositoryType();
        datarepositoryTypes.put(typename,repository.getClass());
    }    
    
    public void unregisterDataRepositoryType(String typename) {
        if (datarepositories!=null) { // first remove all current instances of the repository type
            for (DataRepository rep:getDataRepositories()) {
                if (rep.getRepositoryType().equals(typename)) unregisterDataRepository(rep);
            }
        }
        // now remove the repository type itself
        datarepositoryTypes.remove(typename);
    } 
    
    /** Returns a list of names of the different repository types registered with the engine */
    public String[] getDataRepositoryTypes() {
        if (datarepositoryTypes==null || datarepositoryTypes.isEmpty()) return new String[0];
        String[] list=new String[datarepositoryTypes.size()];
        int i=0;
        for (String name:datarepositoryTypes.keySet()) {
            list[i]=name;
            i++;
        }
        Arrays.sort(list);
        return list;
    }

    /** Returns the Class for the data repository type with the given type name */
    public Class getDataRepositoryType(String name) {
        if (datarepositoryTypes==null) return null;
        else return datarepositoryTypes.get(name);
    }       
    
    /** Returns the Class for the data repository type with the given class name */
    public Class getDataRepositoryTypeFromClassName(String classname) {
        if (datarepositoryTypes==null) return null;
        for (Class repclass:datarepositoryTypes.values()) {
            if (repclass.getName().equals(classname)) return repclass;
        }
        return null;
    }     
    
    
    /** Adds a new data repository instance to the engine */
    public void registerDataRepository(DataRepository repository) {
        if (datarepositories==null) datarepositories=new HashMap<String, DataRepository>();
        String name=repository.getRepositoryName();
        datarepositories.put(name,repository);
    }
    
    /** Removes a specific data repository instance from the engine */    
    public void unregisterDataRepository(DataRepository repository) {
        if (datarepositories==null || repository==null) return;
        String name=repository.getRepositoryName();
        datarepositories.remove(name);
    } 
     
    
    public DataRepository getDataRepository(String name) {
        if (datarepositories==null) return null;
        else return datarepositories.get(name);
    }
    
    public String[] getDataRepositoryNames() {
        if (datarepositories==null) return new String[0];
        Set<String> names=datarepositories.keySet();
        String[] result=new String[names.size()];
        result=names.toArray(result);
        Arrays.sort(result);
        return result;
    }
    
    public DataRepository[] getDataRepositories() {
        if (datarepositories==null) return new DataRepository[0];
        Collection<DataRepository> reps=datarepositories.values();
        DataRepository[] result=new DataRepository[reps.size()];
        result=reps.toArray(result);
        return result;
    }    
    
    
    
    /** Removes the external program with the given names from the internal list
     *  The removed program is returned (but note that this is no longer installed!)
     *  Note that the actual (executable) files related to the program are not deleted.
     *  The program is only removed from the list of available programs.
     */
    public ExternalProgram removeExternalProgram(String programName, boolean deleteConfig) {
        ExternalProgram p=externalPrograms.remove(programName);
        if (p!=null && deleteConfig) {
            String configfilename=p.getConfigurationFile();
            File configFile=new File(configfilename);
            if (!configFile.delete()) logMessage("Unable to remove configuration file for '"+programName+"' : "+configfilename);
        }
        if (p!=null) p.setEngine(null);
        return p;
    }    
    
    /** Reads a configuration file (actually a serialized object) describing the
     *  predefined MotifCollections known to the system
     */
    @SuppressWarnings("unchecked")
    private void importPredefinedMotifCollections() {
        logMessage("Importing Motif Collections");
        File configfile=new File(getMotifLabDirectory()+File.separator+"predefinedMotifCollectionsConfig.txt");
        if (configfile.exists()) {
            try {
               predefinedMotifCollections=loadPredefinedMotifCollectionsConfiguration();
            } catch (Exception e) {errorMessage("Unable to load predefined motif collections: "+e.toString(),0);}
        } else {
            File motifcollectiondir=new File(getMotifLabDirectory()+java.io.File.separator+MotifCollectionDirectory);
            if (!motifcollectiondir.exists()) {
               boolean ok=motifcollectiondir.mkdirs();
               if (!ok) errorMessage("Unable to create directory for predefined motif collections", 0);  
            }
        }        

    }
    
    /** 
     * Returns the name of the file which stores the data for the given motif collection
     * (in MotifLabMotif format)
     */
    public String getFilenameForMotifCollection(String motifCollectionName) {
        Object[] info=predefinedMotifCollections.get(motifCollectionName);
        if (info==null) return null;
        else return getMotifLabDirectory()+File.separator+MotifCollectionDirectory+File.separator+(String)info[0];
    }
     /** 
     * Returns the size of the predefined motif collection with the given name
     * (in MotifLabMotif format)
     */
    public int getSizeForMotifCollection(String motifCollectionName) {
        Object[] info=predefinedMotifCollections.get(motifCollectionName);
        if (info==null) return 0;
        else return (Integer)info[1];
    }
    
    /** Returns a list (or rather a Set) of the names of predefined motif collections known to the engine */
    public Set<String> getPredefinedMotifCollections() {
        return predefinedMotifCollections.keySet();               
    }
    
    /** 
     * Registers the given MotifCollection as part of the set of predefined collections 
     * This will save the collection in the working directory (replacing any previous collections
     * with the same name) and also update the 'registry' of predefined collections
     * @param collection The MotifCollection to be registered
     * @return TRUE if registration was succesful
     */
    public void registerPredefinedMotifCollection(MotifCollection collection) throws Exception {
        String name=collection.getPredefinedCollectionName();
        if (name==null || name.isEmpty()) name=collection.getName();
        String filename=name.replace(" ", "_"); // just in case
        DataFormat format=getDataFormat("MotifLabMotif");
        filename+=("."+format.getSuffix());
        int collectionsize=collection.size();
        String fullpath=getMotifLabDirectory()+File.separator+MotifCollectionDirectory+File.separator+filename;      
        if (format==null) throw new SystemError("Unknown dataformat: MotifLabMotif");
        saveDataToFile(collection, format, fullpath);
        Object[] oldentry=predefinedMotifCollections.get(name);
        predefinedMotifCollections.put(name, new Object[]{filename,new Integer(collectionsize)});
        try {
            collection.setPredefinedCollectionName(name);
            savePredefinedMotifCollectionsConfiguration();
        } catch (Exception e) {
           if (oldentry==null) predefinedMotifCollections.remove(name);
           else predefinedMotifCollections.put(name,oldentry); // restore old entry if something went wrong during save
           throw e;
        } 
     }

     /**
      * Registers a predefined MotifCollection which is read from a stream (in MotifLabFormat)
      * The collection is saved locally beneath the work directory
      * @param stream
      * @param filename The filename of the file where the collection should be saved (just a simple name, not the full path)
      * @param collectionName if not null, the collection will be given this name, else the name will be taken either from the '#Collection = name' line in the stream (if found) or from the given filename
      * @throws java.lang.Exception
      */
     public void registerPredefinedMotifCollectionFromStream(InputStream stream, String filename, String collectionName) throws Exception {
        String name=(collectionName!=null)?collectionName:filename;
        String fullpath=getMotifLabDirectory()+File.separator+MotifCollectionDirectory+File.separator+filename;
        File outputfile=new File(fullpath);
        BufferedReader inputStream=null;
        BufferedWriter outputStream=null;
        int collectionsize=0;
        try {
            inputStream=new BufferedReader(new InputStreamReader(stream));
            outputStream=new BufferedWriter(new FileWriter(outputfile));
            String line;
            while((line=inputStream.readLine())!=null) {
                if (line.startsWith("#ID")) collectionsize++;
                else if (line.startsWith("#Collection")) {
                    String[] parts=line.split("=");
                    if (collectionName==null) name=parts[parts.length-1].trim();
                    else name=collectionName;
                    line="#Collection = "+name;
                }
                outputStream.write(line);
                outputStream.newLine();
            }
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
                if (outputStream!=null) outputStream.close();
            } catch (IOException ioe) {}
        }        
        Object[] oldentry=predefinedMotifCollections.get(name);
        predefinedMotifCollections.put(name, new Object[]{filename,new Integer(collectionsize)});
        try {
            savePredefinedMotifCollectionsConfiguration();
        } catch (Exception e) {
           if (oldentry==null) predefinedMotifCollections.remove(name);
           else predefinedMotifCollections.put(name,oldentry);
           throw e;
        } 
    }
   
    /** Returns TRUE if this is the first time the user starts MotifLab.
     *  The first time MotifLab is started, it must create a system directory for MotifLab and install several configuration files into that directory.
     *  If a user has run MotifLab before, it is indicated by the existence of a file called "installed.txt" inside this MotifLab system directory (the file itself is empty)
     * 
     */
    public boolean isFirstTimeStartup() {
        File test=new File(getMotifLabDirectory()+File.separator+"installed.txt");
        return !test.exists(); // the file above is created on first startup. If it does not exists yet it means this is the first time
    }     

    /** Creates an indicator file in the workdir which signals that installation has been completed
     *  This method should only be called as a final step during installation procedure
     */
    public boolean raiseInstalledFlag() {
        File test=new File(getMotifLabDirectory()+File.separator+"installed.txt");
        try {
            return test.createNewFile();
        } catch (Exception e) {return false;}
    }

    /**
     * Saves the current configuration of predefined motif collections back to the standard config file
     * @throws Exception
     */
    public void savePredefinedMotifCollectionsConfiguration() throws Exception {
           File configfile=new File(getMotifLabDirectory()+File.separator+"predefinedMotifCollectionsConfig.txt");
           BufferedWriter filewriter = new BufferedWriter(new FileWriter(configfile));
           for (String name:predefinedMotifCollections.keySet()) {
               Object[] entry=predefinedMotifCollections.get(name);
               String filename=(String)entry[0];
               int size=(Integer)entry[1];
               filewriter.write(name+"\t"+size+"\t"+filename+"\n");               
           }
           filewriter.close();
           if (!configfile.exists()) throw new ExecutionError("Unable to save config file for predefined motif collections");
    }

    /** Loads information about predefined motif collections from the standard config file and returns this information in the form of a hashmap
     *  @return A hashmap where the key is the name of a collection and the Object[] value contains the filename for this collection and its size
     */
    public HashMap<String, Object[]> loadPredefinedMotifCollectionsConfiguration() throws Exception {
           File configfile=new File(getMotifLabDirectory()+File.separator+"predefinedMotifCollectionsConfig.txt");
           if (!configfile.exists()) return null;
           HashMap<String,Object[]> config = new HashMap<String,Object[]>();
           BufferedReader filereader = new BufferedReader(new FileReader(configfile));
           String line;
           while ((line=filereader.readLine())!=null) {
               if (line.trim().isEmpty() || line.startsWith("#")) continue;
               String[] split=line.split("\t");
               if (split.length!=3) throw new SystemError("Unrecognized line format in config file for predefined motif collections. Expected 3 fields but found "+split.length);
               String collectionName=split[0];
               String filename=split[2];
               int size=0;
               try {
                   size=Integer.parseInt(split[1]);
               } catch (NumberFormatException e) {
                  filereader.close();
                  throw new SystemError("Unable to parse expected numerical value in config file for predefined motif collections. Value = '"+split[1]+"'"); 
               }
               config.put(collectionName, new Object[]{filename,new Integer(size)});
           }
           filereader.close();
           return config;   
    }

     
     
    /** Saves the specified data object to file in the given dataformat */
    public void saveDataToFile(Data data, DataFormat dataformat, String filename) throws Exception {
        OutputData output=new OutputData("output_"+data.getName());
        if (dataformat!=null && !dataformat.canFormatOutput(data)) throw new SystemError("can not format '"+data.getName()+"' ["+data.getTypeDescription()+"] in format '"+dataformat.getName()+"'");
        dataformat.format(data, output, null, null);
        output.saveToFile(filename,this);        
    }
            
    /** Saves an object to file by serialization */
    public void saveSerializedObject(Object object, String filename) throws Exception {
        ObjectOutputStream stream=null;    
        try {
             File file=getFile(filename);
             OutputStream outputstream=getOutputStreamForFile(file);
             stream = new ObjectOutputStream(new BufferedOutputStream(outputstream));
             stream.writeObject(object);
             stream.close();       
        } catch (Exception e) {
            throw e;
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}        
    }
    
    /** Loads a serialized object from file */
    public Object loadSerializedObject(String filename, boolean throwException) throws Exception {
        ObjectInputStream stream=null;
        try {
             File file=getFile(filename);
             InputStream inputstream=getInputStreamForFile(file);            
             stream=new ObjectInputStream(new BufferedInputStream(inputstream));
             Object value=stream.readObject();
             stream.close();
             return value;  
        } catch (Exception e) {
            if (throwException) throw e;
            else return null;
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}     
    }
    
    /** Loads a serialized object from file. This method will return NULL if something fails rather than throwing an exception (for backwards compatibility) */
    public Object loadSerializedObject(String filename) throws Exception {
        return loadSerializedObject(filename, false);
    }
    
    /** Saves a serialized version of this object to the MotifLab directory 
     *  @param filename This should just be a name and not a full path!
     */
    public void storeSystemObject(Object object, String filename) throws Exception {
        filename=getMotifLabDirectory()+File.separator+filename;
        saveSerializedObject(object, filename);
    }
    
    /** Loads a serialized object from a file in the MotifLab directory
     * 
     *  @param filename This should just be a name and not a full path!
     */
    public Object loadSystemObject(String filename) throws Exception {
        filename=getMotifLabDirectory()+File.separator+filename;
        return loadSerializedObject(filename);
    }   
    
    public Object loadSystemObject(String filename, boolean throwException) throws Exception {
        filename=getMotifLabDirectory()+File.separator+filename;
        return loadSerializedObject(filename,throwException);
    }       
    
    public boolean systemObjectFileExists(String filename) {
        if (filename==null) return false;
        filename=getMotifLabDirectory()+File.separator+filename;
        File file=new File(filename);
        return file.exists();
    }      
    

   /**
    * This method should be called whenever an unexpected (uncaught) exception
    * is encountered during the execution of the program. At the very least,
    * it should be called by the defaultUncaughtExceptionHandler (if exists).
    * The purpose of this method is to file a report to an appropriate bugtracking
    * system so that the issue can be dealt with by the developers and fixed in
    * future releases of MotifLab 
    * @param e The Error or Exception that caused the problem
    */     
   public void reportError(Throwable e) {
       logMessage("Sending bug report!");
       String message=getErrorReport(e);
       //logMessage(message);// write error message to log
       try {
            URL url;
            HttpURLConnection urlConnection;
            DataOutputStream printout;
            BufferedReader reader;
            url = new URL (getWebSiteURL()+"reportbug.php");
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            printout = new DataOutputStream (urlConnection.getOutputStream());
            String content = "message=" + URLEncoder.encode(message,"UTF-8");
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream ()));
            String line;
            while ((line = reader.readLine()) != null) {
                logMessage(line);
            }
            reader.close();
        // logMessage("Bug report successful");
     } catch (Exception ex) {
           logMessage("An error occurred while sending bug report: "+ex.getClass().getCanonicalName()+" : "+ex.getMessage());
     } // nothing to do really   
   }     
        
   protected String getErrorReport(Throwable e) {
       StringBuilder builder=new StringBuilder();       
       builder.append(e.getClass().getCanonicalName());
       builder.append(" : ");
       builder.append(e.getMessage());
       builder.append("    [v");
       builder.append(getVersion());
       builder.append("]");
       Throwable cause=e.getCause();
       if (cause!=null) {
         builder.append("\nCaused by: ");
         builder.append(cause.getClass().getCanonicalName()); 
         builder.append(" : ");
         builder.append(cause.getMessage());         
       }
       builder.append("\n===================================");
       StackTraceElement[] trace=e.getStackTrace();
       for (StackTraceElement el:trace) {
           builder.append("\n");
           builder.append(el.toString());
       }       
       String report=builder.toString();
       report=report.replace('\'', '"'); // The receiving SQL database does not allow single quotes in the report. so replace these with double
       return report;
   }
   
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        // System.err.println("uncaughtException: "+e.toString());e.printStackTrace(System.err);
        if (client!=null && client.handleUncaughtException(e)) {
            // client fixed the problem!
        } else {
            if (client!=null) client.shutdown(); // the client should also call shutdown on the engine
            //System.err.println("uncaughtException:"+e.toString());
            System.exit(1);
        }
    }        
       
/** Deletes the given file or directory (and subdirectories recursively)
 *  @param tempdir
 *  @return TRUE if the target file/directory was successfully deleted (along with all subdirectories) or FALSE if some of the files in the tree could not be deleted
 */
public boolean deleteTempFile(File tempdir) {
    if (tempdir.isDirectory()) { // if file is a directory, delete the children before the parent
        boolean ok=true;
        File[] files=tempdir.listFiles();
        if (files==null) return true;
        for (File file:files) {
            if (file.isDirectory()) ok = ok && deleteTempFile(file);
            else ok = ok && file.delete();
            Thread.yield();
        }
        ok = ok && tempdir.delete();
        return ok;
    } else return tempdir.delete();
}

/** Marks the file or directory so that it will be deleted when the VM exits normally
 *  Unlike the normal file.deleteOnExit() method, this will also work recursively for non-empty directories (where the directory has not been marked for deletion before adding files)
 */
public static void deleteOnExit(File file) {  
    file.deleteOnExit(); // call deleteOnExit for the folder first, so it will get deleted last   
    if (file.isDirectory()) {
        File[] files = file.listFiles();  
        if (files != null)  {  
            for (File f: files)  {  
                if (f.isDirectory()) deleteOnExit(f);  
                else f.deleteOnExit();                       
            }  
        }  
    }
}    
 
/** Performs necessary cleanup (like deleting temporary directories) */
public void shutdown() {
    if (dataLoader!=null) dataLoader.shutdown();
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    File tempdir=new File(getTempDirectory());
    deleteTempFile(tempdir);
}


    public void installBundledResource(String resourcepath, String configFilename) throws SystemError {
        InputStream stream=this.getClass().getResourceAsStream(resourcepath);
        installConfigFileFromStream(configFilename,stream);      
    }

    /** Copies the contents of an inputstream to a file in the workdirectory
     * @param filename The name of the target file (withouth pathprefix. Workdir-path will be added)
     */
    public void installConfigFileFromStream(String filename,InputStream stream) throws SystemError {
        File file=new File(getMotifLabDirectory()+File.separator+filename);
        java.io.BufferedWriter outputStream=null;
        java.io.BufferedReader inputStream=null;
        try {
            inputStream=new java.io.BufferedReader(new java.io.InputStreamReader(stream));
            outputStream=new java.io.BufferedWriter(new java.io.FileWriter(file));
            String line;
            while((line=inputStream.readLine())!=null) {
                outputStream.write(line);
                outputStream.newLine();
            }
            inputStream.close();
            outputStream.close();
            stream.close();
        } catch (Exception e) {
            throw new SystemError(e.getClass().getSimpleName()+":"+e.getMessage());
        } finally {
           try {
               if (inputStream!=null) inputStream.close();
               if (outputStream!=null) outputStream.close();
               if (stream!=null) stream.close();
           } catch (Exception ne) {}
        }
    }


/**
 * Creates and returns a new dataobject of the given type (with empty/default content)
 * @param type
 * @param tempName
 * @return
 * @throws ExecutionError
 */
public Data createDataObject(Class type, String tempName) throws ExecutionError {
         if (type==RegionDataset.class) return new RegionDataset(tempName);
    else if (type==NumericDataset.class)  return new NumericDataset(tempName);
    else if (type==DNASequenceDataset.class)  return new DNASequenceDataset(tempName);
    else if (type==MotifCollection.class) return new MotifCollection(tempName);
    else if (type==MotifPartition.class) return new MotifPartition(tempName);
    else if (type==ModuleCollection.class) return new ModuleCollection(tempName);
    else if (type==ModulePartition.class) return new ModulePartition(tempName);
    else if (type==Motif.class) return new Motif(tempName);
    else if (type==Module.class) return new Module(tempName);
    else if (type==TextVariable.class) return new TextVariable(tempName);
    else if (type==NumericVariable.class) return new NumericVariable(tempName,0);
    else if (type==GeneralNumericMap.class) return new GeneralNumericMap(tempName,0);    
    else if (type==SequenceNumericMap.class) return new SequenceNumericMap(tempName,0);
    else if (type==MotifNumericMap.class) return new MotifNumericMap(tempName,0);
    else if (type==ModuleNumericMap.class) return new ModuleNumericMap(tempName,0);   
    else if (type==SequenceTextMap.class) return new SequenceTextMap(tempName,"");
    else if (type==MotifTextMap.class) return new MotifTextMap(tempName,"");
    else if (type==ModuleTextMap.class) return new ModuleTextMap(tempName,"");     
    else if (type==BackgroundModel.class) return new BackgroundModel(tempName);
    else if (type==SequenceCollection.class) return new SequenceCollection(tempName);
    else if (type==SequencePartition.class) return new SequencePartition(tempName);
    else if (type==ExpressionProfile.class) return new ExpressionProfile(tempName);
    else if (type==OutputData.class) return new OutputData(); 
    else if (type==PriorsGenerator.class) return new PriorsGenerator(tempName);           
    throw new ExecutionError("Results data object not created (type="+type.getSimpleName()+")");
}
/**
 * This method returns a Class corresponding to a classtype given by a string.
 * This classtype name is not the same as the one returned by X.getType() where X
 * is a subclass of Data (it is nearly similar except that spaces in the name is removed)
 * This method is only used by ExternalProgram to convert class-strings defined in XML-configuration
 * documents to Classes
 * @param classname
 * @return
 * @throws ClassNotFoundException
 */
public static Class getClassForName(String classname) throws ClassNotFoundException {
         if (classname.equalsIgnoreCase("RegionDataset") || classname.equalsIgnoreCase(RegionDataset.getType())) return RegionDataset.class;
    else if (classname.equalsIgnoreCase("NumericDataset") || classname.equalsIgnoreCase(NumericDataset.getType()))  return NumericDataset.class;
    else if (classname.equalsIgnoreCase("DNASequenceDataset") || classname.equalsIgnoreCase(DNASequenceDataset.getType()))  return DNASequenceDataset.class;
    else if (classname.equalsIgnoreCase("MotifCollection") || classname.equalsIgnoreCase(MotifCollection.getType())) return MotifCollection.class;
    else if (classname.equalsIgnoreCase("MotifPartition") || classname.equalsIgnoreCase(MotifPartition.getType())) return MotifPartition.class;
    else if (classname.equalsIgnoreCase("ModuleCollection") || classname.equalsIgnoreCase(ModuleCollection.getType())) return ModuleCollection.class;
    else if (classname.equalsIgnoreCase("ModulePartition") || classname.equalsIgnoreCase(ModulePartition.getType())) return ModulePartition.class;
    else if (classname.equalsIgnoreCase("Motif")) return Motif.class;
    else if (classname.equalsIgnoreCase("Module")) return Module.class;
    else if (classname.equalsIgnoreCase("TextVariable") || classname.equalsIgnoreCase(TextVariable.getType())) return TextVariable.class;
    else if (classname.equalsIgnoreCase("NumericVariable") || classname.equalsIgnoreCase(NumericVariable.getType())) return NumericVariable.class;
    else if (classname.equalsIgnoreCase("NumericMap") || classname.equalsIgnoreCase("Numeric Map")) return GeneralNumericMap.class;
    else if (classname.equalsIgnoreCase("SequenceNumericMap") || classname.equalsIgnoreCase(SequenceNumericMap.getType())) return SequenceNumericMap.class;
    else if (classname.equalsIgnoreCase("MotifNumericMap") || classname.equalsIgnoreCase(MotifNumericMap.getType())) return MotifNumericMap.class;
    else if (classname.equalsIgnoreCase("ModuleNumericMap") || classname.equalsIgnoreCase(ModuleNumericMap.getType())) return ModuleNumericMap.class;   
    // else if (classname.equalsIgnoreCase("Map") || classname.equalsIgnoreCase("TextMap") || classname.equalsIgnoreCase("Text Map")) return GeneralNumericMap.class;
    else if (classname.equalsIgnoreCase("SequenceMap") || classname.equalsIgnoreCase("SequenceTextMap") || classname.equalsIgnoreCase(SequenceTextMap.getType())) return SequenceTextMap.class;
    else if (classname.equalsIgnoreCase("MotifMap") || classname.equalsIgnoreCase("MotifTextMap") || classname.equalsIgnoreCase(MotifTextMap.getType())) return MotifTextMap.class;
    else if (classname.equalsIgnoreCase("ModuleMap") || classname.equalsIgnoreCase("ModuleTextMap") || classname.equalsIgnoreCase(ModuleTextMap.getType())) return ModuleTextMap.class;   
    else if (classname.equalsIgnoreCase("BackgroundModel") || classname.equalsIgnoreCase(BackgroundModel.getType())) return BackgroundModel.class;
    else if (classname.equalsIgnoreCase("String")) return String.class;
    else if (classname.equalsIgnoreCase("Integer")) return Integer.class;
    else if (classname.equalsIgnoreCase("Double")) return Double.class;
    else if (classname.equalsIgnoreCase("Float")) return Double.class; // included for backwards compatibility but Double is now the prefered type for numeric values
    else if (classname.equalsIgnoreCase("Boolean")) return Boolean.class;
    else if (classname.equalsIgnoreCase("SequenceCollection") || classname.equalsIgnoreCase(SequenceCollection.getType())) return SequenceCollection.class;
    else if (classname.equalsIgnoreCase("SequencePartition") || classname.equalsIgnoreCase(SequencePartition.getType())) return SequencePartition.class;
    else if (classname.equalsIgnoreCase("ExpressionProfile") || classname.equalsIgnoreCase(ExpressionProfile.getType())) return ExpressionProfile.class;
    else if (classname.equalsIgnoreCase("Output")) return OutputData.class;    
    throw new ClassNotFoundException(classname);
}


   /** Given a DNASequenceDataset this method will return a Sequence Collection containing the 
    *  underlying sequences
    *  @param dataset
    *  @param result The SequenceCollection to which the sequences will be added to the payload. If this is NULL a new collection is created and returned
    *
    */
   public SequenceCollection extractSequencesFromFeatureDataset(DNASequenceDataset dataset, SequenceCollection result) {
        if (result==null) result=new SequenceCollection("temporary");
        for (int i=0;i<dataset.getSize();i++) {
            DNASequenceData dnaseq=(DNASequenceData)dataset.getSequenceByIndex(i);           
            int organism=(dnaseq.getTemporaryOrganism()==null)?0:dnaseq.getTemporaryOrganism().intValue();
            Sequence seq=new Sequence(dnaseq.getName(), organism, dnaseq.getTemporaryBuild(), dnaseq.getChromosome(), dnaseq.getRegionStart(), dnaseq.getRegionEnd(), dnaseq.getGeneName(), dnaseq.getTSS(), dnaseq.getTES(), dnaseq.getStrandOrientation());
            result.addSequenceToPayload(seq);
        }
        return result;
   }


    /** Creates a header for an HTML-document up to and including the BODY tag and appends it to the
     *  OutputData object (which should be empty). Any dependencies (javascript/CSS) are handled according
     *  to current settings.
     *  @param extraHeader extra code which will be included within the HEAD tab
     *  @param internalCSS a string containing CSS code in text/css format which will be appended within the documents STYLE tag
     *  @param includeJavascript if TRUE the standard javascript module for MotifLab will be included somehow (either embedded or referenced depending on current settings)
     *  @param includeCSS if TRUE the current stylesheet will be included somehow (either embedded or referenced depending on current settings)
     */
    public void createHTMLheader(String title, String extraHeader, String internalCSS, boolean includeJavascript, boolean includeCSS, boolean includeDefaultCSS, OutputData outputdata) {
        VisualizationSettings settings=getClient().getVisualizationSettings();
        String cssSetting=settings.getCSSSetting();
        String stylesheet=settings.getStylesheetSetting();
        String javascriptSetting=settings.getJavascriptSetting();
        StringBuilder builder=new StringBuilder();
        builder.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">");
        builder.append("<html>\n");
        builder.append("<head>\n");
        builder.append("<title>");
        builder.append(title);
        builder.append("</title>\n");
        if (includeJavascript && !javascriptSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_NONE)) {
           if (javascriptSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_LINK)) {
               builder.append("<script type=\"text/javascript\" src=\"http://www.motiflab.org/motiflab.js\"></script>\n"); //
           } else {
               String scriptCode=null;
               try {scriptCode=MotifLabEngine.readTextFile(getMotifLabDirectory()+java.io.File.separator+"motiflab.js");} catch (IOException e) {System.err.println("Error while reading 'motiflab.js':"+e.toString());}
               if (scriptCode!=null) { // the Javascript code could be read successfully
                    if (javascriptSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_EMBED)) {
                       builder.append("<script type=\"text/javascript\">\n");
                       builder.append(scriptCode);
                       builder.append("\n</script>\n");
                   } else { // save to new or shared file -> create dependency
                        String sharedID=null;
                        if (javascriptSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_SHARED_FILE)) sharedID="motiflab_script";
                        String filename=outputdata.createTextfileDependency(this, scriptCode, sharedID, "js");
                        builder.append("<script type=\"text/javascript\" src=\""+filename+"\"></script>\n");
                   }
               } else builder.append("<script type=\"text/javascript\" src=\"http://www.motiflab.org/motiflab.js\"></script>\n"); // default if script could not be read
           }

        }
        builder.append("<style type=\"text/css\">\n");
        if (includeDefaultCSS) appendDefaultCSSstyle(builder);
        if (internalCSS!=null) builder.append(internalCSS);
        builder.append("</style>\n");
        if (includeCSS && !cssSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_NONE)) {
           String styleCode=null;
           String stylesheetfilename=stylesheet;
           if (stylesheet.startsWith("[") && stylesheet.endsWith("]")) { // system installed stylesheet
              stylesheetfilename=getMotifLabDirectory()+java.io.File.separator+stylesheet.substring(1,stylesheet.length()-1)+".css";
           }
           try {styleCode=MotifLabEngine.readTextFile(stylesheetfilename);} catch (IOException e) {System.err.println("Error while reading stylesheet '"+stylesheetfilename+"':"+e.toString());}

           if (cssSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_LINK)) {
              stylesheetfilename=getWebSiteURL()+stylesheet.substring(1,stylesheet.length()-1)+".css";
              builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+stylesheetfilename+"\" />\n");
           } else if (cssSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_EMBED)) {
              builder.append("<style type=\"text/css\">\n");
              builder.append(styleCode);
              builder.append("\n</style>\n");
           } else { // save to new or shared file -> create dependency
                String sharedID=null;
                if (cssSetting.equalsIgnoreCase(VisualizationSettings.HTML_SETTING_SHARED_FILE)) sharedID="motiflab_style";
                String filename=outputdata.createTextfileDependency(this, styleCode, sharedID, "css");
                builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\""+filename+"\" />\n");
           }
            // if no stylesheet could be located and !includeDefaultCSS appendDefaultCSSstyle(builder); // include default anyway
        }

        if (extraHeader!=null) builder.append(extraHeader);
        builder.append("</head>\n<body>\n");
        outputdata.append(builder.toString(), Analysis.HTML);
        outputdata.setShowAsHTML(true);        
    }

    /** Creates a HTML-header for a document which is never exported or saved externally
     *  but only shown within the MotifLab GUI, such as prompts displaying Analyses
     */
    public void createInternalHTMLheader(OutputData outputdata) {
        createHTMLheader("", null, null, false, false, true, outputdata);
    }

    /** Appends default CSS stylesheet to use for HTML pages to the StringBuilder. This CSS is in text/css format and must be enclosed in an HTML STYLE-element */
    public void appendDefaultCSSstyle(StringBuilder builder) {
        VisualizationSettings settings=getClient().getVisualizationSettings();
        builder.append("table {border-collapse:collapse;}\n");
        builder.append("td {border-width:1px; border-style:solid; border-color:#000000; padding:3px; border-collapse:collapse;}\n");
        builder.append("th {border-width: 1px;border-style:solid; border-color:#000000; padding:3px; border-collapse:collapse; background-color:#CCCCCC;}\n");
        builder.append(".dataitem {font-weight:bold;}\n");
        builder.append("td.verysignificant {background-color:");
        builder.append(settings.getSystemColorAsHTML("verysignificant"));
        builder.append(";text-align:right;}\n");
        builder.append("td.significant {background-color:");
        builder.append(settings.getSystemColorAsHTML("significant"));
        builder.append(";text-align:right;}\n");
        builder.append("td.onlyintarget {background-color:");
        builder.append(settings.getSystemColorAsHTML("onlyintarget"));
        builder.append(";text-align:right;}\n");
        builder.append("td.overrepintarget {background-color:");
        builder.append(settings.getSystemColorAsHTML("overrepintarget"));
        builder.append(";text-align:right;}\n");
        builder.append("td.intarget {background-color:");
        builder.append(settings.getSystemColorAsHTML("intarget"));
        builder.append(";text-align:right;}\n");
        builder.append("td.samerate {background-color:");
        builder.append(settings.getSystemColorAsHTML("samerate"));
        builder.append(";text-align:right;}\n");
        builder.append("td.onlyincontrol {background-color:");
        builder.append(settings.getSystemColorAsHTML("onlyincontrol"));
        builder.append(";text-align:right;}\n");
        builder.append("td.overrepincontrol {background-color:");
        builder.append(settings.getSystemColorAsHTML("overrepincontrol"));
        builder.append(";text-align:right;}\n");
        builder.append("td.incontrol {background-color:");
        builder.append(settings.getSystemColorAsHTML("incontrol"));
        builder.append(";text-align:right;}\n");
        builder.append("td.notpresent {background-color:");
        builder.append(settings.getSystemColorAsHTML("notpresent"));
        builder.append(";text-align:right;}\n");
        builder.append("td.num {text-align:right;}\n");
        builder.append("td.namecolumn {font-weight: bold;}\n");
    }

    
    /** Runs a startup configuration protocol containing default display settings and other things */
    public void executeStartupConfigurationProtocol() {
        // initialize visualizationSettings with some default settings from protocol-file (if available)   
        try {
            File vizConfigFile=new File(getMotifLabDirectory()+File.separator+"startup.config");                    
            if (vizConfigFile.exists()) {
                logMessage("Executing startup protocol",5);
                StandardProtocol vizSettingsProtocol=new StandardProtocol(this, new BufferedInputStream(new FileInputStream(vizConfigFile)));
                executeProtocol(vizSettingsProtocol,true);
            } else logMessage("WARNING: Unable to execute startup configuration protocol: FILE DOES NOT EXIST",5);
        } catch (Exception e) {
            logMessage("WARNING: Unable to execute startup configuration protocol: "+e.toString(),5);
            if (e instanceof NullPointerException) e.printStackTrace(System.err);
        }                
    }    
    
    
    
    /**
     * Returns an URL string which references a given entry in a given database 
     */
    public String getExternalDatabaseURL(String idformat, String identifier) {
        return geneIDResolver.getWebLink(idformat, identifier);   
    }
    
    /** Returns a list of recognized external databases that can be linked to on the web
      */
    public String[] getExternalDatabaseNames() {        
        ArrayList<String> list=geneIDResolver.getExternalDatabaseNames();
        String[] db=new String[list.size()];
        return list.toArray(db);
    }
    /** Returns true if the given name matches a known external database */
    public boolean isExternalDatabaseName(String name) {
        return geneIDResolver.getExternalDatabaseNames().contains(name);
    }
    
    public String[] parseDataReference(String ref) {
        if (ref.startsWith("{") && ref.endsWith("}")) ref=ref.substring(1,ref.length()-1);
        if (ref.contains(":")) {
            return ref.split(":",2);
        } else return new String[]{ref,null};      
    }
    
    /**
     * Returns a String which represents the value of a data reference
     * The reference should be the name of a data object (Numeric Variable, TextVariable, Collection or OutputVariable)
     * The reference name could be enclosed in vertical bars (e.g. |dataname| ) which works as a cardinality operator
     * and the reference can also be followed by one or more option clauses (introduced by colons)
     * 
     * @param reference The name of a data object (for collections this could be enclosed in vertical bars)
     *         If the reference contains a comma, the part after the comma is the "options" part
     *         and this part has different meanings for different data types.
     *         For Collections it specifies a string to separate the entries in the list (defaults to comma)
     *         For NumericVariables (that are not integers!) it can be on the format: X% (or rather [X][%] since both the X and the % are optional)
     *         Here the X specifies the number of digits to output after the decimal point and % means that the value is a percentage fraction and should be multiplied by 100 and followed by a % sign
     * @param lineSeparator If the referenced object is a TextVariable or (non-HTML) OutputData object
     *        containing several lines, the parameter specifies a string to separate these lines in the
     *        single string that is returned.
     * @return a String representing the referenced object, or null of the object was of a type that could not be resolved
     * @throws ExecutionError if the referenced object does not exist
     */
    public String resolveDataReferences(String reference, String lineSeparator) throws ExecutionError {
        if (reference.startsWith("{") && reference.endsWith("}")) reference=reference.substring(1,reference.length()-1);
        String option=null;
        String sortOption=null;
        String ref=null;
        boolean cardinality=false;    
        if (reference.contains(":")) {
            String[] parts=reference.trim().split("\\s*:\\s*");
            ref=parts[0];
            if (parts.length>=2) option=parts[1];  
            if (parts.length>=3) sortOption=parts[2];
            if (option!=null) {
                if (option.equals("size")) cardinality=true;
                option=option.replace("\\n", "\n");
                option=option.replace("\\t", "\t");
                option=option.replace("\\s", " "); // use \s as escape character for space since all spaces are stripped in the split above                
                option=option.replace("colon", ":");            
            }
        } else ref=reference;         
        if (ref.startsWith("|") && ref.endsWith("|")) {
            cardinality=true;
            ref=ref.substring(1,ref.length()-1);
        }     
        Data refObject=getDataItem(ref);
        if (refObject==null) throw new ExecutionError("Unknown data object: "+ref);                
        Class refclass=refObject.getClass();
        String resolvedRef;             
        if (refObject instanceof NumericVariable) {
            double value=((NumericVariable)refObject).getValue();
            if (value==(int)value && option==null) resolvedRef=""+(int)value;
            else if (option==null) resolvedRef=""+value;
            else {
                String suffix=null;
                int digits=-1;
                if (option.endsWith(" %")) {option=option.substring(0,option.length()-2);suffix=" %";}
                else if (option.endsWith("%")) {option=option.substring(0,option.length()-1);suffix="%";}
                if (!option.isEmpty()) try {digits=Integer.parseInt(option);} catch (NumberFormatException e) {throw new ExecutionError("Error in #digits option for reference: "+option);}
                if (suffix!=null) value*=100; // convert to percentage
                resolvedRef=""+value;
                int dotpos=resolvedRef.indexOf('.');
                if (digits>0 && dotpos>=0) {
                    int length=resolvedRef.length();
                    resolvedRef=resolvedRef.substring(0, (length<dotpos+digits+1)?length:(dotpos+digits+1) );
                } else if (digits==0 && dotpos>=0) {
                    resolvedRef=resolvedRef.substring(0,dotpos);
                }
                return (suffix!=null)?(resolvedRef+suffix):resolvedRef;
            }
        } else if (refObject instanceof TextVariable) {
              ArrayList<String> lines=((TextVariable)refObject).getAllStrings();
              if (sortOption!=null && !sortOption.isEmpty()) {
                  boolean[] op=getSortingOptions(sortOption);
                  if (op[0]) Collections.sort(lines,getNaturalSortOrderComparator(op[1]));
                  if (op[2]) lines=MotifLabEngine.removeDuplicateLines(lines,false);
              }               
              resolvedRef=MotifLabEngine.splice(lines,(option!=null && !option.isEmpty())?option:lineSeparator);                
        } else if (DataCollection.class.isAssignableFrom(refclass)) {
            if (cardinality) resolvedRef=""+((DataCollection)refObject).size();
            else {
                ArrayList<String> list=(ArrayList<String>)(((DataCollection)refObject).getValues().clone());
                Collections.sort(list);
                resolvedRef=MotifLabEngine.splice(list,(option!=null)?option:",");
            }
        } else if (refObject instanceof OutputData) {
            if (((OutputData)refObject).isHTMLformatted()) resolvedRef=((OutputData)refObject).getHTMLBody();
            else {
                ArrayList<String> lines=((OutputData)refObject).getContentsAsStrings();
                if (sortOption!=null && !sortOption.isEmpty()) {
                    boolean[] op=getSortingOptions(sortOption);
                    if (op[0]) Collections.sort(lines,getNaturalSortOrderComparator(op[1]));
                    if (op[2]) lines=MotifLabEngine.removeDuplicateLines(lines,false);
                }    
                resolvedRef=MotifLabEngine.splice(lines,(option!=null && !option.isEmpty())?option:lineSeparator);
            }
        } else resolvedRef=null; // not able to resolve
        return resolvedRef;
    }    
    
    private boolean[] getSortingOptions(String sortString) {
        boolean dosort=false;        
        boolean ascending=true;
        if (sortString.contains("sort")) dosort=true;
        if (sortString.contains("asc") || sortString.startsWith("A")) {dosort=true;ascending=true;}
        else if (sortString.contains("des") || sortString.startsWith("D")) {dosort=true;ascending=false;}
        boolean unique=(sortString.contains("uniq") || sortString.contains("U"));
        return new boolean[]{dosort,ascending,unique};
    }
    
    private static final Comparator<String> naturalSortAscendingComparator=new NaturalSortOrderComparator(true);
    private static final Comparator<String> naturalSortDescendingComparator=new NaturalSortOrderComparator(false);
    
    public static Comparator<String> getNaturalSortOrderComparator(boolean sortAscending) {
        return (sortAscending)?naturalSortAscendingComparator:naturalSortDescendingComparator;
    }
    
    public static void sortNaturalOrder(List<String> list, boolean ascending) {
        Collections.sort(list,getNaturalSortOrderComparator(ascending));
    }
    
    public static void sortNaturalOrder(String[] list, boolean ascending) {
        Arrays.sort(list,getNaturalSortOrderComparator(ascending));
    }    

    private static class NaturalSortOrderComparator implements Comparator<String> {
        private boolean ascending=true;
        
        public NaturalSortOrderComparator(boolean sortAscending) {
            ascending=sortAscending;
        }
        @Override
        public int compare(String string1, String string2) { //
            int cmp=MotifLabEngine.compareNaturalOrder(string1,string2);
            return (ascending)?cmp:(-cmp);            
        }                
    }    
    
    
    /**
     * Contacts the MotifLab web server and checks if there are any new notifications
     * @param level the minimum level the notification must be at to be included
     * @param showAll If TRUE then return all notifications, else only return new notifications 
     * @param updateIndex If TRUE, then the index of the last message read will be updated
     * @return 
     */
    public ArrayList<ImportantNotification> getNewNotifications(int level, boolean showAll, boolean updateIndex) {
        ArrayList<ImportantNotification> notifications=new ArrayList<>();
        // first try to determine which was the last notification         
        int notificationIndex=0;
        File indexFile=new File(getMotifLabDirectory()+File.separator+"notificationCounter.txt");
        if (indexFile.exists()) {
            try {
               String contents=readTextFile(indexFile.getAbsolutePath());
               notificationIndex=Integer.parseInt(contents.trim());
            } catch (Exception e) {
               logMessage("An error occurred while reading the system file 'notificationCounter.txt' : "+e.toString());
            }
        }
        boolean ok=true;        
        String uri=getWebSiteURL()+"notifications.xml";
        ImportantNotificationParser parser=new ImportantNotificationParser();
        try {
            notifications=parser.parseNotifications(uri, level, (showAll)?0:notificationIndex+1);
            if (!notifications.isEmpty()) notificationIndex=notifications.get(0).getMessageIndex(); // update index
        } catch (Exception e) {
            logMessage("MotifLab Server Error (notifications.xml): "+e.getMessage());
            ok=false;
        }
        try {
           if (ok && updateIndex) writeTextFile(indexFile, ""+notificationIndex); // this number is the last notification read
        } catch (Exception e) {
           logMessage("An error occurred while writing the file 'notificationCounter.txt' : "+e.toString());
        }
        return notifications;       
    }
    
   /**
     * Saves the current session (as defined by the 'info' map)
     * @param outputStream
     * @param info A map containing the information that should be saved in the session. The map should include the keys (and corresponding values): "data", "visualizationsettings", "protocols", "tabnames", "selectedtab"
     * @param progressListener
     * @throws Exception 
     */
  public void saveSession(ObjectOutputStream outputStream, HashMap<String, Object> info, ProgressListener progressListener) throws Exception {
        // Session format versions: 
        //   1 = Original format 
        //   2 = Included optional requirements
        //   3 = Graph type changed from Integer to String. Sessions restored from versions prior to v3 should have the old integers converted to their new named types
        //   4 = Added support for "transparent gradients". Sessions restored from versions prior to v4 should have their gradients updated
        int sessionFormatVersion=4; // 
        if (progressListener!=null) progressListener.processProgressEvent(this,0);
        try {
            resetOutputDataDependencyProcessedFlags();      
            String versionString=MotifLabEngine.getVersion(); // Either just MotifLab version of "MotifLabVersion;sessionFormat;[requirements;]"
            Object dataObject=info.get("data");
            if (dataObject==null || !(dataObject instanceof ArrayList)) throw new SystemError("'data' property not defined when saving session");
            ArrayList<Data> allDataObjects=(ArrayList<Data>)dataObject;
            String[] requirements=getSessionRequirements(allDataObjects);
            versionString=versionString+";"+sessionFormatVersion;
            if (requirements!=null && requirements.length>0) {
                String req=MotifLabEngine.splice(requirements, ";");
                versionString+=(";"+req);  
            }
            outputStream.writeObject(versionString); // save version string first. This could either be just the MotifLab version or a semi-colon separated list of values (where the first two are MotifLabVersion and Session Format version, and the rest are requirements
            int total=allDataObjects.size();
            outputStream.writeInt(total); // state how many objects are coming next!
            int i=0;
            int startprogress=5; // the progress value before writing all the data objects
            int endprogress=70;    // the progress value after writing all the data objects
            int progressrange=endprogress-startprogress; 
            if (progressListener!=null) progressListener.processProgressEvent(this,startprogress);
            for (Data copy:allDataObjects) { // data objects are not cloned, but updates to data is not allowed by the engine                  
                double fraction=(double)i/(double)total;
                if (progressListener!=null) progressListener.processProgressEvent(this,((int)(startprogress+progressrange*fraction)));
                i++;                          
                if (copy instanceof OutputData) ((OutputData)copy).setSerializeDependencies(true); // Specify that OutputData items should serialize dependecies also!
                outputStream.writeObject(copy);   
                //System.err.println("Saving object["+i+"]: "+copy.getName());
            }                              
            Object vizSettingsObject=info.get("visualizationsettings");
            if (vizSettingsObject==null || !(vizSettingsObject instanceof VisualizationSettings)) throw new SystemError("'visualizationsettings' property not defined when saving session");
            VisualizationSettings vizSettings=(VisualizationSettings)vizSettingsObject;              
            outputStream.writeObject(vizSettings);
            if (progressListener!=null) progressListener.processProgressEvent(this,80);  
            Object protocolObject=info.get("protocols");            
            outputStream.writeObject(protocolObject);
            if (progressListener!=null) progressListener.processProgressEvent(this,90);
            outputStream.writeObject(info.get("tabnames"));
            outputStream.writeObject(info.get("selectedtab"));
            if (progressListener!=null) progressListener.processProgressEvent(this,95);
            outputStream.close();
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new ExecutionError(e.getMessage(),e);
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (Exception x) {}
        }
    }   
  
  /**
   * Restores session information from the provided object stream and returns it as a HashMap
   * Note that this method will not throw an exception if one occurs. Rather, it will return as normal, but the exception will be available as the value for the key "exception"
   * @param input This should be a plain InputStream (it will be wrapped in a buffered stream as necessary)
   */
   public HashMap<String,Object> restoreSession(InputStream input, ProgressListener progressListener) { 
        HashMap<String,Object> restored=new HashMap<String,Object>();
        ArrayList<Data> datalist=null;
        VisualizationSettings settings=null;
        Object restoredProtocols=null; // this can be a SerializedProtocolManager or StandardProtocol
        SequenceCollection defaultCollectionCopy=null;
        String[] tabNames;
        String selectedTabName=null;  
        logMessage("Restoring session...",9);
        int supportedSessionFormat=4; // the newest session format supported by this version of MotifLab
        try {
            DynamicClassLoaderObjectInputStream inputStream = new DynamicClassLoaderObjectInputStream(new BufferedInputStream(input),this);            
            String versionString=(String)inputStream.readObject(); // the version info could potentially be used later if the format changes
            int sessionFormat=2; // default session format if not mentioned in the file is 2
            String[] requirements=null;
            if (versionString.contains(";")) {               
               String[] parts=versionString.split(";");
               if (parts.length>=1) versionString=parts[0];
               if (parts.length>=2) try {sessionFormat=Integer.parseInt(parts[1]);} catch(NumberFormatException nfr) {throw new ExecutionError("Invalid sessionFormatNumber:"+parts[1]);}
               if (parts.length>=3) {
                    requirements=new String[parts.length-2];
                    for (int i=0;i<requirements.length;i++) requirements[i]=parts[i+2];
                }
            }
            if (sessionFormat>supportedSessionFormat) throw new ExecutionError("The session is stored in a newer format (v"+sessionFormat+") that is not supported by this version (only up to v"+supportedSessionFormat+" is supported)"); 
            restored.put("version",versionString); // the full version string (can contains semicolon-separated entries)
            restored.put("sessionFormatVersion",""+sessionFormat); // NB: this is stored as String here
            if (requirements!=null) restored.put("requirements", requirements);
            int total = (Integer)inputStream.readInt();
            datalist=new ArrayList<Data>(total-1); // the default collection will not be added here
            int startprogress=5; // the progress value before reading all the data objects
            int endprogress=70;    // the progress value after reading all the data objects
            int progressrange=endprogress-startprogress;   
            if (progressListener!=null) progressListener.processProgressEvent(this,startprogress);
            logMessage(" - Loading session data",9);
            for (int i=0;i<total;i++) {
                Data data=(Data)inputStream.readObject(); 
                if (data instanceof OutputData) ((OutputData)data).initializeAfterSerialization(this);    
                if (data instanceof RegionDataset) ((RegionDataset)data).fixParentChildRelationships(); // repairs problems caused by an old bug which is still present in some saved sessions
                if (data.getName().equals(getDefaultSequenceCollectionName()) && data instanceof SequenceCollection) defaultCollectionCopy=(SequenceCollection)data;
                else datalist.add(data);    
                double fraction=(double)i/(double)total;
                if (progressListener!=null) progressListener.processProgressEvent(this,(int)(startprogress+progressrange*fraction));                 
            }      
            logMessage(" - Loading session settings",9);
            if (progressListener!=null) progressListener.processProgressEvent(this,endprogress);
            settings=(VisualizationSettings)inputStream.readObject();
            if (progressListener!=null) progressListener.processProgressEvent(this,80);
            restoredProtocols=inputStream.readObject();
            if (progressListener!=null) progressListener.processProgressEvent(this,90);
            tabNames=(String[])inputStream.readObject();
            selectedTabName=(String)inputStream.readObject();
            if (progressListener!=null) progressListener.processProgressEvent(this,95);
            inputStream.close();
            restored.put("data",datalist);
            restored.put("visualizationsettings",settings);
            restored.put("protocols",restoredProtocols);
            restored.put("tabnames",tabNames);
            restored.put("selectedtab",selectedTabName);
            restored.put("defaultsequencecollection", defaultCollectionCopy);
        } catch (Exception e) {           
            // if (e instanceof StreamCorruptedException) e=new ExecutionError("The session file requires a different version of MotifLab (probably newer) or some required plugins might be missing");
            restored.put("exception",e);
        } catch (OutOfMemoryError e) {
            restored.put("exception", new ExecutionError("There is not enough memory available to restore the session",e));
        } catch (Throwable e) {
            restored.put("exception", new ExecutionError(e.getMessage(),e));
        } finally {
            try {if (input!=null) input.close();} catch (Exception x) {}
        }  
        return restored;
    }   
   
// ================  The methods below are for configuring MotifLab the first time it is used by installing some bundled configuration files into a local directory  ====================
     

    public void installResourcesFirstTime() throws SystemError {
        if (!isFirstTimeStartup()) return; // is this really the first time the user runs MotifLab?        
        
        if (client==null) System.err.println("WARNING: No client installed (yet)");
        // --- install general configuration files ---
        ArrayList<String> configfiles=locateBundledFiles("/motiflab/engine/resources",".config");
        if (!configfiles.isEmpty()) logMessage("Installing configuration files");
        int count=0;
        int size=configfiles.size();
        for (String filename:configfiles) {
            try {
                InputStream stream=this.getClass().getResourceAsStream("/motiflab/engine/resources/"+filename);
                installConfigFileFromStream(filename,stream);
                if (client!=null) client.progressReportMessage((int)(10+(10f/(float)size)*count));
                count++;
            } catch (SystemError e) {
               logMessage("Unable to install configuration file '"+filename+"' => "+e.getMessage());
            }
        }      
        if (client!=null) client.progressReportMessage(20);   
        
        // --- install external program configuration files ---
        ArrayList<String> bundled=locateBundledFiles("/motiflab/external",".xml");
        if (!bundled.isEmpty()) logMessage("Installing bundled programs");
        count=0;
        size=bundled.size();
        for (String filename:bundled) {
            try {
                InputStream stream=this.getClass().getResourceAsStream("/motiflab/external/"+filename);
                installBundledProgram(filename,stream);
                if (client!=null) client.progressReportMessage((int)(20+(30f/(float)size)*count));
                count++;
            } catch (SystemError e) {
               if (client!=null) client.logMessage("Unable to install bundled program '"+filename+"' => "+e.getMessage());
            }
        }
        if (client!=null) client.progressReportMessage(50); //  
        
        // --- install predefined Motif Collections ---
        ArrayList<String> bundledMotifs=locateBundledFiles("/motiflab/engine/resources",".mlx");
        if (client!=null) client.logMessage("Installing bundled motif collections");
        installBundledMotifCollections(bundledMotifs,50,100);   
        if (client!=null) client.progressReportMessage(-1); // Tells graphical clients to hide the progressbar
        engine.raiseInstalledFlag(); // installation should now be completed
        if (engine.isFirstTimeStartup()) { // this should now return FALSE if everything was registered OK
            throw new SystemError("It seems that something went wrong during installation. Please try to restart MotifLab");
        }      
        if (client!=null) client.logMessage("Installation completed successfully");  
        importResources();
    }
   
   
    /** Installs the program with XML-config file provided in the given stream
     *  @param filename The name of the XML-config file (including XML-suffix)
     *  @param stream An InputStream providing access to the file
     */
    private void installBundledProgram(String filename,InputStream stream) throws SystemError {
        ExternalProgram program=null;
        program = ExternalProgram.initializeExternalProgramFromStream(stream,filename,true);
        // the following line should not happen (but just in case...)
        if (program==null) throw new SystemError("Unable to read program definition file");
        File externalfile=new File(engine.getMotifLabDirectory()+File.separator+"external"+File.separator+filename);           
        try {
            File parentDir=externalfile.getParentFile();
            if (parentDir!=null && !parentDir.exists()) parentDir.mkdirs();
            program.saveConfigurationToFile(externalfile);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            if (e instanceof SystemError) throw (SystemError)e;
            else throw new SystemError(e.getClass().getSimpleName()+":"+e.getMessage());
        }
        program.clearProperties();
        // a call will later be made to engine.importExternalPrograms() to initialize the programs properly
    }

    
   
   
     private void installBundledMotifCollections(final ArrayList<String> bundledMotifs, final int progressStart, final int progressEnd)  {
        final float range=progressEnd-progressStart;
        int count=0;
        File dir=new File(getMotifLabDirectory()+File.separator+MotifCollectionDirectory);
        if (!dir.exists()) dir.mkdirs();            
        int size=bundledMotifs.size();
        for (String filename:bundledMotifs) {
            try {
                InputStream stream=this.getClass().getResourceAsStream("/motiflab/engine/resources/"+filename);
                if (client!=null) client.progressReportMessage((int)(progressStart+(range/(float)size)*count));
                if (client!=null) client.logMessage("Installing motif collection: "+filename);
                engine.registerPredefinedMotifCollectionFromStream(stream, filename, null);
                count++;
            } catch (Exception e) {
               if (client!=null) client.logMessage("Unable to install bundled motif collection '"+filename+"' => "+e.getClass().getSimpleName()+":"+e.getMessage());
               e.printStackTrace(System.err);
            }
        }
    }    
   
   
   /**
 * This method returns a list of files (with the given suffix) that are bundled
 * with MotifLab. I would really have liked this list to be determined dynamically
 * based on the files that are actually present in the given directories in the
 * packaged JAR file, however, since I (after tedious struggle) still have been unable to
 * find ways to accomplish this, I must simply hardcode the names of the files in the list :-(
 */
private ArrayList<String> locateBundledFiles(String packagedir, String suffix) {
        ArrayList<String> list=new ArrayList<String>();
        if (packagedir.equals("/motiflab/engine/resources") && suffix.endsWith("mlx")) {
            list.add("Transfac_public.mlx");
            list.add("Jaspar_core.mlx");
            list.add("Jaspar_FAM.mlx");
            list.add("Jaspar_phylofacts.mlx");
            list.add("Jaspar_splice.mlx");
            list.add("Jaspar_cne.mlx");
            list.add("Jaspar_polII.mlx");
            list.add("Jaspar_PBM.mlx");
            list.add("Jaspar_PBM_HLH.mlx");
            list.add("Jaspar_PBM_HOMEO.mlx");
            list.add("ScerTF.mlx");   
            list.add("PLACE.mlx");            
        } else if (packagedir.equals("/motiflab/engine/resources") && suffix.endsWith("config")) {
            list.add("Organisms.config");
            list.add("GeneIDResolver.config");
            list.add("startup.config");
            list.add("TFclass.config");                 
             // include these too as config-files
            list.add("motiflab.js");
            list.add("default.css");
            list.add("green.css");  
        } else if (packagedir.equals("/motiflab/external") && suffix.endsWith("xml")) {
            // list.add("Priority.xml");
            list.add("AffinityScanner.xml");
            list.add("SimpleScanner.xml");    
            list.add("StringScanner.xml");             
            list.add("ConsensusScanner.xml");            
            list.add("SimpleModuleScanner.xml");
            list.add("SimpleEnsemble.xml");
            list.add("SimpleModuleSearcher.xml");
            list.add("EMD.xml");
            //list.add("MotifVoter.xml");
        }
        return list;
  }
   
   
    /**
     * Installs the given file in the MotifLab directory and updates
     * current configurations accordingly
     * @param file 
     */
    public void installConfigFile(File newfile) throws IOException, SystemError {       
        String filename=newfile.getName();
        File oldfile=new File(getMotifLabDirectory()+File.separator+filename); 
        if (newfile instanceof DataRepositoryFile) {
            InputStream stream=((DataRepositoryFile)newfile).getFileAsInputStream();
            Files.copy(stream, oldfile.toPath(), StandardCopyOption.REPLACE_EXISTING);            
        } else {
            Files.copy(newfile.toPath(), oldfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        // now update stuff if necessary
        if (filename.equals("GeneIDResolver.config")) {
            getGeneIDResolver().initializeFromFile(newfile, true);
        } else if (filename.equals("Organisms.config")) {
            Organism.initializeFromFile(newfile);
        } else if (filename.equals("TFclass.config")) {
            MotifClassification.initializeFromFile(newfile);
        } else if (filename.equals("DataTracks.xml")) {
            getDataLoader().setup();
        }
    }
    
    /**
     * Installs the files within the ZIP-file in the MotifLab directory 
     * and updates current configurations accordingly
     * @param file 
     */
    public void installConfigFilesFromZip(File zipfile) throws IOException, SystemError {       
        File targetDir=new File(getMotifLabDirectory()); 
        ArrayList<File> unzipped=unzipFile(zipfile, targetDir);
        // now update stuff if necessary
        for (File newfile:unzipped) {
            String filename=newfile.getName();
            if (filename.equals("GeneIDResolver.config")) {
                getGeneIDResolver().initializeFromFile(newfile, true);
            } else if (filename.equals("Organisms.config")) {
                Organism.initializeFromFile(newfile);
            } else if (filename.equals("TFclass.config")) {
                MotifClassification.initializeFromFile(newfile);
            } else if (filename.equals("DataTracks.xml")) {
                getDataLoader().setup();
            }
        }
    }     
    
// **************************************
// **                                  **    
// **       MOTIFLAB RESOURCES         **    
// **                                  **    
// **************************************    
//
// MotifLab resources are objects (or object factories) that can be registered
// and then afterwards retrieved again by name (or type). This allows for more
// dynamic and "pluggable" configurations for those parts of MotifLab that support them.
//     

/**
 * Registers a new MotifLab Resource
 * @param resource The resource to be registered
 * @return returns TRUE if the new resource replaces and existing resource with the same name
 */
public boolean registerResource(MotifLabResource resource) {
    if (resource==null) return false;
    if (motiflabresources==null) motiflabresources=new HashMap<>();
    String registryName=resource.getResourceName();
    String typename=resource.getResourceTypeName();
    if (typename!=null && !typename.isEmpty()) registryName=typename+"|"+registryName;
    boolean exists=motiflabresources.containsKey(registryName);
    motiflabresources.put(registryName,resource);
    return exists;
}   

/**
 * Deregisters a MotifLab Resource
 * @param name The name of the resource to be deregistered
 * @param typename The "type name" of the resource (if it has one), or null/empty 
 * @return If a resource with that name exists, it is returned. If not, this function returns NULL
 */
public MotifLabResource deregisterResource(String name, String typename) {
    if (motiflabresources==null || name==null) return null;
    if (typename!=null && !typename.isEmpty()) name=typename+"|"+name;    
    MotifLabResource res=motiflabresources.get(name);
    motiflabresources.remove(name);
    if (motiflabresources.isEmpty()) motiflabresources=null;
    return res;
}  

/**
 * Deregisters a MotifLab Resource
 * @param resource an  instance of the resource that shall be removed
 * @return If this resource is registered it will be returned. If not, the function returns NULL
 */
public MotifLabResource deregisterResource(MotifLabResource resource) {
    if (motiflabresources!=null && resource!=null) {
         String name = resource.getResourceName();
         String typename = resource.getResourceTypeName();
         if (typename!=null && !typename.isEmpty()) name=typename+"|"+name;    
         MotifLabResource res=motiflabresources.get(name);
         motiflabresources.remove(name);
         if (motiflabresources.isEmpty()) motiflabresources=null;      
         return res;
    }
    return null;
}  

/**
 * Returns the class type of the resource with the specified name,
 * or null if no resource with that name has been registered
 * @param name The name of a resource
 * @param typename The "type name" of the resource (if it has one), or null/empty  * 
 * @return the class of the resource (if found) or null
 */
public Class getResourceClass(String name, String typename) {
    if (motiflabresources==null || name==null) return null;
    if (typename!=null && !typename.isEmpty()) name=typename+"|"+name;      
    MotifLabResource res=motiflabresources.get(name);
    return (res!=null)?res.getResourceClass():null;
}  

/**
 * Returns an Icon for the resource with the specified name,
 * or null if no resource with that name has been registered
 * @param name The name of a resource
 * @param typename The "type name" of the resource (if it has one), or null/empty  * 
 * @return an Icon for the resource (if found) or null
 */
public javax.swing.Icon getResourceIcon(String name, String typename) {
    if (motiflabresources==null || name==null) return null;
    if (typename!=null && !typename.isEmpty()) name=typename+"|"+name;      
    MotifLabResource res=motiflabresources.get(name);
    return (res!=null)?res.getResourceIcon():null;
}  

/**
 * Returns a resource instance object for the resource registered under the given name
 * or null if no resource with that name has been registered
 * @param name The name of a resource
 * @param typename The "type name" of the resource (if it has one), or null/empty  * 
 * @return the resource (if found) or null
 */
public Object getResource(String name, String typename) {
    if (motiflabresources==null || name==null) return null;
    if (typename!=null && !typename.isEmpty()) name=typename+"|"+name;      
    MotifLabResource res=motiflabresources.get(name);
    return (res!=null)?res.getResourceInstance():null;
}  

/**
 * Returns a list of MotifLabResource objects for resources
 * of the matching class type (or subclasses thereof)
 * @param type the class type of the requested resource
 * @return a list of matching MotifLabResources (unsorted)
 */
public ArrayList<MotifLabResource> getResources(Class type) {
    if (motiflabresources==null || type==null) return new ArrayList<>(0);
    ArrayList<MotifLabResource> list = new ArrayList<>(motiflabresources.size());
    for (MotifLabResource res:motiflabresources.values()) {
        if (type.isAssignableFrom(res.getResourceClass())) list.add(res);
    }
    return list;
}  

/**
 * Returns a list of MotifLabResource objects for resources
 * with the specified type name 
 * @param typename the type name of the requested resources (not null)
 * @return a list of matching MotifLabResources (unsorted)
 */
public ArrayList<MotifLabResource> getResources(String typename) {
    if (motiflabresources==null || typename==null) return new ArrayList<>(0);
    ArrayList<MotifLabResource> list = new ArrayList<>(motiflabresources.size());
    for (MotifLabResource res:motiflabresources.values()) {
        if (typename.equals(res.getResourceTypeName())) list.add(res);
    }
    return list;
}  

/**
 * Returns a list of names of resources of the matching class type (or subclasses thereof)
 * @param type the class type of the requested resource
 * @return a list of names of matching MotifLabResources (unsorted)
 */
public ArrayList<String> getResourceNames(Class type) {
    if (motiflabresources==null || type==null) return new ArrayList<>(0);
    ArrayList<String> list = new ArrayList<>(motiflabresources.size());
    for (MotifLabResource res:motiflabresources.values()) {
        if (type.isAssignableFrom(res.getResourceClass())) list.add(res.getResourceName());
    }
    return list;
}  

/**
 * Returns a list of names of resources that have the specified type name
 * @param typename the type name of the requested resources
 * @return a list of names of matching MotifLabResources (unsorted)
 */
public ArrayList<String> getResourceNames(String typename) {
    if (motiflabresources==null || typename==null) return new ArrayList<>(0);
    ArrayList<String> list = new ArrayList<>(motiflabresources.size());
    for (MotifLabResource res:motiflabresources.values()) {
        if (typename.equals(res.getResourceTypeName())) list.add(res.getResourceName());
    }
    return list;
}  

/** @return the number of registered MotifLabResources */
public int getNumberOfResources() {
    if (motiflabresources==null) return 0;
    else return motiflabresources.size();
}

/** Removes all all registered resources */
public void removeAllResources() {
    motiflabresources=null;
}
    
// ------------- just a few utilities below this line. Most of these are static and can be used anywhere. Perhaps they should be movel to a "MotifLabUtil" class later on?  ------------------------      

   
   /**
    * Unescapes the special characters (double quotes, tabs, newlines and backslashes) in the provided string
    * Double backslashes (\\) are replaced with single backslashes.
    * \t are replaced with TABs.
    * \n are replaced with newlines.
    * All other backslashes are just removed.  (E.g. \" is replaced with just " )
    */
    public static String unescapeQuotedString(String string) {
        if (string==null) return string;
        string=string.replace("\\\\", "\b"); // first replace double backslashes with a unique placeholder (here "bell") so as to not introduce new backslashes that might interfere with the following steps if they happen to be placed in front of other things which might then look like other escape sequences  (e.g. if "\\numeric\\" is directly replaced with "\numeric\" the "\n" at the beginning will be interpreted as a newline in the next step).
        string=string.replace("\\t", "\t"); // 
        string=string.replace("\\n", "\n");
        string=string.replace("\\", ""); // remove remaining backslashes
        string=string.replace("\b","\\"); // now introduce back the backslashes that should actually be there.
        return string;
    }
    
   /**
    * Escapes special characters (double quotes, tabs, newlines and backslashes) in the string so that they can be safely placed within double quotes in e.g. protocol scripts
    * Backslashes are escaped as double backslashes
    * TABs are escaped as \t
    * Newlines are escaped as \n
    * Double quotes are escaped as \"
    */
    public static String escapeQuotedString(String string) {
        if (string==null) return string;
        string=string.replace("\\", "\\\\");
        string=string.replace("\t", "\\t");
        string=string.replace("\n", "\\n");
        string=string.replace("\"", "\\\""); 
        return string;
    }    
   
    /** Returns true if the target string is in the list*/
    public static boolean inArray(String target, String[] list, boolean ignoreCase) {
        if (target==null || list==null || list.length==0) return false;
        for (String element:list) {
           if ((ignoreCase && element.equalsIgnoreCase(target)) || (!ignoreCase && element.equals(target))) return true; 
        }
        return false;
    }
    
    /** Returns true if the target string is in the list*/
    public static boolean inArray(String target, ArrayList<String> list, boolean ignoreCase) {
        if (target==null || list==null || list.isEmpty()) return false;
        for (String element:list) {
           if ((ignoreCase && element.equalsIgnoreCase(target)) || (!ignoreCase && element.equals(target))) return true; 
        }
        return false;
    }    
    
    @SuppressWarnings("unchecked")
    public static boolean listcompare(int[] list1, int[]  list2) {
        if (list1.length!=list2.length) return false;
        for (int i:list1) {
            if (!inArray(i,list2)) return false;
        }
        return true;
    }
    
    public static boolean inArray(int element,int[] list) {
        for (int i:list) {
            if (i==element) return true;
        }
        return false;
    }    
    
    /** Fetches a web page from the given URL and returns it as a single String.
     *  Any newlines (or carriage returns) in the page are stripped
     */
    public static String getPage(URL url) throws Exception {
        StringBuilder document=new StringBuilder();
        InputStream inputStream = null;
        BufferedReader dataReader = null;
        URLConnection connection=url.openConnection();
        // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually
        if (connection instanceof HttpURLConnection) {
            int status = ((HttpURLConnection)connection).getResponseCode();
            String location = ((HttpURLConnection)connection).getHeaderField("Location");
            if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
                    ((HttpURLConnection)connection).disconnect();
                    return getPage(new URL(location));
            } 
        }
        inputStream=connection.getInputStream();
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        String line=null;
        try {
           while ((line=dataReader.readLine())!=null){
              document.append(line);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }    
        return document.toString();
    }  
        
    /** Fetches a web page from the given URL and inserts it into the provided Document */
    public static void getPage(URL url, javax.swing.text.Document document) throws Exception {
        InputStream inputStream = null;
        BufferedReader dataReader = null;
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually
	int status = ((HttpURLConnection)connection).getResponseCode();
	String location = ((HttpURLConnection)connection).getHeaderField("Location");
	if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
		((HttpURLConnection)connection).disconnect();
	        getPage(new URL(location), document);
                return;
	}        
        inputStream=connection.getInputStream();
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        String line=null;
        try {
           while ((line=dataReader.readLine())!=null){
              document.insertString(document.getLength(), line, null);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }    
    }       
   
    /** Fetches a web page from the given URL and returns it as a String array with one String per line */
    public static ArrayList<String> getPageAsList(URL url) throws Exception {
        InputStream inputStream = null;
        BufferedReader dataReader = null;
        URLConnection connection=url.openConnection();
        // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually
        if (connection instanceof HttpURLConnection) {
            int status = ((HttpURLConnection)connection).getResponseCode();
            String location = ((HttpURLConnection)connection).getHeaderField("Location");
            if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
                    ((HttpURLConnection)connection).disconnect();
                    return getPageAsList(new URL(location));
            }        
        }
        inputStream=connection.getInputStream();
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        String line=null;
        ArrayList<String> lines=new ArrayList<String>();
        try {
           while ((line=dataReader.readLine())!=null){
              lines.add(line);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }    
        return lines;
    }     
    
    /**
     * This function will return a redirect URL if the original URL is a HTTP site 
     * that is redirected to an HTTPS site by the remote server
     * If no redirection from HTTP to HTTPS takes place, the value NULL will be returned.
     * @param url 
     */
    public static URL checkHTTPredirectToHTTPS(URL url) throws MalformedURLException, IOException {
        URLConnection connection=url.openConnection();
        // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually
        int status = ((HttpURLConnection)connection).getResponseCode();
        String location = ((HttpURLConnection)connection).getHeaderField("Location");
        if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
             return new URL(location);
        }
        return null;
    }
    
    
    /** Retrieves a web page given an URL address using an http POST-request */    
    public static ArrayList<String> getPageUsingHttpPost(URL url, Map<String,Object> parameters, int timeout) throws Exception {    
        ArrayList<String> document=new ArrayList<String>();
        InputStream inputStream = null;
        BufferedReader dataReader = null;      
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : parameters.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");        
       
        OutputStream outputStream=null;
        HttpURLConnection connection=null;
        URL redirect = MotifLabEngine.checkHTTPredirectToHTTPS(url);
        if (redirect!=null) url=redirect;
        //connection.setRequestProperty("Content-type", "text/xml; charset=UTF-8");  
        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            connection.setConnectTimeout(timeout);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false); 
            outputStream = connection.getOutputStream();
            outputStream.write(postDataBytes);  
        } catch (Exception e) {throw e;}
        finally {
            if (outputStream!=null) outputStream.close();
        }  
        if (connection==null) return new ArrayList<String>(0);
        inputStream=connection.getInputStream();
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
           while ((line=dataReader.readLine())!=null){
              //System.err.println(line);
              document.add(line);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }    
        return document;
    }    
    
    
    
    /**
     * Reads the contents of a text file (which can be a local file, a repository file or web file) 
     * and returns its contents as an ArrayList with one String per line.
     * @param filename
     * @return 
     */
    public ArrayList<String> readFileContents(String filenameOrURL) throws Exception {
        InputStream inputStream = null; 
        Object source=getDataSourceForString(filenameOrURL);    
        inputStream=getInputStreamForDataSource(source);
        return readStreamContents(inputStream);              
    }
    
    /**
     * Reads the contents of an InputStream containing text
     * and returns its contents as an ArrayList with one String per line.
     * @param filename
     * @return 
     */
    public ArrayList<String> readStreamContents(InputStream inputStream) throws Exception {
        BufferedReader dataReader = null;    
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        String line=null;
        ArrayList<String> lines=new ArrayList<String>();
        try {
           while ((line=dataReader.readLine())!=null){
              lines.add(line);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }    
        return lines;                
    }    
    
   /** 
     * @return a File, DataRepositoryFile or URL that corresponds to the given filename (or URL)
     * @throws ExecutionError if the filename does not correspond to a potential DataRepositoryFile, an existing local file or a valid URL
    */
   public Object getDataSourceForString(String filenameOrURL) throws ExecutionError {
       DataRepositoryFile repFile=DataRepositoryFile.getRepositoryFileFromString(filenameOrURL, this);
       if (repFile!=null) return repFile;
//       File file=new File(filenameOrURL);
//       if (file.exists()) return file;
//       else {
//            try {
//                URL url=new URL(filenameOrURL);
//                return url;
//            } catch (MalformedURLException e) {
//                throw new ExecutionError("The filename '"+filenameOrURL+"' does not point to a recognized file or URL");
//            }
//       }        
        try {
            URL url=new URL(filenameOrURL);
            return url;
        } catch (MalformedURLException e) {
            return new File(filenameOrURL); // this is not an URL, so just return a local file
        }
              
   } 
   
   /** Returns a File object corresponding to the given filename (path).
    *  The returned file could refer to a regular local file or a Data Repository file
    */
   public File getFile(String filename) {
       DataRepositoryFile repFile=DataRepositoryFile.getRepositoryFileFromString(filename, this);
       if (repFile!=null) return repFile;
       return new File(filename);       
   }
   
   /** Returns a File object corresponding to the given filename and parent directory.
    *  The returned file could refer to a regular local file or a Data Repository file
    */   
   public File getFile(String dir, String filename) {
       DataRepositoryFile parentFile=DataRepositoryFile.getRepositoryFileFromString(dir, this);
       if (parentFile!=null) return new DataRepositoryFile((DataRepositoryFile)parentFile,filename);
       return new File(dir,filename);       
   } 
   
   /** Returns the full canonical path (or absolute path) of the given filename within the given directory.     
    *  The path is returned with the correct file separator according to the parent directory (which can be a regular file or a data repository file)
    */
   public String getFilePath(String dir, String filename) {
       File file=getFile(dir,filename);
       try {
           return file.getCanonicalPath();
       } catch (IOException e) {
           return file.getAbsolutePath();
       }
   }
   
   /** Returns a File object corresponding to the given filename and parent directory.
    *  The returned file could refer to a regular local file or a Data Repository file
    */      
   public static File getFile(File parent, String filename) {
        if (parent instanceof DataRepositoryFile) return new DataRepositoryFile((DataRepositoryFile)parent,filename);
        else return new File(parent, filename);      
   }
   
   /** Returns an InputStream for the provided source. 
    *  @param source The source could be a local file, a data repository file or a URL
    */
   public static InputStream getInputStreamForDataSource(Object source) throws IOException {
       if (source instanceof File) return getInputStreamForFile((File)source);
       else if (source instanceof URL) {
           URL url=(URL)source;
           URLConnection connection=url.openConnection();
            // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually
            if (connection instanceof HttpURLConnection) {
                int status = ((HttpURLConnection)connection).getResponseCode();
                String location = ((HttpURLConnection)connection).getHeaderField("Location");
                if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
                    ((HttpURLConnection)connection).disconnect();
                    return getInputStreamForDataSource(new URL(location));
                } else return connection.getInputStream();
            } 
            else if (connection!=null) return connection.getInputStream();
            else throw new IOException("Unable to open stream: "+source);
       }
       else throw new IOException("SYSTEM ERROR: '"+((source!=null)?source.toString():"null")+"' is not a File or URL");
   }    
    
   /** Returns an InputStream for the provided file. 
    *  @param file This could be a local file or a data repository file
    */   
   public static InputStream getInputStreamForFile(File file) throws IOException {
       if (file instanceof DataRepositoryFile) return ((DataRepositoryFile)file).getFileAsInputStream();
       else return new FileInputStream(file);
   }
   
   /** Returns an OutputStream for the provided file. 
    *  @param file This could be a local file or a data repository file
    */   
   public static OutputStream getOutputStreamForFile(File file) throws IOException {
       if (file instanceof DataRepositoryFile) return ((DataRepositoryFile)file).getFileAsOutputStream();
       else return new FileOutputStream(file);
   }   
    
   /** Returns a reference to the engine random number generator */ 
   public static Random getRandomNumberGenerator() {
       return randomNumberGenerator;
   }
   
   /** Encodes a UTF-8 string by escaping special characters with %XX (where XX is a hexadecimal code for the char) */
   public static String percentEncode(String string) {
        try {
            String output=java.net.URLEncoder.encode(string,"UTF-8");
            return output.replace("+","%20"); // URL encoder uses '+' for spaces be we want "%20"
        } catch (Exception e) {return string;}        
   }
    
   /** Decodes a string where special characters have been escaped with %XX (where XX is a hexadecimal code for the char) */   
   public static String percentDecode(String string) {
        try {
            return java.net.URLDecoder.decode(string,"UTF-8");
        } catch (Exception e) {return string;}        
   }    
   
   /**
    * This method calculates the Matthews correlation coefficient in a safe way
    * @param TP
    * @param FP
    * @param TN
    * @param FN
    * @return 
    */
   public static double calculateMatthewsCorrelationCoefficient(int TP, int FP, int TN, int FN) {   
        double denominator1=(TP+FN);
        double denominator2=(TN+FP);
        double denominator3=(TP+FP);
        double denominator4=(TN+FN);
        if (denominator1==0 || denominator2==0 || denominator3==0 || denominator4==0) return Double.NaN;       
        else {
            double loggedDenominator=0.5*(Math.log(denominator1)+Math.log(denominator2)+Math.log(denominator3)+Math.log(denominator4)); // log of square root expression in denominator
            double firstTerm=Math.log(TP)+Math.log(TN)-loggedDenominator;
            double secondTerm=Math.log(FP)+Math.log(FN)-loggedDenominator;
            return (Math.exp(firstTerm)-Math.exp(secondTerm));
        }
   }

    /** Returns the Watson-Crick reverse complementary sequence for the given input sequence */
    public static String reverseSequence(String seq) {
        if (seq==null) return null;
        int length=seq.length();
        StringBuilder buffer=new StringBuilder(seq);
        for (int i=0;i<length;i++) {
            buffer.setCharAt(length-(i+1), reverseBase(seq.charAt(i)) );
        }
        return buffer.toString();
    }

    /** Returns the Watson-Crick reverse complementary sequence for the given input sequence */
    public static StringBuilder reverseSequence(StringBuilder seq) {
        if (seq==null) return null;
        int length=seq.length();
        StringBuilder buffer=new StringBuilder(seq);
        for (int i=0;i<length;i++) {
            buffer.setCharAt(length-(i+1), reverseBase(seq.charAt(i)) );
        }
        return buffer;
    }
    
    /** Returns the Watson-Crick reverse complementary sequence for the given input sequence
     *  The original case for each position is preserved in the returned sequence
     */
    public static char[] reverseSequence(char[] seq) {
        char[] buffer=new char[seq.length];
        for (int i=0;i<seq.length;i++) {
            switch (seq[i]) {
                case 'A':buffer[buffer.length-(i+1)]='T';break;
                case 'a':buffer[buffer.length-(i+1)]='t';break;
                case 'C':buffer[buffer.length-(i+1)]='G';break;
                case 'c':buffer[buffer.length-(i+1)]='g';break;
                case 'G':buffer[buffer.length-(i+1)]='C';break;
                case 'g':buffer[buffer.length-(i+1)]='c';break;
                case 'T':buffer[buffer.length-(i+1)]='A';break;
                case 't':buffer[buffer.length-(i+1)]='a';break;
                default:buffer[buffer.length-(i+1)]=buffer[i];break;
            }
        }
        return buffer;
    }
    
    /** Returns the Watson-Crick reverse complementary sequence for the given input sequence
     *  which may contain IUPAC degenerate symbols. The original case for each position is preserved in the returned sequence
     */
    public static char[] reverseIUPACSequence(char[] seq) {
        char[] buffer=new char[seq.length];
        for (int i=0;i<seq.length;i++) {
            switch (seq[i]) {
                case 'A':buffer[buffer.length-(i+1)]='T';break;
                case 'a':buffer[buffer.length-(i+1)]='t';break;
                case 'C':buffer[buffer.length-(i+1)]='G';break;
                case 'c':buffer[buffer.length-(i+1)]='g';break;
                case 'G':buffer[buffer.length-(i+1)]='C';break;
                case 'g':buffer[buffer.length-(i+1)]='c';break;
                case 'T':buffer[buffer.length-(i+1)]='A';break;
                case 't':buffer[buffer.length-(i+1)]='a';break;                  
                case 'Y':buffer[buffer.length-(i+1)]='R';break;
                case 'y':buffer[buffer.length-(i+1)]='r';break;
                case 'R':buffer[buffer.length-(i+1)]='Y';break;
                case 'r':buffer[buffer.length-(i+1)]='Y';break;
                case 'M':buffer[buffer.length-(i+1)]='K';break;
                case 'm':buffer[buffer.length-(i+1)]='k';break;
                case 'K':buffer[buffer.length-(i+1)]='M';break;
                case 'k':buffer[buffer.length-(i+1)]='m';break;                    
                case 'B':buffer[buffer.length-(i+1)]='V';break;
                case 'b':buffer[buffer.length-(i+1)]='v';break;
                case 'D':buffer[buffer.length-(i+1)]='H';break;
                case 'd':buffer[buffer.length-(i+1)]='h';break;
                case 'H':buffer[buffer.length-(i+1)]='D';break;
                case 'h':buffer[buffer.length-(i+1)]='d';break;
                case 'V':buffer[buffer.length-(i+1)]='B';break;
                case 'v':buffer[buffer.length-(i+1)]='b';break;                                        
                default:buffer[buffer.length-(i+1)]=buffer[i];break; // S and W are their own complements so they are not processed
            }
        }
        return buffer;
    }    
    
    /** Returns the array with the order reversed */
    public static double[] reverseArray(double[] data) {
        double[] buffer=new double[data.length];
        for (int i=0;i<data.length;i++) {
          buffer[buffer.length-(i+1)]=buffer[i];           
        }
        return buffer;
    }       
    
    
   public static char reverseBase(char c) {
      switch (c) {
          case 'A':return 'T';
          case 'C':return 'G';
          case 'G':return 'C';
          case 'T':return 'A';      
          case 'a':return 't';
          case 'c':return 'g';
          case 'g':return 'c';
          case 't':return 'a';      
      } 
      return c; // return same letter if unknown 
   }

   /** Returns 0 for A, 1 for C, 2 for G and 3 for T, else -1*/
   public static int getBaseIndex(char c) {
      switch (c) {
          case 'A': case 'a' : return 0;
          case 'C': case 'c' : return 1;
          case 'G': case 'g' : return 2;
          case 'T': case 't' : return 3;
          default: return -1;
      } 
   }

   
   public static String[] sortCaseInsensitive(String[] list) {
        ArrayList<String> sorted=new ArrayList<String>(Arrays.asList(list));
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);        
        String[] result=new String[sorted.size()];
        result=sorted.toArray(result);
        return result;
   }
   
   
    /**
     * This method takes a string with double-quoted comma-separated values 
    *  and returns a list of the values without the quotes.
     * Note that the values themselves can contain commas within the quotes,
     * and the values can also contain other quotes if these are escaped with backslash
     * E.g. the string:  "CACGTG","CAC.{2,4}TAT","TATAA","TA\"T\"AA"
     * will return a list with the 4 values:
     *     CACGTG
     *     CAC.{2,4}TAT
     *     TATAA
     *     TA"T"AA
     *
     *@throws ExecutionError if the given text does not have the correct format
     */
    public static ArrayList<String> splitQuotedStringListOnComma(CharSequence text) throws ParseError {
        ArrayList<String> result=new ArrayList<String>();
        int laststart=0; // last start of 
        int state=3; // 0=Inside quotes, 1=Outside quotes (pre-comma), 2=Outside quotes (post-comma), 3=Outside quotes (Start state)
        for (int i=0;i<text.length();i++) { // 
            char current=text.charAt(i);
            if (state==0) { // Inside quotes 
                if (current=='"' && text.charAt(i-1)!='\\') { // unescaped quote encountered -> quote is ended!
                    String element=text.subSequence(laststart, i).toString();
                    result.add(element.replace("\\\"", "\"")); // remove escape character from escaped quotes
                    state=1;
                }                
            } else if (state==1) { // Outside quotes (pre-comma)
                if (current==',') {state=2;}
                else if (!Character.isWhitespace(current)) {
                    String context=text.subSequence(i, Math.min(i+20,text.length())).toString();
                    throw new ParseError("Expected comma before \""+context+"...\"");
                }                
            } else if (state==2) { // Outside quotes (post-comma)
                if (current=='"') {state=0;laststart=i+1;}
                else if (!Character.isWhitespace(current)) {
                    String context=text.subSequence(i, Math.min(i+20,text.length())).toString();
                    throw new ParseError("Expected double quote before \""+context+"...\"");
                }                 
            } else { // Start state: search for double quote
                if (current=='"') {state=0;laststart=i+1;}
                else if (!Character.isWhitespace(current)) {
                    String context=text.subSequence(i, Math.min(i+20,text.length())).toString();
                    throw new ParseError("Expected double quote at start of text before \""+context+"...\"");
                }                
            }
        }    
        if (state==0) throw new ParseError("Unexpected end of text (missing end quote)");
        if (state==2) throw new ParseError("Unexpected end of text (ending with comma)");
        return result;
    }
    
    
     /** Takes a string and splits it on every comma found (also commas within quotes) */
     public static ArrayList<String> splitOnCommaSimple(String string) {
        if (string==null || string.trim().isEmpty()) return new ArrayList<String>(0);
        String[] list=string.split("\\s*,\\s*");
        ArrayList<String> result=new ArrayList<String>(list.length);
        result.addAll(Arrays.asList(list));
        return result;
    }   
     
    /** Takes a string of comma-separated values and splits it into a list.
      * However, the method will not split on commas that are found within double quotes.
      * Elements that appear inside double-quotes will have their quotes removed
      */ 
    public static ArrayList<String> splitOnComma(CharSequence text) throws ParseError {
        return splitOnCharacter(text,',');
    }
    
    /**
     * This method takes a string with a list of values separated by the given character
     * and splits it into a list of elements. If the split-character is found inside 
     * an element enclosed in double-quotes, the split-character will be ignored.
     * Elements that appear inside double-quotes will have their quotes removed
     *
     *@throws ExecutionError if the given text does not have the correct format
     */
    public static ArrayList<String> splitOnCharacter(CharSequence text, char splitchar) throws ParseError {
        ArrayList<String> result=new ArrayList<String>();
        int laststart=0; // last start of 
        int state=0; // 0=Outside quotes,1=Inside quotes,
        for (int i=0;i<text.length();i++) { // 
            char current=text.charAt(i);
            if (state==0) { // Outside quotes 
                if (current=='"') {
                    if (i>0 && text.charAt(i-1)=='\\') { // is this quote attempted escaped?
                        String context=text.subSequence(i, Math.min(i+20,text.length())).toString();
                        throw new ParseError("Misplaced escaped quote near: "+context+"...");                        
                    }
                    state=1; // go to "within quotes" state
                }
                else if (current==splitchar) { // comma is outside quotes so we should split on this comma!
                    String element=text.subSequence(laststart, i).toString().trim();
                    if (element.length()>=2 && element.startsWith("\"") && element.endsWith("\"") && !element.endsWith("\\\"")) {element=element.substring(1,element.length()-1);} // remove enclosing quotes
                    result.add(element.replace("\\\"", "\"")); // remove escape character from escaped quotes                    
                    laststart=i+1;
                }              
            } else if (state==1) { // Inside quotes 
                if (current=='"' && (i>0 && text.charAt(i-1)!='\\')) { // unescaped quote encountered -> quote ended
                    state=0; // go to "outside quotes" state
                }                
            } 
        }    
        String element=text.subSequence(laststart, text.length()).toString().trim();
        if (element.length()>=2 && element.startsWith("\"") && element.endsWith("\"") && !element.endsWith("\\\"")) {element=element.substring(1,element.length()-1);} // remove enclosing quotes
        result.add(element.replace("\\\"", "\"")); // remove escape character from escaped quotes                    
        if (state==1) throw new ParseError("Unexpected end of text (missing end quote)");
        return result;
    }

    public static String[] splitOnCommaToArray(CharSequence text) throws ParseError  {
        ArrayList<String> list=splitOnComma(text);
        String[] arrlist=new String[list.size()];
        return list.toArray(arrlist);
    }
    
    /**
     * This method takes a string and splits it up on SPACE characters.
     * However, SPACE characters that appear inside double-quotes  
     * are not considered for separation.
     *
     *@throws ExecutionError if the given text does not have the correct format
     */
    public static ArrayList<String> splitOnSpace(CharSequence text, boolean stripquotes, boolean removeEscapesFromQuotes) throws ParseError {
        ArrayList<String> result=new ArrayList<String>();
        int laststart=0; // last start of
        int state=0; // 0=Outside quotes,1=Inside quotes,
        for (int i=0;i<text.length();i++) { //
            char current=text.charAt(i);
            if (state==0) { // Outside quotes
                if (current=='"') {
                    if (i>0 && text.charAt(i-1)=='\\') { // is this quote attempted escaped?
                        String context=text.subSequence(i, Math.min(i+20,text.length())).toString();
                        throw new ParseError("Misplaced escaped quote near: "+context+"...");
                    }
                    state=1; // go to "within quotes" state
                }
                else if (current==' ') { // space is outside quotes so we should split on this space!
                    String element=text.subSequence(laststart, i).toString().trim();
                    if (!element.isEmpty()) {
                        if (stripquotes) element=stripQuotes(element);
                        if (removeEscapesFromQuotes) element=element.replace("\\\"", "\"");
                        result.add(element);
                    } // remove escape character from escaped quotes
                    laststart=i+1;
                }
            } else if (state==1) { // Inside quotes
                if (current=='"' && (i>0 && text.charAt(i-1)!='\\')) { // unescaped quote encountered -> quote ended
                    state=0; // go to "outside quotes" state
                }
                }
            }
        String element=text.subSequence(laststart, text.length()).toString().trim();
        if (!element.isEmpty()) {
            if (stripquotes) element=stripQuotes(element);
            if (removeEscapesFromQuotes) element=element.replace("\\\"", "\"");
            result.add(element);
        } // remove escape character from escaped quotes
        if (state==1) throw new ParseError("Unexpected end of text (missing end quote)");
        return result;
    }

    /** Strips double-quotes from a string if both the first and last characters in the string are double-quotes */
    public static String stripQuotes(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")) string=string.substring(1,string.length()-1);
        return string;
    }
    
    /** Adds double-quotes to a string unless it is already quoted */
    public static String addQuotes(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")) return string;
        else return "\""+string+"\"";
    }    
    
    /** Strips enclosing characters from a string if they are present */
    public static String stripBraces(String string, String prefix, String suffix) {
        if (string.startsWith(prefix)) string=string.substring(prefix.length());
        if (string.endsWith(suffix)) string=string.substring(0,string.length()-suffix.length());
        return string;
    }    
    
    /**
     * Takes a string and splits it into a list of substrings at the location of the given separator character
     * However, if this character appears within "braces" defined by two brace-characters (open and close)
     * it will be ignored
     * @param text
     * @param separator
     * @param open
     * @param close
     * @return
     * @throws ParseError 
     */
    public static ArrayList<String> splitOnCharacter(CharSequence text, char separator, char open, char close) throws ParseError {
        ArrayList<String> result=new ArrayList<String>();
        int laststart=0; // last start of 
        int state=0; // 0=Outside braces,1=Inside braces,
        for (int i=0;i<text.length();i++) { // 
            char current=text.charAt(i);
            if (state==0) { // Outside braces 
                if (current==open) {
                    state=1; // go to "within braces" state
                }
                else if (current==separator) { // comma is outside braces so we should split on this comma!
                    String element=text.subSequence(laststart, i).toString().trim();
                    result.add(element); //                    
                    laststart=i+1;
                }              
            } else if (state==1) { // Inside braces 
                if (current==close) { //
                    state=0; // go to "outside braces" state
                }                
            } 
        }    
        String element=text.subSequence(laststart, text.length()).toString().trim();
        result.add(element); //                
        if (state==1) throw new ParseError("Unexpected end of text (missing closing brace)");
        return result;
    }    
    

    /** Takes a list of Strings and returns a new string which is a concatenation of
     *  the elements in the original list separated by by a defined separator string
     *  @param list List of strings
     *  @param separator A string which will be inserted between each pair of string in the list
     */
    public static String splice(String[] list, String separator) {
        StringBuilder string=new StringBuilder();
        for (int i=0;i<list.length;i++) {
            string.append(list[i]);
            if (i<list.length-1) string.append(separator);
        }
        return string.toString();
    }
      
    /** Takes a list of Strings and returns a new string which is a concatenation of
     *  the elements in the original list separated by by a defined separator string
     *  @param list List of strings
     *  @param separator A string which will be inserted between each pair of string in the list
     */
    public static String splice(List list, String separator) {
        StringBuilder string=new StringBuilder();
        int size=list.size();
        for (int i=0;i<size;i++) {
            string.append(list.get(i));
            if (i<size-1) string.append(separator);
        }
        return string.toString();
    }
    
    /**
     * If given a string containing a number, this will return a String array
     * where the first element is the part to the left of the (first) number and 
     * the second element is the numeric part and the third element is the part 
     * to the right of the number. 
     * Either the first or third or both of these elements could be empty ("")
     * If the string does not contain a number a NULL value is returned
     * @param string
     * @return 
     */
    public static String[] splitOnNumericPart(String string) {
        Pattern pattern=Pattern.compile("(\\D*?)(\\d+)(\\D*)");
        Matcher matcher=pattern.matcher(string);
        if (matcher.matches()) {
            String prefix=matcher.group(1);
            String number=matcher.group(2);
            String suffix=matcher.group(3);           
            if (prefix==null) prefix="";
            if (suffix==null) suffix="";
            return new String[]{prefix,number,suffix};
        } else return null;
    }
    
    /** Given an object which can be an Integer, Double, String, ArrayList<String> or Boolean     
     *  this method will try to convert the provided value to an object of the given class type if possible
     *  @throws ExecutionError if the target type is not supported or the conversion failed
     */
    public static Object convertToType(Object value, Class type) throws ExecutionError {
        if (type.isAssignableFrom(value.getClass()) || value==null) return value; // no need to convert
        if (type==Integer.class) {
            if (value instanceof Double) return new Integer( ((Double)value).intValue() );
            else try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {}
        } else if (type==Double.class) {
            if (value instanceof Integer) return new Double( ((Integer)value).doubleValue() );
            else try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {}            
        } else if (type==Boolean.class) {
            if (value instanceof Integer) return (((Integer)value).intValue()==0)?Boolean.FALSE:Boolean.TRUE;
            if (value instanceof Double) return (((Double)value).doubleValue()==0)?Boolean.FALSE:Boolean.TRUE;
            if (value instanceof String) {
                String bool=(String)value;
                if (bool.equalsIgnoreCase("TRUE") || bool.equalsIgnoreCase("YES") || bool.equalsIgnoreCase("ON")) return Boolean.TRUE;
                if (bool.equalsIgnoreCase("FALSE") || bool.equalsIgnoreCase("NO") || bool.equalsIgnoreCase("OFF")) return Boolean.FALSE;
            }
        } else if (type==String.class) {
            if (value instanceof ArrayList) return MotifLabEngine.splice((ArrayList<String>)value, ",");
            else return value.toString();
        } else if (type==ArrayList.class) {
           ArrayList<String> newList=new ArrayList<String>(1);
           newList.add(value.toString());
           return newList;
        }
        throw new ExecutionError("Unable to convert "+value.getClass().getSimpleName()+" to "+type.getSimpleName());
    }
    
    /**
     * Returns the index of the first element in the list which has the given value
     * or -1 if none of the values in the list are the same as the one given
     * @param values
     * @return 
     */
    public static int getIndexOfFirstMatch(double[] values, double value) {
        if (values==null || values.length==0) return -1;
        for (int i=0;i<values.length;i++) {
            if (values[i]==value) return i;
        }
        return -1;
    }    
    
    /**
     * Returns the smallest value in a list
     * @param values
     * @return the smallest value in the list (or NaN is the list is empty)
     */
    public static double getMinimumValue(double[] values) {
        if (values==null || values.length==0) return Double.NaN;
        double smallest=values[0];
        for (int i=1;i<values.length;i++) {
            if (values[i]<smallest) smallest=values[i];
        }
        return smallest;
    }    
    
    /**
     * Returns the largest value in a list
     * @param values
     * @return the largest value in the list (or NaN is the list is empty)
     */
    public static double getMaximumValue(double[] values) {
        if (values==null || values.length==0) return Double.NaN;
        double largest=values[0];
        for (int i=1;i<values.length;i++) {
            if (values[i]>largest) largest=values[i];
        }
        return largest;
    } 
    
    public static int getFirstIndexOfSmallestValue(int[] values) {
        if (values==null || values.length==0) return -1;
        int index=0;
        int smallest=values[index];
        for (int i=1;i<values.length;i++) {
            if (values[i]<smallest) {smallest=values[i];index=i;}
        }
        return index;
    }
    
    public static int getFirstIndexOfLargestValue(int[] values) {
        if (values==null || values.length==0) return -1;
        int index=0;
        int largest=values[index];
        for (int i=1;i<values.length;i++) {
            if (values[i]>largest) {largest=values[i];index=i;}
        }
        return index;
    }    
    
    /**
     * Returns the median value of a list of values (which must be sorted in ascending order!)
     * @param sortedvalues
     * @return median
     */
    public static double getMedianValue(double[] sortedvalues) {
        if (sortedvalues.length%2==0) { // even number of values
              double first=sortedvalues[(int)(sortedvalues.length/2.0)-1];
              double second=sortedvalues[(int)(sortedvalues.length/2.0)];
              return (first+second)/2.0;
        } else { // odd number of values
              return sortedvalues[(int)(sortedvalues.length/2.0)];
        }        
    }

    public static double getMedianValue(ArrayList<Double> sortedvalues) {
        if (sortedvalues.size()%2==0) { // even number of values
              double first=sortedvalues.get((int)(sortedvalues.size()/2)-1);
              double second=sortedvalues.get((int)(sortedvalues.size()/2));
              return (first+second)/2.0f;
         } else {
              return sortedvalues.get((int)(sortedvalues.size()/2));
         }     
    }    
    
    /**
     * Returns the 1st quartile value of a list of values (which must be sorted in ascending order!)
     * @param sortedvalues
     * @return 1st quartile
     */
    public static double getFirstQuartileValue(double[] sortedvalues) {     
        return getPercentileValue(sortedvalues, 0.25);
    }  
    
    /**
     * Returns the 1st quartile value of a list of values (which must be sorted in ascending order!)
     * @param sortedvalues
     * @return 1st quartile
     */
    public static double getFirstQuartileValue(ArrayList<Double> sortedvalues) {
        return getPercentileValue(sortedvalues, 0.25);      
    }      
    
    
    /**
     * Returns the 3rd quartile value of a list of values (which must be sorted in ascending order!)
     * @param sortedvalues
     * @return 3rd quartile
     */
    public static double getThirdQuartileValue(double[] sortedvalues) {
        return getPercentileValue(sortedvalues, 0.75);     
    }       
    
     /**
     * Returns the 3rd quartile value of a list of values (which must be sorted in ascending order!)
     * @param sortedvalues
     * @return 3rd quartile
     */   
    public static double getThirdQuartileValue(ArrayList<Double> sortedvalues) {
        return getPercentileValue(sortedvalues, 0.75);       
    }
    
     /**
     * Returns the value of a specified percentile of a list of values (which must be sorted in ascending order!)
     * The rationale behind the implementation is this:
     * The length of the list (L) is multiplied by the percentile (or divided by 1/percentile) which 
     * divides it into two sublists, the first with length L*percentile and the latter with length L*(1-percentile)
     * if the length of the first list (and hence the last list) is NOT an integer, it means that the original
     * list has been divided in the "middle" of a single element, and this is the percentile element whose value
     * will be returned. If the two sublists have integer lengths, this means that the percentile splits the original
     * list exactly between two elements, and the returned value will then be the average of these two.
     * @param sortedvalues
     * @return percentile A number between 0 and 1
     */      
    public static double getPercentileValue(ArrayList<Double> sortedvalues, double percentile) {
        if (sortedvalues.size()==1) return sortedvalues.get(0);
        if (percentile==0) return sortedvalues.get(0); // first value
        
        if (percentile>=1) return sortedvalues.get(sortedvalues.size()-1); // last value
        double L=sortedvalues.size()*percentile;        
        if (L==(int)L) { // integer value?
           int index=(int)(L-1); // subtract one since array is 0-indexed
           if (index<0) index=0;
           return (sortedvalues.get(index)+sortedvalues.get(index+1))/2; // use mean of two values
        } else {
           int index=(int)Math.floor(L); // since L is NOT an integer, this will round down to closest int and make the index 0-based
           if (index<0) index=0;
           return sortedvalues.get(index);           
        }          
    } 
    
     /**
     * Returns the value of a specified percentile of a list of values (which must be sorted in ascending order!)
     * The rationale behind the implementation is this:
     * The length of the list (L) is multiplied by the percentile (or divided by 1/percentile) which 
     * divides it into two sublists, the first with length L*percentile and the latter with length L*(1-percentile)
     * if the length of the first list (and hence the last list) is NOT an integer, it means that the original
     * list has been divided in the "middle" of a single element, and this is the percentile element whose value
     * will be returned. If the two sublists have integer lengths, this means that the percentile splits the original
     * list exactly between two elements, and the returned value will then be the average of these two.
     * @param sortedvalues
     * @return percentile A number between 0 and 1
     */      
    public static double getPercentileValue(double[] sortedvalues, double percentile) {
        if (sortedvalues.length==1) return sortedvalues[0];
        if (percentile==0) return sortedvalues[0]; // first value
        
        if (percentile>=1) return sortedvalues[sortedvalues.length-1]; // last value
        double L=sortedvalues.length*percentile;        
        if (L==(int)L) { // integer value?
           int index=(int)(L-1); // subtract one since array is 0-indexed
           if (index<0) index=0;
           return (sortedvalues[index]+sortedvalues[index+1])/2; // use mean of two values
        } else {
           int index=(int)Math.floor(L); // since L is NOT an integer, this will round down to closest int and make the index 0-based
           if (index<0) index=0;
           return sortedvalues[index];           
        }          
    }     
    
     /**
     * Returns the average and standard deviation (and sum and size) from the list of values
     */      
    public static double[] getAverageAndStandardDeviation(ArrayList<Double> values) {
        double size=values.size();
        double sum=0;
        double devSum=0;
        for (double value:values) {
            sum+=value;
        }
        double average=sum/size;
        for (double value:values) {
            devSum+=(value-average)*(value-average);
        }
        double stddev=(double)Math.sqrt(devSum/size);         
        return new double[]{average,stddev,sum,size};
    }     

    /** Reads a text-based file which can be a local file or a file on the web
     *  (but not a repository file) and returns its contents as a String
     */
    public static String readTextFile(String filename) throws IOException {
        Object input=null;
        if (filename.startsWith("http://") || filename.startsWith("https://") || filename.startsWith("ftp://") || filename.startsWith("ftps://")) {
            input=new URL(filename);
        } else {
           input=new File(filename);
        }
        java.io.InputStream inputStream=null;
        try {
            if (input instanceof File) inputStream = new FileInputStream((File)input);
            else inputStream = ((URL)input).openStream();
            return readTextFile(inputStream);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
           try {
               if (inputStream!=null) inputStream.close();
           } catch (Exception ne) {}
        }
    }
    
    /** Writes the given text string to the local file
     */
    public static void writeTextFile(File file, String text) throws IOException {
        java.io.BufferedWriter outputStream=null;
        try {
            outputStream=new java.io.BufferedWriter(new java.io.FileWriter(file));
            outputStream.write(text);
            outputStream.close();
        } catch (IOException e) {
            throw e;
        } finally {
           try {
               if (outputStream!=null) outputStream.close();
           } catch (Exception ne) {}
        }
    }
    
    public static String readTextFile(InputStream inputStream) throws IOException {
        StringBuilder builder=new StringBuilder();
        java.io.BufferedReader reader=null;
        try {
            BufferedInputStream buffer=new BufferedInputStream(inputStream);
            reader=new java.io.BufferedReader(new java.io.InputStreamReader(buffer));
            String line;
            while((line=reader.readLine())!=null) {
                builder.append(line);
                builder.append("\n");
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
           try {
               if (inputStream!=null) inputStream.close();
               if (reader!=null) reader.close();
           } catch (Exception ne) {}
        }
        return builder.toString();
    }    
    
    
    /** Compares two strings alphanumerically according to their natural order
     */
    public static int compareNaturalOrder(String string1, String string2) {     
        return naturalordercomparator.compare(string1, string2);
    }   
    
    
    /**
     * Removes consecutive duplicate lines from the given list
     * E.g. the list [A,A,G,A,G,G,T,T,T,A,C,C] will return [A,G,A,G,T,A,C]
     * @param lines
     * @param onlyconsecutive if this flag is set, only consecutive duplicates will be removed
     *        but if the same string occurs in different places (with other strings inbetween)
     *        they will be kept
     * @return 
     */
    public static ArrayList<String> removeDuplicateLines(ArrayList<String> lines, boolean onlyconsecutive) {      
        ArrayList<String> result=new ArrayList<String>(lines.size());
        if (lines.isEmpty()) return result;
        if (onlyconsecutive) {
            String last=null;     
            for (String line:lines) {
                if (last==null || !last.equals(line)) {
                    result.add(line);
                    last=line;
                }
            }
        } else {
           HashSet<String> used=new HashSet<String>();
           for (String line:lines) {
              if (!used.contains(line)) {
                  result.add(line);
                  used.add(line);
              }
           }           
        }
        return result;
    }
    
    /* Goes through all the elements in a lists and adds them to new lists. 
     * If any strings contain comma-separated entries, these will be added individually
     * E.g. the list String[]{"one", "two , three", "four, five"} will be flattened to String[]{"one","two","three","four","five"}
     */
    public static String[] flattenCommaSeparatedSublists(String[] list) {
        ArrayList<String> newlist=new ArrayList<>();
        for (String entry:list) {
            if (entry!=null && entry.contains(",")) {
                newlist.addAll(Arrays.asList(entry.trim().split("\\s*,\\s*")));
            } else newlist.add(entry);
        }
        String[] result=new String[newlist.size()];
        return newlist.toArray(result);
    }
    
    /** removes duplicate entries in a String list */
    public static String[] removeDuplicates(String[] list, boolean removeEmpty) {
        if (list==null || list.length==0) return list;
        ArrayList<String> newlist=new ArrayList<>(list.length);
        for (String l:list) {
            if (newlist.contains(l) || (removeEmpty && (l==null || l.isEmpty()))) continue;
            newlist.add(l);
        }
        String[] result=new String[newlist.size()];
        return newlist.toArray(result);
    }
    
    /** removes duplicate entries in a String list */
    public static int[] removeDuplicates(int[] list) {
        if (list==null || list.length==0) return list;
        ArrayList<Integer> newlist=new ArrayList<>(list.length);
        for (Integer l:list) {
            if (!newlist.contains(l)) newlist.add(l);
        }
        int[] result=new int[newlist.size()];
        for (int i=0;i<result.length;i++) {
            result[i]=newlist.get(i);
        }
        return result;
    }    
    
    /** Unzips a ZIP-file to a target directory 
     *  @param directory The target directory where the extracted files should be placed (if NULL the file will be extracted to the same directory where the ZIP-file currently resides)
     *  @return A list containing the newly extracted files (including directories)
     */
    public ArrayList<File> unzipFile(File sourcefile, File directory) throws IOException {
         ArrayList<File> newfiles=new ArrayList<File>();
         if (directory==null) directory=sourcefile.getParentFile();
         if (directory instanceof DataRepositoryFile) throw new IOException("ZIP-files can not be extracted to data repositories"); //... at least not yet
         if (sourcefile instanceof DataRepositoryFile) { // just make a local copy of the ZIP-file. That will be easier to work with
               File tempfile=createTempFile();
               InputStream stream=((DataRepositoryFile)sourcefile).getFileAsInputStream();
               Files.copy(stream, tempfile.toPath(), StandardCopyOption.REPLACE_EXISTING); 
               sourcefile=tempfile;
         }
         int BUFFER = 2048;
         BufferedOutputStream dest = null;
         BufferedInputStream is = null;
         ZipEntry entry;
         ZipFile zipfile = new ZipFile(sourcefile);
         Enumeration e = zipfile.entries();
         while(e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();

            is = new BufferedInputStream (zipfile.getInputStream(entry));
            int count;
            byte data[] = new byte[BUFFER];       
            File outfile=new File(directory,entry.getName());
            //System.err.println("Extracting: " +entry+"      to => "+outfile.getAbsolutePath());
            newfiles.add(outfile);
            outfile.getParentFile().mkdirs();
            if (entry.isDirectory()) {
                outfile.mkdir();
                outfile.setExecutable(true);
                outfile.setReadable(true);
                outfile.setWritable(true);            
                continue;
            }
            FileOutputStream fos = new FileOutputStream(outfile);
            dest = new BufferedOutputStream(fos, BUFFER);
            while ((count = is.read(data, 0, BUFFER))!= -1) {
               dest.write(data, 0, count);
            }
            dest.flush();
            dest.close();
            is.close();
            if (outfile.exists()) {// just in case?
                outfile.setExecutable(true);
                outfile.setReadable(true);
                outfile.setWritable(true);
            }
         }
         zipfile.close();
         return newfiles;
    }

    
    public static boolean startsWithIgnoreCase(String string, String prefix) {
        int prefixLength=prefix.length();
        if (prefixLength>string.length()) return false;
        for (int i=0;i<prefixLength;i++) {
           if (Character.toLowerCase(string.charAt(i))!=Character.toLowerCase(prefix.charAt(i))) return false;
        }        
        return true;
    }    
 
    /** 
     * Takes an "INI" configuration file as input (in the form of a list of Strings)
     * and returns a HashMap of key=value configuration pairs.
     * Note that although the returned map has "Object" values, the actual values
     * returned by this method will all be Strings so any type conversions will
     * have to be performed later
     * @return a HashMap 
     */
    public static HashMap<String,Object> parseINIfile(ArrayList<String> inifile) {
        HashMap<String,Object> map=new HashMap<String,Object>();
        for (String string:inifile) {
            string=string.trim(); // just in case
            if (string.isEmpty()) continue;
            if (!Character.isLetter(string.charAt(0))) continue; // ignore all lines not starting with a regular letter. These lines can be comments or section headers (or just mistakes)
            String[] pair=string.trim().split("\\s*=\\s*",2);
            if (pair.length==2) map.put(pair[0],pair[1]);
        }
        return map;
    }
    
    
    /**
     * Copies an existing file to a new destination. Both the source file and the destination files can be DataRepositoryFiles
     * @param source
     * @param destination
     * @throws Exception 
     */
    public static void copyFile(File source, File destination) throws Exception {
        InputStream inputstream=null;
        OutputStream outputstream=null;
        try {
            inputstream=MotifLabEngine.getInputStreamForFile(source);
            outputstream=MotifLabEngine.getOutputStreamForFile(destination);
            IOUtils.copy(inputstream,outputstream);
        } catch (Exception e) {
            throw e;
        }
        finally {
           if (inputstream!=null) inputstream.close();
           if (outputstream!=null) outputstream.close();
        }     
    }
    
    /**
     * Takes a time in milliseconds and formats it as "?h ?m ?s"
     * Hours and minutes are skipped if the time span is too small
     * For times less than 10 the milliseconds are also included
     * @param milliseconds
     * @return 
     */
    public static String formatTime(long milliseconds) {
            String string="";
            int seconds = (int) (milliseconds / 1000) % 60 ;
            int minutes = (int) ((milliseconds / (1000*60)) % 60);
            int hours   = (int) ((milliseconds / (1000*60*60)));
            if (hours>0) string = hours+"h ";
            if (minutes>0 || hours>0) string+=minutes+"m ";
            int milli=(int) (milliseconds) % 1000 ;
            if (milliseconds<10000) string=seconds+"."+milli+"s"; // use milliseconds if time is less than 10 seconds
            else string+=seconds+"s";  
            return string;
    }    
    
    /** Returns a string representation of a number where digits have been grouped using commas (3 digits in each group starting from the back) */
    public static String groupDigitsInNumber(int number) {
        boolean negative=(number<0);
        if (negative) number=-number;
        String numberAsString=""+number;
        String prefix=(negative)?"-":"";
        String buffer="";
        while (numberAsString.length()>3) {
            int cutoff=numberAsString.length()-3;          
            String group=numberAsString.substring(cutoff);
            numberAsString=numberAsString.substring(0,cutoff);            
            buffer=group+buffer;
            if (numberAsString.length()>0) buffer=(","+buffer);
        }
        return prefix+numberAsString+buffer;
    }
    
    /**
     * Breaks up a long text string by replacing some of the spaces with linebreak characters.
     * Note that the string must contain spaces at sufficient intervals for this to work well.
     * If no spaces are found, the last word on the new line will be forcibly split up with hyphens (in non-optimal places)
     * @param line The original line of text
     * @param maxlength The maximum number of characters on each line. This is a hard limit!
     * @param linebreak The string to insert in order to introduce a linebreak. This could be e.g. \n or <br> depending on the context
     * @return The original string with linebreaks introduced
     */
    public static String breakLine(String line, int maxlength, String linebreak) {
        StringBuilder newline=new StringBuilder();
        if (line.length()<=maxlength) return line; // no need to break it at all
        boolean done=false;
        int lastbreak=0;
        int pos=maxlength;
        int startpoint=pos;  
        while (!done) {
            //System.err.println("   breakLine: new iteration. Last break="+lastbreak+", pos="+pos+", startpoint="+startpoint+"  maxlength="+maxlength);            
            while (pos>lastbreak && line.charAt(pos)!=' ') {
                pos--;
            }
            //System.err.println("   breakLine: - new pos = "+pos);
            // the pos pointer should now either be at a space or have been backtracked to the previous breakpoint
            if (pos==lastbreak) { // did not find a space by backtracking. Insert hyphen at end of line
                pos=startpoint-1; // reset to "full line" but subtract 1 to make room for hyphen
                newline.append(line.substring(lastbreak, pos)); 
                newline.append("-");             
                newline.append(linebreak);        
            } else if (line.charAt(pos)==' ') { // found a space
                newline.append(line.substring(lastbreak, pos));
                newline.append(linebreak);
                pos++; // skip past the space
            }
            if (pos+maxlength>=line.length()) { // rest of text is smaller than maxlength. Just add the rest and we are done
                //System.err.println("   breakLine: adding rest of string. line length="+line.length()+".   Pos+max="+(pos+maxlength));
                // the pos pointer should now either be at a space o                
                newline.append(line.substring(pos));
                done=true;
            } else {
                lastbreak=pos; // this should be 
                startpoint=pos+maxlength;
                pos=startpoint;
                //System.err.println("   breakLine: preparing for new iteration. Last break="+lastbreak+", pos="+pos+", startpoint="+startpoint);
            }
        }
        return newline.toString();
    }
    
    /**
     * This method tries to open a connection to the provided URL. 
     * If successful it will return the same URL. 
     * If the protocol is HTTP(S) and the request was redirected (status 3xx) the new location will be returned
     * If an error occurs while trying to connect to the server, the original URL will be returned 
     * (or as far as it was able to resolve before the error occurred)
     * @param url
     * @return 
     */
    public static URL resolveURL(URL url) {
        String protocol=url.getProtocol();
        if (!protocol.startsWith("http")) return url;
        try {
 	    HttpURLConnection connection = (HttpURLConnection)url.openConnection();            
            int status = connection.getResponseCode();
            if (status>=300 && status<400) {               
                String newUrl = connection.getHeaderField("Location"); // get redirect url from "location" header field
                url=resolveURL(new URL(newUrl)); // try recursively in case you are redirected multiple times
            } 
        } catch (IOException iox) {
            return url;
        }
	return url;       
    }
    
    
    /** Writes the given line to STDERR after first indenting it */
    public static void debugOutput(String line, int indent) {
        for (int i=0;i<indent;i++) {System.err.print("    ");} // each level indents 4 spaces
        System.err.println(line);
    }
}
