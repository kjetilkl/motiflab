/*
 * DecisionTreeClassifier.java
 *
 * Created on 21. april 2006, 21:26
 *
 * To change this template, choose Tools | Template Manager
 
 */
package motiflab.engine.data.classifier.decisiontree;

import java.util.*;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.classifier.Classifier;
import motiflab.engine.data.classifier.ClassifierDataSet;
import motiflab.engine.data.classifier.ClassifierOutput;
import motiflab.engine.data.classifier.ClassifierVisualizer;
import motiflab.engine.data.classifier.Example;
import motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class DecisionTreeClassifier extends Classifier {

     private int numberofbinsfornumericattributes=20;
     private ArrayList<String> binscategories=null;

     private DecisionTreeNode decisionTree=null;
     private boolean useBackWardPruning=true; // prune tree after training if true
     private boolean pruneUnusedSubtrees=false; // if true replace subtrees that are unused by validation set (pruning examples) with most common class in subtree
     private boolean replaceBasedOnValidationData=false; // if this is true then the leafnode replacing a pruned subtree has label equal to the most common class in validation examples classified by this subtree.
                                                        // if value is false the leafnode label is set to the most common label among leafnodes in the subtree (the latter strategy uses only information from training set)


    /** Creates a new instance of DecisionTreeClassifier */
    public DecisionTreeClassifier() {
         setName("DecisionTree");
         addParameter("Number of discrete bins",Integer.class,numberofbinsfornumericattributes,new Integer[]{1,10000},"",true,false);
         addParameter("Prune tree",Boolean.class,useBackWardPruning, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"",true,false);
         addParameter("Prune unused subtrees",Boolean.class,pruneUnusedSubtrees, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"",true,false);
         addParameter("Use validation data for pruning",Boolean.class,replaceBasedOnValidationData, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"",true,false);
         refactorBinsCategories();
    }

    @Override
    public String getClassifierType() {return "Decision Tree";}

    /** Returns the root of the decision tree */
    public DecisionTreeNode getRoot() {
        return decisionTree;
    }

    /** set pruning and pruning parameters (if prune=false, then the latter parameters have no function) */
    public void usePruning(boolean prune, boolean pruneUnusedSubtrees, boolean replaceBasedOnValidationData) {
        useBackWardPruning=prune;
        this.pruneUnusedSubtrees=pruneUnusedSubtrees;
        this.replaceBasedOnValidationData=replaceBasedOnValidationData;

    }

    /** set pruning */
    public void usePruning(boolean prune) {
        useBackWardPruning=prune;
    }
   /** set pruning */
    public void usePruning(Integer prune) {
        useBackWardPruning=(prune==1);
    }
   /** set pruning */
    public void usePruneUnusedSubtrees(Integer value) {
        pruneUnusedSubtrees=(value==1);
    }
   /** set pruning */
    public void usePruningBasedOnValidationData(Integer value) {
        replaceBasedOnValidationData=(value==1);
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
    public String classify(Example example) {
        if (decisionTree==null) return null;
        else return descendTree(decisionTree,example);
    }

    @Override
    public double scoreExample(Example example) {
        String result=classify(example);
        if (result.equals(CLASS_POSITIVE)) return 1; else return 0;
    }


    private String descendTree(DecisionTreeNode node, Example example) {
        if (node.isLeafNode()) return node.getClassLabel();
        else {
            int branchAttribute=node.getBranchAttribute();
            String exampleValue=getProperValueOfAttribute(example,branchAttribute);
            DecisionTreeNode subtree = node.getSubTreeForValue(exampleValue);
            return descendTree(subtree, example);
        }
    }


    @Override
    public void train(ClassifierDataSet trainingset, ClassifierDataSet validationSet, OperationTask task) throws InterruptedException {
       ArrayList<Example> examples = trainingset.getTrainingExamples(getSamplingStrategy());
       ArrayList<Integer> attributes=new ArrayList<Integer>();
       int numattributes=trainingset.getNumberOfAttributes();
       for (int i=0;i<numattributes;i++) {attributes.add(new Integer(i));}
       //
       if (shouldAbort() || Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
       decisionTree = constructTree(examples, attributes, null);
       notifyOutputListenersRound(new ClassifierOutput("  Finished construction of full tree with "+decisionTree.getNumberOfSubtreeNodes()+" nodes!"));
       HashMap<String,Object> results=validate(trainingset,Classifier.TRAINING);
       //results.putAll(validateWeighted(trainingset));
       if (validationSet!=null) results.putAll(validate(validationSet,Classifier.VALIDATION));
       results.put(Classifier.DECISION_TREE,decisionTree);
       notifyOutputListenersRound(createOutputInformation(results,this.getName(), this.getClassifierType()));
        
       if (this.useBackWardPruning && validationSet!=null) {
           pruneTree(validationSet);
           notifyOutputListenersRound(new ClassifierOutput("\n  Pruning tree down to "+decisionTree.getNumberOfSubtreeNodes()+" nodes with backward pruning"));
           results=validate(trainingset,Classifier.TRAINING);
           results.put(Classifier.DECISION_TREE,decisionTree);
           //results.putAll(validateWeighted(trainingset));
           results.putAll(validate(validationSet,Classifier.VALIDATION));
           notifyOutputListenersRound(createOutputInformation(results,this.getName(), this.getClassifierType()));
       }
       if (shouldAbort() || Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();       
       setStatus(TRAINED);
       notifyOutputListenersFinished(createOutputInformation(results,this.getName(), this.getClassifierType()));
       //decisionTree.visualize(System.out,"",0);
       //System.out.println("===================================================================================");
    } // end train()

    /**
     * constructs the decision tree recursively by splitting on different attributes
     */
private DecisionTreeNode constructTree(ArrayList<Example>examples, ArrayList<Integer>attributes, String defaultvalue) {
            if (examples.isEmpty())     return DecisionTreeNode.createLeafNode(defaultvalue);
       else if (isHomogenous(examples)) return DecisionTreeNode.createLeafNode(examples.get(0).getClassification()); // no need to split further since all examples have the same classlabel
       else if (attributes.isEmpty())   return DecisionTreeNode.createLeafNode(getMostCommonClasslabelInExamples(examples));
       else {
           int bestAttributeIndex = selectBestAttributeToSplitOn(examples, attributes); //
           DecisionTreeNode tree = DecisionTreeNode.createBranchNode(bestAttributeIndex);
           String majoritylabel = getMostCommonClasslabelInExamples(examples); // most common class for these examples
           ArrayList<String> allAttributeValues=getAllAttributeValues(bestAttributeIndex, examples.get(0));
           HashMap<String,ArrayList<Example>> valueClusters = clusterExamplesOnAttributeValue(examples,bestAttributeIndex);
           for (String value : allAttributeValues) {
               ArrayList<Example> subclusterExamples = valueClusters.get(value); // get those examples that have specified value for the selected attribute
               if (subclusterExamples==null) {
                  DecisionTreeNode subtree = DecisionTreeNode.createLeafNode(majoritylabel); // No training examples for this value so just assign it the common class
                  tree.addBranch(value, subtree);
               } else {
                 ArrayList<Integer> restAttributes = removeAttributeFromList(attributes, new Integer(bestAttributeIndex));  // a list of the remaining attributes (index Integers)
                 DecisionTreeNode subtree = constructTree(subclusterExamples,restAttributes,majoritylabel);
                 tree.addBranch(value, subtree);
               }
           }
           return tree;
       }
    } // end constructTree


    /** From a list of examples, return the classlabel that occurs most times - (*OPTIMIZED*) */
    private String getMostCommonClasslabelInExamples(ArrayList<Example> examples) {
        HashMap<String,Integer> counts=new HashMap<String,Integer>();
        String bestsofarclass=null; int bestsofarcount=Integer.MIN_VALUE;
        for (Example example:examples) { // count the number of occurrences of each value and place them in a key->value map (that is value->#occurrences)
            String classlabel=example.getClassification();
            int value;
            if (counts.containsKey(classlabel)) {value = counts.get(classlabel).intValue()+1;}
            else {value=1;}
            counts.put(classlabel,new Integer(value));
            if (value>bestsofarcount) {bestsofarcount=value;bestsofarclass=classlabel;}
        }
        return bestsofarclass; // return the value which scored the best (or the "first" if a tie has occurred)
    }



    /** returns the key corresponding to the highest value in a key->value hash */
    private String getBestScoring(HashMap<String,Integer> set) {
      String best=null; int bestscore=Integer.MIN_VALUE;
      for (String candidate : set.keySet()) {
          if (set.get(candidate).intValue()>bestscore) {best=candidate; bestscore=set.get(candidate).intValue();}
      }
      return best;
    }

     /** Gets a "usable value" for specified attribute of this example. Categorical values are returned as is. Numerical attributes are discritized into a number of bins with String names "0","1",..."N" */
     private String getProperValueOfAttribute(Example example, int attributeindex) {
         if (example.getAttributeType(attributeindex)==example.UNKNOWN) {System.err.println("Attribute has unknown type in DecisionTreeClassifier.getProperValue(example,attributeindex)");}
         else if (example.getAttributeType(attributeindex)==example.NUMERIC) return example.getCategoryBinnedValue(attributeindex,numberofbinsfornumericattributes);
         return example.getCategoricalValue(attributeindex);
     }

     /** returns true is the list of examples all have the same classification */
     private boolean isHomogenous(ArrayList<Example> examples) {
         if (examples==null || examples.isEmpty()) return true;
         String commonclass=examples.get(0).getClassification();
         for (Example example : examples) {
             if (!example.getClassification().equals(commonclass)) return false; // found example with different class
         }
         return true;
     }

     /** returns the argument list of examples but clustered on attribute value. It returns a hashmap indexed on attribute value. HashMap.get(avalue) then returns an ArrayList containing those examples that have value=avalue for this attribute */
     private HashMap<String,ArrayList<Example>> clusterExamplesOnAttributeValue(ArrayList<Example> examples, int attributeIndex) {
         HashMap<String,ArrayList<Example>> map=new HashMap<String,ArrayList<Example>>();
          for (Example example : examples) {
             String propertyvalue=getProperValueOfAttribute(example,attributeIndex);
             if (!map.containsKey(propertyvalue)) map.put(propertyvalue,new ArrayList<Example>());
             map.get(propertyvalue).add(example);
         }
         return map;
     }

    /** returns a (new) list of possible attribute values for the target attribute */
     @SuppressWarnings("unchecked")
     private ArrayList<String> getAllAttributeValues(int attributeIndex, Example example) {
          int attributeType=example.getAttributeType(attributeIndex);
          if (attributeType==Example.NUMERIC) return binscategories;
          else if (attributeType==Example.CATEGORICAL) return (ArrayList<String>)example.getAttributeValues(attributeIndex);
          else return null;
     }

    /** returns a (new) list of which is a copy of attributes (a list of Integers) but with the value attributeIndex removed */
     private ArrayList<Integer> removeAttributeFromList(ArrayList<Integer> attributes, Integer attributeIndex) {
          ArrayList<Integer> list=new ArrayList<Integer>(attributes);
          list.remove(attributeIndex);
          return list;
     }




      /** initializes the global ArrayList 'binscategories' with value {"0","1",...."N"} depending on the number of bins */
      private void refactorBinsCategories(){
        binscategories=new ArrayList<String>();
        for (int i=0;i<numberofbinsfornumericattributes;i++) binscategories.add(new String(""+i));
     }




     /** returns the index of the attribute with highest gain on examples among those listed (or alternatively as here: the one with the lowest remainder */
     private int selectBestAttributeToSplitOn(ArrayList<Example>examples, ArrayList<Integer>attributes) {
         double lowestRemainderScore=Double.POSITIVE_INFINITY;
         int bestAttributeFound=-1;
         for (Integer attribute : attributes) {
             double result=remainder(examples,attribute.intValue());
             if (result<lowestRemainderScore) {lowestRemainderScore=result; bestAttributeFound=attribute.intValue();}
         }
         return bestAttributeFound;
     }

     /** calculates the information requirement remaining if we split on this attribute */
     private double remainder(ArrayList<Example>examples, int attributeIndex) {
         ArrayList<String> attributeValues=getAllAttributeValues(attributeIndex,examples.get(0));
         double result=0.0;
         HashMap<String,ArrayList<Example>> valueClusters = clusterExamplesOnAttributeValue(examples,attributeIndex);
         //System.out.println("\n\nexamples has "+examples.size()+" entries, valueClusters has "+valueClusters.size()+" clusters");
         for (String value : attributeValues) {
             double weightsOfExamplesWithAttributeValue = (!valueClusters.containsKey(value)) ? 0 : getWeightsOfExamples(valueClusters.get(value));
             result+=calculateRemainderForAttributeValue(examples, attributeIndex, value, weightsOfExamplesWithAttributeValue);
         }
         //System.out.println("Gain for attribute ["+attributeIndex+"] = "+(1-result)+ " (on "+examples.size()+" examples) remainder="+result);
         return result;
     }

     private double calculateRemainderForAttributeValue(ArrayList<Example>examples, int attributeIndex, String attributeValue, double weightsOfExamplesWithAttributeValue) {
         if (weightsOfExamplesWithAttributeValue==0 || examples.isEmpty()) return 0.0;
         double fraction=weightsOfExamplesWithAttributeValue/getWeightsOfExamples(examples);
         int numberofclasses=examples.get(0).getNumberOfClasses();
         double[] classfractions=new double[numberofclasses];
         int i=0;
         //System.out.println("  Attribute["+attributeIndex+"] value="+attributeValue+",  weightAttr="+weightsOfExamplesWithAttributeValue+"/"+getWeightsOfExamples(examples)+", fraction="+fraction);
         ArrayList<String> classlabels=examples.get(0).getClasses();
         for (String classlabel : classlabels) {
             double weightsOfExamplesWithClassAndAttributeValue=getWeightsOfExamplesWithClassAndAttributeValue(examples,classlabel,attributeIndex,attributeValue);
             classfractions[i]=weightsOfExamplesWithClassAndAttributeValue/weightsOfExamplesWithAttributeValue; // this is the value 'n' in entropy's n*log2(n)
             //System.out.println("    class="+classlabel+", entropy n="+weightsOfExamplesWithClassAndAttributeValue+"/"+weightsOfExamplesWithAttributeValue+", fraction="+classfractions[i]);
             i++;
         }
         //System.out.println("  => fraction="+fraction+",  information="+calculateInformationContent(classfractions));
         return fraction*calculateInformationContent(classfractions);
     }

     /** returns for a given set of examples either the sum of weights (if useWeightedExamples()==true) or the number of examples in the set (if useWeightedExamples()==false) */
     private double getWeightsOfExamples(ArrayList<Example> examples) {
         if (!useWeightedExamples()) return examples.size();
         else {
             double sum=0.0;
             for (Example example : examples) sum+=example.getWeight();
             return sum;
         }
     }


     /** returns the argument list of examples but clustered on class. It returns a hashmap indexed on classlabel. HashMap.get(label) then returns an ArrayList containing those examples that have getClassification()=label */
     private HashMap<String,ArrayList<Example>> clusterExamplesOnClass(ArrayList<Example> examples) {
         HashMap<String,ArrayList<Example>> map=new HashMap<String,ArrayList<Example>>();
          for (Example example : examples) {
             String classlabel=example.getClassification();
             if (!map.containsKey(classlabel)) map.put(classlabel,new ArrayList<Example>());
             map.get(classlabel).add(example);
         }
         return map;
     }

     /** sums the weights of examples in 'examples' that have the given attributevalue in addition to the given classification
      *  however if useWeightedExamples==true it just returns the total count of these examples (i.e. treating weights as 1.0)
      */
     private double getWeightsOfExamplesWithClassAndAttributeValue(ArrayList<Example>examples, String classlabel, int attributeIndex, String attributeValue) {
         double result=0.0;
         for (Example example:examples) {
             if (example.getClassification().equals(classlabel) && getProperValueOfAttribute(example,attributeIndex).equals(attributeValue)) {
                 if (useWeightedExamples()) result+=example.getWeight();
                 else result+=1.0;
             }
         }
         return result;
     }

     private double calculateInformationContent(double[] fractions) {
         double result=0.0;
         for (int i=0;i<fractions.length;i++) {
             if (fractions[i]!=0) result-=fractions[i]*log2(fractions[i]);
         }
         return result;
     }

     private double log2(double value) {
         return Math.log(value)/Math.log(2);
     }





     /** implements a backwards pruning strategy
      *
      *  1) First we calculate errors and other nice stuff by sending testexamples down the tree and see how it goes
      *  2) Then we use these numbers to see whether to replace an internal node by the majority-vote of its children.
      *     this is done recursively from the bottom up. So each internal node would only have leaf-children when processed.
      *
      */
     public void pruneTree(ClassifierDataSet testset) {
         decisionTree.resetSubtreeCounts(); //
         ArrayList<Example> examples=testset.getExamples();
         for (Example example : examples) { // make calculations for each example
             DecisionTreeNode node=descendTreeAndRecord(decisionTree, example);
             if (!(example.getClassification().equals(node.getClassLabel()))) node.incrementError(); // this is not used in the pseudocode!!
         }
         descendTreeAndPrune(decisionTree); // do the pruning
     }

     /** This method recursively descends an example down the tree and returns the leaf-node it stops in
      *  Along the way each internal node has its "class-count" updated. That is, each internal node holds a
      *  mapping class->integer that records how many examples of a given class has passed it.
      */
     private DecisionTreeNode descendTreeAndRecord(DecisionTreeNode node, Example example) {
        if (node.isLeafNode()) return node;
        else {
            node.incrementClassCount(example.getClassification());
            int branchAttribute=node.getBranchAttribute();
            String exampleValue=getProperValueOfAttribute(example,branchAttribute);
            DecisionTreeNode subtree = node.getSubTreeForValue(exampleValue);
            return descendTreeAndRecord(subtree, example);
        }
      }

     /**
      *
      * Uses information gathered in a previous step by descendTreeAndRecord() and works up recursively from the
      * bottom up replacing subtrees with leafnodes if that improves the performance on the testset (which was recorded by descendTreeAndRecord)
      *
      * However, since the fully trained tree is probably targeted specifically to this trainingset (and classifies the trainingset 100% accurately)
      * it is most likely extremely large and the validation set is probably to small to get assessments from every subtree of it.
      * The strategy I use then is : if a subtree is not used by the validation set, it is probably useless to descend it in general and it should be replaced by a leafnode
      */
      private void descendTreeAndPrune(DecisionTreeNode node) {
          if (node.isLeafNode()) return;
          else {
             for (DecisionTreeNode childnode : node.getChildren()) {descendTreeAndPrune(childnode);}  // process all children first (if any)
             int passingExamples=node.getTotalPassingExamples();
             String majorityclass = getMajorityClassInSubtree(node); // most common class among the leafnodes in this subtree
             if (passingExamples==0) {
                 if (pruneUnusedSubtrees) replaceWithLeafNode(node,majorityclass); // this subtree was not used at all by the validation set so just prune it
             } else {
               int misclassificationsInSubtree=node.getSubtreeErrors(); // number of examples that have passed this node and been incorrectly classified in its subtree
               if (replaceBasedOnValidationData) {// replace based on most common class in validation dataset
                 String mostCommonClassInValidationSet = getBestScoringClass(node.getClassCounts());
                 int replacementError = node.getTotalPassingExamples()-node.getCountsForClass(mostCommonClassInValidationSet); // the number of misclassifications we would see on the testset if this internal node was replaced with a leafnode with the most common class
                 if (replacementError<=misclassificationsInSubtree) replaceWithLeafNode(node,mostCommonClassInValidationSet);
               } else { // replace based on most common class among leafnodes
                 int replacementError = node.getTotalPassingExamples()-node.getCountsForClass(majorityclass); // the number of misclassifications we would see on the testset if this internal node was replaced with a leafnode with the majority class
                 if (replacementError<=misclassificationsInSubtree) replaceWithLeafNode(node,majorityclass);
               }
             }
          }
      }

      /** Replaces the given subtree with a leafnode with the given classlabel */
      private void replaceWithLeafNode(DecisionTreeNode subtree, String classlabel) {
          DecisionTreeNode parent=subtree.getParent();
          if (parent==null) return; // this seems to be the root
          DecisionTreeNode newsubtree=DecisionTreeNode.createLeafNode(classlabel);
          parent.replaceBranch(subtree,newsubtree);
      }

      /** Returns the most common classlabel among leafnodes in this subtree */
      private String getMajorityClassInSubtree(DecisionTreeNode subtree) {
          String majorityclass = null;
          HashMap<String,Integer> map = new HashMap<String,Integer>();
          map = countClassesInLeafNodes(subtree,map);
          return getBestScoringClass(map);
      }

      /** Counts the number of times each class occurs in any of the leafnodes of this tree and returns a list (or map) of classlabels->counts*/
      private HashMap<String,Integer> countClassesInLeafNodes(DecisionTreeNode subtree, HashMap<String,Integer> map) {
          if (subtree.isLeafNode()) {
              String classlabel=subtree.getClassLabel();
              if (map.containsKey(classlabel)) map.put(classlabel, new Integer(map.get(classlabel).intValue()+1));
              else map.put(classlabel,new Integer(1));
          } else {
            for (DecisionTreeNode childnode : subtree.getChildren()) map=countClassesInLeafNodes(childnode,map);
          }
          return map;
      }

      /*
     * When given a HashMap of names->values, returns the name corresponding to the highest value
     */
    private String getBestScoringClass (HashMap<String,Integer> classes) {
        int maxvalue = Integer.MIN_VALUE;
        String mapclassname=null;
        for (String name : classes.keySet()) {
            int classvalue=classes.get(name).intValue();
            if (classvalue>maxvalue) {maxvalue=classvalue;mapclassname=name;}
        }
        return mapclassname;
    }

     public String getParametersDescription() {
         return "discrete bins="+this.numberofbinsfornumericattributes+", prune="+useBackWardPruning+", pruneUnused="+pruneUnusedSubtrees+", useValidation="+replaceBasedOnValidationData+", useWeightedExamples="+useWeightedExamples()+", sampling="+Classifier.SAMPLING_STRATEGY[getSamplingStrategy()];
     }

    @Override
    public ClassifierVisualizer getVisualizer() {
        return new DecisionTreeVisualizer(this);
    }

    @Override
    public void initializeFromParameters(ParameterSettings settings, MotifLabEngine engine) throws ExecutionError {
        int bins=0;
        boolean prune=false;
        boolean pruneUnusedSubtrees=false;
        boolean replaceBasedOnValidationData=false;

        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              bins=(Integer)settings.getResolvedParameter("Number of discrete bins",defaults,engine);
              prune=(Boolean)settings.getResolvedParameter("Prune tree",defaults,engine);
              pruneUnusedSubtrees=(Boolean)settings.getResolvedParameter("Prune unused subtrees",defaults,engine);
              replaceBasedOnValidationData=(Boolean)settings.getResolvedParameter("Use validation data for pruning",defaults,engine);
           } catch (Exception ex) {
              throw new ExecutionError("An error occurred during classifier initialization", ex);
           }
        } else {
            bins=(Integer)getDefaultValueForParameter("Number of discrete bins");
            prune=(Boolean)getDefaultValueForParameter("Prune tree");
            pruneUnusedSubtrees=(Boolean)getDefaultValueForParameter("Prune unused subtrees");
            replaceBasedOnValidationData=(Boolean)getDefaultValueForParameter("Use validation data for pruning");
        }
        this.setNumberOfBinsForNumericAttributes(bins);
        this.usePruning(prune, pruneUnusedSubtrees, replaceBasedOnValidationData);
    }


}
