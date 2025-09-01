
package org.motiflab.gui;
import java.awt.Color;

/**
 * A subclass of LineBorder which allows for access to change the border color
 * @author kjetikl
 */
public class TrackBorder extends javax.swing.border.LineBorder {
 
    
    public TrackBorder(Color color, int thickness)  {
        super(color, thickness, false);
    }    
    
    public void setBorderColor(Color color) {
        this.lineColor=color;
    }
}
