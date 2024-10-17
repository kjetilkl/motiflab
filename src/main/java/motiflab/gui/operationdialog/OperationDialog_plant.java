/*
 
 
 */

package motiflab.gui.operationdialog;


import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import motiflab.engine.data.*;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.operations.Operation_plant;
import motiflab.gui.ParametersPanel;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_plant extends OperationDialog {
    private JComboBox sourceDataCombobox=null;
    private JTextField targetDataTextfield=null;
    private JTextField targetSitesTrackTextfield=null;
    private JPanel sourceTargetPanel=null;
    private JPanel sequenceSubsetPanel;
    private JPanel additionalParametersPanel;
    private JPanel motifsPanel;
    private JComboBox sequenceSubsetCombobox;
    private JComboBox motifsComboBox;
    private ParametersPanel parametersPanel=null;
    private ParameterSettings parameterSettings=null;
    private Class[] allowedtypes=new Class[]{MotifCollection.class,ModuleCRM.class,Motif.class};

    public OperationDialog_plant(JFrame parent) {
        super(parent);
        //initComponents();
    }

    public OperationDialog_plant() {
        super();
    }


    @Override
    public void initComponents() {
        super.initComponents();
        setResizable(false);
        setIconImage(gui.getFrame().getIconImage());
        String sourceName=parameters.getSourceDataName();
        parameterSettings=(ParameterSettings)parameters.getParameter(Operation_plant.PARAMETERS);
        if (parameterSettings==null) {
            parameterSettings=new ParameterSettings();
            parameters.setParameter(Operation_plant.PARAMETERS,parameterSettings);
        }
        initSourceTargetPanel();
        initSequenceSubsetPanel();
        initMotifsPanel((String)parameters.getParameter(Operation_plant.MOTIFS_NAME));
        sourceTargetPanel.setBorder(commonBorder);
        sequenceSubsetPanel.setBorder(commonBorder);
        motifsPanel.setBorder(commonBorder);
        additionalParametersPanel=new JPanel();
        additionalParametersPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        additionalParametersPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(motifsPanel);
        JScrollPane scrollPane=new JScrollPane(additionalParametersPanel);
        scrollPane.setMaximumSize(new Dimension(600,400));
        add(scrollPane);
        add(sequenceSubsetPanel);
        add(getOKCancelButtonsPanel());
        if (sourceName!=null && sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedItem(sourceName);
        else {
           if (sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedIndex(0);
           sourceName=(String)sourceDataCombobox.getSelectedItem();
        }
        showParametersPanel(getParameterSettingsPanel());
        pack();
    }
     
    @Override
    protected void setParameters() {
        String sourceName=(String)sourceDataCombobox.getSelectedItem();
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceName);
        String sequenceCollection=(String)sequenceSubsetCombobox.getSelectedItem();
        if (!sequenceSubsetCombobox.isEnabled() || sequenceCollection==null || sequenceCollection.equals(engine.getDefaultSequenceCollectionName())) sequenceCollection=null;
        parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
        if (parametersPanel!=null) {
            parametersPanel.setParameters();
            parameters.setParameter(Operation_plant.PARAMETERS, parametersPanel.getParameterSettings());
        }        
        String targetName=targetDataTextfield.getText();
        if (targetName!=null) targetName=targetName.trim();
        if (targetName==null || targetName.isEmpty()) targetName="SequenceWithPlantedMotifs";
        String targetSitesTrackName=targetSitesTrackTextfield.getText();
        if (targetSitesTrackName!=null) targetSitesTrackName=targetSitesTrackName.trim();
        if (targetSitesTrackName==null || targetSitesTrackName.isEmpty()) targetSitesTrackName="PlantedMotifs";
        String motifsOrModuleName=(String)motifsComboBox.getSelectedItem();
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);
        parameters.setParameter(Operation_plant.TARGET_SITES_TRACK, targetSitesTrackName);
        parameters.setParameter(Operation_plant.MOTIFS_NAME, motifsOrModuleName);
        parameters.reserveDataName(targetSitesTrackName);
        parameters.addAffectedDataObject(targetName, DNASequenceDataset.class);
        parameters.addAffectedDataObject(targetSitesTrackName, RegionDataset.class);
    }

    private void initSequenceSubsetPanel() {
        sequenceSubsetPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        sequenceSubsetPanel.add(new JLabel("In sequence collection  "));
        String selectedName=(String)parameters.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (selectedName==null) selectedName=engine.getDefaultSequenceCollectionName();
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(new Class[]{SequenceCollection.class});
        sequenceSubsetCombobox=new JComboBox(model);
        sequenceSubsetCombobox.setSelectedItem(selectedName);
        sequenceSubsetPanel.add(sequenceSubsetCombobox);
    }


    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel leftVertical=new JPanel();
        JPanel rightVertical=new JPanel();
        leftVertical.setLayout(new BoxLayout(leftVertical, BoxLayout.Y_AXIS));
        rightVertical.setLayout(new BoxLayout(rightVertical, BoxLayout.Y_AXIS));
        JPanel sourcePanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel trackPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel sitesPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));

        Class[] sourceCandidates=parameters.getOperation().getDataSourcePreferences();
        String sourceName=parameters.getSourceDataName();
        String targetName=parameters.getTargetDataName();
        String targetSitesTrackName=(String)parameters.getParameter(Operation_plant.TARGET_SITES_TRACK);

        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("SequenceWithMotifs",getDataTypeTable());
        if (targetSitesTrackName==null || targetSitesTrackName.isEmpty()) targetSitesTrackName=gui.getGenericDataitemName("PlantedMotifs",getDataTypeTable());
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(sourceCandidates);
        sourceDataCombobox=new JComboBox(model);
        if (sourceName!=null && sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedItem(sourceName);
        else {
           if (sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedIndex(0);
           sourceName=(String)sourceDataCombobox.getSelectedItem();
        }
        JPanel tmp=new JPanel();
        sourcePanel.add(new JLabel("Source  "));
        sourcePanel.add(sourceDataCombobox);
        tmp.add(new JLabel("  "));
        leftVertical.add(sourcePanel);
        leftVertical.add(tmp);
        targetSitesTrackTextfield=new JTextField(targetSitesTrackName);
        targetSitesTrackTextfield.setColumns(16);
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(16);
        trackPanel.add(new JLabel("     Store DNA Sequence in  "));
        trackPanel.add(targetDataTextfield);
        sitesPanel.add(new JLabel("     Store sites in  "));
        sitesPanel.add(targetSitesTrackTextfield);
        rightVertical.add(trackPanel);
        rightVertical.add(sitesPanel);
        sourceTargetPanel.add(leftVertical);
        sourceTargetPanel.add(rightVertical);
    }

     private void initMotifsPanel(String motifsName) {
        motifsPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        motifsPanel.add(new JLabel("Motifs or Module  "));
        motifsComboBox=new JComboBox(getDataCandidates(allowedtypes));
        if (motifsName==null){
            if (motifsComboBox.getItemCount()>0) motifsComboBox.setSelectedIndex(0);
        }
        else motifsComboBox.setSelectedItem(motifsName);
        motifsPanel.add(motifsComboBox);

    }


    /** */
    private void showParametersPanel(JPanel panel) {
        additionalParametersPanel.removeAll();
        additionalParametersPanel.add(panel);
    }

    /** */
    private JPanel getParameterSettingsPanel() {
        Operation_plant plantoperation=(Operation_plant)engine.getOperation("plant");
        ParametersPanel panel=new ParametersPanel(plantoperation.getParameters(),parameterSettings, this);
        parametersPanel=panel;
        return panel;
    }


}
