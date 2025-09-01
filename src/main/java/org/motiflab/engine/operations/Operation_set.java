package org.motiflab.engine.operations;

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
public class Operation_set extends ArithmeticOperation {
    private static final String name="set";
    private static final String description="Assignment operator. Sets selected numeric values in a data object to the specific number";
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
    public double calculateNewValue(double oldvalue, double argument) throws ExecutionError {
        return argument;
    }

    @Override
    public Object calculateNewPropertyValue(Object oldvalue, Object argument) throws ExecutionError {
        // if ((argument instanceof String || argument instanceof ArrayList) && oldvalue instanceof Number) throw new ExecutionError("Text values can not be used in this context");
        return argument;
    }
    
}
