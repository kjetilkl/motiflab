/*
 
 
 */

package org.motiflab.gui.operationdialog;

import javax.swing.JFrame;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_set extends ArithmeticOperationDialog {
 
    public OperationDialog_set(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_set() {
        super();    
    }

    @Override
    public String getOperationNameLabel() {
        return "Set";
    }

    @Override
    public String getPreposition() {
        return "to";
    }
    
    
}
