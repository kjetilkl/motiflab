/*
 * DASRegistryDialog.java
 *
 * Created on 20. september 2011, 18:17
 * 
 */

package motiflab.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import motiflab.engine.data.*;
import motiflab.engine.datasource.*;
import motiflab.engine.MotifLabEngine;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * @author  kjetikl
 */
public class DASRegistryDialog extends javax.swing.JDialog {
    private final String TABLECOLUMN_NAME="Name";    
    private final String TABLECOLUMN_DESCRIPTION="Description";
    private final String TABLECOLUMN_BUILD="Build";
    private final String TABLECOLUMN_SOURCE="Source";    
    private JTable featuresTable;
    private DefaultTableModel featuresTableModel;
    private MotifLabGUI gui;
    private MotifLabEngine engine;
    private boolean OKpressed=false;    
    //private RowFilter<Object,Object> showSupportedRowFilter=null;
    private RowFilter<Object,Object> expressionRowFilter=null;
    private RowFilter<Object,Object> combinedRowFilter=null;
    private ArrayList<RowFilter<Object,Object>> filters=new ArrayList<RowFilter<Object,Object>>(2);
    private boolean isprocessing=false;
    private boolean isprocessingInterrupted=false; 
    private ObtainDataSwingWorker worker=null; 
    private DASSource selectedDASSource=null;
    
    public DASRegistryDialog(MotifLabGUI gui) {
        this(gui,null);
    }    
    
    /** Creates new form DataTrackDialog */
    public DASRegistryDialog(MotifLabGUI gui, DataTrack track) {
        super(gui.getFrame(), "Select track from DAS Registry", true);
        this.gui=gui;
        engine=gui.getEngine();
        initComponents();
 
        featuresTableModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_DESCRIPTION,TABLECOLUMN_BUILD,TABLECOLUMN_SOURCE},0);
        featuresTable=new JTable(featuresTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        
        featuresTable.setFillsViewportHeight(true);
        featuresTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        featuresTable.setRowSelectionAllowed(true);
        featuresTable.getTableHeader().setReorderingAllowed(false);
        featuresTable.getColumn(TABLECOLUMN_BUILD).setCellRenderer(new GenomeBuildRenderer());
        featuresTable.getColumn(TABLECOLUMN_DESCRIPTION).setCellRenderer(new DescriptionRenderer());
        featuresTable.setRowHeight(18);

        scrollPane.setViewportView(featuresTable);  
        // getRootPane().setDefaultButton(okButton); // this was a bit annoying actually
        
        featuresTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int row=featuresTable.getSelectedRow();
                if (row>=0) {
                    String trackname=(String)featuresTable.getValueAt(row, 0);
                    trackname=trackname.replaceAll("[^\\w\\-]", "_"); // replace illegal characters with underscores
                    trackname=trackname.replaceAll("__+", "_"); // collapse runs of underscores
                    trackname=trackname.replaceAll("^_", ""); // remove leading underscore
                    trackname=trackname.replaceAll("_$", ""); // remove trailing underscore
                    datanameTextfield.setText(trackname);
                    String build=(String)featuresTable.getValueAt(row, 2);
                    String resolvedbuild=Organism.getDefaultBuildName(build, 0);
                    if (resolvedbuild!=null) buildTextField.setText(resolvedbuild);
                    else buildTextField.setText(build);
                    okButton.setEnabled(true);
                }
            }
        });
        featuresTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2) {
                   okButton.doClick();
               }
            }            
        });
        // Disable OK-button if there are currently no sequences 
        if (gui.getEngine().getDefaultSequenceCollection().isEmpty()) okButton.setEnabled(false);
        featuresTable.setAutoCreateRowSorter(true);
        featuresTable.getRowSorter().toggleSortOrder(featuresTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        filters.add(expressionRowFilter);
        filterTextfield.requestFocusInWindow();
        String[] supportedOrganisms=Organism.getSupportedOrganisms();
        Arrays.sort(supportedOrganisms);
        String[] supportedPlusAll=new String[supportedOrganisms.length+1];
        supportedPlusAll[0]="";
        System.arraycopy(supportedOrganisms, 0, supportedPlusAll, 1, supportedOrganisms.length);
        organismCombobox.setModel(new DefaultComboBoxModel(supportedPlusAll));
        ActionListener updateListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTable();
            }
        };
        organismCombobox.addActionListener(updateListener);
        registryCombobox.addActionListener(updateListener);        
        onlySupportedBuildsCombobox.addActionListener(updateListener);
        if (track!=null) {
            datanameTextfield.setText(track.getName());
            datanameTextfield.setEditable(false);
        }
        // We will show the build textfield and let it be editble to allow users to "override" build annotations
//        buildLabel.setVisible(false);
//        buildTextField.setVisible(false);
        organismCombobox.setSelectedIndex(0); // to get things started
    }

    /** This method is called from within the constructor to
     * init((TableRowSorter)datatrackTable.getRowSorter()).setRowFilter(null);
    }ialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        internalPanel = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        topPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        dataNameLabel = new javax.swing.JLabel();
        datanameTextfield = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        organismlabel = new javax.swing.JLabel();
        organismCombobox = new javax.swing.JComboBox();
        buildLabel = new javax.swing.JLabel();
        buildTextField = new javax.swing.JTextField();
        onlySupportedBuildsCombobox = new javax.swing.JCheckBox();
        buttonsPanel = new javax.swing.JPanel();
        filterPanel = new javax.swing.JPanel();
        filterLabel = new javax.swing.JLabel();
        filterTextfield = new javax.swing.JTextField();
        progressPanel = new javax.swing.JPanel();
        progressbar = new javax.swing.JProgressBar();
        okCancelButtonsPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        serverPanel = new javax.swing.JPanel();
        dasRegistryLabel = new javax.swing.JLabel();
        registryCombobox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DASRegistryDialog.class);
        setTitle(resourceMap.getString("DatatrackDialog.title")); // NOI18N
        setName("DatatrackDialog"); // NOI18N

        internalPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4), javax.swing.BorderFactory.createEtchedBorder()));
        internalPanel.setName("internalPanel"); // NOI18N
        internalPanel.setLayout(new java.awt.BorderLayout());

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 8, 10));
        mainPanel.setMinimumSize(new java.awt.Dimension(500, 400));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(700, 400));
        mainPanel.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        internalPanel.add(mainPanel, java.awt.BorderLayout.CENTER);

        topPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 1, 8, 1));
        topPanel.setName("topPanel"); // NOI18N
        topPanel.setLayout(new javax.swing.BoxLayout(topPanel, javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        dataNameLabel.setText(resourceMap.getString("dataNameLabel.text")); // NOI18N
        dataNameLabel.setName("dataNameLabel"); // NOI18N
        jPanel2.add(dataNameLabel);

        datanameTextfield.setColumns(50);
        datanameTextfield.setText(resourceMap.getString("datanameTextfield.text")); // NOI18N
        datanameTextfield.setName("datanameTextfield"); // NOI18N
        datanameTextfield.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                datanameTextfieldActionPerformed(evt);
            }
        });
        jPanel2.add(datanameTextfield);

        topPanel.add(jPanel2);

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 5));

        organismlabel.setText(resourceMap.getString("organismlabel.text")); // NOI18N
        organismlabel.setName("organismlabel"); // NOI18N
        jPanel1.add(organismlabel);

        organismCombobox.setName("organismCombobox"); // NOI18N
        jPanel1.add(organismCombobox);

        buildLabel.setText(resourceMap.getString("buildLabel.text")); // NOI18N
        buildLabel.setName("buildLabel"); // NOI18N
        jPanel1.add(buildLabel);

        buildTextField.setColumns(8);
        buildTextField.setText(resourceMap.getString("buildTextField.text")); // NOI18N
        buildTextField.setName("buildTextField"); // NOI18N
        jPanel1.add(buildTextField);

        onlySupportedBuildsCombobox.setSelected(true);
        onlySupportedBuildsCombobox.setText(resourceMap.getString("onlySupportedBuildsCombobox.text")); // NOI18N
        onlySupportedBuildsCombobox.setToolTipText(resourceMap.getString("onlySupportedBuildsCombobox.toolTipText")); // NOI18N
        onlySupportedBuildsCombobox.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        onlySupportedBuildsCombobox.setName("onlySupportedBuildsCombobox"); // NOI18N
        jPanel1.add(onlySupportedBuildsCombobox);

        topPanel.add(jPanel1);

        internalPanel.add(topPanel, java.awt.BorderLayout.NORTH);

        getContentPane().add(internalPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        filterPanel.setMinimumSize(null);
        filterPanel.setName("filterPanel"); // NOI18N

        filterLabel.setText(resourceMap.getString("filterLabel.text")); // NOI18N
        filterLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 0));
        filterLabel.setName("filterLabel"); // NOI18N
        filterPanel.add(filterLabel);

        filterTextfield.setColumns(18);
        filterTextfield.setText(resourceMap.getString("filterTextfield.text")); // NOI18N
        filterTextfield.setName("filterTextfield"); // NOI18N
        filterTextfield.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                keyReleasedInFilterTextfield(evt);
            }
        });
        filterPanel.add(filterTextfield);

        buttonsPanel.add(filterPanel, java.awt.BorderLayout.WEST);

        progressPanel.setName("progressPanel"); // NOI18N

        progressbar.setName("progressbar"); // NOI18N
        progressbar.setPreferredSize(new java.awt.Dimension(100, 23));
        progressPanel.add(progressbar);

        buttonsPanel.add(progressPanel, java.awt.BorderLayout.CENTER);

        okCancelButtonsPanel.setName("okCancelButtonsPanel"); // NOI18N
        okCancelButtonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setName("okButton"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });
        okCancelButtonsPanel.add(okButton);

        cancelButton.setText(resourceMap.getString("cancelButton.text")); // NOI18N
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonPressed(evt);
            }
        });
        okCancelButtonsPanel.add(cancelButton);

        buttonsPanel.add(okCancelButtonsPanel, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        serverPanel.setName("serverPanel"); // NOI18N
        serverPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        dasRegistryLabel.setText(resourceMap.getString("dasRegistryLabel.text")); // NOI18N
        dasRegistryLabel.setName("dasRegistryLabel"); // NOI18N
        serverPanel.add(dasRegistryLabel);

        registryCombobox.setEditable(true);
        registryCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "www.ebi.ac.uk/das-srv/registry/das/sources", "www.dasregistry.org/das/sources" }));
        registryCombobox.setName("registryCombobox"); // NOI18N
        serverPanel.add(registryCombobox);

        getContentPane().add(serverPanel, java.awt.BorderLayout.NORTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
    String error=null;
    int row=featuresTable.getSelectedRow();
    if (row>=0 && error==null) {
//        String organismName=(String)organismCombobox.getSelectedItem();
//        int organismID=Organism.getTaxonomyID(organismName);        
        String title=(String)featuresTable.getValueAt(row, 0);
        String description=(String)featuresTable.getValueAt(row, 1);
        String baseURL=(String)featuresTable.getValueAt(row, 3);
        selectedDASSource=new DASSource(title, description);        
        String build=buildTextField.getText().trim();
        if (build.isEmpty() && error==null) error="Missing genome build specification";
        int organismID=Organism.getOrganismForGenomeBuild(build);
        String resolvedbuild=(build!=null)?Organism.getDefaultBuildName(build,organismID):null;
        if (resolvedbuild==null && error==null) error="Unknown/unsupported genome build: "+build;       
        else {         
            selectedDASSource.build=resolvedbuild; 
            selectedDASSource.url=baseURL; 
        }
    } else error="No track selected";
    String trackname=datanameTextfield.getText().trim();
    if (error==null && trackname.isEmpty()) error="Missing track name";
    else if (error==null && !trackname.matches("[\\w\\-]+")) error="Track name contains illegal characters (only letters, numbers, underscores and hyphens allowed)";    
    if (error==null) {
        setVisible(false);
        OKpressed=true;
    } else {
        selectedDASSource=null;
        reportError(error);
    }
}//GEN-LAST:event_okButtonPressed

public boolean isOKPressed() {
    return OKpressed && selectedDASSource!=null;
}

private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
    OKpressed=false;
    setVisible(false);
}//GEN-LAST:event_cancelButtonPressed

private void datanameTextfieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_datanameTextfieldActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_datanameTextfieldActionPerformed

@SuppressWarnings("unchecked")
private void keyReleasedInFilterTextfield(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyReleasedInFilterTextfield
    String text=filterTextfield.getText();//GEN-LAST:event_keyReleasedInFilterTextfield
    int namecol=featuresTableModel.findColumn(TABLECOLUMN_NAME);
    if (text!=null && text.isEmpty()) expressionRowFilter=null;
    else {
        text=text.replaceAll("\\W", ""); // to avoid problems with regex characters
        expressionRowFilter=RowFilter.regexFilter("(?i)"+text,namecol);
    }
    installFilters();
}

@SuppressWarnings("unchecked")
private void installFilters() {
   // RowFilter.regexFilter(null, );
   filters.clear();
   //if (showSupportedRowFilter!=null) filters.add(showSupportedRowFilter);
   if (expressionRowFilter!=null) filters.add(expressionRowFilter);
   combinedRowFilter=RowFilter.andFilter(filters);
   ((TableRowSorter)featuresTable.getRowSorter()).setRowFilter(combinedRowFilter);
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel buildLabel;
    private javax.swing.JTextField buildTextField;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel dasRegistryLabel;
    private javax.swing.JLabel dataNameLabel;
    private javax.swing.JTextField datanameTextfield;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JPanel filterPanel;
    private javax.swing.JTextField filterTextfield;
    private javax.swing.JPanel internalPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel okCancelButtonsPanel;
    private javax.swing.JCheckBox onlySupportedBuildsCombobox;
    private javax.swing.JComboBox organismCombobox;
    private javax.swing.JLabel organismlabel;
    private javax.swing.JPanel progressPanel;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JComboBox registryCombobox;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JPanel serverPanel;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables

    private void reportError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public DataTrack getDataTrack() {
        if (selectedDASSource==null) return null; // not valid 
        String dataformat="GFF"; // I am not sure if this is used...
//        String organismName=(String)organismCombobox.getSelectedItem();
//        int organismID=Organism.getTaxonomyID(organismName);
        String trackname=datanameTextfield.getText().trim();     
        String featurename=selectedDASSource.title;
        String baseURL=selectedDASSource.url;
        String description=selectedDASSource.description;
        String build=selectedDASSource.build; // this should have been resolved
        String sourceString="Unknown";
        int organismID=Organism.getOrganismForGenomeBuild(build);
        try {
           URL temp=new URL(baseURL);
           sourceString=temp.getHost();
        } catch (Exception e) {}
        DataTrack datatrack=new DataTrack(trackname, RegionDataset.class, sourceString, description);
        DataSource source=new DataSource_DAS(datatrack, organismID, build, baseURL, dataformat, null);
        datatrack.addDataSource(source);
        return datatrack;
    }    
    
    /** 
     * This method is used to retrieve a new DataSource object based on the selections made in the dialog
     * The source should be added to a preexisting datatrack (it is not automatically added to the argument track)
     * @param datatrack The parent datatrack for this source
     * @return DataSource
     */
    public DataSource getDataSource(DataTrack datatrack) {
        if (selectedDASSource==null) return null;
        String dataformat="GFF"; // I am not sure if this is used...
//        String organismName=(String)organismCombobox.getSelectedItem();
//        int organismID=Organism.getTaxonomyID(organismName);
        String baseURL=selectedDASSource.url;
        String build=selectedDASSource.build; // this should have been resolved
        int organismID=Organism.getOrganismForGenomeBuild(build);
        DataSource source=new DataSource_DAS(datatrack, organismID, build, baseURL, dataformat, null);        
        return source;
    }    
    
    
  /** Contacts the DAS registry and obtains data in an off-EDT thread */
    private void updateTable() {
        String organismName=(String)organismCombobox.getSelectedItem();
        int organism=Organism.getTaxonomyID(organismName);
        if (worker!=null && !worker.isDone()) cancelProcessingAndWait();
        //searchBox.setEnabled(false);
        featuresTableModel.setRowCount(0); // remove all current rows
        isprocessingInterrupted=false;
        progressbar.setVisible(true);
        //String url="http://www.dasregistry.org/das/sources?type=Chromosome";  // this does not work anymore. Perhaps the URL for the registry should be exposed?
        //String url="http://www.ebi.ac.uk/das-srv/registry/das/sources";  // this is a mirror of the one above
        String url=registryCombobox.getSelectedItem().toString();
        if (!(url.startsWith("http://") || url.startsWith("https://"))) url="http://"+url;
        if (!url.contains("?")) url=url+"?type=Chromosome";
        if (organism>0) url+=("&organism="+organism);
        worker=new ObtainDataSwingWorker(url);
        isprocessing=true;
        progressbar.setIndeterminate(true);
        progressbar.setVisible(true);
        worker.execute();        
        
    }
    
    
  /** Cancels the current comparison process.
   *  A call to this method is synchroneous and will not return until
   *  the process is finished or properly canceled.
   */
  public void cancelProcessingAndWait() {
      isprocessingInterrupted=true;
      if (worker!=null) {
          while (!worker.isDone()) {
               Thread.yield();
          }
      }
      isprocessing=false;
      isprocessingInterrupted=false;
     // searchBox.setEnabled(true);
  }

  /** Returns TRUE if the panel is currently involved in calculating comparisons */
  public boolean isProcessing() {
      return isprocessing;
  }    



 private void addSourceToList(final DASSource source) {
     Runnable runner=new Runnable() {
            @Override
            public void run() {
                //if (!worker.isStillActive) return;
                if (onlySupportedBuildsCombobox.isSelected()) {
                   String build=source.build;
                   String defaultBuildName=Organism.getDefaultBuildName(build,0);
                   if (!Organism.isGenomeBuildSupported(defaultBuildName)) return;
                   if (!isSelectedOrganism(build)) return; // do not add the source if it does not match the organism filter
                }
                featuresTableModel.addRow(new String[]{source.title,source.description,source.build,source.url});             
                //progressbar.setValue(progress);
            }
        };
     SwingUtilities.invokeLater(runner);
 }

 /** Returns true if the provided build corresponds to the organism currently selected in the organism combobox (if no selection is made it will always return TRUE) */
 private boolean isSelectedOrganism(String build) {
       String organismName=(String)organismCombobox.getSelectedItem();
       int organismID=0;
       if (organismName==null || organismName.isEmpty()) {
           return true; // no organism selection
       } else {
           organismID=Organism.getTaxonomyID(organismName); 
       }                    
       return (organismID==Organism.getOrganismForGenomeBuild(build));
 }
 
 private class ObtainDataSwingWorker extends SwingWorker<Boolean, Void> {
        boolean isStillActive=true; // still active means it was not cancelled
        String url;

        public ObtainDataSwingWorker(String url) {
            this.url=url;
        }

        @Override
        public Boolean doInBackground() {
            DASRegistryParser parser = new DASRegistryParser();
            try {
                parser.parse(url);
            } catch (Exception e) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        } // end doInBackground
        @Override
        public void done() { // this method is invoked on the EDT!
            isprocessing=false;
            //searchBox.setEnabled(true);
            if (isStillActive) { // if still active then it was allowed to complete
                progressbar.setValue(0);
                progressbar.setVisible(false);
            }
        }
    }; // end of SwingWorker class


 private class GenomeBuildRenderer extends DefaultTableCellRenderer {
    public GenomeBuildRenderer() {
       super();    
    }
    @Override
    public void setValue(Object value) {
       String build=(value!=null)?value.toString():"Unknown";       
       String resolvedbuild=Organism.getDefaultBuildName(build, 0);
       if (resolvedbuild!=null) {
           if (resolvedbuild.equalsIgnoreCase(build)) setText(resolvedbuild);
           else setText(resolvedbuild+"   ("+build+")");
       }
       else setText(build);
    }
}
 private class DescriptionRenderer extends DefaultTableCellRenderer {
    public DescriptionRenderer() {
       super();    
    }
    @Override
    public void setValue(Object value) {
       super.setValue(value);
       setToolTipText((value==null||((String)value).isEmpty())?null:MotifLabGUI.formatTooltipString((String)value,80));
    }
} 
 
private class DASSource {
    String title=null;
    String description=null;
    String build=null;
    String url=null;
    
    public DASSource(String title, String description) {
        this.title=title;
        this.description=(description!=null)?description:"";        
    }
    
    public boolean isValid() {
        return (title!=null && url!=null && build!=null);
    }
    
    @Override
    public String toString() {
        return title+" ["+build+"] "+url;
    }
}    
    
    
/** Parser for the DAS Registry */    
private class DASRegistryParser {
    SAXParserFactory factory;
    SAXParser saxParser;
    ElementParser handler;
    DASSource current=null;
    
    public void parse(String uri) throws Exception {
        factory = SAXParserFactory.newInstance();
        saxParser = factory.newSAXParser();
        handler = new ElementParser();
        URL url=new URL(uri);
        URLConnection connection=url.openConnection();
        connection.setConnectTimeout(gui.getEngine().getNetworkTimeout());
        // Check if the response is a redirection from HTTP to HTTPS. This must be handled manually        
        int status = ((HttpURLConnection)connection).getResponseCode();
        String location = ((HttpURLConnection)connection).getHeaderField("Location");
        if (status>300 && status<400 && location!=null && "http".equalsIgnoreCase(url.getProtocol()) && location.startsWith("https")) {
                ((HttpURLConnection)connection).disconnect();
                parse(location);
                return;
        }        
        InputStream inputStream = connection.getInputStream();
        saxParser.parse(inputStream, handler);
    } 
    
    /** Implements important methods in the callback interface for the SAX XML-reader*/
    private class ElementParser extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("SOURCE")) {               
                String title=attributes.getValue("title");  
                String description=attributes.getValue("description");    
                current=new DASSource(title,description);
            } else if (qName.equals("PROP")) {               
                String name=attributes.getValue("name");  
                String value=attributes.getValue("value");    
//                if (name.equals("features") && value.equals("valid") && current!=null) current.isValid=true;
            } else if (qName.equals("COORDINATES")) {
                String authority=attributes.getValue("authority");  
                String version=attributes.getValue("version");    
                if (authority==null) authority="";
                if (version==null) version="";
                if (current!=null) current.build=authority+version;
            } else if (qName.equals("CAPABILITY")) {
                String query_uri=attributes.getValue("query_uri");  
                String type=attributes.getValue("type");    
                if (type.equals("das1:features") && current!=null) current.url=query_uri;
            }    
        }
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("SOURCE")) {
                if (current!=null && current.isValid()) addSourceToList(current);
            }  
        }


        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {

        }
        
        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            super.fatalError(e);
        }
        
    } // end internal class ElementParser
    
    
}

}
