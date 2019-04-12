/*
 
 
 */

package motiflab.engine.util;

import motiflab.engine.data.FeatureSequenceData;

/**
 *
 * @author kjetikl
 */
public class SequenceComparator_Park extends SequenceComparator {

    @Override
    public void compareAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double compareSequences(String sequenceAname, String sequenceBname) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double compareSequences(FeatureSequenceData sequenceA, FeatureSequenceData sequenceB) {
        String nameA=sequenceA.getSequenceName();
        String nameB=sequenceB.getSequenceName();
        return nameA.compareTo(nameB);
    }

    @Override
    public String getName() {
        return "Park";
    }

}
