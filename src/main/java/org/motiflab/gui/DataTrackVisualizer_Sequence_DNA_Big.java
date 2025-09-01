/*
 
 
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.Sequence;


/**
 * This is a DataTrackVisualizer used to render DNA Sequence Dataset tracks.
 * It first draws the background in the selected background colors, and then,
 * if the scale permits, the base letters are drawn on top in their respective colors
 * using a large font (the same font used for the "DNA Graph" type for numeric tracks, but the letters are not scaled)
 * For smaller scales, the bases are just drawn as boxes in the respective colors (same as the default renderer).
 * @author kjetikl
 */
public class DataTrackVisualizer_Sequence_DNA_Big extends DataTrackVisualizer_Sequence {
    
    private static double dnaCutoff=5.0; // NUMERIC_DNA_GRAPH will only draw base letters above 5.0 scale (500%)
    private static Font logoFont=null;
    private Rectangle rectangle = new Rectangle(0, 0, 0, 0);    

    /** Constructs a new DefaultDataTrackVisualizer */
    public DataTrackVisualizer_Sequence_DNA_Big() {
        super();
    }

    /** An alternative constructor factory method */
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
       super.initialize(sequenceName, featureName, settings);
       if (sequencedata!=null && !(sequencedata instanceof DNASequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}      
       if (logoFont==null) logoFont=MotifLogo.getLogoFont(140); // this size is chosen to correspond with the baseImage size (though not exactly the same value)     
    } 
    
    @Override
    public String getGraphTypeName() {
        return "DNA Big";
    }    
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.SEQUENCE_ICON_BASES,null);
    } 
    
    
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {
       double scale=settings.getScale(sequenceName);
       if (!(bases<=width && scale>=dnaCutoff)) { // draw "regular DNA track" if scale is to large
            Paint colorA=settings.getBaseColor('A');
            Paint colorC=settings.getBaseColor('C');
            Paint colorG=settings.getBaseColor('G');
            Paint colorT=settings.getBaseColor('T');
            Paint colorX=settings.getBaseColor('X');     

            double basesprpixel=1.0/scale;
            int pixels=(optimize)?width:bases; // the number of pixels to draw in the graphics object (before affine transform)
            for (int i=0;i<pixels;i++) { // draw visible region
                  Object value=null;
                  int pos=start+i;
                  if (optimize) {
                      int step=(orientation==Sequence.DIRECT)?i:(pixels-1)-i; // step is "segment index"
                      //pos=start+(int)((step/scale)+0.5); // sample equidistant pixels
                      double exact=(start+step/scale);
                      pos=(int)(exact-exact%basesprpixel); // round to nearest 'round' number depending on scale
                  }
                  char base;
                  value=sequencedata.getValueAtRelativePosition(pos);

                  if (value instanceof Character) { // DNA track
                      base=((Character)value).charValue();
                      if (orientation==FeatureSequenceData.REVERSE) { base=reverseBase(base); }
                      switch(base) {
                          case 'A': case 'a': graphics.setPaint(colorA);break;
                          case 'C': case 'c': graphics.setPaint(colorC);break;
                          case 'G': case 'g': graphics.setPaint(colorG);break;
                          case 'T': case 't': graphics.setPaint(colorT);break;
                          default : graphics.setPaint(colorX);break;
                      }                     
                      rectangle.x=graphicsXoffset+i;
                      rectangle.y=graphicsYoffset;
                      rectangle.width=1;
                      rectangle.height=height+1;
                      graphics.fill(rectangle);                    
                  }
            } // end for each pixel             
       } // end draw "regular" track
    } // end of drawVisibleSegment


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
          if (!(valueObject instanceof Character)) continue;
          char base=(Character)valueObject;//(orientation==Sequence.REVERSE)?(dnasequence[dnasequence.length-(i+1)]):dnasequence[i];     
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
         drawBaseImage(g, x, yoffset, baseWidth, height, base, useColor);
                                  
      } // end for each base
    } // end drawDNAGraphOverlay    
    
   
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


    @Override
    public boolean optimizeForSpeed(double scale) {
        return scale<optimizationThreshold;
    }


}
