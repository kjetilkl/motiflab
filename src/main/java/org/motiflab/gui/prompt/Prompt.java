/*
 * Prompt.java
 *
 * Created on 25. mars 2009, 16:45
 */

package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.motiflab.engine.DataListener;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.dataformat.DataFormat;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.operations.PromptConstraints;
import org.motiflab.engine.protocol.DataTypeTable;
import org.motiflab.engine.protocol.StandardParametersParser;
import org.motiflab.engine.protocol.StandardProtocol;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.gui.MotifLabGUI;

/**
 *
 * @author  kjetikl
 */
public abstract class Prompt extends javax.swing.JDialog implements DataListener {
    protected MotifLabEngine engine;
    protected MotifLabGUI gui;
    private boolean okPressed=false;
    private boolean silentOK=false;
    private String existingDataName=null;
    private boolean iseditable=true;
    private String importFromFile_filename=null;
    private String importFromFile_dataformatname=null;
    private ParameterSettings importFromFile_dataformatsettings=null;
    private ActionListener callbackOnOK=null;
    protected StandardProtocol protocol=null;
    protected DataTypeTable datatypetable=null;
    protected boolean abortFlag=false; // used to signal that a lengthy task has been aborted by closing the dialog

    protected static Font errorMessageFont=new Font(Font.SANS_SERIF,Font.BOLD,12);

    /** Creates new form Prompt */

    public Prompt(MotifLabGUI gui, String prompt, boolean modal) {
        this(gui,prompt,modal,null,null);
    }

    public Prompt(MotifLabGUI gui, String prompt, boolean modal, StandardProtocol protocol, DataTypeTable table) {
        super(gui.getFrame(), modal);
        this.protocol=protocol;
        this.datatypetable=table;
        this.engine=gui.getEngine();
        this.gui=gui;
        initComponents();
        abortFlag=false;
        promptProgressBar.setVisible(false);
        if (prompt!=null) promptLabel.setText("<html><font color='red'><b>"+prompt+"</b></font><br>"+"&nbsp;</html>");
        else promptLabel.setText("");
        getRootPane().setDefaultButton(okButton);
        this.setMinimumSize(new Dimension(300,200));
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                engine.removeDataListener(Prompt.this);
            }
        });
    }

    /** Sets the value of the prompt's progressbar. Legal values should be in the range 0 to 100.
     *  Values outside this range will hide the progressbar, except the value Integer.MAX_VALUE
     *  which will show the progress as "indetermined".
     */
    public void setProgressInPrompt(final int value) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                promptProgressBar.setVisible((value>=0 && value<=100) || value==Integer.MAX_VALUE);
                promptProgressBar.setIndeterminate(value==Integer.MAX_VALUE);
                promptProgressBar.setValue(value);
                if (value>=0 && value<=100) promptProgressBar.setValue(value);
            } // end run()
          }; // end Runnable
          if (SwingUtilities.isEventDispatchThread()) runnable.run(); // invoke directly on EDT
          else {SwingUtilities.invokeLater(runnable);} // queue on EDT
    }

    /**
     * This method can be used to add a single generic callback listener
     * to this prompt which will be notified (by a call to actionPerformed)
     * after the OK button has been pressed (an not vetoed). Thus the call will
     * be the last thing the dialog does before it closes, after it calls setVisible(false)
     * on itself
     * @param listener
     */
    public void setCallbackOnOKPressed(ActionListener listener) {
        callbackOnOK=listener;
    }

    public void setHeaderVisible(boolean visible) {
        promptHeaderPanel.setVisible(visible);
    }

    /**
     * Specifies that an existing data item is shown in the prompt
     * @param data
     */
    public void setExistingDataItem(Data data) {
        if (data!=null) {
            existingDataName=data.getName();
            engine.addDataListener(this);
        } else existingDataName=null;
    }

    protected void setMainPanel(JComponent mainpanel) {
        mainPanel.removeAll();
        mainPanel.add(mainpanel,BorderLayout.CENTER);
    }

    /**
     * Returns TRUE if the dialog was closed by pressing the OK button (and not CANCEL)
     * @return
     */
    public boolean isOKPressed() {
        return okPressed;
    }

    /** Returns TRUE if the SHIFT button was held down when the OK button was pressed.
     *  This is a flag which indicates that the results should not be displayed in popup dialogs
     */
    public boolean isSilentOK() {
        return silentOK;
    }

    /** Returns TRUE if the 'abortFlag' is set */
    public boolean isAborted() {
       return abortFlag;
    }

    /**
     * If the prompt is used to import a data object from file, this method can be used to
     * store information about the file, fileformat and settings used (in the onOKPressed() method)
     * These settings can later be probed by getImportFromFileSettings or
     * @param filename
     * @param dataformatName
     * @param formatSettings
     */
    protected void setImportFromFileSettings(String filename, String dataformatName, ParameterSettings formatSettings) {
        importFromFile_filename=filename;
        importFromFile_dataformatname=dataformatName;
        importFromFile_dataformatsettings=formatSettings;
    }

    /**
     * This will return TRUE if the prompt was used to import a data object from file,
     * and setImportFromFileSettings() was later used to specify information about
     * filename, dataformat and settings
     * @return
     */
    public boolean isDataImportedFromFile() {
        return (importFromFile_filename!=null);
    }

    /**
     * If the prompt was used to import a data object from file (as specified by a call to setImportFromFileSettings())
     * this method can be used to retrieve the filename, dataformat and formatsettings used.
     *
     * @return an Object array containing (in order) filename (string), dataformatname (String) and dataformatsettings (ParameterSettings)
     */
    public Object[] getImportFromFileSettings() {
        return new Object[] {importFromFile_filename,importFromFile_dataformatname,importFromFile_dataformatsettings};
    }

    /**
     *
     * @param task
     * @return TRUE if the data object was imported from file and the settings have been set in the Task
     *         or FALSE if data object was not imported from file and no changes was made in the Task
     */
    public boolean setImportFromFileSettingsInTask(OperationTask task) {
        if (importFromFile_filename==null) return false;
        task.setParameter(Operation_new.PARAMETERS, Operation_new.FILE_PREFIX);  // required to signal import
        task.setParameter(Operation_new.FILENAME, importFromFile_filename);  //
        task.setParameter(Operation_new.DATA_FORMAT, importFromFile_dataformatname);
        task.setParameter(Operation_new.DATA_FORMAT_SETTINGS, importFromFile_dataformatsettings);
        return true;
    }

    /**
     * Returns TRUE if the prompt should allow the user to change the data item
     * @return
     */
    public boolean isDataEditable() {
        return iseditable;
    }

    /**
     * Specifies whether the prompt should allow the user to change the data item
     * If editing is not allowed, the usual OK/CANCEL-button pair will be replaced
     * by a single CLOSE-button.
     * @return
     */
    public void setDataEditable(boolean editable) {
        iseditable=editable;
        if (editable) {
            okButton.setVisible(true);
            cancelButton.setText("Cancel");
            pack();
        } else {
            okButton.setVisible(false);
            cancelButton.setText("Close");
            cancelButton.requestFocusInWindow();
            pack();
        }
    }

    /**
     * Specifies whether the prompt header (containing the prompt message and name of data element)
     * should be visible or not
     * @param visible
     */
    public void setPromptHeaderVisible(boolean visible) {
        promptHeaderPanel.setVisible(visible);
    }

    /** Programmatically click the OK button */
    public void clickOK() {
        okButton.doClick();
    }

    public void setDataItemName(String name) {
        dataItemNameTextField.setText(name);
    }

    public String getDataItemName() {
        return dataItemNameTextField.getText().trim();
    }

    public void setDataItemNameLabelText(String newText) {
        dataItemNameLabel.setText(newText);
    }

    public void setDataItemIcon(Icon icon, boolean border) {
        dataItemIconLabel.setIcon(icon);
        if (border) dataItemIconLabel.setBorder(BorderFactory.createRaisedBevelBorder());
        iconpaddinglabel.setText("  ");
    }

    public void setDataItemColorIcon(Icon icon) {
        dataItemNameLabel.setIcon(icon);
        dataItemNameLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        dataItemNameLabel.setIconTextGap(8);
    }

    public void disableNameEdit() {
        dataItemNameTextField.setEditable(false);
    }

    /** Transfers focus to OK button (or Close) */
    public void focusOKButton() {
        if (iseditable) okButton.requestFocusInWindow();
        else cancelButton.requestFocusInWindow();
    }

    /**
     * This method is called automatically when the user presses OK
     * Subclasses should implement this method to perform necessary data creations
     * The returned boolean value can be used to control whether the dialog should
     * close when the OK button was pressed. If something wrong has happened
     * (for instance the user has entered an illegal value) then the function can
     * return false to keep the dialog open
     */
    public abstract boolean onOKPressed();

    /** Returns the Data item that was prompted */
    public abstract Data getData();

    /** Used to set the current data object (which can differ in each subclass)
     */
    public abstract void setData(Data newdata);

    /** Returns a value that could be used to initialize a new dataitem based on the users selections */
    public String getValueAsParameterString() {
        Data data=getData();
        if (data!=null) {
            if (isDataImportedFromFile()) {
                String parameters=Operation_new.FILE_PREFIX+"\""+importFromFile_filename+"\"";
                if (importFromFile_dataformatname!=null) {
                    parameters+=", format="+importFromFile_dataformatname;
                    DataFormat formatter=engine.getDataFormat(importFromFile_dataformatname);
                    StandardParametersParser spp=(protocol!=null)?((StandardParametersParser)protocol.getParametersParser()):new StandardParametersParser(engine, datatypetable);
                    String settings=spp.getCommandString(formatter.getParameters(), importFromFile_dataformatsettings);
                    if (!settings.isEmpty()) parameters+=" {"+settings+"}";
                }
                return parameters;
            }
            else return data.getValueAsParameterString();
        }
        else return "";
    }


    public javax.swing.JPanel getControlsPanel() {
        return controlsPanel;
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        promptHeaderPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        dataItemNameLabel = new javax.swing.JLabel();
        dataItemNameTextField = new javax.swing.JTextField();
        iconpaddinglabel = new javax.swing.JLabel();
        dataItemIconLabel = new javax.swing.JLabel();
        promptMessagePanel = new javax.swing.JPanel();
        promptLabel = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        controlsPanel = new javax.swing.JPanel();
        buttonsPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        progressPanel = new javax.swing.JPanel();
        promptProgressBar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        promptHeaderPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        promptHeaderPanel.setAlignmentX(0.0F);
        promptHeaderPanel.setMinimumSize(new java.awt.Dimension(110, 10));
        promptHeaderPanel.setName("promptHeaderPanel"); // NOI18N
        promptHeaderPanel.setLayout(new java.awt.BorderLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 5, 5));
        jPanel3.setMaximumSize(new java.awt.Dimension(2147483647, 40));
        jPanel3.setMinimumSize(new java.awt.Dimension(50, 34));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(100, 34));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(Prompt.class);
        dataItemNameLabel.setText(resourceMap.getString("dataItemNameLabel.text")); // NOI18N
        dataItemNameLabel.setName("dataItemNameLabel"); // NOI18N
        jPanel3.add(dataItemNameLabel);

        dataItemNameTextField.setColumns(30);
        dataItemNameTextField.setText(resourceMap.getString("dataItemNameTextField.text")); // NOI18N
        dataItemNameTextField.setMaximumSize(new java.awt.Dimension(500, 40));
        dataItemNameTextField.setMinimumSize(new java.awt.Dimension(150, 20));
        dataItemNameTextField.setName("dataItemNameTextField"); // NOI18N
        dataItemNameTextField.setPreferredSize(new java.awt.Dimension(250, 20));
        jPanel3.add(dataItemNameTextField);

        iconpaddinglabel.setText(resourceMap.getString("iconpaddinglabel.text")); // NOI18N
        iconpaddinglabel.setName("iconpaddinglabel"); // NOI18N
        jPanel3.add(iconpaddinglabel);

        dataItemIconLabel.setText(resourceMap.getString("dataItemIconLabel.text")); // NOI18N
        dataItemIconLabel.setName("dataItemIconLabel"); // NOI18N
        jPanel3.add(dataItemIconLabel);

        promptHeaderPanel.add(jPanel3, java.awt.BorderLayout.PAGE_END);

        promptMessagePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        promptMessagePanel.setName("promptMessagePanel"); // NOI18N
        promptMessagePanel.setLayout(new javax.swing.BoxLayout(promptMessagePanel, javax.swing.BoxLayout.LINE_AXIS));

        promptLabel.setText(resourceMap.getString("promptLabel.text")); // NOI18N
        promptLabel.setName("promptLabel"); // NOI18N
        promptMessagePanel.add(promptLabel);

        promptHeaderPanel.add(promptMessagePanel, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(promptHeaderPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());
        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        controlsPanel.setName("controlsPanel"); // NOI18N
        controlsPanel.setLayout(new java.awt.BorderLayout());

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(200, 36));
        buttonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 5));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMaximumSize(new java.awt.Dimension(75, 27));
        okButton.setMinimumSize(new java.awt.Dimension(75, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.setPreferredSize(new java.awt.Dimension(75, 27));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });
        buttonsPanel.add(okButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setPreferredSize(new java.awt.Dimension(75, 27));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonPressed(evt);
            }
        });
        buttonsPanel.add(cancelButton);

        controlsPanel.add(buttonsPanel, java.awt.BorderLayout.EAST);

        progressPanel.setName("progressPanel"); // NOI18N
        progressPanel.setPreferredSize(new java.awt.Dimension(50, 10));
        progressPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        promptProgressBar.setName("promptProgressBar"); // NOI18N
        promptProgressBar.setPreferredSize(new java.awt.Dimension(40, 22));
        progressPanel.add(promptProgressBar);

        controlsPanel.add(progressPanel, java.awt.BorderLayout.WEST);

        getContentPane().add(controlsPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
     abortFlag=true;
     okPressed=false;
     engine.removeDataListener(Prompt.this);
     setVisible(false);
}//GEN-LAST:event_cancelButtonPressed

private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
    silentOK=false;
    String targetName=getDataItemName();
    String nameError=(getData() instanceof Sequence)?engine.checkSequenceNameValidity(targetName,false):engine.checkNameValidity(targetName,false);
    if (nameError!=null) {
        JOptionPane.showMessageDialog(this, nameError+":\n"+targetName, "Illegal data name", JOptionPane.ERROR_MESSAGE);
        return;
    }
    okPressed=true;
    silentOK=((evt.getModifiers() & ActionEvent.SHIFT_MASK)==ActionEvent.SHIFT_MASK);
    if (onOKPressed()) {
        engine.removeDataListener(Prompt.this);
        setVisible(false);
        if (callbackOnOK!=null) callbackOnOK.actionPerformed(new ActionEvent(Prompt.this,0,null));
    }
}//GEN-LAST:event_okButtonPressed

/** This method can be used to manually close the dialog in a state of "OK".
 *  This can be invoked if a lengthy operation has to be performed to create the data object
 *  after the "OK" button has been pressed and this is outsourced to a background-thread worker
 *  so the dialog cannot be closed immediately. The method should be called when the worker-thread
 *  is finished and the finished data object available for use. However, if the "CANCEL" button has
 *  been pressed in the meantime (raising the abortFlag), the method will return immediately without
 *  performing its work.
 */
private void manualOKoverride() {
    if (abortFlag) return;
    okPressed=true;
    engine.removeDataListener(Prompt.this);
    setVisible(false);
    if (callbackOnOK!=null) callbackOnOK.actionPerformed(new ActionEvent(Prompt.this,0,null));
}

protected Dimension getDataNameLabelDimensions() {
    return dataItemNameLabel.getSize();
}
protected Dimension getDataNameFieldDimensions() {
    return dataItemNameTextField.getSize();
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel controlsPanel;
    private javax.swing.JLabel dataItemIconLabel;
    private javax.swing.JLabel dataItemNameLabel;
    private javax.swing.JTextField dataItemNameTextField;
    private javax.swing.JLabel iconpaddinglabel;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel progressPanel;
    private javax.swing.JPanel promptHeaderPanel;
    private javax.swing.JLabel promptLabel;
    private javax.swing.JPanel promptMessagePanel;
    private javax.swing.JProgressBar promptProgressBar;
    // End of variables declaration//GEN-END:variables

    public javax.swing.JButton getOKbutton() {
        return okButton;
    }

    public javax.swing.JButton getCancelButton() {
        return cancelButton;
    }

    @Override
    public void dataAdded(Data data) {}

    @Override
    public void dataAddedToSet(Data parentDataset, Data child) {}

    @Override
    public void dataRemoved(Data data) {}

    @Override
    public void dataRemovedFromSet(Data parentDataset, Data child) {}

    @Override
    public void dataUpdated(Data data) {}

    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {
        if (oldvalue!=null && oldvalue.getName().equals(existingDataName)) {
            //new Throwable().printStackTrace();
            Runnable runner=new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(Prompt.this, "Data object '"+existingDataName+"' has been modified elsewhere.", "WARNING", JOptionPane.WARNING_MESSAGE);
                }
            };
            SwingUtilities.invokeLater(runner);
        }
    }

    /**
     * This method implements convenient functionality that can be used by subclass-prompts
     * that need a lot of time to create data objects after the user clicks the OK button.
     * Rather than creating the new data object on the EDT (and freeze up the GUI), the prompt
     * should then spawn a background process to create the object.
     * This method provides common framework functionality to help
     * - Spawn a background process in which to create the data object
     * - Allow the progress of the data creation to be reported by a progress bar
     * - Disables the OK button, but the CANCEL button can still be used to abort the process (and close the dialog)
     * - Reports on errors (and enables the OK button again) if something went wrong
     * - If all went right, it will close the dialog when the data creation is complete and make the data object available in the prompt by a call to the getData() method
     *
     * The only things required to use this method is that the subclass must override the following call-back method from the superclass (which just returns NULL in the super implementation)
     * - createDataObject(Object[], OperationTask): required to create the data object of a specific type according to a given set of parameters and return it
     *
     * The method should be called from the onOKpressed() method when the time has come to create the data object
     * and it should be immediately followed by a "return false;" statement to exit that method while still keeping the prompt dialog open.
     *
     * @param parameters This array should be filled with the parameters necessary to properly create the data object
     *                   These will be passed on directly to the createDataObject(Object[]) method
     */
    public void createDataObjectInBackground(final Object[] parameters) {
        final OperationTask task=new OperationTask("dummy"); // this task is used to report progress and to send abort signals
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(ExecutableTask.PROGRESS)) {
                    Object value=evt.getNewValue();
                    if (value instanceof Integer) setProgressInPrompt((Integer)value);
                    if (isAborted()) task.setStatus(ExecutableTask.ABORTED);
                }
            }
        });
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            Data data=null;
            @Override
            public Boolean doInBackground() {
                try {
                    setProgressInPrompt(0); // shows the progressbar
                    data=createDataObject(parameters, task);
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    setProgressInPrompt(101); // hides the progressbar
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                if (ex!=null) {
                     getOKbutton().setEnabled(true); // things went wrong. Enable the OK button so it can be pressed again
                     if (ex instanceof InterruptedException) return;
                     String exceptionText=ex.getClass().getSimpleName();
                     if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                     else exceptionText+=":";
                     JOptionPane.showMessageDialog(Prompt.this, "An error occurred while generating the data object:\n"+exceptionText+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
                } else if (data==null) {
                     getOKbutton().setEnabled(true); // something went wrong. Enable the OK button so it can be pressed again
                     JOptionPane.showMessageDialog(Prompt.this, "An unidentified error occurred while generating the data object","Unknown Error",JOptionPane.ERROR_MESSAGE);
                } else { // everything went OK
                     String newName=getDataItemName();
                     if (!data.getName().equals(newName)) data.rename(newName);
                     setData(data);
                     manualOKoverride();
                }
            }
        }; // end of SwingWorker class
        getOKbutton().setEnabled(false);
        worker.execute();
    }

    /**
     * This method is a call-back interface method called by createDataObjectInBackground() to create the actual data object.).
     * Subclasses that want to make use of createDataObjectInBackground() must therefore override this method
     * to return a valid data object created according to the specified parameters (which are the exact same as
     * the ones passed to the createDataObjectInBackground(Object[] parameters) method.
     * (since not every prompt will make use of this functionality, the method is not tagged as "abstract"
     *  and a default implementation is provided in the superclass which just returns NULL)
     * @param parameters The same parameters that were passed to createDataObjectInBackground
     * @param task This task can be used by the createDataObject to report on the progress (using its setProgress() method)
     *             The task should also be queried regularly to see if the method should abort (in which case it should just return a dummy object)
     */
    protected Data createDataObject(final Object[] parameters, OperationTask task) throws Exception {
        return null;
    }


    /** Sets additional constraints on the values that are allowed to be selected by the user */
    public void setConstraints(PromptConstraints constraint) {
        // super implementation does nothing
    }


}
