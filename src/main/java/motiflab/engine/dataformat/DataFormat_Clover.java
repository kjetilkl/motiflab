/*


 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Region;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.Parameter;
import motiflab.engine.data.DataSegment;
/**
 *
 * @author kjetikl
 */
public class DataFormat_Clover extends DataFormat {
    private String name="Clover";
    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE="Relative";
    private static final String OPPOSITE="Opposite";    



    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    public DataFormat_Clover() {
        addParameter("Orientation", RELATIVE,new String[]{DIRECT,REVERSE,RELATIVE,OPPOSITE},"Orientation of relative coordinates"); 
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
        return "txt"; // this is not used anyway?
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
        String sequenceName=null;        
        for (String line:input) { // parsing each line in succession
            line=line.trim();
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               Thread.yield();
            }
            Pattern pattern=Pattern.compile("^\\s*(\\w+)\\s+(\\d+)\\s-\\s+(\\d+)\\s{4}(\\+|\\-)\\s+([ACGTacgt]+)\\s+([\\d\\.]+)$");           
            if (line.matches("^>[a-zA-Z_0-9.+-]+$")) { // sequence name                
                sequenceName=line.substring(1);
            } else {
                HashMap<String,Object> map=parseSingleLineInStandardFormat(line, sequenceName, pattern, count);
                if (map==null) continue; // not a TFBS line. Ignore it
                if (sequenceName==null) throw new ParseError("CLOVER Format Error: Encountered TFBS line before Sequence line", count);
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
                } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-Region data as target for Clover dataformat: "+target.getClass().getSimpleName(), count);                
            }
        }
        return target;
    }

    
    private void addRegionToTarget(Object target, HashMap<String,Object> map, String orientationString) throws ParseError {
        int start=0, end=0; // these are offset relative to the start of the parent sequence
        int targetOrientation=Sequence.DIRECT;
        int targetSize=0;
        int relativeoffset=1;
        if (target instanceof RegionSequenceData) {
            targetOrientation=((RegionSequenceData)target).getStrandOrientation();
            targetSize=((RegionSequenceData)target).getSize();
        } else if (target instanceof DataSegment) {
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
    private HashMap<String,Object> parseSingleLineInStandardFormat(String line, String sequenceName, Pattern pattern, int linenumber) throws ParseError {
        Matcher matcher=pattern.matcher(line);
        if (matcher.find()) {
           //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
            String motifID=matcher.group(1);  
            String startString=matcher.group(2);
            String endString=matcher.group(3);
            String strandString=matcher.group(4);
            //String sequenceMatch=matcher.group(5); // this will be added by motif scanning operation?
            String scoreString=matcher.group(6);   
            HashMap<String,Object> result=new HashMap<String,Object>();
            result.put("SEQUENCENAME",sequenceName);
            result.put("FEATURE","misc_feature");
            result.put("SOURCE","Clover"); // this is correct
            //result.put("TYPE",fields[2]); //
            int start=0;
            int end=0;
            try {
                start=Integer.parseInt(startString);
                result.put("START",start);
            } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+e.getMessage(), linenumber);}
            try {
                end=Integer.parseInt(endString);
                result.put("END",end);
            } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+e.getMessage(), linenumber);}
            try {
                double score=Double.parseDouble(scoreString);
                if (score<0) score=score*(-1); // why are the scores negative?
                result.put("SCORE",score);
            } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+e.getMessage(), linenumber);}
    //        if (end<start) result.put("STRAND","-");
    //        else result.put("STRAND", "+");
            result.put("STRAND",strandString);
            result.put("TYPE",motifID);
            return result;           
        } else return null; // not a TFBS line

    }

}





