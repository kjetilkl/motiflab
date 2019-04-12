/*
 
 
 */

package motiflab.gui.operationdialog;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.TextVariable;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Operation_prompt;
import motiflab.engine.operations.Operation;
import motiflab.engine.operations.PromptConstraints;
import motiflab.engine.protocol.ParseError;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_prompt extends OperationDialog {
    private JPanel sourcePanel;
    private JPanel messagePanel;
    private JPanel constraintsPanel;
    private JTextField messageTextfield;
    private JTextField constraintsTextfield;    
    private JComboBox sourceCombobox;
    
    
    public OperationDialog_prompt(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_prompt() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        sourcePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        sourcePanel.setBorder(commonBorder);
        sourcePanel.add(new JLabel("Target "));

        Operation promptOperation=engine.getOperation("prompt");
        DefaultComboBoxModel sourceComboModel=getDataCandidates(promptOperation.getDataSourcePreferences());

        sourceCombobox = new JComboBox(sourceComboModel);
        sourceCombobox.setEditable(true);
        String currentSourceData=(String)parameters.getParameter(OperationTask.SOURCE_NAME);
        if (currentSourceData!=null && !currentSourceData.isEmpty()) sourceCombobox.setSelectedItem(currentSourceData);
        else sourceCombobox.setSelectedIndex(0);
        sourcePanel.add(sourceCombobox);                   
        messagePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        messagePanel.setBorder(commonBorder);
        messagePanel.add(new JLabel("Message "));
        messageTextfield=new JTextField();
        messagePanel.add(messageTextfield);
        messageTextfield.setColumns(26);
        constraintsPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        constraintsPanel.setBorder(commonBorder);
        constraintsPanel.add(new JLabel("Constraints "));
        constraintsTextfield=new JTextField();
        constraintsPanel.add(constraintsTextfield);
        constraintsTextfield.setColumns(26);    
        JLabel constraintHelp=new JLabel(informationIcon);
        constraintHelp.setToolTipText(getConstraintHelp());
        constraintsPanel.add(constraintHelp);
        String currentMessage=(String)parameters.getParameter(Operation_prompt.PROMPT_MESSAGE);
        if (currentMessage!=null) messageTextfield.setText(currentMessage);
        PromptConstraints constraints=(PromptConstraints)parameters.getParameter(Operation_prompt.PROMPT_CONSTRAINTS);
        if (constraints!=null) {
            String constraintString=constraints.getConstraintString();
            if (constraintString!=null) constraintsTextfield.setText(constraintString);
        } 
        add(sourcePanel);
        add(messagePanel);
        add(constraintsPanel);        
        add(getOKCancelButtonsPanel());
        updateConstraint();
        sourceCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               updateConstraint();
            }
        });
        pack();        
    }

    @Override
    protected void setParameters() {
        String sourceName=(String)sourceCombobox.getSelectedItem();
        parameters.setParameter(OperationTask.SOURCE_NAME,sourceName);
        parameters.setParameter(OperationTask.TARGET_NAME,sourceName);
        String message=messageTextfield.getText().trim();
        if (!message.isEmpty()) parameters.setParameter(Operation_prompt.PROMPT_MESSAGE, message);
        else parameters.setParameter(Operation_prompt.PROMPT_MESSAGE, null);
        Class oldclass=getClassForDataItem(sourceName);
        parameters.addAffectedDataObject(sourceName, oldclass);
        try {
           PromptConstraints constraint=getPromptConstraint();
           if (constraint!=null) parameters.setParameter(Operation_prompt.PROMPT_CONSTRAINTS, constraint);
        } catch (ParseError e) {} // this will already have been reported by checkForErrors()
    }
    
    private Class getSourceClass() {
        String sourceName=(String)sourceCombobox.getSelectedItem();
        return getClassForDataItem(sourceName);
    }
    
    @Override
    protected String checkForErrors() {
        try {getPromptConstraint();}
        catch (ParseError e) {return "Constraint error: "+e.getMessage();}
        return null;
    }
    
    private void updateConstraint() {
        Class sourceClass=getSourceClass();
        if (sourceClass==NumericVariable.class || sourceClass==TextVariable.class) {
            constraintsTextfield.setEnabled(true); 
        } else {
            constraintsTextfield.setText("");
            constraintsTextfield.setEnabled(false); 
        }
    }
    
    private PromptConstraints getPromptConstraint() throws ParseError {
        Class sourceClass=getSourceClass();
        if (constraintsTextfield.isEnabled() && (sourceClass==NumericVariable.class || sourceClass==TextVariable.class)) {
            String constraints=constraintsTextfield.getText();
            if (constraints!=null && !constraints.trim().isEmpty()) {
                return new PromptConstraints(sourceClass, constraints);
            }
        } return null;        
    }
    
    private String getConstraintHelp() {
        return "<html>Values of  <b>Text Variables</b> and <b>Numeric Variables</b> can be <i>constrained</i>.</html>";
    }
}
