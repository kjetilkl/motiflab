package motiflab.engine.util;

import java.util.ArrayList;
import java.util.Collection;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.RegionVisualizationFilter;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class MotifPositionHistogramComputer {

    public static final int ALIGN_UPSTREAM=0;
    public static final int ALIGN_DOWNSTREAM=1;
    public static final int ALIGN_TSS=2;
    public static final int ALIGN_TES=3;
    public static final int ALIGN_CENTER=4;    
    
    public static final int ANCHOR_UPSTREAM=0;
    public static final int ANCHOR_DOWNSTREAM=1;
    public static final int ANCHOR_CENTER=2;
    public static final int ANCHOR_SPAN=3;
 
    
    private int align=ALIGN_TSS;
    private int anchor=ANCHOR_SPAN;
    private RegionDataset dataset=null;  
    private VisualizationSettings settings=null;
    private MotifLabGUI gui;
    private int relativeStart=0;
    private int relativeEnd=0;
    private boolean flatten=false;
    
    public  MotifPositionHistogramComputer(MotifLabGUI gui, int align, int anchor, boolean flatten, RegionDataset dataset) {
       this.gui=gui;
       this.settings=(gui!=null)?gui.getVisualizationSettings():null;
       this.align=align;
       this.anchor=anchor;
       this.flatten=flatten;
       this.dataset=dataset;
    }
    
    public void setAnchor(int anchor) {
        this.anchor=anchor;
    }
    public void setAlignment(int alignment) {
        this.align=alignment;
    }

    public void setFlatten(boolean flatten) {
        this.flatten=flatten;
    }
    
    /**
     * Updates the counts with 
     * @param counts An array which will be filled with the (normalized) histogram counts (required)
     * @param typefilter If specified (not null) only regions of this type will be considered
     * @param onlyVisible only consider currently visible regions 
     */
    public void countRegions(double[] counts, String typefilter, Collection<String> sequenceFilter, boolean onlyVisible) {       
        int total=0;
        int span=0;
        if (settings==null) onlyVisible=false; // to avoid NullPointers later on...
        try {
            span=determineSequenceSpan(dataset, sequenceFilter, onlyVisible);
        } catch (Exception e) {return;}
        double binspan=(double)span/(double)counts.length;        
        RegionVisualizationFilter filter=(onlyVisible)?gui.getRegionVisualizationFilter():null;
        for (int i=0;i<counts.length;i++) counts[i]=0; // clear counts
        //boolean isMotiforModuleTrack=(dataset.isMotifTrack()||dataset.isModuleTrack());
        int seqCount=0;
        boolean[] flattenedCounts=(flatten)?new boolean[counts.length]:null;
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
           if (onlyVisible && !settings.isSequenceVisible(seq.getSequenceName())) continue;
           if (sequenceFilter!=null && !sequenceFilter.contains(seq.getSequenceName())) continue;
           if (flattenedCounts!=null) for (int i=0;i<flattenedCounts.length;i++) flattenedCounts[i]=false; // reset flattened counts
           ArrayList<Region> list=((RegionSequenceData)seq).getAllRegions();
           boolean reverse=(seq.getStrandOrientation()==Sequence.REVERSE);
           for (Region region:list) {
                if (typefilter!=null && !region.getType().equals(typefilter)) continue;
                if (onlyVisible) {
                    if (!settings.isRegionTypeVisible(region.getType())) continue;
                    if (filter!=null && !filter.shouldVisualizeRegion(region)) continue;
                }
                if (anchor==ANCHOR_SPAN) { // can span multiple bins
                   int startBinIndex=getBinForRegion(seq, region, ANCHOR_UPSTREAM, align, reverse, binspan); 
                   int endBinIndex=getBinForRegion(seq, region, ANCHOR_DOWNSTREAM, align, reverse, binspan); 
                   if (startBinIndex>endBinIndex) {int swap=startBinIndex;startBinIndex=endBinIndex;endBinIndex=swap;}
                   if (endBinIndex<0 || startBinIndex>=counts.length) continue; // region is fully outside
                   if (startBinIndex<0) startBinIndex=0;
                   if (endBinIndex>=counts.length) endBinIndex=counts.length-1;
                   if (flatten) {
                      for (int i=startBinIndex;i<=endBinIndex;i++) flattenedCounts[i]=(flattenedCounts[i]||true); // count each bin only once
                   } else { // anchor==ANCHOR_FLATTEN
                      for (int i=startBinIndex;i<=endBinIndex;i++) counts[i]+=1;
                   }
                   total++;
                } else { // single bin
                    int binIndex=getBinForRegion(seq, region, anchor, align, reverse, binspan);
                    if (binIndex>=0 && binIndex<counts.length) {
                        if (flatten) flattenedCounts[binIndex]=(flattenedCounts[binIndex]||true);
                        else counts[binIndex]+=1;
                        total++;
                    } 
                }
           }
           if (flattenedCounts!=null) { // increase counts from flattenedCounts
              for (int i=0;i<flattenedCounts.length;i++) {
                  if (flattenedCounts[i]) counts[i]++;
              } //
           }
           seqCount++;
        }
        // normalize bins
        double divideby=(flatten)?(double)seqCount:(double)total;
        for (int i=0;i<counts.length;i++) {
            counts[i]=counts[i]/divideby;
        }    
    }

    private int getBinForRegion(FeatureSequenceData seq, Region region, int anchor, int align, boolean reverse, double binspan) {
        double relativepos=0; // this will usually be an integer
        if (anchor==ANCHOR_CENTER) relativepos=(reverse)?((seq.getSize()-1)-(region.getRelativeStart()+(double)region.getLength()/2.0)):(region.getRelativeStart()+(double)region.getLength()/2.0);
        else if (anchor==ANCHOR_UPSTREAM) relativepos=(reverse)?((seq.getSize()-1)-region.getRelativeEnd()):(region.getRelativeStart());
        else if (anchor==ANCHOR_DOWNSTREAM) relativepos=(reverse)?((seq.getSize()-1)-region.getRelativeStart()):(region.getRelativeEnd());
        // have relative position of anchor point wrt upstream start of sequence
        if (align==ALIGN_DOWNSTREAM) {
            double basesfromend=seq.getSize()-relativepos;
            relativepos=-(relativeStart+basesfromend);           
        } else if (align==ALIGN_CENTER) {
           int center=(int)((seq.getRegionStart()+seq.getRegionEnd())/2.0);
           int sequpstream=(reverse)?(seq.getRegionEnd()-center):(center-seq.getRegionStart());
           int seqOffset=Math.abs(relativeStart)-sequpstream;
           relativepos+=seqOffset;           
        } else if (align==ALIGN_TSS) {
           int tss=seq.getTSS().intValue();
           int sequpstream=(reverse)?(seq.getRegionEnd()-tss):(tss-seq.getRegionStart());
           int seqOffset=Math.abs(relativeStart)-sequpstream;
           relativepos+=seqOffset;
        } else if (align==ALIGN_TES) {
           int tss=seq.getTES().intValue();
           int sequpstream=(reverse)?(seq.getRegionEnd()-tss):(tss-seq.getRegionStart());
           int seqOffset=Math.abs(relativeStart)-sequpstream;
           relativepos+=seqOffset;                   
        }
        int binIndex=(int)(relativepos/binspan);  
        return binIndex;
    }


    /** Determines the total sequence span based on the length of the sequences and selected alignment */
    private int determineSequenceSpan(RegionDataset dataset, Collection<String> sequenceFilter, boolean onlyVisible) throws ExecutionError {
         if (align==ALIGN_UPSTREAM || align==ALIGN_DOWNSTREAM || align==ALIGN_CENTER) {
             int length=0;
             for (FeatureSequenceData seq:dataset.getAllSequences()) {
                 if (onlyVisible && !settings.isSequenceVisible(seq.getSequenceName())) continue;
                 if (sequenceFilter!=null && !sequenceFilter.contains(seq.getSequenceName())) continue;                 
                 int seqlen=seq.getSize();
                 if (seqlen>length) length=seqlen;
             }
             if (align==ALIGN_UPSTREAM) {relativeStart=0;relativeEnd=length;}
             else if (align==ALIGN_DOWNSTREAM) {relativeStart=-length;relativeEnd=0;}
             else {int halfsize=(int)(length/2.0);relativeStart=-halfsize;relativeEnd=-halfsize+length;} // maybe this is one to big :|            
             return length;
         } else if (align==ALIGN_TSS) {
             int upstream=0;
             int downstream=0;
             for (FeatureSequenceData seq:dataset.getAllSequences()) {
                 if (onlyVisible && !settings.isSequenceVisible(seq.getSequenceName())) continue;
                 if (sequenceFilter!=null && !sequenceFilter.contains(seq.getSequenceName())) continue;                 
                 Integer TSS=seq.getTSS();
                 if (TSS==null) throw new ExecutionError("Missing TSS");
                 int tss=TSS.intValue();
                 boolean direct=seq.isOnDirectStrand();
                 int sequpstream=(direct)?(tss-seq.getRegionStart()):(seq.getRegionEnd()-tss);
                 int seqdownstream=(direct)?(seq.getRegionEnd()-tss):(tss-seq.getRegionStart());
                 if (sequpstream>upstream) upstream=sequpstream;
                 if (seqdownstream>downstream) downstream=seqdownstream;
             }
             relativeStart=-upstream;
             relativeEnd=downstream;            
             return upstream+downstream+1; // +1 is for TSS
         } else if (align==ALIGN_TES) {
             int upstream=0;
             int downstream=0;
             for (FeatureSequenceData seq:dataset.getAllSequences()) {
                 if (onlyVisible && !settings.isSequenceVisible(seq.getSequenceName())) continue;
                 if (sequenceFilter!=null && !sequenceFilter.contains(seq.getSequenceName())) continue;                 
                 Integer TES=seq.getTES();
                 if (TES==null) throw new ExecutionError("Missing TES");
                 int tes=TES.intValue();
                 boolean direct=seq.isOnDirectStrand();
                 int sequpstream=(direct)?(tes-seq.getRegionStart()):(seq.getRegionEnd()-tes);
                 int seqdownstream=(direct)?(seq.getRegionEnd()-tes):(tes-seq.getRegionStart());
                 if (sequpstream>upstream) upstream=sequpstream;
                 if (seqdownstream>downstream) downstream=seqdownstream;
             }
             relativeStart=-upstream;
             relativeEnd=downstream;
             return upstream+downstream+1; // +1 is for TES
         } else return 0; // this should not happen!
    }
        
}
