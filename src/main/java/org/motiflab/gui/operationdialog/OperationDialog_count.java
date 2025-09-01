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
import org.motiflab.engine.operations.Operation_count;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_count extends FeatureTransformOperationDialog {
    private JPanel windowpanel;
    private JComboBox countPropertyCombobox;
    private JComboBox overlappingCombobox;
    private JComboBox windowSizeCombobox;
    private JComboBox windowAnchorCombobox;

    
    public OperationDialog_count(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_count() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        targetDataTextfield.setText(gui.getGenericDataitemName("countTrack",getDataTypeTable()));
        windowpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        windowpanel.setBorder(commonBorder);
        windowpanel.add(new JLabel("Count  "));
        DefaultComboBoxModel<String> comboboxModel=new DefaultComboBoxModel<>(new String[]{Operation_count.REGION_NUMBER_COUNT,Operation_count.REGION_SCORES_COUNT,Operation_count.REGION_SCORES_IC_CONTENT});
        countPropertyCombobox = new JComboBox(comboboxModel);
        countPropertyCombobox.setEditable(true);
        String countPropertyString=(String)parameters.getParameter(Operation_count.COUNT_PROPERTY);
        if (countPropertyString==null) countPropertyString=(String)countPropertyCombobox.getModel().getElementAt(0);
        countPropertyCombobox.setSelectedItem(countPropertyString);
        windowpanel.add(countPropertyCombobox);                   
        overlappingCombobox = new JComboBox(new String[]{"overlapping","within"});
        String overlappingString=(String)parameters.getParameter(Operation_count.OVERLAPPING_OR_WITHIN);
        if (overlappingString==null) overlappingString=(String)overlappingCombobox.getModel().getElementAt(0);
        overlappingCombobox.setSelectedItem(overlappingString);
        windowpanel.add(overlappingCombobox);                   
        windowpanel.add(new JLabel(" window of size "));
        DefaultComboBoxModel wbmodel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        windowSizeCombobox = new JComboBox(wbmodel);
        windowSizeCombobox.setEditable(true);
        windowpanel.add(windowSizeCombobox);                   
        String windowSizeString=(String)parameters.getParameter(Operation_count.WINDOW_SIZE);
        if (windowSizeString==null) windowSizeString="5";
        windowSizeCombobox.setSelectedItem(windowSizeString);
        windowAnchorCombobox=new JComboBox(new String[]{Operation_count.CENTER,Operation_count.UPSTREAM,Operation_count.DOWNSTREAM});
        String windowAnchor=(String)parameters.getParameter(Operation_count.ANCHOR);
        if (windowAnchor==null || windowAnchor.isEmpty()) windowAnchor=(String)Operation_count.CENTER;
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
        parameters.setParameter(Operation_count.COUNT_PROPERTY, (String)countPropertyCombobox.getSelectedItem());
        parameters.setParameter(Operation_count.OVERLAPPING_OR_WITHIN, (String)overlappingCombobox.getSelectedItem());
        parameters.setParameter(Operation_count.WINDOW_SIZE, (String)windowSizeCombobox.getSelectedItem());
        parameters.setParameter(Operation_count.ANCHOR, (String)windowAnchorCombobox.getSelectedItem());
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
