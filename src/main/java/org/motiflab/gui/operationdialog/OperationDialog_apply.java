/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.operations.Operation_apply;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_apply extends FeatureTransformOperationDialog {
    private JPanel windowpanel;
    private JComboBox windowTypeCombobox;
    private JComboBox windowSizeCombobox;
    private JComboBox windowAnchorCombobox;

    
    public OperationDialog_apply(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_apply() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        windowpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        windowpanel.setBorder(commonBorder);
        windowpanel.add(new JLabel("Apply window   "));
        windowTypeCombobox = new JComboBox(Operation_apply.getWindowTypes());
        String windowTypeString=(String)parameters.getParameter(Operation_apply.WINDOW_TYPE);
        if (windowTypeString==null) windowTypeString=(String)windowTypeCombobox.getModel().getElementAt(0);
        windowTypeCombobox.setSelectedItem(windowTypeString);
        windowpanel.add(windowTypeCombobox);                   
        windowpanel.add(new JLabel("    size  "));
        DefaultComboBoxModel wbmodel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        windowSizeCombobox = new JComboBox(wbmodel);
        windowSizeCombobox.setEditable(true);
        windowpanel.add(windowSizeCombobox);                   
        String windowSizeString=(String)parameters.getParameter(Operation_apply.WINDOW_SIZE);
        if (windowSizeString==null) windowSizeString="5";
        windowSizeCombobox.setSelectedItem(windowSizeString);
        windowAnchorCombobox=new JComboBox(Operation_apply.getWindowAnchors());
        String windowAnchor=(String)parameters.getParameter(Operation_apply.ANCHOR);
        if (windowAnchor==null || windowAnchor.isEmpty()) windowAnchor=Operation_apply.getWindowAnchors()[0];
        windowAnchorCombobox.setSelectedItem(windowAnchor);
        windowpanel.add(new JLabel("    anchor  "));
        windowpanel.add(windowAnchorCombobox);
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(windowpanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        parameters.setParameter(Operation_apply.WINDOW_TYPE, (String)windowTypeCombobox.getSelectedItem());
        parameters.setParameter(Operation_apply.WINDOW_SIZE, (String)windowSizeCombobox.getSelectedItem());
        parameters.setParameter(Operation_apply.ANCHOR, (String)windowAnchorCombobox.getSelectedItem());
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
