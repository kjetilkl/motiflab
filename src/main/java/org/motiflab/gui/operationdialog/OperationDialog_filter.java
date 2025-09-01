/*
 
 
 */

package org.motiflab.gui.operationdialog;

import javax.swing.JFrame;
import javax.swing.JPanel;
import org.motiflab.engine.data.RegionDataset;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_filter extends FeatureTransformOperationDialog {
  
    
    public OperationDialog_filter(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_filter() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();                   
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_REGION);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }


    @Override
    protected void setParameters() {
        super.setParameters();
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, RegionDataset.class);
    }
}
