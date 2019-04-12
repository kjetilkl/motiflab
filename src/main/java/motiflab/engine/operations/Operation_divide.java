package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.*;

/**
 *
 * @author kjetikl
 */
public class Operation_divide extends ArithmeticOperation {
    private static final String name="divide";
    private static final String description="Division operator. Divides selected numeric values in a data object by a specified number";
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
    public double calculateNewValue(double oldvalue, double argument) throws ExecutionError{
        if (argument==0) throw new ExecutionError("Division by zero");
        return oldvalue/argument;
    }

   @Override
    public Object calculateNewPropertyValue(Object oldvalue, Object argument) throws ExecutionError {
        if (argument==null || (argument instanceof String && ((String)argument).isEmpty())) {
           return oldvalue;        
        } else if (argument instanceof Number) {
            if (((Number)argument).doubleValue()==0) throw new ExecutionError("Division by zero");
            if (oldvalue instanceof Number) return new Double(((Number)oldvalue).doubleValue()/((Number)argument).doubleValue());
            else return stringListRemove(oldvalue,argument); // return new Double(0);
        } else if (argument instanceof String || argument instanceof ArrayList) {
            // if (oldvalue instanceof Number) throw new ExecutionError("Unable to divide old numeric value by new text value");            
            return stringListRemove(oldvalue,argument);
        } else if (argument instanceof Boolean) {
            if (oldvalue==null) return argument;
            boolean oldbool=(oldvalue instanceof Boolean)?((Boolean)oldvalue):false;
            return !(oldbool && (Boolean)argument); // NAND
        } else throw new ExecutionError("Unknown type for value: "+argument);
    }

}
