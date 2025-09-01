/*


 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_delete extends FeatureTransformOperationDialog {
    private JPanel operatorpanel;
    private JComboBox operatorCombobox;
    private JPanel sourcePanel;
    private JTextField sourceTextfield;

    public OperationDialog_delete(JFrame parent) {
        super(parent);
    }

    public OperationDialog_delete() {
        super();
    }

    @Override
    public void initComponents() {
        super.initComponents();
        initSourcePanel();
        sourcePanel.setBorder(commonBorder);
        add(sourcePanel);
        add(getOKCancelButtonsPanel());
        pack();
    }

    @Override
    protected void setParameters() {
        String sourceNames=sourceTextfield.getText();
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceNames);
    }

    private void initSourcePanel() {
        sourcePanel=new JPanel();
        sourcePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        String sourceNames=parameters.getSourceDataName();
        sourcePanel.add(new JLabel("Delete   "));
        sourceTextfield=new JTextField(sourceNames);
        sourceTextfield.setColumns(16);
        sourcePanel.add(sourceTextfield);
    }

}
