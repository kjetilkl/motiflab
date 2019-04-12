/*
 
 
 */

package motiflab.gui;

import java.io.Serializable;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import motiflab.engine.MotifLabEngine;
import javax.swing.undo.UndoableEdit;
import motiflab.engine.data.*;

/**
 * PartialUndo represents an UndoableEdit where a small part of a larger dataobject has been changed.
 * For instance, a single sequence in a NumericDataset has been updated
 * by the Draw tool or a single Region in a RegionDataset has been changed
 *
 * @author kjetikl
 */
public class PartialUndo implements UndoableEdit {
    private static final int FEATURE_SEQUENCE=0;
    private static final int REGION=1;

    private GUIUndoManager undoManager=null; // provides functionality for caching UNDO-data

    private MotifLabEngine engine=null;
    private String originalDataUndoTicket=null; // stores UNDO information
    private String finalDataUndoTicket=null;  // stores REDO information
    private boolean isDone=false;
    private String presentationName;
    private String parentFeaturesetName=null;
    private String sequenceName=null;
    private int type=-1;




    /**
     * Creates a new PartialUndo
     * @param parent A reference to the parent dataobject. This should be a FeatureDataset if oldvalue/newvalue are FeatureSequenceData and a RegionSequenceData if oldvalue/newvalue is a Region
     * @param oldvalue This should be an unregistered clone of a FeatureSequenceData object or a Region (or null if Region is added)
     * @param newvalue This should be an unregistered clone of a FeatureSequenceData object or a Region (or null if Region is removed)
     */
    public PartialUndo(GUIUndoManager undoManager, String presentationName, String featurename, String sequencename, Serializable oldvalue, Serializable newvalue) {
        this.undoManager=undoManager;
        this.presentationName=presentationName;
        this.engine=undoManager.getEngine();
        this.parentFeaturesetName=featurename;
        this.sequenceName=sequencename;
        if (oldvalue instanceof FeatureSequenceData || newvalue instanceof FeatureSequenceData) type=FEATURE_SEQUENCE;
        else if (oldvalue instanceof Region || newvalue instanceof Region) type=REGION;

        if (oldvalue instanceof FeatureSequenceData) ((FeatureSequenceData)oldvalue).setParent(null); // remove reference to parent to avoid serializing this also
        if (newvalue instanceof FeatureSequenceData) ((FeatureSequenceData)newvalue).setParent(null); // remove reference to parent to avoid serializing this also
        if (oldvalue instanceof Region) ((Region)oldvalue).setParent(null); // remove reference to parent to avoid serializing this also
        if (newvalue instanceof Region) ((Region)newvalue).setParent(null); // remove reference to parent to avoid serializing this also
        originalDataUndoTicket=(oldvalue!=null)?undoManager.getUniqueUndoID():null;
        finalDataUndoTicket=(newvalue!=null)?undoManager.getUniqueUndoID():null;
        if (originalDataUndoTicket!=null) undoManager.storeObjectInCache(originalDataUndoTicket, oldvalue);
        if (finalDataUndoTicket!=null) undoManager.storeObjectInCache(finalDataUndoTicket, newvalue);
        isDone=true;
    }


    /**
     */
    public void rollback() {
        Object originalData=null;
        if (originalDataUndoTicket!=null) originalData=(Object)undoManager.getObjectFromCache(originalDataUndoTicket);
        FeatureDataset dataset=(FeatureDataset)engine.getDataItem(parentFeaturesetName);
        dataset=(FeatureDataset)dataset.clone();
        if (type==FEATURE_SEQUENCE && originalData!=null) {
            dataset.replaceSequence((FeatureSequenceData)originalData); //
        } else if (type==REGION) {
            RegionSequenceData sequencedata=(RegionSequenceData)dataset.getSequenceByName(sequenceName);
            Object finalData=null;
            if (finalDataUndoTicket!=null) finalData=(Object)undoManager.getObjectFromCache(finalDataUndoTicket);
            if (finalData!=null) sequencedata.removeSimilarRegion((Region)finalData);           
            if (originalData!=null) sequencedata.addRegion((Region)originalData);            
        } else engine.logMessage("SYSTEM ERROR: Something went wrong in PartialUndo during rollback");
        if (dataset instanceof NumericDataset) ((NumericDataset)dataset).updateAllowedMinMaxValuesFromData();
        else if (dataset instanceof RegionDataset) ((RegionDataset)dataset).updateMaxScoreValueFromData();
        try {
            engine.updateDataItem(dataset);
        } catch (Exception e) {
            engine.errorMessage("WARNING: an error occurred on rollback: "+e.toString(), 0);
        }
    }

   /**
     * Redos changes that has been undone by reestablishing the state of data as it
     */
    private void rollforward() {
        Object newData=null;
        if (finalDataUndoTicket!=null) newData=(Object)undoManager.getObjectFromCache(finalDataUndoTicket);
        FeatureDataset dataset=(FeatureDataset)engine.getDataItem(parentFeaturesetName);
        dataset=(FeatureDataset)dataset.clone();
        if (type==FEATURE_SEQUENCE && newData!=null) {
            dataset.replaceSequence((FeatureSequenceData)newData); //
        } else if (type==REGION) {
            RegionSequenceData sequencedata=(RegionSequenceData)dataset.getSequenceByName(sequenceName);
            Object originalData=null;
            if (originalDataUndoTicket!=null) originalData=(Object)undoManager.getObjectFromCache(originalDataUndoTicket);
            if (originalData!=null) sequencedata.removeSimilarRegion((Region)originalData);
            if (newData!=null) sequencedata.addRegion((Region)newData);
        } else engine.logMessage("SYSTEM ERROR: Something went wrong in PartialUndo during rollforward");
        if (dataset instanceof NumericDataset) ((NumericDataset)dataset).updateAllowedMinMaxValuesFromData();
        else if (dataset instanceof RegionDataset) ((RegionDataset)dataset).updateMaxScoreValueFromData();
        try {
           //engine.logMessage("RB: Recover " +item.getName()+"   type="+item.getTypeDescription());
           engine.updateDataItem(dataset);
        } catch (Exception e) {
            engine.errorMessage("WARNING: an error occurred on rollback: "+e.toString(), 0);
        }
    }







    @Override
    public boolean addEdit(UndoableEdit anEdit) { return false; }
    @Override
    public boolean replaceEdit(UndoableEdit anEdit) {return false;}

    @Override
    public boolean canRedo() {
        return (!isDone && type>=0);
    }

    @Override
    public boolean canUndo() {
        return (type>=0); // correct data registered
    }


    @Override
    public String getPresentationName() {
        return presentationName;
    }

    @Override
    public String getRedoPresentationName() {
        return "Redo "+presentationName;
    }

    @Override
    public String getUndoPresentationName() {
        return "Undo "+presentationName;
    }

    @Override
    public boolean isSignificant() { return true; }


    @Override
    public void undo() throws CannotUndoException {
       if (type<0) throw new CannotUndoException();
       else {
          rollback();
          isDone=false;
       }
       
    }

    @Override
    public void redo() throws CannotRedoException {
       if (isDone || type<0) throw new CannotRedoException();
       else { //engine.logMessage("Performing rollforward");
          rollforward();
          isDone=true;
       }
    }



    @Override
    public void die() {
        //
    }

 }
