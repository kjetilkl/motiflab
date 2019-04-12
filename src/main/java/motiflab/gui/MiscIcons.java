/*
 * 
 */

package motiflab.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.Icon;



/**
 * This class is an implementation of Icon that can draw different
 * simple icons that are used at various places
 * 
 * @author kjetikl
 */
public class MiscIcons implements Icon {
    public static final int CLOSE_ICON=0;
    public static final int ERROR_ICON=1;
    public static final int LEFT_ARROW=2;
    public static final int RIGHT_ARROW=3;
    public static final int TREE_GROUPS=4;
    public static final int ADD_DATA=5;
    public static final int UPREGULATED=6;
    public static final int DOWNREGULATED=7;
    public static final int UP_TRIANGLE=8;
    public static final int DOWN_TRIANGLE=9;
    public static final int PLUS_ICON=10;
    public static final int XCROSS_ICON=11;
    public static final int BOX_WITH_PLUS=12;
    public static final int BOX_WITH_MINUS=13;
    public static final int FLAT_FILLED=14;
    public static final int VERTICAL_GRADIENT_FILLED=15;
    public static final int HORIZONTAL_GRADIENT_FILLED=16;
    public static final int GREATER_THAN_OR_EQUAL=17;
    public static final int SMALLER_THAN_OR_EQUAL=18;   
    public static final int TREE_LINE=19;    
    public static final int LEFT_ARROW_LONG=20;    
    public static final int RIGHT_ARROW_LONG=21; 
    public static final int LOGGER_LEVEL=22; 
    public static final int ELLIPSIS=23;
    public static final int BULLET_MARK=24;
    
    public static final Polygon upregulatedPolygon =new Polygon(new int[]{5,0,0,2,2,9,9,11,11,6}, new int[]{0,5,6,6,10,10,6,6,5,0}, 10);
    public static final Polygon downregulatedPolygon=new Polygon(new int[]{6,11,11,9,9,2,2,0,0,5}, new int[]{10,5,4,4,0,0,4,4,5,10}, 10);


    private int type=0;
    private Color foregroundcolor=Color.black;
    private Color fillcolor=Color.black;
    private int setheight=-1;
    private int setwidth=-1;
    private Object property=null;

    
    public MiscIcons(int type) {
        this.type=type;
    }
    
    public MiscIcons(int type, Color color) {
        this.type=type;
        this.foregroundcolor=color;
    }    

    @Override
    public int getIconHeight() {
        if (setheight>0) return setheight;
        switch(type) {       
          case CLOSE_ICON: return 12;
          case LEFT_ARROW: case RIGHT_ARROW: return 13;
          case LEFT_ARROW_LONG: case RIGHT_ARROW_LONG: return 13;
          case TREE_GROUPS: return 10;
          case ADD_DATA: return 10;
          case DOWN_TRIANGLE: return 5;
          case UP_TRIANGLE: return 5;
          case XCROSS_ICON: return 7;
          case UPREGULATED: case DOWNREGULATED: return 11;
          case BOX_WITH_PLUS: case BOX_WITH_MINUS: return 9;
          case GREATER_THAN_OR_EQUAL: case SMALLER_THAN_OR_EQUAL: return 7;
          case TREE_LINE: return 10;
          case LOGGER_LEVEL: return 15;  
          case ELLIPSIS: return 8;
          case BULLET_MARK: return 8;              
          default: return 8;
        }
    }

    @Override
    public int getIconWidth() {
        if (setwidth>0) return setwidth;
        switch(type) {       
          case CLOSE_ICON: return 12;
          case LEFT_ARROW: case RIGHT_ARROW: return 13;
          case LEFT_ARROW_LONG: case RIGHT_ARROW_LONG: return 20;
          case DOWN_TRIANGLE: return 9;
          case UP_TRIANGLE: return 9;
          case XCROSS_ICON: return 8;
          case UPREGULATED: case DOWNREGULATED: return 12;
          case BOX_WITH_PLUS: case BOX_WITH_MINUS: return 9; 
          case GREATER_THAN_OR_EQUAL: case SMALLER_THAN_OR_EQUAL: return 11;     
          case TREE_LINE: return 10;     
          case LOGGER_LEVEL: return 15;    
          case ELLIPSIS: return 14;
          case BULLET_MARK: return 8;               
          default: return 8;
        }
    }

    public int getIconType() {
        return type;
    }
    
    public void setIconType(int type) {
        this.type=type;
    }
    
    public void setForegroundColor(Color color) {
        this.foregroundcolor=color;
    }
    public void setFillColor(Color color) {
        this.fillcolor=color;
    }
    
    public void setProperty(Object value) {
        this.property=value;
    }    

    public void setSize(int width, int height) {
        this.setwidth=width;
        this.setheight=height;
    }
    
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      switch(type) {
          case CLOSE_ICON: drawCloseIcon(g, x, y);break;          
          case ERROR_ICON: drawErrorIcon(g, x, y);break;          
          case LEFT_ARROW: drawLeftArrowIcon(g, x, y);break;          
          case RIGHT_ARROW: drawRightArrowIcon(g, x, y);break;          
          case TREE_GROUPS: drawTreeGroupsIcon(g, x, y);break;          
          case ADD_DATA: drawAddDataIcon(g, x, y);break;
          case UPREGULATED: drawUpregulatedIcon(g, x, y);break;
          case DOWNREGULATED: drawDownregulatedIcon(g, x, y);break;
          case DOWN_TRIANGLE: drawDownTriangleIcon(g, x, y);break;
          case UP_TRIANGLE: drawUpTriangleIcon(g, x, y);break;
          case PLUS_ICON: drawPlusIcon(g, x, y);break;
          case XCROSS_ICON: drawXcrossIcon(g, x, y);break;
          case BOX_WITH_MINUS: drawBoxWithSign(g, x, y,false);break;              
          case BOX_WITH_PLUS: drawBoxWithSign(g, x, y,true);break;
          case FLAT_FILLED: drawFilled(g,x,y,0);break;
          case VERTICAL_GRADIENT_FILLED: drawFilled(g,x,y,1);break;
          case HORIZONTAL_GRADIENT_FILLED: drawFilled(g,x,y,2);break;
          case GREATER_THAN_OR_EQUAL: drawOperator(g,x,y,true);break;
          case SMALLER_THAN_OR_EQUAL: drawOperator(g,x,y,false);break;  
          case LEFT_ARROW_LONG: drawLeftArrowLongIcon(g, x, y);break;          
          case RIGHT_ARROW_LONG: drawRightArrowLongIcon(g, x, y);break;                  
          case TREE_LINE: drawTreeLine(g,x,y);break;     
          case LOGGER_LEVEL: drawLoggerLevel(g,x,y);break;        
          case ELLIPSIS: drawEllipsis(g,x,y);break;  
          case BULLET_MARK: drawBulletMark(g,x,y);break;               
      }
    }
    
    private void drawCloseIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(x+2,y+2,x+8,y+8); // down left-right
         g.drawLine(x+2,y+3,x+7,y+8); // down left-right 
         g.drawLine(x+3,y+2,x+8,y+7); // down left-right     
         g.drawLine(x+2,y+7,x+7,y+2); // up left-right 
         g.drawLine(x+2,y+8,x+8,y+2); // up left-right 
         g.drawLine(x+3,y+8,x+8,y+3); // up left-right 
         g.drawRect(x+0, x+0, 10, 10);
    }
    
    private void drawErrorIcon(Graphics g, int x, int y) {
         g.setColor(Color.RED);   
         g.fillRect(x+1,y+1,6,6); // 
         g.drawLine(x+2,y+0,x+5,y+0); // 
         g.drawLine(x+2,y+7,x+5,y+7); //      
         g.drawLine(x+0,y+2,x+0,y+5); //
         g.drawLine(x+7,y+2,x+7,y+5); // 
         g.setColor(Color.WHITE);
         g.drawLine(x+3,y+1,x+3,y+4); // up left-right 
         g.drawLine(x+4,y+1,x+4,y+4); // up left-right 
         g.drawLine(x+3,y+6,x+4,y+6); // up left-right 

    }

    private void drawLeftArrowIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(x+0,y+6,x+0,y+6); // 
         g.drawLine(x+1,y+5,x+1,y+7); // 
         g.drawLine(x+2,y+4,x+2,y+8); // 
         g.drawLine(x+3,y+3,x+3,y+9); // 
         g.drawLine(x+4,y+2,x+4,y+10); // 
         g.drawLine(x+5,y+1,x+5,y+11); //          
         g.fillRect(x+6,y+3,7,7); //      
    }

    private void drawRightArrowIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(x+12,y+6,x+12,y+6); // 
         g.drawLine(x+11,y+5,x+11,y+7); // 
         g.drawLine(x+10,y+4,x+10,y+8); // 
         g.drawLine(x+9,y+3,x+9,y+9); // 
         g.drawLine(x+8,y+2,x+8,y+10); // 
         g.drawLine(x+7,y+1,x+7,y+11); //          
         g.fillRect(x,y+3,7,7); // 
    }
    
    private void drawLeftArrowLongIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);  
         g.fillRect(x+6,y+5,12,3); //  horisontal line           
         x+=2; // offset
         g.drawLine(x+0,y+6,x+0,y+6); // 
         g.drawLine(x+1,y+5,x+1,y+7); // 
         g.drawLine(x+2,y+4,x+2,y+8); // 
         g.drawLine(x+3,y+3,x+3,y+9); // 
         g.drawLine(x+4,y+2,x+4,y+10); // 
         //g.drawLine(x+5,y+1,x+5,y+11); //            
    }

    private void drawRightArrowLongIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);  
         g.fillRect(x+2,y+5,14,3); // //  horisontal line         
         x+=5; // offset
         g.drawLine(x+12,y+6,x+12,y+6); // 
         g.drawLine(x+11,y+5,x+11,y+7); // 
         g.drawLine(x+10,y+4,x+10,y+8); // 
         g.drawLine(x+9,y+3,x+9,y+9); // 
         g.drawLine(x+8,y+2,x+8,y+10); // 
         //g.drawLine(x+7,y+1,x+7,y+11); //            
    }    

    private void drawTreeGroupsIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(x,y,x+1,y); // 
         g.drawLine(x+1,y,x+1,y+8); // 
         g.drawLine(x+1,y+3,x+3,y+3); // 
         g.drawLine(x+1,y+8,x+3,y+8); // 
         g.drawLine(x+5,y+2,x+7,y+2); // 
         g.drawLine(x+5,y+3,x+7,y+3); // 
         g.drawLine(x+5,y+4,x+7,y+4); // 
         g.drawLine(x+5,y+7,x+7,y+7); // 
         g.drawLine(x+5,y+8,x+7,y+8); // 
         g.drawLine(x+5,y+9,x+7,y+9); // 
    }

    private void drawAddDataIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawLine(x+5,y+3,x+5,y+7); // vertical
         g.drawLine(x+3,y+5,x+7,y+5); // horisontal
    }

    private void drawUpregulatedIcon(Graphics g, int x, int y) {
         g.translate(x, y);
         g.setColor(fillcolor);
         g.fillPolygon(upregulatedPolygon);
         g.setColor(foregroundcolor);
         g.drawPolygon(upregulatedPolygon);
         g.translate(-x, -y);
    }

    private void drawDownregulatedIcon(Graphics g, int x, int y) {
         g.translate(x, y);
         g.setColor(fillcolor);
         g.fillPolygon(downregulatedPolygon);
         g.setColor(foregroundcolor);
         g.drawPolygon(downregulatedPolygon);
         g.translate(-x, -y);
    }

    private void drawDownTriangleIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawLine(x,y,x+8,y);
         g.drawLine(x+1,y+1,x+7,y+1);
         g.drawLine(x+2,y+2,x+6,y+2);
         g.drawLine(x+3,y+3,x+5,y+3);
         g.drawLine(x+4,y+4,x+4,y+4);
    }
    private void drawUpTriangleIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawLine(x,y+4,x+8,y+4);
         g.drawLine(x+1,y+3,x+7,y+3);
         g.drawLine(x+2,y+2,x+6,y+2);
         g.drawLine(x+3,y+1,x+5,y+1);
         g.drawLine(x+4,y,x+4,y);
    }
    
    private void drawPlusIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawLine(x+4,y+1,x+4,y+7);
         g.drawLine(x+1,y+4,x+7,y+4);
    }
    
    private void drawXcrossIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawLine(x,y,x+6,y+6);
         g.drawLine(x+1,y,x+7,y+6);
         g.drawLine(x,y+6,x+6,y);
         g.drawLine(x+1,y+6,x+7,y);
    }
    
    private void drawBoxWithSign(Graphics g, int x, int y, boolean drawPlus) {
         g.setColor(foregroundcolor);
         g.drawRect(x, y, 8, 8);
         g.drawLine(x+2,y+4,x+6,y+4);
         if (drawPlus) g.drawLine(x+4,y+2,x+4,y+6);
    } 
    
    private void drawFilled(Graphics g, int x, int y, int fill) {
        int width=getIconWidth();
        int height=getIconHeight();
        if (fill==1) {
           int red=foregroundcolor.getRed();
           int blue=foregroundcolor.getBlue();
           int green=foregroundcolor.getGreen();
           Color lightercolor=new Color((int)((255-red)*0.65)+red,(int)((255-green)*0.65)+green,(int)((255-blue)*0.5)+blue);
           ((Graphics2D)g).setPaint(new java.awt.GradientPaint(0, y, foregroundcolor, 0, y+(height/2f), lightercolor, true));            
         } else if (fill==2) {
           int red=foregroundcolor.getRed();
           int blue=foregroundcolor.getBlue();
           int green=foregroundcolor.getGreen();
           Color lightercolor=new Color((int)((255-red)*0.65)+red,(int)((255-green)*0.65)+green,(int)((255-blue)*0.5)+blue);
           ((Graphics2D)g).setPaint(new java.awt.GradientPaint(x, 0, foregroundcolor, x+(width/2f), 0, lightercolor, true));
            
         } else g.setColor(foregroundcolor);
         g.fillRect(x,y,getIconWidth(),getIconHeight()); 
    }

    
    private void drawOperator(Graphics g, int x, int y, boolean greater) {
         g.setColor(foregroundcolor); 
         if (greater) {
            g.drawLine(x+3,y+3,x+0,y+0); // draw >            
            g.drawLine(x+4,y+3,x+1,y+0); //    
            g.drawLine(x+3,y+3,x+0,y+6); //             
            g.drawLine(x+4,y+3,x+1,y+6); //                                     
         } else {
            g.drawLine(x+0,y+3,x+3,y+0); //  draw <          
            g.drawLine(x+1,y+3,x+4,y+0); //    
            g.drawLine(x+0,y+3,x+3,y+6); //             
            g.drawLine(x+1,y+3,x+4,y+6); //              
         }
         g.drawLine(x+6,y+2,x+10,y+2); // draw equals sign =
         g.drawLine(x+6,y+4,x+10,y+4); //    
    }    
    
    private void drawTreeLine(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawLine(x+4, y, x+4, y+5);
         g.drawLine(x+4, y+5, x+10, y+5);
    } 
    
    private void drawLoggerLevel(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         int level=(property instanceof Integer)?(Integer)property:1;
              if (level==4) g.fillRect(x, y, 15, 15);
         else if (level==3) g.fillRect(x, y+4, 15, 11);
         else if (level==2) g.fillRect(x, y+8, 15, 7);
         else if (level==1) g.fillRect(x, y+12, 15, 3);
         else g.fillOval(x, y, 8, 8);
    }  
    
  
    private void drawEllipsis(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         int c=2;
         g.fillRect(x+2, y+4, c, c);
         g.fillRect(x+6, y+4, c, c);
         g.fillRect(x+10, y+4, c, c);         
    }    
          
    private void drawBulletMark(Graphics g, int x, int y) {
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(foregroundcolor);
        g.fillOval(x+1, y+1, 6, 6);
        g.setColor(Color.BLACK);
        g.drawOval(x+1, y+1, 6, 6);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);          
         
    }              
}
