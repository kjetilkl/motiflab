/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.datasource.DataTrack;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.gui.MotifLabGUI;

/**
 *
 * @author kjetikl
 */
public class Prompt_RegionDataset extends Prompt {
    
    private JPanel mainpanel;
    private JLabel messageLabel;
    private String parameters = "";
    private JComboBox datatracksCombobox;
    
    public Prompt_RegionDataset(MotifLabGUI gui, String prompt, String dataname, String parameterString) {
        this(gui,prompt,dataname, parameterString, true);
    }  

    public Prompt_RegionDataset(MotifLabGUI gui, String prompt, RegionDataset dataset, boolean modal) {
        this(gui,prompt, (dataset!=null)?dataset.getName():"RegionDataset", (dataset!=null)?dataset.getValueAsParameterString():null, modal);
    }     
    
    public Prompt_RegionDataset(MotifLabGUI gui, String prompt, String dataname, String parameterString, boolean modal) {
        super(gui,prompt, modal);
        setDataItemName(dataname);
        setTitle("Regions Dataset settings");
        mainpanel=new JPanel();
        mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
        //mainpanel.add(valueTextField);
        messageLabel=new JLabel("  ");
        messageLabel.setFont(errorMessageFont);
        messageLabel.setForeground(java.awt.Color.RED);
        JPanel internPanel=new JPanel();
        internPanel.setLayout(new BoxLayout(internPanel, BoxLayout.Y_AXIS));
        JPanel tracksPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        DataTrack[] trackslist=engine.getDataLoader().getAvailableDatatracks(RegionDataset.class);
        String[] tracksnames=new String[trackslist.length];
        for (int i=0;i<trackslist.length;i++) tracksnames[i]=trackslist[i].getName();
        java.util.Arrays.sort(tracksnames);
        datatracksCombobox=new JComboBox(tracksnames);
        tracksPanel.add(new JLabel("DataTrack  "));
        tracksPanel.add(datatracksCombobox);      
        internPanel.add(tracksPanel);
        mainpanel.add(internPanel);
        mainpanel.add(messageLabel);
        Dimension size=new Dimension(250,60);
        mainpanel.setMinimumSize(size);
        //mainpanel.setPreferredSize(size);
        setMainPanel(mainpanel);    
        if (parameterString!=null && parameterString.startsWith(Operation_new.DATA_TRACK_PREFIX)) {
            if (parameterString.length()>Operation_new.DATA_TRACK_PREFIX.length()) {
                String trackname=parameterString.substring(Operation_new.DATA_TRACK_PREFIX.length());
                datatracksCombobox.setSelectedItem(trackname);
            }
        } else {}

        pack();
    }
    
    
    @Override
    public boolean onOKPressed() {
        parameters=Operation_new.DATA_TRACK_PREFIX+(String)datatracksCombobox.getSelectedItem();
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
//       task.setParameter(Operation_new.DATA_TYPE, RegionDataset.getType());
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
//          data=new RegionDataset(getDataItemName());
//          ((RegionDataset)data).setupDefaultDataset(engine.getDefaultSequenceCollection().getAllSequences(engine));        
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