/*
 
 
 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericConstant;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;

/**
 *
 * @author kjetikl
 */
public class Operation_normalize extends FeatureTransformOperation {
    public static final String SUM_TO_ONE="sumToOne"; 
    public static final String NORMALIZE_TO_RANGE="normalizeToRange"; 
    public static final String MODE="mode"; 
    public static final String OLD_MIN="oldMinimum"; 
    public static final String OLD_MAX="oldMaximum"; //
    public static final String NEW_MIN="newMinimum"; //
    public static final String NEW_MAX="newMaximum"; //
    private static final String OLD_MIN_VALUE="oldMinimumValue"; 
    private static final String OLD_MAX_VALUE="oldMaximumValue"; //
    private static final String NEW_MIN_VALUE="newMinimumValue"; //
    private static final String NEW_MAX_VALUE="newMaximumValue"; //    
    private static final String SEQUENCE_MIN="sequenceMin";
    private static final String SEQUENCE_MAX="sequenceMax";
    private static final String DATASET_MIN="datasetMin";    
    private static final String DATASET_MAX="datasetMax";
    private static final String COLLECTION_MIN="collectionMin";
    private static final String COLLECTION_MAX="collectionMax";
    public static final String PROPERTY_NAME="propertyName"; // the name of the property that the operation should be applied to (if applied to Region Datasets or Sequences/Motifs/Modules). Defaults to "score" if null
    
    private static final String name="normalize";
    private static final String description="Normalizes a dataset by transforming numeric values from one range to another";
    private Class[] datasourcePreferences=new Class[]{NumericDataset.class,RegionDataset.class};
    private static String[] specialvalues=new String[]{"dataset.min","dataset.max","sequence.min","sequence.max","collection.min","collection.max"};

    @Override
    public String getOperationGroup() {
        return "Transform";
    }
    
    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {return name;}
    
    @Override
    public boolean isSubrangeApplicable() {return true;}
    
    /** Returns a list of special value settings for the normalization operation */
    public static String[] getSpecialValues() {
        return (String[])specialvalues.clone();
    }

    private boolean isSpecialValue(String string) {
        for (String special:specialvalues) {
            if (special.equals(string)) return true;
        }
        return false;
    }

    @Override
    public void resolveParameters(OperationTask task) throws Exception {
        String propertyName=(String)task.getParameter(Operation_normalize.PROPERTY_NAME);
        if (propertyName==null || propertyName.isEmpty()) propertyName="score";
        String mode=(String)task.getParameter(MODE);
        if (mode.equals(NORMALIZE_TO_RANGE)) {
            String oldMinString=(String)task.getParameter(OLD_MIN);
            if (!isSpecialValue(oldMinString)) {
                Data oldMinData=null;
                oldMinData=engine.getDataItem(oldMinString);
                if (oldMinData==null) {
                    try {
                      double value=Double.parseDouble(oldMinString);
                      oldMinData=new NumericConstant(oldMinString, (double)value);
                   } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+oldMinString+"' neither data nor numeric constant",task.getLineNumber());}         
                }
                if (!(oldMinData instanceof NumericConstant || oldMinData instanceof NumericVariable || oldMinData instanceof SequenceNumericMap)) throw new ExecutionError("'"+oldMinString+"' is not a valid numeric data object",task.getLineNumber());
                task.setParameter(OLD_MIN_VALUE, oldMinData);
            } else task.setParameter(OLD_MIN_VALUE, oldMinString);
            // -- Old max --
            String oldMaxString=(String)task.getParameter(OLD_MAX);
            //engine.logMessage("Resolve: oldMaxString="+oldMaxString);
            if (!isSpecialValue(oldMaxString)) {
                Data oldMaxData=null;
                oldMaxData=engine.getDataItem(oldMaxString);
                if (oldMaxData==null) {
                    try {
                      double value=Double.parseDouble(oldMaxString);
                      oldMaxData=new NumericConstant(oldMaxString, (double)value);
                   } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+oldMaxString+"' neither data nor numeric constant",task.getLineNumber());}         
                }
                if (!(oldMaxData instanceof NumericConstant || oldMaxData instanceof NumericVariable || oldMaxData instanceof SequenceNumericMap)) throw new ExecutionError("'"+oldMaxString+"' is not a valid numeric data object",task.getLineNumber());
                task.setParameter(OLD_MAX_VALUE, oldMaxData);
            } else task.setParameter(OLD_MAX_VALUE, oldMaxString); 
            // -- New min --
            String newMinString=(String)task.getParameter(NEW_MIN);
            //engine.logMessage("Resolve: newMinString="+newMinString);
           if (!isSpecialValue(newMinString)) {
                Data newMinData=null;
                newMinData=engine.getDataItem(newMinString);
                if (newMinData==null) {
                    try {
                      double value=Double.parseDouble(newMinString);
                      newMinData=new NumericConstant(newMinString, (double)value);
                   } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+newMinString+"' neither data nor numeric constant",task.getLineNumber());}         
                }
                if (!(newMinData instanceof NumericConstant || newMinData instanceof NumericVariable || newMinData instanceof SequenceNumericMap)) throw new ExecutionError("'"+newMinString+"' is not a valid numeric data object",task.getLineNumber());
                task.setParameter(NEW_MIN_VALUE, newMinData);
            } else task.setParameter(NEW_MIN_VALUE, newMinString);
            // -- New max --
            String newMaxString=(String)task.getParameter(NEW_MAX);
            //engine.logMessage("Resolve: newMaxString="+newMaxString);
            if (!isSpecialValue(newMaxString)) {
                Data newMaxData=null;
                newMaxData=engine.getDataItem(newMaxString);
                if (newMaxData==null) {
                    try {
                      double value=Double.parseDouble(newMaxString);
                      newMaxData=new NumericConstant(newMaxString, (double)value);
                   } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+newMaxString+"' neither data nor numeric constant",task.getLineNumber());}         
                }
                if (!(newMaxData instanceof NumericConstant || newMaxData instanceof NumericVariable || newMaxData instanceof SequenceNumericMap)) throw new ExecutionError("'"+newMaxString+"' is not a valid numeric data object",task.getLineNumber());
                task.setParameter(NEW_MAX_VALUE, newMaxData);
            } else task.setParameter(NEW_MAX_VALUE, newMaxString);
            // calculate min&max values for the sequences, collection and dataset
            SequenceCollection collection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION); 
            FeatureDataset sourcedataset=(FeatureDataset)task.getSourceData();
            int numSequences=sourcedataset.getSize();
            double datasetMin=(numSequences==0)?0:Double.MAX_VALUE;
            double datasetMax=(numSequences==0)?0:-Double.MAX_VALUE;    
            double collectionMin=(collection==null || collection.isEmpty())?0:Double.MAX_VALUE;
            double collectionMax=(collection==null || collection.isEmpty())?0:-Double.MAX_VALUE;
            double sequenceMin[]=new double[numSequences];
            double sequenceMax[]=new double[numSequences];
            for (int i=0;i<numSequences;i++) {
                sequenceMin[i]=Double.MAX_VALUE;
                sequenceMax[i]=-Double.MAX_VALUE;
            }
            for (int i=0;i<sourcedataset.getSize();i++) {
                FeatureSequenceData sequenceData = (FeatureSequenceData)sourcedataset.getSequenceByIndex(i);
                String sequenceName=sequenceData.getName();
                double[] values=new double[]{0,0};
                if (sequenceData instanceof NumericSequenceData) values=((NumericSequenceData)sequenceData).getMinMaxFromData();
                else if (sequenceData instanceof RegionSequenceData) values=((RegionSequenceData)sequenceData).getMinMaxFromData(propertyName);
                sequenceMin[i]=values[0];
                sequenceMax[i]=values[1];
                if (values[0]<datasetMin) datasetMin=values[0];
                if (values[1]>datasetMax) datasetMax=values[1];
                if (collection!=null && collection.contains(sequenceName)) {
                    if (values[0]<collectionMin) collectionMin=values[0];
                    if (values[1]>collectionMax) collectionMax=values[1];
                }
            }   
            if (collection==null) {
                collectionMin=datasetMin;
                collectionMax=datasetMax;
            }
            task.setParameter(SEQUENCE_MIN, sequenceMin);
            task.setParameter(SEQUENCE_MAX, sequenceMax);
            task.setParameter(DATASET_MIN, datasetMin);
            task.setParameter(DATASET_MAX, datasetMax);
            task.setParameter(COLLECTION_MIN, collectionMin);
            task.setParameter(COLLECTION_MAX, collectionMax);
        }
    }

    
    @Override
    public void transformSequence(FeatureSequenceData sourceSequence, FeatureSequenceData targetSequence, OperationTask task) throws Exception {
        String seqname=sourceSequence.getName();
        FeatureDataset dataset=(FeatureDataset)task.getSourceData();
        int index=dataset.getIndexForSequence(seqname);
        double oldMinValue=0;
        double oldMaxValue=0;
        double newMinValue=0;
        double newMaxValue=0;
        
        String propertyName=(String)task.getParameter(PROPERTY_NAME);
        if (propertyName==null || propertyName.isEmpty()) propertyName="score";
        String mode=(String)task.getParameter(MODE);        
        if (mode==null) mode=Operation_normalize.SUM_TO_ONE;
        if (mode.equals(SUM_TO_ONE)) {
            double sum=0;
            if (targetSequence instanceof NumericSequenceData) {
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    double oldValue=((NumericSequenceData)targetSequence).getValueAtGenomicPosition(i);
                    sum+=oldValue;
                }
                if (sum==0) return; // nothing to normalize
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    double oldValue=((NumericSequenceData)targetSequence).getValueAtGenomicPosition(i);
                    double normalizedValue=oldValue/sum;
                    ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i, normalizedValue);
                }                
            } else if (targetSequence instanceof RegionSequenceData) {
                for (Region region:((RegionSequenceData)targetSequence).getOriginalRegions()) {
                    Object oldvalue=region.getProperty(propertyName);
                    if (oldvalue instanceof Number) sum+=((Number)oldvalue).doubleValue();
                }
                if (sum==0) return; // nothing to normalize
                for (Region region:((RegionSequenceData)targetSequence).getOriginalRegions()) {
                    Object oldvalue=region.getProperty(propertyName);
                    if (oldvalue instanceof Number) {
                        double newvalue=((Number)oldvalue).doubleValue()/sum;
                        region.setProperty(propertyName, newvalue);
                    }
                }             
            } else {
               // targetSequence is not Numeric Dataset or Region Dataset
               // ... just ignore for now
            }
        } else if (mode.equals(NORMALIZE_TO_RANGE)) {
            // -- Old min --
            Object oldMinObject=task.getParameter(OLD_MIN_VALUE);
            if (oldMinObject instanceof String) {
                     if (((String)oldMinObject).equals("sequence.min")) oldMinValue=((double[])task.getParameter(SEQUENCE_MIN))[index];
                else if (((String)oldMinObject).equals("sequence.max")) oldMinValue=((double[])task.getParameter(SEQUENCE_MAX))[index];
                else if (((String)oldMinObject).equals("dataset.min")) oldMinValue=(Double)task.getParameter(DATASET_MIN);
                else if (((String)oldMinObject).equals("dataset.max")) oldMinValue=(Double)task.getParameter(DATASET_MAX);
                else if (((String)oldMinObject).equals("collection.min")) oldMinValue=(Double)task.getParameter(COLLECTION_MIN);
                else if (((String)oldMinObject).equals("collection.max")) oldMinValue=(Double)task.getParameter(COLLECTION_MAX);
            }
            else if (oldMinObject instanceof SequenceNumericMap) oldMinValue=(double)((SequenceNumericMap)oldMinObject).getValue(seqname).doubleValue();
            else if (oldMinObject instanceof NumericVariable) oldMinValue=(double)((NumericVariable)oldMinObject).getValue().doubleValue();
            else if (oldMinObject instanceof NumericConstant) oldMinValue=(double)((NumericConstant)oldMinObject).getValue().doubleValue();
            //engine.logMessage("Oldmin="+oldMinObject.getClass()+"   OldMinString='"+oldMinObject.toString()+"'    oldMinValue="+oldMinValue);
            // -- Old max --
            Object oldMaxObject=task.getParameter(OLD_MAX_VALUE);
            if (oldMaxObject instanceof String) {
                     if (((String)oldMaxObject).equals("sequence.min")) oldMaxValue=((double[])task.getParameter(SEQUENCE_MIN))[index];
                else if (((String)oldMaxObject).equals("sequence.max")) oldMaxValue=((double[])task.getParameter(SEQUENCE_MAX))[index];
                else if (((String)oldMaxObject).equals("dataset.min")) oldMaxValue=(Double)task.getParameter(DATASET_MIN);
                else if (((String)oldMaxObject).equals("dataset.max")) oldMaxValue=(Double)task.getParameter(DATASET_MAX);
                else if (((String)oldMaxObject).equals("collection.min")) oldMaxValue=(Double)task.getParameter(COLLECTION_MIN);
                else if (((String)oldMaxObject).equals("collection.max")) oldMaxValue=(Double)task.getParameter(COLLECTION_MAX);
            }
            else if (oldMaxObject instanceof SequenceNumericMap) oldMaxValue=(double)((SequenceNumericMap)oldMaxObject).getValue(seqname).doubleValue();
            else if (oldMaxObject instanceof NumericVariable) oldMaxValue=(double)((NumericVariable)oldMaxObject).getValue().doubleValue();
            else if (oldMaxObject instanceof NumericConstant) oldMaxValue=(double)((NumericConstant)oldMaxObject).getValue().doubleValue();
            //engine.logMessage("Oldmax="+oldMaxObject.getClass()+"   OldMaxString='"+oldMaxObject.toString()+"'    oldMaxValue="+oldMaxValue);
            // -- New mix --
            Object newMinObject=task.getParameter(NEW_MIN_VALUE);
            if (newMinObject instanceof String) {
                     if (((String)newMinObject).equals("sequence.min")) newMinValue=((double[])task.getParameter(SEQUENCE_MIN))[index];
                else if (((String)newMinObject).equals("sequence.max")) newMinValue=((double[])task.getParameter(SEQUENCE_MAX))[index];
                else if (((String)newMinObject).equals("dataset.min")) newMinValue=(Double)task.getParameter(DATASET_MIN);
                else if (((String)newMinObject).equals("dataset.max")) newMinValue=(Double)task.getParameter(DATASET_MAX);
                else if (((String)newMinObject).equals("collection.min")) newMinValue=(Double)task.getParameter(COLLECTION_MIN);
                else if (((String)newMinObject).equals("collection.max")) newMinValue=(Double)task.getParameter(COLLECTION_MAX);
            }
            else if (newMinObject instanceof SequenceNumericMap) newMinValue=(double)((SequenceNumericMap)newMinObject).getValue(seqname).doubleValue();
            else if (newMinObject instanceof NumericVariable) newMinValue=(double)((NumericVariable)newMinObject).getValue().doubleValue();
            else if (newMinObject instanceof NumericConstant) newMinValue=(double)((NumericConstant)newMinObject).getValue().doubleValue();

            // -- New max --
            Object newMaxObject=task.getParameter(NEW_MAX_VALUE);
            if (newMaxObject instanceof String) {
                     if (((String)newMaxObject).equals("sequence.min")) newMaxValue=((double[])task.getParameter(SEQUENCE_MIN))[index];
                else if (((String)newMaxObject).equals("sequence.max")) newMaxValue=((double[])task.getParameter(SEQUENCE_MAX))[index];
                else if (((String)newMaxObject).equals("dataset.min")) newMaxValue=(Double)task.getParameter(DATASET_MIN);
                else if (((String)newMaxObject).equals("dataset.max")) newMaxValue=(Double)task.getParameter(DATASET_MAX);
                else if (((String)newMaxObject).equals("collection.min")) newMaxValue=(Double)task.getParameter(COLLECTION_MIN);
                else if (((String)newMaxObject).equals("collection.max")) newMaxValue=(Double)task.getParameter(COLLECTION_MAX);
            }
            else if (newMaxObject instanceof SequenceNumericMap) newMaxValue=(double)((SequenceNumericMap)newMaxObject).getValue(seqname).doubleValue();
            else if (newMaxObject instanceof NumericVariable) newMaxValue=(double)((NumericVariable)newMaxObject).getValue().doubleValue();
            else if (newMaxObject instanceof NumericConstant) newMaxValue=(double)((NumericConstant)newMaxObject).getValue().doubleValue();

            //engine.logMessage("Sequence["+seqname+"]:  calculated seqMin="+((double[])task.getParameter(SEQUENCE_MIN))[index]+",  seqMax="+((double[])task.getParameter(SEQUENCE_MAX))[index]+",  datasetMin="+(Double)task.getParameter(DATASET_MIN)+",  datasetMax="+(Double)task.getParameter(DATASET_MAX));
            //engine.logMessage("Sequence["+seqname+"]:  oldMin="+oldMinValue+",  oldMax="+oldMaxValue+",  newMin="+newMinValue+",  newMax="+newMaxValue);
            double oldrange=oldMaxValue-oldMinValue;
            if (targetSequence instanceof NumericSequenceData) {
                for (int i=sourceSequence.getRegionStart();i<=sourceSequence.getRegionEnd();i++) {
                    if (positionSatisfiesCondition(seqname,i,task)) {
                        double oldValue=((NumericSequenceData)targetSequence).getValueAtGenomicPosition(i);
                        double normalizedValue=newMinValue;
                        if (oldrange!=0) { // if (oldrange==0) throw new ExecutionError("Can not normalize data from zero range");
                            double oldScaleOffset=(oldValue-oldMinValue)/(oldrange);
                            double newScaledOffset=oldScaleOffset*(newMaxValue-newMinValue);
                            normalizedValue+=newScaledOffset;
                        }
                        ((NumericSequenceData)targetSequence).setValueAtGenomicPosition(i, normalizedValue);
                   } // satisfies 'where'-condition
                }
            } else if (targetSequence instanceof RegionSequenceData) {
                ArrayList<Region> regions=((RegionSequenceData)targetSequence).getAllRegions();
                for (Region region:regions) {
                    if (regionSatisfiesCondition(seqname,region,task)) {
                        Object currentValue=region.getProperty(propertyName);
                        if (!(currentValue instanceof Number)) continue; // just skip this region
                        double oldValue=((Number)currentValue).doubleValue();
                        double normalizedValue=newMinValue;
                        if (oldrange!=0) { // if (oldrange==0) throw new ExecutionError("Can not normalize data from zero range");
                            double oldScaleOffset=(oldValue-oldMinValue)/(oldrange);
                            double newScaledOffset=oldScaleOffset*(newMaxValue-newMinValue);
                            normalizedValue+=newScaledOffset;
                        }
                        region.setProperty(propertyName,normalizedValue);
                   } // satisfies 'where'-condition
                }
            }
        }// end normalize to range
      } // end transformSequence 
    
    
    
    }
