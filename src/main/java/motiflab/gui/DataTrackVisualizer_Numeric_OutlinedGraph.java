/*
 
 
 */

package motiflab.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Sequence;

/**
 *
 * @author kjetikl
 */
public class DataTrackVisualizer_Numeric_OutlinedGraph extends DataTrackVisualizer_Numeric {
       
    private Stroke THIN_LINE=new BasicStroke(1f);      
    
    public DataTrackVisualizer_Numeric_OutlinedGraph() {
        super();
    } 
    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);
        if (sequencedata!=null && !(sequencedata instanceof NumericSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}
    }     
    
    @Override
    public String getGraphTypeName() {
        return "Graph (outlined)";
    }
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.NUMERIC_TRACK_OUTLINED_GRAPH_ICON,null);
    }       
    
    public boolean isBaseLineRelative() {
        return true;
    }    
         
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {  
       double scale=settings.getScale(sequenceName);  
       double thickness=(double)settings.getSettingAsType(featureName+".thickness", 1.0d);
       Stroke useStroke=(thickness==1)?THIN_LINE:new BasicStroke((float)thickness);
       Color linecolor=settings.getSecondaryColor(featureName);  
       Color fillcolor=settings.getForeGroundColor(featureName);          
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
       int leftOffset=graphicsXoffset;
       int vizStart=settings.getSequenceViewPortStart(sequenceName);
       int vizEnd=settings.getSequenceViewPortEnd(sequenceName);
       boolean inside=(vizStart>=sequenceStart && vizEnd<=sequenceEnd);
       if (orientation==Sequence.REVERSE) {
           if (vizEnd>sequenceEnd && !optimize) leftOffset+=(int)(scale*(vizEnd-sequenceEnd));
       } else {
           if (vizStart<sequenceStart && !optimize) leftOffset+=(int)(scale*(sequenceStart-vizStart));
       }         
       if (vizStart<sequenceStart) vizStart=sequenceStart;
       if (vizEnd>sequenceEnd) vizEnd=sequenceEnd;       

       int aboveHeight=0;
       boolean drawBaseLine=false;
       double range=Math.abs(maxvalue-minvalue);
       if (baselinevalue>minvalue) {         
            double aboveMaximum=Math.abs(maxvalue-baselinevalue);
            aboveHeight=(int)Math.ceil((aboveMaximum/range)*(height-1)); 
            drawBaseLine=true;
       }
       int pixels=(optimize)?width:bases;       

       GeneralPath curve=new GeneralPath();     
       boolean jump=false; // "jumps" are used when the graph line is broken by positions missing valid values
       int edge=1;
       double baselineValueOffset=maxvalue-baselinevalue;
       int baselineY=(int)Math.round((baselineValueOffset/range)*(height))+1; // note that the +2 offset here is different from the corrdinate that the baseline is drawn at  
       //int firstX=(optimize)?(leftOffset-edge):(leftOffset+(int)((0.5-edge)*scale));
       //int firstX=(optimize)?(leftOffset-edge):(leftOffset-edge+(int)(scale));
       int firstX=leftOffset-edge;
       int pixelX=firstX;
       int pixelY=baselineY;      

       boolean firstbase=true;
       int i=0;
       for (i=0-edge;i<pixels+edge;i++) { // the reason for including an edge is to draw additional bases outside of the VP so the curves extending outside of the "edge points" (which could be located at a margin from the edge) are "correct" with respect to the next/previous base                   
              Object value=null;
              if (optimize) { // choose which value to visualize at this pixel from the covered sequence segment (also orientation dependent)
                  int step=i;
                  if (orientation==Sequence.REVERSE) step=(pixels-1)-i;
                  int pos=start+(int)((step/scale)+0.5);
                  int pos2=start+(int)(((step+1)/scale)+0.5)-1;
                  value=getRepresentativeValueInRegion(pos,pos2);
              } else {
                  int pos=(orientation==Sequence.REVERSE)?(start+(pixels-1)-i):(start+i); // relative position within sequence. Use direct orientation. An affine transformation is applied to reverse it if necessary
                  value=sequencedata.getValueAtRelativePosition(pos);
              }
              if (value instanceof Double) { 
                  pixelX=(optimize)?(leftOffset+i):(leftOffset+(int)((i+0.5)*scale)); // the +0.5 puts the value in the center of the pixel for small scales
                  double relativeValueOffset=maxvalue-((Double)value).doubleValue();
                  pixelY=(int)Math.round((relativeValueOffset/range)*(height-1))+1;    
                  if (firstbase) { // use "zero order hold" to draw graph from midpoint of base and to the left edge
                      if (optimize) firstX=pixelX;
                      curve.moveTo(firstX,baselineY);
                      curve.lineTo(firstX,pixelY);                      
                      firstbase=false; 
                  } 
                  if (jump) curve.moveTo(pixelX,pixelY);
                  else curve.lineTo(pixelX,pixelY);                  
                  jump=false;
              } else {
                  if (!firstbase) jump=true;
              } // the graph line will be broken if the value is missing
       }  
       if (!optimize) pixelX=(leftOffset+(int)((i-1)*scale)); // adjust pixelX to right edge of viewport (or just keep current pixelX if optimized mode)
       if (firstbase) curve.moveTo(pixelX,pixelY); // PANIC-HACK! This would only happen if all values are non-Double
       curve.lineTo(pixelX,pixelY); // use "zero order hold" to draw the last value to the right edge of the viewport
       curve.lineTo(pixelX,baselineY); // finish the line at the baseline so the shape is correctly filled
       curve.lineTo(firstX,baselineY); // close the curve by going back to start (at baseline)
       graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
       Stroke defaultStroke=graphics.getStroke();
       AffineTransform saveTransform=graphics.getTransform();
       graphics.setTransform(saveAt); // restores 'original state' (saveAt is set in superclass) 
       graphics.setColor(fillcolor);
       graphics.fill(curve);
       graphics.setStroke(useStroke);
       graphics.setColor(linecolor);
       graphics.draw(curve);
       graphics.setStroke(defaultStroke);
       graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
       graphics.setTransform(saveTransform);
        
       if (drawBaseLine) {     
            graphics.setColor(settings.getBaselineColor(featureName));
            if (optimize) {            
                int first[] = getScreenCoordinateFromGenomic(vizStart);
                int last[]  = getScreenCoordinateFromGenomic(vizEnd);
                int startX  = (first[0]<=last[0])?first[0]:last[0];
                int endX    = (first[0]<=last[0])?last[1]:first[1];
                graphics.drawLine(graphicsXoffset+startX, graphicsYoffset+aboveHeight, graphicsXoffset+endX, graphicsYoffset+aboveHeight);
            } else {       
                graphics.fillRect(graphicsXoffset, graphicsYoffset+aboveHeight, pixels, 1);               
            }
        }
       
    }    
    
    
}