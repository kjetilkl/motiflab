/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModulePartition;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.operations.Operation_difference;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_difference extends OperationDialog {
    private JPanel compareToPanel;
    private JComboBox compareToCombobox;
    private Data sourceData=null;
    private JComboBox regionDatasetOptionsCombobox;
    private JComboBox regionDatasetTargetPropertyCombobox;   
    private JLabel targetPropertyLabel;
    
    public OperationDialog_difference(JFrame parent) {
        super(parent); 
        initComponents();        
    }
    
    public OperationDialog_difference() {
        super();
    }    
        
    
    @Override
    public void initComponents() {
        super.initComponents();
        sourceLabel.setText("Compare  ");
        String targetName=(String)parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetDataTextfield.setText(gui.getGenericDataitemName(Data.class, getDataTypeTable()));
        ActionListener[] list=sourceDataCombobox.getActionListeners();
        for (ActionListener listener:list) sourceDataCombobox.removeActionListener(listener);
        sourceDataCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sourceName=(String)sourceDataCombobox.getSelectedItem();
                Class sourceclass1=getClassForDataItem(sourceName);
                if (sourceclass1==null) compareToCombobox.setModel(new DefaultComboBoxModel(new String[0]));
                else compareToCombobox.setModel(getDataCandidates(sourceclass1));
                regionDatasetOptionsCombobox.setVisible(sourceclass1==RegionDataset.class);
                targetPropertyLabel.setVisible(sourceclass1==RegionDataset.class);
                regionDatasetTargetPropertyCombobox.setVisible(sourceclass1==RegionDataset.class);                
            }
        });   
        compareToCombobox = new JComboBox(); 
        compareToCombobox.setEditable(true);
        String[] tmp=Operation_difference.getRegionDatasetOptions();
        String[] regionOptions=new String[tmp.length+1];
        System.arraycopy(tmp, 0, regionOptions, 1, tmp.length);
        regionOptions[0]="";
        regionDatasetOptionsCombobox = new JComboBox(regionOptions);
        regionDatasetTargetPropertyCombobox = new JComboBox(new String[]{"","score"});        
        regionDatasetTargetPropertyCombobox.setEditable(true);

        compareToPanel=new JPanel();
        compareToPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        compareToPanel.setBorder(commonBorder);
        JLabel toLabel=new JLabel("To  ");
        toLabel.setPreferredSize(sourceLabel.getPreferredSize());
        toLabel.setMinimumSize(sourceLabel.getMinimumSize());
        //compareToPanel.add(new JLabel("To  "));
        compareToPanel.add(toLabel);        
        compareToPanel.add(compareToCombobox);
        targetPropertyLabel=new JLabel("  property ");
        compareToPanel.add(targetPropertyLabel);
        compareToPanel.add(regionDatasetTargetPropertyCombobox);           
        compareToPanel.add(new JLabel("  "));
        compareToPanel.add(regionDatasetOptionsCombobox);   
        add(getSourceTargetPanel());   
        add(compareToPanel);
        add(getOKCancelButtonsPanel());
        String sourceName=parameters.getSourceDataName();
        if (sourceName!=null && !sourceName.isEmpty()) {
            if (sourceName.contains(",")) {
                String[] parts=sourceName.split("\\s*,\\s*");
                sourceDataCombobox.setSelectedItem(parts[0]);
                compareToCombobox.setSelectedItem(parts[1]);
            }
            else sourceDataCombobox.setSelectedItem(sourceName);
        }
        else if (sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedIndex(0);
        String compareToName=(String)parameters.getParameter(Operation_difference.COMPARE_AGAINST_DATA);
        if (compareToName!=null && !compareToName.isEmpty()) compareToCombobox.setSelectedItem(compareToName);
        String option=(String)parameters.getParameter(Operation_difference.REGION_DATASET_OPTIONS);
        if (option!=null && !option.isEmpty()) regionDatasetOptionsCombobox.setSelectedItem(option);    
        String targetProperty=(String)parameters.getParameter(Operation_difference.REGION_DATASET_TARGET_PROPERTY);
        if (targetProperty!=null && !targetProperty.isEmpty()) regionDatasetTargetPropertyCombobox.setSelectedItem(targetProperty);           
        pack();
    }

    @Override
    protected void setParameters() {
        super.setParameters();
        String sourceName2=(String)compareToCombobox.getSelectedItem();
        parameters.setParameter(Operation_difference.COMPARE_AGAINST_DATA, sourceName2);
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        Class sourceclass1=getClassForDataItem(parameters.getSourceDataName()); 
        Class targetclass=null;
             if (sourceclass1==NumericDataset.class) targetclass=NumericDataset.class;
        else if (sourceclass1==DNASequenceDataset.class) targetclass=NumericDataset.class;
        else if (sourceclass1==RegionDataset.class) targetclass=RegionDataset.class;
        else if (NumericMap.class.isAssignableFrom(sourceclass1)) targetclass=sourceclass1;
        else if (sourceclass1==MotifCollection.class) targetclass=MotifPartition.class;
        else if (sourceclass1==ModuleCollection.class) targetclass=ModulePartition.class;
        else if (sourceclass1==SequenceCollection.class) targetclass=SequencePartition.class;  
        if (sourceclass1==RegionDataset.class) {
            String option=(String)regionDatasetOptionsCombobox.getSelectedItem();
            if (option.isEmpty()) parameters.removeParameter(Operation_difference.REGION_DATASET_OPTIONS);  
            else parameters.setParameter(Operation_difference.REGION_DATASET_OPTIONS, option);
            String targetProperty=(String)regionDatasetTargetPropertyCombobox.getSelectedItem();
            if (targetProperty.isEmpty()) parameters.removeParameter(Operation_difference.REGION_DATASET_TARGET_PROPERTY);  
            else parameters.setParameter(Operation_difference.REGION_DATASET_TARGET_PROPERTY, targetProperty);            
        } else {
            parameters.removeParameter(Operation_difference.REGION_DATASET_OPTIONS);
            parameters.removeParameter(Operation_difference.REGION_DATASET_TARGET_PROPERTY);
        }  
        parameters.addAffectedDataObject(targetName, targetclass);
    }
    
    
  
   
}

