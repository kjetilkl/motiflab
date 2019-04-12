/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Sequence;

/**
 *
 * @author kjetikl
 */
public class DataTrackVisualizer_Numeric_GradientBarGraph extends DataTrackVisualizer_Numeric {
    

    public DataTrackVisualizer_Numeric_GradientBarGraph() {
        super();
    } 
    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);
        if (sequencedata!=null && !(sequencedata instanceof NumericSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}
    }     
    
    @Override
    public String getGraphTypeName() {
        return "Graph (gradient)";
    }
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.NUMERIC_TRACK_GRADIENT_BAR_ICON,null);
    }       
    
    public boolean isBaseLineRelative() {
        return true;
    }    
    
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {
        double scale=settings.getScale(sequenceName);                
        Color foreground=Color.BLACK;
        GradientPaint bar_gradient_pos=null; // return new java.awt.GradientPaint(0, y, current, 0, y+(height/2f), lightercolor, true);
        GradientPaint bar_gradient_neg=null;
        double maxvalue=1;
        double minvalue=0;
        double baselinevalue=0;
        int aboveHeight=0; // number of pixels for displaying values above baseline
        int belowHeight=0; // number of pixels for displaying values below baseline
        boolean drawBaseLine=false;
        double directionalMaximum=0; // 
        double aboveMaximum=0; // 
        double belowMaximum=0; //         
        foreground=settings.getForeGroundColor(featureName);        
        
        maxvalue=((NumericSequenceData)sequencedata).getMaxAllowedValue();
        minvalue=((NumericSequenceData)sequencedata).getMinAllowedValue();
        baselinevalue=((NumericSequenceData)sequencedata).getBaselineValue();
                      
        if (baselinevalue>minvalue) {
            double range=Math.abs(maxvalue-minvalue);
            aboveMaximum=Math.abs(maxvalue-baselinevalue);
            belowMaximum=Math.abs(baselinevalue-minvalue);
            directionalMaximum=Math.max(aboveMaximum, belowMaximum);
            aboveHeight=(int)Math.ceil((aboveMaximum/range)*(height-1)); // baseline is placed relative to range
            belowHeight=height-(aboveHeight+1); 
            drawBaseLine=true;
        }
                
        int pixels=(optimize)?width:bases; // the number of pixels to draw in the graphics object (before affine transform)
        if (drawBaseLine) {
            bar_gradient_pos=new java.awt.GradientPaint(graphicsXoffset, graphicsYoffset, foreground, graphicsXoffset, graphicsYoffset+aboveHeight, settings.getSecondaryColor(featureName), true);
            bar_gradient_neg=new java.awt.GradientPaint(graphicsXoffset, graphicsYoffset+aboveHeight, settings.getSecondaryColor(featureName), graphicsXoffset, graphicsYoffset+height,foreground, true);
        } else {
            bar_gradient_pos=new java.awt.GradientPaint(graphicsXoffset, graphicsYoffset, foreground, graphicsXoffset, graphicsYoffset+height, settings.getSecondaryColor(featureName), true);
        }        
        
        // draw visible region
        //gui.logMessage(sequenceName+":"+featureName+"   "+pixels+" pixels from "+start+" to "+(start+(int)((pixels/scale)+0.5))+"   width="+bases+"   genomic start="+sequencedata.getGenomicPositionFromRelative(start)+" to "+sequencedata.getGenomicPositionFromRelative((start+(int)((pixels/scale)+0.5))));
        for (int i=0;i<pixels;i++) { 
              Object value=null;
              if (optimize) {
                  int step=i;
                  if (orientation==Sequence.REVERSE) step=(pixels-1)-i;
                  int pos=start+(int)((step/scale)+0.5);
                  int pos2=start+(int)(((step+1)/scale)+0.5)-1;
                  value=getRepresentativeValueInRegion(pos,pos2);
              } else {
                int pos=start+i; // relative position within sequence. Use direct orientation. An affine transformation is applied to reverse it if necessary
                value=sequencedata.getValueAtRelativePosition(pos);
              }
              if (value instanceof Double) { // bargraph or gradient. Instead of drawing a single solid line, finetune the "tip" of the bar with a gradient color to get more precision
                    if (drawBaseLine) { // track has both positive and negative values. Positive values must be drawn above the baseline and negative values below.
                       double val=((Double)value).doubleValue();
                       if (val==baselinevalue) continue; // this will be hidden behind the baseline anyway
                       else if (val>baselinevalue) { // value above baseline
                           double relativeValue=Math.abs((val-baselinevalue)/aboveMaximum);
                           int solidbarheight=(int)Math.floor(aboveHeight*relativeValue); // round down to nearest pixel
                           if (solidbarheight>0) {
                               ((Graphics2D)graphics).setPaint(bar_gradient_pos);
                               drawBase(graphics,graphicsXoffset+i, graphicsYoffset+aboveHeight-(solidbarheight),  solidbarheight-1);
                           }
                      } else { // value below baseline
                           double relativeValue=Math.abs((baselinevalue-val)/belowMaximum);
                           int solidbarheight=(int)Math.floor(belowHeight*relativeValue); // round down to nearest pixel
                           if (solidbarheight>0) { // draw the solid bar itself
                               ((Graphics2D)graphics).setPaint(bar_gradient_neg);
                               drawBase(graphics,graphicsXoffset+i, (graphicsYoffset+aboveHeight), solidbarheight);
                           }
                      }
                    } else { // Bar graph without baseline. Only need to draw positive values.
                       double relativeValue=((Double)value).doubleValue()/maxvalue;
                       int solidbarheight=(int)Math.floor(height*relativeValue); // round down to nearest pixel
                       if (solidbarheight>0) {
                           ((Graphics2D)graphics).setPaint(bar_gradient_pos); 
                           drawBase(graphics,graphicsXoffset+i, graphicsYoffset+height-(solidbarheight),  solidbarheight-1);
                       }
                    }
                  
              } // end of (value instanceof Double)
        } // end for each position
       
        // draw baseline
        if (drawBaseLine) {     
            graphics.setColor(settings.getBaselineColor(featureName));
            if (optimize) {
                int vizStart=settings.getSequenceViewPortStart(sequenceName);
                int vizEnd=settings.getSequenceViewPortEnd(sequenceName);
                if (vizStart<sequenceStart) vizStart=sequenceStart;
                if (vizEnd>sequenceEnd) vizEnd=sequenceEnd;              
                int first[] = getScreenCoordinateFromGenomic(vizStart);
                int last[]  = getScreenCoordinateFromGenomic(vizEnd);
                int startX  = (first[0]<=last[0])?first[0]:last[0];
                int endX    = (first[0]<=last[0])?last[1]:first[1];
                graphics.drawLine(graphicsXoffset+startX, graphicsYoffset+aboveHeight, graphicsXoffset+endX, graphicsYoffset+aboveHeight);
            } else {       
                graphics.fillRect(graphicsXoffset, graphicsYoffset+aboveHeight, pixels, 1);               
            }
        }
    } // end of drawVisibleSegment
    
    
    
    private void drawBase(Graphics2D graphics,int x, int y, int height) {
        if (height<0) return;
        graphics.fillRect(x, y, 1, height+1); // Note: The reason for drawing bases with width=2 is to avoid streaking artifacts which can appear at some scales due to "bugs" (?) in the affine transform.    +1 to ?.
    }
       
    
    
}