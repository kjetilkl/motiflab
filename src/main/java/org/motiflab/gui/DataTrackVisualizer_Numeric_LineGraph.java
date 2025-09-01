/*
 
 
 */

package org.motiflab.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.Sequence;

/**
 * An implementation of the DataTrackVisualizer for Numeric Datasets that draws the data as a single line graph
 * @author kjetikl
 */
public class DataTrackVisualizer_Numeric_LineGraph extends DataTrackVisualizer_Numeric {
          
    private static final DataTrackVisualizerSetting lineWidthSetting;     
    
    public DataTrackVisualizer_Numeric_LineGraph() {
        super();
    } 
    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);
        if (sequencedata!=null && !(sequencedata instanceof NumericSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}
    }     
    
    @Override
    public String getGraphTypeName() {
        return "Graph (line)";
    }
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.NUMERIC_TRACK_LINEGRAPH_ICON,null);
    }     
    
    @Override
    public boolean isBaseLineRelative() {
        return true;
    }    
    
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {
        // Line graphs are always drawn in 1-to-1 scale so we restore the 'original state' aka "saveAt" which has been set in the superclass by the calling function                      
        AffineTransform saveTransform=graphics.getTransform();
        graphics.setTransform(saveAt); // why do I support non-optimized if I only draw at this scale ?! Because the drawLineGraph() method was copied directly from the old single DTV for numeric tracks
        Color foreground=settings.getForeGroundColor(featureName);              
        drawLineGraph(graphics, start, height, width, bases, graphicsXoffset, graphicsYoffset, orientation, foreground, optimize);             
        graphics.setTransform(saveTransform);
    } 

       
    private void drawLineGraph(Graphics2D graphics, int start, int height, int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, Color linecolor, boolean optimize) {
       double scale=settings.getScale(sequenceName);  
       double thickness=(double)settings.getSettingAsType(featureName+".lineWidth", 1.0d);
       Stroke useStroke=new BasicStroke((float)thickness);
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

       if (drawBaseLine) { // note that the baseline here is drawn behind the graph itself and not on top
           graphics.setColor(settings.getBaselineColor(featureName)); 
           if (optimize) {          
                int first[] = getScreenCoordinateFromGenomic(vizStart);
                int last[]  = getScreenCoordinateFromGenomic(vizEnd);
                int startX  = (first[0]<=last[0])?first[0]:last[0];
                int endX    = (first[0]<=last[0])?last[1]:first[1];             
                graphics.drawLine(leftOffset+startX, graphicsYoffset+aboveHeight, leftOffset+endX, graphicsYoffset+aboveHeight);                                          
           } else {
                 // graphics.drawLine(leftOffset, graphicsYoffset+aboveHeight, leftOffset+(int)(pixels*scale), graphicsYoffset+aboveHeight);   
                 graphics.fillRect(leftOffset, graphicsYoffset+aboveHeight, (int)(pixels*scale), 1);                    
           }  
       }
       GeneralPath curve=new GeneralPath();     
       boolean jump=true;
       int edge=1;
       for (int i=0-edge;i<pixels+edge;i++) { 
              Object value=null;
              if (optimize) {
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
                  int pixelX=(optimize)?(leftOffset+i):(leftOffset+(int)((i+0.5)*scale)); // the +0.5 puts the value in the center of the pixel for small scales
                  double relativeValueOffset=maxvalue-((Double)value).doubleValue();
                  int pixelY=(int)Math.round((relativeValueOffset/range)*(height-1))+1;                   
                  if (jump) curve.moveTo(pixelX,pixelY);
                  else curve.lineTo(pixelX,pixelY);                  
                  jump=false;
              } else jump=true;
       }      
       graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
       graphics.setColor(linecolor);
       Stroke defaultStroke=graphics.getStroke();
       graphics.setStroke(useStroke);
       graphics.draw(curve);
       graphics.setStroke(defaultStroke);
       graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

 
    @Override
    public ArrayList<DataTrackVisualizerSetting> getGraphTypeSettings() {
        ArrayList<DataTrackVisualizerSetting> graphsettings = super.getGraphTypeSettings();
        if (graphsettings==null) graphsettings=new ArrayList<>();            
        addGraphTypeSetting(graphsettings, lineWidthSetting, false);              
        return graphsettings;
    }
    
    static {
        // Since I only have one setting it might as well be MAJOR, but change it to MINOR if you add more
        lineWidthSetting=new DataTrackVisualizerSetting("Line Width", "lineWidth", DataTrackVisualizerSetting.MAJOR_SETTING, DataTrackVisualizerSetting.ALL, 0);
        lineWidthSetting.addOption("Thin",1.0); // horizontal fill is the default style according to the legacy setting, so I place that as the first option
        lineWidthSetting.addOption("Medium",1.50);         
        lineWidthSetting.addOption("Thick",2.0);    
        lineWidthSetting.addOption("Very Thick",2.5);                 
    }     
    
}