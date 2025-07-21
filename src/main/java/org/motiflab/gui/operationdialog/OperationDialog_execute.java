/*
 
 
 */

package org.motiflab.gui.operationdialog;


import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.operations.Operation_execute;
import org.motiflab.external.ExternalProgram;
import org.motiflab.gui.ParametersPanel;
import org.motiflab.engine.protocol.DataTypeTable;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.gui.InfoDialog;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_execute extends OperationDialog {
    private JTextField targetDataTextfield=null; // hides field in superclass
    private JPanel sourceTargetPanel=null;
    private JPanel sequenceSubsetPanel;
    private JPanel algorithmPanel;
    private JPanel additionalParametersPanel;
    private JComboBox sequenceSubsetCombobox;
    private JComboBox algorithmComboBox; 
    private ParametersPanel parametersPanel=null;
    private ParameterSettings parameterSettings=null;
    private String[] initialEditValues=new String[2]; // if set to other than NULL, this array contains the original algorithm and trackname for a protocol line that is being edited
    private JLabel storeResultsInLabel;
    private String STORE_RESULTS_TEXT="Store results in";
    
    public OperationDialog_execute(JFrame parent) {
        super(parent);   
    }
    
    public OperationDialog_execute() {
        super();
    }    
    
    public DefaultComboBoxModel getExternalAlgorithms() {
         String[] list=engine.getOtherExternalPrograms();
         DefaultComboBoxModel newmodel=new DefaultComboBoxModel(list);
         return newmodel;
    } 
    
    
    
    @Override
    public void initComponents() {
        super.initComponents();  
        setResizable(false);
        setIconImage(gui.getFrame().getIconImage());
        String algorithmName=(String)parameters.getParameter(Operation_execute.ALGORITHM);
        parameterSettings=(ParameterSettings)parameters.getParameter(Operation_execute.PARAMETERS);   
        if (parameterSettings==null) {
            parameterSettings=new ParameterSettings();
            parameters.setParameter(Operation_execute.PARAMETERS,parameterSettings);    
        }
        initAlgorithmPanel(algorithmName);
        initSourceTargetPanel();
        initSequenceSubsetPanel();
        algorithmPanel.setBorder(commonBorder);    
        sourceTargetPanel.setBorder(commonBorder);    
        sequenceSubsetPanel.setBorder(commonBorder);
        additionalParametersPanel=new JPanel();
        additionalParametersPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        additionalParametersPanel.setBorder(commonBorder);
        add(sourceTargetPanel);   
        add(algorithmPanel);
        JScrollPane scrollPane=new JScrollPane(additionalParametersPanel);  
        scrollPane.setMaximumSize(new Dimension(500,420));
        add(scrollPane);
        add(sequenceSubsetPanel);
        add(getOKCancelButtonsPanel());
        Object formatSelected=algorithmComboBox.getSelectedItem();
        algorithmComboBox.setSelectedItem(formatSelected); // programatic 'click' to show the initial panel (if any applicable)
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
        if (algorithmComboBox.getItemCount()==0) setOK(false); // if no algorithms exists the OK button will effectively just cancel the dialog
        else {
            String algorithmName=(String)algorithmComboBox.getSelectedItem();
            parameters.setParameter(Operation_execute.ALGORITHM, algorithmName);
            if (parametersPanel!=null) {
                parametersPanel.setParameters();
                parameters.setParameter(Operation_execute.PARAMETERS, parametersPanel.getParameterSettings());
            }
            Class[] resultTypes=getDataTypesForAlgorithmResultParameters(algorithmName);
            String targetName=targetDataTextfield.getText();
            if (targetName!=null) targetName=targetName.trim();
            if (targetName==null || targetName.isEmpty()) targetName=getDefaultTargetNames(algorithmName);
            String[] temptargets=targetName.split("\\s*,\\s*");
            String[] targets; // this will hold individual names of target data objects
            if (resultTypes.length>temptargets.length) { // missing target names: add generic names (note that at least one name has been specified here)
                targets=new String[resultTypes.length];
                System.arraycopy(temptargets, 0, targets, 0, temptargets.length);
                DataTypeTable typetable=getDataTypeTable();
                for (int i=temptargets.length;i<resultTypes.length;i++) {
                    String name=gui.getGenericDataitemName(resultTypes[i], typetable);
                    try {typetable.register(name, resultTypes[i]);} catch (ParseError pe) {} // update typetable. It is OK since we are not going to use it for anything else anymore
                    targets[i]=name;
                }
            } else if (resultTypes.length<temptargets.length) { // too many names: cut a few
                targets=new String[resultTypes.length];
                System.arraycopy(temptargets, 0, targets, 0, targets.length);
                targetName=MotifLabEngine.splice(targets,",");
            } else {
                targets=temptargets;
            }
            parameters.setParameter(OperationTask.TARGET_NAME, targetName);
            String sequenceCollection=(String)sequenceSubsetCombobox.getSelectedItem();
            if (!sequenceSubsetCombobox.isEnabled() || sequenceCollection==null || sequenceCollection.equals(engine.getDefaultSequenceCollectionName())) sequenceCollection=null;
            parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
            for (int i=0;i<targets.length;i++) {
                parameters.addAffectedDataObject(targets[i], resultTypes[i]);
            }
        } 
    }
    
    private void initSequenceSubsetPanel() {
        sequenceSubsetPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        sequenceSubsetPanel.add(new JLabel("In sequence collection  "));
        String selectedName=(String)parameters.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (selectedName==null) selectedName=engine.getDefaultSequenceCollectionName();
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(new Class[]{SequenceCollection.class});
        sequenceSubsetCombobox=new JComboBox(model);
        sequenceSubsetCombobox.setSelectedItem(selectedName);
        sequenceSubsetPanel.add(sequenceSubsetCombobox);                
    } 
    
     private void initAlgorithmPanel(String algorithmName) {
        algorithmPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        algorithmPanel.add(new JLabel("Program  "));
        algorithmComboBox=new JComboBox(getExternalAlgorithms());        
        if (algorithmName==null){
            if (algorithmComboBox.getItemCount()>0) algorithmComboBox.setSelectedIndex(0);
        }
        else algorithmComboBox.setSelectedItem(algorithmName);
        algorithmPanel.add(algorithmComboBox);   
        algorithmComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selected=(String)algorithmComboBox.getSelectedItem();
                if (selected!=null) {
                    if (initialEditValues[0]!=null && (selected).equals(initialEditValues[0])) {
                       targetDataTextfield.setText(initialEditValues[1]);
                    } else {
                        targetDataTextfield.setText(getDefaultTargetNames(selected));
                    }
                    showParametersPanel(getAlgorithmSettingsPanel(selected));
                    int resultsCount=getNumberOfResultsParametersForAlgorithm(selected);
                    storeResultsInLabel.setText(STORE_RESULTS_TEXT+" ("+resultsCount+")  ");
                    boolean showSubsetPanel=appliesToSequenceCollections(selected);
                    sequenceSubsetPanel.setVisible(showSubsetPanel);
                }
                else {
                    additionalParametersPanel.removeAll();
                    sequenceSubsetPanel.setVisible(false);
                }
                pack();
            }
        });
        JButton algorithmHelpButton=new JButton(this.informationIcon);
        algorithmHelpButton.setToolTipText("Show description of algorithm");
        algorithmPanel.add(algorithmHelpButton);
        algorithmHelpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedAlgorithm=(String)algorithmComboBox.getSelectedItem();
                if (selectedAlgorithm==null || selectedAlgorithm.isEmpty()) return;
                ExternalProgram externalprogram=engine.getExternalProgram(selectedAlgorithm);
                if (externalprogram==null) return;
                gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                String document=externalprogram.getProgramDocumentation();
                InfoDialog dialog=new InfoDialog(gui, "External Program", document);
                gui.getFrame().setCursor(Cursor.getDefaultCursor());
                dialog.setVisible(true);
                dialog.dispose();
            }
        });      
    }  
    

    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel sPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        String targetName=parameters.getTargetDataName();
        initialEditValues[0]=(String)parameters.getParameter(Operation_execute.ALGORITHM);
        initialEditValues[1]=targetName;
        if (targetName==null || targetName.isEmpty()) targetName=engine.getNextAvailableDataName("Data", 0);
        storeResultsInLabel=new JLabel(STORE_RESULTS_TEXT); // number will be changed later
        sPanel.add(storeResultsInLabel);
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
    private JPanel getAlgorithmSettingsPanel(String algorithmName) {
        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        if (algorithm==null) {
            parametersPanel=null;
            return new JPanel();
        }
        ParametersPanel panel=new ParametersPanel(algorithm.getParameters(),parameterSettings, this);
        parametersPanel=panel;
        return panel;
    }

    /** Returns a comma separated list of suggested datatarget names
     *  The number of names in this list will depend on the number of data items
     *  returned by the algorithm, and the names will be generic names based
     *  on the class-types of these data items
     */
    private String getDefaultTargetNames(String algorithmName) {
//        Class[] types=getDataTypesForAlgorithmResultParameters(algorithmName);
//        String[] names=gui.getGenericDataitemNames(types,getDataTypeTable());  // old names were based on the class. New names on the parameter names      
        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        int cardinality=algorithm.getNumberOfResultParameters();
        String[] names=new String[cardinality];
        for (int i=0;i<names.length;i++) {
            String paramname=algorithm.getNameForResultParameter(i);
            paramname=MotifLabEngine.convertToLegalDataName(paramname);
            names[i]=gui.getGenericDataitemName(paramname, getDataTypeTable());
        }
        return MotifLabEngine.splice(names,",");
    }

    /** Returns a list of class types for the data items returned by the algorithm
     *  The size of this list will be algorithm-dependent
     */
    private Class[] getDataTypesForAlgorithmResultParameters(String algorithmName) {
        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        int cardinality=algorithm.getNumberOfResultParameters();
        Class[] datatypes=new Class[cardinality];
        for (int i=0;i<cardinality;i++) {
            datatypes[i]=algorithm.getTypeForResultParameter(i);
        }
        return datatypes;
    }
    
    private int getNumberOfResultsParametersForAlgorithm(String algorithmName) {
        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        int cardinality=algorithm.getNumberOfResultParameters();
        return cardinality;
    }    
    
    private boolean appliesToSequenceCollections(String algorithmName) {
        ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
        return algorithm.isApplicableToSequenceCollections();
    }       

}
