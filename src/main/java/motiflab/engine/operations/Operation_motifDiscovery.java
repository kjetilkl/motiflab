/*
 
 
 */

package motiflab.engine.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import motiflab.engine.task.OperationTask;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;
import motiflab.external.ExternalProgram;
import motiflab.external.MotifDiscovery;

/**
 *
 * @author kjetikl
 */
public class Operation_motifDiscovery extends Operation {
    private static final String name="motifDiscovery";
    private static final String description="Performs de novo motif discovery with a chosen algorithm";
    public static final String ALGORITHM="algorithm";
    public static final String PARAMETERS="parameters";
    public static final String MOTIFCOLLECTION="MotifCollection";
    public static final String MOTIFPREFIX="Motifs prefix";
    public static final String ADDITIONAL_RESULTS="Additional results";
    private Class[] datasourcePreferences=new Class[]{DNASequenceDataset.class};

    @Override
    public String getOperationGroup() {
        return "Motif";
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
    
    public void resolveParameters(OperationTask task, MotifDiscovery program) throws Exception {
        ParameterSettings parameterSettings=(ParameterSettings)task.getParameter(Operation_motifDiscovery.PARAMETERS);
        if (parameterSettings==null) throw new ExecutionError("Missing parameters for motif discovery algorithm");
        Parameter[] arguments=program.getParameters();
        parameterSettings.applyConditions(arguments); // triggers actions that might change some parameter settings depending on the values of others          
        for (int i=0;i<arguments.length;i++) {
            String parameterName=arguments[i].getName();
            Object value=parameterSettings.getResolvedParameter(parameterName, program.getParameters(), engine);
            if (arguments[i].isRequired() && (value==null || value.toString().isEmpty())) throw new ExecutionError("Missing value for required parameter '"+parameterName+"'");
            task.setParameter(parameterName,value);
        }
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
        if (sequenceCollection.isEmpty()) throw new ExecutionError("Attempting to perform motif discovery on empty Sequence Collection '"+sequenceCollection.getName()+"'",task.getLineNumber());
        else if (sequenceCollection.size()==1) task.getEngine().logMessage("WARNING: Attempting to perform motif discovery on Sequence Collection '"+sequenceCollection.getName()+"' containing only 1 sequence");
    }
    
             
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String algorithmName=(String)task.getParameter(Operation_motifDiscovery.ALGORITHM);
        Object object=engine.getExternalProgram(algorithmName);
        if (object==null) throw new ExecutionError("Unknown algorithm "+algorithmName);
        if (!(object instanceof MotifDiscovery)) throw new ExecutionError(algorithmName+" is not a motif discovery algorithm");
        MotifDiscovery program = (MotifDiscovery)object;
        task.setStatusMessage("Executing operation: "+task.getOperationName());
        task.setProgress(5);
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();        
        if ((targetName==null || targetName.isEmpty()) && program.returnsSiteResults()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        String motifCollectionName=(String)task.getParameter(Operation_motifDiscovery.MOTIFCOLLECTION);
        if ((motifCollectionName==null || motifCollectionName.isEmpty()) && program.returnsMotifResults()) throw new ExecutionError("Missing name for returned motif collection",task.getLineNumber());
        
        Data sourceData=engine.getDataItem(sourceName); 
        if (sourceName==null || sourceName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());         
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError("Operation 'motifDiscovery' can not work on '"+sourceName+"'",task.getLineNumber());            
        resolveParameters(task,program);        
        RegionDataset targetData = performMotifDiscovery(program, (DNASequenceDataset)sourceData, targetName,motifCollectionName, task);
        if (targetData!=null) {
            targetData.updateMaxScoreValueFromData();
            targetData.setMotifTrack(true);
        }
        MotifCollection motifsData=(MotifCollection)task.getParameter("Motifs");
        task.setProgress(99);        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {
            if (targetData!=null) engine.updateDataItem(targetData);
            if (motifsData!=null) engine.updateDataItem(motifsData);
        } catch (ClassCastException ce) {
            throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());
        }
        String additionalResults=(String)task.getParameter(Operation_motifDiscovery.ADDITIONAL_RESULTS);
        // check if additional results objects need to be handled
        if (additionalResults!=null && !additionalResults.isEmpty()) {
            String[] resultName=additionalResults.split("\\s*,\\s*");
            ArrayList<Parameter> additional=program.getAdditionalResultsParameters();
            if (resultName.length!=additional.size()) throw new ExecutionError("Specified number of result parameters ("+resultName.length+") does not match expected number ("+additional.size()+")");
            for (int i=0;i<resultName.length;i++) {
                String internalName=additional.get(i).getName(); 
                String externalName=resultName[i].trim();
                if (externalName.isEmpty()) throw new ExecutionError("Missing name for result data object");
                Data resultData=(Data)task.getParameter(internalName);
                resultData.rename(externalName);
                try {engine.updateDataItem(resultData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
            }
        }
        task.setProgress(100);        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();        
        return true;
    }
        
    private RegionDataset performMotifDiscovery(MotifDiscovery program, DNASequenceDataset sourceData, String targetName,String motifCollectionName, OperationTask task) throws Exception {
        task.setParameter(ExternalProgram.SOURCES,new Data[]{sourceData});
        program.execute(task);
        String motifprefix=(String)task.getParameter(MOTIFPREFIX);
        if (motifprefix==null) motifprefix="MM";
        RegionDataset targetData=(RegionDataset)task.getParameter("Result");
        MotifCollection motifsData=(MotifCollection)task.getParameter("Motifs");
        if (motifsData==null && program.returnsMotifResults()) throw new ExecutionError("No motif collection returned");        
        if (targetData==null && program.returnsSiteResults()) throw new ExecutionError("No motif track returned");              
        filterDuplicateMotifs(targetData, motifsData);
        if (motifsData!=null) {
            motifsData.setName(motifCollectionName); 
            int numberofmotifsfound=motifsData.getPayloadSize(); // after filtering duplicates
            String[] newnames=task.getEngine().getNextAvailableDataNames(motifprefix, 5, numberofmotifsfound);
            HashMap<String,String> newnamesmap=new HashMap<String,String>(numberofmotifsfound);
            for (int i=0;i<numberofmotifsfound;i++) {
                String oldname=motifsData.renameMotifInPayload(i, newnames[i]);
                newnamesmap.put(oldname,newnames[i]);
            }                  
            if (targetData!=null) annotateTFBS(targetData,sourceData,newnamesmap);
        } else if (targetData!=null) { // no motifs returned but rename the TFBS anyway
            ArrayList<String> types=new ArrayList<String>(targetData.getAllRegionTypes());
            int numberofmotifsfound=types.size();
            String[] newnames=task.getEngine().getNextAvailableDataNames(motifprefix, 5, numberofmotifsfound);
            HashMap<String,String> newnamesmap=new HashMap<String,String>(numberofmotifsfound);
            for (int i=0;i<numberofmotifsfound;i++) {
                String oldname=types.get(i);
                newnamesmap.put(oldname,newnames[i]);
            }                  
            annotateTFBS(targetData,sourceData,newnamesmap);        
        }       
        if (targetData!=null) targetData.setName(targetName);
        return targetData;
    }
    
    /** Adds a 'sequence' property to the Regions and renames the region if necessary*/
    private void annotateTFBS(RegionDataset targetData, DNASequenceDataset sourceData, HashMap<String,String> newnames) {
        ArrayList<FeatureSequenceData> list=targetData.getAllSequences();
        for (FeatureSequenceData sequenceData:list) {
            RegionSequenceData regionSequenceData=(RegionSequenceData)sequenceData;
            DNASequenceData dnaSeq=(DNASequenceData)sourceData.getSequenceByName(regionSequenceData.getName());            
            ArrayList<Region> regions=regionSequenceData.getAllRegions();
            for (Region region:regions) {
                int start=region.getGenomicStart();
                int end=region.getGenomicEnd();
                int orientation=region.getOrientation();
                String oldtype=region.getType();
                String newtype=newnames.get(oldtype);
                if (newtype==null) newtype=oldtype; // this should not happen
                region.setType(newtype);
                char[] site=(char[])dnaSeq.getValueInGenomicInterval(start, end);
                if (site!=null) {
                   if (orientation==Region.DIRECT) region.setProperty("sequence", new String(site));
                   else region.setProperty("sequence", new String(MotifLabEngine.reverseSequence(site)));
                }
            }
        }
    }


    /** Removes motifs that are duplicates of other motifs in the set
     *  This is included here because some programs that are forced to find more than one motif
     *  will sometimes return the same motif multiple times 
     */ 
    private void filterDuplicateMotifs(RegionDataset dataset, MotifCollection motifs) {
        if (motifs==null || dataset==null) return;
        int numberofmotifsfound=motifs.getPayloadSize();
        List<Motif> motiflist=motifs.getPayload();
        boolean[] issimilar=new boolean[numberofmotifsfound]; // flag which marks whether the motif at this index is similar to any previous motifs
        HashMap<String,ArrayList<int[]>> occurrences=separateMotifs(dataset);
        for (int i=1;i<numberofmotifsfound;i++) {
            for (int j=i-1;j>=0;j--) {
                String motifname1=motiflist.get(i).getName();
                String motifname2=motiflist.get(j).getName();
                ArrayList<int[]> list1=occurrences.get(motifname1);
                ArrayList<int[]> list2=occurrences.get(motifname2);
                if (motifOccurrencesEquals(list1, list2)) {issimilar[i]=true;break;}
            }
        }
        for (int i=numberofmotifsfound-1;i>=0;i--) {
            if (issimilar[i]) {
                String motifname1=motiflist.get(i).getName();
                motifs.removeMotifFromPayloadAtIndex(i);
                filterRegions(dataset,motifname1);
            }
        }
    }

    /** Creates a list of occurrences (HashMap-value) for each motif type (HashMap-key)
     *  Each motif occurrence is in the format: [sequence number, start, end]
     */
    private HashMap<String,ArrayList<int[]>> separateMotifs(RegionDataset dataset) {
        HashMap<String,ArrayList<int[]>> result=new HashMap<String,ArrayList<int[]>>();
        ArrayList<FeatureSequenceData> sequences=dataset.getAllSequences();
        for (int i=0;i<sequences.size();i++) {
            RegionSequenceData seq=(RegionSequenceData)sequences.get(i);
            ArrayList<Region> allregions=((RegionSequenceData)seq).getOriginalRegions();
            for (Region region:allregions) {
                 String motiftype=region.getType();
                 if (!result.containsKey(motiftype)) result.put(motiftype, new ArrayList<int[]>());
                 result.get(motiftype).add(new int[]{i,region.getRelativeStart(),region.getRelativeEnd()});
            }
        }
        return result;
    }

    /** Returns true if the two lists contains regions that fully overlap each other
     *  The lists should be sorted the same way
     */
    private boolean motifOccurrencesEquals(ArrayList<int[]> list1, ArrayList<int[]> list2) {
        if (list1==null || list2==null) return false;
        if (list1.size()!=list2.size()) return false;
        for (int i=0;i<list1.size();i++) {
            int[] r1=list1.get(i);
            int[] r2=list2.get(i);
            if (r1[0]!=r2[0] || r1[1]!=r2[1] || r1[2]!=r2[2]) return false;
        }
        return true;
    }

    /** Removes all regions corresponding to the given typename from the dataset */
    private void filterRegions(RegionDataset dataset, String motifname) {
        ArrayList<FeatureSequenceData> sequences=dataset.getAllSequences();
        for (int i=0;i<sequences.size();i++) {
            RegionSequenceData seq=(RegionSequenceData)sequences.get(i);
            ArrayList<Region> allregions=((RegionSequenceData)seq).getOriginalRegions();
            ArrayList<Region> tobefiltered=new ArrayList<Region>();
            for (Region region:allregions) {
                if (motifname.equals(region.getType())) tobefiltered.add(region);
            }
            for (Region region:tobefiltered) {
                seq.removeRegion(region);
            }
        }
    }
}