/*
 
 
 */

package motiflab.engine.datasource;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.SystemError;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.RegionDataset;

/**
 * A DataSource objects that retrieves data using a simple HTTP "GET" request to obtain a
 * webpage containing the relevant data. The webpage is then parsed by a DataFormat object
 * (according to the specified dataFormat) to construct a FeatureSequenceData object.
 * Information necessary to obtain the data includes the base-URL that points to the web
 * page from where the data can be obtained and a parameterStringTemplate which specifies
 * all additional settings. The parameter string should contain templates for CHROMOSOME,
 * START and END which will be substituted with correct values for each sequence before the
 * page is downloaded. Thus, the final http-request will look something like this: 
 * "http://baseURL?resolvedParameterString" 
 * 
 * @author Kjetil Klepper
 */
public class DataSource_http_GET extends DataSource {

    private String baseURL;
    private String parameterStringTemplate; // template for the "query" part of the URL
 
    /**
     * Creates a new instance of DataSource_http_GET based on the given arguments
     * @param name
     * @param organism
     * @param baseURL
     * @param dataFormatName
     * @param delay
     * @param parameterStringTemplate
     */
    public DataSource_http_GET(DataTrack datatrack, int organism, String genomebuild, String baseURL, String dataFormatName, String parameterStringTemplate) {
        super(datatrack,organism, genomebuild, dataFormatName);
        this.baseURL=baseURL;
        this.parameterStringTemplate=parameterStringTemplate;        
    }
    
    public DataSource_http_GET(DataTrack datatrack, int organism, String genomebuild,String dataFormatName) {
        super(datatrack,organism, genomebuild, dataFormatName);       
    }    
       
    @Override
    public Class[] getSupportedData() {
        return new Class[]{DNASequenceDataset.class,RegionDataset.class,NumericDataset.class};
    } 
    
    public static boolean supportsFeatureDataType(Class type) {
        return (type==DNASequenceDataset.class || type==RegionDataset.class || type==NumericDataset.class);
    }    
    
    @Override
    public void initializeDataSourceFromMap(HashMap<String,Object> map) throws SystemError{
        if (!map.containsKey("URL")) throw new SystemError("Missing parameter: URL");
        if (!map.containsKey("Parameter")) throw new SystemError("Missing parameter: Parameter");        
        this.baseURL=map.get("URL").toString();
        this.parameterStringTemplate=map.get("Parameter").toString();
    }
    
    @Override
    public HashMap<String,Object> getParametersAsMap() {
        HashMap<String,Object> map=new HashMap<String, Object>();
        map.put("URL", baseURL);
        if (parameterStringTemplate!=null && !parameterStringTemplate.isEmpty()) map.put("Parameter", parameterStringTemplate);
        return map;
    }    
    
    @Override
    public boolean equals(DataSource other) {
        if (!(other instanceof DataSource_http_GET)) return false;
        if (!super.equals(other)) return false;
        if (baseURL==null && ((DataSource_http_GET)other).baseURL!=null) return false;
        if (baseURL!=null && ((DataSource_http_GET)other).baseURL==null) return false;        
        if (baseURL!=null && !baseURL.equals(((DataSource_http_GET)other).baseURL)) return false;
        if (parameterStringTemplate==null && ((DataSource_http_GET)other).parameterStringTemplate!=null) return false;
        if (parameterStringTemplate!=null && ((DataSource_http_GET)other).parameterStringTemplate==null) return false;            
        if (parameterStringTemplate!=null && !parameterStringTemplate.equals(((DataSource_http_GET)other).parameterStringTemplate)) return false;
        return true;
    }    
    
    @Override
    public String reportFirstDifference(DataSource other) {
        if (!(other instanceof DataSource_http_GET)) return "Other is not DataSource_http_GET";
        String superstring=super.reportFirstDifference(other);
        if (superstring!=null) return superstring;
        if (!baseURL.equals(((DataSource_http_GET)other).baseURL)) return "baseURL: "+baseURL+" vs "+(((DataSource_http_GET)other).baseURL);
        if (!parameterStringTemplate.equals(((DataSource_http_GET)other).parameterStringTemplate)) return "featurename: "+parameterStringTemplate+" vs "+(((DataSource_http_GET)other).parameterStringTemplate);
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
        if (serveraddress.startsWith("http://")) serveraddress=serveraddress.substring("http://".length());
        String newaddress=baseURL;
        boolean startsWithHTTP=newaddress.startsWith("http://");
        if (startsWithHTTP) newaddress=newaddress.substring("http://".length());
        int slashpos=newaddress.indexOf("/");
        if (slashpos>=0) {
            String suffix=newaddress.substring(slashpos);
            serveraddress+=suffix;          
        } 
        if (startsWithHTTP) serveraddress="http://"+serveraddress;
        baseURL=serveraddress;
        return true;
    }       
    
    @Override
    public String getProtocol() {return HTTP_GET;}    
    
    public String getParameter() {
        return parameterStringTemplate;
    }
    public void setParameter(String parameter) {
        this.parameterStringTemplate=parameter;
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
    public DataSegment loadDataSegment(DataSegment segment, ExecutableTask task) throws Exception {
        int timeout=dataloader.getEngine().getNetworkTimeout();
        String chromosome=segment.getChromosome();
        String start=""+segment.getSegmentStart();
        String end=""+segment.getSegmentEnd();
        String resolvedParameterString=parameterStringTemplate;
        resolvedParameterString=resolvedParameterString.replace(PATTERN_TEMPLATE_CHROMOSOME, chromosome);
        resolvedParameterString=resolvedParameterString.replace(PATTERN_TEMPLATE_START, start);
        resolvedParameterString=resolvedParameterString.replace(PATTERN_TEMPLATE_END, end); 
        if (!(baseURL.startsWith("http://") || baseURL.startsWith("https://"))) baseURL="http://"+baseURL;
        URL url=new URL(baseURL+"?"+resolvedParameterString);        
        resolveDataFormat(); // sets the 'dataformat' property for this source
        if (dataformat==null) {
            if (dataformatName==null || dataformatName.isEmpty()) throw new ExecutionError("Configuration Error: Data format not specified for source:\n"+this.toString()+"\nSelect 'Configure Datatracks' from the 'Configure' menu to set the data format for this source");
            throw new ExecutionError("Unable to resolve data format '"+dataformatName+"' in "+this.toString());
        }      
        if (dataformat!=null && dataformat.canOnlyParseDirectlyFromLocalFile()) throw new ExecutionError("Data format '"+dataformat.getName()+"' can only be used with local files.");
        int waitperiod=getServerTimeslot();
        if (waitperiod>0) {Thread.sleep(waitperiod);} // this can throw InterruptedException
        ArrayList<String> page=getPage(url, timeout,task);
        if (dataformat!=null) dataformat.parseInput(page,segment,dataformatSettings, task);
        return segment;
    }
    
 
    
    private ArrayList<String> getPage(URL url, int timeout, ExecutableTask task) throws Exception {
        ArrayList<String> document=new ArrayList<String>();
        InputStream inputStream = null;
        BufferedReader dataReader = null;
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(timeout);
        inputStream=connection.getInputStream();
        dataReader = new BufferedReader(new InputStreamReader(inputStream));
        long count=0;
        String line;
        try {
           while ((line=dataReader.readLine())!=null){
              count++;
              if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
              document.add(line);
           }
        } catch (Exception e) {throw e;}
        finally {
            if (dataReader!=null) dataReader.close();
            if (inputStream!=null) inputStream.close();
        }            
        return document;
    }

    @Override
    public DataSource clone() {
        DataSource_http_GET copy=new DataSource_http_GET(dataTrack, organism, genomebuild, baseURL, dataformatName, parameterStringTemplate);
        copy.delay=this.delay;
        copy.dataformat=this.dataformat;
        copy.dataformatSettings=this.dataformatSettings;
        return copy;
    } 

    @Override
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = super.getXMLrepresentation(document);
        org.w3c.dom.Element protocol=document.createElement("Protocol");
        protocol.setAttribute("type", HTTP_GET);
        org.w3c.dom.Element url=document.createElement("URL");
        url.setTextContent(baseURL);
        org.w3c.dom.Element parameter=document.createElement("Parameter");
        parameter.setTextContent(parameterStringTemplate);
        protocol.appendChild(url);
        protocol.appendChild(parameter);
        element.appendChild(protocol);
        resolveDataFormat(); // try to locate data format based on name just in case
        if (dataformat!=null) {
            org.w3c.dom.Element dataformatelement=dataformat.getXMLrepresentation(document,dataformatSettings);
            element.appendChild(dataformatelement);
        }
        return element;
    }        

}
