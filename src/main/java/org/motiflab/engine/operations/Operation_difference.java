package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.*;


/**
 *
 * @author kjetikl
 */
public class Operation_difference extends Operation {
    public static final String COMPARE_AGAINST_DATA="compareAgainst";
    public static final String REGION_DATASET_OPTIONS="regionDatasetOptions"; // the properties that should be considered when deciding if two regions from different tracks are really the same
    
    public static final String REGION_DATASET_TARGET_PROPERTY="regionNumericProperty"; // A specific numeric property that should compared
    
    private static final int COMPARE_ALL_PROPERTIES=0;      
    private static final int COMPARE_ONLY_LOCATION_AND_TYPE=1;    
    private static final int COMPARE_ONLY_STANDARD=2;    
    
    private static final String name="difference";
    private static final String description="Compares one data object to another and reports the differences";
    private Class[] datasourcePreferences=new Class[]{FeatureDataset.class, DataCollection.class, DataMap.class};
       
    private static String[] regiondatasetOptions=new String[]{"compare only location and type", "compare only standard properties"};

    @Override
    public String getOperationGroup() {
        return "Derive";
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
    public boolean canHaveMultipleInput() {
        return true;
    }

    
    public static String[] getRegionDatasetOptions() {
        return regiondatasetOptions;
    }
    
    private int getComparisonOptionFromString(String option) {
        if (option==null || option.isEmpty()) return COMPARE_ALL_PROPERTIES;
        if (option.equalsIgnoreCase("compare only standard properties")) return COMPARE_ONLY_STANDARD;
        else if (option.equalsIgnoreCase("compare only location and type")) return COMPARE_ONLY_LOCATION_AND_TYPE;
        return COMPARE_ALL_PROPERTIES;
    }

    @Override
    public boolean execute(OperationTask task) throws Exception {
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        String sourceName1=task.getSourceDataName();
        if (sourceName1==null || sourceName1.isEmpty()) throw new ExecutionError("Missing name for data object",task.getLineNumber());
        String sourceName2=(String)task.getParameter(COMPARE_AGAINST_DATA);
        if (sourceName2==null || sourceName2.isEmpty()) throw new ExecutionError("Missing name for second data object",task.getLineNumber());
        Data data1=engine.getDataItem(sourceName1);
        if (data1==null) throw new ExecutionError("Unknown data object:"+sourceName1);
        Data data2=engine.getDataItem(sourceName2);
        if (data2==null) throw new ExecutionError("Unknown data object:"+sourceName2);
        if (data1.getClass()!=data2.getClass()) throw new ExecutionError("Data object '"+sourceName1+"' cannot be compared to '"+sourceName2+"' because they have different types");       
        Data targetData=null;
        if (NumericMap.class.isAssignableFrom(data1.getClass())) targetData=compareNumericMaps((NumericMap)data1,(NumericMap)data2,task);           
        else if (TextMap.class.isAssignableFrom(data1.getClass())) targetData=compareTextMaps((TextMap)data1,(TextMap)data2,task);           
        else if (DataCollection.class.isAssignableFrom(data1.getClass())) targetData=compareCollections((DataCollection)data1,(DataCollection)data2,task);
        else if (data1 instanceof FeatureDataset) {
            if (data1 instanceof DNASequenceDataset || data1 instanceof NumericDataset) targetData=setupNumericResultDataset(task);
            else if (data1 instanceof RegionDataset) targetData=setupRegionResultDataset(task);
            
            ArrayList<Sequence> sequences=engine.getDefaultSequenceCollection().getAllSequences(engine);            
            TaskRunner taskRunner=engine.getTaskRunner();
            task.setProgress(0L,sequences.size());
            long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

            ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
            for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask((FeatureDataset)data1,(FeatureDataset)data2,(FeatureDataset)targetData, sequence.getName(), task, counters));
            List<Future<FeatureSequenceData>> futures=null;
            int countOK=0;            
            try {
                futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
                for (Future<FeatureSequenceData> future:futures) {
                    if (future.isDone() && !future.isCancelled()) {
                        future.get(); // this blocks until completion but the return value is not used
                        countOK++;
                    }
                }
            } catch (Exception e) {  
               taskRunner.shutdownNow(); // Note: this will abort all executing tasks (even those that did not cause the exception), but that is OK. 
               if (e instanceof java.util.concurrent.ExecutionException) throw (Exception)e.getCause(); 
               else throw e; 
            }       
            if (countOK!=sequences.size()) {
                throw new ExecutionError("Some mysterious error occurred while performing operation: "+getName());
            }              
            if (data1 instanceof RegionDataset) {
                if (((RegionDataset)data1).isMotifTrack() && ((RegionDataset)data2).isMotifTrack()) ((RegionDataset)targetData).setMotifTrack(true);
                if (((RegionDataset)data1).isModuleTrack() && ((RegionDataset)data2).isModuleTrack()) ((RegionDataset)targetData).setModuleTrack(true);
                if (((RegionDataset)data1).isNestedTrack() && ((RegionDataset)data2).isNestedTrack()) ((RegionDataset)targetData).setNestedTrack(true);                  
            }
        }
        else throw new ExecutionError("Unable to compare data objects of type: "+engine.getTypeNameForDataClass(data1.getClass()));
        task.setProgress(100);
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
   
    
    private void transformDNA(DNASequenceData seq1, DNASequenceData seq2, NumericSequenceData target, OperationTask task) throws Exception {
         for (int pos=0;pos<seq1.getSize();pos++) {
             if (seq1.getValueAtRelativePosition(pos).charValue()!=seq2.getValueAtRelativePosition(pos).charValue()) target.setValueAtRelativePosition(pos, 1.0);
         }        
    }
    
    private void transformNumeric(NumericSequenceData seq1, NumericSequenceData seq2, NumericSequenceData target, OperationTask task) throws Exception {
         for (int pos=0;pos<seq1.getSize();pos++) {
             double diff=seq1.getValueAtRelativePosition(pos)-seq2.getValueAtRelativePosition(pos);
             target.setValueAtRelativePosition(pos, diff);
         }     
    }    
    
    private void transformRegions(RegionSequenceData seq1, RegionSequenceData seq2, RegionSequenceData resultSeq, OperationTask task) throws Exception {
         int compare=getComparisonOptionFromString((String)task.getParameter(REGION_DATASET_OPTIONS));
         String targetProperty=null;
         Object prop=task.getParameter(REGION_DATASET_TARGET_PROPERTY);      
         if (prop instanceof String && !(((String)prop).isEmpty())) targetProperty=(String)prop;
         String track1=seq1.getParent().getName();
         String track2=seq2.getParent().getName();        
         if (seq1.getNumberOfRegions()>0 && seq2.getNumberOfRegions()==0) { // one sequence is empty while the other contains regions (all regions are different)
              for (Region region:seq1.getAllRegions()) {
                 Region clonedregion=region.clone();
                 clonedregion.setProperty("onlyIn", track1);
                 //clonedregion.setProperty("difference_"+targetProperty, 0); // default to 0 if only in one track (use onlyIn property to decide whether to use it)
                 resultSeq.addRegionWithoutSorting(clonedregion);                    
             }                
         } else if (seq1.getNumberOfRegions()==0 && seq2.getNumberOfRegions()>0) { // one sequence is empty while the other contains regions (all regions are different)
              for (Region region:seq2.getAllRegions()) {
                 Region clonedregion=region.clone();
                 clonedregion.setProperty("onlyIn", track2);
                 //clonedregion.setProperty("difference_"+targetProperty, 0);  // default to 0 if only in one track (use onlyIn property to decide whether to use it)
                 resultSeq.addRegionWithoutSorting(clonedregion);
             }                  
         } else if (seq1.getNumberOfRegions()>0 && seq2.getNumberOfRegions()>0) {
              ArrayList<Region> seq1Regions=seq1.getAllRegions();
              ArrayList<Region> seq2Regions=seq2.getAllRegions();
              int pointerSeq1=0;
              int pointerSeq2=0;     
              while (pointerSeq1<seq1Regions.size() && pointerSeq2<seq2Regions.size()) {
                  Region reg1=seq1Regions.get(pointerSeq1);
                  Region reg2=seq2Regions.get(pointerSeq2);
                  if (reg1.getRelativeStart()<reg2.getRelativeStart()) {
                      while(reg1.getRelativeStart()<reg2.getRelativeStart()) { // these regions in track1 start before the next region in track2, hence they can only be in track1
                         Region clonedregion=reg1.clone();
                         clonedregion.setProperty("onlyIn", track1);
                         //clonedregion.setProperty("difference_"+targetProperty, 0);  // default to 0 if only in one track (use onlyIn property to decide whether to use it)
                         resultSeq.addRegionWithoutSorting(clonedregion);    
                         pointerSeq1++;
                         if (pointerSeq1<seq1Regions.size()) reg1=seq1Regions.get(pointerSeq1);
                         else break;
                      }
                  } else if (reg2.getRelativeStart()<reg1.getRelativeStart()) { // these regions in track2 start before the next region in track1, hence they can only be in track2
                       while(reg2.getRelativeStart()<reg1.getRelativeStart()) {
                         Region clonedregion=reg2.clone();
                         clonedregion.setProperty("onlyIn", track2);
                         //clonedregion.setProperty("difference_"+targetProperty, 0);  // default to 0 if only in one track (use onlyIn property to decide whether to use it)
                         resultSeq.addRegionWithoutSorting(clonedregion);    
                         pointerSeq2++;
                         if (pointerSeq2<seq2Regions.size()) reg2=seq2Regions.get(pointerSeq2);
                         else break;
                      }                     
                  }
                  if (reg1.getRelativeStart()==reg2.getRelativeStart()) {
                      int pos=reg1.getRelativeStart();
                      int firstRegionAtNextPosSeq1=pointerSeq1;
                      int firstRegionAtNextPosSeq2=pointerSeq2;
                      while (firstRegionAtNextPosSeq1<seq1Regions.size() && seq1Regions.get(firstRegionAtNextPosSeq1).getRelativeStart()==pos) {                        
                          firstRegionAtNextPosSeq1++;                        
                      }
                      while (firstRegionAtNextPosSeq2<seq2Regions.size() && seq2Regions.get(firstRegionAtNextPosSeq2).getRelativeStart()==pos) {                        
                          firstRegionAtNextPosSeq2++;                        
                      }        
                      ArrayList<Region> diff=compareRegionDatasetsAtSamePosition(subList(seq1Regions, pointerSeq1, firstRegionAtNextPosSeq1), subList(seq2Regions, pointerSeq2, firstRegionAtNextPosSeq2), track1, track2, compare, targetProperty);
                      for (Region reg:diff) resultSeq.addRegionWithoutSorting(reg);
                      pointerSeq1=firstRegionAtNextPosSeq1;
                      pointerSeq2=firstRegionAtNextPosSeq2;
                  }
              } // end while-loop 
              // one (or both) of the sequences has now been exhausted, but there might be regions left in the other
              if (pointerSeq1==seq1Regions.size() && pointerSeq2<seq2Regions.size()) {
                  for (int j=pointerSeq2;j<seq2Regions.size();j++) {
                     Region clonedregion=seq2Regions.get(j).clone();
                     clonedregion.setProperty("onlyIn", track2);
                     //clonedregion.setProperty("difference_"+targetProperty, 0);  // default to 0 if only in one track (use onlyIn property to decide whether to use it)
                     resultSeq.addRegionWithoutSorting(clonedregion);                           
                  }
              }
              if (pointerSeq2==seq2Regions.size() && pointerSeq1<seq1Regions.size()) {
                   for (int j=pointerSeq1;j<seq1Regions.size();j++) {
                     Region clonedregion=seq1Regions.get(j).clone();
                     clonedregion.setProperty("onlyIn", track1);
                     //clonedregion.setProperty("difference_"+targetProperty, 0);  // default to 0 if only in one track (use onlyIn property to decide whether to use it)
                     resultSeq.addRegionWithoutSorting(clonedregion);                           
                  }                     
              }                  
         }
         // end sequence
         resultSeq.updateRegionSortOrder();     
    }
    
    private NumericDataset setupNumericResultDataset(OperationTask task) throws Exception  {
        String targetName=task.getTargetDataName();
        NumericDataset result=new NumericDataset(targetName);
        result.setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));
        return result;
    }  
    
    private RegionDataset setupRegionResultDataset(OperationTask task) throws Exception  {
        String targetName=task.getTargetDataName();
        RegionDataset result=new RegionDataset(targetName);
        result.setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));
        return result;
    }      
    

    private ArrayList<Region> subList(ArrayList<Region> original, int fromIndex, int toIndex) {
        ArrayList<Region> result=new ArrayList<Region>();
        for (int i=fromIndex;i<toIndex;i++) result.add(original.get(i));
        return result;
    }
    
    /** Compares two lists of regions that all start at the same position and returns the difference as a new list */
    private ArrayList<Region> compareRegionDatasetsAtSamePosition(ArrayList<Region> regions1, ArrayList<Region> regions2, String track1, String track2, int comparison, String targetProperty) {
        ArrayList<Region> diff=new ArrayList<Region>();
        Iterator<Region> iterator=regions1.iterator();
        while (iterator.hasNext()) {
            Region reg=iterator.next();
            Region identical=findIdenticalRegion(regions2,reg, comparison);
            if (identical==null) { // the region from "regions1" was not found in "regions2". Add it to the "diff" list
                Region clonedRegion=reg.clone();
                clonedRegion.setProperty("onlyIn", track1);                
                diff.add(clonedRegion);
            } else { // the regions are identical. remove from "regions2" list
                regions2.remove(identical);
                if (targetProperty==null) {
                    iterator.remove(); // remove from difference list the region that is present in both tracks (is this really necessary?)
                } else { // do not remove identical regions. Rather compare the target property
                    Object value1=reg.getProperty(targetProperty); // value in the first region
                    Object value2=identical.getProperty(targetProperty); // value in the second region
                    if (value1 instanceof Number && value2 instanceof Number) {
                        Region clonedRegion=reg.clone();
                        double diffValue=((Number)value1).doubleValue()-((Number)value2).doubleValue();
                        clonedRegion.setProperty("difference "+targetProperty, diffValue);                       
                        diff.add(clonedRegion);                        
                    }
                }
            }
        }
        for (Region reg:regions2) { // Add the remaining regions from "regions2" that were not found in "region" to the "diff" list
           Region clonedRegion=reg.clone();
           clonedRegion.setProperty("onlyIn", track2);
           diff.add(clonedRegion);           
        }  
        return diff;
    }
    
    private Region findIdenticalRegion(ArrayList<Region> list, Region region, int comparison) {
        for (Region reg2:list) {
            if (comparison==COMPARE_ALL_PROPERTIES) {
                if (reg2.isIdenticalTo(region)) return reg2;
            } else if (comparison==COMPARE_ONLY_LOCATION_AND_TYPE) {
                if (reg2.hasSameLocationAndType(region)) return reg2;                
            } else if (comparison==COMPARE_ONLY_STANDARD) {
                if (reg2.hasSameStandardProperties(region)) return reg2; 
            }
        }
        return null;
    }
//    private void removeIdenticalRegion(ArrayList<Region> list, Region region) {
//        Region target=null;
//        for (Region reg:list) {
//            if (reg.isIdenticalTo(region)) {target=reg;break;}
//        }
//        if (target!=null) list.remove(target);
//    }    
    
    private NumericMap compareNumericMaps(NumericMap data1, NumericMap data2, OperationTask task) throws Exception  {
        String targetName=task.getTargetDataName();
        NumericMap result=null;
             if (data1 instanceof MotifNumericMap) result=new MotifNumericMap(targetName,0);
        else if (data1 instanceof ModuleNumericMap) result=new ModuleNumericMap(targetName,0);
        else if (data1 instanceof SequenceNumericMap) result=new SequenceNumericMap(targetName,0);
        ArrayList<String> keys=data1.getAllKeys(engine);       
        for (String key:keys) {
            double diff=data1.getValue(key)-data2.getValue(key);
            result.setValue(key, diff);
        }
        result.setDefaultValue(data1.getValue()-data2.getValue()); // compare default values also
        return result;
    }
    
    private TextMap compareTextMaps(TextMap data1, TextMap data2, OperationTask task) throws Exception  {
        String targetName=task.getTargetDataName();
        TextMap result=null;
             if (data1 instanceof MotifTextMap) result=new MotifTextMap(targetName,"");
        else if (data1 instanceof ModuleTextMap) result=new ModuleTextMap(targetName,"");
        else if (data1 instanceof SequenceTextMap) result=new SequenceTextMap(targetName,"");
        ArrayList<String> keys=data1.getAllKeys(engine);       
        for (String key:keys) {
            String value1=data1.getValue(key);
            String value2=data2.getValue(key);
            if (value1.equals(value2)) result.setValue(key,"");
            else result.setValue(key,value1+" <> "+value2);
        }
        // compare default values also
        String value1=data1.getValue();
        String value2=data2.getValue() ;
        if (value1.equals(value2)) result.setDefaultValue("");
        else result.setDefaultValue(value1+" <> "+value2);        
        return result;
    }    
    
    private DataPartition compareCollections(DataCollection data1, DataCollection data2, OperationTask task) throws Exception  {
        String targetName=task.getTargetDataName();
        DataPartition result=null;
             if (data1 instanceof MotifCollection) result=new MotifPartition(targetName);
        else if (data1 instanceof ModuleCollection) result=new ModulePartition(targetName);
        else if (data1 instanceof SequenceCollection) result=new SequencePartition(targetName);
        for (String entry:data1.getValues()) {
            Data entryitem=engine.getDataItem(entry);
            if (entryitem==null) continue;
            if (data2.contains(entry)) result.addItem(entryitem, "Present_in_both");
            else result.addItem(entryitem, "Only_in_"+data1.getName());
        }
        for (String entry:data2.getValues()) {
            Data entryitem=engine.getDataItem(entry);
            if (entryitem==null) continue;
            if (!data1.contains(entry)) result.addItem(entryitem, "Only_in_"+data2.getName());
        }
        ArrayList<Data> allEntries=engine.getAllDataItemsOfType(result.getMembersClass());
        for (Data entry:allEntries) {
            if (!result.contains(entry.getName())) result.addItem(entry, "Present_in_neither");
        }
        return result;
    }
    
    protected class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final FeatureDataset targetDataset;
        final FeatureDataset sourceDataset1;
        final FeatureDataset sourceDataset2;        
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;  
        
        public ProcessSequenceTask(FeatureDataset sourceDataset1, FeatureDataset sourceDataset2, FeatureDataset targetDataset, String sequencename, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.sourceDataset1=sourceDataset1;
           this.sourceDataset2=sourceDataset2;
           this.targetDataset=targetDataset; 
           this.counters=counters;
           this.task=task;
        }
         
        @Override
        @SuppressWarnings("unchecked")
        public FeatureSequenceData call() throws Exception {
            synchronized(counters) {
               counters[0]++; // number of sequences started  
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            FeatureSequenceData sourceSequence1=(FeatureSequenceData)sourceDataset1.getSequenceByName(sequencename);
            FeatureSequenceData sourceSequence2=(FeatureSequenceData)sourceDataset2.getSequenceByName(sequencename);
            FeatureSequenceData targetSequence=(FeatureSequenceData)targetDataset.getSequenceByName(sequencename);

            if (sourceSequence1 instanceof DNASequenceData) transformDNA((DNASequenceData)sourceSequence1,(DNASequenceData)sourceSequence2,(NumericSequenceData)targetSequence, task);
            else if (sourceSequence1 instanceof NumericSequenceData) transformNumeric((NumericSequenceData)sourceSequence1,(NumericSequenceData)sourceSequence2,(NumericSequenceData)targetSequence, task);
            else if (sourceSequence1 instanceof RegionSequenceData) transformRegions((RegionSequenceData)sourceSequence1,(RegionSequenceData)sourceSequence2,(RegionSequenceData)targetSequence, task);
            else throw new ExecutionError("Unsupported source data in operation 'difference'"); 
                
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                task.setStatusMessage("Executing "+task.getOperationName()+":  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return targetSequence;
        }   
    }    
    
}
