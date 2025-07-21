/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.operations.Condition_position;
import org.motiflab.engine.operations.Operation_extend;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_extend extends FeatureTransformOperationDialog {
    private JPanel extendPanel;
    private JPanel firstPanel;
    private JPanel secondPanel;
    private JPanel firstInternalPanel;
    private JPanel secondInternalPanel;
    private JComboBox firstDirectionCombobox;
    private JComboBox secondDirectionCombobox;
    private JComboBox firstOperatorCombobox;
    private JComboBox secondOperatorCombobox;
    private JComboBox firstExtendByCombobox;
    private JComboBox secondExtendByCombobox;
    private ConditionPanel_position firstConditionPanel;
    private ConditionPanel_position secondConditionPanel;
    
    
    
    public OperationDialog_extend(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_extend() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        extendPanel=new JPanel();
        extendPanel.setLayout(new BoxLayout(extendPanel, BoxLayout.Y_AXIS));
        extendPanel.setBorder(commonBorder);
        JLabel label=new JLabel("Extend   ");
        JLabel labelEmpty=new JLabel(" ");
        labelEmpty.setPreferredSize(label.getPreferredSize());
        labelEmpty.setMinimumSize(label.getMinimumSize());
        labelEmpty.setMaximumSize(label.getMaximumSize());
        label.setVerticalAlignment(JLabel.TOP);

        firstPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        secondPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        firstInternalPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        secondInternalPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        firstDirectionCombobox = new JComboBox(new String[]{"","upstream","downstream"});
        secondDirectionCombobox = new JComboBox(new String[]{"","upstream","downstream"});
        firstOperatorCombobox = new JComboBox(new String[]{"by","while","until"});
        secondOperatorCombobox = new JComboBox(new String[]{"by","while","until"});
        int minHeight=firstOperatorCombobox.getMinimumSize().height;
        Dimension dirComboSize=new Dimension(110,minHeight);
        Dimension operatorComboSize=new Dimension(70,minHeight);
        firstDirectionCombobox.setMinimumSize(dirComboSize);
        secondDirectionCombobox.setMinimumSize(dirComboSize);
        firstDirectionCombobox.setPreferredSize(dirComboSize);
        secondDirectionCombobox.setPreferredSize(dirComboSize);
        firstOperatorCombobox.setMinimumSize(operatorComboSize);
        secondOperatorCombobox.setMinimumSize(operatorComboSize);
        firstOperatorCombobox.setPreferredSize(operatorComboSize);
        secondOperatorCombobox.setPreferredSize(operatorComboSize);
        
        firstPanel.add(label);
        firstPanel.add(firstDirectionCombobox);
        firstInternalPanel.add(firstOperatorCombobox);
        secondPanel.add(labelEmpty);
        secondPanel.add(secondDirectionCombobox);
        secondInternalPanel.add(secondOperatorCombobox);
        secondPanel.add(secondInternalPanel);
        firstPanel.add(firstInternalPanel);

        final JPanel firstCardsPanel=new JPanel(new CardLayout());
        final JPanel secondCardsPanel=new JPanel(new CardLayout());
        JPanel firstCards[]=new JPanel[2];        
        JPanel secondCards[]=new JPanel[2];
        Condition_position firstCondition=null;
        Condition_position secondCondition=null;
        
        firstExtendByCombobox=getNumericCombobox();
        firstCards[0]=new JPanel(new FlowLayout(FlowLayout.LEADING));
        firstCards[0].setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        firstCards[0].setAlignmentY(JComponent.CENTER_ALIGNMENT);
        firstCards[0].add(firstExtendByCombobox);

         
        secondExtendByCombobox=getNumericCombobox();
        secondCards[0]=new JPanel(new FlowLayout(FlowLayout.LEADING));
        secondCards[0].setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        secondCards[0].setAlignmentY(JComponent.CENTER_ALIGNMENT);
        secondCards[0].add(secondExtendByCombobox);

        String extendBothOperator=(String)parameters.getParameter(Operation_extend.EXTEND_OPERATOR);
        String extendUpstreamOperator=(String)parameters.getParameter(Operation_extend.EXTEND_UPSTREAM_OPERATOR);
        String extendDownstreamOperator=(String)parameters.getParameter(Operation_extend.EXTEND_DOWNSTREAM_OPERATOR);
        if (extendBothOperator==null && extendUpstreamOperator==null && extendDownstreamOperator==null) extendBothOperator="by";

        if ((extendBothOperator!=null)) {
            firstOperatorCombobox.setSelectedItem(extendBothOperator);
            Object extendVal=parameters.getParameter(Operation_extend.EXTEND_EXPRESSION);
            if (extendVal==null) extendVal="10";
            if (extendVal instanceof Condition_position) {
              firstCondition=((Condition_position)extendVal).clone();
              secondCondition=((Condition_position)extendVal).clone();
            } else {
                firstExtendByCombobox.setSelectedItem(extendVal);
                secondExtendByCombobox.setSelectedItem(extendVal);
            }
        } else {
            Object extendUpstreamVal=parameters.getParameter(Operation_extend.EXTEND_UPSTREAM_EXPRESSION);
            Object extendDownstreamVal=parameters.getParameter(Operation_extend.EXTEND_DOWNSTREAM_EXPRESSION);
            if (extendUpstreamOperator!=null && extendDownstreamOperator!=null) {// both directions specified independently
                if (extendUpstreamVal instanceof Condition_position) firstCondition=((Condition_position)extendUpstreamVal).clone();
                else firstExtendByCombobox.setSelectedItem(extendUpstreamVal);
                if (extendDownstreamVal instanceof Condition_position) secondCondition=((Condition_position)extendDownstreamVal).clone();
                else secondExtendByCombobox.setSelectedItem(extendDownstreamVal);                                
            } else if (extendUpstreamOperator!=null) { // only upstream specified
                if (extendUpstreamVal instanceof Condition_position) firstCondition=((Condition_position)extendUpstreamVal).clone();
                else firstExtendByCombobox.setSelectedItem(extendUpstreamVal);             
            } else { // only downstream specified
                if (extendDownstreamVal instanceof Condition_position) firstCondition=((Condition_position)extendDownstreamVal).clone();
                else firstExtendByCombobox.setSelectedItem(extendDownstreamVal);              
            }
        }
        firstConditionPanel=new ConditionPanel_position(firstCondition, this);
        secondConditionPanel=new ConditionPanel_position(secondCondition, this);
        firstCards[1]=firstConditionPanel;
        secondCards[1]=secondConditionPanel;

        firstCardsPanel.add(firstCards[0],"By");
        firstCardsPanel.add(firstCards[1],"WhileUntil");
        secondCardsPanel.add(secondCards[0],"By");
        secondCardsPanel.add(secondCards[1],"WhileUntil");
        firstInternalPanel.add(firstCardsPanel);
        secondInternalPanel.add(secondCardsPanel);
        
        firstDirectionCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedDirection=firstDirectionCombobox.getSelectedItem().toString();
                if (selectedDirection.isEmpty()) secondPanel.setVisible(false);
                else {
                    String other="downstream";
                    if (selectedDirection.equals("downstream")) other="upstream";
                    secondDirectionCombobox.setModel(new DefaultComboBoxModel(new String[]{"",other}));
                    secondDirectionCombobox.setSelectedIndex(0);
                    secondPanel.setVisible(true);
                }
                pack();
            }
        });
        secondDirectionCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (secondDirectionCombobox.getSelectedItem().toString().isEmpty()) secondInternalPanel.setVisible(false);
                else secondInternalPanel.setVisible(true);
                pack();
            }
        });
        firstOperatorCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedOperator=(String)firstOperatorCombobox.getSelectedItem();
                //System.err.println("selectedOperator='"+selectedOperator+"'");
                     if (selectedOperator==null || selectedOperator.equals("by")) ((CardLayout)firstCardsPanel.getLayout()).show(firstCardsPanel, "By");
                else ((CardLayout)firstCardsPanel.getLayout()).show(firstCardsPanel, "WhileUntil");
                pack();
            }
        });
        secondOperatorCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedOperator=(String)secondOperatorCombobox.getSelectedItem();
                     if (selectedOperator==null || selectedOperator.equals("by")) ((CardLayout)secondCardsPanel.getLayout()).show(secondCardsPanel, "By");
                else ((CardLayout)secondCardsPanel.getLayout()).show(secondCardsPanel, "WhileUntil");
                pack();
            }
        });
        
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_REGION);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        extendPanel.add(firstPanel);
        extendPanel.add(secondPanel);
        add(extendPanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
        
        if ((extendBothOperator!=null) ||(extendUpstreamOperator==null && extendDownstreamOperator==null)) {
               //System.err.println("Initializing both directions same");
               firstDirectionCombobox.setSelectedItem("");
               firstOperatorCombobox.setSelectedItem(extendBothOperator);
        } else {
            if (extendUpstreamOperator!=null && extendDownstreamOperator!=null) {// both directions specified independently
                //System.err.println("Initializing both direction independently");
                firstDirectionCombobox.setSelectedItem("upstream");
                firstOperatorCombobox.setSelectedItem(extendUpstreamOperator);
                secondDirectionCombobox.setSelectedItem("downstream");
                secondOperatorCombobox.setSelectedItem(extendDownstreamOperator);                                             
            } else if (extendUpstreamOperator!=null) { // only upstream specified
                //System.err.println("Initializing upstream");
                firstDirectionCombobox.setSelectedItem("upstream");
                firstOperatorCombobox.setSelectedItem(extendUpstreamOperator);
                secondDirectionCombobox.setSelectedItem("");                
            } else { // only downstream specified
                //System.err.println("Initializing downstream="+extendDownstreamOperator);
                firstDirectionCombobox.setSelectedItem("downstream");
                firstOperatorCombobox.setSelectedItem(extendDownstreamOperator);
                secondDirectionCombobox.setSelectedItem("");                 
            }
        }       
    
    }

    
    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        String bothDirectionsOperatorParameter=null;
        String upstreamOperatorParameter=null;
        String downstreamOperatorParameter=null;
        Object bothDirectionsExpressionParameter=null;
        Object upstreamExpressionParameter=null;
        Object downstreamExpressionParameter=null;
        String firstSelectedDirection=firstDirectionCombobox.getSelectedItem().toString();
        if (firstSelectedDirection.isEmpty()) { // both directions same
            bothDirectionsOperatorParameter=(String)firstOperatorCombobox.getSelectedItem();
            if (bothDirectionsOperatorParameter.equals("by")) {
                bothDirectionsExpressionParameter=(String)firstExtendByCombobox.getSelectedItem();
                if (bothDirectionsExpressionParameter==null || bothDirectionsExpressionParameter.toString().trim().isEmpty()) bothDirectionsExpressionParameter="0";
            } else bothDirectionsExpressionParameter=firstConditionPanel.getCondition();
        } else { // different directions, or just one specified
            String firstOperator=(String)firstOperatorCombobox.getSelectedItem();
            if (firstSelectedDirection.equals("upstream")) { // firstdirection==upstream
                upstreamOperatorParameter=firstOperator;
                if (upstreamOperatorParameter.equals("by")) {
                    upstreamExpressionParameter=(String)firstExtendByCombobox.getSelectedItem();
                    if (upstreamExpressionParameter==null || upstreamExpressionParameter.toString().trim().isEmpty()) upstreamExpressionParameter="0";
                } else upstreamExpressionParameter=firstConditionPanel.getCondition();               
            } else { // firstdirection==downstream
                downstreamOperatorParameter=firstOperator;
                if (downstreamOperatorParameter.equals("by")) {
                    downstreamExpressionParameter=(String)firstExtendByCombobox.getSelectedItem();
                    if (downstreamExpressionParameter==null || downstreamExpressionParameter.toString().trim().isEmpty()) downstreamExpressionParameter="0";
                } else downstreamExpressionParameter=firstConditionPanel.getCondition();                               
            }
            String secondSelectedDirection=(String)secondDirectionCombobox.getSelectedItem();
            if (secondSelectedDirection!=null && !secondSelectedDirection.equals("")) {
                String secondOperator=(String)secondOperatorCombobox.getSelectedItem();
                if (secondSelectedDirection.equals("upstream")) { // seconddirection==upstream
                    upstreamOperatorParameter=secondOperator;
                    if (upstreamOperatorParameter.equals("by")) {
                        upstreamExpressionParameter=(String)secondExtendByCombobox.getSelectedItem();
                        if (upstreamExpressionParameter==null || upstreamExpressionParameter.toString().trim().isEmpty()) upstreamExpressionParameter="0";
                    } else upstreamExpressionParameter=secondConditionPanel.getCondition();               
                } else { // seconddirection==downstream
                    downstreamOperatorParameter=secondOperator;
                    if (downstreamOperatorParameter.equals("by")) {
                        downstreamExpressionParameter=(String)secondExtendByCombobox.getSelectedItem();
                        if (downstreamExpressionParameter==null || downstreamExpressionParameter.toString().trim().isEmpty()) downstreamExpressionParameter="0";
                    } else downstreamExpressionParameter=secondConditionPanel.getCondition();                               
                }                                                
            }                                   
        }
        parameters.setParameter(Operation_extend.EXTEND_OPERATOR, bothDirectionsOperatorParameter);
        parameters.setParameter(Operation_extend.EXTEND_EXPRESSION, bothDirectionsExpressionParameter);
        parameters.setParameter(Operation_extend.EXTEND_UPSTREAM_OPERATOR, upstreamOperatorParameter);
        parameters.setParameter(Operation_extend.EXTEND_UPSTREAM_EXPRESSION, upstreamExpressionParameter);
        parameters.setParameter(Operation_extend.EXTEND_DOWNSTREAM_OPERATOR, downstreamOperatorParameter);
        parameters.setParameter(Operation_extend.EXTEND_DOWNSTREAM_EXPRESSION, downstreamExpressionParameter);
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, RegionDataset.class);
    }
    
    private JComboBox getNumericCombobox() {
        DefaultComboBoxModel model=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class,MotifNumericMap.class,ModuleNumericMap.class});
        JComboBox combobox = new JComboBox(model);
        combobox.setEditable(true);  
        return combobox;
    }    
}
