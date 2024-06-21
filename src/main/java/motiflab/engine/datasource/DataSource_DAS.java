/*
 
 
 */

package motiflab.engine.datasource;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.SystemError;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Region;
import motiflab.engine.data.NumericDataset;

/**
 * A DataSource objects that retrieves data from a DAS server.
 * The retrieved XML-document is then parsed by a DataFormat object
 * 
 * @author Kjetil Klepper
 */
public class DataSource_DAS extends DataSource {

    public static final String PROTOCOL_NAME="DAS";
    
    private String baseURL;
    private String featurename; // template
 
    /**
     * Creates a new instance of DataSource_DAS based on the given arguments
     * @param datatrack
     * @param organism
     * @param genomebuild
     * @param baseURL
     * @param dataFormatName
     * @param featurename
     */
    public DataSource_DAS(DataTrack datatrack, int organism, String genomebuild, String baseURL, String dataFormatName, String featurename) {
        super(datatrack,organism,genomebuild,dataFormatName);
        this.baseURL=baseURL;
        this.featurename=featurename;   
    }
    
    public DataSource_DAS(DataTrack datatrack, int organism, String genomebuild,String dataFormatName) {
        super(datatrack,organism, genomebuild, dataFormatName);       
    }   
    
   
    private DataSource_DAS() {}
    
    public static DataSource_DAS getTemplateInstance() {
        return new DataSource_DAS();
    }    
        
    @Override
    public Class[] getSupportedData() {
        return new Class[]{DNASequenceDataset.class,RegionDataset.class};
    }   
    
//    public static boolean supportsFeatureDataType(Class type) {
//        return (type==DNASequenceDataset.class || type==RegionDataset.class);
//    }
    
    @Override    
    public void initializeDataSourceFromMap(HashMap<String,Object> map) throws SystemError {
        if (!map.containsKey("URL")) throw new SystemError("Missing parameter: URL");      
        this.baseURL=map.get("URL").toString();
        if (map.containsKey("Feature")) this.featurename=map.get("Feature").toString();    
    }    
    
    @Override
    public HashMap<String,Object> getParametersAsMap() {
        HashMap<String,Object> map=new HashMap<String, Object>();
        map.put("URL", baseURL);
        if (featurename!=null && !featurename.isEmpty()) map.put("Feature", featurename);
        return map;
    }       
    
    @Override
    public boolean equals(DataSource other) {
        if (!(other instanceof DataSource_DAS)) return false;
        if (!super.equals(other)) return false;
        if (baseURL==null && ((DataSource_DAS)other).baseURL!=null) return false;
        if (baseURL!=null && ((DataSource_DAS)other).baseURL==null) return false;
        if (baseURL!=null && !baseURL.equals(((DataSource_DAS)other).baseURL)) return false;
        if (featurename==null && ((DataSource_DAS)other).featurename!=null) return false;
        if (featurename!=null && ((DataSource_DAS)other).featurename==null) return false;       
        if (featurename!=null && !featurename.equals(((DataSource_DAS)other).featurename)) return false;
        return true;
    }
    
    @Override
    public String reportFirstDifference(DataSource other) {
        if (!(other instanceof DataSource_DAS)) return "Other is not DataSource_DAS";
        String superstring=super.reportFirstDifference(other);
        if (superstring!=null) return superstring;
        if (!baseURL.equals(((DataSource_DAS)other).baseURL)) return "baseURL: "+baseURL+" vs "+(((DataSource_DAS)other).baseURL);
        if (!featurename.equals(((DataSource_DAS)other).featurename)) return "featurename: "+featurename+" vs "+(((DataSource_DAS)other).featurename);
        return null;
    }    
    
    @Override
    public String getServerAddress() {
        try {
            URL url=new URL(baseURL);
            String host=url.getHost();
            return host;
        } catch (MalformedURLException e) {return null;}
    }
    
    @Override
    public boolean setServerAddress(String serveraddress) { 
        // Replace the host part of this datasource's current baseURL based on the provided serveraddress. 
        // This is mainly used in cloned datasources to try out alternative mirrors
        if (serveraddress.startsWith("http://")) serveraddress=serveraddress.substring("http://".length());
        else if (serveraddress.startsWith("https://")) serveraddress=serveraddress.substring("https://".length());        
        boolean startsWithHTTP=baseURL.startsWith("http://");
        boolean startsWithHTTPS=baseURL.startsWith("https://");
        String newaddress=baseURL;       
        if (startsWithHTTP) newaddress=newaddress.substring("http://".length());
        else if (startsWithHTTPS) newaddress=newaddress.substring("https://".length());        
        int slashpos=newaddress.indexOf("/");
        if (slashpos>=0) {
            String suffix=newaddress.substring(slashpos);
            serveraddress+=suffix;          
        } 
        if (startsWithHTTP) serveraddress="http://"+serveraddress;
        else if (startsWithHTTPS) serveraddress="https://"+serveraddress;        
        baseURL=serveraddress;
        return true;
    }     
    
    @Override
    public String getProtocol() {return PROTOCOL_NAME;}
  
    public String getFeature() {
        return featurename;
    }
    public void setFeature(String feature) {
        this.featurename=feature;
    }
    public String getBaseURL() {
        return baseURL;
    }
    public void setBaseURL(String baseURL) {
        this.baseURL=baseURL;
    }
    
    @Override
    public boolean useCache() {
        return true;
    }    
    
    @Override
    public boolean usesStandardDataFormat() {
        return false;
    }    
    
    @Override
    public DataSegment loadDataSegment(DataSegment segment, ExecutableTask task) throws Exception {        
        int timeout=dataloader.getEngine().getNetworkTimeout();
        String chromosome=segment.getChromosome();
        String start=""+segment.getSegmentStart();
        String end=""+segment.getSegmentEnd();
        if (!(baseURL.startsWith("http://") || baseURL.startsWith("https://"))) baseURL="http://"+baseURL;
        //dataformat=engine.getDataFormat(dataformatName); // this is not used!!!
        int waitperiod=getServerTimeslot();
        if (waitperiod>0) {Thread.sleep(waitperiod);} // throws InterruptedException
        
        Object data=null;
        if (dataTrack.getDataType()==NumericDataset.class) {
            // This is not complete!!! Missing parser for DAS Numeric data. I guess that is OK for now since DAS specification 1 doesn't seem to include support for numeric tracks
            data=null;
            throw new ExecutionError("SLOPPY PROGRAMMING ERROR: DAS protocol has not been implemented for Numeric Data");            
        } else if (dataTrack.getDataType()==RegionDataset.class) {
            String uri=baseURL+"?segment="+chromosome+":"+start+","+end;
            if (featurename!=null && !featurename.isEmpty()) uri+=";type="+featurename;
            DASParser_Region parser=new DASParser_Region();
            ArrayList<Region> list=parser.parse(uri,timeout);            
            for (Region reg:list) { // set correct relative coordinates
                reg.setRelativeStart(reg.getRelativeStart()-segment.getSegmentStart());
                reg.setRelativeEnd(reg.getRelativeEnd()-segment.getSegmentStart());
            }
            data=list;            
        } else if (dataTrack.getDataType()==DNASequenceDataset.class) { 
            String uri=baseURL+"?segment="+chromosome+":"+start+","+end;
            DASParser_DNA parser=new DASParser_DNA();
            char[] buffer=parser.parse(uri,timeout); 
            data=buffer;
        }
        segment.setSegmentData(data);
        return segment;
    }    
    
    @Override
    public DataSource clone() {
        DataSource_DAS copy=new DataSource_DAS(dataTrack, organism, genomebuild, baseURL, dataformatName, featurename);
        copy.delay=this.delay;
        copy.dataformat=this.dataformat;
        if (dataformatSettings!=null) copy.dataformatSettings=(ParameterSettings)this.dataformatSettings.clone();
        return copy;
    }  

    
    @Override
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = super.getXMLrepresentation(document);
        org.w3c.dom.Element protocol=document.createElement("Protocol");
        protocol.setAttribute("type", PROTOCOL_NAME);
        org.w3c.dom.Element url=document.createElement("URL");
        url.setTextContent(baseURL);
        protocol.appendChild(url);
        if (featurename!=null) {
            org.w3c.dom.Element feature=document.createElement("Feature");
            feature.setTextContent(featurename);
            protocol.appendChild(feature);
        }
        element.appendChild(protocol);
        return element;
    }    
    
}
