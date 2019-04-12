/*
 
 
 */

package motiflab.engine.data.classifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterExporter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.classifier.bayes.NaiveBayesClassifier;
import motiflab.engine.data.classifier.decisiontree.DecisionTreeClassifier;
import motiflab.engine.data.classifier.neuralnet.FeedForwardNeuralNet;
import motiflab.engine.task.OperationTask;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

/**
 *
 * @author Kjetil
 */
public abstract class Classifier implements ParameterExporter, Serializable {
    public static final int UNTRAINED=0;
    public static final int TRAINED=1;
    
    public static final int NONE=0; 
    public static final int WEIGHTED=1;
    public static final int STOCHASTIC_UNIVERSAL_SAMPLING=2;
    
    public static final String TRAININGSET_SIZE="trainingsetsize";
    public static final String VALIDATIONSET_SIZE="validationsetsize";

    public static final String TRAINING="training";
    public static final String VALIDATION="validation";

    public static final String TRAINING_MISTAKES="trainingmistakes";
    public static final String TRAINING_CORRECT="trainingcorrect";
    public static final String TRAINING_MISTAKES_WEIGHTED="trainingmistakesWeighted";
    public static final String TRAINING_CORRECT_WEIGHTED="trainingcorrectWeighted";
    public static final String TRAINING_TP="trainingTP";
    public static final String TRAINING_FP="trainingFP";
    public static final String TRAINING_TN="trainingTN";
    public static final String TRAINING_FN="trainingFN";
    public static final String TRAINING_RANK_CORRELATION="trainingRankCorrelation";     
    public static final String VALIDATION_MISTAKES="validationmistakes";
    public static final String VALIDATION_CORRECT="validationcorrect";
    public static final String VALIDATION_TP="validationTP";
    public static final String VALIDATION_FP="validationFP";
    public static final String VALIDATION_TN="validationTN";
    public static final String VALIDATION_FN="validationFN";
    public static final String VALIDATION_RANK_CORRELATION="validationRankCorrelation";    

    public static final String TRAINING_ERROR="trainingerror";
    public static final String VALIDATION_ERROR="validationerror";

    public static final String EPOCH="epoch";
    public static final String NUMBER_OF_EPOCS="numberofepochs";
    
    public static final String CLASSIFIER_NAME="classifiername";
    public static final String CLASSIFIER_NUMBER="classifiernumber";
    public static final String CLASSIFIER_TYPE="classifiertype";
   
    public static final String CLASSIFIERS="classifiers";
    public static final String CLASSIFIER_WEIGHTS="classifierweights";
    public static final String EXAMPLES="examples";
    public static final String EXAMPLES_WEIGHTS="examplesweights";
    
    public static final String NEURAL_NETWORK="neuralnetwork";
    public static final String DECISION_TREE="decisiontree";
    
    public static final String SAMPLING_STRATEGY[]=new String[]{"None","Weighted","Stochastic Universal"}; // Note that the order here should correspond to the "public static final int"s above
    public static final String TRUE="YES";
    public static final String FALSE="NO";

    public static final String CLASS_POSITIVE="1";
    public static final String CLASS_NEGATIVE="0";


    // private ArrayList<OutputGenerator> outputlisteners;
    private String name="no name";
    private boolean useweightedexamples=false;
    private int classifierstatus=UNTRAINED;
    private transient int samplingStrategy=NONE;
    private transient boolean shouldabort=false;
    private transient ArrayList<ClassifierOutputListener> outputlisteners=null;
    protected transient ArrayList<Parameter> parameters=new ArrayList<Parameter>();

    public static int getSamplingStrategyForName(String strategy) {
        if (strategy==null) return NONE;
        for (int i=0;i<SAMPLING_STRATEGY.length;i++) {
            if (strategy.equalsIgnoreCase(SAMPLING_STRATEGY[i])) return i;
        }
        return NONE;
    }
    
    /** Notifies this classifier that it should abort its training */
    public void setAbort(boolean abort) {
       shouldabort=abort;
    }

    /** This method should be called at regular intervals if the Classifier undertakes labourious tasks
     *  If the method returns TRUE the classifier should abort what it is doing and return as soon as possible
     */
    protected boolean shouldAbort() {
        return shouldabort;
    }

    /** Trains the classifier on a training set
     *
     * @param trainingset
     */
    public void train(ClassifierDataSet trainingset, OperationTask task) throws InterruptedException  {train(trainingset,null, task);}

    /** Trains the classifier on a training set and evaluates its performance on the validationset
     *  The evaluation results on the training and validationsets can be obtained
     */
    public abstract void train(ClassifierDataSet trainingset, ClassifierDataSet validationset, OperationTask task) throws InterruptedException ;
    
    /** Returns a String with the name of the class predicted for this example by the classifier */
    public abstract String classify(Example example);

    /** For binary classification problems (2 classes), this method will return a double value in the range 0 to 1
     *  reflecting a belief that the example belongs to either of the two classes. A score closer to 0 suggests that
     *  the example belongs to the CLASS_NEGATIVE class while a score closer to 1 suggests it belongs to the CLASS_POSITIVE class.
     *  Depending on the classifier the returned value can be 'continuous' (for instance with neural nets or logistic regression classifiers)
     *  or it can be 'binary' (for decision trees)
     */
    public abstract double scoreExample(Example example);

    
    /**
     * Returns a visualizer for this Classifier
     * @return
     */
    public abstract ClassifierVisualizer getVisualizer();


  public void addOutputListener(ClassifierOutputListener listener) {
     if (outputlisteners==null) outputlisteners=new ArrayList<ClassifierOutputListener>();
     outputlisteners.add(listener);
  }

  public void removeOutputListener(ClassifierOutputListener listener) {
     if (outputlisteners==null) return;
     outputlisteners.remove(listener);
  }

  public void notifyOutputListenersStart(ClassifierOutput info) {
      if (outputlisteners==null) return;
      for (ClassifierOutputListener listener : outputlisteners) listener.start(info);
  }

  public void notifyOutputListenersRound(ClassifierOutput info) {
      if (outputlisteners==null) return;
      for (ClassifierOutputListener listener : outputlisteners) listener.round(info);
  }

  public void notifyOutputListenersFinished(ClassifierOutput info) {
      if (outputlisteners==null) return;      
      for (ClassifierOutputListener listener : outputlisteners) listener.finished(info);
  }

  public abstract void initializeFromParameters(ParameterSettings settings, MotifLabEngine engine) throws ExecutionError;

  public void ping(){}
  public String getName() {return name;}
  public void setName(String newname) {this.name=newname;}

  public boolean useWeightedExamples() {return useweightedexamples;}

  public void setUseWeightedExamples(boolean value) {useweightedexamples=value;}
  public void setUseWeightedExamples(Integer value) {
      useweightedexamples=(value.intValue()==1);
  }

  public void setStatus(int status) {this.classifierstatus=status;}
  public int getStatus() {return classifierstatus;}
  public boolean isTrained() {return classifierstatus==TRAINED;}

  public int getSamplingStrategy() {return samplingStrategy;}
  public void setSamplingStrategy(int strategy) {samplingStrategy=strategy;}
  public void setSamplingStrategy(Integer strategy) {samplingStrategy=strategy.intValue();}


  /**
   * Validates the classifiers performance on a dataset and returns a HashMap containing various statistics
   * such as the number of correctly and incorrectly classified instances
   * Note that classifiers overriding this method can return a superset of these statistics
   * @param validationset
   * @param phase A string which can be either 'training' or 'validation' (constants are available in Classifier)
   * @return
   */
  public HashMap<String,Object> validate(ClassifierDataSet validationset, String phase) { // phase is 'training' or 'validation''
     HashMap<String,Object> map=new HashMap<String,Object>();
     if (validationset==null) return map;
     int errors=0;
     int tp=0,fp=0,tn=0,fn=0;
     double[] rankX=new double[phase.equals(TRAINING)?0:validationset.size()];
     double[] rankY=new double[phase.equals(TRAINING)?0:validationset.size()];
     SpearmansCorrelation spearman=new SpearmansCorrelation();    
     for (int i=0;i<validationset.size();i++){
        Example ex=validationset.getExample(i);
        String correctClass=ex.getClassification();
        if (phase.equals(VALIDATION)) {
            if (correctClass.equals(CLASS_POSITIVE)) rankX[i]=1; else rankX[i]=0;
            rankY[i]=scoreExample(ex);
        }
        if (classify(ex).equals(correctClass)) {
            if (correctClass.equals(CLASS_POSITIVE)) tp++; else tn++;
        } else {
            errors++;
            if (correctClass.equals(CLASS_POSITIVE)) fn++; else fp++;
        }
     }
     double spearmanCorrelation=0;
     if (phase.equals(VALIDATION) && validationset.size()>0) {     
        spearmanCorrelation=spearman.correlation(rankX, rankY);
     }
     if (phase.equals(TRAINING)) {
        map.put(TRAININGSET_SIZE,new Integer(validationset.size()));
        map.put(TRAINING_MISTAKES,new Integer(errors));
        map.put(TRAINING_CORRECT,new Integer(validationset.size()-errors));
        map.put(TRAINING_TP,new Integer(tp));
        map.put(TRAINING_FP,new Integer(fp));
        map.put(TRAINING_TN,new Integer(tn));
        map.put(TRAINING_FN,new Integer(fn));
        map.put(TRAINING_RANK_CORRELATION,new Double(spearmanCorrelation));
     }
     else {
        map.put(VALIDATIONSET_SIZE,new Integer(validationset.size()));
        map.put(VALIDATION_MISTAKES,new Integer(errors));
        map.put(VALIDATION_CORRECT,new Integer(validationset.size()-errors));
        map.put(VALIDATION_TP,new Integer(tp));
        map.put(VALIDATION_FP,new Integer(fp));
        map.put(VALIDATION_TN,new Integer(tn));
        map.put(VALIDATION_FN,new Integer(fn));
        map.put(VALIDATION_RANK_CORRELATION,new Double(spearmanCorrelation));        
     }
     return map;
  }

    /**
     */
    public HashMap<String,Object> validateWeighted(ClassifierDataSet validationset) {
     HashMap<String,Object> map=new HashMap<String,Object>();
     if (validationset==null) return map;
     double errorsweight=0.0, correctweight=0.0;
     for (int i=0;i<validationset.size();i++){
        Example ex=validationset.getExample(i);
        if (!classify(ex).equals(ex.getClassification())) errorsweight+=ex.getWeight();
        else correctweight+=ex.getWeight();
     }
     map.put(TRAININGSET_SIZE,new Integer(validationset.size()));
     map.put(TRAINING_MISTAKES_WEIGHTED,new Double(errorsweight));
     map.put(TRAINING_CORRECT_WEIGHTED,new Double(correctweight));
     return map;
  }

    /**
     * A convenient method for creating outputinformation from training and validation results
     */
     protected ClassifierOutput createOutputInformation(HashMap<String, Object> map, String classifiername, String classifierType){
           ClassifierOutput info=new ClassifierOutput(map);
           String text="";
           info.put(CLASSIFIER_NAME,classifiername);
           info.put(CLASSIFIER_TYPE,classifierType);
           if (map.get(EPOCH)!=null) text="Epoch #"+map.get(EPOCH)+": \n";
           if (map.get(TRAINING_MISTAKES)!=null) {
              int misclassified=((Integer)map.get(TRAINING_MISTAKES)).intValue();
              int correct=((Integer)map.get(TRAINING_CORRECT)).intValue();
              int size=((Integer)map.get(TRAININGSET_SIZE)).intValue();
              int correctclassifiedpercentage= (int)Math.round((correct*1.0/size*1.0)*100);
              int misclassifiedpercentage= (int)Math.round((misclassified*1.0/size*1.0)*100);
              text=text.concat("  "+classifiername+" ["+classifierType+"] classified "+(correct)+" out of "+size+" training-examples correctly ("+(correctclassifiedpercentage)+"%). "+misclassified+" misclassified ("+(misclassifiedpercentage)+"%).");
           }
           if (map.get(TRAINING_MISTAKES_WEIGHTED)!=null) {
              double misclassifiedWeighted=((Double)map.get(TRAINING_MISTAKES_WEIGHTED)).doubleValue();
              double correctclassifiedWeighted=((Double)map.get(TRAINING_CORRECT_WEIGHTED)).doubleValue();
              text=text.concat(" Weighted performance = "+((int)Math.round(correctclassifiedWeighted*100.00))+"%");
           }
           if (map.get(VALIDATION_MISTAKES)!=null) {
              int misclassified=((Integer)map.get(VALIDATION_MISTAKES)).intValue();
              int correct=((Integer)map.get(VALIDATION_CORRECT)).intValue();
              int size=((Integer)map.get(VALIDATIONSET_SIZE)).intValue();
              int correctclassifiedpercentage= (int)((correct*1.0/size*1.0)*100);
              int misclassifiedpercentage= (int)((misclassified*1.0/size*1.0)*100);
              text=text.concat("\n  "+classifiername+" ["+classifierType+"] classified "+(correct)+" out of "+size+" validation-examples correctly ("+(correctclassifiedpercentage)+"%). "+misclassified+" misclassified ("+(misclassifiedpercentage)+"%).");
           }
           info.setOutputText(text);
           return info;
  }

 public void makeClean() {setStatus(UNTRAINED);}

    @Override
    public Object getDefaultValueForParameter(String parameterName) {
        for (Parameter par:parameters) {
            if (parameterName.equals(par.getName())) return par.getDefaultValue();
        }
        return null;
    }

    @Override
    public Parameter[] getParameters() {
        Parameter[] list=new Parameter[parameters.size()];
        return parameters.toArray(list);
    }

    @Override
    public Parameter getParameterWithName(String parameterName) {
        for (Parameter parameter:parameters) {
            if (parameter.getName().equals(parameterName)) return parameter;
        }
        return null;
    }


    /**
     * Adds a regular parameter to the Classifier. This is used for initialization of Classifier objects and should only be called in a constructor or similar setup method
     */
    protected void addParameter(String parameterName, Class type ,Object defaultValue, Object[] allowedValues, String description, boolean required, boolean hidden) {
        parameters.add(new Parameter(parameterName,type,defaultValue,allowedValues,description,required,hidden));
    }

    @Override
    public String toString() {
        return getName();
    }

    /** Returns a String describing the type of classifier represented by this object. E.g. "Neural Network" or "AdaBoost" */
    public abstract String getClassifierType();



    public static String[] getAvailableClassifierTypes() {
        return new String[]{"Neural Network","Naive Bayes","Decision Tree"};
    }

    public static Classifier getNewClassifier(String classifierType) {
        if (classifierType.equals("Neural Network")) return new FeedForwardNeuralNet();
        else if (classifierType.equals("Naive Bayes")) return new NaiveBayesClassifier();
        else if (classifierType.equals("Decision Tree")) return new DecisionTreeClassifier();
        else return null;
    }


    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }

}
