/*
 
 
 */

package org.motiflab.engine.data.classifier.neuralnet;

/**
 *
 * @author Kjetil
 */
public class LayeredNodeID extends NodeID implements java.io.Serializable {
   private int layer;
   private int number;

  /** Creates new LayeredNodeID */
  public LayeredNodeID(int layer, int number) {
     this.layer=layer;
     this.number=number;
  }

  public int getLayer() {return layer;}
  public int getNumber() {return number;}

  public String toString() {
     return layer+","+number;
  }
}