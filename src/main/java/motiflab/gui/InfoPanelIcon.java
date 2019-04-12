/*
 * 
 */

package motiflab.gui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import javax.swing.Icon;



/**
 * This class is an implementation of Icon that can draw different
 * simple icons that are used in SequenceVisualization.InfoPanels
 * 
 * @author kjetikl
 */
public class InfoPanelIcon implements Icon {
    public static final int ZOOM_ICON=0;
    public static final int ZOOM_IN_ICON=1;
    public static final int ZOOM_OUT_ICON=2;
    public static final int ZOOM_RESET_ICON=3;
    public static final int ZOOM_TO_FIT_ICON=4;
    public static final int CONSTRAIN_ICON=5;
    public static final int LEFT_ICON=6;
    public static final int RIGHT_ICON=7;
    public static final int LEFT_END_ICON=8;
    public static final int RIGHT_END_ICON=9;
    public static final int LOCK_ICON=10;
    public static final int FLIP_ICON=11;
    public static final int ALIGN_LEFT_ICON=12;
    public static final int ALIGN_RIGHT_ICON=13;
    public static final int ALIGN_TSS_ICON=14;
    public static final int ALIGN_NONE_ICON=15;
    public static final int TRIANGLE_DOWN_ICON=16;

    private int type=0;
    private Color foregroundcolor=Color.black;

    
    public InfoPanelIcon(int type) {
        this.type=type;
    }
    
    @Override
    public int getIconHeight() {
        return 8;
    }

    @Override
    public int getIconWidth() {
        switch(type) {
          case CONSTRAIN_ICON: return 16;
          case LEFT_ICON: return 6;
          case RIGHT_ICON: return 6;       
          case LOCK_ICON: return 13;        
          case FLIP_ICON: return 14;
          case TRIANGLE_DOWN_ICON: return 7;
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
    

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      switch(type) {
          case ZOOM_ICON: drawZoomIcon(g, x, y);break;
          case ZOOM_IN_ICON: drawZoomInIcon(g, x, y); break;
          case ZOOM_OUT_ICON: drawZoomOutIcon(g, x, y);break;
          case ZOOM_RESET_ICON: drawZoomResetIcon(g, x, y);break;
          case ZOOM_TO_FIT_ICON: drawZoomToFitIcon(g, x, y);break;
          case CONSTRAIN_ICON: drawConstrainIcon(g, x, y);break;
          case LEFT_ICON: drawLeftIcon(g, x, y);  break;
          case LEFT_END_ICON: drawLeftEndIcon(g, x, y);  break;
          case RIGHT_ICON: drawRightIcon(g, x, y);  break;
          case RIGHT_END_ICON: drawRightEndIcon(g, x, y);   break;           
          case LOCK_ICON: drawLockIcon(g, x, y);   break;           
          case FLIP_ICON: drawFlipIcon(g, x, y);   break;           
          case ALIGN_LEFT_ICON: drawAlignLeftIcon(g, x, y);   break;           
          case ALIGN_RIGHT_ICON: drawAlignRightIcon(g, x, y);   break;           
          case ALIGN_TSS_ICON: drawAlignTSSIcon(g, x, y);   break;           
          case ALIGN_NONE_ICON: drawAlignNoneIcon(g, x, y);   break;     
          case TRIANGLE_DOWN_ICON: drawTriangleDownIcon(g, x, y);   break;
      }
    }
    
    private void drawZoomInIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);         
         g.drawRect(3, 1, 1, 5); // vertical
         g.drawRect(1, 3, 5, 1); // horizontal
    }

    private void drawZoomOutIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawRect(1, 3, 5, 1); // horizontal
    }

    private void drawZoomResetIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor); 
         g.fillRect(2, 1, 4, 6); // vertical
         g.fillRect(1, 2, 6, 4); // horizontal
    }

    private void drawZoomToFitIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(0,3,7,3); // horiztonal
         g.drawLine(0,3,6,6); // horiztonal
         g.drawLine(1,2,1,5); // left arrow   
         g.drawLine(2,1,2,6); // left arrow 
         g.drawLine(5,1,5,6); // right arrow 
         g.drawLine(6,2,6,5); // right arrow 
    }
 
    private void drawZoomIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(1,0,3,0); // circle top
         g.drawLine(0,1,0,3); // circle left
         g.drawLine(4,1,4,3); // circle right
         g.drawLine(1,4,3,4); // circle bottom
         g.drawLine(4,5,6,7); // handle 1
         g.drawLine(5,5,7,7); // handle 2
         g.drawLine(5,4,7,6); // handle 3
         g.setColor(new java.awt.Color(foregroundcolor.getRed(),foregroundcolor.getGreen(),foregroundcolor.getBlue(),65));
         g.drawLine(1,1,1,1); // TL
         g.drawLine(3,1,3,1); // TR
         g.drawLine(1,3,1,3); // BL
         g.drawLine(3,3,3,3); // BR
         g.drawLine(4,4,4,4); // BR
    }    
    
    private void drawConstrainIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor); 
         g.drawLine(1,2,1,5); // 1. vertical
         g.drawLine(2,2,2,5); //  
         g.drawLine(7,2,7,5); // 2. vertical
         g.drawLine(6,2,6,5); // right corners   
         g.drawLine(2,1,6,1); // top horizontal
         g.drawLine(2,6,6,6); // bottom horizontal
         
         g.drawLine(5,3,11,3); // middle chain
         g.drawLine(5,4,11,4); // middle chain
         
         g.drawLine(9,2,9,5); // 1. vertical
         g.drawLine(14,2,14,5); // 
         g.drawLine(10,2,10,5); // left corners 
         g.drawLine(15,2,15,5); // 2. vertical     
         g.drawLine(10,1,14,1); // top horizontal
         g.drawLine(10,6,14,6); // bottom horizontal
         
         g.setColor(new java.awt.Color(foregroundcolor.getRed(),foregroundcolor.getGreen(),foregroundcolor.getBlue(),65));
         g.drawLine(3,2,3,2);
         g.drawLine(5,2,5,2);
         g.drawLine(3,5,3,5);
         g.drawLine(5,5,5,5);
         g.drawLine(4,3,4,4);
         g.drawLine(11,2,11,2);
         g.drawLine(13,2,13,2);
         g.drawLine(11,5,11,5);
         g.drawLine(13,5,13,5);
         g.drawLine(12,3,12,4);
         
         

    }

    private void drawLeftIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);  
         g.drawLine(1,3,1,4); // 2px
         g.drawLine(2,2,2,5); // 4px
         g.drawLine(3,1,3,6); // 6px
         g.drawLine(4,1,4,6); // 6px
    }
    
    private void drawLeftEndIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);  
         g.drawLine(1,1,1,6); // 6px        
         g.drawLine(2,1,2,6); // 6px        
         g.drawLine(3,3,3,4); // 2px
         g.drawLine(4,2,4,5); // 4px
         g.drawLine(5,1,5,6); // 6px
         g.drawLine(6,1,6,6); // 6px                
    }
    
    private void drawRightIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);  
         g.drawLine(1,1,1,6); // 6px
         g.drawLine(2,1,2,6); // 6px      
         g.drawLine(3,2,3,5); // 4px
         g.drawLine(4,3,4,4); // 2px
    }
    
    private void drawRightEndIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(1,1,1,6); // 6px
         g.drawLine(2,1,2,6); // 6px      
         g.drawLine(3,2,3,5); // 4px
         g.drawLine(4,3,4,4); // 2px         
         g.drawLine(5,1,5,6); // 6px        
         g.drawLine(6,1,6,6); // 6px          
    }

    private void drawLockIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);  
         g.drawLine(1,3,1,6); // left bump
         g.drawLine(2,3,2,6); // left bump
         g.drawLine(4,5,4,5); // right bump
         g.drawLine(1,3,7,3); // horizontal
         g.drawLine(1,4,7,4); // horizontal
         g.drawLine(7,2,7,5); // left circle
         g.drawLine(12,2,12,5); // right circle
         g.drawLine(8,1,11,1); // top circle
         g.drawLine(8,6,11,6); // bottom circle
         g.drawRect(8, 2, 3, 3); // inner circle
    }

    private void drawFlipIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);  
         g.drawLine(3,2,3,6); //
         g.drawLine(1,4,5,4); // 
         g.drawLine(2,5,4,5); //
         g.drawLine(4,1,6,1); //
         g.drawLine(4,2,4,2); //
         
         g.drawLine(10,1,10,5); //
         g.drawLine(8,3,12,3); // 
         g.drawLine(9,2,11,2); //
         g.drawLine(7,6,9,6); //
         g.drawLine(9,5,9,5); //
    }

 
    private void drawAlignLeftIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(1,1,1,6); // vertical
         g.drawLine(2,1,2,6); // vertical      
         g.drawLine(1,5,6,5); // horisontal
         g.drawLine(1,6,6,6); // horisontal        
    }
 
    private void drawAlignRightIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(5,1,5,6); // vertical
         g.drawLine(6,1,6,6); // vertical      
         g.drawLine(1,5,6,5); // horisontal
         g.drawLine(1,6,6,6); // horisontal        
    }
 
    private void drawAlignTSSIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(1,2,1,6); // vertical
         g.drawLine(1,2,7,2); // horisontal      
         g.drawLine(5,0,5,4); // long arrowhead        
         g.drawLine(6,1,6,3); // small arrowhead        
    }
 
    private void drawAlignNoneIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);   
         g.drawLine(1,0,7,6); // down left-right
         g.drawLine(1,1,6,6); // down left-right 
         g.drawLine(2,0,7,5); // down left-right     
         g.drawLine(1,5,6,0); // up left-right 
         g.drawLine(1,6,7,0); // up left-right 
         g.drawLine(2,6,7,1); // up left-right 
    }
 
    private void drawTriangleDownIcon(Graphics g, int x, int y) {
         g.setColor(foregroundcolor);
         g.drawLine(x+0,y+2,x+6,y+2);
         g.drawLine(x+1,y+3,x+5,y+3);
         g.drawLine(x+2,y+4,x+4,y+4);
         g.drawLine(x+3,y+5,x+3,y+5);
    }
 

  
    
}
