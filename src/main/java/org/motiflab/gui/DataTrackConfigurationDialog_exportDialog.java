/*
 * DataTrackConfigurationDialog_exportDialog.java
 *
 * Created on Jan 8, 2014, 2:04:04 PM
 */
package org.motiflab.gui;

import java.awt.Cursor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.datasource.DataConfiguration;
import org.motiflab.engine.datasource.DataSource;
import org.motiflab.engine.datasource.DataTrack;
import org.motiflab.engine.datasource.Server;

/**
 *
 * @author kjetikl
 */
public class DataTrackConfigurationDialog_exportDialog extends javax.swing.JDialog {
    SimpleDataPanelIcon DNAIcon;
    SimpleDataPanelIcon numericIcon;
    SimpleDataPanelIcon regionIcon;
    Icon serverIcon=null;
    Icon datasourceIcon=null;
    DataTrackConfigurationDialog parent;
    MotifLabGUI gui;
    DefaultTreeModel model;


    /** Creates new form DataTrackConfigurationDialog_exportDialog */
    public DataTrackConfigurationDialog_exportDialog(DataTrackConfigurationDialog parent, MotifLabGUI gui) {
        super(parent,"Export Datatracks Configuration", true);
        this.parent=parent;
        this.gui=gui;
        initComponents();
        numericIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.NUMERIC_TRACK_GRAPH_ICON,null);
        numericIcon.setForegroundColor(java.awt.Color.BLUE);
        numericIcon.setBackgroundColor(null);
        regionIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.REGION_ICON,null);
        regionIcon.setForegroundColor(java.awt.Color.GREEN);
        regionIcon.setBackgroundColor(null);
        DNAIcon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.SEQUENCE_ICON_BASES,null);
        DNAIcon.setForegroundColor(java.awt.Color.BLACK);
        datasourceIcon=new ImageIcon(getClass().getResource("resources/icons/database_16.png"));
        DataConfiguration configuration=parent.getWorkingDataConfiguration();
        model=new DefaultTreeModel(setupTreeModel(configuration));
        CheckBoxTree tree=new CheckBoxTree();
        tree.setModel(model);
        tree.setRootVisible(false);
        File exportFile=new File(gui.getLastUsedDirectory(),"DataTracks.xml");
        filenameTextfield.setText(exportFile.getAbsolutePath());
        scrollPane.setViewportView(tree);
        this.setSize(500, 400);
    }

    private CheckBoxNode setupTreeModel(DataConfiguration configuration) {
        CheckBoxNode root=new CheckBoxNode(null,"root");

        HashMap<String,DataTrack> tracks=configuration.getAvailableTracks();
        CheckBoxNode tracksRoot=new CheckBoxNode(null,"Datatracks");
        ArrayList<String> trackNames=new ArrayList<String>(tracks.keySet());
        Collections.sort(trackNames);
        for (String trackName:trackNames) {
            DataTrack track=tracks.get(trackName);
            Class datatype=track.getDataType();
            Icon useicon=null;
                 if (datatype==DNASequenceDataset.class) useicon=DNAIcon;
            else if (datatype==NumericDataset.class) useicon=numericIcon;
            else if (datatype==RegionDataset.class) useicon=regionIcon;
            CheckBoxNode tracknode=new CheckBoxNode(track, trackName, useicon, trackName);
            // add sources as children
            for (DataSource source:track.getDatasources()) {
                String build=source.getGenomeBuild();
                String organism=org.motiflab.engine.data.Organism.getCommonName(source.getOrganism());
                String serverAddress=source.getServerAddress();
                if (serverAddress==null) serverAddress="";
                String sourceName=organism+" ("+build+"): "+serverAddress;
                String sourceTooltip="<html>"+organism+" ("+build+")<br>Server = "+serverAddress+"<br>Protocol = "+source.getProtocol()+"<br>Data format = "+source.getDataFormat()+"</html>";
                CheckBoxNode servernode=new CheckBoxNode(source, sourceName, datasourceIcon, sourceTooltip);
                tracknode.add(servernode);
            }
            tracksRoot.add(tracknode);
        }

        HashMap<String,Server> servers=configuration.getServers();
        CheckBoxNode serverRoot=new CheckBoxNode(null,"Servers settings");
        ArrayList<String> serverNames=new ArrayList<String>(servers.keySet());
        Collections.sort(serverNames);
        for (String serverName:serverNames) {
            Server server=servers.get(serverName);
            CheckBoxNode servernode=new CheckBoxNode(server, serverName, serverIcon, serverName);
            serverRoot.add(servernode);
        }
        root.add(tracksRoot);
        root.add(serverRoot);
        return root;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        filenameTextfield = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        buttonsPanel = new javax.swing.JPanel();
        OKbutton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 4, 4));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        jPanel2.add(scrollPane, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(400, 36));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DataTrackConfigurationDialog_exportDialog.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(jLabel1, gridBagConstraints);

        filenameTextfield.setText(resourceMap.getString("filenameTextfield.text")); // NOI18N
        filenameTextfield.setName("filenameTextfield"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(filenameTextfield, gridBagConstraints);

        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(jButton1, gridBagConstraints);

        jPanel1.add(jPanel3, java.awt.BorderLayout.SOUTH);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setPreferredSize(new java.awt.Dimension(396, 26));
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel4.add(jLabel2);

        jPanel1.add(jPanel4, java.awt.BorderLayout.PAGE_START);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(400, 36));
        buttonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        OKbutton.setText(resourceMap.getString("OKbutton.text")); // NOI18N
        OKbutton.setName("OKbutton"); // NOI18N
        OKbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okPressed(evt);
            }
        });
        buttonsPanel.add(OKbutton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelPressed(evt);
            }
        });
        buttonsPanel.add(cancelButton);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okPressed
        String filename=filenameTextfield.getText();
        File file = gui.getEngine().getFile(filename);
         if (file.exists()) {
            int choice=JOptionPane.showConfirmDialog(this, "Overwrite existing file \""+file.getName()+"\" ?","Export Settings",JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);
            if (choice!=JOptionPane.OK_OPTION) return;
        }
        parent.saveSettingsToFile(file,getSelectedTracks());
        gui.setLastUsedDirectory(file.getParentFile());
        gui.logMessage("Exporting datatracks to: "+file.getAbsolutePath());
        setVisible(false);
    }//GEN-LAST:event_okPressed

    private void selectPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectPressed
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String filename=filenameTextfield.getText();
        File file = (filename!=null && !filename.isEmpty())?gui.getEngine().getFile(filename):null;
        File parentDir=(file!=null)?file.getParentFile():gui.getLastUsedDirectory();
        final JFileChooser fc = gui.getFileChooser(parentDir);// new JFileChooser();
        fc.setDialogTitle("Export Settings To File");
        fc.setSelectedFile(file);
        FileNameExtensionFilter filter=new FileNameExtensionFilter("XML-files (*.xml)", "xml","XML");
        fc.addChoosableFileFilter(filter);
        fc.setFileFilter(filter);
        int returnValue=fc.showSaveDialog(this);
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            file=fc.getSelectedFile();
            if (file!=null) filenameTextfield.setText(file.getAbsolutePath());
            if (file!=null) gui.setLastUsedDirectory(file.getParentFile());
        }
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }//GEN-LAST:event_selectPressed

    private void cancelPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelPressed
        setVisible(false);
    }//GEN-LAST:event_cancelPressed

    private DataConfiguration getSelectedTracks() {
        DataConfiguration selectedConfig=new DataConfiguration();
        CheckBoxNode root=(CheckBoxNode)model.getRoot();
        for (int i=0;i<root.getChildCount();i++) {
            CheckBoxNode topnode=(CheckBoxNode)root.getChildAt(i); // topnode represents all tracks or all servers
            for (int j=0;j<topnode.getChildCount();j++) {
                CheckBoxNode node=(CheckBoxNode)topnode.getChildAt(j); // this node represents a single track or server
                Object object=node.getUserObject();
                if (object instanceof Server && node.isChecked()) {
                    String name=((Server)object).getServerAddress();
                    Server copy=((Server)object).clone();
                    selectedConfig.addServer(name, copy);
                } else if (object instanceof DataTrack) {
                    ArrayList<DataSource> includeSources=new ArrayList<DataSource>();
                    for (int k=0;k<node.getChildCount();k++) {
                        CheckBoxNode sourcenode=(CheckBoxNode)node.getChildAt(k);
                        if (sourcenode.isChecked()) includeSources.add((DataSource)sourcenode.getUserObject());
                    }
                    if (!includeSources.isEmpty()) { // add both track and included sources
                        String name=((DataTrack)object).getName();
                        DataTrack copy=((DataTrack)object).cloneWithNewSources(includeSources);
                        selectedConfig.addDataTrack(name, copy);
                    }
                }
            }
        }
        return selectedConfig;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton OKbutton;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField filenameTextfield;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
