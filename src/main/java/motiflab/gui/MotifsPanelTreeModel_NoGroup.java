/*
 
 
 */

package motiflab.gui;

import java.util.ArrayList;
import motiflab.engine.data.Data;
import motiflab.engine.MotifLabEngine;
import javax.swing.SwingUtilities;
import motiflab.engine.data.Motif;


/**
 *
 * @author kjetikl
 */
public class MotifsPanelTreeModel_NoGroup extends MotifsPanelTreeModel {
    private MotifLabEngine engine=null;
    private Class classfilter[]=new Class[]{Motif.class};
    private MotifsPanel panel;

    
    public MotifsPanelTreeModel_NoGroup(MotifLabEngine engine, MotifsPanel panel) {
        super();
        this.engine=engine;   
        this.panel=panel;
        engine.addDataListener(this);
        ArrayList<Data> list=engine.getAllDataItemsOfType(Motif.class);
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
                if (data instanceof Motif) {
                    for (int i=0;i<root.getChildCount();i++) {// check if node already exists!!!
                         CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                         Object val=node.getUserObject();
                         if (val!=null && val instanceof Data && ((Data)val).getName().equals(data.getName())) return; // it exists already! do nothing
                    }  
                    CheckBoxTreeNode motifNode=new CheckBoxTreeNode(data);
                    int index=findCorrectIndex((CheckBoxTreeNode)root,data.getName());
                    insertNodeInto(motifNode, (CheckBoxTreeNode)root, index);
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
                if (data instanceof Motif) {            
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
     * @param data
     */
    public void dataUpdated(final Data data) {
        if (!isAccepted(data)) return;
        if (MotifLabEngine.isTemporary(data)) return; // do not show temporary data items
        Runnable runner=new Runnable() {
            public void run() {
                if (data instanceof Motif) {
                    for (int i=0;i<root.getChildCount();i++) {// check if node already exists!!!
                         CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                         Object val=node.getUserObject();
                         if (val!=null && val instanceof Data && ((Data)val).getName().equals(data.getName())) {
                            node.setUserObject(data);
                            nodeChanged(node);
                            break;
                         } 
                    }  
                }
            }
        };
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
    
}
