
package motiflab.gui.operationdialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_boolean;
import motiflab.engine.operations.Condition_position_boolean;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Condition_region_boolean;
import motiflab.engine.protocol.StandardOperationParser;
import motiflab.engine.protocol.StandardOperationParser_new;

/**
 *
 * @author kjetikl
 */
public class ConditionPanel_boolean extends JPanel {
    
    protected OperationDialog dialog;
    private Condition condition=null;  
    private JTree tree;
    private StandardOperationParser parser;
    
    
    public ConditionPanel_boolean(Condition condition, OperationDialog dialog) {
        this.dialog=dialog;
        parser=new StandardOperationParser_new(); // just to be able to parse conditions
        if (condition==null) {} // this should not happen
        else if (condition instanceof Condition_boolean) {
           this.condition=condition;
        } else if (condition instanceof Condition_position) {
           this.condition=new Condition_position_boolean(Condition_boolean.AND);
           ((Condition_position_boolean)this.condition).addCondition((Condition_position)condition);
        } else if (condition instanceof Condition_region) {
           this.condition=new Condition_region_boolean(Condition_boolean.AND);
           ((Condition_region_boolean)this.condition).addCondition((Condition_region)condition);            
        }
        init();
        this.setToolTipText("Use context menu to edit this condition");
    }
    
    /**
     * Initializes the values in the panel based on the supplied Condition
     */
    private void init() {       
        DefaultTreeModel treemodel=getTreeModelFromCondition(condition);
        tree=new JTree(treemodel);
        tree.setCellRenderer(new ConditionTreeCellRenderer());
        Dimension size=new Dimension(420,70);
        JScrollPane scrollPane=new JScrollPane(tree);
        setLayout(new FlowLayout(FlowLayout.LEADING));
        scrollPane.setPreferredSize(size);
        scrollPane.setMinimumSize(size);
        scrollPane.setMaximumSize(size);        
        add(scrollPane, BorderLayout.CENTER);
        expandAll(tree,true);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseEvent(e); 
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseEvent(e); 
            }            
        });         
    }
    
    private void mouseEvent(java.awt.event.MouseEvent evt) {
        int row = tree.getRowForLocation(evt.getX(), evt.getY());
        if (row<0) return;
        tree.setSelectionRow(row);
        TreePath path=tree.getPathForRow(row);
        if (evt.getID()==MouseEvent.MOUSE_PRESSED && evt.getButton()==MouseEvent.BUTTON1 && evt.getClickCount()==2) {
            Condition oldCondition=getConditionFromPath(path);
                if (!(oldCondition instanceof Condition_boolean)) {
                Condition newCondition=editCondition(oldCondition);
                if (newCondition!=oldCondition) changeCondition(path,newCondition); 
            }
            return;
        }
        if (evt.isPopupTrigger()) {
            JPopupMenu popup=new ConditionContextMenu(path);
            if (popup!=null) popup.show(evt.getComponent(), evt.getX(),evt.getY());
        }  
    }      
    
    private DefaultTreeModel getTreeModelFromCondition(Condition condition) {
        DefaultTreeModel model=new DefaultTreeModel(getNodeForCompoundCondition(condition));
        return model;
    }
    
    private DefaultMutableTreeNode getNodeForCompoundCondition(Condition condition) {
         if (condition instanceof Condition_boolean) {
           DefaultMutableTreeNode node=new DefaultMutableTreeNode(condition);
           ArrayList<? extends Condition> level=((Condition_boolean)condition).getConditions();
           for (int i=0;i<level.size();i++) {
               Object element=level.get(i);
               if (element instanceof Condition_boolean) {
                   node.add(getNodeForCompoundCondition((Condition)element));                   
               } else if (element instanceof Condition) { // single leaf condition
                   node.add(new DefaultMutableTreeNode(element));
               }
           }
           return node;
        } 
        return new DefaultMutableTreeNode("ERROR:"+((condition==null)?"null":condition.getClass()));       
    }
    
    private Condition getConditionFromModel(DefaultTreeModel model) {
        return (Condition)((DefaultMutableTreeNode)model.getRoot()).getUserObject();
    }
    
    public Condition getCondition() {
        Condition cond=getConditionFromModel((DefaultTreeModel)tree.getModel());
        return cleanup(cond);
    }
    
    /** Takes a condition as input and returns a condition where compound subconditions containing only a single clause are replaced by the single clause */
    private Condition cleanup(Condition condition) {
        if (condition instanceof Condition_boolean) {
           Condition_boolean boolCond=(Condition_boolean)condition;
           if (boolCond.size()==0) { // This is empty
               return null;
           } else if (boolCond.size()==1) { // This does not have to be a compound. Flatten the condition
               return cleanup(boolCond.getConditions().get(0)); 
           } else {
               ArrayList<? extends Condition> list=(ArrayList<? extends Condition>)boolCond.getConditions().clone();
               for (int i=0;i<list.size();i++) {
                  Condition child=list.get(i);
                  if (child instanceof Condition_boolean) {
                     Condition cleanchild=cleanup(child);
                     if (cleanchild==null) boolCond.removeCondition(i);
                     else if (cleanchild!=child) boolCond.replaceCondition(i, cleanchild);                    
                  }
               }
               if (boolCond.size()==0) return null; // all children were removed when cleaning
               else return condition;
           }           
        }
        return condition;
    }    
    
    private void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode)tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }
    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode)parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e=node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode)e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) tree.expandPath(parent);
        else tree.collapsePath(parent); 
    }    
    
    
    private Condition editCondition(Condition condition) {
        OperationTask whereparameters=new OperationTask("Condition");   
        whereparameters.setParameter("where", (condition!=null)?condition.clone():null);
        whereparameters.setParameter("_popup_where",Boolean.TRUE); // a flag to indicate that the condition panel is a popup         
        whereparameters.setParameter(OperationTask.OPERATION_NAME, (condition==null)?"Add Condition":"Edit Condition");
        EditConditionDialog editdialog=new EditConditionDialog();       
        editdialog.initialize(whereparameters, dialog.getDataTypeTable(), dialog.gui);
        Point location=ConditionPanel_boolean.this.getLocationOnScreen();                
        editdialog.setLocation(location);
        editdialog.setVisible(true);
        boolean okpressed=editdialog.okPressed();
        editdialog.dispose();
        if (okpressed) {
           return (Condition)whereparameters.getParameter("where");
        } else return condition;
    }
    
    private class ConditionTreeCellRenderer extends DefaultTreeCellRenderer {
        private Font boldFont;
        private Font normalFont;
        
        public ConditionTreeCellRenderer() {
            super();      
//            MiscIcons lineicon=new MiscIcons(MiscIcons.TREE_LINE);
//            lineicon.setForegroundColor(Color.LIGHT_GRAY);
            setLeafIcon(null);
            setClosedIcon(null);
            setOpenIcon(null);
        }
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            String nodeText="ERROR:";
            DefaultMutableTreeNode node=(DefaultMutableTreeNode)value;
            Condition nodeCondition=(Condition)node.getUserObject();
            if (nodeCondition instanceof Condition_boolean) {
                int type=((Condition_boolean)nodeCondition).getOperatorType();
                nodeText=(type==Condition_boolean.AND)?"AND":"OR";
            } else {
                nodeText=parser.getCommandString_condition(nodeCondition); 
            }
            Component comp=super.getTreeCellRendererComponent(tree, nodeText, selected, expanded, leaf, row, hasFocus);
            if (normalFont==null) {
                Font font=comp.getFont();
                normalFont=font;
                boldFont=font.deriveFont(Font.BOLD);             
            }
            comp.setFont((nodeCondition instanceof Condition_boolean)?boldFont:normalFont);
            return comp;
        }
    }    
    
    /** Adds a new single subcondition to a boolean condition node located at the given node in the tree */
    private void addSubCondition(TreePath path, Condition newCondition) { 
        Condition cond=getConditionFromPath(path);
        if (cond instanceof Condition_boolean) {
             ((Condition_boolean)cond).addCondition(newCondition);
             DefaultMutableTreeNode node=(DefaultMutableTreeNode)path.getLastPathComponent();
             int currentCount=node.getChildCount();
             node.add(new DefaultMutableTreeNode(newCondition));
             ((DefaultTreeModel)tree.getModel()).nodesWereInserted((TreeNode)path.getLastPathComponent(), new int[]{currentCount}); // added one child to the end of this parent             
        }
    }
    
    /** Removes the condition at the given location in the tree from its parent boolean compound condition node */
    private void removeCondition(TreePath path) {
        TreePath parentpath=path.getParentPath();
        if (parentpath==null) return; // do not remove the root node!
        DefaultMutableTreeNode nodeToRemove=(DefaultMutableTreeNode)path.getLastPathComponent();
        Condition parentcond=getConditionFromPath(parentpath);
        if (parentcond instanceof Condition_boolean) {
              DefaultMutableTreeNode parentnode=(DefaultMutableTreeNode)parentpath.getLastPathComponent();
              int index=parentnode.getIndex(nodeToRemove);
              if (index<0) return;
              ((Condition_boolean)parentcond).removeCondition(index);
              parentnode.remove(index);
             ((DefaultTreeModel)tree.getModel()).nodesWereRemoved(parentnode, new int[]{index}, new Object[]{nodeToRemove});              
        }
    } 
       
    /** Changes the operator type (AND/OR) of the compound condition at the given location
     *  in the tree to a new type
     */
    private void changeOperatorInCondition(TreePath path, int newtype) {
        Condition cond=getConditionFromPath(path);
        if (cond!=null) {
            if (cond instanceof Condition_boolean) ((Condition_boolean)cond).setOperatorType(newtype);
            else System.err.println("ERROR: Condtion == "+cond.getClass().getSimpleName());
            ((DefaultTreeModel)tree.getModel()).nodeChanged((TreeNode)path.getLastPathComponent());            
        } else System.err.println("ERROR:Condtion==null in changeOperatorInCondition:"+path);         
    }
    
    /** Replaces the current condition at the given node in tree by a new compound condition
     *  of the given type and makes the old condition the first entry in this new compound
     */
    private void addToNewGroupOperatorInCondition(TreePath path, int type) {
        Condition oldCondition=getConditionFromPath(path);
        if (oldCondition!=null) {
            DefaultMutableTreeNode nodeToRemove=(DefaultMutableTreeNode)path.getLastPathComponent();            
            TreePath parentpath=path.getParentPath();
            if (parentpath==null) { // the condition is the root node
                  // setup new group condition
                  Condition_boolean newGroupCondition=null;
                  if (oldCondition instanceof Condition_position) newGroupCondition=new Condition_position_boolean(type);
                  else newGroupCondition=new Condition_region_boolean(type);
                  newGroupCondition.addCondition(oldCondition);
                  DefaultMutableTreeNode groupNode=new DefaultMutableTreeNode(newGroupCondition);
                  groupNode.add(nodeToRemove);
                  ((DefaultTreeModel)tree.getModel()).setRoot(groupNode);
                  ((DefaultTreeModel)tree.getModel()).nodeStructureChanged(groupNode);
                  expandAll(tree, true);  
            } else {                
                Condition parentcond=getConditionFromPath(parentpath);
                if (parentcond instanceof Condition_boolean) {
                      DefaultMutableTreeNode parentnode=(DefaultMutableTreeNode)parentpath.getLastPathComponent();
                      int index=parentnode.getIndex(nodeToRemove);
                      if (index<0) return; // not found
                      ((Condition_boolean)parentcond).removeCondition(index);
                      parentnode.remove(index);
                      Condition_boolean newGroupCondition=null;
                      if (oldCondition instanceof Condition_position) newGroupCondition=new Condition_position_boolean(type);
                      else newGroupCondition=new Condition_region_boolean(type);
                      newGroupCondition.addCondition(oldCondition);
                      ((Condition_boolean)parentcond).addCondition((Condition)newGroupCondition,index); // add new boolean condition back to parent in same location as the old one
                      DefaultMutableTreeNode groupNode=new DefaultMutableTreeNode(newGroupCondition);
                      groupNode.add(nodeToRemove); // add old condition node as a child to new group node
                      parentnode.insert(groupNode,index);// add new group node to the tree
                      ((DefaultTreeModel)tree.getModel()).nodeStructureChanged(parentnode);
                      expandAll(tree, parentpath, true);                     
                } else {System.err.println("ERROR: Parent condition not boolean");return;}                  
            }            
        } else System.err.println("ERROR:Condtion==null in addToNewGroupOperatorInCondition:"+path);         
     }    
    
    
    
    private void changeCondition(TreePath path, Condition newCondition) {
        Condition current=getConditionFromPath(path);
        if (current instanceof Condition_boolean) {
           // not allowed
        } else {
            current.importCondition(newCondition);     
           ((DefaultTreeModel)tree.getModel()).nodeChanged((TreeNode)path.getLastPathComponent());        
        }
    }    
    
    private Condition getConditionFromPath(TreePath path) {
        return (Condition)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
    }
    
    
    private class ConditionContextMenu extends JPopupMenu  {       
        public ConditionContextMenu(TreePath path) {
            MenuListener listener=new MenuListener(path);
            Condition cond=getConditionFromPath(path);
            if (cond==null) return;
            boolean isRoot=(path.getParentPath()==null);
            if (cond instanceof Condition_boolean) {
                JMenuItem addItem=new JMenuItem("Add New Condition to Group");
                addItem.addActionListener(listener);
                this.add(addItem); 
                JMenuItem splitAndItem=new JMenuItem("Add to New AND Group");
                splitAndItem.addActionListener(listener);
                this.add(splitAndItem);
                JMenuItem splitORItem=new JMenuItem("Add to New OR Group");
                splitORItem.addActionListener(listener);
                this.add(splitORItem); 
                if (!isRoot) {
                    JMenuItem removeItem=new JMenuItem("Remove Group");
                    removeItem.addActionListener(listener);
                    this.add(removeItem);  
                }
                boolean isAND=(getTypeFromCondition(cond)==Condition_boolean.AND);
                JMenuItem changeItem=new JMenuItem("Change Operator to: "+((isAND)?"OR":"AND"));
                changeItem.addActionListener(listener);
                this.add(changeItem);                
            } else { // A regular condition
                JMenuItem editItem=new JMenuItem("Edit Condition");
                editItem.addActionListener(listener);
                this.add(editItem);
                JMenuItem splitAndItem=new JMenuItem("Add to New AND Group");
                splitAndItem.addActionListener(listener);
                this.add(splitAndItem);
                JMenuItem splitORItem=new JMenuItem("Add to New OR Group");
                splitORItem.addActionListener(listener);
                this.add(splitORItem);
                JMenuItem removeItem=new JMenuItem("Remove Condition");
                removeItem.addActionListener(listener);
                if (!isRoot) this.add(removeItem);                
            }            
        }
    }
    
    private int getTypeFromCondition(Condition cond) {
       if (cond instanceof Condition_boolean) return ((Condition_boolean)cond).getOperatorType();
       else {
           System.err.println("ERROR: expected boolean condition in '' but got "+cond.getClass().getSimpleName());
           return -1;
       }
    }
    
    private class MenuListener implements ActionListener {
        private TreePath path;
        
        public MenuListener(TreePath path) {
            this.path=path;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd=e.getActionCommand();
            if (cmd.equals("Add New Condition to Group")) {
                Condition newCondition=editCondition(null);
                if (newCondition!=null) addSubCondition(path,newCondition);               
            } else if (cmd.equals("Edit Condition")) {                
                Condition oldCondition=getConditionFromPath(path);
                Condition newCondition=editCondition(oldCondition);
                if (newCondition!=oldCondition) changeCondition(path,newCondition);                  
            } else if (cmd.equals("Remove Condition")) {                
                removeCondition(path);                  
            } else if (cmd.equals("Remove Group")) {                
                removeCondition(path);                  
            } else if (cmd.startsWith("Change Operator to")) {
                int newtype=(cmd.endsWith("AND"))?Condition_boolean.AND:Condition_boolean.OR;
                changeOperatorInCondition(path,newtype);
            } else if (cmd.startsWith("Add to New")) {
                int newtype=(cmd.equals("Add to New AND Group"))?Condition_boolean.AND:Condition_boolean.OR;
                addToNewGroupOperatorInCondition(path,newtype);
            }
        }
        
    }
    
    
    private class EditConditionDialog extends FeatureTransformOperationDialog {
        
        public EditConditionDialog() {
            super();         
        }
        
        @Override
        protected void setParameters() {
            ConditionPanel condpanel=getConditionPanel();
            if (condpanel!=null) {
                Condition condition=condpanel.getCondition();
                parameters.setParameter("where", condition);
            } else parameters.setParameter("where", null);            
        }
        
        @Override
        public void initComponents() {
            super.initComponents();
            Condition editcondition=(Condition)this.parameters.getParameter("where");
            int type=-1;
            if (editcondition==null) {
                if (condition instanceof Condition_position) type=FeatureTransformOperationDialog.CONDITION_NUMERIC;
                else type=FeatureTransformOperationDialog.CONDITION_REGION;
            } else type=(editcondition instanceof Condition_position)?FeatureTransformOperationDialog.CONDITION_NUMERIC:FeatureTransformOperationDialog.CONDITION_REGION;
            JPanel whereClausePanel=getWhereClausePanel(type);
            add(whereClausePanel);
            add(getOKCancelButtonsPanel());
            pack();        
        }       
    }

}
