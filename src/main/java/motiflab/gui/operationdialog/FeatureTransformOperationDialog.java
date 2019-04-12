/*
 
 
 */

package motiflab.gui.operationdialog;


import java.awt.CardLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public abstract class FeatureTransformOperationDialog extends OperationDialog {
    public static final int CONDITION_NONE=0;
    public static final int CONDITION_NUMERIC=1;
    public static final int CONDITION_REGION=2;    
    private ConditionPanel conditionPanel=null;
    private JPanel sequenceSubsetPanel=null;
    private JPanel whereclausePanel=null;
    private JComboBox sequenceSubsetCombobox=null;
    private JComboBox whereCombobox=null;
    
    public FeatureTransformOperationDialog(java.awt.Frame parent) {
        super(parent);
    }
    
    public FeatureTransformOperationDialog() {
        super();
    }
    
    @Override
    protected void initComponents() {
        super.initComponents();
    }
    
    protected JPanel getWhereClausePanel(int conditionType) {
        if (whereclausePanel==null) initWhereClausePanel(conditionType);
        return whereclausePanel;
    }
    
    protected ConditionPanel getConditionPanel() {
        return conditionPanel;
    }
    
    protected JPanel getSequenceSubsetPanel() {
        if (sequenceSubsetPanel==null) initSequenceSubsetPanel();
        return sequenceSubsetPanel;
    }
    
    @Override
    protected void setParameters() {
        super.setParameters();
        if (sequenceSubsetPanel!=null && sequenceSubsetPanel.isVisible()) {
          String sequenceCollection=sequenceSubsetCombobox.getSelectedItem().toString();
          if (sequenceCollection.equals(engine.getDefaultSequenceCollectionName())) sequenceCollection=null;
          parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
        }
        if (whereclausePanel!=null && whereclausePanel.isVisible() && whereCombobox.getSelectedItem().equals("Where")) {
            Condition condition=conditionPanel.getCondition();
            parameters.setParameter("where", condition);
        } else parameters.setParameter("where", null);
    }
    

    private void initWhereClausePanel(int conditionType) {
       Condition condition=(Condition)parameters.getParameter("where");
       Boolean popupCondition=(Boolean)parameters.getParameter("_popup_where");
       if (popupCondition==null) popupCondition=false;
       final JPanel cardsPanel=new JPanel(new CardLayout());
       JPanel card[]=new JPanel[2];
       card[0]=new JPanel();     
       card[1]=new JPanel(new FlowLayout(FlowLayout.LEADING));
       whereclausePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
       whereCombobox=new JComboBox(new String[]{"","Where"});
       whereCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (whereCombobox.getSelectedItem().equals("Where")) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "useCondition");
                else ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "noCondition");                       
            }
        });
       if (conditionType==CONDITION_NUMERIC) conditionPanel=new ConditionPanel_position((Condition_position)condition, this, !popupCondition);
       else if (conditionType==CONDITION_REGION) conditionPanel=new ConditionPanel_region((Condition_region)condition, this, !popupCondition);       
       if (conditionPanel!=null) {
           card[1].add(conditionPanel);
           cardsPanel.add(card[0],"noCondition");
           cardsPanel.add(card[1],"useCondition");
           if (!popupCondition) {           
               whereclausePanel.add(new JLabel("Condition  "));
               whereclausePanel.add(whereCombobox);
           }
           whereclausePanel.add(cardsPanel);
       }
       if (condition!=null || popupCondition) whereCombobox.setSelectedItem("Where");
       else whereCombobox.setSelectedItem("");

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
