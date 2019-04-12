/*
 
 
 */

package motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.*;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.DataTypeTable;
import motiflab.engine.protocol.StandardOperationParser_statistic;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.GenericSequenceBrowserPanel;
import motiflab.gui.LoadFromFilePanel;

import motiflab.gui.MotifLabGUI;
import motiflab.gui.SimpleDataPanelIcon;
import motiflab.gui.operationdialog.OperationDialog_statistic;


/**
 *
 * @author kjetikl
 */
public class Prompt_SequenceNumericMap extends Prompt {
    
    private SequenceNumericMap data;
    
    private JPanel mainpanel;
    private JPanel manualEntryPanel;
    private JPanel fromPropertyPanel;    
    private JPanel fromStatisticPanel;        
    private LoadFromFilePanel importPanel;
    private JPanel distributionPanel;
    private DistributionPanel distributionGraphPanel;
    private JPanel parseListPanel;
    private JTextArea parseListTextArea;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;
    private JTabbedPane tabbedPanel;
    private JTable table;
    private JTextField defaultTextfield;
    private JLabel messageLabel;
    private JComboBox fromPropertyCombobox;    
    private JTextArea fromStatisticTextArea;
    
    private boolean isModal=false;
    private boolean showExisting=false;

    public Prompt_SequenceNumericMap(MotifLabGUI gui, String prompt, SequenceNumericMap dataitem) {
        this(gui,prompt,dataitem,true);
    }
      
    public Prompt_SequenceNumericMap(MotifLabGUI gui, String prompt, SequenceNumericMap dataitem, boolean modal) {
        super(gui,prompt, modal);
        this.isModal=modal;
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
            showExisting=true;
        }
        else data=new SequenceNumericMap(gui.getGenericDataitemName(SequenceNumericMap.class, null), 0);
        setDataItemName(data.getName());
        setTitle("Sequence Numeric Map");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.SEQUENCE_NUMERIC_MAP_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(Color.WHITE);
        setDataItemIcon(icon, true);
        setupManualEntryPanel();
        setupParseListPanel();
        setupFromPropertyPanel();
        setupFromStatisticPanel();
        setupImportPanel();
        setupDistributionPanel();
        mainpanel=new JPanel();
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Manual Entry", manualEntryPanel);
        tabbedPanel.addTab("From List", parseListPanel);
        tabbedPanel.addTab("From Property", fromPropertyPanel);
        tabbedPanel.addTab("From Statistic", fromStatisticPanel);
        tabbedPanel.addTab("Import", importPanel);
        tabbedPanel.addTab("Distribution", distributionPanel);
        mainpanel=new JPanel(new BorderLayout());
        Dimension size=new Dimension(520,420);
        //mainpanel.setMinimumSize(size);
        mainpanel.setPreferredSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromPropertyPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromStatisticPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));        
        importPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        distributionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        mainpanel.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(mainpanel);
        pack();
        if (dataitem!=null) focusOKButton();
    }

    private void setupManualEntryPanel() {
        SequenceCollection dataset=engine.getDefaultSequenceCollection();
        int size=dataset.getNumberofSequences();
        Object[][] rowData=new Object[dataset.size()][2];
        for (int i=0;i<size;i++) {
            Sequence sequence=dataset.getSequenceByIndex(i, engine);
            rowData[i][0]=sequence;
            if (data.contains(sequence.getName())) rowData[i][1]=data.getValue(sequence.getName());
            else rowData[i][1]=null;
        }
        DefaultTableModel model = new DefaultTableModel(rowData, new String[]{"Sequence","Value"}) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column==0) return Sequence.class; else return Double.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return (column==1 && isDataEditable());
            }
        };
        manualEntryPanel=new JPanel(new BorderLayout());
        JPanel internalPanel=new JPanel(new BorderLayout());
        GenericSequenceBrowserPanel browserpanel=new GenericSequenceBrowserPanel(gui, model, false, isModal); // no controls here
        internalPanel.add(browserpanel);
        table=browserpanel.getTable();        
        table.getColumnModel().getColumn(1).setCellRenderer(new CellRenderer_value(data.getValue()));        
        DataMapTableSorter tablesorter=new DataMapTableSorter(table.getModel());
        tablesorter.setComparator(0, ((TableRowSorter)table.getRowSorter()).getComparator(0));        
        tablesorter.setComparator(1, new MapValueComparator(data));
        table.setRowSorter(tablesorter);
        table.getTableHeader().setReorderingAllowed(false);
        ExcelAdapter adapter=new ExcelAdapter(table, false, ExcelAdapter.CONVERT_TO_DOUBLE);
        JPanel controlsPanel=new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
        JPanel controlsPanelLeft=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel controlsPanelRight=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlsPanel.add(controlsPanelLeft);
        controlsPanel.add(controlsPanelRight);
        JLabel sequenceNameLabel=new JLabel("Default  ");
        sequenceNameLabel.setForeground(java.awt.Color.RED);
        defaultTextfield=new JTextField(16);
        defaultTextfield.setHorizontalAlignment(JTextField.RIGHT);
        defaultTextfield.setText(data.getValue().toString());
        controlsPanelLeft.add(sequenceNameLabel);
        controlsPanelLeft.add(defaultTextfield);
        controlsPanelRight.add(browserpanel.getSearchField());
        browserpanel.add(controlsPanel,BorderLayout.SOUTH);
        messageLabel=new JLabel("  ");
        messageLabel.setFont(errorMessageFont);
        messageLabel.setForeground(java.awt.Color.RED);
        JPanel messagePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        messagePanel.add(messageLabel);
        manualEntryPanel.add(internalPanel,BorderLayout.CENTER);
        manualEntryPanel.add(messagePanel,BorderLayout.SOUTH);
    }

  

    private void setupParseListPanel() {
        parseListPanel=new JPanel();
        parseListPanel.setLayout(new BoxLayout(parseListPanel, BoxLayout.Y_AXIS));
        JPanel internal1=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal2=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal3=new JPanel(new BorderLayout());
        ignoreParseErrors=new JCheckBox("Ignore parse errors", false);
        JLabel label=new JLabel("<html>Enter assignment pairs below separated by newline or comma.<br>Each pair should be in the format: Sequence = value<br>The Sequence field can refer to a single sequence,<br>a sequence collection or a cluster within a sequence partition.<br>(Like so: <tt>PartitionName->ClusterName = value</tt>)<br>Wildcards (*) are allowed within sequence names.</html>");
        resolveInProtocol=new JCheckBox("Resolve in protocol", true);
        resolveInProtocol.setToolTipText("<html>If selected, the listed entries will be recorded verbatim in the protocol and references will be resolved when the protocol is executed.<br>If not selected, references will be resolved right away and the resulting entries will be recorded in the protocol.<br></html>");        
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
        if (showExisting && data.isFromList()) {
            String list=data.getFromListString();
            list=list.replace(",", "\n");
            parseListTextArea.setText(list);
            resolveInProtocol.setSelected(true);
        }        
    }

    private void setupFromPropertyPanel() {
        fromPropertyPanel=new JPanel();
        JPanel internal=new JPanel();
        internal.setLayout(new FlowLayout(FlowLayout.CENTER));
        internal.setBorder(BorderFactory.createEmptyBorder(50, 10, 20, 10));
        fromPropertyPanel.setLayout(new BorderLayout());
        String[] numproperties=Sequence.getNumericProperties(engine);
        String[] allProperties=new String[numproperties.length+1];
        System.arraycopy(numproperties, 0, allProperties, 0, numproperties.length);
        allProperties[numproperties.length]="sort order";
        fromPropertyCombobox=new JComboBox(allProperties);
        if (data.getFromPropertyName()!=null) { // initialize fields according to current map configuration
            fromPropertyCombobox.setSelectedItem(data.getFromPropertyName());
        }
        internal.add(new JLabel("Create map from property  "));
        internal.add(fromPropertyCombobox);
        fromPropertyPanel.add(internal);      
    }    
    
   private void setupFromStatisticPanel() {
        fromStatisticPanel=new JPanel();
        fromStatisticPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        JPanel upperControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal=new JPanel();
        internal.setLayout(new GridBagLayout());
        internal.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        JLabel label1=new JLabel("Statistic");
        JButton fromStatisticSelectButton = new JButton("Select");
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
        ArrayList<String> operandValues=new ArrayList<String>();
        operandValues.addAll(engine.getNamesForAllDataItemsOfType(NumericVariable.class));
        operandValues.addAll(engine.getNamesForAllDataItemsOfType(SequenceNumericMap.class));        
        upperControls.add(new JScrollPane(fromStatisticTextArea));
        upperControls.add(fromStatisticSelectButton);
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
        fromStatisticPanel.add(internal);
        if (data.isFromStatistic()) { // initialize fields according to current map configuration
            String fromStatisticString=data.getFromStatisticString();
            fromStatisticTextArea.setText(fromStatisticString);
        }        
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
    
     private void setupImportPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(SequenceNumericMap.class);
        importPanel=new LoadFromFilePanel(dataformats,gui,SequenceNumericMap.class);
    }

    private void setupDistributionPanel() {
        distributionPanel=new JPanel();
        distributionPanel.setLayout(new BorderLayout());
        distributionPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        distributionGraphPanel=new DistributionPanel(data, 100, 3, engine);
        distributionPanel.add(distributionGraphPanel,BorderLayout.CENTER);
        JPanel distributionPanelControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        ArrayList<String> collections=engine.getNamesForAllDataItemsOfType(SequenceCollection.class);
        for (Data partition:engine.getAllDataItemsOfType(SequencePartition.class)) {
            for (String cluster:((SequencePartition)partition).getClusterNames()) {
                collections.add(partition.getName()+"."+cluster);
            }
        }
        Collections.sort(collections);        
        collections.add(0, "");
        final JComboBox selectCollection=new JComboBox(collections.toArray());
        selectCollection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double[] values=null;
                String colName=(String)selectCollection.getSelectedItem();
                if (!colName.isEmpty()) {
                    Data selcol=null;
                    if (colName.contains(".")) {
                        String[] split=colName.split("\\.");
                        Data part=engine.getDataItem(split[0]);
                        if (part instanceof SequencePartition) selcol=((SequencePartition)part).getClusterAsSequenceCollection(split[1], engine);
                        
                    } else selcol=engine.getDataItem(colName);
                    if (selcol instanceof SequenceCollection) {
                        values=new double[((SequenceCollection)selcol).size()];
                        for (int i=0;i<values.length;i++) values[i]=data.getValue(((SequenceCollection)selcol).getSequenceNameByIndex(i));
                    }   
                }
                distributionGraphPanel.highlight(values);
                distributionGraphPanel.updateHistogram(null);
                distributionGraphPanel.repaint();
            }
        });
        distributionPanelControls.add(new JLabel("     Highlight "));
        distributionPanelControls.add(selectCollection);
        distributionPanel.add(distributionPanelControls,BorderLayout.SOUTH);
    }

    @Override
    public boolean onOKPressed() {   
        if (tabbedPanel.getSelectedComponent()==importPanel) { 
            try {
                String filename=importPanel.getFilename();
                if (filename==null) throw new ExecutionError("Missing filename");
                data=(SequenceNumericMap)importPanel.loadData(data,SequenceNumericMap.getType());
                DataFormat format=importPanel.getDataFormat();
                ParameterSettings settings=importPanel.getParameterSettings();
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing data from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel || tabbedPanel.getSelectedComponent()==distributionPanel) {
            for (int i=0;i<table.getRowCount();i++) {
                Sequence sequence=(Sequence)table.getValueAt(i, 0);
                if (sequence==null) continue; // this could happen if the user has deleted entries in the name column            
                String sequenceName=sequence.getName();
                if (!engine.dataExists(sequenceName, Sequence.class)) {
                   reportErrorInTable("Unknown sequence:  "+sequenceName,i);
                   return false;
                }
                Object valueObject=table.getValueAt(i, 1);
                String valueText=(valueObject!=null)?valueObject.toString().trim():"";
                if (valueText.isEmpty()) data.removeValue(sequenceName);
                else try {
                    double value=Double.parseDouble(valueText);
                    data.setValue(sequenceName,value);
                } catch (NumberFormatException e) {
                   reportErrorInTable("Value for '"+sequenceName+"' is not numeric:  "+valueText,i);
                   return false;
                }
           }
           try {
              double defaultvalue=Double.parseDouble(defaultTextfield.getText());
              data.setDefaultValue(defaultvalue);
          } catch (NumberFormatException e) {
              reportErrorInTable("Default value is not numeric",-1);
              return false;
          }
        } else if (tabbedPanel.getSelectedComponent()==fromPropertyPanel) {
            String propertyName=(String)fromPropertyCombobox.getSelectedItem();
            try {
                data=SequenceNumericMap.createSequenceNumericMapFromParameterString(data.getName(), Operation_new.FROM_PROPERTY_PREFIX+propertyName, null, engine, null);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while creating map based on sequence property '"+propertyName+"':\n"+exceptionText+e.getMessage(),"Data Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==fromStatisticPanel) {
            String statisticString=fromStatisticTextArea.getText();
            if (statisticString!=null && !statisticString.isEmpty()) {
                if (showExisting) { // create a complete new map so that it can be compared with (and replace) the current data object
                    createDataObjectInBackground(new Object[]{"statistic",data.getName(),statisticString}); // this returns right away but spawns a lengthy background process which will eventually close the prompt             
                    return false; // keeps the dialog open until the background task is finished         
                } else { // the prompt is shown in response to an "Add New Sequence Numeric Map" request. Just set the basic parameters needed for a command line. A "new" operation task will be run later to create the finished data object and this avoids creating the map twice
                    data=new SequenceNumericMap(data.getName(),data.getValue());
                    data.setFromStatisticString(statisticString);
                }               
               
            }
        } else if (tabbedPanel.getSelectedComponent()==parseListPanel) {
            boolean reportErrors=!ignoreParseErrors.isSelected();
            String text=parseListTextArea.getText();
            text=text.replaceAll("\\n", ",");
            text=text.replaceAll(",+", ",");
            text=text.replaceAll("\\t", "="); // allow 2 TAB-separated columns instead of =
            text=text.replaceAll("^,", ""); // remove leading comma
            text=text.replaceAll(",$", ""); // remove trailing comma               
            ArrayList<String> notfound=new ArrayList<String>();
            SequenceNumericMap parsedMap=null;
            try {
                parsedMap=SequenceNumericMap.createSequenceNumericMapFromParameterString(data.getName(), text, notfound, engine, null);
            } catch (Exception e) {} // exceptions will not be thrown when parameter (notfound != null)

            if (reportErrors) {
                if (notfound.size()>15) {
                    String errormsg=notfound.size()+" problems encountered:\n\n";
                    JOptionPane.showMessageDialog(this, errormsg, "Parsing error", JOptionPane.ERROR_MESSAGE);
                    return false;
                } else if (notfound.size()>0) {
                    String errormsg="Problems encountered with the following entries:\n\n";
                    for (int i=0;i<notfound.size()-1;i++) {
                         errormsg+=notfound.get(i)+"\n";
                    }
                    errormsg+=notfound.get(notfound.size()-1);
                    JOptionPane.showMessageDialog(this, errormsg, "Parsing error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else { // silent mode (only report errors in log) and return map anyway
                for (String error:notfound) engine.logMessage(error);
            }
            if (parsedMap!=null) data.importData(parsedMap);
            if (resolveInProtocol.isSelected()) data.setFromListString(text);            
        }
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);        
        return true;
    }
    
    private void reportErrorInTable(String errorMsg, int tablerow) {
        messageLabel.setText(errorMsg);
        if (tablerow>=0) {
            table.setRowSelectionInterval(tablerow, tablerow);
            table.scrollRectToVisible(table.getCellRect(tablerow,0,true));
        }        
    }
    
    @Override
    public Data createDataObject(final Object[] parameters, OperationTask task) throws Exception {
        if (parameters==null || parameters.length==0) return new SequenceNumericMap("dummy",0);
        if (((String)parameters[0]).equals("statistic")) {
            String targetName=(String)parameters[1];
            String parameter=(String)parameters[2];
            return SequenceNumericMap.createSequenceNumericMapFromParameterString(targetName, Operation_new.FROM_STATISTIC_PREFIX+parameter, null, engine, task);
        } else return new SequenceNumericMap("dummy",0);
    }    
    
    @Override
    public Data getData() {
       return data; 
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof SequenceNumericMap) data=(SequenceNumericMap)newdata; 
    }     
    
private class CellRenderer_value extends DefaultTableCellRenderer {
     private double defaultvalue=0;
     public CellRenderer_value(double defaultvalue) {
       super();
       this.defaultvalue=defaultvalue;
       this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
    } 
     
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component=super.getTableCellRendererComponent(table, (value!=null)?value:defaultvalue, isSelected, hasFocus, row, column);
        component.setForeground((value!=null)?java.awt.Color.BLACK:java.awt.Color.LIGHT_GRAY); // use gray color for default value
        return component;
    }    
} // end class CellRenderer_value


}
