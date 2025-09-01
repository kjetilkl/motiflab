/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.operations.Operation_physical;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_physical extends FeatureTransformOperationDialog {
    private JPanel propertypanel;
    private JPanel windowpanel;
    private JComboBox physicalPropertyCombobox;
    private JComboBox windowSizeCombobox;
    private JComboBox windowAnchorCombobox;
    private JTextField oligoTextfield;

    
    public OperationDialog_physical(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_physical() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        propertypanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        propertypanel.setBorder(commonBorder);
        propertypanel.add(new JLabel("Physical property "));
        oligoTextfield=new JTextField(5);
        physicalPropertyCombobox = new JComboBox(Operation_physical.getPhysicalProperties());
        physicalPropertyCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String property=physicalPropertyCombobox.getSelectedItem().toString();
                if (property.equals(Operation_physical.FREQUENCY)) {
                    oligoTextfield.setEnabled(true);
                    //property=property+"-"+oligoTextfield.getText().trim();
                } else oligoTextfield.setEnabled(false);
                String trackName=property.replace("-", "_"); // minus signs are not allowed in track names anymore
                trackName=trackName.replace(":", "_"); //
                trackName=trackName.replace(" ", "_"); //
                trackName=gui.getGenericDataitemName(trackName, getDataTypeTable());               
                targetDataTextfield.setText(trackName);                
            }
        });
        String physicalPropertyString=(String)parameters.getParameter(Operation_physical.PHYSICAL_PROPERTY);
        if (physicalPropertyString==null) physicalPropertyString=(String)physicalPropertyCombobox.getModel().getElementAt(0);   
        String oligoString="A";
        if (physicalPropertyString.startsWith(Operation_physical.FREQUENCY+":") && physicalPropertyString.length()>Operation_physical.FREQUENCY.length()+1) {
           oligoString=physicalPropertyString.substring(Operation_physical.FREQUENCY.length()+1);
           physicalPropertyString=Operation_physical.FREQUENCY;
        }
        physicalPropertyCombobox.setSelectedItem(physicalPropertyString);
        propertypanel.add(physicalPropertyCombobox);          
        oligoTextfield.setText(oligoString);
        propertypanel.add(new JLabel("   oligo "));
        propertypanel.add(oligoTextfield);    
        if (physicalPropertyString.equals(Operation_physical.FREQUENCY)) {
            oligoTextfield.setEnabled(true);  
        } else oligoTextfield.setEnabled(false);  
        windowpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        windowpanel.setBorder(commonBorder);
        windowpanel.add(new JLabel("Window size  "));
        DefaultComboBoxModel wbmodel=getDataCandidates(new Class[]{NumericVariable.class,SequenceNumericMap.class});
        windowSizeCombobox = new JComboBox(wbmodel);
        windowSizeCombobox.setEditable(true);
        windowpanel.add(windowSizeCombobox);                   
        String windowSizeString=(String)parameters.getParameter(Operation_physical.WINDOW_SIZE);
        if (windowSizeString==null) windowSizeString="10";
        windowSizeCombobox.setSelectedItem(windowSizeString);
        windowAnchorCombobox=new JComboBox(Operation_physical.getWindowAnchors());
        String windowAnchor=(String)parameters.getParameter(Operation_physical.ANCHOR);
        if (windowAnchor==null || windowAnchor.isEmpty()) windowAnchor=(String)Operation_physical.CENTER;
        windowAnchorCombobox.setSelectedItem(windowAnchor);
        windowpanel.add(new JLabel("    anchor  "));
        windowpanel.add(windowAnchorCombobox);
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(propertypanel);
        add(windowpanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        String property=(String)physicalPropertyCombobox.getSelectedItem();
        if (property.equals(Operation_physical.FREQUENCY)) {
            property=property+":"+oligoTextfield.getText().trim();
        } 
        parameters.setParameter(Operation_physical.PHYSICAL_PROPERTY, property);
        parameters.setParameter(Operation_physical.WINDOW_SIZE, (String)windowSizeCombobox.getSelectedItem());
        parameters.setParameter(Operation_physical.ANCHOR, (String)windowAnchorCombobox.getSelectedItem());
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
