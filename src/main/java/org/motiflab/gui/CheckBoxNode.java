
package org.motiflab.gui;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class represents nodes in the general CheckBoxTree widget
 * @author kjetikl
 */
public class CheckBoxNode extends DefaultMutableTreeNode {
    private boolean checked=true;
    private boolean processed=false;    
    private Icon icon=null;
    private String tooltip=null;
    private String label=null;
    
    public CheckBoxNode(Object userObject) {
        super(userObject);
    } 
    
    public CheckBoxNode(Object userObject, String label) {
        super(userObject);
        this.label=label;
    }
    
    public CheckBoxNode(Object userObject, String label, boolean allowsChildren) {
        super(userObject, allowsChildren);
        this.label=label;
    }
    
    public CheckBoxNode(Object userObject, String label, Icon icon, String tooltip) {
        super(userObject);
        this.label=label;
        this.icon=icon;
        this.tooltip=tooltip;
    }
    
    public CheckBoxNode(Object userObject, String label, Icon icon, String tooltip, boolean allowsChildren) {
        super(userObject, allowsChildren);
        this.label=label;
        this.icon=icon;
        this.tooltip=tooltip;        
    }    

    public boolean isChecked() {
         return checked;
    }
    
    public void setChecked(boolean checkedvalue) {
        checked=checkedvalue;
    }
    
    public boolean isProcessed() {
       return processed;
    }
    
    public void setProcessed(boolean processedvalue) {
        this.processed=processedvalue;
    }    
    
    public String getLabel() {
        if (label!=null) return label;
        if (userObject==null) return "";
        else return userObject.toString();
    }
    
    @Override
    public String toString() {
        return getLabel();
    }
    
    public Icon getIcon() {
        return icon;
    }
    
    public String getTooltip() {
        return (tooltip!=null)?tooltip:getLabel();
    }
}
