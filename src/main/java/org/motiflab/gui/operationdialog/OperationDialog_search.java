/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.operations.Operation_search;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_search extends FeatureTransformOperationDialog {
    private JPanel searchforpanel;
    private JComboBox searchforVariableCombobox;
    private JComboBox searchforMotifCombobox;
    private JTextField searchforTextfield;
    private JRadioButton searchForLiteralButton;
    private JRadioButton searchForVariableButton;
    private JRadioButton searchForMotifButton;
    private JRadioButton searchForRepeatsButton;
    private JComboBox searchStrandCombobox;
    private JSpinner mismatchTextfield;
    private JSpinner minHalfsiteSizeTextfield;
    private JSpinner maxHalfsiteSizeTextfield;
    private JSpinner minGapSizeTextfield;
    private JSpinner maxGapSizeTextfield;
    private JComboBox repeatDirectionComboBox;
    private JComboBox reportSiteComboBox;
    
   
    
    public OperationDialog_search(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_search() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        searchforpanel=new JPanel(new GridBagLayout());
        GridBagConstraints constraints=new GridBagConstraints();
        searchforpanel.setBorder(commonBorder);
        boolean searchForRepeats=false;
        String searchForString=(String)parameters.getParameter(Operation_search.SEARCH_EXPRESSION);
        if (searchForString!=null && searchForString.equals(Operation_search.SEARCH_REPEAT)) searchForRepeats=true;
        if (searchForString==null) searchForString="\"CACGTG\"";  
        boolean initFromVariable=true;
        if (searchForString.startsWith("\"") && searchForString.endsWith("\"")) {
            searchForString=searchForString.substring(1, searchForString.length()-1);
            initFromVariable=false;
        }
        searchForLiteralButton = new JRadioButton("Search for regular expression   ");
        searchForVariableButton = new JRadioButton("Search for Text Variable");
        searchForMotifButton = new JRadioButton("Search for motif consensus");
        searchForRepeatsButton = new JRadioButton("Search for repeats");
        ButtonGroup group=new ButtonGroup();
        group.add(searchForLiteralButton);
        group.add(searchForVariableButton);
        group.add(searchForMotifButton);
        group.add(searchForRepeatsButton);

        DefaultComboBoxModel wbmodel=getDataCandidates(new Class[]{TextVariable.class});
        searchforVariableCombobox = new JComboBox(wbmodel);
        searchforVariableCombobox.setEditable(false);
        DefaultComboBoxModel wbmotifmodel=getDataCandidates(new Class[]{Motif.class,MotifCollection.class});
        searchforMotifCombobox = new JComboBox(wbmotifmodel);
        searchforMotifCombobox.setEditable(false);
        searchforTextfield=new JTextField(15);
        if (searchForRepeats) {
            searchForRepeatsButton.setSelected(true);
        }
        else if (initFromVariable) {
            if (getClassForDataItem(searchForString)==Motif.class || getClassForDataItem(searchForString)==MotifCollection.class) {
                searchForMotifButton.setSelected(true);
                searchforMotifCombobox.setSelectedItem(searchForString);
                if (searchforMotifCombobox.getSelectedIndex()<0 && searchforMotifCombobox.getModel().getSize()>0) searchforMotifCombobox.setSelectedIndex(0);                
            } else {
                searchForVariableButton.setSelected(true);
                searchforVariableCombobox.setSelectedItem(searchForString);
                if (searchforVariableCombobox.getSelectedIndex()<0 && searchforVariableCombobox.getModel().getSize()>0) searchforVariableCombobox.setSelectedIndex(0);
            }
        } else {
           searchForLiteralButton.setSelected(true); 
           searchforTextfield.setText(searchForString);
        }
        searchforTextfield.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
               if (!searchForLiteralButton.isSelected()) searchForLiteralButton.setSelected(true);
            }
        });
        String searchStrandString=(String)parameters.getParameter(Operation_search.SEARCH_STRAND);
        if (searchStrandString==null) searchStrandString=Operation_search.STRAND_BOTH;

        Integer mismatchesInteger=(Integer)parameters.getParameter(Operation_search.MISMATCHES);
        int mismatches=0; if (mismatchesInteger!=null) mismatches=mismatchesInteger.intValue();
        Integer minHalfsiteInteger=(Integer)parameters.getParameter(Operation_search.MIN_HALFSITE_LENGTH);
        int minhalfsitesize=Operation_search.MINIMUM_HALFSITE_SIZE+1; if (minHalfsiteInteger!=null) minhalfsitesize=minHalfsiteInteger.intValue();
      
        Integer maxHalfsiteInteger=(Integer)parameters.getParameter(Operation_search.MAX_HALFSITE_LENGTH);
        int maxhalfsitesize=12; if (maxHalfsiteInteger!=null) maxhalfsitesize=maxHalfsiteInteger.intValue();

        Integer minGapInteger=(Integer)parameters.getParameter(Operation_search.MIN_GAP_LENGTH);
        int mingapsize=0; if (minGapInteger!=null) mingapsize=minGapInteger.intValue();
   
        Integer maxGapInteger=(Integer)parameters.getParameter(Operation_search.MAX_GAP_LENGTH);
        int maxgapsize=6; if (maxGapInteger!=null) maxgapsize=maxGapInteger.intValue();
        if (maxgapsize>Operation_search.MAXIMUM_GAP_SIZE) maxgapsize=Operation_search.MAXIMUM_GAP_SIZE;

        String repeatDirectionString=(String)parameters.getParameter(Operation_search.SEARCH_REPEAT_DIRECTION);
        if (repeatDirectionString==null) repeatDirectionString=Operation_search.SEARCH_REPEAT_DIRECT;
        String reportSiteString=(String)parameters.getParameter(Operation_search.REPORT_SITE);
        if (reportSiteString==null) reportSiteString=Operation_search.REPORT_SITE_HALFSITE;
        
        searchStrandCombobox = new JComboBox(new String[]{Operation_search.STRAND_BOTH,Operation_search.STRAND_GENE,Operation_search.STRAND_OPPOSITE,Operation_search.STRAND_DIRECT,Operation_search.STRAND_REVERSE});
        searchStrandCombobox.setSelectedItem(searchStrandString);

        mismatchTextfield = new JSpinner(new SpinnerNumberModel(mismatches, 0, 200, 1));
        minHalfsiteSizeTextfield = new JSpinner(new SpinnerNumberModel(minhalfsitesize, Operation_search.MINIMUM_HALFSITE_SIZE, 500, 1));
        maxHalfsiteSizeTextfield = new JSpinner(new SpinnerNumberModel(maxhalfsitesize, Operation_search.MINIMUM_HALFSITE_SIZE, 500, 1));
        minGapSizeTextfield = new JSpinner(new SpinnerNumberModel(mingapsize, 0, Operation_search.MAXIMUM_GAP_SIZE, 1));
        maxGapSizeTextfield = new JSpinner(new SpinnerNumberModel(maxgapsize, 0, Operation_search.MAXIMUM_GAP_SIZE, 1));
    
        repeatDirectionComboBox=new JComboBox(new String[]{Operation_search.SEARCH_REPEAT_DIRECT,Operation_search.SEARCH_REPEAT_INVERTED});
        repeatDirectionComboBox.setSelectedItem(repeatDirectionString);
        reportSiteComboBox=new JComboBox(new String[]{Operation_search.REPORT_SITE_HALFSITE,Operation_search.REPORT_SITE_FULL});
        reportSiteComboBox.setSelectedItem(reportSiteString);
        
        constraints.anchor=GridBagConstraints.BASELINE_LEADING;
        constraints.gridy=0;constraints.gridx=0;constraints.gridwidth=1;
        searchforpanel.add(searchForLiteralButton, constraints);
        constraints.gridx=1;constraints.gridwidth=GridBagConstraints.REMAINDER;
        searchforpanel.add(searchforTextfield, constraints);
        constraints.gridy++;constraints.gridx=0;constraints.gridwidth=1;
        searchforpanel.add(searchForVariableButton, constraints);
        constraints.gridx=1;constraints.gridwidth=GridBagConstraints.REMAINDER;
        searchforpanel.add(searchforVariableCombobox, constraints);
        constraints.gridy++;constraints.gridx=0;constraints.gridwidth=1;
        searchforpanel.add(searchForMotifButton, constraints);
        constraints.gridx=1;constraints.gridwidth=GridBagConstraints.REMAINDER;
        searchforpanel.add(searchforMotifCombobox, constraints);
        constraints.gridy++;
        constraints.gridx=0;constraints.gridwidth=1;
        searchforpanel.add(searchForRepeatsButton, constraints);
        constraints.gridx++;        
        searchforpanel.add(repeatDirectionComboBox, constraints);
        constraints.gridx++;        
        searchforpanel.add(new JLabel("    Halfsite size:  from "), constraints);
        constraints.gridx++;        
        searchforpanel.add(minHalfsiteSizeTextfield, constraints);
        constraints.gridx++;        
        searchforpanel.add(new JLabel(" to "), constraints);
        constraints.gridx++;        
        searchforpanel.add(maxHalfsiteSizeTextfield, constraints);
        constraints.gridx++;
        searchforpanel.add(new JLabel("    Report: "), constraints);
        constraints.gridx++;
        searchforpanel.add(reportSiteComboBox, constraints);
        constraints.gridy++;
        constraints.gridx=2;
        searchforpanel.add(new JLabel("    Gap size:  from "), constraints);
        constraints.gridx++;
        searchforpanel.add(minGapSizeTextfield, constraints);
        constraints.gridx++;        
        searchforpanel.add(new JLabel(" to "), constraints);
        constraints.gridx++;        
        searchforpanel.add(maxGapSizeTextfield, constraints);
        constraints.gridy++;constraints.gridx=0;constraints.gridwidth=1;
        searchforpanel.add(new JLabel("   "), constraints); // this is just to create some space
        constraints.gridy++;constraints.gridx=0;constraints.gridwidth=1;
        searchforpanel.add(new JLabel("      on strand  "), constraints);
        constraints.gridx=1;constraints.gridwidth=GridBagConstraints.REMAINDER;
        searchforpanel.add(searchStrandCombobox, constraints);
        constraints.gridy++;constraints.gridx=0;constraints.gridwidth=1;
        searchforpanel.add(new JLabel("      mismatches allowed "), constraints);
        constraints.gridx=1;constraints.gridwidth=GridBagConstraints.REMAINDER;
        searchforpanel.add(mismatchTextfield, constraints);

        JPanel sourceTargetPanel=getSourceTargetPanel();
        sourceTargetPanel.setBorder(commonBorder);
        JPanel whereClausePanel=getWhereClausePanel(FeatureTransformOperationDialog.CONDITION_NUMERIC);
        whereClausePanel.setBorder(commonBorder);
        JPanel subsetPanel=getSequenceSubsetPanel();
        subsetPanel.setBorder(commonBorder);
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("SearchTrack", getDataTypeTable());
        targetDataTextfield.setText(targetName);
        JPanel internalpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        internalpanel.add(searchforpanel);
        add(sourceTargetPanel);
        add(internalpanel);
        add(whereClausePanel);
        add(subsetPanel);
        add(getOKCancelButtonsPanel());
        pack();        
        searchforVariableCombobox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                searchForVariableButton.setSelected(true);
            }
        });
        searchforMotifCombobox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                searchForMotifButton.setSelected(true);
            }
        });
        repeatDirectionComboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                searchForRepeatsButton.setSelected(true);
            }
        });
        searchForRepeatsButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                searchStrandCombobox.setEnabled(!searchForRepeatsButton.isSelected());
            }
        });
        searchStrandCombobox.setEnabled(!searchForRepeatsButton.isSelected());
    }

    @Override
    protected void setParameters() {
        super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        String searchExpression;
        if (searchForLiteralButton.isSelected()) {
            searchExpression="\""+searchforTextfield.getText().trim()+"\"";
        } else if (searchForMotifButton.isSelected()) {
           searchExpression=(String)searchforMotifCombobox.getSelectedItem();
           if (searchExpression==null || searchExpression.isEmpty()) searchExpression="\"\"";
        } else if (searchForRepeatsButton.isSelected()) {
           searchExpression=Operation_search.SEARCH_REPEAT;
        } else {
           searchExpression=(String)searchforVariableCombobox.getSelectedItem(); 
           if (searchExpression==null || searchExpression.isEmpty()) searchExpression="\"\"";
        }
        parameters.setParameter(Operation_search.SEARCH_EXPRESSION, searchExpression);      
        parameters.setParameter(Operation_search.SEARCH_STRAND, (String)searchStrandCombobox.getSelectedItem());
        Integer mismatchesAllowed=(Integer)mismatchTextfield.getValue();
        parameters.setParameter(Operation_search.MISMATCHES, mismatchesAllowed);
        Integer minHalfSiteSize=(Integer)minHalfsiteSizeTextfield.getValue();
        parameters.setParameter(Operation_search.MIN_HALFSITE_LENGTH, minHalfSiteSize);
        Integer maxHalfSiteSize=(Integer)maxHalfsiteSizeTextfield.getValue();
        parameters.setParameter(Operation_search.MAX_HALFSITE_LENGTH, maxHalfSiteSize);
        Integer minGapSize=(Integer)minGapSizeTextfield.getValue();
        parameters.setParameter(Operation_search.MIN_GAP_LENGTH, minGapSize);
        Integer maxGapSize=(Integer)maxGapSizeTextfield.getValue();
        parameters.setParameter(Operation_search.MAX_GAP_LENGTH, maxGapSize);
        String repeatDirection=(String)repeatDirectionComboBox.getSelectedItem();
        parameters.setParameter(Operation_search.SEARCH_REPEAT_DIRECTION, repeatDirection);
        String reportSite=(String)reportSiteComboBox.getSelectedItem();
        parameters.setParameter(Operation_search.REPORT_SITE, reportSite);
        String targetName=(String)parameters.getParameter(org.motiflab.engine.task.OperationTask.TARGET_NAME); // this should have been set in super.setParameters() above
        parameters.addAffectedDataObject(targetName, RegionDataset.class);
    }
}
