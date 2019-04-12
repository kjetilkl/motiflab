
package motiflab.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JToolTip;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import motiflab.engine.data.analysis.AnnotatedValue;

/**
 * This class is used for column headers in some JTables.
 * If the column contains numeric values, pointing the mouse at the column header
 * will display a tooltip containing statistics about the distribution of the values
 * (including a histogram).
 * @author kjetikl
 */
public class ToolTipHeader extends JTableHeader {
    ArrayList<String> toolTipStrings;
    HeaderTooltip headertooltip=null;
    private int histogrambins=50;
    private int barwidth=2; // pixel width for each histogram bin
    private int histogramheight=20;
    private Color histogramColor=new Color(255,100,100);
    private int[][] histograms=null;
    private double[][] minMaxAverageNans=null;    

    public ToolTipHeader(TableColumnModel model,ArrayList<String> headerColumnToolTips) {
      super(model);
      this.toolTipStrings = headerColumnToolTips;
      int columns=model.getColumnCount();
      headertooltip=new HeaderTooltip();
      histograms=new int[columns][];
      minMaxAverageNans=new double[columns][];       
    }
    
    public ToolTipHeader(TableColumnModel model) {
       this(model,null); 
    }    

    @Override
    public String getToolTipText(MouseEvent e) {
      int col = columnAtPoint(e.getPoint());
      int modelCol = getTable().convertColumnIndexToModel(col); 
      headertooltip.drawHistogram=false;
      if (modelCol<0 || modelCol>=table.getModel().getColumnCount()) {         
         return null;
      }
      headertooltip.currentColumn=modelCol;
      StringBuilder builder=new StringBuilder("<html>");
      if (toolTipStrings!=null) {
          builder.append(toolTipStrings.get(modelCol));
      } else {
          builder.append(table.getColumnName(col));
      }
      Class columnclass=table.getModel().getColumnClass(modelCol);      
      if (Number.class.isAssignableFrom(columnclass)) {
         headertooltip.drawHistogram=true;
         if (histograms[modelCol]==null) histograms[modelCol]=getHistogram(modelCol);    
         builder.append("<br><br><br>"); // make room for the histogram painting
         if (minMaxAverageNans[modelCol][4]==0) builder.append("No valid values");
         else {
            builder.append("Min: "); 
            builder.append(minMaxAverageNans[modelCol][0]); 
            builder.append("<br>"); 
            builder.append("Max: "); 
            builder.append(minMaxAverageNans[modelCol][1]); 
            builder.append("<br>"); 
            builder.append("Average: ");
            builder.append(minMaxAverageNans[modelCol][2]); 
            builder.append("<br>"); 
            builder.append("Valid:"); 
            builder.append((int)minMaxAverageNans[modelCol][4]);             
            builder.append(", Invalid:"); 
            builder.append((int)minMaxAverageNans[modelCol][3]);             
         }
      } 
      builder.append("</html>");
      return builder.toString();
    }

    @Override
    public JToolTip createToolTip() {
        return headertooltip;
    }
      
         
    private int[] getHistogram(int column) {
        int[] counts=new int[histogrambins];
        TableModel model=table.getModel();
        minMaxAverageNans[column]=getMinMaxAverageNan(model,column);
        double minvalue=minMaxAverageNans[column][0];
        double maxvalue=minMaxAverageNans[column][1];
        if (maxvalue==-Double.MAX_VALUE && minvalue==Double.MAX_VALUE) return counts; // no applicable numbers present?
        for (int i=0;i<model.getRowCount();i++) {
           Object rawvalue=model.getValueAt(i,column);
           if (rawvalue instanceof AnnotatedValue) rawvalue=((AnnotatedValue)rawvalue).getValue();
           if (rawvalue==null || !(rawvalue instanceof Number)) continue;
           double value=((Number)rawvalue).doubleValue();
           if (Double.isNaN(value) || Double.isInfinite(value)) continue;
           double relativeValue=(value-minvalue)/(maxvalue-minvalue);
           int bin=(int)(relativeValue*(double)histogrambins);              
           if (bin<0) bin=0;
           if (bin>=histogrambins) bin=histogrambins-1;
           counts[bin]++;
        }  
        return counts;
    }

    private double[] getMinMaxAverageNan(TableModel model, int column) {
        int nans=0;
        int valid=0;
        double maxvalue=-Double.MAX_VALUE;
        double minvalue=Double.MAX_VALUE;  
        double sum=0;
        for (int i=0;i<model.getRowCount();i++) {
           Object rawvalue=model.getValueAt(i,column);
           if (rawvalue instanceof AnnotatedValue) rawvalue=((AnnotatedValue)rawvalue).getValue();
           if (rawvalue==null || !(rawvalue instanceof Number)) {nans++;continue;}
           double value=((Number)rawvalue).doubleValue();
           if (Double.isNaN(value) || Double.isInfinite(value)) {nans++;continue;}
           if (value<minvalue) minvalue=value;
           if (value>maxvalue) maxvalue=value;
           sum+=value;
           valid++;
        }
        return new double[]{minvalue,maxvalue,sum/(double)valid,nans,valid};
    }   
    
    
    private class HeaderTooltip extends JToolTip { 
        int currentColumn=0; // the modelcolumn currently pointed at
        boolean drawHistogram=false;
 
        public HeaderTooltip() {
            super();           
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension dim=super.getPreferredSize();
            if (drawHistogram) {               
               //dim.height+=(histogramheight+7);
               if (dim.width<(barwidth*histogrambins)+10) dim.width=(barwidth*histogrambins)+10;
            } 
            return dim;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (drawHistogram) {
                //Dimension dim=super.getPreferredSize();
                double maxvalue=0;
                for (int i=0;i<histograms[currentColumn].length;i++) {
                   if (histograms[currentColumn][i]>maxvalue) maxvalue=histograms[currentColumn][i];
                }
                int histogramwidth=(barwidth*histogrambins);
                int topX=5; int topY=26; // top of histogram. The Y-coordinate is just a guess :-S
                g.setColor(Color.WHITE);
                g.fillRect(topX-1, topY-1, histogramwidth+1, histogramheight+1);
                g.setColor(Color.BLACK);
                g.drawRect(topX-1, topY-1, histogramwidth+1, histogramheight+1);
                g.setColor(histogramColor);
                for (int i=0;i<histograms[currentColumn].length;i++) {
                   int barheight=(int)Math.round((((double)histograms[currentColumn][i])/maxvalue)*histogramheight);
                   if (barheight==0 && histograms[currentColumn][i]>0) barheight=1; // min 1px for non-zero bins!
                   g.fillRect(topX+i*barwidth, topY+histogramheight-barheight, barwidth, barheight);
                }                
            }        
        } 
    }  
}   

