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
import org.motiflab.engine.data.SequenceNumericMap;

/**
 *
 * @author kjetikl
 */
public class Operation_interpolate extends FeatureTransformOperation {
    public static final String METHOD="method"; //
    public static final String PERIOD="period"; // 
    private static final String PERIOD_DATA="periodData"; // 
    public static final String MAX_DISTANCE="maxDistance"; // 
    private static final String MAX_DISTANCE_DATA="maxDistanceData"; //     
    public static final String ZERO_ORDER_HOLD="zero order hold"; //
    public static final String LINEAR="linear interpolation"; // 
    private static final String name="interpolate";
    private static final String description="Fills in missing values between discrete non-zero points in numeric datasets";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class};

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
        String periodString=(String)task.getParameter(PERIOD);
        if (periodString!=null) {
            Data periodData=null;
            periodData=engine.getDataItem(periodString);
            if (periodData==null) {
                try {
                  double value=Double.parseDouble(periodString);
                  periodData=new NumericVariable(periodString, (double)value);
               } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+periodString+"' neither data nor numeric constant",task.getLineNumber());}         
            }
            if (!(periodData instanceof NumericConstant || periodData instanceof NumericVariable || periodData instanceof SequenceNumericMap)) throw new ExecutionError("'"+periodString+"' is not a valid numeric value",task.getLineNumber());          
            task.setParameter(PERIOD_DATA, periodData);
        }
        
        String maxString=(String)task.getParameter(MAX_DISTANCE);
        if (maxString!=null) {
            Data maxData=null;
            maxData=engine.getDataItem(maxString);
            if (maxData==null) {
                try {
                  double value=Double.parseDouble(maxString);
                  maxData=new NumericVariable(maxString, (double)value);
               } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+maxString+"' neither data nor numeric constant",task.getLineNumber());}         
            }
            if (!(maxData instanceof NumericConstant || maxData instanceof NumericVariable || maxData instanceof SequenceNumericMap)) throw new ExecutionError("'"+maxString+"' is not a valid numeric value",task.getLineNumber());          
            task.setParameter(MAX_DISTANCE_DATA, maxData);
        }      
    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String method=(String)task.getParameter(METHOD);
        Data periodData=(Data)task.getParameter(PERIOD_DATA);
        Data maxDistanceData=(Data)task.getParameter(MAX_DISTANCE_DATA);
        String seqname=sourceSequence.getName();
        double perioddouble=0;
        if (periodData instanceof NumericConstant) {
              perioddouble=((NumericConstant)periodData).getValue();
        } else if (periodData instanceof SequenceNumericMap) {
              perioddouble=((SequenceNumericMap)periodData).getValue(seqname);
        } else if (periodData instanceof NumericVariable) {
              perioddouble=((NumericVariable)periodData).getValue();
        }
        int period=(int)perioddouble;
             
        double maxdistancedouble=0;
        if (maxDistanceData instanceof NumericConstant) {
              maxdistancedouble=((NumericConstant)maxDistanceData).getValue();
        } else if (maxDistanceData instanceof SequenceNumericMap) {
              maxdistancedouble=((SequenceNumericMap)maxDistanceData).getValue(seqname);
        } else if (maxDistanceData instanceof NumericVariable) {
              maxdistancedouble=((NumericVariable)maxDistanceData).getValue();
        }
        int maxdistance=(int)maxdistancedouble;        
        if (maxdistance<0) maxdistance=0;
        int first=-1;
        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
            double value=((NumericSequenceData)sourceSequence).getValueAtGenomicPosition(i);
            if (value!=0) {first=i;break;}
        }
        if (first<0) {} //task.getEngine().logMessage("Only zeros in "+seqname);
        else {
           if (period==0) transformWithoutPeriod((NumericSequenceData)sourceSequence,(NumericSequenceData)targetSequence,task,method, first, maxdistance);
           else transformWithPeriod((NumericSequenceData)sourceSequence,(NumericSequenceData)targetSequence,task,method,period,first);
        }
    }
    
    /** Interpolates between non-zero valued points in the sequence */
    private void transformWithoutPeriod(NumericSequenceData sourceSequence, NumericSequenceData targetSequence, OperationTask task, String method, int first, int maxdistance) throws Exception {
        int lastpos=first;
        double lastvalue=((NumericSequenceData)sourceSequence).getValueAtGenomicPosition(lastpos);
        int i=lastpos;
        int end=sourceSequence.getRegionEnd();
        while (i<=end) {
            double value=((NumericSequenceData)sourceSequence).getValueAtGenomicPosition(i);
            if (value!=0) {
                double endvalue=(method.equals(ZERO_ORDER_HOLD))?lastvalue:value;
                if (maxdistance<=0 || ((i-1)-lastpos<maxdistance)) fillIn(targetSequence,lastpos,lastvalue,i-1,endvalue,task);
                lastpos=i;
                lastvalue=value;
            }
            i++;
        }
        if (lastpos!=end) {
            if (maxdistance<=0 || (end-lastpos<maxdistance)) fillIn(targetSequence,lastpos,lastvalue,end,lastvalue,task);
        }
    }    
    
    /** Fills in values between two selected points (start/end) in a sequence. The values to use for these endpoints must also 
     *  be provided and the values in between are based on linear interpolation between the two end values. 
     * (thus, zero-order-hold is imposed by using the same value for both end points). 
     *  
     */
    private void fillIn(NumericSequenceData targetSequence, int start, double startvalue, int end, double endvalue, OperationTask task) throws Exception{
        String seqname=targetSequence.getName();
        double increment=(endvalue-startvalue)/(double)(end-start+1);
        double value=startvalue;
        for (int i=start;i<=end;i++) {
            if (positionSatisfiesCondition(seqname,i,task)) {
               targetSequence.setValueAtGenomicPosition(i, value);
            }
            value+=increment;
        }
    }
    
    /** Interpolates between points with a fixed distance */
    private void transformWithPeriod(NumericSequenceData sourceSequence, NumericSequenceData targetSequence, OperationTask task, String method, int period, int first) throws Exception {
        String seqname=sourceSequence.getName();
        int i=first;
        int end=sourceSequence.getRegionEnd();
        while (i<=end) {
            double value=sourceSequence.getValueAtGenomicPosition(i);
            int nextpos=i+period;
            double nextvalue=0;
            if (nextpos>=end) {
                nextpos=end+1;
                nextvalue=sourceSequence.getValueAtGenomicPosition(end);
            } else nextvalue=sourceSequence.getValueAtGenomicPosition(nextpos);
            fillIn(targetSequence,i,value,nextpos-1,(method.equals(ZERO_ORDER_HOLD))?value:nextvalue,task);
            i=nextpos;
            
        }
    }
    
    
    public static String[] getInterpolationMethods() {
        return new String[]{ZERO_ORDER_HOLD,LINEAR};
    }
    
}
