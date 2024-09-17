/*
 * ConfigureOrganismsAndIdentifiersDialog.java
 *
 * Created on May 11, 2012, 4:33:41 PM
 */
package motiflab.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import motiflab.engine.GeneIDResolver;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Organism;
import motiflab.engine.protocol.ParseError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 *
 * @author kjetikl
 */
public class ConfigureOrganismsAndIdentifiersDialog extends javax.swing.JDialog {
 
    private MotifLabGUI gui;
    
    /** Creates new form ConfigureOrganismsAndIdentifiersDialog */
    public ConfigureOrganismsAndIdentifiersDialog(MotifLabGUI gui) {
        super(gui.getFrame(), true);
        this.gui=gui;       
        initComponents();
        organismsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        geneIDsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        otherIDsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        biomartTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        organismsTable.getTableHeader().setReorderingAllowed(false);
        geneIDsTable.getTableHeader().setReorderingAllowed(false);
        otherIDsTable.getTableHeader().setReorderingAllowed(false);
        biomartTable.getTableHeader().setReorderingAllowed(false);
        this.setPreferredSize(new Dimension(800,400));        
        pack();
    }

    private DefaultTableModel getOrganismsTableModel() {
        String[] columnNames=new String[]{"Taxonomy ID","Common Name","Latin Name","Clade","BioMart Dataset","Genome Builds"};
        Integer[] organisms=Organism.getSupportedOrganismIDs();
        Object[][] data=new Object[organisms.length][5];
        GeneIDResolver resolver=gui.getEngine().getGeneIDResolver();        
        for (int i=0;i<organisms.length;i++) {
            int taxonomyID=organisms[i]; 
            String commonName=Organism.getCommonName(taxonomyID);
            String latinName=Organism.getLatinName(taxonomyID);
            String clade=Organism.getCladeForOrganism(taxonomyID);
            String biomartDB=resolver.getBiomartDBnameForOrganisms(taxonomyID);
            String buildStrings=Organism.getSupportedGenomeBuildsAsString(taxonomyID);            
            data[i]=new Object[]{organisms[i],(commonName!=null)?commonName:"",(latinName!=null)?latinName:"",(clade!=null)?clade:"",biomartDB,buildStrings};
        }
        gui.getEngine().getGeneIDResolver();
        DefaultTableModel model=new DefaultTableModel(data, columnNames);
        return model;
    }
    
    private DefaultTableModel getGeneIdentifiersTableModel() {
        String[] columnNames=new String[]{"Database","ID Presentation Name","BioMart ID","Web Link"};        
        GeneIDResolver resolver=gui.getEngine().getGeneIDResolver();
        String[] ids=resolver.getSupportedIDFormatsInternalNames();
        Object[][] data=new Object[ids.length][4];
        for (int i=0;i<ids.length;i++) {
            String id=ids[i];
            String dbname=resolver.getDatabaseName(id);
            String presentationName=resolver.getPresentationName(id);
            String biomartID=resolver.getBiomartID(id);
            String weblink=resolver.getWebLinkTemplate(id);   
            if (dbname==null) dbname="";
            if (presentationName==null) presentationName="";
            if (biomartID==null) biomartID="";
            if (weblink==null) weblink="";
            data[i]=new Object[]{dbname,presentationName,biomartID,weblink};
        }       
        DefaultTableModel model=new DefaultTableModel(data, columnNames);        
        return model;
    }
    
    private DefaultTableModel getOtherIdentifiersTableModel() {
        String[] columnNames=new String[]{"ID Name","Web Link"};        
        GeneIDResolver resolver=gui.getEngine().getGeneIDResolver();
        String[] ids=resolver.getOtherIDs();
        Object[][] data=new Object[ids.length][4];
        for (int i=0;i<ids.length;i++) {
            String id=ids[i];
            String weblink=resolver.getWebLinkTemplate(id.toLowerCase());   
            if (id==null) id="";
            if (weblink==null) weblink="";
            data[i]=new Object[]{id,weblink};
        }       
        DefaultTableModel model=new DefaultTableModel(data, columnNames);        
        return model;
    }    
    
    private DefaultTableModel getBioMartTableModel() {
        String[] columnNames=new String[]{"Build","URL","VirtualSchemaName","ConfigVersion","Attributes"};
        final GeneIDResolver resolver=gui.getEngine().getGeneIDResolver();        
        String[][] data=resolver.getURLsForBuilds(5);
        final DefaultTableModel model=new DefaultTableModel(data, columnNames);  
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                for (int i=0;i<model.getRowCount();i++) {
                    String attributes=(model.getValueAt(i, 4) instanceof String)?(String)model.getValueAt(i, 4):"";
                    if (attributes.equals("new") || attributes.equals("old")) {
                        String[] list=resolver.getBioMartAttributes(attributes.equals("new"));
                        String attributesString=MotifLabEngine.splice(list, ",");
                        model.setValueAt(attributesString, i, 4);
                        return;
                    }
                }
            }
        });
        return model;
    }    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        toppanel = new javax.swing.JPanel();
        internalPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        organismsPanel = new javax.swing.JPanel();
        organismsPanelTop = new javax.swing.JPanel();
        organismsPanelScrollPane = new javax.swing.JScrollPane();
        organismsTable = new javax.swing.JTable();
        organismsPanelBottom = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        AddOrganismButton = new javax.swing.JButton();
        removeOrganismButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        geneIDformatsPanel = new javax.swing.JPanel();
        geneIDformatsPanelTop = new javax.swing.JPanel();
        geneIDformatsPanelScrollPane = new javax.swing.JScrollPane();
        geneIDsTable = new javax.swing.JTable();
        geneIDformatsPanelBottom = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        addGeneIDButton = new javax.swing.JButton();
        removeGeneIDButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        otherIDsPanel = new javax.swing.JPanel();
        otherIDsPanelTop = new javax.swing.JPanel();
        otherIDsPanelScrollPane = new javax.swing.JScrollPane();
        otherIDsTable = new javax.swing.JTable();
        otherIDsPanelBottom = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        addOtherIDButton = new javax.swing.JButton();
        removeOtherIDButton1 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        biomartPanel = new javax.swing.JPanel();
        biomartPanelTop = new javax.swing.JPanel();
        biomartPanelScrollPane = new javax.swing.JScrollPane();
        biomartTable = new javax.swing.JTable();
        biomartPaneBottom = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        addBiomartButton = new javax.swing.JButton();
        removeBiomartButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        bottomControlsPanel = new javax.swing.JPanel();
        ok_cancel_panel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        bottomLeftControlsPanel = new javax.swing.JPanel();
        addFromFileButton = new javax.swing.JButton();
        saveToFileButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ConfigureOrganismsAndIdentifiersDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(200, 200));
        setName("Form"); // NOI18N

        toppanel.setMaximumSize(new java.awt.Dimension(32767, 30));
        toppanel.setMinimumSize(new java.awt.Dimension(0, 6));
        toppanel.setName("toppanel"); // NOI18N
        toppanel.setPreferredSize(new java.awt.Dimension(400, 6));

        javax.swing.GroupLayout toppanelLayout = new javax.swing.GroupLayout(toppanel);
        toppanel.setLayout(toppanelLayout);
        toppanelLayout.setHorizontalGroup(
            toppanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 616, Short.MAX_VALUE)
        );
        toppanelLayout.setVerticalGroup(
            toppanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 6, Short.MAX_VALUE)
        );

        getContentPane().add(toppanel, java.awt.BorderLayout.PAGE_START);

        internalPanel.setMaximumSize(new java.awt.Dimension(1000, 800));
        internalPanel.setMinimumSize(new java.awt.Dimension(400, 300));
        internalPanel.setName("internalPanel"); // NOI18N
        internalPanel.setPreferredSize(new java.awt.Dimension(600, 400));
        internalPanel.setLayout(new java.awt.BorderLayout());

        tabbedPane.setName("tabbedPane"); // NOI18N

        organismsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5))));
        organismsPanel.setMaximumSize(new java.awt.Dimension(2147483647, 200));
        organismsPanel.setName("organismsPanel"); // NOI18N
        organismsPanel.setPreferredSize(new java.awt.Dimension(384, 200));
        organismsPanel.setLayout(new java.awt.BorderLayout());

        organismsPanelTop.setMaximumSize(new java.awt.Dimension(32767, 8));
        organismsPanelTop.setMinimumSize(new java.awt.Dimension(60, 8));
        organismsPanelTop.setName("organismsPanelTop"); // NOI18N
        organismsPanelTop.setPreferredSize(new java.awt.Dimension(384, 8));
        organismsPanelTop.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        organismsPanel.add(organismsPanelTop, java.awt.BorderLayout.NORTH);

        organismsPanelScrollPane.setMinimumSize(new java.awt.Dimension(400, 30));
        organismsPanelScrollPane.setName("organismsPanelScrollPane"); // NOI18N
        organismsPanelScrollPane.setRequestFocusEnabled(false);

        organismsTable.setAutoCreateRowSorter(true);
        organismsTable.setModel(getOrganismsTableModel());
        organismsTable.setFillsViewportHeight(true);
        organismsTable.setMinimumSize(new java.awt.Dimension(300, 64));
        organismsTable.setName("organismsTable"); // NOI18N
        organismsPanelScrollPane.setViewportView(organismsTable);

        organismsPanel.add(organismsPanelScrollPane, java.awt.BorderLayout.CENTER);

        organismsPanelBottom.setMaximumSize(new java.awt.Dimension(32767, 36));
        organismsPanelBottom.setMinimumSize(new java.awt.Dimension(141, 36));
        organismsPanelBottom.setName("organismsPanelBottom"); // NOI18N
        organismsPanelBottom.setPreferredSize(new java.awt.Dimension(576, 36));
        organismsPanelBottom.setLayout(new java.awt.BorderLayout());

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        AddOrganismButton.setText(resourceMap.getString("AddOrganismButton.text")); // NOI18N
        AddOrganismButton.setName("AddOrganismButton"); // NOI18N
        AddOrganismButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOrganismButtonPressed(evt);
            }
        });
        jPanel1.add(AddOrganismButton);

        removeOrganismButton.setText(resourceMap.getString("removeOrganismButton.text")); // NOI18N
        removeOrganismButton.setName("removeOrganismButton"); // NOI18N
        removeOrganismButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeOrganismButtonPressed(evt);
            }
        });
        jPanel1.add(removeOrganismButton);

        organismsPanelBottom.add(jPanel1, java.awt.BorderLayout.WEST);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabel1.setIcon(resourceMap.getIcon("jLabel1.icon")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setToolTipText(resourceMap.getString("jLabel1.toolTipText")); // NOI18N
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jLabel1.setIconTextGap(10);
        jLabel1.setName("jLabel1"); // NOI18N
        jPanel2.add(jLabel1);

        organismsPanelBottom.add(jPanel2, java.awt.BorderLayout.EAST);

        organismsPanel.add(organismsPanelBottom, java.awt.BorderLayout.SOUTH);

        tabbedPane.addTab(resourceMap.getString("organismsPanel.TabConstraints.tabTitle"), organismsPanel); // NOI18N

        geneIDformatsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5))));
        geneIDformatsPanel.setMaximumSize(new java.awt.Dimension(2147483647, 200));
        geneIDformatsPanel.setName("geneIDformatsPanel"); // NOI18N
        geneIDformatsPanel.setPreferredSize(new java.awt.Dimension(384, 200));
        geneIDformatsPanel.setLayout(new java.awt.BorderLayout());

        geneIDformatsPanelTop.setMaximumSize(new java.awt.Dimension(32767, 8));
        geneIDformatsPanelTop.setMinimumSize(new java.awt.Dimension(10, 8));
        geneIDformatsPanelTop.setName("geneIDformatsPanelTop"); // NOI18N
        geneIDformatsPanelTop.setPreferredSize(new java.awt.Dimension(628, 8));
        geneIDformatsPanelTop.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        geneIDformatsPanel.add(geneIDformatsPanelTop, java.awt.BorderLayout.PAGE_START);

        geneIDformatsPanelScrollPane.setMinimumSize(new java.awt.Dimension(400, 30));
        geneIDformatsPanelScrollPane.setName("geneIDformatsPanelScrollPane"); // NOI18N

        geneIDsTable.setAutoCreateRowSorter(true);
        geneIDsTable.setModel(getGeneIdentifiersTableModel());
        geneIDsTable.setFillsViewportHeight(true);
        geneIDsTable.setMinimumSize(new java.awt.Dimension(300, 64));
        geneIDsTable.setName("geneIDsTable"); // NOI18N
        geneIDformatsPanelScrollPane.setViewportView(geneIDsTable);

        geneIDformatsPanel.add(geneIDformatsPanelScrollPane, java.awt.BorderLayout.CENTER);

        geneIDformatsPanelBottom.setMaximumSize(new java.awt.Dimension(32767, 36));
        geneIDformatsPanelBottom.setName("geneIDformatsPanelBottom"); // NOI18N
        geneIDformatsPanelBottom.setPreferredSize(new java.awt.Dimension(576, 36));
        geneIDformatsPanelBottom.setLayout(new java.awt.BorderLayout());

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addGeneIDButton.setText(resourceMap.getString("addGeneIDButton.text")); // NOI18N
        addGeneIDButton.setName("addGeneIDButton"); // NOI18N
        addGeneIDButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addGeneIDButtonPressed(evt);
            }
        });
        jPanel3.add(addGeneIDButton);

        removeGeneIDButton.setText(resourceMap.getString("removeGeneIDButton.text")); // NOI18N
        removeGeneIDButton.setName("removeGeneIDButton"); // NOI18N
        removeGeneIDButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeGeneIDButtonPressed(evt);
            }
        });
        jPanel3.add(removeGeneIDButton);

        geneIDformatsPanelBottom.add(jPanel3, java.awt.BorderLayout.WEST);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabel2.setIcon(resourceMap.getIcon("jLabel2.icon")); // NOI18N
        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setToolTipText(resourceMap.getString("jLabel2.toolTipText")); // NOI18N
        jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jLabel2.setIconTextGap(10);
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel4.add(jLabel2);

        geneIDformatsPanelBottom.add(jPanel4, java.awt.BorderLayout.EAST);

        geneIDformatsPanel.add(geneIDformatsPanelBottom, java.awt.BorderLayout.PAGE_END);

        tabbedPane.addTab(resourceMap.getString("geneIDformatsPanel.TabConstraints.tabTitle"), geneIDformatsPanel); // NOI18N

        otherIDsPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5))));
        otherIDsPanel.setMaximumSize(new java.awt.Dimension(2147483647, 200));
        otherIDsPanel.setName("otherIDsPanel"); // NOI18N
        otherIDsPanel.setPreferredSize(new java.awt.Dimension(384, 200));
        otherIDsPanel.setLayout(new java.awt.BorderLayout());

        otherIDsPanelTop.setMaximumSize(new java.awt.Dimension(32767, 8));
        otherIDsPanelTop.setMinimumSize(new java.awt.Dimension(10, 8));
        otherIDsPanelTop.setName("otherIDsPanelTop"); // NOI18N
        otherIDsPanelTop.setPreferredSize(new java.awt.Dimension(628, 8));
        otherIDsPanelTop.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        otherIDsPanel.add(otherIDsPanelTop, java.awt.BorderLayout.PAGE_START);

        otherIDsPanelScrollPane.setMinimumSize(new java.awt.Dimension(400, 30));
        otherIDsPanelScrollPane.setName("otherIDsPanelScrollPane"); // NOI18N

        otherIDsTable.setAutoCreateRowSorter(true);
        otherIDsTable.setModel(getOtherIdentifiersTableModel());
        otherIDsTable.setFillsViewportHeight(true);
        otherIDsTable.setMinimumSize(new java.awt.Dimension(300, 64));
        otherIDsTable.setName("otherIDsTable"); // NOI18N
        otherIDsPanelScrollPane.setViewportView(otherIDsTable);

        otherIDsPanel.add(otherIDsPanelScrollPane, java.awt.BorderLayout.CENTER);

        otherIDsPanelBottom.setMaximumSize(new java.awt.Dimension(32767, 36));
        otherIDsPanelBottom.setName("otherIDsPanelBottom"); // NOI18N
        otherIDsPanelBottom.setPreferredSize(new java.awt.Dimension(576, 36));
        otherIDsPanelBottom.setLayout(new java.awt.BorderLayout());

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addOtherIDButton.setText(resourceMap.getString("addOtherIDButton.text")); // NOI18N
        addOtherIDButton.setName("addOtherIDButton"); // NOI18N
        addOtherIDButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addOtherIDButtonPressed(evt);
            }
        });
        jPanel7.add(addOtherIDButton);

        removeOtherIDButton1.setText(resourceMap.getString("removeOtherIDButton1.text")); // NOI18N
        removeOtherIDButton1.setName("removeOtherIDButton1"); // NOI18N
        removeOtherIDButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeOtherIDButton1Pressed(evt);
            }
        });
        jPanel7.add(removeOtherIDButton1);

        otherIDsPanelBottom.add(jPanel7, java.awt.BorderLayout.WEST);

        jPanel8.setName("jPanel8"); // NOI18N
        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabel4.setToolTipText(resourceMap.getString("jLabel4.toolTipText")); // NOI18N
        jLabel4.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jLabel4.setIconTextGap(10);
        jLabel4.setName("jLabel4"); // NOI18N
        jPanel8.add(jLabel4);

        otherIDsPanelBottom.add(jPanel8, java.awt.BorderLayout.EAST);

        otherIDsPanel.add(otherIDsPanelBottom, java.awt.BorderLayout.PAGE_END);

        tabbedPane.addTab(resourceMap.getString("otherIDsPanel.TabConstraints.tabTitle"), otherIDsPanel); // NOI18N

        biomartPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5), javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEtchedBorder(), javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5))));
        biomartPanel.setMaximumSize(new java.awt.Dimension(2147483647, 200));
        biomartPanel.setName("biomartPanel"); // NOI18N
        biomartPanel.setPreferredSize(new java.awt.Dimension(384, 200));
        biomartPanel.setLayout(new java.awt.BorderLayout());

        biomartPanelTop.setMaximumSize(new java.awt.Dimension(32767, 8));
        biomartPanelTop.setMinimumSize(new java.awt.Dimension(10, 8));
        biomartPanelTop.setName("biomartPanelTop"); // NOI18N
        biomartPanelTop.setPreferredSize(new java.awt.Dimension(628, 8));
        biomartPanelTop.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        biomartPanel.add(biomartPanelTop, java.awt.BorderLayout.NORTH);

        biomartPanelScrollPane.setMinimumSize(new java.awt.Dimension(400, 30));
        biomartPanelScrollPane.setName("biomartPanelScrollPane"); // NOI18N

        biomartTable.setAutoCreateRowSorter(true);
        biomartTable.setModel(getBioMartTableModel());
        biomartTable.setFillsViewportHeight(true);
        biomartTable.setMinimumSize(new java.awt.Dimension(300, 64));
        biomartTable.setName("biomartTable"); // NOI18N
        biomartPanelScrollPane.setViewportView(biomartTable);

        biomartPanel.add(biomartPanelScrollPane, java.awt.BorderLayout.CENTER);

        biomartPaneBottom.setMaximumSize(new java.awt.Dimension(32767, 36));
        biomartPaneBottom.setName("biomartPaneBottom"); // NOI18N
        biomartPaneBottom.setPreferredSize(new java.awt.Dimension(576, 36));
        biomartPaneBottom.setLayout(new java.awt.BorderLayout());

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addBiomartButton.setText(resourceMap.getString("addBiomartButton.text")); // NOI18N
        addBiomartButton.setName("addBiomartButton"); // NOI18N
        addBiomartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBioMartButtonPressed(evt);
            }
        });
        jPanel5.add(addBiomartButton);

        removeBiomartButton.setText(resourceMap.getString("removeBiomartButton.text")); // NOI18N
        removeBiomartButton.setName("removeBiomartButton"); // NOI18N
        removeBiomartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeBioMartButtonPressed(evt);
            }
        });
        jPanel5.add(removeBiomartButton);

        biomartPaneBottom.add(jPanel5, java.awt.BorderLayout.WEST);

        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabel3.setIcon(resourceMap.getIcon("jLabel3.icon")); // NOI18N
        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setToolTipText(resourceMap.getString("jLabel3.toolTipText")); // NOI18N
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        jLabel3.setIconTextGap(10);
        jLabel3.setName("jLabel3"); // NOI18N
        jPanel6.add(jLabel3);

        biomartPaneBottom.add(jPanel6, java.awt.BorderLayout.EAST);

        biomartPanel.add(biomartPaneBottom, java.awt.BorderLayout.SOUTH);

        tabbedPane.addTab(resourceMap.getString("biomartPanel.TabConstraints.tabTitle"), biomartPanel); // NOI18N

        internalPanel.add(tabbedPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(internalPanel, java.awt.BorderLayout.CENTER);

        bottomControlsPanel.setMinimumSize(new java.awt.Dimension(10, 40));
        bottomControlsPanel.setName("bottomControlsPanel"); // NOI18N
        bottomControlsPanel.setPreferredSize(new java.awt.Dimension(400, 40));
        bottomControlsPanel.setLayout(new java.awt.BorderLayout());

        ok_cancel_panel.setName("ok_cancel_panel"); // NOI18N
        ok_cancel_panel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

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
        ok_cancel_panel.add(okButton);

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
        ok_cancel_panel.add(cancelButton);

        bottomControlsPanel.add(ok_cancel_panel, java.awt.BorderLayout.EAST);

        bottomLeftControlsPanel.setName("bottomLeftControlsPanel"); // NOI18N
        bottomLeftControlsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addFromFileButton.setText(resourceMap.getString("addFromFileButton.text")); // NOI18N
        addFromFileButton.setMaximumSize(null);
        addFromFileButton.setMinimumSize(null);
        addFromFileButton.setName("addFromFileButton"); // NOI18N
        addFromFileButton.setPreferredSize(null);
        addFromFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFromFileButtonPressed(evt);
            }
        });
        bottomLeftControlsPanel.add(addFromFileButton);

        saveToFileButton.setText(resourceMap.getString("saveToFileButton.text")); // NOI18N
        saveToFileButton.setMaximumSize(null);
        saveToFileButton.setMinimumSize(null);
        saveToFileButton.setName("saveToFileButton"); // NOI18N
        saveToFileButton.setPreferredSize(null);
        saveToFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportToFileButtonPressed(evt);
            }
        });
        bottomLeftControlsPanel.add(saveToFileButton);

        bottomControlsPanel.add(bottomLeftControlsPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(bottomControlsPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
        this.setVisible(false);
    }//GEN-LAST:event_cancelButtonPressed

    private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
        ArrayList<String[]> newconfiguration=new ArrayList<String[]>();
        ArrayList<String[]> biomartDBlist=new ArrayList<String[]>();
        ArrayList<String[]> geneIDlist=new ArrayList<String[]>();
        ArrayList<String[]> buildList=new ArrayList<String[]>();
        ArrayList<String[]> otherIDlist=new ArrayList<String[]>();        
        
        for (int row=0;row<organismsTable.getRowCount();row++) {
           Object taxIDobject=organismsTable.getValueAt(row, 0);
           String taxID=(taxIDobject!=null)?taxIDobject.toString().trim():"";
           String commonName=((String)organismsTable.getValueAt(row, 1));
           String latinName=((String)organismsTable.getValueAt(row, 2));
           String clade=((String)organismsTable.getValueAt(row, 3));
           String bioMartDB=((String)organismsTable.getValueAt(row, 4));
           String builds=((String)organismsTable.getValueAt(row, 5));
           if (commonName==null) commonName=""; else commonName=commonName.trim();
           if (latinName==null) latinName=""; else latinName=latinName.trim();
           if (clade==null) clade=""; else clade=clade.trim();
           if (bioMartDB==null) bioMartDB=""; else bioMartDB=bioMartDB.trim();
           if (builds==null) builds=""; else builds=builds.trim();
           
           String[] rowdata=new String[]{taxID,commonName,latinName,builds,clade};
           if (!(taxID.isEmpty() && commonName.isEmpty() && latinName.isEmpty() && clade.isEmpty() && builds.isEmpty())) newconfiguration.add(rowdata);
           if (!(taxID.isEmpty() && bioMartDB.isEmpty())) biomartDBlist.add(new String[]{taxID,bioMartDB});
        }
        for (int row=0;row<geneIDsTable.getRowCount();row++) {
           String geneIDDatabaseName=((String)geneIDsTable.getValueAt(row, 0)).trim();
           String geneIDPresentationName=((String)geneIDsTable.getValueAt(row, 1)).trim();
           String geneIDBioMartName=((String)geneIDsTable.getValueAt(row, 2)).trim();
           String geneIDWebLink=((String)geneIDsTable.getValueAt(row, 3)).trim();           
           if (!(geneIDPresentationName.isEmpty() && geneIDBioMartName.isEmpty())) {
               if (geneIDDatabaseName.isEmpty()) {
                   geneIDDatabaseName=geneIDPresentationName;
                   if (geneIDDatabaseName.contains(" ")) geneIDDatabaseName=geneIDDatabaseName.substring(0,geneIDDatabaseName.indexOf(' '));
               }
               geneIDlist.add(new String[]{geneIDPresentationName,geneIDBioMartName,geneIDDatabaseName,geneIDWebLink});
           }
        } 
        for (int row=0;row<otherIDsTable.getRowCount();row++) {
           String otherIDName=((String)otherIDsTable.getValueAt(row, 0)).trim();
           String otherIDWebLink=((String)otherIDsTable.getValueAt(row, 1)).trim();      
           if (!otherIDName.isEmpty()) {
               otherIDlist.add(new String[]{otherIDName,otherIDWebLink});
           }
        }         
        for (int row=0;row<biomartTable.getRowCount();row++) {
           String buildName=((String)biomartTable.getValueAt(row, 0));
           String biomartURL=((String)biomartTable.getValueAt(row, 1));
           String schema=((String)biomartTable.getValueAt(row, 2));
           String configversion=((String)biomartTable.getValueAt(row, 3)); 
           String attributes=((String)biomartTable.getValueAt(row, 4));            
           if (buildName!=null) buildName=buildName.trim();
           if (biomartURL!=null) biomartURL=biomartURL.trim();
           if (schema!=null) schema=schema.trim();
           if (configversion!=null) configversion=configversion.trim();     
           if (attributes!=null) attributes=attributes.trim();        
           
           if (!(buildName.isEmpty() && biomartURL.isEmpty())) {         
               buildList.add(new String[]{buildName,biomartURL,schema,configversion,attributes});
           }
        }          
        try {
            Organism.replaceandSave(newconfiguration, null); // check first for errors
            gui.getEngine().getGeneIDResolver().replaceAndSave(biomartDBlist,geneIDlist,otherIDlist,buildList, null); // check first for errors
        } catch (ParseError e) {
            JOptionPane.showMessageDialog(ConfigureOrganismsAndIdentifiersDialog.this, e.getMessage(),"Configuration Error",JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception e) {
             JOptionPane.showMessageDialog(ConfigureOrganismsAndIdentifiersDialog.this, e.getMessage(),e.getClass().getSimpleName(),JOptionPane.ERROR_MESSAGE);           
             return;
        }
        // no errors in the configuration. Try again and save the configurations this time
         try {
          File organismFile=new File(gui.getEngine().getMotifLabDirectory()+java.io.File.separator+"Organisms.config");
          Organism.replaceandSave(newconfiguration, organismFile); // check first for errors
          File geneIDresolverFile=new File(gui.getEngine().getMotifLabDirectory()+java.io.File.separator+"GeneIDResolver.config");
          gui.getEngine().getGeneIDResolver().replaceAndSave(biomartDBlist,geneIDlist,otherIDlist,buildList, geneIDresolverFile); // check first for errors
        } catch (Exception e) {
             JOptionPane.showMessageDialog(ConfigureOrganismsAndIdentifiersDialog.this, e.getMessage(),e.getClass().getSimpleName(),JOptionPane.ERROR_MESSAGE);           
             return;
        }       
        this.setVisible(false);
    }//GEN-LAST:event_okButtonPressed

    private void addBioMartButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBioMartButtonPressed
        ((DefaultTableModel)biomartTable.getModel()).addRow(new Object[]{"",""});
        int lastrow=biomartTable.getRowCount()-1;
        biomartTable.editCellAt(lastrow, 0);
        TableCellEditor editor=biomartTable.getCellEditor(lastrow, 0);
        editor.shouldSelectCell(new ListSelectionEvent(biomartTable,lastrow,lastrow,true));
        biomartTable.setRowSelectionInterval(lastrow, lastrow);
        Component comp=editor.getTableCellEditorComponent(biomartTable, "", true, lastrow, 0);
        biomartTable.scrollRectToVisible(comp.getBounds());        
        if (comp instanceof JTextField) ((JTextField)comp).requestFocus();
      
    }//GEN-LAST:event_addBioMartButtonPressed

    private void removeBioMartButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBioMartButtonPressed
        int[] selected=biomartTable.getSelectedRows();
        int[] modelselected=new int[selected.length];
        for (int i=0;i<selected.length;i++) modelselected[i]=biomartTable.convertRowIndexToModel(selected[i]);
        DefaultTableModel model=(DefaultTableModel)biomartTable.getModel();
        Arrays.sort(modelselected);
        for (int i=modelselected.length-1;i>=0;i--) { // remove rows from the end to avoid problems with indices changing
            model.removeRow(modelselected[i]);
        }
    }//GEN-LAST:event_removeBioMartButtonPressed

    private void addGeneIDButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addGeneIDButtonPressed
        ((DefaultTableModel)geneIDsTable.getModel()).addRow(new Object[]{"","","",""});
        int lastrow=geneIDsTable.getRowCount()-1;
        geneIDsTable.editCellAt(lastrow, 0);     
        TableCellEditor editor=geneIDsTable.getCellEditor(lastrow, 0);
        editor.shouldSelectCell(new ListSelectionEvent(geneIDsTable,lastrow,lastrow,true));
        geneIDsTable.setRowSelectionInterval(lastrow, lastrow);
        Component comp=editor.getTableCellEditorComponent(geneIDsTable, "", true, lastrow, 0);
        geneIDsTable.scrollRectToVisible(comp.getBounds());
        if (comp instanceof JTextField) ((JTextField)comp).requestFocus();      
    }//GEN-LAST:event_addGeneIDButtonPressed

    private void removeGeneIDButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeGeneIDButtonPressed
        int[] selected=geneIDsTable.getSelectedRows();
        int[] modelselected=new int[selected.length];
        for (int i=0;i<selected.length;i++) modelselected[i]=geneIDsTable.convertRowIndexToModel(selected[i]);
        DefaultTableModel model=(DefaultTableModel)geneIDsTable.getModel();
        Arrays.sort(modelselected);
        for (int i=modelselected.length-1;i>=0;i--) { // remove rows from the end to avoid problems with indices changing
            model.removeRow(modelselected[i]);
        }
    }//GEN-LAST:event_removeGeneIDButtonPressed

    private void addOrganismButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addOrganismButtonPressed
        ((DefaultTableModel)organismsTable.getModel()).addRow(new Object[]{"","","","","",""});
        int lastrow=organismsTable.getRowCount()-1;
        organismsTable.editCellAt(lastrow, 0); 
        TableCellEditor editor=organismsTable.getCellEditor(lastrow, 0);
        editor.shouldSelectCell(new ListSelectionEvent(organismsTable,lastrow,lastrow,true));
        organismsTable.setRowSelectionInterval(lastrow, lastrow);
        Component comp=editor.getTableCellEditorComponent(organismsTable, "", true, lastrow, 0);
        organismsTable.scrollRectToVisible(comp.getBounds());        
        if (comp instanceof JTextField) ((JTextField)comp).requestFocus();           
    }//GEN-LAST:event_addOrganismButtonPressed

    private void removeOrganismButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeOrganismButtonPressed
        int[] selected=organismsTable.getSelectedRows();
        int[] modelselected=new int[selected.length];
        for (int i=0;i<selected.length;i++) modelselected[i]=organismsTable.convertRowIndexToModel(selected[i]);
        DefaultTableModel model=(DefaultTableModel)organismsTable.getModel();
        Arrays.sort(modelselected);
        for (int i=modelselected.length-1;i>=0;i--) { // remove rows from the end to avoid problems with indices changing
            model.removeRow(modelselected[i]);
        }
    }//GEN-LAST:event_removeOrganismButtonPressed

    private void addOtherIDButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addOtherIDButtonPressed
        ((DefaultTableModel)otherIDsTable.getModel()).addRow(new Object[]{"",""});
        int lastrow=otherIDsTable.getRowCount()-1;
        otherIDsTable.editCellAt(lastrow, 0);     
        TableCellEditor editor=otherIDsTable.getCellEditor(lastrow, 0);
        editor.shouldSelectCell(new ListSelectionEvent(otherIDsTable,lastrow,lastrow,true));
        otherIDsTable.setRowSelectionInterval(lastrow, lastrow);
        Component comp=editor.getTableCellEditorComponent(otherIDsTable, "", true, lastrow, 0);
        otherIDsTable.scrollRectToVisible(comp.getBounds());
        if (comp instanceof JTextField) ((JTextField)comp).requestFocus();         
        
    }//GEN-LAST:event_addOtherIDButtonPressed

    private void removeOtherIDButton1Pressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeOtherIDButton1Pressed
        int[] selected=otherIDsTable.getSelectedRows();
        int[] modelselected=new int[selected.length];
        for (int i=0;i<selected.length;i++) modelselected[i]=otherIDsTable.convertRowIndexToModel(selected[i]);
        DefaultTableModel model=(DefaultTableModel)otherIDsTable.getModel();
        Arrays.sort(modelselected);
        for (int i=modelselected.length-1;i>=0;i--) { // remove rows from the end to avoid problems with indices changing
            model.removeRow(modelselected[i]);
        }              
    }//GEN-LAST:event_removeOtherIDButton1Pressed

    private void addFromFileButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFromFileButtonPressed
        final JFileChooser fc = gui.getFileChooser(null);
        fc.setDialogTitle("Import Configuration File");
        FileNameExtensionFilter xmlFilter=new FileNameExtensionFilter("Configuration File (*.xml)", "XML","xml");
        fc.addChoosableFileFilter(xmlFilter);
        fc.setFileFilter(xmlFilter);        
        int returnValue=fc.showOpenDialog(this);
        if (returnValue!=JFileChooser.APPROVE_OPTION) return; // user cancelled
        File file=fc.getSelectedFile();
        gui.setLastUsedDirectory(file.getParentFile());
        try {
            BufferedInputStream inputstream=new BufferedInputStream(MotifLabEngine.getInputStreamForFile(file));
            int[] result=parseConfigurationFile(inputstream); 
            String message="Imported:\n\n   "+result[0]+" organism"+((result[0]==1)?"":"s")+"\n   "+result[1]+" gene identifier"+((result[1]==1)?"":"s")+"\n   "+result[2]+" other identifier"+((result[2]==1)?"":"s")+"\n   "+result[3]+" BioMart"+((result[3]==1)?"":"s");
            JOptionPane.showMessageDialog(ConfigureOrganismsAndIdentifiersDialog.this, message, "Import Configuration", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception fnf) {
            JOptionPane.showMessageDialog(ConfigureOrganismsAndIdentifiersDialog.this, fnf.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } 

    }//GEN-LAST:event_addFromFileButtonPressed

    private void exportToFileButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportToFileButtonPressed
        final JFileChooser fc = gui.getFileChooser(null);
        fc.setDialogTitle("Export Configuration File"); 
        FileNameExtensionFilter xmlFilter=new FileNameExtensionFilter("Configuration File (*.xml)", "XML","xml");
        fc.addChoosableFileFilter(xmlFilter);
        fc.setFileFilter(xmlFilter);         
        File preselected=MotifLabEngine.getFile(gui.getLastUsedDirectory(), "organisms.xml");
        fc.setSelectedFile(preselected);
        int returnValue=fc.showSaveDialog(ConfigureOrganismsAndIdentifiersDialog.this);
        if (returnValue==JFileChooser.APPROVE_OPTION) {
            File outfile=fc.getSelectedFile(); 
            if (outfile.exists()) {
                int confirm=JOptionPane.showConfirmDialog(toppanel, "Do you want to replace existing file '"+outfile.getName()+"'?");
                if (confirm!=JOptionPane.OK_OPTION) return;
            }
            try {
                saveConfigurationToFile(outfile);
                JOptionPane.showMessageDialog(ConfigureOrganismsAndIdentifiersDialog.this, "Configuration saved to file '"+outfile.getAbsolutePath()+"'", "Export Configuration", JOptionPane.INFORMATION_MESSAGE);
            }
            catch (Exception fnf) {
                JOptionPane.showMessageDialog(ConfigureOrganismsAndIdentifiersDialog.this, fnf.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                gui.logError(fnf);
            } 
        } 
    }//GEN-LAST:event_exportToFileButtonPressed

    
    private int[] parseConfigurationFile(InputStream inputstream) throws ParserConfigurationException, SAXException, IOException {
         int[] importCounters=new int[]{0,0,0,0}; // count imported organism, gene identifiers, other identifiers, biomarts
         boolean askBeforeOverwrite=true;
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document doc = builder.parse(inputstream);
         NodeList configurationnodes = doc.getElementsByTagName("configuration");
         if (configurationnodes.getLength()==0) throw new SAXException("Missing <configuration> root element");
         Element configurationNode=(Element)configurationnodes.item(0);
                
         NodeList organismsnodes = configurationNode.getElementsByTagName("organisms");
         if (organismsnodes.getLength()>0) {
             Element organismsNode=(Element)organismsnodes.item(0);
             NodeList organismNodes = organismsNode.getElementsByTagName("organism");
             for (int i=0;i<organismNodes.getLength();i++) {
                 Element organismElement=(Element)organismNodes.item(i);
                 String taxonomy=organismElement.getAttribute("taxonomy");
                 String commonname=organismElement.getAttribute("name");
                 String latinname=organismElement.getAttribute("latin");
                 String clade=organismElement.getAttribute("clade");
                 String biomart=organismElement.getAttribute("biomart");
                 StringBuilder genomebuffer=new StringBuilder();
                 NodeList buildnodes = organismElement.getElementsByTagName("genomebuild");
                 for (int j=0;j<buildnodes.getLength();j++) {
                     Element buildElement=(Element)buildnodes.item(j);
                     String buildname=buildElement.getTextContent();
                     buildname=buildname.replace(",", "|"); // alternative names are separated by commas in the configfile but vertical bars in the table
                     if (j>0) genomebuffer.append(",");
                     genomebuffer.append(buildname);
                 }
                 String buildstring=genomebuffer.toString();
                 String[] fields=new String[]{taxonomy,commonname,latinname,clade,biomart,buildstring};
                 askBeforeOverwrite=(addToOrganismTable(fields, askBeforeOverwrite, importCounters) && askBeforeOverwrite);
             }
         }    
         askBeforeOverwrite=true; // reset before each new section
         NodeList identifiersnodes = configurationNode.getElementsByTagName("identifiers");
         if (identifiersnodes.getLength()>0) {
             Element identifiersNode=(Element)identifiersnodes.item(0);
             NodeList identifierNodes = identifiersNode.getElementsByTagName("identifier");
             for (int i=0;i<identifierNodes.getLength();i++) {
                 Element identifierElement=(Element)identifierNodes.item(i);
                 String type=identifierElement.getAttribute("type");
                 String name=identifierElement.getAttribute("name");
                 String database=identifierElement.getAttribute("database");
                 String biomart=identifierElement.getAttribute("biomart");                 
                 StringBuilder linksbuffer=new StringBuilder();
                 NodeList linknodes = identifierElement.getElementsByTagName("link");
                 for (int j=0;j<linknodes.getLength();j++) {
                     if (j>0) linksbuffer.append(",");                     
                     Element linkElement=(Element)linknodes.item(j);
                     String url=linkElement.getTextContent();
                     String match=linkElement.getAttribute("match");
                     if (match!=null && !match.isEmpty()) {
                       linksbuffer.append("[");
                       linksbuffer.append(match);
                       linksbuffer.append(",");  
                       linksbuffer.append(url);
                       linksbuffer.append("]");
                     } else linksbuffer.append(url);
                 }
                 String linkstring=linksbuffer.toString();
                 if (type.equals("gene")) {
                     String[] fields=new String[]{database,name,biomart,linkstring};
                     askBeforeOverwrite=(addToGeneIDTable(fields, askBeforeOverwrite, importCounters) && askBeforeOverwrite);
                 } else {
                     String[] fields=new String[]{name,linkstring};
                     askBeforeOverwrite=(addToOtherIDTable(fields, askBeforeOverwrite, importCounters) && askBeforeOverwrite);                    
                 }
             }
         }
         askBeforeOverwrite=true; // reset before each new section
         NodeList biomartsnodes = configurationNode.getElementsByTagName("biomarts");
         if (biomartsnodes.getLength()>0) {
             Element biomartsNode=(Element)biomartsnodes.item(0);
             NodeList biomartNodes = biomartsNode.getElementsByTagName("biomart");
             for (int i=0;i<biomartNodes.getLength();i++) {
                 Element biomartElement=(Element)biomartNodes.item(i);
                 String build=biomartElement.getAttribute("build");
                 String schema=biomartElement.getAttribute("virtualschema");
                 String configVersion=biomartElement.getAttribute("configversion");
                 StringBuilder attributesbuffer=new StringBuilder();
                 NodeList attributesnodes = biomartElement.getElementsByTagName("attributes");
                 if (attributesnodes.getLength()>0) {
                     Element attributesNode=(Element)attributesnodes.item(0);
                     NodeList attributenodes = attributesNode.getElementsByTagName("attribute");
                     for (int j=0;j<attributenodes.getLength();j++) {
                         if (j>0) attributesbuffer.append(",");
                         Element attributeElement=(Element)attributenodes.item(j);
                         String attributeName=attributeElement.getTextContent();
                         attributesbuffer.append(attributeName);
                     }
                 }
                 String url="";
                 NodeList sourcenodes = biomartElement.getElementsByTagName("source");
                 if (sourcenodes.getLength()>0) {
                     Element source=(Element)sourcenodes.item(0);
                     String type=source.getAttribute("type");
                     String location=source.getTextContent();
                     if (type.equalsIgnoreCase("URL")) url=location;
                     else url=type+"="+location;
                 }
                 String attributesstring=attributesbuffer.toString();
                 String[] fields=new String[]{build,url,schema,configVersion,attributesstring};
                 askBeforeOverwrite=(addToBioMartTable(fields, askBeforeOverwrite, importCounters) && askBeforeOverwrite);
             }
         }   
         return importCounters;
         
    }
    
    private boolean addToOrganismTable(String[] fields, boolean askBeforeOverwrite, int[] counters) {
        int matchingRow=getRowIndexForTableEntry(organismsTable,0,fields[0]);
        if (matchingRow>=0) { // an entry already exists for this organisms. Ask to replace it
            boolean replace=true;
            if (askBeforeOverwrite) {
                int ask=askToOverwrite("An organism with taxonomy ID '"+fields[0]+"' already exists. Do you want to replace it?");
                if (ask==2) replace=false; // Do not replace
                else if (ask==1) askBeforeOverwrite=false; // replace this and all others (don't ask again)
            }
            if (replace) {
                for (int j=0;j<fields.length;j++) organismsTable.setValueAt(fields[j], matchingRow, j);
                counters[0]++;
            }
            return askBeforeOverwrite;
        } else { // organisms i completely new, just add it
            ((DefaultTableModel)organismsTable.getModel()).addRow(fields);
             counters[0]++;
            return askBeforeOverwrite;
        }
    }
    
    private boolean addToGeneIDTable(String[] fields, boolean askBeforeOverwrite, int[] counters) {
        int matchingRow=getRowIndexForTableEntry(geneIDsTable,1,fields[1]);
        if (matchingRow>=0) {
            boolean replace=true;
            if (askBeforeOverwrite) {
                int ask=askToOverwrite("An identifier with name '"+fields[1]+"' already exists in the 'Gene Identifiers' table. Do you want to replace it?");
                if (ask==2) replace=false; // Do not replace
                else if (ask==1) askBeforeOverwrite=false; // replace this and all others (don't ask again)
            }
            if (replace) {
                for (int j=0;j<fields.length;j++) geneIDsTable.setValueAt(fields[j], matchingRow, j);
                counters[1]++;
            }
            return askBeforeOverwrite;
        } else {
            ((DefaultTableModel)geneIDsTable.getModel()).addRow(fields);
            counters[1]++;
            return askBeforeOverwrite;
        }
    }   
    
    private boolean addToOtherIDTable(String[] fields, boolean askBeforeOverwrite, int[] counters) {
        int matchingRow=getRowIndexForTableEntry(otherIDsTable,0,fields[0]);
        if (matchingRow>=0) {
            boolean replace=true;
            if (askBeforeOverwrite) {
                int ask=askToOverwrite("An identifier with name '"+fields[0]+"' already exists in the 'Other Identifiers' table. Do you want to replace it?");
                if (ask==2) replace=false; // Do not replace
                else if (ask==1) askBeforeOverwrite=false; // replace this and all others (don't ask again)
            }
            if (replace) {
                for (int j=0;j<fields.length;j++) otherIDsTable.setValueAt(fields[j], matchingRow, j);
                counters[2]++;
            }
            return askBeforeOverwrite;
        } else {
            ((DefaultTableModel)otherIDsTable.getModel()).addRow(fields);
            counters[2]++;
            return askBeforeOverwrite;
        }
    }   
    
    private boolean addToBioMartTable(String[] fields, boolean askBeforeOverwrite, int[] counters) {
        int matchingRow=getRowIndexForTableEntry(biomartTable,0,fields[0]);
        if (matchingRow>=0) {
            boolean replace=true;
            if (askBeforeOverwrite) {
                int ask=askToOverwrite("An entry for genome build '"+fields[0]+"' already exists in the 'BioMart' table. Do you want to replace it?");
                if (ask==2) replace=false; // Do not replace
                else if (ask==1) askBeforeOverwrite=false; // replace this and all others (don't ask again)
            }
            if (replace) {
                for (int j=0;j<fields.length;j++) biomartTable.setValueAt(fields[j], matchingRow, j);
                counters[3]++;
            }
            return askBeforeOverwrite;
        } else {
            ((DefaultTableModel)biomartTable.getModel()).addRow(fields);
            counters[3]++;
            return askBeforeOverwrite;
        }
    }     
    
    /** Searches through a given table and returns the index of the row which contains the targetValue in the specified column
     *  or -1 if no matches were found
     */
    private int getRowIndexForTableEntry(JTable table, int column, String targetValue) {
        for (int i=0;i<table.getRowCount();i++) {
            Object value=table.getValueAt(i, column);
            if (value==null) continue;
            if (value.toString().equals(targetValue)) return i;
        }
        return -1;
    }
    
    /** Returns 0=Y*/
    private int askToOverwrite(String message) {
        String[] buttons = { "Yes", "Yes to all", "No"};
        int reply = JOptionPane.showOptionDialog(ConfigureOrganismsAndIdentifiersDialog.this, message, "Replace entry", JOptionPane.WARNING_MESSAGE, 0, null, buttons, buttons[2]);
        return reply;
    }

    private void saveConfigurationToFile(File file) throws Exception {
        Document document=getXMLrepresentation();
        TransformerFactory factory=TransformerFactory.newInstance();
        try {
            factory.setAttribute("indent-number", new Integer(3));
        } catch (IllegalArgumentException iae) {}
        Transformer transformer=factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source=new DOMSource(document); 
        OutputStream stream=MotifLabEngine.getOutputStreamForFile(file);
        StreamResult result=new StreamResult(new OutputStreamWriter(new BufferedOutputStream(stream),"UTF-8"));
        transformer.transform(source, result);        
    }

    public Document getXMLrepresentation() throws Exception {
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document document = builder.newDocument();
         Element configuration=document.createElement("configuration");    
         
         if (organismsTable.getRowCount()>0) { 
            Element organisms=document.createElement("organisms");       
            for (int row=0;row<organismsTable.getRowCount();row++) {
               Element organism=document.createElement("organism");  
               Object taxIDobject=organismsTable.getValueAt(row, 0);
               String taxID=(taxIDobject!=null)?taxIDobject.toString().trim():"";
               String commonName=((String)organismsTable.getValueAt(row, 1));
               String latinName=((String)organismsTable.getValueAt(row, 2));
               String clade=((String)organismsTable.getValueAt(row, 3));
               String bioMartDB=((String)organismsTable.getValueAt(row, 4));
               String builds=((String)organismsTable.getValueAt(row, 5));
               if (commonName==null) commonName=""; else commonName=commonName.trim();
               if (latinName==null) latinName=""; else latinName=latinName.trim();
               if (clade==null) clade=""; else clade=clade.trim();
               if (bioMartDB==null) bioMartDB=""; else bioMartDB=bioMartDB.trim();               
               if (builds==null) builds=""; else builds=builds.trim();

               if (!taxID.isEmpty()) organism.setAttribute("taxonomy", taxID);
               if (!commonName.isEmpty()) organism.setAttribute("name", commonName);
               if (!latinName.isEmpty()) organism.setAttribute("latin", latinName);
               if (!clade.isEmpty()) organism.setAttribute("clade", clade);
               if (!bioMartDB.isEmpty()) organism.setAttribute("biomart", bioMartDB);
               if (!builds.isEmpty()) {
                   String[] split1=builds.split(",");
                   for (String build:split1) {
                       build=build.replace("|", ","); // use commas instead of pipes to separate alternative names for same build in the config-file
                       Element genomebuild=document.createElement("genomebuild");  
                       genomebuild.setTextContent(build);
                       organism.appendChild(genomebuild);                       
                   }
               }               
               organisms.appendChild(organism);
            }
            configuration.appendChild(organisms);
         } // end organisms
         
         if (geneIDsTable.getRowCount()+otherIDsTable.getRowCount()>0) {
             Element identifiers=document.createElement("identifiers");  
             for (int row=0;row<geneIDsTable.getRowCount();row++) {
               Element identifier=document.createElement("identifier"); 
               identifier.setAttribute("type", "gene");
               String geneIDDatabaseName=((String)geneIDsTable.getValueAt(row, 0)).trim();
               String geneIDPresentationName=((String)geneIDsTable.getValueAt(row, 1)).trim();
               String geneIDBioMartName=((String)geneIDsTable.getValueAt(row, 2)).trim();
               String geneIDWebLink=((String)geneIDsTable.getValueAt(row, 3)).trim();                
               if (!(geneIDPresentationName.isEmpty() && geneIDBioMartName.isEmpty())) {
                   if (geneIDDatabaseName.isEmpty()) {
                       geneIDDatabaseName=geneIDPresentationName;
                       if (geneIDDatabaseName.contains(" ")) geneIDDatabaseName=geneIDDatabaseName.substring(0,geneIDDatabaseName.indexOf(' '));
                   }
               }
               if (!geneIDPresentationName.isEmpty()) identifier.setAttribute("name", geneIDPresentationName);  
               if (!geneIDDatabaseName.isEmpty()) identifier.setAttribute("database", geneIDDatabaseName);                
               if (!geneIDBioMartName.isEmpty()) identifier.setAttribute("biomart", geneIDBioMartName); 
               if (!geneIDWebLink.isEmpty()) {
                   ArrayList<String[]> links=parseWebLinks(geneIDWebLink);
                   for (String[] pair:links) {
                       Element linkElement=document.createElement("link"); 
                       if (pair[0]!=null) linkElement.setAttribute("match", pair[0]);
                       linkElement.setTextContent(pair[1]);
                       identifier.appendChild(linkElement);
                   }
               }
               identifiers.appendChild(identifier);
            } 
            for (int row=0;row<otherIDsTable.getRowCount();row++) {
               Element identifier=document.createElement("identifier"); 
               identifier.setAttribute("type", "other");                
               String otherIDName=((String)otherIDsTable.getValueAt(row, 0)).trim();   
               String otherIDWebLink=((String)otherIDsTable.getValueAt(row, 1)).trim();      
               if (!otherIDName.isEmpty()) identifier.setAttribute("name", otherIDName);  
               ArrayList<String[]> links=parseWebLinks(otherIDWebLink);
               for (String[] pair:links) {
                   Element linkElement=document.createElement("link"); 
                   if (pair[0]!=null) linkElement.setAttribute("match", pair[0]);
                   linkElement.setTextContent(pair[1]);
                   identifier.appendChild(linkElement);
               }
               identifiers.appendChild(identifier);
            }  
            configuration.appendChild(identifiers);
        }
        if (biomartTable.getRowCount()>0) {
            Element biomarts=document.createElement("biomarts");            
            for (int row=0;row<biomartTable.getRowCount();row++) {
               
               String buildName=((String)biomartTable.getValueAt(row, 0));
               String biomartURL=((String)biomartTable.getValueAt(row, 1));
               String schema=((String)biomartTable.getValueAt(row, 2));
               String configversion=((String)biomartTable.getValueAt(row, 3)); 
               String attributes=((String)biomartTable.getValueAt(row, 4));            
               if (buildName!=null) buildName=buildName.trim();
               if (biomartURL!=null) biomartURL=biomartURL.trim();
               if (schema!=null) schema=schema.trim();
               if (configversion!=null) configversion=configversion.trim();     
               if (attributes!=null) attributes=attributes.trim();        
               if (!(buildName.isEmpty() && biomartURL.isEmpty())) {   
                   Element biomart=document.createElement("biomart");
                   if (!buildName.isEmpty()) biomart.setAttribute("build", buildName);
                   if (!configversion.isEmpty()) biomart.setAttribute("configversion", configversion);
                   if (!schema.isEmpty()) biomart.setAttribute("virtualschema", schema);
                   if (!attributes.isEmpty()) {
                       Element attributesElement=document.createElement("attributes");
                       for (String attr:attributes.split(",")) {
                          Element attrElement=document.createElement("attribute"); 
                          attrElement.setTextContent(attr);
                          attributesElement.appendChild(attrElement);
                       }
                       biomart.appendChild(attributesElement);
                   }
                   Element source=document.createElement("source");
                   if (biomartURL.startsWith("config=")) {
                       source.setAttribute("type","config");
                       source.setTextContent(biomartURL.substring("config=".length()));                       
                   } else if (biomartURL.startsWith("webfile=")) {
                       source.setAttribute("type","webfile");
                       source.setTextContent(biomartURL.substring("webfile=".length()));                       
                   } else if (biomartURL.startsWith("file=")) {
                       source.setAttribute("type","file");
                       source.setTextContent(biomartURL.substring("file=".length()));                       
                   } else {
                       source.setAttribute("type","URL");
                       source.setTextContent(biomartURL);                       
                   } 
                   biomart.appendChild(source);
                   biomarts.appendChild(biomart);
               }  
            }
            configuration.appendChild(biomarts);
        }  
        document.appendChild(configuration);
        return document;        
    }
    
    /** Parses the web links format and returns its components as a list of pairs where the first element in each pair is the regex (could be null!) and the second is the URL */
    private ArrayList<String[]> parseWebLinks(String linkstring) throws ParseError {
        ArrayList<String> parts=MotifLabEngine.splitOnCharacter(linkstring, ',', '[', ']');
        ArrayList<String[]> result=new ArrayList<>();
        for (String element:parts) {
            if (element.startsWith("[")) {
                element=MotifLabEngine.stripBraces(element, "[", "]");
                String[] pair=element.split(",",2);
                if (pair.length!=2) throw new ParseError("Syntax error in web-link pattern (missing comma):"+element);
                result.add(pair);
            } else {
               result.add(new String[]{null,element});
            }
        }
        return result;
    }    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AddOrganismButton;
    private javax.swing.JButton addBiomartButton;
    private javax.swing.JButton addFromFileButton;
    private javax.swing.JButton addGeneIDButton;
    private javax.swing.JButton addOtherIDButton;
    private javax.swing.JPanel biomartPaneBottom;
    private javax.swing.JPanel biomartPanel;
    private javax.swing.JScrollPane biomartPanelScrollPane;
    private javax.swing.JPanel biomartPanelTop;
    private javax.swing.JTable biomartTable;
    private javax.swing.JPanel bottomControlsPanel;
    private javax.swing.JPanel bottomLeftControlsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel geneIDformatsPanel;
    private javax.swing.JPanel geneIDformatsPanelBottom;
    private javax.swing.JScrollPane geneIDformatsPanelScrollPane;
    private javax.swing.JPanel geneIDformatsPanelTop;
    private javax.swing.JTable geneIDsTable;
    private javax.swing.JPanel internalPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel ok_cancel_panel;
    private javax.swing.JPanel organismsPanel;
    private javax.swing.JPanel organismsPanelBottom;
    private javax.swing.JScrollPane organismsPanelScrollPane;
    private javax.swing.JPanel organismsPanelTop;
    private javax.swing.JTable organismsTable;
    private javax.swing.JPanel otherIDsPanel;
    private javax.swing.JPanel otherIDsPanelBottom;
    private javax.swing.JScrollPane otherIDsPanelScrollPane;
    private javax.swing.JPanel otherIDsPanelTop;
    private javax.swing.JTable otherIDsTable;
    private javax.swing.JButton removeBiomartButton;
    private javax.swing.JButton removeGeneIDButton;
    private javax.swing.JButton removeOrganismButton;
    private javax.swing.JButton removeOtherIDButton1;
    private javax.swing.JButton saveToFileButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JPanel toppanel;
    // End of variables declaration//GEN-END:variables
}
