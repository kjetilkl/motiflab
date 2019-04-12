/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Region;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.SequenceCollection;

/**
 * Interactions is a "variable style" format for representing 
 * (long-range) interactions between different chromatin regions (on the same chromosome)
 * The interactions are converted to nested-region tracks
 * @author kjetikl
 */
public class DataFormat_Interactions extends DataFormat {
    private String name="Interactions";

    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    public DataFormat_Interactions() {
        //addAdvancedParameter("Coordinate system", "BED", new String[]{"BED","GFF"},"In the default BED coordinate-system the chromosome starts at position 0 and the end-coordinate is exclusive, whereas in the GFF-system the chromosome starts at 1 and the end-coordinate is inclusive.");
        // addAdvancedParameter("Custom fields", "", null,"<html>See documentation for BigBED format</html>"); // not implemented yet
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof RegionSequenceData || data instanceof RegionDataset);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class));
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
//        if (settings!=null) {
//          try{
//             Parameter[] defaults=getParameters();
//             String addPrefixString=(String)settings.getResolvedParameter("Add CHR prefix",defaults,engine); 
//             String coordinateSystem=(String)settings.getResolvedParameter("Coordinate system",defaults,engine);
//             String formatString=(String)settings.getResolvedParameter("Format",defaults,engine);
//             if (formatString!=null && !formatString.trim().isEmpty()) format=formatString.trim().split("\\s*,\\s*");
//             if (addPrefixString!=null && addPrefixString.equalsIgnoreCase("no")) addPrefix=false;
//             if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
//                 startpos=1;
//                 exclusiveEnd=false;
//             }           
//          } catch (ExecutionError e) {
//             throw e;
//          } catch (Exception ex) {
//              throw new ExecutionError("An error occurred during output formatting", ex);
//          }
//        } else {
//           String addPrefixString=(String)getDefaultValueForParameter("Add CHR prefix");
//           String coordinateSystem=(String)getDefaultValueForParameter("Coordinate system");
//           String formatString=(String)getDefaultValueForParameter("Format");
//           if (formatString!=null && !formatString.trim().isEmpty()) format=formatString.trim().split("\\s*,\\s*");          
//           if (addPrefixString!=null && addPrefixString.equalsIgnoreCase("no")) addPrefix=false;
//           if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
//                 startpos=1;
//                 exclusiveEnd=false;
//           }          
//        }
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
            outputString=outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, addPrefix, startpos, exclusiveEnd, task, engine);
        } else if (dataobject instanceof RegionSequenceData){
            StringBuilder builder=new StringBuilder();
            outputRegions((RegionSequenceData)dataobject, addPrefix, startpos, exclusiveEnd, task, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }


    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, boolean addPrefix, int startpos, boolean exclusiveEnd, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence
              String sequenceName=sequence.getName();
              if (sourcedata!=null) { // output RegionDataset
                  RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
                  outputRegions(sourceSequence, addPrefix, startpos, exclusiveEnd, task, outputString);
              } 
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        return outputString.toString();
    }    
    
    /** output-formats a single region */
    private void outputRegions(RegionSequenceData sequence, boolean addPrefix, int startpos, boolean exclusiveEnd, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
        ArrayList<Region> regionList=sequence.getAllRegions();
        //String sequenceName=sequence.getName();
        String chromosome=sequence.getChromosome();
        if (addPrefix && !chromosome.startsWith("chr")) chromosome="chr"+chromosome;
        //String featureName=sequence.getParent().getName();
        int count=0;
        for (Region region:regionList) {
           ArrayList<Region> children=region.getNestedRegions(false);
           int parts=(children==null)?0:children.size();
           if (parts==0) continue; // not a linked region...  ignore it!
           Collections.sort(children, sequence.getRegionSortOrderComparator()); // just to get the regions properly ordered
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
   
           int thickStart=start;
           int thickEnd=end;        
           Integer thStart=(Integer)region.getPropertyAsType("thickStart", Integer.class);
           Integer thEnd=(Integer)region.getPropertyAsType("thickEnd", Integer.class);
           if (thStart!=null && thEnd!=null) {
                thickStart=thStart;thickEnd=thEnd;
                if (thickEnd<thickStart) {thickStart=start;thickEnd=end;} // ignore if not properly formatted
                else {
                    if (startpos==0) { // use zero-indexed positions instead of one-indexed
                       thickStart--;
                       thickEnd--;
                    }
                    if (exclusiveEnd) thickEnd++;
                }
           } else { // legacy
               Object thick=region.getProperty("thick");
               if (thick==null) thick=region.getProperty("CDS"); // allow this as alternative property
               if (thick instanceof String) { // 
                    int pos=((String)thick).indexOf('-');
                    if (pos<0) pos=((String)thick).indexOf(',');
                    if (pos>0 && pos<((String)thick).length()-1) {
                        try {
                            thickStart=Integer.parseInt(((String)thick).substring(0, pos)); // these are 1-indexed and inclusive in the user-defined parameter
                            thickEnd=Integer.parseInt(((String)thick).substring(pos+1));    // these are 1-indexed and inclusive in the user-defined parameter
                            if (thickEnd<thickStart) {thickStart=start;thickEnd=end;} // ignore if not properly formatted
                            else {
                                if (startpos==0) { // use zero-indexed positions instead of one-indexed
                                   thickStart--;
                                   thickEnd--;
                                }
                                if (exclusiveEnd) thickEnd++;
                            }
                        } catch (Exception e) {thickStart=start;thickEnd=end;}
                    }
               }
           }
                
           boolean includeTypesOfChildred=(true && doChildrenHaveTypes(children));
         
           outputString.append(chromosome);
           outputString.append("\t");
           outputString.append(start);
           outputString.append("\t");
           outputString.append(end);
           outputString.append("\t");
           outputString.append(region.getType());          
           
           outputString.append("\t");
           outputString.append(region.getScore());
           outputString.append("\t");
           outputString.append(strand);
           outputString.append("\t");
           outputString.append(thickStart); // 
           outputString.append("\t");  
           outputString.append(thickEnd);   // 
           outputString.append("\t");      
           outputString.append("0");   // "reserved"
           outputString.append("\t");            
           outputString.append(parts); // "blockCount" 
           outputString.append("\t");      
           
           // output children (or at least their locations)
           for (int i=0;i<parts;i++) {
              Region child=children.get(i);
              outputString.append(child.getLength());
              outputString.append(",");    
           }
           outputString.append("\t");
           for (int i=0;i<parts;i++) {
              Region child=children.get(i);
              int childstart=child.getGenomicStart()-start;
              if (startpos==0) childstart--; // adjust to zero-indexed positions
              outputString.append(childstart);
              outputString.append(",");               
           }           
           if (includeTypesOfChildred) {
               for (int i=0;i<parts;i++) {
                  Region child=children.get(i);                
                  outputString.append(child.getType());
                  outputString.append(",");                    
               }               
           }            
           
           outputString.append("\n");
        }
    }
    
    /** Returns TRUE if all the regions in the provided list have non-null type properties that are not location strings */
    private boolean doChildrenHaveTypes(ArrayList<Region> children) {
        for (Region child:children) {
            String type=child.getType();
            if (type==null || isLocationString(type)) return false;
        }
        return true;
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
        int startpos=0;
        boolean exclusiveEnd=true;
        String[] customfields=null;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             String formatString=(String)settings.getResolvedParameter("Custom fields",defaults,engine);
             if (formatString!=null && !formatString.trim().isEmpty()) customfields=formatString.trim().split("\\s*,\\s*");                         
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           String formatString=(String)getDefaultValueForParameter("Custom fields");
           if (formatString!=null && !formatString.trim().isEmpty()) customfields=formatString.trim().split("\\s*,\\s*");                       
        }     
        int count=0;
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               Thread.yield();
            }
            if (line.startsWith("#") || line.isEmpty()) continue; // assume comment line
            HashMap<String,Object> map=parseSingleLine(line, startpos, exclusiveEnd, customfields);
            String regionchromosome=(String)map.get("CHROMOSOME");
            int regionstart=(Integer)map.get("START"); // These should now be 1-indexed and end-inclusive (like GFF-coordinates)
            int regionend=(Integer)map.get("END");     // These should now be 1-indexed and end-inclusive (like GFF-coordinates)
            RegionSequenceData targetSequence=null;

            if (target instanceof RegionSequenceData) {
               targetSequence=(RegionSequenceData)target;
               if (!targetSequence.getChromosome().equals(regionchromosome)) continue;
               if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
               addRegionToTarget(targetSequence,map);
            } else if (target instanceof RegionDataset) { // add region to all applicable sequences (those that cover the genomic region)
                ArrayList<FeatureSequenceData> sequences=((RegionDataset)target).getAllSequences();
                for (FeatureSequenceData seq:sequences) {
                    targetSequence=(RegionSequenceData)seq;
                    if (!targetSequence.getChromosome().equals(regionchromosome)) continue;
                    if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
                    addRegionToTarget(targetSequence,map);
                }
            } else if (target instanceof DataSegment) {
                addRegionToTarget(target,map);
            } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-Region data as target for BED dataformat: "+target.getClass().getSimpleName());
        
        }        
        return target;
    }


    private Region addRegionToTarget(Object target, HashMap<String,Object> map) throws ParseError {
        int start=0, end=0; // these are offset relative to the start of the parent sequence or segment
        int targetStart=0;
        if (target instanceof RegionSequenceData) {
            targetStart=((RegionSequenceData)target).getRegionStart();
        } else if (target instanceof DataSegment) {
            targetStart=((DataSegment)target).getSegmentStart();
        } else if (target instanceof Region) {
            targetStart=(Integer)map.get("ParentTargetStart");
        } else throw new ParseError("Target object neither RegionSequenceData nor DataSegment in DataFormat_Interactions.addRegionToTarget():"+target.toString());
        Object startValue=map.get("START");
        if (startValue==null) throw new ParseError("Missing 'start' coordinate");
        if (startValue instanceof Integer) start=(Integer)startValue;        
        Object endValue=map.get("END");
        if (startValue==null) throw new ParseError("Missing 'end' coordinate");
        if (endValue instanceof Integer) end=(Integer)endValue;
        double score=0;
        Object scoreValue=map.get("SCORE");
        if (scoreValue instanceof Double) score=(Double)scoreValue;
        String type=null;
        if (map.get("TYPE") instanceof String) type=(String)map.get("TYPE");
        else if (map.get("TYPE") instanceof String[]) {
            type=MotifLabEngine.splice((String[])map.get("TYPE"), ",");
        }
        String[] typeChildren=null;
        if (map.get("TYPE_CHILDREN") instanceof String[]) typeChildren=(String[])map.get("TYPE_CHILDREN");
        
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
        for (String property:map.keySet()) {
            if (property.equalsIgnoreCase("CHROMOSOME") || property.equalsIgnoreCase("CHR") ||
                property.equalsIgnoreCase("FEATURE") ||
                property.equalsIgnoreCase("SOURCE") ||
                property.equalsIgnoreCase("START") ||
                property.equalsIgnoreCase("END") ||
                property.equalsIgnoreCase("SCORE") ||
                property.equalsIgnoreCase("TYPE") || property.equalsIgnoreCase("TYPE_CHILDREN") ||
                property.equalsIgnoreCase("STRAND") || property.equalsIgnoreCase("ORIENTATION") ||                 
                property.equalsIgnoreCase("reserved") ||                  
                property.equalsIgnoreCase("blockSizes") ||
                property.equalsIgnoreCase("chromStarts") ||
                property.equalsIgnoreCase("nestedRegionName") ||
                property.equalsIgnoreCase("ParentTargetStart") ||     
                property.equalsIgnoreCase("linkedRegions")                    
            ) continue;
            else {
                newRegion.setProperty(property, map.get(property));
            }
        }
        // System.err.println("Add region: "+newRegion.toString());
        if (target instanceof RegionSequenceData) ((RegionSequenceData)target).addRegion(newRegion);
        else if (target instanceof DataSegment) ((DataSegment)target).addRegion(newRegion);
        else if (target instanceof Region) { // add nested region
            String nestedRegionName=(String)map.get("nestedRegionName");
            ((Region)target).addNestedRegion(nestedRegionName,newRegion,true);
        } 
        if (map.containsKey("linkedRegions")) { // this is a "top-level" region
            // Add the nested regions
            int parts=(Integer)map.get("linkedRegions");            
            int[] blockSizes=(int[])map.get("blockSizes"); 
            int[] chromStarts=(int[])map.get("chromStarts");
            for (int i=0;i<parts;i++) {
                HashMap<String,Object> childMap=new HashMap<String, Object>();
                childMap.put("CHROMOSOME", map.get("CHROMOSOME"));
                childMap.put("SCORE", map.get("SCORE"));
                childMap.put("STRAND", map.get("STRAND"));
                int childStart=chromStarts[i]; // these are relative to parent region
                int childEnd=childStart+blockSizes[i]-1; // not sure if the end-coordinate here is exlusive or not 
                childMap.put("START", childStart);
                childMap.put("END", childEnd);
                int genomicStart=((Integer)startValue)+chromStarts[i];
                int genomicEnd=genomicStart+blockSizes[i]-1;
                String location=map.get("CHROMOSOME")+":"+genomicStart+"-"+genomicEnd;
                String childType=null;
                if (typeChildren!=null && typeChildren.length==parts) { // 
                    childType=typeChildren[i];
                } else childType=location; // else set the type of the child region to is genomic location
                childMap.put("TYPE", childType);
                childMap.put("nestedRegionName", "Region#"+(i+1));
                childMap.put("ParentTargetStart", -start);
                addRegionToTarget(newRegion,childMap);       
            }           
        }
        return newRegion;
    }

    
    /** parses a single line in a BED-file and returns a HashMap with the different properties (with values as strings!) according to the capturing groups in the formatString */
    private HashMap<String,Object> parseSingleLine(String line, int startpos, boolean exclusiveEnd, String[] customfields) throws ParseError {
        String[] format=new String[]{"chr","start","end","type","score","strand","thickStart","thickEnd","reserved","linkedRegions","blockSizes","chromStarts","typeChildren"};
        HashMap<String,Object> result=new HashMap<String,Object>();
        String[] fields=line.split("\t");
        // if (fields.length<format.length) throw new ParseError("Expected "+format.length+" field(s) per line in BED-format, but found only "+fields.length);
        int start=-1;
        int end=-1;
        int thickstart=-1;
        int thickend=-1;        
        for (int i=0;i<fields.length;i++) {
            if (i>=format.length) break; // no more columns in input
            String property=format[i];
            if (property.equals("*")) continue; // property defined as wildcard. Skip this column!
            String value=fields[i];
            if (property.equalsIgnoreCase("CHROMOSOME") || property.equalsIgnoreCase("CHR")) {
                if (value.startsWith("chr")) value=value.substring(3);
                result.put("CHROMOSOME",value);
            }
            else if (property.equalsIgnoreCase("start")) {
                try {
                    start=Integer.parseInt(value);
                    if (startpos==0) start++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
                    result.put("START",start);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+value);}
            }
            else if (property.equalsIgnoreCase("thickStart")) {
                try {
                    thickstart=Integer.parseInt(value);
                    if (startpos==0) thickstart++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for THICKSTART: "+value);}
            }            
            else if (property.equalsIgnoreCase("end")) {
                try {
                    end=Integer.parseInt(value);
                    if (startpos==0) end++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
                    if (exclusiveEnd) end--; // substract 1 because the end-coordinate in BED-files are non-inclusive. (Note: if (startpos==0 && exclusiveEnd==true) then the end-coordinate is not really changed)
                    result.put("END",end);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+value);}
            }
            else if (property.equalsIgnoreCase("thickEnd")) {
                try {
                    thickend=Integer.parseInt(value);
                    if (startpos==0) thickend++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
                    if (exclusiveEnd) thickend--; // substract 1 because the end-coordinate in BED-files are non-inclusive. (Note: if (startpos==0 && exclusiveEnd==true) then the end-coordinate is not really changed)
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+value);}
            }            
            else if (property.equalsIgnoreCase("type")) {
                Object type=value;
                if (value.contains(",")) type=value.split(",");
                result.put("TYPE",type);
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
            } 
            else if (property.equalsIgnoreCase("blockSizes") || property.equalsIgnoreCase("chromStarts")) {
                int parts=(Integer)result.get("linkedRegions");
                int[] newvalue=parseIntegerList(value,parts);
                result.put(property,newvalue);
            } 
            else if (property.equalsIgnoreCase("linkedRegions")) {
                Object newvalue=parseValue(value);
                if (property.equalsIgnoreCase("linkedRegions")) newvalue=new Integer(((Double)newvalue).intValue());
                result.put(property,newvalue);
            }
            else if (property.equalsIgnoreCase("typeChildren")) {
                if (value.endsWith(",")) value=value.substring(0,value.length()-1); // strip ending comma
                String[] newvalue=value.split(",");
                result.put("TYPE_CHILDREN",newvalue);
            }            
        }
        if (thickstart>=0 && thickend>=thickstart && (thickstart!=start || thickend!=end)) {            
            result.put("thickStart",thickstart); // these are only set if the thick bit differs from the entire region
            result.put("thickEnd",thickend);
        }
        return result;
    }
    
    
    /** Parses a (possibly numeric) value from a string and returns a possibly numeric object or string */
    private Object parseValue(String value) {
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

    private int[] parseIntegerList(String list, int expected) throws ParseError {
        String[] parts=list.trim().split("\\s*,\\s*");
        if (parts.length<expected) throw new ParseError("Expected list with "+expected+" numbers, but got: "+list);
        int[] numbers=new int[expected];
        for (int i=0;i<expected;i++) {
           try {numbers[i]=Integer.parseInt(parts[i]);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected number: "+parts[i]);}
        }
        return numbers;
    }
    
    private boolean isLocationString(String string) {
        return (string.matches("(chr)?(\\w+):(\\d+)-(\\d+)"));
    }

}





