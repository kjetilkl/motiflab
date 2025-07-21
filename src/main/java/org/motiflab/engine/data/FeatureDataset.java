/*
 
 
 */

package org.motiflab.engine.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


/**
 * FeatureDataset represents a collection of individual FeatureSequenceData objects.
 * 
 * @author kjetikl
 */
public abstract class FeatureDataset extends Data {
    protected String datasetName;
    protected String datasetDescription;
    protected ArrayList<FeatureSequenceData> storage; // used to store the child sequences
    protected boolean isderived=false;
    protected String dataSourceName="unknown_source";
    
    public transient String displayDirectives=null; // if this is set, MotifLab will execute these display settings (and then remove them) when the data item is added to the engine 
    
    /**
     * Constructs a new initially "empty" instance of FeatureDataset
     * with the specified name 
     * 
     * @param datasetName A name for this dataset
     */
    public FeatureDataset(String datasetName) {
       this.datasetName=datasetName;
       storage=new ArrayList<FeatureSequenceData>(20);
    }
   
    /**
     * After an empty FeatureDataset has been created (with no sequences), 
     * this method can be used to add sequences to it (with default content according to type) 
     * The supplied list of sequences should represent all the sequences that the FeatureDataset should
     * contain, and any sequence in this list that is not currently present will be created and
     * added to the dataset
     * @param sequences 
     */
    public abstract void setupDefaultDataset(ArrayList<Sequence> sequences);
    
    
    /**
     * Specifies a new name for this dataset
     * 
     * @param name the name for this dataset
     */
    public void setName(String name) {
        this.datasetName=name;
    }
    
    @Override
    public String getName() {
        return datasetName;
    }
    
    @Override
    public void rename(String name) {
        setName(name);
    }
    
    @Override
    public Object getValue() {return this;} // should maybe change later
    
    @Override
    public String getValueAsParameterString() {return "N/A";}
    
   /**
    * Sets a textual description for this dataset
    * 
    * @param description a short description
    */
    public void setDescription(String description) {
        this.datasetDescription=description;
    }
    
   /**
    * Returns a textual description for this dataset
    * 
    * @return description
    */
    public String getDescription() {
        return datasetDescription;
    }

    

    
    /**
     * Returns all the FeatureSequenceData objects in this dataset
     * 
     * @return A list of FeatureSequenceData objects
     */
    public ArrayList<FeatureSequenceData> getAllSequences() {
        return storage;
    }    
    
     /**
     * Returns all the FeatureSequenceData objects in this dataset
     * that are part of the provided collection
     * @return A list of FeatureSequenceData objects
     */
    public ArrayList<FeatureSequenceData> getSequencesFromCollection(SequenceCollection collection) {
        ArrayList<FeatureSequenceData> result=new ArrayList<FeatureSequenceData>(storage.size());
        if (collection==null) return result;
        for (FeatureSequenceData sequence : storage) {
            if (collection.contains(sequence.getName())) result.add(sequence);
        }
        return result;
    }      
               
    /**
     * Returns the FeatureSequenceData corresponding to the given name.
     * 
     * @param name The name of the sequence
     * @return the specified sequence (if found) or null
     */
    public FeatureSequenceData getSequenceByName(String name) {
        for (FeatureSequenceData sequence : storage) {
            if (sequence.getName().equals(name)) return sequence;
        }
        return null;
    }
    
    /**
     * Returns the FeatureSequenceData corresponding to the given index.
     * This method could be used if you want to iterate through all sequences
     * in a dataset. Note, however, that the order of sequences in one FeatureDataset
     * need not be the same as the order of (the same) sequences in another FeatureDataset
     * (i.e. three sequences Seq1,Seq2 and Seq3 might not have the same order within
     * the two FeatureDatasets "Repeats" and "Conservation").
     * 
     * @param index The index of the sequence
     * @return the specified sequence (if exists) or null
     */
    public FeatureSequenceData getSequenceByIndex(int index) {
        return storage.get(index);
    }
    
    /**
     * Returns the index (order within the dataset) of the sequence with the given name 
     * If no sequence with the given name exists within the dataset the value -1 is returned.
     * Note that the order of sequences in one FeatureDataset
     * need not be the same as the order of (the same) sequences in another FeatureDataset
     * (i.e. three sequences Seq1,Seq2 and Seq3 might not have the same order within
     * the two FeatureDatasets "Repeats" and "Conservation").
     * @param name The name of the sequence
     * @return index of the sequence (between 0 and size-1) or -1
     */
    public int getIndexForSequence(String name) {
        for (int i=0;i<storage.size();i++) {
            FeatureSequenceData data=storage.get(i);
            if (data.getName().equals(name)) return i;
        }
        return -1;
    }
    
    
    
    
   /**
     * Adds a new FeatureSequenceData object to the dataset.
     * 
     * @param sequence The FeatureSequenceData to be added
     */
    public void addSequence(FeatureSequenceData sequence) {
        storage.add(sequence);
        sequence.setParent(this);
        notifyListenersOfDataAddition(sequence);
    }
    
   /**
     * Removes a FeatureSequenceData from this dataset.
     * 
     * @param sequence The name of the sequence to be removed
     */
    public void removeSequence(String sequenceName) {
        FeatureSequenceData target=null;
        for (FeatureSequenceData seq:storage) {
            if (seq.getName().equals(sequenceName)) {target=seq;break;}
        }
        if (target!=null) {;
            storage.remove(target);
            target.setParent(null);
            notifyListenersOfDataRemoval(target);            
        }
    }    
    
   /**
     * Replaces a FeatureSequenceData object in this dataset.
     * The old sequence (with the same name as the argument)
     * is not replaced per se. Rather, the data from the new sequence
     * is imported into the old sequence
     * 
     * @param sequence The FeatureSequenceData to be changed
     */
    public void replaceSequence(FeatureSequenceData sequence) {
        int index=-1;
        for (int i=0;i<storage.size();i++) {
            if (storage.get(i).getName().equals(sequence.getName())) {index=i;break;}
        }
        if (index<0) return;
        FeatureSequenceData oldsequence=storage.get(index);
        oldsequence.importData(sequence);
        notifyListenersOfDataUpdate();
    }
    
    /**
     * Reorders the list of sequences in the dataset by moving the sequence
     * at the specified current position to the new position.
     * 
     * @param oldposition Position of the sequence to be moved
     * @param newposition New position to move sequence to
     */
    public void reorderSequences(int currentposition, int newposition) {
        FeatureSequenceData temp=storage.remove(currentposition);
        storage.add(newposition,temp);
        notifyListenersOfDataUpdate();
    }   
    
    /**
     * Returns the number of sequences in this dataset
     * 
     * @return number of sequences
     */
    public int getNumberofSequences() {
        return storage.size();
    }
    
    /**
     * Returns the number of sequences in this dataset (same as getNumberofSequence)
     * 
     * @return number of sequences
     */
    public int getSize() {
        return storage.size();
    }
    
    /** Returns a string describing the origin of this dataset (for instance a database or prediction program) */
    public String getDataSource() {
        return dataSourceName;
    }
    
    /** Sets a data source for this dataset (this could be a database or prediction program) 
     * The name should not contain whitespace
     */
    public void setDataSource(String sourcename) {
        dataSourceName=sourcename;  
    }

        
    /**
     * This function should return <tt>true</tt> if this dataset has been
     * derived from other datasets through the use of operations rather than 
     * being an original dataset obtained from some data repository (db or file)
     */
    public boolean isDerived() {
        return isderived;
    }
    
    /**
     * This function can be used to specify whether this dataset has been derived
     * from other datasets through operations or whether it has been obtained 
     * from some data repository (database or file)
     * @param isderived Should be <tt>true</tt> if the dataset has been derived from others
     */
    public void setIsDerived(boolean isderived) {
        this.isderived=isderived;
    }
    
    /**
     * This method takes all sequences in the specified dataset and adds them
     * to this dataset
     * @param dataset The dataset to be incorporated into this dataset
     */
    public void merge(FeatureDataset dataset) {
        int size=dataset.getNumberofSequences();
        for (int i=0;i<size;i++) {
            FeatureSequenceData data=dataset.getSequenceByIndex(i);
            storage.add(data); // I use this directly instead of calling addSequence() to limit the number of notifications sent 
        }              
        notifyListenersOfDataAddition(dataset);
    }
    
    @Override
    public void importData(Data source) throws ClassCastException {
        FeatureDataset datasource=(FeatureDataset)source;
        this.datasetName=datasource.datasetName;
        this.datasetDescription=datasource.datasetDescription;
        this.isderived=datasource.isderived;
        // propagate import to children. Remove sequences no longer in source or add sequences that are new in source
        HashSet<String> toBeRemoved=new HashSet<String>();
        for (FeatureSequenceData seq:storage) {
            toBeRemoved.add(seq.getSequenceName()); // add all to begin with then remove those that are in the source's storage
        }
        for (FeatureSequenceData seqsource:datasource.storage) { // go through sequences in source
            toBeRemoved.remove(seqsource.getSequenceName());
            FeatureSequenceData seqthis=getSequenceByName(seqsource.getSequenceName());
            if (seqthis!=null) seqthis.importData(seqsource);
            else { // sequence exists in source but not here. It has probably been added in the source. Clone it here
                FeatureSequenceData copy=(FeatureSequenceData)seqsource.clone();        
                addSequence(copy);
            }
        }
        // now remove missing sequences
        for (String seqName:toBeRemoved) {
            removeSequence(seqName);
        }
    }
    
    /** Outputs information about this dataset into the buffer */
    public StringBuilder debug(StringBuilder builder) {
        if (builder==null) builder=new StringBuilder();
        builder.append("Track["+getName()+"] "+getSize()+" sequences   ["+System.identityHashCode(this)+"]\n");
        if (storage!=null) {
            for (FeatureSequenceData seq:storage) {
                seq.debug(builder);
                builder.append("\n");
            }
        }
        return builder;
    }
    
    private static final long serialVersionUID = -5743509433028154684L;
}
