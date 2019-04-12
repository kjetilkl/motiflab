/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import motiflab.engine.data.*;
import motiflab.engine.operations.ArithmeticOperation;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Operation_threshold;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_threshold extends FeatureTransformOperationDialog {
    private JPanel thresholdpanel;
    private JPanel aboveBelowPanel;
    private JComboBox aboveCombobox;
    private JComboBox belowCombobox;
    private JComboBox cutoffCombobox;
    private JPanel whereclausePanel;
    private JComboBox whereCombobox;    
    private ConditionPanel_region regionConditionPanel;
    private ConditionPanel_position positionConditionPanel;
    private JPanel usedConditionPanel;
    private JPanel subsetPanel=null;
    private JPanel motifSubsetPanel=null;
    private JPanel moduleSubsetPanel=null;
    private JPanel sequenceSubsetPanel=null;
    private JComboBox motifSubsetCombobox=null;
    private JComboBox moduleSubsetCombobox=null;
    private JComboBox sequenceSubsetCombobox=null;  
    
    
    public OperationDialog_threshold(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_threshold() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        String sourceName=(String)sourceDataCombobox.getSelectedItem();
        if (sourceName!=null) sourceName=sourceName.trim();
        Class sourceType=getClassForDataItem(sourceName);        
        thresholdpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        thresholdpanel.setBorder(commonBorder);

        aboveCombobox = new JComboBox(getThresholdValuesModel(sourceType));
        aboveCombobox.setEditable(true);
        String aboveString=(String)parameters.getParameter(Operation_threshold.ABOVE_OR_EQUAL_STRING);
        if (aboveString==null) aboveString="dataset.max";
        aboveCombobox.setSelectedItem(aboveString);

        belowCombobox = new JComboBox(getThresholdValuesModel(sourceType));
        belowCombobox.setEditable(true);
        String belowString=(String)parameters.getParameter(Operation_threshold.BELOW_STRING);
        if (belowString==null) belowString="dataset.min";
        belowCombobox.setSelectedItem(belowString);

        cutoffCombobox = new JComboBox(getCutoffValuesModel(sourceType));
        cutoffCombobox.setEditable(true);
        String cutoffString=(String)parameters.getParameter(Operation_threshold.CUTOFF_THRESHOLD_STRING);
        if (cutoffString==null) cutoffString="50%";
        cutoffCombobox.setSelectedItem(cutoffString);

        aboveBelowPanel=new JPanel();
        aboveBelowPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.anchor=GridBagConstraints.BASELINE_LEADING;
        constraints.gridy=0; constraints.gridx=0;
        aboveBelowPanel.add(new JLabel("Set values above (or equal) to  "),constraints);
        constraints.gridy=0; constraints.gridx=1;
        aboveBelowPanel.add(aboveCombobox,constraints);
        constraints.gridy=1; constraints.gridx=0;
        aboveBelowPanel.add(new JLabel("Set values below to"),constraints);
        constraints.gridy=1; constraints.gridx=1;
        aboveBelowPanel.add(belowCombobox,constraints);

        thresholdpanel.add(new JLabel("Cutoff-threshold  "));
        thresholdpanel.add(cutoffCombobox);
        thresholdpanel.add(new JLabel("    "));
        thresholdpanel.add(aboveBelowPanel);

        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        initWhereClausePanels();
        whereclausePanel.setBorder(commonBorder);
        subsetPanel=getSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(thresholdpanel);
        add(whereclausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sourceName=(String)sourceDataCombobox.getSelectedItem();
                if (sourceName!=null) sourceName=sourceName.trim();
                else sourceName="";
                Class sourceType=getClassForDataItem(sourceName);
                setWhereClausePanelBasedOnSource(sourceType);
                setSubsetPanel(sourceType);
                belowCombobox.setModel(getThresholdValuesModel(sourceType));
                aboveCombobox.setModel(getThresholdValuesModel(sourceType));
                cutoffCombobox.setModel(getCutoffValuesModel(sourceType));
                setVisibilityOfRangePanel(sourceType==NumericDataset.class || sourceType==RegionDataset.class);
                pack();

            }
        });
        setWhereClausePanelBasedOnSource(sourceType);
        setSubsetPanel(sourceType);
        setVisibilityOfRangePanel(sourceType==NumericDataset.class || sourceType==RegionDataset.class);
        pack();          
    }
    
    private void setWhereClausePanelBasedOnSource(Class sourceType) {
        usedConditionPanel.removeAll();
        if (sourceType==NumericDataset.class) {
            usedConditionPanel.add(positionConditionPanel);
            whereclausePanel.setVisible(true);
        } else if (sourceType==RegionDataset.class) {
            usedConditionPanel.add(regionConditionPanel);
            whereclausePanel.setVisible(true);
        } else whereclausePanel.setVisible(false);
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
        parameters.setParameter(Operation_threshold.CUTOFF_THRESHOLD_STRING, (String)cutoffCombobox.getSelectedItem());
        parameters.setParameter(Operation_threshold.ABOVE_OR_EQUAL_STRING, (String)aboveCombobox.getSelectedItem());
        parameters.setParameter(Operation_threshold.BELOW_STRING, (String)belowCombobox.getSelectedItem());
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
        Class sourceType=getClassForDataItem(sourceName);
        String datacollectionname=null;        
        if (sourceType!=null) {
          if (sourceType==NumericDataset.class || sourceType==RegionDataset.class || sourceType==SequenceNumericMap.class || sourceType==ExpressionProfile.class) {
              datacollectionname=sequenceSubsetCombobox.getSelectedItem().toString();
              if (datacollectionname.equals(engine.getDefaultSequenceCollectionName())) datacollectionname=null;
          } else if (sourceType==MotifNumericMap.class) {
             datacollectionname=motifSubsetCombobox.getSelectedItem().toString();
             if (datacollectionname!=null && datacollectionname.isEmpty()) datacollectionname=null;
          } else if (sourceType==ModuleNumericMap.class) {
             datacollectionname=moduleSubsetCombobox.getSelectedItem().toString();
             if (datacollectionname!=null && datacollectionname.isEmpty()) datacollectionname=null;
          }
        }
        if (datacollectionname!=null) parameters.setParameter(ArithmeticOperation.DATA_COLLECTION_NAME, datacollectionname);        
    }
    
    protected JPanel getSubsetPanel() {
        if (subsetPanel==null) initSubsetPanel();
        return subsetPanel;
    }
      
    private void initMotifSubsetPanel() {
        motifSubsetPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        motifSubsetPanel.add(new JLabel("In motif collection  "));
        String selectedName=(String)parameters.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(new Class[]{MotifCollection.class},true);
        motifSubsetCombobox=new JComboBox(model);
        motifSubsetCombobox.setEditable(true);
        if (selectedName!=null) motifSubsetCombobox.setSelectedItem(selectedName);
        else motifSubsetCombobox.setSelectedIndex(0);
        motifSubsetPanel.add(motifSubsetCombobox);
    }

   private void initModuleSubsetPanel() {
        moduleSubsetPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        moduleSubsetPanel.add(new JLabel("In module collection  "));
        String selectedName=(String)parameters.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(new Class[]{ModuleCollection.class},true);
        moduleSubsetCombobox=new JComboBox(model);
        moduleSubsetCombobox.setEditable(true);
        if (selectedName!=null) moduleSubsetCombobox.setSelectedItem(selectedName);
        else moduleSubsetCombobox.setSelectedIndex(0);
        moduleSubsetPanel.add(moduleSubsetCombobox);
    }

    private void initSequenceSubsetPanel() {
        sequenceSubsetPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        sequenceSubsetPanel.add(new JLabel("In sequence collection  "));
        String selectedName=(String)parameters.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        if (selectedName==null) selectedName=engine.getDefaultSequenceCollectionName();
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(new Class[]{SequenceCollection.class});
        sequenceSubsetCombobox=new JComboBox(model);
        sequenceSubsetCombobox.setEditable(true);
        sequenceSubsetCombobox.setSelectedItem(selectedName);
        sequenceSubsetPanel.add(sequenceSubsetCombobox);
    }

    private void initSubsetPanel() {
        initSequenceSubsetPanel();
        initMotifSubsetPanel();
        initModuleSubsetPanel();
        subsetPanel=new JPanel();
        subsetPanel.setLayout(new CardLayout());
        subsetPanel.add(sequenceSubsetPanel,"SequenceCollection");
        subsetPanel.add(motifSubsetPanel,"MotifCollection");
        subsetPanel.add(moduleSubsetPanel,"ModuleCollection");
        subsetPanel.add(new JPanel(),"VOID");
    }

    private void setSubsetPanel(Class type) {
        if (type==NumericDataset.class || type==RegionDataset.class || type==SequenceNumericMap.class || type==ExpressionProfile.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "SequenceCollection");
        else if (type==MotifNumericMap.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "MotifCollection");
        else if (type==ModuleNumericMap.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "ModuleCollection");
        else ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "VOID");
    }    
    
    private DefaultComboBoxModel getThresholdValuesModel(Class type) {       
        DefaultComboBoxModel model=null;        
        if (type==NumericDataset.class || type==RegionDataset.class || type==ExpressionProfile.class) {
            model=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
            model.insertElementAt("sequence.max", 0);
            model.insertElementAt("sequence.min", 0);
            model.insertElementAt("collection.max", 0);
            model.insertElementAt("collection.min", 0);
            model.insertElementAt("dataset.max", 0);
            model.insertElementAt("dataset.min", 0);              
        } else if (type!=null && NumericMap.class.isAssignableFrom(type)) {
            model=getDataCandidates(new Class[]{NumericVariable.class,type});
            model.insertElementAt("collection.max", 0);
            model.insertElementAt("collection.min", 0);  
            model.insertElementAt("dataset.max", 0);
            model.insertElementAt("dataset.min", 0);             
        } else {
            model=getDataCandidates(new Class[]{NumericVariable.class});
        }      
        return model;
    }
    
    private DefaultComboBoxModel getCutoffValuesModel(Class type) {
        DefaultComboBoxModel model=null;        
        if (type==NumericDataset.class || type==RegionDataset.class || type==ExpressionProfile.class) {
            model=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class}); 
        } else {
            model=getDataCandidates(new Class[]{NumericVariable.class});
        }      
        return model;
    }    
    
}
