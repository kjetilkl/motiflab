/*
 * DataSet.java
 *
 * Created on 18. april 2006, 19:33
 *
 * To change this template, choose Tools | Template Manager
 
 */

package motiflab.engine.data.classifier;

import java.util.*;
import java.io.*;
import javax.swing.SwingUtilities;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.Region;
import motiflab.engine.data.Sequence;
import motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public class ClassifierDataSet implements Serializable {

    private ArrayList<Example> examples = new ArrayList<Example>();
    private HashMap<Example,Double> weights = new HashMap<Example,Double>(); // note that this requires the strictest definition of "equals" for Examples: 2 examples are only equal if they are in fact the same object
    private ArrayList<String> classes = new ArrayList<String>(); // a list of the different categories examples can be classified as
    private Object[] attributeValues=null; /* Specifies the "legal" (or at least the known) attribute values for each attribute.
                                            * It is a nested datastructure indexed on attribute number.
                                            * The 'Object' at position 'i' is either an ArrayList<String> of possible values
                                            * or a Double[] with 2 values specifying the minimum and maximum value for numeric values
                                            * (these min/max values are usually inferred from the examples).
                                            */
    private Double[] globalRange=null;      // specifies the minimum [0] and maximum [1] values taken over all numeric attributes in the dataset
    private ArrayList<String> attributeNames=new ArrayList<String>();   // the names of each attribute (feature) variable
    private String targetFeatureName="unknown_target";   // the name of the feature which will be predicted
    //private ClassifierDataSet parent=null; // child datasets inherit several properties from their "parents" except for the

    private boolean useCrossValidation=false;
    private int folds=0; // number of folds
    private int[] foldassignment=null; // this is used when cross-validation is employed. Each example is assigned to one of N folds
    private int positiveCounter=0; // used in fold assignments
    private int negativeCounter=0; // used in fold assignments
    private int validationfold=0; // the current fold used as validation set. All the other folds together make up the current training set
    private int[] foldsizes=null; // contains the sizes of each fold

    public static final int NONE=0;
    public static final int WEIGHTED=1;
    public static final int STOCHASTIC_UNIVERSAL_SAMPLING=2;

    public final static String FULL_SEQUENCE="Full sequence";
    public final static String TOTALLY_RANDOM="Random sampling";
    public final static String EQUAL_POSITIVE_AND_NEGATIVE="X positive, X negative";    
    public final static String EVENLY_SPACED="Evenly spaced";
    public final static String EVENLY_SPACED_WITHIN_CLASS="Evenly spaced within each class";
    public final static String ALL_POSITIVE_RANDOM_NEGATIVE="All positive, random negative";
    public final static String ALL_POSITIVE_EVENLY_SPACED_NEGATIVE="All positive, evenly spaced negative";
    public final static String MIDPOINT="Midpoint of each sequence";
    public final static String FROM_FILE="Import data from file";
    public final static String CROSSVALIDATION="N-fold crossvalidation";
    public final static String SUBSET_OF_TRAINING="Subset of training set";  

    /** Creates a new empty instance of ClassifierDataSet */
    public ClassifierDataSet() {
        examples=new ArrayList<Example>();
        weights = new HashMap<Example,Double>();
        globalRange=new Double[]{Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY};
    }

    /** Creates a new empty instance of ClassifierDataSet but with attribute names and allowed values copied from an existing dataset */
    @SuppressWarnings("unchecked")
    public ClassifierDataSet(ClassifierDataSet template) {
        examples=new ArrayList<Example>();
        weights = new HashMap<Example,Double>();
        globalRange=new Double[]{template.globalRange[0],template.globalRange[1]};
        this.classes=(ArrayList<String>)template.classes.clone();
        this.attributeNames=(ArrayList<String>)template.attributeNames.clone();
        this.targetFeatureName=template.targetFeatureName;
        this.attributeValues=new Object[template.attributeValues.length];
        for (int i=0;i<attributeValues.length;i++) {
            Object entry=template.attributeValues[i];
            if (entry instanceof Double[]) {
                Double[] resolved=(Double[])entry;
                this.attributeValues[i]=new Double[]{resolved[0],resolved[1]};
            } else if (entry instanceof ArrayList) {
                ArrayList<String> resolved=(ArrayList<String>)entry;
                this.attributeValues[i]=(ArrayList<String>)resolved.clone();
            } else System.err.println("Warning: unknown attribute range object: "+entry.getClass());
        }
    }


//    /** Creates a new instance of ClassifierDataSet based on an arraylist of examples */
//    public ClassifierDataSet(ArrayList<Example> examples) {
//        this.examples=examples;
//        int numberofattributes=examples.get(0).getNumberOfAttributes();
//        resetCategories(numberofattributes);
//        updateAttributeCategories(examples); // update allowable classes, attributevalues and global by going through each example
//        weights = new HashMap<Example,Double>();
//        resetWeights();
//    }
//
//    /** Creates a new instance of ClassifierDataSet based on an arraylist of examples and a HashMap of weights */
//    public ClassifierDataSet(ArrayList<Example> examples, HashMap<Example,Double> weights) {
//        this.examples=examples;
//        this.weights=weights;
//        int numberofattributes=examples.get(0).getNumberOfAttributes();
//        resetCategories(numberofattributes);
//        updateAttributeCategories(examples); // update allowable classes, attributevalues and global by going through each example
//    }

    /** Sets the name of the attributes */
    public void setAttributeNames(ArrayList<String> attributes) {
        if (attributes==null && attributeNames!=null) attributeNames.clear();
        else attributeNames=attributes;
    }
    /** Add an attribute name */
    public void addAttributeName(String attributeName) {
        if (attributeNames==null) attributeNames=new ArrayList<String>();
        else attributeNames.add(attributeName);
    }
    /** Sets the name of the target feature */
    public void setTargetFeatureName(String name) {
        targetFeatureName=name;
    }

    /** Sets the name of the classes that the target feature can have */
    public void setClassNames(ArrayList<String> newclasses) {
        if (newclasses==null && classes!=null) classes.clear();
        else classes=newclasses;
    }

    public boolean isKnownClass(String classname) {
        if (classes!=null && classes.contains(classname)) return true;
        else return false;
    }

    private void resetCategories(int numberofattributes, boolean resetclasses, boolean resetrange) {
//        if (parent!=null) parent.resetCategories(numberofattributes);
//        else {
          attributeValues=new Object[numberofattributes];
          if (resetclasses) classes = new ArrayList<String>();
          if (resetrange) globalRange=new Double[]{Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY};
       // }
    }


    /** Returns an ArrayList containing the names of possible classes for the examples */
    public ArrayList<String> getClasses() {
        // if (parent!=null) return parent.getClasses();
        return classes;
    }

    /** Returns the number of classes for the examples in this dataset */
    public int getNumberOfClasses() {
        //if (parent!=null) return parent.getNumberOfClasses();
        return classes.size();
    }

    /** Returns the number of attributes for the examples in this dataset */
    public int getNumberOfAttributes() {
        //if (parent!=null) return parent.getNumberOfAttributes();
        return attributeValues.length;
    }


    public void setWeights(HashMap<Example,Double> weights) {
        this.weights=weights;
    }

    public void setExamples(ArrayList<Example> examples) {
        this.examples=examples;
    }

    /** Clears all examples from this dataset
     *  but keeps the classes and attribute ranges
     */
    public void clearExamples() {
        this.examples.clear();
        this.weights.clear();
    }

    @SuppressWarnings("unchecked")
    public void removeDuplicateExamples() {
        //System.err.println("Before filtering duplicates: size="+examples.size());
        ArrayList<Example> newlist=(ArrayList<Example>)examples.clone();
        Collections.sort(newlist);
        for (int i=1;i<newlist.size();i++) {
            Example previousEx=newlist.get(i-1);
            Example thisEx=newlist.get(i);
            if (thisEx.isSimilar(previousEx)) removeExample(thisEx, false);
        }
        resetWeights();
        //System.err.println("After filtering duplicates: size="+examples.size());
    }

    /** Shuffles the order of the examples in this dataset */
    public void shuffle() {
        Collections.shuffle(examples);
    }

    /**
     * This method must be called (after all examples have been added)
     * in order to prepare this dataset for use in crossvalidation
     * It will distribute all the examples into different folds, trying to
     * distribute positive and negative examples evenly into all the folds
     * @param folds
     */
    public void setupCrossValidation(int folds) {
        this.folds=folds;
        foldassignment=new int[examples.size()];
        foldsizes=new int[folds];
        positiveCounter=0;
        negativeCounter=0;
        for (int i=0;i<examples.size();i++) {
            if (examples.get(i).getClassification().equals(Classifier.CLASS_POSITIVE)) {
                positiveCounter++;
                foldassignment[i]=positiveCounter%folds;
                foldsizes[positiveCounter%folds]++;
            }
            else {
                negativeCounter++;
                foldassignment[i]=negativeCounter%folds;
                foldsizes[negativeCounter%folds]++;
            }
        }
        useCrossValidation=true;
        setCurrentValidationFold(0);
    }

    /** Returns the number of folds in this cross validation */
    public int getNumberOfCrossValidationFolds() {
        return folds;
    }

    /** Sets the index of the fold which should currently be used as validation set
     *  It also resets the current weights for all examples
     */
    public void setCurrentValidationFold(int foldindex) {
        validationfold=foldindex;
        resetWeights();
    }

     /** Returns the size of the fold with the given index */
    public int getCrossValiationFoldSize(int foldindex) {
        return foldsizes[foldindex];
    }
     /** Returns the size of the current crossvalidation training set */
    public int getCrossValidationTrainingsetSize() {
        return examples.size()-foldsizes[validationfold];
    }

    /** Returns the size of the current crossvalidation validation set */
    public int getCrossValidationValidationsetSize() {
        return foldsizes[validationfold];
    }

    /** Call this method in order to stop using cross validation */
    public void stopUsingCrossValidation() {
        foldassignment=null;
        foldsizes=null;
        resetWeights();
    }


    public ArrayList<Example> getExamples() {
        return examples;
    }

    public HashMap<Example,Double> getExampleWeights() {
        return weights;
    }

    /** set weight of example ex to w */
    public void setWeight(Example ex, Double w) {
       weights.put(ex,w);
    }

    /** set weight of example ex to w */
    public void setWeight(Example ex, double w) {
       weights.put(ex,new Double(w));
    }

    /** set weight of example number i to w */
    public void setWeight(int i, double w) {
       Example ex=examples.get(i);
       weights.put(ex,new Double(w));
    }


    /**
     *  Reset all weights for examples to 1/N where N is the size of the full dataset.
     *  However, if crossvalidation is employed, the weights for training examples will be set
     *  to 1/M (where M is the size of trainingset) and validation set examples' weights to 0.
     */
    public void resetWeights() {
       if (useCrossValidation) {
           int N=examples.size();
           int M=getCrossValidationTrainingsetSize();
           for (int i=0;i<N;i++){
               Example ex=examples.get(i);
               if (foldassignment[i]==validationfold) weights.put(ex,new Double(0));
               else weights.put(ex,new Double(1.0/(double)M));
           }
       } else {
           double N=examples.size();
           for (int i=0;i<N;i++){
               Example ex=examples.get(i);
               weights.put(ex,new Double(1.0/(double)N));
           }
       }
    }



    /** Normalize weights so that they all sum to 1, but maintain relative magnitudes. The result will be a probability distribution */
    public void normalizeWeights() {
       Iterator i = weights.keySet().iterator();
       double sum=0;
       while(i.hasNext()) {
           Example ex=(Example)i.next();
           sum+=weights.get(ex);
       }
       i = weights.keySet().iterator();
       while(i.hasNext()) {
           Example ex=(Example)i.next();
           double exweight=weights.get(ex);
           weights.put(ex,new Double(exweight/sum)); // normalize every weight by dividing with the total sum
       }
    }



    /** Adds a new example to the ClassifierDataSet and recalculates the weights so that all are 1/N */
    public void addExample(Example example) {
       examples.add(example);
       weights.put(example,new Double(1));
       updateAttributeCategories(example);
       resetWeights();
    }


    /** Removes an example from the ClassifierDataSet and recalculates the weights so that all are 1/N */
    public void removeExample(Example example) {
       examples.remove(example);
       weights.remove(example);
       resetWeights();
    }

    /** Removes an example from the ClassifierDataSet and optionally recalculates the weights so that all are 1/N */
    public void removeExample(Example example, boolean resetweights) {
       examples.remove(example);
       weights.remove(example);
       if (resetweights) resetWeights();
    }



    /** Returns the size (number of examples) in this dataset*/
    public int size() {return examples.size();}

    /** Returns example number i in the (ordered) set */
    public Example getExample(int i) {return examples.get(i);}

    /** Returns the weight of example number i in the (ordered) set */
    public Double getWeight(Example ex) {return weights.get(ex);}

    public int getAttributeType(int attributeindex) {
       //if (parent!=null) return parent.getAttributeType(attributeindex);
       if (attributeValues[attributeindex].getClass()==ArrayList.class) return Example.CATEGORICAL;
       else return Example.NUMERIC;
    }


    /** Returns the name for the attribute at index=attributeindex
     */
    public String getAttributeName(int attributeindex) {
       //if (parent!=null) return parent.getAttributeName(attributeindex);
       if (attributeNames==null) return null;
       else return attributeNames.get(attributeindex);
    }

    /** Returns the names for the all the attributes in order
     */
    public ArrayList<String> getAttributeNames() {
       //if (parent!=null) return parent.getAttributeNames();
       if (attributeNames==null) return new ArrayList<String>(0);
       else return attributeNames;
    }

    /** Returns the index corresponding to the given attributeName
     * or -1 if no attribute with the given name exists
     */
    public int getIndexForAttributeName(String name) {
       //if (parent!=null) return parent.getIndexForAttributeName(name);
       return attributeNames.indexOf(name);
    }

    /** Returns the allowed values for the attribute at index=attributeindex
     *  If the attribute is numberic it returns a Double[] array where [0] specifies minimal value and [1] specifies the maximal value.
     *  If the attribute is categorical it returns an ArrayList which lists the possible values
     */
    public Object getAttributeValues(int attributeindex) {
       //if (parent!=null) return parent.getAttributeValues(attributeindex);
       if (attributeValues==null) return null;
       else return attributeValues[attributeindex];
    }
     /** Sets the allowed attributevalues for given attribute.
      * The values could either be an ArrayList of allowed values (for categorical attributes)
      * or a Double[] array where [0] specifies minimum value and [1] maximum value
      */
     public void setAttributeValues(int attributeindex, Object attributevalues) {
       //if (parent!=null) parent.setAttributeValues(attributeindex, attributevalues);
       attributeValues[attributeindex]=attributevalues;
    }

     /** Sets the allowed attributevalues for given  attribute (which should be numeric).
      * the first element of range [0] specifies minimum value and the second [1] the maximum value
      */
     public void setAttributeRange(int attributeindex, Double[] range) {
       //if (parent!=null) parent.setAttributeRange(attributeindex, range);
       attributeValues[attributeindex]=range;
    }

     /** Sets the global numeric range taken over all numeric attributes in the featurevector.
      * the first element of range [0] specifies minimum value and the second [1] the maximum value
      */
      public void setNumericRange(Double[] range) {
       //if (parent!=null) parent.setNumericRange(range);
       globalRange=range;
    }

    /** Returns the global numeric range taken over all numeric attributes in the featurevector.
      * the first element of range [0] specifies minimum value and the second [1] the maximum value
      */
      public Double[] getNumericRange() {
        //if (parent!=null) return parent.getNumericRange();
        return globalRange;
    }

    //public void setParent(ClassifierDataSet parent) {this.parent=parent;}

    /** Returns the name for the target feature
     */
    public String getTargetFeatureName() {
       //if (parent!=null) return parent.getTargetFeatureName();
       return targetFeatureName;
    }

  /** Adjusts the allowed numerical range for this attribute if the value given is outside the current range.
   *  Also adjusts the "global" extreme values if necessary.
   */
  private void adjustExtremeValues(int attributeindex, double value)  {
      //if (parent!=null) {parent.adjustExtremeValues(attributeindex,value); return;}
      if (attributeValues[attributeindex]!=null && attributeValues[attributeindex].getClass()!=Double[].class) {System.err.println("WARNING: Problems with attribute range in adjustExtremeValues(): object is = "+attributeValues[attributeindex]);return;}
      if (attributeValues[attributeindex]==null) attributeValues[attributeindex]=new Double[]{Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY};
      Double[] range=(Double[])attributeValues[attributeindex];
      if (value<range[0].doubleValue()) range[0]=new Double(value);
      if (value>range[1].doubleValue()) range[1]=new Double(value);
      if (value<globalRange[0].doubleValue()) globalRange[0]=new Double(value);
      if (value>globalRange[1].doubleValue()) globalRange[1]=new Double(value);
  }

  /** adds a new categoryclass for this attribute (if it is not already registered) */
  @SuppressWarnings("unchecked")
  private void addCategoryClass(int attributeindex, String classname)  {
      //if (parent!=null) {parent.addCategoryClass(attributeindex,classname); return;}
      if (attributeValues[attributeindex]!=null && attributeValues[attributeindex].getClass()!=ArrayList.class) {System.err.println("WARNING: Problems with attribute categories in addCategoryClass(): object is = "+attributeValues[attributeindex]);return;}
      if (attributeValues[attributeindex]==null) {attributeValues[attributeindex]=new ArrayList<String>();}
      else if (((ArrayList<String>)attributeValues[attributeindex]).contains(classname)) return;
      ((ArrayList<String>)attributeValues[attributeindex]).add(classname);
  }

  /** updates the dataset's possible classes, attribute category values and numeric ranges given this specific example */
  private void updateAttributeCategories(Example example) {
      //if (parent!=null) {parent.updateAttributeCategories(example); return;}
      if (attributeValues==null) resetCategories(example.getNumberOfAttributes(),(classes==null || classes.isEmpty()),true);
      String classname=example.getClassification();
      if (!classes.contains(classname)) classes.add(classname);
      for (int i=0;i<example.getNumberOfAttributes();i++) {
          Object value=example.getRawValue(i);
          if (value.getClass()==Double.class) adjustExtremeValues(i,((Double)value).doubleValue());
          else addCategoryClass(i,(String)value);
      }
  }

  /** updates the dataset's possible classes, attribute category values and numeric ranges given a list of examples */
  private void updateAttributeCategories(ArrayList<Example>examples) {
      //if (parent!=null) {parent.updateAttributeCategories(examples); return;}
      for (Example example:examples) updateAttributeCategories(example);
  }


  /** Writes a short 'debug' report to STDERR specifying the values of different parameters */
  public void debug(boolean verbose) {
      if (verbose) {
          System.err.println("DATASET :\n==========================================================");
          System.err.println("Number of examples in dataset = "+size());
          System.err.println("Number of category classes = "+classes.size()+", values = "+getDebugObjectValues(classes));
          System.err.println("Predicted feature = "+targetFeatureName);
          System.err.println("Number of attributes = "+attributeValues.length);
          for (int i=0;i<attributeValues.length;i++) {
              String attrtype=null;
              if (attributeValues[i].getClass()==ArrayList.class) attrtype="CATEGORICAL";
              else if (attributeValues[i].getClass()==Double[].class) attrtype="NUMERICAL";
              System.err.println("  Attribute["+i+"] name="+attributeNames.get(i)+" type="+attrtype+", values = "+getDebugObjectValues(attributeValues[i]));
          }
          System.err.println("\nGlobal numeric range = "+getDebugObjectValues(globalRange));
          System.err.println("---------------------------------");
      } else {
         System.err.println("DATASET:  attributeNames="+attributeNames+"  classes="+classes+"  target="+targetFeatureName);
      }
  }

  /** just a utility function for debug */
  private String getDebugObjectValues(Object val) {
      if (val==null) {return "IS NULL";}
      if (val.getClass()==ArrayList.class) {return ""+((ArrayList)val).toString()+"";}
      if (val.getClass()==Double[].class) {
          Double[] range=(Double[])val;
          return "{min="+range[0]+", max="+range[1]+"}";
      }
      return "Default";
  }

  /** Returns a list of all examples in this dataset that are assigned to the given class
   */
  public ArrayList<Example> getAllExamplesInClass(String classification) {
      ArrayList<Example> subsetExamples=new ArrayList<Example>();
       for (Example e:examples) {
          if (e.getClassification().equals(classification)) subsetExamples.add(e);
      }
      return subsetExamples;
  }

  /** Returns a new DataSet containing a 'fraction' of the examples from the "old" set (the examples are removed from the old set).
   *  The subset maintains the old dataset as its "parent" and as such inherits many properties from the parent dataset, such as lists of allowed classes and attributevalues
   */
  public ClassifierDataSet extractSubset(double fraction) {
      ClassifierDataSet subset=new ClassifierDataSet(this);
      //subset.setParent(this);
      ArrayList<Example> subsetExamples=new ArrayList<Example>();
      int numbertoremove=(int)(this.size()*fraction); //System.err.println("Initializing subset with "+numbertoremove+"/"+this.size()+" examples. Fraction="+fraction);
      for (int i=0;i<numbertoremove;i++) {
          int newsize=this.size();
          int randomExample=(int)(Math.random()*newsize);
          Example example=this.getExample(randomExample);
          this.removeExample(example);
          subsetExamples.add(example);
          example.setDataSet(subset);
      }
      subset.setExamples(subsetExamples);
      subset.resetWeights();
      //System.err.println("subset now got "+subset.size()+" examples. Parent got "+this.size());
      return subset;
  }

  /** Returns a new DataSet containing a 'fraction' of the examples from the "old" set (the examples are removed from the old set).
   *  The subset maintains the old dataset as its "parent" and as such inherits many properties from the parent dataset, such as lists of allowed classes and attributevalues
   *  With this method the number of examples from each class in the extracted set should be approximately equal
   */
  public ClassifierDataSet extractSubsetEqualFractions(int subsetsize) {
      if (subsetsize>=size()*0.8) subsetsize=size()/2; // if trying to extract more than 80% of the dataset, use 50% instead
      ClassifierDataSet subset=new ClassifierDataSet(this);
      //subset.setParent(this);
      ArrayList<Example> subsetExamples=new ArrayList<Example>();
      int numclass=this.getNumberOfClasses();
        
      int numineachclass=subsetsize/numclass; // number of examples to extract from each class, provided that more than enough are available
      HashMap<String,Integer> classSampleSize=new HashMap<String,Integer>(); // number of examples to sample from each class. No more than 50% of size of each class    
      int totalSampleSize=0; // number of examples to return in the extracted set (adjusted for small size)
      for (String classname:classes) {
          int classSize=this.getNumberOfExamplesInClass(classname);
          int samplesize=((classSize/2)>=numineachclass)?numineachclass:(classSize/2);
          classSampleSize.put(classname, samplesize);
          totalSampleSize+=samplesize;
      } 
      
      //System.err.println("DatasetSize="+size()+"   Number of examples in each class="+numineachclass+"   extract="+totalSampleSize);
      HashMap<String,Integer> picked=new HashMap<String, Integer>(numclass); // number of examples currently picked from each class
      for (String classname:classes) {
          picked.put(classname, 0);
      }
      Collections.shuffle(examples); // shuffle to randomize, then go through examples in order and pick out as many as needed from each class
      for (int i=0;i<examples.size();i++) {
          Example example=this.getExample(i);
          String classification=example.getClassification();
          int currently=picked.get(classification);
          if (currently==classSampleSize.get(classification)) continue; // do not pick more from this class
          picked.put(classification,(currently+1));
          subsetExamples.add(example);
          example.setDataSet(subset);
          if (subsetExamples.size()==totalSampleSize) break; // have sampled enough
      }
      for (Example x:subsetExamples) this.removeExample(x); // remove the subset from this dataset
      subset.setExamples(subsetExamples);
      subset.resetWeights();
      //System.err.println("subset now got "+subset.size()+" examples. Parent got "+this.size());
      return subset;
  }


  /** Returns the number of examples in this dataset with the given classification */
  public int getNumberOfExamplesInClass(String classification) {
      int result=0;
      for (Example e:examples) {
          if (e.getClassification().equals(classification)) result++;
      }
      return result;
  }

 public ArrayList<Example> getCrossValidationTrainingExamples() {
     ArrayList<Example> samples = new ArrayList<Example>(getCrossValidationTrainingsetSize());
     for (int i=0;i<examples.size();i++) {
         if (foldassignment[i]!=validationfold) samples.add(examples.get(i));
     }
     return samples;
 }

  public ArrayList<Example> getCrossValidationValidationExamples() {
     ArrayList<Example> samples = new ArrayList<Example>(getCrossValidationValidationsetSize());
     for (int i=0;i<examples.size();i++) {
         if (foldassignment[i]==validationfold) samples.add(examples.get(i));
     }
     return samples;
 }

 /** this can return either the full set of examples or a sampled list (with replacements) based on the current weights distribution and sampling strategy */
 public ArrayList<Example> getTrainingExamples(int samplingstrategy) {
  if (samplingstrategy==NONE) {
       if (useCrossValidation) return getCrossValidationTrainingExamples();
       else return examples; // returning full set here
  }
  else if (samplingstrategy==WEIGHTED) return sampleWeightedExamples();
  else if (samplingstrategy==STOCHASTIC_UNIVERSAL_SAMPLING) return sampleWeightedExamplesWithStochasticUniversalSampling();
  else return null;
}



  /** returns a list of training examples sampled with replacement from the entire set using the weights distribution and a Fitness Proportionale sampling stragegy */
  private ArrayList<Example> sampleWeightedExamples() {
   //   System.err.println("using sampled trainingset");
    int totalsize=this.examples.size();
    int setsize=(useCrossValidation)?getCrossValidationTrainingsetSize():totalsize;
    ArrayList<Example> samples = new ArrayList<Example>(setsize);
    //HashMap<Example,Integer> vis=new HashMap<Example,Integer>(); // for visualization of the sampling
    double[] wheel=new double[totalsize];
    // set up selectionwheel
    for (int i=0;i<totalsize;i++) {
       double w = getWeight(examples.get(i)).doubleValue(); // sum of all weights should be 1.0 since this is a proper distribution
       if (i==0) wheel[i]=w;
       else wheel[i]=wheel[i-1]+w;
    }
    wheel[totalsize-1]=1.0;
    // select individuals
    while(samples.size()<setsize) {
    double winner=Math.random(); // select a random number [0,1)
    int wheelpointer=0;
    while (wheel[wheelpointer]<winner) wheelpointer++; // find the index of thr example corresponding to segment of the wheel that contains the 'winner' random number
       Example example=examples.get(wheelpointer);
       if (getWeight(example)>0) samples.add(example); // just to be sure we don't sample examples with weight==0
      // if (vis.containsKey(example)) vis.put(example,new Integer(vis.get(example).intValue()+1)); else vis.put(example,new Integer(1));
    }
    // for (Example ex:vis.keySet()) printSample(vis.get(ex),ex.getWeight());

    return samples;
 }

  /** returns a list of training examples sampled with replacement from the entire set using the weights distribution and a Stochastic Universal Sampling stragegy */
  private ArrayList<Example> sampleWeightedExamplesWithStochasticUniversalSampling() {
    int totalsize=this.examples.size();
    int setsize=(useCrossValidation)?getCrossValidationTrainingsetSize():totalsize;
    ArrayList<Example> samples = new ArrayList<Example>(setsize);
    double[] wheel=new double[totalsize];
    // set up selectionwheel. Each example gets a small portion of the cumulative distribution
    for (int i=0;i<totalsize;i++) {
       double w = getWeight(examples.get(i)).doubleValue(); // sum of all weights should be 1.0 since this is a proper distribution
       if (i==0) wheel[i]=w;
       else wheel[i]=wheel[i-1]+w;
    }
    wheel[totalsize-1]=1.0;
    double[] selectors = new double[setsize]; // a set of roulette wheel pointers
    double stepsize=1.0/(setsize*1.0); // distance between selectors
    double spinoffset = Math.random(); // this spins the wheel
    for (int i=0;i<setsize;i++) { // set up selectors by assigning equally spaced values in [0,1] to each of them
       double v = spinoffset + stepsize*i;
       if (v>1) v-=1; // wrap around those that exceed the interval [0,1]
       selectors[i]=v;
    }
    Arrays.sort(selectors); // adjust so that selectors that 'wrapped around' comes in the right order
    // match selectors to wheel
    int wheelpointer=0;
    for (int i=0;i<setsize;i++) {
       while (wheel[wheelpointer]<selectors[i]) wheelpointer++; // find the index of the example corresponding to segment of the wheel that contains the next selector
       samples.add(examples.get(wheelpointer));
    }
    for (Example e:samples) {
        if (getWeight(e)>0) continue;
        else {
            System.err.println("Warning: Sampled example with weight==0 in StochasticUniversalSampling");
            break;
        }
    }
    return samples;
 }

  private void printSample(int times, double weight) {
      for (int i=0;i<times;i++) System.out.print("*");
      System.out.println(" "+weight);
  }

  /**
   * DatasetIterator should be used when crossvalidation is employed,
   * in order to iterate through training or validation examples
   */
  private class DatasetIterator implements Iterator<Example> {
        private int index=-1;
        private boolean useValidation=false;

        /**
         * @param useValidation If TRUE the Iterator will iterate through validation examples
         *                      If FALSE the Iterator will iterate through training examples
         */
        public DatasetIterator(boolean useValidation) {
            this.useValidation=useValidation;
        }
        @Override
        public boolean hasNext() {
            if (index<0) index=0;
            if (index>=examples.size()) return false;
            while(index<examples.size()) {
                if ((foldassignment[index]==validationfold && useValidation)
                    || (foldassignment[index]!=validationfold && !useValidation)) return true;
                index++;
            }
            return false;
        }

        @Override
        public Example next() {
            if (index<0 || index>=examples.size()) return null;
            else return examples.get(index);
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
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

    // --------------- static setup function to create datasets by sampling ------------

 
   /** This method sets up training and validation datasets (and returns them in an array) based on selected settings */
   public static ClassifierDataSet[] setupDatasets(String trainingSamplingStrategy, String trainingSubset, int trainingSetSize, boolean filterDuplicatesTraining, boolean filterDuplicatesValidation, String validationSamplingStrategy, String validationSubset, int validationSetSize, ArrayList<FeatureDataset> features, RegionDataset targetFeature, File trainingDatasetFile, File validationDatasetFile, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException, ExecutionError {
       ClassifierDataSet trainingDataset=null;
       ClassifierDataSet validationDataset=null;
       boolean visualizeSamples=false; // *** DEBUG ***: if this is set to TRUE, a NumericDataset will be constructed which "visualizes" the positions of sampled examples. This was used during programming/debugging and was set to false later, but it could also be included as a feature in the final program
       SequenceCollection trainingCollection=(SequenceCollection)engine.getDataItem(trainingSubset);
       SequenceCollection validationCollection=(SequenceCollection)engine.getDataItem(validationSubset);
       if (trainingSamplingStrategy.equals(MIDPOINT)) trainingSetSize=trainingCollection.getNumberofSequences();
       if (trainingSamplingStrategy.equals(FULL_SEQUENCE)) trainingSetSize=getCombinedSequenceLengths(trainingCollection,engine);
       if (validationSamplingStrategy.equals(MIDPOINT)) validationSetSize=validationCollection.getNumberofSequences();
       if (validationSamplingStrategy.equals(FULL_SEQUENCE)) validationSetSize=getCombinedSequenceLengths(validationCollection,engine);         
       int size=0;
       if (trainingSamplingStrategy.equals(FULL_SEQUENCE)) {
           int[] counts=getRegionAndBackgroundCounts(targetFeature, trainingCollection);
           trainingSetSize=counts[0]+counts[1];
           size+=trainingSetSize;
       } else if (trainingSamplingStrategy.equals(ALL_POSITIVE_RANDOM_NEGATIVE) || trainingSamplingStrategy.equals(ALL_POSITIVE_EVENLY_SPACED_NEGATIVE)) {
           int[] counts=getRegionAndBackgroundCounts(targetFeature, trainingCollection);
           if (trainingSetSize==0) trainingSetSize=counts[0];
           if (trainingSetSize>counts[1]) trainingSetSize=counts[1];
           size+=trainingSetSize+counts[0];
       } else if (trainingSamplingStrategy.equals(MIDPOINT)) {
           trainingSetSize=trainingCollection.getNumberofSequences();
           size+=trainingSetSize;
       } else size+=trainingSetSize;
       int halfway=size;
       if (validationSamplingStrategy.equals(FULL_SEQUENCE)) {
           int[] counts=getRegionAndBackgroundCounts(targetFeature, validationCollection);
           validationSetSize=counts[0]+counts[1];
           size+=validationSetSize;
       } else if (validationSamplingStrategy.equals(ALL_POSITIVE_RANDOM_NEGATIVE) || validationSamplingStrategy.equals(ALL_POSITIVE_EVENLY_SPACED_NEGATIVE)) {
           int[] counts=getRegionAndBackgroundCounts(targetFeature, validationCollection);
           if (validationSetSize==0) validationSetSize=counts[0];
           if (validationSetSize>counts[1]) validationSetSize=counts[1];
           size+=validationSetSize+counts[0];
       } else if (validationSamplingStrategy.equals(MIDPOINT)) {
           validationSetSize=validationCollection.getNumberofSequences();
           size+=validationSetSize;
       } else size+=validationSetSize;
       if (progressbar!=null) {
           progressbar.setMinimum(0);
           progressbar.setMaximum(size);
       }
            if (trainingSamplingStrategy.equals(FULL_SEQUENCE)) trainingDataset=sampleFullSequence(features, targetFeature, trainingCollection, 0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(MIDPOINT)) trainingDataset=sampleMidpoint(features, targetFeature, trainingCollection, 0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(TOTALLY_RANDOM)) trainingDataset=sampleRandom(features, targetFeature, trainingCollection, trainingSetSize, 0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(EQUAL_POSITIVE_AND_NEGATIVE)) trainingDataset=sampleRandomEqualDistribution(features, targetFeature, trainingCollection, trainingSetSize, 0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(EVENLY_SPACED)) trainingDataset=sampleEvenSpaced(features, targetFeature, trainingCollection, trainingSetSize, 0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(EVENLY_SPACED_WITHIN_CLASS)) trainingDataset=sampleEvenSpacedWithinClass(features, targetFeature, trainingCollection, trainingSetSize, 0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(ALL_POSITIVE_RANDOM_NEGATIVE)) trainingDataset=sampleAllPositiveRandomNegative(features, targetFeature, trainingCollection, trainingSetSize,0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(ALL_POSITIVE_EVENLY_SPACED_NEGATIVE)) trainingDataset=sampleAllPositiveEvenSpacedNegative(features, targetFeature, trainingCollection, trainingSetSize, 0, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (trainingSamplingStrategy.equals(FROM_FILE)) {
           try {trainingDataset=importDataFromFile(trainingDatasetFile,features, targetFeature, isInterrupted, progressbar);} catch (InterruptedException ie) {throw ie;} catch (ExecutionError e) {throw e;}
       }
            if (validationSamplingStrategy.equals(FULL_SEQUENCE)) validationDataset=sampleFullSequence(features, targetFeature, validationCollection, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(MIDPOINT)) validationDataset=sampleMidpoint(features, targetFeature, validationCollection, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(TOTALLY_RANDOM)) validationDataset=sampleRandom(features, targetFeature, validationCollection, validationSetSize, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(EQUAL_POSITIVE_AND_NEGATIVE)) validationDataset=sampleRandomEqualDistribution(features, targetFeature, validationCollection, validationSetSize, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(EVENLY_SPACED)) validationDataset=sampleEvenSpaced(features, targetFeature, validationCollection, validationSetSize, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(EVENLY_SPACED_WITHIN_CLASS)) validationDataset=sampleEvenSpacedWithinClass(features, targetFeature, validationCollection, validationSetSize, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(ALL_POSITIVE_RANDOM_NEGATIVE)) validationDataset=sampleAllPositiveRandomNegative(features, targetFeature, validationCollection, validationSetSize, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(ALL_POSITIVE_EVENLY_SPACED_NEGATIVE)) validationDataset=sampleAllPositiveEvenSpacedNegative(features, targetFeature, validationCollection, validationSetSize, halfway, visualizeSamples, isInterrupted, progressbar, engine, task);
       else if (validationSamplingStrategy.equals(SUBSET_OF_TRAINING) && trainingDataset!=null) {
           if (trainingDataset.size()==0) throw new ExecutionError("No samples in training set");
           validationDataset=trainingDataset.extractSubsetEqualFractions(validationSetSize);
       }
       else if (validationSamplingStrategy.equals(FROM_FILE)) {
            try {validationDataset=importDataFromFile(validationDatasetFile,features, targetFeature, isInterrupted, progressbar);} catch (InterruptedException ie) {throw ie;} catch (ExecutionError e) {throw e;}
       }
       if (filterDuplicatesTraining && trainingDataset!=null) trainingDataset.removeDuplicateExamples();
       if (filterDuplicatesValidation && validationDataset!=null) validationDataset.removeDuplicateExamples();

       if (trainingDataset!=null) setDatasetProperties(trainingDataset, features, targetFeature.getName());
       if (validationDataset!=null) setDatasetProperties(validationDataset, features, targetFeature.getName());
       return new ClassifierDataSet[]{trainingDataset,validationDataset};
   }


   /** NB NB NB NB NB NB NB ----- THIS IMPLEMENTATION SAMPLES WITH REPLACEMENT, ALSO smaller sequences have an equal change of being picked as larger sequences! */
   private static ClassifierDataSet sampleRandom(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int samples, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {
       ClassifierDataSet dataset=new ClassifierDataSet();
       HashMap<String,Integer> sequencelengths=getSequenceLengths(collection,engine);
       int numberOfSequences=collection.getNumberofSequences();
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null;  // * * * * * DEBUG
       for (int i=0;i<samples;i++) {
            if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
            if (i%200==0) Thread.yield();
            if (i%100==0) {setProgress(i+progressOffset,progressbar);}
            int randomSequenceIndex=(int)(Math.random()*numberOfSequences);
            String randomSequenceName=collection.getSequenceNameByIndex(randomSequenceIndex);
            int randomPosition=(int)(Math.random()*sequencelengths.get(randomSequenceName));
            Object[] featurevalues=new Object[features.size()];
            for (int j=0;j<features.size();j++) {
                featurevalues[j]=getFeatureValueforPosition(features.get(j), randomSequenceName, randomPosition);
            }
            Example example=new Example(featurevalues);
            example.setDataSet(dataset);
            String classification=getClassificationForPosition(targetFeature, randomSequenceName, randomPosition);
            example.setClassification(classification);
            dataset.addExample(example);
            if (visualizeSamples) {
                NumericSequenceData visualiSeq=(NumericSequenceData)visual.getSequenceByName(randomSequenceName); // * * * * * DEBUG
                visualiSeq.setValueAtRelativePosition(randomPosition, 1); // * * * * * DEBUG
            }
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }
   
   /** NOTE: This method is not properly implemented yet, it is just a copy of the method above!!!! */
   private static ClassifierDataSet sampleRandomEqualDistribution(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int samples, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {
       ClassifierDataSet dataset=new ClassifierDataSet();
       HashMap<String,Integer> sequencelengths=getSequenceLengths(collection,engine);
       int numberOfSequences=collection.getNumberofSequences();
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null;  // * * * * * DEBUG
       for (int i=0;i<samples;i++) {
            if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
            if (i%200==0) Thread.yield();
            if (i%100==0) {setProgress(i+progressOffset,progressbar);}
            int randomSequenceIndex=(int)(Math.random()*numberOfSequences);
            String randomSequenceName=collection.getSequenceNameByIndex(randomSequenceIndex);
            int randomPosition=(int)(Math.random()*sequencelengths.get(randomSequenceName));
            Object[] featurevalues=new Object[features.size()];
            for (int j=0;j<features.size();j++) {
                featurevalues[j]=getFeatureValueforPosition(features.get(j), randomSequenceName, randomPosition);
            }
            Example example=new Example(featurevalues);
            example.setDataSet(dataset);
            String classification=getClassificationForPosition(targetFeature, randomSequenceName, randomPosition);
            example.setClassification(classification);
            dataset.addExample(example);
            if (visualizeSamples) {
                NumericSequenceData visualiSeq=(NumericSequenceData)visual.getSequenceByName(randomSequenceName); // * * * * * DEBUG
                visualiSeq.setValueAtRelativePosition(randomPosition, 1); // * * * * * DEBUG
            }
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }
   
   private static ClassifierDataSet sampleEvenSpaced(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int samples, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {
       ClassifierDataSet dataset=new ClassifierDataSet();
       int numberOfSequences=collection.getNumberofSequences();
       int totalLength=getCombinedSequenceLengths(collection,engine);
       double spacing=(double)totalLength/(double)samples;
       int count=0;
       double offset=0;
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null; // * * * * * DEBUG
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           Sequence seq=collection.getSequenceByName(sequenceName, engine);
           NumericSequenceData visualiSeq=(visualizeSamples)?(NumericSequenceData)visual.getSequenceByName(sequenceName):null; // * * * * * DEBUG
           int seqlength=seq.getSize();
           for (double i=offset;i<seqlength;i+=spacing) {
                count++;
                if (count>samples) break;
                if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
                if (count%200==0) Thread.yield();
                if (count%100==0) {setProgress(count+progressOffset,progressbar);}
                Object[] featurevalues=new Object[features.size()];
                for (int j=0;j<features.size();j++) {
                    featurevalues[j]=getFeatureValueforPosition(features.get(j), sequenceName, (int)i);
                }
                Example example=new Example(featurevalues);
                example.setDataSet(dataset);
                String classification=getClassificationForPosition(targetFeature, sequenceName, (int)i);
                example.setClassification(classification);
                dataset.addExample(example);
                if (visualizeSamples) visualiSeq.setValueAtRelativePosition((int)i, 1); // * * * * * DEBUG
           }
           offset=seqlength%spacing;
           if (count>samples) break;
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }


   private static ClassifierDataSet sampleEvenSpacedWithinClass(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int samples, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {       
       ClassifierDataSet dataset=new ClassifierDataSet();
       int numberOfSequences=collection.getNumberofSequences();
       int insideCount=0;
       int outsideCount=0;
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           RegionSequenceData targetSequence=(RegionSequenceData)targetFeature.getSequenceByName(sequenceName);
           int regionspan=targetSequence.getNumberOfBasesSpannedByRegions();
           int size=targetSequence.getSize();
           insideCount+=regionspan;
           outsideCount+=(size-regionspan);
       }
       int smallest=(insideCount<outsideCount)?insideCount:outsideCount;
       if (smallest<samples/2) samples=2*smallest;
       double step=0;
       double insidestep=(double)insideCount/((double)samples/2f);
       double outsidestep=(double)outsideCount/((double)samples/2f);
       int count=0;
       double insideoffset=0;
       double outsideoffset=0;
       int side=1; // side=1 inside regions, side=0 outside regions;
       int endstretch=0;
       int stretchindex=0;
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null;  // * * * * * DEBUG
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           Sequence seq=collection.getSequenceByName(sequenceName, engine);
           int seqlength=seq.getSize();
           NumericSequenceData visualiSeq=(visualizeSamples)?(NumericSequenceData)visual.getSequenceByName(sequenceName):null; // * * * * * DEBUG
           ArrayList<int[]> stretches=getRegionOverview((RegionSequenceData)targetFeature.getSequenceByName(sequenceName));
           stretchindex=0;
           int[] stretch=stretches.get(stretchindex);
           side=stretch[0];
           endstretch=stretch[1];
           double i=(side==1)?insideoffset:outsideoffset;
           while (i<seqlength) {
                count++;
                if (count>samples) break;
                if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
                if (count%200==0) Thread.yield();
                if (count%100==0) {setProgress(count+progressOffset,progressbar);}
                Object[] featurevalues=new Object[features.size()];
                for (int j=0;j<features.size();j++) {
                    featurevalues[j]=getFeatureValueforPosition(features.get(j), sequenceName, (int)i);
                }
                Example example=new Example(featurevalues);
                example.setDataSet(dataset);
                //String classification=getClassificationForPosition(targetFeature, sequenceName, (int)i);
                example.setClassification((side==1)?Classifier.CLASS_POSITIVE:Classifier.CLASS_NEGATIVE);
                dataset.addExample(example);
                if (side==1) step=insidestep; else step=outsidestep;
                if (visualizeSamples) visualiSeq.setValueAtRelativePosition((int)i, 1); // * * * * * DEBUG
                if (i+step>endstretch) { // next sample would be in a different stretch
                    if (side==1) { // currently we are inside regions, next will be outside (or in a different sequence)
                        insideoffset=(endstretch==0)?(i+step):(i+step)%(endstretch);
                        i=endstretch+1+outsideoffset;
                    } else { // currently we are outside regions, next will be inside
                        outsideoffset=(endstretch==0)?(i+step):(i+step)%(endstretch);
                        i=endstretch+1+insideoffset;
                    }
                    stretchindex++;
                    if (endstretch<seqlength-1) {
                        int[] nextstretch=stretches.get(stretchindex);
                        side=nextstretch[0];
                        endstretch=nextstretch[1];
                    }
                } else { // next step is within same stretch
                   i+=step;
                }
           }
           if (count>samples) break;
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }

   private static ClassifierDataSet sampleFullSequence(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {
       ClassifierDataSet dataset=new ClassifierDataSet();
       int numberOfSequences=collection.getNumberofSequences();
       int count=0;
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null;  // * * * * * DEBUG
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           NumericSequenceData visualiSeq=(visualizeSamples)?(NumericSequenceData)visual.getSequenceByName(sequenceName):null; // * * * * * DEBUG
           Sequence seq=collection.getSequenceByName(sequenceName, engine);
           int seqlength=seq.getSize();
           for (int i=0;i<seqlength;i++) {
                count++;
                if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
                if (count%200==0) Thread.yield();
                if (count%100==0) {setProgress(count+progressOffset,progressbar);}
                Object[] featurevalues=new Object[features.size()];
                for (int j=0;j<features.size();j++) {
                    featurevalues[j]=getFeatureValueforPosition(features.get(j), sequenceName, i);
                }
                Example example=new Example(featurevalues);
                example.setDataSet(dataset);
                String classification=getClassificationForPosition(targetFeature, sequenceName, i);
                example.setClassification(classification);
                dataset.addExample(example);
                if (visualizeSamples) visualiSeq.setValueAtRelativePosition(i, 1); // * * * * * DEBUG
           }
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }

   /** Samples here is negative samples! */
   private static ClassifierDataSet sampleAllPositiveRandomNegative(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int samples, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {
      ClassifierDataSet dataset=new ClassifierDataSet();
       int numberOfSequences=collection.getNumberofSequences();
       int insideCount=0;
       int outsideCount=0;
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           RegionSequenceData targetSequence=(RegionSequenceData)targetFeature.getSequenceByName(sequenceName);
           int regionspan=targetSequence.getNumberOfBasesSpannedByRegions();
           int size=targetSequence.getSize();
           insideCount+=regionspan;
           outsideCount+=(size-regionspan);
       }
       double probability=(double)samples/((double)outsideCount);
       int count=0;
       int side=1; // side=1 inside regions, side=0 outside regions;
       int endstretch=0;
       int stretchindex=0;
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null;  // * * * * * DEBUG
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           Sequence seq=collection.getSequenceByName(sequenceName, engine);
           int seqlength=seq.getSize();
           NumericSequenceData visualiSeq=(visualizeSamples)?(NumericSequenceData)visual.getSequenceByName(sequenceName):null; // * * * * * DEBUG
           ArrayList<int[]> stretches=getRegionOverview((RegionSequenceData)targetFeature.getSequenceByName(sequenceName));
           stretchindex=0;
           int[] stretch=stretches.get(stretchindex);
           side=stretch[0];
           endstretch=stretch[1];
           for (int i=0;i<seqlength;i++) {
                //if (count>samples) break;
                if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
                if (count%200==0) Thread.yield();
                if (count%100==0) {setProgress(count+progressOffset,progressbar);}
                double random=(double)Math.random();
                if (side==1 || random<probability) {
                    Object[] featurevalues=new Object[features.size()];
                    for (int j=0;j<features.size();j++) {
                        featurevalues[j]=getFeatureValueforPosition(features.get(j), sequenceName, (int)i);
                    }
                    Example example=new Example(featurevalues);
                    example.setDataSet(dataset);
                    //String classification=getClassificationForPosition(targetFeature, sequenceName, (int)i);
                    example.setClassification((side==1)?Classifier.CLASS_POSITIVE:Classifier.CLASS_NEGATIVE);
                    if (side==1 || count<samples) dataset.addExample(example);
                    if (side==0) count++;
                    if (visualizeSamples) visualiSeq.setValueAtRelativePosition((int)i, 1); // * * * * * DEBUG
                }
                if (i>=endstretch) { // next sample would be in a different stretch
                    stretchindex++;
                    if (endstretch<seqlength-1) {
                        int[] nextstretch=stretches.get(stretchindex);
                        side=nextstretch[0];
                        endstretch=nextstretch[1];
                    }
                }
           }
           //if (count>samples) break;
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }
   
   
   private static ClassifierDataSet sampleAllPositiveEvenSpacedNegative(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int samples, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {
       ClassifierDataSet dataset=new ClassifierDataSet();
       int numberOfSequences=collection.getNumberofSequences();
       int insideCount=0;
       int outsideCount=0;
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           RegionSequenceData targetSequence=(RegionSequenceData)targetFeature.getSequenceByName(sequenceName);
           int regionspan=targetSequence.getNumberOfBasesSpannedByRegions();
           int size=targetSequence.getSize();
           insideCount+=regionspan;
           outsideCount+=(size-regionspan);
       }
       double step=0;
       double outsidestep=(double)outsideCount/((double)samples);
       int count=0; // this method counts only outside samples
       double outsideoffset=0;
       int side=1; // side=1 inside regions, side=0 outside regions;
       int endstretch=0;
       int stretchindex=0;
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null;  // * * * * * DEBUG
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           Sequence seq=collection.getSequenceByName(sequenceName, engine);
           int seqlength=seq.getSize();
           NumericSequenceData visualiSeq=(visualizeSamples)?(NumericSequenceData)visual.getSequenceByName(sequenceName):null; // * * * * * DEBUG
           ArrayList<int[]> stretches=getRegionOverview((RegionSequenceData)targetFeature.getSequenceByName(sequenceName));
           stretchindex=0;
           int[] stretch=stretches.get(stretchindex);
           side=stretch[0];
           endstretch=stretch[1];
           double i=(side==1)?0:outsideoffset;
           while (i<seqlength) { // still inside sequence
                if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
                if (count%500==0) Thread.yield();
                if (count%100==0) {setProgress(count+progressOffset,progressbar);}
                Object[] featurevalues=new Object[features.size()];
                for (int j=0;j<features.size();j++) {
                    featurevalues[j]=getFeatureValueforPosition(features.get(j), sequenceName, (int)i);
                }
                Example example=new Example(featurevalues);
                example.setDataSet(dataset);
                //String classification=getClassificationForPosition(targetFeature, sequenceName, (int)i);
                example.setClassification((side==1)?Classifier.CLASS_POSITIVE:Classifier.CLASS_NEGATIVE);
                if (side==1 || count<samples) dataset.addExample(example);
                if (side==1) step=1; else {step=outsidestep;count++;} // step=1 inside regions
                if (visualizeSamples) visualiSeq.setValueAtRelativePosition((int)i, 1); // * * * * * DEBUG
                if (i+step>endstretch) { // next sample would be in a different stretch
                    if (side==1) { // currently we are inside regions, next will be outside (or in a different sequence)
                        i=endstretch+1+outsideoffset;
                    } else { // currently we are outside regions, next will be inside
                        outsideoffset=(endstretch==0)?(i+step):(i+step)%(endstretch);
                        i=endstretch+1;
                    }
                    stretchindex++;
                    if (endstretch<seqlength-1) {
                        int[] nextstretch=stretches.get(stretchindex);
                        side=nextstretch[0];
                        endstretch=nextstretch[1];
                    }
                } else { // next step is within same stretch
                   i+=step;
                }
           }
           //if (count>samples) break;
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }

   private static ClassifierDataSet sampleMidpoint(ArrayList<FeatureDataset> features, RegionDataset targetFeature, SequenceCollection collection, int progressOffset, boolean visualizeSamples, boolean[] isInterrupted, javax.swing.JProgressBar progressbar, MotifLabEngine engine, OperationTask task) throws InterruptedException {
       ClassifierDataSet dataset=new ClassifierDataSet();
       int numberOfSequences=collection.getNumberofSequences();
       NumericDataset visual=(visualizeSamples)?setupSampleVisualizingDataset(engine):null;  // * * * * * DEBUG
       for (int s=0;s<numberOfSequences;s++) {
            String sequenceName=collection.getSequenceNameByIndex(s);
            NumericSequenceData visualiSeq=(visualizeSamples)?(NumericSequenceData)visual.getSequenceByName(sequenceName):null; // * * * * * DEBUG
            Sequence seq=collection.getSequenceByName(sequenceName, engine);
            int seqlength=seq.getSize();
            int i=seqlength/2;
            if (isInterrupted[0] || Thread.interrupted() || (task!=null && task.isAborted())) {isInterrupted[0]=false;throw new InterruptedException();}
            if (s%5==0) Thread.yield();
            setProgress(s+progressOffset,progressbar);
            Object[] featurevalues=new Object[features.size()];
            for (int j=0;j<features.size();j++) {
                featurevalues[j]=getFeatureValueforPosition(features.get(j), sequenceName, i);
            }
            Example example=new Example(featurevalues);
            example.setDataSet(dataset);
            String classification=getClassificationForPosition(targetFeature, sequenceName, i);
            example.setClassification(classification);
            dataset.addExample(example);
            if (visualizeSamples) visualiSeq.setValueAtRelativePosition(i, 1); // * * * * * DEBUG
       }
       if (visualizeSamples) try {engine.storeDataItem(visual);} catch(ExecutionError e) {} // * * * * * DEBUG
       return dataset;
   }

   /** Sets the attributenames, targetfeature name and class categories in the dataset*/
   private static void setDatasetProperties(ClassifierDataSet dataset, ArrayList<FeatureDataset> features, String targetFeatureName) {
       ArrayList<String>featureNames=new ArrayList<String>(features.size());
       for (FeatureDataset d:features) featureNames.add(d.getName());
       ArrayList<String>classes=new ArrayList<String>(2);
       classes.add(Classifier.CLASS_NEGATIVE);
       classes.add(Classifier.CLASS_POSITIVE);
       dataset.setAttributeNames(featureNames);
       dataset.setTargetFeatureName(targetFeatureName);
       dataset.setClassNames(classes);
   }

   // This is called off-EDT!
   private static ClassifierDataSet importDataFromFile(File file, ArrayList<FeatureDataset> features, RegionDataset targetFeature, boolean[] isInterrupted, javax.swing.JProgressBar progressbar) throws InterruptedException, ExecutionError {
       if (file==null) return null;
       ClassifierDataSet dataset=new ClassifierDataSet();
       setDatasetProperties(dataset, features, targetFeature.getName());
       //boolean readAgainAndOverwrite=false;
       try {
           readARFFfile(file, dataset, false, isInterrupted, progressbar);
       } catch (IOException ioe) {
             throw new ExecutionError(ioe.getMessage());
       } catch (ExecutionError e) {
           String msg=e.getMessage();
           if (msg.startsWith("MISMATCH:")) {
               msg=msg.substring("MISMATCH:".length());
               msg="Import Error: The features found in the file do not match your current selections.\n\nProblem description:\n"+msg+"\n\n"+file.getAbsolutePath();
               throw new ExecutionError(msg);
           } else {
               //JOptionPane.showMessageDialog(this, "An error occurred while importing examples file:\n"+e.getMessage(),"Import Error (parsing)",JOptionPane.ERROR_MESSAGE);
               //return null;
               throw new ExecutionError(msg);
           }
       }
       String unknownClass=null;
       for (String cl:dataset.getClasses()) {
           if (!(cl.equals(Classifier.CLASS_POSITIVE) || cl.equals(Classifier.CLASS_NEGATIVE))) {unknownClass=cl;break;}
       }
       if (unknownClass!=null) {
           //JOptionPane.showMessageDialog(this, "An error occurred while importing examples file:\nUnknown class = '"+unknownClass+"'","Import Error (parsing)",JOptionPane.ERROR_MESSAGE);
           throw new ExecutionError("An error occurred while importing examples file:\nUnknown class = '"+unknownClass+"'");
           //return null;
       }
       return dataset;
   }

 /**
   * Fills this dataset with examples read from file in ARFF-format (appends if dataset is not empty)
   * If overwrite is FALSE and the attributes read from file do not match the current settings for attribute names, class names and target feature
   * an ExecutionError with a message starting with the keyword "MISMATCH:" will be thrown.
   * If overwrite is TRUE, however, the data read will overwrite the current settings in this dataset (which should be empty)
   * @param file The file containing the data
   * @param overwrite If set to TRUE the current settings in this dataset (attributes, classes etc) will be overwritten
   *                  If set to FALSE an exception will be thrown if the contents of the file does not match the current settings
   */
  public static void readARFFfile(File file, ClassifierDataSet dataset, boolean overwrite, boolean[] isInterrupted, javax.swing.JProgressBar progressbar) throws InterruptedException, IOException, ExecutionError {
    long filesize=file.length();
    if (progressbar!=null) {
        progressbar.setValue(0);
        progressbar.setMaximum(100);
    }
    BufferedReader inputStream=null;
    if (overwrite) {
        dataset.clearExamples();
        dataset.setAttributeNames(null);
        dataset.setClassNames(null);
        dataset.setTargetFeatureName(null);
    }
    int numAttr=dataset.getAttributeNames().size();
    boolean relationCorrect=false;
    boolean startedReadingData=false;
    int currentAttr=0;
    int attributeLUT[]=new int[numAttr]; // a LUT to resolve attribute indexes (which in the file might be different from the current dataset)
    try {
        InputStream stream=MotifLabEngine.getInputStreamForFile(file);
        inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
        String line;
        long count=0;
        long readsofar=0;
        while((line=inputStream.readLine())!=null) {
            readsofar+=line.length();
            count++;
            if (count%50==0) {
                if (isInterrupted[0]) {isInterrupted[0]=false;throw new InterruptedException();}
                if (progressbar!=null) setProgress((int)((double)readsofar/(double)filesize*100.0),progressbar);
                Thread.yield();
            }
            line=line.trim();
            if (line.isEmpty() || line.startsWith("%")) continue; // blank lines and comments
            if (line.startsWith("@")) {
                String[] fields=line.split("\\s+");
                if (fields[0].equalsIgnoreCase("@RELATION")) {
                   if (fields.length<2) throw new ExecutionError("Wrong format in @RELATION line: \n"+line);
                   if (overwrite || dataset.getTargetFeatureName()==null) dataset.setTargetFeatureName(fields[1]);
                   else if (dataset.getTargetFeatureName()!=null && !fields[1].equals(dataset.getTargetFeatureName())) throw new ExecutionError("MISMATCH:Current target feature = '"+dataset.getTargetFeatureName()+"'\nName of target feature in file = '"+fields[1]+"'");
                   relationCorrect=true;
                } else if (fields[0].equalsIgnoreCase("@ATTRIBUTE")) {
                    if (startedReadingData) throw new ExecutionError("New attributes can not be added after @DATA:\n"+line);
                    if (fields.length<3) throw new ExecutionError("Wrong format in @ATTRIBUTE line: \n"+line);
                    if (fields[1].equalsIgnoreCase("class")) { //
                        if (fields.length>3) {for (int k=3;k<fields.length;k++) fields[2]+=fields[k];} // spaces in classes
                        if (fields[2].startsWith("{") && fields[2].endsWith("}")) fields[2]=fields[2].substring(1,fields[2].length()-1);
                        String[] cl=fields[2].split("\\s*,\\s*");
                        ArrayList<String>detectedClasses=new ArrayList<String>(cl.length);
                        for (String c:cl) detectedClasses.add(c);
                        ArrayList<String>classes=dataset.getClasses();
                        if (classes==null || classes.isEmpty() || overwrite) dataset.setClassNames(detectedClasses);
                        else if (!stringListCompare(classes, detectedClasses)) throw new ExecutionError("MISMATCH:Class names found in file "+detectedClasses.toString()+" does not match known classes "+classes.toString());
                        continue;
                    } else if (!fields[2].equalsIgnoreCase("NUMERIC")) throw new ExecutionError("The classifier only accepts NUMERIC type data for attributes:\n"+line);
                    if (overwrite) {dataset.addAttributeName(fields[1]);}
                    else {
                        int aindex=dataset.getIndexForAttributeName(fields[1]);
                        if (aindex<0) throw new ExecutionError("MISMATCH:Attribute '"+fields[1]+"' encountered in file does not match any selected features. (line "+count+")");
                        attributeLUT[currentAttr]=aindex;
                    }
                    currentAttr++;
                } else if (fields[0].equalsIgnoreCase("@DATA")) {
                    if (overwrite) numAttr=currentAttr;
                    if (!relationCorrect) throw new ExecutionError("@DATA encountered before @RELATION at line "+count);
                    if (currentAttr<numAttr) throw new ExecutionError("MISMATCH:Missing @ATTRIBUTE specification for all required attributes. Expected "+numAttr+" found "+currentAttr);
                    startedReadingData=true;
                } else throw new ExecutionError("Unknown header: "+fields[0]);
            } else {// expecting dataline
                if (!startedReadingData) throw new ExecutionError("Missing @DATA header before data at line "+count);
                String[] fields=line.split("\\s*,\\s*");
                if (fields.length!=(numAttr+1)) throw new ExecutionError("Expected "+numAttr+" values per line, but encountered "+fields.length+" at line "+count);
                Example example=new Example(numAttr);
                example.setDataSet(dataset);

                for (int j=0;j<fields.length-1;j++) {
                    double value=0;
                    try {value=Double.parseDouble(fields[j]);} catch (NumberFormatException nfe) {throw new ExecutionError("Unable to parse expected numeric value at line "+count+": "+fields[j]);}
                    int attrIndex=(overwrite)?j:attributeLUT[j];
                    example.setValue(attrIndex, new Double(value));
                }
                String cl=fields[fields.length-1];
                if (!dataset.isKnownClass(cl)) throw new ExecutionError("Unknown classification '"+cl+"' for example encountered at line "+count+".\nKnown classes = "+dataset.getClasses().toString());
                example.setClassification(cl);
                dataset.addExample(example);
            }
        }
        if (!relationCorrect) throw new ExecutionError("Missing @RELATION header in file");
        if (currentAttr<numAttr) throw new ExecutionError("Missing @ATTRIBUTES in file. Expected "+numAttr+" found "+currentAttr);
        if (!startedReadingData) throw new ExecutionError("Missing @DATA header in file");
    } catch (IOException e) {
        throw e;
    } finally {
        try {
            if (inputStream!=null) inputStream.close();
        } catch (IOException ioe) {}
    }
  }


  /** Returns true if the lists contains the same elements but not necessarily in same order*/
 private static boolean stringListCompare(ArrayList<String>list1, ArrayList<String>list2) {
     if (list1.size()!=list2.size()) return false;
     for (String s:list1) if (!list2.contains(s)) return false;
     return true;
 }

  /**
   * Fills this dataset with examples read from file in ARFF-format (appends if dataset is not empty)
   * The file should be in CSV-format with values (either numeric or alphanumeric) for each attribute separated
   * by a specified delimiter. The last value on each line is interpreted as the class category of the example
   */
  public static void writeARFFfile(File file, ClassifierDataSet dataset) throws IOException {
      PrintWriter outputStream=null;
      try {
          OutputStream stream=MotifLabEngine.getOutputStreamForFile(file);
          outputStream=new PrintWriter(stream);
          outputStream.println("@RELATION "+dataset.getTargetFeatureName()+"\n");
          for (String attributename:dataset.getAttributeNames()) {
              outputStream.println("@ATTRIBUTE "+attributename+" NUMERIC");
          }
          String classString=dataset.getClasses().toString();
          classString=classString.replace("[", "{");
          classString=classString.replace("]", "}");
          outputStream.println("@ATTRIBUTE class "+classString);
          outputStream.println("\n@DATA");
          int n=dataset.getNumberOfAttributes();
          for (Example example:dataset.getExamples()) {
              for (int i=0;i<n;i++) {
                  int type=example.getAttributeType(i);
                  if (type==Example.NUMERIC) outputStream.print(example.getNumericValue(i)+",");
                  else if (type==Example.CATEGORICAL) outputStream.print(example.getCategoricalValue(i)+",");
              }
              outputStream.println(example.getClassification());
          }
       } catch (IOException e) {
        throw e;
       } finally {
        try {if (outputStream!=null) outputStream.close();} catch (Exception ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing PrintWriter in OutputData.saveToFile(): "+ioe.getMessage());}
       }
  }



   /**
    * Returns a "look-up-table" containing the length of each sequence indexed on sequence name
    * of the sequences in the given collection
    */
   private static HashMap<String,Integer> getSequenceLengths(SequenceCollection collection, MotifLabEngine engine) {
       HashMap<String,Integer> result=new HashMap<String,Integer>(collection.size());
       for (Sequence seq:collection.getAllSequences(engine)) {
           result.put(seq.getSequenceName(), seq.getSize());
       }
       return result;
   }

   /**
    * Returns the combined length of all the sequences in the given collection
    */
   private static int getCombinedSequenceLengths(SequenceCollection collection, MotifLabEngine engine) {
       int result=0;
       for (Sequence seq:collection.getAllSequences(engine)) {
           result+=seq.getSize();
       }
       return result;
   }

   /** Returns the number of bases inside and outside regions in the targe feature dataset */
   private static int[] getRegionAndBackgroundCounts(RegionDataset targetFeature, SequenceCollection collection) {
       int numberOfSequences=collection.getNumberofSequences();
       int insideCount=0;
       int outsideCount=0;
       for (int s=0;s<numberOfSequences;s++) {
           String sequenceName=collection.getSequenceNameByIndex(s);
           RegionSequenceData targetSequence=(RegionSequenceData)targetFeature.getSequenceByName(sequenceName);
           int regionspan=targetSequence.getNumberOfBasesSpannedByRegions();
           int size=targetSequence.getSize();
           insideCount+=regionspan;
           outsideCount+=(size-regionspan);
       }
       return new int[]{insideCount,outsideCount};
   }

   /** Creates a new "empty" NumericDataset can can be used to visualize sampling */
   private static NumericDataset setupSampleVisualizingDataset(MotifLabEngine engine) {
       NumericDataset visual=new NumericDataset("Samples");
       ArrayList<Sequence>sequences=engine.getDefaultSequenceCollection().getAllSequences(engine);
       for (Sequence seq:sequences) {
           NumericSequenceData visualSeq=new NumericSequenceData(seq,0);
           visual.addSequence(visualSeq);
       }
       return visual;
   }


   /** Returns a list of elements which describes the alternating nature of a Region sequence
    *  Each int[] in the list corresponds to a stretch of either Region or background.
    *  the first value ([0]) denotes type (0=background,1=region) while the second value ([1])
    *  states the end position (inclusive) for this stretch
    */
   private static ArrayList<int[]> getRegionOverview(RegionSequenceData seq) {
       ArrayList<int[]>result=new ArrayList<int[]>();
       int length=seq.getSize();
       ArrayList<Region> regions=seq.getCollapsedRegions();
       if (regions.isEmpty()) {
           result.add(new int[]{0,length-1});
           return result;
       }
       Region first=regions.get(0);
       if (first.getRelativeStart()>0) {
           result.add(new int[]{0,first.getRelativeStart()-1}); // start with outside
       }
       for (int i=0;i<regions.size()-1;i++) {
           result.add(new int[]{1,regions.get(i).getRelativeEnd()}); //
           result.add(new int[]{0,regions.get(i+1).getRelativeStart()-1}); 
       }
       // add last region and last outside
       Region last=regions.get(regions.size()-1);
       result.add(new int[]{1,last.getRelativeEnd()}); //
       if (last.getRelativeEnd()<length-1) {
           result.add(new int[]{0,length-1}); // end with outside
       }
       return result;
   }    
   


   private static Double getFeatureValueforPosition(FeatureDataset dataset, String sequenceName, int position) {
           if (dataset instanceof NumericDataset) {
               Double value=((NumericSequenceData)dataset.getSequenceByName(sequenceName)).getValueAtRelativePosition(position);
               return (value==null)?0:new Double(value.doubleValue());
           } else if (dataset instanceof RegionDataset) {
               int overlapping=((RegionSequenceData)dataset.getSequenceByName(sequenceName)).getNumberOfRegionsAtRelativePosition(position);
               if (overlapping>0) return new Double(1); else return new Double(0);
           } else return new Double(0);
   }

   private static String getClassificationForPosition(RegionDataset dataset, String sequenceName, int position) {
               int overlapping=((RegionSequenceData)dataset.getSequenceByName(sequenceName)).getNumberOfRegionsAtRelativePosition(position);
               if (overlapping>0) return Classifier.CLASS_POSITIVE; else return Classifier.CLASS_NEGATIVE;
   }   
   
  /** Updates the value of the progressbar on the EDT */
   private static void setProgress(final int value, final javax.swing.JProgressBar progressbar) {
       if (progressbar==null) return; 
       Runnable runner=new Runnable() {
            @Override
            public void run() {
                if (value<0) progressbar.setIndeterminate(true);
                else {
                    progressbar.setIndeterminate(false);
                    progressbar.setValue(value);
                }
                progressbar.repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
   }
    
}
