/*
 * LoadFromFilePanel.java
 *
 * Created on 16. november 2009, 12:43
 */

package motiflab.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import motiflab.engine.datasource.DataLoader;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.dataformat.DataFormat;

/**
 *
 * @author  kjetikl
 */
public class LoadFromFilePanel extends javax.swing.JPanel {    
    private ParametersPanel parametersPanel=null;
    private ParameterSettings parameterSettings=null;
    ArrayList<DataFormat> dataformats=null;
    MotifLabEngine engine;
    MotifLabGUI gui;
    
    /** Creates new form LoadFromFilePanel */
    public LoadFromFilePanel(ArrayList<DataFormat> dataformatslist, MotifLabGUI pgui, Class dataclass) {
        this.gui=pgui;
        this.engine=gui.getEngine();
        initComponents();
        this.dataformats=dataformatslist;
        DataFormat defaultformat=null;
        if (dataclass!=null) {
            defaultformat=engine.getDefaultDataFormat(dataclass);
            if (!dataformatslist.contains(defaultformat)) defaultformat=null;
        }
        //dataformats.remove(engine.getDataFormat("Plain"));
        DataFormat[] formats=new DataFormat[dataformats.size()];
        for (int i=0;i<formats.length;i++) formats[i]=dataformats.get(i);
        Arrays.sort(formats);
        formatCombobox.setModel(new DefaultComboBoxModel(formats));
        formatCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selected=formatCombobox.getSelectedItem();
                if (selected!=null) showParametersPanel(getFormatSettingsPanel((DataFormat)selected)); 
                else additionalParametersPanel.removeAll();
                additionalParametersPanel.revalidate();
                additionalParametersPanel.repaint();
                //scrollpane.repaint();
            }
        });
        final JPanel thispanel=this;
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                File currentfile=gui.getLastUsedDirectory();
                String currentfilename=filenameTextfield.getText();
                if (currentfilename!=null && !currentfilename.isEmpty()) {
                    currentfile=new File(currentfilename);
                }
                JFileChooser chooser=gui.getFileChooser(currentfile);// new JFileChooser(currentfile);
                chooser.setDialogTitle("Import from file");
                FileNameExtensionFilter usefilter=null;
                for (DataFormat format:dataformats) {
                    String[] suffix=format.getSuffixAlternatives();
                    FileNameExtensionFilter filter;
                    String description=format.getName();
                    if (suffix.length>0) {
                        description+=" (";
                        for (int i=0;i<suffix.length-1;i++) {
                            description+="*."+suffix[i]+",";
                        }
                        description+="*."+suffix[suffix.length-1]+")";
                        filter=new FileNameExtensionFilter(description, suffix); // *** This line is ad-hoc ***
                        chooser.addChoosableFileFilter(filter);
                        if (format==formatCombobox.getSelectedItem()) usefilter=filter;
                    }                  
                }
                if (usefilter!=null) chooser.setFileFilter(usefilter);
                int choice=chooser.showOpenDialog(thispanel);
                if (choice==JFileChooser.APPROVE_OPTION) {
                    File file=chooser.getSelectedFile();
                    filenameTextfield.setText(file.getAbsolutePath());
                    gui.setLastUsedDirectory(file.getParentFile());
                }
                setCursor(Cursor.getDefaultCursor());
            }
        });  
        if (helpDataFormatButton.getIcon()==null) helpDataFormatButton.setText("Help");        
        helpDataFormatButton.setToolTipText("See HELP page for this data format");
        helpDataFormatButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showDataFormatHelp();
            }
        });        
        scrollpane.setPreferredSize(new Dimension(200,200));
        if (formatCombobox.getItemCount()>0) {
            if (defaultformat!=null) formatCombobox.setSelectedItem(defaultformat);
            else formatCombobox.setSelectedIndex(0);
        }
    }

    /** */
    public void setDataFormats(ArrayList<DataFormat> dataformatslist, DataFormat selected) {
        this.dataformats=dataformatslist;
        DataFormat[] formats=new DataFormat[dataformats.size()];
        for (int i=0;i<formats.length;i++) formats[i]=dataformats.get(i);
        Arrays.sort(formats);        
        formatCombobox.setModel(new DefaultComboBoxModel(formats));
        if (selected!=null) formatCombobox.setSelectedItem(selected);
        else formatCombobox.setSelectedIndex(0);
    }
    
    
    /** */
    private void showParametersPanel(JPanel panel) {
        additionalParametersPanel.removeAll();
        additionalParametersPanel.add(panel);     
    }
   
    /** */
    private JPanel getFormatSettingsPanel(DataFormat outputFormat) {
        if (outputFormat==null) {
            parametersPanel=null;
            return new JPanel();
        }
        ParametersPanel panel=new ParametersPanel(outputFormat.getParameters(),parameterSettings, null, "input", engine);
        parametersPanel=panel;        
        return panel;
    }    
    

    
    
    /** Returns the filename selected in this dialog. Or null if no name is selected*/
    public String getFilename() {
        String name=filenameTextfield.getText();
        if (name!=null) name=name.trim();
        if (name.isEmpty()) name=null;
        return name;
    }
    
    public DataFormat getDataFormat() {
        return (DataFormat)formatCombobox.getSelectedItem();
    }
    
    public ParameterSettings getParameterSettings() {
       if (parametersPanel==null) return null;
       parametersPanel.setParameters();
       return parametersPanel.getParameterSettings();            
    }
    
    /** The target can be null */
    public Data loadData(Data target,String datatype) throws Exception {
        DataLoader loader=engine.getDataLoader();
        String filename=getFilename();
        DataFormat format=getDataFormat();
        ParameterSettings parameters=getParameterSettings();
        if (filename==null) return null;
        //File file=new File(filename);
        return loader.loadData(filename,datatype,target,format, parameters, null);
    }
    
    private void showDataFormatHelp() {
        Object help=null;
        String error="Help unavailable";
        DataFormat format=(DataFormat)formatCombobox.getSelectedItem();
        if (format!=null) {
            help=format.getHelp(engine);
            error="<h1>"+format.getName()+"</h1><br><br>Detailed documentation for this data format is currently unavailable.";                
        } else help="Unable to determine which data format you refer to...";       
        InfoDialog infodialog=null;        
        if (help instanceof String) infodialog=new InfoDialog(gui, "Help for data format: "+format.getName(), (String)help, 700, 450);
        else if (help instanceof URL) infodialog=new InfoDialog(gui, "Help for data format: "+format.getName(), (URL)help, 700, 450, false);
        infodialog.setErrorMessage(error);
        if (infodialog!=null) infodialog.setVisible(true);
    }     
    
    private DefaultComboBoxModel<String> getImportSources() {
        return new DefaultComboBoxModel<String>(new String[]{"File","Web URL"});
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollpane = new javax.swing.JScrollPane();
        additionalParametersPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        internalPanel = new javax.swing.JPanel();
        sourcePanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        filenameTextfield = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        formatPanel = new javax.swing.JPanel();
        formatLabel = new javax.swing.JLabel();
        formatCombobox = new javax.swing.JComboBox();
        helpDataFormatButton = new javax.swing.JButton();

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        scrollpane.setName("scrollpane"); // NOI18N

        additionalParametersPanel.setName("additionalParametersPanel"); // NOI18N
        additionalParametersPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        scrollpane.setViewportView(additionalParametersPanel);

        add(scrollpane, java.awt.BorderLayout.CENTER);

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        internalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        internalPanel.setName("internalPanel"); // NOI18N
        internalPanel.setLayout(new javax.swing.BoxLayout(internalPanel, javax.swing.BoxLayout.Y_AXIS));

        sourcePanel.setName("sourcePanel"); // NOI18N
        sourcePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(LoadFromFilePanel.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        jLabel1.setPreferredSize(new java.awt.Dimension(70, 14));
        sourcePanel.add(jLabel1);

        filenameTextfield.setColumns(20);
        filenameTextfield.setText(resourceMap.getString("filenameTextfield.text")); // NOI18N
        filenameTextfield.setName("filenameTextfield"); // NOI18N
        sourcePanel.add(filenameTextfield);

        browseButton.setText(resourceMap.getString("browseButton.text")); // NOI18N
        browseButton.setName("browseButton"); // NOI18N
        sourcePanel.add(browseButton);

        internalPanel.add(sourcePanel);

        formatPanel.setName("formatPanel"); // NOI18N
        formatPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        formatLabel.setText(resourceMap.getString("formatLabel.text")); // NOI18N
        formatLabel.setName("formatLabel"); // NOI18N
        formatLabel.setPreferredSize(new java.awt.Dimension(70, 14));
        formatPanel.add(formatLabel);

        formatCombobox.setName("formatCombobox"); // NOI18N
        formatPanel.add(formatCombobox);

        helpDataFormatButton.setIcon(resourceMap.getIcon("helpDataFormatButton.icon")); // NOI18N
        helpDataFormatButton.setText(resourceMap.getString("helpDataFormatButton.text")); // NOI18N
        helpDataFormatButton.setName("helpDataFormatButton"); // NOI18N
        formatPanel.add(helpDataFormatButton);

        internalPanel.add(formatPanel);

        jPanel1.add(internalPanel);

        add(jPanel1, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel additionalParametersPanel;
    private javax.swing.JButton browseButton;
    private javax.swing.JTextField filenameTextfield;
    private javax.swing.JComboBox formatCombobox;
    private javax.swing.JLabel formatLabel;
    private javax.swing.JPanel formatPanel;
    private javax.swing.JButton helpDataFormatButton;
    private javax.swing.JPanel internalPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane scrollpane;
    private javax.swing.JPanel sourcePanel;
    // End of variables declaration//GEN-END:variables

}
