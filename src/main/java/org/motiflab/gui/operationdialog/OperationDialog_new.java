/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.CompoundTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.*;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.gui.Favorite;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.prompt.*;


/**
 *
 * @author kjetikl
 */
public class OperationDialog_new extends OperationDialog {
    private JPanel nameAndTypePanel;
    private JPanel parametersPanel;
    private JTextField targetDataTextfield;
    private JTextArea parametersTextarea;
    private JTextArea descriptionTextarea;
    private JComboBox dataTypeCombobox;
    private JButton configureButton;
    private Data dataobject=null;
    private Object[] importedFromFileSettings=null; // used if a prompt is used to import object from file
    
    private String currentdataname=null;
    private boolean userDefinedName=false; // true if the name of the data object has been typed by the user, false if it is a system generated default name
    private boolean usedByFavorites=false;
    
    
    public OperationDialog_new(JFrame parent) {
        super(parent);   
    }
    
    public OperationDialog_new() {
        super();   
    }
    
    public void initializeForUseInFavorites(MotifLabGUI gui, Favorite favorite) {
        this.setModal(true);
        this.engine=gui.getEngine();
        this.gui=gui;
        OperationTask task=new OperationTask("temp");
        task.setParameter(OperationTask.OPERATION_NAME, "new");
        Operation operation=engine.getOperation("new");
        task.setParameter(OperationTask.OPERATION, operation);
        task.setParameter(OperationTask.ENGINE, engine);      
        usedByFavorites=true;
        if (favorite!=null) {
            task.setParameter(OperationTask.TARGET_NAME, favorite.getName());
            task.setParameter(Operation_new.DATA_TYPE, favorite.getType());
            task.setParameter("Favorites_description", favorite.getDescription());
            task.setParameter(Operation_new.PARAMETERS, favorite.getParameter());
            if (operation instanceof Operation_new) {
                try {
                   dataobject=((Operation_new)operation).createDataItem(task);
                } catch (Exception e) {}
            }
        }
        initialize(task, null, gui);        
        setTitle((favorite!=null)?"Edit Favorite":"Add New Favorite");
        helpButton.setVisible(false);
    }    
    
    @Override
    public void initComponents() {
        super.initComponents();
        nameAndTypePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        nameAndTypePanel.setBorder(commonBorder);
        nameAndTypePanel.add(new JLabel("Name "));
        targetDataTextfield=new JTextField();
        nameAndTypePanel.add(targetDataTextfield);
        targetDataTextfield.setColumns(16);
        String currentdatatype=(String)parameters.getParameter(Operation_new.DATA_TYPE);
        String currentParametersString=(String)parameters.getParameter(Operation_new.PARAMETERS);
        String[] datatypes=Operation_new.getAvailableTypes();
        dataTypeCombobox = new JComboBox(datatypes);
        dataTypeCombobox.setEditable(false);
        nameAndTypePanel.add(new JLabel("      Type  "));
        nameAndTypePanel.add(dataTypeCombobox);  
        add(nameAndTypePanel);        
        if (usedByFavorites) {
            JPanel descriptionPanel=new JPanel(new BorderLayout());
            descriptionPanel.setBorder(commonBorder);
            descriptionPanel.add(new JLabel(" Description "),BorderLayout.NORTH);
            descriptionTextarea=new JTextArea(4,30);
            String currentDescriptionString=(String)parameters.getParameter("Favorites_description");
            descriptionTextarea.setText(currentDescriptionString);
            descriptionPanel.add(new JScrollPane(descriptionTextarea));
            add(descriptionPanel);
        }
        parametersPanel=new JPanel(new BorderLayout());
        parametersPanel.setBorder(commonBorder);
        parametersPanel.add(new JLabel(" Parameters "),BorderLayout.NORTH);
        parametersTextarea=new JTextArea(8,30);
        parametersPanel.add(new JScrollPane(parametersTextarea));
        add(parametersPanel);
        JPanel buttonsPanel=new JPanel();
        buttonsPanel.setLayout(new BorderLayout());
        configureButton=new JButton("Configure");
        JPanel configbuttonpanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        configbuttonpanel.add(configureButton);
        buttonsPanel.add(configbuttonpanel,BorderLayout.WEST);
        buttonsPanel.add(getOKCancelButtonsPanel(),BorderLayout.EAST);
        add(buttonsPanel);
        pack();        
        configureButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperationDialog_new.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                configureUsingPrompt();
                OperationDialog_new.this.setCursor(Cursor.getDefaultCursor());
            }
        });
        dataTypeCombobox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selected=(String)dataTypeCombobox.getSelectedItem();
                parametersTextarea.setText("");
                if (selected.equals(NumericVariable.getType()) || selected.equals(Sequence.getType())) {
                   configureButton.setVisible(false);
                   //parametersTextarea.setEditable(true);
                } else {
                   configureButton.setVisible(true);
                   //parametersTextarea.setEditable(false);                    
                }
                updateDataName(selected);
            }
        });
        currentdataname=(String)parameters.getParameter(OperationTask.TARGET_NAME);
        if (currentdataname!=null && !currentdataname.isEmpty()) userDefinedName=true;
        targetDataTextfield.setText(currentdataname); 
        if (currentdatatype!=null) setSelectedDatatype(dataTypeCombobox,currentdatatype);
        else dataTypeCombobox.setSelectedIndex(0);
        targetDataTextfield.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {propertyChange();}
            @Override public void removeUpdate(DocumentEvent e) {propertyChange();}
            @Override public void changedUpdate(DocumentEvent e) {propertyChange();}                                        
            private void propertyChange() {
                String newName=targetDataTextfield.getText().trim();
                if (!currentdataname.equals(newName)) userDefinedName=true;
            }
        });
        if (currentParametersString!=null) {
            if (currentdatatype!=null && currentdatatype.equals(TextVariable.getType())) currentParametersString=getTextFromTextVariable(currentParametersString);
            parametersTextarea.setText(currentParametersString);
        }
    }

    private void updateDataName(String typename) {
        if (userDefinedName) return; // do not change user defined names
        Class type=engine.getDataClassForTypeName(typename);
        currentdataname=gui.getGenericDataitemName(type, getDataTypeTable());    
        targetDataTextfield.setText(currentdataname);
        userDefinedName=false; 
    }    
    
    /** This method circumvents issues with different case in data type names*/
    private void setSelectedDatatype(JComboBox box, String datatypename) {
        ComboBoxModel model=box.getModel();
        for (int i=0;i<model.getSize();i++) {
           if (((String)model.getElementAt(i)).equalsIgnoreCase(datatypename)) {
               box.setSelectedIndex(i); break;
           } 
        }       
    }
    
    /* This method is overridden here in order to return a compound task if necessary */
    @Override
    public ExecutableTask getOperationTask() {
        if (dataobject instanceof MotifCollection && ((MotifCollection)dataobject).isImportedCollection()) {
            boolean importAsCompound=false; // this functionality is actually disabled
            if (importAsCompound) return importCollectionAsCompoundTask((MotifCollection)dataobject);
            else {
               return parameters;   
            }
        } else { // not imported MotifCollection
            return parameters; 
        }
    }
    
    /** When using this method the "import Motif Collection from file" */
    private CompoundTask importCollectionAsCompoundTask(MotifCollection collection) {
            CompoundTask compoundTask=new CompoundTask("new "+MotifCollection.getType());
            List<Motif> motifslist=collection.getPayload();
            String parameterString="";
            for (int i=0;i<motifslist.size();i++) {
                Motif motif=motifslist.get(i);
                String motifname=motif.getName();
                if (i==motifslist.size()-1) parameterString+=motifname;
                else parameterString+=motifname+",";
                OperationTask newmotiftask=new OperationTask("new");
                newmotiftask.setParameter(OperationTask.OPERATION_NAME, "new");
                newmotiftask.setParameter(Operation_new.DATA_TYPE, Motif.getType());
                newmotiftask.setParameter(OperationTask.TARGET_NAME,motifname);
                newmotiftask.setParameter(OperationTask.ENGINE,gui.getEngine());                    
                newmotiftask.setParameter(Operation_new.PARAMETERS, motif.getValueAsParameterString());
                compoundTask.addTask(newmotiftask);
            }
            parameters.setParameter(Operation_new.PARAMETERS, parameterString); // new MotifCollection task
            compoundTask.addTask(parameters);  
            return compoundTask;        
    }
    
    @Override
    protected void setParameters() {
        //super.setParameters(); // sets the common feature-transform parameters like 'source','target','where' and 'collection'
        String type=(String)dataTypeCombobox.getSelectedItem();
        parameters.setParameter(Operation_new.DATA_TYPE, type);
        String dataname=targetDataTextfield.getText().trim();
        if (dataname.isEmpty()) dataname="X";
        parameters.setParameter(OperationTask.TARGET_NAME, dataname);
        if (!type.equals(TextVariable.getType())) preprocessParametersTextarea(type);
        String parameterstext=parametersTextarea.getText();
        if (type.equals(TextVariable.getType())) parameterstext=preprocessTextForTextVariable(parameterstext);
        if (importedFromFileSettings!=null && importedFromFileSettings[0]!=null) {
            parameters.setParameter(Operation_new.PARAMETERS, Operation_new.FILE_PREFIX);  // required to signal import
            parameters.setParameter(Operation_new.FILENAME, importedFromFileSettings[0]);  //
            parameters.setParameter(Operation_new.DATA_FORMAT, importedFromFileSettings[1]);
            parameters.setParameter(Operation_new.DATA_FORMAT_SETTINGS, importedFromFileSettings[2]);
        }
        else if(!parameterstext.isEmpty()) parameters.setParameter(Operation_new.PARAMETERS, parameterstext);
        else parameters.setParameter(Operation_new.PARAMETERS, null);
        if (dataobject!=null) dataobject.setAdditionalOperationNewTaskParameters(parameters);
        Class typeclass=engine.getDataClassForTypeName(dataname);
        parameters.addAffectedDataObject(dataname, typeclass);
        if (usedByFavorites) {
            String description=descriptionTextarea.getText();
            if (description==null) description=""; else description=description.trim();
            parameters.setParameter("Favorites_description", description);
        }
    }
    
    private void configureUsingPrompt() {
        String dataname=(usedByFavorites)?"temp":targetDataTextfield.getText().trim();
        String type=(String)dataTypeCombobox.getSelectedItem();
        preprocessParametersTextarea(type);
        String parameterstring=parametersTextarea.getText();
        Prompt prompt=null;
        if (type.equals(NumericDataset.getType())) {
              prompt=new Prompt_NumericDataset(gui, null, dataname,parameterstring);
        }
        else if (type.equals(RegionDataset.getType())) {
              prompt=new Prompt_RegionDataset(gui, null, dataname,parameterstring);         
        }
        else if (type.equals(DNASequenceDataset.getType())) {
              prompt=new Prompt_DNASequenceDataset(gui, null, dataname,parameterstring,this);         
        }
        else if (type.equals(BackgroundModel.getType())) {
            prompt=new Prompt_BackgroundModel(gui, null, (dataobject instanceof BackgroundModel)?(BackgroundModel)dataobject:null);         
        }
        else if (type.equals(SequenceCollection.getType())) {
            prompt=new Prompt_SequenceCollection(gui, null, (dataobject instanceof SequenceCollection)?(SequenceCollection)dataobject:null);      
        }
        else if (type.equals(SequencePartition.getType())) {
            prompt=new Prompt_SequencePartition(gui, null, (dataobject instanceof SequencePartition)?(SequencePartition)dataobject:null);
        }
        else if (type.equals(MotifPartition.getType())) {
            prompt=new Prompt_MotifPartition(gui, null, (dataobject instanceof MotifPartition)?(MotifPartition)dataobject:null);
        }
        else if (type.equals(ModulePartition.getType())) {
            prompt=new Prompt_ModulePartition(gui, null, (dataobject instanceof ModulePartition)?(ModulePartition)dataobject:null);
        }
        else if (type.equals(TextVariable.getType())) {
            prompt=new Prompt_TextVariable(gui, null, (dataobject instanceof TextVariable)?(TextVariable)dataobject:null);
        }
        else if (type.equals(MotifCollection.getType())) {
            prompt=new Prompt_MotifCollection(gui, null, (dataobject instanceof MotifCollection)?(MotifCollection)dataobject:null);
        }
        else if (type.equals(ModuleCollection.getType())) {
            if (dataobject!=null && dataobject instanceof ModuleCollection)
            prompt=new Prompt_ModuleCollection(gui, null, (ModuleCollection)dataobject,getDataTypeTable());
            else prompt=new Prompt_ModuleCollection(gui, null, null);
        }
        else if (type.equals(Motif.getType())) {
            prompt=new Prompt_Motif(gui, null, (dataobject instanceof Motif)?(Motif)dataobject:null, gui.getVisualizationSettings());
        }
        else if (type.equals(ModuleCRM.getType())) {
            prompt=new Prompt_Module(gui, null, (dataobject instanceof ModuleCRM)?(ModuleCRM)dataobject:null);
        }
        else if (type.equals(ExpressionProfile.getType())) {
            prompt=new Prompt_ExpressionProfile(gui, null, (dataobject instanceof ExpressionProfile)?(ExpressionProfile)dataobject:null);
        }
        else if (type.equals(PriorsGenerator.getType())) {
            prompt=new Prompt_PriorsGenerator(gui, null, (dataobject instanceof PriorsGenerator)?(PriorsGenerator)dataobject:null);
        }
        else if (type.equals(SequenceNumericMap.getType())) {
            prompt=new Prompt_SequenceNumericMap(gui, null, (dataobject instanceof SequenceNumericMap)?(SequenceNumericMap)dataobject:null);
        }
        else if (type.equals(MotifNumericMap.getType())) {
            prompt=new Prompt_MotifNumericMap(gui, null, (dataobject instanceof MotifNumericMap)?(MotifNumericMap)dataobject:null);
        }
        else if (type.equals(ModuleNumericMap.getType())) {
            prompt=new Prompt_ModuleNumericMap(gui, null, (dataobject instanceof ModuleNumericMap)?(ModuleNumericMap)dataobject:null);
        }
        else if (type.equals(SequenceTextMap.getType())) {
            prompt=new Prompt_SequenceTextMap(gui, null, (dataobject instanceof SequenceTextMap)?(SequenceTextMap)dataobject:null);
        }
        else if (type.equals(MotifTextMap.getType())) {
            prompt=new Prompt_MotifTextMap(gui, null, (dataobject instanceof MotifTextMap)?(MotifTextMap)dataobject:null);
        }
        else if (type.equals(ModuleTextMap.getType())) {
            prompt=new Prompt_ModuleTextMap(gui, null, (dataobject instanceof ModuleTextMap)?(ModuleTextMap)dataobject:null);
        }        
        if (prompt!=null) {
              prompt.setDataItemName(dataname);
              prompt.setLocation(this.getX()+this.getWidth()/2-prompt.getWidth()/2,this.getY()+this.getHeight()/2-prompt.getHeight()/2);
              prompt.disableNameEdit();
              prompt.setPromptHeaderVisible(false);
              prompt.setVisible(true);
              if (prompt.isOKPressed()) {
                 parameterstring=prompt.getValueAsParameterString();
                 parametersTextarea.setText(parameterstring);
                 dataobject=prompt.getData();
                 if (prompt.isDataImportedFromFile()) importedFromFileSettings=prompt.getImportFromFileSettings();
                 else importedFromFileSettings=null;
              }
              prompt.dispose();
        }
        
    }
    
    
    private void preprocessParametersTextarea(String type) {        
        String val = parametersTextarea.getText().trim();
        //val=val.replaceAll("\n\\s*\n", "\n"); // remove empty lines
             if (type.equals(SequenceCollection.getType()) || type.equals(SequencePartition.getType()) || type.equals(MotifCollection.getType()) || type.equals(MotifPartition.getType()) || type.equals(ModuleCollection.getType())) val=val.replaceAll(",?\n", ","); // replace newlines with commas
        else if (type.equals(Motif.getType())) val=val.replaceAll(";?\n", ";"); // replace newlines with commas
        else val=val.replaceAll("\n", " "); // replace newlines with spaces
        parametersTextarea.setText(val);
    }
    
    /** If the text is not already properly escaped, do so!*/
    private String preprocessTextForTextVariable(String text) {
        if (text.endsWith("\n")) text=text.substring(0,text.length()-1); // remove ending newline to avoid that the splitting beneath causes an unecessary empty line at the end
        String lines[] = text.split("\n",-1); // the -1 parameter will keep empty lines at the end of the document instead of discarding them        
        StringBuilder builder=new StringBuilder();
        boolean first=true;
        for (String line:lines) {
            line=MotifLabEngine.escapeQuotedString(line);
            if (first) first=false; else builder.append(",");
            builder.append("\"");
            builder.append(line);
            builder.append("\"");
        }
        return builder.toString();
    }
    
    private String getTextFromTextVariable(String parameterstring) {
        try {
            ArrayList<String> list=MotifLabEngine.splitOnComma(parameterstring);
            StringBuilder builder=new StringBuilder();
            for (String line:list) {
                line=MotifLabEngine.unescapeQuotedString(line);
                builder.append(line);
                builder.append("\n");
            }
            return builder.toString();
        } catch (ParseError p) {return "";}
    }
}
