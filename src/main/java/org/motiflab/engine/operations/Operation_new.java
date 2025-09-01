package org.motiflab.engine.operations;

import org.motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.*;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.datasource.DataTrack;


/**
 *
 * @author kjetikl
 */
public class Operation_new extends Operation {
    private static final String name="new";
    private static final String description="Creates a new data object of a selected type";
    private Class[] datasourcePreferences=new Class[]{};
    public static final String DATA_TYPE="datatype";
    public static final String PARAMETERS="parameters";
    public static final String DATA_TRACK_PREFIX="DataTrack:";
    public static final String FILE_PREFIX="File:";
    public static final String FILENAME="filename";
    public static final String DATA_FORMAT="dataformat";
    public static final String DATA_FORMAT_SETTINGS="dataformatSettings";
    public static final String MODEL_PREFIX="Model:";        
    public static final String COLLECTION_PREFIX="Collection:";
    public static final String FROM_TRACK_PREFIX="Track:";
    public static final String FROM_MAP_PREFIX="Map:";
    public static final String FROM_LIST_PREFIX="List:";
    public static final String FROM_PROPERTY_PREFIX="Property:";
    public static final String FROM_STATISTIC_PREFIX="Statistic:";
    public static final String COPY_PREFIX="Copy:";
    public static final String GENERATOR_PREFIX="Generator:";
    public static final String CONFIGURATION_PREFIX="Configuration:";
    public static final String RANDOM_PREFIX="Random:";
    public static final String INTERACTIONS_PREFIX="Interactions:";
    public static final String INPUT_FROM_DATAOBJECT_PREFIX="Input:";
    public static final String INPUT_DATA_NAME="inputDataName";
        
    
    public static final int UNION=0;
    public static final int INTERSECTION=1;
    public static final int SUBTRACTION=2;
    public static final int COMPLEMENT=3;
    
    
    /** 
     * Returns a list of types for data objects that can be instantiated by the new operator
     * These should preferably be in sorted order (by type String), however...
     * !!!!**** NOTE ****!!! In cases where one type is the prefix of another type (like Motif and Motif Collection)
     * the data type with the longer name must be listed before the one with the shorter name!)
     */
    public static String[] getAvailableTypes() {
        return new String[]{RegionDataset.getType(),DNASequenceDataset.getType(),NumericDataset.getType(), SequenceCollection.getType(), SequencePartition.getType(), SequenceTextMap.getType(), SequenceNumericMap.getType(), Sequence.getType(), BackgroundModel.getType(), MotifCollection.getType(), MotifPartition.getType(), MotifTextMap.getType(), MotifNumericMap.getType(), Motif.getType(), ModuleCollection.getType(), ModulePartition.getType(), ModuleTextMap.getType(), ModuleNumericMap.getType(), ModuleCRM.getType(), ExpressionProfile.getType(), GeneralNumericMap.getType(), NumericVariable.getType(), TextVariable.getType(), PriorsGenerator.getType(), OutputData.getType()};
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
    

    /**
     * Creates a new data item according to the specifications given in the OpreationTask object
     * @param task
     * @return
     * @throws Exception 
     */
    public Data createDataItem(OperationTask task) throws Exception {
        if (task.getTargetData() instanceof Data) { // If the data exists here, it has probably been created by a prompt and we can reuse it instead of creating it again         
            Data data=task.getTargetData();
            data.rename(task.getTargetDataName()); // just in case
            return data;
        }
        String targetName=task.getTargetDataName();        
        if (targetName==null || targetName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        String datatype=(String)task.getParameter(DATA_TYPE);      
        if (datatype==null || datatype.isEmpty()) throw new ExecutionError("Missing data type specification",task.getLineNumber());
        Data newDataItem=null;
        String parameter=(String)task.getParameter(PARAMETERS);
        if ((datatype.equalsIgnoreCase(NumericDataset.getType()) || datatype.equalsIgnoreCase(RegionDataset.getType()) || datatype.equalsIgnoreCase(DNASequenceDataset.getType())) && engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        if (parameter!=null && !parameter.isEmpty() && parameter.startsWith(DATA_TRACK_PREFIX)) {
            String trackName=parameter.substring(DATA_TRACK_PREFIX.length());
            newDataItem=engine.getDataLoader().loadDataTrack(trackName,task);
                 if (datatype.equalsIgnoreCase(NumericDataset.getType()) && !(newDataItem instanceof NumericDataset)) throw new ExecutionError("Incompatible assignment to Numeric Dataset for DataTrack="+trackName,task.getLineNumber());
            else if (datatype.equalsIgnoreCase(RegionDataset.getType()) && !(newDataItem instanceof RegionDataset)) throw new ExecutionError("Incompatible assignment to Region Dataset for DataTrack="+trackName,task.getLineNumber());
            else if (datatype.equalsIgnoreCase(DNASequenceDataset.getType()) && !(newDataItem instanceof DNASequenceDataset)) throw new ExecutionError("Incompatible assignment to Sequence Dataset for DataTrack="+trackName,task.getLineNumber());
            newDataItem.rename(targetName);
            DataTrack datatrack=engine.getDataLoader().getDataTrack(trackName);
            String displayDirectives=(datatrack!=null)?datatrack.getDisplayDirectivesProtocol():null;
            if (displayDirectives!=null && !displayDirectives.isEmpty()) {
                ((FeatureDataset)newDataItem).displayDirectives=displayDirectives; // the directives will be executed when the data item is added to the engine
            }
        }
        else if (parameter!=null && !parameter.isEmpty() && parameter.startsWith(FILE_PREFIX)) {
            String filename=(String)task.getParameter(FILENAME); // would it be better to just parse it here?
            if (filename==null || filename.isEmpty()) throw new ExecutionError("Missing filename for operation 'new'");
            String dataformatname=(String)task.getParameter(DATA_FORMAT);
            ParameterSettings dataformatsettings=(ParameterSettings)task.getParameter(DATA_FORMAT_SETTINGS);
            DataFormat formatter;
            if (dataformatname!=null) formatter=engine.getDataFormat(dataformatname);
            else formatter=engine.getDefaultDataFormat(engine.getDataClassForTypeName(datatype));
            if (formatter==null) throw new ExecutionError("Unknown data format: '"+dataformatname+"'");
            newDataItem=engine.getDataLoader().loadData(filename, datatype, null, formatter, dataformatsettings, task);
            newDataItem.rename(targetName);
        }
        else if (parameter!=null && !parameter.isEmpty() && parameter.startsWith(INPUT_FROM_DATAOBJECT_PREFIX)) {
            String dataobjectname=(String)task.getParameter(INPUT_DATA_NAME); // would it be better to just parse it here?
            if (dataobjectname==null || dataobjectname.isEmpty()) throw new ExecutionError("Missing name of input data object for operation 'new'");
            Data sourcedata=engine.getDataItem(dataobjectname);
            if (sourcedata==null) throw new ExecutionError("Unknown data object: "+dataobjectname);
            if (!(sourcedata instanceof TextVariable || sourcedata instanceof OutputData)) throw new ExecutionError("Input data object must be either a Text Variable or an OutputData object");
            String dataformatname=(String)task.getParameter(DATA_FORMAT);
            ParameterSettings dataformatsettings=(ParameterSettings)task.getParameter(DATA_FORMAT_SETTINGS);
            DataFormat formatter;
            if (dataformatname!=null) formatter=engine.getDataFormat(dataformatname);
            else formatter=engine.getDefaultDataFormat(engine.getDataClassForTypeName(datatype));
            if (formatter==null) throw new ExecutionError("Unknown data format: '"+dataformatname+"'");
            newDataItem=engine.getDataLoader().loadData(sourcedata, datatype, null, formatter, dataformatsettings, task);
            newDataItem.rename(targetName);
        }        
        else if (parameter!=null && !parameter.isEmpty() && parameter.startsWith(COPY_PREFIX)) {
            String originalDataName=parameter.substring(COPY_PREFIX.length());
            Data originalData=engine.getDataItem(originalDataName)      ;
            if (originalData==null) throw new ExecutionError("Unknown data item: '"+originalDataName+"'");
            String originalType=engine.getTypeNameForDataClass(originalData.getClass());
            if (!originalType.equals(datatype)) throw new ExecutionError(originalDataName+" is not of type '"+datatype+"' but of type '"+originalType+"'");
            newDataItem=originalData.clone();
            newDataItem.rename(targetName);
        }
        else if (parameter!=null && !parameter.isEmpty() && parameter.startsWith(GENERATOR_PREFIX)) {
            String constructorName=parameter.substring(GENERATOR_PREFIX.length());
            Data constructorData=engine.getDataItem(constructorName)      ;
            if (constructorData==null) throw new ExecutionError("Unknown data item: '"+constructorName+"'");
            if (!(constructorData instanceof PriorsGenerator)) throw new ExecutionError(constructorName+" is not a Priors Generator object");
            if (!datatype.equalsIgnoreCase(NumericDataset.getType())) throw new ExecutionError("Priors Generators can only be used to create Numeric Datasets");
            newDataItem=new NumericDataset(targetName);
            ArrayList<Sequence> sequences=engine.getDefaultSequenceCollection().getAllSequences(engine);
            for (Sequence seq:sequences) {
               NumericSequenceData numericSequence=new NumericSequenceData(seq,0);
               ((NumericDataset)newDataItem).addSequence(numericSequence);
            }
            newDataItem=((PriorsGenerator)constructorData).estimatePriorForDataset(((NumericDataset)newDataItem), engine, task);
        }
        else if (datatype.equalsIgnoreCase(NumericDataset.getType())) {
            //if (existingTarget!=null && !(existingTarget instanceof NumericDataset)) throw new ExecutionError("Operation 'new' cannot be applied to existing incompatible data object '"+targetName+"'");
            newDataItem=NumericDataset.createNumericDatasetFromParameter(parameter, targetName, engine, task);
            
        } else if (datatype.equalsIgnoreCase(RegionDataset.getType())) {
            //if (existingTarget!=null && !(existingTarget instanceof RegionDataset)) throw new ExecutionError("Operation 'new' cannot be applied to existing incompatible data object '"+targetName+"'");
            newDataItem=RegionDataset.createRegionDatasetFromParameter(parameter, targetName, engine, task);
        } else if (datatype.equalsIgnoreCase(DNASequenceDataset.getType())) {
            //if (existingTarget!=null && !(existingTarget instanceof DNASequenceDataset)) throw new ExecutionError("Operation 'new' cannot be applied to existing incompatible data object '"+targetName+"'");
            newDataItem=DNASequenceDataset.createDNASequenceDatasetFromParameter(parameter, targetName, engine, task);
        } else if (datatype.equalsIgnoreCase(NumericVariable.getType())) {
            double defaultvalue=0;
            if (parameter!=null && !parameter.isEmpty()) {
                try {
                    double val=Double.parseDouble(parameter);
                    defaultvalue=val;
                } catch (Exception e) {
                    Data dataobject=engine.getDataItem(parameter);
                         if (dataobject instanceof NumericVariable) defaultvalue=((NumericVariable)dataobject).getValue();
                    else if (dataobject instanceof NumericMap) defaultvalue=((NumericMap)dataobject).getValue();
                    else if (dataobject instanceof DataCollection) defaultvalue=((DataCollection)dataobject).size();
                    else throw new ExecutionError("Specified value for "+targetName+" is not numeric");
                }
            }
            newDataItem=new NumericVariable(targetName,defaultvalue);
        } else if (datatype.equalsIgnoreCase(BackgroundModel.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                newDataItem=BackgroundModel.parseBackgroundModelParameters(parameter,targetName,engine,task);
            } else newDataItem=new BackgroundModel(targetName); 
        } else if (datatype.equalsIgnoreCase(SequenceCollection.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                newDataItem=SequenceCollection.parseSequenceCollectionParameters(parameter,targetName,null,engine,task);
            } else newDataItem=new SequenceCollection(targetName);
        } else if (datatype.equalsIgnoreCase(SequencePartition.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                newDataItem=SequencePartition.parseSequencePartitionParameters(parameter,targetName,null,engine);
            } else newDataItem=new SequencePartition(targetName);
        } else if (datatype.equalsIgnoreCase(MotifPartition.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                newDataItem=MotifPartition.parseMotifPartitionParameters(parameter,targetName,null,engine);
            } else newDataItem=new MotifPartition(targetName);
        } else if (datatype.equalsIgnoreCase(Sequence.getType())) {
              newDataItem=Sequence.parseSequenceParameters(parameter,targetName,engine);
        } else if (datatype.equalsIgnoreCase(MotifCollection.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                  newDataItem=MotifCollection.parseMotifCollectionParameters(parameter,targetName,null,engine,task);
            } else newDataItem=new MotifCollection(targetName);
        } else if (datatype.equalsIgnoreCase(Motif.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                 newDataItem=Motif.parseMotifParameters(parameter,targetName,engine);
            } else throw new ExecutionError("Missing parameters for motif "+targetName);
        } else if (datatype.equalsIgnoreCase(ModuleCRM.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                 newDataItem=ModuleCRM.createModuleFromParameterString(targetName,parameter,engine);
            } else throw new ExecutionError("Missing parameters for module "+targetName);
        } else if (datatype.equalsIgnoreCase(ModuleCollection.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                  newDataItem=ModuleCollection.parseModuleCollectionParameters(parameter,targetName,null,engine,task);
            } else newDataItem=new ModuleCollection(targetName);
        } else if (datatype.equalsIgnoreCase(ModulePartition.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
                newDataItem=ModulePartition.parseModulePartitionParameters(parameter,targetName,null,engine);
            } else newDataItem=new ModulePartition(targetName);
        } else if (datatype.equalsIgnoreCase(SequenceNumericMap.getType())) {
                 newDataItem=SequenceNumericMap.createSequenceNumericMapFromParameterString(targetName,parameter,null,engine,task);
        } else if (datatype.equalsIgnoreCase(MotifNumericMap.getType())) {
                 newDataItem=MotifNumericMap.createMotifNumericMapFromParameterString(targetName,parameter,null,engine,task);
        } else if (datatype.equalsIgnoreCase(ModuleNumericMap.getType())) {
                 newDataItem=ModuleNumericMap.createModuleNumericMapFromParameterString(targetName,parameter,null,engine,task);
        } else if (datatype.equalsIgnoreCase(SequenceTextMap.getType())) {
                 newDataItem=SequenceTextMap.createSequenceTextMapFromParameterString(targetName,parameter,null,engine);
        } else if (datatype.equalsIgnoreCase(MotifTextMap.getType())) {
                 newDataItem=MotifTextMap.createMotifTextMapFromParameterString(targetName,parameter,null,engine);
        } else if (datatype.equalsIgnoreCase(ModuleTextMap.getType())) {
                 newDataItem=ModuleTextMap.createModuleTextMapFromParameterString(targetName,parameter,null,engine);
        } else if (datatype.equalsIgnoreCase(ExpressionProfile.getType())) {
                 newDataItem=ExpressionProfile.createExpressionProfileFromParameterString(targetName,parameter,engine);
        } else if (datatype.equalsIgnoreCase(PriorsGenerator.getType())) {
                 newDataItem=PriorsGenerator.createPriorsGeneratorFromParameterString(targetName,parameter,engine,task);
        } else if (datatype.equalsIgnoreCase(TextVariable.getType())) {
            if (parameter!=null && !parameter.isEmpty()) {
               newDataItem=TextVariable.createTextVariableFromParameterString(targetName,parameter,engine);
            } else newDataItem=new TextVariable(targetName);
        } else {throw new ExecutionError("Unknown data type for operation 'new': "+datatype);}
        return newDataItem;
    }
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        Data newDataItem=createDataItem(task);       
        if (newDataItem==null) throw new ExecutionError("Unable to create new data object",task.getLineNumber());
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        engine.storeDataItem(newDataItem);
        task.setProgress(100);
        return true;
    }        


    
     
}
    