/*
 * DataTrackDialog.java
 *
 * Created on 6. april 2009, 13:32
 */

package motiflab.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowFilter.Entry;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import motiflab.engine.task.CompoundTask;
import motiflab.engine.data.*;
import motiflab.engine.datasource.*;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation_new;

/**
 *
 * @author  kjetikl
 */
public class DataTrackDialog extends javax.swing.JDialog {
    private final String TABLECOLUMN_NAME="Name";    
    private final String TABLECOLUMN_TYPE="Type";
    private final String TABLECOLUMN_SOURCE="Provider";
    private final String TABLECOLUMN_ORGANISM="Supported organism";
    private final String TABLECOLUMN_SUPPORTED="";
    private JTable datatrackTable;
    private DefaultTableModel datatrackTableModel;
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private RowFilter<Object,Object> showSupportedRowFilter=null;
    private RowFilter<Object,Object> expressionRowFilter=null;
    private RowFilter<Object,Object> combinedRowFilter=null;
    private ArrayList<RowFilter<Object,Object>> filters=new ArrayList<RowFilter<Object,Object>>(2);
    private SupportedFilter supportedFilter=null;
    
    /** Creates new form DataTrackDialog */
    public DataTrackDialog(MotifLabGUI gui) {
        super(gui.getFrame(), true);
        this.gui=gui;
        engine=gui.getEngine();
        initComponents();
 
        datatrackTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_TYPE,TABLECOLUMN_SOURCE,TABLECOLUMN_ORGANISM,TABLECOLUMN_SUPPORTED},0);
        datatrackTable=new JTable(datatrackTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        
        datatrackTable.setFillsViewportHeight(true);
        datatrackTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        datatrackTable.setRowSelectionAllowed(true);
        datatrackTable.getTableHeader().setReorderingAllowed(false);
        datatrackTable.getColumn(TABLECOLUMN_SUPPORTED).setMinWidth(20);
        datatrackTable.getColumn(TABLECOLUMN_SUPPORTED).setMaxWidth(20);
        datatrackTable.getColumn(TABLECOLUMN_TYPE).setMaxWidth(50);
        datatrackTable.getColumn(TABLECOLUMN_TYPE).setMaxWidth(50);
        datatrackTable.getColumn(TABLECOLUMN_NAME).setCellRenderer(new DescriptionRenderer());        
        datatrackTable.getColumn(TABLECOLUMN_SUPPORTED).setCellRenderer(new SupportedDatatrackRenderer());
        datatrackTable.getColumn(TABLECOLUMN_TYPE).setCellRenderer(new DataTypeRenderer());
        datatrackTable.getColumn(TABLECOLUMN_ORGANISM).setCellRenderer(new SupportedOrganismsRendered());
        datatrackTable.setRowHeight(18);

        scrollPane.setViewportView(datatrackTable);  
        getRootPane().setDefaultButton(okButton);
        
        DataTrack[] availableTracks=gui.getEngine().getDataLoader().getAvailableDatatracks();
        //System.err.println("Got "+availableTracks.length+" tracks");
        String[] presentBuilds=engine.getDefaultSequenceCollection().getGenomeBuilds(engine);
        for (DataTrack track:availableTracks) {
            String supportedOrganisms=track.getSupportedOrganismsAsString();
            String supportedOrganismsTooltip=track.getSupportedGenomeBuildsTooltip(null);
            Boolean isSupported=track.isSupported(presentBuilds);
            Object[] values=new Object[]{track.getName(),track.getDataType(),track.getSourceSite(),new String[]{supportedOrganisms,supportedOrganismsTooltip},isSupported};
            datatrackTableModel.addRow(values);
        }
        
        datatrackTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int rowcount=datatrackTable.getSelectedRowCount();
                datanameTextfield.setText(getSelectedTrackNamesAsList());
                if (rowcount<2) {
                    datanameTextfield.setEditable(true);
                    int row=datatrackTable.getSelectedRow();
                    if (row>=0) {
                      Boolean issupported=(Boolean)datatrackTable.getValueAt(row, datatrackTable.getColumn(TABLECOLUMN_SUPPORTED).getModelIndex());
                      okButton.setEnabled(issupported);
                    } 
                } else { // multiple rows selected
                    datanameTextfield.setEditable(false);
                    boolean issupported=true;
                    for (int row:datatrackTable.getSelectedRows()) {
                      if (!(Boolean)datatrackTable.getValueAt(row, datatrackTable.getColumn(TABLECOLUMN_SUPPORTED).getModelIndex())) {issupported=false;break;}
                    }
                    okButton.setEnabled(issupported);
                }
            }
        });
        datatrackTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
                   okButton.doClick();
               }
            }           
        });
        // Disable OK-button until a valid selection is made
        okButton.setEnabled(false);
        // do not show supported column?
        //datatrackTable.removeColumn(datatrackTable.getColumn(datatrackTable.getColumnName(TABLECOLUMN_SUPPORTED)));
        //datatrackTable.moveColumn(1, 2);
        datatrackTable.setAutoCreateRowSorter(true);
        datatrackTable.getRowSorter().toggleSortOrder(datatrackTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        filters.add(showSupportedRowFilter);
        filters.add(expressionRowFilter);
        int col=datatrackTableModel.findColumn(TABLECOLUMN_SUPPORTED);
        supportedFilter=new SupportedFilter(col);
        filterTextfield.requestFocusInWindow();
    }

    /** This method is called from within the constructor to
     * init((TableRowSorter)datatrackTable.getRowSorter()).setRowFilter(null);
    }ialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        trackSelectionCombobox = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JPanel();
        dataNameLabel = new javax.swing.JLabel();
        datanameTextfield = new javax.swing.JTextField();
        buttonsPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        filterLabel = new javax.swing.JLabel();
        filterTextfield = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DataTrackDialog.class);
        setTitle(resourceMap.getString("DatatrackDialog.title")); // NOI18N
        setName("DatatrackDialog"); // NOI18N

        topPanel.setMinimumSize(new java.awt.Dimension(100, 46));
        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(100, 46));
        topPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 6, 5));
        jPanel1.setMaximumSize(new java.awt.Dimension(180, 32767));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(180, 100));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        trackSelectionCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Show all tracks", "Show supported tracks" }));
        trackSelectionCombobox.setName("trackSelectionCombobox"); // NOI18N
        trackSelectionCombobox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                tracksSelectionItemStateChanged(evt);
            }
        });
        jPanel1.add(trackSelectionCombobox);

        topPanel.add(jPanel1, java.awt.BorderLayout.LINE_END);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 5, 6, 10));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        dataNameLabel.setText(resourceMap.getString("dataNameLabel.text")); // NOI18N
        dataNameLabel.setName("dataNameLabel"); // NOI18N
        jPanel2.add(dataNameLabel);

        datanameTextfield.setColumns(30);
        datanameTextfield.setText(resourceMap.getString("datanameTextfield.text")); // NOI18N
        datanameTextfield.setName("datanameTextfield"); // NOI18N
        datanameTextfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                datanameTextfieldActionPerformed(evt);
            }
        });
        jPanel2.add(datanameTextfield);

        topPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        buttonsPanel.setMinimumSize(new java.awt.Dimension(100, 90));
        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(100, 42));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        filterLabel.setText(resourceMap.getString("filterLabel.text")); // NOI18N
        filterLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 0));
        filterLabel.setName("filterLabel"); // NOI18N
        jPanel3.add(filterLabel);

        filterTextfield.setColumns(18);
        filterTextfield.setText(resourceMap.getString("filterTextfield.text")); // NOI18N
        filterTextfield.setName("filterTextfield"); // NOI18N
        filterTextfield.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                keyReleasedInFilterTextfield(evt);
            }
        });
        jPanel3.add(filterTextfield);

        buttonsPanel.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMaximumSize(new java.awt.Dimension(75, 27));
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });
        jPanel4.add(okButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setPreferredSize(null);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonPressed(evt);
            }
        });
        jPanel4.add(cancelButton);

        buttonsPanel.add(jPanel4, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        mainPanel.setMinimumSize(new java.awt.Dimension(500, 400));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(700, 400));
        mainPanel.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
    int rowcount=datatrackTable.getSelectedRowCount();
    if (rowcount==0) {setVisible(false);return;} // no datatrack selected    
    else if (rowcount==1) {
        int row=datatrackTable.getSelectedRow();
        if (row<0) {setVisible(false);return;} // no datatrack selected
        String targetName=datanameTextfield.getText().trim();
        String nameError=engine.checkNameValidity(targetName,false);
        if (nameError!=null) {
            JOptionPane.showMessageDialog(this, nameError+":\n"+targetName, "Illegal datatrack name", JOptionPane.ERROR_MESSAGE);
            return;
        }
        setVisible(false);
        Operation operation=gui.getEngine().getOperation("new");
        Object type=datatrackTable.getValueAt(row, datatrackTable.getColumn(TABLECOLUMN_TYPE).getModelIndex());
        String datatype=null;
        if (type instanceof String) datatype=(String)type;
        else if (type instanceof Class) datatype=getTypeStringForClass((Class)type);
        String newTrackSource=(String)datatrackTable.getValueAt(row, datatrackTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        OperationTask newTrackTask=new OperationTask("new "+datatype);
        newTrackTask.setParameter(OperationTask.OPERATION, operation);
        newTrackTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
        newTrackTask.setParameter(OperationTask.TARGET_NAME, targetName);
        newTrackTask.setParameter(OperationTask.SOURCE_NAME, targetName);
        newTrackTask.setParameter(Operation_new.DATA_TYPE, datatype);
        newTrackTask.setParameter(Operation_new.PARAMETERS,Operation_new.DATA_TRACK_PREFIX+newTrackSource);
        Class dataclass=engine.getDataClassForTypeName(datatype);
        newTrackTask.addAffectedDataObject(targetName, dataclass);
        gui.launchOperationTask(newTrackTask, gui.isRecording());
    } else { // selected multiple tracks
        multipleTracksSelected();
    }
    
}//GEN-LAST:event_okButtonPressed

private void multipleTracksSelected() {
    int[] rows=datatrackTable.getSelectedRows();
    if (rows.length==0) {setVisible(false);return;} // no datatrack selected
    setVisible(false);
    CompoundTask addMultipleTracksTask=new CompoundTask("add tracks");
    Operation operation=gui.getEngine().getOperation("new");
    for (int row:rows) {
        String newTrackSource=(String)datatrackTable.getValueAt(row, datatrackTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        String targetName=newTrackSource.replaceAll("[^a-zA-Z_0-9]", "_");
        Object type=datatrackTable.getValueAt(row, datatrackTable.getColumn(TABLECOLUMN_TYPE).getModelIndex());
        String datatype=null;
        if (type instanceof String) datatype=(String)type;
        else if (type instanceof Class) datatype=getTypeStringForClass((Class)type);
        OperationTask newTrackTask=new OperationTask("new "+datatype);
        newTrackTask.setParameter(OperationTask.OPERATION, operation);
        newTrackTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
        newTrackTask.setParameter(OperationTask.TARGET_NAME, targetName);
        newTrackTask.setParameter(OperationTask.SOURCE_NAME, targetName);
        newTrackTask.setParameter(Operation_new.DATA_TYPE, datatype);
        newTrackTask.setParameter(Operation_new.PARAMETERS,Operation_new.DATA_TRACK_PREFIX+newTrackSource);
        Class dataclass=engine.getDataClassForTypeName(datatype);
        newTrackTask.addAffectedDataObject(targetName, dataclass);
        addMultipleTracksTask.addTask(newTrackTask);
    }
    gui.launchOperationTask(addMultipleTracksTask, gui.isRecording());
}

private String getSelectedTrackNamesAsList() {
   int[] rows=datatrackTable.getSelectedRows();
   if (rows==null || rows.length==0) return "";
   StringBuilder string=new StringBuilder();
   for (int i=0;i<rows.length;i++) {
        String newTrackSource=(String)datatrackTable.getValueAt(rows[i], datatrackTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        String targetName=newTrackSource.replaceAll("[^a-zA-Z_0-9]", "_");
        string.append(targetName);
        if (i<rows.length-1) string.append(",");
   }
   return string.toString();
}

private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
   setVisible(false);
}//GEN-LAST:event_cancelButtonPressed

private void datanameTextfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_datanameTextfieldActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_datanameTextfieldActionPerformed

@SuppressWarnings("unchecked")
private void keyReleasedInFilterTextfield(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyReleasedInFilterTextfield
    String text=filterTextfield.getText();//GEN-LAST:event_keyReleasedInFilterTextfield
    int namecol=datatrackTableModel.findColumn(TABLECOLUMN_NAME);
    if (text!=null && text.isEmpty()) expressionRowFilter=null;
    else {
        text=text.replaceAll("\\W", ""); // to avoid problems with regex characters
        expressionRowFilter=RowFilter.regexFilter("(?i)"+text,namecol);
    }
    installFilters();
}

private void tracksSelectionItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_tracksSelectionItemStateChanged
    String selection=(String)trackSelectionCombobox.getSelectedItem();
    if (selection.equals("Show all tracks")) showSupportedRowFilter=null;
    else {   
        int col=datatrackTableModel.findColumn(TABLECOLUMN_SUPPORTED);
        showSupportedRowFilter=supportedFilter;
    }
    installFilters();
}//GEN-LAST:event_tracksSelectionItemStateChanged

@SuppressWarnings("unchecked")
private void installFilters() {
   // RowFilter.regexFilter(null, );
   filters.clear();
   if (showSupportedRowFilter!=null) filters.add(showSupportedRowFilter);
   if (expressionRowFilter!=null) filters.add(expressionRowFilter);
   combinedRowFilter=RowFilter.andFilter(filters);
   ((TableRowSorter)datatrackTable.getRowSorter()).setRowFilter(combinedRowFilter);
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel dataNameLabel;
    private javax.swing.JTextField datanameTextfield;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JTextField filterTextfield;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JPanel topPanel;
    private javax.swing.JComboBox trackSelectionCombobox;
    // End of variables declaration//GEN-END:variables


 private class DescriptionRenderer extends DefaultTableCellRenderer {
    public DescriptionRenderer() {
       super();    
    }
    @Override
    public void setValue(Object value) {
       super.setValue(value);
       String tooltip=null;
       DataTrack track=gui.getEngine().getDataLoader().getDataTrack((String)value);
       if (track!=null) {
           tooltip=track.getDescription();
           if (tooltip!=null && tooltip.isEmpty()) tooltip=null;
       }
       setToolTipText((tooltip==null)?null:MotifLabGUI.formatTooltipString((String)tooltip,80));
    }
}     
    
private class SupportedDatatrackRenderer extends DefaultTableCellRenderer {
    private ImageIcon redbullet;
    private ImageIcon greenbullet;
    public SupportedDatatrackRenderer() {
           super();
           this.setHorizontalAlignment(CENTER);
           java.net.URL redBulletURL=getClass().getResource("resources/icons/redbullet.png");
           java.net.URL greenBulletURL=getClass().getResource("resources/icons/greenbullet.png");
           redbullet=new ImageIcon(redBulletURL); 
           greenbullet=new ImageIcon(greenBulletURL);       
       }
    @Override
    public void setValue(Object value) {
       if (!(value instanceof Boolean)) {
           setIcon(null);
           setToolTipText(null);
       }
       else {
           Boolean bool=(Boolean)value;
           if (bool.booleanValue()) {
               setIcon(greenbullet);
               setToolTipText("This track is available for all required organisms/genome builds");
           } else {
               setIcon(redbullet);
               setToolTipText("This track is NOT available for all required organisms/genome builds");
           }
       }
    }
}

private class DataTypeRenderer extends DefaultTableCellRenderer {
    private SimpleDataPanelIcon numericIcon;
    private SimpleDataPanelIcon regionIcon;
    private SimpleDataPanelIcon DNAIcon;
    public DataTypeRenderer() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);   
       numericIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.NUMERIC_TRACK_GRAPH_ICON,null);
       numericIcon.setForegroundColor(java.awt.Color.BLUE);
       numericIcon.setBackgroundColor(null);
       regionIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.REGION_ICON,null);
       regionIcon.setForegroundColor(java.awt.Color.GREEN);
       regionIcon.setBackgroundColor(null);
       DNAIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.SEQUENCE_ICON_BASES,null);
       DNAIcon.setForegroundColor(java.awt.Color.BLACK);           
       setOpaque(true);
    }
    
   @Override
   public void setValue(Object value) {
       String tooltip=null;
       if (value instanceof String) {
           String val=(String)value;
                if (val.equalsIgnoreCase(NumericDataset.getType())) {setIcon(numericIcon);tooltip="Numeric Dataset";}
           else if (val.equalsIgnoreCase(RegionDataset.getType())) {setIcon(regionIcon);tooltip="Region Dataset";}
           else if (val.equalsIgnoreCase(DNASequenceDataset.getType())) {setIcon(DNAIcon);tooltip="DNA Sequence Dataset";}
       } else if (value instanceof Class) {
           Class cl=(Class)value;
                if (cl==NumericDataset.class) {setIcon(numericIcon);tooltip="Numeric Dataset";}
           else if (cl==RegionDataset.class) {setIcon(regionIcon);tooltip="Region Dataset";}
           else if (cl==DNASequenceDataset.class) {setIcon(DNAIcon);tooltip="DNA Sequence Dataset";}
       }
       setToolTipText(tooltip);
   }
   
}

private String getTypeStringForClass(Class type) {
       if (type==NumericDataset.class) return NumericDataset.getType();
  else if (type==RegionDataset.class) return RegionDataset.getType();
  else if (type==DNASequenceDataset.class) return DNASequenceDataset.getType();
  else return null;
}


private class SupportedOrganismsRendered extends DefaultTableCellRenderer {
    
       public SupportedOrganismsRendered() {
           super();
       }
       @Override
       public void setValue(Object value) {
           if (value instanceof String[]) {
               String[] parts=(String[])value;
               setText(parts[0]);
               setToolTipText(parts[1]);              
           } else {
               setText("<error>");
               setToolTipText(null);   
           }
       }
}

private class SupportedFilter extends RowFilter<Object,Object> {
        int col=0;
        public SupportedFilter(int column) {
            col=column;
        }
        
        @Override
        public boolean include(Entry<? extends Object, ? extends Object> entry) {
            return ((Boolean)entry.getValue(col)).booleanValue();
        }             
}


}
