/*
 
 
 */

package org.motiflab.engine.operations;

import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.external.ExternalProgram;

/**
 *
 * @author kjetikl
 */
public class Operation_execute extends Operation {
    private static final String name="execute";
    private static final String description="Executes an external program";
    public static final String ALGORITHM="algorithm";
    public static final String PARAMETERS="parameters";

    private Class[] datasourcePreferences=new Class[]{};



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
    
    public void resolveParameters(OperationTask task, ExternalProgram program) throws Exception {
        ParameterSettings parameterSettings=(ParameterSettings)task.getParameter(Operation_execute.PARAMETERS);
        if (parameterSettings==null) throw new ExecutionError("Missing parameters for external algorithm");
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
        String algorithmName=(String)task.getParameter(Operation_execute.ALGORITHM);
        ExternalProgram program=engine.getExternalProgram(algorithmName);
        if (program==null) throw new ExecutionError("Unknown algorithm "+algorithmName);
        task.setStatusMessage("Executing program: "+algorithmName);
        task.setProgress(5);
        //String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();        
        if (targetName==null || targetName.isEmpty()) throw new ExecutionError("Missing name for target data object(s)",task.getLineNumber());
        //Data sourceData=engine.getDataItem(sourceName); 
        //if (sourceData==null) throw new ExecutionError("Unable to locate data object '"+sourceName+"'",task.getLineNumber());
        //if (!canUseAsSource(sourceData)) throw new ExecutionError("Operation 'execute' can not work on '"+sourceName+"'",task.getLineNumber());            
        String[] targetNames=targetName.split("\\s*,\\s*");
        if (targetNames.length!=program.getNumberOfResultParameters()) throw new ExecutionError(targetNames.length+" target name"+((targetNames.length==1)?" is":"s are")+" specified, but external program '"+program.getName()+"' returns "+program.getNumberOfResultParameters()+" data object"+((program.getNumberOfResultParameters()==1)?"":"s")+".",task.getLineNumber());
        resolveParameters(task,program);
        Data[] targetData = runProgram(program, targetNames, task);
        task.setProgress(100);        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        for (Data target:targetData) {
            if (target instanceof RegionDataset) ((RegionDataset)target).updateMaxScoreValueFromData();
            try {engine.updateDataItem(target);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
        }
        return true;
    }
        
    private Data[] runProgram(ExternalProgram program, String[] targetName, OperationTask task) throws Exception {
        //task.setParameter(ExternalProgram.SOURCES,new Data[]{sourceData});
        program.execute(task);
        Data[] result=new Data[targetName.length];
        for (int i=0;i<targetName.length;i++) {
            Data targetData=(Data)task.getParameter(program.getNameForResultParameter(i));
            targetData.rename(targetName[i]);
            result[i]=targetData;
        }
        return result;
    }
    
    

}