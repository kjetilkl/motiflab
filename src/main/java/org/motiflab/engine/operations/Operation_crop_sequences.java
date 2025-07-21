/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.task.ExecutableTask;

/**
 *
 * @author kjetikl
 */
public class Operation_crop_sequences extends Operation {
    public static final String CROP_TO_REGIONS="cropToRegions"; // the name of a RegionDataset (or null)
    public static final String CROP_EXPRESSION="cropBothWays"; // points to a String containing a number or the name of a Data object
    public static final String CROP_UPSTREAM_EXPRESSION="cropUpstream"; // points to a String containing a number or the name of a Data object
    public static final String CROP_DOWNSTREAM_EXPRESSION="cropDownstream"; // points to a String containing a number or the name of a Data object
    public static final String USE_RELATIVE_ORIENTATION="useRelativeOrientation";
    
    private static final String CROP_UPSTREAM_EXPRESSION_RESOLVED="cropUpstreamResolved";     
    private static final String CROP_DOWNSTREAM_EXPRESSION_RESOLVED="cropDownstreamResolved"; 
    private static final String CROP_TO_REGIONS_RESOLVED="cropToRegionsResolved"; // points to a RegionDataset (or null) 
   
    private static final String name="crop_sequences";
    private static final String description="Crops bases off the ends of sequences";
    private Class[] datasourcePreferences=new Class[]{SequenceCollection.class};

    @Override
    public String getOperationGroup() {
        return "Sequence"; //
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
        String cropRegionsExpression=(String)task.getParameter(CROP_TO_REGIONS);
        String cropBothDirectionsExpression=(String)task.getParameter(CROP_EXPRESSION);
        String cropUpstreamExpression=(String)task.getParameter(CROP_UPSTREAM_EXPRESSION);
        String cropDownstreamExpression=(String)task.getParameter(CROP_DOWNSTREAM_EXPRESSION);
        
        Object resolvedUpstreamExpression=null;
        Object resolvedDownstreamExpression=null;
        Object resolvedRegionsExpression=null;
        
        if (cropRegionsExpression!=null) {
                Object regiondataset = engine.getDataItem(cropRegionsExpression);
                if (regiondataset!=null) {
                    if (!(regiondataset instanceof RegionDataset)) throw new ExecutionError("'"+cropRegionsExpression+"' is not a Region Dataset",task.getLineNumber());  
                    resolvedRegionsExpression=regiondataset;
                } else {
                    throw new ExecutionError("Unknown data object '"+cropRegionsExpression+"'",task.getLineNumber());  
                }            
        } else { // explicit number of bases given      
                if (cropBothDirectionsExpression!=null) { // split bidirectional expression into upstream and downstream   
                    cropUpstreamExpression=cropBothDirectionsExpression;
                    cropDownstreamExpression=cropBothDirectionsExpression;
                } 
                if (cropUpstreamExpression!=null) {
                    Object dataUP = engine.getDataItem(cropUpstreamExpression);
                    if (dataUP==null) {
                         try {
                          int value=Integer.parseInt(cropUpstreamExpression);
                          resolvedUpstreamExpression = new Integer(value);
                         } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+cropUpstreamExpression+"' neither data nor integer constant",task.getLineNumber());}                        
                    } else {
                        if (!(dataUP instanceof NumericVariable || dataUP instanceof NumericMap)) throw new ExecutionError("'"+cropUpstreamExpression+"' is not a valid Numeric Variable in this context",task.getLineNumber());  
                        resolvedUpstreamExpression=dataUP;
                    }
                }

                if (cropDownstreamExpression!=null) {
                    Object dataDOWN = engine.getDataItem(cropDownstreamExpression);
                    if (dataDOWN==null) {
                         try {
                          int value=Integer.parseInt(cropDownstreamExpression);
                          resolvedDownstreamExpression = new Integer(value);
                         } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+cropDownstreamExpression+"' neither data nor integer constant",task.getLineNumber());}                        
                    } else {
                        if (!(dataDOWN instanceof NumericVariable || dataDOWN instanceof NumericMap)) throw new ExecutionError("'"+cropDownstreamExpression+"' is not a valid Numeric Variable in this context",task.getLineNumber());  
                        resolvedDownstreamExpression=dataDOWN;
                    }
                }
        }

        // these parameters are now the only ones we need for further execution of the transform
        task.setParameter(CROP_UPSTREAM_EXPRESSION_RESOLVED,resolvedUpstreamExpression);
        task.setParameter(CROP_DOWNSTREAM_EXPRESSION_RESOLVED,resolvedDownstreamExpression);
        task.setParameter(CROP_TO_REGIONS_RESOLVED,resolvedRegionsExpression);
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
    }
    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
                      
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
        resolveParameters(task);
        RegionDataset cropToRegionsObject=(RegionDataset)task.getParameter(CROP_TO_REGIONS_RESOLVED);
        Object cropUpstreamObject=task.getParameter(CROP_UPSTREAM_EXPRESSION_RESOLVED);
        Object cropDownstreamObject=task.getParameter(CROP_DOWNSTREAM_EXPRESSION_RESOLVED);
        Object orientation=task.getParameter(USE_RELATIVE_ORIENTATION);
        boolean useRelativeOrientation=(orientation instanceof Boolean)?((Boolean)orientation):false;
        
        try {           
            ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
            ArrayList<Sequence> clonedsequences=new ArrayList<Sequence>(sequences.size());
            // For now I clone the full FeatureDatasets (not just the sequences in the collection) as was done in CropTask
            ArrayList<Data> features=engine.getAllDataItemsOfType(FeatureDataset.class);
            ArrayList<FeatureDataset> clonedfeatures=new ArrayList<FeatureDataset>(features.size());
            for (int i=0;i<features.size();i++) { // clone the feature dataset. The clones are registered with the UndoManager to represent to original sequences                    
                FeatureDataset feature=(FeatureDataset)features.get(i);                   
                FeatureDataset clonedfeature=(FeatureDataset)feature.clone();
                clonedfeatures.add(clonedfeature);
            }
            for (int i=0;i<sequences.size();i++) {
               clonedsequences.add((Sequence)sequences.get(i).clone()); 
            }
            int cropped=0;
            int i=0;
            for (Data data:clonedsequences) {  
                task.setProgress(i,sequences.size()*2); // *2 is so that this part will end at 50%
                i++;
                Sequence sequence=(Sequence)data;
                int[] newGenomicRange=getNewGenomicCoordinates(sequence, cropToRegionsObject, cropUpstreamObject, cropDownstreamObject, useRelativeOrientation);
                if (newGenomicRange==null) {
                    engine.logMessage("Unable to crop "+sequence.getName()+" (new sequence will be empty)");                        
                    continue;
                }
                if (newGenomicRange[0]==sequence.getRegionStart() && newGenomicRange[1]==sequence.getRegionEnd()) continue; // no cropping
                int[] newRelativeRange=new int[]{newGenomicRange[0]-sequence.getRegionStart(),newGenomicRange[1]-sequence.getRegionStart()};
                // update sequences
                //gui.logMessage(sequence.getName()+"  genomic["+newGenomicRange[0]+"-"+newGenomicRange[1]+"]   relative["+newRelativeRange[0]+"-"+newRelativeRange[1]+"]  size="+(newRelativeRange[1]-newRelativeRange[0]+1));
                if (newRelativeRange[0]>newRelativeRange[1]) {
                    engine.logMessage("Unable to crop "+sequence.getName()+" (new length is negative)");                        
                    continue;                        
                }               
                sequence.setRegionStart(newGenomicRange[0]);
                sequence.setRegionEnd(newGenomicRange[1]);
                cropped++;
                for (Data feature:clonedfeatures) { // update features for this sequence
                    FeatureSequenceData featureseq=((FeatureDataset)feature).getSequenceByName(sequence.getName());
                    try {
                        featureseq.cropTo(newRelativeRange[0], newRelativeRange[1]);
                    } catch (Exception e) {
                        engine.logMessage(sequence.getName()+":"+feature.getName()+" => "+e.getMessage()+",  ["+newRelativeRange[0]+"-"+newRelativeRange[1]+"]");
                    }
                    if (feature instanceof NumericDataset) ((NumericDataset)feature).updateAllowedMinMaxValuesFromData();
                    else if (feature instanceof RegionDataset) ((RegionDataset)feature).updateMaxScoreValueFromData();
                }                                                     
            }    
            int totaldata=sequences.size()+features.size();
            int counter=0;
            if (cropped>0) {
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
                for (i=0;i<clonedfeatures.size();i++) {
                    if (i%2==0) {
                        task.checkExecutionLock(); // checks to see if this task should suspend execution
                        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                        Thread.yield();
                        task.setProgress(50+(int)((double)counter/(double)totaldata*50.0));
                    }
                    engine.updateDataItem(clonedfeatures.get(i));
                    counter++;                    
                }      
            } // end: if (cropped>0)            

        }  catch (ClassCastException c) {
            throw new ExecutionError(c.getMessage());
        } catch (ExecutionError e) {
            throw e;
        }
        return true;
    }  

    private int[] getNewGenomicCoordinates(Sequence sequence, RegionDataset cropToRegions, Object cropStartObject, Object cropEndObject, boolean useRelativeOrientation) {
        int[] result=new int[]{sequence.getRegionStart(),sequence.getRegionEnd()};
        //gui.logMessage(sequence.getName()+":pre:"+result[0]+"-"+result[1]);
        if (cropToRegions!=null) {
            RegionSequenceData seqTrack=(RegionSequenceData)cropToRegions.getSequenceByName(sequence.getName());
            int[] minmax=seqTrack.getMinMaxPositionsOfAllRegions();
            if (minmax!=null) {
                result[0]=sequence.getRegionStart()+minmax[0];
                result[1]=sequence.getRegionStart()+minmax[1];
                //engine.logMessage(sequence.getName()+"  min="+minmax[0]+"  max="+minmax[1]);            
            }
            if (result[0]<sequence.getRegionStart()) result[0]=sequence.getRegionStart(); // it is only allowed to make sequences smaller, not longer 
            if (result[1]>sequence.getRegionEnd()) result[1]=sequence.getRegionEnd(); // it is only allowed to make sequences smaller, not longer   
            return result;
        } else {
            int cropStart=0;
            int cropEnd=0;
            if (cropStartObject instanceof SequenceNumericMap) cropStart=((SequenceNumericMap)cropStartObject).getValue(sequence.getSequenceName()).intValue();
            else if (cropStartObject instanceof NumericVariable) cropStart=((NumericVariable)cropStartObject).getValue().intValue();
            else if (cropStartObject instanceof Number) cropStart=((Number)cropStartObject).intValue();
            if (cropEndObject instanceof SequenceNumericMap) cropEnd=((SequenceNumericMap)cropEndObject).getValue(sequence.getSequenceName()).intValue();
            else if (cropEndObject instanceof NumericVariable) cropEnd=((NumericVariable)cropEndObject).getValue().intValue();
            else if (cropEndObject instanceof Number) cropEnd=((Number)cropEndObject).intValue();
            
            if (cropStart+cropEnd>sequence.getSize()) {                
                return null;
            } // cropping too much... do nothing
            if (useRelativeOrientation && sequence.getStrandOrientation()==Sequence.REVERSE) {
               result[0]=result[0]+cropEnd;
               result[1]=result[1]-cropStart;                
            } else {
               result[0]=result[0]+cropStart;
               result[1]=result[1]-cropEnd;
            }
            //gui.logMessage(sequence.getName()+":post:"+result[0]+"-"+result[1]);
            return result;
        }
    }     
    
}
