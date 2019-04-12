package motiflab.engine.operations;

import motiflab.engine.task.ExecutableTask;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.*;
import motiflab.gui.VisualizationSettings;


/**
 *
 * @author kjetikl
 */
public class Operation_copy extends Operation {
    private static final String name="copy";
    private static final String description="Creates a copy of an existing data item";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class,DNASequenceDataset.class,NumericDataset.class, SequenceCollection.class, SequencePartition.class, SequenceTextMap.class, SequenceNumericMap.class, BackgroundModel.class, MotifCollection.class, MotifPartition.class, MotifTextMap.class, MotifNumericMap.class, Motif.class, ModuleCollection.class, ModulePartition.class, ModuleTextMap.class, ModuleNumericMap.class, Module.class, ExpressionProfile.class, NumericVariable.class, TextVariable.class, PriorsGenerator.class};
    

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
        String targetName=task.getTargetDataName();        
        if (targetName==null || targetName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        String sourceDataName=task.getSourceDataName();
        Data originalData=engine.getDataItem(sourceDataName)      ;
        if (originalData==null) throw new ExecutionError("Unknown data item: '"+sourceDataName+"'"); 
        if (!canUseAsSource(originalData)) throw new ExecutionError("Unable to create copy of data item: '"+sourceDataName+"'");
        Data newDataItem=originalData.clone();  
        newDataItem.rename(targetName);
        if (newDataItem==null) throw new ExecutionError("Unable to create new data object",task.getLineNumber());
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        if (originalData instanceof FeatureDataset && engine.getClient() instanceof motiflab.gui.MotifLabGUI && !engine.dataExists(targetName, null)) { // a small hack to copy visualization settings from source when creating a new target
                boolean hasFG=engine.getClient().getVisualizationSettings().hasSetting(VisualizationSettings.FOREGROUND_COLOR, targetName);
                boolean hasVisibility=engine.getClient().getVisualizationSettings().hasSetting(VisualizationSettings.TRACK_VISIBLE, targetName);
                engine.getClient().getVisualizationSettings().copySettings(sourceDataName, targetName, false);    
                if (!hasFG) engine.getClient().getVisualizationSettings().setForeGroundColor(targetName,null); // clear copied color in order to assign a new
                if (!hasVisibility) engine.getClient().getVisualizationSettings().setTrackVisible(targetName,true); // always show new track (unless it is already specified to be hidden)
       
        } 
        engine.storeDataItem(newDataItem);
        task.setProgress(100);
        return true;
    }        


     
}
    