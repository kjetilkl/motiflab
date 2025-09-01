/*
 
 
 */

package org.motiflab.engine.data.classifier.neuralnet;

/**
 *
 * @author Kjetil
 */
public class ThresholdNode extends Node implements java.io.Serializable {

  /** Creates new ThresholdNode */
  public ThresholdNode(NodeID id) {
        super(id);
  }

  public double getOutput() {return 1;}

  public boolean isErrorValid() {return true;}
  public double getErrorTerm() {return 0;}

}