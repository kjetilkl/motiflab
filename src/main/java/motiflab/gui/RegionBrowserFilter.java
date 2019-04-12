/*
 */
package motiflab.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;

/**
 * This RVF filter is used by the Region Browser tool to dynamically hide 
 * (or gray out) regions not matching the selected filter criterion
 * @author kjetikl
 */
 public class RegionBrowserFilter implements RegionVisualizationFilter {
        private Color VERY_LIGHT_GRAY=new Color(225,225,225);
        private Color LIGHTER_GRAY=new Color(212,212,212);    
        private Color[] filteredMotifColors=new Color[]{LIGHTER_GRAY,LIGHTER_GRAY,LIGHTER_GRAY,LIGHTER_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY};

        private HashSet<Region> targetRegions=new HashSet<>();
        private boolean active=false;
        private RegionDataset track=null;
        private boolean hideOtherRegions;
        private ArrayList<PropertyChangeListener> listeners=null;
        private String[] noTarget=new String[0];
        private String[] singleTarget=new String[1];
        
        @Override
        public String getRegionVisualizationFilterName() {
            return "Region Browser";
        }
        
        public HashSet<Region> getTargetRegions() {
            return targetRegions;
        }
        
        public void setTrack(RegionDataset regiontrack) {
            clearAll();
            this.track=regiontrack;
            singleTarget[0]=this.track.getName();
        }        
        
        public void clearAll() {
            targetRegions.clear();
        }
        
        public void addRegion(Region region) {
            if (track==null) track=(RegionDataset)region.getParent().getParent();
            targetRegions.add(region);
        }  
        
        public void removeRegion(Region region) {
            if (track==null) track=(RegionDataset)region.getParent().getParent();
            targetRegions.remove(region);
        }    
        
        public boolean containsRegion(Region region) {
            return (targetRegions!=null && targetRegions.contains(region));
        }             
                        
        public void addListener(PropertyChangeListener listener) {
            if (listeners==null) listeners=new ArrayList<>();
            listeners.add(listener);
        }
        
        public void removeListener(PropertyChangeListener listener) {
            if (listeners!=null) listeners.remove(listener);
        }   
        
        public void fireFilterEvent(PropertyChangeEvent e) {
            if (listeners==null || listeners.isEmpty()) return;
            for (PropertyChangeListener listener:listeners) {
                listener.propertyChange(e);
            }
        }          
        
        
        public void enable() {active=true;}
        public void disable() {active=false;}
        public void setHideOtherRegions(boolean hideOtherRegions) {this.hideOtherRegions=hideOtherRegions;}
        
        @Override
        public boolean shouldVisualizeRegion(Region region) {
            if (!active || region.getParent().getParent()!=track) return true;
            if (hideOtherRegions) return (targetRegions.contains(region))?true:false;
            return true;
        }
  
        @Override
        public java.awt.Color getDynamicRegionColor(Region region) {
            if (!active || region.getParent().getParent()!=track) return null;
            return (targetRegions.contains(region))?null:VERY_LIGHT_GRAY;
        }    

        @Override
        public java.awt.Color getDynamicRegionLabelColor(Region region) {
            if (!active || region.getParent().getParent()!=track) return null;            
            return (targetRegions.contains(region))?null:Color.WHITE;
        }   
        
        @Override
        public java.awt.Color getDynamicRegionBorderColor(Region region) {
            if (!active || region.getParent().getParent()!=track) return null;
            return (targetRegions.contains(region))?null:LIGHTER_GRAY;
        }         

        @Override
        public java.awt.Color[] getDynamicMotifLogoColors(Region region) {
            if (!active || region.getParent().getParent()!=track) return null;            
            return (targetRegions.contains(region))?null:filteredMotifColors;
        }

        @Override
        public void shutdown() {

        }

        @Override
        public boolean appliesToTrack(String featureName) {
            if (track==null) return false;
            else return track.getName().equals(featureName);
        }

        @Override
        public void drawOverlay(Region region, Graphics2D g, Rectangle rect, VisualizationSettings settings, DataTrackVisualizer visualizer) {
            
        }

        @Override
        public boolean drawsOverlay() {
            return false;
        }

        public int getPriority() { // filter priority
            return FILTER_PRIORITY_HIGH;
        }

        @Override
        public String[] getTargets() {
            return (track==null)?noTarget:singleTarget;
        }


        @Override
        public void sequenceRepainted(String sequenceName, String featureName) {
            
        }
        
        
        
   } 