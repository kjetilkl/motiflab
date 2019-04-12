/*
 
 
 */

package motiflab.gui;

import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JTree;
import motiflab.engine.data.Data;
import motiflab.engine.MotifLabEngine;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import motiflab.engine.data.Module;
import motiflab.engine.data.ModuleMotif;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;


/**
 *
 * @author kjetikl
 */
public class MotifsPanelTreeModel_GroupByModules extends MotifsPanelTreeModel {
    private MotifLabEngine engine=null;
    private Class classfilter[]=new Class[]{Module.class, Motif.class};
    private MotifsPanel panel;


    public MotifsPanelTreeModel_GroupByModules(MotifLabEngine engine, MotifsPanel panel) {
        super();
        this.engine=engine;
        this.panel=panel;
        engine.addDataListener(this);
        ArrayList<Data> list=engine.getAllDataItemsOfType(Module.class);
        for (Data module:list) {
           dataAdded(module);
        }
        //if (((TreeNode)getRoot()).isLeaf()) insertNodeInto(new CheckBoxTreeNode("-EMTPY-"),((CheckBoxTreeNode)getRoot()),0);
        //insertNodeInto(new CheckBoxTreeNode("-EMTPY-"),((CheckBoxTreeNode)getRoot()),0);
    }

    public MotifLabEngine getEngine() {return engine;}

    /**
     * This method is called by the engine when a new data item has been added to the pool
     * @param data
     */
    public void dataAdded(final Data data) {
        if (!isAccepted(data)) return;
        if (MotifLabEngine.isTemporary(data)) return; // do not show temporary data items
        Runnable runner=new Runnable() {
            public void run() {
                if (data instanceof Module) {
                    for (int i=0;i<root.getChildCount();i++) {// check if node already exists!!!
                         CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                         Object val=node.getUserObject();
                         if (val!=null && val instanceof Data && ((Data)val).getName().equals(data.getName())) return; // it exists already! do nothing
                    }
                    CheckBoxTreeNode moduleNode=new CheckBoxTreeNode(data);
                    int index=findCorrectIndex((CheckBoxTreeNode)root,data.getName());
                    insertNodeInto(moduleNode, (CheckBoxTreeNode)root, index);
                    JTree tree=panel.getTree();
                    ArrayList<ModuleMotif> singlemotifslist=((Module)data).getModuleMotifs();
                    for (ModuleMotif singlemotif:singlemotifslist) {
                       CheckBoxTreeNode singlemotifNode=new CheckBoxTreeNode(singlemotif);
                       int singlemotifindex=moduleNode.getChildCount();
                       insertNodeInto(singlemotifNode, moduleNode, singlemotifindex);
                       MotifCollection motifs=singlemotif.getMotifAsCollection();
                       if (motifs!=null) {
                           for (Motif motif:motifs.getAllMotifs(engine)) {
                               CheckBoxTreeNode motifNode=new CheckBoxTreeNode(motif);
                               int motifindex=findCorrectIndex(singlemotifNode,motif.getName());
                               insertNodeInto(motifNode, singlemotifNode, motifindex);
                           }
                       }
                       tree.collapsePath(getPath(singlemotifNode));
                    }
                    tree.collapsePath(getPath(moduleNode));
                } else if (data instanceof Motif) {
                    dataUpdated(data);
                }
           }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }


    /**
     * This method is called by the engine when a data item has been removed from the pool
     * @param data
     */
    public void dataRemoved(final Data data) {
        if (!isAccepted(data)) return;
         Runnable runner=new Runnable() {
            public void run() {
                if (data instanceof Module) {
                    for (int i=0;i<root.getChildCount();i++) {
                        CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                        if (!(node.getUserObject() instanceof Data)) continue;
                        Data nodedata=(Data)node.getUserObject();
                        if (nodedata.getName().equals(data.getName())) {
                            removeNodeFromParent(node);
                            break;
                        }
                    } // end for each child
                } // end if instance of Motif
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    /**
     * This method is called by the engine when a data item has been updated
     * This will update the children of the module node, but the module node
     * itself need not be updated explicitly with nodeChanged since
     * 
     * @param data
     */
    public void dataUpdated(final Data data) {
     if (!(data instanceof Module || data instanceof Motif)) return;
          Runnable runner=new Runnable() {
            public void run() {
                if (data instanceof Module) {
                    CheckBoxTreeNode moduleNode=null;
                    for (int i=0;i<root.getChildCount();i++) {
                        CheckBoxTreeNode node =(CheckBoxTreeNode)root.getChildAt(i);
                        if (!(node.getUserObject() instanceof Data)) continue;
                        Data nodedata=(Data)node.getUserObject();
                        if (nodedata.getName().equals(data.getName())) {
                            moduleNode=node;
                            break;
                        }
                    }
                    if (moduleNode==null) return; // module node not found                  
                    int children=moduleNode.getChildCount();
                    for (int i=0;i<children;i++) { // remove all children
                        CheckBoxTreeNode node =(CheckBoxTreeNode)moduleNode.getChildAt(0); //
                        removeNodeFromParent(node);
                    }
                    // add the constituent motifs anew                   
                    ArrayList<ModuleMotif> singlemotifslist=((Module)data).getModuleMotifs();
                    for (ModuleMotif singlemotif:singlemotifslist) {
                       CheckBoxTreeNode singlemotifNode=new CheckBoxTreeNode(singlemotif);
                       int singlemotifindex=moduleNode.getChildCount();
                       insertNodeInto(singlemotifNode, moduleNode, singlemotifindex);
                       MotifCollection motifs=singlemotif.getMotifAsCollection();
                       if (motifs!=null) {
                           for (Motif motif:motifs.getAllMotifs(engine)) {
                               CheckBoxTreeNode motifNode=new CheckBoxTreeNode(motif);
                               int motifindex=findCorrectIndex(singlemotifNode,motif.getName());
                               insertNodeInto(motifNode, singlemotifNode, motifindex);
                           }
                       }
                    }
                } else if (data instanceof Motif) { // update all Motif nodes
                    updateUserObjectDataRecursively((CheckBoxTreeNode)root, data);
                }
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    public void dataAddedToSet(final Data parent, final Data child) {}

    public void dataRemovedFromSet(final Data parent, final Data child) {}

    public void dataUpdate(Data oldvalue, Data newvalue) {}

    
    /**
     * This method returns TRUE if the supplied Data argument
     * is an instance of one of the classes accepted by this model
     */
    private boolean isAccepted(Data data) {
        for (int i=0;i<classfilter.length;i++) {
            if (classfilter[i].isInstance(data)) return true;
        }
        return false;
    }


    /** This method finds the correct index where a new node with given string value should be inserted
     * (nodes are listed alphabetically on each level (for each parent))
     */
    private int findCorrectIndex(CheckBoxTreeNode node, String string) {
        int index=0;
        for (index=0;index<node.getChildCount();index++) {
            CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(index);
            String childString="";
            if (child.getUserObject()!=null) childString=child.getUserObject().toString();
            else childString=child.toString();
            if (string.compareTo(childString)<=0) break;
        }
        return index;
    }

// Returns a TreePath containing the specified node.
    private TreePath getPath(TreeNode node) {
        ArrayList<TreeNode> list = new ArrayList<TreeNode>();
        while (node != null) {
            list.add(node);
            node = node.getParent();
        }
        Collections.reverse(list); // Convert array of nodes to TreePath
        return new TreePath(list.toArray());
    }

}
