/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Sequence;


/**
 * This is the default DataTrackVisualizer used to render DNA Sequence Dataset tracks
 * It draws each base as a colored box and with a letter on top (if the scale permits)
 * @author kjetikl
 */
public class DataTrackVisualizer_Sequence_DNA extends DataTrackVisualizer_Sequence {
    
    private Rectangle rectangle = new Rectangle(0, 0, 0, 0);
    

    /** Constructs a new DefaultDataTrackVisualizer */
    public DataTrackVisualizer_Sequence_DNA() {
        super();
    }

    /** An alternative constructor factory method */
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
       super.initialize(sequenceName, featureName, settings);
       if (sequencedata!=null && !(sequencedata instanceof DNASequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}      
    } 
    
    @Override
    public String getGraphTypeName() {
        return "DNA";
    }    
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.SEQUENCE_ICON_BASES,null);
    } 
    
/**
 * This function renders the actual datatrack. 
 * 
 * @param graphics An instance of the graphics object where the datatrack should be rendered
 * @param start The relative start of viewPort within the sequence. The region of the sequence that should be rendered is by decided by "start" and "width"
 * @param height The height to be used for visualization of the datatrack
 * @param width The total width of the track (in pixels) 
 * @param bases The number of bases to be rendered
 * @param graphicsXoffset The x-coordinate within the graphics object where rendering should placed
 * @param graphicsYoffset The y-coordinate within the graphics object where rendering should placed
 * @param orientation The strand and orientation that should be used when rendering (DIRECT=1, REVERSE=-1).
 */
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {
        Paint colorA=settings.getBaseColor('A');
        Paint colorC=settings.getBaseColor('C');
        Paint colorG=settings.getBaseColor('G');
        Paint colorT=settings.getBaseColor('T');
        Paint colorX=settings.getBaseColor('X');
        
        if (settings.useGradientFill(featureName)>0) { // using only vertical gradients here!
           colorA=getGradientPaintForColor((Color)colorA, graphicsYoffset, height);
           colorC=getGradientPaintForColor((Color)colorC, graphicsYoffset, height);
           colorG=getGradientPaintForColor((Color)colorG, graphicsYoffset, height);
           colorT=getGradientPaintForColor((Color)colorT, graphicsYoffset, height);
           colorX=getGradientPaintForColor((Color)colorX, graphicsYoffset, height);
        }

        double scale=settings.getScale(sequenceName);
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
              }  
    } // end of drawVisibleSegment

    private java.awt.GradientPaint getGradientPaintForColor(Color color, int y, int height) {
       int red=color.getRed();
       int blue=color.getBlue();
       int green=color.getGreen();
       Color lightercolor=new Color((int)((255-red)*0.65)+red,(int)((255-green)*0.65)+green,(int)((255-blue)*0.5)+blue);
       java.awt.GradientPaint gradient = new java.awt.GradientPaint(0, y, color, 0, (y+height)/2f, lightercolor, true);
       return gradient;
    }

   @Override
   public void drawVisibleSegmentOverlay(Graphics2D g, int start, int height, int width, int bases, int xoffset, int yoffset, int orientation) {
        Font baseFont=settings.getDNAFont();     
       
       // draw base letters if there is room
       int viewPortStart=settings.getSequenceViewPortStart(sequenceName);
       int viewPortEnd=settings.getSequenceViewPortEnd(sequenceName);
       double pixelsprbase=(double)settings.getScale(sequenceName);
       double letterwidth=g.getFontMetrics(baseFont).charWidth('G');
       double letterheight=g.getFontMetrics(baseFont).getAscent();
       if (height>=letterheight+2 && pixelsprbase>=letterwidth+2) {
          g.setFont(baseFont);
          g.setColor(java.awt.Color.BLACK);
          int pos=0;
          int vpwidth=viewPortEnd-viewPortStart+1;
          Character[] characterbuffer=new Character[vpwidth];
          for (int i=0;i<vpwidth;i++) {
              pos=viewPortStart+i;
              Object value=sequencedata.getValueAtGenomicPosition(pos);
              if (!(value instanceof Character)) continue;
              char base=((Character)value).charValue();
              if (orientation==FeatureSequenceData.REVERSE) characterbuffer[vpwidth-(i+1)]=reverseBase(base);
              else characterbuffer[i]=base;
          }
          double letteroffset=(pixelsprbase-letterwidth)/2;
          double letteroffsetY=letterheight;
          if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          for (int i=0;i<vpwidth;i++) {
             if (characterbuffer[i]!=null) {
                 if (settings.isBaseColorVeryDark(characterbuffer[i])) g.setColor(Color.WHITE); 
                 else g.setColor(Color.BLACK);
                 int correction=(Character.isLowerCase(characterbuffer[i]))?1:0;
                 if (characterbuffer[i]=='T') correction+=1;
                 else if (characterbuffer[i]=='t') correction+=2;
                 g.drawString(""+characterbuffer[i], (int)(xoffset+pixelsprbase*i+letteroffset+0.5+correction), (int)(yoffset+(height+letteroffsetY)/2)-1);
             }
          }
          if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
       }
    } // end paintComponent


    @Override
    public boolean optimizeForSpeed(double scale) {
        return scale<optimizationThreshold;
    }


}
