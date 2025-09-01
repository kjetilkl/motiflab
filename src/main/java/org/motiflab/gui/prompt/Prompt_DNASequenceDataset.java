/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.datasource.DataTrack;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.operationdialog.OperationDialog_new;
/**
 *
 * @author kjetikl
 */
public class Prompt_DNASequenceDataset extends Prompt {
    
    private JTextField defaultbaseTextfield;
    private JComboBox markovModelCombobox;
    private JPanel mainpanel;
    private JLabel messageLabel;
    private String parameters = "";
    private JRadioButton useDefaultBase;
    private JRadioButton useMarkovModel;
    private JRadioButton useDataTrack;
    private JComboBox datatrackCombobox;
    
    
    public Prompt_DNASequenceDataset(MotifLabGUI gui, String prompt, String dataname, String parameterString, OperationDialog_new dialog) {
        this(gui,prompt,dataname,parameterString,dialog,true);
    }
    
    public Prompt_DNASequenceDataset(MotifLabGUI gui, String prompt, DNASequenceDataset dataset, boolean modal) {
        this(gui,prompt, (dataset!=null)?dataset.getName():"DNA", (dataset!=null)?dataset.getValueAsParameterString():null, null, modal);
    }     
    
    public Prompt_DNASequenceDataset(MotifLabGUI gui, String prompt, String dataname, String parameterString, OperationDialog_new dialog, boolean modal) {
        super(gui,prompt,modal);
        setDataItemName(dataname);
        setTitle("DNA Sequence Dataset settings");
        mainpanel=new JPanel();
        mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
        //mainpanel.add(valueTextField);
        messageLabel=new JLabel("  ");
        messageLabel.setFont(errorMessageFont);
        messageLabel.setForeground(java.awt.Color.RED);
        useDefaultBase=new JRadioButton("Initialize with default base");
        useMarkovModel=new JRadioButton("Construct from Background Model");
        useDataTrack=new JRadioButton("Construct from Datatrack");
        ButtonGroup group=new ButtonGroup();
        group.add(useDataTrack);
        group.add(useDefaultBase);
        group.add(useMarkovModel);
        DefaultComboBoxModel comboboxmodel;
        if (dialog!=null) comboboxmodel=dialog.getDataCandidates(BackgroundModel.class);
        else {
            ArrayList<Data> list=engine.getAllDataItemsOfType(BackgroundModel.class);
            String[] bgmodelsnames=new String[list.size()];
            for (int i=0;i<list.size();i++) bgmodelsnames[i]=list.get(i).getName();
            comboboxmodel=new DefaultComboBoxModel(bgmodelsnames);
        }
        markovModelCombobox=new JComboBox(comboboxmodel);
        defaultbaseTextfield=new JTextField(3);
        DataTrack[] trackslist=engine.getDataLoader().getAvailableDatatracks(DNASequenceDataset.class);
        String[] tracksnames=new String[trackslist.length];
        for (int i=0;i<trackslist.length;i++) tracksnames[i]=trackslist[i].getName();
        java.util.Arrays.sort(tracksnames);
        datatrackCombobox=new JComboBox(tracksnames);
        JPanel internPanel=new JPanel();
        internPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JPanel gridPanel=new JPanel(new GridLayout(3, 2, 10, 10));
        gridPanel.add(useDataTrack);
        gridPanel.add(datatrackCombobox);       
        gridPanel.add(useMarkovModel);
        gridPanel.add(markovModelCombobox);       
        gridPanel.add(useDefaultBase);
        gridPanel.add(defaultbaseTextfield);
        internPanel.add(gridPanel);        
        internPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        mainpanel.add(internPanel);
        mainpanel.add(messageLabel);
        Dimension size=new Dimension(250,60);
        mainpanel.setMinimumSize(size);
        //mainpanel.setPreferredSize(size);
        setMainPanel(mainpanel);        
        defaultbaseTextfield.setText("N");
        if (parameterString==null || parameterString.isEmpty()) {
            useDataTrack.setSelected(true);
        } else if (parameterString.startsWith(Operation_new.DATA_TRACK_PREFIX)) {
            useDataTrack.setSelected(true);
            if (parameterString.length()>Operation_new.DATA_TRACK_PREFIX.length()) {
                String trackname=parameterString.substring(Operation_new.DATA_TRACK_PREFIX.length());
                datatrackCombobox.setSelectedItem(trackname);
            }
        } else { // track created from background model or a default base
              Class parclass=null;
              if (dialog!=null) parclass=dialog.getClassForDataItem(parameterString);
              else {
                 Data paramdata=engine.getDataItem(parameterString);
                 if (paramdata!=null) parclass=paramdata.getClass();
              }
              if (parclass!=null && parclass==BackgroundModel.class) {
                  markovModelCombobox.setSelectedItem(parameterString);
                  useMarkovModel.setSelected(true);
                  defaultbaseTextfield.setText("N");
              } else {
                  if (parameterString.startsWith("'")) parameterString=parameterString.replaceAll("'", "");
                  if (parameterString.isEmpty()) parameterString="N";
                  defaultbaseTextfield.setText(parameterString.substring(0,1));
                  useDefaultBase.setSelected(true);
              }
        }
        pack();
    }
    
    
    @Override
    public boolean onOKPressed() {
        // no data object is created here, but the parameters string is set to give instruction on how it can be created later        
        if (useDataTrack.isSelected()) {            
            parameters=Operation_new.DATA_TRACK_PREFIX+(String)datatrackCombobox.getSelectedItem();
        } else if (useDefaultBase.isSelected()) {
            String val=defaultbaseTextfield.getText().trim();
            if (val.isEmpty()) val="N";
            parameters="'"+val.substring(0,1)+"'";
        } else if (useMarkovModel.isSelected()) {
            parameters=(String)markovModelCombobox.getSelectedItem();
        } else{
            parameters="";
        }
        return true;
    }
    
    @Override
    public Data getData() {
        // NOTE: there is really no need to create and return a full object, since the only time this prompt is used is by the "new" operation dialog,
        //       and that dialog initiates a new task to create the track based on the defined parameter string.
        //       This method would be required to return a valid data object if the prompt could be used to edit existing data, but that is not the case for feature tracks.
        return null;
//       Data data=null;      
//       Operation_new operationNew=(Operation_new)engine.getOperation("new");
//       OperationTask task=new OperationTask("new object");
//       task.setParameter(Operation_new.DATA_TYPE, DNASequenceDataset.getType());
//       task.setParameter(Operation_new.PARAMETERS, parameters);
//       task.setParameter(OperationTask.TARGET_NAME, getDataItemName());
//       try {
//           data=operationNew.createDataItem(task);
//       } catch (Exception e) {
//           engine.logMessage("ERROR: "+e.getMessage());
//       }
//       if (data==null) {
//          engine.logMessage("ERROR: unable to create new dataset as requested. Returning empty dataset instead");
//          data=new DNASequenceDataset(getDataItemName());
//          ((DNASequenceDataset)data).setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));        
//       }
//       return data;
    }
    
    @Override
    public void setData(Data newdata) {
       return;
    }     
    
    @Override
    public String getValueAsParameterString() {
        return parameters;
    }
    
}