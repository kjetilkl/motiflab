/*
 */
package motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import motiflab.engine.operations.Operation_split_sequences;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_split_sequences extends FeatureTransformOperationDialog { // the extension is a convenience to inherit the source, target and subset widgets
    
    private JCheckBox deleteSequencesCheckbox;
    
    @Override
    public void initComponents() {
        super.initComponents();                   
        JPanel sourceTargetPanel=getSourceTargetPanel();
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("splitSequencesPartition", getDataTypeTable());        
        targetDataTextfield.setText(targetName);        
        sourceTargetPanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        JPanel otherParametersPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        Boolean delete=(Boolean)parameters.getParameter(Operation_split_sequences.DELETE_ORIGINAL_SEQUENCES);
        if (delete==null) delete=Boolean.FALSE;
        deleteSequencesCheckbox=new JCheckBox("Delete original sequences", delete);
        otherParametersPanel.add(deleteSequencesCheckbox);
        add(sourceTargetPanel);
        add(otherParametersPanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }


    @Override
    protected void setParameters() {
        super.setParameters();
        boolean delete=deleteSequencesCheckbox.isSelected();
        parameters.setParameter(Operation_split_sequences.DELETE_ORIGINAL_SEQUENCES, delete);
        parameters.setBlockGUI(true);
        parameters.setTurnOffGUInotifications(true);        
    }    
    
    
}
