package motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import motiflab.engine.data.Data;
import motiflab.engine.data.Module;
import motiflab.engine.data.Motif;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Region;

/**
 * This is the abstract superclass for DataTrackVisualizers used to render Region Dataset tracks
 * @author kjetikl
 */
public abstract class DataTrackVisualizer_Region extends DataTrackVisualizer {
    
    protected Region lastSelectedRegion=null;
    
    /** Constructs a new DefaultDataTrackVisualizer */
    public DataTrackVisualizer_Region() {
        super();     
    }

    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);   
        if (sequencedata!=null && !(sequencedata instanceof RegionSequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Region is used to render data of type:"+sequencedata.getClass().toString());}    
    }        
    
    /**
     * Returns TRUE if this track should be drawn in "expanded" modes where overlapping regions are drawn beneath each other rather than on top of each other
     * @return 
     */
    public boolean isExpanded() {
        return settings.isExpanded(featureName);
    }
   
    @Override
    public boolean hasVariableHeight() {
       return settings.isExpanded(featureName);
    }    
     
    @Override
    public void describeBaseForTooltip(int genomicCoordinate, int shownOrientation, StringBuilder buffer) {
        if (sequencedata==null) return;
        if (((RegionSequenceData)sequencedata).isMotifTrack()) return;  // this case is handled by a special non-default tooltip class, so there is no need to update the default tooltip
        if (((RegionSequenceData)sequencedata).isModuleTrack()) return; // this case is handled by a special non-default tooltip class, so there is no need to update the default tooltip    
        Object value=sequencedata.getValueAtGenomicPosition(genomicCoordinate);    
        if (value instanceof ArrayList) {
              ArrayList list=(ArrayList)value;
              int size=list.size();
              int hidden=0;
              for (Object region:list) {
                  String type=((Region)region).getType();
                  if (!settings.isRegionTypeVisible(type)) hidden++;
              }
              size-=hidden; // number of visible regions at this position
              if (size==0) {
                  //builder.append(featureName);
                  return;
              }  
              boolean colorByType=settings.useMultiColoredRegions(featureName);
              int usesize=Math.min(size,15); // only display first 15 regions at this position
              int i=0, included=0;
              String separator=", "; // separator=(size==1)?"<br>":", ";
              while (included<usesize) {
                  Region region=(Region)list.get(i);
                  String type=region.getType();
                  if (settings.isRegionTypeVisible(type)) {
                      included++;                  
                      String orientation=".";
                      int ro=region.getOrientation();
                      if (ro==Region.DIRECT) orientation="+";
                      else if (ro==Region.REVERSE) orientation="&ndash;";
                      if (colorByType) {
                          Color color=settings.getFeatureColor(region.getType());
                          buffer.append("<font color=\"");
                          buffer.append(VisualizationSettings.convertColorToHTMLrepresentation(color));
                          buffer.append("\">&#9632;</font> ");
                      }
                      buffer.append(type);
                      buffer.append("&nbsp;&nbsp;[");
                      buffer.append(orientation);
                      buffer.append("]&nbsp;&nbsp;");
                      buffer.append(region.getLength());
                      buffer.append(" bp");
                      buffer.append(",  score=");
                      buffer.append(MotifLabGUI.formatNumber(region.getScore()));
                      String otherProperties=region.getPropertiesAsString(separator,60,false);
                      if (otherProperties!=null && !otherProperties.isEmpty()) {
                          buffer.append(separator);
                          buffer.append(otherProperties);
                      }
                      if (i<usesize-1) buffer.append("<br>");
                  }
                  i++;
              }
              if (usesize<size) {
                  buffer.append("<br><br>+");
                  buffer.append((size-usesize));
                  buffer.append(" more...");
              }
          } else if (value==null) {
              if (genomicCoordinate>=sequenceStart && genomicCoordinate<=sequenceEnd) buffer.append("NULL!");
              else buffer.append("Outside sequence:  "+sequenceStart+"-"+sequenceEnd+"  ("+(sequenceEnd-sequenceStart+1)+" bp)");            
        }        
    }    

    /*
     * This default implementation draws a solid color background corresponding to the visible sequence segment
     * It will always draw at 1:1 scale 
     */
    @Override    
    public void drawVisibleSegmentBackground(Graphics2D graphics, int start, int height, int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {      
        Color background=settings.getBackGroundColor(featureName);
        if (background==null) return;
        graphics.setColor(background);        
        AffineTransform saveTransform=graphics.getTransform();
        graphics.setTransform(saveAt); // restores 'original state' (saveAt is set in superclass)
        int vizStart=settings.getSequenceViewPortStart(sequenceName);
        int vizEnd=settings.getSequenceViewPortEnd(sequenceName);      
        if (vizStart<sequenceStart) vizStart=sequenceStart;
        if (vizEnd>sequenceEnd) vizEnd=sequenceEnd;               
        int[] screenrange = getScreenCoordinateRangeFromGenomic(vizStart,vizEnd);
        width=screenrange[1]-screenrange[0]+1;            
        graphics.fillRect(graphicsXoffset+screenrange[0], graphicsYoffset, width, height);                                
        graphics.setTransform(saveTransform);   
    } 
    

   
   @Override
   public void drawEditOverlay(Graphics2D g, int start, int height, int width, int bases, int xoffset, int yoffset, int orientation) {
       if (newregion!=null) {// draw new region (if DRAW tool is in use)
           int[] bounds=getScreenCoordinateRangeFromGenomic(newregion.getGenomicStart(), newregion.getGenomicEnd(), xoffset);
           drawNewRegionBox(g, newregion, bounds[0], yoffset, bounds[1]-bounds[0], height);
       }
    } // end paintComponent
   
   /**
    * Draws a region box with the given size at the given coordinates at scale=1.0 (1:1 ratio between graphics buffer coordinates and pixels)
    * This method is used by the "draw" tool to draw a new region. The default implementation in the superclass 
    * draws a simple, slightly transparent filled rectangle in the foreground color
    */
    protected void drawNewRegionBox(Graphics2D g, Region newregion, int x, int y, int width, int height) {
           Color featureColor=settings.getForeGroundColor(featureName);
           Color useColor=new Color(featureColor.getRed(),featureColor.getGreen(),featureColor.getBlue(),160);
           g.setColor(useColor);   
           g.fillRect(x, y, width+1, height);                     
    }
   

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) gui.statusMessage("");
    }
       
    @Override
    public void mouseClicked(MouseEvent e) {      
       if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.getClickCount()<2 || sequencedata==null || !gui.getSelectedTool().equals(MotifLabGUI.SELECTION_TOOL)) return;
       Region selected=getCurrentRegion();
       lastSelectedRegion=selected;
       if (!(e.isShiftDown() || e.isControlDown() || e.getButton()==MouseEvent.BUTTON2)) {
           if (selected!=null) editRegionProperties(selected);
           return;
       }
       if (!(((RegionSequenceData)sequencedata).isMotifTrack() || ((RegionSequenceData)sequencedata).isModuleTrack())) return;
       gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
       int x=e.getPoint().x;
       x=x-bordersize;
       int[] range=getGenomicCoordinateFromScreenOffset(x);
       final ArrayList<Region> regions=getVisibleRegions((range[0]+range[1])/2);
       if (regions.isEmpty()) {
           gui.getFrame().setCursor(Cursor.getDefaultCursor());
           return;
       }
       if (regions.size()==1) {
              Region region=regions.get(0);
              String type=region.getType();
              Data motifdata=gui.getEngine().getDataItem(type);
              if (motifdata!=null && (motifdata instanceof Motif || motifdata instanceof Module)) showPrompt(motifdata);
       } else {
           final JPopupMenu menu=new JPopupMenu();
           ActionListener menulistener=new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    Object obj=ev.getSource();
                    for (int i=0;i<menu.getComponentCount();i++) {
                        if (obj==menu.getComponent(i)) {
                            Region region=regions.get(i);
                            String type=region.getType();
                            Data data=gui.getEngine().getDataItem(type);
                            if (data!=null) showPrompt(data);
                        }
                    }
                }
            };
           for (Region region:regions) {
              String type=region.getType();
              Data motifdata=gui.getEngine().getDataItem(type);
              if (motifdata!=null && (motifdata instanceof Motif || motifdata instanceof Module)) {
                  String command=null;
                  if (motifdata instanceof Motif) command=((Motif)motifdata).getPresentationName();
                  else if (motifdata instanceof Module) command=((Module)motifdata).getName();
                  if (menuContainsItem(menu, command)) continue;
                  JMenuItem menuitem=new JMenuItem(command);
                  menuitem.addActionListener(menulistener);
                  menu.add(menuitem);
              }              
           }
           if (menu.getComponentCount()==1) { // The multiple overlapping motifs were all of the same type so just show the prompt
                Region region=regions.get(0);
                String type=region.getType();
                Data data=gui.getEngine().getDataItem(type);
                if (data!=null) showPrompt(data);
           } else {
               gui.getFrame().setCursor(Cursor.getDefaultCursor());
               menu.show(e.getComponent(), e.getX(),e.getY());
           }
       }
    }
    
    
    private boolean menuContainsItem(JPopupMenu menu, String text) {
        for (int i=0;i<menu.getComponentCount();i++) {
            Component item=menu.getComponent(i);
            if (item instanceof JMenuItem && ((JMenuItem)item).getActionCommand().equals(text)) return true;
        }
        return false;
    }

    protected void showPrompt(Data data) {
        gui.getMotifsPanel().showPrompt(data, false, false); // showPrompt in MotifsPanel will show different prompt depending on whether the Data object is a Motif or a Module
    }


    /** Returns TRUE if this DataTrackVisualizer is coupled to a motif datatrack */
    public boolean isMotifTrack() {
        if (sequencedata==null || !(sequencedata instanceof RegionSequenceData)) return false;
        return ((RegionSequenceData)sequencedata).isMotifTrack();
    }

    /** Returns TRUE if this DataTrackVisualizer is coupled to a module datatrack */
    public boolean isModuleTrack() {
        if (sequencedata==null || !(sequencedata instanceof RegionSequenceData)) return false;
        return ((RegionSequenceData)sequencedata).isModuleTrack();
    }
    
    /** Returns TRUE if this DataTrackVisualizer is coupled to a datatrack possibly containing nested regions (that are not necessarily modules) */
    public boolean isNestedTrack() {
        if (sequencedata==null || !(sequencedata instanceof RegionSequenceData)) return false;
        return ((RegionSequenceData)sequencedata).isNestedTrack();
    }    


    /** 
     * Returns a list of the currently visible regions (as determined by visibility status and dynamic filters)
     * that overlap with the given genomic position
     */
    public ArrayList<Region> getVisibleRegions(int genomicpos) {
       ArrayList<Region> regions=((RegionSequenceData)sequencedata).getValueAtGenomicPosition(genomicpos);
       if (regions==null) return new ArrayList<Region>();
       if (regions.isEmpty()) return regions;
       RegionVisualizationFilter regionfilter=settings.getGUI().getRegionVisualizationFilter();           
       Iterator<Region> iter=regions.iterator();
       while (iter.hasNext()) {
           Region region=iter.next();
           String type=region.getType();
           if (!settings.isRegionTypeVisible(type)) iter.remove();
           if (regionfilter!=null && !regionfilter.shouldVisualizeRegion(region)) iter.remove();
       }
       return regions;
    }
    
    /** 
     * Returns a the first visible region (as determined by visibility status and dynamic filters)
     * that overlap with the given genomic position. If more than one overlaps the position,
     * an arbitrary region will be returned (albeit the same each time).
     */
    public Region getFirstVisibleRegion(int genomicpos) {
       ArrayList<Region> regions=((RegionSequenceData)sequencedata).getValueAtGenomicPosition(genomicpos);
       if (regions==null || regions.isEmpty()) return null;
       RegionVisualizationFilter regionfilter=settings.getGUI().getRegionVisualizationFilter();           
       for (Region region:regions) {
           String type=region.getType();
           if (!settings.isRegionTypeVisible(type)) continue;
           if (regionfilter!=null && !regionfilter.shouldVisualizeRegion(region)) continue;
           return region;
       }
       return null;
    }    
    
    /** Returns the region currently pointed at by the mouse (or null)
     *  If there are multiple regions overlapping at this point the "first" one will be returned
     */
    public Region getCurrentRegion() {
        return getFirstVisibleRegion(currentmouseposition);
    }     

    /** Shows a dialog to edit the properties of a region and commits the changes
     *  if the OK button is pressed and the properties have been changes
     */
    protected void editRegionProperties(Region oldregion) {
        boolean updateregion=true;
        Region regionclone=oldregion.clone();
        EditRegionPropertiesDialog propertiesdialog=new EditRegionPropertiesDialog(regionclone, gui);
        propertiesdialog.setLocation(gui.getFrame().getWidth()/2-propertiesdialog.getWidth()/2, gui.getFrame().getHeight()/2-propertiesdialog.getHeight()/2);
        propertiesdialog.setVisible(true);
        if (!propertiesdialog.okPressed()) updateregion=false;
        propertiesdialog.dispose();
        if (updateregion && (oldregion!=null && oldregion.isIdenticalTo(regionclone))) updateregion=false; // no changes have been made
        if (updateregion) {
            gui.updatePartialDataItem(featureName, sequenceName, oldregion, regionclone); // update the registered dataset with the new data from the buffer (this will update the whole dataset not just the single sequence).
        }
    }
    
    

 //    ---------------   Functionality for the "Draw tool"   ------------------------------

    protected int anchor1=-1;   // position of first placed mark! (genomic coordinate)
    protected int anchor2=-1;   // position of second placed mark! (genomic coordinate)
    protected int currentPosition=0;
    protected int firstAnchorPos=0; // screen coordinate of first placed mark
    protected Region newregion=null;


    @Override
    public void mousePressed(MouseEvent e) {
       if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return; 
       if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
             int x=e.getX()-bordersize;
             setFirstAnchor(x);
             setSecondAnchor(x);
             updateRegion();
       }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
       if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return;    
       if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
            // commit changes to engine. using the gui's "updateDataItem" item
            gui.statusMessage("");
            boolean addregion=true;
            if (e.isShiftDown() || e.isControlDown()) { // edit properties of new region
                 EditRegionPropertiesDialog propertiesdialog=new EditRegionPropertiesDialog(newregion, gui);
                 propertiesdialog.setLocation(gui.getFrame().getWidth()/2-propertiesdialog.getWidth()/2, gui.getFrame().getHeight()/2-propertiesdialog.getHeight()/2);
                 propertiesdialog.setVisible(true);
                 if (!propertiesdialog.okPressed()) addregion=false;
                 propertiesdialog.dispose();
            }
            if (addregion) {
                gui.updatePartialDataItem(featureName, sequenceName, null, newregion); // 
            }
            newregion=null;
            settings.getGUI().getVisualizationPanel().getSequenceVisualizer(sequenceName).requestFocusInWindow();
       }
    }

    @Override
    public void mouseDragged(MouseEvent e) { 
        if (gui.getVisualizationPanel().isMouseEventsBlocked() || SwingUtilities.isRightMouseButton(e)) return;    
        if (!gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) return;
        int x=e.getX();
        setCurrentPosition(x);
        setSecondAnchor(x);
        updateRegion();
    }


    /** Sets the first anchor to a genomic coordinate based on the relative position within the window given by the argument
     * (specifically, if single screen pixel represents multiple sequence bases, the first anchor should be set to the first (leftmost) of these)
     * However, note that the first anchor might change later depending on the position of the second anchor relative to the first (when 1 pixel represents multiple bases)
     * @param pos Position of anchor within the window (first position within window is pos=0)
     */
    private void setFirstAnchor(int pos) {
        int largest=settings.getSequenceWindowSize();
        if (pos<0) pos=0;
        if (pos>=largest) pos=largest;
        firstAnchorPos=pos;
        anchor1=getGenomicCoordinateFromScreenOffset(pos)[0]; // this might change later depending on relative position of anchor2
    }


    private void setSecondAnchor(int pos) {
        int largest=settings.getSequenceWindowSize()-1;
        if (pos<0) pos=0;
        if (pos>=largest) pos=largest;
        int[] firstAnchorGenomic=getGenomicCoordinateFromScreenOffset(firstAnchorPos);
        int[] currentPosGenomic=getGenomicCoordinateFromScreenOffset(pos);
        if (firstAnchorGenomic[0]<=currentPosGenomic[0]) {
            anchor1=firstAnchorGenomic[0];
            anchor2=currentPosGenomic[1];
        } else {
            anchor1=firstAnchorGenomic[1];
            anchor2=currentPosGenomic[0];
        }
        int VPstart=settings.getSequenceViewPortStart(sequenceName);
        int VPend=settings.getSequenceViewPortEnd(sequenceName);
        if (anchor1<VPstart) anchor1=VPstart;  // just in case
        else if (anchor1>VPend) anchor1=VPend; // just in case
        if (anchor2<VPstart) anchor2=VPstart;  // just in case
        else if (anchor2>VPend) anchor2=VPend; // just in case
    }

    /**
     * Updates the current (genomic) position as the mouse is moved/dragged
     * @param pos (relative position within sequence window!)
     */
    private void setCurrentPosition(int pos) {
        int largest=settings.getSequenceWindowSize()-1;
        if (pos<0) pos=0;
        if (pos>=largest) pos=largest;
        int[] firstAnchorGenomic=getGenomicCoordinateFromScreenOffset(firstAnchorPos);
        int[] currentPosGenomic=getGenomicCoordinateFromScreenOffset(pos);
        if (firstAnchorGenomic[0]<=currentPosGenomic[0]) {
            anchor1=firstAnchorGenomic[0];
            currentPosition=currentPosGenomic[1];
        } else {
            anchor1=firstAnchorGenomic[1];
            currentPosition=currentPosGenomic[0];
        }
    }

    // adjusts the start/end position of the currently drawn region
    private void updateRegion() {
       int start=(anchor1<anchor2)?anchor1:anchor2;
       int end=(anchor1>anchor2)?anchor1:anchor2;
       gui.statusMessage(featureName+": new region ["+start+"-"+end+"]    ("+(end-start+1)+" bp)");
       start=sequencedata.getRelativePositionFromGenomic(start);
       end=sequencedata.getRelativePositionFromGenomic(end);
       if (newregion==null) {
           double score=((RegionSequenceData)sequencedata).getMaxScoreValue(); // use highest score so that the regions shows
           if (score==0) score=1.0;
           newregion=new Region(((RegionSequenceData)sequencedata), start, end, "unknown", score, Region.INDETERMINED);
       } else { // update start/end properties
           newregion.setRelativeStart(start);
           newregion.setRelativeEnd(end);
       }
       repaintVisibleSequenceSegment(); // DataTrackVisualizer_Region.this.repaint();
    }
}
