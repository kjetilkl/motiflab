/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.operations.Operation_output;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.gui.InfoDialog;
import org.motiflab.gui.ParametersPanel;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_output extends OperationDialog {
    private JComboBox sourceDataCombobox=null; 
    private JTextField targetDataTextfield=null; 
    private JPanel sourceTargetPanel=null;
    private JPanel sequenceSubsetPanel;
    private JPanel formatPanel;
    private JPanel additionalParametersPanel;
    private JComboBox sequenceSubsetCombobox;
    private JComboBox formatComboBox; 
    private JLabel formatErrorLabel;
    private String formatErrorString=" *** No supported output formats for this data type ***";
    private ParametersPanel parametersPanel=null;
    private ParameterSettings parameterSettings=null;
    private JScrollPane scrollPane;
    private boolean isEditingDirectOutput=false;
    private JPanel directOutputPanel=null;
    private JTextArea directOutputTextArea=null;
    private JTextField directOutputReferencesTextField=null;    
    
    public OperationDialog_output(JFrame parent) {
        super(parent); 
        initComponents();
    }
    
    public OperationDialog_output() {
        super();
    }    
    
    public DefaultComboBoxModel getFormatsForSource(String sourceName) {
         ArrayList<DataFormat> outputformatsList;
         Class sourceClass=getClassForDataItem(sourceName);
         if (sourceClass==null) outputformatsList=engine.getAllDataFormats();
         else outputformatsList=engine.getDataOutputFormats(sourceClass);
         String[] list=new String[outputformatsList.size()];
         int i=0;
         for (DataFormat formatter:outputformatsList) {
             list[i]=formatter.getName();
             i++;
         }
         Arrays.sort(list);
         DefaultComboBoxModel newmodel=new DefaultComboBoxModel(list);
         return newmodel;
    } 
    
    
    
    @Override
    public void initComponents() {
        super.initComponents();  
        setResizable(false);
        setIconImage(gui.getFrame().getIconImage());        
        String directOutputString=(String)parameters.getParameter(Operation_output.DIRECT_OUTPUT); 
        String directOutputReferences=(String)parameters.getParameter(Operation_output.DIRECT_OUTPUT_REFERENCES);
        isEditingDirectOutput=(directOutputString!=null);        
        String formatName=(String)parameters.getParameter(Operation_output.OUTPUT_FORMAT);          
        String sourceName=parameters.getSourceDataName();
        parameterSettings=(ParameterSettings)parameters.getParameter(Operation_output.OUTPUT_FORMAT_PARAMETERS);     
        if (parameterSettings==null) {
            parameterSettings=new ParameterSettings();
            parameters.setParameter(Operation_output.OUTPUT_FORMAT_PARAMETERS,parameterSettings);    
        }
        initSourceTargetPanel();
        if (isEditingDirectOutput) {  
            directOutputPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
            directOutputTextArea=new JTextArea(10,30);
            directOutputPanel.add(new JScrollPane(directOutputTextArea));
            directOutputTextArea.setText(directOutputString);
            add(sourceTargetPanel);
            add(directOutputPanel);
            JPanel internal=new JPanel(new FlowLayout(FlowLayout.LEADING));
            internal.add(new JLabel("References: "));
            directOutputReferencesTextField=new JTextField(20);
            directOutputReferencesTextField.setText(directOutputReferences);
            internal.add(directOutputReferencesTextField);
            add(internal);                       
        } else {
            initFormatPanel(sourceName,formatName);
            initSequenceSubsetPanel();
            formatPanel.setBorder(commonBorder);    
            sourceTargetPanel.setBorder(commonBorder);    
            sequenceSubsetPanel.setBorder(commonBorder);
            additionalParametersPanel=new JPanel();
            additionalParametersPanel.setLayout(new FlowLayout(FlowLayout.LEADING));        
            additionalParametersPanel.setBorder(commonBorder);
            add(sourceTargetPanel);   
            add(formatPanel);
            scrollPane=new JScrollPane(additionalParametersPanel);  
            scrollPane.setMaximumSize(new Dimension(550,320));
            add(scrollPane);
            add(sequenceSubsetPanel);
        }
        add(getOKCancelButtonsPanel());  
        if (!isEditingDirectOutput) {
            if (sourceName!=null && sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedItem(sourceName);
            else {
               if (sourceDataCombobox.getItemCount()>0) sourceDataCombobox.setSelectedIndex(0);
               sourceName=(String)sourceDataCombobox.getSelectedItem();
            }
            // programatic 'click' to show the initial panel (if any applicable)
            if (formatName!=null) formatComboBox.setSelectedItem(formatName); 
        }
        pack();        
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
        String targetName=targetDataTextfield.getText();
        targetName.trim();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName(OutputData.class,getDataTypeTable());;
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);         
        if (isEditingDirectOutput) {
            parameters.removeParameter(OperationTask.SOURCE_NAME);            
            parameters.removeParameter(Operation_output.OUTPUT_FORMAT);
            parameters.removeParameter(Operation_output.OUTPUT_FORMAT_PARAMETERS);               
            parameters.removeParameter(OperationTask.SEQUENCE_COLLECTION_NAME);  
            String outputString=directOutputTextArea.getText();
            parameters.setParameter(Operation_output.DIRECT_OUTPUT, outputString);
            String outputReferences=directOutputReferencesTextField.getText();
            if (outputReferences!=null) outputReferences=outputReferences.trim();
            if (outputReferences.isEmpty()) parameters.removeParameter(Operation_output.DIRECT_OUTPUT_REFERENCES);
            else parameters.setParameter(Operation_output.DIRECT_OUTPUT_REFERENCES,outputReferences);
        } else {
            String sourceName=(String)sourceDataCombobox.getSelectedItem();       
            parameters.setParameter(OperationTask.SOURCE_NAME, sourceName);    
            String sequenceCollection=(String)sequenceSubsetCombobox.getSelectedItem();
            if (!sequenceSubsetCombobox.isEnabled() || sequenceCollection==null || sequenceCollection.equals(engine.getDefaultSequenceCollectionName())) sequenceCollection=null;
            parameters.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
            if (formatComboBox.getItemCount()==0) setOK(false); // if no applicable output formats exists the OK button will effectively just cancel the dialog
            else {
                String outputformatName=(String)formatComboBox.getSelectedItem();
                parameters.setParameter(Operation_output.OUTPUT_FORMAT, outputformatName);
                if (parametersPanel!=null) {
                    parametersPanel.setParameters();
                    parameters.setParameter(Operation_output.OUTPUT_FORMAT_PARAMETERS, parametersPanel.getParameterSettings());
                }
                String affectedTargetName=targetName;
                if (affectedTargetName==null || affectedTargetName.isEmpty() || affectedTargetName.equals(sourceName)) affectedTargetName=engine.getDefaultOutputObjectName(); // this is how it is done in Operation_output!
                parameters.addAffectedDataObject(affectedTargetName, OutputData.class);
            } // end valid output format
            parameters.removeParameter(Operation_output.DIRECT_OUTPUT);            
            parameters.removeParameter(Operation_output.DIRECT_OUTPUT_REFERENCES);
        }
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
    
     private void initFormatPanel(String sourceName, String formatName) {
        formatPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        formatPanel.add(new JLabel("Output format  "));
        formatComboBox=new JComboBox(getFormatsForSource(sourceName));        
        if (formatName==null) {
            Class sourceClass=getClassForDataItem(sourceName);
            if (sourceClass!=null) {
                DataFormat of=engine.getDefaultDataFormat(sourceClass);
                if (of!=null) formatName=of.getName();
            }
        }
        if (formatName==null){
            if (formatComboBox.getItemCount()>0) formatComboBox.setSelectedIndex(0);
        }
        else formatComboBox.setSelectedItem(formatName);
        formatPanel.add(formatComboBox);   
        if (formatComboBox.getItemCount()>0) formatErrorLabel=new JLabel("");
        else formatErrorLabel=new JLabel(formatErrorString);
        formatErrorLabel.setForeground(java.awt.Color.RED);
        formatPanel.add(formatErrorLabel);
        formatComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object selected=formatComboBox.getSelectedItem();
                String selectedSourceName=(String)sourceDataCombobox.getSelectedItem();   
                Class sourceClass=getClassForDataItem(selectedSourceName);
                if (selected!=null) showParametersPanel(getFormatSettingsPanel((String)selected,sourceClass)); 
                else additionalParametersPanel.removeAll();
                pack();
                //setLocation(gui.getFrame().getWidth()/2-getWidth()/2, gui.getFrame().getHeight()/2-getHeight()/2);
            }
        });
        JButton helpDataFormatButton=new JButton();
        if (informationIcon!=null) helpDataFormatButton.setIcon(informationIcon);
        else helpDataFormatButton.setText("Help");        
        helpDataFormatButton.setToolTipText("See HELP page for this data format");
        helpDataFormatButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDataFormatHelp();
            }
        });
        formatPanel.add(helpDataFormatButton);       
    }  
    

    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel sPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel tPanel=new JPanel(new FlowLayout((isEditingDirectOutput)?FlowLayout.LEFT:FlowLayout.RIGHT));
        Class[] sourceCandidates=parameters.getOperation().getDataSourcePreferences();
        String sourceName=parameters.getSourceDataName();
        String targetName=parameters.getTargetDataName();
        //if (targetName==null || targetName.isEmpty()) targetName=engine.getDefaultOutputObjectName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName(OutputData.class,getDataTypeTable());
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
        targetDataTextfield.setColumns(16);
        tPanel.add(new JLabel(((isEditingDirectOutput)?"":"       ")+"Output to  "));
        tPanel.add(targetDataTextfield);  
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                additionalParametersPanel.removeAll(); 
                String selectedSourceName=(String)sourceDataCombobox.getSelectedItem();                
                formatComboBox.setModel(getFormatsForSource(selectedSourceName));                
                if (formatComboBox.getItemCount()>0) {
                    String defaultFormat=null;
                    formatErrorLabel.setText("");
                    Class sourceClass=getClassForDataItem(selectedSourceName);
                    if (sourceClass!=null) {
                       DataFormat of=engine.getDefaultDataFormat(sourceClass);
                       if (of!=null) defaultFormat=of.getName();
                    }
                    if (defaultFormat!=null) formatComboBox.setSelectedItem(defaultFormat);
                    else formatComboBox.setSelectedIndex(0);
                    String selectedFormatName=(String)formatComboBox.getSelectedItem();
                    showParametersPanel(getFormatSettingsPanel((String)selectedFormatName,sourceClass));    
                }
                else {
                    formatErrorLabel.setText(formatErrorString);                            
                }
                String sel=(String)sourceDataCombobox.getSelectedItem();
                Data dataitem=engine.getDataItem(sel);                
                if (dataitem!=null && (dataitem instanceof FeatureDataset)) {
                    sequenceSubsetCombobox.setEnabled(true);
                    sequenceSubsetPanel.setVisible(true);
                } else {
                    sequenceSubsetCombobox.setEnabled(false);
                    sequenceSubsetPanel.setVisible(false);
                }
                pack();
            }
        });  
        if (!isEditingDirectOutput) sourceTargetPanel.add(sPanel);
        sourceTargetPanel.add(tPanel);
    }  
    
    /** */
    private void showParametersPanel(JPanel panel) {
        additionalParametersPanel.removeAll();
        additionalParametersPanel.add(panel);
        additionalParametersPanel.repaint();
    }
   
    /** */
    private JPanel getFormatSettingsPanel(String outputformatName, Class sourceDataclass) {
        DataFormat outputFormat=engine.getDataFormat(outputformatName);
        if (outputFormat==null) {
            parametersPanel=null;
            return new JPanel();
        }
        Parameter[] formatparameters=null;        
        if (Analysis.class.isAssignableFrom(sourceDataclass)) {
            Analysis analysis=engine.getAnalysisForClass(sourceDataclass);
            if (analysis!=null) {
                formatparameters=analysis.getOutputParameters(outputformatName);
                formatparameters=filterOutputParameters(analysis, formatparameters, outputformatName); // discard output parameters that are not applicable for this data format
            } else formatparameters=outputFormat.getParameters();
        }
        else formatparameters=outputFormat.getParameters();
        
        ParametersPanel panel=new ParametersPanel(formatparameters,parameterSettings, this, "output", null);  // do not show parameters that only applies to "input"
        parametersPanel=panel;
        return panel;
    }
    
    
    private Parameter[] filterOutputParameters(Analysis analysis, Parameter[] parameters, String outputformatName) {
        boolean filteringRequired=false;
        for (Parameter parameter:parameters) { // first check if filtering is required or if we can just return the input list unchanged
            if (analysis.getOutputParameterFilter(parameter.getName())!=null) {filteringRequired=true;break;}
        }
        if (filteringRequired) {
            ArrayList<Parameter> newlist=new ArrayList<Parameter>();
             for (Parameter parameter:parameters) { // first check if filtering is required or if we can just return the input list unchanged
                String[] filters=analysis.getOutputParameterFilter(parameter.getName());
                if (filters==null || filters.length==0) newlist.add(parameter);
                else {
                   for (String filter:filters) {
                       if (filter.equals(outputformatName)) {newlist.add(parameter);break;}
                   } 
                }
            }    
            Parameter[] newparameters=new Parameter[newlist.size()];
            newparameters=newlist.toArray(newparameters);
            return newparameters;
        } else return parameters;
    }
    
    private void showDataFormatHelp() {
        Object help=null;
        String error="Help unavailable";
        String dataformatName=(String)formatComboBox.getSelectedItem();
        DataFormat format=engine.getDataFormat(dataformatName);
        if (format!=null) {
            help=format.getHelp(engine);
            error="<h1>"+format.getName()+"</h1><br><br>Detailed documentation for this data format is currently unavailable.";                
        } else help="Unable to determine which data format you refer to...";       
        InfoDialog infodialog=null;        
        if (help instanceof String) infodialog=new InfoDialog(gui, "Help for data format: "+dataformatName, (String)help, 700, 450);
        else if (help instanceof URL) infodialog=new InfoDialog(gui, "Help for data format: "+dataformatName, (URL)help, 700, 450, false);
        infodialog.setErrorMessage(error);
        if (infodialog!=null) infodialog.setVisible(true);
    }       

}
