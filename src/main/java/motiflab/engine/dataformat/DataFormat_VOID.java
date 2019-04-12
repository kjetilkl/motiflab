/*
 * This dataformat will always return an empty object
 */
package motiflab.engine.dataformat;

import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.Sequence;
import motiflab.engine.protocol.ParseError;



/**
 *
 * @author kjetikl
 */
public class DataFormat_VOID extends DataFormat {
 
    private String name="VOID";
    private Class[] supportedTypes=new Class[]{};
        
    public DataFormat_VOID() {
      
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return false;
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return false;
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return false;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return false;
    }
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "txt";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
        else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");

    }    
           
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {        
        if (target instanceof FeatureDataset) {
            ArrayList<Sequence> allSequences=engine.getDefaultSequenceCollection().getAllSequences(engine);
            ((FeatureDataset)target).setupDefaultDataset(allSequences);
        }
        return target;
    }
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
         return target;
    }    
    
}
