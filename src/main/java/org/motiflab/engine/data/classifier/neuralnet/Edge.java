/*
 
 
 */

package org.motiflab.engine.data.classifier.neuralnet;

/**
 *
 * @author Kjetil
 */
public class Edge implements java.io.Serializable {

  private double weight=0;
  private double lastupdate=0;
  private Node source, destination;

  /** Creates new Edge */
  public Edge(Node source, Node destination) {
    this.source=source;
    this.destination=destination;
  }


  public double getWeight() {
     return weight;
  }

  public void setWeight(double d) {
    lastupdate=d-weight;
    weight=d;
  }

  public double getLastUpdate() {
     return lastupdate;
  }

  public Node getSource() {return source;}
  public Node getDestination() {return destination;}
}