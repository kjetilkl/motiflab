/*
 
 
 */

package motiflab.gui;

import motiflab.engine.data.FeatureSequenceData;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.border.Border;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import javax.swing.event.MouseInputListener;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JDialog;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.MotifLabResource;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.Sequence;

/**
 * This is the superclass for all datatrack visualizers. 
 * A DataTrackVisualizer is responsible for visualizing one type of data (for one sequence)
 * @author kjetikl
 */
public abstract class DataTrackVisualizer extends JComponent implements MouseInputListener, MotifLabResource {
    
    protected FeatureSequenceData sequencedata; // this is the single sequence from which this datatrack stems
    protected VisualizationSettings settings;
    protected String sequenceName;
    protected String featureName;
    protected MotifLabGUI gui;
    protected int bordersize=1; // size of black border surrounding track;
    protected int geneOrientation;
    protected int sequenceStart;
    protected int sequenceEnd;
    protected int TSS=-1;
    protected int TES=-1;
    protected int currentmouseposition=-1; // stores the genomic coordinate corresponding to the current mouse position
    protected AffineTransform saveAt; // this is the "original" transform of the graphics object, where one pixel in the drawing canvas corresponds to one pixel on screen
    protected double optimizationThreshold=0.4f; // use optimization below this scale
    protected static java.awt.Font trackNameFont=new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 10); 
    private boolean initialized=false;
     
    protected boolean isVisualizingDirectStrand=true; // this is set every time the DTV repaints. It is TRUE if the genomic DIRECT strand is being visualized and FALSE if the genomic REVERSE strand is visualized
    protected boolean isVisualizingReverseStrand=false; // this is set every time the DTV repaints. It is TRUE if the genomic REVERSE strand is being visualized and FALSE if the genomic DIRECT strand is visualized
    protected boolean isVisualizingRelativeStrand=true; // this is set every time the DTV repaints. It is TRUE if we are currently visualizing the relative strand (i.e. the same strand that the sequence is oriented in)
     
     
  public DataTrackVisualizer() {}; // null constructor     
     
    
  /** This should be called after an object has been created to setup of everything. */
  public void initialize(String sequenceName, String featureName, VisualizationSettings settings) {
      this.sequenceName=sequenceName;
      this.featureName=featureName;
      this.settings=settings;
      this.gui=settings.getGUI();     
      if (featureName!=null && sequenceName!=null) {
         FeatureDataset dataset=(FeatureDataset)settings.getEngine().getDataItem(featureName);          
         sequencedata=dataset.getSequenceByName(sequenceName);   
      }
      if (sequencedata!=null) {
         geneOrientation=sequencedata.getStrandOrientation();
         sequenceStart=sequencedata.getRegionStart();
         sequenceEnd=sequencedata.getRegionEnd();  
         Integer TSSint=sequencedata.getTSS();
         if (TSSint!=null) TSS=TSSint.intValue();
         Integer TESint=sequencedata.getTES();
         if (TESint!=null) TES=TESint.intValue();
      }
      setBackground(Color.GRAY);
      setOpaque(true);
      addMouseListener(this);
      addMouseMotionListener(this);
      setFocusable(false);
      settings.setVariableTrackHeight(featureName, false); // default. just to override if the featureName has been used for something else before
      setInitialized();     
  }
  
  public boolean isInitialized() {
      return initialized;
  }
  public void setInitialized() {
      initialized=true;
  }
  
  public String getFeatureName() {
      return featureName;
  }  
  
  /** Returns the name of the "graph type" that this visualizer renders, e.g. "Graph (filled)", "Graph (line)", "DNA" */
  public abstract String getGraphTypeName();
  
  /** Returns an icon to use in the Features Panel for this graph type */  
  public abstract SimpleDataPanelIcon getGraphTypeIcon();
  

  
 @Override
   public Dimension getPreferredSize() {
     int height=getHeight(); // settings.getDataTrackHeight(featureName);
     int width=settings.getSequenceWindowSize();
     Border border=getBorder();
      if (border==null) {return new Dimension(width,height);}
      else {
          Insets insets=getBorder().getBorderInsets(this);
          return new Dimension(width+insets.left+insets.right,height+insets.top+insets.bottom);
      }
   }

 /**
  * This method is used to determine whether this track has a static user-defined height,
  * or whether the height can vary depending on the contents of the track (for instance if
  * overlapping regions are drawn beneath each other on separate lines rather than on top of each other).
  * The default return value is FALSE (meaning the height is static), but this can be overridden by subclasses. 
  * Subclasses that return TRUE for this method should also override getTrackHeight() to return the actual height. 
  * @return
  */
  public boolean hasVariableHeight() {
      return false;
  }

  @Override
   public int getHeight() { // this returns the track height + size of borders
      int height=getTrackHeight();
      Border border=getBorder();    
      if (border==null) {return height;}
      else {
        Insets insets=getBorder().getBorderInsets(this);
        return height+insets.top+insets.bottom;
      }
   }
  
  /**
   * Returns the height of this track (similar to getHeight() but without border (if present))
   * Subclasses that implement variable height visualization should override this method and return
   * the dynamically calculated height instead of the static value used by default.
   * @return
   */
  public int getTrackHeight() {    
      return settings.getDataTrackHeight(featureName);
  }

  @Override
   public int getWidth() {
      int width=settings.getSequenceWindowSize();
      Border border=getBorder();
      if (border==null) {return width;}
      else {
        Insets insets=getBorder().getBorderInsets(this);
        return width+insets.left+insets.right;
      }
   }

  /**
   * This method is called by the enclosing SequenceVisualizer when the FeatureDataset has been updated
   * in order to notify this DataTrackVisualizer. The default implementation is empty but it can be 
   * overridden by specific subclasses if they have to perform some administrative steps 
   */
  public void dataUpdated() {}
  
  /** This method is called by the enclosing SequenceVisualizer when it rearranges the tracks (not necessarily because of data update)
   *  It can be used to update settings, if necessary, to make sure they are up to date when a major layout event happens that triggers a redraw. 
   *  The default implementation is empty but it can be overridden in subclasses. Implementing subclasses should call "super.refresh()" just in case.
   */
  public void refreshSettings() { 
      //
  }  

  /** Tries to locate the sequence data if it could not be found at construction time */
  protected void installSequenceData() {
      FeatureDataset dataset=(FeatureDataset)settings.getEngine().getDataItem(featureName);          
      sequencedata=dataset.getSequenceByName(sequenceName);     
      if (sequencedata==null) return;
      geneOrientation=sequencedata.getStrandOrientation();
      sequenceStart=sequencedata.getRegionStart();
      sequenceEnd=sequencedata.getRegionEnd();      
      Integer TSSint=sequencedata.getTSS();
      if (TSSint!=null) TSS=TSSint.intValue();
      Integer TESint=sequencedata.getTES();
      if (TESint!=null) TES=TESint.intValue();
  }

  /** renames the feature dataset for this DataTrackVisualizer */
  public void rename(String newname) {
      featureName=newname;
  }
    
  /** 
   * If this track is a "responsible parent" of a grouped track, this method will return DTVs for the children that should be included (excluding tracks that are hidden)
   */
  private DataTrackVisualizer[] getGroupChildred() {      
      if (!settings.isGroupTrackParent(featureName)) return null;
      ArrayList<String> childTracks=settings.getTrackGroup(featureName);
      if (childTracks==null) return null;    
      DataTrackVisualizer[] childTrackVisualizers=new DataTrackVisualizer[childTracks.size()];
      SequenceVisualizer sv=gui.getVisualizationPanel().getSequenceVisualizer(sequenceName);
      for (int i=0;i<childTracks.size();i++) {
          DataTrackVisualizer dtv=sv.getTrackVisualizer(childTracks.get(i)); // 
          childTrackVisualizers[i]=dtv;
      }
      return childTrackVisualizers;
  }
  
  
   /*
   * This is the method that actually draws the track.
   * It first determines the ViewPort region, which is the portion of the sequence that will be visible in the sequence window (and hence should be drawn).
   * It then prepares 2 AffineTransforms:
   *  - One is called "saveAt" which is the normal default transform from the graphics object (almost?) with scale=1.0 (i.e. one pixel in the drawing canvas corresponds to one pixel on the screen)
   *  - The other is called "scaleTransform" which is scaled based on the current zoom level for this sequence (and it could also be translated). Here, one pixel in the drawing canvas corresponds to one DNA base (which may be wider or narrower than a screen pixel)
   * The "scaleTransform" is the one normally used because it makes the actual drawing easier for the subclasses.
   * With this transform the DTV is told what segment of the sequence to draw, e.g. bases 232-489 in the sequence,
   * and the DTV should then draw these bases with one base per coordinate in the graphics object starting at first coordinate X=0 and increasing.
   * The scaleTransform will then automatically scale and translate this buffer to fit exactly with the size of the sequence window int the GUI.
   * The strand orientation is also taken care of automatically, since the transform will flip the graphics buffer if necessary.
   * Example: even if the sequence window in the GUI is 800 pixels wide, the DTV could be told to draw only 340 bases, and these will be stretched 
   * to fit the 800 pixels.  However, if the user zooms out so that e.g. a full sequence of length 200,000bp is shown in the middle 300 pixels of the 800 pixels GUI window,
   * it would be foolish to waste time drawing 200Kbp into a buffer when these will be shrinked down to fit into only 300px. 
   * This is where the "optimized mode" comes in to play. The optimized mode allows the DTV to draw the visible sequence segment at 1:1 scale in the GUI,
   * meaning that one x-coordinate in the graphics object (one "canvas pixel") will be drawn as one pixel in the final visualization on screen.
   * In this case, the DTV should divide up the 200,000bp visible segment into 300 subsegments and choose 1 value from each subsegment (this could be a single base value or an aggregated representative value).
   * Each of these values are then drawn into the 300 pixel wide buffer (again starting at X=0, since the buffer will be translated automatically if necessary).
   * If the visible sequence segment is smaller than the pixel width to draw into, e.g. if 30 bases should be drawn into 300 pixels, 
   * then it will be the responsibility of the DTV to draw each base 10 pixels wide so as to fit correctly into the sequence window.
   * In the optimized mode, the DTV is also responsible for drawing in the opposite direction when displaying the opposite strand, since this is not handled automatically.
   * In short:  
   *    - regular mode (non-optimized) uses the "scaleTransform" where drawing is done at 1:1 scale between the DNA sequence bases and the graphics buffer coordinates (but one base does not have to correspond to one pixel in the final rendering, since the graphics buffer can be streched or shrinked)
   *    - optimized mode uses the "saveAt" transform where drawing is done at 1:1 scale between the graphics buffer coordinates and the actual pixels on screen
   * 
   * The actual painting is done in several steps:
   * 
   *   1) The DTVs "optimizeForSpeed()" method is called. 
   *      If this returns TRUE it means that the optimized mode should be used (with the "saveAt" transform).
   *      If the method returns FALSE, the DTV should just draw the specified sequence segment into the graphics buffer with one base per coordinate. Scaling us done automatically.
   *   2) The "drawVisibleSegmentBackground()" method is called to draw the background
   *   3) The "drawVisibleSegment()" method is called to draw the foreground
   *   4) The "drawVisibleSegmentOverlay()" method is called to draw overlays. 
   *      Overlays are stuff that can be superimposed on top of the foreground. These are always drawn with the "optimized" transform ("saveAt") with 1:1 ratio between graphics buffer coordinates and pixels. 
   *      This method can be implemented to draw stuff that should NOT be scaled automatically, which can sometimes make the drawing process a bit easier depending on what is to be drawn.
   *   5) If this track is the parent of a grouped track, the steps 1+3+4 will be repeated for each child.
   *      I.e. each child will be asked if it wants to use the regular or the optimized mode, then its foreground is drawn (but not the background!) and its overlays (with "saveAt" transform)
   *   6) Finally, the "drawEditOverlay()" method will be called to draw an overlay for the "Draw Tool" when this is in use (with "saveAt" transform)
   *   7) If "show track names" are enabled, the track name will be drawn superimposed.
   */
    @Override
    public void paintComponent(Graphics g) { // this is the method that actually paints the track 
          super.paintComponent(g);
          if (sequencedata==null) {
              installSequenceData();
              if (sequencedata==null) {
                  setToolTipText(featureName+" data missing");
                  return; // data track is still missing, so we will not be able to paint at this moment
              }
          }   
          DataTrackVisualizer[] groupChildren=getGroupChildred();
          int height=getTrackHeight();
          int width=settings.getSequenceWindowSize();
          int sequenceGenomicStart=sequencedata.getRegionStart();
          int sequenceGenomicEnd=sequencedata.getRegionEnd();
          int yoffset=0;
          int xoffset=0;
          Border border=getBorder();
          if (border!=null) {
              Insets insets=getBorder().getBorderInsets(this);
              yoffset=insets.top;
              xoffset=insets.left;
          }    

          int viewPortStart=settings.getSequenceViewPortStart(sequenceName);
          int viewPortEnd=settings.getSequenceViewPortEnd(sequenceName);
          double scaleFactor=settings.getScale(sequenceName);
          int orientation=settings.getSequenceOrientation(sequenceName);
          isVisualizingDirectStrand=(orientation==Sequence.DIRECT);
          isVisualizingReverseStrand=(orientation==Sequence.REVERSE);          
          isVisualizingRelativeStrand=(orientation==geneOrientation);
          boolean optimize=optimizeForSpeed(scaleFactor);
          int startPosWithinSequence=(viewPortStart>sequenceGenomicStart)?viewPortStart:sequenceGenomicStart; // start at highest
          int endPosWithinSequence=(viewPortEnd>sequenceGenomicEnd)?sequenceGenomicEnd:viewPortEnd; // end at lowest
          int drawnSequenceRegionLength=endPosWithinSequence-startPosWithinSequence+1; // size of the sequence segment to be drawn (in bp)
          int relativeStart;
          int relativeStart_regular=0;
          int relativeStart_optimized=0;
          Graphics2D buffer=(Graphics2D)g;                  
          saveAt=buffer.getTransform(); // this is the default transform ("no transform")
          AffineTransform scaleTransform; // The transform used for scaling and translation in non-optimized mode
          
          if (drawnSequenceRegionLength>0) {
              if (isPaintingForPrint()) {
                  Color background=settings.getVisualizationPanelBackgroundColor();
                  if (background==null) background=Color.GRAY;
                  buffer.setColor(background);
                  buffer.drawRect(0,0,width,height);
                  buffer.fillRect(0,0,width,height);
              }
              relativeStart_regular=startPosWithinSequence-sequenceGenomicStart; // relativeStart is always >=0 and translation offset is made by affine transform
              relativeStart_optimized=viewPortStart-sequenceGenomicStart;

              int translation=sequenceGenomicStart-viewPortStart+relativeStart_regular;  
              if (orientation==VisualizationSettings.REVERSE) translation-=(int)(Math.ceil((double)width/(double)scaleFactor))+2; // reverse oriented sequences should be "right aligned"
              double orientedScaleFactor=scaleFactor*orientation; // use affine transform to draw reverse orientations by flipping the sequence
              // filled rectanges (with some width) rather than narrow lines. In my experience, it was best to
              // used filled (but not outlined rectangles) to achieve this. When scale is large, the filled
              // outline of the rectange would occupy 50% of the width of a base, so for a rectangle of
              // width=2 the filling would occupy the middle 50% of 2 bases (which equals the width of one base)
              // Since I only use this filled part I must therefore translate the sequence slightly to
              // make up for this, and the size of this translation is scaledependent.
              scaleTransform=buffer.getTransform();
              scaleTransform.translate(-scaleFactor+xoffset,0);            
              scaleTransform.scale(orientedScaleFactor,1.0);
              scaleTransform.translate(translation,0); //  
                 
              // - - - - -
              relativeStart=(optimize)?relativeStart_optimized:relativeStart_regular;
              // - - - First draw the background  (The background is only painted for the parent track)             
              if (!optimize) buffer.setTransform(scaleTransform);              
              drawVisibleSegmentBackground(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset, yoffset, orientation, optimize); 
              // - - - Now draw the track itself: first the underlays (1:1), the the graph at arbitrary scales, and then overlays (1:1)
              buffer.setTransform(saveAt); // restore default transform to draw underlays
              drawVisibleSegmentUnderlay(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset, yoffset, orientation);              
              if (!optimize) buffer.setTransform(scaleTransform);
              drawVisibleSegment(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset, yoffset, orientation, optimize);              
              buffer.setTransform(saveAt); // restore default transform to draw overlays
              drawVisibleSegmentOverlay(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset, yoffset, orientation);  
              // - - - - -
              if (groupChildren!=null) { // draw children in grouped track
                  for (DataTrackVisualizer dtv:groupChildren) {
                      if (dtv!=null) {
                          dtv.saveAt=saveAt; // set the same saveAt transform in the child. Note that this does not change the buffer transform, it only sets a property that the object can access if it wants to
                          boolean optimizeInChild=dtv.optimizeForSpeed(scaleFactor);
                          relativeStart=(optimizeInChild)?relativeStart_optimized:relativeStart_regular;
                          buffer.setTransform(saveAt);
                          dtv.drawVisibleSegmentUnderlay(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset, yoffset, orientation);                          
                          buffer.setTransform((optimizeInChild)?saveAt:scaleTransform);                                
                          dtv.drawVisibleSegment(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset, yoffset, orientation, optimizeInChild);
                          buffer.setTransform(saveAt);
                          dtv.drawVisibleSegmentOverlay(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset, yoffset, orientation);
                      }
                  }
              }   
              buffer.setTransform(saveAt); // restore default transform again before drawing the "edit overlay"
              drawEditOverlay(buffer, relativeStart, height, width, drawnSequenceRegionLength, xoffset,yoffset, orientation);  
              // - - - - -              
             int labelStyle=(Integer)settings.getSettingAsType(VisualizationSettings.TRACK_LABEL_STYLE, new Integer(2));              
             if (labelStyle>=0) {                  
                 double align=(Double)settings.getSettingAsType(VisualizationSettings.TRACK_LABEL_ALIGNMENT, new Double(0.0));
                 int fontSize=(Integer)settings.getSettingAsType(VisualizationSettings.TRACK_LABEL_SIZE, new Integer(10));
                 if (fontSize>4 && fontSize<=40 && trackNameFont.getSize()!=fontSize) trackNameFont=new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, fontSize); // To Do: allow font to be changed just like other system fonts!
                 int ascent=g.getFontMetrics(trackNameFont).getAscent();              
                 if (ascent+4<height) {
                     ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
                     g.setFont(trackNameFont);                  
                     drawTrackLabel(labelStyle, buffer, width, ascent, align);
                     ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                 } // do not draw if track height is too small    
              } // end showTrackNames
          } // end (drawnSequenceRegionLength>0)
      
     } // end paintComponent
           
  private static final Color labelShadowColor=new Color(0,0,0,32); 
  
  private void drawTrackLabel(int labelStyle, Graphics2D g, int width, int ascent, double align) {
      int x=5, y=2+ascent;
      int stringwidth=g.getFontMetrics(trackNameFont).stringWidth(featureName);
      if (align==0.5) {                 
          x=(width-stringwidth)/2;                      
      } else if (align==1.0) {                  
         x=width-(stringwidth+10);
      }   
     if (labelStyle==0) { // draw track label as rounded white rectangle with black border and black text                
          g.setColor(labelShadowColor); // shadow
          g.fillRoundRect(x-2+3, 3+3, stringwidth+8, ascent+2,12,12);        
          g.setColor(Color.WHITE);
          g.fillRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12);               
          g.setColor(Color.BLACK);
          g.drawRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12); 
          g.drawString(featureName, x+3, y);
      } else if (labelStyle==1) { // draw track label as rounded white rectangle with black border and text in color of feature                
          g.setColor(labelShadowColor); // shadow
          g.fillRoundRect(x-2+3, 3+3, stringwidth+8, ascent+2,12,12);       
          g.setColor(Color.WHITE);
          g.fillRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12);               
          g.setColor(Color.BLACK);
          g.drawRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12); 
          if (sequencedata instanceof DNASequenceData) g.setColor(Color.BLACK);  
          else g.setColor(settings.getForeGroundColor(featureName));
          g.drawString(featureName, x+3, y);
      } else if (labelStyle==2) { // draw track label as rounded rectangle in color of feature with black border and black text                
          g.setColor(labelShadowColor); // shadow
          g.fillRoundRect(x-2+3, 3+3, stringwidth+8, ascent+2,12,12);       
          Color color=settings.getForeGroundColor(featureName);
          if (color.equals(Color.BLACK)) color=Color.lightGray;
          else color=VisualizationSettings.brighter(color, 0.31f);
          g.setColor(color);
          if (sequencedata instanceof DNASequenceData) g.setColor(Color.WHITE);  
          g.fillRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12);               
          g.setColor(Color.BLACK);
          g.drawRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12); 
          g.setColor((getBrightness(color)<130)?Color.WHITE:Color.BLACK);
          g.drawString(featureName, x+3, y);
      } else if (labelStyle==3) { // draw track label as rounded black rectangle with white text                
          g.setColor(labelShadowColor); // shadow
          g.fillRoundRect(x-2+2, 3+2, stringwidth+8, ascent+2,12,12);                                       
          g.setColor(Color.BLACK);
          g.fillRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12);               
          g.setColor(Color.WHITE);                
          g.drawString(featureName, x+3, y);
      } else if (labelStyle==4) { // draw track label as rounded black rectangle with text in feature color           
          g.setColor(labelShadowColor); // shadow
          g.fillRoundRect(x-2+2, 3+2, stringwidth+8, ascent+2,12,12);                                       
          g.setColor(Color.BLACK);
          g.fillRoundRect(x-2, 3, stringwidth+8, ascent+2,12,12);               
          if (sequencedata instanceof DNASequenceData) g.setColor(Color.WHITE);  
          else {
              Color color=settings.getForeGroundColor(featureName);
              if (color.equals(Color.BLACK)) g.setColor(Color.WHITE); else g.setColor(color);             
          }                   
          g.drawString(featureName, x+3, y);
      } else if (labelStyle==5) { // draw track label as gray text with white "shadow" to make the gray colored name stand out more against a dark background
          g.setColor(Color.WHITE); 
          g.drawString(featureName, x+1, y+1);                               
          g.setColor(Color.GRAY);
          g.drawString(featureName, x, y);                     
      } else { // draw track label as simple black text                            
          g.setColor(Color.BLACK);
          g.drawString(featureName, x, y);                     
      }            
  }

/**
 * This function renders the actual datatrack. Subclasses for different graph types should override this method
 * and render the track according to the parameters given (and the data fields in this class)
 * 
 * @param graphics An instance of the graphics object where the datatrack should be rendered
 * @param start The relative start of viewPort within the sequence. The segment of the sequence that should be rendered is decided by "start" and "bases"
 *              In non-optimized mode, the start will always be positive (>=0) and the rendered buffer will be translated automatically. 
 *              In optimized mode, the start can be negative if the viewport starts before the start of the sequence itself.
 * @param height The height to be used for visualization of the datatrack
 * @param width The width of the whole sequence window (in pixels). This is not necessarily the same as the number of bases in the viewPort
 * @param bases The number of bases that should be drawn (bases from the sequence that actually fall inside the viewport, not the total number of bases within the viewport itself).
 * @param graphicsXoffset The x-coordinate within the graphics object where rendering should be placed
 * @param graphicsYoffset The y-coordinate within the graphics object where rendering should be placed
 * @param orientation The strand/orientation that should be used when rendering (DIRECT=1, REVERSE=-1). I.e. actual strand to display in the visualization, irrespective of the original strand of the sequence.
 * @param optimize If FALSE, each of the bases in the viewport should be drawn 1 graphics coordinate wide in the canvas and an affine transform will automatically be applied to fit the size of the viewport in within the sequence window (in pixels). 
 *                 If TRUE, the visualizer must select an appropriate subset of 'width' equidistant values from the viewport to be visualized in the window at normal scale (=1.0, one graphics coordinate = one pixel)
 *                 Normally, when optimize=FALSE you only need to draw the part of the sequence that is actually visible, which could just a a few bases if you zoom in close.
 *                 The calling function would ensure (via affine transforms) that the buffer you draw into (at 1 graphics coordinate per base) is correctly positioned and stretched to fit the full sequence window.
 *                 However, if you zoom out wide, then each pixel would cover many bases, and it would be a waste of resources to draw e.g. a full sequence with thousands 
 *                 of bases into a buffer if this buffer is then squished into a few dozen pixels in the sequence window. This is where the "optimization" comes in.
 *                 When optimize=true, the graphics will not be transformed/scaled automatically. Rather, the implementor of this function must choose which values to
 *                 draw into the sequence window at 1:1 scale (one graphics coordinate=one pixel). Tthe values could be an equidistant subsampling of the full visible sequence segment.    
 *             
 */
    public abstract void drawVisibleSegment(Graphics2D graphics, int start, int height, int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize);

    
     /**
     * This method can be overridden by subclasses in order to draw additional background behind the track in scale=1.0 before the track is painted normally at arbitrary scales
     * @param graphics
     * @param start
     * @param height
     * @param width
     * @param bases  The number of bases to be rendered
     * @param graphicsXoffset
     * @param graphicsYoffset
     * @param orientation
     */
    public void drawVisibleSegmentUnderlay(Graphics2D graphics, int start, int height, int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation) {}

    
    /**
     * This method can be overridden by subclasses in order to overlay additional annotations on top of the track in scale=1.0 after the track has been painted normally at arbitrary scales
     * @param graphics
     * @param start
     * @param height
     * @param width
     * @param bases  The number of bases to be rendered
     * @param graphicsXoffset
     * @param graphicsYoffset
     * @param orientation
     */
    public void drawVisibleSegmentOverlay(Graphics2D graphics, int start, int height, int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation) {}

    /**
     * This method can be overridden by subclasses to draw the track background
     * @param graphics
     * @param start
     * @param height
     * @param width
     * @param bases  The number of bases to be rendered
     * @param graphicsXoffset
     * @param graphicsYoffset
     * @param orientation
     * @param optimize (see drawVisibleSegment)
     */
    public void drawVisibleSegmentBackground(Graphics2D graphics, int start, int height, int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {}    
    
    
    /**
     * This method can be overridden by subclasses to draw an "edit" layer on top of the track and overlay. 
     * This should only be drawn when editing is in progress.
     * @param graphics
     * @param start
     * @param height
     * @param width
     * @param bases  The number of bases to be rendered
     * @param graphicsXoffset
     * @param graphicsYoffset
     * @param orientation
     */    
    public void drawEditOverlay(Graphics2D g, int start, int height, int width, int bases, int xoffset, int yoffset, int orientation) {}    

    
    /** Returns the currently visualized strand */
    public int getStrandOrientation() {
        return settings.getSequenceOrientation(sequenceName);
    }
    
    // ****************** mouse event listeners ********************
    
    @Override
    public void mouseClicked(MouseEvent e) {}  // unneeded interface implementation
    @Override
    public void mouseEntered(MouseEvent e) {}  // unneeded interface implementation
    @Override
    public void mouseExited(MouseEvent e) {}   // unneeded interface implementation
    @Override   
    public void mousePressed(MouseEvent e) { } // unneeded interface implementation
    @Override    
    public void mouseReleased(MouseEvent e) {} // unneeded interface implementation
    @Override    
    public void mouseDragged(MouseEvent e) {}  // unneeded interface implementation
    @Override
    public void mouseMoved(MouseEvent e) { // update tooltip based on position under mouse pointer
          int x=e.getPoint().x;
          if (x<=0 || x>settings.getSequenceWindowSize()
                   || sequencedata==null
                   || SequenceVisualizerContextMenu.isPopupShowing()
          ) {
              setToolTipText(null);
              return;
          }
          x=x-bordersize;
          int[] range=getGenomicCoordinateFromScreenOffset(x);
          int i=(int)((range[0]+range[1])/2); // use midpoint
          //gui.statusMessage("range="+range[0]+"-"+range[1]);
          currentmouseposition=i;
          if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
              setToolTipText(null);
              return;             
          }
          int shownOrientation=settings.getSequenceOrientation(sequenceName);
          StringBuilder builder=new StringBuilder();
          builder.append("<html><nobr>position = ");
          describeBase(i,shownOrientation,builder);
          builder.append("</html>");
          setToolTipText(builder.toString());
    }
  
    /** This method provides a tooltip text to be displayed when the mouse pointer hoovers over a base
     *  The text will be appended to the StringBuilder object provided as argument
     */
    private void describeBase(int genomicCoordinate,int shownOrientation, StringBuilder builder) {
          builder.append(MotifLabEngine.groupDigitsInNumber(genomicCoordinate));
          if (TSS>=0) {
              int relative=(geneOrientation==VisualizationSettings.DIRECT)?genomicCoordinate-TSS:TSS-genomicCoordinate;
              int relativeTSS=0;
              if (gui.skipPosition0()) {
                  if (relative>=0) relative++; // because there is no 0
                  relativeTSS=1;
              }
              if (relative==relativeTSS) builder.append((relative==1)?" [+1 TSS]":" [+0 TSS]");
              else {
                  builder.append(" [");
                  if (relative>=0) builder.append("+");
                  builder.append(relative);
                  builder.append("]");
              }                 
              
          }
          builder.append("&nbsp;&nbsp;<b>");
          builder.append(featureName);
          builder.append("</b></nobr>");
          builder.append("<br>");         
          describeBaseForTooltip(genomicCoordinate,shownOrientation,builder);  // DTV subclass should fill in the rest of the description     
    }

    /**
     * This method should be implemented by DTVs to describe the base under the mouse for use in a tooltip.
     * The description should be added to the provided StringBuilder buffer (HTML tags are allowed in the text). 
     * Note that this text is only used as a default tooltip when no other types of special tooltips are in use 
     * (e.g. special graphical tooltips for motif regions etc.)
     * @param genomicCoordinate the base currently pointed at by the mouse
     * @param shownOrientation The orientation that the sequence is currently shown in 
     * @param buffer A buffer to put the description into
     */
    public abstract void describeBaseForTooltip(int genomicCoordinate,int shownOrientation, StringBuilder buffer);    
    
    public String getPositionString(int genomicCoordinate) {
          String description=""+MotifLabEngine.groupDigitsInNumber(genomicCoordinate);
          if (TSS>=0) {
              int relative=(geneOrientation==VisualizationSettings.DIRECT)?genomicCoordinate-TSS:TSS-genomicCoordinate;
              int relativeTSS=0;
              if (gui.skipPosition0()) {
                  if (relative>=0) relative++; // because there is no 0
                  relativeTSS=1;
              }
              if (relative==relativeTSS) description+=((relative==1)?" [+1 TSS]":" [+0 TSS]");            
              else if (relative>=0) description+=" [+"+relative+"]";
              else description+=" ["+relative+"]";
          }
          return description;
    }
    

    
    
    /** Returns the Watson-Crick complementary base for the specified base. Case is conserved */
    public char reverseBase(char base) {
        char reversebase;
        switch (base) {
            case 'a':reversebase='t';break;
            case 'A':reversebase='T';break;
            case 'c':reversebase='g';break;
            case 'C':reversebase='G';break;
            case 'g':reversebase='c';break;
            case 'G':reversebase='C';break;
            case 't':reversebase='a';break;
            case 'T':reversebase='A';break;
            default:reversebase=base;break;
        }
        return reversebase;    
    }
    
    
    protected int getBrightness(Color c) {
        return (int) Math.sqrt(
          c.getRed() * c.getRed() * .241 +
          c.getGreen() * c.getGreen() * .691 +
          c.getBlue() * c.getBlue() * .068);
    }    
    
    // ****************** End of mouse event listeners ********************
  

    public int[] getGenomicCoordinateFromScreenOffset(int x) {
        return settings.getGenomicCoordinateFromScreenOffset(sequenceName,x);   
    }
    
    public int[] getScreenCoordinateFromGenomic(int x) {
        return settings.getScreenCoordinateFromGenomic(sequenceName,x);       
    } 
    
    /** Given two genomic coordinates. This method returns the leftmost and rightmost 
     *  coordinates on the screen corresponding to the genomic range     
     * @param start the start of the range in genome coordinates
     * @param end the end of the range in genome coordinates
     * @return the first and last screen pixel corresponding to the genomic range
     */
    public int[] getScreenCoordinateRangeFromGenomic(int start, int end) {
        int first[] = settings.getScreenCoordinateFromGenomic(sequenceName,start);
        int last[]  = settings.getScreenCoordinateFromGenomic(sequenceName,end);
        int startX  = (first[0]<=last[0])?first[0]:last[0];
        int endX    = (first[0]<=last[0])?last[1]:first[1];        
        return new int[]{startX,endX};     
    }  
    
    public int[] getScreenCoordinateRangeFromGenomic(int start, int end, int xOffset) {
        int first[] = settings.getScreenCoordinateFromGenomic(sequenceName,start);
        int last[]  = settings.getScreenCoordinateFromGenomic(sequenceName,end);
        int startX  = (first[0]<=last[0])?first[0]:last[0];
        int endX    = (first[0]<=last[0])?last[1]:first[1];        
        return new int[]{startX+xOffset,endX+xOffset};     
    }       
    
    /** Calls this components repaint function but only for the visible segment of the sequence.
     *  This function was implemented as an alternative to the regular repaint() function 
     *  to avoid visual artifacts that would appear inside the datatrack component 
     *  in the flanks on either side of the visible sequence segment when the DRAW tool was used
     *  (specifically, other parts of the screen would be redrawn inside the DTV).
     * 
     */
    public void repaintVisibleSequenceSegment() {        
        int vizStart=settings.getSequenceViewPortStart(sequenceName);
        int vizEnd=settings.getSequenceViewPortEnd(sequenceName);      
        if (vizStart<sequenceStart) vizStart=sequenceStart;
        if (vizEnd>sequenceEnd) vizEnd=sequenceEnd;               
        int[] screenrange = getScreenCoordinateRangeFromGenomic(vizStart,vizEnd);
        int width=screenrange[1]-screenrange[0]+1;   
        int yoffset=0;
        int xoffset=0;
        Border border=getBorder();
        if (border!=null) {
              Insets insets=getBorder().getBorderInsets(this);
              yoffset=insets.top;
              xoffset=insets.left;
        }          
        this.repaint(screenrange[0]+xoffset, yoffset, width, getHeight()); // 
    }
    /** 
     * This method should return true if visualization should be optimized for speed
     * at the specified scale. When visualization is optimized for speed, the bases from
     * the viewport are sampled equidistantly (or averaged over interval) and rendered
     * at 1-to-1 scale rather than rendering all the bases and using an affine transform
     * to scale the viewport to fit the sequence window
     */
    public boolean optimizeForSpeed(double scale) {
        return false;
    }
    
    /** Returns the genomic coordinate corresponding to the current mouse position */
    public int getCurrentGenomicMousePosition() {
        return currentmouseposition;
    }
    
    /** Returns the FeatureSequenceData visualized by this class */
    public FeatureSequenceData getTrack() {
        return sequencedata;
    }
    
    // -------------------------  Graph type attributes  ---------------------------------------------------------------    
    
    /**
     * @return A list of settings that are specific for the graph type 
     */
    ArrayList<DataTrackVisualizerSetting> getGraphTypeSettings() {
        return null; // this can be overriden in subclasses to add more graph type specific settings!
    }
    
    public ArrayList<DataTrackVisualizerSetting> addGraphTypeSetting(ArrayList<DataTrackVisualizerSetting> list, DataTrackVisualizerSetting setting, boolean merge) {
        return addGraphTypeSetting(list, setting, merge, null, false);
    }    
    
    /**
     * A convenience method to add more graph type settings to an existing list.
     * The conveniences include replacing or merging new the setting with an existing setting in the list (that has the same name)
     * or adding the new setting at a specific place in the list, either at the beginning or end or before or after a specific entry
     * (If none of these conveniences are required, you could just add new settings with list.add() as usual)
     * @param list A list of entries. This can be updated by the method
     * @param setting A new setting to add to the list
     * @param merge If FALSE, the new setting will replace an existing entry in the list if it has the same name.
     *              If TRUE, if the new setting has the same name as an existing entry, the options for the new entry will be merged into the existing entry
     * @param relativeTo If a name of an existing setting is provided, the new setting will be added either directly above or below this setting
     *                   Note that if the new setting replaces or merges with an existing setting that has the same name, the relativeTo parameter will be ignored
     * @param before If the name of an existing setting is provided with the "relativeTo" parameter, the new setting will be added either immediately before (if TRUE) or after (if FALSE) that setting
     *               If no name is provided (or the setting does not exist) The new setting will be added either to the beginning (above==TRUE) or end (above==FALSE) of the list
     * @return The list of settings. If a list was provided, the same list will be returned (or else a new list will be created)
     */
    public ArrayList<DataTrackVisualizerSetting> addGraphTypeSetting(ArrayList<DataTrackVisualizerSetting> list, DataTrackVisualizerSetting setting, boolean merge, String relativeTo, boolean before) { 
        if (setting.isSeparator()) {list=addGraphTypeSettingSeparator(list, setting.isMinorSetting(), setting.getScope(), relativeTo, before); return list;}
        if (list==null) {
            list=new ArrayList<>();
            list.add(setting);
        } else {
            int indexFound=-1;
            for (int i=0;i<list.size();i++) {
               DataTrackVisualizerSetting entry=list.get(i);
               if (entry.name().equals(setting.name())) {indexFound=i;break;}
            }            
            if (indexFound>=0) { // a setting with the same name exists already. Either replace the old setting or merge with it
                if (merge) {                   
                    DataTrackVisualizerSetting entry=list.get(indexFound); // DataTrackVisualizerSettings are often defined as STATIC objects to save space, but since we don't want to make changes to possibly static object, we clone it instead
                    DataTrackVisualizerSetting clone=entry.clone();
                    clone.mergeOptions(setting);
                    list.set(indexFound,clone);
                } else list.set(indexFound, setting);
            } else {
                int insertAt=(before)?0:list.size();
                if (relativeTo!=null) { // adjust insertAt index
                    int found=findSettingInList(list,relativeTo);
                    if (found>=0) insertAt=(before)?found:found+1;
                } 
                list.add(insertAt,setting);        
            }
        }
        return list;        
    }
    
    private int findSettingInList(ArrayList<DataTrackVisualizerSetting> list, String settingName) {
        if (list==null || list.isEmpty()) return -1;
        for (int i=0;i<list.size();i++) {
            DataTrackVisualizerSetting setting=list.get(i);
            if (settingName.equals(setting.name())) return i;
        }
        return -1;
    }
    
    public ArrayList<DataTrackVisualizerSetting> addGraphTypeSettingSeparator(ArrayList<DataTrackVisualizerSetting> list, boolean isMinor, int scope, String relativeTo, boolean before) {
        if (list==null) list=new ArrayList<>();
        DataTrackVisualizerSetting separator=new DataTrackVisualizerSetting(DataTrackVisualizerSetting.SEPARATOR,isMinor,scope);
        int insertAt=(before)?0:list.size();
        if (relativeTo!=null) { // adjust insertAt index
            int found=findSettingInList(list,relativeTo);
            if (found>=0) insertAt=(before)?found:found+1;
        }         
        list.add(insertAt,separator);
        return list;          
    }  
    
    public ArrayList<DataTrackVisualizerSetting> addGraphTypeSettingSeparator(ArrayList<DataTrackVisualizerSetting> list, boolean isMinor, int scope) {
        return addGraphTypeSettingSeparator(list,isMinor,scope,null,false);    
    }      
    
    /**
     * A convenience method to remove graph type settings from an existing list
     * @param list A list of entries. This can be updated by the method
     * @param remove An array of names for settings that should be removed from the list
     */
    public void removeGraphTypeSettings(ArrayList<DataTrackVisualizerSetting> list, String[] remove) {
        if (list!=null && !list.isEmpty()) {
            Iterator itr = list.iterator(); 
            while (itr.hasNext()) { 
                DataTrackVisualizerSetting entry=(DataTrackVisualizerSetting)itr.next(); 
                boolean toBeRemoved=MotifLabEngine.inArray(entry.name(), remove, true);
                if (toBeRemoved) itr.remove(); 
            }
        }
    }    
   
    /**
     * Returns a dialog to adjust the named graph type setting (or null if the setting does not use dialogs)
     * @param settingName
     * @param target The target of the setting. This can be a single FeatureDataset or a FeatureDataset[] array
     * @param settings
     * @return 
     */
    public JDialog getGraphTypeSettingDialog(String settingName, Object target, VisualizationSettings settings) {
        return null;
    }
    

    
    // ------------------------- DTV Resource attributes ---------------------------------------------------------------

    @Override
    public String getResourceName() {
        return getGraphTypeName();
    }
    
    @Override
    public String getResourceTypeName() {
        return "DTV";
    }    

    @Override
    public Class getResourceClass() {
        return this.getClass();
    }

    @Override
    public Icon getResourceIcon() {
        return getGraphTypeIcon();
    }

    @Override
    public Object getResourceInstance() {
        Class type=getResourceClass();
        try {
            Object resource=type.newInstance();
            return resource;
        } catch (Exception e) {System.err.println(e);}
        return null;
    }
    
    
}
