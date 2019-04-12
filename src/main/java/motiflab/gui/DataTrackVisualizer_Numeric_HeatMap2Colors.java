/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Sequence;

/**
 *
 * @author kjetikl
 */
public class DataTrackVisualizer_Numeric_HeatMap2Colors extends DataTrackVisualizer_Numeric {
       
    public DataTrackVisualizer_Numeric_HeatMap2Colors() {
        super();
    } 
    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);
        if (sequencedata!=null && !(sequencedata instanceof NumericSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}
    }     
    
    @Override
    public String getGraphTypeName() {
        return "Heatmap (2 colors)";
    }
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.NUMERIC_TRACK_TWOCOLOR_HEATMAP_ICON,null);
    }       
    
    public boolean isBaseLineRelative() {
        return false;
    }    
         
    
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {
        double scale=settings.getScale(sequenceName);                

        ColorGradient gradient=settings.getColorGradient(featureName);
        ColorGradient secondarygradient=settings.getSecondaryGradient(featureName);
        double maxvalue=1;
        double minvalue=0;
        double baselinevalue=0;
        int aboveHeight=0; // number of pixels for displaying values above baseline
        int belowHeight=0; // number of pixels for displaying values below baseline
        boolean drawBaseLine=false;
        double directionalMaximum=0; // 
        double aboveMaximum=0; // 
        double belowMaximum=0; //         
           
        maxvalue=((NumericSequenceData)sequencedata).getMaxAllowedValue();
        minvalue=((NumericSequenceData)sequencedata).getMinAllowedValue();
        baselinevalue=((NumericSequenceData)sequencedata).getBaselineValue();
          
        if (baselinevalue>minvalue) {
            double range=Math.abs(maxvalue-minvalue);
            aboveMaximum=Math.abs(maxvalue-baselinevalue);
            belowMaximum=Math.abs(baselinevalue-minvalue);
            directionalMaximum=Math.max(aboveMaximum, belowMaximum);
            aboveHeight=(int)Math.ceil((height-1)/2f); // baseline is placed in the middle
            belowHeight=height-(aboveHeight+1); 
            drawBaseLine=true;
        }
                
        int pixels=(optimize)?width:bases; // the number of pixels to draw in the graphics object (before affine transform)      
        // draw visible region
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
                    if (drawBaseLine) { // if 'drawBaseLine' => draw values above baseline in primary gradient and those below in secondary
                       double val=((Double)value).doubleValue();
                       if (val==baselinevalue) continue;
                       else if (val>baselinevalue) {
                           double relativeValue=(val-baselinevalue)/directionalMaximum;
                           graphics.setColor(gradient.getColor(relativeValue));
                           drawBase(graphics,graphicsXoffset+i, graphicsYoffset, height-1);
                       } else {
                           double relativeValue=Math.abs((baselinevalue-val)/directionalMaximum);
                           graphics.setColor(secondarygradient.getColor(relativeValue));
                           drawBase(graphics,graphicsXoffset+i, graphicsYoffset, height-1);
                       }
                    } else { // gradient graph without baseline (we suppose that all values are >= baseline!)
                       double relativeValue=((Double)value).doubleValue()/(maxvalue-baselinevalue);
                       graphics.setColor(gradient.getColor(relativeValue));
                       drawBase(graphics,graphicsXoffset+i, graphicsYoffset, height-1);
                    }                  
              } // end of (value instanceof Double)
          } // end for each position
    } // end of drawVisibleSegment
    
    
    private void drawBase(Graphics2D graphics,int x, int y, int height) {
        if (height>=0) graphics.fillRect(x, y, 1, height+1);
    }  
    
    
}