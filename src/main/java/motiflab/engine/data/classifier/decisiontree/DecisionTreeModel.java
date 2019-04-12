/*
 
 
 */

package motiflab.engine.data.classifier.decisiontree;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

/**
 *
 * @author kjetikl
 */
public class DecisionTreeModel implements javax.swing.tree.TreeModel {

    DecisionTreeNode root;

    /** Creates a new instance of DecisionTreeModel */
    public DecisionTreeModel(DecisionTreeNode root) {
        this.root=root;
    }

    @Override
    public DecisionTreeNode getRoot() { return root;}

    @Override
    public DecisionTreeNode getChild(Object parent, int index) {
        return (DecisionTreeNode)((DecisionTreeNode)parent).getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
       return ((DecisionTreeNode)parent).getChildCount();
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((DecisionTreeNode)node).isLeafNode();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((DecisionTreeNode)parent).getIndex((DecisionTreeNode)child);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }

}
