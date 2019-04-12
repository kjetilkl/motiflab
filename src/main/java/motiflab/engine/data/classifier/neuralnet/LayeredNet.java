/*
 
 
 */

package motiflab.engine.data.classifier.neuralnet;

import java.util.ArrayList;

/**
 *
 * @author Kjetil
 */
public class LayeredNet extends Topology implements java.io.Serializable {

  private int hidden_layers=0;
  private ArrayList<ArrayList<Node>> layers; // each inner list represents a layer holding the nodes for that layer
  private ArrayList<ArrayList<Node>> thresholdlayers; // holds the threshold nodes corresponding to each non-input layer
  private int[] layersizes; // specifies the number of nodes in each layer

  /** Creates new LayeredNet */
  public LayeredNet(NeuralNet net, int[] layersizes) {
     super(net);
     this.layersizes=layersizes;
     //System.err.println("Creating Layered network with "+layersizes.length+" layers");
     network.clear();
     int size=layersizes.length;
     hidden_layers=size-2;
     layers=new ArrayList<ArrayList<Node>>(size);
     thresholdlayers=new ArrayList<ArrayList<Node>>(size);
     for (int i=0;i<size;i++) {
        layers.add(i,new ArrayList<Node>(layersizes[i]));
        if (i>0) thresholdlayers.add(i,new ArrayList<Node>(layersizes[i])); // layer 0 is the input layer. These do not have threshold nodes, but the other layers have
        else thresholdlayers.add(i,null);
     }
     // add input nodes
     ArrayList<Node> inputlayer=layers.get(0); // layer 0 is the input layer
     for (int n=0;n<layersizes[0];n++) {
        Node node=new InputNode(new LayeredNodeID(0,n));
        inputlayer.add(node);
        network.addInputNode(node); // second view
     }
     // add nodes to rest of network
     for (int i=1;i<size;i++) {
       for (int n=0;n<layersizes[i];n++) {
         Node node=new Node(new LayeredNodeID(i,n));
         ThresholdNode t_node=new ThresholdNode(new LayeredNodeID(i,n));
         connect(t_node,node); // tie in a thresholdvalue as w0
         layers.get(i).add(node);
         thresholdlayers.get(i).add(t_node);
         network.addNode(t_node);
         if (i==size-1) network.addOutputNode(node);
         else network.addNode(node);
       }
     }
     network.setTopology(this);
  }

  public void connectLayers() {
     for (int i=0;i<layers.size()-1;i++) {
       for (int n=0;n<layers.get(i).size();n++) { // a layer
          Node source=layers.get(i).get(n);
          for (int m=0;m<layers.get(i+1).size();m++) { // the next layer
            Node destination=layers.get(i+1).get(m);
            connect(source,destination);
          }
      }
    }
    restructureEdges();
  }

 public int getNumberOfInputGroups() {
    int res=0;
    for (int i=1;i<layers.size();i++) {
       res+=layers.get(i).size();
    }
    return res;
  }

 private void restructureEdges() {
  ArrayList<Edge> temp=new ArrayList<Edge>(network.getEdges().size());
    for (int i=1;i<layers.size();i++) { // process hidden layers and output
       for (int n=0;n<layers.get(i).size();n++) { // process each node
          Node destination=layers.get(i).get(n);
          temp.addAll(destination.getInputEdges());
      }
    }
    network.setEdges(temp); // replace edges with new organization
  }

  public ArrayList<Edge> getAllEdges() {return network.getEdges();}

  public int[] getInputGroupBoundries() {
     int[] result=new int[getNumberOfInputGroups()];
     int pos=0; int accounted=0;
     for (int i=1;i<layers.size();i++) { // process hidden layers and output
       for (int n=0;n<layers.get(i).size();n++) { // process each node
          Node destination=layers.get(i).get(n);
          result[pos]=accounted; // the position of first edge for this node = after all previous nodes' edges.
          accounted+=destination.getInputEdges().size(); // count edges in this node
          pos++;
      }
    }
    return result;
  }

  public void connect(Node source, Node destination) {
     Edge e=new Edge(source, destination);
     network.addEdge(e);
     source.addOutputEdge(e);
     destination.addInputEdge(e);
  }

  public void disconnect(Node source, Node destination) {
     network.removeEdge(network.getEdge(source, destination));
  }

  public Node getNode(int layer, int number) {
    return layers.get(layer).get(number);
  }

   public ThresholdNode getThresholdNode(int layer, int number) {
    return (ThresholdNode)thresholdlayers.get(layer).get(number);
  }

  public int numlayers() {return layers.size();}
  public int layerSize(int layer) {return layers.get(layer).size();}
  public ArrayList<Node> getLayer(int layer) {return layers.get(layer);}
  public ArrayList<Node> getLayerThresholds(int layer) {return thresholdlayers.get(layer);}

  public String toString() {
   String s;
   s = "Layered Net ["+layers.size()+"]  Inputs="+layerSize(0)+"  Outputs="+layerSize(layers.size()-1)+"   Hidden=";
   for (int i=0;i<layers.size()-2;i++) {s+=layerSize(1+i)+"  ";}
   return s;
  }

  public int[] getLayerSizes() {return this.layersizes;}

}