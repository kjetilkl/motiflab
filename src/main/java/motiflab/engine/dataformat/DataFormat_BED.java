/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Region;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;

import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.SequenceCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_BED extends DataFormat {
    private String name="BED";

    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class, Sequence.class, SequenceCollection.class};

    public DataFormat_BED() {
        addOptionalParameter("Add CHR prefix", "yes", new String[]{"yes","no"},"If selected, the prefix 'chr' will be added before the chromosome number (e.g. chromosome '12' will be output as 'chr12'.");       
        addAdvancedParameter("Coordinate system", "BED", new String[]{"BED","GFF"},"In the default BED coordinate-system the chromosome starts at position 0 and the end-coordinate is exclusive, whereas in the GFF-system the chromosome starts at 1 and the end-coordinate is inclusive.");
        addAdvancedParameter("Format", "", null,"<html>The 'Format' parameter can be used to explicitly specify which properties<br>to include and in which order as a comma-separated list.<br>Standard recognized field codes include: CHROMOSOME,START,END,TYPE,SCORE,STRAND (case-insensitive).<br>Other field codes can be used to refer to user-defined properties (case-sensitive).<br>The special format string '#header' can be used to specify that the first line of the BED file is a header<br>starting with # and containing a TAB- or comma-separated list of field names.<br>The format will then be derived from this header.</html>");
        setParameterFilter("Add CHR prefix","output");       
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof RegionSequenceData || data instanceof RegionDataset || data instanceof Sequence || data instanceof SequenceCollection);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class) || dataclass.equals(Sequence.class) || dataclass.equals(SequenceCollection.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof RegionSequenceData || data instanceof RegionDataset || data instanceof SequenceCollection);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class) || dataclass.equals(SequenceCollection.class));
    }

    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "bed";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);
        boolean addPrefix=true;
        int startpos=0;
        boolean exclusiveEnd=true;
        String[] format=null;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             String addPrefixString=(String)settings.getResolvedParameter("Add CHR prefix",defaults,engine); 
             String coordinateSystem=(String)settings.getResolvedParameter("Coordinate system",defaults,engine);
             String formatString=(String)settings.getResolvedParameter("Format",defaults,engine);
             if (formatString!=null && !formatString.trim().isEmpty()) format=formatString.trim().split("\\s*,\\s*");
             if (addPrefixString!=null && addPrefixString.equalsIgnoreCase("no")) addPrefix=false;
             if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
                 startpos=1;
                 exclusiveEnd=false;
             }           
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           String addPrefixString=(String)getDefaultValueForParameter("Add CHR prefix");
           String coordinateSystem=(String)getDefaultValueForParameter("Coordinate system");
           String formatString=(String)getDefaultValueForParameter("Format");
           if (formatString!=null && !formatString.trim().isEmpty()) format=formatString.trim().split("\\s*,\\s*");          
           if (addPrefixString!=null && addPrefixString.equalsIgnoreCase("no")) addPrefix=false;
           if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
                 startpos=1;
                 exclusiveEnd=false;
           }          
        }
        if (format==null) format=new String[]{"chr","start","end","type","score","strand"};
        String outputString="";
        if (dataobject instanceof RegionDataset) {
            SequenceCollection sequenceCollection=null;
            if (task instanceof OperationTask) {
                String subsetName=(String)((OperationTask)task).getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
                if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
                Data seqcol=engine.getDataItem(subsetName);
                if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
                if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
                sequenceCollection=(SequenceCollection)seqcol;
            }
            if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection();
            outputString=outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, addPrefix, startpos, exclusiveEnd, format, task, engine);
        } else if (dataobject instanceof RegionSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequenceRegionData((RegionSequenceData)dataobject, addPrefix, startpos, exclusiveEnd, format, task, builder);
            outputString=builder.toString();
        } else if (dataobject instanceof SequenceCollection) {
            outputString=outputMultipleSequences(null, (SequenceCollection)dataobject, addPrefix, startpos, exclusiveEnd, format, task, engine);
        } else if (dataobject instanceof Sequence){
            StringBuilder builder=new StringBuilder();
            outputSequence((Sequence)dataobject, addPrefix, startpos, exclusiveEnd, format, task, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, boolean addPrefix, int startpos, boolean exclusiveEnd, String[] format, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence
              String sequenceName=sequence.getName();
              if (sourcedata!=null) { // output RegionDataset
                  RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
                  outputSequenceRegionData(sourceSequence, addPrefix, startpos, exclusiveEnd, format, task, outputString);
              } else { // output Sequence Collection
                  outputSequence(sequence, addPrefix, startpos, exclusiveEnd, format, task, outputString);                
              }
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        return outputString.toString();
    }


    /** output-formats a single sequence */
    private void outputSequenceRegionData(RegionSequenceData sequence, boolean addPrefix, int startpos, boolean exclusiveEnd, String[] format, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
        ArrayList<Region> regionList=sequence.getAllRegions();
        //String sequenceName=sequence.getName();
        String chromosome=sequence.getChromosome();
        if (addPrefix && !chromosome.startsWith("chr")) chromosome="chr"+chromosome;
        //String featureName=sequence.getParent().getName();
        int count=0;
        for (Region region:regionList) {
           count++;
           if (count%200==0) {
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              Thread.yield();
           }
           String strand=".";
           int start=region.getGenomicStart();
           int end=region.getGenomicEnd();
           if (startpos==0) { // use zero-indexed positions instead of one-indexed
               start--;
               end--;
           }
           if (exclusiveEnd) end++; // add 1 because the END-position is non-inclusive.  E.g the range [0,100] includes bases 0 through 99
                if (region.getOrientation()==Region.DIRECT)  strand="+";
           else if (region.getOrientation()==Region.REVERSE) strand="-";
           int counter=0;
           for (String property:format) {
               if (counter>0) outputString.append("\t");
               if (property.equalsIgnoreCase("start")) outputString.append(start);
               else if (property.equalsIgnoreCase("end")) outputString.append(end);
               else if (property.equalsIgnoreCase("chr") || property.equalsIgnoreCase("chromosome")) outputString.append(chromosome);
               else if (property.equalsIgnoreCase("strand") || property.equalsIgnoreCase("orientation")) outputString.append(strand);
               else {
                   Object value=region.getProperty(property);
                   if (value!=null) outputString.append(value.toString());
               }
               counter++;
           }     
           outputString.append("\n");
        }
    }
    
    /** output-formats a single sequence */
    private void outputSequence(Sequence sequence, boolean addPrefix, int startpos, boolean exclusiveEnd, String[] format, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
        String chromosome=sequence.getChromosome();
        if (addPrefix && !chromosome.startsWith("chr")) chromosome="chr"+chromosome;
           String strand=".";
           int start=sequence.getRegionStart();
           int end=sequence.getRegionEnd();
           if (startpos==0) { // use zero-indexed positions instead of one-indexed
               start--;
               end--;
           }
           if (exclusiveEnd) end++; // add 1 because the END-position is non-inclusive.  E.g the range [0,100] includes bases 0 through 99
           String type=sequence.getName();
                if (sequence.getStrandOrientation()==Region.DIRECT)  strand="+";
           else if (sequence.getStrandOrientation()==Region.REVERSE) strand="-";
           int counter=0;
           for (String property:format) {
               if (counter>0) outputString.append("\t");
               if (property.equalsIgnoreCase("start")) outputString.append(start);
               else if (property.equalsIgnoreCase("end")) outputString.append(end);
               else if (property.equalsIgnoreCase("chr") || property.equalsIgnoreCase("chromosome")) outputString.append(chromosome);
               else if (property.equalsIgnoreCase("strand") || property.equalsIgnoreCase("orientation")) outputString.append(strand);
               else if (property.equalsIgnoreCase("type")) outputString.append(type);
               else if (property.equalsIgnoreCase("score")) outputString.append("0");
               else {
                   try {
                       Object value=sequence.getPropertyValue(property,engine);
                       if (value!=null) outputString.append(value.toString());
                   } catch (ExecutionError e) {}                  
               }
               counter++;
           }     
           outputString.append("\n");
        
    }    



    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
         return (Data)parseInputToTarget(input, target, settings, task);
    }

    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        return (DataSegment)parseInputToTarget(input, target, settings, task);
    }

    /** The following method is a common substitute for the above 2 methods */
    private Object parseInputToTarget(ArrayList<String> input, Object target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) target=new RegionDataset("temporary");
        if (target instanceof RegionDataset) { // The RegionDataset might not contain RegionSequenceData objects for all Sequences. Add them if they are missing!
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));
            }
        }
        boolean addPrefix=true;
        int startpos=0;
        boolean exclusiveEnd=true;
        boolean formatFromHeader=false;
        String[] format=null;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             String addPrefixString=(String)settings.getResolvedParameter("Add CHR prefix",defaults,engine);
             String coordinateSystem=(String)settings.getResolvedParameter("Coordinate system",defaults,engine);
             String formatString=(String)settings.getResolvedParameter("Format",defaults,engine);
             if (formatString!=null && !formatString.trim().isEmpty()) {
                 if (formatString.equalsIgnoreCase("#header")) formatFromHeader=true; 
                 else format=formatString.trim().split("\\s*,\\s*");
             }                         
             if (addPrefixString!=null && addPrefixString.equalsIgnoreCase("no")) addPrefix=false;
             if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
                 startpos=1;
                 exclusiveEnd=false;
             }  
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           String addPrefixString=(String)getDefaultValueForParameter("Add CHR prefix");
           String coordinateSystem=(String)getDefaultValueForParameter("Coordinate system");
           String formatString=(String)getDefaultValueForParameter("Format");
           if (formatString!=null && !formatString.trim().isEmpty()) {
                 if (formatString.equalsIgnoreCase("#header")) formatFromHeader=true; 
                 else format=formatString.trim().split("\\s*,\\s*");
           }                      
           if (addPrefixString!=null && addPrefixString.equalsIgnoreCase("no")) addPrefix=false;
           if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
                 startpos=1;
                 exclusiveEnd=false;
           }  
        }
        if (format==null) format=new String[]{"chr","start","end","type","score","strand"};        
        int count=0;
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               Thread.yield();
            }
            if (count==1 && formatFromHeader) {
                if (line.startsWith("#")) line=line.substring(1).trim();
                else throw new ParseError("No header found on first line of BED file", count);
                if (line.contains("\t")) format=line.split("\\t");
                else if (line.contains(",")) format=line.split("\\s*,\\s*");
                else throw new ParseError("Header in BED file does not contain TAB or comma-separated fields", count);
                if (!line.matches(".*\\b(?i:chr(omosome)?)\\b.*")) throw new ParseError("Missing required 'chromosome' field in header", count);             
                if (!line.matches(".*\\b(?i:start)\\b.*")) throw new ParseError("Missing required 'start' field in header", count);             
                if (!line.matches(".*\\b(?i:end)\\b.*")) throw new ParseError("Missing required 'end' field in header", count);             
                continue;
            }
            else if (line.startsWith("#") || line.isEmpty()) continue; // assume comment line
            HashMap<String,Object> map=parseSingleLine(line,startpos,exclusiveEnd, format);
            String regionchromosome=(String)map.get("CHROMOSOME");
            if (regionchromosome==null) throw new ParseError("Unable to parse coordinates for region. Missing chromosome", count);
            if (!(map.containsKey("START"))) throw new ParseError("Unable to parse coordinates for region. Missing START position", count);
            if (!(map.containsKey("END"))) throw new ParseError("Unable to parse coordinates for region. Missing END position", count);
            int regionstart=(Integer)map.get("START"); // These should now be 1-indexed and end-inclusive (like GFF-coordinates)
            int regionend=(Integer)map.get("END");     // These should now be 1-indexed and end-inclusive (like GFF-coordinates)
            RegionSequenceData targetSequence=null;
            if (target instanceof RegionSequenceData) {
               targetSequence=(RegionSequenceData)target;
               if (!targetSequence.getChromosome().equals(regionchromosome)) continue;
               if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
               addRegionToTarget(targetSequence,map,count);
            } else if (target instanceof RegionDataset) { // add region to all applicable sequences (those that cover the genomic region)
                ArrayList<FeatureSequenceData> sequences=((RegionDataset)target).getAllSequences();
                for (FeatureSequenceData seq:sequences) {
                    targetSequence=(RegionSequenceData)seq;
                    if (!targetSequence.getChromosome().equals(regionchromosome)) continue;
                    if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
                    addRegionToTarget(targetSequence,map,count);
                }
            } else if (target instanceof DataSegment) {
                addRegionToTarget(target,map,count);
            } else if (target instanceof SequenceCollection) {
                 String type=(String)map.get("TYPE");
                 String annotatedOrientation=(String)map.get("STRAND"); // the orientation in the BED file
                 if (type==null) type="chr"+regionchromosome+"_"+regionstart+"_"+regionend;
                 int orientation=Sequence.DIRECT;
                     if (annotatedOrientation==null || annotatedOrientation.equals("+")) orientation=Sequence.DIRECT;
                 else if (annotatedOrientation.equals("-")) orientation=Sequence.REVERSE;               
                 Sequence sequence=new Sequence(type, 0, null, regionchromosome, regionstart, regionend, null, null, null, orientation);
                 if (sequence!=null) {
                     String sequencename=sequence.getName();  
                     String fail=engine.checkSequenceNameValidity(sequencename, false);
                     if (fail!=null && engine.autoCorrectSequenceNames()) { // sequencename contains illegal characters that should be corrected  
                        String newsequencename=MotifLabEngine.convertToLegalSequenceName(sequencename);
                        sequence.rename(newsequencename);                                 
                     }                      
                     if (sequence.getSize()>engine.getMaxSequenceLength()) throw new ParseError("Warning: Size of sequence '"+sequencename+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp)", count);
                     else ((SequenceCollection)target).addSequenceToPayload(sequence);
                 }
                 // add additional user-defined properties
                 for (String key:map.keySet()) {
                     if (   key.equalsIgnoreCase("chr") || key.equalsIgnoreCase("chromosome")
                         || key.equalsIgnoreCase("start") || key.equalsIgnoreCase("end")
                         || key.equalsIgnoreCase("type") || key.equalsIgnoreCase("score")
                     ) continue; // these should already be set
                     // sequence.setPropertyValue(key,value); // this should be implemented somehow!
                 }
            } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-Region data as target for BED dataformat: "+target.getClass().getSimpleName(), count);
        }        
        return target;
    }


    private void addRegionToTarget(Object target, HashMap<String,Object> map, int linenumber) throws ParseError {
        int start=0, end=0; // these are offset relative to the start of the parent sequence or segment
        int targetStart=0;
        if (target instanceof RegionSequenceData) {
            targetStart=((RegionSequenceData)target).getRegionStart();
        } else if (target instanceof DataSegment) {
            targetStart=((DataSegment)target).getSegmentStart();
        } else throw new ParseError("Target object neither RegionSequenceData nor DataSegment in DataFormat_BED.addRegionToTarget():"+target.toString(),linenumber);
        Object startValue=map.get("START");
        if (startValue==null) throw new ParseError("Missing 'start' coordinate",linenumber);
        if (startValue instanceof Integer) start=(Integer)startValue;        
        Object endValue=map.get("END");
        if (startValue==null) throw new ParseError("Missing 'end' coordinate",linenumber);
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
        Region newRegion=new Region(parentSequence, start, end, type, score, orientation);
        for (String property:map.keySet()) {
            if (property.equalsIgnoreCase("CHROMOSOME") || property.equalsIgnoreCase("CHR") ||
                property.equalsIgnoreCase("FEATURE") ||
                property.equalsIgnoreCase("SOURCE") ||
                property.equalsIgnoreCase("START") ||
                property.equalsIgnoreCase("END") ||
                property.equalsIgnoreCase("SCORE") ||
                property.equalsIgnoreCase("TYPE") ||
                property.equalsIgnoreCase("STRAND") || property.equalsIgnoreCase("ORIENTATION") 
            ) continue;
            else {
                newRegion.setProperty(property, map.get(property));
            }
        }
        // System.err.println("Add region: "+newRegion.toString());
        if (target instanceof RegionSequenceData) ((RegionSequenceData)target).addRegion(newRegion);
        else if (target instanceof DataSegment) ((DataSegment)target).addRegion(newRegion);
    }


    /** parses a single line in a BED-file and returns a HashMap with the different properties (with values as strings!) according to the capturing groups in the formatString */
    public static HashMap<String,Object> parseSingleLine(String line, int startpos, boolean exclusiveEnd, String[] format) throws ParseError {
        HashMap<String,Object> result=new HashMap<String,Object>();
        String[] fields=line.split("\t");
        if (fields.length==1) throw new ParseError("Line is not in valid BED format: '"+line+"'"); // The (non-empty) line is not TAB separated, so something is probably wrong 
        // if (fields.length<format.length) throw new ParseError("Expected "+format.length+" field(s) per line in BED-format, but found only "+fields.length);
        for (int i=0;i<fields.length;i++) {
            if (i>=format.length) break;
            String property=format[i];
            if (property.equals("*")) continue; // property defined as wildcard. Skip this column!
            String value=fields[i];
            value=value.trim();
            if (property.equalsIgnoreCase("CHROMOSOME") || property.equalsIgnoreCase("CHR")) {
                if (value.startsWith("chr")) value=value.substring(3);
                result.put("CHROMOSOME",value);
            }
            else if (property.equalsIgnoreCase("start")) {
                try {
                    int start=Integer.parseInt(value);
                    if (startpos==0) start++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
                    result.put("START",start);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+value);}
            }
            else if (property.equalsIgnoreCase("end")) {
                try {
                    int end=Integer.parseInt(value);
                    if (startpos==0) end++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
                    if (exclusiveEnd) end--; // substract 1 because the end-coordinate in BED-files are non-inclusive. (Note: if (startpos==0 && exclusiveEnd==true) then the end-coordinate is not really changed)
                    result.put("END",end);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+value);}
            }
            else if (property.equalsIgnoreCase("type")) {
                result.put("TYPE",value);
            }
            else if (property.equalsIgnoreCase("score")) {
                try {
                    result.put("SCORE",Double.parseDouble(value));
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+value);}
            }
            else if (property.equalsIgnoreCase("strand") || property.equalsIgnoreCase("orientation")) {
                String strandString=".";
                if (value.startsWith("+") || value.equals("1") || value.equalsIgnoreCase("direct") || value.equalsIgnoreCase("D")) strandString="+";
                else if (value.startsWith("-") || value.equalsIgnoreCase("reverse") || value.equalsIgnoreCase("r")) strandString="-";
                result.put("STRAND",strandString);
            } else {
                Object newvalue=parseValue(value);
                result.put(property,newvalue);
            }
        }
        return result;
    }
    
    
    /** Parses a (possibly numeric) value from a string and returns a possibly numeric object or string */
    private static Object parseValue(String value) {
        if (value==null) return null;
        try {
            double val=Double.parseDouble(value);
            return new Double(val);
        } catch (NumberFormatException e) {}
        try {
            int val=Integer.parseInt(value);
            return new Integer(val);
        } catch (NumberFormatException e) {}      
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) return Boolean.TRUE;
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) return Boolean.FALSE;
        return value;
    }


}





