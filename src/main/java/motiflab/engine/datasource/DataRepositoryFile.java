package motiflab.engine.datasource;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import motiflab.engine.MotifLabEngine;
import motiflab.gui.SimpleDataPanelIcon;

/**
 *
 * @author kjetikl
 */
public class DataRepositoryFile extends File {
    private DataRepository repository=null;
    public static final ImageIcon repIcon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/database_16.png")));

    public String getRepositoryName() {
        return (repository!=null)?repository.getRepositoryName():"";
    }

    public void setRepository(DataRepository repository) {
        this.repository = repository;
    }
    
    public DataRepository getRepository() {
        return repository;
    }

    /** Returns an InputStream that can be used to read data from this repository file */
    public InputStream getFileAsInputStream() throws IOException {
        if (repository==null) throw new IOException("Missing data repository for: "+path);
        else return repository.getFileAsInputStream(path);        
     }
    
    /** Returns an OutputStream that can be used to write data to this repository file */
    public OutputStream getFileAsOutputStream() throws IOException {
        if (repository==null) throw new IOException("Missing data repository: "+path);
        else return repository.getFileAsOutputStream(path);  
    }    
    
    
    /**
     * Returns a DataRepositoryFile object for the given filename, or NULL if the
     * given filename was not a valid repository file
     * @param filename
     * @param engine
     * @return 
     */
    public static DataRepositoryFile getRepositoryFileFromString(String filepath, MotifLabEngine engine) {
        if (filepath==null) return null;
        if (filepath.contains(":")) {
           String[] parts=filepath.split(":",2);
           DataRepository repository=engine.getDataRepository(parts[0]);
           if (repository==null) return null; // Not a known MotifLab repository. It could just be a hard drive in Windows (e.g.: "C:")
           if (parts.length>=2) {
               if (filepath.startsWith(File.separator)) filepath=filepath.substring(1); // return "Rep:file" instead of Rep:\file" 
               return new DataRepositoryFile(filepath, repository);
           }          
        } 
        return null;
    }    
    
    public Icon getIcon() {
        if (this.isRoot()) {
            Icon icon=repository.getRepositoryIcon();
            return (icon!=null)?icon:repIcon;
        }
        else return null; // a signal to use the default icons if the underlying OS
    }
    
    public boolean isRoot() {
        return this.equals(repository.getRoot());
    }
    
    public String getTypeDescription() {
        if (this.isRoot()) return "Data repository";
        else return null; // use default
    }
    
    public String getSystemDisplayName() {
        if (this.isRoot()) return repository.getRepositoryName();//+":";
        else return null; // use default
    }    
  
    /**
     * This abstract pathname's normalized pathname string.  A normalized
     * pathname string uses the default name-separator_rep character and does not
     * contain any duplicate or redundant separators.
     *
     * @serial
     */
    private String path;

    /**
     * The length of this abstract pathname's prefix, or zero if it has no
     * prefix.
     */
    private transient int prefixLength;

    /**
     * Returns the length of this abstract pathname's prefix.
     * For use by DataRepositoryFileSystem classes.
     */
    int getPrefixLength() {
        return prefixLength;
    }

    /**
     */
    public char separatorChar_rep = (repository!=null)?repository.getSeparator():'/';

    /**
     * The system-dependent default name-separator_rep character, represented as a
     * string for convenience.  This string contains a single character, namely
     * <code>{@link #separatorChar_rep}</code>.
     */
    public String separator_rep = "" + separatorChar_rep;

    /**
     * The system-dependent path-separator_rep character.  This field is
     * initialized to contain the first character of the value of the system
     * property <code>path.separator_rep</code>.  This character is used to
     * separate filenames in a sequence of files given as a <em>path list</em>.
     * On UNIX systems, this character is <code>':'</code>; on Microsoft Windows systems it
     * is <code>';'</code>.
     *
     * @see     java.lang.System#getProperty(java.lang.String)
     */
    public char pathSeparatorChar_rep = (repository!=null)?repository.getPathSeparator():';';

    /**
     * The system-dependent path-separator_rep character, represented as a string
     * for convenience.  This string contains a single character, namely
     * <code>{@link #pathSeparatorChar_rep}</code>.
     */
    public String pathSeparator_rep = "" + pathSeparatorChar_rep;

    private DataRepositoryFile(String pathname, int prefixLength, DataRepository repository) {
        super(pathname); // just to instantiate the superclass in some way
        updateSeparators();
        this.repository=repository;
        this.path = pathname;
        this.prefixLength = prefixLength;
    }   
    
    private void updateSeparators() {
       separatorChar_rep = (repository!=null)?repository.getSeparator():'/'; 
       separator_rep = "" + separatorChar_rep;
       pathSeparatorChar_rep = (repository!=null)?repository.getPathSeparator():';';
       pathSeparator_rep = "" + pathSeparatorChar_rep;
    }
    
    /**
     * Internal constructor for already-normalized pathname strings.
     * The parameter order is used to disambiguate this method from the
     * public(DataRepositoryFile, String) constructor.
     */
    private DataRepositoryFile(String child, DataRepositoryFile parent) {
        super(child);
        this.repository=parent.repository;
        updateSeparators();
        this.path = repository.resolve(parent.path, child);
        this.prefixLength = parent.prefixLength;       
    }
    
    

    /**
     * Creates a new <code>DataRepositoryFile</code> instance by converting the given
     * pathname string into an abstract pathname.  If the given string is
     * the empty string, then the result is the empty abstract pathname.
     *
     * @param   pathname  A pathname string
     * @throws  NullPointerException
     *          If the <code>pathname</code> argument is <code>null</code>
     */
    public DataRepositoryFile(String pathname, DataRepository repository) {        
        super(pathname);
        this.repository=repository;
        updateSeparators();
        if (pathname == null) {
            throw new NullPointerException();
        }
        this.path = repository.normalize(pathname);
        this.prefixLength = repository.prefixLength(this.path);
        
    }

    /* Note: The two-argument DataRepositoryFile constructors do not interpret an empty
       parent abstract pathname as the current user directory.  An empty parent
       instead causes the child to be resolved against the system-dependent
       directory defined by the DataRepositoryFileSystem.getDefaultParent method.  On Unix
       this default is "/", while on Microsoft Windows it is "\\".  This is required for
       compatibility with the original behavior of this class. */

    /**
     * Creates a new <code>DataRepositoryFile</code> instance from a parent pathname string
     * and a child pathname string.
     *
     * <p> If <code>parent</code> is <code>null</code> then the new
     * <code>DataRepositoryFile</code> instance is created as if by invoking the
     * single-argument <code>DataRepositoryFile</code> constructor on the given
     * <code>child</code> pathname string.
     *
     * <p> Otherwise the <code>parent</code> pathname string is taken to denote
     * a directory, and the <code>child</code> pathname string is taken to
     * denote either a directory or a file.  If the <code>child</code> pathname
     * string is absolute then it is converted into a relative pathname in a
     * system-dependent way.  If <code>parent</code> is the empty string then
     * the new <code>DataRepositoryFile</code> instance is created by converting
     * <code>child</code> into an abstract pathname and resolving the result
     * against a system-dependent default directory.  Otherwise each pathname
     * string is converted into an abstract pathname and the child abstract
     * pathname is resolved against the parent.
     *
     * @param   parent  The parent pathname string
     * @param   child   The child pathname string
     * @throws  NullPointerException
     *          If <code>child</code> is <code>null</code>
     */
    public DataRepositoryFile(String parent, String child, DataRepository repository) {
        super(parent);
        this.repository=repository;
        updateSeparators();
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent != null) {
            if (parent.equals("")) {
                this.path = repository.resolve(repository.getRoot().getPath(),
                                       repository.normalize(child));
            } else {
                this.path = repository.resolve(repository.normalize(parent),
                                       repository.normalize(child));
            }
        } else {
            this.path = repository.normalize(child);
        }
        this.prefixLength = repository.prefixLength(this.path);
    }

    /**
     * Creates a new <code>DataRepositoryFile</code> instance from a parent abstract
     * pathname and a child pathname string.
     *
     * <p> If <code>parent</code> is <code>null</code> then the new
     * <code>DataRepositoryFile</code> instance is created as if by invoking the
     * single-argument <code>DataRepositoryFile</code> constructor on the given
     * <code>child</code> pathname string.
     *
     * <p> Otherwise the <code>parent</code> abstract pathname is taken to
     * denote a directory, and the <code>child</code> pathname string is taken
     * to denote either a directory or a file.  If the <code>child</code>
     * pathname string is absolute then it is converted into a relative
     * pathname in a system-dependent way.  If <code>parent</code> is the empty
     * abstract pathname then the new <code>DataRepositoryFile</code> instance is created by
     * converting <code>child</code> into an abstract pathname and resolving
     * the result against a system-dependent default directory.  Otherwise each
     * pathname string is converted into an abstract pathname and the child
     * abstract pathname is resolved against the parent.
     *
     * @param   parent  The parent abstract pathname
     * @param   child   The child pathname string
     * @throws  NullPointerException
     *          If <code>child</code> is <code>null</code>
     */
    public DataRepositoryFile(DataRepositoryFile parent, String child) {
        super(child);
        this.repository=parent.repository;
        updateSeparators();
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent != null) {
            if (parent.path.equals("")) {
                this.path = repository.resolve(repository.getRoot().getPath(),
                                       repository.normalize(child));
            } else {
                this.path = repository.resolve(parent.path,
                                       repository.normalize(child));
            }
        } else {
            this.path = repository.normalize(child);
        }
        this.prefixLength = repository.prefixLength(this.path);
    }


    @Override
    public String getName() {
        int index = path.lastIndexOf(separatorChar_rep);
        if (index < prefixLength) return path.substring(prefixLength);
        return path.substring(index + 1);
    }

    @Override
    public String getParent() {
        int index = path.lastIndexOf(separatorChar_rep);
        if (index < prefixLength) {
            if ((prefixLength > 0) && (path.length() > prefixLength))
                return path.substring(0, prefixLength);
            return null;
        }
        return path.substring(0, index);
    }

    @Override
    public DataRepositoryFile getParentFile() {
        String p = this.getParent();
        if (p == null) return null;
        return new DataRepositoryFile(p, this.prefixLength, repository);
    }

    @Override
    public String getPath() {
        return path;
    }


    @Override
    public boolean isAbsolute() {
        return repository.isAbsolute(this);
    }

    @Override
    public String getAbsolutePath() {
        return repository.resolve(this);
    }

    @Override
    public DataRepositoryFile getAbsoluteFile() {
        String absPath = getAbsolutePath();
        return new DataRepositoryFile(absPath, repository.prefixLength(absPath), repository);
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return repository.canonicalize(repository.resolve(this));
    }

    @Override
    public DataRepositoryFile getCanonicalFile() throws IOException {
        String canonPath = getCanonicalPath();
        return new DataRepositoryFile(canonPath, repository.prefixLength(canonPath), repository);
    }

    private String slashify(String path, boolean isDirectory) {
        String p = path;
        if (separatorChar_rep != '/')
            p = p.replace(separatorChar_rep, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        if (!p.endsWith("/") && isDirectory)
            p = p + "/";
        return p;
    }

    @Override
    @Deprecated
    public URL toURL() throws MalformedURLException {
        return new URL((repository!=null)?repository.getRepositoryName():"null", "", slashify(getAbsolutePath(), isDirectory()));
    }

    @Override
    public URI toURI() {
        try {
            DataRepositoryFile f = getAbsoluteFile();
            String sp = slashify(f.getPath(), f.isDirectory());
            if (sp.startsWith("//"))
                sp = "//" + sp;
            return new URI((repository!=null)?repository.getRepositoryName():"null", null, sp, null);
        } catch (URISyntaxException x) {
            throw new Error(x);         // Can't happen
        }
    }

    @Override
    public boolean exists() {
        //System.err.println("DRF:exists:"+repository+"  path="+path+"  exists="+repository.exists(this));
        try {return repository.exists(this);} catch (IOException e) {}
        return false;
    }    

    @Override
    public boolean canRead() {
        return repository.canRead(this);
    }

    @Override
    public boolean canWrite() {
        return repository.canWrite(this);
    }

    @Override
    public boolean isDirectory() {
        try {return repository.isDirectory(this);} catch (IOException e) {}
        return false;
    }      
    
    @Override
    public boolean isFile() {
        try {return repository.isFile(this);} catch (IOException e) {}
        return false;
    }

    @Override
    public boolean isHidden() {
        try {return repository.isHidden(this);} catch (IOException e) {}
        return false;
    }

    @Override
    public long lastModified() {
        try {return repository.getLastModifiedTime(this);} catch (IOException e) {}
        return 0L;
    }

    @Override
    public long length() {
        try {return repository.getLength(this);} catch (IOException e) {}
        return 0L;
    }


    @Override
    public boolean createNewFile() throws IOException {
        return repository.createNewFile(path);
    }

    @Override
    public boolean delete() {
        try {return repository.delete(this);} catch (IOException e) {}
        return false;
    }

    @Override
    public void deleteOnExit() {
        // this should not be possible I think
    }

    @Override
    public String[] list() {
        try {
            return repository.list(this);} 
            catch (IOException e) {
                //System.err.println("Data repository error: "+e.toString());
                //e.printStackTrace(System.err);
            }
        return new String[0];
    }

    @Override
    public String[] list(FilenameFilter filter) {
        String names[] = list();
        if ((names == null) || (filter == null)) {
            return names;
        }
        List<String> v = new ArrayList<String>();
        for (int i = 0 ; i < names.length ; i++) {
            if (filter.accept(this, names[i])) {
                v.add(names[i]);
            }
        }
        return v.toArray(new String[v.size()]);
    }

    @Override
    public File[] listFiles() {
        String[] ss = list();
        if (ss == null) return null;
        int n = ss.length;
        File[] list = new File[n];
        for (int i = 0; i < n; i++) {
            list[i] = new DataRepositoryFile(ss[i], this);
        }
        return list;
    }

    @Override
    public File[] listFiles(FilenameFilter filter) {
        String ss[] = list();
        if (ss == null) return null;
        ArrayList<File> files = new ArrayList<File>();
        for (String s : ss)
            if ((filter == null) || filter.accept(this, s))
                files.add(new DataRepositoryFile(s, this));
        return files.toArray(new File[files.size()]);
    }

    @Override
    public File[] listFiles(FileFilter filter) {
        String ss[] = list();
        if (ss == null) return null;
        ArrayList<File> files = new ArrayList<File>();
        for (String s : ss) {
            DataRepositoryFile f = new DataRepositoryFile(s, this);
            if ((filter == null) || filter.accept(f))
                files.add(f);
        }
        return files.toArray(new File[files.size()]);
    }

    @Override
    public boolean mkdir() {
        try {return repository.createDirectory(this);} catch (IOException e) {}
        return false; 
    }

    @Override
    public boolean mkdirs() { // this is mostly just copied from the parent class (File) 
        if (exists()) {
            return false;
        }
        if (mkdir()) {
            return true;
        }
        DataRepositoryFile canonFile = null;
        try {
            canonFile = getCanonicalFile();
        } catch (IOException e) {
            return false;
        }

        DataRepositoryFile parent = canonFile.getParentFile();       
        return (parent != null && (parent.mkdirs() || parent.exists()) &&
                canonFile.mkdir());
    }

    @Override
    public boolean renameTo(File dest) {
        if (!(dest instanceof DataRepositoryFile)) return false;
        try {return repository.rename(this, (DataRepositoryFile)dest);} catch (IOException e) {}
        return false;
    }

    @Override
    public boolean setLastModified(long time) {
        try {return repository.setLastModifiedTime(this, time);} catch (IOException e) {}
        return false;
    }

    @Override
    public boolean setReadOnly() {
        return false;
    }

    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }


    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return false;
    }


    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return false;
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public long getTotalSpace() {
        return repository.getTotalSpace();
    }
    @Override
    public long getFreeSpace() {
        return repository.getFreeSpace();
    }

    @Override
    public long getUsableSpace() {        
        return repository.getUsableSpace();
    }

    @Override
    public int compareTo(File pathname) {
        String thisPath=this.getPath();
        String otherPath=pathname.getPath();
        return thisPath.compareTo(otherPath);
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof DataRepositoryFile)) {
            if (!((DataRepositoryFile)obj).getRepositoryName().equals(this.getRepositoryName())) return false;
            return compareTo((DataRepositoryFile)obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return getPath();
    }


    /**
     * WriteObject is called to save this filename.
     * The separator_rep character is saved also so it can be replaced
     * in case the path is reconstituted on a different host type.
     * <p>
     * @serialData  Default fields followed by separator_rep character.
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
//        s.writeChar(this.separatorChar_rep); // Add the separator_rep character
    }

    /**
     * readObject is called to restore this filename.
     * The original separator_rep character is read.  If it is different
     * than the separator_rep character on this system, then the old separator_rep
     * is replaced by the local separator_rep.
     */
    private synchronized void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
//        ObjectInputStream.GetField fields = s.readFields();
//        String pathField = (String)fields.get("path", null);
        s.defaultReadObject();        
//        char sep = s.readChar(); // read the previous separator_rep char
//        String pathField=this.path; // this should have been set by s.defaultReadObject();        
//        if (sep != separatorChar_rep) pathField = pathField.replace(sep, separatorChar_rep);
        this.path = repository.normalize(this.path);
        this.prefixLength = repository.prefixLength(this.path);
    }

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = 301077366599181567L;

    // -- Integration with java.nio.file --

    private volatile transient Path filePath;

    @Override
    public Path toPath() {
        //System.err.println("DRF:toPath:"+path);
        Path result = filePath;
        if (result == null) {
            synchronized (this) {
                result = filePath;
                if (result == null) {
                    result = Paths.get(path);
                    filePath = result;
                }
            }
        }
        return result;
    }    
    
}
