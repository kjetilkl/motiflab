/*
 
 
 */

package motiflab.gui.operationdialog;

import javax.swing.JFrame;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_multiply extends ArithmeticOperationDialog {

    
    public OperationDialog_multiply(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_multiply() {
        super();    
    }

    @Override
    public String getOperationNameLabel() {
        return "Multiply";
    }

    @Override
    public String getPreposition() {
        return "by";
    }

}
