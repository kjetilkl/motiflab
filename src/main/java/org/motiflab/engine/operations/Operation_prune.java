/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.util.MotifComparator;
import org.motiflab.engine.util.MotifComparator_PearsonsCorrelationICWeighted;

/**
 *
 * @author kjetikl
 */
public class Operation_prune extends FeatureTransformOperation {
    public static final String PRUNE="prune";
    public static final String KEEP="keep";
    public static final String MOTIFPARTITION="motifpartition";

    public static final String PRUNE_PALINDROMES="palindromes";
    public static final String PRUNE_ALTERNATIVES="alternatives";
    public static final String PRUNE_ALTERNATIVES_NAIVE="naive alternatives";
    public static final String PRUNE_SIMILAR="similar";    
    public static final String PRUNE_DUPLICATES="duplicates";
    public static final String KEEP_DIRECT_STRAND="direct strand";
    public static final String KEEP_RELATIVE_STRAND="relative strand";
    public static final String KEEP_TOP_SCORING="top scoring";
    public static final String KEEP_HIGH_IC="highest IC";
    public static final String KEEP_FIRST_SORTED_NAME="first sorted name";
//    public static final String KEEP_CLUSTER_REP_HIGH_IC="cluster representative (highest IC)";
//    public static final String KEEP_CLUSTER_REP_FIRST_SORTED_NAME="cluster representative (first sorted name)";

    private static final String name="prune";
    private static final String description="Removes duplicate overlapping regions";
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class};
    
    @Override
    public String getOperationGroup() {
        return "Transform";
    }

    public static String[] getPruneOptions() {
        return new String[]{PRUNE_DUPLICATES,PRUNE_SIMILAR,PRUNE_PALINDROMES,PRUNE_ALTERNATIVES,PRUNE_ALTERNATIVES_NAIVE};
    }
    public static String[] getKeepOptions(String pruneSelection) {
        if (pruneSelection.equals(PRUNE_ALTERNATIVES) || pruneSelection.equals(PRUNE_ALTERNATIVES_NAIVE)) return new String[]{KEEP_TOP_SCORING,KEEP_HIGH_IC,KEEP_FIRST_SORTED_NAME};
        else if (pruneSelection.equals(PRUNE_PALINDROMES)) return new String[]{KEEP_TOP_SCORING,KEEP_DIRECT_STRAND,KEEP_RELATIVE_STRAND};
        else return new String[0];
    }    

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {return name;}

    @Override
    public boolean isSubrangeApplicable() {return true;}

    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        String keepOption=(String)task.getParameter(KEEP);
        if (keepOption!=null && keepOption.equals(KEEP_HIGH_IC)) {
            HashMap<String,Double> ICmap=getICmap();
            task.setParameter("_ICmap", ICmap);
        } else if (keepOption==null) keepOption=KEEP_TOP_SCORING;
        
        String pruneOption=(String)task.getParameter(PRUNE);
        if (pruneOption!=null && (pruneOption.equals(PRUNE_ALTERNATIVES) || pruneOption.equals(PRUNE_ALTERNATIVES_NAIVE))) {
            if (!(task.getSourceData() instanceof RegionDataset && ((RegionDataset)task.getSourceData()).isMotifTrack())) throw new ExecutionError("Prune 'alternatives' can only be applied to motif tracks"); 
            MotifPartition partition=null;            
            String motifpartitionname=(String)task.getParameter(MOTIFPARTITION);
            if (motifpartitionname==null || motifpartitionname.isEmpty()) { // create partition from known alternatives               
                partition=MotifPartition.createPartitionBasedOnAlternatives("temp", null, engine);
            } else {
                Data motifpart=engine.getDataItem(motifpartitionname);
                if (!(motifpart instanceof MotifPartition)) throw new ExecutionError("'"+motifpartitionname+"' is not a Motif Partition");
                else partition=(MotifPartition)motifpart;
            }
            task.setParameter("_motifpartition", partition); // the '_motifpartition' key is only used internally in this class
            if (pruneOption.equals(PRUNE_ALTERNATIVES)) task.setParameter("_optimalAlignments", getOptimalAlignments((MotifPartition)partition));                     
            task.setParameter("_longestMotif", getLongestMotif());
        }
        if (pruneOption!=null && pruneOption.equals(PRUNE_PALINDROMES)) {
            if (!(task.getSourceData() instanceof RegionDataset && ((RegionDataset)task.getSourceData()).isMotifTrack())) throw new ExecutionError("Prune 'palindromes' can only be applied to motif tracks");            
            task.setParameter("_longestMotif", getLongestMotif());
            task.setParameter("_coreRegions", getMotifCoreRegions());            
        }
    }

    private int getLongestMotif() {
        int longest=0;
        for (Data data:engine.getAllDataItemsOfType(Motif.class)) {
            Motif motif=(Motif)data;
            if (motif.getLength()>longest) longest=motif.getLength();
        }
        return longest;
    }
    
    private HashMap<String, int[]> getMotifCoreRegions() {
        HashMap<String, int[]> cores=new HashMap<String, int[]>();
        for (Data data:engine.getAllDataItemsOfType(Motif.class)) {
            Motif motif=(Motif)data;
            cores.put(motif.getName(), motif.getCoreRegion());
        }
        return cores;
    }    

    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        MotifPartition partition=(MotifPartition)task.getParameter("_motifpartition"); // the '_motifpartition' key is only used internally in this class
        String pruneOption=(String)task.getParameter(PRUNE);
        String keepOption=(String)task.getParameter(KEEP);
        ArrayList<Region> list = ((RegionSequenceData)targetSequence).getAllRegions();
        int size=list.size();
        if (size<=1) return; // no need to prune
        String seqName=targetSequence.getName();
        ArrayList<Region> toberemoved=new ArrayList<Region>();
        Integer longestMotif=(Integer)task.getParameter("_longestMotif");
        // find clusters to be pruned 
        if (pruneOption.equals(PRUNE_PALINDROMES)) {
            HashMap<String, int[]> cores=(HashMap<String, int[]>)task.getParameter("_coreRegions");
            for (int i=1;i<list.size();i++) { // i is starting at 1, but will be compared to j=0 
                Region target=list.get(i);
                String targetType=target.getType();
                int start=target.getRelativeStart();
                int end=target.getRelativeEnd();
                int targetOrientation=target.getOrientation();
                for (int j=i-1;j>=0;j--) {
                    Region other=list.get(j);
                    if (other.getRelativeStart()+longestMotif<start) break; // not even overlapping
                    if (!other.getType().equals(targetType)) continue;
                    if (other.getOrientation()==targetOrientation) continue; // not oppositely oriented
                    if (other.getRelativeStart()==start && other.getRelativeEnd()==end) { // exact overlap of same type region in opposite directions
                        toberemoved.add(getPalindromeToRemove(target, other ,sourceSequence.getStrandOrientation(), keepOption));
                    } else { // check whether the cores overlap
                        int[] core=cores.get(targetType);
                        int startmargin=core[0];
                        int endmargin=target.getLength()-(core[1]+1);
                        int coresize=core[1]-core[0]+1;
                        int targetCoreStart=0;
                        int targetCoreEnd=0;
                        int otherCoreStart=0;
                        int otherCoreEnd=0;
                        if (targetOrientation==Region.DIRECT) {
                            targetCoreStart=start+core[0];
                            targetCoreEnd=start+core[1];
                            otherCoreStart=other.getRelativeStart()+endmargin;
                            otherCoreEnd=other.getRelativeStart()+endmargin+coresize-1;
                        } else {
                            targetCoreStart=start+endmargin;
                            targetCoreEnd=start+endmargin+coresize-1;
                            otherCoreStart=other.getRelativeStart()+core[0];
                            otherCoreEnd=other.getRelativeStart()+core[1];                          
                        }
                        if (targetCoreStart==otherCoreStart && targetCoreEnd==otherCoreEnd) { // core regions match (in opposite directions)
                            toberemoved.add(getPalindromeToRemove(target, other, sourceSequence.getStrandOrientation(), keepOption));
                        }
                    }
                }
            }
        } else {
            HashMap<String,Integer[]> optimalAlignments=(HashMap<String,Integer[]>)task.getParameter("_optimalAlignments");            
            ArrayList<OverlappingSimilarMotifsCluster> clustersOfOverlappingMotifs=null; // This is a list of clusters, where each cluster consists of regions representing the same TFBS. In each of these clusters, all but one of the regions should be removed from the dataset.
                 if (pruneOption.equals(PRUNE_ALTERNATIVES)) clustersOfOverlappingMotifs=getAlternativesClusters(list, partition, optimalAlignments, (longestMotif==null)?0:longestMotif.intValue());
            else if (pruneOption.equals(PRUNE_ALTERNATIVES_NAIVE)) clustersOfOverlappingMotifs=getNaiveAlternativesClusters(list, partition, (longestMotif==null)?0:longestMotif.intValue());
            else if (pruneOption.equals(PRUNE_DUPLICATES)) clustersOfOverlappingMotifs=getDuplicatesClusters(list);
            else if (pruneOption.equals(PRUNE_SIMILAR)) {clustersOfOverlappingMotifs=getSimilarRegionsClusters(list);keepOption=KEEP_TOP_SCORING;}
            else throw new ExecutionError("Unrecognized prune option: "+pruneOption);
            // Select single a canonical representative for each cluster and mark the non-canonical regions in each cluster for removal
            Object keepArgument=null;
            if (keepOption!=null) {
                if (keepOption.equals(KEEP_RELATIVE_STRAND)) keepArgument=sourceSequence.getStrandOrientation();
                else if (keepOption.equals(KEEP_HIGH_IC)) keepArgument=task.getParameter("_ICmap");
            }           
            for (OverlappingSimilarMotifsCluster cluster:clustersOfOverlappingMotifs) {
                if (cluster.size()==1) continue; // no need to prune this
                Region canonical=cluster.getCanonicalRepresentative(keepOption,keepArgument); // Note that the canonical region is kept even if it does not satisfy the condition
                 for (Region region:cluster.getRegions()) { // non-canonical regions are pruned only if they satisfy the condition                   
                    if (region!=canonical && regionSatisfiesCondition(seqName, region, task)) toberemoved.add(region); // mark non-canonical regions in the cluster for removal
                }
            }
        }
        // remove marked regions (these also satisfy the 'where' condition)
        for (Region region:toberemoved) {
            ((RegionSequenceData)targetSequence).removeRegion(region);
        }
    }

    
    private Region getPalindromeToRemove(Region region1, Region region2, int sequenceOrientation, String keepOption) {
        // note that we return the region which should be removed not the one which should be kept!
        if (keepOption.equals(KEEP_TOP_SCORING)) {
            if (region1.getScore()>region2.getScore()) return region2;
            else if (region1.getScore()<region2.getScore()) return region1;
            else if (region1.getOrientation()==sequenceOrientation) return region2; // same score? return region on relative strand
            else return region1;
        } else if (keepOption.equals(KEEP_DIRECT_STRAND)) {
            if (region1.getOrientation()==Region.DIRECT) return region2; else return region1;
        } else if (keepOption.equals(KEEP_RELATIVE_STRAND)) {
            if (region1.getOrientation()==sequenceOrientation) return region2; else return region1;
        } else return region1; // this should not happen if the keep option is legal
    }
    /**
     * Returns a list of clusters (Region-lists) for overlapping alternative motifs.
     * @return
     */
    private ArrayList<OverlappingSimilarMotifsCluster> getAlternativesClusters(ArrayList<Region> list, MotifPartition partition, HashMap<String,Integer[]> optimalAlignments, int longestMotif) throws Exception {
        ArrayList<OverlappingSimilarMotifsCluster> clusters=new ArrayList<OverlappingSimilarMotifsCluster>();
        for (Region region:list) {
            if (clusters.isEmpty()) clusters.add(new OverlappingSimilarMotifsCluster(region,partition,optimalAlignments)); // just to get things started
            else {
                boolean added=false;
                int lastcluster=clusters.size()-1;
                for (int i=lastcluster;i>=0;i--) {
                    OverlappingSimilarMotifsCluster candidatecluster=clusters.get(i);
                    if (candidatecluster.fitsInCluster(region,partition,optimalAlignments)) {candidatecluster.addRegion(region);added=true;break;}
                    else { // try to break early if possible...
                        int clusterEnd=candidatecluster.getClusterEnd();
                        if (clusterEnd+longestMotif<region.getRelativeStart()) {
                            break;
                        } // this region will not fit in any previous cluster
                   }
                }
                if (!added) clusters.add(new OverlappingSimilarMotifsCluster(region,partition,optimalAlignments)); // Create new cluster for this region
            }
        }
        return clusters;
    }

     private ArrayList<OverlappingSimilarMotifsCluster> getDuplicatesClusters(ArrayList<Region> list) throws Exception {
        ArrayList<OverlappingSimilarMotifsCluster> clusters=new ArrayList<OverlappingSimilarMotifsCluster>();
        for (Region region:list) {
            if (clusters.isEmpty()) clusters.add(new OverlappingSimilarMotifsCluster(region,null,null)); // just to get things started
            else {
                boolean added=false;
                int lastcluster=clusters.size()-1;
                for (int i=lastcluster;i>=0;i--) {
                    OverlappingSimilarMotifsCluster candidatecluster=clusters.get(i);
                    int clusterstart=candidatecluster.getClusterStart();
                    if (clusterstart<region.getRelativeStart()) break; // if the start positions are different the regions can not be in the same cluster
                    if (region.isIdenticalTo(candidatecluster.getCanonicalRepresentative(null,null))) {candidatecluster.addRegion(region);added=true;break;}
                }
                if (!added) clusters.add(new OverlappingSimilarMotifsCluster(region,null,null)); // Create new cluster for this region
            }
        }
        return clusters;
    }
     
      private ArrayList<OverlappingSimilarMotifsCluster> getSimilarRegionsClusters(ArrayList<Region> list) throws Exception {
        ArrayList<OverlappingSimilarMotifsCluster> clusters=new ArrayList<OverlappingSimilarMotifsCluster>();
        for (Region region:list) {
            if (clusters.isEmpty()) clusters.add(new OverlappingSimilarMotifsCluster(region,null,null)); // just to get things started
            else {
                boolean added=false;
                int lastcluster=clusters.size()-1;
                for (int i=lastcluster;i>=0;i--) {
                    OverlappingSimilarMotifsCluster candidatecluster=clusters.get(i);
                    int clusterstart=candidatecluster.getClusterStart();
                    if (clusterstart<region.getRelativeStart()) break; // if the start positions are different the regions can not be in the same cluster
                    if (region.hasSameLocationAndType(candidatecluster.getCanonicalRepresentative(KEEP_TOP_SCORING,null))) {candidatecluster.addRegion(region);added=true;break;}
                }
                if (!added) clusters.add(new OverlappingSimilarMotifsCluster(region,null,null)); // Create new cluster for this region
            }
        }
        return clusters;
    }    
      
    /**
     * Returns a list of clusters (Region-lists) for overlapping alternative motifs.
     * @return
     */
    private ArrayList<OverlappingSimilarMotifsCluster> getNaiveAlternativesClusters(ArrayList<Region> list, MotifPartition partition, int longestMotif) throws Exception {
        ArrayList<OverlappingSimilarMotifsCluster> clusters=new ArrayList<OverlappingSimilarMotifsCluster>();
        for (Region region:list) {
            if (clusters.isEmpty()) clusters.add(new OverlappingSimilarMotifsCluster(region,partition,null)); // just to get things started
            else {
                boolean added=false;
                int lastcluster=clusters.size()-1;
                for (int i=lastcluster;i>=0;i--) {
                    OverlappingSimilarMotifsCluster candidatecluster=clusters.get(i);
                    if (candidatecluster.fitsInClusterNaive(region,partition)) {candidatecluster.addRegion(region);added=true;break;}
                    else { // try to break early if possible...
                        int clusterEnd=candidatecluster.getClusterEnd();
                        if (clusterEnd+longestMotif<region.getRelativeStart()) {
                            break;
                        } // this region will not fit in any previous cluster
                   }
                }
                if (!added) clusters.add(new OverlappingSimilarMotifsCluster(region,partition,null)); // Create new cluster for this region
            }
        }
        return clusters;
    }      
         
    /**
     * Returns a map which gives the optimal alignment for each motif
     * against its assigned canonical reference (usually the motif in each cluster
     * with highest IC (and first sorted name) or the motif itself for palindromes)
     * The returned alignment value is given as an Integer[]{offset direct,offset reverse, best match, targetMotifLength}
     * The third integer value is 1 if the best match was found with the second motif in direct orientation or -1 if the better match was in reverse orientation
     * @param partition If provided, each motif will be compared against the canonical
     *                  motif in its cluster
     * @return A HashMap where the key is the name of a motif and the value contains information about the motif's optimal alignment with the cluster's selected canonical motif
     */
    private HashMap<String,Integer[]> getOptimalAlignments(MotifPartition partition) {
        HashMap<String,Integer[]> results=new HashMap<String, Integer[]>();
        MotifComparator motifcomparator=new MotifComparator_PearsonsCorrelationICWeighted();
        if (partition!=null) {
            for (String clustername:partition.getClusterNames()) { // for each cluster
                ArrayList<String> cluster=partition.getAllMembersInCluster(clustername);
                String representative=getClusterRepresentativeHighIC(cluster);
                if (!engine.dataExists(representative, Motif.class)) continue; // representative is not a Motif. Skip this cluster
                Motif target=(Motif)engine.getDataItem(representative);
                int targetMotifLength=target.getLength();
                for (String entry:cluster) {
                    Data entrydata=engine.getDataItem(entry);
                    if (entrydata instanceof Motif) {
                        double[] comp=motifcomparator.compareMotifsBothDirections(target, (Motif)entrydata);
                        int directOffset=(int)comp[1];
                        int reverseOffset=(int)comp[3];
                        results.put(entry,new Integer[]{directOffset,reverseOffset,(comp[0]>=comp[2])?1:-1,targetMotifLength}); // third entry marks direction of best match, fourth entry is length of canonical motif
                    }
                }
            }
        } else { 
            // partition==null. There is nothing for us to do...
        }
        return results;
    }

    /** Returns a Map where each key is the name of a cluster in the given partition
     *  and each value is the name of the motif which should function as the representative motif
     *  for that cluster
     *  @param partition
     *  @param highIC  If TRUE, the selected representative will be the motif with highest IC-content
     *                 (in case of a tie the motif with first sorted name will be returned).
     *                 If FALSE, the motif with the first sorted name will be chosen as the representative
     */
    private HashMap<String,String> getClusterRepresentatives(MotifPartition partition, boolean highIC) {
        HashMap<String,String> result=new HashMap<String,String>(partition.getNumberOfClusters());
        for (String clustername:partition.getClusterNames()) {
            ArrayList<String> cluster=partition.getAllMembersInCluster(clustername);
            String representative=(highIC)?getClusterRepresentativeHighIC(cluster):getClusterRepresentativeFirstSortedName(cluster);
            result.put(clustername,representative);
        }
        return result;
    }

    private String getClusterRepresentativeHighIC(ArrayList<String> cluster) {
        if (cluster.size()==1) return cluster.get(0);
        String best="error";
        double highIC=0;
        for (String entry:cluster) {
            Data data=engine.getDataItem(entry);
            if (data instanceof Motif) {
                double IC=((Motif)data).getICcontent();
                if (IC>highIC) {highIC=IC;best=entry;}
                else if (IC==highIC && entry.compareTo(best)<0) {best=entry;}
            }
        }
        return best;
    }

    private String getClusterRepresentativeFirstSortedName(ArrayList<String> cluster) {
        if (cluster.size()==1) return cluster.get(0);
        String best=cluster.get(0);
        for (int i=1;i<cluster.size();i++) {
            String next=cluster.get(i);
            if (next.compareTo(best)<0) best=next;
        }
        return best;
    }
  
    /** Calculates IC-content for each motif and stores them in a LUT*/
    private HashMap<String,Double> getICmap() {
       HashMap<String,Double> ICmap=new HashMap<String, Double>();
       for (Data data:engine.getAllDataItemsOfType(Motif.class)) {
          Motif motif=(Motif)data;
          ICmap.put(motif.getName(), motif.getICcontent());
       }
       return ICmap;
    }
    
    
    /**
     * 
     */
    private class OverlappingSimilarMotifsCluster {
        private ArrayList<Region> cluster;
        private int start=0; // lowest start position for all regions in the cluster
        private int end=0;   // highest end position for all regions in the cluster
        private String type; // generic cluster name
        private int canonicalStart=Integer.MAX_VALUE; // start of canonical reference region 
        private int canonicalOrientation=0;
        
        public OverlappingSimilarMotifsCluster(Region firstRegion, MotifPartition partition, HashMap<String,Integer[]> alignments) {
            cluster=new ArrayList<Region>(3);
            cluster.add(firstRegion);
            start=firstRegion.getRelativeStart();
            end=firstRegion.getRelativeEnd();
            int firstRegionOrientation=firstRegion.getOrientation();
            type=(partition!=null)?partition.getClusterForMotif(firstRegion.getType()):null;
            if (alignments!=null) {
                Integer[] alignment=alignments.get(firstRegion.getType()); // the Integer[] represents firsRegion's motif's optimal alignment with respect to the chosen canonical motif of the motif's cluster (from the "alternatives" partition) 
                if (alignment!=null) { //
                    int bestDirection=alignment[2];
                    if (bestDirection==1) { // the first motif and the target have similar orientation (in theory)
                       if (firstRegionOrientation==Region.DIRECT) canonicalStart=start-alignment[0];
                       else if (firstRegionOrientation==Region.REVERSE) canonicalStart=start+firstRegion.getLength()+alignment[0]-alignment[3]; 
                       canonicalOrientation=firstRegionOrientation;
                    } else {  // the first motif and the target have opposite orientations (in theory)
                       if (firstRegionOrientation==Region.DIRECT) canonicalStart=start+firstRegion.getLength()+alignment[1]-alignment[3]; 
                       else if (firstRegionOrientation==Region.REVERSE) canonicalStart=start-alignment[1];                  
                       canonicalOrientation=firstRegionOrientation*(-1);
                    }
                }
            }
        }


        public ArrayList<Region> getRegions() {
            return cluster;
        }

        public boolean fitsInCluster(Region newregion, MotifPartition partition, HashMap<String,Integer[]> alignments) {
            if (type!=null && partition==null) return false;
            if (type!=null && !type.equals(partition.getClusterForMotif(newregion.getType()))) return false;
            Integer[] alignment=alignments.get(newregion.getType());           
            
            if (alignment==null) return false;
            int regionstart=newregion.getRelativeStart();

            // Note: The following lines compares the new region to the cluster's canonical reference in all possible orientations (D-D, R-R, D-R, and R-D)
            // This allows us to include palindromic motifs in both direction, but is might also have some "artifacts" in that
            // some motifs that happen to be located in an "optimal" location and orientation relative to the canonical motif
            // can be included even though this optimum does not really represent a "true similarity" between the two motifs
            // but only what happened to be the best candidate among many worse.
             
            if (canonicalOrientation==Region.DIRECT) {
                if (regionstart-alignment[0]==canonicalStart) return true; 
                if (regionstart-alignment[1]==canonicalStart) return true;                 
            } else {
                int targetLength=alignment[3];
                int canonicalRelativeStart=canonicalStart+targetLength-1;
                int regionRelativeStart=regionstart+newregion.getLength()-1;               
                if (regionRelativeStart+alignment[0]==canonicalRelativeStart) return true; 
                if (regionRelativeStart+alignment[1]==canonicalRelativeStart) return true;               
            }
            return false;
        }
        
        /** The new region fits if it belongs to the same motif cluster and overlaps this region group (current maximum span) */
        public boolean fitsInClusterNaive(Region newregion, MotifPartition partition) {
            if (type!=null && partition==null) return false;
            if (type!=null && !type.equals(partition.getClusterForMotif(newregion.getType()))) return false;
            int regstart=newregion.getRelativeStart();
            int regend=newregion.getRelativeEnd();   
            if (regstart>end || regend<start) return false; // no overlap with the regions currently in the cluster
            return true;
        }        
        
        public void addRegion(Region newregion) {
            cluster.add(newregion);
            int regstart=newregion.getRelativeStart();
            int regend=newregion.getRelativeEnd();     
            if (regstart<start) start=regstart;
            if (end>regend) end=regend;
        }
        
        public int size() {
            return cluster.size();
        }
        
        public int getClusterStart() {
            return start;
        }
        
        public int getClusterEnd() {
            return end;
        }

        /** Returns a canonical representative region in a cluster (the one region that should be left intact)
         *  The list should have at least 2 members
         *  @param argument Can vary depending on the needs of the keep-option
         *  @return A canonical region which should be kept. Note that this region might not exist in the provided cluster
         *          but can be a new canonical representative for the group. If this is the case, the return Region should
         *          be added to pruned dataset and all the other regions in the cluster should be removed instead.
         */
        private Region getCanonicalRepresentative(String keepOption, Object argument) {
            Region canonical=cluster.get(0);
            if (keepOption==null) return canonical; // It does not matter which we pick. They should all be equal if keepOption==null
            if (keepOption.equals(KEEP_TOP_SCORING)) {
                double bestscore=canonical.getScore(); 
                for (int i=1;i<cluster.size();i++) {
                    Region nextregion=cluster.get(i);
                    if (nextregion.getScore()>bestscore) {
                        canonical=nextregion;
                        bestscore=nextregion.getScore();
                    }
                }                
            } else if (keepOption.equals(KEEP_FIRST_SORTED_NAME)) {
                String firstname=canonical.getType();
                for (int i=1;i<cluster.size();i++) {
                    Region nextregion=cluster.get(i);
                    if (nextregion.getType().compareTo(firstname)<0) {
                        canonical=nextregion;
                        firstname=nextregion.getType();
                    }
                }              
            } else if (keepOption.equals(KEEP_HIGH_IC)) {
                HashMap<String,Double> icTable=(HashMap<String,Double>)argument;
                double bestIC=icTable.get(canonical.getType());
                for (int i=1;i<cluster.size();i++) {
                    Region nextregion=cluster.get(i);
                    double nextIC=icTable.get(nextregion.getType());
                    if (nextIC>bestIC) {
                        canonical=nextregion;
                        bestIC=nextIC;
                    }
                }  
            } else if (keepOption.equals(KEEP_DIRECT_STRAND)) {
                for (int i=0;i<cluster.size();i++) {
                    Region region=cluster.get(i);
                    if (region.getOrientation()==Region.DIRECT) return region; // return first direct
                } 
            } else if (keepOption.equals(KEEP_RELATIVE_STRAND)) {
                 int sequenceOrientation=(Integer)argument;
                 for (int i=0;i<cluster.size();i++) {
                    Region region=cluster.get(i);
                    if (region.getOrientation()==sequenceOrientation) return region; // return first direct
                }                
            }
            return canonical;
        }
        
    } // END class OverlappingSimilarMotifsCluster

}
