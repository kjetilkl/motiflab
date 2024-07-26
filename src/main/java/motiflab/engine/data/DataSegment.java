/*
 
 
 */

package motiflab.engine.data;

import java.io.Serializable;
import java.util.ArrayList;
import motiflab.engine.ExecutionError;

/**
 * This class represent a portion of a Data Track for a Sequence.
 * When loading data for FeatureSequenceData objects, data can be retrieved 
 * either from disc cache or from external DataSources. Sometimes, some
 * portions of the data are available in cache while others must be obtained anew.
 * This class represents such subsegments (either from cache or external source)
 * that together are copied into complete FeatureSequenceData objects.
 * 
 * @author kjetikl
 */
public class DataSegment implements Serializable, Comparable<DataSegment> {
    private String trackName;
    private int organism;
    private String genomebuild;
    private String chromosome;
    private int segmentStart=0; // genomic coordinates for this segment
    private int segmentEnd=0;   // genomic coordinates for this segment
    private Object data=null;   // this could be an array (double[] or char[]) or an ArrayList<Region> depending on the data stored
    private transient boolean saveToCache=false;
//    private boolean useGenomicCoordinatesForRegions=false; // if this is TRUE, the start/end coordinates for all Regions in this DataSegment are genomic (starting at pos 1) rather than relative to the start of this DataSegment
    
    
    /** Creates */
    public DataSegment(String trackName,int organism, String genomebuild, String chromosome, int start, int end, Object data) {
        this.trackName=trackName;
        this.organism=organism;
        this.chromosome=chromosome;
        this.segmentStart=start;
        this.segmentEnd=end;
        this.data=data;
        this.genomebuild=genomebuild;
    }

    /**
     * Returns and empty segment (with no data) corresponding to a subsection of this segment
     * @param relativeStart The start of the subsegment to return relative to this segment (starting at position 0)
     * @param size The size of the subsegment to return (maximum). 
     *             Note that if the subsegment extends beyond the boundary of the parent segment, it will be truncated (size will be smaller than specified)
     * @return a new empty datasegment corresponding to a subsegment of the parent, or NULL if the relativeStart goes beyond the size of the segment
     */
    public DataSegment getEmptySubSegment(int relativeStart, int size) {
        if (relativeStart<0) relativeStart=0;
        int start=segmentStart+relativeStart;
        if (start>segmentEnd) return null;
        int end=start+size-1;
        if (end>segmentEnd) end=segmentEnd;
        return new DataSegment(trackName, organism, genomebuild, chromosome, start, end, null);
    }
     /**
     * Returns and empty segment (with no data) corresponding to a subsection of this segment
     * If the given location is fully outside this segment, the method will return NULL.
     * However, if the given location overlaps but extends outside the boundaries of this segment
     * the location will be adjusted so that the returned subsegment is within the boundaries of the parent
     * @param genomicStart The start of the subsegment given in genomic coordinates
     * @param genomicEnd The end of the subsegment given in genomic coordinates
     * @return a new empty datasegment corresponding to a subsegment of the parent, or NULL if the location of the subsegment does not overlap this segment
     */   
    public DataSegment getEmptySubSegmentGenomicPositions(int genomicStart, int genomicEnd) {
        if (genomicStart>segmentEnd || genomicEnd<segmentStart) return null;
        if (genomicStart<segmentStart) genomicStart=segmentStart;
        if (genomicEnd>segmentEnd) genomicEnd=segmentEnd;
        return new DataSegment(trackName, organism, genomebuild, chromosome, genomicStart, genomicEnd, null);
    }    
    
    public String getChromosome() {
        return chromosome;
    }
    
    public String getGenomeBuild() {
        return genomebuild;
    }
    
    public int getOrganism() {
        return organism;
    }
    
    public String getDatatrackName() {
        return trackName;
    }
    
    public Object getData() {
        return data;
    }

    public int getSegmentEnd() {
        return segmentEnd;
    }

    public int getSegmentStart() {
        return segmentStart;
    }
    
    public void setGenomeBuild(String genomebuild) {
        this.genomebuild=genomebuild;
    }

    public void setSegmentEnd(int end) {
        segmentEnd=end;
    }

    public void setSegmentStart(int start) {
        segmentStart=start;
    }
     
    /** Returns the length of this DataSegment */
    public int getSize() {
        return segmentEnd-segmentStart+1;
    }
    
    public void setSegmentData(Object newdata) {
        this.data=newdata;
    }
    
    public String getLocationAsString() {
        return chromosome+":"+segmentStart+"-"+segmentEnd;
    }
    
//    public void setUseGenomicCoordinatesForRegions(boolean useGenomic) {
//        useGenomicCoordinatesForRegions=useGenomic;
//    }
        
    /**
     * Initializes this data segment with default contents according to the type
     * @param type 
     */
    public void initializeDefault(Class type) {
        if (type==RegionDataset.class) this.data=new ArrayList<Region>();
        else if (type==NumericDataset.class) this.data=new double[getSize()];
        else if (type==DNASequenceDataset.class) {
            char[] buffer=new char[getSize()];
            for (int i=0;i<buffer.length;i++) buffer[i]='N';
            this.data=buffer;
        }
    }
    
    /** 
     * This method can be used to add single Regions to the segment if the DataSegment represents
     * a portion of a RegionSequenceData track. The Regions added must have coordinates relative
     * to the start of the Segment. 
     */
    @SuppressWarnings("unchecked")
    public void addRegion(Region region) {
        if (this.data==null || !(this.data instanceof ArrayList)) this.data=new ArrayList<Region>();
        if (region!=null) ((ArrayList)this.data).add(region);        
    }
    
    /**
     * 
     * @param start a coordinate relative to the start of the buffer
     * @param end a coordinate relative to the start of the buffer
     * @param value the value to insert at this position
     */
    public void addNumericValue(int start, int end, double value) {
        if (this.data==null || !(this.data instanceof double[])) this.data=new double[getSize()];
        if (start<0) start=0;
        if (end>=getSize()) end=getSize()-1;
        for (int i=start;i<=end;i++) {
            ((double[])this.data)[i]=value;
        }
    }
    
    /** 
     * Returns a String that can be used as a unique filename for this DataSegment object
     * if it should be saved to a disc cache
     */
    public String getFileName() {
        return trackName+"_"+organism+"_"+genomebuild+"_"+chromosome+"_"+segmentStart+"_"+segmentEnd;
    }

    @Override
    public int compareTo(DataSegment other) {
        if (this.segmentStart==other.segmentStart) return 0;
        else if (this.segmentStart<other.segmentStart) return -1;
        else return 1;
    }
    
    /** Returns true if the DataSegment object contains all the data for its segment */
    public boolean containsData() {
        return (data!=null);
    }
    
    /** Returns true if this DataSegment currently has no associated data */
    public boolean isEmpty() {
        return (data==null);
    }
    
    public boolean shouldSaveToCache() {
        return saveToCache;
    }
    
    public void setSaveToCache(boolean save) {
        saveToCache=save;
    }
    
    @Override
    public String toString() {
        return chromosome+":"+segmentStart+"-"+segmentEnd+" ["+((data==null)?"empty":"full")+"] @" + Integer.toHexString(hashCode());
    }
    
    /** 
     * Copies the contents of this data object into the corresponding subsegment of
     * the supplied FeatureSequenceData object. 
     * The methods returns TRUE if the copy operation was completed successfully
     * or FALSE if not (which can happen if this object contains no data or if
     * the target FeatureSequenceData object was of an incompatible type).
     */
    public boolean copyData(FeatureSequenceData target) throws ExecutionError {
        if (!containsData()) return false;
        //System.err.println("Copying data into "+target.getSequenceName()+"["+target.getRegionStart()+"-"+target.getRegionEnd()+"]   segment="+segmentStart+"-"+segmentEnd+"   type="+data.getClass().toString());
        int useStart=segmentStart;
        int useEnd=segmentEnd;
        int offset=0;
        if (segmentStart<target.getRegionStart()) {
            offset=target.getRegionStart()-segmentStart;
            useStart=target.getRegionStart();
        }
        if (segmentEnd>target.getRegionEnd()) {
            useEnd=target.getRegionEnd();
        }
        int length=useEnd-useStart+1;
        if (target instanceof DNASequenceData) {
            if (!(data instanceof char[])) {throw new ExecutionError("SYSTEM ERROR: Incorrect data type. Expected DNA sequence data - got '"+data.getClass().getSimpleName()+"'. Possibly wrong data track type specification?");}
            DNASequenceData targetdata=(DNASequenceData)target;
            char[] datavalues=(char[])data;
            //System.err.println("Copying char[] into DNASequenceData from "+useStart+"-"+useEnd+"   offset="+offset+"  length="+length);
            for (int i=0;i<length;i++) {
                targetdata.setValueAtGenomicPosition(useStart+i, datavalues[offset+i]); // note that some positions can be outside the target sequence
            }
        }
        else if (target instanceof NumericSequenceData) {
            if (data instanceof float[]) { // just to make it compatible with previous versions;
                float[] olddata=(float[])data;
                double[] newdata=new double[olddata.length];
                for (int i=0;i<newdata.length;i++) newdata[i]=olddata[i]; // cannot use System.arraycopy() because type-conversion is involved
                data=newdata;
            }
            if (!(data instanceof double[])) {throw new ExecutionError("SYSTEM ERROR: Incorrect data type. Expected numeric data - got '"+data.getClass().getSimpleName()+"'. Possibly wrong data track type specification?");}
            NumericSequenceData targetdata=(NumericSequenceData)target;
            double[] datavalues=(double[])data;
            //System.err.println("Copying double[] into NumericSequenceData from "+useStart+"-"+useEnd+"   offset="+offset+"  length="+length);
            for (int i=0;i<length;i++) {
                targetdata.setValueAtGenomicPosition(useStart+i, datavalues[offset+i]); // note that some positions can be outside the target sequence
            }
        } 
        else if (target instanceof RegionSequenceData) {
            // Note that coordinates for Regions in the segment should initially be relative to the segment start
            // Before the regions are added to the target, the Region coordinates are updated so that
            // they are relative to the start of the target sequence data object.
            if (!(data instanceof ArrayList)) {throw new ExecutionError("SYSTEM ERROR: Incorrect data type. Expected region data - got '"+data.getClass().getSimpleName()+"'. Possibly wrong data track type specification?");}
            ArrayList list=(ArrayList)data;
            RegionSequenceData targetdata=(RegionSequenceData)target;                      
            offset=segmentStart-targetdata.getRegionStart(); // used to adjust the relative start of regions from subsegment in this object to full segment in RegionSequenceData           
            for (Object element:list) {
                if (!(element instanceof Region)) {System.err.println("SYSTEM ERROR: Incorrect data type in DataSegment.copyData: source="+element.getClass().toString()+", target="+target.getClass()+toString());return false;}
                Region region=(Region)element;
                region.updatePositionReferenceFrame(offset);
                if (region.getRelativeStart()>=targetdata.getSize() || region.getRelativeEnd()<0) continue; // Region lies outside targetdata segment
                targetdata.addRegionIfnotAlreadyPresent(region); 
                if (region.hasNestedRegions()) {
                    RegionDataset dataset=(RegionDataset)targetdata.getParent();
                    if (dataset!=null) dataset.setNestedTrack(true);
                }
            }            
        }     
        return true;
    }

    /**
     * Given a source DataSegment which overlaps with this one,
     * the data from relevant portions of the source is imported into this segment
     * Note that existing data in this segment will be kept if possible
     * @param source
     * @return NULL if everything went OK, else an error code
     */
    public String importData(DataSegment source) {        
        if (!this.getChromosome().equals(source.getChromosome())) return "339070"; // wrong chromosome
        if (!this.getGenomeBuild().equals(source.getGenomeBuild())) return "339071"; // wrong genome build
        if (!this.getDatatrackName().equals(source.getDatatrackName())) return "339072"; // wrong data track!
        if (this.segmentStart>source.segmentEnd || this.segmentEnd<source.segmentStart) return "339073"; // no overlap
        if (source.data==null) return "339074"; // missing data
        // find the intersection of the two segments
        int genomicStart=(source.segmentStart>this.segmentStart)?source.segmentStart:this.segmentStart; // choose the largest start coordinate
        int genomicEnd=(source.segmentEnd<this.segmentEnd)?source.segmentEnd:this.segmentEnd; // choose the smallest start end           
        int sourceStart=genomicStart-source.segmentStart; // relative start in source segment
        int thisStart=genomicStart-this.segmentStart; // relative start in this segment        
        int size=genomicEnd-genomicStart+1;
        if (source.data instanceof char[]) {
            initializeDefault(DNASequenceDataset.class);
            for (int i=0;i<size;i++) ((char[])this.data)[thisStart+i]=((char[])source.data)[sourceStart+i];
        } else if (source.data instanceof double[]) {
            initializeDefault(NumericDataset.class);
            // System.err.println("Importing numeric data ("+size+" bp into "+genomicStart+" thisStart="+thisStart+"  sourceStart="+sourceStart);
            for (int i=0;i<size;i++) ((double[])this.data)[thisStart+i]=((double[])source.data)[sourceStart+i];           
        } else if (source.data instanceof ArrayList) {
            initializeDefault(RegionDataset.class);
            for (Region region:(ArrayList<Region>)source.data) {
                int regionGenomicStart=region.getRelativeStart()+source.segmentStart;
                int regionGenomicEnd=region.getRelativeEnd()+source.segmentStart;
                boolean outside=(regionGenomicEnd<this.segmentStart || regionGenomicStart>this.segmentEnd);
                if (!outside) { // add this region, but update the coordinates from the cached segment to this segment!
                    region.updatePositionReferenceFrame(source.segmentStart, this.segmentStart);
                    addRegion(region);
                }                
            }
        } else return "339075"; // unknown data contents
        return null;
    }
    
        
}
