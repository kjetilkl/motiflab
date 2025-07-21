/*
 * ExternalProgramsDialog.java
 *
 * Created on 31. august 2009, 12:42
 */

package org.motiflab.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.SystemError;
import org.motiflab.external.ExternalProgram;

/**
 *
 * @author  kjetikl
 */
public class ExternalProgramsDialog extends javax.swing.JDialog {
    private static final String TABLECOLUMN_NAME="Name";
    private static final String TABLECOLUMN_TYPE="Type";
    private static final String TABLECOLUMN_DEFAULT="P";
    private static final String TABLECOLUMN_LOCATION="Location";

    private static final int COLUMN_PROGRAMNAME=0;
    private static final int COLUMN_DEFAULT=1;
    private static final int COLUMN_PROGRAMTYPE=2;
    private static final int COLUMN_LOCATION=3;


    private MotifLabGUI gui;
    private DefaultTableModel externalProgramsModel;
    private JTable externalProgramsTable;
    private Collection<ExternalProgram> externalProgramsList=null;
    private HashMap<String,String> typeDisplayName=new HashMap<String, String>(5);

    /** Creates new form ExternalProgramsDialog */
    public ExternalProgramsDialog(MotifLabGUI GUI) {
        super(GUI.getFrame(), true);
        this.gui=GUI;
        initComponents();
        getRootPane().setDefaultButton(okButton);
        typeDisplayName.put("MotifDiscovery", "Motif Discovery");
        typeDisplayName.put("MotifScanning", "Motif Scanning");
        typeDisplayName.put("ModuleDiscovery", "Module Discovery");
        typeDisplayName.put("ModuleScanning", "Module Scanning");
        typeDisplayName.put("EnsemblePrediction", "Ensemble Prediction");
        externalProgramsModel=new DefaultTableModel(new String[]{TABLECOLUMN_NAME,TABLECOLUMN_DEFAULT,TABLECOLUMN_TYPE,TABLECOLUMN_LOCATION},0);
        externalProgramsTable=new JTable(externalProgramsModel) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        externalProgramsList=gui.getEngine().getAllExternalPrograms();
        for (ExternalProgram program:externalProgramsList) {
            String type=program.getProgramClass();
            String location=program.getServiceType();
            if (!(location.equalsIgnoreCase("bundled") || location.equalsIgnoreCase("plugin"))) location+=" = "+program.getLocation();
            String isDefault=" ";
            String defaultForType=gui.getDefaultExternalProgram(type.toLowerCase());
            if (defaultForType!=null && defaultForType.equals(program.getName())) isDefault="\u2714";
            Object[] values=new Object[]{program.getName(),isDefault,type,location}; // the null is just a placeholder
            externalProgramsModel.addRow(values);
        }
        externalProgramsTable.setFillsViewportHeight(true);
        externalProgramsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        externalProgramsTable.setRowSelectionAllowed(true);
        externalProgramsTable.getTableHeader().setReorderingAllowed(false);
        externalProgramsTable.setRowHeight(18);
        scrollPane.setViewportView(externalProgramsTable);
        externalProgramsTable.getColumn(TABLECOLUMN_TYPE).setCellRenderer(new CellRenderer_ProgramType());
        externalProgramsTable.getColumn(TABLECOLUMN_TYPE).setPreferredWidth(50);
        externalProgramsTable.getColumn(TABLECOLUMN_DEFAULT).setPreferredWidth(22); // 18
        externalProgramsTable.getColumn(TABLECOLUMN_DEFAULT).setMinWidth(22);
        externalProgramsTable.getColumn(TABLECOLUMN_DEFAULT).setMaxWidth(22);
        externalProgramsTable.getColumn(TABLECOLUMN_DEFAULT).setCellRenderer(new CellRenderer_Preferred());


        externalProgramsTable.setAutoCreateRowSorter(true);
        externalProgramsTable.getRowSorter().toggleSortOrder(externalProgramsTable.getColumn(TABLECOLUMN_NAME).getModelIndex());
        externalProgramsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               super.mouseClicked(e);
               if (e.getClickCount()<2) return;
               int row=externalProgramsTable.getSelectedRow();
               String programname=(String)externalProgramsTable.getValueAt(row, COLUMN_PROGRAMNAME);
               ExternalProgram program=gui.getEngine().getExternalProgram(programname);
               showProgramInfo(program);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                   int row = externalProgramsTable.rowAtPoint(evt.getPoint()); //
                   if (row>=0 && !externalProgramsTable.isRowSelected(row)) externalProgramsTable.getSelectionModel().setSelectionInterval(row, row);
                   showContextMenu(evt);
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                   int row = externalProgramsTable.rowAtPoint(evt.getPoint()); //
                   if (row>=0 && !externalProgramsTable.isRowSelected(row)) externalProgramsTable.getSelectionModel().setSelectionInterval(row, row);
                   showContextMenu(evt);
                }
            }
        });
        externalProgramsTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_D || e.getKeyCode()==KeyEvent.VK_P) { // [D]efault or [P]referred
                   int row=externalProgramsTable.getSelectedRow();
                   if (row>=0) {
                      String programname=(String)externalProgramsTable.getValueAt(row, COLUMN_PROGRAMNAME);
                      String programtype=(String)externalProgramsTable.getValueAt(row, COLUMN_PROGRAMTYPE);
                      updateDefaultProgramInTable(programtype, programname);
                      gui.setDefaultExternalProgram(programtype, programname);
                      externalProgramsTable.repaint();
                   }
                }
            }
        });
        externalProgramsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row=externalProgramsTable.getSelectedRow();
                infoButton.setEnabled(row>=0);
            }
        });
        this.setPreferredSize(new Dimension(700,400));
        pack();
    }

    private void showContextMenu(java.awt.event.MouseEvent evt) {
        int row=externalProgramsTable.getSelectedRow();
        if (row<0) return;
        String programname=(String)externalProgramsTable.getValueAt(row, COLUMN_PROGRAMNAME);
        String programtype=(String)externalProgramsTable.getValueAt(row, COLUMN_PROGRAMTYPE);
        ContextMenu contextMenu=new ContextMenu(programname,programtype);
        contextMenu.show(evt.getComponent(), evt.getX(),evt.getY());
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
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        addProgramButton = new javax.swing.JButton();
        addProgramFromRepositoryButton = new javax.swing.JButton();
        infoButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(ExternalProgramsDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setMinimumSize(new java.awt.Dimension(200, 150));
        setName("Form"); // NOI18N

        jPanel1.setMaximumSize(new java.awt.Dimension(32767, 36));
        jPanel1.setMinimumSize(new java.awt.Dimension(100, 20));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(100, 20));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 621, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_START);

        jPanel2.setMinimumSize(new java.awt.Dimension(10, 30));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addProgramButton.setText(resourceMap.getString("addProgramButton.text")); // NOI18N
        addProgramButton.setInheritsPopupMenu(true);
        addProgramButton.setName("addProgramButton"); // NOI18N
        addProgramButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addProgramFromFile(evt);
            }
        });
        jPanel4.add(addProgramButton);

        addProgramFromRepositoryButton.setText(resourceMap.getString("addProgramFromRepositoryButton.text")); // NOI18N
        addProgramFromRepositoryButton.setName("addProgramFromRepositoryButton"); // NOI18N
        addProgramFromRepositoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addProgramFromRepository(evt);
            }
        });
        jPanel4.add(addProgramFromRepositoryButton);

        infoButton.setIcon(resourceMap.getIcon("infoButton.icon")); // NOI18N
        infoButton.setText(resourceMap.getString("infoButton.text")); // NOI18N
        infoButton.setToolTipText(resourceMap.getString("infoButton.toolTipText")); // NOI18N
        infoButton.setEnabled(false);
        infoButton.setName("infoButton"); // NOI18N
        infoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoButtonPressed(evt);
            }
        });
        jPanel4.add(infoButton);

        jPanel2.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setPreferredSize(new java.awt.Dimension(78, 37));
        jPanel5.setRequestFocusEnabled(false);
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okButton.setText(resourceMap.getString("okButton.text")); // NOI18N
        okButton.setMaximumSize(new java.awt.Dimension(68, 27));
        okButton.setMinimumSize(new java.awt.Dimension(68, 27));
        okButton.setName("okButton"); // NOI18N
        okButton.setPreferredSize(new java.awt.Dimension(68, 27));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonPressed(evt);
            }
        });
        jPanel5.add(okButton);

        jPanel2.add(jPanel5, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel2, java.awt.BorderLayout.PAGE_END);

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 1, 10));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.BorderLayout());

        scrollPane.setName("scrollPane"); // NOI18N
        jPanel3.add(scrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
    this.setVisible(false);

}//GEN-LAST:event_okButtonPressed

private void addProgramFromFile(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addProgramFromFile
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    final JFileChooser fc = gui.getFileChooser(null);// new JFileChooser(gui.getLastUsedDirectory());
    fc.setDialogTitle("Import external program definition file");
    FileNameExtensionFilter xmlFilter=new FileNameExtensionFilter("XML-files (*.xml)", "XML","xml");
    fc.addChoosableFileFilter(xmlFilter);
    fc.setFileFilter(xmlFilter);
    int returnValue=fc.showOpenDialog(this);
    setCursor(Cursor.getDefaultCursor());
    if (returnValue!=JFileChooser.APPROVE_OPTION) return; // user cancelled
    File file=fc.getSelectedFile();
    ExternalProgram program=null;
    try {
        program = ExternalProgram.initializeExternalProgramFromFile(file,true);
    } catch (SystemError ex) {
        JOptionPane.showMessageDialog(rootPane, ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    gui.setLastUsedDirectory(file.getParentFile());
    // the following line should not happen (but just in case...)
    if (program==null) {
        JOptionPane.showMessageDialog(rootPane, "Unable to load program definition file", "Import Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    addProgram(program);
}//GEN-LAST:event_addProgramFromFile

private void addProgramFromRepository(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addProgramFromRepository
    // TODO add your handling code here:
    ExternalProgramAddFromRepositoryDialog repositoryDialog=new ExternalProgramAddFromRepositoryDialog(gui);
    repositoryDialog.setLocation(gui.getFrame().getWidth()/2-repositoryDialog.getWidth()/2, gui.getFrame().getHeight()/2-repositoryDialog.getHeight()/2);
    repositoryDialog.setVisible(true);
    String programName=repositoryDialog.getProgramName();
    String urlString=repositoryDialog.getURL();
    repositoryDialog.dispose();
    if (urlString==null) return;
    ExternalProgram program=null;
    try {
        if (!urlString.startsWith("http")) urlString=gui.getEngine().getRepositoryURL()+urlString; // URL is relative to repository
        URL url=new URL(urlString);
        InputStream stream=url.openStream();
        program = ExternalProgram.initializeExternalProgramFromStream(stream,programName+".xml", true);
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(rootPane, ex.getClass().getSimpleName()+":\n"+ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    // the following line should not happen (but just in case...)
    if (program==null) {
        JOptionPane.showMessageDialog(rootPane, "Unable to load program definition file", "Import Error", JOptionPane.ERROR_MESSAGE);
        return;
    }
    addProgram(program);
}//GEN-LAST:event_addProgramFromRepository

private void infoButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoButtonPressed
       int row=externalProgramsTable.getSelectedRow();
       if (row<0) return;
       String programname=(String)externalProgramsTable.getValueAt(row, COLUMN_PROGRAMNAME);
       ExternalProgram program=gui.getEngine().getExternalProgram(programname);
       showProgramInfo(program);
}//GEN-LAST:event_infoButtonPressed



/** Adds a new program to the list. This method handles the common code for addProgramFromFile() and addProgramFromRepository()
 * @param program The program to be installed (already initiated from file)
 */
public void addProgram(ExternalProgram program) {
    String programName=program.getName();
    if (!program.MotifLabVersionOK()) {
       String programVersion=program.getRequiredMotifLabVersion();
       JOptionPane.showMessageDialog(rootPane, "<html>The program \""+programName+"\" requires a more recent version of MotifLab (version "+programVersion+").<br>The current version is "+MotifLabEngine.getVersion()+"</html>", "MotifLab version error", JOptionPane.ERROR_MESSAGE);
       return;
    }
    String license=(String)program.getProperty("license");
    if (license!=null && !license.trim().isEmpty()) {
        license="<html><body>"+license+"</body></html>";
        LicenseAgreementDialog dialog=new LicenseAgreementDialog(gui.getFrame(), programName+" license", license);
        dialog.setVisible(true);
        boolean isAccepted=dialog.isLicenseAccepted();
        dialog.dispose();
        if (!isAccepted) {
            JOptionPane.showMessageDialog(rootPane, "Installation aborted because license was not accepted", "Installation Aborted", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }
    String register=(String)program.getProperty("register");
    if (register!=null && !register.trim().isEmpty()) {
        try {
           URL url=new URL(register);
           int option=JOptionPane.showOptionDialog(ExternalProgramsDialog.this, "The authors of "+programName+" would like you to register on their web site.\nRegistering allows the authors to keep track of how many people\nare using their software, and it might also help them secure funding\nfor further research.", "Register Software", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Register Now","Proceed Without Registering"}, "Register Now");
           if (option==0) { // register
                java.awt.Desktop.getDesktop().browse(url.toURI());
           } else if (option==1) { // save configuration of PG
               gui.logMessage("Shame on you for not registering the software you use!");
           }
        }
        catch (MalformedURLException e) {}
        catch (IOException e) {gui.logMessage("Unable to launch web browser to register software..."); }
        catch (URISyntaxException e) {gui.logMessage("Unable to launch web browser to register software...");}
    }
    ExternalProgram removedProgram=null;
    if (gui.getEngine().getExternalProgram(programName)!=null) {
        int answer=JOptionPane.showConfirmDialog(rootPane, "There is already a program registered with the name '"+programName+"'.\nWould you like to replace the existing program?", "Warning", JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if (answer==JOptionPane.NO_OPTION) return;
        else {
            for (int i=0;i<externalProgramsModel.getRowCount();i++) {
               if (externalProgramsModel.getValueAt(i,COLUMN_PROGRAMNAME).equals(programName)) {
                   externalProgramsModel.removeRow(i);
                   removedProgram=gui.getEngine().removeExternalProgram(programName,false);
                   break;
               }
            }
        }
    }
    program.setEngine(gui.getEngine());
    // --- check that the location is known. If not prompt the user ---
    if (program.getServiceType().equalsIgnoreCase("local")) {
        boolean promptForLocation=true;
        if (program.getLocation()!=null && !program.getLocation().isEmpty()) {
            File executableFile=new File(program.getLocation());
            if (executableFile.exists()) promptForLocation=false;
        }
        if (promptForLocation) {
            String location=null;
            PromptLocationDialog promptLocationDialog=new PromptLocationDialog(gui, program);
            int height=promptLocationDialog.getHeight();
            int width=promptLocationDialog.getWidth();
            java.awt.Dimension size=gui.getFrame().getSize();
            promptLocationDialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
            promptLocationDialog.setVisible(true);
            location=promptLocationDialog.getExecutableLocation();
            promptLocationDialog.dispose();
            if (location==null) {
                if (removedProgram!=null) installProgramInListAndEngine(removedProgram); // put it back
                return; // cancel 'Add program'
            }
            else program.setLocation(location);
        } // end prompt for location
    } // serviceType == 'local'
    // first remove configuration file of the program that is being replaced (if any)
    if (removedProgram!=null && removedProgram.getConfigurationFile()!=null) {
         File oldfile=new File(removedProgram.getConfigurationFile());
         oldfile.delete();
    }
    File externalfile=new File(gui.getEngine().getMotifLabDirectory()+File.separator+"external"+File.separator+program.getName()+".xml");
    try {
        //gui.logMessage("saving to: "+externalfile.getAbsolutePath());
        program.saveConfigurationToFile(externalfile);
        if (program.getServiceType().equals("bundled") || program.getServiceType().equals("plugin")) { // If the external program is its own Java class (bundled) then change the object to that class instead of the generic superclass
            String classname=program.getLocation();
            if (classname==null || classname.isEmpty()) throw new SystemError("Missing class specification for "+program.getServiceType()+" program: "+program.getName());
            program=(ExternalProgram)Class.forName(program.getLocation()).newInstance();
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(rootPane, e.getClass().getSimpleName()+":"+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        if (e instanceof NullPointerException) e.printStackTrace(System.err);
        return;
    }
    program.setConfigurationFile(externalfile.getAbsolutePath());
    installProgramInListAndEngine(program);
    gui.logMessage("Added External Program: "+program.getName());
    JOptionPane.showMessageDialog(rootPane,"Program '"+program.getName()+"' installed successfully", "Installation complete", JOptionPane.INFORMATION_MESSAGE);
}



private void installProgramInListAndEngine(ExternalProgram program) {
    String type=program.getProgramClass();
    String locationDescription=program.getServiceType();
    if (!(locationDescription.equalsIgnoreCase("bundled") || locationDescription.equalsIgnoreCase("plugin"))) locationDescription+=" = "+program.getLocation();
    String isDefault=" "; // this is not default
    Object[] values=new Object[]{program.getName(),isDefault,type,locationDescription};
    externalProgramsModel.addRow(values);
    program.clearProperties();
    gui.getEngine().registerExternalProgram(program);
}

private void removeProgramInListAndEngine(ExternalProgram program) {
    String programName=program.getName();
    for (int i=0;i<externalProgramsModel.getRowCount();i++) {
       if (externalProgramsModel.getValueAt(i,COLUMN_PROGRAMNAME).equals(programName)) {
           externalProgramsModel.removeRow(i);
           break;
       }
    }
    gui.getEngine().removeExternalProgram(programName,true);
}

private void showProgramInfo(ExternalProgram externalprogram) {
    gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    String document=externalprogram.getProgramDocumentation();
    InfoDialog dialog=new InfoDialog(gui, "External Program", document);
    gui.getFrame().setCursor(Cursor.getDefaultCursor());
    dialog.setVisible(true);
    dialog.dispose();
}

private void updateDefaultProgramInTable(String programtype,String programname) {
    for (int i=0;i<externalProgramsModel.getRowCount();i++) {
        String type=(String)externalProgramsModel.getValueAt(i, COLUMN_PROGRAMTYPE);
        if (!type.equals(programtype)) continue;
        String rowname=(String)externalProgramsModel.getValueAt(i, COLUMN_PROGRAMNAME);
        if (rowname.equals(programname)) externalProgramsModel.setValueAt("\u2714", i, COLUMN_DEFAULT);
        else externalProgramsModel.setValueAt(" ", i, COLUMN_DEFAULT);
    }
}

private String getTypeDisplayName(String type) {
   if (typeDisplayName.containsKey((String)type)) return (typeDisplayName.get((String)type));
   else return type;
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addProgramButton;
    private javax.swing.JButton addProgramFromRepositoryButton;
    private javax.swing.JButton infoButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JButton okButton;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    private class CellRenderer_ProgramType extends DefaultTableCellRenderer {
        public CellRenderer_ProgramType() {
           super();
        }
        @Override
        public void setValue(Object value) {
           setText(getTypeDisplayName((String)value));
        }
    }
    private class CellRenderer_Preferred extends DefaultTableCellRenderer {
        public CellRenderer_Preferred() {
           super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c=super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value.equals(" ")) ((JLabel)c).setToolTipText(null);
            else {
                String name=(String)table.getValueAt(row, COLUMN_PROGRAMNAME);
                String type=(String)table.getValueAt(row, COLUMN_PROGRAMTYPE);
                ((JLabel)c).setToolTipText("<html><b>"+name+"</b> is the currently preferred program for "+getTypeDisplayName(type)+"</html>");
            }
            return c;
        }

    }

    private class ContextMenu extends JPopupMenu {
        String selectedProgramName=null;
        String selectedProgramType=null;

        public ContextMenu(String selectedProgramName,String selectedProgramType) {
            this.selectedProgramName=selectedProgramName;
            this.selectedProgramType=selectedProgramType;
            ContextMenuListener listener=new ContextMenuListener();
            JMenuItem showinfo=new JMenuItem("Show information");
            String displayType=typeDisplayName.get(selectedProgramType);
            if (displayType==null) displayType=selectedProgramType;
            JMenuItem setPreferred=new JMenuItem("Set \""+selectedProgramName+"\" as preferred "+displayType+" program");
            JMenuItem removeProgram=new JMenuItem("Remove \""+selectedProgramName+"\"");
            showinfo.addActionListener(listener);
            setPreferred.addActionListener(listener);
            removeProgram.addActionListener(listener);
            this.add(showinfo);
            this.add(setPreferred);
            this.add(removeProgram);
        }

        private class ContextMenuListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command=e.getActionCommand();
                if (command.equals("Show information")) {
                    ExternalProgram program=gui.getEngine().getExternalProgram(selectedProgramName);
                    if (program!=null) showProgramInfo(program);
                } else if (command.startsWith("Set")) {
                   updateDefaultProgramInTable(selectedProgramType, selectedProgramName);
                   gui.setDefaultExternalProgram(selectedProgramType, selectedProgramName);
                   externalProgramsTable.repaint();
                } else if (command.startsWith("Remove")) {
                    ExternalProgram program=gui.getEngine().getExternalProgram(selectedProgramName);
                    if (program.getServiceType().equals("plugin")) {
                        JOptionPane.showMessageDialog(rootPane, "To remove plugins, go to the Plugins configuration dialog available from the Configure menu", "", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        int option=JOptionPane.showConfirmDialog(rootPane, "Are you sure you want to remove '"+selectedProgramName+"' ?", "Remove external program", JOptionPane.YES_NO_OPTION);
                        if (option==JOptionPane.YES_OPTION && program!=null) {
                            removeProgramInListAndEngine(program);
                        }
                    }
                }
            }

        }

    }



}
