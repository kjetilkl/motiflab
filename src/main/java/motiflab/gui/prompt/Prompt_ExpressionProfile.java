/*
 
 
 */

package motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.SystemError;
import motiflab.engine.data.ExpressionProfile;
import motiflab.engine.data.Sequence;
import motiflab.engine.dataformat.DataFormat;
import motiflab.gui.ColorGradient;
import motiflab.gui.ColorMenu;
import motiflab.gui.ColorMenuListener;
import motiflab.gui.ExcelAdapter;
import motiflab.gui.LoadFromFilePanel;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.SimpleDataPanelIcon;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class Prompt_ExpressionProfile extends Prompt {

    private ExpressionProfile data;

    private JPanel manualEntryPanel;
    private LoadFromFilePanel importFromFilePanel;
    private JTabbedPane tabbedPanel;
    private JTable expressionTable;
    private JScrollPane scrollpane;
    private DefaultTableModel expressionTableModel;
    private JLabel manualPanelErrorLabel;
    private boolean showExisting=false;
    private CellRenderer_Experiment experimentRenderer=new CellRenderer_Experiment();
    private double[] valuesRange=new double[2];
    private JTextField minValueTextField;
    private JTextField maxValueTextField;
    private int minColumnWidth=65;
    private int minSequenceColumnWidth=130;
    private JCheckBox logcolorcheckbox;
    private ColumnHeaderContextMenu headerContextMenu;
    private JLabel upregColorLabel;
    private JLabel downregColorLabel;
    private JLabel backgroundColorLabel;
    private SimpleDataPanelIcon upregColorIcon;
    private SimpleDataPanelIcon downregColorIcon;
    private SimpleDataPanelIcon backgroundColorIcon;
    private VisualizationSettings visualizationsettings;
    private ColorGradient upregulatedGradient;
    private ColorGradient downregulatedGradient;
    private JPopupMenu selectMotifColorMenu;
    private SimpleDataPanelIcon selectedIcon; // used to track the icon whose color is being selected
    

     public Prompt_ExpressionProfile(MotifLabGUI gui, String prompt, ExpressionProfile dataitem) {
         this(gui,prompt,dataitem,true);
         visualizationsettings=gui.getVisualizationSettings();              
     }
    
    public Prompt_ExpressionProfile(MotifLabGUI gui, String prompt, ExpressionProfile dataitem, boolean modal) {
        super(gui,prompt,modal);
        visualizationsettings=gui.getVisualizationSettings();  
        showExisting=(dataitem!=null);
        if (dataitem!=null) {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else data=new ExpressionProfile(gui.getGenericDataitemName(ExpressionProfile.class, null));
        setDataItemName(data.getName());
        setTitle("Expression Profile");
        SimpleDataPanelIcon icon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.EXPRESSION_PROFILE_ICON,SimpleDataPanelIcon.NO_BORDER,gui.getVisualizationSettings());
        icon.setBackgroundColor(Color.WHITE);
        setDataItemIcon(icon, true);
        setupManualEntryPanel();
        setupImportProfilePanel();
        tabbedPanel=new JTabbedPane();
        tabbedPanel.addTab("Manual Entry", manualEntryPanel);
        tabbedPanel.addTab("Import", importFromFilePanel);
        JPanel internal=new JPanel(new BorderLayout());
        Dimension size=new Dimension(650,350);
        internal.setMinimumSize(size);
        internal.setPreferredSize(size);
        // internal.setMaximumSize(size);
        manualEntryPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        importFromFilePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        internal.add(tabbedPanel);
        this.setResizable(true);
        setMainPanel(internal);
        pack();
        if (showExisting) {
           tabbedPanel.setSelectedComponent(manualEntryPanel);
           focusOKButton();
        }
    }

    private void setupManualEntryPanel() {
        manualEntryPanel=new JPanel();
        manualEntryPanel.setLayout(new BorderLayout());
        int numExp=data.getNumberOfConditions();
        if (numExp==0) numExp=1; // create a default condition for new ExpressionProfiles
        String[] columnNames=new String[numExp+1];
        columnNames[0]="Sequence";
        for (int i=0;i<numExp;i++) {
            columnNames[i+1]=data.getHeader(i);
        }
        ArrayList<Sequence> sequences=engine.getDefaultSequenceCollection().getAllSequencesInDefaultOrder(engine);
        Object[][] tabledata=new Object[sequences.size()][numExp+1];
        for (int i=0;i<sequences.size();i++) {
            String sequenceName=sequences.get(i).getName();
            tabledata[i][0]=sequenceName;
            for (int j=1;j<=numExp;j++) {
               tabledata[i][j]=data.getValue(sequenceName, j-1);
               if (tabledata[i][j]==null) tabledata[i][j]=new Double(0);
            }
        }
        valuesRange=data.getValuesRange();
        if (valuesRange==null) valuesRange=new double[]{-3f,3f};
        if (valuesRange[0]>0) valuesRange[0]=0;
        if (valuesRange[1]<0) valuesRange[1]=0;
        expressionTableModel=new ExpressionTableModel(tabledata, columnNames);
        expressionTable=new JTable(expressionTableModel);
        expressionTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus        
        expressionTable.setDefaultEditor(Object.class,new ExpressionEditor(new JTextField()));
        expressionTable.setRowHeight(22);
        expressionTable.setDefaultRenderer(Object.class, experimentRenderer);
        expressionTable.setAutoCreateRowSorter(true);
        expressionTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        expressionTable.setRowSelectionAllowed(true);
        expressionTable.setColumnSelectionAllowed(true);
        setColumnWidths();
        
        expressionTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);        
        expressionTable.setShowGrid(true);
        expressionTable.getTableHeader().setReorderingAllowed(false);
        ((JLabel)expressionTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
        ExcelAdapter adapter=new ExcelAdapter(expressionTable, false, ExcelAdapter.CONVERT_TO_DOUBLE);
        scrollpane=new JScrollPane(expressionTable);
        scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        manualEntryPanel.add(scrollpane,BorderLayout.CENTER);
        JPanel widgetsPanel=new JPanel(new BorderLayout());
        JPanel buttonsPanel=new JPanel(new FlowLayout());

        JButton addConditionButton=new JButton("Add Condition");
        // JButton removeExperimentButton=new JButton("Remove Last Experiment");
        buttonsPanel.add(addConditionButton);
        //buttonsPanel.add(removeExperimentButton);
        // JPanel rangePanel=new JPanel(new FlowLayout());
        buttonsPanel.add(new JLabel("     Range (for coloring): "));
        minValueTextField=new JTextField(8);
        maxValueTextField=new JTextField(8);
        minValueTextField.setHorizontalAlignment(JTextField.RIGHT);
        maxValueTextField.setHorizontalAlignment(JTextField.RIGHT);
        buttonsPanel.add(minValueTextField);
        buttonsPanel.add(new JLabel(" to "));
        buttonsPanel.add(maxValueTextField);
        logcolorcheckbox=new JCheckBox("       Brighten",true);
        logcolorcheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        buttonsPanel.add(logcolorcheckbox);
        upregulatedGradient=visualizationsettings.getExpressionColorUpregulatedGradient();
        downregulatedGradient=visualizationsettings.getExpressionColorDownregulatedGradient();
        upregColorIcon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
        upregColorIcon.setForegroundColor(visualizationsettings.getExpressionColorUpregulated());
        downregColorIcon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
        downregColorIcon.setForegroundColor(visualizationsettings.getExpressionColorDownregulated());
        backgroundColorIcon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
        backgroundColorIcon.setForegroundColor(visualizationsettings.getExpressionColorBackground());
        upregColorLabel = new JLabel(upregColorIcon);
        downregColorLabel = new JLabel(downregColorIcon);
        backgroundColorLabel = new JLabel(backgroundColorIcon);
        Dimension iconDim=new Dimension(13,13);
        upregColorLabel.setPreferredSize(iconDim); // Because of an existing "bug" in SimpleDataPanelIcon, this resizing is necessary in order to draw the border correctly
        downregColorLabel.setPreferredSize(iconDim); 
        backgroundColorLabel.setPreferredSize(iconDim);        
        upregColorLabel.setToolTipText("Select color for upregulation");
        downregColorLabel.setToolTipText("Select color for downregulation");
        backgroundColorLabel.setToolTipText("Select background color");
        buttonsPanel.add(downregColorLabel);
        buttonsPanel.add(backgroundColorLabel);
        buttonsPanel.add(upregColorLabel);
        minValueTextField.setText((valuesRange!=null)?""+valuesRange[0]:"-3");
        maxValueTextField.setText((valuesRange!=null)?""+valuesRange[1]:"3");
        minValueTextField.setCaretPosition(0);
        maxValueTextField.setCaretPosition(0);
        widgetsPanel.add(buttonsPanel,BorderLayout.CENTER);
        manualPanelErrorLabel=new JLabel("  ");
        manualPanelErrorLabel.setFont(errorMessageFont);
        manualPanelErrorLabel.setForeground(java.awt.Color.RED);
        widgetsPanel.add(manualPanelErrorLabel,BorderLayout.SOUTH);
        // manualEntryPanel.add(rangePanel,BorderLayout.NORTH);
        manualEntryPanel.add(scrollpane,BorderLayout.CENTER);
        manualEntryPanel.add(widgetsPanel,BorderLayout.SOUTH);
        addConditionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int columns=expressionTableModel.getColumnCount();
                Double[] columnData=new Double[expressionTable.getRowCount()];
                for (int i=0;i<columnData.length;i++) columnData[i]=new Double(0);               
                String columnName=(""+columns);
                while (columnExists(columnName,-1)) { // does a columns exist with that name?
                    columns++;
                    columnName=(""+columns);
                }
                expressionTableModel.addColumn(columnName,columnData);
                expressionTable.getColumn(columnName).setPreferredWidth(minColumnWidth); 
            }
        });
        minValueTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMinRangeField();
            }
        });
        minValueTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
               updateMinRangeField();
            }

        });
        maxValueTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMaxRangeField();
            }
        });
        maxValueTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateMaxRangeField();
            }

        });
        logcolorcheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expressionTable.repaint();
            }
        });
        headerContextMenu = new ColumnHeaderContextMenu();
        expressionTable.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) showContextMenu(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) showContextMenu(e);
                }         
            }
        );  
        final ColorMenuListener selectcolorlistener=new ColorMenuListener() {
            @Override
            public void newColorSelected(Color color) {
                if (color!=null){
                    selectedIcon.setForegroundColor(color);
                    updateColorGradients();
                }                   
            }
        };
        ColorMenu temp=new ColorMenu("Select Color", selectcolorlistener, Prompt_ExpressionProfile.this);
        selectMotifColorMenu=temp.wrapInPopup();
        MouseAdapter updateColorsListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedIcon=(SimpleDataPanelIcon)((JLabel)e.getSource()).getIcon();
                selectMotifColorMenu.show((Component)e.getSource(), e.getX(), e.getY());
            }         
        };        
        upregColorLabel.addMouseListener(updateColorsListener);
        downregColorLabel.addMouseListener(updateColorsListener);
        backgroundColorLabel.addMouseListener(updateColorsListener);
    }
    
    private void updateColorGradients() {
        Color up=upregColorIcon.getForegroundColor();
        Color down=downregColorIcon.getForegroundColor();
        Color background=backgroundColorIcon.getForegroundColor();
        visualizationsettings.setExpressionColors(up,down,background);     
        upregulatedGradient=visualizationsettings.getExpressionColorUpregulatedGradient();
        downregulatedGradient=visualizationsettings.getExpressionColorDownregulatedGradient();
        upregColorLabel.repaint();
        downregColorLabel.repaint();
        backgroundColorLabel.repaint();     
        expressionTable.repaint();
    }
    
    private void showContextMenu(MouseEvent e) {
         int col=expressionTable.columnAtPoint(new Point(e.getX(), e.getY()));
         if (col<=0) return; // do not show context menu for column=0 (=sequence)
         headerContextMenu.setColumnIndex(col);
         headerContextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    /** Checks if the table already contains a column with the given name */
    private boolean columnExists(String columnName, int skip) {  
        TableColumnModel tcm=expressionTable.getColumnModel();
        int count=tcm.getColumnCount();
	for (int i=0;i<count;i++) {
            if (i==skip) continue;
            TableColumn col=tcm.getColumn(i);
	    Object othername = col.getIdentifier();
            if (othername==null) continue;
	    if (columnName.equals(othername.toString())) return true;
	}               
        return false;
    }
    
    private void setColumnWidths() {
        expressionTable.getColumn("Sequence").setPreferredWidth(minSequenceColumnWidth); 
        TableColumnModel tcm=expressionTable.getColumnModel();
        for (int i=1;i<tcm.getColumnCount();i++) {
            tcm.getColumn(i).setPreferredWidth(minColumnWidth);
        }               
    }
    
    private void updateMinRangeField() {
        try {
           double val=Double.parseDouble(minValueTextField.getText());
           if (val>0) val=0;
           valuesRange[0]=val;
           minValueTextField.setText(""+valuesRange[0]);
           expressionTable.repaint();
       } catch (NumberFormatException nfe) {minValueTextField.setText(""+valuesRange[0]);}
        minValueTextField.setCaretPosition(0);
    }

    private void updateMaxRangeField() {
        try {
           double val=Double.parseDouble(maxValueTextField.getText());
           if (val<0) val=0;
           valuesRange[1]=val;
           maxValueTextField.setText(""+valuesRange[1]);
           expressionTable.repaint();
        } catch (NumberFormatException nfe) {maxValueTextField.setText(""+valuesRange[1]);}
        maxValueTextField.setCaretPosition(0);        
    }

    private void setupImportProfilePanel() {
        ArrayList<DataFormat> dataformats=engine.getDataInputFormats(ExpressionProfile.class);
        //dataformats.remove(engine.getDataFormat("Plain"));
        importFromFilePanel=new LoadFromFilePanel(dataformats,gui,ExpressionProfile.class);
    }



    @Override
    public boolean onOKPressed() {
        if (tabbedPanel.getSelectedComponent()==importFromFilePanel) {
            try {
                String filename=importFromFilePanel.getFilename();
                if (filename==null) throw new SystemError("Missing filename");
                data=(ExpressionProfile)importFromFilePanel.loadData(data,ExpressionProfile.getType());
                DataFormat format=importFromFilePanel.getDataFormat();
                ParameterSettings settings=importFromFilePanel.getParameterSettings();
                setImportFromFileSettings(filename, (format!=null)?format.getName():null, settings);
            } catch (Exception e) {
                String exceptionText=e.getClass().getSimpleName();
                if (exceptionText.contains("ParseError") || exceptionText.contains("ExecutionError")) exceptionText="";
                else exceptionText+=":";
                JOptionPane.showMessageDialog(this, "An error occurred while importing expression profile from file:\n"+exceptionText+e.getMessage(),"Import Error",JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (tabbedPanel.getSelectedComponent()==manualEntryPanel) {
            int colunms=expressionTable.getColumnCount();
            int rows=expressionTable.getRowCount();
            for (int i=0;i<rows;i++) {
              for (int j=1;j<colunms;j++) {
                 Object val=expressionTable.getValueAt(i, j);
                 if (val!=null) {
                     try {
                         Double.parseDouble(val.toString());
                     } catch (NumberFormatException e) {reportManualEntryError("Unable to parse expected numerical value for "+expressionTable.getValueAt(i, 0)+" condition "+j);return false;}
                 }
              }
            }
            // everything should be OK here
            data.clear(); // start from scratch
            for (int i=0;i<rows;i++) {
               String seqname=(String)expressionTable.getValueAt(i, 0);
               data.addSequence(seqname);
            }
            for (int j=0;j<colunms-1;j++) {
               data.addCondition();
            }
            for (int i=0;i<rows;i++) {
              String seqname=(String)expressionTable.getValueAt(i, 0);
              for (int j=1;j<colunms;j++) {
                 Object val=expressionTable.getValueAt(i, j);
                 if (val!=null) {
                     try {
                         double value=(val.toString().trim().isEmpty())?0:Double.parseDouble(val.toString());
                         data.setValue(seqname, j-1, value);
                     } catch (NumberFormatException e) {reportManualEntryError("Unable to parse expected number: "+val.toString());return false;}
                 } else data.setValue(seqname, j-1, 0);
              }
            }
            boolean onlydefault=checkIfDefaultColumnNamesAreUsed();
            if (!onlydefault){
                for (int i=1;i<expressionTable.getColumnCount();i++) {
                    String expected=""+i;
                    String found=expressionTable.getColumnName(i);
                    if (!expected.equals(found)) {
                        try {data.setHeader(i-1, found);} catch (ExecutionError e) {}
                    }
                }
            }
        } // END (tabbedPanel.getSelectedComponent()==manualEntryPanel)
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);
        return true;
    }

    @Override
    public Data getData() {
       return data;
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof ExpressionProfile) data=(ExpressionProfile)newdata; 
    }     


    private void reportManualEntryError(String msg) {
        if (msg==null) msg="NULL";
        manualPanelErrorLabel.setText(msg);
    }

    /** Checks the names of columns 1 to N and returns TRUE if the names of these
     *  are identical to their indices (i.e. "1","2"..."N") or FALSE if columns
     *  have non-standard user-assigned names
     */    
    private boolean checkIfDefaultColumnNamesAreUsed() {
        for (int i=1;i<expressionTable.getColumnCount();i++) {
            String expected=""+i;
            String found=expressionTable.getColumnName(i);
            if (!expected.equals(found)) return false;
        }
        return true;
    }

private class CellRenderer_Experiment extends DefaultTableCellRenderer {

    public CellRenderer_Experiment() {
           super();         
       }

       @Override
       public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int colIndex) {
           if (colIndex==0) this.setHorizontalAlignment(LEFT); else this.setHorizontalAlignment(RIGHT);
           if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
                if (value==null) setText("");
                else setText(value.toString());
            } else if (colIndex==0) { // sequence name column
                String sequenceName=value.toString();
                setBackground(java.awt.Color.WHITE);
                setForeground(visualizationsettings.getSequenceLabelColor(sequenceName));
                setText(sequenceName);
            } else if (value instanceof Double) { // expression value. 
                double number=((Double)value);
                Color background=null;
                Color foreground=Color.BLACK;
                if (number==0) background=upregulatedGradient.getBackgroundColor();
                else if (number>=valuesRange[1]) background=upregulatedGradient.getForegroundColor();
                else if (number<valuesRange[1] && number>0) background=upregulatedGradient.getColor(getColorValue(number,valuesRange[1]));
                else if (number<0 && number>valuesRange[0]) background=downregulatedGradient.getColor(getColorValue(number,valuesRange[0]));
                else if (number<=valuesRange[0]) background=downregulatedGradient.getForegroundColor();
                if (VisualizationSettings.isDark(background)) foreground=Color.WHITE;
                setBackground(background);
                setForeground(foreground);
                setText(value.toString());
            } else if (value==null) { // Missing value error
               setBackground(java.awt.Color.WHITE);
               setForeground(java.awt.Color.RED);
               setText("");
            } else { // Should not happen error
               setBackground(java.awt.Color.WHITE);
               setForeground(java.awt.Color.RED);
               setText(value.toString());
            }
            return this;
       }

}// end class CellRenderer_Experiment

/** Returns a color-index between 0 and 255 by log-transforming the raw value
 *  This will give a better gradient (I think)
 */
private int getColorValue(double value, double maxvalue) {
    double fraction=value/maxvalue;
    if (logcolorcheckbox.isSelected()) {
        return (int)(Math.log10(fraction*9+1)*255);
    } // the argument of log10 will go from 1 to 10 so the log-value will be between 0 and 1
    else return (int)(255*fraction);
}

private class ExpressionEditor extends DefaultCellEditor {

    public ExpressionEditor(JTextField textfield) {
        super(textfield);
    }
    @Override
    public Object getCellEditorValue() {
        Object text=super.getCellEditorValue();
        if (text==null) return "";
        try {
            Double val=Double.parseDouble(text.toString());
            return val;
        } catch (NumberFormatException e) {return text;}
    }

}

 private class ColumnHeaderContextMenu extends JPopupMenu implements ActionListener {
    private int column=0;
     
    public ColumnHeaderContextMenu() {
        JMenuItem item=new JMenuItem("Rename");
        item.addActionListener(this);
        this.add(item);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd=e.getActionCommand();
        if (cmd.equalsIgnoreCase("Rename")) {
            TableColumn tablecol=expressionTable.getColumnModel().getColumn(column);
            String oldname=tablecol.getHeaderValue().toString();
            String newname=JOptionPane.showInputDialog(Prompt_ExpressionProfile.this, "Enter new name", oldname);
            if (newname==null || newname.trim().isEmpty() || newname.contains(";") || newname.equalsIgnoreCase("sequence")) return; // ';' is not allowed in the name
            newname=newname.trim();
            if (!newname.equals(oldname)) {               
                if (!columnExists(newname,column)) { // check if new name is already in use by another column
                    ((ExpressionTableModel)expressionTable.getModel()).setColumnName(column, newname);
                    tablecol.setHeaderValue(newname);
                    tablecol.setIdentifier(newname);
                    expressionTable.getTableHeader().repaint();
                }
            }
            // debugHeaders();
        }
    }
    
    public void setColumnIndex(int index) {
        this.column=index;
    }
    
 }
 
 private class ExpressionTableModel extends DefaultTableModel {
     
        public ExpressionTableModel(Object[][] tabledata, String[] columnNames) {
          super(tabledata, columnNames);
        }
        @Override
        public Class<?> getColumnClass(int column) {
            if (column==0) return String.class; else return Double.class;
        }
        @Override
        public boolean isCellEditable(int row, int col) {
            return (col>0 && isDataEditable());                
        }    
        @SuppressWarnings("unchecked")
        public void setColumnName(int column, String newname) {
            columnIdentifiers.set(column,newname);
        }
 }

 private void debugHeaders() {
     for (int i=0;i<expressionTable.getColumnCount();i++) {
         String colname=expressionTable.getColumnName(i);
         TableColumn col=expressionTable.getColumnModel().getColumn(i);
         gui.logMessage("["+i+"] name="+colname+",  id="+col.getIdentifier()+",  header="+col.getHeaderValue());
     }
 }
 

}
