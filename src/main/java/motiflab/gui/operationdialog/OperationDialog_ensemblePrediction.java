/*
 
 
 */

package motiflab.gui.operationdialog;


import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.operations.Operation_ensemblePrediction;
import motiflab.external.EnsemblePredictionMethod;
import motiflab.external.ExternalProgram;
import motiflab.gui.InfoDialog;
import motiflab.gui.ParametersPanel;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_ensemblePrediction extends OperationDialog {
    private JList sourcesList;
    private JTextField targetDataTextfield=null;
    private JTextField motifCollectionTextfield=null;
    private JTextField motifprefixTextfield=null;
    private JPanel sourceTargetPanel=null;
    private JPanel sequenceSubsetPanel;
    private JPanel algorithmPanel;
    private JPanel additionalParametersPanel;
    private JComboBox sequenceSubsetCombobox;
    private JComboBox algorithmComboBox;
    private JComboBox dnaTrackComboBox;    
    private ParametersPanel parametersPanel=null;
    private ParameterSettings parameterSettings=null;
    private String[] initialEditValues=new String[4]; // if set to other than NULL, this array contains the original algorithm, trackname, motifcollection name and names of additional results (the latter could be a comma-separated string) for a protocol line that is being edited
    private JTextField additionalResultsTextfield=null;
    private JLabel additionalResultsLabel;    
    private JPanel additionalResultsPanel;   
    private JPanel trackPanel;
    private JPanel motifsPanel;
    private static final int textfieldsize=16;
    
    public OperationDialog_ensemblePrediction(JFrame parent) {
        super(parent);
        //initComponents();
    }

    public OperationDialog_ensemblePrediction() {
        super();
    }

    public DefaultComboBoxModel getAlgorithms() {
         String[] list=engine.getAvailableEnsemblePredictionAlgorithms();
         DefaultComboBoxModel newmodel=new DefaultComboBoxModel(list);
         return newmodel;
    }



    @Override
    public void initComponents() {
        super.initComponents();
        setResizable(false);
        setIconImage(gui.getFrame().getIconImage());
        String algorithmName=(String)parameters.getParameter(Operation_ensemblePrediction.ALGORITHM);
        String motifprefix=(String)parameters.getParameter(Operation_ensemblePrediction.MOTIFPREFIX);      
        String sourceName=parameters.getSourceDataName();
        parameterSettings=(ParameterSettings)parameters.getParameter(Operation_ensemblePrediction.PARAMETERS);
        if (parameterSettings==null) {
            parameterSettings=new ParameterSettings();
            parameters.setParameter(Operation_ensemblePrediction.PARAMETERS,parameterSettings);
        }
        initAlgorithmPanel(sourceName,algorithmName,motifprefix);
        initSourceTargetPanel();
        initSequenceSubsetPanel();
        algorithmPanel.setBorder(commonBorder);
        sourceTargetPanel.setBorder(commonBorder);
        sequenceSubsetPanel.setBorder(commonBorder);
        additionalParametersPanel=new JPanel();
        additionalParametersPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        additionalParametersPanel.setBorder(commonBorder);
        add(sourceTargetPanel);
        add(algorithmPanel);
        JScrollPane scrollPane=new JScrollPane(additionalParametersPanel);
        scrollPane.setMaximumSize(new Dimension(600,400));
        add(scrollPane);
        add(sequenceSubsetPanel);
        add(getOKCancelButtonsPanel());
        Object formatSelected=algorithmComboBox.getSelectedItem();
        algorithmComboBox.setSelectedItem(formatSelected); // programatic 'click' to show the initial panel (if any applicable)
        pack();
        if (sourceName!=null && sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedItem(sourceName);
        else {
           if (sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedIndex(0);
           sourceName=(String)sourceDataCombobox.getSelectedItem();
        }
        if (motifprefix!=null && motifprefixTextfield!=null) {
            motifprefixTextfield.setText(motifprefix);
            motifprefixTextfield.setCaretPosition(0);
        }

    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension dim=super.getPreferredSize();
        //if (dim.width>510) dim.width=510; // enforce a maximum size on the dialog
        if (dim.height>500) dim.height=500; // enforce a maximum size on the dialog
        return dim;
    }    

    @Override
    protected void setParameters() {
        Object[] selections=sourcesList.getSelectedValues();
        String sourceNames="";
        for (int i=0;i<selections.length;i++) {
            if (i<selections.length-1) sourceNames+=(String)selections[i]+",";
            else sourceNames+=(String)selections[i];
        }
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceNames);
        String sequenceCollection=(String)sequenceSubsetCombobox.getSelectedItem();
        if (!sequenceSubsetCombobox.isEnabled() || sequenceCollection==null || sequenceCollection.equals(engine.getDefaultSequenceCollectionName())) sequenceCollection=null;
        parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
        String algorithmName="ensemblePrediction";
        String prefix=algorithmName;
        if (algorithmComboBox.getItemCount()==0) setOK(false); // if no algorithms exists the OK button will effectively just cancel the dialog
        else {
            algorithmName=(String)algorithmComboBox.getSelectedItem();
            if (motifprefixTextfield!=null) prefix=(String)motifprefixTextfield.getText().trim();
            if (prefix.isEmpty()) prefix=algorithmName;
            parameters.setParameter(Operation_ensemblePrediction.ALGORITHM, algorithmName);
            parameters.setParameter(Operation_ensemblePrediction.MOTIFPREFIX, prefix);
            if (parametersPanel!=null) {
                parametersPanel.setParameters();
                parameters.setParameter(Operation_ensemblePrediction.PARAMETERS, parametersPanel.getParameterSettings());
            }
        }
        EnsemblePredictionMethod algorithm=(EnsemblePredictionMethod)engine.getExternalProgram(algorithmName);         
        String targetName=targetDataTextfield.getText();
        if (targetName!=null) targetName=targetName.trim();
        if (targetName==null || targetName.isEmpty()) targetName=algorithmName+"_sites";
        String motifCollectionName=motifCollectionTextfield.getText();
        if (motifCollectionName!=null) motifCollectionName=motifCollectionName.trim();
        if (motifCollectionName==null || motifCollectionName.isEmpty()) motifCollectionName=algorithmName+"_motifs";
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);
        parameters.setParameter(Operation_ensemblePrediction.MOTIFCOLLECTION, motifCollectionName);
        if (algorithm.returnsMotifResults()) parameters.reserveDataName(motifCollectionName);
        if (algorithm.returnsMotifResults()) parameters.addAffectedDataObject(motifCollectionName, MotifCollection.class);
        if (algorithm.returnsSiteResults()) parameters.addAffectedDataObject(targetName, RegionDataset.class);                
        String dnaTrackName=(String)dnaTrackComboBox.getSelectedItem();
        parameters.setParameter(Operation_ensemblePrediction.DNA_TRACK_NAME, dnaTrackName);
        if (additionalResultsPanel.isVisible()) { // this should ensure that a valid motif discovery program is selected
            String addResNamesString=additionalResultsTextfield.getText().trim();
            if (!addResNamesString.isEmpty()) { // we have already checked that the number of names is correct (in "checkForErrors()" )
                parameters.setParameter(Operation_ensemblePrediction.ADDITIONAL_RESULTS, addResNamesString);
                String[] addResNames=addResNamesString.split("\\s*,\\s*");                
                for (int i=0;i<addResNames.length;i++) {
                    String resName=addResNames[i].trim();
                    Class paramtype=(algorithm!=null)?algorithm.getTypeForResultParameter(i+2):null;
                    parameters.reserveDataName(resName); // the target name is reserved automatically
                    parameters.addAffectedDataObject(resName, paramtype);                    
                }
            } else parameters.removeParameter(Operation_ensemblePrediction.ADDITIONAL_RESULTS);
        } else parameters.removeParameter(Operation_ensemblePrediction.ADDITIONAL_RESULTS);      
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

     private void initAlgorithmPanel(String sourceName, String algorithmName, String prefix) {
        algorithmPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        algorithmPanel.add(new JLabel("Method  "));
        algorithmComboBox=new JComboBox(getAlgorithms());
        if (algorithmName==null){
            String defaultprogram=gui.getDefaultExternalProgram("ensembleprediction");
            if (defaultprogram!=null) algorithmComboBox.setSelectedItem(defaultprogram);
            else if(algorithmComboBox.getItemCount() > 0) algorithmComboBox.setSelectedIndex(0);
            algorithmName=(String)algorithmComboBox.getSelectedItem();
        }
        else algorithmComboBox.setSelectedItem(algorithmName);
        algorithmPanel.add(algorithmComboBox);
        algorithmComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selected=(String)algorithmComboBox.getSelectedItem();
                if (selected!=null) {
                    showParametersPanel(getAlgorithmSettingsPanel(selected));
                    if (motifprefixTextfield!=null) {
                        motifprefixTextfield.setText(selected);
                        motifprefixTextfield.setCaretPosition(0);
                    }
                    if (initialEditValues[0]!=null && (selected).equals(initialEditValues[0])) {
                       targetDataTextfield.setText(initialEditValues[1]);
                       motifCollectionTextfield.setText(initialEditValues[2]);
                       additionalResultsTextfield.setText(initialEditValues[3]);                       
                    } else {
                       targetDataTextfield.setText(gui.getGenericDataitemName("BindingSites_"+selected,getDataTypeTable()));
                       motifCollectionTextfield.setText(gui.getGenericDataitemName("Motifs_"+selected,getDataTypeTable()));
                       additionalResultsTextfield.setText(getDefaultNamesForAdditionalResults(selected));                    
                    }
                    EnsemblePredictionMethod algorithm=(EnsemblePredictionMethod)engine.getExternalProgram(selected);
                    int additionalresults=algorithm.getNumberOfAdditionalResults();
                    if (additionalresults>0) {
                       additionalResultsPanel.setVisible(true);
                       additionalResultsLabel.setText("Additional results ("+additionalresults+")  ");
                    } else additionalResultsPanel.setVisible(false);
                    motifsPanel.setVisible(algorithm.returnsMotifResults());
                    trackPanel.setVisible(algorithm.returnsSiteResults());
                }
                else {
                    additionalParametersPanel.removeAll();
                    additionalResultsPanel.setVisible(false);
                }                    
                pack();
                //setLocation(gui.getFrame().getWidth()/2-getWidth()/2, gui.getFrame().getHeight()/2-getHeight()/2);
            }
        });
        JButton algorithmHelpButton=new JButton(this.informationIcon);
        algorithmHelpButton.setToolTipText("Show description of algorithm");
        algorithmPanel.add(algorithmHelpButton);
        algorithmHelpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedAlgorithm=(String)algorithmComboBox.getSelectedItem();
                if (selectedAlgorithm==null || selectedAlgorithm.isEmpty()) return;
                ExternalProgram externalprogram=engine.getExternalProgram(selectedAlgorithm);
                if (externalprogram==null) return;
                gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                String document=externalprogram.getProgramDocumentation();
                InfoDialog dialog=new InfoDialog(gui, "External Program", document);
                gui.getFrame().setCursor(Cursor.getDefaultCursor());
                dialog.setVisible(true);
                dialog.dispose();
            }
        });
        if (prefix==null) prefix=algorithmName;
        motifprefixTextfield=new JTextField(10);
        motifprefixTextfield.setText(prefix);
        motifprefixTextfield.setCaretPosition(0);
        algorithmPanel.add(new JLabel("        Motif prefix  "));
        algorithmPanel.add(motifprefixTextfield);
    }


    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        //JPanel leftVertical=new JPanel();
        JPanel rightVertical=new JPanel();
        //leftVertical.setLayout(new BoxLayout(leftVertical, BoxLayout.Y_AXIS));
        rightVertical.setLayout(new BoxLayout(rightVertical, BoxLayout.Y_AXIS));
        JPanel sourcePanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        trackPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        motifsPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel dnaPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));        
        String algorithmName=(String)algorithmComboBox.getSelectedItem();
        Class[] sourceCandidates=parameters.getOperation().getDataSourcePreferences();
        String sourceNames=parameters.getSourceDataName();
        String targetName=parameters.getTargetDataName();
        String dnaTrack=(String)parameters.getParameter(Operation_ensemblePrediction.DNA_TRACK_NAME);          
        String motifCollectionName=(String)parameters.getParameter(Operation_ensemblePrediction.MOTIFCOLLECTION);
        initialEditValues[0]=(String)parameters.getParameter(Operation_ensemblePrediction.ALGORITHM);
        initialEditValues[1]=targetName;
        initialEditValues[2]=motifCollectionName;
        initialEditValues[3]=(String)parameters.getParameter(Operation_ensemblePrediction.ADDITIONAL_RESULTS);                
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("BindingSites_"+algorithmName,getDataTypeTable());
        if (motifCollectionName==null || motifCollectionName.isEmpty()) motifCollectionName=gui.getGenericDataitemName("Motifs_"+algorithmName,getDataTypeTable());
        DefaultComboBoxModel dnamodel=getDataCandidates(DNASequenceDataset.class);
        dnaTrackComboBox=new JComboBox(dnamodel);
        if (dnaTrack!=null && !dnaTrack.isEmpty()) dnaTrackComboBox.setSelectedItem(dnaTrack);
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
        } else {
            sourcesList.setSelectionInterval(0, sourcesList.getModel().getSize()-1);
        }
        Dimension d = new Dimension(160,100);
        JScrollPane sourcescrollpane=new JScrollPane(sourcesList);
        sourcescrollpane.setPreferredSize(d);
        sourcescrollpane.setMinimumSize(d);
        sourcescrollpane.setMaximumSize(d);
        sourcePanel.add(new JLabel("Sources   "));
        sourcePanel.add(sourcescrollpane);

        motifCollectionTextfield=new JTextField(motifCollectionName);
        motifCollectionTextfield.setColumns(textfieldsize);
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(textfieldsize);
        trackPanel.add(new JLabel("        Store sites in  "));
        trackPanel.add(targetDataTextfield);
        motifsPanel.add(new JLabel("       Store motifs in  "));
        motifsPanel.add(motifCollectionTextfield);
        dnaPanel.add(new JLabel("       DNA-track  "));
        dnaPanel.add(dnaTrackComboBox);                   
        rightVertical.add(trackPanel);
        rightVertical.add(motifsPanel);
        initAdditionalResultsPanel();
        rightVertical.add(additionalResultsPanel);        
        rightVertical.add(dnaPanel);        
        sourceTargetPanel.add(sourcePanel);
        sourceTargetPanel.add(rightVertical);       
    }




    /** */
    private void showParametersPanel(JPanel panel) {
        additionalParametersPanel.removeAll();
        additionalParametersPanel.add(panel);
    }

    /** */
    private JPanel getAlgorithmSettingsPanel(String algorithmName) {
        EnsemblePredictionMethod algorithm=(EnsemblePredictionMethod)engine.getExternalProgram(algorithmName);
        if (algorithm==null) {
            parametersPanel=null;
            return new JPanel();
        }
        ParametersPanel panel=new ParametersPanel(algorithm.getParameters(),parameterSettings, this);
        parametersPanel=panel;
        return panel;
    }

    
    private void initAdditionalResultsPanel() {
        additionalResultsPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        additionalResultsLabel=new JLabel("Additional results",this.informationIcon,JLabel.TRAILING);
        additionalResultsTextfield=new JTextField(textfieldsize);
        additionalResultsTextfield.setText("");
        additionalResultsPanel.add(additionalResultsLabel);
        additionalResultsPanel.add(additionalResultsTextfield);
        additionalResultsLabel.setToolTipText("<html>This program can return a number of additional data objects "
                + "(the exact number is shown in parenthesis).<br>"
                + "If you want to keep these additional objects, you must specify names for all of them.<br>"
                + "If you don't want these additional results, you can leave the textfield empty.</html>");       
    }
    
    private String getDefaultNamesForAdditionalResults(String algorithmname) {
        EnsemblePredictionMethod algorithm=(EnsemblePredictionMethod)engine.getExternalProgram(algorithmname);
        ArrayList<Parameter> additional=algorithm.getAdditionalResultsParameters();
        if (!additional.isEmpty()) {
        String[] names=new String[additional.size()];
        for (int i=0;i<names.length;i++) {
            String paramname=additional.get(i).getName();
            paramname=MotifLabEngine.convertToLegalDataName(paramname);
            names[i]=gui.getGenericDataitemName(paramname, getDataTypeTable());
        }
        return MotifLabEngine.splice(names,",");
        } else return ""; // no additional parameters
    }
    
    @Override
    protected String checkForErrors() {
        if (additionalResultsPanel.isVisible()) {
            String algorithmname=(String)algorithmComboBox.getSelectedItem();
            if (algorithmname!=null) {   
                EnsemblePredictionMethod algorithm=(EnsemblePredictionMethod)engine.getExternalProgram(algorithmname);
                int additional=algorithm.getNumberOfAdditionalResults();
                String addResNames=additionalResultsTextfield.getText().trim();
                if (addResNames.isEmpty()) return null;
                String[] res=addResNames.split("\\s*,\\s*");
                if (res.length!=additional) return "The selected program must have either 0 or "+additional+" name(s) for additional result objects.";
            }
        }   
        return null; // no errors to report
    }      

}
