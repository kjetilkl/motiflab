/*
 
 
 */

package motiflab.engine.data;

import java.util.Arrays;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.util.CodonUsage;

/**
 * This class contains DNA data for a single sequence from a specific region in a genome
 * 
 * @author kjetikl
 */
public class DNASequenceData extends FeatureSequenceData {
    private char[] sequencedata; // the DNA sequence it self. The data should come from the direct strand so that sequencedata[0] corresponds to the smaller genomic coordinate
    private static String typedescription = "DNA sequence data";
    
    // the transient fields below are only used when sequence data is read from file (in FASTA format or similar).
    // In that case, the DNASequenceData object will be created _before_ the correponding Sequence object
    // The sequence attributes below are temporarily stored in this object before they are passed over to the Sequence object later on
    private transient Integer temporary_organism=null; 
    private transient String temporary_build=null;   
    private transient String temporary_genename=null;   
    private transient Integer temporary_TSS=null;
    private transient Integer temporary_TES=null;
    private transient int temporary_orientation=Sequence.DIRECT;    
    
    /**
     * Constructs a new DNASequenceData object from the supplied data
     * 
     * @param sequenceName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param sequencedata
     */
    public DNASequenceData(String sequenceName, String chromosome, int startPosition, int endPosition, char[] sequencedata){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         this.sequencedata=sequencedata;
    }
    
    /**
     * Constructs a new DNASequenceData based on the supplied Sequence
     * @param defaultbase A default base to fill the buffer with.
     * @param sequence
     */
    public DNASequenceData(Sequence sequence, char defaultbase){
         setSequenceName(sequence.getName());
         setChromosome(sequence.getChromosome());
         setRegionStart(sequence.getRegionStart());
         setRegionEnd(sequence.getRegionEnd());
         int size=endPosition-startPosition+1;
         this.sequencedata=new char[size];
         for (int i=0;i<size;i++) sequencedata[i]=defaultbase;
    } 
    /**
     * Constructs a new DNASequenceData based on the supplied Sequence
     * 
     * @param sequence
     */
    public DNASequenceData(Sequence sequence, char[] sequencedata){
         setSequenceName(sequence.getName());
         setChromosome(sequence.getChromosome());
         setRegionStart(sequence.getRegionStart());
         setRegionEnd(sequence.getRegionEnd());
         this.sequencedata=sequencedata;
    } 
    
    
    /**
     * Constructs a new DNASequenceData with an initial sequence consisting of only the given default base
     * 
     * @param sequenceName
     * @param geneName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param TSS
     * @param orientation
     * @param defaultbase 
     */
    public DNASequenceData(String sequenceName, String chromosome, int startPosition, int endPosition, char defaultbase){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         int size=endPosition-startPosition+1;
         if (size<0) size=0;
         this.sequencedata=new char[size];
         for (int i=0;i<size;i++) sequencedata[i]=defaultbase;
    }
    
    /**
     * Constructs a new DNASequenceData with an initial sequence constructed from the given Background Model
     * 
     * @param sequenceName
     * @param geneName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param TSS
     * @param orientation
     * @param randomgenerator 
     */
    public DNASequenceData(String sequenceName, String chromosome, int startPosition, int endPosition, BackgroundModel randomgenerator){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         int size=endPosition-startPosition+1;
         try {
             String prefix=randomgenerator.selectSuitablePrefix();
             sequencedata=randomgenerator.getNextBases(prefix, size);
         } catch (ExecutionError e) {
              this.sequencedata=new char[size];
              for (int i=0;i<size;i++) sequencedata[i]='N';
         }
    }

    @Override
    public Object getValue() {return sequencedata;}
    
    
     @Override
     public Object getValueInGenomicInterval(int start, int end) {
        if (end<getRegionStart() || start>getRegionEnd() || end<start) return null; // invalid interval that is fully or partially out of bounds
        if (start<getRegionStart()) start=getRegionStart();
        if (end>getRegionEnd()) end=getRegionEnd();
        int length=end-start+1;
        int relativeStart=getRelativePositionFromGenomic(start);
        char[] subset=new char[length];
        for (int i=0;i<length;i++) {
            subset[i]=sequencedata[relativeStart+i];
        }
        return subset;
    }
    
    /** Returns the dna sequence in the specified interval relative to the start if the parent sequence
     *  The sequence will be from the direct strand. If start is smaller than 0 or end is larger than
     *  the sequence length, the returned sequence will only be the segment intersecting with this sequence,
     *  so the returned interval could be shorter than the interval requested!
     */
    public Object getValueInInterval(int start, int end) {
        if (end<0 || start>=sequencedata.length || end<start) return null;
        if (start<0) start=0;
        if (end>=sequencedata.length) end=sequencedata.length-1;
        int length=end-start+1;
        char[] subset=new char[length];
        System.arraycopy(sequencedata, start, subset, 0, length);
        return subset;
    }    
    
    /** Returns TRUE if the direct strand subsequence starting at the specified position 
     *  matches the given oligo (case-insensitive)
     */
    public boolean isDirectMatchAtPosition(int pos, String oligo) {
        int length=oligo.length();
        int start=getRegionStart();
        if (pos<start || pos>getRegionEnd()-length+1) return false;
        for (int i=0;i<length;i++) {
            if (Character.toUpperCase(sequencedata[pos-start+i])!=oligo.charAt(i)) return false;
        }
        return true;
    }

    
    /**
     * Returns the DNA base at the specified position relative to the
     * start of the sequence (if position==0 it returns the first base in the 
     * sequence). If the position is outside the boundaries of the sequence it returns
     * null
     * 
     * @param position
     * @return the DNA base at the specified position, or null
     */
    @Override
    public Character getValueAtRelativePosition(int position) {
        if (position<0 || position>=sequencedata.length) return null;
        else return new Character(sequencedata[position]);
    }

   /**
     * Returns the DNA base at the specified genomic position.
     * If the position is outside the boundaries of the sequence it returns null.
     * 
     * @param chromosome
     * @param position
     * @return the DNA base at the specified position, or null
     */
    @Override
    public Character getValueAtGenomicPosition(String chromosome, int position) {
        if (!chromosome.equalsIgnoreCase(this.chromosome)) return null;
        if (position<this.startPosition || position>this.endPosition) return null;
        return new Character(sequencedata[position-this.startPosition]);
    }

   /**
     * Returns the DNA base at the specified genomic position.
     * If the position is outside the boundaries of the sequence it returns null.
    * 
     * @param position
     * @return the DNA base at the specified position, or null
     */
    @Override
    public Character getValueAtGenomicPosition(int position) {
        if (position<this.startPosition || position>this.endPosition) return null;
        return new Character(sequencedata[position-this.startPosition]);
    }

   /**
     * Sets the DNA base at the specified genomic position.
     * 
     * @param position The genomic position
     * @param base The new base
     * @return false if position is outside sequence bounds else true
     */       
    public boolean setValueAtGenomicPosition(int position, char base) {
        if (position<this.startPosition || position>this.endPosition) return false;
        sequencedata[position-this.startPosition]=base;
        return true;
    }
    
   /**
     * Sets the DNA base at the specified position relative to the start of the sequence.
     * 
     * @param position The relative position
     * @param base The new base 
     * @return false if position is outside sequence bounds else true
     */       
    public boolean setValueAtRelativePosition(int position, char base) {
        if (position<0 || position>=sequencedata.length) return false;
        sequencedata[position]=base;
        return true;
    }

   /**
     * Returns the whole sequence as a string (from direct strand)
    * 
     * @param position
     * @return the whole sequence  as a string
     */       
    public String getSequenceAsString() {
        return new String(sequencedata);
    }

    /**
     * Replaces the current sequence with a new buffer
     * 
     */
    public void setSequenceData(char[] sequence) throws ExecutionError {
        if (sequence!=null) {
            if (sequence.length!=getSize()) throw new ExecutionError("New sequence buffer size ("+sequence.length+" bp) does not match current sequence length ("+getSize()+" bp)");
        }
        this.sequencedata=sequence;
    }
    
     /**
     * Returns a deep copy of this DNASequenceData object
     */
    @Override
    public DNASequenceData clone() {
        char[] newvalues=new char[sequencedata.length];
        System.arraycopy(sequencedata, 0, newvalues, 0, sequencedata.length);
        DNASequenceData newdata=new DNASequenceData(sequenceName, chromosome, startPosition, endPosition, newvalues);
        newdata.parent=this.parent;
        return newdata;
    }      
    
    @Override
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.sequencedata=((DNASequenceData)source).sequencedata;
        //notifyListenersOfDataUpdate(); 
    }

    @Override
    public void setParent(FeatureDataset parent) {
        this.parent=parent;
    } 
    
    public static String getType() {return typedescription;}   
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }
    
    @Override
    public String getTypeDescription() {return typedescription;}   
    
    @Override
    public Integer getTSS() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getTSS():temporary_TSS;       
    }
    
    @Override
    public Integer getTES() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getTES():temporary_TES;       
    }   
    
    @Override
    public String getGeneName() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getGeneName():temporary_genename;       
    }     
    
    @Override
    public int getStrandOrientation() {
       Sequence seq=getSequenceObject(); 
       return (seq!=null)?seq.getStrandOrientation():temporary_orientation;       
    }      
    
    // The following get/set methods for "temporary" properties are used to store sequence attributes here that will later be passed on to a corresponding Sequence object
    // (which will be created _after_ this DNASequenceData object was created)
    public void setTemporaryBuild(String build) {this.temporary_build=build;}      
    public String getTemporaryBuild() {return temporary_build;}  
    
    public void setTemporaryOrganism(Integer organism) {this.temporary_organism=organism;}    
    public Integer getTemporaryOrganism() {return temporary_organism;}
    
    public void setTemporaryGeneName(String genename) {this.temporary_genename=genename;}       
    public void setTemporaryTSS(Integer tss) {this.temporary_TSS=tss;}      
    public void setTemporaryTES(Integer tes) {this.temporary_TES=tes;}     
    public void setTemporaryOrientation(Integer orientation) {this.temporary_orientation=orientation;}     


    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof DNASequenceData)) return false;
        if (!super.containsSameData(other)) return false;
        return (Arrays.equals(this.sequencedata,((DNASequenceData)other).sequencedata));
    }

    @Override  
    public void cropTo(int relativeStart, int relativeEnd) throws ExecutionError {
        int oldstart=getRegionStart();
        if (relativeStart<0 || relativeStart>=sequencedata.length || relativeEnd<0 || relativeEnd>=sequencedata.length || relativeEnd<relativeStart) throw new ExecutionError("Crop out of bounds");
        if (relativeStart==0 && relativeEnd==sequencedata.length-1) return;
        char[] newsequencedata=Arrays.copyOfRange(sequencedata, relativeStart, relativeEnd+1);
        sequencedata=newsequencedata;        
        setRegionStart(oldstart+relativeStart);         
        setRegionEnd(oldstart+relativeEnd);    
    }  
    
    /**
     * Returns a RegionSequenceData object filled with 3bp long amino acid regions
     * based on the codons found within this DNA sequence
     * @param frame A string specifying which of the six reading frames to use. The Valid options are: Direct-0,Direct-1,Direct-2,Reverse-0,Reverse-1,Reverse-2
     * @return 
     */
    public RegionSequenceData getCodonSequence(String frame) throws ExecutionError {
         Sequence sequence=(Sequence)MotifLabEngine.getEngine().getDataItem(getSequenceName(), Sequence.class);
         if (sequence==null) throw new ExecutionError("Missing sequence '"+getSequenceName()+"'");
         RegionSequenceData codonsequence=new RegionSequenceData(sequence);
         frame=frame.toLowerCase();
         boolean direct=true;
         int offset=0;
         if (frame.startsWith("direct")) direct=true;
         else if (frame.startsWith("reverse")) direct=false;
         else throw new ExecutionError("Unknown reading frame: "+frame);
         if (frame.endsWith("0")) offset=0;
         else if (frame.endsWith("1")) offset=1;
         else if (frame.endsWith("2")) offset=2;
         else throw new ExecutionError("Unknown reading frame: "+frame);
         
         int seqlength=sequencedata.length-offset; // only complete triplets starting from offset
         seqlength=seqlength-seqlength%3; // remove incomplete triplet at the end
         for (int i=0;i<seqlength;i+=3) {
            int start=i+offset;
            String codon=""+sequencedata[start]+sequencedata[start+1]+sequencedata[start+2];
            if (!direct) codon=MotifLabEngine.reverseSequence(codon);
            String aminoAcid=CodonUsage.getAminoAcidForCodon(codon);
            if (aminoAcid!=null) {
                Region region=new Region(codonsequence, start, start+2, aminoAcid, 1.0, Region.INDETERMINED);
                region.setSequence(codon);
                codonsequence.addRegion(region);
            }               
         }             

         return codonsequence;
    }
    
    /** Outputs information about this dataset into the buffer */
    public StringBuilder debug(StringBuilder builder) {
        if (builder==null) builder=new StringBuilder();
        builder.append(" - "+getName()+" : "+getRegionAsString()+", size="+getSize()+", buffersize="+((sequencedata==null)?0:sequencedata.length)+",   ["+System.identityHashCode(this)+"] buffer="+System.identityHashCode(sequencedata));
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
