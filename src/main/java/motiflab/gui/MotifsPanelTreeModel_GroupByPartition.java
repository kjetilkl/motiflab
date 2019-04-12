/*


 */

package motiflab.gui;

import java.util.ArrayList;
import motiflab.engine.data.Data;
import motiflab.engine.MotifLabEngine;
import javax.swing.SwingUtilities;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifPartition;


/**
 *
 * @author kjetikl
 */
public class MotifsPanelTreeModel_GroupByPartition extends MotifsPanelTreeModel {
    private MotifLabEngine engine=null;
    private Class classfilter[]=new Class[]{MotifPartition.class, Motif.class};
    private MotifsPanel panel;

    /** Creates and new Tree Model that groups motifs by class up to a given level between 1 and 4 */
    public MotifsPanelTreeModel_GroupByPartition(MotifLabEngine engine, MotifsPanel panel) {
        super();
        this.engine=engine;
        this.panel=panel;
        engine.addDataListener(this);
        ArrayList<Data> list=engine.getAllDataItemsOfType(MotifPartition.class);
        for (Data collection:list) {
           dataAdded(collection);
        }
    }

    public MotifLabEngine getEngine() {return engine;}

    /**
     * This method is called by the engine when a new data item has been added to the pool
     * @param data
     */
    public final void dataAdded(final Data data) {
        if (!isAccepted(data)) return;
        if (MotifLabEngine.isTemporary(data)) return; // do not show temporary data items
        Runnable runner=new Runnable() {
            public void run() {
                if (data instanceof MotifPartition) {
                    for (int i=0;i<root.getChildCount();i++) {// check if partition node already exists!!!
                         CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                         Object val=node.getUserObject();
                         if (val!=null && val instanceof Data && ((Data)val).getName().equals(data.getName())) return; // it exists already! do nothing
                    }
                    CheckBoxTreeNode partitionNode=new CheckBoxTreeNode(data);
                    int index=findCorrectIndex((CheckBoxTreeNode)root,data.getName()); // insert partition node alphabetically
                    insertNodeInto(partitionNode, (CheckBoxTreeNode)root, index);
                    // now add all the cluster noded and the motifs for each cluster node
                    ArrayList<String> clusters=((MotifPartition)data).getClusterNames();
                    for (String cluster:clusters) {
                       CheckBoxTreeNode clusterNode=new CheckBoxTreeNode(cluster);
                       int clusterindex=findCorrectIndex(partitionNode,cluster);
                       insertNodeInto(clusterNode, partitionNode, clusterindex);
                       // insert motifs in this cluster
                        ArrayList<Motif> motiflist=((MotifPartition)data).getAllMotifsInCluster(cluster, engine);
                        for (Motif motif:motiflist) {
                           CheckBoxTreeNode motifNode=new CheckBoxTreeNode(motif);
                           int motifindex=findCorrectIndex(clusterNode,motif.getName());
                           insertNodeInto(motifNode, clusterNode, motifindex);
                        }
                   }
                } else if (data instanceof Motif) { // this can happen if motifs are updated
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
                if (data instanceof MotifPartition) {
                    for (int i=0;i<root.getChildCount();i++) {
                        CheckBoxTreeNode node=(CheckBoxTreeNode)root.getChildAt(i);
                        if (!(node.getUserObject() instanceof Data)) continue;
                        Data nodedata=(Data)node.getUserObject();
                        if (nodedata.getName().equals(data.getName())) {
                            removeNodeFromParent(node);
                            break;
                        }
                    } // end for each child
                } // end if instance of MotifPartition
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    /** This is called when a Motif changes cluster. We don't know which cluster is affected so just update all of them */
    public void dataUpdated(final Data data) {
     if (!(data instanceof MotifPartition || data instanceof Motif)) return;
          Runnable runner=new Runnable() {
            public void run() {
                if (data instanceof MotifPartition) {
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
                    // insert child nodes anew
                    ArrayList<String> clusters=((MotifPartition)data).getClusterNames();
                    for (String cluster:clusters) {
                       CheckBoxTreeNode clusterNode=new CheckBoxTreeNode(cluster);
                       int clusterindex=findCorrectIndex(parentnode,cluster);
                       insertNodeInto(clusterNode, parentnode, clusterindex);
                       // insert motifs in this cluster
                        ArrayList<Motif> motiflist=((MotifPartition)data).getAllMotifsInCluster(cluster, engine);
                        for (Motif motif:motiflist) {
                           CheckBoxTreeNode motifNode=new CheckBoxTreeNode(motif);
                           int motifindex=findCorrectIndex(clusterNode,motif.getName());
                           insertNodeInto(motifNode, clusterNode, motifindex);
                        }
                   }
                } else { // data instanceof Motif. Update every instance of the Motif
                    for (int i=0;i<root.getChildCount();i++) {
                         CheckBoxTreeNode partitionnode=(CheckBoxTreeNode)root.getChildAt(i);
                         int clusterCount=partitionnode.getChildCount();
                         partitionloop:
                         for (int j=0;j<clusterCount;j++) {
                             CheckBoxTreeNode clusternode=(CheckBoxTreeNode)partitionnode.getChildAt(j);
                             int childCount=clusternode.getChildCount();
                             for (int k=0;k<childCount;k++) {
                                 CheckBoxTreeNode motifnode=(CheckBoxTreeNode)clusternode.getChildAt(k);
                                 Object val=motifnode.getUserObject();
                                 if (val!=null && val instanceof Data && ((Data)val).getName().equals(data.getName())) {
                                    motifnode.setUserObject(data);
                                    nodeChanged(motifnode);
                                    break partitionloop; // the motif can only occur once in each partition
                                 }                                     
                             }                                                      
                         } // for each cluster
                    } // for each partition                                      
                }
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    /** This is called when a motif is added to a cluster */
    public void dataAddedToSet(final Data parent, final Data child) {
        //if (!isAccepted(child)) return;
        if (!(parent instanceof MotifPartition)) return;
        if (!(child instanceof Motif)) return;
        Runnable runner=new Runnable() {
            public void run() {
                String targetClusterName=((MotifPartition)parent).getClusterForMotif((Motif)child);
                for (int i=0;i<root.getChildCount();i++) {
                    CheckBoxTreeNode parentnode=(CheckBoxTreeNode)root.getChildAt(i);
                    Data nodedata=(Data)parentnode.getUserObject();
                    if (nodedata.getName().equals(parent.getName())) { // found node for MotifPartition
                        for (int j=0;j<parentnode.getChildCount();j++) {
                             CheckBoxTreeNode clusternode=(CheckBoxTreeNode)root.getChildAt(j);
                             String clustername=clusternode.getName();
                             if (clustername.equals(targetClusterName)) { // found node for MotifPartition
                                 CheckBoxTreeNode childnode=new CheckBoxTreeNode(child);
                                 int motifindex=findCorrectIndex(clusternode,child.getName());
                                 insertNodeInto(childnode, clusternode, motifindex);
                                 break;
                             }
                        } // end for each cluster in target partition
                    }
                } // end for each partition
           } // end run()
        }; // end class Runnable
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);
    }

    /** This is called when a motif is removed from a cluster but not moved into a different cluster (it is removed from the partition entirely) */
    public void dataRemovedFromSet(final Data parent, final Data child) {
        //if (!isAccepted(child)) return;
        if (!(parent instanceof MotifPartition)) return;
        if (!(child instanceof Motif)) return;
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
                if (parentnode==null) return; // parent partition not found
                // now search every cluster in the partition for the child
                for (int i=0;i<parentnode.getChildCount();i++) {
                    CheckBoxTreeNode clusternode =(CheckBoxTreeNode)parentnode.getChildAt(i);
                    for (int j=0;j<clusternode.getChildCount();j++) {
                        CheckBoxTreeNode node =(CheckBoxTreeNode)clusternode.getChildAt(j);
                        Data nodedata=(Data)node.getUserObject();
                        if (nodedata.getName().equals(child.getName())) {
                            childnode=node;
                            break;
                        }
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