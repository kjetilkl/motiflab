/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import javax.swing.JPanel;
import org.motiflab.engine.Graph;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericMap;

/**
 *
 * @author Kjetil
 */
public class DistributionPanel extends JPanel {
    private int bincount=100;
    private double[] bins;
    private double minvalue=Double.MAX_VALUE;
    private double maxvalue=-Double.MAX_VALUE;
    private int entryCount=0;
    private int explicitValueCount=0;
    private int invalidCount=0;
    private double averagevalue=0;
    private double maxY=0;
    private int minWidth=100;
    private int minHeight=100;
    private int translateX=70;
    private int translateY=30;
    private int width=300; //
    private int height=200;
    private NumericMap map;
    private double[] highlightvalues=null;
    private Graph graph;

    DistributionPanel(NumericMap map, int bincount, int binwidth, MotifLabEngine engine) {
        this.map=map;
        this.bincount=bincount;
        width=binwidth*bincount;
        updateHistogram(engine);
        sizeToFit();
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                sizeToFit();
                repaint();
            }
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}
            @Override
            public void componentHidden(ComponentEvent e) {}
       });
    }

    public final void updateHistogram(MotifLabEngine engine) {
        if (bins==null || engine!=null) {
            Class type=map.getMembersClass();
            if (engine!=null) fillBins(engine, type);
        }
        if (bins==null) {graph=null;return;}            
        if (graph==null) graph=new Graph(null, minvalue, maxvalue, 0, maxY, width, height, translateX, translateY);
    }

    public final void sizeToFit() {
         Dimension current=this.getSize();
         if (current.width<minWidth+translateX+30) width=minWidth; else width=(current.width-(translateX+30));
         if (current.height<minHeight+translateY+30) height=minHeight; else height=(current.height-(translateY+30));
         if (graph!=null) graph.setSize(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (graph==null) updateHistogram(null);
        if (graph==null || bins==null) return;
        graph.setGraphics((Graphics2D)g);
        g.setColor(this.getBackground());
        g.fillRect(0, 0, width+110, height+100);
        g.setColor(Color.WHITE);
        g.fillRect(graph.getXforValue(minvalue),graph.getYforValue(maxY), width, height);
        graph.drawAxes(Graph.BOX, Graph.NONE, true);
        g.setColor(Color.RED);
        graph.drawHistogram(bins);
        if (highlightvalues!=null) {
            g.setColor(new Color(0,0,255,60));
            int yBottom=graph.getYforValue(0);
            int yTop=graph.getYforValue(maxY);
            for (int i=0;i<highlightvalues.length;i++) {
                if (Double.isNaN(highlightvalues[i]) || Double.isInfinite(highlightvalues[i])) continue; // skip these values if present
                int x=graph.getXforValue(highlightvalues[i]);
                g.drawLine(x,yTop,x,yBottom);
            }
        }
        g.setColor(Color.BLACK);
        graph.drawBoundingBox();
    }

    private void fillBins(MotifLabEngine engine, Class type) {
        entryCount=0;
        explicitValueCount=0;
        invalidCount=0;
        double sum=0;
        minvalue=Double.MAX_VALUE;
        maxvalue=-Double.MAX_VALUE;            
        ArrayList<Data> all=engine.getAllDataItemsOfType(type);
        if (all.isEmpty()) {bins=null;return;}
        for (Data entry:all) {
            entryCount++;
            if (map.contains(entry.getName())) explicitValueCount++;
            double val=map.getValue(entry.getName());
            sum+=val;
            if (Double.isNaN(val) || Double.isInfinite(val)) {invalidCount++;continue;} // skip these values if present
            if (val>maxvalue) maxvalue=val;
            if (val<minvalue) minvalue=val;
        }
        double validCount=(entryCount-invalidCount);
        averagevalue=(validCount!=0)?(sum/validCount):Double.NaN;
        double binrange=(maxvalue-minvalue)/(double)bincount;
        bins=new double[bincount];
        for (Data entry:all) {
            double value=map.getValue(entry.getName());
            if (Double.isNaN(value) || Double.isInfinite(value)) continue; // skip these values if present
            int bin=(int)((value-minvalue)/binrange);
            if (bin>=bincount) bin=bincount-1;
            bins[bin]++;
        }
        for (int i=0;i<bins.length;i++) {
            if (bins[i]>maxY) maxY=bins[i];
        }
        maxY=(int)(maxY*1.1); // make the top of the graph 10% higher than the highest bin
        DistributionPanel.this.setToolTipText("<html>Min value: "+minvalue+"<br>Max value: "+maxvalue+"<br>Average value: "+averagevalue+"<br>Total entries: "+entryCount+"<br>Explicit entries: "+explicitValueCount+"<br>Invalid values: "+invalidCount+"</html>");
  }


  /** 
   * This method can be used to set a number of values which will be marked by vertical lines
   * in the histogram. It can for instance be used to show the values for selected map entries
   * Use NULL as argument to turn off the functionality
   * @param values
   */
  public void highlight(double[] values) {
      highlightvalues=values;
  }

}

