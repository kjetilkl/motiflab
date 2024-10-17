/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import motiflab.engine.data.*;
import motiflab.engine.operations.Operation_transform;
import motiflab.engine.operations.ArithmeticOperation;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Condition_region;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_transform extends FeatureTransformOperationDialog {
    private JPanel transformpanel;
    private JComboBox transformCombobox;
    private JComboBox argumentCombobox;
    private JLabel argumentLabel;
    private JComboBox whereCombobox;
    private JPanel whereClausePanel;
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
    private JComboBox propertyCombobox=null;
    
    private DefaultComboBoxModel model_NumericDataset; // operand when source is NumericDataset
    private DefaultComboBoxModel model_SequenceNumericMap;  // operand when source is SequenceNumericMap
    private DefaultComboBoxModel model_MotifNumericMap;  // operand when source is MotifNumericMap
    private DefaultComboBoxModel model_ModuleNumericMap;  // operand when source is ModuleNumericMap
    private DefaultComboBoxModel model_NumericVariable;  // operand when source is NumericVariable
    private DefaultComboBoxModel model_RegionDataset;  // operand when source is RegionDataset
    private DefaultComboBoxModel model_ExpressionProfile;  // operand when source is ExpressionProfile  
    private DefaultComboBoxModel model_Motif;
    private DefaultComboBoxModel model_Module;
    private DefaultComboBoxModel model_Sequence;

    public OperationDialog_transform(JFrame parent) {
        super(parent);
    }

    public OperationDialog_transform() {
        super();
    }

    @Override
    public void initComponents() {
        super.initComponents();

        model_NumericDataset=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        model_RegionDataset=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class,MotifNumericMap.class,ModuleNumericMap.class});
        // model_RegionDataset.addElement("region[propertyname]");
        model_SequenceNumericMap=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        model_MotifNumericMap=getDataCandidates(new Class[]{NumericVariable.class,MotifNumericMap.class});
        model_ModuleNumericMap=getDataCandidates(new Class[]{NumericVariable.class,ModuleNumericMap.class});
        model_NumericVariable=getDataCandidates(new Class[]{NumericVariable.class});
        model_ExpressionProfile=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});   
        model_Motif=getDataCandidates(new Class[]{NumericVariable.class, MotifNumericMap.class});
        model_Module=getDataCandidates(new Class[]{NumericVariable.class, ModuleNumericMap.class});
        model_Sequence=getDataCandidates(new Class[]{NumericVariable.class, SequenceNumericMap.class});        
        
        String sourceName=(String)sourceDataCombobox.getSelectedItem();
        if (sourceName!=null) sourceName=sourceName.trim();
        Class sourceType=getClassForDataItem(sourceName);        
        transformpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));        
        transformpanel.setBorder(commonBorder);
        boolean usePropertyBox=(sourceType==RegionDataset.class || DataCollection.class.isAssignableFrom(sourceType) || sourceType==Sequence.class || sourceType==Motif.class || sourceType==ModuleCRM.class);                
        propertyCombobox=new JComboBox();        
        argumentLabel=new JLabel("   Argument ");
        String[] availableTransforms=(String[])Operation_transform.getAvailableTransforms().clone();
        Arrays.sort(availableTransforms);   
        DefaultComboBoxModel transformmodel=new DefaultComboBoxModel(availableTransforms);
        transformCombobox = new JComboBox(transformmodel);        
        argumentCombobox = new JComboBox(getArgumentModel(NumericVariable.class));
        argumentCombobox.setEditable(true);
        transformCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String sourceName=(String)sourceDataCombobox.getSelectedItem();
                if (sourceName!=null) sourceName=sourceName.trim();
                else sourceName="";
                Class sourceType=getClassForDataItem(sourceName);
                argumentCombobox.setModel(getArgumentModel(sourceType)); //
                pack();
            }
        });
        String transformName=(String)parameters.getParameter(Operation_transform.TRANSFORM_NAME);       
        if (transformName!=null) transformCombobox.setSelectedItem(transformName);
        else {transformCombobox.setSelectedIndex(0);transformName=(String)transformCombobox.getSelectedItem();}

        String argumentString=(String)parameters.getParameter(Operation_transform.TRANSFORM_ARGUMENT_STRING);
        if (Operation_transform.takesArgument(transformName)){
            if (argumentString==null) argumentString=""+Operation_transform.getDefaultArgument(transformName);
            argumentCombobox.setSelectedItem(argumentString);
        } else {
            argumentCombobox.setSelectedItem("");
            argumentCombobox.setVisible(false);
            argumentLabel.setVisible(false);
        }
        transformpanel.add(new JLabel("Transform "));
        transformpanel.add(propertyCombobox);
        transformpanel.add(new JLabel(" with  "));
        transformpanel.add(transformCombobox);        
        transformpanel.add(argumentLabel);
        transformpanel.add(argumentCombobox);
        transformCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String transform=(String)transformCombobox.getSelectedItem();
                if (Operation_transform.takesArgument(transform)) {
                   argumentCombobox.setSelectedItem(""+Operation_transform.getDefaultArgument(transform));
                   argumentCombobox.setVisible(true);
                   argumentLabel.setVisible(true);
                } else {
                   argumentCombobox.setSelectedItem("");
                   argumentCombobox.setVisible(false);
                   argumentLabel.setVisible(false);
                }
            }
        });
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        initWhereClausePanels();
        whereClausePanel.setBorder(commonBorder);
        subsetPanel=getSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(transformpanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sourceName=sourceDataCombobox.getSelectedItem().toString().trim();
                Class sourceType=getClassForDataItem(sourceName);
                argumentCombobox.setModel(getArgumentModel(sourceType));
                setWhereClausePanelBasedOnSource(sourceType);
                setSubsetPanel(sourceType);
                setVisibilityOfRangePanel(sourceType==NumericDataset.class || sourceType==RegionDataset.class);
                boolean usePropertyBox=(sourceType==RegionDataset.class || DataCollection.class.isAssignableFrom(sourceType) || sourceType==Sequence.class || sourceType==Motif.class || sourceType==ModuleCRM.class);                
                propertyCombobox.setModel(getPropertiesCombobox(sourceType));
                propertyCombobox.setEditable(usePropertyBox);                
                pack();

            }
        });
        argumentCombobox.setModel(getArgumentModel(sourceType));
        setWhereClausePanelBasedOnSource(sourceType);
        setSubsetPanel(sourceType);
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

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        parameters.setParameter(Operation_transform.TRANSFORM_NAME, (String)transformCombobox.getSelectedItem());
        String argumentString=(String)argumentCombobox.getSelectedItem();
        if (argumentString!=null && argumentString.trim().isEmpty()) argumentString=null;
        parameters.setParameter(Operation_transform.TRANSFORM_ARGUMENT_STRING, argumentString);
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        String sourceName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.SOURCE_NAME); // this should have been set in super.setParameters() above
        Class sourceType=getClassForDataItem(sourceName);
        if (whereCombobox.getSelectedItem().equals("Where")) {
            Condition condition=null;
            if (sourceType==NumericDataset.class) condition=positionConditionPanel.getCondition();
            else if (sourceType==RegionDataset.class) condition=regionConditionPanel.getCondition();
            parameters.setParameter("where", condition);
        } else parameters.setParameter("where", null);
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
        boolean usePropertyBox=(sourceType==RegionDataset.class || DataCollection.class.isAssignableFrom(sourceType) || sourceType==Sequence.class || sourceType==Motif.class || sourceType==ModuleCRM.class);                      
        if (usePropertyBox) {
            String property=((String)propertyCombobox.getSelectedItem()).trim();
            if (property.isEmpty()) property=null;
            parameters.setParameter(ArithmeticOperation.PROPERTY_NAME, property);
        } else parameters.setParameter(ArithmeticOperation.PROPERTY_NAME, null);    
        if (datacollectionname!=null) parameters.setParameter(ArithmeticOperation.DATA_COLLECTION_NAME, datacollectionname);
        parameters.addAffectedDataObject(targetName, sourceType);
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

    public void setSubsetPanel(Class type) {
        if (type==NumericDataset.class || type==RegionDataset.class || type==SequenceNumericMap.class || type==ExpressionProfile.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "SequenceCollection");
        else if (type==MotifNumericMap.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "MotifCollection");
        else if (type==ModuleNumericMap.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "ModuleCollection");
        else ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "VOID");
    }
    
    private DefaultComboBoxModel getArgumentModel(Class type) {
             if (type==NumericDataset.class) return model_NumericDataset;
        else if (type==ExpressionProfile.class) return model_ExpressionProfile;
        else if (type==SequenceNumericMap.class) return model_SequenceNumericMap;
        else if (type==MotifNumericMap.class) return model_MotifNumericMap;
        else if (type==ModuleNumericMap.class) return model_ModuleNumericMap;
        else if (type==NumericVariable.class) return model_NumericVariable;
        else if (type==Motif.class || type==MotifCollection.class) return model_Motif;
        else if (type==ModuleCRM.class || type==ModuleCollection.class) return model_Module;
        else if (type==Sequence.class || type==SequenceCollection.class) return model_Sequence;
        else if (type==RegionDataset.class) {
            String transformName=(String)transformCombobox.getSelectedItem();
            if (transformName.equals(Operation_transform.TYPE_REPLACE)) return getDataCandidates(new Class[]{TextVariable.class,MotifTextMap.class,ModuleTextMap.class});
            else return model_RegionDataset;
        }
        else return model_NumericVariable;        
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
