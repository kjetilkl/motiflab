/*
 * GeneExpressionAtlasDialog.java
 *
 * Created on 18.des.2010, 15:45:31
 */

package motiflab.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import motiflab.engine.ExecutionError;


/**
 *
 * @author kjetikl
 */
public class GeneExpressionAtlasDialog extends javax.swing.JDialog {
    public static final int UP_REGULATED=1;
    public static final int DOWN_REGULATED=2;
    public static final int UP_OR_DOWN_REGULATED=3;
    public static final int NON_DIFFERENTIALLY_EXPRESSED=4;

    private Font stepLabelFont=new Font(Font.SANS_SERIF,Font.PLAIN,14);
    private Font stepLabelFontBold=new Font(Font.SANS_SERIF,Font.BOLD,14);
    private String[] supportedOrganisms=new String[]{"Any","Arabidopsis thaliana","Bos taurus","Caenorhabditis elegans","Danio rerio","Drosophila melanogaster","Gallus gallus","Homo sapiens","Mus musculus","Rattus norvegicus","Saccharomyces cerevisiae","Xenopus Laevis"};
    private String[] supportedRegulations=new String[]{"Up-regulated","Down-regulated","Up- or down-regulated"};
    private MotifLabGUI gui;
    private JTable searchGenesTable;
    private GeneTableModel genetablemodel;
    private JTable searchSimilarGenesTable;
    private SimilarGenesTableModel similargenestablemodel;
    private JTable searchExperimentsTable;
    private ExperimentsTableModel experimentstablemodel;
    private AtlasCommunicatorSearchGenes communicator=null;
    private AtlasCommunicatorSearchExperiments communicatorExp=null;
    private AtlasCommunicatorSearchSimilarGenes communicatorSimilarGenes=null;
    private int currentstep=1;


    /** Creates new form GeneExpressionAtlasDialog */
    public GeneExpressionAtlasDialog(MotifLabGUI gui) {
        super(gui.getFrame(), "Search for co-expressed genes", true);
        this.gui=gui;
        initComponents();
       
        DefaultComboBoxModel organismsModel=new DefaultComboBoxModel(supportedOrganisms);
        searchOrganismCombobox.setModel(organismsModel);
        searchOrganismCombobox.setSelectedItem("Homo sapiens");
        DefaultComboBoxModel regulationModel=new DefaultComboBoxModel(supportedRegulations);
        searchRegulationCombobox.setModel(regulationModel);
        searchRegulationCombobox.setSelectedIndex(0);
        setupGenesTable();
        setupExperimentsTable();
        setupSimilarGenesTable();
        Dimension dim=new Dimension(700,500);
        mainPanel.setPreferredSize(dim);
        backButton.setEnabled(false);
        nextButton.setEnabled(false);
        ((CardLayout)mainPanel.getLayout()).show(mainPanel, "step"+currentstep+"Panel");
        setHeader(currentstep);
        pack();
    }


    private void setupGenesTable() {
        searchGenesTable=new JTable();
        genetablemodel=new GeneTableModel();
        searchGenesTable.setModel(genetablemodel);
        searchGenesTable.setAutoCreateRowSorter(true);
        searchGenesTable.getTableHeader().setReorderingAllowed(false);
        TableColumn checkColumn=searchGenesTable.getColumn("");
        checkColumn.setMaxWidth(20);
        checkColumn.setPreferredWidth(20);
        checkColumn.setResizable(false);
        searchGenesScrollPane.setViewportView(searchGenesTable);
        searchGenesTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE) {
                    int[] rows=searchGenesTable.getSelectedRows();
                    int checkedRows=0;
                    for (int row:rows) {
                        if ((Boolean)genetablemodel.getValueAt(searchGenesTable.convertRowIndexToModel(row),0)) checkedRows++;
                    }
                    Boolean doCheck=Boolean.TRUE;
                    if (checkedRows==rows.length) doCheck=Boolean.FALSE;
                    for (int row:rows) {
                        genetablemodel.setValueAt(doCheck,searchGenesTable.convertRowIndexToModel(row),0);
                    }
                }
            }
        });
        searchGeneTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchGenesButton.doClick();
            }
        });
    }

    private void setupExperimentsTable() {
        searchExperimentsTable=new JTable();
        experimentstablemodel=new ExperimentsTableModel();
        searchExperimentsTable.setModel(experimentstablemodel);
        searchExperimentsTable.setAutoCreateRowSorter(true);
        searchExperimentsTable.getTableHeader().setReorderingAllowed(false);
        TableColumn checkColumn=searchExperimentsTable.getColumn("");
        checkColumn.setMaxWidth(20);
        checkColumn.setPreferredWidth(20);
        checkColumn.setResizable(false);
        TableColumn dirColumn=searchExperimentsTable.getColumn("Dir");
        dirColumn.setMaxWidth(20);
        dirColumn.setPreferredWidth(20);
        dirColumn.setResizable(false);
        dirColumn.setCellRenderer(new RegulationRenderer(gui));
        TableColumn expColumn=searchExperimentsTable.getColumn("Experiment");
        expColumn.setCellRenderer(new ExperimentIDRenderer());
        searchExperimentsScrollPane.setViewportView(searchExperimentsTable);
        searchExperimentsTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE) {
                    int[] rows=searchExperimentsTable.getSelectedRows();
                    int checkedRows=0;
                    for (int row:rows) {
                        if ((Boolean)experimentstablemodel.getValueAt(searchExperimentsTable.convertRowIndexToModel(row),0)) checkedRows++;
                    }
                    Boolean doCheck=Boolean.TRUE;
                    if (checkedRows==rows.length) doCheck=Boolean.FALSE;
                    for (int row:rows) {
                        experimentstablemodel.setValueAt(doCheck,searchExperimentsTable.convertRowIndexToModel(row),0);
                    }
                }
            }
        });
        searchConditionTextfield.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchExperimentsButton.doClick();
            }
        });
    }

    private void setupSimilarGenesTable() {
        searchSimilarGenesTable=new JTable();
        similargenestablemodel=new SimilarGenesTableModel();
        searchSimilarGenesTable.setModel(similargenestablemodel);
        searchSimilarGenesTable.setAutoCreateRowSorter(true);
        searchSimilarGenesTable.getTableHeader().setReorderingAllowed(false);
        TableColumn checkColumn=searchSimilarGenesTable.getColumn("");
        checkColumn.setMaxWidth(20);
        checkColumn.setPreferredWidth(20);
        checkColumn.setResizable(false);
        searchSimilarGenesScrollPane.setViewportView(searchSimilarGenesTable);
        searchSimilarGenesTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE) {
                    int[] rows=searchSimilarGenesTable.getSelectedRows();
                    int checkedRows=0;
                    for (int row:rows) {
                        if ((Boolean)searchSimilarGenesTable.getValueAt(searchSimilarGenesTable.convertRowIndexToModel(row),0)) checkedRows++;
                    }
                    Boolean doCheck=Boolean.TRUE;
                    if (checkedRows==rows.length) doCheck=Boolean.FALSE;
                    for (int row:rows) {
                        searchSimilarGenesTable.setValueAt(doCheck,searchSimilarGenesTable.convertRowIndexToModel(row),0);
                    }
                }
            }
        });
    }

    private void setHeader(int step) {
        step1Label.setFont(stepLabelFont);
        step2Label.setFont(stepLabelFont);
        step3Label.setFont(stepLabelFont);
        step1Label.setForeground(Color.GRAY);
        step2Label.setForeground(Color.GRAY);
        step3Label.setForeground(Color.GRAY);        
        if (step==1) {
            step1Label.setForeground(Color.BLACK);
            step1Label.setFont(stepLabelFontBold);
        } else if (step==2) {
            step2Label.setForeground(Color.BLACK);
            step2Label.setFont(stepLabelFontBold);           
        } else if (step==3) {
            step3Label.setForeground(Color.BLACK);
            step3Label.setFont(stepLabelFontBold);           
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
        step1Label = new javax.swing.JLabel();
        step2Label = new javax.swing.JLabel();
        step3Label = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        step1Panel = new javax.swing.JPanel();
        step1ControlsPanel = new javax.swing.JPanel();
        searchGenesLabel = new javax.swing.JLabel();
        searchGeneTextField = new javax.swing.JTextField();
        spacerLabel1 = new javax.swing.JLabel();
        searchOrganismLabel = new javax.swing.JLabel();
        searchOrganismCombobox = new javax.swing.JComboBox();
        spacerLabel2 = new javax.swing.JLabel();
        searchGenesButton = new javax.swing.JButton();
        step1ResultsPanel = new javax.swing.JPanel();
        searchGenesScrollPane = new javax.swing.JScrollPane();
        step1BottomPanel = new javax.swing.JPanel();
        searchGenesMessageLabel = new javax.swing.JLabel();
        step2Panel = new javax.swing.JPanel();
        step2ControlsPanel = new javax.swing.JPanel();
        searchRegulationLabel = new javax.swing.JLabel();
        searchRegulationCombobox = new javax.swing.JComboBox();
        spacerLabel3 = new javax.swing.JLabel();
        searchConditionLabel = new javax.swing.JLabel();
        searchConditionTextfield = new javax.swing.JTextField();
        spacerLabel4 = new javax.swing.JLabel();
        searchExperimentsButton = new javax.swing.JButton();
        step2ResultsPanel = new javax.swing.JPanel();
        searchExperimentsScrollPane = new javax.swing.JScrollPane();
        step2BottomPanel = new javax.swing.JPanel();
        searchExperimentsMessageLabel = new javax.swing.JLabel();
        step3Panel = new javax.swing.JPanel();
        step3ControlsPanel = new javax.swing.JPanel();
        searchSimilarGenesButton = new javax.swing.JButton();
        step3ResultsPanel = new javax.swing.JPanel();
        searchSimilarGenesScrollPane = new javax.swing.JScrollPane();
        step3BottomPanel = new javax.swing.JPanel();
        searchSimilarGenesMessageLabel = new javax.swing.JLabel();
        controlsPanel = new javax.swing.JPanel();
        progressPanel = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        buttonsPanel = new javax.swing.JPanel();
        backButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        topPanel.setName("topPanel"); // NOI18N
        topPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 30, 7));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(GeneExpressionAtlasDialog.class);
        step1Label.setText(resourceMap.getString("step1Label.text")); // NOI18N
        step1Label.setName("step1Label"); // NOI18N
        topPanel.add(step1Label);

        step2Label.setText(resourceMap.getString("step2Label.text")); // NOI18N
        step2Label.setName("step2Label"); // NOI18N
        topPanel.add(step2Label);

        step3Label.setText(resourceMap.getString("step3Label.text")); // NOI18N
        step3Label.setName("step3Label"); // NOI18N
        topPanel.add(step3Label);

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 6, 6), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.CardLayout());

        step1Panel.setName("step1Panel"); // NOI18N
        step1Panel.setLayout(new java.awt.BorderLayout());

        step1ControlsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));
        step1ControlsPanel.setName("step1ControlsPanel"); // NOI18N
        step1ControlsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 5));

        searchGenesLabel.setText(resourceMap.getString("searchGenesLabel.text")); // NOI18N
        searchGenesLabel.setName("searchGenesLabel"); // NOI18N
        step1ControlsPanel.add(searchGenesLabel);

        searchGeneTextField.setColumns(10);
        searchGeneTextField.setText(resourceMap.getString("searchGeneTextField.text")); // NOI18N
        searchGeneTextField.setName("searchGeneTextField"); // NOI18N
        step1ControlsPanel.add(searchGeneTextField);

        spacerLabel1.setText(resourceMap.getString("spacerLabel1.text")); // NOI18N
        spacerLabel1.setName("spacerLabel1"); // NOI18N
        step1ControlsPanel.add(spacerLabel1);

        searchOrganismLabel.setText(resourceMap.getString("searchOrganismLabel.text")); // NOI18N
        searchOrganismLabel.setName("searchOrganismLabel"); // NOI18N
        step1ControlsPanel.add(searchOrganismLabel);

        searchOrganismCombobox.setName("searchOrganismCombobox"); // NOI18N
        step1ControlsPanel.add(searchOrganismCombobox);

        spacerLabel2.setText(resourceMap.getString("spacerLabel2.text")); // NOI18N
        spacerLabel2.setName("spacerLabel2"); // NOI18N
        step1ControlsPanel.add(spacerLabel2);

        searchGenesButton.setText(resourceMap.getString("searchGenesButton.text")); // NOI18N
        searchGenesButton.setMaximumSize(new java.awt.Dimension(75, 27));
        searchGenesButton.setMinimumSize(new java.awt.Dimension(75, 27));
        searchGenesButton.setName("searchGenesButton"); // NOI18N
        searchGenesButton.setOpaque(false);
        searchGenesButton.setPreferredSize(new java.awt.Dimension(75, 27));
        searchGenesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonPressed(evt);
            }
        });
        step1ControlsPanel.add(searchGenesButton);

        step1Panel.add(step1ControlsPanel, java.awt.BorderLayout.PAGE_START);

        step1ResultsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 0, 8));
        step1ResultsPanel.setName("step1ResultsPanel"); // NOI18N
        step1ResultsPanel.setLayout(new java.awt.BorderLayout());

        searchGenesScrollPane.setName("searchGenesScrollPane"); // NOI18N
        step1ResultsPanel.add(searchGenesScrollPane, java.awt.BorderLayout.CENTER);

        step1Panel.add(step1ResultsPanel, java.awt.BorderLayout.CENTER);

        step1BottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 6, 6));
        step1BottomPanel.setName("step1BottomPanel"); // NOI18N
        step1BottomPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        searchGenesMessageLabel.setText(resourceMap.getString("searchGenesMessageLabel.text")); // NOI18N
        searchGenesMessageLabel.setName("searchGenesMessageLabel"); // NOI18N
        step1BottomPanel.add(searchGenesMessageLabel);

        step1Panel.add(step1BottomPanel, java.awt.BorderLayout.PAGE_END);

        mainPanel.add(step1Panel, "step1Panel");

        step2Panel.setName("step2Panel"); // NOI18N
        step2Panel.setLayout(new java.awt.BorderLayout());

        step2ControlsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));
        step2ControlsPanel.setName("step2ControlsPanel"); // NOI18N
        step2ControlsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 5));

        searchRegulationLabel.setText(resourceMap.getString("searchRegulationLabel.text")); // NOI18N
        searchRegulationLabel.setName("searchRegulationLabel"); // NOI18N
        step2ControlsPanel.add(searchRegulationLabel);

        searchRegulationCombobox.setName("searchRegulationCombobox"); // NOI18N
        step2ControlsPanel.add(searchRegulationCombobox);

        spacerLabel3.setText(resourceMap.getString("spacerLabel3.text")); // NOI18N
        spacerLabel3.setName("spacerLabel3"); // NOI18N
        step2ControlsPanel.add(spacerLabel3);

        searchConditionLabel.setText(resourceMap.getString("searchConditionLabel.text")); // NOI18N
        searchConditionLabel.setName("searchConditionLabel"); // NOI18N
        step2ControlsPanel.add(searchConditionLabel);

        searchConditionTextfield.setColumns(10);
        searchConditionTextfield.setText(resourceMap.getString("searchConditionTextfield.text")); // NOI18N
        searchConditionTextfield.setName("searchConditionTextfield"); // NOI18N
        step2ControlsPanel.add(searchConditionTextfield);

        spacerLabel4.setText(resourceMap.getString("spacerLabel4.text")); // NOI18N
        spacerLabel4.setName("spacerLabel4"); // NOI18N
        step2ControlsPanel.add(spacerLabel4);

        searchExperimentsButton.setText(resourceMap.getString("searchExperimentsButton.text")); // NOI18N
        searchExperimentsButton.setMaximumSize(new java.awt.Dimension(75, 27));
        searchExperimentsButton.setMinimumSize(new java.awt.Dimension(75, 27));
        searchExperimentsButton.setName("searchExperimentsButton"); // NOI18N
        searchExperimentsButton.setPreferredSize(new java.awt.Dimension(75, 27));
        searchExperimentsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchExperimentsButtonPressed(evt);
            }
        });
        step2ControlsPanel.add(searchExperimentsButton);

        step2Panel.add(step2ControlsPanel, java.awt.BorderLayout.NORTH);

        step2ResultsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 0, 8));
        step2ResultsPanel.setName("step2ResultsPanel"); // NOI18N
        step2ResultsPanel.setLayout(new java.awt.BorderLayout());

        searchExperimentsScrollPane.setName("searchExperimentsScrollPane"); // NOI18N
        step2ResultsPanel.add(searchExperimentsScrollPane, java.awt.BorderLayout.CENTER);

        step2Panel.add(step2ResultsPanel, java.awt.BorderLayout.CENTER);

        step2BottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 6, 6));
        step2BottomPanel.setMinimumSize(new java.awt.Dimension(25, 34));
        step2BottomPanel.setName("step2BottomPanel"); // NOI18N
        step2BottomPanel.setPreferredSize(new java.awt.Dimension(25, 34));
        step2BottomPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        searchExperimentsMessageLabel.setText(resourceMap.getString("searchExperimentsMessageLabel.text")); // NOI18N
        searchExperimentsMessageLabel.setName("searchExperimentsMessageLabel"); // NOI18N
        step2BottomPanel.add(searchExperimentsMessageLabel);

        step2Panel.add(step2BottomPanel, java.awt.BorderLayout.PAGE_END);

        mainPanel.add(step2Panel, "step2Panel");

        step3Panel.setName("step3Panel"); // NOI18N
        step3Panel.setLayout(new java.awt.BorderLayout());

        step3ControlsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 10, 6, 10));
        step3ControlsPanel.setMinimumSize(new java.awt.Dimension(379, 49));
        step3ControlsPanel.setName("step3ControlsPanel"); // NOI18N
        step3ControlsPanel.setPreferredSize(new java.awt.Dimension(379, 49));
        step3ControlsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        searchSimilarGenesButton.setText(resourceMap.getString("searchSimilarGenesButton.text")); // NOI18N
        searchSimilarGenesButton.setMaximumSize(new java.awt.Dimension(75, 27));
        searchSimilarGenesButton.setMinimumSize(new java.awt.Dimension(75, 27));
        searchSimilarGenesButton.setName("searchSimilarGenesButton"); // NOI18N
        searchSimilarGenesButton.setPreferredSize(new java.awt.Dimension(75, 27));
        searchSimilarGenesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchSimilarGenesButtonPressed(evt);
            }
        });
        step3ControlsPanel.add(searchSimilarGenesButton);

        step3Panel.add(step3ControlsPanel, java.awt.BorderLayout.PAGE_START);

        step3ResultsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 0, 8));
        step3ResultsPanel.setName("step3ResultsPanel"); // NOI18N
        step3ResultsPanel.setLayout(new java.awt.BorderLayout());

        searchSimilarGenesScrollPane.setName("searchSimilarGenesScrollPane"); // NOI18N
        step3ResultsPanel.add(searchSimilarGenesScrollPane, java.awt.BorderLayout.CENTER);

        step3Panel.add(step3ResultsPanel, java.awt.BorderLayout.CENTER);

        step3BottomPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 6, 6));
        step3BottomPanel.setMinimumSize(new java.awt.Dimension(34, 34));
        step3BottomPanel.setName("step3BottomPanel"); // NOI18N
        step3BottomPanel.setPreferredSize(new java.awt.Dimension(34, 34));
        step3BottomPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        searchSimilarGenesMessageLabel.setText(resourceMap.getString("searchSimilarGenesMessageLabel.text")); // NOI18N
        searchSimilarGenesMessageLabel.setName("searchSimilarGenesMessageLabel"); // NOI18N
        step3BottomPanel.add(searchSimilarGenesMessageLabel);

        step3Panel.add(step3BottomPanel, java.awt.BorderLayout.PAGE_END);

        mainPanel.add(step3Panel, "step3Panel");

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        controlsPanel.setName("controlsPanel"); // NOI18N
        controlsPanel.setLayout(new java.awt.BorderLayout());

        progressPanel.setName("progressPanel"); // NOI18N
        progressPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        progressbar.setName("progressbar"); // NOI18N
        progressPanel.add(progressbar);

        controlsPanel.add(progressPanel, java.awt.BorderLayout.LINE_START);

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));

        backButton.setText(resourceMap.getString("backButton.text")); // NOI18N
        backButton.setMaximumSize(new java.awt.Dimension(75, 27));
        backButton.setMinimumSize(new java.awt.Dimension(75, 27));
        backButton.setName("backButton"); // NOI18N
        backButton.setPreferredSize(new java.awt.Dimension(75, 27));
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonPressed(evt);
            }
        });
        buttonsPanel.add(backButton);

        nextButton.setText(resourceMap.getString("nextButton.text")); // NOI18N
        nextButton.setMaximumSize(new java.awt.Dimension(75, 27));
        nextButton.setMinimumSize(new java.awt.Dimension(75, 27));
        nextButton.setName("nextButton"); // NOI18N
        nextButton.setPreferredSize(new java.awt.Dimension(75, 27));
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonPressed(evt);
            }
        });
        buttonsPanel.add(nextButton);

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
        buttonsPanel.add(cancelButton);

        controlsPanel.add(buttonsPanel, java.awt.BorderLayout.LINE_END);

        getContentPane().add(controlsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
        setVisible(false);
    }//GEN-LAST:event_cancelButtonPressed

    private void nextButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonPressed
        // current step is the setp we are in before the next-button was pressed
        // the current step will be updated in response to the event
        if (currentstep==1) {
           if (getSelectedGenes().isEmpty()) {
              JOptionPane.showMessageDialog(this, "Please select at least one gene from the list to proceed", "Select genes", JOptionPane.INFORMATION_MESSAGE);
              return;
           }
           nextButton.setEnabled(experimentstablemodel.getRowCount()>0);
           backButton.setEnabled(true);
           currentstep++;
        } else if (currentstep==2) {
           if (getSelectedExperiments().isEmpty()) {
              JOptionPane.showMessageDialog(this, "Please select at least one experiment from the list to proceed", "Select experiments", JOptionPane.INFORMATION_MESSAGE);
              return;
           }
           nextButton.setEnabled(false);
           backButton.setEnabled(true);
           currentstep++;
        } else if (currentstep==3) { // this should not happen?
           nextButton.setEnabled(false);
           backButton.setEnabled(true);
        }
        String currentCard="step"+currentstep+"Panel";
        ((CardLayout)mainPanel.getLayout()).show(mainPanel, currentCard);
        setHeader(currentstep);
    }//GEN-LAST:event_nextButtonPressed

    private void backButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonPressed
        if (currentstep==1) {
           // this should not happen
        } else if (currentstep==2) {
            currentstep--;
            backButton.setEnabled(false);
            nextButton.setEnabled(true);
        } else if (currentstep==3) {
            currentstep--;
            backButton.setEnabled(true);
            nextButton.setEnabled(true);
        }
        String currentCard="step"+currentstep+"Panel";
        ((CardLayout)mainPanel.getLayout()).show(mainPanel, currentCard);
        setHeader(currentstep);
    }//GEN-LAST:event_backButtonPressed

    private void searchButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchButtonPressed
        nextButton.setEnabled(false);
        backButton.setEnabled(false);
        if (searchGenesButton.getText().equals("Search")) {
            searchGenesButton.setText("Stop");
            initiateGeneSearch();
        } else {
            searchGenesButton.setText("Search");
            if (communicator!=null) communicator.abort();
            progressbar.setVisible(false);
            communicator=null;           
        }
        
    }//GEN-LAST:event_searchButtonPressed

    private void searchExperimentsButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchExperimentsButtonPressed
        nextButton.setEnabled(false);
        backButton.setEnabled(false);
        if (searchExperimentsButton.getText().equals("Search")) {
            searchExperimentsButton.setText("Stop");
            initiateExperimentSearch();
        } else {
            searchExperimentsButton.setText("Search");
            if (communicatorExp!=null) communicatorExp.abort();
            progressbar.setVisible(false);
            communicatorExp=null;
        }
    }//GEN-LAST:event_searchExperimentsButtonPressed

    private void searchSimilarGenesButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchSimilarGenesButtonPressed
        nextButton.setEnabled(false);
        backButton.setEnabled(false);
        if (searchSimilarGenesButton.getText().equals("Search")) {
            searchSimilarGenesButton.setText("Stop");
            initiateSimilarGenesSearch();
        } else {
            searchSimilarGenesButton.setText("Search");
            if (communicatorSimilarGenes!=null) communicatorSimilarGenes.abort();
            progressbar.setVisible(false);
            communicatorSimilarGenes=null;
        }
    }//GEN-LAST:event_searchSimilarGenesButtonPressed

    /** Returns a list of the genes currently selected in the genes table */
    private ArrayList<String> getSelectedGenes() {
        ArrayList<String> selected=new ArrayList<String>();
        for (int i=0;i<genetablemodel.getRowCount();i++) {
            if ((Boolean)genetablemodel.getValueAt(i, 0)) {selected.add((String)genetablemodel.getValueAt(i, 4));}
        }
        return selected;                 
    }

    /** Returns a list of the experiments currently selected in the experiments table */
    private ArrayList<SelectedExperiment> getSelectedExperiments() {
        ArrayList<SelectedExperiment> selected=new ArrayList<SelectedExperiment>();
        for (int i=0;i<experimentstablemodel.getRowCount();i++) {
            if ((Boolean)experimentstablemodel.getValueAt(i, 0)) {
                UniqueExperimentID id=(UniqueExperimentID)experimentstablemodel.getValueAt(i, 1);
                int dir=(Integer)experimentstablemodel.getValueAt(i, 3);
                selected.add(new SelectedExperiment(id, dir));
            }
        }
        return selected;
    }

    /**
     * This method is called when pressing the first SEARCH button
     * and it initiates a search for genes matching a given name (or part of name)
     */
    private void initiateGeneSearch() {
        // first remove current contents of both gene and experiments table
        while (genetablemodel.getRowCount()>0) {
            genetablemodel.removeRow(0);
        }
        while (experimentstablemodel.getRowCount()>0) {
            experimentstablemodel.removeRow(0);
        }
        searchExperimentsMessageLabel.setText("  ");
        searchGenesTable.repaint();
        searchGenesMessageLabel.setText("  ");
        String organismString=(String)searchOrganismCombobox.getSelectedItem();
        if (organismString.equalsIgnoreCase("Any")) organismString=null;
        else {
            //organismString=organismString.toLowerCase(); // not necessary
            organismString=organismString.replace(' ', '+'); // escape spaces
        }
        String genes=searchGeneTextField.getText();
        if (genes!=null) genes=genes.trim();
        if (genes.isEmpty()) genes=null;
        if (genes!=null) {
            genes=genes.replaceAll("\\s*,\\s*", "+");
            genes=genes.replaceAll("\\s+", "+");
        }
        String query="https://www.ebi.ac.uk/gxa/api?";
        if (genes!=null) query+="geneIs="+genes;
        if (organismString!=null) query+="&species="+organismString;
        query+="&format=xml";
        gui.logMessage(query);
        searchForGenes(query);
    }

    /** Spawns a background thread that sends a query to the Gene Expression Atlas
     *  This search looks for genes matching a given name (or part of name)
     */
    private void searchForGenes(final String queryString) {
        progressbar.setVisible(true);
        progressbar.setIndeterminate(true);
        nextButton.setEnabled(false);
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                try {
                 if (communicator==null) communicator=new AtlasCommunicatorSearchGenes();
                 communicator.sendQuery(queryString);
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false);
                progressbar.setVisible(false);
                searchGenesButton.setText("Search");
                if (ex!=null) {
                     JOptionPane.showMessageDialog(GeneExpressionAtlasDialog.this, ex.getMessage(),"Error" ,JOptionPane.ERROR_MESSAGE);
                     return;
                } else { // all OK
                     if (genetablemodel.getRowCount()==0) searchGenesMessageLabel.setText("No matches found");
                     else nextButton.setEnabled(true);
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }

    /**
     * This method is called when pressing the second SEARCH button
     * It initiates a search for experiments containing the selected target genes
     * and also matching a given condition
     */
    private void initiateExperimentSearch() {
        // first remove current contents of experiments table
        while (experimentstablemodel.getRowCount()>0) {
            experimentstablemodel.removeRow(0);
        }
        searchExperimentsTable.repaint();
        searchExperimentsMessageLabel.setText("  ");
        String organismString=(String)searchOrganismCombobox.getSelectedItem();
        if (organismString.equalsIgnoreCase("Any")) organismString=null;
        else {
            //organismString=organismString.toLowerCase(); // not necessary
            organismString=organismString.replace(' ', '+'); // escape spaces
        }
        ArrayList<String> genes=getSelectedGenes();
        StringBuilder genestring=new StringBuilder();
        for (String gene:genes) {
            if (genestring.length()>0) genestring.append("+");
            genestring.append(gene);
        }
        String condition=searchConditionTextfield.getText();
        if (condition!=null) {
            condition=condition.trim();
            if (condition.isEmpty()) condition=null;
            else condition=condition.replaceAll("\\s+", "+"); // espace spaces
        }
        String query="https://www.ebi.ac.uk/gxa/api?";
        query+="geneGeneIs="+genestring.toString();
        String regulation=(String)searchRegulationCombobox.getSelectedItem();
        if (condition!=null) {           
                 if (regulation.equals("Up-regulated")) query+="&upIn="+condition;
            else if (regulation.equals("Down-regulated")) query+="&downIn="+condition;
            else if (regulation.equals("Up- or down-regulated")) query+="&updownIn="+condition;
        }
        int direction=NON_DIFFERENTIALLY_EXPRESSED;
             if (regulation.equals("Up-regulated")) direction=UP_REGULATED;
        else if (regulation.equals("Down-regulated")) direction=DOWN_REGULATED;
        else if (regulation.equals("Up- or down-regulated")) direction=UP_OR_DOWN_REGULATED;

        if (organismString!=null) query+="&species="+organismString;
        query+="&format=xml";
        gui.logMessage(query);
        searchForExperiments(query,genes,direction);
    }

    /** Spawns a background thread that sends a query to the Gene Expression Atlas
     *  This search looks for experiments containing the target genes and also matching a given condition
     */
    private void searchForExperiments(final String queryString, final ArrayList<String> geneIDs, final int direction) {
        progressbar.setVisible(true);
        progressbar.setIndeterminate(true);
        nextButton.setEnabled(false);
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                try {
                 if (communicatorExp==null) communicatorExp=new AtlasCommunicatorSearchExperiments();
                 communicatorExp.sendQuery(queryString, geneIDs, direction);
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false);
                progressbar.setVisible(false);
                backButton.setEnabled(true);
                searchExperimentsButton.setText("Search");
                if (ex!=null) {
                     JOptionPane.showMessageDialog(GeneExpressionAtlasDialog.this, ex.getMessage(),"Error" ,JOptionPane.ERROR_MESSAGE);
                     return;
                } else { // all OK
                     if (experimentstablemodel.getRowCount()==0) searchExperimentsMessageLabel.setText("No matches found");
                     else nextButton.setEnabled(true);
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }

    /**
     * This method is called when pressing the third SEARCH button
     * It initiates a search through all selected experiments to find
     * genes that are coregulated with the selected target genes
     */
    private void initiateSimilarGenesSearch() {
        // first remove current contents of experiments table
        while (similargenestablemodel.getRowCount()>0) {
            similargenestablemodel.removeRow(0);
        }
        searchSimilarGenesTable.repaint();
        searchSimilarGenesMessageLabel.setText("  ");
        ArrayList<SelectedExperiment> experiments=getSelectedExperiments();
        gui.logMessage("Search through "+experiments.size()+" selected experiments");
        searchForSimilarGenes(experiments);
    }

    /** Spawns a background thread that sends a query to the Gene Expression Atlas
     *  This search goes through an experiment and searches for other genes with same
     *  expression as the target
     */
    private void searchForSimilarGenes(final ArrayList<SelectedExperiment> experiments) {
        progressbar.setVisible(true);
        progressbar.setIndeterminate(false);
        progressbar.setValue(0);
        nextButton.setEnabled(false);
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                try {
                 if (communicatorSimilarGenes==null) communicatorSimilarGenes=new AtlasCommunicatorSearchSimilarGenes();
                 String query="https://www.ebi.ac.uk/gxa/api?";
                 String condition=null;
                 for (int i=0;i<experiments.size();i++) {
                    SelectedExperiment exp=experiments.get(i);
                    UniqueExperimentID expID=exp.uniqueExperimentID;
                    int direction=exp.direction;
//                     if (direction==UP_REGULATED) query+="&upIn="+condition;
//                     else if (direction==DOWN_REGULATED) query+="&downIn="+condition;
                    String querystring=query;
                    querystring+="experiment="+expID;
                    querystring+="&format=xml";
                    gui.logMessage(querystring);
                    communicatorSimilarGenes.sendQuery(querystring);
                    int progress=(int)((double)i/(double)experiments.size());
                    updateProgress(progress);
                 }
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                }
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false);
                progressbar.setVisible(false);
                backButton.setEnabled(true);
                searchSimilarGenesButton.setText("Search");
                if (ex!=null) {
                     JOptionPane.showMessageDialog(GeneExpressionAtlasDialog.this, ex.getMessage(),"Error" ,JOptionPane.ERROR_MESSAGE);
                     return;
                } else { // all OK
                     if (experimentstablemodel.getRowCount()==0) searchSimilarGenesMessageLabel.setText("No matches found");
                     else nextButton.setEnabled(false);
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }

    /** Updates progress on the EDT */
    private void updateProgress(final int progress) {
        Runnable runner=new Runnable() {
            @Override
            public void run() {
                progressbar.setValue(progress);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backButton;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel controlsPanel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton nextButton;
    private javax.swing.JPanel progressPanel;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JLabel searchConditionLabel;
    private javax.swing.JTextField searchConditionTextfield;
    private javax.swing.JButton searchExperimentsButton;
    private javax.swing.JLabel searchExperimentsMessageLabel;
    private javax.swing.JScrollPane searchExperimentsScrollPane;
    private javax.swing.JTextField searchGeneTextField;
    private javax.swing.JButton searchGenesButton;
    private javax.swing.JLabel searchGenesLabel;
    private javax.swing.JLabel searchGenesMessageLabel;
    private javax.swing.JScrollPane searchGenesScrollPane;
    private javax.swing.JComboBox searchOrganismCombobox;
    private javax.swing.JLabel searchOrganismLabel;
    private javax.swing.JComboBox searchRegulationCombobox;
    private javax.swing.JLabel searchRegulationLabel;
    private javax.swing.JButton searchSimilarGenesButton;
    private javax.swing.JLabel searchSimilarGenesMessageLabel;
    private javax.swing.JScrollPane searchSimilarGenesScrollPane;
    private javax.swing.JLabel spacerLabel1;
    private javax.swing.JLabel spacerLabel2;
    private javax.swing.JLabel spacerLabel3;
    private javax.swing.JLabel spacerLabel4;
    private javax.swing.JPanel step1BottomPanel;
    private javax.swing.JPanel step1ControlsPanel;
    private javax.swing.JLabel step1Label;
    private javax.swing.JPanel step1Panel;
    private javax.swing.JPanel step1ResultsPanel;
    private javax.swing.JPanel step2BottomPanel;
    private javax.swing.JPanel step2ControlsPanel;
    private javax.swing.JLabel step2Label;
    private javax.swing.JPanel step2Panel;
    private javax.swing.JPanel step2ResultsPanel;
    private javax.swing.JPanel step3BottomPanel;
    private javax.swing.JPanel step3ControlsPanel;
    private javax.swing.JLabel step3Label;
    private javax.swing.JPanel step3Panel;
    private javax.swing.JPanel step3ResultsPanel;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables

public class GeneTableModel extends DefaultTableModel {
    public GeneTableModel() {
        super(new String[]{"","Name","Organism","Description","IDs"}, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return (column==0);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
       switch (columnIndex) {
           case 0:return Boolean.class;
           default:return String.class;
       }
    }

} // end GeneTableModel

public class ExperimentsTableModel extends DefaultTableModel {
    public ExperimentsTableModel() {
        super(new String[]{"","Experiment","Condition","Dir","p-value"}, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return (column==0);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
       switch (columnIndex) {
           case 0:return Boolean.class;
           case 1:return UniqueExperimentID.class;
           case 3:return Integer.class;
           case 4:return Double.class;
           default:return String.class;
       }
    }

} // end ExperimentsTableModel


public class SimilarGenesTableModel extends DefaultTableModel {
    public SimilarGenesTableModel() {
        super(new String[]{"","Gene ID","Name","Same","Opposite","p-value"}, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return (column==0);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
       switch (columnIndex) {
           case 0:return Boolean.class;
           case 3:return Integer.class;
           case 4:return Integer.class;
           case 5:return Double.class;
           default:return String.class;
       }
    }

} // end SimilarGenesTableModel

public class AtlasCommunicatorSearchGenes {
    SAXParserFactory factory;
    SAXParser saxParser;
    ElementParser handler;
    StringBuilder buffer;
    boolean keepText=false;
    boolean aborted=false;
    
    String geneid=null;
    String genename=null;
    String geneorganism=null;
    String genedescription=null;


    /** Sends a query to the GeneExpressionAtlas API and parses the response XML-file using SAX */
    public void sendQuery(String uri) throws Exception {
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        URL url=new URL(uri);
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(gui.getEngine().getNetworkTimeout());
        InputStream inputStream=connection.getInputStream();
        try {
            saxParser.parse(inputStream, handler);
        } catch (Exception e) {if (!aborted) throw e;}
        finally {
           inputStream.close();
        }
    }

    public void abort() {
        aborted=true;
    }

    /** Implements important methods in the callback interface for the SAX XML-reader*/
    private class ElementParser extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            buffer=null;
            if (   qName.equals("id")
                || qName.equals("name") 
                || qName.equals("interProTerm")
                || qName.equals("organism")
            ) keepText=true;
            else keepText=false;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            if (qName.equals("id")) {
                geneid=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("name")) {
                genename=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("organism")) {
                geneorganism=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("interProTerm")) {
                genedescription=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("gene")) {
                addRowToGeneTable(genename, geneorganism, genedescription, geneid);
                geneid=null;
                genename=null;
                geneorganism=null;
                genedescription=null;
            }
        }


        @Override
        public void characters(char[] chars, int start, int length) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            if (keepText) {
                if (buffer==null) buffer=new StringBuilder();
                buffer.append(chars, start, length);
            }
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            if (aborted) return;
            throw e;
        }

    } // end internal class ElementParser

    private void addRowToGeneTable(final String name, final String organism, final String description, final String ids) {
        if (aborted) return;
        Runnable runner=new Runnable() {
                @Override
                public void run() {
                    genetablemodel.addRow(new Object[]{Boolean.TRUE,name,organism,description,ids});
                    searchGenesMessageLabel.setText("Found: "+genetablemodel.getRowCount());
                }
        };
        SwingUtilities.invokeLater(runner);
    }

}

/** Class to query and process responses from GEA regarding experiments containing selected genes */
public class AtlasCommunicatorSearchExperiments {
    SAXParserFactory factory;
    SAXParser saxParser;
    ElementParser handler;
    StringBuilder buffer;
    int direction=0; // the target direction specified by the user (can be UP_REGULATED, DOWN_REGULATED or UP_OR_DOWN_REGULATED)
    boolean inGene=false;
    boolean inOuterExpression=false;
    boolean inInnerExpression=false;
    boolean inExperiment=false;
    boolean keepText=false;
    boolean aborted=false;

    String geneID=null;
    String genename=null;
    String experimentID=null;
    String geneorganism=null;
    String ef=null;
    String efv=null;
    String efoTerm=null;
    String geneRegDirection=null;
    String pvalue=null;

    HashMap<UniqueExperimentID,ExperimentGenePool> experimentpools=null;
    ArrayList<String> targetGenes=null;

    /** Sends a query to the GeneExpressionAtlas API and parses the response XML-file using SAX
     *  @param uri The URI string containing the query to GeneExpressionAtlas
     *  @param targetGenes A list of (Ensemble) IDs for the target genes
     *  @param direction The target direction of expression. This can be UP_REGULATED, DOWN_REGULATED or UP_OR_DOWN_REGULATED
     */
    public void sendQuery(String uri, ArrayList<String> targetGenes, int direction) throws Exception {
        if (direction!=UP_REGULATED && direction !=DOWN_REGULATED && direction !=UP_OR_DOWN_REGULATED) throw new ExecutionError("Unrecognized direction of expression: "+direction);
        this.targetGenes=targetGenes;
        this.direction=direction;
        experimentpools=new HashMap<UniqueExperimentID,ExperimentGenePool>();
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        URL url=new URL(uri);
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(gui.getEngine().getNetworkTimeout());
        InputStream inputStream=connection.getInputStream();
        try {
            saxParser.parse(inputStream, handler);
        } catch (Exception e) {if (!aborted) throw e;}
        finally {
           inputStream.close();
        }
    }

    public void abort() {
        aborted=true;
    }

    /** Implements important methods in the callback interface for the SAX XML-reader*/
    private class ElementParser extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            buffer=null;
            if (   qName.equals("id")
                || qName.equals("name")
                || qName.equals("accession")
                || qName.equals("efoTerm")
                || qName.equals("ef")
                || qName.equals("efv")
                || qName.equals("organism")
                || qName.equals("pvalue")
                || (qName.equals("expression") && inExperiment)
            ) keepText=true;
            else keepText=false;
                 if (qName.equals("gene")) inGene=true;
            else if (qName.equals("experiment")) inExperiment=true;
            else if (qName.equals("expression")) {
                if (inExperiment) inInnerExpression=true;
                else inOuterExpression=true;
            }

        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            if (qName.equals("id") && inGene) {
                geneID=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("name") && inGene) {
                genename=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("organism") && inGene) {
                geneorganism=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("ef")) {
                ef=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("efv")) {
                efv=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("efoTerm")) {
                efoTerm=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("expression") && inExperiment) { // inner expression
                geneRegDirection=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("expression") && !inExperiment) { // outer expression
                ef=null;efv=null;efoTerm=null;
            } else if (qName.equals("pvalue") && inExperiment) { // inner expression
                pvalue=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("accession") && inExperiment) { // inner expression
                experimentID=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("experiment")) {
                //boolean admissable=((direction==UP_REGULATED && geneRegDirection.equalsIgnoreCase("UP"))|| (direction==DOWN_REGULATED && geneRegDirection.equalsIgnoreCase("DOWN")) || (direction==UP_OR_DOWN_REGULATED && (geneRegDirection.equalsIgnoreCase("UP") || geneRegDirection.equalsIgnoreCase("DOWN"))));
                addToExperimentGenePool(experimentID, geneRegDirection, pvalue, geneorganism, ef, efv, efoTerm, geneID);
                experimentID=null;
                pvalue=null;
                geneRegDirection=null;
            }
            
                 if (qName.equals("gene")) inGene=false;
            else if (qName.equals("experiment")) inExperiment=false;
            else if (qName.equals("expression")) {
                if (inExperiment) inInnerExpression=false;
                else inOuterExpression=false;
            }
        }


        @Override
        public void characters(char[] chars, int start, int length) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            if (keepText) {
                if (buffer==null) buffer=new StringBuilder();
                buffer.append(chars, start, length);
            }
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            if (aborted) return;
            throw e;
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            // Go through the experimentGenePools and add present those that are still admissible to the user
            for (ExperimentGenePool ex:experimentpools.values()) {
                if (ex.isAdmissible(targetGenes)) addRowToExperimentsTable(ex);
            }
            experimentpools=null; // release
        }



    } // end internal class ElementParser

    /** Adds a newly parsed "Experiment/Gene-pair" to the pool for this experiment */
    private void addToExperimentGenePool(final String experimentID, final String dirString, final String pvalueString, final String organism, final String ef, final String efv, final String efoTerm, final String geneID) {
        UniqueExperimentID UXID=new UniqueExperimentID(experimentID, ef, efv, efoTerm);
        String condition;
        if (ef!=null && efv!=null) condition=ef+"="+efv;
        else if (efoTerm!=null) condition=efoTerm;
        else condition="not found";
        if (!experimentpools.containsKey(UXID)) {
            ExperimentGenePool experimentgenepool=new ExperimentGenePool(UXID, condition, direction);
            experimentpools.put(UXID,experimentgenepool);
        }
        ExperimentGenePool experimentgenepool=experimentpools.get(UXID);
        double genepvalue=0;
        try {
           genepvalue=Double.parseDouble(pvalueString);
        } catch (NumberFormatException nfe) {}
        int genedirection=0;
        if (dirString.equalsIgnoreCase("UP")) genedirection=UP_REGULATED;
        else if (dirString.equalsIgnoreCase("DOWN")) genedirection=DOWN_REGULATED;
        else genedirection=NON_DIFFERENTIALLY_EXPRESSED;

        if (genedirection!=NON_DIFFERENTIALLY_EXPRESSED) experimentgenepool.addGene(geneID, genedirection, genepvalue);
    }


    private void addRowToExperimentsTable(final ExperimentGenePool experiment) {
        if (aborted) return;
        Runnable runner=new Runnable() {
            @Override
            public void run() {
                experimentstablemodel.addRow(new Object[]{Boolean.FALSE, experiment.getUniqueExperimentID(), experiment.getCondition(), experiment.getDirection(), experiment.getPvalue()});
                searchExperimentsMessageLabel.setText("Found: "+experimentstablemodel.getRowCount());
            }
        };
        SwingUtilities.invokeLater(runner);
    }

}

public class AtlasCommunicatorSearchSimilarGenes {
    SAXParserFactory factory;
    SAXParser saxParser;
    ElementParser handler;
    StringBuilder buffer;
    boolean keepText=false;
    boolean aborted=false;

    String geneid=null;
    String genename=null;
    String geneorganism=null;
    String genedescription=null;


    /** Sends a query to the GeneExpressionAtlas API and parses the response XML-file using SAX */
    public void sendQuery(String uri) throws Exception {
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        URL url=new URL(uri);
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(gui.getEngine().getNetworkTimeout());
        InputStream inputStream=connection.getInputStream();
        try {
            saxParser.parse(inputStream, handler);
        } catch (Exception e) {if (!aborted) throw e;}
        finally {
           inputStream.close();
        }
    }

    public void abort() {
        aborted=true;
    }

    /** Implements important methods in the callback interface for the SAX XML-reader*/
    private class ElementParser extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            buffer=null;
            if (   qName.equals("id")
                || qName.equals("name")
                || qName.equals("interProTerm")
                || qName.equals("organism")
            ) keepText=true;
            else keepText=false;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            if (qName.equals("id")) {
                geneid=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("name")) {
                genename=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("organism")) {
                geneorganism=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("interProTerm")) {
                genedescription=(buffer==null)?"ERROR":buffer.toString();
            } else if (qName.equals("gene")) {
                addRowToSimilarGenesTable(genename, geneorganism, genedescription, geneid);
                geneid=null;
                genename=null;
                geneorganism=null;
                genedescription=null;
            }
        }


        @Override
        public void characters(char[] chars, int start, int length) throws SAXException {
            if (aborted) throw new SAXException("*ABORT*");
            if (keepText) {
                if (buffer==null) buffer=new StringBuilder();
                buffer.append(chars, start, length);
            }
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            if (aborted) return;
            throw e;
        }

    } // end internal class ElementParser

    private void addRowToSimilarGenesTable(final String name, final String organism, final String description, final String ids) {
        if (aborted) return;
        Runnable runner=new Runnable() {
                @Override
                public void run() {
                    genetablemodel.addRow(new Object[]{Boolean.TRUE,name,organism,description,ids});
                    searchGenesMessageLabel.setText("Found: "+genetablemodel.getRowCount());
                }
        };
        SwingUtilities.invokeLater(runner);
    }

}



/** Uniquely identifies an experiment based on Experiment Accession (ID) and condition */
private class UniqueExperimentID {
    String experimentAccession=null;
    String ef=null;
    String efv=null;
    String efoTerm=null;

    public UniqueExperimentID(String experimentAccession, String ef, String efv, String efoTerm) {
        this.experimentAccession=experimentAccession;
        this.ef=ef;
        this.efv=efv;
        this.efoTerm=efoTerm;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==null || !(obj instanceof UniqueExperimentID)) return false;
        UniqueExperimentID other=(UniqueExperimentID)obj;
        if (this.experimentAccession==null && other.experimentAccession!=null) return false;
        if (this.experimentAccession!=null && other.experimentAccession==null) return false;
        if (this.experimentAccession!=null && other.experimentAccession!=null && !this.experimentAccession.equals(other.experimentAccession)) return false;

        if (this.ef==null && other.ef!=null) return false;
        if (this.ef!=null && other.ef==null) return false;
        if (this.ef!=null && other.ef!=null && !this.ef.equals(other.ef)) return false;

        if (this.efv==null && other.efv!=null) return false;
        if (this.efv!=null && other.efv==null) return false;
        if (this.efv!=null && other.efv!=null && !this.efv.equals(other.efv)) return false;

        if (this.efoTerm==null && other.efoTerm!=null) return false;
        if (this.efoTerm!=null && other.efoTerm==null) return false;
        if (this.efoTerm!=null && other.efoTerm!=null && !this.efoTerm.equals(other.efoTerm)) return false;

        return true;
    }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + (this.experimentAccession != null ? this.experimentAccession.hashCode() : 0);
            hash = 79 * hash + (this.ef != null ? this.ef.hashCode() : 0);
            hash = 79 * hash + (this.efv != null ? this.efv.hashCode() : 0);
            hash = 79 * hash + (this.efoTerm != null ? this.efoTerm.hashCode() : 0);
            return hash;
        }


}

/** Just a small struct-class to hold info about selected experiments*/
private class SelectedExperiment {
    UniqueExperimentID uniqueExperimentID;
    int direction;

    SelectedExperiment(UniqueExperimentID uniqueExperimentID, int direction) {
        this.uniqueExperimentID=uniqueExperimentID;
        this.direction=direction;
    }
}

/** Represents a pool of genes for the same experiment
 *  As new genes are parsed for experiments they are added to the pool for the same experiment.
 *  If all genes have the same expression direction and this is the same as the target
 *  then the Experiment will be admissible for further analysis and included in the table
 *  presented to the user. If not, the experiment will be discarded from further consideration
 */
private class ExperimentGenePool {
    private boolean banned=false;
    private int targetdirection=0;
    private ArrayList<String> genes=new ArrayList<String>(2);
    private UniqueExperimentID UXID;
    private String condition;    
    private double pvalue=0; // the highest p-value among the genes


    public ExperimentGenePool(UniqueExperimentID id, String condition, int direction) {
        targetdirection=direction;
        this.UXID=id;
        this.condition=condition;

    }

    /** Returns TRUE if all the specified genes in the list
     *  have been found for this experiment and all have the same
     *  direction of expression (in concordance with chosen target direction)
     */
    public boolean isAdmissible(ArrayList<String> geneIDs) {
        if (banned) return false;
        boolean allpresent=true;
        for (String geneID:geneIDs) {
            if (!genes.contains(geneID)) {
                allpresent=false; break;
            }
        }
        return allpresent;
    }

    public int getDirection() {
        return targetdirection;
    }

    public String getCondition() {
        return condition;
    }

    public double getPvalue() {
        return pvalue;
    }
    public String getExperimentAcc() {
        return UXID.experimentAccession;
    }
    public UniqueExperimentID getUniqueExperimentID() {
        return UXID;
    }

    /** Add a gene to this experiment gene pool */
    public void addGene(String geneID, int geneDirection, double genepvalue) {
        if (banned) return; // no need to add more
        if (genes.isEmpty()) {
            if (targetdirection==UP_OR_DOWN_REGULATED) targetdirection=geneDirection; // set specific direction based on first gene!
        }
        if (geneDirection==targetdirection) {
            if (!genes.contains(geneID)) genes.add(geneID);
            if (genepvalue>pvalue) pvalue=genepvalue;
            //System.err.println("Gene '"+geneID+"' dir="+geneDirection+" matches target="+targetdirection);
        } else {
            banned=true;
            //System.err.println("Gene '"+geneID+"' dir="+geneDirection+" does not match target="+targetdirection+"   Experiment["+experimentID+","+condition+"] is BANNED");

        } // this gene does not have the same direction as the target, so the experiment must be banned
    }
}

private class RegulationRenderer extends DefaultTableCellRenderer {

    MiscIcons upregulated;
    MiscIcons downregulated;
    VisualizationSettings settings;

    public RegulationRenderer(MotifLabGUI gui) {
         super();
         upregulated=new MiscIcons(MiscIcons.UPREGULATED);
         downregulated=new MiscIcons(MiscIcons.DOWNREGULATED);
         upregulated.setForegroundColor(Color.GREEN);
         upregulated.setFillColor(new Color(160,255,160));
         downregulated.setForegroundColor(Color.RED);
         downregulated.setFillColor(new Color(255,160,160));

         //setOpaque(false);
         Dimension d=new Dimension(12,12);
         setMinimumSize(d);
         setPreferredSize(d);
         setMaximumSize(d);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); // this will set background color
        if (value instanceof Integer) {
            if (((Integer)value).intValue()==UP_REGULATED) setIcon(upregulated);
            else if (((Integer)value).intValue()==DOWN_REGULATED) setIcon(downregulated);   
            else this.setIcon(null);
        }
        else if(value instanceof String) {
           if (((String)value).equalsIgnoreCase("UP")) setIcon(upregulated);
           else if (((String)value).equalsIgnoreCase("DOWN")) setIcon(downregulated);
           else this.setIcon(null);
        } else this.setIcon(null);
        return this;
    }
}

private class ExperimentIDRenderer extends DefaultTableCellRenderer {

      public ExperimentIDRenderer() {
         super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); // this will set background color
        if (value instanceof UniqueExperimentID) {
           this.setText(((UniqueExperimentID)value).experimentAccession);
        } else this.setText("ERROR");
        return this;
    }
}
}
