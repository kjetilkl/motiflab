/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.PriorsGenerator;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_predict extends OperationDialog {
    private JPanel nameAndTypePanel;
    private JPanel priorsGeneratorPanel;
    private JTextField targetDataTextfield;

    private JComboBox priorsGeneratorCombobox;




    public OperationDialog_predict(JFrame parent) {
        super(parent);
    }

    public OperationDialog_predict() {
        super();
    }

    @Override
    public void initComponents() {
        super.initComponents();
        nameAndTypePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        nameAndTypePanel.setBorder(commonBorder);
        nameAndTypePanel.add(new JLabel("Target dataset name "));
        targetDataTextfield=new JTextField();
        nameAndTypePanel.add(targetDataTextfield);
        targetDataTextfield.setColumns(16);
        String currentdataname=(String)parameters.getParameter(OperationTask.TARGET_NAME);
        if (currentdataname==null || currentdataname.isEmpty()) currentdataname=gui.getGenericDataitemName("Priors",getDataTypeTable());
        targetDataTextfield.setText(currentdataname);

        DefaultComboBoxModel priorsGeneratorComboboxModel=getDataCandidates(PriorsGenerator.class);
        priorsGeneratorCombobox=new JComboBox(priorsGeneratorComboboxModel);
        priorsGeneratorCombobox.setEditable(true);
        String currentSource=(String)parameters.getSourceDataName();
        if (currentSource!=null && !currentSource.isEmpty()) priorsGeneratorCombobox.setSelectedItem(currentSource);
        priorsGeneratorPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        priorsGeneratorPanel.setBorder(commonBorder);
        priorsGeneratorPanel.add(new JLabel("  Priors Generator"));
        priorsGeneratorPanel.add(priorsGeneratorCombobox);

        add(nameAndTypePanel);
        add(priorsGeneratorPanel);
        add(getOKCancelButtonsPanel());
        pack();
    }

    @Override
    protected void setParameters() {
        //super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        String targetName=targetDataTextfield.getText().trim();
        if (targetName.isEmpty()) targetName="priors";
        parameters.setParameter(OperationTask.TARGET_NAME,targetName);
        String priorsGeneratorName=(String)priorsGeneratorCombobox.getSelectedItem();
        parameters.setParameter(OperationTask.SOURCE_NAME,priorsGeneratorName);
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
