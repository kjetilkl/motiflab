/*
 
 
 */

package motiflab.engine.util;

import motiflab.engine.data.Motif;

/**
 * This class contains algorithms to compare a Motif against another Motif or 
 * lists of Motifs
 * 
 * @author kjetikl
 */
public abstract class MotifComparator {

/** Compares two PWM matrices and returns a score value that reflects their similarity
 *  Note that the range of the score can be algorithm dependent
 *  The method compares the first motif in direct orientation to the second motif in either the direct or reverse orientation
 *  @param motifA The first motif
 *  @param motifB The second motif (to be compared with the first)
 *  @result an array with three values:
 *          The first value is the match score for the best alignment
 *          The second value is an integer which reflects the best orientation of motifB with respect to motifA for the best alignment (1=direct, -1=reverse)
 *          The third value (an integer) is the relative offset of motifB with respect to motifA that gave the best alignment
 */
public abstract double[] compareMotifs(Motif motifA, Motif motifB);

/** Similar to compareMotifs except that results for both directions of motifB are returned, not just the best direction.
 *  @result an array with 4 values:
 *      [0] Match score for best alignment for motifB in direct orientation
 *      [1] Relative offset of motifB with respect to motifA at best alignment for motifB in direct orientation
 *      [2] Match score for best alignment for motifB in reverse orientation
 *      [3] Relative offset of motifB with respect to motifA at best alignment for motifB in reverse orientation
 */
public abstract double[] compareMotifsBothDirections(Motif motifA, Motif motifB);

/** Returns a name for this motif comparator */
public abstract String getName();

/** Returns an abbreviation for this motif comparator */
public String getAbbreviatedName() {
    return getName();
}

/** This method returns TRUE if the comparator returns a distance value rather than a similarity value.
 *  For distance metrics the value 0 means exact match and higher values means less similarity.
 *  For similarity metrics on the other hand, higher values means more similarity
 */
public abstract boolean isDistanceMetric();

@Override
public String toString() {return getName();}

}