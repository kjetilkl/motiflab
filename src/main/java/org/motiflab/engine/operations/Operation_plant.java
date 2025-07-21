/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterExporter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleMotif;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class Operation_plant extends Operation implements ParameterExporter {
    private static final String name="plant";
    private static final String description="Plants motifs or modules in a DNA sequence";
    public static final String PARAMETERS="parameters";
    public static final String MOTIFS_NAME="MotifsOrModuleName"; // name of motif, motif collection or module which is to be planted
    public static final String TARGET_SITES_TRACK="targetSitesTrackName";
    private Class[] datasourcePreferences=new Class[]{DNASequenceDataset.class};
    private char[] bases=new char[]{'A','C','G','T'};

    private Parameter[] parameters=new Parameter[]{
       new Parameter("Plant probability", Double.class, new Double(1.0), new Double[]{0.0,1.0}, "The probability that a single motif will be planted", true, false),
       new Parameter("Force plant", Boolean.class, Boolean.TRUE, new Boolean[]{Boolean.FALSE,Boolean.TRUE}, "Tries to plant a motif in a uniformly random position if no other eligible positions are found", false, false),
       new Parameter("Min match", Double.class, new Double(0.75), new Double[]{0.0,1.0}, "Minimum percentage match of the motif. Lower values means more degenerate motifs will be allowed", true, false),
       new Parameter("Max match", Double.class, new Double(1.0), new Double[]{0.0,1.0}, "Maximum percentage match of the motif. Lower values means more degenerate motifs will be used", true, false),
       new Parameter("Reverse probability", Double.class, new Double(0.5), new Double[]{0.0,1.0}, "Probability that the motif or module will appear in reverse orientation in a sequence", false, false),
       new Parameter("Use same pattern", Boolean.class, Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE}, "If selected, the same exact DNA pattern will be used for all planted sites of the same motif", false, false),
       new Parameter("Positional prior", NumericDataset.class, null, new Class[]{NumericDataset.class}, "A positional priors track will influence which positions are more likely to be selected for planting motifs", false, false),
       new Parameter("Use for prior", String.class, "sum", new String[]{"startValue","relativeStartValue","sum","every positive"}, "<html>Specifies how to use the positional priors in relation in the planted sites.<br></html>", false, false),
    };


    @Override
    public String getOperationGroup() {
        return "Motif"; // I place it here (although the operation also applies to Modules)
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
    public Object getDefaultValueForParameter(String parameterName) {
        for (Parameter parameter:parameters) {
            if (parameter.getName().equals(parameterName)) return parameter.getDefaultValue();
        }
        return null;
    }

    @Override
    public Parameter getParameterWithName(String parameterName) {
        for (Parameter parameter:parameters) {
            if (parameter.getName().equals(parameterName)) return parameter;
        }
        return null;
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }


    public void resolveParameters(OperationTask task) throws Exception {
        ParameterSettings parameterSettings=(ParameterSettings)task.getParameter(Operation_plant.PARAMETERS);
        if (parameterSettings==null) throw new ExecutionError("Missing parameters for operation plant");
        Parameter[] arguments=getParameters();
        for (int i=0;i<arguments.length;i++) {
            String parameterName=arguments[i].getName();
            Object value=parameterSettings.getResolvedParameter(parameterName, getParameters(), engine);
            if (arguments[i].isRequired() && (value==null || value.toString().isEmpty())) throw new ExecutionError("Missing value for required parameter '"+parameterName+"'");
            task.setParameter(parameterName,value);
        }
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
        double minMatch=(Double)task.getParameter("Min match");
        double maxMatch=(Double)task.getParameter("Max match");
        if (minMatch<0 || minMatch>1) throw new ExecutionError("'Min match' parameter should be a value between 0 and 1 (got "+minMatch+")",task.getLineNumber());
        if (maxMatch<0 || maxMatch>1) throw new ExecutionError("'Max match' parameter should be a value between 0 and 1 (got "+maxMatch+")",task.getLineNumber());
        if (minMatch>maxMatch) throw new ExecutionError("'Min match' parameter should not be greater than 'Max match'",task.getLineNumber());
        NumericDataset prior=(NumericDataset)task.getParameter("Positional prior");
        if (prior!=null) { // make a work-copy of the priors track since it will be changed later
            NumericDataset copy=(NumericDataset)prior.clone(); // make a working copy
            task.setParameter("Positional prior", copy);
        }
    }



    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        task.setStatusMessage("Executing operation: "+task.getOperationName());
        task.setProgress(5);
        String sourceName=task.getSourceDataName();
        if (sourceName==null || sourceName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());         
        String targetName=task.getTargetDataName();
        Data sourceData=engine.getDataItem(sourceName);
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError("Operation 'plant' can not work on '"+sourceName+"'",task.getLineNumber());
        String targetSitesTrackName=(String)task.getParameter(TARGET_SITES_TRACK);
        if (targetName==null || targetName.isEmpty()) throw new ExecutionError("Missing name for target DNA dataset object",task.getLineNumber());
        if (targetSitesTrackName==null || targetSitesTrackName.isEmpty()) throw new ExecutionError("Missing name for target Region Dataset object",task.getLineNumber());

        String motifsName=(String)task.getParameter(Operation_plant.MOTIFS_NAME);
        if (motifsName==null || motifsName.isEmpty()) throw new ExecutionError("Missing name for motifs or module",task.getLineNumber());
        Object motifobject=engine.getDataItem(motifsName);
        if (motifobject==null) throw new ExecutionError("No such data object '"+motifsName+"'",task.getLineNumber());
        if (!(motifobject instanceof Motif || motifobject instanceof MotifCollection || motifobject instanceof ModuleCRM)) throw new ExecutionError("'"+motifsName+"' is not a Motif, Motif Collection or Module",task.getLineNumber());
        if (motifobject instanceof MotifCollection && ((MotifCollection)motifobject).size()>5) throw new ExecutionError("A maximum of 5 motifs can be planted at a time (Size of "+motifsName+" is "+((MotifCollection)motifobject).size()+").",task.getLineNumber());
        if (motifobject instanceof MotifCollection && ((MotifCollection)motifobject).isEmpty()) throw new ExecutionError(motifsName+" does not contain any motifs",task.getLineNumber());
        resolveParameters(task);
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
        // setup target data objects for DNA sequence and TFBS sites
        DNASequenceDataset targetDNAdataset=(DNASequenceDataset)sourceData.clone();
        targetDNAdataset.setName(targetName);
        RegionDataset targetSitesTrack=new RegionDataset(targetSitesTrackName);
        for (Sequence seq:engine.getDefaultSequenceCollection().getAllSequences(engine)) {
            targetSitesTrack.addSequence(new RegionSequenceData(seq));
        }
             if (motifobject instanceof Motif) plantSingleMotif(targetDNAdataset, targetSitesTrack, (Motif)motifobject, sequenceCollection, task);
        else if (motifobject instanceof MotifCollection) plantMotifCollection(targetDNAdataset, targetSitesTrack, (MotifCollection)motifobject, sequenceCollection, task);
        else if (motifobject instanceof ModuleCRM) plantModule(targetDNAdataset, targetSitesTrack, (ModuleCRM)motifobject, sequenceCollection, task);
        if (motifobject instanceof ModuleCRM) targetSitesTrack.setModuleTrack(true);
        else targetSitesTrack.setMotifTrack(true);
        task.setProgress(100);
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {engine.updateDataItem(targetDNAdataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
        try {engine.updateDataItem(targetSitesTrack);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
        return true;
    }

    /**
     * Plants motifs from the given motif collection in the targetDNAdataset. The motif regions are returned in targetSitesDataset
     * @param targetDNAdataset
     * @param targetSitesdataset
     * @param motifcollection
     * @param task
     */
    private void plantMotifCollection(DNASequenceDataset targetDNADataset, RegionDataset targetSitesDataset, MotifCollection motifcollection, SequenceCollection sequenceCollection, OperationTask task) throws Exception {
        MotifSampler sampler=new MotifSampler();
        boolean useSamePattern=(Boolean)task.getParameter("Use same pattern");
        double reverseProbability=(Double)task.getParameter("Reverse probability");
        double minMatch=(Double)task.getParameter("Min match");
        double maxMatch=(Double)task.getParameter("Max match");
        double probability=(Double)task.getParameter("Plant probability");
        NumericDataset prior=(NumericDataset)task.getParameter("Positional prior");
        if (prior==null) prior=createDefaultPrior(sequenceCollection);
        String useforprior=(String)task.getParameter("Use for prior");
        Boolean forceplant=(Boolean)task.getParameter("Force plant");
        if (forceplant==null) forceplant=Boolean.FALSE;
        if (useforprior==null || useforprior.isEmpty()) useforprior="sum";
        HashMap<String,char[]> samepatternMap=null;
        if (useSamePattern) {
            samepatternMap=new HashMap<String, char[]>(motifcollection.size());
            for (Motif motif:motifcollection.getAllMotifs(engine)) {
                char[] motifpattern=sampler.sample(motif, minMatch, maxMatch);
                samepatternMap.put(motif.getName(), motifpattern);
            }
        }
        // sort the motifs by size from shorter to longer. This order will be used later on
        // when shorter motifs are planted before longer motifs (this is done so that the last N-1 positions
        // which are set to 0 in the priors track can 'grown' and reused.
        ArrayList<String> motifnames=getMotifsNamesSortedByMotifLength(motifcollection);
        
        for (int i=0;i<sequenceCollection.size();i++) {
            String sequenceName=sequenceCollection.getSequenceNameByIndex(i);
            NumericSequenceData priorsequence=(NumericSequenceData)prior.getSequenceByName(sequenceName);
            RegionSequenceData targetSitesSequence=(RegionSequenceData)targetSitesDataset.getSequenceByName(sequenceName);
            DNASequenceData targetSequence=(DNASequenceData)targetDNADataset.getSequenceByName(sequenceName);           
            if (priorsGotNegativeValues(priorsequence)) throw new ExecutionError("The positional priors track '"+prior.getName()+"' contains negative values");
            if (useforprior.equals("every positive") && prior!=null) { // plant motifs at every position with positive prior
                for (int position=0;position<targetSequence.getSize();position++) {
                    if (priorsequence.getValueAtRelativePosition(position)>0 && Math.random()<probability) {
                        int randomMotifIndex=(int)(Math.random()*motifcollection.size());
                        Motif motif=motifcollection.getMotifByIndex(randomMotifIndex, engine);
                        char[] pattern=(useSamePattern)?samepatternMap.get(motif.getName()):sampler.sample(motif, minMatch, maxMatch);
                        String bindingSequence=new String(pattern);
                        double score=getMatchScore(pattern, motif);
                        int orientation=(Math.random()<reverseProbability)?Region.REVERSE:Region.DIRECT;                        
                        int plantposition=position;
                        if (targetSequence.getStrandOrientation()==Sequence.REVERSE) {
                            if (orientation==Region.DIRECT) orientation=Region.REVERSE;
                            else if (orientation==Region.REVERSE) orientation=Region.DIRECT;
                            plantposition=plantposition-motif.getLength()+1; // plant at the other end to the motifs that extend downstream
                        }                                              
                        if (orientation==Region.REVERSE) pattern=MotifLabEngine.reverseSequence(pattern);
                        if (plantposition+motif.getLength()>=targetSequence.getSize()) continue; // motif did not fit at the end
                        plantMotifSite(motif, pattern, bindingSequence, plantposition, score, orientation, targetSitesSequence, targetSequence);
                    }
                }                                
            } else { //
              for (String motifname:motifnames) { // go through every motif name and plant it once
                if (Math.random()<probability) {
                    Motif motif=motifcollection.getMotifByName(motifname, engine);
                    char[] pattern=(useSamePattern)?samepatternMap.get(motifname):sampler.sample(motif, minMatch, maxMatch);
                    String bindingSequence=new String(pattern);
                    double score=getMatchScore(pattern, motif); // how well does the randomly chosen binding pattern match the motif PWM
                    int orientation=(Math.random()<reverseProbability)?Region.REVERSE:Region.DIRECT;
                    if (targetSequence.getStrandOrientation()==Sequence.REVERSE) {
                        if (orientation==Region.DIRECT) orientation=Region.REVERSE;
                        else if (orientation==Region.REVERSE) orientation=Region.DIRECT;
                    }
                    if (orientation==Region.REVERSE) pattern=MotifLabEngine.reverseSequence(pattern);
                    // choose a random position for the motif either by sampling from a priors-distribution or by just selecting a random eligible position
                    setupPriorForSingleMotif(priorsequence, motif.getLength(), useforprior); // set the last N-1 positions in the priorstrack to 0 and normalize
                    int position=-1;
                    // find eligible position which does not overlap existing sites
                    for (int t=0;t<50;t++) { // try up to 50 times to sample a new position before we give up
                       position=sampleSitePosition(priorsequence,motif.getLength(),useforprior);
                       if (position==-2) break; // priors track is just zeros so wo cannot sample from it
                       if (siteOverlapsPrevious(targetSitesSequence, position, motif.getLength())) position=-1;
                       if (position>=0) break;
                    }
                    if (position<0 && forceplant) { // found no good positions when sampling so try again with random positions
                        for (int t=0;t<50;t++) { // try up to 50 times to sample a new position before we give up
                           position=(int)(Math.random()*(targetSequence.getSize()-motif.getLength()+1));
                           if (siteOverlapsPrevious(targetSitesSequence, position, motif.getLength())) position=-1;
                           if (position>=0) break;
                        }
                    }
                    if (position>=0) { // found eligible position
                        // insert DNA sequence into target at selected position
                        for (int j=0;j<pattern.length;j++) {
                           targetSequence.setValueAtRelativePosition(position+j, pattern[j]);
                           priorsequence.setValueAtRelativePosition(position+j, 0); // set priors values to 0 within the planted site so that subsequent sites will not be planted at the same place
                        }
                        // create new region for the TFBS and add it to targetSitesSequence
                        Region bindingsite=new Region(targetSitesSequence, position, position+motif.getLength()-1, motif.getName(), score, orientation);
                        bindingsite.setProperty("sequence", bindingSequence);
                        targetSitesSequence.addRegion(bindingsite);
                    }
                }
              } // end for each motif
            } // end plant every motif once
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+sequenceCollection.size()+")");
            task.setProgress(i+1, sequenceCollection.size());
            if (i%10==0) Thread.yield();
        }
    }
    /** Returns TRUE if a site planted at the given position will overlap will sites already present in the track */
    private boolean siteOverlapsPrevious(RegionSequenceData track, int start, int motiflength) {
        ArrayList<Region> regions=track.getOriginalRegions();
        int end=start+motiflength-1;
        if (regions.isEmpty()) return false;
        for (Region r:regions) {
            if (end<r.getRelativeStart() || start>r.getRelativeEnd()) continue;
            else return true; // overlaps with this region
        }
        return false;
    }

    private void plantSingleMotif(DNASequenceDataset targetDNADataset, RegionDataset targetSitesDataset, Motif motif, SequenceCollection sequenceCollection, OperationTask task) throws Exception {
        MotifSampler sampler=new MotifSampler();
        boolean useSamePattern=(Boolean)task.getParameter("Use same pattern");
        double reverseProbability=(Double)task.getParameter("Reverse probability");
        double minMatch=(Double)task.getParameter("Min match");
        double maxMatch=(Double)task.getParameter("Max match");
        double probability=(Double)task.getParameter("Plant probability");
        Boolean forceplant=(Boolean)task.getParameter("Force plant");
        if (forceplant==null) forceplant=Boolean.FALSE;
        NumericDataset prior=(NumericDataset)task.getParameter("Positional prior");
        String useforprior=(String)task.getParameter("Use for prior");
        if (useforprior==null || useforprior.isEmpty()) useforprior="sum";
        if (prior!=null) {
            if (priorsGotNegativeValues(prior)) throw new ExecutionError("The positional priors track '"+prior.getName()+"' contains negative values");
            if (!useforprior.equals("every positive")) setupPriorForSingleMotif(prior, motif.getLength(), sequenceCollection, useforprior);
        }
        char[] samepattern=null;
        if (useSamePattern) samepattern=sampler.sample(motif, minMatch, maxMatch);
        for (int i=0;i<sequenceCollection.size();i++) {
            String sequenceName=sequenceCollection.getSequenceNameByIndex(i);
            NumericSequenceData priorsequence=(prior!=null)?(NumericSequenceData)prior.getSequenceByName(sequenceName):null;
            RegionSequenceData targetSitesSequence=(RegionSequenceData)targetSitesDataset.getSequenceByName(sequenceName);
            DNASequenceData targetSequence=(DNASequenceData)targetDNADataset.getSequenceByName(sequenceName);
            if (useforprior.equals("every positive") && prior!=null) { // plant motif at every position with positive prior
                for (int position=0;position<targetSequence.getSize();position++) {
                    if (priorsequence.getValueAtRelativePosition(position)>0 && Math.random()<probability) {
                        char[] pattern=(useSamePattern)?samepattern:sampler.sample(motif, minMatch, maxMatch);
                        String bindingSequence=new String(pattern);
                        double score=getMatchScore(pattern, motif);
                        int orientation=(Math.random()<reverseProbability)?Region.REVERSE:Region.DIRECT;
                        int plantposition=position;
                        if (targetSequence.getStrandOrientation()==Sequence.REVERSE) {
                            if (orientation==Region.DIRECT) orientation=Region.REVERSE;
                            else if (orientation==Region.REVERSE) orientation=Region.DIRECT;
                            plantposition=plantposition-motif.getLength()+1; // plant at the other end to the motifs that extend downstream
                        }                       
                        if (orientation==Region.REVERSE) pattern=MotifLabEngine.reverseSequence(pattern);                        
                        if (plantposition+motif.getLength()>=targetSequence.getSize()) continue; // skip planting since the motif will exceed boundary of sequence
                        plantMotifSite(motif, pattern, bindingSequence, plantposition, score, orientation, targetSitesSequence, targetSequence);
                    }
                }
            } else if (Math.random()<probability) { // plant a single site
                char[] pattern=(useSamePattern)?samepattern:sampler.sample(motif, minMatch, maxMatch);
                String bindingSequence=new String(pattern);
                double score=getMatchScore(pattern, motif);
                int orientation=(Math.random()<reverseProbability)?Region.REVERSE:Region.DIRECT;
                if (targetSequence.getStrandOrientation()==Sequence.REVERSE) {
                    if (orientation==Region.DIRECT) orientation=Region.REVERSE;
                    else if (orientation==Region.REVERSE) orientation=Region.DIRECT;
                }
                if (orientation==Region.REVERSE) pattern=MotifLabEngine.reverseSequence(pattern);
                // choose a random position for the motif either by sampling from a priors-distribution or by just selecting a random eligible position
                int position=(priorsequence!=null)?sampleSitePosition(priorsequence,pattern.length,useforprior):(int)(Math.random()*(targetSequence.getSize()-motif.getLength()+1)); // 
                if (position<0 && forceplant) position=(int)(Math.random()*(targetSequence.getSize()-motif.getLength()+1));
                if (position>=0) { // found eligible position
                    plantMotifSite(motif, pattern, bindingSequence, position, score, orientation, targetSitesSequence, targetSequence);
                }
            }
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+sequenceCollection.size()+")");
            task.setProgress(i+1, sequenceCollection.size());
            if (i%10==0) Thread.yield();
        }        
    }

    /** Inserts a site for a motif into the provided Region and DNA sequences */
    private void plantMotifSite(Motif motif, char[] pattern, String bindingsequence, int position, double score, int orientation, RegionSequenceData targetSequence, DNASequenceData dnasequence) {
        // insert DNA sequence into target at selected position
        for (int j=0;j<pattern.length;j++) {
           dnasequence.setValueAtRelativePosition(position+j, pattern[j]);
        }
        // create new region for the TFBS and add it to targetSitesSequence
        Region bindingsite=new Region(targetSequence, position, position+motif.getLength()-1, motif.getName(), score, orientation);
        bindingsite.setProperty("sequence", bindingsequence);
        targetSequence.addRegion(bindingsite);      
    }


    private void plantModule(DNASequenceDataset targetDNADataset, RegionDataset targetSitesDataset, ModuleCRM cisRegModule, SequenceCollection sequenceCollection, OperationTask task)  throws Exception {
        MotifSampler sampler=new MotifSampler();
        boolean useSamePattern=(Boolean)task.getParameter("Use same pattern");
        double reverseProbability=(Double)task.getParameter("Reverse probability");
        double minMatch=(Double)task.getParameter("Min match");
        double maxMatch=(Double)task.getParameter("Max match");
        double probability=(Double)task.getParameter("Plant probability");
        Boolean forceplant=(Boolean)task.getParameter("Force plant");
        if (forceplant==null) forceplant=Boolean.FALSE;
        NumericDataset prior=(NumericDataset)task.getParameter("Positional prior");
        String useforprior=(String)task.getParameter("Use for prior");
        if (useforprior==null || useforprior.isEmpty()) useforprior="sum";
        if (useforprior.equals("every positive")) throw new ExecutionError("The 'use for prior=every positive' settings can only be used with motifs and motif collections not modules.");
        if (prior!=null) {
            if (priorsGotNegativeValues(prior)) throw new ExecutionError("The positional priors track '"+prior.getName()+"' contains negative values");
        }
        ArrayList<MotifStruct> samepattern=(useSamePattern)?sampleModule(cisRegModule, minMatch, maxMatch, sampler):null; //

        for (int i=0;i<sequenceCollection.size();i++) {
            if (Math.random()<probability && cisRegModule.getCardinality()>0) {
                String sequenceName=sequenceCollection.getSequenceNameByIndex(i);
                NumericSequenceData priorsequence=(prior!=null)?(NumericSequenceData)prior.getSequenceByName(sequenceName):null;
                RegionSequenceData targetSitesSequence=(RegionSequenceData)targetSitesDataset.getSequenceByName(sequenceName);
                DNASequenceData targetSequence=(DNASequenceData)targetDNADataset.getSequenceByName(sequenceName);
                // decide how the planted module should look. Which motifs/patterns, their relative orientation, order and distances
                ArrayList<MotifStruct> modulepattern=(useSamePattern)?samepattern:sampleModule(cisRegModule, minMatch, maxMatch, sampler);
                int modulespan=modulepattern.get(modulepattern.size()-1).distance;
                int orientation=(Math.random()<reverseProbability)?Region.REVERSE:Region.DIRECT;
                if (targetSequence.getStrandOrientation()==Sequence.REVERSE) {
                    if (orientation==Region.DIRECT) orientation=Region.REVERSE;
                    else if (orientation==Region.REVERSE) orientation=Region.DIRECT;
                }
                if (priorsequence!=null) setupPriorForSingleMotif(priorsequence, modulespan, useforprior); // set the last N-1 positions in the priorstrack to 0 and normalize

                // choose a random position for the motif either by sampling from a priors-distribution or by just selecting a random eligible position
                int position=(priorsequence!=null)?sampleSitePosition(priorsequence,modulespan,useforprior):(int)(Math.random()*(targetSequence.getSize()-modulespan+1)); // 
                if (position<0 && forceplant) position=(int)(Math.random()*(targetSequence.getSize()-modulespan+1));
                if (position>=0) { // found eligible position, now start planting the motifs
                    Region modulesite=new Region(targetSitesSequence, position, position+modulespan-1, cisRegModule.getName(), 0 , orientation);
                    double modulescore=0;
                    double motifmaxscore=0;
                    if (orientation==Region.REVERSE) position=(position+modulespan)-1;
                    //engine.logMessage(sequenceName+" => position="+position+"  ("+targetSitesSequence.getGenomicPositionFromRelative(position)+")");
                    for (MotifStruct singlemotif:modulepattern) {
                        int motifposition=(orientation==Region.DIRECT)?position:(position-singlemotif.pattern.length+1);
                        double score=singlemotif.score;
                        modulescore+=score;
                        if (score>motifmaxscore) motifmaxscore=score;
                        //insert DNA sequence into target at selected position
                        char[] pattern=singlemotif.pattern;
                        String bindingSequence=new String(pattern);
                        if (orientation==Region.REVERSE) singlemotif.orientation=singlemotif.orientation*(-1); // this will flip orientation if module is oriented on reverse
                        if (singlemotif.orientation==Region.REVERSE) pattern=MotifLabEngine.reverseSequence(pattern);
                        for (int j=0;j<pattern.length;j++) {
                           targetSequence.setValueAtRelativePosition(motifposition+j, pattern[j]);
                        }
                        //engine.logMessage("Motifposition="+motifposition+"  ("+targetSitesSequence.getGenomicPositionFromRelative(motifposition)+")");
                        // create new region for the TFBS and add it to the module
                        Region motifsite=new Region(targetSitesSequence, motifposition, motifposition+pattern.length-1, singlemotif.motifname, score, singlemotif.orientation);
                        motifsite.setProperty("sequence", bindingSequence);
                        motifsite.setParent(targetSitesSequence);
                        modulesite.setProperty(cisRegModule.getSingleMotifName(singlemotif.modulemotifIndex), motifsite);
                        int distToNext=singlemotif.pattern.length+singlemotif.distance;
                        //engine.logMessage("distToNext="+distToNext+"   motiflength="+singlemotif.pattern.length+"  distance="+singlemotif.distance);
                        if (orientation==Region.DIRECT) position+=distToNext; else position-=distToNext;
                    }
                    //modulesite.setScore(modulescore); // use sum of motifscores
                    modulesite.setScore(motifmaxscore); // use max of motifscores
                    targetSitesSequence.addRegion(modulesite);
                    //debugModule(modulesite,modulepattern);
                }

            }
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+sequenceCollection.size()+")");
            task.setProgress(i+1, sequenceCollection.size());
            if (i%10==0) Thread.yield();
        }
    }

    /** Creates a random instance of a module which order, relative orientations
     *  intra-module distances and motif patterns based on the given module and returns this
     *  information as a list of MotifStruct objects.
     *  Included for each MotifStruct (which is listed in the order the motifs should appear in the planted module)
     *  is the distance to the next motif in the module. However, for the very last of the motifs this distance value
     *  is the full span of the entire planted module.
     */
    private ArrayList<MotifStruct> sampleModule(ModuleCRM cisRegModule, double minMatch, double maxMatch, MotifSampler sampler) {
        int modulesize=cisRegModule.getCardinality();
        ArrayList<Integer> motiforder=new ArrayList<Integer>(modulesize);
        for (int i=0;i<modulesize;i++) motiforder.add(i); // default order is 0,1,2,3...,N
        if (!cisRegModule.isOrdered()) Collections.shuffle(motiforder); // choose a random order if order is irrelevant
        ArrayList<MotifStruct> chosenMotifs=new ArrayList<MotifStruct>(modulesize); // Ordered in the same was as the chosen motiforder, Each Object[] array will hold name of single Motif, pattern of single motif and selected orientation
        int totalTFBSspan=0; // the total span of planted TFBS not counting distance between them
        for (int i=0;i<modulesize;i++) { // for each ModuleMotif: select a representative Motif and TFBS pattern as well as relative orientation
            ModuleMotif modulemotif=cisRegModule.getSingleMotif(motiforder.get(i));
            MotifCollection set=modulemotif.getMotifAsCollection();
            int chosenIndex=(int)(Math.random()*set.size());
            Motif motif=set.getMotifByIndex(chosenIndex, engine); // The Motif choosen to represent this ModuleMotif
            char[] motifpattern=sampler.sample(motif, minMatch, maxMatch);
            double score=getMatchScore(motifpattern, motif);
            int orientation=modulemotif.getOrientation();
            if (orientation==ModuleCRM.INDETERMINED) orientation=(Math.random()<0.5)?ModuleCRM.DIRECT:ModuleCRM.REVERSE; // chose a random orientation if no constraint is set
            totalTFBSspan+=motifpattern.length;
            chosenMotifs.add(new MotifStruct(motiforder.get(i),motif.getName(),motifpattern,orientation,score,0));
        }
        int maxlength=cisRegModule.getMaxLength(); // maximum allowed length span for entire module
        int maxdistanceleft=maxlength-totalTFBSspan;
        if (maxdistanceleft<0) maxdistanceleft=(modulesize-1)*10; // this can happen if maxlength is not set explicitly. Use an average distance of 10bp between motifs
        int totaldistance=0; // total span of intra-module distances  (so far)
        // add distance to next motif to each Object[]
        for (int i=0;i<modulesize;i++) {
            MotifStruct motif=(MotifStruct)chosenMotifs.get(i);
            int distance=0;
            if (i==modulesize-1) distance=totalTFBSspan+totaldistance; // for the last motif the distance should be set to full module distance
            else {// for the other motifs, select a random distance to next motif!
                int[] spacingconstraint=cisRegModule.getDistance(i, i+1);
                if (spacingconstraint!=null) {
                    if (spacingconstraint[0]<0) spacingconstraint[0]=0; // to avoid overlapping motifs do not allow negative distances
                    if (spacingconstraint[1]<0) spacingconstraint[1]=0; // to avoid overlapping motifs do not allow negative distances
                    int range=spacingconstraint[1]-spacingconstraint[0];
                    distance=spacingconstraint[0]+(int)(Math.random()*range);
                } else { // no specific constraint, choose a random length depending on how much length is left
                    // if we allow an early motif to choose a random length up to the full length left
                    // then there might be little spacing left for the motifs coming after. What we want is probably
                    // an approximately equal distance between the motifs (maxdistanceleft/modulesize).
                    // here I choose to allow using the full range with a small probability or else the divided range
                    if (Math.random()<0.1) {
                        distance=(int)(Math.random()*maxdistanceleft*0.7);
                    } else {
                        distance=(int)(Math.random()*(maxdistanceleft/(double)(modulesize-(i+1))));
                    }
                    if (distance<0) distance=1; // just a wild guess

                }
            }
            motif.distance=distance;
            totaldistance+=distance;
            maxdistanceleft-=distance;
        }

        return chosenMotifs;
    }


    /** Given a positional prior sequence this method will return a randomly selected position
     *  based on the prior, i.e. positions with higher values are more likely to be selected
     *  and positions with prior-value==0 will never be returned.
     *  The prior should be setup so that only valid positions have values greater than 0.
     *  This means that for a motif of length N, the last N-1 positions must have value==0.
     */
    private int sampleSitePosition(NumericSequenceData prior, int motiflength, String useforprior) {
        double randomnumber=Math.random();
        if (randomnumber==0) randomnumber=1.0; // do not allow value of 0 but allow value of 1;
        // check that the priors track is not all 0's (or worse)
        boolean ok=false;
        for (int pos=0;pos<prior.getSize()-motiflength+1;pos++) if (prior.getValueAtRelativePosition(pos)>0) {ok=true;break;}
        if (!ok) return -2; // cannot sample from this prior!
        double sum=0; // cumulative sum. This should sum to 1.0 if the prior has been normalized properly
        if (useforprior.equals("sum")) {
            double cumsum=0;
            for (int pos=0;pos<prior.getSize()-motiflength+1;pos++) { // determine new cumulative sum since this can be greater than 1.0 when we use priors within the full site
              double sumwindow=0;
              for (int j=pos;j<pos+motiflength;j++) sumwindow+=prior.getValueAtRelativePosition(j);
              cumsum+=sumwindow;
            }
            // now rescale random number and find which position it corresponds to 
            randomnumber=cumsum*randomnumber;
            for (int pos=0;pos<prior.getSize()-motiflength+1;pos++) {
              double sumwindow=0;
              for (int j=pos;j<pos+motiflength;j++) sumwindow+=prior.getValueAtRelativePosition(j);
              sum+=sumwindow; // the positions in the 'middle' of the sequence are included in a number of sums (equal to motiflength) but the first positions are included in fewer!
              if (randomnumber<=sum) return pos;
            }
        } else {
            boolean direct=!(prior.getStrandOrientation()==Sequence.REVERSE && useforprior.equals("relativeStartValue"));
            if (direct) {
                for (int pos=0;pos<prior.getSize()-motiflength+1;pos++) {
                    sum+=prior.getValueAtRelativePosition(pos);
                    if (randomnumber<=sum) return pos;
                }
            } else {
                 for (int pos=prior.getSize()-1;pos>=motiflength-1;pos--) {
                    sum+=prior.getValueAtRelativePosition(pos);
                    if (randomnumber<=sum) return pos-motiflength+1;
                }
            }
        }
        //engine.logMessage("Not able to sample position based prior. I am sorry, this should normally not happen... :|");
        return -1; // not able to find a position
    }

    /** Preprocesses a priors track to be used for placing a single motif
     *  Based on the selected priors usage the priors track can be masked at the upstream
     *  or downstream boundary, and the priors are normalized so that the track sums to 1.0
     */
    private void setupPriorForSingleMotif(NumericDataset prior, int motiflength, SequenceCollection sequenceCollection, String useforprior) {
        for (int i=0;i<sequenceCollection.size();i++) {
            String sequenceName=sequenceCollection.getSequenceNameByIndex(i);
            NumericSequenceData priorsequence=(NumericSequenceData)prior.getSequenceByName(sequenceName);
            setupPriorForSingleMotif(priorsequence, motiflength, useforprior);
        }
    }

    /** Preprocesses a priors track to be used for placing a single motif
     *  The priors for selected target sequences are normalized, possibly after the last N-1 positions have
     *  their values set to 0 (to avoid placing motif that cross the boundary of the sequence)
     */
    private void setupPriorForSingleMotif(NumericSequenceData priorsequence, int motiflength, String useforprior) {
        int seqLength=priorsequence.getSize();
        if (!useforprior.equals("sum")) {
            boolean maskStart=(useforprior.equals("relativeStartValue") && priorsequence.getStrandOrientation()==Sequence.REVERSE);
            if (!maskStart) {
                for (int j=seqLength-motiflength+1;j<seqLength;j++) {
                    priorsequence.setValueAtRelativePosition(j, 0);
                }
            } else {
                for (int j=0;j<motiflength-1;j++) {
                    priorsequence.setValueAtRelativePosition(j, 0);
                }
            }
        }
        normalizePrior(priorsequence);
    }
    
    /** Normalizes the values in the given NumericSequenceData track so the values sum to 1.0
     *  If all values are 0 the track will be converted to a uniform distribution with all values 1/N.
     */
    private void normalizePrior(NumericSequenceData prior) {
        double sum=0;
        int length=prior.getSize();
        for (int i=0;i<length;i++) {
            sum+=prior.getValueAtRelativePosition(i);
        }
        for (int i=0;i<length;i++) {
            double oldvalue=prior.getValueAtRelativePosition(i);           
            //if (sum==0) prior.setValueAtRelativePosition(i,1.0/(double)length);
            if (sum==0) prior.setValueAtRelativePosition(i,0);
            else prior.setValueAtRelativePosition(i, oldvalue/sum);
        }           
    }

    /**
     * changes the values in a given NumericSequenceData object to be a uniform prior
     * Note the the NumericDataset returned will only contain sequences corresponding to
     * those in the given collection
     */
    private NumericDataset createDefaultPrior(SequenceCollection collection) {
        NumericDataset prior=new NumericDataset("prior");
        for (Sequence seq:collection.getAllSequences(engine)) {
            double length=(double)seq.getSize();
            prior.addSequence(new NumericSequenceData(seq, 1.0/length));
        }
        return prior;
    }

    /**
     * checks if the prior is OK or if there are any negative values
     * @return TRUE if prior contains naegative values
     */
    private boolean priorsGotNegativeValues(NumericSequenceData priorsequence) {
        for (int i=0;i<priorsequence.getSize();i++) {
            if (priorsequence.getValueAtRelativePosition(i)<0) return true;
        }
        return false;
    }

    /**
     * checks if the prior is OK or if there are any negative values
     * @return TRUE if prior contains naegative values
     */
    private boolean priorsGotNegativeValues(NumericDataset priors) {
        for (FeatureSequenceData data:priors.getAllSequences()) {
            NumericSequenceData priorsequence=(NumericSequenceData)data;
            for (int i=0;i<priorsequence.getSize();i++) {
                if (priorsequence.getValueAtRelativePosition(i)<0) return true;
            }
        }
        return false;
    }

    /** Returns the match score for a motif when matched at the given relative position within the sequence
     *  The score returned is just the sum of PWM frequencies corresponding to the bases in the sequence
     */
    private double getMatchScore(char[] pattern, Motif motif) {
         double score=0;
         for (int i=0;i<pattern.length;i++) {
             score+=motif.getBaseFrequency(i, pattern[i]);
         }
         double[] minmax=getMinAndMaxScore(motif);
         return (score-minmax[0])/(minmax[1]-minmax[0]);
    }

    /** Returns the minimum and maximum scores obtainable by a motif when summing frequencies */
    private double[] getMinAndMaxScore(Motif motif) {
         double minscore=0;
         double maxscore=0;
         for (int i=0;i<motif.getLength();i++) {
            double[] freq=new double[]{motif.getBaseFrequency(i,'A'),motif.getBaseFrequency(i,'C'),motif.getBaseFrequency(i,'G'),motif.getBaseFrequency(i,'T')};
            double posmax=0;
            double posmin=2.0; // this should be higher than any value
            for (int j=0;j<freq.length;j++) {
                if (freq[j]>posmax) posmax=freq[j];
                if (freq[j]<posmin) posmin=freq[j];
            }
            minscore+=posmin;
            maxscore+=posmax;
         }
         return new double[]{minscore,maxscore};
    }

    /** Returns the names of the motifs in the given collection.
     *  The names are sorted depending on the size of the corresponding motif,
     *  with shorter motifs being sorted before longer motifs
     */
    private ArrayList<String> getMotifsNamesSortedByMotifLength(MotifCollection collection) {
        ArrayList<String> list=collection.getAllMotifNames();
        MotifLengthComparator comparator=new MotifLengthComparator();
        Collections.sort(list, comparator);
        return list;
    }

    private class MotifLengthComparator implements Comparator<String> {
            @Override
            public int compare(String motifname1, String motifname2) { //
                Motif motif1=(Motif)engine.getDataItem(motifname1);
                Motif motif2=(Motif)engine.getDataItem(motifname2);
                int length1=motif1.getLength();
                int length2=motif2.getLength();
                if (length1<length2) return -1;
                else if (length1>length2) return 1;
                else return 0;
            }
    }

    /** Objects in this class holds information about a chosen constituent motif to be planted */
    private class MotifStruct {
        int modulemotifIndex=0;
        String motifname=null;
        char[] pattern=null;
        int orientation=ModuleCRM.INDETERMINED;
        double score=0;
        int distance=0; // distance to next motif
        public MotifStruct(int modulemotifIndex, String motifname, char[] pattern, int orientation, double score, int distance) {
            this.modulemotifIndex=modulemotifIndex;
            this.motifname=motifname;
            this.pattern=pattern;
            this.orientation=orientation;
            this.score=score;
            this.distance=distance;
        }
    }

    /** This class is used to create a bindingsite with score within certain limits
     *  from a PWM
     */
    private class MotifSampler {
        char[] motifpattern=null;
        double score=0;
        double minMatch=0;
        double maxMatch=0;
        double[][] pwm=null;
        double[][] transitions=null;
        double[] minmax=null;

        /**
         * Samples the PWM of a motif to create a motifpattern with score within the given limits
         * @param motif
         * @param min Suggested minimum score (between 0 and 1)
         * @param max Suggested maximum score (between 0 and 1)
         * @return
         */
        public char[] sample(Motif motif, double min, double max) {
            minMatch=min;
            maxMatch=max;
            pwm=motif.getMatrixAsFrequencyMatrix();
            motifpattern=new char[motif.getLength()];
            if (motifpattern.length==0) return motifpattern; // this should hopefully not happpen
            if (min>0.99) { // use only the best pattern
                for (int i=0;i<motifpattern.length;i++) {
                    motifpattern[i]=getBestBaseForPosition(i);
                }
                return motifpattern;
            } else {                      
                // sample a starting point. If min-score is relatively high then sample according to PWM distribution, else just sample randomly
                for (int i=0;i<motifpattern.length;i++) {
                    if (min>0.6) motifpattern[i]=getSampledBaseForPosition(i);
                    else motifpattern[i]=bases[(int)(Math.random()*4)];
                }
                minmax=getMinAndMaxScore(motif);
                score=getMatchScore(motifpattern);
                if (score>=min && score<=max) {
                     return motifpattern;
                }
                // setup the transition matrix
                transitions=new double[pwm.length][pwm[0].length];
                for (int i=0;i<motifpattern.length;i++) {
                    updateTransitionMatrix(i, motifpattern[i]);
                }
                char[] workingcopy=Arrays.copyOf(motifpattern, motifpattern.length);
                for (int tries=0; tries<100; tries++) {
                    ArrayList<int[]> validtransitions=findTransitionsToWithinLimits(workingcopy); 
                    int[] trans=null;
                    boolean valid=(validtransitions.size()>0);
                    if (validtransitions.isEmpty()) { // no single transition can bring the pattern into score limits
                        validtransitions=findSuggestedTransitions(workingcopy, 0.25); // suggest other transitions
                    }
                    trans=(!validtransitions.isEmpty())?validtransitions.get((int)(Math.random()*validtransitions.size())):new int[]{(int)(Math.random()*workingcopy.length),(int)(Math.random()*4)};
                    int changepos=trans[0];
                    int newbase=trans[1];
                    switch(newbase) {
                        case 0: workingcopy[changepos]='A'; break;
                        case 1: workingcopy[changepos]='C'; break;
                        case 2: workingcopy[changepos]='G'; break;
                        case 3: workingcopy[changepos]='T'; break;
                    }
                    updateTransitionMatrix(changepos, bases[newbase]);
                    double newscore=getMatchScore(workingcopy);
                    double targetscore=minMatch+(maxMatch-minMatch)/2; // target score is midway between min and max
                    if (Math.abs(newscore-targetscore)<Math.abs(score-targetscore)) { // is the new score closer to the target
                        score=newscore;
                        motifpattern=Arrays.copyOf(workingcopy, motifpattern.length);
                    }
                    if (score>=minMatch && score<=maxMatch) break; // success!
                }
                return motifpattern;
            }
        }

        private double getMatchScore(char[] pattern) {
             double matchscore=0;
             for (int i=0;i<pattern.length;i++) {
                 switch(pattern[i]) {
                     case 'A': case 'a': matchscore+=pwm[i][0]; break;
                     case 'C': case 'c': matchscore+=pwm[i][1]; break;
                     case 'G': case 'g': matchscore+=pwm[i][2]; break;
                     case 'T': case 't': matchscore+=pwm[i][3]; break;
                 }

             }
             return (matchscore-minmax[0])/(minmax[1]-minmax[0]);
        }
        
        /** Returns the base with highest frequency in the given position
         *  If there are several bases with this same highest frequency a random base is chosen
         */
        private char getBestBaseForPosition(int pos) {
            double[] freq=pwm[pos];
            double best=0;
            for (int i=0;i<freq.length;i++) if (freq[i]>best) best=freq[i];
            int count=0; // number of bases with this frequency
            for (int i=0;i<freq.length;i++) if (freq[i]==best) count++;
            // now select one of these at random
            int chosen=(int)(Math.random()*count); // chosen is now 0,1,2 or 3 (with max value depending on how many had the highest frequency)
            count=0;
            for (int i=0;i<freq.length;i++) {
                if (freq[i]==best) count++;
                if (count>chosen) return bases[i];
            }
            return 'N'; // this should not happen!
        }

        /** Returns a random base for the motif at the given position. The base is choosen
         *  according to the distribution given by the motifs frequency matrix
         */
        private char getSampledBaseForPosition(int pos) {
            double[] freq=pwm[pos];
            double chosen=Math.random();
            if (chosen==0) chosen=1.0; // a value of 0 is not allowed here but a value of 1 is
            double sum=0;
            for (int i=0;i<freq.length;i++) {
                sum+=freq[i];
                if (chosen<=sum) return bases[i];
            }
            return 'N'; // this should not happen! (unless all bases have frequency=0)
        }
        

        /** Updates the transition matrix at a specific position.
         *  The matrix contains the score-effects of making a base transition in a position
         *  E.g. the value of transition[3][2] will contain the (signed value) effect of
         *  changing the base in position 3 to a C (=2)
         */
        private void updateTransitionMatrix(int pos, char current) {
            double range=minmax[1]-minmax[0];
            double currentscore=0;
            switch(current) {
                 case 'A': case 'a': currentscore=pwm[pos][0]; break;
                 case 'C': case 'c': currentscore=pwm[pos][1]; break;
                 case 'G': case 'g': currentscore=pwm[pos][2]; break;
                 case 'T': case 't': currentscore=pwm[pos][3]; break;
            }
            for (int i=0;i<4;i++) {
                transitions[pos][i]=(pwm[pos][i]-currentscore)/range;
            }
        }

        /** Returns a list of single transitions that can be made which will bring the score
         *  of the given pattern to within limits
         */
        private ArrayList<int[]> findTransitionsToWithinLimits(char[] pattern) {
            ArrayList<int[]> validtransitions=new ArrayList<int[]>();
            double patternscore=getMatchScore(pattern);
            for (int i=0;i<pattern.length;i++) { // for each position
                for (int j=0;j<4;j++) {
                    double newscore=patternscore+transitions[i][j];
                    if (newscore>=minMatch && newscore<=maxMatch) {
                        validtransitions.add(new int[]{i,j});
                    }
                }
            }
            return validtransitions;
        }

       /** Returns a list of suggested transitions that can be made which will bring the score
         * closer to the limits but not fully within them
         *  @parm pattern
         *  @param allowWorseProbability A probability threshold which will allow worse (backtracking) transitions
         *          to be included with a certain probability
         */
        private ArrayList<int[]> findSuggestedTransitions(char[] pattern, double allowWorseProbability) {
            ArrayList<int[]> validtransitions=new ArrayList<int[]>();
            double patternscore=getMatchScore(pattern);
            double targetscore=minMatch+(maxMatch-minMatch)/2; // target score is midway between min and max
            for (int i=0;i<pattern.length;i++) { // for each position
                for (int j=0;j<4;j++) {
                    double newscore=patternscore+transitions[i][j];
                    boolean isbetter=(Math.abs(newscore-targetscore)<Math.abs(patternscore -targetscore));
                    if (isbetter || Math.random()<allowWorseProbability) {
                        validtransitions.add(new int[]{i,j});
                    }
                }
            }
            return validtransitions;
        }


    } // end class MotifSampler

    private void debugModule(Region moduleregion, ArrayList<MotifStruct> cisRegModule) {
        FeatureSequenceData parent=moduleregion.getParent();
        String sequence=moduleregion.getParent().getName();
        engine.logMessage("==Module== ["+sequence+"]   pos="+parent.getGenomicPositionFromRelative(moduleregion.getRelativeStart())+" - "+parent.getGenomicPositionFromRelative(moduleregion.getRelativeEnd())+" ["+moduleregion.getOrientation()+"]");
        int i=0;
        for (MotifStruct motif:cisRegModule) {
            i++;
            engine.logMessage(" ["+i+"]  "+motif.motifname+"("+motif.modulemotifIndex+")  pattern="+(new String(motif.pattern))+"  orientation="+motif.orientation+"  distance="+motif.distance);
        }
        engine.logMessage("\n\n");
    }
}
