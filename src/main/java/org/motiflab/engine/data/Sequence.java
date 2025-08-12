/*
 
 
 */

package org.motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.GOengine;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.GeneIdentifier;
import org.motiflab.engine.GeneIDResolver;
import org.motiflab.engine.GeneIDmapping;
import org.motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
/**
 * This class represents a "template" for a genomic region
 * 
 * @author kjetikl
 */
public final class Sequence extends Data implements BasicDataType {
    public static final int DIRECT  =  1;
    public static final int REVERSE = -1;
    
    protected String sequenceName=null;
    protected String associatedGeneName=null;
    protected String chromosome="?";
    protected int startPosition=0;  // genomic coordinate  (chromosome starts at position 1)
    protected int endPosition=0;    // genomic coordinate  (chromosome starts at position 1)
    protected Integer TSS=null; // Transcription Start Site  (genomic coordinate)
    protected Integer TES=null; // Transcription End Site  (genomic coordinate)
    protected int orientation=DIRECT;     
    protected int organism;
    protected String genomeBuild;
    private transient HashMap<String,Object> properties=null; // A list which stores key-value pairs for user-defined properties. These keys are always in UPPERCASE!. This field is transient so that sessions can be forward compatible but it is included in sessions by "manually serialization" in the writeObject() method 
    private transient int[] GOterms=null; // A list of GO-terms associated with this sequence. This field is transient so that sessions can be forward compatible but it is included in sessions by "manually serialization" in the writeObject() method 
   
    private static String typedescription="Sequence";   
    
    private static transient HashMap<String,Class> userdefinedpropertyClasses=null; // contains a lookup-table defining the class-type of each user-defined property
    private static final String[] reservedproperties=new String[]{"name","gene name","genename","chromosome","chr","start","end","genomic start","genomic end","TSS[0]-relative start","TSS[0]-relative end","TES[0]-relative start","TES[0]-relative end","TSS[1]-relative start","TSS[1]-relative end","TES[1]-relative start","TES[1]-relative end","TSS","TES","length","size","location","organism ID","organism","organism name","organism latin name","genome build","genome","build","orientation","strand","orientation string","strand string","orientation sign","strand sign","GO","gene ontology"};
    
    
    /**
     * Constructs a new Sequence object from the supplied data
     * 
     * @param sequenceName
     * @param organism
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param geneName
     * @param TSS
     * @param TES
     * @param orientation
     * @throws ExecutionError if the sequence name is invalid

     */
    public Sequence(String sequenceName, int organism, String genomebuild, String chromosome, int startPosition, int endPosition, String geneName, Integer TSS, Integer TES, int orientation) {
         setSequenceName(sequenceName);
         setOrganism(organism);
         setGeneName(geneName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         setStrandOrientation(orientation);
         setTSS(TSS);
         setTES(TES);
         setGenomeBuild(genomebuild);
    }
    
  

    @Override
    public String getName() {return getSequenceName();}

    @Override
    public Object getValue() {return this;} // should maybe change later
    
    @Override
    public String getValueAsParameterString() {
        StringBuilder parameter=new StringBuilder();
        parameter.append("NAME:");parameter.append(getName());parameter.append(";");
        parameter.append("LOCATION:");parameter.append(getRegionAsString());parameter.append(";");
        parameter.append("ORIENTATION:");parameter.append((orientation==Sequence.REVERSE)?"Reverse":"Direct");parameter.append(";");
        if (associatedGeneName!=null) {parameter.append("GENE:");parameter.append(associatedGeneName);parameter.append(";");}
        if (TSS!=null) {parameter.append("TSS:");parameter.append(TSS);parameter.append(";");}
        if (TES!=null) {parameter.append("TES:");parameter.append(TES);parameter.append(";");}       
        parameter.append("ORGANISM:");parameter.append(organism);parameter.append(";");
        parameter.append("BUILD:");parameter.append(genomeBuild);parameter.append(";");        
        if (GOterms!=null) {parameter.append("GO-TERMS:");parameter.append(MotifLabEngine.splice(getGOtermsWithoutPrefix(),","));parameter.append(";");}
        if (properties!=null) {
            for (String key:properties.keySet()) {
                String value=(String)getUserDefinedPropertyValueAsType(key,String.class);
                if (value!=null) {
                    parameter.append(key);
                    parameter.append(":");
                    parameter.append(value);
                    parameter.append(";");
                }
            }
        }
        return parameter.toString();
    }    
    

    /** Returns a HTML-formatted tooltip for this sequence */
    public String getToolTip(MotifLabEngine engine) {
        String orientationString;
        if (orientation==Sequence.REVERSE) orientationString="reverse"; else orientationString="direct";
        String build=(genomeBuild==null)?"Unknown":genomeBuild;
        StringBuilder tooltip=new StringBuilder(300);
        tooltip.append("<html><b>");
        tooltip.append(sequenceName);
        tooltip.append("</b>&nbsp;&nbsp;&nbsp;&nbsp;(");
        tooltip.append(getSpeciesName());
        tooltip.append(" : ");
        tooltip.append(build);
        tooltip.append(")<br>Sequence at ");
        tooltip.append(getRegionAsStringForPresentation());
        tooltip.append("&nbsp;&nbsp;&nbsp;&nbsp;(");
        tooltip.append(MotifLabEngine.groupDigitsInNumber(getSize()));
        tooltip.append(" bp)");
        String genename=getGeneName();
        if (genename==null) {
            tooltip.append("&nbsp;&nbsp;&nbsp;on ");
            tooltip.append(orientationString);
            tooltip.append(" strand");
        } 
        if (TSS!=null && genename!=null) {
            int relstart=0;
            int relend=0;
            if (orientation==Sequence.DIRECT) {
                relstart=getRegionStart()-TSS.intValue();
                relend=getRegionEnd()-TSS.intValue();
            } else {
                relstart=TSS.intValue()-getRegionEnd();
                relend=TSS.intValue()-getRegionStart();                   
            }
            if (relstart>=0) relstart++; // to account for direct transition from -1 to +1 at TSS
            if (relend>=0) relend++; // to account for direct transition from -1 to +1 at TSS
            tooltip.append("<br>located [");
            tooltip.append(relstart);
            tooltip.append(",");
            tooltip.append(relend);
            tooltip.append("] relative to gene <b>");
            tooltip.append(genename);
            tooltip.append("</b> with TSS at ");
            tooltip.append(MotifLabEngine.groupDigitsInNumber(TSS));
            tooltip.append(" on ");
            tooltip.append(orientationString);
            tooltip.append(" strand");            
        }
        tooltip.append("<br>");
        ArrayList<String> memberof=new ArrayList<String>();        
        ArrayList<Data> collections=engine.getAllDataItemsOfType(SequenceCollection.class);
        int collectionsCount=0;
        int partitionsCount=0;
        for (Data data:collections) {
            if (data==engine.getDefaultSequenceCollection()) continue;
            if (((SequenceCollection)data).contains(sequenceName)) {
                memberof.add(data.getName());
                collectionsCount++;
            }
        }
        collections=engine.getAllDataItemsOfType(SequencePartition.class);
        for (Data data:collections) {
            if (((SequencePartition)data).contains(sequenceName)) {
                String cluster=((SequencePartition)data).getClusterForSequence(sequenceName);
                memberof.add(data.getName()+" &rarr; "+cluster);
                partitionsCount++;
            }
        }        
        if (!memberof.isEmpty()) {
            Collections.sort(memberof);
            tooltip.append("<br>Included in "); 
            if (collectionsCount>0) {
                tooltip.append(collectionsCount);
                tooltip.append(" collection");
                if (collectionsCount!=1) tooltip.append("s");
            }
            if (partitionsCount>0) {
                if (collectionsCount>0) tooltip.append(" and ");
                tooltip.append(partitionsCount);
                tooltip.append(" partition");
                if (partitionsCount!=1) tooltip.append("s");
            }
            tooltip.append(":<br>");
            for (String member:memberof) {
                tooltip.append("&nbsp;&nbsp;&bull;&nbsp;");
                tooltip.append(member);
                tooltip.append("<br>");
            }  
        }
        ArrayList<Data> seqmaps=engine.getAllDataItemsOfType(SequenceNumericMap.class);
        seqmaps.addAll(engine.getAllDataItemsOfType(SequenceTextMap.class));
        Collections.sort(seqmaps);       
        if (!seqmaps.isEmpty()) {
           tooltip.append("<br>");
           tooltip.append("Values in Sequence Maps:<br>");
           for (Data map:seqmaps) {
               boolean hasValue=((DataMap)map).contains(sequenceName);
               Object value=((DataMap)map).getValue(sequenceName);
               tooltip.append("&nbsp;&nbsp;&bull;&nbsp;");
               tooltip.append(map.getName());
               tooltip.append(" = ");
               tooltip.append(value);
               if (!hasValue) tooltip.append("&nbsp;&nbsp;&nbsp;&nbsp;<i>(default)</i><br>");
               else tooltip.append("<br>");
           }
        }    
        tooltip.append("</html>");
        return tooltip.toString();
    }
    
    /**
     * Returns the size of this sequence (the number of bases spanned by the region)
     * 
     * @return size of sequence
     */
    public int getSize() {
        return endPosition-startPosition+1;
    }
    
    /**
     * Sets the name of the sequence
     * 
     * @param sequenceName a name for this sequence (this must be valid!)
     */
    public void setSequenceName(String sequenceName) {
        this.sequenceName=sequenceName;
    }
    
    @Override
    public void rename(String name) {
        setSequenceName(name);
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
    
    public static boolean isValidSequenceName(String name) {
        return (name.matches("[\\w\\.\\-\\+\\[\\]\\(\\)]+")); // allow . - + () and []
    }
    
    /**
     * Sets the name of the gene associated with this genomic region
     * 
     * @param geneName name of associated gene for this region
     */
    public void setGeneName(String geneName) {
        this.associatedGeneName=geneName;
    }
    
    /**
     * Returns the name of the gene associated with this genomic region
     * 
     * @return a name for this sequence
     */
    public String getGeneName() {
        return associatedGeneName;      
    }
 
    /**
     * Returns both the sequence name and the name of the gene associated with the sequence separated by a space
     * @return 
     */
    public String getSequenceAndGeneName() {
        return ((sequenceName!=null)?sequenceName:"")+" "+((associatedGeneName!=null)?associatedGeneName:"");
    }

    /**
     * Sets the genome build for this sequence. All coordinates should be interpreted
     * relative to the specified genome build
     * 
     * @param genomebuild A string specifying the genome build. Eg: 'hg18' or 'mm9'
     */
    public void setGenomeBuild(String genomebuild) {
        this.genomeBuild=genomebuild;
    }
    
    /**
     * Returns the genome build for this sequence. All coordinates should be interpreted
     * relative to this genome build
     * 
     * @return A string specifying the genome build. Eg: 'hg18' or 'mm9'
     */
    public String getGenomeBuild() {
        return genomeBuild;      
    }
        
    
    /**
     * Sets the name of the chromosome for this genomic region
     * 
     * @param chromosome chromosome name for this region
     */
    public void setChromosome(String chromosome) {
        if (chromosome.length()>3 && chromosome.substring(0,3).equalsIgnoreCase("chr")) chromosome=chromosome.substring(3); // remove 'chr' prefix
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
     * Returns the start position of the genomic segment spanned by this sequence within the chromosome
     * (represented by the smallest genomic coordinate (regionStart <= regionEnd))
     * 
     * @return start position
     */
    public int getRegionStart() {
        return startPosition;      
    }
    
    /**
     * Sets the end position within the chromosome for the genomic segment spanned by this sequence
     * Sequence regions should always be defined relative to the direct strand, so that
     * the start position of a sequence region is always smaller than the end position. 
     * 
     * @param end the end position for this region
     */
    public void setRegionEnd(int end) {
        this.endPosition=end;
    }
    
    /**
     * Returns the end position of the genomic segment spanned by this sequence within the chromosome
     * (represented by the largest genomic coordinate (regionStart <= regionEnd))
     * 
     * @return end position
     */
    public int getRegionEnd() {
        return endPosition;      
    }
    
    /**
     * Converts a position relative to the start of this sequence to a genomic position.
     * @relative The relative position within this sequence (starting at position 0)
     * @considerstrand If TRUE, the provided position will be considered as an offset to the relative start of this sequence (i.e. orientation specific)
     *                 If FALSE, the position will be relative to the genomic start of the sequence (direct strand)
     * @return
     */
    public int getGenomicPositionFromRelative(int relative, boolean considerstrand) {
        if (!considerstrand) return startPosition+relative;
        else {
            if (orientation==Sequence.DIRECT) return startPosition+relative;
            else return endPosition-relative;
        }
    }
    
    /**
     * Converts a genomic position to a position relative to the start of this sequence (starting at 0)
     * @param genomic A genomic position
     * @param considerstrand If TRUE, the returned relative position will be an offset to the relative start of this sequence (i.e. orientation specific)
     *                       If FALSE, the returned position will be relative to the genomic start of the sequence (direct strand)
     * @return The relative offset for genomic position within this sequence (starting at 0)
     */
    public int getRelativePositionFromGenomic(int genomic, boolean considerstrand) {
        if (!considerstrand) return genomic-startPosition;
        else {
            if (orientation==Sequence.DIRECT) return genomic-startPosition;
            else return endPosition-genomic;            
        }
    }
    
    /**
     * Returns a genomic position for a given position relative to a provided anchor position
     * @param position A position relative to the anchor. Negative positions will be "before" the anchor and positive positions "after".
     *                 If skip0 is TRUE, a relative position of 1 will return the anchor position and +1 will return the position after.
     *                 If skip0 is FALSE, a relative position of 1 will return the position after the anchor while position 0 will return the anchor.
     *                 A relative position of -1 will always return the position before the anchor (so negative positions will not be affected by skip0).
     * @param anchor A position in genomic coordinates that the relative position should be considered as  
     * @param skip0 If FALSE, the anchor position is considered as being at "position 0" with the immediate downstream position at +1 and the upstream position at -1
     *              If TRUE the anchor position is considered as being at "position 1" with the immediate downstream position at +2 and the upstream position at -1  (the coordinate system skips 0 and proceeds directly from -1 to +1)
     * @param considerstrand If TRUE, the provided position will be considered as an orientation specific offset to the anchor position
     *                       If FALSE, the position will be considered as an offset relative to the anchor position on the direct strand
     * @return  
     */
    public int getGenomicPositionFromAnchoredRelativePosition(int position, int anchor, boolean skip0, boolean considerstrand) {
        int strand=orientation;
        if (!considerstrand) strand=Sequence.DIRECT;
        if (strand==Sequence.DIRECT) {
            if (skip0 && position>0) position--;
            return anchor+position;
        } else { // reverse
            if (skip0 && position>0) position--;
            return anchor-position;
        }
    }
    
    /**
     * Sets the position of the transcription start site (TSS) for the gene
     * associated with this genomic region. The position of TSS should be 
     * specified in genomic coordinates
     * 
     * @param position of TSS
     */
    public void setTSS(Integer TSS) {
        this.TSS=TSS;
    }
    
    /**
     * Gets the position of the transcription start site (TSS) for the gene
     * associated with this genomic region. The position of the TSS is given 
     * in genomic coordinates
     * 
     * @return position of TSS if specified, otherwise <code>null</code>
     */
    public Integer getTSS() {
        return TSS;      
    }
    /**
     * Sets the position of the transcription end site (TES) for the gene
     * associated with this genomic region. The position of TES should be 
     * specified in genomic coordinates
     * 
     * @param position of TSS
     */
    
    
    public void setTES(Integer TES) {
        this.TES=TES;
    }
    
    /**
     * Gets the position of the transcription end site (TES) for the gene
     * associated with this genomic region. The position of the TES is given 
     * in genomic coordinates
     * 
     * @return position of TSS if specified, otherwise <code>null</code>
     */
    public Integer getTES() {
        return TES;      
    }
    
    /** Returns the start position of this sequence relative to TSS 
     *  or NULL if TSS is not defined for this sequence.
     *  The relative position is also orientation relative!
     */
    public Integer getTSSrelativeStart(boolean skip0) {
        if (TSS==null) return null;
        int start=startPosition;
        int end=endPosition;        
        start=(orientation==Sequence.DIRECT)?start-TSS:TSS-start;
        end=(orientation==Sequence.DIRECT)?end-TSS:TSS-end;
        if (orientation==Sequence.REVERSE) {int swap=start; start=end; end=swap;}
        if (skip0 && start>=0) start++;
        return start;
    }
    /** Returns the end position of this sequence relative to TSS 
     *  or NULL if TSS is not defined for this sequence.
     *  The relative position is also orientation relative!
     */
    public Integer getTSSrelativeEnd(boolean skip0) {
        if (TSS==null) return null;
        int start=startPosition;
        int end=endPosition;        
        start=(orientation==Sequence.DIRECT)?start-TSS:TSS-start;
        end=(orientation==Sequence.DIRECT)?end-TSS:TSS-end;
        if (orientation==Sequence.REVERSE) {int swap=start; start=end; end=swap;}
        if (skip0 && end>=0) end++;
        return end;
    }
    /** Returns the start position of this sequence relative to TES 
     *  or NULL if TES is not defined for this sequence.
     *  The relative position is also orientation relative!
     */
    public Integer getTESrelativeStart(boolean skip0) {
        if (TES==null) return null;
        int start=startPosition;
        int end=endPosition;        
        start=(orientation==Sequence.DIRECT)?start-TES:TES-start;
        end=(orientation==Sequence.DIRECT)?end-TES:TES-end;
        if (orientation==Sequence.REVERSE) {int swap=start; start=end; end=swap;}
        if (skip0 && start>=0) start++;
        return start;
    }
    /** Returns the end position of this sequence relative to TES 
     *  or NULL if TES is not defined for this sequence.
     *  The relative position is also orientation relative!
     */
    public Integer getTESrelativeEnd(boolean skip0) {
        if (TES==null) return null;
        int start=startPosition;
        int end=endPosition;        
        start=(orientation==Sequence.DIRECT)?start-TES:TES-start;
        end=(orientation==Sequence.DIRECT)?end-TES:TES-end;
        if (orientation==Sequence.REVERSE) {int swap=start; start=end; end=swap;}
        if (skip0 && end>=0) end++;
        return end;
    }    
    
    
    /**
     * Sets the strand orientation of the gene associated with this 
     * genomic region. This class provides two static constants that can
     * be used as values: GenomicSequenceRegion.DIRECT and GenomicSequenceRegion.REVERSE
     * 
     * @param orientation The strand orientation of the associated gene
     */
    public void setStrandOrientation(int orientation) {
        this.orientation=orientation;
    }
    
    /**
     * Gets the strand orientation of the gene associated with this 
     * genomic region. This class provides two static constants that can
     * be used as values for comparison: GenomicSequenceRegion.DIRECT and GenomicSequenceRegion.REVERSE
     * 
     * @return the strand orientation of the associated gene 
     */
    public int getStrandOrientation() {
        return orientation;      
    }
    
    /**
     * Returns true if the gene associated with this genomic region is located on
     * the direct strand
     * 
     * @return true if associated gene is located on direct strand
     */
    public boolean isOnDirectStrand() {
        return (orientation==DIRECT);      
    }
    

    /**
     * Returns a String representation of the genomic region (location)
     * associated with this sequence on the form "chromosome:start-end"
     * 
     * @return a string specifying the region for this sequence
     */
    public String getRegionAsString() {
        if (chromosome.length()<=2) return "chr"+chromosome+":"+startPosition+"-"+endPosition;
        else return chromosome+":"+startPosition+"-"+endPosition; // chromosome is probably not a "number" (or X/Y etc.)
    }
    
    /**
     * Returns a String representation of the genomic region (location)
     * associated with this sequence on the form "chromosome:start-end"
     * Unlike the related getRegionAsString() method, this will add commas to group digits in large numbers
     * @return a string specifying the region for this sequence
     */
    public String getRegionAsStringForPresentation() {
        if (chromosome.length()<=2) return "chr"+chromosome+":"+MotifLabEngine.groupDigitsInNumber(startPosition)+"-"+MotifLabEngine.groupDigitsInNumber(endPosition);
        else return chromosome+":"+startPosition+"-"+endPosition; // chromosome is probably not a "number" (or X/Y etc.)
    }    

    /**
     * Sets the originating organism for this sequence
     * 
     * @param organism the NCBI taxonomy ID associated with the organism
     */
    public void setOrganism(int organism) {
        this.organism=organism;
    }
    
   /**
    * Returns the common english name of the originating species for this sequence
    * 
    * @return the english name of the originating species
    */
    public String getSpeciesName() {
        return Organism.getCommonName(organism);
    }
    
    
   /**
    * Returns the latin name of the originating species for this sequence
    * 
    * @return the latin name of the originating species
    */
    public String getSpeciesLatinName() {
        return Organism.getLatinName(organism);
    }
    
   /**
    * Returns the NCBI organism taxonomy ID of the originating organism for this sequence
    * 
    * @return the NCBI organism taxonomy ID
    */
    public int getOrganism() {
        return organism;
    }    
    
    
    @Override
    public void importData(Data source) {
        this.sequenceName=((Sequence)source).sequenceName;
        this.associatedGeneName=((Sequence)source).associatedGeneName;
        this.chromosome=((Sequence)source).chromosome;
        this.startPosition=((Sequence)source).startPosition;
        this.endPosition=((Sequence)source).endPosition;
        this.TSS=((Sequence)source).TSS;
        this.TES=((Sequence)source).TES;
        this.orientation=((Sequence)source).orientation;
        this.genomeBuild=((Sequence)source).genomeBuild;        
        this.organism=((Sequence)source).organism; 
        this.properties=((Sequence)source).properties;
        this.GOterms=((Sequence)source).GOterms;    
        //notifyListenersOfDataUpdate(); 
    }

    @Override
    public Sequence clone() {
        Sequence seq=new Sequence(sequenceName, organism, genomeBuild, chromosome, startPosition, endPosition, associatedGeneName, TSS, TES, orientation);
        if (properties!=null) seq.properties=(HashMap<String, Object>)properties.clone();
        seq.GOterms=(this.GOterms==null)?null:(int[])this.GOterms.clone();
        return seq;
    }

    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof Sequence)) return false;
        Sequence source =(Sequence)other;
        if (!this.sequenceName.equals(source.sequenceName)) return false;
        if (!this.associatedGeneName.equals(source.associatedGeneName)) return false;
        if (!this.chromosome.equals(source.chromosome)) return false;
        if (this.startPosition!=source.startPosition) return false;
        if (this.endPosition!=source.endPosition) return false;
        if (this.orientation!=source.orientation) return false;
        if (this.organism!=source.organism) return false; 
        if (this.genomeBuild==null && source.genomeBuild!=null) return false;
        if (this.genomeBuild!=null && source.genomeBuild==null) return false;
        if (this.genomeBuild!=null && source.genomeBuild!=null && !this.genomeBuild.equals(source.genomeBuild)) return false;        
        if (this.TSS==null && source.TSS!=null) return false;
        if (this.TSS!=null && source.TSS==null) return false;
        if (this.TSS!=null && source.TSS!=null && !this.TSS.equals(source.TSS)) return false;
        if (this.TES==null && source.TES!=null) return false;
        if (this.TES!=null && source.TES==null) return false;
        if (this.TES!=null && source.TES!=null && !this.TES.equals(source.TES)) return false;
        if ((source.properties==null && this.properties!=null) || (source.properties!=null && this.properties==null) ||  (source.properties!=null && this.properties!=null && !this.properties.equals(source.properties))) return false;       
        if ((source.GOterms==null && this.GOterms!=null) || (source.GOterms!=null && this.GOterms==null) ||  (source.GOterms!=null && this.GOterms!=null && !MotifLabEngine.listcompare(this.GOterms,source.GOterms))) return false;
        return true;
    }
    
       

    public static String getType() {return typedescription;}    

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    
    /** Returns a list of GO terms associated with this sequence on the form "GO:nnnnnnn" (7 digits)*/
    public ArrayList<String> getGOterms() {
        if (this.GOterms==null) return new ArrayList<String>(0);
        ArrayList<String> terms=new ArrayList<String>(this.GOterms.length);
        for (int i=0;i<GOterms.length;i++) {
            int value=GOterms[i];
            terms.add("GO:"+String.format("%07d", value));
        }
        return terms;
    }
    
    /** Returns a list of GO terms associated with this sequence. This is similar to the
        getGOterms method except that the returned only consist of 7-digit codes that
        do not start with the "GO:" prefix 
     */
    public ArrayList<String> getGOtermsWithoutPrefix() {
        if (this.GOterms==null) return new ArrayList<String>(0);
        ArrayList<String> terms=new ArrayList<String>(this.GOterms.length);
        for (int i=0;i<GOterms.length;i++) {
            int value=GOterms[i];
            terms.add(String.format("%07d", value));
        }
        return terms;
    }     
    
    public void setGOterms(int[] terms) {
        if (terms.length==0) this.GOterms=null;
        else this.GOterms=MotifLabEngine.removeDuplicates(terms);       
    }    
    
    public void setGOterms(Collection<String> terms) throws ParseError {
        String[] array=new String[terms.size()];
        array=terms.toArray(array);
        setGOterms(array);
    }
    
    /** Sets the GO terms of this motif to the provided strings. 
     *  Throws ParseError if one of the GO-terms is invalid 
     *  Note that the GO-terms list should not contain duplicates
     */
    public void setGOterms(String[] terms) throws ParseError {
        if (terms==null || terms.length==0) {this.GOterms=null; return;}
        for (int i=0;i<terms.length;i++) { // preprocess
            if (terms[i]==null || terms[i].trim().isEmpty()) continue;
            if (terms[i].startsWith("GO:") || terms[i].startsWith("go:")) terms[i]=terms[i].substring(3);
        }
        terms=MotifLabEngine.flattenCommaSeparatedSublists(terms);        
        terms=MotifLabEngine.removeDuplicates(terms,true);         
        this.GOterms=new int[terms.length];   
        for (int i=0;i<terms.length;i++) {
            String term=terms[i];          
            try {
                int value=Integer.parseInt(term);
                if (value<0 || value>9999999) throw new NumberFormatException(); // GO terms have 7 digits
                this.GOterms[i]=value;
            } catch (NumberFormatException e) {
                throw new ParseError("Not a valid GO term: "+term);
            }
        }
    }   
    
    public boolean hasGOterm(String term) {
        if (this.GOterms==null || this.GOterms.length==0) return false;
        if (term.startsWith("GO:") || term.startsWith("go:")) term=term.substring(3); // optional prefix
        try {
            int value=Integer.parseInt(term);
            if (value<=0 || value>9999999) return false;
            for (int t:this.GOterms) {
                if (t==value) return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }       
    }
    
    public boolean hasGOterm(int term) {
        if (this.GOterms==null || this.GOterms.length==0) return false;
        for (int t:this.GOterms) {
            if (t==term) return true;
        }
        return false;
    }    
    
    public boolean hasAnyGOterm(String[] terms) {
        for (String term:terms) {
            if (hasGOterm(term)) return true;
        }
        return false;
    }    
    
    /** Returns the names of all properties that can be obtained from Sequence objects */
    public static String[] getAllStandardProperties(MotifLabEngine engine) {
        return new String[] {"gene name","chromosome","genomic start","genomic end","TSS[0]-relative start","TSS[0]-relative end","TES[0]-relative start","TES[0]-relative end","TSS[1]-relative start","TSS[1]-relative end","TES[1]-relative start","TES[1]-relative end","TSS","TES","length","location","name","organism ID","organism name","organism latin name","genome build","orientation","orientation string","orientation sign","GO"};
    }
    
    public static String[] getAllEditableProperties(MotifLabEngine engine) {
        String[] editable=new String[]{"GO","genome build","gene name","TSS","TES","orientation"}; // this list with editable standard properties should be coordinated with those returning TRUE in setPropertyValue()
        String[] userdefined=getAllUserDefinedProperties(engine);
        String[] all=new String[editable.length+userdefined.length];
        System.arraycopy(editable, 0, all, 0, editable.length);
        System.arraycopy(userdefined, 0, all, editable.length, userdefined.length);
        return all;
    }    
    
        /**
     * Returns a list of all motif properties, both standard and user-defined
     * @param includeDerived
     * @param engine
     * @return 
     */
    public static String[] getAllProperties(MotifLabEngine engine) {
        String[] standard=getAllStandardProperties(engine);
        String[] userdefined=getAllUserDefinedProperties(engine);
        String[] all=new String[standard.length+userdefined.length];
        System.arraycopy(standard, 0, all, 0, standard.length);
        System.arraycopy(userdefined, 0, all, standard.length, userdefined.length);
        return all;
    }  

    /** Returns the names of all numeric properties that can be obtained from Sequence objects */
    public static String[] getNumericProperties(MotifLabEngine engine) {
        ArrayList<String> numericProps=new ArrayList<String>();
        String[] props=Sequence.getAllProperties(engine);
        for (String prop:props) {
            Class propclass=Sequence.getPropertyClass(prop,engine);
            if (propclass!=null && Number.class.isAssignableFrom(propclass)) numericProps.add(prop);
        }
        String[] result=new String[numericProps.size()];
        return numericProps.toArray(result);
    }
    
    /**
     * Returns the type-class for the given property
     * or NULL if the property is not recognized
     * @param propertyName
     * @return 
     */
    public static Class getPropertyClass(String propertyName, MotifLabEngine engine) {
             if (propertyName.equalsIgnoreCase("name")) return String.class;
        else if (propertyName.equalsIgnoreCase("location")) return String.class;
        else if (propertyName.equalsIgnoreCase("gene name") || propertyName.equalsIgnoreCase("genename") || propertyName.equalsIgnoreCase("gene")) return String.class;
        else if (propertyName.equalsIgnoreCase("chromosome") || propertyName.equalsIgnoreCase("chr")) return String.class;
        else if (propertyName.equalsIgnoreCase("chromosome string") || propertyName.equalsIgnoreCase("chr string")) return String.class;
        else if (propertyName.equalsIgnoreCase("genomic start")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("genomic start[0]")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("genomic end")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("genomic end[0]")) return Integer.class;        
        else if (propertyName.equalsIgnoreCase("TSS[0]-relative start")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TSS[0]-relative end")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TES[0]-relative start")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TES[0]-relative end")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TSS[1]-relative start")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TSS[1]-relative end")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TES[1]-relative start")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TES[1]-relative end")) return Integer.class;        
        else if (propertyName.equalsIgnoreCase("TSS")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TES")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TSS[0]")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("TES[0]")) return Integer.class;        
        else if (propertyName.equalsIgnoreCase("orientation") || propertyName.equalsIgnoreCase("strand")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("orientation string") || propertyName.equalsIgnoreCase("strand string")) return String.class;
        else if (propertyName.equalsIgnoreCase("orientation sign") || propertyName.equalsIgnoreCase("strand sign")) return String.class;
        else if (propertyName.equalsIgnoreCase("length") || propertyName.equalsIgnoreCase("size")) return Integer.class;   
        else if (propertyName.equalsIgnoreCase("organism ID")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("organism name")) return String.class;
        else if (propertyName.equalsIgnoreCase("organism latin name")) return String.class;
        else if (propertyName.equalsIgnoreCase("genome build") || propertyName.equalsIgnoreCase("genome") || propertyName.equalsIgnoreCase("build")) return String.class;
        else if (propertyName.equalsIgnoreCase("GO") || propertyName.equalsIgnoreCase("gene ontology")) return ArrayList.class;        
        else return Sequence.getClassForUserDefinedProperty(propertyName, engine);            
    }      
 
    /** Returns a value for the sequence property with the given name 
     *  @return an Object representing the value, this can be a 
     *  String, Boolean, Double, Integer depending on the property
     *  @throws ExecutionError If the property is not recognized 
     */
    @Override
    public Object getPropertyValue(String propertyName, MotifLabEngine engine) throws ExecutionError {
             if (propertyName.equalsIgnoreCase("name") || propertyName.equalsIgnoreCase("sequencename") || propertyName.equalsIgnoreCase("sequence name")) return getName();
        else if (propertyName.equalsIgnoreCase("gene name") || propertyName.equalsIgnoreCase("genename") || propertyName.equalsIgnoreCase("gene")) return getGeneName();
        else if (propertyName.equalsIgnoreCase("location")) return getRegionAsString();
        else if (propertyName.equalsIgnoreCase("chromosome") || propertyName.equalsIgnoreCase("chr")) return getChromosome();
        else if (propertyName.equalsIgnoreCase("chromosome string") || propertyName.equalsIgnoreCase("chr string")) return "chr"+getChromosome();        
        else if (propertyName.equalsIgnoreCase("genomic start")) return getRegionStart();
        else if (propertyName.equalsIgnoreCase("genomic start[0]")) return getRegionStart()-1; // 0-indexed
        else if (propertyName.equalsIgnoreCase("genomic end")) return getRegionEnd();
        else if (propertyName.equalsIgnoreCase("genomic end[0]")) return getRegionEnd()-1; // 0-indexed
        else if (propertyName.equalsIgnoreCase("TSS[0]-relative start")) return getTSSrelativeStart(false);
        else if (propertyName.equalsIgnoreCase("TSS[0]-relative end")) return getTSSrelativeEnd(false);
        else if (propertyName.equalsIgnoreCase("TES[0]-relative start")) return getTESrelativeStart(false);
        else if (propertyName.equalsIgnoreCase("TES[0]-relative end")) return getTESrelativeEnd(false);
        else if (propertyName.equalsIgnoreCase("TSS[1]-relative start")) return getTSSrelativeStart(true);
        else if (propertyName.equalsIgnoreCase("TSS[1]-relative end")) return getTSSrelativeEnd(true);
        else if (propertyName.equalsIgnoreCase("TES[1]-relative start")) return getTESrelativeStart(true);
        else if (propertyName.equalsIgnoreCase("TES[1]-relative end")) return getTESrelativeEnd(true);        
        else if (propertyName.equalsIgnoreCase("TSS")) return getTSS();
        else if (propertyName.equalsIgnoreCase("TES")) return getTES();
        else if (propertyName.equalsIgnoreCase("TSS[0]")) return (getTSS()==null)?null:(getTSS()-1);
        else if (propertyName.equalsIgnoreCase("TES[0]")) return (getTES()==null)?null:(getTES()-1);      
        else if (propertyName.equalsIgnoreCase("length") || propertyName.equalsIgnoreCase("size")) return getSize();       
        else if (propertyName.equalsIgnoreCase("organism ID")) return getOrganism();
        else if (propertyName.equalsIgnoreCase("organism name")) return getSpeciesName();
        else if (propertyName.equalsIgnoreCase("organism latin name")) return getSpeciesLatinName();
        else if (propertyName.equalsIgnoreCase("genome build") || propertyName.equalsIgnoreCase("genome") || propertyName.equalsIgnoreCase("build")) return getGenomeBuild();
        else if (propertyName.equalsIgnoreCase("orientation") || propertyName.equalsIgnoreCase("strand")) return getStrandOrientation();
        else if (propertyName.equalsIgnoreCase("orientation string") || propertyName.equalsIgnoreCase("strand string")) {
            if (orientation==Sequence.DIRECT) return "Direct";
            else if (orientation==Sequence.REVERSE) return "Reverse";
            else return "Undetermined";
        }
        else if (propertyName.equalsIgnoreCase("orientation sign") || propertyName.equalsIgnoreCase("strand sign")) {
            if (orientation==Sequence.DIRECT) return "+";
            else if (orientation==Sequence.REVERSE) return "-";
            else return ".";
        }
        else if (propertyName.equalsIgnoreCase("GO") || propertyName.equalsIgnoreCase("gene ontology")) return getGOterms();               
        else {
            if (Sequence.getPropertyClass(propertyName,engine)==null) throw new ExecutionError("Unknown sequence property: "+propertyName);
            if (properties!=null && properties.containsKey(propertyName)) return properties.get(propertyName);
            else return null; // user-defined property exists, but this motif does not have a value for it
        }     
    }
    
    /**
     * Assigns the given property the given value.
     * @param propertyName
     * @param value
     * @return FALSE if the property can not be explicitly set, else TRUE
     * @throws ExecutionError if the given value is not of the appropriate type
     */
    @Override
    public boolean setPropertyValue(String propertyName, Object value) throws ExecutionError {
        try {
                 if (propertyName.equalsIgnoreCase("name")) return false;
            else if (propertyName.equalsIgnoreCase("location")) return false;
            else if (propertyName.equalsIgnoreCase("chromosome") || propertyName.equalsIgnoreCase("chr")) return false;
            else if (propertyName.equalsIgnoreCase("genomic start")) return false;
            else if (propertyName.equalsIgnoreCase("genomic end")) return false;
            else if (propertyName.equalsIgnoreCase("TSS[0]-relative start")) return false;
            else if (propertyName.equalsIgnoreCase("TSS[0]-relative end")) return false;
            else if (propertyName.equalsIgnoreCase("TES[0]-relative start")) return false;
            else if (propertyName.equalsIgnoreCase("TES[0]-relative end")) return false;
            else if (propertyName.equalsIgnoreCase("TSS[1]-relative start")) return false;
            else if (propertyName.equalsIgnoreCase("TSS[1]-relative end")) return false;
            else if (propertyName.equalsIgnoreCase("TES[1]-relative start")) return false;
            else if (propertyName.equalsIgnoreCase("TES[1]-relative end")) return false;        
            else if (propertyName.equalsIgnoreCase("length") || propertyName.equalsIgnoreCase("size")) return false; 
            else if (propertyName.equalsIgnoreCase("organism ID") || propertyName.equalsIgnoreCase("organism")) return false;
            else if (propertyName.equalsIgnoreCase("organism name")) return false;
            else if (propertyName.equalsIgnoreCase("organism latin name")) return false;
            else if (propertyName.equalsIgnoreCase("orientation string") || propertyName.equalsIgnoreCase("strand string")) return false;
            else if (propertyName.equalsIgnoreCase("orientation sign") || propertyName.equalsIgnoreCase("strand sign")) return false;
            else if (propertyName.equalsIgnoreCase("orientation") || propertyName.equalsIgnoreCase("strand")) {
                if (value!=null && value.toString().equals("-1") || value.toString().equals("-") || value.toString().equalsIgnoreCase("reverse")) setStrandOrientation(Sequence.REVERSE);
                else setStrandOrientation(Sequence.DIRECT);
            } 
            else if (propertyName.equalsIgnoreCase("gene name") || propertyName.equalsIgnoreCase("genename") || propertyName.equalsIgnoreCase("gene")) {
                if (value!=null) setGeneName(value.toString());
            }           
            else if (propertyName.equalsIgnoreCase("genome build") || propertyName.equalsIgnoreCase("genome") || propertyName.equalsIgnoreCase("build")) {
                String newbuild=(value!=null)?value.toString():"unknown";
                int neworganism=Organism.getOrganismForGenomeBuild(newbuild);
                 if (neworganism>0) {
                     setGenomeBuild(newbuild);
                     setOrganism(neworganism);
                 }
            }      
            else if (propertyName.equalsIgnoreCase("TSS")) {
                if (value!=null) {
                    try {setTSS((int)Double.parseDouble(value.toString()));}
                    catch (NumberFormatException e) {}
                } else setTSS(null);
            }
            else if (propertyName.equalsIgnoreCase("TES")) {
                if (value!=null) {
                    try {setTES((int)Double.parseDouble(value.toString()));}
                    catch (NumberFormatException e) {}
                } else setTES(null);
            }            
            else if (propertyName.equalsIgnoreCase("GO") || propertyName.equalsIgnoreCase("gene ontology")) {
                if (value instanceof String) setGOterms(((String)value).trim().split("\\s*,\\s*"));
                else if (value instanceof String[]) setGOterms((String[])value);
                else if (value instanceof ArrayList) {
                    String[] list=new String[((ArrayList<String>)value).size()];
                    list=((ArrayList<String>)value).toArray(list);
                    setGOterms(list);
                }
                else throw new ClassCastException();                 
            }                      
            else setUserDefinedPropertyValue(propertyName, value);          
        } catch(Exception e) {    
            if (e instanceof ParseError) throw new ExecutionError(e.getMessage());
            throw new ExecutionError("Unable to set property '"+propertyName+"' to value '"+((value!=null)?value.toString():"")+"'");
        }
        return true;
    }      
    
    
    /** Returns a property CLASS for the user-defined sequence-property with the given name
     *  or NULL if no property with the given name has been defined for any Sequence.
     *  This method works by dynamically querying all registered motifs to see
     *  what user-defined properties they have and what classes they are.
     *  Note that individual Sequences might have different Object (types) stored for the
     *  same property. e.g. One Sequence might have a value of "7" for the property "ID" 
     *  which is stored as a Double while a different Sequence might have the value "H8" 
     *  for the same "ID" property which is stored as a a String. 
     *  This method will return the 'least common class denominator'. 
     *  Which is to say that if all the values (across all sequences) for the same property
     *  are the same, this class will be returned else String.class will be returned
     *  @param propertyname (case-sensitive)
     */
    public static Class getClassForUserDefinedProperty(String propertyName, MotifLabEngine engine) {
        if (userdefinedpropertyClasses!=null && userdefinedpropertyClasses.containsKey(propertyName)) return userdefinedpropertyClasses.get(propertyName); // cached entries
        Class type=getClassForUserDefinedPropertyDynamically(propertyName, engine);
        if (type!=null) Sequence.setUserDefinedPropertyClass(propertyName,type,true); // cache the class in this lookup-table
        return type;       
    }    
    
    private static Class getClassForUserDefinedPropertyDynamically(String propertyName, MotifLabEngine engine) {
        if (engine==null) return null;
        // try to determine the type dynamically by querying all Motifs
        Class firstclass=null;
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data sequence:sequences) {
            Object value=((Sequence)sequence).getUserDefinedPropertyValue(propertyName);
            if (value==null) continue;
            else if (firstclass==null) firstclass=value.getClass();
            else if (firstclass.equals(Integer.class) && value.getClass().equals(Double.class)) firstclass=Double.class; // return Double if values are a mix of Integers and Doubles
            else if (firstclass.equals(Double.class) && value.getClass().equals(Integer.class)) continue; // return Double if values are a mix of Integers and Doubles
            else if (firstclass.equals(ArrayList.class) || value.getClass().equals(ArrayList.class)) return ArrayList.class; // return ArrayList if one of the values is an ArrayList
            else if (!value.getClass().equals(firstclass)) return String.class; // Not compatible
        }
        return firstclass;         
    }

    /** Returns a set of names for user-defined properties that are explicitly 
     *  defined for this sequence object or NULL if no such properties are defined
     */
    public Set<String> getUserDefinedProperties() {
        if (properties!=null) return properties.keySet();
        else return null;
    }
    
    /**
     * Returns the value of a user-defined property for this sequence, or NULL if no property with the given
     * name has been explicitly defined for this motif
     * @param propertyName (case-sensitive)
     * @return
     */
    public Object getUserDefinedPropertyValue(String propertyName) {
         if (properties!=null) return properties.get(propertyName);
         else return null;
    }    
    
    /** Returns the specified user-defined property as an object of the given class
     *  if the property is defined for this sequence and can be "converted" into an object
     *  of the given class, or NULL if the property is not defined or can not be converted
     *  All properties can be converted to String.class if defined
     */
    public Object getUserDefinedPropertyValueAsType(String propertyName, Class type) {
        if (properties==null) return null;
        Object value=properties.get(propertyName);
        return convertPropertyValueToType(value, type);
    }    
    
    public static Object convertPropertyValueToType(Object value, Class type) {
        if (value==null || (value instanceof String && ((String)value).trim().isEmpty())) return null;
        if (value.getClass().equals(type)) return value; // no conversion necessary
        if (type.equals(ArrayList.class)) { // convert to list
            ArrayList<String> list=new ArrayList<String>(1);
            list.add(value.toString());
            return list;
        }
        if (type.equals(Double.class)) {
            if (value instanceof Integer) return new Double(((Integer)value));
            else return null;
        }
        if (type.equals(Integer.class)) {
            if (value instanceof Double) return new Integer(((Double)value).intValue());
            else return null;
        }        
        if (type.equals(String.class)) {
            if (value instanceof ArrayList) {
                return MotifLabEngine.splice((ArrayList)value,",");
            } else return value.toString(); 
        } 
        return null; // no conversion possible (apparently)       
    }
    
    /**
     * Set the value of a user-defined property for this sequence
     * @param propertyName (case-sensitive)
     * @param value should be a Double, Integer, Boolean,  String or ArrayList (if NULL the property will be removed)
     * @return
     */
    public void setUserDefinedPropertyValue(String propertyName, Object value) {
         if (properties==null) properties=new HashMap<String, Object>();
         if (value==null || (value instanceof String && ((String)value).trim().isEmpty())) properties.remove(propertyName); // remove binding if it is empty
         else {
             if (!(value instanceof Double || value instanceof Integer || value instanceof Boolean || value instanceof List || value instanceof String)) value=value.toString(); // convert to String just in case
             properties.put(propertyName, value);
             Sequence.setUserDefinedPropertyClass(propertyName, value.getClass(), false);
         }
    }
    
    /**
     * Sets the class of the user-defined property in the lookup-table
     * @param propertyName
     * @param type
     * @param replaceCurrent 
     * @return Returns the new class for this property (this could be different from the argument type)
     */
    public static Class setUserDefinedPropertyClass(String propertyName, Class type, boolean replaceCurrent) {
        if (userdefinedpropertyClasses==null) userdefinedpropertyClasses=new HashMap<String, Class>();
        if (type!=null && !(type==Double.class || type==Integer.class || type==Boolean.class || List.class.isAssignableFrom(type) || type==String.class)) type=String.class; // just in case
        if (replaceCurrent || !userdefinedpropertyClasses.containsKey(propertyName)) userdefinedpropertyClasses.put(propertyName, type);
        else if (userdefinedpropertyClasses.get(propertyName)!=type) { // current class is different from new class. Try to coerce
              Class oldclass=userdefinedpropertyClasses.get(propertyName);
              if ((oldclass==Double.class && type==Integer.class) || (type==Double.class && oldclass==Integer.class)) userdefinedpropertyClasses.put(propertyName, Double.class);
              else if (List.class.isAssignableFrom(oldclass) || (type!=null && List.class.isAssignableFrom(type))) userdefinedpropertyClasses.put(propertyName, ArrayList.class);
              else userdefinedpropertyClasses.put(propertyName, String.class);
        }
        return userdefinedpropertyClasses.get(propertyName);
    }
    
    /**
     * Removes all mappings for
     */
    public static void clearUserDefinedPropertyClassesLookupTable() {
        if (userdefinedpropertyClasses!=null) userdefinedpropertyClasses.clear();
    }    
    
    /** updates the lookup table for all userdefined properties defined for this dataitem
     *  This could perhaps be more efficient?
     */
    public static void updateUserdefinedPropertiesLookupTable(Sequence dataitem, MotifLabEngine engine) {
        Set<String> props=dataitem.getUserDefinedProperties();
        if (props==null || props.isEmpty()) return;
        for (String prop:props) {
            Class commontype=getClassForUserDefinedPropertyDynamically(prop, engine);
            setUserDefinedPropertyClass(prop,commontype,true);
        }
    }
    
    /**
     * Removes the value of a user-defined property from this motif
     * @param propertyName (case-sensitive)
     * @return
     */
    public void removeUserDefinedPropertyValue(String propertyName) {
         if (properties!=null) properties.remove(propertyName);
    }    
    
    /** Returns an object corresponding to the valuestring.
     *  If the string is NULL or empty a value of NULL will be returned
     *  If the string can be parsed as an integer an Integer will be returned
     *  else If the string can be parsed as a double a Double will be returned
     *  If the string equals TRUE, FALSE, YES or NO (case-insensitive) a Boolean will be returned
     *  If the string contains commas the string will be split and returned as an ArrayList
     *  else a String will be returned (same as input)
     */
    public static Object getObjectForPropertyValueString(String valuestring) {
             if (valuestring==null || valuestring.trim().isEmpty()) return null;
        else if (valuestring.equalsIgnoreCase("TRUE") || valuestring.equalsIgnoreCase("YES")) return Boolean.TRUE;
        else if (valuestring.equalsIgnoreCase("FALSE") || valuestring.equalsIgnoreCase("NO")) return Boolean.FALSE;
        else if (valuestring.contains(",")) {
           String[] splitstring=valuestring.split("\\s*,\\s*");
           ArrayList<String> splitarray=new ArrayList<String>(splitstring.length);
           for (String s:splitstring) {
               if (!s.isEmpty()) splitarray.add(s);
           }
           return splitarray;
        }
        else {
            try {
                int integerValue=Integer.parseInt(valuestring);
                return new Integer(integerValue);

            } catch (NumberFormatException e) {
                  try {
                    double doublevalue=Double.parseDouble(valuestring);
                    return new Double(doublevalue);
                } catch(NumberFormatException ex) {
                    return valuestring; // default: return the same string
                }
            }
        }
    }


    /** Returns TRUE if the given key can be used for user-defined properties */
    public static boolean isValidUserDefinedPropertyKey(String key) {
        if (key==null || key.trim().isEmpty()) return false;
        //if (engine.isReservedWord(key)) return false;
        if (!key.matches("^[a-zA-Z0-9_\\-\\s]+$")) return false; // spaces, underscores and hyphens are allowed
        for (String reserved:reservedproperties) {
            if (key.equalsIgnoreCase(reserved)) return false;
        }
        return true;
    }    
    
    /** Returns a list of names for all user-defined properties across all Sequences.
     *  Note that the names of properties are case-sensitive.
     *  This method works by dynamically querying all registered sequences to see
     *  what user-defined properties they have
     */
    public static String[] getAllUserDefinedProperties(MotifLabEngine engine) {
        HashSet<String> propertyNamesSet=new HashSet<String>();
        String[] propertynames=new String[propertyNamesSet.size()];
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data sequence:sequences) {
            Set<String> sequenceprops=((Sequence)sequence).getUserDefinedProperties();
            if (sequenceprops!=null) propertyNamesSet.addAll(sequenceprops);
        }
        return propertyNamesSet.toArray(propertynames);
    }    
    
// ============================ METHODS BELOW THIS LINE ARE FOR PARSING SEQUENCES FROM FILES OR PARAMETERS =================================================================================    

    private static final int GENE_ID_FIELDS_COUNT=6;
    private static final int MANUAL_ENTRY_FIELDS_COUNT_8=8;
    private static final int MANUAL_ENTRY_FIELDS_COUNT_10=10;
    private static final int MANUAL_ENTRY_FIELDS_COUNT_4=4;    

    /** Creates and returns a new Sequence based on a parameter string */
    public static Sequence parseSequenceParameters(String text, String targetName, MotifLabEngine engine) throws ExecutionError, InterruptedException {
          if (text==null || text.isEmpty()) throw new ExecutionError("Specification of Sequence requires either "+GENE_ID_FIELDS_COUNT+", "+MANUAL_ENTRY_FIELDS_COUNT_8+" or "+MANUAL_ENTRY_FIELDS_COUNT_10+" parameters");
          Sequence sequence=null;
          if (text.toLowerCase().contains("location:") || text.toLowerCase().contains("start:")) { // parameters provided as key:value pairs
               sequence=Sequence.parseArgumentsList(text);                 
          } else {
              String fields[]=text.split("\\s*,\\s*");
              // if (!(fields.length==1 || fields.length==GENE_ID_FIELDS_COUNT || fields.length==MANUAL_ENTRY_FIELDS_COUNT_8 || fields.length==MANUAL_ENTRY_FIELDS_COUNT_10)) throw new ExecutionError("Specification of Sequence requires either 1, "+GENE_ID_FIELDS_COUNT+", "+MANUAL_ENTRY_FIELDS_COUNT_8+" or "+MANUAL_ENTRY_FIELDS_COUNT_10+" parameters");
              if (couldThisBeBED(fields)) {
                  sequence=Sequence.processBEDformat(fields);
              } 
              else if (fields.length==1) { // just a sequence length
                  int sequencesize=0;

                  String sizeString=fields[0];
                  Data dataitem=engine.getDataItem(sizeString);
                  if (dataitem!=null && !(dataitem instanceof Sequence)) { // check for instanceof Sequence since these can have "integer names"
                      if (dataitem instanceof NumericVariable) sequencesize=((NumericVariable)dataitem).getValue().intValue();
                      else throw new ExecutionError("'"+sizeString+"' is not a Numeric Variable");
                  } else {
                      try {
                          sequencesize=Integer.parseInt(sizeString);
                      } catch (NumberFormatException e) {
                          throw new ExecutionError("Unrecognized sequence length '"+sizeString+"' is neither data nor numeric constant for sequence '"+targetName+"'");
                      }
                  }
                  sequence=new Sequence(targetName, 0, "unknown", "?", 1, sequencesize, targetName, null, null, DIRECT);
              } 
              else if (fields.length==GENE_ID_FIELDS_COUNT) { // line specifies a sequence using Gene ID
                  String identifier=fields[0];
                  String idFormat=fields[1];
                  String build=fields[2];
                  int organism=Organism.getOrganismForGenomeBuild(build);
                  GeneIdentifier geneID=new GeneIdentifier(identifier, idFormat, organism, build);
                  int upstream=0;
                  int downstream=0;
                  String anchor=fields[5];
                  try {
                     upstream=Integer.parseInt(fields[3]);
                  } catch (NumberFormatException ne) {
                     throw new ExecutionError("Unable to parse expected numeric range-value for 'from' (value='"+fields[3]+"')");
                  }
                  try {
                     downstream=Integer.parseInt(fields[4]);
                  } catch (NumberFormatException ne) {
                     throw new ExecutionError("Unable to parse expected numeric range-value for 'to' (value='"+fields[4]+"')");
                  }
                  sequence=resolveSequenceForGeneID(geneID,upstream,downstream,anchor,engine);
              } 
              else { // "manual entry" parameters (based on absolute genomic coordinates rather than geneID and gene-relative location)
                  sequence=(fields.length==MANUAL_ENTRY_FIELDS_COUNT_8)?processManualEntryLine8(fields):processManualEntryLine10(fields);
              }
        }
          if (sequence==null) throw new ExecutionError("Unable to determine Sequence from parameters");
          sequence.rename(targetName);
          if (sequence.getSize()>engine.getMaxSequenceLength()) throw new ExecutionError("Size of sequence '"+sequence.getName()+"' exceeds preset maximum ("+engine.getMaxSequenceLength()+" bp)");
          return sequence;
    }
    


    /** Returns true if the first three fields is on the format: chrX start end */
    private static boolean couldThisBeBED(String[] fields) {
        if (fields.length<3) return false;
        if (!fields[0].startsWith("chr")) return false;
        try {
            Integer.parseInt(fields[1]);
            Integer.parseInt(fields[2]);
            return true;
        } catch (NumberFormatException ne) {
            return false;
        }
    }    
    
    private static Sequence parseArgumentsList(String text) throws ExecutionError {
        String[] fields=text.trim().split("\\s*;\\s*");
        boolean definedChromosome=false;
        boolean definedStart=false;
        boolean definedEnd=false;
        Sequence sequence=new Sequence("tempname", Organism.UNKNOWN, "", "?", 0, 0, "unknown", null, null, DIRECT);
        for (String field:fields) {
            if (!field.contains(":")) throw new ExecutionError("All sequence properties must be defined on the format 'key:value'");
            String[] parts=field.split("\\s*:\\s*",2);
            if (parts[0].toLowerCase().equals("start")) {
                if (definedStart) throw new ExecutionError("Sequence start coordinate is defined twice");                
                int start=0;
                try {start=Integer.parseInt(parts[1]);} catch (NumberFormatException nfe) {throw new ExecutionError("Sequence start coordinate is not a number: "+parts[1]);}
                sequence.startPosition=start;
                definedStart=true;
            } else if (parts[0].toLowerCase().equals("end")) {
                if (definedEnd) throw new ExecutionError("Sequence end coordinate is defined twice");
                int end=0;
                try {end=Integer.parseInt(parts[1]);} catch (NumberFormatException nfe) {throw new ExecutionError("Sequence end coordinate is not a number: "+parts[1]);}
                sequence.endPosition=end;
                definedEnd=true;                
            } else if (parts[0].toLowerCase().equals("chromosome") || parts[0].toLowerCase().equals("chr")) {
                if (definedChromosome) throw new ExecutionError("Sequence chromosome coordinate is defined twice");                 
                sequence.chromosome=parts[1];
                definedChromosome=true;
            } else if (parts[0].toLowerCase().equals("location")) {
                if (definedChromosome || definedStart || definedEnd) throw new ExecutionError("Sequence location is defined twice");
                int colonIndex=parts[1].indexOf(':');
                if (colonIndex<=0 || colonIndex>=parts[1].length()-1) throw new ExecutionError("Location property should be on the format 'chr:start-end'");
                String chromosome=parts[1].substring(0, colonIndex);
                sequence.chromosome=chromosome;
                int dashIndex=parts[1].indexOf('-');
                String startString=parts[1].substring(colonIndex+1,dashIndex);
                String endString=parts[1].substring(dashIndex+1);
                try {sequence.startPosition=Integer.parseInt(startString);} catch (NumberFormatException nfe) {throw new ExecutionError("Sequence start coordinate is not a number: "+startString);}
                try {sequence.endPosition=Integer.parseInt(endString);} catch (NumberFormatException nfe) {throw new ExecutionError("Sequence end coordinate is not a number: "+endString);}
                definedChromosome=true;
                definedStart=true;
                definedEnd=true;
            } else if (parts[0].toLowerCase().equals("gene") || parts[0].toLowerCase().equals("gene name") || parts[0].toLowerCase().equals("genename")) {
                sequence.associatedGeneName=parts[1];                
            } else if (parts[0].toLowerCase().equals("tss")) {
                int tss=0;
                try {tss=Integer.parseInt(parts[1]);} catch (NumberFormatException nfe) {throw new ExecutionError("Sequence TSS coordinate is not a number: "+parts[1]);}              
                sequence.TSS=new Integer(tss);
            } else if (parts[0].toLowerCase().equals("tes")) {
                int tes=0;
                try {tes=Integer.parseInt(parts[1]);} catch (NumberFormatException nfe) {throw new ExecutionError("Sequence TES coordinate is not a number: "+parts[1]);}              
                sequence.TES=new Integer(tes);               
            } else if (parts[0].toLowerCase().equals("strand") || parts[0].toLowerCase().equals("orientation")) {
                if (parts[1].toLowerCase().startsWith("rev") || parts[1].equals("-") || parts[1].equals("-1")) sequence.orientation=Sequence.REVERSE;
            } else if (parts[0].toLowerCase().equals("organism")) {
                try {
                    int organism=Integer.parseInt(parts[1]);
                    if (!Organism.isSupportedOrganismID(organism)) throw new ExecutionError("Unknown organism taxonomy id: "+organism);
                    else sequence.organism=organism;
                } catch (NumberFormatException e) {
                    int organism=Organism.getTaxonomyID(parts[1]);
                    if (organism==Organism.UNKNOWN) throw new ExecutionError("Unknown organism name: "+parts[1]);
                    else sequence.organism=organism;
                }
            } else if (parts[0].toLowerCase().equals("build") || parts[0].toLowerCase().equals("genome build")) {
                
            } else if (parts[0].toLowerCase().equals("go-terms")) {
                try {
                   sequence.setGOterms(parts[1].split("\\s*,\\s*"));
                } catch (ParseError p) {
                    throw new ExecutionError(p.getMessage());
                }
            } else { // user defined property
                sequence.setPropertyValue(text, parts);
            }
        }
        if (!definedChromosome) throw new ExecutionError("Invalid location for sequence. Missing specification of chromosome");
        if (!definedStart) throw new ExecutionError("Invalid location for sequence. Missing specification of sequence start");
        if (!definedEnd) throw new ExecutionError("Invalid location for sequence. Missing specification of sequence end");        
        if (sequence.organism!=Organism.UNKNOWN && (sequence.genomeBuild!=null && !sequence.genomeBuild.isEmpty())) { // check that the build is known for the sequence
            if (!Organism.isGenomeBuildSupported(sequence.organism, sequence.genomeBuild)) throw new ExecutionError("Genome build '"+sequence.genomeBuild+"' is not supported for organism '"+Organism.getCommonName(sequence.organism)+"'");
        }
        return sequence;        
    }    
    
    /** Tries to resolve the geneID and returns a sequence based on the first found mapping and the relative location */
    private static Sequence resolveSequenceForGeneID(GeneIdentifier geneid, int upstream, int downstream, String anchor, MotifLabEngine engine) throws ExecutionError {
       GeneIDResolver idResolver=engine.getGeneIDResolver();
       ArrayList<GeneIDmapping> resolvedList=null;
       try {
           ArrayList<GeneIdentifier> idlist=new ArrayList<GeneIdentifier>(1);
           idlist.add(geneid);
           resolvedList=idResolver.resolveIDs(idlist);
       } catch (Exception e) { 
           throw new ExecutionError("An error occurred while resolving gene ID: "+e.getClass().getSimpleName()+": "+e.getMessage());            
       }       
       if (resolvedList==null) resolvedList=new ArrayList<GeneIDmapping>();
       ArrayList<GeneIDmapping> listForGene=getEntriesForID(resolvedList,geneid.identifier);
       if (listForGene.isEmpty()) {
           throw new ExecutionError("Unable to find information about "+Organism.getCommonName(geneid.organism).toLowerCase()+" "+geneid.format+" identifier: "+geneid.identifier);
       } else if (listForGene.size()>1) {
           engine.logMessage("Found "+listForGene.size()+" possible mappings for '"+geneid+"'. Using first.");
       } 
       GeneIDmapping mapping=listForGene.get(0); // use first
       Sequence sequence=new Sequence(mapping.geneID, new Integer(geneid.organism), geneid.build, mapping.chromosome, 0, 0, mapping.geneName, mapping.TSS, mapping.TES, mapping.strand);
       fillInStartAndEndPositions(sequence, upstream, downstream, anchor);
       sequence.setUserDefinedPropertyValue(geneid.format, mapping.geneID);
       if (mapping.GOterms!=null && !mapping.GOterms.isEmpty()) {
           try {sequence.setGOterms(mapping.GOterms);} catch (ParseError e) {} // The terms should have been checked many times already, so just ignore errors at this point
       }       
       return sequence;
    }

    /** Goes through a list of GeneIDmapping and returns only those entries that correspond to the given gene id */
    private static ArrayList<GeneIDmapping> getEntriesForID(ArrayList<GeneIDmapping> list, String id) {
        ArrayList<GeneIDmapping> result=new ArrayList<GeneIDmapping>();
        for (GeneIDmapping entry:list) {
            if (entry.geneID.equalsIgnoreCase(id)) result.add(entry);
        }
        return result;
    }

/** Fills in upstream and downstream coordinates based on user selections and gene orientation */
    private static void fillInStartAndEndPositions(Sequence sequence, int upstream, int downstream, String anchor) throws ExecutionError {
        if (upstream>0) upstream--; // to account for direct transition from -1 to +1 at TSS
        if (downstream>0) downstream--; // to account for direct transition from -1 to +1 at TSS
        int tss=sequence.getTSS();
        int tes=sequence.getTES();
        if (anchor.equalsIgnoreCase("Transcription Start Site") || anchor.equalsIgnoreCase("TSS")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tss+upstream);
               sequence.setRegionEnd(tss+downstream);
            } else { // Reverse Strand
               sequence.setRegionStart(tss-downstream);
               sequence.setRegionEnd(tss-upstream);
            }
        } else if (anchor.equalsIgnoreCase("Transcription End Site") || anchor.equalsIgnoreCase("TES")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tes+upstream);
               sequence.setRegionEnd(tes+downstream);
            } else { // Reverse Strand
               sequence.setRegionStart(tes-downstream);
               sequence.setRegionEnd(tes-upstream);
            }
        } else if (anchor.equalsIgnoreCase("gene") || anchor.equalsIgnoreCase("full gene") || anchor.equalsIgnoreCase("transcript")) {
            if (sequence.getStrandOrientation()==Sequence.DIRECT) {
               sequence.setRegionStart(tss+upstream);
               sequence.setRegionEnd(tes+downstream);
            } else { // Reverse Strand
               sequence.setRegionStart(tes-downstream);
               sequence.setRegionEnd(tss-upstream);
            }                           
        } else {
            throw new ExecutionError("Unsupported anchor site: "+anchor);
        }
        if (sequence.getRegionStart()<0) sequence.setRegionStart(1); // do not let the sequence coordinates be negative
        if (sequence.getRegionEnd()<0) sequence.setRegionEnd(1);     // do not let the sequence coordinates be negative
    }


    /** Creates and returns a new Sequence based on a list of parameters in 'manual entry' format (based on absolute genomic coordinates rather than gene IDs and gene-relative coordinates */
    public static Sequence processManualEntryLine8(String[] fields) throws ExecutionError {
        if (fields.length!=MANUAL_ENTRY_FIELDS_COUNT_8) throw new ExecutionError(MANUAL_ENTRY_FIELDS_COUNT_8+" parameters required");
        String geneName=fields[0];
        String genomeBuild=fields[1];
        int organism=Organism.getOrganismForGenomeBuild(genomeBuild);
        String chromosome=fields[2];
        if (chromosome.length()>3 && chromosome.substring(0,3).equalsIgnoreCase("chr")) chromosome=chromosome.substring(3); // remove 'chr' prefix if present      
        int start=0;
        int end=0;
        Integer TSS=null;
        Integer TES=null;
        int orientation=Sequence.DIRECT;
        try {start=Integer.parseInt(fields[3]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'start' (value='"+fields[3]+"')",0);
        }
        try {end=Integer.parseInt(fields[4]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'end' (value='"+fields[4]+"')",0);
        }
        if (!fields[5].isEmpty() && !fields[5].equalsIgnoreCase("NULL") && !fields[5].equalsIgnoreCase("-") && !fields[5].equalsIgnoreCase(".")) {
            try {
                int tss=Integer.parseInt(fields[5]);
                TSS=new Integer(tss);
            }
            catch (NumberFormatException ne) {
                throw new ExecutionError("Unable to parse expected numeric value for 'TSS' (value='"+fields[5]+"')",0);
            }
        }
        if (!fields[6].isEmpty() && !fields[6].equalsIgnoreCase("NULL") && !fields[6].equalsIgnoreCase("-") && !fields[6].equalsIgnoreCase(".")) {
            try {
                int tes=Integer.parseInt(fields[6]);
                TES=new Integer(tes);
            }
            catch (NumberFormatException ne) {
                throw new ExecutionError("Unable to parse expected numeric value for 'TES' (value='"+fields[6]+"')",0);
            }
        }
        if (fields[7].equalsIgnoreCase("DIRECT") || fields[7].equals("1") || fields[7].startsWith("+")) orientation=Sequence.DIRECT;
        else if (fields[7].equalsIgnoreCase("REVERSE") || fields[7].startsWith("-")) orientation=Sequence.REVERSE;
        else {
            throw new ExecutionError("Unable to parse value for orientation (value='"+fields[7]+"')",0);
        }
        return new Sequence(geneName, organism, genomeBuild, chromosome, start, end, geneName, TSS, TES, orientation);
    }

   /** Creates and returns a new Sequence based on a list of parameters in 'manual entry' format (based on absolute genomic coordinates rather than gene IDs and gene-relative coordinates */
    public static Sequence processManualEntryLine10(String[] fields) throws ExecutionError {
        if (fields.length!=MANUAL_ENTRY_FIELDS_COUNT_10) throw new ExecutionError(MANUAL_ENTRY_FIELDS_COUNT_10+" parameters required");
        String sequenceName=fields[0];
        String geneName=fields[6];
        String genomeBuild=fields[2];
        String organismString=fields[1];
        int organism=0;
        try {
            organism=Integer.parseInt(organismString);
        } catch (NumberFormatException ne) {
            organism=Organism.getTaxonomyID(organismString);
        }
        String chromosome=fields[3];
        if (chromosome.length()>3 && chromosome.substring(0,3).equalsIgnoreCase("chr")) chromosome=chromosome.substring(3); // remove 'chr' prefix if present                     
        int start=0;
        int end=0;
        Integer TSS=null;
        Integer TES=null;
        int orientation=Sequence.DIRECT;
        try {start=Integer.parseInt(fields[4]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'start' (value='"+fields[4]+"')",0);
        }
        try {end=Integer.parseInt(fields[5]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'end' (value='"+fields[5]+"')",0);
        }
        if (!fields[7].isEmpty() && !fields[7].equalsIgnoreCase("NULL") && !fields[7].equalsIgnoreCase("-")) {
            try {
                int tss=Integer.parseInt(fields[7]);
                TSS=new Integer(tss);
            }
            catch (NumberFormatException ne) {
                throw new ExecutionError("Unable to parse expected numeric value for 'TSS' (value='"+fields[7]+"')",0);
            }
        }
        if (!fields[8].isEmpty() && !fields[8].equalsIgnoreCase("NULL") && !fields[8].equalsIgnoreCase("-")) {
            try {
                int tes=Integer.parseInt(fields[8]);
                TES=new Integer(tes);
            }
            catch (NumberFormatException ne) {
                throw new ExecutionError("Unable to parse expected numeric value for 'TES' (value='"+fields[8]+"')",0);
            }
        }
        if (fields[9].equalsIgnoreCase("DIRECT") || fields[9].equals("1") || fields[9].startsWith("+")) orientation=Sequence.DIRECT;
        else if (fields[9].equalsIgnoreCase("REVERSE") || fields[9].startsWith("-")) orientation=Sequence.REVERSE;
        else {
            throw new ExecutionError("Unable to parse value for orientation (value='"+fields[9]+"')",0);
        }
        return new Sequence(sequenceName, organism, genomeBuild, chromosome, start, end, geneName, TSS, TES, orientation);
    }

   /** Creates and returns a new Sequence based on a list of parameters in 'manual entry' format (based on absolute genomic coordinates rather than gene IDs and gene-relative coordinates */
    public static Sequence processManualEntryLine4(String[] fields) throws ExecutionError {
        if (fields.length!=MANUAL_ENTRY_FIELDS_COUNT_4) throw new ExecutionError(MANUAL_ENTRY_FIELDS_COUNT_4+" parameters required");
        String geneName=fields[0];
        String chromosome=fields[1];
        if (chromosome.length()>3 && chromosome.substring(0,3).equalsIgnoreCase("chr")) chromosome=chromosome.substring(3); // remove 'chr' prefix if present      
        int start=0;
        int end=0;
        int orientation=Sequence.DIRECT;
        try {start=Integer.parseInt(fields[2]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'start' (value='"+fields[2]+"')",0);
        }
        try {end=Integer.parseInt(fields[3]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'end' (value='"+fields[3]+"')",0);
        }
        if (start>end) {
            orientation=Sequence.REVERSE;
            int swap=start;
            start=end;
            end=swap;
        }
        return new Sequence(geneName, 0, null, chromosome, start, end, geneName, null, null, orientation);
    }    
    
   /** Creates and returns a new Sequence based on a list of fields in standard BED format */
    public static Sequence processBEDformat(String[] fields) throws ExecutionError {
        if (fields.length<3) throw new ExecutionError("At least 3 fields required by BED format");
        String chromosome=fields[0];
        if (chromosome.length()>3 && chromosome.substring(0,3).equalsIgnoreCase("chr")) chromosome=chromosome.substring(3); // remove 'chr' prefix if present      
        int start=0;
        int end=0;       
        int orientation=0;
        try {start=Integer.parseInt(fields[1]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'start' (value='"+fields[1]+"')",0);
        }
        start++; // convert start-coordinate to 1-indexed. End coordinate is exclusive and hence need no further conversion                   
        try {end=Integer.parseInt(fields[2]);}
        catch (NumberFormatException ne) {
            throw new ExecutionError("Unable to parse expected numeric value for 'end' (value='"+fields[2]+"')",0);
        }
        String geneName;        
        if (fields.length>=4) { // 4th field should be the type (sequence name)!
            geneName=fields[3];
        } else {
            geneName="chr"+chromosome+"_"+start+"_"+end;
        }         
        if (start>end) { // make sure the smallest coordinate is first
            orientation=Sequence.REVERSE;
            int swap=start;
            start=end;
            end=swap;
        }
        if (fields.length>=6) { // 6th field should be the strand!
            String strand=fields[5];
            if (strand.equalsIgnoreCase("DIRECT") || strand.equals("1") || strand.startsWith("+")) orientation=Sequence.DIRECT;
            else if (strand.equalsIgnoreCase("REVERSE") || strand.startsWith("-")) orientation=Sequence.REVERSE;
        } 
        if (orientation==0) { // still undetermined orientation. Just assume direct strand
            orientation=Sequence.DIRECT;          
        }       
        return new Sequence(geneName, 0, null, chromosome, start, end, geneName, null, null, orientation);
    }        
    
    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=2; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
         out.writeObject(properties);
         out.writeObject(GOterms);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         in.defaultReadObject();
         if (currentinternalversion>=2) {
             properties=(HashMap<String, Object>)in.readObject();
             GOterms=(int[])in.readObject();
         } else if (currentinternalversion>2) throw new ClassNotFoundException("Newer version");
    }
}    