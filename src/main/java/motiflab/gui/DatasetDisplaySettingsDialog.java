/*
 * DatasetDisplaySettingsDialog.java
 *
 * Created on 11. november 2008, 13:42
 */
package motiflab.gui;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.event.ChangeEvent;
import motiflab.engine.data.Data;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.FeatureDataset;
import javax.swing.DefaultComboBoxModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.JPanel;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeListener;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.NumericDataset;

/**
 * This class implements a GUI dialog used to specify the display settings (colors, graph type, size etc.)
 * for the visualization of a single dataset (A dataset is displayed as a track for
 * each sequence, and each track from the same dataset has the same display settings
 * across all sequences.)
 * @author kjetikl
 */
public class DatasetDisplaySettingsDialog extends JDialog {

    VisualizationSettings settings = null;
    FeatureDataset dataset = null;
    SimpleDataPanelIcon icon1_region;
    SimpleDataPanelIcon icon2_region;
    MiscIcons gradientfillIcon;
    private static final JColorChooser customColorChooser = setupCustomColorChooser();

    
    /** Creates new form DatasetDisplaySettingsDialog */
    public DatasetDisplaySettingsDialog(java.awt.Frame parent, FeatureDataset dataset, VisualizationSettings settings) {
        super(parent, true); // initialize modal dialog
        this.dataset = dataset;
        this.settings = settings;
        icon1_region = new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.BLANK_ICON, SimpleDataPanelIcon.SIMPLE_BORDER,settings);
        icon2_region = new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.BLANK_ICON, SimpleDataPanelIcon.SIMPLE_BORDER,settings);
        gradientfillIcon=new MiscIcons(MiscIcons.FLAT_FILLED);
        gradientfillIcon.setSize(16,16);
        initComponents();
        gradientFillButton_region.setIcon(gradientfillIcon);
        // setTitle(getTitle()+" "+dataset.getName());
        datasetSelectionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectNewDataset((FeatureDataset) datasetSelectionComboBox.getSelectedItem());
            }
        });        
         if (dataset != null) {
            datasetSelectionComboBox.setSelectedItem(dataset);
        } else {
            datasetSelectionComboBox.setSelectedIndex(0);
        }       
    }

    /**
     * This function is called when the combobox is used to select a new target dataset
     * It should update the panels accordingly with correct information
     * @param o
     */
    private void selectNewDataset(FeatureDataset dataset) {
        String datasetname = dataset.getName();
        //settings.getGUI().debugMessage("Selected on " + dataset.getName());
        this.dataset = dataset;
        if (dataset instanceof RegionDataset) {
                ((CardLayout) mainPanel.getLayout()).show(mainPanel, "regionDataset");
                foregroundColorButton_region.setBackground(settings.getForeGroundColor(datasetname));
                backgroundColorButton_region.setBackground(settings.getBackGroundColor(datasetname));
                baselineColorButton_region.setBackground(settings.getBaselineColor(datasetname));                
                heightSelectionSpinner_region.getModel().setValue(new Integer(settings.getDataTrackHeight(datasetname)));
                expandedRegionHeightSpinner.getModel().setValue(new Integer(settings.getExpandedRegionHeight(datasetname)));
                Color foregroundcolor = settings.getForeGroundColor(datasetname);
                Color backgroundcolor = settings.getBackGroundColor(datasetname);
                icon1_region.setForegroundColor(foregroundcolor);
                icon1_region.setBackgroundColor(backgroundcolor);
                icon2_region.setForegroundColor(foregroundcolor);
                icon2_region.setBackgroundColor(backgroundcolor);
                icon1_region.setIconType(SimpleDataPanelIcon.REGION_ICON);
                icon2_region.setIconType(SimpleDataPanelIcon.REGION_MULTICOLOR_ICON);
                boolean visualizeStrand=settings.shouldVisualizeRegionStrand(datasetname);
                boolean visualizeScore=settings.shouldVisualizeRegionScore(datasetname);
                visualizeStrandCheckbox.setSelected(visualizeStrand);
                visualizeScoreCheckbox.setSelected(visualizeScore);
                int useGradientFill=settings.useGradientFill(datasetname);
                int gradientFillIcon=MiscIcons.FLAT_FILLED;
                if (useGradientFill==1) gradientFillIcon=MiscIcons.VERTICAL_GRADIENT_FILLED;
                else if (useGradientFill==2) gradientFillIcon=MiscIcons.HORIZONTAL_GRADIENT_FILLED;               
                gradientfillIcon.setIconType(gradientFillIcon);
                gradientfillIcon.setForegroundColor(settings.getForeGroundColor(datasetname));                
                boolean showexpanded=settings.isExpanded(datasetname);
                showExpandedCheckbox_region.setSelected(showexpanded);   
                boolean useMultiColoredRegions = settings.useMultiColoredRegions(datasetname);
                singlecolorRadioButton.setSelected(!useMultiColoredRegions);
                multicolorRadioButton.setSelected(useMultiColoredRegions);
                ArrayList<String> graphs = settings.getGUI().getRegisteredDataTrackVisualizers(RegionDataset.class);
                String[] graphTypes=new String[graphs.size()];
                graphTypes=graphs.toArray(graphTypes);              
                graphTypeCombobox_region.setModel(new DefaultComboBoxModel(graphTypes));
                String currentGraph=settings.getGraphType(datasetname);
                if (currentGraph!=null && !currentGraph.isEmpty()) graphTypeCombobox_region.setSelectedItem(currentGraph);                  
                 
                /*
                graphTypeCheckBox1_region.setText("Regions");
                icon2_region.setIconType(SimpleDataPanelIcon.REGION_GRADIENT_ICON);
                graphTypeCheckBox2_region.setText("Gradient regions");
                if (settings.getGraphType(datasetname) == VisualizationSettings.REGION_GRAPH) {
                    graphTypeCheckBox1_region.setSelected(true);
                } else if (settings.getGraphType(datasetname) == VisualizationSettings.GRADIENT_REGION_GRAPH) {
                    graphTypeCheckBox2_region.setSelected(true);
                } else { // defaulting
                    graphTypeCheckBox1_region.setSelected(true);
                }
                */
//                if (((RegionDataset)dataset).isModuleTrack()) {
//                    Object moduleoutline=settings.getModuleOutlineColor();
//                    if (moduleoutline instanceof Color) {
//                        moduleOutlineColorButton.setBackground((Color)moduleoutline);
//                        moduleOutlineColorComboBox.setSelectedItem("Chosen color");
//                    } else if (moduleoutline==null) {
//                        moduleOutlineColorComboBox.setSelectedItem("None");
//                        moduleOutlineColorButton.setBackground(Color.BLACK);
//                    } else {
//                        moduleOutlineColorComboBox.setSelectedItem("Color by type");
//                        moduleOutlineColorButton.setBackground(Color.BLACK);
//                    }
//                    Object modulefill=settings.getModuleFillColor();
//                    if (modulefill instanceof Color) {
//                        moduleFillColorButton.setBackground((Color)modulefill);
//                        moduleFillColorComboBox.setSelectedItem("Chosen color");
//                    } else if (modulefill==null) {
//                        moduleFillColorComboBox.setSelectedItem("None");
//                        moduleFillColorButton.setBackground(Color.BLACK);
//                    } else {
//                        moduleFillColorComboBox.setSelectedItem("Color by type");
//                        moduleFillColorButton.setBackground(Color.BLACK);
//                    }
//                    moduleSettingsPanel.setVisible(true);
//                } else
                moduleSettingsPanel.setVisible(false);
        } else if (dataset instanceof NumericDataset) {
                ((CardLayout) mainPanel.getLayout()).show(mainPanel, "wiggleDataset");
                foregroundColorButton_wiggle.setBackground(settings.getForeGroundColor(datasetname));
                backgroundColorButton_wiggle.setBackground(settings.getBackGroundColor(datasetname));
                baselineColorButton_wiggle.setBackground(settings.getBaselineColor(datasetname));
                secondaryColorButton_wiggle.setBackground(settings.getSecondaryColor(datasetname));
                heightSelectionSpinner_wiggle.getModel().setValue(new Integer(settings.getDataTrackHeight(datasetname)));
                ArrayList<String> graphs = settings.getGUI().getRegisteredDataTrackVisualizers(NumericDataset.class);
                String[] graphTypes=new String[graphs.size()];
                graphTypes=graphs.toArray(graphTypes);              
                graphTypeCombobox_wiggle.setModel(new DefaultComboBoxModel(graphTypes));
                String currentGraph=settings.getGraphType(datasetname);
                if (currentGraph!=null && !currentGraph.isEmpty()) graphTypeCombobox_wiggle.setSelectedItem(currentGraph);
        } else if (dataset instanceof DNASequenceDataset) {
                ((CardLayout) mainPanel.getLayout()).show(mainPanel, "sequenceDataset");
                baseAButton.setBackground(settings.getBaseColor('A'));
                baseCButton.setBackground(settings.getBaseColor('C'));
                baseGButton.setBackground(settings.getBaseColor('G'));
                baseTButton.setBackground(settings.getBaseColor('T'));
                maskedBasecolorButton.setBackground(settings.getBaseColor('X'));
                heightSelectionSpinner_sequence.getModel().setValue(new Integer(settings.getDataTrackHeight(datasetname)));
                ArrayList<String> graphs = settings.getGUI().getRegisteredDataTrackVisualizers(DNASequenceDataset.class);
                String[] graphTypes=new String[graphs.size()];
                graphTypes=graphs.toArray(graphTypes);              
                graphTypeCombobox_sequence.setModel(new DefaultComboBoxModel(graphTypes));
                String currentGraph=settings.getGraphType(datasetname);
                if (currentGraph!=null && !currentGraph.isEmpty()) graphTypeCombobox_sequence.setSelectedItem(currentGraph);                
       }

    }

    /**
     * This method reads a list of all datasets in the engine and adds them to the combo box
     * @return
     */
    private DefaultComboBoxModel getComboBoxModel() {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        ArrayList<Data> alldatasets = settings.getEngine().getAllDataItemsOfType(FeatureDataset.class);
        Collections.sort(alldatasets, new NameComparator());
        for (Data d : alldatasets) {
            model.addElement(d);
        }
        return model;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        featureGraphTypeButtonGroup = new javax.swing.ButtonGroup();
        singlemulticolorButtongroup = new javax.swing.ButtonGroup();
        selectDatasetPanel = new javax.swing.JPanel();
        datasetSelectionLabel = new javax.swing.JLabel();
        datasetSelectionComboBox = new javax.swing.JComboBox();
        mainPanel = new javax.swing.JPanel();
        wiggleDatasetSettingsPanel = new javax.swing.JPanel();
        foregroundColorLabel_wiggle = new javax.swing.JLabel();
        foregroundColorButton_wiggle = new javax.swing.JButton();
        backgroundColorLabel_wiggle = new javax.swing.JLabel();
        backgroundColorButton_wiggle = new javax.swing.JButton();
        graphTypeLabel_wiggle = new javax.swing.JLabel();
        heightSelectionLabel_wiggle = new javax.swing.JLabel();
        heightSelectionSpinner_wiggle = new javax.swing.JSpinner();
        graphTypeCombobox_wiggle = new javax.swing.JComboBox();
        secondaryColorLabel_wiggle = new javax.swing.JLabel();
        secondaryColorButton_wiggle = new javax.swing.JButton();
        baselineColorLabel_wiggle = new javax.swing.JLabel();
        baselineColorButton_wiggle = new javax.swing.JButton();
        sequenceDatasetSettingsPanel = new javax.swing.JPanel();
        basecolorLabel = new javax.swing.JLabel();
        baseAButton = new javax.swing.JButton();
        baseCButton = new javax.swing.JButton();
        baseGButton = new javax.swing.JButton();
        baseTButton = new javax.swing.JButton();
        maskedBaseLabel = new javax.swing.JLabel();
        maskedBasecolorButton = new javax.swing.JButton();
        heightSelectionLabel_sequence = new javax.swing.JLabel();
        heightSelectionSpinner_sequence = new javax.swing.JSpinner();
        graphTypeCombobox_sequence = new javax.swing.JComboBox();
        graphTypeLabel_sequence = new javax.swing.JLabel();
        regionDatasetSettingsPanel = new javax.swing.JPanel();
        foregroundColorLabel_region = new javax.swing.JLabel();
        foregroundColorButton_region = new javax.swing.JButton();
        backgroundColorLabel_region = new javax.swing.JLabel();
        backgroundColorButton_region = new javax.swing.JButton();
        visualizeStrandLabel_region = new javax.swing.JLabel();
        heightSelectionLabel_region = new javax.swing.JLabel();
        heightSelectionSpinner_region = new javax.swing.JSpinner();
        graphType1Icon_region = new javax.swing.JLabel();
        graphType2Icon_region = new javax.swing.JLabel();
        singlecolorRadioButton = new javax.swing.JRadioButton();
        multicolorRadioButton = new javax.swing.JRadioButton();
        visualizeScoreLabel_region = new javax.swing.JLabel();
        visualizeScoreCheckbox = new javax.swing.JCheckBox();
        visualizeStrandCheckbox = new javax.swing.JCheckBox();
        moduleSettingsPanel = new javax.swing.JPanel();
        moduleOutlineColorLabel = new javax.swing.JLabel();
        moduleOutlineColorComboBox = new javax.swing.JComboBox();
        moduleOutlineColorButton = new javax.swing.JButton();
        moduleFillColorLabel = new javax.swing.JLabel();
        moduleFillColorComboBox = new javax.swing.JComboBox();
        moduleFillColorButton = new javax.swing.JButton();
        baselineColorLabel_region = new javax.swing.JLabel();
        baselineColorButton_region = new javax.swing.JButton();
        gradientFillLabel_region = new javax.swing.JLabel();
        gradientFillButton_region = new javax.swing.JButton();
        expandTrackLabel_region = new javax.swing.JLabel();
        showExpandedCheckbox_region = new javax.swing.JCheckBox();
        expandedRegionHeightLabel = new javax.swing.JLabel();
        expandedRegionHeightSpinner = new javax.swing.JSpinner();
        graphTypeLabel_region = new javax.swing.JLabel();
        graphTypeCombobox_region = new javax.swing.JComboBox();
        buttonsPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        applyButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DatasetDisplaySettingsDialog.class);
        setTitle(resourceMap.getString("Display settings.title")); // NOI18N
        setName("Display settings"); // NOI18N
        setResizable(false);

        selectDatasetPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 8, 6, 8));
        selectDatasetPanel.setName("selectDatasetPanel"); // NOI18N
        selectDatasetPanel.setLayout(new javax.swing.BoxLayout(selectDatasetPanel, javax.swing.BoxLayout.LINE_AXIS));

        datasetSelectionLabel.setText(resourceMap.getString("datasetSelectionLabel.text")); // NOI18N
        datasetSelectionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 12));
        datasetSelectionLabel.setName("datasetSelectionLabel"); // NOI18N
        selectDatasetPanel.add(datasetSelectionLabel);

        datasetSelectionComboBox.setModel(getComboBoxModel());
        datasetSelectionComboBox.setName("datasetSelectionComboBox"); // NOI18N
        selectDatasetPanel.add(datasetSelectionComboBox);

        getContentPane().add(selectDatasetPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.CardLayout());

        wiggleDatasetSettingsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8), javax.swing.BorderFactory.createEtchedBorder()));
        wiggleDatasetSettingsPanel.setName("wiggleDatasetSettingsPanel"); // NOI18N
        wiggleDatasetSettingsPanel.setLayout(new java.awt.GridBagLayout());

        foregroundColorLabel_wiggle.setText(resourceMap.getString("foregroundColorLabel_wiggle.text")); // NOI18N
        foregroundColorLabel_wiggle.setName("foregroundColorLabel_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        wiggleDatasetSettingsPanel.add(foregroundColorLabel_wiggle, gridBagConstraints);

        foregroundColorButton_wiggle.setText(resourceMap.getString("foregroundColorButton_wiggle.text")); // NOI18N
        foregroundColorButton_wiggle.setToolTipText(resourceMap.getString("foregroundColorButton_wiggle.toolTipText")); // NOI18N
        foregroundColorButton_wiggle.setBorder(null);
        foregroundColorButton_wiggle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        foregroundColorButton_wiggle.setMaximumSize(new java.awt.Dimension(25, 25));
        foregroundColorButton_wiggle.setMinimumSize(new java.awt.Dimension(25, 25));
        foregroundColorButton_wiggle.setName("foregroundColorButton_wiggle"); // NOI18N
        foregroundColorButton_wiggle.setPreferredSize(new java.awt.Dimension(25, 25));
        foregroundColorButton_wiggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                foregroundColorButton_wigglePressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        wiggleDatasetSettingsPanel.add(foregroundColorButton_wiggle, gridBagConstraints);

        backgroundColorLabel_wiggle.setText(resourceMap.getString("backgroundColorLabel_wiggle.text")); // NOI18N
        backgroundColorLabel_wiggle.setName("backgroundColorLabel_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        wiggleDatasetSettingsPanel.add(backgroundColorLabel_wiggle, gridBagConstraints);

        backgroundColorButton_wiggle.setText(resourceMap.getString("backgroundColorButton_wiggle.text")); // NOI18N
        backgroundColorButton_wiggle.setToolTipText(resourceMap.getString("backgroundColorButton_wiggle.toolTipText")); // NOI18N
        backgroundColorButton_wiggle.setBorder(null);
        backgroundColorButton_wiggle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        backgroundColorButton_wiggle.setMaximumSize(new java.awt.Dimension(25, 25));
        backgroundColorButton_wiggle.setMinimumSize(new java.awt.Dimension(25, 25));
        backgroundColorButton_wiggle.setName("backgroundColorButton_wiggle"); // NOI18N
        backgroundColorButton_wiggle.setPreferredSize(new java.awt.Dimension(25, 25));
        backgroundColorButton_wiggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backgroundColorButton_wigglePressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        wiggleDatasetSettingsPanel.add(backgroundColorButton_wiggle, gridBagConstraints);

        graphTypeLabel_wiggle.setText(resourceMap.getString("graphTypeLabel_wiggle.text")); // NOI18N
        graphTypeLabel_wiggle.setName("graphTypeLabel_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        wiggleDatasetSettingsPanel.add(graphTypeLabel_wiggle, gridBagConstraints);

        heightSelectionLabel_wiggle.setText(resourceMap.getString("heightSelectionLabel_wiggle.text")); // NOI18N
        heightSelectionLabel_wiggle.setName("heightSelectionLabel_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        wiggleDatasetSettingsPanel.add(heightSelectionLabel_wiggle, gridBagConstraints);

        heightSelectionSpinner_wiggle.setModel(getTrackHeightSpinnerModel());
        heightSelectionSpinner_wiggle.setName("heightSelectionSpinner_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        wiggleDatasetSettingsPanel.add(heightSelectionSpinner_wiggle, gridBagConstraints);

        graphTypeCombobox_wiggle.setName("graphTypeCombobox_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        wiggleDatasetSettingsPanel.add(graphTypeCombobox_wiggle, gridBagConstraints);

        secondaryColorLabel_wiggle.setText(resourceMap.getString("secondaryColorLabel_wiggle.text")); // NOI18N
        secondaryColorLabel_wiggle.setName("secondaryColorLabel_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 22, 8, 15);
        wiggleDatasetSettingsPanel.add(secondaryColorLabel_wiggle, gridBagConstraints);

        secondaryColorButton_wiggle.setText(resourceMap.getString("secondaryColorButton_wiggle.text")); // NOI18N
        secondaryColorButton_wiggle.setToolTipText(resourceMap.getString("secondaryColorButton_wiggle.toolTipText")); // NOI18N
        secondaryColorButton_wiggle.setBorder(null);
        secondaryColorButton_wiggle.setMaximumSize(new java.awt.Dimension(25, 25));
        secondaryColorButton_wiggle.setMinimumSize(new java.awt.Dimension(25, 25));
        secondaryColorButton_wiggle.setName("secondaryColorButton_wiggle"); // NOI18N
        secondaryColorButton_wiggle.setPreferredSize(new java.awt.Dimension(25, 25));
        secondaryColorButton_wiggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondaryColorButton_wigglePressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 16);
        wiggleDatasetSettingsPanel.add(secondaryColorButton_wiggle, gridBagConstraints);

        baselineColorLabel_wiggle.setText(resourceMap.getString("baselineColorLabel_wiggle.text")); // NOI18N
        baselineColorLabel_wiggle.setName("baselineColorLabel_wiggle"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 22, 8, 15);
        wiggleDatasetSettingsPanel.add(baselineColorLabel_wiggle, gridBagConstraints);

        baselineColorButton_wiggle.setText(resourceMap.getString("baselineColorButton_wiggle.text")); // NOI18N
        baselineColorButton_wiggle.setToolTipText(resourceMap.getString("baselineColorButton_wiggle.toolTipText")); // NOI18N
        baselineColorButton_wiggle.setBorder(null);
        baselineColorButton_wiggle.setMaximumSize(new java.awt.Dimension(25, 25));
        baselineColorButton_wiggle.setMinimumSize(new java.awt.Dimension(25, 25));
        baselineColorButton_wiggle.setName("baselineColorButton_wiggle"); // NOI18N
        baselineColorButton_wiggle.setPreferredSize(new java.awt.Dimension(25, 25));
        baselineColorButton_wiggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baselineColorButton_wigglePressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 16);
        wiggleDatasetSettingsPanel.add(baselineColorButton_wiggle, gridBagConstraints);

        mainPanel.add(wiggleDatasetSettingsPanel, "wiggleDataset");

        sequenceDatasetSettingsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8), javax.swing.BorderFactory.createEtchedBorder()));
        sequenceDatasetSettingsPanel.setName("sequenceDatasetSettingsPanel"); // NOI18N
        sequenceDatasetSettingsPanel.setLayout(new java.awt.GridBagLayout());

        basecolorLabel.setText(resourceMap.getString("basecolorLabel.text")); // NOI18N
        basecolorLabel.setName("basecolorLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        sequenceDatasetSettingsPanel.add(basecolorLabel, gridBagConstraints);

        baseAButton.setText(resourceMap.getString("baseAButton.text")); // NOI18N
        baseAButton.setToolTipText(resourceMap.getString("baseAButton.toolTipText")); // NOI18N
        baseAButton.setBorder(null);
        baseAButton.setMaximumSize(new java.awt.Dimension(25, 25));
        baseAButton.setMinimumSize(new java.awt.Dimension(25, 25));
        baseAButton.setName("baseAButton"); // NOI18N
        baseAButton.setPreferredSize(new java.awt.Dimension(25, 25));
        baseAButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baseButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 2);
        sequenceDatasetSettingsPanel.add(baseAButton, gridBagConstraints);

        baseCButton.setText(resourceMap.getString("baseCButton.text")); // NOI18N
        baseCButton.setToolTipText(resourceMap.getString("baseCButton.toolTipText")); // NOI18N
        baseCButton.setBorder(null);
        baseCButton.setMaximumSize(new java.awt.Dimension(25, 25));
        baseCButton.setMinimumSize(new java.awt.Dimension(25, 25));
        baseCButton.setName("baseCButton"); // NOI18N
        baseCButton.setPreferredSize(new java.awt.Dimension(25, 25));
        baseCButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baseButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 2, 8, 2);
        sequenceDatasetSettingsPanel.add(baseCButton, gridBagConstraints);

        baseGButton.setText(resourceMap.getString("baseGButton.text")); // NOI18N
        baseGButton.setToolTipText(resourceMap.getString("baseGButton.toolTipText")); // NOI18N
        baseGButton.setBorder(null);
        baseGButton.setMaximumSize(new java.awt.Dimension(25, 25));
        baseGButton.setMinimumSize(new java.awt.Dimension(25, 25));
        baseGButton.setName("baseGButton"); // NOI18N
        baseGButton.setPreferredSize(new java.awt.Dimension(25, 25));
        baseGButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baseButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 2, 8, 2);
        sequenceDatasetSettingsPanel.add(baseGButton, gridBagConstraints);

        baseTButton.setText(resourceMap.getString("baseTButton.text")); // NOI18N
        baseTButton.setToolTipText(resourceMap.getString("baseTButton.toolTipText")); // NOI18N
        baseTButton.setBorder(null);
        baseTButton.setMaximumSize(new java.awt.Dimension(25, 25));
        baseTButton.setMinimumSize(new java.awt.Dimension(25, 25));
        baseTButton.setName("baseTButton"); // NOI18N
        baseTButton.setPreferredSize(new java.awt.Dimension(25, 25));
        baseTButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baseButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(8, 2, 8, 8);
        sequenceDatasetSettingsPanel.add(baseTButton, gridBagConstraints);

        maskedBaseLabel.setText(resourceMap.getString("maskedBaseLabel.text")); // NOI18N
        maskedBaseLabel.setName("maskedBaseLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        sequenceDatasetSettingsPanel.add(maskedBaseLabel, gridBagConstraints);

        maskedBasecolorButton.setText(resourceMap.getString("maskedBasecolorButton.text")); // NOI18N
        maskedBasecolorButton.setToolTipText(resourceMap.getString("maskedBasecolorButton.toolTipText")); // NOI18N
        maskedBasecolorButton.setActionCommand(resourceMap.getString("maskedBasecolorButton.actionCommand")); // NOI18N
        maskedBasecolorButton.setBorder(null);
        maskedBasecolorButton.setMaximumSize(new java.awt.Dimension(25, 25));
        maskedBasecolorButton.setMinimumSize(new java.awt.Dimension(25, 25));
        maskedBasecolorButton.setName("maskedBasecolorButton"); // NOI18N
        maskedBasecolorButton.setPreferredSize(new java.awt.Dimension(25, 25));
        maskedBasecolorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baseButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 2);
        sequenceDatasetSettingsPanel.add(maskedBasecolorButton, gridBagConstraints);

        heightSelectionLabel_sequence.setText(resourceMap.getString("heightSelectionLabel_sequence.text")); // NOI18N
        heightSelectionLabel_sequence.setName("heightSelectionLabel_sequence"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        sequenceDatasetSettingsPanel.add(heightSelectionLabel_sequence, gridBagConstraints);

        heightSelectionSpinner_sequence.setModel(getTrackHeightSpinnerModel());
        heightSelectionSpinner_sequence.setName("heightSelectionSpinner_sequence"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        sequenceDatasetSettingsPanel.add(heightSelectionSpinner_sequence, gridBagConstraints);

        graphTypeCombobox_sequence.setName("graphTypeCombobox_sequence"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        sequenceDatasetSettingsPanel.add(graphTypeCombobox_sequence, gridBagConstraints);

        graphTypeLabel_sequence.setText(resourceMap.getString("graphTypeLabel_sequence.text")); // NOI18N
        graphTypeLabel_sequence.setName("graphTypeLabel_sequence"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        sequenceDatasetSettingsPanel.add(graphTypeLabel_sequence, gridBagConstraints);

        mainPanel.add(sequenceDatasetSettingsPanel, "sequenceDataset");

        regionDatasetSettingsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8), javax.swing.BorderFactory.createEtchedBorder()));
        regionDatasetSettingsPanel.setName("regionDatasetSettingsPanel"); // NOI18N
        regionDatasetSettingsPanel.setLayout(new java.awt.GridBagLayout());

        foregroundColorLabel_region.setText(resourceMap.getString("foregroundColorLabel_region.text")); // NOI18N
        foregroundColorLabel_region.setName("foregroundColorLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        regionDatasetSettingsPanel.add(foregroundColorLabel_region, gridBagConstraints);

        foregroundColorButton_region.setToolTipText(resourceMap.getString("foregroundColorButton_region.toolTipText")); // NOI18N
        foregroundColorButton_region.setBorder(null);
        foregroundColorButton_region.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        foregroundColorButton_region.setMaximumSize(new java.awt.Dimension(25, 25));
        foregroundColorButton_region.setMinimumSize(new java.awt.Dimension(25, 25));
        foregroundColorButton_region.setName("foregroundColorButton_region"); // NOI18N
        foregroundColorButton_region.setPreferredSize(new java.awt.Dimension(25, 25));
        foregroundColorButton_region.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                foregroundColorButton_regionPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        regionDatasetSettingsPanel.add(foregroundColorButton_region, gridBagConstraints);

        backgroundColorLabel_region.setText(resourceMap.getString("backgroundColorLabel_region.text")); // NOI18N
        backgroundColorLabel_region.setName("backgroundColorLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        regionDatasetSettingsPanel.add(backgroundColorLabel_region, gridBagConstraints);

        backgroundColorButton_region.setToolTipText(resourceMap.getString("backgroundColorButton_region.toolTipText")); // NOI18N
        backgroundColorButton_region.setBorder(null);
        backgroundColorButton_region.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        backgroundColorButton_region.setMaximumSize(new java.awt.Dimension(25, 25));
        backgroundColorButton_region.setMinimumSize(new java.awt.Dimension(25, 25));
        backgroundColorButton_region.setName("backgroundColorButton_region"); // NOI18N
        backgroundColorButton_region.setPreferredSize(new java.awt.Dimension(25, 25));
        backgroundColorButton_region.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backgroundColorButton_regionPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        regionDatasetSettingsPanel.add(backgroundColorButton_region, gridBagConstraints);

        visualizeStrandLabel_region.setText(resourceMap.getString("visualizeStrandLabel_region.text")); // NOI18N
        visualizeStrandLabel_region.setToolTipText(resourceMap.getString("visualizeStrandLabel_region.toolTipText")); // NOI18N
        visualizeStrandLabel_region.setName("visualizeStrandLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(16, 8, 8, 15);
        regionDatasetSettingsPanel.add(visualizeStrandLabel_region, gridBagConstraints);

        heightSelectionLabel_region.setText(resourceMap.getString("heightSelectionLabel_region.text")); // NOI18N
        heightSelectionLabel_region.setName("heightSelectionLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        regionDatasetSettingsPanel.add(heightSelectionLabel_region, gridBagConstraints);

        heightSelectionSpinner_region.setModel(getTrackHeightSpinnerModel());
        heightSelectionSpinner_region.setName("heightSelectionSpinner_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        regionDatasetSettingsPanel.add(heightSelectionSpinner_region, gridBagConstraints);

        graphType1Icon_region.setIcon(icon1_region       );
        graphType1Icon_region.setMaximumSize(new java.awt.Dimension(22, 22));
        graphType1Icon_region.setMinimumSize(new java.awt.Dimension(22, 22));
        graphType1Icon_region.setName("graphType1Icon_region"); // NOI18N
        graphType1Icon_region.setPreferredSize(new java.awt.Dimension(22, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        regionDatasetSettingsPanel.add(graphType1Icon_region, gridBagConstraints);

        graphType2Icon_region.setIcon(icon2_region);
        graphType2Icon_region.setMaximumSize(new java.awt.Dimension(22, 22));
        graphType2Icon_region.setMinimumSize(new java.awt.Dimension(22, 22));
        graphType2Icon_region.setName("graphType2Icon_region"); // NOI18N
        graphType2Icon_region.setPreferredSize(new java.awt.Dimension(22, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        regionDatasetSettingsPanel.add(graphType2Icon_region, gridBagConstraints);

        singlemulticolorButtongroup.add(singlecolorRadioButton);
        singlecolorRadioButton.setText(resourceMap.getString("singlecolorRadioButton.text")); // NOI18N
        singlecolorRadioButton.setName("singlecolorRadioButton"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 10);
        regionDatasetSettingsPanel.add(singlecolorRadioButton, gridBagConstraints);

        singlemulticolorButtongroup.add(multicolorRadioButton);
        multicolorRadioButton.setText(resourceMap.getString("multicolorRadioButton.text")); // NOI18N
        multicolorRadioButton.setName("multicolorRadioButton"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 10);
        regionDatasetSettingsPanel.add(multicolorRadioButton, gridBagConstraints);

        visualizeScoreLabel_region.setText(resourceMap.getString("visualizeScoreLabel_region.text")); // NOI18N
        visualizeScoreLabel_region.setToolTipText(resourceMap.getString("visualizeScoreLabel_region.toolTipText")); // NOI18N
        visualizeScoreLabel_region.setName("visualizeScoreLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        regionDatasetSettingsPanel.add(visualizeScoreLabel_region, gridBagConstraints);

        visualizeScoreCheckbox.setText(resourceMap.getString("visualizeScoreCheckbox.text")); // NOI18N
        visualizeScoreCheckbox.setToolTipText(resourceMap.getString("visualizeScoreCheckbox.toolTipText")); // NOI18N
        visualizeScoreCheckbox.setName("visualizeScoreCheckbox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        regionDatasetSettingsPanel.add(visualizeScoreCheckbox, gridBagConstraints);

        visualizeStrandCheckbox.setText(resourceMap.getString("visualizeStrandCheckbox.text")); // NOI18N
        visualizeStrandCheckbox.setToolTipText(resourceMap.getString("visualizeStrandCheckbox.toolTipText")); // NOI18N
        visualizeStrandCheckbox.setName("visualizeStrandCheckbox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(16, 0, 8, 0);
        regionDatasetSettingsPanel.add(visualizeStrandCheckbox, gridBagConstraints);

        moduleSettingsPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        moduleSettingsPanel.setMinimumSize(new java.awt.Dimension(10, 66));
        moduleSettingsPanel.setName("moduleSettingsPanel"); // NOI18N
        moduleSettingsPanel.setPreferredSize(new java.awt.Dimension(10, 66));
        moduleSettingsPanel.setLayout(new java.awt.GridBagLayout());

        moduleOutlineColorLabel.setText(resourceMap.getString("moduleOutlineColorLabel.text")); // NOI18N
        moduleOutlineColorLabel.setName("moduleOutlineColorLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 8);
        moduleSettingsPanel.add(moduleOutlineColorLabel, gridBagConstraints);

        moduleOutlineColorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Chosen color", "Color by type" }));
        moduleOutlineColorComboBox.setName("moduleOutlineColorComboBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        moduleSettingsPanel.add(moduleOutlineColorComboBox, gridBagConstraints);

        moduleOutlineColorButton.setText(resourceMap.getString("moduleOutlineColorButton.text")); // NOI18N
        moduleOutlineColorButton.setBorder(null);
        moduleOutlineColorButton.setMaximumSize(new java.awt.Dimension(19, 19));
        moduleOutlineColorButton.setMinimumSize(new java.awt.Dimension(19, 19));
        moduleOutlineColorButton.setName("moduleOutlineColorButton"); // NOI18N
        moduleOutlineColorButton.setPreferredSize(new java.awt.Dimension(19, 19));
        moduleOutlineColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moduleOutlineColorButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(3, 8, 3, 0);
        moduleSettingsPanel.add(moduleOutlineColorButton, gridBagConstraints);

        moduleFillColorLabel.setText(resourceMap.getString("moduleFillColorLabel.text")); // NOI18N
        moduleFillColorLabel.setName("moduleFillColorLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 8);
        moduleSettingsPanel.add(moduleFillColorLabel, gridBagConstraints);

        moduleFillColorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Chosen color", "Color by type" }));
        moduleFillColorComboBox.setName("moduleFillColorComboBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        moduleSettingsPanel.add(moduleFillColorComboBox, gridBagConstraints);

        moduleFillColorButton.setText(resourceMap.getString("moduleFillColorButton.text")); // NOI18N
        moduleFillColorButton.setBorder(null);
        moduleFillColorButton.setMaximumSize(new java.awt.Dimension(19, 19));
        moduleFillColorButton.setMinimumSize(new java.awt.Dimension(19, 19));
        moduleFillColorButton.setName("moduleFillColorButton"); // NOI18N
        moduleFillColorButton.setPreferredSize(new java.awt.Dimension(19, 19));
        moduleFillColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moduleFillColorButtonPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(3, 8, 3, 0);
        moduleSettingsPanel.add(moduleFillColorButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
        regionDatasetSettingsPanel.add(moduleSettingsPanel, gridBagConstraints);

        baselineColorLabel_region.setText(resourceMap.getString("baselineColorLabel_region.text")); // NOI18N
        baselineColorLabel_region.setName("baselineColorLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 16, 8, 15);
        regionDatasetSettingsPanel.add(baselineColorLabel_region, gridBagConstraints);

        baselineColorButton_region.setText(resourceMap.getString("baselineColorButton_region.text")); // NOI18N
        baselineColorButton_region.setToolTipText(resourceMap.getString("baselineColorButton_region.toolTipText")); // NOI18N
        baselineColorButton_region.setBorder(null);
        baselineColorButton_region.setMaximumSize(new java.awt.Dimension(25, 25));
        baselineColorButton_region.setMinimumSize(new java.awt.Dimension(25, 25));
        baselineColorButton_region.setName("baselineColorButton_region"); // NOI18N
        baselineColorButton_region.setPreferredSize(new java.awt.Dimension(25, 25));
        baselineColorButton_region.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baselineColorButton_regionPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 8);
        regionDatasetSettingsPanel.add(baselineColorButton_region, gridBagConstraints);

        gradientFillLabel_region.setText(resourceMap.getString("gradientFillLabel_region.text")); // NOI18N
        gradientFillLabel_region.setToolTipText(resourceMap.getString("gradientFillLabel_region.toolTipText")); // NOI18N
        gradientFillLabel_region.setName("gradientFillLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(16, 16, 8, 15);
        regionDatasetSettingsPanel.add(gradientFillLabel_region, gridBagConstraints);

        gradientFillButton_region.setText(resourceMap.getString("gradientFillButton_region.text")); // NOI18N
        gradientFillButton_region.setToolTipText(resourceMap.getString("gradientFillButton_region.toolTipText")); // NOI18N
        gradientFillButton_region.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        gradientFillButton_region.setIconTextGap(0);
        gradientFillButton_region.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gradientFillButton_region.setMaximumSize(new java.awt.Dimension(16, 16));
        gradientFillButton_region.setMinimumSize(new java.awt.Dimension(16, 16));
        gradientFillButton_region.setName("gradientFillButton_region"); // NOI18N
        gradientFillButton_region.setPreferredSize(new java.awt.Dimension(16, 16));
        gradientFillButton_region.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gradientFillButton_regionPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        regionDatasetSettingsPanel.add(gradientFillButton_region, gridBagConstraints);

        expandTrackLabel_region.setText(resourceMap.getString("expandTrackLabel_region.text")); // NOI18N
        expandTrackLabel_region.setToolTipText(resourceMap.getString("expandTrackLabel_region.toolTipText")); // NOI18N
        expandTrackLabel_region.setName("expandTrackLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 16, 8, 15);
        regionDatasetSettingsPanel.add(expandTrackLabel_region, gridBagConstraints);

        showExpandedCheckbox_region.setText(resourceMap.getString("showExpandedCheckbox_region.text")); // NOI18N
        showExpandedCheckbox_region.setToolTipText(resourceMap.getString("showExpandedCheckbox_region.toolTipText")); // NOI18N
        showExpandedCheckbox_region.setName("showExpandedCheckbox_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 0);
        regionDatasetSettingsPanel.add(showExpandedCheckbox_region, gridBagConstraints);

        expandedRegionHeightLabel.setText(resourceMap.getString("expandedRegionHeightLabel.text")); // NOI18N
        expandedRegionHeightLabel.setName("expandedRegionHeightLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 16, 8, 15);
        regionDatasetSettingsPanel.add(expandedRegionHeightLabel, gridBagConstraints);

        expandedRegionHeightSpinner.setModel(getExpandedRegionHeightSpinnerModel());
        expandedRegionHeightSpinner.setName("expandedRegionHeightSpinner"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        regionDatasetSettingsPanel.add(expandedRegionHeightSpinner, gridBagConstraints);

        graphTypeLabel_region.setText(resourceMap.getString("graphTypeLabel_region.text")); // NOI18N
        graphTypeLabel_region.setName("graphTypeLabel_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 15);
        regionDatasetSettingsPanel.add(graphTypeLabel_region, gridBagConstraints);

        graphTypeCombobox_region.setName("graphTypeCombobox_region"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        regionDatasetSettingsPanel.add(graphTypeCombobox_region, gridBagConstraints);

        mainPanel.add(regionDatasetSettingsPanel, "regionDataset");

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setName("buttonsPanel"); // NOI18N

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMaximumSize(new java.awt.Dimension(75, 27));
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.setPreferredSize(new java.awt.Dimension(75, 27));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });

        applyButton.setText(resourceMap.getString("applyButton.text")); // NOI18N
        applyButton.setMaximumSize(new java.awt.Dimension(75, 27));
        applyButton.setMinimumSize(new java.awt.Dimension(75, 27));
        applyButton.setName("applyButton"); // NOI18N
        applyButton.setPreferredSize(new java.awt.Dimension(75, 27));
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonPressed(evt);
            }
        });

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setPreferredSize(new java.awt.Dimension(75, 27));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonPressed(evt);
            }
        });

        javax.swing.GroupLayout buttonsPanelLayout = new javax.swing.GroupLayout(buttonsPanel);
        buttonsPanel.setLayout(buttonsPanelLayout);
        buttonsPanelLayout.setHorizontalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonsPanelLayout.createSequentialGroup()
                .addContainerGap(79, Short.MAX_VALUE)
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(applyButton, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        buttonsPanelLayout.setVerticalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(applyButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        getAccessibleContext().setAccessibleName(resourceMap.getString("Display settings.AccessibleContext.accessibleName")); // NOI18N

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
    applyChanges();
    setVisible(false);
}//GEN-LAST:event_okButtonPressed

private void applyButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonPressed
    applyChanges();
}//GEN-LAST:event_applyButtonPressed

private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
   setVisible(false);//GEN-LAST:event_cancelButtonPressed
    }

private void foregroundColorButton_wigglePressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_foregroundColorButton_wigglePressed
    Color newcolor = customColorSelection(foregroundColorButton_wiggle.getBackground());
    foregroundColorButton_wiggle.setBackground(newcolor);
    this.repaint();
}//GEN-LAST:event_foregroundColorButton_wigglePressed

private void backgroundColorButton_wigglePressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundColorButton_wigglePressed
    Color newcolor = customColorSelection(backgroundColorButton_wiggle.getBackground());
    backgroundColorButton_wiggle.setBackground(newcolor);
    this.repaint();
}//GEN-LAST:event_backgroundColorButton_wigglePressed

private void baseButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baseButtonPressed
   Color oldcolor=null;
        if (evt.getSource()==baseAButton) {oldcolor=baseAButton.getBackground();}
   else if (evt.getSource()==baseCButton) {oldcolor=baseCButton.getBackground();}
   else if (evt.getSource()==baseGButton) {oldcolor=baseGButton.getBackground();}
   else if (evt.getSource()==baseTButton) {oldcolor=baseTButton.getBackground();}
   else if (evt.getSource()==maskedBasecolorButton) {oldcolor=maskedBasecolorButton.getBackground();}
   Color newcolor = customColorSelection(oldcolor);
        if (evt.getSource()==baseAButton) {baseAButton.setBackground(newcolor);}
   else if (evt.getSource()==baseCButton) {baseCButton.setBackground(newcolor);}
   else if (evt.getSource()==baseGButton) {baseGButton.setBackground(newcolor);}
   else if (evt.getSource()==baseTButton) {baseTButton.setBackground(newcolor);}
   else if (evt.getSource()==maskedBasecolorButton) {maskedBasecolorButton.setBackground(newcolor);}   
}//GEN-LAST:event_baseButtonPressed

private void foregroundColorButton_regionPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_foregroundColorButton_regionPressed
    Color newcolor = customColorSelection(foregroundColorButton_region.getBackground());
    foregroundColorButton_region.setBackground(newcolor);
    icon1_region.setForegroundColor(newcolor);
    icon2_region.setForegroundColor(newcolor);
    gradientfillIcon.setForegroundColor(newcolor);
    this.repaint();
}//GEN-LAST:event_foregroundColorButton_regionPressed

private void backgroundColorButton_regionPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundColorButton_regionPressed
    Color newcolor = customColorSelection(backgroundColorButton_region.getBackground());
    backgroundColorButton_region.setBackground(newcolor);
    icon1_region.setBackgroundColor(newcolor);
    icon2_region.setBackgroundColor(newcolor);
    this.repaint();
}//GEN-LAST:event_backgroundColorButton_regionPressed

private void moduleOutlineColorButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moduleOutlineColorButtonPressed
    Color newcolor = customColorSelection(moduleOutlineColorButton.getBackground());
    moduleOutlineColorButton.setBackground(newcolor);
    this.repaint();
}//GEN-LAST:event_moduleOutlineColorButtonPressed

private void moduleFillColorButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moduleFillColorButtonPressed
    Color newcolor = customColorSelection(moduleFillColorButton.getBackground());
    moduleFillColorButton.setBackground(newcolor);
    this.repaint();
}//GEN-LAST:event_moduleFillColorButtonPressed

private void baselineColorButton_regionPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baselineColorButton_regionPressed
    Color newcolor = customColorSelection(baselineColorButton_region.getBackground());
    baselineColorButton_region.setBackground(newcolor);
}//GEN-LAST:event_baselineColorButton_regionPressed

private void secondaryColorButton_wigglePressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondaryColorButton_wigglePressed
    Color newcolor = customColorSelection(secondaryColorButton_wiggle.getBackground());
    secondaryColorButton_wiggle.setBackground(newcolor);
}//GEN-LAST:event_secondaryColorButton_wigglePressed

private void baselineColorButton_wigglePressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baselineColorButton_wigglePressed
    Color newcolor = customColorSelection(baselineColorButton_wiggle.getBackground());
    baselineColorButton_wiggle.setBackground(newcolor);
}//GEN-LAST:event_baselineColorButton_wigglePressed

    private void gradientFillButton_regionPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gradientFillButton_regionPressed
        int currentIconType=gradientfillIcon.getIconType();
        int currentfillType=0;
        if (currentIconType==MiscIcons.VERTICAL_GRADIENT_FILLED) currentfillType=1;
        else if (currentIconType==MiscIcons.HORIZONTAL_GRADIENT_FILLED) currentfillType=2;
        currentfillType=(currentfillType+1)%3;
        if (currentfillType==0) gradientfillIcon.setIconType(MiscIcons.FLAT_FILLED);
        else if (currentfillType==1) gradientfillIcon.setIconType(MiscIcons.VERTICAL_GRADIENT_FILLED);
        else if (currentfillType==2) gradientfillIcon.setIconType(MiscIcons.HORIZONTAL_GRADIENT_FILLED);
        gradientFillButton_region.repaint();
    }//GEN-LAST:event_gradientFillButton_regionPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyButton;
    private javax.swing.JButton backgroundColorButton_region;
    private javax.swing.JButton backgroundColorButton_wiggle;
    private javax.swing.JLabel backgroundColorLabel_region;
    private javax.swing.JLabel backgroundColorLabel_wiggle;
    private javax.swing.JButton baseAButton;
    private javax.swing.JButton baseCButton;
    private javax.swing.JButton baseGButton;
    private javax.swing.JButton baseTButton;
    private javax.swing.JLabel basecolorLabel;
    private javax.swing.JButton baselineColorButton_region;
    private javax.swing.JButton baselineColorButton_wiggle;
    private javax.swing.JLabel baselineColorLabel_region;
    private javax.swing.JLabel baselineColorLabel_wiggle;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox datasetSelectionComboBox;
    private javax.swing.JLabel datasetSelectionLabel;
    private javax.swing.JLabel expandTrackLabel_region;
    private javax.swing.JLabel expandedRegionHeightLabel;
    private javax.swing.JSpinner expandedRegionHeightSpinner;
    private javax.swing.ButtonGroup featureGraphTypeButtonGroup;
    private javax.swing.JButton foregroundColorButton_region;
    private javax.swing.JButton foregroundColorButton_wiggle;
    private javax.swing.JLabel foregroundColorLabel_region;
    private javax.swing.JLabel foregroundColorLabel_wiggle;
    private javax.swing.JButton gradientFillButton_region;
    private javax.swing.JLabel gradientFillLabel_region;
    private javax.swing.JLabel graphType1Icon_region;
    private javax.swing.JLabel graphType2Icon_region;
    private javax.swing.JComboBox graphTypeCombobox_region;
    private javax.swing.JComboBox graphTypeCombobox_sequence;
    private javax.swing.JComboBox graphTypeCombobox_wiggle;
    private javax.swing.JLabel graphTypeLabel_region;
    private javax.swing.JLabel graphTypeLabel_sequence;
    private javax.swing.JLabel graphTypeLabel_wiggle;
    private javax.swing.JLabel heightSelectionLabel_region;
    private javax.swing.JLabel heightSelectionLabel_sequence;
    private javax.swing.JLabel heightSelectionLabel_wiggle;
    private javax.swing.JSpinner heightSelectionSpinner_region;
    private javax.swing.JSpinner heightSelectionSpinner_sequence;
    private javax.swing.JSpinner heightSelectionSpinner_wiggle;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel maskedBaseLabel;
    private javax.swing.JButton maskedBasecolorButton;
    private javax.swing.JButton moduleFillColorButton;
    private javax.swing.JComboBox moduleFillColorComboBox;
    private javax.swing.JLabel moduleFillColorLabel;
    private javax.swing.JButton moduleOutlineColorButton;
    private javax.swing.JComboBox moduleOutlineColorComboBox;
    private javax.swing.JLabel moduleOutlineColorLabel;
    private javax.swing.JPanel moduleSettingsPanel;
    private javax.swing.JRadioButton multicolorRadioButton;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel regionDatasetSettingsPanel;
    private javax.swing.JButton secondaryColorButton_wiggle;
    private javax.swing.JLabel secondaryColorLabel_wiggle;
    private javax.swing.JPanel selectDatasetPanel;
    private javax.swing.JPanel sequenceDatasetSettingsPanel;
    private javax.swing.JCheckBox showExpandedCheckbox_region;
    private javax.swing.JRadioButton singlecolorRadioButton;
    private javax.swing.ButtonGroup singlemulticolorButtongroup;
    private javax.swing.JCheckBox visualizeScoreCheckbox;
    private javax.swing.JLabel visualizeScoreLabel_region;
    private javax.swing.JCheckBox visualizeStrandCheckbox;
    private javax.swing.JLabel visualizeStrandLabel_region;
    private javax.swing.JPanel wiggleDatasetSettingsPanel;
    // End of variables declaration//GEN-END:variables

    private static JColorChooser setupCustomColorChooser() {
        final JColorChooser chooser = new JColorChooser();
        JPanel newPreviewPanel = new JPanel();
        final JLabel label = new JLabel("Selected color  ");
        label.setOpaque(true);
        label.setPreferredSize(new Dimension(200, 40));
        newPreviewPanel.add(label);
        chooser.getSelectionModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                label.setBackground(chooser.getColor());
            }
        });
        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        AbstractColorChooserPanel[] newpanels = new AbstractColorChooserPanel[1];
        for (int i = 0; i < panels.length; i++) {
            if (panels[i].getDisplayName().equals("Swatches")) {
                newpanels[0] = panels[i];
            }
        }
        chooser.setChooserPanels(newpanels);
        chooser.setPreviewPanel(newPreviewPanel);
        return chooser; 
    }

 
    /**
     * Displays a custom color chooser and returns the color selected 
     */
    private Color customColorSelection(Color defaultcolor) {
          Color color=JColorChooser.showDialog(this, "Select new color", defaultcolor);
          if (color!=null) return color; else return defaultcolor;
    }

    /**
     * Applies the display settings changes currently made to the selected dataset
     * by setting the appropriate properties in the VisualizationSettings object
     * (a GUI repaint will be forced in response so that changes are immediately reflected)
     */
    private void applyChanges() {
        //settings.getGUI().debugMessage("Applying changes");
        String datasetname=dataset.getName();
        if (dataset instanceof DNASequenceDataset) {
            settings.setBaseColor('A', baseAButton.getBackground());
            settings.setBaseColor('a', baseAButton.getBackground());
            settings.setBaseColor('C', baseCButton.getBackground());
            settings.setBaseColor('c', baseCButton.getBackground());
            settings.setBaseColor('G', baseGButton.getBackground());
            settings.setBaseColor('g', baseGButton.getBackground());
            settings.setBaseColor('T', baseTButton.getBackground());
            settings.setBaseColor('t', baseTButton.getBackground());
            settings.setBaseColor('X', maskedBasecolorButton.getBackground());            
            Integer value=(Integer)heightSelectionSpinner_sequence.getValue();
            settings.setDataTrackHeight(datasetname,value.intValue(),true);
            String graphTypeString=(String)graphTypeCombobox_sequence.getSelectedItem();
            settings.setGraphType(datasetname, graphTypeString);            
        } else if (dataset instanceof NumericDataset) {
            Integer value=(Integer)heightSelectionSpinner_wiggle.getValue();
            settings.setDataTrackHeight(datasetname,value.intValue(),true);
            settings.setForeGroundColor(datasetname, foregroundColorButton_wiggle.getBackground());
            settings.setBackGroundColor(datasetname, backgroundColorButton_wiggle.getBackground());
            settings.setSecondaryColor(datasetname, secondaryColorButton_wiggle.getBackground());
            settings.setBaselineColor(datasetname, baselineColorButton_wiggle.getBackground());            
            String graphTypeString=(String)graphTypeCombobox_wiggle.getSelectedItem();
            settings.setGraphType(datasetname, graphTypeString);
        } else { // RegionDataset
            Integer value=(Integer)heightSelectionSpinner_region.getValue();
            settings.setDataTrackHeight(datasetname,value.intValue(),true);
            int regionHeight=(Integer)expandedRegionHeightSpinner.getValue();
            settings.setExpandedRegionHeight(datasetname,regionHeight,true);
            settings.setForeGroundColor(datasetname, foregroundColorButton_region.getBackground());
            settings.setBackGroundColor(datasetname, backgroundColorButton_region.getBackground());
            settings.setBaselineColor(datasetname, baselineColorButton_region.getBackground());            
            settings.setVisualizeRegionStrand(datasetname,visualizeStrandCheckbox.isSelected());
            settings.setVisualizeRegionScore(datasetname,visualizeScoreCheckbox.isSelected());
            int gradientfill=0;
            if (gradientfillIcon.getIconType()==MiscIcons.VERTICAL_GRADIENT_FILLED) gradientfill=1;
            else if (gradientfillIcon.getIconType()==MiscIcons.HORIZONTAL_GRADIENT_FILLED) gradientfill=2;
            settings.setGradientFillRegions(datasetname,gradientfill);
            settings.setExpanded(datasetname,showExpandedCheckbox_region.isSelected());            
            boolean useMultiColor=multicolorRadioButton.isSelected();
            settings.setUseMultiColoredRegions(datasetname,useMultiColor);            
            String graphTypeString=(String)graphTypeCombobox_region.getSelectedItem();
            settings.setGraphType(datasetname, graphTypeString);
        }
        
    }

    
    /**
     * 
     */
    private SpinnerModel getTrackHeightSpinnerModel() {
        int minValue=VisualizationSettings.DATATRACK_MIN_HEIGHT;
        int maxValue=VisualizationSettings.DATATRACK_MAX_HEIGHT;
        int step=1;
        int value=minValue;
        return new SpinnerNumberModel(value,minValue,maxValue,step);
    }
    
    /**
     * 
     */
    private SpinnerModel getExpandedRegionHeightSpinnerModel() {
        int minValue=1;
        int maxValue=100;
        int step=1;
        int value=minValue;
        return new SpinnerNumberModel(value,minValue,maxValue,step);
    }    
    
    
    /**
     * Just an inner class to sort datasets alphabetically in the combobox menu
     */
    private class NameComparator implements Comparator<Data> {

        public int compare(Data o1, Data o2) {
            return o1.getName().compareTo(o2.getName());
        }

        public boolean equals(Data o1, Data o2) {
            return o1.getName().equals(o2.getName());
        }
    }
   
}
