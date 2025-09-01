/*
 
 
 */

package org.motiflab.engine.data.classifier.neuralnet;

/**
 *
 * @author Kjetil
 */
public class InputNode extends Node implements java.io.Serializable {

  private double value;

  /** Creates new InputNode */
  public InputNode(NodeID id) {
    super(id);
  }


  public void setValue(double v) {
    value=v;
  }

  public double getOutput() {
     return value;
  }

  public boolean isErrorValid() {return true;}
  public double getErrorTerm() {return 0;}

}