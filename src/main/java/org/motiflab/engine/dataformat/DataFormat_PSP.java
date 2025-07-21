/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_PSP extends DataFormat {
    private String name="PSP";    
    private Class[] supportedTypes=new Class[]{NumericSequenceData.class, NumericDataset.class};

    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE="Relative";
    private static final String OPPOSITE="Opposite";
    private static final String PARAMETER_STRAND_ORIENTATION="Orientation";
    private static final String MOTIF_WIDTH="Motif width";
    private static final String INCLUDE_WIDTH="Include width";
    private static final String NORMALIZE="Normalize";
    private static final String NORMALIZE_SUM_TO_1="Sum to 1";
    private static final String NORMALIZE_0_TO_1="Max 1";
    
    public DataFormat_PSP() {
        addParameter(PARAMETER_STRAND_ORIENTATION, RELATIVE, new String[]{DIRECT,REVERSE,RELATIVE,OPPOSITE},"Sequence orientation");
        addParameter(MOTIF_WIDTH, new Integer(8), new Integer[]{5,50},"Width of motif");
        addOptionalParameter(INCLUDE_WIDTH, Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Include the width of the motif in the header (after the name of the sequence)");
        addOptionalParameter(NORMALIZE, "", new String[]{"",NORMALIZE_SUM_TO_1,NORMALIZE_0_TO_1},"Perform normalization on values");
        setParameterFilter(MOTIF_WIDTH,"output");
        setParameterFilter(INCLUDE_WIDTH,"output");        
        setParameterFilter(NORMALIZE,"output");        
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof NumericSequenceData || data instanceof NumericDataset);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(NumericSequenceData.class) || dataclass.equals(NumericDataset.class));
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof NumericSequenceData || data instanceof NumericDataset);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(NumericSequenceData.class) || dataclass.equals(NumericDataset.class));
    }
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "psp";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        String orientation;
        int motifwidth=8;
        String normalizeString="";
        boolean includeWidth=true;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);
             motifwidth=(Integer)settings.getResolvedParameter(MOTIF_WIDTH,defaults,engine);
             normalizeString=(String)settings.getResolvedParameter(NORMALIZE,defaults,engine);
             includeWidth=(Boolean)settings.getResolvedParameter(INCLUDE_WIDTH,defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
           motifwidth=(Integer)getDefaultValueForParameter(MOTIF_WIDTH);
           normalizeString=(String)getDefaultValueForParameter(NORMALIZE);
           includeWidth=(Boolean)getDefaultValueForParameter(INCLUDE_WIDTH);
        }
        if (normalizeString==null) normalizeString="";
        String outputString="";
        if (dataobject instanceof NumericDataset) {
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
            outputString=outputMultipleSequences((NumericDataset)dataobject, sequenceCollection, orientation, motifwidth, includeWidth, normalizeString, task, engine);
        } else if (dataobject instanceof NumericSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((NumericSequenceData)dataobject, orientation, motifwidth, includeWidth, normalizeString, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }    
    
    
    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(NumericDataset sourcedata, SequenceCollection collection, String orientation, int motifwidth, boolean includeWidth, String normalize, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              NumericSequenceData sourceSequence=(NumericSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence, orientation,motifwidth,includeWidth,normalize,outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }    
    
    
    /** output-formats a single sequence */
    private void outputSequence(NumericSequenceData sequence, String orientationString, int motifwidth, boolean includeWidth, String normalize, StringBuilder outputString) {
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
        boolean normalizeSumTo1=false;
        boolean normalize0To1=false;
        if (normalize.equals(NORMALIZE_SUM_TO_1)) normalizeSumTo1=true;
        else if (normalize.equals(NORMALIZE_0_TO_1)) normalize0To1=true;
        double normalizationFactor=1.0;
        int normalizationStart=0;
        int normalizationEnd=sequence.getSize()-1;
        if (shownStrand==Sequence.DIRECT) {
             normalizationEnd=(sequence.getSize()-motifwidth); // padding with 0
        } else {
             normalizationStart=motifwidth-1;
        }            
        if (normalize0To1) {
            double[] minmax=sequence.getMinMaxFromData(normalizationStart, normalizationEnd);
            if (minmax[1]>1) normalizationFactor=minmax[1]; // divide by largest value
        } else if (normalizeSumTo1) {
            double sum=sequence.getSumValueInInterval(normalizationStart, normalizationEnd);
            if (sum>1) normalizationFactor=sum;
        }
        outputString.append(">");
        outputString.append(sequence.getSequenceName());
        if (includeWidth) {
            outputString.append(" ");
            outputString.append(motifwidth);
        }
        outputString.append("\n");
        int start=sequence.getRegionStart();
        int end=sequence.getRegionEnd();
        int basesleft=sequence.getSize();
        if (shownStrand==Sequence.DIRECT) {
            for (int i=start;i<=end;i++) {
                double value=sequence.getValueAtGenomicPosition(i);
                if (basesleft<motifwidth) value=0; // the last width-1 values should be 0
                else value=value/normalizationFactor;
                outputString.append(value);
                outputString.append(" ");
                basesleft--;
            }
        } else {// output reverse strand
            for (int i=end;i>=start;i--) {
                double value=sequence.getValueAtGenomicPosition(i);
                if (basesleft<motifwidth) value=0;  // the last width-1 values should be 0
                else value=value/normalizationFactor;
                outputString.append(value);
                outputString.append(" ");
                basesleft--;
            }                
        }
        outputString.setCharAt(outputString.length()-1, '\n');// change last comma to newline            return outputString;
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) {
            target=new NumericDataset("temporary");
        }
        if (target instanceof NumericDataset) setupNumericDataset((NumericDataset)target);
        if (!(target instanceof NumericSequenceData || target instanceof NumericDataset)) throw new ParseError("SLOPPY PROGRAMMING ERROR: non-NumericSequenceData passed as parameter to parseInput in DataFormat_Priority");        
        if (input.size()<1) return target; // throw new ParseError("Empty input");
        String orientation;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);          
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
            orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
        }
        if (target instanceof NumericSequenceData) target=parseSingleSequenceInput(input, (NumericSequenceData)target, orientation);
        else if (target instanceof NumericDataset) target=parseMultipleSequenceInput(input, (NumericDataset)target, orientation, task);
        else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-DNASequenceData passed as parameter to parseInput in DataFormat_FASTA");
        return target;
    }
    
    

    
    private NumericSequenceData parseSingleSequenceInput(ArrayList<String> input, NumericSequenceData target, String orientationString) throws ParseError, InterruptedException {
        int useStrand=determineOrientation(target,orientationString);
        String complete="";
        int last=input.size();
        int first=-1;
        int i=0;
        int lineNumber=0;
        for (String line:input) {
            lineNumber++;
            if (line.startsWith(">")) {
                String targetName=getSequenceNameFromHeader(line);
                if (targetName==null) throw new ParseError("Unable to extract sequence name from header: "+line, lineNumber);
                String error=engine.checkSequenceNameValidity(targetName, false);
                if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+targetName+"' : "+error, lineNumber);                  
                if (targetName.equals(target.getSequenceName())) first=i;
                else {
                    if (first<0) continue; // sequence not found yet
                    else {last=i;break;} // found sequence after target sequence
                }
            }
            i++;
        }
        if (first<0) return target; // sequence not found in file
        List<String> sub=input.subList(first, last);
        for (String string:sub) complete+=string.trim();   
        target.setData(parseSingleSequence(target.getSize(), complete, useStrand));
        return target;
    }
    
    private int determineOrientation(NumericSequenceData target, String orientationString) {
        int useStrand=Sequence.DIRECT;
            if (orientationString.equals(DIRECT)) useStrand=Sequence.DIRECT;
        else if (orientationString.equals(REVERSE)) useStrand=Sequence.REVERSE;
        else if (orientationString.equals(RELATIVE) || orientationString.equals("From Gene")) {
           useStrand=target.getStrandOrientation();
        } else if (orientationString.equals(OPPOSITE)) {
           useStrand=target.getStrandOrientation();
           if (useStrand==Sequence.DIRECT) useStrand=Sequence.REVERSE;
           else useStrand=Sequence.DIRECT;
        }
        return useStrand;
    }
    
    private double[] parseSingleSequence(int expectedsize, String valuesString, int useStrand) throws ParseError {
        String[] elements=valuesString.split("\\s*,\\s*");
        if (elements.length!=expectedsize) throw new ParseError("Number of values found for sequence ("+elements.length+") does not match expected sequence length ("+expectedsize+")");
        double[] values=new double[elements.length];
        if (useStrand==Sequence.DIRECT) {
            for (int i=0;i<elements.length;i++) {
                try {
                    values[i]=Double.parseDouble(elements[i]);                    
                } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value: "+elements[i]);}
            }
        } else {
            for (int i=0;i<elements.length;i++) {
                try {
                    values[values.length-(i+1)]=Double.parseDouble(elements[i]);                    
                } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value: "+elements[i]);}
            }            
        }
        return values;
    }
    

    private NumericDataset parseMultipleSequenceInput(ArrayList<String> input, NumericDataset target, String orientationString, ExecutableTask task) throws ParseError, InterruptedException {
        int first=-1; int last=0; int size=input.size();
        ArrayList<int[]> startandstop=new ArrayList<int[]>();
        for (int i=0;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith(">")) {
                if (first<0) {
                    first=i;
                } else {
                    last=i;
                    startandstop.add(new int[]{first,last});
                    first=i;
                    last=0;
                }
            }
        }
        if (first>=0) {
            startandstop.add(new int[]{first,size});
        }
        if (first<0) return target; // sequence not found in file
        int count=0;
        for (int[] pair:startandstop) {
            count++;
            if (count%3==0) {
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              Thread.yield();
            }              
            int start=pair[0]; int end=pair[1];
            if (end<=start+1) continue; // no sequence?
            String sequencename=getSequenceNameFromHeader(input.get(start));
            if (sequencename==null) throw new ParseError("Unable to extract sequence name from header: "+input.get(start));
            String error=engine.checkSequenceNameValidity(sequencename, false);
            if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+sequencename+"' : "+error);            
            NumericSequenceData seq=(NumericSequenceData)target.getSequenceByName(sequencename);
            if (seq==null) continue; // unknown sequence
            String complete="";
            for (int j=start+1;j<end;j++) complete+=input.get(j).trim();
            int useStrand=determineOrientation(seq,orientationString);  
            seq.setData(parseSingleSequence(seq.getSize(), complete, useStrand));
        }
        return target;
    }    
    
    
    
    
    private String getSequenceNameFromHeader(String header) {
        Pattern pattern=Pattern.compile(">\\s*([a-zA-Z_0-9.+-]+)");
        Matcher matcher=pattern.matcher(header);
        if (matcher.find()) { 
            String sequenceName=matcher.group(1);
            sequenceName=convertIllegalSequenceNamesIfNecessary(sequenceName, false);              
            return sequenceName;
        } else return null;
    }    
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        String complete="";
        int count=0;
        for (String line:input) {
            line=line.trim();
            if (line.startsWith(">")) continue;
            else if (line.isEmpty()) continue;
            else if (line.startsWith("minmotiflength")) continue;
            else if (line.startsWith("maxmotiflength")) continue;
            else complete+=line;
            count++;
            if (count%200==0) {
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              Thread.yield();
            } 
        }
        target.setSegmentData(parseSingleSequence(target.getSize(), complete, Sequence.DIRECT));
        return target;
    }
    
    
    /** adds in missing sequences */
    private void setupNumericDataset(NumericDataset dataset) {
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data seq:sequences) {
            if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new NumericSequenceData((Sequence)seq,0f));     
        }          
    }
}
