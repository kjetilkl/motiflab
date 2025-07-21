/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.broad.igv.bbfile.BBFileHeader;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BedFeature;
import org.broad.igv.bbfile.BigBedIterator;


/**
 * This Dataformat parses BigBED files.
 * It makes use of the BigWig API found here:  http://code.google.com/p/bigwig/
 * which is available under the GNU Lesser GPL license
 * @author kjetikl
 */
public class DataFormat_BigBed extends DataFormat {
    private String name="BigBed";

    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    public DataFormat_BigBed() {
        addOptionalParameter("CHR prefix", Boolean.TRUE,  new Boolean[]{ Boolean.TRUE, Boolean.FALSE},"Select this option if the chromosomes in the file are prefixed with 'chr' (e.g. chromosome 12 is named 'chr12' rather than just '12'");                  
        addAdvancedParameter("Custom fields", "", null,"<html>A BigBed file is required to contain at least three fields which are in order: CHROMOSOME, START and END.<br>If a line contains more than three fields, the next fields must be TYPE, SCORE and STRAND (+/-/.).<br>If a line contains more than these six fields, the rest will be regarded as 'custom fields'.<br>MotifLab can read these custom fields and add their values to the region as user-defined properties,<br>but to do that the fields must be identified by supplying a comma-separated list of property names.<br>For example, if the 'Custom fields' parameter is set to \"<tt>count,gene</tt>\", each entry in<br>the BigBed file is expected to have (at least) 8 fields where the 7th field is named \"count\" and<br>the 8th field is named \"gene\". If the name of a custom field is set to \"<tt>*</tt>\" it will be ignored.<br>Thus, if the 'Custom fields' parameter is set to \"<tt>*,gene</tt>\", the value in the 7th field will be ignored<br>but the value in the 8th field will be added to the region as a user-defined property named \"gene\".</html>");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return false;
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return false;
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof RegionSequenceData || data instanceof RegionDataset);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class));
    }

    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "bb";
    }
    @Override
    public String[] getSuffixAlternatives() {return new String[]{"bb","bigbed"};}

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());      
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("System Error: Inappropriate use of parseInput(ArrayList<String>...) method in BigWig format");

    }

    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("System Error: Inappropriate use of parseInput(ArrayList<String>...) method in BigWig format");
    }
    
    @Override
    public boolean canOnlyParseDirectlyFromLocalFile() {
        return true;
    }
    
    @Override
    public DataSegment parseInput(String filename, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        Object result=parseInputToTarget(filename, target, settings, task); 
        return (DataSegment)result;
    }  
    
    @Override
    public Data parseInput(String filename, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        Object result=parseInputToTarget(filename, target, settings, task); 
        return (Data)result;
    }      

    /** The following method is a common substitute for the above 2 methods */
    private Object parseInputToTarget(String filename, Object target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) target=new RegionDataset("temporary");
        if (target instanceof RegionDataset) { // The RegionDataset might not contain RegionSequenceData objects for all Sequences. Add them if they are missing!
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));
            }
        }
        boolean addChr=false;
        String[] format=null;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             addChr=(Boolean)settings.getResolvedParameter("CHR prefix",defaults,engine);               
             String formatString=(String)settings.getResolvedParameter("Custom fields",defaults,engine);
             if (formatString!=null && !formatString.trim().isEmpty()) format=formatString.trim().split("\\s*,\\s*");                         
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           addChr=(Boolean)getDefaultValueForParameter("CHR prefix");            
           String formatString=(String)getDefaultValueForParameter("Custom fields");
           if (formatString!=null && !formatString.trim().isEmpty()) format=formatString.trim().split("\\s*,\\s*");              
        }        
        //open big file       
        BBFileReader reader=null;
        try {
            reader=new BBFileReader(filename);
        } catch (IOException e) {
            throw new ParseError(e.getMessage());
        }
        //get the big header
        BBFileHeader bbFileHdr = reader.getBBFileHeader();
        if(!bbFileHdr.isHeaderOK()) {
            throw new ParseError("Bad header for: "+filename);
        }
        if(!(bbFileHdr.isBigBed())){
            throw new ParseError("Not a valid BigBed file: "+filename);
        }        

        if (target instanceof RegionSequenceData) {
            RegionSequenceData targetSequence=(RegionSequenceData)target;
            String chromosome=targetSequence.getChromosome(); 
            int start=targetSequence.getRegionStart();
            int end=targetSequence.getRegionEnd(); 
            parseSequenceSegment((RegionSequenceData)target, chromosome, start, end, addChr, format, reader,task);
        } else if (target instanceof RegionDataset) { // add values to all applicable sequences (those that cover the genomic region)
            int size=((RegionDataset)target).getNumberofSequences();
            int i=0;
            ArrayList<FeatureSequenceData> sequences=((RegionDataset)target).getAllSequences();
            for (FeatureSequenceData seq:sequences) {
                String chromosome=seq.getChromosome(); 
                int start=seq.getRegionStart();
                int end=seq.getRegionEnd(); 
                parseSequenceSegment((RegionSequenceData)seq, chromosome, start, end, addChr, format, reader,task);
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                if (i%3==0) Thread.yield();
                setProgress(i+1,size);
                i++;                
            }
        } else if (target instanceof DataSegment) {
            String chromosome=((DataSegment)target).getChromosome(); 
            int start=((DataSegment)target).getSegmentStart();
            int end=((DataSegment)target).getSegmentEnd();
            parseSequenceSegment((DataSegment)target, chromosome, start, end, addChr, format, reader,task);
        } else throw new ParseError("SLOPPY PROGRAMMING ERROR: target track for BigBed dataformat not a region track: "+target.getClass().getSimpleName());
        
        try {reader.getBBFis().close();} catch (Exception e) {}        
               
        return target;
    }

    private void parseSequenceSegment(Object target, String chromosome, int start, int end, boolean addChr, String[] format, BBFileReader reader, ExecutableTask task) throws ParseError {
       start--; // subtract 1 because the BED file is 0-indexed
       Set<String> set=new HashSet<String>(reader.getChromosomeNames());
       
       String originalChromosome=chromosome;       
       boolean chromosomeFound = set.contains(chromosome);
              
       if (!chromosomeFound && addChr) { // try different versions of chr-prefix. Search until chromosome is found
           String chromosomeWithoutPrefix=chromosome;
           if (chromosomeWithoutPrefix.toLowerCase().startsWith("chr")) chromosomeWithoutPrefix=chromosomeWithoutPrefix.substring(3); // strip prefix if it is already there
           String[] prefixes=new String[]{"chr","","Chr","CHR"}; // try different versions of the prefix and also without
           for (String prefix:prefixes) {
               chromosome=prefix+chromosomeWithoutPrefix;
               if (set.contains(chromosome)) {
                   chromosomeFound=true;
                   break;
               }
           }   
       }    
          
       if(chromosomeFound){
           HashMap<String, Object> map=new HashMap<String,Object>();
           //BBFileHeader header=reader.getBBFileHeader();
           BigBedIterator iter=null;
           iter=reader.getBigBedIterator(chromosome,start,chromosome,end,false); // the last "false" is necessary to include all regions that overlap with the sequence segment and not just those contained within
           ArrayList<HashMap<String,Object>> children=null;
           boolean warned=false;
           while(iter.hasNext()) {                
                BedFeature f=iter.next();
                if (f==null) {
                    if (!warned) {engine.logMessage("Warning: Iterator in BigBed parser returned NULL");warned=true;}
                    continue;
                }
                map.clear();
                int regionstart=f.getStartBase()+1; // add +1 to convert BED-coordinate to regular MotifLab coordinate system
                int regionend=f.getEndBase();
                double score=0;
                String type=null;    
                String strand=null;
                String[] fields=f.getRestOfFields();
                if (fields!=null && fields.length>=1) type=fields[0];
                if (fields!=null && fields.length>=2) {
                    try {
                        score=Double.parseDouble(fields[1]);
                    } catch (NumberFormatException e) {}
                }
                if (fields!=null && fields.length>=3) strand=fields[2];
                
                map.put("START",regionstart);
                map.put("END",regionend);
                map.put("SCORE",score);
                map.put("TYPE",(type!=null)?type:"Unknown");
                map.put("STRAND",(strand!=null)?strand:".");
                
                String parentID=null; // will be set for nested regions
                
                if (fields!=null && fields.length>=4 && format!=null && format.length>0) { // keep custom fields
                    for (int i=0;i<format.length;i++) {
                        if (3+i>=fields.length) break; // format contains more fields than data line. Do not proceed further
                        String property=format[i];
                        if (property.equals("*")) continue; // skip this
                        String valueAsString=fields[3+i]; // first 3 fields are expected to be: type, score and strand. We start after that
                        valueAsString=MotifLabEngine.percentDecode(valueAsString); // in case values have been percent encoded
                        if (valueAsString.equals(".") || valueAsString.equals("\"\"")) continue; // fields cannot be empty in BED files, but we allow . or "" to denote empty values (which we skip)
                        Object value=null;
                        if (valueAsString.contains(",")) {
                            value=MotifLabEngine.splitOnComma(valueAsString);
                        } else {
                            value=MotifLabEngine.getBasicValueForStringAsObject(valueAsString);
                        }
                        map.put(property,value);
                        if (property.equalsIgnoreCase("PARENT")) {
                             parentID=(value!=null)?value.toString():null; 
                             map.put("_PARENT",parentID); // make it case-insensitive
                        } 
                    }
                }
                if (parentID!=null) { // add a nested region      
                    if (children==null) children=new ArrayList<HashMap<String,Object>>();
                    children.add((HashMap<String,Object>)map.clone());
                } else {
                    addRegionToTarget(target,map);
                }
            }   
            if (children!=null && !children.isEmpty()) { // add child regions after all parents have been added (since they are not necessarily ordered in the file)
                for (HashMap<String,Object> childmap:children) {
                    String parentID=(String)childmap.get("_PARENT");
                    if (parentID==null) continue; 
                    Region parentRegion=getRegionFromTarget(target, parentID);
                    if (parentRegion!=null) {                        
                        if (target instanceof RegionSequenceData) {
                            RegionDataset dataset=(RegionDataset)((RegionSequenceData)target).getParent();
                            if (dataset!=null) dataset.setNestedTrack(true);
                            childmap.put("_PARENT_TARGET_START",((RegionSequenceData)target).getRegionStart());
                        } else if (target instanceof DataSegment) {
                            // the DataSegment will set the nestedTrack property later
                            childmap.put("_PARENT_TARGET_START",((DataSegment)target).getSegmentStart());
                        }
                        addRegionToTarget(parentRegion,childmap);                        
                    }
                    else throw new ParseError("Parent region with ID='"+parentID+"' not found. Note that parents must be listed before their children.");                    
                }
            }
       } else task.setStatusMessage("BigBed file '"+reader.getBBFilePath()+"' doesn't contain chromosome:"+originalChromosome);         
    }
    
    private void addRegionToTarget(Object target, HashMap<String,Object> map) throws ParseError {
        int start=0, end=0; // these are offset relative to the start of the parent sequence or segment
        int targetStart=0;
        if (target instanceof RegionSequenceData) {
            targetStart=((RegionSequenceData)target).getRegionStart();
        } else if (target instanceof DataSegment) {
            targetStart=((DataSegment)target).getSegmentStart();
        } else if (target instanceof Region) {
            targetStart=(Integer)map.get("_PARENT_TARGET_START"); // this should have been set!
        } else throw new ParseError("Target object neither RegionSequenceData nor DataSegment in DataFormat_BigBED.addRegionToTarget():"+target.toString());
        Object startValue=map.get("START");
        if (startValue==null) throw new ParseError("Missing 'start' coordinate");
        if (startValue instanceof Integer) start=(Integer)startValue;        
        Object endValue=map.get("END");
        if (startValue==null) throw new ParseError("Missing 'end' coordinate");
        if (endValue instanceof Integer) end=(Integer)endValue;
        double score=0;
        Object scoreValue=map.get("SCORE");
        if (scoreValue instanceof Double) score=(Double)scoreValue;
        String type=(String)map.get("TYPE"); 
        if (type==null) type="Unknown";
        String annotatedOrientation=(String)map.get("STRAND"); // the orientation in the BED file
        start-=targetStart;
        end-=targetStart;
        int orientation=Sequence.DIRECT;
             if (annotatedOrientation!=null && annotatedOrientation.equals("+")) orientation=Region.DIRECT;
        else if (annotatedOrientation!=null && annotatedOrientation.equals("-")) orientation=Region.REVERSE;
        else orientation=Region.INDETERMINED;

        RegionSequenceData parentSequence=null;
        if (target instanceof RegionSequenceData) parentSequence=(RegionSequenceData)target;
        else if (target instanceof Region) parentSequence=((Region)target).getParent();        
        Region newRegion=new Region(parentSequence, start, end, type, score, orientation);
        // add additional properties to the region
        for (String property:map.keySet()) {
            if (property.equalsIgnoreCase("CHROMOSOME") || property.equalsIgnoreCase("CHR") ||
                property.equalsIgnoreCase("START") ||
                property.equalsIgnoreCase("END") ||
                property.equalsIgnoreCase("SCORE") ||
                property.equalsIgnoreCase("TYPE") ||
                property.equalsIgnoreCase("STRAND") || property.equalsIgnoreCase("ORIENTATION") ||  
                property.equalsIgnoreCase("PARENT") || property.equals("_PARENT") || // this will be implicit anyway
                property.equalsIgnoreCase("_PARENT_TARGET_START") // this is just a temporary transfer of values
            ) continue;
            else {
                Object value=map.get(property);
                if (property.equalsIgnoreCase("ID")) property="ID"; // force uppercase for this property
                if (property.equals("thickStart")) {
                    try {
                        int thickStart=(value instanceof Integer)?((Integer)value):(Integer.parseInt(value.toString()));
                        newRegion.setProperty(property, (thickStart+1));  // Add one to convert from 0-indexed BED format to internal coordinates 
                    }  catch (NumberFormatException e) {}                  
                }
                if ((property.equalsIgnoreCase("thick") || property.equalsIgnoreCase("CDS")) && value instanceof String && ((String)value).contains("-")) { // this will be 0-indexed in the file so we must add 1 to convert to MotifLab coordinates
                    String[] parts=((String)value).split("-");
                    try {
                        int thickStart=Integer.parseInt(parts[0]);
                        int thickEnd=Integer.parseInt(parts[1]);
                        value=(thickStart+1)+"-"+thickEnd; // this will increase the first coordinate by 1
                        newRegion.setProperty("thickStart", thickStart);
                        newRegion.setProperty("thickEnd", thickEnd);
                    } catch (NumberFormatException e) {}
                }
                newRegion.setProperty(property, value);
            }
        }
        // System.err.println("Add region: "+newRegion.toString());
        if (target instanceof RegionSequenceData) ((RegionSequenceData)target).addRegion(newRegion);
        else if (target instanceof DataSegment) ((DataSegment)target).addRegion(newRegion);
        else if (target instanceof Region) { // add nested region
            Object IDstring=newRegion.getProperty("ID"); // this must have been set above or else we will not nest the region!
            if (IDstring==null) throw new ParseError("Missing required ID-property for nested region: "+newRegion.toString());
            ((Region)target).addNestedRegion(IDstring.toString(),newRegion,true);
        }        
    }
    
    
    /** Returns the Region with the given value for its ID-property from the provided target parent
     *  or NULL if no matching region could be found.
     *  @param target This should be a RegionSequenceData or DataSegment
     *  @param ID a unique region identifier string
     */
    private Region getRegionFromTarget(Object target, String id) {
        if (target instanceof RegionSequenceData) {
            RegionSequenceData parent=(RegionSequenceData)target;
            return parent.getFirstMatchingRegion("ID",id);
        } else if (target instanceof DataSegment) {
            DataSegment parent=(DataSegment)target;
            Object data=parent.getData();
            if (data instanceof ArrayList) {
                ArrayList<Region> list=(ArrayList<Region>)data;
                for (Region region:list) {
                    Object regionValue=region.getProperty("ID");
                    if (regionValue!=null) {
                        if (regionValue.equals(id) || (id instanceof String && ((String)id).equalsIgnoreCase(regionValue.toString()))) return region;
                    }                   
                }
            }
        } 
        return null;
    }

}





