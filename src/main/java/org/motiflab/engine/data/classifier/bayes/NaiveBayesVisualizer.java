/*
 
 
 */

package org.motiflab.engine.data.classifier.bayes;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import org.motiflab.engine.data.classifier.ClassifierOutput;
import org.motiflab.engine.data.classifier.ClassifierVisualizer;
import org.motiflab.engine.data.classifier.ClassifierVisualizerListener;

/**
 *
 * @author kjetikl
 */
public class NaiveBayesVisualizer extends ClassifierVisualizer implements ClassifierVisualizerListener {
    private NaiveBayesClassifier classifier=null;


   /**
    * Creates a new AdaBoostVisualizer
    */
    public NaiveBayesVisualizer(NaiveBayesClassifier classifier) {
       super();
       this.classifier=classifier;
       this.setLayout(new BorderLayout());
       javax.swing.JPanel innerPanel=new javax.swing.JPanel(new java.awt.FlowLayout());
       innerPanel.add(new JLabel("No visualizer available"));
       this.add(innerPanel);
    }

    @Override
    public void finished(ClassifierOutput info) { }

    @Override
    public void round(ClassifierOutput info) { }

    @Override
    public void start(ClassifierOutput info) { }

    @Override
    public void setSelectedFeature(int featureNumber) {
        
    }


    @Override
    public void featureSelectedInVisualizer(int featureNumber) {
        notifyListenersOfFeatureSelected(featureNumber);
    }


}
