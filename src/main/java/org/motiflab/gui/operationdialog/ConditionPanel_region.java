/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleTextMap;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifTextMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceTextMap;
import org.motiflab.engine.operations.Condition_boolean;
import org.motiflab.engine.operations.Condition_region;
import org.motiflab.engine.operations.Condition_region_boolean;
import org.motiflab.gui.MiscIcons;


/**
 *
 * @author kjetikl
 */
public class ConditionPanel_region extends ConditionPanel {
    private JTextField expressionTextfield=null; // compare type against literal expression
    private JComboBox typeCollectionCombobox=null; // compare type against selected collection, text map or text variable
    private JComboBox numericScoreCombobox=null; // numeric value used for score&length comparison
    private JComboBox numericScoreCombobox2=null; // second numeric value used for score&length comparison (upper bound in ranges)
    private JComboBox numericCombobox=null; // numeric value used for avg|min|max|median comparison with wiggle
    private JComboBox numericCombobox2=null; // second numeric value used for avg|min|max|median comparison with wiggle (upper bound in ranges)
    private JComboBox wiggleCombobox=null; // wiggle dataset for avg|min|max|median comparison
    private JComboBox regionCombobox=null; // region dataset for overlaps/inside comparison
    private JComboBox numericComparatorCombobox=null; // =|>|>=|<|<=|<> for score&length comparison
    private JComboBox numericWiggleComparatorCombobox=null; // =|>|>=|<|<=|<> for avg|min|max|median wiggle comparison
    private JComboBox stringComparatorCombobox=null; // equals|matches|is in
    private JComboBox comparePropertyCombobox=null; // type|score|length|overlaps|inside|covers|average|min|max|median|startValue|endValue|relativeStartValue|relativeEndValue|regionStartValue|regionEndValue|centerValue
    private JComboBox typeRestrictCombobox=null; //
    private JComboBox notComboBox=null; // negate all?
    private JTextField numericUserPropertyName=null;
    private JTextField textUserPropertyName=null;
    private String[] numericComparatorStrings=new String[]{"=",">",">=","<","<=","<>","in"};
    private String[] stringComparatorStrings=new String[]{"equals","is in","matches","matches in"};
    private String[] comparePropertyStrings=new String[]{"type","score","length","overlaps","inside","covers","present in","similar in","distance to closest","distance to any","startValue","endValue","relativeStartValue","relativeEndValue","regionStartValue","regionEndValue","centerValue","min","max","average","median","sum","weighted average","weighted sum","text property","numeric property","boolean property"};
    private String[] typeRestrictStrings=new String[]{"","type-equal","type-matching"};
    private OperationDialog dialog;
    private Condition_region condition;
    private JPanel numericScore2Panel;
    private JPanel numeric2Panel;
    private ConditionPanel_boolean compoundPanel=null; 
    private JButton compoundButton=null;
    private boolean allowCompound=true;

    
    private DefaultComboBoxModel model_wiggle;
    private DefaultComboBoxModel model_region;
    private DefaultComboBoxModel model_region_plus_interaction_partner;
    
    
    public ConditionPanel_region(Condition_region condition, OperationDialog dialog) {
        super();
        if (condition==null) this.condition=new Condition_region();
        else this.condition=condition;
        this.dialog=dialog;
        init();
    }
    
    public ConditionPanel_region(Condition_region condition, OperationDialog dialog, boolean allowCompound) {
        super();
        this.allowCompound=allowCompound;
        if (condition==null) this.condition=new Condition_region();
        else this.condition=condition;
        this.dialog=dialog;
        init();
    }    
    
    /**
     * Initializes the values in the ConditionPanel_wiggle based on the supplied Condition
     */
    private void init() {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        if (condition instanceof Condition_region_boolean) {
            compoundPanel=new ConditionPanel_boolean(condition, dialog);
            this.add(compoundPanel);
            return;
        }        
        final JPanel cardsPanel=new JPanel(new CardLayout());
        JPanel clausePanel[]=new JPanel[5];
        String compareProperty=condition.getCompareProperty();
        if (compareProperty==null || compareProperty.isEmpty()) compareProperty="type";
        String comparator=condition.getComparator();
        String operandAString=condition.getOperandAString();
        String operandBString=condition.getOperandBString();
        String operandB2String=condition.getOperandB2String();
        String typeRestrict=condition.getOtherRegionTypeRestriction();
        Class userPropertyType=condition.getUserDefinedPropertyType();
        if (compareProperty.equals("maximum")) compareProperty="max";
        else if (compareProperty.equals("minimum")) compareProperty="min";
        else if (compareProperty.equals("avg")) compareProperty="average";
        Boolean whereNot=condition.negateAll();
        if (whereNot==null) whereNot=Boolean.FALSE;

        model_wiggle=dialog.getDataCandidates(new Class[]{NumericDataset.class});
        model_region=dialog.getDataCandidates(new Class[]{RegionDataset.class});
        model_region_plus_interaction_partner=dialog.getDataCandidates(new Class[]{RegionDataset.class});
        model_region_plus_interaction_partner.insertElementAt("interaction partner", 0);
        DefaultComboBoxModel model_collection=dialog.getDataCandidates(new Class[]{MotifCollection.class,ModuleCollection.class,SequenceCollection.class,MotifTextMap.class,ModuleTextMap.class,SequenceTextMap.class,TextVariable.class});
        wiggleCombobox = new JComboBox();
        if (compareProperty.startsWith("distance to")) wiggleCombobox.setModel(model_region_plus_interaction_partner);
        else wiggleCombobox.setModel(model_wiggle);
        regionCombobox = new JComboBox(model_region);
        typeCollectionCombobox = new JComboBox(model_collection);
        numericCombobox = getNumericCombobox();
        numericScoreCombobox = getNumericCombobox();
        numericCombobox2 = getNumericCombobox();
        numericScoreCombobox2 = getNumericCombobox();

        stringComparatorCombobox = new JComboBox(stringComparatorStrings);
        numericComparatorCombobox = new JComboBox(numericComparatorStrings);
        numericWiggleComparatorCombobox = new JComboBox(numericComparatorStrings);
        comparePropertyCombobox = new JComboBox(comparePropertyStrings);
        typeRestrictCombobox = new JComboBox(typeRestrictStrings);
        expressionTextfield=new JTextField(10);
        numericUserPropertyName=new JTextField(10);
        textUserPropertyName=new JTextField(10);
        
        String[] notboxchoices=new String[]{"","not"};
        notComboBox = new JComboBox(notboxchoices);
        if (whereNot.booleanValue()) notComboBox.setSelectedIndex(1); else notComboBox.setSelectedIndex(0); 

        this.add(notComboBox);
        this.add(new JLabel(" region "));
        this.add(comparePropertyCombobox);
        this.add(typeRestrictCombobox);

        clausePanel[0]=new JPanel(); // "type panel"
        clausePanel[0].setLayout(new FlowLayout(FlowLayout.LEADING));
        clausePanel[0].add(textUserPropertyName);
        clausePanel[0].add(stringComparatorCombobox);
        clausePanel[0].add(expressionTextfield);
        clausePanel[0].add(typeCollectionCombobox);
    
        clausePanel[1]=new JPanel(); // "score/length panel"
        clausePanel[1].setLayout(new FlowLayout(FlowLayout.LEADING));
        clausePanel[1].add(numericUserPropertyName);
        clausePanel[1].add(numericComparatorCombobox);
        clausePanel[1].add(numericScoreCombobox);
        numericScore2Panel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        numericScore2Panel.add(new JLabel(" to "));
        numericScore2Panel.add(numericScoreCombobox2);
        numericScore2Panel.setVisible(false);
        clausePanel[1].add(numericScore2Panel);
             
        clausePanel[2]=new JPanel(); // "overlaps/inside panel"
        clausePanel[2].setLayout(new FlowLayout(FlowLayout.LEADING));
        clausePanel[2].setAlignmentY(JComponent.CENTER_ALIGNMENT);
        clausePanel[2].add(regionCombobox);
            
        clausePanel[3]=new JPanel(); // "Wiggle panel"
        clausePanel[3].setLayout(new FlowLayout(FlowLayout.LEADING));
        clausePanel[3].add(wiggleCombobox);
        clausePanel[3].add(numericWiggleComparatorCombobox);
        clausePanel[3].add(numericCombobox);
        numeric2Panel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        numeric2Panel.add(new JLabel(" to "));
        numeric2Panel.add(numericCombobox2);
        numeric2Panel.setVisible(false);
        clausePanel[3].add(numeric2Panel);
        
        clausePanel[4]=new JPanel();

        cardsPanel.add(clausePanel[0],"TextPropertyPanel");
        cardsPanel.add(clausePanel[1],"NumericPropertyPanel");
        cardsPanel.add(clausePanel[2],"OverlapsInsideCovers");
        cardsPanel.add(clausePanel[3],"Wiggle");
        cardsPanel.add(clausePanel[4],"NoCondition");
   
        this.add(cardsPanel);  
        
        if (allowCompound) {            
            compoundButton=new JButton(new MiscIcons(MiscIcons.PLUS_ICON));
            Dimension bs=new Dimension(20,19);
            compoundButton.setPreferredSize(bs);
            compoundButton.setMaximumSize(bs);
            compoundButton.setMinimumSize(bs);
            //compoundButton.setBorderPainted(false);
            compoundButton.setToolTipText("Add more conditions connected by AND/OR");
            compoundButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switchToCompoundCondition();
                }
            });
            this.add(compoundButton);      
        }

        comparePropertyCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedProperty=(String)comparePropertyCombobox.getSelectedItem();
                     if (selectedProperty.equals("")) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "NoCondition");
                else if (selectedProperty.equals("type") || selectedProperty.equals("text property") || selectedProperty.equals("boolean property") ) {
                    ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "TextPropertyPanel");
                    textUserPropertyName.setVisible(!selectedProperty.equals("type"));
                }
                else if (selectedProperty.equals("score") || selectedProperty.equals("length") || selectedProperty.equals("numeric property")) {
                    ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "NumericPropertyPanel");
                    numericUserPropertyName.setVisible(selectedProperty.equals("numeric property"));                
                }
                else if (selectedProperty.equals("overlaps") || selectedProperty.equals("inside") || selectedProperty.equals("covers") || selectedProperty.equals("present in") || selectedProperty.equals("similar in")) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "OverlapsInsideCovers");
                else if (selectedProperty.startsWith("distance to")) {
                    wiggleCombobox.setModel(model_region_plus_interaction_partner);
                    ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "Wiggle");
                }
                else {
                    wiggleCombobox.setModel(model_wiggle);
                    ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "Wiggle");
                }
                typeRestrictCombobox.setVisible(selectedProperty.equals("overlaps") || selectedProperty.equals("inside") || selectedProperty.equals("covers") || selectedProperty.startsWith("distance to"));
                dialog.pack();
            }
        });
        stringComparatorCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedComparator=(String)stringComparatorCombobox.getSelectedItem();
                if (selectedComparator!=null && (selectedComparator.equals("is in") || selectedComparator.equals("matches in"))) {expressionTextfield.setVisible(false);typeCollectionCombobox.setVisible(true);}
                else {typeCollectionCombobox.setVisible(false);expressionTextfield.setVisible(true);}
                dialog.pack();
            }
        });
        numericComparatorCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedComparator=(String)numericComparatorCombobox.getSelectedItem();
                if (selectedComparator.equals("in")) numericScore2Panel.setVisible(true);
                else numericScore2Panel.setVisible(false);
                dialog.pack();
            }
        });
        numericWiggleComparatorCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedComparator=(String)numericWiggleComparatorCombobox.getSelectedItem();
                if (selectedComparator.equals("in")) numeric2Panel.setVisible(true);
                else numeric2Panel.setVisible(false);
                dialog.pack();
            }
        });
        numericComparatorCombobox.setSelectedIndex(0);
        numericWiggleComparatorCombobox.setSelectedIndex(0);
        stringComparatorCombobox.setSelectedItem(comparator);
        if (typeRestrict!=null) typeRestrictCombobox.setSelectedItem(typeRestrict); else typeRestrictCombobox.setSelectedIndex(0);
        if (stringComparatorCombobox.getSelectedIndex()<0) stringComparatorCombobox.setSelectedIndex(0);
        if (userPropertyType!=null) {
            if (userPropertyType==Number.class) {
                numericUserPropertyName.setText(compareProperty);
                comparePropertyCombobox.setSelectedItem("numeric property");
            } else if (userPropertyType==String.class) {
                textUserPropertyName.setText(compareProperty);
                comparePropertyCombobox.setSelectedItem("text property");
            } else if (userPropertyType==Boolean.class) {
                textUserPropertyName.setText(compareProperty);
                comparePropertyCombobox.setSelectedItem("boolean property");
            } 
        }
        if (comparator==null) {}
        else if (isNumericalComparison(comparator)) {
            if (operandBString!=null && !operandBString.isEmpty()) numericCombobox.setSelectedItem(operandBString);
            if (operandBString!=null && !operandBString.isEmpty()) numericScoreCombobox.setSelectedItem(operandBString);
            if (operandB2String!=null && !operandB2String.isEmpty()) numericCombobox2.setSelectedItem(operandB2String);
            if (operandB2String!=null && !operandB2String.isEmpty()) numericScoreCombobox2.setSelectedItem(operandB2String);
            if (operandAString!=null && !operandAString.isEmpty()) wiggleCombobox.setSelectedItem(operandAString); 
            numericComparatorCombobox.setSelectedItem(comparator);
            numericWiggleComparatorCombobox.setSelectedItem(comparator);
        } else if (comparator.equals("is in") || comparator.equals("matches in")) {
            if (operandBString!=null && !operandBString.isEmpty())  typeCollectionCombobox.setSelectedItem(operandBString);
        } else if (comparator.equals("equals") || comparator.equals("matches")) {
            expressionTextfield.setText(operandBString.replaceAll("\"", ""));
        } else if (compareProperty.equals("overlaps") || compareProperty.equals("inside") || compareProperty.equals("covers") || compareProperty.equals("present in") || compareProperty.equals("similar in")) {
            if (operandBString!=null && !operandBString.isEmpty()) regionCombobox.setSelectedItem(operandBString); 
        } 
        if (compareProperty==null || compareProperty.isEmpty()) comparePropertyCombobox.setSelectedIndex(0);
        else comparePropertyCombobox.setSelectedItem(compareProperty);
        if (numericCombobox.getSelectedItem()==null || ((String)numericCombobox.getSelectedItem()).isEmpty()) numericCombobox.setSelectedItem("0");
        if (numericScoreCombobox.getSelectedItem()==null || ((String)numericScoreCombobox.getSelectedItem()).isEmpty()) numericScoreCombobox.setSelectedItem("0");
        if (numericCombobox2.getSelectedItem()==null || ((String)numericCombobox2.getSelectedItem()).isEmpty()) numericCombobox2.setSelectedItem("1");
        if (numericScoreCombobox2.getSelectedItem()==null || ((String)numericScoreCombobox2.getSelectedItem()).isEmpty()) numericScoreCombobox2.setSelectedItem("1");
    }    

    
    /**
     * Updates the parameters in the Condition object based on the selection made by the user in the GUI
     */
    @Override
    public Condition_region getCondition() {
        if (compoundPanel!=null) {
            return (Condition_region)compoundPanel.getCondition();
        }        
        String operandAString=null;
        String operandBString=null;
        String operandB2String=null;
        String comparator=null;
        String compareProperty=(String)comparePropertyCombobox.getSelectedItem();
        String typeRestrict=(String)typeRestrictCombobox.getSelectedItem();
        Class userPropertyClass=null;
        if (typeRestrict.isEmpty()) typeRestrict=null;
        Boolean whereNot=Boolean.FALSE;
        boolean OK=true;
        if (notComboBox.getSelectedItem().equals("not")) whereNot=Boolean.TRUE; else whereNot=Boolean.FALSE; 
        if (compareProperty==null || compareProperty.isEmpty()) {OK=false;}
        else if (compareProperty.equals("type") || compareProperty.equals("text property") || compareProperty.equals("boolean property")) {
            if (compareProperty.equals("text property") || compareProperty.equals("boolean property")) {
                userPropertyClass=(compareProperty.equals("text property"))?String.class:Boolean.class;
                compareProperty=textUserPropertyName.getText();
                if (compareProperty!=null) compareProperty=compareProperty.trim();
            }             
            comparator=(String)stringComparatorCombobox.getSelectedItem();
            if (comparator.equals("is in") || comparator.equals("matches in")) {
                operandBString=(String)typeCollectionCombobox.getSelectedItem();
                if (operandBString==null || operandBString.isEmpty()) OK=false;
            }                
            else {
                operandBString=expressionTextfield.getText().trim();
                operandBString.replaceAll("\"", ""); // remove quotes that the user has entered
                if (operandBString.isEmpty()) OK=false;
                if (!operandBString.startsWith("\"")) operandBString="\""+operandBString; // add quotes
                if (!operandBString.endsWith("\"")) operandBString=operandBString+"\""; // add quotes
            }            
        } else if (compareProperty.equals("score") || compareProperty.equals("length") || compareProperty.equals("numeric property")) {
            if (compareProperty.equals("numeric property")) {
                compareProperty=numericUserPropertyName.getText();
                if (compareProperty!=null) compareProperty=compareProperty.trim();
                userPropertyClass=Number.class;
            } 
            comparator=(String)numericComparatorCombobox.getSelectedItem();
            operandBString=((String)numericScoreCombobox.getSelectedItem()).trim();
            if (operandBString==null || operandBString.isEmpty()) OK=false;
            if (comparator.equals("in")){
                operandB2String=(String)numericScoreCombobox2.getSelectedItem();
                if (operandB2String==null || operandB2String.isEmpty())OK=false;
            }
        } else if (compareProperty.equals("overlaps") || compareProperty.equals("inside") || compareProperty.equals("covers") || compareProperty.equals("present in") || compareProperty.equals("similar in")) {
            operandBString=(String)regionCombobox.getSelectedItem();
            if (operandBString==null || operandBString.isEmpty()) OK=false;
        } else {
            comparator=(String)numericWiggleComparatorCombobox.getSelectedItem();
            operandAString=(String)wiggleCombobox.getSelectedItem();
            operandBString=(String)numericCombobox.getSelectedItem();            
            if (operandBString==null || operandBString.isEmpty()) OK=false;
            if (comparator.equals("in")){
                operandB2String=(String)numericCombobox2.getSelectedItem();
                if (operandB2String==null || operandB2String.isEmpty())OK=false;
            }
            if (operandAString==null || operandAString.isEmpty()) OK=false;
        }
        
        condition.setCompareProperty(compareProperty);
        condition.setComparator(comparator);
        condition.setOperandAString(operandAString);
        condition.setOperandBString(operandBString);   
        condition.setOperandB2String(operandB2String);
        condition.setOtherRegionTypeRestriction(typeRestrict);
        condition.setUserDefinedPropertyType(userPropertyClass);
        condition.setNegateAll(whereNot);  
        if (!OK) return null;
        else return condition;
    }  

    
    private boolean inStringArray(String[] list,String element) {
        for (int i=0;i<list.length;i++) {
            if (list[i].equals(element)) return true;
        }
        return false;
    }

    private JComboBox getNumericCombobox() {
        DefaultComboBoxModel model=dialog.getDataCandidates(new Class[]{NumericVariable.class,NumericMap.class},false); // Note that both MotifNumericMap, ModuleNumericMap and SequenceNumericMap are allowed
        model.addElement("property \"name\"");
        JComboBox combobox = new JComboBox(model);
        combobox.setEditable(true);  
        return combobox;
    }
    
    private boolean isNumericalComparison(String cmp) {
        return inStringArray(numericComparatorStrings, cmp);
    }
    
    private void switchToCompoundCondition() {
        Condition_region current=getCondition();
        if (current==null) current=new Condition_region_boolean(Condition_boolean.AND); // and empty placeholder                
        this.removeAll();
        compoundPanel=new ConditionPanel_boolean(current, dialog);
        this.add(compoundPanel);
        dialog.pack();     
    }    
    
}
