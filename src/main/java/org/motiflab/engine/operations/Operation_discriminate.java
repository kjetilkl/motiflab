
package org.motiflab.engine.operations;

import java.util.HashMap;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class Operation_discriminate extends FeatureTransformOperation {
    public static final String POSITIVE_SET="positiveSequences";
    private static final String POSITIVE_SEQUENCES="positiveSequencesData";
    public static final String NEGATIVE_SET="negativeSequences";
    public static final String DNA_SEQUENCE="DNADataset";
    public static final String WORD_SIZE="wordSize";
    public static final String ORIENTATION="orientation";
    public static final String ORIENTATION_DIRECT="direct";
    public static final String ORIENTATION_RELATIVE="relative";
    public static final String ORIENTATION_BOTH="both";
    public static final String ANCHOR="anchor";
    public static final String ANCHOR_START="start";
    public static final String ANCHOR_RELATIVE_START="relative start";

    private static final String DNA_SEQUENCE_DATA="DNADatasetData";
    private static final String WORD_SIZE_INTEGER="wordSizeInteger";
    private static final String POSITIVE_SCORES="positiveScores";
    private static final String NEGATIVE_SCORES="negativeScores";    
    
    private static final String name="discriminate";
    private static final String description="Converts a positional priors track into a discriminative prior by considering the priors values or W-mer words in positive and negative sequences";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class};
  
    private int pos_pseudo=1;
    private int neg_pseudo=9;
    
    
  
    @Override
    public String getOperationGroup() {
        return "Transform";
    }

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }
    @Override
    public boolean isSubrangeApplicable() {return true;}
    
    @Override
    public String toString() {return name;}
    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        task.setStatusMessage("Executing operation: "+task.getOperationName());
        task.setProgress(5);        
        String positiveSequences=(String)task.getParameter(POSITIVE_SET);
        Data positiveData=engine.getDataItem(positiveSequences);
        if (positiveData==null) throw new ExecutionError("Unrecognized data object '"+positiveSequences+"'",task.getLineNumber());          
        if (!(positiveData instanceof SequenceCollection)) throw new ExecutionError("'"+positiveSequences+"' is not a Sequence Collection",task.getLineNumber());          
        task.setParameter(POSITIVE_SEQUENCES, positiveData);
        
        String negativeSequences=(String)task.getParameter(NEGATIVE_SET);
        Data negativeData=engine.getDataItem(negativeSequences);
        if (negativeData==null) throw new ExecutionError("Unrecognized data object '"+negativeSequences+"'",task.getLineNumber());          
        if (!(negativeData instanceof SequenceCollection)) throw new ExecutionError("'"+negativeSequences+"' is not a Sequence Collection",task.getLineNumber());          

        String dnaSequences=(String)task.getParameter(DNA_SEQUENCE);
        Data dnaData=engine.getDataItem(dnaSequences);
        if (dnaData==null) throw new ExecutionError("Unrecognized data object '"+dnaSequences+"'",task.getLineNumber());          
        if (!(dnaData instanceof DNASequenceDataset)) throw new ExecutionError("'"+dnaSequences+"' is not a DNA Sequence Dataset",task.getLineNumber());          
        task.setParameter(DNA_SEQUENCE_DATA, dnaData);
 
        String wordSizeString=(String)task.getParameter(WORD_SIZE);
        Data wordSizeData=engine.getDataItem(wordSizeString);
        int wordsize=0;
        if (wordSizeData==null) {
            try {
              wordsize=Integer.parseInt(wordSizeString);
           } catch (NumberFormatException e) {throw new ExecutionError("'"+wordSizeString+"' should be either an integer constant or a Numeric Variable",task.getLineNumber());}         
        } else {
          if (wordSizeData instanceof NumericVariable) wordsize=((NumericVariable)wordSizeData).getValue().intValue();
          else throw new ExecutionError("'"+wordSizeString+"' should be either an integer constant or a Numeric Variable",task.getLineNumber());
        }
        if (wordsize<=0 || wordsize>12) throw new ExecutionError("The word-size should be a number between 1 and 12 (found "+wordsize+")",task.getLineNumber());
        task.setParameter(WORD_SIZE_INTEGER, new Integer(wordsize));   

        String orientation=(String)task.getParameter(ORIENTATION);
        String anchor=(String)task.getParameter(ANCHOR);
        // calculate frequencies here and put them into the task to be used below!!...       
        HashMap<String,Double> positiveScores=getWmerSums((SequenceCollection)positiveData,(DNASequenceDataset)dnaData, (NumericDataset)task.getSourceData(), wordsize, orientation, anchor, false); // sum of scores for each W-mer in positive sequences
        HashMap<String,Double> negativeScores=getWmerSums((SequenceCollection)negativeData,(DNASequenceDataset)dnaData, (NumericDataset)task.getSourceData(), wordsize, orientation, anchor, false); // sum of scores for each W-mer in negative sequences
//        HashMap<String,Integer> positiveScores=getWmerCounts((SequenceCollection)positiveData,(DNASequenceDataset)dnaData, wordsize, orientation); // sum of scores for each W-mer in positive sequences
//        HashMap<String,Integer> negativeScores=getWmerCounts((SequenceCollection)negativeData,(DNASequenceDataset)dnaData, wordsize, orientation); // sum of scores for each W-mer in negative sequences
     
        task.setParameter(POSITIVE_SCORES, positiveScores);
        task.setParameter(NEGATIVE_SCORES, negativeScores);
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String seqName=sourceSequence.getName();
        SequenceCollection positiveSequences=(SequenceCollection)task.getParameter(POSITIVE_SEQUENCES);
        if (!positiveSequences.contains(seqName)) { // set values in negative sequences to 'all 0'.
            clearSequence((NumericSequenceData)targetSequence);
            return;
        }
        
        HashMap<String,Double> positiveScores=(HashMap<String,Double>)task.getParameter(POSITIVE_SCORES);
        HashMap<String,Double> negativeScores=(HashMap<String,Double>)task.getParameter(NEGATIVE_SCORES);
        DNASequenceDataset dnaDataset=(DNASequenceDataset)task.getParameter(DNA_SEQUENCE_DATA);
        DNASequenceData dnaSequence=(DNASequenceData)dnaDataset.getSequenceByName(seqName);
        int wordsize=(Integer)task.getParameter(WORD_SIZE_INTEGER);
        boolean directStrand=true;
        boolean anchorAtEnd=false;
        String orientation=(String)task.getParameter(ORIENTATION);
        String anchor=(String)task.getParameter(ANCHOR);
        if (orientation!=null && orientation.equalsIgnoreCase(ORIENTATION_RELATIVE) && !sourceSequence.isOnDirectStrand()) directStrand=false;
        if (anchor!=null && anchor.equalsIgnoreCase(ANCHOR_RELATIVE_START) && !sourceSequence.isOnDirectStrand()) anchorAtEnd=true;

        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd()-wordsize+1;i++) {
            //if (positionSatisfiesCondition(seqName,i,task)) {
                char[] dna = (char[])dnaSequence.getValueInGenomicInterval(i, i+wordsize-1);
                String dnaString=new String((directStrand)?dna:MotifLabEngine.reverseSequence(dna));
                Double posScore=positiveScores.get(dnaString);
                Double negScore=negativeScores.get(dnaString);
                if (posScore==null) posScore=0.0;
                if (negScore==null) negScore=0.0;
                double value=0;
                //if (posScore+negScore!=0) value=posScore/(posScore+negScore);
                value=(double)(posScore+pos_pseudo)/(double)(posScore+negScore+pos_pseudo+neg_pseudo);
                int position=(anchorAtEnd)?(i+wordsize-1):i;
                ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(position,value);
            //}
        }
        // fill in with 0s at the 'end' of the squence
        int padStart=(anchorAtEnd)?sourceSequence.getRegionStart():(sourceSequence.getRegionEnd()-wordsize+2);
        int padEnd=(anchorAtEnd)?(sourceSequence.getRegionStart()+wordsize-2):(sourceSequence.getRegionEnd());
        for (int i=padStart;i<=padEnd;i++) {
            if (positionSatisfiesCondition(seqName,i,task)) {
                ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i,0);
            }
        }

     }

    
     HashMap<String,Double> getWmerSums(SequenceCollection collection, DNASequenceDataset dnadataset, NumericDataset feature, int wordsize, String orientation, String anchor, boolean normalize) {
         HashMap<String,Double> sumScores=new HashMap<String,Double>();
         int totalbases=0;
         for (String seqName:collection.getAllSequenceNames()) {
            DNASequenceData dnaSequence=(DNASequenceData)dnadataset.getSequenceByName(seqName);
            NumericSequenceData featureSequence=(NumericSequenceData)feature.getSequenceByName(seqName);
            boolean directStrand=true;
            boolean anchorAtEnd=false;
            if (orientation!=null && orientation.equalsIgnoreCase(ORIENTATION_RELATIVE) && !featureSequence.isOnDirectStrand()) directStrand=false;
            if (anchor!=null && anchor.equalsIgnoreCase(ANCHOR_RELATIVE_START) && !featureSequence.isOnDirectStrand()) anchorAtEnd=true;
            totalbases+=(dnaSequence.getSize()-wordsize+1);
            for (int i=0;i<=dnaSequence.getSize()-wordsize;i++) {
                char[] dna = (char[])dnaSequence.getValueInInterval(i, i+wordsize-1);
                if (orientation.equalsIgnoreCase(ORIENTATION_BOTH)) {
                    int position=(anchorAtEnd)?(i+wordsize-1):i;
                    double value=featureSequence.getValueAtRelativePosition(position); //
                    String dnaString=new String(dna);                    
                    if (sumScores.containsKey(dnaString)) {
                        sumScores.put(dnaString,value+sumScores.get(dnaString));
                    } else sumScores.put(dnaString,value); 
                    String revdnaString=new String(MotifLabEngine.reverseSequence(dna));    
                    if (!revdnaString.equals(dnaString)) {
                        if (sumScores.containsKey(revdnaString)) {
                            sumScores.put(revdnaString,value+sumScores.get(revdnaString));
                        } else sumScores.put(revdnaString,value);    
                    }
                } else if (orientation.equalsIgnoreCase(ORIENTATION_DIRECT)) {
                    String dnaString=new String((featureSequence.isOnDirectStrand())?dna:MotifLabEngine.reverseSequence(dna));
                    int position=(anchorAtEnd)?(i+wordsize-1):i;
                    double value=featureSequence.getValueAtRelativePosition(position); //
                    if (sumScores.containsKey(dnaString)) {
                        sumScores.put(dnaString,value+sumScores.get(dnaString));
                    } else sumScores.put(dnaString,value);                   
                } else if (orientation.equalsIgnoreCase(ORIENTATION_RELATIVE)) {
                    String dnaString=new String((directStrand)?dna:MotifLabEngine.reverseSequence(dna));
                    int position=(anchorAtEnd)?(i+wordsize-1):i;
                    double value=featureSequence.getValueAtRelativePosition(position); //
                    if (sumScores.containsKey(dnaString)) {
                        sumScores.put(dnaString,value+sumScores.get(dnaString));
                    } else sumScores.put(dnaString,value);                      
                }

            }
        }
        if (normalize) {
            for (String key:sumScores.keySet()) {
                 sumScores.put(key,sumScores.get(key)/(double)totalbases);
            }
        }
        return sumScores;
     }
     
//     HashMap<String,Integer> getWmerCounts(SequenceCollection collection, DNASequenceDataset dnadataset, int wordsize, String orientation) {
//         HashMap<String,Integer> counts=new HashMap<String,Integer>();
//         for (String seqName:collection.getAllSequenceNames()) {
//            DNASequenceData dnaSequence=(DNASequenceData)dnadataset.getSequenceByName(seqName);
//            boolean sequenceOnDirectStrand=dnaSequence.isOnDirectStrand();
//            for (int i=0;i<=dnaSequence.getSize()-wordsize;i++) {
//                char[] dna = (char[])dnaSequence.getValueInInterval(i, i+wordsize-1);
//                if (orientation.equalsIgnoreCase(ORIENTATION_BOTH)) {
//                    String dnaString=new String(dna);
//                    if (counts.containsKey(dnaString)) counts.put(dnaString,counts.get(dnaString)+1);
//                    else counts.put(dnaString,1);    
//                    dnaString=MotifLabEngine.reverseSequence(dnaString);
//                    if (counts.containsKey(dnaString)) counts.put(dnaString,counts.get(dnaString)+1);
//                    else counts.put(dnaString,1);                                     
//                } else if (orientation.equalsIgnoreCase(ORIENTATION_DIRECT)) {
//                    String dnaString=new String(dna);
//                    if (counts.containsKey(dnaString)) counts.put(dnaString,counts.get(dnaString)+1);
//                    else counts.put(dnaString,1);                       
//                } else if (orientation.equals(ORIENTATION_RELATIVE)) {
//                    String dnaString=new String((sequenceOnDirectStrand)?dna:MotifLabEngine.reverseSequence(dna));
//                    if (counts.containsKey(dnaString)) counts.put(dnaString,counts.get(dnaString)+1);
//                    else counts.put(dnaString,1);                       
//                }
//            }
//        }
//        return counts;
//     }     
  
     private void clearSequence(NumericSequenceData sequence) {
          double[] data=sequence.getData();
          for (int i=0;i<data.length;i++) data[i]=0;         
     }
}
