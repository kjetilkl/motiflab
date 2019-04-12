/*
 
 
 */

package motiflab.gui.prompt;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.datasource.DataTrack;
import motiflab.engine.operations.Operation_new;
import motiflab.gui.MotifLabGUI;

/**
 *
 * @author kjetikl
 */
public class Prompt_NumericDataset extends Prompt {
    
    private JTextField defaultvalueTextField;
    private JTextField minTextField;
    private JTextField maxTextField;
    private JTextField baselineTextField;
    private JPanel mainpanel;
    private JLabel messageLabel;
    private String parameters = "";
    private JRadioButton useDataTrackRadioButton;
    private JRadioButton useValuesRadioButton;
    private JComboBox datatracksCombobox;
    
    public Prompt_NumericDataset(MotifLabGUI gui, String prompt, String dataname, String parameterString) {
        this(gui,prompt,dataname, parameterString, true);
    }  
    
    public Prompt_NumericDataset(MotifLabGUI gui, String prompt, NumericDataset dataset, boolean modal) {
        this(gui,prompt, (dataset!=null)?dataset.getName():"NumericDataset", (dataset!=null)?dataset.getValueAsParameterString():null, modal);
    }     
    
    public Prompt_NumericDataset(MotifLabGUI gui, String prompt, String dataname, String parameterString, boolean modal) {
        super(gui,prompt, modal);
        setDataItemName(dataname);
        setTitle("Numeric Dataset settings");
        defaultvalueTextField=new JTextField(3);
        minTextField=new JTextField(3);
        maxTextField=new JTextField(3);
        baselineTextField=new JTextField(3);
        mainpanel=new JPanel();
        mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
        //mainpanel.add(valueTextField);
        messageLabel=new JLabel("  ");
        messageLabel.setFont(errorMessageFont);
        messageLabel.setForeground(java.awt.Color.RED);
        useDataTrackRadioButton=new JRadioButton("DataTrack  ");
        useValuesRadioButton=new JRadioButton("Default value ");
        ButtonGroup group=new ButtonGroup();
        group.add(useDataTrackRadioButton);
        group.add(useValuesRadioButton);
        JPanel internPanel=new JPanel();
        internPanel.setLayout(new BoxLayout(internPanel, BoxLayout.Y_AXIS));
        JPanel tracksPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        DataTrack[] trackslist=engine.getDataLoader().getAvailableDatatracks(NumericDataset.class);
        String[] tracksnames=new String[trackslist.length];
        for (int i=0;i<trackslist.length;i++) tracksnames[i]=trackslist[i].getName();
        java.util.Arrays.sort(tracksnames);
        datatracksCombobox=new JComboBox(tracksnames);
        tracksPanel.add(useDataTrackRadioButton);
        tracksPanel.add(datatracksCombobox);
        JPanel valuesPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        valuesPanel.add(useValuesRadioButton);
        valuesPanel.add(defaultvalueTextField);
        valuesPanel.add(new JLabel("  Min "));
        valuesPanel.add(minTextField);
        valuesPanel.add(new JLabel("  Max "));
        valuesPanel.add(maxTextField);
        valuesPanel.add(new JLabel("  Baseline "));
        valuesPanel.add(baselineTextField);        
        internPanel.add(tracksPanel);
        internPanel.add(valuesPanel);
        mainpanel.add(internPanel);
        mainpanel.add(messageLabel);
        Dimension size=new Dimension(250,60);
        mainpanel.setMinimumSize(size);
        //mainpanel.setPreferredSize(size);
        setMainPanel(mainpanel);    
        minTextField.setText("0");
        maxTextField.setText("1");
        baselineTextField.setText("0");
        defaultvalueTextField.setText("0");
        if (parameterString==null || parameterString.isEmpty()) {
            useDataTrackRadioButton.setSelected(true);
        } else if (parameterString.startsWith(Operation_new.DATA_TRACK_PREFIX)) {
            useDataTrackRadioButton.setSelected(true);
            if (parameterString.length()>Operation_new.DATA_TRACK_PREFIX.length()) {
                String trackname=parameterString.substring(Operation_new.DATA_TRACK_PREFIX.length());
                datatracksCombobox.setSelectedItem(trackname);
            }
        } else {
            try {
               double[] values=NumericDataset.parseMinMaxBaselineDefaultParameters(parameterString);
               minTextField.setText(""+values[0]);
               maxTextField.setText(""+values[1]);
               baselineTextField.setText(""+values[2]);
               defaultvalueTextField.setText(""+values[3]);          
            } catch (Exception e) {}  
            useValuesRadioButton.setSelected(true);
        }

        pack();
    }
    
    
    @Override
    public boolean onOKPressed() {
        double defaultvalue=0;
        double minvalue=0;
        double maxvalue=0;
        double baselinevalue=0;
        if (useValuesRadioButton.isSelected()) {
            try { defaultvalue=Double.parseDouble(defaultvalueTextField.getText().trim()); } 
            catch (NumberFormatException e) { 
                messageLabel.setText("Please enter a valid numeric default value"); return false;
            }
            try { minvalue=Double.parseDouble(minTextField.getText().trim()); } 
            catch (NumberFormatException e) { 
                messageLabel.setText("Please enter a valid numeric minimum value"); return false;
            }
            try { maxvalue=Double.parseDouble(maxTextField.getText().trim()); } 
            catch (NumberFormatException e) { 
                messageLabel.setText("Please enter a valid numeric maximum value"); return false;
            }
            try { baselinevalue=Double.parseDouble(baselineTextField.getText().trim()); } 
            catch (NumberFormatException e) { 
                messageLabel.setText("Please enter a valid numeric baseline value"); return false;
            }
            parameters="value="+defaultvalue+",min="+minvalue+",max="+maxvalue+",baseline="+baselinevalue;        
        } else {
            parameters=Operation_new.DATA_TRACK_PREFIX+(String)datatracksCombobox.getSelectedItem();
        }
        return true;
    }
    
    @Override
    public Data getData() {
        // NOTE: there is really no need to create and return a full object, since the only time this prompt is used is by the "new" operation dialog,
        //       and that dialog initiates a new task to create the track based on the defined parameter string.
        //       This method would be required to return a valid data object if the prompt could be used to edit existing data, but that is not the case for feature tracks.
        return null;        
//       Operation_new operationNew=(Operation_new)engine.getOperation("new");
//       OperationTask task=new OperationTask("new object");
//       task.setParameter(Operation_new.DATA_TYPE, NumericDataset.getType());
//       task.setParameter(Operation_new.PARAMETERS, parameters);
//       task.setParameter(OperationTask.TARGET_NAME, getDataItemName());
//       Data data=null;
//       try {
//           data=operationNew.createDataItem(task);
//       } catch (Exception e) {
//           engine.logMessage("ERROR: "+e.getMessage());
//       }
//       if (data==null) {
//          engine.logMessage("ERROR: unable to create new dataset as requested. Returning empty dataset instead");
//          data=new NumericDataset(getDataItemName());
//          ((NumericDataset)data).setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));        
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
