/*


 */

package motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.*;
import motiflab.engine.SystemError;
import motiflab.engine.dataformat.DataFormat;
import motiflab.engine.operations.Operation_new;
import motiflab.gui.ColorMenu;
import motiflab.gui.ColorMenuListener;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.GenericMotifBrowserPanel;
import motiflab.gui.GenericMotifBrowserPanelContextMenu;
import motiflab.gui.LoadFromFilePanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;


/**
 *
 * @author kjetikl
 */
public class Prompt_MotifPartition extends Prompt {

    private MotifPartition partition;

    private JPanel manualEntryPanel;
    private JPanel parseListPanel;
    private JPanel fromMapPanel;
    private JPanel fromPropertyPanel;
    private LoadFromFilePanel importFromFilePanel;
    private PartitionPiePanel displayPanel;    
    private JTabbedPane tabbedPanel;

    private JTable partitionTable;
    private DefaultTableModel partitionTableModel;
    private JLabel errorLabel;
    private JCheckBox ignoreParseErrors;
    private JCheckBox resolveInProtocol;
    private JTextArea parseListTextArea;

    private JComboBox fromMapSelection;
    private JComboBox fromMapOperator;
    private JComboBox fromPropertyCombobox;
    private JComboBox fromMapFirstOperandCombobox;
    private JComboBox fromMapSecondOperandCombobox;
    private JLabel fromMapToLabel;
    private JList fromMapList;
    private DefaultListModel fromMapListModel;
    private JButton fromMapAddClusterButton;
    private JButton fromMapRemoveClusterButton;
    private JTextField fromMapClusterNameTextField;
    private ContextMenu contextMenu;
    
    private final static int COLUMN_COLOR=0;
    private final static int COLUMN_MOTIF_NAME=1;
    private final static int COLUMN_CLUSTER=2;
    private final static int COLUMN_LOGO=3;
    

    private final String UNASSIGNED=" * * *  UNASSIGNED  * * *";

    private boolean showExisting=false;
    private boolean isimported=false;
    private boolean isModal=false;
    
    private VisualizationSettings settings;

    public Prompt_MotifPartition(MotifLabGUI gui, String prompt, MotifPartition dataitem) {
        this(gui,prompt,dataitem,true);
    }
    
    public Prompt_MotifPartition(MotifLabGUI gui, String prompt, MotifPartition dataitem, boolean modal) {
        super(gui,prompt, modal);
        this.isModal=modal;
        settings=gui.getVisualizationSettings();
        showExisting=(dataitem!=null);
        if (dataitem!=null)  {
            partition=dataitem;
            setExistingDataItem(dataitem);
        }
        else partition=new MotifPartition(gui.getGenericDataitemName(MotifPartition.class, null));
        setDataItemName(partition.getName());
        setTitle("Motif Partition");
        setupManualEntryPanel();
        setupImportModelPanel();
        setupParseListPanel();
        setupFromMapPanel();
        setupFromPropertyPanel();
        displayPanel=new PartitionPiePanel(partition, settings);
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Manual Entry", manualEntryPanel);
        tabbedPanel.addTab("From List", parseListPanel);
        tabbedPanel.addTab("From Map", fromMapPanel);
        tabbedPanel.addTab("From Property", fromPropertyPanel);
        tabbedPanel.addTab("Import", importFromFilePanel);
        tabbedPanel.addTab("Display", displayPanel);
        JPanel internal=new JPanel(new BorderLayout());
        Dimension size=new Dimension(580,500);
        internal.setMinimumSize(size);
        internal.setPreferredSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        parseListPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromMapPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        fromPropertyPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        importFromFilePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        displayPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        internal.add(tabbedPanel);
        setMainPanel(internal);
        pack();
        if (showExisting) focusOKButton();
    }

    private void setupManualEntryPanel() {
        ArrayList<Data> dataset=engine.getAllDataItemsOfType(Motif.class);
        int size=dataset.size();
        Object[][] rowData=new Object[dataset.size()][4];
        for (int i=0;i<size;i++) {
            Motif motif=(Motif)dataset.get(i);
            rowData[i][COLUMN_MOTIF_NAME]=motif.getPresentationName();
            rowData[i][COLUMN_LOGO]=motif.getName();
            if (partition.contains(motif.getName())) rowData[i][COLUMN_CLUSTER]=partition.getClusterForMotif(motif.getName());
            else rowData[i][COLUMN_CLUSTER]=null;          
        }
        partitionTableModel=new DefaultTableModel(rowData, new String[]{" ","Motif","Cluster","Logo"}) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case COLUMN_COLOR: return Color.class;                    
                    case COLUMN_MOTIF_NAME: return String.class;                       
                    case COLUMN_CLUSTER: return String.class;
                    case COLUMN_LOGO: return Motif.class;
                    default: return String.class;
                }
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return (column==COLUMN_CLUSTER && isDataEditable());
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (column==COLUMN_LOGO) {
                    String motifname=(String)super.getValueAt(row, column);
                    Data data=engine.getDataItem(motifname);
                    if (data instanceof Motif) return data;
                    else return null;
                }
                if (column==COLUMN_COLOR) {
                    String motifname=(String)super.getValueAt(row, COLUMN_LOGO);
                    return settings.getFeatureColor(motifname);
                }
                return super.getValueAt(row, column);
            }                    
        };
        GenericMotifBrowserPanel genericMotifPanel=new GenericMotifBrowserPanel(gui, partitionTableModel, isModal);
        genericMotifPanel.disableContextMenu();
        contextMenu=new ContextMenu(genericMotifPanel);
        partitionTable=genericMotifPanel.getTable();
        partitionTable.addMouseListener(new java.awt.event.MouseAdapter() { // adding new MouseListener to show my new ContextMenu
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {

            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {               
                int rowNumber = partitionTable.rowAtPoint(evt.getPoint());
                boolean isSelected=partitionTable.isCellSelected(rowNumber, COLUMN_CLUSTER);
                if (partitionTable.getSelectedRowCount()==0 || !isSelected) { // mouse does not point at one of the currently selected entries
		    partitionTable.getSelectionModel().setSelectionInterval( rowNumber, rowNumber );
                }
                if (evt.isPopupTrigger()) showContextMenu(evt);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) showContextMenu(evt);
            }
        });
        partitionTable.getColumn("Cluster").setCellRenderer(new CellRenderer_Cluster());
        partitionTable.getColumn("Cluster").setCellEditor(new ClusterNameValidatingCellEditor());
        partitionTable.getTableHeader().setReorderingAllowed(false);
        partitionTable.setAutoCreateRowSorter(true);
        partitionTable.getRowSorter().toggleSortOrder(COLUMN_CLUSTER);
        ExcelAdapter adapter=new ExcelAdapter(partitionTable, false, ExcelAdapter.CONVERT_NONE);    
        manualEntryPanel=new JPanel();
        manualEntryPanel.setLayout(new BorderLayout());
        manualEntryPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        manualEntryPanel.add(genericMotifPanel);
        errorLabel=new JLabel("  ");
        errorLabel.setFont(errorMessageFont);
        errorLabel.setForeground(java.awt.Color.RED);
        JPanel messagePanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        messagePanel.add(errorLabel);
        manualEntryPanel.add(messagePanel,BorderLayout.SOUTH);
    }

    private void setupImportModelPanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(MotifPartition.class);
        importFromFilePanel=new LoadFromFilePanel(dataformats,gui,MotifPartition.class);
    }

    private void setupParseListPanel() {
        parseListPanel=new JPanel();
        parseListPanel.setLayout(new BoxLayout(parseListPanel, BoxLayout.Y_AXIS));
        JPanel internal1=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal2=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel internal3=new JPanel(new BorderLayout());
        ignoreParseErrors=new JCheckBox("Ignore parse errors", false);
        resolveInProtocol=new JCheckBox("Resolve in protocol", false);
        resolveInProtocol.setToolTipText("<html>If selected, the list will be recorded verbatim in the protocol and references will be resolved when the protocol is executed.<br>If not selected, references will be resolved right away and the resulting list will be recorded in the protocol.<br></html>");
        JLabel label=new JLabel("<html>Enter assignment pairs below separated by newline or semicolon.<br>"
                + "Each pair should be in the format: Motif = Cluster<br>"
                + "The Motif field can contain a comma-separated list of names referring to motifs,<br>"
                + "collections or clusters within other motif partitions (<tt>PartitionName->ClusterName</tt>).<br>"
                + "If motif names contain numbered indices, you can specify a range of motifs using <br>"
                + "a colon operator to separate the first and last motif. For example, the entry \"<tt>ab3cd:ab7cd</tt>\" "
                + "will<br>refer to any motif starting with \"ab\" followed by a number between 3 and 7 and ending with \"cd\".<br>"                
                + "Wildcards (*) are allowed within motif names (except when specifying ranges).</html>");        
        parseListTextArea=new JTextArea();
        internal1.add(label);
        internal2.add(ignoreParseErrors);
        internal2.add(new JLabel("       "));
        internal2.add(resolveInProtocol);
        internal3.add(new JScrollPane(parseListTextArea));
        internal1.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 20));
        internal2.setBorder(BorderFactory.createEmptyBorder(4, 16, 5, 20));
        internal3.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        parseListPanel.add(internal1);
        parseListPanel.add(internal3);
        parseListPanel.add(internal2);
        if (showExisting && partition.isFromList()) {
            String list=partition.getFromListString();
            list=list.replace(";", "\n");
            parseListTextArea.setText(list);
            resolveInProtocol.setSelected(true);
        }
    }

    private void setupFromMapPanel() {
        fromMapPanel=new JPanel();
        fromMapPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        fromMapListModel=new DefaultListModel();
        fromMapList=new JList(fromMapListModel);
        //fromMapList.setVisibleRowCount(8);
        fromMapList.setPreferredSize(new Dimension(350,150));
        JScrollPane listscrollpane=new JScrollPane(fromMapList);
        JPanel gridpanel=new JPanel();
        gridpanel.setLayout(new GridBagLayout());
        gridpanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints constraints=new GridBagConstraints();
        constraints.anchor=GridBagConstraints.BELOW_BASELINE_LEADING;
        constraints.fill=GridBagConstraints.NONE;
        constraints.gridheight=1;//GridBagConstraints.RELATIVE;
        constraints.ipadx=10;
        //constraints.ipady=10;
        DefaultComboBoxModel model=new DefaultComboBoxModel();
        ArrayList<Data> mapslist=engine.getAllDataItemsOfType(MotifNumericMap.class);
        for (Data map:mapslist) {
            model.addElement(map.getName());
        }
        fromMapSelection=new JComboBox(model);
        //fromMapSelection.setPreferredSize(new Dimension(36,20));
        fromMapOperator=new JComboBox(new String[]{"=",">",">=","<","<=","<>","in"});
        //fromMapOperator.setPreferredSize(new Dimension(42,20));
        fromMapOperator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String operator=(String)fromMapOperator.getSelectedItem();
                boolean showSecondOperator=(operator.equals("in"));
                fromMapToLabel.setVisible(showSecondOperator);
                fromMapSecondOperandCombobox.setVisible(showSecondOperator);
            }
        });
        ArrayList<String> mapOperandValues=new ArrayList<String>();
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(NumericVariable.class));
        mapOperandValues.addAll(engine.getNamesForAllDataItemsOfType(MotifNumericMap.class));        
        fromMapFirstOperandCombobox=new JComboBox(mapOperandValues.toArray());
        fromMapFirstOperandCombobox.setEditable(true);
        fromMapFirstOperandCombobox.setSelectedItem("0");
        fromMapSecondOperandCombobox=new JComboBox(mapOperandValues.toArray());
        fromMapSecondOperandCombobox.setEditable(true);
        fromMapSecondOperandCombobox.setSelectedItem("1");
        fromMapClusterNameTextField=new JTextField(12);
        //fromMapSelection.setPreferredSize(new Dimension(120,20));
        fromMapToLabel=new JLabel("  to ");
        fromMapAddClusterButton=new JButton("Add");
        fromMapRemoveClusterButton=new JButton("Remove");
        fromMapRemoveClusterButton.setEnabled(false);
        fromMapAddClusterButton.setEnabled(fromMapSelection.getItemCount()>0);

        JPanel firstRow= new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel secondRow=new JPanel(new FlowLayout(FlowLayout.LEADING));
        JPanel thirdRow= new JPanel(new FlowLayout(FlowLayout.LEADING));

        firstRow.add(fromMapSelection);
        secondRow.add(fromMapOperator);
        secondRow.add(fromMapFirstOperandCombobox);
        secondRow.add(fromMapToLabel);
        secondRow.add(fromMapSecondOperandCombobox);
        thirdRow.add(fromMapClusterNameTextField);
        thirdRow.add(new JLabel("   "));
        thirdRow.add(fromMapAddClusterButton);
        thirdRow.add(new JLabel("   "));
        thirdRow.add(fromMapRemoveClusterButton);

        constraints.fill=GridBagConstraints.HORIZONTAL;
        constraints.gridwidth=2;
        constraints.gridy=0;
        constraints.gridx=0;
        //constraints.insets=new Insets(5, 0, 5, 0);
        gridpanel.add(listscrollpane,constraints);
        constraints.fill=GridBagConstraints.NONE;
        constraints.gridwidth=1;

        constraints.gridy=1;constraints.gridx=0;
        gridpanel.add(new JLabel("Map variable"),constraints);

        constraints.gridy=1;constraints.gridx=1;
        gridpanel.add(firstRow,constraints);

        constraints.gridy=2;constraints.gridx=0;
        gridpanel.add(new JLabel("Map value"),constraints);

        constraints.gridy=2;constraints.gridx=1;
        gridpanel.add(secondRow,constraints);

        constraints.gridy=3;constraints.gridx=0;
        gridpanel.add(new JLabel("Cluster name"),constraints);

        constraints.gridy=3;constraints.gridx=1;
        gridpanel.add(thirdRow,constraints);


        fromMapPanel.add(gridpanel);
        if (showExisting && partition.isFromMap()) { // initialize fields according to current partition configuration
            String fromMapString=partition.getFromMapString();
            String[] clusters=fromMapString.split("\\s*;\\s*");
            for (String cluster:clusters) {
                fromMapListModel.addElement(cluster);
            }
        }
        fromMapOperator.setSelectedItem(">=");
        fromMapList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Object[] selected=fromMapList.getSelectedValues();
                fromMapRemoveClusterButton.setEnabled(selected.length>0);
            }
        });
        fromMapRemoveClusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] selected=fromMapList.getSelectedValues();
                for (Object o:selected) {
                   fromMapListModel.removeElement(o);
                }
                fromMapRemoveClusterButton.setEnabled(false);
            }
        });
        fromMapAddClusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                   String clusterName=fromMapClusterNameTextField.getText();
                   if (clusterName==null) clusterName="";
                   clusterName=clusterName.trim();
                   if (!clusterName.isEmpty() && MotifPartition.isValidClusterName(clusterName)) {
                      String operator=(String)fromMapOperator.getSelectedItem();
                      String config=(String)fromMapSelection.getSelectedItem();
                      String firstOperandText=(String)fromMapFirstOperandCombobox.getSelectedItem();
                      if (firstOperandText!=null) firstOperandText=firstOperandText.trim();
                      if (operator.equals("in")) {
                          String secondOperandText=(String)fromMapSecondOperandCombobox.getSelectedItem();
                          if (secondOperandText!=null) secondOperandText=secondOperandText.trim();
                          config+=" in ["+firstOperandText+","+secondOperandText+"]:"+clusterName;
                      } else {
                          config+=operator+firstOperandText+":"+clusterName;
                      }
                      if (!fromMapListModel.contains(config)) fromMapListModel.addElement(config);
                   } else {
                       String errormsg=(clusterName.isEmpty())?"Please enter a cluster name":("Invalid cluster name: "+clusterName);
                       JOptionPane.showMessageDialog(Prompt_MotifPartition.this, errormsg,"Name error",JOptionPane.ERROR_MESSAGE);
                   }
                }
        });

    }


    private void setupFromPropertyPanel() {
        fromPropertyPanel=new JPanel();
        JPanel internal=new JPanel();
        internal.setLayout(new FlowLayout(FlowLayout.CENTER));
        internal.setBorder(BorderFactory.createEmptyBorder(50, 10, 20, 10));
        fromPropertyPanel.setLayout(new BorderLayout());
        fromPropertyCombobox=new JComboBox(MotifPartition.propertyNames);
        if (showExisting && partition.isFromProperty()) { // initialize fields according to current partition configuration
            fromPropertyCombobox.setSelectedItem(partition.getFromPropertyString());
        }
        internal.add(new JLabel("Cluster motifs according to  "));
        internal.add(fromPropertyCombobox);
        fromPropertyPanel.add(internal);
    }


    @Override
    public boolean onOKPressed() {
       if (tabbedPanel.getSelectedComponent()==importFromFilePanel) {
            try {
                String filename=importFromFilePanel.getFilename();
                if (filename==null) throw new SystemError("Missing filename");
                DataFormat format=importFromFilePanel.getDataFormat();
                ParameterSettings parametersettings=importFromFilePanel.getParameterSettings();
                partition=(MotifPartition)importFromFilePanel.loadData(partition,MotifPartition.getType());
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, parametersettings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing Motif Partition from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel) {
            for (int i=0;i<partitionTableModel.getRowCount();i++) {
                Motif rowmotif=(Motif)partitionTableModel.getValueAt(i, COLUMN_LOGO);
                if (rowmotif==null) continue; // this could happen if the user has deleted entries in the name column               
                String motifName=rowmotif.getName();
                String clusterName=(String)partitionTableModel.getValueAt(i, COLUMN_CLUSTER);
                if (clusterName!=null && (clusterName.trim().isEmpty() || clusterName.equals(UNASSIGNED))) clusterName=null;
                if (clusterName!=null && !MotifPartition.isValidClusterName(clusterName)) {
                    errorLabel.setText("Invalid cluster name:  "+clusterName);
                    int tableRow=partitionTable.convertRowIndexToView(i);
                    partitionTable.setRowSelectionInterval(tableRow, tableRow);
                    partitionTable.scrollRectToVisible(partitionTable.getCellRect(tableRow,0,true));
                    return false;
                }
                String oldcluster=partition.getClusterForMotif(motifName);
                if (oldcluster==null && clusterName==null) continue; // no old mapping and no new mapping
                else if(partition.contains(clusterName, motifName)) continue; // the same assignment already exists in cluster (no change)
                else if (!partition.contains(motifName) && clusterName!=null) { // add new motif to a cluster
                    Motif motif=(Motif)engine.getDataItem(motifName);
                    partition.addMotif(motif,clusterName);
                } else if (partition.contains(motifName) && clusterName==null) { // remove from the partition entirely
                    Motif motif=(Motif)engine.getDataItem(motifName);
                    partition.removeMotif(motif);
                } else { // move to a different partition
                    Motif motif=(Motif)engine.getDataItem(motifName);
                    partition.moveMotif(motif,clusterName);
                }
            }
        } else if (tabbedPanel.getSelectedComponent()==parseListPanel) {
            boolean reportErrors=!ignoreParseErrors.isSelected();
            String text=parseListTextArea.getText();
            text=text.replaceAll("\\n", ";");
            text=text.replaceAll(";+", ";");
            text=text.replaceAll("^;", ""); // remove leading semicolon
            text=text.replaceAll(";$", ""); // remove trailing semicolon
            ArrayList<String> notfound=new ArrayList<String>();
            MotifPartition parsedPartition=null;
            try {
                parsedPartition=MotifPartition.parseMotifPartitionParameters(text, partition.getName(), notfound, engine);
            } catch (Exception e) {} // exceptions will not be thrown when parameter (notfound != null)

            if (reportErrors) {
                if (notfound.size()>15) {
                    String errormsg=notfound.size()+" problems encountered:\n\n";
                    JOptionPane.showMessageDialog(this, errormsg, "Parsing error", JOptionPane.ERROR_MESSAGE);
                    return false;
                } else if (notfound.size()>0) {
                    String errormsg="Problems encountered with the following entries:\n\n";
                    for (int i=0;i<notfound.size()-1;i++) {
                         errormsg+=notfound.get(i)+"\n";
                    }
                    errormsg+=notfound.get(notfound.size()-1);
                    JOptionPane.showMessageDialog(this, errormsg, "Parsing error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else { // silent mode (only report errors in log) and return collection anyway
                for (String error:notfound) engine.logMessage(error);
            }
            if (parsedPartition!=null) partition.importData(parsedPartition);
            else partition.clearAll(engine);
            if (resolveInProtocol.isSelected()) partition.setFromListString(text);
        } else if (tabbedPanel.getSelectedComponent()==fromMapPanel) {
               Object[] configStrings=fromMapListModel.toArray();
               if (configStrings.length==0) partition.clearAll(engine);
               else {
                   StringBuilder configstring=new StringBuilder();
                   configstring.append(Operation_new.FROM_MAP_PREFIX);
                   for (int i=0;i<configStrings.length;i++) {
                       if (i<configStrings.length-1) {
                           configstring.append((String)configStrings[i]);
                           configstring.append(";");
                       }
                       else configstring.append((String)configStrings[i]);
                   }
                   try {
                       partition=MotifPartition.parseMotifPartitionParameters(configstring.toString(), partition.getName(), null, engine);
                   } catch (Exception e) {
                       JOptionPane.showMessageDialog(this, e.getMessage(), "Parsing error", JOptionPane.ERROR_MESSAGE);
                   }
               }
        } else if (tabbedPanel.getSelectedComponent()==fromPropertyPanel) {
               String selectedProperty=(String)fromPropertyCombobox.getSelectedItem();
               String configstring=Operation_new.FROM_PROPERTY_PREFIX+selectedProperty;
               try {
                   partition=MotifPartition.parseMotifPartitionParameters(configstring.toString(), partition.getName(), null, engine);
               } catch (Exception e) {
                   JOptionPane.showMessageDialog(this, e.getMessage(), "Parsing error", JOptionPane.ERROR_MESSAGE);
               }
        }
        String newName=getDataItemName();
        if (!partition.getName().equals(newName)) partition.rename(newName);
        return true;
    }

    @Override
    public Data getData() {
       return partition;
    }

    @Override
    public void setData(Data newdata) {
       if (newdata instanceof MotifPartition) partition=(MotifPartition)newdata; 
    }     
    
    public boolean isImported() {
        return isimported;
    }
    
    private void showContextMenu(java.awt.event.MouseEvent evt) {     
        contextMenu.updateMenu();
        contextMenu.show(evt.getComponent(), evt.getX(),evt.getY());
    }


private class ContextMenu extends GenericMotifBrowserPanelContextMenu implements ActionListener {
     JMenuItem addToExistingCluster=new JMenu("Add To Cluster");
     JMenuItem addToNewCluster=new JMenuItem("Add To New Cluster");
     JMenuItem removefrompartition=new JMenuItem("Remove From Partition");
     JMenuItem renamecluster=new JMenuItem("Rename ?");
     ColorMenu colorMenu;
     String oldname=null;
     
    public ContextMenu(GenericMotifBrowserPanel panel) {
          super(panel);
          ColorMenuListener colormenulistener=new ColorMenuListener() {
              public void newColorSelected(Color color) {
                 if (color!=null) {
                     gui.getVisualizationSettings().setClusterColor(oldname, color);
                     partitionTable.repaint();
                 }
              }
          };         
         colorMenu=new ColorMenu("Set Color For \""+oldname+"\"",colormenulistener,gui.getFrame());
         addToNewCluster.addActionListener(this);
         removefrompartition.addActionListener(this);
         renamecluster.addActionListener(this);
         this.add(new JSeparator(),0);
         this.add(colorMenu,0);
         this.add(renamecluster,0);         
         this.add(removefrompartition,0);
         this.add(addToNewCluster,0);
         this.add(addToExistingCluster,0);     
     }

     @Override public boolean updateMenu() {
         if (partitionTable.getSelectedRowCount()>0) {
              int[] selected=partitionTable.getSelectedRows();
              oldname=(selected==null || selected.length==0)?null:(String)partitionTable.getValueAt(selected[0],COLUMN_CLUSTER);
              if (oldname!=null) {
                  renamecluster.setActionCommand("Rename \""+oldname+"\"");
                  renamecluster.setText("Rename \""+oldname+"\"");
                  colorMenu.setActionCommand("Set Color For \""+oldname+"\"");                  
                  colorMenu.setText("Set Color For \""+oldname+"\"");                  
              }
         }         
         addToExistingCluster.removeAll();
         ArrayList<String>clusterNames=new ArrayList<String>();
         for (int i=0;i<partitionTableModel.getRowCount();i++) {
              String clusterName=(String)partitionTableModel.getValueAt(i, COLUMN_CLUSTER);
              if (clusterName!=null && !clusterNames.contains(clusterName)) clusterNames.add(clusterName);
         }
         Collections.sort(clusterNames);
         for (String clusterName:clusterNames) {
             JMenuItem item=new JMenuItem(clusterName);
             item.addActionListener(this);
             addToExistingCluster.add(item);
         }
         addToExistingCluster.setVisible(!clusterNames.isEmpty());   
         renamecluster.setVisible(oldname!=null);        
         colorMenu.setVisible(oldname!=null);        
         return super.updateMenu();
     }   

     @Override
     public void actionPerformed(ActionEvent e) {
         String cmd=e.getActionCommand();
         if (cmd.equals(addToNewCluster.getActionCommand())) {
              String newcluster=JOptionPane.showInputDialog("Enter name of new cluster");
              if (!MotifPartition.isValidClusterName(newcluster)) {
                 JOptionPane.showMessageDialog(this, "The name entered is not a valid name for a cluster","Invalid cluster name",JOptionPane.ERROR_MESSAGE);
                 return;
              }
              if (newcluster!=null && !newcluster.isEmpty()) setClusterForSelected(newcluster);
         } else if (cmd.equals(removefrompartition.getActionCommand())) {
               setClusterForSelected(null);
         } else if (cmd.startsWith("Rename")) {
              int[] selected=partitionTable.getSelectedRows();
              if (selected.length==0) return;
              Color oldcolor=gui.getVisualizationSettings().getClusterColor(oldname);
              String newcluster=JOptionPane.showInputDialog("Enter new name for cluster",oldname);
              if (!MotifPartition.isValidClusterName(newcluster)) {
                 JOptionPane.showMessageDialog(this, "The name entered is not a valid name for a cluster","Invalid cluster name",JOptionPane.ERROR_MESSAGE);
                 return;
              }
              if (newcluster!=null && !newcluster.isEmpty()) {
                  gui.getVisualizationSettings().setClusterColor(newcluster,oldcolor);
                  for (int i=0;i<partitionTable.getRowCount();i++) {
                     String rowvalue=(String)partitionTable.getValueAt(i,COLUMN_CLUSTER);
                     if (rowvalue!=null && rowvalue.equals(oldname)) partitionTable.setValueAt(newcluster,i,COLUMN_CLUSTER);
                  }
              }
         } else { // this command is the name of an existing cluster
            setClusterForSelected(cmd);
         }
         partitionTable.repaint();
     }

     private void setClusterForSelected(String clusterName) {
         int[] selected=partitionTable.getSelectedRows();
         for (int i:selected) {
             partitionTable.setValueAt(clusterName,i,COLUMN_CLUSTER);
         }
     }


 }



private class CellRenderer_Cluster extends DefaultTableCellRenderer {
    public CellRenderer_Cluster() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
    }
    @Override
    public void setValue(Object value) {
       if (value!=null) {
           setForeground(gui.getVisualizationSettings().getClusterColor(value.toString()));
           setText(value.toString());
       } else {
           setForeground(java.awt.Color.BLACK);
           setText(UNASSIGNED);
       }
    }
}// 

  public class ClusterNameValidatingCellEditor extends DefaultCellEditor {

    public ClusterNameValidatingCellEditor() {
        super(new JTextField());
        ((JTextField)getComponent()).setBorder(new javax.swing.border.LineBorder(Color.black));

    }
    @Override
    public boolean stopCellEditing() {
        JTextField textfield = (JTextField)getComponent();
        String clusterName=textfield.getText();
        if (clusterName==null) return super.stopCellEditing();
        else clusterName=clusterName.trim();
        if (clusterName.isEmpty()) return super.stopCellEditing();
        else if(DataPartition.isValidClusterName(clusterName)) {
            return super.stopCellEditing();
        } else { //text is invalid
            java.awt.Toolkit.getDefaultToolkit().beep();
	    return false; //don't let the editor go away
        }
    }
    @Override
    public Object getCellEditorValue() {
        JTextField textfield = (JTextField)getComponent();
        String clusterName=textfield.getText();
        if (clusterName==null) return null;
        else clusterName=clusterName.trim();
        if (clusterName.isEmpty()) return null;
        else return clusterName;
    }

  }
}
