/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import motiflab.engine.data.*;
import motiflab.engine.operations.Operation_mask;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_mask extends FeatureTransformOperationDialog {
    private JPanel maskpanel;
    private JComboBox backgroundCombobox;
    private JComboBox regionDatasetsCombobox;    
    private JTextField maskLetterTextField;
    private JRadioButton useLetter; 
    private JRadioButton useLowerCase; 
    private JRadioButton useUpperCase; 
    private JRadioButton useBackgroundModel; 
    private JRadioButton useRegionDataset;    
    private JComboBox strandCombobox;

    
    public OperationDialog_mask(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_mask() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        maskpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        maskpanel.setBorder(commonBorder);
        JLabel masklabel=new JLabel("Mask with   ");
        maskpanel.add(masklabel);
        strandCombobox=new JComboBox(new String[]{"Relative","Direct"});
        JPanel strandPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        strandPanel.add(new JLabel("  Strand      "));
        strandPanel.add(strandCombobox);        
        JPanel maskChoicePanel = new JPanel();
        maskChoicePanel.setLayout(new BoxLayout(maskChoicePanel,BoxLayout.Y_AXIS));
        ButtonGroup buttonGroup=new ButtonGroup();
        useLowerCase=new JRadioButton("lowercase letters");
        useUpperCase=new JRadioButton("uppercase letters");
        useLetter=new JRadioButton("specific letter    ");
        useBackgroundModel=new JRadioButton("random bases  ");
        useRegionDataset=new JRadioButton("sequence property from  ");        
        buttonGroup.add(useLowerCase);
        buttonGroup.add(useUpperCase);
        buttonGroup.add(useLetter);
        buttonGroup.add(useBackgroundModel);
        buttonGroup.add(useRegionDataset);        
        JPanel lowercasepanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel uppercasepanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        lowercasepanel.add(useLowerCase);
        uppercasepanel.add(useUpperCase);
        JPanel letterpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        maskLetterTextField = new JTextField(1);
        maskLetterTextField.setText("X");
        letterpanel.add(useLetter);
        letterpanel.add(maskLetterTextField);               
        DefaultComboBoxModel backgroundComboBoxModel=getDataCandidates(new Class[]{BackgroundModel.class});
        backgroundCombobox = new JComboBox(backgroundComboBoxModel);
        backgroundCombobox.setEditable(true);
        DefaultComboBoxModel regiondatasetComboBoxModel=getDataCandidates(new Class[]{RegionDataset.class});
        regionDatasetsCombobox = new JComboBox(regiondatasetComboBoxModel);
        regionDatasetsCombobox.setEditable(true);        
        JPanel backgroundpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        backgroundpanel.add(useBackgroundModel);
        backgroundpanel.add(backgroundCombobox);
        JPanel regiondatasetpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        regiondatasetpanel.add(useRegionDataset);
        regiondatasetpanel.add(regionDatasetsCombobox);        
        maskChoicePanel.add(lowercasepanel);
        maskChoicePanel.add(uppercasepanel);
        maskChoicePanel.add(letterpanel);
        maskChoicePanel.add(backgroundpanel);  
        maskChoicePanel.add(regiondatasetpanel);          
        maskpanel.add(maskChoicePanel);
        String maskString=(String)parameters.getParameter(Operation_mask.MASK_STRING);
        if (maskString==null || maskString.isEmpty()) {
           useLetter.setSelected(true);  
        } else if (maskString.equalsIgnoreCase("uppercase")) {
           useUpperCase.setSelected(true); 
        } else if (maskString.equalsIgnoreCase("lowercase")) {
           useLowerCase.setSelected(true); 
        } else if (maskString.startsWith("'")) {
           useLetter.setSelected(true);
           if (maskString.length()>1) maskLetterTextField.setText(maskString.substring(1,2));
        } else if (maskString.startsWith("\"")) {
           useLetter.setSelected(true);
           if (maskString.length()>1) maskLetterTextField.setText(maskString.substring(1,2));
        } else { // mask string is the name of a data object (Background model or Region Dataset)
            if (regiondatasetComboBoxModel.getIndexOf(maskString)>=0) { // is the maskstring present in the RegionDataset combobox?
               useRegionDataset.setSelected(true);
               regionDatasetsCombobox.setSelectedItem(maskString);               
            } else {
               useBackgroundModel.setSelected(true);
               backgroundCombobox.setSelectedItem(maskString); 
            }
        }
        String strandString=(String)parameters.getParameter(Operation_mask.STRAND);
        if (strandString==null) strandCombobox.setSelectedItem("Relative");
        else if (strandString.equalsIgnoreCase("Sequence")||strandString.equalsIgnoreCase("Relative")||strandString.equalsIgnoreCase("Gene")) strandCombobox.setSelectedItem("Relative");
        else if (strandString.equalsIgnoreCase("Direct")) strandCombobox.setSelectedItem("Direct");
        else strandCombobox.setSelectedItem("Relative");
        backgroundCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useBackgroundModel.setSelected(true);
            }
        });
        regionDatasetsCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useRegionDataset.setSelected(true);
            }
        });        
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(maskpanel);
        add(strandPanel);        
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        String maskString="";
             if (useLetter.isSelected()) maskString="\""+maskLetterTextField.getText()+"\""; //maskString="'"+maskLetterTextField.getText()+"'";
        else if (useLowerCase.isSelected()) maskString="lowercase";
        else if (useUpperCase.isSelected()) maskString="uppercase";
        else if (useBackgroundModel.isSelected()) maskString=(String)backgroundCombobox.getSelectedItem();
        else if (useRegionDataset.isSelected()) maskString=(String)regionDatasetsCombobox.getSelectedItem();
        parameters.setParameter(Operation_mask.MASK_STRING, maskString);
        parameters.setParameter(Operation_mask.STRAND, ((String)strandCombobox.getSelectedItem()).toLowerCase());
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, DNASequenceDataset.class);
    }
}
