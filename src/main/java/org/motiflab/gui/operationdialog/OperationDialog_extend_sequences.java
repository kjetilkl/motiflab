/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.operations.Operation_extend_sequences;
import org.motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_extend_sequences extends OperationDialog {
    private JPanel extendPanel;
    private JPanel firstPanel;
    private JPanel secondPanel;
    private JComboBox firstDirectionCombobox;
    private JComboBox secondDirectionCombobox;
    private JComboBox firstExtendByCombobox;
    private JComboBox secondExtendByCombobox;
    private JCheckBox specifyBothDirectionsCheckBox;
    private JCheckBox useRelativeOrientationCheckBox;
    private JPanel sequenceSubsetPanel=null;
    private JComboBox sequenceSubsetCombobox=null;    
    
    public OperationDialog_extend_sequences(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_extend_sequences() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        extendPanel=new JPanel();
        extendPanel.setLayout(new BoxLayout(extendPanel, BoxLayout.Y_AXIS));
        extendPanel.setBorder(commonBorder);
        JLabel label=new JLabel("   Extend by   ");
        specifyBothDirectionsCheckBox=new JCheckBox("and by  ");
        specifyBothDirectionsCheckBox.setPreferredSize(label.getPreferredSize());
        specifyBothDirectionsCheckBox.setMinimumSize(label.getMinimumSize());
        specifyBothDirectionsCheckBox.setMaximumSize(label.getMaximumSize());        
        useRelativeOrientationCheckBox=new JCheckBox("      Use relative orientations");
        label.setVerticalAlignment(JLabel.TOP);    
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        specifyBothDirectionsCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
        specifyBothDirectionsCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        useRelativeOrientationCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        useRelativeOrientationCheckBox.setToolTipText("<html>When using relative orientations, extensions are specified as going in either<br><i>upstream</i> or <i>downstream</i> directions relative to the orientation of the sequences themselves.<br>Thus, if a sequence located at chr2:1000-1100 on the direct strand is extended by 50 bp downstream,<br>the new location will be chr2:1000-1150. If the same sequence had been on the reverse strand, however,<br>the new location would have been chr2:950-1100. If relative orientations are not used,<br>all sequences are treated as if they were on the direct strand, and extensions<br>are added either <i>before start</i> of the sequence (by decreasing the start coordinate)<br>or <i>after end</i> of sequence (by increasing the end coordinate).</html>");
        
        firstPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        secondPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        firstDirectionCombobox = new JComboBox(new String[]{""});
        secondDirectionCombobox = new JComboBox(new String[]{""});

        firstExtendByCombobox=getNumericCombobox(); 
        secondExtendByCombobox=getNumericCombobox();
        
        firstPanel.add(label);
        firstPanel.add(firstExtendByCombobox);
        firstPanel.add(firstDirectionCombobox);        
        firstPanel.add(useRelativeOrientationCheckBox);
        secondPanel.add(specifyBothDirectionsCheckBox);
        secondPanel.add(secondExtendByCombobox);
        secondPanel.add(secondDirectionCombobox);        
                
        firstDirectionCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String firstSelectedDirection=firstDirectionCombobox.getSelectedItem().toString();
                if (firstSelectedDirection.isEmpty()) secondPanel.setVisible(false);
                else {
                    String other="";
                    if (firstSelectedDirection.equals("upstream")) other="downstream";
                    else if (firstSelectedDirection.equals("downstream")) other="upstream";
                    else if (firstSelectedDirection.equals("before start")) other="after end";
                    else if (firstSelectedDirection.equals("after end")) other="before start";
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
                secondExtendByCombobox.setEnabled(both);
                secondDirectionCombobox.setEnabled(both);
            }
        });
        useRelativeOrientationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setUseRelativeOrientation(useRelativeOrientationCheckBox.isSelected());
            }
        });  

        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);       
        extendPanel.add(firstPanel);
        extendPanel.add(secondPanel);
        add(extendPanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        

        String extendExpression=(String)parameters.getParameter(Operation_extend_sequences.EXTEND_EXPRESSION);
        String extendUpstreamExpression=(String)parameters.getParameter(Operation_extend_sequences.EXTEND_UPSTREAM_EXPRESSION);
        String extendDownstreamExpression=(String)parameters.getParameter(Operation_extend_sequences.EXTEND_DOWNSTREAM_EXPRESSION);
        Object orientation=parameters.getParameter(Operation_extend_sequences.USE_RELATIVE_ORIENTATION);
        boolean useRelativeOrientation=(orientation instanceof Boolean)?((Boolean)orientation):false;
        
        
        // initialize
        useRelativeOrientationCheckBox.setSelected(!useRelativeOrientation); // NB use opposite since doClick on the next line will reverse it
        useRelativeOrientationCheckBox.doClick();        
        if (extendExpression==null && extendUpstreamExpression==null && extendDownstreamExpression==null) {
            // no preselected options
           firstExtendByCombobox.setSelectedItem("100");
           firstDirectionCombobox.setSelectedItem("");
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (extendExpression!=null) {
           firstExtendByCombobox.setSelectedItem(extendExpression);
           firstDirectionCombobox.setSelectedItem(""); 
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (extendUpstreamExpression!=null && extendDownstreamExpression==null) {
           firstExtendByCombobox.setSelectedItem(extendUpstreamExpression);
           secondExtendByCombobox.setSelectedItem("");            
           String direction1=(useRelativeOrientation)?"upstream":"before start";
           String direction2=(useRelativeOrientation)?"downstream":"after end";
           firstDirectionCombobox.setSelectedItem(direction1);    
           secondDirectionCombobox.setSelectedItem(direction2);     
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (extendUpstreamExpression==null && extendDownstreamExpression!=null) {
           firstExtendByCombobox.setSelectedItem(extendDownstreamExpression);
           secondExtendByCombobox.setSelectedItem("");            
           String direction1=(useRelativeOrientation)?"downstream":"after end";
           String direction2=(useRelativeOrientation)?"upstream":"before start";          
           firstDirectionCombobox.setSelectedItem(direction1);    
           secondDirectionCombobox.setSelectedItem(direction2);     
           specifyBothDirectionsCheckBox.setSelected(false);
        } else if (extendUpstreamExpression!=null && extendDownstreamExpression!=null) {
           firstExtendByCombobox.setSelectedItem(extendUpstreamExpression);
           secondExtendByCombobox.setSelectedItem(extendDownstreamExpression);            
           String direction1=(useRelativeOrientation)?"upstream":"before start";
           String direction2=(useRelativeOrientation)?"downstream":"after end";
           firstDirectionCombobox.setSelectedItem(direction1);    
           secondDirectionCombobox.setSelectedItem(direction2);     
           specifyBothDirectionsCheckBox.setSelected(true);
        }
        boolean both=specifyBothDirectionsCheckBox.isSelected();
        secondExtendByCombobox.setEnabled(both);
        secondDirectionCombobox.setEnabled(both);        
    
    }

    private void setUseRelativeOrientation(boolean useRelative) {
        String firstDirection=firstDirectionCombobox.getSelectedItem().toString();  
        String secondDirection="";
        if (useRelative) {
            if (firstDirection==null || firstDirection.isEmpty()) {
                firstDirection="";secondDirection="downstream";
            } else if (firstDirection.equals("upstream") || firstDirection.equals("before start")) {
                firstDirection="upstream";secondDirection="downstream";
            } else {
                firstDirection="downstream";secondDirection="upstream";
            }
            firstDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{"","upstream","downstream"}));
            //secondDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{secondDirection});          
            firstDirectionCombobox.setSelectedItem(firstDirection);
        } else {
            if (firstDirection==null || firstDirection.isEmpty()) {
                firstDirection=""; 
            } else if (firstDirection.equals("upstream") || firstDirection.equals("before start")) {
                firstDirection="before start"; 
            } else {
                firstDirection="after end";
            }
            firstDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{"","before start","after end"}));
            //secondDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{"before start","after end"}));          
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
        
        String firstSelectedDirection=firstDirectionCombobox.getSelectedItem().toString();
        if (firstSelectedDirection.isEmpty()) { // both directions same
            bothDirectionsExpressionParameter=(String)firstExtendByCombobox.getSelectedItem();
            if (bothDirectionsExpressionParameter==null || bothDirectionsExpressionParameter.trim().isEmpty()) bothDirectionsExpressionParameter="0";
        } else { // either different directions, or just one specified
            String firstExpression=(String)firstExtendByCombobox.getSelectedItem();
            if (firstExpression==null || firstExpression.trim().isEmpty()) firstExpression="0";
            boolean firstDirectionUpstream=(firstSelectedDirection.equals("upstream") || firstSelectedDirection.equals("before start"));
            if (firstDirectionUpstream) {
                upstreamExpressionParameter=firstExpression;            
            } else { // firstdirection==downstream
                downstreamExpressionParameter=firstExpression;                
            }
            if (specifyBothDirectionsCheckBox.isSelected()) { // two individual directions
                // second direction must be opposite of first
                String secondExpression=(String)secondExtendByCombobox.getSelectedItem();
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
        String firstDirection=(String)firstDirectionCombobox.getSelectedItem();
        boolean useRelativeOrientation=(firstDirection.equals("upstream") || firstDirection.equals("downstream"));
        parameters.setParameter(Operation_extend_sequences.EXTEND_EXPRESSION, bothDirectionsExpressionParameter);
        parameters.setParameter(Operation_extend_sequences.EXTEND_UPSTREAM_EXPRESSION, upstreamExpressionParameter);
        parameters.setParameter(Operation_extend_sequences.EXTEND_DOWNSTREAM_EXPRESSION, downstreamExpressionParameter);
        parameters.setParameter(Operation_extend_sequences.USE_RELATIVE_ORIENTATION,useRelativeOrientation); 
    }
    
    private JComboBox getNumericCombobox() {
        DefaultComboBoxModel model=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
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
