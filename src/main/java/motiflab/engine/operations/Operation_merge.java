/*
 
 
 */

package motiflab.engine.operations;

import motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import java.util.Iterator;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.Data;
import motiflab.engine.data.Region;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.NumericConstant;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;

/**
 *
 * @author kjetikl
 */
public class Operation_merge extends FeatureTransformOperation {
    public static final String DISTANCE_STRING="distanceString";
    public static final String SIMILAR="similar";
    private static final String DISTANCE_DATA="distanceData";    
    private static final String name="merge";
    private static final String description="Merges regions that overlap or lie close to each other";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};
    private String[] reserved=new String[]{"similar","closer","any"};
    
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
    public String[] getReservedWords() {return reserved;}  
    
    @Override
    public String toString() {return name;}
    
    @Override
    public boolean isSubrangeApplicable() {return true;}
    
    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        String distanceString=(String)task.getParameter(DISTANCE_STRING);
            if (distanceString!=null && !distanceString.isEmpty()) {
            Data distanceData=null;
            distanceData=engine.getDataItem(distanceString);
            if (distanceData==null) {
                try {
                  double value=Double.parseDouble(distanceString);
                  distanceData=new NumericConstant(distanceString, (double)value);
               } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+distanceString+"' neither data nor numeric constant",task.getLineNumber());}         
            }
            if (!(distanceData instanceof NumericConstant || distanceData instanceof NumericVariable || distanceData instanceof SequenceNumericMap)) throw new ExecutionError("'"+distanceString+"' is not a valid numeric value",task.getLineNumber());          
            task.setParameter(DISTANCE_DATA, distanceData);
        }
    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String seqName=targetSequence.getName();
        double distance=0;
        Data distanceData=(Data)task.getParameter(DISTANCE_DATA); 
        if (distanceData instanceof NumericConstant) {
           distance=((NumericConstant)distanceData).getValue();
        } else if (distanceData instanceof SequenceNumericMap) {
           distance=((SequenceNumericMap)distanceData).getValue(seqName);
        } else if (distanceData instanceof NumericVariable) {
           distance=((NumericVariable)distanceData).getValue();
        }
        ArrayList<Region> list = ((RegionSequenceData)sourceSequence).getAllRegions();
        ArrayList<Region> satisfies = new ArrayList<Region>();
        Iterator iter=list.iterator();
        while (iter.hasNext()) {
            Region region=(Region)iter.next();
            if (regionSatisfiesCondition(seqName, region, task)) {
                iter.remove();
                satisfies.add(region);
            }            
        }
        // now the "satisfies"-list will contain all regions that satisfies all where/while clauses
        // and should maybe be merged. The "list"-list contains the remaining regions that should be left as is
        ((RegionSequenceData)targetSequence).clearRegions();
        ArrayList<Region> result=new ArrayList<Region>();
        if (satisfies.size()>1) {
            Region first=satisfies.get(0);
            Region merged=new Region(((RegionSequenceData)targetSequence), first.getRelativeStart(), first.getRelativeEnd(), first.getType(), first.getScore(), first.getOrientation());
            for (int i=1;i<satisfies.size();i++) {
                Region next=satisfies.get(i);
                if (next.getRelativeStart()-(merged.getRelativeEnd()+1)<distance) { // merge                    
                    //merged.setScore(merged.getScore()+next.getScore()); set score to sum of scores over all merged regions
                    if (merged.getScore()>next.getScore()) merged.setScore(next.getScore()); // set score to highest score
                    if (merged.getOrientation()!=next.getOrientation()) merged.setOrientation(Region.INDETERMINED);
                    if (next.getRelativeEnd()>merged.getRelativeEnd()) merged.setRelativeEnd(next.getRelativeEnd());
                    if (!merged.getType().equals(next.getType())) merged.setType("merged");
                } else { // do not merge: finalize the current "merge" object and start a new one
                   result.add(merged);
                   merged=new Region(((RegionSequenceData)targetSequence), next.getRelativeStart(), next.getRelativeEnd(), next.getType(), next.getScore(), next.getOrientation());
                }
            }
            result.add(merged);
        } else if (satisfies.size()==1) {
            result.add(satisfies.get(0)); // only one region in the sequence, so it can not be merged
        }
        for (Region region:result) { // add merged regions
            ((RegionSequenceData)targetSequence).addRegion(region);
        }
        for (Region region:list) { // add back the rest
            ((RegionSequenceData)targetSequence).addRegion(region);
        }
        ((RegionDataset)targetSequence.getParent()).setMotifTrack(false);
        ((RegionDataset)targetSequence.getParent()).setModuleTrack(false);    
        ((RegionDataset)targetSequence.getParent()).setNestedTrack(false);          
    }
    
  
}
