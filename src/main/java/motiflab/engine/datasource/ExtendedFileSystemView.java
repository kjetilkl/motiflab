
package motiflab.engine.datasource;

import javax.swing.*;

import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import motiflab.engine.MotifLabEngine;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import motiflab.engine.datasource.ExtendedFileSystemView.FileSystemRoot;
import sun.awt.shell.*;

/**
 * An extension of the original FileSystemView class
 * @author kjetikl
 */
public abstract class ExtendedFileSystemView extends FileSystemView {
    static DataRepository[] repositories=null;
    
    static ExtendedFileSystemView windowsFileSystemView = null;
    static ExtendedFileSystemView unixFileSystemView = null;
    //static FileSystemView macFileSystemView = null;
    static ExtendedFileSystemView genericFileSystemView = null;

    private boolean useSystemExtensionHiding =
            UIManager.getDefaults().getBoolean("FileChooser.useSystemExtensionHiding");

    public static ExtendedFileSystemView getFileSystemView(DataRepository[] repos) {
        repositories=repos;
        if(File.separatorChar == '\\') { // this is the File separatorChar of the native OS!
            if(windowsFileSystemView == null) {
                windowsFileSystemView = new WindowsFileSystemView();
            }
            return windowsFileSystemView;
        }

        if(File.separatorChar == '/') { // this is the File separatorChar of the native OS!
            if(unixFileSystemView == null) {
                unixFileSystemView = new UnixFileSystemView();
            }
            return unixFileSystemView;
        }

        if(genericFileSystemView == null) {
            genericFileSystemView = new GenericFileSystemView();
        }
        return genericFileSystemView;
    }

    public ExtendedFileSystemView() {
        final WeakReference<ExtendedFileSystemView> weakReference = new WeakReference<ExtendedFileSystemView>(this);

        UIManager.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ExtendedFileSystemView fileSystemView = weakReference.get();

                if (fileSystemView == null) {
                    // FileSystemView was destroyed
                    UIManager.removePropertyChangeListener(this);
                } else {
                    if (evt.getPropertyName().equals("lookAndFeel")) {
                        fileSystemView.useSystemExtensionHiding =
                                UIManager.getDefaults().getBoolean("FileChooser.useSystemExtensionHiding");
                    }
                }
            }
        });
    }

    public FileView getFileView() {
        return new ExtendedFileView();
    }
    
    
    /**
     * Determines if the given file is a root in the navigatable tree(s).
     * Examples: Windows 98 has one root, the Desktop folder. DOS has one root
     * per drive letter, <code>C:\</code>, <code>D:\</code>, etc. Unix has one root,
     * the <code>"/"</code> directory.
     *
     * The default implementation gets information from the <code>ShellFolder</code> class.
     *
     * @param f a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root in the navigatable tree.
     * @see #isFileSystemRoot
     */
    @Override    
    public boolean isRoot(File f) {
        if (f == null || !f.isAbsolute()) {
            return false;
        }

        File[] roots = getRoots();
        for (File root : roots) {
            if (root.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the file (directory) can be visited.
     * Returns false if the directory cannot be traversed.
     *
     * @param f the <code>File</code>
     * @return <code>true</code> if the file/directory can be traversed, otherwise <code>false</code>
     * @see JFileChooser#isTraversable
     * @see FileView#isTraversable
     * @since 1.4
     */
    @Override    
    public Boolean isTraversable(File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    /**
     * Name of a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "M:\" directory
     * displays as "CD-ROM (M:)"
     *
     * The default implementation gets information from the ShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return the file name as it would be displayed by a native file chooser
     * @see JFileChooser#getName
     * @since 1.4
     */
    @Override    
    public String getSystemDisplayName(File f) {
        if (f == null) {
            return null;
        }
        if (f instanceof DataRepositoryFile) {
            String override=((DataRepositoryFile)f).getSystemDisplayName();
            if (override!=null) return override;
        }
        String name = f.getName();

        if (!name.equals("..") && !name.equals(".") &&
                (useSystemExtensionHiding || !isFileSystem(f) || isFileSystemRoot(f)) &&
                (f instanceof ShellFolder || f.exists())) {

            try {
                name = getShellFolder(f).getDisplayName();
            } catch (FileNotFoundException e) {
                return null;
            }

            if (name == null || name.length() == 0) {
                name = f.getPath(); // e.g. "/"
            }
        }

        return name;
    }

    /**
     * Type description for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "Desktop" folder
     * is described as "Desktop".
     *
     * Override for platforms with native ShellFolder implementations.
     *
     * @param f a <code>File</code> object
     * @return the file type description as it would be displayed by a native file chooser
     * or null if no native information is available.
     * @see JFileChooser#getTypeDescription
     * @since 1.4
     */
    @Override    
    public String getSystemTypeDescription(File f) {
        return null;
    }

    /**
     * Icon for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "M:\" directory
     * displays a CD-ROM icon.
     *
     * The default implementation gets information from the ShellFolder class.
     *
     * @param f a <code>File</code> object
     * @return an icon as it would be displayed by a native file chooser
     * @see JFileChooser#getIcon
     * @since 1.4
     */
    @Override    
    public Icon getSystemIcon(File f) {  
        if (f == null) {
            return null;
        }
        if (f instanceof DataRepositoryFile) {
            Icon icon=((DataRepositoryFile)f).getIcon();
            if (icon!=null) return icon;
            // ShellFolder can only be used to get native icons for existing local files, so we have to resort to default L&F icons for directory and file
            return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
        }

        ShellFolder sf;
        try {
            sf = getShellFolder(f);
        } catch (FileNotFoundException e) {
            return null;
        }
        
        Image img = sf.getIcon(false); // this will take the icon from the OS (I think...)
        if (img != null) {
            return new ImageIcon(img, sf.getFolderType());
        } else {
            return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon"); // this will be the default L&F icon for files and folders
        }
    }

    /**
     * On Windows, a file can appear in multiple folders, other than its
     * parent directory in the filesystem. Folder could for example be the
     * "Desktop" folder which is not the same as file.getParentFile().
     *
     * @param folder a <code>File</code> object representing a directory or special folder
     * @param file a <code>File</code> object
     * @return <code>true</code> if <code>folder</code> is a directory or special folder and contains <code>file</code>.
     * @since 1.4
     */
    @Override    
    public boolean isParent(File folder, File file) {
        if (folder == null || file == null) {
            return false;
        } else if (folder instanceof ShellFolder) {
                File parent = file.getParentFile();
                if (parent != null && parent.equals(folder)) {
                    return true;
                }
            File[] children = getFiles(folder, false);
            for (File child : children) {
                if (file.equals(child)) {
                    return true;
                }
            }
            return false;
        } else {
            return folder.equals(file.getParentFile());
        }
    }

    /**
     *
     * @param parent a <code>File</code> object representing a directory or special folder
     * @param fileName a name of a file or folder which exists in <code>parent</code>
     * @return a File object. This is normally constructed with <code>new
     * File(parent, fileName)</code> except when parent and child are both
     * special folders, in which case the <code>File</code> is a wrapper containing
     * a <code>ShellFolder</code> object.
     * @since 1.4
     */
    @Override    
    public File getChild(File parent, String fileName) {
        if (parent instanceof ShellFolder) {
            File[] children = getFiles(parent, false);
            for (File child : children) {
                if (child.getName().equals(fileName)) {
                    return child;
                }
            }
        }
        return createFileObject(parent, fileName);
    }


    /**
     * Checks if <code>f</code> represents a real directory or file as opposed to a
     * special folder such as <code>"Desktop"</code>. Used by UI classes to decide if
     * a folder is selectable when doing directory choosing.
     *
     * @param f a <code>File</code> object
     * @return <code>true</code> if <code>f</code> is a real file or directory.
     * @since 1.4
     */
    @Override    
    public boolean isFileSystem(File f) {
        if (f instanceof ShellFolder) {
            ShellFolder sf = (ShellFolder)f;
            // Shortcuts to directories are treated as not being file system objects,
            // so that they are never returned by JFileChooser.
            return sf.isFileSystem() && !(sf.isLink() && sf.isDirectory());
        } else {
            return true;
        }
    }

    /**
     * Creates a new folder with a default folder name.
     */
    @Override    
    public abstract File createNewFolder(File containingDir) throws IOException;

    /**
     * Returns whether a file is hidden or not.
     */
    @Override    
    public boolean isHiddenFile(File f) {
        return f.isHidden();
    }


    /**
     * Is dir the root of a tree in the file system, such as a drive
     * or partition. Example: Returns true for "C:\" on Windows 98.
     *
     * @param dir a <code>File</code> object representing a directory
     * @return <code>true</code> if <code>f</code> is a root of a filesystem
     * @see #isRoot
     * @since 1.4
     */
    @Override    
    public boolean isFileSystemRoot(File dir) {
        if (dir instanceof DataRepositoryFile) {
            return ((DataRepositoryFile)dir).isRoot();
        }
        return ShellFolder.isFileSystemRoot(dir);
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for drives or partitions, e.g. a "hard disk" icon.
     *
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    @Override    
    public boolean isDrive(File dir) {
        return false; // same as in superclass
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for a floppy disk. Implies isDrive(dir).
     *
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    @Override    
    public boolean isFloppyDrive(File dir) {
        return false;  // same as in superclass
    }

    /**
     * Used by UI classes to decide whether to display a special icon
     * for a computer node, e.g. "My Computer" or a network server.
     *
     * The default implementation has no way of knowing, so always returns false.
     *
     * @param dir a directory
     * @return <code>false</code> always
     * @since 1.4
     */
    @Override    
    public boolean isComputerNode(File dir) {
        return ShellFolder.isComputerNode(dir);
    }


    /**
     * Returns all root partitions on this system. For example, on
     * Windows, this would be the "Desktop" folder, while on DOS this
     * would be the A: through Z: drives.
     */
    @Override    
    public File[] getRoots() {
        // Don't cache this array, because filesystem might change
        File[] roots = (File[])ShellFolder.get("roots");

        for (int i = 0; i < roots.length; i++) {
            if (isFileSystemRoot(roots[i])) {
                roots[i] = createFileSystemRoot(roots[i]);
            }
        }
        // add data repositories as roots
        if (repositories!=null && repositories.length>0) {
            File[] newRoots=new File[roots.length+repositories.length];
            System.arraycopy(roots, 0, newRoots, 0, roots.length);
            for (int i = 0; i < repositories.length; i++) {
                newRoots[i+roots.length]=repositories[i].getRoot();
            }
            roots=newRoots;
        }
        return roots;
    }


    // Providing default implementations for the remaining methods
    // because most OS file systems will likely be able to use this
    // code. If a given OS can't, override these methods in its
    // implementation.
    @Override
    public File getHomeDirectory() {
        return createFileObject(System.getProperty("user.home"));
    }

    /**
     * Return the user's default starting directory for the file chooser.
     *
     * @return a <code>File</code> object representing the default
     *         starting folder
     * @since 1.4
     */
    @Override    
    public File getDefaultDirectory() {
        File f = (File)ShellFolder.get("fileChooserDefaultFolder");
        if (isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }
        return f;
    }

    /**
     * Returns a File object constructed in dir from the given filename.
     */
    @Override    
    public File createFileObject(File dir, String filename) {
        if(dir == null) {
            return createFileObject(filename);
        } else {
            if (dir instanceof DataRepositoryFile) return new DataRepositoryFile((DataRepositoryFile)dir, filename);
            return new File(dir, filename);
        }
    }

    /**
     * Returns a File object constructed from the given path string.
     */
    @Override    
    public File createFileObject(String path) {
        // check if the path points to a repository
        DataRepositoryFile rep=null;
        if (repositories!=null && repositories.length>0) {
            rep=DataRepositoryFile.getRepositoryFileFromString(path, MotifLabEngine.getEngine()); // Note that the second argument is just a shortcut to get to the engine
        }        
        File f = (rep!=null)?rep:new File(path);
        if (isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }
        return f;
    }


    /**
     * Gets the list of shown (i.e. not hidden) files.
     */
    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        //System.err.println("getFiles[]:"+dir.toString()+"  isDRF="+(dir instanceof DataRepositoryFile));
        if (dir instanceof DataRepositoryFile) return getRepositoryFiles(((DataRepositoryFile)dir), useFileHiding);
        
        List<File> files = new ArrayList<File>();

        // add all files in dir
        if (!(dir instanceof ShellFolder)) {
            try {
                dir = getShellFolder(dir);
            } catch (FileNotFoundException e) {
                return new File[0];
            }
        }

        File[] names = ((ShellFolder) dir).listFiles(!useFileHiding);

        if (names == null) {
            return new File[0];
        }

        for (File f : names) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            if (!(f instanceof ShellFolder)) {
                if (isFileSystemRoot(f)) {
                    f = createFileSystemRoot(f);
                }
                try {
                    f = ShellFolder.getShellFolder(f);
                } catch (FileNotFoundException e) {
                    // Not a valid file (wouldn't show in native file chooser)
                    // Example: C:\pagefile.sys
                    continue;
                } catch (InternalError e) {
                    // Not a valid file (wouldn't show in native file chooser)
                    // Example C:\Winnt\Profiles\joe\history\History.IE5
                    continue;
                }
            }
            if (!useFileHiding || !isHiddenFile(f)) {
                files.add(f);
            }
        }

        return files.toArray(new File[files.size()]);
    }
    
    
    public File[] getRepositoryFiles(DataRepositoryFile dir, boolean useFileHiding) {
        List<File> files = new ArrayList<File>();
        File[] names = dir.listFiles(); // returns a File[] containing DataRepositoryFiles
        if (names == null) {
            return new File[0];
        }
        for (File f : names) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            if (!useFileHiding || !isHiddenFile(f)) {
                files.add(f);
            }
        }
        return files.toArray(new File[files.size()]);
    }



    /**
     * Returns the parent directory of <code>dir</code>.
     * @param dir the <code>File</code> being queried
     * @return the parent directory of <code>dir</code>, or
     *   <code>null</code> if <code>dir</code> is <code>null</code>
     */
    @Override    
    public File getParentDirectory(File dir) {
        //System.err.println("getParentDir:"+dir.toString());
        if (dir == null || !dir.exists()) {
            return null;
        }
        if (dir instanceof DataRepositoryFile) {
            return ((DataRepositoryFile)dir).getParentFile();
        }
        
        ShellFolder sf;

        try {
            sf = getShellFolder(dir);
        } catch (FileNotFoundException e) {
            return null;
        }

        File psf = sf.getParentFile();

        if (psf == null) {
            return null;
        }

        if (isFileSystem(psf)) {
            File f = psf;
            if (!f.exists()) {
                // This could be a node under "Network Neighborhood".
                File ppsf = psf.getParentFile();
                if (ppsf == null || !isFileSystem(ppsf)) {
                    // We're mostly after the exists() override for windows below.
                    f = createFileSystemRoot(f);
                }
            }
            return f;
        } else {
            return psf;
        }
    }

    /**
     * Throws {@code FileNotFoundException} if file not found or current thread was interrupted
     */
    ShellFolder getShellFolder(File f) throws FileNotFoundException {
        if (!(f instanceof ShellFolder) && !(f instanceof FileSystemRoot) && isFileSystemRoot(f)) {
            f = createFileSystemRoot(f);
        }

        try {
            return ShellFolder.getShellFolder(f);
        } catch (InternalError e) {
            //System.err.println("FileSystemView.getShellFolder: f="+f);
            //e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new <code>File</code> object for <code>f</code> with correct
     * behavior for a file system root directory.
     *
     * @param f a <code>File</code> object representing a file system root
     *          directory, for example "/" on Unix or "C:\" on Windows.
     * @return a new <code>File</code> object
     * @since 1.4
     */
    @Override    
    protected File createFileSystemRoot(File f) {
        //System.err.println("createFileSystemRoot:"+f.toString());
        return new FileSystemRoot(f);
    }




    static class FileSystemRoot extends File {
        public FileSystemRoot(File f) {
            super(f,"");
        }

        public FileSystemRoot(String s) {
            super(s);
        }

        @Override        
        public boolean isDirectory() {
            return true;
        }
        @Override
        public String getName() {
            return getPath();
        }
    }
    

    // ***********************
    // * FileView operations *
    // ***********************
    protected class ExtendedFileView extends FileView {

        public ExtendedFileView() {       
        }
        @Override
        public String getName(File f) {
            return getSystemDisplayName(f);
        }

        @Override
        public String getDescription(File f) {
            return f.getName();
        }
        @Override
        public String getTypeDescription(File f) {
            String type = getSystemTypeDescription(f);
            if (type == null) {
                if (f.isDirectory()) {
                    type = UIManager.getString("FileChooser.directoryDescriptionText");
                } else {
                    type = UIManager.getString("FileChooser.fileDescriptionText");
                }
            }
            return type;
        }
        @Override
        public Icon getIcon(File f) {
            return getSystemIcon(f);
        }

        public Boolean isHidden(File f) {
            String name = f.getName();
            if(name != null && name.charAt(0) == '.') {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }   
    
}

/**
 * FileSystemView that handles some specific unix-isms.
 */
class UnixFileSystemView extends ExtendedFileSystemView {

    private static final String newFolderString =
            UIManager.getString("FileChooser.other.newFolder");
    private static final String newFolderNextString  =
            UIManager.getString("FileChooser.other.newFolder.subsequent");

    /**
     * Creates a new folder with a default folder name.
     */
    @Override
    public File createNewFolder(File containingDir) throws IOException {
        if(containingDir == null) {
            throw new IOException("Containing directory is null:");
        }
        File newFolder;
        // Unix - using OpenWindows' default folder name. Can't find one for Motif/CDE.
        newFolder = createFileObject(containingDir, newFolderString);
        int i = 1;
        while (newFolder.exists() && i < 100) {
            newFolder = createFileObject(containingDir, MessageFormat.format(
                    newFolderNextString, new Integer(i)));
            i++;
        }

        if(newFolder.exists()) {
            throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
        } else {
            newFolder.mkdirs();
        }

        return newFolder;
    }
    @Override
    public boolean isFileSystemRoot(File dir) {
        return dir != null && dir.getAbsolutePath().equals("/");
    }
    @Override
    public boolean isDrive(File dir) {
        return isFloppyDrive(dir);
    }
    @Override
    public boolean isFloppyDrive(File dir) {
        // Could be looking at the path for Solaris, but wouldn't be reliable.
        // For example:
        // return (dir != null && dir.getAbsolutePath().toLowerCase().startsWith("/floppy"));
        return false;
    }
    @Override
    public boolean isComputerNode(File dir) {
        if (dir != null) {
            String parent = dir.getParent();
            if (parent != null && parent.equals("/net")) {
                return true;
            }
        }
        return false;
    }
}


/**
 * FileSystemView that handles some specific windows concepts.
 */
class WindowsFileSystemView extends ExtendedFileSystemView {

    private static final String newFolderString =
            UIManager.getString("FileChooser.win32.newFolder");
    private static final String newFolderNextString  =
            UIManager.getString("FileChooser.win32.newFolder.subsequent");
    
    @Override    
    public Boolean isTraversable(File f) {
        //System.err.println("[W] isTraversable:"+f.toString());
        return Boolean.valueOf(isFileSystemRoot(f) || isComputerNode(f) || f.isDirectory());
    }
    
    @Override
    public File getChild(File parent, String fileName) {
        //System.err.println("[W] getChild:"+parent.toString()+","+fileName);
        if (fileName.startsWith("\\")
            && !fileName.startsWith("\\\\")
            && isFileSystem(parent)) {

            //Path is relative to the root of parent's drive
            String path = parent.getAbsolutePath();
            if (path.length() >= 2
                && path.charAt(1) == ':'
                && Character.isLetter(path.charAt(0))) {

                return createFileObject(path.substring(0, 2) + fileName);
            }
        }
        return super.getChild(parent, fileName);
    }

    /**
     * Type description for a file, directory, or folder as it would be displayed in
     * a system file browser. Example from Windows: the "Desktop" folder
     * is described as "Desktop".
     *
     * The Windows implementation gets information from the ShellFolder class.
     */
    @Override    
    public String getSystemTypeDescription(File f) {
        if (f == null) {
            return null;
        }
        if (f instanceof DataRepositoryFile) {
            String override=((DataRepositoryFile)f).getTypeDescription();
            if (override!=null) return override;
        }
        try {
            return getShellFolder(f).getFolderType();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /**
     * @return the Desktop folder.
     */
    @Override    
    public File getHomeDirectory() {
        return getRoots()[0];
    }

    /**
     * Creates a new folder with a default folder name.
     */
    @Override    
    public File createNewFolder(File containingDir) throws IOException {
        if(containingDir == null) {
            throw new IOException("Containing directory is null:");
        }
        // Using NT's default folder name
        File newFolder = createFileObject(containingDir, newFolderString);
        int i = 2;
        while (newFolder.exists() && i < 100) {
            newFolder = createFileObject(containingDir, MessageFormat.format(
                newFolderNextString, new Integer(i)));
            i++;
        }

        if(newFolder.exists()) {
            throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
        } else {
            newFolder.mkdirs();
        }

        return newFolder;
    }
    
    @Override
    public boolean isDrive(File dir) {
        //System.err.println("[W] isDrive:"+dir.toString()+" => "+isFileSystemRoot(dir));
        // if (dir instanceof DataRepositoryFile) return false; // Special File icons do not work properly with Nimbus anyway
        return isFileSystemRoot(dir);
    }
    
    @Override
    public boolean isFloppyDrive(final File dir) {
        String path = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return dir.getAbsolutePath();
            }
        });

        return path != null && (path.equals("A:\\") || path.equals("B:\\"));
    }

    /**
     * Returns a File object constructed from the given path string.
     */
    @Override    
    public File createFileObject(String path) {
        // Check for missing backslash after drive letter such as "C:" or "C:filename"
        if (path.length() >= 2 && path.charAt(1) == ':' && Character.isLetter(path.charAt(0))) {
            if (path.length() == 2) {
                path += "\\";
            } else if (path.charAt(2) != '\\') {
                path = path.substring(0, 2) + "\\" + path.substring(2);
            }
        }
        return super.createFileObject(path);
    }
    
    @Override
    protected File createFileSystemRoot(File f) {
        //System.err.println("[W] createFileSystemRoot:"+f.toString());
        // Problem: Removable drives on Windows return false on f.exists()
        // Workaround: Override exists() to always return true.
        return new FileSystemRoot(f) {
            public boolean exists() {
                return true;
            }
        };
    }

}

/**
 * Fallthrough FileSystemView in case we can't determine the OS.
 */
class GenericFileSystemView extends ExtendedFileSystemView {

    private static final String newFolderString =
            UIManager.getString("FileChooser.other.newFolder");

    /**
     * Creates a new folder with a default folder name.
     */
    @Override    
    public File createNewFolder(File containingDir) throws IOException {
        if(containingDir == null) {
            throw new IOException("Containing directory is null:");
        }
        // Using NT's default folder name
        File newFolder = createFileObject(containingDir, newFolderString);

        if(newFolder.exists()) {
            throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
        } else {
            newFolder.mkdirs();
        }

        return newFolder;
    }       
}


