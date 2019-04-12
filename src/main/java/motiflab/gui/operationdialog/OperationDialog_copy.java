/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_copy extends FeatureTransformOperationDialog {
  
    
    public OperationDialog_copy(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_copy() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();                   
        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        String sourceName=parameters.getSourceDataName();
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName="copy_"+((sourceName!=null)?sourceName:"empty");
        targetDataTextfield.setText(targetName);
        // remove the sourceDataCombobox-listener that was installed in super.initComponents();      
        for (ActionListener listener:sourceDataCombobox.getActionListeners()) {
            sourceDataCombobox.removeActionListener(listener);  
        }  
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sourceName=(String)sourceDataCombobox.getSelectedItem();
                targetDataTextfield.setText("copy_"+((sourceName!=null && !sourceName.isEmpty())?sourceName:"empty"));
            }
        });         
        add(sourceTargetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }


    @Override
    protected void setParameters() {
        super.setParameters(); // sets target name (and more)
        String targetName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        String sourceName=(String)parameters.getParameter(motiflab.engine.task.OperationTask.SOURCE_NAME); // this should have been set in super.setParameters() above
        Class sourcetype=getClassForDataItem(sourceName);
        parameters.addAffectedDataObject(targetName, sourcetype);
    }
}
