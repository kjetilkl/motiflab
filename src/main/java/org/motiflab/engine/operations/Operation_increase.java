package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.TextMap;

/**
 *
 * @author kjetikl
 */
public class Operation_increase extends ArithmeticOperation {
    private static final String name="increase";
    private static final String description="Addition operator. Increases selected numeric values in a data object by a specified number";
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
        return oldvalue+argument;
    }

    @Override
    public Object calculateNewPropertyValue(Object oldvalue, Object argument) throws ExecutionError {
        if (argument==null || (argument instanceof String && ((String)argument).isEmpty())) {
           return oldvalue; // nothing to increment with...       
        } else if (argument instanceof Integer) {
            if (oldvalue instanceof Integer) return new Integer((Integer)oldvalue+(Integer)argument);
            if (oldvalue instanceof Double) return new Double((Double)oldvalue+(Integer)argument);
            return stringListAdd(oldvalue,argument); // treat the values as strings
            // return argument; // increasing non-numeric by numeric value. Assume prior value of 0.
        } else if (argument instanceof Double) {
            if (oldvalue instanceof Number) return new Double(((Number)oldvalue).doubleValue()+(Double)argument);
            return stringListAdd(oldvalue,argument); // treat the values as strings
            //return argument; // increasing non-numeric by numeric value. Assume prior value of 0.
        } else if (argument instanceof String || argument instanceof ArrayList) {
            //if (oldvalue instanceof Number) throw new ExecutionError("Unable to increase old numeric value by new text value");
            return stringListAdd(oldvalue,argument);
        } else if (argument instanceof Boolean) {
            if (oldvalue==null) return argument;
            boolean oldbool=(oldvalue instanceof Boolean)?((Boolean)oldvalue):false;
            return (oldbool || (Boolean)argument);
        } else throw new ExecutionError("Unknown type for value: "+argument);
    }
    
}
