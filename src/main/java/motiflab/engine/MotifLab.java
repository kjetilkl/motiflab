package motiflab.engine;

import motiflab.engine.task.OperationTask;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.task.AddSequencesTask;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.jdesktop.application.Application;
import motiflab.engine.data.*;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.dataformat.DataFormat_BED;
import motiflab.engine.dataformat.DataFormat_FASTA;
import motiflab.engine.dataformat.DataFormat_INCLUSive_Background;
import motiflab.engine.dataformat.DataFormat_Location;
import motiflab.engine.datasource.DataTrack;
import motiflab.engine.operations.Operation;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.operations.PromptConstraints;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.SerializedStandardProtocol;
import motiflab.engine.protocol.StandardDisplaySettingsParser;
import motiflab.engine.protocol.StandardProtocol;
import motiflab.gui.MotifLabApp;
import motiflab.gui.VisualizationSettings;

/**
 * This class implements the command-line interface (CLI) version of MotifLab 
 * (The class name was chosen so that it would look better on the command line)
 * Other client interface implementations are the GUI (MotifLabGUI) and minimal GUI (MinimalGUI)
 * The "main" class of MotifLab is called the "engine" (MotifLabEngine)
 * 
 * @author kjetikl
 */
public class MotifLab implements MotifLabClient, PropertyChangeListener {
    
    private static int DO_NOT_RETRY=-2;
    private static int PROMPT_RETRY=-1;    
    
    private static int LOGGER_MODE_SILENT=0;
    private static int LOGGER_MODE_BRIEF=1;
    private static int LOGGER_MODE_VERBOSE=2;
    
    private ClientConsole clientConsole;
    
    private MotifLabEngine engine;
    private String protocolFilename;
    private String sequencesFilename;
    private String outputdirname;
    private String inputSessionFilename; // used if the engine should be populated from an existing session    
    private String outputSessionFilename; // name of file to save current session to
    private BufferedWriter logger=null;
    private int loggerMode=LOGGER_MODE_BRIEF;
    private boolean silentmode=false; // in silent mode the user will not be prompted for values. Default values will be used in stead.
    private boolean saveoutput=true; // if saveoutput is true all output data objects existing when the protocol finishes execution will be saved to files (with default file names unless otherwise specified)
    private VisualizationSettings visualizationsettings=null; // used to set colors for some graphs, etc.
    private int retry=PROMPT_RETRY; // Number of seconds to wait before automatic retry after errors. Special values: PROMPT_RETRY means prompt and DO_NOT_RETRY means abort 
    private StandardProtocol currentProtocol=null;
    private HashMap<String,String> promptInjection=null; // can be used to specify values for data objects prompted in the protocol on the command line rather than interactively 
    private HashMap<String,String> renameOutput=null; // can be used to specify alternative names for output files
    private HashMap<String,String> macros=null; // can be used to specify alternative names for output files
    private HashMap<String,String[]> pluginConfigurations=null; // used to specify config values for plugins
    private int splitGroupSize=0; // if this number is larger than 0, MotifLab should split the sequences into groups of this size (or smaller) and process each group individually in turn (to save resources when processing many sequences)
    private String defaultGenomeBuild=null; // this can be provided as a CLI-option
    private String sequenceFormat=null;
    private boolean assignNewSequenceNames=false;
    
    private static final int BED=0;
    private static final int CUSTOM_BED=1;
    
    
    public MotifLab(MotifLabEngine engine) {
        this(engine, new STDConsole(), true);      
    }
    
    public MotifLab(MotifLabEngine engine, ClientConsole console, boolean addShutdownHook) {
        this.engine=engine;
        engine.setClient(this);
        clientConsole=(console!=null)?(console):new STDConsole();
        if (addShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() { shutdown(); }
            });         
        }
    }    
    
    
    public void setConsole(ClientConsole console) {
        this.clientConsole=console;
    }
    
    public void setProtocolFilename(String protocol) {
        this.protocolFilename=protocol;
    }
    
    public void setSequencesFilename(String sequencesfilename) {
        this.sequencesFilename=sequencesfilename;
    }
    
    /** Specifies a BufferedWriter that should log all messages passed to the client */
    public void setLogger(BufferedWriter logger) {
        this.logger=logger;
    }
    
    /** Sets the level of verbosity used by the logger. 0=Silent, 1=brief, 2=verbose */
    public void setLoggerVerbosity(int mode) {
        if (mode>=0 && mode<=2) this.loggerMode=mode;
    }    
    
    public void setPromptInjections(HashMap<String,String> injections) {
        this.promptInjection=injections;
    }
    
    public void setPluginConfigurations(HashMap<String,String[]> pluginConfigs) {
        this.pluginConfigurations=pluginConfigs;
    }     

    /** Specifies a BufferedWriter that should log all messages passed to the client */
    public void setOutputDirectoryName(String outputdirname) {
        this.outputdirname=outputdirname;
        if (outputdirname!=null) {
            File dir=engine.getFile(outputdirname);
            if (dir!=null && !dir.exists()) dir.mkdirs(); // just in case we forget 
        }
    }
    
    /** Specifies the filename of a session which should be used to populate the engine initially */
    public void setInputSession(String sessionFilename) {
        this.inputSessionFilename=sessionFilename;
    }   
    
    /** Specifies the filename that the current session should be saved to before exit */
    public void setOutputSession(String sessionFilename) {
        this.outputSessionFilename=sessionFilename;
    }     
      
    public void setMacros(HashMap<String,String> macros) {
        if (engine==null || macros==null || macros.isEmpty()) return;
        engine.setMacros(macros);
        if (this.loggerMode==2) {
            logMessage("The following macros have been set:");
            for (String macro:macros.keySet()) {
                logMessage("  * "+macro+"="+macros.get(macro));
            }
        }
    }
    
    @Override
    public boolean handleUncaughtException(Throwable e) {
        if (e instanceof OutOfMemoryError) {
            errorMessage("SYSTEM ERROR: Out of memory", 0);
        } else if (e instanceof ExceptionInInitializerError) {
            return false; // I think this one can happen if the user tries to abort the program while running (e.g. with CTRL+C on Unix systems)
        } else {
            errorMessage("SYSTEM ERROR: Fatal uncaught exception '"+e.getClass().getSimpleName()+"': "+e.getMessage(), 0);
            engine.reportError(e);
        }
        return false;
    }

    @Override
    public void initializeClient(MotifLabEngine engine) {
        initializePlugins();
    }

    @Override
    public VisualizationSettings getVisualizationSettings() {
        if (visualizationsettings==null) visualizationsettings=VisualizationSettings.getInstance(this);
        return visualizationsettings;
    }

    @Override
    public MotifLabEngine getEngine() {
        return engine;
    }

    @Override
    public Data promptValue(Data item, String message, PromptConstraints constraints) throws ExecutionError {
        if (item==null) throw new ExecutionError("Unknown datatype for prompt");
        if (promptInjection!=null && !promptInjection.isEmpty()) { // Have any data items been specified on the command line?
            if (!promptInjection.containsKey(item.getName())) return item; // This one is not explicitly specified. Just use the default
            String value=promptInjection.get(item.getName());
            if (item instanceof NumericVariable) { // check if the value is a constant
                try { 
                    double numVal=Double.parseDouble(value);
                    if (constraints!=null) {String veto=constraints.isValueAllowed(numVal,engine); if (veto!=null) throw new ExecutionError("Value constraint error: "+veto);}
                    ((NumericVariable)item).setValue(numVal);
                    return item;
                } catch (NumberFormatException e) {throw new ExecutionError("Not a valid number: "+value);}
            } if (item instanceof TextVariable && !value.startsWith("file:")) { // TextVariables are read from file only if prefixed by "file:", else use the provided text-value directly
                ((TextVariable)item).setValue(new String[]{value});
                if (constraints!=null) {String veto=constraints.isValueAllowed(value,engine); if (veto!=null) throw new ExecutionError("Value constraint error: "+veto);}
                return item;
            } else { // read data from file
                if (value.startsWith("file:")) value=value.substring("file:".length());
                File file=engine.getFile(value);
                if (!file.exists()) throw new ExecutionError("File not found: "+value);               
                try { // note that constraints are not enforced here
                         if (item instanceof SequenceCollection)  return getDataCollectionFromFile((SequenceCollection)item,file);
                    else if (item instanceof MotifCollection)  return getMotifCollectionFromFile(item.getName(),file);
                    else if (item instanceof ModuleCollection) return getModuleCollectionFromFile(item.getName(),file);
                    else if (item instanceof DataPartition) return getDataPartitionFromFile((DataPartition)item,file);
                    else if (item instanceof NumericMap) return getNumericMapFromFile((NumericMap)item,file);
                    else if (item instanceof TextMap) return getTextMapFromFile((TextMap)item,file);
                    else if (item instanceof TextVariable) return getTextVariableFromFile((TextVariable)item,file);
                    else if (item instanceof BackgroundModel) return getBackgroundModelFromFile(item.getName(),file);
                    else if (item instanceof ExpressionProfile) return getExpressionProfileFromFile((ExpressionProfile)item,file,"\t");
                    else if (item instanceof PriorsGenerator) return getPriorsGeneratorFromFile((PriorsGenerator)item,file);
                    else if (item instanceof FeatureDataset) {
                        OperationTask dummyTask=new OperationTask("load track");
                        String oldname=item.getName();
                        DataFormat format=engine.getDefaultDataFormat(item);                        
                        item=(FeatureDataset)engine.getDataLoader().loadData(file, item.getDynamicType(), item, format, null, dummyTask);
                        item.rename(oldname);    
                        return item;
                    }
                } catch (Exception ex) {
                    throw new ExecutionError(ex.getMessage());
                }                                                
            }
        } // end of prompt-injection
        // No data items specified on the command line. Prompt the user interactively
        Data copy=item.clone(); // this is necessary
             if (item instanceof SequenceCollection) return promptValueSequenceCollection((SequenceCollection)copy, message);
        else if (item instanceof MotifCollection) return promptValueMotifCollection((MotifCollection)copy, message);
        else if (item instanceof ModuleCollection) return promptValueModuleCollection((ModuleCollection)copy, message);
        else if (item instanceof DataPartition) return promptValueDataPartition((DataPartition)copy, message);
        else if (item instanceof NumericMap) return promptValueNumericMap((NumericMap)copy, message);
        else if (item instanceof TextMap) return promptValueTextMap((TextMap)copy, message);
        else if (item instanceof BackgroundModel) return promptValueBackgroundModel((BackgroundModel)copy, message);
        else if (item instanceof NumericVariable) return promptValueNumericVariable((NumericVariable)copy, message, constraints);
        else if (item instanceof TextVariable) return promptValueTextVariable((TextVariable)copy, message, constraints);
        else if (item instanceof ExpressionProfile) return promptValueExpressionProfile((ExpressionProfile)copy, message);
        else if (item instanceof PriorsGenerator) return promptValuePriorsGenerator((PriorsGenerator)copy, message);
        else if (item instanceof FeatureDataset) return promptValueFeatureDataset((FeatureDataset)copy, message);
        else throw new ExecutionError("Unable to prompt for values for data objects of type: "+item.getTypeDescription());
    }

    
    private Data promptValueSequenceCollection(SequenceCollection collection, String message) {
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease select sequences for collection '"+collection.getName()+"': ");
        else clientConsole.println("\n"+message+": ");
       
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("\nPlease select one of the following options");
                clientConsole.println(" 1) Use the default value for the collection (currently including "+collection.size()+" sequence(s))");        
                clientConsole.println(" 2) Load the sequence collection from a file (in Plain-format)");
                clientConsole.println(" 3) Specify which sequences to include in the collection by answering YES or NO to each sequence");
                clientConsole.println(" 4) Enter the names of the sequences to include in the collection manually");
                clientConsole.println(" 0) See the default value for the collection");                
                showUsage=false;
            }
            clientConsole.print("\nEnter a number (0-4): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                collection.clearAll(engine);
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            getDataCollectionFromFile(collection,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("3")) { // answer Yes/No for each sequence
                collection.clearAll(engine);
                ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
                Collections.sort(sequences, new Comparator<Data>() {
                    @Override
                    public int compare(Data o1, Data o2) {
                       return o1.getName().compareTo(o2.getName());
                    }               
                });
                for (Data sequence:sequences) {
                    clientConsole.print("Include sequence '"+sequence.getName()+"' [Y/n]: ");
                    String userInput=clientConsole.readLine();
                    if (userInput==null || userInput.trim().isEmpty()) collection.addSequence((Sequence)sequence);
                    else if (userInput.startsWith("y") || userInput.startsWith("Y")) collection.addSequence((Sequence)sequence);
                }
                ok=true;
            } else if (selectedOption.equals("4")) {
                collection.clearAll(engine);
                clientConsole.println("\nPlease enter the names of the sequences you want to include in the collection.");
                clientConsole.println("You can enter several names on each line by separating them with commas.");
                clientConsole.println("Finish by entering a blank line.");
                clientConsole.println("At any time you can type \"+\" on a line of its own to see which sequences have been included so far,");
                clientConsole.println("or type \"?\" on a line of its own to see which sequences are available.");
                clientConsole.println("");
                boolean finished=false;
                while (!finished) {
                   clientConsole.print("Enter sequence names: "); 
                   String userInput=clientConsole.readLine();
                   userInput=userInput.trim();
                   if (userInput.isEmpty()) {ok=true;finished=true;}
                   else if (userInput.equals("+")) {
                      String output=collection.output().trim();
                      output.replace(",", "\n");
                      clientConsole.println("The collection currently includes "+collection.size()+" sequence(s):");
                      clientConsole.println(output);
                      clientConsole.println("");
                   } else if (userInput.equals("?")) {
                       ArrayList<String> sequences=engine.getNamesForAllDataItemsOfType(Sequence.class);      
                       String output=MotifLabEngine.splice(sequences, ",");
                       clientConsole.println("Available sequences:");
                       clientConsole.println(output);
                       clientConsole.println("");                       
                   } else {
                      String[] entries=userInput.split("\\s*,\\s*");
                      for (String entry:entries) {
                          Data data=engine.getDataItem(entry);
                          if (data instanceof Sequence) collection.addSequence((Sequence)data);
                          else clientConsole.println("ERROR: '"+entry+"' is not a known sequence");
                      }
                   }             
                }           
            } else if (selectedOption.equals("0")) {
                String output=collection.output().trim();
                output.replace(",", "\n");
                clientConsole.println("The collection currently includes "+collection.size()+" sequence(s):");
                clientConsole.println(output);
                clientConsole.println("");
            } else if (selectedOption.isEmpty()) showUsage=true;
        }

        return collection;
    }
    
    private Data promptValueMotifCollection(MotifCollection collection, String message) throws ExecutionError {
        System.err.println("promptValueMotifCollection");
        clientConsole.println("");
        String dataname=collection.getName();
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease select motifs for collection '"+collection.getName()+"': ");
        else clientConsole.println("\n"+message+": ");  
        boolean hasMotifs=engine.hasDataItemsOfType(Motif.class);      
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("\nPlease select one of the following options");
                clientConsole.println(" 1) Use the default value for the collection (currently including "+collection.size()+" motif(s))");        
                clientConsole.println(" 2) Select from a list of predefined motif collections");
                clientConsole.println(" 3) Load the motif collection from a file (containing motif definitions)");
                if (hasMotifs) {
                    clientConsole.println(" 4) Load a list of motif names from file (in Plain-format)");
                    clientConsole.println(" 5) Specify which motifs to include in the collection by answering YES or NO to each motif");
                    clientConsole.println(" 6) Enter the names of the motifs to include in the collection manually");
                }
                clientConsole.println(" 0) See the default value for the collection");                   
                showUsage=false;
            }
            clientConsole.print("Enter a number (0-"+(hasMotifs?"6":"3")+"): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                clientConsole.println("Select one of the following predefined motif collections:");
                collection.clearAll(engine);
                Set<String> predef=engine.getPredefinedMotifCollections();
                String[] collectionNames=new String[predef.size()];
                collectionNames=predef.toArray(collectionNames);  
                Arrays.sort(collectionNames);
                for (int i=0;i<collectionNames.length;i++) {
                    String name=collectionNames[i];
                    int size=engine.getSizeForMotifCollection(name);
                    clientConsole.println("["+(i+1)+"] "+name+" ("+size+" motifs)");
                }       
                boolean predefOK=false;
                while (!predefOK) {
                    clientConsole.print("Enter a number (1-"+collectionNames.length+", or 0 to go back): ");
                    String userInput=clientConsole.readLine();
                    if (userInput!=null && !userInput.trim().isEmpty()) {
                        try { // 
                            int selection=Integer.parseInt(userInput);
                            if (selection>0 && selection<=collectionNames.length) {
                                String collectionName=collectionNames[selection-1];
                                collection=getPredefinedMotifCollection(collection.getName(),collectionName);
                                predefOK=true; ok=true;
                            } else if (selection==0) {predefOK=true;showUsage=true;}
                        } catch (NumberFormatException e) { //                     
                             // just go back in loop 
                        }
                    }
                } // while!predefOK
            } else if (selectedOption.equals("3")) {
                collection.clearAll(engine);
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            collection=getMotifCollectionFromFile(dataname,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("4") && hasMotifs) { // add list from file
                collection.clearAll(engine);
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            getDataCollectionFromFile(collection,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("5") && hasMotifs) { // answer Yes/No for each sequence
                collection.clearAll(engine);
                ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
                Collections.sort(motifs, new Comparator<Data>() {
                    @Override
                    public int compare(Data o1, Data o2) {
                       return o1.getName().compareTo(o2.getName());
                    }               
                });                
                for (Data motif:motifs) {
                    clientConsole.print("Include motif '"+motif.getName()+"' [Y/n]: ");
                    String userInput=clientConsole.readLine();
                    if (userInput==null || userInput.trim().isEmpty()) collection.addMotif((Motif)motif);
                    else if (userInput.startsWith("y") || userInput.startsWith("Y")) collection.addMotif((Motif)motif);
                }
                ok=true;
            } else if (selectedOption.equals("6") && hasMotifs) {
                collection.clearAll(engine);
                clientConsole.println("Please enter the names of the motifs you want to include in the collection.");
                clientConsole.println("You can enter several names on each line by separating them with commas.");
                clientConsole.println("Finish by entering a blank line.");
                clientConsole.println("At any time you can type \"+\" on a line of its own to see which motifs have been included so far,");
                clientConsole.println("or type \"?\" on a line of its own to see which motifs are available.");
                clientConsole.println("");
                boolean finished=false;
                while (!finished) {
                   clientConsole.print("Enter motif names: "); 
                   String userInput=clientConsole.readLine();
                   userInput=userInput.trim();
                   if (userInput.isEmpty()) {ok=true;finished=true;}
                   else if (userInput.equals("+")) {
                      String output=collection.output().trim();
                      output.replace(",", "\n");
                      clientConsole.println("The collection currently includes "+collection.size()+" motif(s):");
                      clientConsole.println(output);
                      clientConsole.println("");
                   } else if (userInput.equals("?")) {
                       ArrayList<String> motifs=engine.getNamesForAllDataItemsOfType(Motif.class);      
                       String output=MotifLabEngine.splice(motifs, ",");
                       clientConsole.println("Available motifs:");
                       clientConsole.println(output);
                       clientConsole.println("");                       
                   } else {
                      String[] entries=userInput.split("\\s*,\\s*");
                      for (String entry:entries) {
                          Data data=engine.getDataItem(entry);
                          if (data instanceof Motif) collection.addMotif((Motif)data);
                          else clientConsole.println("ERROR: '"+entry+"' is not a known motif");
                      }
                   }             
                }           
            } else if (selectedOption.equals("0")) {
                String output=collection.output().trim();
                output.replace(",", "\n");
                clientConsole.println("The collection currently includes "+collection.size()+" motifs(s):");
                clientConsole.println(output);
                if (collection.isPredefined()) {
                   clientConsole.println("The collection is a predefined motif collection named '"+collection.getPredefinedCollectionName()+"'");            
                }
                clientConsole.println("");
            } else if (selectedOption.isEmpty()) showUsage=true;
        }      
        return collection;          
    }
    
    private Data promptValueModuleCollection(ModuleCollection collection, String message) throws ExecutionError {
        clientConsole.println("");
        String dataname=collection.getName();
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease select modules for collection '"+collection.getName()+"': ");
        else clientConsole.println("\n"+message+": ");  
        boolean hasModules=engine.hasDataItemsOfType(ModuleCRM.class);
        
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("\nPlease select one of the following options");
                clientConsole.println(" 1) Use the default value for the collection (currently including "+collection.size()+" module(s))");        
                clientConsole.println(" 2) Load the module collection from a file (containing module definitions)");
                if (hasModules) {
                    clientConsole.println(" 3) Load a list of module names from file (in Plain-format)");
                    clientConsole.println(" 4) Specify which modules to include in the collection by answering YES or NO to each module");
                    clientConsole.println(" 5) Enter the names of the modules to include in the collection manually");
                }
                clientConsole.println(" 0) See the default value for the collection");  
                showUsage=false;
            }            
            clientConsole.print("Enter a number (0-"+(hasModules?"5":"2")+"): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                collection.clearAll(engine);
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            collection=getModuleCollectionFromFile(dataname,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("3") && hasModules) { // add list from file
                collection.clearAll(engine);
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            getDataCollectionFromFile(collection,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("4") && hasModules) { // answer Yes/No for each sequence
                collection.clearAll(engine);
                ArrayList<Data> modules=engine.getAllDataItemsOfType(ModuleCRM.class);
                Collections.sort(modules, new Comparator<Data>() {
                    @Override
                    public int compare(Data o1, Data o2) {
                       return o1.getName().compareTo(o2.getName());
                    }               
                });                
                for (Data cisregModule:modules) {
                    clientConsole.print("Include module '"+cisregModule.getName()+"' [Y/n]: ");
                    String userInput=clientConsole.readLine();
                    if (userInput==null || userInput.trim().isEmpty()) collection.addModule((ModuleCRM)cisregModule);
                    else if (userInput.startsWith("y") || userInput.startsWith("Y")) collection.addModule((ModuleCRM)cisregModule);
                }
                ok=true;
            } else if (selectedOption.equals("5") && hasModules) {
                collection.clearAll(engine);
                clientConsole.println("Please enter the names of the modules you want to include in the collection.");
                clientConsole.println("You can enter several names on each line by separating them with commas.");
                clientConsole.println("Finish by entering a blank line.");
                clientConsole.println("At any time you can type \"+\" on a line of its own to see which modules have been included so far,");
                clientConsole.println("or type \"?\" on a line of its own to see which modules are available.");
                clientConsole.println("");
                boolean finished=false;
                while (!finished) {
                   clientConsole.print("Enter module names: "); 
                   String userInput=clientConsole.readLine();
                   userInput=userInput.trim();
                   if (userInput.isEmpty()) {ok=true;finished=true;}
                   else if (userInput.equals("+")) {
                      String output=collection.output().trim();
                      output.replace(",", "\n");
                      clientConsole.println("The collection currently includes "+collection.size()+" module(s):");
                      clientConsole.println(output);
                      clientConsole.println("");
                   } else if (userInput.equals("?")) {
                       ArrayList<String> modules=engine.getNamesForAllDataItemsOfType(ModuleCRM.class);      
                       String output=MotifLabEngine.splice(modules, ",");
                       clientConsole.println("Available modules:");
                       clientConsole.println(output);
                       clientConsole.println("");                       
                   } else {
                      String[] entries=userInput.split("\\s*,\\s*");
                      for (String entry:entries) {
                          Data data=engine.getDataItem(entry);
                          if (data instanceof ModuleCRM) collection.addModule((ModuleCRM)data);
                          else clientConsole.println("ERROR: '"+entry+"' is not a known module");
                      }
                   }             
                }           
            } else if (selectedOption.equals("0")) {
                String output=collection.output().trim();
                output.replace(",", "\n");
                clientConsole.println("The collection currently includes "+collection.size()+" modules(s):");
                clientConsole.println(output);
                clientConsole.println("");
            } else if (selectedOption.isEmpty()) showUsage=true;
        }      
        return collection;          
    }  
    
       
    private Data promptValueBackgroundModel(BackgroundModel item, String message) throws ExecutionError {
        clientConsole.println("");
        String dataname=item.getName();

        if (message==null || message.isEmpty()) clientConsole.println("\nPlease select a background model for '"+item.getName()+"': ");
        else clientConsole.println("\n"+message+": ");  
                     
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("\nPlease select one of the following options");
                clientConsole.println(" 1) Use the default value for the background model ("+item.getTypeDescription()+")");        
                clientConsole.println(" 2) Select from a list of predefined background models");
                clientConsole.println(" 3) Load the background model from a file");
                clientConsole.println(" 0) See the default value for the background model");
                showUsage=false;
            }
            clientConsole.print("Enter a number (0-3): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                Object[][] predef=BackgroundModel.predefinedModels;
                for (int i=0;i<predef.length;i++) {
                    String name=(String)predef[i][0];
                    int order=(Integer)predef[i][1];
                    int organism=(Integer)predef[i][2];
                    String organismName=Organism.getCombinedName(organism);
                    if (organismName.startsWith("NCBI")) organismName="UNKNOWN";
                    clientConsole.println("["+(i+1)+"] "+name+" (order="+order+", "+organismName+")");
                } 
                boolean predefOK=false;
                while (!predefOK) {
                    clientConsole.print("Enter a model number (1-"+predef.length+", or 0 to go back): ");
                    String userInput=clientConsole.readLine();
                    if (userInput!=null && !userInput.trim().isEmpty()) {
                        try { // 
                            int selection=Integer.parseInt(userInput);
                            if (selection>0 && selection<=predef.length) {
                               String modelname=(String)predef[selection-1][0];
                               item=getPredefinedBackgroundModel(item.getName(),modelname,BackgroundModel.getFilenameForModel(modelname));
                               predefOK=true; ok=true;
                            } else if (selection==0) {predefOK=true;showUsage=true;}
                        } catch (NumberFormatException e) { //                     
                             // just go back in loop 
                        }
                    }
                } // while!predefOK
            } else if (selectedOption.equals("3")) { // load from file
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            item=getBackgroundModelFromFile(dataname,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;              
            } else if (selectedOption.equals("0")) { // show default
                clientConsole.println(item.output());  
                if (item.getPredefinedModel()!=null) clientConsole.println("Name of background model: "+item.getPredefinedModel());
                clientConsole.println("Background model order: "+item.getOrder());
            } else if (selectedOption.isEmpty()) showUsage=true;
        }              
        return item;        
    }
        
    private Data promptValueTextVariable(TextVariable item, String message, PromptConstraints constraints) {
        if (constraints!=null) return promptConstrainedTextVariable(item, message, constraints); // only allow single line input for constrained variables
        boolean ok=false;
        boolean showUsage=true;
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease provide a value for '"+item.getName()+"': ");
        else clientConsole.println("\n"+message+": ");        
        while(!ok) {          
            if (showUsage) {
                clientConsole.println("\nPlease select one of the following options");
                clientConsole.print(" 1) Use the default value for the Text Variable ");
                if (item.getNumberofStrings()==1) {
                    String line=item.getFirstValue();
                    if (line.length()<40)  clientConsole.print(" ( \""+line+"\" )");
                } 
                clientConsole.println("");
                clientConsole.println(" 2) Load the Text Variable from a file (in Plain-format)");
                clientConsole.println(" 3) Enter new content for the Text Variable manually");     
                clientConsole.println(" 0) See the default value for the Text Variable");                   
                showUsage=false;
            }
            clientConsole.print("Enter a number (0-3): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the Text Variable as is
            } else if (selectedOption.equals("2")) {
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            getTextVariableFromFile(item,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("3")) {
                item.clearAll();
                boolean finished=false;
                clientConsole.println("Please enter one or more lines (press Enter after each). Enter a blank line to finish: ");
                while (!finished) {
                   clientConsole.print("> ");
                   String userInput=clientConsole.readLine();
                   if (userInput==null || userInput.trim().isEmpty()) finished=true;
                   else item.append(userInput.trim());
                }
                ok=true;               
            } else if (selectedOption.equals("0")) {
                clientConsole.println("");
                for (String line:item.getAllStrings()) {
                   clientConsole.println("   "+line); // adding a small margin first 
                }
                clientConsole.println("");
            } else if (selectedOption.isEmpty()) showUsage=true;        
        }
        return item;     
    }
    
    private Data promptConstrainedTextVariable(TextVariable item, String message, PromptConstraints constraints) {    
        boolean ok=false;
        String userInput=null;
        clientConsole.println("");
        while (!ok) {
            if (message==null || message.isEmpty()) clientConsole.print("Please provide a value for '"+item.getName()+"'");
            else clientConsole.print(message);
            if (item.getNumberofStrings()==1) clientConsole.print(" (default value is \""+item.getFirstValue()+"\"): "); 
            else clientConsole.print(": ");
            userInput=clientConsole.readLine();
            if (userInput==null || userInput.trim().isEmpty()) ok=true; // use default in this case. Note that this is not checked against constraints!
            else try {
                if (constraints!=null && constraints.isValueAllowed(userInput,engine)!=null) {
                    reportConstraintsViolation(constraints, userInput);
                } else ok=true;                
            } catch (NumberFormatException e) {}       
        }   
        if (userInput!=null && !userInput.trim().isEmpty()) item.setValue(userInput);
        return item;
    }    
    
    private Data promptValueNumericVariable(NumericVariable item, String message, PromptConstraints constraints) {    
        double value=item.getValue();
        boolean ok=false;
        clientConsole.println("");
        while (!ok) {
            if (message==null || message.isEmpty()) clientConsole.print("Please provide a numeric value for '"+item.getName()+"' (default value is "+value+"): ");
            else clientConsole.print(message+" (default value is "+value+"): ");
            String userInput=clientConsole.readLine();
            if (userInput==null || userInput.trim().isEmpty()) ok=true; // use default in this case (note that this is not checked against constraints)
            else try {
                double newvalue=Double.parseDouble(userInput);
                if (constraints!=null && constraints.isValueAllowed(newvalue,engine)!=null) {
                    reportConstraintsViolation(constraints, newvalue);
                } else {
                    value=newvalue;
                    ok=true;
                }                
            } catch (NumberFormatException e) {}       
        }   
        item.setValue(value);
        return item;
    }
    
    
    private Data promptValueDataPartition(DataPartition partition, String message) {    
        Class memberclass=partition.getMembersClass();
        String membertype=engine.getTypeNameForDataClass(memberclass).toLowerCase();
        int numclusters=partition.getNumberOfClusters();
        int assigned=partition.size();
        clientConsole.println("");
        if (message==null || message.isEmpty()) clientConsole.println("Please provide a value for "+partition.getDynamicType()+" '"+partition.getName()+"' : ");
        else clientConsole.println(message+": ");        
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("Please select one of the following options");
                clientConsole.println(" 1) Use the default value for the partition (currently including "+assigned+" "+membertype+"(s) in "+numclusters+" cluster(s))");        
                clientConsole.println(" 2) Load the partition from a file (in Plain-format)");
                clientConsole.println(" 3) Assign a cluster to each "+membertype+" manually");
                clientConsole.println(" 0) See the default value for the partition");                
                showUsage=false;
            }
            clientConsole.print("Enter a number (0-3): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                partition.clearAll(engine);
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            getDataPartitionFromFile(partition,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("3")) { // assign a cluster to each member
                partition.clearAll(engine);
                clientConsole.println("Enter the name of the cluster for each "+membertype+" (or leave blank)");
                ArrayList<Data> members=engine.getAllDataItemsOfType(memberclass);
                Collections.sort(members, new Comparator<Data>() {
                    @Override
                    public int compare(Data o1, Data o2) {
                       return o1.getName().compareTo(o2.getName());
                    }               
                });                
                for (Data member:members) {
                     boolean memberok=false;
                     while (!memberok) {
                         clientConsole.print(member.getName()+" > ");
                         String clusterName=clientConsole.readLine();   
                         clusterName=clusterName.trim();
                         try {
                             if (!clusterName.isEmpty()) partition.addItem(member, clusterName);
                             memberok=true;
                         } catch (ExecutionError e) {
                             clientConsole.println("ERROR: "+e.getMessage());
                         }
                     }
                }
                ok=true;
            } else if (selectedOption.equals("0")) {
                String output=partition.output().trim();
                output.replace(",", "\n");
                clientConsole.println("The partition currently includes "+assigned+" "+membertype+"(s) in "+numclusters+" cluster(s))");        
                clientConsole.println(output);
                clientConsole.println("");
            } else if (selectedOption.isEmpty()) showUsage=true;
        }
        return partition;        
    }    
    
   private Data promptValueNumericMap(NumericMap map, String message) {
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease provide values for "+map.getDynamicType()+" '"+map.getName()+"': ");
        else clientConsole.println("\n"+message+": ");
        Class membersclass=map.getMembersClass();       
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {            
                clientConsole.println("Please select one of the following options");
                clientConsole.println(" 1) Use default values for map (currently including "+map.getNumberOfSpecificallyAssignedEntries()+" entries with specifically assigned values)");        
                clientConsole.println(" 2) Load the numeric map from a file (in Plain-format)");
                clientConsole.println(" 3) Enter a value for each "+engine.getTypeNameForDataClass(membersclass).toLowerCase()+" manually");
                clientConsole.println(" 0) See the default value for the numeric map");                 
                showUsage=false;
            }
            clientConsole.print("Enter a number (0-3): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                map.clear();
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            getNumericMapFromFile(map,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("3")) { // answer Yes/No for each sequence
                map.clear();
                boolean valueOK=false;
                while (!valueOK) {
                clientConsole.print("Please enter the DEFAULT value first: ");
                    String userInput=clientConsole.readLine();
                    userInput=userInput.trim();
                    try {
                       double value=Double.parseDouble(userInput);
                       map.setDefaultValue(value);
                       valueOK=true;
                    } catch (NumberFormatException e) {
                        clientConsole.println("Error: Not a valid numeric value");
                    }
                }
                clientConsole.println("Now enter values for each "+engine.getTypeNameForDataClass(membersclass).toLowerCase()+". Leave line blank to use DEFAULT value.");
                ArrayList<Data> members=engine.getAllDataItemsOfType(membersclass);
                for (Data member:members) {
                  valueOK=false;
                  while (!valueOK) {
                     clientConsole.print("Enter value for "+member.getName()+": ");
                     String userInput=clientConsole.readLine();
                     userInput=userInput.trim();
                     if (!userInput.isEmpty()) {
                         try {
                           double value=Double.parseDouble(userInput);
                           map.setValue(member.getName(), value);
                           valueOK=true;
                         } catch (NumberFormatException e) {
                            clientConsole.println("Error: Not a valid numeric value");
                         }
                     } else valueOK=true;
                  } // while !valueOK
                } // for each member
                ok=true;
            } else if (selectedOption.equals("0")) {
                String output=map.output().trim();
                clientConsole.println(output);
                clientConsole.println("");
            } 
        }
        return map;
    }
   
   private Data promptValueTextMap(TextMap map, String message) {
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease provide values for "+map.getDynamicType()+" '"+map.getName()+"': ");
        else clientConsole.println("\n"+message+": ");
        Class membersclass=map.getMembersClass();       
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("Please select one of the following options");
                clientConsole.println(" 1) Use default values for map (currently including "+map.getNumberOfSpecificallyAssignedEntries()+" entries with specifically assigned values)");        
                clientConsole.println(" 2) Load the map from a file (in Plain-format)");
                clientConsole.println(" 3) Enter a value for each "+engine.getTypeNameForDataClass(membersclass).toLowerCase()+" manually");
                clientConsole.println(" 0) See the default value for the map");                 
                showUsage=false; 
            }
            clientConsole.print("Enter a number (0-3): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                map.clear();
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            getTextMapFromFile(map,file);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("3")) { // answer Yes/No for each sequence
                map.clear();
                clientConsole.print("Please enter the DEFAULT value first: ");
                String userInput=clientConsole.readLine();
                userInput=userInput.trim();
                map.setDefaultValue(userInput);               
                clientConsole.println("Now enter values for each "+engine.getTypeNameForDataClass(membersclass).toLowerCase()+". Leave line blank to use DEFAULT value.");
                ArrayList<Data> members=engine.getAllDataItemsOfType(membersclass);
                for (Data member:members) {
                   clientConsole.print("Enter value for "+member.getName()+": ");
                   userInput=clientConsole.readLine();
                   userInput=userInput.trim();
                   if (!userInput.isEmpty()) map.setValue(member.getName(), userInput);
                } // for each member
                ok=true;
            } else if (selectedOption.equals("0")) {
                String output=map.output().trim();
                clientConsole.println(output);
                clientConsole.println("");
            } 
        }

        return map;
    }   
   
   
    
    private Data promptValueFeatureDataset(FeatureDataset dataset, String message) {
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease provide value for "+dataset.getDynamicType()+" '"+dataset.getName()+"': ");
        else clientConsole.println("\n"+message+": ");
        DataFormat format=engine.getDefaultDataFormat(dataset);     
        boolean ok=false;
        boolean showUsage=true;
        int highnumber=3;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("Please select one of the following options");
                clientConsole.println(" 1) Use default value for dataset");        
                clientConsole.println(" 2) Select predefined track from a list");
                clientConsole.println(" 3) Load dataset from file ("+format.getName()+" format)"); 
                if (dataset instanceof DNASequenceDataset) {
                    clientConsole.println(" 4) Enter single nucleotide letter to use for sequences");
                    highnumber=4;
                    if (engine.hasDataItemsOfType(BackgroundModel.class)) {
                       clientConsole.println(" 5) Create artificial DNA sequences from a selected background model"); 
                       highnumber=5;
                    }
                }
                showUsage=false;
            }
            clientConsole.print("Enter a number (1-"+highnumber+"): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the dataset as is
            } else if (selectedOption.equals("2")) {
                ArrayList<String> trackNames=new ArrayList<String>();
                DataTrack[] availableTracks=engine.getDataLoader().getAvailableDatatracks(dataset.getClass());
                //System.err.println("Got "+availableTracks.length+" tracks");
                for (DataTrack track:availableTracks) {
                    String[] presentBuilds=engine.getDefaultSequenceCollection().getGenomeBuilds(engine);
                    if (track.isSupported(presentBuilds)) trackNames.add(track.getName());
                }
                Collections.sort(trackNames);
                for (int i=0;i<trackNames.size();i++) {
                    clientConsole.println("["+(i+1)+"] "+trackNames.get(i));
                }       
                boolean predefOK=false;
                while (!predefOK) {
                    clientConsole.print("Enter the name of a track or a number (1-"+trackNames.size()+", or 0 to go back): ");
                    String userInput=clientConsole.readLine();
                    if (userInput!=null && !userInput.trim().isEmpty()) {
                        String trackName=null;
                        try { // 
                            int selection=Integer.parseInt(userInput);
                            if (selection>0 && selection<=trackNames.size()) {
                                trackName=trackNames.get(selection-1);                               
                            } else if (selection==0) {predefOK=true;showUsage=true;}
                        } catch (NumberFormatException e) { //                     
                             trackName=userInput.trim();
                             if (!trackNames.contains(trackName)) trackName=null;
                        }
                        if (trackName!=null) {// available track selected
                            OperationTask dummyTask=new OperationTask("load track");
                            try {
                                String datasetname=dataset.getName(); // the next line will change the name to trackName
                                dataset=engine.getDataLoader().loadDataTrack(trackName,dummyTask);
                                dataset.setName(datasetname); // restore old name
                                predefOK=true; ok=true;
                            } catch (Exception e) {
                                errorMessage("ERROR:"+e.getMessage());
                            }
                        }
                    }
                } // while!predefOK
            } else if (selectedOption.equals("3")) { // load track from file (default format)
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            OperationTask dummyTask=new OperationTask("load track");
                            String oldname=dataset.getName();
                            dataset=(FeatureDataset)engine.getDataLoader().loadData(file, dataset.getDynamicType(), dataset, format, null, dummyTask);
                            dataset.setName(oldname);
                            fileOK=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
                if (fileOK) ok=true;
            } else if (selectedOption.equals("4") && dataset instanceof DNASequenceDataset) { //
                boolean bgOK=false;
                while (!bgOK) {
                    clientConsole.print("Enter a single nucleotide letter: ");
                    String userInput=clientConsole.readLine();
                    userInput=userInput.trim();
                    if (userInput.length()==1 && Character.isLetter(userInput.charAt(0))) {
                        Character ch=userInput.charAt(0);
                        dataset=createNewDNAdataset(ch,dataset.getName());
                        bgOK=true;
                    }
                }
                ok=true;
            } else if (selectedOption.equals("5") && dataset instanceof DNASequenceDataset && engine.hasDataItemsOfType(BackgroundModel.class)) { // 
                ArrayList<String> bgmodels=engine.getNamesForAllDataItemsOfType(BackgroundModel.class);
                Collections.sort(bgmodels);                
                for (int i=0;i<bgmodels.size();i++) {
                    clientConsole.println("["+(i+1)+"] "+bgmodels.get(i));
                }       
                boolean bgOK=false;
                while (!bgOK) {
                    clientConsole.print("Enter the name of a background model or a number (1-"+bgmodels.size()+", or 0 to go back): ");
                    String userInput=clientConsole.readLine();
                    if (userInput!=null && !userInput.trim().isEmpty()) {
                        String modelName=null;
                        try { // 
                            int selection=Integer.parseInt(userInput);
                            if (selection>0 && selection<=bgmodels.size()) {
                                modelName=bgmodels.get(selection-1);                               
                            } else if (selection==0) {bgOK=true;showUsage=true;}
                        } catch (NumberFormatException e) { //                     
                             modelName=userInput.trim();
                             if (!bgmodels.contains(modelName)) modelName=null;
                        }
                        if (modelName!=null) {// available model selected
                            BackgroundModel model=(BackgroundModel)engine.getDataItem(modelName);
                            if (model!=null) {
                               dataset=createNewDNAdataset(model,dataset.getName());
                               bgOK=true; 
                               ok=true;
                            } else clientConsole.println("Unknown background model: "+modelName);
                        }
                    }
                } // while !bgOK                
            } else showUsage=true;
        }
        return dataset;
    }
    
    private DNASequenceDataset createNewDNAdataset(Object defaultvalue,String targetName) {
        DNASequenceDataset newDataItem = new DNASequenceDataset(targetName);
        SequenceCollection allSequences = engine.getDefaultSequenceCollection();
        ArrayList<Sequence> seqlist=allSequences.getAllSequences(engine);
        for (Sequence sequence:seqlist) {
            DNASequenceData seq=null;
            if (defaultvalue instanceof BackgroundModel) seq=new DNASequenceData(sequence.getName(), sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(), (BackgroundModel)defaultvalue);
            else seq=new DNASequenceData(sequence.getName(), sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(),(Character)defaultvalue);
            newDataItem.addSequence(seq);
        }  
        return newDataItem;
    }
    
    private Data promptValueExpressionProfile(ExpressionProfile profile, String message) {
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease provide value for "+profile.getDynamicType()+" '"+profile.getName()+"': ");
        else clientConsole.println("\n"+message+": ");             
        boolean ok=false;
        boolean showUsage=true;
        while(!ok) {
            if (showUsage) {
                clientConsole.println("Please select one of the following options");
                clientConsole.println(" 1) Use default values for expression profile (currently including "+profile.getNumberOfConditions()+" condition(s))");        
                clientConsole.println(" 2) Load the expression from a file in Plain-format");
                clientConsole.println(" 3) Load the expression from a file in TAB-separated format");
                clientConsole.println(" 4) Load the expression from a file in comma-separated format");  
                clientConsole.println(" 0) See the default values for the expression profile");                  
                showUsage=false;
            }            
            clientConsole.print("Enter a number (0-4): ");
            String selectedOption=clientConsole.readLine();
            selectedOption=selectedOption.trim();
            if (selectedOption.equals("1")) {
                ok=true; // Do nothing. Just return the collection as is
            } else if (selectedOption.equals("2")) {
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            OperationTask dummyTask=new OperationTask("load data");
                            String oldName=profile.getName();
                            DataFormat format=engine.getDataFormat("Plain");
                            profile=(ExpressionProfile)engine.getDataLoader().loadData(file, profile.getDynamicType(), profile, format, null, dummyTask);
                            profile.rename(oldName);
                            fileOK=true;ok=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
            } else if (selectedOption.equals("3") || selectedOption.equals("4")) { // answer Yes/No for each sequence
                boolean fileOK=false;
                while (!fileOK) {
                    clientConsole.print("Enter filename: ");
                    String userInput=clientConsole.readLine();
                    if (userInput.trim().isEmpty()) {showUsage=true;break;} // abandon this choice and move up a level
                    File file=engine.getFile(userInput.trim());
                    if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                    else {
                        try {
                            String delimiter=(selectedOption.equals("3"))?"\t":",";
                            getExpressionProfileFromFile(profile, file, delimiter);
                            fileOK=true;ok=true;
                        } catch (Exception ex) {
                            clientConsole.println(ex.getMessage());
                        }
                    } 
                }
            } else if (selectedOption.equals("0")) {
                String output=profile.output().trim();
                clientConsole.println(output);
                clientConsole.println("");
            } 
        }
        return profile;
    }
    
    private Data promptValuePriorsGenerator(PriorsGenerator generator, String message) {
        if (message==null || message.isEmpty()) clientConsole.println("\nPlease provide value for "+generator.getDynamicType()+" '"+generator.getName()+"': ");
        else clientConsole.println("\n"+message+": ");    
        DataFormat format=engine.getDefaultDataFormat(generator);
        boolean ok=false;
        while(!ok) {
            clientConsole.print("Enter name of file containing the Priors Generator in "+format.getName()+" format (or leave blank to use default): ");
            String userInput=clientConsole.readLine();
            if (userInput==null || userInput.trim().isEmpty()) {
               ok=true; // return default value 
            } else {
                File file=engine.getFile(userInput.trim());
                if (!file.exists()) clientConsole.println("File not found! Please specify another filename");
                else {
                    try {
                        getPriorsGeneratorFromFile(generator, file);
                        ok=true;
                    } catch (Exception ex) {
                        clientConsole.println(ex.getMessage());
                    }
                } 
            }
        }
        return generator;
    }    

    private void reportConstraintsViolation(PromptConstraints constraints, Object value) {
        String veto=constraints.isValueAllowed(value,engine);
        String range=constraints.getNumericRangeAsString();
        if (veto!=null) clientConsole.println("Value constraints violation: "+veto);
        if (range!=null) clientConsole.println("Value must be in the range "+range);
        else {
            Object[] allowed=constraints.getAllowedValues();
            if (allowed!=null) {
                clientConsole.println("The new value must be one of the following: ");
                printList(allowed, " - ");
            } else clientConsole.println("The specified value is not allowed");
        }   
    }
    
    /** Prints out the list items to the console with one item on each line (each introduced by the prefix) */
    private void printList(Object[] list, String prefix) {
        if (list==null || list.length==0) return;
        for (Object object:list) {
            clientConsole.println(((prefix!=null)?prefix:"")+object);
        }
    }    
    
    
    private PriorsGenerator getPriorsGeneratorFromFile(PriorsGenerator generator,File file) throws Exception {
        OperationTask dummyTask=new OperationTask("load data");
        String oldName=generator.getName();
        DataFormat format=engine.getDefaultDataFormat(generator);        
        generator=(PriorsGenerator)engine.getDataLoader().loadData(file, generator.getDynamicType(), generator, format, null, dummyTask);
        generator.setName(oldName);     
        return generator;
    }
    
    /** Returns a predefined Background model based on specifications */
    private BackgroundModel getPredefinedBackgroundModel(String targetName, String modelName, String filename) throws ExecutionError {
        if (filename==null) throw new ExecutionError("Unknown Background Model: "+modelName);
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            inputStream=new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/motiflab/engine/resources/"+filename)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading predefined Background model: ["+e.getClass().getSimpleName()+"] "+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }           
        DataFormat_INCLUSive_Background format=new DataFormat_INCLUSive_Background();   
        BackgroundModel data=new BackgroundModel(targetName);
        try {data=(BackgroundModel)format.parseInput(input, data,null, null);}
        catch (Exception e) {throw new ExecutionError(e.getMessage());} 
        data.setPredefinedModel(modelName);
        return data;        
    }    

    /** Returns a Background loaded from file */
    private BackgroundModel getBackgroundModelFromFile(String targetName, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading Background model: ["+e.getClass().getSimpleName()+"] "+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        if (input.isEmpty()) throw new ExecutionError("Background file is empty");
        String formatName=BackgroundModel.determineDataFormatFromHeader(input.get(0));
        if (formatName==null) throw new ExecutionError("Unable to determine format of background model file");
        DataFormat format=engine.getDataFormat(formatName);  
        BackgroundModel data=new BackgroundModel(targetName);
        try {data=(BackgroundModel)format.parseInput(input, data,null, null);}
        catch (Exception e) {throw new ExecutionError(e.getMessage());} 
        return data;   
    }      

    /** Returns a predefined Motif Collection based on specifications */
    private MotifCollection getPredefinedMotifCollection(String targetName, String collectionName) throws ExecutionError {
        String filename=engine.getFilenameForMotifCollection(collectionName);
        if (filename==null) throw new ExecutionError("Unknown motif collection: "+collectionName);
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading predefined motif collection: ["+e.getClass().getSimpleName()+"] "+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }           
        String dataformatname="MotifLabMotif";
        DataFormat format=engine.getDataFormat(dataformatname);
        if (format==null) throw new ExecutionError("Unknown data format '"+dataformatname+"'");
        MotifCollection data=new MotifCollection(targetName);
        try {data=(MotifCollection)format.parseInput(input, data,null, null);}
        catch (Exception e) {throw new ExecutionError(e.getMessage());} 
        data.setPredefinedCollectionName(collectionName);
        return data;        
    }    

    /** Returns a MotifCollection loaded from file */
    private MotifCollection getMotifCollectionFromFile(String targetName, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading motif collection: ["+e.getClass().getSimpleName()+"] "+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        if (input.isEmpty()) throw new ExecutionError("Motif collection file is empty");
        String formatName=MotifCollection.determineDataFormatFromHeader(input.get(0));
        if (formatName==null) throw new ExecutionError("Unable to determine format of motif collection file");
        DataFormat format=engine.getDataFormat(formatName);  
        MotifCollection data=new MotifCollection(targetName);
        try {data=(MotifCollection)format.parseInput(input, data, null, null);}
        catch (Exception e) {throw new ExecutionError(e.getMessage());} 
        return data;   
    }    
    
    /** Returns a ModuleCRM Collection loaded from file */
    private ModuleCollection getModuleCollectionFromFile(String targetName, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading module collection: ["+e.getClass().getSimpleName()+"] "+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        if (input.isEmpty()) throw new ExecutionError("Module collection file is empty");
//            String formatName=MotifCollection.determineDataFormatFromHeader(input.get(0));
//            if (formatName==null) throw new ExecutionError("Unable to determine format of motif collection file");
        DataFormat format=engine.getDataFormat("MotifLabModule");  
        ModuleCollection data=new ModuleCollection(targetName);
        try {data=(ModuleCollection)format.parseInput(input, data, null, null);}
        catch (Exception e) {throw new ExecutionError(e.getMessage());} 
        return data;   
    }     
    
    /** Reads a data partition from file (in Plain Format) */    
    private DataPartition getDataPartitionFromFile(DataPartition partition, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading partition from file: ["+e.getClass().getSimpleName()+"] "+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        //if (input.isEmpty()) throw new ExecutionError("Partition file is empty");
        try {
           partition.inputFromPlain(input, engine); 
        } catch (ParseError e) {
            throw new ExecutionError(e.getMessage());
        }
        return partition;
    }   
    
    /** Reads a data collection from file (in Plain Format) */
    private DataCollection getDataCollectionFromFile(DataCollection collection, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading collection from file: ["+e.getClass().getSimpleName()+"] "+e.getMessage());
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        //if (input.isEmpty()) throw new ExecutionError("Partition file is empty");
        try {
           collection.inputFromPlain(input, engine); 
        } catch (ParseError e) {
            throw new ExecutionError(e.getMessage());
        }
        return collection;
    }   
    
    /** Reads a numeric map from file (in Plain Format) */
    private NumericMap getNumericMapFromFile(NumericMap map, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading map from file: ["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {errorMessage("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        try {
           map.inputFromPlain(input, engine); 
        } catch (ParseError e) {
            throw new ExecutionError(e.getMessage());
        }
        return map;
    }       
    

    /** Reads a text map from file (in Plain Format) */
    private TextMap getTextMapFromFile(TextMap map, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading map from file: ["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        try {
           map.inputFromPlain(input, engine); 
        } catch (ParseError e) {
            throw new ExecutionError(e.getMessage());
        }
        return map;
    }      
    
    
    
    /** Reads a numeric map from file (in Plain Format) */
    private TextVariable getTextVariableFromFile(TextVariable text, File file) throws ExecutionError {
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading text from file: ["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }  
        //if (input.isEmpty()) throw new ExecutionError("Partition file is empty");
        try {
           text.inputFromPlain(input, engine); 
        } catch (ParseError e) {
            throw new ExecutionError(e.getMessage());
        }
        return text;
    }      
    
    private ExpressionProfile getExpressionProfileFromFile(ExpressionProfile profile, File file, String delimiter) throws ExecutionError {
        try {
            OperationTask dummyTask=new OperationTask("load data");
            DataFormat format=engine.getDataFormat("ExpressionProfile");
            ParameterSettings settings=new ParameterSettings();
            settings.setParameter("Sequence name delimiter", delimiter);
            settings.setParameter("Condition delimiter", delimiter);
            String oldname=profile.getName();
            profile=(ExpressionProfile)engine.getDataLoader().loadData(file, profile.getDynamicType(), profile, format, settings, dummyTask);
            profile.rename(oldname);
            return profile;
        } catch (Exception ex) {
            throw new ExecutionError(ex.getMessage());
        }     
    }
    
    /** Reads a FASTA file from a file with the given name and returns a set of Sequence objects
     *  based on the information in the file (note that the DNA information itself is discarded;
     *  only information regarding the names and locations/origins of the sequences is used).
     *  If the startIndex and endIndex is greater than or equal to zero, they can be used to
     *  define a range of sequences to return (with the first sequence in the file having index 0)
     *  If the range extends beyond the number of sequences in the file, a smaller set (or even
     *  and empty set) of sequences will be returned.
     *  @param filename
     *  @param startIndex,
     *  @param endIndex
     */
    public static Sequence[] parseSequencesFromFastaFile(String filename, int startIndex, int endIndex, String build, MotifLabEngine motiflabengine) throws ExecutionError { 
        DataFormat dataformat=motiflabengine.getDataFormat("FASTA");
        DataFormat_FASTA fasta=(DataFormat_FASTA)dataformat;
        DNASequenceDataset dataset=null;
        try {
            ParameterSettings parameters=null;
            if (startIndex>=0 && endIndex>=startIndex) {
               parameters=new ParameterSettings();
               parameters.setParameter(DataFormat_FASTA.START_INDEX, new Integer(startIndex));
               parameters.setParameter(DataFormat_FASTA.END_INDEX, new Integer(endIndex));
            }
            dataset=loadSequences(fasta, filename, parameters, motiflabengine);
        } catch (Exception e) {           
            throw new ExecutionError("An error occurred while attempting to read FASTA file\n"+e.getClass().getSimpleName()+" : " +e.getMessage(),e);     
        }
        if (dataset==null) {
            throw new ExecutionError("No dataset returned");        
        }  
        int size=dataset.getSize();
        Sequence[] sequences=new Sequence[size];
        for (int i=0;i<size;i++) {
            DNASequenceData seq=(DNASequenceData)dataset.getSequenceByIndex(i);
            sequences[i]=new Sequence(seq.getSequenceName(), 0, "", seq.getChromosome(),seq.getRegionStart(), seq.getRegionEnd(), seq.getGeneName(), seq.getTSS(), seq.getTES(), seq.getStrandOrientation());      
            if (seq.getTemporaryOrganism()!=null) sequences[i].setOrganism(seq.getTemporaryOrganism());
            if (seq.getTemporaryBuild()!=null) sequences[i].setGenomeBuild(seq.getTemporaryBuild());
            if (sequences[i].getGenomeBuild()==null && build!=null) {
                int organism=Organism.getOrganismForGenomeBuild(build);
                sequences[i].setGenomeBuild(build);
                sequences[i].setOrganism(organism);
            }
        }
        return sequences;    
    }    

    /** Returns a DNASequence Dataset read from a FASTA file */
   public static DNASequenceDataset loadSequences(DataFormat_FASTA fasta, String filename, ParameterSettings settings, MotifLabEngine motiflabengine) throws Exception {
        ArrayList<String>input=new ArrayList<String>();
        Object source=motiflabengine.getDataSourceForString(filename);
        if (source instanceof File && !((File)source).exists()) throw new ExecutionError("File '"+filename+"' does not exist");
        if (source instanceof File && !((File)source).canRead()) throw new ExecutionError("Unable to read file '"+filename+"'");
        BufferedReader inputStream=null;
        try {
            InputStream stream=MotifLabEngine.getInputStreamForDataSource(source);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            int counter=0;
            while((line=inputStream.readLine())!=null) {
                if (counter%50==0 && Thread.interrupted()) { throw new InterruptedException();}
                counter++;
                input.add(line);
            }
        } 
        catch (InterruptedException ie) {throw ie;}
        catch (Exception ioe) { 
            throw new ExecutionError(ioe.getMessage(), ioe);
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }                
        try {
            DNASequenceDataset result=fasta.parseDNASequenceDataset(input, "DNA", settings);
            return result;
        }           
        catch (InterruptedException ie) {throw ie;}
        catch (Exception e) {throw new ExecutionError(e.getClass().getSimpleName()+":"+e.getMessage(), e); }               
    }    
    
    
    @Override
    public boolean shouldRetry(ExecutableTask task, Exception e) {       
        //e.printStackTrace(System.err);
        if (retry==DO_NOT_RETRY) {
           errorMessage("ERROR encountered: "+e.getClass().getSimpleName()+" : "+e.getMessage(),0); 
           return false;
        } else if (retry==PROMPT_RETRY) {
            clientConsole.println("ERROR encountered: "+e.getClass().getSimpleName()+" : "+e.getMessage());
            boolean ok=false;
            while (!ok) {
                clientConsole.print("Retry? [y|n]:");
                String userInput=clientConsole.readLine();
                if (userInput==null || userInput.trim().isEmpty()) continue;
                //System.err.println("Value='"+userInput+"'");
                     if (userInput.equalsIgnoreCase("YES") || userInput.equalsIgnoreCase("Y")) return true;
                else if (userInput.equalsIgnoreCase("NO") || userInput.equalsIgnoreCase("N")) return false;            
                else if (userInput.equalsIgnoreCase("DEBUG") || userInput.equalsIgnoreCase("STACK")) {
                    clientConsole.println("=============================================");
                    clientConsole.println(e.toString());
                    Throwable cause=e.getCause();
                    if (cause!=null) {
                        clientConsole.println("Caused by: "+cause.toString()+"\n");
                    }
                    e.printStackTrace(System.out);
                    clientConsole.println("=============================================");
                }            
            }
            return false;
        } else {
           clientConsole.println("ERROR encountered: "+e.getClass().getSimpleName()+" : "+e.getMessage());
           clientConsole.println("Automatic retry in "+retry+" seconds"); 
           try {Thread.sleep(retry*1000);} catch (InterruptedException ie) {return true;}
           return true;
        }
    }

    @Override
    public boolean shouldRollback() {
        return false; // not really necessary to roll back
    }

    @Override
    public void shutdown() {
        // --- For debugging ---
//          ArrayList<Data> list=engine.getAllDataItemsOfType(Data.class);
//          for (Data data:list) {
//              System.err.println("------------ "+data.getName()+" --------------");
//              System.err.println(data.output());
//              System.err.println("============================");              
//          }
          if (logger!=null) {
              try {
                  logger.close();
                  logger=null;
              } catch (IOException ioe) {System.err.println("SYSTEM ERROR (non-fatal): An error occurred when closing BufferedReader: "+ioe.getMessage());}
          }
          if (engine!=null) engine.shutdown();
    }

    
    @Override
    public void errorMessage(String msg, int errortype) {
        if (logger!=null) {
            try {logger.append(msg+"\n");} catch (IOException e) {System.err.println("Unable to write error message to file: "+e.getMessage());}
        }
        else System.err.println(msg);
    }
    
    public void errorMessage(String msg) {
        errorMessage(msg,0);
    }

    @Override
    public void logMessage(String msg, int level) {
        if (loggerMode==LOGGER_MODE_SILENT) return;
        if (logger!=null) {
            try {logger.append(msg+"\n");} catch (IOException e) {System.err.println("Unable to write log message to file: "+e.getMessage());}
        }
        else System.err.println(msg);
    }
    
    @Override
    public void logMessage(String msg) {
        logMessage(msg, 20);
    }

    @Override
    public void statusMessage(String msg) {
        if (loggerMode==LOGGER_MODE_SILENT) return;
        if (!msg.startsWith("Exec") && loggerMode!=LOGGER_MODE_VERBOSE) return; // in non-verbose mode, only output execution messages (not other messages)
        if (logger!=null) {
            try {logger.append(msg+"\n");} catch (IOException e) {System.err.println("Unable to write log message to file: "+e.getMessage());}
        }
        else System.err.println(msg);       
    }
    
    @Override
    public void progressReportMessage(int progress) { 
        // This is mostly used to update progress bars in graphical clients 
    }     



      @Override
      public void propertyChange(java.beans.PropertyChangeEvent evt) {
          if (loggerMode==LOGGER_MODE_VERBOSE && evt.getPropertyName().equals(ExecutableTask.STATUS_MESSAGE)) {
             String text = (String)(evt.getNewValue());
             if (text!=null && !text.isEmpty()) statusMessage(text);        
          }
      } 
    

    @Override 
    public void saveSession(String filename) throws Exception { 
       File file=engine.getFile(outputdirname,filename);
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
                    if (element instanceof Sequence || element instanceof Motif || element instanceof ModuleCRM) {
                        engine.storeDataItem(element);
                    }
                }
                for (String outputDataName:tabNames){ // restore output data objects (same as tabnames)
                    Data element=getDataByNameFromList(datalist,outputDataName);
                    if (element==null) continue;              
                    engine.storeDataItem(element); // this will show the tab
                }
                for (Data element:datalist) {
                    if (!(element instanceof Sequence || element instanceof OutputData || element instanceof Motif || element instanceof ModuleCRM)) engine.storeDataItem(element);
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
        
    /** This method does the main body of work */
    public void run() {
        engine.addMessageListener(this);
        engine.setClient(this);
        if (pluginConfigurations!=null) {
            try {engine.setPluginConfigurations(pluginConfigurations);}
            catch (ExecutionError e) {errorMessage(e.getMessage(),0);protocolFilename=null;} // clear protocol on error to abort  
        }   
        if (protocolFilename==null) { // nothing else to do?
            logMessage("Finishing up...");        
            shutdown();
            logMessage("Done!");      
            return;
        }
        setMacros(macros);
        if (sequencesFilename!=null && sequencesFilename.contains(",")) {
            try { // run in "whole genome analysis" mode
               performWholeGenomeAnalysis(sequencesFilename);
            } catch (ParseError p) {
                errorMessage("Parse Error: "+p.getMessage(),0);
            } catch (ExecutionError e) {
                errorMessage("Execution Error: "+e.getMessage(),0);
            } catch (Exception e) {
                errorMessage("SYSTEM ERROR ("+e.getClass().getSimpleName()+") : "+e.getMessage(),0);
            }             
        } else if (sequencesFilename!=null && splitGroupSize>0) { // split the sequences into groups
            int groupIndex=0;
            int startIndex=0;
            logMessage("Reading protocol file");            
            StandardProtocol protocol=readProtocolFromFile(protocolFilename);
            currentProtocol=protocol;            
            boolean finished=false;
            while (!finished) {
                groupIndex++;
                int endIndex=startIndex+splitGroupSize-1; // 
                if (inputSessionFilename!=null && !inputSessionFilename.isEmpty()) { // reload input session for every group
                    try {
                        restoreSession(inputSessionFilename);
                    } catch (Exception e) {errorMessage("Session restore error: "+e.getMessage(),0);}
                }   
                int size=0;
                try {
                    size=getSequences(sequencesFilename, startIndex, endIndex,sequenceFormat,defaultGenomeBuild,assignNewSequenceNames,silentmode,engine);
                    if (size==0) break; // no more sequences
                    logMessage("-----------   Analyzing sequence group "+groupIndex+" (Sequences "+(startIndex+1)+" to "+(startIndex+size)+")   -----------");                      
                    engine.executeProtocol(protocol,false); // parses and executes the protocol
                } catch (ParseError p) {
                    errorMessage("Parse Error: "+p.getMessage(),0);
                } catch (ExecutionError e) {
                    errorMessage("Execution Error: "+e.getMessage(),0);
                } catch (Exception e) {
                    errorMessage("SYSTEM ERROR ("+e.getClass().getSimpleName()+") : "+e.getMessage(),0);
                } 
                // now save all "OutputData" objects to files
                if (saveoutput) saveOutputData("_"+groupIndex, outputdirname, renameOutput, engine);
                engine.clearAllData();
                if (size<splitGroupSize) finished=true; // this is the last group
                startIndex+=splitGroupSize;
            }          
        } else { // regular execution (one batch)
            if (inputSessionFilename!=null && !inputSessionFilename.isEmpty()) {
                try {
                    logMessage("Restoring session '"+inputSessionFilename+"'");
                    restoreSession(inputSessionFilename);
                } catch (Exception e) {errorMessage("Session restore error: "+e.getMessage(),0);}
            }                    
            logMessage("Reading protocol file");
            StandardProtocol protocol=readProtocolFromFile(protocolFilename);
            currentProtocol=protocol;
            try {
                if (sequencesFilename!=null) getSequences(sequencesFilename,sequenceFormat,defaultGenomeBuild,assignNewSequenceNames,silentmode,engine);                 
                engine.executeProtocol(protocol,false); // parses and executes the protocol
            } catch (ParseError p) {
                errorMessage("Parse Error: "+p.getMessage(),0);
            } catch (ExecutionError e) {
                errorMessage("Execution Error: "+e.getMessage(),0);
            } catch (Exception e) {
                errorMessage("SYSTEM ERROR ("+e.getClass().getSimpleName()+") : "+e.getMessage(),0);
            } 
            // now save all "OutputData" objects to files before ending the program
            if (saveoutput) saveOutputData(null, outputdirname, renameOutput, engine);

            // optionally save session
            if (outputSessionFilename!=null) {
                try {
                    saveSession(outputSessionFilename);
                } catch (Exception e) {
                    errorMessage("Session save error: "+e.getMessage(),0);
                }
            }
        } // end of execution step
        
        logMessage("Finishing up...");        
        shutdown();
        logMessage("Done!");        
    }
        
    /**
     * This method performs analysis in "whole genome analysis" (WGA) mode, which is different
     * from the regular analysis mode that analyzes a specified list of sequences.
     * In WGA mode, the user specifies a potentially long genomic region (chr:start-end) to analyze,
     * and MotifLab will automatically split this region up into smaller segments (that could overlap)
     * if necessary and execute the protocol on (smaller collections of) these segments in turn
     * @param specifications  A string specifying which genomic region to analyse along with some other settings
     * @throws ExecutionError
     * @throws ParseError
     * @throws SystemError 
     */
    private void performWholeGenomeAnalysis(String specifications) throws ExecutionError, ParseError, SystemError, InterruptedException {
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
        logMessage("Reading protocol file");
        currentProtocol=readProtocolFromFile(protocolFilename);
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
        //clientConsole.println("Number of segments: "+numberOfSegments+", collections="+numberOfCollections);
        for (int i=1;i<=numberOfCollections;i++) {
            int segmentsInThisCollection=(segmentsToGo>=sequenceCollectionSize)?sequenceCollectionSize:segmentsToGo;            
            logMessage("-----------   Analyzing sequence group "+i+" of "+numberOfCollections+"   ("+segmentsInThisCollection+" sequences @ "+seqLength+" bp)   --------");
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
            
            // register sequences with the engine
            AddSequencesTask addSequencesTask=new AddSequencesTask(engine, sequences);
            addSequencesTask.setMotifLabClient(this);  
            addSequencesTask.addPropertyChangeListener(this);
            try {
                addSequencesTask.run();
            } catch (Exception e) {throw new ExecutionError(e.toString());}   
            
            // run the protocol and save the results to file
            engine.executeProtocol(currentProtocol,true); // this can throw exceptions which will be propagated
            saveOutputData("_"+i, outputdirname, renameOutput, engine);
            
            // prepare for next run
            engine.clearAllData();
            segmentsToGo-=segmentsInThisCollection;
        } 
         
    }
    
    /**
     * Reads a file specifying sequences (in Location or FASTA format)
     * and registers these Sequences with the engine.
     * If the sequences were from a FASTA file, a DNA Sequence Data track 
     * will also be created from this file
     * @param sequencesFilename 
     * @return The number of sequences obtained
     */
    
    public static int getSequences(String sequenceFilename, String sequenceFormat, String build, boolean assignNewNames, boolean silent, MotifLabEngine motiflabengine) throws Exception {
        return getSequences(sequenceFilename,-1,-1,sequenceFormat,build,assignNewNames,silent,motiflabengine);
    }
    
    /**
     * Reads a file specifying sequences (in Location or FASTA format)
     * and registers these Sequences with the engine.
     * If the sequences were from a FASTA file, a DNA Sequence Data track 
     * will also be created from this file
     * If the startIndex and endIndex parameters are equal to or greater than zero
     * they can be used to specify the range of sequences to be obtained from the file.
     * (starting at index 0). E.g. the range [0,9] will return the first 10 sequences
     * in the file and the range [17,28] will return sequences 18 through 29
     * (Note that if the indexes are greater than the number of sequences in the file,
     * a smaller set (or even an empty set) can be read.)
     * @param sequencesFilename 
     * @param startIndex
     * @param endIndex
     * @return The number of sequences obtained
     */    
    public static int getSequences(String sequencesFilename, int startIndex, int endIndex, String sequenceFormat, String build, boolean assignNewNames, boolean silent, MotifLabEngine motiflabengine) throws Exception {
        motiflabengine.logMessage("Reading sequences file");
        Sequence[] sequences=null;
        boolean sequenceFromFasta=isFASTAfile(sequencesFilename, motiflabengine);
        if (sequenceFromFasta) {
            sequences=parseSequencesFromFastaFile(sequencesFilename, startIndex, endIndex, build, motiflabengine);
        } else {
            sequences=parseSequencesFile(sequencesFilename, startIndex, endIndex, sequenceFormat, build, assignNewNames, silent, motiflabengine);
        }
        if (sequences.length==0) return 0; // early return if the given range was out of bounds
        
        AddSequencesTask addSequencesTask=new AddSequencesTask(motiflabengine, sequences);
        addSequencesTask.setMotifLabClient(motiflabengine.getClient());  
        addSequencesTask.addPropertyChangeListener((PropertyChangeListener)motiflabengine.getClient()); // NB! this is not guaranteed to work unless the client actually implements this interface!
        addSequencesTask.run(); 
        if (sequenceFromFasta) { // If the sequence-file is a FASTA-file, obtain the DNA sequence data also
            DataFormat fasta=motiflabengine.getDataFormat("FASTA");
            Operation operation=motiflabengine.getOperation("new");      
            OperationTask newDNATrackTask=new OperationTask("new "+DNASequenceDataset.getType());
            newDNATrackTask.setParameter(OperationTask.OPERATION, operation);
            newDNATrackTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
            newDNATrackTask.setParameter(OperationTask.TARGET_NAME, "DNA");
            newDNATrackTask.setParameter(OperationTask.SOURCE_NAME, "DNA");
            newDNATrackTask.setParameter(Operation_new.DATA_TYPE, DNASequenceDataset.getType());    
            newDNATrackTask.setParameter(Operation_new.PARAMETERS,Operation_new.FILE_PREFIX); // the FILE_PREFIX part of the parameter is necessary for proper recognition by Operation_new
            newDNATrackTask.setParameter(Operation_new.FILENAME,sequencesFilename);
            newDNATrackTask.setParameter(Operation_new.DATA_FORMAT,fasta.getName());
            ParameterSettings parameters=null;
            if (startIndex>=0 && endIndex>=startIndex) {
               parameters=new ParameterSettings();
               parameters.setParameter(DataFormat_FASTA.START_INDEX, new Integer(startIndex));
               parameters.setParameter(DataFormat_FASTA.END_INDEX, new Integer(endIndex));
            }
            newDNATrackTask.setParameter(Operation_new.DATA_FORMAT_SETTINGS,parameters);  
            motiflabengine.executeTask(newDNATrackTask);                  
        }
        
        return sequences.length;
    }
    
    
    /** returns true if the file with the given name is believed to be FASTA formatted
     */
    public static boolean isFASTAfile(String filename, MotifLabEngine engine) throws ExecutionError {
        BufferedReader inputStream=null;
        try {
            File file=engine.getFile(filename);
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            if ((line=inputStream.readLine())!=null) { // first line
                if (line.startsWith(">")) return true;
                else return false;
            } return false;
        } catch (IOException e) { 
             throw new ExecutionError("SYSTEM ERROR: While parsing sequences file '"+filename+"': "+e.getMessage(), e);
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {}
        } 
    }
    
    /**
     * Saves all currently registered outputData objects to files.
     * Unless the user has explicitly specified filenames to use for an object,
     * they will be saved to a file named after the object itself plus a suffix 
     * which depends on the data format. If the 'segmentString' is specified (not null)
     * the string will be appended to the filename just before the suffix
     * @param segmentString
     * @return 
     */
    public static boolean saveOutputData(String segmentString, String outputdirname, HashMap<String,String> renameOutput, MotifLabEngine engine) {
        if (segmentString==null) segmentString="";
        ArrayList<Data> outputlist=engine.getAllDataItemsOfType(OutputData.class);
        if (!outputlist.isEmpty()) engine.logMessage("Saving output:");
        for (Data data:outputlist) {
            if (data.isTemporary()) continue; // do not save temporary OutputData objects
            OutputData output=(OutputData)data;
            String outputDataName=output.getName();
            String suffix=output.getPreferredFileSuffix();
            String filename=output.getName()+segmentString+"."+suffix;
            if (renameOutput!=null && renameOutput.containsKey(outputDataName)) {
                filename=renameOutput.get(outputDataName)+segmentString;
                if (!filename.contains(".")) filename=filename+"."+suffix;
            }
            try {
                if (outputdirname!=null && !outputdirname.isEmpty()) filename=engine.getFile(outputdirname, filename).getCanonicalPath();
                engine.logMessage(" - Saving '"+outputDataName+"' to "+filename);
                output.saveToFile(filename,engine);
            } catch (IOException io) {
                engine.errorMessage("ERROR: Unable to save file '"+filename+"' ("+io.getMessage()+")", 0);
                return false;
            }
        } // end output all 
        return true;
    }
    
    private StandardProtocol readProtocolFromFile(String filename) {
        BufferedReader inputStream=null;
        String fullPathName=null;
        String protocolName=null;
        StringBuilder text=new StringBuilder();
        try {
            File file=engine.getFile(filename);
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {
                text.append(line);
                text.append("\n");
            }
            fullPathName=file.getCanonicalPath();
            protocolName=file.getName();
        } catch (IOException e) { 
             errorMessage("SYSTEM ERROR: While parsing protocol file '"+filename+"': "+e.getMessage(), 0);
             System.exit(1);
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {errorMessage("SYSTEM ERROR (non-fatal): An error occurred when closing BufferedReader: "+ioe.getMessage());}
        }
        StandardProtocol protocol=new StandardProtocol(engine,text.toString()); 
        protocol.setName(protocolName);
        protocol.setFileName(fullPathName);
        return protocol;
    }
    
    private void initializePlugins() {
        // initialize plugins from client
        for (Plugin plugin:getEngine().getPlugins()) {
            try {
                plugin.initializePluginFromClient(this);
            } catch (Exception e) {
                logMessage("Unable to initialize plugin \""+plugin.getPluginName()+"\" from client: "+e.getMessage());
            }
        }          
    }
    
    /**
     * Method used to start MotifLab with command-line interface
     * This is the very first method that is executed on startup
     * @param args
     */
    public static void main(String[] args) {     
        String protocolFilename=null;
        String sequencesFilename=null;
        String outputdirFilename=null;
        String inputSessionName=null;    
        String outputSessionName=null;  
        String sequenceFormat=null;
        String build=null;    
        int splitSize=0;
        BufferedWriter logger=null;
        int retrymode=-1;
        int loggermode=LOGGER_MODE_BRIEF;
        boolean saveoutput=true;
        boolean assignNewNames=false;            
        HashMap<String,Object> configsettings=null;
        HashMap<String,String[]> pluginsettings=null;
        HashMap<String,String> promptinjection=null;
        HashMap<String,String> macros=null;
        HashMap<String,String> renameoutput=null;
        try {
            args=preprocessArguments(args); // group together quoted arguments
        } catch (SystemError e) {
            reportErrorShowUsageAndAbort(e.getMessage());
        }       
        for (int i=0;i<args.length;i++) {
            if (!args[i].startsWith("-")) reportErrorShowUsageAndAbort("Options must start with '-'");    
            else if (args[i].equals("-p") || args[i].equals("-protocol")) {
                protocolFilename=getOptionValue(args, i);
                i++; // skip ahead            
            }
            else if (args[i].equals("-s") || args[i].equals("-sequences")) {
                sequencesFilename=getOptionValue(args, i);
                i++; // skip ahead            
            }
            else if (args[i].equals("-i") || args[i].equals("-input")) {
                String[] pair=getOptionValues(args, i ,2);
                if (promptinjection==null) promptinjection=new HashMap<String, String>();
                promptinjection.put(pair[0], pair[1]);
                i+=2; // skip ahead
            }      
            else if (args[i].equals("-m") || args[i].equals("-macro")) {
                String[] pair=getOptionValues(args, i ,2);
                if (macros==null) macros=new HashMap<String, String>();
                macros.put(pair[0], pair[1]); // Note that enclosing quotes are stripped from arguments in preprocessing step, so it should be possible to define and empty macro like so: -macro nameOfMacro ""
                i+=2; // skip ahead
            }                  
            else if (args[i].equals("-o") || args[i].equals("-output")) {
                String[] pair=getOptionValues(args, i ,2);
                if (renameoutput==null) renameoutput=new HashMap<String, String>();
                renameoutput.put(pair[0], pair[1]);
                i+=2; // skip ahead
            }            
            else if (args[i].equals("-dir") || args[i].equals("-outputdir")) {
                outputdirFilename=getOptionValue(args, i);
                i++; // skip ahead
            }
            else if (args[i].equals("-c") || args[i].equals("-config") || args[i].equals("-configuration")) {
                String[] pair=getOptionValues(args, i ,2);
                if (configsettings==null) configsettings=new HashMap<String, Object>();
                String key=pair[0];
                Object value=StandardDisplaySettingsParser.parseUnknown(pair[1]);
                configsettings.put(key,value);
                i+=2; // skip ahead
            }    
            else if (args[i].equals("-plugin")) {
                String[] triple=getOptionValues(args, i ,3);
                if (pluginsettings==null) pluginsettings=new HashMap<String, String[]>();
                String pluginName=triple[0];
                String key=triple[1];
                String value=triple[2];
                pluginsettings.put(pluginName,new String[]{key,value});
                i+=3; // skip ahead
            }            
            else if (args[i].equals("-l") || args[i].equals("-log")) {
                String logfilename=getOptionValue(args, i);
                try {
                    File file=new File(logfilename);
                    logger=new BufferedWriter(new FileWriter(file));
                } catch (IOException e) {
                    reportErrorAndAbort(e.getMessage());
                }
                i++; // skip ahead
            } 
            else if (args[i].equals("-r") || args[i].equals("-retry")) {
                String retryString=getOptionValue(args, i);
                if (retryString.equalsIgnoreCase("off")) retrymode=-2;
                else if (retryString.equalsIgnoreCase("on") || retryString.equalsIgnoreCase("prompt")) retrymode=-1;
                else try {
                    retrymode=Integer.parseInt(retryString);
                    if (retrymode<0) retrymode=-1;
                } catch (NumberFormatException e) {
                    retrymode=-1;
                }                
                i++; // skip ahead            
            }         
            else if (args[i].equalsIgnoreCase("-restoreSession") || args[i].equalsIgnoreCase("-inputSession") || args[i].equalsIgnoreCase("-is")) {
                inputSessionName=getOptionValue(args, i);            
                i++; // skip ahead            
            }   
            else if (args[i].equalsIgnoreCase("-saveSession") || args[i].equalsIgnoreCase("-outputSession") || args[i].equalsIgnoreCase("-os")) {
                outputSessionName=getOptionValue(args, i);            
                i++; // skip ahead            
            }  
            else if (args[i].equalsIgnoreCase("-build") || args[i].equalsIgnoreCase("-genomebuild") || args[i].equalsIgnoreCase("-genome") || args[i].equalsIgnoreCase("-b")) {
                build=getOptionValue(args, i);        
                if (!Organism.isGenomeBuildSupported(build)) reportErrorAndAbort("Unrecognized genome build: "+build);               
                i++; // skip ahead            
            }   
            else if (args[i].equalsIgnoreCase("-format")) {
                sequenceFormat=getOptionValue(args, i);                      
                i++; // skip ahead            
            }             
            else if (args[i].equalsIgnoreCase("-split")) {
                String splitSizeString=getOptionValue(args, i);  
                try {
                    splitSize=Integer.parseInt(splitSizeString);
                } catch (NumberFormatException ne) {
                    reportErrorAndAbort("The 'split' option must specify an integer number");
                }
                i++; // skip ahead            
            }      
            else if (args[i].equalsIgnoreCase("-filepreferences")) {
                // This option can be used to store "preferences" to a regular file rather than using the default storage solution (e.g. "the registry" in Windows).
                // This may be necessary to activate if the user does not have the proper access rights
                System.setProperty("java.util.prefs.PreferencesFactory", motiflab.engine.util.FilePreferencesFactory.class.getName());               
            }
            else if (args[i].equals("-h") || args[i].equals("-help")) {showUsage(System.out);System.exit(0);}
            else if (args[i].equals("-v") || args[i].equals("-version")) {reportVersionAndExit();}
            else if (args[i].equals("-newNames") ) {assignNewNames=true;}
            else if (args[i].equals("-verbose") ) {loggermode=LOGGER_MODE_VERBOSE;}
            else if (args[i].equals("-silent") ) {loggermode=LOGGER_MODE_SILENT;}
            else if (args[i].equals("-no_output")) {saveoutput=false;}
            else if (args[i].equalsIgnoreCase("-cli")) {} // just an option to start the CLI-client. This can safely be ignored since we are already running the CLI-client 
            else if (args[i].equalsIgnoreCase("-gui")) {reportErrorShowUsageAndAbort("The GUI-client can not be started in this way");} //
            else if (args[i].equalsIgnoreCase("-minimal") || args[i].equalsIgnoreCase("-minimalGUI")) {reportErrorShowUsageAndAbort("The minimalGUI-client can not be started in this way");} //
            else {
                reportErrorShowUsageAndAbort("Unknown option: "+args[i]);
            }            
        }
        
        if (protocolFilename==null && configsettings==null && pluginsettings==null) reportErrorShowUsageAndAbort("No protocol file specified"); 
        MotifLabEngine mEngine=MotifLabEngine.getEngine();        
        MotifLab motiflab=new MotifLab(mEngine); // motiflab is the client       
        // set MotifLab directory
        if (mEngine.getMotifLabDirectory()==null || mEngine.getMotifLabDirectory().isEmpty()) {   
            try {
               MotifLabApp application=Application.getInstance(MotifLabApp.class);          
               String workDir=application.getContext().getLocalStorage().getDirectory().getAbsolutePath();            
               mEngine.setMotifLabDirectory(workDir);       
            } catch (Exception lk) {
                
            }             
        }               
        mEngine.initialize();           
        if (mEngine.isFirstTimeStartup()) {
            String document="\n\n" +
                    "###################################\n" +
                    "###################################\n" +
                    "###                             ###\n" +
                    "###                             ###\n" +
                    "###     Welcome to MotifLab     ###\n" +
                    "###                             ###\n" +
                    "###                             ###\n" +
                    "###################################\n" +
                    "###################################\n" +
                    "\n" +
                    "MotifLab is a workbench for motif discovery and regulatory sequence analysis that can integrate various tools and data sources." +
                    "\n\n" +
                    "The software was developed by Kjetil Klepper (kjetil.klepper@ntnu.no) under the supervision of professor Finn Drabløs (finn.drablos@ntnu.no)\n" +
                    "The project was supported by the National Programme for Research in Functional Genomics in Norway (FUGE) " +
                    "in the Research Council of Norway." +
                    "\n\nMotifLab is open source and free to use \"as is\" for academic and commercial purposes.\n" +
                    "However, no parts of the source code may be reused in its original or modified form as part of other commercial software projects without the consent of the authors.\n" +
                    "Also, it is not permitted to sell or otherwise redistribute MotifLab for profit (directly or indirectly) without the authors' consent.\n"+
                    "Note that MotifLab can link to other external programs whose use may be subject to other license restrictions." +
                    "\n\n" +
                    "Bioinformatics and Gene Regulation Group (BiGR)\n"+
                    "Department of Clinical and Molecular Medicine\n"+
                    "Norwegian University of Science and Technology (NTNU)\n" +
                    "\n-----------------------------------------------------------------------------------\n\n"+
                    "";    
            System.out.print(document);
            try {             
                if (mEngine.getClient()==null) mEngine.setClient(motiflab); 
                mEngine.installResourcesFirstTime();
            } catch (SystemError e) {
                System.err.println(e.getMessage());
                System.exit(1); 
            }
        }         
        boolean finish=false;
        if (protocolFilename==null && pluginsettings==null) {finish=true;} // the engine was just started to set a few configs
        if (configsettings!=null) {
            try {MotifLabEngine.getEngine().setConfigurations(configsettings);}
            catch (ExecutionError e) {motiflab.errorMessage(e.getMessage(),0);finish=true;}                       
        }       
        if (finish) { // nothing more do be done... just exit right away!
            motiflab.shutdown();
            System.exit(0);                
        }             
        motiflab.getEngine().executeStartupConfigurationProtocol();
        motiflab.setProtocolFilename(protocolFilename);
        motiflab.setSequencesFilename(sequencesFilename);       
        motiflab.setOutputDirectoryName(outputdirFilename); 
        motiflab.setInputSession(inputSessionName);     
        motiflab.setOutputSession(outputSessionName);           
        motiflab.setLogger(logger);     
        motiflab.setLoggerVerbosity(loggermode);
        motiflab.setPluginConfigurations(pluginsettings);
        motiflab.macros=macros;       
        motiflab.retry=retrymode;        
        motiflab.saveoutput=saveoutput;   
        motiflab.setPromptInjections(promptinjection);
        motiflab.renameOutput=renameoutput;
        motiflab.splitGroupSize=splitSize;        
        motiflab.defaultGenomeBuild=build;  
        motiflab.assignNewSequenceNames=assignNewNames;
        motiflab.sequenceFormat=sequenceFormat;
        System.out.println("Execution starting!");
        motiflab.run();
        System.out.println("Execution finished!");
        System.exit(0);
    }
       
    private static void reportErrorAndAbort(String msg) {
        System.err.println("ERROR: "+msg);
        System.exit(1);
    }
    private static void reportErrorShowUsageAndAbort(String msg) {
        System.err.println("ERROR: "+msg);
        showUsage(System.err);
        System.exit(1);
    }    
    
    private static void reportVersionAndExit() {
        System.err.println("MotifLab version "+MotifLabEngine.getVersion());
        System.exit(0);
    }       
    
    private static String getOptionValue(String[] args, int optionpos) {
        if (optionpos+1>=args.length || args[optionpos+1].startsWith("-")) {
            reportErrorAndAbort("Missing value for option '"+args[optionpos]+"'");
            return null;
        }
        else return args[optionpos+1];
    }
    
    private static String[] getOptionValues(String[] args, int optionpos, int values) {
        if (optionpos+values>=args.length) {
            reportErrorAndAbort("Missing all required values for option '"+args[optionpos]+"'");
            return null;
        }
        for (int i=1;i<=values;i++) {
           if (args[optionpos+i].startsWith("-")) {
               reportErrorAndAbort("Missing all required values for option '"+args[optionpos]+"'");
               return null;
           }             
        }
        String[] returnvalues=new String[values];
        for (int i=0;i<values;i++) returnvalues[i]=args[optionpos+i+1];
        return returnvalues;
    }    
    
    /**
     * Command-line arguments will normally be split on spaces and returned as a String[]
     * with each element corresponding to one word.
     * This method will group together double-quoted strings in the regular arguments array
     * into one element (and also strip the quotes).
     * E.g. the command line: '-dir outputDir -i text1 "hello world" -v'
     *      will normally return String[]{-dir,outputDir,-i,text1,"hello,world",-v}
     *      but this method will convert it to String[]{-dir,outputDir,-i,text1,hello world,-v}
     * @param arg
     * @return 
     */
    private static String[] preprocessArguments(String[] args) throws SystemError {
        ArrayList<String> newlist=new ArrayList<String>();
        String running=null;
        for (int i=0;i<args.length;i++) {
            String arg=args[i];
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                if (running!=null) throw new SystemError("Unmatched quotes");
                arg=arg.substring(1, arg.length()-1);
                newlist.add(arg);
                running=null;
            } else if (arg.startsWith("\"")) {
                arg=arg.substring(1, arg.length());
                running=arg;
            } else if (arg.endsWith("\"")) {
                running+=arg.substring(0, arg.length()-1);             
                newlist.add(running);
                running=null;
            } else {
                if (running!=null) running+=arg; // inside a quote
                else newlist.add(arg);
            }   
        }
        if (running!=null) throw new SystemError("Unmatched quotes");
        String[] list=new String[newlist.size()];
        return newlist.toArray(list);
    }
    
    /**
     * Parses the specified sequence-file and returns an array of corresponding Sequence objects
     * for sequences found
     * @param filename
     * @return
     */
    public static Sequence[] parseSequencesFile(String filename, int startIndex, int endIndex, String sequenceFormat, String defaultGenomeBuild, boolean assignNewSequenceNames, boolean silent, MotifLabEngine engine) throws Exception {
        int formatOverride=-1;
        String[] bedFields=null;
        if (sequenceFormat!=null) {
                 if (sequenceFormat.equalsIgnoreCase("BED")) formatOverride=BED;
            else if (sequenceFormat.equalsIgnoreCase("geneID")) formatOverride=DataFormat_Location.GENE_ID_FIELDS_COUNT;
            else if (sequenceFormat.equalsIgnoreCase("manual4")) formatOverride=DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_4;
            else if (sequenceFormat.equalsIgnoreCase("manual8")) formatOverride=DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_8;
            else if (sequenceFormat.equalsIgnoreCase("manual10")) formatOverride=DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_10;
            else if (sequenceFormat.contains(",")) {
                bedFields=sequenceFormat.split("\\s*,\\s*");
                formatOverride=CUSTOM_BED;
            }
            else {
                throw new ExecutionError("Unknown sequence format "+sequenceFormat);
            }
        }
        ArrayList<Sequence> sequences=new ArrayList<Sequence>();
        ArrayList<String[]> list=readSequencesFile(filename, startIndex, endIndex, engine);
        ArrayList<GeneIdentifier> geneIDs=new ArrayList<GeneIdentifier>();
        // locate all Gene ID entries, parse these and add GeneIdentifier object to list to be resolved
        if (sequenceFormat==null || sequenceFormat.equalsIgnoreCase("geneID")) {
            for (int i=0;i<list.size();i++) {
                String[] entry=list.get(i);
                if (entry.length==DataFormat_Location.GENE_ID_FIELDS_COUNT && !couldThisBeBED(entry)) { // line specifies a sequence using Gene ID
                    geneIDs.add(processGeneIDLine(entry));
                }
            }
        }
        ArrayList<GeneIDmapping> resolved=resolveGenesFromList(geneIDs,engine);
        if (resolved==null) resolved=new ArrayList<GeneIDmapping>();
        // Process full list again, this time resolved Gene ID info should be available
        for (int i=0;i<list.size();i++) {
            String[] entry=list.get(i);
            int lineFormat=formatOverride;
            if (lineFormat<0) { // try to determine format from line
                if (couldThisBeBED(entry)) lineFormat=BED;
                else if (entry.length==DataFormat_Location.GENE_ID_FIELDS_COUNT) lineFormat=DataFormat_Location.GENE_ID_FIELDS_COUNT;
                else if (entry.length==DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_4) lineFormat=DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_4;
                else if (entry.length==DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_8) lineFormat=DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_8;
                else if (entry.length==DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_10) lineFormat=DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_10;
            }
            if (lineFormat==BED || lineFormat==CUSTOM_BED) {
                     Sequence seq=null;                   
                     try {
                         if (lineFormat==BED) seq=Sequence.processBEDformat(entry);
                         else { // custom format
                             String line=MotifLabEngine.splice(entry, "\t");
                             HashMap<String, Object> map=DataFormat_BED.parseSingleLine(line, 0, true, bedFields);
                             String chr=null;
                             int start=-1;
                             int end=-1;
                             String type=null;
                             int strand=Sequence.DIRECT;                            
                             if (map.containsKey("CHROMOSOME")) chr=(String)map.get("CHROMOSOME"); else throw new ParseError("Missing 'chromosome' column in custom BED format");
                             if (map.containsKey("START")) start=(Integer)map.get("START"); else throw new ParseError("Missing 'start' column in custom BED format");
                             if (map.containsKey("END")) end=(Integer)map.get("END"); else throw new ParseError("Missing 'end' column in custom BED format");
                             if (map.containsKey("STRAND")) {
                                 String strandString=(String)map.get("STRAND");
                                 strand=strandString.equals("-")?Sequence.REVERSE:Sequence.DIRECT;
                             }
                             if (map.containsKey("TYPE")) type=(String)map.get("TYPE");
                             if (end<start) {
                                 int swap=start;
                                 start=end;
                                 end=swap;
                                 strand=Sequence.REVERSE;
                             }
                             String sequenceName=(type!=null)?type:(chr+"_"+start+"_"+end);
                             seq=new Sequence(sequenceName, 0, null, chr, start, end, null,null,null,strand);
                             for (String key:map.keySet()) {
                                 if (   key.equalsIgnoreCase("CHROMOSOME")
                                     || key.equalsIgnoreCase("START")
                                     || key.equalsIgnoreCase("END")
                                     || key.equalsIgnoreCase("TYPE")
                                     || key.equalsIgnoreCase("STRAND") || key.equalsIgnoreCase("ORIENTATION")
                                     || key.equalsIgnoreCase("SCORE")
                                     || key.equalsIgnoreCase("ORGANISM")
                                 ) continue;
                                 if (key.equalsIgnoreCase("TSS")) seq.setTSS((Integer)map.get(key));
                                 if (key.equalsIgnoreCase("TES")) seq.setTES((Integer)map.get(key));
                                 if (key.equalsIgnoreCase("gene") || key.equalsIgnoreCase("genename")) seq.setGeneName((String)map.get(key));
                                 if (key.equalsIgnoreCase("build") || key.equalsIgnoreCase("genomeBuild")) {
                                     String build=(String)map.get(key);
                                     int organism=Organism.getOrganismForGenomeBuild(build);
                                     String defBuild=Organism.getDefaultBuildName(build, organism);
                                     seq.setOrganism(organism);
                                     seq.setGenomeBuild(defBuild);
                                 }
                                 seq.setPropertyValue(key, map.get(key));
                             }
                             
                         }
                     } catch (Exception e) {
                         throw new ParseError("Parse Error: "+e.getMessage());
                     }                     
                     if (seq!=null) {                       
                         if (seq.getGenomeBuild()==null && defaultGenomeBuild!=null) {
                            int organism=Organism.getOrganismForGenomeBuild(defaultGenomeBuild);
                            seq.setGenomeBuild(defaultGenomeBuild);
                            seq.setOrganism(organism);
                         }      
                         if (assignNewSequenceNames) seq.rename(getNewSequenceName(i+1, list.size()));
                         else {
                             String sequencename=seq.getName();
                             String fail=engine.checkSequenceNameValidity(sequencename, false);
                             if (fail!=null) {
                                 if (engine.autoCorrectSequenceNames()) { // sequencename contains illegal characters that should be corrected  
                                     String newsequencename=MotifLabEngine.convertToLegalSequenceName(sequencename);
                                     engine.logMessage("NOTE: sequence '"+sequencename+"' was renamed to '"+newsequencename+"'");
                                     seq.rename(newsequencename);                                 
                                 } else {
                                     engine.errorMessage("Illegal sequence name '"+sequencename+"': "+fail, 0);
                                     System.exit(1);
                                 }
                             }
                         }
                         if (seq.getSize()>engine.getMaxSequenceLength()) engine.errorMessage("Warning: Size of sequence '"+seq.getName()+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp). Sequence skipped",0);
                         else sequences.add(seq);
                     }                    
            } 
            else if (lineFormat==DataFormat_Location.GENE_ID_FIELDS_COUNT) { // line specifies a sequence using Gene ID
                Object[] result=processGeneIDLineInFull(entry, defaultGenomeBuild);
                GeneIdentifier geneid=(GeneIdentifier)result[0];                
                int upstream=(Integer)result[1]; 
                int downstream=(Integer)result[2]; 
                String anchor=(String)result[3]; 
                ArrayList<Sequence> choice=resolveSequenceForGeneID(geneid,resolved,upstream,downstream,anchor, silent, engine);              
                int c=0;
                for (Sequence seq:choice) {                   
                    if (assignNewSequenceNames) {
                        char suffix = (char)('a' + c);
                        seq.rename(getNewSequenceName(i+1, list.size())+suffix);
                        c++;
                    }
                    else {                    
                         String sequencename=seq.getName();
                         String fail=engine.checkSequenceNameValidity(sequencename, false);
                         if (fail!=null) {
                             if (engine.autoCorrectSequenceNames()) { // sequencename contains illegal characters that should be corrected  
                                 String newsequencename=MotifLabEngine.convertToLegalSequenceName(sequencename);
                                 engine.logMessage("NOTE: sequence '"+sequencename+"' was renamed to '"+newsequencename+"'");
                                 seq.rename(newsequencename);                                 
                             } else {
                                 throw new ExecutionError("Illegal sequence name '"+sequencename+"': "+fail, 0);
                             }
                         }
                    }
                }
                sequences.addAll(choice);
            } else if (lineFormat==DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_8){
                     Sequence seq=null;
                     try {
                         seq=Sequence.processManualEntryLine8(entry); 
                     } catch (ExecutionError e) {
                         throw new ParseError("Parse Error: "+e.getMessage(),0);
                     }
                     if (seq!=null) {
                         if (assignNewSequenceNames) seq.rename(getNewSequenceName(i+1, list.size()));
                         else {                             
                            String sequencename=seq.getName();
                            String fail=engine.checkSequenceNameValidity(sequencename, false);
                            if (fail!=null) {
                                 if (engine.autoCorrectSequenceNames()) { // sequencename contains illegal characters that should be corrected  
                                     String newsequencename=MotifLabEngine.convertToLegalSequenceName(sequencename);
                                     engine.logMessage("NOTE: sequence '"+sequencename+"' was renamed to '"+newsequencename+"'");
                                     seq.rename(newsequencename);                                 
                                 } else {
                                     engine.errorMessage("Illegal sequence name '"+sequencename+"': "+fail, 0);
                                     System.exit(1);
                                 }
                            }    
                         }
                         if (seq.getSize()>engine.getMaxSequenceLength()) engine.errorMessage("Warning: Size of sequence '"+seq.getName()+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp). Sequence skipped",0);
                         else sequences.add(seq);
                     }
            } else if (lineFormat==DataFormat_Location.MANUAL_ENTRY_FIELDS_COUNT_10){
                     Sequence seq=null;
                     try {
                         seq=Sequence.processManualEntryLine10(entry);
                     } catch (ExecutionError e) {
                         throw new ParseError("Parse Error: "+e.getMessage(),0);
                     }
                     if (seq!=null) {
                         if (assignNewSequenceNames) seq.rename(getNewSequenceName(i+1, list.size()));
                         else {                           
                             String sequencename=seq.getName();
                             String fail=engine.checkSequenceNameValidity(sequencename, false);
                             if (fail!=null) {
                                 if (engine.autoCorrectSequenceNames()) { // sequencename contains illegal characters that should be corrected  
                                     String newsequencename=MotifLabEngine.convertToLegalSequenceName(sequencename);
                                     engine.logMessage("NOTE: sequence '"+sequencename+"' was renamed to '"+newsequencename+"'");
                                     seq.rename(newsequencename);                                 
                                 } else {
                                     engine.errorMessage("Illegal sequence name '"+sequencename+"': "+fail, 0);
                                     System.exit(1);
                                 }
                             }      
                         }
                         if (seq.getSize()>engine.getMaxSequenceLength()) engine.errorMessage("Warning: Size of sequence '"+seq.getName()+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp). Sequence skipped",0);
                         else sequences.add(seq);
                     }
            } else  {
                engine.errorMessage("Wrong number of fields in sequence file (non-BED format)",0);
            }
        }     
        if (sequences.isEmpty() && startIndex<0) { // no sequences found (and no subrange specified)
           throw new ExecutionError("Execution Error: No sequences to analyse",0);                         
        }
        Sequence[] result=new Sequence[sequences.size()];
        for (int i=0;i<result.length;i++) result[i]=sequences.get(i);
        // check that all the names are unique so that sequences will not overwrite each other
        HashSet<String> names=new HashSet<String>();
        for (int i=0;i<result.length;i++) {
            String name=result[i].getSequenceName();
            if (names.contains(name)) engine.logMessage("WARNING: Duplicate sequence with same name '"+name+"' will replace previous sequence");
            else names.add(name);
        }
        return result;
    }
    
    private static String getNewSequenceName(int i, int size) {       
        int digits=(""+size).length(); // how many digits does it take to number all lines         
        String suffix=""+i;
        while (suffix.length()<digits) suffix="0"+suffix;
        return "Sequence"+suffix;   
    }
    
    /** Returns true if the first three fields is on the format: chrX start end */
    private static boolean couldThisBeBED(String[] fields) {
        if (fields.length<3) return false;
        if (!fields[0].startsWith("chr")) return false;
        try {
            Integer.parseInt(fields[1]);
            Integer.parseInt(fields[2]);
            return true;
        } catch (NumberFormatException ne) {
            return false;
        }
    }    
    
    
    /** 
     * Reads a file containing sequence specifications. Each line should specify one sequence
     * in a comma or tab-delimited format using either MANUAL_ENTRY_FIELDS_COUNT fields (for manual entries) or GENE_ID_FIELDS_COUNT fields (for gene ID refs)
     * @return an ArrayList with String arrays containing relevant info. The String-arrays have lengths GENE_ID_FIELDS_COUNT or MANUAL_ENTRY_FIELDS_COUNT
     */
    public static ArrayList<String[]> readSequencesFile(String filename, int startIndex, int endIndex, MotifLabEngine engine) throws ExecutionError{
        ArrayList<String[]> results=new ArrayList<String[]>();
        BufferedReader inputStream=null;
        int validLineNumber=-1; // This is incremented for each valid entry line
        try {
            File file=engine.getFile(filename);
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String originalLine;
            while((originalLine=inputStream.readLine())!=null) {
                String line=originalLine.trim();
                if (!(line.isEmpty() || line.startsWith("#"))) {
                    validLineNumber++;
                    line=line.replace("\t", ",");
                    String[] fields=line.split("\\s*,+\\s*");                    
                    if (startIndex>=0 && endIndex>=startIndex) { // Return a subrange
                        if (validLineNumber>=startIndex) results.add(fields);
                        if (validLineNumber==endIndex) break; // last subset index. Do not read further
                    } else results.add(fields); // add every line
                }
            }
        } catch (IOException e) { 
             throw new ExecutionError("SYSTEM ERROR: While parsing sequences file '"+filename+"': "+e.getMessage(), e);
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {}
        }
        return results;      
    }
    

    
    /** Processes a line with GENE_ID_FIELDS_COUNT String entries and returns a GeneIdentifier */
    private static GeneIdentifier processGeneIDLine(String[] fields)  { 
        String identifier=fields[0];
        String idFormat=fields[1];
        String build=fields[2];
        int organism=Organism.getOrganismForGenomeBuild(build);       
        return new GeneIdentifier(identifier, idFormat, organism, build);
    }
    
    /** 
     * Processes a line with GENE_ID_FIELDS_COUNT String entries and returns an object array
     * containing 
     * [0] a GeneIdentifier,
     * [1] A 'from' (upstream) specification 
     * [2] A 'to' (downstream) specification
     * [3] An anchor (eg. "TSS", "TES" or "GENE")
     * 
     */
    private static Object[] processGeneIDLineInFull(String[] fields, String defaultGenomeBuild) throws ParseError {  
        Object[] result=new Object[]{null,null,null,null};
        String identifier=fields[0];
        String idFormat=fields[1];
        String build=(fields[2]!=null)?fields[2]:defaultGenomeBuild;
        int organism=Organism.getOrganismForGenomeBuild(build);       
        result[0]=new GeneIdentifier(identifier, idFormat, organism, build);
        try {
            int from=Integer.parseInt(fields[3]);
            result[1]=new Integer(from);
        } catch (NumberFormatException ne) {
            throw new ParseError("Parse Error: Unable to parse expected numeric range-value for 'from' (value='"+fields[3]+"')");
        }
        try {
            int to=Integer.parseInt(fields[4]);
            result[2]=new Integer(to);
        } catch (NumberFormatException ne) {
            throw new ParseError("Parse Error: Unable to parse expected numeric range-value for 'to' (value='"+fields[4]+"')");
        }
        result[3]=fields[5];
        return result;
    }
    

    /** Resolves a list of GeneIdentifiers */
    private static ArrayList<GeneIDmapping> resolveGenesFromList(ArrayList<GeneIdentifier> idlist, MotifLabEngine engine) throws ExecutionError{
        GeneIDResolver idResolver=engine.getGeneIDResolver();
        ArrayList<GeneIDmapping> resolvedList=null;
        try {
            resolvedList=idResolver.resolveIDs(idlist);
        } catch (Exception e) { 
            throw new ExecutionError("An error occurred while resolving gene IDs: "+e.getClass().getSimpleName()+": "+e.getMessage());            
        }  
        return resolvedList;
    }
    
    
    /** 
     * Based on a GeneID specified by the user and a list of previously resolved gene IDs (among which the first argument geneid should hopefully be present)
     * the method returns a list of Sequence objects corresponding to the selected gene ID.
     * Note that when resolving Gene IDs, several different hits could be returned. 
     * The user is prompted to select the correct one (or several) and the ones the user selects are turned into Sequence objects
     * and returned as a list
     */
    private static ArrayList<Sequence> resolveSequenceForGeneID(GeneIdentifier geneid, ArrayList<GeneIDmapping> resolvedList, int upstream, int downstream, String anchor, boolean silentmode, MotifLabEngine engine) throws ExecutionError {
           ArrayList<Sequence> sequences=new ArrayList<Sequence>();
           ArrayList<GeneIDmapping> listForGene=getEntriesForID(resolvedList,geneid.identifier);
           if (listForGene.isEmpty()) {
               engine.logMessage("Unable to find information about "+Organism.getCommonName(geneid.organism).toLowerCase()+" "+geneid.format+" identifier: "+geneid.identifier);
           } else if (listForGene.size()>1) {
               if (!silentmode) listForGene=engine.getClient().selectCorrectMappings(listForGene,geneid.identifier); // prune list based on user selections
           } 
           for (GeneIDmapping mapping:listForGene) { // add all those mappings that are left for this ID
               Sequence sequence=new Sequence(mapping.geneID, new Integer(geneid.organism), geneid.build, mapping.chromosome, 0, 0, mapping.geneID, mapping.TSS, mapping.TES, mapping.strand);
               fillInStartAndEndPositions(sequence, upstream, downstream, anchor);
               sequence.setUserDefinedPropertyValue(geneid.format, mapping.geneID);
               if (mapping.GOterms!=null && !mapping.GOterms.isEmpty()) {
                   try {sequence.setGOterms(mapping.GOterms);} catch (ParseError e) {} // The terms should have been checked many times already, so just ignore errors at this point
               }
               if (sequence.getSize()>engine.getMaxSequenceLength()) {
                   engine.errorMessage("Warning: Size of sequence '"+sequence.getName()+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp). Sequence skipped",0);
               } else if (sequence.getSize()<0) {
                   engine.errorMessage("Warning: end position located prior to start position in sequence '"+sequence.getName()+"'. Sequence skipped",0);
               } else sequences.add(sequence);
           }        
           return sequences;
    }        
        
      

    
    
        
    
    /** Goes through a list of GeneIDmapping and returns only those entries that correspond to the given gene id */
    private static ArrayList<GeneIDmapping> getEntriesForID(ArrayList<GeneIDmapping> list, String id) {
        ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>();
        for (GeneIDmapping entry:list) {
            if (entry.geneID.equalsIgnoreCase(id)) result.add(entry);
        }
        return result;
    }
    
/** Fills in upstream and downstream coordinates based on user selections and gene orientation */
    private static void fillInStartAndEndPositions(Sequence sequence, int upstream, int downstream, String anchor) throws ExecutionError {
        if (upstream>0) upstream--; // to account for direct transition from -1 to +1 at TSS
        if (downstream>0) downstream--; // to account for direct transition from -1 to +1 at TSS
        int tss=sequence.getTSS();
        int tes=sequence.getTES();
        if (anchor.equalsIgnoreCase("Transcription Start Site") || anchor.equalsIgnoreCase("TSS")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tss+upstream);
               sequence.setRegionEnd(tss+downstream);           
            } else { // Reverse Strand
               sequence.setRegionStart(tss-downstream);
               sequence.setRegionEnd(tss-upstream);                
            }
        } else if (anchor.equalsIgnoreCase("Transcription End Site") || anchor.equalsIgnoreCase("TES")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tes+upstream);
               sequence.setRegionEnd(tes+downstream);           
            } else { // Reverse Strand
               sequence.setRegionStart(tes-downstream);
               sequence.setRegionEnd(tes-upstream);                
            }        
        } else if (anchor.equalsIgnoreCase("gene") || anchor.equalsIgnoreCase("full gene") || anchor.equalsIgnoreCase("transcript")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tss+upstream);
               sequence.setRegionEnd(tes+downstream);           
            } else { // Reverse Strand
               sequence.setRegionStart(tes-downstream);
               sequence.setRegionEnd(tss-upstream);                
            }        
        } else {
            throw new ExecutionError("Unsupported anchor site: "+anchor);
        }
    }      
    

    @Override
    public ArrayList<GeneIDmapping> selectCorrectMappings(ArrayList<GeneIDmapping> list, String id) {
        ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>();
        clientConsole.println("Found several matches for identifier '"+id+"'. Please select the correct one (or comma-separated list)");
        for (int i=0;i<list.size();i++) {
            GeneIDmapping mapping = list.get(i);
            int start=(mapping.TSS<mapping.TES)?mapping.TSS:mapping.TES;
            int end  =(mapping.TSS<mapping.TES)?mapping.TES:mapping.TSS;
            String string=id+"_"+(i+1);
            string=padToLength(string, 20, false);
            string+=mapping.geneName;
            string=padToLength(string, 30, false);
            String chromosome="chr"+mapping.chromosome+":"+start+"-"+end;
            chromosome=padToLength(chromosome, 30, true);
            string+=chromosome+"       ";
            string+=((mapping.strand==Sequence.DIRECT)?"Direct          ":"Reverse          ");              
            clientConsole.println("["+(i+1)+"] "+string);
        }
        boolean ok=false;
        ArrayList<Integer> indices=new ArrayList<Integer>();
        while (!ok) {
            clientConsole.print("Enter selection: ");
            String userInput=clientConsole.readLine();
            if (userInput==null || userInput.trim().isEmpty()) continue;
            String[] elements=userInput.split("\\s*,\\s*");     
            for (String element:elements) {
                indices.clear();
                try {
                    Integer number=Integer.parseInt(element);
                    if (number<0 || number>list.size()) {
                        clientConsole.println("Selected number out of range: '"+number+"'");
                        break;                        
                    } else indices.add(number);
                } catch (NumberFormatException e) {
                    clientConsole.println("Unable to parse number: '"+element+"'");
                    break;
                }
                ok=true;
            }
        }
        for (Integer index:indices) {
            try {
                GeneIDmapping mapping=list.get(index);
                mapping.geneID=mapping.geneID+"_"+(index+1);
                result.add(mapping);
            } catch (NumberFormatException e) {}
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
    
    @Override
    public ParameterSettings promptForValues(Parameter[] parameters, ParameterSettings defaultsettings, String message, String title) {
        if (message!=null) {
            if (message.startsWith("[ERROR]")) message=message.substring("[ERROR]".length()); // this is just a flag that is mainly used by the GUI to highlight errors differently than regular messages
            clientConsole.println("\n"+message+"\n");
        }
        ParameterSettings finalValues=new ParameterSettings();
        for (Parameter par:parameters) {            
            if (par.isHidden()) continue;
            boolean required=par.isRequired();
            Class type=par.getType();
            String parametername=par.getName();

            Object userSelected=null;
            if (defaultsettings!=null) userSelected=defaultsettings.getParameterAsString(parametername,parameters);
            else userSelected=par.getDefaultValue();
            
            Object allowedValues=par.getAllowedValues();
            boolean hasOptions=(allowedValues instanceof Object[] || allowedValues instanceof Class || allowedValues instanceof Class[]);
            ArrayList<String> options=getParameterOptionsAsStrings(allowedValues);
            boolean ok=false;
            while(!ok) {
               clientConsole.print(parametername+" ");
               if (userSelected!=null) { // defaultValue
                   clientConsole.print("["+userSelected+"]"); 
               }
               clientConsole.print(": ");
               String userInput=null;
               if (par.hasAttributeValue("ui", "password")) userInput=clientConsole.readPassword();
               else userInput=clientConsole.readLine();
               if (userInput.isEmpty()) userInput=userSelected.toString();
               if (userInput.equals("?")) {                   
                   int counter=0;
                   for (String option:options) {
                       counter++;
                       clientConsole.print("["+counter+"] "+option); 
                   }
                   continue; // prompt again after informing the user of the options
               }
               if (required && (userInput==null || userInput.trim().isEmpty())) continue; // the user did not enter a value for a required parameter. Prompt again!
               
               if (type==Boolean.class) {
                   if (userInput.equalsIgnoreCase("true") || userInput.equalsIgnoreCase("yes") || userInput.equalsIgnoreCase("on")) {finalValues.setParameter(parametername, Boolean.TRUE);ok=true;}
                   else if (userInput.equalsIgnoreCase("false") || userInput.equalsIgnoreCase("no") || userInput.equalsIgnoreCase("off")) {finalValues.setParameter(parametername, Boolean.FALSE);ok=true;}
                   else {clientConsole.print("Invalid boolean value");}
               } else { // all other values. I will not check that they are valid, except when explicit options are provided
                   if (hasOptions) {                    
                      Integer number=null;
                      try {number=Integer.parseInt(userInput);} catch (NumberFormatException e) {}
                      if (number==null) { // regular text input
                          if (options.contains(userInput)) {finalValues.setParameter(parametername, userInput);ok=true;}
                          else clientConsole.print("Invalid value. Enter ? to see options.");
                      } else {
                          if (number>=1 && number<=options.size()) {finalValues.setParameter(parametername, options.get(number-1));ok=true;}
                          else clientConsole.print("Invalid value. Enter ? to see options.");
                      }
                   } else {
                      finalValues.setParameter(parametername, userInput);
                      ok=true; 
                   }
               }
            }           
        } // end for each parameter        
        return finalValues;
    }     
    
    private ArrayList<String> getParameterOptionsAsStrings(Object allowedValues) {   
        if (allowedValues instanceof Object[]) {
            ArrayList<String> options=new ArrayList<String>();
            for (Object obj:(Object[])allowedValues) options.add(obj.toString());
        } else if (allowedValues instanceof Class) {
            return engine.getNamesForAllDataItemsOfType((Class)allowedValues);
        } else if (allowedValues instanceof Class[]) {
           return engine.getNamesForAllDataItemsOfTypes((Class[])allowedValues);
        }
        return new ArrayList<String>();
    }
    
    

    
   
    /** Prints usage information */
    private static void showUsage(PrintStream stream) {
        stream.println("\nMotifLab version "+MotifLabEngine.getVersion());
        stream.println("----------------------------");
        stream.println("Usage:");
        stream.println("java -cp MotifLab.jar motiflab.engine.MotifLab -p <protocolfile> [-s <sequencesfile>] [optional arguments]");
        stream.println("");
        stream.println("Recognized arguments: (note that arguments or values containing spaces must be enclosed in double quotes)");
        stream.println("      -p | -protocol <protocolfile>     The protocol to be executed");
        stream.println("      -s | -sequences <sequencesfile>   A file containing either sequences");
        stream.println("                                          in FASTA-format or a specification");
        stream.println("                                          of which sequences to use");
        stream.println("      -l | -log <logfile>               Write log messages to file");
        stream.println("      -r | -retry off|prompt|<seconds>  If errors occur, either abort execution (off),");
        stream.println("                                          prompt the user for what to do, or try again");
        stream.println("                                          after waiting the specified number of seconds");     
        stream.println("      -inputSession <file>              Load the specified session before the protocol");       
        stream.println("                                          is executed. The session can contain data");              
        stream.println("                                          that can be used by the protocol script."); 
        stream.println("      -saveSession <file>               Save everything as a session to the specified");
        stream.println("                                          file after protocol execution");       
        stream.println("      -i | -input <data> <value>        Sets the value of a data object that is");  
        stream.println("                                          prompted by the protocol script on the");        
        stream.println("                                          command line rather than interactively.");
        stream.println("                                          The value should be either a numeric value");  
        stream.println("                                          or the name of a file (in default format).");
        stream.println("                                          This option can be used multiple times");         
        stream.println("                                          to provide values for different objects.");
        stream.println("      -o | -output <outputdata> <file>  Save the given output data object to the");  
        stream.println("                                          specified file (in the output directory)");          
        stream.println("                                          rather than a default file named after");        
        stream.println("                                          the data object itself.");
        stream.println("                                          This option can be used multiple times");         
        stream.println("                                          to provide values for different objects.");        
        stream.println("      -m | -macro <name> <definition>   Defines the value of a macro.");          
        stream.println("                                          This option can be used multiple times");         
        stream.println("                                          to provide values for several macros.");        
        stream.println("      -dir | -outputdir <directory>     A directory to store output files");        
        stream.println("      -no_output | -noOutput            If this option is specified, output data will");
        stream.println("                                          not be saved to files after the protocol is");        
        stream.println("                                          finished. (This option is really only useful");
        stream.println("                                          in combination with the 'saveSession' option");
        stream.println("                                          or if the protocol itself contains commands");
        stream.println("                                          to save the relevant data)");
        stream.println("      -build <buildID>                  Sets a default genome build for sequences without");
        stream.println("                                          an explicitly defined origin");   
        stream.println("      -format <specification>           Specifies the format of the sequences file.");
        stream.println("                                          If this is not provided, MotifLab will try");  
        stream.println("                                          to determine the format of each line in the file");  
        stream.println("                                          automatically and individually based on its contents.");  
        stream.println("                                          Valid formats are (case-insensitive):");  
        stream.println("                                          BED, geneID, Manual4, Manual8 or Manual10.");  
        stream.println("                                          It is also possible to specify a comma-separated");         
        stream.println("                                          list of BED-format columns.");  
        stream.println("      -newNames                         If this option is specified, all sequences will be");
        stream.println("                                          assigned incremental names on the form 'SequenceXXXXX'");          
        stream.println("      -split <size>                     This option can be used if you want to analyze");
        stream.println("                                          a large number of sequences that can be");                
        stream.println("                                          processed independently of each other.");                
        stream.println("                                          MotifLab will split the sequences into groups");                
        stream.println("                                          with at most <size> sequences in each");
        stream.println("                                          and apply the protocol to each group in turn.");       
        stream.println("      -c | -config <key> <value>        Sets the specified configuration setting");
        stream.println("                                          This option can be used multiple times");         
        stream.println("                                          to provide values for different settings.");         
        stream.println("                                          Currently recognized settings include, e.g.:");
        stream.println("                                            '"+MotifLabEngine.PREFERENCES_USE_CACHE_FEATUREDATA+"' (boolean)");
        stream.println("                                            '"+MotifLabEngine.PREFERENCES_USE_CACHE_GENE_ID_MAPPING+"' (boolean)");
        stream.println("                                            '"+MotifLabEngine.MAX_SEQUENCE_LENGTH+"' (integer)");
        stream.println("                                            '"+MotifLabEngine.MAX_CONCURRENT_DOWNLOADS+"' (integer)");
        stream.println("                                            '"+MotifLabEngine.CONCURRENT_THREADS+"' (integer)");
        stream.println("                                            '"+MotifLabEngine.PREFERENCES_NETWORK_TIMEOUT+"' (integer)");     
        stream.println("      -plugin <name> <key> <value>      Sets a configuration parameter for a plugin");  
        stream.println("                                          This option can be used multiple times");         
        stream.println("                                          to set different parameters.");          
        stream.println("      -v | -version                     Output MotifLab version number and exit)");
        stream.println("      -verbose                          Verbose mode (output status messages)");
        stream.println("      -silent                           Silent mode (no messages are output during");
        stream.println("                                          execution except for error messages)");
        stream.println("      -h | -help                        Print this help message"); 
    }
    
    
    /** A simple ClientConsole implementation that communicates via STDIN/STDOUT */
    private static class STDConsole implements ClientConsole {

        @Override
        public void print(Object obj) {
            System.out.print(obj);
        }

        @Override
        public void println(Object obj) {
            System.out.println(obj);
        }

        @Override
        public String readLine() {
            return System.console().readLine();
        }
        
        @Override
        public String readPassword() {
           return new String(System.console().readPassword());        
        }
    }
    
}
