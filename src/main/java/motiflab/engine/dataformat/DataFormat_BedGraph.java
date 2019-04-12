/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Sequence;
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
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.SequenceCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_BedGraph extends DataFormat {
    private String name="BedGraph";

    private Class[] supportedTypes=new Class[]{NumericSequenceData.class, NumericDataset.class};

    public DataFormat_BedGraph() {
        addOptionalParameter("Add CHR prefix", "yes", new String[]{"yes","no"},"If selected, the prefix 'chr' will be added before the chromosome number (e.g. chromosome '12' will be output as 'chr12'.");       
        addAdvancedParameter("Coordinate system", "BED", new String[]{"BED","GFF"},"In the default BED coordinate-system the chromosome starts at position 0 and the end-coordinate is exclusive, whereas in the GFF-system the chromosome starts at 1 and the end-coordinate is inclusive.");
        setParameterFilter("Add CHR prefix","output");       
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
        return "bdg";
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
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             String addPrefixString=(String)settings.getResolvedParameter("Add CHR prefix",defaults,engine); 
             String coordinateSystem=(String)settings.getResolvedParameter("Coordinate system",defaults,engine);
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
           if (addPrefixString!=null && addPrefixString.equalsIgnoreCase("no")) addPrefix=false;
           if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
                 startpos=1;
                 exclusiveEnd=false;
           }          
        }
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
            outputString=outputMultipleSequences((NumericDataset)dataobject, sequenceCollection, addPrefix, startpos, exclusiveEnd, task, engine);
        } else if (dataobject instanceof NumericSequenceData){
            String featureName=((NumericSequenceData)dataobject).getParent().getName();
            StringBuilder builder=new StringBuilder("track type=bedGraph name=\""+featureName+"\"\n");
            outputSequenceData((NumericSequenceData)dataobject, addPrefix, startpos, exclusiveEnd, task, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(NumericDataset sourcedata, SequenceCollection collection, boolean addPrefix, int startpos, boolean exclusiveEnd, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        String featureName=sourcedata.getName();
        StringBuilder outputString=new StringBuilder("track type=bedGraph name=\""+featureName+"\"\n");
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence
              String sequenceName=sequence.getName();
              if (sourcedata!=null) { // output NumericDataset
                  NumericSequenceData sourceSequence=(NumericSequenceData)sourcedata.getSequenceByName(sequenceName);
                  outputSequenceData(sourceSequence, addPrefix, startpos, exclusiveEnd, task, outputString);
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
    private void outputSequenceData(NumericSequenceData sequence, boolean addPrefix, int startpos, boolean exclusiveEnd, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
        String chromosome=sequence.getChromosome();
        if (addPrefix && !chromosome.startsWith("chr")) chromosome="chr"+chromosome;        
        int sequencestart=sequence.getRegionStart();
        int sequenceend=sequence.getRegionEnd();
        double runningvalue=Double.NaN;
        int runStart=-1;
        for (int i=sequencestart;i<=sequenceend+1;i++) { // note that this extends one beyond the end of the sequence
            double value=(i<=sequenceend)?sequence.getValueAtGenomicPosition(i):Double.NaN;
            if (value!=runningvalue || i==sequenceend+1) { // start new run and maybe output previous
                if (runStart>=sequencestart && runningvalue!=0 && !Double.isNaN(runningvalue)) { // output previous run, unless the value is 0 (in which case it is implicit)
                    int runEnd=(exclusiveEnd)?i:i-1;
                    outputString.append(chromosome);       
                    outputString.append("\t");
                    outputString.append((startpos==0)?(runStart-1):runStart); // subtract 1 if 0-indexed
                    outputString.append("\t");
                    outputString.append((startpos==0)?(runEnd-1):runEnd); // subtract 1 if 0-indexed                  
                    outputString.append("\t"); 
                    outputString.append(runningvalue); 
                    outputString.append("\n"); 
                    
                }
                // initialize new run
                runningvalue=value;
                runStart=i;               
            }          
        }
        // output the last run
    }
    
    /** output-formats a single sequence */
 

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
        if (target==null) target=new NumericDataset("temporary");
        if (target instanceof NumericDataset) { // The NumericDataset might not contain NumericSequenceData objects for all Sequences. Add them if they are missing!
            NumericDataset dataset=(NumericDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new NumericSequenceData((Sequence)seq,0));
            }
        }
        int startpos=0;
        boolean exclusiveEnd=true;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             String coordinateSystem=(String)settings.getResolvedParameter("Coordinate system",defaults,engine);
             if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
                 startpos=1;
                 exclusiveEnd=false;
             }  
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           String coordinateSystem=(String)getDefaultValueForParameter("Coordinate system");
           if (coordinateSystem!=null && (coordinateSystem.equalsIgnoreCase("gff") || coordinateSystem.equalsIgnoreCase("regular"))) {
                 startpos=1;
                 exclusiveEnd=false;
           }  
        }   
        int count=0;
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%100==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               if (count%500==0) Thread.yield();
            }
            if (line.startsWith("#") || line.isEmpty()) continue; // assume comment line
            Object[] map=parseSingleLine(line,startpos,exclusiveEnd);
            String chromosome=(String)map[0];
            int regionstart=(Integer)map[1];
            int regionend=(Integer)map[2];
            double value=(Double)map[3];
            NumericSequenceData targetSequence=null;
            if (target instanceof NumericSequenceData) {
               targetSequence=(NumericSequenceData)target;
               if (!targetSequence.getChromosome().equals(chromosome)) continue;
               if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
               addRegionToTarget(targetSequence,map);
            } else if (target instanceof NumericDataset) { // add values to all applicable sequences (those that cover the genomic region)
                ArrayList<FeatureSequenceData> sequences=((NumericDataset)target).getAllSequences();
                for (FeatureSequenceData seq:sequences) {
                    targetSequence=(NumericSequenceData)seq;
                    if (!targetSequence.getChromosome().equals(chromosome)) continue;
                    if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
                    addRegionToTarget(targetSequence,map);
                }
            } else if (target instanceof DataSegment) {
                addRegionToTarget(target,map);
            } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-numeric track as target for BedGraph dataformat: "+target.getClass().getSimpleName());
        }        
        return target;
    }


    /** parses a single line in a BedGraph-file and returns an array containing in order: chromosome (String, without CHR-prefix), region start (Integer, GFF-coordinates), region end (Integer, GFF-coordinates), value (Double) */
    private Object[] parseSingleLine(String line, int startpos, boolean exclusiveEnd) throws ParseError {
        String[] fields=line.split("\t");
        if (fields.length!=4) throw new ParseError("Expected 4 fields per line in BedGraph-format, but found "+fields.length);
        String chromosome=fields[0];
        if (chromosome.startsWith("chr")) chromosome=chromosome.substring(3);
        int start=0;
        int end=0;
        double value=0;
        try {
            start=Integer.parseInt(fields[1]);
            if (startpos==0) start++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START coordinate: "+fields[1]);}        
        try {
            end=Integer.parseInt(fields[2]);
            if (startpos==0) end++; // if BED is zero-indexed, convert to one-indexing which is used by MotifLab
            if (exclusiveEnd) end--; // substract 1 because the end-coordinate in BED-files are non-inclusive        
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END coordinate: "+fields[2]);}        
        try {
            value=Double.parseDouble(fields[3]);
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value: "+fields[3]);}        
        Object[] result=new Object[]{chromosome,start,end,value};
        return result;
    }
    
    /**
     * 
     * @param target This would be a NumericSequenceData object or DataSegment
     * @param map
     * @throws ParseError 
     */
    private void addRegionToTarget(Object target, Object[] map) throws ParseError {
        String chromosome=(String)map[0];
        int start=(Integer)map[1]; // these are here genomic (GFF-coordinates)
        int end=(Integer)map[2]; // these are here genomic (GFF-coordinates)
        double value=(Double)map[3];
        if (target instanceof NumericSequenceData) {
            for (int i=start;i<=end;i++) {
                ((NumericSequenceData)target).setValueAtGenomicPosition(i, value);
            }
        }
        else if (target instanceof DataSegment) {
            int targetStart=((DataSegment)target).getSegmentStart();
            start-=targetStart; // convert to relative coordinates
            end-=targetStart;            
            ((DataSegment)target).addNumericValue(start,end,value);
        } else throw new ParseError("Target object neither NumericSequenceData nor DataSegment in DataFormat_BedGraph.addRegionToTarget():"+target.toString());
    }

}





