/*
 
 
 */

package motiflab.engine.data.classifier.adaboost;

import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.classifier.Classifier;
import motiflab.engine.data.classifier.ClassifierVisualizer;
import motiflab.engine.data.classifier.Example;
import motiflab.engine.data.classifier.ClassifierDataSet;
import motiflab.engine.data.classifier.ClassifierOutput;
import motiflab.engine.data.classifier.neuralnet.FeedForwardNeuralNet;
import motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class AdaBoost extends Classifier {

    public ArrayList<Classifier> classifiers = new ArrayList<Classifier>();
    public HashMap<Classifier,Double> classifiersweights  = new HashMap<Classifier,Double>();


    public AdaBoost(){
      setName("AdaBoost");
    }

    @Override
    public String getClassifierType() {return "AdaBoost";}

    public void addClassifier(Classifier classifier) {
        classifiers.add(classifier);
        classifiersweights.put(classifier,new Double(1));
       // resetClassifierWeights();
    }

    public void removeClassifier(Classifier classifier) {
        classifiers.remove(classifier);
        classifiersweights.remove(classifier);
        resetClassifierWeights();
    }

    /** set all hyphoteses' (classifier) weights to 1 (the value doesn't really matter) */
    public void resetClassifierWeights() {
       int N=classifiers.size();
       for (int i=0;i<N;i++) {
           Classifier c=classifiers.get(i);
           classifiersweights.put(c,new Double(1.0)); // divide by N?
       }
    }

    public void setClassifierWeight(Classifier classifier, Double weight) {
        classifiersweights.put(classifier,weight);
    }

    public Double getClassifierWeight(Classifier classifier) {
        return classifiersweights.get(classifier);
    }

    public int getNumberOfClassifiers() {return classifiers.size();}
    public Classifier getClassifier(int i) {return classifiers.get(i);}
    public Class getClassifierClass(int i) {return classifiers.get(i).getClass();}

    public ArrayList<Classifier> getClassifiers() {return classifiers;}
    public HashMap<Classifier,Double> getClassifierWeights() {return classifiersweights;}


    public void makeClean(int inputs, int outputs) {
        for (Classifier classifier : classifiers) {
            classifier.makeClean();
            if (classifier.getClass()==FeedForwardNeuralNet.class) ((FeedForwardNeuralNet)classifier).setInputOutput(inputs,outputs);
        }
    }

    /**
     *
     * The AdaBoost training algorithm
     */
    @Override
    public void train(ClassifierDataSet trainingSet, ClassifierDataSet validationSet, OperationTask task) throws InterruptedException {
        resetClassifierWeights(); // reset the weights of the hypotheses (classifiers) to the same value 1/M
        trainingSet.resetWeights(); // reset the weights of the training examples to the same value 1/N
        for (Classifier classifier : classifiers) classifier.setStatus(Classifier.UNTRAINED);
        ClassifierOutput info=new ClassifierOutput();
        info.put(Classifier.CLASSIFIERS,getClassifiers());
        info.put(Classifier.CLASSIFIER_WEIGHTS,getClassifierWeights());
        info.put(Classifier.EXAMPLES,trainingSet.getExamples());
        info.put(Classifier.EXAMPLES_WEIGHTS,trainingSet.getExampleWeights());
        info.put(Classifier.CLASSIFIER_TYPE,getClassifierType());
        info.setProgress(0);
        notifyOutputListenersStart(info);
        HashMap<String,Object> results=null;
        for (int i=0;i<classifiers.size();i++) { // for each classifier
            if (shouldAbort() || Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
            Thread.yield();
            Classifier hypothesis = classifiers.get(i); // get each classifier in turn
            info=new ClassifierOutput("\nTraining classifier number "+(i+1)+" ("+hypothesis.getName()+")");
            info.setStatusBarInformation("Training classifier "+(i+1)+" of "+classifiers.size());
            info.setProgress((int)(i*100.0/classifiers.size()));
            notifyOutputListenersRound(info);
            hypothesis.train(trainingSet,validationSet,task); // train the classifier on the weighted dataset
            double error=0; // this error value measures the performance of the trained classifier over every example in the trainingSet. Each time an example is misclassified its weight will be added to the error sum.
            int numerrors=0; // this measures the number of misclassifications of the training set
            for (int j=0;j<trainingSet.size();j++) { // assess oerformance on trainingSet
                Example example = trainingSet.getExample(j); // for every example in the trainingset
                if (!(hypothesis.classify(example).equals(example.getClassification()))) {error = error + trainingSet.getWeight(example).doubleValue();numerrors++;} // update 'error' if the hypothesis misclassifies the example
            }
            //System.err.println("Error (weighted)="+error+" from "+numerrors+" misclassifications. Update="+(error/(1.0-error)));
            if (error<0.005 ) {
                setClassifierWeight(hypothesis,new Double(0)); // perfect classifiers must be deleted since their weights would otherwise be Infinity. If error=0.5 then classifiers weight will be 0 anyway (if error>0.5 then if f*cks up the example weighting system!)
            } else {
                for (int j=0;j<trainingSet.size();j++) { // update training example weights
                   Example example = trainingSet.getExample(j); // for every example in the trainingset
                   if (hypothesis.classify(example).equals(example.getClassification())) trainingSet.setWeight(example,trainingSet.getWeight(example)*error/(1.0-error)); // diminish the weight of the example if it is correctly classified
                }
                trainingSet.normalizeWeights(); // normalize the updated weights of the trainingset so that they sum to 1
                double newclassifierweight = Math.log((1.0-error)/error); //
                setClassifierWeight(hypothesis,new Double(newclassifierweight)); // set the weight of current classifier according to its perfomance on the training set
            }
            info.setOutputText("Classifier #"+(i+1)+" assigned weight "+(getClassifierWeight(hypothesis).doubleValue())+" error="+error);
            info.put(Classifier.CLASSIFIERS,getClassifiers());
            info.put(Classifier.CLASSIFIER_WEIGHTS,getClassifierWeights());
            info.put(Classifier.EXAMPLES,trainingSet.getExamples());
            info.put(Classifier.EXAMPLES_WEIGHTS,trainingSet.getExampleWeights());
            notifyOutputListenersRound(info);
            results=validate(trainingSet,Classifier.TRAINING);  // assess AdaBoost's performance on the trainingset
            results.putAll(validate(validationSet,Classifier.VALIDATION));  // assess AdaBoost's performance on the trainingset
            notifyOutputListenersRound(createAdaBoostClassifierOutput(results,i+1));
        } // end for each classifier

        //results=validate(trainingSet,Classifier.TRAINING);  // assess AdaBoost's performance on the trainingset
        //results.putAll(validate(validationSet,Classifier.VALIDATION));  // assess AdaBoost's performance on the trainingset
        String text="\n\n===============================================================\n\nAdaBoost finished training "+classifiers.size()+" classifiers\n\n";
        for (Classifier c:classifiers) {text=text.concat("  Classifier "+c.getName()+":  weight = "+getClassifierWeight(c)+"\n");}
        info=new ClassifierOutput(text);
        info.setProgress(100);
        if (shouldAbort() || Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();        
        info.setStatusBarInformation("AdaBoost finished training "+classifiers.size()+" classifiers");
        notifyOutputListenersRound(createOutputInformation(results,getName(),getClassifierType()));
        notifyOutputListenersFinished(info);
        setStatus(TRAINED);        
    }



     /**
     *
     * Classifies the given example by a majority vote among all hypotheses (classifiers)
     * All classifiers are tested on the example given and the weight of the classifier is
     * added to the accumulating value of the class returned by this classifier.
     * In the end the class with the most accumulated weight is returned as the majority vote.
     */
    @Override
    public String classify(Example example) {
        HashMap<String,Double> classweights = new HashMap<String,Double>(); // the accumulated prediction weights for each of the classes
        String majorityclass=null; double majorityclassweight=Double.NEGATIVE_INFINITY; // the number and weight of the currently highest scoring class
        for (Classifier classifier : classifiers) { // now let each classifier have a go at the example
            if (!classifier.isTrained()) continue; // skip this classifier for now
            String hypothesizedclass=classifier.classify(example); // the class predicted by a specific classifier
            double hypothesisweight=getClassifierWeight(classifier); // the weight awarded to this classifier
            if (classweights.containsKey(hypothesizedclass)) { // this class have been predicted by a previous classifier
                double oldweight=classweights.get(hypothesizedclass).doubleValue(); // ... this was the old weight given to the class (sum of classifier weights)
                classweights.put(hypothesizedclass,new Double(oldweight+hypothesisweight)); // ... now update it by adding the weight of the new classifier
             }
            else {
                classweights.put(hypothesizedclass,new Double(hypothesisweight)); // create a new weight for this class if no classifier has suggested this class before
            }
            hypothesisweight=classweights.get(hypothesizedclass).doubleValue();
            if (hypothesisweight>majorityclassweight) {
                majorityclassweight=hypothesisweight;
                majorityclass=hypothesizedclass;
            }
        }
        return majorityclass; // the class with the majority vote (most accumulated weight)
    }

    @Override
    public double scoreExample(Example example) {
        double sumScore=0;
        double sumWeights=0;
        for (Classifier classifier : classifiers) { // now let each classifier have a go at the example
            if (!classifier.isTrained()) continue; // skip this classifier for now
            double classifierScore=classifier.scoreExample(example); // the class predicted by a specific classifier
            double classifierWeight=getClassifierWeight(classifier).doubleValue(); // the weight awarded to this classifier
            sumScore+=classifierScore*classifierWeight;
            sumWeights+=(double)classifierWeight;
        }
        return sumScore/sumWeights; // the class with the majority vote (most accumulated weight)

    }




    public String classifyWithClassifier(Example example, Classifier classifier) {
        return classifier.classify(example);
    }

     public String classifyWithClassifierNumber(Example example, int i) {
        Classifier classifier=classifiers.get(i);
        if (classifier!=null) return classifier.classify(example);
        else return null;
    }


     /**
     * A convenient method for creating ClassifierOutput from validation results
     */
     public ClassifierOutput createAdaBoostClassifierOutput(HashMap<String,Object> map, int classifiernumber){
         //System.err.println("-- createAdaBoostClassifierOutput -- ");
         ClassifierOutput info=new ClassifierOutput(map);
           info.put(Classifier.CLASSIFIER_NAME,getName());
           info.put(Classifier.CLASSIFIER_TYPE,getClassifierType());
           info.put(Classifier.CLASSIFIER_NUMBER,new Integer(classifiernumber));
           if (map.get(Classifier.TRAINING_MISTAKES)!=null) {
              int misclassified=((Integer)map.get(Classifier.TRAINING_MISTAKES)).intValue();
              int correct=((Integer)map.get(Classifier.TRAINING_CORRECT)).intValue();
              int size=((Integer)map.get(Classifier.TRAININGSET_SIZE)).intValue();
              int correctclassifiedpercentage= (int)Math.round((correct*1.0/size*1.0)*100);
              int misclassifiedpercentage= (int)Math.round((misclassified*1.0/size*1.0)*100);
              //System.err.println("  AdaBoost("+classifiernumber+") classified "+(correct)+" out of "+size+" training-examples correctly ("+(correctclassifiedpercentage)+"%). "+misclassified+" misclassified ("+(misclassifiedpercentage)+"%).");
           }
           if (map.get(Classifier.VALIDATION_MISTAKES)!=null) {
              int misclassified=((Integer)map.get(Classifier.VALIDATION_MISTAKES)).intValue();
              int correct=((Integer)map.get(Classifier.VALIDATION_CORRECT)).intValue();
              int size=((Integer)map.get(Classifier.VALIDATIONSET_SIZE)).intValue();
              int correctclassifiedpercentage= (int)((correct*1.0/size*1.0)*100);
              int misclassifiedpercentage= (int)((misclassified*1.0/size*1.0)*100);
              //System.err.println("  AdaBoost("+classifiernumber+") classified "+(correct)+" out of "+size+" training-examples correctly ("+(correctclassifiedpercentage)+"%). "+misclassified+" misclassified ("+(misclassifiedpercentage)+"%).");
           }
           return info;
  }

     public String getParametersDescription() {
         return "";
     }

     private void setDebugWeights(ClassifierDataSet trainingSet) {
        trainingSet.setWeight(0,0.01);
        trainingSet.setWeight(1,0.02);
        trainingSet.setWeight(2,0.1);
        trainingSet.setWeight(3,0.2);
        trainingSet.setWeight(4,0.15);
        trainingSet.setWeight(5,0.06);
        trainingSet.setWeight(6,0.25);
        trainingSet.setWeight(7,0.05);
        trainingSet.setWeight(8,0.02);
        trainingSet.setWeight(9,0.03);
        trainingSet.setWeight(10,0.05);
        trainingSet.setWeight(11,0.06);
     }


     public void printClassifiers() {
         for (int i=0;i<classifiers.size();i++) {
             System.err.println("Classifier["+i+"] = "+classifiers.get(i).getClass().toString());
         }
     }

    @Override
    public void initializeFromParameters(ParameterSettings settings, MotifLabEngine engine) throws ExecutionError {
        throw new UnsupportedOperationException("Not supported yet: AdaBoost.initializeFromParameters()");
    }

    @Override
    public ClassifierVisualizer getVisualizer() {
         return new AdaBoostVisualizer(this);
    }


}
