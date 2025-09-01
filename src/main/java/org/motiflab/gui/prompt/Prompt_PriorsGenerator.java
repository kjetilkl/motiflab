/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Graph;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.PriorsGenerator;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.classifier.Classifier;
import org.motiflab.engine.data.classifier.ClassifierVisualizer;
import org.motiflab.engine.data.classifier.ClassifierDataSet;
import org.motiflab.engine.data.classifier.ClassifierOutput;
import org.motiflab.engine.data.classifier.ClassifierOutputListener;
import org.motiflab.engine.data.classifier.ClassifierVisualizerListener;
import org.motiflab.engine.data.classifier.adaboost.AdaBoost;
import org.motiflab.engine.data.classifier.neuralnet.FeedForwardNeuralNet;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.dataformat.DataFormat_PriorsGeneratorFormat;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.gui.LoadFromFilePanel;
import org.motiflab.gui.ParametersDialog;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.SimpleDataPanelIcon;

/**
 * @todo creating PriorsGenerators from configuration should be done in a background thread
 * @author kjetikl
 */
public class Prompt_PriorsGenerator extends Prompt implements ClassifierOutputListener {

    private PriorsGenerator data;

    private JPanel createNewPanel;
    private JPanel showExistingPanel;
    private LoadFromFilePanel importFromFilePanel;
    private JPanel importFromConfigurationPanel;
    private JTabbedPane tabbedPanel;
    private JComboBox classifierCombobox;
    private DefaultListModel classifierslistmodel;
    private int currentPanel=1;
    private JPanel cardspanel;
    private JButton nextButton;
    private JButton backButton;
    private JList featureslist;
    private JList classifierslist;
    private JLabel messageLabel;
    private JTextField configfilenameTextfield;
    private ClassifierDataSet trainingSet=null;
    private ClassifierDataSet validationSet=null;
    private HashMap<String,ParameterSettings> classifierParameterSettings=new HashMap<String, ParameterSettings>();
    private String[] steps=new String[]{"<b>Step 1:</b> Select target and prediction features and add classifiers","<b>Step 2:</b> Select training and validation datasets","<b>Step 3:</b> Train the classifiers and save the Priors Generator","<b>Step 4:</b> Save the Priors Generator"};
    private JProgressBar progressbar;
    private JComboBox targetFeatureCombobox;
    private JTable classifiersTable;
    private boolean showExisting=false;
    private JComboBox samplingStrategyCombobox;
    private boolean isProcessing=false;
    private boolean[] isInterrupted=new boolean[]{false}; // this is an array so it can be passed around by reference
    private LineChartPanel linechartpanel;
    private PieChartPanel piechartpanel;
    private Classifier classifier=null; // the classifier which should be used by the PriorsGenerator. This is "global" here so that training can be aborted by the GUI
    private DecimalFormat show1decimal = new DecimalFormat("#########0.0");
    private ClassifierVisualizer visualizer=null;
    private JButton editButton;
    private JButton removeButton;
    private JLabel selectedFeaturesCountLabel=new JLabel("Selected features: 0  ");
    private JComboBox trainingsetSamplingCombobox;
    private JComboBox trainingsetSequenceCollectionCombobox;
    private JCheckBox trainingsetFilterDuplicates;
    private JCheckBox validationsetFilterDuplicates;
    private JSpinner trainingsetSizeSpinner;
    private JComboBox validationsetSamplingCombobox;
    private JComboBox validationsetSequenceCollectionCombobox;
    private JSpinner validationsetSizeSpinner;
    private final static Color lightRED=new Color(255,120,120);
    private final static Color lightGREEN=new Color(120,255,120);
    private File trainingDatasetFile=null;
    private File validationDatasetFile=null;

    public Prompt_PriorsGenerator(MotifLabGUI gui, String prompt, PriorsGenerator dataitem) {
        this(gui,prompt,dataitem,true);
    }    
    
    public Prompt_PriorsGenerator(MotifLabGUI gui, String prompt, PriorsGenerator dataitem, boolean modal) {
        super(gui,prompt, modal);
        showExisting=(dataitem!=null);
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
            setDataEditable(false); // editing existing objects can not be done at present
        }
        else data=new PriorsGenerator(gui.getGenericDataitemName(PriorsGenerator.class, null));
        setDataItemName(data.getName());
        setTitle("Priors Generator");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.PRIORS_GENERATOR_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(java.awt.Color.WHITE);
        setDataItemIcon(icon, true);
        if (showExisting) {
           setupShowExistingPanel();
           showExistingPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        } else {
            tabbedPanel=new JTabbedPane();
            setupCreateNewPanel();
            setupImportPanel();
            setupImportConfigPanel();            
            tabbedPanel.addTab("Create new", createNewPanel);
            tabbedPanel.addTab("Import From File", importFromFilePanel);
            tabbedPanel.addTab("Import Configuration", importFromConfigurationPanel);
            createNewPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            importFromFilePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            importFromConfigurationPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }

        JPanel internal=new JPanel(new BorderLayout());
        Dimension size=new Dimension(650,380);
        internal.setMinimumSize(size);
        internal.setPreferredSize(size);
        // internal.setMaximumSize(size);

        if (showExisting) internal.add(showExistingPanel); else internal.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(internal);
        if (!showExisting) {
           tabbedPanel.setSelectedComponent(createNewPanel);
        }
        pack();
        if (dataitem!=null) focusOKButton();
    }


    private void setupImportPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(PriorsGenerator.class);
        //dataformats.remove(engine.getDataFormat("Plain"));
        importFromFilePanel=new LoadFromFilePanel(dataformats,gui,PriorsGenerator.class);
    }
    
    private void setupImportConfigPanel() {
        importFromConfigurationPanel = new JPanel();
        importFromConfigurationPanel.setLayout(new BorderLayout());
        JPanel internal1 = new JPanel(new FlowLayout(FlowLayout.LEADING,10,0));
        internal1.setBorder(BorderFactory.createEmptyBorder(50, 26, 20, 20));
        internal1.add(new JLabel("File     "));
        configfilenameTextfield=new JTextField(20);
        JButton configBrowseButton=new JButton("Browse");
        internal1.add(configfilenameTextfield);
        internal1.add(configBrowseButton);
        importFromConfigurationPanel.add(internal1);
        configBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                File file;
                JFileChooser fc = gui.getFileChooser(null);// new JFileChooser(gui.getLastUsedDirectory());
                fc.addChoosableFileFilter(new FileNameExtensionFilter("Configuration-file (*.xml)", "xml","XML"));
                fc.setDialogTitle("Import Priors Generator Configuration File");
                int returnValue=fc.showOpenDialog(Prompt_PriorsGenerator.this);
                Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
                if (returnValue==JFileChooser.APPROVE_OPTION) {
                   file=fc.getSelectedFile();
                   gui.setLastUsedDirectory(file.getParentFile());
                   configfilenameTextfield.setText(file.getAbsolutePath());
                }
            }
        });
    }   

    
    /** Sets up the panel that displays an existing PriorsGenerator */
    private void setupShowExistingPanel() {
        classifier=data.getClassifier();
        showExistingPanel=new JPanel(new BorderLayout());
        String classifierType="No classifier";
        if (classifier!=null) {
            visualizer=classifier.getVisualizer();
            classifierType=classifier.getClassifierType();

        }
        JPanel titlePanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5,5,5,5),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(),
                    BorderFactory.createEmptyBorder(2,2,2,2)
                )
        ));
        JLabel titleLabel=new JLabel("<html><b>"+classifierType+"</b>&nbsp;&nbsp;&nbsp;predicting&nbsp;&nbsp;&nbsp;<b>"+data.getTargetFeatureName()+"</b></html>");
        titleLabel.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        showExistingPanel.add(titlePanel, BorderLayout.NORTH);
        if (classifier==null) return;
        DefaultListModel featureslistmodel = new DefaultListModel();
        String[] features=data.getFeatures();
        for (int i=0;i<features.length;i++) {
            featureslistmodel.addElement(features[i]);
        }
        featureslist = new JList(featureslistmodel);
        featureslist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        featureslist.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedFeature=featureslist.getSelectedIndex();
                visualizer.setSelectedFeature(selectedFeature);
            }
        });
        //featureslist.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane scrollpane1=new JScrollPane(featureslist);
        //scrollpane1.setMaximumSize(listDimension);
        JPanel leftPanel=new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JPanel leftTitle=new JPanel(new FlowLayout(FlowLayout.CENTER));
        leftTitle.add(new JLabel("Features"));
        leftPanel.add(leftTitle,BorderLayout.NORTH);
        leftPanel.add(scrollpane1,BorderLayout.CENTER);
        JPanel rightPanel=new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        JPanel rightTitle=new JPanel(new FlowLayout(FlowLayout.CENTER));
        rightTitle.add(new JLabel("Visualization"));
        rightPanel.add(rightTitle,BorderLayout.NORTH);
        if (visualizer!=null) {
            visualizer.setBorder(BorderFactory.createEtchedBorder());
            rightPanel.add(visualizer,BorderLayout.CENTER);
        } 
        showExistingPanel.add(leftPanel, BorderLayout.WEST);
        showExistingPanel.add(rightPanel, BorderLayout.CENTER);
        visualizer.addListener(new ClassifierVisualizerListener() {
            @Override
            public void featureSelectedInVisualizer(int featureNumber) {
                if (featureNumber>=0) featureslist.setSelectedIndex(featureNumber);
                else featureslist.clearSelection();
            }
        });
    }


    @Override
    public boolean onOKPressed() {
        if (showExisting) return true; // do not change existing PriorsGenerator
        if (tabbedPanel.getSelectedComponent()==importFromFilePanel) {
            try {
                String filename=importFromFilePanel.getFilename();
                if (filename==null) throw new SystemError("Missing filename");
                data=(PriorsGenerator)importFromFilePanel.loadData(data,PriorsGenerator.getType());
                DataFormat format=importFromFilePanel.getDataFormat();
                ParameterSettings settings=importFromFilePanel.getParameterSettings();
                data.setFilename(filename);
                data.setConfigurationFilename(null);
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing Priors Generator from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==importFromConfigurationPanel) {
            try {
                String filename=configfilenameTextfield.getText();
                if (filename!=null) filename=filename.trim();
                if (filename==null || filename.isEmpty()) throw new SystemError("Missing filename");
                data=PriorsGenerator.createPriorsGeneratorFromParameterString(getDataItemName(), Operation_new.CONFIGURATION_PREFIX+filename, engine, null); // To Do: this should be done in a background thread!!!
                data.setConfigurationFilename(filename);
                data.setFilename(null);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing Priors Generator Configuration from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==createNewPanel) {
            if (data.getFilename()==null && data.getConfigurationFilename()==null) {
                JOptionPane.showMessageDialog(this, "The Priors Generator has not been saved","Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (data.getFilename()!=null) {
                DataFormat format=engine.getDefaultDataFormat(data);
                setImportFromFileSettings(data.getFilename(), (format!=null)?format.getName():null, null); // this will help
            }
        }
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);
        return true;
    }

    /** Saves the PriorsGenerator to file. Returns true if the operation went OK */
    private boolean savePriorsGenerator() {
        Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File file;
        File parentDir=gui.getLastUsedDirectory();
        final JFileChooser fc = gui.getFileChooser(parentDir);// new JFileChooser();
        fc.setDialogTitle("Save Priors Generator");
        String suffix=engine.getDataFormat("PriorsGeneratorFormat").getSuffix();
        File preselected=MotifLabEngine.getFile(parentDir,getDataItemName()+"."+suffix);
        fc.setSelectedFile(preselected);
        int returnValue=fc.showSaveDialog(this);
        Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            file=fc.getSelectedFile();
        } else return false;
        if (file.exists()) {
            int choice=JOptionPane.showConfirmDialog(this, "Overwrite existing file \""+file.getName()+"\" ?","Save Priors Generator",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return false;
        }
        gui.setLastUsedDirectory(file.getParentFile());
        data.setTargetFeatureName(((RegionDataset)targetFeatureCombobox.getSelectedItem()).getName());
        Object[] selectedFeatures=featureslist.getSelectedValues();
        String[] features=new String[selectedFeatures.length];
        for (int i=0;i<selectedFeatures.length;i++) features[i]=(String)selectedFeatures[i];
        data.setFeatures(features);
        data.setClassifier(classifier);
        data.setTrainingDataSetUsed(trainingSet); // Note that this call will clear all examples in the trainingSet
        data.setFilename(file.getAbsolutePath());
        data.setConfigurationFilename(null);
        try { // save it to file
            DataFormat_PriorsGeneratorFormat formatter=(DataFormat_PriorsGeneratorFormat)engine.getDataFormat("PriorsGeneratorFormat");
            if (formatter==null) throw new ExecutionError("Unknown format: PriorsGeneratorFormat");
            formatter.savePriorsGeneratorToFile(data, data.getFilename());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An error occurred when saving Priors Generator to file:\n"+e.getMessage(),"Save Error",JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    /** Saves the setup configuration for this PriorsGenerator to file. Returns true if the operation went OK */
    private boolean savePriorsGeneratorConfigFile() {
        Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        File file;
        File parentDir=gui.getLastUsedDirectory();
        final JFileChooser fc = gui.getFileChooser(parentDir);// new JFileChooser();
        fc.setDialogTitle("Save Priors Generator Configuration File");
        String suffix="xml";
        File preselected=MotifLabEngine.getFile(parentDir,getDataItemName()+"."+suffix);
        fc.setSelectedFile(preselected);        
        int returnValue=fc.showSaveDialog(this);
        Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            file=fc.getSelectedFile();
        } else return false;
        if (file.exists()) {
            int choice=JOptionPane.showConfirmDialog(this, "Overwrite existing file \""+file.getName()+"\" ?","Save Priors Generator",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return false;
        }
        gui.setLastUsedDirectory(file.getParentFile());
        data.setTargetFeatureName(((RegionDataset)targetFeatureCombobox.getSelectedItem()).getName());
        Object[] selectedFeatures=featureslist.getSelectedValues();
        String[] features=new String[selectedFeatures.length];
        for (int i=0;i<selectedFeatures.length;i++) features[i]=(String)selectedFeatures[i];
        data.setFeatures(features);
        data.setClassifier(classifier);
        data.setTrainingDataSetUsed(trainingSet); // Note that this call will clear all examples in the trainingSet
        data.setConfigurationFilename(file.getAbsolutePath());
        data.setFilename(null);
        try { // save configuration to file
            saveConfigurationToFile(file);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An error occurred when saving Priors Generator configuration-file:\n"+e.getMessage(),"Save Error",JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }    
    
    @Override
    public Data getData() {
       return data;
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof PriorsGenerator) data=(PriorsGenerator)newdata; 
    }     


    private void saveClassifierDataset(final ClassifierDataSet dataset, final File file) {
        SwingWorker<Boolean, Void> worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                try {
                    ClassifierDataSet.writeARFFfile(file,dataset);
                } catch (Exception ioe) {
                     ex=ioe;
                } 
                return Boolean.TRUE;
            }
            @Override
            public void done() {
                if (ex!=null) JOptionPane.showMessageDialog(Prompt_PriorsGenerator.this, "An error occurred while saving dataset to file:\n"+ex.getClass().getSimpleName()+":"+ex.getMessage(),"Save Error",JOptionPane.ERROR_MESSAGE);

            }
        };
        worker.execute();
    }


    public DefaultComboBoxModel getClassifierTypeNames() {
         String[] list=new String[]{"Neural Network"};
         DefaultComboBoxModel newmodel=new DefaultComboBoxModel(list);
         return newmodel;
    }




    private void setupCreateNewPanel() {
        //parameterSettings=new ParameterSettings();
        //initClassifierPanel(null);
        if (!engine.hasDataItemsOfType(RegionDataset.class)) {
            createNewPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
            createNewPanel.add(new JLabel("<html><br><br><font color=\"red\">You must have some Region features in order to create a new Priors Generator</font></html>"));
            return;
        }
        cardspanel=new JPanel(new CardLayout());
        cardspanel.add(setupFeaturesAndClassifiersPanel(),"Panel1");
        cardspanel.add(setupSelectTrainingdataPanel(),"Panel2");
        cardspanel.add(setupTrainingPanel(),"Panel3");
        cardspanel.add(setupSaveClassifierPanel(),"Panel4");

        JPanel messagePanel=new JPanel();
        messagePanel.setLayout(new BorderLayout());
        JPanel nextButtonPanel=new JPanel();
        nextButtonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        progressbar=new JProgressBar(0, 100);
        Dimension pbardim=new Dimension(80,27);
        progressbar.setMinimumSize(pbardim);
        progressbar.setMaximumSize(pbardim);
        progressbar.setPreferredSize(pbardim);
        nextButtonPanel.add(progressbar);
        backButton=new JButton("< Back");
        nextButtonPanel.add(backButton);
        nextButton=new JButton("Next >");
        nextButtonPanel.add(nextButton);
        messageLabel=new JLabel("");
        messagePanel.add(messageLabel,BorderLayout.CENTER);
        messagePanel.add(nextButtonPanel,BorderLayout.EAST);
        createNewPanel=new JPanel();
        createNewPanel.setLayout(new BorderLayout());
        createNewPanel.add(messagePanel,BorderLayout.SOUTH);
        createNewPanel.add(cardspanel,BorderLayout.CENTER);
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               if (nextButton.getText().equals("Save")) {
                   // what to save?
                   int option=JOptionPane.showOptionDialog(Prompt_PriorsGenerator.this, "Would you like to save the trained PriorsGenerator\nor a configuration file describing how to\nsetup and train the PriorsGenerator", "Save options", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"PriorsGenerator","Configuration","Cancel"}, "PriorsGenerator");
                   if (option==0) { // save PG
                      if (savePriorsGenerator()) clickOK();
                   } else if (option==1) { // save configuration of PG
                      if (savePriorsGeneratorConfigFile()) clickOK();
                   }
                   return;
               } else if (currentPanel==2) {
                   setupTrainingDataSetAndShowTrainPanel();
                   return;
               } else if (currentPanel==3) {
                   trainClassifiers();
                   return;
               }
               currentPanel++;
               showPanel(currentPanel);
               nextButton.setEnabled(currentPanel<4 && featureslist.getSelectedIndex()>=0);
            }
        });
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               if (isProcessing) {
                   isInterrupted[0]=true;
                   if (classifier!=null) classifier.setAbort(true);
               }
               else {
                  if (!nextButton.getText().equals("Save")) currentPanel--;
                  showPanel(currentPanel);
               }
               data.setFilename(null); // signal that data has been changed (possibly) and needs saving
               data.setConfigurationFilename(null); // signal that data has been changed (possibly) and needs saving
            }
        });
        currentPanel=1;
        showPanel(currentPanel);
        nextButton.setEnabled(false);
        backButton.setEnabled(false);
    }

    private void showPanel(int panelnumber) {
       ((CardLayout)cardspanel.getLayout()).show(cardspanel, "Panel"+panelnumber);
       setMessageLabelText(steps[panelnumber-1]);
       nextButton.setEnabled(featureslist.getSelectedIndex()>=0);
       backButton.setEnabled(panelnumber>1);
       if (panelnumber==3) {
           resetTraining();
           nextButton.setText("Train");
       }
       // else if (panelnumber==4) nextButton.setText("Save");
       else nextButton.setText("Next >");
    }

    private void setMessageLabelText(String text) {
        messageLabel.setText("<html>&nbsp;&nbsp;&nbsp;"+text+"</html>");
    }

    private JPanel setupFeaturesAndClassifiersPanel() {
        // ------- TARGET AND PREDICTOR FEATURES ------------
        Dimension list1Dimension=new Dimension(150,180);
        Dimension list2Dimension=new Dimension(150,150);
        JPanel targetAndFeaturesPanel=new JPanel();
        targetAndFeaturesPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor=GridBagConstraints.NORTHWEST;
        constraints.ipadx=20;
        constraints.ipady=20;
        constraints.gridx=0;
        constraints.gridy=0;
        targetAndFeaturesPanel.add(new JLabel("Target"),constraints);

        constraints.gridx=1;
        constraints.gridy=0;
        Object[] allRegionDatasets=engine.getAllDataItemsOfType(RegionDataset.class).toArray();
        Arrays.sort(allRegionDatasets);
        DefaultComboBoxModel targetFeatureComboboxModel = new DefaultComboBoxModel(allRegionDatasets);
        targetFeatureCombobox = new JComboBox(targetFeatureComboboxModel);
        targetAndFeaturesPanel.add(targetFeatureCombobox);

        constraints.gridx=0;
        constraints.gridy=1;
        targetAndFeaturesPanel.add(new JLabel("Features"),constraints);

        constraints.gridx=1;
        constraints.gridy=1;
        DefaultListModel featureslistmodel = new DefaultListModel();
        featureslist = new JList(featureslistmodel);
        featureslist.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
               int numberoffeatures=featureslist.getSelectedIndices().length;
               selectedFeaturesCountLabel.setText("Selected features: "+numberoffeatures);
               if (nextButton!=null) nextButton.setEnabled(numberoffeatures>0 && classifierslistmodel.getSize()>0);
            }
        });
        //featureslist.setBorder(BorderFactory.createLoweredBevelBorder());
        JScrollPane scrollpane1=new JScrollPane(featureslist);
        scrollpane1.setMinimumSize(list1Dimension);
        scrollpane1.setMaximumSize(list1Dimension);
        scrollpane1.setPreferredSize(list1Dimension);
        targetAndFeaturesPanel.add(scrollpane1,constraints);
        targetFeatureCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String current=((JComboBox)e.getSource()).getSelectedItem().toString();
                populateFeaturesList(featureslist, current);
            }
        });
        targetFeatureCombobox.setSelectedIndex(0);
        constraints.gridx=0;
        constraints.gridy=2;
        constraints.gridwidth=2;
        targetAndFeaturesPanel.add(selectedFeaturesCountLabel,constraints);

        // -------  CLASSIFIERS ------------
        JPanel classifiersPanel=new JPanel();
        classifiersPanel.setLayout(new GridBagLayout());
        int listheight=1;
        constraints.anchor=GridBagConstraints.CENTER;
        constraints.ipadx=20;
        constraints.ipady=20;
        constraints.fill=GridBagConstraints.NONE;
        constraints.gridx=0;
        constraints.gridy=0;
        constraints.gridheight=1;
        constraints.gridwidth=1;
        classifiersPanel.add(new JLabel("Classifiers"),constraints);

        constraints.gridx=0;
        constraints.gridy=1;
        constraints.gridheight=listheight;
        constraints.fill=GridBagConstraints.VERTICAL;
        classifierslistmodel = new DefaultListModel();
        classifierslist = new JList(classifierslistmodel);
        classifierslist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        classifierslist.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.getButton()==MouseEvent.BUTTON1 && evt.getClickCount()==2) {
                     Object c=classifierslist.getSelectedValue();
                     if (c instanceof Classifier) showClassifiersPropertiesPrompt((Classifier)c);
                }
            }
        });
        classifierslist.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_DELETE && classifierslist.getSelectedIndex()>=0) {
                    Classifier c=(Classifier)classifierslist.getSelectedValue();
                    classifierslistmodel.remove(classifierslist.getSelectedIndex());
                    classifierParameterSettings.remove(c.getName());
                    if (classifierslistmodel.isEmpty()) {
                        nextButton.setEnabled(false);
                        editButton.setEnabled(false);
                        removeButton.setEnabled(false);
                    }
                }
            }
        });

        classifierslist.setBorder(BorderFactory.createLoweredBevelBorder());
        JScrollPane scrollpane2=new JScrollPane(classifierslist);
        scrollpane2.setMinimumSize(list2Dimension);
        scrollpane2.setMaximumSize(list2Dimension);
        scrollpane2.setPreferredSize(list2Dimension);
        classifiersPanel.add(scrollpane2,constraints);
        
        constraints.gridheight=1;
//        constraints.gridx=0;
//        constraints.gridy=listheight+1;
        constraints.fill=GridBagConstraints.NONE;
        constraints.ipadx=20;
        constraints.ipady=2;
        
        JPanel addnewpanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        DefaultComboBoxModel classifiersComboboxModel = new DefaultComboBoxModel(Classifier.getAvailableClassifierTypes());
        classifierCombobox = new JComboBox(classifiersComboboxModel);
        JButton addnewButton = new JButton("Add New");
        addnewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Classifier c=Classifier.getNewClassifier((String)classifierCombobox.getSelectedItem());
                int size=classifierslistmodel.getSize();
                c.setName(c.getName()+(size+1));
                classifierslistmodel.addElement(c);
                nextButton.setEnabled(featureslist.getSelectedIndex()>=0);
            }
        });
        addnewpanel.add(addnewButton);
        addnewpanel.add(classifierCombobox);
        constraints.gridx=0;
        constraints.gridy=listheight+1;
        constraints.gridwidth=1;
        classifiersPanel.add(addnewpanel,constraints);

        JPanel editandremovepanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        editButton = new JButton("Edit");
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                 Object c=classifierslist.getSelectedValue();
                 if (c instanceof Classifier) showClassifiersPropertiesPrompt((Classifier)c);
             }
        });
        editButton.setEnabled(classifierslist.getSelectedIndex()>=0);
        removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected=classifierslist.getSelectedIndex();
                if (selected<0) return;
                Classifier c=(Classifier)classifierslistmodel.getElementAt(selected);
                classifierslistmodel.remove(selected);
                classifierParameterSettings.remove(c.getName());
                if (classifierslistmodel.isEmpty()) {
                    nextButton.setEnabled(false);
                    editButton.setEnabled(false);
                    removeButton.setEnabled(false);
                }
             }
        });
        removeButton.setEnabled(classifierslist.getSelectedIndex()>=0);
        editandremovepanel.add(editButton);
        editandremovepanel.add(removeButton);
        constraints.gridx=0;
        constraints.gridy=listheight+2;
        constraints.gridwidth=1;
        classifiersPanel.add(editandremovepanel,constraints);

        classifierslist.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                editButton.setEnabled(classifierslist.getSelectedIndex()>=0);
                removeButton.setEnabled(classifierslist.getSelectedIndex()>=0);
            }
        });

        JPanel panel=new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(targetAndFeaturesPanel);
        panel.add(classifiersPanel);
        targetAndFeaturesPanel.setBorder(BorderFactory.createEtchedBorder());
        classifiersPanel.setBorder(BorderFactory.createEtchedBorder());

        return panel;
    }

   /** Adds features to the features list (but excludes the current target feature) */
   private void populateFeaturesList(JList list, String currentTarget) {
       DefaultListModel model=(DefaultListModel)list.getModel();
       model.clear();
       ArrayList<String> templist=new ArrayList<String>();
       for (Data feature:engine.getAllDataItemsOfType(NumericDataset.class)) {
            String featureName=feature.getName();
            if (!featureName.equals(currentTarget)) templist.add(featureName);
       }
       for (Data feature:engine.getAllDataItemsOfType(RegionDataset.class)) {
            String featureName=feature.getName();
            if (!featureName.equals(currentTarget)) templist.add(featureName);
       }
       Collections.sort(templist);
       for (String element:templist) model.addElement(element);
       list.setSelectionInterval(0, model.getSize()-1); // select all by default
   }


   private JPanel setupSelectTrainingdataPanel() {
        JPanel panel=new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        BoxLayout layout=new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);
        JPanel trainingsetpanel=new JPanel();
        trainingsetpanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Training set"));
 
        final SampleExampleClass explainSamplingPanel=new SampleExampleClass(ClassifierDataSet.TOTALLY_RANDOM);
        trainingsetSizeSpinner=new JSpinner(new SpinnerNumberModel(3000, 0, 100000, 1));
        trainingsetFilterDuplicates=new JCheckBox("Remove duplicates");
        trainingsetFilterDuplicates.setHorizontalTextPosition(SwingConstants.LEFT);
        trainingsetFilterDuplicates.setSelected(true);
        trainingsetSamplingCombobox=new JComboBox(new String[]{ClassifierDataSet.TOTALLY_RANDOM,ClassifierDataSet.EVENLY_SPACED,ClassifierDataSet.EVENLY_SPACED_WITHIN_CLASS,ClassifierDataSet.ALL_POSITIVE_RANDOM_NEGATIVE,ClassifierDataSet.ALL_POSITIVE_EVENLY_SPACED_NEGATIVE,ClassifierDataSet.MIDPOINT,ClassifierDataSet.FROM_FILE}); // I removed FULL_SEQUENCE
        DefaultComboBoxModel seqcolmodelTraining=new DefaultComboBoxModel();
        ArrayList<Data> seqCols=engine.getAllDataItemsOfType(SequenceCollection.class);
        Collections.sort(seqCols);        
        for (Data sc:seqCols) {
            seqcolmodelTraining.addElement(sc.getName());
        }
        trainingsetSequenceCollectionCombobox=new JComboBox(seqcolmodelTraining);
        trainingsetSequenceCollectionCombobox.setSelectedItem(engine.getDefaultSequenceCollectionName());
        trainingsetSamplingCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String samplingStrategySelected=(String)trainingsetSamplingCombobox.getSelectedItem();
                explainSamplingPanel.setStrategy(samplingStrategySelected);
                trainingsetSizeSpinner.setEnabled(!(samplingStrategySelected.equals(ClassifierDataSet.MIDPOINT) || samplingStrategySelected.equals(ClassifierDataSet.FULL_SEQUENCE) || samplingStrategySelected.equals(ClassifierDataSet.FROM_FILE)));
                trainingsetSequenceCollectionCombobox.setEnabled(!(samplingStrategySelected.equals(ClassifierDataSet.FROM_FILE)));
            }
        });
        trainingsetSamplingCombobox.setSelectedItem(ClassifierDataSet.EVENLY_SPACED_WITHIN_CLASS);
        trainingsetpanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.anchor=GridBagConstraints.WEST;
        constraints.insets=new Insets(4, 8, 4, 8);
        constraints.gridx=0;
        constraints.gridy=0;
        trainingsetpanel.add(new JLabel("Sampling "),constraints);
        constraints.gridx=1;
        constraints.gridy=0;
        trainingsetpanel.add(trainingsetSamplingCombobox,constraints);
        constraints.gridx=2;
        constraints.gridy=0;
        trainingsetpanel.add(new JLabel("    "),constraints);
        constraints.gridx=3;
        constraints.gridy=0;
        trainingsetpanel.add(new JLabel("Samples "),constraints);
        constraints.gridx=4;
        constraints.gridy=0;
        trainingsetpanel.add(trainingsetSizeSpinner,constraints);
        constraints.gridx=0;
        constraints.gridy=1;
        trainingsetpanel.add(new JLabel("Subset "),constraints);
        constraints.gridx=1;
        constraints.gridy=1;
        constraints.gridwidth=1;
        trainingsetpanel.add(trainingsetSequenceCollectionCombobox,constraints);
        constraints.gridx=3;
        constraints.gridy=1;
        constraints.gridwidth=GridBagConstraints.REMAINDER;
        trainingsetpanel.add(trainingsetFilterDuplicates,constraints);
        constraints.gridx=0;
        constraints.gridy=2;
        constraints.gridwidth=GridBagConstraints.REMAINDER;
        //constraints.fill=GridBagConstraints.NONE;
        trainingsetpanel.add(explainSamplingPanel,constraints);
        constraints.gridwidth=1;

        JPanel validationsetpanel=new JPanel();
        validationsetpanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Validation set"));

        validationsetSizeSpinner=new JSpinner(new SpinnerNumberModel(300, 0, 100000, 1));
        validationsetSamplingCombobox=new JComboBox(new String[]{ClassifierDataSet.SUBSET_OF_TRAINING,ClassifierDataSet.TOTALLY_RANDOM,ClassifierDataSet.EVENLY_SPACED,ClassifierDataSet.EVENLY_SPACED_WITHIN_CLASS,ClassifierDataSet.ALL_POSITIVE_RANDOM_NEGATIVE,ClassifierDataSet.ALL_POSITIVE_EVENLY_SPACED_NEGATIVE,ClassifierDataSet.MIDPOINT,ClassifierDataSet.FROM_FILE,ClassifierDataSet.CROSSVALIDATION}); // I removed FULL_SEQUENCE

        DefaultComboBoxModel seqcolmodelValidation=new DefaultComboBoxModel();
        seqCols=engine.getAllDataItemsOfType(SequenceCollection.class);
        Collections.sort(seqCols);
        for (Data sc:seqCols) {
            seqcolmodelValidation.addElement(sc.getName());
        }
        validationsetSequenceCollectionCombobox=new JComboBox(seqcolmodelValidation);
        validationsetSequenceCollectionCombobox.setSelectedItem(engine.getDefaultSequenceCollectionName());
        validationsetFilterDuplicates=new JCheckBox("Remove duplicates");
        validationsetFilterDuplicates.setHorizontalTextPosition(SwingConstants.LEFT);
        validationsetFilterDuplicates.setSelected(true);
        validationsetSamplingCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String samplingStrategySelected=(String)validationsetSamplingCombobox.getSelectedItem();
                validationsetSizeSpinner.setEnabled(!(samplingStrategySelected.equals(ClassifierDataSet.MIDPOINT) || samplingStrategySelected.equals(ClassifierDataSet.FULL_SEQUENCE) || samplingStrategySelected.equals(ClassifierDataSet.FROM_FILE)));
                validationsetSequenceCollectionCombobox.setEnabled(!(samplingStrategySelected.equals(ClassifierDataSet.FROM_FILE) || samplingStrategySelected.equals(ClassifierDataSet.SUBSET_OF_TRAINING)));
            }
        });
        validationsetSamplingCombobox.setSelectedItem(ClassifierDataSet.SUBSET_OF_TRAINING);

        validationsetpanel.setLayout(new GridBagLayout());
        constraints=new GridBagConstraints();
        constraints.anchor=GridBagConstraints.WEST;
        constraints.insets=new Insets(4, 8, 4, 8);
        constraints.gridx=0;
        constraints.gridy=0;
        validationsetpanel.add(new JLabel("Sampling "),constraints);
        constraints.gridx=1;
        constraints.gridy=0;
        validationsetpanel.add(validationsetSamplingCombobox,constraints);
        constraints.gridx=2;
        constraints.gridy=0;
        validationsetpanel.add(new JLabel("     "),constraints);
        constraints.gridx=3;
        constraints.gridy=0;
        validationsetpanel.add(new JLabel("Samples "),constraints);
        constraints.gridx=4;
        constraints.gridy=0;
        validationsetpanel.add(validationsetSizeSpinner,constraints);
        constraints.gridx=0;
        constraints.gridy=1;
        validationsetpanel.add(new JLabel("Subset "),constraints);
        constraints.gridx=1;
        constraints.gridy=1;
        constraints.gridwidth=1;
        validationsetpanel.add(validationsetSequenceCollectionCombobox,constraints);
        constraints.gridx=3;
        constraints.gridy=1;
        constraints.gridwidth=GridBagConstraints.REMAINDER;
        validationsetpanel.add(validationsetFilterDuplicates,constraints);
        
        panel.add(trainingsetpanel);
        panel.add(validationsetpanel);
        return panel;
    }

   /** Sets up the training panel (initially empty) */
   private JPanel setupTrainingPanel() {
        JPanel panel=new JPanel(new BorderLayout());
        JPanel leftPanel=new JPanel(new BorderLayout());
        JPanel rightPanel=new JPanel();
        BoxLayout layout2=new BoxLayout(rightPanel, BoxLayout.Y_AXIS);
        rightPanel.setLayout(layout2);
        DefaultTableModel model=new DefaultTableModel(new String[]{"Classifier","Status","Training","Validation"},0);
        classifiersTable=new JTable(model) {
               @Override
               public boolean isCellEditable(int row, int col) {
                   return false;
               }
        };
//        classifiersTable.setGridColor(Color.gray); // It seems grids does not work in Nimbus anyway
//        classifiersTable.setShowGrid(true);
//        classifiersTable.setShowVerticalLines(true);
        classifiersTable.setRowSelectionAllowed(false);
        classifiersTable.setColumnSelectionAllowed(false);
        classifiersTable.getColumn("Status").setCellRenderer(new StatusRenderer());
//        leftPanel.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createEmptyBorder(5, 5, 5, 25),
//                BorderFactory.createLoweredBevelBorder()
//         ));
        samplingStrategyCombobox=new JComboBox(Classifier.SAMPLING_STRATEGY);
        JPanel samplingPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        samplingPanel.add(new JLabel("Sampling  "));
        samplingPanel.add(samplingStrategyCombobox);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 25));
        leftPanel.add(new JScrollPane(classifiersTable));
        leftPanel.add(samplingPanel,BorderLayout.SOUTH);
        Dimension linechartDimension=new Dimension(280,90);
        linechartpanel=new LineChartPanel(linechartDimension.width+10, linechartDimension.height+10, 5f);
        //linechartpanel.setCurves(new double[]{20,30,70,80,90}, new double[]{60,50,30,10,6});
        linechartpanel.setMinimumSize(linechartDimension);
        linechartpanel.setMaximumSize(linechartDimension);
        linechartpanel.setPreferredSize(linechartDimension);
        linechartpanel.setBorder(BorderFactory.createEtchedBorder());
        Dimension piechartDimension=new Dimension(280,130);
        piechartpanel=new PieChartPanel(piechartDimension.width+10, piechartDimension.height+10);
        piechartpanel.setBorder(BorderFactory.createEtchedBorder());
        piechartpanel.setMinimumSize(piechartDimension);
        piechartpanel.setMaximumSize(piechartDimension);
        piechartpanel.setPreferredSize(piechartDimension);
        JPanel wrapLinechartPanel=new JPanel(new FlowLayout());
        wrapLinechartPanel.add(linechartpanel);
        wrapLinechartPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,5));
        JPanel wrapPiechartPanel=new JPanel(new FlowLayout());
        wrapPiechartPanel.add(piechartpanel);
        wrapLinechartPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,5));
        JPanel ipanel1=new JPanel(new FlowLayout(FlowLayout.CENTER));
        ipanel1.add(new JLabel("Training progress"));
        JPanel ipanel2=new JPanel(new FlowLayout(FlowLayout.CENTER));
        ipanel2.add(new JLabel("Result"));
        rightPanel.add(ipanel1);
        rightPanel.add(wrapLinechartPanel);
        rightPanel.add(ipanel2);
        rightPanel.add(wrapPiechartPanel);
        JPanel paddingPanel=new JPanel(new BorderLayout());
        paddingPanel.setPreferredSize(new Dimension(100,2));
        rightPanel.add(paddingPanel);
        panel.add(leftPanel,BorderLayout.CENTER);
        panel.add(rightPanel,BorderLayout.EAST);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5,5,1,5),
                BorderFactory.createEtchedBorder()
        ));
        return panel;
    }


      /** Sets up the panel allowing the user to save the classifier */
   private JPanel setupSaveClassifierPanel() {
        JPanel panel=new JPanel(new BorderLayout());
        panel.add(new JLabel("Training finished"));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5,5,1,5),
                BorderFactory.createEtchedBorder()
        ));
        return panel;
    }


   /** Shows a dialog that lets the user change the parameter settings for a classifier */
   private void showClassifiersPropertiesPrompt(Classifier classifier) {
        ParameterSettings parameterSettings=classifierParameterSettings.get(classifier.getName());
        if (parameterSettings==null) parameterSettings=new ParameterSettings();
        ParametersDialog dialog=new ParametersDialog(gui, "Set parameters for "+classifier.getName(), classifier.getParameters(), parameterSettings);
        int x=gui.getGUIFrame().getWidth()/2-dialog.getWidth()/2; if (x<0) x=0;
        int y=gui.getGUIFrame().getHeight()/2-dialog.getHeight()/2; if (y<0) y=0;
        dialog.setLocation(x,y);
        parameterSettings=dialog.getParameterSettings();
        classifierParameterSettings.put(classifier.getName(),parameterSettings);
        dialog.setVisible(true);
        dialog.dispose();
   }

   /** This method will return false if the training and validation set settings    
    *  selected are suspicious (e.g. they refer to the same set?)
    */
   private boolean checkTrainingAndValidationSettings() {
       return true;
   }

   private void setupTrainingDataSetAndShowTrainPanel() {
        trainingDatasetFile=null;
        validationDatasetFile=null;
        if (((String)trainingsetSamplingCombobox.getSelectedItem()).equals(ClassifierDataSet.FROM_FILE)) {
           Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
           JFileChooser fc = gui.getFileChooser(null);// new JFileChooser(gui.getLastUsedDirectory());
           fc.addChoosableFileFilter(new FileNameExtensionFilter("Attribute-relation file format (*.arff)", "arff","ARFF"));
           fc.setDialogTitle("Import training dataset");
           int returnValue=fc.showOpenDialog(this);
           if (returnValue==JFileChooser.APPROVE_OPTION) {
               trainingDatasetFile=fc.getSelectedFile();
               gui.setLastUsedDirectory(trainingDatasetFile.getParentFile());
           }
        }
         if (((String)validationsetSamplingCombobox.getSelectedItem()).equals(ClassifierDataSet.FROM_FILE)) {
           Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
           JFileChooser fc = gui.getFileChooser(null);// new JFileChooser(gui.getLastUsedDirectory());
           fc.addChoosableFileFilter(new FileNameExtensionFilter("Attribute-relation file format (*.arff)", "arff","ARFF"));
           fc.setDialogTitle("Import validation dataset");
           int returnValue=fc.showOpenDialog(this);
           if (returnValue==JFileChooser.APPROVE_OPTION) {
               validationDatasetFile=fc.getSelectedFile();
               gui.setLastUsedDirectory(validationDatasetFile.getParentFile());
           }
        }
        Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
        nextButton.setEnabled(false);
        SwingWorker<Boolean, Void> worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                isProcessing=true;
                try {
                   int trainingSetSize=((Integer)trainingsetSizeSpinner.getValue());
                   int validationSetSize=((Integer)validationsetSizeSpinner.getValue());
                   boolean filterDuplicatesTraining=trainingsetFilterDuplicates.isSelected();
                   boolean filterDuplicatesValidation=validationsetFilterDuplicates.isSelected();
                   String trainingSamplingStrategy=(String)trainingsetSamplingCombobox.getSelectedItem();
                   String trainingSubset=(String)trainingsetSequenceCollectionCombobox.getSelectedItem();
                   String validationSamplingStrategy=(String)validationsetSamplingCombobox.getSelectedItem();
                   String validationSubset=(String)validationsetSequenceCollectionCombobox.getSelectedItem();
                   Data targetFeature=engine.getDataItem(targetFeatureCombobox.getSelectedItem().toString());
                   Object[] selectedFeatures=featureslist.getSelectedValues();
                   ArrayList<FeatureDataset>features=new ArrayList<FeatureDataset>(selectedFeatures.length);
                   for (Object fname:selectedFeatures) {
                       Data fdata=engine.getDataItem(fname.toString());
                       features.add((FeatureDataset)fdata);
                   }
                   trainingSet=null; // just to release memory if necessary
                   validationSet=null;
                   ClassifierDataSet[] datasets=ClassifierDataSet.setupDatasets(trainingSamplingStrategy, trainingSubset, trainingSetSize, filterDuplicatesTraining, filterDuplicatesValidation, validationSamplingStrategy, validationSubset, validationSetSize, features, (RegionDataset)targetFeature, trainingDatasetFile, validationDatasetFile, isInterrupted, progressbar, engine, null);
                   if (datasets==null || datasets[0]==null || (datasets[1]==null && !validationSamplingStrategy.equals(ClassifierDataSet.CROSSVALIDATION))) {
                        throw new ExecutionError("None selected");
                   }
                   else {
                      trainingSet=datasets[0];
                      validationSet=datasets[1];
                   }
                } 
                catch (Exception e) {ex=e;}
                isProcessing=false;
                return Boolean.TRUE;
            }
            
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex==null) {
                  progressbar.setValue(progressbar.getMaximum());
                  progressbar.repaint();
                  int positiveTraining=trainingSet.getNumberOfExamplesInClass(Classifier.CLASS_POSITIVE);
                  int negativeTraining=trainingSet.size()-positiveTraining;
                  int positiveValidation=validationSet.getNumberOfExamplesInClass(Classifier.CLASS_POSITIVE);
                  int negativeValidation=validationSet.size()-positiveValidation;
                  String message="<html><b>Training set</b>: "+positiveTraining+" positive, "+negativeTraining+" negative<br><b>Validation set</b>: "+positiveValidation+" positive, "+negativeValidation+" negative<br><br>Are you satisfied with these datasets?</html>";
                  Object[] options=new Object[]{"Yes","No","Save"};
                  int selection=JOptionPane.showOptionDialog(Prompt_PriorsGenerator.this, message,"Dataset sampling completed", JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,options,options[0]);
                  progressbar.setValue(0);
                  progressbar.repaint();
                  if (selection==1) {
                      nextButton.setEnabled(true);
                      return;
                  }
                  if (selection==2) { // save datasets
                        File file=null;
                        File parentDir=gui.getLastUsedDirectory();
                        final JFileChooser fc = gui.getFileChooser(parentDir);// new JFileChooser(gui.getLastUsedDirectory());
                        fc.setDialogTitle("Save training dataset");
                        fc.addChoosableFileFilter(new FileNameExtensionFilter("Attribute-relation file format (*.arff)", "arff","ARFF"));
                        File preselected=MotifLabEngine.getFile(parentDir,getDataItemName()+"_train.arff");
                        fc.setSelectedFile(preselected);                        
                        Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        int returnValue=fc.showSaveDialog(Prompt_PriorsGenerator.this);
                        Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
                        if (returnValue==JFileChooser.APPROVE_OPTION) {
                            file=fc.getSelectedFile();
                            gui.setLastUsedDirectory(file.getParentFile());
                            saveClassifierDataset(trainingSet,file);
                            // note that save validation is nested in the if-block above. If user cancels the training set save-dialog, validation set will not be saved either
                            fc.setDialogTitle("Save validation dataset");
                            File preselectedTest=MotifLabEngine.getFile(parentDir,getDataItemName()+"_test.arff");
                            fc.setSelectedFile(preselectedTest);                             
                            Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            returnValue=fc.showSaveDialog(Prompt_PriorsGenerator.this);
                            Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
                            if (returnValue==JFileChooser.APPROVE_OPTION) {
                                file=fc.getSelectedFile();
                                gui.setLastUsedDirectory(file.getParentFile());
                                saveClassifierDataset(validationSet,file);
                            }
                        } // end approved training set save
                  }
                  Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
                  int currentsize=((DefaultTableModel)classifiersTable.getModel()).getRowCount();
                  if (currentsize>0) { // remove current contents
                      for (int i=0;i<currentsize;i++) ((DefaultTableModel)classifiersTable.getModel()).removeRow(0);
                  }
                  // setup and show training panel. Populate the table with the selected classifiers
                   for (int i=0;i<classifierslistmodel.getSize();i++) {
                       Object[] rowData=new Object[4];
                       rowData[0]=((Classifier)classifierslistmodel.getElementAt(i)).getName();
                       rowData[1]="Untrained";
                       rowData[2]=null;
                       rowData[3]=null;
                       ((DefaultTableModel)classifiersTable.getModel()).addRow(rowData);
                   }
                   if (classifierslistmodel.getSize()>1) {
                       samplingStrategyCombobox.setSelectedItem(Classifier.SAMPLING_STRATEGY[1]);
                       samplingStrategyCombobox.setEnabled(true);
                       linechartpanel.resetNumberofXPoints(classifierslistmodel.getSize());
                   } else {
                       samplingStrategyCombobox.setSelectedItem(Classifier.SAMPLING_STRATEGY[0]);
                       samplingStrategyCombobox.setEnabled(false);
                       linechartpanel.resetNumberofXPoints(5);
                   }
                   piechartpanel.reset();
                   currentPanel++;
                   showPanel(currentPanel);
                } else {
                   if (ex instanceof ExecutionError && ex.getMessage().equals("None selected")) {}
                   else {
                       if (ex instanceof ExecutionError) JOptionPane.showMessageDialog(Prompt_PriorsGenerator.this,  ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                       else if (!(ex instanceof InterruptedException)) JOptionPane.showMessageDialog(Prompt_PriorsGenerator.this,  ex.getClass().getSimpleName()+":"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                       //if (!(ex instanceof InterruptedException)) ex.printStackTrace(System.err);
                   }
                   progressbar.setValue(0);
                   progressbar.repaint();
                   nextButton.setEnabled(true);
                }
            }
        }; // end of SwingWorker class
        worker.execute();
   }


   private void resetTraining() {
       classifier=null;
       int numberofClassifiers=classifierslistmodel.getSize();
       piechartpanel.reset();
       for (int i=0;i<numberofClassifiers;i++) {
           classifiersTable.setValueAt("Untrained", i, 1);
           classifiersTable.setValueAt(null, i, 2);
           classifiersTable.setValueAt(null, i, 3);
       }
       repaintTableOnEDT();
       if (numberofClassifiers>1) {
            linechartpanel.resetNumberofXPoints(numberofClassifiers);
       } else { // just a single classifier
            if (classifier instanceof FeedForwardNeuralNet) linechartpanel.resetNumberofXPoints(((FeedForwardNeuralNet)classifier).getNumberOfEpochs());
            else linechartpanel.resetNumberofXPoints(100);
       }   
       piechartpanel.repaint();
       linechartpanel.repaint();
   }

   /** This is called when pressing the nextButton (or "Train") in the Train-panel and starts training the classifiers */
   private void trainClassifiers() {
        progressbar.setMaximum(100);
        progressbar.setValue(0);progressbar.repaint(); // this is just to show a small progress to say that something is going on
        nextButton.setEnabled(false);
        SwingWorker<Boolean, Void> worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
               isProcessing=true;
               trainingSet.shuffle(); // why not...
               int samplingStrategy=Classifier.NONE;
               if (samplingStrategyCombobox.isEnabled()) {
                   String strategy=(String)samplingStrategyCombobox.getSelectedItem();
                   samplingStrategy=Classifier.getSamplingStrategyForName(strategy);
               }
               try {
                  int progressmax=initializeClassifiers(samplingStrategy);
                  linechartpanel.resetNumberofXPoints(progressmax);
                  piechartpanel.reset();
                  progressbar.setMaximum(progressmax);
                  trainClassifiersNormal();
               } // end try
               catch (Exception e) {ex=e;}
               isProcessing=false;
               return Boolean.TRUE;
            } // end doInBackground()

            @Override
            public void done() { // this method is invoked on the EDT!
                Prompt_PriorsGenerator.this.setCursor(Cursor.getDefaultCursor());
                if (ex==null) {
                   progressbar.setValue(0);
                   progressbar.repaint();
                   nextButton.setText("Save");
                   nextButton.setEnabled(true);
                } else {
                   if (!(ex instanceof InterruptedException)) {
                       if (ex instanceof ExecutionError && ex.getCause()!=null) {
                           Throwable cause=((ExecutionError)ex).getCause();
                           //cause.printStackTrace(System.err);
                           JOptionPane.showMessageDialog(Prompt_PriorsGenerator.this, cause.getClass().getSimpleName()+":"+cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                       }
                       else JOptionPane.showMessageDialog(Prompt_PriorsGenerator.this, ex.getClass().getSimpleName()+":"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                       ex.printStackTrace(System.err);
                   }
                   progressbar.setValue(0);
                   progressbar.repaint();
                   nextButton.setEnabled(true);
                }
            }
        }; // end of SwingWorker class
        Prompt_PriorsGenerator.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        worker.execute();
   }

   private void trainClassifiersNormal() throws Exception {
       int numberofClassifiers=classifierslistmodel.getSize();
       // train classifiers
       if (classifier instanceof AdaBoost) {
             for (int i=0;i<numberofClassifiers;i++) {((AdaBoost)classifier).getClassifier(i).addOutputListener(Prompt_PriorsGenerator.this);}
             classifier.addOutputListener(Prompt_PriorsGenerator.this);
             classifier.train(trainingSet, validationSet,null);
             classifier.removeOutputListener(Prompt_PriorsGenerator.this);
             for (int i=0;i<numberofClassifiers;i++) {((AdaBoost)classifier).getClassifier(i).removeOutputListener(Prompt_PriorsGenerator.this);}
       } else {
             classifiersTable.setValueAt("Training", 0, 1);
             repaintTableOnEDT();
             classifier.addOutputListener(Prompt_PriorsGenerator.this);
             classifier.train(trainingSet, validationSet,null);
             classifier.removeOutputListener(Prompt_PriorsGenerator.this);
             classifiersTable.setValueAt("Trained", 0, 1);
             repaintTableOnEDT();
       }      
   }

   /**
    * Initializer the classifiers based on selected settings
    * (And also resets the classifier-table in the training panel)
    * @param samplingStrategy
    * @return The maximum value to use for a progressbar (or points in a graph). The meaning of this value varies depending on selected settings
    * @throws Exception
    */
   private int initializeClassifiers(int samplingStrategy) throws Exception {
       classifier=null;
       int numberofClassifiers=classifierslistmodel.getSize();
       piechartpanel.reset();
       for (int i=0;i<numberofClassifiers;i++) {
           classifiersTable.setValueAt("Untrained", i, 1);
           classifiersTable.setValueAt(null, i, 2);
           classifiersTable.setValueAt(null, i, 3);
       }
       repaintTableOnEDT();
       // initialize classifiers
       if (numberofClassifiers>1) {
            classifier=new AdaBoost();
            classifier.setName("AdaBoost");
            for (Object o:classifierslistmodel.toArray()) {
                Classifier componentclassifier=(Classifier)o;
                ParameterSettings settings=classifierParameterSettings.get(componentclassifier.getName());
                if (componentclassifier instanceof FeedForwardNeuralNet) settings=addInputAndOutputNodesToNeuralNetworkTopology(componentclassifier, settings); // this is a "hack" to add the number of input and output nodes to the "Topology" setting
                componentclassifier.initializeFromParameters(settings, engine);
                componentclassifier.setSamplingStrategy(samplingStrategy);
                componentclassifier.setUseWeightedExamples(true);
                componentclassifier.setAbort(false); // reset just in case...
                ((AdaBoost)classifier).addClassifier(componentclassifier);
            }
            return numberofClassifiers;
       } else { // just a single classifier
            classifier=(Classifier)classifierslistmodel.get(0);
            classifier.setAbort(false); // reset just in case...
            ParameterSettings settings=classifierParameterSettings.get(classifier.getName());
            if (classifier instanceof FeedForwardNeuralNet) settings=addInputAndOutputNodesToNeuralNetworkTopology(classifier, settings); // this is a "hack" to add the number of input and output nodes to the "Topology" setting
            classifier.initializeFromParameters(settings, engine);
            classifier.setSamplingStrategy(Classifier.NONE);
            int progressmax=(classifier instanceof FeedForwardNeuralNet)?((FeedForwardNeuralNet)classifier).getNumberOfEpochs():100;
            return progressmax;
       }
   }

   private void repaintTableOnEDT() {
       Runnable runner=new Runnable() {public void run() { classifiersTable.repaint();} };
       SwingUtilities.invokeLater(runner);
   }

   private ParameterSettings addInputAndOutputNodesToNeuralNetworkTopology(Classifier classifier, ParameterSettings settings) throws ExecutionError {
        if (settings==null) settings=new ParameterSettings();
        String topology=(String)settings.getResolvedParameter("Topology", classifier.getParameters(), engine);
        int numberofFeatures=trainingSet.getNumberOfAttributes();
        topology=numberofFeatures+","+topology+",1";
        settings.setParameter("UseTopology", topology);
        return settings;
   }


   private class PieChartPanel extends JPanel {
       private BufferedImage image;
       private Graphics2D g2;
       private Graph trainingGraph;
       private Graph validationGraph;
       private int width;
       private int height;
       private Color[] colors=new Color[]{Color.GREEN, lightGREEN,lightRED,Color.RED};
       public PieChartPanel(int width, int height) {
           super();
           this.setBackground(Color.WHITE);
           this.width=width;
           this.height=height;
           image=new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
           g2=image.createGraphics();
           trainingGraph=new Graph(g2, 0, 1, 0, 1, height-40, height-40, 20, 5);
           validationGraph=new Graph(g2, 0, 1, 0, 1, height-40, height-40, 10+width/2, 5);
           g2.setColor(Color.WHITE);
           g2.fillRect(0, 0, width, height);
       }
       @Override
       public void paintComponent(Graphics g) {
             g.drawImage(image, 0, 0, null);
       }

       /** The values should be (in order) TP, TN, FP and FN */
       public void setTrainingResults(double[] values) {
           g2.setColor(Color.WHITE);
           g2.fillRect(0, 0, width/2, height);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           trainingGraph.drawPieChart(values, colors, true);
           g2.setColor(Color.BLACK);
           double correct=(values[0]+values[1])*100/(values[0]+values[1]+values[2]+values[3]); // percentage
           trainingGraph.drawAlignedString("training: "+show1decimal.format(correct)+"%", 30, height-18, 0, 0);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
       }
       /** The values should be (in order) TP, TN, FP and FN */
       public void setValidationResults(double[] values) {
           g2.setColor(Color.WHITE);
           g2.fillRect(width/2, 0, width/2, height);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           validationGraph.drawPieChart(values, colors, true);
           g2.setColor(Color.BLACK);
           double correct=(values[0]+values[1])*100/(values[0]+values[1]+values[2]+values[3]); // percentage
           validationGraph.drawAlignedString("validation: "+show1decimal.format(correct)+"%", 17+width/2, height-18, 0, 0);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
       }

       public void reset() {
           g2.setColor(Color.WHITE);
           g2.fillRect(0, 0, width, height);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           trainingGraph.drawPieChart(new double[]{100}, new Color[]{Color.LIGHT_GRAY}, true);
           validationGraph.drawPieChart(new double[]{100}, new Color[]{Color.LIGHT_GRAY}, true);
           trainingGraph.drawAlignedString("training:", 30, height-18, 0, 0);
           validationGraph.drawAlignedString("validation:", 17+width/2, height-18, 0, 0);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
       }

   }

  private class LineChartPanel extends JPanel {
       private BufferedImage image;
       private Graphics2D g2;
       private Graph graph;
       private int width;
       private int height;
       private double[] trainingValues=null;
       private double[] validationValues=null;
       private double[] xvalues=null;
       private int currentX=-1;

       public LineChartPanel(int width, int height, double maxX) {
           super();
           this.setBackground(Color.WHITE);
           this.width=width;
           this.height=height;
           image=new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
           g2=image.createGraphics();
           graph=new Graph(g2, 0, maxX, 0, 100, width-15, height-15, 2, 2);
           updateCurves();
       }
       @Override
       public void paintComponent(Graphics g) {
             g.drawImage(image, 0, 0, null);
       }

       public void addResult(double trainingValue, double validationValue) {
           currentX++;
           if (currentX>=xvalues.length) return;
           for (int i=currentX;i<xvalues.length;i++) { // pad the rest of the array with the same value
               trainingValues[i]=trainingValue;
               validationValues[i]=validationValue;
               xvalues[i]=currentX;
           }
           updateCurves();
       }

       /** Resets the number of points in this graph*/
       public void resetNumberofXPoints(int points) {
           graph.setLimits(0, (double)points-1, 0, 100);
           trainingValues=new double[points];
           validationValues=new double[points];
           xvalues=new double[points];
           currentX=-1;
           updateCurves();
       }

       /** updates the curves for training and validation*/
       private void updateCurves() {
           g2.setColor(Color.WHITE);
           g2.fillRect(0, 0, width, height);
           g2.setColor(Color.BLACK);
           graph.drawAxes(Graph.BOX, Graph.DOTTED, false, false, false, false);
           if (xvalues==null) return;
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           g2.setColor(Color.BLUE);
           graph.drawCurve(xvalues, trainingValues);
           graph.drawAlignedString("training", 80, height-18, 0, 0);
           g2.setColor(Color.RED);
           graph.drawCurve(xvalues, validationValues);
           graph.drawAlignedString("validation", 140, height-18, 0, 0);
           g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
       }


   }

    @Override
    public void start(ClassifierOutput info) {
        round(info,0);
    }

    @Override
    public void finished(ClassifierOutput info) {
        round(info,2);
    }
    @Override
    public void round(ClassifierOutput info) {
        round(info,1);
    }

    public void round(final ClassifierOutput info, final int stage) {
        final String classifierName=(String)info.get(Classifier.CLASSIFIER_NAME);
        final String classifierType=(String)info.get(Classifier.CLASSIFIER_TYPE);
        if (classifierType==null) return;
        //System.err.println("["+classifierName+"]{"+stage+"}  "+info.getOutputText());
        Runnable runner=new Runnable() {
            @Override
            public void run() {
                if (classifier instanceof AdaBoost) {
                      if (classifierType.equals("AdaBoost")) { // the output comes from AdaBoost itself
                            if (stage==0) {
                                linechartpanel.resetNumberofXPoints(((AdaBoost)classifier).getNumberOfClassifiers());
                                piechartpanel.reset();
                            }
                            if (info.get(Classifier.TRAINING_CORRECT)!=null) {
                                double trainingCorrect=((Integer)info.get(Classifier.TRAINING_CORRECT)).doubleValue();
                                double trainingMistakes=((Integer)info.get(Classifier.TRAINING_MISTAKES)).doubleValue();
                                double validationCorrect=((Integer)info.get(Classifier.VALIDATION_CORRECT)).doubleValue();
                                double validationMistakes=((Integer)info.get(Classifier.VALIDATION_MISTAKES)).doubleValue();
                                double trainingTP=((Integer)info.get(Classifier.TRAINING_TP)).doubleValue();
                                double trainingFP=((Integer)info.get(Classifier.TRAINING_FP)).doubleValue();
                                double trainingTN=((Integer)info.get(Classifier.TRAINING_TN)).doubleValue();
                                double trainingFN=((Integer)info.get(Classifier.TRAINING_FN)).doubleValue();
                                double validationTP=((Integer)info.get(Classifier.VALIDATION_TP)).doubleValue();
                                double validationFP=((Integer)info.get(Classifier.VALIDATION_FP)).doubleValue();
                                double validationTN=((Integer)info.get(Classifier.VALIDATION_TN)).doubleValue();
                                double validationFN=((Integer)info.get(Classifier.VALIDATION_FN)).doubleValue();
                                linechartpanel.addResult(trainingCorrect/(trainingCorrect+trainingMistakes)*100f, validationCorrect/(validationCorrect+validationMistakes)*100f);
                                linechartpanel.repaint();
                                piechartpanel.setTrainingResults(new double[]{trainingTP,trainingTN,trainingFP,trainingFN});
                                piechartpanel.setValidationResults(new double[]{validationTP,validationTN,validationFP,validationFN});
                                piechartpanel.repaint();
                            }
                      } else { // the output is from one of AdaBoost's component classifiers
                            int index=-1;
                            for (int i=0;i<classifiersTable.getRowCount();i++) {if (((String)classifiersTable.getValueAt(i, 0)).equals(classifierName)) {index=i;break;}}
                            if (stage==0) {classifiersTable.setValueAt("Training",index, 1);classifiersTable.repaint();}
                            else if (stage==2) {
                                classifiersTable.setValueAt("Trained",index, 1);
                                double trainingCorrect=((Integer)info.get(Classifier.TRAINING_CORRECT)).doubleValue();
                                double trainingMistakes=((Integer)info.get(Classifier.TRAINING_MISTAKES)).doubleValue();
                                double validationCorrect=((Integer)info.get(Classifier.VALIDATION_CORRECT)).doubleValue();
                                double validationMistakes=((Integer)info.get(Classifier.VALIDATION_MISTAKES)).doubleValue();
                                double trainingPerformance=trainingCorrect/(trainingCorrect+trainingMistakes)*100f;
                                double validationPerformance=validationCorrect/(validationCorrect+validationMistakes)*100f;
                                classifiersTable.setValueAt("Trained",index, 1);
                                classifiersTable.setValueAt(show1decimal.format(trainingPerformance)+"%",index, 2);
                                classifiersTable.setValueAt(show1decimal.format(validationPerformance)+"%",index, 3);
                                classifiersTable.repaint();
                                progressbar.setValue(index+1);
                                progressbar.repaint();
                            }
                      }
                } else { // single classifier
                    if (stage==0) {classifiersTable.setValueAt("Training",0, 1); }
                    else if (stage==2) {classifiersTable.setValueAt("Trained",0, 1);}
                    if (info.get(Classifier.TRAINING_CORRECT)!=null) {
                        double trainingCorrect=((Integer)info.get(Classifier.TRAINING_CORRECT)).doubleValue();
                        double trainingMistakes=((Integer)info.get(Classifier.TRAINING_MISTAKES)).doubleValue();
                        double validationCorrect=((Integer)info.get(Classifier.VALIDATION_CORRECT)).doubleValue();
                        double validationMistakes=((Integer)info.get(Classifier.VALIDATION_MISTAKES)).doubleValue();
                        double trainingPerformance=trainingCorrect/(trainingCorrect+trainingMistakes)*100f;
                        double validationPerformance=validationCorrect/(validationCorrect+validationMistakes)*100f;
                        double trainingTP=((Integer)info.get(Classifier.TRAINING_TP)).doubleValue();
                        double trainingFP=((Integer)info.get(Classifier.TRAINING_FP)).doubleValue();
                        double trainingTN=((Integer)info.get(Classifier.TRAINING_TN)).doubleValue();
                        double trainingFN=((Integer)info.get(Classifier.TRAINING_FN)).doubleValue();
                        double validationTP=((Integer)info.get(Classifier.VALIDATION_TP)).doubleValue();
                        double validationFP=((Integer)info.get(Classifier.VALIDATION_FP)).doubleValue();
                        double validationTN=((Integer)info.get(Classifier.VALIDATION_TN)).doubleValue();
                        double validationFN=((Integer)info.get(Classifier.VALIDATION_FN)).doubleValue();
                        piechartpanel.setTrainingResults(new double[]{trainingTP,trainingTN,trainingFP,trainingFN});
                        piechartpanel.setValidationResults(new double[]{validationTP,validationTN,validationFP,validationFN});
                        piechartpanel.repaint();
                        if (classifier instanceof FeedForwardNeuralNet) {
                            linechartpanel.addResult(trainingCorrect/(trainingCorrect+trainingMistakes)*100f, validationCorrect/(validationCorrect+validationMistakes)*100f);
                            linechartpanel.repaint();
                            Object epoch=info.get(Classifier.EPOCH);
                            if (epoch instanceof Integer) progressbar.setValue((Integer)epoch);
                            progressbar.repaint();
                        }
                        if (stage==2) {
                           classifiersTable.setValueAt(show1decimal.format(trainingPerformance)+"%",0, 2);
                           classifiersTable.setValueAt(show1decimal.format(validationPerformance)+"%",0, 3);
                           progressbar.setValue(progressbar.getMaximum());
                           progressbar.repaint();
                        }
                    }
                    classifiersTable.repaint();
                }
            }
        };
        SwingUtilities.invokeLater(runner);
    }




    private class StatusRenderer extends DefaultTableCellRenderer {
        private Color darkGreen=new Color(0,220,0);
        private Color darkYellow=Color.ORANGE;

        @Override
        public void setValue(Object value) {
           super.setValue(value);
           if (value==null) setForeground(java.awt.Color.BLACK);
           else if (value.equals("Untrained")) setForeground(java.awt.Color.RED);
           else if (value.equals("Training")) setForeground(darkYellow);
           else if (value.equals("Trained")) setForeground(darkGreen);
           else setForeground(java.awt.Color.BLACK);
       }
    } // end class StatusRenderer


    private class SampleExampleClass extends JPanel {
        private BufferedImage image;
        private Graphics2D graphics;
        private int width=360;
        private int height=50;
        private int offsetX=20;
        private int offsetY=20;
        private int trackWidth=320;
        private int trackHeight=16;
        private int sampleTickHeight=6;


        public SampleExampleClass(String stragegy) {
            this.setMinimumSize(new Dimension(width, height));
            this.setPreferredSize(new Dimension(width, height));
            this.setBackground(new Color(255,255,180));
            this.setBorder(BorderFactory.createLoweredBevelBorder());
            image=new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
            graphics=image.createGraphics();
            setStrategy(stragegy);
        }

        @Override
        public void paintComponent(Graphics g) {
            g.drawImage(image, 0, 0, null);
        }

        public final void setStrategy(String stragegy) {
            graphics.setColor(new Color(255,255,180));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(offsetX, offsetY, trackWidth, trackHeight);
            graphics.setColor(Color.RED);
            graphics.fillRect(offsetX+50, offsetY, 40, trackHeight);
            graphics.fillRect(offsetX+150, offsetY, 20, trackHeight);
            graphics.fillRect(offsetX+210, offsetY, 30, trackHeight);
            graphics.setColor(Color.BLACK);
            graphics.drawRect(offsetX, offsetY, trackWidth, trackHeight);
            graphics.setColor(Color.DARK_GRAY);
            if (stragegy.equals(ClassifierDataSet.TOTALLY_RANDOM) || stragegy.equals(ClassifierDataSet.ALL_POSITIVE_RANDOM_NEGATIVE)) {
                int samples=(stragegy.equals(ClassifierDataSet.TOTALLY_RANDOM))?64:96;
                for (int i=0;i<samples;i++) {
                    int randompoint=(int)(Math.random()*(trackWidth-2))+1;
                    graphics.drawLine(randompoint+offsetX, offsetY+trackHeight+1, randompoint+offsetX, offsetY+trackHeight+sampleTickHeight);
                }
            }
            if (stragegy.equals(ClassifierDataSet.ALL_POSITIVE_EVENLY_SPACED_NEGATIVE) || stragegy.equals(ClassifierDataSet.ALL_POSITIVE_RANDOM_NEGATIVE)) {
               graphics.fillRect(offsetX+50, offsetY+trackHeight+1, 40, sampleTickHeight);
               graphics.fillRect(offsetX+150, offsetY+trackHeight+1, 20, sampleTickHeight);
               graphics.fillRect(offsetX+210, offsetY+trackHeight+1, 30, sampleTickHeight);
            }
            if (stragegy.equals(ClassifierDataSet.EVENLY_SPACED) || stragegy.equals(ClassifierDataSet.ALL_POSITIVE_EVENLY_SPACED_NEGATIVE)) {
                 for (int i=5;i<trackWidth-2;i+=5) {
                    graphics.drawLine(i+offsetX, offsetY+trackHeight+1, i+offsetX, offsetY+trackHeight+sampleTickHeight);
                }
            }
            if (stragegy.equals(ClassifierDataSet.EVENLY_SPACED_WITHIN_CLASS)) {
                for (int i=5;i<trackWidth-2;i+=5) {
                    int x=i+offsetX;
                    if ((x>=offsetX+50 && x<=offsetX+50+40) || (x>=offsetX+150 && x<=offsetX+150+20) || (x>=offsetX+210 && x<=offsetX+210+30)) continue; // skip inside regions
                    graphics.drawLine(x, offsetY+trackHeight+1, x, offsetY+trackHeight+sampleTickHeight);
                }
                 for (int i=4;i<trackWidth-2;i+=2) {
                    int x=i+offsetX;
                    if ((x>=offsetX+50 && x<=offsetX+50+40) || (x>=offsetX+150 && x<=offsetX+150+20) || (x>=offsetX+210 && x<=offsetX+210+30)) {
                        graphics.drawLine(x, offsetY+trackHeight+1, x, offsetY+trackHeight+sampleTickHeight);
                    }
                }
            }
            if (stragegy.equals(ClassifierDataSet.MIDPOINT)) {
               int i=trackWidth/2;
               graphics.drawLine(i+offsetX, offsetY+trackHeight+1, i+offsetX, offsetY+trackHeight+sampleTickHeight);
            }
            if (stragegy.equals(ClassifierDataSet.FULL_SEQUENCE)) {
                graphics.fillRect(offsetX, offsetY+trackHeight+1, trackWidth+1, sampleTickHeight);
            }
            repaint();
        }
    }
    /**
     * Saves the configuration of this PriorsGenerator in XML format to the given file
     */
    private void saveConfigurationToFile(File configurationfile) throws Exception {
        Document document=getConfigurationAsXML();
        TransformerFactory factory=TransformerFactory.newInstance();
        try {
            factory.setAttribute("indent-number", new Integer(3));
        } catch (IllegalArgumentException iae) {}
        Transformer transformer=factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source=new DOMSource(document); 
        OutputStream output=MotifLabEngine.getOutputStreamForFile(configurationfile);
        StreamResult result=new StreamResult(new OutputStreamWriter(new BufferedOutputStream(output),"UTF-8"));
        transformer.transform(source, result);
    }

    /** Returns an XML representation of the configuration of this PriorsGenerator */
    private Document getConfigurationAsXML() throws ParserConfigurationException {
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document document = builder.newDocument();
         Element priorsgeneratorElement=document.createElement("PriorsGenerator");
         //program.setAttribute("name", getName());
         // Features
         Element featuresElement=document.createElement("Features");
         String targetFeature=((RegionDataset)targetFeatureCombobox.getSelectedItem()).getName();
         Element targetFeatureElement=document.createElement("Target");
         targetFeatureElement.setTextContent(targetFeature);
         featuresElement.appendChild(targetFeatureElement);         
         Object[] selectedFeatures=featureslist.getSelectedValues();
         for (int i=0;i<selectedFeatures.length;i++) {
            Element inputFeatureElement=document.createElement("Input");
            inputFeatureElement.setTextContent((String)selectedFeatures[i]);
            featuresElement.appendChild(inputFeatureElement);             
         }
         priorsgeneratorElement.appendChild(featuresElement); 
         // Classifiers
         Element classifierElement=getClassifierConfigElement(classifier,document);
         if (classifier instanceof AdaBoost) {
            String strategy=(String)samplingStrategyCombobox.getSelectedItem();
            classifierElement.setAttribute("samplingStrategy", strategy); 
         }
         priorsgeneratorElement.appendChild(classifierElement);
         // datasets
         int trainingSetSize=((Integer)trainingsetSizeSpinner.getValue());
         int validationSetSize=((Integer)validationsetSizeSpinner.getValue());
         boolean filterDuplicatesTraining=trainingsetFilterDuplicates.isSelected();
         boolean filterDuplicatesValidation=validationsetFilterDuplicates.isSelected();
         String trainingSamplingStrategy=(String)trainingsetSamplingCombobox.getSelectedItem();
         String trainingSubset=(String)trainingsetSequenceCollectionCombobox.getSelectedItem();
         String validationSamplingStrategy=(String)validationsetSamplingCombobox.getSelectedItem();
         String validationSubset=(String)validationsetSequenceCollectionCombobox.getSelectedItem();
         
         Element datasetsElement=document.createElement("Datasets");  
           Element trainingsetElement=document.createElement("Trainingset");  
             trainingsetElement.setAttribute("sampling", trainingSamplingStrategy);
             trainingsetElement.setAttribute("samples", ""+trainingSetSize);
             if (trainingDatasetFile!=null  )trainingsetElement.setAttribute("filename", trainingDatasetFile.getAbsolutePath());
             trainingsetElement.setAttribute("subset", trainingSubset);
             trainingsetElement.setAttribute("remove_duplicates", ""+filterDuplicatesTraining);
           Element validationElement=document.createElement("Validationset"); 
             validationElement.setAttribute("sampling", validationSamplingStrategy);
             validationElement.setAttribute("samples", ""+validationSetSize);
             if (validationDatasetFile!=null ) validationElement.setAttribute("filename", validationDatasetFile.getAbsolutePath());
             validationElement.setAttribute("subset", validationSubset);
             validationElement.setAttribute("remove_duplicates", ""+filterDuplicatesValidation);     
         datasetsElement.appendChild(trainingsetElement);
         datasetsElement.appendChild(validationElement);
         priorsgeneratorElement.appendChild(datasetsElement);

         document.appendChild(priorsgeneratorElement);
         return document;
    }
   
    private Element getClassifierConfigElement(Classifier targetClassifier, Document document) {
         Element classifierElement=document.createElement("Classifier");
         classifierElement.setAttribute("name", targetClassifier.getName());
         classifierElement.setAttribute("type", targetClassifier.getClassifierType());
         Parameter[] classifierParameters=targetClassifier.getParameters();
         ParameterSettings parameterSettings=classifierParameterSettings.get(targetClassifier.getName());
         if (parameterSettings==null) parameterSettings=new ParameterSettings();
         for (Parameter parameter:classifierParameters) {
            String parametername=parameter.getName();        
            try {
                Object value=parameterSettings.getResolvedParameter(parametername, classifierParameters, engine);
                if (value!=null) {
                    Element parameterElement=document.createElement("Parameter");
                    parameterElement.setAttribute("name", parametername);
                    parameterElement.setAttribute("value", value.toString());
                    classifierElement.appendChild(parameterElement);
                }
            } catch (Exception e) {
                engine.logMessage(e.toString());
            }
         }
        if (targetClassifier.getClassifierType().equals("AdaBoost")) {
             // output component Classifiers for AdaBoost
            for (Classifier child:((AdaBoost)targetClassifier).getClassifiers()) {
                Element childElement=getClassifierConfigElement(child, document);
                classifierElement.appendChild(childElement);
            }
         }         
         return classifierElement;
    }
}
