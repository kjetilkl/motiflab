/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
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
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.RegionDataset;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_combine_regions extends FeatureTransformOperationDialog {
    private JPanel operatorpanel;
    private JComboBox operatorCombobox;
    private JPanel sourceTargetPanel;
    private JList sourcesList;
    
    public OperationDialog_combine_regions(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_combine_regions() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();              
        initSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
 
        Object[] selections=sourcesList.getSelectedValues();
        String sourceNames="";
        for (int i=0;i<selections.length;i++) {
            if (i<selections.length-1) sourceNames+=(String)selections[i]+",";
            else sourceNames+=(String)selections[i];
        }
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceNames);
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, RegionDataset.class);
    }
    
    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel sPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel tPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Class[] sourceCandidates=parameters.getOperation().getDataSourcePreferences();
        String sourceNames=parameters.getSourceDataName();
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("Combination",getDataTypeTable());
        DefaultListModel model=(DefaultListModel)getDataCandidatesAsListModel(sourceCandidates);
        sourceDataCombobox=new JComboBox(new String[]{"<error>"}); 
        sourceDataCombobox.setSelectedIndex(0);
        sourcesList=new JList(model);
        sourcesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);        
        if (sourceNames!=null && !sourceNames.isEmpty()) {
            String[] sources=sourceNames.trim().split("\\s*,\\s*");
            for (String name:sources) {
                int index=model.indexOf(name);
                if (index<-1) model.addElement(name);
            }
            int[] indices=new int[sources.length];
            for (int i=0;i<sources.length;i++) {
                indices[i]=model.indexOf(sources[i]);
            }
            sourcesList.setSelectedIndices(indices);
        }
        Dimension d = new Dimension(160,100);
        JScrollPane sourcescrollpane=new JScrollPane(sourcesList);
        sourcescrollpane.setPreferredSize(d);
        sourcescrollpane.setMinimumSize(d);
        sourcescrollpane.setMaximumSize(d);
        sPanel.add(new JLabel("Sources   "));
        sPanel.add(sourcescrollpane);
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(16);
        tPanel.add(new JLabel("       Store results in  "));
        tPanel.add(targetDataTextfield);         
        sourceTargetPanel.add(sPanel);
        sourceTargetPanel.add(tPanel);
    }    

}
