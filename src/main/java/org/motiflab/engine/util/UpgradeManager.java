package org.motiflab.engine.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.motiflab.engine.GeneIDResolver;
import org.motiflab.engine.MotifLabClient;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.Organism;
import org.motiflab.engine.datasource.DataConfiguration;
import org.motiflab.engine.datasource.DataSource;
import org.motiflab.engine.datasource.DataTrack;
import org.motiflab.engine.datasource.Server;
import org.motiflab.external.ExternalProgram;

/**
 *
 * @author kjetikl
 */
public class UpgradeManager {
    
    // The migration number will increase every time changes are made to one or more configuration files bundled 
    // with MotifLab which should trigger an upgrade of the corresponding files that have already been installed.
    // The migration number of the currently installed files can be found in the "status file" (first column of first row)
    private static final int MIGRATION_NUMBER=2; 
    
    private static final int DUPLICATE_SOURCES_ADD_AS_PREFERRED=0;
    private static final int DUPLICATE_SOURCES_ADD_AS_MIRRORS=1;
    private static final int DUPLICATE_SOURCES_REMOVE_OLD=2;
    private static final int DUPLICATE_SOURCES_REPLACE_ALL_SOURCES=3;
    private static final int REPLACE_EXISTING_TRACKS=4;    
    private static final int REPLACE_WHOLE_DATATRACK_CONFIG=5;      
    
    private static final String STATUS_FILE="installed.txt";
    private static File statusFile;
    private final MotifLabEngine engine;
    private final MotifLabClient client;
       
    
    
            
    public UpgradeManager(MotifLabEngine engine) {
        this.engine=engine;
        this.client=engine.getClient(); // this might be null
        statusFile = new File(engine.getMotifLabDirectory(),STATUS_FILE);
    }
    
    
    /**
     * Checks if any configuration files need to be updated because the current
     * version of MotifLab has been upgraded since these files were created
     * or MotifLab has never been run before so all configuration files must be installed from scratch
     * @return TRUE if the configuration files require updates (or first time install)
     */
    public boolean isUpgradeNeeded() {
        Integer oldMigrationNumber = getMigrationNumber();
        if (oldMigrationNumber==null) return true; // This is the first time MotifLab is being executed, so all the resources need to be installed
        else return (oldMigrationNumber<MIGRATION_NUMBER);
    }
    
    /**
     * Returns the current migration number for the previous installation
     * or NULL if MotifLab has not been installed before
     * @return the currently stored  migration number
     */
    public Integer getMigrationNumber() {
        if (!statusFile.exists()) {
            return null; // This is the first time MotifLab is being executed
        } else {
            ArrayList<String> content = readLines(statusFile);
            if (content==null || content.isEmpty()) {
               return 0; // The current installation was done by an older version of MotifLab with no migration info
            } else {
                String status = content.get(0);
                String[] fields = status.split("\t");
                try {
                    int oldMigrationNumber = Integer.parseInt(fields[0]);
                    return oldMigrationNumber;
                } catch (NumberFormatException nfe) {
                    engine.errorMessage("Unable to find installed version number for MotifLab", 0);
                    return 0;
                }
            }
        }     
    }
    
    /**
     * Checks if this is the first time ever that MotifLab is executed on this system
     * @return TRUE if this is the first time that MotifLab is being executed 
     */
    public boolean isFirstTime() {
        return (getMigrationNumber()==null);
      
    }
    
    
    /**
     * Detects the necessary upgrade steps and performs them
     * @throws SystemError if something went badly wrong
     */
    public void performUpgrade() throws SystemError {    
        client.progressReportMessage(1);        
        Integer oldMigrationNumber = getMigrationNumber();
        client.progressReportMessage(2);        
        if (oldMigrationNumber==null) { // MotifLab has never been run before, so all resource files must be installed
            performFirstTimeInstallation();
            updateVersionNumber(); 
            // if (client!=null) client.logMessage("Installation completed successfully");  
            // engine.importResources(); ==> Do this in the engine afterwards       
        } else {
            String upgradeMessage = "This version of MotifLab has made updates to some of its resources and configuration files.\n"
                                  + "It is recommended that you upgrade your existing configurations to match.";              
            client.displayMessage(upgradeMessage, MotifLabClient.WARNING_MESSAGE);                                     
            String makeBackupMessage = "Would you like to create a backup of your configuration files first (to ZIP archive)?";            
            int[] makeBackupChoice = client.selectOption(makeBackupMessage, new String[]{"Yes","No"}, false); 
            if (makeBackupChoice[0]==0) backupCurrentConfigurationFiles();
            performUpgradeSteps(oldMigrationNumber); // upgrade from older version of MotifLab with no migration number
            client.progressReportMessage(99);
            updateVersionNumber();
            client.progressReportMessage(100);
        } 
        if (isUpgradeNeeded()) { // checks that the status file is correctly updated
            client.progressReportMessage(-1);
            throw new SystemError("It seems that something went wrong during installation. Please try to restart MotifLab");
        }     
        client.progressReportMessage(-1);
        client.displayMessage("To make sure that all the new configurations have been applied, it is recommended to restart MotifLab", MotifLabClient.WARNING_MESSAGE);
    }    
            
    /**
     * Performs the upgrades that are necessary to bring the configuration files
     * from the existing old migration state of MotifLab (from the last time it was executed)
     * to the state of the current running version of MotifLab
     * @param oldMigrationNumber A number describing the current state of the configuration files
     */
    private void performUpgradeSteps(int oldMigrationNumber) {       
        if (oldMigrationNumber<2) {
             upgradeToMigrationVersion2();
        } else if (oldMigrationNumber==3) {
            // this may happen in the future. It will deal with it then...
        }
    }
    
    private void upgradeToMigrationVersion2() {
        // --- Organisms and Gene ID resolver ---
        String organismsMessage =  "This version of MotifLab has made updates to:\n\n  * the list of organisms, genome builds and gene ID resolvers\n\n"
                                  + "Unless you have made changes to this configuration yourself, it is recommended to replace the existing configuration with the new version.\n"
                                  + "If you have made your own changes, you can try to merge these with the new updates.";        
        int[] organismsSelection = client.selectOption(organismsMessage, new String[]{"Merge","Replace with new","Keep current"}, false);
        if (organismsSelection==null || organismsSelection.length==0) organismsSelection=new int[]{2};
        switch (organismsSelection[0]) {
            case 0:
            case 1:
                updateOrganismsConfiguration(organismsSelection[0]==0); // Merge if 0, replace if 1
                break;
            default:  // Keep current configuration
                engine.logMessage("Skipping update of the organisms configuration. You can find this resource on the MotifLab website and install it later, if you want to.");
                break;
        }        
        client.progressReportMessage(10); 
        updateGeneIDResolver(organismsSelection[0]==0); // the Gene ID resolver is related to the Organisms config, and the GUI uses the same dialog to edit configurations. So, use the same merge or replace selection here
        client.progressReportMessage(20);   
        
        // --- Gene Ontology database  ---
        replaceConfigFile("GO_table.config", "Gene Ontology database"); // This file cannot be edited by the user, so it is safe to replace it without asking 
        client.progressReportMessage(30);
        
        // --- Startup protocol  ---
        String startupProtocolMessage =  "This version of MotifLab has made changes to:\n\n  * the startup protocol that is executed every time MotifLab is run in GUI mode\n\n"
                                       + "It is recommended that you update your currently installed startup protocol, unless you have made your own changes to it.\n"
                                       + "Would you like to upgrade the protocol?";
        int[] startupSelection = client.selectOption(startupProtocolMessage, new String[]{"Yes","No"}, false);
        
        if (startupSelection!=null && startupSelection.length>0 && startupSelection[0]==0) { // YES
            replaceConfigFile("startup.config", "Startup protocol");
        } else { // NO
            engine.logMessage("Skipping update of the startup protocol for now. You can always find the new protocol on the MotifLab website and install it later, if you want to.");
        }
        client.progressReportMessage(40);

        // --- XML wrappers for bundled external programs --- (These may refer to the old package structure without the "org." prefix)
        ArrayList<String> bundled=locateBundledFiles("/org/motiflab/external","*.xml", new String[]{"MotifVoter.xml"});
        for (String wrapperfile:bundled) {
             replaceExternalProgramWrapper(wrapperfile); 
        }  
        client.progressReportMessage(50);
        
        // --- Datatracks and sources ---
        String datatracksMessage =  "This version of MotifLab has made updates to:\n\n  * the default configuration of data tracks and data sources\n\n"
                                  + "Unless you have made changes to this configuration yourself, it is recommended to replace the existing configuration with the new version.\n"
                                  + "If you have made your own changes, you can try to merge these with the new updates.";        
        int[] datatracksSelection = client.selectOption(datatracksMessage, 
                new String[]{"Add new sources as preferred for existing data tracks",
                             "Add new sources as mirrors for existing data tracks",
                             "Replace sources for same genome build for existing data tracks",
                             "Replace all sources for existing tracks (delete all old sources)",
                             "Replace all configurations for existing tracks",
                             "Replace whole configuration! Do not keep any existing tracks",
                             "Skip update and keep existing configuration"
                            }, 
                false);
        if (datatracksSelection==null || datatracksSelection.length==0 || datatracksSelection[0]==6) engine.logMessage("Skipping update of Data Tracks configuration.");
        else updateDataTracksConfiguration(datatracksSelection[0]); // 
        
        // 
        replaceMotifCollection();    
        updatePlugins();                    
    }

    /**
     * Adds a new entry to the top of the version installation file for the latest migration version.
     * Each line contains 3 tab-separated fields: The migration number, MotifLab's version and a timestamp
     * Existing entries are kept below to keep a history of upgrades.
     * @param file 
     */
    private void updateVersionNumber() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        String newStatus = MIGRATION_NUMBER+"\t"+"MotifLab "+MotifLabEngine.getVersion()+"\t"+formattedDateTime;
        ArrayList<String> allLines=readLines(statusFile);
        allLines.add(0,newStatus);
        writeLines(statusFile, allLines);
    }
    
    /**
     * Reads the contents of a text file and returns a list of lines
     * @param file
     * @return 
     */
    private ArrayList<String> readLines(File file) {
        if (!file.exists()) return new ArrayList<>();
        ArrayList<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            engine.errorMessage("Could not read installed version file",30);
        }
        return lines;
    }
    
    /**
     * Writes lines to a file
     * @param file
     * @param content 
     */
    private void writeLines(File file, ArrayList<String> content) {
        BufferedWriter outputStream=null;
        try {
            outputStream=new BufferedWriter(new FileWriter(file));
            for (String line:content) {
                outputStream.write(line);
                outputStream.newLine();                
            }
            outputStream.close();
        } catch (IOException e) {
            engine.errorMessage("Could not write installed version file",30);
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (IOException ioe) {}
        } 
    }
    
    /**
     * Creates a backup ZIP archive of some configuration files 
     */
    private void backupCurrentConfigurationFiles() {
        String motiflabDir=engine.getMotifLabDirectory();
        File backupDir = new File(motiflabDir,"backup");
        if (!backupDir.isDirectory()) backupDir.mkdirs();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        String timestamp = now.format(formatter);        
        File backupFile = new File(backupDir,"backup_config_files_"+timestamp+".zip");
        ArrayList<File> configfiles = new ArrayList<>();
        configfiles.add(new File(motiflabDir,"Organisms.config"));
        configfiles.add(new File(motiflabDir,"GeneIDResolver.config"));
        configfiles.add(new File(motiflabDir,"TFclass.config"));        
        configfiles.add(new File(motiflabDir,"DataTracks.xml"));
        configfiles.add(new File(motiflabDir,"predefinedMotifCollectionsConfig.txt"));
        try {
            engine.backupConfigurationFiles(backupFile, configfiles);
            engine.logMessage("Created backup of current configuration files in: "+backupFile.getAbsolutePath());
        } catch (IOException error) {
            engine.logMessage("WARNING:  Could not create backup of old configuration files");
        }
           
    }

    
    private void performFirstTimeInstallation() throws SystemError {       
        if (client==null) System.err.println("WARNING: No client installed (yet)");
        if (client!=null) client.progressReportMessage(1);
        // --- install general configuration files ---
        ArrayList<String> configfiles=locateBundledFiles("/org/motiflab/engine/resources","{*.config,*.css, *.js}",null);
        if (!configfiles.isEmpty()) engine.logMessage("Installing configuration files");
        int count=0;
        int size=configfiles.size();
        for (String filename:configfiles) {
            try {          
                InputStream stream=this.getClass().getResourceAsStream("/org/motiflab/engine/resources/"+filename);              
                engine.installConfigFileFromStream(filename,stream);
                if (client!=null) client.progressReportMessage((int)(10+(10f/(float)size)*count));
                count++;
            } catch (SystemError e) {
               engine.logMessage("Unable to install configuration file '"+filename+"' => "+e.getMessage());
            }
        }      
        if (client!=null) client.progressReportMessage(20);   
        
        // --- install external program configuration files ---
        ArrayList<String> bundled=locateBundledFiles("/org/motiflab/external","*.xml", new String[]{"MotifVoter.xml","Priority.xml"});
        if (!bundled.isEmpty()) engine.logMessage("Installing bundled programs");
        count=0;
        size=bundled.size();
        for (String filename:bundled) {
            try { 
                InputStream stream=this.getClass().getResourceAsStream("/org/motiflab/external/"+filename);              
                installBundledProgram(filename,stream);
                if (client!=null) client.progressReportMessage((int)(20+(30f/(float)size)*count));
                count++;
            } catch (SystemError e) {
                engine.logMessage("Unable to install bundled program '"+filename+"' => "+e.getMessage());
            }
        }
        if (client!=null) client.progressReportMessage(50); //  
        
        // --- install predefined Motif Collections ---
        ArrayList<String> bundledMotifs=locateBundledFiles("/org/motiflab/engine/resources","*.mlx",null);
        if (client!=null) client.logMessage("Installing bundled motif collections");
        installBundledMotifCollections(bundledMotifs,50,80);
        // --- install Data Tracks configuration ---
        try {          
            InputStream stream=this.getClass().getResourceAsStream("/org/motiflab/engine/datasource/DataTracks.xml");              
            engine.installConfigFileFromStream("DataTracks.xml",stream);
        } catch (SystemError e) {
           engine.logMessage("Unable to install configuration file 'DataTracks.xml' => "+e.getMessage());
        } 
        if (client!=null) client.progressReportMessage(99); // 
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
        File externalfile = new File(engine.getMotifLabDirectory()+File.separator+"external"+File.separator+filename);
        try {
            File parentDir=externalfile.getParentFile();
            if (parentDir!=null && !parentDir.exists()) parentDir.mkdirs();
            program.saveConfigurationToFile(externalfile);
        } catch (Exception e) {
            // e.printStackTrace(System.err);
            if (e instanceof SystemError) throw (SystemError)e;
            else throw new SystemError(e.getClass().getSimpleName()+":"+e.getMessage());
        }
        program.clearProperties();
        // a call will later be made to engine.importExternalPrograms() to initialize the programs properly
    }  
   
    private void installBundledMotifCollections(final ArrayList<String> bundledMotifs, final int progressStart, final int progressEnd)  {
        final float range=progressEnd-progressStart;
        int count = 0;
        File dir = engine.getPredefinedMotifCollectionDirectory();
        if (!dir.exists()) dir.mkdirs();            
        int size=bundledMotifs.size();
        for (String filename:bundledMotifs) {
            try {
                InputStream stream=this.getClass().getResourceAsStream("/org/motiflab/engine/resources/"+filename);
                if (client!=null) client.progressReportMessage((int)(progressStart+(range/(float)size)*count));
                if (client!=null) client.logMessage("Installing motif collection: "+filename);
                engine.registerPredefinedMotifCollectionFromStream(stream, filename, null);
                count++;
            } catch (Exception e) {
                engine.logMessage("Unable to install bundled motif collection '"+filename+"' => "+e.getClass().getSimpleName()+":"+e.getMessage());
                // e.printStackTrace(System.err);
            }
        }
    }    
   
   
    private ArrayList<String> locateBundledFiles(String resourceDir, String glob, String[] exclude) {
        URL jarLocation = UpgradeManager.class.getProtectionDomain().getCodeSource().getLocation();
        String locationPath = jarLocation.getPath();
        if (!locationPath.endsWith(".jar")) {
            engine.logMessage("WARNING: MotifLab must be run from a JAR file to install necessary resources!");
            return new ArrayList<>();
        }
        ArrayList<String> foundFiles = new ArrayList<>();
        try {
            if (!resourceDir.startsWith("/")) resourceDir="/"+resourceDir;
            URI jarUri = URI.create("jar:" + jarLocation.toURI().toString());
            try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.<String, Object>emptyMap())) {
                Path jarRoot = fs.getPath(resourceDir);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(jarRoot, glob)) {
                    for (Path path : stream) {
                        if (exclude!=null && contains(exclude,path.getFileName().toString())) continue;
                        foundFiles.add(path.getFileName().toString());
                    }
                }
            }
        } catch (Exception e) {
            engine.logMessage("WARNING: Unable to search through bundled resource files");
        }
        return foundFiles;
    }
    
    private boolean contains(String[] array, String value) {
        for (String s : array) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }   
    
  // -----------------------------------------------------------------------------------------------------------------------------------------------
    
    private void replaceConfigFile(String filename, String description) {
        engine.logMessage("Updating "+description);
        try {          
            InputStream stream=this.getClass().getResourceAsStream("/org/motiflab/engine/resources/"+filename);              
            engine.installConfigFileFromStream(filename,stream);
        } catch (Exception e) {
           engine.logMessage("Unable to update "+description+" => "+e.getMessage());
        }        
    }
    
    private void replaceExternalProgramWrapper(String filename) {
        File externalfile = new File(engine.getMotifLabDirectory()+File.separator+"external"+File.separator+filename);
        if (!externalfile.exists()) {
            engine.logMessage("Skipping non-existing external program file: "+filename);
            return;
        }
        engine.logMessage("Updating configuration for external program: "+filename.substring(0,filename.indexOf(".xml")));        
        try { 
            InputStream stream=this.getClass().getResourceAsStream("/org/motiflab/external/"+filename);              
            installBundledProgram(filename,stream);
        } catch (SystemError e) {
            engine.logMessage("Unable to update configuration for external program '"+filename+"' => "+e.getMessage());
        }        
    }    
    
    /**
     * Update the current Data tracks and sources with new ones bundled with MotifLab
     * @param mode controls how the update should be performed (according to constants defined in this class)
     */
    private void updateDataTracksConfiguration(int mode) {
        // first make a copy (clone) of the current data configuration. This should have been loaded during engine.initialize();    
        DataConfiguration dataconfiguration=(DataConfiguration)engine.getDataLoader().getCurrentDataConfiguration().clone();       
        HashMap<String,DataTrack> availableTracks=dataconfiguration.getAvailableTracks(); // direct reference to the HashMap in the DataConfiguration 
        HashMap<String,Server> availableServers=dataconfiguration.getServers();  // direct reference to the HashMap in the DataConfiguration
        System.err.println("Existing configuration contains "+availableTracks.size()+" Data tracks and "+availableServers.size()+" servers");
        DataConfiguration bundledConfig=new DataConfiguration();
        try {
            InputStream inputstream=UpgradeManager.class.getResourceAsStream("/org/motiflab/engine/datasource/DataTracks.xml");
            bundledConfig.loadConfigurationFromStream(inputstream);
        } catch (SystemError e) {
            engine.logMessage("ERROR: Unable to read Data Tracks configuration that comes bundled with MotifLab. Could not update the installed configuration!");
            return;
        }
        HashMap<String,DataTrack> newDatatracks=bundledConfig.getAvailableTracks();
        HashMap<String,Server> newServers=bundledConfig.getServers();
        System.err.println("Read "+newDatatracks.size()+" Data tracks and "+newServers.size()+" server configs from bundled resource file");
  
        if (mode==REPLACE_WHOLE_DATATRACK_CONFIG) {
            availableTracks.clear();
            availableServers.clear();
        }
        for (String trackname:newDatatracks.keySet()) {
            DataTrack newtrack=newDatatracks.get(trackname);
            if (!availableTracks.containsKey(trackname)) {
                availableTracks.put(trackname,newtrack); // no such track from before. Just add it
            } else { // a track with the same name already exists. Now we must either replace the track or merge the sources (after first checking that it is actually compatible)
                DataTrack oldTrack=availableTracks.get(trackname);
                if (mode==REPLACE_EXISTING_TRACKS) {
                    availableTracks.put(trackname, newtrack); // this will replace the whole track with the new
                    continue;
                }
                if (!oldTrack.getDataType().equals(newtrack.getDataType())) { // type conflict with existing track
                    engine.logMessage("ERROR: Unable to merge sources from new track '"+trackname+"' with existing track. The two tracks are incompatible");
                    continue;
                }
                ArrayList<DataSource> newsources=newtrack.getDatasources();
                switch (mode) {
                    case DUPLICATE_SOURCES_ADD_AS_PREFERRED:
                        oldTrack.addPreferredDataSources(newsources);                                               
                        break;
                    case DUPLICATE_SOURCES_ADD_AS_MIRRORS:
                        oldTrack.addDataSources(newsources);                      
                        break;
                    case DUPLICATE_SOURCES_REMOVE_OLD:
                        oldTrack.replaceDataSources(newsources);                    
                        break;
                    case DUPLICATE_SOURCES_REPLACE_ALL_SOURCES:
                        oldTrack.replaceAllDataSources(newsources);
                        break;
                    default:
                        break;
                }
                // copy over other properties if they are set in the new track but not the old
                if (!oldTrack.hasDisplayDirectivesProtocol() && newtrack.hasDisplayDirectivesProtocol()) { // copy over display directives from new track
                    oldTrack.setDisplayDirectivesProtocol(newtrack.getDisplayDirectivesProtocol());
                }
                String oldDescription=oldTrack.getDescription();
                String newDescription=newtrack.getDescription();
                if ((oldDescription==null || oldDescription.isEmpty()) && (newDescription!=null && !newDescription.isEmpty())) {
                    oldTrack.setDescription(newDescription);
                }
                String oldSourceSite=oldTrack.getSourceSite();
                String newSourceSite=newtrack.getSourceSite();
                if ((oldSourceSite==null || oldSourceSite.isEmpty()) && (newSourceSite!=null && !newSourceSite.isEmpty())) {
                    oldTrack.setSourceSite(newSourceSite);
                }                 
            } // end: track exists
        } // end: for each track
        
        // now do the servers. These will just replace current settings!
        for (String servername:newServers.keySet()) {
            Server server=newServers.get(servername);
            availableServers.put(servername, server);
        }        
               
        // save configuration to file
        try {
            File configurationfile = engine.getDataLoader().getDataConfigurationFile();
            dataconfiguration.saveConfigurationToFile(configurationfile);
        } catch (Exception e) {
            engine.logMessage("ERROR: Unable to save Data Tracks configuration: ");
        }        
        engine.getDataLoader().installConfiguration(dataconfiguration); // replace currently used configuration
    }  
    
    
    /**
     * Updates the Organisms configuration on disk
     * The engine will import these new configuration later
     * @param merge 
     */
    private void updateOrganismsConfiguration(boolean merge) {       
        if (merge) {    
            File organismsFile = new File(engine.getMotifLabDirectory(),"Organisms.config");
            ArrayList<Object[]> oldEntries=null;
            try {
               oldEntries=Organism.readOrganismsFromFile(organismsFile);
               ArrayList<Object[]> newEntries=Organism.readOrganismsFromResource("Organisms.config");
               for (int i=0;i<newEntries.size();i++) {
                   Object[] newOrganism=newEntries.get(i);
                   int index=findOrganism((Integer)newOrganism[0], oldEntries);
                   if (index<0) oldEntries.add(newOrganism);
                   else { // merge genome builds for the organism
                       Object[] oldOrganism=oldEntries.get(index);
                       String[][] mergedBuilds = mergeGenomeBuilds(Organism.getBuildsForEntry(oldOrganism),Organism.getBuildsForEntry(newOrganism));
                       Organism.setBuildsForEntry(oldOrganism, mergedBuilds);
                       oldEntries.set(index, oldOrganism);                       
                   }
               }
            } catch (SystemError e) {
                engine.logMessage("Error: Unable to read Organism definitions from file because the file is badly formatted:\n"+e.getMessage(),0);
            } catch (IOException e) {
                engine.logMessage("Error: Unable to read Organism definitions from file because of IO-error: "+e.getMessage(),0);
            } 
            try {
                Organism.writeToFile(oldEntries, organismsFile);
            } catch (IOException e) {
                engine.logMessage("Error: Unable to write Organism configuration to file: "+e.getMessage(),0);
            }
        } else replaceConfigFile("Organisms.config","organisms list");        
    } 
    
    /**
     * Returns the row-index of the given organism if it is found in the table
     * or -1 if it is not found
     * @param organismID the organism to search for in the table (first column)
     * @param organisms A table of organisms
     * @return 
     */
    private int findOrganism(int organismID, ArrayList<Object[]> organisms) {
        for (int i=0;i<organisms.size();i++) {
            Object[] organism=organisms.get(i);
            if ((Integer)(organism[0])==organismID) return i;
        }
        return -1;
    }
    
    /**
     * Returns the row-index of the given genome build if it is found in the table
     * or -1 if it is not found
     * @param build the genome build to search for in the table (only search first element of each nested list)
     * @param builds A table of organisms builds
     * @return 
     */
    private int findBuild(String build, ArrayList<ArrayList<String>> builds) {
        for (int i=0;i<builds.size();i++) {
            ArrayList<String> buildsEntry=builds.get(i);
            if (build.equals(buildsEntry.get(0))) return i;
        }
        return -1;
    }    
       
    /** Merge lists of genome builds for a single organism 
     */
    private String[][] mergeGenomeBuilds(String[][] oldBuilds, String[][] newBuilds) {
        if (newBuilds==null) return oldBuilds;
        if (oldBuilds==null) return newBuilds;
        ArrayList<ArrayList<String>> oldBuildsArray = convertTableArrayToList(oldBuilds);
        ArrayList<ArrayList<String>> newBuildsArray = convertTableArrayToList(newBuilds);
        for (ArrayList<String> newBuild:newBuildsArray) {
            int index = findBuild(newBuild.get(0),oldBuildsArray);
            if (index<0) { // new entry. Add this to the old builds
               oldBuildsArray.add(newBuild);
            } else { // entry already exists. Merge the build lists
                ArrayList<String> oldBuild = oldBuildsArray.get(index);
                for (String build:newBuild) {
                    if (oldBuild.indexOf(build)<0) oldBuild.add(build); // add new builds to old array
                }
                oldBuildsArray.set(index,oldBuild);
            }
        }
        Collections.sort(oldBuildsArray, new GenomeBuildSortOrderComparator()); // sort most recent genome build first (to use as default)
        return convertTableListToArray(oldBuildsArray);        
    }  
       
    private class GenomeBuildSortOrderComparator implements Comparator<ArrayList<String>> {
        public GenomeBuildSortOrderComparator() {}
        @Override
        public int compare(ArrayList<String> builds1, ArrayList<String> builds2) { //
            String string1 = builds1.get(0);
            String string2 = builds2.get(0);
            int cmp=MotifLabEngine.compareNaturalOrder(string1,string2);
            return -cmp; // reverse to sort in descending order            
        }                
    }      
    
    private ArrayList<ArrayList<String>> convertTableArrayToList(String[][] list) {       
        ArrayList<ArrayList<String>> newList = new ArrayList(list.length);
        for (String[] element:list) {
           newList.add(new ArrayList<>(Arrays.asList(element)));
        }
        return newList;
    }
    
    private String[][] convertTableListToArray(ArrayList<ArrayList<String>> list) {        
        String[][] newList =  list.stream()
            .map(row -> row.toArray(new String[0])) // Convert each inner List<String> to String[]
            .toArray(String[][]::new); // Convert the Stream of String[] to String[][]
        return newList;
    }    
    
    /**
     * Makes updates to the GeneIDResolver configuration
     * @param merge 
     */
    private void updateGeneIDResolver(boolean merge) {       
        if (merge) {    
            String configFileName="GeneIDResolver.config";
            File geneIDResolverFile = new File(engine.getMotifLabDirectory(),configFileName);
            GeneIDResolver currentResolver = new GeneIDResolver(engine);
            try {
                currentResolver.initializeFromFile(geneIDResolverFile, true); // load current configuration
                InputStream stream=this.getClass().getResourceAsStream("/org/motiflab/engine/resources/"+configFileName);
                currentResolver.initializeFromStream(stream, false); // load new configuration into the same object. keep existing settings unless they are replaced by the new config 
            } catch (SystemError e) {
                engine.logMessage("Error: Unable to read Gene ID resolver configuration from file because the file is badly formatted:\n"+e.getMessage(),0);
            } catch (IOException e) {
                engine.logMessage("Error: Unable to read Gene ID resolver configuration from file because of IO-error: "+e.getMessage(),0);
            }            
            try {
                currentResolver.writeToFile(geneIDResolverFile);
            } catch (IOException e) {
                engine.logMessage("Error: Unable to write Gene ID resolver configuration to file: "+e.getMessage(),0);
            } 
        } else replaceConfigFile("GeneIDResolver.config","Gene ID resolver");
    }
    
    private void replaceMotifCollection() {
        
    }  
    
    private void updatePlugins() {
        
    }     
    
}
