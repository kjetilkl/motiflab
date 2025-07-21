/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.swing.border.BevelBorder;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.DataTypeTable;
import org.motiflab.engine.protocol.StandardOperationParser_statistic;
import org.motiflab.gui.LoadFromFilePanel;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.SequenceBrowserPanel;
import org.motiflab.gui.SimpleDataPanelIcon;
import org.motiflab.gui.operationdialog.OperationDialog_statistic;

/**
 *
 * @author kjetikl
 */
public class Prompt_SequenceCollection extends Prompt {

    // private JPanel manualEntryPanel;
    private SequenceBrowserPanel manualSelectionPanel;
    private JPanel parseListPanel;
    private JPanel fromPropertyPanel;    
    private JPanel fromMapPanel;
    private JPanel fromStatisticPanel;
    private LoadFromFilePanel importFromFilePanel;
    private JTabbedPane tabbedPanel;

    private JTextArea parseListTextArea;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;
    private JComboBox fromMapSelection;
    private JComboBox fromMapOperator;
    private JComboBox fromMapFirstOperandCombobox;
    private JComboBox fromMapSecondOperandCombobox;
    private JLabel fromMapToLabel;
    
    private JComboBox fromPropertyCombobox;
    private JComboBox fromPropertyOperatorCombobox;   
    private JTextArea fromPropertyTextArea;    
    
    private JTextArea fromStatisticTextArea;
    private JComboBox fromStatisticOperator;
    private JComboBox fromStatisticFirstOperandCombobox;
    private JComboBox fromStatisticSecondOperandCombobox;
    private JButton fromStatisticSelectButton;
    private JLabel fromStatisticToLabel;
    private JLabel includedInCollectionLabel=null;
    private JTable table=null; // manual selection table (obtained from sequenceBrowser)

    boolean isDefaultCollection=false;
    
    private boolean showExisting;
    private boolean isModal=false;
    
    private SequenceCollection collection;    
    
    public Prompt_SequenceCollection(MotifLabGUI gui, String prompt, SequenceCollection dataitem) {
        this(gui,prompt,dataitem,true);
    }
    
    public Prompt_SequenceCollection(MotifLabGUI gui, String prompt, SequenceCollection seqCollection, boolean modal) {
        super(gui,prompt, modal);
        this.isModal=modal;
        showExisting=false;
        if (seqCollection!=null) {
            collection=seqCollection;
            setExistingDataItem(seqCollection);
            showExisting=true;
        }
        else collection=new SequenceCollection(gui.getGenericDataitemName(SequenceCollection.class, null));
        isDefaultCollection=collection.getName().equals(engine.getDefaultSequenceCollectionName());

        setDataItemName(collection.getName());
        setTitle("Sequence Collection");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.SEQUENCE_COLLECTION_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(Color.WHITE);
        setDataItemIcon(icon, true);
        manualSelectionPanel=new SequenceBrowserPanel(gui, collection, modal, false);
        manualSelectionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));     
        if (isDefaultCollection) {
            setMainPanel(manualSelectionPanel);
            setDataEditable(false);
        } else {
            setupImportModelPanel();
            setupParseListPanel();
            setupFromPropertyPanel();            
            setupFromMapPanel();
            setupFromStatisticPanel();
            tabbedPanel=new JTabbedPane();
            tabbedPanel.addTab("Manual Entry", manualSelectionPanel);
            tabbedPanel.addTab("From List", parseListPanel);
            tabbedPanel.addTab("From Property", fromPropertyPanel);             
            tabbedPanel.addTab("From Map", fromMapPanel);
            tabbedPanel.addTab("From Statistic", fromStatisticPanel);
            tabbedPanel.addTab("Import", importFromFilePanel);
            JPanel internal=new JPanel(new BorderLayout());
            Dimension size=new Dimension(600,400);
            internal.setMinimumSize(size);
            //internal.setPreferredSize(size);
            parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            fromMapPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            fromStatisticPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            importFromFilePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            internal.add(tabbedPanel);
            setMainPanel(internal);
        }
        if (showExisting) {
           if (!isDefaultCollection) tabbedPanel.setSelectedComponent(manualSelectionPanel); // if the default collection is show there will be no tabs, only the manualSelectionPanel
           focusOKButton();
        }
        pack();        
    }

    @Override
    public boolean isDataEditable() {
        return (super.isDataEditable() && !isDefaultCollection);
    }

    @Override
    public void setDataEditable(boolean editable) {
        if (isDefaultCollection) super.setDataEditable(false);
        else super.setDataEditable(editable);
    }
    

    private void setupImportModelPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(SequenceCollection.class);
        importFromFilePanel=new LoadFromFilePanel(dataformats,gui,SequenceCollection.class);
    }

    private void setupParseListPanel() {
        parseListPanel=new JPanel();
        parseListPanel.setLayout(new BoxLayout(parseListPanel, BoxLayout.Y_AXIS));
        JPanel internal1=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal2=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal3=new JPanel(new BorderLayout());
        ignoreParseErrors=new JCheckBox("Ignore parse errors", false);
        resolveInProtocol=new JCheckBox("Resolve in protocol", true);
        resolveInProtocol.setToolTipText("<html>If selected, the list will be recorded verbatim in the protocol and references will be resolved when the protocol is executed.<br>If not selected, references will be resolved right away and the resulting list will be recorded in the protocol.<br></html>");
        JLabel label=new JLabel("<html>Enter names of sequences or collections below (separated by commas, semicolons or whitespace).<br>"
                + "Clusters within sequence partitions can be referred to by: <tt>   PartitionName->ClusterName</tt>.<br>"
                + "If sequence names contain numbered indices, you can specify a range of sequences using <br>"
                + "a colon operator to separate the first and last sequence. For example, the entry \"<tt>ab3cd:ab7cd</tt>\" "
                + "will<br>refer to any sequence starting with \"ab\" followed by a number between 3 and 7 and ending with \"cd\".<br>"
                + "Prefix entries with <b>&minus;</b> (minus) to subtract from current selection, <b>&</b> to intersect or <b>!</b> to add complement.<br>"
                + "Wildcards (*) are allowed within sequence names (except when specifying ranges).</html>");
        parseListTextArea=new JTextArea();
        internal1.add(label);
        internal2.add(ignoreParseErrors);
        internal2.add(new JLabel("       "));
        internal2.add(resolveInProtocol);
        internal3.add(new JScrollPane(parseListTextArea));
        internal1.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
        internal2.setBorder(BorderFactory.createEmptyBorder(4, 16, 5, 20));
        internal3.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        parseListPanel.add(internal1);
        parseListPanel.add(internal3);
        parseListPanel.add(internal2);
        if (showExisting && collection.isFromList()) {
            String list=collection.getFromListString();
            list=list.replace(",", "\n");
            parseListTextArea.setText(list);
            resolveInProtocol.setSelected(true);
        }
    }

    private void setupFromPropertyPanel() {
        String[] userdefined=Sequence.getAllUserDefinedProperties(gui.getEngine());
        String[] standard=Sequence.getAllStandardProperties(gui.getEngine());
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
                Class type=Sequence.getPropertyClass(propertyName, engine);
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
        if (showExisting && collection.isFromProperty()) { // initialize fields according to current collection configuration
            String fromPropertyString=collection.getFromPropertyString();
            try {
               Object[] parsed=SequenceCollection.parsePropertyConfigurationString(fromPropertyString,engine);
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
        ArrayList<Data> mapslist=engine.getAllDataItemsOfType(SequenceNumericMap.class);
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
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(SequenceNumericMap.class));        
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
        if (showExisting && collection.isFromMap()) { // initialize fields according to current collection configuration
            String fromMapString=collection.getFromMapString();
            try {
               String[] parsed=SequenceCollection.parseMapConfigurationString(fromMapString,engine);
               String mapvariable=(String)parsed[0];
               String operator=(String)parsed[1];
               String firstOp=(String)parsed[2];
               String secondOp=(String)parsed[3];
               fromMapFirstOperandCombobox.setSelectedItem(firstOp);
               if (secondOp!=null) fromMapSecondOperandCombobox.setSelectedItem(secondOp);
               fromMapSelection.setSelectedItem(mapvariable);
               fromMapOperator.setSelectedItem(operator);
            } catch (ParseError p) {
                fromMapOperator.setSelectedItem(">");
            }
        }
        else fromMapOperator.setSelectedItem(">");
    }

    private void setupFromStatisticPanel() {
        fromStatisticPanel=new JPanel();
        fromStatisticPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        JPanel upperControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel lowerControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal=new JPanel();
        internal.setLayout(new GridBagLayout());
        internal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel label1=new JLabel("Statistic");
        JLabel label2=new JLabel("Value");
        fromStatisticSelectButton = new JButton("Select");
        fromStatisticSelectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               selectStatistic();
            }
        });
        DefaultComboBoxModel model=new DefaultComboBoxModel();
        ArrayList<Data> mapslist=engine.getAllDataItemsOfType(SequenceNumericMap.class);
        for (Data map:mapslist) {
            model.addElement(map.getName());
        }
        fromStatisticTextArea=new JTextArea(6,30);
        
        fromStatisticTextArea.setEditable(false);
        fromStatisticOperator=new JComboBox(new String[]{"=",">",">=","<","<=","<>","in","not in"});
        fromStatisticOperator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String operator=(String)fromStatisticOperator.getSelectedItem();
                boolean showSecondOperator=(operator.equals("in") || operator.equals("not in"));
                fromStatisticToLabel.setVisible(showSecondOperator);
                fromStatisticSecondOperandCombobox.setVisible(showSecondOperator);
            }
        });
        ArrayList<String> operandValues=new ArrayList<String>();
        operandValues.addAll(engine.getNamesForAllDataItemsOfType(NumericVariable.class));
        operandValues.addAll(engine.getNamesForAllDataItemsOfType(SequenceNumericMap.class));        
        fromStatisticFirstOperandCombobox=new JComboBox(operandValues.toArray());
        fromStatisticFirstOperandCombobox.setEditable(true);
        fromStatisticFirstOperandCombobox.setSelectedItem("0");
        fromStatisticSecondOperandCombobox=new JComboBox(operandValues.toArray());
        fromStatisticSecondOperandCombobox.setEditable(true);
        fromStatisticSecondOperandCombobox.setSelectedItem("1");
        fromStatisticToLabel=new JLabel("     to ");
        upperControls.add(new JScrollPane(fromStatisticTextArea));
        upperControls.add(fromStatisticSelectButton);
        JPanel lowerControls_inner=new JPanel(new GridBagLayout());
        GridBagConstraints constr=new GridBagConstraints();
        constr.ipadx=2;constr.ipady=4;
        constr.gridy=0; constr.gridx=0;
        lowerControls_inner.add(fromStatisticOperator,constr);
        constr.gridy=0; constr.gridx=1;
        lowerControls_inner.add(fromStatisticFirstOperandCombobox,constr);
        constr.gridy=1; constr.gridx=0;
        lowerControls_inner.add(fromStatisticToLabel,constr);
        constr.gridy=1; constr.gridx=1;
        lowerControls_inner.add(fromStatisticSecondOperandCombobox,constr);
        lowerControls.add(lowerControls_inner);
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
        fromStatisticPanel.add(internal);
        if (showExisting && collection.isFromStatistic()) { // initialize fields according to current collection configuration
            String fromStatisticString=collection.getFromStatisticString();
            try {
               String[] parsed=SequenceCollection.parseStatisticConfigurationString(fromStatisticString,engine);
               String statisticString=(String)parsed[0];
               String operator=(String)parsed[1];
               String firstOp=(String)parsed[2];
               String secondOp=(String)parsed[3];
               fromStatisticFirstOperandCombobox.setSelectedItem(firstOp);
               if (secondOp!=null) fromStatisticSecondOperandCombobox.setSelectedItem(secondOp);
               fromStatisticTextArea.setText(statisticString);
               fromStatisticOperator.setSelectedItem(operator);
            } catch (ParseError p) {
                fromStatisticOperator.setSelectedItem(">");
            }
        }
        else fromStatisticOperator.setSelectedItem(">");
    }

    private void selectStatistic() {
        String text=fromStatisticTextArea.getText();
        OperationTask task=null;
        StandardOperationParser_statistic parser=new StandardOperationParser_statistic();        
        if (text!=null) {
           parser.setEngine(engine);
           task=null;
           try {
               task=parser.parseInternal(text);    
            } catch (Exception e) { task=new OperationTask("statistic");}
        } else task=new OperationTask("statistic");
        task.setParameter(OperationTask.ENGINE, engine);
        task.setParameter(OperationTask.OPERATION, engine.getOperation("statistic"));
        task.setParameter(OperationTask.OPERATION_NAME,"statistic");
        try {
           DataTypeTable datatypetable=new DataTypeTable(engine);
           datatypetable.populateFromEngine();
           OperationDialog_statistic dialog=new OperationDialog_statistic();
           dialog.initialize(task,datatypetable,gui);
           dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
           dialog.setTargetVisible(false);
           dialog.setVisible(true);
           boolean okpressed=dialog.okPressed();
           dialog.dispose();
           if (okpressed) {
               fromStatisticTextArea.setText(parser.getStatisticCommandString(task));
           }
        } catch (Exception e) {
             JOptionPane.showMessageDialog(gui.getFrame(), e.getClass().toString()+"\n"+e.getMessage(),"Error" ,JOptionPane.ERROR_MESSAGE);
        }             
    }


    @Override
    public boolean onOKPressed() {
        if (collection.getName().equals(engine.getDefaultSequenceCollectionName())) return true; // the default collection can not be changed
        if (tabbedPanel.getSelectedComponent()==importFromFilePanel) {
            try {
                String filename=importFromFilePanel.getFilename();
                if (filename==null) throw new SystemError("Missing filename");
                DataFormat format=importFromFilePanel.getDataFormat();
                ParameterSettings settings=importFromFilePanel.getParameterSettings();
                Data result=importFromFilePanel.loadData(collection,SequenceCollection.getType());
                if (result instanceof SequenceCollection) collection=(SequenceCollection)result;
                else if (result instanceof DNASequenceDataset) collection=engine.extractSequencesFromFeatureDataset((DNASequenceDataset)result,null);
                else throw new ParseError("Wrong data type returned: "+result.getClass());
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                String errorType=(e instanceof IOException)?"I/O-error":"error";
                JOptionPane.showMessageDialog(this, "An "+errorType+"  error occurred while importing Sequence Collection from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==manualSelectionPanel) {
            manualSelectionPanel.updateSequenceCollection();
        } else if (tabbedPanel.getSelectedComponent()==fromMapPanel) {
            String mapName=(String)fromMapSelection.getSelectedItem();
            if (mapName!=null && !mapName.isEmpty()) {
                Data numericmap=engine.getDataItem(mapName);
                if (numericmap instanceof SequenceNumericMap) {
                   String operator=(String)fromMapOperator.getSelectedItem();
                   String firstOperandString=(String)fromMapFirstOperandCombobox.getSelectedItem();
                   String secondOperandString=(String)fromMapSecondOperandCombobox.getSelectedItem();
                   try {
                        collection.createCollectionFromMap((SequenceNumericMap)numericmap, operator, firstOperandString, secondOperandString, engine);
                   } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                   }
               }
            }
        } else if (tabbedPanel.getSelectedComponent()==fromStatisticPanel) {
            String statisticString=fromStatisticTextArea.getText();
            if (statisticString!=null && !statisticString.isEmpty()) {
               String operator=(String)fromStatisticOperator.getSelectedItem();
               String firstOperandString=(String)fromStatisticFirstOperandCombobox.getSelectedItem();
               String secondOperandString=(String)fromStatisticSecondOperandCombobox.getSelectedItem();
               String fromStatistic=null;
               // this is based on the syntax recognized by SequenceCollection
               if (operator.equals("in") || operator.equals("not in")) {
                  fromStatistic="("+statisticString+")"+" "+operator+" ["+firstOperandString+","+secondOperandString+"]";
               } else {
                  fromStatistic="("+statisticString+")"+operator+""+firstOperandString; 
               }               
               if (showExisting) { // create a complete new map so that it can be compared with (and replace) the current data object
                    createDataObjectInBackground(new Object[]{"statistic",collection.getName(),fromStatistic}); // this returns right away but spawns a lengthy background process which will eventually close the prompt             
                    return false; // keeps the dialog open until the background task is finished         
               } else { // the prompt is shown in response to an "Add New Sequence Collection" request. Just set the basic parameters needed for a command line. A "new" operation task will be run later to create the finished data object and this avoids creating the map twice
                    collection=new SequenceCollection(collection.getName());
                    collection.setFromStatisticString(fromStatistic);
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
               collection.createCollectionFromProperty(propertyName, operator, operands, engine);
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
            SequenceCollection parsedCollection=null;
            try {
                parsedCollection=SequenceCollection.parseSequenceCollectionParameters(text, collection.getName(), notfound, engine, null);
            } catch (Exception e) {} // exceptions will not be thrown when parameter (notfound != null)

            if (reportErrors) {
                if (notfound.size()>15) {
                    String errormsg=notfound.size()+" sequences (or collections) were not found:\n\n";
                    JOptionPane.showMessageDialog(this, errormsg, "Unknown sequences/collections", JOptionPane.ERROR_MESSAGE);
                    return false;
                } else if (notfound.size()>0) {
                    String errormsg=(notfound.size()==1)?"The following sequence/collection was not found:\n\n":"The following "+notfound.size()+" sequences/collections were not found:\n\n";
                    for (int i=0;i<notfound.size()-1;i++) {
                         errormsg+=notfound.get(i)+"\n";
                    }
                    errormsg+=notfound.get(notfound.size()-1);
                    JOptionPane.showMessageDialog(this, errormsg, "Unknown sequences/collections", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else { // silent mode (only report errors in log) and return collection anyway
                for (String error:notfound) engine.logMessage(error);
            }
            if (parsedCollection!=null) collection.importData(parsedCollection);
            else collection.clearAll(engine);
            if (resolveInProtocol.isSelected()) collection.setFromListString(text);
        } 
        String newName=getDataItemName();
        if (!collection.getName().equals(newName)) collection.rename(newName);        
        return true;
    }
    
    @Override
    public Data createDataObject(final Object[] parameters, OperationTask task) throws Exception {
        if (parameters==null || parameters.length==0) return new SequenceCollection("dummy");
        if (((String)parameters[0]).equals("statistic")) {
            String targetName=(String)parameters[1];
            String parameter=(String)parameters[2];
            return SequenceCollection.parseSequenceCollectionParameters(Operation_new.FROM_STATISTIC_PREFIX+parameter, targetName, null, engine, task);
        } else return new SequenceCollection("dummy");
    }     
    
    
    @Override
    public Data getData() {
       return collection; 
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof SequenceCollection) collection=(SequenceCollection)newdata; 
    }     
     
}