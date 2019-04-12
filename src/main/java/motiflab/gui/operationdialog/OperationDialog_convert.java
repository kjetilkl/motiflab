/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Operation_convert;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_convert extends FeatureTransformOperationDialog {
    private JComboBox regionValueCombobox;
    private JComboBox regionOperatorCombobox;
    private JComboBox numericdataValueCombobox;
    private JComboBox whereCombobox;
    private JPanel whereclausePanel;
    private ConditionPanel_region regionConditionPanel;
    private ConditionPanel_position positionConditionPanel;
    private JPanel regionValuePanel;
    private JPanel numericdataValuePanel;
    private JPanel usedConditionPanel;
    private JPanel valuePanel;
    private String[] operatorStrings=new String[]{"min","max","average","median","sum"};    
    
    public OperationDialog_convert(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_convert() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        initWhereClausePanels();
        initValuePanels();
        whereclausePanel.setBorder(commonBorder);
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String dataitem=(String)sourceDataCombobox.getSelectedItem(); 
                if (getClassForDataItem(dataitem)==NumericDataset.class) {
                    usedConditionPanel.removeAll();
                    usedConditionPanel.add(positionConditionPanel);
                    valuePanel.removeAll();
                    valuePanel.add(regionValuePanel);
                    setTitle("  convert to region dataset");
                }
                else if (getClassForDataItem(dataitem)==RegionDataset.class) {
                    usedConditionPanel.removeAll();
                    usedConditionPanel.add(regionConditionPanel);
                    valuePanel.removeAll();
                    valuePanel.add(numericdataValuePanel);
                    setTitle("  convert to numeric dataset");
                } 
                pack();
            }
        });
        add(sourceTargetPanel);
        add(valuePanel);
        add(whereclausePanel);
        add(getOKCancelButtonsPanel());
        if (isSourceNumeric()) setTitle("  convert to region dataset");
        else setTitle("  convert to numeric dataset");
        pack();        
    }
    

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        //parameters.setParameter(Operation_weight.WEIGHT_STRING, (String)weightbyCombobox.getSelectedItem());
        if (whereCombobox.getSelectedItem().equals("Where")) {
            Condition condition;
            if (isSourceNumeric()) condition=positionConditionPanel.getCondition();
            else condition=regionConditionPanel.getCondition();
            parameters.setParameter("where", condition);
        } else parameters.setParameter("where", null);
        String targetName=(String)parameters.getParameter(OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        if (isSourceNumeric()) { // convert numeric->region
            if (isOperatorNumeric()) parameters.setParameter(Operation_convert.NEW_VALUE_OPERATOR,regionOperatorCombobox.getSelectedItem());
            else parameters.setParameter(Operation_convert.NEW_VALUE_OPERATOR,null);
            parameters.setParameter(Operation_convert.NEW_VALUE_STRING,regionValueCombobox.getSelectedItem());
            parameters.setParameter(Operation_convert.TARGET_TYPE,Operation_convert.REGION);
            parameters.addAffectedDataObject(targetName, RegionDataset.class);
        } else {  // convert  region->numeric
            parameters.setParameter(Operation_convert.NEW_VALUE_OPERATOR,null);
            parameters.setParameter(Operation_convert.NEW_VALUE_STRING,numericdataValueCombobox.getSelectedItem());
            parameters.setParameter(Operation_convert.TARGET_TYPE,Operation_convert.NUMERIC);
            parameters.addAffectedDataObject(targetName, NumericDataset.class);
        }
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
        if (isSourceNumeric() && condition==null) condition=getDefaultNumericCondition();
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
    
    private Condition_position getDefaultNumericCondition() {
       String dataitem=(String)sourceDataCombobox.getSelectedItem();
       Condition_position condition = new Condition_position();
       condition.setOperandAString(dataitem);
       condition.setComparator(">");
       condition.setOperandBString("0");
       return condition;
    }
 
    private void initValuePanels() {
       String newValue=(String)parameters.getParameter(Operation_convert.NEW_VALUE_STRING);
       String newValueOperator=(String)parameters.getParameter(Operation_convert.NEW_VALUE_OPERATOR);
       numericdataValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
       numericdataValuePanel.setBorder(commonBorder);
       String newNumericTrackValue=newValue;
       if (newNumericTrackValue==null) newNumericTrackValue="1.0";
       DefaultComboBoxModel wbmodel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class,NumericDataset.class});
       wbmodel.addElement("region.count"); 
       wbmodel.addElement("region.sumscore");
       wbmodel.addElement("region.highestscore");
       wbmodel.addElement("region.length");          
       numericdataValueCombobox = new JComboBox(wbmodel);
       numericdataValueCombobox.setEditable(true);
       numericdataValueCombobox.setSelectedItem(newNumericTrackValue);       
       numericdataValuePanel.add(new JLabel("Value = "));
       numericdataValuePanel.add(numericdataValueCombobox);

       regionValuePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
       regionValuePanel.setBorder(commonBorder);
       String newRegionValue=newValue;
       if (newRegionValue==null) newRegionValue="1.0";
       DefaultComboBoxModel regmodel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class,NumericDataset.class});      
       regionValueCombobox = new JComboBox(regmodel);
       regionValueCombobox.setEditable(true);
       regionOperatorCombobox = new JComboBox(operatorStrings);
       regionValuePanel.add(new JLabel("Region.score = "));
       regionValuePanel.add(regionOperatorCombobox);
       regionValuePanel.add(regionValueCombobox);
       regionValueCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isOperatorNumeric()) regionOperatorCombobox.setEnabled(true);
                else regionOperatorCombobox.setEnabled(false);
            }
        });        
       regionValueCombobox.setSelectedItem(newRegionValue);   
       if (newValueOperator!=null) regionOperatorCombobox.setSelectedItem(newValueOperator);
       valuePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
       if (isSourceNumeric()) valuePanel.add(regionValuePanel);
       else valuePanel.add(numericdataValuePanel);
    }    
     
    
    private boolean isSourceNumeric() {
          String dataitem=(String)sourceDataCombobox.getSelectedItem(); 
          if (getClassForDataItem(dataitem)==NumericDataset.class) return true;   
          else return false;
    }
    
    private boolean isOperatorNumeric() {
          String dataitem=(String)regionValueCombobox.getSelectedItem(); 
          if (getClassForDataItem(dataitem)==NumericDataset.class) return true;   
          else return false;
    }
}
