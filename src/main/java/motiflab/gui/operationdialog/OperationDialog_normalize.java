/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import motiflab.engine.data.*;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Operation_normalize;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_normalize extends FeatureTransformOperationDialog {
    private JPanel normalizeRangePanel;
    private JPanel sumToOneButtonPanel;
    private JPanel normalizeRangeButtonPanel;
    private JPanel oldrangepanel;
    private JPanel newrangepanel;    
    private JComboBox oldMinCombobox;
    private JComboBox oldMaxCombobox;
    private JComboBox newMinCombobox;
    private JComboBox newMaxCombobox;
    private JPanel whereclausePanel;
    private JComboBox whereCombobox;    
    private ConditionPanel_region regionConditionPanel;
    private ConditionPanel_position positionConditionPanel;
    private JPanel usedConditionPanel;
    private JRadioButton normalizeToRangeButton;
    private JRadioButton sumToOneButton;
    
    
    public OperationDialog_normalize(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_normalize() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        sumToOneButton=new JRadioButton("Normalize sequence sum to 1");
        normalizeToRangeButton=new JRadioButton("Normalize values to range");
        ButtonGroup exclusiveOptions=new ButtonGroup();
        exclusiveOptions.add(sumToOneButton);
        exclusiveOptions.add(normalizeToRangeButton);
        ChoiceListener choicelistener=new ChoiceListener();
        normalizeToRangeButton.addChangeListener(choicelistener);    
        normalizeRangePanel=new JPanel();
        sumToOneButtonPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        normalizeRangeButtonPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        normalizeRangePanel.setLayout(new BoxLayout(normalizeRangePanel, BoxLayout.Y_AXIS));
        oldrangepanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        oldrangepanel.setBorder(commonBorder);                
        oldrangepanel.add(new JLabel("            From old range:   min = "));
        String[] specialvalues=Operation_normalize.getSpecialValues();
        DefaultComboBoxModel oldMinModel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        for (int i=0;i<specialvalues.length;i++) {
            oldMinModel.insertElementAt(specialvalues[i], i);
        }
        oldMinCombobox = new JComboBox(oldMinModel);
        oldMinCombobox.setEditable(true);
        oldrangepanel.add(oldMinCombobox);                   
        String oldMinString=(String)parameters.getParameter(Operation_normalize.OLD_MIN);
        if (oldMinString==null) oldMinString="dataset.min";
        oldMinCombobox.setSelectedItem(oldMinString);
   
        oldrangepanel.add(new JLabel("     max = "));
        DefaultComboBoxModel oldMaxModel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        for (int i=0;i<specialvalues.length;i++) {
            oldMaxModel.insertElementAt(specialvalues[i], i);
        }
        oldMaxCombobox = new JComboBox(oldMaxModel);
        oldMaxCombobox.setEditable(true);
        oldrangepanel.add(oldMaxCombobox);                   
        String oldMaxString=(String)parameters.getParameter(Operation_normalize.OLD_MAX);
        if (oldMaxString==null) oldMaxString="dataset.max";
        oldMaxCombobox.setSelectedItem(oldMaxString);
          
        newrangepanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        newrangepanel.setBorder(commonBorder);                
        newrangepanel.add(new JLabel("            To new range:      min = "));
        DefaultComboBoxModel newMinModel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        for (int i=0;i<specialvalues.length;i++) {
            newMinModel.insertElementAt(specialvalues[i], i);
        }
        newMinCombobox = new JComboBox(newMinModel);
        newMinCombobox.setEditable(true);
        newrangepanel.add(newMinCombobox);                   
        String newMinString=(String)parameters.getParameter(Operation_normalize.NEW_MIN);
        if (newMinString==null) newMinString="0";
        newMinCombobox.setSelectedItem(newMinString);
   
        newrangepanel.add(new JLabel("     max = "));
        DefaultComboBoxModel newMaxModel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        for (int i=0;i<specialvalues.length;i++) {
            newMaxModel.insertElementAt(specialvalues[i], i);
        }
        newMaxCombobox = new JComboBox(newMaxModel);
        newMaxCombobox.setEditable(true);
        newrangepanel.add(newMaxCombobox);                   
        String newMaxString=(String)parameters.getParameter(Operation_normalize.NEW_MAX);
        if (newMaxString==null) newMaxString="1";
        newMaxCombobox.setSelectedItem(newMaxString);
        sumToOneButtonPanel.add(sumToOneButton);
        normalizeRangeButtonPanel.add(normalizeToRangeButton);
        normalizeRangePanel.add(sumToOneButtonPanel);
        normalizeRangePanel.add(normalizeRangeButtonPanel);
        normalizeRangePanel.add(oldrangepanel);
        normalizeRangePanel.add(newrangepanel);
        
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        getSourcePropertyCombobox().setModel(new DefaultComboBoxModel<String>( new String[]{"score"} ));
        String propertyName=(String)parameters.getParameter(Operation_normalize.PROPERTY_NAME);
        if (propertyName!=null && !propertyName.isEmpty()) getSourcePropertyCombobox().setSelectedItem(propertyName);
        
        initWhereClausePanels();
        whereclausePanel.setBorder(commonBorder);
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String dataitem=(String)sourceDataCombobox.getSelectedItem(); 
                if (getClassForDataItem(dataitem)==NumericDataset.class) {
                    usedConditionPanel.removeAll();
                    usedConditionPanel.add(positionConditionPanel);
                    setSourcePropertyVisible(false);                    
                }
                else if (getClassForDataItem(dataitem)==RegionDataset.class) {
                    usedConditionPanel.removeAll();
                    usedConditionPanel.add(regionConditionPanel);
                    setSourcePropertyVisible(true);
                } 
                pack();
            }
        });  
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(normalizeRangePanel);
        add(whereclausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();   
        String mode=(String)parameters.getParameter(Operation_normalize.MODE);
        if (mode==null) mode=Operation_normalize.SUM_TO_ONE;
        if (mode.equals(Operation_normalize.SUM_TO_ONE)) sumToOneButton.setSelected(true);
        else normalizeToRangeButton.setSelected(true);
        choicelistener.stateChanged(null); // this is just to update the panels 
        setSourcePropertyVisible(!isSourceNumeric());       
    }
    
    private void initWhereClausePanels() {
       Condition condition=(Condition)parameters.getParameter("where");       
       final JPanel cardsPanel=new JPanel(new CardLayout());
       JPanel card[]=new JPanel[2];     
       card[1]=new JPanel(new FlowLayout(FlowLayout.LEADING));
       whereclausePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
       whereCombobox=new JComboBox(new String[]{"","Where"});
       whereCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (whereCombobox.getSelectedItem().equals("Where")) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "useCondition");
                else ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "noCondition");                       
            }
        });
        if (condition == null) {
            positionConditionPanel=new ConditionPanel_position(null, this);
            regionConditionPanel=new ConditionPanel_region(null, this);   
        } else if (condition instanceof Condition_position) {
            positionConditionPanel=new ConditionPanel_position((Condition_position)condition, this);
            regionConditionPanel=new ConditionPanel_region(null, this);   
        } else {
            positionConditionPanel=new ConditionPanel_position(null, this);
            regionConditionPanel=new ConditionPanel_region((Condition_region)condition, this);   
        }
        usedConditionPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        if (isSourceNumeric()) usedConditionPanel.add(positionConditionPanel);
        else usedConditionPanel.add(regionConditionPanel);
        cardsPanel.add(new JPanel(),"noCondition");
        cardsPanel.add(usedConditionPanel,"useCondition");
        whereclausePanel.add(new JLabel("Condition  "));
        whereclausePanel.add(whereCombobox);
        whereclausePanel.add(cardsPanel);

       if (condition!=null) whereCombobox.setSelectedItem("Where");
       else whereCombobox.setSelectedItem("");

    }  
    
    private boolean isSourceNumeric() {
          String dataitem=(String)sourceDataCombobox.getSelectedItem(); 
          if (getClassForDataItem(dataitem)==NumericDataset.class) return true;   
          else return false;
    }
    
    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        if (sumToOneButton.isSelected()) parameters.setParameter(Operation_normalize.MODE,Operation_normalize.SUM_TO_ONE);
        else parameters.setParameter(Operation_normalize.MODE,Operation_normalize.NORMALIZE_TO_RANGE);
        parameters.setParameter(Operation_normalize.OLD_MIN, (String)oldMinCombobox.getSelectedItem());
        parameters.setParameter(Operation_normalize.OLD_MAX, (String)oldMaxCombobox.getSelectedItem());
        parameters.setParameter(Operation_normalize.NEW_MIN, (String)newMinCombobox.getSelectedItem());
        parameters.setParameter(Operation_normalize.NEW_MAX, (String)newMaxCombobox.getSelectedItem());
        if (whereCombobox.getSelectedItem().equals("Where")) {
            Condition condition;
            if (isSourceNumeric()) condition=positionConditionPanel.getCondition();
            else condition=regionConditionPanel.getCondition();
            parameters.setParameter("where", condition);
        } else parameters.setParameter("where", null);
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        String sourceName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.SOURCE_NAME); // this should have been set in super.setParameters() above
        Class oldclass=getClassForDataItem(sourceName);
        parameters.addAffectedDataObject(targetName, oldclass);
        if (oldclass==RegionDataset.class) {
            String propertyName=(String)getSourcePropertyCombobox().getSelectedItem();
            if (propertyName!=null) propertyName=propertyName.trim();
            parameters.setParameter(Operation_normalize.PROPERTY_NAME, propertyName);
        } else parameters.setParameter(Operation_normalize.PROPERTY_NAME, null);
    }
    
    
    private class ChoiceListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            boolean toRange=normalizeToRangeButton.isSelected();
            oldMinCombobox.setEnabled(toRange);
            oldMaxCombobox.setEnabled(toRange);
            newMinCombobox.setEnabled(toRange);
            newMaxCombobox.setEnabled(toRange);
            whereCombobox.setEnabled(toRange);
            positionConditionPanel.setEnabled(toRange);
            regionConditionPanel.setEnabled(toRange);
        }
        
    }
}
