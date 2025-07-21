/*
 
 
 */

package org.motiflab.gui;

import java.util.ArrayList;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.MotifLabEngine;
import javax.swing.SwingUtilities;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleCRM;


/**
 *
 * @author kjetikl
 */
public class MotifsPanelTreeModel_GroupByModuleCollection extends MotifsPanelTreeModel {
    private MotifLabEngine engine=null;
    private Class classfilter[]=new Class[]{ModuleCollection.class, ModuleCRM.class};
    private MotifsPanel panel;

    public MotifsPanelTreeModel_GroupByModuleCollection(MotifLabEngine engine,MotifsPanel panel) {
        super();
        this.engine=engine;
        this.panel=panel;
        engine.addDataListener(this);
        ArrayList<Data> list=engine.getAllDataItemsOfType(ModuleCollection.class);
        for (Data collection:list) {
           dataAdded(collection);
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
                if (data instanceof ModuleCollection) {
                    for (int i=0;i<root.getChildCount();i++) {// check if collection node already exists!!!
                         CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                         Object val=node.getUserObject();
                         if (val!=null && val instanceof Data && ((Data)val).getName().equals(data.getName())) return; // it exists already! do nothing
                    }
                    CheckBoxTreeNode collectionNode=new CheckBoxTreeNode(data);
                    int index=findCorrectIndex((CheckBoxTreeNode)root,data.getName());
                    insertNodeInto(collectionNode, (CheckBoxTreeNode)root, index);
                    ArrayList<ModuleCRM> modulelist=((ModuleCollection)data).getAllModules(engine);
                    for (ModuleCRM cisRegModule:modulelist) {
                       CheckBoxTreeNode moduleNode=new CheckBoxTreeNode(cisRegModule);
                       int moduleindex=findCorrectIndex(collectionNode,cisRegModule.getName());
                       insertNodeInto(moduleNode, collectionNode, moduleindex);
                    }
                } else if (data instanceof ModuleCRM) {
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
                if (data instanceof ModuleCollection) {
                    for (int i=0;i<root.getChildCount();i++) {
                        CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                        if (!(node.getUserObject() instanceof Data)) continue;
                        Data nodedata=(Data)node.getUserObject();
                        if (nodedata.getName().equals(data.getName())) {
                            //if (tree!=null) tree.setSelectionPath(null); else System.err.println("SLOPPY PROGRAMMING ERROR: Forgot to setTree(tree) in MotifsPanelListModel");
                            removeNodeFromParent(node);
                            break;
                        }
                    } // end for each child
                } // end if instance of ModuleCollection
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    /**
     * This method is called by the engine when a data item has been updated
     * @param data
     */
    public void dataUpdated(final Data data) {
     if (!(data instanceof ModuleCollection || data instanceof ModuleCRM)) return; // No need to respond to ModuleCRM changes since the module tree node does not change in the current structure
          Runnable runner=new Runnable() {
            public void run() {
                if (data instanceof ModuleCollection) {
                    CheckBoxTreeNode parentnode=null;
                    for (int i=0;i<root.getChildCount();i++) {
                        CheckBoxTreeNode node =(CheckBoxTreeNode)root.getChildAt(i);
                        if (!(node.getUserObject() instanceof Data)) continue;
                        Data nodedata=(Data)node.getUserObject();
                        if (nodedata.getName().equals(data.getName())) {
                            parentnode=node;
                            break;
                        }
                    }
                    if (parentnode==null) return; // parent not found
                    int children=parentnode.getChildCount();
                    for (int i=0;i<children;i++) { // remove all children
                        CheckBoxTreeNode node =(CheckBoxTreeNode)parentnode.getChildAt(0); //
                        removeNodeFromParent(node);
                    }
                    ArrayList<ModuleCRM> modulelist=((ModuleCollection)data).getAllModules(engine);
                    for (ModuleCRM cisRegModule:modulelist) {
                       CheckBoxTreeNode moduleNode=new CheckBoxTreeNode(cisRegModule);
                       int moduleindex=findCorrectIndex(parentnode,cisRegModule.getName());
                       insertNodeInto(moduleNode, parentnode, moduleindex);
                    }
                } else if (data instanceof ModuleCRM) {
                    updateUserObjectDataRecursively((CheckBoxTreeNode)root, data);
                }
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    public void dataAddedToSet(final Data parent, final Data child) {
        if (!isAccepted(child)) return;
        if (!(parent instanceof ModuleCollection)) return;
         Runnable runner=new Runnable() {
            public void run() {
                for (int i=0;i<root.getChildCount();i++) {
                    CheckBoxTreeNode parentnode=(CheckBoxTreeNode)root.getChildAt(i);
                    Data nodedata=(Data)parentnode.getUserObject();
                    if (nodedata.getName().equals(parent.getName())) {
                        CheckBoxTreeNode childnode=new CheckBoxTreeNode(child);
                        int moduleindex=findCorrectIndex(parentnode,child.getName());
                        insertNodeInto(childnode, parentnode, moduleindex);
                        break;
                    }
                } // end for each child
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    public void dataRemovedFromSet(final Data parent, final Data child) {
        if (!isAccepted(child)) return;
        if (!(parent instanceof ModuleCollection)) return;
          Runnable runner=new Runnable() {
            public void run() {
                CheckBoxTreeNode parentnode=null;
                CheckBoxTreeNode childnode=null;
                for (int i=0;i<root.getChildCount();i++) {
                    CheckBoxTreeNode node =(CheckBoxTreeNode)root.getChildAt(i);
                    Data nodedata=(Data)node.getUserObject();
                    if (nodedata.getName().equals(parent.getName())) {
                        parentnode=node;
                        break;
                    }
                }
                if (parentnode==null) return; // parent not found
                for (int i=0;i<parentnode.getChildCount();i++) {
                    CheckBoxTreeNode node =(CheckBoxTreeNode)parentnode.getChildAt(i);
                    Data nodedata=(Data)node.getUserObject();
                    if (nodedata.getName().equals(child.getName())) {
                        childnode=node;
                        break;
                    }
                }
                if (childnode!=null) removeNodeFromParent(childnode);
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
   }


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

}
