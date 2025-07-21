
package org.motiflab.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;



/**
 * This class extends the regular JTree with checkboxes in front of each node
 * 
 * @author kjetikl
 */
public class CheckBoxTree extends JTree {

   private CheckBoxTreeCellRenderer checkBoxTreeCellRenderer;
    // private SingleMotifTooltip tooltip=null;        


        
  public CheckBoxTree() {
      super();
      initComponents();
      ToolTipManager.sharedInstance().registerComponent(this); // this is necessary for trees
  } 
  
  

    @Override
    public JToolTip createToolTip() {
        JToolTip tooltip=super.createToolTip();
        tooltip.setToolTipText("hei:"+Math.random());
        return tooltip;
    }   

  
  private void initComponents() {   
        setDragEnabled(false);
        setEditable(false);
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE) {
                    int[] rows=getSelectionModel().getSelectionRows();
                    if (rows==null) return;
                    Arrays.sort(rows);
                    int checkedNodes=0;
                    for (int row:rows) {
                        CheckBoxNode node=(CheckBoxNode)getPathForRow(row).getLastPathComponent();
                        if (node.isChecked()) checkedNodes++;
                    }
                    boolean doCheck=true;
                    if (checkedNodes==rows.length) doCheck=false;
                    checkSelectedRows(doCheck);
                } 
            } // end key pressed 
        });
        checkBoxTreeCellRenderer=new CheckBoxTreeCellRenderer();        
        setCellRenderer(checkBoxTreeCellRenderer);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Object node=null;
                int x=e.getX();
                int y=e.getY();
                int row = getRowForLocation(x, y);
                if (row<0) return;
                if (!isRowSelected(row)) {
                    setSelectionRow(row);
                }
                Rectangle rect = getRowBounds(row);
	        if (rect == null) return;	  
                TreePath path=getPathForRow(row);
                if (path!=null) node=path.getLastPathComponent();
                if (node!=null && node instanceof CheckBoxNode) {
                   if (checkBoxTreeCellRenderer.isOnHotspot(x - rect.x, y - rect.y)) { // pressed checkbox
                       CheckBoxNode checknode=(CheckBoxNode)node;
                       checknode.setChecked(!checknode.isChecked()); // reverse Check
                       if (!checknode.isLeaf()) {
                           setCheckedRecursively(checknode,checknode.isChecked());
                       }
                       repaint();
	            } else { // pressed the label
              
                    }                   
                }
                //
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                int row = getRowForLocation(e.getX(), e.getY());
                if (row<0) return;
                if (!isRowSelected(row)) setSelectionRow(row);
                //repaint();
            }            
        });       
  }

/** Sets the check-status of all children of the specified node (if any) to the specified check value.
 * Note that the check-status of the specified node itself is not updated!
 */  
private void setCheckedRecursively(CheckBoxNode node, boolean check) {
    int size=node.getChildCount();
    for (int i=0;i<size;i++) {
      CheckBoxNode child=(CheckBoxNode)node.getChildAt(i);
      child.setChecked(check);
      if (!child.isLeaf()) setCheckedRecursively(child,check);
    }
}


/** Selected rows that are not leafs will be expanded (as will their children) */
public void expandSelectedRows() {
    int[] rows=getSelectionModel().getSelectionRows();
    if (rows==null) return;
    Arrays.sort(rows);
    for (int i=rows.length-1;i>=0;i--) {
        int row=rows[i];  
        TreePath path = getPathForRow(row);  
        expandPathRecursively(path);  
    }                    
    repaint();
}

/** Selected rows that are not leafs will be collapsed (as will their children) */
public void collapseSelectedRows() {
    int[] rows=getSelectionModel().getSelectionRows();
    if (rows==null) return;
    Arrays.sort(rows);
    for (int i=rows.length-1;i>=0;i--) {
        int row=rows[i];  
        TreePath path = getPathForRow(row);  
        collapsePathRecursively(path); 
    }                    
    repaint();
}

private void expandPathRecursively(TreePath path) {
    expandPath(path);
    CheckBoxNode node=(CheckBoxNode)path.getLastPathComponent();
    if (!node.isLeaf()) {
        for (int i=0;i<node.getChildCount();i++) {
            CheckBoxNode child=(CheckBoxNode)node.getChildAt(i);
            expandPathRecursively(path.pathByAddingChild(child));
        }
    }
}

private void collapsePathRecursively(TreePath path) {    
    CheckBoxNode node=(CheckBoxNode)path.getLastPathComponent();
    if (!node.isLeaf()) {
        for (int i=0;i<node.getChildCount();i++) {
            CheckBoxNode child=(CheckBoxNode)node.getChildAt(i);
            collapsePathRecursively(path.pathByAddingChild(child));
        }
        collapsePath(path);
    }   
}

public void checkSelectedRows(boolean docheck) {
    int[] rows=getSelectionModel().getSelectionRows();
    if (rows==null) return;
    Arrays.sort(rows);
    for (int row:rows) {
        CheckBoxNode node=(CheckBoxNode)getPathForRow(row).getLastPathComponent();
        if (node.isProcessed()) continue;
        node.setChecked(docheck);
        if (!node.isLeaf()) {
            node.setChecked(docheck);
            setCheckedRecursively(node, docheck);
            setProcessedRecursively(node, true);
        } 
    }                    
    setProcessedRecursively((CheckBoxNode)getModel().getRoot(),false); // clear processed flags
    repaint();
}

/** Sets the processed-status of all children of the specified node (if any) to the specified value.
 * The processed status of the node itself is also set!
 */  
private void setProcessedRecursively(CheckBoxNode node, boolean processed) {
    node.setProcessed(processed);
    int size=node.getChildCount();
    for (int i=0;i<size;i++) {
      CheckBoxNode child=(CheckBoxNode)node.getChildAt(i);
      child.setProcessed(processed);
      if (!child.isLeaf()) setProcessedRecursively(child,processed);
    }
}

 // ----- Searchable interface -----
    String previoussearchString="";
    public boolean find(String searchstring) {
         CheckBoxNode rootNode=(CheckBoxNode)getModel().getRoot();
         if (rootNode==null || rootNode.isLeaf()) return false;
         TreePath selectedpath=getSelectionPath();
         CheckBoxNode searchFromNode=null;
         if (selectedpath!=null && previoussearchString.equalsIgnoreCase(searchstring)) {            
             searchFromNode=(CheckBoxNode)selectedpath.getLastPathComponent();
         }
         previoussearchString=searchstring;
         boolean[] passedStartNode=new boolean[]{searchFromNode==null};
         CheckBoxNode matchingnode=findNextRecursively(rootNode, searchFromNode, searchstring, passedStartNode);
         if (matchingnode!=null) {
             TreePath path=new TreePath(matchingnode.getPath());
             setSelectionPath(path);
             scrollPathToVisible(path);
             return true;
         } else {
             previoussearchString=""; // this will reset the search and wrap around
             return false;
         }
    }

    private String getSearchNameFromNode(CheckBoxNode node) {
        return node.getLabel();
    }
    
    /** Searches the children of the given parent node (and their children recursively) to see if any of them matches the searchstring
     *  @return the first node encountered (after searchFromNode) that matches the searchstring (or null)
     */
    private CheckBoxNode findNextRecursively(CheckBoxNode parent, CheckBoxNode searchFromNode, String searchstring, boolean[] passedStartNode) {
        int size=parent.getChildCount();
        for (int i=0;i<size;i++) {
            CheckBoxNode node=(CheckBoxNode)parent.getChildAt(i);
            String nodetext=getSearchNameFromNode(node);
            if (nodetext.matches("(?i).*"+searchstring+".*")) {
                if (passedStartNode[0]) return node;
            }
            if (node==searchFromNode) passedStartNode[0]=true;
            if (!node.isLeaf()) {
               CheckBoxNode found=findNextRecursively(node,searchFromNode,searchstring,passedStartNode);
               if (found!=null) return found;
            }
        }
        return null;
    }

 
private class CheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
   DefaultTreeCellRenderer tcr;
   JCheckBox button = new JCheckBox();
   JLabel label = new JLabel();
   
   public boolean isOnHotspot(int x, int y) {
     return (button.getBounds().contains(x, y));
   }

   public CheckBoxTreeCellRenderer() {
     super();
     tcr=new DefaultTreeCellRenderer();
     label.setOpaque(false);
     button.setOpaque(false);
     setOpaque(false);
     Dimension d=new Dimension(18,18);
     button.setMinimumSize(d);
     button.setPreferredSize(d);
     button.setMaximumSize(d);      
     setLayout(new FlowLayout(FlowLayout.LEADING,0,0));
     add(button);    
     add(label);
   }
 
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded,boolean leaf, int row,boolean hasFocus) {
        if (isSelected) label.setForeground(tcr.getTextSelectionColor());
        else label.setForeground(tcr.getTextNonSelectionColor());
//        button.setIcon(null);
//        button.setSelectedIcon(null);   
        label.setForeground(Color.BLACK);
        String tooltip=null;
        Icon icon=null;
        String labelText="";
        if (value instanceof CheckBoxNode) {
            button.setEnabled(true);
            button.setSelected(((CheckBoxNode)value).isChecked());
            icon=((CheckBoxNode)value).getIcon();
            tooltip=((CheckBoxNode)value).getTooltip();
            labelText=((CheckBoxNode)value).getLabel();
        } else {
            button.setEnabled(false);
            labelText=(value!=null)?value.toString():"";
        }
        label.setText(labelText);
        label.setIcon(icon);
        label.setToolTipText(tooltip);
//        revalidate();
//           
//        setComponentOrientation(tree.getComponentOrientation());	    
        return this;
    }
        
}

}
