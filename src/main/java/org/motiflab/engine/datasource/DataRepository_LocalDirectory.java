package org.motiflab.engine.datasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author kjetikl
 */
public class DataRepository_LocalDirectory extends DataRepository {
    private File root=null;
    private String repositoryName="error";

    
    public DataRepository_LocalDirectory(String name, String rootpath) {
        repositoryName=name;
        root=new File(rootpath);
    }    
    
    public DataRepository_LocalDirectory() {
        repositoryName="Repository";
        root=new File("");
    }     
    
    
    @Override
    public InputStream getFileAsInputStream(String filepath) throws IOException {   
        File file=getFileInRepository(filepath);      
        return new FileInputStream(file);
    }

    @Override
    public OutputStream getFileAsOutputStream(String filepath) throws IOException {
        File file=getFileInRepository(filepath);
        return new FileOutputStream(file);
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }
    
    @Override
    public void setRepositoryName(String name) {
        repositoryName=name;
    }    
    
    @Override
    public String getRepositoryType() {
        return "Local Directory";
    }    
    
    @Override
    public String getRepositoryTypeDescription() {
        return "Local Directory repositories stores the files in a regular directory in the local filesystem.";        
    }    
    
    @Override
    public String getConfigurationString() {
        return root.getAbsolutePath();
    }
    
    @Override
    public void setConfigurationString(String config) throws SystemError {
        // check if the config string is in key=value format
        if (config.startsWith("Path=")) {
            super.setConfigurationString(config); // this will call setConfigurationParameters()
            return;
        }
        // assuming that the config string is just a path
        File file=new File(config);
        if (!file.exists()) throw new SystemError("Directory does not exist: "+config);
        if (!file.isDirectory()) throw new SystemError("\""+config+"\" is not a directory");        
        root=file;
    }     
    
    @Override
    public Parameter[] getConfigurationParameters() {
        Parameter path=new Parameter("Path", String.class, null, null, "The local path of the repository directory", true, false);
        Parameter[] parameters = new Parameter[] { path };
        return parameters;        
    }
    
    @Override
    public ParameterSettings getConfigurationParameterSettings() {
        ParameterSettings settings=new ParameterSettings();
        settings.setParameter("Path", (root!=null)?root.getAbsolutePath():"");
        return settings;
    }   
      
    @Override  
    public void setConfigurationParameters(ParameterSettings settings) throws SystemError {
        String path=settings.getParameterAsString("Path", getConfigurationParameters());
        if (path!=null) {
            File file=new File(path);
            if (!file.exists()) throw new SystemError("Directory does not exist: "+path);
            if (!file.isDirectory()) throw new SystemError("\""+path+"\" is not a directory");        
            root=file;            
        } else throw new SystemError("Missing specification of 'Path' parameter");
    }    
    
    

    @Override
    public void saveFileToRepository(File file, String filepath) throws IOException {
        File newfile=getFileInRepository(filepath);
        FileUtils.copyFile(file, newfile);
    }
    
    private File getFileInRepository(String filepath) throws IOException {
        if (filepath.startsWith(repositoryName+":")) filepath=filepath.substring((repositoryName+":").length());
        return new File(root,filepath);
    }
    
    private File getFileInRepository(DataRepositoryFile file) throws IOException {
        String filepath=file.getPath();
        if (filepath.startsWith(repositoryName+":")) filepath=filepath.substring((repositoryName+":").length());
        else if (filepath.startsWith(repositoryName)) filepath=filepath.substring((repositoryName).length());
        return new File(root,filepath);
    }    

    @Override
    public boolean exists(DataRepositoryFile file) throws IOException {
        if (file.isRoot()) return true;
        File repFile=getFileInRepository(file);
        return repFile.exists();
    }    
    
    @Override
    public boolean canExecute(DataRepositoryFile file) {
        return true;
    }

    @Override
    public boolean canRead(DataRepositoryFile file) {
        return true;
    }

    @Override
    public boolean canWrite(DataRepositoryFile file) {
        return true;
    }

    @Override
    public String canonicalize(String path) {
        return path; // not sure if I should do something intelligent here...
    }

    @Override
    public boolean createDirectory(DataRepositoryFile file) throws IOException {
        File newfile=getFileInRepository(file);
        return newfile.mkdir();
    }

    @Override
    public boolean createNewFile(String path) throws IOException {
       return false;
    }

    @Override
    public boolean delete(DataRepositoryFile file)  throws IOException {       
        File newfile=getFileInRepository(file);
        return newfile.delete();
    }

    @Override
    public long getFreeSpace() {
        return root.getFreeSpace();
    }

    @Override
    public long getLastModifiedTime(DataRepositoryFile file) throws IOException {
        File repfile=getFileInRepository(file);
        return repfile.lastModified();
    }

    @Override
    public long getLength(DataRepositoryFile file) throws IOException {
        File repfile=getFileInRepository(file);
        return repfile.length();
    }

    @Override
    public char getPathSeparator() {
        return ';';
    }

    @Override
    public DataRepositoryFile getRoot() {
       return new DataRepositoryFile(repositoryName+":", this);
       //return new DataRepositoryFile(root.getPath(), this);
    }

    @Override
    public char getSeparator() {
        return '/';
    }

    @Override
    public long getTotalSpace() {
        return 0L;
    }

    @Override
    public long getUsableSpace() {
        return 0L;
    }

    @Override
    public boolean isAbsolute(DataRepositoryFile file) {
        return true; // all should be absolute?
        //File newfile=getFileInRepository(file);
        //return newfile.isAbsolute();
    }

    @Override
    public boolean isDirectory(DataRepositoryFile file) throws IOException {
        File newfile=getFileInRepository(file);
        return newfile.isDirectory();
    }

    @Override
    public boolean isFile(DataRepositoryFile file) throws IOException {
        File newfile=getFileInRepository(file);
        return newfile.isFile();
    }

    @Override
    public boolean isHidden(DataRepositoryFile file) throws IOException {
        File newfile=getFileInRepository(file);
        return newfile.isHidden();
    }

    @Override
    public String[] list(DataRepositoryFile file) throws IOException {
        File newfile=getFileInRepository(file);
        return newfile.list();
    }

    @Override
    public String normalize(String path) {
        return path;
    }

    @Override
    public boolean rename(DataRepositoryFile oldfile, DataRepositoryFile newfile) throws IOException {
        File oldFILE=getFileInRepository(oldfile);
        File newFILE=getFileInRepository(newfile);
        return oldFILE.renameTo(newFILE);
    }

    @Override
    public String resolve(String parent, String child) {
        //System.err.println("Resolve:["+parent+"]+["+child+"]");
        if (parent==null || parent.isEmpty()) return repositoryName+":"+child;
        else if (parent.equals(repositoryName)) return repositoryName+":"+child;
        else if (parent.equals(repositoryName+":")) return repositoryName+":"+child;
        else return parent+getSeparator()+child; // the parent should start with "repositoryname:"      
    }

    @Override
    public String resolve(DataRepositoryFile file) {        
        return file.getPath(); // not sure if this will do the trick (but it seems to work)
    }

    @Override
    public boolean setLastModifiedTime(DataRepositoryFile file, long time) {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }
    
}
