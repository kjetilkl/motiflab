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
public class DataTrackVisualizer_Numeric_HeatMapRainbow extends DataTrackVisualizer_Numeric {
       
    public DataTrackVisualizer_Numeric_HeatMapRainbow() {
        super();
    } 
    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);
        if (sequencedata!=null && !(sequencedata instanceof NumericSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}
    }     
    
    @Override
    public String getGraphTypeName() {
        return "Heatmap (rainbow)";
    }
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.NUMERIC_TRACK_RAINBOW_HEATMAP_ICON,null);
    }       
    
    public boolean isBaseLineRelative() {
        return false;
    }    
         
    
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {
        double scale=settings.getScale(sequenceName);                
        ColorGradient gradient=settings.getRainbowGradient();
        double maxvalue=maxvalue=((NumericSequenceData)sequencedata).getMaxAllowedValue();
        double minvalue=minvalue=((NumericSequenceData)sequencedata).getMinAllowedValue();       
        int pixels=(optimize)?width:bases; // the number of pixels to draw in the graphics object (before affine transform)      
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
                   double relativeValue=(((Double)value).doubleValue()-minvalue)/(maxvalue-minvalue);
                   graphics.setColor(gradient.getColor(relativeValue));
                   drawBase(graphics,graphicsXoffset+i, graphicsYoffset, height-1);                 
              } // end of (value instanceof Double)
          } // end for each position
    } // end of drawVisibleSegment
    
    
    private void drawBase(Graphics2D graphics,int x, int y, int height) {
        if (height>=0) graphics.fillRect(x, y, 1, height+1);
    }   
     
}