/*
 
 
 */

package org.motiflab.gui.operationdialog;

import javax.swing.JFrame;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_decrease extends ArithmeticOperationDialog {
    
    public OperationDialog_decrease(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_decrease() {
        super();    
    }

    @Override
    public String getOperationNameLabel() {
        return "Decrease";
    }

    @Override
    public String getPreposition() {
        return "by";
    }

}
