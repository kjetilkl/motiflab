/*
 
 
 */

package motiflab.engine.data.classifier;

/**
 *
 * @author kjetikl
 */
public interface ClassifierVisualizerListener {

    /**
     * This is a callback notification that a feature has been selected in the visualizer
     * If a negative number is returned, it means that any previous features have been
     * deselected (and no feature is currently selected)
     * @param featureNumber
     */
    public void featureSelectedInVisualizer(int featureNumber);
    
}
