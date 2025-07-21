/*
 A Data Source represents a way of obtaining a specific data track for a given genome build via a certain protocol.
 E.g. one data source could fetch a DNA track for hg19 from a DAS server and another data source could get a list of SNP regions for hg38 from an SQL database.
 Subclasses of DataSource implement different "protocols" (ways of obtaining data) such as the HTTP GET protocol, databases access or reading from local files.
 Each object instance of a subclass contains the information/configuration necessary to obtain a specific datatrack (or part of a track) via that protocol for a given organism/genome build.
 */

package org.motiflab.engine.datasource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.swing.Icon;
import org.motiflab.engine.MotifLabResource;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.dataformat.DataFormat;

/**
 *
 * @author Kjetil Klepper 
 */
public abstract class DataSource implements Cloneable, MotifLabResource {
     public static final String PATTERN_TEMPLATE_CHROMOSOME="$CHROMOSOME";
     public static final String PATTERN_TEMPLATE_START="$START";
     public static final String PATTERN_TEMPLATE_END="$END";
     public static final String PATTERN_TEMPLATE_START_ZERO="$STARTZERO";  
               
     protected String name; // The name of the datatrack obtained by this data source
     protected DataTrack dataTrack;
     protected int organism;  
     protected int delay=0;
     protected int maxSequenceLength=0; // maximum span of sequence region that can be downloaded in one request from this source    
     protected String genomebuild=null;
     protected String dataformatName=null;
     protected DataFormat dataformat=null;
     protected ParameterSettings dataformatSettings=null; // additional parameters for the DataFormat used by this source object
     
     protected static DataLoader dataloader=null;
     
     private static int counter=1; // global counter of number of Data Sources (for debugging)
     private int myindex=0;
     
     public DataSource(DataTrack dataTrack, int organism, String genomebuild, String dataformatName) {
         this.dataTrack=dataTrack;
         if (dataTrack!=null) this.name=dataTrack.getName();
         this.organism=organism;
         this.dataformatName=dataformatName;
         this.genomebuild=genomebuild;
         resolveDataFormat();
         myindex=counter++;
     }
     
     /** This constructor is only used to create "template" resources */
     @Deprecated
     public DataSource() {
         myindex=counter++;
     }
     
     /**
      * This method can be used to initialize basic attributes after creating an instance using the zero-argument DataSource() constructor
      * @param dataTrack
      * @param organism
      * @param genomebuild
      * @param dataformatName 
      */
     public void initializeDataSource(DataTrack dataTrack, int organism, String genomebuild, String dataformatName) {
         this.dataTrack=dataTrack;
         if (dataTrack!=null) this.name=dataTrack.getName();
         this.organism=organism;
         this.dataformatName=dataformatName;
         this.genomebuild=genomebuild;
         resolveDataFormat();         
     }
     
     /**
      * After a data source has been created with standard parameters (track, organism/build, dataformatName),
      * this method can be called to initialize other settings based on values in the map.
      * This method is called automatically after creation of the Data Source when loading a configuration from a file/stream.
      * In this case the map is filled in with all key-value attributes found inside the protocol-tag for this Data Source in the XML configuration file.
      * Each child-tag of the protocol tag is one attribute where the child-tag name is the key and the contents of the tag is the value.
      * If the protocol-specific configuration for this data source is more advanced (for instance using nested tags) 
      * the initializeSourceFromXML(protocolElement) method should be used instead of this method to process the XML-file "manually"
      * @param map
      * @throws SystemError 
      */
     public void initializeDataSourceFromMap(HashMap<String,Object> map) throws SystemError {}      
     
     /**
      * After a data source has been created with standard parameters (track, organism/build, dataformatName),
      * this method can be called to initialize other settings based on attributes defined in the "protocol" section of an XML configuration file.
      * The method is an alternative to initializeDataSourceFromMap(map) for data sources that have more complex configuration format needs.
      * Note that both the initializeDataSourceFromMap() and this initializeSourceFromXML() method is called after each other, 
      * and a data source should preferably implement no more than one of these two.
      * @param protocolElement
      * @throws Exception 
      */
     public void initializeSourceFromXML(org.w3c.dom.Element protocolElement) throws Exception {}
     
     
    /**
     * Returns the non-standard (i.e. data source type-specific) parameters of this datasource as a Map
     * @return 
     */
     public HashMap<String,Object> getParametersAsMap() { return new HashMap<String, Object>();}     
     
     
     public static void setDataLoader(DataLoader loader) {
         dataloader=loader;
     }

     
     /** Returns an array of Feature Dataset classes (DNASequenceDataset.class,
      *  NumericDataset.class, RegionDataset.class) denoting which data types
      *  are supported by this DataSource
      *  @return a list of data classes that this Data Source can handle
      */
     public abstract Class[] getSupportedData();
     
     /**
      * Returns True if this Data Source can handle datasets of the given type
      * @param type The type of dataset, e.g. NumericDataset.class, DNASequenceDataset.class or RegionDataset.class
      * @return 
      */
     public boolean supportsFeatureDataType(Class type) {     
         Class[] supported = getSupportedData();
         if (supported==null || supported.length==0) return false;
         for (Class supportedtype:supported) {
             if (supportedtype.isAssignableFrom(type)) return true;
         }
         return false;
     }
     
     
     public DataTrack getDataTrack() {
         return dataTrack;
     }
     
     public void setDataTrack(DataTrack track) {
         this.dataTrack=track;
         if (dataTrack!=null) this.name=dataTrack.getName();
     }     
          
     public String getName() {
         return name;
     }
     
     public void setName(String name) {
         this.name=name;
     }     
     
     public int getOrganism() {
         return organism;
     }   
     
     public void setOrganism(int organism) {
         this.organism=organism;
     }     
     
     public String getGenomeBuild() {
         return genomebuild;
     }
     
     public void setGenomeBuild(String build) {
         genomebuild=build;
     }
     
     /** This should return TRUE if the DataSource relies on a standard data format (such as e.g. FASTA or GTF format)
      *  which means that the track data can be parsed by one of MotifLab*s internal DataFormat classes 
      *  or FALSE if the DataSource uses a proprietary format and must therefore be responsible for parsing its own data 
      */
     public abstract boolean usesStandardDataFormat();
     
     public String getDataFormat() {
         return dataformatName;
     }
     
     public void setDataFormat(String dataformat) {
         this.dataformatName=dataformat;
     }
     
     public void setDataFormatSettings(ParameterSettings dataformatsettings) {
         dataformatSettings=dataformatsettings;
     }

     public ParameterSettings getDataFormatSettings() {
        return dataformatSettings;
     }

     /** Sets the internal DataFormat object reference based on the DataFormat name*/
     protected final DataFormat resolveDataFormat() {
         if (dataformatName==null) return null;
         if (dataloader==null || dataloader.getEngine()==null) return null;
         dataformat=dataloader.getEngine().getDataFormat(dataformatName);
         return dataformat;
     }
 
     
     /** This method can be overridden in subclasses to filter out DataFormats
      *  from the provided list that are not supported by the DataSource type/protocol
      * 
      * @param list
      * @return 
      */
     public ArrayList<DataFormat> filterProtocolSupportedDataFormats(ArrayList<DataFormat> list) {
         return list;
     }
     
    /**
     * Returns TRUE if the argument data source has the same organism
     * and genome build as this source
     * @param other
     * @return 
     */
     public boolean hasSameOrigin(DataSource other) {
        return (organism==other.organism && genomebuild.equals(other.genomebuild));
     }     
     
     /**
      * Returns TRUE if the argument data source is the same as this one
      * (has all the same settings)
      * @param other
      * @return 
      */
     public boolean equals(DataSource other) {
         if (!this.getClass().equals(other.getClass())) return false;
         String thisname=(name!=null)?name:((dataTrack!=null)?dataTrack.getName():null);
         String othername=(other.name!=null)?other.name:((other.dataTrack!=null)?other.dataTrack.getName():null);
         if (thisname!=null && othername==null) return false;
         if (thisname==null && othername!=null) return false;
         if (thisname!=null && othername!=null && !thisname.equals(othername)) return false;
         if (!name.equals(other.name)) return false;
         if (organism!=other.organism) return false;
         if (!genomebuild.equals(other.genomebuild)) return false;
         if (dataformatName!=null && other.dataformatName==null) return false;
         if (dataformatName==null && other.dataformatName!=null) return false;
         if (dataformatName!=null && other.dataformatName!=null && !dataformatName.equals(other.dataformatName)) return false;
         if (maxSequenceLength!=other.maxSequenceLength) return false;
         return true;
     }
     
     public String reportFirstDifference(DataSource other) {
         if (!this.getClass().equals(other.getClass())) return "Class: "+this.getClass()+" vs "+other.getClass();
         String thisname=(name!=null)?name:((dataTrack!=null)?dataTrack.getName():null);
         String othername=(other.name!=null)?other.name:((other.dataTrack!=null)?other.dataTrack.getName():null);
         if (thisname!=null && othername==null) return "track name: "+thisname+" vs "+othername;
         if (thisname==null && othername!=null) return "track name: "+thisname+" vs "+othername;
         if (thisname!=null && othername!=null && !thisname.equals(othername)) return "track name: "+thisname+" vs "+othername;
         if (organism!=other.organism) return "organism: "+organism+" vs "+organism;
         if (!genomebuild.equals(other.genomebuild)) return "build: "+genomebuild+" vs "+genomebuild;
         if (dataformatName!=null && other.dataformatName==null) return "dataformat name: "+thisname+" vs "+othername;
         if (dataformatName==null && other.dataformatName!=null) return "dataformat name: "+thisname+" vs "+othername;
         if (dataformatName!=null && other.dataformatName!=null && !dataformatName.equals(other.dataformatName)) return "dataformat name: "+thisname+" vs "+othername;
         if (maxSequenceLength!=other.maxSequenceLength) return "track maxSequenceLength: "+maxSequenceLength+" vs "+other.maxSequenceLength;
         return null;
     }
     
     /**
      * Returns the name of the protocol that the DataSource subclass implements, e.g. GET, DAS or SQL
      * @return 
      */
     public abstract String getProtocol();
     
     /** 
      * Returns the server address (domain name or IP) for the internet server
      * associated with this DataSource (or null)
      */
     public abstract String getServerAddress();
     
     /** 
      * Sets the server address (domain name or IP) for the internet server
      * associated with this DataSource 
      * If the new address includes a protocol, the new protocol will replace the old.
      * If the new address includes a path-component, this will be used as a path-prefix before the original path.
      * E.g. if the old baseURL is "http://xxx.com/database" and the new server address is "https://www.yyy.org/xcopy",
      * then the new baseURL will be "https://www.yyy.org/xcopy/database".
      * This function is mostly used to swap out the address with alternative mirror-servers for the DataSource if the original server fails to deliver
      */
     public abstract boolean setServerAddress(String address);     
     
     /**
      * If this method returns TRUE, MotifLab should attempt to obtain data
      * from its local cache before requesting the data from this (possibly slow)
      * data source
      * @return 
      */
     public abstract boolean useCache();
     
    
    /** 
     * This method will return the number of milliseconds one should wait before 
     * trying to once more access the server for this DataSource. 
     * The number is based on the time of last access (as registered by the a 
     * call to notifyDataSourceAccess()) and the "delay" property for this DataSource
     */
    
    public synchronized int getServerTimeslot() {
        String serveraddress=getServerAddress();        
        int wait=dataloader.getWaitingPeriodForServer(serveraddress); 
        long timestamp=new Date().getTime();
        timestamp+=wait;
        dataloader.notifyDataSourceAccess(serveraddress, new Date(timestamp));       
        return wait;
    } 

    /**
     * Returns the maximum sequence region (in base pairs) that can be obtained from this server
     * in one single request.
     * @return 
     */
    public int getMaxSequenceSpan() {
        return maxSequenceLength;
    }
    
    /**
     * Sets the maximum sequence region (in base pairs) that can be obtained from this server in one single request.
     */
    public void setMaxSequenceSpan(int span) {
        maxSequenceLength=span;
    }    
    
     /**
      * Obtains data from the the genomic region corresponding to the given (empty) DataSegment object. 
      * The obtained data is stored in the DataSegment object (overwriting any previous content).
      * @param segment
      * @return
      * @throws org.motiflab.engine.ExecutionError
      */
     public abstract DataSegment loadDataSegment(DataSegment segment, ExecutableTask task) throws Exception;

                          
     /** Returns an XML element for this DataSource that can be included in DataConfiguration XML-documents */
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = document.createElement("DataSource");
        element.setAttribute("organism", ""+organism);
        if (genomebuild!=null) element.setAttribute("build", genomebuild);
        if (maxSequenceLength>0) element.setAttribute("maxspan", ""+maxSequenceLength);
        return element;
    }

    @Override
    public abstract DataSource clone();
          
    
    /**
     * This method can be implemented by subclasses other than the 5 "standard protocols" (GET, DAS, SQL, FILE and VOID)
     * to return a GUI panel that can be used to set the properties of the Data Source. The panel should be filled
     * in based on the current settings of the provided data source object
     * @return 
     */    
    public javax.swing.JPanel getConfigurationPanel() {
        return null;
    }
    
    /**
     * This method can be implemented by subclasses other than the 5 "standard protocols" (GET, DAS, SQL, FILE and VOID)
     * to update the configuration of a Data Source based on the provided GUI panel. 
     * Note that the data source does not have to be "this" object
     * @param configPanel A GUI panel that contains the new settings for this data source
     * @throws SystemError if the panel is not recognized or something went wrong when parsing the panel
     */    
    public void updateConfigurationFromPanel(javax.swing.JPanel configPanel) throws SystemError {

    }  
    
    /**
     * This method can be implemented by subclasses other than the 5 "standard protocols" (GET, DAS, SQL, FILE and VOID)
     * to return a documentation string for this Data Source. The string can contain HTML formatting
     * @return a help string
     */    
    public String getHelp() {
        return "";
    }    
    
    
    @Override
    public String toString() {
        return "DataSource["+name+"] for {"+org.motiflab.engine.data.Organism.getCommonName(organism)+"/"+getGenomeBuild()+"}  protocol="+getProtocol()+", format="+getDataFormat();
    }
    
    public void debug() {
        System.err.println("   ["+myindex+"] "+org.motiflab.engine.data.Organism.getCommonName(organism)+"  "+getProtocol()+"   "+getGenomeBuild());        
    }

    
    // -------------------------- Note: Data Sources are registered as MotifLab Resources only for the sake of the different data source "protocols" and not for the individual data sources
    // --------------------------       In other words, only a singleton template (dummy) instance is registered as a resources for each DataSource subclass (protocol), but new DataSources can then be created as needed
    
    @Override
    public String getResourceName() {
        return getProtocol();
    }

    @Override
    public Class getResourceClass() {
        return this.getClass();
    }

    @Override
    public String getResourceTypeName() {
        return "DataSource";
    }

    @Override
    public Icon getResourceIcon() {
        return null;
    }

    @Override
    public Object getResourceInstance() { // note that this returns the singleton template instance for this data source
        return this;
    }
    
    
}
