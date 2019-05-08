/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_Priority extends DataFormat {
    private String name="PRIORITY";    
    private Class[] supportedTypes=new Class[]{NumericSequenceData.class, NumericDataset.class};

    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE="Relative";
    private static final String OPPOSITE="Opposite";
    private static final String PARAMETER_STRAND_ORIENTATION="Orientation";
    private static final String PARAMETER_SEPARATOR="Separator";
    private static final String TAB_SEPARATOR="TAB";
    private static final String COMMA_SEPARATOR="Comma";   
    private static final String SPACE_SEPARATOR="Space";          
    private static final String NEWLINE_SEPARATOR="Newline";       
    
    public DataFormat_Priority() {
        addParameter(PARAMETER_SEPARATOR, COMMA_SEPARATOR, new String[]{COMMA_SEPARATOR,TAB_SEPARATOR,SPACE_SEPARATOR,NEWLINE_SEPARATOR},"Character that separates values");
        addParameter(PARAMETER_STRAND_ORIENTATION, RELATIVE, new String[]{DIRECT,REVERSE,RELATIVE,OPPOSITE},"Sequence orientation");
        addParameter("Include header", Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Sequence orientation",false,true);
        setParameterFilter("Include header","output");
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
        return "prior";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        String orientation;
        String separator=COMMA_SEPARATOR;
        boolean includeHeader=true;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);
             separator=(String)settings.getResolvedParameter(PARAMETER_SEPARATOR,defaults,engine);     
             includeHeader=(Boolean)settings.getResolvedParameter("Include header",defaults,engine);              
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
           separator=(String)getDefaultValueForParameter(PARAMETER_SEPARATOR);
           includeHeader=(Boolean)getDefaultValueForParameter("Include header");           
        }
        if (separator.equals(TAB_SEPARATOR)) separator="\t";
        else if (separator.equals(COMMA_SEPARATOR)) separator=",";
        else if (separator.equals(SPACE_SEPARATOR)) separator=" ";
        else if (separator.equals(NEWLINE_SEPARATOR)) separator="\n";        
        else separator=",";
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
            outputString=outputMultipleSequences((NumericDataset)dataobject, sequenceCollection, orientation, separator, includeHeader, task, engine);
        } else if (dataobject instanceof NumericSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((NumericSequenceData)dataobject, orientation, separator, includeHeader, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }    
    
    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(NumericDataset sourcedata, SequenceCollection collection, String orientation, String separator, boolean includeHeader, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              NumericSequenceData sourceSequence=(NumericSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence, orientation, separator, includeHeader, outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    } 
    
    
    /** outputformats a single sequence into StringBuilder*/
    private void outputSequence(NumericSequenceData sequence, String orientationString, String separator, boolean includeHeader, StringBuilder outputString) {
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
            if (includeHeader) {
                outputString.append(">");
                outputString.append(sequence.getSequenceName());
                outputString.append("\n");
            }
            int start=sequence.getRegionStart();
            int end=sequence.getRegionEnd();
            if (shownStrand==Sequence.DIRECT) {
                for (int i=start;i<=end;i++) {
                    double value=sequence.getValueAtGenomicPosition(i);
                    if (value%1==0) outputString.append((int)value);
                    else outputString.append(value);
                    outputString.append(separator);
                }
            } else {// output reverse strand
                for (int i=end;i>=start;i--) {
                    double value=sequence.getValueAtGenomicPosition(i);
                    if (value%1==0) outputString.append((int)value);
                    else outputString.append(value);
                    outputString.append(separator);
                }                
            }
            outputString.setCharAt(outputString.length()-1, '\n');// change last separator to newline
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
        String separator;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);          
              separator=(String)settings.getResolvedParameter(PARAMETER_SEPARATOR,defaults,engine);          
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
            orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
            separator=(String)getDefaultValueForParameter(PARAMETER_SEPARATOR);
        }
        if (separator.equals(TAB_SEPARATOR)) separator="\t";
        else if (separator.equals(COMMA_SEPARATOR)) separator=",";
        else if (separator.equals(SPACE_SEPARATOR)) separator=" ";
        else if (separator.equals(NEWLINE_SEPARATOR)) separator="\n";        
        else separator=",";        
        if (target instanceof NumericSequenceData) target=parseSingleSequenceInput(input, (NumericSequenceData)target, orientation, separator);
        else if (target instanceof NumericDataset) target=parseMultipleSequenceInput(input, (NumericDataset)target, orientation, separator, task);
        else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-DNASequenceData passed as parameter to parseInput in DataFormat_FASTA");
        return target;
    }
    
    

    
    private NumericSequenceData parseSingleSequenceInput(ArrayList<String> input, NumericSequenceData target, String orientationString, String separator) throws ParseError, InterruptedException {
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
        target.setData(parseSingleSequence(target.getSize(), complete, useStrand, separator,target.getSequenceName()));
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
    
    private double[] parseSingleSequence(int expectedsize, String valuesString, int useStrand, String separator, String sequenceName) throws ParseError {
        if (separator.equals(",")) separator="\\s*"+separator+"\\s*";
        String[] elements=valuesString.split(separator);
        if (elements.length==expectedsize*2) engine.logMessage("Found double-stranded priors for '"+sequenceName+", but will only use the first half of the values.");
        else if (elements.length!=expectedsize) throw new ParseError("Number of values found for sequence '"+sequenceName+"' ("+elements.length+") does not match expected sequence length ("+expectedsize+")");
        double[] values=new double[expectedsize];
        if (useStrand==Sequence.DIRECT) {
            for (int i=0;i<expectedsize;i++) {
                try {
                    values[i]=Double.parseDouble(elements[i]);                    
                } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value: "+elements[i]);}
            }
        } else {
            for (int i=0;i<expectedsize;i++) {
                try {
                    values[values.length-(i+1)]=Double.parseDouble(elements[i]);                    
                } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value: "+elements[i]);}
            }            
        }
        return values;
    }
    

    private NumericDataset parseMultipleSequenceInput(ArrayList<String> input, NumericDataset target, String orientationString, String separator, ExecutableTask task) throws ParseError, InterruptedException {
        int first=-1; int last=0; int size=input.size();
        boolean whitespaceSeparator=(separator.trim().isEmpty());
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
            NumericSequenceData seq=(NumericSequenceData)target.getSequenceByName(sequencename);
            if (seq==null) continue; // unknown sequence
            StringBuilder complete=new StringBuilder();
            for (int j=start+1;j<end;j++) { // add all the lines to a single String
                String string=input.get(j).trim();                
                complete.append(string);
                if (whitespaceSeparator) complete.append(separator); // add back separator if it was removed by the trim() above
            }
            int useStrand=determineOrientation(seq,orientationString);  
            seq.setData(parseSingleSequence(seq.getSize(), complete.toString(), useStrand, separator, sequencename));
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
        String separator;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
               separator=(String)settings.getResolvedParameter(PARAMETER_SEPARATOR,defaults,engine);          
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
            separator=(String)getDefaultValueForParameter(PARAMETER_SEPARATOR);
        }
        if (separator.equals(TAB_SEPARATOR)) separator="\t";
        else if (separator.equals(COMMA_SEPARATOR)) separator=",";
        else if (separator.equals(SPACE_SEPARATOR)) separator=" ";
        else if (separator.equals(NEWLINE_SEPARATOR)) separator="\n";        
        else separator=",";          
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
        target.setSegmentData(parseSingleSequence(target.getSize(), complete, Sequence.DIRECT, separator,"segment"));
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
