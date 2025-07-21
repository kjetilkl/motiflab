/*


 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.DataSegment;
/**
 *
 * @author kjetikl
 */
public class DataFormat_FIMO extends DataFormat {
    private String name="FIMO";
    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE="Relative";
    private static final String OPPOSITE="Opposite";


    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    public DataFormat_FIMO() {
        addParameter("Orientation", RELATIVE,new String[]{DIRECT,REVERSE,RELATIVE,OPPOSITE},"Orientation of relative coordinates (only applicable if Position=relative)");
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
        return "gff";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
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
        String orientation="";
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           orientation=(String)getDefaultValueForParameter("Orientation");
        }
        if (target==null) target=new RegionDataset("temporary");
        if (target instanceof RegionDataset) { // The RegionDataset might not contain RegionSequenceData objects for all Sequences. Add them if they are missing!
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));
            }
        }
        int count=0;
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               Thread.yield();
            }
            if (line.startsWith("#") || line.isEmpty()) continue; // GFF comment line
            HashMap<String,Object> map=parseSingleLineInStandardFormat(line, count);
            String sequenceName=(String)map.get("SEQUENCENAME");
            RegionSequenceData targetSequence=null;
            //System.err.println("Parsed line: sequenceName="+sequenceName);
            if (target instanceof RegionSequenceData) {
               targetSequence=(RegionSequenceData)target;
               if (targetSequence==null || !targetSequence.getName().equals(sequenceName)) continue; // the sequence mentioned in the file does not correspond to any known sequence
               addRegionToTarget(targetSequence,map,orientation);
            } else if (target instanceof RegionDataset) {
                targetSequence=(RegionSequenceData)((RegionDataset)target).getSequenceByName(sequenceName);
                if (targetSequence==null) continue; // the sequence mentioned in the file does not correspond to any known sequence
                addRegionToTarget(targetSequence,map,orientation);
            } else if (target instanceof DataSegment) {
                addRegionToTarget(target,map,orientation);
            } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-Region data as target for GFF dataformat: "+target.getClass().getSimpleName(), count);
        }
        return target;
    }


    private void addRegionToTarget(Object target, HashMap<String,Object> map, String orientationString) throws ParseError {
        int start=0, end=0; // these are offset relative to the start of the parent sequence
        int targetOrientation=Sequence.DIRECT;
        int targetStart=0;
        int targetSize=0;
        int relativeoffset=1;
        if (target instanceof RegionSequenceData) {
            targetOrientation=((RegionSequenceData)target).getStrandOrientation();
            targetStart=((RegionSequenceData)target).getRegionStart();
            targetSize=((RegionSequenceData)target).getSize();
        } else if (target instanceof DataSegment) {
            targetStart=((DataSegment)target).getSegmentStart();
            targetSize=((DataSegment)target).getSize();
        } else throw new ParseError("Target object neither RegionSequenceData nor DataSegment in DataFormat_GFF.addRegionToTarget():"+target.toString());
        Object startValue=map.get("START");
        if (startValue instanceof Integer) start=(Integer)startValue;
        Object endValue=map.get("END");
        if (endValue instanceof Integer) end=(Integer)endValue;
        double score=0;
        Object scoreValue=map.get("SCORE");
        if (scoreValue instanceof Double) score=(Double)scoreValue;
        String type=(String)map.get("TYPE");
        if (type==null) type="unknown_type";
        String annotatedOrientation=(String)map.get("STRAND"); // the orientation in the GFF file
        int annotatedStrand=Sequence.DIRECT; // the strand corresponding to + in the input
             if (orientationString.equals(DIRECT)) annotatedStrand=Sequence.DIRECT;
        else if (orientationString.equals(REVERSE)) annotatedStrand=Sequence.REVERSE;
        else if (orientationString.equals(RELATIVE) || orientationString.equals("From Sequence") || orientationString.equals("From Gene")) {
           annotatedStrand=targetOrientation;
        } else if (orientationString.equals(OPPOSITE)) {
           annotatedStrand=targetOrientation;
           if (annotatedStrand==Sequence.DIRECT) annotatedStrand=Sequence.REVERSE;
           else annotatedStrand=Sequence.DIRECT;
        }
        int orientation=Sequence.DIRECT;
        if (annotatedOrientation==null) annotatedOrientation="+";
        // relative offsets. And orientation could be DIRECT,REVERSE,FROM GENE or OPPOSITE
        if (annotatedStrand==Sequence.DIRECT) {
           start-=relativeoffset;
           end-=relativeoffset;
                if (annotatedOrientation.equals(".")) orientation=Region.INDETERMINED;
           else if (annotatedOrientation.equals("+")) orientation=Region.DIRECT;
           else orientation=Region.REVERSE;
        } else { // relative offset and reverse orientation
           int annotatedStart=start;
           int annotatedEnd=end;
           start=(targetSize-1)-annotatedEnd+relativeoffset;
           end=(targetSize-1)-annotatedStart+relativeoffset;
                if (annotatedOrientation.equals(".")) orientation=Region.INDETERMINED;
           else if (annotatedOrientation.equals("+")) orientation=Region.REVERSE;
           else orientation=Region.DIRECT;
        }
        if (end<0 || start>=targetSize) return; // region is outside sequence
        RegionSequenceData parentSequence=null;
        if (target instanceof RegionSequenceData) parentSequence=(RegionSequenceData)target;
        Region newRegion=new Region(parentSequence, start, end, type, score, orientation);
        for (String property:map.keySet()) {
            if (property.equalsIgnoreCase("SEQUENCENAME") ||
                property.equalsIgnoreCase("FEATURE") ||
                property.equalsIgnoreCase("SOURCE") ||
                property.equalsIgnoreCase("START") ||
                property.equalsIgnoreCase("END") ||
                property.equalsIgnoreCase("SCORE") ||
                property.equalsIgnoreCase("TYPE") ||
                property.equalsIgnoreCase("STRAND")) continue;
            else {
                newRegion.setProperty(property, map.get(property));
            }
        }
        //System.err.println("Add to ["+target.toString()+"]  offset="+offsetString+"  relative="+relativeoffset+"  orientionString="+orientationString+":  region="+newRegion.toString());
        if (target instanceof RegionSequenceData) ((RegionSequenceData)target).addRegion(newRegion);
        else if (target instanceof DataSegment) ((DataSegment)target).addRegion(newRegion);
    }




    /** parses a single line in a GFF-file and returns a HashMap with the different properties (with values as strings!) according to the capturing groups in the formatString */
    private HashMap<String,Object> parseSingleLineInStandardFormat(String line, int lineNumber) throws ParseError {
        HashMap<String,Object> result=new HashMap<String,Object>();
        String[] fields=line.split("\t");
        if (fields.length<8) throw new ParseError("Expected at least 8 fields per line in GFF-format. Got "+fields.length+":\n"+line, lineNumber);
        //System.err.println("Parsed standard: "+line+" =>"+fields[0]);
        result.put("SEQUENCENAME",fields[0]);
        result.put("FEATURE",fields[1]);
        result.put("SOURCE",fields[1]); // this is correct
        //result.put("TYPE",fields[2]); //
        int start=0;
        int end=0;
        try {
            start=Integer.parseInt(fields[3]);
            result.put("START",start);
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+e.getMessage(), lineNumber);}
        try {
            end=Integer.parseInt(fields[4]);
            result.put("END",end);
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+e.getMessage(), lineNumber);}
        try {
            double score=Double.parseDouble(fields[5]);
            if (score<0) score=score*(-1); // why are the scores negative?
            result.put("SCORE",score);
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+e.getMessage(), lineNumber);}
//        if (end<start) result.put("STRAND","-");
//        else result.put("STRAND", "+");
        result.put("STRAND",fields[6]);
        if (fields.length>=9) {
            String[] attributes=fields[8].split(";");
            for (String attribute:attributes) {
                String[] pair=attribute.split("=");
                if (pair.length!=2) throw new ParseError("Attribute not in recognized 'key=value' format: "+attribute, lineNumber);
                String key=pair[0].trim();
                if (key.equalsIgnoreCase("motif_name")) key="TYPE";
                String value=pair[1].trim();
                if (value.startsWith("\"") && value.endsWith("\"")) value=value.substring(1, value.length()-1);
                result.put(key,value);
            }
        }
        return result;
    }

}





