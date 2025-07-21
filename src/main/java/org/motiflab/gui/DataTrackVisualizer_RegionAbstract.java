package org.motiflab.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import javax.swing.JToolTip;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Region;

/**
 * This is an abstract DataTrackVisualizer class used to render Region Dataset tracks
 * It extends the minimal implementation in the DataTrackVisualizer_Region superclass by adding more functionality
 * and also implements the drawVisibleSegment() method from the top DataTracVisualizer superclass.
 * The reason it is still tagged as an abstract class is that it delegates the actual drawing process 
 * to two new methods that must be implemented by a subclass: drawRegion() and drawConnectors()
 * @author kjetikl
 */
public abstract class DataTrackVisualizer_RegionAbstract extends DataTrackVisualizer_Region {

    private static final DataTrackVisualizerSetting borderSetting;                 
    private static final DataTrackVisualizerSetting shadowSetting;
    private static final DataTrackVisualizerSetting showMotifLogoSetting;
    private static final DataTrackVisualizerSetting scaleMotifLogoSetting; 
    private static final DataTrackVisualizerSetting showTypeLabelSetting;
    private static final DataTrackVisualizerSetting renderUpsideDownSetting;
    private static final DataTrackVisualizerSetting drawDirectionArrowsSetting;    
    private static final DataTrackVisualizerSetting labelColorSetting;        
    private static final DataTrackVisualizerSetting drawNestedSetting;
    private static final DataTrackVisualizerSetting scaleConnectorsSetting;  
    private static final DataTrackVisualizerSetting filterNestedSetting;        
    private static final DataTrackVisualizerSetting cropSetting;    
    
    public static int optimize_cutoff=1000; // If a segment contains more than 1000 regions, try to optimize the visualization by not drawing all
    public static int optimize_step=10; // If a segment contains more than 1000 regions, try to optimize the visualization by not drawing all
    
    protected static final Color TRANSPARENT_WHITE=new Color(255,255,255,220);  // used as background for motif match logos. It is translucent to allow region color to shine through
    protected static final int MOTIF_LOGO_FONT_HEIGHT=25; // if you change this size you must also change the ASCENT_CORRECTION below accordingly (but I am not sure about the relationship...)
    protected static final Font MOTIF_LOGO_FONT=MotifLogo.getLogoFont(MOTIF_LOGO_FONT_HEIGHT);
    protected static final Font TYPE_LABEL_FONT=new Font(Font.SANS_SERIF,Font.BOLD,12); //new Font(Font.SERIF,Font.BOLD,13);
    protected static final char[] BASES=new char[]{'A','C','G','T'};
    protected static final double ASCENT_CORRECTION=-2.5; // this is a needed hack because the ascent returned by the FontMetrics is not really the same height as that of a capital letter (even though that was stated in the documentation)
    protected static final double closeupScaleLimit=10; // closeup = 10x magnification    
    protected static final int clipRectMargin=20; // a little bit of top-margin around the ClipRect to allow some bleeding. Regions will be drawn if they fall within ClipRect+/-margin
    protected FontMetrics label_font_metrics=null;
   
    protected static int typeLabelHeight=15; // the height of the motif name text displayed on regions in closeup mode
    protected static int minlogoheight=8; //
    protected static int shadowOffset=3;   
    protected static Color shadowColor=new Color(0,0,0,32); // This is translucent gray use for drawing region shadows in close up       
    private static Color glowColor=new Color(240,210,0,70); // an additional glow color used for extra emphasis around the current region 
    protected int regionspacing=2; // minimum horizontal spacing between regions (in bp) when packing regions. The 2 bp will make room for triangles outside the boxes to indicate strand orientation
    protected boolean cropAtSequenceEdges=true;
    
    private Rectangle rectangle=new Rectangle(); // a reusable rectangle object       
    private final double[] brightnessfactors=new double[]{0.60,0.60,0.45}; // RGB factors used to derive a brighter shade of a given color. The default factor for blue is slightly smaller so that the brighter color will appear warmer (more yellow) rather than "hospital white"
   
    private MotifTooltip customTooltip=null;    
    private ExpandedViewMotifTooltip expandedViewTooltip=null;    
    
    protected int expandedRegionHeight=3; //=3 Height of regions when scale is too small to draw proper motif boxes
    protected int rowSpacing=2; // vertical spacing between rows
    protected int expanded_track_margin=2; // number of blank pixel lines at the top (and bottom) of the track in expanded mode.
    
    protected boolean showStrand=true;
    protected boolean showScore=true;    
    protected boolean showTypeLabel=true;
    protected boolean showMotifLogo=true;    
    protected boolean drawDirectionArrows=true;   
    protected int labelColorStyle=0; // 0 is dynamic color, 1=BLACK, 2=WHITE
    protected boolean scaleMotifLogoByIC=true;
    protected boolean dropShadows=false;
    protected boolean drawNested=false;     
    protected boolean upsideDown=false;    
    protected int regionBorderStyle=0; // 0=no borders, 1=border in darker shade of the region color, 2=black border  
    protected String connectorType;
    protected boolean scaleConnectors=false;
    protected boolean filterNested=false;
    protected boolean expanded=false;    
    protected int contractedTopMargin=0;
    protected int contractedBottomMargin=0;    
    
    protected Region currentRegion=null;
    protected boolean valid=false; // A flag indicating whether the current layout of the regions is valid. If not they should be repacked   
    protected boolean repaintingAfterError=false;
    
    protected static Color[] defaultBaseColors=new Color[]{Color.GREEN,Color.BLUE,Color.YELLOW,Color.RED,Color.LIGHT_GRAY,Color.LIGHT_GRAY,Color.LIGHT_GRAY,Color.LIGHT_GRAY}; // used for motif match logos. Note that the first four colors will be replaced later
    protected int totalRows = 0;
    protected int sequenceWindowWidth=0;
    
    
    /** Constructs a new DefaultDataTrackVisualizer */
    public DataTrackVisualizer_RegionAbstract() {
        super();     
    }

    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);   
        if (isExpanded()) packRegions(); // performs layout by assigning all overlapping regions to different rows           
        refreshSettings();
    }      
    
    @Override
    public void dataUpdated() {
        valid=false;
        packRegions();
    }    
    
    
     @Override
     public int getTrackHeight() {
        if (sequencedata==null) return super.getTrackHeight();
        if (!valid) packRegions();
        if (hasVariableHeight()) { // this hasVariableHeight() method checks isExpanded setting
            if (totalRows==0) return 6; // use a default size of 6px for empty tracks (just to make it visible)
            return totalRows*expandedRegionHeight+(totalRows-1)*rowSpacing+(2*expanded_track_margin); // rows + spacing + top and bottom margins
        } else return settings.getDataTrackHeight(featureName);
            
     } 
     
     /** Returns the row number of a given Y-coordinate. Note that this assumes the track is not a child in a grouped track */
     private int getRowForY(int y) { 
        y=y-expanded_track_margin; // account for offset
        int rowHeight=expandedRegionHeight+rowSpacing;
        return (int)((double)y/(double)rowHeight);
     }     
     
     /** Returns the Y-coordinate range for the requested row in expanded mode
      *  @param row the requested row number starting at 0(should be within range between 0 and totalRows)
      *  @param height The assumed height of the track. This should normally be the height of the track itself or the height of the parent if track is grouped
      *  @return The top and bottom Y-coordinate for the requested row
      */
     protected int[] getBoundsForRow(int row, int height, int graphicsYoffset) {
        if (!valid) packRegions();
        int useExpandedRegionHeight=expandedRegionHeight;
        int useRowSpacing=rowSpacing;
        int useExpandedTopMargin=expanded_track_margin;
        if (settings.isGroupedTrack(featureName)) { // recalculate region height, rowspacing and top margin to use
            int originalHeight=totalRows*expandedRegionHeight+(totalRows-1)*rowSpacing+(expanded_track_margin+expanded_track_margin);                  
            useExpandedRegionHeight=(int)Math.round(((double)expandedRegionHeight/(double)originalHeight)*(double)height);
            useRowSpacing=(int)Math.round(((double)rowSpacing/(double)originalHeight)*(double)height);
            useExpandedTopMargin=(int)Math.round(((double)expanded_track_margin/(double)originalHeight)*(double)height);    
        }  
        if (useExpandedRegionHeight<1) useExpandedRegionHeight=1;
        int top=useExpandedTopMargin+graphicsYoffset;
        if (row>0) top+=(useExpandedRegionHeight+useRowSpacing)*row;
        int bottom=top+useExpandedRegionHeight-1;
        return new int[]{top,bottom};          
     }
    
    /**
     * Returns TRUE if current display is in "closeup-mode". This usually means that the regions are wide enough to draw superimposed motif logos on top
     */
    protected boolean isCloseUp() {
        return ((isMotifTrack() || isModuleTrack()) && settings.getScale(sequenceName)>=closeupScaleLimit); // && settings.isCloseupViewEnabled()
    }
    
    /**
     * This method can be overridden in subclasses to determine if the superclass should draw the baseline or not
     * @return If this returns TRUE, the DTV should draw the baseline in the track where applicable. If this returns FALSE, the baseline should newer be drawn
     */
    protected boolean shouldDrawBaseline() {
        return true;
    }
       
    @Override
    public void refreshSettings() {
        defaultBaseColors[0]=settings.getBaseColor('A');
        defaultBaseColors[1]=settings.getBaseColor('C');
        defaultBaseColors[2]=settings.getBaseColor('G');
        defaultBaseColors[3]=settings.getBaseColor('T');    
        expandedRegionHeight=settings.getExpandedRegionHeight(featureName); // this is used by several methods, so I update it here
        rowSpacing=settings.getRowSpacing(featureName);        
        showTypeLabel=(Boolean)settings.getSettingAsType(featureName+".showTypeLabel", Boolean.TRUE);  
        showMotifLogo=(Boolean)settings.getSettingAsType(featureName+".showMotifLogo", Boolean.TRUE);  
        drawDirectionArrows=(Boolean)settings.getSettingAsType(featureName+".drawDirectionArrows", Boolean.TRUE); 
        labelColorStyle=(Integer)settings.getSettingAsType(featureName+".typeLabelColor", new Integer(0)); // 0=BLACK or WHITE depending on bg, 1=BLACK, 2=WHITE.
        scaleMotifLogoByIC=(Boolean)settings.getSettingAsType(featureName+".scaleMotifLogoByIC", Boolean.TRUE);          
        regionspacing=(Integer)settings.getSettingAsType(featureName+".regionSpacing", new Integer(regionspacing));
        if (regionspacing<2) regionspacing=2;       
        expanded_track_margin=rowSpacing; // I have chosen to use rowSpacing as margin instead of having the margin as a separate setting (line below)
        // expanded_track_margin=(Integer)settings.getSettingAsType(featureName+".trackMargin", new Integer(expanded_track_margin)); //         
        if (expanded_track_margin<0) expanded_track_margin=0;    
        shadowOffset=(Integer)settings.getSettingAsType(featureName+".shadowOffset", new Integer(shadowOffset)); 
        if (isExpanded() && !settings.hasVariableTrackHeight(featureName)) settings.setVariableTrackHeight(featureName, true); // Just in case: these two should always be synchronized for expanded tracks.
        dropShadows=settings.useDropShadows(featureName);
        drawNested=settings.visualizeNestedRegions(featureName);
        regionBorderStyle=settings.drawRegionBorders(featureName); 
        sequenceWindowWidth=settings.getSequenceWindowSize();
        connectorType=settings.getConnectorType(featureName);
        upsideDown=settings.renderUpsideDown(featureName);        
        cropAtSequenceEdges=(Boolean)settings.getSettingAsType(featureName+".cropAtSequenceEdges", Boolean.TRUE);           
        scaleConnectors=(Boolean)settings.getSettingAsType(featureName+".scaleConnectors", Boolean.FALSE);  
        filterNested=(Boolean)settings.getSettingAsType(featureName+".filterNestedRegions", Boolean.FALSE);
        expanded=isExpanded();
        showStrand=settings.shouldVisualizeRegionStrand(featureName);
        showScore=settings.shouldVisualizeRegionScore(featureName);       
    }
    
    @Override
    public boolean optimizeForSpeed(double scale) {
        return true; // This class should always draw at 1:1 scale
    }    
    
    /**
     * Implementing subclasses should return the names of connector types that it supports
     * @return 
     */
    protected abstract String[] getConnectorTypes();

    @Override
    public void drawVisibleSegment(Graphics2D graphics, int start, int height, int width, int numbases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {  
        // gui.logMessage("DTV["+sequenceName+"]("+featureName+")  height="+getHeight()+" exp="+isExpanded()+"  bounds="+getBounds()+"  clip="+graphics.getClipBounds()+"  seqVizHeight="+gui.getVisualizationPanel().getSequenceVisualizer(sequenceName).getHeight());
        refreshSettings(); // I am misusing this, so it is necessary with a manual call here to update all settings
        // add top and bottom margins in contracted mode
        if (!expanded && (contractedTopMargin!=0 || contractedBottomMargin!=0)) {
            int useTopMargin=(contractedTopMargin<0)?(int)(((-contractedTopMargin)/100.0)*height):contractedTopMargin; // negative values should be interpreted as a percentage, i.e. "-N" means a margin that is N% of height
            int useBottomMargin=(contractedBottomMargin<0)?(int)(((-contractedBottomMargin)/100.0)*height):contractedBottomMargin; // negative values should be interpreted as a percentage, i.e. "-N" means a margin that is N% of height
            int offsetHeight=height-(useTopMargin+useBottomMargin);
            if (offsetHeight>0) { // only apply margins if there is room
                height=offsetHeight;
                graphicsYoffset+=useTopMargin;
            }
        }
        boolean errorFlag=false;
        boolean isModuleTrack=((RegionSequenceData)sequencedata).isModuleTrack();
        boolean isNestedTrack=((RegionSequenceData)sequencedata).isNestedTrack();
        boolean useMulticolor=settings.useMultiColoredRegions(featureName);  
        int aboveHeight=(int)Math.ceil((height-1)/2f); // number of pixels for displaying values above baseline. Here I have just divided the track into two equal halfs
        int belowHeight=height-(aboveHeight+1); // number of pixels for displaying values below baseline              
        int genomicStart=sequencedata.getRegionStart();
        if (start>0) genomicStart+=start; // In optimized mode, "start" (relative to sequence start) can be negative if the VP starts before the sequence, but we will only offset if start is positive
        int genomicEnd=genomicStart+numbases-1;
        Color foreground=settings.getForeGroundColor(featureName);   
        label_font_metrics=graphics.getFontMetrics(TYPE_LABEL_FONT);  
        if (expanded && !valid) packRegions();
        int useExpandedRegionHeight=expandedRegionHeight;
        int useRowSpacing=rowSpacing;
        int useExpandedTopMargin=expanded_track_margin;
        if (settings.isGroupedTrack(featureName)) { // recalculate region height, rowspacing and top margin to use
            int originalHeight=totalRows*expandedRegionHeight+(totalRows-1)*rowSpacing+(expanded_track_margin+expanded_track_margin);                  
            useExpandedRegionHeight=(int)Math.round(((double)expandedRegionHeight/(double)originalHeight)*(double)height);
            useRowSpacing=(int)Math.round(((double)rowSpacing/(double)originalHeight)*(double)height);
            useExpandedTopMargin=(int)Math.round(((double)expanded_track_margin/(double)originalHeight)*(double)height);    
        }     
        if (useExpandedRegionHeight<1) useExpandedRegionHeight=1;
        Rectangle clip=graphics.getClipBounds(); // this should already have been set to the portion of the DTV that is visible on the screen
        int clipTop=clip.y-clipRectMargin;
        int clipBottom=clip.y+clip.height+clipRectMargin;
        int lineheight=useExpandedRegionHeight+useRowSpacing; // distance between rows in expanded mode   
        if (cropAtSequenceEdges) {
            Rectangle cropclip=graphics.getClipBounds(); // this should already have been set to the portion of the DTV that is visible on the screen
            int[] sequencesegmentspan=getScreenCoordinateRangeFromGenomic(genomicStart, genomicEnd, graphicsXoffset);
            if (sequencesegmentspan[0]>0) cropclip.x=sequencesegmentspan[0];
            cropclip.width=sequencesegmentspan[1]-sequencesegmentspan[0]+1; 
            if (cropclip.width>sequenceWindowWidth) cropclip.width=sequenceWindowWidth;
            graphics.setClip(cropclip);
        } // this clips the rendering at the start and end of sequence segment                       
        RegionVisualizationFilter filter=gui.getRegionVisualizationFilter(); 
        if (filter instanceof RegionVisualizationFilterGroup) filter=((RegionVisualizationFilterGroup)filter).getApplicableFilters(featureName); // only consider those filters that actually apply
        else if (filter!=null && !filter.appliesToTrack(featureName)) filter=null;       
        if (filter!=null) filter.sequenceRepainted(sequenceName,featureName); // notify filter(s) that we are about to repaint this track
        boolean isOverlayFilter=(filter!=null && filter.drawsOverlay());
        
        int[] range=((RegionSequenceData)sequencedata).getRegionsOverlappingGenomicIntervalAsSlice(genomicStart, genomicEnd);        
        int numberOfRegions=(range==null)?0:(range[1]-range[0]+1);        
        if (numberOfRegions>0) { 
            // --- Loop through all regions in the visible segment and draw those that are not filtered out ---            
            for (int index=range[0];index<=range[1];index++) { // for each region
                Region region=((RegionSequenceData)sequencedata).getRegionByIndex(index);
                if (region==null) { // This should not happen, but it could be caused by a synchronization thing if the track is altered?
                    errorFlag=true; // The sequence may be in erroneous state which can prevent it from being drawn properly at this time. A new repaint will be scheduled later.
                    continue;
                } 
                if (region.getGenomicEnd()<genomicStart || region.getGenomicStart()>genomicEnd) continue; // skip regions that are out of bounds
                if (!settings.isRegionTypeVisible(region.getType())) continue;       // check if the region type is hidden
                if (filter!=null && !filter.shouldVisualizeRegion(region)) continue; // check if the region is hidden by filters               
                if (expanded) { // check if the region is within the clipbounds. If not, don't waste time painting it!
                    int regionTop=region.row*lineheight;
                    int regionBottom=regionTop+lineheight;
                    if (regionBottom<clipTop || regionTop>clipBottom) continue; // region is outside vertical clipbounds (not visible on screen)     
                }
                
                // START: ----- *** TESTING *** Do not draw very small regions
                // if (optimizeDrawing && region.getLength()*zoom<0.1) continue; // if region is just 1/10th of a pixel wide => do not draw it
                // if (optimizeDrawing && region.getLength()*zoom<1 && index%optimize_step!=0) continue; // only draw every 20th region (except if they are very long)
                // if (optimizeDrawing && region.getLength()*zoom<0.1 && index%20!=0) continue; // only draw every 20th region (except if they are very long)
                // END:   ----- *** TESTING *** Do not draw very small regions

                Color usecolor=(useMulticolor)?settings.getFeatureColor(region.getType()):foreground;  
                Color dynamicColor=(filter!=null)?filter.getDynamicRegionColor(region):null;
                if (dynamicColor!=null) usecolor=dynamicColor;
          
                boolean visibleRegionBox=(usecolor!=null && usecolor.getAlpha()>0); // If the region is totally transparent it should not be drawn (but overlays can be drawn). This can be used as a trick to only draw overlays
   
                if (!(visibleRegionBox || isOverlayFilter)) continue; // neither the region nor overlays need to be drawn
                if (expanded)  rectangle = determineRegionBoundsExpanded(region, graphicsXoffset, graphicsYoffset, useExpandedRegionHeight, useRowSpacing, useExpandedTopMargin, rectangle);              
                else rectangle = determineRegionBounds(region, graphicsXoffset, graphicsYoffset, height, aboveHeight, belowHeight, orientation, rectangle);
                if (visibleRegionBox) { // draw the box for this region
                         if (isModuleTrack) drawModuleRegion(graphics, region, (RegionSequenceData)sequencedata, rectangle, orientation, graphicsXoffset, usecolor, filter); //
                    else if (isNestedTrack) drawNestedRegion(graphics, region, (RegionSequenceData)sequencedata, rectangle, orientation, graphicsXoffset, usecolor, filter); //
                    else drawRegularRegion(graphics, region, (RegionSequenceData)sequencedata, rectangle, null, null, orientation, usecolor, filter); //                                               
                    if (expanded && currentRegion==region && SequenceVisualizer.highlighCurrentRegion && newregion==null) drawRegionHalo(graphics, rectangle);  
                }
                if (isOverlayFilter) drawOverlayFilters(filter, region, graphics, rectangle);             
            } // end: for each region
        } // end: if number of regions > 0
        
        // draw "baseline" if showStrand option is enabled in contracted mode
        if (showStrand && !expanded && shouldDrawBaseline()) {
            int vizStart=settings.getSequenceViewPortStart(sequenceName);
            int vizEnd=settings.getSequenceViewPortEnd(sequenceName);
            if (vizStart<sequenceStart) vizStart=sequenceStart;
            if (vizEnd>sequenceEnd) vizEnd=sequenceEnd;       
            int[] segmentSpan=getScreenCoordinateRangeFromGenomic(vizStart, vizEnd);           
            graphics.setColor(settings.getBaselineColor(featureName));            
            graphics.drawLine(graphicsXoffset+segmentSpan[0], graphicsYoffset+aboveHeight, graphicsXoffset+segmentSpan[1], graphicsYoffset+aboveHeight);         
        }
        
        if (errorFlag) {// Reschedule painting if an error happended. hopefully the problem should go away the next time the sequence is drawn
            if (repaintingAfterError) { // to prevent an endless repainting loop we only allow for one repaint caused by errors
               repaintingAfterError=false;
            } else {
                repaintingAfterError=true;
                repaint();
            }
        } 
        graphics.setClip(clip);
    } // end of drawVisibleSegment

    /**
     *  This will determine where to position the region within the track in contracted mode (its "bounds").
     *  It calculates the region's X-coordinate and width (which depends on the region's genomic location)
     *  as well as the Y-coordinate and height (which depends on score and strand, and some other properties and settings (like "upside down" mode)).     
     */
    private Rectangle determineRegionBounds(Region region, int graphicsXoffset, int graphicsYoffset, int height, int aboveHeight, int belowHeight, int orientation, Rectangle rectangle) {
        double maxvalue=((RegionSequenceData)sequencedata).getMaxScoreValue();                                   
        double unit=height/maxvalue;
        double aboveunit=aboveHeight/maxvalue;
        double belowunit=belowHeight/maxvalue;
        int[] regionSpan=getScreenCoordinateRangeFromGenomic(region.getGenomicStart(), region.getGenomicEnd()); // can this be made more efficient?
        int regionX=regionSpan[0]+graphicsXoffset;
        int regionWidth=regionSpan[1]-regionSpan[0]+1;       
        int regionY=graphicsYoffset;
        int regionHeight=height;
        int regionOrientation=region.getOrientation(); // Orientation to draw region in. This will be based on the region itself as well as the current drawing orientation of the sequence       
        int direction=Region.INDETERMINED;
        if (orientation==regionOrientation) direction=Region.DIRECT;
        else if (orientation!=regionOrientation && regionOrientation!=Region.INDETERMINED) direction=Region.REVERSE;        
        
        // adjust Y-position and height depending on strand and score 
        if (showScore && showStrand) { // show both score and strand
             double score=region.getScore();
             if (score<0) score=0;
             int aboveboxheight=(int)Math.ceil(score*aboveunit);
             int belowboxheight=(int)Math.ceil(score*belowunit);
             if (aboveboxheight<=0) aboveboxheight=1; // set minimum height to 1 px to ensure that every region is visible to some extent
             if (belowboxheight<=0) belowboxheight=1; // set minimum height to 1 px to ensure that every region is visible to some extent            
                  if (direction == Region.DIRECT)  {regionY=graphicsYoffset+(aboveHeight-aboveboxheight);regionHeight=aboveboxheight;}
             else if (direction == Region.REVERSE) {regionY=graphicsYoffset+aboveHeight+1;regionHeight=belowboxheight;}
             else {regionY=graphicsYoffset+(aboveHeight-aboveboxheight);regionHeight=aboveboxheight+belowboxheight+1;} // INDETERMINATE orientation
                  
        } else if (!showScore && showStrand) { // show strand but not score
                 if (direction == Region.DIRECT)  {regionY=graphicsYoffset;regionHeight=aboveHeight;}
            else if (direction == Region.REVERSE) {regionY=graphicsYoffset+aboveHeight+1;regionHeight=belowHeight;}
            else {regionY=graphicsYoffset;regionHeight=aboveHeight+belowHeight+1;} // INDETERMINATE orientation
                 
        } else if (showScore && !showStrand) { // show score but not strand. In this case it is possible to flip the whole track upside down also
            double score=region.getScore();
            int boxheight=(int)Math.ceil(score*unit);
            if (boxheight<=0) boxheight=1; // to ensure that every region is visible to some extent
            regionY=(upsideDown)?(graphicsYoffset):(graphicsYoffset+(height-boxheight));    
            regionHeight=boxheight;            
        } 
        if (rectangle==null) rectangle=new Rectangle(regionX,regionY,regionWidth,regionHeight);
        else rectangle.setBounds(regionX,regionY,regionWidth,regionHeight);
        return rectangle;        
    }    
    
   /** Determines the bounds of a region in expanded mode. The X-coordinate and width will be determined by the region's genomic location, and the Y-coordinate and height by the region's assigned row */
    private Rectangle determineRegionBoundsExpanded(Region region, int graphicsXoffset, int graphicsYoffset, int regionheight, int rowspacing, int topmargin, Rectangle rectangle) { 
        int[] regionSpan=getScreenCoordinateRangeFromGenomic(region.getGenomicStart(), region.getGenomicEnd()); // can this be made more efficient?
        int regionX=regionSpan[0]+graphicsXoffset;
        int regionWidth=regionSpan[1]-regionSpan[0]+1;       
        int regionY=graphicsYoffset+region.row*(regionheight+rowspacing)+topmargin;          
        if (rectangle==null) rectangle=new Rectangle(regionX,regionY,regionWidth,regionheight);
        else rectangle.setBounds(regionX,regionY,regionWidth,regionheight);
        return rectangle;          
    }  
       
    /** Determines the bounds of a nested region. The X-coordinate and width will be determined by the nested region's genomic location while the Y-coordinate and height are inherited from the parent region */
    private Rectangle determineNestedRegionBounds(Region region, int graphicsXoffset, Rectangle parentbounds, Rectangle rectangle) { 
        int[] regionSpan=getScreenCoordinateRangeFromGenomic(region.getGenomicStart(), region.getGenomicEnd()); // can this be made more efficient?
        int regionX=regionSpan[0]+graphicsXoffset;
        int regionWidth=regionSpan[1]-regionSpan[0]+1;               
        if (rectangle==null) rectangle=new Rectangle(regionX,parentbounds.y,regionWidth,parentbounds.height);
        else rectangle.setBounds(regionX,parentbounds.y,regionWidth,parentbounds.height);
        return rectangle;          
    }       
    
    /** Prepares to draw a region within a given rectangle 
     *  The method will determine which colors to use as well as other properties and then call "drawRegion()" to draw the actual region.
     * 
     * @param graphics
     * @param region
     * @param sequencedata
     * @param bounds
     * @param thickStart
     * @param thickEnd
     * @param orientation The orientation that the sequence should be drawn in (could be different from the sequence's actual orientation or the region's orientation)
     * @param usecolor This should not be null
     * @param filter 
     */
    private void drawRegularRegion(Graphics2D graphics, Region region, RegionSequenceData sequencedata, Rectangle bounds, Integer thickStart, Integer thickEnd, int orientation, Color regioncolor, RegionVisualizationFilter filter) {                              
        Color dynamicBorderColor = (filter!=null)?filter.getDynamicRegionBorderColor(region):null; 
        Color borderColor = null;
        if (dynamicBorderColor!=null) borderColor=dynamicBorderColor;
        else if (regionBorderStyle>0) borderColor=(regionBorderStyle==2)?Color.BLACK:regioncolor.darker();
        
        Color dynamicLabelColor   = (filter!=null)?filter.getDynamicRegionLabelColor(region):null;
        Color labelColor=Color.BLACK; // do not change this default (the code below assumes it)!
        if (dynamicLabelColor!=null) labelColor=dynamicLabelColor; 
        else if (labelColorStyle==2 || (labelColorStyle==0 && regioncolor!=null && (getBrightness(regioncolor)<130))) labelColor=java.awt.Color.WHITE;         
        
        Color[] basecolors=(filter!=null)?filter.getDynamicMotifLogoColors(region):null;
        
        Motif motif=null;
        if (isCloseUp()) { // superimpose motif logo if there is enough space                 
            if (settings.showMotifLogoInCloseupView() && bounds.height>=minlogoheight) { //
                String motifName=region.getType();
                motif=(Motif)gui.getEngine().getDataItem(motifName,Motif.class);    
                if (motif!=null) {
                    String sequence=(String)region.getProperty("sequence");
                    if (sequence==null || region.getLength()!=sequence.length() || region.getLength()!=((Motif)motif).getLength()) motif=null; // Mismatch between region and its annotated sequence! Something is wrong! Do not draw logo 
                    // if (region.getOrientation()==Region.INDETERMINED) motif=null; // Do not draw logo if the region's orientation is INDETERMINED 
                }
            }       
        }       
        
        int regionOrientation=region.getOrientation();
        int direction=Region.INDETERMINED;
        if (orientation==regionOrientation) direction=Region.DIRECT;
        else if (orientation!=regionOrientation && regionOrientation!=Region.INDETERMINED) direction=Region.REVERSE;
        
        drawRegion(graphics, region, sequencedata, bounds, direction, regioncolor, borderColor, labelColor, motif, basecolors, thickStart, thickEnd, dropShadows); 
    }
    
    /** Prepares to draw a module motif region (i.e. the nested child of a module parent) within a given rectangle 
     *  The method will determine which colors to use as well as other properties and then call "drawRegion()" to draw the actual region.
     *  If the parent region uses color overrides determined by a dynamic filter, these colors will also be applied to the motif child
     *  If the parent region does not specify overrides, the colors will be determined by the dynamic filter applied to the child itself (if a filter is provided)
     *  or else the default colors for the child will be used
     * 
     * @param graphics
     * @param region
     * @param sequencedata
     * @param bounds
     * @param modulemotifname The name of the module motif (common name for the motif equivalence class representing the TF)
     * @param orientation
     * @param dynamicModuleColor The dynamic region color of the parent module region, as determined by a dynamic filter. Or NULL if there is not filter or no dynamic color
     * @param dynamicModuleBorderColor The dynamic border color of the parent module region, as determined by a dynamic filter. Or NULL if there is not filter or no dynamic color
     * @param dynamicModuleLabelColor The dynamic label color of the parent module region, as determined by a dynamic filter. Or NULL if there is not filter or no dynamic color
     * @param dynamicModuleBaseColors The dynamic motif DNA base colors of the parent module region, as determined by a dynamic filter. Or NULL if there is not filter or no dynamic colors
     * @param filter 
     */
    private void drawModuleMotifRegion(Graphics2D graphics, Region region, RegionSequenceData sequencedata, Rectangle bounds, String modulemotifname, int orientation, Color dynamicModuleColor, Color dynamicModuleBorderColor, Color dynamicModuleLabelColor, Color[] dynamicModuleBaseColors, RegionVisualizationFilter filter, boolean drawShadows) {                              
        Color regionColor=dynamicModuleColor; // use parent module's dynamic filter color override, if defined
        if (regionColor==null) {
            regionColor=(filter!=null)?filter.getDynamicRegionColor(region):null;
            if (regionColor==null) regionColor=settings.getFeatureColor(modulemotifname);  
        }
        Color borderColor = dynamicModuleBorderColor;  // use parent module's dynamic filter color override, if defined     
        if (borderColor==null) {
            Color dynamicBorderColor = (filter!=null)?filter.getDynamicRegionBorderColor(region):null; 
            if (dynamicBorderColor!=null) borderColor=dynamicBorderColor;
            else if (regionBorderStyle>0) borderColor=(regionBorderStyle==2)?Color.BLACK:regionColor.darker();            
        }
        Color labelColor=dynamicModuleLabelColor; // use parent module's dynamic filter color override, if defined 
        if (labelColor==null) {
            labelColor=Color.BLACK; // do not change this default (the rest of the code assumes it)!
            Color dynamicLabelColor   = (filter!=null)?filter.getDynamicRegionLabelColor(region):null; 
            if (dynamicLabelColor!=null) labelColor=dynamicLabelColor; 
            else if (labelColorStyle==2 || (labelColorStyle==0 && regionColor!=null && (getBrightness(regionColor)<130))) labelColor=java.awt.Color.WHITE;                
        }
        Color[] basecolors=dynamicModuleBaseColors; // use parent module's dynamic filter color override, if defined    
        if (basecolors==null) {
           basecolors=(filter!=null)?filter.getDynamicMotifLogoColors(region):null; 
        }       
        
        Motif motif=null;
        if (isCloseUp()) { // superimpose motif logo if there is enough space                 
            if (settings.showMotifLogoInCloseupView() && bounds.height>=minlogoheight) { //
                String motifName=region.getType();
                motif=(Motif)gui.getEngine().getDataItem(motifName,Motif.class);    
                if (motif!=null) {
                    String sequence=(String)region.getProperty("sequence");
                    if (sequence==null || region.getLength()!=sequence.length() || region.getLength()!=((Motif)motif).getLength()) motif=null; // Mismatch between region and its annotated sequence! Something is wrong! Do not draw logo 
                    // if (region.getOrientation()==Region.INDETERMINED) motif=null; // Do not draw logo if the region's orientation is INDETERMINED 
                }
            }       
        }       
        
        int regionOrientation=region.getOrientation();
        int direction=Region.INDETERMINED;
        if (orientation==regionOrientation) direction=Region.DIRECT;
        else if (orientation!=regionOrientation && regionOrientation!=Region.INDETERMINED) direction=Region.REVERSE;
        
        drawRegion(graphics, region, sequencedata, bounds, direction, regionColor, borderColor, labelColor, motif, basecolors, null, null, drawShadows); 
    }    
    
    /** Draws a parent module region (or rather a connector) in a module track and then loops through and draws all the nested motif children */    
    private void drawModuleRegion(Graphics2D graphics, Region region, RegionSequenceData sequencedata, Rectangle bounds, int orientation, int graphicsXoffset, Color regioncolor, RegionVisualizationFilter filter) { 
        ModuleCRM cisRegModule=(ModuleCRM)settings.getEngine().getDataItem(region.getType(), ModuleCRM.class);
        if (cisRegModule==null) { // the region type does not correspond to a known module type. Just draw a regular region box                 
            drawRegularRegion(graphics, region, (RegionSequenceData)sequencedata, bounds, null, null, orientation, regioncolor, filter);
        } else { // this is a proper module region. Draw connectors and all nested regions also
            Color dynamicRegionColor = (filter!=null)?filter.getDynamicRegionColor(region):null;
            Color dynamicBorderColor = (filter!=null)?filter.getDynamicRegionBorderColor(region):null; 
            Color dynamicLabelColor = (filter!=null)?filter.getDynamicRegionLabelColor(region):null; 
            Color[] dynamicBaseColors = (filter!=null)?filter.getDynamicMotifLogoColors(region):null;
            Color borderColor = null;
            if (dynamicBorderColor!=null) borderColor=dynamicBorderColor;
            else if (regionBorderStyle>0) borderColor=(regionBorderStyle==2)?Color.BLACK:regioncolor.darker();
            int regionOrientation=region.getOrientation();
            int direction=Region.INDETERMINED;
            if (orientation==regionOrientation) direction=Region.DIRECT;
            else if (orientation!=regionOrientation && regionOrientation!=Region.INDETERMINED) direction=Region.REVERSE;        
            drawConnectors(graphics, region, (RegionSequenceData)sequencedata, bounds, graphicsXoffset, direction, regioncolor, borderColor, connectorType);   
            boolean drawShadows=(dropShadows && !connectorType.equals("Region"));
            if (showTypeLabel) {
                // what about drawing the label? And will the label color depend on the connector type?
            }
            if (drawNested) {
                Rectangle nestedRegionBounds=null;                
                for (int i=0;i<cisRegModule.getCardinality();i++) {
                    String singlemotifname=cisRegModule.getSingleMotifName(i);
                    Object singlemotifregion=region.getProperty(singlemotifname);
                    if (singlemotifregion==null) continue;
                    String mmfeaturename=region.getType()+"."+singlemotifname;
                    if (!settings.isRegionTypeVisible(mmfeaturename)) continue; // do not display this module motif                 
                    if (singlemotifregion instanceof Region) {  
                        Region nestedregion=(Region)singlemotifregion;
                        if (!settings.isRegionTypeVisible(nestedregion.getType())) continue; // do not display this motif
                        if (filterNested && filter!=null && !filter.shouldVisualizeRegion(nestedregion)) continue;                      
                        nestedRegionBounds = determineNestedRegionBounds(nestedregion, graphicsXoffset, bounds, nestedRegionBounds); // this will place the nested region within the Y-bounds of the parent! 
                        if (nestedRegionBounds.x+nestedRegionBounds.width<0 || nestedRegionBounds.x>sequenceWindowWidth+10) continue; // this nested region is outside of X-bounds                        
                        drawModuleMotifRegion(graphics, nestedregion, sequencedata, nestedRegionBounds, mmfeaturename, orientation, dynamicRegionColor, dynamicBorderColor, dynamicLabelColor, dynamicBaseColors, (filterNested)?filter:null, drawShadows);
                    } else if (singlemotifregion instanceof ArrayList) { // multiple sites for this modulemotif
                        for (Object smr:((ArrayList)singlemotifregion)) {
                            if (smr instanceof Region) {
                                Region nestedregion=(Region)smr;
                                if (!settings.isRegionTypeVisible(nestedregion.getType())) continue;
                                if (filterNested && filter!=null && !filter.shouldVisualizeRegion(nestedregion)) continue; // just filter visibility on parent?
                                nestedRegionBounds = determineNestedRegionBounds(nestedregion, graphicsXoffset, bounds, nestedRegionBounds); // this will place the nested region within the Y-bounds of the parent!  
                                if (nestedRegionBounds.x+nestedRegionBounds.width<0 || nestedRegionBounds.x>sequenceWindowWidth+10) continue; // this nested region is outside of X-bounds                        
                                drawModuleMotifRegion(graphics, nestedregion, sequencedata, nestedRegionBounds, mmfeaturename, orientation, dynamicRegionColor, dynamicBorderColor, dynamicLabelColor, dynamicBaseColors, (filterNested)?filter:null, drawShadows);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /** Draws a parent region (or rather a connector) in a nested track and then loops through and draws all the nested children */
    private void drawNestedRegion(Graphics2D graphics, Region region, RegionSequenceData sequencedata, Rectangle bounds, int orientation, int graphicsXoffset, Color regioncolor, RegionVisualizationFilter filter) {            
        Color dynamicBorderColor = (filter!=null)?filter.getDynamicRegionBorderColor(region):null; 
        Color borderColor = null;
        if (dynamicBorderColor!=null) borderColor=dynamicBorderColor;
        else if (regionBorderStyle>0) borderColor=(regionBorderStyle==2)?Color.BLACK:regioncolor.darker();
        int regionOrientation=region.getOrientation();
        int direction=Region.INDETERMINED;
        if (orientation==regionOrientation) direction=Region.DIRECT;
        else if (orientation!=regionOrientation && regionOrientation!=Region.INDETERMINED) direction=Region.REVERSE;        
        drawConnectors(graphics, region, (RegionSequenceData)sequencedata, bounds, graphicsXoffset, direction, regioncolor, borderColor, connectorType);
        boolean drawShadows=dropShadows; // (dropShadows && !connectorType.equals("Region"));
        if (drawNested) {
            Rectangle nestedRegionBounds = null;
            Integer thickStart = (Integer)region.getPropertyAsType("thickStart", Integer.class);
            Integer thickEnd   = (Integer)region.getPropertyAsType("thickEnd", Integer.class);
            int[] thickRange=(thickStart!=null && thickEnd!=null)?getScreenCoordinateRangeFromGenomic(thickStart, thickEnd, graphicsXoffset):null; // this does not have to account for orientation :-)         
            ArrayList<Region> nestedRegions=region.getNestedRegions(false);        
            for (Region nestedregion:nestedRegions) { //  
                if (!settings.isRegionTypeVisible(nestedregion.getType())) continue;                
                if (filterNested && filter!=null && !filter.shouldVisualizeRegion(nestedregion)) continue; // 
                nestedRegionBounds = determineNestedRegionBounds(nestedregion, graphicsXoffset, bounds, nestedRegionBounds); // the Y-placement of this nested region should be within the parent!! (using same Y-bounds as the parent)
                if (nestedRegionBounds.x+nestedRegionBounds.width<0 || nestedRegionBounds.x>sequenceWindowWidth+10) continue; // this nested region is outside of X-bounds
                Integer useThickStart=null;
                Integer useThickEnd=null;
                if (thickRange!=null) {
                    if (thickRange[0]>nestedRegionBounds.x) useThickStart=thickRange[0]; // start of region should be thin
                    if (thickRange[1]<nestedRegionBounds.x+nestedRegionBounds.width) useThickEnd=thickRange[1];                     
                }
                int nestedRegionOrientation=region.getOrientation();
                int nestedDirection=Region.INDETERMINED;
                if (orientation==nestedRegionOrientation) nestedDirection=Region.DIRECT;
                else if (orientation!=nestedRegionOrientation && nestedRegionOrientation!=Region.INDETERMINED) nestedDirection=Region.REVERSE;   
                
                drawRegion(graphics, nestedregion, sequencedata, nestedRegionBounds, nestedDirection, regioncolor, borderColor, null, null, null, useThickStart, useThickEnd, drawShadows); // use the parent's colors for the child, but don't draw the child's label (hence labelcolor=null)
            }  
        }
    }
    
 
    
    /** Process overlay filters (which can be nested into filter groups) */
    private void drawOverlayFilters(RegionVisualizationFilter filter, Region region, Graphics2D graphics, Rectangle rectangle) {
        if (filter instanceof RegionVisualizationFilterGroup) {
             for (RegionVisualizationFilter rvf:((RegionVisualizationFilterGroup)filter).getFilters()) {
                    if (rvf instanceof RegionVisualizationFilterGroup) drawOverlayFilters(rvf, region, graphics, rectangle);
                    else if (rvf!=null && rvf.drawsOverlay()) rvf.drawOverlay(region, graphics, rectangle, settings, this);
             }
        } else if (filter!=null && filter.drawsOverlay()) filter.drawOverlay(region, graphics, rectangle, settings, this);       
    }    
    
   /**
    * 
    * @param g
    * @param motif A motif model from which a PFM can be obtained
    * @param sequence The matching DNA sequence
    * @param xoffset The X-position where the match logo is to be drawn
    * @param yoffset The Y-position where the match logo is to be drawn
    * @param maxwidth The maximum width of the logo (in pixels)
    * @param height The height of the logo
    * @param showDirectMotif If TRUE the logo will be painted from the 
    * @param settings
    * @param basecolors An array of colors to use for the base color. This should contain 8 colors where the first 4 is regular colors for A, C, G, T and the next 4 are mismatch colors for A, C, G, T
    * @param bordercolor
    * @param scaleByIC 
    */ 
   protected void paintMatchLogo(Graphics2D g, Motif motif, CharSequence sequence, int xoffset, int yoffset, int maxwidth, int height, boolean showDirectMotif, VisualizationSettings settings, Color[] basecolors, Paint bordercolor, boolean scaleByIC) {
        if (basecolors==null) basecolors=defaultBaseColors;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);       
        double[][] matrix=motif.getMatrix();
        if (matrix==null) return;
        if (motif.getMatrixType()==Motif.LOG_ODDS_MATRIX) matrix=motif.getMatrixAsFrequencyMatrix();
        FontMetrics metrics=g.getFontMetrics(MOTIF_LOGO_FONT);
        int ascent=(int)(metrics.getAscent()-ASCENT_CORRECTION+0.5);
        int[] letterWidths=new int[]{metrics.charWidth('A'),metrics.charWidth('C'),metrics.charWidth('G'),metrics.charWidth('T')};
        int basewidth=letterWidths[2]; // basewidth == width of letter 'G' (this is the widest)
        double[] letterXoffset=new double[4];          
        letterXoffset[0]=(basewidth-letterWidths[0])/2;
        letterXoffset[1]=(basewidth-letterWidths[1])/2;
        letterXoffset[2]=(basewidth-letterWidths[2])/2;
        letterXoffset[3]=(basewidth-letterWidths[3])/2;
        int logowidth=matrix.length*basewidth;
        g.setFont(MOTIF_LOGO_FONT);
        AffineTransform restore=g.getTransform(); // original transform (to be restored later)
        AffineTransform save=g.getTransform();
        double widthscale=(double)maxwidth/(double)logowidth;
        double heightscale=(double)height/(double)ascent;        
        save.translate(xoffset, yoffset-1);
        if (logowidth!=maxwidth) save.scale(widthscale, heightscale); // scale X-direction so that logo fits irrespective of size
        else save.scale(1, heightscale);        
        g.setTransform(save);
        g.setColor(TRANSPARENT_WHITE);  
        g.fillRect(0, 0, logowidth, ascent);         
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double xpos=0;
        AffineTransform scaleY = new AffineTransform();
        for (int i=0;i<matrix.length;i++) {
            int pos=(showDirectMotif)?i:(matrix.length-(i+1));
            char baseAtPosition=Character.toUpperCase(sequence.charAt(i)); // note that the matching sequence har already been reversed when needed
            double[] counts;
            if (showDirectMotif) counts=new double[]{matrix[pos][0],matrix[pos][1],matrix[pos][2],matrix[pos][3]};
            else counts=new double[]{matrix[pos][3],matrix[pos][2],matrix[pos][1],matrix[pos][0]};
            double total=counts[0]+counts[1]+counts[2]+counts[3];
            double ic=Motif.calculateColumnIC(counts[0], counts[1], counts[2], counts[3], false);
            int[] sorted=new int[4]; // sorted in ascending order. Values are base-indices (i.e. 0=>A, 1=>C, 2=>G, 3=>T)
            int indexA=0, indexC=0, indexG=0, indexT=0;
            if (counts[0]>=counts[1]) indexA++;
            if (counts[0]>=counts[2]) indexA++;
            if (counts[0]>=counts[3]) indexA++;
            if (counts[1]>counts[0])  indexC++;
            if (counts[1]>=counts[2]) indexC++;
            if (counts[1]>=counts[3]) indexC++;
            if (counts[2]>counts[0])  indexG++;
            if (counts[2]>counts[1])  indexG++;
            if (counts[2]>=counts[3]) indexG++;
            if (counts[3]>counts[0])  indexT++;
            if (counts[3]>counts[1])  indexT++;
            if (counts[3]>counts[2])  indexT++;
            sorted[indexA]=0;
            sorted[indexC]=1;
            sorted[indexG]=2;
            sorted[indexT]=3;
            double currentYoffset=0;
            if (scaleByIC) currentYoffset+=(ascent-(ascent*ic/2f));            
            
            for (int j=3;j>=0;j--) { // draws letters from top to bottom (most frequent first)
                int base=sorted[j];
                double fraction=counts[base]/total;
                scaleY.setTransform(save);
                scaleY.translate(letterXoffset[base],currentYoffset); // translated in 1-to-1 scale
                if (scaleByIC) scaleY.scale(1,ic/2f); // scale by IC-content in position
                scaleY.scale(1, fraction);
                if (baseAtPosition==BASES[base]) {
                    g.setColor(basecolors[base]);
                }
                else g.setColor(basecolors[base+4]);                                          
                g.setTransform(scaleY);
                g.drawString(""+BASES[base], (float)xpos, (float)ascent); // draw all letters at same position and use transform to place them correctly
                currentYoffset+=ascent*fraction*((scaleByIC)?(ic/2f):1.0);             
            } // end for each base
            xpos+=basewidth; //
        } // end for each position
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);      
        g.setTransform(restore);  
        if (bordercolor!=null) {
            g.setPaint(bordercolor);
            g.drawRect(xoffset+1, yoffset-1, maxwidth-3, height-1); // Inset border. These numbers work for my purpose, but I don't trust them much
        }  
    }  
    
  
    /**
     * Draws a single region at the specified location according to the specified settings
     * @param graphics
     * @param region The region to be drawn
     * @param sequencedata A reference to the sequence that the region is part of 
     * @param bounds The full bounding box inside which the region should be drawn
     * @param direction The direction to draw the region in. This will depend on the region's orientation relative to visualized sequence orientation. Region.DIRECT==towards the right, REGION.REVERSE==towards the left, or REGION.INDETERMINATE==no direction
     * @param color The color to use for the region
     * @param bordercolor The color to use for the border
     * @param labelcolor The color to use for the label
     * @param motif The motif for the region (if motif track)
     * @param basecolors The colors to use for the bases when drawing a motif logo
     * @param thickStart This marks the start of the "thick" portion of the region. This can be used to distinguish between translated and untranslated regions (UTRs) of a gene. If both thickStart and thickEnd are NULL, a regular region should be drawn
     * @param thickEnd This marks the end of the "thick" portion of the region. This can be used to distinguish between translated and untranslated regions (UTRs) of a gene. If both thickStart and thickEnd are NULL, a regular region should be drawn
     * @param drawShadow If TRUE, a drop shadow should be drawn
     */
    protected abstract void drawRegion(Graphics2D graphics, Region region, RegionSequenceData sequencedata, Rectangle bounds, int direction, Color color, Color bordercolor, Color labelcolor, Motif motif, Color[] basecolors, Integer thickStart, Integer thickEnd, boolean drawShadow);
    
    /**
     * For nested regions, e.g. module regions or genes with exon/intron structures, this method will draw the connectors that group the children together
     * 
     * @param graphics
     * @param region
     * @param sequencedata
     * @param bounds
     * @param graphicsXoffset
     * @param direction
     * @param regioncolor
     * @param bordercolor
     * @param connectorType 
     */
    protected void drawConnectors(Graphics2D graphics, Region region, RegionSequenceData sequencedata, Rectangle bounds, int graphicsXoffset, int direction, Color regioncolor, Color bordercolor, String connectorType) {
       if (connectorType.equals("Region")) {
          // if (regioncolor!=null) regioncolor=regioncolor.brighter(); if (bordercolor!=null) bordercolor=bordercolor.brighter(); // use brighter versions of the colors
          drawRegion(graphics, region, sequencedata, bounds, Region.DIRECT, regioncolor, bordercolor, null, null, null, null, null, dropShadows);
       }
       else if (!connectorType.equals("None")) { // draw bounding box as default connector (unless the "None" type is chosen)
          if (dropShadows) {
              graphics.setColor(shadowColor);
              graphics.drawRect(bounds.x+shadowOffset, bounds.y+shadowOffset, bounds.width-1, bounds.height-1);
          }
          graphics.setColor(regioncolor);
          graphics.drawRect(bounds.x, bounds.y, bounds.width-1, bounds.height-1);
       }        
    }
    
    
  
    /* This method is used to draw a new region box when the user creates a new region with the DRAW tool. The implementation overrides the superclass to add gradient fill color */
    @Override
    protected void drawNewRegionBox(Graphics2D g, Region newregion, int x, int y, int width, int height) {
           Color featureColor=(settings.useMultiColoredRegions(featureName))?settings.getFeatureColor(newregion.getType()):settings.getForeGroundColor(featureName);
           Color useColor=new Color(featureColor.getRed(),featureColor.getGreen(),featureColor.getBlue(),160);
           int gradientfill=settings.useGradientFill(featureName);
           if (gradientfill>0) g.setPaint(getGradientPaint(useColor, x, y, width, height, gradientfill));    
           g.fillRect(x, y, width+1, height);                    
    }
   

    /**
     * Returns a Paint object that will paint in a gradient
     * @param color
     * @param x
     * @param y
     * @param width
     * @param height
     * @param type: 0=flat, 1=vertical gradient, 2=horizontal gradient, 3=radial gradient
     * @return 
     */
    protected java.awt.Paint getGradientPaint(Color color, int x, int y, int width, int height, int type) {
       int red=color.getRed();
       int blue=color.getBlue();
       int green=color.getGreen();
       red=(int)((255-red)*brightnessfactors[0])+red;
       green=(int)((255-green)*brightnessfactors[1])+green;
       blue=(int)((255-blue)*brightnessfactors[2])+blue;
       if (red<0) red=0; if (green<0) green=0; if (blue<0) blue=0;
       if (red>255) red=255; if (green>255) green=255; if (blue>255) blue=255;
       Color lightercolor=new Color(red,green,blue); 
            if (type==1) return new java.awt.GradientPaint(0, y, color, 0, y+((height-1)/2f), lightercolor, true);
       else if (type==2) return new java.awt.GradientPaint(x, 0, color, x+(width/2f), 0,  lightercolor, true); // this choice of horizontal "midpoint" (P2) might seem a bit strange. It is not optimal, but at least it gives same results irrespective of strand orientation          
       else if (type==3) { // radial gradient - EXPERIMENTAL!
           float[] dist = {0.1f, 0.9f};
           Color[] colors = {lightercolor,color};
           float radius=Math.max(width,height)/2f; // 
           return new java.awt.RadialGradientPaint(x+(width/2f), y+((height-1)/2f), radius, dist, colors);          
       } else if (type==4) return new java.awt.GradientPaint(0, y, lightercolor, 0, y+((height-1)/2f), color, true);
        return color; // default: return flat fill in the specified color
    }   
    
    
    /** Assigns each region in the whole sequence to a row for visualization */
    protected void packRegions() { 
       if (sequencedata==null) return;
       ArrayList<Region> regions = ((RegionSequenceData)sequencedata).getAllRegions();
       ArrayList<Integer> lastplacement=new ArrayList<Integer>(); // contains the last used position for each row. New rows can be added when necessary
       for (Region region:regions) {
           int start=region.getRelativeStart();
           int chosenrow=-1;
           for (int row=0;row<lastplacement.size();row++) {
               int lastpos=lastplacement.get(row);
               if (start>lastpos+regionspacing) {// available space on this row after the previous region (with added spacing)
                   chosenrow=row;
                   lastplacement.set(row, region.getRelativeEnd());
                   region.row=(short)chosenrow;
                   break;
               }
           }
           if (chosenrow<0) { // no available space in current rows. Add a new
               chosenrow=lastplacement.size();
               region.row=(short)chosenrow;
               lastplacement.add(region.getRelativeEnd());
           }
       }
       totalRows=lastplacement.size();
       // ((RegionSequenceData)sequencedata).totalRows=(short)lastplacement.size();
       valid=true;
    }    
   
    /* This overrides the super implementation to clear data in the tool tip*/
    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        currentRegion=null;        
        if (customTooltip!=null) customTooltip.clearData();
    }
    

    @Override
    public void mouseMoved(MouseEvent e) { // this handler updates tooltip based on position under mouse pointer
          super.mouseMoved(e);
          if (!isExpanded()) {currentRegion=null;return;} //  in contracted mode, the current region can be obtained with getCurrentRegion() which selects the first of potentially many overlapping regions
          if (sequencedata==null) return;
          if (SequenceVisualizerContextMenu.isPopupShowing()) {setToolTipText(null); return;} // Do not display tooltips on top of ContextMenus!
          else setToolTipText(""+Math.random()); // the Math.random call is made to change the tooltip text each time the mouse is moved. This is necessary to update the location and size of the tooltip as the mouse moves.
                                                 // the random tooltip text generated here is not used in any way, since the overridden paintComponent() method draws its own dynamic text
          int x=e.getPoint().x;
          if (x<=0 || x>settings.getSequenceWindowSize()) {currentRegion=null;return;}
          x=x-bordersize;
          int[] range=getGenomicCoordinateFromScreenOffset(x);
          int i=(int)((range[0]+range[1])/2); // use midpoint
          currentmouseposition=i;
          int row=getRowForY(e.getY());
          Region region =((RegionSequenceData)sequencedata).getRegionAtGenomicPositionAndRow(currentmouseposition,row);
          if (region==null) {currentRegion=null;return;}
          RegionVisualizationFilter filter=gui.getRegionVisualizationFilter();
          if (!settings.isRegionTypeVisible(region.getType()) || (filter!=null && !filter.shouldVisualizeRegion(region))) {
              currentRegion=null;
          } else {
              currentRegion=region;
          }
    }    
       
    
    @Override
    public JToolTip createToolTip() {
        if (isExpanded()) {
            if (expandedViewTooltip==null) {
                expandedViewTooltip=new ExpandedViewMotifTooltip(this, settings);
                expandedViewTooltip.setComponent(this);
            }
            return expandedViewTooltip;            
        } else {
            if (customTooltip==null) {
                customTooltip=new MotifTooltip(this,settings);
                customTooltip.setComponent(this);
            }
            return customTooltip;
        }
    }
    

    @Override
    public Region getCurrentRegion() {
        if (currentRegion!=null) {
            return currentRegion;
        }
        else {
            currentRegion=getFirstVisibleRegion(currentmouseposition);
            return currentRegion;
        }
    }  
    
    @Override
    public void setCurrentRegion(Region region) {
        currentRegion = region;
    }      
    
    /** Draws a glowing box around the region that the mouse currently points at to indicate that it has "focus" */
    protected void drawRegionHalo(Graphics2D graphics, Rectangle rectangle) { 
        graphics.setColor(glowColor); // add additional translucent glow
        graphics.drawRect(rectangle.x-1, rectangle.y-1, rectangle.width+1, rectangle.height+1);   
        graphics.drawRect(rectangle.x-1, rectangle.y-1, rectangle.width+1, rectangle.height+1); 
        graphics.drawRect(rectangle.x-1, rectangle.y-1, rectangle.width+1, rectangle.height+1); // repeated thrice to make it darker (color is translucent)
        graphics.drawRect(rectangle.x-2, rectangle.y-2, rectangle.width+3, rectangle.height+3);   
        graphics.drawRect(rectangle.x-2, rectangle.y-2, rectangle.width+3, rectangle.height+3); // repeated twice to make it darker (color is translucent)
        graphics.drawRect(rectangle.x-3, rectangle.y-3, rectangle.width+5, rectangle.height+5);          
     }    
    
    @Override
    public ArrayList<DataTrackVisualizerSetting> getGraphTypeSettings() {
        ArrayList<DataTrackVisualizerSetting> graphsettings = super.getGraphTypeSettings();  
        if (graphsettings==null) graphsettings=new ArrayList<>();     
        DataTrackVisualizerSetting connectors=new DataTrackVisualizerSetting("Connectors", VisualizationSettings.CONNECTOR_TYPE, DataTrackVisualizerSetting.MAJOR_SETTING, DataTrackVisualizerSetting.NESTED_TRACK|DataTrackVisualizerSetting.MODULE_TRACK, KeyEvent.VK_L); 
        connectors.addOption("Bounding Box");
        connectors.addOption("Region");        
        String[] otherConnectors=getConnectorTypes();
        if (otherConnectors!=null && otherConnectors.length>0) {
            for(String type:otherConnectors) connectors.addOption(type);
        }
        connectors.addOption("None");
              
        addGraphTypeSetting(graphsettings, borderSetting, false);       
        addGraphTypeSetting(graphsettings, connectors, false);    
        addGraphTypeSetting(graphsettings, showTypeLabelSetting, false);           
        addGraphTypeSetting(graphsettings, labelColorSetting, false);    
        addGraphTypeSetting(graphsettings, showMotifLogoSetting, false);             
        addGraphTypeSetting(graphsettings, scaleMotifLogoSetting, false);           
        addGraphTypeSetting(graphsettings, drawDirectionArrowsSetting, false);        
        addGraphTypeSetting(graphsettings, shadowSetting, false);  
        addGraphTypeSetting(graphsettings, renderUpsideDownSetting, false);          
        addGraphTypeSetting(graphsettings, cropSetting, false);        
        addGraphTypeSettingSeparator(graphsettings, DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.NESTED_OR_MODULE);
        addGraphTypeSetting(graphsettings, drawNestedSetting, false);          
        addGraphTypeSetting(graphsettings, filterNestedSetting, false);       
        addGraphTypeSetting(graphsettings, scaleConnectorsSetting, false); 
        return graphsettings;
    }
    
    static {
        // Some of these setting are configured to correspond with legacy settings        
        borderSetting=new DataTrackVisualizerSetting("Border", VisualizationSettings.DRAW_REGION_BORDERS, DataTrackVisualizerSetting.MAJOR_SETTING, DataTrackVisualizerSetting.ALL, KeyEvent.VK_B);
        borderSetting.addOption("None",0); 
        borderSetting.addOption("Darker Shade",1);
        borderSetting.addOption("Black",2);
             
        shadowSetting=new DataTrackVisualizerSetting("Draw Shadow", VisualizationSettings.DROPSHADOW, DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.ALL, KeyEvent.VK_W);
        shadowSetting.addOption("Off",false); 
        shadowSetting.addOption("On",true);    
        shadowSetting.setBooleanOption(true);       
        
        showMotifLogoSetting=new DataTrackVisualizerSetting("Draw Motif Logo", "showMotifLogo", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.MOTIF_OR_MODULE, 0);
        showMotifLogoSetting.addOption("On",true);   
        showMotifLogoSetting.addOption("Off",false);  
        showMotifLogoSetting.setBooleanOption(true); 
        
        scaleMotifLogoSetting=new DataTrackVisualizerSetting("Scale Motif Logo by IC", "scaleMotifLogoByIC", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.MOTIF_OR_MODULE, KeyEvent.VK_Y);
        scaleMotifLogoSetting.addOption("On",true);   
        scaleMotifLogoSetting.addOption("Off",false);  
        scaleMotifLogoSetting.setBooleanOption(true);      
        
        showTypeLabelSetting=new DataTrackVisualizerSetting("Draw Type Label", "showTypeLabel", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.ALL, 0);
        showTypeLabelSetting.addOption("On",true);   
        showTypeLabelSetting.addOption("Off",false);  
        showTypeLabelSetting.setBooleanOption(true);  
        
        drawDirectionArrowsSetting=new DataTrackVisualizerSetting("Indicate orientation", "drawDirectionArrows", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.ALL, 0); 
        drawDirectionArrowsSetting.addOption("On",true);   
        drawDirectionArrowsSetting.addOption("Off",false);  
        drawDirectionArrowsSetting.setBooleanOption(true);     
        drawDirectionArrowsSetting.setDocumentationString("Draw indicators (e.g. small arrows) to show region orientation. These may not always be visible");         
        
        renderUpsideDownSetting=new DataTrackVisualizerSetting("Draw Upside Down", VisualizationSettings.RENDER_UPSIDE_DOWN, DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.ALL, 0); // this has no key-event but is handled as a special case in FeaturesPanel
        renderUpsideDownSetting.addOption("On",true);   
        renderUpsideDownSetting.addOption("Off",false);  
        renderUpsideDownSetting.setBooleanOption(true); 
        renderUpsideDownSetting.setDocumentationString("Draws connectors upside down. In contracted mode, the regions themselves will also be aligned at the top rather than bottom when 'show orientation' is disabled");         
        
        labelColorSetting=new DataTrackVisualizerSetting("Label Color", "typeLabelColor", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.ALL, 0); 
        labelColorSetting.addOption("Dynamic",0);   
        labelColorSetting.addOption("Black",1);     
        labelColorSetting.addOption("White",2);             
        
        drawNestedSetting=new DataTrackVisualizerSetting("Draw Nested Regions", VisualizationSettings.VISUALIZE_NESTED_REGIONS, DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.NESTED_OR_MODULE, KeyEvent.VK_N); 
        drawNestedSetting.addOption("On",true);   
        drawNestedSetting.addOption("Off",false);  
        drawNestedSetting.setBooleanOption(true);   
        drawNestedSetting.setDocumentationString("Draw nested regions, such as individual TFBS within a module region");        
        
        cropSetting=new DataTrackVisualizerSetting("Crop At Sequence Edges", "cropAtSequenceEdges", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.ALL, 0); 
        cropSetting.addOption("On",true);   
        cropSetting.addOption("Off",false);  
        cropSetting.setBooleanOption(true); 
        cropSetting.setDocumentationString("Crop regions that fall partially outside of the sequence itself");          
        
        scaleConnectorsSetting=new DataTrackVisualizerSetting("Scale Connectors", "scaleConnectors", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.NESTED_OR_MODULE, 0); 
        scaleConnectorsSetting.addOption("Off",false);   
        scaleConnectorsSetting.addOption("On",true);  
        scaleConnectorsSetting.setBooleanOption(true);  
        scaleConnectorsSetting.setDocumentationString("Scale the height of arched connectors depending on their width");        
        
        filterNestedSetting=new DataTrackVisualizerSetting("Filter Nested Regions", "filterNestedRegions", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.NESTED_OR_MODULE, 0); 
        filterNestedSetting.addOption("Off",false);   
        filterNestedSetting.addOption("On",true);  
        filterNestedSetting.setBooleanOption(true);  
        filterNestedSetting.setDocumentationString("Apply dynamic region visualization filters also to nested regions (e.g. individual TFBS inside modules)");
    }
    

}