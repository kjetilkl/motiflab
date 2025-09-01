/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.motiflab.engine.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.MotifLabClient;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.RegionVisualizationFilter;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class SequenceSorter {
    // These are duplicated in SortSequenceDialog!
    public static final String SORT_BY_SEQUENCE_NAME="Sequence name";
    public static final String SORT_BY_SEQUENCE_LENGTH="Sequence length";
    public static final String SORT_BY_REGION_COUNT="Region count";
    public static final String SORT_BY_REGION_COVERAGE="Region coverage";
    public static final String SORT_BY_VISIBLE_REGION_COUNT="Visible region count";
    public static final String SORT_BY_VISIBLE_REGION_COVERAGE="Visible region coverage";
    public static final String SORT_BY_REGION_SCORES_SUM="Region scores sum";
    public static final String SORT_BY_VISIBLE_REGION_SCORES_SUM="Visible region scores sum";
    public static final String SORT_BY_NUMERIC_MAP="Numeric map";
    public static final String SORT_BY_NUMERIC_TRACK_SUM="Numeric track sum";
    public static final String SORT_BY_GC_CONTENT="GC-content";
    public static final String SORT_BY_MARK="Mark"; 
    public static final String SORT_BY_LOCATION="Location";     
    
    private static HashMap<String,Double> sequencePropertyValues=null;
    private static HashMap<String,Object[]> sequenceLocations=null;
    private static SequencePartition groupByPartition=null;    
    
    /** Returns a list of the modes available for sorting */
    public static String[] getSortOptions() {
       return new String[]{SORT_BY_SEQUENCE_NAME,SORT_BY_SEQUENCE_LENGTH,SORT_BY_LOCATION,SORT_BY_REGION_COUNT,SORT_BY_VISIBLE_REGION_COUNT,SORT_BY_REGION_COVERAGE,SORT_BY_VISIBLE_REGION_COVERAGE,SORT_BY_REGION_SCORES_SUM,SORT_BY_VISIBLE_REGION_SCORES_SUM,SORT_BY_NUMERIC_MAP,SORT_BY_NUMERIC_TRACK_SUM,SORT_BY_GC_CONTENT,SORT_BY_MARK};        
    }    
    
   /**
     * Sorts the sequences in the given collection according to the given parameters and returns TRUE if the sort was successful
     * If the sort is unsuccessful it does not report why, but the method checkParameters(...) can be used to check
     * the parameters and throw an exception upon encountering problems  
     * @param trackName
     * @param map
     * @param compareProperty
     * @param ascending
     * @param partitionName 
     */
     public static synchronized boolean sortBy(SequenceCollection collection, String compareProperty, boolean ascending, String dataName, String partitionName, MotifLabClient client) {
          MotifLabEngine engine=client.getEngine();
          boolean ok=true;
          groupByPartition=null;
          FeatureDataset track=null;
          SequenceNumericMap map=null;
          if (dataName!=null && !dataName.isEmpty()) {
              Data d=engine.getDataItem(dataName);
              if (d instanceof FeatureDataset) track=(FeatureDataset)d;
              else if (d instanceof SequenceNumericMap) map=(SequenceNumericMap)d;
              else ok=false;
          }     
          if (!ok) return ok;
          if (partitionName!=null && !partitionName.isEmpty()) {
            Data pd=engine.getDataItem(partitionName);
            if (pd instanceof SequencePartition) groupByPartition=(SequencePartition)pd;
          }    
          // gather necessary information
               if (compareProperty.equalsIgnoreCase(SORT_BY_SEQUENCE_LENGTH)) ok=getSequenceLengths(engine);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_REGION_COUNT)) ok=countRegions(track);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_REGION_COVERAGE)) ok=findRegionCoverage(track);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_VISIBLE_REGION_COUNT)) ok=countVisibleRegions(track, client);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_VISIBLE_REGION_COVERAGE)) ok=findVisibleRegionCoverage(track, client);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_REGION_SCORES_SUM)) ok=sumRegions(track);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_VISIBLE_REGION_SCORES_SUM)) ok=sumVisibleRegions(track,client);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_NUMERIC_TRACK_SUM)) ok=sumNumeric(track);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_NUMERIC_MAP)) ok=valueNumericMap(map,engine);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_GC_CONTENT)) ok=getGC(track);
          else if (compareProperty.equalsIgnoreCase(SORT_BY_MARK)) ok=getMarks(engine,client.getVisualizationSettings());              
          else if (compareProperty.equalsIgnoreCase(SORT_BY_LOCATION)) ok=getSequenceLocations(engine);              
          
          Comparator comparator=null;
          if (compareProperty.equalsIgnoreCase(SORT_BY_SEQUENCE_NAME) || compareProperty.equalsIgnoreCase("Sequence similarity")) {comparator=null;}
          else if (compareProperty.equalsIgnoreCase(SORT_BY_LOCATION)) {comparator=new LocationComparator();}
          else {comparator=new ValueComparator();}
          
          ArrayList<String> list = collection.getAllSequenceNames();
          if (ok && !list.isEmpty()) {
             Collections.sort(list, new SortOrderComparator(ascending,comparator));
             collection.setSequenceOrder(list);
          } 
          client.getVisualizationSettings().redraw(); // will redraw if the client is a GUI
          sequencePropertyValues=null;
          sequenceLocations=null;          
          groupByPartition=null;  
          return ok;
    }    
     
    /** Checks if the sort parameters are valid and throws an exception if they are not */
    public static void checkParameters(String compareProperty, String dataName, String partitionName, MotifLabEngine engine) throws ExecutionError {
          Data dataitem=null;
          if (dataName!=null && !dataName.isEmpty()) {
              dataitem=engine.getDataItem(dataName);
              if (dataitem==null) throw new ExecutionError("No such data item: "+dataName);
          }     
          if (partitionName!=null && !partitionName.isEmpty()) {
            Data pd=engine.getDataItem(partitionName);
            if (!(pd instanceof SequencePartition)) throw new ExecutionError("'"+partitionName+"' is not a Sequence Partition");
          }
               if (compareProperty.equalsIgnoreCase(SORT_BY_NUMERIC_TRACK_SUM) && !(dataitem instanceof NumericDataset)) throw new ExecutionError("'"+dataName+"' is not a Numeric Dataset");
          else if (compareProperty.equalsIgnoreCase(SORT_BY_NUMERIC_MAP) && !(dataitem instanceof SequenceNumericMap)) throw new ExecutionError("'"+dataName+"' is not a Sequence Numeric Map");
          else if (compareProperty.equalsIgnoreCase(SORT_BY_GC_CONTENT) && !(dataitem instanceof DNASequenceDataset)) throw new ExecutionError("'"+dataName+"' is not a DNA Sequence Dataset");
          
          else if (compareProperty.equalsIgnoreCase(SORT_BY_REGION_COUNT) && !(dataitem instanceof RegionDataset)) throw new ExecutionError("'"+dataName+"' is not a Region Dataset");
          else if (compareProperty.equalsIgnoreCase(SORT_BY_REGION_COVERAGE)&& !(dataitem instanceof RegionDataset)) throw new ExecutionError("'"+dataName+"' is not a Region Dataset");
          else if (compareProperty.equalsIgnoreCase(SORT_BY_VISIBLE_REGION_COUNT)&& !(dataitem instanceof RegionDataset)) throw new ExecutionError("'"+dataName+"' is not a Region Dataset");
          else if (compareProperty.equalsIgnoreCase(SORT_BY_VISIBLE_REGION_COVERAGE)&& !(dataitem instanceof RegionDataset)) throw new ExecutionError("'"+dataName+"' is not a Region Dataset");
          else if (compareProperty.equalsIgnoreCase(SORT_BY_REGION_SCORES_SUM)&& !(dataitem instanceof RegionDataset)) throw new ExecutionError("'"+dataName+"' is not a Region Dataset");
          else if (compareProperty.equalsIgnoreCase(SORT_BY_VISIBLE_REGION_SCORES_SUM)&& !(dataitem instanceof RegionDataset)) throw new ExecutionError("'"+dataName+"' is not a Region Dataset");   
    }     
     
     
   private static class SortOrderComparator implements Comparator<String> {
        private int dir=1;
        private Comparator<String> comparator;

        public SortOrderComparator(boolean ascending, Comparator<String> comparator) {
            if (!ascending) dir=-1;
            this.comparator=comparator;
        }

        @Override
        public int compare(String seq1, String seq2) {
            int result=0;
            if (groupByPartition!=null) result=compareClusters(seq1, seq2);
            if (result==0) { // within same cluster               
                if (comparator==null) result=MotifLabEngine.compareNaturalOrder(seq1, seq2);// compare the names of the sequences
                else result=comparator.compare(seq1, seq2);
            }
            return result*dir;
        }

        public static int compareClusters(String seq1, String seq2) { //
            String cluster1=groupByPartition.getClusterForSequence(seq1);
            String cluster2=groupByPartition.getClusterForSequence(seq2);
            if (cluster1==null && cluster2==null) return 0;
            else if (cluster1==null && cluster2!=null) return 1;
            else if (cluster1!=null && cluster2==null) return -1;
            else return cluster1.compareTo(cluster2);
        }
    }

    private static class ValueComparator implements Comparator<String> {
        @Override
        public int compare(String seq1, String seq2) {
            Double val1=sequencePropertyValues.get(seq1);
            Double val2=sequencePropertyValues.get(seq2);
            if (val1==null || val2==null) return 0; // just in case. This could maybe happen if new sequences are added asynchroneously
            return val1.compareTo(val2);
        }
    }
    
    private static class LocationComparator implements Comparator<String> {
        @Override
        public int compare(String seq1, String seq2) {
            Object[] val1=sequenceLocations.get(seq1);
            Object[] val2=sequenceLocations.get(seq2);
            if (val1==null || val2==null) return 0; // just in case. This could maybe happen if new sequences are added asynchroneously
            int chrOrder=MotifLabEngine.compareNaturalOrder((String)val1[0], (String)val2[0]);
            if (chrOrder!=0) return chrOrder;
            int pos1=(Integer)val1[1];
            int pos2=(Integer)val2[1];           
            int order=(pos1 < pos2) ? -1 : ((pos1 == pos2) ? 0 : 1);
            if (order!=0) return order;
            pos1=(Integer)val1[2];
            pos2=(Integer)val2[2];         
            return (pos1 < pos2) ? -1 : ((pos1 == pos2) ? 0 : 1);          
        }
    }    


    private static boolean countRegions(FeatureDataset dataset) {
        if (dataset==null || !(dataset instanceof RegionDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            int count=((RegionSequenceData)seq).getNumberOfRegions();
            sequencePropertyValues.put(seq.getSequenceName(), new Double(count));
        }
        return true;
    }
    
    private static boolean findRegionCoverage(FeatureDataset dataset) {
        if (dataset==null || !(dataset instanceof RegionDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            int count=((RegionSequenceData)seq).getBaseCoverage();
            double ratio=(double)count/(double)seq.getSize();
            sequencePropertyValues.put(seq.getSequenceName(), new Double(ratio));
        }
        return true;
    }    
    
   private static boolean findVisibleRegionCoverage(FeatureDataset dataset, MotifLabClient client) {
        if (dataset==null || !(dataset instanceof RegionDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        //boolean ismotiftrack=((RegionDataset)dataset).isMotifTrack();
        VisualizationSettings settings=client.getVisualizationSettings();
        RegionVisualizationFilter filter=(client instanceof MotifLabGUI)?(((MotifLabGUI)client).getRegionVisualizationFilter()):null;
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            ArrayList<Region> visibleregions=new ArrayList<Region>();
            if (settings.isSequenceVisible(seq.getName())) {
                for (Region region:((RegionSequenceData)seq).getOriginalRegions()) {
                    if (settings.isRegionTypeVisible(region.getType()) && (filter==null || filter.shouldVisualizeRegion(region))) visibleregions.add(region);                   
                }
            } 
            visibleregions=((RegionSequenceData)seq).flattenRegions(visibleregions);
            int bases=0;
            for (Region reg:visibleregions) bases+=reg.getLength();
            double ratio=(double)bases/(double)seq.getSize();
            sequencePropertyValues.put(seq.getSequenceName(), new Double(ratio));
        }
        return true;
    }
   
   private static boolean countVisibleRegions(FeatureDataset dataset, MotifLabClient client) {
        if (dataset==null || !(dataset instanceof RegionDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        //boolean ismotiftrack=((RegionDataset)dataset).isMotifTrack();
        VisualizationSettings settings=client.getVisualizationSettings();
        RegionVisualizationFilter filter=(client instanceof MotifLabGUI)?(((MotifLabGUI)client).getRegionVisualizationFilter()):null;
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            int count=0;
            if (settings.isSequenceVisible(seq.getName())) {
                for (Region region:((RegionSequenceData)seq).getOriginalRegions()) {
                    if (settings.isRegionTypeVisible(region.getType()) && (filter==null || filter.shouldVisualizeRegion(region))) count++;                    
                }
            }           
            sequencePropertyValues.put(seq.getSequenceName(), new Double(count));
        }
        return true;
    }   

    private static boolean sumRegions(FeatureDataset dataset) {
        if (dataset==null || !(dataset instanceof RegionDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            double sum=0;
            for (Region r:((RegionSequenceData)seq).getAllRegions()) {
                sum+=r.getScore();
            }
            sequencePropertyValues.put(seq.getSequenceName(), new Double(sum));
        }
        return true;
    }
    
    private static boolean sumVisibleRegions(FeatureDataset dataset, MotifLabClient client) {
        if (dataset==null || !(dataset instanceof RegionDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        VisualizationSettings settings=client.getVisualizationSettings();
        RegionVisualizationFilter filter=(client instanceof MotifLabGUI)?(((MotifLabGUI)client).getRegionVisualizationFilter()):null;
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            double sum=0;
            if (settings.isSequenceVisible(seq.getName())) {
                for (Region region:((RegionSequenceData)seq).getOriginalRegions()) {
                    if (settings.isRegionTypeVisible(region.getType()) && (filter==null || filter.shouldVisualizeRegion(region))) sum+=region.getScore();                   
                }
            }  
            sequencePropertyValues.put(seq.getSequenceName(), new Double(sum));
        }
        return true;
    }    

    private static boolean sumNumeric(FeatureDataset dataset) {
        if (dataset==null || !(dataset instanceof NumericDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            double sum=0;
            NumericSequenceData nseq=(NumericSequenceData)seq;
            for (int i=0;i<seq.getSize();i++) {
                sum+=nseq.getValueAtRelativePosition(i);
            }
            sequencePropertyValues.put(seq.getSequenceName(), new Double(sum));
        }
        return true;
    }

    private static boolean valueNumericMap(SequenceNumericMap map, MotifLabEngine engine) {
        if (map==null) return false;
        SequenceCollection collection=engine.getDefaultSequenceCollection();
        sequencePropertyValues=new HashMap<String,Double>(collection.size());
        for (String sequenceName:collection.getAllSequenceNames()) {
            sequencePropertyValues.put(sequenceName, map.getValue(sequenceName).doubleValue());
        }
        return true;
    }

    private static boolean getGC(FeatureDataset dataset) {
        if (dataset==null || !(dataset instanceof DNASequenceDataset)) return false;
        sequencePropertyValues=new HashMap<String,Double>(dataset.getNumberofSequences());
        for (FeatureSequenceData seq:dataset.getAllSequences()) {
            double GC=0;
            DNASequenceData dnaseq=(DNASequenceData)seq;
            for (int i=0;i<seq.getSize();i++) {
                char base=dnaseq.getValueAtRelativePosition(i);
                if (base=='G' || base=='g' || base=='C' || base=='c') GC+=1;
            }
            sequencePropertyValues.put(seq.getSequenceName(), new Double(GC/(double)seq.getSize()));
        }
        return true;
    }

    private static boolean getSequenceLocations(MotifLabEngine engine) {
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        sequenceLocations=new HashMap<String,Object[]>(sequences.size());
        for (Data seq:sequences) {
            sequenceLocations.put(seq.getName(),new Object[]{((Sequence)seq).getChromosome(),((Sequence)seq).getRegionStart(),((Sequence)seq).getRegionEnd()});
        }
        return true;
    }    
    
    private static boolean getSequenceLengths(MotifLabEngine engine) {
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        sequencePropertyValues=new HashMap<String,Double>(sequences.size());
        for (Data seq:sequences) {
            sequencePropertyValues.put(seq.getName(), new Double(((Sequence)seq).getSize()));
        }
        return true;
    }

    private static boolean getMarks(MotifLabEngine engine,VisualizationSettings settings) {
        ArrayList<String> sequenceNames=engine.getNamesForAllDataItemsOfType(Sequence.class);
        sequencePropertyValues=new HashMap<String,Double>(sequenceNames.size());
        for (String seq:sequenceNames) {
            Double sortvalue=(settings.isSequenceMarked(seq))?1.0:0.0;
            sequencePropertyValues.put(seq, sortvalue);
        }
        return true;
    }
    
    
     
    
}
