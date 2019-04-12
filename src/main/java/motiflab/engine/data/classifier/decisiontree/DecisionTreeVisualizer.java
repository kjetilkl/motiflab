/*
 
 
 */

package motiflab.engine.data.classifier.decisiontree;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Enumeration;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import motiflab.engine.data.classifier.ClassifierOutput;
import motiflab.engine.data.classifier.ClassifierVisualizer;
import motiflab.engine.data.classifier.ClassifierVisualizerListener;

/**
 *
 * @author kjetikl
 */
public class DecisionTreeVisualizer extends ClassifierVisualizer implements ClassifierVisualizerListener {

    private Font visfont;
    private Font visfontBold;
    private JScrollPane panel;
    private DecisionTreeClassifier classifier;

    /** Creates a new instance of DecisionTreeVisualizer */
    public DecisionTreeVisualizer(DecisionTreeClassifier classifier) {
       this.classifier=classifier;
       initialize();
       visfont=new Font("Monospaced",Font.PLAIN,12);
       visfontBold=new Font("Monospaced",Font.BOLD,12);
   }

   private void initialize() {
       this.setSize(500, 500); // this is the size of the JFrame
       this.setLayout(new BorderLayout());
       DecisionTreeNode root=classifier.getRoot();
       DecisionTreeModel model=new DecisionTreeModel(root);
       JTree tree=new JTree(model);
       tree.putClientProperty("JTree.lineStyle","None");
       tree.setCellRenderer(new MyCellRenderer());
       expandTree(tree,root);
       panel = new JScrollPane(tree);
       this.add(panel);
       tree.repaint();
   }

  @Override
  public void round(ClassifierOutput info) {
//     if (info.get(Classifier.DECISION_TREE)==null) return;
//     DecisionTreeNode root=(DecisionTreeNode)info.get(Classifier.DECISION_TREE);
//     DecisionTreeModel model=new DecisionTreeModel(root);
//     JTree tree=new JTree(model);
//     tree.putClientProperty("JTree.lineStyle","None");
//     tree.setCellRenderer(new MyCellRenderer());
//     expandTree(tree,root);
//     panel = new JScrollPane(tree);
//     this.add(panel);
//     tree.repaint();
//     SwingUtilities.invokeLater(new Runnable() {
//       public void run() {
//            panel.repaint();
//        }
//     });
  }
  @Override
  public void finished(ClassifierOutput info) {}
  @Override
  public void start(ClassifierOutput info) {round(info);}

 private void expandTree(JTree tree, DefaultMutableTreeNode start) {
	for (Enumeration children = start.children(); children.hasMoreElements();) {
		DefaultMutableTreeNode dtm = (DefaultMutableTreeNode) children.nextElement();
		if (!dtm.isLeaf()) {
			TreePath tp = new TreePath( dtm.getPath() );
			tree.expandPath(tp);
			expandTree(tree, dtm);
		}
	}
	return;
}

  class MyCellRenderer extends DefaultTreeCellRenderer {
      private Color DarkGreen=new Color(0,200,0);
      private Color LightRed=new Color(255,50,50);

      @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
             JPanel panel=new JPanel();
             panel.setLayout(new BorderLayout());
             JLabel label1=new JLabel();
             JLabel label2=new JLabel();
             label1.setOpaque(true);
             label2.setOpaque(true);
             panel.add(label1,BorderLayout.WEST);
             panel.add(label2,BorderLayout.EAST);
             label1.setBackground(Color.WHITE);
             label2.setBackground(Color.WHITE);
             label1.setFont(visfont);
                 label1.setText(((DecisionTreeNode)value).getBranchAttributeValue()+" --> ");
             if (leaf) {
                 label2.setForeground(DarkGreen);
                 label2.setFont(visfontBold);
                 label2.setText(((DecisionTreeNode)value).getClassLabel());
             } else {
                 label2.setFont(visfont);
                 label2.setBackground(LightRed);
                 label2.setText("  "+((DecisionTreeNode)value).getBranchAttribute()+"  ");

             }
             return panel;
        }


  }

    @Override
    public void setSelectedFeature(int featureNumber) {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void featureSelectedInVisualizer(int featureNumber) {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

}
