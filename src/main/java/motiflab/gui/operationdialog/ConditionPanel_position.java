/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.TextVariable;
import motiflab.engine.operations.Condition_boolean;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Condition_position_boolean;
import motiflab.gui.MiscIcons;

/**
 *
 * @author kjetikl
 */
public class ConditionPanel_position extends ConditionPanel {
    private JComboBox dataAcombobox=null;
    private JComboBox dataBregioncombobox=null;
    private JComboBox dataBwigglecombobox=null;
    private JComboBox dataB2wigglecombobox=null;
    private JComboBox dataBdnacombobox=null;
    private JComboBox regionComparator=null;
    private JComboBox wiggleComparator=null;
    private JComboBox dnaComparator=null;    
    private JComboBox notBox=null;
    private String[] comparatorStrings=new String[]{"=",">",">=","<","<=","<>","in"};
    private String[] regionComparatorStrings=new String[]{"inside","bases overlap","bases not overlap","regions overlap","regions not overlap"};
    private String[] dnaComparatorStrings=new String[]{"equals","equals relative strand","case-sensitive equals","case-sensitive equals relative strand","matches","matches relative strand","is uppercase","is lowercase","has same case as"};
    private OperationDialog dialog;
    private Condition_position condition;
    private JPanel dataB2panel;
    private ConditionPanel_boolean compoundPanel=null;
    private JButton compoundButton=null;
    private boolean allowCompound=true;
    
    public ConditionPanel_position(Condition_position condition, OperationDialog dialog) {
        super();
        if (condition==null) this.condition=new Condition_position();
        else this.condition=condition;
        this.dialog=dialog;
        init();
    }
    
    public ConditionPanel_position(Condition_position condition, OperationDialog dialog, boolean allowCompound) {
        super();
        this.allowCompound=allowCompound;
        if (condition==null) this.condition=new Condition_position();
        else this.condition=condition;
        this.dialog=dialog;
        init();
    }    
    
    /**
     * Initializes the values in the ConditionPanel_position based on the supplied Condition
     */
    private void init() {        
        setLayout(new FlowLayout(FlowLayout.LEADING));
        if (condition instanceof Condition_position_boolean) {
            compoundPanel=new ConditionPanel_boolean(condition, dialog);
            this.add(compoundPanel);
            return;
        }
        final JPanel cardsPanel=new JPanel(new CardLayout());
        JPanel clausePanel[]=new JPanel[4];
        String dataA_string=condition.getOperandAString();
        Class dataA_class=dialog.getClassForDataItem(dataA_string);
        String dataB_string=condition.getOperandBString();
        String dataB2_string=condition.getOperandB2String();
//        if (dataB_string==null || dataB_string.isEmpty()) dataB_string="0";
//        if (dataB2_string==null || dataB2_string.isEmpty()) dataB2_string="1";
        String whereComparatorString=condition.getComparator();   
        Boolean whereNot=condition.negateAll();
        if (whereNot==null) whereNot=Boolean.FALSE;
        DefaultComboBoxModel model_dataA=dialog.getDataCandidates(new Class[]{RegionDataset.class,NumericDataset.class,DNASequenceDataset.class});
        DefaultComboBoxModel model_dataBregion=dialog.getDataCandidates(new Class[]{RegionDataset.class});
        DefaultComboBoxModel model_dataBwiggle=dialog.getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class,NumericDataset.class});
        DefaultComboBoxModel model_dataB2wiggle=dialog.getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class,NumericDataset.class});
        DefaultComboBoxModel model_dataBdna=dialog.getDataCandidates(new Class[]{DNASequenceDataset.class, TextVariable.class});
        dataAcombobox = new JComboBox(model_dataA);
        dataBregioncombobox = new JComboBox(model_dataBregion);
        dataBwigglecombobox = new JComboBox(model_dataBwiggle);
        dataBwigglecombobox.setEditable(true);
        dataB2wigglecombobox = new JComboBox(model_dataB2wiggle);
        dataB2wigglecombobox.setEditable(true);
        dataBdnacombobox = new JComboBox(model_dataBdna);
        dataBdnacombobox.setEditable(true);
        regionComparator = new JComboBox(regionComparatorStrings);
        wiggleComparator = new JComboBox(comparatorStrings);
        dnaComparator = new JComboBox(dnaComparatorStrings);        

        String[] notboxchoices=new String[]{"","not"};
        notBox = new JComboBox(notboxchoices);
        if (whereNot.booleanValue()) notBox.setSelectedIndex(1); else notBox.setSelectedIndex(0); 

        clausePanel[0]=new JPanel(); 
        
        clausePanel[1]=new JPanel(); 
        clausePanel[1].setLayout(new FlowLayout(FlowLayout.LEADING));
        clausePanel[1].add(regionComparator);
        clausePanel[1].add(dataBregioncombobox);
             
        clausePanel[2]=new JPanel();
        clausePanel[2].setLayout(new FlowLayout(FlowLayout.LEADING));
        clausePanel[2].add(wiggleComparator);
        clausePanel[2].add(dataBwigglecombobox);
        dataB2panel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        dataB2panel.add(new JLabel(" to "));
        dataB2panel.add(dataB2wigglecombobox);
        dataB2panel.setVisible(false);
        clausePanel[2].add(dataB2panel);
        
        clausePanel[3]=new JPanel(); 
        clausePanel[3].setLayout(new FlowLayout(FlowLayout.LEADING));
        clausePanel[3].add(dnaComparator);
        clausePanel[3].add(dataBdnacombobox);     

        cardsPanel.add(clausePanel[0],"Everywhere");
        cardsPanel.add(clausePanel[1],"Region");
        cardsPanel.add(clausePanel[2],"Wiggle");
        cardsPanel.add(clausePanel[3],"DNA");        

        this.add(notBox);       
        this.add(dataAcombobox);       
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
        
        dataAcombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedName=(String)dataAcombobox.getSelectedItem();
                if (selectedName==null || selectedName.isEmpty()) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "Everywhere");
                else if (dialog.getClassForDataItem(selectedName)==RegionDataset.class) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "Region");
                else if (dialog.getClassForDataItem(selectedName)==NumericDataset.class) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "Wiggle");
                else if (dialog.getClassForDataItem(selectedName)==DNASequenceDataset.class) ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "DNA");
                else ((CardLayout)cardsPanel.getLayout()).show(cardsPanel, "Everywhere");
                dialog.pack();
            }
        });
        regionComparator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedComparator=(String)regionComparator.getSelectedItem();
                if (selectedComparator.equals("inside") || selectedComparator.equals("outside")) dataBregioncombobox.setVisible(false);
                else dataBregioncombobox.setVisible(true);
                dialog.pack();
            }
        });
        wiggleComparator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedComparator=(String)wiggleComparator.getSelectedItem();
                if (selectedComparator.equals("in")) dataB2panel.setVisible(true);
                else dataB2panel.setVisible(false);
                dialog.pack();
            }
        });
        dnaComparator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedComparator=(String)dnaComparator.getSelectedItem();
                if (selectedComparator.equals("is uppercase") || selectedComparator.equals("is lowercase")) dataBdnacombobox.setVisible(false);
                else dataBdnacombobox.setVisible(true);
                dialog.pack();
            }
        });        
        String selectedComparator=(String)regionComparator.getSelectedItem();
        if (selectedComparator.equals("inside") || selectedComparator.equals("outside")) dataBregioncombobox.setVisible(false);
        else dataBregioncombobox.setVisible(true);
      
        if (dataA_string!=null && !dataA_string.isEmpty()) dataAcombobox.setSelectedItem(dataA_string);
        else if (dataAcombobox.getModel().getSize()>0) dataAcombobox.setSelectedIndex(0);
        if (dataB_string!=null) dataBregioncombobox.setSelectedItem(dataB_string); 
        else if (dataBregioncombobox.getItemCount()>0) dataBregioncombobox.setSelectedIndex(0);       
        dataBwigglecombobox.setSelectedItem((dataB_string==null || dataB_string.isEmpty() || !(dataA_class==NumericDataset.class))?"0":dataB_string);
        dataBdnacombobox.setSelectedItem((dataB_string==null || dataB_string.isEmpty() || !(dataA_class==DNASequenceDataset.class))?"\"A\"":dataB_string);        
        dataB2wigglecombobox.setSelectedItem((dataB2_string==null || dataB2_string.isEmpty())?"1":dataB2_string);
        if (whereComparatorString==null || !inStringArray(regionComparatorStrings, whereComparatorString)) regionComparator.setSelectedIndex(0);
        else regionComparator.setSelectedItem(whereComparatorString);
        if (whereComparatorString==null || !inStringArray(comparatorStrings, whereComparatorString)) wiggleComparator.setSelectedIndex(0);
        else wiggleComparator.setSelectedItem(whereComparatorString);   
        if (whereComparatorString==null || !inStringArray(dnaComparatorStrings, whereComparatorString)) dnaComparator.setSelectedIndex(0);
        else dnaComparator.setSelectedItem(whereComparatorString);          
    }    

    
    /**
     * Updates the parameters in the Condition object based on the selection made by the user in the GUI
     */
    @Override
    public Condition_position getCondition() {
        if (compoundPanel!=null) {
            return (Condition_position)compoundPanel.getCondition();
        }
        String whereOperandA=(String)dataAcombobox.getSelectedItem();
        String whereOperandB=null;
        String whereOperandB2=null;
        String whereComparator=null;
        Boolean whereNot=Boolean.FALSE;
        if (whereOperandA==null || whereOperandA.isEmpty()) {
                 whereOperandA=null;
        } else if (dialog.getClassForDataItem(whereOperandA)==RegionDataset.class) {
            whereComparator=(String)regionComparator.getSelectedItem();
            if (whereComparator.equals("inside") || whereComparator.equals("outside")) {
               whereOperandB=null; 
            } else {
               whereOperandB=(String)dataBregioncombobox.getSelectedItem(); 
            }
        } else if (dialog.getClassForDataItem(whereOperandA)==NumericDataset.class) {
            whereComparator=(String)wiggleComparator.getSelectedItem();
            whereOperandB=(String)dataBwigglecombobox.getSelectedItem(); 
            whereOperandB2=(String)dataB2wigglecombobox.getSelectedItem();
        } else if (dialog.getClassForDataItem(whereOperandA)==DNASequenceDataset.class) {
            whereComparator=(String)dnaComparator.getSelectedItem();
            if (whereComparator.equals("is uppercase") || whereComparator.equals("is lowercase")) {
               whereOperandB=null; 
            } else {
               whereOperandB=(String)dataBdnacombobox.getSelectedItem(); 
               if (!(whereOperandB.startsWith("\"") && whereOperandB.endsWith("\""))) {
                   if (dialog.getClassForDataItem(whereOperandB)==null && whereOperandB.length()==1) whereOperandB="\""+whereOperandB+"\""; // the user probably forgot quotes so just add them
               }
            }
        }
        if (notBox.getSelectedItem().equals("not")) whereNot=Boolean.TRUE; else whereNot=Boolean.FALSE; 
        condition.setComparator(whereComparator);
        condition.setOperandAString(whereOperandA);
        condition.setOperandBString(whereOperandB);   
        condition.setOperandB2String(whereOperandB2);
        condition.setNegateAll(whereNot);  
        if (whereOperandA==null) return null;
        else return condition;
    }  
    
    
    private boolean inStringArray(String[] list,String element) {
        for (int i=0;i<list.length;i++) {
            if (list[i].equals(element)) return true;
        }
        return false;
    }

    private void switchToCompoundCondition() {
        Condition_position current=getCondition();
        if (current==null) current=new Condition_position_boolean(Condition_boolean.AND); // and empty placeholder                
        this.removeAll();
        compoundPanel=new ConditionPanel_boolean(current, dialog);
        this.add(compoundPanel);
        dialog.pack();     
    }
    
}
