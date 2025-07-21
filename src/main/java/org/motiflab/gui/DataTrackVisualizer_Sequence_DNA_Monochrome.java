/*
 
 
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.FeatureSequenceData;


/**
 * This is a DataTrackVisualizer used to render DNA Sequence Dataset tracks.
 * It first draws the background in the selected background colors, and then,
 * if the scale permits, the base letters are drawn on top in the selected foreground color
 * and the bases are separated by lines drawn in the secondary color
 * @author kjetikl
 */
public class DataTrackVisualizer_Sequence_DNA_Monochrome extends DataTrackVisualizer_Sequence {
 
    /** Constructs a new DefaultDataTrackVisualizer */
    public DataTrackVisualizer_Sequence_DNA_Monochrome() {
        super();
    }

    /** An alternative constructor factory method */
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
       super.initialize(sequenceName, featureName, settings);
       if (sequencedata!=null && !(sequencedata instanceof DNASequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}      
    } 
    
    @Override
    public String getGraphTypeName() {
        return "DNA Monochrome";
    }    
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new SimpleDataPanelIcon(20,20,SimpleDataPanelIcon.SEQUENCE_ICON_BASES,null);
    } 
    
    
    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {

    } // end of drawVisibleSegment


   @Override
   public void drawVisibleSegmentOverlay(Graphics2D g, int start, int height, int width, int bases, int xoffset, int yoffset, int orientation) {
        Paint foreground=settings.getForeGroundColor(featureName);
        Paint secondary=settings.getSecondaryColor(featureName);
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
                 g.setPaint(foreground);                   
                 int correction=(Character.isLowerCase(characterbuffer[i]))?2:1;
                 if (characterbuffer[i]=='T') correction+=1;
                 else if (characterbuffer[i]=='t') correction+=2;
                 else if (characterbuffer[i]=='G') correction-=1;
                 g.drawString(""+characterbuffer[i], (int)(xoffset+pixelsprbase*i+letteroffset+0.5+correction), (int)(yoffset+(height+letteroffsetY)/2)-1);
                 if (i>0) {
                     g.setPaint(secondary);
                     g.drawLine((int)(xoffset+pixelsprbase*i), (int)(yoffset), (int)(xoffset+pixelsprbase*i), yoffset+height);
                 }
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
