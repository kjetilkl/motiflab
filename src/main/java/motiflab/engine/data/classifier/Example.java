package motiflab.engine.data.classifier;

import java.util.ArrayList;

/**
 * This class represents a training example (instance) which is a member of a trainingset or validationset
 * and should hopefully be correctly classified by a trained classifier (neural net, decision tree etc.)
 * The class holds an array of feature-variables (doubles) which describes this particular instance
 * of the feature-space, along with the correct classification (an integer) of this instance.
 *
 * @author  Kjetil Klepper
 * @version
 */
public class Example implements Comparable<Example>{

  private Object[] variables; // the featurevector of variables (attributes) describing this instance. Objects could either be "Double" for numeric attributes or "String" for all other
  private String classification; // the correct classification of this instance (for validation purposes).

  private ClassifierDataSet dataset=null; // the dataset this example belongs to. The dataset can contain additional information regarding the "whole group" of examples, such as allowable values / ranges for attributes

  public static final int UNKNOWN=0; // signals that an attribute is of type UNKNOWN (usually because nothing has been observed yet)
  public static final int NUMERIC=1; // signals that an attribute is of numeric type
  public static final int CATEGORICAL=2; // signals that an attribute has values from a limited number of categorical values

  /**  Creates new Example. "size" is the dimension of the featurespace.
    *  Values of each instance variable must be set with setValue
    *  @param size Number of features
    */
  public Example(int size) {
      variables=new Object[size];

  }

  /** Creates new Example with the featurevector as argument.
   *  Also finds and sets the maximum and minimum values for each dimension
   */
  public Example(Object[] vars) {
      variables=vars;
    }

  /** Sets a reference back to the dataset which this example should be a part of
   *  the dataset object contains useful information about legal attributevalues and such
   */
  public void setDataSet(ClassifierDataSet dataset) {this.dataset=dataset;}


  /** returns the raw value of the given attribute of the featurevector of type Object */
  public Object getRawValue(int attributeindex) {
     return variables[attributeindex];
  }

  /** returns the value of the given attribute of the featurevector as a String (provided it is one)*/
  public String getCategoricalValue(int attributeindex) {
      if (variables[attributeindex].getClass()!=String.class) {System.err.println("WARNING: attribute["+attributeindex+"] is not a categorical value"); return null;}
      return (String)variables[attributeindex];
  }

  /** returns the value of the given attribute of the featurevector as a double (provided it is a numerical value)*/
  public double getNumericValue(int attributeindex) {
      if (variables[attributeindex].getClass()!=Double.class) {System.err.println("WARNING: attribute["+attributeindex+"] is not a numerical value"); return Double.NaN;}
      return ((Double)variables[attributeindex]).doubleValue();
  }

  /**
   * This method return a categorical (String) value for an attribute which is initially numeric by dividing the
   * "legal range" of values (taken from DataSet) linearly into different bins named "0","1","2"..."N" (where N is the value one less than
   * the specified 'numberofbins' to use). If an example's value should happen to be outside the legal range then "0" or "N"
   * is returned depending on whether the value was lower or higher.
   *
   */
  public String getCategoryBinnedValue(int attributeindex, int numberofbins) {
      if (variables[attributeindex].getClass()!=Double.class) {System.err.println("Example attribute["+attributeindex+"] is not numeric (but rather '"+variables[attributeindex].getClass()+"')"); return null;}
      if (this.dataset==null) {System.err.println("no dataset in getBinnedValue()");return null;}
      Object values= this.dataset.getAttributeValues(attributeindex);
      if (values.getClass()!=Double[].class) {System.err.println("DataSet attribute["+attributeindex+"] has no numeric range in getBinnedValue"); return null;}
      Double[] doublevalues=(Double[])values;
      double min=doublevalues[0].doubleValue();
      double max=doublevalues[1].doubleValue();
      double attributevalue=(Double)variables[attributeindex];
      if (attributevalue<=min) return new String("0");
      if (attributevalue>=max) return new String(""+(numberofbins-1));
      int bin=(int)Math.floor( ((attributevalue-min)/(max-min))*(double)numberofbins);
      if (bin>=numberofbins) bin=numberofbins-1; // just in case
      if (bin<0) bin=0; // just in case
      return new String(""+bin);
  }

  /** returns the "adjusted" value in dimension "var" of the featurevector
   *  The return value is adjusted relative to the range of this particular attribute as well as the global range of all numerical attributes
   */
  public double getAdjustedValue(int var, double valmin, double valmax) {
     // System.err.println("Var="+var+"  max#"+max.length+"  min#="+min.length);
     if (dataset==null) {System.err.println("WARNING: Dataset==null in Example.getAdjustedValue"); return Double.NaN;}
     Double[] attributerange=(Double[])dataset.getAttributeValues(var);

     double attributemin=attributerange[0];
     double attributemax=attributerange[1];
     double diff=attributemax-attributemin;
     if (diff==0) diff=1; // if all observed values for this attribute are equal
     double value=valmin+((valmax-valmin)/(diff))*(((Double)variables[var]).doubleValue()-attributemin);
     if (value<0) value=0;
     if (value>1) value=1;
     return value;
  }


  /** sets the value in dimension "var" of the featurevector to "value" */
  public void setValue(int var, Object value) {
     variables[var]=value;
  }




  public void setClassification(String c) {
       classification=c;
  }

  /** Returns the assigned class of this example */
  public String getClassification() {
       return classification;
  }


  /** Return the type of attribute for the specified index */
  public int getAttributeType(int attributeindex) {
      Object attributevalues=dataset.getAttributeValues(attributeindex);
      if (attributevalues==null) return UNKNOWN;
      else if (attributevalues.getClass()==Double[].class) return NUMERIC;
      else if (attributevalues.getClass()==ArrayList.class) return CATEGORICAL;
      else return UNKNOWN;
  }

  /** Returns the possible values for this attribute (as learned from examples or set explisitly in the dataset.
   *  Returned object is either a ArraList<String> which contains the allowed values, or, for numeric attributes,
   *  a Double[] in which [0] specifies the minimum value and [1] the maximum value
   */
  public Object getAttributeValues(int attributeindex) {
      if (dataset==null) {System.err.println("WARNING: Example has no dataset in getAttributeValues()");return null;}
      else return dataset.getAttributeValues(attributeindex);
  }



  /** Return the number of attributes describing this example */
  public int getNumberOfAttributes() {return variables.length;}

 /** Return the number of possible classes for an example */
  public int getNumberOfClasses() {return dataset.getNumberOfClasses();}

 /** Return a list of the the possible classes for an example */
  public ArrayList<String> getClasses() {return dataset.getClasses();}



  public String toString() {
     String s="Example{";
     for (int i=0; i<variables.length;i++)
          s+=" ["+i+"]="+getRawValue(i);
     s+="}";
     return s;
  }

  public String exampleString() {
     String s="";
     for (int i=0;i<variables.length;i++)
          s+=(variables[i]+",");

     s+=""+getClassification();
     return s;
  }

  /** Returns a weight to use for this example when boosting is employed */
  public double getWeight() {
    if (dataset==null) return Double.NaN;
    else return dataset.getWeight(this).doubleValue();
  }

    @Override
    public int compareTo(Example o) {
        int cc=this.classification.compareTo(o.classification);
        if (cc!=0) return cc;
        for (int i=0;i<variables.length;i++) {
            if (variables[i] instanceof Double) {
               cc=Double.compare((Double)variables[i], (Double)variables[i]);
               if (cc!=0) return cc;
            } else if (variables[i] instanceof String) {
               cc=((String)this.variables[i]).compareTo((String)o.variables[i]);
               if (cc!=0) return cc;
            }
        }
        return 0;
    }

    /** Returns TRUE if this example has the same classification and attribute values as the other*/
    public boolean isSimilar(Example o) {
        if (!this.classification.equals(o.classification)) return false;
        for (int i=0;i<variables.length;i++) {
            if (variables[i] instanceof Double) {
               if (((Double)variables[i]).doubleValue()!=((Double)o.variables[i]).doubleValue()) return false;
            } else if (variables[i] instanceof String) {
              if (!((String)variables[i]).equals(((String)o.variables[i]))) return false;
            }
        }
        return true;
    }
}