package motiflab.engine.operations;

import motiflab.engine.task.OperationTask;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.*;
import motiflab.engine.data.analysis.Analysis;


/**
 *
 * @author kjetikl
 */
public class Operation_delete extends Operation {
    private static final String name="delete";
    private static final String description="Deletes data objects";
    private Class[] datasourcePreferences=new Class[]{Analysis.class, RegionDataset.class, DNASequenceDataset.class, NumericDataset.class, MotifCollection.class, ModuleCollection.class, SequenceCollection.class, SequencePartition.class, MotifPartition.class, ModulePartition.class, BackgroundModel.class, NumericVariable.class, MotifTextMap.class, ModuleTextMap.class, SequenceTextMap.class, MotifNumericMap.class, ModuleNumericMap.class, SequenceNumericMap.class, TextVariable.class, PriorsGenerator.class, ExpressionProfile.class, OutputData.class};


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

    @Override
    public boolean execute(OperationTask task) throws Exception {
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        String sourceName=task.getSourceDataName();
        if (sourceName==null || sourceName.isEmpty()) throw new ExecutionError("Missing name for data object",task.getLineNumber());
        String[] sourceNames=sourceName.trim().split("\\s*,\\s*");
        for (String source:sourceNames) {
            Data data=engine.getDataItem(source);
            if (data==null) throw new ExecutionError("Unknown data item: "+source,task.getLineNumber());
            if (data==engine.getDefaultSequenceCollection()) throw new ExecutionError("'"+engine.getDefaultSequenceCollectionName()+"' can not be deleted with the 'delete' operation",task.getLineNumber());
            if (data instanceof Sequence) throw new ExecutionError("Sequences can not be deleted with the 'delete' operation (use 'drop_sequences' instead)",task.getLineNumber());
            if (data instanceof Motif) throw new ExecutionError("Motifs can not be deleted with the 'delete' operation",task.getLineNumber());
            if (data instanceof Module) throw new ExecutionError("Modules can not be deleted with the 'delete' operation",task.getLineNumber());
        }
        for (String source:sourceNames) {
            engine.removeDataItem(source);
        }
        task.setProgress(100);
        return true;
    }

}
