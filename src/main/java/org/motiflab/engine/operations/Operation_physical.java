/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericConstant;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;

/**
 *
 * @author kjetikl
 */
public class Operation_physical extends FeatureTransformOperation {
    public static final String WINDOW_SIZE="windowSize"; 
    private static final String WINDOW_SIZE_VALUE="windowSizeValue"; 
    private static final String OLIGO="oligo"; 
    public static final String PHYSICAL_PROPERTY="physhicalProperty"; //
    public static final String ANCHOR="anchor"; //
    public static final String UPSTREAM="start"; //
    public static final String DOWNSTREAM="end"; //
    public static final String CENTER="center"; //
    private static final String name="physical";
    private static final String description="Derives numeric datasets based on physical properties of DNA sequences";
    private Class[] datasourcePreferences=new Class[]{DNASequenceDataset.class};
   
    private static final String GC_CONTENT="GC-content";
    private static final String AT_CONTENT="AT-content";
    private static final String GC_SKEW="GC-skew";
    private static final String AT_SKEW="AT-skew";
    private static final String STACKING_ENERGY="stacking energy";
    private static final String PROPELLER_TWIST="propeller twist";
    private static final String A_PHILICITY="A-philicity";
    private static final String PROTEIN_INDUCED_DEFORMABILITY="protein-induced deformability";
    private static final String DUPLEX_FREE_ENERGY="duplex free energy";
    private static final String DUPLEX_DISRUPT_ENERGY="duplex disrupt energy";
    private static final String DNA_DENATURATION="DNA denaturation";
    private static final String DNA_BENDING_STIFFNESS="DNA bending-stiffness";
    private static final String B_DNA_TWIST="B-DNA twist";
    private static final String PROTEIN_DNA_TWIST="protein-DNA twist";
    private static final String Z_DNA_STABILIZING_ENERGY="Z-DNA stabilizing energy";
    private static final String NUCLEOSOME_POSITIONING="nucleosome position preference";
    private static final String BENDABILITY="bendability";
    public static final String FREQUENCY="frequency";
    
    private HashMap<String,Table> tables;
    
    public static String[] getWindowAnchors() {return new String[]{CENTER,UPSTREAM,DOWNSTREAM};}
     
    public static boolean isRecognizedAnchor(String anchor) {
        for (String s:getWindowAnchors()) {
            if (s.equals(anchor)) return true;
        }
        return false;
    }    
    
    public Operation_physical() {
        tables=new HashMap<String,Table>();
        Table stackingEnergyTable=new Table();
        stackingEnergyTable.addOligoAndReverse("AA", -5.47);
        stackingEnergyTable.addOligoAndReverse("AC", -10.51);
        stackingEnergyTable.addOligoAndReverse("AG", -6.78);
        stackingEnergyTable.addOligoAndReverse("AT", -6.57);
        stackingEnergyTable.addOligoAndReverse("CA", -6.57);
        stackingEnergyTable.addOligoAndReverse("CC", -8.26);
        stackingEnergyTable.addOligoAndReverse("CG", -9.69);
        stackingEnergyTable.addOligoAndReverse("GA", -9.81);
        stackingEnergyTable.addOligoAndReverse("GC", -14.59);
        stackingEnergyTable.addOligoAndReverse("TA", -3.821);
        tables.put(STACKING_ENERGY,stackingEnergyTable);

        Table propellerTwistTable=new Table();
        propellerTwistTable.addOligoAndReverse("AA", -18.66);
        propellerTwistTable.addOligoAndReverse("AC", -13.10);
        propellerTwistTable.addOligoAndReverse("AG", -14.00);
        propellerTwistTable.addOligoAndReverse("AT", -15.01);
        propellerTwistTable.addOligoAndReverse("CA", -9.45);
        propellerTwistTable.addOligoAndReverse("CC", -8.11);
        propellerTwistTable.addOligoAndReverse("CG", -10.03);
        propellerTwistTable.addOligoAndReverse("GA", -13.48);
        propellerTwistTable.addOligoAndReverse("GC", -11.08);
        propellerTwistTable.addOligoAndReverse("TA", -11.851);
        tables.put(PROPELLER_TWIST,propellerTwistTable);        

        Table proteinIndDeformTable=new Table();
        proteinIndDeformTable.addOligoAndReverse("AA", 2.9);
        proteinIndDeformTable.addOligoAndReverse("AC", 2.3);
        proteinIndDeformTable.addOligoAndReverse("AG", 2.1);
        proteinIndDeformTable.addOligoAndReverse("AT", 1.6);
        proteinIndDeformTable.addOligoAndReverse("CA", 9.8);
        proteinIndDeformTable.addOligoAndReverse("CC", 6.1);
        proteinIndDeformTable.addOligoAndReverse("CG", 12.1);
        proteinIndDeformTable.addOligoAndReverse("GA", 4.5);
        proteinIndDeformTable.addOligoAndReverse("GC", 4.0);
        proteinIndDeformTable.addOligoAndReverse("TA", 6.3);
        tables.put(PROTEIN_INDUCED_DEFORMABILITY,proteinIndDeformTable);        
        
        Table duplexfreenergyTable=new Table();
        duplexfreenergyTable.addOligoAndReverse("AA", -1.2);
        duplexfreenergyTable.addOligoAndReverse("AC", -1.5);
        duplexfreenergyTable.addOligoAndReverse("AG", -1.5);
        duplexfreenergyTable.addOligoAndReverse("AT", -0.9);
        duplexfreenergyTable.addOligoAndReverse("CA", -1.7);
        duplexfreenergyTable.addOligoAndReverse("CC", -2.3);
        duplexfreenergyTable.addOligoAndReverse("CG", -2.8);
        duplexfreenergyTable.addOligoAndReverse("GA", -1.5);
        duplexfreenergyTable.addOligoAndReverse("GC", -2.3);
        duplexfreenergyTable.addOligoAndReverse("TA", -0.9);
        tables.put(DUPLEX_FREE_ENERGY,duplexfreenergyTable);  
        
        Table duplexdisruptenergyTable=new Table();
        duplexdisruptenergyTable.addOligoAndReverse("AA", 1.9);
        duplexdisruptenergyTable.addOligoAndReverse("AC", 1.3);
        duplexdisruptenergyTable.addOligoAndReverse("AG", 1.6);
        duplexdisruptenergyTable.addOligoAndReverse("AT", 0.9);
        duplexdisruptenergyTable.addOligoAndReverse("CA", 1.9);
        duplexdisruptenergyTable.addOligoAndReverse("CC", 3.1);
        duplexdisruptenergyTable.addOligoAndReverse("CG", 3.6);
        duplexdisruptenergyTable.addOligoAndReverse("GA", 1.6);
        duplexdisruptenergyTable.addOligoAndReverse("GC", 3.1);
        duplexdisruptenergyTable.addOligoAndReverse("TA", 1.5);
        tables.put(DUPLEX_DISRUPT_ENERGY,duplexdisruptenergyTable);        
        
        Table denaturationTable=new Table();
        denaturationTable.addOligoAndReverse("AA", 66.51);
        denaturationTable.addOligoAndReverse("AC", 108.80);
        denaturationTable.addOligoAndReverse("AG", 85.12);
        denaturationTable.addOligoAndReverse("AT", 72.29);
        denaturationTable.addOligoAndReverse("CA", 64.92);
        denaturationTable.addOligoAndReverse("CC", 99.31);
        denaturationTable.addOligoAndReverse("CG", 88.84);
        denaturationTable.addOligoAndReverse("GA", 80.03);
        denaturationTable.addOligoAndReverse("GC", 153.83);
        denaturationTable.addOligoAndReverse("TA", 50.11);
        tables.put(DNA_DENATURATION,denaturationTable);        
        
        Table bendingstiffnessTable=new Table();
        bendingstiffnessTable.addOligoAndReverse("AA", 35.0);
        bendingstiffnessTable.addOligoAndReverse("AC", 60.0);
        bendingstiffnessTable.addOligoAndReverse("AG", 60.0);
        bendingstiffnessTable.addOligoAndReverse("AT", 20.0);
        bendingstiffnessTable.addOligoAndReverse("CA", 60.0);
        bendingstiffnessTable.addOligoAndReverse("CC", 130.0);
        bendingstiffnessTable.addOligoAndReverse("CG", 85.0);
        bendingstiffnessTable.addOligoAndReverse("GA", 60.0);
        bendingstiffnessTable.addOligoAndReverse("GC", 85.0);
        bendingstiffnessTable.addOligoAndReverse("TA", 20.0);
        tables.put(DNA_BENDING_STIFFNESS,bendingstiffnessTable);        

        Table bdnatwist=new Table();
        bdnatwist.addOligoAndReverse("AA", 35.5);
        bdnatwist.addOligoAndReverse("AC", 33.1);
        bdnatwist.addOligoAndReverse("AG", 30.6);
        bdnatwist.addOligoAndReverse("AT", 43.2);
        bdnatwist.addOligoAndReverse("CA", 37.7);
        bdnatwist.addOligoAndReverse("CC", 35.3);
        bdnatwist.addOligoAndReverse("CG", 31.3);
        bdnatwist.addOligoAndReverse("GA", 39.6);
        bdnatwist.addOligoAndReverse("GC", 38.4);
        bdnatwist.addOligoAndReverse("TA", 31.6);
        tables.put(B_DNA_TWIST,bdnatwist);        

        Table proteintwist=new Table();
        proteintwist.addOligoAndReverse("AA", 35.1);
        proteintwist.addOligoAndReverse("AC", 31.5);
        proteintwist.addOligoAndReverse("AG", 31.9);
        proteintwist.addOligoAndReverse("AT", 29.3);
        proteintwist.addOligoAndReverse("CA", 37.3);
        proteintwist.addOligoAndReverse("CC", 32.9);
        proteintwist.addOligoAndReverse("CG", 36.1);
        proteintwist.addOligoAndReverse("GA", 36.3);
        proteintwist.addOligoAndReverse("GC", 33.6);
        proteintwist.addOligoAndReverse("TA", 37.8);
        tables.put(PROTEIN_DNA_TWIST,proteintwist);        
    
        Table zdnastabilizingenergy=new Table();
        zdnastabilizingenergy.addOligoAndReverse("AA", 3.9);
        zdnastabilizingenergy.addOligoAndReverse("AC", 4.6);
        zdnastabilizingenergy.addOligoAndReverse("AG", 3.4);
        zdnastabilizingenergy.addOligoAndReverse("AT", 5.9);
        zdnastabilizingenergy.addOligoAndReverse("CA", 1.3);
        zdnastabilizingenergy.addOligoAndReverse("CC", 2.4);
        zdnastabilizingenergy.addOligoAndReverse("CG", 0.7);
        zdnastabilizingenergy.addOligoAndReverse("GA", 3.4);
        zdnastabilizingenergy.addOligoAndReverse("GC", 4.0);
        zdnastabilizingenergy.addOligoAndReverse("TA", 2.5);
        tables.put(Z_DNA_STABILIZING_ENERGY,zdnastabilizingenergy); 
        
        Table nucleosome_position=new Table();
        nucleosome_position.addOligoAndReverse("AAA", -36.0);
        nucleosome_position.addOligoAndReverse("AAC", -6.0);
        nucleosome_position.addOligoAndReverse("AAG", 6.0);
        nucleosome_position.addOligoAndReverse("AAT", -30.0);
        nucleosome_position.addOligoAndReverse("ACA", 6.0);
        nucleosome_position.addOligoAndReverse("ACC", 8.0);
        nucleosome_position.addOligoAndReverse("ACG", 8.0);
        nucleosome_position.addOligoAndReverse("ACT", 11.0);
        nucleosome_position.addOligoAndReverse("AGA", -9.0);
        nucleosome_position.addOligoAndReverse("AGC", 25.0);
        nucleosome_position.addOligoAndReverse("AGG", 8.0);
        nucleosome_position.addOligoAndReverse("ATA", -13.0);
        nucleosome_position.addOligoAndReverse("ATC", 7.0);
        nucleosome_position.addOligoAndReverse("ATG", 18.0);
        nucleosome_position.addOligoAndReverse("CAA", -9.0);
        nucleosome_position.addOligoAndReverse("CAC", 17.0);
        nucleosome_position.addOligoAndReverse("CAG", -2.0);
        nucleosome_position.addOligoAndReverse("CCA", 8.0);
        nucleosome_position.addOligoAndReverse("CCC", 13.0);
        nucleosome_position.addOligoAndReverse("CCG", 2.0);
        nucleosome_position.addOligoAndReverse("CGA", 31.0);
        nucleosome_position.addOligoAndReverse("CGC", 25.0);
        nucleosome_position.addOligoAndReverse("CTA", -18.0);
        nucleosome_position.addOligoAndReverse("CTC", 8.0);
        nucleosome_position.addOligoAndReverse("GAA", -12.0);
        nucleosome_position.addOligoAndReverse("GAC", 8.0);
        nucleosome_position.addOligoAndReverse("GCA", 13.0);
        nucleosome_position.addOligoAndReverse("GCC", 45.0);
        nucleosome_position.addOligoAndReverse("GGA", -5.0);
        nucleosome_position.addOligoAndReverse("GTA", -6.0);
        nucleosome_position.addOligoAndReverse("TAA", -20.0);
        nucleosome_position.addOligoAndReverse("TCA", 8.0);
        tables.put(NUCLEOSOME_POSITIONING,nucleosome_position);        
   
        Table bendability=new Table();
        bendability.addOligoAndReverse("AAA", -0.274);
        bendability.addOligoAndReverse("AAC", -0.204);
        bendability.addOligoAndReverse("AAG", -0.081);
        bendability.addOligoAndReverse("AAT", -0.28);
        bendability.addOligoAndReverse("ACA", -0.006);
        bendability.addOligoAndReverse("ACC", -0.032);
        bendability.addOligoAndReverse("ACG", -0.033);
        bendability.addOligoAndReverse("ACT", -0.183);
        bendability.addOligoAndReverse("AGA", 0.027);
        bendability.addOligoAndReverse("AGC", 0.017);
        bendability.addOligoAndReverse("AGG", -0.057);
        bendability.addOligoAndReverse("ATA", 0.182);
        bendability.addOligoAndReverse("ATC", -0.110);
        bendability.addOligoAndReverse("ATG", 0.134);
        bendability.addOligoAndReverse("CAA", 0.015);
        bendability.addOligoAndReverse("CAC", 0.040);
        bendability.addOligoAndReverse("CAG", 0.175);
        bendability.addOligoAndReverse("CCA", -0.246);
        bendability.addOligoAndReverse("CCC", -0.012);
        bendability.addOligoAndReverse("CCG", -0.136);
        bendability.addOligoAndReverse("CGA", -0.003);
        bendability.addOligoAndReverse("CGC", -0.077);
        bendability.addOligoAndReverse("CTA", 0.09);
        bendability.addOligoAndReverse("CTC", 0.031);
        bendability.addOligoAndReverse("GAA", -0.037);
        bendability.addOligoAndReverse("GAC", -0.013);
        bendability.addOligoAndReverse("GCA", 0.076);
        bendability.addOligoAndReverse("GCC", 0.107);
        bendability.addOligoAndReverse("GGA", 0.0135);
        bendability.addOligoAndReverse("GTA", 0.025);
        bendability.addOligoAndReverse("TAA", 0.068);
        bendability.addOligoAndReverse("TCA", 0.194);
        tables.put(BENDABILITY,bendability);               
    }            

    @Override
    public String getOperationGroup() {
        return "Derive";
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
        String windowSizeString=(String)task.getParameter(WINDOW_SIZE);
        Data windowSizeData=null;
        windowSizeData=engine.getDataItem(windowSizeString);
        if (windowSizeData==null) {
            try {
              double value=Double.parseDouble(windowSizeString);
              windowSizeData=new NumericConstant(windowSizeString, (double)value);
           } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+windowSizeString+"' neither data nor numeric constant",task.getLineNumber());}         
        }
        task.setParameter(WINDOW_SIZE_VALUE, windowSizeData);
        String property=(String)task.getParameter(PHYSICAL_PROPERTY); 
        if (property.equals(FREQUENCY)) throw new ExecutionError("Missing nucleotide pattern specification for frequency property");
        if (property.startsWith(FREQUENCY+":")) {
            if (property.length()<FREQUENCY.length()+2) throw new ExecutionError("Missing nucleotide pattern specification for frequency property");
            task.setParameter(OLIGO, property.substring(FREQUENCY.length()+1).toUpperCase()); 
        }
    }
    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String sourceDatasetName=task.getSourceDataName();
        String targetDatasetName=task.getTargetDataName();        
        if (sourceDatasetName==null || sourceDatasetName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());         
        FeatureDataset sourceDataset=(FeatureDataset)engine.getDataItem(sourceDatasetName);
        if (sourceDataset==null) throw new ExecutionError("Unknown data object '"+sourceDatasetName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceDataset)) throw new ExecutionError(sourceDatasetName+"("+sourceDataset.getTypeDescription()+") is of a type not supported by the '"+getName()+"' operation",task.getLineNumber());
        NumericDataset targetDataset=new NumericDataset(targetDatasetName);
        ArrayList<Data> sequenceList=engine.getAllDataItemsOfType(Sequence.class);
        for (Data seq:sequenceList) {
            NumericSequenceData numericSeq=new NumericSequenceData((Sequence)seq, 0);
            targetDataset.addSequence(numericSeq);
        }
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) condition.resolve(engine, task);        
        
        Condition_within within=(Condition_within)task.getParameter("within");
        if (within!=null) within.resolve(engine, task);
        
        resolveParameters(task);
        
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        if (isSubrangeApplicable() && within!=null) { // remove sequences with no selection windows (if within-condition is used)
            Iterator iter=sequences.iterator();
            while (iter.hasNext()) {
                Sequence seq = (Sequence) iter.next();
                if (!within.existsSelectionWithinSequence(seq.getName(), task)) iter.remove();
            }           
        }
        
        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,sequences.size());
        long[] counters=new long[]{0,0,sequences.size()}; // counters[0]=sequences started, [1]=sequences completed, [2]=total number of sequences

        ArrayList<ProcessSequenceTask> processTasks=new ArrayList<ProcessSequenceTask>(sequences.size());
        for (Sequence sequence:sequences) processTasks.add(new ProcessSequenceTask(sourceDataset,targetDataset, sequence.getName(), task, counters));
        List<Future<FeatureSequenceData>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(processTasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<FeatureSequenceData> future:futures) {
                if (future.isDone() && !future.isCancelled()) {
                    future.get(); // this blocks until completion but the return value is not used
                    countOK++;
                }
            }
        } catch (Exception e) {  
           taskRunner.shutdownNow(); // Note: this will abort all executing tasks (even those that did not cause the exception), but that is OK. 
           if (e instanceof java.util.concurrent.ExecutionException) throw (Exception)e.getCause(); 
           else throw e; 
        }       
        if (countOK!=sequences.size()) {
            throw new ExecutionError("Some mysterious error occurred while performing operation: "+getName());
        }          
        
        
        if (targetDataset instanceof NumericDataset) ((NumericDataset)targetDataset).updateAllowedMinMaxValuesFromData();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        targetDataset.setIsDerived(true);
        try {engine.updateDataItem(targetDataset);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String physicalProperty=(String)task.getParameter(PHYSICAL_PROPERTY);
        Data windowSizeData=(Data)task.getParameter(WINDOW_SIZE_VALUE);
        String anchor=(String)task.getParameter(ANCHOR);
        int windowsize=5;   
        String seqname=sourceSequence.getName();
        if (windowSizeData instanceof SequenceNumericMap) windowsize=(int)((SequenceNumericMap)windowSizeData).getValue(seqname).doubleValue();
        else if (windowSizeData instanceof NumericVariable) windowsize=(int)((NumericVariable)windowSizeData).getValue().doubleValue();
        else if (windowSizeData instanceof NumericConstant) windowsize=(int)((NumericConstant)windowSizeData).getValue().doubleValue();
        if (windowsize==0) throw new ExecutionError("Window size can not be zero");            
        int orientation=sourceSequence.getStrandOrientation();
        String oligo=(String)task.getParameter(OLIGO);
        int reverseOffset=0; // might be set to a larger value to offset window on reverse strand
        if (orientation==Sequence.REVERSE) {
           if (oligo!=null) {
               oligo=MotifLabEngine.reverseSequence(oligo); // for reverse strand search
               reverseOffset=oligo.length()-1;
           } else if (tables.containsKey(physicalProperty)) {
               reverseOffset=getOligoLength(physicalProperty)-1;
           }
        }
        for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
            int start=0, end=0;
            if (anchor.equals(UPSTREAM)) {
                start=(orientation==Sequence.DIRECT)?i:i-windowsize+1;
                end=(orientation==Sequence.DIRECT)?i+windowsize-1:i;
            } else if (anchor.equals(DOWNSTREAM)) {
                start=(orientation==Sequence.DIRECT)?i-windowsize+1:i;                
                end=(orientation==Sequence.DIRECT)?i:i+windowsize-1;
            } else {
               if (windowsize%2==0) { // window has even number of bases - anchor left of center
                   int flanksize=(int)(windowsize/2);
                   start=(orientation==Sequence.DIRECT)?i-flanksize+1:i-flanksize;
                   end=(orientation==Sequence.DIRECT)?i+flanksize:i+flanksize-1;                   
               } else { // window has odd number of bases
                   int flanksize=(int)(windowsize/2);
                   start=i-flanksize;
                   end=i+flanksize;
               }
            }
            start+=reverseOffset;
            end+=reverseOffset;
            if (positionSatisfiesCondition(seqname,i,task)) {
              double newvalue=getNewValue((DNASequenceData)sourceSequence,start,end,physicalProperty, orientation, oligo);
              ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i, newvalue);
           } // satisfies 'where'-condition
        }
    }
    
    /** Start and end are genomic positions */
    private double getNewValue(DNASequenceData sourceSequence, int start, int end, String physicalProperty, int orientation, String oligo) throws ExecutionError {
        if (physicalProperty==null) physicalProperty=GC_CONTENT;
        if (physicalProperty.equalsIgnoreCase(GC_CONTENT)) {
            double GCs=0;
            double counted=0;
            for (int j=start;j<=end;j++) {
                if (j<sourceSequence.getRegionStart() || j>sourceSequence.getRegionEnd()) continue; // outside sequence. just skip
                counted++; // How many bases are included
                char base=sourceSequence.getValueAtGenomicPosition(j);
                if (base=='C' || base=='G' || base=='c' || base=='g') GCs++;
            }
            if (counted>0) return GCs/counted;
            else return 0;          
        } else if (physicalProperty.equalsIgnoreCase(GC_SKEW)) {
            double GCs=0;
            double Gs=0;
            double Cs=0;
            double counted=0;
            for (int j=start;j<=end;j++) {
                if (j<sourceSequence.getRegionStart() || j>sourceSequence.getRegionEnd()) continue; // outside sequence. just skip
                counted++; // How many bases are included
                char base=sourceSequence.getValueAtGenomicPosition(j);
                if (base=='G' || base=='g') {Gs++;GCs++;}
                else if (base=='C' || base=='c') {Cs++;GCs++;}
            }
            double GCdiff=(orientation==Sequence.DIRECT)?(Gs-Cs):(Cs-Gs);
            if (counted>0) return GCdiff/counted;
            else return 0;          
        } else if (physicalProperty.equalsIgnoreCase(AT_CONTENT)) {
            double ATs=0;
            double counted=0;
            for (int j=start;j<=end;j++) {
                if (j<sourceSequence.getRegionStart() || j>sourceSequence.getRegionEnd()) continue; // outside sequence. just skip
                counted++; // How many bases are included
                char base=sourceSequence.getValueAtGenomicPosition(j);
                if (base=='A' || base=='T' || base=='a' || base=='t') ATs++;
            }
            if (counted>0) return ATs/counted;
            else return 0;          
        } else if (physicalProperty.equalsIgnoreCase(AT_SKEW)) {
            double ATs=0;
            double As=0;
            double Ts=0;
            double counted=0;
            for (int j=start;j<=end;j++) {
                if (j<sourceSequence.getRegionStart() || j>sourceSequence.getRegionEnd()) continue; // outside sequence. just skip
                counted++; // How many bases are included
                char base=sourceSequence.getValueAtGenomicPosition(j);
                if (base=='A' || base=='a') {As++;ATs++;}
                else if (base=='T' || base=='t') {Ts++;ATs++;}
            }
            double ATdiff=(orientation==Sequence.DIRECT)?(As-Ts):(Ts-As);
            if (counted>0) return ATdiff/counted;
            else return 0;                    
        } else if (physicalProperty.startsWith(FREQUENCY)) {
            double oligomatch=0;
            double counted=0;
            int oligolength=oligo.length();
            for (int j=start;j<=end;j++) {
                if (j<sourceSequence.getRegionStart() || j>sourceSequence.getRegionEnd()-oligolength+1) continue; // outside sequence. just skip
                counted++; // How many positions are included in the window
                if (oligolength==1) {
                    char c=sourceSequence.getValueAtGenomicPosition(j);
                    if (Character.toUpperCase(c)==oligo.charAt(0)) oligomatch++;
                } else {
                   if (sourceSequence.isDirectMatchAtPosition(j,oligo)) oligomatch++; 
                }                     
            }
            if (counted>0) return oligomatch/counted;
            else return 0;          
        } else {
            Table table=tables.get(physicalProperty);
            int oligolength=getOligoLength(physicalProperty);
            if (table==null) throw new ExecutionError("Unknown physical property function: "+physicalProperty);
            char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(start, end+1); // +1 for dinucleotide. +2 for trinucleotides
            if (sequence==null) return 0; // window wholly or partially outside of sequence
            double result=0;
            for (int j=0;j<sequence.length-oligolength+1;j++) { // -1 for dinucleotides, -2 for trinucleotides
                 Double value=table.get(new String(sequence,j,oligolength).toUpperCase());
                 // no need to reverse sequence since all properties in tables are "symmetric"
                 if (value!=null) result+=value;
            }
            return result;             
        }

    }
    
    
    /** Returns the length of the n-nucleotide words that are used for calculating the given physical property 
     *  This is usually either a di- or trinucleotide (or single nucleotide for frequencies).
     *  Note that this value is different from the window-length
     */
    private int getOligoLength(String property) {
             if (property.equals(BENDABILITY) || property.equals(NUCLEOSOME_POSITIONING)) return 3;
        else return 2;
    }
    
    
    public static String[] getPhysicalProperties() {return new String[]{GC_CONTENT,GC_SKEW,AT_CONTENT,AT_SKEW,FREQUENCY,STACKING_ENERGY,PROPELLER_TWIST,NUCLEOSOME_POSITIONING,BENDABILITY,PROTEIN_INDUCED_DEFORMABILITY,DUPLEX_FREE_ENERGY,DUPLEX_DISRUPT_ENERGY,DNA_DENATURATION,DNA_BENDING_STIFFNESS,B_DNA_TWIST,PROTEIN_DNA_TWIST,Z_DNA_STABILIZING_ENERGY};}
    
    private class Table extends HashMap<String,Double> {
         private void addOligoAndReverse(String oligo, Double value) {
             put(oligo, value);
             put(MotifLabEngine.reverseSequence(oligo),value);                    
         }
         
    }

}
