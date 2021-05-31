/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public class NumericDataset extends FeatureDataset implements Cloneable {
    private static String typedescription="Numeric Dataset";
    private double min=0;
    private double max=1;
    private double baseline=0;
    
    
    public NumericDataset(String datasetName) {
        super(datasetName);
    }
    
    @Override
    public void setupDefaultDataset(ArrayList<Sequence> sequences) {
        for (Sequence seq:sequences) {
            if (getSequenceByName(seq.getName())==null) {               
                NumericSequenceData seqdata=new NumericSequenceData(seq,0);
                addSequence(seqdata);
            }
        }
    }    
    
    /** 
     * Sets the value at every position in every sequence in this dataset to the provided value
     */
    public void setAllPositionsToValue(double value) {
        for (FeatureSequenceData seq:getAllSequences()) {
            ((NumericSequenceData)seq).setAllPositionsToValue(value);
        }
    }
    
   /**
     * Returns the maximum value allowed for this dataset
     * @return the maximum allowed value
     */       
    public Double getMaxAllowedValue() {
        return max;
    }

   /**
     * Sets the maximum value allowed for this dataset
     * The value must be equal to or greater than the baseline value
     * (if the value is smaller than the baseline, then the max value is set to the baseline value instead!)
     * @param max The maximum value for this dataset
     */       
    public void setMaxAllowedValue(double max) {
        if (max<baseline) max=baseline;
        this.max=max;
    }

   /**
     * Returns the minimum value allowed for this dataset
     * @return the minimum allowed value
     */       
    public Double getMinAllowedValue() {
        return min;
    }

   /**
     * Sets the minimum value allowed for this dataset
     * The value must be equal to or smaller than the baseline value
     * (if the value is greater than the baseline, then the min value is set to the baseline value instead!)
     * @param min The minimum value for this dataset
     */       
    public void setMinAllowedValue(double min) {
        if (min>baseline) min=baseline;
        this.min=min;
    }

   /**
     * Returns the baseline value for this dataset
     * @return the baseline value
     */       
    public Double getBaselineValue() {
        return baseline;
    }

   /**
     * Sets the baseline value for this dataset
     * @param baseline The baseline value for this dataset
     */       
    public void setBaselineValue(double baseline) {
        this.baseline=baseline;
    }
    
    /** Updates the allowed min and max values for this dataset based on the data for the current sequences.
     *  If the min/max-values of the current data are outside the allowed range, the range is extended to 
     *  included these values. If the current min/max-values are within the old range, the old allowed boundaries
     *  are kept. However, if the new range is significantly different smaller than the old range (less than 25%)
     *  the allowed range is updated to fit the new current min/max values
     */
    public void updateAllowedMinMaxValuesFromData() {
        double oldmin=min; double oldmax=max; double oldrange=oldmax-oldmin;
        double newmin=Double.MAX_VALUE; double newmax=-Double.MAX_VALUE;
        //min=0; max=1;
        ArrayList<FeatureSequenceData> sequences=getAllSequences();
        for (FeatureSequenceData seq:sequences) {
            double[] newvalues=((NumericSequenceData)seq).updateAllowedMinMaxValuesFromDataRange();
            if (newvalues[0]<min) min=newvalues[0];
            if (newvalues[1]>max) max=newvalues[1];            
            if (newvalues[0]<newmin) newmin=newvalues[0];
            if (newvalues[1]>newmax) newmax=newvalues[1];            
        }
        double newrange=newmax-newmin;
        if (newrange/oldrange<0.25) {min=newmin;max=newmax;}
        if (min==max) { // use at least some range
            if (min==0) max=1.0;
            else max=min+(Math.abs(min)*0.1); // set max=10% larger than min
        }
    }
    
    /** 
     * Returns the actual min/max-values in this dataset based on a search in the sequences
     * (this is used by the SetRangeDialog)
     */
    public double[] getMinMaxValuesFromData() {
        double newmin=Double.MAX_VALUE; double newmax=-Double.MAX_VALUE;
        //min=0; max=1;
        ArrayList<FeatureSequenceData> sequences=getAllSequences();
        for (FeatureSequenceData seq:sequences) {
            double[] newvalues=((NumericSequenceData)seq).getMinMaxFromData();          
            if (newvalues[0]<newmin) newmin=newvalues[0];
            if (newvalues[1]>newmax) newmax=newvalues[1];            
        }
        return new double[]{newmin,newmax};
    }
    
   /**
     * Sets a new value at the specified genomic position. A call to this method
     * will affect all sequences in this dataset which spans the given position 
     * @param chrom the chromsome
     * @param position The genomic position
     * @param value The new value at this position
     * @return FALSE if position is outside bounds of sequence else true
     */       
    public boolean setValueAtGenomicPosition(String chrom, int position, double value) {
        boolean result=false;
        for (FeatureSequenceData sequence:storage) {
            if (!(sequence.getChromosome().equals(chrom))) continue;
            result=result || ((NumericSequenceData)sequence).setValueAtGenomicPosition(position, value);
        }
        return result;
    }    
    
    /** 
     * Returns a deep copy of this object (but without listeners)
     */
    @Override
    public NumericDataset clone() {
        NumericDataset newdataset=new NumericDataset(datasetName); // creates a new dataset with the same name and empty storage
        newdataset.datasetDescription=this.datasetDescription;
        newdataset.min=this.min;
        newdataset.max=this.max;
        newdataset.baseline=this.baseline;
        int size=getNumberofSequences();
        for (int i=0;i<size;i++) {
            NumericSequenceData seq=(NumericSequenceData)getSequenceByIndex(i);
            if (seq==null) throw new ConcurrentModificationException("Sequence was deleted before the dataset could be cloned"); // not sure what else to do
            newdataset.addSequence((NumericSequenceData)seq.clone());
        }
        return newdataset;
    }      

    @Override
    public void importData(Data source) throws ClassCastException {
        super.importData(source); // this will also import the child sequences
        this.max=((NumericDataset)source).max;
        this.min=((NumericDataset)source).min;
        this.baseline=((NumericDataset)source).baseline;
        //notifyListenersOfDataUpdate(); 
    }
    

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}   

    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof NumericDataset)) return false;
        NumericDataset other=(NumericDataset)data;
        if (this.min!=other.min) return false;
        if (this.max!=other.max) return false;
        if (this.baseline!=other.baseline) return false;
        for (FeatureSequenceData sequence:getAllSequences()) {
            if (!sequence.containsSameData(other.getSequenceByName(sequence.getName()))) return false;
        }
        return false;
    }


    public static NumericDataset createNumericDatasetFromParameter(String parameter, String targetName, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        NumericDataset newDataItem=new NumericDataset(targetName);
        double[] min_max_baseline_default=new double[]{0.0f,1.0f,0.0f,0.0f};
        if (parameter!=null && !parameter.isEmpty()) {
              min_max_baseline_default=parseMinMaxBaselineDefaultParameters(parameter);
        }                 
        ((NumericDataset)newDataItem).setMinAllowedValue(min_max_baseline_default[0]);
        ((NumericDataset)newDataItem).setMaxAllowedValue(min_max_baseline_default[1]);
        ((NumericDataset)newDataItem).setBaselineValue(min_max_baseline_default[2]);
        SequenceCollection allSequences = engine.getDefaultSequenceCollection();
        ArrayList<Sequence> seqlist=allSequences.getAllSequences(engine);
        int size=seqlist.size();
        int i=0;
        for (Sequence sequence:seqlist) {
            i++;
            NumericSequenceData seq=new NumericSequenceData(sequence.getName(), sequence.getGeneName(), sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(), sequence.getTSS(), sequence.getTES(), sequence.getStrandOrientation(),min_max_baseline_default[3]);
            // set some default values also...
            ((NumericDataset)newDataItem).addSequence(seq);
            if (task!=null) {
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
                task.setProgress(i, size);
            }             
        }
        return newDataItem;
    }    
    


    public static double[] parseMinMaxBaselineDefaultParameters(String text) throws ExecutionError {
        double[] min_max_baseline_default=new double[]{0.0f,1.0f,0.0f,0.0f};
        boolean[] isDefined=new boolean[]{false,false,false,false};
        String[] parts=text.split(",");
        if (parts.length==1) { // only a single default value given
            try {
              min_max_baseline_default[3]=Double.parseDouble(parts[0]);
            } catch(Exception e) {throw new ExecutionError("Unable to parse parameter: "+text);}
        } else {
           for(String part:parts) {
               Pattern pattern=Pattern.compile("(min|max|baseline|value|default)\\s*=\\s*(\\S+)");
               Matcher matcher=pattern.matcher(part);
               if (matcher.find()) {
               String paramname=matcher.group(1);
               String valuestring=matcher.group(2);
                 try {
                   int index=0;
                        if (paramname.equals("min")) {index=0;isDefined[0]=true;}
                   else if (paramname.equals("max")) {index=1;isDefined[1]=true;}
                   else if (paramname.equals("baseline")) {index=2;isDefined[2]=true;}
                   else if (paramname.equals("value") || paramname.equals("default")) {index=3;isDefined[3]=true;}
                   double val=Double.parseDouble(valuestring);
                   min_max_baseline_default[index]=val;
                 } catch (Exception e) {throw new ExecutionError("Not a valid number: "+valuestring);}
               } else throw new ExecutionError("Unable to parse: "+part);
           }
        }
        if (min_max_baseline_default[0]>min_max_baseline_default[1]) throw new ExecutionError("Minimum value must be smaller than maximum value");
        if ((min_max_baseline_default[2]>min_max_baseline_default[1] || min_max_baseline_default[2]<min_max_baseline_default[0]) && isDefined[2] && isDefined[0] && isDefined[1]) throw new ExecutionError("Baseline value is outside min-max range");
        if ((min_max_baseline_default[3]>min_max_baseline_default[1] || min_max_baseline_default[3]<min_max_baseline_default[0]) && isDefined[3] && isDefined[0] && isDefined[1]) throw new ExecutionError("Default value is outside min-max range");
        return min_max_baseline_default;
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
