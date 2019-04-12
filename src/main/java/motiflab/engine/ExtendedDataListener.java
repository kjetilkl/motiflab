
package motiflab.engine;

import motiflab.engine.data.Data;

/**
 *
 * @author kjetikl
 */
public interface ExtendedDataListener extends DataListener {
    
    /**
     * Notifies interested listeners that an update has occurred on 
     * the data object that has resulted in a change in order among its children
     * The data object would typically be a sequence collection  
     * 
     * @param Data the affected data item
     * @param oldPosition If a single element has been moved, this parameter will hold the original position. If multiple elements have been moved, this value should be NULL
     * @param newPosition If a single element has been moved, this parameter will hold the new position. If multiple elements have been moved, this value should be NULL     * 
     */
    public void dataOrderChanged(Data data, Integer oldposition, Integer newposition);    
    
}
