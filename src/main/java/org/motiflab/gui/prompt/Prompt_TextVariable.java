/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.operations.PromptConstraints;
import org.motiflab.gui.LoadFromFilePanel;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.SimpleDataPanelIcon;

/**
 *
 * @author kjetikl
 */
public class Prompt_TextVariable extends Prompt {
    
    private JTextArea valueTextField;
    private TextVariable data;
    private JPanel manualEntryPanel;
    private LoadFromFilePanel importFromFilePanel;
    private JTabbedPane tabbedPanel;    
    private JPanel internal;
    private JComponent constrainedValuesComponent=null;
    private PromptConstraints constraints=null;
    
    public Prompt_TextVariable(MotifLabGUI gui, String prompt, TextVariable dataitem) {
        this(gui,prompt,dataitem,true);
    }  
     
    public Prompt_TextVariable(MotifLabGUI gui, String prompt, TextVariable dataitem, boolean modal) {
        super(gui,prompt, modal);
        if (dataitem!=null) {
            data=dataitem;
            setExistingDataItem(dataitem);
        } else data=new TextVariable(gui.getGenericDataitemName(TextVariable.class, null));
        setDataItemName(data.getName());
        setTitle("Text Variable");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.TEXT_VARIABLE_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(java.awt.Color.WHITE);
        setDataItemIcon(icon, true);
        setupManualEntryPanel();
        setupImportProfilePanel();
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Manual Entry", manualEntryPanel);
        tabbedPanel.addTab("Import", importFromFilePanel);
        internal=new JPanel(new BorderLayout());
        Dimension size=new Dimension(550,380);
        internal.setMinimumSize(size);
        internal.setPreferredSize(size);
        // internal.setMaximumSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        importFromFilePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        internal.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(internal);
        pack();
        if (dataitem!=null) {
           tabbedPanel.setSelectedComponent(manualEntryPanel);
           focusOKButton();
        }
    }

    private void setupManualEntryPanel() {
        manualEntryPanel=new JPanel();
        manualEntryPanel.setLayout(new BorderLayout());
        StringBuilder values=new StringBuilder();
        for (String string:data.getAllStrings()) {
            values.append(string);
            values.append("\n");
        }
        valueTextField=new JTextArea(values.toString());
        manualEntryPanel=new JPanel();
        manualEntryPanel.setLayout(new BorderLayout());
        manualEntryPanel.add(new JScrollPane(valueTextField)); 
    }
    
     private void setupImportProfilePanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(TextVariable.class);
        importFromFilePanel=new LoadFromFilePanel(dataformats,gui,ExpressionProfile.class);
    }   
    
   @Override
    public boolean onOKPressed() {
        if (constraints!=null) {
           if (constrainedValuesComponent instanceof JComboBox) {
               String selected=(String)((JComboBox)constrainedValuesComponent).getSelectedItem();
               if (selected!=null) data.setValue(new String[]{selected});
           } else if (constrainedValuesComponent instanceof JListWrapper) {
               String selected=((JListWrapper)constrainedValuesComponent).getValue();
               if (selected!=null) data.setValue(selected);
           } else if (constrainedValuesComponent instanceof JTextFieldWrapper) {
               String value=((JTextFieldWrapper)constrainedValuesComponent).getValue();
               String error=constraints.isValueAllowed(value,engine);
               if (error!=null) {
                   ((JTextFieldWrapper)constrainedValuesComponent).setError(error);
                   return false;
               } else data.setValue(((JTextFieldWrapper)constrainedValuesComponent).getValue());
           }
        } else if (tabbedPanel.getSelectedComponent()==importFromFilePanel) {
            try {
                String filename=importFromFilePanel.getFilename();
                if (filename==null) throw new SystemError("Missing filename");
                data=(TextVariable)importFromFilePanel.loadData(data,TextVariable.getType());
                DataFormat format=importFromFilePanel.getDataFormat();
                ParameterSettings settings=importFromFilePanel.getParameterSettings();
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing Text Variable from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel) {       
            String val = valueTextField.getText();
            if (val.trim().isEmpty()) { // no strings are entered 
                data.clearAll();
            } else {
                if (val.endsWith("\n")) val=val.substring(0,val.length()-1); // remove ending newline to avoid that the splitting beneath causes an unecessary empty line at the end
                String list[] = val.split("\n",-1); // the -1 parameter will keep empty lines at the end of the document instead of discarding them
                //String list[] = val.split("\n"); // This default form of splitting will discard empty lines at the end
                // for (int i=0;i<list.length;i++) { list[i]=list[i].trim(); } // do not trim()!  Whitespace could be important in some instances!
                data.setValue(list);
            }
        }
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);        
        return true;
    }
    
    @Override
    public Data getData() {
       return data; 
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof TextVariable) data=(TextVariable)newdata; 
    }     
    
    @Override
    public void setConstraints(PromptConstraints promptconstraints) {
        constraints=promptconstraints;
        if (constraints!=null && constraints.getValue(PromptConstraints.ALLOWED_VALUES) instanceof String[]) {
            getCancelButton().setVisible(false); // if the values are constrained the user should not be allowed to default by pressing the cancel button but must explicitly select a single value
            String widget=(String)constraints.getValue(PromptConstraints.WIDGET);
            if (widget==null || !(widget.equalsIgnoreCase(PromptConstraints.TEXTBOX) || widget.equalsIgnoreCase(PromptConstraints.MENU) || widget.equalsIgnoreCase(PromptConstraints.LIST))) {
                // widget not specified or chosen widget is not applicable to Text data. Use a suitable default
                String[] allowedValues=(String[])constraints.getValue(PromptConstraints.ALLOWED_VALUES);
                if (allowedValues.length==0) widget=PromptConstraints.TEXTBOX;
                else widget=PromptConstraints.MENU;
            }
            constrainedValuesComponent=getValueComponent(widget, data.getFirstValue());
            if (constrainedValuesComponent instanceof JPanel) setMainPanel(constrainedValuesComponent);
            else {
                JPanel constrainedPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
                constrainedPanel.add(constrainedValuesComponent); 
                setMainPanel(constrainedPanel); 
            }
                                 
        } else {
            setMainPanel(internal); 
        }
        pack();
    }    
    
    private JComponent getValueComponent(String type, String value) {
        JComponent vcomponent=null;
        Object values=constraints.getValue(PromptConstraints.ALLOWED_VALUES);  
        boolean constrainedValues=(values instanceof String[] && ((String[])values).length>0);
        if (values instanceof String[] && ((String[])values).length==1) { // check if this is a special case where values should be takes from a data object
            String[] allowedValues=(String[])values;
            TextVariable temp=(TextVariable)engine.getDataItem(((String[])values)[0],TextVariable.class);
            if (temp!=null) {  // use values from specified Text Variable
                allowedValues=new String[temp.getNumberofStrings()];
                allowedValues=temp.getAllStrings().toArray(allowedValues);     
                values=allowedValues;
            }                                    
        }
        if (type==null) type=PromptConstraints.MENU;
        if (type.equalsIgnoreCase(PromptConstraints.MENU)) {
             if (constrainedValues) {
                 vcomponent=new JComboBox((String[])values);
                 ((JComboBox)vcomponent).setSelectedItem(value);                            
             }
        } else if (type.equalsIgnoreCase(PromptConstraints.LIST)) {
             if (constrainedValues) {
                 vcomponent=new JListWrapper((String[])values, value);
             }                  
        } 
        if (vcomponent==null) {// return regular textfield if not set above
           vcomponent=new JTextFieldWrapper(value);
        }
        return vcomponent;        
    }
    
    private class JListWrapper extends JPanel {
        JList list;
        
        public JListWrapper(String[] values, String value) {
             list=new JList(values);
             list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  
             list.setSelectedValue(value, true);
             this.setLayout(new BorderLayout());
             JScrollPane scroller=new JScrollPane(list);
             Dimension dim=new Dimension(getDataNameFieldDimensions().width,90);
             scroller.setPreferredSize(dim);
             scroller.setMinimumSize(dim);
             scroller.setMaximumSize(dim);
             this.add(scroller);
             list.addMouseListener(new java.awt.event.MouseAdapter() {
                 @Override
                 public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount()==2) {
                        getOKbutton().doClick();
                    }
                 } 
             });
        }
        
        public String getValue() {
            return (String)list.getSelectedValue();
        }
    }    
    
    private class JTextFieldWrapper extends JPanel {
        JTextField textfield;
        JLabel errorMessageLabel;
        
        public JTextFieldWrapper(String value) {
             textfield=new JTextField(30);
             textfield.setText(value);
             BoxLayout layout=new BoxLayout(this, BoxLayout.Y_AXIS);
             this.setLayout(layout);
             JPanel inner=new JPanel(new FlowLayout(FlowLayout.LEFT));
             inner.add(textfield);
             errorMessageLabel=new JLabel(" ");
             errorMessageLabel.setFont(errorMessageFont);
             errorMessageLabel.setForeground(java.awt.Color.RED);             
             this.add(inner);
             this.add(errorMessageLabel);
        }
        
        public String getValue() {
            return textfield.getText();
        }
        
        public void setError(String error) {
            errorMessageLabel.setText((error!=null)?error:"");
        }
    }      
    
}
