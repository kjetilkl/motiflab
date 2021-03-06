/*
 * ExtendSequencesDialog.java
 *
 * Created on 25 october 2012, 17:10:21
 */
package motiflab.gui;

import motiflab.engine.task.ExtendSequencesTask;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.SequenceNumericMap;

/**
 *
 * @author kjetikl
 */
public class ExtendSequencesDialog extends javax.swing.JDialog {
    MotifLabGUI gui;
    
    /** Creates new form CropSequencesDialog */
    public ExtendSequencesDialog(MotifLabGUI gui) {
        super(gui.getFrame(),"Extend Sequences", true);
        this.gui=gui;
        initComponents();
        progressbar.setVisible(false);
        //this.setMinimumSize(new Dimension(580,186));
//        this.setPreferredSize(new Dimension(580,170));        
        setResizable(false);
        getRootPane().setDefaultButton(okButton);
        if (gui.getEngine().getDefaultSequenceCollection().isEmpty() || !gui.getScheduler().isIdle()) okButton.setEnabled(false);
        orientationCombobox.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                cropBasesFocusGained(evt);
            }
        });
        startCombobox.setModel(getExtendComboboxModel());
        endCombobox.setModel(getExtendComboboxModel());  
        pack();        
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        jPanel6 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        extendRadioButton = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        startCombobox = new javax.swing.JComboBox();
        endCombobox = new javax.swing.JComboBox();
        orientationCombobox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        jPanel1.setMinimumSize(new java.awt.Dimension(100, 10));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 10));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 882, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 10, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_START);

        jPanel2.setMinimumSize(new java.awt.Dimension(100, 30));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setPreferredSize(new java.awt.Dimension(400, 40));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 15, 5));

        progressbar.setName("progressbar"); // NOI18N
        jPanel7.add(progressbar);

        jPanel2.add(jPanel7, java.awt.BorderLayout.CENTER);

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 5));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ExtendSequencesDialog.class);
        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });
        jPanel6.add(okButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonPressed(evt);
            }
        });
        jPanel6.add(cancelButton);

        jPanel2.add(jPanel6, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_END);

        jPanel3.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8))));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.GridBagLayout());

        buttonGroup1.add(extendRadioButton);
        extendRadioButton.setSelected(true);
        extendRadioButton.setText(resourceMap.getString("extendRadioButton.text")); // NOI18N
        extendRadioButton.setName("extendRadioButton"); // NOI18N
        jPanel3.add(extendRadioButton, new java.awt.GridBagConstraints());

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 11);
        jPanel3.add(jLabel1, gridBagConstraints);

        startCombobox.setEditable(true);
        startCombobox.setName("startCombobox"); // NOI18N
        startCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cropBasesChangedHandler(evt);
            }
        });
        jPanel3.add(startCombobox, new java.awt.GridBagConstraints());

        endCombobox.setEditable(true);
        endCombobox.setName("endCombobox"); // NOI18N
        endCombobox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cropBasesChangedHandler(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        jPanel3.add(endCombobox, gridBagConstraints);

        orientationCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Relative orientation", "Direct orientation" }));
        orientationCombobox.setName("orientationCombobox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 17, 0, 0);
        jPanel3.add(orientationCombobox, gridBagConstraints);

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel3.add(jLabel2, gridBagConstraints);

        getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
        setVisible(false);
    }//GEN-LAST:event_cancelButtonPressed

    private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
        okButton.setEnabled(false);
        ExtendSequencesTask extendtask=null;
        Object extendStart=null;
        Object extendEnd=null;
        String error=null;
        String startString=((String)startCombobox.getSelectedItem()).trim();
        try {
           int value=Integer.parseInt(startString); 
           extendStart=new Integer(value);
        } catch (NumberFormatException e) {
            Object data=gui.getEngine().getDataItem(startString);
            if (data instanceof SequenceNumericMap || data instanceof NumericVariable) extendStart=data;
            else error="'"+startString+"' is not a valid number, Numeric Variable or Sequence Numeric Map";
        }
        String endString=((String)endCombobox.getSelectedItem()).trim();
        try {
           int value=Integer.parseInt(endString); 
           extendEnd=new Integer(value);
        } catch (NumberFormatException e) {
            Object data=gui.getEngine().getDataItem(endString);
            if (data instanceof SequenceNumericMap || data instanceof NumericVariable) extendEnd=data;
            else error="'"+endString+"' is not a valid number, Numeric Variable or Sequence Numeric Map";
        }            
        if (error!=null) {
            JOptionPane.showMessageDialog(this, error, "Range error", JOptionPane.ERROR_MESSAGE);
        } else {
            String orientation=(String)orientationCombobox.getSelectedItem(); 
            boolean useRelativeOrientation=orientation.equals("Relative orientation");
            extendtask=new ExtendSequencesTask(gui.getEngine(), extendStart, extendEnd, useRelativeOrientation);
        }
        if (extendtask==null) {
           okButton.setEnabled(true);
           return;
        } else {
            ExtendSequencesDialog.this.setVisible(false);
            gui.launchUndoableTask(extendtask);
        }        
    }//GEN-LAST:event_okButtonPressed

    private void cropBasesChangedHandler(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cropBasesChangedHandler
        extendRadioButton.setSelected(true);
    }//GEN-LAST:event_cropBasesChangedHandler

    private void cropBasesFocusGained(java.awt.event.FocusEvent evt) {
        extendRadioButton.setSelected(true);
    }
    
    private ComboBoxModel getExtendComboboxModel() {
        ArrayList<String> datanames=gui.getEngine().getNamesForAllDataItemsOfTypes(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        Collections.sort(datanames);
        datanames.add(0, "0");
        String[] names=new String[datanames.size()];
        names=datanames.toArray(names);
        DefaultComboBoxModel model=new DefaultComboBoxModel(names);
        return model;
    }
      
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox endCombobox;
    private javax.swing.JRadioButton extendRadioButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox orientationCombobox;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JComboBox startCombobox;
    // End of variables declaration//GEN-END:variables
}
