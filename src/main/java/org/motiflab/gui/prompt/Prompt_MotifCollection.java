/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.gui.LoadFromFilePanel;
import org.motiflab.gui.MotifBrowserPanel;
import org.motiflab.gui.MotifLabGUI;


/**
 *
 * @author kjetikl
 */
public class Prompt_MotifCollection extends Prompt {

    private MotifCollection data;

    private MotifBrowserPanel manualSelectionPanel;
    private JPanel predefinedCollectionsPanel;
    private JPanel importCollectionPanel;
    private LoadFromFilePanel loadFromFilePanel;
    private JPanel parseListPanel;
    private JPanel fromTrackPanel;
    private JPanel fromPropertyPanel;
    private JTabbedPane tabbedPanel=null;
    private DefaultTableModel predefinedCollectionsTableModel;
    private JTable predefinedCollectionsTable;
    private JLabel predefinedPanelErrorLabel;
    private JComboBox sequencesupportnumber;
    private JComboBox sequencesupportnumber2;    
    private JComboBox fromTrackSelection;
    private JComboBox fromTrackSelectionSeqCol;
    private JComboBox fromPropertyCombobox;
    private JComboBox fromPropertyOperatorCombobox;    
    private JComboBox sequencesupportoperator;
    private JComboBox sequencesupportpercentageoperator;
    private JScrollPane scrollpane;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;
    private JPanel fromMapPanel;
    private JComboBox fromMapSelection;
    private JComboBox fromMapOperator;
    private JComboBox fromMapFirstOperandCombobox;
    private JComboBox fromMapSecondOperandCombobox;
    private JLabel fromMapToLabel;
    private JLabel fromTrackToLabel;    
    private boolean showExisting=false;
    private boolean isimported=false;

    private JTextArea parseListTextArea;
    private JTextArea fromPropertyTextArea;

    public Prompt_MotifCollection(MotifLabGUI gui, String prompt, MotifCollection dataitem) {
        this(gui,prompt,dataitem,false,"Motif Collection",true);
    }
    
    public Prompt_MotifCollection(MotifLabGUI gui, String prompt, MotifCollection dataitem, boolean modal) {
        this(gui,prompt,dataitem,false,"Motif Collection",modal);
    }

    public Prompt_MotifCollection(MotifLabGUI gui, String prompt, MotifCollection dataitem, boolean showOnlyManualSelection, String title, boolean modal) {
        super(gui,prompt, modal);
        showExisting=(dataitem!=null);
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else data=new MotifCollection(gui.getGenericDataitemName(MotifCollection.class, null));
        setDataItemName(data.getName());
        setTitle(title);
        manualSelectionPanel=new MotifBrowserPanel(gui, data, modal, false);
        setupPredefinedModelsPanel();
        setupFromTrackPanel();
        setupParseListPanel();
        setupFromPropertyPanel();
        setupImportModelPanel();
        setupFromMapPanel();    
        boolean motifsAvailable=engine.hasDataItemsOfType(Motif.class);        
        if (!showOnlyManualSelection) {         
            tabbedPanel=new JTabbedPane();
            tabbedPanel.addTab("Predefined", predefinedCollectionsPanel);
            if (motifsAvailable) tabbedPanel.addTab("Manual Selection", manualSelectionPanel);
            if (motifsAvailable) tabbedPanel.addTab("From List", parseListPanel);
            if (motifsAvailable) tabbedPanel.addTab("From Property", fromPropertyPanel);
            if (motifsAvailable) tabbedPanel.addTab("From Track", fromTrackPanel);
            if (motifsAvailable) tabbedPanel.addTab("From Map", fromMapPanel);        
            tabbedPanel.addTab("Import", importCollectionPanel);            
        }
        JPanel internal=new JPanel(new BorderLayout());
        Dimension size=new Dimension(600,500);
        internal.setMinimumSize(size);
        internal.setPreferredSize(size);
        // internal.setMaximumSize(size);
        manualSelectionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        if (predefinedCollectionsPanel!=null) predefinedCollectionsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        if (importCollectionPanel!=null) importCollectionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromPropertyPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromTrackPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromMapPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        if (!showOnlyManualSelection) internal.add(tabbedPanel);
        else internal.add(manualSelectionPanel);
        this.setResizable(true);
        setMainPanel(internal);        
        pack();
        if (showExisting && !showOnlyManualSelection) {
            if (motifsAvailable) tabbedPanel.setSelectedComponent(manualSelectionPanel);
            else tabbedPanel.setSelectedComponent(predefinedCollectionsPanel);
        }
        if (showExisting) focusOKButton();
    }
    
    private void setupPredefinedModelsPanel() {
        predefinedCollectionsPanel=new JPanel();
        String[] columns = new String[]{"Name","Motifs"};
        Set<String> collectionNames=engine.getPredefinedMotifCollections();        
        Object[][] predefinedcollectionstable = new Object[collectionNames.size()][2];
        int i=0;
        for (String name:collectionNames) {
            predefinedcollectionstable[i][0]=name;
            predefinedcollectionstable[i][1]=new Integer(engine.getSizeForMotifCollection(name));
            i++;
        }
        
        predefinedCollectionsTableModel=new DefaultTableModel(predefinedcollectionstable,columns){
            @Override
            public Class getColumnClass(int col) {
                if (col==1) return Integer.class;
                else  return String.class;
            }
        };
        predefinedCollectionsTable=new JTable(predefinedCollectionsTableModel) {
            @Override
            public boolean isCellEditable(int row,int col) {return false;}
        };
        predefinedCollectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        predefinedCollectionsTable.getColumn("Motifs").setMinWidth(65);
        predefinedCollectionsTable.getColumn("Motifs").setMaxWidth(65);
        predefinedCollectionsTable.getColumn("Motifs").setWidth(65);
        predefinedCollectionsPanel.setLayout(new BorderLayout());
        scrollpane=new JScrollPane(predefinedCollectionsTable);
        predefinedCollectionsTable.setFillsViewportHeight(true);
        predefinedCollectionsTable.setAutoCreateRowSorter(true);
        predefinedCollectionsTable.getTableHeader().setReorderingAllowed(false);
        predefinedCollectionsPanel.add(scrollpane,BorderLayout.CENTER);
        JPanel statuspanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        predefinedPanelErrorLabel=new JLabel("  ");
        predefinedPanelErrorLabel.setFont(errorMessageFont);
        predefinedPanelErrorLabel.setForeground(java.awt.Color.RED);
        statuspanel.add(predefinedPanelErrorLabel);
        predefinedCollectionsPanel.add(statuspanel,BorderLayout.SOUTH);
        predefinedCollectionsTable.getColumn("Motifs").setCellRenderer(new CellRenderer_RightAlign());
        predefinedCollectionsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (showExisting) return;
                int row=predefinedCollectionsTable.getSelectedRow();
                if (row>=0) {
                  String trackname=(String)predefinedCollectionsTable.getValueAt(row, predefinedCollectionsTable.getColumn("Name").getModelIndex());
                  setDataItemName(trackname.replace(" ", "_"));
                } else setDataItemName(data.getName());
            }
        });
        predefinedCollectionsTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount()==2) clickOK();
                 }
        });
        predefinedCollectionsTable.getRowSorter().toggleSortOrder(0);
    }
    
    
    private void setupImportModelPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(MotifCollection.class);
        //dataformats.remove(engine.getDataFormat("Plain"));
        importCollectionPanel=new JPanel(new BorderLayout());
        loadFromFilePanel=new LoadFromFilePanel(dataformats,gui,MotifCollection.class);
        importCollectionPanel.add(loadFromFilePanel,BorderLayout.CENTER);
    }
    
    private void setupParseListPanel() {
        parseListPanel=new JPanel();     
        parseListPanel.setLayout(new BoxLayout(parseListPanel, BoxLayout.Y_AXIS));  
        JPanel internal1=new JPanel(new FlowLayout(FlowLayout.LEADING));        
        JPanel internal2=new JPanel(new BorderLayout());  
        JPanel internal3=new JPanel(new FlowLayout(FlowLayout.LEADING));
        ignoreParseErrors=new JCheckBox("Ignore parse errors", false);
        resolveInProtocol=new JCheckBox("Resolve in protocol", true);
        resolveInProtocol.setToolTipText("<html>If selected, the list will be recorded verbatim in the protocol and references will be resolved when the protocol is executed.<br>If not selected, references will be resolved right away and the resulting list will be recorded in the protocol.<br></html>");
        JLabel label=new JLabel("<html>Enter names of motifs or collections below (separated by commas, semicolons or whitespace).<br>"
                + "Clusters within motif partitions can be referred to by: <tt>   PartitionName->ClusterName</tt>.<br>"
                + "If motif names contain numbered indices, you can specify a range of motifs using <br>"
                + "a colon operator to separate the first and last motif. For example, the entry \"<tt>ab3cd:ab7cd</tt>\" "
                + "will<br>refer to any motif starting with \"ab\" followed by a number between 3 and 7 and ending with \"cd\".<br>"
                + "Prefix entries with <b>&minus;</b> (minus) to subtract from current selection, <b>&</b> to intersect or <b>!</b> to add complement.<br>"
                + "Wildcards (*) are allowed within motif names (except when specifying ranges).</html>");
        
        
        parseListTextArea=new JTextArea();
        internal1.add(label);
        internal2.add(new JScrollPane(parseListTextArea));
        internal3.add(ignoreParseErrors);
        internal3.add(new JLabel("       "));
        internal3.add(resolveInProtocol);
        internal1.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
        internal2.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        internal3.setBorder(BorderFactory.createEmptyBorder(4, 16, 5, 20));
        parseListPanel.add(internal1);
        parseListPanel.add(internal2);
        parseListPanel.add(internal3);
        if (showExisting && data.isFromList()) {
            String list=data.getFromListString();
            list=list.replace(",", "\n");
            parseListTextArea.setText(list);
            resolveInProtocol.setSelected(true);
        }
    }  

    private void setupFromPropertyPanel() {
        String[] userdefined=Motif.getAllUserDefinedProperties(gui.getEngine());
        String[] standard=Motif.getAllStandardProperties(true);
        String[] knownproperties=new String[standard.length+userdefined.length];
        System.arraycopy(standard, 0, knownproperties, 0, standard.length);
        System.arraycopy(userdefined, 0, knownproperties, standard.length, userdefined.length);
        Arrays.sort(knownproperties);               
         
        DefaultComboBoxModel propertyModel=new DefaultComboBoxModel(knownproperties);        
        fromPropertyCombobox=new JComboBox();        
        fromPropertyCombobox.setModel(propertyModel);
        fromPropertyCombobox.setEditable(true);
        fromPropertyCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String propertyName=(String)fromPropertyCombobox.getSelectedItem();
                Class type=Motif.getPropertyClass(propertyName, engine);
                String[] operators=getOperatorsForPropertyClass(type);
                fromPropertyOperatorCombobox.setModel(new DefaultComboBoxModel(operators));
            }
        });
        fromPropertyOperatorCombobox=new JComboBox(); 
        fromPropertyPanel=new JPanel();     
        fromPropertyPanel.setLayout(new BoxLayout(fromPropertyPanel, BoxLayout.Y_AXIS));  
        JPanel internal1=new JPanel(new FlowLayout(FlowLayout.LEADING));        
        JPanel internal2=new JPanel(new BorderLayout());  
        JPanel internal3=new JPanel(new FlowLayout(FlowLayout.LEADING)); 
        internal3.add(new JLabel(" ")); // just to create some room
        JLabel label=new JLabel("Property ");
        fromPropertyTextArea=new JTextArea();
        internal1.add(label);
        internal1.add(fromPropertyCombobox);
        internal1.add(fromPropertyOperatorCombobox); 
        internal2.add(new JScrollPane(fromPropertyTextArea));
        internal1.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
        internal2.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        fromPropertyPanel.add(internal1);
        fromPropertyPanel.add(internal2);
        fromPropertyPanel.add(internal3);
        if (showExisting && data.isFromProperty()) { // initialize fields according to current collection configuration
            String fromPropertyString=data.getFromPropertyString();
            try {
               Object[] parsed=MotifCollection.parsePropertyConfigurationString(fromPropertyString,engine);
               String propertyName=(String)parsed[0];
               String operator=(String)parsed[1];
               String[] operands=(String[])parsed[2];
               String text=MotifLabEngine.splice(operands, "\n");
               fromPropertyTextArea.setText(text);
               fromPropertyCombobox.setSelectedItem(propertyName);
               fromPropertyOperatorCombobox.setSelectedItem(operator);
            } catch (ParseError p) {
                //
            }
        } else fromPropertyCombobox.setSelectedItem(standard[0]);      
    }     
    
    private String[] getOperatorsForPropertyClass(Class type) { 
        if (type==null) return new String[]{"Unknown property"};
             if (type.equals(Boolean.class)) return new String[]{"=","<>"};
        else if (Number.class.isAssignableFrom(type)) return new String[]{"=","<=","<",">=",">","<>","in","not in"};
        else if (type.equals(String.class) || type.equals(ArrayList.class)) return new String[]{"equals","not equals","matches", "not matches","in","not in"};
        else return new String[]{"N/A"};
    }
    
    private void setupFromTrackPanel() {
        fromTrackPanel=new JPanel();  
        fromTrackPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        JPanel upperControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel lowerControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel seqColControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal=new JPanel();
        internal.setLayout(new GridBagLayout());
        internal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 6));
        JLabel label1=new JLabel("Motif track");
        JLabel label2=new JLabel("Sequence support");
        JLabel label3=new JLabel("Sequence collection");        
        DefaultComboBoxModel seqColmodel=new DefaultComboBoxModel();
        ArrayList<String> seqColList=engine.getNamesForAllDataItemsOfType(SequenceCollection.class);
        for (String seqColName:seqColList) {
            seqColmodel.addElement(seqColName);
        }
        fromTrackSelectionSeqCol=new JComboBox(seqColmodel);    
        fromTrackSelectionSeqCol.setSelectedItem(engine.getDefaultSequenceCollectionName());
        DefaultComboBoxModel tracksmodel=new DefaultComboBoxModel();
        ArrayList<Data> regionsets=engine.getAllDataItemsOfType(RegionDataset.class);
        for (Data regiondata:regionsets) {
            if (((RegionDataset)regiondata).isMotifTrack()) tracksmodel.addElement(regiondata.getName());
        }
        fromTrackSelection=new JComboBox(tracksmodel);
        sequencesupportoperator=new JComboBox(new String[]{"=",">",">=","<","<=","<>","in","not in"});   
        sequencesupportoperator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String operator=(String)sequencesupportoperator.getSelectedItem();
                boolean showSecondOperator=(operator.equals("in") || operator.equals("not in"));
                fromTrackToLabel.setVisible(showSecondOperator);
                sequencesupportnumber2.setVisible(showSecondOperator);
            }
        });           
        sequencesupportpercentageoperator=new JComboBox(new String[]{" ","%"});
        ArrayList<String> mapOperandValues=new ArrayList<String>();
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(NumericVariable.class));
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(MotifNumericMap.class));        
        sequencesupportnumber=new JComboBox(mapOperandValues.toArray());
        sequencesupportnumber.setEditable(true);
        sequencesupportnumber.setSelectedItem("1");
        sequencesupportnumber2=new JComboBox(mapOperandValues.toArray());
        sequencesupportnumber2.setEditable(true);
        sequencesupportnumber2.setSelectedItem("100");   
        fromTrackToLabel=new JLabel("  to ");
        upperControls.add(fromTrackSelection);
        lowerControls.add(sequencesupportoperator);
        lowerControls.add(sequencesupportnumber);
        lowerControls.add(fromTrackToLabel);
        lowerControls.add(sequencesupportnumber2);
        lowerControls.add(sequencesupportpercentageoperator);
        lowerControls.add(new JLabel(" "));
        seqColControls.add(fromTrackSelectionSeqCol);        
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.anchor=GridBagConstraints.BELOW_BASELINE_LEADING;
        constraints.gridheight=1;
        constraints.gridwidth=1;
        constraints.ipadx=10;
        constraints.gridy=0;
        constraints.gridx=0;
        constraints.gridwidth=1;
        constraints.fill=GridBagConstraints.NONE;
        internal.add(label1,constraints);
        constraints.gridx=1;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        internal.add(upperControls,constraints);
        constraints.gridy=1;
        constraints.gridx=0;
        constraints.fill=GridBagConstraints.NONE;
        internal.add(label2,constraints);
        constraints.gridx=1;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        constraints.gridwidth=GridBagConstraints.REMAINDER;
        internal.add(lowerControls,constraints);
        constraints.gridy=2;
        constraints.gridx=0;
        constraints.fill=GridBagConstraints.NONE;
        internal.add(label3,constraints);
        constraints.gridx=1;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        constraints.gridwidth=GridBagConstraints.REMAINDER;
        internal.add(seqColControls,constraints);        
        fromTrackPanel.add(internal);
        if (showExisting && data.isFromTrack()) { // initialize fields according to current collection configuration
            String fromTrackString=data.getFromTrackString();
            try {
               String[] parsed=MotifCollection.parseTrackConfigurationString(fromTrackString,engine);
               String trackName=(String)parsed[0];
               String operator=(String)parsed[1];
               String firstOp=(String)parsed[2];
               String secondOp=(String)parsed[3];
               String percentage=(String)parsed[4];
               String seqCollectionName=(String)parsed[5];
               if (percentage.equals(Boolean.TRUE.toString())) sequencesupportpercentageoperator.setSelectedItem("%");
               else sequencesupportpercentageoperator.setSelectedItem(" ");
               sequencesupportnumber.setSelectedItem(firstOp);
               if (secondOp!=null) sequencesupportnumber2.setSelectedItem(secondOp);
               fromTrackSelection.setSelectedItem(trackName);
               sequencesupportoperator.setSelectedItem(operator);
               if (seqCollectionName!=null) fromTrackSelectionSeqCol.setSelectedItem(seqCollectionName);
            } catch (ParseError p) {
                sequencesupportoperator.setSelectedItem(">=");
            }
        }
        else sequencesupportoperator.setSelectedItem(">=");
    }        
    
   private void setupFromMapPanel() {
        fromMapPanel=new JPanel();
        fromMapPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        JPanel upperControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel lowerControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal=new JPanel();
        internal.setLayout(new GridBagLayout());
        internal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label1=new JLabel("Map variable");
        JLabel label2=new JLabel("Map value");
        DefaultComboBoxModel model=new DefaultComboBoxModel();
        ArrayList<Data> mapslist=engine.getAllDataItemsOfType(MotifNumericMap.class);
        for (Data map:mapslist) {
            model.addElement(map.getName());
        }
        fromMapSelection=new JComboBox(model);
        fromMapOperator=new JComboBox(new String[]{"=",">",">=","<","<=","<>","in","not in"});
        fromMapOperator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String operator=(String)fromMapOperator.getSelectedItem();
                boolean showSecondOperator=(operator.equals("in") || operator.equals("not in"));
                fromMapToLabel.setVisible(showSecondOperator);
                fromMapSecondOperandCombobox.setVisible(showSecondOperator);
            }
        });
        ArrayList<String> mapOperandValues=new ArrayList<String>();
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(NumericVariable.class));
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(MotifNumericMap.class));        
        fromMapFirstOperandCombobox=new JComboBox(mapOperandValues.toArray());
        fromMapFirstOperandCombobox.setEditable(true);
        fromMapFirstOperandCombobox.setSelectedItem("0");
        fromMapSecondOperandCombobox=new JComboBox(mapOperandValues.toArray());
        fromMapSecondOperandCombobox.setEditable(true);
        fromMapSecondOperandCombobox.setSelectedItem("1");
        fromMapToLabel=new JLabel("  to ");
        upperControls.add(fromMapSelection);
        lowerControls.add(fromMapOperator);
        lowerControls.add(fromMapFirstOperandCombobox);
        lowerControls.add(fromMapToLabel);
        lowerControls.add(fromMapSecondOperandCombobox);
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.anchor=GridBagConstraints.BELOW_BASELINE_LEADING;
        constraints.gridheight=1;
        constraints.gridwidth=1;
        constraints.ipadx=10;
        constraints.gridy=0;
        constraints.gridx=0;
        constraints.gridwidth=1;
        constraints.fill=GridBagConstraints.NONE;
        internal.add(label1,constraints);
        constraints.gridx=1;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        internal.add(upperControls,constraints);
        constraints.gridy=1;
        constraints.gridx=0;
        constraints.fill=GridBagConstraints.NONE;
        internal.add(label2,constraints);
        constraints.gridx=1;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        constraints.gridwidth=GridBagConstraints.REMAINDER;
        internal.add(lowerControls,constraints);
        fromMapPanel.add(internal);
        if (showExisting && data.isFromMap()) { // initialize fields according to current collection configuration
            String fromMapString=data.getFromMapString();
            try {
               String[] parsed=MotifCollection.parseMapConfigurationString(fromMapString,engine);
               String mapvariable=(String)parsed[0];
               String operator=(String)parsed[1];
               String firstOp=(String)parsed[2];
               String secondOp=(String)parsed[3];
               fromMapFirstOperandCombobox.setSelectedItem(firstOp);
               if (secondOp!=null) fromMapSecondOperandCombobox.setSelectedItem(secondOp);
               fromMapSelection.setSelectedItem(mapvariable);
               fromMapOperator.setSelectedItem(operator);
            } catch (ParseError p) {
                fromMapOperator.setSelectedItem(">=");
            }
        }
        else fromMapOperator.setSelectedItem(">=");
    }
    
    
    @Override
    public boolean onOKPressed() {            
        if (tabbedPanel==null || tabbedPanel.getSelectedComponent()==manualSelectionPanel) {
            manualSelectionPanel.updateMotifCollection();           
        } else if (tabbedPanel.getSelectedComponent()==predefinedCollectionsPanel) {
             int row=predefinedCollectionsTable.getSelectedRow();
             if (row<0) return false;
             String collectionname=(String)predefinedCollectionsTable.getValueAt(row, 0);
             data.setPredefinedCollectionName(collectionname);
             if (showExisting) {
                 try {
                    data=importPredefinedCollection(data); // replace already existing collection. This will not result in "new" operation
                 } catch (Exception e) {
                    //e.printStackTrace(System.err);
                    reportPredefinedCollectionlError(e.getMessage());
                    return false;                    
                 }
             }
        } else if (tabbedPanel.getSelectedComponent()==importCollectionPanel) { 
            try {
                String filename=loadFromFilePanel.getFilename();
                if (filename==null) throw new SystemError("Missing filename");
                DataFormat format=loadFromFilePanel.getDataFormat();
                ParameterSettings settings=loadFromFilePanel.getParameterSettings();
                data=(MotifCollection)loadFromFilePanel.loadData(data,MotifCollection.getType());
                isimported=true;
                data.setPredefinedCollectionName(null);
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String errorType=(e instanceof IOException)?"I/O-error":"error";
                JOptionPane.showMessageDialog(this, "An "+errorType+" occurred while importing collection from file:\n\n"+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==fromTrackPanel) {
            String trackName=(String)fromTrackSelection.getSelectedItem();
            if (trackName!=null && !trackName.isEmpty()) {
                Data track=engine.getDataItem(trackName);
                boolean percentage=false;
                if (track instanceof RegionDataset) {
                   String selectionoperation=(String)sequencesupportoperator.getSelectedItem(); 
                   String quorum=(String)sequencesupportnumber.getSelectedItem(); 
                   String quorum2=(String)sequencesupportnumber2.getSelectedItem();
                   if (quorum!=null) quorum=quorum.trim();
                   if (quorum2!=null) quorum2=quorum2.trim();
                   if (!(selectionoperation.equals("in") || selectionoperation.equals("not in"))) quorum2=null;
                   String percentagestring=(String)sequencesupportpercentageoperator.getSelectedItem(); 
                   if (percentagestring!=null && percentagestring.equals("%")) percentage=true;       
                   String seqColName=(String)fromTrackSelectionSeqCol.getSelectedItem();
                   if (seqColName.equals(engine.getDefaultSequenceCollectionName())) seqColName=null;
                   String fromTrackString=null;
                   // this is based on the syntax recognized by MotifCollection
                   if (selectionoperation.equals("in") || selectionoperation.equals("not in")) fromTrackString=trackName+",support "+selectionoperation+" ["+quorum+","+quorum2+"]"+((percentage)?"%":""); //
                   else fromTrackString = trackName + ",support" + selectionoperation + quorum + ((percentage) ? "%" : ""); //                  
                   if (seqColName!=null) fromTrackString+=",collection="+seqColName;         
                   if (showExisting) { // create a complete new map so that it can be compared with (and replace) the current data object
                        createDataObjectInBackground(new Object[]{"track",data.getName(),fromTrackString}); // this returns right away but spawns a lengthy background process which will eventually close the prompt             
                        return false; // keeps the dialog open until the background task is finished         
                   } else { // the prompt is shown in response to an "Add New Motif Collection" request. Just set the basic parameters needed for a command line. A "new" operation task will be run later to create the finished data object and this avoids creating the object twice
                        data=new MotifCollection(data.getName());
                        data.setFromTrackString(fromTrackString);
                   }                     
                }                    
            }      
        } else if (tabbedPanel.getSelectedComponent()==fromMapPanel) {            
            String mapName=(String)fromMapSelection.getSelectedItem();
            if (mapName!=null && !mapName.isEmpty()) {
                Data numericmap=engine.getDataItem(mapName);
                if (numericmap instanceof MotifNumericMap) {
                   String operator=(String)fromMapOperator.getSelectedItem();
                   String firstOperandString=(String)fromMapFirstOperandCombobox.getSelectedItem();
                   if (firstOperandString!=null) firstOperandString=firstOperandString.trim();
                   String secondOperandString=(String)fromMapSecondOperandCombobox.getSelectedItem();
                   if (secondOperandString!=null) secondOperandString=secondOperandString.trim();
                   try {
                     data.createCollectionFromMap((MotifNumericMap)numericmap, operator, firstOperandString, secondOperandString, engine);
                   } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                   }                     
               }
            }
        } else if (tabbedPanel.getSelectedComponent()==fromPropertyPanel) {            
            String propertyName=(String)fromPropertyCombobox.getSelectedItem();
            String operator=(String)fromPropertyOperatorCombobox.getSelectedItem();
            String text=fromPropertyTextArea.getText();
            if (text==null) text=""; else text=text.trim();
            text=text.replaceAll("\n", ",");
            text=text.replaceAll(",\\s*,", ",,"); // remove 'blank' entries
            text=text.replaceAll(",+", ","); // remove 'blank' entries
            String[] operands=text.split("\\s*,\\s*");
            if (text==null) text="";
            try {
               data.createCollectionFromProperty(propertyName, operator, operands, engine);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }                                             
        } else if (tabbedPanel.getSelectedComponent()==parseListPanel) {           
            boolean reportErrors=!ignoreParseErrors.isSelected();
            String text=parseListTextArea.getText();
            text=text.replaceAll("\\s+", ",");
            text=text.replace(";", ",");
            text=text.replaceAll(",+", ",");
            text=text.replaceAll("^,", ""); // remove leading comma
            text=text.replaceAll(",$", ""); // remove trailing comma
            ArrayList<String> notfound=new ArrayList<String>();
            MotifCollection parsedCollection=null;
            try {
                parsedCollection=MotifCollection.parseMotifCollectionParameters(text, data.getName(), notfound, engine, null);
            } catch (Exception e) {} // exceptions will not be thrown when parameter (notfound != null)

            if (reportErrors) {
                if (notfound.size()>15) {
                    String errormsg=notfound.size()+" motifs (or collections) were not found:\n\n";
                    JOptionPane.showMessageDialog(this, errormsg, "Unknown motifs/collections", JOptionPane.ERROR_MESSAGE);
                    return false;
                } else if (notfound.size()>0) {
                    String errormsg=(notfound.size()==1)?"The following motif/collection was not found:\n\n":"The following "+notfound.size()+" motifs/collections were not found:\n\n";
                    for (int i=0;i<notfound.size()-1;i++) {
                         errormsg+=notfound.get(i)+"\n";
                    }
                    errormsg+=notfound.get(notfound.size()-1);
                    JOptionPane.showMessageDialog(this, errormsg, "Unknown motifs/collections", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else { // silent mode (only report errors in log) and return collection anyway
                for (String error:notfound) engine.logMessage(error);
            }
            if (parsedCollection!=null) data.importData(parsedCollection);
            else data.clearAll(engine);
            if (resolveInProtocol.isSelected()) data.setFromListString(text);
        }
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);
        return true;
    }
    
    @Override
    public Data getData() {
       return data; 
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof MotifCollection) data=(MotifCollection)newdata; 
    }     
    
    @Override
    public Data createDataObject(final Object[] parameters, OperationTask task) throws Exception {
        if (parameters==null || parameters.length==0) return new MotifCollection("dummy");
        if (((String)parameters[0]).equals("track")) {
            String targetName=(String)parameters[1];
            String parameter=(String)parameters[2];
            return MotifCollection.parseMotifCollectionParameters(Operation_new.FROM_TRACK_PREFIX+parameter, targetName, null, engine, task);
        } else return new MotifCollection("dummy");
    }     
    
    public boolean isImported() {
        return isimported;
    }
    

    private void reportPredefinedCollectionlError(String msg) {
        if (msg==null) msg="NULL";
        predefinedPanelErrorLabel.setText(msg);
    }
    

    
    private MotifCollection importPredefinedCollection(MotifCollection data) throws ExecutionError {
        String collectionName=data.getPredefinedCollectionName();
        if (collectionName==null) throw new ExecutionError("SYSTEM ERROR: No predefined collection defined");
        String filename=engine.getFilenameForMotifCollection(collectionName);
        String formatname="MotifLabMotif";
        if (filename==null) throw new ExecutionError("Unknown Motif Collection: "+collectionName);
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            //inputStream=new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/org/motiflab/engine/resources/"+filename)));
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(filename))))); // these files will always be installed locally
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading predefined Motif Collection:\n["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader in Prompt_MotifCollection.importPredefinedCollection(): "+ioe.getMessage());}
        }           
        DataFormat format = engine.getDataFormat(formatname);
        if (format==null) throw new ExecutionError("Unknown Dataformat: "+formatname);    
        try {data=(MotifCollection)format.parseInput(input, data,null,null);}
        catch (Exception e) {throw new ExecutionError(e.getClass().getSimpleName()+":"+e.getMessage());} 
        data.setPredefinedCollectionName(collectionName);
        return data;        
    }
    
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

}
