/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.operations.Operation_crop_sequences;
import org.motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_crop_sequences extends OperationDialog {
    
    private JRadioButton cropToRegionsButton;
    private JRadioButton cropByXBpButton;
    private JPanel cropPanel;
    private JPanel firstPanel;
    private JPanel secondPanel;
    private JPanel cropToRegionsPanel;
    private JComboBox cropToRegionsCombobox;
    private JComboBox firstDirectionCombobox;
    private JComboBox secondDirectionCombobox;
    private JComboBox firstCropByCombobox;
    private JComboBox secondCropByCombobox;
    private JCheckBox specifyBothDirectionsCheckBox;
    private JCheckBox useRelativeOrientationCheckBox;
    private JPanel sequenceSubsetPanel=null;
    private JComboBox sequenceSubsetCombobox=null;    
    
    public OperationDialog_crop_sequences(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_crop_sequences() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();            
        cropToRegionsButton=new JRadioButton();
        cropByXBpButton=new JRadioButton();
        cropByXBpButton.setVerticalAlignment(SwingConstants.TOP);
        cropByXBpButton.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0)); // a hack to achieve proper alignment
        ButtonGroup gr=new ButtonGroup();
        gr.add(cropByXBpButton);
        gr.add(cropToRegionsButton);
        
        cropToRegionsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        cropToRegionsCombobox=getRegionsCombobox();
        cropToRegionsPanel.add(new JLabel("Crop to "));
        cropToRegionsPanel.add(cropToRegionsCombobox);
        
        cropPanel=new JPanel();
        cropPanel.setLayout(new BoxLayout(cropPanel, BoxLayout.Y_AXIS));
        cropPanel.setBorder(commonBorder);
        JLabel label=new JLabel("Crop by ");
        Dimension labelsize=label.getPreferredSize();
        labelsize.width+=20;
        label.setPreferredSize(labelsize);
        specifyBothDirectionsCheckBox=new JCheckBox("and by ");
        specifyBothDirectionsCheckBox.setPreferredSize(labelsize);
        specifyBothDirectionsCheckBox.setMinimumSize(labelsize);
        specifyBothDirectionsCheckBox.setMaximumSize(labelsize);        
        useRelativeOrientationCheckBox=new JCheckBox("      Use relative orientations");
        label.setVerticalAlignment(JLabel.TOP);    
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        specifyBothDirectionsCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
        specifyBothDirectionsCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        useRelativeOrientationCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        useRelativeOrientationCheckBox.setToolTipText("<html>When using relative orientations, extensions are specified as going in either<br><i>upstream</i> or <i>downstream</i> directions relative to the orientation of the sequences themselves.<br>Thus, if a sequence located at chr2:1000-1100 on the direct strand is croped by 50 bp downstream,<br>the new location will be chr2:1000-1150. If the same sequence had been on the reverse strand, however,<br>the new location would have been chr2:950-1100. If relative orientations are not used,<br>all sequences are treated as if they were on the direct strand, and extensions<br>are added either <i>before start</i> of the sequence (by decreasing the start coordinate)<br>or <i>after end</i> of sequence (by increasing the end coordinate).</html>");
        
        firstPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        secondPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        firstDirectionCombobox = new JComboBox(new String[]{""});
        secondDirectionCombobox = new JComboBox(new String[]{""});

        firstCropByCombobox=getNumericCombobox(); 
        secondCropByCombobox=getNumericCombobox();
        
        firstPanel.add(label);
        firstPanel.add(firstCropByCombobox);
        firstPanel.add(firstDirectionCombobox);        
        firstPanel.add(useRelativeOrientationCheckBox);
        secondPanel.add(specifyBothDirectionsCheckBox);
        secondPanel.add(secondCropByCombobox);
        secondPanel.add(secondDirectionCombobox);        
                
        firstDirectionCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String firstSelectedDirection=firstDirectionCombobox.getSelectedItem().toString();
                if (firstSelectedDirection.isEmpty()) secondPanel.setVisible(false);
                else {
                    String other="";
                    if (firstSelectedDirection.equals("from upstream end")) other="from downstream end";
                    else if (firstSelectedDirection.equals("from downstream end")) other="from upstream end";
                    else if (firstSelectedDirection.equals("from start")) other="from end";
                    else if (firstSelectedDirection.equals("from end")) other="from start";
                    secondDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{other}));
                    secondDirectionCombobox.setSelectedIndex(0);
                    secondPanel.setVisible(true);
                }
                pack();
            }
        });
        specifyBothDirectionsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean both=specifyBothDirectionsCheckBox.isSelected();
                secondCropByCombobox.setEnabled(both);
                secondDirectionCombobox.setEnabled(both);
            }
        });
        useRelativeOrientationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setUseRelativeOrientation(useRelativeOrientationCheckBox.isSelected());
            }
        }); 
        cropByXBpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cropPanel.setEnabled(true);
                cropToRegionsPanel.setEnabled(false);
            }
        });
        cropToRegionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cropPanel.setEnabled(false);
                cropToRegionsPanel.setEnabled(true);
            }
        });         
        

        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);  
        
        cropPanel.add(firstPanel);
        cropPanel.add(secondPanel);
        
        JPanel outerPanel=new JPanel(new GridBagLayout());
        outerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.fill = java.awt.GridBagConstraints.BOTH;
        constraints.anchor = java.awt.GridBagConstraints.NORTHWEST;        
        constraints.ipadx = 5;
        constraints.ipady = 5;
        
        constraints.gridy=1;constraints.gridx=0;
        outerPanel.add(cropByXBpButton,constraints);
        
        constraints.gridy=1;constraints.gridx=1;
        outerPanel.add(cropPanel,constraints);
        
        constraints.gridy=0;constraints.gridx=0;
        outerPanel.add(cropToRegionsButton,constraints);
        
        constraints.gridy=0;constraints.gridx=1;
        outerPanel.add(cropToRegionsPanel,constraints);        
        
        add(outerPanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        

        String cropToRegionExpression=(String)parameters.getParameter(Operation_crop_sequences.CROP_TO_REGIONS);
        String cropExpression=(String)parameters.getParameter(Operation_crop_sequences.CROP_EXPRESSION);
        String cropUpstreamExpression=(String)parameters.getParameter(Operation_crop_sequences.CROP_UPSTREAM_EXPRESSION);
        String cropDownstreamExpression=(String)parameters.getParameter(Operation_crop_sequences.CROP_DOWNSTREAM_EXPRESSION);
        Object orientation=parameters.getParameter(Operation_crop_sequences.USE_RELATIVE_ORIENTATION);
        boolean useRelativeOrientation=(orientation instanceof Boolean)?((Boolean)orientation):false;
        
        
        // initialize
        if (cropToRegionsCombobox.getModel().getSize()>0) cropToRegionsCombobox.setSelectedIndex(0); // just a default selection
        useRelativeOrientationCheckBox.setSelected(!useRelativeOrientation); // NB use opposite since doClick on the next line will reverse it
        useRelativeOrientationCheckBox.doClick();        
        cropByXBpButton.setSelected(true);
        if (cropExpression==null && cropUpstreamExpression==null && cropDownstreamExpression==null) {
            // no preselected options
           firstCropByCombobox.setSelectedItem("100");
           firstDirectionCombobox.setSelectedItem("");
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (cropExpression!=null) {
           firstCropByCombobox.setSelectedItem(cropExpression);
           firstDirectionCombobox.setSelectedItem(""); 
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (cropUpstreamExpression!=null && cropDownstreamExpression==null) {
           firstCropByCombobox.setSelectedItem(cropUpstreamExpression);
           secondCropByCombobox.setSelectedItem("");            
           String direction1=(useRelativeOrientation)?"from upstream end":"from start";
           String direction2=(useRelativeOrientation)?"from downstream end":"from end";
           firstDirectionCombobox.setSelectedItem(direction1);    
           secondDirectionCombobox.setSelectedItem(direction2);     
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (cropUpstreamExpression==null && cropDownstreamExpression!=null) {
           firstCropByCombobox.setSelectedItem(cropDownstreamExpression);
           secondCropByCombobox.setSelectedItem("");            
           String direction1=(useRelativeOrientation)?"from downstream end":"from end";
           String direction2=(useRelativeOrientation)?"from upstream end":"from start";          
           firstDirectionCombobox.setSelectedItem(direction1);    
           secondDirectionCombobox.setSelectedItem(direction2);     
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (cropUpstreamExpression!=null && cropDownstreamExpression!=null) {
           firstCropByCombobox.setSelectedItem(cropUpstreamExpression);
           secondCropByCombobox.setSelectedItem(cropDownstreamExpression);            
           String direction1=(useRelativeOrientation)?"from upstream end":"from start";
           String direction2=(useRelativeOrientation)?"from downstream end":"from end";
           firstDirectionCombobox.setSelectedItem(direction1);    
           secondDirectionCombobox.setSelectedItem(direction2);     
           specifyBothDirectionsCheckBox.setSelected(true);
        }
        boolean both=specifyBothDirectionsCheckBox.isSelected();
        secondCropByCombobox.setEnabled(both);
        secondDirectionCombobox.setEnabled(both);   
        
        if (cropToRegionExpression!=null && !cropToRegionExpression.isEmpty()) {
            cropToRegionsButton.setSelected(true); // this should deselect the other option
            cropToRegionsCombobox.setSelectedItem(cropToRegionExpression);
        }
    
    }

    private void setUseRelativeOrientation(boolean useRelative) {
        String firstDirection=firstDirectionCombobox.getSelectedItem().toString();  
        String secondDirection="";
        if (useRelative) {
            if (firstDirection==null || firstDirection.isEmpty()) {
                firstDirection="";secondDirection="from downstream end";
            } else if (firstDirection.equals("from upstream end") || firstDirection.equals("from start")) {
                firstDirection="from upstream end";secondDirection="from downstream end";
            } else {
                firstDirection="from downstream end";secondDirection="from upstream end";
            }
            firstDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{"","from upstream end","from downstream end"}));
            //secondDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{secondDirection});          
            firstDirectionCombobox.setSelectedItem(firstDirection);
        } else {
            if (firstDirection==null || firstDirection.isEmpty()) {
                firstDirection=""; 
            } else if (firstDirection.equals("from upstream end") || firstDirection.equals("from start")) {
                firstDirection="from start"; 
            } else {
                firstDirection="from end";
            }
            firstDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{"","from start","from end"}));
            //secondDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{"from start","from end"}));          
            firstDirectionCombobox.setSelectedItem(firstDirection);
        }        
    }
    
    
    
    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        
        if (sequenceSubsetPanel!=null) {
          String sequenceCollection=sequenceSubsetCombobox.getSelectedItem().toString();
          if (sequenceCollection.equals(engine.getDefaultSequenceCollectionName())) sequenceCollection=null;
          parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
        }
        
        String bothDirectionsExpressionParameter=null;
        String upstreamExpressionParameter=null;
        String downstreamExpressionParameter=null;
        String cropToRegionsExpressionParameter=null;        
        
        if (cropToRegionsButton.isSelected()) {
           cropToRegionsExpressionParameter=(String)cropToRegionsCombobox.getSelectedItem(); 
        } else {
            String firstSelectedDirection=firstDirectionCombobox.getSelectedItem().toString();
            if (firstSelectedDirection.isEmpty()) { // both directions same
                bothDirectionsExpressionParameter=(String)firstCropByCombobox.getSelectedItem();
                if (bothDirectionsExpressionParameter==null || bothDirectionsExpressionParameter.trim().isEmpty()) bothDirectionsExpressionParameter="0";
            } else { // either different directions, or just one specified
                String firstExpression=(String)firstCropByCombobox.getSelectedItem();
                if (firstExpression==null || firstExpression.trim().isEmpty()) firstExpression="0";
                boolean firstDirectionUpstream=(firstSelectedDirection.equals("from upstream end") || firstSelectedDirection.equals("from start"));
                if (firstDirectionUpstream) {
                    upstreamExpressionParameter=firstExpression;            
                } else { // firstdirection==downstream
                    downstreamExpressionParameter=firstExpression;                
                }
                if (specifyBothDirectionsCheckBox.isSelected()) { // two individual directions
                    // second direction must be opposite of first
                    String secondExpression=(String)secondCropByCombobox.getSelectedItem();
                    if (secondExpression==null || secondExpression.trim().isEmpty()) secondExpression="0";                
                    if (firstDirectionUpstream) {
                        downstreamExpressionParameter=secondExpression;            
                    } else { // firstdirection==downstream
                        upstreamExpressionParameter=secondExpression;                
                    }                
                }                                 
            }
            if (bothDirectionsExpressionParameter!=null) {
                upstreamExpressionParameter=null;
                downstreamExpressionParameter=null;
            }
        }
        
        String firstDirection=(String)firstDirectionCombobox.getSelectedItem();
        boolean useRelativeOrientation=(firstDirection.equals("from upstream end") || firstDirection.equals("from downstream end"));
        parameters.setParameter(Operation_crop_sequences.CROP_TO_REGIONS, cropToRegionsExpressionParameter);
        parameters.setParameter(Operation_crop_sequences.CROP_EXPRESSION, bothDirectionsExpressionParameter);
        parameters.setParameter(Operation_crop_sequences.CROP_UPSTREAM_EXPRESSION, upstreamExpressionParameter);
        parameters.setParameter(Operation_crop_sequences.CROP_DOWNSTREAM_EXPRESSION, downstreamExpressionParameter);
        parameters.setParameter(Operation_crop_sequences.USE_RELATIVE_ORIENTATION,useRelativeOrientation); 
    }
    
    private JComboBox getNumericCombobox() {
        DefaultComboBoxModel model=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        JComboBox combobox = new JComboBox(model);
        combobox.setEditable(true);  
        return combobox;
    }    
    
    private JComboBox getRegionsCombobox() {
        DefaultComboBoxModel model=getDataCandidates(new Class[]{RegionDataset.class});
        JComboBox combobox = new JComboBox(model);
        combobox.setEditable(true);  
        return combobox;
    }      
    
    private JPanel getSequenceSubsetPanel() {
        if (sequenceSubsetPanel==null) initSequenceSubsetPanel();
        return sequenceSubsetPanel;
    }
    
    private void initSequenceSubsetPanel() {
        sequenceSubsetPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        sequenceSubsetPanel.add(new JLabel("In sequence collection  "));
        String selectedName=(String)parameters.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (selectedName==null) selectedName=engine.getDefaultSequenceCollectionName();
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(new Class[]{SequenceCollection.class});
        sequenceSubsetCombobox=new JComboBox(model);
        sequenceSubsetCombobox.setEditable(true);
        sequenceSubsetCombobox.setSelectedItem(selectedName);
        sequenceSubsetPanel.add(sequenceSubsetCombobox);                    
    }    
}
