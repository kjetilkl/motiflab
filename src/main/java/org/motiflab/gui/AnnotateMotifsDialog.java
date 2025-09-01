/*


 */

/*
 * AnnotateMotifsDialog.java
 *
 * Created on 06.sep.2010, 11:34:25
 */

package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.UpdateMotifsTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifClassification;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.dataformat.DataFormat;
import org.jdesktop.application.Action;

/**
 *
 * @author kjetikl
 */
public class AnnotateMotifsDialog extends javax.swing.JDialog {
    private MotifLabGUI gui;
    private final static String REPLACE_CURRENT="Replace current";
    private final static String MERGE_OR_REPLACE_CURRENT="Merge when possible else replace current";
    private final static String MERGE_OR_KEEP_CURRENT="Merge when possible else keep current";
    private final static String KEEP_CURRENT="Keep current";
    private final static String INTERACTIVE_MODE="Ask interactively";
    private final static String CANCEL="Cancel";
    private LoadFromFilePanel loadFromFilePanel;
    private HashMap<String,Motif> motiflist=new HashMap<String, Motif>(); // this holds the updated motifs that will replace the old motifs (with an UpdateMotifsTask)

    private ParseListContextMenu contextmenu;
    private String interactiveSelection=null;

    /** Creates new form AnnotateMotifsDialog */
    public AnnotateMotifsDialog(MotifLabGUI gui) {
        super(gui.getFrame(), true);
        setTitle("Update Motif Properties");
        this.gui=gui;
        initComponents();

        // fix accelerators
        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getActionMap(AnnotateMotifsDialog.class, this);
        javax.swing.Action ac=actionMap.get("promptUseNewValue");
        promptUseNewValueButton.getActionMap().put("promptUseNewValue", ac);
        promptUseNewValueButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('n'), "promptUseNewValue");
        ac=actionMap.get("promptKeepOldValue");
        promptKeepCurrentValueButton.getActionMap().put("promptKeepOldValue", ac);
        promptKeepCurrentValueButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('c'), "promptKeepOldValue");
        ac=actionMap.get("promptMergeOrReplaceCurrent");
        promptMergeOrUseNewButton.getActionMap().put("promptMergeOrReplaceCurrent", ac);
        promptMergeOrUseNewButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('m'), "promptMergeOrReplaceCurrent");
        ac=actionMap.get("promptMergeOrKeepCurrent");
        promptMergeOrKeepCurrentButton.getActionMap().put("promptMergeOrKeepCurrent", ac);
        promptMergeOrKeepCurrentButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('k'), "promptMergeOrKeepCurrent");

        String[] userdefined=Motif.getAllUserDefinedProperties(gui.getEngine());
        String[] standard=Motif.getAllStandardProperties(false);
        String[] knownproperties=new String[standard.length+userdefined.length];
        System.arraycopy(standard, 0, knownproperties, 0, standard.length); // leave first position blank
        System.arraycopy(userdefined, 0, knownproperties, standard.length, userdefined.length);
        Arrays.sort(knownproperties);
        DefaultComboBoxModel propertyModel=new DefaultComboBoxModel(knownproperties);
        DefaultComboBoxModel combineStrategyModel=new DefaultComboBoxModel(new String[]{MERGE_OR_REPLACE_CURRENT,MERGE_OR_KEEP_CURRENT,REPLACE_CURRENT, INTERACTIVE_MODE});
        propertyCombobox.setModel(propertyModel);
        propertyCombobox.setEditable(true);
        propertyCombobox.setSelectedItem(standard[0]);
        combineStrategyCombobox.setModel(combineStrategyModel);
        ArrayList<DataFormat> dataformats=gui.getEngine().getDataInputFormats(MotifCollection.class);
        loadFromFilePanel=new LoadFromFilePanel(dataformats,gui,MotifCollection.class);
        importCollectionTab.add(loadFromFilePanel,BorderLayout.CENTER);
        pack();
        contextmenu=new ParseListContextMenu();
        parseListTextArea.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) contextmenu.show(e.getComponent(),e.getX(),e.getY());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) contextmenu.show(e.getComponent(),e.getX(),e.getY());
            }
        });
        parseListTextArea.requestFocusInWindow();
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

        promptUserDialog = new javax.swing.JDialog();
        promptTop = new javax.swing.JPanel();
        promptOuter = new javax.swing.JPanel();
        promptMain = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        promptUserMotifIDField = new javax.swing.JTextField();
        promptUserPropertyField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        promptUserOldValueField = new javax.swing.JTextField();
        promptUserNewValueField = new javax.swing.JTextField();
        promptBottom = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        promptUseNewValueButton = new javax.swing.JButton();
        promptKeepCurrentValueButton = new javax.swing.JButton();
        promptMergeOrUseNewButton = new javax.swing.JButton();
        promptMergeOrKeepCurrentButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jButton4 = new javax.swing.JButton();
        topPanel = new javax.swing.JPanel();
        settingsPanel = new javax.swing.JPanel();
        propertyLabel = new javax.swing.JLabel();
        propertyCombobox = new javax.swing.JComboBox();
        combineStrategyLabel = new javax.swing.JLabel();
        combineStrategyCombobox = new javax.swing.JComboBox();
        mainPanel = new javax.swing.JPanel();
        tabPane = new javax.swing.JTabbedPane();
        parseListTab = new javax.swing.JPanel();
        scrollpane = new javax.swing.JScrollPane();
        parseListTextArea = new javax.swing.JTextArea();
        internal1 = new javax.swing.JPanel();
        parseListDescriptionLabel = new javax.swing.JLabel();
        importCollectionTab = new javax.swing.JPanel();
        buttonsPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        skipUnrecognizedCheckbox = new javax.swing.JCheckBox();
        useTFnames = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(AnnotateMotifsDialog.class);
        promptUserDialog.setTitle(resourceMap.getString("promptUserDialog.title")); // NOI18N
        promptUserDialog.setModal(true);
        promptUserDialog.setName("promptUserDialog"); // NOI18N

        promptTop.setName("promptTop"); // NOI18N
        promptUserDialog.getContentPane().add(promptTop, java.awt.BorderLayout.PAGE_START);

        promptOuter.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        promptOuter.setName("promptOuter"); // NOI18N
        promptOuter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 10));

        promptMain.setName("promptMain"); // NOI18N
        promptMain.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 20, 5);
        promptMain.add(jLabel1, gridBagConstraints);

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 20, 20, 5);
        promptMain.add(jLabel2, gridBagConstraints);

        promptUserMotifIDField.setColumns(16);
        promptUserMotifIDField.setEditable(false);
        promptUserMotifIDField.setText(resourceMap.getString("promptUserMotifIDField.text")); // NOI18N
        promptUserMotifIDField.setFocusable(false);
        promptUserMotifIDField.setName("promptUserMotifIDField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 20, 5);
        promptMain.add(promptUserMotifIDField, gridBagConstraints);

        promptUserPropertyField.setColumns(12);
        promptUserPropertyField.setEditable(false);
        promptUserPropertyField.setText(resourceMap.getString("promptUserPropertyField.text")); // NOI18N
        promptUserPropertyField.setFocusable(false);
        promptUserPropertyField.setName("promptUserPropertyField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 20, 5);
        promptMain.add(promptUserPropertyField, gridBagConstraints);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        promptMain.add(jLabel3, gridBagConstraints);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        promptMain.add(jLabel4, gridBagConstraints);

        promptUserOldValueField.setColumns(26);
        promptUserOldValueField.setEditable(false);
        promptUserOldValueField.setText(resourceMap.getString("promptUserOldValueField.text")); // NOI18N
        promptUserOldValueField.setFocusable(false);
        promptUserOldValueField.setName("promptUserOldValueField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        promptMain.add(promptUserOldValueField, gridBagConstraints);

        promptUserNewValueField.setColumns(26);
        promptUserNewValueField.setEditable(false);
        promptUserNewValueField.setText(resourceMap.getString("promptUserNewValueField.text")); // NOI18N
        promptUserNewValueField.setFocusable(false);
        promptUserNewValueField.setName("promptUserNewValueField"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        promptMain.add(promptUserNewValueField, gridBagConstraints);

        promptOuter.add(promptMain);

        promptUserDialog.getContentPane().add(promptOuter, java.awt.BorderLayout.CENTER);

        promptBottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 0, 0));
        promptBottom.setName("promptBottom"); // NOI18N
        promptBottom.setLayout(new java.awt.BorderLayout());

        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getActionMap(AnnotateMotifsDialog.class, this);
        promptUseNewValueButton.setAction(actionMap.get("promptUseNewValue")); // NOI18N
        promptUseNewValueButton.setMnemonic('n');
        promptUseNewValueButton.setName("promptUseNewValueButton"); // NOI18N
        jPanel3.add(promptUseNewValueButton);

        promptKeepCurrentValueButton.setAction(actionMap.get("promptKeepOldValue")); // NOI18N
        promptKeepCurrentValueButton.setMnemonic('c');
        promptKeepCurrentValueButton.setName("promptKeepCurrentValueButton"); // NOI18N
        jPanel3.add(promptKeepCurrentValueButton);

        promptMergeOrUseNewButton.setAction(actionMap.get("promptMergeOrReplaceCurrent")); // NOI18N
        promptMergeOrUseNewButton.setMnemonic('m');
        promptMergeOrUseNewButton.setName("promptMergeOrUseNewButton"); // NOI18N
        jPanel3.add(promptMergeOrUseNewButton);

        promptMergeOrKeepCurrentButton.setAction(actionMap.get("promptMergeOrKeepCurrent")); // NOI18N
        promptMergeOrKeepCurrentButton.setMnemonic('k');
        promptMergeOrKeepCurrentButton.setName("promptMergeOrKeepCurrentButton"); // NOI18N
        jPanel3.add(promptMergeOrKeepCurrentButton);

        promptBottom.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButton4.setAction(actionMap.get("promptCancel")); // NOI18N
        jButton4.setName("jButton4"); // NOI18N
        jPanel4.add(jButton4);

        promptBottom.add(jPanel4, java.awt.BorderLayout.EAST);

        promptUserDialog.getContentPane().add(promptBottom, java.awt.BorderLayout.PAGE_END);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N

        topPanel.setName("topPanel"); // NOI18N
        topPanel.setLayout(new java.awt.BorderLayout());

        settingsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 5, 4, 5));
        settingsPanel.setName("settingsPanel"); // NOI18N
        settingsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING));

        propertyLabel.setText(resourceMap.getString("propertyLabel.text")); // NOI18N
        propertyLabel.setName("propertyLabel"); // NOI18N
        settingsPanel.add(propertyLabel);

        propertyCombobox.setName("propertyCombobox"); // NOI18N
        settingsPanel.add(propertyCombobox);

        combineStrategyLabel.setText(resourceMap.getString("combineStrategyLabel.text")); // NOI18N
        combineStrategyLabel.setName("combineStrategyLabel"); // NOI18N
        settingsPanel.add(combineStrategyLabel);

        combineStrategyCombobox.setName("combineStrategyCombobox"); // NOI18N
        settingsPanel.add(combineStrategyCombobox);

        topPanel.add(settingsPanel, java.awt.BorderLayout.SOUTH);

        getContentPane().add(topPanel, java.awt.BorderLayout.PAGE_START);

        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        tabPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        tabPane.setName("tabPane"); // NOI18N

        parseListTab.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        parseListTab.setName("parseListTab"); // NOI18N
        parseListTab.setLayout(new java.awt.BorderLayout());

        scrollpane.setName("scrollpane"); // NOI18N

        parseListTextArea.setColumns(20);
        parseListTextArea.setRows(5);
        parseListTextArea.setName("parseListTextArea"); // NOI18N
        scrollpane.setViewportView(parseListTextArea);

        parseListTab.add(scrollpane, java.awt.BorderLayout.CENTER);

        internal1.setAlignmentY(0.0F);
        internal1.setMinimumSize(new java.awt.Dimension(96, 70));
        internal1.setName("internal1"); // NOI18N
        internal1.setPreferredSize(new java.awt.Dimension(363, 70));
        internal1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING));

        parseListDescriptionLabel.setText(resourceMap.getString("parseListDescriptionLabel.text")); // NOI18N
        parseListDescriptionLabel.setMinimumSize(new java.awt.Dimension(86, 62));
        parseListDescriptionLabel.setName("parseListDescriptionLabel"); // NOI18N
        parseListDescriptionLabel.setPreferredSize(new java.awt.Dimension(736, 62));
        internal1.add(parseListDescriptionLabel);

        parseListTab.add(internal1, java.awt.BorderLayout.PAGE_START);

        tabPane.addTab(resourceMap.getString("parseListTab.TabConstraints.tabTitle"), parseListTab); // NOI18N

        importCollectionTab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        importCollectionTab.setName("importCollectionTab"); // NOI18N
        importCollectionTab.setLayout(new java.awt.BorderLayout());
        tabPane.addTab(resourceMap.getString("importCollectionTab.TabConstraints.tabTitle"), importCollectionTab); // NOI18N

        mainPanel.add(tabPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setMinimumSize(new java.awt.Dimension(100, 40));
        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(400, 40));
        buttonsPanel.setLayout(new java.awt.BorderLayout());

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        skipUnrecognizedCheckbox.setText(resourceMap.getString("skipUnrecognizedCheckbox.text")); // NOI18N
        skipUnrecognizedCheckbox.setToolTipText(resourceMap.getString("skipUnrecognizedCheckbox.toolTipText")); // NOI18N
        skipUnrecognizedCheckbox.setName("skipUnrecognizedCheckbox"); // NOI18N
        jPanel2.add(skipUnrecognizedCheckbox);

        useTFnames.setText(resourceMap.getString("useTFnames.text")); // NOI18N
        useTFnames.setToolTipText(resourceMap.getString("useTFnames.toolTipText")); // NOI18N
        useTFnames.setName("useTFnames"); // NOI18N
        jPanel2.add(useTFnames);

        buttonsPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

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
        jPanel1.add(okButton);

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
        jPanel1.add(cancelButton);

        buttonsPanel.add(jPanel1, java.awt.BorderLayout.EAST);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonPressed
        setVisible(false);
    }//GEN-LAST:event_cancelButtonPressed

    private void okButtonPressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonPressed
        String property=(String)propertyCombobox.getSelectedItem();
        String strategy=(String)combineStrategyCombobox.getSelectedItem();
        if (tabPane.getSelectedComponent()==importCollectionTab) {
            MotifCollection collection=null;
            try {
                String filename=loadFromFilePanel.getFilename();
                if (filename==null) throw new ExecutionError("Missing filename");
                collection=(MotifCollection)loadFromFilePanel.loadData(null,MotifCollection.getType());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "An error occurred while importing collection from file:\n"+e.getClass().getSimpleName()+":"+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                processCollection(collection, strategy);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "An error occurred while processing Motif Collection:\n"+e.getMessage(),"Parse Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if (tabPane.getSelectedComponent()==parseListTab) {
           try {
             if (property.isEmpty()) throw new ExecutionError("Please select a property");
             String text=parseListTextArea.getText();
             String[] lines=text.split("\n");
             boolean ok=parseList(lines,property, strategy);
             if (!ok) return;
           } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "An error occurred while parsing list:\n"+e.getMessage(),"Parse Error",JOptionPane.ERROR_MESSAGE);
                return;
           }
        }
        // create task and launch it
        UpdateMotifsTask task=new UpdateMotifsTask(gui.getEngine());
        for (Motif motif:motiflist.values()) {
            task.addMotif(motif);
        }
        gui.launchOperationTask(task, false);
        setVisible(false);
    }//GEN-LAST:event_okButtonPressed


    /** Merges properties of the current motifs with another collection imported from file */
    private void processCollection(MotifCollection collection, String strategy) throws ExecutionError {
        List<Motif> list=collection.getPayload();
        for (Motif motif:list) {
            String motifname=motif.getName();
            Motif[] temp=null;
            try {
               temp=getMotif(motifname);
            } catch (ExecutionError gme) { // this happens if the motifname is not a recognized ID
               if (skipUnrecognizedCheckbox.isSelected()) continue;
               else throw gme;
            }
            Motif originalMotif=temp[0];
            Motif updatedMotif=temp[1];
            try {
                if (strategy.equals(REPLACE_CURRENT)) {
                    String shortname=motif.getShortName();
                    if (shortname!=null && !shortname.isEmpty()) updatedMotif.setShortName(shortname);
                    String longname=motif.getLongName();
                    if (longname!=null && !longname.isEmpty()) updatedMotif.setLongName(longname);
                    String classification=motif.getClassification();
                    if (classification!=null && !classification.isEmpty()) updatedMotif.setClassification(classification);
                    int quality=motif.getQuality();
                    if (quality!=6) updatedMotif.setQuality(quality);
                    String part=motif.getPart();
                    if (part!=null && !part.equals(Motif.FULL)) updatedMotif.setPart(part);
                    String factors=motif.getBindingFactors();
                    if (factors!=null && !factors.isEmpty()) updatedMotif.setBindingFactors(factors);
                    String organisms=motif.getOrganisms();
                    if (organisms!=null && !organisms.isEmpty()) updatedMotif.setOrganisms(organisms);
                    ArrayList<String> alternatives=motif.getKnownDuplicatesNames();
                    String[] namelist=new String[alternatives.size()];
                    if (alternatives!=null && !alternatives.isEmpty()) updatedMotif.setKnownDuplicatesNames(alternatives.toArray(namelist));
                    ArrayList<String> interaction=motif.getInteractionPartnerNames();
                    namelist=new String[interaction.size()];
                    if (interaction!=null && !interaction.isEmpty()) updatedMotif.setInteractionPartnerNames(interaction.toArray(namelist));
                    ArrayList<String> tissues=motif.getTissueExpressionAsStringArray();
                    namelist=new String[tissues.size()];
                    if (tissues!=null && !tissues.isEmpty()) updatedMotif.setTissueExpression(tissues.toArray(namelist));
                    for (String userproperty:motif.getUserDefinedProperties()) {
                        Object value=motif.getUserDefinedPropertyValue(userproperty);
                        if (value instanceof ArrayList) value=((ArrayList)value).clone();
                        updatedMotif.setUserDefinedPropertyValue(userproperty, value);
                    }
                    double[][] matrix=motif.getMatrix();
                    if (matrix!=null) updatedMotif.setMatrix(matrix);
                }
                else if (strategy.equals(MERGE_OR_REPLACE_CURRENT)) {
                    String shortname=motif.getShortName();
                    if (shortname!=null && !shortname.isEmpty()) updatedMotif.setShortName(shortname);
                    String longname=motif.getLongName();
                    if (longname!=null && !longname.isEmpty()) updatedMotif.setLongName(longname);
                    String classification=motif.getClassification();
                    if (classification!=null && !classification.isEmpty()) updatedMotif.setClassification(classification);
                    int quality=motif.getQuality();
                    if (quality!=6) updatedMotif.setQuality(quality);
                    String part=motif.getPart();
                    if (part!=null && !part.equals(Motif.FULL)) updatedMotif.setPart(part);

                    String factors=motif.getBindingFactors();
                    if (factors!=null && !factors.isEmpty())  mergeOrReplaceCurrentProperty(originalMotif,updatedMotif, "Factors", factors);
                    String organisms=motif.getOrganisms();
                    if (organisms!=null && !organisms.isEmpty()) mergeOrReplaceCurrentProperty(originalMotif,updatedMotif, "Organisms", organisms);

                    ArrayList<String> alternatives=motif.getKnownDuplicatesNames();
                    ArrayList<String> alternativesOriginal=originalMotif.getKnownDuplicatesNames();
                    if (alternatives!=null && !alternatives.isEmpty()) updatedMotif.setKnownDuplicatesNames(mergeStringLists(alternativesOriginal, alternatives));

                    ArrayList<String> interactions=motif.getInteractionPartnerNames();
                    ArrayList<String> interactionsOriginal=originalMotif.getInteractionPartnerNames();
                    if (interactions!=null && !interactions.isEmpty()) updatedMotif.setInteractionPartnerNames(mergeStringLists(interactionsOriginal, interactions));

                    ArrayList<String> tissues=motif.getTissueExpressionAsStringArray();
                    ArrayList<String> tissuesOriginal=originalMotif.getTissueExpressionAsStringArray();
                    if (tissues!=null && !tissues.isEmpty()) updatedMotif.setTissueExpression(mergeStringLists(tissuesOriginal, tissues));

                    double[][] matrix=motif.getMatrix();
                    if (matrix!=null) updatedMotif.setMatrix(matrix);

                    for (String userproperty:motif.getUserDefinedProperties()) {
                       Class type=Motif.getClassForUserDefinedProperty(userproperty, gui.getEngine());
                       if (type!=null && (type.equals(ArrayList.class) || type.equals(String.class))) { // can the new properties be merged?
                           String merged=mergeStringsLists( (String)originalMotif.getUserDefinedPropertyValueAsType(userproperty,String.class), (String)motif.getUserDefinedPropertyValueAsType(userproperty,String.class));
                           String[] mergedSplit=merged.split("\\s*,\\s*");
                           ArrayList<String> value=new ArrayList<String>(mergedSplit.length);
                           value.addAll(Arrays.asList(merged));
                           updatedMotif.setUserDefinedPropertyValue(userproperty, value);
                       } else updatedMotif.setUserDefinedPropertyValue(userproperty, motif.getUserDefinedPropertyValue(userproperty)); // the property can not be merged
                    }

                }
                else if (strategy.equals(MERGE_OR_KEEP_CURRENT)) {
                    String shortname=motif.getShortName();
                    String originalshortname=originalMotif.getShortName();
                    if (shortname!=null && !shortname.isEmpty() && (originalshortname==null || originalshortname.isEmpty())) updatedMotif.setShortName(shortname);
                    String longname=motif.getLongName();
                    String originallongname=originalMotif.getLongName();
                    if (longname!=null && !longname.isEmpty() && (originallongname==null || originallongname.isEmpty())) updatedMotif.setLongName(longname);
                    String classification=motif.getClassification();
                    String originalclassification=originalMotif.getClassification();
                    if (classification!=null && !classification.isEmpty() && (originalclassification==null || originalclassification.isEmpty())) updatedMotif.setClassification(classification);
//                    int quality=motif.getQuality();
//                    if (quality!=6) updatedMotif.setQuality(quality);
//                    String part=motif.getPart();
//                    if (part!=null && !part.equals(Motif.FULL)) updatedMotif.setPart(part);
                    String factors=motif.getBindingFactors();
                    if (factors!=null && !factors.isEmpty())  mergeOrKeepCurrentProperty(originalMotif,updatedMotif, "Factors", factors);
                    String organisms=motif.getOrganisms();
                    if (organisms!=null && !organisms.isEmpty()) mergeOrKeepCurrentProperty(originalMotif,updatedMotif, "Organisms", organisms);

                    ArrayList<String> alternatives=motif.getKnownDuplicatesNames();
                    ArrayList<String> alternativesOriginal=originalMotif.getKnownDuplicatesNames();
                    if (alternatives!=null && !alternatives.isEmpty()) updatedMotif.setKnownDuplicatesNames(mergeStringLists(alternativesOriginal, alternatives));

                    ArrayList<String> interactions=motif.getInteractionPartnerNames();
                    ArrayList<String> interactionsOriginal=originalMotif.getInteractionPartnerNames();
                    if (interactions!=null && !interactions.isEmpty()) updatedMotif.setInteractionPartnerNames(mergeStringLists(interactionsOriginal, interactions));

                    ArrayList<String> tissues=motif.getTissueExpressionAsStringArray();
                    ArrayList<String> tissuesOriginal=originalMotif.getTissueExpressionAsStringArray();
                    if (tissues!=null && !tissues.isEmpty()) updatedMotif.setTissueExpression(mergeStringLists(tissuesOriginal, tissues));

                    if (originalMotif.getMatrix()==null) {
                        double[][] matrix=motif.getMatrix();
                        if (matrix!=null) updatedMotif.setMatrix(matrix);
                    }

                    for (String userproperty:motif.getUserDefinedProperties()) {
                       Class type=Motif.getClassForUserDefinedProperty(userproperty, gui.getEngine());
                       if (type!=null && (type.equals(ArrayList.class) || type.equals(String.class))) { // can the new properties be merged?
                           String merged=mergeStringsLists( (String)originalMotif.getUserDefinedPropertyValueAsType(userproperty,String.class), (String)motif.getUserDefinedPropertyValueAsType(userproperty,String.class));
                           String[] mergedSplit=merged.split("\\s*,\\s*");
                           ArrayList<String> value=new ArrayList<String>(mergedSplit.length);
                           value.addAll(Arrays.asList(merged));
                           updatedMotif.setUserDefinedPropertyValue(userproperty, value);
                       } else updatedMotif.setUserDefinedPropertyValue(userproperty, originalMotif.getUserDefinedPropertyValue(userproperty)); // the property can not be merged
                    }
                }
            } catch (ExecutionError e) {
               throw new ExecutionError("ParseError for motif '"+motifname+"'.\n"+e.getMessage());
           }
        }
    }

    /** Parses a list of properties entered by the user in a dialog textbox */
    private boolean parseList(String[] lines, String property, String strategy) throws ExecutionError {
        int counter=0;
        boolean skipUnrecognized=skipUnrecognizedCheckbox.isSelected();
        boolean useTFNames=useTFnames.isSelected();
        ArrayList<Data> allMotifs=(useTFNames)?gui.getEngine().getAllDataItemsOfType(Motif.class):null;
        for (String line:lines) {
           counter++;
           line=line.trim();
           String useproperty="";
           if (line.isEmpty() || line.startsWith("#")) continue;
           String[] parts=line.split("\t|=");
           if (parts.length<2 || parts.length>3) continue;// throw new ExecutionError("Unable to parse expected key-value pair at line "+counter+": "+line);
           String motifname=parts[0].trim();
           useproperty=(parts.length==3)?parts[1]:property;
           String value=parts[parts.length-1].trim(); // the value is the last column
           if (useTFNames) { // the TF names could match several motifs
               ArrayList<Motif[]> list;
               try {
                   list=getMatchingMotifs(motifname,allMotifs);
               } catch (ExecutionError gme) { // this happens if the motifname is not a recognized ID
                  if (skipUnrecognized) continue;
                  else throw gme;
               }
               for (Motif[] temp:list) {
                   Motif originalMotif=temp[0];
                   Motif updatedMotif=temp[1];
                   try {
                        String mode=strategy;
                            if (mode.equals(INTERACTIVE_MODE)) mode=promptUser(originalMotif, useproperty, value);
                            if (mode.equals(CANCEL)) return false;
                            if (mode.equals(REPLACE_CURRENT)) replaceProperty(updatedMotif, useproperty, value);
                       else if (mode.equals(MERGE_OR_REPLACE_CURRENT)) mergeOrReplaceCurrentProperty(originalMotif,updatedMotif, useproperty, value);
                       else if (mode.equals(MERGE_OR_KEEP_CURRENT)) mergeOrKeepCurrentProperty(originalMotif,updatedMotif, useproperty, value);
                   } catch (ExecutionError e) {
                       throw new ExecutionError("ParseError on line "+counter+".\n"+e.getMessage());
                   }
               }
           } else { // just a single ID
               Motif[] temp;
               try {
                   temp=getMotif(motifname);
               } catch (ExecutionError gme) { // this happens if the motifname is not a recognized ID
                   if (skipUnrecognized) continue;
                   else throw gme;
               }
               Motif originalMotif=temp[0];
               Motif updatedMotif=temp[1];
               try {
                     String mode=strategy;
                        if (mode.equals(INTERACTIVE_MODE)) mode=promptUser(originalMotif, useproperty, value);
                        if (mode.equals(CANCEL)) return false;
                        if (mode.equals(REPLACE_CURRENT)) replaceProperty(updatedMotif, useproperty, value);
                   else if (mode.equals(MERGE_OR_REPLACE_CURRENT)) mergeOrReplaceCurrentProperty(originalMotif,updatedMotif, useproperty, value);
                   else if (mode.equals(MERGE_OR_KEEP_CURRENT)) mergeOrKeepCurrentProperty(originalMotif,updatedMotif, useproperty, value);
               } catch (ExecutionError e) {
                   throw new ExecutionError("ParseError on line "+counter+".\n"+e.getMessage());
               }
           }
       }
       return true;
    }

    private String promptUser(Motif original, String property, String newValue) {
         promptUserMotifIDField.setText(original.getPresentationName());
         promptUserPropertyField.setText(property);
         Object oldValue=null;
         try {oldValue=original.getPropertyValue(property, gui.getEngine());} catch (Exception e) {}
         if (oldValue instanceof List) oldValue=MotifLabEngine.splice((List)oldValue, ",");
         if (oldValue==null || oldValue.toString().isEmpty()) return REPLACE_CURRENT; // just update with the new value if the old is missing
         promptUserOldValueField.setText((oldValue!=null)?oldValue.toString():"");
         promptUserOldValueField.setToolTipText(MotifLabGUI.formatTooltipString((oldValue!=null)?oldValue.toString():"", 100));
         promptUserNewValueField.setText(newValue);
         promptUserNewValueField.setToolTipText(MotifLabGUI.formatTooltipString(newValue, 100));
         promptUserMotifIDField.setCaretPosition(0);
         promptUserOldValueField.setCaretPosition(0);
         promptUserNewValueField.setCaretPosition(0);
         promptUserDialog.pack();
         promptUserDialog.setLocation(gui.getFrame().getWidth()/2-promptUserDialog.getWidth()/2, gui.getFrame().getHeight()/2-promptUserDialog.getHeight()/2);
         promptUserDialog.setVisible(true);
         return interactiveSelection;
    }

    /** Updates a motif property by overwriting the old value */
    private void replaceProperty(Motif updated, String property, String newValue) throws ExecutionError {
        if (property.equals("Short name")) {
           updated.setShortName(newValue);
       } else if (property.equals("Long name")) {
           updated.setLongName(newValue);
       } else if (property.equals("Consensus")) {
           updated.setConsensusMotifAndUpdatePWM(newValue);
       } else if (property.equals("Factors")) {
           updated.setBindingFactors(newValue);
       } else if (property.equals("Classification")) {
           if (newValue.matches("[^0-9\\.]")) throw new ExecutionError("Incorrect class label: "+newValue);
           int level=MotifClassification.getClassLevel(newValue);
           while (level>4) {
               newValue=MotifClassification.getParentLevel(newValue);
               level=MotifClassification.getClassLevel(newValue);
           }
           if (!MotifClassification.isKnownClassString(newValue)) throw new ExecutionError("Unknown motif class:"+newValue);
           updated.setClassification(newValue);
       } else if (property.equals("Quality")) {
           try {
               int value=Integer.parseInt(newValue);
               if (value<1 || value>6) throw new ExecutionError("Quality should be a number between 1 and 6");
               updated.setQuality(value);
           } catch (NumberFormatException nfe) {
               throw new ExecutionError("Unable to parse expected numeric value for quality: "+newValue);
           }
       } else if (property.equals("Alternatives")) {
            String[] names=newValue.split("\\s*,\\s*");
            updated.setKnownDuplicatesNames(names);
       } else if (property.equals("Interactions")) {
            String[] names=newValue.split("\\s*,\\s*");
            updated.setInteractionPartnerNames(names);
       } else if (property.equals("Organisms")) {
            updated.setOrganisms(newValue);
       } else if (property.equals("Expression")) {
           String[] tissues=newValue.split("\\s*,\\s*");
           updated.setTissueExpression(tissues);
       } else if (property.equals("Part")) {
            if (!Motif.isValidPart(newValue)) throw new ExecutionError("'"+newValue+"' is not a valid value for the 'part' property");
            updated.setPart(newValue);
       } else {// userdefined property
           if (!Motif.isValidUserDefinedPropertyKey(property)) throw new ExecutionError("'"+property+"' is not a valid name for a user-defined property");
           if (newValue.contains(";")) throw new ExecutionError("Value can not contain the ';' character");
           Object value=Motif.getObjectForPropertyValueString(newValue);
           updated.setUserDefinedPropertyValue(property, value);
       }
    }

    /** Updates a motif property by merging with the old value (if it is possible, i.e. a list) else overwriting the old value */
    private void mergeOrReplaceCurrentProperty(Motif original, Motif updated, String property, String newValue) throws ExecutionError {
        if (property.equals("Short name") || property.equals("Long name") || property.equals("Classification") || property.equals("Part") || property.equals("Quality") || property.equals("Consensus")) { // these properties can not be merged
           replaceProperty(updated, property, newValue); // these properties can not be merged
       } else if (property.equals("Factors")) {
           String mergedList=mergeStringsLists(original.getBindingFactors(),newValue);
           updated.setBindingFactors(mergedList);
       } else if (property.equals("Alternatives")) {
            String[] names=newValue.split("\\s*,\\s*");
            String[] originalNames=new String[original.getKnownDuplicatesNames().size()];
            originalNames=original.getKnownDuplicatesNames().toArray(originalNames);
            String[] merged=mergeStringLists(names, originalNames);
            updated.setKnownDuplicatesNames(merged);
       } else if (property.equals("Interactions")) {
            String[] names=newValue.split("\\s*,\\s*");
            String[] originalNames=new String[original.getInteractionPartnerNames().size()];
            originalNames=original.getInteractionPartnerNames().toArray(originalNames);
            String[] merged=mergeStringLists(names, originalNames);
            updated.setInteractionPartnerNames(merged);
       } else if (property.equals("Organisms")) {
           String mergedList=mergeStringsLists(original.getOrganisms(),newValue);
           updated.setOrganisms(mergedList);
       } else if (property.equals("Expression")) {
           String[] tissues=newValue.split("\\s*,\\s*");
           String[] originalNames=new String[original.getTissueExpressionAsStringArray().size()];
           originalNames=original.getTissueExpressionAsStringArray().toArray(originalNames);
           String[] merged=mergeStringLists(tissues, originalNames);
           updated.setTissueExpression(merged);
       } else if (property.equals("Part")) {
            if (!Motif.isValidPart(newValue)) throw new ExecutionError("'"+newValue+"' is not a valid value for the 'part' property");
            updated.setPart(newValue);
       } else {
           if (!Motif.isValidUserDefinedPropertyKey(property)) throw new ExecutionError("'"+property+"' is not a valid name for a user-defined property");
           if (newValue.contains(";")) throw new ExecutionError("Value can not contain the ';' character");
           Class type=Motif.getClassForUserDefinedProperty(property, gui.getEngine());
           if (type!=null && (type.equals(ArrayList.class) || type.equals(String.class))) {
               String merged=mergeStringsLists( (String)original.getUserDefinedPropertyValueAsType(property,String.class), newValue);
               String[] mergedSplit=merged.split("\\s*,\\s*");
               ArrayList<String> value=new ArrayList<String>(mergedSplit.length);
               value.addAll(Arrays.asList(merged));
               updated.setUserDefinedPropertyValue(property, value);
           } else replaceProperty(updated, property, newValue); // the property can not be merged (or can it?)
       }
    }

    /** Updates a motif property by merging with the old value (if it is possible, i.e. a list) else keeping the old value */
    private void mergeOrKeepCurrentProperty(Motif original, Motif updated, String property, String newValue) throws ExecutionError {
        if (property.equals("Short name")) {
           String originalName=original.getShortName();
           if (originalName==null || originalName.isEmpty()) updated.setShortName(newValue); else updated.setShortName(originalName);
       } else if (property.equals("Long name")) {
           String originalName=original.getLongName();
           if (originalName==null || originalName.isEmpty()) updated.setLongName(newValue); else updated.setLongName(originalName);
       } else if (property.equals("Consensus")) {
           String originalConsensus=original.getConsensusMotif();
           if (originalConsensus==null || originalConsensus.isEmpty()) updated.setConsensusMotifAndUpdatePWM(newValue); else updated.setConsensusMotifAndUpdatePWM(originalConsensus);
       } else if (property.equals("Factors")) {
           String mergedList=mergeStringsLists(original.getBindingFactors(),newValue);
           updated.setBindingFactors(mergedList);
       } else if (property.equals("Classification")) {
           if (newValue.matches("[^0-9\\.]")) throw new ExecutionError("Incorrect class label: "+newValue);
           int level=MotifClassification.getClassLevel(newValue);
           while (level>4) {
               newValue=MotifClassification.getParentLevel(newValue);
               level=MotifClassification.getClassLevel(newValue);
           }
           if (!MotifClassification.isKnownClassString(newValue)) throw new ExecutionError("Unknown motif class:"+newValue);
           if (original.getClassification()==null) updated.setClassification(newValue);
       } else if (property.equals("Quality")) {
           try {
               int value=Integer.parseInt(newValue);
               if (value<1 || value>6) throw new ExecutionError("Quality should be a number between 1 and 6");
               if (original.getQuality()==6) updated.setQuality(value);
           } catch (NumberFormatException nfe) {
               throw new ExecutionError("Unable to parse expected numeric value for quality: "+newValue);
           }
       } else if (property.equals("Alternatives")) {
            String[] names=newValue.split("\\s*,\\s*");
            String[] originalNames=new String[original.getKnownDuplicatesNames().size()];
            originalNames=original.getKnownDuplicatesNames().toArray(originalNames);
            String[] merged=mergeStringLists(names, originalNames);
            updated.setKnownDuplicatesNames(merged);
       } else if (property.equals("Interactions")) {
            String[] names=newValue.split("\\s*,\\s*");
            String[] originalNames=new String[original.getInteractionPartnerNames().size()];
            originalNames=original.getInteractionPartnerNames().toArray(originalNames);
            String[] merged=mergeStringLists(names, originalNames);
            updated.setInteractionPartnerNames(merged);
       } else if (property.equals("Organisms")) {
           String mergedList=mergeStringsLists(original.getOrganisms(),newValue);
           updated.setOrganisms(mergedList);
       } else if (property.equals("Expression")) {
           String[] tissues=newValue.split("\\s*,\\s*");
           String[] originalNames=new String[original.getTissueExpressionAsStringArray().size()];
           originalNames=original.getTissueExpressionAsStringArray().toArray(originalNames);
           String[] merged=mergeStringLists(tissues, originalNames);
           updated.setTissueExpression(merged);
       } else if (property.equals("Part")) {
            if (!Motif.isValidPart(newValue)) throw new ExecutionError("'"+newValue+"' is not a valid value for the 'part' property");
            updated.setPart(newValue);
       } else {
           if (!Motif.isValidUserDefinedPropertyKey(property)) throw new ExecutionError("'"+property+"' is not a valid name for a user-defined property");
           if (newValue.contains(";")) throw new ExecutionError("Value can not contain the ';' character");
           Class type=Motif.getClassForUserDefinedProperty(property, gui.getEngine());
           if (type!=null && (type.equals(ArrayList.class) || type.equals(String.class))) {
               String merged=mergeStringsLists( (String)original.getUserDefinedPropertyValueAsType(property,String.class), newValue);
               String[] mergedSplit=merged.split("\\s*,\\s*");
               ArrayList<String> value=new ArrayList<String>(mergedSplit.length);
               value.addAll(Arrays.asList(merged));
               updated.setUserDefinedPropertyValue(property, value);
           } else updated.setUserDefinedPropertyValue(property, original.getUserDefinedPropertyValue(property)); // the property can not be merged (or can it?)
       }
    }

    /** Merges to strings containing comma-separated elements into a single string
     *  with no duplicate elements
     */
    private String mergeStringsLists(String string1, String string2) {
        if (string1==null || string1.isEmpty()) return string2;
        else if (string2==null || string2.isEmpty()) return string1;
        else {
            String[] elements1=string1.split("\\s*,\\s*");
            String[] elements2=string2.split("\\s*,\\s*");
            ArrayList<String> result=new ArrayList<String>(elements1.length);
            for (String e:elements1) result.add(e.trim());
            for (String e:elements2) if (!result.contains(e.trim())) result.add(e.trim());
            StringBuilder builder=new StringBuilder(string1.length()+string2.length());
            builder.append(result.get(0));
            for (int i=1;i<result.size();i++) {builder.append(",");builder.append(result.get(i));}
            return builder.toString();
        }
    }

     /** Merges two String[] lists into one, with duplicates removed */
     private String[] mergeStringLists(String[] stringlist1, String[] stringlist2) {
        if (stringlist1==null || stringlist1.length==0) return stringlist2;
        else if (stringlist2==null || stringlist2.length==0) return stringlist1;
        else {
            ArrayList<String> result=new ArrayList<String>(stringlist1.length);
            for (String e:stringlist1) result.add(e.trim());
            for (String e:stringlist2) if (!result.contains(e.trim())) result.add(e.trim());
            String[] resultlist=new String[result.size()];
            resultlist=result.toArray(resultlist);
            return resultlist;
        }
    }

     /** Merges two ArrayList<String> into one String[], with duplicates removed */
     private String[] mergeStringLists(ArrayList<String>stringlist1, ArrayList<String> stringlist2) {
        if (stringlist1==null || stringlist1.isEmpty()) {
            String[] resultlist=new String[stringlist2.size()];
            return stringlist2.toArray(resultlist);
        }
        else if (stringlist2==null || stringlist2.isEmpty()) {
            String[] resultlist=new String[stringlist1.size()];
            return stringlist1.toArray(resultlist);
        }
        else {
            ArrayList<String> result=new ArrayList<String>(stringlist1.size());
            for (String e:stringlist1) result.add(e.trim());
            for (String e:stringlist2) if (!result.contains(e.trim())) result.add(e.trim());
            String[] resultlist=new String[result.size()];
            resultlist=result.toArray(resultlist);
            return resultlist;
        }
    }

    /**
     * Returns a two-element array representing the motif with the given ID
     * The first element will be the original motif with the given name
     * The second element will be a motif-clone that will be used as buffer to update the original motif
     * (this clone will also be automatically added to the global HashMap named "motiflist" )
     */
    private Motif[] getMotif(String motifname) throws ExecutionError {
        Data data=gui.getEngine().getDataItem(motifname);
        if (data==null) throw new ExecutionError("Unknown data item: "+motifname);
        else if (!(data instanceof Motif)) throw new ExecutionError(motifname+" is not a motif");
        else {
            Motif motif=null;
            if (motiflist.containsKey(motifname)) {
                motif=motiflist.get(motifname);
            } else {
               motif=((Motif)data).clone();
               motiflist.put(motifname,motif);
            }
            return new Motif[]{(Motif)data,motif};
        }
    }

    private ArrayList<Motif[]> getMatchingMotifs(String TFname, ArrayList<Data> allMotifs) throws ExecutionError {
        ArrayList<Motif[]> list=new ArrayList<Motif[]>();
        for (Data data:allMotifs) {
            if (((Motif)data).matchesTF(TFname)) {
                String motifname=((Motif)data).getName();
                Motif motif=null;
                if (motiflist.containsKey(motifname)) {
                    motif=motiflist.get(motifname);
                } else {
                   motif=((Motif)data).clone();
                   motiflist.put(motifname,motif);
                }
                list.add(new Motif[]{(Motif)data,motif});
            }
        }
        return list;
    }



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox combineStrategyCombobox;
    private javax.swing.JLabel combineStrategyLabel;
    private javax.swing.JPanel importCollectionTab;
    private javax.swing.JPanel internal1;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel parseListDescriptionLabel;
    private javax.swing.JPanel parseListTab;
    private javax.swing.JTextArea parseListTextArea;
    private javax.swing.JPanel promptBottom;
    private javax.swing.JButton promptKeepCurrentValueButton;
    private javax.swing.JPanel promptMain;
    private javax.swing.JButton promptMergeOrKeepCurrentButton;
    private javax.swing.JButton promptMergeOrUseNewButton;
    private javax.swing.JPanel promptOuter;
    private javax.swing.JPanel promptTop;
    private javax.swing.JButton promptUseNewValueButton;
    private javax.swing.JDialog promptUserDialog;
    private javax.swing.JTextField promptUserMotifIDField;
    private javax.swing.JTextField promptUserNewValueField;
    private javax.swing.JTextField promptUserOldValueField;
    private javax.swing.JTextField promptUserPropertyField;
    private javax.swing.JComboBox propertyCombobox;
    private javax.swing.JLabel propertyLabel;
    private javax.swing.JScrollPane scrollpane;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JCheckBox skipUnrecognizedCheckbox;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JPanel topPanel;
    private javax.swing.JCheckBox useTFnames;
    // End of variables declaration//GEN-END:variables


 private class ParseListContextMenu extends JPopupMenu {
        public ParseListContextMenu() {
            JMenuItem menuitem;
            menuitem=new JMenuItem(gui.getApplication().getContext().getActionMap().get("cut"));
            menuitem.setIcon(null);
            this.add(menuitem);
            menuitem=new JMenuItem(gui.getApplication().getContext().getActionMap().get("copy"));
            menuitem.setIcon(null);
            this.add(menuitem);
            menuitem=new JMenuItem(gui.getApplication().getContext().getActionMap().get("paste"));
            menuitem.setIcon(null);
            this.add(menuitem);
        }
 }

    @Action
    public void promptUseNewValue() {
        interactiveSelection=REPLACE_CURRENT;
        promptUserDialog.setVisible(false);
    }

    @Action
    public void promptKeepOldValue() {
        interactiveSelection=KEEP_CURRENT;
        promptUserDialog.setVisible(false);
    }

    @Action
    public void promptCancel() {
        interactiveSelection=CANCEL;
        promptUserDialog.setVisible(false);
    }

    @Action
    public void promptMergeOrKeepCurrent() {
        interactiveSelection=MERGE_OR_KEEP_CURRENT;
        promptUserDialog.setVisible(false);
    }

    @Action
    public void promptMergeOrReplaceCurrent() {
        interactiveSelection=MERGE_OR_REPLACE_CURRENT;
        promptUserDialog.setVisible(false);
    }

}
