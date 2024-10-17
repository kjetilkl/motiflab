package motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.ModuleMotif;

/**
 * This class implements TableCellRenderer and paints a graphical representation of a module
 * @author kjetikl
 */
public class ModuleLogo extends DefaultTableCellRenderer {
    private static Font modulemotiffont = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final Color darkGreen=new Color(0,200,0);
    private VisualizationSettings settings;
    private ModuleCRM cisRegModule;
    private static int boxspacing=19;
    private static int boxmargin=3;
    private static int logomargin=5;
    private boolean showTooltip=true;
    private int maxwidth=0;

    public ModuleLogo(VisualizationSettings settings) {
        this.settings=settings;
    }

    public void showToolTip(boolean flag) {
        showTooltip=flag;
    }

    public static Font getModuleMotifFont() {
        return modulemotiffont;
    }

    public void setModule(ModuleCRM cisRegModule) {
        this.cisRegModule=cisRegModule;
    }
    
    public ModuleCRM getModule() {
        return cisRegModule;
    }    
    
    /** Sets a maximum width int pixels for the logo (this only applies outside)
     *  Set to 0 or negative number to allow infinite width
     */
    public void setMaxWidth(int max) {
        this.maxwidth=max;
    }    

    public int getLogoWidth(Graphics graphics) {
        FontMetrics metrics=graphics.getFontMetrics(modulemotiffont);
        return getLogoWidth(metrics, cisRegModule);
    }
    
    /** Returns the expected with of the rendered module */
    public static int getLogoWidth(FontMetrics metrics, ModuleCRM cisRegModule) {
       int size=cisRegModule.getCardinality();
       int x=0;
       for (int i=0;i<size;i++) {
           if (i>0) {
               x+=boxspacing;
           }
           ModuleMotif mm=cisRegModule.getSingleMotif(i);
           String mmName=mm.getRepresentativeName();
           int namewidth=metrics.stringWidth(mmName);
           int boxwidth=namewidth+boxmargin+boxmargin;
           x+=boxwidth;
       }
       return x+logomargin+logomargin; // 10px empty border on either side of logo
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value!=null && value instanceof ModuleCRM) cisRegModule=(ModuleCRM)value;
        else cisRegModule=null;
        value=""; // this avoids text being painted by super-class
        if (cisRegModule!=null && showTooltip) table.setToolTipText(cisRegModule.getName());
        else table.setToolTipText(null);
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (cisRegModule!=null) paintModuleLogo((Graphics2D)g, cisRegModule, logomargin, 9, settings, this, 0);
    }

   public void paintModuleLogo(Graphics2D graphics, int x, int y) {
      if (cisRegModule!=null) paintModuleLogo(graphics, cisRegModule, logomargin+x, 9+y, settings, this, maxwidth); 
   }   
    
   /** Paints a small graphical representation of the module (with motifs rendered as boxes) */
   public static void paintModuleLogo(Graphics2D graphics, ModuleCRM cisRegModule, int x, int y, VisualizationSettings vizsettings, JComponent component, int maxwidth) {
       Font current=graphics.getFont();
       Object alias=graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
       int size=cisRegModule.getCardinality();
       FontMetrics metrics=graphics.getFontMetrics(modulemotiffont);
       int logowidth=getLogoWidth(metrics, cisRegModule);
       int boxheight=metrics.getHeight()+2;
       graphics.setFont(modulemotiffont);
       int width=(component!=null)?component.getWidth():0;
       if (maxwidth>0 && (width>maxwidth || width==0)) width=maxwidth;       
       double scaleX=(logowidth>width && width>0)?(width/(double)logowidth):1f;
       graphics.scale(scaleX, 1); // scale X-direction so that logo fits irrespective of size
       for (int i=0;i<size;i++) {
           if (i>0) {
               if (cisRegModule.isOrdered()) { // draw distance constraint
                   int[] distance=cisRegModule.getDistance(i-1, i);
                   //graphics.setColor((distance!=null)?Color.RED:Color.BLACK);
                   graphics.setColor(Color.BLACK);
                   // draw lines between boxes;
                   int starty=(int)(y+(boxheight/2))+1;
                   graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);                              
                   graphics.drawLine(x+1, starty, (int)(x+boxspacing/2)+1, starty-4);
                   graphics.drawLine((int)(x+boxspacing/2), starty-4, x+boxspacing-1, starty);
                   graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);                             
                   if (distance!=null) { // distance constrained symbol: []
                       int xd=x+6;
                       int yd=y-2;
                       graphics.drawLine(xd,yd,xd,yd+4); // vertical left
                       graphics.drawLine(xd+7,yd,xd+7,yd+4); // vertical right
                       graphics.drawLine(xd,yd,xd+1,yd); // serif top left
                       graphics.drawLine(xd,yd+4,xd+1,yd+4); // serif bottom left
                       graphics.drawLine(xd+6,yd,xd+7,yd); // serif top right
                       graphics.drawLine(xd+6,yd+4,xd+7,yd+4); // serif bottom right
                   }
               }
               x+=boxspacing;
           }
           ModuleMotif mm=cisRegModule.getSingleMotif(i);
           String mmName=mm.getRepresentativeName();
           int namewidth=metrics.stringWidth(mmName);
           int boxwidth=namewidth+boxmargin+boxmargin; // 3px border on each side
           Color mmColor=vizsettings.getFeatureColor(cisRegModule.getName()+"."+mmName);
           boolean isDark=VisualizationSettings.isDark(mmColor);
           graphics.setColor(mmColor);
           graphics.fillRect(x, y, boxwidth, boxheight);
           graphics.setColor(Color.BLACK);
           graphics.drawRect(x, y, boxwidth, boxheight);
           if (isDark) graphics.setColor(Color.WHITE); // else black (current)
           graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           graphics.drawString(mmName, x+boxmargin+1, y+boxheight-4);
           graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, alias);
           int orientation=mm.getOrientation();
           if (orientation!=ModuleCRM.INDETERMINED) {
              graphics.setColor((orientation==ModuleCRM.DIRECT)?darkGreen:Color.RED);
              graphics.drawLine(x, y-3, x+boxwidth, y-3);
              graphics.drawLine(x, y-4, x+boxwidth, y-4);
              int l1=((orientation==ModuleCRM.DIRECT))?x+boxwidth-2:x+2; // larger line
              int l2=((orientation==ModuleCRM.DIRECT))?x+boxwidth-1:x+1; // smaller line
              graphics.drawLine(l1, y-6, l1, y-1);
              graphics.drawLine(l2, y-5, l2, y-2);
           }
           x+=boxwidth;
       }
       graphics.setFont(current);
   }

}
