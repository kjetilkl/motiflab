/*
 * RegionBrowser.java
 *
 * Created on 15. september 2015, 13:09
 */

package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Sequence;

/**
 *
 * @author  kjetikl
 */
public class RegionBrowser extends javax.swing.JDialog implements WindowListener, ListSelectionListener, RowSorterListener, RedrawListener {
    public static final String VISUALIZE_ALL="";
    public static final String VISUALIZE_SHOWN="only matching regions"; // visualize only regions matching current search filter
    public static final String VISUALIZE_SELECTED="only selected regions"; // visualize only regions that are currently highlighted in the table (rows have dark blue background).
    public static final String VISUALIZE_CHOSEN="only manually chosen regions"; // use the SPACE key or S/H/O (with SHIFT) to select/deselect regions to show from the table. Selected regions will have a black border around the color icon, whereas unselected will have a gray border.

    private RegionBrowserPanel regionbrowser;
    private JTable table;
    private VisualizationSettings settings;
    private JComboBox regionTrackCombobox;
    private JToggleButton hideSequencesToggleButton;
    private JToggleButton hideOtherRegionsToggleButton;
    private MotifLabGUI gui;
    private MotifLabEngine engine;

    private boolean hideOtherSequences=false;
    private boolean hideOtherRegions=false;
    private String[] sequenceNames;
    private RegionBrowserFilter regionBrowserFilter;

    private ImageIcon hideRegionsIcon;
    private ImageIcon grayRegionIcon;
    private ImageIcon showSequencesIcon;
    private ImageIcon hideSequencesIcon;

    /** Creates new form RegionBrowser */
    public RegionBrowser(MotifLabGUI gui, RegionDataset usetrack) {
        super(gui.getFrame(), false);
        this.gui=gui;
        this.settings=gui.getVisualizationSettings();
        engine=gui.getEngine();
        initComponents();
        hideRegionsIcon=gui.getIcon("regions_hidden.png");
        grayRegionIcon=gui.getIcon("regions_gray.png");
        showSequencesIcon=gui.getIcon("sequencebrowser.png");
        hideSequencesIcon=gui.getIcon("sequences_hidden.png");
        Dimension buttonSize=new Dimension(hideRegionsIcon.getIconWidth(),hideRegionsIcon.getIconHeight());
        hideSequencesToggleButton=new JToggleButton(showSequencesIcon, hideOtherSequences);
        hideSequencesToggleButton.setRolloverIcon(showSequencesIcon);
        hideSequencesToggleButton.setSelectedIcon(hideSequencesIcon);
        hideSequencesToggleButton.setPressedIcon(hideSequencesIcon);
        hideSequencesToggleButton.setBorderPainted(false);
        hideSequencesToggleButton.setContentAreaFilled(false);
        hideSequencesToggleButton.setToolTipText((hideOtherSequences)?"Hide Other Sequences":"Show All Sequences");
        hideSequencesToggleButton.setPreferredSize(buttonSize);
        hideSequencesToggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideOtherSequences=hideSequencesToggleButton.isSelected();
                hideSequencesToggleButton.setToolTipText((hideOtherSequences)?"Hide Other Sequences":"Show All Sequences");
                hideSequencesToggleButton.setRolloverIcon((hideOtherSequences)?hideSequencesIcon:showSequencesIcon);
                if (hideOtherSequences) updateVisualization();
                else settings.setSequenceVisible(sequenceNames, true); // show all sequences if unselected
                RegionBrowser.this.gui.redraw();
            }
        });
        hideOtherRegionsToggleButton=new JToggleButton(grayRegionIcon, hideOtherSequences);
        hideOtherRegionsToggleButton.setRolloverIcon(grayRegionIcon);
        hideOtherRegionsToggleButton.setSelectedIcon(hideRegionsIcon);
        hideOtherRegionsToggleButton.setPressedIcon(hideRegionsIcon);
        hideOtherRegionsToggleButton.setBorderPainted(false);
        hideOtherRegionsToggleButton.setContentAreaFilled(false);
        hideOtherRegionsToggleButton.setToolTipText((hideOtherRegions)?"Hide Other Regions":"Gray Out Other Regions");
        hideOtherRegionsToggleButton.setPreferredSize(buttonSize);
        hideOtherRegionsToggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideOtherRegions=hideOtherRegionsToggleButton.isSelected();
                hideOtherRegionsToggleButton.setToolTipText((hideOtherRegions)?"Hide Other Regions":"Gray Out Other Regions");
                hideOtherRegionsToggleButton.setRolloverIcon((hideOtherRegions)?hideRegionsIcon:grayRegionIcon);
                regionBrowserFilter.setHideOtherRegions(hideOtherRegions);
                RegionBrowser.this.gui.redraw();
            }
        });
        bottomControlPanel.add(new JLabel("   "));
        bottomControlPanel.add(hideSequencesToggleButton);
        bottomControlPanel.add(new JLabel(" "));
        bottomControlPanel.add(hideOtherRegionsToggleButton);
        ArrayList<String> seqNames=engine.getNamesForAllDataItemsOfType(Sequence.class);
        sequenceNames=new String[seqNames.size()];
        sequenceNames=seqNames.toArray(sequenceNames);
        regionbrowser=new RegionBrowserPanel(gui,false,true);
        ArrayList<String> trackNames=gui.getEngine().getNamesForAllDataItemsOfType(RegionDataset.class);
        Collections.sort(trackNames,MotifLabEngine.getNaturalSortOrderComparator(true));
        regionTrackCombobox=new JComboBox(trackNames.toArray());
        regionTrackCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selected=regionTrackCombobox.getSelectedItem();
                if (selected instanceof String) {
                    RegionDataset dataset=(RegionDataset)engine.getDataItem((String)selected,RegionDataset.class);
                    regionbrowser.setRegionTrack(dataset);
                    table=regionbrowser.getTable();
                    table.getRowSorter().addRowSorterListener(RegionBrowser.this);
                    table.getSelectionModel().addListSelectionListener(RegionBrowser.this);
                    updateVisualization();
                }
            }
        });

        Border emptyBorder=BorderFactory.createEmptyBorder(10,6,0,6);
        Dimension size=new Dimension(580,500);
        regionbrowser.setPreferredSize(size);
        regionbrowser.setBorder(BorderFactory.createCompoundBorder(emptyBorder, BorderFactory.createRaisedBevelBorder()));
        this.add(regionbrowser);
        JPanel trackSelectionPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        trackSelectionPanel.add(new JLabel("Region track "));
        trackSelectionPanel.add(regionTrackCombobox);
        this.add(trackSelectionPanel,BorderLayout.NORTH);
        pack();
        getRootPane().setDefaultButton(closeButton);
        visualizefilterCombobox.setModel(new DefaultComboBoxModel(new String[]{VISUALIZE_ALL,VISUALIZE_SHOWN,VISUALIZE_SELECTED,VISUALIZE_CHOSEN}));
        regionBrowserFilter=regionbrowser.getRegionBrowserFilter();
        regionBrowserFilter.setHideOtherRegions(hideOtherRegions);
        regionBrowserFilter.addListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateVisualization();
            }
        });
        gui.addRegionVisualizationFilter(regionBrowserFilter);
        this.addWindowListener(this);
        if (usetrack!=null) {
           String trackName=usetrack.getName();
           if (trackNames.contains(trackName)) regionTrackCombobox.setSelectedItem(trackName);
        } else if (!trackNames.isEmpty()) regionTrackCombobox.setSelectedIndex(0);
    }

    @Override
    public void windowActivated(WindowEvent e) { }

    @Override
    public void windowClosed(WindowEvent e) {
        gui.removeRegionVisualizationFilter(regionBrowserFilter);
        gui.redraw();
    }

    @Override
    public void windowClosing(WindowEvent e) { }

    @Override
    public void windowDeactivated(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) { }

    @Override
    public void windowOpened(WindowEvent e) {}


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        bottomControlPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        visualizefilterCombobox = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(RegionBrowser.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        jPanel1.setMinimumSize(new java.awt.Dimension(100, 40));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 40));
        jPanel1.setLayout(new java.awt.BorderLayout());

        bottomControlPanel.setName("bottomControlPanel"); // NOI18N
        bottomControlPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        bottomControlPanel.add(jLabel1);

        visualizefilterCombobox.setName("visualizefilterCombobox"); // NOI18N
        visualizefilterCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                visualizefilterchanged(evt);
            }
        });
        bottomControlPanel.add(visualizefilterCombobox);

        jPanel1.add(bottomControlPanel, java.awt.BorderLayout.CENTER);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setMaximumSize(new java.awt.Dimension(75, 27));
        closeButton.setMinimumSize(new java.awt.Dimension(75, 27));
        closeButton.setName("closeButton"); // NOI18N
        closeButton.setOpaque(false);
        closeButton.setPreferredSize(new java.awt.Dimension(75, 27));
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonClicked(evt);
            }
        });
        jPanel3.add(closeButton);

        jPanel1.add(jPanel3, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);

        getAccessibleContext().setAccessibleName(resourceMap.getString("Form.AccessibleContext.accessibleName")); // NOI18N

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void closeButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonClicked
// TODO add your handling code here:

    setVisible(false);//GEN-LAST:event_closeButtonClicked
    getContentPane().removeAll(); // this will release some resources
    this.dispose();
}


private void visualizefilterchanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_visualizefilterchanged
       updateVisualization();
       String selection=(String)visualizefilterCombobox.getSelectedItem();//GEN-LAST:event_visualizefilterchanged
}

    @Override
    public void valueChanged(ListSelectionEvent e) {
       updateVisualization();
    }

    @Override
    public void sorterChanged(RowSorterEvent e) {
        table.clearSelection();
        updateVisualization();
    }



    @Override
    public void redrawEvent() {
        repaint();
    }

    private void updateSequenceVisibility() {
        if (regionBrowserFilter.getTargetRegions().isEmpty()) settings.setSequenceVisible(sequenceNames, true);
        else {
            settings.setSequenceVisible(sequenceNames, false);
            int size=regionBrowserFilter.getTargetRegions().size();
            int i=0;
            for (Region region:regionBrowserFilter.getTargetRegions()) {
                i++;
                String sequenceName=region.getParent().getSequenceName();
                settings.setSequenceVisible(sequenceName, true, (i==size)?true:false);
            }
            settings.redraw();
        }
    }

    /** Hides or shows regions according to specification */
    public void updateVisualization() {
         String selection=(String)visualizefilterCombobox.getSelectedItem();
         if (selection.equalsIgnoreCase(RegionBrowser.VISUALIZE_SHOWN)) {
             regionBrowserFilter.clearAll();
             regionbrowser.setManualFilterSelectionModeEnabled(false);
             for (Region region:regionbrowser.getRegionsShownInTable()) {
                 regionBrowserFilter.addRegion(region);
             }
             if (hideOtherSequences) updateSequenceVisibility();
             regionBrowserFilter.enable();
         } else if (selection.equalsIgnoreCase(RegionBrowser.VISUALIZE_SELECTED)) {
             regionbrowser.setManualFilterSelectionModeEnabled(false);
             regionBrowserFilter.clearAll();
             for (Region region:regionbrowser.getSelectedRegions()) {
                 regionBrowserFilter.addRegion(region);
             }
             if (hideOtherSequences) updateSequenceVisibility();
             regionBrowserFilter.enable();
         } else if (selection.equalsIgnoreCase(RegionBrowser.VISUALIZE_CHOSEN)) { // this is called every time a selection is made, but this might not necessarily update the selection
             regionbrowser.setManualFilterSelectionModeEnabled(true);
             if (hideOtherSequences) updateSequenceVisibility();
             regionBrowserFilter.enable();
         } else {
            regionbrowser.setManualFilterSelectionModeEnabled(false);
            regionBrowserFilter.clearAll();
            regionBrowserFilter.disable();
         }
         gui.redraw();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottomControlPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JComboBox visualizefilterCombobox;
    // End of variables declaration//GEN-END:variables

}
