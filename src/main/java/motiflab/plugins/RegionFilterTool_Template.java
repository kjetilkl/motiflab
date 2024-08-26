package motiflab.plugins;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabClient;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Plugin;
import motiflab.engine.SystemError;
import motiflab.engine.data.Data;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;
import motiflab.gui.DataTrackVisualizer;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.RegionVisualizationFilter;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public abstract class RegionFilterTool_Template implements Plugin, RegionVisualizationFilter {
  
    private FilterDialog dialog = null;     
    private MotifLabGUI gui = null;
    private MotifLabEngine engine = null;
    private RegionDataset targetDataset = null;
    
    @Override
    public void initializePlugin(MotifLabEngine engine) throws ExecutionError, SystemError {
       this.engine = engine;
    }
    
    @Override
    public void initializePluginFromClient(MotifLabClient client) throws ExecutionError, SystemError {
        try {
            if (client instanceof MotifLabGUI) {
                this.gui=(MotifLabGUI)client;             
                JMenuItem item=new JMenuItem(getPluginName());
                // ImageIcon icon=new ImageIcon(EnhancerHighlighter.class.getResource("ehicon.png"));                 
                // item.setIcon(icon);
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showDialog();
                    }
                });
                if (!gui.addToToolsMenu(item)) throw new ExecutionError("Unable to add '"+getPluginName()+"' to Tools menu");                                                 
            }
        }
        catch (Exception e) {throw new SystemError(e.toString());}
    }
    
    @Override
    public void uninstallPlugin(MotifLabEngine engine) {
       if (dialog!=null) {
           dialog.setVisible(false);
           dialog.dispose();
           dialog = null;
       }
       // remove the tool menu item from the Tools menu       
       if (gui instanceof MotifLabGUI) {
           gui.removeFromMenu("Tools", getPluginName());           
       }
       gui = null;
       engine = null;
    }    
                                 

    @Override
    public void shutdownPlugin() { // shut down the plugin 
        if (dialog!=null) {
           dialog.setVisible(false);
           dialog.dispose();
           dialog = null;
        }  
        gui.removeRegionVisualizationFilter(this);
    }

    @Override
    public void shutdown() { // shut down the filter      
        if (dialog!=null) {
           dialog.setVisible(false);
           dialog.dispose();
           dialog = null;
        }        
    }

    @Override
    public String getRegionVisualizationFilterName() {
        return getPluginName();
    }
       
    
    public abstract boolean hasSingleTarget();
    public abstract boolean isPersistant();
    public abstract int getFilterPriority();
    public abstract void targetChanged(RegionDataset target);    
    public abstract void setupDialog(JPanel panel);
    
    
    
    public RegionDataset getTargetDataset() {
        return targetDataset;
    }
    
    @Override
    public String[] getTargets() {
        if (hasSingleTarget()) {
            if (targetDataset!=null) return new String[]{targetDataset.getName()};
            else return new String[0]; // no target for the filter
        } else return null; // applies to all tracks
    }
    
    @Override
    public boolean appliesToTrack(String featureName) {
        if (hasSingleTarget()) {
            if (targetDataset!=null) return targetDataset.getName().equals(featureName);
            else return false;
        } else return true;       
    }

    @Override
    public void drawOverlay(Region region, Graphics2D g, Rectangle rect, VisualizationSettings settings, DataTrackVisualizer visualizer) {
    }

    @Override
    public boolean drawsOverlay() {
        return false;
    }

    @Override
    public int getPriority() {
        return getFilterPriority();
    }


    @Override
    public void sequenceRepainted(String sequenceName, String featureName) {

    }   
    
     
    private void showDialog() {
        if (dialog == null) dialog = new FilterDialog(gui);
        int height = dialog.getHeight();
        int width = dialog.getWidth();
        java.awt.Dimension size = gui.getGUIFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);   
    }    
    
     private class FilterDialog extends javax.swing.JDialog {
        private JPanel topPanel;
        private JPanel mainPanel;
        private JPanel bottomPanel;
        private JPanel controlsPanel;
        private JComboBox<String> tracksCombobox;
        private JButton closeButton;

        public FilterDialog(MotifLabGUI guiclient) {
            super(guiclient.getFrame(), getPluginName(), false);
            this.setResizable(false);
            initComponents();
            gui.addRegionVisualizationFilter(RegionFilterTool_Template.this);    
        }
        
        /** Returns ComboboxModels containing all NumericVariables and MotifNumericMaps as well as a default constant integer */
        private DefaultComboBoxModel getDataTracks() {
            ArrayList<String>candidateNames=new ArrayList<String>();
            for (Data item:engine.getAllDataItemsOfType(RegionDataset.class)) {
                candidateNames.add(item.getName());
            } 
            Collections.sort(candidateNames);
            String[] entries=new String[candidateNames.size()];
            entries=candidateNames.toArray(entries);
            DefaultComboBoxModel model=new DefaultComboBoxModel(entries);
            return model;       
        }



        private void closeButtonPressed(java.awt.event.ActionEvent evt) {                                        
            setVisible(false);
            if (!isPersistant()) {
                gui.removeRegionVisualizationFilter(RegionFilterTool_Template.this);
                gui.redraw();
                this.dispose();
            }
        }      

        @SuppressWarnings("unchecked")                   
        private void initComponents() {        
            setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            topPanel = new JPanel();     
            mainPanel = new JPanel();
            bottomPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());

            closeButton = new JButton("Close");
            closeButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    closeButtonPressed(evt);
                }
            });
            bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            bottomPanel.add(closeButton);
            mainPanel.add(bottomPanel,BorderLayout.SOUTH);
            
            if (hasSingleTarget()) {
                DefaultComboBoxModel model = getDataTracks();
                tracksCombobox = new JComboBox<>(model);
                tracksCombobox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String trackName = (String)tracksCombobox.getSelectedItem();
                        targetDataset = (RegionDataset)engine.getDataItem(trackName, RegionDataset.class);
                        targetChanged(targetDataset);
                    }                    
                });
                if (model.getSize()>0) tracksCombobox.setSelectedIndex(0);
                topPanel.add(tracksCombobox);
                mainPanel.add(topPanel,BorderLayout.NORTH);
            }

            controlsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
            setupDialog(controlsPanel);
            mainPanel.add(controlsPanel,BorderLayout.CENTER);            
            getContentPane().add(mainPanel);
            pack();
        }              
        
    }
    
}
