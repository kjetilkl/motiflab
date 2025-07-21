/*
 
 
 */

package org.motiflab.gui;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import org.motiflab.engine.DataListener;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;

/**
 *
 * @author Kjetil
 */
public abstract class MotifsPanelTreeModel extends DefaultTreeModel implements DataListener {

    public MotifsPanelTreeModel() {
        super(new CheckBoxTreeNode("Root Node"));
    }

    public void clearAll() {
        Runnable runner=new Runnable() {
           public void run() {
               root=new CheckBoxTreeNode("Root Node");
               nodeStructureChanged(root);
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }
    
    protected void updateUserObjectDataRecursively(CheckBoxTreeNode node, Data data) {
        if (node.isLeaf()) {
             Object val=node.getUserObject();
             if (val!=null && val instanceof Data && ((Data)val).getName().equals(data.getName())) {
                node.setUserObject(data);
                nodeChanged(node);
             }               
        } else { // process children recursively
            for (int i=0;i<node.getChildCount();i++) {
                CheckBoxTreeNode childnode =(CheckBoxTreeNode)node.getChildAt(i);
                updateUserObjectDataRecursively(childnode,data);
            } 
        }       
    }
    
}
