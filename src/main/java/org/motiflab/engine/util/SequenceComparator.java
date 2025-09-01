/*
 
 
 */

package org.motiflab.engine.util;

import org.motiflab.engine.data.FeatureSequenceData;

/**
 * This class contains algorithms to compare a FeatureSequenceData against another FeatureSequenceData or
 *
 * @author kjetikl
 */
public abstract class SequenceComparator {

/**
 *  This method works in the same way as compareSequences(FeatureSequenceData,FeatureSequenceData)
 *  but relies on previously calculated and cached. In order to use this method, the compareAll()
 *  method have to have been called first
 *
 *  @param sequenceA The name of the first sequence
 *  @param sequenceB The name of the second sequence
 *  @result A double value reflecting either the difference or similarity between the two sequences
 */
public abstract double compareSequences(String sequenceAname, String sequenceBname);

/** Compares two sequences (FeatureSequenceData tracks) and returns a value
 *  reflecting either the similarity or difference (distance) between the two
 *  sequences.
 *  If the 'isDistanceMetric' returns TRUE (default) the value returned by
 *  compareSequences should reflect the difference between sequences,
 *  i.e. if the pair of sequences are equal the method should return 0 and higher
 *  numbers means the sequences are less similar.
 *  If, however, isDistanceMetric has been overriden to return FALSE is means that
 *  this SequenceComparator is a similarity metric and higher values means that
 *  the sequences are more similar
 *
 *  Note that the range of the scores can be algorithm dependend
 *
 *  @param sequenceA The first sequence
 *  @param sequenceB The second sequence
 *  @result A double value reflecting either the difference or similarity between the two sequences
 */
public abstract double compareSequences(FeatureSequenceData sequenceA, FeatureSequenceData sequenceB);


public boolean isDistanceMetric() {return true;}

/** Returns a name for this SequenceComparator */
public abstract String getName();

/** Compares all sequences and caches the results in a distance-matrix (or similarity-matrix)
 */
public abstract void compareAll();

@Override
public String toString() {return getName();}

}