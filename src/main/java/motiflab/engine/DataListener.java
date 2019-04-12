/*
 
 
 */

package motiflab.engine;
import motiflab.engine.data.Data;

/**
 * This interface specifies callback methods that are called either by the engine
 * or by Data objects themselves to notify of changes in data
 *  
 * @author kjetikl
 */
public interface DataListener {
    
    /**
     * Notifies interested listeners that a new Data item has been added
     * This method is called whenever a new Data item is stored in the Engine,
     * @param data The data item that has been added 
     */
    public void dataAdded(Data data);
    
    /**
     * Notifies interested listeners that a new Data item has been added
     * to a dataset object which is already stored in the engine 
     * Note that the child data item can be a dataset of its own (and not just an
     * "atomic" data item). If this is the case it usually means that the child 
     * dataset has been merged into the parent dataset 
     * @param parentDataset The dataset which receives the data item
     * @param child The data item which is added to the parent dataset
     *              this can be an "atomic" data item or a dataset (in which case
     *              the event usually signals a merger between parent and child)
     */
    public void dataAddedToSet(Data parentDataset, Data child);
    
    /**
     * Notifies interested listeners that a Data item has been removed
     * This method is called whenever a Data item is removed in full from storage
     * @param data The data item that has been removed 
     */
    public void dataRemoved(Data data);
    
    /**
     * Notifies interested listeners that a Data item has been removed from 
     * a dataset object (which is still present in the engine)
     * @param parentDataset The dataset from which a data item is removed
     * @param child The data item that has been removed from the parent dataset
     *              this can be an "atomic" data item or a dataset (in which case
     *              the event usually signals a "set subtraction").
     */
    public void dataRemovedFromSet(Data parentDataset,Data child);
    
    /**
     * Notifies interested listeners that an "atomic" data item has been updated
     * (if data items are added or removed from a "dataset" object the methods
     * dataAddedToSet or dataRemovedFromSet will be used instead to notify about
     * such events).
     * @param data The data item that has been updated
     */
    public void dataUpdated(Data data);
    
    /**
     * Notifies interested listeners that an update is about to happen on a data
     * object which is already registered with the engine. This method is called
     * just before an update happens and gives listeners a chance to react before
     * the old data is overwritten. This method will only be invoked in response
     * to a call to MotifLabEngine.updateDataItem(Data newdata) when there is
     * already an existing Data item with the same name. All relevant values
     * from the new Data item will then be imported into the old Data item to
     * update the former data item. Note that none of the arguments are allowed 
     * to be null
     * 
     * @param oldvalue The data item that has been updated
     * @param newvalue The data item that has been updated
     */
    public void dataUpdate(Data oldvalue, Data newvalue);
    
}
