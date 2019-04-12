/*
 
 
 */

package motiflab.external;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.operations.Operation_ensemblePrediction;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Region;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Motif;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;


/**
 *
 * @author Kjetil
 */
public class EMD extends EnsemblePredictionMethod {

    public EMD() {
        this.name="EMD";
        this.programclass="EnsemblePrediction";
        this.serviceType="bundled";

        addParameter("Number of motifs",Integer.class, new Integer(5),new Integer[]{1,100},"Number of motifs reported by each component algorithm",true,false);
        addParameter("Motif size",Integer.class, new Integer(10),new Integer[]{1,100},"Length of motifs",true,false);
        addResultParameter("Result", RegionDataset.class, null, null, "output track");
        addResultParameter("Motifs", MotifCollection.class, null, null, "Motif collection");
    }



    @Override
    @SuppressWarnings("unchecked")
    public void execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String targetDatasetName=task.getTargetDataName();
        RegionDataset[] sources=(RegionDataset[])task.getParameter(Operation_ensemblePrediction.SOURCE_DATA);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for ensemble motif prediction with EMD");
        int numberofmotifs=(Integer)task.getParameter("Number of motifs");
        int motiflength=(Integer)task.getParameter("Motif size");
        int numberofAlgorithms=sources.length;
        DNASequenceDataset dnatrack=(DNASequenceDataset)task.getParameter(Operation_ensemblePrediction.DNA_TRACK);
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection();  
        int size=sequenceCollection.getNumberofSequences();
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);  
        int i=0;
        ScoreComparator scorecomparator=new ScoreComparator();
        NumericDataset[] votetracks=new NumericDataset[numberofmotifs];   
        for (int m=0;m<numberofmotifs;m++) votetracks[m]=new NumericDataset("votemotif_"+m);
        for (Sequence sequence:sequences) { // for each sequence
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            String sequenceName=sequence.getName();
            Object[] algorithmSequenceGroups=new Object[numberofAlgorithms]; // this will contain an array of site groups (ArrayList<Region>) for each algorithm in one sequence
            int sequenceGroupsIndex=0;
            for (RegionDataset algorithmPredictions:sources) {
                String algorithm=algorithmPredictions.getName(); // use the name of the track as proxy for method name
                RegionSequenceData regionseq=(RegionSequenceData)algorithmPredictions.getSequenceByName(sequenceName);
                // Step 1: Collecting: collect all predicted sites for each [Sequence,Algorith]
                ArrayList<Region> sites=regionseq.getAllRegions();
                // Step 2: Grouping
                // Step 2a: Sort the sites by score 
                Collections.sort(sites, scorecomparator);
                // Step 2b: Divide into K (=numberofmotifs) groups of equal size
                int groupsize=(int)(((double)sites.size()/(double)numberofmotifs)+0.5);
                ArrayList[] groups=new ArrayList[numberofmotifs];
                for (int g=0;g<groups.length;g++) groups[g]=new ArrayList<Region>(); // initialize
                int groupindex=0;
                int counter=0;
                for (int s=0;s<sites.size();s++) { // assign each site to a group
                    Region reg=sites.get(s);
                    groups[groupindex].add(reg); // add site to group
                    counter++;
                    if (counter==groupsize) { // filled up this group
                        counter=0; // reset counter
                        if (groupindex<groups.length-1) groupindex++; // increase groupindex, but not higher than (groups.length-1)
                    }
                }                
                algorithmSequenceGroups[sequenceGroupsIndex]=groups;
                sequenceGroupsIndex++;
            } // end: for each algorithm's predictions in one sequence            
            // step 2c: Join together groups from all algorithms with same score rank
            ArrayList[] scoreRankGroups=new ArrayList[numberofmotifs];
            for (int rank=0;rank<numberofmotifs;rank++) { // for each ranked motif
                scoreRankGroups[rank]=new ArrayList<Region>(); // create a new list of sites of this scorerank from all algorithms
                for (int algIndex=0;algIndex<algorithmSequenceGroups.length;algIndex++) { // for each algorithm...
                    ArrayList[] algorithGroups=(ArrayList[])algorithmSequenceGroups[algIndex]; // get the score rank groups for this algorithm
                    ArrayList<Region> algorithmRankGroup=algorithGroups[rank]; // then get the group for this particular rank
                    scoreRankGroups[rank].addAll(algorithmRankGroup); // add all sites in this rank group (for this algorithm) to the combined group for all algorithms
                }
            }
            // Step 3: Voting: for each position p in the sequence, count the number of times this position is predicted in one rank group (for all algorithms combined)
            for (int rank=0;rank<numberofmotifs;rank++) {
               NumericSequenceData votetrack=new NumericSequenceData(sequence, 0);
               votetracks[rank].addSequence(votetrack); // add this sequence to the dataset
               ArrayList<Region> regions=(ArrayList<Region>)scoreRankGroups[rank];
               for (Region reg:regions) {
                   int start=reg.getRelativeStart();
                   int end=reg.getRelativeEnd();
                   for (int p=start;p<=end;p++) {
                       Double oldvalue=votetrack.getValueAtRelativePosition(p);
                       if (oldvalue!=null) votetrack.setValueAtRelativePosition(p, oldvalue+1.0); // increase count by 1 for each position within the region
                   }
               }
            }
            // Step 4: smooth the vote tracks (for this sequences)
             for (int rank=0;rank<numberofmotifs;rank++) {
               NumericSequenceData votetrack=(NumericSequenceData)votetracks[rank].getSequenceByName(sequence.getName());
               smoothSequence(votetrack, (int)(motiflength+0.5));
            }           
            task.setStatusMessage("Executing ensemblePrediction with EMD:  ("+(i+1)+"/"+size+")");
            task.setProgress(i+1, size);
            i++;
            Thread.yield();
        }
        ArrayList<Sequence> allSequences=engine.getDefaultSequenceCollection().getAllSequences(engine);            
        // --- DEBUG: Start ---
        // add other sequences to the votetracks (in case a subset Sequence Collection was used)
        // and register with the engine so we can visualize the result    
//        for (int m=0;m<numberofmotifs;m++) {
//            votetracks[m].setupDefaultDataset(allSequences); // this will add missing sequences
//            engine.storeDataItem(votetracks[m]);
//        }        
        // --- DEBUG: End ---
        
        // step 5: Extract the sites
        MotifCollection motifcollection=new MotifCollection("EMD_Motifs");
        RegionDataset targetDataset=new RegionDataset(targetDatasetName);
        targetDataset.setupDefaultDataset(allSequences);
        for (int rank=0;rank<numberofmotifs;rank++) { // for each motif
           NumericDataset votes=votetracks[rank];          
           String motifname="EMD_Motif_"+(rank+1);
           double score=numberofmotifs-rank; // score is inverse of rank
           int leftflank=(int)(motiflength/2); // round down
           int rightflank=motiflength-leftflank; 
           String[] sites=new String[sequences.size()];
           Region[] siteRegions=new Region[sequences.size()];
           for (int index=0;index<sequences.size();index++) { // extract site for this motif from each sequence     
               Sequence seq=sequences.get(index);
               int pos=pickLocalTop((NumericSequenceData)votes.getSequenceByName(seq.getName()));
               if (pos>=0) { // found peak in votes track. Create a predicted site around the peak
                   int regionstart=pos-leftflank;
                   int regionend=pos+rightflank-1;
                   if (regionstart<0 || regionend>=seq.getSize()) {
                       // region partially outside sequence
                       engine.logMessage("WARNING: predicted motif partially outside sequence. Skipping");
                   } else {
                       RegionSequenceData regseq=(RegionSequenceData)targetDataset.getSequenceByName(seq.getName());
                       Region newsite=new Region(regseq, regionstart, regionend, motifname, score, Region.INDETERMINED);
                       regseq.addRegion(newsite);
                       siteRegions[index]=newsite;
                       DNASequenceData dnaseq=(DNASequenceData)dnatrack.getSequenceByName(seq.getName());                   
                       sites[index]=new String((char[])dnaseq.getValueInGenomicInterval(dnaseq.getGenomicPositionFromRelative(regionstart), dnaseq.getGenomicPositionFromRelative(regionend)));                       
                   }
               }
           }
           // decide the proper orientation for each site and create the motif
           int[] orientations=findBestOrientations(sites,motiflength);
           for (int index=0;index<orientations.length;index++) {
               if (siteRegions[index]!=null) siteRegions[index].setOrientation(orientations[index]);
               //engine.logMessage("["+motifname+"]{"+sequences.get(index).getName()+"} "+orientations[index]+"  => "+sites[index]);
               // the "sequence" property will be set later in Operation_ensemblePrediction 
           }
           double[][] matrix=getMatrixFromSites(sites, orientations, motiflength);
           Motif motif=new Motif(motifname);
           motif.setMatrix(matrix);
           motifcollection.addMotifToPayload(motif);
        } // end: extract sites from all sequences for each rank (motif)      
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);
        task.setParameter("Motifs", motifcollection);
        // the motif collection above should be constructed properly!!! not just a dummy like here...
    }

    
    /** 
     * Checks the size of every region in the source datasets and returns the common size
     * or throws an ExecutionError if not all regions have the same length
     */
    private int getMotifLengthFromData(RegionDataset[] sources, ArrayList<Sequence> sequences) throws ExecutionError {
        int length=-1;
        for (RegionDataset dataset:sources) {
            for (Sequence seq:sequences) {
                RegionSequenceData regionseq=(RegionSequenceData)dataset.getSequenceByName(seq.getName());
                ArrayList<Region> regions=regionseq.getOriginalRegions();
                for (Region region:regions) {
                    if (length<0) length=region.getLength();
                    else if (length!=region.getLength()) throw new ExecutionError("All sites should have the same size when using the EMD algorithm");
                }
            }  
        }        
        return length;
    }

    /** Smooths a sequence using a sliding "sum"-window with a given size anchored at the center */
    private void smoothSequence(NumericSequenceData track, int windowsize) {   
        double[] smoothed=new double[track.getSize()];
        for (int p=0;p<=track.getSize()-windowsize;p++) {
            double sum=0;
            for (int i=0;i<windowsize;i++) {
                sum+=track.getValueAtRelativePosition(p+i);
            }
            smoothed[p+(int)(windowsize/2)+1]=sum;            
        }
        for (int p=0;p<track.getSize();p++) {
            track.setValueAtRelativePosition(p, smoothed[p]);
        }
    }
    
    /** Returns the position of the highest value in a track, or -1 if no values are greater than 0
     *  if several consecutive positions have the maximum value the middle position (to the right)
     *  is returned. If multiple peaks have the same max value only the first peak is returned
     */
    private int pickLocalTop(NumericSequenceData track) {
        double maxvalue=-1;
        int maxpos=-1;
        boolean inRun=false;
        for (int p=0;p<track.getSize();p++) {
            double val=track.getValueAtRelativePosition(p);
            if (val>0 && val>maxvalue) {
                maxvalue=val;
                maxpos=p;
                inRun=true;
            } else if (val==maxvalue && inRun) { // new peak with same maxvalue in same run, update maxpos to position in middle of run
                   maxpos=(int)((maxpos+p)/2)+1;                            
            } else { // value smaller than max
                inRun=false; 
            }
        }
        return maxpos;
    }  
    
    private class ScoreComparator implements Comparator<Region> {
            @Override
            public int compare(Region reg1, Region reg2) { //
                double score1=reg1.getScore();
                double score2=reg2.getScore();
                if (score1>score2) return -1;
                else if (score1<score2) return 1;
                else return 0;
            }
    }    
    
    
    private int[] findBestOrientations(String[] sites, int motiflength) {
        int[] orientations=new int[sites.length];
        boolean started=false;
        for (int i=0;i<sites.length;i++) {
            if (!started) {
                if (sites[i]!=null) {
                  started=true;
                  orientations[i]=Region.DIRECT; // set first (non-null) site to direct
                }
            } else {
                orientations[i]=Region.DIRECT;
                double icDirect=calculateIC(sites, orientations, motiflength);
                orientations[i]=Region.REVERSE;
                double icReverse=calculateIC(sites, orientations, motiflength);
                if (icDirect>=icReverse) {
                   orientations[i]=Region.DIRECT;
                } else {
                   orientations[i]=Region.REVERSE; 
                }
            }
        }
        return orientations;
    }

    private double[][] getMatrixFromSites(String[] sequences, int[] orientations, int motiflength) {   
        if (sequences.length==0) return null;
        double[][] matrix=new double[motiflength][4];
        for (int i=0;i<motiflength;i++) { // for each column
            for (int j=0;j<sequences.length;j++) { // for each sequence
                if (orientations[j]==Region.INDETERMINED || sequences[j]==null) continue; // skip sequences without set orientation
                else if (orientations[j]==Region.DIRECT) {
                    char base=Character.toUpperCase(sequences[j].charAt(i));
                    if (base=='A') matrix[i][0]++;
                    else if (base=='C') matrix[i][1]++;
                    else if (base=='G') matrix[i][2]++;
                    else if (base=='T') matrix[i][3]++;                  
                } else { // reverse orientation
                    char base=Character.toUpperCase(sequences[j].charAt(sequences[j].length()-(i+1)));
                    if (base=='A') matrix[i][3]++;
                    else if (base=='C') matrix[i][2]++;
                    else if (base=='G') matrix[i][1]++;
                    else if (base=='T') matrix[i][0]++;                     
                }
            }            
        }
        return matrix;     
    }    
    
    /**
     * Calculates the IC content of a matrix made up by the specified sequences (taken from DIRECT strand)
     * using the corresponding orientation as given for each sequence
     * @param sequences
     * @param orientations
     * @return 
     */
    private double calculateIC(String[] sequences, int[] orientations, int motiflength) {   
        if (sequences.length==0) return 0;
        double ic=0;
        for (int i=0;i<motiflength;i++) { // for each column
            double a_count=0;
            double c_count=0;
            double g_count=0;
            double t_count=0;
            for (int j=0;j<sequences.length;j++) { // for each sequence
                if (orientations[j]==Region.INDETERMINED || sequences[j]==null) continue; // skip sequences without set orientation
                else if (orientations[j]==Region.DIRECT) {
                    char base=Character.toUpperCase(sequences[j].charAt(i));
                    if (base=='A') a_count++;
                    else if (base=='C') c_count++;
                    else if (base=='G') g_count++;
                    else if (base=='T') t_count++;                  
                } else { // reverse orientation
                    char base=Character.toUpperCase(sequences[j].charAt(sequences[j].length()-(i+1)));
                    if (base=='A') t_count++;
                    else if (base=='C') g_count++;
                    else if (base=='G') c_count++;
                    else if (base=='T') a_count++;                     
                }
            }
            ic+=calculateColumnIC(a_count, c_count, g_count, t_count, false);
        }
        return ic;       
    }
    
    
    /** Calculates the information content in one position (for non-log-odds matrices) */
    private double calculateColumnIC(double a_count, double c_count, double g_count, double t_count,boolean correctForSmallSampleBias) {
           double total= a_count + c_count + g_count + t_count;
           double Pa=a_count/total;
           double Pc=c_count/total;
           double Pg=g_count/total;
           double Pt=t_count/total;
           double errorterm=3f/(2f*(double)Math.log(2)*total); // approximation. Found this in an appendix to a paper by Thomas D. Schneider
           double entropyA=(Pa>0)?Pa*log2(Pa):0;
           double entropyC=(Pc>0)?Pc*log2(Pc):0;
           double entropyG=(Pg>0)?Pg*log2(Pg):0;
           double entropyT=(Pt>0)?Pt*log2(Pt):0;
           double HsL=-(entropyA+entropyC+entropyG+entropyT);
           if (correctForSmallSampleBias) return 2-errorterm-HsL;
           else return 2-HsL;
    }
    
    /** Return the Log2-value of the input*/
    private double log2(double value) {
        return (double)(Math.log(value)/Math.log(2));
    }
    
}
