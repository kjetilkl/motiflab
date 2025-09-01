/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import org.motiflab.engine.operations.Condition_within;

/**
 *
 * @author kjetikl
 */
public class ConditionPanel_within extends ConditionPanel {
    private JCheckBox applyCheckbox=null;
    private JTextField rangetextfield=null;
    // private JComboBox notBox=null;
    private OperationDialog dialog;
    private Condition_within condition;
    
    public ConditionPanel_within(Condition_within condition, OperationDialog dialog) {
        super();
        this.condition=condition;
        this.dialog=dialog;
        init();
    }
    
    /**
     * Initializes the values in the ConditionPanel_within based on the supplied Condition
     */
    private void init() {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        applyCheckbox = new JCheckBox("Apply operation only within selected windows");
        rangetextfield = new JTextField(16);
        add(applyCheckbox);
        add(rangetextfield);
        if (condition!=null) {
            rangetextfield.setText(condition.toString());
            rangetextfield.setCaretPosition(0);
            applyCheckbox.setSelected(true);
        } 
    }    

    
    /**
     * Updates the parameters in the Condition object based on the selection made by the user in the GUI
     */
    @Override
    public Condition_within getCondition() {
        if (!applyCheckbox.isSelected()) return null;
        String text=rangetextfield.getText();
        if (text==null) return null;
        if (condition==null) condition=new Condition_within();
        condition.setOperandAString(text.trim());
        return condition;
    }  
    



}
