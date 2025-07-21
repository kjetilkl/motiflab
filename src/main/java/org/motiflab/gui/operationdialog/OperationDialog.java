/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.border.Border;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.operations.Condition_within;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.protocol.DataTypeTable;
import org.motiflab.gui.InfoDialog;
import org.motiflab.gui.MotifLabGUI;

/**
 * This class is the top class specifying GUIs (both renderers and editors) for
 * operations within the GUI interface.
 * 
 * @author kjetikl
 */
public abstract class OperationDialog extends JDialog {
    protected OperationTask parameters;
    protected MotifLabEngine engine;
    private boolean okPressed=false;
    protected JButton okButton;
    protected JButton cancelButton;
    protected JButton helpButton;
    private JPanel buttonsPanel;
    private JPanel buttonsInternalPanel;
    //protected Border commonBorder=BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4), BorderFactory.createEtchedBorder());
    protected Border commonBorder=BorderFactory.createEmptyBorder(4,4,4,4);
    private DataTypeTable lookup;
    protected JComboBox sourceDataCombobox=null; 
    protected JComboBox sourcePropertyCombobox=null;     
    protected JTextField targetDataTextfield=null; 
    private JPanel sourceTargetPanel=null;
    private ConditionPanel_within windowSelectionPanel=null;
    protected ImageIcon informationIcon=null;
    protected MotifLabGUI gui=null;
    protected JLabel sourceLabel=null;
    protected JLabel sourcePropertyLabel=null;    
    protected JLabel targetLabel=null;

    

    /** Creates new form TestExtendOperationDialog */
    public OperationDialog(java.awt.Frame parent) {
        super(parent);               
    }
    
    /** 
     * Creates a new OperationsDialog. 
     * The initialize() method should be called immediately after construction to set required parameters
     */
    public OperationDialog() {
        super();
    }
        
    /** This method should be called immediately after construction whenever an empty constructor is used */
    public void initialize(OperationTask parameters, DataTypeTable lookuptable, MotifLabGUI gui) {
        //System.err.println("Initialize["+parameters.getOperationName()+"]: lookuptable="+lookuptable+"   size="+lookuptable.size());
        this.setModal(true);
        this.parameters=parameters;
        this.engine=(MotifLabEngine)parameters.getParameter(OperationTask.ENGINE);
        this.gui=gui;
        if (lookuptable!=null) lookup=lookuptable;
        else {
           lookup=new DataTypeTable(engine);
           lookup.populateFromEngine();
        }
        Operation op=parameters.getOperation();
        String operationName=(op!=null)?op.getName():parameters.getOperationName();
        setTitle("  "+operationName);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setResizable(false);
        java.net.URL iconURL=getClass().getResource("/org/motiflab/gui/resources/icons/help_icon.png");
        if (iconURL!=null) informationIcon=new ImageIcon(iconURL); 
        else gui.logMessage("System fault: Missing resource for 'help' icon");
        initComponents();   
    }
  
    public ImageIcon getInformationIcon() {return informationIcon;}
    
    public ExecutableTask getOperationTask() {
        return parameters;
    }
    
    public JPanel getOKCancelButtonsPanel() {
        return buttonsPanel;
    }
    
    public JPanel getRangeSelectionPanel() {
        return windowSelectionPanel;
    }
        
    public boolean okPressed() {return okPressed;}
    protected void setOK(boolean ok) {okPressed=ok;}
    
    /** This method can be overridden in subclasses
     *  It will be called when the OK-button is pressed
     *  and if it returns an error string (other than the
     *  default value of null), the string will be displayed
     *  as an error message and the control of the dialog
     *  will be handled back to the user (rather than closing it)
     */
    protected String checkForErrors() {return null;}

    protected void initComponents() {
        okButton=new JButton("  OK  ");
        cancelButton=new JButton("Cancel");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parameters.removeParameter("_SHOW_RESULTS");
                String errors=checkForErrors();
                if (errors!=null) {
                    JOptionPane.showMessageDialog(rootPane, errors, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    setOK(true);
                    setParameters();
                    int modifiers=e.getModifiers();
                    if ((modifiers & ActionEvent.SHIFT_MASK)==ActionEvent.SHIFT_MASK || (modifiers & ActionEvent.CTRL_MASK)==ActionEvent.CTRL_MASK) parameters.setParameter("_SHOW_RESULTS", Boolean.FALSE);
                    setVisible(false);
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed=false;
                setVisible(false);
            }
        });
        buttonsPanel=new JPanel();
        buttonsInternalPanel=new JPanel();
        buttonsInternalPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonsInternalPanel.add(okButton);
        buttonsInternalPanel.add(cancelButton);  
        buttonsPanel.setLayout(new BorderLayout());
        buttonsPanel.add(buttonsInternalPanel,BorderLayout.EAST);
        getRootPane().setDefaultButton(okButton);
        Operation operation=parameters.getOperation();        
        if (operation!=null) initSourceTargetPanel();
        Condition_within windowselection=(Condition_within)parameters.getParameter("within");

        if (operation!=null && operation.isSubrangeApplicable() && windowselection!=null) {
            windowSelectionPanel=new ConditionPanel_within(windowselection, this);
            buttonsPanel.add(windowSelectionPanel,BorderLayout.NORTH);
        }       
        helpButton=new JButton();
        if (informationIcon!=null) helpButton.setIcon(informationIcon);
        else helpButton.setText("Help");
        helpButton.setToolTipText("See HELP page for this operation");
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        }); 
        JPanel helpInternalPanel=new JPanel();
        helpInternalPanel.setLayout(new FlowLayout(FlowLayout.LEFT));      
        if (operation!=null) helpInternalPanel.add(helpButton);
        buttonsPanel.add(helpInternalPanel,BorderLayout.WEST);
    }
    
    /**
     * In this method the parameters set by the user in the GUI dialog
     * should be parsed and transferred to the OperationTask object
     */
    protected void setParameters() {
        String sourceName=(String)sourceDataCombobox.getSelectedItem();
        parameters.setParameter(OperationTask.SOURCE_NAME, sourceName);
        String targetName=targetDataTextfield.getText();
        if (targetName!=null) targetName.trim();
        if (targetName==null || targetName.isEmpty()) targetName=sourceName;
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);
        Class sourcetype=getClassForDataItem(sourceName);
        Operation operation=parameters.getOperation();
        if (operation.isSubrangeApplicable() && windowSelectionPanel!=null && FeatureDataset.class.isAssignableFrom(sourcetype)) {
            Condition_within within=windowSelectionPanel.getCondition();
            if (within!=null) parameters.setParameter("within",within);
            else parameters.removeParameter("within");
        } else {
            parameters.removeParameter("within"); // just in case
        }
    }
    
    /**
     * If argument is FALSE the range panel will be hidden
     * If TRUE the range panel will be shown but only if there exists
     * current selections
     * @param show
     */
    protected void setVisibilityOfRangePanel(boolean show) {
        if (windowSelectionPanel==null) return;
        else if (!show) windowSelectionPanel.setVisible(false);
        else {
           if (parameters.getParameter("within")!=null) windowSelectionPanel.setVisible(true);
        }
    }

    /** Returns (as DefaultComboBoxModel) a list of names for all data items of the given class 
     * that will potentially be available at this point in the protocol 
     * either because a data item has been registered with the engine outside the protocol
     * or a data item has been defined earlier in the protocol
     */      
    public DefaultComboBoxModel getDataCandidates(Class candidateClass) {
         return getDataCandidates(new Class[]{candidateClass},false);
    }
    
    /** Returns (as DefaultComboBoxModel) a list of names for all data items of the given class 
     * that will potentially be available at this point in the protocol 
     * either because a data item has been registered with the engine outside the protocol
     * or a data item has been defined earlier in the protocol
     */      
    public DefaultComboBoxModel getDataCandidates(Class candidateClass, boolean topBlank) {
         return getDataCandidates(new Class[]{candidateClass},topBlank);
    }
    
    /** Returns (as DefaultComboBoxModel) a list of names for all data items of the given classes 
     * that will potentially be available at this point in the protocol
     * either because a data item has been registered with the engine outside the protocol
     * or a data item has been defined earlier in the protocol
     */    
    public DefaultComboBoxModel getDataCandidates(Class[] candidateClasses) {
         return getDataCandidates(candidateClasses,false);
    }
    
    public DefaultComboBoxModel getDataCandidates(Class[] candidateClasses, boolean topBlank) {
        ArrayList<String>candidateNames=lookup.getAllDataItemsOfType(candidateClasses);
        Collections.sort(candidateNames);
        int size=candidateNames.size();
        if (topBlank) size++;
        String[] entries=new String[size];
        int i=0;
        if (topBlank) {
            entries[i]="";
            i++;
        }
        for (String dataName:candidateNames) {
            entries[i]=dataName;
            i++;
        }
        DefaultComboBoxModel model=new DefaultComboBoxModel(entries);
        return model;       
    }  
    
    public DefaultListModel getDataCandidatesAsListModel(Class[] candidateClasses) {
        DefaultListModel model=new DefaultListModel();
        ArrayList<String>candidateNames=lookup.getAllDataItemsOfType(candidateClasses);
        Collections.sort(candidateNames);
        for (String dataName:candidateNames) {
            model.addElement(dataName);
        }        
        return model;       
    }  
   
    
    /** Returns true if a data item with the given will potentially exists
     *  either because it is already registered with the engine, or because
     *  the name has been defined earlier in the protocol script 
     */
    public boolean dataItemNameInUse(String name) {
        return lookup.contains(name);
    }
    
    public DataTypeTable getDataTypeTable() {
        return lookup;
    }
    
    /** Returns the class for the dataitem with the given name based on the current status
     *  (or the position of the command within a protocol)
     */
    public Class getClassForDataItem(String name) {
        if (lookup==null || name==null) return null;
        if (name.contains(",")) name=name.substring(0, name.indexOf(",")); // if this is a list of names, just use the first
        return lookup.getClassFor(name);
    }
    
    protected JPanel getSourceTargetPanel() {return sourceTargetPanel;} 

    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel sPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel tPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Class[] sourceCandidates=parameters.getOperation().getDataSourcePreferences();
        String sourceName=parameters.getSourceDataName();
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=sourceName;
        DefaultComboBoxModel model=(DefaultComboBoxModel)getDataCandidates(sourceCandidates);
        sourceDataCombobox=new JComboBox(model);
        sourceDataCombobox.setEditable(true);
        if (sourceName!=null && !sourceName.isEmpty()) sourceDataCombobox.setSelectedItem(sourceName);
        sourceLabel=new JLabel("Source  ");
        sPanel.add(sourceLabel);
        sPanel.add(sourceDataCombobox);

        sourcePropertyCombobox=new JComboBox(new String[]{});
        sourcePropertyCombobox.setEditable(true);  
        sourcePropertyLabel=new JLabel("  Property  ");
        sPanel.add(sourcePropertyLabel);        
        sPanel.add(sourcePropertyCombobox);         
        sourcePropertyLabel.setVisible(false);
        sourcePropertyCombobox.setVisible(false);
        
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(16);
        targetLabel=new JLabel("       Store results in  ");
        tPanel.add(targetLabel);
        tPanel.add(targetDataTextfield);  
        sourceDataCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                targetDataTextfield.setText(sourceDataCombobox.getSelectedItem().toString());
            }
        });        
        sourceTargetPanel.add(sPanel);
        sourceTargetPanel.add(tPanel);
    }
  
    public JComboBox getSourcePropertyCombobox() {
        return sourcePropertyCombobox;
    }
    
    public void setSourceVisible(boolean visible) {
        sourceLabel.setVisible(visible);
        sourceDataCombobox.setVisible(visible);
        pack();
    }
    
    public void setSourcePropertyVisible(boolean visible) {
        sourcePropertyLabel.setVisible(visible);
        sourcePropertyCombobox.setVisible(visible);
        pack();
    }    

    public void setTargetVisible(boolean visible) {
        targetLabel.setVisible(visible);
        targetDataTextfield.setVisible(visible);
        pack();
    }

    protected void debug() {
        int count=this.getContentPane().getComponentCount();
        for (int i=0;i<count;i++) {
            java.awt.Component c=this.getContentPane().getComponent(i);
            gui.logMessage(c.getClass().getSimpleName()+":"+c.getBounds().toString());
        }
    }
    
    private void showHelp() {
        Object help=null;
        Operation operation=parameters.getOperation();
        String error="Help unavailable";
        if (operation!=null) {
            help=operation.getHelp(engine);
            error="<h1>"+operation.getName()+"</h1><br>"+operation.getDescription()+"<br><br>Detailed documentation for this operation is currently unavailable.";                
        } else help="Unable to determine which operation you refer to...";
        InfoDialog infodialog=null;        
        if (help instanceof String) infodialog=new InfoDialog(gui, "Help for operation: "+parameters.getOperationName(), (String)help, 700, 450);
        else if (help instanceof URL) infodialog=new InfoDialog(gui, "Help for operation: "+parameters.getOperationName(), (URL)help, 700, 450, false);      
        if (infodialog!=null) {
            infodialog.setErrorMessage(error);
            infodialog.setVisible(true);
        }
    }
}
