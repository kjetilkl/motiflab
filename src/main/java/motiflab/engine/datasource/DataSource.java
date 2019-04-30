/*
 
 
 */

package motiflab.engine.datasource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.SystemError;
import motiflab.engine.data.DataSegment;
import motiflab.engine.dataformat.DataFormat;

/**
 *
 * @author Kjetil Klepper 
 */
public abstract class DataSource implements Cloneable {
     public static final String PATTERN_TEMPLATE_CHROMOSOME="$CHROMOSOME";
     public static final String PATTERN_TEMPLATE_START="$START";
     public static final String PATTERN_TEMPLATE_END="$END";
     
     public static final String HTTP_GET="GET";
     public static final String HTTP_POST="POST"; // for future use maybe
     public static final String DAS_SERVER="DAS";
     public static final String FILE_SERVER="FILE";     
     public static final String SQL_SERVER="SQL";        
     public static final String VOID="VOID";
     public static final String TEST="TEST";
               
     protected String name;
     protected DataTrack dataTrack;
     protected int organism;  
     protected int delay=0;
     protected int maxSequenceLength=0; // maximum span of sequence region that can be downloaded in one request from this source    
     protected String genomebuild=null;
     protected String dataformatName=null;
     protected DataFormat dataformat=null;
     protected ParameterSettings dataformatSettings=null; // additional parameters for the DataFormat
     
     protected static DataLoader dataloader=null;
     
     private static int counter=1;
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
     
     /**
      * After the data source has been created with standard parameters (track, organism/build, dataformatName),
      * this method can be called to initialize other settings based on values in the map
      * @param map
      * @throws SystemError 
      */
     public void initializeDataSourceFromMap(HashMap<String,Object> map) throws SystemError {}      
     
    /**
     * Returns the non-standard (i.e. data source type-specific) parameters of this datasource as a Map
     * @return 
     */
     public HashMap<String,Object> getParametersAsMap() { return new HashMap<String, Object>();}     
     
     
     public static void setDataLoader(DataLoader loader) {
         dataloader=loader;
     }
     
     public static String[] getSupportedProtocols() {
         return new String[]{HTTP_GET,DAS_SERVER,SQL_SERVER,FILE_SERVER,VOID};
     }
     
     /** Returns a list of DataSource types (protocols) that support the given feature dataset type */
     public static String[] getDataSourceProtocolsSupportingFeatureDataType(Class type) {
         ArrayList<String> supported=new ArrayList<String>();
         if (DataSource_http_GET.supportsFeatureDataType(type)) supported.add(HTTP_GET);
         if (DataSource_DAS.supportsFeatureDataType(type)) supported.add(DAS_SERVER);
         if (DataSource_SQL.supportsFeatureDataType(type)) supported.add(SQL_SERVER);
         if (DataSource_FileServer.supportsFeatureDataType(type)) supported.add(FILE_SERVER);
         if (DataSource_VOID.supportsFeatureDataType(type)) supported.add(VOID);
         String[] result=new String[supported.size()];
         return supported.toArray(result);
     }
     
     /** Returns a list of DataSource types (protocols) that do not support the given feature dataset type */
     public static String[] getDataSourceProtocolsNotSupportingFeatureDataType(Class type) {        
         ArrayList<String> collection=new ArrayList<String>();
         collection.addAll(Arrays.asList(getSupportedProtocols()));
         String[] supported=getDataSourceProtocolsSupportingFeatureDataType(type);
         collection.removeAll(Arrays.asList(supported));
         String[] result=new String[collection.size()];
         return collection.toArray(result);
     }     
     
     
     /** Returns an array of Feature Dataset classes (DNASequenceDataset.class,
      *  NumericDataset.class, RegionDataset.class) denoting which data types
      *  are supported by this DataSource
      */
     public abstract Class[] getSupportedData();
     
     public DataTrack getDataTrack() {
         return dataTrack;
     }
     
     public String getName() {
         return name;
     }
     
     public int getOrganism() {
         return organism;
     }          
     
     public String getGenomeBuild() {
         return genomebuild;
     }
     
     public void setGenomeBuild(String build) {
         genomebuild=build;
     }
     public void setOrganism(int organism) {
         this.organism=organism;
     }
     
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
     
     public abstract String getProtocol();
     
     /** 
      * Returns the server address (domain name or IP) for the internet server
      * associated with this DataSource (or null)
      */
     public abstract String getServerAddress();
     
     /** 
      * Sets the server address (domain name or IP) for the internet server
      * associated with this DataSource (or null)
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
     * accessing the server for this DataSource. The number is based on the time 
     * of last access (as registered by the a call to notifyDataSourceAccess())
     * and the "delay" property for this DataSource
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
      * @throws motiflab.engine.ExecutionError
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
          
    @Override
    public String toString() {
        return "DataSource["+name+"] for {"+motiflab.engine.data.Organism.getCommonName(organism)+"/"+getGenomeBuild()+"}  "+getProtocol()+","+getDataFormat();
    }
    
    public void debug() {
        System.err.println("   ["+myindex+"] "+motiflab.engine.data.Organism.getCommonName(organism)+"  "+getProtocol()+"   "+getGenomeBuild());        
    }
}
