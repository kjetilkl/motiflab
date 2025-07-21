/*
 
 
 */

package org.motiflab.gui;
import java.awt.Color;
import java.io.Serializable;
/**
 * Creates a gradient with a range of colors between the first and second given color
 * 
 * @author kjetikl
 */
public class ColorGradient implements Serializable {
    
    private static final long serialVersionUID = 2491862500733241617L;
     
    Color[] gradient = null;
    
    /**
     * Creates a new color gradient consisting of 100 color shades between
     * the specified target and background color
     * @param targetcolor
     * @param background
     */
    public ColorGradient(Color targetcolor,Color background) {
        this(targetcolor,background,100);
    }
    
    /**
     * Creates a new color gradient consisting of 100 color shades between
     * the specified target and a fully transparent version of this color (with alpha=0)
     * @param targetcolor
     * @param background
     */
    public ColorGradient(Color targetcolor) {
        this(targetcolor,100);
    } 
          
    /**
     * Creates a new "solid" color gradient consisting of the specified number of color shades between
     * the specified target and background colors 
     * (actually the number of colors in the gradient will be one more than the argument 
     * so that gradient[0] is the background and gradient[colors] is the target color.
     * @param targetcolor
     * @param background
     * @param colors
     */
    public ColorGradient(Color targetcolor, Color background, int colors) {
        gradient = new Color[colors+1];
        float col1_rgb[];
        float col2_rgb[];
        col1_rgb = background.getRGBComponents(null);
        col2_rgb = targetcolor.getRGBComponents(null);
        float diff_r=(col2_rgb[0]-col1_rgb[0])/(float)colors;
        float diff_g=(col2_rgb[1]-col1_rgb[1])/(float)colors;
        float diff_b=(col2_rgb[2]-col1_rgb[2])/(float)colors;
        float diff_alpha=(col2_rgb[3]-col1_rgb[3])/(float)colors;
        for (int i=0;i<=colors;i++) {
            float newred=col1_rgb[0]+diff_r*i;
            float newgreen=col1_rgb[1]+diff_g*i;
            float newblue=col1_rgb[2]+diff_b*i;
            float newalpha=col1_rgb[3]+diff_alpha*i;            
            if (newred>1.0f) newred=1.0f;
            if (newgreen>1.0f) newgreen=1.0f;
            if (newblue>1.0f) newblue=1.0f;
            if (newalpha>1.0f) newalpha=1.0f;             
            if (newred<0.0f) newred=0.0f;
            if (newgreen<0.0f) newgreen=0.0f;
            if (newblue<0.0f) newblue=0.0f;            
            if (newalpha<0.0f) newalpha=0.0f;           
            gradient[i]=new Color(newred,newgreen,newblue,newalpha);
        }
    } 
    
    /**
     * Creates a new transparent color gradient consisting of the specified number of color shades between
     * the specified target color (which can have alpha less than 1.0) and a fully transparent version of this color (with alpha=0)
     * (actually the number of colors in the gradient will be one more than the argument 
     * so that gradient[0] is the transparent background and gradient[colors] is the target color.
     * @param targetcolor
     * @param background
     * @param colors
     */
    public ColorGradient(Color targetcolor, int colors) {
        gradient = new Color[colors+1];
        float col2_rgb[] = targetcolor.getRGBComponents(null);        
        float newred=col2_rgb[0];
        float newgreen=col2_rgb[1];
        float newblue=col2_rgb[2];        
        float diff_alpha=(col2_rgb[3])/(float)colors;
        for (int i=0;i<=colors;i++) {
            float newalpha=diff_alpha*i;            
            if (newalpha>1.0f) newalpha=1.0f;                         
            if (newalpha<0.0f) newalpha=0.0f;           
            gradient[i]=new Color(newred,newgreen,newblue,newalpha);
        }
    }      
    
   /** Creates a new color gradient consisting of 100 color shades between
     * blue and red (i.e. rainbow colors)
     * @param targetcolor
     * @param background  
     */
    public ColorGradient() {
        gradient = new Color[101];
        for (int i=0;i<=100;i++) {
            gradient[100-i]=Color.getHSBColor((float)(i/100f*0.6666f), 1.0f, 1.0f); // 0.666 is blue color (higher than that it goes through violet and magenta back to red)
        }
    }
    
  /**
   * Returns the color for a given value which should be an integer 
   * (between 0 and getMaxColorIndex())
   * @param value
   */
    public Color getColor(int value) {
        if (value>gradient.length-1) {value=gradient.length-1;} 
        else if (value<0) {value=0;}
        return gradient[value];
    }
         
  /**
   * Returns the color for a given value which should be a double between 0.0 and 1.0
   * @param value
   */
    public Color getColor(double value) {
        int maxindex=(gradient==null || gradient.length==0)?0:gradient.length-1;
        int i=(int)Math.round(value*maxindex);
        if (i>maxindex) {i=maxindex;} 
        else if (i<0) {i=0;}
        return gradient[i];
    }
    
    /**
     * Returns the foreground color associated with this gradient (highest color)
     * @return foreground color
     */
    public Color getForegroundColor() {
        return gradient[gradient.length-1];
    }
    
    /**
     * Returns the background color associated with this gradient (lowest color)
     * Note that if this gradient is not "solid" the background will be transparent
     * @return background color
     */
    public Color getBackgroundColor() {
        return gradient[0];
    }
    
    /** Returns the index of the foreground color (highest color index)
     */
    public int getMaxColorIndex() {
        if (gradient==null || gradient.length==0) return 0;
        else return gradient.length-1;
    }
}
