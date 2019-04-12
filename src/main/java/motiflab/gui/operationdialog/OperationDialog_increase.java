/*
 
 
 */

package motiflab.gui.operationdialog;

import javax.swing.JFrame;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_increase extends ArithmeticOperationDialog {

    public OperationDialog_increase(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_increase() {
        super();    
    }

    @Override
    public String getOperationNameLabel() {
        return "Increase";
    }

    @Override
    public String getPreposition() {
        return "by";
    }
      
}
