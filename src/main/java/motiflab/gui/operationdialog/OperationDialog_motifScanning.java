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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.operations.Operation_motifScanning;
import motiflab.external.ExternalProgram;
import motiflab.external.MotifScanning;
import motiflab.gui.InfoDialog;
import motiflab.gui.ParametersPanel;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_motifScanning extends OperationDialog {
    private JComboBox sourceDataCombobox=null; 
    private JTextField targetDataTextfield=null; 
    private JPanel sourceTargetPanel=null;
    private JPanel sequenceSubsetPanel;
    private JPanel algorithmPanel;
    private JPanel additionalParametersPanel;
    private JComboBox sequenceSubsetCombobox;
    private JComboBox algorithmComboBox; 
    private ParametersPanel parametersPanel=null;
    private ParameterSettings parameterSettings=null; // the algorithm specific parameter settings
    private String[] initialEditValues=new String[3]; // if set to other than NULL, this array contains the original algorithm, TFBS trackname and names of additional results (the latter could be a comma-separated string) for a protocol line that is being edited
    private JTextField additionalResultsTextfield=null;
    private JLabel additionalResultsLabel;    
    private JPanel additionalResultsPanel;   
    private static final int textfieldsize=16;
    
    public OperationDialog_motifScanning(JFrame parent) {
        super(parent); 
        //initComponents();
    }
    
    public OperationDialog_motifScanning() {
        super();
    }    
    
    public DefaultComboBoxModel getMotifScanningAlgorithms() {
         String[] list=engine.getAvailableMotifScanningAlgorithms();
         DefaultComboBoxModel newmodel=new DefaultComboBoxModel(list);
         return newmodel;
    } 
      
    
    @Override
    public void initComponents() {
        super.initComponents();  
        setResizable(false);
        setIconImage(gui.getFrame().getIconImage());        
        String algorithmName=(String)parameters.getParameter(Operation_motifScanning.ALGORITHM);
        String sourceName=parameters.getSourceDataName();
        parameterSettings=(ParameterSettings)parameters.getParameter(Operation_motifScanning.PARAMETERS);   
        if (parameterSettings==null) {
            parameterSettings=new ParameterSettings();
            parameters.setParameter(Operation_motifScanning.PARAMETERS,parameterSettings);    
        }
        initAlgorithmPanel(sourceName,algorithmName);
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
        scrollPane.setMaximumSize(new Dimension(500,420));
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
        String sourceName=(String)sourceDataCombobox.getSelectedItem();       
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceName);
        String targetName=targetDataTextfield.getText();
        targetName.trim();
        if (targetName==null || targetName.isEmpty()) targetName=sourceName;
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);        
        String sequenceCollection=(String)sequenceSubsetCombobox.getSelectedItem();
        if (!sequenceSubsetCombobox.isEnabled() || sequenceCollection==null || sequenceCollection.equals(engine.getDefaultSequenceCollectionName())) sequenceCollection=null;
        parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
        String algorithmName="";
        if (algorithmComboBox.getItemCount()==0) setOK(false); // if no algorithms exists the OK button will effectively just cancel the dialog
        else {
            algorithmName=(String)algorithmComboBox.getSelectedItem();
            parameters.setParameter(Operation_motifScanning.ALGORITHM, algorithmName);
            if (parametersPanel!=null) {
                parametersPanel.setParameters();
                parameters.setParameter(Operation_motifScanning.PARAMETERS, parametersPanel.getParameterSettings());
            }
            parameters.addAffectedDataObject(targetName, RegionDataset.class);
        } 
        if (additionalResultsPanel.isVisible()) { // this should ensure that a valid motif discovery program is selected
            String addResNamesString=additionalResultsTextfield.getText().trim();
            if (!addResNamesString.isEmpty()) { // we have already checked that the number of names is correct (in "checkForErrors()" )
                ExternalProgram algorithm=engine.getExternalProgram(algorithmName);
                parameters.setParameter(Operation_motifScanning.ADDITIONAL_RESULTS, addResNamesString);
                String[] addResNames=addResNamesString.split("\\s*,\\s*");                
                for (int i=0;i<addResNames.length;i++) {
                    String resName=addResNames[i].trim();
                    Class paramtype=(algorithm!=null)?algorithm.getTypeForResultParameter(i+1):null; // +1 since the first result object is the TFBS track
                    parameters.reserveDataName(resName); // the target name is reserved automatically
                    parameters.addAffectedDataObject(resName, paramtype);                    
                }
            } else parameters.removeParameter(Operation_motifScanning.ADDITIONAL_RESULTS);
        } else parameters.removeParameter(Operation_motifScanning.ADDITIONAL_RESULTS);      
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
    
     private void initAlgorithmPanel(String sourceName, String algorithmName) {
        algorithmPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        algorithmPanel.add(new JLabel("Method  "));
        algorithmComboBox=new JComboBox(getMotifScanningAlgorithms());        
        if (algorithmName==null){
            String defaultprogram=gui.getDefaultExternalProgram("motifscanning");
            if (defaultprogram!=null) algorithmComboBox.setSelectedItem(defaultprogram);
            else if(algorithmComboBox.getItemCount() > 0) algorithmComboBox.setSelectedIndex(0);
        }
        else algorithmComboBox.setSelectedItem(algorithmName);
        algorithmPanel.add(algorithmComboBox);   
        algorithmComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selected=(String)algorithmComboBox.getSelectedItem();
                if (selected!=null) {
                    if (initialEditValues[0]!=null && (selected).equals(initialEditValues[0])) {
                       targetDataTextfield.setText(initialEditValues[1]);
                       additionalResultsTextfield.setText(initialEditValues[2]);                       
                    } else {
                        targetDataTextfield.setText(gui.getGenericDataitemName("BindingSites_"+selected,getDataTypeTable()));
                        additionalResultsTextfield.setText(getDefaultNamesForAdditionalResults(selected));
                    }
                    showParametersPanel(getAlgorithmSettingsPanel(selected));
                    MotifScanning algorithm=(MotifScanning)engine.getExternalProgram(selected);
                    int additionalresults=algorithm.getNumberOfAdditionalResults();
                    if (additionalresults>0) {
                       additionalResultsPanel.setVisible(true);
                       additionalResultsLabel.setText("Additional results ("+additionalresults+")  ");
                    } else additionalResultsPanel.setVisible(false);                    
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
    }  
    

    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel rightVertical=new JPanel();
        rightVertical.setLayout(new BoxLayout(rightVertical, BoxLayout.Y_AXIS));        
        JPanel sPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel tPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Class[] sourceCandidates=parameters.getOperation().getDataSourcePreferences();
        String sourceName=parameters.getSourceDataName();
        String targetName=parameters.getTargetDataName();
        String algorithmName=(String)algorithmComboBox.getSelectedItem();
        initialEditValues[0]=(String)parameters.getParameter(Operation_motifScanning.ALGORITHM);
        initialEditValues[1]=targetName;
        initialEditValues[2]=(String)parameters.getParameter(Operation_motifScanning.ADDITIONAL_RESULTS);
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("BindingSites_"+algorithmName,getDataTypeTable());
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(sourceCandidates);
        sourceDataCombobox=new JComboBox(model);
        if (sourceName!=null && sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedItem(sourceName);
        else {
           if (sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedIndex(0);
           sourceName=(String)sourceDataCombobox.getSelectedItem();
        }
        sPanel.add(new JLabel("Source  "));
        sPanel.add(sourceDataCombobox);
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(textfieldsize);
        tPanel.add(new JLabel("       Store results in  "));
        tPanel.add(targetDataTextfield);  
        sourceTargetPanel.add(sPanel);
        initAdditionalResultsPanel();
        rightVertical.add(tPanel);          
        rightVertical.add(additionalResultsPanel);             
        sourceTargetPanel.add(rightVertical);
    }  
    
    /** */
    private void showParametersPanel(JPanel panel) {
        additionalParametersPanel.removeAll();
        additionalParametersPanel.add(panel);        
    }
   
    /** */
    private JPanel getAlgorithmSettingsPanel(String algorithmName) {
        MotifScanning algorithm=(MotifScanning)engine.getExternalProgram(algorithmName);
        if (algorithm==null) {
            parametersPanel=null;
            return new JPanel();
        }
        String proxyMotifCollection=(String)parameters.getParameter(Operation_motifScanning.PROXY_SOURCE_MOTIFCOLLECTION);
        Data motifcollection=(proxyMotifCollection!=null)?engine.getDataItem(proxyMotifCollection):null;
        if (motifcollection instanceof MotifCollection) { // try to infuse proxy
            String mcolParameterName=algorithm.getNameOfFirstRegularParameterForType(MotifCollection.class);
            if (mcolParameterName!=null) {
                parameterSettings.setParameter(mcolParameterName, motifcollection);
            }
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
        MotifScanning algorithm=(MotifScanning)engine.getExternalProgram(algorithmname);
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
                MotifScanning algorithm=(MotifScanning)engine.getExternalProgram(algorithmname);
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
