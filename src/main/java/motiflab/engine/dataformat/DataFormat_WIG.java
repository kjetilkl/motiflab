/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
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
public class DataFormat_WIG extends DataFormat {
    private String name="WIG";    
    private Class[] supportedTypes=new Class[]{NumericSequenceData.class, NumericDataset.class};

    
    public DataFormat_WIG() {
       // addParameter("Position", "Genomic", new String[]{"Relative","Genomic"},null); 
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
        return "wig";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
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
            outputString=outputMultipleSequences((NumericDataset)dataobject, sequenceCollection, task, engine);
        } else if (dataobject instanceof NumericSequenceData){
            String featureName=((NumericSequenceData)dataobject).getParent().getName();
            StringBuilder builder=new StringBuilder("track type=wiggle_0 name=\""+featureName+"\"\n");
            outputSequence((NumericSequenceData)dataobject,builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }    
    

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(NumericDataset sourcedata, SequenceCollection collection, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        String featureName=sourcedata.getName();
        StringBuilder outputString=new StringBuilder("track type=wiggle_0 name=\""+featureName+"\"\n");        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              NumericSequenceData sourceSequence=(NumericSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence,outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }     
    
    
    /** outputformats a single sequence */
    private void outputSequence(NumericSequenceData sequence, StringBuilder outputString) {
        String chromosome=sequence.getChromosome();
        if (!chromosome.startsWith("chr")) chromosome="chr"+chromosome;
        outputString.append("variableStep chrom=");
        outputString.append(chromosome);
        outputString.append("\n");
        int start=sequence.getRegionStart();
        int end=sequence.getRegionEnd();
        for (int i=start;i<=end;i++) {
            double value=sequence.getValueAtGenomicPosition(i);
            outputString.append(i);
            outputString.append("\t");
            outputString.append(value);
            outputString.append("\n");
        }
    }

    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) {
            target=new NumericDataset("temporary");
        }
        if (target instanceof NumericDataset) setupNumericDataset((NumericDataset)target);
        if (!(target instanceof NumericSequenceData || target instanceof NumericDataset)) throw new ParseError("SLOPPY PROGRAMMING ERROR: non-NumericSequenceData passed as parameter to parseInput in DataFormat_WIG");        
        int span=1;
        int step=1;
        int position=0;
        boolean fixed=false;
        boolean bedGraph=false; // this format can be embedded in WIG!
        String chrom="unknown";
        int count=0;
        for (String line:input) {
            count++;
            if (count%300==0) { // Yield every 300 lines (arbitrary choice)
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              Thread.yield();
            }   
            line=line.trim();
            //if (line.contains("no data points found")) throw new ParseError("No data points found in Wiggle-file");
            if (line.contains("no data points found")) break; // do not parse anymore. just return an "empty" dataset
            else if (line.startsWith("track")) continue;
            else if (line.startsWith("#bedGraph")) {bedGraph=true;continue;}            
            else if (line.startsWith("#")) continue;
            else if (line.isEmpty()) continue;
            else if (line.startsWith("variableStep")) {
               bedGraph=false;
               Pattern pattern=Pattern.compile("variableStep\\s+chrom=chr(\\w+)(\\s+span=(\\d+))?");
               Matcher matcher=pattern.matcher(line);
               String spanString=null;
               if (matcher.find()) {
                   chrom=matcher.group(1);
                   //if (!chrom.equalsIgnoreCase(matcher.group(1))) throw new ParseError("Expected data from chromosome '"+chrom+"' found chromosome '"+matcher.group(1)+"'");
                   spanString=matcher.group(3);
               } else throw new ParseError("Unable to parse Wiggle-format declaration line: "+line);
               if (spanString!=null) {
                    try {span=Integer.parseInt(spanString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+spanString);}
               }
               fixed=false;
            } else if (line.startsWith("fixedStep")) {
               bedGraph=false;
               Pattern pattern=Pattern.compile("fixedStep\\s+chrom=chr(\\w+)\\s+start=(\\d+)\\s+step=(\\d+)(\\s+span=(\\d+))?");
               Matcher matcher=pattern.matcher(line);
               String startString=null;
               String stepString=null;
               String spanString=null;
               if (matcher.find()) {
                   chrom=matcher.group(1);
                   //if (!chrom.equalsIgnoreCase(matcher.group(1))) throw new ParseError("Expected data from chromosome '"+chrom+"' found chromosome '"+matcher.group(1)+"'");
                   startString=matcher.group(2);
                   stepString=matcher.group(3);
                   spanString=matcher.group(5);
                 } else throw new ParseError("Unable to parse Wiggle-format declaration line:\n"+line);
               if (startString!=null) try {position=Integer.parseInt(startString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+startString);}
               if (stepString!=null) try {step=Integer.parseInt(stepString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+stepString);}
               if (spanString!=null) try {span=Integer.parseInt(spanString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+spanString);}
               fixed=true;
            } else { // data line
                String[] values=line.split("\\s+");
                if (bedGraph) { // embedded bedGraph section
                     if (values.length!=4) throw new ParseError("Expected 4 values per line for embedded 'bedGraph' in Wiggle format (found "+values.length+"): "+line);                   
                     int start=0;
                     int end=0;
                     double numericalValue=0;
                     try {start=Integer.parseInt(values[1]);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+values[1]);}
                     try {end=Integer.parseInt(values[2]);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+values[2]);}
                     try {numericalValue=Double.parseDouble(values[3]);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+values[3]);}
                     start+=1; // Add +1 to start coordinate since wiggle and MotifLab is 1-indexed but bedGraph is 0-indexed
                     if (target instanceof NumericSequenceData) {
                       for (int i=start;i<=end;i++)  ((NumericSequenceData)target).setValueAtGenomicPosition(chrom, i, numericalValue);
                     } else if (target instanceof NumericDataset) {
                       for (int i=start;i<=end;i++) ((NumericDataset)target).setValueAtGenomicPosition(chrom, i, numericalValue);
                     }                                            
                } else if (fixed) {// fixedStep
                    if (values.length!=1) throw new ParseError("Expected 1 value per line for fixedStep Wiggle format:\n"+line);
                    double numericalValue=0;
                    try {
                        numericalValue=Double.parseDouble(values[0]);
                    } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value:\n"+values[0]);}
                    if (target instanceof NumericSequenceData) {
                       for (int i=0;i<span;i++)  ((NumericSequenceData)target).setValueAtGenomicPosition(chrom, position+i, numericalValue);
                    } else if (target instanceof NumericDataset) {
                       for (int i=0;i<span;i++) ((NumericDataset)target).setValueAtGenomicPosition(chrom, position+i, numericalValue);
                    }                     
                    position+=step;
                } else { // variableStep
                    if (values.length!=2) throw new ParseError("Expected 2 values per line for variableStep Wiggle format (found "+values.length+"):\n"+line);
                    double numericalValue=0;
                    int pos=0;
                    try {
                        pos=Integer.parseInt(values[0]);
                        numericalValue=Double.parseDouble(values[1]);
                    } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric values: "+values[0]+" or "+values[1]);}
                    if (target instanceof NumericSequenceData) {
                       for (int i=0;i<span;i++)  ((NumericSequenceData)target).setValueAtGenomicPosition(chrom, pos+i, numericalValue);
                    } else if (target instanceof NumericDataset) {
                       for (int i=0;i<span;i++) ((NumericDataset)target).setValueAtGenomicPosition(chrom, pos+i, numericalValue);
                    }           
                }
                
            }

        }
        return target;
    }
    
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        int span=1;
        int step=1;
        int position=target.getSegmentStart();
        int dataSegmentStart=target.getSegmentStart();
        boolean fixed=false;
        boolean bedGraph=false; // this format can be embedded in WIG!
        String chrom=target.getChromosome();
        double[] buffer=new double[target.getSize()];
        int count=0;
        for (String line:input) {
            count++;
            if (count%300==0) { // Yield every 300 lines (arbitrary choice)
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              Thread.yield();
            }        
            if (line.startsWith("#ERROR:")) throw new ParseError(line.substring("#ERROR:".length()).trim());                       
            else if (line.contains("no data points found")) {engine.logMessage("WARNING: no data points found");break;} //throw new ParseError("No data points found in Wiggle-file");
            else if (line.startsWith("track")) continue;
            else if (line.startsWith("#bedGraph")) {bedGraph=true;continue;}
            else if (line.startsWith("#")) continue;
            else if (line.startsWith("variableStep")) {
               bedGraph=false;                
               Pattern pattern=Pattern.compile("variableStep\\s+chrom=chr(\\w+)(\\s+span=(\\d+))?");
               Matcher matcher=pattern.matcher(line);
               String spanString=null;
               if (matcher.find()) {
                   if (!chrom.equalsIgnoreCase(matcher.group(1))) throw new ParseError("Expected data from chromosome '"+chrom+"' found chromosome '"+matcher.group(1)+"'");
                   spanString=matcher.group(3);
               } else throw new ParseError("Unable to parse Wiggle-format declaration line: "+line);
               if (spanString!=null) {
                    try {span=Integer.parseInt(spanString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+spanString);}
               }
               fixed=false;
            } else if (line.startsWith("fixedStep")) {
               bedGraph=false;
               Pattern pattern=Pattern.compile("fixedStep\\s+chrom=chr(\\w+)\\s+start=(\\d+)\\s+step=(\\d+)(\\s+span=(\\d+))?");
               Matcher matcher=pattern.matcher(line);
               String startString=null;
               String stepString=null;
               String spanString=null;
               if (matcher.find()) {
                   if (!chrom.equalsIgnoreCase(matcher.group(1))) throw new ParseError("Expected data from chromosome '"+chrom+"' found chromosome '"+matcher.group(1)+"'");
                   startString=matcher.group(2);
                   stepString=matcher.group(3);
                   spanString=matcher.group(5);
                 } else throw new ParseError("Unable to parse Wiggle-format declaration line: "+line);
               if (startString!=null) try {position=Integer.parseInt(startString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for 'start': "+startString);}
               if (stepString!=null) try {step=Integer.parseInt(stepString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for 'step': "+stepString);}
               if (spanString!=null) try {span=Integer.parseInt(spanString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for 'span': "+spanString);}
               fixed=true;
            } else { // data line
                String[] values=line.split("\\s+");
                if (bedGraph) { // embedded bedGraph section
                     if (values.length!=4) throw new ParseError("Expected 4 values per line for embedded 'bedGraph' in Wiggle format (found "+values.length+"): "+line);                   
                     int start=0;
                     int end=0;
                     double numericalValue=0;
                     try {start=Integer.parseInt(values[1]);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+values[1]);}
                     try {end=Integer.parseInt(values[2]);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+values[2]);}
                     try {numericalValue=Double.parseDouble(values[3]);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+values[3]);}
                     int bufferpointer=0;
                     for (int i=start;i<end;i++) { // the end-coordinate is not included in bedGraph [start,end)
                        bufferpointer=i-dataSegmentStart+1; // add +1 because wiggle and MotifLab uses 1-indexed chromosomes while bedGraph uses 0-indexed
                        if (bufferpointer>=0 && bufferpointer<buffer.length) buffer[bufferpointer]=numericalValue;
                     }                      
                } else if (fixed) {// fixedStep
                    if (values.length!=1) throw new ParseError("Expected 1 value per line for fixedStep Wiggle format (found "+values.length+"): "+line);
                    double numericalValue=0;
                    try {
                        numericalValue=Double.parseDouble(values[0]);
                    } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+values[0]);}
                    int bufferpointer=0;
                    for (int i=0;i<span;i++) {
                        bufferpointer=position+i-dataSegmentStart;
                        if (bufferpointer>=0 && bufferpointer<buffer.length) buffer[bufferpointer]=numericalValue;
                    }
                    position+=step;
                } else { // variableStep
                    if (values.length!=2) throw new ParseError("Expected 2 values per line for variableStep Wiggle format (found "+values.length+"): "+line);
                    double numericalValue=0;
                    int pos=0;
                    try {
                        pos=Integer.parseInt(values[0]);
                        numericalValue=Double.parseDouble(values[1]);
                    } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric values: "+values[0]+" or "+values[1]);}
                    int bufferpointer=0;
                    for (int i=0;i<span;i++) {
                        bufferpointer=pos+i-dataSegmentStart;
                        if (bufferpointer>=0 && bufferpointer<buffer.length) buffer[bufferpointer]=numericalValue;
                    }              
                }
                
            }

        }
        target.setSegmentData(buffer);
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
