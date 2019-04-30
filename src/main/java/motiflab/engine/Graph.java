/*
 
 
 */

package motiflab.engine;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import motiflab.engine.protocol.ParseError;
import motiflab.gui.VisualizationSettings;

/**
 * This class represents a library for easy construction of graphs and graphical charts
 *
 * @author Kjetil
 */
public class Graph {
    // grid settings
    public static final int NONE=0;
    public static final int SOLID=1;
    public static final int DASHED=2;
    public static final int DOTTED=3;
    public static final int DASHED_LONG=4; 
    // axis layout options
    public static final int BOX=0;
    public static final int CROSS=1;

    private Graphics2D g;
    double minX=0;
    double maxX=0;
    double minY=0;
    double maxY=0;

    int graphwidth=0; // the width of the graph in pixels
    int graphheight=0; // the height of the graph in pixels

    int translateX=0; // a translation offset for the top-left corner of the graph-box (axes)
    int translateY=0; // a translation offset for the top-left corner of the graph-box (axes)
    
    public static Color lightGray=new Color(230,230,230);
    public static final Locale locale=new Locale("EN");

    public static Stroke DASHED_STROKE=new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f,3f}, 0f);
    public static Stroke DASHED_LONG_STROKE=new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f,2f}, 0f);
    public static Stroke DOTTED_STROKE=new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f}, 0f);
    public static Stroke SMALL_DOTTED_STROKE=new BasicStroke(1f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{1f,1f}, 0f);
    public static Stroke LINE_STROKE=new BasicStroke(1f);

    private static final NumberFormat normalNumberFormat=NumberFormat.getInstance(locale);
    private static final NumberFormat percentageNumberFormat=NumberFormat.getPercentInstance(locale);
    private static final NumberFormat scientificNumberFormat=new DecimalFormat("0.#####E0");

    public int labelpaddingX=20; // min distance between tick labels in X-direction    
    public int labelpaddingY=2; // min distance between tick labels in Y-direction    
    
    static {
        normalNumberFormat.setGroupingUsed(false);
        percentageNumberFormat.setGroupingUsed(false);
        scientificNumberFormat.setGroupingUsed(false);
        normalNumberFormat.setMaximumFractionDigits(5);
        percentageNumberFormat.setMaximumFractionDigits(5);
        scientificNumberFormat.setMaximumFractionDigits(5);

    }

    public Graph(Graphics2D graphics, double minX, double maxX, double minY, double maxY, int graphwidth, int graphheight, int translateX, int translateY) {
        g=graphics;
        this.minX=minX;
        this.maxX=maxX;
        this.minY=minY;
        this.maxY=maxY;
        this.graphwidth=graphwidth;
        this.graphheight=graphheight;
        this.translateX=translateX;
        this.translateY=translateY;
    }

    /** Sets the target graphics for this Graph */
    public void setGraphics(Graphics2D graphics) {
       g=graphics; 
    }
    
    /**
     * Sets new limits for this Graph. Note that an existing graph must be completely redrawn in order to update to the new limits
     * @param minX
     * @param maxX
     * @param minY
     * @param maxY
     */
    public void setLimits(double minX, double maxX, double minY, double maxY) {
        this.minX=minX;
        this.maxX=maxX;
        this.minY=minY;
        this.maxY=maxY;
    }
    /**
     * Sets a new size for this Graph (in pixels). Note that an existing graph must be completely redrawn in order to update to the new size
     * @param width
     * @param height
     */
    public void setSize(int width, int height) {
        this.graphwidth=width;
        this.graphheight=height;
    }
    /**
     * Sets a new translation coordinate for this Graph. The translation coordinate marks the top left corner of the graph box. Note that an existing graph must be completely redrawn in order to update to the new size
     * @param x
     * @param y
     */
    public void setGraphTranslate(int x, int y) {
        this.translateX=x;
        this.translateY=y;
    }

    /**
     * Draws an axes system (X and Y axis) for this graph
     * @param axeslayout A number specifying the layout of the axes.
     *        BOX: the axes (x and y axes with ticks) will be drawn "outside" the box like an L (to the left and at the bottom)
     *        CROSS: the axes (x and y axes with ticks) be drawn at origo like a cross
     * @param gridlines A number specifying the type of gridlines to use. NONE, SOLID, DASHED or DOTTED
     * @param boundingBox Draw a box around the whole graph
     *
     */
    public void drawAxes(int axeslayout, int gridlines, boolean boundingBox) {
        drawAxes(axeslayout, gridlines, gridlines, boundingBox, true, true, false, false);
    }

    /**
     * Draws an axes system (X and Y axis) for this graph
     * @param axeslayout A number specifying the layout of the axes.
     *        BOX: the axes (x and y axes with ticks) will be drawn "outside" the box like an L (to the left and at the bottom)
     *        CROSS: the axes (x and y axes with ticks) be drawn at origo like a cross
     * @param gridlines A number specifying the type of gridlines to use. NONE, SOLID, DASHED or DOTTED
     * @param boundingBox Draw a box around the whole graph
     * @param drawTicks Decides if tick marks should be drawn or not
     * @param xAxisIsPercentages A flag to indicate whether the ticks on the X axis represent percentages
     * @param yAxisIsPercentages A flag to indicate whether the ticks on the Y axis represent percentages
     *
     */
    public void drawAxes(int axeslayout, int gridlines, boolean boundingBox,  boolean drawTicks, boolean xAxisIsPercentages, boolean yAxisIsPercentages) {
         Stroke gridlinestroke=null;
         Stroke defaultStroke=g.getStroke();
         if (gridlines==DASHED) {
             gridlinestroke=DASHED_STROKE;
         } else if (gridlines==DASHED_LONG) {
             gridlinestroke=DASHED_LONG_STROKE;
         } else if (gridlines==DOTTED) {
             gridlinestroke=DOTTED_STROKE;
         } else if (gridlines==SOLID) {
             gridlinestroke=defaultStroke;
         }
         if (gridlinestroke!=null) {
             g.setStroke(gridlinestroke);
             g.setColor(lightGray);
             drawHorizontalGridLines();
             drawVerticalGridLines();
             g.setStroke(defaultStroke);             
         }
         
         g.setColor(Color.BLACK);
         if (drawTicks) {
             int xAxisPos=(axeslayout==CROSS)?getYforValue(0):translateY+graphheight;
             int yAxisPos=(axeslayout==CROSS)?getXforValue(0):translateX;
             drawXaxisWithTicks(xAxisPos,(axeslayout==CROSS), xAxisIsPercentages);
             drawYaxisWithTicks(yAxisPos,(axeslayout==CROSS), yAxisIsPercentages);
         }
         if (boundingBox) {
             g.drawRect(translateX, translateY, graphwidth, graphheight);
         }       
    }

    /**
     * Draws an axes system (X and Y axis) for this graph
     * @param axeslayout A number specifying the layout of the axes.
     *        BOX:   the axes (x and y axes with ticks) will be drawn "outside" the box like an L (to the left and at the bottom)
     *        CROSS: the axes (x and y axes with ticks) be drawn at origo like a cross
     * @param horizontalGridlines A number specifying the type of gridlines to use in horizontal direction. NONE, SOLID, DASHED or DOTTED
     * @param verticalGridlines   A number specifying the type of gridlines to use in vertical direction. NONE, SOLID, DASHED or DOTTED
     * @param boundingBox Draw a box around the whole graph
     * @param drawTicksX Decides if tick marks should be drawn or not on the X-axis
     * @param drawTicksY Decides if tick marks should be drawn or not on the Y-axis
     * @param xAxisIsPercentages A flag to indicate whether the ticks on the X axis represent percentages
     * @param yAxisIsPercentages A flag to indicate whether the ticks on the Y axis represent percentages
     *
     */
    public void drawAxes(int axeslayout, int horizontalGridlines, int verticalGridlines, boolean boundingBox,  boolean drawTicksX, boolean drawTicksY, boolean xAxisIsPercentages, boolean yAxisIsPercentages) {          
         Stroke gridlinestroke=null;
         Stroke defaultStroke=g.getStroke();
         if (horizontalGridlines==DASHED) {
             gridlinestroke=DASHED_STROKE;
         } else if (horizontalGridlines==DASHED_LONG) {
             gridlinestroke=DASHED_LONG_STROKE;
         } else if (horizontalGridlines==DOTTED) {
             gridlinestroke=DOTTED_STROKE;
         } else if (horizontalGridlines==SOLID) {
             gridlinestroke=defaultStroke;
         }         
         if (gridlinestroke!=null) {
             g.setStroke(gridlinestroke);
             g.setColor(lightGray);
             drawHorizontalGridLines();
             g.setStroke(defaultStroke);
         } 
         gridlinestroke=null;
         if (verticalGridlines==DASHED) {
             gridlinestroke=DASHED_STROKE;
         } else if (verticalGridlines==DASHED_LONG) {
             gridlinestroke=DASHED_LONG_STROKE;
         } else if (verticalGridlines==DOTTED) {
             gridlinestroke=DOTTED_STROKE;
         } else if (verticalGridlines==SOLID) {
             gridlinestroke=defaultStroke;
         }          
         if (gridlinestroke!=null) {
             g.setStroke(gridlinestroke);
             g.setColor(lightGray);
             drawVerticalGridLines();
             g.setStroke(defaultStroke);
         }
         g.setColor(Color.BLACK);       
         if (drawTicksX) {
             int xAxisPos=(axeslayout==CROSS)?getYforValue(0):translateY+graphheight;
             drawXaxisWithTicks(xAxisPos,(axeslayout==CROSS), xAxisIsPercentages);
         }       
         if (drawTicksY) {
             int yAxisPos=(axeslayout==CROSS)?getXforValue(0):translateX;
             drawYaxisWithTicks(yAxisPos,(axeslayout==CROSS), yAxisIsPercentages);
         }
         if (boundingBox) {
             g.drawRect(translateX, translateY, graphwidth, graphheight);
         }
    }

    /** Draws a box around the graph in the current color and stroke */
    public void drawBoundingBox() {
        g.drawRect(translateX, translateY, graphwidth, graphheight);
    }
    
    /** Fills the background of the graph with the specified color
     *  @param background The color to use for the background
     */
    public void drawBackground(Color background) {
        Color current=g.getColor();
        g.setColor(background);
        g.fillRect(translateX, translateY, graphwidth, graphheight);
        g.setColor(current);
    }    
   
    /**
     * Draws an X-axis with sensible ticks within the range minX to maxX
     * @param translateY The Y-offset where the axis-line should be drawn
     * @param skipZero If TRUE then no label will be drawn for 0
     */
    public void drawXaxisWithTicks(int positionY, boolean skipZero, boolean formatAsPercentages) {
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        if (maxX==minX) maxX=minX+Math.abs((minX*0.1)); // set minX to 10% higher value if range==0
        double range=(maxX-minX);
        double tickscaleX=ceilToNearestTenth(range);
        if ((range/tickscaleX>=20) && (range/tickscaleX<=30)) tickscaleX=roundToNearestX(tickscaleX*2, tickscaleX); // check if there are too many ticks
        else if (range/tickscaleX>30) tickscaleX=roundToNearestX(tickscaleX*5, tickscaleX); // check if there are too many ticks
        double tickAnchorX=0;
        if (range==0) tickAnchorX=minX;
        else if((minX < 0 && maxX < 0) || (minX > 0 && maxX > 0)) {
            // 0 is not included in the range. Find some other anchor to start off
            tickAnchorX=ceilToNearestX(minX,tickscaleX);
        } 
        ArrayList<Double> ticksLabels=(range>0)?getXTickLabels(tickAnchorX, tickscaleX, skipZero, formatAsPercentages):new ArrayList<Double>(); // ticks that should have labels
        int tickAnchorXcoordinate=getXforValue(tickAnchorX);       
        g.drawLine(tickAnchorXcoordinate, positionY, tickAnchorXcoordinate, positionY+5);
        int decimals=getDecimalCount(ticksLabels, formatAsPercentages);
        if (shouldDrawTick(tickAnchorX,ticksLabels,tickscaleX)) drawAlignedNumber(tickAnchorX,tickAnchorXcoordinate,positionY+7,0.5f,1f,formatAsPercentages,decimals);
        if (range>0) {
            double lefttick=tickAnchorX-tickscaleX;
            int lefttickXcoordinate=getXforValue(lefttick);
            while (lefttickXcoordinate>=translateX) {
               g.drawLine(lefttickXcoordinate, positionY, lefttickXcoordinate, positionY+5);
               if (shouldDrawTick(lefttick,ticksLabels,tickscaleX)) drawAlignedNumber(lefttick,lefttickXcoordinate,positionY+7,0.5f,1f,formatAsPercentages,decimals);
               lefttick-=tickscaleX;
               lefttickXcoordinate=getXforValue(lefttick);
            }
            double righttick=tickAnchorX+tickscaleX;
            int righttickXcoordinate=getXforValue(righttick);
            while (righttickXcoordinate<=graphwidth+translateX) {
               g.drawLine(righttickXcoordinate, positionY, righttickXcoordinate, positionY+5);
               if (shouldDrawTick(righttick,ticksLabels,tickscaleX)) drawAlignedNumber(righttick,righttickXcoordinate,positionY+7,0.5f,1f,formatAsPercentages,decimals);
               righttick+=tickscaleX;
               righttickXcoordinate=getXforValue(righttick);
            }
        }
        g.drawLine(translateX,positionY,translateX+graphwidth,positionY); // horizontal axis line
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    
       /**
     * Draws a Y-axis with sensible ticks within the range minY to maxY
     * @param positionX The X-offset where the axis-line should be drawn
     * @param skipZero If TRUE then no label will be drawn for 0
     * @param formatAsPercentages If TRUE, a tick value of e.g. 0.284232 will be drawn as 28.42%
     */
    public void drawYaxisWithTicks(int positionX, boolean skipZero, boolean formatAsPercentages) {            
        drawYaxisWithTicks(positionX, skipZero, formatAsPercentages, true, null);
    }

    /**
     * Draws a Y-axis with sensible ticks within the range minY to maxY
     * @param positionX The X-offset where the axis-line should be drawn
     * @param skipZero If TRUE then no label will be drawn for 0
     * @param formatAsPercentages If TRUE, a tick value of e.g. 0.284232 will be drawn as 28.42%
     * @param leftSide If TRUE, the ticks will be drawn to the left of the axis (which is at positionX),
     *                 If FALSE the ticks will be drawn to the right
     * @param Color labelColor The color to use when drawing the tick labels (but not tick marks). If set to NULL, the current color will be used
     */
    public void drawYaxisWithTicks(int positionX, boolean skipZero, boolean formatAsPercentages, boolean leftSide, Color color) {            
        float Xalign=(leftSide)?1.0f:0.f;
        int tickLabelXPos=(leftSide)?(positionX-7):(positionX+7);
        int tickMarkXstart=(leftSide)?positionX-5:positionX+5;
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        if (maxY==minY) maxY=minY+Math.abs((minY*0.1)); // set maxY to 10% higher value if range==0
        double range=(maxY-minY);
        double tickscaleY=ceilToNearestTenth(range);
        if ((range/tickscaleY>=20) && (range/tickscaleY<=30)) tickscaleY=roundToNearestX(tickscaleY*2, tickscaleY); // check if there are too many ticks
        else if (range/tickscaleY>30) tickscaleY=roundToNearestX(tickscaleY*5, tickscaleY); // check if there are too many ticks
        double tickAnchorY=0;
        if (range==0) tickAnchorY=minX;
        else if((minY < 0 && maxY < 0) || (minY > 0 && maxY > 0)) {
            // 0 is not included in the range. Find some other anchor
            tickAnchorY=ceilToNearestX(minY,tickscaleY);
        }
        ArrayList<Double> ticksLabels=(range>0)?getYTickLabels(tickAnchorY, tickscaleY, skipZero, formatAsPercentages):new ArrayList<Double>(); // ticks that should have labels);        
        int tickAnchorYcoordinate=getYforValue(tickAnchorY);
        g.drawLine(tickMarkXstart, tickAnchorYcoordinate, positionX, tickAnchorYcoordinate); // tick mark             
        int decimals=getDecimalCount(ticksLabels, formatAsPercentages); 
        if (shouldDrawTick(tickAnchorY,ticksLabels,tickscaleY)) drawAlignedNumber(tickAnchorY,tickLabelXPos, tickAnchorYcoordinate,Xalign,0.3f,formatAsPercentages,decimals,color);
        if (range>0) {
            double lefttick=tickAnchorY-tickscaleY;
            int lefttickYcoordinate=getYforValue(lefttick);
            while (lefttickYcoordinate<=graphheight+translateY) {
               g.drawLine(tickMarkXstart,lefttickYcoordinate, positionX, lefttickYcoordinate); // tick mark      
               if (shouldDrawTick(lefttick,ticksLabels,tickscaleY)) drawAlignedNumber(lefttick,tickLabelXPos,lefttickYcoordinate,Xalign,0.3f,formatAsPercentages,decimals,color);
               lefttick-=tickscaleY;
               lefttickYcoordinate=getYforValue(lefttick);
            }
            double righttick=tickAnchorY+tickscaleY;
            int righttickYcoordinate=getYforValue(righttick);
            while (righttickYcoordinate>=translateY) {
               g.drawLine(tickMarkXstart, righttickYcoordinate, positionX, righttickYcoordinate); // tick mark      
               if (shouldDrawTick(righttick,ticksLabels,tickscaleY)) drawAlignedNumber(righttick,tickLabelXPos,righttickYcoordinate,Xalign,0.3f,formatAsPercentages,decimals,color);
               righttick+=tickscaleY;
               righttickYcoordinate=getYforValue(righttick);
            }
        }
        g.drawLine(positionX,translateY,positionX,translateY+graphheight); // vertical axis line
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }    
           
  /**
     * Returns an array of values where ticks with labels should be drawn along the X-axis
     * @param skipZero If TRUE then no label will be drawn for 0
     * @param formatAsPercentages whether or not the labels should be formatted as percentage numbers
     */
    public ArrayList<Double> getXTickLabels(double tickAnchorX, double tickscaleX, boolean skipZero, boolean formatAsPercentages) {
        ArrayList<Double> ticks=getTicks(tickAnchorX, tickscaleX, 1, skipZero, minX, maxX);
        if (tickLabelsFitX(ticks, formatAsPercentages)) return ticks; // do the labels fit?
        // labels don't fit. Try something else
        if (tickAnchorX==0) {
            ticks=getTicks(tickAnchorX, tickscaleX, 2, skipZero, minX, maxX); // try with every second tick
            if (tickLabelsFitX(ticks, formatAsPercentages)) return ticks;
            ticks=getTicks(tickAnchorX, tickscaleX, 5, skipZero, minX, maxX); // every 5th ?
            if (tickLabelsFitX(ticks, formatAsPercentages)) return ticks;            
            ticks=getTicks(tickAnchorX, tickscaleX, 10, skipZero, minX, maxX); // every 10th then. This should really be enough
            if (tickLabelsFitX(ticks, formatAsPercentages)) return ticks; 
            ticks=new ArrayList<Double>();
            ticks.add(tickAnchorX);
            return ticks; // it should probably never come to this, but just in case
        } else { // this is more tricky... we might have to select a more sensible anchor than the current
            double newAnchor=((int)(tickAnchorX/(tickscaleX*2)))*(tickscaleX*2);
            ticks=getTicks(newAnchor, tickscaleX, 2, skipZero, minX, maxX); // try with every second tick
            if (tickLabelsFitX(ticks, formatAsPercentages)) return ticks;
            newAnchor=((int)(tickAnchorX/(tickscaleX*5)))*(tickscaleX*5);
            ticks=getTicks(newAnchor, tickscaleX, 5, skipZero, minX, maxX); // every 5th ?
            if (tickLabelsFitX(ticks, formatAsPercentages)) return ticks;            
            newAnchor=((int)(tickAnchorX/(tickscaleX*10)))*(tickscaleX*10);
            ticks=getTicks(newAnchor, tickscaleX, 10, skipZero, minX, maxX); // every 10th then. This should really be enough
            if (tickLabelsFitX(ticks, formatAsPercentages)) return ticks; 
            ticks=new ArrayList<Double>();
            ticks.add(tickAnchorX);
            return ticks; // it should probably never come to this, but just in case
        }               
    }    

  /**
     * Returns an array of values where ticks with labels should be drawn along the Y-axis
     * @param tickAnchorY an anchor value that should correspond to a tick (typically 0 if this is within the range)
     * @param skipZero If TRUE then no label will be drawn for 0
     * @param formatAsPercentages whether or not the labels should be formatted as percentage numbers
     * @param a list of values that should be drawn as tick labels on the Y-axis (not that the values need not be returned in sorted order)
     */
    public ArrayList<Double> getYTickLabels(double tickAnchorY, double tickscaleY, boolean skipZero, boolean formatAsPercentages) {
        ArrayList<Double> ticks=getTicks(tickAnchorY, tickscaleY, 1, skipZero, minY, maxY); 
        if (tickLabelsFitY(ticks, formatAsPercentages)) return ticks; // do the labels fit?
        // labels don't fit. Try something else
        if (tickAnchorY==0) {
            ticks=getTicks(tickAnchorY, tickscaleY, 2, skipZero, minY, maxY); // try with every second tick
            if (tickLabelsFitY(ticks, formatAsPercentages)) return ticks;
            ticks=getTicks(tickAnchorY, tickscaleY, 5, skipZero, minY, maxY); // every 5th ?
            if (tickLabelsFitY(ticks, formatAsPercentages)) return ticks;            
            ticks=getTicks(tickAnchorY, tickscaleY, 10, skipZero, minY, maxY); // every 10th then. This should really be enough
            if (tickLabelsFitY(ticks, formatAsPercentages)) return ticks; 
            ticks=new ArrayList<Double>();
            ticks.add(tickAnchorY); // return just a single tick label corresponding to the anchor
            return ticks; // it should probably never come to this, but just in case
        } else { // this is more tricky... we might have to select a more sensible anchor
            double newAnchor=((int)(tickAnchorY/(tickscaleY*2)))*(tickscaleY*2);
            ticks=getTicks(newAnchor, tickscaleY, 2, skipZero, minY, maxY); // try with every second tick
            if (tickLabelsFitY(ticks, formatAsPercentages)) return ticks;
            newAnchor=((int)(tickAnchorY/(tickscaleY*5)))*(tickscaleY*5);
            ticks=getTicks(newAnchor, tickscaleY, 5, skipZero, minY, maxY); // every 5th ?
            if (tickLabelsFitY(ticks, formatAsPercentages)) return ticks;            
            newAnchor=((int)(tickAnchorY/(tickscaleY*10)))*(tickscaleY*10);            
            ticks=getTicks(newAnchor, tickscaleY, 10, skipZero, minY, maxY); // every 10th then. This should really be enough
            if (tickLabelsFitY(ticks, formatAsPercentages)) return ticks; 
            ticks=new ArrayList<Double>();
            ticks.add(tickAnchorY);
            return ticks; // it should probably never come to this, but just in case
        }               
    }       
    
    /** Returns a list of tick values with the given spacing on either side of the anchor */
    private ArrayList<Double> getTicks(double tickAnchor, double ticksspacing, int step, boolean skipZero, double min, double max) {
        min=min-(ticksspacing*0.1); // this is to allow for some precision problems
        max=max+(ticksspacing*0.1); // this is to allow for some precision problems
        ArrayList<Double> ticks=new ArrayList<Double>();
        if (!skipZero || tickAnchor!=0) ticks.add(tickAnchor);
        double lefttick=tickAnchor;
        lefttick-=(ticksspacing*step); 
        while (lefttick>=min) {
           if (!skipZero || lefttick!=0) ticks.add(lefttick);
           lefttick-=(ticksspacing*step);
        }
        double righttick=tickAnchor;
        righttick+=(ticksspacing*step);
        while (righttick<=max) {
           if (!skipZero || righttick!=0) ticks.add(righttick);
           righttick+=(ticksspacing*step);
        }   
        Collections.sort(ticks);
        return ticks;  
    }
    
    /** Returns TRUE if labels for all ticks in the list can be output without overlapping each other */
    private boolean tickLabelsFitX(ArrayList<Double> ticks, boolean formatAsPercentages) {  
        float[] allowedXrange=new float[]{-Float.MAX_VALUE,Float.MAX_VALUE};
        float[] allowedYrange=new float[]{-Float.MAX_VALUE,Float.MAX_VALUE};        
        int decimals=getDecimalCount(ticks,formatAsPercentages);
        for (Double tick:ticks) {             
           int tickAnchorXcoordinate=getXforValue(tick);
           float[] drawnRange=fitAlignedNumber(tick,tickAnchorXcoordinate,0,0.5f,1f,formatAsPercentages,allowedXrange,allowedYrange,decimals);
           if (drawnRange!=null) {allowedXrange[0]=drawnRange[1]+labelpaddingX;}
           else return false; // did not fit
        }
        return true;
    }
    
    /** Returns TRUE if labels for all ticks in the list can be output without overlapping each other */
    private boolean tickLabelsFitY(ArrayList<Double> ticks, boolean formatAsPercentages) {  
        float[] allowedXrange=new float[]{-Float.MAX_VALUE,Float.MAX_VALUE};
        float[] allowedYrange=new float[]{-Float.MAX_VALUE,Float.MAX_VALUE};        
        int decimals=getDecimalCount(ticks,formatAsPercentages);
        for (Double tick:ticks) {             
           int tickAnchorYcoordinate=getYforValue(tick);
           float[] drawnRange=fitAlignedNumber(tick,0,tickAnchorYcoordinate,0,0.5f,formatAsPercentages,allowedXrange,allowedYrange,decimals);
           if (drawnRange!=null) {allowedYrange[1]=drawnRange[2]-labelpaddingY;}
           else return false; // did not fit
        }
        return true;
    }    
    


    /**
     * Draws a curve that goes through the list of points specified as (x,y)-value
     * the values are graph-values (not pixel coordinates)
     */
    public void drawCurve(double[][] points) {
        if (points==null || points.length<2) return;
        GeneralPath graph=new GeneralPath();
        graph.moveTo(getXforValue(points[0][0]),getYforValue(points[0][1]));
        for (int i=1;i<points.length;i++) {
            graph.lineTo(getXforValue(points[i][0]),getYforValue(points[i][1]));
        }
        g.draw(graph);
    }

    /**
     * Draws a curve that goes through the list of points specified
     * the values are graph-values (not pixel coordinates)
     */
    public void drawCurve(double[] Xpoints, double[] Ypoints) {
        if (Xpoints==null || Xpoints==null ||  Xpoints.length!=Ypoints.length || Xpoints.length<2) return;
        GeneralPath graph=new GeneralPath();
        graph.moveTo(getXforValue(Xpoints[0]),getYforValue(Ypoints[0]));
        for (int i=1;i<Xpoints.length;i++) {
            graph.lineTo(getXforValue(Xpoints[i]),getYforValue(Ypoints[i]));
        }
        g.draw(graph);
    }

    /**
     * Draws a point at the coordinate of the values
     * @param xValue
     * @param yValue
     * @param style 0=circle, 1=filled circle, 2=box, 3=filled box, 4=x-cross, 5= +cross, 6= *cross, 7=pixel (dot)
     * @param a size parameter for the point
     */
    public void drawDataPoint(double xValue, double yValue, int style, int size) {
        if (style>7 || style<0) style=0;
        int x=getXforValue(xValue);
        int y=getYforValue(yValue);
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (!(style==2 || style==3)) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
             if (style==0) {g.drawOval(x-size, y-size, size*2, size*2);}
        else if (style==1) {g.fillOval(x-size, y-size, size*2, size*2);g.drawOval(x-size, y-size, size*2, size*2);}
        else if (style==2) {g.drawRect(x-size, y-size, size*2, size*2);}
        else if (style==3) {g.fillRect(x-size, y-size, size*2, size*2);g.drawRect(x-size, y-size, size*2, size*2);}
        else if (style==4) {g.drawLine(x-size, y-size, x+size, y+size);g.drawLine(x+size, y-size, x-size, y+size);}
        else if (style==5) {g.drawLine(x, y-size, x, y+size);g.drawLine(x+size, y, x-size, y);}
        else if (style==6) {
             g.drawLine(x, y-size, x, y+size);g.drawLine(x+size, y, x-size, y);// +
             int circleLine=(int)Math.round(size*(0.70710678118654752440084436210485)); // = size*(sqrt(2)/2) = size*cos(45 degrees)
             g.drawLine(x-circleLine, y-circleLine, x+circleLine, y+circleLine);g.drawLine(x+circleLine, y-circleLine, x-circleLine, y+circleLine); // X
        }
        else if (style==7) {g.drawLine(x, y, x, y);} // just a single pixel
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /**
     * Draws a point at the coordinate of the values
     * The point is represented by a text string drawn in the current font
     * @param xValue
     * @param yValue
     * @param text
     */
    public void drawDataPoint(double xValue, double yValue, String text) {
        int x=getXforValue(xValue);
        int y=getYforValue(yValue);
        Rectangle2D b=g.getFontMetrics().getStringBounds(text,g);
        g.drawString(text, x-(int)(b.getWidth()/2.0), y-(int)(b.getHeight()/2.0));
    }
    
    /**
     * Draws a line from the point (startX,startY) to the point (endX,endY)
     * values are given in graph coordinate space
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    public void drawLine(double startX, double startY, double endX, double endY) {
        g.drawLine(getXforValue(startX),getYforValue(startY),getXforValue(endX),getYforValue(endY));
    }    
    
    /**
     * Draws a group of bars for a barchart.
     * @param values The values for each of the bars to be drawn
     * @param colors a set of colors to color the bars (if this list is shorter than the provided list of values, the colors will be cycled)
     * @param bordercolors an optional set of colors to use for borders around the bars. (if this list is shorter than the provided list of values, the colors will be cycled)
     * @param barwidth The width of the bar in pixels
     * @param bardist The distance from the start of one bar to the next.
     *                If this is 0, all bars will drawn at the same location,
     *                If bardist is less than barwidth they bars will overlap
     * @param x The pixel x-coordinate for the start of the bars
     */
    public void drawBars(double[] values, Color[] colors, Color[] bordercolors,  int barwidth, int bardist, int x, boolean gradient, int shadow) {
        for (int i=0;i<values.length;i++) {
            Color color=colors[i%colors.length];
            Color border=(bordercolors!=null)?bordercolors[i%bordercolors.length]:null;            
            drawBar(values[i],color,border,barwidth,x,gradient,shadow);
            x+=bardist;
        }
    }
    
    /**
     * Draws a group of bars for a barchart.
     * @param values The values for each of the bars to be drawn
     * @param stddev standard deviation for each value (to be drawn as whiskers)
     * @param colors a set of colors to color the bars (if this list is shorter than the provided list of values, the colors will be cycled)
     * @param bordercolors an optional set of colors to use for borders around the bars. (if this list is shorter than the provided list of values, the colors will be cycled)
     * @param barwidth The width of the bar in pixels
     * @param bardist The distance from the start of one bar to the next.
     *                If this is 0, all bars will drawn at the same location,
     *                If bardist is less than barwidth they bars will overlap
     * @param x The pixel x-coordinate for the start of the bars
     */
    public void drawBars(double[] values, double[] stddev, Color[] colors, Color[] bordercolors, int barwidth, int bardist, int x, boolean gradient, int shadow) {
        for (int i=0;i<values.length;i++) {
            Color color=colors[i%colors.length];
            Color border=(bordercolors!=null)?bordercolors[i%bordercolors.length]:null; 
            drawBar(values[i],stddev[i],color,border,barwidth,x,gradient,shadow);
            x+=bardist;
        }
    }    

    /**
     * Draws a bar in a barchart.
     * @param value The value for the bar to be drawn
     * @param color The color to use for the bar
     * @param bordercolor An optional color with which to paint a border around the bar (can be null)
     * @param barwidth The width of the bar in pixels
     * @param x The pixel x-coordinate for the bar
     */
    public void drawBar(double value, Color color, Color bordercolor, int barwidth, int x, boolean gradient, int shadow) {
        Color oldcolor=g.getColor();              
        int y=getYforValue(value);
        int y0=getYforValue(0);
        int barheight=0;
        int top=0;
        if (value==0 || Double.isNaN(value)) return; // do not draw bar if value is 0 or NaN
        g.setColor(color);
        if (value>0) {
          top=y;
          barheight=(y0-y);
        } else { // negative value
          top=y0;
          barheight=(y-y0);
        }
        if (shadow>0) {
           g.setColor(new Color(0,0,0,25));
           int sx=x+shadow-((bordercolor!=null)?1:0);
           int sw=barwidth+((bordercolor!=null)?2:0);            
           if (value>0) {
               g.fillRect(sx, y+shadow, sw, (y0-y)-shadow);
               g.fillRect(sx+1, y+shadow+1, sw-2, (y0-y)-(shadow+1));
           } else { // negative value shadow
               g.fillRect(sx, y0, sw, (y-y0)+shadow);
               g.fillRect(sx+1, y0, sw-2, (y-y0)+shadow-1);
           }
        }        
        if (gradient) g.setPaint(getHorizontalGradientPaint(color, barwidth, x));  
        else g.setColor(color);
        g.fillRect(x, top, barwidth, barheight);
        if (bordercolor!=null) {
            g.setColor(bordercolor);
            g.drawRect(x, top, barwidth, barheight);            
        }
        g.setColor(oldcolor);
    }
    
    /**
     * Draws a bar in a barchart.
     * @param value The value for the bar to be drawn
     * @param stddev The stddev (which will be drawn as a black whisker on top of bar if stddev>0) 
     * @param color The color to use for the bar
     * @param bordercolor An optional color with which to paint a border around the bar (can be null)
     * @param barwidth The width of the bar in pixels
     * @param x The pixel x-coordinate for the bar
     */
    public void drawBar(double value, double stddev, Color color, Color bordercolor, int barwidth, int x, boolean gradient, int shadow) {
        Color oldcolor=g.getColor();
        drawBar(value,color,bordercolor,barwidth,x,gradient, shadow);
        if (stddev>0) { // draw StdDev whiskers
           int topY=getYforValue(value+stddev);
           int bottomY=getYforValue(value-stddev);
           g.setColor(Color.BLACK);
           int centerX=x+(int)(barwidth/2);
           double whiskerwidth=(int)(barwidth*0.4); // 40% of barwidth
           if (whiskerwidth<3) whiskerwidth=0; // draw only centerline
           if (whiskerwidth>0) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0), topY, (int)Math.ceil(centerX+whiskerwidth/2.0), topY);
           if (whiskerwidth>0) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0), bottomY,(int)Math.ceil(centerX+whiskerwidth/2.0), bottomY);
           g.drawLine(centerX, topY, centerX, bottomY);          
        }
        g.setColor(oldcolor);
    }    
    
    /** A more advanced bar drawing */
    public void drawBar(int x, double value, double stddev, Color color, String fill, int barwidth, int whiskerwidth, Color whiskerColor, boolean doubleWhiskers, int bevel, int shadow) {
        if (value==0 || Double.isNaN(value)) return; // do not draw bar if value is 0 or NaN
        Color oldcolor=g.getColor();              
        int y=getYforValue(value);
        int y0=getYforValue(0);
        int barheight=0;
        int top=0;
        g.setColor(color);
        if (value>0) {
           top=y;
           barheight=(y0-y);
        } else { // negative value
           top=y0;
           barheight=(y-y0);
        }
        if (shadow>0) {
           g.setColor(new Color(0,0,0,25));
           int sx=x+shadow;          
           if (value>=0) {
               g.fillRect(sx, y+shadow, barwidth, (y0-y)-shadow);
               //g.fillRect(sx+1, y+shadow+1, barwidth-2, (y0-y)-(shadow+1)); // blur shadow by drawing a smaller shadow box inside 
               if (stddev>0 && whiskerwidth>0) { // whisker shadow
                   int topY=getYforValue(value+stddev);
                   int centerX=x+(int)(barwidth/2); 
                   if (whiskerwidth>=3) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0)+shadow, topY+shadow, (int)Math.ceil(centerX+whiskerwidth/2.0)+shadow, topY+shadow); // top whisker
                   g.drawLine(centerX+shadow, topY+1+shadow, centerX+shadow, y-1+shadow); // vertical line                    
               }
           } else { // negative value shadow
               g.fillRect(sx, y0, barwidth, (y-y0)+shadow);
               //g.fillRect(sx+1, y0, barwidth-2, (y-y0)+shadow-1); // blur shadow by drawing a smaller shadow box inside 
               if (stddev>0 && whiskerwidth>0) { // whisker shadow                 
                   int bottomY=getYforValue(value-stddev);
                   int centerX=x+(int)(barwidth/2);                       
                   if (whiskerwidth>=3) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0)+shadow, bottomY+shadow,(int)Math.ceil(centerX+whiskerwidth/2.0)+shadow, bottomY+shadow); // bottom whisker
                   g.drawLine(centerX+shadow, y+1+shadow, centerX+shadow, bottomY-1+shadow); // vertical line                     
               }               
           }           
        }        
        Paint fillcolor=null;
        if (fill.equals("none")) fillcolor=null;
        else if (fill.equals("color")) fillcolor=color;
        else if (fill.equals("bar") || fill.equals("hbar")) fillcolor=Graph.getHorizontalGradientPaint(color, barwidth, x);
        else if (fill.equals("vbar")) fillcolor=Graph.getVerticalGradientPaint(color, barheight, top);
        else if (fill.startsWith("gradient")) {
            int dir=0;
            if (fill.startsWith("gradient:")) try {dir=Integer.parseInt(fill.substring("gradient:".length()));} catch (Throwable e) {}
            fillcolor=Graph.getDiagonalGradientPaint(color, x, top, barwidth, barheight, dir);
        } else {
            try {
               fillcolor=getColorFromSetting(fill, color);
            } catch (ExecutionError p) {
               fillcolor=new Color(color.getRed(),color.getGreen(),color.getBlue(),80);
            }  
        }
        if (fillcolor!=null) {
            g.setPaint(fillcolor);
            g.fillRect(x, top, barwidth, barheight+1);
        }
        g.setColor(color);      
        int upperBoxCoordinate=top;
        int lowerBoxCoordinate=top+barheight;
        if (bevel==0 || bevel>3) {  // regular box
            g.drawRect(x,upperBoxCoordinate, barwidth-1, (lowerBoxCoordinate-upperBoxCoordinate));
        } else if (bevel==1) { // box with brighter top/left and darker bottom/right
            g.setColor(color.darker());
            g.drawLine(x+barwidth-1, upperBoxCoordinate, x+barwidth-1, lowerBoxCoordinate); // right
            g.drawLine(x, lowerBoxCoordinate, x+barwidth-1, lowerBoxCoordinate); // bottom          
            g.setColor(brighter(color,0.4f)); 
            g.drawLine(x, upperBoxCoordinate, x, lowerBoxCoordinate); // left
            g.drawLine(x, upperBoxCoordinate, x+barwidth-1, upperBoxCoordinate); // top              
        } else if (bevel==2 || bevel==3) {
            g.setColor(color.darker());
            g.drawLine(x+barwidth-1, upperBoxCoordinate, x+barwidth-1, lowerBoxCoordinate); // right
            g.drawLine(x+barwidth-2, upperBoxCoordinate, x+barwidth-2, lowerBoxCoordinate); // right-1
            g.drawLine(x, lowerBoxCoordinate, x+barwidth-1, lowerBoxCoordinate); // bottom     
            g.drawLine(x, lowerBoxCoordinate-1, x+barwidth-1, lowerBoxCoordinate-1); // bottom-1         
            g.setColor(brighter(color,0.40f)); 
            g.drawLine(x, upperBoxCoordinate+1, x+barwidth-2, upperBoxCoordinate+1); // top+1   
            g.drawLine(x+1, upperBoxCoordinate, x+1, lowerBoxCoordinate-1); // left+1
            if (bevel==3) g.setColor(color); // outer border in original color for contrast purposes            
            g.drawLine(x, upperBoxCoordinate, x+barwidth-1, upperBoxCoordinate); // top   
            g.drawLine(x, upperBoxCoordinate, x, lowerBoxCoordinate); // left                          
        }
        // draw standard deviation whiskers
        if (stddev>0 && whiskerwidth>0) { // draw StdDev whiskers
           int topY=getYforValue(value+stddev);
           int bottomY=getYforValue(value-stddev);
           g.setColor(whiskerColor);
           int centerX=x+(int)(barwidth/2);
           if (whiskerwidth<3) whiskerwidth=0; // draw only centerline if whiskerwidth is less than 2 or less
           if (doubleWhiskers) {
               if (whiskerwidth>0) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0), topY, (int)Math.ceil(centerX+whiskerwidth/2.0), topY); // top whisker
               if (whiskerwidth>0) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0), bottomY,(int)Math.ceil(centerX+whiskerwidth/2.0), bottomY); // bottom whisker
               g.drawLine(centerX, topY+1, centerX, bottomY-1); // vertical line   
           } else if (value>=0) { // single whisker positive value
               if (whiskerwidth>0) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0), topY, (int)Math.ceil(centerX+whiskerwidth/2.0), topY); // top whisker
               g.drawLine(centerX, topY+1, centerX, y-1); // vertical line 
           } else { // single whisker negative value
               if (whiskerwidth>0) g.drawLine((int)Math.floor(centerX-whiskerwidth/2.0), bottomY,(int)Math.ceil(centerX+whiskerwidth/2.0), bottomY); // bottom whisker
               g.drawLine(centerX, y+1, centerX, bottomY-1); // vertical line
           }
        }
        g.setColor(oldcolor);
    }       

    /**
     * Draws a histogram based on the bin-counts provided. 
     * The bins must span the whole value range! and the count values
     * must lie within the Y-range of the graph (with 0 as minimum value)
     * @param counts
     * @param outline If TRUE, the outline of the histogram will be painted in the current color
     *                and the histogram will be filled with an transparent version of the current color.
     *                This is useful if multiple histograms are to be drawn on top of each other.
     *                If FALSE, the histogram will be drawn and filled with the current color.
     */
    public void drawHistogram(double[] counts, boolean outline) {
        double binwidth=(double)graphwidth/(double)counts.length;
        Polygon histogram=new Polygon();
        histogram.addPoint(0, graphheight);
        for (int i=0;i<counts.length;i++) {
            double binvalue=counts[i];
            int stackheight=(int)Math.ceil((binvalue/maxY)*graphheight);
            histogram.addPoint((int)(i*binwidth), graphheight-stackheight);
            histogram.addPoint((int)((i+1)*binwidth), graphheight-stackheight);
        }
        histogram.addPoint((int)(counts.length*binwidth), graphheight);
        histogram.translate(translateX+1, translateY);
        Color color=g.getColor();
        if (outline) g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),80)); // if (outline) fill the rectangle in a slightly more transparent color       
        g.fillPolygon(histogram);
        g.setColor(color);
        if (outline) g.drawPolygon(histogram);
    }
    
    /**
     * Draws an outlined histogram. This method is similar to drawHistogram(counts,true)
     * @param counts 
     */
    public void drawHistogram(double[] counts) {
        drawHistogram(counts,true);
    }

    
    /**
     * Draws a histogram based on the bin-counts provided. 
     * @param minX The leftmost end of the x-Range
     * @param maxX The rightmost end of the x-Range
     * @param counts
     * @param outline If TRUE, the outline of the histogram will be painted in the current color
     *                and the histogram will be filled with an transparent version of the current color.
     *                This is useful if multiple histograms are to be drawn on top of each other.
     *                If FALSE, the histogram will be drawn and filled with the current color.
     */
    public void drawHistogram(double[] counts, boolean outline, double minX, double maxX) {
        int xLeft=getXforValue(minX);
        int xRight=getXforValue(maxX);
        int width=xRight-xLeft;
        if (outline) width-=2; // this is necessary to get it right
        double binwidth=(double)width/(double)counts.length;
        Polygon histogram=new Polygon();
        histogram.addPoint(0, graphheight);
        for (int i=0;i<counts.length;i++) {
            double binvalue=counts[i];
            int stackheight=(int)Math.ceil((binvalue/maxY)*graphheight);
            histogram.addPoint((int)(i*binwidth), graphheight-stackheight);
            histogram.addPoint((int)((i+1)*binwidth), graphheight-stackheight);
        }
        histogram.addPoint((int)(counts.length*binwidth), graphheight);
        histogram.translate(xLeft+1, translateY);
        Color color=g.getColor();
        if (outline) g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),80)); // if (outline) fill the rectangle in a slightly more transparent color       
        g.fillPolygon(histogram);
        g.setColor(color);
        if (outline) g.drawPolygon(histogram);
    }    
    
    /**
     * Draws a horizontally aligned Box and Whiskers plot.
     * The box is drawn from the 1st quartile to the 3rd quartile with a vertical line inside for the median value 
     * The whiskers are drawn at the given min and max values
     * @param min
     * @param max
     * @param median
     * @param firstQuartile
     * @param thirdQuartile
     * @param yPos the Y-coordinate where the box should be drawn
     * @param halfheight Half the size of the box
     */
    public void drawHorizontalBoxAndWhiskers(double min, double max, double median, double firstQuartile, double thirdQuartile, int yPos, int boxhalfheight) {
        int boxheight=boxhalfheight*2;
        Color color=g.getColor();
        g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),80));
        int minXcoordinate=getXforValue(min);
        int maxXcoordinate=getXforValue(max);
        int leftBoxCoordinate=getXforValue(firstQuartile);
        int rightBoxCoordinate=getXforValue(thirdQuartile);
        int medianXcoordinate=getXforValue(median);
        g.fillRect(leftBoxCoordinate, yPos, (rightBoxCoordinate-leftBoxCoordinate), boxheight);
        g.setColor(color);
        g.drawRect(leftBoxCoordinate, yPos, (rightBoxCoordinate-leftBoxCoordinate), boxheight);
        g.drawLine(minXcoordinate, yPos, minXcoordinate, yPos+boxheight);
        g.drawLine(maxXcoordinate, yPos, maxXcoordinate, yPos+boxheight);
        g.drawLine(medianXcoordinate, yPos, medianXcoordinate, yPos+boxheight);   
        g.drawLine(minXcoordinate, yPos+boxhalfheight, leftBoxCoordinate, yPos+boxhalfheight);
        g.drawLine(rightBoxCoordinate, yPos+boxhalfheight, maxXcoordinate, yPos+boxhalfheight);
    }

    /**
     * Draws a horizontally aligned Mean+Standard Deviation plot.
     * The plot is drawn with a diamond shape at the position of the mean with
     * angular whiskers extending one standard deviation to either side
     * @param mean
     * @param stddev
     * @param yPos the Y-coordinate where the box should be drawn
     */
    public void drawHorizontalMeanAndStdDeviation(double mean, double stddev, int yPos,int boxhalfheight) {
        int boxheight=boxhalfheight*2;
        int diamondradius=boxhalfheight-3;  if (diamondradius<4) diamondradius=4;
        int leftStdXcoordinate=getXforValue(mean-stddev);
        int rightStdXcoordinate=getXforValue(mean+stddev);
        int meanXcoordinate=getXforValue(mean);
        int middleline=yPos+boxhalfheight;
        g.drawLine(leftStdXcoordinate, middleline, rightStdXcoordinate,middleline); // full line       
        g.drawPolyline(new int[]{leftStdXcoordinate+boxhalfheight,leftStdXcoordinate,leftStdXcoordinate+boxhalfheight}, new int[]{yPos,middleline,yPos+boxheight}, 3);
        g.drawPolyline(new int[]{rightStdXcoordinate-boxhalfheight,rightStdXcoordinate,rightStdXcoordinate-boxhalfheight}, new int[]{yPos,middleline,yPos+boxheight}, 3);
        Polygon diamond=new Polygon(new int[]{meanXcoordinate-diamondradius,meanXcoordinate,meanXcoordinate+diamondradius,meanXcoordinate}, new int[]{middleline,middleline-diamondradius,middleline,middleline+diamondradius}, 4);
        g.fillPolygon(diamond);
        g.drawPolygon(diamond);
    }

   /**
     * Draws a vertically aligned Box and Whiskers plot.
     * The box is drawn from the 1st quartile to the 3rd quartile with a vertical line inside for the median value
     * The whiskers are drawn at the given min and max values
     * @param min
     * @param max
     * @param median
     * @param firstQuartile
     * @param thirdQuartile
     * @param xPos the X-coordinate where the box should be drawn
     */
    public void drawVerticalBoxAndWhiskers(double min, double max, double median, double firstQuartile, double thirdQuartile, int xPos, int boxhalfwidth) {
        int boxwidth=boxhalfwidth*2;
        int minYcoordinate=getYforValue(min);
        int maxYcoordinate=getYforValue(max);
        int upperBoxCoordinate=getYforValue(thirdQuartile);
        int lowerBoxCoordinate=getYforValue(firstQuartile);
        int medianYcoordinate=getYforValue(median);
        Color color=g.getColor();
        g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),80));        
        g.fillRect(xPos,upperBoxCoordinate, boxwidth, (lowerBoxCoordinate-upperBoxCoordinate));
        g.setColor(color);
        g.drawRect(xPos,upperBoxCoordinate, boxwidth, (lowerBoxCoordinate-upperBoxCoordinate));
        g.drawLine(xPos,minYcoordinate, xPos+boxwidth, minYcoordinate);
        g.drawLine(xPos,maxYcoordinate, xPos+boxwidth, maxYcoordinate);
        g.drawLine(xPos,medianYcoordinate, xPos+boxwidth, medianYcoordinate);
        g.drawLine(xPos+boxhalfwidth, maxYcoordinate, xPos+boxhalfwidth, upperBoxCoordinate);
        g.drawLine(xPos+boxhalfwidth, lowerBoxCoordinate, xPos+boxhalfwidth, minYcoordinate);
    }
 
    public void drawVerticalBoxAndWhiskers(double min, double max, double median, double firstQuartile, double thirdQuartile, int xPos, int boxwidth, int whiskerswidth, Paint fillcolor, int bevel) {
        Color color=g.getColor(); // save current color in case we need to change it
        int boxhalfwidth=boxwidth/2;
        int whiskershalfwidth=whiskerswidth/2;
        int minYcoordinate=getYforValue(min);
        int maxYcoordinate=getYforValue(max);
        int upperBoxCoordinate=getYforValue(thirdQuartile);
        int lowerBoxCoordinate=getYforValue(firstQuartile);
        int medianYcoordinate=getYforValue(median);
        int xPosWhiskers=xPos-whiskershalfwidth;
        xPos-=boxhalfwidth;
        if (fillcolor!=null) { // 
            g.setPaint(fillcolor);  // this could be a gradient      
            g.fillRect(xPos,upperBoxCoordinate, boxwidth, (lowerBoxCoordinate-upperBoxCoordinate));
            g.setColor(color);
        }
        // draw whiskers and median line "underneath" the actual box       
        if (whiskerswidth>0){
            g.drawLine(xPos+boxhalfwidth, maxYcoordinate+1, xPos+boxhalfwidth, upperBoxCoordinate-1); // vertical line above box
            g.drawLine(xPos+boxhalfwidth, lowerBoxCoordinate+1, xPos+boxhalfwidth, minYcoordinate-1); // vertical line below box
            g.drawLine(xPosWhiskers,minYcoordinate, xPosWhiskers+whiskerswidth-1, minYcoordinate); // bottom whisker  (0% percentile)
            g.drawLine(xPosWhiskers,maxYcoordinate, xPosWhiskers+whiskerswidth-1, maxYcoordinate);  // top whisker    (100% percentile)
        }
        g.setColor(color.darker());   
        g.drawLine(xPos,medianYcoordinate, xPos+boxwidth-1, medianYcoordinate); // median line (50% percentile)
        g.setColor(color);        
        if (bevel==0 || bevel>3) {  // regular box
            g.drawRect(xPos,upperBoxCoordinate, boxwidth-1, (lowerBoxCoordinate-upperBoxCoordinate));
        } else if (bevel==1) { // box with brighter top/left and darker bottom/right
            g.setColor(color.darker());
            g.drawLine(xPos+boxwidth-1, upperBoxCoordinate, xPos+boxwidth-1, lowerBoxCoordinate); // right
            g.drawLine(xPos, lowerBoxCoordinate, xPos+boxwidth-1, lowerBoxCoordinate); // bottom          
            g.setColor(brighter(color,0.4f)); 
            g.drawLine(xPos, upperBoxCoordinate, xPos, lowerBoxCoordinate); // left
            g.drawLine(xPos, upperBoxCoordinate, xPos+boxwidth-1, upperBoxCoordinate); // top              
        } else if (bevel==2 || bevel==3) {
            g.setColor(color.darker());
            g.drawLine(xPos+boxwidth-1, upperBoxCoordinate, xPos+boxwidth-1, lowerBoxCoordinate); // right
            g.drawLine(xPos+boxwidth-2, upperBoxCoordinate, xPos+boxwidth-2, lowerBoxCoordinate); // right-1
            g.drawLine(xPos, lowerBoxCoordinate, xPos+boxwidth-1, lowerBoxCoordinate); // bottom     
            g.drawLine(xPos, lowerBoxCoordinate-1, xPos+boxwidth-1, lowerBoxCoordinate-1); // bottom-1         
            g.setColor(brighter(color,0.40f)); 
            g.drawLine(xPos, upperBoxCoordinate+1, xPos+boxwidth-2, upperBoxCoordinate+1); // top+1   
            g.drawLine(xPos+1, upperBoxCoordinate, xPos+1, lowerBoxCoordinate-1); // left+1
            if (bevel==3) g.setColor(color); // outer border in original color for contrast purposes            
            g.drawLine(xPos, upperBoxCoordinate, xPos+boxwidth-1, upperBoxCoordinate); // top   
            g.drawLine(xPos, upperBoxCoordinate, xPos, lowerBoxCoordinate); // left                          
        }
        g.setColor(color);
    }
    
    /** Returns the coordinates for [x,y,width,height] for the box plot box */
    public Rectangle getBoxRectangle(double firstQuartile, double thirdQuartile, int xPos, int boxwidth) {
        int upperBoxCoordinate=getYforValue(thirdQuartile);
        int lowerBoxCoordinate=getYforValue(firstQuartile);
        xPos-=boxwidth/2;     
        return new Rectangle(xPos, upperBoxCoordinate, boxwidth, lowerBoxCoordinate-upperBoxCoordinate);
    }

    /**
     * Draws a vertically aligned Mean+Standard Deviation plot.
     * The plot is drawn with a diamond shape at the position of the mean with
     * angular whiskers extending one standard deviation to either side
     * @param mean
     * @param stddev
     * @param yPos the Y-coordinate where the box should be drawn
     */
    public void drawVerticalMeanAndStdDeviation(double mean, double stddev, int xPos, int boxhalfwidth) {
        int diamondradius=boxhalfwidth-3;  if (diamondradius<4) diamondradius=4;
        int upperStdYcoordinate=getYforValue(mean+stddev);
        int lowerStdYcoordinate=getYforValue(mean-stddev);
        int meanYcoordinate=getYforValue(mean);
        int middleline=xPos+boxhalfwidth;
        g.drawLine(middleline, upperStdYcoordinate, middleline,lowerStdYcoordinate); // full vertical line
        g.drawPolyline(new int[]{middleline-boxhalfwidth, middleline, middleline+boxhalfwidth}, new int[]{upperStdYcoordinate+boxhalfwidth,upperStdYcoordinate,upperStdYcoordinate+boxhalfwidth}, 3); // upper angle /\
        g.drawPolyline(new int[]{middleline-boxhalfwidth, middleline, middleline+boxhalfwidth}, new int[]{lowerStdYcoordinate-boxhalfwidth,lowerStdYcoordinate,lowerStdYcoordinate-boxhalfwidth}, 3); // lower angle \/      
        Polygon diamond=new Polygon(new int[]{middleline-diamondradius,middleline,middleline+diamondradius,middleline},new int[]{meanYcoordinate,meanYcoordinate+diamondradius,meanYcoordinate,meanYcoordinate-diamondradius}, 4);
        g.fillPolygon(diamond);
        g.drawPolygon(diamond);
    }
    
    /**
     * Draws a vertically aligned Mean+Standard Deviation plot.
     * The plot is drawn with a diamond shape at the position of the mean with
     * angular whiskers extending one standard deviation to either side
     * @param mean
     * @param stddev
     * @param xPos the X-coordinate where the middle line of the box should be drawn
     */
    public void drawVerticalMeanAndStdDeviation(double mean, double stddev, int xPos, int whiskerswidth, int diamondradius, int bevel) {        
        int boxhalfwidth=whiskerswidth/2;
        int upperStdYcoordinate=getYforValue(mean+stddev);
        int lowerStdYcoordinate=getYforValue(mean-stddev);
        int meanYcoordinate=getYforValue(mean);
        int middleline=xPos;
        if (whiskerswidth>0) {
            g.drawLine(middleline, upperStdYcoordinate+1, middleline,lowerStdYcoordinate-1); // full vertical line
            g.drawPolyline(new int[]{middleline-boxhalfwidth, middleline, middleline+boxhalfwidth}, new int[]{upperStdYcoordinate+boxhalfwidth,upperStdYcoordinate,upperStdYcoordinate+boxhalfwidth}, 3); // upper angle /\
            g.drawPolyline(new int[]{middleline-boxhalfwidth, middleline, middleline+boxhalfwidth}, new int[]{lowerStdYcoordinate-boxhalfwidth,lowerStdYcoordinate,lowerStdYcoordinate-boxhalfwidth}, 3); // lower angle \/      
        }
        Polygon diamond=new Polygon(new int[]{middleline-diamondradius,middleline,middleline+diamondradius,middleline},new int[]{meanYcoordinate,meanYcoordinate+diamondradius,meanYcoordinate,meanYcoordinate-diamondradius}, 4);
        g.fillPolygon(diamond);
        g.drawPolygon(diamond);
        if (bevel>0) {
            Color color=g.getColor();
            g.setColor(color.darker());
            g.drawPolygon(diamond);
            g.setColor(brighter(color,0.40f)); 
            g.drawPolyline(new int[]{middleline, middleline-diamondradius, middleline-1}, new int[]{meanYcoordinate-diamondradius,meanYcoordinate,meanYcoordinate+diamondradius-1}, 3); //
        }
    }    
    /**
     * Returns the graphics X coordinate corresponding to a given value within the graph
     * @param value the requested value in the graph
     * @return
     */
    public int getXforValue(double value) {
        return (int)Math.round((value-minX)/(maxX-minX)*graphwidth)+translateX;
    }

    /**
     * Returns the graphics Y coordinate corresponding to a given value within the graph
     * @param value the requested value in the graph
     * @return
     */
    public int getYforValue(double value) {
        return graphheight-(int)Math.round((value-minY)/(maxY-minY)*graphheight)+translateY;
    }

    /**
     * Returns the x-value in the graph which corresponds to the pixel with the given X coordinate
     * @param x The X oordinate
     * @return
     */
    public double getValueForX(int x) {
        return (x-translateX)*(((maxX-minX)/graphwidth))+minX;
    }

    /**
     * Returns the y-value in the graph which corresponds to the pixel with the given Y coordinate
     * @param value The Y coordinate
     * @return
     */
    public double getValueForY(int y) {
        return maxY-(y-translateY)*(((maxY-minY)/graphheight));
    }


    /**
     * Draws horizontal gridlines in the current color and stroke
     */
    public void drawHorizontalGridLines() {
        double range=(maxY-minY);
        if (range<=0) return; // this should not happen!
        double tickscaleY=ceilToNearestTenth(range);
        if (range/tickscaleY>=20) tickscaleY=roundToNearestX(tickscaleY*5, tickscaleY); // check if there are too many ticks
        else if (range/tickscaleY>15) tickscaleY=roundToNearestX(tickscaleY*2, tickscaleY); // check if there are too many ticks
        double tickAnchorY=0;
        if ((minY<0 && maxY<0) || (minY>0 && maxY>0)) {
            // 0 is not included in the range. Find some other anchor
            tickAnchorY=ceilToNearestX(minY,tickscaleY);
        }
        int tickAnchorYcoordinate=getYforValue(tickAnchorY);
        g.drawLine(translateX, tickAnchorYcoordinate, translateX+graphwidth, tickAnchorYcoordinate);
        double lefttick=tickAnchorY-tickscaleY;
        int lefttickYcoordinate=getYforValue(lefttick);
        while (lefttickYcoordinate<=graphheight+translateY) {
           g.drawLine(translateX, lefttickYcoordinate, translateX+graphwidth, lefttickYcoordinate);
           lefttick-=tickscaleY;
           lefttickYcoordinate=getYforValue(lefttick);
        }
        double righttick=tickAnchorY+tickscaleY;
        int righttickYcoordinate=getYforValue(righttick);
        while (righttickYcoordinate>=translateY) {
           g.drawLine(translateX, righttickYcoordinate, translateX+graphwidth, righttickYcoordinate);
           righttick+=tickscaleY;
           righttickYcoordinate=getYforValue(righttick);
        }
    }

    /** Draws a horizontal gridline in the selected color with the selected line type at the specified pixel y-coordinate */
    public void drawHorizontalGridLine(int y, int gridstyle, Color color) {
         Stroke gridlinestroke=null;
         Stroke defaultStroke=g.getStroke();
         Color currentColor=g.getColor();
         if (gridstyle==DASHED) {
             gridlinestroke=DASHED_STROKE;
         } else if (gridstyle==DASHED_LONG) {
             gridlinestroke=DASHED_LONG_STROKE;
         } else if (gridstyle==DOTTED) {
             gridlinestroke=DOTTED_STROKE;
         } else if (gridstyle==SOLID) {
             gridlinestroke=defaultStroke;
         }             
         if (gridlinestroke!=null) {
             g.setStroke(gridlinestroke);
             g.setColor(color);
             g.drawLine(translateX, y, translateX+graphwidth, y);
             g.setStroke(defaultStroke);
             g.setColor(currentColor);
         }

    }

    /**
     * Draws vertical gridlines in the current color and stroke
     */
    public void drawVerticalGridLines() {
        // X-axis ticks
        double range=(maxX-minX);
        if (range<=0) return; // this should not happen!        
        double tickscaleX=ceilToNearestTenth(range);
        if (range/tickscaleX>=20) tickscaleX=roundToNearestX(tickscaleX*5, tickscaleX); // check if there are too many ticks
        else if (range/tickscaleX>15) tickscaleX=roundToNearestX(tickscaleX*2, tickscaleX); // check if there are too many ticks
        double tickAnchorX=0;
        if ((minX<0 && maxX<0) || (minX>0 && maxX>0)) {
            // 0 is not included in the range. Find some other anchor
            tickAnchorX=ceilToNearestX(minX,tickscaleX);
        }
        int tickAnchorXcoordinate=getXforValue(tickAnchorX);
        g.drawLine(tickAnchorXcoordinate, translateY, tickAnchorXcoordinate, translateY+graphheight);
        double lefttick=tickAnchorX-tickscaleX;
        int lefttickXcoordinate=getXforValue(lefttick);
        while (lefttickXcoordinate>=translateX) {
           g.drawLine(lefttickXcoordinate, translateY, lefttickXcoordinate, translateY+graphheight);
           lefttick-=tickscaleX;
           lefttickXcoordinate=getXforValue(lefttick);
        }
        double righttick=tickAnchorX+tickscaleX;
        int righttickXcoordinate=getXforValue(righttick);
        while (righttickXcoordinate<=graphwidth+translateX) {
           g.drawLine(righttickXcoordinate, translateY, righttickXcoordinate, translateY+graphheight);
           righttick+=tickscaleX;
           righttickXcoordinate=getXforValue(righttick);
        }
    }    
    
      /** Draws a vertical gridline in the selected color with the selected line type at the specified pixel x-coordinate */
    public void drawVerticalGridLine(int x, int gridstyle, Color color) {
         Stroke gridlinestroke=null;
         Stroke defaultStroke=g.getStroke();
         Color currentColor=g.getColor();
         if (gridstyle==DASHED) {
             gridlinestroke=DASHED_STROKE;
         } else if (gridstyle==DASHED_LONG) {
             gridlinestroke=DASHED_LONG_STROKE;
         } else if (gridstyle==DOTTED) {
             gridlinestroke=DOTTED_STROKE;
         } else if (gridstyle==SOLID) {
             gridlinestroke=defaultStroke;
         } 
         if (gridlinestroke!=null) {
             g.setStroke(gridlinestroke);
             g.setColor(color);
             g.drawLine(x, translateY, x, translateY+graphheight);
             g.setStroke(defaultStroke);
             g.setColor(currentColor);
         }
    }
    
    /** Calculates the size of bounding box for the given string drawn with the given font in the given graphics context */
    public static Dimension getDimension(String string, Font font, Graphics2D g) {
        if (g==null) {
            BufferedImage image=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
            g=image.createGraphics();
        }
        FontMetrics metrics=(font!=null)?g.getFontMetrics(font):g.getFontMetrics();
        Rectangle2D rect=metrics.getStringBounds(string, g);
        return new Dimension((int)Math.ceil(rect.getWidth()), (int)Math.ceil(rect.getHeight()));
    }    
    
    /**
     * Draws a legend-box connecting names and colors at the specified coordinate
     * The names will be placed beneath each other
     * @param names
     * @param colors
     * @param x
     * @param y
     * @boundingBox If TRUE a rectangular box will be drawn around the legends
     *
     */
    public void drawLegendBox(String[] names, Color[] colors, int x, int y, boolean boundingBox) {
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        int namewidth=findLongestString(names,g.getFont());
        int fontHeight=g.getFontMetrics().getHeight();
        int margin=6;
        int boxtextspace=4;
        int ascent=g.getFontMetrics().getAscent();
        int spacing=4;
        int colorboxsize=(int)(fontHeight*0.7);
        int legendheight=margin+fontHeight*names.length+spacing*(names.length-1)+margin;
        int legendwidth=margin+colorboxsize+boxtextspace+namewidth+margin;
        g.setColor(Color.WHITE);
        g.fillRect(x, y, legendwidth, legendheight);
        g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        if (boundingBox) g.drawRect(x, y, legendwidth-1, legendheight-1);
        y=y+margin+ascent;
        x=x+margin;
        for (int i=0;i<names.length;i++) {
            int boxy=y-colorboxsize;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(colors[i%colors.length]);
            g.fillRect(x, boxy, colorboxsize, colorboxsize);
            g.setColor(Color.BLACK);
            g.drawRect(x, boxy, colorboxsize, colorboxsize);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            g.drawString(names[i], x+boxtextspace+colorboxsize, y);
            y+=(fontHeight+spacing);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /** Calculates the size of a vertical legend box containing the given names */
    public static Dimension getLegendDimension(String[] names, Font font) {
        BufferedImage image=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        return getLegendDimension(names, font, g);

    }
    
    /** Calculates the size of a vertical legend box containing the given names */
    public static Dimension getLegendDimension(String[] names, Font font, Graphics2D g) {
        int namewidth=findLongestString(names,font);        
        FontMetrics metrics=(font!=null)?g.getFontMetrics(font):g.getFontMetrics();
        int fontHeight=metrics.getHeight();
        int margin=6;
        int boxtextspace=4;
        int spacing=4;
        int colorboxsize=(int)(fontHeight*0.7);
        int legendheight=margin+fontHeight*names.length+spacing*(names.length-1)+margin;
        int legendwidth=margin+colorboxsize+boxtextspace+namewidth+margin;
        return new Dimension(legendwidth, legendheight);       
    }
    
    
    
    /**
     * Draws a legend-box connecting names and colors at the specified coordinate
     * The names will be placed after each other on the same line
     * @param names
     * @param colors
     * @param x
     * @param y
     * @param spacing The number of pixels between two entries
     * @boundingBox If TRUE a rectangular box will be drawn around the legends
     *
     */
    public void drawHorizontalLegendBox(String[] names, Color[] colors, int x, int y, int spacing, boolean boundingBox) {
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        int fontHeight=g.getFontMetrics().getHeight();
        int margin=6;
        int boxtextspace=4; // spacing between box and text
        int ascent=g.getFontMetrics().getAscent();
        int colorboxsize=(int)(fontHeight*0.7);
        int textY=y+margin+ascent;
        int textX=x+margin;
        int legendwidth=0;
        for (int i=0;i<names.length;i++) {
            int boxy=textY-colorboxsize;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(colors[i%colors.length]);
            g.fillRect(textX, boxy, colorboxsize, colorboxsize);
            g.setColor(Color.BLACK);
            g.drawRect(textX, boxy, colorboxsize, colorboxsize);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
            g.drawString(names[i], textX+boxtextspace+colorboxsize, textY);
            int entrywidth=g.getFontMetrics().stringWidth(names[i])+boxtextspace+colorboxsize;
            legendwidth+=(entrywidth+spacing);                    
            textX+=(entrywidth+spacing);
        }
        int legendheight=margin+fontHeight+margin;
        legendwidth+=(margin+margin-spacing); // remove end spacing that was added in loop above
        if (boundingBox) {
            g.setColor(Color.BLACK);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.drawRect(x, y, legendwidth-1, legendheight-1);     
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }    

    /** Calculates the size of a vertical legend box containing the given names */
    public static Dimension getHorizontalLegendDimension(String[] names, int spacing, Font font, Graphics2D g) {
        FontMetrics metrics=(font!=null)?g.getFontMetrics(font):g.getFontMetrics();
        int fontHeight=metrics.getHeight();
        int margin=6;
        int boxtextspace=4;
        int colorboxsize=(int)(fontHeight*0.7);
        int legendwidth=0;
        for (int i=0;i<names.length;i++) {
            int entrywidth=metrics.stringWidth(names[i])+boxtextspace+colorboxsize;
            legendwidth+=(entrywidth+spacing);
        }
        int legendheight=margin+fontHeight+margin;
        legendwidth+=(margin+margin-spacing); // remove end spacing that was added in loop above
        return new Dimension(legendwidth, legendheight);
    } 
    
    public static Dimension getHorizontalLegendDimension(String[] names, int spacing, Font font) {
        BufferedImage image=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();  
        return getHorizontalLegendDimension(names, spacing, font, g);
    }
    
    
    /**
     * Draws a text string at the given pixel-coordinate in the graph
     * @param g
     * @param text
     * @param x
     * @param y
     * @param alignX This parameter aligns the horizontal position of the text in relation to the X coordinate.
     * @param alignY This parameter aligns the vertical position of the text in relation to the Y coordinate.
     */
    public void drawAlignedString(String text, int x, int y, double alignX, double alignY) {
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        int stringlength=g.getFontMetrics().stringWidth(text);
        int ascent=g.getFontMetrics().getAscent();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        g.drawString(text, (float)(x-stringlength*alignX), (float)(y+ascent*alignY));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /**
     * Draws a vertically oriented text string at the given pixel-coordinate in the graph
     * @param text
     * @param x
     * @param y
     * @param up If true the text will be read upwards (with head tilted to the left). If false the text will be read downwards (with head tilted to the right)
     */
    public void drawVerticalString(String text, int x, int y, boolean up) {
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        int ascent=g.getFontMetrics().getAscent();
        AffineTransform oldtransform=g.getTransform();
        if (up) g.rotate(3*Math.PI/2,x,y);
        else g.rotate(Math.PI/2,x,y);
        g.drawString(text,  x, y+(int)((double)ascent/2f)-2); //
        g.translate(x, y);
        g.setTransform(oldtransform);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /**
     * Draws a vertically oriented text string at the given pixel-coordinate in the graph
     * @param text
     * @param x
     * @param y
     * @param alignX Note that this is alignment in direction of text (which would be horizontally of the text was horizontal)
     * @param alignY Note that this is alignment in direction of text (which would be vertically of the text was horizontal)
     * @param up If true the text will be read upwards (with head tilted to the left). If false the text will be read downwards (with head tilted to the right)
     */
    public void drawVerticalAlignedString(String text, int x, int y, double alignX, double alignY, boolean up) {
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        int stringlength=g.getFontMetrics().stringWidth(text);
        int ascent=g.getFontMetrics().getAscent();
        AffineTransform oldtransform=g.getTransform();
        if (up) g.rotate(3*Math.PI/2,x,y);
        else g.rotate(Math.PI/2,x,y);
        g.drawString(text, (float)(x-stringlength*alignX), (float)(y+ascent*alignY));
        g.translate(x, y);
        g.setTransform(oldtransform);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /** Returns TRUE if the given value is in the ticks list (or at least within +/- 10% of the scale to counteract precision problems) */
    private boolean shouldDrawTick(double value, ArrayList<Double> ticks, double scale) {
        scale=scale/10.0;
        for (double tick:ticks) {
            if (value>tick-scale && value<tick+scale) return true;
        }
        return false;
    }


    /** Returns the values corresponding to the standard tickmarks on the vertical axis for a graph with the given height and range.
     *  This method is static so that it can be used before a Graph object is created to determine
     *  which ticks will appear on the vertical axis and from that derive a useful setting for translateX
     *  to be used when the Graph is created
     */
    public static ArrayList<Double> getVerticalTicks(double minY, double maxY) {
        ArrayList<Double> ticks=new ArrayList<Double>();
        if (maxY==minY) return ticks;
        double range=(maxY-minY);
        double tickscaleY=ceilToNearestTenth(range);
        if (range/tickscaleY>=20) tickscaleY=roundToNearestX(tickscaleY*5, tickscaleY); // check if there are too many ticks
        else if (range/tickscaleY>15) tickscaleY=roundToNearestX(tickscaleY*2, tickscaleY); // check if there are too many ticks
        double tickAnchorY=0;
        if ((minY < 0 && maxY < 0) || (minY > 0 && maxY > 0)) {
            // 0 is not included in the range. Find some other anchor
            tickAnchorY=ceilToNearestX(minY,tickscaleY);
        }
        if (range>0) {        
            ticks.add(tickAnchorY);
            double lefttick=tickAnchorY-tickscaleY;
            ticks.add(lefttick);
            while (lefttick>=minY) {
               ticks.add(lefttick);
               lefttick-=tickscaleY;
            }
            double righttick=tickAnchorY+tickscaleY;
            ticks.add(righttick);
            while (righttick<=maxY) {
               ticks.add(righttick);
               righttick+=tickscaleY;
            }
        }
        return ticks;
    }
    
    /** Returns the values corresponding to the standard tickmarks on the horizontal axis for a graph with the given width and range.
     *  This method is static so that it can be used before a Graph object is created to determine
     *  which ticks will appear on the horizontal axis 
     */
    public static ArrayList<Double> getHorizontalTicks(double minX, double maxX) {
        ArrayList<Double> ticks=new ArrayList<Double>();
        if (maxX==minX) return ticks;
        double range=(maxX-minX);
        double tickscaleX=ceilToNearestTenth(range);
        if (range/tickscaleX>=20) tickscaleX=roundToNearestX(tickscaleX*5, tickscaleX); // check if there are too many ticks
        else if (range/tickscaleX>15) tickscaleX=roundToNearestX(tickscaleX*2, tickscaleX); // check if there are too many ticks
        double tickAnchorX=0;
        if ((minX < 0 && maxX < 0) || (minX > 0 && maxX > 0)) {
            // 0 is not included in the range. Find some other anchor
            tickAnchorX=ceilToNearestX(minX,tickscaleX);
        }
        if (range>0) {        
            ticks.add(tickAnchorX);
            double lefttick=tickAnchorX-tickscaleX;
            ticks.add(lefttick);
            while (lefttick>=minX) {
               ticks.add(lefttick);
               lefttick-=tickscaleX;
            }
            double righttick=tickAnchorX+tickscaleX;
            ticks.add(righttick);
            while (righttick<=maxX) {
               ticks.add(righttick);
               righttick+=tickscaleX;
            }
        }
        return ticks;
    }    
    
    /**
     * Draws a tick with the given text (horizontally oriented) along the x-axis at the given position
     * @param text
     * @param x x-coordinate for the tick (in pixels)
     * @param y y-coordinate for the top of the tickline (in pixels)
     */
    public void drawHorizontalStringXTick(String text, int x, int y) {
        int ticksize=5;
        g.drawLine(x, y, x, y+ticksize);
        drawAlignedString(text, x, y+ticksize,0.5,1);
    }

    /**
     * Draws a tick with the given text (vertically oriented) along the x-axis at the given position
     * @param text
     * @param x x-coordinate for the tick (in pixels)
     * @param y y-coordinate for the top of the tickline (in pixels)
     * @param up If true the text will be read upwards (with head tilted to the left). If false the text will be read downwards (with head tilted to the right)
     */
    public void drawVerticalStringXTick(String text, int x, int y, boolean up) {
        int ticksize=5;
        g.drawLine(x, y, x, y+ticksize);
        int stringwidth=g.getFontMetrics().stringWidth(text);
        if (up) drawVerticalString(text, x, y+ticksize+4+stringwidth, up);
        else drawVerticalString(text, x, y+ticksize+4, up);
    }

    /**
     * Draws a tick with the given text (horizontally oriented) along the y-axis at the given position
     * @param text
     * @param x x-coordinate for rightmost pixel of the tickline (usually the y-axis)
     * @param y y-coordinate for the tick (in pixels)
     */
    public void drawHorizontalStringYTick(String text, int x, int y) {
        int ticksize=5;
        g.drawLine(x, y, x-ticksize, y);
        drawAlignedString(text, x-(ticksize+2), y, 1.0,0.3f);
    }    
    

    /** */
    public void drawPieChart(double[] values, Color[] colors, boolean drawOutLine, boolean outlineSegments, boolean bevel) {
        double sum=0;
        for (double v:values) sum+=v;
        double start=0;
        Point center=new Point(translateX+graphwidth/2, translateY+graphheight/2);
        float radius=(graphwidth>graphheight)?graphwidth/2f:graphheight/2f;
        if (values.length>1) {
            for (int i=0;i<values.length;i++) {
                double angle=(values[i]/sum)*360.0;
                if (colors==null || colors[i]==null) g.setColor(new Color(0,0,0,0)); // use transparent color if undefined
                else {
                   if (bevel) {
                      Color[] gradcolors=new Color[]{colors[i],darker(colors[i],0.95),darker(colors[i],0.9),darker(colors[i],0.7)};
                      RadialGradientPaint paint=new RadialGradientPaint(center, radius, new float[]{0.8f,0.9f,0.95f,1f}, gradcolors);
                      g.setPaint(paint);
                   } else g.setColor(colors[i]);
                }
                Shape s = new Arc2D.Double(translateX, translateY, graphwidth, graphheight, start, angle, Arc2D.PIE);
                g.fill(s);
                if (outlineSegments) {
                    g.setColor(Color.BLACK);
                    g.draw(s);
                }
                start+=angle;
            }
        }
        if (drawOutLine) {
            g.setColor(Color.BLACK);
            g.drawOval(translateX, translateY, graphwidth, graphheight);
        }
    }
    
    public void drawPieChart(double[] values, Color[] colors, boolean drawOutLine) {
        drawPieChart(values, colors, drawOutLine, false, false);
    }    
    
    public void drawPieChartShadow(int offset) {
        g.translate(offset, offset);
        Point center=new Point(translateX+graphwidth/2, translateY+graphheight/2);
        float radius=(graphwidth>graphheight)?graphwidth/2f:graphheight/2f; 
        RadialGradientPaint paint=new RadialGradientPaint(center, radius, new float[]{0.85f,1f}, new Color[]{new Color(10,10,10,200),new Color(10,10,10,0)});
        g.setPaint(paint);      
        g.fillOval(translateX, translateY, graphwidth, graphheight); 
        g.translate(-offset,-offset);
    }

    /**
     * Draws a number at the given pixel-coordinate in the graph
     * This is used to draw standard tickmarks
     * @param number
     * @param x x-coordinate (in pixels)
     * @param y y-coordinate (in pixels)
     * @param alignX a value between 0 and 1 which determines the alignment along the horizontal axis (0=left aligned, 0.5=centered, 1=right aligned)
     * @param alignYa value between 0 and 1 which determines the alignment along the vertical axis (0=top, 0.5=centered, 1=bottom)
     * @param formatAsPercentage
     */
    public void drawAlignedNumber(double number, int x, int y, double alignX, double alignY, boolean formatAsPercentage, int decimals) {
        String text=formatNumber(number,formatAsPercentage, decimals);
        int stringlength=g.getFontMetrics().stringWidth(text);
        int ascent=g.getFontMetrics().getAscent();
        float minXcoordinate=(float)(x-stringlength*alignX);
        Object aliasing=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 
        g.drawString(text,minXcoordinate, (float)(y+ascent*alignY));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (aliasing!=null)?aliasing:RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    
    public void drawAlignedNumber(double number, int x, int y, double alignX, double alignY, boolean formatAsPercentage, int decimals, Color color) {
        Color save=null;
        if (color!=null) {
            save=g.getColor();
            g.setColor(color);
        }       
        drawAlignedNumber(number, x, y, alignX, alignY, formatAsPercentage, decimals);
        if (save!=null) g.setColor(save);       
    }    

    /**
     * Used by tick mark layout algorithm to test formatting of a tick label at a given position
     * @param number
     * @param x x-coordinate (in pixels)
     * @param y y-coordinate (in pixels)
     * @param alignX a value between 0 and 1 which determines the alignment along the horizontal axis (0=left aligned, 0.5=centered, 1=right aligned)
     * @param alignYa value between 0 and 1 which determines the alignment along the vertical axis (0=top, 0.5=centered, 1=bottom)
     * @param formatAsPercentage
     * @param allowedXcoordinates This specifies the range (min,max) of X-coordinates that the number can be drawn at, if the combination of x-position, x-alignment and length of drawn number is such that the parts or all of the number will be drawn outside this range, the number will not be drawn. This parameter can be used to avoid drawing overlapping tickmarks
     * @return the x-coordinates and y-coordinates (as float) of the left and right and top and bottom edges of the drawn number if it is drawn else null;
     */
    private float[] fitAlignedNumber(double number, int x, int y, double alignX, double alignY, boolean formatAsPercentage, float[] allowedXcoordinates, float[] allowedYcoordinates, int decimals) {
        String text=formatNumber(number,formatAsPercentage, decimals);
        FontMetrics metrics=g.getFontMetrics();
        int stringlength=metrics.stringWidth(text);
        int ascent=metrics.getAscent();
        int descent=metrics.getDescent();
        float baselineY=(float)(y+ascent*alignY);
        float minXcoordinate=(float)(x-stringlength*alignX);
        float maxXcoordinate=minXcoordinate+stringlength;
        float minYcoordinate=baselineY-ascent;
        float maxYcoordinate=baselineY+descent;  
        if (minXcoordinate<allowedXcoordinates[0] || maxXcoordinate>allowedXcoordinates[1]) return null;
        else if (minYcoordinate<allowedYcoordinates[0] || maxYcoordinate>allowedYcoordinates[1]) return null;
        else return new float[]{minXcoordinate,maxXcoordinate,minYcoordinate, maxYcoordinate};

   }       
    
    /** Returns a vertical gradient paint that starts at the current color, grows lighter and then goes back again */
    public static java.awt.Paint getVerticalGradientPaint(Color current, int height, int y) {
//       int red=current.getRed();
//       int blue=current.getBlue();
//       int green=current.getGreen();
//       Color lightercolor=new Color((int)((255-red)*0.65)+red,(int)((255-green)*0.65)+green,(int)((255-blue)*0.5)+blue);
        Color lightercolor=brighter(current,0.4f);
        return new java.awt.GradientPaint(0, y, current, 0, y+(height/2), lightercolor, true);
    }  
    
    /** Returns a horizontal gradient paint that starts at the current color, grows lighter and then goes back again */
    public static java.awt.Paint getHorizontalGradientPaint(Color current, int width, int x) {        
//       int red=current.getRed();
//       int blue=current.getBlue();
//       int green=current.getGreen();
//       Color lightercolor=new Color((int)((255-red)*0.65)+red,(int)((255-green)*0.65)+green,(int)((255-blue)*0.5)+blue);
       Color lightercolor=brighter(current,0.4f);       
       return new java.awt.GradientPaint(x, 0, current, x+(width/2),0, lightercolor, true);
    }    
    
    /** Returns a gradient paint that starts at the current color and ends with a brighter color
     *  @param direction This parameter should be a number between 0 and 12 which controls the clockwise direction of the gradient from dark to light
     */
    public static java.awt.Paint getDiagonalGradientPaint(Color current, int x, int y, int width, int height, int direction) {
//       int red=current.getRed();
//       int blue=current.getBlue();
//       int green=current.getGreen();
//       Color lightercolor=new Color((int)((255-red)*0.65)+red,(int)((255-green)*0.65)+green,(int)((255-blue)*0.5)+blue);
       Color lightercolor=brighter(current,0.4f);       
       direction=direction%12;
       if (direction==0)  return new java.awt.GradientPaint(x, y, lightercolor, x, y+height, current, false); // up
       else if (direction==1 || direction==2) return new java.awt.GradientPaint(x, y+height, current, x+width, y, lightercolor, false); // up right
       else if (direction==3) return new java.awt.GradientPaint(x, y, current, x+width, y, lightercolor, false); // right
       else if (direction==4 || direction==5) return new java.awt.GradientPaint(x, y, current, x+width, y+height, lightercolor, false); // down right
       else if (direction==6) return new java.awt.GradientPaint(x, y, current, x, y+height, lightercolor, false); // down
       else if (direction==7 || direction==8) return new java.awt.GradientPaint(x+width, y, current, x, y+height, lightercolor, false); // down left
       else if (direction==9) return new java.awt.GradientPaint(x+width, y, current, x, y, lightercolor, false); // left
       else if (direction==10 || direction==11) return new java.awt.GradientPaint(x+width, y+height, current, x, y, lightercolor, false); // up left
       return current;
    }       
    
    /**
     * Returns the width in pixels of the longest string in the list if drawn with the given font
     * (if the font parameter is null a default font will be used)
     */
    public static int findLongestString(String[] list, Font font) {
        BufferedImage image=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        FontMetrics metrics=(font!=null)?g.getFontMetrics(font):g.getFontMetrics();
        int max=0;
        for (String s:list) {
            int stringlength=metrics.stringWidth(s);
            if (stringlength>max) max=stringlength;
        }
        return max;
    }


    /**
     * Returns the width in pixels of the longest string in the list if drawn with the given font
     * (if the font parameter is null a default font will be used)
     */
    public static int findLongestString(ArrayList<String> list, Font font) {
        BufferedImage image=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        FontMetrics metrics=(font!=null)?g.getFontMetrics(font):g.getFontMetrics();
        int max=0;
        for (String s:list) {
            int stringlength=metrics.stringWidth(s);
            if (stringlength>max) max=stringlength;
        }
        return max;
    }

    private static double ceilToNearestX(double value, double tick) {
        int div=(int)(value/tick);
        return (div+1)*tick;
    }

    private static double roundToNearestX(double value, double tick) {
        int div=(int)(value/tick);
        double nearest=div*tick;
        if (Math.abs(value-div*tick)>Math.abs(value-(div+1)*tick)) nearest=(div+1)*tick;
        return nearest;
    }

    private static double ceilToNearestTenth(double value) {
        double tick=tick=1E-1;
        if (value<tick) tick=1E-5;     
        if (value<tick) tick=1E-10;        
        if (value<tick) tick=1E-20;
        if (value<tick) tick=1E-30;
        if (value<tick) tick=1E-50;
        if (value<tick) tick=1E-75;
        if (value<tick) tick=1E-100;
        if (value<tick) tick=1E-120;
        if (value<tick) tick=1E-140;
        if (value<tick) tick=1E-160;
        if (value<tick) tick=1E-180;
        if (value<tick) tick=1E-200;
        if (value<tick) tick=1E-240;
        if (value<tick) tick=1E-260;
        if (value<tick) tick=1E-280;
        if (value<tick) tick=1E-300;
        if (value<tick) tick=1E-320;

        while (tick*1E2<=value) {
            tick*=1E1; // multiply by 10
        }
        return tick;
    }

    /**
     * 
     * @param number
     * @param formatAsPercentage
     * @param decimals The minimum number of decimals to use
     * @return 
     */
    public static synchronized String formatNumber(Double number, boolean formatAsPercentage, int mindecimals) {
        if (number==null) return "";
        if (Double.isNaN(number)) return ""; //return "NaN";
        else if (number==0) return "0"+((formatAsPercentage)?"%":"");
        NumberFormat formatter;
        if (formatAsPercentage) formatter=percentageNumberFormat;
        else if (Math.abs(number) < 1E-5 || Math.abs(number) > 1E5) formatter=scientificNumberFormat; // note that #E-5 corresponds to the default number of decimals used
        else formatter=normalNumberFormat;
        formatter.setMinimumFractionDigits(mindecimals);
        String numberString=formatter.format(number);
        formatter.setMinimumFractionDigits(0); // restore the default setting of 0
        return numberString;
    }
    
    public static synchronized String formatNumber(Double number, boolean formatAsPercentage, int mindecimals, int maxdecimals) {
        if (number==null) return "";
        if (Double.isNaN(number)) return ""; //return "NaN";
        else if (number==0) return "0"+((formatAsPercentage)?"%":"");
        NumberFormat formatter;
        if (formatAsPercentage) formatter=percentageNumberFormat;
        else if (Math.abs(number) < 1E-5 || Math.abs(number) > 1E5) formatter=scientificNumberFormat; // note that #E-5 corresponds to the default number of decimals used
        else formatter=normalNumberFormat;
        formatter.setMinimumFractionDigits(mindecimals);
        formatter.setMaximumFractionDigits(maxdecimals);
        String numberString=formatter.format(number);
        formatter.setMinimumFractionDigits(0); // restore the default setting of 0
        return numberString;
    }    
    
    public static synchronized String formatNumber(Double number, boolean formatAsPercentage) {
        return formatNumber(number,formatAsPercentage,0);
    }    
   
    public static int countDecimals(String numberstring) {
        int dotpos=numberstring.indexOf('.');
        if (dotpos<0) return 0; // no decimals
        int count=0;
        for (int i=dotpos+1;i<numberstring.length();i++) {
            char c=numberstring.charAt(i);
            if (Character.isDigit(c)) count++;
            else break;
        }
        return count;
    }
    
    /** Returns the largest number of decimals needed when outputting the provided numbers */
    public static int getDecimalCount(ArrayList<Double> numbers, boolean formatAsPercentage) {
        int count=0;
        for (double number:numbers) {
            String numberstring=formatNumber(number, formatAsPercentage);
            int decimals=countDecimals(numberstring);
            if (decimals>count) count=decimals;
        }
        return count;
    }    
    
    /**
     * Given some information about a graph to be created, this static method will calculate the best values to use for various settings, such as image width and height 
     * @param fixedSize if TRUE, the imageWidth and imageHeight parameters will be held fixed and the graphWidth and graphHeight settings will be adjusted so that the graph fits within the image together with the other components
     *                  if FALSE, the graphWidth and graphHeight parameters will be used as the size of the graph and the imageWidth and imageHeight will be set to the minimal image size that can enclose the graph and all the other components
     * @param properties A map with specifications for various settings. At minimum, these should include either imageWidth+imageHeight or graphWidth+graphHeight and minX,maxX,minY,maxY
     * @param settings
     * @param graphics  The graphics context where the graph is to be drawn. If this is null, a dummy context based on a BufferedImage will be used
     * @return the same properties map as input but with some properties added or adjusted, including: imageWidth, imageHeight, graphWidth, graphHeight, translateX, translateY
     */
    public static HashMap<String,Object> layoutGraphImage(boolean fixedSize, HashMap<String,Object> properties, VisualizationSettings settings, Graphics2D graphics) {      
        int translateY=0; // the Y coordinate for the top of the graph itself
        int translateX=0; // the X coordinate for the leftmost side of the graph itself

        int labelYWidth=0; int labelYHeight=0;
        int labelXWidth=0; int labelXHeight=0;
        int yTicksWidth=0; int yTicksHeight=0;
        int xTicksWidth=0; int xTicksHeight=0;
        int legendWidth=0;
        int legendHeight=0;
        int titleWidth=0;
        int titleHeight=0;
        
        if (graphics==null) {
            BufferedImage image=new BufferedImage(10,10, BufferedImage.TYPE_INT_RGB);
            graphics=image.createGraphics();
        }        
                     
        int imageWidth=(Integer)getValueAsType(properties,"imageWidth",new Integer(100));
        int imageHeight=(Integer)getValueAsType(properties,"imageHeight",new Integer(100));
        int graphWidth=(Integer)getValueAsType(properties,"graphWidth",new Integer(100));
        int graphHeight=(Integer)getValueAsType(properties,"graphHeight",new Integer(100));
        
        double minX=(Double)getValueAsType(properties,"minX",new Double(0));
        double maxX=(Double)getValueAsType(properties,"maxX",new Double(1));
        double minY=(Double)getValueAsType(properties,"minY",new Double(0));
        double maxY=(Double)getValueAsType(properties,"maxY",new Double(1));
        
        int yLabelMargin=(Integer)getValueAsType(properties,"yLabelMargin",new Integer(10));         
        int xLabelMargin=(Integer)getValueAsType(properties,"xLabelMargin",new Integer(10));      
        int yTicksMargin=(Integer)getValueAsType(properties,"yTicksMargin",new Integer(10));         
        int xTicksMargin=(Integer)getValueAsType(properties,"xTicksMargin",new Integer(10));
        int titleMargin=(Integer)getValueAsType(properties,"titleMargin",new Integer(10));    
        int margin=(Integer)getValueAsType(properties,"margin",new Integer(5));          

        boolean drawXticks=(Boolean)getValueAsType(properties,"drawXticks",Boolean.TRUE);
        boolean drawYticks=(Boolean)getValueAsType(properties,"drawYticks",Boolean.TRUE);           
        boolean formatAsPercentageX=(Boolean)getValueAsType(properties,"formatAsPercentageX",Boolean.FALSE);
        boolean formatAsPercentageY=(Boolean)getValueAsType(properties,"formatAsPercentageY",Boolean.FALSE);    
        boolean verticalXticks=(Boolean)getValueAsType(properties,"verticalXticks",Boolean.FALSE);           
        
        HashMap<Double,String> xTicks=(HashMap<Double,String>)getValueAsType(properties,"xTickLabels",null);
        HashMap<Double,String> yTicks=(HashMap<Double,String>)getValueAsType(properties,"yTickLabels",null);
        if (xTicks!=null) drawXticks=true;
        if (yTicks!=null) drawYticks=true;
        
        String xLabel=(String)getValueAsType(properties,"xLabel",null);         
        String yLabel=(String)getValueAsType(properties,"yLabel",null);        
        String title=(String)getValueAsType(properties,"title",null);      
        
        int axisLayout=(Integer)getValueAsType(properties,"axisLayout",Graph.BOX); 
        
        int legendMargin=(Integer)getValueAsType(properties,"legendMargin",new Integer(20));
        int legendSpacing=(Integer)getValueAsType(properties,"legendSpacing",new Integer(10));        
        String[] legend=(String[])getValueAsType(properties,"legend",null);      
        boolean horizontalLegend=(Boolean)getValueAsType(properties,"legendHorizontal",Boolean.FALSE);         
        String legendPlacement=(String)getValueAsType(properties,"legendPlacement","right"); 
        if (legend==null || legend.length==0) legendPlacement="none";
        
        Font legendFont=settings.getSystemFont("graph.legendFont"); // used in the legend box
        Font titleFont=settings.getSystemFont("graph.titleFont"); // used for the title above the graph       
        Font labelFont=settings.getSystemFont("graph.labelFont"); // used for axis labels (one on each axis)
        Font tickFont=settings.getSystemFont("graph.tickFont"); // used for tick mark label
        
        FontMetrics metrics=graphics.getFontMetrics(tickFont);        
        // find the width and height of the Y-axis ticks
        if (drawYticks) {
            if (yTicks!=null) { // explicit list of tick labels
                Iterator<String> iter=yTicks.values().iterator();
                while(iter.hasNext()) {
                    String tickString=iter.next();
                    Rectangle2D rect=metrics.getStringBounds(tickString, graphics);
                    if (rect.getHeight()>yTicksHeight) yTicksHeight=(int)rect.getHeight();
                    if (rect.getWidth()>yTicksWidth) yTicksWidth=(int)rect.getWidth();
                }
            } else {
                ArrayList<Double> ticksY=Graph.getVerticalTicks(minY,maxY);
                int decimals=Graph.getDecimalCount(ticksY,formatAsPercentageY);
                for (double tick:ticksY) {
                    String tickString=Graph.formatNumber(tick, formatAsPercentageY, decimals);
                    Rectangle2D rect=metrics.getStringBounds(tickString, graphics);
                    if (rect.getHeight()>yTicksHeight) yTicksHeight=(int)rect.getHeight();
                    if (rect.getWidth()>yTicksWidth) yTicksWidth=(int)rect.getWidth();
                }             
            }
            if (yTicksWidth>0) yTicksWidth+=yTicksMargin; // this margin should allow for the tick line and some extra spacing
        }
        
        // find the width and height of the X-axis ticks   
        if (drawXticks) {
            if (xTicks!=null) { // explicit list of tick labels
                Iterator<String> iter=xTicks.values().iterator();
                while(iter.hasNext()) {
                    String tickString=iter.next();
                    Rectangle2D rect=metrics.getStringBounds(tickString, graphics);
                    if (verticalXticks) {
                        if (rect.getHeight()>xTicksWidth) xTicksWidth=(int)rect.getHeight();
                        if (rect.getWidth()>xTicksHeight) xTicksHeight=(int)rect.getWidth();                    
                    } else {
                        if (rect.getHeight()>xTicksHeight) xTicksHeight=(int)rect.getHeight();
                        if (rect.getWidth()>xTicksWidth) xTicksWidth=(int)rect.getWidth();
                    }
                }
            } else {
                ArrayList<Double> ticksX=Graph.getHorizontalTicks(minX,maxX);
                int decimals=Graph.getDecimalCount(ticksX,formatAsPercentageX);
                for (double tick:ticksX) {
                    String tickString=Graph.formatNumber(tick, formatAsPercentageX, decimals);
                    Rectangle2D rect=metrics.getStringBounds(tickString, graphics);
                    if (verticalXticks) {
                        if (rect.getHeight()>xTicksWidth) xTicksWidth=(int)rect.getHeight();
                        if (rect.getWidth()>xTicksHeight) xTicksHeight=(int)rect.getWidth();                    
                    } else {
                        if (rect.getHeight()>xTicksHeight) xTicksHeight=(int)rect.getHeight();
                        if (rect.getWidth()>xTicksWidth) xTicksWidth=(int)rect.getWidth();
                    }
                }             
            }  
            if (xTicksHeight>0) xTicksHeight+=xTicksMargin; // this margin should allow for the tick line and some extra spacing
        }
        if (yLabel!=null && !yLabel.isEmpty()) {
            metrics=graphics.getFontMetrics(labelFont);        
            Rectangle2D rect=metrics.getStringBounds(yLabel, graphics);      
            labelYWidth=(int)rect.getHeight(); // since yLabel is drawn in vertical direction, we switch width and height 
            labelYHeight=(int)rect.getWidth(); //  - " -
        }
        if (xLabel!=null && !xLabel.isEmpty()) {
            metrics=graphics.getFontMetrics(labelFont);        
            Rectangle2D rect=metrics.getStringBounds(xLabel, graphics);      
            labelXWidth=(int)rect.getWidth();
            labelXHeight=(int)rect.getHeight();
        }  
        if (title!=null && !title.isEmpty()) {
            metrics=graphics.getFontMetrics(titleFont);        
            Rectangle2D rect=metrics.getStringBounds(title, graphics);      
            titleWidth=(int)rect.getWidth();
            titleHeight=(int)rect.getHeight();
        }       
        if (legend!=null && legend.length!=0) {
            Dimension dim=(horizontalLegend)?getHorizontalLegendDimension(legend, legendSpacing, legendFont, graphics):getLegendDimension(legend, legendFont, graphics);             
            legendWidth=dim.width;
            legendHeight=dim.height;
        }   
        // calculate translateX and translateY, then derive either image or graph sizes later
        translateY=margin;
        if (titleHeight>0) translateY+=(titleHeight+titleMargin);
        if (legendPlacement.equals("top")) translateY+=(legendHeight+legendMargin);
        if (yTicksHeight>0) translateY+=(int)(yTicksHeight/2)+2; // make some extra room for potentially overshooting ticks.

        translateX=margin;
        if (legendPlacement.equals("left")) translateX+=(legendWidth+legendMargin);        
        if (labelYWidth>0) translateX+=(labelYWidth+yLabelMargin);
        // if (axisLayout==Graph.CROSS) it would be nice to do a test to see how much the yAxis ticks actually extend to the left of the axis line
        // Perhaps there is no need to further add to translateX if the tick labels already fit within the axis system.
        // But alas, that is not possible to do at this point... :-(
        translateX+=yTicksWidth; // yTicksWidth already includes yTicksMargin if yTicksWidth>0     
        if (yTicksWidth==0 && xTicksWidth>0) translateX+=(int)(xTicksWidth/2)+5; // make some extra room for potentially overshooting ticks from the X-axis (tick labels on the x-axis that extend beyond the left edge of the graph).
        
        if (fixedSize) { // adjust graphWidth and graphHeight to fit within image. Find space that is left in image after all other components have been placed
            int usedWidth=translateX+margin; // margin is on the right side. translateX already includes left margin
            if (legendPlacement.equals("right")) usedWidth+=(legendWidth+legendMargin);
            if (xTicksWidth>0) usedWidth+=(int)(xTicksWidth/2);
            graphWidth=imageWidth-usedWidth;
            if (graphWidth<1) graphWidth=1;
            int usedHeight=translateY+margin; // margin is on the bottom. translateY already includes top margin
            if (yTicksHeight>0 && xTicksHeight==0) usedHeight+=(int)(yTicksHeight/2); // make some extra room for potentially overshooting ticks beneath the graph
            usedHeight+=xTicksHeight; // xTicksHeight already includes xTicksMargin if xTicksHeight>0
            if (labelXHeight>0) usedHeight+=(labelXHeight+xLabelMargin);            
            if (legendPlacement.equals("bottom")) usedHeight+=(legendHeight+legendMargin);
            graphHeight=imageHeight-usedHeight;
            if (graphHeight<1) graphHeight=1;            
        } else { // set imageWidth and imageHeight to enclose all components
            imageWidth=translateX+graphWidth; // translateX already accounts for left margin, yTicks, yLabel and left-side legend
            if (xTicksWidth>0) imageWidth+=(int)(xTicksWidth/2); // make some extra room on the right for potentially overshooting ticks.
            if (legendPlacement.equals("right")) imageWidth+=(legendWidth+legendMargin);
            else if (legendPlacement.equals("top") || legendPlacement.equals("bottom")) {
               if (legendWidth>graphWidth) imageWidth+=(legendWidth-graphWidth); // adjust for overshooting legend box 
            }
            if (titleWidth>graphWidth) { 
                // should we adjust for long title or long x-label?
            }
            imageHeight=translateY+graphHeight; // translateX already accounts for top margin, title and overshoot above graph
            int belowGraph=0;
            if (yTicksHeight>0 && xTicksHeight==0) belowGraph+=(int)(yTicksHeight/2); // make some extra room for potentially overshooting ticks beneath the graph
            belowGraph+=xTicksHeight; // xTicksHeight already includes xTicksMargin if xTicksHeight>0
            if (labelXHeight>0) belowGraph+=(labelXHeight+xLabelMargin);
            imageHeight+=belowGraph;
            if (legendPlacement.equals("bottom")) imageHeight+=(legendHeight+legendMargin);
            else if (legendPlacement.equals("left") || legendPlacement.equals("right")) {
               if (legendHeight>(graphHeight+belowGraph)) imageHeight+=(legendHeight-(graphHeight+belowGraph)); // adjust for overshooting legend box 
            }
            imageHeight+=margin;
        }
        
        // now return all the properties
        properties.put("yTicksWidth",yTicksWidth);
        properties.put("yTicksHeight",yTicksHeight);  
        properties.put("xTicksWidth",xTicksWidth);
        properties.put("xTicksHeight",xTicksHeight);  
        properties.put("yTicksMargin",yTicksMargin);
        properties.put("xTicksMargin",xTicksMargin);   
        properties.put("titleMargin",titleMargin);            
        
        properties.put("labelYWidth",labelYWidth);
        properties.put("labelYHeight",labelYHeight);  
        properties.put("labelXWidth",labelXWidth);
        properties.put("labelXHeight",labelXHeight);    
        properties.put("labelYMargin",yLabelMargin);
        properties.put("labelXMargin",xLabelMargin);         
        
        properties.put("legendMargin",legendMargin);
        properties.put("legendSpacing",legendSpacing);
        properties.put("legendWidth",legendWidth);
        properties.put("legendHeight",legendHeight);  
        
        properties.put("titleWidth",titleWidth);
        properties.put("titleHeight",titleHeight);           
        
        properties.put("imageWidth",imageWidth);
        properties.put("imageHeight",imageHeight);
        properties.put("graphWidth",graphWidth);
        properties.put("graphHeight",graphHeight);
        properties.put("translateX",translateX);
        properties.put("translateY",translateY);     
        // for (String key:properties.keySet()) System.err.println(key+" = "+properties.get(key));
        return properties;
    } 
    
    private static Object getValueAsType(HashMap<String, Object> settings, String property, Object defaultValue) {
        if (!settings.containsKey(property)) return defaultValue;
        Object value=settings.get(property);
        if (defaultValue==null) return value;
        if (value==null) return defaultValue;
        if (defaultValue.getClass().isAssignableFrom(value.getClass())) return value;
        else return defaultValue;
    }
    
    /** Draws a "blank" graph according to the provided layout specifications, 
     *  which should have been made by a call to layoutGraphImage()
     *  The drawn graph includes the graph box with optional axes, ticks, labels, title and legend
     *  but no actual graph content.
     */
    public void drawAxesAndStuff(HashMap<String,Object> properties, VisualizationSettings settings) {     
        int yLabelMargin=(Integer)getValueAsType(properties,"yLabelMargin",new Integer(10));         
        int xLabelMargin=(Integer)getValueAsType(properties,"xLabelMargin",new Integer(10));      
        int yTicksWidth=(Integer)getValueAsType(properties,"yTicksWidth",new Integer(10));         
        int xTicksHeight=(Integer)getValueAsType(properties,"xTicksHeight",new Integer(10));        
        int titleMargin=(Integer)getValueAsType(properties,"titleMargin",new Integer(10));    
        int margin=(Integer)getValueAsType(properties,"margin",new Integer(5));   
        int imageHeight=(Integer)getValueAsType(properties,"imageHeight",new Integer(100));          

        boolean drawXticks=(Boolean)getValueAsType(properties,"drawXticks",Boolean.TRUE);
        boolean drawYticks=(Boolean)getValueAsType(properties,"drawYticks",Boolean.TRUE);           
        boolean formatAsPercentageX=(Boolean)getValueAsType(properties,"formatAsPercentageX",Boolean.FALSE);
        boolean formatAsPercentageY=(Boolean)getValueAsType(properties,"formatAsPercentageY",Boolean.FALSE);    
        boolean verticalXticks=(Boolean)getValueAsType(properties,"verticalXticks",Boolean.FALSE);    
        boolean verticalXticksUp=(Boolean)getValueAsType(properties,"verticalXticksUp",Boolean.FALSE);          
        
        boolean boundingBox=(Boolean)getValueAsType(properties,"box",Boolean.TRUE);  
        int horizontalGrid=(Integer)getValueAsType(properties,"horizontalGrid",Graph.DOTTED); 
        int verticalGrid=(Integer)getValueAsType(properties,"verticalGrid",Graph.DOTTED); 
        double[] horizontalGridLines=(double[])getValueAsType(properties,"horizontalGridLines",null); 
        double[] verticalGridLines=(double[])getValueAsType(properties,"verticalGridLines",null); 
        int axisLayout=(Integer)getValueAsType(properties,"axisLayout",Graph.BOX);        
              
        HashMap<Double,String> xTicks=(HashMap<Double,String>)getValueAsType(properties,"xTickLabels",null);
        HashMap<Double,String> yTicks=(HashMap<Double,String>)getValueAsType(properties,"yTickLabels",null);
        if (xTicks!=null) drawXticks=false; // Do not draw regular ticks if they are drawn explicitly. 
        if (yTicks!=null) drawYticks=false; // Note that these two lines are opposite from layoutGraphImage()
        
        String xLabel=(String)getValueAsType(properties,"xLabel",null);         
        String yLabel=(String)getValueAsType(properties,"yLabel",null);        
        String title=(String)getValueAsType(properties,"title",null);         
        
        int legendMargin=(Integer)getValueAsType(properties,"legendMargin",new Integer(10));
        int legendSpacing=(Integer)getValueAsType(properties,"legendSpacing",new Integer(10));  
        int legendHeight=(Integer)getValueAsType(properties,"legendHeight",new Integer(10));         
        String[] legend=(String[])getValueAsType(properties,"legend",null);      
        Color[] legendColors=(Color[])getValueAsType(properties,"legendColors",null);      
        boolean horizontalLegend=(Boolean)getValueAsType(properties,"legendHorizontal",Boolean.FALSE);  
        boolean legendBoundingBox=(Boolean)getValueAsType(properties,"legendBox",Boolean.TRUE); 
        String legendPlacement=(String)getValueAsType(properties,"legendPlacement","right"); 
        if (legend==null || legend.length==0 || legendColors==null || legendColors.length!=legend.length) legendPlacement="none";
        
        Font legendFont=settings.getSystemFont("graph.legendFont"); // used in the legend box
        Font titleFont=settings.getSystemFont("graph.titleFont"); // used for the title above the graph       
        Font labelFont=settings.getSystemFont("graph.labelFont"); // used for axis labels (one on each axis)
        Font tickFont=settings.getSystemFont("graph.tickFont"); // used for tick mark label        
        Font storedFont=g.getFont();        
        g.setFont(tickFont);
        // if gridlines are set explicitly, draw these first so that axes and bounding boxes will be drawn on top later      
        if (horizontalGridLines!=null) {
            for (double value:horizontalGridLines) drawHorizontalGridLine(getYforValue(value),horizontalGrid,lightGray);         
        }        
        if (verticalGridLines!=null) {            
            for (double value:verticalGridLines) drawVerticalGridLine(getXforValue(value),verticalGrid,lightGray);           
        }            
        drawAxes(axisLayout, (horizontalGridLines!=null)?NONE:horizontalGrid, (verticalGridLines!=null)?NONE:verticalGrid, boundingBox, drawXticks, drawYticks, formatAsPercentageX, formatAsPercentageY);          
        if (xTicks!=null) { // draw explicit tick labels. Only works well for boxed axes
            for (Double value:xTicks.keySet()) {
                String label=xTicks.get(value);
                if (verticalXticks) drawVerticalStringXTick(label, getXforValue(value), translateY+graphheight, verticalXticksUp);
                else drawHorizontalStringXTick(label, getXforValue(value), translateY+graphheight);
            }
        }      
        if (yTicks!=null) { // draw explicit tick labels. Only works ewll for boxed axes
            for (Double value:yTicks.keySet()) {
                String label=yTicks.get(value);
                drawHorizontalStringYTick(label, translateX, getYforValue(value));
            }
        }           
        if (yLabel!=null && !yLabel.isEmpty()) {
            int yPos=translateY+graphheight/2;
            int xPos=translateX;
            xPos-=(yLabelMargin+yTicksWidth);
            g.setFont(labelFont);
            drawVerticalAlignedString(yLabel, xPos, yPos, 0.5, 0, true);
        }
        if (xLabel!=null && !xLabel.isEmpty()) {
            int yPos=translateY+graphheight;
            int xPos=translateX+graphwidth/2;
            yPos+=(xLabelMargin+xTicksHeight);
            g.setFont(labelFont);
            drawAlignedString(xLabel, xPos, yPos, 0.5, 1);
        }  
        if (title!=null && !title.isEmpty()) {
            int yPos=margin;
            if (legendHeight>0 && legendPlacement.equalsIgnoreCase("top")) yPos+=(legendHeight+legendMargin);
            int xPos=translateX+graphwidth/2;
            g.setFont(titleFont);
            drawAlignedString(title, xPos, yPos, 0.5, 1);
        }     
        if (!legendPlacement.equalsIgnoreCase("none")) {
            int legendX=0;
            int legendY=0;
            if (legendPlacement.equalsIgnoreCase("left")) {
               legendX=margin;
               legendY=translateY;
            } else if (legendPlacement.equalsIgnoreCase("top")) {
              legendX=translateX;
              legendY=margin;
            } else if (legendPlacement.equalsIgnoreCase("bottom")) {
              legendX=translateX;
              legendY=imageHeight-(margin+legendHeight); // easier this way            
            } else { // placement right
               legendX=translateX+graphwidth+legendMargin;
               legendY=translateY;                
            }
            g.setFont(legendFont);
            if (horizontalLegend) drawHorizontalLegendBox(legend, legendColors, legendX, legendY, legendSpacing, legendBoundingBox);
            else drawLegendBox(legend, legendColors, legendX, legendY, legendBoundingBox);
        }
        g.setFont(storedFont);
    } 
 
   /** This method implements a different way to return a brighter color than the on inside the Color class
     * (since that does not work for "pure" colors where some channels are 0
     */
   public static Color brighter(Color color, float factor) {
      int red=color.getRed();
      int green=color.getGreen();
      int blue=color.getBlue();
      if (red==0 && green==0 && blue==0) return Color.GRAY; // just to get things started      
      if (red==green && red==blue) return oldBrighter(color, factor); // use this implementation to avoid strange hues for grayscale colors
      float[] hsb=Color.RGBtoHSB(red,green,blue,null);
      if (hsb[2]<1.0) { // increase brightness
          hsb[2]=hsb[2]/factor; // this will increase the value
          if (hsb[2]>1.0) hsb[2]=1.0f;
      } else if (hsb[1]>0) { // if maximum brightness, lower saturation
          hsb[1]=hsb[1]*factor; // 
          if (hsb[1]<0) hsb[1]=0f;    
      }
      return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
   }
   
   public static Color brighter(Color color) {
       return brighter(color, 0.7f);
   }

    public static Color oldBrighter(Color color, float FACTOR) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int alpha = color.getAlpha();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int)(1.0/(1.0-FACTOR));
        if ( r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return new Color(Math.min((int)(r/FACTOR), 255),
                         Math.min((int)(g/FACTOR), 255),
                         Math.min((int)(b/FACTOR), 255),
                         alpha);
    }   
   
   /** This is the same as the darker() method in the Color class, except that the FACTOR (default=0.7) is made explicit */
   public static Color darker(Color color, double factor) {
       if (color==null) return null;
       return new Color(Math.max((int)(color.getRed()*factor), 0), Math.max((int)(color.getGreen()*factor), 0), Math.max((int)(color.getBlue()*factor), 0), color.getAlpha());
   }
   
  public static Color getColorFromSetting(String value, Color original) throws ExecutionError {
      if (value.equalsIgnoreCase("color")) return original;
      else if (value.equalsIgnoreCase("translucent") || value.equalsIgnoreCase("transparent")) return (original!=null)?(new Color(original.getRed(),original.getGreen(),original.getBlue(),80)):original;
      else if (value.equalsIgnoreCase("darker")) return (original!=null)?original.darker():original;
      else if (value.equalsIgnoreCase("brighter")) return (original!=null)?Graph.brighter(original):original;
      else {
          Color color=null;
          try {
              color=VisualizationSettings.parseColor(value);
          } catch (ParseError e) {
              throw new ExecutionError(e.getMessage());
          } 
          if (color!=null) return color;
          else return (original!=null)?original:Color.BLACK;
      }
  }   
   
}
