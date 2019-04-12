/*
 * This is a top-level class for data repositories
 * that can be used for import/export of files representing
 * data objects (in some dataformat), sessions, protocols or whatever.
 */
package motiflab.engine.datasource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import javax.swing.Icon;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.SystemError;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.StandardParametersParser;


/**
 *
 * @author kjetikl
 */
public abstract class DataRepository implements Serializable {
    
    public MotifLabEngine getEngine() {
        return MotifLabEngine.getEngine();
    }
    
    /**
     * Returns the name of this repository instance (it should not contain spaces or other special characters)
     * @return 
     */
    public abstract String getRepositoryName();
    
    /**
     * Sets the name of this repository instance (it should not contain spaces or other special characters)
     * @param name 
     */
    public abstract void setRepositoryName(String name);    
    
    /**
     * Returns the type of this repository. Each extending subclass should return a different type name.
     * @return 
     */
    public abstract String getRepositoryType();
    
    /**
     * Returns a general description of this type of data repository
     * @return 
     */
    public abstract String getRepositoryTypeDescription();    
      
    /**
     * Returns a string describing the configuration of this repository.
     * Subclasses can override this function to return type-specific configurations.
     * (e.g. for a "Local Directory" repository this String could be the directory file path)
     * The superclass implementation will return a comma-separated list of key=value parameter pairs
     * based on the parameters returned by @getConfigurationParameters
     * @return 
     */
    public String getConfigurationString() {
        Parameter[] paramlist=getConfigurationParameters();
        ParameterSettings settings=getConfigurationParameterSettings();
        if (paramlist==null || paramlist.length==0 || settings==null) return "";
        StandardParametersParser parser=new StandardParametersParser(getEngine());
        return parser.getCommandString(paramlist, settings);
    }
    
    /**
     * Updates the configuration of the data repository based on the provided configuration string.
     * The superclass implementation expects a list of parameters in standard format (as key=value pairs)
     * and will parse the parameters and call the @setConfigurationParameters method to update the parameter values.
     * Subclasses are free to override this method and parse the configuration string in a different way
     * @throws SystemError The repository can throw a System Error if the provided configuration string is not accepted
     * @return 
     */
    public void setConfigurationString(String config) throws SystemError {
        Parameter[] paramlist=getConfigurationParameters();
        if ((paramlist==null || paramlist.length==0) && !config.isEmpty()) throw new SystemError("This data repository does not have any configurable settings");
        StandardParametersParser parser=new StandardParametersParser(getEngine());
        try {
            ParameterSettings settings=parser.parse(config, paramlist);
            setConfigurationParameters(settings);
        } catch (ParseError e) {
            throw new SystemError(e.getMessage());
        }
    }
    
    /**
     * Returns a list of parameters that can be used to configure this repository (this list can be NULL if no configuration is necessary)
     * @return 
     */
    public abstract Parameter[] getConfigurationParameters();
    
    /**
     * Returns a ParametersSettings object reflecting the current configuration of this data repository
     * @return 
     */  
    public abstract ParameterSettings getConfigurationParameterSettings();    
      
    /**
     * Updates the configuration of this data repository based on the provided parameter settings
     * @throws SystemError The repository can throw a System Error if the provided configuration string is not accepted
     * @return 
     */      
    public abstract void setConfigurationParameters(ParameterSettings settings) throws SystemError;
  
    
    
    
    /**
     * Takes the given file from the local file system and stores a copy
     * of it in the repository 
     * @param file The original file to be pushed into the repository
     * @param filepath The path of the file in the repository
     * @throws IOException 
     */
    public abstract void saveFileToRepository(File file, String filepath) throws IOException;
    
    public abstract InputStream getFileAsInputStream(String filepath)  throws IOException;
    
    public abstract OutputStream getFileAsOutputStream(String filepath)  throws IOException;
    
    public abstract char getSeparator();
    public abstract char getPathSeparator();
    
    public abstract String resolve(String parent, String child);
    public abstract String resolve(DataRepositoryFile file);
    public abstract String normalize(String path);
    public abstract String canonicalize(String path);    
     
    public int prefixLength(String path) {
        if (path.startsWith(getRepositoryName()+":")) return getRepositoryName().length()+1;
        else return 0;
    }
    
    public abstract boolean exists(DataRepositoryFile file) throws IOException;
    public abstract boolean canRead(DataRepositoryFile file);
    public abstract boolean canWrite(DataRepositoryFile file);
    public abstract boolean canExecute(DataRepositoryFile file);
    public abstract boolean isFile(DataRepositoryFile file) throws IOException;
    public abstract boolean isHidden(DataRepositoryFile file) throws IOException;
    public abstract boolean isDirectory(DataRepositoryFile file) throws IOException;
    public abstract boolean delete(DataRepositoryFile file) throws IOException;
    public abstract boolean isAbsolute(DataRepositoryFile file);
    
    public abstract long getLastModifiedTime(DataRepositoryFile file) throws IOException;    
    public abstract boolean setLastModifiedTime(DataRepositoryFile file, long time) throws IOException;    
    public abstract long getLength(DataRepositoryFile file) throws IOException;    
    public abstract boolean createNewFile(String path) throws IOException;
    public abstract boolean createDirectory(DataRepositoryFile file) throws IOException;
    public abstract boolean rename(DataRepositoryFile oldfile,DataRepositoryFile newfile) throws IOException;
    
    public abstract String[] list(DataRepositoryFile file) throws IOException;
       
    public abstract long getTotalSpace();
    public abstract long getFreeSpace();
    public abstract long getUsableSpace();
    
    /**
     * Returns the root directory of this repository
     * @return 
     */
    public abstract DataRepositoryFile getRoot();
    
    /** Returns an Icon to use for this data repository in file dialogs etc.
     *  The default implementation returns NULL which is a signal to use a default icon
     *  Override this method to return a different icon
     */
    public Icon getRepositoryIcon() {
        return null;
    }
    
    /** This method should return TRUE if the repository is protected and requires a user to authenticate interactively before accessing it
     *  DataRepositories that are protected can not be used as "default" startup folders by the client
     */
    public abstract boolean isProtected();
}
