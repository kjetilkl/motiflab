/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.external.ExternalProgram;
import org.motiflab.external.MotifScanning;

/**
 *
 * @author kjetikl
 */
public class Operation_motifScanning extends Operation {
    private static final String name="motifScanning";
    private static final String description="Scans DNA sequences for matches to a set of known motifs";
    public static final String ALGORITHM="algorithm";
    public static final String PARAMETERS="parameters";
    public static final String PROXY_SOURCE_MOTIFCOLLECTION="proxySourceMotifCollection";
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
    public boolean canUseAsSourceProxy(Data object) {
        return (object instanceof Motif || object instanceof MotifCollection);
    }       
    @Override
    public boolean assignToProxy(Object proxysource, OperationTask operationtask) {
        Data proxy=null;
        if (proxysource instanceof Data) proxy=(Data)proxysource;
        else if (proxysource instanceof Data[] && ((Data[])proxysource).length>0) {
            for (Data data:(Data[])proxysource) {
                if (data instanceof MotifCollection) {proxy=data;break;}
            }
        }
        if (proxy instanceof MotifCollection) {
          operationtask.setParameter(PROXY_SOURCE_MOTIFCOLLECTION,proxy.getName());
          return true;
        } else return false;
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
    
    public void resolveParameters(OperationTask task,MotifScanning program) throws Exception {
        ParameterSettings parameterSettings=(ParameterSettings)task.getParameter(Operation_motifScanning.PARAMETERS);
        if (parameterSettings==null) throw new ExecutionError("Missing parameters for motif scanning algorithm");
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
    }
    
             
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String algorithmName=(String)task.getParameter(Operation_motifScanning.ALGORITHM);
        Object object=engine.getExternalProgram(algorithmName);
        if (object==null) throw new ExecutionError("Unknown algorithm "+algorithmName);
        if (!(object instanceof MotifScanning)) throw new ExecutionError(algorithmName+" is not a motif scanning algorithm");
        MotifScanning program = (MotifScanning)object;
        task.setStatusMessage("Executing operation: "+task.getOperationName());
        task.setProgress(5);
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();        
        if (sourceName==null || sourceName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());
        if (targetName==null || targetName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        Data sourceData=engine.getDataItem(sourceName); 
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError("Operation 'motifScanning' can not work on '"+sourceName+"'",task.getLineNumber());            
        resolveParameters(task,program);
        RegionDataset targetData = performMotifScanning(program, (DNASequenceDataset)sourceData, targetName, task);
        targetData.updateMaxScoreValueFromData();
        targetData.setMotifTrack(true);
        task.setProgress(99);        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
        String additionalResults=(String)task.getParameter(Operation_motifDiscovery.ADDITIONAL_RESULTS);
        // check if additional results objects need to be handled
        if (additionalResults!=null && !additionalResults.isEmpty()) {
            String[] resultName=additionalResults.split("\\s*,\\s*");
            for (int i=0;i<resultName.length;i++) {
                String internalName=program.getNameForResultParameter(i+1); // +1 because the first has been processed already
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
        
    private RegionDataset performMotifScanning(MotifScanning program, DNASequenceDataset sourceData, String targetName, OperationTask task) throws Exception {
        task.setParameter(ExternalProgram.SOURCES,new Data[]{sourceData});
        program.execute(task);
        RegionDataset targetData=(RegionDataset)task.getParameter("Result");
        annotateTFBS(targetData, sourceData);
        targetData.setName(targetName);
        return targetData;
    }
    
    private void annotateTFBS(RegionDataset targetData, DNASequenceDataset sourceData) {
        ArrayList<FeatureSequenceData> list=targetData.getAllSequences();
        for (FeatureSequenceData sequenceData:list) {
            RegionSequenceData regionSequenceData=(RegionSequenceData)sequenceData;
            ArrayList<Region> regions=regionSequenceData.getAllRegions();
            for (Region region:regions) {
                int start=region.getGenomicStart();
                int end=region.getGenomicEnd();
                int orientation=region.getOrientation();
                DNASequenceData dnaSeq=(DNASequenceData)sourceData.getSequenceByName(regionSequenceData.getName());
                char[] site=(char[])dnaSeq.getValueInGenomicInterval(start, end);
                if (site!=null) {
                   if (orientation==Region.DIRECT) region.setProperty("sequence", new String(site));
                   else region.setProperty("sequence", new String(MotifLabEngine.reverseSequence(site))); // sequence property is the actual relative binding sequence, not the direct strand sequence
                }
            }
        }
    }
    
    
   

}