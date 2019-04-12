package motiflab.gui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import motiflab.engine.data.Region;

/**
 *  This class represents a single filter interface to a group of underlying RegionVisualizationFilters
 * @author kjetikl
 */
public class RegionVisualizationFilterGroup implements RegionVisualizationFilter {

    private ArrayList<RegionVisualizationFilter> filters; // this should not never be null after initialization
    private int priority=FILTER_PRIORITY_HIGH; // default is to add new filters to the end of the list. So also for groups such as this
    private String name;
    
    
    public RegionVisualizationFilterGroup(String groupname) {
        this.name=groupname;
        this.filters=new ArrayList<RegionVisualizationFilter>();
    }     
    
    @Override
    public String getRegionVisualizationFilterName() {
        return name;
    }
    
    /** Adds the given filter to the group (unless it is already in the group).
     *  New filters with higher priority are added before existing filters with lower (or same) priority and will thus be processed before those.
     *  Note: for synchronization reasons, this method should only be called on the EDT to avoid problems with other objects that might be iterating over the filter (on the EDT)
     * @param filter
     * @return The number of filters in this group after (possibly) adding the new filter
     */
    public int addFilter(RegionVisualizationFilter filter) {
        if (!filters.contains(filter)) {
            int preferredPriority=filter.getPriority();
            int index=0;
            for (index=0;index<filters.size();index++) {
                if (filters.get(index).getPriority()<=preferredPriority) break; // if other filter has lower priority => break and insert before  
            }
            filters.add(index,filter);
        }          
        return filters.size();
    }
    
    /** Adds the given filter to the end of the this group (unless it is already in the group).
     * @param filter
     */
    public void addFilterToEnd(RegionVisualizationFilter filter) {
        if (!filters.contains(filter)) filters.add(filter);
    }    
        
    /** Removes the given filter from the group and returns      
     *  Note: for synchronization reasons, this method should only be called on the EDT to avoid problems with other objects that might be iterating over the filter (on the EDT)
     * @param filter
     * @return The number of filters left in the group 
     */
    public int removeFilter(RegionVisualizationFilter filter) {        
        filters.remove(filter);  
        return filters.size();
    }    
    
    public int size() {
        return filters.size();
    }
    
    public boolean isEmpty() {
        return filters.isEmpty();
    }
    
    /** Returns TRUE if this group contains the given filter 
     *  (Note that subgroups are not searched recursively)
     */
    public boolean containsFilter(RegionVisualizationFilter filter) {
        return filters.contains(filter);
    }
    
    /** Returns a list containing the filters in this group (not a clone!)*/
    public ArrayList<RegionVisualizationFilter> getFilters() {
        return filters;
    }
    
    /** Returns the first filter in this group (or NULL if the group is empty) */
    public RegionVisualizationFilter getFirstFilter() {
        if (filters==null || filters.isEmpty()) return null;
        else return filters.get(0);
    }
      
    @Override
    public boolean shouldVisualizeRegion(Region region) {
        for (RegionVisualizationFilter filter:filters) {
            if (!filter.shouldVisualizeRegion(region)) return false;
        }
        return true;
    }

    @Override
    public java.awt.Color getDynamicRegionColor(Region region) {
        for (RegionVisualizationFilter filter:filters) {
            java.awt.Color color=filter.getDynamicRegionColor(region);
            if (color!=null) return color;
        }
        return null;        
    }
     
    
    @Override
    public java.awt.Color getDynamicRegionLabelColor(Region region){
        for (RegionVisualizationFilter filter:filters) {
            java.awt.Color color=filter.getDynamicRegionLabelColor(region);
            if (color!=null) return color;
        }
        return null;        
    }
    
    @Override
    public java.awt.Color getDynamicRegionBorderColor(Region region){
        for (RegionVisualizationFilter filter:filters) {
            java.awt.Color color=filter.getDynamicRegionBorderColor(region);
            if (color!=null) return color;
        }
        return null;        
    }    
    
    @Override
    public java.awt.Color[] getDynamicMotifLogoColors(Region region) {
        for (RegionVisualizationFilter filter:filters) {
            java.awt.Color[] color=filter.getDynamicMotifLogoColors(region);
            if (color!=null) return color;
        }
        return null;        
    }    

    @Override    
    public int getPriority() {
        return priority;
    }
    
    /** Can be used to set the preferred priority of this group */
    public void setPriority(int newpriority) {
        priority=newpriority;
    }
    
    @Override
    public String[] getTargets() {
        if (filters==null || filters.isEmpty()) return new String[0]; // applies to no tracks
        ArrayList<String> list=new ArrayList<>();
        for (RegionVisualizationFilter filter:filters) {
            String[] otherlist=filter.getTargets();
            if (otherlist==null) return null; // group should apply to all tracks since one of the filters does
            for (String track:otherlist) {
                if (!list.contains(track)) list.add(track);
            }
        } 
        String[] arraylist=new String[list.size()];
        return list.toArray(arraylist);
    }

    
    @Override
    public boolean appliesToTrack(String featureName) { // returns TRUE if at least one of the filters in the group applies
        if (filters==null || filters.isEmpty()) return false;
        for (RegionVisualizationFilter filter:filters) {
            if (filter.appliesToTrack(featureName)) return true;
        } 
        return false;
    }    

    /**
     * Returns a flattened RegionVisualizationFilterGroup with all filters (and subfilters) from this group that apply to the given feature track (or null if none apply)
     * @param featureName
     * @return 
     */
    public RegionVisualizationFilterGroup getApplicableFilters(String featureName) {
        if (filters==null || filters.isEmpty()) return null;
        RegionVisualizationFilterGroup newgroup=new RegionVisualizationFilterGroup(null);
        for (RegionVisualizationFilter filter:filters) {
            if (filter instanceof RegionVisualizationFilterGroup) {
                ((RegionVisualizationFilterGroup)filter).addApplicableFilters(newgroup,featureName);
            } else if (filter.appliesToTrack(featureName)) newgroup.addFilterToEnd(filter);
        } 
        return (newgroup.isEmpty())?null:newgroup;
    }
    
    private void addApplicableFilters(RegionVisualizationFilterGroup group, String featureName) {
        if (filters==null || filters.isEmpty()) return; // no new additions
        for (RegionVisualizationFilter filter:filters) {
            if (filter instanceof RegionVisualizationFilterGroup) {
                ((RegionVisualizationFilterGroup)filter).addApplicableFilters(group,featureName);
            } else if (filter.appliesToTrack(featureName)) group.addFilterToEnd(filter);
        } 
    }    
    
    
    
    public void sequenceRepainted(String sequenceName, String featureName) {
        if (filters==null || filters.isEmpty()) return;
        for (RegionVisualizationFilter filter:filters) {
            filter.sequenceRepainted(sequenceName, featureName);
        }       
    }      


    @Override
    public void drawOverlay(Region region, Graphics2D g, Rectangle rect, VisualizationSettings settings, DataTrackVisualizer visualizer) {
        if (filters==null || filters.isEmpty()) return;
        for (RegionVisualizationFilter filter:filters) {
            filter.drawOverlay(region, g, rect, settings, visualizer);
        }  
    }
    
    @Override
    public boolean drawsOverlay() {
        if (filters==null || filters.isEmpty()) return false;
        for (RegionVisualizationFilter filter:filters) {
            if (filter.drawsOverlay()) return true;
        }          
        return false;
    }
          
    /**
     * Returns true if any of the filters are of the given type
     * Filter groups are processed recursively
     * @return 
     */    
    public boolean hasFiltersOfType(Class type) {
        if (filters==null || filters.isEmpty()) return false;
        for (RegionVisualizationFilter filter:filters) {
            if (type.isAssignableFrom(filter.getClass())) return true;            
            if (filter instanceof RegionVisualizationFilterGroup && ((RegionVisualizationFilterGroup)filter).hasFiltersOfType(type)) return true;
        }
        return false;
    }
    
    @Override
    public void shutdown() { }    
    
    
}
