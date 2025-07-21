/*
 * DataTrackConfigurationDialog_importDialog.java
 *
 * Created on Jan 8, 2014, 2:04:04 PM
 */
package org.motiflab.gui;

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.SystemError;
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
public class DataTrackConfigurationDialog_importDialog extends javax.swing.JDialog {
    SimpleDataPanelIcon DNAIcon;
    SimpleDataPanelIcon numericIcon;
    SimpleDataPanelIcon regionIcon;
    Icon serverIcon=null;
    Icon datasourceIcon=null;
    DataTrackConfigurationDialog parent;
    MotifLabGUI gui;
    CheckBoxTree tree;
    DefaultTreeModel model;


    /** Creates new form DataTrackConfigurationDialog_importDialog */
    public DataTrackConfigurationDialog_importDialog(DataTrackConfigurationDialog parent, MotifLabGUI gui) {
        super(parent,"Import Datatracks Configuration", true);
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
        CheckBoxNode rootNode=setupTreeModel(null);
        model=new DefaultTreeModel(rootNode);
        tree=new CheckBoxTree();
        tree.setModel(model);
        tree.setRootVisible(false);
        scrollPane.setViewportView(tree);
        this.setSize(500, 400);
        browseButton.requestFocus();
    }

    private CheckBoxNode setupTreeModel(DataConfiguration configuration) {
        CheckBoxNode root=new CheckBoxNode(null,"root");
        if (configuration==null) return root;
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
        browseButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        buttonsPanel = new javax.swing.JPanel();
        OKbutton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        modeCombobox = new javax.swing.JComboBox();

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

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DataTrackConfigurationDialog_importDialog.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(jLabel1, gridBagConstraints);

        filenameTextfield.setText(resourceMap.getString("filenameTextfield.text")); // NOI18N
        filenameTextfield.setName("filenameTextfield"); // NOI18N
        filenameTextfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enterPressedInFileTextField(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(filenameTextfield, gridBagConstraints);

        browseButton.setText(resourceMap.getString("browseButton.text")); // NOI18N
        browseButton.setName("browseButton"); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectPressed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        jPanel3.add(browseButton, gridBagConstraints);

        jPanel1.add(jPanel3, java.awt.BorderLayout.SOUTH);

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel5.add(jLabel2);

        jPanel1.add(jPanel5, java.awt.BorderLayout.NORTH);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.BorderLayout());

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(150, 10));
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

        jPanel7.add(buttonsPanel, java.awt.BorderLayout.EAST);

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        modeCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Add new sources as primary", "Add new sources as mirrors", "Replace conflicting sources", "Replace all sources for existing tracks", "Replace existing tracks", "Replace whole configuration" }));
        modeCombobox.setToolTipText(resourceMap.getString("modeCombobox.toolTipText")); // NOI18N
        modeCombobox.setName("modeCombobox"); // NOI18N
        jPanel6.add(modeCombobox);

        jPanel7.add(jPanel6, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel7, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okPressed
        String infoMessage=updateConfigurationBasedOnSelections();
        setVisible(false);
        if (infoMessage!=null) {
            InfoDialog infodialog=new InfoDialog(gui, "Imported data tracks and sources", infoMessage,500,300);
            infodialog.setMonospacedFont(12);
            infodialog.setVisible(true);
            infodialog.dispose();
        }
    }//GEN-LAST:event_okPressed

    private void selectPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectPressed
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final JFileChooser fc = gui.getFileChooser(null);// new JFileChooser();
        fc.setDialogTitle("Import Settings From File");
        FileNameExtensionFilter filter=new FileNameExtensionFilter("XML-files (*.xml)", "xml","XML");
        fc.addChoosableFileFilter(filter);
        fc.setFileFilter(filter);
        int returnValue=fc.showOpenDialog(this);
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            File file=fc.getSelectedFile();
            if (file!=null) filenameTextfield.setText(file.getAbsolutePath());
            if (file!=null) gui.setLastUsedDirectory(file.getParentFile());
        }
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setupTreeModelFromFile();
    }//GEN-LAST:event_selectPressed

    private void cancelPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelPressed
        setVisible(false);
    }//GEN-LAST:event_cancelPressed

    private void enterPressedInFileTextField(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enterPressedInFileTextField
        String filename=filenameTextfield.getText();
        if (filename==null || filename.trim().isEmpty()) return;
        setupTreeModelFromFile();
    }//GEN-LAST:event_enterPressedInFileTextField

    private void setupTreeModelFromFile() {
        String filename=filenameTextfield.getText().trim();
        File file = (filename!=null && !filename.isEmpty())?gui.getEngine().getFile(filename):null;
        try {
            if (file==null || !file.exists()) throw new SystemError("File does not exist");
            if (!file.canRead()) throw new SystemError("Unable to read file");
            InputStream stream=MotifLabEngine.getInputStreamForFile(file);
            DataConfiguration newconfig=new DataConfiguration();
            newconfig.loadConfigurationFromStream(stream);
            model=new DefaultTreeModel(setupTreeModel(newconfig));
            tree.setModel(model);
            tree.setRootVisible(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
        }

    }


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

    private String updateConfigurationBasedOnSelections() {
        DataConfiguration configuration=parent.getWorkingDataConfiguration();
        HashMap<String,DataTrack> availableTracks=configuration.getAvailableTracks(); // reference to the actual object in the current dataconfiguration
        HashMap<String,Server> availableServers=configuration.getServers();        // reference to the actual object in the current dataconfiguration

        DataConfiguration selectedconfig=getSelectedTracks();
        HashMap<String,DataTrack> datatracks=selectedconfig.getAvailableTracks();
        HashMap<String,Server> servers=selectedconfig.getServers();
        if ((datatracks==null || datatracks.isEmpty()) && (servers==null || servers.isEmpty())) return null; // nothing new here...
        StringBuilder builder=new StringBuilder("<table border=0>");

        String modeString=(String)modeCombobox.getSelectedItem();
        int mode=DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_PREFERRED;
        if (modeString.equals("Add new sources as primary")) mode=DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_PREFERRED;
        else if (modeString.equals("Add new sources as mirrors")) mode=DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_MIRRORS;
        else if (modeString.equals("Replace conflicting sources")) mode=DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_REMOVE_OLD;
        else if (modeString.equals("Replace all sources for existing tracks")) mode=DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_REPLACE_ALL_SOURCES;
        else if (modeString.equals("Replace existing tracks")) mode=DataTrackConfiguration_AddFromConfigFileDialog.REPLACE_TRACK;
        else if (modeString.equals("Replace whole configuration")) {
            mode=DataTrackConfiguration_AddFromConfigFileDialog.REPLACE_TRACK;
            builder.append("<tr><td>&nbsp;</td><td><font color=\"#000000\">&nbsp;&nbsp;&nbsp;DELETED&nbsp;&nbsp;&nbsp;</font></td><td>");
            int trackssize=availableTracks.size();
            builder.append(trackssize);
            builder.append("&nbsp;old track"+((trackssize!=1)?"s":""));
            builder.append("</td></tr>");
            builder.append("<tr><td>&nbsp;</td><td><font color=\"#000000\">&nbsp;&nbsp;&nbsp;DELETED&nbsp;&nbsp;&nbsp;</font></td><td>");
            int serverssize=availableServers.size();
            builder.append(serverssize);
            builder.append("&nbsp;old server"+((serverssize!=1)?"s":""));
            builder.append("</td></tr>");
            availableTracks.clear();
            availableServers.clear();
        }

        for (String trackname:datatracks.keySet()) {
            DataTrack newtrack=datatracks.get(trackname);
            if (!availableTracks.containsKey(trackname)) {
                availableTracks.put(trackname,newtrack); // no such track from before. Just add it
                builder.append("<tr><td>");
                builder.append(trackname);
                builder.append("</td><td><font color=\"#00BB00\">&nbsp;&nbsp;&nbsp;NEW TRACK&nbsp;&nbsp;&nbsp;</font></td><td>");
                int size=newtrack.getDatasources().size();
                builder.append(size);
                builder.append("&nbsp;source"+((size!=1)?"s":""));
                builder.append("</td></tr>");
            } else { // a track with the same name already exists. Now we must either replace the track or merge the sources (after first checking that it is actually compatible)
                DataTrack oldTrack=availableTracks.get(trackname);
                builder.append("<tr><td>");
                builder.append(trackname);
                if (mode==DataTrackConfiguration_AddFromConfigFileDialog.REPLACE_TRACK) {
                    availableTracks.put(trackname, newtrack); // this will replace the whole track with the new
                    builder.append("</td><td><font color=\"#FF0000\">&nbsp;&nbsp;&nbsp;REPLACED TRACK&nbsp;&nbsp;&nbsp;</font></td><td>");
                    int size=newtrack.getDatasources().size();
                    builder.append(size);
                    builder.append("&nbsp;source"+((size!=1)?"s":""));
                    builder.append("</td></tr>");
                    continue;
                }
                if (!oldTrack.getDataType().equals(newtrack.getDataType())) { // type conflict with existing track
                    builder.append("</td><td colspan=\"2\"><font color=\"#FF0000\">&nbsp;&nbsp;&nbsp;***&nbsp;INCOMPATIBLE&nbsp;***&nbsp;&nbsp;&nbsp;</font></td></tr>");
                    continue;
                }
                boolean doMerge=(mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_PREFERRED || mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_MIRRORS);
                String mergeString=(doMerge)?"MERGED":"REPLACED";
                builder.append("</td><td><font color=\"#FFA500\">&nbsp;&nbsp;&nbsp;"+mergeString+"&nbsp;&nbsp;&nbsp;</font></td><td>");
                int size=newtrack.getDatasources().size();
                builder.append(size);
                builder.append("&nbsp;new source"+((size!=1)?"s":""));
                builder.append("</td></tr>");
                ArrayList<DataSource> newsources=newtrack.getDatasources();
                if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_PREFERRED) {
                    oldTrack.addPreferredDataSources(newsources);
                } else if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_ADD_AS_MIRRORS) {
                    oldTrack.addDataSources(newsources);
                } else if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_REMOVE_OLD) {
                    oldTrack.replaceDataSources(newsources);
                } else if (mode==DataTrackConfiguration_AddFromConfigFileDialog.DUPLICATE_SOURCES_REPLACE_ALL_SOURCES) {
                    oldTrack.replaceAllDataSources(newsources);
                }
            }
        }
        // now do the servers. These will just replace current settings!
        for (String servername:servers.keySet()) {
            Server server=servers.get(servername);
            boolean replaced=availableServers.containsKey(servername);
            availableServers.put(servername, server);
            builder.append("<tr><td>");
            builder.append(servername);
            builder.append("</td><td><font color=\""+((replaced)?"#FF0000":"#00BB00")+"\">&nbsp;&nbsp;&nbsp;SERVER&nbsp;&nbsp;&nbsp;</font></td><td>");
            builder.append("</td></tr>");
        }
        builder.append("</table>");
        return builder.toString();
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton OKbutton;
    private javax.swing.JButton browseButton;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField filenameTextfield;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JComboBox modeCombobox;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
