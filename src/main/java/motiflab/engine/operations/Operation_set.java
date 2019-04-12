package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.*;

/**
 *
 * @author kjetikl
 */
public class Operation_set extends ArithmeticOperation {
    private static final String name="set";
    private static final String description="Assignment operator. Sets selected numeric values in a data object to the specific number";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class, NumericVariable.class, NumericMap.class, RegionDataset.class, ExpressionProfile.class, Sequence.class, Motif.class, Module.class, SequenceCollection.class, MotifCollection.class, ModuleCollection.class, TextMap.class};



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
    public boolean isSubrangeApplicable() {return true;}

    @Override
    public double calculateNewValue(double oldvalue, double argument) throws ExecutionError {
        return argument;
    }

    @Override
    public Object calculateNewPropertyValue(Object oldvalue, Object argument) throws ExecutionError {
        // if ((argument instanceof String || argument instanceof ArrayList) && oldvalue instanceof Number) throw new ExecutionError("Text values can not be used in this context");
        return argument;
    }
    
}
