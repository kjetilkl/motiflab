/*
 * UCSC_API_RegionAttributesDialog.java
 *
 */

package org.motiflab.gui;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.motiflab.engine.MotifLabEngine;
import org.jdesktop.application.Action;
import org.motiflab.engine.protocol.ParseError;


/**
 *
 * @author  kjetikl
 */
public class UCSC_API_RegionAttributesDialog extends javax.swing.JDialog {

    private static final String apiEndPoint="https://api.genome.ucsc.edu";
    private boolean OKpressed=false;
    private boolean everythingOK=true;
    private boolean interruptFlag=false;

    private MotifLabGUI gui=null;
    private MotifLabEngine engine;
    
    private static final String TABLECOLUMN_ATTRIBUTE="Attribute";
    private static final String TABLECOLUMN_FIELD="Data source field";
    private static final String TABLECOLUMN_DESCRIPTION="Description";
    private static final String TABLECOLUMN_EXAMPLES="Examples";    
    private static final String EXAMPLES_DELIMITER=" , ";
    
    private JTable attributesTable;
    private DefaultTableModel attributesTableModel;
    
    private JScrollPane scrollPane;
    private JButton okButton;
    private JButton cancelButton;
    private JButton addExtraAttributeButton;    
    private JProgressBar progressBar;

    private String trackName;
    private String genomeBuild;
    
    private HashMap<String,String[]> attributes;
    private HashMap<String,String> examples;
    
    private HashMap<String,String> finalValues=null; 


/**
 * @param gui
 * @param genomeBuild
 * @param trackName 
 */
    public UCSC_API_RegionAttributesDialog(MotifLabGUI gui, String genomeBuild, String trackName) {
        super(gui.getFrame(), true);
        this.gui=gui;
        this.engine=gui.getEngine();
        this.trackName=trackName;
        this.genomeBuild=genomeBuild;
        setTitle("Configure region attributes");
        initComponents();
        setupAttributesTable();
        updateTable(genomeBuild,trackName);
    }
  
    private void initComponents() {
        JPanel mainPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        
        JPanel topPanel = new JPanel();          
        topPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));
        topPanel.add(new JLabel("<html>Please select the <b>data source field</b> corresponding to each <b>region attribute</b>. Start and end corrdinates are required.<br>Names of extra attributes can be edited, but not the names of the standard attributes.</html>"));
        
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setMinimumSize(new java.awt.Dimension(100, 90));
        buttonsPanel.setPreferredSize(new java.awt.Dimension(100, 42));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        addExtraAttributeButton=new JButton("Add Extra Attribute");
        addExtraAttributeButton.setEnabled(false);
        addExtraAttributeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addExtraAttribute();
            }
        });
        jPanel3.add(addExtraAttributeButton);
   
        buttonsPanel.add(jPanel3, java.awt.BorderLayout.WEST);

        JPanel jPanel5 = new JPanel();
        jPanel5.setLayout(new BorderLayout());   
        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 20, 8, 20));        
        progressBar = new JProgressBar(0,100);
        jPanel5.add(progressBar, java.awt.BorderLayout.CENTER);
        buttonsPanel.add(jPanel5, java.awt.BorderLayout.CENTER);
                
        JPanel jPanel4 = new JPanel();
        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getActionMap(UCSC_API_RegionAttributesDialog.class, this);
        okButton = new JButton();
        okButton.setMaximumSize(new java.awt.Dimension(75, 27));
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton");
        okButton.setAction(actionMap.get("OKAction")); // NOI18N
        okButton.setText("OK");
        jPanel4.add(okButton);

        cancelButton = new JButton();
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setAction(actionMap.get("cancelAction")); // NOI18N        
        cancelButton.setText("Cancel");
        jPanel4.add(cancelButton);

        buttonsPanel.add(jPanel4, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        mainPanel.setMinimumSize(new java.awt.Dimension(900, 350));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(900, 350));
        mainPanel.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);        
        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);
        getRootPane().setDefaultButton(okButton);       
        pack();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);    
    }
    
    private void setupAttributesTable() {
        
        attributesTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_ATTRIBUTE, TABLECOLUMN_FIELD, TABLECOLUMN_DESCRIPTION, TABLECOLUMN_EXAMPLES},0);
        attributesTable=new JTable(attributesTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return (col==1 || (col==0 && row>=5));
            }
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                if (column==1) {
                    return new DefaultCellEditor(getFieldEditor(row,column));
                } else return super.getCellEditor(row, column);
            }
        };
      
        TableCellRenderer fieldComboBoxRenderer = new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComboBox box = getFieldEditor(row,column); 
                box.setOpaque(false);             
                return box;
            }
        };
           
        DescriptionRenderer cellRenderer = new DescriptionRenderer();
        ExampleRenderer exampleRenderer = new ExampleRenderer();
        attributesTable.setFillsViewportHeight(true);
        attributesTable.setRowSelectionAllowed(false);
        attributesTable.getTableHeader().setReorderingAllowed(false);   
        attributesTable.getColumn(TABLECOLUMN_ATTRIBUTE).setMinWidth(160);
        attributesTable.getColumn(TABLECOLUMN_ATTRIBUTE).setMaxWidth(160);        
        attributesTable.getColumn(TABLECOLUMN_FIELD).setMinWidth(160);
        attributesTable.getColumn(TABLECOLUMN_FIELD).setMaxWidth(160);                
        attributesTable.getColumn(TABLECOLUMN_FIELD).setCellRenderer(fieldComboBoxRenderer);
        attributesTable.getColumn(TABLECOLUMN_ATTRIBUTE).setCellRenderer(cellRenderer);
        attributesTable.getColumn(TABLECOLUMN_EXAMPLES).setCellRenderer(exampleRenderer);
        attributesTable.setRowHeight(24);    
        attributesTable.setShowGrid(true);
        attributesTable.setGridColor(Color.lightGray);     
        scrollPane.setViewportView(attributesTable);

        // Disable OK-button until a valid selection is made
        okButton.setEnabled(false);        
        ActionListener escapeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                interruptFlag=true;
            }
        };
        rootPane.registerKeyboardAction(
            escapeListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );        
    }
    
    private JComboBox<String> getFieldEditor(int row, int column) {
        Object value = attributesTableModel.getValueAt(row, column);
        JComboBox<String> comboBox = new JComboBox<>();
        switch (row) {
            case 0 -> comboBox.setModel(getAttributesModel("number",true)); // start coordinate (required number)
            case 1 -> comboBox.setModel(getAttributesModel("number",true)); // end coordinate  (required number)
            case 2 -> comboBox.setModel(getAttributesModel("string",false)); // type name (optional string)
            case 3 -> comboBox.setModel(getAttributesModel("number",false)); // score (optional number)
            case 4 -> comboBox.setModel(getAttributesModel(null,false));    // strand (optional string or number)                
           default -> comboBox.setModel(getExtraAttributesModel(row));    // extra (only show remaining fields)
        }       
        comboBox.setSelectedItem(value);
        comboBox.addActionListener((ActionEvent e) -> {
            String selected=(String)comboBox.getSelectedItem();
            String description=getAttributeDescription(selected);
            attributesTableModel.setValueAt(description, row, 2);
            String examples=getExamplesForAttribute(selected);
            attributesTableModel.setValueAt(examples, row, 3);            
            if (row>4) {
                attributesTableModel.setValueAt(selected, row, 0); // set name of extra attribute to selected field by default. It can be edited later
            }
        });
        return comboBox;        
    }
                             
       
    @SuppressWarnings("unchecked")
    private void updateTable(String genomebuild, String trackName) {
        if (genomebuild==null) {
            gui.logMessage("ERROR: Genome build missing for data track");
            return;
        }  
        if (trackName==null) {
            gui.logMessage("ERROR: name missing for data track");
            return;
        }  
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        SwingWorker worker=new SwingWorker<HashMap, Void>() {
            Exception ex=null;
            @Override
            public HashMap<String,String[]> doInBackground() {
                try {
                   SwingUtilities.invokeLater(() -> {progressBar.setString("Fetching region examples");});                
                   examples = getExamplesFromUCSC(genomebuild,trackName);
                   SwingUtilities.invokeLater(() -> {progressBar.setString("Fetching data track schema");});
                   return getSchemaFromUCSC(genomebuild,trackName);
                } catch (InterruptedException i) {
                    //
                } catch (IOException | ParseError e ) {
                    ex=e;
                }
                return null;
            }
            @Override
            public void done() {
                progressBar.setIndeterminate(false);
                if (ex!=null) {
                    String message=ex.getMessage();
                    progressBar.setString("ERROR: "+message);
                    gui.logMessage("ERROR: "+message);
                    everythingOK=false;
                    okButton.setEnabled(false);
                    addExtraAttributeButton.setEnabled(false);
                } else {
                    try {
                       updateTableFromSchema(get());
                    } catch (Exception e) {}
                    progressBar.setVisible(false);
                    progressBar.setValue(100);
                    okButton.setEnabled(true);
                    addExtraAttributeButton.setEnabled(true);
                }
            }
        }; // end of SwingWorker class
        worker.execute();
    }
    
    private void updateTableFromSchema(HashMap<String,String[]> map) {
        if (map==null) {
            reportError("No attributes found for Datatrack!");
            return;
        }
        attributes=map;
        attributesTableModel.setRowCount(0); // clear current table
               
        String start_field="";
             if (attributes.containsKey("start")) start_field="start";
        else if (attributes.containsKey("chromStart")) start_field="chromStart";
        else if (attributes.containsKey("genoStart")) start_field="genoStart";  
        else if (attributes.containsKey("txStart")) start_field="txStart"; 
        else start_field=getAttributeMatchingSubstring("start","number");
        attributesTableModel.addRow(new Object[]{"Start coordinate",start_field,getAttributeDescription(start_field),getExamplesForAttribute(start_field)});             
  
        String end_field="";
             if (attributes.containsKey("end")) end_field="end";
        else if (attributes.containsKey("chromEnd")) end_field="chromEnd";
        else if (attributes.containsKey("genoEnd")) end_field="genoEnd";  
        else if (attributes.containsKey("txEnd")) end_field="txEnd"; 
        else end_field=getAttributeMatchingSubstring("end","number");
        attributesTableModel.addRow(new Object[]{"End coordinate",end_field,getAttributeDescription(end_field),getExamplesForAttribute(end_field)});         
        
        String type_field="";
             if (attributes.containsKey("name")) type_field="name";
        else if (attributes.containsKey("type")) type_field="type";
        else type_field=getAttributeMatchingSubstring("name","string");
        if (type_field.isBlank()) type_field=getAttributeMatchingSubstring("type","string");
        attributesTableModel.addRow(new Object[]{"Region type",type_field,getAttributeDescription(type_field),getExamplesForAttribute(type_field)});
        
        String score_field="";
             if (attributes.containsKey("score")) score_field="score";
        if (score_field.isBlank()) score_field=getAttributeMatchingSubstring("score","number");
        attributesTableModel.addRow(new Object[]{"Score",score_field,getAttributeDescription(score_field),getExamplesForAttribute(score_field)});    
        
        String strand_field="";
             if (attributes.containsKey("score")) strand_field="strand";
        if (strand_field.isBlank()) strand_field=getAttributeMatchingSubstring("strand",null);
        attributesTableModel.addRow(new Object[]{"Strand",strand_field,getAttributeDescription(strand_field),getExamplesForAttribute(strand_field)});        
                    
    }
  
    private void addExtraAttribute() {
        HashSet<String> remainingAttributes = new HashSet<>(attributes.keySet());               
        for (int i=0;i<attributesTable.getRowCount();i++) {
           String value=(String)attributesTable.getValueAt(i, 1);
           if (!value.isBlank()) remainingAttributes.remove(value);   
        }
        String extra_field="";
        for (String extra:remainingAttributes) {
           extra_field=extra;
        }
        attributesTableModel.addRow(new Object[]{extra_field,extra_field,getAttributeDescription(extra_field),getExamplesForAttribute(extra_field)});  
    }
    
    private String getAttributeMatchingSubstring(String substring, String filter) {
        for (String string:attributes.keySet()) {
            String[] info = attributes.get(string);
            if (filter!=null && !filter.equals(info[1])) continue;
            if (string.toLowerCase().contains(substring)) return string;
        }
        return "";
    }
    
    private String getAttributeDescription(String attribute) {
        if (attribute==null || attribute.isBlank() || !attributes.containsKey(attribute)) return "";
        String[] info = attributes.get(attribute);
        return info[0];              
    }
    

    private DefaultComboBoxModel<String> getAttributesModel(String filter, boolean required) {
        ArrayList<String> list=new ArrayList<>();
        for (String attribute:attributes.keySet()) {
            String[] info = attributes.get(attribute);
            if (filter==null || filter.equals(info[1])) list.add(attribute);
        }
        list.sort(null);
        if (!required) list.add(0, "");     
        String[] a=new String[list.size()];
        return new DefaultComboBoxModel<>(list.toArray(a));
    }  
    
    /** Return only options that are not already used */
    private DefaultComboBoxModel<String> getExtraAttributesModel(int row) {
        HashSet<String> remainingAttributes = new HashSet<>(attributes.keySet());               
        for (int i=0;i<attributesTable.getRowCount();i++) {
           if (i==row) continue; // allow current value to be selected again
           String value=(String)attributesTable.getValueAt(i, 1);
           remainingAttributes.remove(value);   
        }
        remainingAttributes.remove("");     
        ArrayList<String> list=new ArrayList<>(remainingAttributes);
        list.sort(null);
        list.add(0, "");     
        String[] a=new String[list.size()];
        return new DefaultComboBoxModel<>(list.toArray(a));
    }      
    
    private String getExamplesForAttribute(String attribute) {
        if (examples!=null && examples.containsKey(attribute)) return examples.get(attribute);
        else return "";   
    }
    
    private void checkValues() throws ParseError {
        // check that no attribute names or fields have been used twice
        HashSet<String> usedFieldNames = new HashSet<>();  
        HashSet<String> usedFieldValues = new HashSet<>();        
        for (int i=0;i<attributesTable.getRowCount();i++) {
           String field_name=(String)attributesTable.getValueAt(i, 0);
           field_name=field_name.toLowerCase(); // 
           String field_value=(String)attributesTable.getValueAt(i, 1);
           if (i<2 && (field_value==null || field_value.isBlank())) throw new ParseError("Missing data source field for required attribute: "+field_name);
           if (usedFieldNames.contains(field_name)) throw new ParseError("Each attribute name can only be used once! \""+field_name+"\" has been used twice");
           if (usedFieldValues.contains(field_value)) throw new ParseError("Each data source field can only be used once! \""+field_value+"\" has been used twice");
                if (i==0) usedFieldNames.add("start"); // these have different names in the table, so I add the actual field name
           else if (i==1) usedFieldNames.add("end"); // these have different names in the table, so I add the actual field name
           else if (i==2) usedFieldNames.add("type");// these have different names in the table, so I add the actual field name
           if (!field_name.isBlank()) usedFieldNames.add(field_name); 
           if (!field_value.isBlank()) usedFieldValues.add(field_value);            
        }          
    }
    
    private HashMap<String,String> getFinalValues() {
        HashMap<String, String> map=new HashMap<>();
        String extra_fields=null;
        String start_field=(String)attributesTable.getValueAt(0, 1);
        String end_field=(String)attributesTable.getValueAt(1, 1);
        String type_field=(String)attributesTable.getValueAt(2, 1);
        String score_field=(String)attributesTable.getValueAt(3, 1);
        String strand_field=(String)attributesTable.getValueAt(4, 1); 
        map.put("start", start_field);
        map.put("end", end_field);
        if (!type_field.isBlank()) map.put("type", type_field);
        if (!score_field.isBlank()) map.put("score", score_field);
        if (!strand_field.isBlank()) map.put("strand", strand_field);
       
        for (int i=5;i<attributesTable.getRowCount();i++) {
           String extra_name=(String)attributesTable.getValueAt(i, 0);
           String extra_value=(String)attributesTable.getValueAt(i, 1);
           if (extra_name.isBlank() || extra_value.isBlank()) continue; 
           if (extra_fields==null) extra_fields=extra_name+"="+extra_value;
           else extra_fields=extra_fields+","+extra_name+"="+extra_value;
        }        
        if (extra_fields!=null) map.put("extra",extra_fields);
        return map;
    }
    
    
    @Action
    public void OKAction() {       
        OKpressed=true;
        everythingOK=false;
        try {
            checkValues();
        } catch (ParseError p) {
            finalValues=null;
            reportError(p.getMessage());            
            return;
        }     
        finalValues=getFinalValues();
        everythingOK=true;        
        setVisible(false);
    }

    @Action
    public void cancelAction() {
        OKpressed=false;
        interruptFlag=true;
        setVisible(false);
    }

    public boolean isOKPressed() {
        return OKpressed && everythingOK;
    }

    private void reportError(String message) {
       JOptionPane.showMessageDialog(this, message, "Error" ,JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method is used to retrieve a new DataTrack object based on the selections made in the dialog
     * the returned DataTrack object contains all necessary information to register a new track
     * @return DataTrack
     */
    public HashMap<String,String> getSelectedAttributes() {
        if (everythingOK && finalValues!=null) return finalValues;
        else return null;
    }
    

    /**
     * Retrieve the schema for the given track from the UCSC Genome Browser API. 
     * @param genome
     * @param trackName
     * @throws InterruptedException 
     */
    private HashMap<String,String[]> getSchemaFromUCSC(String genome, String trackName) throws InterruptedException, IOException, ParseError { 
        HashMap<String,String[]> attributes=new HashMap<>();
        URL url = null; 
        try {
            url = new URL(apiEndPoint+"/list/schema?genome="+genome+";track="+trackName);  
            // gui.logMessage(url.toExternalForm());
        } catch (MalformedURLException ex) {}
        try (InputStream in = url.openStream()) {
            JsonFactory factory = new JsonFactory();           
            JsonParser parser = factory.createParser(in);
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) break;
               
                if (JsonToken.FIELD_NAME.equals(token) && "columnTypes".equals(parser.currentName())) {
                    token=parser.nextToken(); // move to START_ARRAY
                    if (!JsonToken.START_ARRAY.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected start of array but got "+token);
                    token=parser.nextToken(); // this should now be START_OBJECT OR END_ARRAY                 
                    
                    while (token != JsonToken.END_ARRAY) {    
                        String field_name=null;
                        String field_sqlType=null;
                        String field_jsonType=null;
                        String field_description=null;   
                        
                        if (!JsonToken.START_OBJECT.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected start of object (attribute) but got "+token);
                        
                        token=parser.nextToken(); // this should now be "name" FIELD
                        if (!JsonToken.FIELD_NAME.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected field name but got "+token);
                        if (!"name".equals(parser.getValueAsString())) throw new ParseError("Unexpected token in JSON file. Expected 'name' attribute but got "+parser.getValueAsString());
                        token=parser.nextToken(); // this should now be "name" VALUE
                        if (!JsonToken.VALUE_STRING.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected String-value for 'name' but got "+token);
                        field_name=parser.getValueAsString();
                        
                        token=parser.nextToken(); // this should now be "sqlType" FIELD
                        if (!JsonToken.FIELD_NAME.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected field name but got "+token);
                        if (!"sqlType".equals(parser.getValueAsString())) throw new ParseError("Unexpected token in JSON file. Expected 'sqlType' attribute but got "+parser.getValueAsString());
                        token=parser.nextToken(); // this should now be "sqlType" VALUE
                        if (!JsonToken.VALUE_STRING.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected String-value for 'sqlType' but got "+token);
                        field_sqlType=parser.getValueAsString();

                        token=parser.nextToken(); // this should now be "jsonType" FIELD
                        if (!JsonToken.FIELD_NAME.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected field name but got "+token);
                        if (!"jsonType".equals(parser.getValueAsString())) throw new ParseError("Unexpected token in JSON file. Expected 'jsonType' attribute but got "+parser.getValueAsString());
                        token=parser.nextToken(); // this should now be "jsonType" VALUE
                        if (!JsonToken.VALUE_STRING.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected String-value for 'jsonType' but got "+token);
                        field_jsonType=parser.getValueAsString();

                        token=parser.nextToken(); // this should now be "description" FIELD
                        if (!JsonToken.FIELD_NAME.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected field name but got "+token);
                        if (!"description".equals(parser.getValueAsString())) throw new ParseError("Unexpected token in JSON file. Expected 'description' attribute but got "+parser.getValueAsString());
                        token=parser.nextToken(); // this should now be "description" VALUE
                        if (!JsonToken.VALUE_STRING.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected String-value for 'description' but got "+token);
                        field_description=parser.getValueAsString();                        
                        
                        attributes.put(field_name,new String[]{field_description,field_jsonType,field_sqlType});
                        
                        token=parser.nextToken(); 
                        if (!JsonToken.END_OBJECT.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected end of object (attribute) but got "+token);
                        token=parser.nextToken(); // this should now be the start of a new object or the end of the array                      
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return attributes;
    }
            
    private HashMap<String,String> getExamplesFromUCSC(String genome, String trackName) throws InterruptedException, IOException, ParseError { 
        HashMap<String,String> examples=new HashMap<>();
        URL url = null; 
        try {
            url = new URL(apiEndPoint+"/getData/track?genome="+genome+";track="+trackName+";maxItemsOutput=5");  
            // gui.logMessage(url.toExternalForm());
        } catch (MalformedURLException ex) {}
        try (InputStream in = url.openStream()) {
            JsonFactory factory = new JsonFactory();           
            JsonParser parser = factory.createParser(in);
            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) break;
               
                if (JsonToken.FIELD_NAME.equals(token) && trackName.equals(parser.currentName())) {
                    token=parser.nextToken(); // move to START_OBJECT or START_ARRAY
                    if (JsonToken.START_OBJECT.equals(token)) { // object contains individual chromosomes
                        token=parser.nextToken(); // this should now be name of field (chromosome)
                        if (!JsonToken.FIELD_NAME.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected name of chromosome but got "+token+" = "+parser.getValueAsString());
                        token=parser.nextToken(); // this should now be start of array
                        if (!JsonToken.START_ARRAY.equals(token)) throw new ParseError("Unexpected token in JSON file. Expected start of array but got "+token+" = "+parser.getValueAsString());                        
                    } if (JsonToken.START_ARRAY.equals(token)) {
                        // no individual chromosomes. next should be start of object
                    } else throw new ParseError("Unexpected token in JSON file. Expected start of object or array but got "+token);                                                         
                    token=parser.nextToken(); // this should now be start of object (or end of array)
                    while (token != JsonToken.END_ARRAY) {    
                        if (!JsonToken.START_OBJECT.equals(token)) throw new ParseError("Unexpected token in JSON file #2. Expected start of object but got "+token);
                        token=parser.nextToken(); // this should now be field name or end of object
                        while (token != JsonToken.END_OBJECT) {   
                            if (!JsonToken.FIELD_NAME.equals(token)) throw new ParseError("Unexpected token in JSON file #2. Expected field name but got "+token);
                            String fieldName = parser.getValueAsString();
                            token=parser.nextToken(); // this should now be the value of the field
                            String fieldValue = parser.getValueAsString();
                            if (examples.containsKey(fieldName)) examples.put(fieldName, examples.get(fieldName)+EXAMPLES_DELIMITER+fieldValue);
                            else examples.put(fieldName,fieldValue);                                 
                            token=parser.nextToken(); // this should now be the name of another field or END_OBJECT
                        }                                                
                        token=parser.nextToken(); // this should now be the start of a new object or the end of the array                      
                    }
                }
            }
        } catch (Exception e) {
            gui.logMessage(e.toString());
            // throw e;
        }
//        for (String ex:examples.keySet()) {
//            gui.logMessage("Found example["+ex+"] = "+examples.get(ex));
//        }
        return examples;
    }
    
  
    

private class DescriptionRenderer extends DefaultTableCellRenderer {   
    public DescriptionRenderer() {
       super();
    }
    @Override
    public void setValue(Object value) {
        super.setValue(value);
        String tooltip=(String)value;
        setToolTipText((tooltip==null)?null:MotifLabGUI.formatTooltipString((String)tooltip,80));
    }
}

private class ExampleRenderer extends DefaultTableCellRenderer {   
    public ExampleRenderer() {
       super();
    }
    @Override
    public void setValue(Object value) {
        super.setValue(value);
        String tooltip="<html>"+((String)value).replace(EXAMPLES_DELIMITER,"<br>")+"</html>";
        setToolTipText(tooltip);
    }
}

    

}
