/*
 
 
 */

package org.motiflab.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.TaskRunner;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.TextVariable;

/**
 *
 * @author kjetikl
 */
public class StringScanner extends MotifScanning {
    private static final int reportSpanLength=50000;

    public StringScanner() {
        this.name="StringScanner";
        this.programclass="MotifScanning";
        this.serviceType="bundled";

        addSourceParameter("Sequence", DNASequenceDataset.class, null, null, "input sequences");
        addParameter("Motif Collection",Data.class, null,new Class[]{MotifCollection.class,Motif.class,TextVariable.class},"A single motif or collection of motifs with binding sequences included as a user-defined properly, or a Text Variable containing the strings to search for",true,false);
        addParameter("Binding sequence property", String.class, "Binding sequences", null,"<html>This parameter should name the user-defined motif property holding the list of binding sequences for the motif (unless the strings are listed in a Text Variable)</html>",false,false);   
        addParameter("Allow variable lengths", Boolean.class, Boolean.FALSE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"<html>If selected, the annotated binding sequences for a motif is allowed to have different lengths</html>",false,false);   
        addResultParameter("Result", RegionDataset.class, null, null, "output track");
    }     
    
    @Override
    public void execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String targetDatasetName=task.getTargetDataName();
        Data[] sources=(Data[])task.getParameter(SOURCES);
        if (sources==null || sources.length==0) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for motif scanning with StringScanner");
        task.setStatusMessage("Executing StringScanner:  Preprocessing");
        DNASequenceDataset sourceDataset=(DNASequenceDataset)sources[0];
        RegionDataset targetDataset=new RegionDataset(targetDatasetName);
        ArrayList<Data> allsequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data sequence:allsequences) {
             RegionSequenceData regionsequence=new RegionSequenceData((Sequence)sequence);
             targetDataset.addSequence(regionsequence);
        }
        
        boolean allowVariableLengths=(Boolean)task.getParameter("Allow variable lengths");       
        HashMap<String, Object> motifs=new HashMap<String, Object>(); // for variable length motifs the value will be a HashMap<Integer,HashSet<String>> else the value will be only HashSet<String> 

        String propertyName=(String)task.getParameter("Binding sequence property");
        
        Data motifdata=(Data)task.getParameter("Motif Collection");
        if (motifdata instanceof Motif) {
            if (propertyName==null || propertyName.trim().isEmpty()) throw new ExecutionError("Missing name of motif's binding sequence property");
            Object strings=processSingleMotif((Motif)motifdata,propertyName,allowVariableLengths);
            if (strings==null) throw new ExecutionError("Motif "+motifdata.getName()+" does not have any annotated binding sequences");
            motifs.put(motifdata.getName(),strings);
            engine.logMessage("Searching for "+getSetSize(strings)+" different binding sequences for "+motifdata.getName(),5);            
        } else if (motifdata instanceof MotifCollection) {
            if (propertyName==null || propertyName.trim().isEmpty()) throw new ExecutionError("Missing name of motif's binding sequence property");            
            ArrayList<Motif> allMotifs=((MotifCollection)motifdata).getAllMotifs(engine);
            for (Motif motif:allMotifs) { // 
                Object strings=processSingleMotif(motif,propertyName,allowVariableLengths);
                if (strings==null) throw new ExecutionError("Motif "+motif.getName()+" does not have any annotated binding sequences");
                motifs.put(motif.getName(),strings);
                engine.logMessage("Searching for "+getSetSize(strings)+" different binding sequences for "+motifdata.getName(),5);                
            }             
        } else if (motifdata instanceof TextVariable) {
            Object strings=indexStrings(((TextVariable)motifdata).getAllStrings(),motifdata.getName(),allowVariableLengths);
            if (strings==null) throw new ExecutionError("Text Variable "+motifdata.getName()+" does not contain any binding sequences");
            motifs.put(motifdata.getName(),strings);      
            engine.logMessage("Searching for "+getSetSize(strings)+" different binding sequences for "+motifdata.getName(),5);            
        } else throw new ExecutionError("'"+motifdata.getName()+"' is neither a Motif, Motif Collection or Text Variable");
        
        task.setStatusMessage("Executing StringScanner:  Scanning");   
        
        SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION); 
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequences(engine);
        long totalsize=0;
        for (Sequence sequence:sequences) {totalsize+=sequence.getSize();}
        totalsize=totalsize*motifs.size(); // total number of bp to search by all motifs combined

        TaskRunner taskRunner=engine.getTaskRunner();
        task.setProgress(0L,totalsize);
        long[] counters=new long[]{0,0,sequences.size(),0,totalsize}; // counters[0]=#sequences started, [1]=#sequences completed, [2]=#total number of sequences, [3]=total bp processed so far (by all motifs), [4]=total number of bp to process (combined sequence lengths * number of motifs)       

        ArrayList<ScanSequenceTask<RegionSequenceData>> scantasks=new ArrayList<ScanSequenceTask<RegionSequenceData>>(sequences.size());
        for (Sequence sequence:sequences) scantasks.add(new ScanSequenceTask<RegionSequenceData>(sourceDataset, targetDataset, sequence.getName(), motifs, allowVariableLengths, task, counters));
        List<Future<RegionSequenceData>> futures=null;
        int countOK=0;            
        try {
            futures=taskRunner.invokeAll(scantasks); // this call apparently blocks until all tasks finish (either normally or by exceptions or being cancelled)                             
            for (Future<RegionSequenceData> future:futures) {
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
            throw new ExecutionError("Some mysterious error occurred while scanning");
        }             

        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setParameter("Result", targetDataset);   
    }        
    
    
    private int getSetSize(Object set) {
        if (set instanceof HashSet) {
            return ((HashSet)set).size();
        } else if (set instanceof HashMap) {
            int count=0;
            for (Object key:((HashMap)set).keySet()) {
                HashSet<String> val=(HashSet<String>)((HashMap)set).get(key);
                if (val!=null) count+=val.size();
            }
            return count;
            
        } return 0;
    }
    
    public void searchInSequence(DNASequenceData sourceSequence, RegionSequenceData targetSequence, HashMap<String,HashSet<String>> motifs, OperationTask task, long[] counters) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        if (sequence==null) {
            if (sourceSequence.getRegionEnd()<startOffset) throw new ExecutionError("Something is wrong with the sequence \""+seqname+"\". Genomic end coordinate ("+sourceSequence.getRegionEnd()+") is before start coordinate ("+startOffset+").");
            else throw new ExecutionError("Empty sequence segment: "+seqname+":"+startOffset+"-"+sourceSequence.getRegionEnd());
        }
        int reportEvery=(sequence.length>reportSpanLength)?reportSpanLength:Integer.MAX_VALUE; // for long sequences, update progress every 10Kbp
        
        int counter=0;
        int bpProcessedSinceLastUpdated=0;
        for (String motifname:motifs.keySet()) { // search with each motif in turn
            counter++;     
            HashSet<String> bindingsequences=motifs.get(motifname);
            String first=bindingsequences.iterator().next();
            int motifsize=first.length();
            
            for (int i=0;i<=sequence.length-motifsize;i++) {
                bpProcessedSinceLastUpdated++;
                String directSequence=new String(sequence, i, motifsize);
                String reverseSequence=MotifLabEngine.reverseSequence(directSequence);
                
                if (bindingsequences.contains(directSequence)) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,1,Region.DIRECT);
                     targetSequence.addRegionWithoutSorting(region);
                }
                if (bindingsequences.contains(reverseSequence)) {
                     Region region=new Region(targetSequence,i,i+motifsize-1,motifname,1,Region.REVERSE);
                     targetSequence.addRegionWithoutSorting(region);
                }
                if (bpProcessedSinceLastUpdated==reportEvery) {
                    synchronized(counters) { 
                        counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                        bpProcessedSinceLastUpdated=0;
                        task.setProgress(counters[3],counters[4]);
                    }
                    task.checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                                   
                    Thread.yield();
                }
            } // end: for each bp in sequence
            synchronized(counters) { 
                counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                bpProcessedSinceLastUpdated=0;
                task.setProgress(counters[3],counters[4]);
            }         
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                            
            Thread.yield();
        } // end: for each motif          
        targetSequence.updateRegionSortOrder(); // this must be called since regions were added unsorted       
    }
    
    
    public void searchInSequenceVariable(DNASequenceData sourceSequence, RegionSequenceData targetSequence, HashMap<String,HashMap<Integer,HashSet<String>>> motifs, OperationTask task, long[] counters) throws Exception {
        String seqname=sourceSequence.getName();        
        int startOffset=sourceSequence.getRegionStart();             
        char[] sequence=(char[])sourceSequence.getValueInGenomicInterval(startOffset, sourceSequence.getRegionEnd());
        if (sequence==null) {
            if (sourceSequence.getRegionEnd()<startOffset) throw new ExecutionError("Something is wrong with the sequence \""+seqname+"\". Genomic end coordinate ("+sourceSequence.getRegionEnd()+") is before start coordinate ("+startOffset+").");
            else throw new ExecutionError("Empty sequence segment: "+seqname+":"+startOffset+"-"+sourceSequence.getRegionEnd());
        }
        int reportEvery=(sequence.length>reportSpanLength)?reportSpanLength:Integer.MAX_VALUE; // for long sequences, update progress every 10Kbp
        
        int counter=0;
        double bpProcessedSinceLastUpdated=0;
        for (String motifname:motifs.keySet()) { // search with each motif in turn
            counter++;     
            HashMap<Integer,HashSet<String>> subgroups=motifs.get(motifname);
            int subgroupcount=subgroups.size();
            
            for (Integer motifsize:subgroups.keySet()) {
                HashSet<String> bindingsequences=subgroups.get(motifsize);
                for (int i=0;i<=sequence.length-motifsize;i++) {
                    bpProcessedSinceLastUpdated+=(1.0/subgroupcount);
                    String directSequence=new String(sequence, i, motifsize);
                    String reverseSequence=MotifLabEngine.reverseSequence(directSequence);

                    if (bindingsequences.contains(directSequence)) {
                         Region region=new Region(targetSequence,i,i+motifsize-1,motifname,1,Region.DIRECT);
                         targetSequence.addRegionWithoutSorting(region);
                    }
                    if (bindingsequences.contains(reverseSequence)) {
                         Region region=new Region(targetSequence,i,i+motifsize-1,motifname,1,Region.REVERSE);
                         targetSequence.addRegionWithoutSorting(region);
                    }
                    if (bpProcessedSinceLastUpdated>=reportEvery) {
                        synchronized(counters) { 
                            counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                            bpProcessedSinceLastUpdated=0;
                            task.setProgress(counters[3],counters[4]);
                        }
                        task.checkExecutionLock(); // checks to see if this task should suspend execution
                        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                                   
                        Thread.yield();
                    }
               } // end: for each bp in sequence
            } // end for each length subgroup
            synchronized(counters) { 
                counters[3]+=bpProcessedSinceLastUpdated; // number of bp processed so far in total
                bpProcessedSinceLastUpdated=0;
                task.setProgress(counters[3],counters[4]);
            }         
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                            
            Thread.yield();
        } // end: for each motif          
        targetSequence.updateRegionSortOrder(); // this must be called since regions were added unsorted       
    }    
    
    
    /** @return Either HashSet<String> or HashMap<Integer,HashSet<String>> */
    private Object processSingleMotif(Motif motif, String propertyName, boolean allowVariableLengths) throws ExecutionError {
        Object siteProperty=motif.getPropertyValue(propertyName, engine);
        if (siteProperty instanceof String) {            
            String text=(String)siteProperty;
            if (text.isEmpty()) throw new ExecutionError(motif.getName()+" does not have any annotated binding sequences in property '"+propertyName+"'");
            String[] list=null;         
            if (text.contains(",")) list=text.split(",");
            else if (text.contains(" ") || text.contains("\n") || text.contains("\t")) list=text.split("\\s+");
            else list=new String[]{text}; //
            return indexStrings(list, motif.getName(), allowVariableLengths);
        } else if (siteProperty instanceof ArrayList) {
            return indexStrings((ArrayList<String>)siteProperty, motif.getName(), allowVariableLengths);
        } else throw new ExecutionError("Motif property '"+propertyName+"' does not have a text value for motif "+motif.getName());         
    }
    
    /** @return Either HashSet<String> or HashMap<Integer,HashSet<String>> */    
    private Object indexStrings(ArrayList<String> list, String motifname, boolean allowVariableLengths) throws ExecutionError {
        if (list==null || list.isEmpty()) return null;
        if (allowVariableLengths) {
            HashMap<Integer,HashSet<String>> stringset=new HashMap<>();        
            for (String string:list) {
                string=string.trim();     
                if (!stringset.containsKey(string.length())) stringset.put(string.length(), new HashSet<String>());
                HashSet<String> stringsubset=stringset.get(string.length());
                String reverse=MotifLabEngine.reverseSequence(string);
                if (!(stringsubset.contains(string) || stringsubset.contains(reverse))) stringsubset.add(string); // only add one orientation to save memory. We will check both later anyway
            }
            if (stringset.isEmpty()) return null;
            return stringset;        
        } else {
            HashSet<String> stringset=new HashSet<>();        
            int expectedLength=list.get(0).trim().length();
            for (String string:list) {
                string=string.trim();            
                if (string.length()!=expectedLength) throw new ExecutionError("The binding sequences for '"+motifname+"' do not have the same length");
                String reverse=MotifLabEngine.reverseSequence(string);
                if (!(stringset.contains(string) || stringset.contains(reverse))) stringset.add(string); // only add one orientation to save memory. We will check both later anyway
            }
            if (stringset.isEmpty()) return null;
            return stringset;
        }
    }
    
    /** @return Either HashSet<String> or HashMap<Integer,HashSet<String>> */    
    private Object indexStrings(String[] list, String motifname, boolean allowVariableLengths) throws ExecutionError {
        if (list==null || list.length==0) return null;
        if (allowVariableLengths) {
            HashMap<Integer,HashSet<String>> stringset=new HashMap<>();        
            for (String string:list) {
                string=string.trim();     
                if (!stringset.containsKey(string.length())) stringset.put(string.length(), new HashSet<String>());
                HashSet<String> stringsubset=stringset.get(string.length());
                String reverse=MotifLabEngine.reverseSequence(string);
                if (!(stringsubset.contains(string) || stringsubset.contains(reverse))) stringsubset.add(string); // only add one orientation to save memory. We will check both later anyway
            }
            if (stringset.isEmpty()) return null;
            return stringset;        
        } else {
            HashSet<String> stringset=new HashSet<>();        
            int expectedLength=list[0].trim().length();
            for (String string:list) {
                string=string.trim();            
                if (string.length()!=expectedLength) throw new ExecutionError("The binding sequences for '"+motifname+"' do not have the same length");
                String reverse=MotifLabEngine.reverseSequence(string);
                if (!(stringset.contains(string) || stringset.contains(reverse))) stringset.add(string); // only add one orientation to save memory. We will check both later anyway
            }
            if (stringset.isEmpty()) return null;
            return stringset;
        }
    }    
    
      
    private class ScanSequenceTask<RegionSequenceData> implements Callable<RegionSequenceData> {
        final RegionDataset targetDataset;
        final DNASequenceDataset sourceDataset;
        final long[] counters; // NB: this array will be shared with other tasks since all tasks are given the same pointer
        final String sequencename;
        final OperationTask task;
        final Object motifs;
        final boolean allowVariableLengths;

        
        public ScanSequenceTask(DNASequenceDataset sourceDataset, RegionDataset targetDataset, String sequencename, HashMap<String,Object> motifs, boolean allowVariableLengths, OperationTask task, long[] counters) {
           this.counters=counters;
           this.sequencename=sequencename;
           this.sourceDataset=sourceDataset;
           this.targetDataset=targetDataset;    
           this.motifs=motifs;
           this.allowVariableLengths=allowVariableLengths;
           this.task=task;     
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public RegionSequenceData call() throws Exception {
            synchronized(counters) {
                counters[0]++; // number of sequences started
            }        
            task.checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            DNASequenceData sourceSequence=(DNASequenceData)sourceDataset.getSequenceByName(sequencename);
            org.motiflab.engine.data.RegionSequenceData targetSequence=(org.motiflab.engine.data.RegionSequenceData)targetDataset.getSequenceByName(sequencename);
            if (allowVariableLengths) searchInSequenceVariable(sourceSequence,targetSequence, (HashMap<String,HashMap<Integer,HashSet<String>>>)motifs, task, counters);
            else searchInSequence(sourceSequence,targetSequence, (HashMap<String,HashSet<String>>)motifs, task, counters);
            synchronized(counters) { // finished one of the sequences
                counters[1]++; // number of sequences completed
                task.setStatusMessage("Executing StringScanner:  ("+counters[1]+"/"+counters[2]+")");
            }   
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();           
            return (RegionSequenceData)targetSequence;
        }   
    }    
  
}