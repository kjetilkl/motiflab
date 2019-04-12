/*
 
 
 */

package motiflab.engine.data;

import java.util.Arrays;
import motiflab.engine.ExecutionError;

/**
 * This class holds feature data for a single sequence from a specified
 * region in a genome. The type of data contained in these objects are "wiggle data"
 * that specify a different value for each position along the sequence.
 * 
 * The data in this object is stored as an array of double values with one entry for each position in the sequence (direct orientation).
 * 
 * @author kjetikl
 */
public class NumericSequenceData extends FeatureSequenceData implements Cloneable {
    private double[] numericdata; //  The data should come from the direct strand so that numericdata[0] corresponds to the smaller genomic coordinate
    
    // the three values below should only be used to determine range for visualization purposes
    private double min=0; // these are only for local use. Query the parent to find dataset min/max/baseline values
    private double max=1;  // these are only for local use. Query the parent to find dataset min/max/baseline values
    private double baseline=0;  // these are only for local use. Query the parent to find dataset min/max/baseline values

    private static String typedescription="Numeric data";    
    
    /**
     * Constructs a new NumericSequenceData object from the supplied data
     * 
     * @param sequenceName
     * @param geneName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param TSS
     * @param TES
     * @param orientation
     * @param sequencedata
     */
    public NumericSequenceData(String sequenceName, String geneName, String chromosome, int startPosition, int endPosition, Integer TSS, Integer TES, int orientation, double[] numerictrack){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         // the geneName, TSS, TES and orientation attributes are no longer stored in FeatureSequenceData objects but only derived from Sequence objects. They are kept in the constructor for backwards compatibility
         this.numericdata=numerictrack;
         for (int i=0;i<numerictrack.length;i++) {
             if (numerictrack[i]>max) max=numerictrack[i];
             if (numerictrack[i]<min) min=numerictrack[i];
         }
    }
    
    /**
     * Constructs a new NumericSequenceData object from the supplied data
     * 
     * @param sequenceName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param sequencedata
     */
    public NumericSequenceData(String sequenceName, String chromosome, int startPosition, int endPosition, double[] numerictrack){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         this.numericdata=numerictrack;
         for (int i=0;i<numerictrack.length;i++) {
             if (numerictrack[i]>max) max=numerictrack[i];
             if (numerictrack[i]<min) min=numerictrack[i];
         }
    }    
      
    
    /**
     * Constructs a new "empty" NumericSequenceData object 
     * 
     * @param sequenceName
     * @param geneName
     * @param chromosome
     * @param startPosition
     * @param endPosition
     * @param TSS
     * @param orientation
     */
    public NumericSequenceData(String sequenceName, String geneName, String chromosome, int startPosition, int endPosition, Integer TSS, Integer TES, int orientation, double defaultvalue){
         setSequenceName(sequenceName);
         setChromosome(chromosome);
         setRegionStart(startPosition);
         setRegionEnd(endPosition);
         // the geneName,TSS,TES and orientation attributes are no longer stored in FeatureSequenceData objects but only derived from Sequence objects. They are kept in the constructor for backwards compatibility
         this.numericdata=new double[endPosition-startPosition+1];
         setAllPositionsToValue(defaultvalue);
         //for (int i=0;i<numericdata.length;i++) numericdata[i]=(double)Math.random();
    }
    
    /**
     * Creates a new initially "empty" NumericSequenceData object based on the given Sequence template
     * @param sequence
     */
    public NumericSequenceData(Sequence sequence, double defaultvalue) {
         setSequenceName(sequence.getName());
         setChromosome(sequence.getChromosome());
         setRegionStart(sequence.getRegionStart());
         setRegionEnd(sequence.getRegionEnd());
         this.numericdata=new double[endPosition-startPosition+1];
         setAllPositionsToValue(defaultvalue);
    }
    
    /** Resets the allowed min and max values based on the current data
     * the range will include the baseline. Note that the baseline value will always
     * be included in the returned range, so the two numbers might not necessarily reflect
     * the actual min and max values in the dataset
     * @return a double array with two components [0]=>min, [1]=>max
     */
    public double[] updateAllowedMinMaxValuesFromDataRange() {
       min=getBaselineValue();
       max=getBaselineValue();
       for (int i=0;i<numericdata.length;i++) {
         if (numericdata[i]>max) max=numericdata[i];
         if (numericdata[i]<min) min=numericdata[i];
       }       
       return new double[]{min,max};         
    }
    
    /** Returns min and max values based on the current data
     * @return a double array with two components [0]=>min, [1]=>max
     */
    public double[] getMinMaxFromData() {
       double datamin=Double.MAX_VALUE;
       double datamax=-Double.MAX_VALUE;
       for (int i=0;i<numericdata.length;i++) {
         if (numericdata[i]>datamax) datamax=numericdata[i];
         if (numericdata[i]<datamin) datamin=numericdata[i];
       }       
       return new double[]{datamin,datamax};         
    }
    
    /** Returns min and max values based on the current data within the range
     *  given as relative positions. getMinMaxFromData() is equal to getMinMaxFromData(0,sequencelength-1)
     * @param fromPos
     * @param toPos
     * @return 
     */
    public double[] getMinMaxFromData(int fromPos, int toPos) {
       double datamin=Double.MAX_VALUE;
       double datamax=-Double.MAX_VALUE;
       for (int i=fromPos;i<=toPos;i++) {
         if (numericdata[i]>datamax) datamax=numericdata[i];
         if (numericdata[i]<datamin) datamin=numericdata[i];
       }       
       return new double[]{datamin,datamax};         
    }    
    
    public final void setAllPositionsToValue(double defaultvalue)  { 
        for (int i=0;i<numericdata.length;i++) numericdata[i]=defaultvalue; 
        min=defaultvalue;
        max=defaultvalue;
    }
    
    @Override
    public Object getValue() {return numericdata;}
    
    @Override
     public Object getValueInGenomicInterval(int start, int end) {
        if (end<getRegionStart() || start>getRegionEnd() || end<start) return null;
        if (start<getRegionStart()) start=getRegionStart();
        if (end>getRegionEnd()) end=getRegionEnd();
        int length=end-start+1;
        int relativeStart=getRelativePositionFromGenomic(start);
        double[] subset=new double[end-start+1];
        for (int i=0;i<length;i++) {
            subset[i]=numericdata[relativeStart+i];
        }
        return subset;
    } 
    
    /**
     * Returns the data value at the specified position relative to the
     * start of the sequence (if position==0 it returns the value of the first 
     * position in the sequence.) If the position is outside the boundaries of the 
     * sequence it returns null
     * 
     * @return the data value at the specified position, or null
     */
    @Override
    public Double getValueAtRelativePosition(int position) {
        if (position<0 || position>=numericdata.length) return null;
        else return new Double(numericdata[position]);
    }

   /**
     * Returns the DNA base at the specified genomic position.
     * If the position is outside the boundaries of the sequence it returns null.
     * @param chromosome    
     * @param position
     * @return the DNA base at the specified position, or null
     */
    @Override
    public Double getValueAtGenomicPosition(String chromosome, int position) {
        if (!chromosome.equalsIgnoreCase(this.chromosome)) return null;
        if (position<this.startPosition || position>this.endPosition) return null;
        return new Double(numericdata[position-this.startPosition]);
    }

   /**
     * Returns the DNA base at the specified genomic position for the chromosome of this gene.
     * If the position is outside the boundaries of the sequence it returns null.
     * @param position
     * @return the DNA base at the specified position, or null
     */       
    @Override
    public Double getValueAtGenomicPosition(int position) {
        if (position<this.startPosition || position>this.endPosition) return null;
        return new Double(numericdata[position-this.startPosition]);
    }

   /**
     * Sets a new value at the specified genomic position
     * @param position The genomic position
     * @param value The new value at this position
     * @return FALSE if position is outside bounds of sequence else true
     */       
    public boolean setValueAtGenomicPosition(int position, double value) {
        if (position<this.startPosition || position>this.endPosition) return false;
        numericdata[position-this.startPosition]=value;
        if (value>max) max=value;
        if (value<min) min=value;
        if (parent!=null) {
            if (max>((NumericDataset)parent).getMaxAllowedValue()) ((NumericDataset)parent).setMaxAllowedValue(max);
            if (min<((NumericDataset)parent).getMinAllowedValue()) ((NumericDataset)parent).setMinAllowedValue(min);
        }
        return true;
    }
    
   /**
     * Sets a new value at the specified genomic position
     * @param chrom the chromsome
     * @param position The genomic position
     * @param value The new value at this position
     * @return FALSE if position is outside bounds of sequence else true
     */       
    public boolean setValueAtGenomicPosition(String chrom, int position, double value) {
        if (chrom==null || chrom.equals(this.chromosome)) return false;
        if (position<this.startPosition || position>this.endPosition) return false;
        numericdata[position-this.startPosition]=value;
        if (value>max) max=value;
        if (value<min) min=value;
        if (parent!=null) {
            if (max>((NumericDataset)parent).getMaxAllowedValue()) ((NumericDataset)parent).setMaxAllowedValue(max);
            if (min<((NumericDataset)parent).getMinAllowedValue()) ((NumericDataset)parent).setMinAllowedValue(min);
        }
        return true;
    }

   /**
     * Sets a new value at the specified position relative to the start of the sequence
     * @param position The genomic position
     * @param value The new value at this position
     * @return FALSE if position is outside bounds of sequence else true
     */       
    public boolean setValueAtRelativePosition(int position, double value) {
        if (position<0 || position>=numericdata.length) return false;
        numericdata[position]=value;
        if (value>max) max=value;
        if (value<min) min=value;
        if (parent!=null) {
            if (max>((NumericDataset)parent).getMaxAllowedValue()) ((NumericDataset)parent).setMaxAllowedValue(max);
            if (min<((NumericDataset)parent).getMinAllowedValue()) ((NumericDataset)parent).setMinAllowedValue(min);
        }
        return true;
    }

   
    @Override
    public void setParent(FeatureDataset parent) {
        this.parent=parent;
        if (parent==null) return;
        if (max>((NumericDataset)parent).getMaxAllowedValue()) ((NumericDataset)parent).setMaxAllowedValue(max);
        if (min<((NumericDataset)parent).getMinAllowedValue()) ((NumericDataset)parent).setMinAllowedValue(min);
    }    
    
   /**
     * Returns the maximum value allowed for this dataset
     * @return the maximum allowed value
     */       
    public Double getMaxAllowedValue() {
        if (parent==null) return max;
        else return ((NumericDataset)parent).getMaxAllowedValue();
    }

   /**
     * Returns the minimum value allowed for this dataset
     * @return the minimum allowed value
     */       
    public Double getMinAllowedValue() {
        if (parent==null) return min;
        else return ((NumericDataset)parent).getMinAllowedValue();
    }

    /** Returns the smallest value found in this sequence, or the baseline value (if this is smaller still) */
    public double getMinValueFromThisSequence() {
        return min;
    }

    /** Returns the largest value found in this sequence, or the baseline value (if this is greater still) */
    public double getMaxValueFromThisSequence() {
        return max;
    }
    
    /** Returns the baseline value from this sequence */
    public double getBaselineValueFromThisSequence() {
        return baseline;
    }    

   /**
     * Returns the baseline value for this dataset
     * @return the baseline value
     */       
    public Double getBaselineValue() {
        if (parent==null) return baseline;
        else return ((NumericDataset)parent).getBaselineValue();
    }
        
    public void setData(double[] data) { // replaces the current array of values with the one provided
        numericdata=data;
        min=baseline; max=baseline;
        for (int i=0;i<numericdata.length;i++) {
          if (numericdata[i]>max) max=numericdata[i];       
          else if (numericdata[i]<min) min=numericdata[i]; 
        }  
        if (parent!=null) {
            if (max>((NumericDataset)parent).getMaxAllowedValue()) ((NumericDataset)parent).setMaxAllowedValue(max);
            if (min<((NumericDataset)parent).getMinAllowedValue()) ((NumericDataset)parent).setMinAllowedValue(min);
        }
    }
      
    public double[] getData() {
       return numericdata;   
    }
    
    /**
     * Returns the smallest value in this Numeric sequence within the specified interval
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The minimum value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return NULL
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getMinValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double minval=Double.MAX_VALUE;     
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        for (int i=start;i<=end;i++) {
            if (numericdata[i]<minval) minval=numericdata[i];
        }
        return minval;
    }
    
    /**
     * Returns the largest value in this Numeric sequence within the specified interval
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The maximum value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return NULL
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getMaxValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap        
        if (end<0 || start>numericdata.length-1) return null;
        double maxval=-Double.MAX_VALUE; // Note: Double.MIN_VALUE is the smallest POSITIVE number!
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        for (int i=start;i<=end;i++) {
            if (numericdata[i]>maxval) maxval=numericdata[i];
        }
        return maxval;
    }

    /**
     * Returns the value with largest magnitude in this Numeric sequence within the specified interval
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The most extreme value in the interval between start and end (inclusive), i.e. the value with largest absolute value
     *         If the interval lies fully outside the defined sequence the function will return NULL
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getExtremeValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null; // outside range
        double extreme=0;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        for (int i=start;i<=end;i+=1) {
            if (Math.abs(numericdata[i])>Math.abs(extreme)) extreme=numericdata[i];
        }
        return extreme;
    }
    
   /** This method does the same as the getExtremeValueInInterval(start,end) method
     * except that when the size of the segment is larger than the cutoff, the value
     * will not be the extreme from all positions within the segment but rather the
     * extreme of a limited number of positions sampled at equal distances throughout
     * the interval
     * @param start
     * @param end
     * @param cutoff The size threshold above which sampling should be performed rather than including all positions
     * @param sample The number of positions to sample in the interval 
     */
    public Double getExtremeValueInInterval(int start, int end, int cutoff, int sample) {        
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null; // outside range     
        double extreme=0;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        int step=(end-start<=cutoff)?1:(int)((end-start)/sample); //
        if (step<=0) step=1;
        for (int i=start;i<=end;i+=step) {
            if (Math.abs(numericdata[i])>Math.abs(extreme)) extreme=numericdata[i];
        }
        return extreme;
    } 
    
    public Double getCenterValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null; // outside range
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;    
        int centerPosition=start+(end-start)/2;
        return numericdata[centerPosition];
    } 
    
    /**
     * Returns the average value within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The average value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return null
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getAverageValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double avg=0;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        double length=end-start+1;
        for (int i=start;i<=end;i++) {
            avg+=numericdata[i];
        }        
        avg=avg/length; // note that length can not be 0 since length=end-start+1 (and start is always smaller than end)
        return avg;
    }
    
    /**
     * This method does the same as the getAverageValueInInterval(start,end) method
     * except that when the size of the segment is larger than the cutoff, the value
     * will not be the average from all positions within the segment but rather the
     * average of a limited number of positions sampled at equal distances throughout
     * the interval
     * @param start
     * @param end
     * @param cutoff The size threshold above which sampling should be performed rather than including all positions
     * @param sample The number of positions to sample in the interval
     * @return 
     */
    public Double getAverageValueInInterval(int start, int end, int cutoff, int sample) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double avg=0;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        double length=0;
        int step=(end-start<=cutoff)?1:(int)((end-start)/sample); // if the segment is very long, sample only a few positions for efficiency
        if (step<=0) step=1;        
        for (int i=start;i<=end;i+=step) {
            avg+=numericdata[i]; length++;
        }        
        return (length>0)?(avg=avg/length):avg; 
    }    

    /**
     * Returns the sum of values within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The sum of the values in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return NULL
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getSumValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double sum=0;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        for (int i=start;i<=end;i++) {
            sum+=numericdata[i];
        }
        return sum;
    }
    /**
     * Returns the product of values within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The product of the values in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return NULL
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getProductValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double product=1.0f;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        for (int i=start;i<=end;i++) {
            product*=numericdata[i];
        }
        return product;
    }

    /**
     * Returns the median value within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The median value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return null
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getMedianValueInInterval(int start, int end) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double median=0;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        int length=end-start+1;
        double[] buffer=new double[length];  
        int j=0;
        for (int i=start;i<=end;i++) {
            buffer[j]=numericdata[i];
            j++;
        }
        Arrays.sort(buffer);
        if (length%2==0) { // even number of values. Note that the length can NOT be 0!
          int index=length/2;
          median=(buffer[index-1]+buffer[index])/2;
        } else { // odd number of values
          median=buffer[(int)length/2];  
        }
        return median;
    }

    /**
     * Returns the smallest weighted value in this Numeric sequence within the specified interval
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param weights An array containing weights. The value in each position in the sequence (pos=start+i) will be weighted by the corresponding value in this array (at pos=i)
     * @return The weighted minimum value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return NULL
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getWeightedMinValueInInterval(int start, int end, double[] weights) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double minval=Double.MAX_VALUE;     
        for (int i=0;i<weights.length;i++) {
            int pos=start+i;
            if (pos<0 || pos>=numericdata.length) continue;
            double weightedvalue=numericdata[pos]*weights[i];
            if (weightedvalue<minval) minval=weightedvalue;
        }
        return minval;
    }  
    
    /**
     * Returns the largest weighted value in this Numeric sequence within the specified interval
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param weights An array containing weights. The value in each position in the sequence (pos=start+i) will be weighted by the corresponding value in this array (at pos=i)
     * @return The weighted maximum value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return NULL
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getWeightedMaxValueInInterval(int start, int end, double[] weights) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double maxval=-Double.MAX_VALUE;     
        for (int i=0;i<weights.length;i++) {
            int pos=start+i;
            if (pos<0 || pos>=numericdata.length) continue;
            double weightedvalue=numericdata[pos]*weights[i];
            if (weightedvalue>maxval) maxval=weightedvalue;
        }
        return maxval;
    }     
    
     /**
     * Returns the weighted average value within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param weights An array containing weights. The value in each position in the sequence (pos=start+i) will be weighted by the corresponding value in this array (at pos=i)
     * @return The weighted average value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return null
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getWeightedAverageValueInInterval(int start, int end, double[] weights) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double sum=0;
        if (weights==null || weights.length!=(end-start+1)) return null;
        double weightssum=0;
        for (int i=0;i<weights.length;i++) weightssum+=weights[i];
        if (weightssum==0) return sum;
        for (int i=0;i<weights.length;i++) {
            int pos=start+i;
            if (pos<0 || pos>=numericdata.length) continue;
            sum+=(numericdata[pos]*(weights[i]/weightssum)); // the weights are normalized here so they sum to 1 by diving by the sum
        }
        return sum; // 
    }

     /**
     * Returns the weighted sum value within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param weights An array containing weights. The value in each position in the sequence (pos=start+i) will be weighted by the corresponding value in this array (at pos=i)
     * @return The weighted sum value in the interval between start and end (inclusive).     
     *         If the interval lies fully outside the defined sequence the function will return null
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getWeightedSumValueInInterval(int start, int end, double[] weights) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double sum=0;
        if (weights==null || weights.length!=(end-start+1)) return null;
        for (int i=0;i<weights.length;i++) {
            int pos=start+i;
            if (pos<0 || pos>=numericdata.length) continue;
            sum+=(numericdata[pos]*weights[i]);
        }
        return sum;
    }
    
     /**
     * Returns the weighted product value within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param weights An array containing weights. The value in each position in the sequence (pos=start+i) will be weighted by the corresponding value in this array (at pos=i)
     * @return The weighted product value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return null
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getWeightedProductValueInInterval(int start, int end, double[] weights) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double product=1f;
        if (weights==null || weights.length!=(end-start+1)) return null;
        for (int i=0;i<weights.length;i++) {
            int pos=start+i;
            if (pos<0 || pos>=numericdata.length) continue;
            product*=(numericdata[pos]*weights[i]);
        }
        return product;
    }

    /**
     * Returns the median value within the specified interval in this Numeric sequence 
     * @param start The start position of the interval (offset from beginning of sequence, i.e. relative position)
     * @param end The end position of the interval (offset from beginning of sequence, i.e. relative position)
     * @return The weighted median value in the interval between start and end (inclusive).
     *         If the interval lies fully outside the defined sequence the function will return null
     *         If the interval lies partially outside the defined sequence, only the covered region will be considered
     *
     */
    public Double getWeightedMedianValueInInterval(int start, int end, double[] weights) {
        if (end<start) {int end2=end;end=start;start=end2;} // swap           
        if (end<0 || start>numericdata.length-1) return null;
        double median=0; 
        int seqstart=start;
        if (start<0) start=0;
        if (end>numericdata.length-1) end=numericdata.length-1;
        int length=end-start+1;
        double[] buffer=new double[length];  
        for (int i=0;i<=weights.length;i++) {
            int pos=seqstart+i;
            if (pos<0 || pos>=numericdata.length) continue;            
            buffer[i]=(numericdata[pos]*weights[i]);
        }
        Arrays.sort(buffer);
        if (length%2==0) { // even number of values. Note that the length can NOT be 0!
          int index=length/2;
          median=(buffer[index-1]+buffer[index])/2;
        } else { // odd number of values
          median=buffer[(int)length/2];  
        }
        return median;
    }   
    
    /**
     * Returns a deep copy of this NumericSequenceData object
     */
    @Override
    public NumericSequenceData clone() {
        double[] newvalues=new double[numericdata.length];
        System.arraycopy(numericdata, 0, newvalues, 0, numericdata.length);
        NumericSequenceData newdata=new NumericSequenceData(sequenceName, chromosome, startPosition, endPosition, newvalues);
        newdata.max=this.max;
        newdata.min=this.min;
        newdata.baseline=this.baseline;
        newdata.parent=this.parent;
        return newdata;
    }    

    
    @Override
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        this.max=((NumericSequenceData)source).max;
        this.min=((NumericSequenceData)source).min;
        this.baseline=((NumericSequenceData)source).baseline;
        setData(((NumericSequenceData)source).numericdata);
        //notifyListenersOfDataUpdate(); 
    }
    
    @Override  
    public void cropTo(int relativeStart, int relativeEnd) throws ExecutionError {
        if (relativeStart<0 || relativeStart>=numericdata.length || relativeEnd<0 || relativeEnd>=numericdata.length || relativeEnd<relativeStart) throw new ExecutionError("Crop out of bounds");
        if (relativeStart==0 && relativeEnd==numericdata.length-1) return;
        double[] newnumericdata=Arrays.copyOfRange(numericdata, relativeStart, relativeEnd+1);
        numericdata=newnumericdata;  
        int oldstart=getRegionStart();
        setRegionStart(oldstart+relativeStart);         
        setRegionEnd(oldstart+relativeEnd);         
    }  
    
    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }
    
    @Override
    public String getTypeDescription() {return typedescription;}

    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof NumericSequenceData)) return false;
        if (!super.containsSameData(other)) return false;
        if (this.max!=((NumericSequenceData)other).max) return false;
        if (this.min!=((NumericSequenceData)other).min) return false;
        if (this.baseline!=((NumericSequenceData)other).baseline) return false;
        return (Arrays.equals(this.numericdata,((NumericSequenceData)other).numericdata));
    }


    /** Returns a hashcode based on the contents of the numeric sequence */
    public int getDebugHashCode() {
        return Arrays.hashCode(numericdata);
    }

    /** Outputs information about this dataset into the buffer */
    public StringBuilder debug(StringBuilder builder) {
        if (builder==null) builder=new StringBuilder();
        builder.append(" - "+getName()+" : "+getRegionAsString()+", size="+getSize()+", buffersize="+((numericdata==null)?0:numericdata.length)+", dataDebugHashcode="+getDebugHashCode()+",   ["+System.identityHashCode(this)+"]");
        return builder;
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
