/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Operation_merge;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_merge extends FeatureTransformOperationDialog {
    private JPanel parametersPanel;
    private JComboBox distanceCombobox;  
    private JComboBox similarComboBox;  
    
    public OperationDialog_merge(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_merge() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();                   
        parametersPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        parametersPanel.setBorder(commonBorder);
        parametersPanel.add(new JLabel("Merge "));
        similarComboBox=new JComboBox(new String[]{"any","similar"});
        String similarString=(String)parameters.getParameter(Operation_merge.SIMILAR);
        if (similarString==null) similarString="any";
        similarComboBox.setSelectedItem(similarString);
        parametersPanel.add(similarComboBox);
        String distanceString=(String)parameters.getParameter(Operation_merge.DISTANCE_STRING);
        if (distanceString==null) distanceString="0";
        DefaultComboBoxModel wbmodel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        distanceCombobox = new JComboBox(wbmodel);
        distanceCombobox.setEditable(true);
        distanceCombobox.setSelectedItem(distanceString);
        parametersPanel.add(new JLabel(" closer than "));
        parametersPanel.add(distanceCombobox);     
        
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_REGION);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(parametersPanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        parameters.setParameter(Operation_merge.DISTANCE_STRING, (String)distanceCombobox.getSelectedItem());
        if (((String)similarComboBox.getSelectedItem()).equals("similar")) parameters.setParameter(Operation_merge.SIMILAR, "similar");
        else parameters.setParameter(Operation_merge.SIMILAR, null);
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, RegionDataset.class);
    }    
    
}
