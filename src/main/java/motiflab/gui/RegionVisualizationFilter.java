package motiflab.gui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import motiflab.engine.data.Region;

/**
 *  RegionVisualizationFilters can be used to dynamically decide visualization
 *  properties of each Region separately based on various settings and properties
 *  determined by each implementing filter
 * @author kjetikl
 */
public interface RegionVisualizationFilter {

    public static final int FILTER_PRIORITY_HIGH=100;
    public static final int FILTER_PRIORITY_MIDDLE=50;
    public static final int FILTER_PRIORITY_LOW=0;
           
    
    /**
     * Returns a display name for the filter
     * @return 
     */
    public String getRegionVisualizationFilterName();
        
    
    /** Determines whether the given region should be shown or hidden
     *  Note that this method is always called before the other methods that return dynamic colors for the same region,
     *  so the filter is free to perform all the processing in this method and cache them to be returned by the other
     *  methods when the same region is queried
     *  @param region
     */
    public boolean shouldVisualizeRegion(Region region);

    /** Returns a color to use for this region
     *  @param region
     *  @return The color to use for this specific region, or NULL if a default (non-dynamic) color should be used
     */
    public java.awt.Color getDynamicRegionColor(Region region);
    
    /** Returns a color to use for the label of this region
     *  @param region
     *  @return The label color to use for this specific region, or NULL if a default (non-dynamic) color should be used
     */
    public java.awt.Color getDynamicRegionLabelColor(Region region);
    
    /** Returns a color to use for the border of this region
     *  @param region
     *  @return The border color to use for this specific region, or NULL if a default (non-dynamic) color should be used
     */
    public java.awt.Color getDynamicRegionBorderColor(Region region);    
    
    /** Returns colors to use for bases in motif match logos
     *  @param region
     *  @return The colors to use for motif logos for this specific region, 
     *  or NULL if a default (non-dynamic) colors should be used.
     *  The return array should contain 8 color entries
     *  The first four colors are the normal colors for A, C, G, T respectively.
     *  and the last four should be the colors for non-matched letters (A,C,G,T)
     */
    public java.awt.Color[] getDynamicMotifLogoColors(Region region);     
    
    
    /**
     * This method should return TRUE if the filter will draw overlays with the drawOverlay method
     * @return 
     */
    public boolean drawsOverlay();    
    
    /**
     * This method can be overridden to draw on top of the visualized Region.
     * Note that the drawsOverlay() method must return TRUE or the overlay is not drawn at all
     * @param region The region to be drawn
     * @param g The Graphics context to draw into
     * @param rect The bounding rectangle of the region
     * @param settings A reference to the visualization settings
     * @param visualizer A reference to the DataTrackVisualizer responsible for drawing this track
     */
    public void drawOverlay(Region region, Graphics2D g, Rectangle rect, VisualizationSettings settings, DataTrackVisualizer visualizer);    
    
    
    /**
     * This method should return the names of the feature tracks that this filter applies to.
     * A return value of NULL means that the filter should apply to all tracks. 
     * An empty list means it applies to no tracks
     * @return 
     */
    public String[] getTargets();
    
    
    /**
     * This method should return TRUE if the filter applies to the feature track with the given name
     */    
    public boolean appliesToTrack(String featureName);

    /**
     * This method is called once every time a SequenceVisualizer is invoked to redraw a particular track for a sequence in the Visualization Panel
     * It can be overridden to respond to this event
     */
    public void sequenceRepainted(String sequenceName, String featureName);    
    
    
    /**
     * Returns a suggested priority for this filter which applies when multiple filters are added to the same filter group
     * Filters with higher priority values will be processed before those with lower values. 
     * This means that higher priority filters are more likely to have their preferred colors chosen.
     * Overlays of higher priority filters are also drawn before lower priority filters. Hence, lower priority overlays will be drawn on top of higher priorities.
     * The constants FILTER_PRIORITY_HIGH/MIDDLE/LOW are defined in this class for this use
     * @return 
     */
    public int getPriority();
    
    /** This is a callback method called by the GUI when the
     *  GUI wants to remove the filter. Filters should use this method
     *  to perform cleanup and perhaps close any open dialogs they
     *  are using
     */
    public void shutdown();
}
