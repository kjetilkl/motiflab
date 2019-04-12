/*
 * This panel displays the contents of a DataPartition as a pie chart
 */
package motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import motiflab.engine.Graph;
import motiflab.engine.data.DataPartition;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
/**
 *
 * @author Kjetil
 */
public class PartitionPiePanel extends JPanel {
    private int minPieSize=250; // 
    private DataPartition partition;
    private Graph graph;
    private JScrollPane scrollpane;
    private GraphPanel innerpanel;
    private VisualizationSettings settings;    
    private JCheckBox orderBySizeCheckbox;
    
    private double[] values;
    private String[] clusters;
    private Color[] colors;
            
    private static final int topLeft=20;
    private static final int spacingBetween=40;
    private static final int legendTop=20;
    private static final int margin=30;
    private static final int controlsPanelHeight=20;
    private Dimension legendDim=new Dimension();

    public PartitionPiePanel(DataPartition partition, VisualizationSettings settings) {
        this.partition=partition;
        this.settings=settings;
        innerpanel=new GraphPanel();
        scrollpane=new JScrollPane(innerpanel);
        scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setLayout(new BorderLayout());
        this.add(scrollpane);
        JPanel controlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        orderBySizeCheckbox=new JCheckBox("Order by size",false);
        orderBySizeCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGraph();
                innerpanel.repaint();
            }
        });
        controlsPanel.add(orderBySizeCheckbox);
        this.add(controlsPanel,BorderLayout.SOUTH);
        graph=new Graph(null, 0, 1, 0, 1, minPieSize, minPieSize, topLeft, topLeft);
        updateGraph();
    }
        
    private void updateGraph() {
        ArrayList<String> names=partition.getClusterNames(); // the cluster names will be returned in sorted order
        final ArrayList<String> unassigned=partition.getAllUnassigned(settings.getEngine());
        int piesegments=names.size();
        if (!unassigned.isEmpty()) { // show "unassigned" as its own segment
            names.add("<Unassigned>");
            piesegments++;
        } 
        clusters=new String[piesegments];
        values=new double[piesegments];
        colors=new Color[piesegments];
        // sort the clusters by name or size
        if (orderBySizeCheckbox.isSelected()) {
            Collections.sort(names, new Comparator<String>() {
                @Override
                public int compare(String name1, String name2) {
                    int size1=(name1.equals("<Unassigned>"))?unassigned.size():partition.getClusterSize(name1);
                    int size2=(name2.equals("<Unassigned>"))?unassigned.size():partition.getClusterSize(name2);
                    return Integer.compare(size1, size2)*(-1); // decreasing order
                }            
            });
        }
        // 
        for (int i=0;i<names.size();i++) {
            String clustername=names.get(i);
            if (clustername.equals("<Unassigned>")) {
                values[i]=unassigned.size();            
                clusters[i]="<Unassigned> = "+(int)values[i];
                colors[i]=null;                   
            } else {
                values[i]=partition.getClusterSize(clustername);            
                clusters[i]=clustername+" = "+(int)values[i];
                colors[i]=settings.getClusterColor(clustername);
            }
        }
        Dimension dim=Graph.getLegendDimension(clusters, null);
        legendDim.width=dim.width;legendDim.height=dim.height;
        int minwidth=minPieSize+dim.width+topLeft+spacingBetween+margin;
        int minheight=minPieSize+topLeft+margin;
        if (dim.height+margin+legendTop>minheight) minheight=dim.height+margin+legendTop;
        dim.width=minwidth;
        dim.height=minheight;
        innerpanel.setPreferredSize(dim);
        innerpanel.revalidate();        
    }
    
    private class GraphPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            graph.setGraphics((Graphics2D)g);
            g.setColor(this.getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());     
            for (int i=0;i<colors.length;i++) {
                if (colors[i]==null) colors[i]=this.getBackground(); // unassigned color
            }
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int availableWidth=PartitionPiePanel.this.getWidth()-(legendDim.width+margin+topLeft+spacingBetween);
            int availableHeight=PartitionPiePanel.this.getHeight()-(margin+topLeft+controlsPanelHeight);
            
            if (availableWidth<minPieSize) availableWidth=minPieSize;  
            if (availableHeight<minPieSize) availableHeight=minPieSize;
            int piesize=(availableWidth<availableHeight)?availableWidth:availableHeight;
            graph.setSize(piesize, piesize);
            //graph.drawPieChartShadow(10);
            graph.drawPieChart(values, colors, true, true, true);
            graph.drawLegendBox(clusters, colors, topLeft+piesize+spacingBetween, legendTop, true);
        }
        
    }


}

