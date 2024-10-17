/*
 
 
 */

package motiflab.gui;

import javax.swing.tree.DefaultMutableTreeNode;
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.ModuleMotif;
import motiflab.engine.data.Motif;

/**
 * This class is used for the nodes in the checkbox Tree in the MotifsPanel where the nodes
 * can represent Motif or Modules (or not)
 * 
 * @author kjetikl
 */
public class CheckBoxTreeNode extends DefaultMutableTreeNode {
    private boolean processed=false;
    private boolean checked=true;
    private static VisualizationSettings settings;
    
    public static void setVisualizationSettings(VisualizationSettings s) {settings=s;}
    
    public CheckBoxTreeNode(Object userObject) {
        super(userObject);
    }
    
    public CheckBoxTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public boolean isChecked() {
       if (userObject instanceof Motif || userObject instanceof ModuleCRM) return settings.isRegionTypeVisible(getName());
       else if (userObject instanceof ModuleMotif) {
           CheckBoxTreeNode myparent=(CheckBoxTreeNode)getParent();
           ModuleCRM cisRegModule=(ModuleCRM)myparent.getUserObject();
           return settings.isRegionTypeVisible(cisRegModule.getName()+"."+((ModuleMotif)userObject).getRepresentativeName());
       }
       else return checked;
    }
    
    public void setChecked(boolean checkedvalue) {
        if (userObject instanceof Motif || userObject instanceof ModuleCRM) settings.setRegionTypeVisible(getName(),checkedvalue,false);
        else if (userObject instanceof ModuleMotif) {
           CheckBoxTreeNode myparent=(CheckBoxTreeNode)getParent();
           ModuleCRM cisRegModule=(ModuleCRM)myparent.getUserObject();
           settings.setRegionTypeVisible(cisRegModule.getName()+"."+((ModuleMotif)userObject).getRepresentativeName(),checkedvalue,false);
        }
        else checked=checkedvalue;
    }
    
    public boolean isProcessed() {
       return processed;
    }
    
    public void setProcessed(boolean processedvalue) {
        this.processed=processedvalue;
    }
    
    public String getName() {
        if (userObject==null) return "empty";
        else return userObject.toString();
    }
    
    public String toString() {
        return getName();
    }
}

