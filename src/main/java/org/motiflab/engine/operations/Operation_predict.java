/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.PriorsGenerator;

/**
 *
 * @author kjetikl
 */
public class Operation_predict extends Operation {
    private static final String name="predict";
    private static final String description="Creates a new positional priors track to predict a specific feature using a Priors Generator object";
    private Class[] datasourcePreferences=new Class[]{PriorsGenerator.class};


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
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String priorsGeneratorName=task.getSourceDataName();
        if (priorsGeneratorName==null || priorsGeneratorName.isEmpty()) throw new ExecutionError("No Priors Generator specified",task.getLineNumber());
        Data constructorData=engine.getDataItem(priorsGeneratorName);
        if (constructorData==null) throw new ExecutionError("No such data object: ",task.getLineNumber());
        if (!(constructorData instanceof PriorsGenerator)) throw new ExecutionError("'"+priorsGeneratorName+"' is not a Priors Generator object",task.getLineNumber());
        PriorsGenerator priorsGenerator=(PriorsGenerator)constructorData;
        String targetDatasetName=task.getTargetDataName();
        if (targetDatasetName==null || targetDatasetName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        NumericDataset targetDataset=new NumericDataset(targetDatasetName);
        targetDataset.setName(targetDatasetName);
        SequenceCollection sequenceCollection=engine.getDefaultSequenceCollection();
        int size=sequenceCollection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        for (Sequence sequence:sequences) { // for each sequence
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            NumericSequenceData numericSequence=new NumericSequenceData(sequence,0);
            priorsGenerator.estimatePriorForDataset(numericSequence, engine, task);
            targetDataset.addSequence(numericSequence);
            task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+size+")");
            task.setProgress(i+1, size);
            i++;
            Thread.yield();
        }
        if (targetDataset instanceof NumericDataset) ((NumericDataset)targetDataset).updateAllowedMinMaxValuesFromData();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.setIsDerived(true);
        try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }


    

}
