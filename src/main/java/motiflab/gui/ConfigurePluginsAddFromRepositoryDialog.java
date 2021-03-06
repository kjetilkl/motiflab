/*
 * ConfigurePluginsAddFromRepositoryDialog.java
 *
 */

package motiflab.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import motiflab.engine.Plugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import motiflab.engine.SystemError;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * @author kjetikl
 */
public class ConfigurePluginsAddFromRepositoryDialog extends javax.swing.JDialog {
    private JTable pluginsTable;
    private DefaultTableModel tablemodel;
    private boolean installButtonClicked=false;
    private HashMap<String,String> descriptions=new HashMap<String,String>();
    private HashMap<String,String> URLs=new HashMap<String,String>();
    private HashMap<String,HashMap<String,Object>> metadata=new HashMap<String,HashMap<String,Object>>();
    private URL repositoryURL=null;
    private MotifLabGUI gui;

    /** Creates new form ExternalPluginAddFromRepositoryDialog */
    public ConfigurePluginsAddFromRepositoryDialog(MotifLabGUI pegui) {
        super(pegui.getFrame(), "MotifLab Plugins Repository", true);
        this.gui=pegui;
        initComponents();
        try {
            repositoryURL=new URL(gui.getEngine().getPluginsRepositoryURL()+"plugins_repository.xml");
        } catch (Exception e) {}
        tablemodel=new DefaultTableModel(new String[]{"Name","Version","Type","Description"},0);
        pluginsTable=new JTable(tablemodel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        pluginsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pluginsTable.setRowSelectionAllowed(true);
        pluginsTable.setColumnSelectionAllowed(false);
        //pluginsTable.setFillsViewportHeight(true);
        pluginsTable.getTableHeader().setReorderingAllowed(false);
        //pluginsTable.setRowHeight(18);
        pluginsTable.getColumn("Version").setPreferredWidth(60);
        pluginsTable.getColumn("Version").setMaxWidth(60);
        pluginsTable.getColumn("Type").setPreferredWidth(120);        
        pluginsTable.setAutoCreateRowSorter(true);
        pluginsTable.getRowSorter().toggleSortOrder(0);
        pluginsTable.getColumn("Name").setCellRenderer(new RepositoryRenderer());
        scrollPane.setViewportView(pluginsTable);
        pluginsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean rowSelected=(pluginsTable.getSelectedRow()>=0);
                installButton.setEnabled(rowSelected);
                infoButton.setEnabled(rowSelected);
            }
        });
        pluginsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getClickCount()<2) return;
               installButton.doClick();
            }
        });
        Dimension d=new Dimension(650,500);
        this.setMinimumSize(d);
        this.setPreferredSize(d);
        SwingWorker<Boolean, Void> worker=new SwingWorker<Boolean, Void>() {
            Exception ex=null;
            @Override
            public Boolean doInBackground() {
                try {
                   populateList();
                }
                catch (Exception e) {ex=e;}
                return Boolean.TRUE;
            }
            @Override
            public void done() { // this method is invoked on the EDT!
                progressbar.setVisible(false);
                if (ex!=null) {
                    JOptionPane.showMessageDialog(gui.getFrame(), ex.getClass().getSimpleName()+":\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }; // end of SwingWorker class
        progressbar.setVisible(true);
        progressbar.setIndeterminate(true);
        worker.execute();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        jPanel2 = new javax.swing.JPanel();
        infoButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        installButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 20));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 444, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_START);

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        jPanel3.add(scrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel5.setName("jPanel5"); // NOI18N

        progressbar.setMaximumSize(new java.awt.Dimension(32767, 21));
        progressbar.setMinimumSize(new java.awt.Dimension(10, 21));
        progressbar.setName("progressbar"); // NOI18N
        progressbar.setPreferredSize(new java.awt.Dimension(150, 21));
        jPanel5.add(progressbar);

        jPanel4.add(jPanel5, java.awt.BorderLayout.WEST);

        jPanel2.setMaximumSize(new java.awt.Dimension(32767, 40));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ConfigurePluginsAddFromRepositoryDialog.class);
        infoButton.setIcon(resourceMap.getIcon("infoButton.icon")); // NOI18N
        infoButton.setText(resourceMap.getString("infoButton.text")); // NOI18N
        infoButton.setEnabled(false);
        infoButton.setName("infoButton"); // NOI18N
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoButtonPressed(evt);
            }
        });
        jPanel2.add(infoButton);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel2.add(jLabel1);

        installButton.setText(resourceMap.getString("installButton.text")); // NOI18N
        installButton.setEnabled(false);
        installButton.setMaximumSize(null);
        installButton.setMinimumSize(null);
        installButton.setName("installButton"); // NOI18N
        installButton.setPreferredSize(null);
        installButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installButtonClicked(evt);
            }
        });
        jPanel2.add(installButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setMaximumSize(null);
        cancelButton.setMinimumSize(null);
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setPreferredSize(null);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonClicked(evt);
            }
        });
        jPanel2.add(cancelButton);

        jPanel4.add(jPanel2, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel4, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonClicked
        setVisible(false);
    }//GEN-LAST:event_cancelButtonClicked

    private void installButtonClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_installButtonClicked
        installButtonClicked=true;
        setVisible(false);
    }//GEN-LAST:event_installButtonClicked

    private void infoButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoButtonPressed
        int row=pluginsTable.getSelectedRow();
        if (row<0) return;
        int modelrow=pluginsTable.convertRowIndexToModel(row);
        String pluginName=(String)tablemodel.getValueAt(modelrow, 0);
        HashMap<String,Object> meta=metadata.get(pluginName);
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        String document=getPluginMetaDataAsHTML(meta);
        InfoDialog dialog=new InfoDialog(gui, "Plugin: "+pluginName, document);
        gui.getFrame().setCursor(Cursor.getDefaultCursor());
        dialog.setVisible(true);
        dialog.dispose(); 
    }//GEN-LAST:event_infoButtonPressed

    private String getPluginMetaDataAsHTML(HashMap<String,Object> meta) {
        StringBuilder builder=new StringBuilder();
        builder.append("<html><header></header><body>");
        ArrayList<String> keys=new ArrayList<String>(meta.size());
        keys.addAll(meta.keySet());
        Collections.sort(keys);
        builder.append("<h1>"+meta.get("name")+"</h1>");
        builder.append("<table>");
        for (String key:keys) {
           if (key.equals("_plugin")) continue;
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
    
    
    
    /** Returns the URL pointing to the ZIP-file for the selected plugin
     * (or NULL if no plugin was selected)
     */
    public String getURL() {
        if (!installButtonClicked) return null;
        int row=pluginsTable.getSelectedRow();
        if (row<0) return null;
        int modelrow=pluginsTable.convertRowIndexToModel(row);
        String pluginName=(String)tablemodel.getValueAt(modelrow, 0);
        return URLs.get(pluginName);

    }
    /** Returns name of the selected plugin
     * (or NULL if no plugin was selected)
     */
    public String getPluginName() {
        if (!installButtonClicked) return null;
        int row=pluginsTable.getSelectedRow();
        if (row<0) return null;
        int modelrow=pluginsTable.convertRowIndexToModel(row);
        String pluginName=(String)tablemodel.getValueAt(modelrow, 0);
        return pluginName;

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton infoButton;
    private javax.swing.JButton installButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    /** Populates the tablemodel with entries from the repositoryURL
     *  Note that since this method is run OFF the EDT all updates to any table
     *  models must be invoked on the EDT-queue!
     */
    private void populateList() throws Exception {
         URLs.clear();
         descriptions.clear();
         if (repositoryURL==null) throw new SystemError("No address for plugins repository");
         InputStream repositoryStream=repositoryURL.openStream();
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document doc = builder.parse(repositoryStream);
         NodeList pluginnodes = doc.getElementsByTagName("plugin");
         for (int i=0;i<pluginnodes.getLength();i++) {
             Element pluginNode = (Element)pluginnodes.item(i);
            
             final String pluginName=pluginNode.getAttribute("name");
             if (pluginName==null || pluginName.isEmpty()) throw new SystemError("Repository Error: Missing name for plugin #"+(i+1));

             String pluginURL=pluginNode.getAttribute("URL");
             if (pluginURL==null || pluginURL.isEmpty()) throw new SystemError("Repository Error: Missing URL specification for plugin '"+pluginName+"'");
             if (!pluginURL.startsWith("http")) pluginURL=gui.getEngine().getPluginsRepositoryURL()+pluginURL; // URL is relative to repository
             final String pluginVersion=pluginNode.getAttribute("version");
             final String pluginDescription=pluginNode.getAttribute("description");
             final String pluginType=pluginNode.getAttribute("type");             
             URLs.put(pluginName, pluginURL);
             descriptions.put(pluginName, pluginDescription);
             HashMap<String,Object> meta=new HashMap<String,Object>();
             NamedNodeMap attributes=pluginNode.getAttributes();
             for (int j=0;j<attributes.getLength();j++) {
                 Node node=attributes.item(j);
                 meta.put(node.getNodeName(),node.getNodeValue());
             }
             meta.put("URL", pluginURL); // use full URL
             metadata.put(pluginName, meta);
             Runnable runner=new Runnable() {
                @Override
                public void run() {
                    pluginsTable.repaint();
                    tablemodel.addRow(new Object[]{pluginName,pluginVersion,pluginType,pluginDescription});
                }
             };
             SwingUtilities.invokeLater(runner);
         }
    }

    private class RepositoryRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            super.setValue(value);
            String description=descriptions.get((String)value).trim();
            if (description.isEmpty()) this.setToolTipText(null);
            else this.setToolTipText(description);
        }
        
        
    }


}
