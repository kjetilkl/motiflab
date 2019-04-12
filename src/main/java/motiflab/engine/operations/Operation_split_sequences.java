/*
 
 
 */

package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.*;
import motiflab.engine.task.ExecutableTask;


/**
 *
 * @author kjetikl
 */
public class Operation_split_sequences extends Operation {
   
    public static final String DELETE_ORIGINAL_SEQUENCES="deleteOriginalSequences";
    
    private static final String name="split_sequences";
    private static final String description="Creates new sequences based on a set of regions";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};

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
    
    @Override
    public boolean affectsSequenceOrder() {return true;}   
    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
         task.setProgress(1);
        // if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");      
         String resultName=(String)task.getParameter(OperationTask.TARGET_NAME);     
         String regionDatasetName=(String)task.getParameter(OperationTask.SOURCE_NAME);
         if (regionDatasetName==null || regionDatasetName.isEmpty()) throw new ExecutionError("Missing required Region Dataset");
         Data regionDataset=engine.getDataItem(regionDatasetName);
         if (regionDataset==null) throw new ExecutionError("No such dataset: '"+regionDataset+"'",task.getLineNumber());
         if (!(regionDataset instanceof RegionDataset)) throw new ExecutionError(regionDataset+" is not a Region Dataset",task.getLineNumber());
         task.setParameter("_SHOW_RESULTS", Boolean.FALSE);         
         if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
                      
         String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
         if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
         Data seqcol=engine.getDataItem(subsetName);
         if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
         if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
         SequenceCollection sequenceCollection=(SequenceCollection)seqcol;        
         if (subsetName.equals(engine.getDefaultSequenceCollectionName())) {
             sequenceCollection=sequenceCollection.clone(); // the default collection will change when adding sequences, so we make a copy instead
         }
         task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
         
         Boolean deleteOriginals=(Boolean)task.getParameter(Operation_split_sequences.DELETE_ORIGINAL_SEQUENCES);
         if (deleteOriginals==null) deleteOriginals=Boolean.FALSE;
         SequencePartition partition=new SequencePartition(resultName);
         
         // create new sequences (with featuredatasets) and add them
         
         ArrayList<String> sequenceNames=sequenceCollection.getAllSequenceNames();  
         int size=sequenceNames.size();
         int count=0;
         ArrayList<Data> featureDatasets=engine.getAllDataItemsOfType(FeatureDataset.class);         
         ArrayList<FeatureDataset> featureDatasetClones=new ArrayList<>();
         for (Data feature:featureDatasets) {
             FeatureDataset newDataset=null;
             if (feature instanceof RegionDataset) newDataset=((RegionDataset)feature).clone();
             if (feature instanceof NumericDataset) newDataset=((NumericDataset)feature).clone();
             if (feature instanceof DNASequenceDataset) newDataset=((DNASequenceDataset)feature).clone();
             if (newDataset!=null) featureDatasetClones.add(newDataset); else throw new ExecutionError("Unable to clone feature dataset "+feature.getName());
         }        
         
         for (String sequenceName:sequenceNames) {            
             Sequence parentSequence=(Sequence)engine.getDataItem(sequenceName, Sequence.class);
             if (parentSequence==null) throw new ExecutionError("Sequence not found: "+sequenceName);
             RegionSequenceData regionsequence=(RegionSequenceData)((RegionDataset)regionDataset).getSequenceByName(sequenceName);
             ArrayList<Region> regions=regionsequence.getAllRegions();
             for (int i=0;i<regions.size();i++) { // each region will become a new sequence
                 Region region=regions.get(i);
                 String newSequenceName=sequenceName+"_"+(i+1); // 
                 if (engine.getDataItem(newSequenceName)!=null) throw new ExecutionError("Trying to create a new sequence with name '"+newSequenceName+"', but an object with that name already exists.");
                 int orientation=region.getOrientation();
                 if (orientation==Region.INDETERMINED) orientation=Region.DIRECT; // the orientation of the new sequence is based on the region, not the parent sequence
                 int newSequenceStart=region.getGenomicStart();
                 int newSequenceEnd=region.getGenomicEnd();
                 if (newSequenceStart<parentSequence.getRegionStart()) newSequenceStart=parentSequence.getRegionStart(); // If the region extends beyond the edge of the parent sequence, crop the sequence at the edge. Do not allow it to extend past the parent just because the region does so
                 if (newSequenceEnd>parentSequence.getRegionEnd()) newSequenceEnd=parentSequence.getRegionEnd(); // If the region extends beyond the edge of the parent sequence, crop the sequence at the edge. Do not allow it to extend past the parent just because the region does so                 
                 Sequence newSequence=new Sequence(newSequenceName, parentSequence.getOrganism(), parentSequence.getGenomeBuild(), parentSequence.getChromosome(), newSequenceStart, newSequenceEnd, parentSequence.getGeneName(), parentSequence.getTSS(), parentSequence.getTES(), orientation); 
                 partition.addSequence(newSequence, sequenceName); // add new sequence to a cluster named after the parent sequence                 
                 task.addAffectedDataObject(newSequenceName, Sequence.class); //
                 engine.storeDataItem_useBackdoor(newSequence);  // since feature data is already loaded we need to use the backdoor to add new sequences     
                 // create new entries in all the feature datasets
                 for (Data feature:featureDatasetClones) { // for each feature dataset: create and add new sequence                  
                     FeatureSequenceData sequencedata=null;
                          if (feature instanceof RegionDataset)  sequencedata= getRegionSequence(newSequence, (RegionDataset)feature, sequenceName);
                     else if (feature instanceof NumericDataset) sequencedata=getNumericSequence(newSequence, (NumericDataset)feature, sequenceName);
                     else if (feature instanceof DNASequenceDataset) sequencedata=getDNASequence(newSequence, (DNASequenceDataset)feature, sequenceName);
                     if (sequencedata==null) throw new ExecutionError("Unable to create feature dataset sequence for "+newSequenceName);
                     else ((FeatureDataset)feature).addSequence(sequencedata);
                 }
             } // end for each region    
             count++;
             task.setProgress(count,size+(deleteOriginals?12:2)); // add some room for more progress is the sequences are also to be deleted
         }              
         for (FeatureDataset feature:featureDatasetClones) { // for each feature dataset                  
             task.addAffectedDataObject(feature.getName(), feature.getClass()); // 
             try {engine.updateDataItem(feature);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
         }                  
         if (deleteOriginals) {
             deleteSequences(sequenceCollection, task);        
         }
         task.setProgress(100);
         task.checkExecutionLock(); // checks to see if this task should suspend execution
         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
         try {engine.updateDataItem(partition);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
         return true;         
    }  

    
    @SuppressWarnings("unchecked")
    private RegionSequenceData getRegionSequence(Sequence newSequence, RegionDataset feature, String parentSequenceName) throws Exception {
        RegionSequenceData parentSequence=(RegionSequenceData)feature.getSequenceByName(parentSequenceName);
        RegionSequenceData sequence=new RegionSequenceData(newSequence); // this is empty
        // we must now copy all the regions from the parent sequence into the new sequence
        ArrayList<Region> regionsList=(ArrayList<Region>)parentSequence.getValueInGenomicInterval(newSequence.getRegionStart(), newSequence.getRegionEnd());        
        for (Region newRegion:regionsList) {
            Region regionCopy=newRegion.clone();
            // we must change the coordinates of the new region since it is relative to a different sequence.             
            regionCopy.updatePositionReferenceFrame(parentSequence.getRegionStart(), sequence.getRegionStart()); // This will also update any nested regions properly!  
            sequence.addRegion(regionCopy);    
        }
        return sequence;
    }
    
    private NumericSequenceData getNumericSequence(Sequence newSequence, NumericDataset feature, String parentSequenceName) throws Exception {
        NumericSequenceData parentSequence=(NumericSequenceData)feature.getSequenceByName(parentSequenceName);
        NumericSequenceData sequence=new NumericSequenceData(newSequence,0);
        double[] newdata=(double[])parentSequence.getValueInGenomicInterval(newSequence.getRegionStart(), newSequence.getRegionEnd());
        sequence.setData(newdata);
        return sequence;
    }    
    
    private DNASequenceData getDNASequence(Sequence newSequence, DNASequenceDataset feature, String parentSequenceName) throws Exception {
        DNASequenceData parentSequence=(DNASequenceData)feature.getSequenceByName(parentSequenceName);
        char[] newdata=(char[])parentSequence.getValueInGenomicInterval(newSequence.getRegionStart(), newSequence.getRegionEnd());        
        DNASequenceData sequence=new DNASequenceData(newSequence,newdata);
        return sequence;
    }  
    
    private void deleteSequences(SequenceCollection collection, ExecutableTask task) throws Exception {;
         ArrayList<Sequence> sequences=collection.getAllSequences(engine);
         ArrayList<String> sequenceNames=collection.getAllSequenceNames();          
         
         // remove sequence entries from all other feature datasets, collections, partitions and maps
         ArrayList<Data> list=engine.getAllDataItemsOfType(SequenceCollection.class);   
         for (Data data:list) { // for each SequenceCollection
             SequenceCollection copy=((SequenceCollection)data).clone();
             for (Sequence seq:sequences) {
                ((SequenceCollection)copy).removeSequence(seq);
             }
             task.addAffectedDataObject(copy.getName(), SequenceCollection.class);
             engine.updateDataItem(copy);
         }

         list=engine.getAllDataItemsOfType(SequencePartition.class);   
         for (Data data:list) { // for each SequencePartition
             SequencePartition copy=((SequencePartition)data).clone();
             for (Sequence seq:sequences) {
                ((SequencePartition)copy).removeSequence(seq);
             }
             task.addAffectedDataObject(copy.getName(), SequencePartition.class);
             engine.updateDataItem(copy);
         }    

         list=engine.getAllDataItemsOfType(SequenceNumericMap.class);   
         for (Data data:list) { // for each SequenceNumericMap
             SequenceNumericMap copy=((SequenceNumericMap)data).clone();
             for (Sequence seq:sequences) {
                ((SequenceNumericMap)copy).removeValue(seq.getSequenceName());
             }
             task.addAffectedDataObject(copy.getName(), SequenceNumericMap.class);
             engine.updateDataItem(copy);
         }  

         list=engine.getAllDataItemsOfType(SequenceTextMap.class);   
         for (Data data:list) { // for each SequenceTextMap
             SequenceTextMap copy=((SequenceTextMap)data).clone();
             for (Sequence seq:sequences) {
                ((SequenceTextMap)copy).removeValue(seq.getSequenceName());
             }
             task.addAffectedDataObject(copy.getName(), SequenceTextMap.class);
             engine.updateDataItem(copy);
         }

         list=engine.getAllDataItemsOfType(ExpressionProfile.class); 
         for (Data data:list) { // for each ExpressionProfile
             ExpressionProfile copy=((ExpressionProfile)data).clone();
             for (Sequence seq:sequences) {
                ((ExpressionProfile)copy).removeSequence(seq.getSequenceName());
             }
             task.addAffectedDataObject(copy.getName(), ExpressionProfile.class);
             engine.updateDataItem(copy);
         }     

         list=engine.getAllDataItemsOfType(RegionDataset.class); 
         for (Data data:list) { // for each ExpressionProfile
             RegionDataset copy=((RegionDataset)data).clone();
             for (Sequence seq:sequences) {
                ((RegionDataset)copy).removeSequence(seq.getSequenceName());
             }
             task.addAffectedDataObject(copy.getName(), RegionDataset.class);
             engine.updateDataItem(copy);
         }  
         list=engine.getAllDataItemsOfType(NumericDataset.class); 
         for (Data data:list) { // for each ExpressionProfile
             NumericDataset copy=((NumericDataset)data).clone();
             for (Sequence seq:sequences) {
                ((NumericDataset)copy).removeSequence(seq.getSequenceName());
             }
             task.addAffectedDataObject(copy.getName(), NumericDataset.class);
             engine.updateDataItem(copy);
         }
         list=engine.getAllDataItemsOfType(DNASequenceDataset.class); 
         for (Data data:list) { // for each ExpressionProfile
             DNASequenceDataset copy=((DNASequenceDataset)data).clone();
             for (Sequence seq:sequences) {
                ((DNASequenceDataset)copy).removeSequence(seq.getSequenceName());
             }
             task.addAffectedDataObject(copy.getName(), DNASequenceDataset.class);
             engine.updateDataItem(copy);
         }             
         
         // finally remove the sequences themselves          
         for (String sequenceName:sequenceNames) {
            task.addAffectedDataObject(sequenceName, null); // the NULL parameter value signals that the data object is being deleted
            engine.removeDataItem(sequenceName);
         }         
    }
    
}
