/*
 
 
 */

package org.motiflab.engine.operations;

import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabClient;
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.ModulePartition;
import org.motiflab.engine.data.ModuleTextMap;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.MotifTextMap;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.PriorsGenerator;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.data.SequenceTextMap;
import org.motiflab.engine.data.TextVariable;

/**
 *
 * @author kjetikl
 */
public class Operation_prompt extends Operation {
    private static final String name="prompt";
    private static final String description="Asks the user to provide a value for a data object";
    private Class[] datasourcePreferences=new Class[]{SequenceCollection.class, SequencePartition.class, ExpressionProfile.class, NumericVariable.class, SequenceTextMap.class, SequenceNumericMap.class, BackgroundModel.class, MotifTextMap.class, ModuleTextMap.class, MotifNumericMap.class, ModuleNumericMap.class, TextVariable.class, MotifCollection.class, MotifPartition.class, ModuleCollection.class, ModulePartition.class, Motif.class, ModuleCRM.class, PriorsGenerator.class, RegionDataset.class, NumericDataset.class, DNASequenceDataset.class};
    public static final String PROMPT_MESSAGE="PromptMessage";
    public static final String PROMPT_CONSTRAINTS="PromptConstraints";
    
    
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
    public boolean execute(OperationTask task) throws Exception {
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        String sourceName=task.getSourceDataName();            
        if (sourceName==null || sourceName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        Data sourceData=engine.getDataItem(sourceName);       
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceName+"'",task.getLineNumber());
        String message=(String)task.getParameter(PROMPT_MESSAGE);      
        PromptConstraints constraints=(PromptConstraints)task.getParameter(PROMPT_CONSTRAINTS);           
        Data newDataItem=null;        
        MotifLabClient client=engine.getClient();
        if (client==null) throw new ExecutionError("System Error: Missing client specification in PROMPT");
        else newDataItem=client.promptValue(sourceData, message, constraints);
        if (newDataItem==null) throw new ExecutionError("System Error: Got NULL value when prompting for value of '"+sourceName+"'",task.getLineNumber());
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();        
        task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
        engine.updateDataItem(newDataItem);
        return true;
    }
    
    
    
}
    