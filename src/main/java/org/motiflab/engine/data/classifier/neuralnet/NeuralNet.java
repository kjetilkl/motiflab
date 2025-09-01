/*
 
 
 */

package org.motiflab.engine.data.classifier.neuralnet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.classifier.Classifier;
import org.motiflab.engine.data.classifier.ClassifierDataSet;
import org.motiflab.engine.data.classifier.ClassifierOutput;
import org.motiflab.engine.data.classifier.Example;
import org.motiflab.engine.task.OperationTask;

/**
 *
 * @author Kjetil
 */
public abstract class NeuralNet extends Classifier implements java.io.Serializable {

  private ArrayList<Node> nodes;
  private ArrayList<Node> inputnodes; // a "view" - holds a subset of Arraylist "nodes""
  private ArrayList<Node> outputnodes;  // a "view" - holds a subset of Arraylist "nodes""
  private ArrayList<Edge> edges;
  private double netValmax=1.0; // highest value allowed in the net (this may be changed when net is trained on examples with different range)
  private double netValmin=0.0; // lowest value allowed in the net (this may be changed when net is trained on examples with different range)


  private ArrayList<String> classnames; //

  private Topology topology;

  private int numberofepochs=1; // number of epochs to use when training of a dataset

  /** Creates new NeuralNet */
  public NeuralNet() {
    nodes=new ArrayList<Node>();
    inputnodes=new ArrayList<Node>();
    outputnodes=new ArrayList<Node>();
    edges=new ArrayList<Edge>();
  }

  public void setClasses(ArrayList<String> classes) {this.classnames=classes;}

  public String getClassNameFromNumber(int i) {return classnames.get(i);}

  public int getClassNumberFromName(String name) {
     return classnames.indexOf(name);
  }


  public void invalidate() { // invalidates node outputs
     for (int i=0;i<nodes.size();i++) {
        ((Node)nodes.get(i)).invalidate();
      }
  }
  public void invalidateErrors() { // invalidates node errorterms (used by backpropagation)
     for (int i=0;i<nodes.size();i++) {
        ((Node)nodes.get(i)).invalidateError();
      }
  }

  public void clear() {
    nodes.clear();
    inputnodes.clear();
    outputnodes.clear();
    edges.clear();
  }

  public void setNetMinAndMaxValues(double min, double max) {
      netValmax=max;
      netValmin=min;
  }

  public void addNode(Node n)       {nodes.add(n);}
  public void addInputNode (Node n) {nodes.add(n);inputnodes.add(n);}
  public void addOutputNode(Node n) {nodes.add(n);outputnodes.add(n);}

  public void addEdge(Edge e) {edges.add(e);}
  public void setEdges(ArrayList<Edge>newedgeset) {edges=newedgeset;}
  public void removeEdge(Edge e) {edges.remove(e);}
  public Edge getEdge(Node source, Node destination) { return null;}
  public ArrayList<Edge> getEdges() {return edges;}
  public ArrayList<Node> getNodes() {return nodes;}

  public Topology getTopology() {return topology;}
  public void setTopology(Topology topology) {
      this.topology=topology;
      //notifyListeners();
  }

  public void setEdgeWeight(int edgenumber, double weight) {
    Edge edge=(Edge)edges.get(edgenumber);
    edge.setWeight(weight);
  }

  public void setEdgeWeights(double[] weights) {
    for (int i=0;i<weights.length;i++) {
      Edge edge=(Edge)edges.get(i);
      edge.setWeight(weights[i]);
    }
  }

  /** Randomly initializes all edgeweights in the net with a value between 0 and argument "mag" */
  public void randomInitialize(double mag) {
      for (int i=0;i<edges.size();i++) {
         Edge e=(Edge)edges.get(i);
         double weight=(Math.random()*mag);
         e.setWeight(weight);
      }
  }

  /** Randomly initializes all edgeweights in the net with a value between -mag and +mag.
   *  if "round" parameter is set to true, weights are rounded off to nearest integer.
   */
  public void randomInitializeSigned(double mag, boolean round) {
      for (int i=0;i<edges.size();i++) {
         Edge e=(Edge)edges.get(i);
         double weight=(Math.random()*mag);
         if (Math.random()<0.5) weight*=-1;
         //System.err.println("round="+round+"   weight="+weight+"   rounded="+Math.round(weight));
         if (round) e.setWeight(Math.round(weight));
         else e.setWeight(weight);
      }
  }

  /**
   * Returns the hypothesized classification (an integer) of the given example
   * according to this (trained or untrained) network. The classification returned
   * is the "number" of the highest scoring outputnode
   */
  private int getHighestScoringNodeNumber(Example example) {
    // initialize net
    invalidate();
    // System.err.println("Classify: #inputnodes="+inputnodes.size());
    for (int i=0;i<inputnodes.size();i++) {
       InputNode node=(InputNode)inputnodes.get(i);
       node.setValue(example.getAdjustedValue(i,netValmin,netValmax));
    }
    //extract classification from max of outputnodes
    double max=0;
    int bestnode=-1;
      for (int i=0;i<outputnodes.size();i++) {
        Node node=(Node)outputnodes.get(i);
        //System.err.println("Node#"+i+"="+node.getOutput()+"  max="+max);
        if (node.getOutput()>max) {bestnode=i;max=node.getOutput();}
      }
      if (outputnodes.size()==1) { // single outputnode classifiers (return high or low value) 0 or 1
         if (max<(netValmin+(netValmax-netValmin)/2)) bestnode=0; else bestnode=1;
      }
      //System.err.println("best="+bestnode+"  max="+max);
    return bestnode;
  }

  /**
   *
   * Returns the hypothesized class (a string) of the given example
   * according to this (trained or untrained) network.
   */
   @Override
  public String classify(Example example) {
      int bestnode=getHighestScoringNodeNumber(example); // get the number of the highest scoring node (this corresponds to some class)
      return getClassNameFromNumber(bestnode) ; // convert the number to the corresponding class name
  }

   @Override
  public double scoreExample(Example example) {
        invalidate();
        for (int i=0;i<inputnodes.size();i++) { // send the example into the net
           InputNode node=(InputNode)inputnodes.get(i);
           node.setValue(example.getAdjustedValue(i,netValmin,netValmax));
        }
        Node node=(Node)outputnodes.get(0); // Collect output. I assume only 1 outputnode is used if the scoreExample() method is called
        double score=node.getOutput(); // the score can have any value so normalize it
        double normalizedScore=(double)((score-netValmin)/(netValmax-netValmin));
        return normalizedScore;
  }
   

  double momentum=0.2;
  double learningrate=0.15;

  public void setMomentum(double m) {momentum=m;}
  public void setLearningRate(double l) {learningrate=l;}
  public void setMomentum(Double m) {momentum=m.doubleValue();}
  public void setLearningRate(Double l) {learningrate=l.doubleValue();}
  public double getMomentum() {return momentum;}
  public double getLearningRate() {return learningrate;}



//  @Override
//  public HashMap<String,Object> validate(ClassifierDataSet validationset, String phase) {
//     HashMap<String,Object> results=new HashMap<String,Object>();
//     int errors=0;
//     int tp=0,fp=0,tn=0,fn=0;
//     double[] squarederror=new double[outputnodes.size()];
//     double[] confidence=new double[outputnodes.size()];
//     double sumsquarederrors=0;
//     for (int i=0;i<validationset.size();i++){
//        Example ex=validationset.getExample(i);
//        String correctClass=ex.getClassification();
//        if (classify(ex).equals(correctClass)) {
//            if (correctClass.equals(CLASS_POSITIVE)) tp++; else tn++;
//        } else {
//            errors++;
//            if (correctClass.equals(CLASS_POSITIVE)) fn++; else fp++;
//        }
//        confidence=getOutputConfidence(confidence); // load the values of the outputnodes into confidence[]
//        if (squarederror.length>1) {
//          for (int j=0;j<squarederror.length;j++) squarederror[j]=0; // reset all entries to 0...
//          squarederror[getClassNumberFromName(ex.getClassification())]=1; // ...except the node corresponding to the correct classification
//          for (int j=0;j<squarederror.length;j++) {
//          sumsquarederrors+=Math.pow((squarederror[j]-confidence[j]),2);
//          }
//        } else { // only one output node
//          squarederror[0]=getClassNumberFromName(ex.getClassification());
//          sumsquarederrors+=Math.pow((squarederror[0]-confidence[0]),2);
//        }
//     }
//      if (phase.equals(Classifier.TRAINING)) {
//         results.put(Classifier.TRAININGSET_SIZE,new Integer(validationset.size()));
//         results.put(Classifier.TRAINING_MISTAKES,new Integer(errors));
//         results.put(Classifier.TRAINING_CORRECT,new Integer(validationset.size()-errors));
//         results.put(Classifier.TRAINING_ERROR,new Double(sumsquarederrors));
//     } else {
//         results.put(Classifier.VALIDATIONSET_SIZE,new Integer(validationset.size()));
//         results.put(Classifier.VALIDATION_MISTAKES,new Integer(errors));
//         results.put(Classifier.VALIDATION_CORRECT,new Integer(validationset.size()-errors));
//         results.put(Classifier.VALIDATION_ERROR,new Double(sumsquarederrors));
//     }
//
//     return results;
//  }
//
//    @Override
//    public HashMap<String,Object> validateWeighted(ClassifierDataSet validationset) {
//     HashMap<String,Object> results=new HashMap<String,Object>();
//     double errorsweight=0.0, correctweight=0.0;
//     for (int i=0;i<validationset.size();i++){
//        Example ex=validationset.getExample(i);
//        if (!classify(ex).equals(ex.getClassification())) errorsweight+=ex.getWeight();
//        else correctweight+=ex.getWeight();
//     }
//     results.put(Classifier.TRAINING_MISTAKES_WEIGHTED,new Double(errorsweight));
//     results.put(Classifier.TRAINING_CORRECT_WEIGHTED,new Double(correctweight));
//     return results;
//  }


  /** Serialize and save the entire network to file */
  public void saveNet(String filename) {
    try {
     ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
     out.writeObject(nodes);
     out.writeObject(inputnodes);
     out.writeObject(outputnodes);
     out.writeObject(edges);
     out.writeObject(topology);
     out.close();
    } catch(Exception e) {System.err.println(e.getMessage());}
  }

  /** Load an entire serialized network from file */
  @SuppressWarnings("unchecked")
  public void loadNet(String filename) {
   try {
     ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
     nodes       = (ArrayList<Node>)in.readObject();
     inputnodes  = (ArrayList<Node>)in.readObject();
     outputnodes = (ArrayList<Node>)in.readObject();
     edges       = (ArrayList<Edge>)in.readObject();
     topology    = (Topology)in.readObject();
     in.close();
     //notifyListeners();
    } catch(Exception e) {System.err.println(e.getMessage());}

  }


  public void setNumberOfEpochs(int epochs) {
      numberofepochs=epochs;
  }
  public void setNumberOfEpochs(Integer epochs) {
      numberofepochs=epochs.intValue();
  }

  public int getNumberOfEpochs() {
      return numberofepochs;
  }

  private int outputstep=1;
  public void setOutputStep(int outputstep) {
      this.outputstep=outputstep;
  }



  /** trains the network on a set of examples by training on each of them in turn
   *  (by calling the train(Example) method) and then repeat this process through several 'epochs'
   */
  @Override
  public void train(ClassifierDataSet trainingset, ClassifierDataSet validationSet, OperationTask task) throws InterruptedException  {
    ArrayList<Example> examples=trainingset.getTrainingExamples(getSamplingStrategy());
    setClasses(trainingset.getClasses()); // mapping between class names and "node numbers" (i think)
    ClassifierOutput info=new ClassifierOutput();
    info.put(Classifier.NUMBER_OF_EPOCS,new Integer(getNumberOfEpochs()));
    info.put(Classifier.NEURAL_NETWORK,this);  // for the custom network visualizer
    info.put(Classifier.CLASSIFIER_NAME,getName());
    info.put(Classifier.CLASSIFIER_TYPE,getClassifierType());
    info.setProgress(0);
    notifyOutputListenersStart(info);
    HashMap<String,Object> results=validate(trainingset, Classifier.TRAINING);          // test performance of the the untrained net first
    //results.putAll(validateWeighted(trainingset));          // test performance of the the untrained net first
    if (validationSet!=null) results.putAll(validate(validationSet,Classifier.VALIDATION));
    results.put(Classifier.EPOCH,new Integer(0));
    results.put(Classifier.NUMBER_OF_EPOCS,new Integer(getNumberOfEpochs()));
    results.put(Classifier.NEURAL_NETWORK,this);
    results.put(ClassifierOutput.PROGRESS,new Integer(0));
    notifyOutputListenersRound(createOutputInformation(results, getName(), getClassifierType()));
    for (int epoch=1;epoch<=getNumberOfEpochs();epoch++) {
          if (shouldAbort() || Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
          Thread.yield();
          for (Example ex : examples) {
              train(ex); // update this method to return some output information
          }
          if (epoch%outputstep==0 || epoch==getNumberOfEpochs()) { // only create output every 'outputstep' step
              results=validate(trainingset, Classifier.TRAINING);          // test performance of the the untrained net first
              //results.putAll(validateWeighted(trainingset));          // test performance of the the untrained net first
              if (validationSet!=null) results.putAll(validate(validationSet,Classifier.VALIDATION));
              results.put(Classifier.EPOCH,new Integer(epoch));
              results.put(Classifier.NUMBER_OF_EPOCS,new Integer(getNumberOfEpochs()));
              results.put(Classifier.NEURAL_NETWORK,this);
              results.put(ClassifierOutput.PROGRESS,new Integer((int)((double)epoch/(double)getNumberOfEpochs()*100)));
              notifyOutputListenersRound(createOutputInformation(results, getName(), getClassifierType()));
          }
     }
     notifyOutputListenersFinished(createOutputInformation(results, getName(), getClassifierType()));
     setStatus(TRAINED);
  }



  /** trains the network on a single example by updating weights through backpropagation */
  public boolean train(Example example) { // Train the network on a single Example
    // invalidate errorterms for all nodes
    invalidateErrors();

    //System.err.println("netValmax="+netValmax+", netValmin="+netValmin);
    // initialize net, and propagate forward to classify example
    int prediction=getHighestScoringNodeNumber(example);
    int result=getClassNumberFromName(example.getClassification()); // the correct answer obtained from the training data

    // calculate error of outputnodes directly
    double o_k, t_k;
    if (outputnodes.size()>1) {
      for (int i=0;i<outputnodes.size();i++) {
       Node node=(Node)outputnodes.get(i);
       o_k=node.getOutput();
       if (result==i) t_k=netValmax; else t_k=netValmin; // assumes classification == highest output node
       node.setErrorTerm(o_k*(1-o_k)*(t_k-o_k));
      }
    }
    else { // if only one output node - classification based on value close to min or max (2 classes)
       Node node=(Node)outputnodes.get(0);
       o_k=node.getOutput();
       if (result==1) t_k=netValmax; else t_k=netValmin; //
       node.setErrorTerm(o_k*(1-o_k)*(t_k-o_k));
    }

    // calculate errorterms for rest of the nodes
    for (int i=0;i<nodes.size();i++) {
       Node node=(Node)nodes.get(i);
       if (!node.isErrorValid()) node.getErrorTerm(); // this forces update of error terms with backpropagation.
                                                      // It is recursive if necessery, so order of nodes called is
                                                      // irrelevant (it takes care of itself...)
    }

    // update all edgeweights
    for (int i=0;i<edges.size();i++) {
       Edge edge=(Edge)edges.get(i);
       Node source=edge.getSource();
       Node destination=edge.getDestination();
       double weight=edge.getWeight();
       double lastupdate=edge.getLastUpdate();
       weight+=(learningrate*destination.getErrorTerm()*source.getOutput())+momentum*lastupdate; // adjust edgeweight
       edge.setWeight(weight);
    }
    return (prediction==result);
  }

 /**  Returns the values of the outputnodes */
  public double[] getOutputConfidence(double[] measures) {
    if (measures==null || measures.length!=outputnodes.size()) measures=new double[outputnodes.size()];
    for (int i=0;i<outputnodes.size();i++)
        measures[i]=((Node)outputnodes.get(i)).getOutput();
    return measures;
  }

  // NetUpdateListener stuff... notify listeners when network is updated
//  private ArrayList<NetUpdateListener> listeners;
//
//  public void addListener(NetUpdateListener x) {
//    if (listeners==null) listeners=new ArrayList<NetUpdateListener>();
//    listeners.add(x);
//  }
//
//  public void removeListener(NetUpdateListener x){
//     listeners.remove(x);
//  }
//
//  public void notifyListeners() {
//     if (listeners==null) return;
//     System.err.println("Net notifying "+listeners.size()+" listeners");
//     for (int i=0;i<listeners.size();i++) {
//         NetUpdateListener x=(NetUpdateListener)listeners.get(i);
//         x.netUpdatePerformed();
//     }
//  }

     public String getParametersDescription() {
         return "Epochs="+getNumberOfEpochs()+", learningrate="+learningrate+", momentum="+momentum;
     }

     public void ping() {
//        OutputInformation info=new OutputInformation();
//        info.put(Classifier.NEURAL_NETWORK,this);
//        notifyOutputListenersStart(info);
     };


     @Override
     public NeuralNet clone() {
         throw new InstantiationError("clone(): Not implemented yet in NeuralNet - Kjetikl");
     }

     public void importData(Data source) {
         throw new InstantiationError("importData(): Not implemented yet in NeuralNet - Kjetikl");
     }



}