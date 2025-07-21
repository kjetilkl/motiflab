/*
 
 
 */

package org.motiflab.engine.data;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Closeable;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import org.motiflab.engine.datasource.DataRepository;
import org.motiflab.engine.datasource.DataRepositoryFile;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.dataformat.DataFormat_HTML;
import org.motiflab.engine.protocol.ParseError;
import org.apache.commons.io.FileUtils;


/**
 * This Data subclass represents output objects made for instance
 * by the "output" operations.
 * 
 * @author kjetikl
 */
public class OutputData extends Data {

    protected String name="Output";
    protected static final String typedescription="Output";
    protected AbstractDocument document;
    protected String dataformat=null; // a string describing the name of the dataformat used when outputting to this data object. If multiple formats are used the special symbol "*" should be used to denote this
    protected transient String filename=null; // associated file name (full path). This is the file that the document is currently being saved to
    protected Object sourcefile=null; // the sourcefile from where this document originated (if imported from a file). This can be a regular File, a path (String) or an URL
    protected boolean dirtyflag=false; // this flag should be set to true if the object has been updated since the last save
    protected boolean showAsHTML=false;
    protected ArrayList<OutputDataDependency> dependencies=null; // lists the dependencies that are associated with this output object (e.g. external image files). Dependency-files are usually temp-files and will be copied and renamed on save
    private transient Object[] restoredDependencies=null; // this is used to temporary store images and other dependencies that will be saved to tempfiles during session restore
    private boolean serializeDependencies=false; // a flag which determines whether dependent files should be serialized also. As a rule, dependent files need not be serialized except when saving sessions
    protected boolean binaryFormat=false; // OutputData objects containing binary contents can not be shown in the MotifLab GUI, but the contents can still be saved to disc!
    protected String preferredSuffix=null; // the suffix is usually based on the default suffix of the data format, but it can be overridden on a per object basis
    public transient int tabIndex=-1; // this is used by the GUI to restore the OutputData tab in the correct order when undoing "close tab".  -1 means "no assigned index"
    
    @Override
    public String getName() {
        return name;
    }
    

    public static String getType() {
        return typedescription;
    }
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    
    @Override
    public String getTypeDescription() {return typedescription;}   
        
    @Override
    public Object getValue() {
        return document;
    }


    @Override
    public String getValueAsParameterString() {return document.toString();}
    
    /** Returns the Document that contains the text for this output object */
    public AbstractDocument getDocument() {
        return document;
    }

    /** 
     * Returns the name of the Data Format used for the text in this output object 
     * If there is no text, the format will be NULL. If the object contains text in 
     * several different formats, the special symbol "*" will be returned to denote this.
     */
    public String getDataFormat() {
        return dataformat;
    }
    
    
    /** updates the DataFormat (or formats) used for the text in this output object */
    public void setDataFormat(String dataFormat) {
        if (dataformat==null) dataformat=dataFormat;
        else if (!dataformat.equals(dataFormat)) dataformat="*";          
    }
    
    /** Sets the name of a file to be associated with this Output object (the file that this object is saved to locally) */
    public void setFileName(String filename) {
        this.filename=filename;      
    }
    
    /** Gets the name of the file currently associated with this Output object */
    public String getFileName() {
        return filename;      
    }
    
    /** Sets the preferred file suffix to use for this object only. 
     *  This value will be returned by the getPreferredFileSuffix() method.
     *  If no suffix is set (or it is set to NULL) the suffix returned by the getPreferredFileSuffix()
     *  method will instead be the default suffix returned by the data format that formatted this object
     *  (or "txt" if the object contains a mix of formats)
     */
    public void setPreferredFileSuffix(String suffix) {
        preferredSuffix=suffix;
    }
    
    /** Returns the preferred file suffix to use when saving this object to file
     *  This is usually based on the data format but can be overridden with a call to setPreferredFileSuffix(). 
     */
    public String getPreferredFileSuffix() {
        if (preferredSuffix!=null) return preferredSuffix;
        String dataFormatName=getDataFormat();
        String suffix="txt";
        if (dataFormatName!=null && !dataFormatName.equals("*")) {
            DataFormat format=MotifLabEngine.getEngine().getDataFormat(dataFormatName);
            if (format!=null) suffix=format.getSuffix();
        } 
        return suffix;
    }
    
    /** Set this to true if the output in this object contains HTML tags and should be displayed formatted */
    public void setShowAsHTML(boolean flag) {
        showAsHTML=flag;
    }
    
    /** Returns TRUE if this OutputData object contains HTML tags and should be displayed formatted */
    public boolean isHTMLformatted() {
        return showAsHTML;
    }
    
    /** Returns true if the Output object has been updated/changed since the last saved version */
    public boolean isDirty() {
        return dirtyflag;
    } 
    
    /** Sets the "dirty flag" for this Output object (which indicates that the object has been updated/changed since the last saved version) */
    public void setDirty(boolean isDirty) {
        dirtyflag=isDirty;
    }
    
    /** Set this to true if the output in this object is a binary file that should not be displayed in the MotifLab GUI */
    public void setBinary(boolean isBinary) {
        binaryFormat=isBinary;
    }
    
    /** Returns true if the Output object contains data in a binary format that should not be displayed
     *  in the MotifLab GUI
     */
    public boolean isBinary() {
        return binaryFormat;
    }     

    /** Sets the File, path or URL that this Output object was imported from */
    public void setSourceFile(Object source) {
        if (source instanceof DataRepositoryFile) this.sourcefile=((DataRepositoryFile)source).getAbsolutePath(); // not taking any chances
        this.sourcefile=source;      
    }
    
    /** Gets the File, path (String) or URL of the file that this Output object was imported from */
    public Object getSourceFile() {
        return sourcefile;      
    }    
    
    
    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        this.name=((OutputData)source).name;
        this.filename=((OutputData)source).filename;
        this.sourcefile=((OutputData)source).sourcefile;
        this.dirtyflag=((OutputData)source).dirtyflag;
        this.dataformat=((OutputData)source).dataformat;
        this.showAsHTML=((OutputData)source).showAsHTML;
        this.binaryFormat=((OutputData)source).binaryFormat;
        this.dependencies=((OutputData)source).dependencies;
        this.dataformat=((OutputData)source).dataformat;
        try { // copy only the text of the document - no other properties! (such as DocumentListeners)
            document.remove(0, document.getLength()); // clear current content
            Document sourcedocument=((OutputData)source).document;
            String fullText=sourcedocument.getText(0, sourcedocument.getLength());
            document.insertString(0, fullText, null);
        } catch (BadLocationException e) {}
        //notifyListenersOfDataUpdate(); 
    }


    @Override
    public void rename(String newName) {
        this.name=newName;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public OutputData clone() {  
        PlainDocument doc=new PlainDocument();        
        try {
            String fullText=document.getText(0, document.getLength());
            doc.insertString(0, fullText, null);
        } catch (BadLocationException e) {}
        OutputData newdata=new OutputData(name, doc,dataformat);
        newdata.filename=filename;
        newdata.sourcefile=sourcefile;
        newdata.dirtyflag=dirtyflag;
        newdata.showAsHTML=showAsHTML;
        newdata.binaryFormat=binaryFormat;
        if (dependencies!=null) newdata.dependencies=(ArrayList<OutputDataDependency>)dependencies.clone();
        else newdata.dependencies=null;
        return newdata;
    }   
    
    @Override
    public String output() {
        try {
           return document.getText(0, document.getLength());
        } catch (Exception e) {return "Error!";}
    } 
    
   /**
    * Erases the contents of this output object.
    * Including dependencies and all other meta-information.
    * @return true if the clear operation was successful
    */
    public boolean clear() {
        try {
            document.remove(0, document.getLength());
            dataformat=null;
            dirtyflag=true;
            sourcefile=null;
            binaryFormat=false;
            dependencies=null;
            notifyListenersOfDataUpdate();
            return true;
        } catch (BadLocationException e) {return false;}
    }

    /**
     * Appends the given text to the end of this OutputData object
     * @param text The text to be appended at the end
     * @param dataformat The name of the DataFormat used for this text
     * @return true if the append operation was successful
     */
    public boolean append(String text, String dataFormat) {       
        try {
            document.insertString(document.getLength(), text, null);
            setDataFormat(dataFormat);
            dirtyflag=true;
            notifyListenersOfDataUpdate();
            return true;
        } catch (BadLocationException e) {return false;}
    }
    
    /**
     * Appends the given lines of text to the end of this OutputData object
     * (using \n as line separator)
     * @param text The text to be appended at the end
     * @param dataformat The name of the DataFormat used for this text
     * @return true if the append operation was successful
     */
    public boolean append(ArrayList<String> text, String dataFormat) {       
        try {
            for (String line:text) {
                document.insertString(document.getLength(), line, null);
                document.insertString(document.getLength(), "\n", null);                
            }
            setDataFormat(dataFormat);
            dirtyflag=true;
            notifyListenersOfDataUpdate();
            return true;
        } catch (BadLocationException e) {return false;}
    }    
    
    public ArrayList<String> getContentsAsStrings() {        
        String fullText=null;
        try {
            fullText=document.getText(0, document.getLength());
        } catch (BadLocationException e) {System.err.println("SYSTEM ERROR: BadLocationException in OutputData.getContentsAsStrings()");}
        String[] lines=fullText.split("\n");
        ArrayList<String> result=new ArrayList<String>();
        result.addAll(Arrays.asList(lines));
        return result;
    }
    
    public String getContentsAsString() {        
        String fullText=null;
        try {
            fullText=document.getText(0, document.getLength());
        } catch (BadLocationException e) {System.err.println("SYSTEM ERROR: BadLocationException in OutputData.getContentsAsStrings()");}
        return fullText;
    }    
    
    /**
     * Returns the contents of this document which is between the <body ...> and </body> tags
     * If the document is not marked as HTML formatted an empty String will be returned
     * @return 
     */
    public String getHTMLBody() { 
        if (!isHTMLformatted()) return "";
        String fullText=null;
        try {
            fullText=document.getText(0, document.getLength());
        } catch (BadLocationException e) {System.err.println("SYSTEM ERROR: BadLocationException in OutputData.getContentsAsStrings()");}
        int start=fullText.indexOf("<BODY");
        if (start<0) start=fullText.indexOf("<body");
        if (start<0) start=fullText.indexOf("<Body");
        if (start<0) return "";
        start=fullText.indexOf(">",start);
        int end=fullText.indexOf("</BODY",start);
        if (end<0) end=fullText.indexOf("</body",start);
        if (end<0) end=fullText.indexOf("</Body",start);
        if (end<0) return "";
        return fullText.substring(start+1, end);        
    }     
    
    
     /**
     * Creates a new OutputData object with default name "outputdata"
     * @param name
     * @param document
     */
    public OutputData() {
        this.name="outputdata";
        this.document=new PlainDocument();
        this.dataformat=null;
    }   
    
    /**
     * Creates a new OutputData object with the given name
     * @param name
     * @param document
     */
    public OutputData(String name) {
        this.name=name;
        this.document=new PlainDocument();
        this.dataformat=null;
    }
    
    
    /**
     * Creates a new OutputData object with the given name which wraps
     * the given Document
     * @param name
     * @param document
     */
    public OutputData(String name, PlainDocument document, String dataformat) {
        this.name=name;
        this.document=document;
        this.dataformat=dataformat;
    }
    
    /**
     * Creates a new OutputData object with the given name which wraps the given HTML page
     * @param name
     * @param page The URL of the page to wrap
     */
    public OutputData(String name, java.net.URL page) {
        this.name=name;        
        this.showAsHTML=true;
        this.dataformat=DataFormat_HTML.HTML;
        this.document=new PlainDocument();
        try {
           MotifLabEngine.getPage(page,document);
        } catch (Exception ex) {
            try {
                String errorString="<h1><u>ERROR</u></h1><br><br><b>Unable to load page from:</b>&nbsp;&nbsp;&nbsp;"+page.toString()+"<br><br><b>Cause of error:</b>&nbsp;&nbsp;&nbsp;"+ex.toString();
                document.insertString(0, errorString, null);
            } catch (BadLocationException be) {}            
        }
    }    
    
    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        clear();
        for (String line:input) {
            append(line, "Plain");
            append("\n", "Plain");
        }      
    }      
    
    public boolean hasDependencies() {
        return (dependencies!=null && !dependencies.isEmpty());
    }
    
    public int getNumberOfDependencies() {
        return (dependencies!=null)?dependencies.size():0;
    }
    
    /**
     * Returns a count of the number of dependencies whose files either match 
     * or do not match a list of file suffixes
     */
    public int getNumberOfDependencies(String[] suffixes, boolean shouldMatch) {
        if (dependencies==null || dependencies.isEmpty()) return 0;
        int count=0;
        for (OutputDataDependency dep:dependencies) {
            boolean match=endsWithSuffix(suffixes,dep.getInternalPathName());
            if (match && shouldMatch) count++;
            else if (!match && !shouldMatch) count++;
        }
        return count;
    }      
    
    private boolean endsWithSuffix(String[] suffixes, String target) {
        for (String suffix:suffixes) {
            if (target.endsWith(suffix)) return true;
        }
        return false;
    }
    
 
    /** Returns the original list of registered dependencies for this OutputData object */
    public ArrayList<OutputDataDependency> getDependencies() {
         return dependencies;
    }
    
    /** Returns a list of registered dependencies for this OutputData object that either match
     *  or do not match a list of file suffixes
     */
    public ArrayList<OutputDataDependency> getDependencies(String[] suffixes, boolean shouldMatch) {
         ArrayList<OutputDataDependency> result=new ArrayList<OutputDataDependency>();
         for (OutputDataDependency dep:dependencies) {
            boolean match=endsWithSuffix(suffixes,dep.getInternalPathName());
            if (match && shouldMatch) result.add(dep);
            else if (!match && !shouldMatch) result.add(dep);
         }        
         return result;
    }    
    
    /** Returns the OutputDataDependency object with the given index number
     * (starting at 0 for the first dependency). Returns NULL if no dependency
     *  exists for the given index.
     */
    public OutputDataDependency getDependency(int index) {
        if (dependencies==null || dependencies.isEmpty()) return null;
        if (index>=dependencies.size()) return null;
        else return dependencies.get(index);
    }
    
    /** Creates a dependent file and adds it to this output object. 
     *  Dependency files are files such as e.g. image files that are associated with this object and should
     *  be saved together with the object when saving to file
     *  This method is mostly for backwards compatibility
     *  @param engine
     *  @param suffix     
     */
    public File createDependentFile(MotifLabEngine engine, String suffix) {
       OutputDataDependency dependency=createDependency(engine, suffix, false);
       return dependency.getFile();
    }
    
    public File createDependentBinaryFile(MotifLabEngine engine, String suffix) {
       OutputDataDependency dependency=createDependency(engine, suffix, true);
       return dependency.getFile();
    }    

    /** Creates a dependent file and adds it to this output object.
     *  Dependency files are files such as e.g. image files that are associated with this object and should
     *  be saved together with the object when saving to file
     *  @param engine
     *  @param suffix
     */
    public OutputDataDependency createDependency(MotifLabEngine engine, String suffix, boolean binary) {
       if (!suffix.startsWith(".")) suffix="."+suffix;
       File file=engine.createTempFileWithSuffix(suffix);
       String depfilename=file.getAbsolutePath();
       if (dependencies==null) dependencies=new ArrayList<OutputDataDependency>();
       OutputDataDependency dependency=new OutputDataDependency(depfilename);
       dependency.setBinary(binary);
       dependencies.add(dependency);
       return dependency;
    }
    
    /** Makes a separate copy of an existing dependency and adds it as a new dependency to this OutputData object 
     *  The copy will not be "shared" even if the original was
     *  @return A copy of the dependency, complete with its contents in a separate file
     *          or NULL if MotifLab was unable to copy the dependency (for instance if the file could not be duplicated).
     */
    public OutputDataDependency copyDependency(MotifLabEngine engine, OutputDataDependency source) {
        String sourcefilename=source.getInternalPathName();
        if (sourcefilename==null) return null;
        String suffix=".tmp";
        int pos=sourcefilename.lastIndexOf(".");
        if (pos>0) suffix=sourcefilename.substring(pos);
        File file=engine.createTempFileWithSuffix(suffix);
        File sourceFile=source.getFile();
        try {
            MotifLabEngine.copyFile(sourceFile, file);
        } catch (Exception e) {
            return null;
        }
        String depfilename=file.getAbsolutePath();
        OutputDataDependency dependency=new OutputDataDependency(depfilename);             
        dependency.setBinary(source.isBinary());
        if (dependencies==null) dependencies=new ArrayList<OutputDataDependency>();        
        dependencies.add(dependency);
        return dependency;
    }
    
    /** Adds an existing shared dependency to the dependencies of this OutputData object also
     *  Returns TRUE if the dependency was added or FALSE if the dependency was not shared in the first place
     */
    public boolean addSharedDependency(OutputDataDependency shared) {
        if (!shared.isShared()) return false;
        if (dependencies==null) dependencies=new ArrayList<OutputDataDependency>();        
        dependencies.add(shared);        
        return true;
    }
    
     /** 
     *  Creates a shared dependent file and adds it to this output object. 
     *  Shared dependent files have explicit identifiers and will not be created
     *  anew if an instance of the file already exists (they are singletons).
     *  This means that the same file can be reused by multiple OutputData objects
     *  @param engine
     *  @param sharedID
     *  @param suffix
     * 
     */
    public OutputDataDependency createSharedDependency(MotifLabEngine engine, String sharedID, String suffix, boolean binary) {
       if (dependencies==null) dependencies=new ArrayList<OutputDataDependency>();
       OutputDataDependency dependency=engine.getSharedOutputDependency(sharedID);     
       if (dependency==null) { // dependency does not exist, so we have to create it and register it as "shared" with the engine
           if (!suffix.startsWith(".")) suffix="."+suffix;
           dependency=engine.createSharedOutputDependency(sharedID, suffix); // this will add it to the "global" list also
           dependency.setBinary(binary);
       }
       dependencies.add(dependency);
       return dependency;
    }
       
    /**
     * Creates a dependency file containing the given text
     * @param engine 
     * @param text
     * @param sharedID if this is non-NULL the dependency will be shared
     * @return the name of the file that was created locally for this dependency (full path)
     */
    public String createTextfileDependency(MotifLabEngine engine, String text, String sharedID, String suffix) {
       OutputDataDependency dependency=(sharedID!=null)?createSharedDependency(engine, sharedID, suffix, false):createDependency(engine, suffix, false);
       File file=dependency.getFile();
       try {MotifLabEngine.writeTextFile(file, text);} catch (IOException e) {engine.errorMessage("Error while creating dependent text file => "+e.toString(),0);}
       return file.getAbsolutePath();
    }
    
    /** Saves the OutputData contents to a temp file created by the engine
     *  Note that dependent files are not saved together with the document!
     * @return a reference to the new temp-file containing the document
     */
    public File saveToTempFile(MotifLabEngine engine) {
        File file=engine.createTempFile();
        PrintWriter outputStream=null;
        try {
            outputStream=new PrintWriter(new FileWriter(file));
            Element root=document.getDefaultRootElement();
            int linecount=root.getElementCount();
            for (int i=0;i<linecount;i++) { 
                  Element e=root.getElement(i);  
                  String line=document.getText(e.getStartOffset(),e.getEndOffset()-e.getStartOffset());
                  // remove newlines
                  if (line.endsWith("\r\n")) line=line.substring(0,line.length()-2);
                  else if (line.endsWith("\n")) line=line.substring(0,line.length()-1);
                  if (i<linecount-1 || !line.isEmpty()) outputStream.println(line);
            }                    
        } catch (Exception bad) { 
            return null;
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (Exception ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing PrintWriter in OutputData.saveToFile(): "+ioe.getMessage());}
        }
        return file;
    }
    
    
    
    /**
     * Saves the contents of this Output object to a file with the given name. Dependent files will also be saved to the same directory
     * (and associates this object with the given filename from now on)
     * @param filename The full pathname of the file to save in (this could be preceded by a data repository prefix)
     * Note: this method should preferably NOT be called on the EDT 
     */
    public void saveToFile(String filename, MotifLabEngine engine) throws IOException {
        DataRepositoryFile repositoryFile=DataRepositoryFile.getRepositoryFileFromString(filename,engine);
        if (repositoryFile!=null) {
            saveToFile(repositoryFile,true,null,engine);
        } else {
            File file=new File(filename);
            saveToFile(file,true,null,engine);
        }
        
    }
    

    /**
     * Saves the contents of this Output object to the given file
     * @param file The target file (note that depending on the contents of this OutputData object other dependent files can be created as well)
     * @param updateSaveStatus If TRUE this OutputData object will be marked as "saved" (not dirty) and associated with the given file so subsequent calls to getFileName() will return the name of this file
     * Note: this method should preferably NOT be called on the EDT
     * @param file The file to save this OutputData to (this could be a regular File or a DataRepositoryFile)
     * @param updateSaveStatus 
     * @param progressListener If provided, this PropertyChangeListener will receive propertyChange() events 
     *                         where the 'new value' of the event will be an integer between 0 and 100 which
     *                         informs of the progress (in percentage completed)
     */
    public void saveToFile(File file, boolean updateSaveStatus, PropertyChangeListener progressListener, MotifLabEngine engine) throws IOException {
        if (isBinary()) { // use a different method if the contents of the OutputData object is binary
            saveBinaryToFile(file,updateSaveStatus,progressListener,engine);
            return;
        }
        // create necessary parent directories
        boolean ok=true;
        File dir=file.getParentFile();     
        if (dir!=null && !dir.exists()) ok=dir.mkdirs();  
        if (!ok) throw new IOException("Unable to create parent directories for "+file.toString());
        
        String dependencyPrefix=file.getName();
        int suffixpos=dependencyPrefix.lastIndexOf('.');
        if (suffixpos>=0) {
            dependencyPrefix=dependencyPrefix.substring(0,suffixpos);
        }
        Element root=document.getDefaultRootElement();
        int linecount=root.getElementCount();
        PrintWriter outputStream=null; // PrintWriter is buffered!
        try {
            if (file instanceof DataRepositoryFile) outputStream=new PrintWriter(((DataRepositoryFile)file).getFileAsOutputStream());
            else if (file instanceof File) outputStream=new PrintWriter((File)file);
            for (int i=0;i<linecount;i++) { 
                  Element e=root.getElement(i);  
                  String line=document.getText(e.getStartOffset(),e.getEndOffset()-e.getStartOffset());
                  // remove newlines
                  if (line.endsWith("\r\n")) line=line.substring(0,line.length()-2);
                  else if (line.endsWith("\n")) line=line.substring(0,line.length()-1);
                  if (dependencies!=null) line=replaceDependencies(line, dependencyPrefix, true); // replace internal dependency-filenames with the names of the external files (which they will have after they have been saved, which will happen later...) 
                  if (i<linecount-1 || !line.isEmpty()) outputStream.println(line);
                  if (i%100==0 && progressListener!=null) {// report progress every 100th line
                      double p=(double)i/(double)linecount; // document progress
                      double documentFraction=(dependencies==null)?1.0:0.5; // documentFraction=1.0/(double)((dependencies==null)?0:dependencies.size()+1); // portion of total progress reserved for document
                      Integer progress=new Integer((int)(p*documentFraction*100.0));
                      progressListener.propertyChange(new PropertyChangeEvent(this, org.motiflab.engine.task.ExecutableTask.PROGRESS, null, progress));
                  }
            }                    
        } catch (BadLocationException bad) { 
            System.err.println("SYSTEM ERROR: BadLocationException in OutputData.saveToFile(): "+bad.getMessage());
            return;
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (Exception ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing PrintWriter in OutputData.saveToFile(): "+ioe.getMessage());}
        }          
        if (dependencies!=null) saveDependencies(file,dependencyPrefix,progressListener,true,engine);
        if (updateSaveStatus) {
            dirtyflag=false;
            setFileName(filename);
        }
    }
    
    /** This method is used as a 'replacement' for the regular saveToFile method in order     
     *  to save OutputData in binary format. Such OutputData objects do not contain a "document"
     *  that should be saved to file, but only a single dependency which should be saved to the target
     *  file instead of the missing document.
     */
    private void saveBinaryToFile(File file, boolean updateSaveStatus, PropertyChangeListener progressListener, MotifLabEngine engine) throws IOException {
        // create necessary parent directories
        boolean ok=true;
        File dir=file.getParentFile();
        if (dir!=null && !dir.exists()) ok=dir.mkdirs();  
        if (!ok) throw new IOException("Unable to create parent directories for "+file.toString());        

        String dependencyPrefix=file.getName();
        int suffixpos=dependencyPrefix.lastIndexOf('.');
        if (suffixpos>=0) {
            dependencyPrefix=dependencyPrefix.substring(0,suffixpos);
        }     
        if (dependencies==null || dependencies.size()!=1) throw new IOException("Dependency error for binary format");        
        saveDependencies(file,dependencyPrefix,progressListener,false,engine); // do not use incremental file names here!
        if (updateSaveStatus) {
            dirtyflag=false;
            setFileName(filename);
        }
    }    
    
    /**
     * Saves (or rather copies from temp-files) the dependencies of this document
     * to external files in the same directory as the given target file
     * @param file The file that the main document has been saved to. 
     *             This file is only used as a reference so that the dependencies can be saved in the same directory
     * @param prefix A prefix to use in the filenames for the saved dependencies
     *               Dependencies will be given names like "prefix_counter.suffix"
     *               unless they are shared dependencies
     * @throws IOException 
     */
    private void saveDependencies(File file, String prefix, PropertyChangeListener progressListener, boolean useIncrementalFileNames, MotifLabEngine engine) throws IOException {
        int size=dependencies.size();
        for (int i=0;i<size;i++) {
            OutputDataDependency dependency=dependencies.get(i);
            String oldfilename=dependency.getInternalPathName();
            File oldfile=new File(oldfilename); // this is a local dependency
            String newfilename=replaceDependencies(oldfilename, prefix, useIncrementalFileNames); // obtain new filename: "replace" (or rather just return) the new external filename for this dependent file  
            if (file instanceof DataRepositoryFile) {
                DataRepositoryFile newfile=new DataRepositoryFile(((DataRepositoryFile)file).getParentFile(),newfilename); // save dependency files in the same dir as 'file'                
                if (!( dependency.isShared() && newfile.exists() )) {
                    DataRepository repository=engine.getDataRepository(newfile.getRepositoryName());
                    if (repository==null) throw new IOException("Unknown data repository:"+newfile.getRepositoryName());
                    repository.saveFileToRepository(oldfile, newfile.getAbsolutePath());
                }                                 
            } else {
                File newfile=new File(((File)file).getParent(),newfilename); // save dependency files in the same dir as 'file'                
                if (!( dependency.isShared() && newfile.exists() )) {
                    FileUtils.copyFile(oldfile, newfile);
                }                 
            }
            if (progressListener!=null) {
                //Integer progress=new Integer((int)(((double)i+2.0/((double)size+1.0))*100.0)); // i+2/size+1 includes the document itself as first element
                Integer progress=new Integer((int)(((0.5*(double)i/(double)size)+0.5)*100.0)); // first 50% of progress is used by the document itself
                progressListener.propertyChange(new PropertyChangeEvent(this, org.motiflab.engine.task.ExecutableTask.PROGRESS, null, progress));
            }
        }      
    }
 
    
    /** Looks for parts of the string that looks like pathnames for dependent files
     *  and replaces these parts of the text with new filenames 
     *  The new filenames start with the given prefix followed by an underscore and
     *  then a number which corresponds to the order of the dependency in the dependencies list
     *  (so if the same dependency is referenced multiple times in the document 
     *  the new filename will be the same in each replacement)
     */
    private String replaceDependencies(String line, String prefix, boolean useIncrementalFileNames) {
         for (int i=0;i<dependencies.size();i++) {
             OutputDataDependency dependency=dependencies.get(i);
             String oldfilename=dependency.getInternalPathName();
             String suffix="";
             int suffixpos=oldfilename.lastIndexOf('.');
             if (suffixpos>=0) {
                 suffix=oldfilename.substring(suffixpos);
             }
             String newfilename;
             if (dependency.isShared()) newfilename=dependency.getSharedID()+suffix; // always use same name for shared dependencies
             else {
                 if (useIncrementalFileNames) newfilename=prefix+"_"+(i+1)+suffix;
                 else newfilename=prefix+suffix; // note that this will save all dependencies to the same file, so there better be only one!
             }             
             line=line.replace(oldfilename, newfilename);
             line=line.replace("file:///", ""); // remove the file:/// prefix to make the references relative   (Could this affect intentional paths also?)
         }
         return line;
    }
    

    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof OutputData)) return false;
        OutputData other=(OutputData)data;
        if (this.dataformat!=null && other.dataformat==null) return false;
        if (this.dataformat==null && other.dataformat!=null) return false;
        if (this.dataformat!=null && other.dataformat!=null && !this.dataformat.equals(other.dataformat)) return false;
        if (this.filename!=null && other.filename==null) return false;
        if (this.filename==null && other.filename!=null) return false;
        if (this.filename!=null && other.filename!=null && !this.filename.equals(other.filename)) return false;
        if (this.dirtyflag!=other.dirtyflag) return false;
        if (this.showAsHTML!=other.showAsHTML) return false;
        if (this.binaryFormat!=other.binaryFormat) return false;
        if (this.document!=null && other.document==null) return false;
        if (this.document==null && other.document!=null) return false;
        if (this.document!=null && other.document!=null && !this.document.equals(other.document)) return false;
        if (this.dependencies!=null && other.dependencies==null) return false;
        if (this.dependencies==null && other.dependencies!=null) return false;
        if (this.dependencies!=null && other.dependencies!=null && !this.dependencies.equals(other.dependencies)) return false;
        return true;
    }


    // ------------ Serialization ---------

    private static final long serialVersionUID = 1L;

   /** Specifies whether dependent files (e.g. images) should be serialized together
     * with the text when serializing instances of this Data type
     */
    public void setSerializeDependencies(boolean flag) {
        serializeDependencies=flag;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
         // Version 1: All fields are serialized with out.defaultWriteObject(), and then the dependencies are output afterwards
         // Version 2: The "document" field is skipped on serialization, since it is a Swing class and can change over time. Instead, only the text contents of the document is saved.
         short currentinternalversion=2; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         // out.defaultWriteObject(); // to avoid writing the Swing "document", I output all other fields manually and then only save the text for the document
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("name", name);
        fields.put("dataformat", dataformat);
        fields.put("sourcefile", sourcefile);
        fields.put("dirtyflag", dirtyflag);
        fields.put("showAsHTML", showAsHTML);
        fields.put("dependencies", dependencies);
        fields.put("serializeDependencies", serializeDependencies);
        fields.put("binaryFormat", binaryFormat);
        fields.put("preferredSuffix", preferredSuffix);
        out.writeFields();
         // Write the text content from the document              
        String serializedDocument; 
        try {
            serializedDocument=(document!=null)?document.getText(0, document.getLength()):"";
        } catch (BadLocationException e) {serializedDocument = "";}         
        out.writeObject(serializedDocument);
        // output dependencies
        if (serializeDependencies && dependencies!=null) {
            for (int i=0;i<dependencies.size();i++) {
                OutputDataDependency dependency=dependencies.get(i);              
                String depfilename=dependency.getInternalPathName();
                if (dependency.isShared() && dependency.hasBeenProcessed()) {
                     continue;                   
                } else {
                    if (depfilename.toLowerCase().endsWith(".png") || depfilename.toLowerCase().endsWith(".gif")) {
                        File depfile=new File(depfilename);
                        BufferedImage image=ImageIO.read(depfile);
                        SerializableBufferedImage serImage=new SerializableBufferedImage(image);
                        out.writeObject(serImage);
                    } else if (dependency.isBinary()) { // Excel document
                        File depfile=new File(depfilename);
                        try {           
                            // copy the Excel document from the file into the stream as raw bytes
                            byte[] binaryfile=readFileToByteArray(depfile);
                            int size=binaryfile.length;
                            out.writeInt(size);                              
                            out.write(binaryfile);                      
                        } catch (Exception e) {throw new IOException(e.getClass().getSimpleName()+":"+e.getMessage());}
                    } else { // assuming text-based file
                        String text=MotifLabEngine.readTextFile(depfilename);
                        out.writeObject(text);
                    }
                    dependency.setHasBeenProcessed(true); // signal to other OutputDataObjects that share this dependency
                }                    
            }
        }
    }


    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        short currentinternalversion=in.readShort();
        if (currentinternalversion==1) {
            try {               
                in.defaultReadObject();
                readDependencies(in);
            } catch (Exception e) {
                // This graceful handling of errors will probably not work :-(
                String errorMsg = "ERROR: Unable to restore incompatible Output data from earlier version";
                document = new PlainDocument();
                try {
                    document.insertString(0, errorMsg, null);
                } catch (BadLocationException ble) {}   
                return;
            }
        } else if (currentinternalversion==2) {
            // in.defaultReadObject(); // Don't use the default read method, since the "document" field has been skipped in version 2. Instead, restore all fields manually
            ObjectInputStream.GetField fields = in.readFields();           
            name = (String)fields.get("name", "Output");
            dataformat = (String)fields.get("dataformat", null);
            sourcefile = fields.get("sourcefile", null);
            dirtyflag = fields.get("dirtyflag", false);
            showAsHTML = fields.get("showAsHTML", false);
            dependencies = (ArrayList<OutputDataDependency>)fields.get("dependencies", null);
            serializeDependencies = fields.get("serializeDependencies", false);
            binaryFormat = fields.get("binaryFormat", false);
            preferredSuffix = (String)fields.get("preferredSuffix", null);       
            // restore "document" from its text (saved as a String)
            String documentText = (String)in.readObject();
            document = new PlainDocument();
             try {
                 document.insertString(0, documentText, null);
             } catch (BadLocationException e) {}
             readDependencies(in);             
        }         
        else if (currentinternalversion>2) throw new IOException("Unable to read OutputData version "+currentinternalversion);
    }
    
    private void readDependencies(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int dependenciesSize=(dependencies!=null)?dependencies.size():0;
        if (serializeDependencies && dependenciesSize>0) { // note that the serializeDependencies flag used here was just read from the stream
            restoredDependencies=new Object[dependenciesSize];
            for (int i=0;i<dependenciesSize;i++) { // read the contents of each serialized dependency. These will be saved to temp-files later
                OutputDataDependency dependency=dependencies.get(i);
                String depfilename=dependency.getInternalPathName();
                if (dependency.isShared() && dependency.hasBeenProcessed()) {
                    continue;  // don't to anything. It has been done already
                } else {
                    if (depfilename.toLowerCase().endsWith(".png") || depfilename.toLowerCase().endsWith(".gif") || depfilename.toLowerCase().endsWith(".jpg")) {
                        SerializableBufferedImage serImage=(SerializableBufferedImage)in.readObject();
                        BufferedImage image=serImage.getImage();
                        restoredDependencies[i]=image;
                    } 
                    else if (dependency.isBinary()) { // E.g. Excel file
                        try {          
                            int size=in.readInt();                                 
                            byte[] binaryfile=toByteArray(in,size); 
                            restoredDependencies[i]=binaryfile;    
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                            throw new IOException(e.getClass().getSimpleName()+":"+e.getMessage());
                        }
                    } 
                    else {
                        String text=(String)in.readObject();
                        restoredDependencies[i]=text;
                    }
                    dependency.setHasBeenProcessed(true);
                }
            }
        }        
    }

    
    /** This must be called after serialization in order to properly update
     *  previous internal filenames for dependencies to new internal filenames
     */
    public void initializeAfterSerialization(MotifLabEngine engine) throws IOException {
        int dependenciesSize=(dependencies!=null)?dependencies.size():0;
        if (dependenciesSize>0) {           
            ArrayList<OutputDataDependency> newdependencies=new ArrayList<OutputDataDependency>(dependenciesSize);
            for (int i=0;i<dependenciesSize;i++) { // read in each serialized imagefile and output to a new tempfile
                OutputDataDependency dependency=dependencies.get(i);              
                String oldfilename=dependency.getInternalPathName();
                String type=oldfilename.substring(oldfilename.lastIndexOf('.')+1).toLowerCase();
                boolean isImage=(type.equals("png") || type.equals("gif") || type.equals("jpg")); // 
                File file=null;
                if (dependency.isShared()) {
                    OutputDataDependency sharedDependency=engine.getSharedOutputDependency(dependency.getSharedID());
                    if (sharedDependency==null) { // does this shared dependency exists already?
                        // no existing dependency for this shared dependency. Create it and use that from now on
                        sharedDependency=engine.createSharedOutputDependency(dependency.getSharedID(), "."+type);
                        sharedDependency.setBinary(dependency.isBinary());
                        file=sharedDependency.getFile();
                        if (isImage) {
                            BufferedImage image=(BufferedImage)restoredDependencies[i];
                            ImageIO.write(image, type, file); // save image to temp-file
                        } else if (dependency.isBinary()) {
                            byte[] binaryFile=(byte[])restoredDependencies[i];
                            writeByteArrayToFile(file, binaryFile, false);
                        } else {
                            String text=(String)restoredDependencies[i];                         
                            MotifLabEngine.writeTextFile(file, text);
                        }
                    }
                    newdependencies.add(sharedDependency);                   
                } else { // unshared dependency   
                    file=engine.createTempFileWithSuffix("."+type);                
                    String newfilename=file.getAbsolutePath();
                    OutputDataDependency newdependency=new OutputDataDependency(newfilename);
                    newdependency.setBinary(dependency.isBinary());
                    newdependencies.add(newdependency);                  
                    if (isImage) {
                        BufferedImage image=(BufferedImage)restoredDependencies[i];
                        ImageIO.write(image, type, file); // save image to temp-file
                    } else if (dependency.isBinary()) {
                        byte[] binaryFile=(byte[])restoredDependencies[i];
                        writeByteArrayToFile(file, binaryFile,false);
                    } else {
                        String text=(String)restoredDependencies[i];
                        MotifLabEngine.writeTextFile(file, text);
                    }
                }  
           }
           document=changeDependencies(dependencies, newdependencies);
           dependencies=newdependencies; // replace dependencies with new ones
        }
        restoredDependencies=null; // release resources
    }

    /** Takes the current document and returns a new similar one where the 
     *  old temp-filenames of the dependencies are replaced with new dependencies
     *  temp-filenames
     */
    private PlainDocument changeDependencies(ArrayList<OutputDataDependency> oldlist, ArrayList<OutputDataDependency> newlist) {
        PlainDocument doc=new PlainDocument();   
        String fullText=null;
        try {
            fullText=document.getText(0, document.getLength());
        } catch (BadLocationException e) {System.err.println("SYSTEM ERROR: BadLocationException in OutputData.changeDependencies()");}
        for (int i=0;i<oldlist.size();i++) {
            String oldfilename=oldlist.get(i).getInternalPathName();
            String newfilename=newlist.get(i).getInternalPathName();
            fullText=fullText.replace(oldfilename, newfilename);
        }
        try {
            doc.insertString(0, fullText, null);
        } catch (BadLocationException e) {}
        return doc;
    }

    
    /** A class to serialize images */
    private class SerializableBufferedImage implements Serializable {

        private int width;
        private int height;
        private int[] pixels;
        private int type;

        public SerializableBufferedImage(BufferedImage image) {
            width = image.getWidth();
            height = image.getHeight();
            pixels = new int[width*height];
            type=image.getType();
            image.getRGB(0, 0, width, height, pixels, 0, width);
        }

        public BufferedImage getImage() {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, width, height, pixels, 0, width);
            return image;
        }
        
        private static final long serialVersionUID = 1268297814249427773L;
        
    }

    
    // ---------------------------------------------------------------------------
    
    /* 
     * The methods below are some convenience methods taken from Apache Commons IO
     * to read and write byte arrays from file.
     * They are included here to avoid too many dependencies
     */
    private byte[] toByteArray(InputStream input, int size) throws IOException {
        if (size < 0) throw new IllegalArgumentException("Size must be equal or greater than zero: " + size);       
        if (size == 0) return new byte[0];
        
        byte[] data = new byte[size];
        int offset = 0;
        int readed;

        while (offset < size && (readed = input.read(data, offset, size - offset)) != -1) { // -1 is EOF mark
            offset += readed;
        }
        if (offset != size) throw new IOException("Unexpected readed size. current: " + offset + ", excepted: " + size);      
        return data;
    }  
    
    private void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();        
        } catch (IOException ioe) {}
    } 
     
    private void writeByteArrayToFile(File file, byte[] data, boolean append) throws IOException {
        OutputStream out = null;
        try {
            out = openOutputStream(file, append);
            out.write(data);
            out.close(); // don't swallow close Exception if copy completes normally
        } finally {
            closeQuietly(out);
        }
    }   
    
    private FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) throw new IOException("File '" + file + "' exists but is a directory");         
            if (file.canWrite() == false) throw new IOException("File '" + file + "' cannot be written to");           
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }    
    
    private byte[] readFileToByteArray(File file) throws IOException {
        InputStream in = null;
        try {
            in = openInputStream(file);
            if (file.length() > Integer.MAX_VALUE) throw new IllegalArgumentException("Size cannot be greater than Integer max value: " + file.length());                         
            return toByteArray(in, (int)file.length());
        } finally {
            closeQuietly(in);
        }
    } 
    
    private FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) throw new IOException("File '" + file + "' exists but is a directory");        
            if (file.canRead() == false) throw new IOException("File '" + file + "' cannot be read");          
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }    
     
}   
    
