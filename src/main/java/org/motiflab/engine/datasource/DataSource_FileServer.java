package org.motiflab.engine.datasource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.dataformat.DataFormat_2bit;
import org.motiflab.engine.dataformat.DataFormat_BED;
import org.motiflab.engine.dataformat.DataFormat_BigBed;
import org.motiflab.engine.dataformat.DataFormat_BigWig;
import org.motiflab.engine.dataformat.DataFormat_FASTA;
import org.motiflab.engine.dataformat.DataFormat_GFF;
import org.motiflab.engine.dataformat.DataFormat_GTF;
import org.motiflab.engine.dataformat.DataFormat_Interactions;
import org.motiflab.engine.dataformat.DataFormat_WIG;

/**
 * The FileServer class relies on data read from files on the local filesystem
 * rather than being obtained from an external network server.
 * The best way to use a FileServer is to link it to a single file in one of
 * the three efficient binary dataformats: BigBed (for Region Datasets), 
 * BigWig (for Numeric Dataset) and 2bit (for DNA sequences). 
 * Support for whole-genome BED, Interactions and GTF files were added later,
 * but files in these format are recommended to use as sources unless they are quite small (at most a few MB).
 * 
 * However, the class also supports a second "legacy" mode in which plain-text
 * full-genome data files in either FASTA, BED or WIG formats (and only these!) may be split into 
 * smaller segment-files in order to access the middle of chromosomes more efficiently
 * (a sort of "poor-man's random access" file). 
 * When the dataset is split across multiple segment files, the filepath should 
 * point to a parent directory containing subdirectories named after each chromosome (e.g. "chr13")
 * and containing files with filenames on the format "chrXXX_nnn.[fasta,wig,bed]"
 * where "XXX" is the chromosome and "nnn" is an integer number denoting the start position of the segment in that file (0-indexed) 
 * The size of each segment file is a parameter of the data format. 
 * If the segment size is 0, it means that the whole genome dataset is contained in a single file.
 * If the segment size is at least as big as the largest chromosome, it means that each chromosome is contained in a single file.
 * If the segment size is greater than 0 but smaller than the largest chromosome, it means that each chromosome is
 * potentially split across multiple files.
 * 
 * @author kjetikl
 */
public class DataSource_FileServer extends DataSource {
    
    public static final String PROTOCOL_NAME="FILE";    
    
    
    private String filepath=""; // For "single-file" sources this should point directly to the file. For split-files, it should point to the directory.
    private int segmentsize=0; // Legacy setting. The size of each segment file (in bp). If the size is 0, the filepath should point directly to a single (unsegmented) file. If the size is larger than 0 the filepath should point to a directory containing split-segment files.
    
    public DataSource_FileServer(DataTrack datatrack, int organism, String genomebuild, String filepath, int segmentsize, String dataFormatName) {
        super(datatrack,organism, genomebuild, dataFormatName);
        this.filepath=filepath;
        this.segmentsize=segmentsize; // a size of 0 is a flag to signal no segmentation. A negative size means the setting is disabled!
    } 
    
    public DataSource_FileServer(DataTrack datatrack, int organism, String genomebuild, String dataFormatName) {
        super(datatrack,organism, genomebuild, dataFormatName);
        this.filepath="";
        this.segmentsize=0;
    }      

    private DataSource_FileServer() {}
    
    public static DataSource_FileServer getTemplateInstance() {
        return new DataSource_FileServer();
    }       
    
    
    @Override
    public void initializeDataSourceFromMap(HashMap<String,Object> map) throws SystemError {
        if (!map.containsKey("Filepath")) throw new SystemError("Missing parameter: Filepath");     
        this.filepath=map.get("Filepath").toString();
        this.segmentsize=0;
        if (map.containsKey("Segmentsize")) {
            int segsize=0;
            Object ss=map.get("Segmentsize");
            if (ss instanceof Number) segsize=((Number)ss).intValue();
            else {
                try {
                    segsize=Integer.parseInt(ss.toString());
                } catch (NumberFormatException e) {throw new SystemError("Expected integer value for parameter 'Segmentsize'. Got '"+ss+"'");}
            }
            this.segmentsize=segsize; // a size of zero is taken as a flag meaning no file segmentation
        } 
    }  
    
    @Override
    public HashMap<String,Object> getParametersAsMap() {
        HashMap<String,Object> map=new HashMap<String, Object>();
        map.put("Filepath", filepath);
        map.put("Segmentsize", new Integer(segmentsize));
        return map;
    }
  
    @Override
    public boolean equals(DataSource other) {
        if (!(other instanceof DataSource_FileServer)) return false;
        if (!super.equals(other)) return false;
        if (filepath==null && ((DataSource_FileServer)other).filepath!=null) return false;
        if (filepath!=null && ((DataSource_FileServer)other).filepath==null) return false;        
        if (filepath!=null && !filepath.equals(((DataSource_FileServer)other).filepath)) return false;
        if (segmentsize!=((DataSource_FileServer)other).segmentsize) return false;
        return true;
    }    
      
    @Override
    public DataSource clone() {
        DataSource_FileServer copy=new DataSource_FileServer(dataTrack, organism, genomebuild, filepath, segmentsize, dataformatName);
        copy.delay=this.delay;
        copy.dataformat=this.dataformat;
        if (dataformatSettings!=null) copy.dataformatSettings=(ParameterSettings)this.dataformatSettings.clone();
        return copy;
    } 

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }
    
    @Override
    public Class[] getSupportedData() {
        return new Class[]{DNASequenceDataset.class,RegionDataset.class,NumericDataset.class};
    }     
     

    @Override
    public String getServerAddress() {
        return filepath;
    }
    
    @Override
    public boolean setServerAddress(String address) {
        return true; // not needed
    }    
    
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String path) {
        this.filepath=path;
    }
    public int getSegmentsize() {
        return segmentsize;
    }
    public void setSegmentsize(int size) {
        this.segmentsize=size;
    }     
    
    @Override
    public boolean useCache() {
        return false; // Never use local caching for file servers, since these servers are probably faster than the cache anyway
    }    

    @Override
    public boolean usesStandardDataFormat() {
        return true;
    }     
    
    @Override
     public ArrayList<DataFormat> filterProtocolSupportedDataFormats(ArrayList<DataFormat> list) { 
         // For now, the FILE DataSource protocol only supports the DataFormats explicitly listed below. 
         // GFF is not supported, because MotifLab expects the first column of GFF to contain a sequence name rather than a chromosome
        Class[] supported = new Class[]{DataFormat_BigBed.class, DataFormat_BigWig.class, DataFormat_2bit.class, DataFormat_FASTA.class, DataFormat_BED.class, DataFormat_GTF.class, DataFormat_Interactions.class, DataFormat_WIG.class}; 
        ArrayList<DataFormat> result=new ArrayList<>();
        for (DataFormat format:list) {
            if (inClassFilter(format, supported)) result.add(format);
        }
        return result;              
    }    
     
    private boolean inClassFilter(Object o, Class[] filter) {
        for (Class c:filter) {
            if (o.getClass()==c) return true;
        }
        return false;
    }     
     
    
    @Override
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = super.getXMLrepresentation(document);
        org.w3c.dom.Element protocol=document.createElement("Protocol");
        protocol.setAttribute("type", PROTOCOL_NAME);
        org.w3c.dom.Element file=document.createElement("Filepath");
        file.setTextContent(filepath);
        protocol.appendChild(file);
        if (segmentsize>0) { // only include segment size if larger than zero
            org.w3c.dom.Element segmentSizeString=document.createElement("Segmentsize");
            segmentSizeString.setTextContent(""+segmentsize);
            protocol.appendChild(segmentSizeString);
        }
        element.appendChild(protocol);
        resolveDataFormat(); // try to locate data format based on name just in case
        if (dataformat!=null) {
            org.w3c.dom.Element dataformatelement=dataformat.getXMLrepresentation(document,dataformatSettings);
            element.appendChild(dataformatelement);
        }
        return element;
    }     
    
    @Override
    public DataSegment loadDataSegment(DataSegment segment, ExecutableTask task) throws Exception {
        String chromosome=segment.getChromosome();
        if (chromosome.equals("?")) throw new ExecutionError("Unknown chromosome");
        int start=segment.getSegmentStart();
        int end=segment.getSegmentEnd();
        Class type=dataTrack.getDataType();
        resolveDataFormat();   
        if (dataformat==null) {
            if (dataformatName==null || dataformatName.isEmpty()) throw new ExecutionError("Configuration Error: Data format not specified for File Server:\n"+this.toString()+"\nSelect 'Configure Datatracks' from the 'Configure' menu to set the data format for this source");
            throw new ExecutionError("Unable to resolve data format '"+dataformatName+"' in "+this.toString());
        }
        if (dataformat.canOnlyParseDirectlyFromLocalFile()) { // Probably 2bit, BigBED or BigWig
            dataformat.parseInput(filepath, segment, dataformatSettings, task);
        } 
        else if (segmentsize==0) { // the whole genome should be in a single file
            dataformat.parseInput(filepath, segment, dataformatSettings, task);
        }
        else { // the whole-genome data is split across multiple files in subdirectories named after each chromosome
            ArrayList<String> page=null;
                 if (type==DNASequenceDataset.class) page=getDNAData(chromosome,start,end, task.getMotifLabEngine());
            else if (type==NumericDataset.class) page=getNumericData(chromosome,start,end, task.getMotifLabEngine());
            else if (type==RegionDataset.class) page=getRegionData(chromosome,start,end, task.getMotifLabEngine());
            else throw new ExecutionError("Unrecognized datatype '"+type+"' in FileServer.loadDataSegment()");
            if (page!=null) dataformat.parseInput(page,segment,dataformatSettings, task);
        }
        return segment;
    } 
    
    private int getIntegerFromString(String input) throws ExecutionError {
       try {
          return Integer.parseInt(input);
       } catch (NumberFormatException e) {
          throw new ExecutionError("Unable to parse expected numeric value: "+input);
       }
    }    
    
    /**
     * Reads DNA data for the given segment from this file server
     * and returns the data as a list of strings
     * @param chromosome
     * @param start
     * @param end
     * @return 
     */   
    private ArrayList<String> getDNAData(String chromosome, int start, int end, MotifLabEngine engine) throws Exception {
        if (segmentsize<=0) throw new ExecutionError("Segment size <= 0 when reading DNA from segmented files");
        ArrayList<String> page=new ArrayList<String>();
        start--;end--; // since the data files are stored as 0-indexed and the requested region is probably 1-indexed,
                       // I will offset the start and end positions by -1        
        int firstSegment=(int)(start/segmentsize);
        int lastSegment=(int)(end/segmentsize);

        if (firstSegment==lastSegment) { // the requested sequence region is within a single file (segment)
            int segmentStart=firstSegment*segmentsize;
            String filename=engine.getFilePath(filepath,"chr"+chromosome+File.separator+"chr"+chromosome+"_"+segmentStart+".fasta"); // filename is in format: basedir/chrX/chrX_nnnn.fasta
            int startOffset=start-segmentStart;
            int endOffset=end-segmentStart;
            int segmentSize=endOffset-startOffset+1;
            //print "filename: start=startOffset, size=segmentSize\n";
            outputSegmentString(filename,startOffset,segmentSize,page);
        } else { // the requested sequence region is split up among multiple files (segments)
          for (int segment=firstSegment;segment<=lastSegment;segment++) {
            int segmentStart=segment*segmentsize;
            String filename=engine.getFilePath(filepath,"chr"+chromosome+File.separator+"chr"+chromosome+"_"+segmentStart+".fasta"); // filename is in format: basedir/chrX/chrX_nnnn.fasta
             if (segment==firstSegment)  { // the requested sequence region starts somewhere in this segment and continues through the end
                 int startOffset=start-segmentStart;
                 int endOffset=end-segmentStart;
                 int segmentSize=endOffset-startOffset+1; // this is not accurate but it does not matter
                 outputSegmentString(filename,startOffset,segmentSize,page);
             } else if (segment==lastSegment) { // the requested sequence region ends somewhere within this segment
                 int startOffset=0; // it continues on from previous segment so we should output from the start
                 int endOffset=end-segmentStart;
                 int segmentSize=endOffset-startOffset+1;
                 outputSegmentString(filename,startOffset,segmentSize,page);
             } else { // internal segment. Output the whole thing
                 outputWholeSegment(filename, page);
             }
          }
        }        
        return page;
    }
    
    /** 
     * reads a segment file and outputs the specified DNA range from start to end relative to segment start (0)
     * @param filename 
     * @param start This is an offset into the segment (which starts at 0)
     * @param seglength The length of the segment to return
     */
    private void outputSegmentString(String filename, int start, int seglength, ArrayList<String> page) throws Exception {
        int read=0; // number of bases read so far in the segment
        int end=start+seglength-1; // this end coordinate is inclusive
        int offset=start;
        BufferedReader inputStream;
        File file=new File(filename);
        inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line=null;
        while((line=inputStream.readLine())!=null) {
            line.trim();
            int linelength=line.length();
            read+=linelength;
            if (start>=read) {offset-=linelength;continue;} // we have not gotten to start position yet. Keep reading
            if (offset>=0) { // segments starts at this line
                //print "SEGMENT starts here. offset=offset!\n";
                if (offset+seglength>=linelength) {page.add(line.substring(offset));} // output rest of this line. From offset onwards
                else {page.add(line.substring(offset,offset+seglength));} // segment also ends at this line
            } else { // segment does not start at this line, but continues on this line. Output from the beginning of the line
                //print "SEGMENT does not start here. offset=offset, read=read, end=end. Ends on this line=".(read>end)." after ".(end%(read-linelength))."\n";
                if (read>end) {page.add(line.substring(0,end%(read-linelength)+1));} // segment ends at this line
                else {page.add(line);}  // segment neither starts nor ends within this line. Output the full line!
            }
            offset-=linelength;
            if (read>end) {break;} // we have passed the end of requested segment and are finished
        }
        inputStream.close();
    }    
    
    /** Reads a file and adds it contents to the the array */
    private void outputWholeSegment(String filename, ArrayList<String> page) throws Exception {
        BufferedReader inputStream;
        File file=new File(filename);
        inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line=null;
        while((line=inputStream.readLine())!=null) {
            page.add(line);
        }
        inputStream.close();
    }    
    
    
    /**
     * Reads numeric data for the given segment from this file server
     * and returns the data as a list of strings
     * @param chromosome
     * @param start
     * @param end
     * @return 
     */   
    private ArrayList<String> getNumericData(String chromosome, int start, int end, MotifLabEngine engine) throws Exception  {
        if (segmentsize<=0) throw new ExecutionError("Segment size <= 0 when reading numeric data from segmented files");
        ArrayList<String> page=new ArrayList<String>();
        String trackname=dataTrack.getName();        
        int filenum=1;
        int filestart=1;
        int nextfilestart=segmentsize;

        if (start>=segmentsize) {
            filenum=(int)(start/segmentsize);
            filestart=filenum*segmentsize;
            nextfilestart=filestart+segmentsize;
        }
        int linecount=0;
        int filelinecount=0;
        String filename=engine.getFilePath(filepath,"chr"+chromosome+File.separator+"chr"+chromosome+"_"+filestart+".wig"); // filename is in format: basedir/chrX/chrX_nnnn.wig
        int currentfilepos=filestart;
        
        BufferedReader inputStream;
        File file=new File(filename);
        inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        page.add("track type=wiggle_0 name="+trackname+" description=\""+trackname+"\" visibility=2\n");
        page.add("fixedStep chrom=chr"+chromosome+" start="+start+" step=1\n");
        int count=0;
        String value="";
        OUTER: while (currentfilepos<=end) {
            String line=inputStream.readLine();
            linecount++;filelinecount++;
            if (line==null) throw new ExecutionError("Reached end of file sooner than expected. Perhaps the 'Segmentsize' parameter for the file server is to large?");                       
            String[] elements=line.split("\\t");
            value=elements[0];
            if (elements.length>1) {count=getIntegerFromString(elements[1]);} else {count=1;}
            while (count>0) {
                if (currentfilepos>=start) {
                    if (value.isEmpty()) {page.add("0");} else {page.add(value);}
                }
                if (currentfilepos==end) {break OUTER;} // finished
                currentfilepos++;
                count--; // count down
            }
            if (currentfilepos==nextfilestart && currentfilepos<=end) {
                inputStream.close();
                filestart=nextfilestart;
                nextfilestart+=segmentsize;
                filename=engine.getFilePath(filepath,"chr"+chromosome+File.separator+"chr"+chromosome+"_"+filestart+".wig"); //
                file=new File(filename);
                if (file.exists()) {
                    inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    filelinecount=0;
                } else { // The input file does not exist (it is probably empty anyway). Just output a lot of zeros
                     int dummycount=end-currentfilepos+1;
                     for (int di=0;di<dummycount;di++) {
                        page.add("0");
                     }
                }
            }
        } // OUTER
        inputStream.close();     
        return page;
    }
    


    /**
     * Reads region data for the given segment from this file server
     * and returns the data as a list of strings
     * @param chromosome
     * @param start
     * @param end
     * @return 
     */   
    private ArrayList<String> getRegionData(String chromosome, int start, int end, MotifLabEngine engine) throws Exception  {
        if (segmentsize<=0) throw new ExecutionError("Segment size <= 0 when reading region data from segmented files");        
        start--; // since this script gets its data from BED-files (which are 0-indexed) whereas the requested region is probably 1-indexed
                 // I will offset the region by 1 base upstream just to make sure I return the whole range requested by the user.
                 // The actual handling of the coordinates will be handled later by the dataformat parser anyway.
        int splitsize=(segmentsize<=0)?Integer.MAX_VALUE:segmentsize;
        int firstSegment=(int)(start/splitsize);
        int lastSegment=(int)(end/splitsize);
        BufferedReader inputStream;
        ArrayList<String> page=new ArrayList<String>();
        for (int segment=firstSegment;segment<=lastSegment;segment++) {
            int segmentStart=segment*splitsize;
            String filename=engine.getFilePath(filepath,"chr"+chromosome+File.separator+"chr"+chromosome+"_"+segmentStart+".bed"); // filename is in format: basedir/chrX/chrX_nnnn.bed
            File file=new File(filename);
            if (!file.exists()) continue; // The file does not exist, which means there are no regions in this segment. Just skip it
            inputStream=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while((line=inputStream.readLine())!=null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] fields=line.split("\\t");
                if (fields.length<3) throw new ExecutionError("Unable to parse line in BED-format from file server: "+line);
                int regionstart=getIntegerFromString(fields[1]);
                int regionend=getIntegerFromString(fields[2]);
                if (regionend>=start && regionstart<=end) { // region overlaps with requested segment => output region
                   if (regionstart<segmentStart && segment>firstSegment) {continue;} // This region was output as part of previous segment, so skip it here
                   page.add(line);
                }
                if (segment==lastSegment && regionstart>end) {break;} // If the contents of the segment files is sorted, then no more regions will overlap the requested segment
            }
            inputStream.close();
        }
        return page;
    } 
    
    
    
    
}
