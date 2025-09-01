/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.ModuleTextMap;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.MotifTextMap;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequenceTextMap;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.operations.ArithmeticOperation;
import org.motiflab.engine.operations.Condition;
import org.motiflab.engine.operations.Condition_position;
import org.motiflab.engine.operations.Condition_region;

/**
 *
 * @author kjetikl
 */
public abstract class ArithmeticOperationDialog extends FeatureTransformOperationDialog {
    private JPanel operandPanel;
    private JComboBox operandCombobox;
    private JComboBox whereCombobox;
    private JPanel whereClausePanel;
    private ConditionPanel_region regionConditionPanel;
    private ConditionPanel_position positionConditionPanel;
    private JPanel usedConditionPanel;
    private JPanel subsetPanel=null;
    private DefaultComboBoxModel model_NumericDataset; // operand when source is NumericDataset
    private DefaultComboBoxModel model_SequenceNumericMap;  // operand when source is SequenceNumericMap
    private DefaultComboBoxModel model_MotifNumericMap;  // operand when source is MotifNumericMap
    private DefaultComboBoxModel model_ModuleNumericMap;  // operand when source is ModuleNumericMap
    private DefaultComboBoxModel model_SequenceTextMap;  // operand when source is SequenceTextMap
    private DefaultComboBoxModel model_MotifTextMap;  // operand when source is MotifTextMap
    private DefaultComboBoxModel model_ModuleTextMap;  // operand when source is ModuleTextMap
    private DefaultComboBoxModel model_NumericVariable;  // operand when source is NumericVariable
    private DefaultComboBoxModel model_RegionDataset;  // operand when source is RegionDataset
    private DefaultComboBoxModel model_ExpressionProfile;  // operand when source is ExpressionProfile
    private DefaultComboBoxModel model_Motif;  // operand when source is Motif
    private DefaultComboBoxModel model_Module;  // 
    private DefaultComboBoxModel model_Sequence;  //

    private JPanel motifSubsetPanel=null;
    private JPanel moduleSubsetPanel=null;
    private JPanel sequenceSubsetPanel=null;
    private JComboBox motifSubsetCombobox=null;
    private JComboBox moduleSubsetCombobox=null;
    private JComboBox sequenceSubsetCombobox=null;
    private JComboBox regionOperatorCombobox=null;
    private JComboBox propertyCombobox=null;
    private String[] operatorStrings=new String[]{"min","max","average","median","sum","weighted average","weighted sum","startValue","endValue","relativeStartValue","relativeEndValue","regionStartValue","regionEndValue","centerValue"};

    /**
     * Returns the text of the label which will be displayed in front of the operand combobox
     * @return
     */
    public abstract String getPreposition();
    public abstract String getOperationNameLabel();

    public ArithmeticOperationDialog(java.awt.Frame parent) {
        super(parent);
    }

    public ArithmeticOperationDialog() {
        super();
    }

    protected JPanel getSubsetPanel() {
        if (subsetPanel==null) initSubsetPanel();
        return subsetPanel;
    }

    @Override
    public void initComponents() {
        super.initComponents();
        String sourceName=(String)sourceDataCombobox.getSelectedItem();
        if (sourceName!=null) sourceName=sourceName.trim();
        Class sourceType=getClassForDataItem(sourceName);
        boolean usePropertyBox=(sourceType==RegionDataset.class || DataCollection.class.isAssignableFrom(sourceType) || sourceType==Sequence.class || sourceType==Motif.class || sourceType==ModuleCRM.class);                
        propertyCombobox=new JComboBox();
        operandPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        operandPanel.setBorder(commonBorder);
        operandPanel.add(new JLabel(getOperationNameLabel()+" "));
        operandPanel.add(propertyCombobox);
        operandPanel.add(new JLabel(" "+getPreposition()+" "));
        String operandString=(String)parameters.getParameter(ArithmeticOperation.OPERAND_STRING);
        if (operandString==null) operandString="1.0";
        String regionOperatorString=(String)parameters.getParameter(ArithmeticOperation.REGION_OPERATOR);
        if (regionOperatorString==null) regionOperatorString=operatorStrings[0];
        model_NumericDataset=getDataCandidates(new Class[]{NumericDataset.class, NumericVariable.class,SequenceNumericMap.class});
        model_RegionDataset=getDataCandidates(new Class[]{NumericDataset.class, NumericVariable.class,SequenceNumericMap.class,MotifNumericMap.class,ModuleNumericMap.class,SequenceTextMap.class,MotifTextMap.class,ModuleTextMap.class});
        model_RegionDataset.addElement("region[propertyname]");
        model_SequenceNumericMap=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        model_MotifNumericMap=getDataCandidates(new Class[]{NumericVariable.class,MotifNumericMap.class});
        model_ModuleNumericMap=getDataCandidates(new Class[]{NumericVariable.class,ModuleNumericMap.class});
        model_SequenceTextMap=getDataCandidates(new Class[]{TextVariable.class, SequenceTextMap.class, NumericVariable.class,SequenceNumericMap.class});
        model_MotifTextMap=getDataCandidates(new Class[]{TextVariable.class, MotifTextMap.class, NumericVariable.class,MotifNumericMap.class});
        model_ModuleTextMap=getDataCandidates(new Class[]{TextVariable.class, ModuleTextMap.class, NumericVariable.class,ModuleNumericMap.class});
        model_NumericVariable=getDataCandidates(new Class[]{NumericVariable.class});
        model_ExpressionProfile=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        model_Motif=getDataCandidates(new Class[]{NumericVariable.class, TextVariable.class, MotifNumericMap.class, MotifTextMap.class});
        model_Module=getDataCandidates(new Class[]{NumericVariable.class, TextVariable.class, ModuleNumericMap.class, ModuleTextMap.class});
        model_Sequence=getDataCandidates(new Class[]{NumericVariable.class, TextVariable.class, SequenceNumericMap.class, SequenceTextMap.class});
        regionOperatorCombobox=new JComboBox(operatorStrings);
        regionOperatorCombobox.setSelectedItem(regionOperatorString);
        operandCombobox = new JComboBox();
        operandCombobox.setEditable(true);
        operandPanel.add(regionOperatorCombobox);
        operandPanel.add(operandCombobox);
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        initWhereClausePanels();
        whereClausePanel.setBorder(commonBorder);
        subsetPanel=getSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(operandPanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sourceName=sourceDataCombobox.getSelectedItem().toString().trim();
                String operandString=((String)operandCombobox.getSelectedItem()).trim();
                Class sourceType=getClassForDataItem(sourceName);
                Class operandType=getClassForDataItem(operandString);
                setWhereClausePanelBasedOnSource(sourceType);
                setSubsetPanel(sourceType);
                setOperandComboboxModelBasedOnSource(sourceType);
                setVisibilityOfRangePanel(sourceType==NumericDataset.class || sourceType==RegionDataset.class);
                regionOperatorCombobox.setVisible(sourceType==RegionDataset.class && operandType==NumericDataset.class);
                boolean usePropertyBox=(sourceType==RegionDataset.class || DataCollection.class.isAssignableFrom(sourceType) || sourceType==Sequence.class || sourceType==Motif.class || sourceType==ModuleCRM.class);                
                propertyCombobox.setModel(getPropertiesCombobox(sourceType));
                propertyCombobox.setEditable(usePropertyBox);
                pack();

            }
        });
        operandCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object sourceObject=sourceDataCombobox.getSelectedItem();
                if (sourceObject==null) return;
                String sourceName=sourceObject.toString().trim();
                String operandString=operandCombobox.getSelectedItem().toString().trim();
                Class sourceType=getClassForDataItem(sourceName);
                Class operandType=getClassForDataItem(operandString);
                regionOperatorCombobox.setVisible(sourceType==RegionDataset.class && operandType==NumericDataset.class);
                boolean usePropertyBox=(sourceType==RegionDataset.class || DataCollection.class.isAssignableFrom(sourceType) || sourceType==Sequence.class || sourceType==Motif.class || sourceType==ModuleCRM.class);                
                Object currentSelectionObject=propertyCombobox.getSelectedItem();
                propertyCombobox.setModel(getPropertiesCombobox(sourceType));
                propertyCombobox.setEditable(usePropertyBox);
                propertyCombobox.setSelectedItem(currentSelectionObject); // stops the property from "resetting" when the operand is changed
                pack();

            }
        });
        Class operandType=getClassForDataItem(operandString);
        setWhereClausePanelBasedOnSource(sourceType);
        setSubsetPanel(sourceType);
        setOperandComboboxModelBasedOnSource(sourceType);
        operandCombobox.setSelectedItem(operandString);
        regionOperatorCombobox.setVisible(sourceType==RegionDataset.class && operandType==NumericDataset.class);
        propertyCombobox.setModel(getPropertiesCombobox(sourceType));
        propertyCombobox.setEditable(usePropertyBox);
        String propertyString=(String)parameters.getParameter(ArithmeticOperation.PROPERTY_NAME);
        if (propertyString!=null) propertyCombobox.setSelectedItem(propertyString);
        else propertyCombobox.setSelectedIndex(0);
        setVisibilityOfRangePanel(sourceType==NumericDataset.class || sourceType==RegionDataset.class);
        pack();
    }

    private void setWhereClausePanelBasedOnSource(Class sourceType) {
        usedConditionPanel.removeAll();
        if (sourceType==NumericDataset.class) {
            usedConditionPanel.add(positionConditionPanel);
            whereClausePanel.setVisible(true);
        } else if (sourceType==RegionDataset.class) {
            usedConditionPanel.add(regionConditionPanel);
            whereClausePanel.setVisible(true);
        } else whereClausePanel.setVisible(false);
    }

    private void setOperandComboboxModelBasedOnSource(Class sourceType) {
             if (sourceType==NumericDataset.class) operandCombobox.setModel(model_NumericDataset);
        else if (sourceType==RegionDataset.class) operandCombobox.setModel(model_RegionDataset);             
        else if (sourceType==NumericVariable.class) operandCombobox.setModel(model_NumericVariable);
        else if (sourceType==SequenceNumericMap.class) operandCombobox.setModel(model_SequenceNumericMap);
        else if (sourceType==MotifNumericMap.class) operandCombobox.setModel(model_MotifNumericMap);
        else if (sourceType==ModuleNumericMap.class) operandCombobox.setModel(model_ModuleNumericMap);
        else if (sourceType==SequenceTextMap.class) operandCombobox.setModel(model_SequenceTextMap);
        else if (sourceType==MotifTextMap.class) operandCombobox.setModel(model_MotifTextMap);
        else if (sourceType==ModuleTextMap.class) operandCombobox.setModel(model_ModuleTextMap);        
        else if (sourceType==ExpressionProfile.class) operandCombobox.setModel(model_ExpressionProfile);  
        else if (sourceType==Motif.class || sourceType==MotifCollection.class) operandCombobox.setModel(model_Motif);  
        else if (sourceType==ModuleCRM.class || sourceType==ModuleCollection.class) operandCombobox.setModel(model_Module);  
        else if (sourceType==Sequence.class || sourceType==SequenceCollection.class) operandCombobox.setModel(model_Sequence);  
        else operandCombobox.setModel(model_NumericVariable);              
    }

    
    
    @Override
    protected void setParameters() {
        super.setParameters();
        String operandString=operandCombobox.getSelectedItem().toString().trim();
        parameters.setParameter(ArithmeticOperation.OPERAND_STRING, operandString);
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        String sourceName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.SOURCE_NAME); // this should have been set in super.setParameters() above
        Class sourceType=getClassForDataItem(sourceName);
        Class operandType=getClassForDataItem(operandString);
        parameters.addAffectedDataObject(targetName, sourceType);
        if (whereCombobox.getSelectedItem().equals("Where")) {
            Condition condition=null;
            if (sourceType==NumericDataset.class) condition=positionConditionPanel.getCondition();
            else if (sourceType==RegionDataset.class) condition=regionConditionPanel.getCondition();
            parameters.setParameter("where", condition);
        } else parameters.setParameter("where", null);
        String datacollectionname=null;
        if (sourceType!=null) {
          if (sourceType==NumericDataset.class || sourceType==SequenceNumericMap.class || sourceType==SequenceTextMap.class || sourceType==RegionDataset.class || sourceType==ExpressionProfile.class) {
              datacollectionname=sequenceSubsetCombobox.getSelectedItem().toString();
              if (datacollectionname.equals(engine.getDefaultSequenceCollectionName())) datacollectionname=null;
          } else if (sourceType==MotifNumericMap.class || sourceType==MotifTextMap.class) {
             datacollectionname=motifSubsetCombobox.getSelectedItem().toString();
             if (datacollectionname!=null && datacollectionname.isEmpty()) datacollectionname=null;
          } else if (sourceType==ModuleNumericMap.class || sourceType==ModuleTextMap.class) {
             datacollectionname=moduleSubsetCombobox.getSelectedItem().toString();
             if (datacollectionname!=null && datacollectionname.isEmpty()) datacollectionname=null;
          }
        }
        if (sourceType==RegionDataset.class && operandType==NumericDataset.class) parameters.setParameter(ArithmeticOperation.REGION_OPERATOR, (String)regionOperatorCombobox.getSelectedItem());
        boolean usePropertyBox=(sourceType==RegionDataset.class || DataCollection.class.isAssignableFrom(sourceType) || sourceType==Sequence.class || sourceType==Motif.class || sourceType==ModuleCRM.class);                      
        if (usePropertyBox) {
            String property=((String)propertyCombobox.getSelectedItem()).trim();
            if (property.isEmpty()) property=null;
            parameters.setParameter(ArithmeticOperation.PROPERTY_NAME, property);
        } else parameters.setParameter(ArithmeticOperation.PROPERTY_NAME, null);
        if (datacollectionname!=null) parameters.setParameter(ArithmeticOperation.DATA_COLLECTION_NAME, datacollectionname);
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

    public void setSubsetPanel(Class type) {
        if (type==NumericDataset.class || type==SequenceNumericMap.class || type==SequenceTextMap.class || type==RegionDataset.class || type==ExpressionProfile.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "SequenceCollection");
        else if (type==MotifNumericMap.class || type==MotifTextMap.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "MotifCollection");
        else if (type==ModuleNumericMap.class || type==ModuleTextMap.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "ModuleCollection");
        else ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "VOID");
    }

    private void initWhereClausePanels() {
       Condition condition=(Condition)parameters.getParameter("where");
       final JPanel cardsPanel=new JPanel(new CardLayout());
       JPanel card[]=new JPanel[2];
       card[1]=new JPanel(new FlowLayout(FlowLayout.LEADING));
       whereClausePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
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
        String sourceitem=(String)sourceDataCombobox.getSelectedItem();
        if (getClassForDataItem(sourceitem)==NumericDataset.class) usedConditionPanel.add(positionConditionPanel);
        else usedConditionPanel.add(regionConditionPanel);
        cardsPanel.add(new JPanel(),"noCondition");
        cardsPanel.add(usedConditionPanel,"useCondition");
        whereClausePanel.add(new JLabel("Condition  "));
        whereClausePanel.add(whereCombobox);
        whereClausePanel.add(cardsPanel);

       if (condition!=null) whereCombobox.setSelectedItem("Where");
       else whereCombobox.setSelectedItem("");

    }
    
    private DefaultComboBoxModel getPropertiesCombobox(Class type) {
        if (type==RegionDataset.class) return new DefaultComboBoxModel(new String[]{"score","type"});
        else if (type==Sequence.class || type==SequenceCollection.class) {
            String[] props=Sequence.getAllEditableProperties(engine);
            return new DefaultComboBoxModel(props);
        } else if (type==Motif.class || type==MotifCollection.class) {
            String[] props=Motif.getAllEditableProperties(engine);
            return new DefaultComboBoxModel(props);
        } else if (type==ModuleCRM.class || type==ModuleCollection.class) {
            String[] props=ModuleCRM.getAllEditableProperties(engine);
            return new DefaultComboBoxModel(props);
        } else return new DefaultComboBoxModel(new String[]{"value"});
    }

}
