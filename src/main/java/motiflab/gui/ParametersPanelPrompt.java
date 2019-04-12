/*
 * ParametersPanelPrompt.java
 *
 * Created on Sep 12, 2014, 11:48:33 AM
 */
package motiflab.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;

/**
 * This dialog class functions as a wrapper around a ParameterPanel
 * It is used by e.g. promptForValues() interface method in graphical MotifLab clients
 * to allow the user to specify a set of values.
 * @author kjetikl
 */
public class ParametersPanelPrompt extends javax.swing.JDialog {
    private boolean isOK=false;
    private ParametersPanel parametersPanel;
    MotifLabEngine engine;
    VetoableChangeListener validator=null;
    
    /** Creates new form ParametersPanelPrompt */
    public ParametersPanelPrompt(Frame owner, MotifLabEngine engine, String message, String title, Parameter[] parameters, ParameterSettings settings) {
        super(owner, title, true);
        this.engine=engine;
        initComponents();
        if (message!=null) {
            boolean isError=message.startsWith("[ERROR]");
            if (isError) message=message.substring("[ERROR]".length());
            if (message.contains("\n")) {
                message=message.replace("\n", "<br>");
            }
            if (isError) message="<html><font color=\"red\"><b>"+message+"</b></font></html>";
            else message="<html><b>"+message+"</b></html>";            
            messageLabel.setText(message);
            messageLabel.setIcon((isError)?new MiscIcons(MiscIcons.ERROR_ICON):null);
        }
        parametersPanel=new ParametersPanel(parameters, settings, engine);
        scrollPane.setViewportView(parametersPanel);
        getRootPane().setDefaultButton(okButton);
        this.setMaximumSize(new Dimension(1000,700));
        pack();
    }
    
    /** If this method is called, the panel will be configured so that pressing ENTER while the focus is on */
    public void enableDefaultOKButton() {
        parametersPanel.addExternalActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() instanceof JTextComponent) okButton.doClick();
            }
        });
    }

    /** Sets a validator for this prompt
     *  The object of the validator will be to check the parameter values before the dialog is closed.
     *  If a validator is specified, the vetoableChange() method of the validator will be called after the user has pressed the OK button (with NULL argument).
     *  The validator should then check the parameters and throw a PropertyVetoException if it has any objections.
     *  If an exception is thrown, the exception message will be displayed to the user and the dialog will be kept open.
     *  If not, the dialog will be closed as usual.
     * 
     * 
     */
    public void setValidator(VetoableChangeListener validator) {
        this.validator=validator;
    }
    
    /** Returns the inner JPanel which encloses the JScrollPane that contains the ParametersPanel as a view */
    public JPanel getInnerPanel() {
        return innerPanel;
    }
    
    /** Returns the panel which is displayed above the parameters panel. You can use this to add additional components to the panel */
    public JPanel getTopPanel() {
        return topPanel;
    }    
    
    public ParametersPanel getParameterPanel() {
        return parametersPanel;
    }
    
    /** Replaces the current parameters with a new set. The dialog might have to be revalidated/repacked afterwards */
    public void updateParameters(Parameter[] parameters, ParameterSettings settings) {
        parametersPanel=new ParametersPanel(parameters, settings, engine);
        scrollPane.setViewportView(parametersPanel);
    }    
    
    /** Returns the parameter values from dialog. This should be called after the dialog is closed to retrieve the new parameter values.
     *  If the user pressed the OK button, the values will be updated based on the new selections in the GUI widgets.
     *  If the user pressed the CLOSE button, the values will be the ones provided as defaults when the dialog was created.
     */
    public ParameterSettings getParameterSettings() {        
        if (isOK) parametersPanel.setParameters(); // updates the internal settings based on the widget values if OK was pressed. If cancel was pressed, return the default values.
        return parametersPanel.getParameterSettings();
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
        messageLabel = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        innerPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        buttonsPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));
        topPanel.setName("topPanel"); // NOI18N
        topPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ParametersPanelPrompt.class);
        messageLabel.setText(resourceMap.getString("messageLabel.text")); // NOI18N
        messageLabel.setName("messageLabel"); // NOI18N
        topPanel.add(messageLabel);

        getContentPane().add(topPanel, java.awt.BorderLayout.NORTH);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 6, 1, 6));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        innerPanel.setName("innerPanel"); // NOI18N
        innerPanel.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        innerPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        mainPanel.add(innerPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(350, 37));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setName("jPanel1"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 267, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 33, Short.MAX_VALUE)
        );

        buttonsPanel.add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okPressed(evt);
            }
        });
        jPanel2.add(okButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelPressed(evt);
            }
        });
        jPanel2.add(cancelButton);

        buttonsPanel.add(jPanel2, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okPressed
        if (validator!=null) { // allow the validator to check the new values and make objections before we close the dialog
            try {
                validator.vetoableChange(null);
            } catch (PropertyVetoException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        isOK=true;
        setVisible(false);
    }//GEN-LAST:event_okPressed

    private void cancelPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelPressed
        isOK=false;
        setVisible(false);
    }//GEN-LAST:event_cancelPressed

    public boolean isOKPressed() {
        return isOK;
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel innerPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
}
