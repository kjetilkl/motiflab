/*
 
 
 */

package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;
import motiflab.engine.data.FeatureSequenceData;

/**
 *
 * @author kjetikl
 */
public class Operation_mask extends FeatureTransformOperation {
    public static final int UPPERCASE=0;
    public static final int LOWERCASE=1;
    public static final int LETTER=2;
    public static final int RANDOM_BASES=3;   
    public static final int REGION_SEQUENCE_PROPERTY=4;      
    public static final String MASKTYPE="masktype";
    public static final String MASK_STRING="masktype"; // the string describing the masking type to use: 'X' | uppercase | lowercase | Background model name
    public static final String BACKGROUND_MODEL="BackgroundModel";
    public static final String REGION_DATASET="RegionDataset";    
    public static final String MASK_LETTER="maskletter";
    public static final String STRAND="strandOrientation";
    
    private static final String name="mask";
    private static final String description="Masks selected bases using either lowercase/uppercase, a specified letter or random bases sampled from a background model";
    private Class[] datasourcePreferences=new Class[]{DNASequenceDataset.class};

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
    public String toString() {return name;}
    
    @Override
    public boolean isSubrangeApplicable() {return true;}
    
    
    @Override
    public boolean canUseAsSourceProxy(Data object) {
        return (object instanceof RegionDataset || object instanceof BackgroundModel);
    }    
    
    @Override
    public boolean assignToProxy(Object proxysource, OperationTask operationtask) {
        Data proxy=null;
        if (proxysource instanceof Data) proxy=(Data)proxysource;
        else if (proxysource instanceof Data[] && ((Data[])proxysource).length>0) {
            for (Data data:(Data[])proxysource) {
                if (data instanceof BackgroundModel) {proxy=data;break;}
            }
        }
        if (proxy instanceof RegionDataset) {
          Condition_position condition = new Condition_position();
          condition.setOperandAString(proxy.getName());
          condition.setComparator("inside");
          operationtask.setParameter("where", condition);
          return true;
        } else if (proxy instanceof BackgroundModel) {
          operationtask.setParameter(MASK_STRING,proxy.getName());
          return true;
        } else return false;
    }      
    
    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        Object object=task.getParameter(Operation_mask.MASK_STRING);
        String maskString=(object!=null)?maskString=object.toString():null;
        if (maskString==null || maskString.trim().isEmpty()) throw new ExecutionError("Missing mask specification");
        if (maskString.equalsIgnoreCase("lowercase")) task.setParameter(MASKTYPE, LOWERCASE);
        else if (maskString.equalsIgnoreCase("uppercase")) task.setParameter(MASKTYPE, UPPERCASE);
        else if (maskString.startsWith("'") || maskString.startsWith("\"")) {
            char maskletter;
            if (maskString.length()!=3 || !(maskString.endsWith("'") || maskString.endsWith("\""))) throw new ExecutionError("Mask letter should be one character surrounded by quotes");
            else maskletter=maskString.charAt(1);
            task.setParameter(MASKTYPE, LETTER);
            task.setParameter(MASK_LETTER, new Character(maskletter));
        } else {
            Object data=engine.getDataItem(maskString);
            if (data==null) throw new ExecutionError("Unknown data object: "+maskString);
            if (data instanceof BackgroundModel) {           
                task.setParameter(MASKTYPE, RANDOM_BASES);
                task.setParameter(BACKGROUND_MODEL, data);
            } else if (data instanceof RegionDataset) {
                task.setParameter(MASKTYPE, REGION_SEQUENCE_PROPERTY);
                task.setParameter(REGION_DATASET, data);
            } else throw new ExecutionError(maskString+" is not a Background model or Region Dataset");  
        }        
    }

    double[] debug=new double[16];
    int[] debugcounts=new int[4];
    int seqs=0;
        
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        char letter='X';
        int masktype=LOWERCASE;
        BackgroundModel backgroundModel=(BackgroundModel)task.getParameter(Operation_mask.BACKGROUND_MODEL);
        RegionDataset regionDataset=(RegionDataset)task.getParameter(Operation_mask.REGION_DATASET);
        String strandString=(String)task.getParameter(Operation_mask.STRAND);
        boolean useRelativeStrand=(strandString.equalsIgnoreCase("relative")||strandString.equalsIgnoreCase("sequence")||strandString.equalsIgnoreCase("gene"));
        boolean reverse =(useRelativeStrand && sourceSequence.getStrandOrientation()==Sequence.REVERSE);
        Integer maskTypeInt=(Integer)task.getParameter(Operation_mask.MASKTYPE);
        if (maskTypeInt==null) throw new ExecutionError("Missing mask type specification");
        else masktype=maskTypeInt.intValue();
        
        Character maskLetter=(Character)task.getParameter(Operation_mask.MASK_LETTER);
        if (maskLetter!=null) {
            letter=maskLetter.charValue();
            if (reverse) letter=MotifLabEngine.reverseBase(letter);
        }
        int order=(backgroundModel!=null)?backgroundModel.getOrder():0;           
        String seqname=sourceSequence.getName();
        RegionSequenceData regionSequence=(regionDataset!=null)?(RegionSequenceData)regionDataset.getSequenceByName(seqname):null;
        if (reverse && masktype==RANDOM_BASES) { // if a Markov-model is used on the reverse strand we must apply it in the opposite direction
              for (int i=sourceSequence.getRegionEnd();i>=sourceSequence.getRegionStart();i--) {
                if (positionSatisfiesCondition(seqname,i,task)) {
                      String prefix=getPrefixForPosition(i,order,(DNASequenceData)targetSequence, reverse); // the prefix must be taken from the target for continuity
                      char filler=backgroundModel.getNextBase(prefix);
                      ((DNASequenceData)targetSequence).setValueAtGenomicPosition(i,MotifLabEngine.reverseBase(filler));                                               
                } // satisfies 'where'-condition
            }             
        } else {
             for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                if (positionSatisfiesCondition(seqname,i,task)) {
                  if (masktype==LETTER) ((DNASequenceData)targetSequence).setValueAtGenomicPosition(i,letter);
                  else if (masktype==LOWERCASE) ((DNASequenceData)targetSequence).setValueAtGenomicPosition(i,Character.toLowerCase((Character)sourceSequence.getValueAtGenomicPosition(i)));
                  else if (masktype==UPPERCASE) ((DNASequenceData)targetSequence).setValueAtGenomicPosition(i,Character.toUpperCase((Character)sourceSequence.getValueAtGenomicPosition(i)));
                  else if (masktype==RANDOM_BASES) {
                      String prefix=getPrefixForPosition(i,order,(DNASequenceData)targetSequence, reverse); // the prefix must be taken from the target for continuity
                      char filler=backgroundModel.getNextBase(prefix);
                      ((DNASequenceData)targetSequence).setValueAtGenomicPosition(i,filler);                
                  } else if (masktype==REGION_SEQUENCE_PROPERTY) {
                      ArrayList<Region> overlapping=regionSequence.getRegionsOverlappingGenomicInterval(i,i);
                      if (overlapping!=null && !overlapping.isEmpty()) {
                          Region region=null;
                          for (Region r:overlapping) {
                              if (r.getSequence()!=null) {region=r;break;} // use first region that has sequence property
                          }
                          if (region!=null) {
                              char newbase='X';                              
                              String sequence=region.getSequence();
                              if (sequence.length()!=region.getLength()) continue; // sequence property not correct length
                              if (region.getOrientation()==Region.REVERSE) {
                                  int pos=i-region.getGenomicStart(); // relative position within the region
                                  pos=(sequence.length()-1)-pos; // count from the end
                                  newbase=sequence.charAt(pos);  
                                  newbase=MotifLabEngine.reverseBase(newbase);
                              } else {
                                  int pos=i-region.getGenomicStart(); // relative position within the region
                                  newbase=sequence.charAt(pos);
                              }
                              ((DNASequenceData)targetSequence).setValueAtGenomicPosition(i,newbase);  
                          }
                      }
                  }                 
                } // satisfies 'where'-condition
            }                
        } // end else 
    }
    
    private String getPrefixForPosition(int i, int order, DNASequenceData source, boolean reverse) {
        if (order==0) return "";
        if (reverse) { // reverse strand
            if (i==source.getRegionEnd()) return "";
            int start=i+1;
            int end=i+order;    
            char[] prefix=(char[])source.getValueInGenomicInterval(start, end);
            prefix=MotifLabEngine.reverseSequence(prefix);
            return new String(prefix);
        } else { // direct strand
            if (i==source.getRegionStart()) return "";        
            int start=i-order;
            int end=i-1;
            String prefix=new String((char[])source.getValueInGenomicInterval(start, end)); 
            return prefix;
        }
    }
}
