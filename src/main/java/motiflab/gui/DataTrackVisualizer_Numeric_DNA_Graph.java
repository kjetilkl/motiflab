/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Sequence;

/**
 * An implementation of the DataTrackVisualizer for Numeric Datasets that draws the data using
 * colored DNA base letters scaled to the height of the value at each position.
 * The DNA sequence is based on the first DNA Sequence Dataset found in the Features Panel.
 * If the scale is such that the letters would be to narrow to see (less than 5px per position), the track is drawn similar to the bar graph type,
 * but the bar at each position will be drawn in the color of the base. 
 * If no DNA track is found, or the scale is less than 100%, a regular bar graph is drawn in the track's foreground color.
 * @author kjetikl
 */
public class DataTrackVisualizer_Numeric_DNA_Graph extends DataTrackVisualizer_Numeric {
       
    private static double dnaCutoff=5.0; // NUMERIC_DNA_GRAPH will only draw base letters above 5.0 scale (500%)
    private static Font logoFont=null;
    private char[] dnasequence=null; // used for rendering tracks as DNA height graphs
    
   
    public DataTrackVisualizer_Numeric_DNA_Graph() {
        super();
    } 
    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);
        if (sequencedata!=null && !(sequencedata instanceof NumericSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}
        if (logoFont==null) logoFont=MotifLogo.getLogoFont(140); // this size is chosen to correspond with the baseImage size (though not exactly the same value)     
    }     
    
    @Override
    public String getGraphTypeName() {
        return "DNA Graph";
    }

    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.NUMERIC_TRACK_DNA_GRAPH_ICON,null);
    }     
    
    @Override
    public boolean isBaseLineRelative() {
        return true;
    }
    
    
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {       
        double scale=settings.getScale(sequenceName); 
        boolean drawBarGraphInstead=(scale<1.0);  // Do not use NUMERIC_DNA_GRAPH if the scale < 100% since the base colors can not be discerned (default to BAR_GRAPH instead)
        Color foreground=settings.getForeGroundColor(featureName); 
        ColorGradient gradient=settings.getColorGradient(featureName);  
        double maxvalue=1;
        double minvalue=0;
        double baselinevalue=0;
        int aboveHeight=0; // number of pixels for displaying values above baseline
        int belowHeight=0; // number of pixels for displaying values below baseline
        boolean drawBaseLine=false;
        double directionalMaximum=0; // 
        double aboveMaximum=0; // 
        double belowMaximum=0; //             
        
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
        
        // get the sequence
        // DNASequenceDataset dnadataset=(DNASequenceDataset)gui.getFeaturesPanel().getFirstItemOfType(DNASequenceDataset.class); // use first DNA track is FeaturesPanel as reference
        DNASequenceDataset dnadataset=gui.getFeaturesPanel().getReferenceDNAtrack(featureName); // use first DNA track above current track as reference
        if (dnadataset!=null) {
            DNASequenceData dnaseq=(DNASequenceData)dnadataset.getSequenceByName(sequenceName);
            dnasequence=(char[])dnaseq.getValueInInterval(start, start+bases-1);
        } else {
            dnasequence=null;
        } // No applicable DNA sequence found. Use regular bar graph instead
        if (dnasequence==null) drawBarGraphInstead=true; // drawBarGraphInstead is already set to TRUE if scale < 100%
                      
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
                 if (drawBarGraphInstead) { // this draws a completely "normal" bar graph in the foreground color.
                    if (drawBaseLine) { // track has both positive and negative values. Positive values must be drawn above the baseline and negative values below.
                       double val=((Double)value).doubleValue();
                       if (val==baselinevalue) continue; // this will be hidden behind the baseline anyway
                       else if (val>baselinevalue) { // value above baseline
                           double relativeValue=Math.abs((val-baselinevalue)/aboveMaximum);
                           int solidbarheight=(int)Math.floor(aboveHeight*relativeValue); // round down to nearest pixel
                           double overshoot=(aboveHeight*relativeValue)-solidbarheight; // amount overshooting the nearest pixel. Use this value to get a gradient color and place one pixel with this color on top of the bar (simulates higher precision)
                           if (solidbarheight<aboveHeight) { // draw gray pixel on top of bar to represent "fractional pixel"
                               graphics.setColor(gradient.getColor(overshoot));
                               drawBase(graphics,graphicsXoffset+i, graphicsYoffset+aboveHeight-(solidbarheight+1), 1);
                           }
                           if (solidbarheight>0) {
                               graphics.setColor(foreground);
                               drawBase(graphics,graphicsXoffset+i, graphicsYoffset+aboveHeight-(solidbarheight),  solidbarheight-1);
                           }
                      } else { // value below baseline
                           double relativeValue=Math.abs((baselinevalue-val)/belowMaximum);
                           int solidbarheight=(int)Math.floor(belowHeight*relativeValue); // round down to nearest pixel
                           double overshoot=(belowHeight*relativeValue)-solidbarheight; // amount overshooting the nearest pixel. Use this value to get a gradient color and place one pixel with this color on top of the bar (simulates higher precision)
                           if (solidbarheight<belowHeight && overshoot>0) { // draw overshoot
                               graphics.setColor(gradient.getColor(overshoot));
                               drawBase(graphics,graphicsXoffset+i, (graphicsYoffset+aboveHeight)+solidbarheight, 1);
                           }
                           if (solidbarheight>0) { // draw the solid bar itself
                               graphics.setColor(foreground);
                               drawBase(graphics,graphicsXoffset+i, (graphicsYoffset+aboveHeight), solidbarheight);
                           }
                      }
                    } else { // Bar graph without baseline. Only need to draw positive values.
                       double relativeValue=((Double)value).doubleValue()/maxvalue;
                       int solidbarheight=(int)Math.floor(height*relativeValue); // round down to nearest pixel
                       double overshoot=(height*relativeValue)-solidbarheight; // amount overshooting the nearest pixel. Use this value to get a gradient color and place one pixel with this color on top of the bar (simulates higher precision)
                       if (solidbarheight<height) { // draw 'gray' pixel on top of bar to represent "fractional pixel"
                           graphics.setColor(gradient.getColor(overshoot));
                           drawBase(graphics,graphicsXoffset+i, graphicsYoffset+height-(solidbarheight+1), 0);
                       }
                       if (solidbarheight>0) {
                           graphics.setColor(foreground);
                           drawBase(graphics,graphicsXoffset+i, graphicsYoffset+height-(solidbarheight),  solidbarheight-1);
                       }
                    }
                 } else  { // Draw DNA GRAPH. This graph type will draw scaled DNA base letters on scales >=500% (using overlay). On scales 100%-500% it will draw a bar graph with bars in base colors
                    char base=(i>=0 && i<dnasequence.length)?dnasequence[i]:'X';
                    if (orientation==Sequence.REVERSE) base=MotifLabEngine.reverseBase(base);
                    if (drawBaseLine) { // track has both positive and negative values. Positive values must be drawn above the baseline and negative values below.
                       double val=((Double)value).doubleValue();
                       if (val==baselinevalue) continue; // this will be hidden behind the baseline anyway
                       else if (scale>=dnaCutoff) continue; // if scale is higher than cutoff we will in overlay instead of here 
                       else if (val>baselinevalue) { // value above baseline
                           double relativeValue=Math.abs((val-baselinevalue)/aboveMaximum);
                           int solidbarheight=(int)Math.floor(aboveHeight*relativeValue); // round down to nearest pixel
                           if (solidbarheight>0) {
                               graphics.setColor(settings.getBaseColor(base));                           
                               drawBase(graphics,graphicsXoffset+i, graphicsYoffset+aboveHeight-(solidbarheight),  solidbarheight-1);                              
                           }
                      } else { // value below baseline
                           double relativeValue=Math.abs((baselinevalue-val)/belowMaximum);
                           int solidbarheight=(int)Math.floor(belowHeight*relativeValue); // round down to nearest pixel
                           if (solidbarheight>0) { // draw the solid bar itself
                              graphics.setColor(settings.getBaseColor(base));
                              drawBase(graphics,graphicsXoffset+i, (graphicsYoffset+aboveHeight), solidbarheight);                              
                           }
                      }
                    } else { // Bar graph without baseline. Only need to draw positive values.
                       double relativeValue=((Double)value).doubleValue()/maxvalue;
                       int solidbarheight=(int)Math.floor(height*relativeValue); // round down to nearest pixel
                       if (solidbarheight>0 && scale<dnaCutoff) {
                           graphics.setColor(settings.getBaseColor(base));
                           drawBase(graphics,graphicsXoffset+i, graphicsYoffset+height-(solidbarheight),  solidbarheight-1);                          
                       }
                    }
                 } // end of graph types if-else
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
                int first[]=getScreenCoordinateFromGenomic(vizStart);
                int last[]=getScreenCoordinateFromGenomic(vizEnd);
                int startX=(first[0]<last[0])?first[0]:last[1];
                int endX=(first[0]<last[0])?last[1]:first[0];
                graphics.drawLine(graphicsXoffset+startX, graphicsYoffset+aboveHeight, graphicsXoffset+endX, graphicsYoffset+aboveHeight);
            } else {       
                graphics.fillRect(graphicsXoffset, graphicsYoffset+aboveHeight, pixels, 1);               
            }
        }
    } // end of drawVisibleSegment
    
   
    
    private void drawBase(Graphics2D graphics,int x, int y, int height) {
        if (height>=0) graphics.fillRect(x, y, 1, height+1); 
    }
       
    
   @Override
   public void drawVisibleSegmentOverlay(Graphics2D g, int start, int height, int width, int bases, int xoffset, int yoffset, int orientation) {
       double scale=settings.getScale(sequenceName);
       if (bases<=width && scale>=dnaCutoff) drawDNAGraphOverlay(g, start, height, width, bases, xoffset, yoffset, orientation, scale);
   }
    
   private void drawDNAGraphOverlay(Graphics2D g, int start, int height, int width, int bases, int xoffset, int yoffset, int orientation, double scale) {
      Paint colorA=settings.getBaseColor('A');
      Paint colorC=settings.getBaseColor('C');
      Paint colorG=settings.getBaseColor('G');
      Paint colorT=settings.getBaseColor('T');
      Paint colorX=settings.getBaseColor('X');
      if (dnasequence==null) return;
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

       int aboveHeight=0; // number of pixels for displaying values above baseline
       int belowHeight=0; // number of pixels for displaying values below baseline
       boolean drawBaseLine=false; 
       double aboveMaximum=0; // 
       double belowMaximum=0; //        

      if (baselinevalue>minvalue) {
         double range=Math.abs(maxvalue-minvalue);
         aboveMaximum=Math.abs(maxvalue-baselinevalue);
         belowMaximum=Math.abs(baselinevalue-minvalue);
         aboveHeight=(int)Math.ceil((aboveMaximum/range)*(height-1)); // baseline is placed relative to range
         belowHeight=height-(aboveHeight+1); 
         drawBaseLine=true;
      }
      int viewPortStart=settings.getSequenceViewPortStart(sequenceName);
      int viewPortEnd=settings.getSequenceViewPortEnd(sequenceName); 
      int viewPortSize=viewPortEnd-viewPortStart+1;
      int offset=viewPortSize-bases;  // "offset" is the number of "blank" flanking bases that must be skipped on the left side before starting to draw the sequence. Here it is calculated by just subtracting the number of visible bases from the number of bases that can fit in the window (=size of VP). This is only correct if the sequence has blank flanking regions on at most one side, however.
      if (offset>=0) { // check if this is the correct direction          
          if ((orientation==Sequence.DIRECT && start>0) || (orientation==Sequence.REVERSE && start==0)) offset=0;
      }
      if (bases==sequencedata.getSize()) { // whole sequence fits within VP. There could possibly be blank flanking segments on both sides, so the offset must be set to only left flank
           if (orientation==Sequence.DIRECT) offset=sequencedata.getRegionStart()-viewPortStart;
           else offset=viewPortEnd-sequencedata.getRegionEnd();                                   
      }
      g.setFont(logoFont);        
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);     
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);   // VALUE_INTERPOLATION_BILINEAR
      
      for (int i=0;i<bases;i++) {
          int position=start+i;
          if (orientation==Sequence.REVERSE) position=(start+bases-1)-i;
          Object valueObject=sequencedata.getValueAtRelativePosition(position);
          if (!(valueObject instanceof Double)) continue;
          double value=(Double)valueObject;
          char base=(orientation==Sequence.REVERSE)?(dnasequence[dnasequence.length-(i+1)]):dnasequence[i];     
          if (orientation==Sequence.REVERSE) base=reverseBase(base);
          Paint useColor=colorX;
          switch(base) {
              case 'A': case 'a': useColor=colorA;break;
              case 'C': case 'c': useColor=colorC;break;
              case 'G': case 'g': useColor=colorG;break;
              case 'T': case 't': useColor=colorT;break;
              default : useColor=colorX;base='X';break;
          }      
         int x=(int)(xoffset+(i+offset)*scale);    
         double baseWidth=scale;
         // -------------
         if (drawBaseLine) { // track has both positive and negative values. Positive values must be drawn above the baseline and negative values below.
           if (value==baselinevalue) continue; // this will be hidden behind the baseline anyway
           else if (value>baselinevalue) { // value above baseline
               double relativeValue=Math.abs((value-baselinevalue)/aboveMaximum);
               int solidbarheight=(int)Math.floor(aboveHeight*relativeValue); // round down to nearest pixel
               if (solidbarheight>0) {
                   drawBaseImage(g, x ,yoffset+aboveHeight-(solidbarheight), baseWidth, solidbarheight, base, useColor);
               }
          } else { // value below baseline
               double relativeValue=Math.abs((baselinevalue-value)/belowMaximum);
               int solidbarheight=(int)Math.floor(belowHeight*relativeValue); // round down to nearest pixel
               if (solidbarheight>0) { // draw the solid bar itself                
                   drawBaseImage(g, x, yoffset+aboveHeight+1, baseWidth, solidbarheight, base, useColor); // I have added a +1 to the Y-coordinate here because it seems to work better with the font (not drawing over the baseline)
               }
          }
        } else { // Bar graph without baseline. Only need to draw positive values.
           double relativeValue=value/maxvalue;
           int solidbarheight=(int)Math.floor(height*relativeValue); // round down to nearest pixel
           if (solidbarheight>0) {            
               drawBaseImage(g, x, yoffset+height-solidbarheight, baseWidth, solidbarheight, base, useColor);
           }
        }                  
      }
    } // end paintComponent    
    
   
    private void drawBaseImage(Graphics2D g, int x, int y, double width, int height, char base, Paint useColor) {
        double baseSize=100.0; // NB: the base character is drawn at 100x100 pixels, corresponding to a font size = 140
        double ratioW=(width/baseSize); 
        double ratioH=(height/baseSize);
        AffineTransform originalTransform=g.getTransform(); 
        AffineTransform newTransform=new AffineTransform();
        x+=originalTransform.getTranslateX();
        y+=originalTransform.getTranslateY();   
        newTransform.concatenate(AffineTransform.getTranslateInstance(x,y));
        newTransform.concatenate(AffineTransform.getScaleInstance(ratioW, ratioH));             
        g.setTransform(newTransform);
        g.setPaint(useColor);     
        if (base=='X') g.fillRect(0, 0, (int)baseSize, (int)baseSize); // just draw a box for non-DNA letters
        else g.drawString(""+base, 0, (int)baseSize);
        g.setTransform(originalTransform); // restore         
    }
    
}