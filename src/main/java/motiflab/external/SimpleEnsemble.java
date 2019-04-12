/*


 */

package motiflab.external;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
public class SimpleEnsemble extends EnsemblePredictionMethod {

    public SimpleEnsemble() {
        this.name="SimpleEnsemble";
        this.programclass="EnsemblePrediction";
        this.serviceType="bundled";

        addParameter("Min support",Integer.class, new Integer(2),new Integer[]{1,100},"Minimum number of methods that must predict a base position for that position to be considered true",true,false);
        addParameter("Min bases",Double.class, new Double(50.0),new Double[]{1.0,100.0},"Minimum number of base positions in a site that must be true for the whole site to considered true (See also 'threshold type' parameter)",true,false);
        addParameter("Threshold type",String.class, "Percentage",new String[]{"Absolute","Percentage"},"Decides whether the 'Min bases' parameter should be interpreted as an absolute number or a percentage number",true,false);
        addResultParameter("Result", RegionDataset.class, null, null, "output track");
        addResultParameter("Motifs", MotifCollection.class, null, null, "Motif collection");
    }



    @Override
    @SuppressWarnings("unchecked")
    public void execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String targetDatasetName=task.getTargetDataName();
        RegionDataset[] sources=(RegionDataset[])task.getParameter(Operation_ensemblePrediction.SOURCE_DATA);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for ensemble motif prediction with SimpleEnsemble");
        int minSupport=(Integer)task.getParameter("Min support");
        double minBases=(Double)task.getParameter("Min bases");
        String ttype=(String)task.getParameter("Threshold type");
        boolean percentage=ttype.equalsIgnoreCase("Percentage");
        if (percentage && minBases>=1) minBases=minBases/100.0; // minBases is given as a number between 1 and 100 so divide by 100
        if (minBases<=0) throw new ExecutionError("'Min bases' parameter should be greater than 0 for ensemble motif prediction with SimpleEnsemble");
        if (minBases>1 && percentage) throw new ExecutionError("'Min bases' parameter can not be greater than 100% for ensemble motif prediction with SimpleEnsemble");
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection();
        int size=sequenceCollection.getNumberofSequences();
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        int i=0;
        RegionDataset targetDataset=new RegionDataset(targetDatasetName);
        targetDataset.setMotifTrack(true);
        targetDataset.setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));
        HashSet<String> predictedMotifs=new HashSet<String>(); // the names of motifs that have retained sites
        for (Sequence sequence:sequences) { // for each sequence
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            String sequenceName=sequence.getName();
            NumericSequenceData votesTrack=new NumericSequenceData(sequence, 0); // contains votes for each position in the sequence. Each algorithm gets one vote for each position and the resulting value in each position is the number of methods that predicts at least one motif site in that position
            for (RegionDataset algorithmPredictions:sources) { // for each method
                RegionSequenceData regionseq=(RegionSequenceData)algorithmPredictions.getSequenceByName(sequenceName);
                for (int pos=0;pos<sequence.getSize();pos++) {
                    int predictedSites=regionseq.getNumberOfRegionsAtRelativePosition(pos);
                    if (predictedSites>0) votesTrack.setValueAtRelativePosition(pos, votesTrack.getValueAtRelativePosition(pos)+1); // this method votes TRUE for this position, so increase its value by one in the votes track
                }
            }
            // The votes are in for this sequence. Now process the votes by filtering out (setting to 0) those positions that did not obtain the required minimum of votes and set to 1 those positions that did
            for (int pos=0;pos<sequence.getSize();pos++) {
                double votes=votesTrack.getValueAtRelativePosition(pos);
                if (votes>=minSupport) votesTrack.setValueAtRelativePosition(pos, 1); else votesTrack.setValueAtRelativePosition(pos, 0);
            }
            // Now go through the predicted sites again and see if any of the sites obtained enough votes
            RegionSequenceData targetDatasetSequence=(RegionSequenceData)targetDataset.getSequenceByName(sequenceName);
            for (RegionDataset algorithmPredictions:sources) { // for each method
                RegionSequenceData regionseq=(RegionSequenceData)algorithmPredictions.getSequenceByName(sequenceName);
                for (Region region:regionseq.getAllRegions()) {
                    double truePositions=votesTrack.getSumValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
                    double requiredPositions=(percentage)?(minBases*region.getLength()):minBases;
                    if (truePositions>=requiredPositions) {
                        targetDatasetSequence.addRegion(region.clone()); // add site to track
                        predictedMotifs.add(region.getType()); // add motif type to set
                    }
                 }
            }
            task.setStatusMessage("Executing ensemblePrediction with SimpleEnsemble:  ("+(i+1)+"/"+size+")");
            task.setProgress(i+1, size);
            i++;
            Thread.yield();
        } // end for each sequence

        // Make a collection of the motifs that were used by the sites predicted by the ensemble
        MotifCollection motifcollection=new MotifCollection("SimpleEnsemble_Motifs");
        for (String motifname:predictedMotifs) {
            if (engine.getDataItem(motifname) instanceof Motif) motifcollection.addMotifName(motifname);
        }
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);
        task.setParameter("Motifs", motifcollection);
        // the motif collection above should be constructed properly!!! not just a dummy like here...
    }



}
