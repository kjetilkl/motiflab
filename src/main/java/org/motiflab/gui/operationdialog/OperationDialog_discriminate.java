/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.operations.Operation_discriminate;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_discriminate extends FeatureTransformOperationDialog {
    private JPanel parametersPanel1;
    private JPanel parametersPanel2;    
    private JComboBox positiveSetCombobox;
    private JComboBox negativeSetCombobox;
    private JComboBox dnaDatasetCombobox;
    private JComboBox wordsizeCombobox;
    private JComboBox orientationCombobox;
    private JComboBox anchorCombobox;
    
    
    public OperationDialog_discriminate(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_discriminate() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        getWhereClausePanel(0).setVisible(false);
        parametersPanel1=new JPanel(new FlowLayout(FlowLayout.LEADING));
        parametersPanel1.setBorder(commonBorder);
        parametersPanel2=new JPanel(new FlowLayout(FlowLayout.LEADING));
        parametersPanel2.setBorder(commonBorder);        
        
        DefaultComboBoxModel wordSizeModel=getDataCandidates(NumericVariable.class);     
        wordsizeCombobox = new JComboBox(wordSizeModel);
        wordsizeCombobox.setEditable(true);
        String wordsizeString=(String)parameters.getParameter(Operation_discriminate.WORD_SIZE);
        if (wordsizeString==null) wordsizeString="8";
        wordsizeCombobox.setSelectedItem(wordsizeString);
           
        DefaultComboBoxModel positiveSetModel=getDataCandidates(SequenceCollection.class);
        String positiveSetString=(String)parameters.getParameter(Operation_discriminate.POSITIVE_SET);
        positiveSetCombobox = new JComboBox(positiveSetModel);
        positiveSetCombobox.setEditable(true);
        if (positiveSetString!=null) positiveSetCombobox.setSelectedItem(positiveSetString);
        
        DefaultComboBoxModel negativeSetModel=getDataCandidates(SequenceCollection.class);
        String negativeSetString=(String)parameters.getParameter(Operation_discriminate.NEGATIVE_SET);
        negativeSetCombobox = new JComboBox(negativeSetModel);
        negativeSetCombobox.setEditable(true);
        if (negativeSetString!=null) negativeSetCombobox.setSelectedItem(negativeSetString);
        
        DefaultComboBoxModel dnaDatasetModel=getDataCandidates(DNASequenceDataset.class);
        String dnaDatasetString=(String)parameters.getParameter(Operation_discriminate.DNA_SEQUENCE);
        dnaDatasetCombobox = new JComboBox(dnaDatasetModel);
        dnaDatasetCombobox.setEditable(true);
        if (dnaDatasetString!=null) dnaDatasetCombobox.setSelectedItem(dnaDatasetString);
      
        orientationCombobox=new JComboBox(new String[]{Operation_discriminate.ORIENTATION_BOTH,Operation_discriminate.ORIENTATION_RELATIVE,Operation_discriminate.ORIENTATION_DIRECT});
        orientationCombobox.setEditable(false);
        String orientationString=(String)parameters.getParameter(Operation_discriminate.ORIENTATION);
        if (orientationString!=null) orientationCombobox.setSelectedItem(orientationString);
        else orientationCombobox.setSelectedItem(Operation_discriminate.ORIENTATION_BOTH);

        anchorCombobox=new JComboBox(new String[]{Operation_discriminate.ANCHOR_START,Operation_discriminate.ANCHOR_RELATIVE_START});
        anchorCombobox.setEditable(false);
        String anchorString=(String)parameters.getParameter(Operation_discriminate.ANCHOR);
        if (anchorString!=null) anchorCombobox.setSelectedItem(anchorString);
        else anchorCombobox.setSelectedItem(Operation_discriminate.ANCHOR_RELATIVE_START);
        
        parametersPanel1.add(new JLabel("Discriminate values in positive set"));
        parametersPanel1.add(positiveSetCombobox);
        parametersPanel1.add(new JLabel("from negative set"));
        parametersPanel1.add(negativeSetCombobox);   
        parametersPanel2.add(new JLabel("based on words of size "));
        parametersPanel2.add(wordsizeCombobox);     
        parametersPanel2.add(new JLabel(" in "));
        parametersPanel2.add(dnaDatasetCombobox);          
        parametersPanel2.add(new JLabel(" on "));
        parametersPanel2.add(orientationCombobox);
        parametersPanel2.add(new JLabel(" strand(s) with anchor at "));
        parametersPanel2.add(anchorCombobox);
             
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(parametersPanel1);
        add(parametersPanel2);        
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        parameters.setParameter(Operation_discriminate.POSITIVE_SET, (String)positiveSetCombobox.getSelectedItem());
        parameters.setParameter(Operation_discriminate.NEGATIVE_SET, (String)negativeSetCombobox.getSelectedItem());
        parameters.setParameter(Operation_discriminate.DNA_SEQUENCE, (String)dnaDatasetCombobox.getSelectedItem());
        parameters.setParameter(Operation_discriminate.WORD_SIZE, (String)wordsizeCombobox.getSelectedItem());
        parameters.setParameter(Operation_discriminate.ORIENTATION, (String)orientationCombobox.getSelectedItem());   
        parameters.setParameter(Operation_discriminate.ANCHOR, (String)anchorCombobox.getSelectedItem());
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
