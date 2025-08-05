/*
 
 
 */

package org.motiflab.gui.operationdialog;


import org.motiflab.engine.data.analysis.Analysis;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.operations.Operation_analyze;
import org.motiflab.gui.InfoDialog;
import org.motiflab.gui.ParametersPanel;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_analyze extends OperationDialog {
    private JTextField targetDataTextfield=null; 
    private JPanel sourceTargetPanel=null;
    private JPanel analysisPanel;
    private JPanel additionalParametersPanel;
    private JComboBox analysisComboBox; 
    private ParametersPanel parametersPanel=null;
    private ParameterSettings parameterSettings=null;
    
    public OperationDialog_analyze(JFrame parent) {
        super(parent); 
        //initComponents();    
    }
    
    public OperationDialog_analyze() {
        super();
    }    
    
    public DefaultComboBoxModel getAnalysisNames() {
         String[] list=engine.getAnalysisNames();
         DefaultComboBoxModel newmodel=new DefaultComboBoxModel(list);
         return newmodel;
    } 
    
    
    
    @Override
    public void initComponents() {
        super.initComponents();  
        setResizable(false);
        setIconImage(gui.getFrame().getIconImage());        
        String analysisName=(String)parameters.getParameter(Operation_analyze.ANALYSIS);
        parameterSettings=(ParameterSettings)parameters.getParameter(Operation_analyze.PARAMETERS);   
        if (parameterSettings==null) {
            parameterSettings=new ParameterSettings();
            parameters.setParameter(Operation_analyze.PARAMETERS,parameterSettings);    
        }
        initAnalysisPanel(analysisName);
        initSourceTargetPanel();
        analysisPanel.setBorder(commonBorder);    
        sourceTargetPanel.setBorder(commonBorder);    
        additionalParametersPanel=new JPanel();
        additionalParametersPanel.setLayout(new FlowLayout(FlowLayout.LEADING));     
        additionalParametersPanel.setBorder(commonBorder);
        add(sourceTargetPanel);   
        add(analysisPanel);
        JScrollPane scrollPane=new JScrollPane(additionalParametersPanel); 
        scrollPane.setMaximumSize(new Dimension(500,400));                               
        add(scrollPane);
        add(getOKCancelButtonsPanel());
        Object analysisSelected=analysisComboBox.getSelectedItem();
        analysisComboBox.setSelectedItem(analysisSelected); // programatic 'click' to show the initial panel (if any applicable)
        //this.setMinimumSize(new Dimension(300,300));   
        pack();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim=super.getPreferredSize();
        //if (dim.width>510) dim.width=510; // enforce a maximum size on the dialog
        if (dim.height>500) dim.height=500; // enforce a maximum size on the dialog
        return dim;
    }
    
    
    

    @Override
    protected void setParameters() {
        String targetName=targetDataTextfield.getText();
        if (targetName!=null) targetName=targetName.trim();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName(Analysis.class, getDataTypeTable());
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);        
        if (analysisComboBox.getItemCount()==0) setOK(false); // if no algorithms exists the OK button will effectively just cancel the dialog
        else {
            String analysisName=(String)analysisComboBox.getSelectedItem();
            parameters.setParameter(Operation_analyze.ANALYSIS, analysisName);
            if (parametersPanel!=null) {
                parametersPanel.setParameters();
                parameters.setParameter(Operation_analyze.PARAMETERS, parametersPanel.getParameterSettings());
            }
            Analysis temp=engine.getAnalysis(analysisName);
            if (temp!=null) parameters.addAffectedDataObject(targetName, temp.getClass());
        } 
    }
    
    
     private void initAnalysisPanel(String algorithmName) {
        analysisPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        analysisPanel.add(new JLabel("Analysis  "));
        analysisComboBox=new JComboBox(getAnalysisNames());        
        if (algorithmName==null){
            if (analysisComboBox.getItemCount()>0) analysisComboBox.setSelectedIndex(0);
        }
        else analysisComboBox.setSelectedItem(algorithmName);
        analysisPanel.add(analysisComboBox);   
        analysisComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected=analysisComboBox.getSelectedItem();
                if (selected!=null) showParametersPanel(getAnalysisSettingsPanel((String)selected)); 
                else additionalParametersPanel.removeAll();               
                pack();
            }
        });
        JButton helpAnalysisButton=new JButton(informationIcon);
        if (informationIcon!=null) helpAnalysisButton.setIcon(informationIcon);
        else helpAnalysisButton.setText("Help");         
        helpAnalysisButton.setToolTipText("See HELP page for this analysis");
        helpAnalysisButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAnalysisHelp();
            }
        });   
        analysisPanel.add(helpAnalysisButton);
    }  
    

    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel sPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName(Analysis.class, getDataTypeTable());
        sPanel.add(new JLabel("Store results in  "));
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(16);
        sPanel.add(targetDataTextfield);  
        sourceTargetPanel.add(sPanel);
    }  
    
    /** */
    private void showParametersPanel(JPanel panel) {
        additionalParametersPanel.removeAll();
        additionalParametersPanel.add(panel);        
    }
   
    /** */
    private JPanel getAnalysisSettingsPanel(String analysisName) {
        Analysis analysis=engine.getAnalysis(analysisName);
        if (analysis==null) {
            parametersPanel=null;
            return new JPanel();
        }
        ParametersPanel panel=new ParametersPanel(analysis.getParameters(),parameterSettings, this);
        parametersPanel=panel;
        return panel;
    }
    
    
    private void showAnalysisHelp() {
        Object help=null;
        String error="Help unavailable";
        String analysisName=(String)analysisComboBox.getSelectedItem();
        Analysis analysis=engine.getAnalysis(analysisName);
        if (analysis!=null) {
            help=analysis.getHelp(engine);
            error="<h1>"+analysis.getAnalysisName()+"</h1><br>"+analysis.getDescription()+"<br><br>Detailed documentation for this analysis is currently unavailable.";                
        } else help="Unable to determine which analysis you refer to...";       
        InfoDialog infodialog=null;     
        if (help instanceof String || help == null) infodialog=new InfoDialog(gui, "Help for analysis: "+analysisName, (String)help, 700, 450);
        else if (help instanceof URL) infodialog=new InfoDialog(gui, "Help for analysis: "+analysisName, (URL)help, 700, 450, false);
        infodialog.setErrorMessage(error);
        if (infodialog!=null) infodialog.setVisible(true);
    }    

}
