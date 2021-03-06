/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * DataTrackConfiguration_AddFromConfigFileDialog.java
 *
 * Created on Apr 4, 2013, 1:09:00 PM
 */
package motiflab.gui;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.datasource.DataConfiguration;
import motiflab.engine.SystemError;


/**
 *
 * @author kjetikl
 */
public class DataTrackConfiguration_AddFromConfigFileDialog extends javax.swing.JDialog {
    public static int DUPLICATE_SOURCES_ADD_AS_PREFERRED=0;
    public static int DUPLICATE_SOURCES_ADD_AS_MIRRORS=1;
    public static int DUPLICATE_SOURCES_REMOVE_OLD=2;
    public static int DUPLICATE_SOURCES_REPLACE_ALL_SOURCES=3;    
    public static int REPLACE_TRACK=4;    
    
    private MotifLabGUI gui;
    private DataConfiguration newconfig=null;
    private int modeFlag=DUPLICATE_SOURCES_ADD_AS_PREFERRED;
    
    private String defaultText=" ";
   
    
    /** Creates new form DataTrackConfiguration_AddFromConfigFileDialog */
    public DataTrackConfiguration_AddFromConfigFileDialog(DataTrackConfigurationDialog parent, MotifLabGUI gui) {
        super(parent, "Import configuration file", true);        
        this.gui=gui;
        initComponents();
        progressbar.setVisible(false);
        this.getRootPane().setDefaultButton(OKButton);
        textArea.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        defaultText=textArea.getText();
        if (defaultText.length()>10) defaultText=defaultText.substring(0,10);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        OpenFileButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        modeCombobox = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        OKButton = new javax.swing.JButton();
        CancelButton = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        jPanel9 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(700, 96));
        setName("Form"); // NOI18N

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 8, 1, 8));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DataTrackConfiguration_AddFromConfigFileDialog.class);
        OpenFileButton.setText(resourceMap.getString("OpenFileButton.text")); // NOI18N
        OpenFileButton.setName("OpenFileButton"); // NOI18N
        OpenFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileButtonPressed(evt);
            }
        });
        jPanel5.add(OpenFileButton);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jPanel5.add(jLabel3);

        jButton1.setIcon(resourceMap.getIcon("jButton1.icon")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setToolTipText(resourceMap.getString("jButton1.toolTipText")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButtonPressed(evt);
            }
        });
        jPanel5.add(jButton1);

        jPanel2.add(jPanel5, java.awt.BorderLayout.WEST);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.BorderLayout());

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setMinimumSize(new java.awt.Dimension(100, 14));
        jLabel2.setName("jLabel2"); // NOI18N
        jLabel2.setPreferredSize(new java.awt.Dimension(100, 14));
        jPanel4.add(jLabel2, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setToolTipText(resourceMap.getString("jLabel1.toolTipText")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel6.add(jLabel1);

        modeCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Add new sources as primary", "Add new sources as mirrors", "Replace conflicting sources", "Replace all sources", "Replace existing tracks" }));
        modeCombobox.setToolTipText(resourceMap.getString("modeCombobox.toolTipText")); // NOI18N
        modeCombobox.setName("modeCombobox"); // NOI18N
        jPanel6.add(modeCombobox);

        jPanel2.add(jPanel6, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_START);

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 8, 1, 8));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 300));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        textArea.setFont(resourceMap.getFont("textArea.font")); // NOI18N
        textArea.setText(resourceMap.getString("textArea.text")); // NOI18N
        textArea.setName("textArea"); // NOI18N
        jScrollPane1.setViewportView(textArea);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        OKButton.setText(resourceMap.getString("OKButton.text")); // NOI18N
        OKButton.setName("OKButton"); // NOI18N
        OKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OKButtonPressed(evt);
            }
        });
        jPanel7.add(OKButton);

        CancelButton.setText(resourceMap.getString("CancelButton.text")); // NOI18N
        CancelButton.setName("CancelButton"); // NOI18N
        CancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelButtonPressed(evt);
            }
        });
        jPanel7.add(CancelButton);

        jPanel3.add(jPanel7, java.awt.BorderLayout.EAST);

        jPanel8.setName("jPanel8"); // NOI18N

        progressbar.setMinimumSize(new java.awt.Dimension(100, 23));
        progressbar.setName("progressbar"); // NOI18N
        progressbar.setPreferredSize(new java.awt.Dimension(100, 23));
        progressbar.setRequestFocusEnabled(false);
        jPanel8.add(progressbar);

        jPanel3.add(jPanel8, java.awt.BorderLayout.WEST);

        jPanel9.setName("jPanel9"); // NOI18N
        jPanel3.add(jPanel9, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel3, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void openFileButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileButtonPressed
        JFileChooser filechooser = gui.getFileChooser(null); // new JFileChooser(gui.getLastUsedDirectory());
        filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        filechooser.setDialogTitle("Open data configuration file");
        FileNameExtensionFilter xmlFilter=new FileNameExtensionFilter("XML-files (*.xml)", "XML","xml");
        filechooser.addChoosableFileFilter(xmlFilter);
        filechooser.setFileFilter(xmlFilter);

        int status=filechooser.showOpenDialog(this);
        if (status==JFileChooser.APPROVE_OPTION) {
            File selected=filechooser.getSelectedFile();
            gui.setLastUsedDirectory(selected);
            try {
               loadFileInBackground(selected);
            } catch (Exception e) {}
        }   
    }//GEN-LAST:event_openFileButtonPressed

    private void CancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CancelButtonPressed
        newconfig=null;
        this.setVisible(false);
    }//GEN-LAST:event_CancelButtonPressed

    private void OKButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OKButtonPressed
        String mode=(String)modeCombobox.getSelectedItem();
        if (mode.equals("Add new sources as primary")) modeFlag=DUPLICATE_SOURCES_ADD_AS_PREFERRED;
        else if (mode.equals("Add new sources as mirrors")) modeFlag=DUPLICATE_SOURCES_ADD_AS_MIRRORS;
        else if (mode.equals("Replace conflicting sources")) modeFlag=DUPLICATE_SOURCES_REMOVE_OLD;
        else if (mode.equals("Replace all sources")) modeFlag=DUPLICATE_SOURCES_REPLACE_ALL_SOURCES;
        else if (mode.equals("Replace existing tracks")) modeFlag=REPLACE_TRACK;
        else modeFlag=DUPLICATE_SOURCES_ADD_AS_PREFERRED;
        
        String text=textArea.getText();
        text=text.trim();
        if (text.isEmpty() || text.startsWith(defaultText)) { // treat as if cancel was pressed when no file is present
            newconfig=null;
            this.setVisible(false);
            return;
        } 
        InputStream stream = new ByteArrayInputStream(text.getBytes());
        newconfig = new DataConfiguration();
        try {
           newconfig.loadConfigurationFromStream(stream);
           int newsize=newconfig.getAvailableTracks().size();
           if (newsize==0) throw new SystemError("No valid data track specifications found");
           this.setVisible(false);
        } catch (Exception e) {
           newconfig=null;
           String message=e.getMessage();
           JOptionPane.showMessageDialog(DataTrackConfiguration_AddFromConfigFileDialog.this, message, "Parse Error", JOptionPane.ERROR_MESSAGE);            
        }
    }//GEN-LAST:event_OKButtonPressed

    private void helpButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButtonPressed
        Object help=null;
        try {
           help=new java.net.URL(gui.getEngine().getWebSiteURL()+"getHelp.php?section=documentation_Configure_datatracks_XML");
        } catch (Exception e) {
            help="Help unavailable";
        }   
        InfoDialog infodialog=null;   
        if (help instanceof String) infodialog=new InfoDialog(gui, "Help for Datatracks configuration", (String)help, 700, 450);
        else if (help instanceof java.net.URL) infodialog=new InfoDialog(gui, "Help for Datatracks configuration", (java.net.URL)help, 700, 450, false);                    
        if (infodialog!=null) {
            infodialog.setErrorMessage("Help unavailable");  
            infodialog.setVisible(true);
        }
    }//GEN-LAST:event_helpButtonPressed

    public DataConfiguration getConfiguration() {
        return newconfig;
    }
    
    public int getTreatDuplicatesMode() {
        return modeFlag;
    }    
    
    /** Loads and displays a file  */
    private void loadFileInBackground(final File file) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            StringBuilder text=new StringBuilder();

            @Override 
            public Boolean doInBackground() {
                progressbar.setVisible(true);
                progressbar.setIndeterminate(true);            
                BufferedReader inputStream=null;
                try {
                    InputStream input = MotifLabEngine.getInputStreamForFile(file);
                    inputStream=new BufferedReader(new InputStreamReader(input));
                    String line;
                    while((line=inputStream.readLine())!=null) {
                        text.append(line);
                        text.append("\n");
                    }
                } catch (Exception e) { 
                    ex=e;
                    return Boolean.FALSE;
                } finally {
                    try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {}
                }
                return Boolean.TRUE;
            }
                       
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setIndeterminate(false); 
                progressbar.setVisible(false);
                if (ex!=null) {
                    String message=ex.getMessage();
                    JOptionPane.showMessageDialog(DataTrackConfiguration_AddFromConfigFileDialog.this, message, "Import Error", JOptionPane.ERROR_MESSAGE);
                } else {
                   textArea.setText(text.toString());
                   textArea.revalidate();
                }
            }
        }; // end of SwingWorker class
        
        worker.execute();
    }     
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton CancelButton;
    private javax.swing.JButton OKButton;
    private javax.swing.JButton OpenFileButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox modeCombobox;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JTextArea textArea;
    // End of variables declaration//GEN-END:variables
}
