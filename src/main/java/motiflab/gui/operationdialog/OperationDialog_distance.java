/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.operations.Operation_distance;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_distance extends OperationDialog {
    private JPanel nameAndTypePanel;
    private JPanel parametersPanel;
    private JTextField targetDataTextfield;
    private JRadioButton anchorTSS;
    private JRadioButton anchorTES;
    private JRadioButton anchorUpstream;
    private JRadioButton anchorDownstream;
    private JRadioButton anchorRegion;
    private JRadioButton anchorNumeric;
    private JComboBox regionDatasetCombobox;
    private JComboBox numericAnchorCombobox;    
    private JComboBox relativeAnchorCombobox;     
    private JComboBox directionCombobox;

    
    
    public OperationDialog_distance(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_distance() {
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
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("DistanceTrack", getDataTypeTable());
        targetDataTextfield.setText(targetName);
        parametersPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        parametersPanel.setBorder(commonBorder);
        parametersPanel.add(new JLabel("From anchor point   "));
        JPanel anchorsPanel=new JPanel();
        anchorsPanel.setLayout(new BoxLayout(anchorsPanel, BoxLayout.Y_AXIS));
        anchorTSS=new JRadioButton("Transcription start site");
        anchorTES=new JRadioButton("Transcription end site");
        anchorUpstream=new JRadioButton("Sequence upstream end");
        anchorDownstream=new JRadioButton("Sequence downstream end");
        anchorRegion=new JRadioButton("Region");
        DefaultComboBoxModel regionComboModel=getDataCandidates(RegionDataset.class);
        regionDatasetCombobox=new JComboBox(regionComboModel);
        regionDatasetCombobox.setEditable(true);
        regionDatasetCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                anchorRegion.setSelected(true);
            }
        });
        anchorNumeric=new JRadioButton("Position");
        DefaultComboBoxModel numericComboModel=getDataCandidates(new Class[]{SequenceNumericMap.class,NumericVariable.class});
        numericAnchorCombobox=new JComboBox(numericComboModel);
        numericAnchorCombobox.setEditable(true);
        numericAnchorCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                anchorNumeric.setSelected(true);
            }
        });   
        relativeAnchorCombobox=new JComboBox(new String[]{"transcription start site","transcription end site","sequence upstream end","sequence downstream end","chromosome start"});
        JPanel regionInternal1=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel regionInternal2=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel regionInternal3=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel regionInternal4=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel regionInternal5=new JPanel(new FlowLayout(FlowLayout.LEADING));     
        JPanel regionInternal6=new JPanel(new FlowLayout(FlowLayout.LEADING));     
        regionInternal1.add(anchorTSS);
        regionInternal2.add(anchorTES);
        regionInternal3.add(anchorUpstream);
        regionInternal4.add(anchorDownstream);
        regionInternal5.add(anchorRegion);
        regionInternal5.add(regionDatasetCombobox);
        regionInternal6.add(anchorNumeric);
        regionInternal6.add(numericAnchorCombobox);
        regionInternal6.add(new JLabel(" relative to "));
        regionInternal6.add(relativeAnchorCombobox);
        anchorsPanel.add(regionInternal1);
        anchorsPanel.add(regionInternal2);
        anchorsPanel.add(regionInternal3);
        anchorsPanel.add(regionInternal4);
        anchorsPanel.add(regionInternal5);
        anchorsPanel.add(regionInternal6);
        parametersPanel.add(anchorsPanel);
        ButtonGroup buttonGroup=new ButtonGroup();
        buttonGroup.add(anchorTSS);
        buttonGroup.add(anchorTES);
        buttonGroup.add(anchorUpstream);
        buttonGroup.add(anchorDownstream);
        buttonGroup.add(anchorRegion);
        buttonGroup.add(anchorNumeric);
        String anchorPoint=(String)parameters.getParameter(Operation_distance.ANCHOR_POINT);
        if (anchorPoint==null || anchorPoint.isEmpty()) anchorTSS.setSelected(true);
        else if (anchorPoint.equalsIgnoreCase("transcription start site")) anchorTSS.setSelected(true);
        else if (anchorPoint.equalsIgnoreCase("transcription end site")) anchorTES.setSelected(true);
        else if (anchorPoint.equalsIgnoreCase("sequence upstream end")) anchorUpstream.setSelected(true);
        else if (anchorPoint.equalsIgnoreCase("sequence downstream end")) anchorDownstream.setSelected(true);
        else { // anchorPoint is the name of a data object
           Class anchorclass=getClassForDataItem(anchorPoint);
           if (anchorclass==RegionDataset.class) {
               anchorRegion.setSelected(true);
               regionDatasetCombobox.setSelectedItem(anchorPoint);
           } else {
               anchorNumeric.setSelected(true);
               numericAnchorCombobox.setSelectedItem(anchorPoint);
           }
        }
        String relativeAnchorPoint=(String)parameters.getParameter(Operation_distance.RELATIVE_ANCHOR_POINT);
        if (relativeAnchorPoint!=null) relativeAnchorCombobox.setSelectedItem(relativeAnchorPoint);                
        JPanel directionPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        directionPanel.add(new JLabel("Direction   "));
        directionCombobox=new JComboBox(new String[]{Operation_distance.BOTH,Operation_distance.UPSTREAM,Operation_distance.DOWNSTREAM});
        String direction=(String)parameters.getParameter(Operation_distance.DIRECTION);
        if (direction==null || direction.isEmpty()) direction=Operation_distance.BOTH;
        directionCombobox.setSelectedItem(direction);
        directionPanel.add(directionCombobox);
        add(nameAndTypePanel);
        add(directionPanel);
        add(parametersPanel);
        add(getOKCancelButtonsPanel());
        pack();        
    }
 
    @Override
    protected void setParameters() {
        //super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        String targetName=targetDataTextfield.getText().trim();
        if (targetName.isEmpty()) targetName=gui.getGenericDataitemName("DistanceTrack", getDataTypeTable());
        parameters.setParameter(OperationTask.TARGET_NAME,targetName);
        String anchorPoint=null;
             if (anchorTSS.isSelected()) anchorPoint="transcription start site";
        else if (anchorTES.isSelected()) anchorPoint="transcription end site";
        else if (anchorUpstream.isSelected()) anchorPoint="sequence upstream end";
        else if (anchorDownstream.isSelected()) anchorPoint="sequence downstream end";
        else if (anchorRegion.isSelected()) anchorPoint=(String)regionDatasetCombobox.getSelectedItem();
        else if (anchorNumeric.isSelected()) anchorPoint=(String)numericAnchorCombobox.getSelectedItem();
        parameters.setParameter(Operation_distance.ANCHOR_POINT,anchorPoint);
        if (anchorNumeric.isSelected()) parameters.setParameter(Operation_distance.RELATIVE_ANCHOR_POINT,(String)relativeAnchorCombobox.getSelectedItem());
        else parameters.setParameter(Operation_distance.RELATIVE_ANCHOR_POINT,null);
        String direction=(String)directionCombobox.getSelectedItem();
        parameters.setParameter(Operation_distance.DIRECTION,direction);
        parameters.addAffectedDataObject(targetName, NumericDataset.class);
    }
}
