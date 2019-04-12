/*
 
 
 */

package motiflab.engine.operations;

import motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import motiflab.engine.data.Region;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;

/**
 *
 * @author kjetikl
 */
public class Operation_filter extends FeatureTransformOperation {
    private static final String name="filter";
    private static final String description="Removes regions that satisfy a given condition";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};

    @Override
    public String getOperationGroup() {
        return "Transform";
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
    public boolean isSubrangeApplicable() {return true;}
    
        @Override
    public void resolveParameters(OperationTask task) throws Exception {
        
    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        ArrayList<Region> list = ((RegionSequenceData)targetSequence).getAllRegions();
        String seqName=targetSequence.getName();
        for (Region region:list) {
            if (regionSatisfiesCondition(seqName, region, task)) {
                ((RegionSequenceData)targetSequence).removeRegion(region);
            }            
        } // end: for each region
    }
    
    
}
