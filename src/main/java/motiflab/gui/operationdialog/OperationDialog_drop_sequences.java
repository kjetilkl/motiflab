/*


 */

package motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_drop_sequences extends OperationDialog {
    private JPanel sourcePanel;
    private JComboBox sourceCombobox=null;  

    public OperationDialog_drop_sequences(JFrame parent) {
        super(parent);
    }

    public OperationDialog_drop_sequences() {
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
        String sourceNames=(String)sourceCombobox.getSelectedItem();
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceNames);
        parameters.setBlockGUI(true);
        parameters.setTurnOffGUInotifications(true);
    }

    private void initSourcePanel() {
        sourcePanel=new JPanel();
        sourcePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        String sourceName=parameters.getSourceDataName();
        sourcePanel.add(new JLabel("Drop sequences   "));
        DefaultComboBoxModel model=getDataCandidates(SequenceCollection.class);
        model.removeElement(engine.getDefaultSequenceCollectionName());
        sourceCombobox=new JComboBox(model);
        if (sourceName!=null && !sourceName.isEmpty()) sourceCombobox.setSelectedItem(sourceName);
        sourcePanel.add(sourceCombobox);
    }

}
