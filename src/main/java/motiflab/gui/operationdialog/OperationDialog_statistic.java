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
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Operation_statistic;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_statistic extends FeatureTransformOperationDialog {
    private JComboBox statisticFunctionCombobox;
    private JComboBox regionPropertyCombobox;    
    private JComboBox whereCombobox;
    private JPanel whereclausePanel;
    private ConditionPanel_region regionConditionPanel;
    private ConditionPanel_position positionConditionPanel;
    private JPanel usedConditionPanel;    
    private JPanel valuePanel;
    private DefaultComboBoxModel numericStatisticFunctions;
    private DefaultComboBoxModel regionStatisticFunctions;
    private DefaultComboBoxModel dnaStatisticFunctions;
    private JLabel strandLabel;
    private JComboBox strandCombobox;

    
    public OperationDialog_statistic(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_statistic() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        initWhereClausePanels();
        valuePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        valuePanel.setBorder(commonBorder);
        valuePanel.add(new JLabel("Statistic function   "));
        strandLabel=new JLabel("     Strand");
        strandCombobox=new JComboBox(new String[]{Operation_statistic.STRAND_DIRECT,Operation_statistic.STRAND_REVERSE,Operation_statistic.STRAND_GENE,Operation_statistic.STRAND_OPPOSITE});
        regionPropertyCombobox=new JComboBox(new String[]{"score"});
        regionPropertyCombobox.setEditable(true);
        numericStatisticFunctions=new DefaultComboBoxModel(Operation_statistic.getStatisticFunctions_NumericDataset());
        regionStatisticFunctions=new DefaultComboBoxModel(Operation_statistic.getStatisticFunctions_RegionDataset());
        dnaStatisticFunctions=new DefaultComboBoxModel(Operation_statistic.getStatisticFunctions_DNADataset());
        statisticFunctionCombobox=new JComboBox();
        ActionListener updateStatisticActionListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatisticFunction();
            }
        };
        statisticFunctionCombobox.addActionListener(updateStatisticActionListener);
        regionPropertyCombobox.addActionListener(updateStatisticActionListener);
        ActionListener[] list=sourceDataCombobox.getActionListeners();
        for (ActionListener listener:list) { // remove old listeners. I will make a new one instead
            sourceDataCombobox.removeActionListener(listener);
        }               
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sourcename=(String)sourceDataCombobox.getSelectedItem();
                updatePanelsForSource(sourcename);
                updateStatisticFunction();
            }
        });
        valuePanel.add(statisticFunctionCombobox);
        valuePanel.add(regionPropertyCombobox);        
        valuePanel.add(strandLabel);
        valuePanel.add(strandCombobox);
        whereclausePanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(valuePanel);
        add(whereclausePanel);
        add(getSequenceSubsetPanel());
        add(getOKCancelButtonsPanel());
        updatePanelsForSource((String)sourceDataCombobox.getSelectedItem());
        String propertyName=(String)parameters.getParameter(Operation_statistic.REGION_DATASET_PROPERTY);
        if (propertyName!=null && !propertyName.isEmpty()) regionPropertyCombobox.setSelectedItem(propertyName);         
        String statisticFunction=(String)parameters.getParameter(Operation_statistic.STATISTIC_FUNCTION);
        if (statisticFunction == null) statisticFunctionCombobox.setSelectedIndex(0);
        else statisticFunctionCombobox.setSelectedItem(statisticFunction);
        String strand=(String)parameters.getParameter(Operation_statistic.STRAND);
        if (strand==null) strandCombobox.setSelectedItem(Operation_statistic.STRAND_GENE);
        else strandCombobox.setSelectedItem(strand);
        String targetName=(String)parameters.getTargetDataName();
        if (targetName!=null && !targetName.isEmpty()) targetDataTextfield.setText(targetName);       
    }

    /** Sets proper condition and statistical functions depending on the class type of the source data
     */
    private void updatePanelsForSource(String sourceName) {
        Class sourcedataclass=null;
        if (sourceName!=null && !sourceName.isEmpty()) sourcedataclass=getClassForDataItem(sourceName);
        if (sourcedataclass==NumericDataset.class) {
            usedConditionPanel.removeAll();
            usedConditionPanel.add(positionConditionPanel);
            statisticFunctionCombobox.setModel(numericStatisticFunctions);
            regionPropertyCombobox.setVisible(false);            
            strandLabel.setVisible(false);
            strandCombobox.setVisible(false);
        } else if (sourcedataclass==RegionDataset.class) {
            usedConditionPanel.removeAll();
            usedConditionPanel.add(regionConditionPanel);
            statisticFunctionCombobox.setModel(regionStatisticFunctions);
            regionPropertyCombobox.setVisible(true); // default probably takes parameter
            strandLabel.setVisible(false);
            strandCombobox.setVisible(false);
        } else if (sourcedataclass==DNASequenceDataset.class) {
            usedConditionPanel.removeAll();
            usedConditionPanel.add(positionConditionPanel);
            statisticFunctionCombobox.setModel(dnaStatisticFunctions);
            strandLabel.setVisible(true);
            strandCombobox.setVisible(true);
            regionPropertyCombobox.setVisible(false);             
        } else { // this should not happen!
            usedConditionPanel.removeAll();
            strandLabel.setVisible(false);
            strandCombobox.setVisible(false);
            regionPropertyCombobox.setVisible(false);             
            statisticFunctionCombobox.setModel(new DefaultComboBoxModel(new String[]{" "}));
        }        
        pack();
    }


    private void updateStatisticFunction() {
        String statistic=(String)statisticFunctionCombobox.getSelectedItem();
        if (statistic==null || statistic.isEmpty() || statistic.equals(" ")) statistic="unknown";
        statistic=statistic.replace("-", "_");
        statistic=statistic.replace(" ", "_");
        String sourceName=(String)sourceDataCombobox.getSelectedItem();
        String propertyName=null;
        if (sourceName==null || sourceName.isEmpty()) sourceName="statistic";
        else {
           Class sourcedataclass=getClassForDataItem(sourceName);
           if (sourcedataclass==RegionDataset.class) {
               if (Operation_statistic.statisticTakesProperty(statistic)) {
                   regionPropertyCombobox.setVisible(true);
                   propertyName=(String)regionPropertyCombobox.getSelectedItem();
               } else regionPropertyCombobox.setVisible(false);
           }
        }
        String targetName=sourceName+"_"+statistic;
        if (propertyName!=null) {
            propertyName=propertyName.replace("-", "_");
            propertyName=propertyName.replace(" ", "_");           
            targetName+=("_"+propertyName);
        }
        targetDataTextfield.setText(gui.getGenericDataitemName(targetName, getDataTypeTable()));
        targetDataTextfield.setCaretPosition(0);
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
        String statisticFunction=(String)statisticFunctionCombobox.getSelectedItem();
        parameters.setParameter(Operation_statistic.STATISTIC_FUNCTION, statisticFunction);
        String dataitem=(String)sourceDataCombobox.getSelectedItem();
        Class dataitemclass=getClassForDataItem(dataitem);
        if (dataitemclass==DNASequenceDataset.class) parameters.setParameter(Operation_statistic.STRAND, (String)strandCombobox.getSelectedItem());
        else parameters.removeParameter(Operation_statistic.STRAND);
        if (dataitemclass==RegionDataset.class) {
            if (Operation_statistic.statisticTakesProperty(statisticFunction)) {
                String propertyName=(String)regionPropertyCombobox.getSelectedItem();
                if (propertyName==null || propertyName.isEmpty()) propertyName="score";
                parameters.setParameter(Operation_statistic.REGION_DATASET_PROPERTY, propertyName);
            } else parameters.removeParameter(Operation_statistic.REGION_DATASET_PROPERTY);
        } else parameters.removeParameter(Operation_statistic.REGION_DATASET_PROPERTY);
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, SequenceNumericMap.class);
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
          Class dataitemclass=getClassForDataItem(dataitem);
          if (dataitemclass==NumericDataset.class || dataitemclass==DNASequenceDataset.class) return true;
          else return false;
    }
    
}
