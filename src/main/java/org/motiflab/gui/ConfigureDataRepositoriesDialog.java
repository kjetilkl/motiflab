/*
 * ConfigureDataRepositorysDialog.java
 *
 * Created on Dec 2, 2013, 1:31:20 PM
 */
package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.datasource.DataRepository;


/**
 *
 * @author kjetikl
 */
public class ConfigureDataRepositoriesDialog extends javax.swing.JDialog {
    private static final String TABLECOLUMN_NAME="Name";
    private static final String TABLECOLUMN_TYPE="Type";
    private static final String TABLECOLUMN_DESCRIPTION="Configuration";

    private static final int COLUMN_NAME=0;
    private static final int COLUMN_TYPE=1;
    private static final int COLUMN_DESCRIPTION=2;


    private MotifLabGUI gui;
    private DefaultTableModel repositoryModel;
    private JTable repositoryTable;



    /** Creates new form ConfigureDataRepositorysDialog */
    public ConfigureDataRepositoriesDialog(final MotifLabGUI gui) {
        super(gui.getFrame(), "Data Repositories", true);
        this.gui=gui;
        initComponents();
        infoButton.setVisible(false);
        getRootPane().setDefaultButton(closeButton);
        repositoryModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_TYPE,TABLECOLUMN_DESCRIPTION},0);
        repositoryTable=new JTable(repositoryModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        for (DataRepository repository:gui.getEngine().getDataRepositories()) {
            String type=repository.getRepositoryType();
            String name=repository.getRepositoryName();
            String description=repository.getConfigurationString();
            Object[] values=new Object[]{name,type,description}; // the null is just a placeholder
            repositoryModel.addRow(values);
        }
        repositoryTable.setFillsViewportHeight(true);
        repositoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repositoryTable.setRowSelectionAllowed(true);
        repositoryTable.getTableHeader().setReorderingAllowed(false);
        repositoryTable.setRowHeight(18);
        scrollPane.setViewportView(repositoryTable);

        repositoryTable.setAutoCreateRowSorter(true);
        repositoryTable.getRowSorter().toggleSortOrder(repositoryTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        repositoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getClickCount()<2) return;
               int row=repositoryTable.getSelectedRow();
               String repositoryname=(String)repositoryTable.getValueAt(row, COLUMN_NAME);
               DataRepository repository=gui.getEngine().getDataRepository(repositoryname);
               addOrEdit(repository);
               //showDataRepositoryInfo(repository);
            }
        });
        repositoryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row=repositoryTable.getSelectedRow();
                if (row>=0) {
                    infoButton.setEnabled(true);
                    removeButton.setEnabled(true);
                    configureButton.setEnabled(true);
                } else {
                    infoButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    configureButton.setEnabled(false);
                }
            }
        });
        this.setPreferredSize(new Dimension(700,400));
        pack();
    }


    private void showDataRepositoryInfo(DataRepository repository) {
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String document=repository.getConfigurationString(); // I should probably replace this with something better
        InfoDialog dialog=new InfoDialog(gui, "Data Repository", document,400,200);
        gui.getFrame().setCursor(Cursor.getDefaultCursor());
        dialog.setVisible(true);
        dialog.dispose();
    }

    private void removeDataRepositoryInListAndEngine(DataRepository repository) {
        String repositoryName=repository.getRepositoryName();
        for (int i=0;i<repositoryModel.getRowCount();i++) {
           if (repositoryModel.getValueAt(i,COLUMN_NAME).equals(repositoryName)) {
               repositoryModel.removeRow(i);
               break;
           }
        }
        gui.getEngine().unregisterDataRepository(repository);
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
        mainPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        buttonsPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        configureButton = new javax.swing.JButton();
        infoButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        headerPanel.setName("headerPanel"); // NOI18N
        headerPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
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

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ConfigureDataRepositoriesDialog.class);
        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonPressed(evt);
            }
        });
        jPanel1.add(closeButton);

        buttonsPanel.add(jPanel1, java.awt.BorderLayout.EAST);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonPressed(evt);
            }
        });
        jPanel3.add(addButton);

        removeButton.setText(resourceMap.getString("removeButton.text")); // NOI18N
        removeButton.setEnabled(false);
        removeButton.setName("removeButton"); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonPressed(evt);
            }
        });
        jPanel3.add(removeButton);

        configureButton.setText(resourceMap.getString("configureButton.text")); // NOI18N
        configureButton.setEnabled(false);
        configureButton.setName("configureButton"); // NOI18N
        configureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configureButtonPressed(evt);
            }
        });
        jPanel3.add(configureButton);

        infoButton.setIcon(resourceMap.getIcon("infoButton.icon")); // NOI18N
        infoButton.setText(resourceMap.getString("infoButton.text")); // NOI18N
        infoButton.setEnabled(false);
        infoButton.setName("infoButton"); // NOI18N
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoButtonPressed(evt);
            }
        });
        jPanel3.add(infoButton);

        buttonsPanel.add(jPanel3, java.awt.BorderLayout.WEST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonPressed
        gui.getEngine().storeDataRepositoryConfigurations();
        setVisible(false);
    }//GEN-LAST:event_closeButtonPressed

    private void addButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonPressed
        addOrEdit(null);
    }//GEN-LAST:event_addButtonPressed

    private void removeButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonPressed
       int row=repositoryTable.getSelectedRow();
       if (row<0) return;
       String repositoryName=(String)repositoryTable.getValueAt(row, COLUMN_NAME);
       DataRepository repository=gui.getEngine().getDataRepository(repositoryName);
       int option=JOptionPane.showConfirmDialog(rootPane, "Are you sure you want to remove \""+repositoryName+"\" ?", "Remove Data Repository", JOptionPane.YES_NO_OPTION);
       if (option==JOptionPane.YES_OPTION && repository!=null) {
            removeDataRepositoryInListAndEngine(repository);
       }
    }//GEN-LAST:event_removeButtonPressed

    private void configureButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureButtonPressed
       int row=repositoryTable.getSelectedRow();
       if (row<0) return;
       String repositoryName=(String)repositoryTable.getValueAt(row, COLUMN_NAME);
       DataRepository repository=gui.getEngine().getDataRepository(repositoryName);
       addOrEdit(repository);
    }//GEN-LAST:event_configureButtonPressed

    private void infoButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoButtonPressed
       int row=repositoryTable.getSelectedRow();
       if (row<0) return;
       String repositoryName=(String)repositoryTable.getValueAt(row, COLUMN_NAME);
       DataRepository repository=gui.getEngine().getDataRepository(repositoryName);
       showDataRepositoryInfo(repository);
    }//GEN-LAST:event_infoButtonPressed


    private void addOrEdit(DataRepository repository) {
        final boolean addNew=(repository==null);
        Parameter[] parameters=(addNew)?new Parameter[0]:repository.getConfigurationParameters();
        final ParameterSettings currentSettings=(addNew)?null:repository.getConfigurationParameterSettings();

        final ParametersPanelPrompt dialog=new ParametersPanelPrompt(gui.getFrame(), gui.getEngine(), null, ((addNew)?"Add New":"Edit")+" Data Repository", parameters, currentSettings);
        final JTextField repositoryNameField=new JTextField(16);
        String[] types=gui.getEngine().getDataRepositoryTypes();
        final JComboBox<String> typeCombobox=new JComboBox<String>(types);
        typeCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String typeName=(String)typeCombobox.getSelectedItem();
                Class repositoryClass=gui.getEngine().getDataRepositoryType(typeName);
                try {
                    DataRepository newRepository=(DataRepository)repositoryClass.newInstance();
                    Parameter[] newParames=newRepository.getConfigurationParameters();
                    ParameterSettings newParameSettings=(currentSettings!=null)?currentSettings:newRepository.getConfigurationParameterSettings();
                    dialog.updateParameters(newParames, newParameSettings);
                    dialog.pack();
                } catch (Exception ex) {}
            }
        });
        dialog.getInnerPanel().setBorder(BorderFactory.createTitledBorder("Parameters"));
        JPanel commonSettingsPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        commonSettingsPanel.add(new JLabel("Name: "));
        commonSettingsPanel.add(repositoryNameField);
        commonSettingsPanel.add(new JLabel("  Type: "));
        commonSettingsPanel.add(typeCombobox);
        JPanel topPanel=dialog.getTopPanel();
        topPanel.removeAll();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(commonSettingsPanel);
        final String[] used=gui.getEngine().getDataRepositoryNames();
        VetoableChangeListener validator=new VetoableChangeListener() {
            @Override
            public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
                String name=repositoryNameField.getText();
                if (name==null || name.trim().isEmpty()) throw new PropertyVetoException("You must provide a name for the repository", null);
                name=name.trim();
                if (addNew) {
                    for (String inuse:used) {
                        if (name.equals(inuse)) throw new PropertyVetoException("A repository with this name already exists", null);
                    }
                }
                // check the rest of the parameters
                try {
                    String typeName=(String)typeCombobox.getSelectedItem();
                    Class repositoryClass=gui.getEngine().getDataRepositoryType(typeName);
                    DataRepository newRepository=(DataRepository)repositoryClass.newInstance();
                    dialog.getParameterPanel().setParameters(); // this will update the parameters in the panel
                    ParameterSettings settings=dialog.getParameterSettings();
                    newRepository.setConfigurationParameters(settings);
                } catch (Exception ex) {
                    throw new PropertyVetoException(ex.getMessage(),null);
                }
            }
        };
        dialog.setValidator(validator);
        if (addNew) { // add new repository
            typeCombobox.setSelectedIndex(0);
        } else { // edit configuration of existing repository
            repositoryNameField.setText(repository.getRepositoryName());
            repositoryNameField.setEditable(false);
            typeCombobox.setSelectedItem(repository.getRepositoryType());
            typeCombobox.setEditable(false);
            typeCombobox.setEnabled(false);
        }

        dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
        dialog.setVisible(true); // this will stay open until the values have been validated
        if (dialog.isOKPressed()) {
            if (addNew) {
                String name=repositoryNameField.getText(); name=name.trim();
                String typeName=(String)typeCombobox.getSelectedItem();
                Class repositoryClass=gui.getEngine().getDataRepositoryType(typeName);
                dialog.getParameterPanel().setParameters(); // this will update the parameters in the panel
                ParameterSettings settings=dialog.getParameterSettings();
                try {
                    DataRepository newRepository=(DataRepository)repositoryClass.newInstance();
                    newRepository.setRepositoryName(name);
                    newRepository.setConfigurationParameters(settings);
                    gui.getEngine().registerDataRepository(newRepository);
                    repositoryModel.addRow(new String[]{name,typeName,newRepository.getConfigurationString()});
                } catch (Exception ex) {gui.logMessage("An error occurred while adding data repository: "+ex.toString());}
            } else { // update existing repository
                 dialog.getParameterPanel().setParameters(); // this will update the parameters in the panel
                 ParameterSettings settings=dialog.getParameterSettings();
                 try {
                     repository.setConfigurationParameters(settings);
                     updateTable(repository);
                 } catch (Exception ex) {gui.logMessage("An error occurred while updating data repository: "+ex.toString());}
            }
        }
    }

    private void updateTable(DataRepository repository) {
        String name=repository.getRepositoryName();
        for (int i=0;i<repositoryTable.getRowCount();i++) {
            if (repositoryTable.getValueAt(i, COLUMN_NAME).equals(name)) {
                String config=repository.getConfigurationString();
                repositoryTable.setValueAt(config, i, COLUMN_DESCRIPTION);
                break;
            }
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton configureButton;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JButton infoButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton removeButton;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
