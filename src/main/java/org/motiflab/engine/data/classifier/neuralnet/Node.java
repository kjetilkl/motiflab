/*
 
 
 */

package org.motiflab.engine.data.classifier.neuralnet;

import java.util.ArrayList;

/**
 *
 * @author Kjetil
 */
public class Node implements java.io.Serializable {

  private ArrayList<Edge> inputedges;
  private ArrayList<Edge> outputedges;
  private double xcoordinate, ycoordinate; // for graphical representations
  private boolean valid=false;
  private boolean errorvalid=false;
  private double output;
  private double errorterm;
  private NodeID id;

  /** Creates new Node */
  public Node(NodeID id) {
    this.id=id;
    inputedges=new ArrayList<Edge>(10);
    outputedges=new ArrayList<Edge>(10);
  }

  public double getX() {return xcoordinate;}
  public double getY() {return ycoordinate;}
  public void setX(double x) {xcoordinate=x;}
  public void setY(double y) {ycoordinate=y;}

  public double getOutput() {
    if (valid) return output;

    double net=0;
    for (int i=0;i<inputedges.size();i++) {
       Edge e=(Edge)inputedges.get(i);
       net+=e.getSource().getOutput()*e.getWeight();
    }
    output=1/(1+Math.exp(-net)); // sigmoid output
    valid=true;
    return output;
  }

  public void invalidate() {valid=false;}
  public void invalidateError() {errorvalid=false;}

  public NodeID getID() {return id;}

  public void addInputEdge(Edge e)     {inputedges.add(e);}
  public void removeInputEdge(Edge e)  {inputedges.remove(e);}
  public void addOutputEdge(Edge e)   {outputedges.add(e);}
  public void removeOutputEdge(Edge e) {outputedges.remove(e);}
  public ArrayList<Edge> getInputEdges()  {return inputedges;}
  public ArrayList<Edge> getOutputEdges() {return outputedges;}

  public double getErrorTerm() {
    if (errorvalid) return errorterm;
    double propagatederrorsum=0;
    for (int i=0;i<outputedges.size();i++) {
       Edge e=(Edge)outputedges.get(i); Node destination=e.getDestination();
       propagatederrorsum+=e.getWeight()*destination.getErrorTerm();
    }
    errorterm=getOutput()*(1-getOutput())*propagatederrorsum;
    errorvalid=true;
    return errorterm;
  }

  public void setErrorTerm(double e) {errorterm=e; errorvalid=true;}
  public boolean isErrorValid() {return errorvalid;}

}