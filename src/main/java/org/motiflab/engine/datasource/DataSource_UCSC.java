package org.motiflab.engine.datasource;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.dataformat.DataFormat;
import static org.motiflab.engine.datasource.DataSource.dataloader;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.motiflab.engine.data.Region;


/**
 * A DataSource object that retrieves data from the UCSC Genome Browser API.
 * The data is obtained in JSON format.
 * 
 * E.g: 
 *  - DNA: https://api.genome.ucsc.edu/getData/sequence?genome=hg38;chrom=chr19;start=10004321;end=10005678
 *  - Region: https://api.genome.ucsc.edu/getData/track?genome=hg38;track=rmsk;chrom=chr1;start=100000000;end=100010000
 *  - Numeric: https://api.genome.ucsc.edu/getData/track?genome=hg38;track=phastCons100way;chrom=chr1;start=100000000;end=100000015;jsonOutputArrays=1
 * 
 * @author Kjetil Klepper
 */
public class DataSource_UCSC extends DataSource {

    public static final String PROTOCOL_NAME="UCSC";    

    private final String serverAddressUCSC = "api.genome.ucsc.edu";
    
    private String baseURL_API;    // these are set with setBaseAPI_URL(server) below
    private String baseURL_DNA;    // these are set with setBaseAPI_URL(server) below    
    private String baseURL_Region; // these are set with setBaseAPI_URL(server) below 
    private String baseURL_Numeric;// these are set with setBaseAPI_URL(server) below
   

    private String trackName = "?"; // name of the track in the UCSC database. E.g. the name of the RepeatMasker track is "rmsk"
    // name of fields to look for in the JSON for various attributes that are needed
    private String fieldname_start = null;
    private String fieldname_end = null;
    private String fieldname_type = null;   
    private String fieldname_strand = null;  
    private String fieldname_score = null;
    private HashMap<String,String> extra_fields=null; // list of extra attributes to include for region tracks

    private boolean keepCaseInDNA = false; // DNA sequences can be in mixed case. Convert to uppercase if this is FALSE    
 
    /**
     * Creates a new instance of DataSource_UCSC based on the given arguments
     * @param datatrack
     * @param organism
     * @param genomebuild
     */
    public DataSource_UCSC(DataTrack datatrack, int organism, String genomebuild) {
        super(datatrack, organism, genomebuild, null);
        setBaseAPI_URL(serverAddressUCSC);
    }
   
    
    private DataSource_UCSC() {
        setBaseAPI_URL(serverAddressUCSC);
    }
    
    public static DataSource_UCSC getTemplateInstance() {
        return new DataSource_UCSC();
    }
    
    private void setBaseAPI_URL(String server) {
        baseURL_API = "https://"+server+"/getData/";
        baseURL_DNA     = baseURL_API+"sequence?genome=$GENOME;chrom="+PATTERN_TEMPLATE_CHROMOSOME+";start="+PATTERN_TEMPLATE_START+";end="+PATTERN_TEMPLATE_END;     
        baseURL_Region  = baseURL_API+"track?genome=$GENOME;track=$TRACK;chrom="+PATTERN_TEMPLATE_CHROMOSOME+";start="+PATTERN_TEMPLATE_START+";end="+PATTERN_TEMPLATE_END;  
        baseURL_Numeric = baseURL_API+"track?genome=$GENOME;track=$TRACK;chrom="+PATTERN_TEMPLATE_CHROMOSOME+";start="+PATTERN_TEMPLATE_START+";end="+PATTERN_TEMPLATE_END+";jsonOutputArrays=1";            
    }

    @Override
    public Class[] getSupportedData() {
        return new Class[]{DNASequenceDataset.class,RegionDataset.class,NumericDataset.class};
    } 
          
    
    @Override
    public void initializeDataSourceFromMap(HashMap<String,Object> map) throws SystemError {
        if (dataTrack.getDataType()==NumericDataset.class || dataTrack.getDataType()==RegionDataset.class) {
            if (!(map.containsKey("track"))) throw new SystemError("Missing parameter: track");          
        } 
        if (dataTrack.getDataType()==RegionDataset.class) {
            if (!(map.containsKey("start") || map.containsKey("end"))) throw new SystemError("Missing parameter: 'start' and/or 'end'");
            if (!(map.containsKey("type"))) throw new SystemError("Missing parameter: type");
            // if (!(map.containsKey("strand"))) throw new SystemError("Missing parameter: strand");
            // if (!(map.containsKey("score"))) throw new SystemError("Missing parameter: score");
        }    
        if (map.containsKey("track")) trackName=map.get("track").toString();else trackName=null;
        if (map.containsKey("start")) fieldname_start=map.get("start").toString();else fieldname_start=null;
        if (map.containsKey("end")) fieldname_end=map.get("end").toString();else fieldname_end=null;
        if (map.containsKey("type")) fieldname_type=map.get("type").toString();else fieldname_type=null;
        if (map.containsKey("strand")) fieldname_strand=map.get("strand").toString();else fieldname_strand=null;
        if (map.containsKey("score")) fieldname_score=map.get("score").toString();else fieldname_score=null;
        if (map.containsKey("keepCaseInDNA")) {
             keepCaseInDNA=Boolean.parseBoolean(map.get("keepCaseInDNA").toString());             
        } else keepCaseInDNA=false;
        if (map.containsKey("extra")) {
            Object value=map.get("extra");
            if (value instanceof HashMap) extra_fields=(HashMap<String,String>)value;
            else if (value instanceof String) extra_fields=parseExtraFields((String)value);
        }
        
    }
    
    @Override
    public HashMap<String,Object> getParametersAsMap() {
        HashMap<String,Object> map=new HashMap<>();
        if (trackName!=null) map.put("track", trackName);
        if (fieldname_start!=null) map.put("start", fieldname_start);
        if (fieldname_end!=null) map.put("start", fieldname_end);
        if (fieldname_type!=null) map.put("start", fieldname_type);
        if (fieldname_strand!=null) map.put("start", fieldname_strand);
        if (fieldname_score!=null) map.put("start", fieldname_score);
        if (keepCaseInDNA) map.put("keepCaseInDNA", keepCaseInDNA);  
        if (extra_fields!=null && !extra_fields.isEmpty()) map.put("extra",getExtraFieldsAsString());         
        return map;
    }    
    
    @Override
    public boolean equals(DataSource other) {
        if (!(other instanceof DataSource_UCSC)) return false;
        if (!super.equals(other)) return false;
        if (!stringsEqual(trackName,((DataSource_UCSC)other).trackName)) return false;
        if (!stringsEqual(fieldname_start,((DataSource_UCSC)other).fieldname_start)) return false;
        if (!stringsEqual(fieldname_end,((DataSource_UCSC)other).fieldname_end)) return false;
        if (!stringsEqual(fieldname_type,((DataSource_UCSC)other).fieldname_type)) return false;
        if (!stringsEqual(fieldname_strand,((DataSource_UCSC)other).fieldname_strand)) return false;
        if (!stringsEqual(fieldname_score,((DataSource_UCSC)other).fieldname_score)) return false;
        if (!mapsEqual(extra_fields,((DataSource_UCSC)other).extra_fields)) return false;
        if (keepCaseInDNA!=((DataSource_UCSC)other).keepCaseInDNA) return false;
        return true;
    }

    private boolean stringsEqual(String s1, String s2) {
        if (s1==null && s2!=null) return false;
        else if (s1!=null && (s2==null || !s1.equals(s2))) return false;
        return true;
    } 
    private boolean mapsEqual(HashMap<String,String> map1, HashMap<String,String> map2) {
        if (map1==null && map2!=null) return false;
        else if (map1!=null && map2==null) return false;
        else if (map1.size()!=map2.size()) return false;        
        else {
            for (String key:map1.keySet()) {
                if (!map2.containsKey(key) || !(map1.get(key).equals(map2.get(key)))) return false;
            }
        }
        return true;
    }     
    
    @Override
    public String reportFirstDifference(DataSource other) {
        if (!(other instanceof DataSource_UCSC)) return "Other is not DataSource_UCSC";
        String superstring=super.reportFirstDifference(other);
        if (superstring!=null) return superstring;
        if (!stringsEqual(trackName,((DataSource_UCSC)other).trackName)) return "track: "+trackName+" vs "+(((DataSource_UCSC)other).trackName);
        if (!stringsEqual(fieldname_start,((DataSource_UCSC)other).fieldname_start)) return "start: "+fieldname_start+" vs "+(((DataSource_UCSC)other).fieldname_start);
        if (!stringsEqual(fieldname_end,((DataSource_UCSC)other).fieldname_end)) return "end: "+fieldname_end+" vs "+(((DataSource_UCSC)other).fieldname_end);
        if (!stringsEqual(fieldname_type,((DataSource_UCSC)other).fieldname_type)) return "type: "+fieldname_type+" vs "+(((DataSource_UCSC)other).fieldname_type);
        if (!stringsEqual(fieldname_strand,((DataSource_UCSC)other).fieldname_strand)) return "strand: "+fieldname_strand+" vs "+(((DataSource_UCSC)other).fieldname_strand);
        if (!stringsEqual(fieldname_score,((DataSource_UCSC)other).fieldname_score)) return "score: "+fieldname_score+" vs "+(((DataSource_UCSC)other).fieldname_score);
        if (!mapsEqual(extra_fields,((DataSource_UCSC)other).extra_fields)) return "extra: "+getExtraFieldsAsString()+" vs "+(((DataSource_UCSC)other).getExtraFieldsAsString());
        if (keepCaseInDNA!=((DataSource_UCSC)other).keepCaseInDNA) return "keepCaseInDNA: "+keepCaseInDNA+" vs "+(((DataSource_UCSC)other).keepCaseInDNA);        
        return null;
    }     
    
    @Override
    public String getServerAddress() {
        return serverAddressUCSC;
    } 
    
    @Override
    public boolean setServerAddress(String serveraddress) { 
        // Replace the host part of this datasource's current baseURL based on the provided serveraddress. 
        // This is mainly used in cloned datasources to try out alternative mirrors
        String host;
        try {
           URL url = new URL(serveraddress); 
           host = url.getHost();
        } catch (MalformedURLException e) {
            host=serveraddress;
            if (!host.contains(".")) return false;
        }         
        setBaseAPI_URL(host);               
        return true;
    }       
    
    @Override
    public String getProtocol() {return PROTOCOL_NAME;}    
    
    public String getParameter(String name) {
             if (name.equalsIgnoreCase("track")) return trackName;
        else if (name.equalsIgnoreCase("start")) return fieldname_start;
        else if (name.equalsIgnoreCase("end")) return fieldname_end;
        else if (name.equalsIgnoreCase("type")) return fieldname_type;
        else if (name.equalsIgnoreCase("strand")) return fieldname_strand;
        else if (name.equalsIgnoreCase("score")) return fieldname_score;
        else if (name.equalsIgnoreCase("keepCaseInDNA")) return (keepCaseInDNA)?"true":"false";
        else if (name.equalsIgnoreCase("extra")) return getExtraFieldsAsString();
        else return null;
    }
    
    public void setParameter(String name, String value) {
             if (name.equalsIgnoreCase("track")) trackName=value;
        else if (name.equalsIgnoreCase("start")) fieldname_start=value;
        else if (name.equalsIgnoreCase("end")) fieldname_end=value;
        else if (name.equalsIgnoreCase("type")) fieldname_type=value;
        else if (name.equalsIgnoreCase("strand")) fieldname_strand=value;
        else if (name.equalsIgnoreCase("score")) fieldname_score=value;
        else if (name.equalsIgnoreCase("keepCaseInDNA")) keepCaseInDNA=(value!=null && value.equalsIgnoreCase("true"));
        else if (name.equalsIgnoreCase("extra")) extra_fields=parseExtraFields(value);
    }

    
    /**
     * Returns the extra fields from the extra_fields map
     * as a comma-separated list of key=value pairs
     * @return 
     */
    private String getExtraFieldsAsString() {
        if (extra_fields==null || extra_fields.isEmpty()) return "";
        StringBuilder builder=new StringBuilder();
        int size = extra_fields.size();
        int index=1;
        for (String key: extra_fields.keySet()) {
            builder.append(key);
            builder.append("=");
            builder.append(extra_fields.get(key));
            if (index<size) builder.append(",");
            index++;
        }  
        return builder.toString();
    }
    
    /**
     * Parses a string of comma-separated key=value pairs and returns
     * the result as a HashMap
     * @return 
     */
    private HashMap<String,String> parseExtraFields(String value) {
        if (value==null || value.trim().isEmpty()) return null;
        HashMap<String,String> map = new HashMap<>();
        String[] attributes=value.trim().split(",");
        for (String attribute:attributes) {
            String[] parts=attribute.split("=");
            if (parts.length==2) {
                map.put(parts[0].trim(),parts[1].trim());
            } else if (parts.length==1) {
                map.put(parts[0].trim(),parts[0].trim()); // use same key and value
            }
        }
        return (map.isEmpty())?null:map;
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
    public ArrayList<DataFormat> filterProtocolSupportedDataFormats(ArrayList<DataFormat> list) {
        return new ArrayList<>(0); // no dataformats needed
    }      
    
    @Override
    public DataSource clone() {
        DataSource_UCSC copy=new DataSource_UCSC(dataTrack, organism, genomebuild);        
        copy.delay=this.delay; // ?!
        copy.trackName = this.trackName;
        copy.fieldname_start = this.fieldname_start;
        copy.fieldname_end = this.fieldname_end; 
        copy.fieldname_type = this.fieldname_type; 
        copy.fieldname_strand = this.fieldname_strand; 
        copy.fieldname_score = this.fieldname_score;        
        copy.keepCaseInDNA = this.keepCaseInDNA;
        if (this.extra_fields!=null) copy.extra_fields = (HashMap<String,String>)this.extra_fields.clone();
        return copy;
    } 

    @Override
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = super.getXMLrepresentation(document);
        org.w3c.dom.Element protocol=document.createElement("Protocol");
        protocol.setAttribute("type", PROTOCOL_NAME);
        
        if ((dataTrack.getDataType()==RegionDataset.class || dataTrack.getDataType()==NumericDataset.class) && trackName!=null) {
            org.w3c.dom.Element trackElement=document.createElement("track");
            trackElement.setTextContent(trackName);
            protocol.appendChild(trackElement);
        }
        if (dataTrack.getDataType()==RegionDataset.class) {
            if (fieldname_start!=null) {
                org.w3c.dom.Element fieldStartElement=document.createElement("start");
                fieldStartElement.setTextContent(fieldname_start);
                protocol.appendChild(fieldStartElement);
            }
            if (fieldname_end!=null) {
                org.w3c.dom.Element fieldEndElement=document.createElement("end");
                fieldEndElement.setTextContent(fieldname_end);
                protocol.appendChild(fieldEndElement);
            }
            if (fieldname_type!=null) {
                org.w3c.dom.Element fieldTypeElement=document.createElement("type");
                fieldTypeElement.setTextContent(fieldname_type);
                protocol.appendChild(fieldTypeElement);
            }     
            if (fieldname_strand!=null) {
                org.w3c.dom.Element fieldStrandElement=document.createElement("strand");
                fieldStrandElement.setTextContent(fieldname_strand);
                protocol.appendChild(fieldStrandElement);
            }
            if (fieldname_score!=null) {
                org.w3c.dom.Element fieldScoreElement=document.createElement("score");
                fieldScoreElement.setTextContent(fieldname_score);
                protocol.appendChild(fieldScoreElement);
            }  
            if (extra_fields!=null && !extra_fields.isEmpty()) {
                org.w3c.dom.Element fieldExtraElement=document.createElement("extra");
                fieldExtraElement.setTextContent(getExtraFieldsAsString());
                protocol.appendChild(fieldExtraElement);
            }     
        }
        if (dataTrack.getDataType()==DNASequenceDataset.class && keepCaseInDNA) {
            org.w3c.dom.Element keepCaseElement=document.createElement("keepCaseInDNA");
            keepCaseElement.setTextContent("true");
            protocol.appendChild(keepCaseElement);
        }       
        element.appendChild(protocol);
        return element;
    }       
    
    @Override
    public DataSegment loadDataSegment(DataSegment segment, ExecutableTask task) throws Exception {
        int timeout=dataloader.getEngine().getNetworkTimeout();
        String chromosome=segment.getChromosome();
        if (!chromosome.startsWith("chr")) chromosome="chr"+chromosome; // some genomes apparently require this
        int start=segment.getSegmentStart();
        int end=segment.getSegmentEnd();
        String apiURL;
       
        
        if (dataTrack.getDataType()==DNASequenceDataset.class) {
            apiURL = baseURL_DNA;
        } else if (dataTrack.getDataType()==RegionDataset.class) {
            apiURL = baseURL_Region;
        } else if (dataTrack.getDataType()==NumericDataset.class) {
            apiURL = baseURL_Numeric;
        } else throw new ExecutionError("Unknown track type for UCSC data source");
        apiURL=apiURL.replace(PATTERN_TEMPLATE_CHROMOSOME, chromosome);    
        apiURL=apiURL.replace(PATTERN_TEMPLATE_START, ""+(start-1)); // convert to 0-indexed BED-format used by UCSC in the URL    
        apiURL=apiURL.replace(PATTERN_TEMPLATE_END, ""+end); // this does not need to be converted since it is "exclusive" in BED-format
        if (trackName!=null) apiURL=apiURL.replace("$TRACK", trackName);  
        if (genomebuild!=null) apiURL=apiURL.replace("$GENOME", genomebuild);  
        URL url;        
        url = new URL(apiURL);
        int waitperiod=getServerTimeslot();
        if (waitperiod>0) {Thread.sleep(waitperiod);} // this can throw InterruptedException
        
        Object data=null;
        if (dataTrack.getDataType()==DNASequenceDataset.class) {
            int expectedLength=end-start+1;
            data=loadDNAdata(url,expectedLength,task);    
        } else if (dataTrack.getDataType()==RegionDataset.class) {
            data=loadRegionData(url,start,task);           
        } else if (dataTrack.getDataType()==NumericDataset.class) {
            int expectedLength=end-start+1;
            data=loadNumericData(url,start,expectedLength,task);    
        }
        segment.setSegmentData(data);
        return segment;
    }
    

    public char[] loadDNAdata(URL url, int length, ExecutableTask task) throws Exception {
        try (InputStream in = url.openStream()) {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(in);

            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) break;

                if (JsonToken.FIELD_NAME.equals(token) && "dna".equals(parser.currentName())) {
                    String dnaString=parser.nextTextValue();
                    if (dnaString.length()!=length) throw new ExecutionError("Received DNA sequence of incorrect length from data source. Expected "+length+" bp but got "+dnaString.length());
                    if (!keepCaseInDNA) dnaString=dnaString.toUpperCase();
                    return dnaString.toCharArray();
                }
            }
        }
        throw new ExecutionError("Did not receive DNA sequence from data source");
    }
    
    /**
     * 
     * @param url The url to use to load data for the segment
     * @param genomicStart The 1-based genomic start of the segment to be returned
     * @param expectedLength The size of the segment
     * @param task
     * @return
     * @throws Exception 
     */
    public double[] loadNumericData(URL url, int genomicStart, int expectedLength, ExecutableTask task) throws Exception {
        // System.err.println("Loading data from: "+url);
        double[] segment = new double[expectedLength];
        int progress=0;
                       
        try (InputStream in = url.openStream()) {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(in);
            String chromosome=null;

            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) break;

                if (JsonToken.FIELD_NAME.equals(token) && "chrom".equals(parser.currentName())) {
                    chromosome = parser.nextTextValue(); // find the name of the field (from "chrom") that contains the data we want
                } else if (JsonToken.FIELD_NAME.equals(token) && parser.currentName().equals(chromosome)) { // This is the data we want                  
                    parser.nextToken(); // move to START_ARRAY of "data"
                    int relativeStart=0;
                    int relativeEnd=0;
                    int start=0; // this is 0-based (read from JSON)
                    int end=0;   // this is 0-based and exclusive (read from JSON)                  
                    double value=0;
                    while (parser.nextToken() != JsonToken.END_ARRAY) { // this next token should either be the start of a nested list (tuple) or the end of the outer list
                        token = parser.nextToken();
                        if (JsonToken.VALUE_NUMBER_INT.equals(token)) start=parser.getValueAsInt();
                        else throw new ExecutionError("Unexpected token in JSON file. Expected integer but got "+token);
                        token = parser.nextToken();
                        if (JsonToken.VALUE_NUMBER_INT.equals(token)) end=parser.getValueAsInt();
                        else throw new ExecutionError("Unexpected token in JSON file. Expected integer but got "+token);
                        token = parser.nextToken();
                        if (JsonToken.VALUE_NUMBER_INT.equals(token) || JsonToken.VALUE_NUMBER_FLOAT.equals(token) ) value=parser.getValueAsDouble();
                        else throw new ExecutionError("Unexpected token in JSON file. Expected numeric value but got "+token);                       
                    
                        start++; // convert to 1-based coordinates. Since "end" is exclusive in UCSC coordinates but inclusive in MotifLab, I don't have to increment it
                        relativeStart=start-genomicStart;
                        relativeEnd=end-genomicStart;
                        if (relativeStart<0 || relativeStart>=expectedLength) throw new ExecutionError("Start coordinate "+start+" is outside segment range ["+genomicStart+"-"+(genomicStart+expectedLength-1)+"]");
                        if (relativeEnd<0 || relativeEnd>=expectedLength) throw new ExecutionError("End coordinate "+end+" is outside segment range ["+genomicStart+"-"+(genomicStart+expectedLength-1)+"]");
                        if (relativeStart==relativeEnd) segment[relativeStart]=value;
                        else { // the coordinates correspond to a range. I don't know if this will ever happen, but just in case...
                            for (int i=relativeStart;i<=relativeEnd;i++) {
                                segment[i]=value;
                            }
                        }
                        
                        parser.nextToken(); // this token should now be the END_ARRAY of the tuple
                        
                        progress++;
                        if (progress>=2000) {
                            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            progress=0;
                        }
                    }
                }
            }
        }
        return segment;
    }    
      
    public ArrayList<Region> loadRegionData(URL url, int genomicStart, ExecutableTask task) throws Exception {
        ArrayList<Region> segment = new ArrayList<>();
        int progress=0;

        try (InputStream in = url.openStream()) {
            JsonFactory factory = new JsonFactory();           
            JsonParser parser = factory.createParser(in);

            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) break;

                if (JsonToken.FIELD_NAME.equals(token) && trackName.equals(parser.currentName())) {
                    token=parser.nextToken(); // move to START_ARRAY of "data"
                    if (!JsonToken.START_ARRAY.equals(token)) throw new ExecutionError("Unexpected token in JSON file. Expected start of array but got "+token);
                    token=parser.nextToken(); // this should now be START_OBJECT or END_ARRAY
                    while (token != JsonToken.END_ARRAY) {                       
                        if (!JsonToken.START_OBJECT.equals(token)) throw new ExecutionError("Unexpected token in JSON file. Expected start of object but got "+token);
                        Integer start=null; 
                        Integer end=null; 
                        String type=null;
                        Double score=null;
                        Integer strand=null;
                        Region region = new Region(null, 0, 0); // the parent is null here but will be set later
                        region.setOrientation(Region.INDETERMINED);
                        region.setScore(1.0); // just a default
                        token=parser.nextToken(); // this should now be FIELD_NAME
                        while (!JsonToken.END_OBJECT.equals(token)) {
                            if (JsonToken.FIELD_NAME.equals(token)) {
                                String field=parser.getValueAsString();
                                token=parser.nextToken(); // this is now the corresponding value
                                if (field.equals(fieldname_start)) {
                                   if (!JsonToken.VALUE_NUMBER_INT.equals(token)) throw new ExecutionError("Unexpected token in JSON file. Expected integer value for region start but got "+token);
                                   start=parser.getValueAsInt()+1; // convert from 0-based UCSC coordinates to 1-based MotifLab coordinates
                                } else if (field.equals(fieldname_end)) {
                                   if (!JsonToken.VALUE_NUMBER_INT.equals(token)) throw new ExecutionError("Unexpected token in JSON file. Expected integer value for region end but got "+token);
                                   end=parser.getValueAsInt();                                  
                                } else if (field.equals(fieldname_type)) {
                                   type=parser.getValueAsString();                                  
                                } else if (field.equals(fieldname_score)) {
                                   if (!(JsonToken.VALUE_NUMBER_INT.equals(token) || JsonToken.VALUE_NUMBER_FLOAT.equals(token))) throw new ExecutionError("Unexpected token in JSON file. Expected numeric value for region score but got "+token);
                                   score=parser.getValueAsDouble();                                   
                                } else if (field.equals(fieldname_strand)) {
                                  String strandString=parser.getValueAsString();
                                       if (strandString.equals("+") || strandString.equals("+1") || strandString.equals("1")) strand=1;
                                  else if (strandString.equals("-") || strandString.equals("-1")) strand=-1;
                                  else strand=0;
                                } else if (extra_fields!=null && extra_fields.containsKey(field)) {
                                    Object extraValue;
                                         if (JsonToken.VALUE_NUMBER_INT.equals(token)) extraValue=parser.getValueAsInt();
                                    else if (JsonToken.VALUE_NUMBER_FLOAT.equals(token)) extraValue=parser.getValueAsDouble();
                                    else if (JsonToken.VALUE_TRUE.equals(token)) extraValue=Boolean.TRUE;
                                    else if (JsonToken.VALUE_FALSE.equals(token)) extraValue=Boolean.FALSE;                                    
                                    else extraValue=parser.getValueAsString();                                       
                                    region.setProperty(field, extraValue);
                                }   
                            } else throw new ExecutionError("Unexpected token in JSON file. Expected name of field but got "+token);
                            token=parser.nextToken(); // this should now either be a new field or the end of the object
                        }
                        // check if the region has all the attributes, then add it to the list if everything is OK
                        if (start==null) throw new ExecutionError("Missing required region attribute: start");
                        if (end==null) throw new ExecutionError("Missing required region attribute: end");
                        if (type==null) throw new ExecutionError("Missing required region attribute: type");
                        // if (strand==null) throw new ExecutionError("Missing required region attribute: strand"); // should this be required?
                        // if (score==null) throw new ExecutionError("Missing required region attribute: score");   // should this be required?                   
                        region.setRelativeStart(start-genomicStart);
                        region.setRelativeEnd(end-genomicStart);
                        region.setType(type);
                        if (score!=null) region.setScore(score);
                        if (strand!=null) region.setOrientation(strand);
                        segment.add(region);
                        
                        token=parser.nextToken(); // this should now be START_OBJECT or END_ARRAY
                        
                        progress++;
                        if (progress>=500) {
                            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            progress=0;
                        }                        
                    }
                }
            }
        }
        return segment;
    }
    
}
