/*
 
 
 */

package motiflab.engine.data.classifier.neuralnet;

import java.io.Serializable;

/**
 *
 * @author Kjetil
 */
public abstract class Topology implements Serializable {
     protected NeuralNet network;

      /** Creates new Topology */
      public Topology(NeuralNet net) {
         network=net;
      }

      public Topology() {
      }

      public void setNetwork(NeuralNet network) {
        this.network=network;
      }

      public String toString() {return "Unspecified Topology";}
}