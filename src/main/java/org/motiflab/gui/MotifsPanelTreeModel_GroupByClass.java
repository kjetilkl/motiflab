/*
 
 
 */

package org.motiflab.gui;

import java.util.ArrayList;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.MotifLabEngine;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifClassification;


/**
 *
 * @author kjetikl
 */
public class MotifsPanelTreeModel_GroupByClass extends MotifsPanelTreeModel {
    private MotifLabEngine engine=null;
    private Class classfilter[]=new Class[]{Motif.class};
    private int levels;
    private MotifsPanel panel;
    
    /** Creates and new Tree Model that groups motifs by class up to a given level between 1 and 6 */
    public MotifsPanelTreeModel_GroupByClass(MotifLabEngine engine, int levels, MotifsPanel panel) {
        super();
        this.engine=engine;   
        this.levels=levels;
        this.panel=panel;
        if (levels<1) levels=1;
        else if (levels>6) levels=6;
        engine.addDataListener(this);
        ArrayList<Data> list=engine.getAllDataItemsOfType(Motif.class);
        for (Data collection:list) {
           dataAdded(collection); 
        }
        //if (((TreeNode)getRoot()).isLeaf()) insertNodeInto(new CheckBoxTreeNode("-EMTPY-"),((CheckBoxTreeNode)getRoot()),0);
        //insertNodeInto(new CheckBoxTreeNode("-EMTPY-"),(CheckBoxTreeNode)root,0);
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
                    if (getNodeForName((CheckBoxTreeNode)root,data.getName())!=null) return; // node already exists! 
                    String motifclass=getClassForMotif((Motif)data);
                    if (motifclass==null) motifclass=MotifClassification.UNKNOWN_CLASS_LABEL;
                    CheckBoxTreeNode classNode=getClassNode(motifclass);                    
                    CheckBoxTreeNode motifNode=new CheckBoxTreeNode(data);
                    int index=findCorrectIndex(classNode,data.getName());
                    insertNodeInto(motifNode, classNode, index);
                }
           }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);  
    }
        
    /** Searches through the tree and returns the (first) node that corresponds to the given name
     * or NULL if no matching node is found
     */
    private CheckBoxTreeNode getNodeForName(CheckBoxTreeNode node, String name) {
        if (node.getName().equals(name)) return node;
        for (int i=0;i<node.getChildCount();i++) {
           CheckBoxTreeNode found=getNodeForName((CheckBoxTreeNode)node.getChildAt(i),name);
           if (found!=null) return found;
        }
        return null;
    }

    /** Returns the class the given motif up to the level specified for this treemodel */
    private String getClassForMotif(Motif data) {
        String motifclass=data.getClassification();
        if (motifclass==null) return null;
        String[] path=MotifClassification.getClassPath(motifclass);
        if (path.length<=levels) return motifclass;
        else return path[levels-1];
    }
    
    /** 
     * Returns the Node corresponding to the motif class with the given classString
     * If no such node is present in the tree when this method is called, a new class
     * node will be created anew and added to the tree at the correct position
     */
    private CheckBoxTreeNode getClassNode(String classString) {
        String[] classPath=MotifClassification.getClassPath(classString);
        CheckBoxTreeNode currentNode=(CheckBoxTreeNode)root;
        for (int i=0;i<classPath.length;i++) {
            String nextLevel=classPath[i];
            CheckBoxTreeNode nextNode=getNodeForName(currentNode, nextLevel);
            if (nextNode==null) {
                //System.err.println("Creating new node for "+classString);
                nextNode=new CheckBoxTreeNode(nextLevel);
                int motifindex=findCorrectIndexForClassNode(currentNode,nextLevel);
                insertNodeInto(nextNode, currentNode, motifindex);                 
            }
            currentNode=nextNode; 
        }
        return currentNode;
    }
    
    /**
     * This method is called by the engine when a data item has been removed from the pool
     * If the data represents a motif it will be removed from the tree along with
     * any classnodes that are left empty as a result
     * @param data
     */
    public void dataRemoved(final Data data) {
        if (!isAccepted(data)) return;
        final CheckBoxTreeNode node=getNodeForName((CheckBoxTreeNode)root,data.getName());        
        if (node==null) return; // node not found
        Runnable runner=new Runnable() {
           public void run() { 
               CheckBoxTreeNode classnode=(CheckBoxTreeNode)node.getParent();
               node.removeFromParent();
               while (classnode!=root && classnode!=null) {   
                   if (classnode.isLeaf()) { // no other motifs in class after 'data' was removed
                      CheckBoxTreeNode parent=(CheckBoxTreeNode)classnode.getParent(); 
                      classnode.removeFromParent();
                      classnode=parent;
                   } else break;
               }
               nodeStructureChanged(root);
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);                   
    }
    
            
            
    public void dataUpdate(final Data oldvalue,final Data newvalue) {
        if (!isAccepted(newvalue)) return;
        if (!(oldvalue instanceof Motif) || !(newvalue instanceof Motif)) return;
        String oldclass=((Motif)oldvalue).getClassification();
        String newclass=((Motif)newvalue).getClassification();
        boolean classchanged=((oldclass==null && newclass!=null) || (oldclass!=null && newclass==null) || (oldclass!=null && newclass!=null && !newclass.equals(oldclass)));
        Runnable runner=null;
        if (classchanged) {
              runner=new Runnable() {            
                public void run() { // this will move the node to a new position
                    dataRemoved(oldvalue);
                    dataAdded(newvalue);
                    nodeStructureChanged(root); // the tree does not update unless I do this :-(
                } // end run()
            }; // end class Runnable

        } else { // the motif class has not changed so there is not need to move the node, but it needs to be updated anyway!
             runner=new Runnable() {  
                private void processRecursively(TreeNode parent) {
                    for (int i=0;i<parent.getChildCount();i++) {
                        TreeNode node=parent.getChildAt(i);
                        if (!node.isLeaf()) processRecursively(node);
                        else if (node instanceof CheckBoxTreeNode) {
                             Object val=((CheckBoxTreeNode)node).getUserObject();
                             if (val!=null && val instanceof Data && ((Data)val).getName().equals(newvalue.getName())) {
                                ((CheckBoxTreeNode)node).setUserObject(newvalue);
                                nodeChanged(node);
                             }                             
                        }
                    }
                }                    
                public void run() { // this will move the node to a new position
                    processRecursively(root);
                } // end run()
            }; // end class Runnable            
        }
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);         
    }      

    
    // The following empty methods complete the interface implementation but are not needed
    public void dataUpdated(final Data data) {} // the necessary updates are handled by dataUpdate(oldvalue,newvalue)
    public void dataAddedToSet(final Data parent, final Data child) {}    
    public void dataRemovedFromSet(final Data parent, final Data child) {}

    
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
    
    /** This method finds the correct index where a new node with given string value should be inserted
     * (nodes are listed alphabetically on each level (for each parent))
     */
    private int findCorrectIndexForClassNode(CheckBoxTreeNode node, String string) {
        int index=0;
        for (index=0;index<node.getChildCount();index++) {
            CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(index);
            String childString="";
            if (child.getUserObject()!=null) childString=child.getUserObject().toString();
            else childString=child.toString();
            if (MotifClassification.compareClassStrings(string,childString)<=0) break;
        }
        return index;
    }

   
    
}
