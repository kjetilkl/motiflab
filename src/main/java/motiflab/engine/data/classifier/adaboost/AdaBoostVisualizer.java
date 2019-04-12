/*
 
 
 */

package motiflab.engine.data.classifier.adaboost;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import motiflab.engine.data.classifier.Classifier;
import motiflab.engine.data.classifier.ClassifierOutput;
import motiflab.engine.data.classifier.ClassifierVisualizer;
import motiflab.engine.data.classifier.ClassifierVisualizerListener;

/**
 *
 * @author kjetikl
 */
public class AdaBoostVisualizer extends ClassifierVisualizer implements ClassifierVisualizerListener {
    private AdaBoost classifier=null;
    private JPanel adaboostpanel;
    private JTable classifiersTable;
    private int selectedFeature=-1;
    private ClassifierVisualizer classifierVisualizer;
    private JPanel visualizerPanel;
    private JLabel visualizerPanelTitle;


   /**
    * Creates a new AdaBoostVisualizer
    */
    public AdaBoostVisualizer(AdaBoost classifier) {
       super();
       this.classifier=classifier;
       this.setLayout(new BorderLayout());
       adaboostpanel=new JPanel(new BorderLayout());
       Object[][] values=new Object[classifier.getNumberOfClassifiers()][2];
       for (int i=0;i<classifier.getNumberOfClassifiers();i++) {
           Classifier componentClassifier=classifier.getClassifier(i);
           values[i][0]=componentClassifier.getName();
           values[i][1]=classifier.getClassifierWeight(componentClassifier);
       }
       classifiersTable=new JTable(values, new String[]{"Classifier","Weight"}) {
           @Override
           public boolean isCellEditable(int row, int column) {return false;}
       };
       classifiersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
       classifiersTable.setColumnSelectionAllowed(false);
       classifiersTable.setRowSelectionAllowed(true);
       classifiersTable.getTableHeader().setReorderingAllowed(false);
       classifiersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedRow=classifiersTable.getSelectedRow();
                showClassifier(selectedRow);
            }
        }
       );
//       classifiersTable.getColumn("Classifier").setMaxWidth(80);
       classifiersTable.getColumn("Weight").setCellRenderer(new DoubleRenderer());
       //classifiersTable.setMaximumSize(new Dimension(200, 2000));
       JScrollPane scrollpane=new JScrollPane(classifiersTable);
       scrollpane.setMaximumSize(new Dimension(200, 2000));
       scrollpane.setPreferredSize(new Dimension(200, 300));
       adaboostpanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
       adaboostpanel.add(scrollpane,BorderLayout.CENTER);
       this.add(adaboostpanel,BorderLayout.WEST);
       visualizerPanelTitle=new JLabel("- None selected -");
       JPanel internal1=new JPanel(new FlowLayout(FlowLayout.CENTER));
       internal1.add(visualizerPanelTitle);
       visualizerPanel=new JPanel(new BorderLayout());
       JPanel internal2=new JPanel(new BorderLayout());
       internal2.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
       visualizerPanel.setBorder(BorderFactory.createEtchedBorder());
       internal2.add(internal1,BorderLayout.NORTH);
       internal2.add(visualizerPanel,BorderLayout.CENTER);
       visualizerPanel.setMinimumSize(new Dimension(300, 300));
       visualizerPanel.setPreferredSize(new Dimension(300, 300));
       this.add(internal2,BorderLayout.CENTER);
       classifiersTable.getSelectionModel().setSelectionInterval(0, 0);
    }

    @Override
    public void finished(ClassifierOutput info) { }

    @Override
    public void round(ClassifierOutput info) { }

    @Override
    public void start(ClassifierOutput info) { }

    @Override
    public void setSelectedFeature(int featureNumber) {
        selectedFeature=featureNumber;
        if (classifierVisualizer!=null) classifierVisualizer.setSelectedFeature(selectedFeature);
    }

    private void showClassifier(int classifierNumber) {
       if (classifierVisualizer!=null) classifierVisualizer.removeListener(this);
       Classifier componentClassifier=classifier.getClassifier(classifierNumber);
       classifierVisualizer=componentClassifier.getVisualizer();
       classifierVisualizer.setSelectedFeature(selectedFeature);
       visualizerPanel.removeAll();
       visualizerPanel.add(classifierVisualizer,BorderLayout.CENTER);
       classifierVisualizer.addListener(this);
       visualizerPanelTitle.setText(componentClassifier.getName());
       this.revalidate();
    }

    @Override
    public void featureSelectedInVisualizer(int featureNumber) {
        notifyListenersOfFeatureSelected(featureNumber);
    }

    private class DoubleRenderer extends DefaultTableCellRenderer {
        private DecimalFormat show2decimals = new DecimalFormat( "#########0.00");
        public DoubleRenderer() {
               super();
               this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        }
        @Override
        public void setValue(Object value) {
               if (value instanceof Double) setText(show2decimals.format(((Double)value).doubleValue()));
               else setText("");
        }
    }//

}
