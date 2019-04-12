/*
 * PositionalDistributionViewer.java
 *
 * Created on 25.apr.2011, 22:55:03
 */

package motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.data.Data;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;

/**
 *
 * @author Kjetil
 */
public class PositionalDistributionViewer extends javax.swing.JDialog implements RedrawListener {
    private int numberOfHistograms=6;
    private double[][] counts; // first index is histogram. second is bin. These are unnormalized counts
    private double[] normalizationConstants; // one for each histogram
    private int[] totalcounts;// one for each histogram
    private int[] supportcounts;// one for each histogram
    private int[] visibleSeq; //one for each histogram;
    private double[] binspan; //one for each histogram;
    private boolean[] visible;
    private Color[] colors;
    private Color[] translucentColors;
    private int currentHistogram=-1;   // the histogram currently being updated
    private JToggleButton[] histogramButtons;
    private Graph graph;
    private int width=400;
    private int height=200;
    private int minWidth=200;
    private int minHeight=100;
    private int translateX=50;
    private int translateY=24;
    private MotifLabGUI gui;
    private String error=null;
    private int relativeStart=0;
    private int relativeEnd=0;
    private Exception ex;
    private GraphPanel panel;
    Dimension dim=new Dimension(0,0);
    private double maxValue=0;
    private boolean manualclose=false; // closed by pressing CLOSE-button?
    private String lastAlignSetting=null;
    
    private static final int ALIGN_UPSTREAM=0;
    private static final int ALIGN_DOWNSTREAM=1;
    private static final int ALIGN_TSS=2;
    private static final int ALIGN_TES=3;
    private static final int ALIGN_CENTER=4;    
    
    private static final int ANCHOR_UPSTREAM=0;
    private static final int ANCHOR_DOWNSTREAM=1;
    private static final int ANCHOR_CENTER=2;
    private static final int ANCHOR_SPAN=3;
    private SwingWorkerUpdater previousUpdater=null;
    

    /** Creates new form PositionalDistributionViewer */
    public PositionalDistributionViewer(MotifLabGUI gui) {
        super(gui.getFrame(), "Positional Distribution Viewer", false);
        this.gui=gui;
        colors=new Color[numberOfHistograms];
        translucentColors=new Color[numberOfHistograms];
        for (int i=0;i<numberOfHistograms;i++) {
            colors[i]=gui.getVisualizationSettings().getSystemColor("color"+(i+1));
            translucentColors[i]=new Color(colors[i].getRed(),colors[i].getGreen(),colors[i].getBlue(),80);
        }
        counts=new double[numberOfHistograms][];
        normalizationConstants=new double[numberOfHistograms];
        totalcounts=new int[numberOfHistograms];
        supportcounts=new int[numberOfHistograms];
        visibleSeq=new int[numberOfHistograms];
        binspan=new double[numberOfHistograms];
        visible=new boolean[numberOfHistograms];        
        visible[0]=true;        
        dim.setSize(width+translateX+20,height+translateY+30);
        initComponents();
        ActionListener updateListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource()==alignCombobox) { // reset counts!
                    String newAlign=(String)alignCombobox.getSelectedItem();
                    if (newAlign.equals(lastAlignSetting)) return;
                    lastAlignSetting=newAlign;
                    counts=new double[numberOfHistograms][];
                    normalizationConstants=new double[numberOfHistograms];
                    totalcounts=new int[numberOfHistograms];
                    supportcounts=new int[numberOfHistograms];
                    visibleSeq=new int[numberOfHistograms]; 
                    binspan=new double[numberOfHistograms];                    
                }
                update();
            }
        };
        anchorCombobox.addActionListener(updateListener);
        alignCombobox.addActionListener(updateListener);
        trackCombobox.addActionListener(updateListener);
        flattenCheckbox.addActionListener(updateListener);

        panel=new GraphPanel();
        panel.setBackground(Color.WHITE);
        panel.setMinimumSize(dim);
        middlePanel.add(getHistogramButtonsPanel(),BorderLayout.SOUTH);
        mainPanel.add(panel);
        progressbar.setVisible(false);
        ArrayList<Data> datasets=gui.getEngine().getAllDataItemsOfType(RegionDataset.class);
        ArrayList<String> names=new ArrayList<String>(datasets.size());
        for (Data data:datasets) {
            // if (((RegionDataset)data).isMotifTrack()) names.add(data.getName());
            names.add(data.getName());
        }
        Collections.sort(names);
        DefaultComboBoxModel tracksModel=new DefaultComboBoxModel(names.toArray());
        trackCombobox.setModel(tracksModel);
        
        ArrayList<String> seqColNames=gui.getEngine().getNamesForAllDataItemsOfType(SequenceCollection.class);
        Collections.sort(seqColNames);
        DefaultComboBoxModel seqColModel=new DefaultComboBoxModel(seqColNames.toArray());
        sequenceCollectionCombobox.setModel(seqColModel);   
        sequenceCollectionCombobox.setSelectedItem(gui.getEngine().getDefaultSequenceCollectionName());
        sequenceCollectionCombobox.addActionListener(updateListener);        
        alignCombobox.setSelectedItem("TSS");
        lastAlignSetting=(String)alignCombobox.getSelectedItem();
        mainPanel.addComponentListener(new ComponentListener() {
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
        this.setPreferredSize(new Dimension(740,400));
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                if (!manualclose) closeButton.doClick();
            }
            
        });      
        binsSpinner.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (autoUpdateButton.isSelected()) update();
            }
        });
        autoUpdateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (autoUpdateButton.isSelected()) update();
            }
        });
        pack();
        update();
        gui.addRedrawListener(this);
    }

    /** Creates a new graph object for the current counts and calls repaint() to update painting */
    private void updateGraphics() {
        // Note: this is apparently not called on the EDT, but I think it is OK
        maxValue=0;
        int counted=0;
        for (int i=0;i<numberOfHistograms;i++) {
            if (counts[i]==null || !visible[i]) continue;
            for (int j=0;j<counts[i].length;j++) if (counts[i][j]>maxValue) maxValue=counts[i][j];
            counted++;
        }
        if (error!=null) {
            progressbar.setVisible(false);
            graph=null;
            repaint();
            return;
        }
        maxValue = maxValue * 1.1; // just to give some margin on top
        if (maxValue>1.0) maxValue=1.0; // the y-axis is normalized to [0,1]
        // the graphics object for the graph will be set later on
        if (graph==null) graph=new Graph(null, relativeStart, relativeEnd, 0, maxValue, width, height, translateX, translateY);
        else graph.setLimits(relativeStart, relativeEnd, 0, maxValue);
        sizeToFit();
        repaint();
    }

    private void sizeToFit() {
        Dimension currentsize=panel.getSize();
        if (currentsize.width<minWidth+translateX+20) width=minWidth; else width=(currentsize.width-(translateX+20));
        if (currentsize.height<minHeight+translateY+30) height=minHeight; else height=(currentsize.height-(translateY+30));
        dim.setSize(width+translateX+20,height+translateY+30);
        if (graph!=null) graph.setSize(width, height);
    }

    /** Draws the current histogram (or an error message) */
    private void drawHistogram(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, dim.width, dim.height);      
        if (error!=null) {
            g.setColor(Color.BLACK);
            int errorwidth=g.getFontMetrics().stringWidth(error);
            int xoffset=(dim.width-errorwidth)/2;
            if (xoffset<0) xoffset=0;
            g.drawString(error, xoffset, (int)(dim.height/2));
            return;
        }         
        if (counts==null || graph==null) return;
        for (int i=0;i<numberOfHistograms;i++) {
            if (!visible[i] || counts[i]==null) continue;
            g.setColor(translucentColors[i]);
            graph.drawHistogram(counts[i],false);
        }
        g.setColor(Color.BLACK);
        graph.drawBoundingBox();
        graph.drawXaxisWithTicks(height+translateY, false, false);
        graph.drawYaxisWithTicks(translateX, false, true);       
    }

    /** Recalculates the histogram and updates the graphics */
    private void update() {
        progressbar.setVisible(true);
        String datasetname=(String)trackCombobox.getSelectedItem();
        if (datasetname==null) {error="No dataset selected";updateGraphics();return;}
        Data data=gui.getEngine().getDataItem(datasetname);
        if (!(data instanceof RegionDataset)) {
            if (data==null) error="ERROR: Selected dataset is missing"; 
            else error="ERROR: Selected dataset is not a Region Dataset"; 
            updateGraphics();
            return;
        }
        final RegionDataset regiondataset=(RegionDataset)data;
        
        String seqColName=(String)sequenceCollectionCombobox.getSelectedItem();
        if (seqColName==null) {seqColName=gui.getEngine().getDefaultSequenceCollectionName();}
        Data seqCol=gui.getEngine().getDataItem(seqColName);
        if (!(seqCol instanceof SequenceCollection)) {
            error="ERROR: No such sequence collection"; 
            updateGraphics();
            return;
        }        
        final SequenceCollection sequencecollection=(SequenceCollection)seqCol;
        
        int numbins=(Integer)binsSpinner.getValue();
        if (previousUpdater!=null) {
            if (!previousUpdater.isDone()) previousUpdater.abortUpdate(); // abort if another updater is already running
        }
        SwingWorkerUpdater worker=new SwingWorkerUpdater(regiondataset,sequencecollection,numbins);
        previousUpdater=worker;
        worker.execute();
    }

    private synchronized void replaceCounts(double[] newcounts, int totalcounts, int supportcount, int sequencecount, int histogramIndex, double binspan) {
        if (currentHistogram<0) return;
        if (counts[histogramIndex]==null || counts[histogramIndex].length!=newcounts.length) counts[histogramIndex]=new double[newcounts.length];
        System.arraycopy(newcounts, 0, counts[histogramIndex], 0, counts[histogramIndex].length);
        this.supportcounts[histogramIndex]=supportcount;
        this.totalcounts[histogramIndex]=totalcounts;
        this.visibleSeq[histogramIndex]=sequencecount;
        this.binspan[histogramIndex]=binspan;        
    }
    
    @Override
    public void redrawEvent() {
        if (autoUpdateButton.isSelected()) update();
    }

    private JPanel getHistogramButtonsPanel() {
        JPanel bpanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        histogramButtons=new JToggleButton[numberOfHistograms];
        for (int i=0;i<numberOfHistograms;i++) {
            final int index=i;
            JToggleButton button=new JToggleButton(""+(i+1));
            histogramButtons[i]=button;
            button.setBorder(BorderFactory.createEmptyBorder(4,16,4,16));
            button.setOpaque(false);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JToggleButton source=(JToggleButton)e.getSource();
                    boolean selected=source.isSelected();
                    if (index!=currentHistogram) { // make this current and select it!
                       setHistogramVisible(index,true);                       
                       setCurrentHistogram(index,true);
                       source.setBackground(colors[index]);                        
                       source.setSelected(true); // it might have been selected already, or not
                    } else if (selected) { // current histogram which is -> unselect it 
                       setHistogramVisible(index,false);
                       setCurrentHistogram(index,false);
                       source.setBackground(null);
                       source.setSelected(false);                       
                    } else { // this clause has the exact same contents as the one above, but it is needed!
                       setHistogramVisible(index,false);
                       setCurrentHistogram(index,false);
                       source.setBackground(null);
                       source.setSelected(false);                        
                    }
                    updateGraphics();
                }
            });
            bpanel.add(button);
        }
//        JCheckBox normalizeCheckbox = new JCheckBox("Normalize",true);
//        normalizeCheckbox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                renormalize(((JCheckBox)e.getSource()).isSelected());
//            }
//        });
//        bpanel.add(new JLabel("   "));
//        bpanel.add(normalizeCheckbox);
        histogramButtons[0].doClick();

        return bpanel;
    }


    private void setHistogramVisible(int histogramindex, boolean isvisible) {
       visible[histogramindex]=isvisible;
    }
    
    private void setCurrentHistogram(int histogramindex, boolean isSelected) {
       if (isSelected) currentHistogram=histogramindex;
       else { 
          int firstSelected=-1;
          for (int i=0;i<numberOfHistograms;i++) {
             if (histogramButtons[i].isSelected()) {firstSelected=i;break;}
          }
          currentHistogram=firstSelected;
       }
       for (int i=0;i<numberOfHistograms;i++) {
         histogramButtons[i].setForeground((i==currentHistogram)?Color.WHITE:Color.BLACK);
       }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        trackLabel = new javax.swing.JLabel();
        trackCombobox = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        sequenceCollectionCombobox = new javax.swing.JComboBox();
        progressbar = new javax.swing.JProgressBar();
        middlePanel = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        bottomPanel = new javax.swing.JPanel();
        controlsPanel = new javax.swing.JPanel();
        binsLabel = new javax.swing.JLabel();
        binsSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        alignCombobox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        anchorCombobox = new javax.swing.JComboBox();
        flattenCheckbox = new javax.swing.JCheckBox();
        okCloseButtonsPanel = new javax.swing.JPanel();
        autoUpdateButton = new javax.swing.JToggleButton();
        updateButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        topPanel.setName("topPanel"); // NOI18N
        topPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(PositionalDistributionViewer.class);
        trackLabel.setText(resourceMap.getString("trackLabel.text")); // NOI18N
        trackLabel.setName("trackLabel"); // NOI18N
        topPanel.add(trackLabel);

        trackCombobox.setName("trackCombobox"); // NOI18N
        topPanel.add(trackCombobox);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        topPanel.add(jLabel3);

        sequenceCollectionCombobox.setName("sequenceCollectionCombobox"); // NOI18N
        topPanel.add(sequenceCollectionCombobox);

        progressbar.setMaximumSize(new java.awt.Dimension(60, 22));
        progressbar.setMinimumSize(new java.awt.Dimension(40, 22));
        progressbar.setName("progressbar"); // NOI18N
        progressbar.setPreferredSize(new java.awt.Dimension(40, 22));
        topPanel.add(progressbar);

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        middlePanel.setName("middlePanel"); // NOI18N
        middlePanel.setLayout(new java.awt.BorderLayout());

        mainPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6), javax.swing.BorderFactory.createEtchedBorder()));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());
        middlePanel.add(mainPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(middlePanel, java.awt.BorderLayout.CENTER);

        bottomPanel.setName("bottomPanel"); // NOI18N
        bottomPanel.setLayout(new java.awt.BorderLayout());

        controlsPanel.setName("controlsPanel"); // NOI18N

        binsLabel.setText(resourceMap.getString("binsLabel.text")); // NOI18N
        binsLabel.setName("binsLabel"); // NOI18N
        controlsPanel.add(binsLabel);

        binsSpinner.setModel(new javax.swing.SpinnerNumberModel(50, 0, 1000, 1));
        binsSpinner.setToolTipText(resourceMap.getString("binsSpinner.toolTipText")); // NOI18N
        binsSpinner.setName("binsSpinner"); // NOI18N
        controlsPanel.add(binsSpinner);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        controlsPanel.add(jLabel1);

        alignCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TSS", "TES", "Upstream", "Downstream", "Center" }));
        alignCombobox.setToolTipText(resourceMap.getString("alignCombobox.toolTipText")); // NOI18N
        alignCombobox.setName("alignCombobox"); // NOI18N
        controlsPanel.add(alignCombobox);

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        controlsPanel.add(jLabel2);

        anchorCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Center", "Upstream", "Downstream", "Span" }));
        anchorCombobox.setToolTipText(resourceMap.getString("anchorCombobox.toolTipText")); // NOI18N
        anchorCombobox.setName("anchorCombobox"); // NOI18N
        controlsPanel.add(anchorCombobox);

        flattenCheckbox.setText(resourceMap.getString("flattenCheckbox.text")); // NOI18N
        flattenCheckbox.setToolTipText(resourceMap.getString("flattenCheckbox.toolTipText")); // NOI18N
        flattenCheckbox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        flattenCheckbox.setName("flattenCheckbox"); // NOI18N
        controlsPanel.add(flattenCheckbox);

        bottomPanel.add(controlsPanel, java.awt.BorderLayout.WEST);

        okCloseButtonsPanel.setName("okCloseButtonsPanel"); // NOI18N
        okCloseButtonsPanel.setPreferredSize(new java.awt.Dimension(193, 40));
        okCloseButtonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        autoUpdateButton.setIcon(resourceMap.getIcon("autoUpdateButton.icon")); // NOI18N
        autoUpdateButton.setSelected(true);
        autoUpdateButton.setText(resourceMap.getString("autoUpdateButton.text")); // NOI18N
        autoUpdateButton.setToolTipText(resourceMap.getString("autoUpdateButton.toolTipText")); // NOI18N
        autoUpdateButton.setAlignmentX(0.5F);
        autoUpdateButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        autoUpdateButton.setMaximumSize(new java.awt.Dimension(27, 23));
        autoUpdateButton.setMinimumSize(new java.awt.Dimension(27, 23));
        autoUpdateButton.setName("autoUpdateButton"); // NOI18N
        autoUpdateButton.setPreferredSize(new java.awt.Dimension(27, 23));
        okCloseButtonsPanel.add(autoUpdateButton);

        updateButton.setText(resourceMap.getString("updateButton.text")); // NOI18N
        updateButton.setName("updateButton"); // NOI18N
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonPressed(evt);
            }
        });
        okCloseButtonsPanel.add(updateButton);

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonPressed(evt);
            }
        });
        okCloseButtonsPanel.add(closeButton);

        bottomPanel.add(okCloseButtonsPanel, java.awt.BorderLayout.EAST);

        getContentPane().add(bottomPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonPressed
        manualclose=true;
        setVisible(false);
        gui.removeRedrawListener(this);
        this.dispose();
    }//GEN-LAST:event_closeButtonPressed

    private void updateButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonPressed
        update();
    }//GEN-LAST:event_updateButtonPressed

    private class GraphPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);          
            if (graph!=null) graph.setGraphics((Graphics2D)g);
            drawHistogram((Graphics2D)g);
            if (currentHistogram>=0 && visible[currentHistogram]) {
                 g.setColor(colors[currentHistogram]);
                 if (graph!=null) graph.drawAlignedString(totalcounts[currentHistogram]+" region"+((totalcounts[currentHistogram]!=1)?"s":"")+" in "+supportcounts[currentHistogram]+" out of "+visibleSeq[currentHistogram]+" sequence"+((visibleSeq[currentHistogram]!=1)?"s":"")+".  Bin width="+MotifLabGUI.formatNumber(binspan[currentHistogram])+"bp", translateX+10, translateY-5, 0, 0);
            }
        }

    }



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox alignCombobox;
    private javax.swing.JComboBox anchorCombobox;
    private javax.swing.JToggleButton autoUpdateButton;
    private javax.swing.JLabel binsLabel;
    private javax.swing.JSpinner binsSpinner;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel controlsPanel;
    private javax.swing.JCheckBox flattenCheckbox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel middlePanel;
    private javax.swing.JPanel okCloseButtonsPanel;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JComboBox sequenceCollectionCombobox;
    private javax.swing.JPanel topPanel;
    private javax.swing.JComboBox trackCombobox;
    private javax.swing.JLabel trackLabel;
    private javax.swing.JButton updateButton;
    // End of variables declaration//GEN-END:variables


   private class SwingWorkerUpdater extends SwingWorker {
        private double[] newcounts; 
        private int countssum=0; // total number of regions
        private int countsupport=0; // number of sequences that contains visible regions
        private int countseq=0;  // total number of visible sequences (from collection)     
        private double binspan=0;
        private boolean abort=false;
        RegionDataset regiondataset;
        SequenceCollection collection;        
        
        public SwingWorkerUpdater (RegionDataset regiondataset, SequenceCollection collection, int numbins) {
            this.regiondataset=regiondataset;
            this.collection=collection;
            newcounts=new double[numbins];
        }
       
        public void abortUpdate() {
            abort=true;
            this.cancel(true);
        }
        
        @Override
        protected Object doInBackground() throws Exception {  
            //final long startTime = System.currentTimeMillis();            
            countRegions(regiondataset,collection);          
            if (abort) return null;            
            replaceCounts(newcounts,countssum,countsupport,countseq,currentHistogram,binspan);
            updateGraphics();   
            //final long endTime = System.currentTimeMillis();
            //gui.logMessage("Processing all regions took: " + (endTime - startTime)+" milliseconds" );                 
            return null;
        }
        @Override
        protected void done() {
            if (abort) return;
            progressbar.setVisible(false);
        }
        
    // this method will update the global counts[] variable which is the basis for the histogram
    private void countRegions(RegionDataset dataset,SequenceCollection seqCollection) {
        VisualizationSettings settings=gui.getVisualizationSettings();
        int total=0;
        int span=0;
        try {
            // NOTE: in earlier versions (before 2.0.-2) the seqCollection parameter was used as the second argument
            //       However, if one wants create two histograms based on two different sequence subsets, this could lead
            //       to undesirable behaviour if the sequences subsets have sequences of varying sizes (which are aligned differently).
            //       Therefore, to promote consistent behaviour we shall from now on always use the full set of sequences to determine span.
            span=determineSequenceSpan(dataset,gui.getEngine().getDefaultSequenceCollection());  // span=determineSequenceSpan(dataset,seqCollection)
        } catch (Exception e) {
            error="ERROR: "+e.getMessage();
            return;
        }
        error=null;
        binspan=(double)span/(double)newcounts.length;
        RegionVisualizationFilter filter=gui.getRegionVisualizationFilter();
        for (int i=0;i<newcounts.length;i++) newcounts[i]=0; // clear counts

        int seqCount=0;
        int support=0;
        int align=ALIGN_UPSTREAM;
        int anchor=ANCHOR_UPSTREAM;
        String alignString=(String)alignCombobox.getSelectedItem();
             if (alignString.equals("Upstream")) align=ALIGN_UPSTREAM;
        else if (alignString.equals("Downstream")) align=ALIGN_DOWNSTREAM;
        else if (alignString.equals("TSS")) align=ALIGN_TSS;
        else if (alignString.equals("TES")) align=ALIGN_TES; 
        else if (alignString.equals("Center")) align=ALIGN_CENTER;              
        String anchorString=(String)anchorCombobox.getSelectedItem();
             if (anchorString.equals("Upstream")) anchor=ANCHOR_UPSTREAM;
        else if (anchorString.equals("Downstream")) anchor=ANCHOR_DOWNSTREAM;
        else if (anchorString.equals("Center")) anchor=ANCHOR_CENTER;
        else if (anchorString.equals("Span")) anchor=ANCHOR_SPAN;
        boolean flatten=flattenCheckbox.isSelected();
        boolean[] flattenedCounts=(flatten)?new boolean[newcounts.length]:null;
        for (FeatureSequenceData seq:dataset.getSequencesFromCollection(seqCollection)) {
           if (!settings.isSequenceVisible(seq.getSequenceName())) continue;
           if (flattenedCounts!=null) for (int i=0;i<flattenedCounts.length;i++) flattenedCounts[i]=false; // reset flattened counts
           ArrayList<Region> list=((RegionSequenceData)seq).getAllRegions();
           boolean reverse=(seq.getStrandOrientation()==Sequence.REVERSE);
           int regionsInSequence=0;
           for (Region region:list) {
                if (abort) return;
                // count only currently visible regions
                if (!settings.isRegionTypeVisible(region.getType())) continue;
                if (filter!=null && !filter.shouldVisualizeRegion(region)) continue;
                regionsInSequence++;
                if (anchor==ANCHOR_SPAN) { // can span multiple bins
                   int startBinIndex=getBinForRegion(seq, region, ANCHOR_UPSTREAM, align, reverse); 
                   int endBinIndex=getBinForRegion(seq, region, ANCHOR_DOWNSTREAM, align, reverse); 
                   if (startBinIndex>endBinIndex) {int swap=startBinIndex;startBinIndex=endBinIndex;endBinIndex=swap;}
                   if (endBinIndex<0 || startBinIndex>=newcounts.length) continue; // region is fully outside
                   if (startBinIndex<0) startBinIndex=0;
                   if (endBinIndex>=newcounts.length) endBinIndex=newcounts.length-1;
                   if (flatten) {
                      for (int i=startBinIndex;i<=endBinIndex;i++) flattenedCounts[i]=(flattenedCounts[i]||true); // count each bin only once
                   } else { //
                      for (int i=startBinIndex;i<=endBinIndex;i++) newcounts[i]+=1;
                   }
                   total++;
                } else { // single bin
                    int binIndex=getBinForRegion(seq, region, anchor, align, reverse);
                    if (binIndex>=0 && binIndex<newcounts.length) {
                        if (flatten) flattenedCounts[binIndex]=(flattenedCounts[binIndex]||true);
                        else newcounts[binIndex]+=1;
                        total++;
                    } 
                }
           }
           if (flattenedCounts!=null) { // increase counts from flattenedCounts
              for (int i=0;i<flattenedCounts.length;i++) {
                  if (flattenedCounts[i]) newcounts[i]++;
              } //
           }
           seqCount++; // count visible sequence            
           if (regionsInSequence>0) support++; // sequence contains visible regions           
           setProgressValue((int)(seqCount/seqCollection.size()));
           this.cancel(manualclose);
        }
        // normalize bins
        double divideby=(flatten)?(double)seqCount:(double)total;
        for (int i=0;i<newcounts.length;i++) {
            newcounts[i]=newcounts[i]/divideby;
        }
        countssum=total;
        countseq=seqCount;  
        countsupport=support;
    }

    private int getBinForRegion(FeatureSequenceData seq, Region region, int anchor, int align, boolean reverse) {
        double relativepos=0; // this will usually be an integer
        if (anchor==ANCHOR_CENTER) relativepos=(reverse)?((seq.getSize()-1)-(region.getRelativeStart()+(double)region.getLength()/2.0)):(region.getRelativeStart()+(double)region.getLength()/2.0);
        else if (anchor==ANCHOR_UPSTREAM) relativepos=(reverse)?((seq.getSize()-1)-region.getRelativeEnd()):(region.getRelativeStart());
        else if (anchor==ANCHOR_DOWNSTREAM) relativepos=(reverse)?((seq.getSize()-1)-region.getRelativeStart()):(region.getRelativeEnd());
        // have relative position of anchor point wrt upstream start of sequence
        // now adjust this position with respect to selected alignment
        if (align==ALIGN_DOWNSTREAM) {
            double basesfromend=seq.getSize()-relativepos;
            relativepos=-(relativeStart+basesfromend);           
         } else if (align==ALIGN_CENTER) {
           int center=(int)((seq.getRegionStart()+seq.getRegionEnd())/2.0);
           int sequpstream=(reverse)?(seq.getRegionEnd()-center):(center-seq.getRegionStart());
           int seqOffset=Math.abs(relativeStart)-sequpstream;
           relativepos+=seqOffset;         
         } else if (align==ALIGN_TSS) {
           int tss=seq.getTSS().intValue();
           int sequpstream=(reverse)?(seq.getRegionEnd()-tss):(tss-seq.getRegionStart());
           int seqOffset=Math.abs(relativeStart)-sequpstream;
           relativepos+=seqOffset;
        } else if (align==ALIGN_TES) {
           int tes=seq.getTES().intValue();
           int sequpstream=(reverse)?(seq.getRegionEnd()-tes):(tes-seq.getRegionStart());
           int seqOffset=Math.abs(relativeStart)-sequpstream;
           relativepos+=seqOffset;                   
        }
        int binIndex=(int)(relativepos/binspan);       
        return binIndex;
    }


    /** Determines the total sequence span based on the length of the sequences (subset) and selected alignment
     *  Also updates a few global fields
     */
    private int determineSequenceSpan(RegionDataset dataset, SequenceCollection seqCollection) throws ExecutionError {
         String alignment = (String)alignCombobox.getSelectedItem();
         if (alignment.equals("Upstream") || alignment.equals("Downstream") || alignment.equals("Center")) {
             int length=0; // the length of the longest sequence
             for (FeatureSequenceData seq:dataset.getSequencesFromCollection(seqCollection)) {
                 int seqlen=seq.getSize();
                 if (seqlen>length) length=seqlen; // length is determined by longest sequence
             }
             if (alignment.equals("Upstream")) {relativeStart=0;relativeEnd=length;} // maybe this is one to big :|
             else if (alignment.equals("Downstream")) {relativeStart=-length;relativeEnd=0;} // maybe this is one to big :|
             else {int halfsize=(int)(length/2.0);relativeStart=-halfsize;relativeEnd=-halfsize+length;} // maybe this is one to big :|
             return length;
         } else if (alignment.equals("TSS")) {
             int upstream=0;
             int downstream=0;
             for (FeatureSequenceData seq:dataset.getSequencesFromCollection(seqCollection)) {
                 Integer TSS=seq.getTSS();
                 if (TSS==null) throw new ExecutionError("Missing TSS (maybe try a different alignment)");
                 int tss=TSS.intValue();
                 boolean direct=seq.isOnDirectStrand();
                 int sequpstream=(direct)?(tss-seq.getRegionStart()):(seq.getRegionEnd()-tss);
                 int seqdownstream=(direct)?(seq.getRegionEnd()-tss):(tss-seq.getRegionStart());
                 if (sequpstream>upstream) upstream=sequpstream;
                 if (seqdownstream>downstream) downstream=seqdownstream;
             }
             relativeStart=-upstream;
             relativeEnd=downstream;            
             return upstream+downstream+1; // +1 is for TSS (which is not included in either upstream or downstream)
         } else if (alignment.equals("TES")) {
             int upstream=0;
             int downstream=0;
             for (FeatureSequenceData seq:dataset.getSequencesFromCollection(seqCollection)) {
                 Integer TES=seq.getTES();
                 if (TES==null) throw new ExecutionError("Missing TES (maybe try a different alignment)");
                 int tes=TES.intValue();
                 boolean direct=seq.isOnDirectStrand();
                 int sequpstream=(direct)?(tes-seq.getRegionStart()):(seq.getRegionEnd()-tes);
                 int seqdownstream=(direct)?(seq.getRegionEnd()-tes):(tes-seq.getRegionStart());
                 if (sequpstream>upstream) upstream=sequpstream;
                 if (seqdownstream>downstream) downstream=seqdownstream;
             }
             relativeStart=-upstream;
             relativeEnd=downstream;
             return upstream+downstream+1; // +1 is for TES (which is not included in either upstream or downstream)
         } else return 0; // this should not happen!
    }

    private void setProgressValue(final int percentage) {
        Runnable runner=new Runnable() {
            @Override
            public void run() {
                progressbar.setValue(percentage);
            }
        };
        SwingUtilities.invokeLater(runner);
    }
    
    };

}
