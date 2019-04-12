/*
 
 
 */

package motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.*;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_new;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.GenericSequenceBrowserPanel;
import motiflab.gui.LoadFromFilePanel;

import motiflab.gui.MotifLabGUI;
import motiflab.gui.SimpleDataPanelIcon;


/**
 *
 * @author kjetikl
 */
public class Prompt_SequenceTextMap extends Prompt {
    
    private SequenceTextMap data;
    
    private JPanel mainpanel;
    private JPanel manualEntryPanel;
    private JPanel fromPropertyPanel;      
    private LoadFromFilePanel importPanel;
    private JPanel parseListPanel;
    private JTextArea parseListTextArea;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;    
    private JTabbedPane tabbedPanel;
    private JTable table;
    private JTextField defaultTextfield;
    private JLabel messageLabel;
    private JComboBox fromPropertyCombobox;    
    
    private boolean isModal=false;
    private boolean showExisting=false;    

    public Prompt_SequenceTextMap(MotifLabGUI gui, String prompt, SequenceTextMap dataitem) {
        this(gui,prompt,dataitem,true);
    }
      
    public Prompt_SequenceTextMap(MotifLabGUI gui, String prompt, SequenceTextMap dataitem, boolean modal) {
        super(gui,prompt, modal);
        this.isModal=modal;
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
            showExisting=true;            
        }
        else data=new SequenceTextMap(gui.getGenericDataitemName(SequenceTextMap.class, null), "");
        setDataItemName(data.getName());
        setTitle(data.getDynamicType());
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.SEQUENCE_TEXT_MAP_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(Color.WHITE);
        setDataItemIcon(icon, true);
        setupManualEntryPanel();
        setupParseListPanel();
        setupFromPropertyPanel();
        setupImportPanel();
        mainpanel=new JPanel();
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Manual Entry", manualEntryPanel);
        tabbedPanel.addTab("From List", parseListPanel);
        tabbedPanel.addTab("From Property", fromPropertyPanel);
        tabbedPanel.addTab("Import", importPanel);
        mainpanel=new JPanel(new BorderLayout());
        Dimension size=new Dimension(500,420);
        //mainpanel.setMinimumSize(size);
        mainpanel.setPreferredSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromPropertyPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));        
        importPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
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
                if (column==0) return Sequence.class; else return String.class;
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
        ExcelAdapter adapter=new ExcelAdapter(table, false, ExcelAdapter.CONVERT_NONE);
        JPanel controlsPanel=new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
        JPanel controlsPanelLeft=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel controlsPanelRight=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlsPanel.add(controlsPanelLeft);
        controlsPanel.add(controlsPanelRight);
        JLabel sequenceNameLabel=new JLabel("Default  ");
        sequenceNameLabel.setForeground(java.awt.Color.RED);
        defaultTextfield=new JTextField(16);
        defaultTextfield.setHorizontalAlignment(JTextField.LEFT);
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
        JLabel label=new JLabel("<html>Enter assignment pairs below separated by newlines.<br>Each pair should be in the format: <tt>Sequence = value</tt><br>The Sequence field can refer to a single sequence, a sequence collection,<br>a <i>range</i> of sequences or a cluster within a sequence partition<br>(like so: <tt>PartitionName->ClusterName = value</tt>)<br>Wildcards (*) are allowed within sequence names.</html>");
        
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
        String[] allProperties=Sequence.getAllProperties(engine);
        fromPropertyCombobox=new JComboBox(allProperties);
        if (data.getFromPropertyName()!=null) { // initialize fields according to current map configuration
            fromPropertyCombobox.setSelectedItem(data.getFromPropertyName());
        }
        internal.add(new JLabel("Create map from property  "));
        internal.add(fromPropertyCombobox);
        fromPropertyPanel.add(internal);      
    }    
    

    
     private void setupImportPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(SequenceTextMap.class);
        importPanel=new LoadFromFilePanel(dataformats,gui,SequenceTextMap.class);
    }


    @Override
    public boolean onOKPressed() {   
        if (tabbedPanel.getSelectedComponent()==importPanel) { 
            try {
                String filename=importPanel.getFilename();
                if (filename==null) throw new ExecutionError("Missing filename");
                data=(SequenceTextMap)importPanel.loadData(data,SequenceTextMap.getType());
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
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel) {
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
                else data.setValue(sequenceName,valueText);              
           }
           String defaultValue=defaultTextfield.getText();
           if (defaultValue==null) defaultValue="";           
           data.setDefaultValue(defaultValue.trim());
        } else if (tabbedPanel.getSelectedComponent()==fromPropertyPanel) {
            String propertyName=(String)fromPropertyCombobox.getSelectedItem();
            try {
                data=SequenceTextMap.createSequenceTextMapFromParameterString(data.getName(), Operation_new.FROM_PROPERTY_PREFIX+propertyName, null, engine);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while creating map based on sequence property '"+propertyName+"':\n"+exceptionText+e.getMessage(),"Data Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==parseListPanel) {
            boolean reportErrors=!ignoreParseErrors.isSelected();
            String text=parseListTextArea.getText();
            text=escapeListText(text);             
            ArrayList<String> notfound=new ArrayList<String>();
            SequenceTextMap parsedMap=null;
            try {
                parsedMap=SequenceTextMap.createSequenceTextMapFromParameterString(data.getName(), text, notfound, engine);
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
    
    /* Takes the text in the "from List" panel containing one list entry per line and performs necessary escaping before it can be used as a constructor string */
    private String escapeListText(String text) {
        StringBuilder buffer = new StringBuilder(text.length()+100);
        String[] lines=text.split("\n");
        int i=0;
        for (String line:lines) {
            if (line.trim().isEmpty()) continue;
            if (i>0) buffer.append(",");
            String[] parts=line.split("=",2);
            String key=parts[0].trim();
            String value=parts[1].trim();
            if (value.startsWith("\"") && value.endsWith("\"")) value=value.substring(1,value.length()-1); // strip enclosing quotes
            if (value.contains("\"")) value=value.replace("\"", "\\\""); // escape internal double quotes in the string
            if (value.contains(",") || value.contains("\"")) value="\""+value+"\""; // escape whole string with double quotes if it contains commas or quotes
            buffer.append(key);
            buffer.append("=");
            buffer.append(value);
            i++;            
        }
        return buffer.toString();
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
       if (newdata instanceof SequenceTextMap) data=(SequenceTextMap)newdata; 
    }     
    
private class CellRenderer_value extends DefaultTableCellRenderer {
     private String defaultvalue="";
     public CellRenderer_value(String defaultvalue) {
       super();
       this.defaultvalue=defaultvalue;
       this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
    } 
     
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component=super.getTableCellRendererComponent(table, (value!=null)?value:defaultvalue, isSelected, hasFocus, row, column);
        component.setForeground((value!=null)?java.awt.Color.BLACK:java.awt.Color.LIGHT_GRAY); // use gray color for default value
        return component;
    }    
} // end class CellRenderer_value


}
