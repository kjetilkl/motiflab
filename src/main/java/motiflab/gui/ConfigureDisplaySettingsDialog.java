/*
 * ConfigureDisplaySettingsDialog.java
 *
 * Created on Dec 2, 2013, 1:31:20 PM
 */
package motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;


/**
 *
 * @author kjetikl
 */
public class ConfigureDisplaySettingsDialog extends javax.swing.JDialog {
    private static final String TABLECOLUMN_NAME="Name";
    private static final String TABLECOLUMN_TYPE="Type";
    private static final String TABLECOLUMN_VALUE="Value";
    
    private static final int COLUMN_NAME=0;
    private static final int COLUMN_TYPE=1;
    private static final int COLUMN_VALUE=2;
    
    
    private MotifLabGUI gui;
    private DefaultTableModel tablemodel;
    private JTable table;
    private VisualizationSettings settings;
    private SimpleDataPanelIcon newSettingColorIcon=new SimpleDataPanelIcon(16,16,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER_INSIDE, null);

    private HashSet<String> uneditableSettings=new HashSet<String>();
    private HashSet<Class> recognizedTypes=new HashSet<Class>();
    
    
    /** Creates new form ConfigureDataRepositorysDialog */
    public ConfigureDisplaySettingsDialog(final MotifLabGUI gui) {
        super(gui.getFrame(), "Display Settings", true);
        this.gui=gui;
        this.settings=gui.getVisualizationSettings();
        initComponents();
        // --- the following components were added after the Swing Application Framework lost support in NetBeans...
        JButton clearFilteredSettingsButton = new JButton();
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(ConfigureDisplaySettingsDialog.class, this);
        clearFilteredSettingsButton.setAction(actionMap.get("clearFilteredSettings")); // 
        clearFilteredSettingsButton.setText("Clear Filtered Settings"); // 
        clearFilteredSettingsButton.setName("clearFilteredSettingsButton"); // 
        headerPanelRight.add(clearFilteredSettingsButton,0);
        // --- end ---
        headerPanelLeft.add(new JLabel("  Filter "),0); // 
        newSettingColorIcon.setForegroundColor(Color.RED);
        uneditableSettings.addAll(Arrays.asList(new String[]{"Javascript","CSS","stylesheet"}));
        recognizedTypes.add(Boolean.class);
        recognizedTypes.add(Integer.class);
        recognizedTypes.add(Double.class);
        recognizedTypes.add(String.class);
        recognizedTypes.add(Color.class);
        
        getRootPane().setDefaultButton(closeButton);
        tablemodel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_TYPE,TABLECOLUMN_VALUE},0);
        table=new JTable(tablemodel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                if (1<2) return false; // After some consideration I have decided to not allow ANY edits of existing properties since they can potentially mess up a lot :(
                String settingname=(String)table.getValueAt(row, COLUMN_NAME);
                if (uneditableSettings.contains(settingname)) return false;
                return (col==COLUMN_VALUE);
            }
        };
        table.setDefaultRenderer(Object.class, new SettingRenderer());
        SettingEditor settingEditor=new SettingEditor(table);
        table.setDefaultEditor(Object.class, settingEditor);
        populateTable();
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);             
        table.setRowHeight(18);
        table.getColumn(TABLECOLUMN_TYPE).setMinWidth(70);
        table.getColumn(TABLECOLUMN_TYPE).setMaxWidth(70);
        table.getColumn(TABLECOLUMN_TYPE).setPreferredWidth(70);
        scrollPane.setViewportView(table);  
                
        table.setAutoCreateRowSorter(true);
        table.getRowSorter().toggleSortOrder(table.getColumn(TABLECOLUMN_NAME).getModelIndex());
//        table.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//               super.mouseClicked(e);
//               if (e.getClickCount()<2) return;
//               int row=table.getSelectedRow();
//            }             
//        });                           
        this.setPreferredSize(new Dimension(700,400));

        newSettingColorLabel.setIcon(newSettingColorIcon);
        ColorMenuListener colorListener = new ColorMenuListener() {
            @Override
            public void newColorSelected(Color color) {
                if (color!=null) newSettingColorIcon.setForegroundColor(color);
                newSettingColorLabel.repaint();
            }
        };
        ColorMenu colormenu=new ColorMenu(null, colorListener, newSettingColorLabel);
        final JPopupMenu popup=colormenu.wrapInPopup();
        newSettingColorLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                popup.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        });                                    
        newSettingTypeCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               String type=(String)newSettingTypeCombobox.getSelectedItem();
               if (type.equals("Boolean")) {
                   newSettingBooleanCheckbox.setVisible(true);
                   newSettingColorLabel.setVisible(false);
                   newSettingValueTextField.setVisible(false);
               } else if (type.equals("Color")) {
                   newSettingBooleanCheckbox.setVisible(false);
                   newSettingColorLabel.setVisible(true);
                   newSettingValueTextField.setVisible(false);                  
               } else {
                   newSettingBooleanCheckbox.setVisible(false);
                   newSettingColorLabel.setVisible(false);
                   newSettingValueTextField.setVisible(true);                  
               }
               buttonsPanel.revalidate();
               buttonsPanel.repaint();
            }
        }); 
        newSettingNameTextField.setText("new setting name");
        newSettingValueTextField.setText("0");
        newSettingTypeCombobox.setSelectedItem("Integer");
        pack();     
        settingEditor.addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                Object[] info=((SettingEditor)e.getSource()).getInfo();
                String name=(String)info[0];
                Class type=(Class)info[1];
                Object oldvalue=info[2];
                Object newvalue=info[3];
                gui.logMessage(info[0]+"["+info[1]+"] "+info[2]+" => "+info[3]);
                if (newvalue==oldvalue || newvalue==null) return; // no change in value
                if (oldvalue!=null && newvalue.getClass()!=oldvalue.getClass()) return; // oops :-|
                setProperty(name,newvalue);
            }

            @Override
            public void editingCanceled(ChangeEvent e) {
                //
            }
        });
    }

    private void populateTable() {
        ArrayList<String> keys=gui.getVisualizationSettings().getAllKeys(false);
        for (String key:keys) {
            Object value=settings.getSetting(key);
            if (value==null || !recognizedTypes.contains(value.getClass())) uneditableSettings.add(key);
            Class type=(value==null)?null:value.getClass();
            Object[] values=new Object[]{key,type,value}; // 
            tablemodel.addRow(values);
        }          
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        headerPanel = new javax.swing.JPanel();
        headerPanelLeft = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        filterTextField = new javax.swing.JTextField();
        headerPanelRight = new javax.swing.JPanel();
        clearAllSettingsButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        buttonsPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();
        addNewSettingPanel = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        newSettingNameTextField = new javax.swing.JTextField();
        newSettingTypeCombobox = new javax.swing.JComboBox();
        newSettingValueTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        newSettingBooleanCheckbox = new javax.swing.JCheckBox();
        newSettingColorLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        headerPanel.setName("headerPanel"); // NOI18N
        headerPanel.setLayout(new java.awt.BorderLayout());

        headerPanelLeft.setName("headerPanelLeft"); // NOI18N
        headerPanelLeft.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setName("jLabel2"); // NOI18N
        headerPanelLeft.add(jLabel2);

        filterTextField.setColumns(20);
        filterTextField.setName("filterTextField"); // NOI18N
        filterTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                keyReleasedInFilterTextfield(evt);
            }
        });
        headerPanelLeft.add(filterTextField);

        headerPanel.add(headerPanelLeft, java.awt.BorderLayout.WEST);

        headerPanelRight.setName("headerPanelRight"); // NOI18N
        headerPanelRight.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(ConfigureDisplaySettingsDialog.class, this);
        clearAllSettingsButton.setAction(actionMap.get("clearAllSettings")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ConfigureDisplaySettingsDialog.class);
        clearAllSettingsButton.setText(resourceMap.getString("clearAllSettingsButton.text")); // NOI18N
        clearAllSettingsButton.setName("clearAllSettingsButton"); // NOI18N
        headerPanelRight.add(clearAllSettingsButton);

        headerPanel.add(headerPanelRight, java.awt.BorderLayout.CENTER);

        getContentPane().add(headerPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 6, 1, 6));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        jPanel2.add(scrollPane, java.awt.BorderLayout.CENTER);

        mainPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 30, 0, 0));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonPressed(evt);
            }
        });
        jPanel1.add(closeButton);

        buttonsPanel.add(jPanel1, java.awt.BorderLayout.EAST);

        addNewSettingPanel.setName("addNewSettingPanel"); // NOI18N
        addNewSettingPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonPressed(evt);
            }
        });
        addNewSettingPanel.add(addButton);

        newSettingNameTextField.setColumns(10);
        newSettingNameTextField.setName("newSettingNameTextField"); // NOI18N
        addNewSettingPanel.add(newSettingNameTextField);

        newSettingTypeCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Boolean", "Color", "Double", "Integer", "String" }));
        newSettingTypeCombobox.setName("newSettingTypeCombobox"); // NOI18N
        addNewSettingPanel.add(newSettingTypeCombobox);

        newSettingValueTextField.setColumns(10);
        newSettingValueTextField.setName("newSettingValueTextField"); // NOI18N
        addNewSettingPanel.add(newSettingValueTextField);

        jLabel1.setName("jLabel1"); // NOI18N
        addNewSettingPanel.add(jLabel1);

        newSettingBooleanCheckbox.setSelected(true);
        newSettingBooleanCheckbox.setName("newSettingBooleanCheckbox"); // NOI18N
        addNewSettingPanel.add(newSettingBooleanCheckbox);

        newSettingColorLabel.setName("newSettingColorLabel"); // NOI18N
        addNewSettingPanel.add(newSettingColorLabel);

        buttonsPanel.add(addNewSettingPanel, java.awt.BorderLayout.WEST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonPressed
        setVisible(false);
    }//GEN-LAST:event_closeButtonPressed

    private void addButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonPressed
       String error=null;
       String type=(String)newSettingTypeCombobox.getSelectedItem();
       String settingName=newSettingNameTextField.getText();
       if (settingName==null) settingName=""; else settingName=settingName.trim();
       if (settingName.isEmpty()) error="Missing name for new setting";
       if (settingName.equalsIgnoreCase("new setting name")) error="Please select a meaningful name for your new setting";
       Object value=null;
       if (settings.hasSetting(settingName)) error="A setting named '"+settingName+"' already exists!\nPlease edit it directly in the table instead.";
       if (type.equals("Boolean")) {
           value=(Boolean)newSettingBooleanCheckbox.isSelected();
       } else if (type.equals("Color")) {
           value=newSettingColorIcon.getForegroundColor();            
       } else if (type.equals("String")) {
           value=newSettingValueTextField.getText();       
       } else if (type.equals("Integer")) {
           String string=newSettingValueTextField.getText().trim();        
           try {
               if (string.isEmpty()) error="Missing integer value";
               else value=new Integer(Integer.parseInt(string));          
           } catch (NumberFormatException e) {error="Not a valid integer value";}
       } else if (type.equals("Double")) {
           String string=newSettingValueTextField.getText();
           try {
               if (string.isEmpty()) error="Missing numeric value";
               else value=new Double(Double.parseDouble(string));          
           } catch (NumberFormatException e) {error="Not a valid numeric value";}           
       } 
       if (error!=null) {
           JOptionPane.showMessageDialog(rootPane, error, "Setting Error", JOptionPane.ERROR_MESSAGE);
       } else {       
          setProperty(settingName, value);       
          // add to table also
          Class classtype=null;
          if (value!=null) classtype=value.getClass();
          Object[] values=new Object[]{settingName,classtype,value};
          tablemodel.addRow(values);
          int newRowIndex=-1;
          for (int i=0;i<table.getRowCount();i++) {
              if (table.getValueAt(i, COLUMN_NAME).equals(settingName)) {newRowIndex=i;break;}
          }
          if (newRowIndex>=0) highLightRow(newRowIndex);
       } 
    }//GEN-LAST:event_addButtonPressed

    private void keyReleasedInFilterTextfield(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyReleasedInFilterTextfield
        String text=filterTextField.getText();                                             
        RowFilter expressionRowFilter;
        if (text!=null && text.isEmpty()) expressionRowFilter=null;
        else {
            //text=text.replaceAll("\\W", ""); // to avoid problems with regex characters
            expressionRowFilter=RowFilter.regexFilter("(?i)"+text);
        }
        ((TableRowSorter)table.getRowSorter()).setRowFilter(expressionRowFilter);
    }//GEN-LAST:event_keyReleasedInFilterTextfield

    
    public void highLightRow(int rowIndex) {
        table.scrollRectToVisible(table.getCellRect(rowIndex,0, true)); 
        table.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
    }    
    
    private void setProperty(String name, Object value) { // ensures that the 
       String dataname=name;
       if (name.contains(".")) dataname=name.substring(name.lastIndexOf("."));
       if (name.endsWith(".visible")) {
           
       } else if (name.endsWith(".graphType")) {
           
       } else if (name.endsWith(".trackHeight") || name.endsWith(".expandedRegionsHeight") || name.endsWith(".expandedRegionsRowSpacing")) {
                     
       } else if (name.endsWith(".regionTrackExpanded") || name.endsWith(".trackMargin") || name.endsWith(".regionSpacingMargin")) {
           
       } else 
       settings.storeSetting(name, value);       
       settings.redraw();        
    }
    
    
    private class SettingRenderer extends DefaultTableCellRenderer {
        SimpleDataPanelIcon coloricon;
        JCheckBox checkbox;
        CheckBoxIcon checkBoxIcon; 
        public SettingRenderer() {
            coloricon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
            checkbox=new JCheckBox();
            // checkboxPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,2,0));
            checkBoxIcon=new CheckBoxIcon(checkbox);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component parent=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel label=(JLabel)parent;          
            if (column==COLUMN_VALUE) {
                if (value instanceof Color) {
                    coloricon.setForegroundColor((Color)value);
                    label.setIcon(coloricon);
                    //label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setText(null);           
                } else if (value instanceof Boolean) {
                    checkbox.setSelected((Boolean)value);                    
                    label.setIcon(checkBoxIcon);
                    label.setText("");
                } else {      
                    label.setIcon(null);
                    //label.setHorizontalAlignment((value instanceof Number)?SwingConstants.RIGHT:SwingConstants.LEFT);
                    label.setText((value!=null)?value.toString():"");            
                }
            } else if (column==COLUMN_TYPE){ // 
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setIcon(null);
                if (value!=null && recognizedTypes.contains((Class)value)) {
                    label.setText(((Class)value).getSimpleName());
                } else {
                    //label.setForeground(Color.LIGHT_GRAY);
                    label.setText("Special");
                }
            } else { // 
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setIcon(null);
                //if (uneditableSettings.contains((String)value)) label.setForeground(Color.LIGHT_GRAY);               
                label.setForeground(Color.BLACK);
            }
            return parent;
        }    
    }  
    
    private class SettingEditor extends AbstractCellEditor implements TableCellEditor, CellEditorListener {
        ColorEditor colorEditor;
        DefaultCellEditor checkBoxEditor;
        DefaultCellEditor textEditor;
        JCheckBox checkbox;
        JTextField textfield;
        TableCellEditor currentEditor;
        Border offsetBorder=BorderFactory.createEmptyBorder(0,3,0,0);
        Border emptyBorder=BorderFactory.createEmptyBorder(0,5,0,0);        
        Border lineBorder=BorderFactory.createLineBorder(Color.BLACK);
        
        Class settingType=null;
        Object oldValue=null;
        String settingName=null;   
        Object newValue=null;
        
        
        public SettingEditor(Component parent) {
            colorEditor=new ColorEditor(parent);
            checkbox=new JCheckBox();
            checkBoxEditor=new DefaultCellEditor(checkbox);
            textfield=new JTextField();
            textEditor=new DefaultCellEditor(textfield); 
            colorEditor.addCellEditorListener(this);
            checkBoxEditor.addCellEditorListener(this);
            textEditor.addCellEditorListener(this);
        }
        
        @Override
        public Object getCellEditorValue() {
            return newValue;
        }
        
        public Object[] getInfo() {
            return new Object[]{settingName, settingType, oldValue, getCellEditorValue()};
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int column) {
            oldValue=value;
            settingName=(String)table.getValueAt(row, COLUMN_NAME);
            settingType=(Class)table.getValueAt(row, COLUMN_TYPE);            
            if (value instanceof Boolean) currentEditor=checkBoxEditor;
            else if (value instanceof Color) currentEditor=colorEditor;
            else currentEditor=textEditor;
            JComponent comp=(JComponent)currentEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
            if (currentEditor==checkBoxEditor) comp.setBorder(offsetBorder);
            else if (currentEditor==colorEditor) comp.setBorder(emptyBorder);
            else comp.setBorder(lineBorder);
            return comp;          
        }

        @Override
        public void editingCanceled(ChangeEvent e) {
            newValue=oldValue;
            fireEditingCanceled();
        }

        @Override
        public void editingStopped(ChangeEvent e) {
            newValue=currentEditor.getCellEditorValue();
            gui.logMessage("Newvalue="+newValue+"  "+newValue.getClass()+"   expected="+settingType);
            if (settingType==Integer.class) { // check if the new value is valid
                if (newValue instanceof String) {
                    try {newValue=Integer.parseInt((String)newValue);} catch (NumberFormatException nfe) {editingCanceled(e);return;}
                }
            } else if (settingType==Double.class) {
                if (newValue instanceof String) {
                    try {newValue=Double.parseDouble((String)newValue);} catch (NumberFormatException nfe) {editingCanceled(e);return;}
                }                
            }
            if (newValue.getClass()!=settingType) {editingCanceled(e);return;} // last check           
            fireEditingStopped();
        }

        
    }

        
    private class ColorEditor extends AbstractCellEditor implements TableCellEditor, ColorMenuListener {
        Color currentColor=null;
        JLabel renderer;
        ColorMenu colormenu;
        JPopupMenu popup;   
        SimpleDataPanelIcon coloricon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);

        public ColorEditor(Component parent) {
            renderer=new JLabel(coloricon);
            renderer.setHorizontalAlignment(SwingConstants.LEFT);
            colormenu=new ColorMenu(null, this, parent);
            popup=colormenu.wrapInPopup();
            renderer.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent evt) {
                    popup.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            });             
        }
        
        @Override
        public Object getCellEditorValue() {
            return currentColor;
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table,Object value,boolean isSelected,int row,int column) {
            currentColor = (Color)value;
            coloricon.setForegroundColor(currentColor);           
            if (isSelected) renderer.setBackground(table.getSelectionBackground());
            return renderer;
        }

        @Override
        public void newColorSelected(Color color) {
            currentColor = color;
            coloricon.setForegroundColor(color);
            renderer.repaint();
            fireEditingStopped();
        }
        
    }    
    
  // paints component as icon  
  private class CheckBoxIcon implements Icon {
      private final JCheckBox check;
      private int marginX=2;
      public CheckBoxIcon(JCheckBox check) {
        this.check = check;
      }
      @Override public int getIconWidth() {
        return check.getPreferredSize().width;
      }
      @Override public int getIconHeight() {
        return check.getPreferredSize().height;
      }
      @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        SwingUtilities.paintComponent(
            g, check, (Container)c, x-marginX, y, getIconWidth(), getIconHeight());
      }
}

    @org.jdesktop.application.Action
    public void clearAllSettings() {
        settings.clearAllSettings();
        // Remove all the rows from the table
        int rowCount = tablemodel.getRowCount();
        for (int i=rowCount-1; i>=0; i--) {
            tablemodel.removeRow(i);
        }
    }
    
    @org.jdesktop.application.Action
    public void clearFilteredSettings() {
        String text=filterTextField.getText();
        if (text==null || text.trim().isEmpty()) return;
        settings.clearAllSettingsFor(text, true);
        gui.redraw();
        // Remove all the rows from the table
        int rowCount = tablemodel.getRowCount();
        for (int i=rowCount-1; i>=0; i--) {
            tablemodel.removeRow(i);
        }
        populateTable();        
    }    
  


  
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JPanel addNewSettingPanel;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton clearAllSettingsButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JPanel headerPanelLeft;
    private javax.swing.JPanel headerPanelRight;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JCheckBox newSettingBooleanCheckbox;
    private javax.swing.JLabel newSettingColorLabel;
    private javax.swing.JTextField newSettingNameTextField;
    private javax.swing.JComboBox newSettingTypeCombobox;
    private javax.swing.JTextField newSettingValueTextField;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
