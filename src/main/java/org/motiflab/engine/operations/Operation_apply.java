/*
 
 
 */

package org.motiflab.engine.operations;

import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericConstant;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceNumericMap;

/**
 *
 * @author kjetikl
 */
public class Operation_apply extends FeatureTransformOperation {
    public static final String WINDOW_SIZE="windowSize"; 
    private static final String WINDOW_SIZE_VALUE="windowSizeValue"; 
    public static final String WINDOW_TYPE="windowType"; //
    public static final String ANCHOR="anchor"; //
    public static final String UPSTREAM="start"; //
    public static final String DOWNSTREAM="end"; //
    public static final String CENTER="center"; //
    private static final String name="apply";
    private static final String description="Applies sliding window functions to Numeric Datasets";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class};
   
    public static String[] getWindowTypes() {return new String[]{"Uniform","Bartlett","Gaussian","Sum","Minimum","Maximum","Shift","Edge","Peak","Valley","Valley2"};}
    
    public static String[] getWindowAnchors() {return new String[]{CENTER,UPSTREAM,DOWNSTREAM};}
    
    public static boolean isRecognizedWindowType(String type) {
        for (String s:getWindowTypes()) {
            if (s.equals(type)) return true;
        }
        return false;
    }    
    public static boolean isRecognizedAnchor(String anchor) {
        for (String s:getWindowAnchors()) {
            if (s.equals(anchor)) return true;
        }
        return false;
    }
    
    
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
    public void resolveParameters(OperationTask task) throws Exception {
        String windowSizeString=(String)task.getParameter(WINDOW_SIZE);
        Data windowSizeData=null;
        windowSizeData=engine.getDataItem(windowSizeString);
        if (windowSizeData==null) {
            try {
              double value=Double.parseDouble(windowSizeString);
              windowSizeData=new NumericConstant(windowSizeString, (double)value);
           } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+windowSizeString+"' neither data nor numeric constant",task.getLineNumber());}         
        }
        if (!(windowSizeData instanceof NumericConstant || windowSizeData instanceof NumericVariable || windowSizeData instanceof SequenceNumericMap)) throw new ExecutionError("'"+windowSizeString+"' is not a valid numeric value",task.getLineNumber());                  
        task.setParameter(WINDOW_SIZE_VALUE, windowSizeData);
    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String windowType=(String)task.getParameter(WINDOW_TYPE);
        Data windowSizeData=(Data)task.getParameter(WINDOW_SIZE_VALUE);
        String anchor=(String)task.getParameter(ANCHOR);
        int windowsize=5;   
        String seqname=sourceSequence.getName();
        if (windowSizeData instanceof SequenceNumericMap) windowsize=(int)((SequenceNumericMap)windowSizeData).getValue(seqname).doubleValue();
        else if (windowSizeData instanceof NumericVariable) windowsize=(int)((NumericVariable)windowSizeData).getValue().doubleValue();
        else if (windowSizeData instanceof NumericConstant) windowsize=(int)((NumericConstant)windowSizeData).getValue().doubleValue();
        double[] window=null;
        if (windowsize==0) throw new ExecutionError("Window size can not be zero");
        // set up coefficients for a window function if needed (needed for Gaussian and Bartlett)
        if (windowType.equalsIgnoreCase("gaussian")) {
            // the gaussian window function is based on code from Matlab 7 which is further based on the paper:  fredric j. harris "On the Use of Windows for Harmonic Analysis with the Discrete Fourier Transform" Proceedings of the IEEE, Vol. 66, No. 1, January 1978
            window=new double[windowsize];
            double alpha=2.5f; //
            double total=0;
            for (int i=0;i<windowsize;i++) {
                double k=(-((double)windowsize-1f)/2f)+i; 
                double halfwindow=((double)windowsize)/2f;                
                double powarg=alpha*k/halfwindow;
                double pow=(double)(-0.5*Math.pow(powarg,2));
                double value=(double)Math.exp(pow); 
                //System.err.println("k=>"+k+"  powarg="+powarg+"  pow="+pow+"  value="+value);
                window[i]=value;
                total+=window[i];
            }    
            for (int i=0;i<windowsize;i++) { // normalize weights so they sum to 1
                window[i]=window[i]/total;
            }            
        } else if (windowType.equalsIgnoreCase("bartlett")) {
            window=new double[windowsize];
            double half=((int)(windowsize/2))+1;
            for (int i=0;i<half;i++) {
                window[i]=(1f/half)*(i+1);
                window[windowsize-(i+1)]=(1f/half)*(i+1);
            }
            double total=0;
            for (int i=0;i<windowsize;i++) {
                total+=window[i];
            }
            for (int i=0;i<windowsize;i++) { // normalize weights so they sum to 1
                window[i]=window[i]/total;
            }
        } else if (windowType.equalsIgnoreCase("edge")) { // place -1 on the left side and +1 on the right side
            window=new double[windowsize];
            double half=((int)(windowsize/2));
            for (int i=0;i<half;i++) {
                window[i]=-1;
                window[windowsize-(i+1)]=+1;
            }
        }
            
        int orientation=sourceSequence.getStrandOrientation();
        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) { // for each base
            int start=0, end=0;
            if (anchor.equals(UPSTREAM)) {
                start=(orientation==Sequence.DIRECT)?i:i-windowsize+1;
                end=(orientation==Sequence.DIRECT)?i+windowsize-1:i;
            } else if (anchor.equals(DOWNSTREAM)) {
                start=(orientation==Sequence.DIRECT)?i-windowsize+1:i;                
                end=(orientation==Sequence.DIRECT)?i:i+windowsize-1;
            } else { // center anchor
               if (windowsize%2==0) { // window has even number of bases - anchor left of center
                   int flanksize=(int)(windowsize/2);
                   start=(orientation==Sequence.DIRECT)?i-flanksize+1:i-flanksize;
                   end=(orientation==Sequence.DIRECT)?i+flanksize:i+flanksize-1;                   
               } else { // window has odd number of bases
                   int flanksize=(int)(windowsize/2);
                   start=i-flanksize;
                   end=i+flanksize;
               }
            }
            if (windowType.equalsIgnoreCase("shift")) {
                if (anchor.equals(DOWNSTREAM)) {
                    if (orientation==Sequence.REVERSE) start=end;
                } else {
                    if (orientation==Sequence.DIRECT) start=end;
                }
            }
            if (positionSatisfiesCondition(seqname,i,task)) {
              double newvalue=getNewValue((NumericSequenceData)sourceSequence,sourceSequence.getRelativePositionFromGenomic(start),sourceSequence.getRelativePositionFromGenomic(end),windowType,window);
              ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i, newvalue);
           } // satisfies 'where'-condition
        }
    }
    
    private double getNewValue(NumericSequenceData sourceSequence, int start, int end, String windowType, double[] window) throws ExecutionError {
        if (windowType==null) windowType="uniform";
        if (windowType.equalsIgnoreCase("uniform")) {
            double result=0;
            double counted=0;
            for (int j=start;j<=end;j++) {
                if (j<0 || j>=sourceSequence.getSize()) continue; // outside sequence. just skip
                counted++; // How many bases are included
                result+=sourceSequence.getValueAtRelativePosition(j);
            }
            if (counted>0) result=result/counted;
            return result;            
        } else if (windowType.equalsIgnoreCase("shift")) {
            if (start<0 || start>=sourceSequence.getSize()) return 0; // outside sequence. just skip
            else return sourceSequence.getValueAtRelativePosition(start);
                      
        } else if (windowType.equalsIgnoreCase("sum")) {
            double result=0;
            for (int j=start;j<=end;j++) {
                if (j<0 || j>=sourceSequence.getSize()) continue; // outside sequence. just skip
                result+=sourceSequence.getValueAtRelativePosition(j);
            }
            return result;            
        } else if (windowType.equalsIgnoreCase("minimum")) {
            double minimum=Double.MAX_VALUE;
            for (int j=start;j<=end;j++) {
                if (j<0 || j>=sourceSequence.getSize()) continue; // outside sequence. just skip
                double value=sourceSequence.getValueAtRelativePosition(j);
                if (value<minimum) minimum=value;
            }
            return minimum;             
        } else if (windowType.equalsIgnoreCase("maximum")) {
            double maximum=-Double.MAX_VALUE;
            for (int j=start;j<=end;j++) {
                if (j<0 || j>=sourceSequence.getSize()) continue; // outside sequence. just skip
                double value=sourceSequence.getValueAtRelativePosition(j);
                if (value>maximum) maximum=value;
            }
            return maximum;
        } else if (windowType.equalsIgnoreCase("valley") || windowType.equalsIgnoreCase("valley2")) {
               // The valley-score is based on the definition used in the paper:
               // Ramsey & Shmulevich et al. (2010) "Genome-wide histone acetylation data improve prediction of mammalian transcription factor binding sites", Bioinformatics, 26(17) : 2071-2075
               // The sliding window is divided into a left (40%), central (20%) and right (40%) part,
               // and the highest values within both the left and right flanks are determined. 
               // The smallest of these two maximum scores from the left and right flanks
               // is here called "minofmax". If the score in the center point is less than 90% 
               // of minofmax then the center point is considered to be a "valley-point" 
               // and is assigned a value>0. If, however, the score in the center is within 10% 
               // of the minofmax-value it is not a valley-point and is assigned a value of 0.
               boolean alternativeScore=windowType.equalsIgnoreCase("valley2");
               int windowsize=end-start+1;
               int center=start+(int)(windowsize/2);
               int sequenceEnd=sourceSequence.getSize()-1;
               if (center<0) center=0;
               else if (center>sequenceEnd) center=sequenceEnd;
               double currentValue=sourceSequence.getValueAtRelativePosition(center);
               double leftmax=-Double.MAX_VALUE;
               double rightmax=-Double.MAX_VALUE;
               int leftWindowStart=start;
               int leftWindowEnd=start+(int)(windowsize*0.4); // leftWindow is first 40% of the window
               int rightWindowStart=start+(int)(windowsize*0.6); // leftWindow is first last% of the window
               int rightWindowEnd=end;
               if (leftWindowStart<0) leftWindowStart=0;
               if (leftWindowEnd<0) leftWindowEnd=0;
               if (rightWindowStart<0) rightWindowStart=0;
               if (rightWindowEnd<0) rightWindowEnd=0;
               if (rightWindowStart>sequenceEnd) rightWindowStart=sequenceEnd;
               if (rightWindowEnd>sequenceEnd) rightWindowEnd=sequenceEnd;
               if (leftWindowStart>sequenceEnd) leftWindowStart=sequenceEnd;
               if (leftWindowEnd>sequenceEnd) leftWindowEnd=sequenceEnd;
               for (int j=leftWindowStart;j<=leftWindowEnd;j++) {
                   double windowvalue=sourceSequence.getValueAtRelativePosition(j);
                   if (windowvalue>leftmax) leftmax=windowvalue;
               }
               for (int j=rightWindowStart;j<=rightWindowEnd;j++) {
                   double windowvalue=sourceSequence.getValueAtRelativePosition(j);
                   if (windowvalue>rightmax) rightmax=windowvalue;
               }
               double minofmax=(leftmax<rightmax)?leftmax:rightmax;
               if (currentValue<0.9*minofmax) return (alternativeScore)?minofmax-currentValue:minofmax; // this definition of valley-score used in the paper was just "minofmax" which will give high scores to small dumps between high peaks but my alternative score is to subtract the current value (which will give higher scores the deeper the valley is)
               else return 0; // not a valley point
        } else if (windowType.equalsIgnoreCase("peak")) {
               // The peak detection algorithm is an "inverse" of the valley detection above 
               int windowsize=end-start+1;
               int center=start+(int)(windowsize/2);
               int sequenceEnd=sourceSequence.getSize()-1;
               if (center<0) center=0;
               else if (center>sequenceEnd) center=sequenceEnd;
               double currentValue=sourceSequence.getValueAtRelativePosition(center);
               double leftmin=Double.MAX_VALUE;
               double rightmin=Double.MAX_VALUE;
               int leftWindowStart=start;
               int leftWindowEnd=start+(int)(windowsize*0.4); // leftWindow is first 40% of the window
               int rightWindowStart=start+(int)(windowsize*0.6); // leftWindow is first last% of the window
               int rightWindowEnd=end;
               if (leftWindowStart<0) leftWindowStart=0;
               if (leftWindowEnd<0) leftWindowEnd=0;
               if (rightWindowStart<0) rightWindowStart=0;
               if (rightWindowEnd<0) rightWindowEnd=0;
               if (rightWindowStart>sequenceEnd) rightWindowStart=sequenceEnd;
               if (rightWindowEnd>sequenceEnd) rightWindowEnd=sequenceEnd;
               if (leftWindowStart>sequenceEnd) leftWindowStart=sequenceEnd;
               if (leftWindowEnd>sequenceEnd) leftWindowEnd=sequenceEnd;
               for (int j=leftWindowStart;j<=leftWindowEnd;j++) {
                   double windowvalue=sourceSequence.getValueAtRelativePosition(j);
                   if (windowvalue<leftmin) leftmin=windowvalue;
               }
               for (int j=rightWindowStart;j<=rightWindowEnd;j++) {
                   double windowvalue=sourceSequence.getValueAtRelativePosition(j);
                   if (windowvalue<rightmin) rightmin=windowvalue;
               }
               double maxofmin=(leftmin>rightmin)?leftmin:rightmin;
               if (maxofmin<0.1*currentValue) return currentValue; // surrounding values should be less than 10% of middle
               else return 0; // not a peak point
        } else if (windowType.equalsIgnoreCase("gaussian") || windowType.equalsIgnoreCase("bartlett") || windowType.equalsIgnoreCase("edge") ) {
            // apply windowing function
            double result=0;
            int index=0;
            for (int j=start;j<=end;j++) { // if the window lies partially outside the sequence the sequence value at the edge (first or final in sequence) will be used to 'pad' the sequence
                double value=0;
                if (j>=0 && j<sourceSequence.getSize()) value=sourceSequence.getValueAtRelativePosition(j); // outside sequence set to 0
                result+=value*window[index];
                index++;
            }
            return result;
        } else throw new ExecutionError("Unknown window function: "+windowType);

    }
    
    
}
