/*


 */

package org.motiflab.engine.dataformat;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_PSP_binned extends DataFormat {
    private String name="PSP_binned";
    private Class[] supportedTypes=new Class[]{};

    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE="Relative";
    private static final String OPPOSITE="Opposite";
    private static final String PARAMETER_STRAND_ORIENTATION="Orientation";
    private static final String MOTIF_WIDTH="Motif width";
    private static final String NORMALIZE="Normalize";
    private static final String NORMALIZE_SUM_TO_1="Sum to 1";
    private static final String NORMALIZE_0_TO_1="Max 1";
    private static final String BINS="Bins";

    public DataFormat_PSP_binned() {
        addOptionalParameter(NORMALIZE, "", new String[]{"",NORMALIZE_SUM_TO_1,NORMALIZE_0_TO_1},"Perform normalization on values");
        addOptionalParameter(BINS, new Integer(50), new Integer[]{1,Integer.MAX_VALUE},"");
        addParameter(PARAMETER_STRAND_ORIENTATION, RELATIVE, new String[]{DIRECT,REVERSE,RELATIVE,OPPOSITE},"Sequence orientation");
        addParameter(MOTIF_WIDTH, new Integer(8), new Integer[]{5,50},"Width of motif");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
       //  return (data instanceof NumericDataset); 
       return false; // I return false since I do not want this format to be visible to users
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
       //   return (dataclass.equals(NumericDataset.class));
       return false; // I return false since I do not want this format to be visible to users
    }

    @Override
    public boolean canParseInput(Data data) {
        return false;
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return false;
    }

    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "pspbin";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!(dataobject instanceof NumericDataset)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        int bins=50;
        String orientation;
        int motifwidth=8;
        setProgress(5);
        String normalizeString="";
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             normalizeString=(String)settings.getResolvedParameter(NORMALIZE,defaults,engine);
             bins=(Integer)settings.getResolvedParameter(BINS,defaults,engine);
             orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);
             motifwidth=(Integer)settings.getResolvedParameter(MOTIF_WIDTH,defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           normalizeString=(String)getDefaultValueForParameter(NORMALIZE);
           bins=(Integer)getDefaultValueForParameter(BINS);
           orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
           motifwidth=(Integer)getDefaultValueForParameter(MOTIF_WIDTH);
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
            outputString=outputBinnedValues((NumericDataset)dataobject, sequenceCollection, bins, normalizeString, orientation, motifwidth, task, engine);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }


    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputBinnedValues(NumericDataset sourcedata, SequenceCollection collection, int bins, String normalize, String orientationString, int motifwidth, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {

        DecimalFormat decimalformatter=new DecimalFormat("0.00000");
        StringBuilder outputString=new StringBuilder();
        int size=collection.getNumberofSequences();
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);

        boolean normalizeSumTo1=false;
        boolean normalize0To1=false;
        if (normalize.equals(NORMALIZE_SUM_TO_1)) normalizeSumTo1=true;
        else if (normalize.equals(NORMALIZE_0_TO_1)) normalize0To1=true;
        double minPrior=1.0;
        double maxPrior=0;
        int[] bincounts=new int[bins];
        double[] normalizationFactor=new double[sequences.size()];
        double[] minValues=new double[sequences.size()];
        double[] maxValues=new double[sequences.size()];
        for (int j=0;j<sequences.size();j++) {
            normalizationFactor[j]=1.0;
        }
        int i=0;
        if (sequences.isEmpty()) return "";
        for (Sequence sequence:sequences) { // for each sequence
              int shownStrand=getStrand(orientationString, sequence);
              String sequenceName=sequence.getName();
              double sequencemin=1.0;
              double sequencemax=0;
              double sum=0;
              NumericSequenceData sourceSequence=(NumericSequenceData)sourcedata.getSequenceByName(sequenceName);
              int sequenceSize=sourceSequence.getSize();
              for (int pos=0;pos<sequenceSize;pos++) {
                 double value=sourceSequence.getValueAtRelativePosition(pos);
                 if (shownStrand==Sequence.DIRECT) {
                     if (pos>=sequenceSize-motifwidth+1) value=0; // padding with 0 last (motifwidth-1) positions
                 } else {
                     if (pos<motifwidth-1) value=0;
                 }
                 if (value==0) continue;
                 if (value<sequencemin) sequencemin=value;
                 if (value>sequencemax) sequencemax=value;
                 sum+=value;
              }
              minValues[i]=sequencemin;
              maxValues[i]=sequencemax;
              if (normalize0To1) {
                 if (sequencemax>normalizationFactor[i]) normalizationFactor[i]=sequencemax; // when normalizing: divide each value by largest value
              } else if (normalizeSumTo1) {                
                  if (sum>normalizationFactor[i]) normalizationFactor[i]=sum; // when normalizing: divide each value by total sum
              }           
              minValues[i]=minValues[i]/normalizationFactor[i];
              maxValues[i]=maxValues[i]/normalizationFactor[i];
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        for (int j=0;j<sequences.size();j++) {
            if (minValues[j]<minPrior) minPrior=minValues[j];
            if (maxValues[j]>maxPrior) maxPrior=maxValues[j];
        }
        int count=0;
        double scale=(double)(bins-1)/(maxPrior-minPrior); // the -1 term was missing in MEME 4.8.0 
        //System.err.println("scale="+scale+"  bins="+bins+"  maxPrior="+maxPrior+"   minPrior="+minPrior+"  range="+(maxPrior-minPrior));
        i=0;
        for (Sequence sequence:sequences) { // for each sequence
              int shownStrand=getStrand(orientationString, sequence);
              String sequenceName=sequence.getName();
              NumericSequenceData sourceSequence=(NumericSequenceData)sourcedata.getSequenceByName(sequenceName);
              int sequenceSize=sourceSequence.getSize();
              for (int pos=0;pos<sequenceSize;pos++) {
                 double value=sourceSequence.getValueAtRelativePosition(pos)/normalizationFactor[i];
                 if (shownStrand==Sequence.DIRECT) {
                     if (pos>=sequenceSize-motifwidth+1) value=0; // padding with 0
                 } else {
                     if (pos<motifwidth-1) value=0; // padding with 0
                 }
                 count++;
                 int binIndex=(int)Math.round((value-minPrior)*scale);
//                 if (binIndex==bincounts.length) binIndex--;
                 if (binIndex<0) binIndex=0;
                 bincounts[binIndex]++;
                 //System.err.println("value="+value+"  minPrior="+minPrior+"  scale="+scale+"  binIndex="+binIndex+"  motifwidth="+motifwidth);
              }
              i++;
        }
        outputString.append(decimalformatter.format(minPrior));
        outputString.append("\n");
        outputString.append(decimalformatter.format(maxPrior));
        outputString.append("\n");
        if (maxPrior==minPrior) {
             outputString.append(1.0);
             outputString.append("\n");
        } else {
            for (int j=0;j<bins;j++) {
                outputString.append(decimalformatter.format(((double)bincounts[j]/(double)count)));
                outputString.append("\n");
            }
        }
        return outputString.toString();
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Unable to parseInput with PSP_binned");
    }

    private int getStrand(String orientationString, Sequence sequence) {
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
         return shownStrand;
    }

 
}
