/*
 * EditRegionPropertiesDialog.java
 *
 * Created on 27.jan.2011, 16:28:53
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Module;
import motiflab.engine.data.Motif;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;

/**
 *
 * @author Kjetil
 */
public class EditRegionPropertiesDialog extends javax.swing.JDialog {
    Region region;
    MotifLabGUI gui;
    String currentAnchor="Relative"; // do not change this!
    private boolean okpressed=false;
    private int startSpinnerOldValue=0; // just to keep track of which direction we are spinning
    private int endSpinnerOldValue=0; // just to keep track of which direction we are spinning
    private JTable additionalPropertiesTable;

    /** Creates new form EditRegionPropertiesDialog */
    public EditRegionPropertiesDialog(Region region, MotifLabGUI gui) {
        super(gui.getFrame(),"Edit Region Properties" ,true);
        this.region=region;
        this.gui=gui;
        initComponents();
        initializeValues();
        getRootPane().setDefaultButton(okButton);
        okButton.requestFocusInWindow();
        this.setMinimumSize(new java.awt.Dimension(430,565));
        this.setResizable(false);
    }

    private void initializeValues() {
        typeTextfield.setText(region.getType());
        typeTextfield.setCaretPosition(0);
        scoreTextfield.setText(""+region.getScore());
        scoreTextfield.setCaretPosition(0);
             if (region.getOrientation()==Region.DIRECT) orientationCombobox.setSelectedItem("Direct");
        else if (region.getOrientation()==Region.REVERSE) orientationCombobox.setSelectedItem("Reverse");
        else orientationCombobox.setSelectedItem("Undetermined");
        startSpinner.setValue(new Integer(region.getRelativeStart()));
        endSpinner.setValue(new Integer(region.getRelativeEnd()));
        positionCombobox.addActionListener(new PositionAnchorListener());
        positionCombobox.setSelectedItem("Genomic"); // this will also update the values in the textfield
        if (region.isMotif()) {
            showDataPromptButton.setText("Show Motif");
            showDataPromptButton.setVisible(true);
            sequenceLabel.setVisible(true);
            sequenceTextfield.setVisible(true); 
            String bindingsequence=region.getSequence();
            sequenceTextfield.setText((bindingsequence!=null)?bindingsequence:"");
        } else if (region.isModule()) {
            showDataPromptButton.setText("Show Module");
            showDataPromptButton.setVisible(true);
            sequenceLabel.setVisible(false);
            sequenceTextfield.setVisible(false);            
        } else {
            showDataPromptButton.setVisible(false);
            sequenceLabel.setVisible(true);
            sequenceTextfield.setVisible(true); // why not...
            String bindingsequence=region.getSequence();
            sequenceTextfield.setText((bindingsequence!=null)?bindingsequence:"");                        
        }
        sequenceTextfield.setCaretPosition(0);        
        messageLabel.setText(" ");
        startSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newvalue=(Integer)startSpinner.getValue();
                if (newvalue==0 && ((String)positionCombobox.getSelectedItem()).equals("Relative to TSS") && region.getParent().getTSS()!=null && skipZeroCheckbox.isSelected()) {
                    if (startSpinnerOldValue==1) newvalue=-1; //going down
                    else newvalue=1;  //going up
                }
                startSpinnerOldValue=newvalue;
                startSpinner.setValue(newvalue);
                updateLength();
            }
        });
        endSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newvalue=(Integer)endSpinner.getValue();
                if (newvalue==0 && ((String)positionCombobox.getSelectedItem()).equals("Relative to TSS") && region.getParent().getTSS()!=null && skipZeroCheckbox.isSelected()) {
                    if (endSpinnerOldValue==1) newvalue=-1; //going down
                    else newvalue=1;  //going up
                }
                endSpinnerOldValue=newvalue;
                endSpinner.setValue(newvalue);
                updateLength();
            }
        });
        startSpinnerOldValue=(Integer)startSpinner.getValue();
        endSpinnerOldValue=(Integer)endSpinner.getValue();
        updateLength();
        initializeAdditionalPropertiesTable();

    }

    
    private void initializeAdditionalPropertiesTable() {
        HashMap<String,Object[]> props=getAdditionalProperties();
        Object[][] propdata=new Object[props.size()][4];
        int i=0;
        for (String key:props.keySet()) {
           Object[] prop=props.get(key);
           Object value=prop[0];
           Boolean isEditable=(Boolean)prop[1];
           propdata[i][0]=key;
           propdata[i][1]=value;
           propdata[i][2]=(value!=null)?value.getClass():null;
           propdata[i][3]=isEditable;
           i++;
        }
        final DefaultTableModel apTableModel=new DefaultTableModel(propdata, new String[]{"Property","Value","Type","Editable"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return (column<2 && (Boolean)getValueAt(row, 3)); // type column is not editable
            }            
        };
        additionalPropertiesTable = new JTable(apTableModel);
        additionalPropertiesTable.setAutoCreateRowSorter(true);
        additionalPropertiesTable.getTableHeader().setReorderingAllowed(false);
        additionalPropertiesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        additionalPropertiesTable.setCellSelectionEnabled(true);
        additionalPropertiesTable.setDefaultRenderer(Object.class, new UserDefinedPropertiesRenderer());
        additionalPropertiesTable.removeColumn(additionalPropertiesTable.getColumn("Editable")); // do not display this column
        apTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType()==TableModelEvent.UPDATE && e.getColumn()==1) { // update value column
                    Object stringvalue=apTableModel.getValueAt(e.getFirstRow(), 1);
                    Object value=null;
                    if (stringvalue!=null && !stringvalue.toString().trim().isEmpty()) value=Motif.getObjectForPropertyValueString(stringvalue.toString());
                    if (value!=null) {
                        if (value instanceof ArrayList) value="x"; // just a simple conversion to String (or "Text") list lists are not used here
                        apTableModel.setValueAt(value.getClass(),e.getFirstRow(),2);
                    }
                    else apTableModel.setValueAt(null, e.getFirstRow(), 2);
                    additionalPropertiesTable.repaint();
                }
            }
        });
        addPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               Object[] newRowValues=new Object[]{"","",null,Boolean.TRUE};
               apTableModel.addRow(newRowValues);
               int newrow=getFirstEmptyRowInTable(additionalPropertiesTable);
               if (newrow>=0) {
                   boolean canedit=additionalPropertiesTable.editCellAt(newrow, 0);
                   if (canedit) {
                       additionalPropertiesTable.changeSelection(newrow, 0, false, false);
                       additionalPropertiesTable.requestFocus();
                   }
               }
            }
        });                  
        additionalParametersScrollpane.setViewportView(additionalPropertiesTable);
        additionalParametersScrollpane.setMinimumSize(new java.awt.Dimension(300,150));
        additionalPropertiesPanel.setMinimumSize(new java.awt.Dimension(300,180));
        additionalPropertiesTable.getRowSorter().toggleSortOrder(0); // sort by property name alphabetically
    }
    
    private int getFirstEmptyRowInTable(JTable table) {
        for (int i=0;i<table.getRowCount();i++) {
            boolean empty=true;
            for (int j=0;j<2;j++) {
                Object val=table.getValueAt(i, j);
                if (val!=null && !val.toString().isEmpty()) {empty=false;break;}
            }
            if (empty) return i;
        }
        return -1;
    }    
    
    private HashMap<String,Object[]> getAdditionalProperties() {
        Module module=getModuleForRegion();
        HashMap<String, Object[]> props=new HashMap<String,Object[]>();
        for (String key:region.getAllPropertyNames()) {
            Boolean editable=Boolean.TRUE;
            if (module!=null && module.hasMotif(key)) editable=Boolean.FALSE; // Module motif. Do not edit! (do not display it either)
            Object value=region.getProperty(key);
            props.put(key, new Object[]{value,editable});
        }
        return props;
    }
    
    private Module getModuleForRegion() {
        if (region.isModule()) {
            Data data=gui.getEngine().getDataItem(region.getType());
            if (data instanceof Module) return (Module)data;
        }     
        return null;
    }
    
    private Motif getMotifForRegion() {
        if (region.isMotif()) {
            Data data=gui.getEngine().getDataItem(region.getType());
            if (data instanceof Motif) return (Motif)data;
        }     
        return null;
    }    
    
    private void updateLength() {
        boolean doReverse=(currentAnchor.equals("Relative to TSS") && region.getParent().getTSS()!=null && region.getParent().getStrandOrientation()==Sequence.REVERSE);
        int start=(Integer)startSpinner.getValue();
        int end=(Integer)endSpinner.getValue();
        int relativeStart=0; // start of region relative to start of sequence (starting at 0)
        int relativeEnd=0;   // end of region relative to start of sequence (starting at 0)
        if (doReverse) { // swap start and end
            int temp=start;
            start=end;
            end=temp;
        }
        relativeStart=convertToRelativeCoordinates(start, currentAnchor);
        relativeEnd=convertToRelativeCoordinates(end, currentAnchor);
        int length=relativeEnd-relativeStart+1;
        sizeLabel.setText("Size: "+length+" bp");
    }

    private class PositionAnchorListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            boolean doReverse=(currentAnchor.equals("Relative to TSS") && region.getParent().getTSS()!=null && region.getParent().getStrandOrientation()==Sequence.REVERSE);
            int start=(Integer)startSpinner.getValue();
            int end=(Integer)endSpinner.getValue();
            int relativeStart=0; // start of region relative to start of sequence (starting at 0)
            int relativeEnd=0;   // end of region relative to start of sequence (starting at 0)
            if (doReverse) { // swap start and end
                int temp=start;
                start=end;
                end=temp;
            }
            // convert the current coordinates to coordinates relative to start of sequences
            relativeStart=convertToRelativeCoordinates(start, currentAnchor);
            relativeEnd=convertToRelativeCoordinates(end, currentAnchor);
            // convert the sequence start relative coordinates to coordinates relative to selected anchor
            String newAnchor=(String)positionCombobox.getSelectedItem();
            start=convertFromRelativeToOtherCoordinates(relativeStart, newAnchor);
            end=convertFromRelativeToOtherCoordinates(relativeEnd, newAnchor);
            doReverse=(newAnchor.equals("Relative to TSS") && region.getParent().getTSS()!=null && region.getParent().getStrandOrientation()==Sequence.REVERSE);
            if (doReverse) { // swap start and end
                int temp=start;
                start=end;
                end=temp;
            }
            startSpinner.setValue(new Integer(start));
            endSpinner.setValue(new Integer(end));
            currentAnchor=newAnchor;
            skipZeroCheckbox.setVisible(newAnchor.equals("Relative to TSS"));
        }
    }

    private int convertToRelativeCoordinates(int coordinate,String anchor) {
        int relativeCoordinate=0;
        RegionSequenceData seq=region.getParent();
        Integer TSS=seq.getTSS();
        if (anchor.equals("Relative") || (anchor.equals("Relative to TSS") && TSS==null)) {
            relativeCoordinate=coordinate;
        } else if (anchor.equals("Relative to TSS")) {
            // convert TSS-relative coordinates to sequence-relative coordinates
            if (skipZeroCheckbox.isSelected()) {
               if (coordinate>=1) coordinate--;
            }
            if (seq.getStrandOrientation()==Sequence.DIRECT) {
               relativeCoordinate=seq.getRelativePositionFromGenomic(TSS+coordinate);
            } else {
               relativeCoordinate=seq.getRelativePositionFromGenomic(TSS-coordinate);
            }
        } else if (anchor.equals("Genomic")) {
            relativeCoordinate=seq.getRelativePositionFromGenomic(coordinate);
        }
        return relativeCoordinate;
    }

    private int convertFromRelativeToOtherCoordinates(int relativeCoordinate,String anchor) {
        int coordinate=0;
        RegionSequenceData seq=region.getParent();
        Integer TSS=seq.getTSS();
            if (anchor.equals("Relative") || (anchor.equals("Relative to TSS") && TSS==null)) {
                coordinate=relativeCoordinate;
            } else if (anchor.equals("Relative to TSS")) {
                if (seq.getStrandOrientation()==Sequence.DIRECT) {
                    coordinate=seq.getRegionStart()+relativeCoordinate-TSS;
                } else {
                    coordinate=TSS-(seq.getRegionStart()+relativeCoordinate);
                }
                if (skipZeroCheckbox.isSelected()) {
                   if (coordinate>=0) coordinate++;
                }
            } else if (anchor.equals("Genomic")) {
                coordinate=seq.getGenomicPositionFromRelative(relativeCoordinate);
            }

        return coordinate;
    }


    /** Returns TRUE if dialog was closed by pressing OK or FALSE if dialog was closed by pressing CANCEL */
    public boolean okPressed() {
        return okpressed;
    }

    /** Updates the Region properties when OK is pressed */
    private boolean onOKpressed() {
        int start=(Integer)startSpinner.getValue();
        int end=(Integer)endSpinner.getValue();
        String anchor=(String)positionCombobox.getSelectedItem();
        RegionSequenceData seq=region.getParent();
        boolean doReverse=(anchor.equals("Relative to TSS") && seq.getTSS()!=null && seq.getStrandOrientation()==Sequence.REVERSE);
        if (doReverse) { // swap start and end
            int temp=start;
            start=end;
            end=temp;
        }
        int relativeStart=convertToRelativeCoordinates(start, anchor);
        int relativeEnd=convertToRelativeCoordinates(end, anchor);
        if (relativeEnd<0 || relativeStart>=seq.getSize()) {
            messageLabel.setText("Region is outside sequence");
            return false;
        }
        // I will allow regions that cross the boundary, since many loaded tracks contains such regions
//            if ((relativeStart<0 && relativeEnd>=0) || (relativeStart<seq.getSize() && relativeEnd>=seq.getSize())) {
//                messageLabel.setText("Region is partially outside sequence");
//                return false;
//            }
        if (relativeStart>relativeEnd) {
            messageLabel.setText("start is after end");
            return false;
        }
        region.setRelativeStart(relativeStart);
        region.setRelativeEnd(relativeEnd);
        int oldorientation=region.getOrientation();
        String orientation=(String)orientationCombobox.getSelectedItem();
             if (orientation.equals("Direct")) region.setOrientation(Region.DIRECT);
        else if (orientation.equals("Reverse")) region.setOrientation(Region.REVERSE);
        else region.setOrientation(Region.INDETERMINED);
        // if orientation is flipped, flip sequence property also
//            if ((oldorientation==Region.DIRECT && orientation.equals("Reverse")) || (oldorientation==Region.REVERSE && orientation.equals("Direct"))) {
//               String sequence=(String)region.getProperty("sequence");
//               if (sequence!=null) region.setProperty("sequence", MotifLabEngine.reverseSequence(sequence));
//            }
        try {
            double score=Double.parseDouble(scoreTextfield.getText());
            region.setScore(score);
        } catch (NumberFormatException e) {
            // no bother...
        }
        String type=typeTextfield.getText().trim();
        if (!type.isEmpty()) region.setType(type);
        if (sequenceTextfield.isVisible()) {
            String sequence=sequenceTextfield.getText().trim();
            region.setSequence(sequence);
        }        
        
        // parse additional properties
        Module module=getModuleForRegion();
        TableModel additionalPropertiesTableModel=additionalPropertiesTable.getModel();
        int rows=additionalPropertiesTable.getRowCount();
        for (int i=0;i<rows;i++) {
           Object key=additionalPropertiesTable.getValueAt(i, 0);
           Class oldclass=(Class)additionalPropertiesTable.getValueAt(i, 2);
           Boolean isEditable=(Boolean)additionalPropertiesTableModel.getValueAt(additionalPropertiesTable.convertRowIndexToModel(i), 3);
           if (!isEditable) continue; // this can not have been changed anyway
           if (key==null || (key instanceof String && ((String)key).trim().isEmpty())) continue;               
           String propertyName=key.toString().trim();
           if (Region.isReservedProperty(propertyName)) {
               messageLabel.setText("Property '"+propertyName+"' is reserved");
               additionalPropertiesTable.setRowSelectionInterval(i, i);
               additionalPropertiesTable.setColumnSelectionInterval(0, 0);
               additionalPropertiesTable.scrollRectToVisible(additionalPropertiesTable.getCellRect(i,0,true));                   
               return false;                   
           }
           if (module!=null && module.hasMotif(propertyName)) {
               messageLabel.setText("Property '"+propertyName+"' can not be altered");
               additionalPropertiesTable.setRowSelectionInterval(i, i);
               additionalPropertiesTable.setColumnSelectionInterval(0, 0);
               additionalPropertiesTable.scrollRectToVisible(additionalPropertiesTable.getCellRect(i,0,true));                                      
               return false;             
           }               
           Object valueObject=additionalPropertiesTable.getValueAt(i, 1);
           Object oldValue=region.getProperty(propertyName);
           if (valueObject!=null && oldValue!=null && valueObject.equals(oldValue)) continue; // no change
           if (valueObject==null || valueObject.toString().trim().isEmpty()) {
               region.setProperty(propertyName, null); // removes this property
           } else {
               String valueAsString=valueObject.toString().trim();
               Object value=Motif.getObjectForPropertyValueString(valueAsString); // a helper method that will convert "YES/NO" and "TRUE/FALSE" to Boolean objects, numbers to Numeric objects and comma-separated strings to lists 
               if (value instanceof ArrayList) value=MotifLabEngine.splice((ArrayList)value, ",");
               if (value instanceof Integer && (oldclass!=null && oldclass.equals(Double.class))) value=new Double(((Integer)value).doubleValue());
               region.setProperty(propertyName, value);
           }           
        }           
        return true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        topPanel = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        internal1 = new javax.swing.JPanel();
        standardPropertiesPanel = new javax.swing.JPanel();
        startLabel = new javax.swing.JLabel();
        startSpinner = new javax.swing.JSpinner();
        endLabel = new javax.swing.JLabel();
        endSpinner = new javax.swing.JSpinner();
        orientationLabel = new javax.swing.JLabel();
        typeLabel = new javax.swing.JLabel();
        scoreLabel = new javax.swing.JLabel();
        typeTextfield = new javax.swing.JTextField();
        scoreTextfield = new javax.swing.JTextField();
        orientationCombobox = new javax.swing.JComboBox();
        positionCombobox = new javax.swing.JComboBox();
        skipZeroCheckbox = new javax.swing.JCheckBox();
        sizeLabel = new javax.swing.JLabel();
        showDataPromptButton = new javax.swing.JButton();
        sequenceLabel = new javax.swing.JLabel();
        sequenceTextfield = new javax.swing.JTextField();
        additionalPropertiesPanel = new javax.swing.JPanel();
        additionalParametersScrollpane = new javax.swing.JScrollPane();
        internal3 = new javax.swing.JPanel();
        addPropertyButton = new javax.swing.JButton();
        buttonsPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        messageLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        topPanel.setMinimumSize(new java.awt.Dimension(5, 5));
        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(5, 5));
        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5), javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(EditRegionPropertiesDialog.class);
        internal1.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8), javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("internal1.border.insideBorder.title")))); // NOI18N
        internal1.setName("internal1"); // NOI18N
        internal1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        standardPropertiesPanel.setName("standardPropertiesPanel"); // NOI18N
        standardPropertiesPanel.setLayout(new java.awt.GridBagLayout());

        startLabel.setText(resourceMap.getString("startLabel.text")); // NOI18N
        startLabel.setName("startLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 2, 10);
        standardPropertiesPanel.add(startLabel, gridBagConstraints);

        startSpinner.setModel(new javax.swing.SpinnerNumberModel());
        startSpinner.setName("startSpinner"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 2, 0);
        standardPropertiesPanel.add(startSpinner, gridBagConstraints);

        endLabel.setText(resourceMap.getString("endLabel.text")); // NOI18N
        endLabel.setName("endLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 15, 10);
        standardPropertiesPanel.add(endLabel, gridBagConstraints);

        endSpinner.setModel(new javax.swing.SpinnerNumberModel());
        endSpinner.setName("endSpinner"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 15, 0);
        standardPropertiesPanel.add(endSpinner, gridBagConstraints);

        orientationLabel.setText(resourceMap.getString("orientationLabel.text")); // NOI18N
        orientationLabel.setName("orientationLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 2, 10);
        standardPropertiesPanel.add(orientationLabel, gridBagConstraints);

        typeLabel.setText(resourceMap.getString("typeLabel.text")); // NOI18N
        typeLabel.setName("typeLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 2, 10);
        standardPropertiesPanel.add(typeLabel, gridBagConstraints);

        scoreLabel.setText(resourceMap.getString("scoreLabel.text")); // NOI18N
        scoreLabel.setName("scoreLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 8, 10);
        standardPropertiesPanel.add(scoreLabel, gridBagConstraints);

        typeTextfield.setColumns(10);
        typeTextfield.setText(resourceMap.getString("typeTextfield.text")); // NOI18N
        typeTextfield.setName("typeTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 2, 0);
        standardPropertiesPanel.add(typeTextfield, gridBagConstraints);

        scoreTextfield.setColumns(10);
        scoreTextfield.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        scoreTextfield.setText(resourceMap.getString("scoreTextfield.text")); // NOI18N
        scoreTextfield.setName("scoreTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 8, 0);
        standardPropertiesPanel.add(scoreTextfield, gridBagConstraints);

        orientationCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Direct", "Reverse", "Undetermined" }));
        orientationCombobox.setName("orientationCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 0, 0);
        standardPropertiesPanel.add(orientationCombobox, gridBagConstraints);

        positionCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Genomic", "Relative", "Relative to TSS" }));
        positionCombobox.setName("positionCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 17, 2, 0);
        standardPropertiesPanel.add(positionCombobox, gridBagConstraints);

        skipZeroCheckbox.setSelected(true);
        skipZeroCheckbox.setText(resourceMap.getString("skipZeroCheckbox.text")); // NOI18N
        skipZeroCheckbox.setToolTipText(resourceMap.getString("skipZeroCheckbox.toolTipText")); // NOI18N
        skipZeroCheckbox.setName("skipZeroCheckbox"); // NOI18N
        skipZeroCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                skip0clicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        standardPropertiesPanel.add(skipZeroCheckbox, gridBagConstraints);

        sizeLabel.setText(resourceMap.getString("sizeLabel.text")); // NOI18N
        sizeLabel.setName("sizeLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 17, 15, 0);
        standardPropertiesPanel.add(sizeLabel, gridBagConstraints);

        showDataPromptButton.setText(resourceMap.getString("showDataPromptButton.text")); // NOI18N
        showDataPromptButton.setName("showDataPromptButton"); // NOI18N
        showDataPromptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDataPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 2, 0);
        standardPropertiesPanel.add(showDataPromptButton, gridBagConstraints);

        sequenceLabel.setText(resourceMap.getString("sequenceLabel.text")); // NOI18N
        sequenceLabel.setName("sequenceLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 8, 10);
        standardPropertiesPanel.add(sequenceLabel, gridBagConstraints);

        sequenceTextfield.setColumns(10);
        sequenceTextfield.setText(resourceMap.getString("sequenceTextfield.text")); // NOI18N
        sequenceTextfield.setName("sequenceTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 8, 0);
        standardPropertiesPanel.add(sequenceTextfield, gridBagConstraints);

        internal1.add(standardPropertiesPanel);

        mainPanel.add(internal1, java.awt.BorderLayout.PAGE_START);

        additionalPropertiesPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 2, 8), javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("additionalPropertiesPanel.border.insideBorder.title")))); // NOI18N
        additionalPropertiesPanel.setName("additionalPropertiesPanel"); // NOI18N
        additionalPropertiesPanel.setLayout(new java.awt.BorderLayout());

        additionalParametersScrollpane.setName("additionalParametersScrollpane"); // NOI18N
        additionalPropertiesPanel.add(additionalParametersScrollpane, java.awt.BorderLayout.CENTER);

        internal3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 6, 1));
        internal3.setName("internal3"); // NOI18N
        internal3.setPreferredSize(new java.awt.Dimension(386, 34));
        internal3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING));

        addPropertyButton.setText(resourceMap.getString("addPropertyButton.text")); // NOI18N
        addPropertyButton.setName("addPropertyButton"); // NOI18N
        addPropertyButton.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        internal3.add(addPropertyButton);

        additionalPropertiesPanel.add(internal3, java.awt.BorderLayout.PAGE_END);

        mainPanel.add(additionalPropertiesPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(400, 37));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING));

        messageLabel.setText(resourceMap.getString("messageLabel.text")); // NOI18N
        messageLabel.setName("messageLabel"); // NOI18N
        jPanel4.add(messageLabel);

        buttonsPanel.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMaximumSize(new java.awt.Dimension(75, 27));
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.setPreferredSize(new java.awt.Dimension(75, 27));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });
        jPanel3.add(okButton);

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
        jPanel3.add(cancelButton);

        buttonsPanel.add(jPanel3, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
        okpressed=false;
        this.setVisible(false);
    }//GEN-LAST:event_cancelButtonPressed

    private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
        if (!onOKpressed()) return; // values are not OK
        okpressed=true;
        this.setVisible(false);
    }//GEN-LAST:event_okButtonPressed

    private void skip0clicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_skip0clicked
        String anchor=(String)positionCombobox.getSelectedItem();
        if (anchor.equals("Relative to TSS") && region.getParent().getTSS()!=null) {
            boolean skip=skipZeroCheckbox.isSelected();
            int start=(Integer)startSpinner.getValue();
            int end=(Integer)endSpinner.getValue();
            if (!skip) { // turning off skip
                if (start>=1) start--;
                if (end>=1) end--;
            } else { // turning on skip
                if (start>=0) start++;
                if (end>=0) end++;
            }
            startSpinner.setValue(start);
            endSpinner.setValue(end);
        }
    }//GEN-LAST:event_skip0clicked

    private void showDataPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showDataPressed
        Data data=null;
        if (region.isMotif()) data=getMotifForRegion();
        else if (region.isModule()) data=getModuleForRegion();            
       
        if (data!=null) gui.getMotifsPanel().showPrompt(data, false, true); // showPrompt in MotifsPanel will show different prompt depending on whether the Data object is a Motif or a Module
    }//GEN-LAST:event_showDataPressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPropertyButton;
    private javax.swing.JScrollPane additionalParametersScrollpane;
    private javax.swing.JPanel additionalPropertiesPanel;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel endLabel;
    private javax.swing.JSpinner endSpinner;
    private javax.swing.JPanel internal1;
    private javax.swing.JPanel internal3;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox orientationCombobox;
    private javax.swing.JLabel orientationLabel;
    private javax.swing.JComboBox positionCombobox;
    private javax.swing.JLabel scoreLabel;
    private javax.swing.JTextField scoreTextfield;
    private javax.swing.JLabel sequenceLabel;
    private javax.swing.JTextField sequenceTextfield;
    private javax.swing.JButton showDataPromptButton;
    private javax.swing.JLabel sizeLabel;
    private javax.swing.JCheckBox skipZeroCheckbox;
    private javax.swing.JPanel standardPropertiesPanel;
    private javax.swing.JLabel startLabel;
    private javax.swing.JSpinner startSpinner;
    private javax.swing.JPanel topPanel;
    private javax.swing.JLabel typeLabel;
    private javax.swing.JTextField typeTextfield;
    // End of variables declaration//GEN-END:variables

    private class UserDefinedPropertiesRenderer extends DefaultTableCellRenderer {
         public UserDefinedPropertiesRenderer() {
             super();
             //this.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Object propKey=table.getValueAt(row, 0);
            Object propValue=table.getValueAt(row, 1);
            boolean keyDefined=(propKey!=null && !propKey.toString().trim().isEmpty());
            boolean valueDefined=(propValue!=null && !propValue.toString().trim().isEmpty());
            this.setForeground(Color.BLACK);
            if (column==0) { // key column
                if (keyDefined) {
//                    if (valueDefined) this.setForeground(Color.BLACK);
//                    else this.setForeground(Color.LIGHT_GRAY);
                } else { // not key is given
                    if (valueDefined) {
                        this.setText("*** Missing property name ***");
                        this.setForeground(Color.RED);
                    }
                }
                setToolTipText((keyDefined)?propKey.toString():"*** Missing property name ***");
            } else if (column==1) { // value column
                if (valueDefined) {
                    String valuestring=value.toString();
                    if (valuestring.contains(",")) {
                        String split=valuestring.replaceAll("\\s*,\\s*", "<br>");   
                        setToolTipText("<html>"+split+"</html>");  
                    } else setToolTipText(valuestring);
                } else setToolTipText("");
            } else { // type column
                Class classType=(Class)table.getValueAt(row, 2);
                if (classType==null || !valueDefined || !keyDefined) this.setText("");
                else if (List.class.isAssignableFrom(classType)) this.setText("List"); // this is currently not used. Lists are displayed as "Text"
                else if (Number.class.isAssignableFrom(classType)) this.setText("Numeric");
                else if (classType.equals(String.class)) this.setText("Text");
                else this.setText(classType.getSimpleName());
            }
            return this;
        }
    }
}
