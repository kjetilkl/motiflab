/*
 
 
 */

package org.motiflab.engine.data;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public class DNASequenceDataset extends FeatureDataset {
    private static String typedescription = "DNA Sequence Dataset";
    
    public DNASequenceDataset(String datasetName) {
        super(datasetName);
    }
   
    @Override
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        //notifyListenersOfDataUpdate(); 
    }
    
    /** 
     * Returns a deep copy of this object 
     */
    @Override
    public DNASequenceDataset clone() {
        DNASequenceDataset newdataset=new DNASequenceDataset(datasetName); // creates a new dataset with the same name and empty storage
        newdataset.datasetDescription=this.datasetDescription;
        int size=getNumberofSequences();
        for (int i=0;i<size;i++) {
            DNASequenceData seq=(DNASequenceData)getSequenceByIndex(i);
            if (seq==null) throw new ConcurrentModificationException("Sequence was deleted before the dataset could be cloned"); // not sure what else to do
            newdataset.addSequence((DNASequenceData)seq.clone());
        }
        return newdataset;
    }  
    
    @Override
    public void setupDefaultDataset(ArrayList<Sequence> sequences) {
        for (Sequence seq:sequences) {
            if (getSequenceByName(seq.getName())==null) {            
                DNASequenceData seqdata=new DNASequenceData(seq,'N');
                addSequence(seqdata);
            }
        }
    }
             

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof DNASequenceDataset)) return false;
        DNASequenceDataset other=(DNASequenceDataset)data;
        for (FeatureSequenceData sequence:getAllSequences()) {
            if (!sequence.containsSameData(other.getSequenceByName(sequence.getName()))) return false;
        }
        return false;
    }

    
    public static DNASequenceDataset createDNASequenceDatasetFromParameter(String parameter, String targetName, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        DNASequenceDataset newDataItem=new DNASequenceDataset(targetName);
        Object defaultvalue=new Character('N');
        if (parameter!=null && !parameter.isEmpty()) {
            if (parameter.startsWith("'")) {
                if (parameter.length()!=3 || !parameter.endsWith("'")) throw new ExecutionError("Unable to parse default base:"+parameter);
                else defaultvalue=parameter.charAt(1);
            }
            else { // the parameter should contain the name of an existing background model
                defaultvalue=engine.getDataItem(parameter);
                if (defaultvalue==null) throw new ExecutionError("No such data object: "+parameter);
                if (!(defaultvalue instanceof BackgroundModel)) throw new ExecutionError(parameter+" is not a Background model");                    
            }
        }
        SequenceCollection allSequences = engine.getDefaultSequenceCollection();
        ArrayList<Sequence> seqlist=allSequences.getAllSequences(engine);
        int size=seqlist.size();
        int i=0;
        for (Sequence sequence:seqlist) {
            i++;
            DNASequenceData seq=null;
            if (defaultvalue instanceof BackgroundModel) seq=new DNASequenceData(sequence.getName(), sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(), (BackgroundModel)defaultvalue);
            else seq=new DNASequenceData(sequence.getName(), sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(),(Character)defaultvalue);
            // set some default values also...
            ((DNASequenceDataset)newDataItem).addSequence(seq);
            if (task!=null) {
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
                task.setProgress(i, size);
            }            
        }   
        return newDataItem;
    }
    
    
    @Override
    public String[] getResultVariables() {
        return new String[]{"Direct-0","Direct-1","Direct-2","Reverse-0","Reverse-1","Reverse-2"};
    }
       
    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return RegionDataset.class; // all current exports are region datasets
    }   
    
    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (!hasResult(variablename)) throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        RegionDataset newdata=new RegionDataset("Result");
        for (FeatureSequenceData seq:getAllSequences()) {
            RegionSequenceData codonsequence=((DNASequenceData)seq).getCodonSequence(variablename);
            newdata.addSequence(codonsequence);
        }
        newdata.updateMaxScoreValueFromData();
        newdata.setMotifTrack(false);
        newdata.setModuleTrack(false);
        newdata.setNestedTrack(false);
        newdata.displayDirectives="$multicolor(?)=true;$gradient(?)=off;$setting(?.showTypeLabel)=true;$setting(?.typeLabelColor)=0;";
        return newdata;              
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
