package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.*;

/**
 *
 * @author kjetikl
 */
public class Operation_multiply extends ArithmeticOperation {
    private static final String name="multiply";
    private static final String description="Multiplication operator. Multiplies selected numeric values in a data object by a specified number";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class, NumericVariable.class, NumericMap.class, RegionDataset.class, ExpressionProfile.class, Sequence.class, Motif.class, ModuleCRM.class, SequenceCollection.class, MotifCollection.class, ModuleCollection.class, TextMap.class};



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
        return oldvalue*argument;
    }

   @Override
    public Object calculateNewPropertyValue(Object oldvalue, Object argument) throws ExecutionError {        
        if (argument==null || (argument instanceof String && ((String)argument).isEmpty())) {
           return oldvalue;        
        } else if (argument instanceof Integer) {
            if (oldvalue instanceof Integer) return new Integer((Integer)oldvalue*(Integer)argument);
            if (oldvalue instanceof Double) return new Double((Double)oldvalue*(Integer)argument);
            return stringListAdd(oldvalue,argument); // treat both old and new value as string
            //return argument; // increasing non-numeric by numeric value. Assume prior value of 0.
        } else if (argument instanceof Double) {
            if (oldvalue instanceof Number) return new Double(((Number)oldvalue).doubleValue()*(Double)argument);
            return stringListAdd(oldvalue,argument); // treat both old and new value as string
            //return argument; // increasing non-numeric by numeric value. Assume prior value of 0.
        } else if (argument instanceof String || argument instanceof ArrayList) {
            // if (oldvalue instanceof Number) throw new ExecutionError("Unable to multiply old numeric value by new text value"); 
            return stringListAdd(oldvalue,argument);
        } else if (argument instanceof Boolean) {
            if (oldvalue==null) return argument;
            boolean oldbool=(oldvalue instanceof Boolean)?((Boolean)oldvalue):false;
            return (oldbool && (Boolean)argument); // AND
        } else throw new ExecutionError("Unknown type for value: "+argument);
    }

}
