/*
 
 
 */

package org.motiflab.engine.data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;

/**
 *
 * @author kjetikl
 */
public class Region implements Cloneable,Serializable {
    public static final int DIRECT=1;
    public static final int REVERSE=-1;
    public static final int INDETERMINED=0;
    
    
    private int start=0;
    private int end=0;
    private RegionSequenceData parent=null;
    private String type; // e.g. Alu-repeat, NFkB-binding site
    private int orientation;
    private double score;
    private String sequence=null;
    //private boolean ismodule=false;
    private HashMap<String,Object> properties=null; // useful properties: name, score, source, orientation. All names should be lowercase (properties are case-insensitive)
    public short row=-1; // this is used by track visualizers that display regions on multiple rows

    /**
     * Constructs a new region located at the specified start and end positions
     * relative to the start of the parent RegionSequenceData object 
     * (that is, the start and end positions are offsets).  
     * 
     * @param parent
     * @param start (position relative to parent sequence)
     * @param end (position relative to parent sequence)
     */
    public Region(RegionSequenceData parent, int start, int end) {
        this.parent=parent;
        this.start=start;
        this.end=end;   
        if (this.start>this.end) {int swap=start;start=end;end=swap;} // It is the callers responsibility to make sure that start<=end. But just in case...
        this.type="unknown";
        this.score=0;
        this.orientation=INDETERMINED;
    }
    /**
     * Constructs a new region located at the specified start and end positions
     * relative to the start of the parent RegionSequenceData object (that is,
     * the start and end positions are offsets).  
     * 
     * @param parent
     * @param start
     * @param end
     * @param type
     * @param score
     * @param orientation
     */
    public Region(RegionSequenceData parent, int start, int end, String type, double score, int orientation) {
        this.parent=parent;
        this.start=start;
        this.end=end;   
        if (this.start>this.end) {int swap=start;start=end;end=swap;} // It is the callers responsibility to make sure that start<=end. But just in case...
        this.type=type;
        this.score=score;
        this.orientation=orientation;
    }
    
    /**
     * Returns the parent data sequence of this region
     * @return RegionSequenceData
     */
    public RegionSequenceData getParent() {
        return parent;
    }   
    
     /**
     * Sets the parent data sequence of this region
     * (if the Region has "child-regions" within its properties (as is the case for module-regions),
     * these will also be assigned to the new parent)
     * @param newparent
     */
    public void setParent(RegionSequenceData newparent) {
        parent=newparent;
        if (properties!=null) {
            for (String key:properties.keySet()) {
                Object value=properties.get(key);
                if (value instanceof Region) ((Region)value).setParent(newparent);
                else if (value instanceof ArrayList) {
                    for (Object obj:(ArrayList)value) {
                        if (obj instanceof Region) ((Region)obj).setParent(newparent);
                    }
                }
            }

        }
    }   
    
    /** Returns the RegionDataset that this region is part of.
     *  This is the same as its 'grandparent' returned by region.getParent().getParent()
     */
    public RegionDataset getRegionTrack() {
        return (RegionDataset)getParent().getParent(); 
    }
    
    /** Returns the name of the RegionDataset track that this region is a part of (or null) if the region is not part of a track */
    public String getTrackName() {
        if (parent==null) return null;
        FeatureDataset dataset=parent.getParent();
        if (dataset!=null) return dataset.getName();
        else return null;
    }
    /**
     * Returns the start position of this region relative to the parent sequence (Direct orientation)
     * @return start position
     */
    public int getRelativeStart() {
        return start;
    }
    
   /**
     * Returns the end position of this region relative to the parent sequence (Direct orientation)
     * @return end position
     */
    public int getRelativeEnd() {
        return end;
    }
    
    /**
     * Returns the start position of this region relative to the parent sequence (relative orientation)
     * @return start position
     */
    public int getStrandRelativeStart() {
        int strand=(parent!=null)?parent.getStrandOrientation():0; // default to UNDETERMINED, which will return regular start property
        if (strand>0) { // direct
            return start;
        } else if (strand<0) { // reverse
            return (parent.getSize()-1)-end;
        }
        return start;
    }
    
   /**
     * Returns the end position of this region relative to the parent sequence (relative orientation)
     * @return end position
     */
    public int getStrandRelativeEnd() {
        int strand=(parent!=null)?parent.getStrandOrientation():0; // default to UNDETERMINED, which will return regular start property
        if (strand>0) { // direct
            return end;
        } else if (strand<0) { // reverse
            return (parent.getSize()-1)-start;
        }
        return end;
    }    
    
    /**
     * Returns the start position of this region in genomic coordinates (1-indexed)
     * @return start position
     */
    public int getGenomicStart() {
        if (parent==null) return start;
        return parent.getRegionStart()+start;
    }
    
   /**
     * Returns the end position of this region in genomic coordinates (1-indexed)
     * @return end position
     */
    public int getGenomicEnd() {
        if (parent==null) return end;
        return parent.getRegionStart()+end;
    }
 
    /** Returns the start or end position of the region relative to an anchor position (either TSS or TES) 
     *  @param useTSS.    If TRUE, the anchor position will be TSS else the anchor will be TES
     *  @param useStart.  If TRUE, the method will return the relative start position of the region, if false the method will return the relative end position
     *  @param skip0.     If TRUE, the ruler will jump from -1 to +1 [TSS/TES] instead of having TSS/TES at +0
     */
    public int getAnchorRelativePosition(boolean useTSS, boolean useStart, boolean skip0) {   
        if (parent==null) return (useStart)?getRelativeStart():getRelativeEnd();
        Sequence seq=(Sequence)MotifLabEngine.getEngine().getDataItem(parent.getSequenceName(), Sequence.class);
        if (seq==null) return (useStart)?getRelativeStart():getRelativeEnd();
        Integer anchor=(useTSS)?seq.getTSS():seq.getTES();
        if (anchor==null) return (useStart)?getRelativeStart():getRelativeEnd();
        int seqOrientation=seq.getStrandOrientation();
        int regionstart=getGenomicStart();
        int regionend=getGenomicEnd();           
        regionstart=(seqOrientation==Sequence.DIRECT)?regionstart-anchor:anchor-regionstart;
        regionend=(seqOrientation==Sequence.DIRECT)?regionend-anchor:anchor-regionend;
        if (seqOrientation==Sequence.REVERSE) {int swap=regionstart; regionstart=regionend; regionend=swap;}
        if (skip0) { // 
           if (regionstart>=0) regionstart++; // because there is no "0" position
           if (regionend>=0) regionend++; 
        }        
        return (useStart)?regionstart:regionend;
    }
    
    /**
     * Sets the start position of this region relative to the parent sequence
     * @param start New start position
     */
    public void setRelativeStart(int start) {
        this.start=start;
    }
    
   /**
     * Sets the end position of this region relative to the parent sequence
     * @param end New end position
     */
    public void setRelativeEnd(int end) {
        this.end=end;
    }    
    
   /**
    * Returns the length of the region in base pairs
    * @return
    */
    public int getLength() {
        return end-start+1;
    }
    
    
    public String getType() {
        return (type!=null)?type:"";
    }
    
    public void setType(String type) {
        this.type=type;      
    }
    
    public double getScore() {
        return score;
    }

    /** Sets the score property for this Region */
    public void setScore(double score) {
        this.score=score;
    }
    
    /**
     * @return The orientation of this region (relative to chromosome, not parent sequence)
     */
    public int getOrientation() {
        return orientation;
    }
    
    /**
     * @return The orientation opposite of the region's orientation. I.e. if the region's orientation is DIRECT, this will return REVERSE and vice versa. If the orientation is INDETERMINED, it will return INDETERMINED
     */    
    public int getFlippedOrientation() {
        if (orientation==Region.DIRECT) return Region.REVERSE;
        else if (orientation==Region.REVERSE) return Region.DIRECT;
        else return orientation;
    }    
    
    public void setOrientation(int orientation) {
        this.orientation=orientation;
    }
    
    /** This will return +1 if this region has the same orientation as the parent sequence and -1 if they have opposite orientations
     *  If either the sequence or region has indetermined orientation the value 0 will be returned
     */
    public int getRelativeOrientation() {
        if (this.orientation==0 || parent==null || parent.getStrandOrientation()==0) return 0;
        return (parent.getStrandOrientation()==this.orientation)?1:-1;
    }  
    
    private String getOrientationSign(int orientationValue) {
        if (orientationValue>0) return "+";
        else if (orientationValue<0) return "-";
        else return ".";
    }
    
    private String getOrientationString(int orientationValue) {
        if (orientationValue>0) return "Direct";
        else if (orientationValue<0) return "Reverse";
        else return "Undetermined";
    }    
    
    /** A convenience method to set the 'sequence' property
     *  The sequence property is mostly used by motif regions to store
     *  the actual sequence bound by the TF at this site. 
     *  The sequence is relative to the orientation of the region 
     *  (i.e. not necessarily direct strand)      
     */
    public void setSequence(String sequence) {
        this.sequence=sequence;
    }
    
    /** A convenience method to get the 'sequence' property.
     *  The sequence property is mostly used by motif regions to store
     *  the actual sequence bound by the TF at this site. 
     *  The sequence is relative to the orientation of the region 
     *  (i.e. not necessarily direct strand) 
     */
    public String getSequence() {
        return sequence;
    }    
    
    /** Returns the given property as an object of the given class
     *  or NULL if either the property does not exist or it cannot be
     *  converted to the given type
     */
    public Object getPropertyAsType(String property, Class type) {
        Object value=getProperty(property);
        if (value==null) return null;
        if (type.isAssignableFrom(value.getClass())) return value; // already correct class
        if (type==Integer.class) {
            try {return Integer.parseInt(value.toString());} catch (NumberFormatException e) {return null;}
        } else if (type==Double.class || type==Number.class) {
            try {return Double.parseDouble(value.toString());} catch (NumberFormatException e) {return null;}
        } else if (type==String.class) {
            return value.toString();
        } else if (type==Boolean.class) {
            String string=value.toString();
            if (string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes")) return Boolean.TRUE;
            else return Boolean.FALSE; 
        }
        return null;
    }
    
    /**
     * 
     * @param property The name of the property
     * @return The value of the property, if specified, else null
     */
    public Object getProperty(String property) {
            int offset=0; boolean xoffset=false;
            if (property.endsWith("[0]")) {xoffset=true;property=property.substring(0,property.length()-3);}
            else if (property.endsWith("[1]")) {offset=1;xoffset=true;property=property.substring(0,property.length()-3);}
                 if (property.equalsIgnoreCase("type")) return getType();
            else if (property.equalsIgnoreCase("score")) return getScore();
            else if (property.equalsIgnoreCase("orientation") || property.equalsIgnoreCase("strand")) return getOrientation();
            else if (property.equalsIgnoreCase("relative orientation") || property.equalsIgnoreCase("relative strand")) return getRelativeOrientation();
            else if (property.equalsIgnoreCase("orientation sign") || property.equalsIgnoreCase("strand sign")) return getOrientationSign(getOrientation());
            else if (property.equalsIgnoreCase("orientation string") || property.equalsIgnoreCase("strand string")) return getOrientationString(getOrientation());
            else if (property.equalsIgnoreCase("relative orientation sign") || property.equalsIgnoreCase("relative strand sign")) return getOrientationSign(getRelativeOrientation());
            else if (property.equalsIgnoreCase("relative orientation string") || property.equalsIgnoreCase("relative strand string")) return getOrientationString(getRelativeOrientation());
            else if (property.equalsIgnoreCase("sequence")) return getSequence(); // DNA sequence spanning the region                   
            else if (property.equalsIgnoreCase("sequence name")) return (parent!=null)?parent.getSequenceName():""; // name of parent sequence                   
            else if (property.equalsIgnoreCase("start")) return getRelativeStart()+offset;  
            else if (property.equalsIgnoreCase("end")) return getRelativeEnd()+offset;  
            else if (property.equalsIgnoreCase("relative start")) return getStrandRelativeStart()+offset;  
            else if (property.equalsIgnoreCase("relative end")) return getStrandRelativeEnd()+offset;   
            else if (property.equalsIgnoreCase("genomic start")) return getGenomicStart()+((xoffset && offset==0)?-1:0);  // genomic coordinates should be 1-indexed by default, but subtract 1 to get 0-indexed if explicitly requested
            else if (property.equalsIgnoreCase("genomic end")) return getGenomicEnd()+((xoffset && offset==0)?-1:0);      // genomic coordinates should be 1-indexed by default, but subtract 1 to get 0-indexed if explicitly requested
            else if (property.equalsIgnoreCase("size") || property.equalsIgnoreCase("length")) return getLength();    
            else if (property.equalsIgnoreCase("chr") || property.equalsIgnoreCase("chromosome")) return ((parent!=null)?parent.getChromosome():"?");     
            else if (property.equalsIgnoreCase("chr string") || property.equalsIgnoreCase("chromosome string")) return "chr"+((parent!=null)?parent.getChromosome():"?");     
            else if (property.equalsIgnoreCase("TSS-relative start")) return getAnchorRelativePosition(true,true,true);  // if [0] or [1] is not specified it defaults to [1]
            else if (property.equalsIgnoreCase("TSS-relative end")) return getAnchorRelativePosition(true,false,true);   //   -- " --
            else if (property.equalsIgnoreCase("TES-relative start")) return getAnchorRelativePosition(false,true,true); //   -- " --
            else if (property.equalsIgnoreCase("TES-relative end")) return getAnchorRelativePosition(false,false,true);  //   -- " --        
            else if (property.equalsIgnoreCase("TSS[0]-relative start")) return getAnchorRelativePosition(true,true,false);
            else if (property.equalsIgnoreCase("TSS[0]-relative end")) return getAnchorRelativePosition(true,false,false);
            else if (property.equalsIgnoreCase("TES[0]-relative start")) return getAnchorRelativePosition(false,true,false);
            else if (property.equalsIgnoreCase("TES[0]-relative end")) return getAnchorRelativePosition(false,false,false);           
            else if (property.equalsIgnoreCase("TSS[1]-relative start")) return getAnchorRelativePosition(true,true,true);
            else if (property.equalsIgnoreCase("TSS[1]-relative end")) return getAnchorRelativePosition(true,false,true);
            else if (property.equalsIgnoreCase("TES[1]-relative start")) return getAnchorRelativePosition(false,true,true);
            else if (property.equalsIgnoreCase("TES[1]-relative end")) return getAnchorRelativePosition(false,false,true);                       
            else if (property.equalsIgnoreCase("location")) return getChromosomalLocationAndOrientationAsString();
            else if (property.toLowerCase().startsWith("sequence:")) { // return property of parent sequence
                if (parent==null) return null;
                property=property.substring("sequence:".length());
                Sequence seq=(Sequence)MotifLabEngine.getEngine().getDataItem(parent.getName(), Sequence.class);
                if (seq!=null) try {
                    return seq.getPropertyValue(property, MotifLabEngine.getEngine());
                } catch (ExecutionError e) {}
                return null;
            } else if (property.toLowerCase().startsWith("motif:")) { // return property of motif
                property=property.substring("motif:".length());
                Motif motif=(Motif)MotifLabEngine.getEngine().getDataItem(type, Motif.class);
                if (motif!=null) try {
                    return motif.getPropertyValue(property, MotifLabEngine.getEngine());
                } catch (ExecutionError e) {}
                return null;
            } else if (property.toLowerCase().startsWith("module:")) { // return property of module
                property=property.substring("module:".length());
                ModuleCRM cisRegModule=(ModuleCRM)MotifLabEngine.getEngine().getDataItem(type, ModuleCRM.class);
                if (cisRegModule!=null) try {
                    return cisRegModule.getPropertyValue(property, MotifLabEngine.getEngine());
                } catch (ExecutionError e) {}
                return null;
            }    
            else {
                if (properties==null) return null;
                else return properties.get(property); // previously "property.toLowerCase()" but this caused problems with modulemotifs
        }
    }
    
    public void setProperty(String property, Object value) {
        if (property.equalsIgnoreCase("start")) {
             if (value instanceof Integer) setRelativeStart((Integer)value);
        }
        else if (property.equalsIgnoreCase("end")) {
             if (value instanceof Integer) setRelativeEnd((Integer)value);
        }        
        else if (property.equalsIgnoreCase("type")) {
             if (value instanceof ArrayList) setType(MotifLabEngine.splice((ArrayList)value, " "));
             else setType(value.toString());
        }
        else if (property.equalsIgnoreCase("score")) {
            if (value instanceof Number) setScore(((Number)value).doubleValue());
            else setScore(0); // I don't bother throwing errors
        }
        else if (property.equalsIgnoreCase("orientation") || property.equalsIgnoreCase("strand")) {
            if (value instanceof Number) {
                  if (((Number)value).intValue()==Region.DIRECT) setOrientation(Region.DIRECT);
                else if (((Number)value).intValue()==Region.REVERSE) setOrientation(Region.REVERSE);
                else setOrientation(Region.INDETERMINED);
            } else setOrientation(Region.INDETERMINED);
        }
        else if (property.equalsIgnoreCase("sequence")) {
            setSequence((String)value);
        } else if (property.equalsIgnoreCase("sequence from track")) { // this is sort of a hack to set the sequence based on an existing track
            DNASequenceDataset dnatrack=(DNASequenceDataset)MotifLabEngine.getEngine().getDataItem((String)value, DNASequenceDataset.class);
            if (dnatrack!=null && parent!=null) {
                DNASequenceData dnaseq=(DNASequenceData)dnatrack.getSequenceByName(parent.getSequenceName());
                if (dnaseq!=null) {
                    char[] dnasequence=(char[])dnaseq.getValueInGenomicInterval(getGenomicStart(), getGenomicEnd());
                    if (dnasequence!=null && dnasequence.length==getLength()) {
                         String siteseq=new String(dnasequence);
                         if (getOrientation()==Region.REVERSE) siteseq=MotifLabEngine.reverseSequence(siteseq);
                         setSequence(siteseq);                        
                    }
                }
            }
        }                
        else { // Set user-defined property. Note that these are case-sensitive
            if (properties==null) properties=new HashMap<String, Object>(2);
            if (value==null || value.toString().isEmpty()) properties.remove(property); //
            else {
                if (value instanceof ArrayList) value=MotifLabEngine.splice((ArrayList)value, ",");
                properties.put(property, value); //
            }
        }
    }
    
    /**
     * Returns a list containing the names of all non-standard properties associated with this Region 
     */
    public String[] getAllPropertyNames() {
        if (properties==null) return new String[0];
        Set<String> keySet=properties.keySet();
        String[] result=new String[keySet.size()];
        int i=0;
        for (String key:keySet) {
            result[i]=key;
            i++;
        }
        return result;
    }    
    
    /** 
     * Returns the (original) map of user-defined properties
     * @return 
     */
    public HashMap<String, Object> getUserDefinedProperties() {
       return properties; 
    }
    
    public static boolean isReservedProperty(String name) {
        return (   name.equalsIgnoreCase("type")
                || name.equalsIgnoreCase("score")
                || name.equalsIgnoreCase("orientation")
                || name.equalsIgnoreCase("orientation sign")
                || name.equalsIgnoreCase("orientation string")                 
                || name.equalsIgnoreCase("strand")
                || name.equalsIgnoreCase("sequence")
                || name.equalsIgnoreCase("sequence name")                
                || name.equalsIgnoreCase("start")
                || name.equalsIgnoreCase("end")
                || name.equalsIgnoreCase("genomic start")
                || name.equalsIgnoreCase("genomic end")  
                || name.equalsIgnoreCase("relative start")
                || name.equalsIgnoreCase("relative end")                  
                || name.equalsIgnoreCase("chr")  
                || name.equalsIgnoreCase("chromosome")                  
                || name.equalsIgnoreCase("length")
                || name.equalsIgnoreCase("location")                
                || name.equalsIgnoreCase("size")
                || name.equalsIgnoreCase("TSS[0]-relative start") 
                || name.equalsIgnoreCase("TSS[0]-relative end")                 
                || name.equalsIgnoreCase("TES[0]-relative start") 
                || name.equalsIgnoreCase("TES[0]-relative end")    
                || name.equalsIgnoreCase("TSS[1]-relative start") 
                || name.equalsIgnoreCase("TSS[1]-relative end")                 
                || name.equalsIgnoreCase("TES[1]-relative start") 
                || name.equalsIgnoreCase("TES[1]-relative end")                 
                );
    }
    
    /**
     * Checks whether this region is located prior to another region according to sorting order
     * (ie. the start if this region lies before the other region on the direct strand
     * or this region ends before the other regions if they both start at the same position)
     * Note that if the two regions have identical locations (both start and end) the method will return FALSE.
     * @param anotherRegion
     * @return true if this region is located prior to the other region. 
     */
    public boolean isLocatedPriorTo (Region other) {
        if (start<other.start) return true;
        else if (start==other.start && end<other.end) return true;
        else return false;
    }
    
    /**
     * Checks whether this region is located prior to another region when looking at the sequence in reverse orientation
     * (ie. the start if this region lies before the other region on the reverse strand
     * or this region ends before the other regions if they both start at the same position)
     * @param anotherRegion
     * @return true if this region is located prior to the other region on reverse strand
     */
    public boolean isLocatedPriorToOnReverseStrand (Region other) {
        if (end>other.end) return true;
        else if (end==other.end && start<other.start) return true;
        else return false;
    }
    
    /** Returns TRUE is this region is designated as a potential "module" region (and possibly contains nested motif-regions) */
    public boolean isModule() {
        return (parent!=null && parent.isModuleTrack());
    }
    
    /** Returns TRUE is this region is designated as a potential "TFBS" region  */
    public boolean isMotif() {
        return (parent!=null && parent.isMotifTrack());
    }    

    /** Returns all Regions that are nested within this region. E.g. if this Region represents a module site
     *  this method will return a list of all constituent single motif sites in the module
     *  @param clone If TRUE the returned regions will be clones of the originals, if FALSE the original regions will be returned
     */
    public ArrayList<Region> getNestedRegions(boolean clone) {
        ArrayList<Region> nested=new ArrayList<Region>();
        if (properties!=null) {
            for (String key:properties.keySet()) {
                Object value=properties.get(key);
                if (value instanceof Region) nested.add((clone)?(Region)((Region)value).clone():(Region)value);
                else if (value instanceof ArrayList) {
                   for (Object obj:(ArrayList)value) {
                       if (obj instanceof Region) nested.add((clone)?(Region)((Region)obj).clone():(Region)obj);
                   }
                } 
            }
        }
        return nested;
    }
    
    public boolean hasNestedRegions() {
        if (properties!=null) {
            for (String key:properties.keySet()) {
                Object value=properties.get(key);
                if (value instanceof Region) return true;
                else if (value instanceof ArrayList) {
                   for (Object obj:(ArrayList)value) {
                       if (obj instanceof Region) return true;
                   }
                } 
            }
        }
        return false;
    }    

    /**
     * Returns a HashMap indexed in property name where the value is
     * either a single Region or an ArrayList<Region> for properties that
     * contain nested regions
     * @param clone If TRUE the returned regions will be clones of the originals, if FALSE the original regions will be returned
     * @return
     */
    public HashMap<String,Object> getNestedRegionProperties(boolean clone) {
        HashMap<String,Object> result=new HashMap<String, Object>();
        if (properties!=null) {
            for (String key:properties.keySet()) {
                Object value=properties.get(key);
                if (value instanceof Region) result.put(key,(clone)?(Region)((Region)value).clone():(Region)value);
                else if (value instanceof ArrayList) {
                   ArrayList<Region> nested=new ArrayList<Region>();
                   for (Object obj:(ArrayList)value) {
                       if (obj instanceof Region) nested.add((clone)?(Region)((Region)obj).clone():(Region)obj);
                   }
                   result.put(key,nested);
                }
            }
        }
        return result;
    }

    /**
     * Adds a nested Region to this region. The nested region could for instance represent the binding site
     * for a single motif if this Region represents a module site
     * @param propertyname The property name to nest the region under (e.g. the name of the modulemotif)
     * @param region
     * @param replace If true, the existing property entry will be replaced. If false, the region will be added to the current list
     */
    public void addNestedRegion(String propertyname, Region region, boolean replace) {
        if (replace) {
            setProperty(propertyname, region);
        } else {
            Object existing=getProperty(propertyname);
            if (existing instanceof Region) {
                ArrayList<Region> nested=new ArrayList<Region>(2);
                nested.add((Region)existing);
                nested.add(region);
                setProperty(propertyname, nested);
            } else if (existing instanceof ArrayList) {
                ((ArrayList)existing).add(region);
            } else setProperty(propertyname, region); // previous property not compatible, so replace it
        }
        region.setParent(parent); // the parent is the parent sequence
    }

    
    public int[] getThickSegment() {
        if (properties==null) return null;
        Object thickStart = properties.get("thickStart"); // previously "property.toLowerCase()" but this caused problems with modulemotifs        
        Object thickEnd   = properties.get("thickEnd"); // previously "property.toLowerCase()" but this caused problems with modulemotifs 
        if (thickStart instanceof Integer && thickEnd instanceof Integer) return new int[]{(Integer)thickStart,(Integer)thickEnd}; // are both always specified?
        else return null;
    }
    
    /**
     * Returns a deep clone of this Region object
     */
    @SuppressWarnings("unchecked")
    @Override
    public Region clone() {
        Region newdata=new Region(parent, start, end, type, score, orientation);
        newdata.sequence=this.sequence;
        if (properties!=null) {
            newdata.properties=new HashMap<String, Object>(properties.size());
            for (String key:properties.keySet()) {
                Object value=properties.get(key);
                if (value instanceof Region) newdata.properties.put(key, ((Region)value).clone());
                else if (value instanceof ArrayList) {
                   ArrayList<Region> list=new ArrayList<Region>();
                   for (Object obj:(ArrayList)value) {
                       list.add((Region)((Region)obj).clone());
                   }
                   newdata.properties.put(key, list);
                } else newdata.properties.put(key, value);
            }

        }
        newdata.row=this.row;
        return newdata;
    }
    
    @Override
    public String toString() {
        StringBuilder desc = new StringBuilder();
        desc.append("chr");
        desc.append(((parent!=null)?parent.getChromosome():"?"));
        desc.append(":");
        desc.append(((parent!=null)?getGenomicStart():"?"));
        desc.append("-");
        desc.append(((parent!=null)?getGenomicEnd():"?"));
        desc.append("    [relative=");
        desc.append(getRelativeStart());
        desc.append("-");
        desc.append(getRelativeEnd());
        desc.append("]");
        desc.append("(");
        desc.append(orientation);
        desc.append(")");
        if (type!=null && !type.isEmpty()) {
            desc.append("  type=");
            desc.append(type);
        }
        desc.append("   score=");
        desc.append(score);
        desc.append("  ");
        desc.append(getPropertiesAsString(", ",0,false));
        return desc.toString();
    }
    
    public String getChromosomalLocationAsString() {
        StringBuilder desc = new StringBuilder();
        desc.append("chr");
        desc.append(((parent!=null)?parent.getChromosome():"?"));
        desc.append(":");
        desc.append(((parent!=null)?getGenomicStart():"?"));
        desc.append("-");
        desc.append(((parent!=null)?getGenomicEnd():"?"));
        return desc.toString();
    }
    
    public String getOrientationAsString() {
        if (orientation==Region.DIRECT) return "+";
        else if (orientation==Region.REVERSE) return "-";
        else return ".";
    }

    public String getChromosomalLocationAndOrientationAsString() {
        return getChromosomalLocationAsString()+"("+getOrientationAsString()+")";           
    }
    
    /** Returns the registered properties as a string with key=value pairs (excluding: type, score, orientation, thickStart and thickEnd) 
      * @param separator A string that will be inserted between every key=value pair in the returned String
      * @param maxlength if, when adding a new key=value pair, the current length of the string exceeds this length, an "..." will be added and no more properties will be processed.
     *                    If this is set to 0, all properties will be returned
     */
    public String getPropertiesAsString(String separator, int maxlength, boolean includeNested) {      
        if (properties==null) return "";
        StringBuilder result=new StringBuilder(); // (type!=null)?("type="+type+", "):"";
        Set<String> keys=properties.keySet();
        int i=0;
        int total=keys.size();
        for (String key:keys) {
            if (key.equalsIgnoreCase("thickStart") || key.equalsIgnoreCase("thickEnd") || key.equalsIgnoreCase("thick")) continue;
            if (maxlength>0 && result.length()>maxlength) {
                result.append("...");
                break;
            }
            Object value=properties.get(key);
            if (!includeNested && value instanceof Region) continue;
            if (value instanceof Region) value=((Region)value).getType(); // nested region
            result.append(key);
            result.append("=");
            result.append(value.toString());
            i++;           
            if (i<total) result.append(separator);         
        }               
        return result.toString();
    }
    
    /** 
     * This method returns TRUE if this region is 'identical to' the other region supplied.
     * The definition of 'identical to' is that the two regions have the same relative location
     * to their respective parents (if any) and that ALL the other properties are the same
     * (Note that two regions can be 'identical' even though their parents are not the same
     * (in which case they could also be 'identical' even though they are actually located
     * at different genomic positions (albeit same relative positions))). 
     */
    public boolean isIdenticalTo(Region otherregion) {
             if (this.start!=otherregion.start) return false;
        else if (this.end!=otherregion.end) return false;
        else if (!this.type.equals(otherregion.type)) return false;
        else if (this.score!=otherregion.score) return false;
        else if (this.orientation!=otherregion.orientation) return false;
        else if ((this.sequence==null && otherregion.sequence!=null) || (this.sequence!=null && otherregion.sequence==null) || (this.sequence!=null && otherregion.sequence!=null && !this.sequence.equals(otherregion.sequence))) return false;
        else if (!propertiesAreSimilarTo(otherregion)) return false;
        else return true;      
    }
    

    /** Reports the first property found that differs between this region and the otherregion
     *  This method is used for debugging
     */
    public String reportFirstDifference(Region otherregion) {
             if (this.start!=otherregion.start) return "start";
        else if (this.end!=otherregion.end) return "end";
        else if (!this.type.equals(otherregion.type)) return "type";
        else if (this.score!=otherregion.score) return "score";
        else if (this.orientation!=otherregion.orientation) return "orientation";
        else if ((this.sequence==null && otherregion.sequence!=null) || (this.sequence!=null && otherregion.sequence==null) || (this.sequence!=null && otherregion.sequence!=null && !this.sequence.equals(otherregion.sequence))) return "sequence";
        else return reportFirstPropertyDifference(otherregion);
    }
    
    /**
     * This method returns TRUE if this region is 'similar to' the other region supplied.
     * The definition of 'similar to' is that the two regions have the same relative location
     * to their respective parents (if any), same orientation and also the same type.
     * However, other properties like score, sequence and user-defined properties need not be the same
     */
    public boolean hasSameLocationAndType(Region otherregion) {
             if (this.start!=otherregion.start) return false;
        else if (this.end!=otherregion.end) return false;
        else if (!this.type.equals(otherregion.type)) return false;
        else if (this.orientation!=otherregion.orientation) return false;
        else return true;
    }
    
    /**
     * This method returns TRUE if this region is 'similar to' the other region supplied.
     * The definition of 'similar to' is that the two regions have the same relative location
     * to their respective parents (if any), same orientation and also the same type.
     * In addition, the two standard properties "score" and "sequence" must also be the same,
     * but other user-defined properties can differ
     */
    public boolean hasSameStandardProperties(Region otherregion) {
             if (this.start!=otherregion.start) return false;
        else if (this.end!=otherregion.end) return false;
        else if (!this.type.equals(otherregion.type)) return false;
        else if (this.score!=otherregion.score) return false;
        else if (this.orientation!=otherregion.orientation) return false;
        else if ((this.sequence==null && otherregion.sequence!=null) || (this.sequence!=null && otherregion.sequence==null) || (this.sequence!=null && otherregion.sequence!=null && !this.sequence.equals(otherregion.sequence))) return false;
        else return true; 
    }    

    private boolean propertiesAreSimilarTo(Region otherregion) {
        if (this.properties==null && otherregion.properties==null) return true;
        else if (this.properties==null && otherregion.properties!=null) return false;
        else if (this.properties!=null && otherregion.properties==null) return false;
        if (this.properties.size()!=otherregion.properties.size()) return false;
        for (String key:this.properties.keySet()) {
            if (!otherregion.properties.containsKey(key)) return false;
            Object thisPropertyObject=this.properties.get(key);
            Object otherPropertyObject=otherregion.properties.get(key);
            if (thisPropertyObject==null && otherPropertyObject==null) continue;
            else if (thisPropertyObject!=null && otherPropertyObject==null) return false;
            else if (thisPropertyObject==null && otherPropertyObject!=null) return false;
            boolean similar=true;
            if (thisPropertyObject instanceof Region && otherPropertyObject instanceof Region) similar=((Region)thisPropertyObject).isIdenticalTo((Region)otherPropertyObject);
            else if (thisPropertyObject instanceof ArrayList && otherPropertyObject instanceof ArrayList) similar=compareArrayLists((ArrayList)thisPropertyObject,(ArrayList)otherPropertyObject);
            else similar=thisPropertyObject.equals(otherPropertyObject);
            if (!similar) return false;
        }
        return true;
    }
    
    /** Reports on the first encountered difference with the other region. Used for debugging */
    private String reportFirstPropertyDifference(Region otherregion) {
        if (this.properties==null && otherregion.properties==null) return "*** No differences (no user-properties) ***";
        else if (this.properties==null && otherregion.properties!=null) return "This region has no properties (other has)";
        else if (this.properties!=null && otherregion.properties==null) return "Other region has no properties (this one has)";
        if (this.properties.size()!=otherregion.properties.size()) return "Different number of user-defined properties";
        for (String key:this.properties.keySet()) {
            if (!otherregion.properties.containsKey(key)) return "Other region missing property:"+key;
            Object thisobject=this.properties.get(key);
            Object otherobject=otherregion.properties.get(key);
            if (thisobject==null && otherobject==null) continue;
            else if (thisobject!=null && otherobject==null) return "Other region property==null:"+key;
            else if (thisobject==null && otherobject!=null) return "This region property==null:"+key;
            boolean similar=true;
            if (thisobject instanceof Region && otherobject instanceof Region) {
                similar=((Region)thisobject).isIdenticalTo((Region)otherobject);
                if (!similar) {
                    String nestedDifference=((Region)thisobject).reportFirstDifference((Region)otherobject);
                    return "Difference in nested region ["+key+"], nested property: "+nestedDifference;
                }
            }
            else if (thisobject instanceof ArrayList && otherobject instanceof ArrayList) {
                similar=compareArrayLists((ArrayList)thisobject,(ArrayList)otherobject);
                if (!similar) return "Difference in arraylist:"+key;
            }
            else similar=thisobject.equals(otherobject);
            if (!similar) return "Difference in property:"+key;
        }
        return "*** No differences ***";
    }

    private boolean compareArrayLists(ArrayList list1, ArrayList list2) {
        if (list1.size()!=list2.size()) return false;
        for (int i=0;i<list1.size();i++) {
            Object o1=list1.get(i);
            Object o2=list2.get(i);
            if (o1 instanceof Region && o2 instanceof Region && !((Region)o1).isIdenticalTo((Region)o2)) return false;
            else if (!o1.equals(o2)) return false;
        }
        return true;
    }

    /**
     * The coordinates of Regions are internally represented as offset coordinates
     * relative to particular frame of reference (usually the parent sequence)
     * rather than as absolute genomic coordinates. This turned out to perhaps
     * not be the best design choice since a Region can sometimes be taken out of
     * its context and be placed in a new frame of reference. This method can be
     * used to update the relative positions of the regions from the old frame
     * to the new frame so that their genomic position will still be same in the new context.
     * @param oldStart The start of the old reference frame (genomic position)
     * @param newStart The start of the new reference frame (genomic position)
     */
    public void updatePositionReferenceFrame(int oldStart, int newStart) {
        int regStart=getRelativeStart()+oldStart; // genomic coordinate
        int regEnd=getRelativeEnd()+oldStart; // genomic coordinate
        setRelativeStart(regStart-newStart);       
        setRelativeEnd(regEnd-newStart); 
        for (Region nested:getNestedRegions(false)) {
            nested.updatePositionReferenceFrame(oldStart, newStart);
        }
    }  
    
    /** The provided offset value will be added to the relative coordinates of this region (and all nested regions also) */
    public void updatePositionReferenceFrame(int offset) {
        int regStart=getRelativeStart(); // genomic coordinate
        int regEnd=getRelativeEnd(); // genomic coordinate
        setRelativeStart(regStart+offset);       
        setRelativeEnd(regEnd+offset); 
        for (Region nested:getNestedRegions(false)) {
            nested.updatePositionReferenceFrame(offset);
        }
    }    
    
    public static int flipOrientation(int orientation) {
        if (orientation==DIRECT) return REVERSE;
        else if (orientation==REVERSE) return DIRECT;
        else return orientation;
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
