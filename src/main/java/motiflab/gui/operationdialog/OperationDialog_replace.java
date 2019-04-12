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
import javax.swing.JTextField;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Operation_replace;
import motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_replace extends FeatureTransformOperationDialog {
    private JPanel replacePanel;

    private JComboBox whereCombobox;
    private JPanel whereClausePanel;
    private ConditionPanel_region regionConditionPanel;

    private JPanel usedConditionPanel;
    private JPanel subsetPanel=null;
    private JPanel sequenceSubsetPanel=null;
    private JPanel propertyPanel=null;
    private JPanel propertyPanel2=null;    
    private JPanel propertyPanel3=null;      
    private JPanel modePanel=null;
    private JPanel regexPanel=null;
    private JPanel variablePanel=null;  
    private JPanel mapPanel=null;      
    private JPanel insertPanel=null;    
    private JComboBox sequenceSubsetCombobox=null;
    private JComboBox propertyCombobox=null;
    private JComboBox propertyCombobox2=null;  
    private JComboBox propertyCombobox3=null;     
    private JComboBox modeCombobox=null;
    
    private JTextField searchExpressionTextField=null;
    private JTextField replaceExpressionTextField=null;
    private JTextField insertExpressionTextField=null;
    private JComboBox searchDataCombobox=null;
    private JComboBox replaceDataCombobox=null;
    private JComboBox searchMapCombobox=null;    
    
    private DefaultComboBoxModel mode_TextVariable;  // operand when source is TextVariable
    private DefaultComboBoxModel mode_TextVariable_or_RegionDataset;  // operand when source is TextVariable or RegionDataset  

    private DefaultComboBoxModel searchDataComboboxModel;
    private DefaultComboBoxModel replaceDataComboboxModel;
    private DefaultComboBoxModel searchMapComboboxModel;    
    
    public OperationDialog_replace(JFrame parent) {
        super(parent);
    }

    public OperationDialog_replace() {
        super();
    }

    @Override
    public void initComponents() {
        super.initComponents();
        mode_TextVariable=new DefaultComboBoxModel<>(new String[]{"expression","map","beginning","end","macros"});
        mode_TextVariable_or_RegionDataset=new DefaultComboBoxModel<>(new String[]{"expression","variable","map"});   
        searchDataComboboxModel=getDataCandidates(TextVariable.class);
        replaceDataComboboxModel=getDataCandidates(TextVariable.class);
        searchMapComboboxModel=getDataCandidates(DataMap.class);        
        searchDataCombobox=new JComboBox(searchDataComboboxModel);
        replaceDataCombobox=new JComboBox(replaceDataComboboxModel);
        searchMapCombobox=new JComboBox(searchMapComboboxModel);        
        
        replacePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));        
        replacePanel.setBorder(commonBorder);     
        String sourceName=(String)sourceDataCombobox.getSelectedItem();
        if (sourceName!=null) sourceName=sourceName.trim();
        Class sourceType=(sourceName!=null)?getClassForDataItem(sourceName):null;        
        modeCombobox=new JComboBox<>();
        if (sourceType==TextVariable.class) modeCombobox.setModel(mode_TextVariable);
        else modeCombobox.setModel(mode_TextVariable_or_RegionDataset);
        modeCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String mode=(String)modeCombobox.getSelectedItem();
                if (mode.equals("expression")) {
                   ((CardLayout)modePanel.getLayout()).show(modePanel, "regexPanel");
                    String sourceName=(String)sourceDataCombobox.getSelectedItem();
                    Class sourceType=(sourceName!=null)?getClassForDataItem(sourceName):null;
                    setWhereClausePanelBasedOnSource(sourceType);
                    setSubsetPanel(sourceType);
                    setVisibilityOfRangePanel(sourceType==RegionDataset.class);                          
                    propertyPanel.setVisible(sourceType==RegionDataset.class);                        
                } else if (mode.equals("variable")) {
                   ((CardLayout)modePanel.getLayout()).show(modePanel, "variablePanel");
                    String sourceName=(String)sourceDataCombobox.getSelectedItem();
                    Class sourceType=(sourceName!=null)?getClassForDataItem(sourceName):null;
                    setWhereClausePanelBasedOnSource(sourceType);
                    setSubsetPanel(sourceType);
                    setVisibilityOfRangePanel(sourceType==RegionDataset.class);                          
                    propertyPanel2.setVisible(sourceType==RegionDataset.class);                        
                } else if (mode.equals("map")) {
                   ((CardLayout)modePanel.getLayout()).show(modePanel, "mapPanel");
                    String sourceName=(String)sourceDataCombobox.getSelectedItem();
                    Class sourceType=(sourceName!=null)?getClassForDataItem(sourceName):null;
                    setWhereClausePanelBasedOnSource(sourceType);
                    setSubsetPanel(sourceType);
                    setVisibilityOfRangePanel(sourceType==RegionDataset.class);                          
                    propertyPanel3.setVisible(sourceType==RegionDataset.class);                        
                } else if (mode.equals("beginning")|| mode.equals("end")) {
                   ((CardLayout)modePanel.getLayout()).show(modePanel, "insertPanel");
                    setWhereClausePanelBasedOnSource(TextVariable.class);
                    setSubsetPanel(TextVariable.class);
                    setVisibilityOfRangePanel(false);                         
                } else {
                   ((CardLayout)modePanel.getLayout()).show(modePanel, "macrosPanel");
                    setWhereClausePanelBasedOnSource(TextVariable.class);
                    setSubsetPanel(TextVariable.class);
                    setVisibilityOfRangePanel(false);                    
                }
                pack();
            }
        });

        propertyCombobox=new JComboBox(new String[]{"type"});
        propertyCombobox.setEditable(true);
        propertyPanel = new JPanel();        
        propertyPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        propertyPanel.add(new JLabel(" for property "));
        propertyPanel.add(propertyCombobox);
        
        regexPanel=new JPanel();
        regexPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        regexPanel.setFocusCycleRoot(true); // quick fix to problem of 'illogical' focus traversal. Now, pressing TAB in the search expression textfield will move focus to replace textfield (and vice versa)
        searchExpressionTextField=new JTextField(10);
        replaceExpressionTextField=new JTextField(10);
        regexPanel.add(searchExpressionTextField);
        regexPanel.add(new JLabel(" with "));
        regexPanel.add(replaceExpressionTextField);
        regexPanel.add(propertyPanel);   
        
        propertyCombobox2=new JComboBox(new String[]{"type"});
        propertyCombobox2.setEditable(true);
        propertyPanel2 = new JPanel();        
        propertyPanel2.setLayout(new FlowLayout(FlowLayout.LEFT));
        propertyPanel2.add(new JLabel(" for property "));
        propertyPanel2.add(propertyCombobox2);
        
        variablePanel=new JPanel();
        variablePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        variablePanel.add(searchDataCombobox);
        variablePanel.add(new JLabel(" with "));
        variablePanel.add(replaceDataCombobox);
        variablePanel.add(propertyPanel2);     
        
        propertyCombobox3=new JComboBox(new String[]{"type"});
        propertyCombobox3.setEditable(true);
        propertyPanel3 = new JPanel();        
        propertyPanel3.setLayout(new FlowLayout(FlowLayout.LEFT));
        propertyPanel3.add(new JLabel(" for property "));
        propertyPanel3.add(propertyCombobox3);
        
        mapPanel=new JPanel();
        mapPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        mapPanel.add(searchMapCombobox);
        mapPanel.add(propertyPanel3);  
        
        
        insertPanel=new JPanel();
        insertPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        insertPanel.setFocusCycleRoot(true); // quick fix to problem of 'illogical' focus traversal. Now, pressing TAB in the search expression textfield will move focus to replace textfield (and vice versa)
        insertExpressionTextField=new JTextField(10);
        insertPanel.add(new JLabel(" with "));
        insertPanel.add(insertExpressionTextField);        
        
        modePanel=new JPanel();
        modePanel.setLayout(new CardLayout());
        modePanel.add(regexPanel,"regexPanel");
        modePanel.add(variablePanel,"variablePanel");      
        modePanel.add(mapPanel,"mapPanel");           
        modePanel.add(insertPanel,"insertPanel");        
        modePanel.add(new JPanel(),"macrosPanel");          
        
        String searchExpressionString=(String)parameters.getParameter(Operation_replace.SEARCH_PATTERN); 
        String replaceExpressionString=(String)parameters.getParameter(Operation_replace.REPLACE_PATTERN);
                     
        if (searchExpressionString!=null) {
            searchExpressionString=MotifLabEngine.escapeQuotedString(searchExpressionString); // escapes TABs and newlines so they fit on one line
            searchExpressionTextField.setText(searchExpressionString); 
            searchExpressionTextField.setCaretPosition(0);
            replaceDataCombobox.setSelectedItem(searchExpressionString);
        }
        if (replaceExpressionString!=null) {
            replaceExpressionString=MotifLabEngine.escapeQuotedString(replaceExpressionString); // escapes TABs and newlines so they fit on one line            
            replaceExpressionTextField.setText(replaceExpressionString); 
            replaceExpressionTextField.setCaretPosition(0);
            insertExpressionTextField.setText(replaceExpressionString); 
            insertExpressionTextField.setCaretPosition(0);     
            replaceDataCombobox.setSelectedItem(replaceExpressionString);
        }
                           
        replacePanel.add(new JLabel("Replace "));
        replacePanel.add(modeCombobox);
        replacePanel.add(modePanel);

        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        initWhereClausePanels();
        whereClausePanel.setBorder(commonBorder);
        subsetPanel=getSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(replacePanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String mode=(String)modeCombobox.getSelectedItem(); // current selection              
                String sourceName=sourceDataCombobox.getSelectedItem().toString().trim();
                Class sourceType=getClassForDataItem(sourceName);
                if (sourceType==TextVariable.class) modeCombobox.setModel(mode_TextVariable);
                else modeCombobox.setModel(mode_TextVariable_or_RegionDataset);                  
                if (sourceType!=TextVariable.class && !(mode.equals("expression") || mode.equals("variable") || mode.equals("map"))) {
                    modeCombobox.setSelectedItem("expression"); // macros/beinning/end are not valid choices for other types than Text Variable                
                } else modeCombobox.setSelectedItem(mode);
                // note that changing the mode will also update other options
            }
        });
        // initialize
        Boolean useMacros=(Boolean)parameters.getParameter(Operation_replace.EXPAND_MACROS);
        Boolean insertBefore=(Boolean)parameters.getParameter(Operation_replace.INSERT_BEFORE);
        Boolean insertAfter=(Boolean)parameters.getParameter(Operation_replace.INSERT_AFTER);  
        Boolean expressionFromTextVariable=(Boolean)parameters.getParameter(Operation_replace.EXPRESSIONS_FROM_TEXTVARIABLE);   
        Boolean expressionFromMap=(Boolean)parameters.getParameter(Operation_replace.EXPRESSIONS_FROM_MAP);         
        if (useMacros!=null && useMacros.booleanValue()) modeCombobox.setSelectedItem("macros");
        else if (insertBefore!=null && insertBefore.booleanValue()) modeCombobox.setSelectedItem("beginning");
        else if (insertAfter!=null && insertAfter.booleanValue()) modeCombobox.setSelectedItem("end");
        else if (expressionFromTextVariable!=null && expressionFromTextVariable.booleanValue()) modeCombobox.setSelectedItem("variable");
        else if (expressionFromMap!=null && expressionFromMap.booleanValue()) modeCombobox.setSelectedItem("map");
        else modeCombobox.setSelectedItem("expression");        
        
        String propertyString=(String)parameters.getParameter(Operation_replace.REGION_PROPERTY);     
        if (propertyString!=null) {
            propertyCombobox.setSelectedItem(propertyString);
            propertyCombobox2.setSelectedItem(propertyString);
        }
        pack();
    }

    private void setWhereClausePanelBasedOnSource(Class sourceType) {
        usedConditionPanel.removeAll();
        if (sourceType==RegionDataset.class) {
            usedConditionPanel.add(regionConditionPanel);
            whereClausePanel.setVisible(true);
        } else whereClausePanel.setVisible(false);
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        boolean useMacros=((String)modeCombobox.getSelectedItem()).equals("macros");
        boolean insertBefore=((String)modeCombobox.getSelectedItem()).equals("beginning");
        boolean insertAfter=((String)modeCombobox.getSelectedItem()).equals("end");   
        boolean expressionFromTextVariable=((String)modeCombobox.getSelectedItem()).equals("variable");   
        boolean expressionFromMap=((String)modeCombobox.getSelectedItem()).equals("map");         
        parameters.removeParameter(Operation_replace.EXPAND_MACROS);          
        parameters.removeParameter(Operation_replace.INSERT_AFTER);          
        parameters.removeParameter(Operation_replace.INSERT_BEFORE);   
        parameters.removeParameter(Operation_replace.EXPRESSIONS_FROM_TEXTVARIABLE);        
        if (useMacros) {
            parameters.setParameter(Operation_replace.EXPAND_MACROS,Boolean.TRUE);   
        } else if (insertBefore) {
            parameters.setParameter(Operation_replace.INSERT_BEFORE,Boolean.TRUE);  
            String replaceExpression=insertExpressionTextField.getText();
            replaceExpression=MotifLabEngine.unescapeQuotedString(replaceExpression);
            parameters.setParameter(Operation_replace.REPLACE_PATTERN, replaceExpression);            
        } else if (insertAfter) {
            parameters.setParameter(Operation_replace.INSERT_AFTER,Boolean.TRUE);  
            String replaceExpression=insertExpressionTextField.getText();
            replaceExpression=MotifLabEngine.unescapeQuotedString(replaceExpression);
            parameters.setParameter(Operation_replace.REPLACE_PATTERN, replaceExpression);   
        } else if (expressionFromTextVariable) {
            parameters.setParameter(Operation_replace.EXPRESSIONS_FROM_TEXTVARIABLE,Boolean.TRUE);  
            String searchExpression=(String)searchDataCombobox.getSelectedItem();
            String replaceExpression=(String)replaceDataCombobox.getSelectedItem();
            searchExpression=MotifLabEngine.unescapeQuotedString(searchExpression);
            replaceExpression=MotifLabEngine.unescapeQuotedString(replaceExpression);
            parameters.setParameter(Operation_replace.SEARCH_PATTERN, searchExpression);
            parameters.setParameter(Operation_replace.REPLACE_PATTERN, replaceExpression); 
        } else if (expressionFromMap) {
            parameters.setParameter(Operation_replace.EXPRESSIONS_FROM_MAP,Boolean.TRUE);  
            String searchExpression=(String)searchMapCombobox.getSelectedItem();
            String replaceExpression=null;
            searchExpression=MotifLabEngine.unescapeQuotedString(searchExpression);
            parameters.setParameter(Operation_replace.SEARCH_PATTERN, searchExpression);
            parameters.setParameter(Operation_replace.REPLACE_PATTERN, replaceExpression); 
        } else {
            String searchExpression=searchExpressionTextField.getText();
            String replaceExpression=replaceExpressionTextField.getText();
            searchExpression=MotifLabEngine.unescapeQuotedString(searchExpression);
            replaceExpression=MotifLabEngine.unescapeQuotedString(replaceExpression);            
            parameters.setParameter(Operation_replace.SEARCH_PATTERN, searchExpression);
            parameters.setParameter(Operation_replace.REPLACE_PATTERN, replaceExpression);
        }
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        String sourceName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.SOURCE_NAME); // this should have been set in super.setParameters() above
        Class sourceType=getClassForDataItem(sourceName);
        if (whereCombobox.getSelectedItem().equals("Where")) {
            Condition condition=null;
            if (sourceType==RegionDataset.class) condition=regionConditionPanel.getCondition();
            parameters.setParameter("where", condition);
        } else parameters.setParameter("where", null);
        String datacollectionname=null;
        if (sourceType!=null) {
          if (sourceType==RegionDataset.class) {
              datacollectionname=sequenceSubsetCombobox.getSelectedItem().toString();
              if (datacollectionname.equals(engine.getDefaultSequenceCollectionName())) datacollectionname=null;
          } 
        }
        boolean usePropertyBox=(sourceType==RegionDataset.class);                      
        if (usePropertyBox) {
            String property=null;
            if (expressionFromTextVariable) property=((String)propertyCombobox2.getSelectedItem()).trim();
            else if (expressionFromMap) property=((String)propertyCombobox3.getSelectedItem()).trim();
            else property=((String)propertyCombobox.getSelectedItem()).trim();
            if (property.isEmpty() || property.equalsIgnoreCase("type")) property=null;
            parameters.setParameter(Operation_replace.REGION_PROPERTY, property);
        } else parameters.setParameter(Operation_replace.REGION_PROPERTY, null);   
        if (datacollectionname!=null) parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, datacollectionname);
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
            regionConditionPanel=new ConditionPanel_region(null, this);
        } else if (condition instanceof Condition_position) {
            regionConditionPanel=new ConditionPanel_region(null, this);
        } else {
            regionConditionPanel=new ConditionPanel_region((Condition_region)condition, this);
        }
        usedConditionPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        usedConditionPanel.add(regionConditionPanel);
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

    private void initSubsetPanel() {
        initSequenceSubsetPanel();
        subsetPanel=new JPanel();
        subsetPanel.setLayout(new CardLayout());
        subsetPanel.add(sequenceSubsetPanel,"SequenceCollection");
        subsetPanel.add(new JPanel(),"VOID");
    }

    public void setSubsetPanel(Class type) {
        if (type==RegionDataset.class) ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "SequenceCollection");
        else ((CardLayout)subsetPanel.getLayout()).show(subsetPanel, "VOID");
    }
    
            
}
