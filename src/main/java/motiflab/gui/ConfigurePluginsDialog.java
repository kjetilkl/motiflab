/*
 * ConfigurePluginsDialog.java
 *
 * Created on Dec 2, 2013, 1:31:20 PM
 */
package motiflab.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import motiflab.engine.ConfigurablePlugin;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Plugin;
import motiflab.engine.SystemError;
import motiflab.engine.datasource.DataRepositoryFile;

/**
 *
 * @author kjetikl
 */
public class ConfigurePluginsDialog extends javax.swing.JDialog {
    private static final String TABLECOLUMN_NAME="Name";
    private static final String TABLECOLUMN_TYPE="Type";
    private static final String TABLECOLUMN_VERSION="Version";    
    private static final String TABLECOLUMN_DESCRIPTION="Description";
    
    private static final int COLUMN_NAME=0;
    private static final int COLUMN_TYPE=1;
    private static final int COLUMN_VERSION=2;    
    private static final int COLUMN_DESCRIPTION=3;
    
    
    private MotifLabGUI gui;
    private DefaultTableModel pluginsModel;
    private JTable pluginsTable;
    private HashSet<String> isConfigurable;

    
    
    /** Creates new form ConfigurePluginsDialog */
    public ConfigurePluginsDialog(final MotifLabGUI gui) {
        super(gui.getFrame(),"Plugins", true);
        this.gui=gui;
        initComponents();
        progressBar.setVisible(false);
        getRootPane().setDefaultButton(closeButton);
        pluginsModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_VERSION,TABLECOLUMN_TYPE,TABLECOLUMN_DESCRIPTION},0);
        pluginsTable=new JTable(pluginsModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        isConfigurable=new HashSet<String>();
        for (Plugin plugin:gui.getEngine().getPlugins()) {
            String name=plugin.getPluginName();
            HashMap<String,Object> metadata=gui.getEngine().getPluginMetaData(name);
            String type=metadata.containsKey("type")?metadata.get("type").toString():"";
            String version=metadata.containsKey("version")?metadata.get("version").toString():"";
            String description=metadata.containsKey("description")?metadata.get("description").toString():"";            
            Object[] values=new Object[]{name,version,type,description}; // the null is just a placeholder
            pluginsModel.addRow(values);
            if (plugin instanceof ConfigurablePlugin) isConfigurable.add(name);
        }       
        pluginsTable.getColumn(TABLECOLUMN_VERSION).setPreferredWidth(60);
        pluginsTable.getColumn(TABLECOLUMN_VERSION).setMaxWidth(60);        
        pluginsTable.setFillsViewportHeight(true);
        pluginsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pluginsTable.setRowSelectionAllowed(true);
        pluginsTable.getTableHeader().setReorderingAllowed(false);             
        pluginsTable.setRowHeight(18);
        scrollPane.setViewportView(pluginsTable);  
                
        pluginsTable.setAutoCreateRowSorter(true);
        pluginsTable.getRowSorter().toggleSortOrder(pluginsTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        pluginsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getClickCount()<2) return;
               int row=pluginsTable.getSelectedRow();
               String pluginname=(String)pluginsTable.getValueAt(row, COLUMN_NAME);
               Plugin plugin=gui.getEngine().getPlugin(pluginname);
               showPluginHelp(plugin);
            }             
        });
        pluginsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row=pluginsTable.getSelectedRow();
                if (row>=0) {
                    infoButton.setEnabled(true);
                    helpButton.setEnabled(true);
                    removeButton.setEnabled(true);
                    String pluginName=(String)pluginsTable.getValueAt(row, COLUMN_NAME);
                    configureButton.setEnabled(isConfigurable.contains(pluginName));
                } else {
                    infoButton.setEnabled(true);
                    helpButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    configureButton.setEnabled(false);
                }
            }
        });                              
        this.setPreferredSize(new Dimension(700,400));
        pack();        
    }

    
    /** Shows the metadata for a plugin */
    private void showPluginInfo(Plugin plugin) {
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        HashMap<String,Object> metadata=gui.getEngine().getPluginMetaData(plugin.getPluginName());
        InfoDialog dialog=new InfoDialog(gui, "Plugin: "+plugin.getPluginName(), getPluginMetaDataAsHTML(metadata));
        gui.getFrame().setCursor(Cursor.getDefaultCursor());
        dialog.setVisible(true);
        dialog.dispose();          
    }
    
    /** Shows the documentation for a plugin */
    private void showPluginHelp(Plugin plugin) {
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        HashMap<String,Object> metadata=gui.getEngine().getPluginMetaData(plugin.getPluginName());
        String documentation=metadata.containsKey("documentation")?metadata.get("documentation").toString():"";
        InfoDialog dialog=null;   
        if (!documentation.isEmpty()) {
            try {
                URL url=new URL(documentation);
                dialog=new InfoDialog(gui, "Plugin: "+plugin.getPluginName(), url, 750,600);
            } catch (MalformedURLException mfu) {
                System.err.println("Malformed Plugin documentation URL: "+documentation);
            } catch (Exception er) {
                System.err.println(er.toString());
            }            
        }
        if (dialog==null) dialog=new InfoDialog(gui, "Plugin: "+plugin.getPluginName(), "No documentation...");
        gui.getFrame().setCursor(Cursor.getDefaultCursor());
        dialog.setVisible(true);
        dialog.dispose();          
    }    
    
    private String getPluginMetaDataAsHTML(HashMap<String,Object> meta) {
        StringBuilder builder=new StringBuilder();
        builder.append("<html><header></header><body>");
        ArrayList<String> keys=new ArrayList<String>(meta.size());
        keys.addAll(meta.keySet());
        Collections.sort(keys);
        builder.append("<h1>"+meta.get("name")+"</h1>");
        builder.append("<table>");
        for (String key:keys) {
           if (key.startsWith("_")) continue; // properties starting with underscore, such as "_plugin" are considered "internal" and should be hidden from view
           builder.append("<tr style=\"border: 1px solid black; border-collapse:collapse;\"><td style=\"border: 1px solid black;border-collapse:collapse;background:#E0E0E0;\"><b>");
           builder.append(key);
           builder.append("</b></td><td style=\"border: 1px solid black;border-collapse:collapse;background:#F0F0F0;\">");
           Object value=meta.get(key);
           builder.append((value==null)?"":value.toString());
           builder.append("</tr>");
        }
        builder.append("</table>");
        builder.append("</body></html>");
        return builder.toString();
    }

    private boolean removePluginInListAndEngine(Plugin plugin) throws SystemError{
        boolean ok=gui.getEngine().uninstallPlugin(plugin);
        String pluginName=plugin.getPluginName();
        for (int i=0;i<pluginsModel.getRowCount();i++) {
           if (pluginsModel.getValueAt(i,COLUMN_NAME).equals(pluginName)) {
               pluginsModel.removeRow(i);
               isConfigurable.remove(pluginName);
               gui.pluginInstallationUpdated();
               break;
           }
        }
        return ok;
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        headerPanel = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        buttonsPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        closeButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        addFromRepository = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        configureButton = new javax.swing.JButton();
        infoButton = new javax.swing.JButton();
        helpButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        headerPanel.setName("headerPanel"); // NOI18N
        headerPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        getContentPane().add(headerPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 6, 1, 6));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        jPanel2.add(scrollPane, java.awt.BorderLayout.CENTER);

        mainPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 30, 0, 0));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        progressBar.setMinimumSize(new java.awt.Dimension(30, 20));
        progressBar.setName("progressBar"); // NOI18N
        progressBar.setPreferredSize(new java.awt.Dimension(30, 20));
        jPanel1.add(progressBar);

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ConfigurePluginsDialog.class);
        closeButton.setText(resourceMap.getString("closeButton.text")); // NOI18N
        closeButton.setName("closeButton"); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonPressed(evt);
            }
        });
        jPanel1.add(closeButton);

        buttonsPanel.add(jPanel1, java.awt.BorderLayout.EAST);

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addButton.setText(resourceMap.getString("addButton.text")); // NOI18N
        addButton.setName("addButton"); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonPressed(evt);
            }
        });
        jPanel3.add(addButton);

        addFromRepository.setText(resourceMap.getString("addFromRepository.text")); // NOI18N
        addFromRepository.setName("addFromRepository"); // NOI18N
        addFromRepository.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPluginFromRepository(evt);
            }
        });
        jPanel3.add(addFromRepository);

        removeButton.setText(resourceMap.getString("removeButton.text")); // NOI18N
        removeButton.setEnabled(false);
        removeButton.setName("removeButton"); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonPressed(evt);
            }
        });
        jPanel3.add(removeButton);

        configureButton.setText(resourceMap.getString("configureButton.text")); // NOI18N
        configureButton.setEnabled(false);
        configureButton.setName("configureButton"); // NOI18N
        configureButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configureButtonPressed(evt);
            }
        });
        jPanel3.add(configureButton);

        infoButton.setIcon(resourceMap.getIcon("infoButton.icon")); // NOI18N
        infoButton.setText(resourceMap.getString("infoButton.text")); // NOI18N
        infoButton.setEnabled(false);
        infoButton.setName("infoButton"); // NOI18N
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoButtonPressed(evt);
            }
        });
        jPanel3.add(infoButton);

        helpButton.setText(resourceMap.getString("helpButton.text")); // NOI18N
        helpButton.setEnabled(false);
        helpButton.setName("helpButton"); // NOI18N
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButtonPressed(evt);
            }
        });
        jPanel3.add(helpButton);

        buttonsPanel.add(jPanel3, java.awt.BorderLayout.WEST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonPressed
        setVisible(false);
    }//GEN-LAST:event_closeButtonPressed

    private void addButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonPressed
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        final JFileChooser fc = gui.getFileChooser(null);// new JFileChooser(gui.getLastUsedDirectory());
        fc.setDialogTitle("Install plugin from file");
        FileNameExtensionFilter mlpFilter=new FileNameExtensionFilter("MotifLab Plugin (*.mlp)", "MLP","mlp");
        fc.addChoosableFileFilter(mlpFilter);
        FileNameExtensionFilter zipFilter=new FileNameExtensionFilter("ZIP-files (*.zip)", "ZIP","zip");
        fc.addChoosableFileFilter(zipFilter);        
        fc.setFileFilter(mlpFilter);
        int returnValue=fc.showOpenDialog(this);
        setCursor(Cursor.getDefaultCursor());
        if (returnValue!=JFileChooser.APPROVE_OPTION) return; // user cancelled
        File file=fc.getSelectedFile();
        gui.setLastUsedDirectory(file.getParentFile());
        addPluginInBackground(file);
    }//GEN-LAST:event_addButtonPressed

    private void removeButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonPressed
       int row=pluginsTable.getSelectedRow();
       if (row<0) return;
       String pluginName=(String)pluginsTable.getValueAt(row, COLUMN_NAME);
       Plugin plugin=gui.getEngine().getPlugin(pluginName);
       int option=JOptionPane.showConfirmDialog(rootPane, "Are you sure you want to remove the plug-in \""+pluginName+"\" ?", "Remove plug-in", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
       if (option==JOptionPane.YES_OPTION && plugin!=null) {
            try {
                boolean ok=removePluginInListAndEngine(plugin);
                if (ok) JOptionPane.showMessageDialog(this, "The plugin has been uninstalled", "Uninstall plugin", JOptionPane.INFORMATION_MESSAGE);              
                else JOptionPane.showMessageDialog(this, "The plugin has been uninstalled, but some resources could not be reclaimed.\nIn order to complete the process you should restart MotifLab", "Uninstall plugin", JOptionPane.WARNING_MESSAGE);                              
            } catch (SystemError e) {
                 JOptionPane.showMessageDialog(this, e.getMessage(), "Remove Plugin", JOptionPane.ERROR_MESSAGE);
            }
        }    
    }//GEN-LAST:event_removeButtonPressed

    private void configureButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureButtonPressed
       int row=pluginsTable.getSelectedRow();
       if (row<0) return;
       String pluginName=(String)pluginsTable.getValueAt(row, COLUMN_NAME);
       Plugin plugin=gui.getEngine().getPlugin(pluginName);
       if (plugin==null || !(plugin instanceof ConfigurablePlugin)) return;
       JDialog dialog=((ConfigurablePlugin)plugin).getPluginConfigurationDialog(this);
       if (dialog!=null) {
            int height=dialog.getHeight();
            int width=dialog.getWidth();
            java.awt.Dimension size=gui.getFrame().getSize();
            dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
            dialog.setVisible(true);
            dialog.dispose();           
       } else { // Create configuration panel from exported Parameters
           Parameter[] parameters=((ConfigurablePlugin)plugin).getPluginParameters();
           ParameterSettings settings=((ConfigurablePlugin)plugin).getPluginParameterSettings();
           ParametersDialog parametersDialog=new ParametersDialog(gui, "Configure "+pluginName, parameters, settings);
           int height=parametersDialog.getHeight();
           int width=parametersDialog.getWidth();
           java.awt.Dimension size=gui.getFrame().getSize();
           parametersDialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
           parametersDialog.setVisible(true);
           settings=parametersDialog.getParameterSettings();
           parametersDialog.dispose();  
           try {
            ((ConfigurablePlugin)plugin).setPluginParameterSettings(settings);
           } catch (ExecutionError e) {
              JOptionPane.showMessageDialog(this, e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE); 
           }      
       }
    }//GEN-LAST:event_configureButtonPressed

    private void infoButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoButtonPressed
       int row=pluginsTable.getSelectedRow();
       if (row<0) return;
       String pluginName=(String)pluginsTable.getValueAt(row, COLUMN_NAME);
       Plugin plugin=gui.getEngine().getPlugin(pluginName);
       showPluginInfo(plugin); 
    }//GEN-LAST:event_infoButtonPressed

    private void helpButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButtonPressed
       int row=pluginsTable.getSelectedRow();
       if (row<0) return;
       String pluginName=(String)pluginsTable.getValueAt(row, COLUMN_NAME);
       Plugin plugin=gui.getEngine().getPlugin(pluginName);
       showPluginHelp(plugin); 
    }//GEN-LAST:event_helpButtonPressed

    private void addPluginFromRepository(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPluginFromRepository
        ConfigurePluginsAddFromRepositoryDialog repositoryDialog=new ConfigurePluginsAddFromRepositoryDialog(gui);
        repositoryDialog.setLocation(gui.getFrame().getWidth()/2-repositoryDialog.getWidth()/2, gui.getFrame().getHeight()/2-repositoryDialog.getHeight()/2);
        repositoryDialog.setVisible(true); // this is modal
        String pluginName=repositoryDialog.getPluginName();
        String urlString=repositoryDialog.getURL();
        repositoryDialog.dispose();
        if (urlString==null) return;
        addPluginInBackground(urlString); 
    }//GEN-LAST:event_addPluginFromRepository

    private void addPluginInBackground(final Object source) {
        SwingWorker worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                try {
                    File localZipFile=null;
                    if (source instanceof URL || source instanceof String) {
                        URL url=(source instanceof URL)?(URL)source:new URL((String)source);
                        localZipFile=gui.getEngine().createTempFile();
                        org.apache.commons.io.FileUtils.copyURLToFile(url, localZipFile);
                    } else if (source instanceof DataRepositoryFile) {
                        localZipFile=gui.getEngine().createTempFile();
                        InputStream stream=((DataRepositoryFile)source).getFileAsInputStream();
                        Files.copy(stream, localZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);                      
                    } else if (source instanceof File) {
                        localZipFile=(File)source;
                    } else throw new ExecutionError("Unrecognized source: "+source);
                    addPlugin(localZipFile);
                } catch (Exception e) {
                    ex=e;
                    return Boolean.FALSE;
                } finally {
              
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                progressBar.setVisible(false);
                closeButton.setEnabled(true);
                if (ex!=null) {
                    JOptionPane.showMessageDialog(ConfigurePluginsDialog.this, ex.getMessage(), "Plugin Error", JOptionPane.ERROR_MESSAGE); 
                } else { // install went OK
                    JOptionPane.showMessageDialog(ConfigurePluginsDialog.this, "The plugin was installed successfully", "Install Plugin", JOptionPane.INFORMATION_MESSAGE); 
                }
            }
        }; // end of SwingWorker class
        closeButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);      
        worker.execute();                 
    }
     
    
    /** Installs the plugin from the given zipFile into the plugin directory with the given name (just a relative name)
     *  The zipFile should be a regular file (not a DataRepositoryFile)
     */
    private void addPlugin(File zipFile) throws ExecutionError {
        MotifLabEngine engine=gui.getEngine();
        HashMap<String,Object> metadata=null;    
        String pluginName=null;
        try {
            metadata=engine.readPluginMetaDataFromZIP(zipFile);
            pluginName=metadata.get("name").toString();
        } catch (SystemError e) {
            throw new ExecutionError(e.getMessage());
        }
        if (metadata.containsKey("motiflab_version")) {
            String requiredVersion=metadata.get("motiflab_version").toString();          
            if (MotifLabEngine.compareVersions(requiredVersion)<0) throw new ExecutionError("This plugin requires version "+requiredVersion+" or higher of MotifLab");
        }
        Plugin plugin=engine.getPlugin(pluginName); // check if there already exists a plugin with the same name
        if (plugin!=null) { // 
            int option=JOptionPane.showConfirmDialog(rootPane, "A plugin with the same name is already registered (\""+pluginName+"\").\nDo you want to replace the current version?", "Replace plug-in", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (option==JOptionPane.YES_OPTION) {
                try {
                    boolean ok=removePluginInListAndEngine(plugin);
                    if (!ok) throw new ExecutionError("The previous plugin was uninstalled but some resources could not be reclaimed. In order to complete the process you should restart MotifLab and then install the new version again");                             
                } catch (SystemError e) {
                     throw new ExecutionError(e.getMessage(),e);
                }
            } else throw new ExecutionError("Plugin was not replaced");       
        }      
        String dirName=pluginName.replaceAll("\\W", "_");
        File pluginDir=new File(engine.getPluginsDirectory(), dirName);
        boolean ok=true;
        if (!pluginDir.exists()) {
            ok=pluginDir.mkdir();
            if (!ok) throw new ExecutionError("Unable to create directory for plugin installation");
        }   
        try {
            engine.unzipFile(zipFile, pluginDir);
        } catch (IOException io) {
            throw new ExecutionError("Unable to extract plugin ZIP-file");
        }
        plugin=null;
        try {       
            metadata=engine.readPluginMetaDataFromDirectory(pluginDir);
            plugin=engine.instantiatePluginFromDirectory(pluginDir);
        } catch (SystemError se) {
            plugin=null;
            removePluginDir(pluginDir);
            throw new ExecutionError(se.getMessage());
        }        
        // now initialize the plugin and register it
        try {
            plugin.initializePlugin(engine);            
        } catch (SystemError se) { // A critical initialization error has occurred. The plugin should not be registered with the engine, so we will delete the plugin files
            plugin=null;
            removePluginDir(pluginDir);
            throw new ExecutionError(se.getMessage());
        } catch (ExecutionError e) {
            gui.logMessage("Plugin error for \""+pluginName+"\" => "+e.getMessage()); // this is by definition a non-critical error
        }      
        engine.registerPlugin(plugin, metadata);
        try {
            plugin.initializePluginFromClient(gui);
        } catch (Exception e) {
            gui.logMessage("Plugin error for \""+pluginName+"\" when connecting with client => "+e.getMessage());
            e.printStackTrace(System.err);
        }
        installPluginInList(plugin, metadata);
        if (plugin instanceof ConfigurablePlugin) isConfigurable.add(pluginName);
    }
    
    private void removePluginDir(File pluginDir) {
        // first we must discard the classloader which may have placed locks on JAR-files
        ClassLoader classLoader=gui.getEngine().getPluginClassLoader(pluginDir);
        if (classLoader instanceof URLClassLoader) {
            try {
                ((URLClassLoader)classLoader).close(); // necessary to release the file lock on the JAR-files
                classLoader=null;
            } catch (IOException io) {} 
        }     
        System.gc(); // this is necessary in order to garbage collect the ClassLoader (which is necessary in order to delete the JAR-file). However, it is not guaranteed to work...     
        gui.getEngine().deleteTempFile(pluginDir); // this might fail...
        MotifLabEngine.deleteOnExit(pluginDir); // backup       
    }


    private void installPluginInList(Plugin plugin, HashMap<String,Object> meta) {
        String type=getPropertyAsString(meta,"type");
        String version=getPropertyAsString(meta,"version");
        String description=getPropertyAsString(meta,"description");      
        Object[] values=new Object[]{plugin.getPluginName(),version,type,description};
        pluginsModel.addRow(values);  
        gui.pluginInstallationUpdated();  // updates the GUI also      
    }      
    
    private String getPropertyAsString(HashMap<String,Object> meta, String property) {
        Object value=meta.get(property);
        return (value==null)?"":value.toString();
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton addFromRepository;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton configureButton;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JButton helpButton;
    private javax.swing.JButton infoButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton removeButton;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
