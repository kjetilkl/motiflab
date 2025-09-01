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
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.operations.Operation_score;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_score extends FeatureTransformOperationDialog {
    private JPanel parametersPanel;
    private JComboBox motifCombobox;
    private JComboBox scoreCombobox;
    private JComboBox rawOrLogoddsCombobox;
    private JComboBox strandCombobox;
    private JComboBox backgroundCombobox;
    private JPanel backgroundPanel;    
    
    
    public OperationDialog_score(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_score() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        ActionListener[] listeners=targetDataTextfield.getActionListeners();
        for (ActionListener listener:listeners) targetDataTextfield.removeActionListener(listener);
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("MotifScore", getDataTypeTable());        
        targetDataTextfield.setText(targetName);
        parametersPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        parametersPanel.setBorder(commonBorder);
        
        DefaultComboBoxModel backgroundComboBoxModel=getDataCandidates(new Class[]{BackgroundModel.class},true);
        backgroundCombobox = new JComboBox(backgroundComboBoxModel);
        backgroundCombobox.setEditable(true);

                
        backgroundPanel=new JPanel(new FlowLayout(FlowLayout.LEADING,0,0));
        backgroundPanel.add(new JLabel("   against  "));
        backgroundPanel.add(backgroundCombobox); 
        
        parametersPanel.add(new JLabel("Motif  "));
        String motifName=(String)parameters.getParameter(Operation_score.MOTIFNAME);
        // what if there are no motifs
        DefaultComboBoxModel wbmodel=getDataCandidates(new Class[]{Motif.class,MotifCollection.class});
        motifCombobox = new JComboBox(wbmodel);
        motifCombobox.setEditable(false);
        if (wbmodel.getSize()==0) motifCombobox.setSelectedIndex(-1);
        else if (motifName==null) motifCombobox.setSelectedIndex(0);
        else motifCombobox.setSelectedItem(motifName);
        parametersPanel.add(motifCombobox);   
              
        parametersPanel.add(new JLabel("   Score  "));
        scoreCombobox=new JComboBox(new String[]{Operation_score.SCORE_ABSOLUTE,Operation_score.SCORE_RELATIVE});
        String scoreString=(String)parameters.getParameter(Operation_score.SCORE);
        if (scoreString!=null) scoreCombobox.setSelectedItem(scoreString);       
        parametersPanel.add(scoreCombobox);
        
        rawOrLogoddsCombobox=new JComboBox(new String[]{Operation_score.RAW,Operation_score.LOGLIKELIHOOD});
        String rawOrLogoddsString=(String)parameters.getParameter(Operation_score.RAW_OR_LOGLIKELIHOOD);
        if (rawOrLogoddsString!=null) rawOrLogoddsCombobox.setSelectedItem(rawOrLogoddsString);
        rawOrLogoddsCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selected=(String)rawOrLogoddsCombobox.getSelectedItem();
                backgroundPanel.setVisible(selected.equalsIgnoreCase(Operation_score.LOGLIKELIHOOD));
            }
        });
        parametersPanel.add(rawOrLogoddsCombobox);                      
        parametersPanel.add(backgroundPanel);
        String backgroundString=(String)parameters.getParameter(Operation_score.BACKGROUNDMODEL);
        if (backgroundString!=null) backgroundCombobox.setSelectedItem(backgroundString);
        else backgroundCombobox.setSelectedIndex(0);        
        
        parametersPanel.add(new JLabel("   Strand  "));
        strandCombobox=new JComboBox(new String[]{Operation_score.STRAND_BOTH,Operation_score.STRAND_RELATIVE,Operation_score.STRAND_OPPOSITE,Operation_score.STRAND_DIRECT,Operation_score.STRAND_REVERSE});
        String strandString=(String)parameters.getParameter(Operation_score.STRAND);
        if (strandString!=null) strandCombobox.setSelectedItem(strandString);
        parametersPanel.add(strandCombobox);
        
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(parametersPanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();         
        String selected=(String)rawOrLogoddsCombobox.getSelectedItem();
        backgroundPanel.setVisible(selected.equalsIgnoreCase(Operation_score.LOGLIKELIHOOD));
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        parameters.setParameter(Operation_score.MOTIFNAME, (String)motifCombobox.getSelectedItem());
        String rawOrLogodds=(String)rawOrLogoddsCombobox.getSelectedItem();
        parameters.setParameter(Operation_score.RAW_OR_LOGLIKELIHOOD,rawOrLogodds);
        parameters.setParameter(Operation_score.SCORE, (String)scoreCombobox.getSelectedItem());
        parameters.setParameter(Operation_score.STRAND, (String)strandCombobox.getSelectedItem());
        if (rawOrLogodds.equalsIgnoreCase(Operation_score.LOGLIKELIHOOD)) {
            String background=(String)backgroundCombobox.getSelectedItem();
            if (background!=null && background.trim().isEmpty()) background=null;
            parameters.setParameter(Operation_score.BACKGROUNDMODEL,background);
        } else parameters.setParameter(Operation_score.BACKGROUNDMODEL,null);
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
