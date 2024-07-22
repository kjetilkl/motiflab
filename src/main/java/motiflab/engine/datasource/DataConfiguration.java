/*
 
 
 */

package motiflab.engine.datasource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.SystemError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.DNASequenceDataset;
import org.w3c.dom.Node;

/**
 * The DataConfiguration class contains information about the currently
 * registered servers and available data tracks.
 * It contains methods to load and save a configuration from an XML-file
 * and to return lists (or rather maps) of available tracks and servers. 
 * 
 * @author kjetikl
 */
public class DataConfiguration implements Cloneable {
    private HashMap<String,Server> servers; // the key is the serveraddress
    private HashMap<String,DataTrack> availableTracks; // the key is the datatrack name    
    
    public DataConfiguration() {
        servers=new HashMap<String,Server>();
        availableTracks=new HashMap<String,DataTrack>();
    }
    
    public DataConfiguration(HashMap<String,DataTrack> availableTracks, HashMap<String,Server> servers) {
        this.availableTracks=availableTracks;
        this.servers=servers;
    }
    
    /** Returns a datastructure containing the available datatracks (with datasources) */
    public HashMap<String,DataTrack> getAvailableTracks() {
        return availableTracks;
    }
    
    /** Returns a datastructure containing information about particular servers */    
    public HashMap<String,Server> getServers() {
        return servers;
    }
    
    public void setAvailableTracks(HashMap<String,DataTrack> tracks) {
        availableTracks=tracks;
    }
    
    public void addDataTrack(String name, DataTrack track) {
       availableTracks.put(name, track); 
    }
    
    public void setServers(HashMap<String,Server> servers) {
        this.servers=servers;
    }
    
    public void addServer(String name, Server server) {
        servers.put(name, server);
    }    
       
    private Document getXMLrepresentation() throws ParserConfigurationException {
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document document = builder.newDocument();
         Element root=document.createElement("root");
         for (Server server:servers.values()) {
             org.w3c.dom.Element serverelement=server.getXMLrepresentation(document);
             root.appendChild(serverelement);             
         }
         for (DataTrack track:availableTracks.values()) {
             org.w3c.dom.Element trackelement=track.getXMLrepresentation(document);
             root.appendChild(trackelement);
         }
         document.appendChild(root);
         return document;
    }
    
    /**
     * Saves this DataConfiguration in XML format to the given file
     */
    public void saveConfigurationToFile(File configurationfile) throws Exception{
        Document document=getXMLrepresentation();
        TransformerFactory factory=TransformerFactory.newInstance();
        factory.setAttribute("indent-number", new Integer(3));
        Transformer transformer=factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source=new DOMSource(document); 
        OutputStream stream=MotifLabEngine.getOutputStreamForFile(configurationfile);
        StreamResult result=new StreamResult(new OutputStreamWriter(new BufferedOutputStream(stream),"UTF-8"));
        transformer.transform(source, result);
    }

    
    public void loadConfigurationFromFile(File configurationfile) throws SystemError {
        try {
            BufferedInputStream inputstream=new BufferedInputStream(MotifLabEngine.getInputStreamForFile(configurationfile));
            loadConfigurationFromStream(inputstream); 
        }
        catch (FileNotFoundException fnf) {throw new SystemError("File not found error: "+configurationfile.getAbsolutePath());}
        catch (IOException ioe) {throw new SystemError("File error: "+ioe.getMessage());}
        catch (SystemError e) {throw e;}          
    }
    
    /**
     * Initializes this DataConfiguration object based on descriptions in the given XML settings read from a stream
     */
    public void loadConfigurationFromStream(InputStream inputstream) throws SystemError {
        HashMap<String, DataTrack> newTracks=new HashMap<String, DataTrack>();
        HashMap<String, Server> newServers=new HashMap<String, Server>();
        try {
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document doc = builder.parse(inputstream);
         NodeList nodes = doc.getElementsByTagName("Server");
         for (int i=0;i<nodes.getLength();i++) {
            int serverdelay=0; // this should probably have been 3000ms
            int maxSequenceLength=0; //
            Element server = (Element) nodes.item(i);
            String serveraddress=server.getAttribute("address");
            String delayString=server.getAttribute("delay");
            String spanString=server.getAttribute("maxspan");            
            if (delayString!=null && !delayString.isEmpty()) {
               try {serverdelay=Integer.parseInt(delayString);} catch(Exception e){ throw new SystemError("Unable to parse server delay attribute for server="+serveraddress+". Expected integer value, got '"+delayString+"'");} 
            }
            if (spanString!=null && !spanString.isEmpty()) {
               try {maxSequenceLength=Integer.parseInt(spanString);} catch(Exception e){ throw new SystemError("Unable to parse maximum sequence span attribute for server="+serveraddress+". Expected integer value, got '"+spanString+"'");} 
            }            
            Server newserver=new Server(serveraddress, serverdelay);
            newserver.setMaxSequenceLength(maxSequenceLength);
            NodeList mirrornodes = server.getElementsByTagName("Mirror");
            for (int j=0;j<mirrornodes.getLength();j++) {
               Element mirror = (Element) mirrornodes.item(j);
               String mirroraddress=mirror.getTextContent();
               newserver.addMirror(mirroraddress);
            }
            newServers.put(serveraddress,newserver);
         }                  
         nodes = doc.getElementsByTagName("DataTrack");
         for (int i=0;i<nodes.getLength();i++) {
            Element datatrack = (Element) nodes.item(i);
            String datatrackName=datatrack.getAttribute("name");
            String datatypeName=datatrack.getAttribute("type");
            String sourceSite=datatrack.getAttribute("source");
            String description=datatrack.getAttribute("description");
            Class datatype;
                 if (datatypeName.equalsIgnoreCase(DataTrack.NUMERIC_DATA) || datatypeName.equalsIgnoreCase("wiggle")) datatype=NumericDataset.class; // 'wiggle' is a legacy name for numeric data
            else if (datatypeName.equalsIgnoreCase(DataTrack.REGION_DATA)) datatype=RegionDataset.class;
            else if (datatypeName.equalsIgnoreCase(DataTrack.SEQUENCE_DATA)) datatype=DNASequenceDataset.class;
            else throw new SystemError("Unrecognized data type '"+datatypeName+"' for DataTrack="+datatrackName);
            DataTrack track=new DataTrack(datatrackName, datatype, sourceSite, description);
            if (newTracks.containsKey(datatrackName)) throw new SystemError("Duplicate entry for datatrack='"+datatrackName+"' in 'datatracks.txt'");
            newTracks.put(datatrackName,track);
            
            Element displaydirectives = (Element)datatrack.getElementsByTagName("DisplayDirectives").item(0);
            String directives=(displaydirectives!=null)?displaydirectives.getTextContent():null;
            if (directives!=null && directives.trim().isEmpty()) directives=null;
            track.setDisplayDirectivesProtocol(directives);
                       
            NodeList datasources = datatrack.getElementsByTagName("DataSource");
            for (int j=0;j<datasources.getLength();j++) {
                Element datasourcenode = (Element) datasources.item(j);
                String organismString=datasourcenode.getAttribute("organism");
                int organism=0;
                try {
                    organism=Integer.parseInt(organismString);
                } catch (NumberFormatException nfe) {throw new SystemError("Organism specification must be an NCBI Taxonomy ID (integer) for DataSource["+(j+1)+"] for DataTrack="+datatrackName+". Got value="+organismString);}                
                String maxSpanString=datasourcenode.getAttribute("maxspan");
                if (maxSpanString==null || maxSpanString.isEmpty()) maxSpanString="0";
                int maxspan=0;
                try {
                    maxspan=Integer.parseInt(maxSpanString);
                } catch (NumberFormatException nfe) {throw new SystemError("Unable to parse expected numeric value for 'maxspan' attribute for DataSource["+(j+1)+"] for DataTrack="+datatrackName+". Got value="+maxSpanString);}               
                String build=datasourcenode.getAttribute("build");               
                Element protocol = (Element)datasourcenode.getElementsByTagName("Protocol").item(0);
                Element dataformat = (Element)datasourcenode.getElementsByTagName("dataformat").item(0);
                String dataformatName=null;
                String protocoltype=null;
                HashMap<String,Object> parameters=new HashMap<String, Object>();
                if (protocol==null) throw new SystemError("Missing Protocol speficification for DataSource["+(j+1)+"] for DataTrack="+datatrackName);
                else {
                    protocoltype=protocol.getAttribute("type");
                    if (protocoltype==null) throw new SystemError("Missing Protocol Type attribute for DataSource["+(j+1)+"] for DataTrack="+datatrackName);
                     NodeList parametersList=protocol.getChildNodes();
                     for (int p=0;p<parametersList.getLength();p++) {
                         Node param = (Node)parametersList.item(p);
                         if (param instanceof Element) {
                             String paramName=((Element)param).getTagName();
                             String paramValue=((Element)param).getTextContent();
                             parameters.put(paramName, paramValue);
                         }
                     }                    
                }
                ParameterSettings dataformatsettings=null;
                if (dataformat!=null) {
                    dataformatName=dataformat.getAttribute("name");
                    NodeList settingslist = dataformat.getElementsByTagName("setting");
                    if (settingslist.getLength()>0) dataformatsettings=new ParameterSettings();
                    for (int m=0;m<settingslist.getLength();m++) {                                      
                        Element settingnode = (Element) settingslist.item(m);
                        String settingname=settingnode.getAttribute("name"); 
                        String settingvalue=settingnode.getTextContent();
                        dataformatsettings.setParameter(settingname, settingvalue);
                    }
                }
                DataSource datasource=null;

                     if (protocoltype.equals(DataSource_http_GET.PROTOCOL_NAME))  datasource=new DataSource_http_GET(track, organism, build, dataformatName);
                else if (protocoltype.equals(DataSource_DAS.PROTOCOL_NAME)) datasource=new DataSource_DAS(track, organism, build, dataformatName);
                else if (protocoltype.equals(DataSource_FileServer.PROTOCOL_NAME)) datasource=new DataSource_FileServer(track, organism, build, dataformatName);
                else if (protocoltype.equals(DataSource_SQL.PROTOCOL_NAME)) datasource=new DataSource_SQL(track, organism, build);
                else if (protocoltype.equals(DataSource_VOID.PROTOCOL_NAME)) datasource=new DataSource_VOID(track, organism, build);
                else { // this could be a plugin datasource
                    Object ds=MotifLabEngine.getEngine().getResource(protocoltype, "DataSource");
                    if (ds instanceof DataSource) {
                       Class datasourcetype=ds.getClass();
                       datasource=(DataSource)datasourcetype.newInstance();
                       datasource.initializeDataSource(track, organism, build, dataformatName);
                    } else throw new SystemError("Unknown Data Source protocol: "+protocoltype);                    
                }
                
                if (datasource!=null) {
                    try {
                        datasource.initializeDataSourceFromMap(parameters); // a data source could implement this method or the one below to initialize its attributes, but probably not both
                        datasource.initializeSourceFromXML(protocol);       // a data source could implement this method or the one above to initialize its attributes, but probably not both
                    } catch (Exception e) {
                        throw new SystemError("Parameter error for DataTrack ["+datatrackName+"], data source number "+(j+1)+": "+e.getMessage());
                    }                   
                    datasource.setDataFormatSettings(dataformatsettings);                    
                    datasource.setMaxSequenceSpan(maxspan);
                    track.addDataSource(datasource);
                }
                // System.err.println("     DataSource: "+organism+" ["+build+"]["+serverdelay+"] '"+featureName+"'  {"+dataformatName+"}  URL=> "+baseURL);                
            }
         }
       } // -- end try
       catch (Exception e) {
          throw new SystemError("["+e.getClass().getSimpleName()+"] "+e.getMessage());
       }
       this.availableTracks=newTracks;
       this.servers=newServers;
    }    
    
    
    public void loadDefaultConfiguration() throws SystemError {
         loadConfigurationFromStream(this.getClass().getResourceAsStream("/motiflab/engine/datasource/DataTracks.xml"));
    }    
    
    @SuppressWarnings("unchecked")
    @Override
    public DataConfiguration clone() { // makes a deep copy
        HashMap<String,DataTrack> cloned_availableTracks=new HashMap<String,DataTrack>();
        HashMap<String,Server> cloned_servers=new HashMap<String,Server>();
        for (String key:availableTracks.keySet()) {
            DataTrack track=availableTracks.get(key);
            DataTrack clonedtrack=track.clone();
            cloned_availableTracks.put(key, clonedtrack);
        }
        for (String key:servers.keySet()) {
            Server server=servers.get(key);
            Server clonedserver=server.clone();
            cloned_servers.put(key, clonedserver);
        }        
        DataConfiguration copy=new DataConfiguration(cloned_availableTracks,cloned_servers);
        return copy;
    }      
}
