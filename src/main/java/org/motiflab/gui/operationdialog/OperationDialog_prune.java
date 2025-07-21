package org.motiflab.gui.operationdialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Operation_prune;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_prune extends FeatureTransformOperationDialog {
    private JPanel prunePanel;
    private JComboBox pruneOptionCombobox;
    private JComboBox alternativesPartitionCombobox;
    private JComboBox keepCombobox;
    private JLabel pruneFromLabel;
    private HashMap<String,DefaultComboBoxModel> keepModels;
    private JLabel keepLabel;



    public OperationDialog_prune(JFrame parent) {
        super(parent);
    }

    public OperationDialog_prune() {
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
        setupPrunePanel();
        prunePanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(prunePanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();
    }

    private void setupPrunePanel() {
        prunePanel=new JPanel(new BorderLayout());
        String[] pruneOptions=Operation_prune.getPruneOptions();
        pruneOptionCombobox=new JComboBox(pruneOptions);
        alternativesPartitionCombobox=new JComboBox(getDataCandidates(MotifPartition.class,true));
        JPanel pruneTopPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel pruneBottomPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));       
        pruneTopPanel.add(new JLabel("Prune  "));
        pruneTopPanel.add(pruneOptionCombobox);
        pruneFromLabel=new JLabel("  from   ");
        pruneTopPanel.add(pruneFromLabel);
        pruneTopPanel.add(alternativesPartitionCombobox);
        keepModels=new HashMap<String, DefaultComboBoxModel>();
        for (String option:pruneOptions) {
            DefaultComboBoxModel optionmodel=new DefaultComboBoxModel(Operation_prune.getKeepOptions(option));
            keepModels.put(option, optionmodel);
        }
        keepCombobox=new JComboBox();
        keepLabel=new JLabel("Keep  ");
        pruneBottomPanel.add(keepLabel);
        pruneBottomPanel.add(keepCombobox);
        pruneOptionCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selected=(String)pruneOptionCombobox.getSelectedItem();
                if (selected.equals(Operation_prune.PRUNE_ALTERNATIVES) || selected.equals(Operation_prune.PRUNE_ALTERNATIVES_NAIVE)) {
                    pruneFromLabel.setVisible(true);
                    alternativesPartitionCombobox.setVisible(true);
                } else { // palindromes and duplicates
                    pruneFromLabel.setVisible(false);
                    alternativesPartitionCombobox.setVisible(false);                    
                }
                DefaultComboBoxModel keepModel=keepModels.get(selected);
                keepCombobox.setModel(keepModel);
                if (keepModel.getSize()>0) {
                    keepLabel.setVisible(true);
                    keepCombobox.setVisible(true);                    
                } else {
                    keepLabel.setVisible(false);
                    keepCombobox.setVisible(false); 
                }
                 pack();
            }
        });
        String selectedPrune=(String)parameters.getParameter(Operation_prune.PRUNE);
        String selectedPartition=(String)parameters.getParameter(Operation_prune.MOTIFPARTITION);
        String selectedKeep=(String)parameters.getParameter(Operation_prune.KEEP);
        if (selectedPrune!=null) {
            pruneOptionCombobox.setSelectedItem(selectedPrune);
            if (selectedPartition!=null) alternativesPartitionCombobox.setSelectedItem(selectedPartition);
            if (selectedKeep!=null) keepCombobox.setSelectedItem(selectedKeep);
        } else pruneOptionCombobox.setSelectedIndex(0);
        prunePanel.add(pruneTopPanel,BorderLayout.NORTH);
        prunePanel.add(pruneBottomPanel,BorderLayout.SOUTH);
    }


    @Override
    protected void setParameters() {
        super.setParameters();
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, RegionDataset.class);
        String selectedPrune=(String)pruneOptionCombobox.getSelectedItem();
        String selectedPartition=(String)alternativesPartitionCombobox.getSelectedItem();
        if (selectedPartition!=null && selectedPartition.trim().isEmpty()) selectedPartition=null;
        String selectedKeep=(String)keepCombobox.getSelectedItem();
        parameters.setParameter(Operation_prune.PRUNE,selectedPrune);
        boolean includePartition=(selectedPrune.equals(Operation_prune.PRUNE_ALTERNATIVES) || selectedPrune.equals(Operation_prune.PRUNE_ALTERNATIVES_NAIVE));
        parameters.setParameter(Operation_prune.MOTIFPARTITION,(includePartition)?selectedPartition:null);
        parameters.setParameter(Operation_prune.KEEP,selectedKeep);
    }
}
