/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.operations.Operation_interpolate;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_interpolate extends FeatureTransformOperationDialog {
    private JPanel methodpanel;
    private JComboBox methodCombobox;
    private JCheckBox usePeriodCheckbox;
    private JComboBox periodCombobox;
    private JComboBox maxDistanceCombobox;    
    
    
    public OperationDialog_interpolate(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_interpolate() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        methodpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        methodpanel.setBorder(commonBorder);
        methodpanel.add(new JLabel("Interpolate using   "));
        String methodString=(String)parameters.getParameter(Operation_interpolate.METHOD);
        methodCombobox = new JComboBox(Operation_interpolate.getInterpolationMethods());
        if (methodString!=null) methodCombobox.setSelectedItem(methodString);
        else methodCombobox.setSelectedIndex(0);
        methodpanel.add(methodCombobox);         
        String periodString=(String)parameters.getParameter(Operation_interpolate.PERIOD);
        boolean usePeriod=true;
        if (periodString==null) {
            usePeriod=false;
            periodString="25";
        }
        String maxDistanceString=(String)parameters.getParameter(Operation_interpolate.MAX_DISTANCE);
        if (maxDistanceString==null) {
            maxDistanceString="0";
        }        
        DefaultComboBoxModel periodmodel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        periodCombobox = new JComboBox(periodmodel);
        periodCombobox.setEditable(true);
        periodCombobox.setSelectedItem(periodString);
        
        DefaultComboBoxModel maxDistanceModel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        maxDistanceCombobox = new JComboBox(maxDistanceModel);
        maxDistanceCombobox.setEditable(true);
        maxDistanceCombobox.setSelectedItem(maxDistanceString);        
        
        usePeriodCheckbox=new JCheckBox(" with period  ");
        usePeriodCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                periodCombobox.setEnabled(usePeriodCheckbox.isSelected());
            }
        });
        usePeriodCheckbox.setSelected(usePeriod);
        periodCombobox.setEnabled(usePeriod);
        methodpanel.add(usePeriodCheckbox);  
        methodpanel.add(periodCombobox);
        methodpanel.add(new JLabel("  max distance")); 
        methodpanel.add(maxDistanceCombobox);         
        
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(methodpanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        parameters.setParameter(Operation_interpolate.METHOD, (String)methodCombobox.getSelectedItem());
        if (usePeriodCheckbox.isSelected()) parameters.setParameter(Operation_interpolate.PERIOD, (String)periodCombobox.getSelectedItem());
        else parameters.setParameter(Operation_interpolate.PERIOD, null);
        String maxDistance=(String)maxDistanceCombobox.getSelectedItem();
        if (maxDistance==null) maxDistance=""; else maxDistance=maxDistance.trim();
        if (!maxDistance.isEmpty() && !maxDistance.equals("0")) {parameters.setParameter(Operation_interpolate.MAX_DISTANCE, maxDistance);}
        else parameters.setParameter(Operation_interpolate.MAX_DISTANCE, null);      
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
