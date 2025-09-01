package org.motiflab.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JDialog;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Region;

/**
 * This class is the default DataTrackVisualizer implementation used for rendering Region Datasets
 * 
 * @author kjetikl
 */
public class DataTrackVisualizer_Region_Default extends DataTrackVisualizer_RegionAbstract {
    
    private static final DataTrackVisualizerSetting fillSetting;       
    private static final DataTrackVisualizerSetting boxPaddingSetting;       
    
    private static final String[] connectorTypes=new String[]{"Straight Line","Angled Line","Curved Line","Ribbon"};    

    protected int gradientfill=0;      
    protected boolean regionBoxPadding=false;    
    
    /** Constructs a new DefaultDataTrackVisualizer */
    public DataTrackVisualizer_Region_Default() {
        super();     
    }
    
    @Override
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
        super.initialize(sequenceName, featureName, settings);   
    }     
    
    @Override
    public String getGraphTypeName() {
        return "Region";
    }  
    
    @Override
    public SimpleDataPanelIcon getGraphTypeIcon() {
        return new DefaultRegionSimpleDataPanelIcon(20,20);
    }      
        
    @Override
    public void refreshSettings() {
        super.refreshSettings();
        gradientfill=settings.useGradientFill(featureName);
        regionBoxPadding=(Boolean)settings.getSettingAsType(featureName+".regionBoxMargin", Boolean.TRUE);             
    }    

    @Override
    public String[] getConnectorTypes() {
        return connectorTypes;
    }
    
    
    
    /*
     * Draws a single region in 1:1 scale (canvas pixel == screen pixel)
     */
    @Override
    protected void drawRegion(Graphics2D g, Region region, RegionSequenceData sequencedata, Rectangle bounds, int direction, Color regioncolor, Color bordercolor, Color labelcolor, Motif motif, Color[] basecolors, Integer thickStart, Integer thickEnd, boolean drawShadows) {        
        int x=bounds.x;
        int y=bounds.y;
        int width=bounds.width;
        int height=bounds.height;
        
        Paint color=(gradientfill>0)?getGradientPaint(regioncolor, x, y, width, height, gradientfill):regioncolor;                     
        if (thickStart!=null || thickEnd!=null) { // draw Thick/Thin-box
            int y2=(int)(y+height*0.25+0.5);
            int y3=(int)(y+height*0.75+0.5);
            int y4=y+height;   
            int end=x+width;            
            Polygon thickThinBox;
            Polygon thickThinBoxBorder;
                 if ((thickStart!=null && thickStart>x+width) || (thickEnd!=null && thickEnd<x)) thickThinBox=new Polygon(new int[]{x,end,end,x}, new int[]{y2,y2,y3,y3}, 4); // just a thin box all the way
            else if (thickStart!=null && thickEnd==null)   thickThinBox=new Polygon(new int[]{x,thickStart,thickStart,end,end,thickStart,thickStart,x}, new int[]{y2,y2,y,y,y4,y4,y3,y3}, 8);   // thin beginning and thick end
            else if (thickStart!=null && thickEnd!=null)   thickThinBox=new Polygon(new int[]{x,thickStart,thickStart,thickEnd,thickEnd,end,end,thickEnd,thickEnd,thickStart,thickStart,x}, new int[]{y2,y2,y,y,y2,y2,y3,y3,y4,y4,y3,y3}, 12);    // thin beginning, thick in the middle and thin end
            else if (thickStart==null && thickEnd!=null)   thickThinBox=new Polygon(new int[]{x,thickEnd,thickEnd,end,end,thickEnd,thickEnd,x}, new int[]{y,y,y2,y2,y3,y3,y4,y4}, 8);   // thick beginning and thin end
            else thickThinBox=new Polygon(new int[]{x,end,end,x}, new int[]{y,y,y4,y4}, 4); // Thick box all the way. This is just included for testing
            if (drawShadows) {
                g.setColor(shadowColor);
                thickThinBox.translate(shadowOffset, shadowOffset);          
                g.fillPolygon(thickThinBox);
                thickThinBox.translate(-1, -1);         
                g.fillPolygon(thickThinBox);
                thickThinBox.translate(-(shadowOffset-1), -(shadowOffset-1));            
            }   
            g.setPaint(color);
            g.fill(thickThinBox);
            if (bordercolor!=null) {             
                // adjust coordinates for border since this is normally offset +1 at right and bottom edge
                y4=y4-1;
                y3=y3-1;  
                end=end-1;
                int useThickEnd=0;
                if (thickEnd!=null) {useThickEnd=(thickEnd>x)?thickEnd-1:thickEnd;} // adjust thickEnd, but make sure it does not end up before the start because that will lead to a twisted shape      
                     if ((thickStart!=null && thickStart>x+width) || (thickEnd!=null && thickEnd<x)) thickThinBoxBorder=new Polygon(new int[]{x,end,end,x}, new int[]{y2,y2,y3,y3}, 4); // just a thin box all the way
                else if (thickStart!=null && thickEnd==null)   thickThinBoxBorder=new Polygon(new int[]{x,thickStart,thickStart,end,end,thickStart,thickStart,x}, new int[]{y2,y2,y,y,y4,y4,y3,y3}, 8);   // thin beginning and thick end
                else if (thickStart!=null && thickEnd!=null)   thickThinBoxBorder=new Polygon(new int[]{x,thickStart,thickStart,useThickEnd,useThickEnd,end,end,useThickEnd,useThickEnd,thickStart,thickStart,x}, new int[]{y2,y2,y,y,y2,y2,y3,y3,y4,y4,y3,y3}, 12);    // thin beginning, thick in the middle and thin end
                else if (thickStart==null && thickEnd!=null)   thickThinBoxBorder=new Polygon(new int[]{x,useThickEnd,useThickEnd,end,end,useThickEnd,useThickEnd,x}, new int[]{y,y,y2,y2,y3,y3,y4,y4}, 8);   // thick beginning and thin end
                else thickThinBoxBorder=new Polygon(new int[]{x,end,end,x}, new int[]{y,y,y4,y4}, 4); // Thick box all the way. This is just included for testing
                g.setPaint(bordercolor);
                g.draw(thickThinBoxBorder);
            }              
        } else if (!drawDirectionArrows || direction==Region.INDETERMINED || !isCloseUp()) { // draw as a regular square box in non-closeup mode
            // draw the region box itself and optional border
            int padding=(regionBoxPadding && isCloseUp())?3:0;              
            int useX=x-padding;
            int useWidth=width+padding+padding;
            if (drawShadows) {
                g.setColor(shadowColor);
                g.fillRect(useX+shadowOffset, y+shadowOffset, useWidth, height);     // I paint two shadow boxes slighly offset to create a sort of feather/blend effect                
                g.fillRect(useX+shadowOffset-1, y+shadowOffset-1, useWidth, height); //                  
            }              
            g.setPaint(color);
            g.fillRect(useX, y, useWidth, height); // Since we are drawing at 1:1 scale. The width and height are correct when using fillRect()
            if (bordercolor!=null) {
                g.setPaint(bordercolor);
                g.drawRect(useX, y, useWidth-1, height-1); // The right and bottom border are drawn one pixel off in 1:1 scale so we must adjust for this
            }  
        } else { // draw as box with "arrows" indicating the orientation
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);        
            Polygon polygon;
            int padding=(regionBoxPadding && isCloseUp())?3:0;             
            int leftEdge  = x-padding;       // The old DTV used an offset of -3 here to create an additional padding around the motif logo
            int rightEdge = x+width+padding; // The old DTV used an offset of +3 here to create an additional padding around the motif logo
            int boxheight = height;
            double scale=settings.getScale(sequenceName);
            int wedgewidth=(int)(scale*0.5); if (wedgewidth>10) wedgewidth=10; // The size of the wedge is 1/2 of a bp (or max 10)
            int[] xpoints=null;
            int[] ypoints=null;
            
            if (direction==Region.DIRECT) { // Draw region pointing towards the right   
                xpoints = new int[]{leftEdge,rightEdge,rightEdge+wedgewidth,rightEdge,leftEdge};
                ypoints = new int[]{y, y, (int)(y+((double)boxheight)/2f+0.5),y+boxheight,y+boxheight};
            } else if (direction==Region.REVERSE){ // Draw region pointing towards the left
                xpoints = new int[]{rightEdge,leftEdge,leftEdge-(wedgewidth),leftEdge,rightEdge};
                ypoints = new int[]{y, y, (int)(y+((double)boxheight)/2f+0.5),y+boxheight,y+boxheight};
            }       
            polygon=new Polygon(xpoints,ypoints, xpoints.length);
            if (drawShadows) {
                g.setColor(shadowColor);
                polygon.translate(shadowOffset, shadowOffset);          
                g.fillPolygon(polygon);
                polygon.translate(-1, -1);         
                g.fillPolygon(polygon);
                polygon.translate(-(shadowOffset-1), -(shadowOffset-1));            
            }           
            g.setPaint(color);
            g.fillPolygon(polygon); // Since we are drawing at 1:1 scale. The width and height are correct when using fillRect()
            if (bordercolor!=null) {
                // When "drawing" rather than "filling" the polygon, the right and bottom edges are offset by +1 pixel. 
                // Here we create a new polygon to correct for this (which may really be overkill, but I do it anyway...)
                rightEdge=rightEdge-1;
                boxheight=boxheight-1;
                if (direction==Region.DIRECT) { // Draw region pointing towards the right   
                    xpoints = new int[]{leftEdge,rightEdge,rightEdge+wedgewidth,rightEdge,leftEdge};
                    ypoints = new int[]{y, y, (int)(y+((double)boxheight)/2f+0.5),y+boxheight,y+boxheight};
                } else if (direction==Region.REVERSE){ // Draw region pointing towards the left
                    xpoints = new int[]{rightEdge,leftEdge,leftEdge-(wedgewidth),leftEdge,rightEdge};
                    ypoints = new int[]{y, y, (int)(y+((double)boxheight)/2f+0.5),y+boxheight,y+boxheight};
                }  
                polygon=new Polygon(xpoints,ypoints, xpoints.length);
                g.setPaint(bordercolor);
                g.drawPolygon(polygon); // The right and bottom border are drawn one pixel off in 1:1 scale so we must adjust for this
            }         
        }
        
        int logoheight=(showTypeLabel && labelcolor!=null)?(height-(typeLabelHeight+2)):(height-2);      
        boolean drawLogo  = (showMotifLogo && motif!=null && logoheight>=minlogoheight && region!=null && region.getOrientation()!=Region.INDETERMINED); 
        boolean drawLabel = (showTypeLabel && labelcolor!=null && height>typeLabelHeight && region!=null);

        if (drawLogo) { // draw motif logo for motif regions.
            int useY=y+1;
            int useLogoHeight=(drawLabel)?logoheight:logoheight-2;
            String sequence=(String)region.getProperty("sequence");
            boolean showDirect=(direction==Region.DIRECT); //                       
            if (!showDirect) sequence=MotifLabEngine.reverseSequence(sequence);  
            Rectangle oldClipRect=g.getClipBounds();
            Rectangle rect=new Rectangle(x+1,useY,width-2,useLogoHeight+2);         
            rect=oldClipRect.intersection(rect);
            g.setClip(rect); // use a clip-rect to force a border around the match logo where the background color of the region can shine through
            paintMatchLogo(g, motif, sequence, x, useY+2, width, useLogoHeight, showDirect, settings, basecolors, bordercolor, scaleMotifLogoByIC);    
            g.setClip(oldClipRect);
        }
            
        if (drawLabel) { // draw type label (if space permits). If a motif is provided, try to fit the full presentation name first, if this is too long, try the short name or finally the region type 
            int textstart=x; // these two coordinates can be used to offset the label if only parts of the region is visible (normally it is drawn in the middle of the region)
            int textend=x+width;
            int boxWidth=textend-textstart+1;
            g.setFont(TYPE_LABEL_FONT);
            g.setColor(labelcolor);
            String boxtext=(motif!=null)?motif.getPresentationName():region.getType();
            if (boxtext==null) boxtext="UNKNOWN";
            int stringwidth=label_font_metrics.stringWidth(boxtext)+4; // +2 is just a little margin
            if (stringwidth>=boxWidth) { // motif presentation name does not fit in the box. Try just the shortname
                boolean fallback=false;
                if (motif!=null) {
                    boxtext=motif.getShortName();
                    if (boxtext==null || boxtext.isEmpty()) fallback=true;
                    else {
                         stringwidth=label_font_metrics.stringWidth(boxtext)+4; // +2 is just a little margin
                         if (stringwidth>=boxWidth) fallback=true; // just use MotifID if shortname is too wide
                    }
                } else fallback=true;
                if (fallback) {
                    boxtext=region.getType();
                    if (boxtext==null) boxtext="UNKNOWN";
                    stringwidth=label_font_metrics.stringWidth(boxtext)+4; // +2 is just a little margin
                }
            }
            int textoffset=(int)(textstart+(boxWidth-stringwidth+0.5f)/2.0f+2);
            int label_y=(drawLogo)?(y+height-4):(y+(height+typeLabelHeight)/2-3); // If there is no logo, align the text in the middle vertically. else align the text at the bottom
            if (stringwidth<boxWidth) {
                Object hint=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);                   
                g.drawString(boxtext, textoffset, label_y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,hint);
            }  
        }            
    }
    

    
     /**
      * Draws a horizontal line throughout the middle of the region
      * or arched connector lines between pairs of successive nested regions
      * (the superclass draws the "bounding box" or "region" styles, if these are selected) 
      */
     @Override
     protected void drawConnectors(Graphics2D graphics, Region region, RegionSequenceData sequencedata, Rectangle bounds, int graphicsXoffset, int direction, Color regioncolor, Color bordercolor, String connectorType) {
        if (connectorType.equals("Straight Line")) {  
            Object renderHint=graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);            
            int y=bounds.y+bounds.height/2;
            int thickness=1;
            if (bounds.height>20) {thickness++;}
            if (bounds.height>30) {thickness++;}
            Stroke arrowStroke=new BasicStroke((float)thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
            Stroke currentStroke=graphics.getStroke();
            graphics.setStroke(arrowStroke);              
            if (dropShadows) {
                graphics.setColor(shadowColor);
                graphics.drawLine(bounds.x+shadowOffset, y+shadowOffset, (bounds.x+bounds.width-1)-shadowOffset, y+shadowOffset);
            } 
            graphics.setColor(regioncolor);
            graphics.drawLine(bounds.x, y, (bounds.x+bounds.width-1), y);            
            // draw direction arrows on the line
            if (showStrand && direction!=Region.INDETERMINED) {                    
                int spacing=(Integer)settings.getSettingAsType(featureName+".arrowSpacing", 20); // should these be "system settings" rather than feature specific?                
                int arrowSize=(4+thickness);// (Integer)settings.getSettingAsType(featureName+".arrowSize", 5);                 
                int startPixel=bounds.x;
                int endPixel=bounds.x+bounds.width;                
                // if (startPixel<0) startPixel=startPixel%spacing; // this can not be set to simply zero, since the arrowheads then will not scroll smoothly across the edge                
                if (endPixel>sequenceWindowWidth+spacing) endPixel=sequenceWindowWidth+spacing;
                int[] yCoordinates=new int[]{y-arrowSize,y,y+arrowSize};
                for (int offset=startPixel;offset<endPixel-spacing/2;offset+=spacing) {
                     int anchor=offset+spacing/2;
                     int[] xCoordinates=(direction==Region.DIRECT)?new int[]{anchor-arrowSize,anchor,anchor-arrowSize}:new int[]{anchor+arrowSize,anchor,anchor+arrowSize};
                     graphics.drawPolyline(xCoordinates,yCoordinates,3);
                }  
            }
            graphics.setStroke(currentStroke); 
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,renderHint);
            return;
        } 
        if (!(connectorType.equals("Angled Line") || connectorType.equals("Curved Line") || connectorType.equals("Ribbon"))) {
            // if the connectorType is not implemented here, ask the parent to draw it instead
            super.drawConnectors(graphics, region, sequencedata, bounds, graphicsXoffset, direction, regioncolor, bordercolor, connectorType);
            return;
        }    
        double strokeWidth=(Double)settings.getSettingAsType(featureName+".connectorWidth", 1.4);      
        Stroke currentStroke=graphics.getStroke(); 
        Stroke connectorStroke=new BasicStroke((float)strokeWidth, BasicStroke.CAP_BUTT, (connectorType.equals("Angled Line"))?BasicStroke.JOIN_MITER:BasicStroke.JOIN_ROUND);
        graphics.setStroke(connectorStroke);        
        Object renderHint=graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int top_y=bounds.y;// +((relativeOrientation)?topOffset:-topOffset); // y is middle of region     
        int bottom_y=bounds.y+bounds.height-1;
        int middle_y=(int)(bounds.y+bounds.height/2);
        int controlOffsetReset=(drawNested)?(bounds.height/2):bounds.height;
        if (!connectorType.equals("Angled Line")) controlOffsetReset*=2; // because the way quadcurves are designed. The controlpoint must be twice as far away for the curve to cross at the same point
        
        ArrayList<Region> nested=region.getNestedRegions(false);
        Collections.sort(nested, sequencedata.getRegionSortOrderComparator()); // just to get the regions properly ordered
        for (int i=1;i<nested.size();i++) {
            graphics.setColor(regioncolor);            
            Region first=nested.get(i-1); // the first region in the pair
            Region second=nested.get(i);
            int[] connectorSegment=getScreenCoordinateRangeFromGenomic(first.getGenomicEnd(),second.getGenomicStart(),graphicsXoffset);
            int connectorStart=connectorSegment[0];
            int connectorEnd=connectorSegment[1];
            int controlOffset=controlOffsetReset;
            if (scaleConnectors && !expanded) {
                int archWidth=connectorEnd-connectorStart;
                double percentage=(double)archWidth/((double)sequenceWindowWidth*0.6); // 0.6 is a scaling factor. Hence, if archwidth is 60% of window the percentage will be 100%          
                if (percentage<0.1) percentage=0.1; // just to give a minimum rise to the arch
                if (percentage>0.98) percentage=0.98; // just to add a little padding
                controlOffset*=percentage;     
            }                        
            int anchorY=(drawNested)?middle_y:((upsideDown)?top_y-2:bottom_y+2); // the point where the connector attaches to the region. Here I use the middle of the box when nested regions are drawn, else the full height (either top or bottom depending on "upsideDown")
            int middleX=(int)(connectorStart+0.5+(connectorEnd-connectorStart)/2.0);
            int controlTopY=(upsideDown)?anchorY+controlOffset:anchorY-controlOffset;
            if (showStrand && direction==Region.REVERSE) { // flip connectors upside down
                anchorY=(drawNested)?middle_y:((upsideDown)?bottom_y:top_y);    
                controlTopY=(upsideDown)?anchorY-controlOffset:anchorY+controlOffset;                
            } else if (showStrand && direction==Region.INDETERMINED) { // do not curve
                anchorY=(int)(bounds.y+bounds.height/2);
                controlTopY=anchorY;
            }           
            if (connectorType.equals("Angled Line")) {
                if (dropShadows) {
                    graphics.setColor(shadowColor);
                    graphics.drawPolyline(new int[]{connectorStart+shadowOffset,middleX+shadowOffset,connectorEnd+shadowOffset},new int[]{anchorY+shadowOffset,controlTopY+shadowOffset,anchorY+shadowOffset},3); // should this be in the middle? 
                }
                graphics.setColor(regioncolor);           
                graphics.drawPolyline(new int[]{connectorStart,middleX,connectorEnd},new int[]{anchorY,controlTopY,anchorY},3); //                  
            }
            else if (connectorType.equals("Curved Line")) {
                QuadCurve2D quadcurve;
                if (dropShadows) {
                    graphics.setColor(shadowColor);
                    quadcurve = new QuadCurve2D.Float(connectorStart+shadowOffset, anchorY+shadowOffset, middleX+shadowOffset, controlTopY+shadowOffset, connectorEnd+shadowOffset, anchorY+shadowOffset);
                    graphics.draw(quadcurve);
                }    
                graphics.setColor(regioncolor);                  
                quadcurve = new QuadCurve2D.Float(connectorStart, anchorY, middleX, controlTopY, connectorEnd, anchorY);                
                graphics.draw(quadcurve); 
            } else if (connectorType.equals("Ribbon")) {             
                int topOffset=(controlTopY<anchorY)?-2:2; // just an offset so that the ribbon has some thickness at the top point
                int[] outerConnectorSegment=getScreenCoordinateRangeFromGenomic(first.getGenomicStart(),second.getGenomicEnd(),graphicsXoffset);
                int firstStart=outerConnectorSegment[0];
                int secondEnd=outerConnectorSegment[1];
                GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
                polygon.moveTo(connectorStart, anchorY);
                polygon.quadTo(middleX, controlTopY-topOffset, connectorEnd, anchorY);
                polygon.lineTo(secondEnd, anchorY);
                polygon.quadTo(middleX, controlTopY+topOffset, firstStart, anchorY);   
                polygon.closePath();
                graphics.fill(polygon);
                if (bordercolor!=null) {
                   graphics.setColor(bordercolor);
                   graphics.draw(polygon);
                } 
            } 
        }     
        graphics.setStroke(currentStroke);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,renderHint);         
     } // end of drawRegion     
    

    // --------------------------------------------------------------------------------
    
    
    private class DefaultRegionSimpleDataPanelIcon extends SimpleDataPanelIcon {
        
        public DefaultRegionSimpleDataPanelIcon(int height, int width) {
            super(height,width,SimpleDataPanelIcon.CUSTOM_ICON,null);
        }

        public void drawCustomIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {  
            boolean expanded=settings.isExpanded(this.trackName); // trackName is inherited from SimpleDataPanelIcon
            boolean multiColor=settings.useMultiColoredRegions(this.trackName);
            if (backgroundcolor!=null) {
               g.setColor(backgroundcolor);
               g.fillRect(x, y, width, height);
            }                   
            if (expanded) {
                 super.drawRegionExpandedIcon(g, x, y, multiColor);
            } else {
                 if (multiColor) {
                     super.drawRegionMulticolorIcon(g, x, y);
                 } else {
                     int gradientfill=settings.useGradientFill(this.trackName);
                          if (gradientfill==1) drawRegionGradientIcon(g, x, y, true); 
                     else if (gradientfill==2) drawRegionGradientIcon(g, x, y, false); 
                     else drawRegionIcon(g, x, y);
                 }
            }   
        }  
    }

    // ---------------------------------------------------------------------------
      
    @Override
    public ArrayList<DataTrackVisualizerSetting> getGraphTypeSettings() {
        ArrayList<DataTrackVisualizerSetting> graphsettings = super.getGraphTypeSettings();
        if (graphsettings==null) graphsettings=new ArrayList<>();            
        addGraphTypeSetting(graphsettings, fillSetting, false);     
        addGraphTypeSetting(graphsettings, boxPaddingSetting, false);          
        return graphsettings;
    }
    
    static {
        // This setting is configured to correspond with a legacy setting
        fillSetting=new DataTrackVisualizerSetting("Fill", VisualizationSettings.GRADIENT_FILL_REGIONS, DataTrackVisualizerSetting.MAJOR_SETTING, DataTrackVisualizerSetting.ALL, KeyEvent.VK_F);
        fillSetting.addOption("Horizontal Gradient",1); // horizontal fill is the default style according to the legacy setting, so I place that as the first option
        fillSetting.addOption("Vertical Gradient",2);         
        fillSetting.addOption("Flat Fill",0);     

        boxPaddingSetting=new DataTrackVisualizerSetting("Add Box Padding", "regionBoxMargin", DataTrackVisualizerSetting.MINOR_SETTING, DataTrackVisualizerSetting.ALL, 0); 
        boxPaddingSetting.addOption("On",true);   
        boxPaddingSetting.addOption("Off",false);  
        boxPaddingSetting.setBooleanOption(true);  
        boxPaddingSetting.setDocumentationString("Extend the region box with a small horizontal padding on both sides when viewing close up"); 
               
    }    
    
    
}
