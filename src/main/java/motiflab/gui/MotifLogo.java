/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import javax.swing.JTable;
import motiflab.engine.data.Region;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import motiflab.engine.data.Motif;

/**
 * This class implements functionality to draw "sequence logos" of motifs.
 * It implements the TableCellRenderer interface in order to directly incorporate
 * the logos into JTables.
 * If the class is used as a TableCellRenderer, the motif to paint is taken from
 * the table (which must contain instances of Motif objects). Also, if the table
 * contains a column describing the motif orientation it is possible to link to
 * this column and paint the motif in the orientation described.
 * If the class is used outside tables to paint single logos, the motif 
 * (and orientation to use) must be set directly.
 * @author kjetikl
 */
public class MotifLogo extends DefaultTableCellRenderer {
    private Font logofont;
    private char[] bases=new char[]{'A','C','G','T'};
    private int fontheight=25; // if you change this size you must also change the ascentCorrection below accordingly (but I am not sure about the relationship...)
    private double ascentCorrection=-2.5; // this is a needed hack because the ascent returned by the FontMetrics is not really the same height as that of a capital letter (even though that was stated in the documentation)
    private Color[] basecolors;
    private Motif motif=null;
    private boolean antialias=true;
//    private TableModel tablemodel=null;
    private JTable table=null;
    private int strandcolumn=-1; // use this column to decide the strand orientation if motifs are within a JTable
    private int offsetcolumn=-1; // use this column to decide the start position offset if motifs are within a JTable
    private int strand=Region.DIRECT; // use this column for strand orientation if painting single motif
    private int currentrow=-1;
    private boolean rowselected=false;
    private int maxwidth=0;
    private int logobasecount=0; // size of logo in number of bases (counting from defaultoffset). This can be larger than the motif itself (in which case the motif will be padded as necessary)
    private int defaultoffset=0; // the position (in bases) normally used as horisontal offset when drawing motif logos
    private boolean showaligned=false;
    private int highlightSegment=0; // If the value of this is set larger than 0, a segment this number of bp long is highlighted starting from defaultoffset
    private static Font templateFont=null;
    private boolean drawBorder=true;
    private boolean scaleByIC=true;
    private boolean sortBasesByFrequency=true;
    private DecimalFormat formatter=new DecimalFormat("#.###");
    private CharSequence bindingsequence=null; // if set to a sequence with same length as the motif matrix, this will be used to draw a match logo
    private Color highlightColor=new Color(255,128,255,20);
    private Color highlightBorderColor=new Color(255,128,255,86);
    
    
    public static Font getLogoFont(int height) {
        if (templateFont==null) {
             try {
              java.io.InputStream is=MotifLogo.class.getResourceAsStream("resources/ACGT.ttf"); //arialbd.ttf
              templateFont=Font.createFont(Font.TRUETYPE_FONT, is);
            } catch (Exception ex) {
              templateFont=new Font(Font.SANS_SERIF,Font.BOLD,20);
              System.err.println("Logofont not loaded.  Using standard sans-serif font.");
            }
        }
        return templateFont.deriveFont((float)height);
    }

    public MotifLogo(Color[] basecolors, int fontheight) {
        this.fontheight=fontheight;
        this.ascentCorrection=-(double)fontheight/10.0;
        logofont=getLogoFont(fontheight);
        this.basecolors=basecolors;
    }
    
    public MotifLogo(Color[] basecolors) {
        logofont=getLogoFont(fontheight);
        this.basecolors=basecolors;
    }
    
    public void setBaseColors(Color[] basecolors) {
        this.basecolors=basecolors;     
    }
    
    public void setBaseColor(Color newcolor, int base) {
        if (base>=0 && base<basecolors.length && newcolor!=null) basecolors[base]=newcolor;     
    }    
    
    
    public Color[] getBaseColors() {
        return basecolors;     
    }    
    
    public void setFontHeight(int fontheight) {
        if (fontheight==this.fontheight) return;
        this.fontheight=fontheight;
        this.ascentCorrection=-(double)fontheight/10.0;
        logofont=getLogoFont(fontheight);
    }
    
    public int getFontHeight() {
        return this.fontheight;
    }
    
    /** Sets the index of the column (in the model) that contains strand information
     *  (set to -1 to clear)
     */
    public void setStrandColumn(int column) {
        this.strandcolumn=column;
    }

     /** Sets the index of the column (in the model) that contains offset information
     *  (set to -1 to clear)
     */
    public void setOffsetColumn(int column) {
        this.offsetcolumn=column;
    }

    /** Sets the default offset (int bases) for drawing motifs */
    public void setDefaultOffset(int defaultOffset) {
        defaultoffset=defaultOffset;
    }

    /** Returns the default offset (int bases) for drawing motifs */
    public int getDefaultOffset() {
        return defaultoffset;
    }
//    public void setTableModel(TableModel model) {
//        tablemodel=model;
//    }
    
    /**
     * If set to TRUE, motifs will be drawned in alignment.
     * Note that for this to work, the 'default offset' and 'logo size' properties
     * must also have been set, as well as the 'offset column' in a table which provides
     * the offsets for each individual motif in a table 
     * @param align
     */
    public void setShowAligned(boolean align) {
        showaligned=align;
    }

    /**
     * If this length is set to a value larger than 0,
     * a segment of this length will be highlighted
     * starting from the default offset
     * @param length 
     */
    public void setHighlightSegment(int length) {
        highlightSegment=length;
    }
    
    /** Specifies whether or not to draw a black bounding box around the logo
     */
    public void setDrawBorder(boolean drawborder) {
        this.drawBorder=drawborder;
    }
    
    /** */
    public void setStrand(int strand) {
        this.strand=strand;
    }
    
    public void setMotif(Motif motif) {
        this.motif=motif;
    }
    
    /** If this sequence is set to something other than Null,
      * it will be used as the bases to draw a "match motif"
      */
    public void setBindingSequence(CharSequence sequence) {       
        this.bindingsequence=sequence;
    }    
    
    /** Sets a maximum width int pixels for the logo (this only applies outside)
     *  Set to 0 or negative number to allow infinite width
     */
    public void setMaxWidth(int max) {
        this.maxwidth=max;
    }
    

    /** Sets the size (in bases) to use when drawing the logo (at default offset).
     *  If the size is larger than the actual motif drawn, the motif
     *  will be padded as necessary to fill the specified length
     */
    public void setLogoSize(int size) {
        if (size>0) this.logobasecount=size; else logobasecount=0;
    }

   /**  Gets the size (in bases) to use when drawing the logo (at default offset)
    *   as set by setLogoSize(int size)
    */
    public int getLogoSize() {
        return logobasecount;
    }

    /** Specifies whether or not the base letters should be drawn in antialias mode
     *  (antialiasing gives nicer graphics but might be slower)
     */
    public void setUseAntialias(boolean useantialias) {
        antialias=useantialias;
    }

    /** Returns the height of the logo when drawn within the given Graphics object */
    public int getAscent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;
        FontMetrics metrics=g.getFontMetrics(logofont);
        int ascent=(int)(metrics.getAscent()-ascentCorrection+0.5);
        return ascent;
    }

    public boolean getScaleByIC() {
        return scaleByIC;
    }
    
    public void setScaleByIC(boolean scale) {
        scaleByIC=scale;
    }
    
    public boolean getSortByFrequency() {
        return sortBasesByFrequency;
    }
    
    public void setSortByFrequency(boolean sort) {
        sortBasesByFrequency=sort;
    }    
    
    
    public String getMotifInfoTooltip() {
        if (motif==null) return null;
        StringBuilder string=new StringBuilder();
        string.append(motif.getLength());
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(1);
        string.append(" bp, GC=");
        string.append(formatter.format(motif.getGCcontent()*100.0));
        string.append("%, IC=");
        formatter.setMinimumFractionDigits(3);
        formatter.setMaximumFractionDigits(3);
        string.append(formatter.format(motif.getICcontent()));
        return string.toString();
    }

    @Override  
    protected void paintComponent(Graphics graphics) {
        int xoffset=0; int yoffset=0;
        int motifOffset=(showaligned)?defaultoffset:0; // motif offset in bases
        if (motif==null) return;
        double[][] matrix=motif.getMatrix();
        if (motif.getMatrixType()==Motif.LOG_ODDS_MATRIX) matrix=motif.getMatrixAsFrequencyMatrix();
        if (table!=null) {
            TableModel model=table.getModel();
            int row=table.convertRowIndexToModel(currentrow);
            if (strandcolumn>=0) {
                Integer usestrand=(Integer)model.getValueAt(row, strandcolumn);
                if (usestrand==Region.REVERSE) matrix=Motif.reverseComplementMatrix(matrix);
            } else {
                if (strand==Region.REVERSE) matrix=Motif.reverseComplementMatrix(matrix);
            }
            if (offsetcolumn>=0 && showaligned) {
                Integer mo=(Integer)model.getValueAt(row, offsetcolumn);
                if (mo!=null) motifOffset+=mo;
            }
        } else {
            if (strand==Region.REVERSE) matrix=Motif.reverseComplementMatrix(matrix);
        }
        double[] letterXoffset=new double[4];                
        if (matrix==null) return;
        Graphics2D g = (Graphics2D)graphics;
        FontMetrics metrics=g.getFontMetrics(logofont);
        int ascent=(int)(metrics.getAscent()-ascentCorrection+0.5);
        int widthA=metrics.charWidth('A');     
        int widthC=metrics.charWidth('C');     
        int widthG=metrics.charWidth('G');     
        int widthT=metrics.charWidth('T');
        letterXoffset[0]=(widthG-widthA)/2;
        letterXoffset[1]=(widthG-widthC)/2;
        letterXoffset[3]=(widthG-widthT)/2;
        int logowidth=matrix.length*widthG; // default size of logo in pixels
        if (showaligned) {
            if (logobasecount*widthG>logowidth) logowidth=logobasecount*widthG;
            logowidth+=defaultoffset*widthG;
        }    
        if (antialias) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(logofont);
        AffineTransform restore=g.getTransform();
        AffineTransform save=g.getTransform();  
        double width=getWidth();
        if (rowselected) {
               g.setColor(table.getSelectionBackground());
               g.fillRect(0, yoffset, (int)width, (int)getHeight());
        } else {
               Color bg=this.getBackground();            
               g.setColor((bg!=null)?bg:Color.WHITE);
               g.fillRect(0, yoffset, (int)width, (int)getHeight());
        }           
        if (maxwidth>0 && (width>maxwidth || width==0)) width=maxwidth;
        double scaleX=(logowidth>width && width>0)?(width/(double)logowidth):1f;
        // ----- start hightlight segment -----        
        if (highlightSegment>0) {
            int start=defaultoffset*widthG;
            int end=(defaultoffset+highlightSegment)*widthG;
            g.setColor(highlightColor);
            g.fillRect((int)(scaleX*start),yoffset,(int)(scaleX*(end-start)),ascent+10); // the height not technically correct, but at least it will paint the full height of the cell
            g.setColor(highlightBorderColor);
            g.drawLine((int)(scaleX*start),yoffset,(int)(scaleX*start),ascent+10);
            g.drawLine((int)(scaleX*end)-1,yoffset,(int)(scaleX*end)-1,ascent+10);            
        }         
        // ----- end hightlight segment -----
        save.scale(scaleX, 1); // scale X-direction so that logo fits irrespective of size
        AffineTransform scaleY = new AffineTransform();
        xoffset=motifOffset*widthG;
        double xpos=xoffset;
        double[] ic=new double[matrix.length];
        for (int i=0;i<matrix.length;i++) {
            double[] counts=new double[]{matrix[i][0],matrix[i][1],matrix[i][2],matrix[i][3]};
            ic[i]=Motif.calculateColumnIC(counts[0], counts[1], counts[2], counts[3], false);
        }        
        for (int i=0;i<matrix.length;i++) {
            char baseAtPosition=(bindingsequence==null || bindingsequence.length()!=matrix.length)?'\n':Character.toUpperCase(bindingsequence.charAt(i)); // note that the matching sequence har already been reversed when needed. '\n' is used as a flag to indicate that sequence match should not be used           
            double[] counts=new double[]{matrix[i][0],matrix[i][1],matrix[i][2],matrix[i][3]};
            double total=counts[0]+counts[1]+counts[2]+counts[3];
            //double ic=Motif.calculateColumnIC(counts[0], counts[1], counts[2], counts[3], false);
            int[] sorted=new int[4]; // sorted in ascending order. Values are base-indices (i.e. 0=>A, 1=>C, 2=>G, 3=>T)
            int indexA=0, indexC=0, indexG=0, indexT=0;
            if (counts[0]>=counts[1]) indexA++;
            if (counts[0]>=counts[2]) indexA++;
            if (counts[0]>=counts[3]) indexA++;                
            if (counts[1]>counts[0])  indexC++;
            if (counts[1]>=counts[2]) indexC++;
            if (counts[1]>=counts[3]) indexC++;               
            if (counts[2]>counts[0])  indexG++;
            if (counts[2]>counts[1])  indexG++;
            if (counts[2]>=counts[3]) indexG++;                
            if (counts[3]>counts[0])  indexT++;
            if (counts[3]>counts[1])  indexT++;
            if (counts[3]>counts[2])  indexT++;
            sorted[indexA]=0;
            sorted[indexC]=1;
            sorted[indexG]=2;
            sorted[indexT]=3;
                double currentYoffset=yoffset;
                if (scaleByIC) currentYoffset+=(ascent-(ascent*ic[i]/2f));
                for (int j=3;j>=0;j--) { // draws letters from top to bottom (most frequent first)
                    int base=(sortBasesByFrequency)?sorted[j]:(3-j);
                    double fraction=counts[base]/total;
                    scaleY.setTransform(save);
                    scaleY.translate(letterXoffset[base],currentYoffset); // translated in 1-to-1 scale                    
                    if (scaleByIC) scaleY.scale(1,ic[i]/2f); // scale by IC-content in position
                    scaleY.scale(1, fraction);   
                    if (baseAtPosition=='\n' || baseAtPosition==bases[base]) {
                        g.setColor(basecolors[base]);
                    }
                    else g.setColor(Color.LIGHT_GRAY);                     
                    g.setTransform(scaleY);
                    g.drawString(""+bases[base], (float)xpos, (float)ascent); // draw all letters at same position and use transform to place them correctly
                    currentYoffset+=ascent*fraction*((scaleByIC)?(ic[i]/2f):1.0);
                } // end for each base  
            xpos+=widthG; // 
        } // end for each position
        g.setColor(Color.BLACK);
        g.setTransform(restore);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        if (drawBorder) g.drawRect((int)(scaleX*xoffset),yoffset,(int)(scaleX*(matrix.length*widthG))-1,ascent);
    }

    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        currentrow=row;
        rowselected=isSelected;
        this.table=table;
        if (value!=null && value instanceof Motif) motif=(Motif)value;
        else motif=null;
        setToolTipText(getMotifInfoTooltip());
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
    
    /** Returns the expected width for this motif */
    public int getDefaultMotifWidth() {
        FontMetrics metrics=getFontMetrics(logofont);     
        int widthG=metrics.charWidth('G');     
        if (motif==null) return 0;        
        int logowidth=motif.getLength()*widthG;   
        if (maxwidth>0 && logowidth>maxwidth) return maxwidth;
        else return logowidth;
    }
    
    /** Paints the logo into the given Graphics object */
    public void paintLogo(Graphics g) {
         paintComponent(g);
    }
    
    public void paintLogo(Graphics graphics, int x, int y, int width, int height, boolean reverse, boolean transparent, boolean grid) {
        int xoffset=x; int yoffset=y;
        if (motif==null) return;
        double[][] matrix=motif.getMatrix();
        if (motif.getMatrixType()==Motif.LOG_ODDS_MATRIX) matrix=motif.getMatrixAsFrequencyMatrix();
        if (reverse) matrix=Motif.reverseComplementMatrix(matrix);
        
        double[] letterXoffset=new double[4];                
        if (matrix==null) return;
        Graphics2D g = (Graphics2D)graphics;
        FontMetrics metrics=g.getFontMetrics(logofont);
        int ascent=(int)(metrics.getAscent()-ascentCorrection+0.5);
        int widthA=metrics.charWidth('A');     
        int widthC=metrics.charWidth('C');     
        int widthG=metrics.charWidth('G');     
        int widthT=metrics.charWidth('T');
        letterXoffset[0]=(widthG-widthA)/2;
        letterXoffset[1]=(widthG-widthC)/2;
        letterXoffset[3]=(widthG-widthT)/2;
        int logowidth=matrix.length*widthG; // default size of logo in pixels
        g.setFont(logofont);
        AffineTransform restore=g.getTransform();
        AffineTransform save=g.getTransform();  
        if (!transparent) {
            g.setColor(Color.WHITE);
            g.fillRect(xoffset, yoffset, (int)width, (int)height);           
        }
        if (grid) {            
            g.setColor(new Color(225,225,225));
            int y0=yoffset+(int)(height*0);
            int y1=yoffset+(int)(height*0.25);
            int y2=yoffset+(int)(height*0.5);
            int y3=yoffset+(int)(height*0.75);
            int y4=yoffset+(int)(height*1.0);                   
            g.drawLine(xoffset,y0,xoffset+width,y0);            
            g.drawLine(xoffset,y1,xoffset+width,y1);      
            g.drawLine(xoffset,y2,xoffset+width,y2); 
            g.drawLine(xoffset,y3,xoffset+width,y3); 
            g.drawLine(xoffset,y4,xoffset+width,y4);             
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);        
        double scaleXfactor=width/(double)logowidth;
        double scaleYfactor=height/(double)ascent;        
        save.scale(scaleXfactor, scaleYfactor); // scale logo to fit with specified height and width
        AffineTransform scaleY = new AffineTransform();
        double xpos=xoffset;
        double[] ic=new double[matrix.length];
        for (int i=0;i<matrix.length;i++) {
            double[] counts=new double[]{matrix[i][0],matrix[i][1],matrix[i][2],matrix[i][3]};
            ic[i]=Motif.calculateColumnIC(counts[0], counts[1], counts[2], counts[3], false);
        }        
        for (int i=0;i<matrix.length;i++) {
            double[] counts=new double[]{matrix[i][0],matrix[i][1],matrix[i][2],matrix[i][3]};
            double total=counts[0]+counts[1]+counts[2]+counts[3];
            int[] sorted=new int[4]; // sorted in ascending order. Values are base-indices (i.e. 0=>A, 1=>C, 2=>G, 3=>T)
            int indexA=0, indexC=0, indexG=0, indexT=0;
            if (counts[0]>=counts[1]) indexA++;
            if (counts[0]>=counts[2]) indexA++;
            if (counts[0]>=counts[3]) indexA++;                
            if (counts[1]>counts[0])  indexC++;
            if (counts[1]>=counts[2]) indexC++;
            if (counts[1]>=counts[3]) indexC++;               
            if (counts[2]>counts[0])  indexG++;
            if (counts[2]>counts[1])  indexG++;
            if (counts[2]>=counts[3]) indexG++;                
            if (counts[3]>counts[0])  indexT++;
            if (counts[3]>counts[1])  indexT++;
            if (counts[3]>counts[2])  indexT++;
            sorted[indexA]=0;
            sorted[indexC]=1;
            sorted[indexG]=2;
            sorted[indexT]=3;
            double currentYoffset=yoffset;
            if (scaleByIC) currentYoffset+=(ascent-(ascent*ic[i]/2f));
            for (int j=3;j>=0;j--) { // draws letters from top to bottom (most frequent first)
                int base=(sortBasesByFrequency)?sorted[j]:(3-j);
                double fraction=counts[base]/total;
                scaleY.setTransform(save);
                scaleY.translate(letterXoffset[base],currentYoffset); // translated in 1-to-1 scale                    
                if (scaleByIC) scaleY.scale(1,ic[i]/2f); // scale by IC-content in position
                scaleY.scale(1, fraction);   
                g.setColor(basecolors[base]);                  
                g.setTransform(scaleY);
                g.drawString(""+bases[base], (float)xpos, (float)ascent); // draw all letters at same position and use transform to place them correctly
                currentYoffset+=ascent*fraction*((scaleByIC)?(ic[i]/2f):1.0);
            } // end for each base  
            xpos+=widthG; // 
        } // end for each position
        g.setColor(Color.BLACK);
        g.setTransform(restore);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        if (drawBorder) g.drawRect(xoffset,yoffset,width-1,height-1);
    }    
    
    
}
