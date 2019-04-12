/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Sequence;

/**
 * This is the abstract superclass for DataTrackVisualizers used to render Numeric Dataset tracks
 * @author kjetikl
 */
public abstract class DataTrackVisualizer_Numeric extends DataTrackVisualizer {
             
    // The following settings are used for optimization on large zoom scales 
    // when a single pixel represents an interval rather than a single position.
    public static final int DISPLAY_EXTREME_VALUE=0;
    public static final int DISPLAY_AVERAGE_VALUE=1;
    public static final int DISPLAY_CENTER_VALUE=2;
    
    public static int display_value_setting=DISPLAY_EXTREME_VALUE;
    public static int sampling_length_cutoff=1000; // If a pixel represents more than this number of pixels, the value will be based on a sampling of positions rather than all positions in the interval
    public static int sampling_size=20; // If 'sampling' is used, this is the number of positions that will be sampled in each interval
       
   
    public DataTrackVisualizer_Numeric() {
        super();
    } 
    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);
        if (sequencedata!=null && !(sequencedata instanceof NumericSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}
    }     
    
    /** 
     * Returns a representative value from a region to use for display. This is relevant when a single pixel in the viewport covers more than one sequence position.
     * The representative value could either be the single value at the center of the region, the average over all values in the region (or a sampled subset) or the most extreme value in the region (or over a sampled subset).
     * The value which is returned will depend on the static settings "display_value_setting", "sampling_length_cutoff" and "sampling_size" from this class. 
     * 
     * @param relativeStart The start position of the segment covered by the pixel (relative to start of sequence)
     * @param relativeEnd The end position of the segment covered by the pixel (relative to start of sequence)
     */
    public Double getRepresentativeValueInRegion(int relativeStart, int relativeEnd) {
          if (DataTrackVisualizer_Numeric.display_value_setting==DataTrackVisualizer_Numeric.DISPLAY_EXTREME_VALUE) return ((NumericSequenceData)sequencedata).getExtremeValueInInterval(relativeStart, relativeEnd, DataTrackVisualizer_Numeric.sampling_length_cutoff, DataTrackVisualizer_Numeric.sampling_size);
          else if (DataTrackVisualizer_Numeric.display_value_setting==DataTrackVisualizer_Numeric.DISPLAY_AVERAGE_VALUE) return ((NumericSequenceData)sequencedata).getAverageValueInInterval(relativeStart, relativeEnd, DataTrackVisualizer_Numeric.sampling_length_cutoff, DataTrackVisualizer_Numeric.sampling_size);
          else return ((NumericSequenceData)sequencedata).getCenterValueInInterval(relativeStart, relativeEnd);     
    }
    
    
    /** Subclasses that draw graphs where the vertical position of the baseline is determined by the range should return TRUE in this method.
     *  Graph types where the baseline is always in the middle should return FALSE.
     */
    public abstract boolean isBaseLineRelative();
        
       
    @Override
    public boolean optimizeForSpeed(double scale) {
        return (scale<optimizationThreshold);
    }    
    
    @Override
    public void describeBaseForTooltip(int genomicCoordinate, int shownOrientation, StringBuilder buffer) {
        if (sequencedata==null) return;
        Object value=sequencedata.getValueAtGenomicPosition(genomicCoordinate);    
        if (value instanceof Double) { 
              buffer.append(featureName);
              buffer.append(" = ");
              buffer.append(value.toString());
              buffer.append(",&nbsp;&nbsp;&nbsp;&nbsp;shown range=[");
              boolean individual = settings.scaleShownNumericalRangeByIndividualSequence(featureName);
              if (individual) {
                  buffer.append(((NumericSequenceData)sequencedata).getMinValueFromThisSequence());
                  buffer.append(",");
                  buffer.append(((NumericSequenceData)sequencedata).getMaxValueFromThisSequence());
                  buffer.append("]");                 
              } else {
                  buffer.append(((NumericSequenceData)sequencedata).getMinAllowedValue());
                  buffer.append(",");
                  buffer.append(((NumericSequenceData)sequencedata).getMaxAllowedValue());
                  buffer.append("]");
              }
        } else if (value==null) {
              if (genomicCoordinate>=sequenceStart && genomicCoordinate<=sequenceEnd) buffer.append("NULL!");
              else buffer.append("Outside sequence:  "+sequenceStart+"-"+sequenceEnd+"  ("+(sequenceEnd-sequenceStart+1)+" bp)");            
        }        
    }     
    
    /*
     * This default implementation draws a solid color background corresponding to the visible sequence segment
     */
    @Override    
    public void drawVisibleSegmentBackground(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {      
        Color background=settings.getBackGroundColor(featureName);
        if (background==null) return;
        graphics.setColor(background);
        AffineTransform saveTransform=graphics.getTransform();
        graphics.setTransform(saveAt); // restores 'original state' (saveAt is set in superclass)
        double scale=settings.getScale(sequenceName); 
        int pixels=(optimize)?width:bases;  
        int leftOffset=graphicsXoffset;
        int vizStart=settings.getSequenceViewPortStart(sequenceName);
        int vizEnd=settings.getSequenceViewPortEnd(sequenceName);
        if (orientation==Sequence.REVERSE) {
           if (vizEnd>sequenceEnd && !optimize) leftOffset+=(int)(scale*(vizEnd-sequenceEnd));
        } else {
           if (vizStart<sequenceStart && !optimize) leftOffset+=(int)(scale*(sequenceStart-vizStart));
        }         
        if (vizStart<sequenceStart) vizStart=sequenceStart;
        if (vizEnd>sequenceEnd) vizEnd=sequenceEnd;          
        if (optimize) {          
            int first[] = getScreenCoordinateFromGenomic(vizStart);
            int last[]  = getScreenCoordinateFromGenomic(vizEnd);
            int startX  = (first[0]<=last[0])?first[0]:last[0];
            int endX    = (first[0]<=last[0])?last[1]:first[1];
            graphics.fillRect(leftOffset+startX, graphicsYoffset, endX-startX+1, height);                                
        } else {
            graphics.fillRect(leftOffset, graphicsYoffset, (int)(pixels*scale), height);           
        }
        graphics.setTransform(saveTransform);     
    }      
    
//    ---------------   Functionality for the "Draw tool"   ------------------------------
            
    private Double oldvalue=null;
    private Integer oldX=null;
    private NumericSequenceData buffer;
    private NumericSequenceData original;

    
    @Override   
    public void mousePressed(MouseEvent e) {
       if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return;
       if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
             original=(NumericSequenceData)sequencedata;
             buffer=(NumericSequenceData)sequencedata.clone();
             sequencedata=buffer; // 'buffer' is not referenced in the engine. We switch the current 'reference' (sequencedata) to point to this buffer
             mouseDragged(e); // this is just to paint at the first pixel (before dragging has commenced)         
       }        
    } 
    
    @Override    
    public void mouseReleased(MouseEvent e) {
       if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return;
       if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
            // commit changes to engine. using the gui's "updateDataItem" item
            oldvalue=null;
            oldX=null;
            gui.updatePartialDataItem(featureName, sequenceName, null, buffer); // update the registered dataset with the new data from the buffer (this will update the whole dataset not just the single sequence).
            sequencedata=original; // and point back to the (new) data now registered with the engine instead of the temporary buffer which is not registered
            this.repaint();
       }       
    } 
    
    @Override    
    public void mouseDragged(MouseEvent e) {
        if (gui.getVisualizationPanel().isMouseEventsBlocked() || SwingUtilities.isRightMouseButton(e)) return;
        if (!gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) return;
        boolean useLocalRange=settings.scaleShownNumericalRangeByIndividualSequence(featureName);       
        double maxvalue=0;
        double minvalue=0;
        if (useLocalRange) {
            maxvalue=((NumericSequenceData)sequencedata).getMaxValueFromThisSequence();
            minvalue=((NumericSequenceData)sequencedata).getMinValueFromThisSequence();       
        } else {
            maxvalue=((NumericSequenceData)sequencedata).getMaxAllowedValue();
            minvalue=((NumericSequenceData)sequencedata).getMinAllowedValue();
        } 
        int y=e.getY();
        int[] range=getGenomicCoordinateFromScreenOffset(e.getX()-bordersize); // remember to subtract the border when calculating relative X-coordinate
        double value=getValueForY(y);
        if (value>maxvalue && !e.isShiftDown()) value=maxvalue;
        else if (value<minvalue && !e.isShiftDown()) value=minvalue;
        if (oldX==null) {
            oldX=range[0];
            int newX=range[1];
            oldvalue=value;
            for (int i=oldX;i<=newX;i++) ((NumericSequenceData)sequencedata).setValueAtGenomicPosition(i, value);
        } else {
            int newX=(oldX<range[1])?range[1]:range[0];
            if (newX==oldX) {
                ((NumericSequenceData)sequencedata).setValueAtGenomicPosition(newX, value);
            } else {
                double increment=(value-oldvalue)/Math.abs(newX-oldX);
                double current=oldvalue;
                int inc=(newX<oldX)?-1:1;
                for (int i=oldX;i!=newX;i+=inc) {
                    ((NumericSequenceData)sequencedata).setValueAtGenomicPosition(i, current);
                    current+=increment;
                }
            }
            oldX=newX;
            oldvalue=value;            
            gui.statusMessage(featureName+" = "+value);           
        }
        repaintVisibleSequenceSegment(); // this is used instead of regular repaint() to avoid artifacts that appeared on the flanks around the sequence
    }  
    
    
    @Override    
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e); // necessary to update tooltips
        if (gui.getVisualizationPanel().isMouseEventsBlocked()) {return;}         
        if (!gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) return;
        int y=e.getY();
        int height=settings.getDataTrackHeight(featureName); 
        double value=getValueForY(y);
        gui.statusMessage((y<=height)?(featureName+" = "+value):"");  
    }   
    
    
    @Override    
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) gui.statusMessage("");
    }
    
   /** Returns the track value corresponding to the given mouse Y position within the track
     * An y value of 0 should correspond to 
     */
    private double getValueForY(int y) {
        Border border=getBorder();
        if (border!=null) {
          y-=getBorder().getBorderInsets(this).top;
        }    
        double maxvalue=0;
        double minvalue=0;
        double baselinevalue=0;       
        boolean useLocalRange=settings.scaleShownNumericalRangeByIndividualSequence(featureName);        
        if (useLocalRange) {
            maxvalue=((NumericSequenceData)sequencedata).getMaxValueFromThisSequence();
            minvalue=((NumericSequenceData)sequencedata).getMinValueFromThisSequence();
            baselinevalue=((NumericSequenceData)sequencedata).getBaselineValueFromThisSequence();            
        } else {
            maxvalue=((NumericSequenceData)sequencedata).getMaxAllowedValue();
            minvalue=((NumericSequenceData)sequencedata).getMinAllowedValue();
            baselinevalue=((NumericSequenceData)sequencedata).getBaselineValue();
        } 
        int height=settings.getDataTrackHeight(featureName);
        if (baselinevalue>minvalue) { // baseline is used
            double range=Math.abs(maxvalue-minvalue);
            double aboveRange=Math.abs(maxvalue-baselinevalue); // magnitude above baseline
            double belowRange=Math.abs(baselinevalue-minvalue); // magnitude below baseline
            //double directionalMaximum=Math.max(aboveMaximum, belowMaximum);
            double aboveHeight=0;
            boolean relativeBaseLine=isBaseLineRelative();
            if (relativeBaseLine) aboveHeight=(int)Math.ceil((aboveRange/range)*(height-1)); // baseline is placed relative to range
            else aboveHeight=(int)Math.ceil((height-1)/2f); // baseline is placed in the middle
            double belowHeight=height-(aboveHeight+1); 
            //gui.logMessage("belowHeight="+belowHeight+"    y="+y+"  height="+height+"  fraction="+((y-aboveHeight)/belowHeight)+"  line="+(y-aboveHeight));
            if (y==aboveHeight) return baselinevalue; //baselinevalue;
            else if (y<=aboveHeight) return ((aboveHeight-y)/(double)aboveHeight)*aboveRange+baselinevalue;
            else return (belowHeight>0)?(minvalue+(height-(y+1))*(belowRange/belowHeight)):(minvalue+(height-(y+1))*(range/height));
                        
        } else { // baseline not used
            return (maxvalue-minvalue)*((height-y)/(double)height);
        }   
    }
    
    
    
    
    
  
    
}
