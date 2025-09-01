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
import javax.swing.JButton;
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
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.DataTypeTable;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.protocol.StandardParametersParser;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.gui.LoadFromFilePanel;
import org.motiflab.gui.ModuleBrowserPanel;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.ParametersPanel;
import static org.motiflab.gui.prompt.Prompt.errorMessageFont;


/**
 *
 * @author kjetikl
 */
public class Prompt_ModuleCollection extends Prompt {

    private ModuleCollection data;

    private ModuleBrowserPanel manualSelectionPanel;
    private JPanel predefinedCollectionsPanel;    
    private JPanel importCollectionPanel;
    private LoadFromFilePanel loadFromFilePanel;
    private JPanel parseListPanel;
    private JPanel fromPropertyPanel;    
    private JPanel fromInteractionsPanel;
    private JTabbedPane tabbedPanel=null;    
    private boolean showExisting=false;
    private boolean isimported=false;

    private DefaultTableModel predefinedCollectionsTableModel;
    private JTable predefinedCollectionsTable;
    private JLabel predefinedPanelErrorLabel; 
    private JScrollPane scrollpane;    
    
    private JTextArea parseListTextArea;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;
         
    private JPanel fromMapPanel;
    private JComboBox fromMapSelection;
    private JComboBox fromMapOperator;
    private JComboBox fromMapFirstOperandCombobox;
    private JComboBox fromMapSecondOperandCombobox;
    private JLabel fromMapToLabel;    
    
    private JComboBox fromPropertyCombobox;
    private JComboBox fromPropertyOperatorCombobox;    
    private JTextArea fromPropertyTextArea;    
    
    private JPanel fromTrackPanel;
    private JLabel fromTrackToLabel;   
    private JComboBox fromTrackSelection;
    private JComboBox fromTrackSelectionSeqCol;    
    private JComboBox sequencesupportoperator;
    private JComboBox sequencesupportpercentageoperator; 
    private JComboBox sequencesupportnumber;
    private JComboBox sequencesupportnumber2;      
    
    private ParametersPanel fromInteractionsParametersPanel;


    public Prompt_ModuleCollection(MotifLabGUI gui, String prompt, ModuleCollection dataitem) {
        this(gui,prompt,dataitem,true,null);
    }
    
    public Prompt_ModuleCollection(MotifLabGUI gui, String prompt, ModuleCollection dataitem, boolean modal) {
        this(gui,prompt,dataitem,modal,null);
    }    

    public Prompt_ModuleCollection(MotifLabGUI gui, String prompt, ModuleCollection dataitem, DataTypeTable table) {
        this(gui,prompt,dataitem,true,table);
    }    

    public Prompt_ModuleCollection(MotifLabGUI gui, String prompt, ModuleCollection dataitem, boolean modal, DataTypeTable table) {
        super(gui,prompt, modal,null,table);
        showExisting=(dataitem!=null);
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else data=new ModuleCollection(gui.getGenericDataitemName(ModuleCollection.class, null));
        setDataItemName(data.getName());
        setTitle("Module Collection");
        manualSelectionPanel=new ModuleBrowserPanel(gui, data, modal, false);
        setupPredefinedModelsPanel();        
        setupImportModelPanel();
        setupParseListPanel();
        setupFromInteractionsPanel();
        setupFromPropertyPanel();        
        setupFromMapPanel();
        setupFromTrackPanel();
        boolean modulesAvailable=engine.hasDataItemsOfType(ModuleCRM.class);
        boolean motifsAvailable=engine.hasDataItemsOfType(Motif.class);
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Predefined", predefinedCollectionsPanel);        
        if (modulesAvailable) tabbedPanel.addTab("Manual Selection", manualSelectionPanel);
        if (modulesAvailable) tabbedPanel.addTab("From List", parseListPanel);
        if (modulesAvailable) tabbedPanel.addTab("From Property", fromPropertyPanel);        
        if (modulesAvailable) tabbedPanel.addTab("From Track", fromTrackPanel);
        if (modulesAvailable) tabbedPanel.addTab("From Map", fromMapPanel);        
        if (motifsAvailable) tabbedPanel.addTab("From Interactions", fromInteractionsPanel);
        tabbedPanel.addTab("Import Collection", importCollectionPanel);
        JPanel internal=new JPanel(new BorderLayout());
        Dimension size=new Dimension(750,500);
        internal.setMinimumSize(size);
        internal.setPreferredSize(size);
        // internal.setMaximumSize(size);
        manualSelectionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        if (predefinedCollectionsPanel!=null) predefinedCollectionsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));        
        importCollectionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromInteractionsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromMapPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromTrackPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));        
        internal.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(internal);
        pack();
        if (modulesAvailable) tabbedPanel.setSelectedComponent(manualSelectionPanel);
        else tabbedPanel.setSelectedComponent(predefinedCollectionsPanel);
        if (showExisting) focusOKButton();
    }   
    
    private void setupPredefinedModelsPanel() {
        predefinedCollectionsPanel=new JPanel();
        String[] columns = new String[]{"Name","Modules"};
        Set<String> collectionNames=engine.getPredefinedModuleCollections();        
        Object[][] predefinedcollectionstable = new Object[collectionNames.size()][2];
        int i=0;
        for (String name:collectionNames) {
            predefinedcollectionstable[i][0]=name;
            predefinedcollectionstable[i][1]=new Integer(engine.getSizeForModuleCollection(name));
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
        predefinedCollectionsTable.getColumn("Modules").setMinWidth(65);
        predefinedCollectionsTable.getColumn("Modules").setMaxWidth(65);
        predefinedCollectionsTable.getColumn("Modules").setWidth(65);
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
        predefinedCollectionsTable.getColumn("Modules").setCellRenderer(new Prompt_ModuleCollection.CellRenderer_RightAlign());
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
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(ModuleCollection.class);
        //dataformats.remove(engine.getDataFormat("Plain"));
        importCollectionPanel=new JPanel(new BorderLayout());
        loadFromFilePanel=new LoadFromFilePanel(dataformats,gui,ModuleCollection.class);
        importCollectionPanel.add(loadFromFilePanel,BorderLayout.CENTER);
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
        JLabel label1=new JLabel("Module track");
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
            if (((RegionDataset)regiondata).isModuleTrack()) tracksmodel.addElement(regiondata.getName());
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
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(ModuleNumericMap.class));        
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
               String[] parsed=ModuleCollection.parseTrackConfigurationString(fromTrackString,engine);
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
    
    private void setupFromPropertyPanel() {
        String[] userdefined=ModuleCRM.getAllUserDefinedProperties(gui.getEngine());
        String[] standard=ModuleCRM.getAllStandardProperties(true);
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
                Class type=ModuleCRM.getPropertyClass(propertyName, engine);
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
               Object[] parsed=ModuleCollection.parsePropertyConfigurationString(fromPropertyString,engine);
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
        ArrayList<Data> mapslist=engine.getAllDataItemsOfType(ModuleNumericMap.class);
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
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(ModuleNumericMap.class));        
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
               String[] parsed=ModuleCollection.parseMapConfigurationString(fromMapString,engine);
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
    
    private void setupParseListPanel() {
        parseListPanel=new JPanel();
        parseListPanel.setLayout(new BoxLayout(parseListPanel, BoxLayout.Y_AXIS));
        JPanel internal1=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal2=new JPanel(new BorderLayout());
        JPanel internal3=new JPanel(new FlowLayout(FlowLayout.LEADING));
        ignoreParseErrors=new JCheckBox("Ignore parse errors", false);
        resolveInProtocol=new JCheckBox("Resolve in protocol", true);
        resolveInProtocol.setToolTipText("<html>If selected, the list will be recorded verbatim in the protocol and references will be resolved when the protocol is executed.<br>If not selected, references will be resolved right away and the resulting list will be recorded in the protocol.<br></html>");
        JLabel label=new JLabel("<html>Enter names of modules or collections below (separated by commas, semicolons or whitespace).<br>"
                + "Clusters within module partitions can be referred to by: <tt>   PartitionName->ClusterName</tt>.<br>"
                + "If module names contain numbered indices, you can specify a range of modules using <br>"
                + "a colon operator to separate the first and last module. For example, the entry \"<tt>ab3cd:ab7cd</tt>\" "
                + "will<br>refer to any module starting with \"ab\" followed by a number between 3 and 7 and ending with \"cd\".<br>"
                + "Prefix entries with <b>&minus;</b> (minus) to subtract from current selection, <b>&</b> to intersect or <b>!</b> to add complement.<br>"
                + "Wildcards (*) are allowed within module names (except when specifying ranges).</html>");
        
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
    
    private void setupFromInteractionsPanel() {
        fromInteractionsPanel=new JPanel();
        fromInteractionsPanel.setLayout(new BorderLayout());
        ParameterSettings parametersettings=null;
        Parameter[] parameters=ModuleCollection.getCreateFromInteractionsParameters();
        if (showExisting && data.isFromInteractions()) {           
            StandardParametersParser parser=(protocol!=null)?((StandardParametersParser)protocol.getParametersParser()):new StandardParametersParser(engine, datatypetable);                              
            try {
              parametersettings=parser.parse(data.getFromInteractionsConfiguration(),parameters);
            } catch (ParseError e) {}
        } else parametersettings=new ParameterSettings();
        fromInteractionsParametersPanel=new ParametersPanel(parameters, parametersettings, gui.getEngine());
        JScrollPane interactionsScrollpane=new JScrollPane(fromInteractionsParametersPanel);
        JPanel interactionsControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        fromInteractionsPanel.add(interactionsScrollpane,BorderLayout.CENTER);
        fromInteractionsPanel.add(interactionsControls,BorderLayout.SOUTH);
        JButton testGenerationButton=new JButton("How many modules will be created?");
        testGenerationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testCreateFromInterations();
            }
        });
        interactionsControls.add(testGenerationButton);
        JLabel description=new JLabel(
                "<html>Create modules based on the annotated interactions of motifs.<br>"
                + "All motifs will first be clustered into 'meta-motifs' together with their known alternatives.<br>"
                + "A meta-motif is said to interact with another such meta-motif if any of the motif models<br>"
                + "in the cluster is known to interact with any of the motifs in the other meta-motif cluster.<br>"
                + "Such interacting meta-motifs will form the basis of modules generated according to<br>the specified settings below.<br>"
                + "</html>");
        JPanel header=new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        header.add(description);
        fromInteractionsPanel.add(header,BorderLayout.NORTH);
    }    

    public void testCreateFromInterations() {
        fromInteractionsParametersPanel.setParameters();
        ParameterSettings parameters=fromInteractionsParametersPanel.getParameterSettings();
        parameters.setParameter("Testing", Boolean.TRUE);
        try {
           String message="Sloppy Programming Error...";
           ModuleCollection newcollection=ModuleCollection.createModuleCollectionFromInteractions("temp", parameters, engine, null);
           if (newcollection.hasPayload()) {
               Data payload1=newcollection.getPayload().get(0);
               if (payload1 instanceof NumericVariable) {
                   int modulecount=((NumericVariable)payload1).getValue().intValue();
                   message="The current settings will generate "+modulecount+" module"+((modulecount==1)?"":"s")+".\n(Unless an upper limit is imposed)";
               }
           }
           JOptionPane.showMessageDialog(this, message,"Module Collection",JOptionPane.INFORMATION_MESSAGE);           
        } catch (Exception e) {
            e.printStackTrace(System.err);
            JOptionPane.showMessageDialog(this, "An error occurred while generating modules:\n\n"+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        } 
    }

    @Override
    public boolean onOKPressed() {
        if (tabbedPanel==null || tabbedPanel.getSelectedComponent()==manualSelectionPanel) {
            manualSelectionPanel.updateModuleCollection();
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
                data=(ModuleCollection)loadFromFilePanel.loadData(data,ModuleCollection.getType());
                isimported=true;
                data.setPredefinedCollectionName(null);
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String errorType=(e instanceof IOException)?"I/O-error":"error";
                JOptionPane.showMessageDialog(this, "An "+errorType+" occurred while importing collection from file:\n\n"+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==fromInteractionsPanel) {
            fromInteractionsParametersPanel.setParameters();
            ParameterSettings parameters=fromInteractionsParametersPanel.getParameterSettings();                        
            if (showExisting) { // create a complete new map so that it can be compared with (and replace) the current data object               
                createDataObjectInBackground(new Object[]{"interactions",data.getName(),parameters}); // this returns right away but spawns a lengthy background process which will eventually close the prompt             
                return false; // keeps the dialog open until the background task is finished         
            } else { // the prompt is shown in response to an "Add New ModuleCRM Collection" request. Just set the basic parameters needed for a command line. A "new" operation task will be run later to create the finished data object and this avoids creating the object twice
                data=new ModuleCollection(data.getName());
                Parameter[] parameterDefinitions=ModuleCollection.getCreateFromInteractionsParameters();
                StandardParametersParser parser=new StandardParametersParser();
                String commandString=parser.getCommandString(parameterDefinitions, parameters);
                data.setFromInteractionsConfiguration(commandString);
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
                   
                   String fromTrackString=null;
                   // this is based on the syntax recognized by ModuleCollection
                   if (selectionoperation.equals("in") || selectionoperation.equals("not in")) fromTrackString=trackName+",support "+selectionoperation+" ["+quorum+","+quorum2+"]"+((percentage)?"%":""); //
                   else fromTrackString = trackName + ",support" + selectionoperation + quorum + ((percentage) ? "%" : ""); //                  
                            
                   if (showExisting) { // create a complete new map so that it can be compared with (and replace) the current data object
                        createDataObjectInBackground(new Object[]{"track",data.getName(),fromTrackString}); // this returns right away but spawns a lengthy background process which will eventually close the prompt             
                        return false; // keeps the dialog open until the background task is finished         
                   } else { // the prompt is shown in response to an "Add New Motif Collection" request. Just set the basic parameters needed for a command line. A "new" operation task will be run later to create the finished data object and this avoids creating the object twice
                        data=new ModuleCollection(data.getName());
                        data.setFromTrackString(fromTrackString);
                   }                     
                }                    
            }      
        } else if (tabbedPanel.getSelectedComponent()==fromMapPanel) {            
            String mapName=(String)fromMapSelection.getSelectedItem();
            if (mapName!=null && !mapName.isEmpty()) {
                Data numericmap=engine.getDataItem(mapName);
                if (numericmap instanceof ModuleNumericMap) {
                   String operator=(String)fromMapOperator.getSelectedItem();
                   String firstOperandString=(String)fromMapFirstOperandCombobox.getSelectedItem();
                   if (firstOperandString!=null) firstOperandString=firstOperandString.trim();
                   String secondOperandString=(String)fromMapSecondOperandCombobox.getSelectedItem();
                   if (secondOperandString!=null) secondOperandString=secondOperandString.trim();
                   try {
                     data.createCollectionFromMap((ModuleNumericMap)numericmap, operator, firstOperandString, secondOperandString, engine);
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
            ModuleCollection parsedCollection=null;
            try {
                parsedCollection=ModuleCollection.parseModuleCollectionParameters(text, data.getName(), notfound, engine, null);
            } catch (Exception e) {} // exceptions will not be thrown when parameter (notfound != null)

            if (reportErrors) {
                if (notfound.size()>15) {
                    String errormsg=notfound.size()+" modules (or collections) were not found:\n\n";
                    JOptionPane.showMessageDialog(this, errormsg, "Unknown modules/collections", JOptionPane.ERROR_MESSAGE);
                    return false;
                } else if (notfound.size()>0) {
                    String errormsg=(notfound.size()==1)?"The following module/collection was not found:\n\n":"The following "+notfound.size()+" modules/collections were not found:\n\n";
                    for (int i=0;i<notfound.size()-1;i++) {
                         errormsg+=notfound.get(i)+"\n";
                    }
                    errormsg+=notfound.get(notfound.size()-1);
                    JOptionPane.showMessageDialog(this, errormsg, "Unknown modules/collections", JOptionPane.ERROR_MESSAGE);
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
       if (newdata instanceof ModuleCollection) {
           data=(ModuleCollection)newdata;         
       }
    }    
    
    @Override
    public Data createDataObject(final Object[] parameters, OperationTask task) throws Exception {
        if (parameters==null || parameters.length==0) return new ModuleCollection("dummy");
        if (((String)parameters[0]).equals("interactions")) {
            String targetName=(String)parameters[1];
            ParameterSettings parameterSettings=(ParameterSettings)parameters[2];
            ModuleCollection newcollection=ModuleCollection.createModuleCollectionFromInteractions(targetName, parameterSettings, engine, task);
            return newcollection;
        } else if (((String)parameters[0]).equals("track")) {
            String targetName=(String)parameters[1];
            String parameter=(String)parameters[2];
            return ModuleCollection.parseModuleCollectionParameters(Operation_new.FROM_TRACK_PREFIX+parameter, targetName, null, engine, task);        
        }
        else return new ModuleCollection("dummy");
    }     

    public boolean isImported() {
        return isimported;
    }

    private void reportPredefinedCollectionlError(String msg) {
        if (msg==null) msg="NULL";
        predefinedPanelErrorLabel.setText(msg);
    }
    

    
    private ModuleCollection importPredefinedCollection(ModuleCollection data) throws ExecutionError {
        String collectionName=data.getPredefinedCollectionName();
        if (collectionName==null) throw new ExecutionError("SYSTEM ERROR: No predefined collection defined");
        String filename=engine.getFilenameForModuleCollection(collectionName);
        String formatname="MotifLabModule";
        if (filename==null) throw new ExecutionError("Unknown Module Collection: "+collectionName);
        BufferedReader inputStream=null;
        ArrayList<String> input=new ArrayList<String>();
        try {
            inputStream=new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(new File(filename))))); // these files will always be installed locally
            String line;
            while((line=inputStream.readLine())!=null) {input.add(line);}
        } catch (IOException e) { 
            throw new ExecutionError("An error occurred when loading predefined Module Collection:\n["+e.getClass().getSimpleName()+"] "+e.getMessage(),0);
        } finally {
            try {
                if (inputStream!=null) inputStream.close();
            } catch (IOException ioe) {System.err.println("SYSTEM ERROR: An error occurred when closing BufferedReader in Prompt_ModuleCollection.importPredefinedCollection(): "+ioe.getMessage());}
        }           
        DataFormat format = engine.getDataFormat(formatname);
        if (format==null) throw new ExecutionError("Unknown Dataformat: "+formatname);    
        try {data=(ModuleCollection)format.parseInput(input, data,null,null);}
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
