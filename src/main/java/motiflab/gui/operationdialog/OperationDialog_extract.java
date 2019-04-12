/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import motiflab.engine.data.*;
import motiflab.engine.data.analysis.Analysis;
import motiflab.engine.operations.Operation_extract;
import motiflab.engine.operations.Operation_new;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_extract extends OperationDialog {
    private JPanel resultVariableNamePanel;
    private JComboBox resultVariableCombobox;
    private JComboBox resultVariableTypeCombobox;
    private Data sourceData=null;
    
    public OperationDialog_extract(JFrame parent) {
        super(parent); 
        initComponents();        
    }
    
    public OperationDialog_extract() {
        super();
    }    
        
    
    @Override
    public void initComponents() {
        super.initComponents();
        String targetName=(String)parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetDataTextfield.setText(gui.getGenericDataitemName(Data.class, getDataTypeTable()));
        ActionListener[] list=sourceDataCombobox.getActionListeners();
        for (ActionListener listener:list) sourceDataCombobox.removeActionListener(listener);
        sourceDataCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sourceName=(String)sourceDataCombobox.getSelectedItem();
                sourceData=engine.getDataItem(sourceName);
                if (sourceData==null) {
                    resultVariableCombobox.setModel(new DefaultComboBoxModel(new String[0]));
                } else {
                    String[] resultVariables=sourceData.getResultVariables();
                    Arrays.sort(resultVariables);
                    resultVariableCombobox.setModel(new DefaultComboBoxModel(resultVariables));
                    if (resultVariableCombobox.getItemCount()>0) resultVariableCombobox.setSelectedIndex(0);
                }
            }
        });
        String[] allowedTypes=Operation_new.getAvailableTypes(); 
        String[] analysisNames=engine.getAnalysisNames();
        String[] allTypes=new String[allowedTypes.length+analysisNames.length];
        System.arraycopy(allowedTypes, 0, allTypes, 0, allowedTypes.length);
        for (int i=0;i<analysisNames.length;i++) {allTypes[allowedTypes.length+i]="Analysis: "+analysisNames[i];}
        Arrays.sort(allTypes);
        resultVariableTypeCombobox = new JComboBox(allTypes);
        resultVariableTypeCombobox.setEditable(false);

        resultVariableCombobox = new JComboBox();
        resultVariableCombobox.setEditable(true);
        resultVariableCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fieldName=(String)resultVariableCombobox.getSelectedItem();
                String sourceName=(String)sourceDataCombobox.getSelectedItem();
                sourceData=engine.getDataItem(sourceName);
                if (sourceData!=null) {
                    Class datatypeclass=sourceData.getResultType(fieldName);
                    String typeclassname=getProperTypeName(datatypeclass);
                    if (!typeclassname.toLowerCase().startsWith("unknown")) resultVariableTypeCombobox.setSelectedItem(typeclassname);
                }
            }
        });

        resultVariableNamePanel=new JPanel();
        resultVariableNamePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        resultVariableNamePanel.setBorder(commonBorder);
        resultVariableNamePanel.add(new JLabel("Extract data   "));
        resultVariableNamePanel.add(resultVariableCombobox);
        resultVariableNamePanel.add(new JLabel("  as  "));
        resultVariableNamePanel.add(resultVariableTypeCombobox);
        add(getSourceTargetPanel());   
        add(resultVariableNamePanel);
        add(getOKCancelButtonsPanel());
        String sourceName=parameters.getSourceDataName();
        if (sourceName!=null && !sourceName.isEmpty()) sourceDataCombobox.setSelectedItem(sourceName);
        else if (sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedIndex(0);
        String variableName=(String)parameters.getParameter(Operation_extract.RESULT_VARIABLE_NAME);
        if (variableName!=null && !variableName.isEmpty()) resultVariableCombobox.setSelectedItem(variableName);
        else if (resultVariableCombobox.getItemCount()>0) resultVariableCombobox.setSelectedIndex(0);
        pack();
    }
    
    private String getProperTypeName(Class datatypeclass) {
        String typeclassname=engine.getTypeNameForDataClass(datatypeclass);
        if (typeclassname.equals(Analysis.getType())) {
            // determine subtype class
            Analysis analysis=engine.getAnalysisForClass(datatypeclass);
            return analysis.getTypeDescription(); // this should return "Analysis: <analysis type name>"
        } else return typeclassname;
    }

    @Override
    protected void setParameters() {
        super.setParameters();
        String resultVariableName=(String)resultVariableCombobox.getSelectedItem();
        if (resultVariableName==null || resultVariableName.isEmpty()) resultVariableName="N/A";
        parameters.setParameter(Operation_extract.RESULT_VARIABLE_NAME, resultVariableName);
        String resultVariableType=(String)resultVariableTypeCombobox.getSelectedItem();
        Class datatypeclass=engine.getDataClassForTypeName(resultVariableType);
        parameters.setParameter(Operation_extract.RESULT_VARIABLE_TYPE, datatypeclass);
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, datatypeclass);
    }
    
    
  
   
}

