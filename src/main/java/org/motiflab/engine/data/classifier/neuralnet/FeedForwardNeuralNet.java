/*
 
 
 */

package org.motiflab.engine.data.classifier.neuralnet;

import java.util.StringTokenizer;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.classifier.Classifier;
import org.motiflab.engine.data.classifier.ClassifierVisualizer;

/**
 *
 * @author Kjetil
 */
public class FeedForwardNeuralNet extends NeuralNet {

    private int[] layernodes; // each entry is a layer that specifies the number of nodes in that layer. Layer 0 is input and the last entry is output layer

    @Override
    public String getClassifierType() {return "Neural Network";}

    public FeedForwardNeuralNet() {
         setName("NeuralNet");
         addParameter("Topology",String.class,"8", null,"Specifies the number of nodes in each hidden layer (multiple hidden-layers can be specified separated by commas)",true,false);
         addParameter("UseTopology",String.class,"", null,"This will be the same as above but flanked with the number of input and output nodes ",true,true);
         addParameter("Epochs",Integer.class,300,new Integer[]{1,50000},"",true,false);
         addParameter("Learning rate",Double.class,0.15, new Double[]{0.0,10.0},"",true,false);
         addParameter("Momentum",Double.class,0.2, new Double[]{0.0,10.0},"",true,false);
         // addParameter("Sampling",String.class,Classifier.SAMPLING_STRATEGY[0], new String[]{Classifier.SAMPLING_STRATEGY[0],Classifier.SAMPLING_STRATEGY[1],Classifier.SAMPLING_STRATEGY[2]},"A strategy to use when sampling examples for boosting",false,false);
    }


    /** Creates a new instance of FeedForwardNeuralNet with the number of nodes in each layer specified by the argument array*/
    @Deprecated
    public FeedForwardNeuralNet(int[] nodes) {
       setName("NeuralNet");
       layernodes=nodes;
       LayeredNet LNtopology=new LayeredNet(this, nodes); // this will also connect the network with the topology
       LNtopology.connectLayers(); // initialize to fully connected network
       this.randomInitialize(0.1);
    }

    /** Creates a new instance of FeedForwardNeuralNet with the number of nodes in each layer specified by the argument array*/
    @Deprecated
    public FeedForwardNeuralNet(int[] nodes,double learningRate, double momentum, double randomInitialize) {
        setName("NeuralNet");
        LayeredNet LNtopology=new LayeredNet(this, nodes); // this will also connect the network with the topology
        layernodes=nodes;
        LNtopology.connectLayers();
        this.randomInitialize(randomInitialize);
        this.setLearningRate(learningRate);
        this.setMomentum(momentum);
    }

    @Override
    public ClassifierVisualizer getVisualizer() {
        return new LayeredNetVisualizer(this);
    }

    public Object getValue() {
        return this;
    }




    public String getValueAsParameterString() {
        throw new UnsupportedOperationException("Not supported: FeedForwardNeuralNet.getValueAsParameterString()");
    }

    @Override
    public String getParametersDescription() {
       String text="Topology=[";
       for (int i=0;i<layernodes.length-1;i++) text=text.concat(layernodes[i]+",");
       text=text.concat(layernodes[layernodes.length-1]+"], epochs="+getNumberOfEpochs()+", learningrate="+learningrate+", momentum="+momentum+", sampling="+Classifier.SAMPLING_STRATEGY[getSamplingStrategy()]);
       return text;
    }


      public void setInputOutput(int inputs, int outputs) {
        layernodes[0]=inputs;
        layernodes[layernodes.length-1]=outputs;
        LayeredNet LNtopology=new LayeredNet(this, layernodes); // this will also connect the network with the topology
        LNtopology.connectLayers();
        this.randomInitialize(0.1);
      }


     /** changes the number of nodes in hidden layer and refactors net */
     public void setNodesInSingleHiddenLayer(int number) {
        layernodes[1]=number;
        LayeredNet LNtopology=new LayeredNet(this, layernodes); // this will also connect the network with the topology
        LNtopology.connectLayers(); // initialize to fully connected network
        this.randomInitialize(0.1);
     }

     public int getSingleHiddenLayerNodes() {
         if (layernodes==null) return 0;
         else return layernodes[1]; // this is the middle layer
     }

     private String getHiddenNodesAsString() {
         String text="";
         for (int i=1;i<layernodes.length-3;i++) text+=""+layernodes[i]+",";
         text+=""+layernodes[layernodes.length-2];
         return text;
     }

     public void setHiddenNodesAsString(String inputstring) {
        int[] nodes = tokanizeString(inputstring);
        nodes[0]=layernodes[0]; // copy first
        nodes[nodes.length-1]=layernodes[layernodes.length-1]; // and last value from previous net!
        layernodes=nodes;
        LayeredNet LNtopology=new LayeredNet(this, layernodes); // this will also connect the network with the topology
        LNtopology.connectLayers(); // initialize to fully connected network
        this.randomInitialize(0.1);
     }

    private int[] tokanizeString(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line,",");
        int size=tokenizer.countTokens();
        int[] result=new int[size+2];
        int i=1;
        while (tokenizer.hasMoreTokens()) {
            String word=tokenizer.nextToken();
            try {int value=Integer.parseInt(word);result[i]=value;}
            catch(NumberFormatException i3) {System.err.println("Error parsing hidden layer nodes");result[i]=3;}
            i++;
         }
        return result;
   }


//  public OperatorPanel getOperatorPanel(javax.swing.JFrame parent) {
//    OperatorPanel panel=null;
//       try {
//
//       Method m1=FeedForwardNeuralNet.class.getMethod("setHiddenNodesAsString", new Class[]{String.class});
//       Method m2=FeedForwardNeuralNet.class.getMethod("setNumberOfEpochs", new Class[]{Integer.class});
//       Method m3=FeedForwardNeuralNet.class.getMethod("setLearningRate", new Class[]{Double.class});
//       Method m4=FeedForwardNeuralNet.class.getMethod("setMomentum", new Class[]{Double.class});
//       Method m5=FeedForwardNeuralNet.class.getMethod("setSamplingStrategy", new Class[]{Integer.class});
//
//      panel=new OperatorPanel(
//       parent,
//       "Neural Network Classifier",
//       new Object[]{
//          new Object[]{"Hidden layers",String.class,getHiddenNodesAsString(),m1},
//          new Object[]{"Epochs",Integer.class,""+getNumberOfEpochs(),m2},
//          new Object[]{"Learning rate",Double.class,""+getLearningRate(),m3},
//          new Object[]{"Momentum",Double.class,""+getMomentum(),m4},
//          new Object[]{"Sampling",new Object[]{Labels.SAMPLING_STRATEGY[0],Labels.SAMPLING_STRATEGY[1],Labels.SAMPLING_STRATEGY[2]},Labels.SAMPLING_STRATEGY[getSamplingStrategy()],m5},
//
//        },
//         "The number of nodes in hidden layers is\n"
//       + "specified by a comma separated list of \n"
//       + "integers.\n"
//       + "The sampling strategy specifies how training\n"
//       + "examples are selected. If no sampling is used\n"
//       + "then every training example is used for \n"
//       + "training. If some other scheme is chosen\n"
//       + "then the classifier is trained on a slightly\n"
//       + "different trainingset where examples are\n"
//       + "sampled with replacement from the original\n"
//       + "trainingset according to their weights.\n"
//       , this);
//       }  catch(Exception ce) {System.err.println("Problems occurred during OperatorPanel initialization\n"+ce.getMessage()+"\n"+ce.getClass().toString());}
//    return panel;
//  }

    @Override
    public void initializeFromParameters(ParameterSettings settings, MotifLabEngine engine) throws ExecutionError {
        double learningRate=0.15;
        double newmomentum=0.2;
        int epochs=1;
        String layerString=null;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              learningRate=(Double)settings.getResolvedParameter("Learning rate",defaults,engine);
              newmomentum=(Double)settings.getResolvedParameter("Momentum",defaults,engine);
              epochs=(Integer)settings.getResolvedParameter("Epochs",defaults,engine);          
              layerString=(String)settings.getResolvedParameter("UseTopology",defaults,engine);
           } catch (ExecutionError e) {
              throw e;
           } catch (Exception ex) {
              throw new ExecutionError("An error occurred during classifier initialization", ex);
           }
        } else {
            learningRate=(Double)getDefaultValueForParameter("Learning rate");
            newmomentum=(Double)getDefaultValueForParameter("Momentum");
            epochs=(Integer)getDefaultValueForParameter("Epochs");
            layerString=(String)getDefaultValueForParameter("UseTopology");
        }    
        int[] nodes=null;
        String[] layers=layerString.split("\\s*,\\s*");
        nodes=new int[layers.length];
        for (int i=0;i<layers.length;i++) {
            try {
                nodes[i]=Integer.parseInt(layers[i]);
            } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse expected numeric entry for number of nodes in network layer: "+layers[i]);}
        }
        LayeredNet LNtopology=new LayeredNet(this, nodes); // this will also connect the network with the topology
        layernodes=nodes;
        LNtopology.connectLayers();
        this.randomInitialize(0.1);
        this.setLearningRate(learningRate);
        this.setMomentum(newmomentum); 
        this.setNumberOfEpochs(epochs);
    }


}

