/*
 * FavoritesDialog.java
 *
 * Created on 24. may 2012, 14:32
 */

package motiflab.gui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import motiflab.engine.task.CompoundTask;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.StandardOperationParser_new;
import motiflab.gui.operationdialog.OperationDialog_new;

/**
 *
 * @author  kjetikl
 */
public class FavoritesDialog extends javax.swing.JDialog {
    private final String TABLECOLUMN_NAME="Name";    
    private final String TABLECOLUMN_TYPE="Type";
    private final String TABLECOLUMN_DESCRIPTION="Description";
    private final String TABLECOLUMN_PARAMETER="Parameter";

    private JTable dataTable;
    private DefaultTableModel dataTableModel;
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private RowFilter<Object,Object> expressionRowFilter=null;
    private RowFilter<Object,Object> combinedRowFilter=null;
    private ArrayList<RowFilter<Object,Object>> filters=new ArrayList<RowFilter<Object,Object>>(2);
    private Favorites favorites;
    
    /** Creates new form DataTrackDialog */
    public FavoritesDialog(MotifLabGUI gui, Favorites favorites) {
        super(gui.getFrame(), true);
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("resources/icons/bookmark.png")));
        this.gui=gui;
        engine=gui.getEngine();
        this.favorites=favorites;
        initComponents();
 
        dataTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_TYPE,TABLECOLUMN_DESCRIPTION,TABLECOLUMN_PARAMETER},0);
        dataTable=new JTable(dataTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        
        dataTable.setFillsViewportHeight(true);
        dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dataTable.setRowSelectionAllowed(true);
        dataTable.getTableHeader().setReorderingAllowed(false);
        dataTable.getColumn(TABLECOLUMN_TYPE).setMaxWidth(50);
        dataTable.getColumn(TABLECOLUMN_TYPE).setMaxWidth(50);
        dataTable.getColumn(TABLECOLUMN_TYPE).setCellRenderer(new DataTypeRenderer());        
        dataTable.getColumn(TABLECOLUMN_DESCRIPTION).setCellRenderer(new DescriptionRenderer());        
        dataTable.setRowHeight(18);

        scrollPane.setViewportView(dataTable);  
        getRootPane().setDefaultButton(okButton);
        
        ArrayList<Favorite> list=favorites.getFavorites();
        for (Favorite favorite:list) {
            addFavoriteToTable(favorite); 
        }
        
        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int rowcount=dataTable.getSelectedRowCount();
                datanameTextfield.setText(getSelectedDataNamesAsList());
                datanameTextfield.setEditable(rowcount<2);
                okButton.setEnabled(true);
                editFavoriteButton.setEnabled(rowcount==1);
                removeFavoriteButton.setEnabled(rowcount>0);
            }
        });
        dataTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
                   okButton.doClick();
               }
            }
            
        });
        // Disable OK-button if there are currently no sequences 
        dataTable.setAutoCreateRowSorter(true);
        dataTable.getRowSorter().toggleSortOrder(dataTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        filters.add(expressionRowFilter);
        addFavoriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFavorite();
            }
        });
        removeFavoriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeFavorite();
            }
        });
        editFavoriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editFavorite();
            }
        });        
        removeFavoriteButton.setEnabled(false);
        editFavoriteButton.setEnabled(false);
        filterTextfield.requestFocusInWindow();
    }

    
    private void addFavoriteToTable(Favorite favorite) {
        Object[] values=new Object[]{favorite.getName(),favorite.getType(),favorite.getDescription(),favorite.getParameter()};
        dataTableModel.addRow(values);       
    }
    private void replaceFavoriteInTable(Favorite oldfavorite, Favorite newfavorite) {
        String oldname=oldfavorite.getName();
        int rows=dataTableModel.getRowCount();
        int namecolumn=dataTable.getColumn(TABLECOLUMN_NAME).getModelIndex();
        int typecolumn=dataTable.getColumn(TABLECOLUMN_TYPE).getModelIndex();
        int paramcolumn=dataTable.getColumn(TABLECOLUMN_PARAMETER).getModelIndex();
        int desccolumn=dataTable.getColumn(TABLECOLUMN_DESCRIPTION).getModelIndex();
        for (int i=0;i<rows;i++) {
            String entryname=(String)dataTableModel.getValueAt(i, namecolumn);
            if (oldname.equalsIgnoreCase(entryname)) {
                dataTableModel.setValueAt(newfavorite.getName(), i, namecolumn);
                dataTableModel.setValueAt(newfavorite.getType(), i, typecolumn);
                dataTableModel.setValueAt(newfavorite.getParameter(), i, paramcolumn);
                dataTableModel.setValueAt(newfavorite.getDescription(), i, desccolumn);
                break;
            }
         }       
    }    
    
    private void removeFavorite() {
        int[] rows=dataTable.getSelectedRows();
        if (rows==null || rows.length==0) return;
        int answer=JOptionPane.showConfirmDialog(rootPane, "Are you sure you want to remove the selected entrie(s)","Remove Favorites",JOptionPane.YES_NO_OPTION);
        if (answer==JOptionPane.YES_OPTION) {    
            int[] modelrows=new int[rows.length];   
            for (int i=0;i<rows.length;i++) modelrows[i]=dataTable.convertRowIndexToModel(rows[i]);
            Arrays.sort(modelrows);
            ArrayList<String> datanames=new ArrayList<String>();
            int namecolumn=dataTable.getColumn(TABLECOLUMN_NAME).getModelIndex();
            for (int row:rows) {
                String name=(String)dataTable.getValueAt(row, namecolumn);
                datanames.add(name);
            }
            for (int i=modelrows.length-1;i>=0;i--) {
                dataTableModel.removeRow(modelrows[i]);
            }            
            for (String name:datanames) {
                favorites.removeFavorite(name);
            }
        }
    }
    
    private void addFavorite() {
        OperationDialog_new newdialog=new OperationDialog_new(gui.getFrame());
        newdialog.initializeForUseInFavorites(gui, null);
        newdialog.setLocation(gui.getFrame().getWidth()/2-newdialog.getWidth()/2, gui.getFrame().getHeight()/2-newdialog.getHeight()/2);
        newdialog.setVisible(true);
        if (newdialog.okPressed()) {
           ExecutableTask task=newdialog.getOperationTask(); 
           if (task instanceof OperationTask) {
               Favorite newfavFavorite=getFavoriteFromTask((OperationTask)task);
               if (favorites.hasFavorite(newfavFavorite.getName())) {
                   JOptionPane.showMessageDialog(rootPane, "An entry with the same name already exists", "Error", JOptionPane.ERROR_MESSAGE);
               } else {
                   addFavoriteToTable(newfavFavorite);
                   favorites.addFavorite(newfavFavorite);      
               }
           }
        }
    }
    
    private void editFavorite() {
        int row=dataTable.getSelectedRow();
        if (row<0) return;
        String favoriteName=(String)dataTable.getValueAt(row, dataTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        Favorite favorite=favorites.getFavoriteByName(favoriteName);
        if (favorite!=null) {
            OperationDialog_new newdialog=new OperationDialog_new(gui.getFrame());
            newdialog.initializeForUseInFavorites(gui, favorite);
            newdialog.setLocation(gui.getFrame().getWidth()/2-newdialog.getWidth()/2, gui.getFrame().getHeight()/2-newdialog.getHeight()/2);                    
            newdialog.setVisible(true);
            if (newdialog.okPressed()) {
               ExecutableTask task=newdialog.getOperationTask(); 
               if (task instanceof OperationTask) {
                   Favorite newfavFavorite=getFavoriteFromTask((OperationTask)task);
                   replaceFavoriteInTable(favorite, newfavFavorite);
                   favorites.replaceFavorite(favorite, newfavFavorite);               
               }
            }           
        }
    }    
    
    private Favorite getFavoriteFromTask(OperationTask task) {
        String name=task.getTargetDataName();
        String description=(String)task.getParameter("Favorites_description");
        String type=(String)task.getParameter(Operation_new.DATA_TYPE);
        String parameters=(String)task.getParameter(Operation_new.PARAMETERS);
        if (parameters==null) parameters="";
        if (description==null) description="";
        if (parameters.equals(Operation_new.FILE_PREFIX)) {
            parameters=StandardOperationParser_new.getImportFromFileParameter(task,engine);
        }
        return new Favorite(name, type, parameters, description);
    }
    
    /** Saves the Favorites repository to a preset file in a background thread */
    private void updateFavorites() {
        SwingWorker worker = new SwingWorker() {
            public Exception ex=null;
            @Override
            protected Object doInBackground() throws Exception {
                try {
                    favorites.saveUpdates();
                } catch (Exception e) {
                    ex=e;
                }
                return null;
            }
            @Override
            protected void done() {
                if (ex!=null) gui.logMessage("An error occurred when saving Favorites: "+ex.getMessage());
            }            
        };
        worker.execute();
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        topPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        addFavoriteButton = new javax.swing.JButton();
        editFavoriteButton = new javax.swing.JButton();
        removeFavoriteButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        dataNameLabel = new javax.swing.JLabel();
        datanameTextfield = new javax.swing.JTextField();
        mainPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        buttonsPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        filterLabel = new javax.swing.JLabel();
        filterTextfield = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(FavoritesDialog.class);
        setTitle(resourceMap.getString("DatatrackDialog.title")); // NOI18N
        setName("DatatrackDialog"); // NOI18N

        topPanel.setMinimumSize(new java.awt.Dimension(100, 46));
        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(100, 40));
        topPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 5));
        jPanel1.setMaximumSize(new java.awt.Dimension(180, 32767));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(240, 46));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 5));

        addFavoriteButton.setText(resourceMap.getString("addFavoriteButton.text")); // NOI18N
        addFavoriteButton.setName("addFavoriteButton"); // NOI18N
        jPanel1.add(addFavoriteButton);

        editFavoriteButton.setText(resourceMap.getString("editFavoriteButton.text")); // NOI18N
        editFavoriteButton.setName("editFavoriteButton"); // NOI18N
        jPanel1.add(editFavoriteButton);

        removeFavoriteButton.setText(resourceMap.getString("removeFavoriteButton.text")); // NOI18N
        removeFavoriteButton.setName("removeFavoriteButton"); // NOI18N
        jPanel1.add(removeFavoriteButton);

        topPanel.add(jPanel1, java.awt.BorderLayout.LINE_END);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        dataNameLabel.setText(resourceMap.getString("dataNameLabel.text")); // NOI18N
        dataNameLabel.setName("dataNameLabel"); // NOI18N
        jPanel2.add(dataNameLabel);

        datanameTextfield.setColumns(25);
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

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        mainPanel.setMinimumSize(new java.awt.Dimension(500, 400));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(700, 400));
        mainPanel.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setMinimumSize(new java.awt.Dimension(100, 33));
        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(100, 38));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 5));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });
        jPanel5.add(okButton);

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
        jPanel5.add(cancelButton);

        jPanel3.add(jPanel5, java.awt.BorderLayout.EAST);

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        filterLabel.setText(resourceMap.getString("filterLabel.text")); // NOI18N
        filterLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 0));
        filterLabel.setName("filterLabel"); // NOI18N
        jPanel6.add(filterLabel);

        filterTextfield.setColumns(16);
        filterTextfield.setText(resourceMap.getString("filterTextfield.text")); // NOI18N
        filterTextfield.setName("filterTextfield"); // NOI18N
        filterTextfield.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                keyReleasedInFilterTextfield(evt);
            }
        });
        jPanel6.add(filterTextfield);

        jPanel3.add(jPanel6, java.awt.BorderLayout.WEST);

        buttonsPanel.add(jPanel3, java.awt.BorderLayout.CENTER);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
    if (favorites.isDirty()) updateFavorites();
    int rowcount=dataTable.getSelectedRowCount();
    if (rowcount==0) {setVisible(false);return;} // no data item selected    
    else if (rowcount==1) {
        int row=dataTable.getSelectedRow();
        if (row<0) {setVisible(false);return;} // no data item selected
        String targetName=datanameTextfield.getText().trim();
        String nameError=engine.checkNameValidity(targetName,false);
        if (nameError!=null) {
            JOptionPane.showMessageDialog(this, nameError+":\n"+targetName, "Illegal data name", JOptionPane.ERROR_MESSAGE);
            return;
        }
        setVisible(false);
        Operation operation=gui.getEngine().getOperation("new");
        Object type=dataTable.getValueAt(row, dataTable.getColumn(TABLECOLUMN_TYPE).getModelIndex());
        String datatype=null;
        if (type instanceof String) datatype=(String)type;
        else if (type instanceof Class) datatype=engine.getTypeNameForDataClass((Class)type);
        String newDataItemName=(String)dataTable.getValueAt(row, dataTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        String newDataItemParameter=(String)dataTable.getValueAt(row, dataTable.getColumn(TABLECOLUMN_PARAMETER).getModelIndex());      
        OperationTask newTask=new OperationTask("new "+datatype);
        newTask.setParameter(OperationTask.OPERATION, operation);
        newTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
        newTask.setParameter(OperationTask.TARGET_NAME, targetName);
        newTask.setParameter(OperationTask.SOURCE_NAME, targetName);
        newTask.setParameter(Operation_new.DATA_TYPE, datatype);
        Class dataclass=engine.getDataClassForTypeName(datatype);        
        if (newDataItemParameter.startsWith(Operation_new.FILE_PREFIX)) {
            try {
                StandardOperationParser_new.parseAndSetImportFromFileParameters(newTask, newDataItemParameter, dataclass, null, engine);
                newTask.setParameter(Operation_new.PARAMETERS,Operation_new.FILE_PREFIX);
            } catch (ParseError e) {newTask.setParameter(Operation_new.PARAMETERS,"*** Parse Error ***");}
        } else newTask.setParameter(Operation_new.PARAMETERS,newDataItemParameter);
        newTask.addAffectedDataObject(targetName, dataclass);
        gui.launchOperationTask(newTask, gui.isRecording());
    } else { // selected multiple tracks
        multipleDataItemsSelected();
    }
    
}//GEN-LAST:event_okButtonPressed

private void multipleDataItemsSelected() {
    int[] rows=dataTable.getSelectedRows();
    if (rows.length==0) {setVisible(false);return;} // no data items selected
    setVisible(false);
    CompoundTask addMultipleDataItemsTask=new CompoundTask("new data");
    Operation operation=gui.getEngine().getOperation("new");
    for (int row:rows) {
        String newDataItemName=(String)dataTable.getValueAt(row, dataTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        String newDataItemParameter=(String)dataTable.getValueAt(row, dataTable.getColumn(TABLECOLUMN_PARAMETER).getModelIndex());
        String targetName=newDataItemName.replaceAll("[^a-zA-Z_0-9]", "_");
        Object type=dataTable.getValueAt(row, dataTable.getColumn(TABLECOLUMN_TYPE).getModelIndex());
        String datatype=null;
        if (type instanceof String) datatype=(String)type;
        else if (type instanceof Class) datatype=engine.getTypeNameForDataClass((Class)type);
        OperationTask newTask=new OperationTask("new "+datatype);
        newTask.setParameter(OperationTask.OPERATION, operation);
        newTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
        newTask.setParameter(OperationTask.TARGET_NAME, targetName);
        newTask.setParameter(OperationTask.SOURCE_NAME, targetName);
        newTask.setParameter(Operation_new.DATA_TYPE, datatype);        
        Class dataclass=engine.getDataClassForTypeName(datatype);        
        if (newDataItemParameter.startsWith(Operation_new.FILE_PREFIX)) {
            try {
                StandardOperationParser_new.parseAndSetImportFromFileParameters(newTask, newDataItemParameter, dataclass, null, engine);
                newTask.setParameter(Operation_new.PARAMETERS,Operation_new.FILE_PREFIX);
            } catch (ParseError e) {newTask.setParameter(Operation_new.PARAMETERS,"*** Parse Error ***");}
        } else newTask.setParameter(Operation_new.PARAMETERS,newDataItemParameter);
        newTask.addAffectedDataObject(targetName, dataclass);
        addMultipleDataItemsTask.addTask(newTask);
    }
    gui.launchOperationTask(addMultipleDataItemsTask, gui.isRecording());
}

private String getSelectedDataNamesAsList() {
   int[] rows=dataTable.getSelectedRows();
   if (rows==null || rows.length==0) return "";
   StringBuilder string=new StringBuilder();
   for (int i=0;i<rows.length;i++) {
        String newSource=(String)dataTable.getValueAt(rows[i], dataTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        String targetName=newSource.replaceAll("[^a-zA-Z_0-9]", "_");
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
    int namecol=dataTableModel.findColumn(TABLECOLUMN_NAME);
    int desccol=dataTableModel.findColumn(TABLECOLUMN_DESCRIPTION);
    if (text!=null && text.isEmpty()) expressionRowFilter=null;
    else {
        text=text.replaceAll("\\W", ""); // to avoid problems with regex characters
        expressionRowFilter=RowFilter.regexFilter("(?i)"+text,namecol,desccol);
    }
    installFilters();
}

@SuppressWarnings("unchecked")
private void installFilters() {
   filters.clear();
   //if (showSupportedRowFilter!=null) filters.add(showSupportedRowFilter);
   if (expressionRowFilter!=null) filters.add(expressionRowFilter);
   combinedRowFilter=RowFilter.andFilter(filters);
   ((TableRowSorter)dataTable.getRowSorter()).setRowFilter(combinedRowFilter);
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFavoriteButton;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel dataNameLabel;
    private javax.swing.JTextField datanameTextfield;
    private javax.swing.JButton editFavoriteButton;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JTextField filterTextfield;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JButton removeFavoriteButton;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables


 private class DescriptionRenderer extends DefaultTableCellRenderer {
    public DescriptionRenderer() {
       super();    
    }
    @Override
    public void setValue(Object value) {
       super.setValue(value);
       if (value instanceof String) {
          setToolTipText(MotifLabGUI.formatTooltipString(value.toString(),80)); 
       } else setToolTipText(null);     
    }
}     

private class DataTypeRenderer extends DefaultTableCellRenderer {

    public DataTypeRenderer() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);    
       setOpaque(true);
    }
    
   @Override
   public void setValue(Object value) {
       if (value==null) {
           setIcon(SimpleDataPanelIcon.getIconForDataType("Unknown"));
           setToolTipText("Unknown");
       } else {
           String type=(value instanceof Class)?engine.getTypeNameForDataClass((Class)value):value.toString();
           setIcon(SimpleDataPanelIcon.getIconForDataType(type));
           setToolTipText(type);
       }
   }
}


}
