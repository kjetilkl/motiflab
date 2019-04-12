
package motiflab.engine.data;

import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;

/**
 * This is the top (abstract) class for all data objects that encapsulates
 * feature data about a single sequence from a specific region within a genome
 * (whether it be the DNA bases themselves or some other features represented
 * as numeric or region data).
 * <p>
 * Data stored in FeatureSequenceData should be from direct strand. If the region
 * is associated with a gene on the reverse strand, any conversion (for display
 * purposes or for upstream/downstream operation directives) should be done as 
 * necessary at a later time using appropriate information about orientation.
 * 
 * The class Sequence serves as template class for FeatureSequenceData regarding which
 * genomic region to cover. The template Sequence object should have the same "sequenceName"
 * as the corresponding FeatureSequenceData object.
 * 
 * FeatureSequenceData objects are grouped into FeatureDataset collections. The parent dataset
 * provides additional information and functionality which could be useful for FeatureSequenceData objects
 * 
 * @author kjetikl
 */
public abstract class FeatureSequenceData extends Data {
    public static final int DIRECT  =  1;  // These are (and must be!) identical to the same static fields used in Sequence
    public static final int REVERSE = -1;  // These are (and must be!) identical to the same static fields used in Sequence
    
    protected FeatureDataset parent=null;      
    protected String sequenceName=null;

    // The following three location properties are copied from the corresponding Sequence object, 
    // but they are immutable properties in Sequence and are therefore safe to duplicate here
    // (Note: they can be modified by the "crop_sequences" operation, but that operation will also update the properties here)
    protected String chromosome="?";
    protected int startPosition=0; // genomic coordinate
    protected int endPosition=0; // genomic coordinate
    
    // The four sequence properties below use to be copied from the correponding Sequence object, 
    // but from MotifLab version 2.0 onwards they are allowed to be modified in the Sequence.
    // For simplicity and to avoid duplication we should therefore start refering to these properties
    // directly in the corresponding Sequence
    
//    protected String associatedGeneName=null;
//    protected Integer TSS=null;
//    protected Integer TES=null;
//    protected int orientation=DIRECT; 

    


    @Override
    public String getName() {return getSequenceName();}

    @Override
    public Object getValue() {return this;} // should maybe change later
    
    @Override
    public String getValueAsParameterString() {return "N/A";}

    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof FeatureSequenceData)) return false;
        FeatureSequenceData source =(FeatureSequenceData)other;
        if ((this.sequenceName!=null && source.sequenceName==null) || (this.sequenceName==null && source.sequenceName!=null) || (!this.sequenceName.equals(source.sequenceName))) return false;
        if ((this.chromosome!=null && source.chromosome==null) || (this.chromosome==null && source.chromosome!=null) || (!this.chromosome.equals(source.chromosome))) return false;
        if (this.startPosition!=source.startPosition) return false;
        if (this.endPosition!=source.endPosition) return false;
        // the fields below have now been deprecated from FeatureSequenceData and are rather referenced directly in the corresponding Sequence object        
//        if (!this.associatedGeneName.equals(source.associatedGeneName)) return false;        
//        if (this.TSS==null && source.TSS!=null) return false;
//        if (this.TSS!=null && source.TSS==null) return false;
//        if (this.TSS!=null && source.TSS!=null && !this.TSS.equals(source.TSS)) return false;
//        if (this.TES==null && source.TES!=null) return false;
//        if (this.TES!=null && source.TES==null) return false;
//        if (this.TES!=null && source.TES!=null && !this.TES.equals(source.TES)) return false;
//        if (this.orientation!=source.orientation) return false;        
        return true;
    }

    /** 
     * Returns an object representing the values for this FeatureSequenceData within the
     * specified genomic interval. If either of the start and end coordinates lie outside
     * the genomic region for this sequence, they will be moved (without warning) to 
     * coincide with the actual start or end coordinate. Hence, if a segment larger than
     * the region covered by this sequence is specified, only data for the region covered 
     * will be returned (so the returned interval can be shorter than the interval requested!) 
     * If the end coordinate is smaller than the start coordinate, or if the specified segments 
     * lies completely outside the bounds of the sequence a NULL value will be returned
     */
    public abstract Object getValueInGenomicInterval(int start, int end);

    /**
     * Returns the size of this sequence (the number of bases spanned by the region)
     * 
     * @return size of sequence
     */
    public int getSize() {return endPosition-startPosition+1;}
    
    /**
     * Sets the name of the sequence
     * 
     * @param sequenceName a name for this sequence 
     */
    public void setSequenceName(String sequenceName) {
        this.sequenceName=sequenceName;
    }
    
    /**
     * Returns the name of the sequence. If the sequence has no specific name this
     * function will return a name based on the genomic region.
     * 
     * @return a name for this sequence
     */
    public String getSequenceName() {
        if (sequenceName!=null) {
            return sequenceName;
        } else {
          return chromosome+":"+startPosition+"-"+endPosition;   
        }
    }

    @Override
    public void rename(String name) {
        setSequenceName(name);
    }    
    
//    /**
//     * Sets the name of the gene associated with this genomic region
//     * Deprecated since the gene name can now be changed in the corresponding Sequence object
//     * @param geneName name of associated gene for this region
//     */
//    @deprecated
//    public void setGeneName(String geneName) {
//        this.associatedGeneName=geneName;
//    }
    
    /**
     * Returns the name of the gene associated with this genomic region
     * 
     * @return a gene name for this sequence
     */
    public String getGeneName() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getGeneName():null;       
    }
    
    /**
     * Sets the name of the chromosome for this genomic region
     * 
     * @param chromosome chromosome name for this region
     */
    public void setChromosome(String chromosome) {
        this.chromosome=chromosome;
    }
    
    /**
     * Returns the chromosome name for this genomic region
     * 
     * @return chromosome name
     */
    public String getChromosome() {
        return chromosome;      
    }
    
    /**
     * Sets the start position within the chromosome for this genomic region
     * Regions should always be defined relative to the direct strand, so that
     * the start position of a region is always smaller than the end position. 
     * 
     * @param start the start position for this region
     */
    public void setRegionStart(int start) {
        this.startPosition=start;
    }
    
    /**
     * Returns the start position of this genomic region within the chromosome
     * (represented by the smallest genomic coordinate (regionStart is equal or less than regionEnd))
     * 
     * @return start position
     */
    public int getRegionStart() {
        return startPosition;      
    }
    
    /**
     * Sets the end position within the chromosome for this genomic region
     * Regions should always be defined relative to the direct strand, so that
     * the start position of a region is always smaller than the end position. 
     * 
     * @param end the end position for this region
     */
    public void setRegionEnd(int end) {
        this.endPosition=end;
    }
    
    /**
     * Returns the end position of this genomic region within the chromosome
     * (represented by the largest genomic coordinate (regionStart is equal or less than regionEnd))
     * 
     * @return end position
     */
    public int getRegionEnd() {
        return endPosition;      
    }
    
    
//    /**
//     * Sets the position of the transcription start site (TSS) for the gene
//     * associated with this genomic region. The position of TSS should be 
//     * specified in genomic coordinates
//     * 
//     * @param position of TSS
//     */
//    public void setTSS(Integer TSS) {
//        this.TSS=TSS;
//    }
    
    /**
     * Gets the position of the transcription start site (TSS) for the gene
     * associated with this genomic region. The position of the TSS is given 
     * in genomic coordinates
     * 
     * @return position of TSS if specified, otherwise <code>null</code>
     */
    public Integer getTSS() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getTSS():null;       
    }
    
//    /**
//     * Sets the position of the transcription end site (TES) for the gene
//     * associated with this genomic region. The position of TES should be 
//     * specified in genomic coordinates
//     * 
//     * @param position of TSS
//     */
//    public void setTES(Integer TES) {
//        this.TES=TES;
//    }
    
    /**
     * Gets the position of the transcription end site (TES) for the gene
     * associated with this genomic region. The position of the TES is given 
     * in genomic coordinates
     * 
     * @return position of TSS if specified, otherwise <code>null</code>
     */
    public Integer getTES() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getTES():null;    
    }
        
    /**
     * Sets the strand orientation of the gene associated with this 
     * genomic region. This class provides two static constants that can
     * be used as values: GenomicSequenceRegion.DIRECT and GenomicSequenceRegion.REVERSE
     * This method has been deprecated since the orientation can now be set in the 
     * @param orientation The strand orientation of the associated gene
     */
//   @Deprecated
//    public void setStrandOrientation(int orientation) {
//        this.orientation=orientation;
//    }
    
    /**
     * Gets the strand orientation of the gene associated with this 
     * genomic region. This class provides two static constants that can
     * be used as values for comparison: FeatureSequenceData.DIRECT and FeatureSequenceData.REVERSE
     * 
     * @return the strand orientation of the associated gene 
     */
    public int getStrandOrientation() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getStrandOrientation():Sequence.DIRECT;
    }
    
    /**
     * Returns true if the gene associated with this genomic region is located on
     * the direct strand
     * 
     * @return true if associated gene is located on direct strand
     */
    public boolean isOnDirectStrand() {
        return (getStrandOrientation()==DIRECT);      
    }
    
    /**
     * Returns the Sequence object that this FeatureSequenceData object is based on (or NULL if it could not be found)
     * @return 
     */
    protected Sequence getSequenceObject() {
       return (Sequence)MotifLabEngine.getEngine().getDataItem(sequenceName,Sequence.class);        
    }
    
    /** 
     * Crops this sequence to a new segment. 
     * The new sequence location should be located within the confines of the old segment
     */    
    public abstract void cropTo(int relativeStart, int relativeEnd) throws ExecutionError;
    
    /**
     * Returns a String representation of the genomic region (location)
     * associated with this sequence on the form "chromosome:start-end"
     * 
     * @return a string specifying the region for this sequence
     */
    public String getRegionAsString() {
        return "chr"+chromosome+":"+startPosition+"-"+endPosition;
    }
    
    /**
     * Returns the value at the position specified as an offset from the 
     * start of the sequence (that is, pos=0 is the first position in the sequence)
     * 
     * @param position
     * @return an object depending on the type of data provided by this class
     */     
    public abstract Object getValueAtRelativePosition(int position);
    
    /**
     * Returns the value at the genomic position specified
     * 
     * @param chromosome     
     * @param position
     * @return an object depending on the type of data provided by this class
     */    
    
    public abstract Object getValueAtGenomicPosition(String chromosome, int position);

    /**
     * Returns the value at the genomic position within the chromosome of this gene
     * 
     * @param position
     * @return an object depending on the type of data provided by this class
     */     
    public abstract Object getValueAtGenomicPosition(int position);
    
    /**
     * Sets the parent dataset for this FeatureSequenceData object
     * @param parent
     */
    
    /**
     * Converts a relative position to a genomic position for this sequence
     * @return
     */
    public int getGenomicPositionFromRelative(int relative) {
        return startPosition+relative;
    }
    
    /**
     * Converts a genomic position to a relative position for this sequence
     * @return
     */
    public int getRelativePositionFromGenomic(int genomic) {
        return genomic-startPosition;
    }
    
    
    
    public abstract void setParent(FeatureDataset parent);
    
    
    /**
     * Returns the parent FeatureDataset for this sequence
     * @param parent
     */
    public FeatureDataset getParent() {
        return parent;
    }
    
    /** Returns a string describing the origin of this dataset (for instance a database or prediction program) */
    public String getDataSource() {
        if (parent==null) return "unknown_source";
        else return parent.getDataSource();
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        FeatureSequenceData data=(FeatureSequenceData)source;
        this.sequenceName=data.sequenceName;
        this.chromosome=data.chromosome;
        this.startPosition=data.startPosition;
        this.endPosition=data.endPosition;
        //MotifLabEngine.getEngine().logMessage("importData("+parent.getName()+":"+sequenceName+")["+System.identityHashCode(this)+"][Other:"+System.identityHashCode(source)+"]  "+chromosome+":"+startPosition+"-"+endPosition+"  ("+(endPosition-startPosition+1)+" bp)"); 
        //this.parent=parent; Keep the current parent! Do not change to the "stored" parent. Only import "flat data" not "family structure"
    }
    
    /** Outputs information about this sequence into the buffer */
    public abstract StringBuilder debug(StringBuilder builder);  
    
    private static final long serialVersionUID = -3983766859272820043L;    
    
}
