/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import motiflab.engine.ExecutionError;
import motiflab.engine.SystemError;

/**
 * This class holds feature data for a single sequence from a specified
 * region in a genome. The type of data contained in these objects are "region data"
 * which are discrete regions with associated values, for instance the location
 * of transcription factor binding sites or SINE repeats within the region.
 * The data itself is stored as a set of such regions that all fall within the 
 * span of the genomic sequence itself. The individual regions and their 
 * properties are implemented as objects of type Region 
 * 
 * @author kjetikl
 */
public class RegionSequenceData extends FeatureSequenceData implements Cloneable {
    private static String typedescription="Region data";
    private static Comparator defaultRegionSortOrderComparator=null;
    
    private ArrayList<Region> regions;  // this list should always be kept sorted according to start position of regions!
                                        // if two elements have the same start position, the shortest is placed first in the list
   
    private int sequencesize=0; // keeps track of the length of this sequence. 
                                // Unlike for DNA and Numeric Data, this type of dataset has
                                // no array that stores data for each base in a 1-to-1 relationship
        
    private double maxScore=0; // the maximum region score in this sequence. Used mainly by track visualizers
    
    public short totalRows=0; // DEPRECATED: this property was previously used by track visualizers that display Regions in multiple rows. 
        
    /**
     * Constructs a new RegionSequenceData object from the supplied data
     * 
     * @param sequenceName
     * @param geneName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param TSS
     * @param TES
     * @param orientation
     */
    public RegionSequenceData(String sequenceName, String geneName, String chromosome, int startPosition, int endPosition, Integer TSS, Integer TES, int orientation){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         // the geneName,TSS,TES and orientation attributes are no longer stored in FeatureSequenceData objects but only derived from Sequence objects. They are kept in the constructor for backwards compatibility
         this.regions=new ArrayList<Region>();
         this.sequencesize=endPosition-startPosition+1;
    }
    
    /**
     * Constructs a new RegionSequenceData object from the supplied data
     * 
     * @param sequenceName
     * @param geneName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param TSS
     * @param TES
     * @param orientation
     */
    public RegionSequenceData(String sequenceName, String chromosome, int startPosition, int endPosition){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         this.regions=new ArrayList<Region>();
         this.sequencesize=endPosition-startPosition+1;
    }    
    
    /**
     * Creates a new initially "empty" RegionSequenceData object based on the given Sequence template
     * @param sequence
     */
    public RegionSequenceData(Sequence sequence) {
         setSequenceName(sequence.getName());
         setChromosome(sequence.getChromosome());
         setRegionStart(sequence.getRegionStart());
         setRegionEnd(sequence.getRegionEnd());
         this.regions=new ArrayList<Region>();
         this.sequencesize=endPosition-startPosition+1;      
    }

    @Override
    public Object getValue() {return regions;}  
    
     @Override
     /**
      * Returns a new ArrayList containing regions that overlap with the given
      * genomic interval. The returned list is new but it contains references
      * to the original regions (not clones) 
      */
     public Object getValueInGenomicInterval(int start, int end) {
        if (end<getRegionStart() || start>getRegionEnd() || end<start) return null;
        if (start<getRegionStart()) start=getRegionStart();
        if (end>getRegionEnd()) end=getRegionEnd();
        ArrayList<Region> list=getRegionsOverlappingGenomicInterval(start, end);
        return list;
//        ArrayList<Region> newList=new ArrayList<Region>(); 
//        for (Region region:list) {
//            Region newregion=region.clone();
//            newregion.setParent(null);
//            newList.add(newregion);
//        }
//        return newList; // return deep clone of segment
    }    
    
     /**
      * Returns a new ArrayList containing regions that overlap with the given
      * genomic interval.
      */
     public Object getRegionsOverlappingGenomicIntervalAsClonesWithoutParents(int start, int end) {
        if (end<getRegionStart() || start>getRegionEnd() || end<start) return null;
        if (start<getRegionStart()) start=getRegionStart();
        if (end>getRegionEnd()) end=getRegionEnd();
        ArrayList<Region> list=getRegionsOverlappingGenomicInterval(start, end);
        ArrayList<Region> newList=new ArrayList<Region>(); 
        for (Region region:list) {
            Region newregion=region.clone();
            newregion.setParent(null);
            newList.add(newregion);
        }
        return newList; // return deep clone of segment
    }      
     
    /**
     * Adds a new Region object to this sequence. 
     * The new region is added to the list in sorted order
     * 
     * @param region The region to be added to the list
     */       
    public void addRegion(Region region) {
        region.setParent(this);
        int size=regions.size();
        double score=region.getScore();
        if (score>maxScore) setMaxScoreValue(score);
        if (size==0) {regions.add(region);}
        else if (!region.isLocatedPriorTo(regions.get(size-1))) {regions.add(region);} // add to the end
        else {
          int index=0;      
          while (index<size) {
            Region nextregion=regions.get(index);
            if (region.isLocatedPriorTo(nextregion)) {regions.add(index, region);index=size;} // insert at first position where this region is located prior to the subsequent in the list
            index++;
          }
        }            
    }
    
     /**
     * Adds a new Region object to this sequence, but only
     * if the sequence does not already contain an identical region
     * The new region is added to the list in sorted order
     * 
     * @param region The region to be added to the list
     */     
    public void addRegionIfnotAlreadyPresent(Region region) {
        region.setParent(this);
        int size=regions.size();
        double score=region.getScore();
        if (score>maxScore) setMaxScoreValue(score);
        if (size==0) {regions.add(region);}
        else if (!region.isLocatedPriorTo(regions.get(size-1))) { // the new region is either located after the current last region (or at the exact same location)
            if (!region.isIdenticalTo(regions.get(size-1))) regions.add(region); // add to the end of the list (unless identical to existing last region)
        }
        else { 
          int index=0;      
          while (index<size) {
            Region nextregion=regions.get(index);
            if (region.isIdenticalTo(nextregion)) return;
            if (region.isLocatedPriorTo(nextregion)) {regions.add(index, region);index=size;} // insert at first position where this region is located prior to the subsequent in the list
            index++;
          }
        }            
    }    
    
    /**
     * Adds a new Region object to this sequence. 
     * This method may be used if many regions should be added at the same time.
     * The regions are not added in sorted order and the region list must be
     * sorted manually after all regions have been added by calling the method
     * updateRegionSortOrder()
     * 
     * @param region The region to be added to the list
     */       
    public void addRegionWithoutSorting(Region region) {
        //if (region.getParent()==null)
        region.setParent(this);
        double score=region.getScore();
        if (score>maxScore) setMaxScoreValue(score);
        regions.add(region);
    }    

    
    public Comparator getRegionSortOrderComparator() {
        if (defaultRegionSortOrderComparator==null) defaultRegionSortOrderComparator=new RegionSortOrderComparator(true);
        return defaultRegionSortOrderComparator;
    }
    
    /** 
     *  This method should be called if any regions have updated the start or end positions
     *  after they were added to a sequence. The regions will then be sorted in their proper order again
     */
    public void updateRegionSortOrder() {
        Collections.sort(regions, getRegionSortOrderComparator());
    }
    
    /**
     * Removes a region from this sequence object
     *
     * @param region The region to be removed
     */
    public void removeRegion(Region region) {
        regions.remove(region);
    }

    /**
     * Removes a region from this sequence object that is similar to the
     * argument region (the argument could for instance be a clone of the region
     * which should be removed)
     * @param region and "identical copy" of the region to be removed
     * @return TRUE if the region was found and removed else FALSE
     */
    public boolean removeSimilarRegion(Region region) {
        Region target=null;
        for (Region r:regions) {
            if (r.isIdenticalTo(region)) {target=r;break;}
        }
        if (target!=null) {
            regions.remove(target);
            return true;
        } else return false;
    }
   
    /**
     * Returns the number of regions associated with this sequence
     * 
     * @return The number of regions
     */       
    public int getNumberOfRegions() {
        return regions.size();
    }
   
    /**
     * Returns the region corresponding to the specified index
     * This function can be used (in combination with getNumberOfRegions)
     * to iterated through the set of Regions
     * 
     * @param i The index of the Region to be retrieved
     */       
    public Region getRegionByIndex(int i) {
        if (i<0 || i>=regions.size()) return null;
        return (Region)regions.get(i);
    }
    
    

    private transient int lastSearchIndex=0;
    
    /** Returns the region that comes after the given region in the track */
    public Region getNextRegion(Region region) { // optimize !!
        int currentRegionIndex=getIndexForRegion(region);
        if (currentRegionIndex>=0 && currentRegionIndex+1<regions.size()) {
            lastSearchIndex=currentRegionIndex+1;
            return regions.get(currentRegionIndex+1);        
        }
        return null;
    } 
    
    /** Returns the region that comes before the given region */    
    public Region getPreviousRegion(Region region) { // optimize !!
        int currentRegionIndex=getIndexForRegion(region);
        if (currentRegionIndex>0) {
            lastSearchIndex=currentRegionIndex-1;
            return regions.get(currentRegionIndex-1);        
        }
        return null;
    }   
    
    private int getIndexForRegion(Region region) { // returns the index of the given region if it is in this track, else -1
        if (region==null) return -1;
        int lastIndex=lastSearchIndex;
        int size=regions.size();  
        int currentRegionIndex=-1;
        if (lastIndex>=0 && lastIndex<size && region==regions.get(lastIndex)) { // Try same region as last time
          currentRegionIndex=lastIndex;
        } else if (lastIndex>=0 && lastIndex+1<size && region==regions.get(lastIndex+1)) { // try the one directly after
          currentRegionIndex=lastIndex+1;
        } else if (lastIndex>0 && lastIndex<size && region==regions.get(lastIndex-1)) { // try the one before
          currentRegionIndex=lastIndex-1;
        } else { // search all regions
            for (int i=0;i<regions.size()-1;i++) {
                Region current=regions.get(i);
                if (region==current) {currentRegionIndex=i;break;}
            }
        }
        return currentRegionIndex;
    }
    
    public Region getFirstRegion() {
        if (regions!=null && !regions.isEmpty()) return regions.get(0);
        else return null;
    }
    
    public Region getLastRegion() {
        if (regions!=null && !regions.isEmpty()) return regions.get(regions.size()-1);
        else return null;
    }    
    
   /** Returns min and max region score values based on the current data
     * If the sequence contains no regions the min and max values will both be returned as zero
     * @return a double array with two components [0]=>min, [1]=>max
     */
    public double[] getMinMaxFromData() {
       double min=Double.MAX_VALUE; 
       double max=-Double.MAX_VALUE;
       if (regions==null || regions.isEmpty()) return new double[]{0,0}; 
       for (Region region:regions) {
         double score=region.getScore();
         if (score>max) max=score;
         else if (score<min) min=score;
       }       
       return new double[]{min,max};         
    }
    
   /** Returns min and max values for the given numeric property based on the current data
     * If the sequence contains no regions or the property is not numerical, the min and max values will both be returned as zero
     * @return a double array with two components [0]=>min, [1]=>max
     */
    public double[] getMinMaxFromData(String property) {
       if (property==null || property.equalsIgnoreCase("score")) return getMinMaxFromData();
       double min=Double.MAX_VALUE; 
       double max=-Double.MAX_VALUE;
       if (regions==null || regions.isEmpty()) return new double[]{0,0}; 
       for (Region region:regions) {
         Object propvalue=region.getProperty(property);
         if (!(propvalue instanceof Number)) continue;
         double value=((Number)propvalue).doubleValue();
         if (value>max) max=value;
         else if (value<min) min=value;
       }       
       return new double[]{min,max};         
    }    
    
    /**
     * Returns the data value at the specified position relative to the
     * start of the sequence (if position==0 it returns the value of the first 
     * position in the sequence.) If the position is outside the boundaries of the 
     * sequence it returns null. 
     * The value returned is an ArrayList containing Region objects
     * (since individually specified Regions can potentially overlap) 
     * 
     * @return An ArrayList containing the set of regions (if any) that overlap with the specified position, or null if the position is outside the boundaries of the sequence
     */    
    @Override
    public ArrayList<Region> getValueAtRelativePosition(int position) {
        if (position<0 || position>=sequencesize) return null;
        ArrayList<Region> list=new ArrayList<Region>();
        int startIndex=getSensibleStartIndex(position);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getRelativeStart();
            if (rstart>position) break;
            if (position>=rstart && position<=r.getRelativeEnd()) list.add(r);
        }
        return list;
    }

    /**
     * Returns the data value at the specified genomic position. relative to the
     * If the position is outside the boundaries of the sequence it returns null. 
     * The value returned is an ArrayList containing Region objects
     * (since individually specified Regions can potentially overlap) 
     * 
     * @param chromosome 
     * @param position 
     * @return An ArrayList containing the set of regions (if any) that overlap with the specified position, or null if the position is outside the boundaries of the sequence
     */      
    @Override
    public ArrayList<Region> getValueAtGenomicPosition(String chromosome, int position) {
        if (!chromosome.equalsIgnoreCase(this.chromosome)) return null;
        if (position<this.startPosition || position>this.endPosition) return null;
        position-=this.startPosition; // convert genomic position to an offset!
        ArrayList<Region> list=new ArrayList<Region>();
        int startIndex=getSensibleStartIndex(position);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getRelativeStart();
            if (rstart>position) break;
            if (position>=rstart && position<=r.getRelativeEnd()) list.add(r);
        }
        return list;
    }
    /**
     * Returns the data value at the specified genomic position. 
     * If the position is outside the boundaries of the sequence it returns null. 
     * The value returned is an ArrayList containing Region objects
     * (since individually specified Regions can potentially overlap) 
     * 
     * @param position 
     * @return An ArrayList containing the set of regions (if any) that overlap with the specified position, or null if the position is outside the boundaries of the sequence
     */      
    @Override
    public ArrayList<Region> getValueAtGenomicPosition(int position) {
        if (position<this.startPosition || position>this.endPosition) return null;
        position-=this.startPosition; // convert genomic position to an offset!
        ArrayList<Region> list=new ArrayList<Region>();
        int startIndex=getSensibleStartIndex(position);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getRelativeStart();
            if (rstart>position) break;
            if (position>=rstart && position<=r.getRelativeEnd()) list.add(r);
        }
        return list;
    }

    /**
     * Returns a region that spans the given genomic position and with "row" property
     * equal to the given argument
     * @param position Genomic position
     * @param row Visualization row
     * @return A reference to a Region satisfying the constraints or NULL if none were found
     */
    public Region getRegionAtGenomicPositionAndRow(int position, int row) {
        if (position<this.startPosition || position>this.endPosition) return null;
        position-=this.startPosition; // convert genomic position to an offset!
        int startIndex=getSensibleStartIndex(position);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getRelativeStart();
            if (rstart>position) break;
            if (position>=rstart && position<=r.getRelativeEnd() && r.row==row) return r;
        }
        return null;
    }

    /**
     * Returns TRUE if the position relative to sequence start lies within a region
     *
     * @param position the relative position within the sequence
     * @return TRUE if the position is within a region
     */
    public boolean isWithinRegion(int position) {
        if (position<0 || position>=sequencesize) return false;
        int startIndex=getSensibleStartIndex(position);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getRelativeStart();
            if (rstart>position) break;
            if (position>=rstart && position<=r.getRelativeEnd()) return true;
        }
        return false;
    }

    /**
     * Returns the number of regions overlapping with the specified genomic position
     * 
     * @param position 
     * @return The number of regions at this position
     */      
    public int getNumberOfRegionsAtGenomicPosition(int position) {
        int number=0;
        if (position<this.startPosition || position>this.endPosition) return 0;
        position-=this.startPosition; // convert genomic position to an offset!
        int startIndex=getSensibleStartIndex(position);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getRelativeStart();
            if (rstart>position) break;
            if (position>=rstart && position<=r.getRelativeEnd()) number++;
        }
        return number;
    }

    /**
     * Returns the number of regions overlapping with the specified position relative to sequence start
     * 
     * @param position 
     * @return The number of regions at this position
     */      
    public int getNumberOfRegionsAtRelativePosition(int position) {
        int number=0;
        if (position<0 || position>=sequencesize) return 0;
        int startIndex=getSensibleStartIndex(position);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getRelativeStart();
            if (rstart>position) break;
            if (position>=rstart && position<=r.getRelativeEnd()) number++;
        }
        return number;
    }

    /**
     * Returns the number of regions overlapping with the specified genomic interval
     * These regions need not span the entire interval as long as some part overlaps
     * @param start
     * @param end 
     * @return The number of regions overlapping this interval
     */      
    public int getNumberOfRegionsOverlappingGenomicInterval(int start, int end) {
        int number=0;
        if (end<this.startPosition || start>this.endPosition) return 0;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>end) break;
            if (!(end<rstart || start>r.getGenomicEnd())) number++;
        }
        return number;
    }

    /**
     * Returns true this dataset contains any regions that overlaps with the given genomic interval
     * @param start
     * @param end
     * @param restrictType If this is non-null the types of the regions must also match this String
     * @param exact If true and restrictTupe!=null the type of the regions in this dataset must be identical to the restrictType
     *              in order to be considered. If exact=false the type of the regions just have to 'match' the restrictType
     * @return TRUE if this dataset contains regions overlapping the interval which also satisfy any restrictions on type
     */
    public boolean hasRegionsOverlappingGenomicInterval(int start, int end, String restrictType, boolean exact) {// optimize !!
        if (end<this.startPosition || start>this.endPosition) return false;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>end) break;
            if (!(end<rstart || start>r.getGenomicEnd())) {
                if (restrictType==null) return true;
                else if (exact && restrictType.equals(r.getType())) return true;
                else if (!exact && (restrictType.contains(r.getType()) || r.getType().contains(restrictType))) return true;
            }
        }
        return false;
    }

    /**
     * Returns a list containing regions overlapping with the specified genomic interval
     * These regions need not span the entire interval as long as some part overlaps
     * Note that the list points to original regions and not clones!
     * @param start
     * @param end 
     * @return a list of regions
     */      
    public ArrayList<Region> getRegionsOverlappingGenomicInterval(int start, int end) {
        ArrayList<Region> list=new ArrayList<Region>();
        if (end<this.startPosition || start>this.endPosition) return list;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>end) break;
            if (!(end<rstart || start>r.getGenomicEnd())) list.add(r);
        }
        return list;
    }
    
    /**
     * Returns a pair of range indices into the regions-list for regions
     * that could potentially overlap a segment between the given start and end
     * coordinates.
     * NB! NB! NB! Note that the method could potentially also return a few
     * regions that are outside (before) the given interval (if there are other longer
     * regions before them that start outside the interval but cross over into it).
     * This method should thus only be used if the regions are checked a second time
     * for verification or if this fact is not critical (i.e. for visualization 
     * purposes regions outside the interval will be hidden outside of the drawing
     * area anyway). To get a correct list of regions overlapping the interval, 
     * use the slower method getRegionsOverlappingGenomicInterval().
     * The function returns NULL if the given interval is outside this sequence or no regions overlap the interval (potentially)
     * @param start
     * @param end
     * @return 
     */
    public int[] getRegionsOverlappingGenomicIntervalAsSlice(int start, int end) {
        int[] result=new int[]{Integer.MAX_VALUE,Integer.MIN_VALUE};
        if (end<this.startPosition || start>this.endPosition) return null;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        int count=0;
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>end) break;
            if (!(end<rstart || start>r.getGenomicEnd())) { // region potentially overlaps slice
                if (i<result[0]) result[0]=i;
                if (i>result[1]) result[1]=i;
                count++;
            }
        }
        if (count==0) return null;
        return result;
    }    

    /**
     * Returns the number of regions that lie fully within the specified genomic interval
     * 
     * @param start
     * @param end 
     * @return a list of regions
     */      
    public int getNumberOfRegionsWithinGenomicInterval(int start, int end) {
        int number=0;
        if (end<this.startPosition || start>this.endPosition) return 0;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>end) break;
            if (rstart>=start && r.getGenomicEnd()<=end) number++;
        }
        return number;
    }

    /**
     * Returns true if the dataset contains regions that lie fully within the specified genomic interval

     * @param start
     * @param end
     * @param restrictType If this is non-null the types of the regions must also match this String
     * @param exact If true and restrictTupe!=null the type of the regions in this dataset must be identical to the restrictType
     *              in order to be considered. If exact=false the type of the regions just have to 'match' the restrictType
     * @return TRUE if this dataset contains regions that lie within the interval and which also satisfy any restrictions on type
     */
    public boolean hasRegionsWithinGenomicInterval(int start, int end, String restrictType, boolean exact) {
        if (end<this.startPosition || start>this.endPosition) return false;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>end) break;
            if (rstart>=start && r.getGenomicEnd()<=end) {
                if (restrictType==null) return true;
                else if (exact && restrictType.equals(r.getType())) return true;
                else if (!exact && (restrictType.contains(r.getType()) || r.getType().contains(restrictType))) return true;
            }
        }
        return false;
    }

    /**
     * Returns a list containing regions that lie fully within the specified genomic interval
     * 
     * @param start
     * @param end 
     * @return a list of regions
     */      
    public ArrayList<Region> getRegionsWithinGenomicInterval(int start, int end) {
        ArrayList<Region> list=new ArrayList<Region>();
        if (end<this.startPosition || start>this.endPosition) return list;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>end) break;
            if (rstart>=start && r.getGenomicEnd()<=end) list.add(r);
        }
        return list;
    }

    /**
     * Returns the number of regions that fully span (completely cover) the genomic interval
     *
     * @param start
     * @param end
     * @return a list of regions
     */
    public int getNumberOfRegionsSpanningGenomicInterval(int start, int end) {
        int number=0;
        if (end<this.startPosition || start>this.endPosition) return 0;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>start) break;
            if (rstart<=start && r.getGenomicEnd()>=end) number++;
        }
        return number;
    }

    /**
     * Returns true if the dataset contains regions that fully span (completely cover) the genomic interval
     * @param start
     * @param end
     * @param restrictType If this is non-null the types of the regions must also match this String
     * @param exact If true and restrictType!=null the type of the regions in this dataset must be identical to the restrictType
     *              in order to be considered. If exact=false the type of the regions just have to 'match' the restrictType
     * @return TRUE if this dataset contains regions that fully span (completely cover) the interval and which also satisfy any restrictions on type
     */
    public boolean hasRegionsSpanningGenomicInterval(int start, int end, String restrictType, boolean exact) {// optimize !!
        if (end<this.startPosition || start>this.endPosition) return false;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>start) break;
            if (rstart<=start && r.getGenomicEnd()>=end) {
                if (restrictType==null) return true;
                else if (exact && restrictType.equals(r.getType())) return true;
                else if (!exact && (restrictType.contains(r.getType()) || r.getType().contains(restrictType))) return true;
            }
        }
        return false;
    }

    /**
     * Returns a list containing regions that fully span (completely cover) the genomic interval
     *
     * @param start
     * @param end
     * @return a list of regions
     */
    public ArrayList<Region> getRegionsSpanningGenomicInterval(int start, int end) {
        ArrayList<Region> list=new ArrayList<Region>();
        if (end<this.startPosition || start>this.endPosition) return list;
        int startIndex=getSensibleStartIndex(start);
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>start) break;
            if (rstart<=start && r.getGenomicEnd()>=end) list.add(r);
        }
        return list;
    }

    /**
     * Returns a list of regions in this track that are identical in every way to the given region
     * (This is usually no more than 1, but the method returns a list just in case and also
     * to be consistent with similar methods).
     * @param region
     * @return
     */
    public ArrayList<Region> getSameRegion(Region region) {
        int start=region.getGenomicStart();
        int end=region.getGenomicEnd();
        ArrayList<Region> list=new ArrayList<Region>(1);
        if (end<this.startPosition || start>this.endPosition) return list; // the region is not even within this sequence
        int startIndex=getSensibleStartIndex(region.getRelativeStart());
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>start) break;
            if (rstart==start && r.getGenomicEnd()==end) {
                if (region.isIdenticalTo(r)) list.add(r);
            }
        }
        return list;
    }

    /**
     * Returns a list of regions in this track that are similar to the given region
     * in the sense that they have the same location, type and orientation,
     * but others properties can be different.
     * @param region
     * @return
     */
    public ArrayList<Region> getSimilarRegion(Region region) {
        int start=region.getGenomicStart();
        int end=region.getGenomicEnd();
        ArrayList<Region> list=new ArrayList<Region>(1);
        if (end<this.startPosition || start>this.endPosition) return list; // the region is not even within this sequence
        int startIndex=getSensibleStartIndex(region.getRelativeStart());
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            int rstart=r.getGenomicStart();
            if (rstart>start) break;
            if (rstart==start && r.getGenomicEnd()==end) {
                if (region.hasSameLocationAndType(r)) list.add(r);
            }
        }
        return list;
    }
    
    /** 
     * Returns a list of all regions whose property matches the provided target value (which should not be null)
     * If the provided value is a String, it can be successfully be matched case-insensitively against any region value as converted to a string
     * (e.g. the value (String)"123" will match (Integer)123. 
     */
    public ArrayList<Region> getMatchingRegions(String property, Object value) {
        ArrayList<Region> list=new ArrayList();
        for (Region region:regions) {
            Object regionValue=region.getProperty(property);
            if (regionValue!=null) {
                if (regionValue.equals(value) || (value instanceof String && ((String)value).equalsIgnoreCase(regionValue.toString()))) list.add(region);
            }
        }
        return list;
    }

    /** 
     * Returns the first found region whose property matches the provided target value (which should not be null)
     * If the provided value is a String, it can be successfully be matched case-insensitively against any region value as converted to a string
     * (e.g. the value (String)"123" will match (Integer)123. 
     */
    public Region getFirstMatchingRegion(String property, Object value) {
        for (Region region:regions) {
            Object regionValue=region.getProperty(property);
            if (regionValue!=null) {
                if (regionValue.equals(value) || (value instanceof String && ((String)value).equalsIgnoreCase(regionValue.toString()))) return region;
            }
        }
        return null;
    }    
    
    /**
     * Returns a list of non-overlapping regions that together span the same genomic segments
     * that the original (possibly overlapping) regions span.
     * If the regions extend outside the edge of the sequence they are cropped
     * @return a list of regions
     */
    public ArrayList<Region> getCollapsedRegions() {
        ArrayList<Region> list=new ArrayList<Region>();
        if (regions.isEmpty()) return list;
        int currentStart=regions.get(0).getRelativeStart();
        if (currentStart<0) currentStart=0;
        int currentEnd=regions.get(0).getRelativeEnd();
        if (currentEnd>=sequencesize) currentEnd=sequencesize-1; // crop region if it extends outside the sequence        
        Region current=new Region(this, currentStart, currentEnd);
        for (int i=1;i<regions.size();i++) {
            Region next=regions.get(i);
            if (next.getRelativeEnd()<=currentEnd) {} // next region is within the previous
            else if (next.getRelativeStart()>current.getRelativeEnd()+1) {
                list.add(current);
                currentStart=next.getRelativeStart();
                if (currentStart<0) currentStart=0;
                currentEnd=next.getRelativeEnd();
                if (currentEnd>=sequencesize) currentEnd=sequencesize-1;               
                current=new Region(this, currentStart,currentEnd);
            } else { // overlapping or back-to-back
                currentEnd=next.getRelativeEnd();
                if (currentEnd>=sequencesize) currentEnd=sequencesize-1;
                current.setRelativeEnd(currentEnd);
            }
        }
        list.add(current);
        return list;
    }

    /**
     * Returns the total number of bases that are spanned by regions within this sequence
     */
    public int getNumberOfBasesSpannedByRegions() {
        if (regions.isEmpty()) return 0;
        int count=0;
        int currentStart=regions.get(0).getRelativeStart(); 
        int currentEnd=regions.get(0).getRelativeEnd();
        if (currentStart<0) currentStart=0; // crop region if it extends outside the sequence
        if (currentEnd>=sequencesize) currentEnd=sequencesize-1; // crop region if it extends outside the sequence
        for (int i=1;i<regions.size();i++) {
            Region next=regions.get(i);        
            if (next.getRelativeEnd()<=currentEnd) {} // next region is within the previous
            else if (next.getRelativeStart()>currentEnd+1) { // no overlap with next region
                count+=(currentEnd-currentStart+1);
                currentStart=next.getRelativeStart(); 
                currentEnd=next.getRelativeEnd();
            } else { // overlapping or back-to-back
                currentEnd=next.getRelativeEnd();
            }
            if (currentStart<0) currentStart=0;
            if (currentEnd>=sequencesize) currentEnd=sequencesize-1; // crop region if it extends outside the sequence          
        }
        count+=(currentEnd-currentStart+1);
        return count;
    }

    /**
     * Returns the closest region lying upstream of the specified genomic position
     * or the most downstream region that overlaps the given position, or NULL if no
     * region overlaps or lies upstream of the specified position
     * 
     * @param position A genomic position
     * @return a Region
     */      
    public Region getClosestUpstreamRegion(int position) {// optimize !!
        Region candidate=null;
        int relativePos=getRelativePositionFromGenomic(position);
        if (getStrandOrientation()==DIRECT) {
               for (Region r:regions) {
                 if (r.getRelativeStart()>relativePos) break;
                 else candidate=r;
               }
               return candidate;
        } else {
             for (int i=regions.size()-1;i>=0;i--) {
                 Region r=regions.get(i);
                 if (r.getRelativeEnd()<relativePos) break;
                 else candidate=r;                 
             }   
             return candidate;
        }            
    }
    
    
    /**
     * Returns the closest region lying downstream of the specified genomic position
     * or the most upstream region that overlaps the given position, or NULL if no
     * region overlaps or lies downstream of the specified position
     * 
     * @param position A genomic position
     * @return a Region
     */      
    public Region getClosestDownstreamRegion(int position) {// optimize !!
        Region candidate=null;
        int relativePos=getRelativePositionFromGenomic(position);
        if (getStrandOrientation()==REVERSE) {
               for (Region r:regions) {
                 if (r.getRelativeStart()>relativePos) break;
                 else candidate=r;
               }
               return candidate;
        } else {
             for (int i=regions.size()-1;i>=0;i--) {
                 Region r=regions.get(i);
                 if (r.getRelativeEnd()<relativePos) break;
                 else candidate=r;                 
             }   
             return candidate;
        }            
    }
    
    /**
     * Returns a list of all Regions associated with this RegionSequenceData object.
     * The returned ArrayList is a (shallow) clone of the internal list
     */   
    @SuppressWarnings("unchecked")
    public ArrayList<Region> getAllRegions() {
        return (ArrayList<Region>)regions.clone();
    }

    /**
     * Returns a list of all Regions of the given type associated with this RegionSequenceData object.
     * The returned ArrayList refers to a subset of the original Regions (so do not change the regions!)
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Region> getAllRegions(String type) {
        ArrayList<Region> results=new ArrayList<Region>();
        for (Region r:regions) {
            String rtype=r.getType();
            if (rtype!=null && rtype.equals(type)) results.add(r);
        }
        return results;
    }
    /**
     * Returns a list of all Regions of the given types associated with this RegionSequenceData object.
     * The returned ArrayList refers to a subset of the original Regions (so do not change the regions!)
     */    
    public ArrayList<Region> getAllRegions(HashSet<String> types) {
        ArrayList<Region> results=new ArrayList<Region>();
        for (Region r:regions) {
            String rtype=r.getType();
            if (rtype!=null && types.contains(rtype)) results.add(r);
        }
        return results;
    }    
    
    /**
     * Returns list of all Regions associated with this RegionSequenceData object
     * The returned ArrayList is the original list (not a copy) and should not be modified!
     */   
    public ArrayList<Region> getOriginalRegions() {
        return regions;
    }
          
    /**
     * Returns a count of the number of regions of the given type
     */
    public int countRegion(String type) {
        int count=0;
        for (Region r:regions) {
            if (type.equals(r.getType())) count++;
        }
        return count;
    }      
    
    /**
     * Returns a set containing all region types present in this sequence
     * and the number of times each of these region types occurs
     */
    public HashMap<String,Integer> countAllRegions() {
        HashMap<String,Integer> counts=new HashMap<String,Integer>();
        for (Region r:regions) {
            String type=r.getType();
            if (counts.containsKey(type)) counts.put(type,(counts.get(type)+1));
            else counts.put(type,1);
        }
        return counts;
    }      
    
    /** 
     * Returns true if this RegionSequenceData object already contains a Region 
     * that is 'identical' to the one supplied (both with respect to location and all other properties).
     * ToDo: this can be easily optimized
     */
    public boolean containsRegion(Region newregion) {
        int startIndex=getSensibleStartIndex(newregion.getRelativeStart());
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            if (r.isIdenticalTo(newregion)) return true;
        }
        return false;
    }
    
    /** 
     * Returns true if this RegionSequenceData object already contains a Region 
     * that is 'similar' to the one supplied (both with respect to location and other properties).
     * ToDo: this can be easily optimized
     * @param compareStandardProperties If TRUE all standard properties must be equal, including e.g. score (but not userdefined properties). 
     *                                  If false, only location/orientation and type has to be equal
     */
    public boolean hasSimilarRegion(Region newregion, boolean compareStandardProperties) {
        int startIndex=getSensibleStartIndex(newregion.getRelativeStart());
        int size=regions.size();
        for (int i=startIndex;i<size;i++) {
            Region r=regions.get(i);
            if (compareStandardProperties) {
               if (r.hasSameStandardProperties(newregion)) return true;
            } else if (r.hasSameLocationAndType(newregion)) return true;
        }
        return false;
    }    
 
    /**
     * clears all regions associated with this sequence
     * 
     */      
    public void clearRegions() {
        regions.clear();
    }

    
    @Override
    public void setParent(FeatureDataset parent) {
        this.parent=parent;
        if (parent==null) return;
        if (maxScore>((RegionDataset)parent).getMaxScoreValue()) ((RegionDataset)parent).setMaxScoreValue(maxScore);
    } 
    
    /** Updates all regions so that their "parent" property points to this object.
     *  This method was introduced to fix a legacy bug which is still present in some older saved sessions.
     */
    public void fixParentChildRelationships(FeatureDataset parent) {
        if (regions==null) return;
        for (Region region:regions) {
            region.setParent(this);
        }
        setParent(parent);
    }
    
    /** Returns TRUE if this RegionDataset contains motifs (or transcription factors binding sites),
     *  in which case the typenames of the regions should match the names of motifs registered with the engine
     */
    public boolean isMotifTrack() {
        if (parent==null) return false;
        else return ((RegionDataset)parent).isMotifTrack();
    }

    /** Returns TRUE if this RegionDataset contains modules (or composite transcription factors binding sites),
     *  in which case the typenames of the regions should match the names of modules registered with the engine
     */
    public boolean isModuleTrack() {
        if (parent==null) return false;
        else return ((RegionDataset)parent).isModuleTrack();
    }
    
    /** Returns TRUE if this RegionDataset contains "nested regions" (that are not modules)
     */
    public boolean isNestedTrack() {
        if (parent==null) return false;
        else return ((RegionDataset)parent).isNestedTrack();
    }    



   /**
     * Returns the maximum score value for regions in this dataset (the whole dataset not this sequence)
     * @return the maximum score value
     */       
    public double getMaxScoreValue() {
        if (parent==null) return maxScore;
        else return ((RegionDataset)parent).getMaxScoreValue();
    }

   /**
     * Returns the maximum score value for this sequence. To get the maximum value in the whole dataset use getMaxScoreValue()
     * @return the maximum score value
     */
    public double getMaxScoreValueForThisSequence() {
        return maxScore;
    }
    
   /**
     * Sets the maximum score value for this sequence 
    *  and also updates the maximum for the whole dataset if the given value is larger than the current max
     * @param max The maximum score value for this sequence (and dataset?)
     */       
    public void setMaxScoreValue(double max) {
        this.maxScore=max;
        if (parent!=null && (maxScore>((RegionDataset)parent).getMaxScoreValue())) ((RegionDataset)parent).setMaxScoreValue(maxScore);
    } 

    /**
     * Updates the cached max score for this sequence based on the current regions' scores
     * @return The maximum score value for this sequence
     */
    public double updateMaxScoreValueFromData() {
       double thismax=getMaxScoreValueForThisSequenceFromData();
       this.maxScore=thismax;
       return thismax;
    }

    /** Returns the largest score value among the regions in this sequence
     *  The value is calculated anew unlike getMaxScoreValue() and getMaxScoreValueForThisSequence()
     *  which returns a cached or preset value
     *  @return The maximum score value for this sequence (or 0 if the sequence contains no regions)
     */
    public double getMaxScoreValueForThisSequenceFromData() {
       if (regions==null || regions.isEmpty()) return 0;
       double datamax=-Double.MAX_VALUE;
       for (Region region:regions) {
           if (region.getScore()>datamax) datamax=region.getScore();
       }
       return datamax;
    }

    
    /**
     * Returns a deep copy of this RegionSequenceData object
     */
    @Override
    public RegionSequenceData clone() {
        RegionSequenceData newdata=new RegionSequenceData(sequenceName, chromosome, startPosition, endPosition);
        int size=getNumberOfRegions();
        for (int i=0;i<size;i++) {
            Region reg=getRegionByIndex(i);
            newdata.addRegion((Region)reg.clone()); // this will also set the parent property of the cloned regions to point to the new sequence
        }
        newdata.maxScore=this.maxScore;
        newdata.totalRows=this.totalRows;
        newdata.parent=this.parent; // this is not imported with importData() but is needed in order to get correct settings for e.g. isMotifTrack()
        newdata.sequencesize=this.sequencesize;
        return newdata;
    }

    
    @Override
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        RegionSequenceData rsource=(RegionSequenceData)source;
        this.regions=rsource.regions; // do not clone on import!        
        for (Region region:this.regions) region.setParent(this); // Update the region's parent property to point to this sequence. This line was not added until v2.0.-3.  How the hell did this work before?
        this.maxScore=((RegionSequenceData)source).maxScore;
        this.totalRows=((RegionSequenceData)source).totalRows;
        this.sequencesize=((RegionSequenceData)source).sequencesize;
        //notifyListenersOfDataUpdate(); 
    } 
    
    @Override  
    public void cropTo(int relativeStart, int relativeEnd) throws ExecutionError {
        if (relativeStart<0) throw new ExecutionError("Crop out of bounds (start<0)");
        if (relativeStart>=sequencesize) throw new ExecutionError("Crop out of bounds (start>size, size="+sequencesize+")");
        if (relativeEnd<0) throw new ExecutionError("Crop out of bounds (end<0)");
        if (relativeEnd>=sequencesize) throw new ExecutionError("Crop out of bounds (end>size, size="+sequencesize+")");
        if (relativeEnd<relativeStart) throw new ExecutionError("Crop out of bounds (end<start)");
        if (relativeStart==0 && relativeEnd==sequencesize-1) return;
        sequencesize=relativeEnd-relativeStart+1;
        int oldstart=getRegionStart();
        setRegionStart(oldstart+relativeStart);         
        setRegionEnd(oldstart+relativeEnd);          
        Iterator<Region> iterator=regions.iterator();
        while (iterator.hasNext()) {
            Region region=iterator.next();
            int oldRegionStart=region.getRelativeStart();
            int oldRegionEnd=region.getRelativeEnd();
            int newRegionStart=oldRegionStart-relativeStart;
            int newRegionEnd=oldRegionEnd-relativeStart;
            if (newRegionStart>=sequencesize || newRegionEnd<0) iterator.remove(); // region is fully outside new segment -> remove it
            else {
               region.setRelativeStart(newRegionStart);
               region.setRelativeEnd(newRegionEnd);
            }
        }    
        updateRegionSortOrder(); // 
    }    

    public static String getType() {return typedescription;}    

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}   

    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof RegionSequenceData)) return false;
        if (!super.containsSameData(other)) return false;
        return (regions.equals(((RegionSequenceData)other).regions));
    }
    
    /**
     * Returns a HashMap describing the names and class types of all user-defined properties that are defined for regions in this sequence.
     * If the type class of the value for a given property is the same across all regions, this class will be used as the type.
     * However, if the classes of the values varies, a "greatest common" class will be determined based on the following simple rule:
     * If all values are variations of Numbers (e.g. Integer or Doubles) the class will be Number, else the returned class will be String (since all values can be converted into Strings with toString()).
     * @param map If provided, the properties will be added to this map. Properties already existing in this map. If map is NULL a new map will be returned
     * @return 
     */
    public HashMap<String,Class> getUserDefinedProperties(HashMap<String,Class> map) {
        if (map==null) map=new HashMap<String,Class>();
        for (Region region:regions) {
            HashMap<String, Object> properties=region.getUserDefinedProperties();
            if (properties!=null) {
                for (String key:properties.keySet()) {
                    Object value=properties.get(key);
                    if (value!=null) {
                        Class propClass=value.getClass();
                        if (map.containsKey(key)) { // check for compatibility
                            if (map.get(key)!=propClass) { // must 'upgrade' class in map
                                Class mapclass=map.get(key);
                                if (mapclass==Number.class && value instanceof Number) {} // ok
                                else if (Number.class.isAssignableFrom(mapclass) && value instanceof Number) map.put(key,Number.class);
                                else {
                                    map.put(key,String.class);
                                }
                            }
                        } else map.put(key, propClass); // first entry
                    }
                }
            }
        }
        
        return map;
    }    
    

    private class RegionSortOrderComparator implements Comparator<Region> {
            boolean ascending=true;
            public RegionSortOrderComparator(boolean ascending) {
                this.ascending=ascending;
            }
            @Override
            public int compare(Region region1, Region region2) {
                if (ascending) {
                    if (region1.getRelativeStart()==region2.getRelativeStart()) { // regions start at same position
                        return region1.getLength()-region2.getLength(); // is region1 smaller than region2
                    }
                    else if (region1.isLocatedPriorTo(region2)) return -1;
                    else return 1;
                } else {
                    if (region1.getRelativeEnd()==region2.getRelativeEnd()) { // regions "start" at same position
                        return region1.getLength()-region2.getLength(); // is region1 smaller than region2
                    }
                    else if (region1.isLocatedPriorToOnReverseStrand(region2)) return -1;
                    else return 1;                    
                }
            }
    }

    /**
     * This method flattens the regions in this RegionSequenceData object, i.e., the original list of regions in this
     * sequence is replaced by a new list wherein the regions cover the exact same bases as the regions in
     * the original list, but none of the regions in the new list overlap. Overlapping regions (or regions lying
     * back-to-back) are thus merged into a single new region. Regions that go beyond the boundaries of the sequence
     * are cropped so that they end at the sequence start or end.
     * The new regions do not have score, type or orientation properties.
     * This method should be used with caution and should not be applied to 'original datasets' but rather on temporary
     * clones that are used for specific calculations where it is necessary to know which bases are covered by regions, but where
     * it would be inadvisable to base these calculations on the original regions.
     *
     */
    protected void flattenRegions() {
        ArrayList<Region> flattened=new ArrayList<Region>();
        if (regions.isEmpty()) return;
        for (int i=0;i<regions.size();i++) {
            Region region=regions.get(i);
            int regionStart=region.getRelativeStart();
            int regionEnd=region.getRelativeEnd();
            if (regionEnd<0 || regionStart>=sequencesize) continue; // region is fully outside sequence. Can this happen?
            if (regionStart<0) regionStart=0; // partially outside left end
            if (regionEnd>=sequencesize) regionEnd=sequencesize-1; // partially outside right end
            if (flattened.isEmpty()) flattened.add(new Region(null,regionStart,regionEnd));
            else {
                Region lastflattened=flattened.get(flattened.size()-1);
                if (regionStart<=lastflattened.getRelativeEnd()+1) {// overlapping or back-to-back => extend old region
                   if (regionEnd>lastflattened.getRelativeEnd()) lastflattened.setRelativeEnd(regionEnd);
                } else { // not overlapping => add new region
                   flattened.add(new Region(this,regionStart,regionEnd));
                }
            }
        }
        regions=flattened;
        updateRegionSortOrder();
    }
    
    /** Works the same way as the flattenRegion method with no arguments, except that it takes a list of regions
     *  to be flattened as argument and returns a list of flattened regions instead of changing the RegionSequenceData object itself.
     *  The provided list should originate from this RegionSequenceData object (since it uses information about its location/length),
     *  but it can be a subset of the regions.
     */
    public ArrayList<Region> flattenRegions(ArrayList<Region> list) {
        ArrayList<Region> flattened=new ArrayList<Region>();
        if (list.isEmpty()) return flattened;
        for (int i=0;i<list.size();i++) {
            Region region=list.get(i);
            int regionStart=region.getRelativeStart();
            int regionEnd=region.getRelativeEnd();
            if (regionEnd<0 || regionStart>=sequencesize) continue; // region is fully outside sequence. Cant this happen?
            if (regionStart<0) regionStart=0; // partially outside left end
            if (regionEnd>=sequencesize) regionEnd=sequencesize-1; // partially outside right end
            if (flattened.isEmpty()) flattened.add(new Region(null,regionStart,regionEnd));
            else {
                Region lastflattened=flattened.get(flattened.size()-1);
                if (regionStart<=lastflattened.getRelativeEnd()+1) {// overlapping or back-to-back => extend old region
                   if (regionEnd>lastflattened.getRelativeEnd()) lastflattened.setRelativeEnd(regionEnd);
                } else { // not overlapping => add new region
                   flattened.add(new Region(this,regionStart,regionEnd));
                }
            }
        }
        return flattened;
    }

    /** Goes through a segment from relativeStart to relativeEnd in the sequence and counts the number
     *  of regions at each position. The results are returned in a buffer starting a relativeStart (The buffer must be at least as long as the segment).
     * 
     * @param buffer An array to hold the results. If the buffer is NULL a new buffer will be provided and returned
     */
    public int[] flattenSegment(int[] buffer, int relativeStart, int relativeEnd) {
         int size=relativeEnd-relativeStart+1;
         if (size<=0) throw new AssertionError("End position is less than start position in RegionSequenceData.flattenSegment");
         if (relativeStart<0) throw new AssertionError("Negative start position in RegionSequenceData.flattenSegment");
         if (relativeEnd>this.getSize()-1) throw new AssertionError("End position outside sequence in RegionSequenceData.flattenSegment");
         if (buffer!=null && buffer.length<size) throw new AssertionError("Buffer too small in RegionSequenceData.flattenSegment");
         if (buffer==null) buffer=new int[size];
         else {for (int i=0;i<buffer.length;i++) buffer[i]=0;} // initialize current buffer
         if (regions.isEmpty()) return buffer;
         int genomicStart=getGenomicPositionFromRelative(relativeStart);
         int genomicEnd=getGenomicPositionFromRelative(relativeEnd);
         int[] slice=getRegionsOverlappingGenomicIntervalAsSlice(genomicStart,genomicEnd);        
         if (slice==null) return buffer;
         for (int i=slice[0];i<=slice[1];i++) {
            Region region=regions.get(i);
            int regionStart=region.getRelativeStart();
            int regionEnd=region.getRelativeEnd();           
            if (regionEnd<relativeStart || regionStart>relativeEnd) continue; // region is fully outside segment. Just skip it
            if (regionStart<relativeStart) regionStart=relativeStart; // trim region outside segment
            if (regionEnd>relativeEnd) regionEnd=relativeEnd;         // trim region outside segment
            for (int j=regionStart;j<=regionEnd;j++) {
                buffer[j-relativeStart]++;
            }
         }       
         return buffer;     
    }
    
    /** Returns the number of bases that are covered by regions in this sequence*/
    public int getBaseCoverage() {
        RegionSequenceData copy=this.clone();
        copy.flattenRegions();
        int bases=0;
        for (Region r:copy.getOriginalRegions()) {
            bases+=r.getLength();
        }
        return bases;
    }
    
    /** If this dataset represents a nested track or module track, this method will return a new sequence
     *  containing only the nested regions (e.g. the single TFBS within the modules.)
     *  If it is not a nested/module track it will just return an empty sequence   
     */
    protected RegionSequenceData getNestedRegions() {
        RegionSequenceData newdata=new RegionSequenceData(sequenceName, chromosome, startPosition, endPosition);
        if (isModuleTrack() || isNestedTrack()) {
            for (Region reg:regions) {
                for (String property:reg.getAllPropertyNames()) {
                    Object val=reg.getProperty(property);
                    if (val instanceof Region) newdata.addRegion((Region)((Region)val).clone());
                    else if (val instanceof ArrayList) {
                        for (Object obj:((ArrayList)val)) {
                            if (obj instanceof Region) newdata.addRegion((Region)((Region)obj).clone());
                        }
                    }
                } // end for each property
            } // end for each region
        }
        newdata.maxScore=this.maxScore; // keep parent's scale
        newdata.totalRows=0;
        newdata.parent=this.parent; // this is not imported with importData() but is needed in order to get correct settings for e.g. isMotifTrack()
        return newdata;
    }
    
    /** If this dataset represents a nested track or module track, this method will return a new sequence
     *  containing only the nested regions (e.g. the single TFBS within the modules or exons within genes.)
     *  If it is not a nested/module track it will just return an empty sequence
     *  The difference between this method and getNestedRegions() is that this method will add two new numerical region properties
     *  to each of the extracted nested regions:  "region_index" and "region_count"
     *  "region_count" is the total number of nested regions extracted from the parent and "region_index" is an incremental index
     *  number assigned to each child region counting from the start of the parent region (relative to the orientation of the parent).     
     *  Hence, if the parent region is a gene the "region_index" will be the exon number and the "region_count" will be the number of exons for that gene
     */
    protected RegionSequenceData getNumberedNestedRegions() {
        RegionSequenceData newdata=new RegionSequenceData(sequenceName, chromosome, startPosition, endPosition);

        Comparator<Region> compD=new Comparator<Region>() {                        
            @Override
            public int compare(Region region1, Region region2) {
                int diff=region1.getRelativeStart()-region2.getRelativeStart();
                return (diff!=0)?diff:region1.getRelativeEnd()-region2.getRelativeEnd();
            }        
        };
        Comparator<Region> compR=new Comparator<Region>() {                        
            @Override
            public int compare(Region region1, Region region2) {
                int diff=region2.getRelativeEnd()-region1.getRelativeEnd();
                return (diff!=0)?diff:region2.getRelativeStart()-region1.getRelativeStart();              
            }        
        };        
        if (isModuleTrack() || isNestedTrack()) {
            for (Region reg:regions) {
                ArrayList<Region> nestedRegions=new ArrayList<Region>();
                for (String property:reg.getAllPropertyNames()) {
                    Object val=reg.getProperty(property);
                    if (val instanceof Region) nestedRegions.add(((Region)val).clone());
                    else if (val instanceof ArrayList) {
                        for (Object obj:((ArrayList)val)) {
                            if (obj instanceof Region) nestedRegions.add(((Region)obj).clone());
                        }
                    }
                } // end for each property
                // add numbers to nested regions                
                int region_count=nestedRegions.size();
                for (Region newregion:nestedRegions) { // add nested regions to parent (and add count)
                    newregion.setProperty("region_count", region_count);
                    newdata.addRegion(newregion);
                }
                Comparator<Region> comp=(reg.getOrientation()==Region.REVERSE)?compR:compD;
                Collections.sort(nestedRegions, comp);
                int index=0;
                for (Region newregion:nestedRegions) { // assign region indexes
                    index++;
                    newregion.setProperty("region_index", index);
                }                
            } // end for each parent region in 
        }
        newdata.maxScore=this.maxScore; // keep parent's scale
        newdata.totalRows=0;
        newdata.parent=this.parent; // this is not imported with importData() but is needed in order to get correct settings for e.g. isMotifTrack()
        return newdata;
    }
    
    /** If this dataset represents a nested track or module track, this method will return a new sequence
     *  containing only the top level regions without the nested regions
     */
    protected RegionSequenceData getTopLevelRegions() {
        RegionSequenceData newdata=new RegionSequenceData(sequenceName, chromosome, startPosition, endPosition);
        if (isModuleTrack() || isNestedTrack()) {
            for (Region reg:regions) {
                Region newregion=(Region)((Region)reg).clone();
                // remove all nested region from this top level region
                for (String property:newregion.getAllPropertyNames()) {
                    Object val=newregion.getProperty(property);
                    if (val instanceof Region) newregion.setProperty(property, null);
                    else if (val instanceof ArrayList) { // list of regions?
                        for (Object obj:((ArrayList)val)) {
                            if (obj instanceof Region) {newregion.setProperty(property, null);break;}
                        }
                    }
                } // end for each property     
                newdata.addRegion(newregion);
            } // end for each region
        }
        newdata.maxScore=this.maxScore;
        newdata.totalRows=0;
        newdata.parent=this.parent; // this is not imported with importData() but is needed in order to get correct settings for e.g. isMotifTrack()
        return newdata;
    }    

    /** Returns a new Region Sequence Data track based on this track where the regions are 
     *  represented by a single position so that their size is only 1bp. 
     *  The position is located either at the center or at the boundary of the original region,
     *  as specified by the anchor parameter
     *  @anchor center, start, end, regionStart, regionEnd, relativeStart, relativeEnd
     */
    protected RegionSequenceData extractRegionsAsPositions(String anchor) throws ExecutionError {
        RegionSequenceData newdata=new RegionSequenceData(sequenceName, chromosome, startPosition, endPosition);
        for (Region reg:regions) {
            Region copy = (Region)reg.clone();
            int regOr=reg.getOrientation();
                 if (anchor.equals("start")) {copy.setRelativeEnd(reg.getRelativeStart());}
            else if (anchor.equals("end"))   {copy.setRelativeStart(reg.getRelativeEnd());}
            else if (anchor.equals("center")) {int center=(reg.getRelativeStart()+reg.getRelativeEnd())/2;copy.setRelativeStart(center);copy.setRelativeEnd(center);}
            else if (anchor.equals("regionStart")) {
               if (regOr==Region.REVERSE) copy.setRelativeStart(reg.getRelativeEnd());
               else copy.setRelativeEnd(reg.getRelativeStart());
            }
            else if (anchor.equals("regionEnd")) {
               if (regOr==Region.REVERSE) copy.setRelativeEnd(reg.getRelativeStart());  
               else copy.setRelativeStart(reg.getRelativeEnd());        
            }
            else if (anchor.equals("relativeStart")) {
               if (getStrandOrientation()==Sequence.REVERSE) copy.setRelativeStart(reg.getRelativeEnd());
               else copy.setRelativeEnd(reg.getRelativeStart());             
            }
            else if (anchor.equals("relativeEnd")) {
               if (getStrandOrientation()==Sequence.REVERSE) copy.setRelativeEnd(reg.getRelativeStart());  
               else copy.setRelativeStart(reg.getRelativeEnd());              
            }
            else if (anchor.equals("edges")) { // must make two copies one for each end
               copy.setRelativeEnd(reg.getRelativeStart()); // start edge
               newdata.addRegion(copy);
               copy = (Region)reg.clone();
               copy.setRelativeStart(reg.getRelativeEnd()); // end edge. This is added to dataset below           
            }            
            else throw new ExecutionError("Unknown region position reference: "+anchor);
            newdata.addRegion(copy); //
        } // end for each region
        newdata.maxScore=this.maxScore;
        newdata.totalRows=0;
        newdata.parent=this.parent; // this is not imported with importData() but is needed in order to get correct settings for e.g. isMotifTrack()
        return newdata;
    }    
    
    /** 
     * If this track does not contain any region it returns null
     * otherwise it returns the smallest[0] and largest[1] relative coordinate
     * that is occupied by a region in this dataset. hence the returned coordinates
     * represents the "span" of the regions
     * Note that the smallest and largest relative coordinates can be outside the
     * sequence itself if a region extends beyond the sequence boundaries 
     */
    public int[] getMinMaxPositionsOfAllRegions() {
        if (regions==null || regions.isEmpty()) return null;
        int[] result=new int[]{regions.get(0).getRelativeStart(),regions.get(0).getRelativeEnd()};
        for (Region region:regions) {
            if (region.getRelativeStart()<result[0]) result[0]=region.getRelativeStart();
            if (region.getRelativeEnd()>result[1]) result[1]=region.getRelativeEnd();
        }
        return result;
    }
    
    /**
     * Returns a set containing all unique region types found in this sequence
     * @return 
     */
    public HashSet<String> getRegionTypes() {
        HashSet<String> types=new HashSet<String>();
        for (Region region:regions) {
            String type=region.getType();
            if (type!=null) types.add(type);
        }
        return types;
    }

    
    /**
     * This method assumes all regions are in sorted order and will use binary search
     * to return an index into the regions list pointing to a region that can 
     * be used as a good starting point when iterating through a subset of 
     * the regions list for a segment that starts at the given position.
     * Thus, the returned index should point to a region that is either one of the
     * last regions before the given position or is the first region to start at
     * or after the given position. (Or said in a different way, there should be no
     * other regions before the indexed region that lies at or after the given position).
     * given position
     * @param pos The relative position
     * @return A good index to start iteration at
     */
    public int getSensibleStartIndex(int pos) {
        return 0; // it is not possible to optimize without further knowledge (e.g. size of longest region)
//        //if (regions.size()<20) return 0; // very short list: don't bother optimizing
//        // the binary search algorithm is copied from the implementation in Collections
//        int lastBefore=0; // index of last known region whose start pos is < target pos
//        int low = 0;
//        int high = regions.size()-1;       
//        while (low <= high) {
//            int mid = (low + high) >>> 1; // this is supposedly a clever way of calculating the midpoint
//            Region region = regions.get(mid);
//            int regStart=region.getRelativeStart();
//            if (regStart<pos) // this region is located before target position
//                 {low = mid + 1; lastBefore=mid;}
//            else if (regStart>=pos) // this regions is after (or at) target position
//                 {high = mid - 1;}
//        }
//        return lastBefore;
    }
    
    
    
    /** Outputs information about this dataset into the buffer */
    public StringBuilder debug(StringBuilder builder) {
        if (builder==null) builder=new StringBuilder();
        builder.append(" - "+getName()+" : "+getRegionAsString()+", size="+getSize()+", regions="+((regions==null)?0:regions.size())+",   ["+System.identityHashCode(this)+"]");
        return builder;
    }      
    
    
    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }
}
