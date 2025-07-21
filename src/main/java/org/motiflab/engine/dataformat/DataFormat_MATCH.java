/*


 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.SequenceCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_MATCH extends DataFormat {
    private String name="MATCH";
    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE="Relative";
    private static final String OPPOSITE="Opposite";
    private static final String IDENTIFIER="ID";
    private static final String SHORT_NAME="Short name";



    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    public DataFormat_MATCH() {
        addParameter("Use motif name", IDENTIFIER,new String[]{IDENTIFIER,SHORT_NAME},"Specifies whether to use the ID or the 'short name' to identify motifs in the output");
        addParameter("Orientation", RELATIVE,new String[]{DIRECT,REVERSE,RELATIVE,OPPOSITE},"Orientation of relative coordinates");
        addParameter("Score", "Matrix score",new String[]{"Matrix score","Core score"},"Specifies whether to use the calculated 'matrix score' or 'core score' as the main score property of the motif region"); 
        setParameterFilter("Score","input");
        setParameterFilter("Use motif name","output");
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
        return "out"; // this is not used anyway?
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);
        boolean useshortname=false;
        String orientation;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             String namestring=(String)settings.getResolvedParameter("Use motif name",defaults,engine);
             useshortname=(namestring.equalsIgnoreCase("short name"));
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           String namestring=(String)getDefaultValueForParameter("Use motif name");
           useshortname=(namestring.equalsIgnoreCase("short name"));
           orientation=(String)getDefaultValueForParameter("Orientation");
        }
        HashMap<String,String> namemap=null;
        int maxnamelength=22;
        if (useshortname) { // create mapping ID->shortname and find longest name
            ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
            namemap=new HashMap<String,String>(motifs.size());
            for (Data motif:motifs) {
                String motifID=motif.getName();
                String shortname=((Motif)motif).getShortName();
                if (shortname==null) shortname=motifID;
                namemap.put(motifID, shortname);
                if (shortname.length()>maxnamelength) maxnamelength=shortname.length();
            }
        } else { // find longest name
            ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
            for (Data motif:motifs) {
                String motifID=motif.getName();
                if (motifID.length()>maxnamelength) maxnamelength=motifID.length();
            }
        }
        RegionComparator regionComparator=new RegionComparator(namemap);
        String outputString="";
        StringBuilder builder=new StringBuilder();
        builder.append("Search for sites by WeightMatrix library:\n");
        builder.append("Sequence file:\n");
        builder.append("Site selection profile:\n");
        builder.append("\n");
        builder.append("\n");
        int[] counts=new int[]{0,0}; // first element is total number of bases in sequences, second is total number of TFBS
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
            outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, regionComparator, orientation, task, builder, counts, maxnamelength);
        } else if (dataobject instanceof RegionSequenceData){
            outputSequence((RegionSequenceData)dataobject,regionComparator,orientation, task, builder, counts, maxnamelength);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        builder.append(" Total sequences length=");      
        builder.append(counts[0]);      
        builder.append("\n\n");      
        builder.append(" Total number of found sites=");      
        builder.append(counts[1]);
        builder.append("\n\n");
        builder.append(" Frequency of sites per nucleotide=");
        builder.append((double)counts[1]/(double)counts[0]);
        builder.append("\n\n");
        outputString=builder.toString();
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, RegionComparator regionComparator, String orientationString, ExecutableTask task, StringBuilder outputString, int[] counts, int maxnamelength) throws InterruptedException {
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence
              String sequenceName=sequence.getName();
              RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence,regionComparator,orientationString, task, outputString, counts, maxnamelength);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        return outputString.toString();
    }


    /** output-formats a single sequence */
    private void outputSequence(RegionSequenceData sequence, RegionComparator regionComparator, String orientationString, ExecutableTask task, StringBuilder outputString, int[] counts, int maxnamelength) throws InterruptedException {
            outputString.append("Inspecting sequence ID   ");
            outputString.append(sequence.getName());
            outputString.append("\n\n");
            ArrayList<Region> regionList=sequence.getAllRegions();
            counts[0]+=sequence.getSize();
            counts[1]+=regionList.size();
            int shownStrand=Sequence.DIRECT;
                if (orientationString.equals(DIRECT)) shownStrand=Sequence.DIRECT;
            else if (orientationString.equals(REVERSE)) shownStrand=Sequence.REVERSE;
            else if (orientationString.equals(RELATIVE) || orientationString.equals("From Sequence") || orientationString.equals("From Gene")) {
               shownStrand=sequence.getStrandOrientation();
            } else if (orientationString.equals(OPPOSITE)) {
               shownStrand=sequence.getStrandOrientation();
               if (shownStrand==Sequence.DIRECT) shownStrand=Sequence.REVERSE;
               else shownStrand=Sequence.DIRECT;
            }
            Collections.sort(regionList, regionComparator);
            int count=0;
            int maxscorelength=7; // including 4 places for decimal sign + 3 decimals. Should have at least 1 space in front
            for (Region region:regionList) { // determine max score values (for 
               double matrixscore=region.getScore();
               double corescore=matrixscore;
               if (region.getProperty("Core score") instanceof Double) corescore=(Double)region.getProperty("Core score");
               if (region.getProperty("Matrix score") instanceof Double) matrixscore=(Double)region.getProperty("Matrix score");
               if (matrixscore>corescore) corescore=matrixscore; // use corescore
               int digits=(int)Math.log10(corescore)+1;
               if (digits+5>maxscorelength) maxscorelength=digits+5; // 5 => 1 space at the beginning + decimal sign + 3 decimals
            }
            for (Region region:regionList) {
               count++;
               if (count%200==0) {
                  if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                  if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                  Thread.yield();
               }
               outputRegion(region, regionComparator.getNameForRegion(region), sequence, shownStrand, outputString, maxnamelength, maxscorelength);
            }
            outputString.append("\n");
    }


    private void outputRegion(Region region, String usename, RegionSequenceData sequence, int shownStrand, StringBuilder outputString, int maxnamelength, int maxscorelength) {
       int relativeoffset=1;
       int start=-1;
       String strand=" (.)";
       String matchsequence=(String)region.getProperty("sequence");
       if (shownStrand==Sequence.DIRECT) {
           start=region.getRelativeStart()+relativeoffset;
       } else { // relative offset and reverse orientation
           start=(sequence.getSize()-1)-region.getRelativeEnd()+relativeoffset;
       }
            if (region.getOrientation()==Region.INDETERMINED) strand=" (.)";
       else if (region.getOrientation()==shownStrand) strand=" (+)";
       else {strand=" (-)"; matchsequence=MotifLabEngine.reverseSequence(matchsequence);}

       double matrixscore=region.getScore();
       double corescore=region.getScore();
       if (region.getProperty("Core score") instanceof Double) corescore=(Double)region.getProperty("Core score");
       if (region.getProperty("Matrix score") instanceof Double) matrixscore=(Double)region.getProperty("Matrix score");

       outputString.append(" ");
       outputString.append(usename);
       pad(outputString,maxnamelength-usename.length());
       outputString.append(" |");
       outputString.append(formatFixedWidthInteger(start, 9));
       outputString.append(strand);
       outputString.append(" |");
       outputString.append(formatFixedWidthDouble(corescore, maxscorelength, 3));
       outputString.append(" |");
       outputString.append(formatFixedWidthDouble(matrixscore, maxscorelength, 3));
       outputString.append(" | ");
       outputString.append(matchsequence);
       outputString.append("\n");
    }

    private void pad(StringBuilder builder, int spaces) {
        for (int i=0;i<spaces;i++) builder.append(' ');
    }

    /**
     * Compares two Regions according to their IDs or a second string property
     * provided as a map
     */
    private class RegionComparator implements Comparator<org.motiflab.engine.data.Region> {
        HashMap<String,String> map;

        public RegionComparator(HashMap<String,String> map) {
            this.map=map;
        }

        @Override
        public int compare(Region region1, Region region2) {
            String name1=getNameForRegion(region1);
            String name2=getNameForRegion(region2);
            return name1.compareTo(name2);
        }

        public String getNameForRegion(Region region) {
             String regiontype=region.getType();
             if (map!=null) return map.get(regiontype);
             else return regiontype;
        }
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
        String usescore="";
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
             usescore=(String)settings.getResolvedParameter("Score",defaults,engine);
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           orientation=(String)getDefaultValueForParameter("Orientation");
           usescore=(String)getDefaultValueForParameter("Score");
        }
        if (target==null) target=new RegionDataset("temporary");
        if (target instanceof RegionDataset) { // The RegionDataset might not contain RegionSequenceData objects for all Sequences. Add them if they are missing!
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));
            }
        }
        boolean useMatrixScore=(usescore.equalsIgnoreCase("Matrix score"));
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
            Pattern pattern=Pattern.compile("^\\s*(\\S+)\\s*\\|\\s*(\\d+)\\s+\\((\\+|\\-)\\)\\s*\\|\\s*([\\d\\.]+)\\s*\\|\\s*([\\d\\.]+)\\s*\\|\\s*(\\w+)");           
            if (line.startsWith("Inspecting sequence ID")) { // sequence name line               
                sequenceName=line.substring("Inspecting sequence ID".length());
                sequenceName=sequenceName.trim();
            } else {
                HashMap<String,Object> map=parseSingleLineInStandardFormat(line, sequenceName, pattern, useMatrixScore, count);
                if (map==null) continue; // not a TFBS line. Ignore it
                if (sequenceName==null) throw new ParseError("MATCH Format Error: Encountered TFBS line before Sequence line", count);
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
                } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-Region data as target for MATCH dataformat: "+target.getClass().getSimpleName());                
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
        } else throw new ParseError("Target object neither RegionSequenceData nor DataSegment in DataFormat_MATCH.addRegionToTarget():"+target.toString());
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
             if (orientationString.equalsIgnoreCase(DIRECT)) annotatedStrand=Sequence.DIRECT;
        else if (orientationString.equalsIgnoreCase(REVERSE)) annotatedStrand=Sequence.REVERSE;
        else if (orientationString.equalsIgnoreCase(RELATIVE) || orientationString.equalsIgnoreCase("From Sequence") || orientationString.equalsIgnoreCase("From Gene")) {
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




    /** parses a single line in a MATCH-file and returns a HashMap with the different properties according to the capturing groups in the formatString */
    private HashMap<String,Object> parseSingleLineInStandardFormat(String line, String sequenceName, Pattern pattern, boolean useMatrixScore, int lineNumber) throws ParseError {
        Matcher matcher=pattern.matcher(line);
        if (matcher.find()) {
           //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
            String motifID=matcher.group(1);  
            String startString=matcher.group(2);
            String strandString=matcher.group(3);
            String coreScoreString=matcher.group(4);   
            String matrixScoreString=matcher.group(5);   
            String sequenceMatchString=matcher.group(6);   
            HashMap<String,Object> result=new HashMap<String,Object>();
            result.put("SEQUENCENAME",sequenceName);
            result.put("FEATURE","misc_feature");
            result.put("SOURCE","MATCH"); // this is correct
            //result.put("TYPE",fields[2]); //
            int start=0;
            double corescore=0;
            double matrixscore=0;
            
            try {
                start=Integer.parseInt(startString);
            } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+e.getMessage(), lineNumber);}
            try {
                matrixscore=Double.parseDouble(matrixScoreString);
            } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+e.getMessage(), lineNumber);}
            try {
                corescore=Double.parseDouble(coreScoreString);
            } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+e.getMessage(), lineNumber);}

            int end=0;
            int motifsize=sequenceMatchString.length();
            end=start+motifsize-1;
            result.put("STRAND",strandString);
            result.put("TYPE",motifID);
            result.put("START",start);
            result.put("END",end);               
            if (useMatrixScore) {
                result.put("SCORE",matrixscore);
                result.put("Core score",corescore);
            } else {
                result.put("SCORE",corescore);
                result.put("Matrix score",matrixscore);
            }
            return result;           
        } else return null; // not a TFBS line

    }

}





