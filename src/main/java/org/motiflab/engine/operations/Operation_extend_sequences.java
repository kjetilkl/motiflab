/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.task.ExecutableTask;

/**
 *
 * @author kjetikl
 */
public class Operation_extend_sequences extends Operation {
    public static final String EXTEND_EXPRESSION="extendBothWays"; // points to A String containing a number or the name of a Data object
    public static final String EXTEND_UPSTREAM_EXPRESSION="extendUpstream"; // points to A String containing a number or the name of a Data object
    public static final String EXTEND_DOWNSTREAM_EXPRESSION="extendDownstream"; // points to A String containing a number or the name of a Data object
    public static final String USE_RELATIVE_ORIENTATION="useRelativeOrientation";
    
    private static final String EXTEND_UPSTREAM_EXPRESSION_RESOLVED="extendUpstreamResolved";     
    private static final String EXTEND_DOWNSTREAM_EXPRESSION_RESOLVED="extendDownstreamResolved"; 
   
    private static final String name="extend_sequences";
    private static final String description="Extends sequences upstream and/or downstream";
    private Class[] datasourcePreferences=new Class[]{SequenceCollection.class};

    @Override
    public String getOperationGroup() {
        return "Sequence"; 
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
    public boolean isSubrangeApplicable() {return false;}
    
    
    private void resolveParameters(OperationTask task) throws Exception {
        String extendBothDirectionsExpression=(String)task.getParameter(EXTEND_EXPRESSION);
        String extendUpstreamExpression=(String)task.getParameter(EXTEND_UPSTREAM_EXPRESSION);
        String extendDownstreamExpression=(String)task.getParameter(EXTEND_DOWNSTREAM_EXPRESSION);
        
        Object resolvedUpstreamExpression=null;
        Object resolvedDownstreamExpression=null;
        
        if (extendBothDirectionsExpression!=null) { // split bidirectional into upstream and downstream   
            extendUpstreamExpression=extendBothDirectionsExpression;
            extendDownstreamExpression=extendBothDirectionsExpression;
        } 
        if (extendUpstreamExpression!=null) {
            Object dataUP = engine.getDataItem(extendUpstreamExpression);
            if (dataUP==null) {
                 try {
                  int value=Integer.parseInt(extendUpstreamExpression);
                  resolvedUpstreamExpression = new Integer(value);
                 } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+extendUpstreamExpression+"' neither data nor integer constant",task.getLineNumber());}                        
            } else {
                if (!(dataUP instanceof NumericVariable || dataUP instanceof NumericMap)) throw new ExecutionError("'"+extendUpstreamExpression+"' is not a valid Numeric Variable in this context",task.getLineNumber());  
                resolvedUpstreamExpression=dataUP;
            }
        }

        if (extendDownstreamExpression!=null) {
            Object dataDOWN = engine.getDataItem(extendDownstreamExpression);
            if (dataDOWN==null) {
                 try {
                  int value=Integer.parseInt(extendDownstreamExpression);
                  resolvedDownstreamExpression = new Integer(value);
                 } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+extendDownstreamExpression+"' neither data nor integer constant",task.getLineNumber());}                        
            } else {
                if (!(dataDOWN instanceof NumericVariable || dataDOWN instanceof NumericMap)) throw new ExecutionError("'"+extendDownstreamExpression+"' is not a valid Numeric Variable in this context",task.getLineNumber());  
                resolvedDownstreamExpression=dataDOWN;
            }
        }

        // these two parameters are now the only ones we need for further execution of the transform
        task.setParameter(EXTEND_UPSTREAM_EXPRESSION_RESOLVED,resolvedUpstreamExpression);
        task.setParameter(EXTEND_DOWNSTREAM_EXPRESSION_RESOLVED,resolvedDownstreamExpression);
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);        
    }
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        if (engine.hasDataItemsOfType(FeatureDataset.class)) throw new ExecutionError("Sequences can not be extended after feature data has been loaded");
                      
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
        resolveParameters(task);
        Object extendUpstreamObject=task.getParameter(EXTEND_UPSTREAM_EXPRESSION_RESOLVED);
        Object extendDownstreamObject=task.getParameter(EXTEND_DOWNSTREAM_EXPRESSION_RESOLVED);
        Object orientation=task.getParameter(USE_RELATIVE_ORIENTATION);
        boolean useRelativeOrientation=(orientation instanceof Boolean)?((Boolean)orientation):false;
        
        try {           
            ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
            ArrayList<Sequence> clonedsequences=new ArrayList<Sequence>(sequences.size());
            for (int i=0;i<sequences.size();i++) {
               clonedsequences.add((Sequence)sequences.get(i).clone()); 
            }
            int extended=0;
            int i=0;
            for (Data data:clonedsequences) {  
                task.setProgress(i,sequences.size()*2); // *2 is so that this part will end at 50%
                i++;
                Sequence sequence=(Sequence)data;
                int[] newGenomicRange=getNewGenomicCoordinates(sequence, extendUpstreamObject, extendDownstreamObject, useRelativeOrientation);
                if (newGenomicRange==null) {
                    engine.logMessage("Unable to extend "+sequence.getName());                        
                    continue;
                }
                if (newGenomicRange[0]==sequence.getRegionStart() && newGenomicRange[1]==sequence.getRegionEnd()) continue; // same location as before. Do nothing
                sequence.setRegionStart(newGenomicRange[0]);
                sequence.setRegionEnd(newGenomicRange[1]);
                extended++;                                                 
            }    
            int totaldata=sequences.size();
            int counter=0;
            if (extended>0) {
                for (i=0;i<clonedsequences.size();i++) {
                    if (i%100==0) {
                        task.checkExecutionLock(); // checks to see if this task should suspend execution
                        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                        Thread.yield();
                        task.setProgress(50+(int)((double)counter/(double)totaldata*50.0));
                    }
                    engine.updateDataItem(clonedsequences.get(i));
                    counter++;
                }   
            } // end: if (extended>0)
        }  catch (ClassCastException c) {
            throw new ExecutionError(c.getMessage());
        } catch (ExecutionError e) {
            throw e;
        }
        return true;
    }  

    private int[] getNewGenomicCoordinates(Sequence sequence, Object extendStartObject, Object extendEndObject, boolean useRelativeOrientation) {
        int[] result=new int[]{sequence.getRegionStart(),sequence.getRegionEnd()};
            int extendStart=0;
            int extendEnd=0;
            if (extendStartObject instanceof SequenceNumericMap) extendStart=((SequenceNumericMap)extendStartObject).getValue(sequence.getSequenceName()).intValue();
            else if (extendStartObject instanceof NumericVariable) extendStart=((NumericVariable)extendStartObject).getValue().intValue();
            else if (extendStartObject instanceof Number) extendStart=((Number)extendStartObject).intValue();
            if (extendEndObject instanceof SequenceNumericMap) extendEnd=((SequenceNumericMap)extendEndObject).getValue(sequence.getSequenceName()).intValue();
            else if (extendEndObject instanceof NumericVariable) extendEnd=((NumericVariable)extendEndObject).getValue().intValue();
            else if (extendEndObject instanceof Number) extendEnd=((Number)extendEndObject).intValue();
            
            if (extendStart<0) extendStart=0;
            if (extendEnd<0) extendEnd=0;
            if (useRelativeOrientation && sequence.getStrandOrientation()==Sequence.REVERSE) {
               result[0]=result[0]-extendEnd;
               result[1]=result[1]+extendStart;                
            } else { // Direct
               result[0]=result[0]-extendStart;
               result[1]=result[1]+extendEnd;
            }
            return result;        
    }      
    
}
