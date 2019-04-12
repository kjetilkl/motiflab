/*
 
 
 */

package motiflab.engine.data.classifier.decisiontree;


import java.util.*;
import javax.swing.tree.*;

/**
 *
 * @author kjetikl
 */
public class DecisionTreeNode extends DefaultMutableTreeNode  {

    private HashMap<String,DecisionTreeNode> childrenset;
    private String classlabel=null; // the classlabel (used for
    private int branchAttributeIndex=-1; // the attribute selected for test at this node
    private DecisionTreeNode parent=null;
    private ArrayList<DecisionTreeNode> childrenaslist=new ArrayList<DecisionTreeNode>();

    // the following to fields are used for pruning bookkeeping
    private  int error=0;
    private int totalPassingExamples=0;
    private HashMap<String,Integer> classcounts=null; // classlabel->number of examples propagated to this node with given classlabel


    /** Creates a new instance of DecisionTreeNode */
    public DecisionTreeNode() {
        super("hole");
    }

    public boolean isLeafNode() {
        return (classlabel!=null);
    }


    public void addBranch(String branchValue, DecisionTreeNode subtree) { // this can also be used for replacement!
        if (childrenset==null) childrenset=new HashMap<String,DecisionTreeNode>();
        childrenaslist.add(subtree);
        childrenset.put(branchValue,subtree);
        subtree.setParent(this);
    }

    /** replaces a previous child with the new one. The associated branch-value remains the same for the new subtree*/
    public void replaceBranch(DecisionTreeNode oldsubtree, DecisionTreeNode newsubtree) { // this can also be used for replacement!
        if (childrenset==null) childrenset=new HashMap<String,DecisionTreeNode>();
        String branchValue=null;
        for (String value : childrenset.keySet()) { // go through map to find the split attribute value of oldsubtree
            if (childrenset.get(value)==oldsubtree) {branchValue=value;break;}
        }
        childrenset.put(branchValue,newsubtree); // replaces oldsubtree with newsubtree
        int index=childrenaslist.indexOf(oldsubtree);
        childrenaslist.add(index,newsubtree);
        childrenaslist.remove(oldsubtree);
        newsubtree.setParent(this);
    }

    private void setParent(DecisionTreeNode node) {
        parent=node;
    }

    public DecisionTreeNode getParent() {
        return parent;
    }

    private void setClassLabel(String label) {
        classlabel=label;
    }

    public String getClassLabel() {
        return classlabel;
    }

    public int getBranchAttribute() {
        return branchAttributeIndex;
    }

    public String getBranchAttributeValueForChild(DecisionTreeNode child) {
        String branchValue=null;
        for (String value : childrenset.keySet()) { // go through map to find the split attribute value of oldsubtree
            if (childrenset.get(value)==child) {branchValue=value;break;}
        }
        return branchValue;
    }

    public String getBranchAttributeValue() {
        if (parent==null) return "ROOT";
        return parent.getBranchAttributeValueForChild(this);
    }

    public DecisionTreeNode getSubTreeForValue(String value) {
        return childrenset.get(value);
    }



     private void setAttributeIndex(int index) {
        branchAttributeIndex=index;
    }

    public static DecisionTreeNode createLeafNode(String classlabel) {
        DecisionTreeNode node = new DecisionTreeNode();
        node.setClassLabel(classlabel);
        return node;
    }

    public static DecisionTreeNode createBranchNode(int attributeIndex) {

        DecisionTreeNode node = new DecisionTreeNode();
        node.setAttributeIndex(attributeIndex);
        return node;
    }

    public int getNumberOfSubtreeNodes() {
        if (isLeafNode()) return 1;
        int nodes=0;
        for (String branch : childrenset.keySet()) {
            nodes+=childrenset.get(branch).getNumberOfSubtreeNodes();
        }
        return nodes+1;
    }

    public int getSubtreeErrors() {
        if (isLeafNode()) return this.error;
        int errors=0;
        for (String branch : childrenset.keySet()) {
            errors+=childrenset.get(branch).getSubtreeErrors();
        }
        return errors;
    }

    public void incrementError() {error++;}
    
    public void resetSubtreeCounts() {
        int errors=0;
        totalPassingExamples=0;
        if (isLeafNode()) return;
        classcounts=null;
        for (String branch : childrenset.keySet()) {
            childrenset.get(branch).resetSubtreeCounts();
        }
    }

    public void incrementClassCount(String classlabel) {
        if (classcounts==null) classcounts=new HashMap<String,Integer>();
        if (classcounts.get(classlabel)==null) classcounts.put(classlabel,new Integer(1));
        else classcounts.put(classlabel,new Integer(classcounts.get(classlabel).intValue()+1));
        totalPassingExamples++;
    }

    public Collection<DecisionTreeNode> getChildren() {
       return childrenset.values();
    }

    public int getCountsForClass(String classlabel) {
       if (classcounts==null) {return 0;}
       else if (!classcounts.containsKey(classlabel)) {return 0;}
       else return classcounts.get(classlabel).intValue();
    }

    public HashMap<String,Integer> getClassCounts() {
        return classcounts;
    }

    public int getTotalPassingExamples() {
       return totalPassingExamples;
    }

    private String indent(int level) {
        int tab=3;
        String text="";
        for (int i=0;i<level*tab;i++) text=text.concat(" ");
        return text;
    }

    public void visualize(java.io.PrintStream stream,String value,int level) {
        if (isLeafNode()) stream.println(indent(level)+"o "+value+"="+classlabel);
        else {
            if (value!=null) stream.println(indent(level)+"+ "+value+" ---> split on ["+branchAttributeIndex+"]");
            else stream.println(indent(level)+"---> split on ["+branchAttributeIndex+"]");
            for (String branch : childrenset.keySet()) childrenset.get(branch).visualize(stream,branch,level+1);
        }
    }

    public String toString() {
        if (isLeafNode()) return classlabel;
        else return ""+branchAttributeIndex;
    }

    // ---------------------------------------

    public boolean isLeaf() { // implementation of GUI interface
        return (classlabel!=null);
    }

    public boolean getAllowsChildren() { // implementation of GUI interface
        return (classlabel==null);
    }

    public int getChildCount() {
        return childrenset.size();
    }

    public TreeNode getChildAt(int childIndex) {
        return (TreeNode)childrenaslist.get(childIndex);
    }

    public Enumeration children() {
        return new MyOwnEnumeration(childrenaslist);
    }

    public int getIndex(TreeNode node) {
        return childrenaslist.indexOf(node);
    }

    class MyOwnEnumeration implements Enumeration {
        ArrayList list;
        int counter=0;
        public MyOwnEnumeration(ArrayList list) {
            this.list=list;
        }
        public boolean hasMoreElements() {return counter<list.size();}
        public Object nextElement() {
           Object o=list.get(counter);
           counter++;
           return o;
        }
    }

}

