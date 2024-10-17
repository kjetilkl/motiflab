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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.*;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.LoadFromFilePanel;
import motiflab.gui.MotifLabGUI;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.task.OperationTask;
import motiflab.gui.GenericModuleBrowserPanel;
import motiflab.gui.SimpleDataPanelIcon;
import motiflab.gui.VisualizationSettings;


/**
 *
 * @author kjetikl
 */
public class Prompt_ModuleNumericMap extends Prompt {

    private ModuleNumericMap data;
    private JPanel mainpanel;
    private JPanel manualEntryPanel;
    private JPanel parseListPanel;
    private JTextArea parseListTextArea;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;    
    private JPanel fromTrackPanel;
    private JPanel fromPropertyPanel;
    private JPanel distributionPanel;
    private DistributionPanel distributionGraphPanel;
    private LoadFromFilePanel importPanel;
    private JTable table;
    private JTextField defaultTextfield;
    private JLabel messageLabel;
    private JComboBox fromPropertyCombobox;    
    private JComboBox fromTrackDatatsetCombobox;
    private JComboBox fromTrackCollectionCombobox;
    private JComboBox fromTrackWithinRegionsCombobox;
    private JComboBox fromTrackPropertyCombobox;
    private JTabbedPane tabbedPanel;
    private VisualizationSettings settings;
    private boolean isModal=false;
    private boolean showExisting=false;


    public Prompt_ModuleNumericMap(MotifLabGUI gui, String prompt, ModuleNumericMap dataitem) {
        this(gui,prompt,dataitem,true);
    }

    public Prompt_ModuleNumericMap(MotifLabGUI gui, String prompt, ModuleNumericMap dataitem, boolean modal) {
        super(gui,prompt, modal);
        this.isModal=modal;
        settings=gui.getVisualizationSettings();
        if (dataitem!=null) {
            data=dataitem;
            setExistingDataItem(dataitem);
            showExisting=true;
        }
        else data=new ModuleNumericMap(gui.getGenericDataitemName(ModuleNumericMap.class, null), 0);
        setDataItemName(data.getName());
        setTitle("Module Numeric Map");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.MODULE_NUMERIC_MAP_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(java.awt.Color.WHITE);
        setDataItemIcon(icon, true);
        setupManualEntryPanel();
        setupParseListPanel();
        setupFromPropertyPanel();
        setupFromTrackPanel();
        setupImportPanel();
        setupDistributionPanel();
        mainpanel=new JPanel();
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Manual Entry", manualEntryPanel);
        tabbedPanel.addTab("From List", parseListPanel);
        tabbedPanel.addTab("From Property", fromPropertyPanel);        
        tabbedPanel.addTab("From Track", fromTrackPanel);
        tabbedPanel.addTab("Import", importPanel);
        tabbedPanel.addTab("Distribution", distributionPanel);
        mainpanel=new JPanel(new BorderLayout());
        Dimension size=new Dimension(530,420);
        //mainpanel.setMinimumSize(size);
        mainpanel.setPreferredSize(size);
        // internal.setMaximumSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        importPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromTrackPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromPropertyPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        distributionPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        mainpanel.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(mainpanel);
        pack();
        if (dataitem!=null) focusOKButton();
    }


    private void setupManualEntryPanel() {
        ArrayList<Data> dataset=engine.getAllDataItemsOfType(ModuleCRM.class);
        int size=dataset.size();
        Object[][] rowData=new Object[dataset.size()][4];
        for (int i=0;i<size;i++) {
            ModuleCRM cisRegModule=(ModuleCRM)dataset.get(i);
            rowData[i][1]=cisRegModule.getName();
            rowData[i][3]=cisRegModule;
            if (data.contains(cisRegModule.getName())) rowData[i][2]=data.getValue(cisRegModule.getName());
            else rowData[i][2]=null;
        }
        DefaultTableModel model=new DefaultTableModel(rowData, new String[]{" ","Module","Value","Logo"}) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: return Color.class;
                    case 1: return String.class;
                    case 2: return Double.class;
                    case 3: return ModuleCRM.class;
                    default: return String.class;
                }
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column==2;
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (column==0) return settings.getFeatureColor((String)getValueAt(row,1));
                return super.getValueAt(row, column);
            }
        };

        manualEntryPanel=new JPanel(new BorderLayout());
        JPanel internalPanel=new JPanel(new BorderLayout());
        GenericModuleBrowserPanel browserpanel=new GenericModuleBrowserPanel(gui, model, false, isModal); // no controls here
        internalPanel.add(browserpanel);
        table=browserpanel.getTable();
        table.getColumnModel().getColumn(1).setCellRenderer(new CellRenderer_Name());
        table.getColumnModel().getColumn(2).setCellRenderer(new CellRenderer_value(data.getValue()));
        DataMapTableSorter tablesorter=new DataMapTableSorter(table.getModel());
        tablesorter.setComparator(2, new MapValueComparator(data));
        table.setRowSorter(tablesorter);
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
        resolveInProtocol=new JCheckBox("Resolve in protocol", true);
        resolveInProtocol.setToolTipText("<html>If selected, the listed entries will be recorded verbatim in the protocol and references will be resolved when the protocol is executed.<br>If not selected, references will be resolved right away and the resulting entries will be recorded in the protocol.<br></html>");                
        JLabel label=new JLabel("<html>Enter assignment pairs below separated by newline or comma.<br>Each pair should be in the format: Module = value<br>The Module field can refer to a single module or a module collection.<br>Wildcards (*) are allowed within module names.</html>");
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
        fromPropertyCombobox=new JComboBox(ModuleCRM.getNumericProperties(engine));
        if (data.getFromPropertyName()!=null) { // initialize fields according to current partition configuration
            fromPropertyCombobox.setSelectedItem(data.getFromPropertyName());
        }
        internal.add(new JLabel("Create map from property  "));
        internal.add(fromPropertyCombobox);
        fromPropertyPanel.add(internal);
    }    

     private void setupFromTrackPanel() {
        fromTrackPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal = new JPanel(new GridBagLayout());
        internal.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.insets=new Insets(4,4,4,4);
        constraints.anchor=GridBagConstraints.BASELINE_LEADING;
        constraints.gridy=0;constraints.gridx=0;
        internal.add(new JLabel("Module Track  "),constraints);
        constraints.gridy=1;constraints.gridx=0;
        internal.add(new JLabel("Property  "),constraints);        
        constraints.gridy=2;constraints.gridx=0;
        internal.add(new JLabel("Sequence Collection  "),constraints);
        constraints.gridy=3;constraints.gridx=0;
        internal.add(new JLabel("Within regions"),constraints);

        DefaultComboBoxModel tracksmodel=new DefaultComboBoxModel();
        ArrayList<Data> moduleTracks=engine.getAllDataItemsOfType(RegionDataset.class);
        for (Data moduletrack:moduleTracks) {
            if (((RegionDataset)moduletrack).isModuleTrack()) tracksmodel.addElement(moduletrack.getName());
        }
        fromTrackDatatsetCombobox=new JComboBox(tracksmodel);
        String trackName=null;
        String propertyName=null;
        String collectionName=null;
        String withinName=null;
        String fromTrackString=data.getFromTrackString();
        if (fromTrackString!=null) {
            String[] parameters=fromTrackString.split("\\s*,\\s*");
            trackName=parameters[0].trim();   
            if (parameters.length>1) {
                for (int j=0;j<parameters.length;j++) {
                    String[] split1=parameters[j].trim().split("\\s*(:|=)\\s*");
                    if (split1[0].equalsIgnoreCase("Sequence Collection")) collectionName=split1[1].trim();
                    else if (split1[0].equalsIgnoreCase("within")) withinName=split1[1].trim();
                    else if (split1[0].equalsIgnoreCase("property")) propertyName=split1[1].trim();
                }
            }            
        }  
        if (trackName!=null) fromTrackDatatsetCombobox.setSelectedItem(trackName);
        constraints.gridy=0;constraints.gridx=1;
        internal.add(fromTrackDatatsetCombobox,constraints);

        DefaultComboBoxModel collectionmodel=new DefaultComboBoxModel();
        ArrayList<Data> collections=engine.getAllDataItemsOfType(SequenceCollection.class);
        for (Data col:collections) {
            collectionmodel.addElement(col.getName());
        }
        fromTrackCollectionCombobox=new JComboBox(collectionmodel);
        if (collectionName!=null) fromTrackCollectionCombobox.setSelectedItem(collectionName);
        else fromTrackCollectionCombobox.setSelectedItem(engine.getDefaultSequenceCollectionName());
        constraints.gridy=2;constraints.gridx=1;
        internal.add(fromTrackCollectionCombobox,constraints);        
        
        fromTrackPropertyCombobox=new JComboBox(ModuleNumericMap.getFromTrackPropertyNames());
        if (propertyName!=null) fromTrackPropertyCombobox.setSelectedItem(propertyName);
        else fromTrackPropertyCombobox.setSelectedIndex(0);
        constraints.gridy=1;constraints.gridx=1;
        internal.add(fromTrackPropertyCombobox,constraints);

        DefaultComboBoxModel withinregionsmodel=new DefaultComboBoxModel();
        withinregionsmodel.addElement("");
        ArrayList<Data> regionTracks=engine.getAllDataItemsOfType(RegionDataset.class);
        for (Data regiontrack:regionTracks) {
            withinregionsmodel.addElement(regiontrack.getName());
        }
        fromTrackWithinRegionsCombobox=new JComboBox(withinregionsmodel);
        if (withinName!=null) fromTrackWithinRegionsCombobox.setSelectedItem(withinName);      
        else fromTrackWithinRegionsCombobox.setSelectedItem("");
        constraints.gridy=3;constraints.gridx=1;
        internal.add(fromTrackWithinRegionsCombobox,constraints);

        fromTrackPanel.add(internal);
    }


    private void setupImportPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(ModuleNumericMap.class);
        //dataformats.remove(engine.getDataFormat("Plain"));
        importPanel=new LoadFromFilePanel(dataformats,gui,ModuleNumericMap.class);
    }

    private void setupDistributionPanel() {
        distributionPanel=new JPanel();
        distributionPanel.setLayout(new BorderLayout());
        distributionPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        distributionGraphPanel=new DistributionPanel(data, 100, 3, engine);
        distributionPanel.add(distributionGraphPanel,BorderLayout.CENTER);
        JPanel distributionPanelControls=new JPanel(new FlowLayout(FlowLayout.LEADING));
        ArrayList<String> collections=engine.getNamesForAllDataItemsOfType(ModuleCollection.class);
        for (Data partition:engine.getAllDataItemsOfType(ModulePartition.class)) {
            for (String cluster:((ModulePartition)partition).getClusterNames()) {
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
                        if (part instanceof ModulePartition) selcol=((ModulePartition)part).getClusterAsModuleCollection(split[1], engine);

                    } else selcol=engine.getDataItem(colName);
                    if (selcol instanceof ModuleCollection) {
                        values=new double[((ModuleCollection)selcol).size()];
                        for (int i=0;i<values.length;i++) values[i]=data.getValue(((ModuleCollection)selcol).getModuleNameByIndex(i));
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
                data=(ModuleNumericMap)importPanel.loadData(data,ModuleNumericMap.getType());
                DataFormat format=importPanel.getDataFormat();
                ParameterSettings parametersettings=importPanel.getParameterSettings();
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, parametersettings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing data from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==fromPropertyPanel) {
            String propertyName=(String)fromPropertyCombobox.getSelectedItem();
            try {
                data=ModuleNumericMap.createModuleNumericMapFromParameterString(data.getName(), Operation_new.FROM_PROPERTY_PREFIX+propertyName, null, engine,null);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while creating map based on module property '"+propertyName+"':\n"+exceptionText+e.getMessage(),"Data Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==fromTrackPanel) {
            try {
                String trackName=(String)fromTrackDatatsetCombobox.getSelectedItem();
                if (trackName==null || trackName.isEmpty()) throw new ExecutionError("Missing track");
                String propertyName=(String)fromTrackPropertyCombobox.getSelectedItem();
                int property=0;
                if (propertyName.equalsIgnoreCase("Total count")) property=0;
                else if (propertyName.equalsIgnoreCase("Sequence support")) property=2;
                //else if(propertyName.equalsIgnoreCase("Frequency")) property = 1;
                else throw new ExecutionError("Unknown property: "+propertyName);
                String collectionName=(String)fromTrackCollectionCombobox.getSelectedItem();
                SequenceCollection collection=null;
                if (collectionName!=null && engine.dataExists(collectionName, SequenceCollection.class)) collection=(SequenceCollection)engine.getDataItem(collectionName);
                else collection=engine.getDefaultSequenceCollection();

                RegionDataset datatrack=null;
                if (engine.dataExists(trackName, RegionDataset.class)) datatrack=(RegionDataset)engine.getDataItem(trackName);
                else throw new ExecutionError(trackName+" is not a Region Dataset");

                RegionDataset withinRegions=null;
                String withinRegionsName=(String)fromTrackWithinRegionsCombobox.getSelectedItem();
                if (withinRegionsName!=null && !withinRegionsName.isEmpty() && withinRegionsName.equals(trackName)) throw new ExecutionError("It is not advisable to use same module track to specify 'within' limits");
                if (withinRegionsName!=null && !withinRegionsName.isEmpty()) {
                    if (engine.dataExists(withinRegionsName, RegionDataset.class)) withinRegions=(RegionDataset)engine.getDataItem(withinRegionsName);
                    else throw new ExecutionError(withinRegionsName+" is not a Region Dataset");
                }
                String fromTrackString=trackName+",property="+propertyName;
                if (collection!=null && !collection.getName().equals(engine.getDefaultSequenceCollectionName())) fromTrackString+=",Sequence Collection="+collection.getName();
                if (withinRegions!=null) fromTrackString+=",within="+withinRegionsName;
                
                if (showExisting) { // create a complete new map so that it can be compared with (and replace) the current data object
                    createDataObjectInBackground(new Object[]{"track",data.getName(),fromTrackString}); // this returns right away but spawns a lengthy background process which will eventually close the prompt             
                    return false; // keeps the dialog open until the background task is finished         
                } else { // the prompt is shown in response to an "Add New ModuleCRM Numeric Map" request. Just set the basic parameters needed for a command line. A "new" operation task will be run later to create the finished data object and this avoids creating the map twice
                    data=new ModuleNumericMap(data.getName(),0);
                    data.setFromTrackString(fromTrackString);
                }                  
            
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while generating model from track:\n"+exceptionText+e.getMessage(),"Model Error",JOptionPane.ERROR_MESSAGE);
                return false;
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
            ModuleNumericMap parsedMap=null;
            try {
                parsedMap=ModuleNumericMap.createModuleNumericMapFromParameterString(data.getName(), text, notfound, engine, null);
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
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel || tabbedPanel.getSelectedComponent()==distributionPanel) {
          for (int i=0;i<table.getRowCount();i++) {
                Object tablevalue=table.getValueAt(i, 1);
                if (tablevalue==null) continue; // this could happen if the user has deleted entries in the name column
                String moduleName=tablevalue.toString().trim();              
                if (!engine.dataExists(moduleName, ModuleCRM.class)) {
                   reportErrorInTable("Unknown module: "+moduleName, i);
                   return false;
                }
                Object valueObject=table.getValueAt(i, 2);
                String valueText=(valueObject!=null)?valueObject.toString().trim():"";
                if (valueText.isEmpty()) data.removeValue(moduleName);
                else try {
                    double value=Double.parseDouble(valueText);
                    data.setValue(moduleName,value);
                } catch (NumberFormatException e) {
                   reportErrorInTable("Value for '"+moduleName+"' is not numeric:  "+valueText,i);
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
    public Data getData() {
       return data;
    }

    @Override
    public void setData(Data newdata) {
       if (newdata instanceof ModuleNumericMap) data=(ModuleNumericMap)newdata; 
    }     
    
    @Override
    public Data createDataObject(final Object[] parameters, OperationTask task) throws Exception {
        if (parameters==null || parameters.length==0) return new ModuleNumericMap("dummy",0);
        if (((String)parameters[0]).equals("track")) {
            String targetName=(String)parameters[1];
            String parameter=(String)parameters[2];
            return ModuleNumericMap.createModuleNumericMapFromParameterString(targetName, Operation_new.FROM_TRACK_PREFIX+parameter, null, engine, task);
        } else return new ModuleNumericMap("dummy",0);
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

private class CellRenderer_Name extends DefaultTableCellRenderer {
    public CellRenderer_Name() {
           super();
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Data cisRegModule=(value instanceof String)?engine.getDataItem((String)value):null;
        String shownString;
        if (cisRegModule instanceof ModuleCRM) {
            shownString=((ModuleCRM)cisRegModule).getName();
        } else shownString=(value!=null)?value.toString():null;
        Component component=super.getTableCellRendererComponent(table, shownString, isSelected, hasFocus, row, column);
        String name="";
        if (value!=null) name=value.toString();
        if (data.contains(name)) component.setForeground((isSelected)?java.awt.Color.WHITE:java.awt.Color.BLACK);
        else component.setForeground(java.awt.Color.GRAY);
        return component;
    }
}// end class CellRenderer_RightAlign




}
