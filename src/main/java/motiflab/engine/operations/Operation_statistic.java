/*
 
 
 */

package motiflab.engine.operations;

import motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.TaskRunner;
import motiflab.engine.data.*;


/**
 *
 * @author kjetikl
 */
public class Operation_statistic extends Operation {
    
    public static final String STRAND_DIRECT="direct strand";
    public static final String STRAND_REVERSE="reverse strand"; 
    public static final String STRAND_GENE="relative strand"; 
    public static final String STRAND_OPPOSITE="opposite strand";
    public static final String STRAND="strand"; 

    public static final String STATISTIC_FUNCTION="statistic"; //
    public static final String FOR_INTERNAL_USE="_forInternalUse"; //
    public static final String INITIATE_ALL_TO_ZERO="initiateToZero";
    public static final String RESULT="_result"; //
    
    public static final String REGION_DATASET_PROPERTY="regionDatasetProperty"; //

    private static final String name="statistic";
    private static final String description="Calculates a simple statistic (such as maximum or average value) for each sequence in a dataset";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class, RegionDataset.class, DNASequenceDataset.class};
    
    private final String lock="lock"; // just used for synchronization of threads

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
    public boolean isSubrangeApplicable() {return true;}
    
    public void resolveParameters(OperationTask task) throws Exception {
        String statistic=(String)task.getParameter(STATISTIC_FUNCTION);
        if (statistic==null) throw new ExecutionError("Missing statistic function");
        // for backwards compatibility
             if (statistic.equalsIgnoreCase("minimum score")) {statistic="minimum"; task.setParameter(REGION_DATASET_PROPERTY, "score");}
        else if (statistic.equalsIgnoreCase("maximum score")) {statistic="maximum"; task.setParameter(REGION_DATASET_PROPERTY, "score");}
        else if (statistic.equalsIgnoreCase("average score")) {statistic="average"; task.setParameter(REGION_DATASET_PROPERTY, "score");}
        else if (statistic.equalsIgnoreCase("extreme score")) {statistic="extreme"; task.setParameter(REGION_DATASET_PROPERTY, "score");}
        else if (statistic.equalsIgnoreCase("sum score")) {statistic="sum"; task.setParameter(REGION_DATASET_PROPERTY, "score");}
        if (!isKnownFunction(statistic)) throw new ExecutionError("Unknown statistic function");        
    }
    
    public void transformSequence(FeatureSequenceData sourceSequence, SequenceNumericMap targetdata, OperationTask task) throws Exception {
        String statistic=(String)task.getParameter(STATISTIC_FUNCTION);        
        String sequenceName=sourceSequence.getName();
        if (sourceSequence instanceof NumericSequenceData) {
            if (statistic.equalsIgnoreCase("minimum value")) {
                double min=Double.MAX_VALUE;
                int basecount=0;
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    if (!positionSatisfiesCondition(sequenceName, i, task)) continue;
                    double value=((NumericSequenceData)sourceSequence).getValueAtGenomicPosition(i);
                    if (value<min) min=value;
                    basecount++;
                }
                synchronized(lock) {
                    if (basecount>0) targetdata.setValue(sequenceName, min);
                    if (basecount>0 && min<targetdata.getValue()) targetdata.setDefaultValue(min);
                }
            } else if (statistic.equalsIgnoreCase("maximum value")) {
                double max=-Double.MAX_VALUE;
                int basecount=0;
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    if (!positionSatisfiesCondition(sequenceName, i, task)) continue;
                    double value=((NumericSequenceData)sourceSequence).getValueAtGenomicPosition(i);
                    if (value>max) max=value;
                    basecount++;
                }
                synchronized(lock) {                
                    if (basecount>0) targetdata.setValue(sequenceName, max);
                    if (basecount>0 && max>targetdata.getValue()) targetdata.setDefaultValue(max);
                }
            } else if (statistic.equalsIgnoreCase("extreme value")) {
                double extreme=0;
                int basecount=0;
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    if (!positionSatisfiesCondition(sequenceName, i, task)) continue;
                    double value=((NumericSequenceData)sourceSequence).getValueAtGenomicPosition(i);
                    if (Math.abs(value)>Math.abs(extreme)) extreme=value;
                    basecount++;
                }
                synchronized(lock) {
                    if (basecount>0) targetdata.setValue(sequenceName, extreme);
                    if (basecount>0 && Math.abs(extreme)>Math.abs(targetdata.getValue())) targetdata.setDefaultValue(extreme);
                }
            } else if (statistic.equalsIgnoreCase("average value") || statistic.equalsIgnoreCase("sum values")) {
                double sum=0;
                int basecount=0;
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    if (!positionSatisfiesCondition(sequenceName, i, task)) continue;
                    double value=((NumericSequenceData)sourceSequence).getValueAtGenomicPosition(i);
                    sum+=value;
                    basecount++;
                }
                synchronized(lock) {                
                    double average=(basecount==0)?0:(double)sum/(double)basecount;
                    if (statistic.equalsIgnoreCase("sum values")) targetdata.setValue(sequenceName, sum);
                    else if (basecount>0) targetdata.setValue(sequenceName, average);
                    double globalsum=(Double)task.getParameter("GlobalSum");
                    task.setParameter("GlobalSum", globalsum+sum);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+basecount);   
                }
            } else if (statistic.equalsIgnoreCase("base count")) {
                int basecount=0;
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    if (!positionSatisfiesCondition(sequenceName, i, task)) continue;
                    basecount++;
                }
                synchronized(lock) {
                    targetdata.setValue(sequenceName, basecount);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+basecount);   
                }
            } else throw new ExecutionError("The statistic '"+statistic+"' is not applicable to Numeric Datasets");
        } else if (sourceSequence instanceof RegionSequenceData) {
            String propertyName=(task.getParameter(REGION_DATASET_PROPERTY) instanceof String)?((String)task.getParameter(REGION_DATASET_PROPERTY)):"score";
            ArrayList<Region> regions=((RegionSequenceData)sourceSequence).getAllRegions();
            // filter out regions that do not satisfy the conditions
            Iterator iter=regions.iterator();
            while (iter.hasNext()) {
                Region region=(Region)iter.next();
                if (!regionSatisfiesCondition(sequenceName, region, task)) iter.remove();
            }
            if (statistic.equalsIgnoreCase("minimum")) {
                double min=Double.MAX_VALUE;
                for (Region region:regions) {
                    Object value=region.getProperty(propertyName);
                    if (value instanceof Number && ((Number)value).doubleValue()<min) min=((Number)value).doubleValue();
                }
                synchronized(lock) {                
                    if (!regions.isEmpty()) targetdata.setValue(sequenceName, min);
                    if (!regions.isEmpty() && min<targetdata.getValue()) targetdata.setDefaultValue(min);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+regions.size());
                }
            } else if (statistic.equalsIgnoreCase("maximum")) {
                double max=-Double.MAX_VALUE;
                for (Region region:regions) {
                    Object value=region.getProperty(propertyName);
                    if (value instanceof Number && ((Number)value).doubleValue()>max) max=((Number)value).doubleValue();
                }
                synchronized(lock) {                
                    if (!regions.isEmpty()) targetdata.setValue(sequenceName, max);
                    if (!regions.isEmpty() && max>targetdata.getValue()) targetdata.setDefaultValue(max);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+regions.size());
                }
            } else if (statistic.equalsIgnoreCase("extreme")) {
                double extreme=0;
                for (Region region:regions) {
                    Object value=region.getProperty(propertyName);
                    if (value instanceof Number && Math.abs(((Number)value).doubleValue())>Math.abs(extreme)) extreme=((Number)value).doubleValue();                                       
                }
                synchronized(lock) {                
                    if (!regions.isEmpty()) targetdata.setValue(sequenceName, extreme);
                    if (!regions.isEmpty() && Math.abs(extreme)>Math.abs(targetdata.getValue())) targetdata.setDefaultValue(extreme);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+regions.size());
                }
            } else if (statistic.equalsIgnoreCase("average") || statistic.equalsIgnoreCase("sum")) {
                double sum=0;
                int regioncount=regions.size();
                for (Region region:regions) {
                    Object value=region.getProperty(propertyName);
                    if (value instanceof Number) sum+=((Number)value).doubleValue();
                }
                synchronized(lock) {                
                    double average=(regions.isEmpty())?0:(double)sum/(double)regioncount;
                    if (statistic.equalsIgnoreCase("sum")) targetdata.setValue(sequenceName, sum);
                    else if (!regions.isEmpty()) targetdata.setValue(sequenceName, average);
                    double globalsum=(Double)task.getParameter("GlobalSum");
                    task.setParameter("GlobalSum", globalsum+sum);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+regioncount);
                }
            } else if (statistic.equalsIgnoreCase("region count")) {
                int regioncount=regions.size();
                synchronized(lock) {                
                    targetdata.setValue(sequenceName, (double)regioncount);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+regioncount);
                }
            } else if (statistic.equalsIgnoreCase("region base count")) {
                int basecount=0;
                if (regions.size()>0) { // all the remaining regions satisfy the conditions
                    boolean[] flattened=new boolean[sourceSequence.getSize()]; // make a boolean array the size of the sequence to mark which bases are occupied by possibly overlapping regions
                    for (Region region:regions) { // mark the occupied positions for each region
                        int rstart=region.getRelativeStart();
                        int rend=region.getRelativeEnd();
                        if (rstart<0) rstart=0;
                        if (rstart>=flattened.length) rstart=flattened.length-1;
                        if (rend<0) rend=0;
                        if (rend>=flattened.length) rend=flattened.length-1;
                        for (int i=rstart;i<=rend;i++) flattened[i]=true;
                    }
                    for (boolean pos:flattened) { // count the number of positions marked as TRUE (=occupied by region)
                        if (pos==true) basecount++;
                    }
                }
                synchronized(lock) {                
                    targetdata.setValue(sequenceName, (double)basecount);
                    int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                    task.setParameter("GlobalBaseCount", globalbasecount+basecount);
                }
            } else throw new ExecutionError("The statistic '"+statistic+"' is not applicable to Region Datasets");
        } else if (sourceSequence instanceof DNASequenceData) {
             String usestrand=(String)task.getParameter(Operation_statistic.STRAND);
             if (usestrand==null) usestrand=Operation_statistic.STRAND_GENE; // orientation specified by the operation-settings
             int orientation=sourceSequence.getStrandOrientation(); // orientation of this sequence
             boolean reverse=(    (usestrand.equals(Operation_statistic.STRAND_REVERSE))
                               || ((usestrand.equals(Operation_statistic.STRAND_GENE) || usestrand.equals("gene strand")) && orientation==Sequence.REVERSE)
                               || (usestrand.equals(Operation_statistic.STRAND_OPPOSITE) && orientation==Sequence.DIRECT)
                               );

             int Acount=0;
             int Ccount=0;
             int Gcount=0;
             int Tcount=0;
             int Xcount=0;
             int totalcount=0;
             for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                if (!positionSatisfiesCondition(sequenceName, i, task)) continue;
                totalcount++;
                char base=((DNASequenceData)sourceSequence).getValueAtGenomicPosition(i);
                switch (base) {
                    case 'A': case 'a': if (!reverse) Acount++; else Tcount++; break;
                    case 'C': case 'c': if (!reverse) Ccount++; else Gcount++; break;
                    case 'G': case 'g': if (!reverse) Gcount++; else Ccount++; break;
                    case 'T': case 't': if (!reverse) Tcount++; else Acount++; break;
                    default: Xcount++;
                }
             }
             double resultValue=0;
             double resultSum=0;

                  if (statistic.equalsIgnoreCase("A-count")) {resultValue=Acount; resultSum=Acount;}
             else if (statistic.equalsIgnoreCase("C-count")) {resultValue=Ccount; resultSum=Ccount;}
             else if (statistic.equalsIgnoreCase("G-count")) {resultValue=Gcount; resultSum=Gcount;}
             else if (statistic.equalsIgnoreCase("T-count")) {resultValue=Tcount; resultSum=Tcount;}
             else if (statistic.equalsIgnoreCase("Unknown-count")) {resultValue=Xcount; resultSum=Xcount;}
             else if (statistic.equalsIgnoreCase("A-frequency")) {resultValue=(totalcount==0)?0:((double)Acount/(double)totalcount); resultSum=Acount;}
             else if (statistic.equalsIgnoreCase("C-frequency")) {resultValue=(totalcount==0)?0:((double)Ccount/(double)totalcount); resultSum=Ccount;}
             else if (statistic.equalsIgnoreCase("G-frequency")) {resultValue=(totalcount==0)?0:((double)Gcount/(double)totalcount); resultSum=Gcount;}
             else if (statistic.equalsIgnoreCase("T-frequency")) {resultValue=(totalcount==0)?0:((double)Tcount/(double)totalcount); resultSum=Tcount;}
             else if (statistic.equalsIgnoreCase("Unknown-frequency")) {resultValue=(totalcount==0)?0:((double)Xcount/(double)totalcount); resultSum=Xcount;}
             else if (statistic.equalsIgnoreCase("GC-content")) {resultValue=(totalcount==0)?0:((double)(Gcount+Ccount)/(double)totalcount); resultSum=(Gcount+Ccount);}
             else if (statistic.equalsIgnoreCase("base count")) {resultValue=(double)totalcount; resultSum=totalcount;}
             else throw new ExecutionError("The statistic '"+statistic+"' is not applicable to DNA Sequence Datasets");                 
             //engine.logMessage(sequenceName+":"+statistic+"   resultValue="+resultValue+"   resultSum="+resultSum);
             synchronized(lock) {                  
                 targetdata.setValue(sequenceName, resultValue);
                 double globalsum=(Double)task.getParameter("GlobalSum");
                 task.setParameter("GlobalSum", globalsum+resultSum);             
                 int globalbasecount=(Integer)task.getParameter("GlobalBaseCount");
                 task.setParameter("GlobalBaseCount", globalbasecount+totalcount);
             }
        } // end 'switch-case' on dataset-type

    }   

    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String sourceDatasetName=task.getSourceDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());          
        String targetDataName=task.getTargetDataName();
        if (targetDataName==null || targetDataName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());          
        FeatureDataset sourceDataset=(FeatureDataset)engine.getDataItem(sourceDatasetName);
        if (sourceDataset==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceDataset)) throw new ExecutionError(sourceDatasetName+"("+sourceDataset.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());

        SequenceNumericMap targetData=new SequenceNumericMap(targetDataName, 0);
        for (String key:targetData.getAllKeys(engine)) { // initialize all entries to 0 to avoid some entries defaulting
            targetData.setValue(key,0); 
        }
     
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) condition.resolve(engine, task);  
        
        Condition_within within=(Condition_within)task.getParameter("within");
        if (within!=null) within.resolve(engine, task);        
        
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SOURCE, sourceDataset);
        task.setParameter(OperationTask.TARGET, targetData);
        task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
        resolveParameters(task);
        
        int i=0;
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        if (isSubrangeApplicable() && within!=null) { // remove sequences with no selection windows (if within-condition is used)
            Iterator iter=sequences.iterator();
            while (iter.hasNext()) {
                Sequence seq = (Sequence) iter.next();
                if (!within.existsSelectionWithinSequence(seq.getName(), task)) iter.remove();
            }           
        }

        int size=sequences.size();
        String statistic=(String)task.getParameter(STATISTIC_FUNCTION);
  
             if (statistic.startsWith("minimum")) targetData.setDefaultValue(Double.MAX_VALUE);
        else if (statistic.startsWith("maximum")) targetData.setDefaultValue(-Double.MAX_VALUE);
        else targetData.setDefaultValue(0);
        task.setParameter("GlobalSum", new Double(0));
        task.setParameter("GlobalBaseCount", new Integer(0));
            
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(sourceDataset, targetData, sequence.getName(), task, counters));
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
        
        // set default value
        if (statistic.equalsIgnoreCase("average value") || statistic.equalsIgnoreCase("average score") || statistic.equalsIgnoreCase("average")) {
             double sum=(Double)task.getParameter("GlobalSum"); // this could be sum of position values or region-scores
             int count=(Integer)task.getParameter("GlobalBaseCount"); // this count could be satisfying positions or satisfying regions
             double average=(count==0)?0:(double)sum/(double)count;
             targetData.setDefaultValue(average);
        } else if (statistic.equalsIgnoreCase("sum values") || statistic.equalsIgnoreCase("sum scores") || statistic.equalsIgnoreCase("sum")) {
             double sum=(Double)task.getParameter("GlobalSum"); // this could be sum of position values or region-scores
             targetData.setDefaultValue(sum);
        } else if (statistic.equalsIgnoreCase("base count") || statistic.equalsIgnoreCase("region base count") || statistic.equalsIgnoreCase("region count")) {
             int count=(Integer)task.getParameter("GlobalBaseCount"); //
             targetData.setDefaultValue(count);
        } else if (statistic.endsWith("-count")) {
             double counts=(Double)task.getParameter("GlobalSum"); // this sum is counts for DNA sequences (for single selected base or G+C)
             targetData.setDefaultValue(counts);
        } else if (statistic.endsWith("-frequency") || statistic.endsWith("-content")) {
             double sum=(Double)task.getParameter("GlobalSum"); // this could be sum of position values or region-scores
             int count=(Integer)task.getParameter("GlobalBaseCount"); // this count could be satisfying positions or satisfying regions
             double frequency=(count==0)?0:(double)sum/(double)count;
             targetData.setDefaultValue(frequency);
        }
        if (statistic.equalsIgnoreCase("minimum") || statistic.equalsIgnoreCase("maximum") || statistic.equalsIgnoreCase("minimum score") || statistic.equalsIgnoreCase("maximum score")) {
            if ((Integer)task.getParameter("GlobalBaseCount")==0) targetData.setDefaultValue(0); // set default score value to 0 if no regions satisfied the conditions
        }

        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        Boolean internalUse=(Boolean)task.getParameter(FOR_INTERNAL_USE);
        if (internalUse!=null && internalUse.booleanValue()) {
           task.setParameter(RESULT, targetData);
        } else {
           try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        }
        return true;
    }
       
    
    
    protected boolean positionSatisfiesCondition(String sequencename, int pos, OperationTask task) throws Exception {
        Condition_position condition=(Condition_position)task.getParameter("where");
        Condition_within within=(Condition_within)task.getParameter("within");  
        if (within!=null) {
            if (!within.isConditionSatisfied(sequencename, pos, task)) return false;
        }
        if (condition==null) return true; // no where-condition is set, so the transform should be applied everywhere
        else return condition.isConditionSatisfied(sequencename, pos, task);
    }
    
   
    protected boolean regionSatisfiesCondition(String sequencename, Region region, OperationTask task) throws Exception {
        Condition_region condition=(Condition_region)task.getParameter("where");
        Condition_within within=(Condition_within)task.getParameter("within");  
        if (within!=null) {
            if (!within.isConditionSatisfied(sequencename, region.getGenomicStart(),region.getGenomicEnd(), task)) return false;
        }
        if (condition==null) return true; // no where-condition is set, so the transform should be applied everywhere
        else return condition.isConditionSatisfied(sequencename, region, task);
    }    

    
    public static boolean isKnownFunction(String statistic) {
        for (String string:getStatisticFunctions_NumericDataset()) {
            if (statistic.equalsIgnoreCase(string)) return true;
        }
        for (String string:getStatisticFunctions_RegionDataset()) {
            if (statistic.equalsIgnoreCase(string)) return true;
        }
        for (String string:getStatisticFunctions_DNADataset()) {
            if (statistic.equalsIgnoreCase(string)) return true;
        }
        return false;
    }

    public static boolean isKnownStrand(String strand) {
        return (strand!=null && (strand.equals(STRAND_DIRECT) || strand.equals(STRAND_REVERSE) || strand.equals(STRAND_GENE) || strand.equals(STRAND_OPPOSITE)));
    }

    // NOTE: Changing the names of these statistics could result in unseen consequences, so don't do it unless you read throught the code carefully and update where needed

    /** Returns the statistical functions available for Numeric datasets */
    public static String[] getStatisticFunctions_NumericDataset() {return new String[]{"minimum value","maximum value","extreme value","average value","sum values","base count"};}

    /** Returns the statistical functions available for Region datasets */
    public static String[] getStatisticFunctions_RegionDataset() {return new String[]{"minimum","maximum","extreme","average","sum","region count","region base count"};}

    public static boolean statisticTakesProperty(String statistic) {
        return  (
                  statistic.equalsIgnoreCase("minimum")
               || statistic.equalsIgnoreCase("maximum")
               || statistic.equalsIgnoreCase("extreme")
               || statistic.equalsIgnoreCase("average")
               || statistic.equalsIgnoreCase("sum")
            );
    }
    
    /** If the given string contains a statistic that takes a property name followed by a property name, the returned pair will be [0] statistic and [1] propertyName
     *  If not NULL will be returned
     */
    public static String[] getStatisticAndPropertyFromInput(String string) {
        if (string.toLowerCase().startsWith("minimum ") && string.length()>"minimum ".length()) return new String[]{"minimum", string.substring("minimum ".length()).trim()};
        if (string.toLowerCase().startsWith("maximum ") && string.length()>"maximum ".length()) return new String[]{"maximum", string.substring("maximum ".length()).trim()};
        if (string.toLowerCase().startsWith("average ") && string.length()>"average ".length()) return new String[]{"average", string.substring("average ".length()).trim()};
        if (string.toLowerCase().startsWith("extreme ") && string.length()>"extreme ".length()) return new String[]{"extreme", string.substring("extreme ".length()).trim()};
        if (string.toLowerCase().startsWith("sum ") && string.length()>"sum ".length()) return new String[]{"sum", string.substring("sum ".length()).trim()};        
        return null;
    }
    
    /** Returns the statistical functions available for DNA sequence datasets */
    public static String[] getStatisticFunctions_DNADataset() {return new String[]{"GC-content","A-count","A-frequency","C-count","C-frequency","G-count","G-frequency","T-count","T-frequency","Unknown-count","Unknown-frequency","base count"};}

    /** Returns the names of all statistical functions available for any data type */
    public static String[] getStatisticFunctions() {
        String[] numeric=getStatisticFunctions_NumericDataset();
        String[] region=getStatisticFunctions_NumericDataset();
        String[] dna=getStatisticFunctions_NumericDataset();
        String[] all=new String[numeric.length+region.length+dna.length];
        int i=0;
        for (String function:numeric) {all[i]=function;i++;}
        for (String function:region) {all[i]=function;i++;}
        for (String function:all) {all[i]=function;i++;}
        return all;
    }
    
    protected class ProcessSequenceTask implements Callable<FeatureSequenceData> {
        final SequenceNumericMap targetData;
        final FeatureDataset sourceDataset;
        final long[] counters; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences.  NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;  
        
        public ProcessSequenceTask(FeatureDataset sourceDataset, SequenceNumericMap targetdata, String sequencename, OperationTask task, long[] counters) {
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetData=targetdata; 
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
            FeatureSequenceData sourceSequence=(FeatureSequenceData)sourceDataset.getSequenceByName(sequencename);

            transformSequence(sourceSequence, targetData, task);
            
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                task.setStatusMessage("Executing "+task.getOperationName()+":  ("+counters[1]+"/"+counters[2]+")");
                task.setProgress(counters[1],counters[2]);                
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return sourceSequence;
        }   
    }      
    
}
