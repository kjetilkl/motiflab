/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import motiflab.engine.data.ModuleNumericMap;
import motiflab.engine.data.MotifNumericMap;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.operations.Operation_combine_numeric;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_combine_numeric extends FeatureTransformOperationDialog {
    private JPanel operatorpanel;
    private JComboBox operatorCombobox;
    private JPanel sourceTargetPanel;
    private JList sourcesList;
    private JComboBox sourceTypeCombobox;
    private JPanel whereClausePanel;
    private JPanel subsetPanel;
    
    public OperationDialog_combine_numeric(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_combine_numeric() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        operatorpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        operatorpanel.setBorder(commonBorder);
        operatorpanel.add(new JLabel("Combine using  "));
        String operatorString=(String)parameters.getParameter(Operation_combine_numeric.OPERATOR);
        if (operatorString==null) operatorString="sum";
        if (operatorString.equals("minimum")) operatorString="min";
        if (operatorString.equals("maximum")) operatorString="max";    
        operatorCombobox = new JComboBox(new String[]{"sum","product","average","min","max"});
        operatorCombobox.setEditable(false);
        operatorCombobox.setSelectedItem(operatorString);
        operatorpanel.add(operatorCombobox);                   
        initSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(operatorpanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());       
        pack();    
        String sourceNames=parameters.getSourceDataName();
        initializeSources(sourceNames);                
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        parameters.setParameter(Operation_combine_numeric.OPERATOR, (String)operatorCombobox.getSelectedItem());
 
        Object[] selections=sourcesList.getSelectedValues();
        String sourceNames="";
        for (int i=0;i<selections.length;i++) {
            if (i<selections.length-1) sourceNames+=(String)selections[i]+",";
            else sourceNames+=(String)selections[i];
        }
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceNames);
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        Class sourceType=getSelectedType();
        parameters.addAffectedDataObject(targetName, sourceType);
    }
    
    private void initSourceTargetPanel() {
        sourceTypeCombobox = new JComboBox(new String[]{"Numeric Datasets","Motif Numeric Maps","Module Numeric Maps","Sequence Numeric Maps","Numeric Variables"});
        sourceTypeCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Class sourceType=getSelectedType();
                setSourceType(sourceType);
            }
        });
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel sPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel tPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("Combination",getDataTypeTable());
        sourceDataCombobox=new JComboBox(new String[]{"<error>"}); // this widget from the superclass is not displayed in this dialog
        sourceDataCombobox.setSelectedIndex(0);
        sourcesList=new JList();     
        Dimension d = new Dimension(160,100);
        JScrollPane sourcescrollpane=new JScrollPane(sourcesList);
        sourcescrollpane.setPreferredSize(d);
        sourcescrollpane.setMinimumSize(d);
        sourcescrollpane.setMaximumSize(d);
        sPanel.add(new JLabel("Sources   "));
        sPanel.add(sourcescrollpane);
        sPanel.add(new JLabel("     Source type   "));
        sPanel.add(sourceTypeCombobox);
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(16);
        tPanel.add(new JLabel("       Store results in  "));
        tPanel.add(targetDataTextfield);         
        sourceTargetPanel.add(sPanel);
        sourceTargetPanel.add(tPanel);
    }    

    private void setSourceType(Class classtype) {
        String sourceNames=parameters.getSourceDataName();
        DefaultListModel model=(DefaultListModel)getDataCandidatesAsListModel(new Class[]{classtype});
        sourcesList.setModel(model);
        sourcesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);     
        // select current sources
        if (sourceNames!=null && !sourceNames.isEmpty()) {
            String[] sources=sourceNames.trim().split("\\s*,\\s*");
            // add selected sources not already present in the list?
//            for (String name:sources) {
//                int index=model.indexOf(name);
//                if (index<-1) model.addElement(name); 
//            }
            ArrayList<Integer> selected=new ArrayList<>(sources.length);
            for (int i=0;i<sources.length;i++) {
                int index=model.indexOf(sources[i]);
                if (index>=0) selected.add(index);
            }
            int[] indices=new int[selected.size()];
            for (int i=0;i<selected.size();i++) {
                indices[i]=selected.get(i);
            }
            sourcesList.setSelectedIndices(indices);
        }    
        boolean isNumericDataset=(classtype==NumericDataset.class);
        if (whereClausePanel!=null) whereClausePanel.setVisible(isNumericDataset);          
        if (subsetPanel!=null) subsetPanel.setVisible(isNumericDataset);         
        pack();        
    }
    
    private void initializeSources(String sourceNames) {
        if (sourceNames!=null && !sourceNames.isEmpty()) {
            String[] sources=sourceNames.trim().split("\\s*,\\s*");
            if (sources.length>0) {
                Class sourceType=getClassForDataItem(sources[0]);
                if (sourceType==MotifNumericMap.class) sourceTypeCombobox.setSelectedItem("Motif Numeric Maps");
                else if (sourceType==ModuleNumericMap.class) sourceTypeCombobox.setSelectedItem("Module Numeric Maps");
                else if (sourceType==SequenceNumericMap.class) sourceTypeCombobox.setSelectedItem("Sequence Numeric Maps");
                else if (sourceType==NumericVariable.class) sourceTypeCombobox.setSelectedItem("Numeric Variables");
                else sourceTypeCombobox.setSelectedItem("Numeric Datasets");
                setSourceType(sourceType); // this could be redundant?
            } else {
                sourceTypeCombobox.setSelectedItem("Numeric Datasets");
                setSourceType(NumericDataset.class); // this could be redundant?               
            }
        } else { // default  
            sourceTypeCombobox.setSelectedItem("Numeric Datasets");
            setSourceType(NumericDataset.class); // this could be redundant
        }       
    }
    
    private Class getSelectedType() {
        String selected=(String)sourceTypeCombobox.getSelectedItem();
             if (selected.equals("Numeric Datasets")) return NumericDataset.class;
        else if (selected.equals("Motif Numeric Maps")) return MotifNumericMap.class;
        else if (selected.equals("Module Numeric Maps")) return ModuleNumericMap.class;
        else if (selected.equals("Sequence Numeric Maps")) return SequenceNumericMap.class;
        else if (selected.equals("Numeric Variables")) return NumericVariable.class;
        else return NumericDataset.class;        
    }
}
