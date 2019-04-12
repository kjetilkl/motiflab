/*
 
 
 */

package motiflab.engine.dataformat;


import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Parameter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.protocol.ParseError;


/**
 * A formatter/parser for the MultiFASTA format used by ChIPMunk.
 * The reason that the class is called MFASTA and not MultiFASTA is
 * because of some SVN problems with my original file (which was in
 * fact called DataFormat_MultiFASTA), so I just deleted that file
 * and created a totally new one.
 * 
 * @author kjetikl
 */
public class DataFormat_MFASTA extends DataFormat {
    private String name="MultiFASTA";
    private Class[] supportedTypes=new Class[]{};

    private static final String PARAMETER_FORMAT="Format";
    private static final String PARAMETER_WEIGHTS_DATA="WeightsData";
    private static final String PARAMETER_PEAKS_DATA="PeaksData";
    private static final String PARAMETER_STRAND_ORIENTATION="Strand orientation";
    private static final String PARAMETER_SIMPLE="Simple";
    private static final String PARAMETER_SINGLE_STRAND="Single strand";    
    private static final String PARAMETER_WEIGHTED="Weighted";
    private static final String PARAMETER_PEAK="Peak";
    
    public DataFormat_MFASTA() {
        addParameter(PARAMETER_FORMAT, PARAMETER_SIMPLE, new String[]{PARAMETER_SIMPLE,PARAMETER_SINGLE_STRAND,PARAMETER_WEIGHTED,PARAMETER_PEAK},"Specifies what data to include in the header. Simple=just sequence index, Single strand=also just sequence index, weighted=different weight for each sequence, peak=different weights for each position in the sequence");
        addOptionalParameter(PARAMETER_WEIGHTS_DATA, null, new Class[]{SequenceNumericMap.class},"If you select the 'weights' format, you must select a Sequence Numeric Map here");
        addOptionalParameter(PARAMETER_PEAKS_DATA, null, new Class[]{NumericDataset.class},"If you select the 'peaks' format, you must select a Numeric Dataset here");
        addParameter(PARAMETER_STRAND_ORIENTATION, "Relative", new String[]{"Relative","Direct","Reverse"},null);        
        setParameterFilter(PARAMETER_FORMAT,"output");             
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof DNASequenceDataset);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(DNASequenceDataset.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return false;// (data instanceof DNASequenceData || data instanceof DNASequenceDataset || data instanceof SequenceCollection);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return false;//(dataclass.equals(DNASequenceData.class) || dataclass.equals(DNASequenceDataset.class) || dataclass.equals(SequenceCollection.class));
    }

    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "mfa";
    }
    @Override
    public String[] getSuffixAlternatives() {return new String[]{"mfa","fas","fasta"};}

    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        String orientation;
        String headerformat=null;
        Object headerdata=null;
        SequenceNumericMap map=null;
        NumericDataset peaks=null;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);                       
              headerformat=(String)settings.getResolvedParameter(PARAMETER_FORMAT,defaults,engine);
              map=(SequenceNumericMap)settings.getResolvedParameter(PARAMETER_WEIGHTS_DATA,defaults,engine);
              peaks=(NumericDataset)settings.getResolvedParameter(PARAMETER_PEAKS_DATA,defaults,engine);            
           } catch (ExecutionError e) {
              throw e;
           } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
           }
        } else {
            orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
            headerformat=(String)getDefaultValueForParameter(PARAMETER_FORMAT);
        }
        if (headerformat.equalsIgnoreCase(PARAMETER_SIMPLE) || headerformat.equalsIgnoreCase(PARAMETER_SINGLE_STRAND) || headerformat.equalsIgnoreCase("s") || headerformat.equalsIgnoreCase("r")) {
            headerdata=null;
        } else if (headerformat.equalsIgnoreCase(PARAMETER_WEIGHTED) || headerformat.equalsIgnoreCase("w")) {
           if (map==null) throw new ExecutionError("No weights specified");
           else headerdata=map;
        } else if (headerformat.equals(PARAMETER_PEAK) || headerformat.equalsIgnoreCase("p")) {
           if (peaks==null) throw new ExecutionError("No peaks data specified");
           else headerdata=peaks; 
        }

        String outputString="";
        if (dataobject instanceof DNASequenceDataset) {
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
            outputString=outputMultipleSequences((DNASequenceDataset)dataobject, sequenceCollection, orientation, headerdata, task, engine);
//        } else if (dataobject instanceof DNASequenceData){
//            StringBuilder builder=new StringBuilder();
//            outputSequence((DNASequenceData)dataobject, orientation, headerdata, engine, builder);
//            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.trim()+"\n",getName());
        setProgress(100);
        return outputobject;
    }    
    
    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(DNASequenceDataset sourcedata, SequenceCollection collection, String orientation, Object headerdata, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              DNASequenceData sourceSequence=(DNASequenceData)sourcedata.getSequenceByName(sequenceName);
              int sequenceIndex=collection.getIndexForSequence(sequenceName);
              outputSequence(sourceSequence, sequenceIndex, orientation, headerdata, engine,outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }
    
    
    
    
    /** outputformats a single sequence */
    private void outputSequence(DNASequenceData data, int sequenceIndex, String orientation, Object headerdata, MotifLabEngine engine, StringBuilder outputString) {
        String sequence;
        if (orientation==null) orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
        if (orientation.equals("Reverse") || ((orientation.equals("Relative")||orientation.equals("From Sequence")||orientation.equals("From Gene")) && data.getStrandOrientation()==Sequence.REVERSE)) {
            sequence=MotifLabEngine.reverseSequence(data.getSequenceAsString());
        } else {
            sequence=data.getSequenceAsString();
        }
        outputString.append(">");
        if (headerdata==null) {
            outputString.append(sequenceIndex);
        } else if (headerdata instanceof SequenceNumericMap) {
            double weight=((SequenceNumericMap)headerdata).getValue(data.getSequenceName());
            outputString.append(weight);
        } else if (headerdata instanceof NumericDataset) {
            NumericSequenceData seqdata=(NumericSequenceData)((NumericDataset)headerdata).getSequenceByName(data.getSequenceName());
            double[] dataarray=(double[])seqdata.getValue();
            boolean reverse=false;
            if (orientation.equals("Reverse") || ((orientation.equals("Relative")||orientation.equals("From Sequence")||orientation.equals("From Gene")) && data.getStrandOrientation()==Sequence.REVERSE)) {
                reverse=true;
            } 
            for (int i=0;i<dataarray.length;i++) {
                if (i>0) outputString.append(" ");
                outputString.append((reverse)?dataarray[(dataarray.length-1)-i]:dataarray[i]);
            }              
        } else outputString.append("ERROR");
        outputString.append("\n");
        outputString.append(sequence);
        outputString.append("\n");          
    }

        
    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("MotifLab is currently unable to parse MultiFasta files");
    }
  
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("MotifLab is currently unable to parse MultiFasta files");
    }
     
}

        
       
        
        
