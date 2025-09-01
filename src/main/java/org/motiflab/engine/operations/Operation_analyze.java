/*
 
 
 */

package org.motiflab.engine.operations;

import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.analysis.Analysis;

/**
 *
 * @author kjetikl
 */
public class Operation_analyze extends Operation {
    private static final String name="analyze";
    private static final String description="Runs a chosen analysis";
    public static final String ANALYSIS="analysis";
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
    
    public void resolveParameters(OperationTask task, Analysis analysis) throws Exception {
        ParameterSettings parameterSettings=(ParameterSettings)task.getParameter(PARAMETERS);
        if (parameterSettings==null) throw new ExecutionError("Missing parameters for analysis");
        Parameter[] arguments=analysis.getParameters();
        parameterSettings.applyConditions(arguments); // triggers actions that might change some parameter settings depending on the values of others          
        for (int i=0;i<arguments.length;i++) {
            String parameterName=arguments[i].getName();
            Object value=parameterSettings.getResolvedParameter(parameterName, analysis.getParameters(), engine);
            if (arguments[i].isRequired() && (value==null || value.toString().isEmpty())) throw new ExecutionError("Missing value for required parameter '"+parameterName+"'");
            task.setParameter(parameterName,value);
        }
    }
    
             
    @Override
    public boolean execute(OperationTask task) throws Exception {
        //if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String analysisName=(String)task.getParameter(Operation_analyze.ANALYSIS);
        Analysis analysis=engine.getNewAnalysis(analysisName);
        if (analysis==null) throw new ExecutionError("Unknown analysis '"+analysisName+"'");
        task.setStatusMessage("Executing analysis: "+analysisName);
        task.setProgress(5);
        String targetName=task.getTargetDataName();        
        if (targetName==null || targetName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        analysis.rename(targetName);
        resolveParameters(task,analysis);
        analysis.runAnalysis(task);
        task.setProgress(100);        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {engine.updateDataItem(analysis);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}      
        return true;
    }
        
    
    

}