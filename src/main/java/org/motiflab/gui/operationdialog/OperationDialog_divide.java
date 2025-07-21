/*
 
 
 */

package org.motiflab.gui.operationdialog;

import javax.swing.JFrame;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_divide extends ArithmeticOperationDialog {

    public OperationDialog_divide(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_divide() {
        super();    
    }
    @Override
    public String getOperationNameLabel() {
        return "Divide";
    }

    @Override
    public String getPreposition() {
        return "by";
    }
}
