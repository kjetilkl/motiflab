/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataMap;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.protocol.StandardProtocol;
import org.motiflab.gui.VisualizationSettings;


/**
 *
 * @author kjetikl
 */
public class Operation_replace extends FeatureTransformOperation {
    public static final String SEARCH_PATTERN="searchExpression"; // 
    public static final String REPLACE_PATTERN="replaceExpression"; // 
    public static final String EXPAND_MACROS="expandMacros"; //
    public static final String INSERT_BEFORE="insertBefore"; //    
    public static final String INSERT_AFTER="insertAfter"; //    
    public static final String REGION_PROPERTY="regionProperty"; //      
    public static final String EXPRESSIONS_FROM_TEXTVARIABLE="expressionsFromTextVariable"; //        
    public static final String EXPRESSIONS_FROM_MAP="expressionsFromMap"; //       
    
    private static final String name="replace";
    private static final String description="Replaces portions of a Text Variable matching a given search pattern with a new expression";


    private Class[] datasourcePreferences=new Class[]{TextVariable.class, RegionDataset.class};

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


    }

 
    @Override
    public boolean execute(OperationTask task) throws Exception {
        String sourceDatasetName=task.getSourceDataName();
        String targetDatasetName=task.getTargetDataName();
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());          
        Data sourceData=engine.getDataItem(sourceDatasetName);
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError("The 'replace' operation can only be applied to Text Variables or Region Datasets",task.getLineNumber());
        String searchExpression=(String)task.getParameter(SEARCH_PATTERN);
        String replaceExpression=(String)task.getParameter(REPLACE_PATTERN);     
        Pattern regex=null;
        Object[][] regexMap=null;
        DataMap datamap=null;
        Object useMacrosObject=task.getParameter(EXPAND_MACROS);
        boolean useMacros=(useMacrosObject instanceof Boolean && ((Boolean)useMacrosObject).booleanValue());
        Object insertBeforeObject=task.getParameter(INSERT_BEFORE);
        boolean insertBefore=(insertBeforeObject instanceof Boolean && ((Boolean)insertBeforeObject).booleanValue());
        Object insertAfterObject=task.getParameter(INSERT_AFTER);
        boolean insertAfter=(insertAfterObject instanceof Boolean && ((Boolean)insertAfterObject).booleanValue());
        Object expressionsFromTextVariableObject=task.getParameter(EXPRESSIONS_FROM_TEXTVARIABLE);
        boolean expressionsFromTextVariable=(expressionsFromTextVariableObject instanceof Boolean && ((Boolean)expressionsFromTextVariableObject).booleanValue());  
        Object expressionsFromMapObject=task.getParameter(EXPRESSIONS_FROM_MAP);
        boolean expressionsFromMap=(expressionsFromMapObject instanceof Boolean && ((Boolean)expressionsFromMapObject).booleanValue());         

        // check that parameters are OK
        if (useMacros) { // use macros?
            if(!(sourceData instanceof TextVariable)) throw new ExecutionError("Macro expansion with 'replace' can only be applied to Text Variables");
        } else if (insertBefore || insertAfter) { // append to current lines
            if(!(sourceData instanceof TextVariable)) throw new ExecutionError("Line insertion can only be applied to Text Variables");
            if (replaceExpression==null) throw new ExecutionError("Missing text for 'replace' operation");
        } else { // search for specific (regular) expressions         
           if (searchExpression==null) throw new ExecutionError("Missing search expression for 'replace' operation");       
           if (expressionsFromMap) { 
               Data searchDataObject=engine.getDataItem(searchExpression);
               if (searchDataObject instanceof DataMap) {
                   datamap=(DataMap)searchDataObject;
                   task.setParameter("_map", datamap);  // used for the Region Dataset case    
               } else throw new ExecutionError("'"+searchExpression+"' is not a Map for 'replace' operation");
           } else { // search expression is either a TextVariable or a literal string           
               if (replaceExpression==null) throw new ExecutionError("Missing replacement string for 'replace' operation");
               if (expressionsFromTextVariable) { 
                   TextVariable searchData=(TextVariable)engine.getDataItem(searchExpression, TextVariable.class);
                   if (searchData==null) throw new ExecutionError("Search expression '"+searchExpression+"' is not a Text Variable");
                   TextVariable replaceData=(TextVariable)engine.getDataItem(replaceExpression, TextVariable.class);
                   if (replaceData==null) throw new ExecutionError("Replacement expression '"+replaceExpression+"' is not a Text Variable");  
                   if (searchExpression.equals(replaceExpression)) { // same object. Treat as 2-column Map
                       regexMap=new Object[searchData.size()][2]; 
                       for (int i=0;i<searchData.size();i++) {
                           String line=searchData.getString(i);
                           String[] parts=null;
                           if (line.contains("\t")) parts=line.split("\t", 2);
                           else if (line.contains("=>")) parts=line.split("=>", 2);
                           else throw new ExecutionError("When using a single Text Variable as search/replace map, each line must contain two columns separated by either TAB or =>");
                           regexMap[i][0]=Pattern.compile(parts[0]);
                           regexMap[i][1]=parts[1];
                       }
                   } else { // two different objects. They should have the same number of lines
                       if (searchData.size()!=replaceData.size()) throw new ExecutionError("The search expression variable and replacement expression variable must have the same number of lines! ("+searchData.size()+" vs "+replaceData.size()+")");
                       regexMap=new Object[searchData.size()][2];
                       for (int i=0;i<searchData.size();i++) {
                           String search=searchData.getString(i);
                           String replace=replaceData.getString(i);
                           regexMap[i][0]=Pattern.compile(search);
                           regexMap[i][1]=replace;
                       }
                   }
                   task.setParameter("_regexMap", regexMap); // used for the Region Dataset case                 
               } else { // single literal search expression
                   regex=Pattern.compile(searchExpression);
                   task.setParameter("_regex", regex); // used for the Region Dataset case              
               }
            }
        }

        if (sourceData instanceof TextVariable) {
            task.setProgress(5);
            task.setStatusMessage("Executing operation: "+task.getOperationName());
            ArrayList<String> result=new ArrayList<String>();
            if (useMacros) {
                result.addAll(expandMacros(((TextVariable)sourceData).getAllStrings(), task));
            } else if (insertBefore || insertAfter) {
                if (insertAfter) result.addAll(((TextVariable)sourceData).getAllStrings());
                String[] lines=replaceExpression.split("\\n"); // this looks for "\n" in the text (split on newlines)  old: replaceExpression.split("\\\\n");
                result.addAll(Arrays.asList(lines));
                if (insertBefore) result.addAll(((TextVariable)sourceData).getAllStrings());
            } else { // regular regex replacement. This can either be a single expression or based on a map              
                for (String string:((TextVariable)sourceData).getAllStrings()) {
                    if (regex!=null) { // single regex
                        Matcher matcher=regex.matcher(string);
                        String transformed=matcher.replaceAll(replaceExpression);
                        result.add(transformed);
                    } else if (regexMap!=null) { // multiple regexes
                        for (Object[] pair:regexMap) {
                           Pattern regEx=(Pattern)pair[0];
                           String replace=(String)pair[1];
                           Matcher matcher=regEx.matcher(string);
                           string=matcher.replaceAll(replace);                          
                        }
                        result.add(string);                          
                    } else if (datamap!=null) { // search expressions based on map. Apply each one in turn (these expressions are not regexes)
                        for (String key:datamap.getAllKeys(engine)) {
                            String value=datamap.getValue(key).toString();
                            string.replace(key, value);
                        }
                    }
                }
            }
            TextVariable targetData=new TextVariable(targetDatasetName, result);            
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            task.setProgress(100);
            try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        } else if (sourceData instanceof RegionDataset) {
            RegionDataset sourceDataset=(RegionDataset)sourceData;
            RegionDataset targetDataset=(RegionDataset)sourceDataset.clone(); // Double-buffer.
            targetDataset.setName(targetDatasetName);

            if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");

            Condition condition=(Condition)task.getParameter("where");
            if (condition!=null) condition.resolve(engine, task);

            Condition_within within=(Condition_within)task.getParameter("within");
            if (within!=null) within.resolve(engine, task);

            task.setParameter(OperationTask.SOURCE, sourceDataset);
            task.setParameter(OperationTask.TARGET, targetDataset);

            String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
            if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
            Data seqcol=engine.getDataItem(subsetName);
            if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
            if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
            SequenceCollection sequenceCollection=(SequenceCollection)seqcol;

            ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
            if (isSubrangeApplicable() && within!=null) { // remove sequences with no selection windows (if within-condition is used)
                Iterator iter=sequences.iterator();
                while (iter.hasNext()) {
                    Sequence seq = (Sequence) iter.next();
                    if (!within.existsSelectionWithinSequence(seq.getName(), task)) iter.remove();
                }
            }
            
            TaskRunner taskRunner=engine.getTaskRunner();
            task.setProgress(0L,sequences.size());
            long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

            ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
            for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(sourceDataset,targetDataset, sequence.getName(), task, counters));
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
            
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            targetDataset.setIsDerived(true);
            if (engine.getClient() instanceof org.motiflab.gui.MotifLabGUI && !targetDatasetName.equals(sourceDatasetName) && !engine.dataExists(targetDatasetName, null)) { // a small hack to copy visualization settings from source when creating a new target
                boolean hasFG=engine.getClient().getVisualizationSettings().hasSetting(VisualizationSettings.FOREGROUND_COLOR, targetDatasetName);
                boolean hasVisibility=engine.getClient().getVisualizationSettings().hasSetting(VisualizationSettings.TRACK_VISIBLE, targetDatasetName);
                engine.getClient().getVisualizationSettings().copySettings(sourceDatasetName, targetDatasetName, false);    
                if (!hasFG) engine.getClient().getVisualizationSettings().setForeGroundColor(targetDatasetName,null); // clear copied color in order to assign a new
                if (!hasVisibility) engine.getClient().getVisualizationSettings().setTrackVisible(targetDatasetName,true); // always show new track (unless it is already specified to be hidden)
            }          
            try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        }
        return true;
    }

    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        if (!(sourceSequence instanceof RegionSequenceData)) throw new ExecutionError("Source sequence not from a Region Dataset track");
        Pattern regex=(Pattern)task.getParameter("_regex");
        Object[][] regexMap=(Object[][])task.getParameter("_regexMap");        
        DataMap datamap=(DataMap)task.getParameter("_map");  
        String replacement=(String)task.getParameter(REPLACE_PATTERN);
        String property=(String)task.getParameter(REGION_PROPERTY);
        if (property==null || property.isEmpty()) property="type";
        String seqname=sourceSequence.getName();        
        
        ArrayList<Region> list = ((RegionSequenceData)targetSequence).getAllRegions();
        if (regex!=null) {  // single regex      
            for (Region region:list) {
                if (regionSatisfiesCondition(seqname, region, task)) {
                     Object propertyValue=region.getProperty(property);
                     if (propertyValue==null) continue;
                     String value=propertyValue.toString();
                     Matcher matcher=regex.matcher(value);
                     String transformed=matcher.replaceAll(replacement);
                     region.setProperty(property,transformed);                                          
                 }
            } // end: for each region
        } else if (regexMap!=null) { // map of regexes
            for (Region region:list) {
                if (regionSatisfiesCondition(seqname, region, task)) {
                     Object propertyValue=region.getProperty(property);
                     if (propertyValue==null) continue;
                     String value=propertyValue.toString();
                     String transformed=value;
                     for (Object[] pair:regexMap) {
                         Pattern regEx=(Pattern)pair[0];
                         String replace=(String)pair[1];
                         Matcher matcher=regEx.matcher(value);
                         transformed=matcher.replaceAll(replace);
                         if (!transformed.equals(value)) break; // only use first match
                     }
                     region.setProperty(property,transformed);                                          
                 }
            } // end: for each region            
        } else if (datamap!=null) { // regular map
            for (Region region:list) {
                if (regionSatisfiesCondition(seqname, region, task)) {
                     Object propertyValue=region.getProperty(property);
                     if (propertyValue==null) continue;
                     String value=propertyValue.toString();
                     String transformed=datamap.getValue(value).toString(); // transform value using map
                     region.setProperty(property,transformed);                                          
                 }
            } // end: for each region             
        }
    }   
    
    private ArrayList<String> expandMacros(ArrayList<String> input, OperationTask task) throws ExecutionError {
        ExecutableTask topTask=task;
        while (topTask.getParentTask()!=null) topTask=task.getParentTask(); // propagate to the top
        Object protocol=topTask.getParameter("_protocol");
        ArrayList<String[]> macros=null;
        if (protocol instanceof StandardProtocol) macros=(ArrayList)((StandardProtocol)protocol).getMacros();
        else macros=engine.getMacros();
        try {
            return StandardProtocol.expandAllMacros(input, macros);
        } catch (ParseError p) {
            throw new ExecutionError(p.getMessage(),p);
        }
    }
    
}



