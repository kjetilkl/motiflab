/*
 
 
 */

package motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import motiflab.engine.ExecutionError;
import motiflab.engine.Graph;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.*;

import motiflab.engine.SystemError;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.dataformat.DataFormat_INCLUSive_Background;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.task.OperationTask;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.LoadFromFilePanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.SimpleDataPanelIcon;


/**
 *
 * @author kjetikl
 */
public class Prompt_BackgroundModel extends Prompt {

    private BackgroundModel data;

    private JPanel manualEntryPanel;
    private JPanel predefinedModelsPanel;
    private JPanel fromTrackPanel;
    private LoadFromFilePanel importModelPanel;
    private JPanel distributionPanel;
    private JTabbedPane tabbedPanel;
    private JComboBox orderCombobox;
    private JComboBox fromTrackOrderCombobox;
    private JComboBox fromTrackStrandCombobox;    
    private JComboBox fromTrackDatasetCombobox;
    private JComboBox fromTrackSequencesCombobox;    
    private JTable matrixTable;
    private JTable oligoTable;
    private JScrollPane scrollpane;    
    private JScrollPane oligoscrollpane;    
    private DefaultTableModel matrixTableModel;
    private DefaultTableModel oligoTableModel;
    private DefaultTableModel snfTableModel;
    private DefaultTableModel predefinedModelTableModel;
    private JTable predefinedModelTable;
    private JTable rowheaderView;
    private JTable oligoRowheaderView;
    private String[] bases = new String[]{"A","C","G","T"};
    private JLabel predefinedPanelErrorLabel;
    private JLabel manualPanelErrorLabel;
    private boolean showExisting=false;
    private int rowheadersize=60;

    public Prompt_BackgroundModel(MotifLabGUI gui, String prompt, BackgroundModel dataitem) {
        this(gui,prompt,dataitem,true);
    }

    
    public Prompt_BackgroundModel(MotifLabGUI gui, String prompt, BackgroundModel dataitem, boolean modal) {
        super(gui, prompt, modal);
        showExisting=(dataitem!=null);
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else data=new BackgroundModel(gui.getGenericDataitemName(BackgroundModel.class, null));
        setDataItemName(data.getName());
        setTitle("Background Model");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.MARKOV_MODEL_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(java.awt.Color.WHITE);
        setDataItemIcon(icon, true);
        setupPredefinedModelsPanel();
        setupManualEntryPanel();
        setupFromTrackPanel();
        setupImportModelPanel();
        setupDistributionPanel();
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Predefined Models", predefinedModelsPanel);
        tabbedPanel.addTab("Manual Entry", manualEntryPanel);
        tabbedPanel.addTab("From Track", fromTrackPanel);
        tabbedPanel.addTab("Import Model", importModelPanel);
        tabbedPanel.addTab("Distribution", distributionPanel);
        JPanel internal=new JPanel(new BorderLayout());
        Dimension size=new Dimension(600,500);
        internal.setMinimumSize(size);
        internal.setPreferredSize(size);
        // internal.setMaximumSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        predefinedModelsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        importModelPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromTrackPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        distributionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        internal.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(internal);        
        pack();
        if (showExisting) {
            String predefinedModelName=dataitem.getPredefinedModel();
            String trackName=dataitem.getTrackName();
            if (predefinedModelName!=null) {
                tabbedPanel.setSelectedComponent(predefinedModelsPanel);
                ListSelectionModel selectionmodel=predefinedModelTable.getSelectionModel();
                for (int i=0;i<predefinedModelTableModel.getRowCount();i++) {
                    if (((String)predefinedModelTableModel.getValueAt(i,0)).equals(predefinedModelName)) {
                        selectionmodel.setSelectionInterval(i, i); // select the original model in the table
                        break;
                    }
                }
            }
//            else if (trackName!=null) {
//                if (engine.dataExists(trackName, DNASequenceDataset.class)) tabbedPanel.setSelectedComponent(fromTrackPanel);
//                else tabbedPanel.setSelectedComponent(manualEntryPanel);
//            }
            else 
            tabbedPanel.setSelectedComponent(manualEntryPanel);
            focusOKButton();
        } 
    }
    
    private void setupPredefinedModelsPanel() {
       predefinedModelsPanel=new JPanel();
        String[] columns = new String[]{"Name","Order","Organism","Filename"};
        Object[][] predefinedmodelstable = BackgroundModel.predefinedModels;
        
        predefinedModelTableModel=new DefaultTableModel(predefinedmodelstable,columns);
        predefinedModelTable=new JTable(predefinedModelTableModel) {
            public boolean isCellEditable(int row,int col) {return false;}
        };
        predefinedModelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        predefinedModelTable.getColumn("Order").setMinWidth(15);
        predefinedModelTable.getColumn("Order").setMaxWidth(50);
        predefinedModelTable.removeColumn(predefinedModelTable.getColumn("Filename"));
        predefinedModelsPanel.setLayout(new BorderLayout());
        scrollpane=new JScrollPane(predefinedModelTable);
        predefinedModelTable.setFillsViewportHeight(true);
        predefinedModelTable.setAutoCreateRowSorter(true);
        predefinedModelTable.getTableHeader().setReorderingAllowed(false);
        predefinedModelsPanel.add(scrollpane,BorderLayout.CENTER);
        JPanel statuspanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        predefinedPanelErrorLabel=new JLabel("  ");
        predefinedPanelErrorLabel.setFont(errorMessageFont);
        predefinedPanelErrorLabel.setForeground(java.awt.Color.RED);
        statuspanel.add(predefinedPanelErrorLabel);
        predefinedModelsPanel.add(statuspanel,BorderLayout.SOUTH);
        predefinedModelTable.getColumn("Order").setCellRenderer(new CellRenderer_RightAlign());
        predefinedModelTable.getColumn("Organism").setCellRenderer(new CellRenderer_Organism());
        predefinedModelTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) { // update name of new data object based on selected predefined model
                if (showExisting) return;
                int row=predefinedModelTable.getSelectedRow();
                if (row>=0) {
                  String trackname=(String)predefinedModelTable.getValueAt(row, predefinedModelTable.getColumn("Name").getModelIndex());
                  setDataItemName(trackname);
                } else setDataItemName(data.getName());
            }
        });
        predefinedModelTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) clickOK();
             }
        });
    }
    
    private void setupManualEntryPanel() {
        manualEntryPanel=new JPanel();
        manualEntryPanel.setLayout(new BorderLayout());
        JPanel manualEntryPanelInternalPanel=new JPanel();
        String[] bases1 = new String[]{"A","C","G","T"};        
        manualEntryPanelInternalPanel.setLayout(new BoxLayout(manualEntryPanelInternalPanel, BoxLayout.Y_AXIS));
               
        snfTableModel=new DefaultTableModel(new Double[][]{{new Double(data.getSNF('A')),new Double(data.getSNF('C')),new Double(data.getSNF('G')),new Double(data.getSNF('T'))}},bases1);
        JTable snfTable=new JTable(snfTableModel);
        snfTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        ExcelAdapter adapter1=new ExcelAdapter(snfTable,false, ExcelAdapter.CONVERT_TO_DOUBLE);
        snfTable.setFillsViewportHeight(true);
        matrixTableModel=new DefaultTableModel(data.getTransitionMatrix(),bases);
        matrixTable=new JTable(matrixTableModel);
        matrixTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        ExcelAdapter adapter2=new ExcelAdapter(matrixTable,false, ExcelAdapter.CONVERT_TO_DOUBLE);
        scrollpane=new JScrollPane(matrixTable);
        rowheaderView=getRowHeaderView(data.getOrder());
        scrollpane.setRowHeaderView(rowheaderView);
        Dimension rowsize=new Dimension(rowheadersize,170);
        scrollpane.getRowHeader().setMaximumSize(rowsize);
        scrollpane.getRowHeader().setMinimumSize(rowsize);
        scrollpane.getRowHeader().setPreferredSize(rowsize);
        matrixTable.setFillsViewportHeight(true);
        snfTable.getTableHeader().setReorderingAllowed(false);
        matrixTable.getTableHeader().setReorderingAllowed(false);
        snfTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        snfTable.setCellSelectionEnabled(true);
        matrixTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        matrixTable.setCellSelectionEnabled(true);
        
        double[] oligoFrequencies=data.getOligoFrequencies();
        Double[][] oligovalues=new Double[oligoFrequencies.length][1];
        for (int i=0;i<oligovalues.length;i++) oligovalues[i][0]=oligoFrequencies[i];
        oligoTableModel=new DefaultTableModel(oligovalues,new String[]{"Frequency"});
        oligoTable=new JTable(oligoTableModel);
        oligoTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus       
        ExcelAdapter adapter3=new ExcelAdapter(oligoTable,false, ExcelAdapter.CONVERT_TO_DOUBLE);
        oligoTable.setFillsViewportHeight(true);
        oligoTable.getTableHeader().setReorderingAllowed(false);
        oligoTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        oligoTable.setCellSelectionEnabled(true);        
        oligoscrollpane=new JScrollPane(oligoTable);
        oligoRowheaderView=getOligoRowHeaderView(data.getOrder());
        oligoscrollpane.setRowHeaderView(oligoRowheaderView);
        oligoscrollpane.getRowHeader().setMaximumSize(rowsize);
        oligoscrollpane.getRowHeader().setMinimumSize(rowsize);
        oligoscrollpane.getRowHeader().setPreferredSize(rowsize);
        
        JPanel internal1=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal2label=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal3label=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal4label=new JPanel(new FlowLayout(FlowLayout.LEADING));
        internal1.add(new JLabel("Model Order   "));
        orderCombobox=new JComboBox(new Integer[]{0,1,2,3,4,5});
        internal1.add(orderCombobox);
        if (data!=null) {
            int GCcontent=(int)Math.round((data.getGCcontent()*100.0));
            internal1.add(new JLabel("       GC-content: "+GCcontent+" %"));
        }

        Dimension d=new Dimension(500,38);
        Dimension d2=new Dimension(500,120);

        JPanel internal2table=new JPanel(new BorderLayout());
        JPanel internal2=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal3=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal4=new JPanel(new FlowLayout(FlowLayout.LEADING));
        internal2table.add(snfTable.getTableHeader(),BorderLayout.NORTH);
        internal2table.add(snfTable,BorderLayout.CENTER);        
        internal3.add(oligoscrollpane);
        internal4.add(scrollpane);
        scrollpane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowheaderView.getTableHeader());
        oligoscrollpane.setCorner(JScrollPane.UPPER_LEFT_CORNER, oligoRowheaderView.getTableHeader());
        internal2table.setMinimumSize(d);
        internal2table.setMaximumSize(d);
        internal2table.setPreferredSize(d);
        scrollpane.setMinimumSize(d2);
        scrollpane.setMaximumSize(d2);
        scrollpane.setPreferredSize(d2);
        oligoscrollpane.setMinimumSize(d2);
        oligoscrollpane.setMaximumSize(d2);
        oligoscrollpane.setPreferredSize(d2);
        JPanel statusPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setMinimumSize(d);
        statusPanel.setMaximumSize(d);
        statusPanel.setPreferredSize(d);
        JPanel statuspanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        manualPanelErrorLabel=new JLabel("  ");
        manualPanelErrorLabel.setFont(errorMessageFont);
        manualPanelErrorLabel.setForeground(java.awt.Color.RED);
        statuspanel.add(manualPanelErrorLabel);
        internal2.add(new JScrollPane(internal2table)); // Previously: internal2.add(internal2table);  The scrollpane is not necessary, but it provides a similar border to the other tables
        internal2label.add(new JLabel("Single Nucleotide Frequencies"));
        internal3label.add(new JLabel("Oligo Frequencies"));
        internal4label.add(new JLabel("Transition Matrix"));
        statusPanel.add(manualPanelErrorLabel);
        manualEntryPanelInternalPanel.add(internal1);
        manualEntryPanelInternalPanel.add(internal2label);
        manualEntryPanelInternalPanel.add(internal2);
        manualEntryPanelInternalPanel.add(internal3label);
        manualEntryPanelInternalPanel.add(internal3);
        manualEntryPanelInternalPanel.add(internal4label);
        manualEntryPanelInternalPanel.add(internal4);
        manualEntryPanelInternalPanel.add(statusPanel);
        orderCombobox.setSelectedItem(new Integer(data.getOrder()));
        orderCombobox.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int order=((Integer)orderCombobox.getSelectedItem()).intValue();
                matrixTableModel=new DefaultTableModel(getDefaultModel(order),bases);
                matrixTable.setModel(matrixTableModel);
                rowheaderView=getRowHeaderView(order);
                scrollpane.setRowHeaderView(rowheaderView);
                scrollpane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowheaderView.getTableHeader());
                Dimension rowsize=new Dimension(rowheadersize,170);
                scrollpane.getRowHeader().setMaximumSize(rowsize);
                scrollpane.getRowHeader().setMinimumSize(rowsize);
                scrollpane.getRowHeader().setPreferredSize(rowsize);

                oligoTableModel=new DefaultTableModel(getDefaultOligoModel(order),new String[]{"Frequency"});
                oligoTable.setModel(oligoTableModel);     
                oligoRowheaderView=getOligoRowHeaderView(order);
                oligoscrollpane.setRowHeaderView(oligoRowheaderView);
                oligoscrollpane.setCorner(JScrollPane.UPPER_LEFT_CORNER, oligoRowheaderView.getTableHeader());
                oligoscrollpane.getRowHeader().setMaximumSize(rowsize);
                oligoscrollpane.getRowHeader().setMinimumSize(rowsize);
                oligoscrollpane.getRowHeader().setPreferredSize(rowsize);            
                manualEntryPanel.repaint();            
            }
        });     
        manualEntryPanelInternalPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        manualEntryPanel.add(new JScrollPane(manualEntryPanelInternalPanel));
    }

    private void setupFromTrackPanel() {
        fromTrackPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal = new JPanel(new GridBagLayout());
        internal.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.insets=new Insets(4,4,4,4);
        constraints.anchor=GridBagConstraints.BASELINE_LEADING;
        constraints.gridy=0;constraints.gridx=0;
        internal.add(new JLabel("DNA Track  "),constraints);
        constraints.gridy=1;constraints.gridx=0;
        internal.add(new JLabel("Sequences  "),constraints);
        constraints.gridy=2;constraints.gridx=0;        
        internal.add(new JLabel("Model Order  "),constraints);
        constraints.gridy=3;constraints.gridx=0;
        internal.add(new JLabel("Strand  "),constraints);


        
        DefaultComboBoxModel tracksmodel=new DefaultComboBoxModel();
        ArrayList<String> dnaTracks=engine.getNamesForAllDataItemsOfType(DNASequenceDataset.class);
        Collections.sort(dnaTracks);
        for (String dnatrack:dnaTracks) {
            tracksmodel.addElement(dnatrack);
        }
        fromTrackDatasetCombobox=new JComboBox(tracksmodel);
        if (data.getTrackName()!=null) fromTrackDatasetCombobox.setSelectedItem(data.getTrackName());
        constraints.gridy=0;constraints.gridx=1;
        internal.add(fromTrackDatasetCombobox,constraints);
        
        DefaultComboBoxModel collectionsmodel=new DefaultComboBoxModel();
        ArrayList<String> collections=engine.getNamesForAllDataItemsOfType(SequenceCollection.class);
        Collections.sort(collections);
        for (String element:collections) {
            collectionsmodel.addElement(element);
        }
        fromTrackSequencesCombobox=new JComboBox(collectionsmodel);
        if (data.getTrackSequenceCollection()!=null) fromTrackSequencesCombobox.setSelectedItem(data.getTrackSequenceCollection());
        else fromTrackDatasetCombobox.setSelectedItem(engine.getDefaultSequenceCollectionName());
        constraints.gridy=1;constraints.gridx=1;
        internal.add(fromTrackSequencesCombobox,constraints);        
        
        fromTrackOrderCombobox=new JComboBox(new Integer[]{0,1,2,3,4,5});
        fromTrackOrderCombobox.setSelectedItem(new Integer(data.getOrder()));
        constraints.gridy=2;constraints.gridx=1;
        internal.add(fromTrackOrderCombobox,constraints);
        
        fromTrackStrandCombobox=new JComboBox(new String[]{"Relative","Direct"});
        if (data.isTrackFromRelativeStrand()) fromTrackStrandCombobox.setSelectedItem("Relative");
        else fromTrackStrandCombobox.setSelectedItem("Direct");        
        constraints.gridy=3;constraints.gridx=1;
        internal.add(fromTrackStrandCombobox,constraints);   
        
        fromTrackPanel.add(internal);
    }

    private void setupImportModelPanel() {  
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(BackgroundModel.class);
        //dataformats.remove(engine.getDataFormat("Plain"));
        importModelPanel=new LoadFromFilePanel(dataformats,gui,BackgroundModel.class);
    }
    
    
    private void setupDistributionPanel() {
        distributionPanel = new JPanel();
        final DistributionGraphPanel graphPanel = new DistributionGraphPanel(0,3);
        graphPanel.setLayout(new BorderLayout());
        distributionPanel.setLayout(new BorderLayout());
        distributionPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        distributionPanel.add(graphPanel,BorderLayout.CENTER);
        JPanel controlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));        
        distributionPanel.add(controlsPanel,BorderLayout.SOUTH);
        final JComboBox maxOrderCombobox=new JComboBox(new String[]{"1-mer","2-mer","3-mer","4-mer"});
        final JComboBox minOrderCombobox=new JComboBox(new String[]{"1-mer","2-mer","3-mer","4-mer"});
        minOrderCombobox.setSelectedIndex(0);
        maxOrderCombobox.setSelectedIndex(3);
        controlsPanel.add(new JLabel("   K-mer range  "));
        controlsPanel.add(minOrderCombobox);
        controlsPanel.add(new JLabel("  to  "));
        controlsPanel.add(maxOrderCombobox);
        ActionListener rangeListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedMin=minOrderCombobox.getSelectedIndex();
                int selectedMax=maxOrderCombobox.getSelectedIndex();
                if (selectedMin>selectedMax) { // swap 
                    int swap=selectedMin;
                    selectedMin=selectedMax;
                    selectedMax=swap;
                }
                graphPanel.setupGraph(selectedMin, selectedMax);
                graphPanel.repaint();
            }
        };
        minOrderCombobox.addActionListener(rangeListener);
        maxOrderCombobox.addActionListener(rangeListener);
    }    
    
    private JTable getRowHeaderView(int order) {
        int size=(int)Math.pow(4,order);
        String[][] prefixes=new String[size][1];
        for (int i=0;i<size;i++) {
            prefixes[i]=new String[]{BackgroundModel.integerToPattern(i, order)};
        }        
        DefaultTableModel rowHeaders=new DefaultTableModel(prefixes,new String[]{"Prefix"});
        JTable header=new JTable(rowHeaders);
        header.setFillsViewportHeight(true);
        header.getColumn(header.getColumnName(0)).setMaxWidth(rowheadersize);
        header.getTableHeader().setReorderingAllowed(false);
        header.setEnabled(false);
        Dimension d=new Dimension(rowheadersize,170);
        header.setMinimumSize(d);     
        return header;
    }
    
    private JTable getOligoRowHeaderView(int order) {
        int size=4;
        if (order>1) size=(int)Math.pow(4,order);
        String[][] prefixes=new String[size][1];
        for (int i=0;i<size;i++) {
            prefixes[i]=new String[]{BackgroundModel.integerToPattern(i, (order==0)?1:order)};
        }        
        DefaultTableModel rowHeaders=new DefaultTableModel(prefixes,new String[]{"Oligo"});
        JTable header=new JTable(rowHeaders);
        header.setFillsViewportHeight(true);
        header.getColumn(header.getColumnName(0)).setMaxWidth(rowheadersize);
        header.getTableHeader().setReorderingAllowed(false);
        header.setEnabled(false);
        Dimension d=new Dimension(rowheadersize,170);
        header.setMinimumSize(d);
        return header;
    }
    
    
    private Double[][] getDefaultModel(int order) {
        int size=(int)Math.pow(4,order);
        Double[][] values=new Double[size][4];
        for (int i=0;i<size;i++) {
            for (int j=0;j<4;j++) values[i][j]=new Double(0.25f);
        }        
        return values;
    }
    
    private Double[][] getDefaultOligoModel(int order) {
        int size=4;
        if (order>1) size=(int)Math.pow(4,order);
        double freq=1.0f/size;
        Double[][] values=new Double[size][1];
        for (int i=0;i<size;i++) {
           values[i][0]=new Double(freq);
        }        
        return values;
    }
    
    
    @Override
    public boolean onOKPressed() {
        if (tabbedPanel.getSelectedComponent()==predefinedModelsPanel) {
             int row=predefinedModelTable.getSelectedRow();
             if (row<0) return false;
             String modelname=(String)predefinedModelTable.getValueAt(row, 0);
             data.setPredefinedModel(modelname);
             if (showExisting) {
                 try {
                     data=importPredefinedModel(data);
                 } catch (Exception e) {
                      //e.printStackTrace(System.err);
                      reportPredefinedModelError(e.getMessage());
                      return false;                    
                 }
             }
        } else if (tabbedPanel.getSelectedComponent()==importModelPanel) { 
            try {
                String filename=importModelPanel.getFilename();
                if (filename==null) throw new ExecutionError("Missing filename");
                DataFormat format=importModelPanel.getDataFormat();
                ParameterSettings settings=importModelPanel.getParameterSettings();
                data=(BackgroundModel)importModelPanel.loadData(data,BackgroundModel.getType());
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing background model from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==fromTrackPanel) {
            try {
                String trackName=(String)fromTrackDatasetCombobox.getSelectedItem();
                if (trackName==null || trackName.isEmpty()) throw new ExecutionError("Missing track");
                int order=(Integer)fromTrackOrderCombobox.getSelectedItem();
                String strandString=(String)fromTrackStrandCombobox.getSelectedItem();
                String seqColString=(String)fromTrackSequencesCombobox.getSelectedItem();                
                boolean relativeOrientation=(strandString.equalsIgnoreCase("Relative"));
                DNASequenceDataset datatrack=null;
                if (engine.dataExists(trackName, DNASequenceDataset.class)) datatrack=(DNASequenceDataset)engine.getDataItem(trackName);
                else throw new ExecutionError(trackName+" is not a DNA Sequence Dataset");
                SequenceCollection collection=null;
                if (seqColString!=null && !seqColString.equals(engine.getDefaultSequenceCollectionName())) {
                    if (engine.dataExists(seqColString, SequenceCollection.class)) collection=(SequenceCollection)engine.getDataItem(seqColString);
                    else throw new ExecutionError(seqColString+" is not a Sequence Collection");   
                }
                if (showExisting) { // create a complete new model so that it can be compared with (and replace) the current data object
                    createDataObjectInBackground(new Object[]{"track",datatrack,collection,order,relativeOrientation}); // this returns right away but spawns a lengthy background process which will eventually close the prompt             
                    return false; // keeps the dialog open until the background task is finished         
                } else { // the prompt is shown in response to an "Add New Background Model" request. Just set the basic parameters needed for a command line. A "new" operation task will be run later to create the finished data object and this avoids creating the model twice
                    data=new BackgroundModel("dummy");
                    data.setTrackName(trackName);
                    if (collection!=null) data.setTrackSequenceCollection(collection.getName());            
                    data.setTrackFromRelativeStrand(relativeOrientation);
                    data.setOrder(order);
                }
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while generating model from track:\n"+exceptionText+e.getMessage(),"Model Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel) {
            int order=((Integer)orderCombobox.getSelectedItem()).intValue();
            double[] newSNF=new double[4];
            for (int col=0;col<4;col++) {
                String txt=snfTableModel.getValueAt(0, col).toString();
                try {
                    newSNF[col]=Double.parseDouble(txt);
                } catch (NumberFormatException e) {
                    reportManualEntryError("Value in SNF column "+(col+1)+" is not a valid number");
                    return false;
                }
            }
            double sumSNF=newSNF[0]+newSNF[1]+newSNF[2]+newSNF[3];
            if (sumSNF>1.02 || sumSNF<0.98) {
                reportManualEntryError("Values in SNF should sum to 1.0 (Currently "+sumSNF+")");
                return false;
            }    
            int oligorows=4;
            if (order>1) oligorows=((int)Math.pow(4, order));  
            int oligoorder=order;
            if (oligoorder==0) oligoorder=1;
            double[] newOligos=new double[oligorows];
            for (int row=0;row<oligorows;row++) {
                String txt=oligoTableModel.getValueAt(row, 0).toString();
                try {
                    newOligos[row]=Double.parseDouble(txt);
                } catch (NumberFormatException e) {
                    String prefix=BackgroundModel.integerToPattern(row, oligoorder);
                    reportManualEntryError("Oligo frequency value for "+(prefix)+" is not a valid number");
                    return false;
                }
            }
            double oligosum=0;
            for (int row=0;row<oligorows;row++) oligosum+=newOligos[row];
            if (oligosum>1.02 || oligosum<0.98) {
                reportManualEntryError("Sum of Oligo frequencies should be 1.0 (currently "+oligosum+")");
                return false;
            }                                   
            int rows=((int)Math.pow(4, order));            
            int size=rows*4;
            double[] newMatrix=new double[size];
            int i=0;
            for (int r=0;r<rows;r++) {
                for (int c=0;c<4;c++) {
                   String txt=matrixTableModel.getValueAt(r, c).toString();
                   try {
                       newMatrix[i]=Double.parseDouble(txt);
                       //System.err.println("value("+r+","+c+")[i="+i+"] set to=> "+Double.parseDouble(txt));
                   } catch (NumberFormatException e) {
                       String prefix=BackgroundModel.integerToPattern(r, order);
                       String nextbase=BackgroundModel.integerToPattern(c, 1);
                       reportManualEntryError("Value in transition matrix("+prefix+","+nextbase+") is not a valid number");
                       return false;
                   }                   
                i++;
                }
                double rowsum=newMatrix[r*4]+newMatrix[r*4+1]+newMatrix[r*4+2]+newMatrix[r*4+3];
                if (rowsum>1.02 || rowsum<0.98) {
                   String prefix=BackgroundModel.integerToPattern(r, order);
                   reportManualEntryError("Values in transition matrix row="+prefix+" should sum to 1.0 (Currently "+rowsum+")");
                   return false;
                }                
            }
            data.setValue(newSNF, newOligos,newMatrix);
        }
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);
        return true;
    }
    
    @Override
    public Data createDataObject(final Object[] parameters, OperationTask task) throws Exception {
        if (parameters==null || parameters.length==0) return new BackgroundModel("dummy");
        if (((String)parameters[0]).equals("track")) {
            DNASequenceDataset datatrack=(DNASequenceDataset)parameters[1];
            SequenceCollection collection=(SequenceCollection)parameters[2];
            int order=(Integer)parameters[3];
            boolean relativeOrientation=(Boolean)parameters[4];
            return BackgroundModel.createModelFromDNATrack(datatrack,collection,order,relativeOrientation,task);
        } else return new BackgroundModel("dummy");
    }
    
    @Override
    public Data getData() {
       return data; 
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof BackgroundModel) data=(BackgroundModel)newdata; 
    }    
    

    private void reportManualEntryError(String msg) {
        if (msg==null) msg="NULL";
        manualPanelErrorLabel.setText(msg);
    }
    
    private void reportPredefinedModelError(String msg) {
        if (msg==null) msg="NULL";
        predefinedPanelErrorLabel.setText(msg);
    }
    
    
    @Deprecated
    private BackgroundModel importModelFromFile(String filename, BackgroundModel model) throws IOException, ParseError, SystemError, InterruptedException {
        // this is executed on the EDT for now
        File file=engine.getFile(filename);
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(stream)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw e;
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader in Prompt_BackgroundModel.importModelFromFile: "+ioe.getMessage());}
        }        
        DataFormat format=engine.getDataFormat("INCLUSive_Background_Model");
        if (format==null) throw new SystemError("Unknown DataFormat: INCLUSive_Background_Model");
        model=(BackgroundModel)format.parseInput(input, model,null, null);
        return model;
    }

    private BackgroundModel importPredefinedModel(BackgroundModel data) throws ExecutionError {
        String modelName=data.getPredefinedModel();
        if (modelName==null) throw new ExecutionError("SYSTEM ERROR: No predefined model defined");
        String filename=BackgroundModel.getFilenameForModel(modelName);
        if (filename==null) throw new ExecutionError("Unknown Background Model: "+modelName);
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            inputStream=new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/motiflab/engine/resources/"+filename)));
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading predefined Background Model:\n["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader Prompt_BackgroundModel.importPredefinedModel(): "+ioe.getMessage());}
        }           
        DataFormat_INCLUSive_Background format=new DataFormat_INCLUSive_Background();   
        try {data=(BackgroundModel)format.parseInput(input, data,null,null);}
        catch (Exception e) {throw new ExecutionError(e.getClass().getSimpleName()+":"+e.getMessage());} 
        data.setPredefinedModel(modelName);
        return data;        
    }
    
    
    
    
    
private class CellRenderer_Organism extends DefaultTableCellRenderer {

    public CellRenderer_Organism() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);              
    }
    @Override
    public void setValue(Object value) {
       if (value==null) {setText("");}
       else if (value instanceof Integer) {
           if (((Integer)value).intValue()==0) setForeground(java.awt.Color.RED);
           else setForeground(java.awt.Color.BLACK);
           setText(Organism.getCommonName(((Integer)value).intValue()));
       }  else {
           setForeground(java.awt.Color.RED);
           setText("* * *");
       }
   }
}// end class CellRenderer_Organism

private class CellRenderer_RightAlign extends DefaultTableCellRenderer {
    public CellRenderer_RightAlign() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);              
    }
    @Override
    public void setValue(Object value) {
       if (value!=null)setText(value.toString());
       else setText("");
    }
}// end class CellRenderer_RightAlign    
    
/** A panel which draws a k-mer histogram */
private class DistributionGraphPanel extends JPanel {
        int bincount=256;
        double[] bins_0; // bins for 1-mers (or SNF)  4 different
        double[] bins_1; // bins for 2-mers     16 different
        double[] bins_2; // bins for 3-mers     64 different
        double[] bins_3; // bins for 4-mers     256 different
//        int width=bincount*2; // Note: this width must be an integer value X bincount (so that each bin will be an integer number of pixels wide)
//        int height=366;
        //BufferedImage image;
        int maxOrder=3; // the highest order shown; 3 => 4-mers
        int minOrder=0; // the smallest order shown; 0 => 1-mers
        double maxY=0;

        DistributionGraphPanel(int minOrder, int maxOrder) {            
            setupGraph(minOrder, maxOrder);
        }

        protected final void setupGraph(int minOrder, int maxOrder) {
            if (data==null) return;
            int order=data.getOrder();            
            if (maxOrder>order) maxOrder=order;
            if (minOrder>order) minOrder=order;
            this.minOrder=minOrder;
            this.maxOrder=maxOrder;
            //if (image==null) image=new BufferedImage(width+100,height+40,BufferedImage.TYPE_INT_RGB);                         
            double[] oligoFreqs=BackgroundModel.convertBackgroundToFrequencyFormat(data);
            maxY=0;
            int startRange=0;
            int endRange=340;
                 if (minOrder==0) startRange=0;
            else if (minOrder==1) startRange=4;
            else if (minOrder==2) startRange=20; //  4+16
            else if (minOrder==3) startRange=84; // 20+64
                 if (maxOrder==0) endRange=4;
            else if (maxOrder==1) endRange=20;
            else if (maxOrder==2) endRange=84;
            else if (maxOrder==3) endRange=340; // 84+256          
            for (int i=startRange;i<endRange;i++) { // find highest value that in the range. This is the highest value shown
                if (oligoFreqs[i]>maxY) maxY=oligoFreqs[i];
            }
            maxY=maxY*1.05; // increase maxY a little bit to get some margin         
//            Graphics2D g=image.createGraphics();
//            g.setColor(this.getBackground());
//            g.fillRect(0, 0, width+110, height+100);
//            Graph graph=new Graph(g, 0, bincount, 0, maxY, width, height, 50, 30);
//            g.setColor(Color.WHITE);
//            g.fillRect(graph.getXforValue(0),graph.getYforValue(maxY), width, height);
//            graph.drawAxes(Graph.BOX, Graph.NONE, Graph.NONE, true, false, true, false, true);
//            g.setColor(Color.LIGHT_GRAY);
//            int yTop=graph.getYforValue(maxY);
//            int yBottom=graph.getYforValue(0);
//            g.setStroke(Graph.DOTTED_STROKE);
//            for (int i=1;i<16;i++) {
//                int x=graph.getXforValue(i*16);
//                g.drawLine(x, yTop, x, yBottom);
//            }
//            g.setColor(Color.DARK_GRAY);
//            g.setStroke(Graph.LINE_STROKE);
//            for (int i=1;i<4;i++) {
//                int x=graph.getXforValue(i*64);
//                g.drawLine(x, yTop, x, yBottom);
//            }
//            Color useColor=new Color(0,0,255,80); // transparent blue
//            g.setColor(useColor);
            fillBins(order,oligoFreqs);          
//            if (minOrder<=0 && maxOrder>=0) graph.drawHistogram(bins_0,false);
//            if (minOrder<=1 && maxOrder>=1) graph.drawHistogram(bins_1,false);
//            if (minOrder<=2 && maxOrder>=2) graph.drawHistogram(bins_2,false);
//            if (minOrder<=3 && maxOrder>=3) graph.drawHistogram(bins_3,false);
//            g.setColor(Color.BLACK);
//            graph.drawBoundingBox();   
//            // Draw  A,C,G,T labels above the histogram
//            int ty=24;
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g.drawString("A", graph.getXforValue(bincount*1.0/8.0)-3, ty);
//            g.drawString("C", graph.getXforValue(bincount*3.0/8.0)-3, ty);
//            g.drawString("G", graph.getXforValue(bincount*5.0/8.0)-3, ty);
//            g.drawString("T", graph.getXforValue(bincount*7.0/8.0)-3, ty);
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        
        private void drawGraph(Graphics graphics, int width, int height) {
            //if (image!=null) graphics.drawImage(image,0,0,null);
            Graphics2D g=(Graphics2D)graphics;
            g.setColor(this.getBackground());
            g.fillRect(0, 0, width+110, height+100);
            Graph graph=new Graph(g, 0, bincount, 0, maxY, width, height, 50, 30);
            g.setColor(Color.WHITE);
            g.fillRect(graph.getXforValue(0),graph.getYforValue(maxY), width, height);
            graph.drawAxes(Graph.BOX, Graph.NONE, Graph.NONE, true, false, true, false, true);
            g.setColor(Color.LIGHT_GRAY);
            int yTop=graph.getYforValue(maxY);
            int yBottom=graph.getYforValue(0);
            g.setStroke(Graph.DOTTED_STROKE);
            for (int i=1;i<16;i++) {
                int x=graph.getXforValue(i*16);
                g.drawLine(x, yTop, x, yBottom);
            }
            g.setColor(Color.DARK_GRAY);
            g.setStroke(Graph.LINE_STROKE);
            for (int i=1;i<4;i++) {
                int x=graph.getXforValue(i*64);
                g.drawLine(x, yTop, x, yBottom);
            }
            Color useColor=new Color(0,0,255,80); // transparent blue
            g.setColor(useColor);         
            if (minOrder<=0 && maxOrder>=0) graph.drawHistogram(bins_0,false);
            if (minOrder<=1 && maxOrder>=1) graph.drawHistogram(bins_1,false);
            if (minOrder<=2 && maxOrder>=2) graph.drawHistogram(bins_2,false);
            if (minOrder<=3 && maxOrder>=3) graph.drawHistogram(bins_3,false);
            g.setColor(Color.BLACK);
            graph.drawBoundingBox();   
            // Draw  A,C,G,T labels above the histogram
            int ty=24;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawString("A", graph.getXforValue(bincount*1.0/8.0)-3, ty);
            g.drawString("C", graph.getXforValue(bincount*3.0/8.0)-3, ty);
            g.drawString("G", graph.getXforValue(bincount*5.0/8.0)-3, ty);
            g.drawString("T", graph.getXforValue(bincount*7.0/8.0)-3, ty);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);            
            
        }        
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int height=DistributionGraphPanel.this.getHeight()-40;
            int width=DistributionGraphPanel.this.getWidth()-60;
            width=width-(width%256); // round down to nearest of multiple of 256
            if (width<256) width=256;
            if (height<50) height=50;
            drawGraph(g,width,height);
        }
        
        private void fillBins(int order, double[] oligoFreqs) {
            // fill in SNF
            bins_0 = new double[256];
            for (int i=0;i<64;i++)    bins_0[i]=oligoFreqs[0]; // A
            for (int i=64;i<128;i++)  bins_0[i]=oligoFreqs[1]; // C
            for (int i=128;i<192;i++) bins_0[i]=oligoFreqs[2]; // G
            for (int i=192;i<256;i++) bins_0[i]=oligoFreqs[3]; // T
            if (order>=1) { // fill in dinucleotide frequencies 16
                bins_1 = new double[256];
                for (int i=0;i<256;i++) {
                    bins_1[i]=oligoFreqs[4+(int)(i/16)];
                }           
            }
            if (order>=2) { // fill in trinucleotide frequencies
                bins_2 = new double[256];
                for (int i=0;i<256;i++) {
                    bins_2[i]=oligoFreqs[20+(int)(i/4)];
                }           
            }
            if (order>=3) { // fill in 4-mer frequencies
                bins_3 = new double[256];
                for (int i=0;i<256;i++) bins_3[i]=oligoFreqs[84+i];          
            }
        
        }



      

}

}
