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
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.*;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.LoadFromFilePanel;
import motiflab.gui.MotifLabGUI;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_new;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.SimpleDataPanelIcon;
import motiflab.gui.VisualizationSettings;


/**
 *
 * @author kjetikl
 */
public class Prompt_MotifTextMap extends Prompt {
    
    private MotifTextMap data;
    private JPanel mainpanel;
    private JPanel manualEntryPanel;
    private JPanel parseListPanel;
    private JPanel fromPropertyPanel;
    private JTextArea parseListTextArea;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;    
    private JComboBox fromPropertyCombobox;
    private LoadFromFilePanel importPanel;
    private JTable table;
    private JTextField defaultTextfield;
    private JLabel messageLabel;
    private JTabbedPane tabbedPanel;
    private VisualizationSettings settings;
    private boolean isModal=false;
    private boolean showExisting=false;
      
    
    public Prompt_MotifTextMap(MotifLabGUI gui, String prompt, MotifTextMap dataitem) {
        this(gui,prompt,dataitem,true);
    }
    
    public Prompt_MotifTextMap(MotifLabGUI gui, String prompt, MotifTextMap dataitem, boolean modal) {
        super(gui,prompt, modal);
        this.isModal=modal;
        settings=gui.getVisualizationSettings();
        if (dataitem!=null) {
            data=dataitem;
            setExistingDataItem(dataitem);
            showExisting=true;
        }
        else data=new MotifTextMap(gui.getGenericDataitemName(MotifTextMap.class, null),"");
        setDataItemName(data.getName());
        setTitle(data.getDynamicType());
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.MOTIF_TEXT_MAP_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(java.awt.Color.WHITE);
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
        Dimension size=new Dimension(550,420);
        //mainpanel.setMinimumSize(size);
        mainpanel.setPreferredSize(size);
        // internal.setMaximumSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        importPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromPropertyPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        mainpanel.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(mainpanel);
        pack();
        if (dataitem!=null) focusOKButton();
    }
    

    private void setupManualEntryPanel() {
        ArrayList<Data> dataset=engine.getAllDataItemsOfType(Motif.class);
        int size=dataset.size();
        Object[][] rowData=new Object[dataset.size()][4];
        for (int i=0;i<size;i++) {
            Motif motif=(Motif)dataset.get(i);
            rowData[i][1]=motif.getName();
            rowData[i][3]=motif;
            if (data.contains(motif.getName())) rowData[i][2]=data.getValue(motif.getName());
            else rowData[i][2]=null; // =data.getValue(); // else rowData[i][2]=null;          
        }
        DefaultTableModel model=new DefaultTableModel(rowData, new String[]{" ","Motif","Value","Logo"}) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: return Color.class;                    
                    case 1: return String.class;
                    case 2: return String.class;
                    case 3: return Motif.class;
                    default: return String.class;
                }
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return (column==2 && isDataEditable());
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (column==0) return settings.getFeatureColor((String)getValueAt(row,1));
                return super.getValueAt(row, column);
            }                    
        };

        manualEntryPanel=new JPanel(new BorderLayout());
        JPanel internalPanel=new JPanel(new BorderLayout());
        GenericMotifBrowserPanel browserpanel=new GenericMotifBrowserPanel(gui, model, false, isModal); // no controls here
        internalPanel.add(browserpanel);
        table=browserpanel.getTable();
        table.getColumnModel().getColumn(1).setCellRenderer(new CellRenderer_Name());
        table.getColumnModel().getColumn(2).setCellRenderer(new CellRenderer_value(data.getValue()));
        DataMapTableSorter tablesorter=new DataMapTableSorter(table.getModel());
        tablesorter.setComparator(2, new MapValueComparator(data));
        table.setRowSorter(tablesorter);
        table.getRowSorter().toggleSortOrder(1);
        
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
        resolveInProtocol.setToolTipText("<html>If selected, the listed entries will be recorded verbatim in the protocol and references will be resolved when the protocol is executed.<br>If not selected, references will be resolved right away and the resulting entries will be recorded in the protocol.<br></html>");                
        JLabel label=new JLabel("<html>Enter assignment pairs below separated by newlines.<br>Each pair should be in the format: <tt>Motif = value</tt><br>The Motif field can refer to a single motif, a motif collection,<br>a <i>range</i> of motifs or a cluster within a motif partition<br>(like so: <tt>PartitionName->ClusterName = value</tt>)<br>Wildcards (*) are allowed within motif names.</html>");
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
        fromPropertyCombobox=new JComboBox(Motif.getAllProperties(true,engine));
        if (data.getFromPropertyName()!=null) { // initialize fields according to current partition configuration
            fromPropertyCombobox.setSelectedItem(data.getFromPropertyName());
        }
        internal.add(new JLabel("Create map from property  "));
        internal.add(fromPropertyCombobox);
        fromPropertyPanel.add(internal);
    }

    private void setupImportPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(MotifTextMap.class);
        //dataformats.remove(engine.getDataFormat("Plain"));
        importPanel=new LoadFromFilePanel(dataformats,gui,MotifTextMap.class);
    }


    @Override
    public boolean onOKPressed() {    
        if (tabbedPanel.getSelectedComponent()==importPanel) {
            try {
                String filename=importPanel.getFilename();
                if (filename==null) throw new ExecutionError("Missing filename");
                data=(MotifTextMap)importPanel.loadData(data,MotifTextMap.getType());
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
                data=MotifTextMap.createMotifTextMapFromParameterString(data.getName(), Operation_new.FROM_PROPERTY_PREFIX+propertyName, null, engine);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while creating map based on motif property '"+propertyName+"':\n"+exceptionText+e.getMessage(),"Data Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==parseListPanel) {
            boolean reportErrors=!ignoreParseErrors.isSelected();
            String text=parseListTextArea.getText();
            text=escapeListText(text);           
            ArrayList<String> notfound=new ArrayList<String>();
            MotifTextMap parsedMap=null;
            try {
                parsedMap=MotifTextMap.createMotifTextMapFromParameterString(data.getName(), text, notfound, engine);
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
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel) {
          for (int i=0;i<table.getRowCount();i++) {
                Object tablevalue=table.getValueAt(i, 1);
                if (tablevalue==null) continue; // this could happen if the user has deleted entries in the name column
                String motifName=tablevalue.toString().trim();
                if (!engine.dataExists(motifName, Motif.class)) {
                   reportErrorInTable("Unknown motif: "+motifName,i);
                   return false;
                }
                Object valueObject=table.getValueAt(i, 2);
                String valueText=(valueObject!=null)?valueObject.toString().trim():"";
                if (valueText.isEmpty()) data.removeValue(motifName);
                else data.setValue(motifName,valueText);

           }
           String defaultvalue=defaultTextfield.getText();
           if (defaultvalue==null) defaultvalue="";
           defaultvalue=defaultvalue.trim();
           data.setDefaultValue(defaultvalue);

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
       if (newdata instanceof MotifTextMap) data=(MotifTextMap)newdata; 
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

private class CellRenderer_Name extends DefaultTableCellRenderer {
    public CellRenderer_Name() {
           super();
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Data motif=(value instanceof String)?engine.getDataItem((String)value):null;
        String shownString;
        if (motif instanceof Motif) {
            shownString=((Motif)motif).getPresentationName();
        } else shownString=(value!=null)?value.toString():null;
        Component component=super.getTableCellRendererComponent(table, shownString, isSelected, hasFocus, row, column);
        String name="";
        if (value!=null) name=value.toString();
        if (data.contains(name)) component.setForeground((isSelected)?java.awt.Color.WHITE:java.awt.Color.BLACK);
        else component.setForeground(java.awt.Color.GRAY);
        return component;
    }
}// end class CellRenderer_Name


    
}
