/*
 
 
 */

package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.*;
import motiflab.engine.data.FeatureSequenceData;

/**
 *
 * @author kjetikl
 */
public class Operation_extend extends FeatureTransformOperation {
    public static final String EXTEND_EXPRESSION="extendBothWays"; // points to A String containing a number or the name of a Data object or a Condition_position
    public static final String EXTEND_OPERATOR="extendBothWaysBy"; // by|while|until
    public static final String EXTEND_UPSTREAM_EXPRESSION="extendUpstream"; // points to A String containing a number or the name of a Data object or a Condition_position
    private static final String EXTEND_UPSTREAM_EXPRESSION_RESOLVED="extendUpstreamResolved"; 
    public static final String EXTEND_UPSTREAM_OPERATOR="extendUpstreamOperator";   // by|while|until
    public static final String EXTEND_DOWNSTREAM_EXPRESSION="extendDownstream"; // points to A String containing a number or the name of a Data object or a Condition_position
    private static final String EXTEND_DOWNSTREAM_EXPRESSION_RESOLVED="extendDownstreamResolved"; 
    public static final String EXTEND_DOWNSTREAM_OPERATOR="extendDownstreamOperator";  // by|while|until

    private static final String name="extend";
    private static final String description="Extends the span of regions upstream and/or downstream";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};

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
        String extendBothDirectionsOperator=(String)task.getParameter(EXTEND_OPERATOR);
        String extendUpstreamOperator=(String)task.getParameter(EXTEND_UPSTREAM_OPERATOR);
        String extendDownstreamOperator=(String)task.getParameter(EXTEND_DOWNSTREAM_OPERATOR);
        Object extendBothDirectionsExpression=task.getParameter(EXTEND_EXPRESSION);
        Object extendUpstreamExpression=task.getParameter(EXTEND_UPSTREAM_EXPRESSION);
        Object extendDownstreamExpression=task.getParameter(EXTEND_DOWNSTREAM_EXPRESSION);
        
        if (extendBothDirectionsOperator!=null) { // split bidirectional into upstream and downstream
            extendUpstreamOperator=extendBothDirectionsOperator;
            extendDownstreamOperator=extendBothDirectionsOperator;    
            extendUpstreamExpression=extendBothDirectionsExpression;
            if (extendBothDirectionsExpression instanceof Condition) extendDownstreamExpression=((Condition)extendBothDirectionsExpression).clone();
            else extendDownstreamExpression=extendBothDirectionsExpression;
        } 
        if (extendUpstreamExpression instanceof String) {
            Object data = engine.getDataItem((String)extendUpstreamExpression);
            if (data==null) {
                 try {
                  int value=Integer.parseInt((String)extendUpstreamExpression);
                  extendUpstreamExpression = new Integer(value);
                 } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+extendUpstreamExpression+"' neither data nor integer constant",task.getLineNumber());}                        
            } else {
                if (!(data instanceof NumericVariable || data instanceof NumericMap)) throw new ExecutionError("'"+extendUpstreamExpression+"' is not a valid Numeric Variable in this context",task.getLineNumber());  
                extendUpstreamExpression=data;
            }
        } else if (extendUpstreamExpression instanceof Condition_position) {
            Condition_position condition=(Condition_position)extendUpstreamExpression;
            if (extendUpstreamOperator.equals("until")) { // negate condition and treat as while
                if (condition.negateAll()==Boolean.TRUE) condition.setNegateAll(Boolean.FALSE);
                else condition.setNegateAll(Boolean.TRUE);
            }
            condition.resolve(engine, task);
        }

        if (extendDownstreamExpression instanceof String) {
            Object data = engine.getDataItem((String)extendDownstreamExpression);
            if (data==null) {
                 try {
                  int value=Integer.parseInt((String)extendDownstreamExpression);
                  extendDownstreamExpression = new Integer(value);
                 } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+extendDownstreamExpression+"' neither data nor integer constant",task.getLineNumber());}                        
            } else {
                if (!(data instanceof NumericVariable || data instanceof NumericMap)) throw new ExecutionError("'"+extendDownstreamExpression+"' is not a valid Numeric Variable in this context",task.getLineNumber());  
                extendDownstreamExpression=data;
            }
        } else if (extendDownstreamExpression instanceof Condition_position) {
            Condition_position condition=(Condition_position)extendDownstreamExpression;
            if (extendDownstreamOperator.equals("until")) { // negate condition and treat as while
                if (condition.negateAll()==Boolean.TRUE) condition.setNegateAll(Boolean.FALSE);
                else condition.setNegateAll(Boolean.TRUE);
            }
            condition.resolve(engine, task);
        }

        // these two parameters are now the only ones we need for further execution of the transform
        task.setParameter(EXTEND_UPSTREAM_EXPRESSION_RESOLVED,extendUpstreamExpression);
        task.setParameter(EXTEND_DOWNSTREAM_EXPRESSION_RESOLVED,extendDownstreamExpression);        
    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        ArrayList<Region> list = ((RegionSequenceData)targetSequence).getAllRegions();
        String seqName=targetSequence.getName();
        int orientation=targetSequence.getStrandOrientation();
        int seqLength=targetSequence.getSize();
        // Extend_Expression is either an Integer,a NumericVariable, a SequenceNumericMap or a Condition_position
        Object extendUpstreamExpression=task.getParameter(EXTEND_UPSTREAM_EXPRESSION_RESOLVED);
        Object extendDownstreamExpression=task.getParameter(EXTEND_DOWNSTREAM_EXPRESSION_RESOLVED);
        if (extendUpstreamExpression instanceof SequenceNumericMap) extendUpstreamExpression=new Integer(((SequenceNumericMap)extendUpstreamExpression).getValue(seqName).intValue()); 
        else if (extendUpstreamExpression instanceof NumericVariable) extendUpstreamExpression=new Integer(((NumericVariable)extendUpstreamExpression).getValue().intValue()); 
        if (extendDownstreamExpression instanceof SequenceNumericMap) extendDownstreamExpression=new Integer(((SequenceNumericMap)extendDownstreamExpression).getValue(seqName).intValue()); 
        else if (extendDownstreamExpression instanceof NumericVariable) extendDownstreamExpression=new Integer(((NumericVariable)extendDownstreamExpression).getValue().intValue()); 
        for (Region region:list) {
            if (regionSatisfiesCondition(seqName, region, task)) {
                extend(region, (RegionSequenceData)targetSequence, seqLength, orientation, extendUpstreamExpression, extendDownstreamExpression, task);
            }            
        } // end: for each region
        ((RegionSequenceData)targetSequence).updateRegionSortOrder(); // updates the sorting of regions so that they are listed in correct order
    }
    

    /**
     * Called by the execute() this method does the real work
     * @param region
     * @param seqLenth - the length of the sequence wherein the region resides
     * @param orientation - orientation of the sequence (not the region)
     * @param targetSequence
     * @param upstream
     * @param downstream
     */
    private void extend(Region region, RegionSequenceData targetSequence, int seqLength, int orientation, Object upstreamLength, Object downstreamLength, OperationTask task) throws Exception {
           String sequenceName=targetSequence.getName();
           int oldStart=region.getRelativeStart();
           int oldEnd=region.getRelativeEnd();
           int newStart=oldStart;
           int newEnd=oldEnd;
           int upstream=0; // the number of bases to extend upstream
           int downstream=0; // the number of bases to extend downstream
           int upstreamEnd=(orientation==FeatureSequenceData.DIRECT)?oldStart:oldEnd;
           int downstreamEnd=(orientation==FeatureSequenceData.DIRECT)?oldEnd:oldStart;
           int upstreamDirection=(orientation==FeatureSequenceData.DIRECT)?-1:1;
           int downstreamDirection=(orientation==FeatureSequenceData.DIRECT)?1:-1;
           if (upstreamLength instanceof Integer) upstream=((Integer)upstreamLength).intValue(); // Note: SequenceNumericMaps have already been resolved to Integers
           else if (upstreamLength instanceof NumericMap) { // Not SequenceNumericMaps but all other types (MotifNumericMap and ModuleNumericMap)
               String type=region.getType();
               if (type!=null) upstream=((NumericMap)upstreamLength).getValue(type).intValue();
               else upstream=((NumericMap)upstreamLength).getValue().intValue();
           }
           else if (upstreamLength instanceof Condition_position) upstream=findExtendLength((Condition_position)upstreamLength,upstreamEnd,targetSequence,sequenceName,seqLength,upstreamDirection,task);
           if (downstreamLength instanceof Integer) downstream=((Integer)downstreamLength).intValue();
           else if (downstreamLength instanceof NumericMap) { // Not SequenceNumericMaps but all other types (MotifNumericMap and ModuleNumericMap)
               String type=region.getType();
               if (type!=null) downstream=((NumericMap)downstreamLength).getValue(type).intValue();
               else downstream=((NumericMap)downstreamLength).getValue().intValue();
           }           
           else if (downstreamLength instanceof Condition_position) downstream=findExtendLength((Condition_position)downstreamLength,downstreamEnd,targetSequence,sequenceName,seqLength,downstreamDirection,task);                          
           if (orientation==FeatureSequenceData.DIRECT) {
                 newStart-=upstream;
                 if (newStart<0) newStart=0;
                 newEnd+=downstream;
                 if (newEnd>=seqLength) newEnd=seqLength-1;
           } else { // reverse strand
                 newStart-=downstream;
                 if (newStart<0) newStart=0;
                 newEnd+=upstream;
                 if (newEnd>=seqLength) newEnd=seqLength-1;               
           }
           region.setRelativeStart(newStart);
           region.setRelativeEnd(newEnd);
           if (newEnd<newStart) targetSequence.removeRegion(region); // delete if region has been shrunk into nothing
    }      
    
    
    /**
     * Searches along the sequence from a given start position in a given direction and returns the size of the region that 
     * satisfies the "extend while expression" condition
     */
    private int findExtendLength(Condition_position condition, int startPosition, RegionSequenceData targetSequence, String sequenceName, int seqLength, int direction, OperationTask task) throws Exception {
        int extendLength=0;  
        int position=startPosition+direction;
        //condition.debug();
        int genomicPosition=targetSequence.getGenomicPositionFromRelative(position);
        while(position>=0 && position<seqLength && condition.isConditionSatisfied(sequenceName, genomicPosition, task)) {
            extendLength++;
            position+=direction;
            genomicPosition+=direction;
        }
        return extendLength;
    }
    
       
    
}
