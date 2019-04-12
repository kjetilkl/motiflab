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
import motiflab.external.EnsemblePredictionMethod;

/**
 *
 * @author kjetikl
 */
public class Operation_ensemblePrediction extends Operation {
    private static final String name="ensemblePrediction";
    private static final String description="Predicts motifs and binding sites based on the consensus of an ensemble of motif discovery algorithms";
    public static final String SOURCE_DATA="sourceData"; // reference to an array with RegionDatasets
    public static final String DNA_TRACK_NAME="dnaTrackName"; 
    public static final String DNA_TRACK="dnaTrack"; 
    public static final String ALGORITHM="algorithm";
    public static final String PARAMETERS="parameters";
    public static final String MOTIFCOLLECTION="MotifCollection";
    public static final String MOTIFPREFIX="Motifs prefix";
    public static final String ADDITIONAL_RESULTS="Additional results";    
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};

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
    
    @Override
    public boolean canHaveMultipleInput() {
        return true; 
    }       

    public void resolveParameters(OperationTask task, EnsemblePredictionMethod program) throws Exception {
        String sourceDataString=(String)task.getSourceDataName(); // this could now potentially be a comma-separated list!
        if (sourceDataString==null || sourceDataString.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());                
        String[] sourceNames=sourceDataString.split(",");
        RegionDataset[] sources=new RegionDataset[sourceNames.length];
        for (int i=0;i<sourceNames.length;i++) {
           String sourcename=sourceNames[i].trim();
           if (sourcename.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());
           Data data=engine.getDataItem(sourcename);
           if (data==null) throw new ExecutionError("Unknown data object '"+sourcename+"'",task.getLineNumber());
           if (!(data instanceof RegionDataset)) throw new ExecutionError("Data object '"+sourcename+"' is not of required type (Region Dataset)",task.getLineNumber());
           sources[i]=(RegionDataset)data;
        }
        task.setParameter(SOURCE_DATA, sources);

        ParameterSettings parameterSettings=(ParameterSettings)task.getParameter(Operation_ensemblePrediction.PARAMETERS);
        if (parameterSettings==null) throw new ExecutionError("Missing parameters for ensemble prediction algorithm");        
        Parameter[] arguments=program.getParameters();
        parameterSettings.applyConditions(arguments); // triggers actions that might change some parameter settings depending on the values of others          
        for (int i=0;i<arguments.length;i++) {
            String parameterName=arguments[i].getName();
            Object value=parameterSettings.getResolvedParameter(parameterName, program.getParameters(), engine);
            if (arguments[i].isRequired() && (value==null || value.toString().isEmpty())) throw new ExecutionError("Missing value for required parameter '"+parameterName+"'",task.getLineNumber());
            task.setParameter(parameterName,value);
        }
        String dnaTrackName=(String)task.getParameter(DNA_TRACK_NAME);
        if (dnaTrackName==null || dnaTrackName.isEmpty()) throw new ExecutionError("Missing DNA track",task.getLineNumber());
        Data dnaTrack=engine.getDataItem(dnaTrackName);
        if (dnaTrack==null) throw new ExecutionError("No such data item: '"+dnaTrack+"'",task.getLineNumber());
        if (!(dnaTrack instanceof DNASequenceDataset)) throw new ExecutionError(dnaTrack+" is not a DNA Sequence Dataset",task.getLineNumber());
        task.setParameter(DNA_TRACK, dnaTrack);
        
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
        if (sequenceCollection.isEmpty()) throw new ExecutionError("Attempting to perform ensemble motif prediction on empty Sequence Collection '"+sequenceCollection.getName()+"'",task.getLineNumber());
    }



    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String algorithmName=(String)task.getParameter(Operation_ensemblePrediction.ALGORITHM);
        Object object=engine.getExternalProgram(algorithmName);
        if (object==null) throw new ExecutionError("Unknown algorithm "+algorithmName);
        if (!(object instanceof EnsemblePredictionMethod)) throw new ExecutionError(algorithmName+" is not an ensemble prediction algorithm");
        EnsemblePredictionMethod program = (EnsemblePredictionMethod)object;
        task.setStatusMessage("Executing operation: "+task.getOperationName());
        task.setProgress(5);
        String targetName=task.getTargetDataName();
        if ((targetName==null || targetName.isEmpty()) && program.returnsSiteResults())  throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        String motifCollectionName=(String)task.getParameter(Operation_ensemblePrediction.MOTIFCOLLECTION);
        if ((motifCollectionName==null || motifCollectionName.isEmpty()) && program.returnsMotifResults()) throw new ExecutionError("Missing name for returned motif collection",task.getLineNumber());

        resolveParameters(task,program);
        RegionDataset[] sources=(RegionDataset[])task.getParameter(SOURCE_DATA);
        if (sources.length==0) throw new ExecutionError("Missing source data objects",task.getLineNumber());

        RegionDataset targetData = performEnsemblePrediction(program, sources, targetName, motifCollectionName, task);
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

    private RegionDataset performEnsemblePrediction(EnsemblePredictionMethod program, RegionDataset[] sources, String targetName, String motifCollectionName, OperationTask task) throws Exception {
        task.setParameter(ExternalProgram.SOURCES,sources);
        program.execute(task);
        String motifprefix=(String)task.getParameter(MOTIFPREFIX);
        if (motifprefix==null) motifprefix="MM";
        RegionDataset targetData=(RegionDataset)task.getParameter("Result");
        MotifCollection motifsData=(MotifCollection)task.getParameter("Motifs");
        if (motifsData==null && program.returnsMotifResults()) throw new ExecutionError("No motif collection returned");        
        if (targetData==null && program.returnsSiteResults()) throw new ExecutionError("No motif track returned");                      
        DNASequenceDataset dnaTrack=(DNASequenceDataset)task.getParameter(DNA_TRACK);
        if (motifsData!=null) {   
            motifsData.setName(motifCollectionName);            
            int numberofmotifsfound=motifsData.getPayloadSize(); // after filtering duplicates
            String[] newnames=task.getEngine().getNextAvailableDataNames(motifprefix, 5, numberofmotifsfound);
            HashMap<String,String> newnamesmap=new HashMap<String,String>(numberofmotifsfound);
            for (int i=0;i<numberofmotifsfound;i++) {
                String oldname=motifsData.renameMotifInPayload(i, newnames[i]);
                newnamesmap.put(oldname,newnames[i]);
            }          
            if (targetData!=null) annotateTFBS(targetData,dnaTrack,newnamesmap);
        } else if (targetData!=null) {
            ArrayList<String> types=new ArrayList<String>(targetData.getAllRegionTypes());
            int numberofmotifsfound=types.size();
            String[] newnames=task.getEngine().getNextAvailableDataNames(motifprefix, 5, numberofmotifsfound);
            HashMap<String,String> newnamesmap=new HashMap<String,String>(numberofmotifsfound);
            for (int i=0;i<numberofmotifsfound;i++) {
                String oldname=types.get(i);
                newnamesmap.put(oldname,newnames[i]);
            }                  
            annotateTFBS(targetData,dnaTrack,newnamesmap);             
        }
        if (targetData!=null) targetData.setName(targetName);
        return targetData;
    }

    /** Adds a 'sequence' property to the Regions and renames the region if necessary*/
    private void annotateTFBS(RegionDataset targetData, DNASequenceDataset sourceData, HashMap<String,String> newnames) {
        ArrayList<FeatureSequenceData> list=targetData.getAllSequences();
        for (FeatureSequenceData sequenceData:list) {
            RegionSequenceData regionSequenceData=(RegionSequenceData)sequenceData;
            ArrayList<Region> regions=regionSequenceData.getAllRegions();
            for (Region region:regions) {
                int start=region.getGenomicStart();
                int end=region.getGenomicEnd();
                int orientation=region.getOrientation();
                String oldtype=region.getType();
                String newtype=newnames.get(oldtype);
                if (newtype==null) newtype=oldtype; // this should not happen
                region.setType(newtype);
                if (sourceData!=null) {
                    DNASequenceData dnaSeq=(DNASequenceData)sourceData.getSequenceByName(regionSequenceData.getName());
                    char[] site=(char[])dnaSeq.getValueInGenomicInterval(start, end);
                    if (orientation==Region.DIRECT) region.setProperty("sequence", new String(site));
                    else region.setProperty("sequence", new String(MotifLabEngine.reverseSequence(site)));
                }
            }
        }
    }
  
}