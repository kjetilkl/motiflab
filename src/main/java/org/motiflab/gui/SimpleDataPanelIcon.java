package org.motiflab.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.motiflab.engine.Graph;
import org.motiflab.engine.data.*;
import org.motiflab.engine.data.analysis.Analysis;

/**
 * This class is an implementation of Icon that can draw different
 * simple icons to be used in various circumstances
 * 
 * @author kjetikl
 */
public class SimpleDataPanelIcon implements Icon {
    public static final int UNKNOWN_ICON=0;
    public static final int BLANK_ICON=1;
    public static final int SEQUENCE_ICON=2;
    public static final int SEQUENCE_ICON_BASES=3;
    public static final int REGION_ICON=4;
    public static final int REGION_MULTICOLOR_ICON=5;
    public static final int REGION_GRADIENT_ICON=6;
    public static final int NUMERIC_TRACK_GRAPH_ICON=7;
    public static final int NUMERIC_TRACK_GRADIENT_ICON=8;
    public static final int HIDDEN_ICON=9;
    public static final int COLOR_ICON=10;
    public static final int SEQUENCE_COLLECTION_ICON=11;
    public static final int MARKOV_MODEL_ICON=12;
    public static final int NUMERIC_VARIABLE_ICON=13;
    public static final int STRING_VARIABLE_ICON=14;
    public static final int TEXT_VARIABLE_ICON=15;
    public static final int COLOR_ICON_SELECTED=16;
    public static final int SEQUENCE_NUMERIC_MAP_ICON=17;
    public static final int MOTIF_NUMERIC_MAP_ICON=18;
    public static final int ANALYSIS_ICON=19;
    public static final int SEQUENCE_PARTITION_ICON=20;
    public static final int EXPRESSION_PROFILE_ICON=21;
    public static final int PRIORS_GENERATOR_ICON=22;
    public static final int REGION_EXPANDED_ICON=23;
    public static final int REGION_EXPANDED_MULTICOLOR_ICON=24;
    public static final int MOTIF_ICON=25;      
    public static final int MOTIF_COLLECTION_ICON=26;
    public static final int MOTIF_PARTITION_ICON=27;
    public static final int MODULE_ICON=28;     
    public static final int MODULE_COLLECTION_ICON=29;
    public static final int MODULE_PARTITION_ICON=30;
    public static final int MODULE_NUMERIC_MAP_ICON=31;    
    public static final int NUMERIC_TRACK_LINEGRAPH_ICON=32;
    public static final int NUMERIC_TRACK_RAINBOW_HEATMAP_ICON=33;    
    public static final int NUMERIC_TRACK_TWOCOLOR_HEATMAP_ICON=34; 
    public static final int NUMERIC_TRACK_OUTLINED_GRAPH_ICON=35;
    public static final int REGION_HORISONTAL_GRADIENT_ICON=36;    
    public static final int MOTIF_TEXT_MAP_ICON=37;     
    public static final int MODULE_TEXT_MAP_ICON=38;     
    public static final int SEQUENCE_TEXT_MAP_ICON=39;   
    public static final int OUTPUT_DATA_ICON=40;    
    public static final int NUMERIC_TRACK_GRADIENT_BAR_ICON=41; 
    public static final int NUMERIC_TRACK_DNA_GRAPH_ICON=42;
    public static final int IMAGE_ICON=43;
    public static final int CUSTOM_ICON=44; // this should be drawn by a subclass in the overridable drawCustom method      

    
    public static final int NO_BORDER=0;
    public static final int SIMPLE_BORDER=1; // this will draw the border 1px outside the icon's area on the right and bottom sides
    public static final int BEVEL_BORDER=2; // this will draw the border 1px outside the icon's area on the right and bottom sides
    public static final int SIMPLE_BORDER_INSIDE=3; // this correctly places the border inside the icon's area   
    public static final int BEVEL_BORDER_INSIDE=4; // this correctly places the border inside the icon's area   

    public static final ImageIcon bgicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/bgmodel.gif")));
    public static final ImageIcon analysisicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/analysis_icon_simple.png"))); // previously "analysis_icon.gif"
    public static final ImageIcon expressionprofileicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/expressionprofile.png")));
    public static final ImageIcon priorsgeneratoricon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/gear.gif")));
    public static final ImageIcon numericvariableicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/numericvariable.gif")));
    public static final ImageIcon numericmapSicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/numericmapS.gif")));
    public static final ImageIcon numericmapMicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/numericmapMotif.gif")));
    public static final ImageIcon numericmapModuleicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/numericmapModule.gif")));
    public static final ImageIcon textmapSicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/textmapS.gif")));
    public static final ImageIcon textmapMicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/textmapMotif.gif")));
    public static final ImageIcon textmapModuleicon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/textmapModule.gif")));
    public static final ImageIcon stringIcon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/stringicon.gif")));
    public static final ImageIcon textVariableIcon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/stringicon.gif")));  // or "stringcolicon.gif"  (abc/def)
    public static final ImageIcon motifIcon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/motif_icon.png")));
    public static final ImageIcon moduleIcon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/module_icon.png")));
    public static final ImageIcon outputDataIcon=new ImageIcon(Toolkit.getDefaultToolkit().getImage(SimpleDataPanelIcon.class.getResource("resources/icons/outputData_icon.png")));
 
    
    protected int height=0;
    protected int width=0;
    protected int type=0;
    protected Color foregroundcolor=Color.red;
    protected Color backgroundcolor=null;// Color.white;
    protected Color secondarycolor=Color.red;    
    protected Color bordercolor=Color.black;
    protected Color baselinecolor=Color.lightGray;
    protected ColorGradient gradient=null;
    protected ColorGradient secondarygradient=null;
    protected int drawborder=NO_BORDER;
    protected boolean is3D=false;
    protected VisualizationSettings settings;
    protected ImageIcon image=null;
    protected int imageOffsetX=0;
    protected int imageOffsetY=0;
    protected String trackName=null; // name of associated datatrack (used for looking up settings directly in VisualizationSettings)
        
    public SimpleDataPanelIcon(int height, int width, int type, VisualizationSettings settings) {
        this.height=height;
        this.width=width;
        this.type=type;
        this.settings=settings;
    }
   
    public SimpleDataPanelIcon(int height, int width, int type, int bordertype, VisualizationSettings settings) {
        this.height=height;
        this.width=width;
        this.type=type;
        this.drawborder=bordertype;
        this.settings=settings;
    }

    /* Returns a color-icon (a colored square with a simple border */
    public SimpleDataPanelIcon(int height, int width, Color color) {
        this.height=height;
        this.width=width;
        this.type=COLOR_ICON;
        this.drawborder=SIMPLE_BORDER;
        this.settings=null;
        this.foregroundcolor=color;
        this.backgroundcolor=Color.BLACK;
    }

 
    public void setVisualizationSettings(VisualizationSettings settings) {
        this.settings=settings;
    }   

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }
    
    public void setIconWidth(int width) {
        this.width=width;
    }
    
    public void setIconHeight(int height) {
        this.height=height;
    }
    
    public void setIconType(int type) {
        this.type=type;
    }
    
    public int getIconType() {
        return type;
    }    
    
    public void setForegroundColor(Color color) {
        this.foregroundcolor=color;
    }
    
    public Color getForegroundColor() {
        return this.foregroundcolor;
    }    
    
    public void setSecondaryColor(Color color) {
        this.secondarycolor=color;
    }    
    
    public void setBackgroundColor(Color color) {
        this.backgroundcolor=color;
    }
    
    public void setBaselineColor(Color color) {
        this.baselinecolor=color;
    }
    
    public void setGradient(ColorGradient gradient) {
        this.gradient=gradient;
    }
    
    public void setSecondaryGradient(ColorGradient gradient) {
        this.secondarygradient=gradient;
    }    
    
    public void setBorderColor(Color color) {
        this.bordercolor=color;
    }

    public void drawBorder(int borderType) {
        this.drawborder=borderType;
    }
    
    public void drawBorder(boolean drawborder) {
        if (drawborder) this.drawborder=SIMPLE_BORDER;
        else this.drawborder=NO_BORDER;
    }
    
    public void set3D(boolean is3D) {
        this.is3D=is3D;
    }
    
    public void setImage(ImageIcon icon, int x, int y) {
        this.image=icon;
        this.imageOffsetX=x;
        this.imageOffsetY=y;
    }    
    
    public void setTrackName(String trackName) {
        this.trackName=trackName;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {           
             if (type==SEQUENCE_ICON) drawSingleSequenceIcon(g, x, y);
        else if (type==SEQUENCE_ICON_BASES) drawSequenceIcon(g, x, y, true);
        else if (type==REGION_ICON) drawRegionIcon(g, x, y);
        else if (type==REGION_MULTICOLOR_ICON) drawRegionMulticolorIcon(g, x, y);
        else if (type==REGION_GRADIENT_ICON) drawRegionGradientIcon(g, x, y, true);
        else if (type==REGION_HORISONTAL_GRADIENT_ICON) drawRegionGradientIcon(g, x, y, false);        
        else if (type==NUMERIC_TRACK_GRAPH_ICON) drawNumericTrackGraphIcon(g, x, y);  
        else if (type==NUMERIC_TRACK_LINEGRAPH_ICON) drawNumericTrackLineGraphIcon(g, x, y);  
        else if (type==NUMERIC_TRACK_OUTLINED_GRAPH_ICON) drawNumericTrackOutlinedGraphIcon(g, x, y);  
        else if (type==NUMERIC_TRACK_GRADIENT_BAR_ICON) drawNumericTrackGradientBarGraphIcon(g, x, y);
        else if (type==NUMERIC_TRACK_GRADIENT_ICON) drawNumericTrackGradientIcon(g, x, y);  
        else if (type==NUMERIC_TRACK_RAINBOW_HEATMAP_ICON) drawNumericTrackRainbowHeatmapIcon(g, x, y);  
        else if (type==NUMERIC_TRACK_TWOCOLOR_HEATMAP_ICON) drawNumericTrack2colorHeatmapIcon(g, x, y);  
        else if (type==NUMERIC_TRACK_DNA_GRAPH_ICON) drawNumericTrackDNAGraphIcon(g, x, y);
        else if (type==HIDDEN_ICON) drawHiddenIcon(g, x, y);  
        else if (type==COLOR_ICON) drawColorIcon(g, x, y);  
        else if (type==COLOR_ICON_SELECTED) drawColorSelectedIcon(g, x, y);  
        else if (type==SEQUENCE_COLLECTION_ICON) drawSequenceCollectionIcon(g, x, y);  
        else if (type==SEQUENCE_PARTITION_ICON) drawSequencePartitionIcon(g, x, y);  
        else if (type==NUMERIC_VARIABLE_ICON) numericvariableicon.paintIcon(c, g, x, y);//drawNumericVariableIcon(g, x, y);
        else if (type==MARKOV_MODEL_ICON) bgicon.paintIcon(c, g, x, y);//drawMarkovModelIcon(g, x, y);
        else if (type==UNKNOWN_ICON) drawUnknownIcon(g, x, y);  
        else if (type==SEQUENCE_NUMERIC_MAP_ICON) numericmapSicon.paintIcon(c, g, x, y);//drawSequenceNumericMapIcon(g, x, y);
        else if (type==MOTIF_NUMERIC_MAP_ICON) numericmapMicon.paintIcon(c, g, x, y);//drawMotifNumericMapIcon(g, x, y);
        else if (type==MODULE_NUMERIC_MAP_ICON) numericmapModuleicon.paintIcon(c, g, x, y);//drawModuleNumericMapIcon(g, x, y);
        else if (type==SEQUENCE_TEXT_MAP_ICON) textmapSicon.paintIcon(c, g, x, y);//drawSequenceTextMapIcon(g, x, y);
        else if (type==MOTIF_TEXT_MAP_ICON) textmapMicon.paintIcon(c, g, x, y);//drawMotifTextMapIcon(g, x, y);
        else if (type==MODULE_TEXT_MAP_ICON) textmapModuleicon.paintIcon(c, g, x, y);//drawModuleTextMapIcon(g, x, y);
        else if (type==TEXT_VARIABLE_ICON) textVariableIcon.paintIcon(c, g, x, y);//drawTextVariableIcon(g, x, y);
        else if (type==ANALYSIS_ICON) analysisicon.paintIcon(c, g, x, y);
        else if (type==EXPRESSION_PROFILE_ICON) expressionprofileicon.paintIcon(c, g, x, y+2);
        else if (type==PRIORS_GENERATOR_ICON) priorsgeneratoricon.paintIcon(c, g, x, y);
        else if (type==REGION_EXPANDED_ICON) drawRegionExpandedIcon(g, x, y, false);
        else if (type==REGION_EXPANDED_MULTICOLOR_ICON) drawRegionExpandedIcon(g, x, y, true);
        else if (type==MOTIF_PARTITION_ICON) drawMotifPartitionIcon(c, g, x, y);
        else if (type==MODULE_PARTITION_ICON) drawModulePartitionIcon(c, g, x, y);
        else if (type==MOTIF_COLLECTION_ICON) drawMotifCollectionIcon(c, g, x, y);
        else if (type==MODULE_COLLECTION_ICON) drawModuleCollectionIcon(c, g, x, y);
        else if (type==MOTIF_ICON) motifIcon.paintIcon(c, g, x, y);
        else if (type==MODULE_ICON) moduleIcon.paintIcon(c, g, x, y);            
        else if (type==OUTPUT_DATA_ICON) outputDataIcon.paintIcon(c, g, x, y);  
        else if (type==IMAGE_ICON && image!=null) drawImageIcon(c, g, x, y);
        else if (type==CUSTOM_ICON) drawCustomIcon(c, g, x, y);             

        if (drawborder==SIMPLE_BORDER) drawSimpleIconBorder(g, x, y, 0);
        else if (drawborder==BEVEL_BORDER) drawBevelIconBorder(g, x, y, 0);
        else if (drawborder==SIMPLE_BORDER_INSIDE) drawSimpleIconBorder(g, x, y, -1);        
        else if (drawborder==BEVEL_BORDER_INSIDE) drawBevelIconBorder(g, x, y, -1);        
    }
       
    protected void drawImageIcon(Component c, Graphics g, int x, int y) {    
        if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
        }        
        image.paintIcon(c, g, x+imageOffsetX, y+imageOffsetY);
    }
    
    /** This method should be overridden by subclasses of SimpleDataPanelIcon to implement custom icons */
    public void drawCustomIcon(Component c, Graphics g, int x, int y) {    
        if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
        }                
    }    
    
    protected void drawSingleSequenceIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         g.setColor(Color.BLACK);
         y+=5; // valign=center
         g.drawRect(x, y+2, 17, 2);   
         
         g.setColor(new Color(50,50,255)); // light Blue
         g.drawLine(x+1, y+3, x+16, y+3);

         g.setColor(Color.RED);
         g.drawLine(x+1, y+3, x+2, y+3);
         g.drawLine(x+13, y+3, x+14, y+3);
         
         g.setColor(Color.YELLOW);
         g.drawLine(x+3, y+3, x+4, y+3);
         g.drawLine(x+11, y+3, x+12, y+3);

         g.setColor(Color.GREEN);
         g.drawLine(x+7, y+3, x+8, y+3);
         g.drawLine(x+15, y+3, x+16, y+3);
    }    
    
    
    protected void drawSequenceIcon(Graphics g, int x, int y, boolean colorBases) {
         int xoffset=x;
         int yoffset=y+5;
         g.setColor(foregroundcolor);
         Color antialias=getTransparentColor(foregroundcolor,70);
         //g.setFont(smallfont);
         //g.drawString("ACGT", x, y+14);
         // - A (anti-aliasing) -
         g.setColor(antialias);
         g.drawLine(xoffset+0, yoffset, xoffset+3, yoffset);     // top bar
         // - A - 
         g.setColor(foregroundcolor);         
         g.drawLine(xoffset, yoffset+1, xoffset, yoffset+6);     // vertical left
         g.drawLine(xoffset+3, yoffset+1, xoffset+3, yoffset+6); // vertical right
         g.drawLine(xoffset+1, yoffset, xoffset+2, yoffset);     // top bar
         g.drawLine(xoffset+1, yoffset+3, xoffset+2, yoffset+3); // middle bar
             
         // - C (anti-aliasing) -
         g.setColor(antialias);
         g.drawLine(xoffset+5, yoffset, xoffset+8, yoffset);      // top line
         g.drawLine(xoffset+5, yoffset+6, xoffset+8, yoffset+6);  // bottom line       
         // - C -
         g.setColor(foregroundcolor);           
         g.drawLine(xoffset+5, yoffset+1, xoffset+5, yoffset+5);  // vertical
         g.drawLine(xoffset+6, yoffset, xoffset+7, yoffset);      // top horizontal
         g.drawLine(xoffset+6, yoffset+6, xoffset+7, yoffset+6);  // bottom horizontal
         g.drawLine(xoffset+8, yoffset+1, xoffset+8, yoffset+1);  // top curve
         g.drawLine(xoffset+8, yoffset+5, xoffset+8, yoffset+5);  // bottom curve
         
         // - G (anti-aliasing) -
         g.setColor(antialias);
         g.drawLine(xoffset+10, yoffset, xoffset+13, yoffset);     // top line              
         g.drawLine(xoffset+10, yoffset+6, xoffset+13, yoffset+6); // bottom line         
         // - G -
         g.setColor(foregroundcolor);           
         g.drawLine(xoffset+10, yoffset+1, xoffset+10, yoffset+5); // vertical
         g.drawLine(xoffset+11, yoffset, xoffset+12, yoffset);     // top horizontal
         g.drawLine(xoffset+11, yoffset+6, xoffset+12, yoffset+6); // bottom horizontal
         g.drawLine(xoffset+13, yoffset+1, xoffset+13, yoffset+1); // top curve
         g.drawLine(xoffset+13, yoffset+5, xoffset+13, yoffset+4); // bottom curve
         g.drawLine(xoffset+12, yoffset+3, xoffset+13, yoffset+3); // middle bar

         // - T -
         g.drawLine(xoffset+17, yoffset, xoffset+17, yoffset+6); // vertical
         g.drawLine(xoffset+15, yoffset, xoffset+19, yoffset);     // top horizontal
                  
         if (colorBases) {
             int yoff=yoffset+9;
             if (settings!=null) g.setColor(settings.getBaseColor('A')); else g.setColor(Color.GREEN);
             g.fillRect(xoffset, yoff, 5, 5);
             if (settings!=null) g.setColor(settings.getBaseColor('C')); else g.setColor(Color.BLUE);
             g.fillRect(xoffset+5, yoff, 5, 5);
             if (settings!=null) g.setColor(settings.getBaseColor('G')); else g.setColor(Color.YELLOW);
             g.fillRect(xoffset+10, yoff, 5, 5);
             if (settings!=null) g.setColor(settings.getBaseColor('T')); else g.setColor(Color.RED);
             g.fillRect(xoffset+15, yoff, 5, 5);
         }
    }
    
    protected void drawRegionIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
//           if (backgroundcolor.equals(Color.black)) g.setColor(Color.white);
//           else g.setColor(Color.black);
//         } else g.setColor(Color.black);
         }
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);   
         int baseline=y+10;
         g.drawLine(x, baseline, x+width, baseline);
         g.setColor(foregroundcolor);
         g.fillRect(x+3, baseline-4, 5, 9);
         g.fillRect(x+11, baseline-4, 7, 9);
    }
    
    protected void drawRegionMulticolorIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);           
         int baseline=y+10;
         final int xoffset=3;
         g.drawLine(x, baseline, x+width, baseline);
         if (backgroundcolor!=null) {
           if (backgroundcolor.equals(Color.black)) g.setColor(Color.white);
           else g.setColor(Color.black);
         } else g.setColor(Color.black);              
         g.fillRect(x+xoffset, baseline-4, 5, 9);
         g.fillRect(x+xoffset+8, baseline-4, 7, 9);
         g.setColor(settings.getColorFromIndex(0));
         g.drawLine(x+xoffset+1, baseline-3, x+xoffset+1, baseline+3);
         g.setColor(settings.getColorFromIndex(6));
         g.drawLine(x+xoffset+2, baseline-3, x+xoffset+2, baseline+3);
         g.setColor(settings.getColorFromIndex(1));
         g.drawLine(x+xoffset+3, baseline-3, x+xoffset+3, baseline+3);

         g.setColor(settings.getColorFromIndex(4));
         g.drawLine(x+xoffset+9, baseline-3, x+xoffset+9, baseline+3);
         g.setColor(settings.getColorFromIndex(3));
         g.drawLine(x+xoffset+10, baseline-3, x+xoffset+10, baseline+3);
         g.setColor(settings.getColorFromIndex(7));
         g.drawLine(x+xoffset+11, baseline-3, x+xoffset+11, baseline+3);
         g.setColor(settings.getColorFromIndex(2));
         g.drawLine(x+xoffset+12, baseline-3, x+xoffset+12, baseline+3);
         g.setColor(settings.getColorFromIndex(5));
         g.drawLine(x+xoffset+13, baseline-3, x+xoffset+13, baseline+3);

    }
    
    protected void drawRegionExpandedIcon(Graphics g, int x, int y, boolean multicolor) {
        y+=3; x++; // offset icon
        if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         if (multicolor) g.setColor(settings.getColorFromIndex(0));
         else g.setColor(foregroundcolor);
         g.drawLine(x, y, x+6, y);
         g.drawLine(x, y+1, x+6, y+1);
          if (multicolor) g.setColor(settings.getColorFromIndex(6));
         g.drawLine(x+10,y, x+15, y);
         g.drawLine(x+10,y+1, x+15, y+1);
          if (multicolor) g.setColor(settings.getColorFromIndex(1));
         g.drawLine(x+3,y+3, x+10, y+3);
         g.drawLine(x+3,y+4, x+10, y+4);
          if (multicolor) g.setColor(settings.getColorFromIndex(3));
         g.drawLine(x+1,y+6, x+5, y+6);
         g.drawLine(x+1,y+7, x+5, y+7);
          if (multicolor) g.setColor(settings.getColorFromIndex(4));
         g.drawLine(x+8,y+6, x+13, y+6);
         g.drawLine(x+8,y+7, x+13, y+7);
          if (multicolor) g.setColor(settings.getColorFromIndex(2));
         g.drawLine(x+2,y+9, x+11, y+9);
         g.drawLine(x+2,y+10, x+11, y+10);
          if (multicolor) g.setColor(settings.getColorFromIndex(5));
         g.drawLine(x+6,y+12, x+16, y+12);
         g.drawLine(x+6,y+13, x+16, y+13);
    }
        
    
    protected void drawRegionGradientIcon(Graphics g, int x, int y, boolean vertical) {    
         Color current=g.getColor();
         if (backgroundcolor!=null) g.setColor(backgroundcolor); else g.setColor(Color.white);
         g.fillRect(x, y, width, height);
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);          
         int baseline=y+10;
         g.drawLine(x, baseline, x+width, baseline);
         int red=foregroundcolor.getRed();
         int blue=foregroundcolor.getBlue();
         int green=foregroundcolor.getGreen();
         Color lightercolor=new Color((int)((255-red)*0.65)+red,(int)((255-green)*0.65)+green,(int)((255-blue)*0.5)+blue);
         if (vertical) {
             ((Graphics2D)g).setPaint(new java.awt.GradientPaint(0, y+4, foregroundcolor, 0, y+(height/2), lightercolor, true));
              g.fillRect(x+3, baseline-4, 5, 9);
              g.fillRect(x+11, baseline-4, 7, 9);
         } else {
              ((Graphics2D)g).setPaint(new java.awt.GradientPaint(x+3, 0, foregroundcolor, x+5, 0, lightercolor, true));
              g.fillRect(x+3, baseline-4, 5, 9);
              ((Graphics2D)g).setPaint(new java.awt.GradientPaint(x+11, 0, foregroundcolor, x+14, 0, lightercolor, true));
              g.fillRect(x+11, baseline-4, 7, 9);             
         }        
         ((Graphics2D)g).setPaint(current);
    }
    
    protected void drawNumericTrackGraphIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         int baseline=y+14;
         g.setColor(foregroundcolor);
         int[] lineheights=new int[]{1,2,1,4,6,5,8,4,8,9,7,5,2,0,-2,-4,-3,1,2,4,1,2};
         for (int i=0;i<width;i++) {
             if (lineheights[i]!=0) g.drawLine(x+i, baseline, x+i, baseline-lineheights[i]);
         }
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);          
         g.drawLine(x, baseline, x+width, baseline);
    }
    
    protected void drawNumericTrackGradientBarGraphIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         int baseline=y+14;
         int[] lineheights=new int[]{1,2,1,4,6,5,8,4,8,9,7,5,2,0,-2,-4,-3,1,2,4,1,2};
         ((Graphics2D)g).setPaint(new java.awt.GradientPaint(x, y+5, foregroundcolor, x, baseline, secondarycolor, true));
         for (int i=0;i<width;i++) {
             if (lineheights[i]>0) g.drawLine(x+i, baseline, x+i, baseline-lineheights[i]);
         }
         ((Graphics2D)g).setPaint(new java.awt.GradientPaint(x, y+baseline, secondarycolor, x, y+height, foregroundcolor, true));
         for (int i=0;i<width;i++) {
             if (lineheights[i]<0) g.drawLine(x+i, baseline, x+i, baseline-lineheights[i]);
         }         
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);          
         g.drawLine(x, baseline, x+width, baseline);
    }    
    
    
    protected void drawNumericTrackDNAGraphIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         Color[] colors;
         if (settings==null) colors=new Color[]{new Color(0,230,0),Color.blue,new Color(255,205,0),Color.red};
         else colors=settings.getBaseColors();
         int baseline=y+14;
         int[] lineheights=new int[]{1,2,1,4,6,5,8,4,8,9,7,5,2,0,-2,-4,-3,1,2,4,1,2};
         int[] col=new int[]{0,1,2,3,2,3,0,0,1,2,3,2,1,3,1,2,0,1,3,2,1,2,0,1,2,1,3,2,1};
         for (int i=0;i<width;i++) {
             g.setColor(colors[col[i]]);
             if (lineheights[i]!=0) g.drawLine(x+i, baseline, x+i, baseline-lineheights[i]);
         }
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);          
         g.drawLine(x, baseline, x+width, baseline);
    }            
    
    
    protected void drawNumericTrackLineGraphIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         int baseline=y+14;
         g.setColor(foregroundcolor);
         int[] lineheights= new int[]{1,2,1,4,6,5,8,4,8,9,7,5,2,0,-2,-4,-3,1,2,4,1,2};
         int[] startheights=new int[]{0,0,0,0,4,5,4,3,4,7,5,1,0,0, 0,-2,-0,1,2,1,1,2};         
         for (int i=0;i<width;i++) {
             if (lineheights[i]!=0) g.drawLine(x+i, baseline-startheights[i], x+i, baseline-lineheights[i]);
         }
//         if (backgroundcolor!=null && backgroundcolor.equals(Color.black)) g.setColor(Color.white);
//         else g.setColor(Color.black);
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);         
         g.drawLine(x, baseline, x+width, baseline);
    }    
 
    protected void drawNumericTrackOutlinedGraphIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         int baseline=y+14;
         int[] lineheights= new int[]{1,2,1,4,6,5,8,4,8,9,7,5,2,0,-2,-4,-3,1,2,4,1,2};
         int[] startheights=new int[]{0,0,0,0,4,5,4,3,4,7,5,1,0,0, 0,-2,-0,1,2,1,1,2};  
         g.setColor(foregroundcolor);         
         for (int i=0;i<width;i++) {
             if (lineheights[i]!=0) g.drawLine(x+i, baseline, x+i, baseline-lineheights[i]);
         }
         g.setColor(secondarycolor);  
         for (int i=0;i<width;i++) {
             if (lineheights[i]!=0) g.drawLine(x+i, baseline-startheights[i], x+i, baseline-lineheights[i]);
         }        
//         if (backgroundcolor!=null && backgroundcolor.equals(Color.black)) g.setColor(Color.white);
//         else g.setColor(Color.black);
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);          
         g.drawLine(x, baseline, x+width, baseline);
    }    
    
    protected void drawNumericTrackGradientIcon(Graphics g, int x, int y) {
         g.setColor(backgroundcolor);
         g.fillRect(x, y, width, height);    
         int baseline=y+10;
         g.setColor(foregroundcolor);
         float[] rgb = foregroundcolor.getRGBComponents(null);
         Color gradients[]=new Color[6];
         gradients[0]=new Color(rgb[0],rgb[1],rgb[2],0.0f);  
         gradients[1]=new Color(rgb[0],rgb[1],rgb[2],0.2f);
         gradients[2]=new Color(rgb[0],rgb[1],rgb[2],0.4f);
         gradients[3]=new Color(rgb[0],rgb[1],rgb[2],0.6f);
         gradients[4]=new Color(rgb[0],rgb[1],rgb[2],0.8f);
         gradients[5]=foregroundcolor;
         int[] use=new int[]{0,3,2,1,3,5,3,1,2,1,5,4,3,2,4,0,3,0,1,2,4,2,3,5,0,0,0,0};
         int[] ori=new int[]{0,0,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0,1,1,1,0,0,0,0,0,0,0};
         for (int i=0;i<width;i++) {
             g.setColor(gradients[use[i]]);
             if (ori[i]==0) g.drawLine(x+i, baseline-6, x+i, baseline);
             else g.drawLine(x+i, baseline, x+i, baseline+6);
         }
//         if (backgroundcolor.equals(Color.black)) g.setColor(Color.white);
//         else g.setColor(Color.black);
         if (baselinecolor.equals(Color.lightGray)) g.setColor(Color.BLACK); 
         else g.setColor(baselinecolor);         
         g.drawLine(x, baseline, x+width, baseline);
    }

    protected void drawNumericTrackRainbowHeatmapIcon(Graphics g, int x, int y) {
         g.setColor(backgroundcolor);
         g.fillRect(x, y, width, height);    
         int baseline=y+10;
         if (settings!=null) {
             ColorGradient gradient=settings.getRainbowGradient();
              for (int i=0;i<width;i++) {
                 g.setColor(gradient.getColor((double)i/(double)width));
                 g.drawLine(x+i, baseline-6, x+i, baseline+6);
             }            
         } else {
             Color gradients[]=new Color[6];
             gradients[0]=Color.getHSBColor(0.6f, 1f, 1f);
             gradients[1]=Color.getHSBColor(0.5f, 1f, 1f);
             gradients[2]=Color.getHSBColor(0.55f, 1f, 1f);
             gradients[3]=Color.getHSBColor(0.42f, 1f, 1f);
             gradients[4]=Color.getHSBColor(0.21f, 1f, 1f);
             gradients[5]=Color.getHSBColor(0.1f, 1f, 1f);
             int[] use=new int[]{0,1,2,1,1,2,2,1,3,4,4,3,3,2,1,0,0,1,2,1,2,2,3,5,0,0,0,0};
             for (int i=0;i<width;i++) {
                 g.setColor(gradients[use[i]]);
                 g.drawLine(x+i, baseline-6, x+i, baseline+6);
             }
         }
    }   
    
     protected void drawNumericTrack2colorHeatmapIcon(Graphics g, int x, int y) {
         g.setColor(backgroundcolor);
         g.fillRect(x, y, width, height);    
         int baseline=y+10;
         if (settings!=null && gradient!=null && secondarygradient!=null) {
              for (int i=0;i<(int)(width/2);i++) {
                 g.setColor(secondarygradient.getColor((double)((width/2)-i)/(double)(width/2)));
                 g.drawLine(x+i, baseline-6, x+i, baseline+6);
              } 
              for (int i=0;i<(int)(width/2);i++) {
                 g.setColor(gradient.getColor((double)i/(double)(width/2)));
                 g.drawLine(x+i+((int)(width/2))+1, baseline-6, x+i+((int)(width/2))+1, baseline+6);
              }                 
         } else { // no settings ?! :|
             Color gradients[]=new Color[6];
             gradients[0]=Color.getHSBColor(0.6f, 1f, 1f);
             gradients[1]=Color.getHSBColor(0.5f, 1f, 1f);
             gradients[2]=Color.getHSBColor(0.55f, 1f, 1f);
             gradients[3]=Color.getHSBColor(0.42f, 1f, 1f);
             gradients[4]=Color.getHSBColor(0.21f, 1f, 1f);
             gradients[5]=Color.getHSBColor(0.1f, 1f, 1f);
             int[] use=new int[]{0,1,2,1,1,2,2,1,3,4,4,3,3,2,1,0,0,1,2,1,2,2,3,5,0,0,0,0};
             for (int i=0;i<width;i++) {
                 g.setColor(gradients[use[i]]);
                 g.drawLine(x+i, baseline-6, x+i, baseline+6);
             }
         }
    }       
    
    protected void drawHiddenIcon(Graphics g, int x, int y) {
         //g.setColor(new Color(0.8f,0.8f,0.8f));
         g.setColor(new Color(0.9f,0.9f,0.9f));         
         g.fillRect(x, y, width, height);  
         g.setColor(Color.white);
         g.drawLine(x, y, x+width, y+height);
         g.drawLine(x, y+height, x+width, y);
         
         g.drawLine(x+1, y, x+width, y+height-1);
         g.drawLine(x+1, y+height, x+width, y+1);
         
         g.drawLine(x, y+1, x+width-1, y+height);
         g.drawLine(x, y+height-1, x+width-1, y);
    }
    
    
    protected void drawSequenceCollectionIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         g.setColor(Color.BLACK);
         g.drawRect(x, y+2, 17, 2);  
         g.drawRect(x, y+6, 11, 2);  
         g.drawRect(x, y+10, 15, 2);  
         g.drawRect(x, y+14, 13, 2);  
         
         g.setColor(new Color(50,50,255)); // light Blue
         g.drawLine(x+1, y+3, x+16, y+3);
         g.drawLine(x+1, y+7, x+10, y+7);
         g.drawLine(x+1, y+11, x+14, y+11);
         g.drawLine(x+1, y+15, x+12, y+15);

         g.setColor(Color.RED);
         g.drawLine(x+1, y+3, x+2, y+3);
         g.drawLine(x+13, y+3, x+14, y+3);
         g.drawLine(x+5, y+7, x+6, y+7);         
         g.drawLine(x+3, y+11, x+4, y+11);
         g.drawLine(x+11, y+11, x+12, y+11);         
         g.drawLine(x+3, y+15, x+4, y+15);
         g.drawLine(x+9, y+15, x+10, y+15);
         
         g.setColor(Color.YELLOW);
         g.drawLine(x+3, y+3, x+4, y+3);
         g.drawLine(x+11, y+3, x+12, y+3);
         g.drawLine(x+1, y+7, x+2, y+7);         
         g.drawLine(x+9, y+7, x+10, y+7);
         g.drawLine(x+7, y+11, x+8, y+11);         
         g.drawLine(x+11, y+15, x+12, y+15);

         g.setColor(Color.GREEN);
         g.drawLine(x+7, y+3, x+8, y+3);
         g.drawLine(x+15, y+3, x+16, y+3);
         g.drawLine(x+3, y+7, x+4, y+7);         
         g.drawLine(x+5, y+11, x+6, y+11);
         g.drawLine(x+13, y+11, x+14, y+11);         
         g.drawLine(x+1, y+15, x+2, y+15);
         g.drawLine(x+7, y+15, x+8, y+15);

    }
    
    protected void drawSequencePartitionIcon(Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         g.setColor(Color.BLACK);
         g.drawRect(x, y+2, 17, 2);  
         g.drawRect(x, y+6, 11, 2);  
         g.drawRect(x, y+10, 15, 2);  
         g.drawRect(x, y+14, 13, 2);  

         g.setColor(Color.RED);
         g.drawLine(x+1, y+3, x+16, y+3);
         g.drawLine(x+1, y+7, x+10, y+7);
         
         g.setColor(Color.GREEN);
         g.drawLine(x+1, y+11, x+14, y+11);
         g.drawLine(x+1, y+15, x+12, y+15);
  
    }    
    
    protected void drawMotifPartitionIcon(Component c, Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         motifIcon.paintIcon(c, g, x, y);
         drawLetterP(g, x+12, y+9);         
    }

    protected void drawModulePartitionIcon(Component c, Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         moduleIcon.paintIcon(c, g, x, y);
         drawLetterP(g, x+12, y+9);         
    }
  
    protected void drawMotifCollectionIcon(Component c, Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         motifIcon.paintIcon(c, g, x, y);
         drawLetterC(g, x+12, y+9);    
    }

    protected void drawModuleCollectionIcon(Component c, Graphics g, int x, int y) {
         if (backgroundcolor!=null) {
           g.setColor(backgroundcolor);
           g.fillRect(x, y, width, height);
         }
         moduleIcon.paintIcon(c, g, x, y);
         drawLetterC(g, x+12, y+9);
    }    
    
    protected void drawLetterP(Graphics g, int x, int y) {
        g.setColor(Color.WHITE);
        g.fillRect(x,y,7,8);
        g.setColor(Color.BLACK);
        g.drawRect(x,y,7,8);
        g.drawLine(x+2,y+2,x+2,y+6); // vertical
        g.drawLine(x+2,y+2,x+4,y+2); // upper
        g.drawLine(x+2,y+4,x+4,y+4); // lower
        g.drawLine(x+5, y+3, x+5, y+3); // "loop"       
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(x+5, y+2, x+5, y+2); // "aliasing"
        g.drawLine(x+5, y+4, x+5, y+4); // "aliasing"
        g.drawLine(x+4, y+3, x+4, y+3); // "aliasing"       
    }
    protected void drawLetterC(Graphics g, int x, int y) {
        g.setColor(Color.WHITE);
        g.fillRect(x,y,7,8);
        g.setColor(Color.BLACK);
        g.drawRect(x,y,7,8);
        g.drawLine(x+2,y+3,x+2,y+5); // vertical
        g.drawLine(x+3,y+2,x+5,y+2); // upper
        g.drawLine(x+3,y+6,x+5,y+6); // lower     
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(x+2, y+2, x+2, y+2); // "aliasing"
        g.drawLine(x+2, y+6, x+2, y+6); // "aliasing"
        g.drawLine(x+5, y+3, x+5, y+3); // "aliasing"       
        g.drawLine(x+5, y+5, x+5, y+5); // "aliasing"       
    }    
    
    protected void drawColorIcon(Graphics g, int x, int y) {
         g.setColor((foregroundcolor!=null)?foregroundcolor:new Color(0, 0, 0, 0));
         g.fillRect(x, y, width, height);  
         if (foregroundcolor==null || foregroundcolor.getAlpha()==0) { // completely transparent color. Draw an X instead as an indication of "no color"
             g.setColor(Color.DARK_GRAY);
             g.drawLine(x, y, x+width, y+height);
             g.drawLine(x+width, y, x, y+height);             
         }
         // 3D effect
         if (is3D && foregroundcolor!=null) {
             int offset=(drawborder==NO_BORDER)?0:1;
             g.setColor(foregroundcolor.darker());
             g.drawLine(x+width-offset, y+offset, x+width-offset, y+height-offset);
             g.drawLine(x+offset, y+height-offset, x+width-offset, y+height-offset);         
             g.setColor(foregroundcolor.equals(Color.BLACK)?new Color(106,106,106):Graph.brighter(foregroundcolor));
             g.drawLine(x+offset, y+offset, x+width-offset, y+offset);
             g.drawLine(x+offset, y+offset, x+offset, y+height-offset);
         } 
    }
    
    protected void drawColorSelectedIcon(Graphics g, int x, int y) {
         g.setColor((foregroundcolor!=null)?foregroundcolor:new Color(0, 0, 0, 0));
         g.fillRect(x, y, width, height);  
         // 3D effect
         if (is3D && foregroundcolor!=null) {
             int offset=(drawborder==NO_BORDER)?0:1;
             g.setColor(foregroundcolor.darker());
             g.drawLine(x+width-offset, y+offset, x+width-offset, y+height-offset);
             g.drawLine(x+offset, y+height-offset, x+width-offset, y+height-offset);         
             g.setColor(foregroundcolor.brighter());
             g.drawLine(x+offset, y+offset, x+width-offset, y+offset);
             g.drawLine(x+offset, y+offset, x+offset, y+height-offset);
         }          
         // check the box with an X
         g.setColor(Color.BLACK);
         g.drawLine(x, y, x+width, y+height);  
         g.drawLine(x+width,y, x, y+height);  
    }
    
    
    protected void drawUnknownIcon(Graphics g, int x, int y) {
         Font f=g.getFont();
         g.setColor(Color.BLACK);
         //g.setFont(largeFont);
         g.drawString("?", x+5, y+height-4); 
         g.setFont(f);
    }
   
    protected Color getTransparentColor(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
    
 
    
    // To draw the border inside the icon's area, the offset should be set to -1. If offset is 0, the right and bottom borders will be 1px outside 
    protected void drawBevelIconBorder(Graphics g, int x, int y, int offset) {
         g.setColor(Color.darkGray);                 
         g.drawLine(x, y+height+offset, x+width+offset, y+height+offset);
         g.drawLine(x+width+offset, y, x+width+offset, y+height+offset);
         
         g.setColor(Color.lightGray);     
         g.drawLine(x, y, x+width+offset, y);
         g.drawLine(x, y, x, y+height+offset);
    }
    
    // To draw the border inside the icon's area, the offset should be set to -1. If offset is 0, the right and bottom borders will be 1px outside
    protected void drawSimpleIconBorder(Graphics g, int x, int y, int offset) {
         g.setColor(bordercolor);                 
         g.drawRect(x, y, width+offset, height+offset);
    }
    
    public static int getIconTypeForDataType(String datatype) {
             if (datatype==null) return UNKNOWN_ICON;
        else if (datatype.equals(Sequence.getType())) return SEQUENCE_ICON;
        else if (datatype.equals(SequenceCollection.getType())) return SEQUENCE_COLLECTION_ICON;
        else if (datatype.equals(SequencePartition.getType())) return SEQUENCE_PARTITION_ICON;
        else if (datatype.equals(DNASequenceDataset.getType())) return SEQUENCE_ICON_BASES;
        else if (datatype.equals(NumericDataset.getType())) return NUMERIC_TRACK_GRAPH_ICON;
        else if (datatype.equals(RegionDataset.getType())) return REGION_ICON;
        else if (datatype.equals(NumericVariable.getType())) return NUMERIC_VARIABLE_ICON;
        else if (datatype.equals(MotifNumericMap.getType())) return MOTIF_NUMERIC_MAP_ICON;
        else if (datatype.equals(ModuleNumericMap.getType())) return MODULE_NUMERIC_MAP_ICON;
        else if (datatype.equals(SequenceNumericMap.getType())) return SEQUENCE_NUMERIC_MAP_ICON;
        else if (datatype.equals(MotifTextMap.getType())) return MOTIF_TEXT_MAP_ICON;
        else if (datatype.equals(ModuleTextMap.getType())) return MODULE_TEXT_MAP_ICON;
        else if (datatype.equals(SequenceTextMap.getType())) return SEQUENCE_TEXT_MAP_ICON;        
        else if (datatype.equals(BackgroundModel.getType())) return MARKOV_MODEL_ICON;
        else if (datatype.equals(ExpressionProfile.getType())) return EXPRESSION_PROFILE_ICON;
        else if (datatype.equals(PriorsGenerator.getType())) return PRIORS_GENERATOR_ICON;
        else if (datatype.equals(TextVariable.getType())) return TEXT_VARIABLE_ICON;
        else if (datatype.equals(Motif.getType())) return MOTIF_ICON;
        else if (datatype.equals(MotifCollection.getType())) return MOTIF_COLLECTION_ICON;
        else if (datatype.equals(MotifPartition.getType())) return MOTIF_PARTITION_ICON;
        else if (datatype.equals(ModuleCRM.getType())) return MODULE_ICON;       
        else if (datatype.equals(ModulePartition.getType())) return MODULE_PARTITION_ICON;      
        else if (datatype.equals(ModuleCollection.getType())) return MODULE_COLLECTION_ICON;  
        else if (datatype.equals(OutputData.getType())) return OUTPUT_DATA_ICON;          
        else if (datatype.equals(Analysis.getType())) return ANALYSIS_ICON;          
        else return UNKNOWN_ICON;
    }
    
    public static int getIconTypeForData(Data data) {
             if (data instanceof Sequence) return SEQUENCE_ICON;
        else if (data instanceof SequenceCollection) return SEQUENCE_COLLECTION_ICON;
        else if (data instanceof SequencePartition) return SEQUENCE_PARTITION_ICON;
        else if (data instanceof DNASequenceDataset) return SEQUENCE_ICON_BASES;
        else if (data instanceof NumericDataset) return NUMERIC_TRACK_GRAPH_ICON;
        else if (data instanceof RegionDataset) return REGION_ICON;
        else if (data instanceof NumericVariable) return NUMERIC_VARIABLE_ICON;
        else if (data instanceof MotifNumericMap) return MOTIF_NUMERIC_MAP_ICON;
        else if (data instanceof ModuleNumericMap) return MODULE_NUMERIC_MAP_ICON;
        else if (data instanceof SequenceNumericMap) return SEQUENCE_NUMERIC_MAP_ICON;
        else if (data instanceof MotifTextMap) return MOTIF_TEXT_MAP_ICON;
        else if (data instanceof ModuleTextMap) return MODULE_TEXT_MAP_ICON;
        else if (data instanceof SequenceTextMap) return SEQUENCE_TEXT_MAP_ICON;        
        else if (data instanceof BackgroundModel) return MARKOV_MODEL_ICON;
        else if (data instanceof ExpressionProfile) return EXPRESSION_PROFILE_ICON;
        else if (data instanceof PriorsGenerator) return PRIORS_GENERATOR_ICON;
        else if (data instanceof TextVariable) return TEXT_VARIABLE_ICON;
        else if (data instanceof Motif) return MOTIF_ICON;
        else if (data instanceof MotifCollection) return MOTIF_COLLECTION_ICON;
        else if (data instanceof MotifPartition) return MOTIF_PARTITION_ICON;
        else if (data instanceof ModuleCRM) return MODULE_ICON;
        else if (data instanceof ModuleCollection) return MODULE_COLLECTION_ICON;        
        else if (data instanceof ModulePartition) return MODULE_PARTITION_ICON;
        else if (data instanceof OutputData) return OUTPUT_DATA_ICON;
        else if (data instanceof Analysis) return ANALYSIS_ICON;        
        else return UNKNOWN_ICON;
    }  
    
    public static int getIconTypeForDataClass(Class type) {
             if (type==null) return UNKNOWN_ICON;
        else if (type==Sequence.class) return SEQUENCE_ICON;
        else if (type==SequenceCollection.class) return SEQUENCE_COLLECTION_ICON;
        else if (type==SequencePartition.class) return SEQUENCE_PARTITION_ICON;
        else if (type==DNASequenceDataset.class) return SEQUENCE_ICON_BASES;
        else if (type==NumericDataset.class) return NUMERIC_TRACK_GRAPH_ICON;
        else if (type==RegionDataset.class) return REGION_ICON;
        else if (type==NumericVariable.class) return NUMERIC_VARIABLE_ICON;
        else if (type==MotifNumericMap.class) return MOTIF_NUMERIC_MAP_ICON;
        else if (type==ModuleNumericMap.class) return MODULE_NUMERIC_MAP_ICON;
        else if (type==SequenceNumericMap.class) return SEQUENCE_NUMERIC_MAP_ICON;
        else if (type==MotifTextMap.class) return MOTIF_TEXT_MAP_ICON;
        else if (type==ModuleTextMap.class) return MODULE_TEXT_MAP_ICON;
        else if (type==SequenceTextMap.class) return SEQUENCE_TEXT_MAP_ICON;        
        else if (type==BackgroundModel.class) return MARKOV_MODEL_ICON;
        else if (type==ExpressionProfile.class) return EXPRESSION_PROFILE_ICON;
        else if (type==PriorsGenerator.class) return PRIORS_GENERATOR_ICON;
        else if (type==TextVariable.class) return TEXT_VARIABLE_ICON;
        else if (type==Motif.class) return MOTIF_ICON;
        else if (type==MotifCollection.class) return MOTIF_COLLECTION_ICON;
        else if (type==MotifPartition.class) return MOTIF_PARTITION_ICON;
        else if (type==ModuleCRM.class) return MODULE_ICON;
        else if (type==ModuleCollection.class) return MODULE_COLLECTION_ICON;        
        else if (type==ModulePartition.class) return MODULE_PARTITION_ICON;
        else if (type==OutputData.class) return OUTPUT_DATA_ICON;
        else if (type==Analysis.class) return ANALYSIS_ICON;        
        else return UNKNOWN_ICON;
    }     
    
    public static Icon getIconForData(Data data) {
        SimpleDataPanelIcon icon = new SimpleDataPanelIcon(20, 20, getIconTypeForData(data), null);   
        if (icon.type==REGION_ICON) icon.setForegroundColor(Color.GREEN);
        else if (icon.type==NUMERIC_TRACK_GRAPH_ICON) icon.setForegroundColor(Color.BLUE);
        return icon;
    }
    
    public static Icon getIconForDataType(String datatype) {
        SimpleDataPanelIcon icon = new SimpleDataPanelIcon(20, 20, getIconTypeForDataType(datatype), null);        
        if (icon.type==REGION_ICON) icon.setForegroundColor(Color.GREEN);
        else if (icon.type==NUMERIC_TRACK_GRAPH_ICON) icon.setForegroundColor(Color.BLUE);
        return icon;    
    }  
    
    public static Icon getIconForDataClass(Class datatype) {
        SimpleDataPanelIcon icon = new SimpleDataPanelIcon(20, 20, getIconTypeForDataClass(datatype), null);        
        if (icon.type==REGION_ICON) icon.setForegroundColor(Color.GREEN);
        else if (icon.type==NUMERIC_TRACK_GRAPH_ICON) icon.setForegroundColor(Color.BLUE);
        return icon;    
    }     

}