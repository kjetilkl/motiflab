/*
 
 
 */

package motiflab.engine.data.classifier;

import java.util.ArrayList;
import javax.swing.JPanel;

/**
 *
 * @author kjetikl
 */
public abstract class ClassifierVisualizer extends JPanel implements ClassifierOutputListener {
    private ArrayList<ClassifierVisualizerListener> listeners=new ArrayList<ClassifierVisualizerListener>();

    /**
     * Adds a new listener to this ClassifierVisualizer
     * @param listener
     */
    public void addListener(ClassifierVisualizerListener listener) {
        listeners.add(listener);
    }

     /**
     * Removes a listener from this ClassifierVisualizer
     * @param listener
     */
    public void removeListener(ClassifierVisualizerListener listener) {
        listeners.remove(listener);
    }


    public void notifyListenersOfFeatureSelected(int featureNumber) {
        for (ClassifierVisualizerListener listener:listeners) listener.featureSelectedInVisualizer(featureNumber);
    }

    /**
     *  This method can be used to notify the ClassifierVisualizer that
     *  it should visualize the given feature as "selected" (if the visualizer supports this behaviour)
     *  If the featureNumber is negative it means that no particular feature should be "selected"
     */
    public abstract void setSelectedFeature(int featureNumber);

}
