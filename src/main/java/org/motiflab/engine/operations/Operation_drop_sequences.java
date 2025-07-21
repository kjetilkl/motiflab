/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.data.SequenceTextMap;

/**
 *
 * @author kjetikl
 */
public class Operation_drop_sequences extends Operation {
   
    private static final String name="drop_sequences";
    private static final String description="Deletes a set of sequences and also corresponding entries from related data objects (collections, partitions and maps)";
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
    
    @Override
    public boolean affectsSequenceOrder() {return true;}   
    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        // if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");                      
         String subsetName=(String)task.getParameter(OperationTask.SOURCE_NAME);
         if (subsetName==null || subsetName.isEmpty() || subsetName.equals(engine.getDefaultSequenceCollectionName())) throw new ExecutionError("Cannot 'drop' the default Sequence Collection");
         Data seqcol=engine.getDataItem(subsetName);
         if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
         if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
         SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
         task.setParameter("_SHOW_RESULTS", Boolean.FALSE); 
        
         ArrayList<String> sequenceNames=sequenceCollection.getAllSequenceNames(); // the names of all sequences to be removed
         ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine); // the sequences to be removed
        
         // first remove sequence entries from all affected data types        
         ArrayList<Data> list=engine.getAllDataItemsOfType(SequenceCollection.class);   
         for (Data data:list) { // for each SequenceCollection
             if (data.getName().equals(subsetName)) continue; // deal with this later (remove it completely)
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

         // remove the target collection itself (this only contains deleted sequences anyway)
         task.addAffectedDataObject(subsetName, null); // the NULL parameter value signals that the data object is being deleted
         engine.removeDataItem(subsetName);  
        
         // finally remove the sequences themselves      
         for (String sequenceName:sequenceNames) {
            task.addAffectedDataObject(sequenceName, null); // the NULL parameter value signals that the data object is being deleted
            engine.removeDataItem(sequenceName);
         }       
//         
         return true;
    }  

    
}
