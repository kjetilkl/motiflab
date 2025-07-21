/*
 
 
 */

package org.motiflab.engine.data.classifier.bayes;

import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.classifier.Classifier;
import org.motiflab.engine.data.classifier.ClassifierDataSet;
import org.motiflab.engine.data.classifier.ClassifierVisualizer;
import org.motiflab.engine.data.classifier.Example;
import org.motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class NaiveBayesClassifier extends Classifier {



    /* The Naive Bayes Classifier finds the most likely hypothesis (maximum-likelihood hypothesis)
     * for an example instance x. That is, it finds the highest P(C|x) by maximizing the posterior probability P(x|c)*P(c) of
     * seeing the data, assuming class c, multiplied by the prior probability of observing class c.
     * (since P(c|x)=P(x|c)*P(c) from Bayes rule)
     */

    /* Lookup table for P(Xi|C) - the class-conditional data-probabilities
     * It is a nested datastructure initially indexed with classname (C)
     * the hashmap returns an arraylist which should then be indexed by attribute-index i
     * and this arrayList.get(i) should return a probability-distribution for values in the form
     * of a Hashmap<String,Double> indexed on the attribute value (a string) and returning
     * a probability assignment for this attribute value (conditioned on classname) in the form of a Double
     */
    private HashMap<String, ArrayList<HashMap>> conditionaldataprobabilities;

    // lookup table for P(C) - the a priory class probabilities
    private HashMap<String, Double> priorclassprobabilities; // indexed on classname
    private ArrayList<String> classnames; // the classnames as obtained from the dataset

    private int numberofbinsfornumericattributes=20;
    private ArrayList<String> binscategories=null;
    private double priorcount=1.0f; // this is do ensure that no attribute probabilities are 0 (which can cause problems since a probability of 0 will always dominate in a product)

    @Override
    public String getClassifierType() {return "Naive Bayes";}

    /** Creates a new instance of NaiveBayesClassifier */
    public NaiveBayesClassifier() {
         setName("NaiveBayes");
         addParameter("Number of discrete bins",Integer.class,numberofbinsfornumericattributes,new Integer[]{1,10000},"",true,false);
         addParameter("Pseudo prior",Double.class,priorcount, new Double[]{0.0,1000.0},"ensures that no attribute probabilities are 0 (which can cause problems since a probability of 0 will always dominate in a product)",true,false);
         refactorBinsCategories();
    }


    public void setPseudoPrior(double prior) {
        priorcount=prior;
    }



    public void setNumberOfBinsForNumericAttributes(int value) {
        numberofbinsfornumericattributes=value;
        refactorBinsCategories();
    }
    public void setNumberOfBinsForNumericAttributes(Integer value) {
        numberofbinsfornumericattributes=value.intValue();
        refactorBinsCategories();
    }

    @Override
    public void train(ClassifierDataSet trainingSet, ClassifierDataSet validationSet, OperationTask task) throws InterruptedException{
        this.classnames=trainingSet.getClasses();
        priorclassprobabilities=new HashMap<String,Double>(); // P(C)
        conditionaldataprobabilities=new HashMap<String, ArrayList<HashMap>>(); // P(X|C)
        ArrayList<Example> examples=trainingSet.getTrainingExamples(getSamplingStrategy());
        HashMap<String, ArrayList<Example>> groups = new HashMap<String, ArrayList<Example>>(); // for separation of examples into groups
        //System.out.println("TrainingSet has "+examples.size()+" examples with "+classnames.size()+" class categories");
         // Cluster the examples into smaller groups based on their assigned classes
        int c=0;
         for (Example example : examples) { // go through each example in turn...
            c++;
            if (c%100==0) {if (shouldAbort() || Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();}
            String classname=example.getClassification();
            if (!groups.containsKey(classname)) groups.put(classname,new ArrayList<Example>());
            groups.get(classname).add(example); // add example to group
         }
         //for (String classlabel:groups.keySet()) System.out.println("After grouping: "+classlabel+" has "+groups.get(classlabel).size()+ " examples");
         // for each group, calculate the proper conditional probability distributions and store them
         // also, count the number of examples in each group and calculate the prior probabilities of each class on this basis
         for (String classname:classnames) {
           if (groups.get(classname)==null) { // the sampling process has missed out on some classes, just assign some dummy probabilities for this case
               conditionaldataprobabilities.put(classname,assignDummyConditionalProbabilities(classname, trainingSet));
               priorclassprobabilities.put(classname,new Double(priorcount));
           }
           else {
              conditionaldataprobabilities.put(classname,calculateConditionalProbabilities(classname, groups.get(classname)));
              priorclassprobabilities.put(classname,new Double(calculatePriorClassProbabilities(groups,classname))); // store the number of instances of each class
           }
         }
         normalizeProbabilities(priorclassprobabilities); // normalize the group counts by dividing by the total number of examples. This will turn it into a probability distribution
         // training done, now test to see how well it performs on the training set after training
         HashMap<String,Object> results=validate(trainingSet,Classifier.TRAINING);
         //results.putAll(validateWeighted(trainingSet));
         if (validationSet!=null) results.putAll(validate(validationSet,Classifier.VALIDATION));
         if (c%100==0) {if (shouldAbort() || Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();}         
         notifyOutputListenersFinished(createOutputInformation(results,this.getName(),this.getClassifierType()));
         setStatus(TRAINED);
         //debugDump();
    } // end of method train()


    private double calculatePriorClassProbabilities(HashMap<String, ArrayList<Example>> groups, String classname) {
        return (double)groups.get(classname).size();
    }


    @SuppressWarnings("unchecked")
    private ArrayList<HashMap> assignDummyConditionalProbabilities(String classname, ClassifierDataSet dataset) {
        ArrayList<HashMap> conditionalprobabilities=new ArrayList();
        int size=dataset.getNumberOfAttributes(); //
        for (int i=0;i<size;i++) { // now do calculations for each attribute
            ArrayList<String> categories;
            if (dataset.getAttributeType(i)==Example.NUMERIC) categories=this.binscategories;
            else categories=(ArrayList<String>)dataset.getAttributeValues(i);
            HashMap<String, Double> probabilities=assignDummyConditionalProbabilityForAttribute(categories); // calculate conditional probabilites for the i-th attribute
            conditionalprobabilities.add(i,probabilities); // ... and add it to the "vector"
        }
        return conditionalprobabilities;
    }

    private HashMap<String, Double> assignDummyConditionalProbabilityForAttribute(ArrayList<String> categories) {
        HashMap<String,Double> probabilities=new HashMap<String,Double>();
        double numberofcategories=(double)categories.size();
        for (String attributevalue : categories) {
           probabilities.put(attributevalue,new Double(1.0/numberofcategories));
        }
        return probabilities;
    }





    /**
     * Calculates conditional probabilites of all attributes' values in dataset subcluster containing all examples of the same class
     */
    @SuppressWarnings("unchecked")
    private ArrayList<HashMap> calculateConditionalProbabilities(String classname, ArrayList<Example> dataset) {
        ArrayList<HashMap> conditionalprobabilities=new ArrayList();
        Example e=dataset.get(0);
        int size=e.getNumberOfAttributes(); // the example itself is not really relevant now, only the number of attributes
        for (int i=0;i<size;i++) { // now do calculations for each attribute
            ArrayList<String> categories;
            if (e.getAttributeType(i)==Example.NUMERIC) categories=this.binscategories;
            else categories=(ArrayList<String>)e.getAttributeValues(i);
            HashMap<String, Double> probabilities=calculateConditionalProbabilityForAttribute(i,dataset,categories); // calculate conditional probabilites for the i-th attribute
            conditionalprobabilities.add(i,probabilities); // ... and add it to the "vector"
        }
        return conditionalprobabilities;
    }

    /**
     * Calculates conditional probabilites of a single attribute with different values across all examples (with a specific class)
     * the categories ArrayList is a set containing all possible values (categories) for this attribute
     */
    private HashMap<String, Double> calculateConditionalProbabilityForAttribute(int attributeindex,ArrayList<Example> dataset,ArrayList<String> categories) {
        HashMap<String,Double> probabilities=new HashMap<String,Double>();
        // initialize every category count to priorcount/numberOfCategories to ensure that all attributevalues have non-null values in the distribution
        double weight = 1.0;
        if (useWeightedExamples()) weight = getLowestWeight(dataset);
        int numberofcategories=categories.size();
        for (String attributevalue : categories) {
           probabilities.put(attributevalue,new Double(priorcount/numberofcategories*weight));
           //System.err.println("Prior="+probabilities.get(attributevalue)+" numberofcategories="+numberofcategories);
        }
        for (Example example : dataset) { // go through each example in turn...
            String attributevalue=getProperValueOfAttribute(example,attributeindex);
            if (useWeightedExamples()) weight=example.getWeight(); else weight = 1.0;
            Double prob=probabilities.get(attributevalue).doubleValue();
            probabilities.put(attributevalue,new Double(((prob==null)?0:prob.doubleValue())+weight)); // increment by 1 (multiplied with example's weight)
        }
        normalizeProbabilities(probabilities); // turn the counts into a proper probability distribution
        return probabilities;
    }

    @Override
    public String classify(Example example) {
        HashMap<String,Double> classweights=new HashMap<String,Double>(); // lookup map: classname -> posterior class probability, these need not be normalized since we are only interested in which name corresponds to the highest value but not the value itself
        for (String classname : classnames) {
            // the class probability given the example is P(c|x)=P(x|c)*P(c)
            double classprobability=getConditionalProbabilityOfExampleGivenClass(example,classname)*getPriorClassProbability(classname);
            classweights.put(classname,new Double(classprobability)); // enter into lookuptable
        }
        return getBestScoringClass(classweights); // return name of the highest scoring class in the table
    }

    @Override
    public double scoreExample(Example example) {
        return (double)(getConditionalProbabilityOfExampleGivenClass(example,CLASS_POSITIVE)*getPriorClassProbability(CLASS_POSITIVE));
    }
    
    /**
     * returns the probability of observing a given value 'datavalue' of attribute with index 'attributeindex'
     * given that the class is specified as 'classname'
     * P(x_i=datavalue|C)
     */
    @SuppressWarnings("unchecked")
    private double getProbabilityOfAttributeValueGivenClass(int attributeindex, String datavalue, String classname) {
        ArrayList<HashMap> attributes = conditionaldataprobabilities.get(classname); // get probabilities given this specific class
        HashMap<String,Double> probabilityDistribution = attributes.get(attributeindex); // get the distribution for this attribute (already conditioned on class)
        if (!probabilityDistribution.containsKey(datavalue)) {
            //System.err.println("Attribute ["+attributeindex+"] has no recorded probability for value="+datavalue+" and class="+classname+"!!"); 
            return 0.001; // just a small value?
        }
        else return probabilityDistribution.get(datavalue).doubleValue();
    }

    /*
     * Examples are given as ArrayLists of Strings: {"yes,"high","no","middle","old"}
     * Thus the attribute at index i has value: String s=list.get(i);
     * The total conditional probability is just the product of all the individual conditional
     * probabilities for each attribute value (since the naive bayes classifier assumes independence
     * of all attributes).
     *
     * returns: P(X|C) (that is product of all P(x_i|C))
     */
    private double getConditionalProbabilityOfExampleGivenClass(Example example, String classname) {
        double probability=1;
        for (int i=0;i<example.getNumberOfAttributes();i++) {
            String attributevalue=getProperValueOfAttribute(example,i); // returns a (possibly "pre-processed") value for this attribute
            probability*=getProbabilityOfAttributeValueGivenClass(i,attributevalue,classname); // multiply together probabilities for each attribute-value. This is fine since naive Bayes assumes attribute independence
        }
        return probability;
    }

    private double getPriorClassProbability(String classname){
        if (!priorclassprobabilities.containsKey(classname)) {System.err.println("Class ["+classname+"] has no prior probability!!"); return 0.0;}
        else return priorclassprobabilities.get(classname).doubleValue();
    }


    /*
     * When given a HashMap of names->values, returns the name corresponding to the highest value
     */
    private String getBestScoringClass (HashMap<String,Double> classes) {
        double maxvalue=Double.NEGATIVE_INFINITY;
        String mapclassname=null;
        for (String name : classes.keySet()) {
            double classvalue=classes.get(name).doubleValue();
            if (classvalue>maxvalue) {maxvalue=classvalue;mapclassname=name;}
        }
        return mapclassname;
    }



     /**
      * normalizes probabilities in a map of (name->probability) pairs by dividing each probability by the total
      * sum of probabilites
      */
     private void normalizeProbabilities(HashMap<String, Double> probabilities) {
         double normalizingconstant=0;
         for (String entry : probabilities.keySet()) normalizingconstant+=probabilities.get(entry).doubleValue();
         for (String category : probabilities.keySet()) {probabilities.put(category,probabilities.get(category).doubleValue()/normalizingconstant);} // divide entries by normalizingconstant
     }

     /** Gets a "usable value" for specified attribute of this example. Categorical values are returned as is. Numerical attributes are discritized into a number of bins with String names "0","1",..."N" */
     private String getProperValueOfAttribute(Example example, int attributeindex) {
         if (example.getAttributeType(attributeindex)==Example.UNKNOWN) {System.err.println("Attribute has unknown type in NaiveBayesClassifier.getProperValue(example,attributeindex)");}
         else if (example.getAttributeType(attributeindex)==Example.NUMERIC) return example.getCategoryBinnedValue(attributeindex,numberofbinsfornumericattributes);
         return example.getCategoricalValue(attributeindex);
     }


     private void refactorBinsCategories(){
        binscategories=new ArrayList<String>();
        for (int i=0;i<numberofbinsfornumericattributes;i++) binscategories.add(new String(""+i));
     }


  /** finds the lowest weight value among a set of examples */
  private double getLowestWeight(ArrayList<Example> dataset) {
      double lowest=Double.POSITIVE_INFINITY;
      for (Example example:dataset) {
          double weight=example.getWeight();
          if (weight<lowest) lowest=weight;
      }
      return lowest;
  }

     @SuppressWarnings("unchecked")
     private void debugDump() {
         System.out.println("Prior class probabilities\n-------------------------");
         for (String classlabel : priorclassprobabilities.keySet()) System.out.println("  "+classlabel+"=>"+priorclassprobabilities.get(classlabel));
         System.out.println("\nConditional probabilities\n-------------------------");
         for (String classlabel : conditionaldataprobabilities.keySet()) {
             System.out.println("  class="+classlabel);
             ArrayList<HashMap> condlist = conditionaldataprobabilities.get(classlabel);
             for (int i=0;i<condlist.size();i++) {
                 System.out.println("     Attribute=["+i+"]");
                 HashMap<String,Double> distr=condlist.get(i);
                 for (String val : distr.keySet()) System.out.println("        "+val+"=>"+distr.get(val));
             }
         }
         System.out.println("priorcount="+priorcount);
         System.out.println("================================================================");
     }

     public String getParametersDescription() {
         return "discrete bins="+this.numberofbinsfornumericattributes+", prior="+priorcount+", useWeightedExamples="+useWeightedExamples()+", sampling="+Classifier.SAMPLING_STRATEGY[getSamplingStrategy()];
     }


    @Override
    public void initializeFromParameters(ParameterSettings settings, MotifLabEngine engine) throws ExecutionError {
        int bins=0;
        double pseudoprior=0.1f;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              bins=(Integer)settings.getResolvedParameter("Number of discrete bins",defaults,engine);
              pseudoprior=(Double)settings.getResolvedParameter("Pseudo prior",defaults,engine);
           } catch (Exception ex) {
              throw new ExecutionError("An error occurred during classifier initialization", ex);
           }
        } else {
            bins=(Integer)getDefaultValueForParameter("Number of discrete bins");
            pseudoprior=(Double)getDefaultValueForParameter("Pseudo prior");
        }
        this.setNumberOfBinsForNumericAttributes(bins);
        this.setPseudoPrior(pseudoprior);
    }

    @Override
    public ClassifierVisualizer getVisualizer() {
         return new NaiveBayesVisualizer(this);
    }
}
