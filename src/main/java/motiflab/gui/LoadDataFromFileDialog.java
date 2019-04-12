/*
 * LoadDataFromFileDialog.java
 *
 * Created on 17. november 2009, 12:28
 */

package motiflab.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.*;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation;
import motiflab.engine.operations.Operation_new;

/**
 *
 * @author  kjetikl
 */
public class LoadDataFromFileDialog extends javax.swing.JDialog {
    private MotifLabGUI gui;
    private LoadFromFilePanel loadfromfilepanel;
    private String defaultname=null;
    private boolean userDefinedName=false; // true if the name of the data object has been typed by the user, false if it is a system generated default name
   
    
    /** Creates new form LoadDataFromFileDialog */
    public LoadDataFromFileDialog(MotifLabGUI pegui) {
        super(pegui.getFrame(),"Import data" ,true);
        this.gui=pegui;
        initComponents();

        String[] supporteddatatypes=new String[]{NumericDataset.getType(),RegionDataset.getType(),DNASequenceDataset.getType(),BackgroundModel.getType(),MotifCollection.getType(),MotifPartition.getType(),MotifTextMap.getType(),MotifNumericMap.getType(),ModuleCollection.getType(),ModulePartition.getType(),ModuleTextMap.getType(), ModuleNumericMap.getType(), SequenceCollection.getType(), SequencePartition.getType(),SequenceTextMap.getType(),SequenceNumericMap.getType(), ExpressionProfile.getType(), PriorsGenerator.getType(), TextVariable.getType(), OutputData.getType()}; // OutputData is included for testing       
        Arrays.sort(supporteddatatypes);
        DefaultComboBoxModel datatypemodel=new DefaultComboBoxModel(supporteddatatypes);
        datatypeCombobox.setModel(datatypemodel);
        loadfromfilepanel=new LoadFromFilePanel(new ArrayList<DataFormat>(), gui, null);
        loadfromfilepanelwrapper.add(loadfromfilepanel);
        datatypeCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newtype=(String)datatypeCombobox.getSelectedItem();
                ArrayList<DataFormat>supported=null;
                Class newtypeclass=gui.getEngine().getDataClassForTypeName(newtype);
                 if (newtypeclass!=null) {
                   supported=gui.getEngine().getDataInputFormats(newtypeclass);
                   updateDataName(newtypeclass);
                }
                if (!supportsPlainInputFormat(newtype)) supported.remove(gui.getEngine().getDataFormat("Plain"));
                DataFormat defaultformat=(newtypeclass!=null)?gui.getEngine().getDefaultDataFormat(newtypeclass):null;
                loadfromfilepanel.setDataFormats(supported,defaultformat);
            }
        });
        datatypeCombobox.setSelectedIndex(0);
        setMinimumSize(new Dimension(550, 600));
        //setPreferredSize(new Dimension(800, 700));
        getRootPane().setDefaultButton(okButton);
         
        datanameTextfield.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {propertyChange();}
            @Override public void removeUpdate(DocumentEvent e) {propertyChange();}
            @Override public void changedUpdate(DocumentEvent e) {propertyChange();}                                        
            private void propertyChange() {
                String newName=datanameTextfield.getText().trim();
                if (!defaultname.equals(newName)) userDefinedName=true;
            }
        });
    }

    private void updateDataName(Class type) {
        if (userDefinedName) return; // do not change user defined names
        defaultname=gui.getGenericDataitemName(type, null);    
        datanameTextfield.setText(defaultname);   
        userDefinedName=false;
    }

    private boolean supportsPlainInputFormat(String type) {
             if (type.equals(SequenceCollection.getType())) return true;
        else if (type.equals(SequencePartition.getType())) return true;
        else if (type.equals(SequenceNumericMap.getType())) return true;        
        else if (type.equals(SequenceTextMap.getType())) return true;
        else if (type.equals(MotifCollection.getType())) return true;
        else if (type.equals(MotifPartition.getType())) return true;        
        else if (type.equals(MotifTextMap.getType())) return true;        
        else if (type.equals(MotifNumericMap.getType())) return true;
        else if (type.equals(ModuleCollection.getType())) return true;
        else if (type.equals(ModuleTextMap.getType())) return true;
        else if (type.equals(ModuleNumericMap.getType())) return true;        
        else if (type.equals(ModulePartition.getType())) return true;
        else if (type.equals(TextVariable.getType())) return true;   
        else if (type.equals(OutputData.getType())) return true;  // just for testing!      
        else return false;
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

        buttonsPanel = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        topPanel = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        generalparameterspanel = new javax.swing.JPanel();
        internalpanel = new javax.swing.JPanel();
        datanamelabel = new javax.swing.JLabel();
        typelabel = new javax.swing.JLabel();
        datanameTextfield = new javax.swing.JTextField();
        datatypeCombobox = new javax.swing.JComboBox();
        loadfromfilepanelwrapper = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(100, 46));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(LoadDataFromFileDialog.class);
        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setMaximumSize(new java.awt.Dimension(72, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(72, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setOpaque(false);
        cancelButton.setPreferredSize(new java.awt.Dimension(72, 27));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonClicked(evt);
            }
        });

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMaximumSize(new java.awt.Dimension(72, 27));
        okButton.setMinimumSize(new java.awt.Dimension(72, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.setPreferredSize(new java.awt.Dimension(72, 27));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });

        javax.swing.GroupLayout buttonsPanelLayout = new javax.swing.GroupLayout(buttonsPanel);
        buttonsPanel.setLayout(buttonsPanelLayout);
        buttonsPanelLayout.setHorizontalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonsPanelLayout.createSequentialGroup()
                .addContainerGap(364, Short.MAX_VALUE)
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        buttonsPanelLayout.setVerticalGroup(
            buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(buttonsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        topPanel.setName("topPanel"); // NOI18N
        topPanel.setPreferredSize(new java.awt.Dimension(100, 5));

        javax.swing.GroupLayout topPanelLayout = new javax.swing.GroupLayout(topPanel);
        topPanel.setLayout(topPanelLayout);
        topPanelLayout.setHorizontalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 528, Short.MAX_VALUE)
        );
        topPanelLayout.setVerticalGroup(
            topPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 5, Short.MAX_VALUE)
        );

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        generalparameterspanel.setName("generalparameterspanel"); // NOI18N
        generalparameterspanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        internalpanel.setName("internalpanel"); // NOI18N
        internalpanel.setLayout(new java.awt.GridBagLayout());

        datanamelabel.setText(resourceMap.getString("datanamelabel.text")); // NOI18N
        datanamelabel.setName("datanamelabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(15, 10, 15, 0);
        internalpanel.add(datanamelabel, gridBagConstraints);

        typelabel.setText(resourceMap.getString("typelabel.text")); // NOI18N
        typelabel.setName("typelabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(15, 10, 15, 0);
        internalpanel.add(typelabel, gridBagConstraints);

        datanameTextfield.setColumns(22);
        datanameTextfield.setText(resourceMap.getString("datanameTextfield.text")); // NOI18N
        datanameTextfield.setName("datanameTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 0);
        internalpanel.add(datanameTextfield, gridBagConstraints);

        datatypeCombobox.setName("datatypeCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 0);
        internalpanel.add(datatypeCombobox, gridBagConstraints);

        generalparameterspanel.add(internalpanel);

        mainPanel.add(generalparameterspanel, java.awt.BorderLayout.PAGE_START);

        loadfromfilepanelwrapper.setName("loadfromfilepanelwrapper"); // NOI18N
        loadfromfilepanelwrapper.setLayout(new java.awt.BorderLayout());
        mainPanel.add(loadfromfilepanelwrapper, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void cancelButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonClicked
   this.setVisible(false);
}//GEN-LAST:event_cancelButtonClicked

private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
    String targetName=datanameTextfield.getText().trim();//GEN-LAST:event_okButtonPressed
    String nameError=gui.getEngine().checkNameValidity(targetName,false);
    if (nameError!=null) {
        JOptionPane.showMessageDialog(this, nameError+":\n"+targetName, "Illegal data name", JOptionPane.ERROR_MESSAGE);
        return;
    }
    String filename=loadfromfilepanel.getFilename();
    if (filename==null) {
        JOptionPane.showMessageDialog(this, "Filename missing", "Import error", JOptionPane.ERROR_MESSAGE);
        return;        
    }
    setVisible(false);    
    Operation operation=gui.getEngine().getOperation("new");      
    String datatype=(String)datatypeCombobox.getSelectedItem();
    OperationTask newTask=new OperationTask("new "+datatype);
    newTask.setParameter(OperationTask.OPERATION, operation);
    newTask.setParameter(OperationTask.OPERATION_NAME, operation.getName());
    newTask.setParameter(OperationTask.TARGET_NAME, targetName);
    newTask.setParameter(OperationTask.SOURCE_NAME, targetName);
    newTask.setParameter(Operation_new.DATA_TYPE, datatype);
    newTask.setParameter(Operation_new.PARAMETERS,Operation_new.FILE_PREFIX); // the FILE_PREFIX part of the parameter is necessary for proper recognition by Operation_new
    DataFormat dataformat=loadfromfilepanel.getDataFormat();
    ParameterSettings settings=loadfromfilepanel.getParameterSettings();
    newTask.setParameter(Operation_new.FILENAME,filename);
    newTask.setParameter(Operation_new.DATA_FORMAT,dataformat.getName());
    newTask.setParameter(Operation_new.DATA_FORMAT_SETTINGS,settings);
    Class dataclass=gui.getEngine().getDataClassForTypeName(datatype);
    newTask.addAffectedDataObject(targetName, dataclass);
    gui.launchOperationTask(newTask, gui.isRecording());
}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField datanameTextfield;
    private javax.swing.JLabel datanamelabel;
    private javax.swing.JComboBox datatypeCombobox;
    private javax.swing.JPanel generalparameterspanel;
    private javax.swing.JPanel internalpanel;
    private javax.swing.JPanel loadfromfilepanelwrapper;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel topPanel;
    private javax.swing.JLabel typelabel;
    // End of variables declaration//GEN-END:variables

}
