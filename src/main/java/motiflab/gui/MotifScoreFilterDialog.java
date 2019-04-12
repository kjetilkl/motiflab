/*
 * MotifScoreFilterDialog.java
 *
 * Created on 14. april 2010, 14:53
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Rectangle;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import motiflab.engine.DataListener;
import motiflab.engine.data.Data;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;

/**
 * This tool has been renamed "Region Score Filter" in MotifLab version 2.0 (but I have kept the old name in the code)
 * @author  kjetikl
 */
public class MotifScoreFilterDialog extends javax.swing.JDialog implements ChangeListener, RegionVisualizationFilter, DataListener, VisualizationSettingsListener {
    
    private MotifLabGUI gui;
    private VisualizationSettings settings=null;
    double currentScoreThreshold=0;    
    double maxvalue=0;
    double minvalue=0;    
    boolean isForciblyClosed=false;
    int property=SCORE;
    NumericDataset numericTrack=null;
    private boolean greaterThan=true;
    private MiscIcons greaterThanIcon=new MiscIcons(MiscIcons.GREATER_THAN_OR_EQUAL);
    private MiscIcons smallerThanIcon=new MiscIcons(MiscIcons.SMALLER_THAN_OR_EQUAL);
    private ContextMenu popup=null;
    private Color VERY_LIGHT_GRAY=new Color(225,225,225);
    private Color LIGHTER_GRAY=new Color(212,212,212);    
    private Color COLOR_GREEN=Color.GREEN;
    private Color COLOR_RED=Color.RED;
    private JPopupMenu selectColorMenu;    
    
    private SimpleDataPanelIcon matchingRegionColorIcon;
    private SimpleDataPanelIcon nonmatchingRegionColorIcon;   
    private SimpleDataPanelIcon selectedIcon=null;
    
    private Color[] filteredMotifColors=new Color[]{LIGHTER_GRAY,LIGHTER_GRAY,LIGHTER_GRAY,LIGHTER_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY,VERY_LIGHT_GRAY};
    private String targetTrackName="";
    
    private static final int SCORE=0;
    private static final int MIN=1;
    private static final int MAX=2;
    private static final int AVERAGE=3;
    private static final int SUM=4;
    private static final int MEDIAN=5;
    private static final int START=6;
    private static final int END=7;
    private static final int UPSTREAM=8;
    private static final int DOWNSTREAM=9;
    private static final int CENTER=10;   
    private static final int LENGTH=11;
    
    private static final int HIDE_NON_MATCHING=0;
    private static final int GRAY_OUT_NON_MATCHING=1;
    private static final int GREEN_AND_RED=2;
    
    private int colorSetting=HIDE_NON_MATCHING;
    
    
    
    /** Creates new form MotifScoreFilterDialog */
    public MotifScoreFilterDialog(MotifLabGUI gui) {
        super(gui.getFrame(),false);
        this.gui=gui;
        settings=gui.getVisualizationSettings();
        settings.addVisualizationSettingsListener(this);
        initComponents();     
        VERY_LIGHT_GRAY=(Color)gui.getVisualizationSettings().getSettingAsType("system.filter.lightGray", VERY_LIGHT_GRAY);
        LIGHTER_GRAY=(Color)gui.getVisualizationSettings().getSettingAsType("system.filter.gray", LIGHTER_GRAY);
        COLOR_GREEN=(Color)gui.getVisualizationSettings().getSettingAsType("system.filter.green", COLOR_GREEN);
        COLOR_RED=(Color)gui.getVisualizationSettings().getSettingAsType("system.filter.red", COLOR_RED);        
        popup=new ContextMenu();
        operatorButton.setIcon(greaterThanIcon);
        settingsButton.setIcon(new MiscIcons(MiscIcons.DOWN_TRIANGLE));
        settingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                popup.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        });        
        Dimension size=new Dimension(380,47);
        //getRootPane().setPreferredSize(size);
        //getRootPane().setMaximumSize(new Dimension(500,47));
        scoreSlider.setMinimum(0);
        scoreSlider.setMaximum(100);
        scoreSlider.setValue(0);    
        scoreSlider.addChangeListener(this);
        initRange();
        thresholdLabel.setText(String.format("%6.3f",currentScoreThreshold));        
        gui.addRegionVisualizationFilter(this);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                if (!isForciblyClosed) closeButton.doClick();
            }

        });
        ArrayList<String> regiontracks=gui.getEngine().getNamesForAllDataItemsOfType(RegionDataset.class);
        String[] targettracks=new String[regiontracks.size()];
        for (int i=0;i<targettracks.length;i++) {
            targettracks[i]=regiontracks.get(i);
        }
        DefaultComboBoxModel model=new DefaultComboBoxModel(targettracks);
        targetTrackCombobox.setModel(model);        
        
        ArrayList<String> numerictracks=gui.getEngine().getNamesForAllDataItemsOfType(NumericDataset.class);
        String[] tracks=new String[numerictracks.size()];
        for (int i=0;i<tracks.length;i++) {
            tracks[i]=numerictracks.get(i);
        }
        DefaultComboBoxModel numerictracksmodel=new DefaultComboBoxModel(tracks);
        trackCombobox.setModel(numerictracksmodel);
        propertyCombobox.setSelectedItem("Score");
        pack();

        String newTrack=(String)targetTrackCombobox.getSelectedItem();
        if (newTrack!=null) targetTrackName=newTrack;
        else targetTrackName="";    
        updateRange();

        matchingRegionColorIcon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER_INSIDE, null);
        matchingRegionColorIcon.setForegroundColor(COLOR_GREEN);        
        matchingRegionColorLabel.setIcon(matchingRegionColorIcon);
        
        nonmatchingRegionColorIcon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER_INSIDE, null);
        nonmatchingRegionColorIcon.setForegroundColor(COLOR_RED);              
        nonmatchingRegionColorLabel.setIcon(nonmatchingRegionColorIcon);
        
        final ColorMenuListener selectcolorlistener=new ColorMenuListener() {
            @Override
            public void newColorSelected(Color color) {
                if (color!=null){
                    selectedIcon.setForegroundColor(color);
                    if (selectedIcon==matchingRegionColorIcon) {COLOR_GREEN=color;settings.storePersistentSetting("system.filter.green", color);}
                    else if (selectedIcon==nonmatchingRegionColorIcon) {COLOR_RED=color;settings.storePersistentSetting("system.filter.red", color);}
                    settings.redraw();                    
                }                   
            }
        };        
        ColorMenu temp=new ColorMenu("Select Color", selectcolorlistener, MotifScoreFilterDialog.this);
        selectColorMenu=temp.wrapInPopup();
        MouseAdapter updateColorsListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedIcon=(SimpleDataPanelIcon)((JLabel)e.getSource()).getIcon();
                selectColorMenu.show((Component)e.getSource(), e.getX(), e.getY());
            }         
        };        
        matchingRegionColorLabel.addMouseListener(updateColorsListener);
        nonmatchingRegionColorLabel.addMouseListener(updateColorsListener);               
    }

      /** The previous version filtered all visible motif tracks, but the new version (below) only filters a single selected region tracks */
//    private void initRange() {
//        double max=-Double.MAX_VALUE;
//        double min=Double.MAX_VALUE;        
//        ArrayList<Data> regionTracks=gui.getEngine().getAllDataItemsOfType(RegionDataset.class);
//        for (Data data:regionTracks) {
//            if (!((RegionDataset)data).isMotifTrack()) continue;
//            if (!gui.getVisualizationSettings().isVisible(data.getName())) continue;
//            for (FeatureSequenceData seq:((RegionDataset)data).getAllSequences()) {
//                if (!gui.getVisualizationSettings().isSequenceVisible(seq.getName())) continue;
//                for (Region region:((RegionSequenceData)seq).getAllRegions()) {
//                    Double score=getRegionValue(region);
//                    if (score==null || score.isNaN()) continue;
//                    if (score<min) min=score;
//                    if (score>max) max=score;
//                }
//            }          
//        }
//        maxvalue=max;  
//        minvalue=min;  
//        if (max==-Double.MAX_VALUE && min==Double.MAX_VALUE) {maxvalue=0;minvalue=0;}
//        scoreSlider.setEnabled(maxvalue>=minvalue);
//    }
        
    
    private void initRange() {
        double max=-Double.MAX_VALUE;
        double min=Double.MAX_VALUE;    
        String trackName=(String)targetTrackCombobox.getSelectedItem();
        Data data=gui.getEngine().getDataItem(trackName);
        if (data instanceof RegionDataset) {
            for (FeatureSequenceData seq:((RegionDataset)data).getAllSequences()) {
                if (!gui.getVisualizationSettings().isSequenceVisible(seq.getName())) continue;
                for (Region region:((RegionSequenceData)seq).getAllRegions()) {
                    Double score=getRegionValue(region);
                    if (score==null || score.isNaN()) continue;
                    if (score<min) min=score;
                    if (score>max) max=score;
                }
            }      
        }
        maxvalue=max;  
        minvalue=min;  
        if (max==-Double.MAX_VALUE && min==Double.MAX_VALUE) {maxvalue=0;minvalue=0;}
        scoreSlider.setEnabled(maxvalue>=minvalue);
    }       
    
    @Override
    public void stateChanged(ChangeEvent e) {
       int percentage=scoreSlider.getValue();       
       currentScoreThreshold=(maxvalue-minvalue)*(percentage/100.0)+minvalue;
       thresholdLabel.setText(String.format("%6.3f",currentScoreThreshold));
       gui.redraw();
    }

    private boolean isRegionMatching(Region region) {
        if (property==SCORE) return (greaterThan)?(region.getScore()>=currentScoreThreshold):(region.getScore()<=currentScoreThreshold);
        else if (property==LENGTH) return (greaterThan)?(region.getLength()>=currentScoreThreshold):(region.getLength()<=currentScoreThreshold);
        else {
              RegionSequenceData seq=region.getParent();
              if (seq==null || numericTrack==null) return false;
              Double score=getRegionValue(region);
              if (score==null || score.isNaN()) return false;
              return (greaterThan)?(score>=currentScoreThreshold):(score<=currentScoreThreshold);     
        }        
    }
    
    @Override 
    public String getRegionVisualizationFilterName() {
        return "Score Filter";
    }    
    
    @Override
    public boolean shouldVisualizeRegion(Region region) {
        // if (!region.getParent().isMotifTrack()) return true; // only filter motif tracks
        // if (!region.getTrackName().equals(targetTrackName)) return true; // only filter target track
        if (colorSetting!=HIDE_NON_MATCHING) return true; // do not hide filtered regions, but use different colors instead
        return isRegionMatching(region);
    }
    
    private Double getRegionValue(Region region) {
        if (property==SCORE) return region.getScore();
        else if (property==LENGTH) return (double)region.getLength();
        else {
              RegionSequenceData seq=region.getParent();
              if (seq==null || numericTrack==null) return Double.NaN;
              NumericSequenceData numericSequence=(NumericSequenceData)numericTrack.getSequenceByName(seq.getName()); // 
                   if (property==START) return numericSequence.getValueAtRelativePosition(region.getRelativeStart());
              else if (property==END) return numericSequence.getValueAtRelativePosition(region.getRelativeEnd());
              else if (property==UPSTREAM) return numericSequence.getValueAtRelativePosition((numericSequence.getStrandOrientation()==Sequence.DIRECT)?region.getRelativeStart():region.getRelativeEnd());
              else if (property==DOWNSTREAM) return numericSequence.getValueAtRelativePosition((numericSequence.getStrandOrientation()==Sequence.DIRECT)?region.getRelativeEnd():region.getRelativeStart());
              else if (property==CENTER) return numericSequence.getValueAtRelativePosition((int)((region.getRelativeStart()+region.getRelativeEnd())/2.0));
              else if (property==AVERAGE) return numericSequence.getAverageValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (property==MIN) return numericSequence.getMinValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (property==MAX) return numericSequence.getMaxValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (property==SUM) return numericSequence.getSumValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (property==MEDIAN) return numericSequence.getMedianValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else return Double.NaN;   
        }
    }    
   
    @Override
    public java.awt.Color getDynamicRegionColor(Region region) {
        // if (!region.getTrackName().equals(targetTrackName)) return null; // use filter only on selected track
        if (colorSetting==HIDE_NON_MATCHING) return null;          
        if (colorSetting==GREEN_AND_RED) {
          return (isRegionMatching(region))?COLOR_GREEN:COLOR_RED;
        } else if (colorSetting==GRAY_OUT_NON_MATCHING) {
          return (isRegionMatching(region))?null:VERY_LIGHT_GRAY;
        } else return null;
    }    
    
    @Override
    public java.awt.Color getDynamicRegionLabelColor(Region region) {
        // if (!region.getTrackName().equals(targetTrackName)) return null; // use filter only on selected track
        if (colorSetting==HIDE_NON_MATCHING || colorSetting==GREEN_AND_RED) return null;       
        if (colorSetting==GRAY_OUT_NON_MATCHING) {
          return (isRegionMatching(region))?null:Color.WHITE;
        } else return null; 
    }   
    
    @Override
    public java.awt.Color getDynamicRegionBorderColor(Region region) {
        // if (!region.getTrackName().equals(targetTrackName)) return null; // use filter only on selected track
        if (colorSetting==HIDE_NON_MATCHING || colorSetting==GREEN_AND_RED) return null;       
        if (colorSetting==GRAY_OUT_NON_MATCHING) {
          boolean drawborder=(settings.drawRegionBorders(region.getTrackName())>0);
          return (isRegionMatching(region))?null:((drawborder)?Color.LIGHT_GRAY:null);
        } else return null; 
    }      
    
    @Override
    public java.awt.Color[] getDynamicMotifLogoColors(Region region) {
        // if (!region.getTrackName().equals(targetTrackName)) return null; // use filter only on selected track
        if (colorSetting==HIDE_NON_MATCHING || colorSetting==GREEN_AND_RED) return null;       
        if (colorSetting==GRAY_OUT_NON_MATCHING) {
          return (isRegionMatching(region))?null:filteredMotifColors;
        } else return null; 
    }

    @Override
    public boolean appliesToTrack(String featureName) {
        if (targetTrackName==null || targetTrackName.isEmpty()) return false;
        else return targetTrackName.equals(featureName);
    }

    @Override
    public void drawOverlay(Region region, Graphics2D g, Rectangle rect, VisualizationSettings settings, DataTrackVisualizer visualizer) {

    }

    @Override
    public boolean drawsOverlay() {
        return false;
    }

    public int getPriority() { // filter priority
        return FILTER_PRIORITY_HIGH;
    }

    @Override
    public String[] getTargets() {
        if (targetTrackName==null || targetTrackName.isEmpty()) return new String[0];
        else return new String[]{targetTrackName};
    }


    @Override
    public void sequenceRepainted(String sequenceName, String featureName) {

    }    
    
    @Override
    public void shutdown() {
        isForciblyClosed=true;
        gui.getVisualizationSettings().removeVisualizationSettingsListener(this);        
        gui.redraw();
        setVisible(false);
        this.dispose();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        targetTrackCombobox = new javax.swing.JComboBox();
        jPanel9 = new javax.swing.JPanel();
        matchingRegionColorLabel = new javax.swing.JLabel();
        nonmatchingRegionColorLabel = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        thresholdLabel = new javax.swing.JLabel();
        scoreSlider = new javax.swing.JSlider();
        jPanel6 = new javax.swing.JPanel();
        operatorButton = new javax.swing.JButton();
        settingsButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        propertyCombobox = new javax.swing.JComboBox();
        trackCombobox = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(MotifScoreFilterDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(350, 133));
        setName("Form"); // NOI18N
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel8.setName("jPanel8"); // NOI18N
        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        targetTrackCombobox.setName("targetTrackCombobox"); // NOI18N
        targetTrackCombobox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                newTargetTrackSelected(evt);
            }
        });
        targetTrackCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newTrackSelected(evt);
            }
        });
        jPanel8.add(targetTrackCombobox);

        jPanel7.add(jPanel8, java.awt.BorderLayout.CENTER);

        jPanel9.setName("jPanel9"); // NOI18N
        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 10));

        matchingRegionColorLabel.setText(resourceMap.getString("matchingRegionColorLabel.text")); // NOI18N
        matchingRegionColorLabel.setToolTipText(resourceMap.getString("matchingRegionColorLabel.toolTipText")); // NOI18N
        matchingRegionColorLabel.setName("matchingRegionColorLabel"); // NOI18N
        jPanel9.add(matchingRegionColorLabel);

        nonmatchingRegionColorLabel.setText(resourceMap.getString("nonmatchingRegionColorLabel.text")); // NOI18N
        nonmatchingRegionColorLabel.setToolTipText(resourceMap.getString("nonmatchingRegionColorLabel.toolTipText")); // NOI18N
        nonmatchingRegionColorLabel.setName("nonmatchingRegionColorLabel"); // NOI18N
        jPanel9.add(nonmatchingRegionColorLabel);

        jPanel7.add(jPanel9, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel7);

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 8))));
        jPanel1.setMinimumSize(new java.awt.Dimension(290, 40));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 40));
        jPanel1.setLayout(new java.awt.BorderLayout());

        thresholdLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        thresholdLabel.setText(resourceMap.getString("thresholdLabel.text")); // NOI18N
        thresholdLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 10));
        thresholdLabel.setFocusable(false);
        thresholdLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        thresholdLabel.setMaximumSize(new java.awt.Dimension(70, 33));
        thresholdLabel.setMinimumSize(new java.awt.Dimension(70, 33));
        thresholdLabel.setName("thresholdLabel"); // NOI18N
        thresholdLabel.setPreferredSize(new java.awt.Dimension(70, 33));
        jPanel1.add(thresholdLabel, java.awt.BorderLayout.WEST);

        scoreSlider.setAlignmentY(0.0F);
        scoreSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        scoreSlider.setMaximumSize(new java.awt.Dimension(32767, 33));
        scoreSlider.setMinimumSize(new java.awt.Dimension(200, 33));
        scoreSlider.setName("scoreSlider"); // NOI18N
        scoreSlider.setPreferredSize(new java.awt.Dimension(200, 33));
        jPanel1.add(scoreSlider, java.awt.BorderLayout.CENTER);

        jPanel5.add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel6.setName("jPanel6"); // NOI18N

        operatorButton.setText(resourceMap.getString("operatorButton.text")); // NOI18N
        operatorButton.setToolTipText(resourceMap.getString("operatorButton.toolTipText")); // NOI18N
        operatorButton.setMaximumSize(new java.awt.Dimension(32, 32));
        operatorButton.setMinimumSize(new java.awt.Dimension(32, 32));
        operatorButton.setName("operatorButton"); // NOI18N
        operatorButton.setPreferredSize(new java.awt.Dimension(32, 32));
        operatorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                operatorButtonPressed(evt);
            }
        });
        jPanel6.add(operatorButton);

        settingsButton.setText(resourceMap.getString("settingsButton.text")); // NOI18N
        settingsButton.setMaximumSize(new java.awt.Dimension(32, 32));
        settingsButton.setMinimumSize(new java.awt.Dimension(32, 32));
        settingsButton.setName("settingsButton"); // NOI18N
        settingsButton.setPreferredSize(new java.awt.Dimension(32, 32));
        jPanel6.add(settingsButton);

        jPanel5.add(jPanel6, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel5);

        jPanel2.setMinimumSize(new java.awt.Dimension(179, 33));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        propertyCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Score", "Length", "Minimum value", "Maximum value", "Average value", "Median value", "Sum value", "Start value", "Center value", "End value", "Relative start value", "Relative end value" }));
        propertyCombobox.setName("propertyCombobox"); // NOI18N
        propertyCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                propertySelected(evt);
            }
        });
        jPanel3.add(propertyCombobox);

        trackCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        trackCombobox.setName("trackCombobox"); // NOI18N
        trackCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackSelectionUpdated(evt);
            }
        });
        jPanel3.add(trackCombobox);

        jPanel2.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel4.setName("jPanel4"); // NOI18N

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonClicked(evt);
            }
        });
        jPanel4.add(closeButton);

        jPanel2.add(jPanel4, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel2);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void closeButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonClicked
    gui.getVisualizationSettings().removeVisualizationSettingsListener(this);
    gui.removeRegionVisualizationFilter(this);
    gui.redraw();
    setVisible(false);
    this.dispose();
}//GEN-LAST:event_closeButtonClicked

private void propertySelected(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_propertySelected
    String propertyString=(String)propertyCombobox.getSelectedItem();
         if (propertyString.equals("Score")) property=SCORE;
    else if (propertyString.equals("Length")) property=LENGTH;
    else if (propertyString.equals("Minimum value")) property=MIN;
    else if (propertyString.equals("Maximum value")) property=MAX;
    else if (propertyString.equals("Average value")) property=AVERAGE;
    else if (propertyString.equals("Median value")) property=MEDIAN;
    else if (propertyString.equals("Sum value")) property=SUM;
    else if (propertyString.equals("Start value")) property=START;
    else if (propertyString.equals("Center value")) property=CENTER;
    else if (propertyString.equals("End value")) property=END;
    else if (propertyString.equals("Relative start value")) property=UPSTREAM;
    else if (propertyString.equals("Relative end value ")) property=DOWNSTREAM;
    if (property!=SCORE && property!=LENGTH && numericTrack==null) {
        String trackName=(String)trackCombobox.getSelectedItem();
        Data data=gui.getEngine().getDataItem(trackName);
        if (data!=null && data instanceof NumericDataset) numericTrack=(NumericDataset)data;
        else numericTrack=null;        
    }
    initRange(); 
    scoreSlider.setValue(0);    
    trackCombobox.setVisible(property!=SCORE && property!=LENGTH);
    pack();
}//GEN-LAST:event_propertySelected

private void trackSelectionUpdated(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trackSelectionUpdated
    String trackName=(String)trackCombobox.getSelectedItem();
    Data data=gui.getEngine().getDataItem(trackName);
    if (data!=null && data instanceof NumericDataset) numericTrack=(NumericDataset)data;
    else numericTrack=null;
    updateRange(); 
}//GEN-LAST:event_trackSelectionUpdated

    private void operatorButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_operatorButtonPressed
        greaterThan=!greaterThan;        
        if (greaterThan) operatorButton.setIcon(greaterThanIcon);
        else operatorButton.setIcon(smallerThanIcon);
        gui.redraw();
    }//GEN-LAST:event_operatorButtonPressed

    private void newTrackSelected(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newTrackSelected
        String newTrack=(String)targetTrackCombobox.getSelectedItem();
        if (newTrack!=null) targetTrackName=newTrack;
        else targetTrackName="";
        updateRange();
        gui.redraw();
    }//GEN-LAST:event_newTrackSelected

    private void newTargetTrackSelected(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_newTargetTrackSelected
        String newTrack=(String)targetTrackCombobox.getSelectedItem();
        if (newTrack!=null) targetTrackName=newTrack;
        else targetTrackName="";
        updateRange();        
        gui.redraw();
    }//GEN-LAST:event_newTargetTrackSelected

    @Override
    public void dataAdded(Data data) {
        if (data instanceof RegionDataset) {
            updateRange();
        }
    }
    @Override
    public void dataRemoved(Data data) {
        if (data instanceof RegionDataset) {
            updateRange();
        }    
    }
    
    @Override
    public void dataUpdated(Data data) {
       if (data instanceof RegionDataset || data.getName().equals(numericTrack.getName()) || data==numericTrack) {
        updateRange();
       }  
    }

    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {}
    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) {}
    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {}

    @Override
    public void dataRenamedEvent(String oldname, String newname) { }

    @Override
    public void sequenceMarginSizeChangedEvent(int newsize) {}

    @Override
    public void sequenceWindowSizeChangedEvent(int newsize, int oldsize) {}

    @Override
    public void sequencesLayoutEvent(int type, Data affected) {
        updateRange();
    }

    @Override
    public void trackReorderEvent(int type, Data affected) {
        updateRange();
    }

    @Override
    public void sequencesReordered(Integer oldposition, Integer newposition) {}


    private void updateRange() {
        initRange();
        scoreSlider.setValue(0);       
    }




    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JLabel matchingRegionColorLabel;
    private javax.swing.JLabel nonmatchingRegionColorLabel;
    private javax.swing.JButton operatorButton;
    private javax.swing.JComboBox propertyCombobox;
    private javax.swing.JSlider scoreSlider;
    private javax.swing.JButton settingsButton;
    private javax.swing.JComboBox targetTrackCombobox;
    private javax.swing.JLabel thresholdLabel;
    private javax.swing.JComboBox trackCombobox;
    // End of variables declaration//GEN-END:variables


    private class ContextMenu extends JPopupMenu implements ActionListener {
        private JCheckBoxMenuItem item1;
        private JCheckBoxMenuItem item2;
        private JCheckBoxMenuItem item3;
                
        public ContextMenu() {
           item1=new JCheckBoxMenuItem("Hide non-matching");
           item2=new JCheckBoxMenuItem("Gray out non-matching");
           item3=new JCheckBoxMenuItem("Use selected colors");
           ButtonGroup group=new ButtonGroup();
           group.add(item1);
           group.add(item2);
           group.add(item3);
           item1.addActionListener(this);
           item2.addActionListener(this);
           item3.addActionListener(this);
           add(item1);
           add(item2);
           add(item3);
           item1.doClick();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd=e.getActionCommand();
            if (cmd.equals(item1.getActionCommand())) {
                colorSetting=HIDE_NON_MATCHING;
            } else if (cmd.equals(item2.getActionCommand())) {
                colorSetting=GRAY_OUT_NON_MATCHING;
            } else if (cmd.equals(item3.getActionCommand())) {
                colorSetting=GREEN_AND_RED;
            }
            gui.redraw();
        }            
    }

}
