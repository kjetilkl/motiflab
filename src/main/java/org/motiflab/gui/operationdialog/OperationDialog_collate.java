/*
 
 
 */

package org.motiflab.gui.operationdialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataMap;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.TextMap;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.data.analysis.CollatedAnalysis;
import org.motiflab.engine.operations.Operation_collate;
import org.motiflab.gui.MiscIcons;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_collate extends OperationDialog {

    private JPanel sourceTargetPanel;
    private JPanel addColumnButtonPanel;
    private JPanel columnsPanel;
    private JTextField headlineTextfield;
    private JButton addColumnButton;
    private JComboBox collateTypeComboBox;
    private String[] sequenceproperties;
    private String[] motifproperties;    
    private String[] moduleproperties;
    
    
    public OperationDialog_collate(JFrame parent) {
        super(parent);    
    }
    
    public OperationDialog_collate() {
        super();    
    }
    
    @Override
    public void initComponents() {
        super.initComponents();
        this.setLayout(new BorderLayout()); // this will replace the current layout
        
        motifproperties=Motif.getAllProperties(true,engine);
        moduleproperties=ModuleCRM.getProperties(engine);
        sequenceproperties=Sequence.getAllProperties(engine); 
        Arrays.sort(motifproperties);
        initColumnsPanel();        
        initAddColumnButtonPanel();
        initSourceTargetPanel(); // this must be initialized after the other panels to avoid NullPointerExceptions!
        sourceTargetPanel.setBorder(commonBorder);
        JPanel paddingPanel=new JPanel(new BorderLayout());
        JPanel spacePanel=new JPanel(new BorderLayout());
        paddingPanel.add(columnsPanel,BorderLayout.NORTH);
        paddingPanel.add(spacePanel,BorderLayout.CENTER);
        JScrollPane scrollpane=new JScrollPane(paddingPanel) {
            @Override
            public Dimension getPreferredSize() {
                Dimension dim=super.getPreferredSize();
                if (dim.height>350) dim.height=350; //
                if (dim.height<220) dim.height=220; //
                return dim;
            }
        };
        add(sourceTargetPanel, BorderLayout.NORTH);
        add(scrollpane,BorderLayout.CENTER);
        JPanel okcancelbuttonspanel=getOKCancelButtonsPanel();
        okcancelbuttonspanel.add(addColumnButtonPanel,BorderLayout.WEST);
        add(okcancelbuttonspanel, BorderLayout.SOUTH);
        pack();        
    }

    private void initSourceTargetPanel() {
        sourceTargetPanel=new JPanel();
        sourceTargetPanel.setLayout(new BoxLayout(sourceTargetPanel, BoxLayout.LINE_AXIS));
        JPanel targetPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel headlinePanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel collateTypePanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName(Analysis.class,getDataTypeTable());      
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(16);
        targetPanel.add(new JLabel("Store results in  "));
        targetPanel.add(targetDataTextfield);
        
        collateTypeComboBox=new JComboBox(new String[]{Motif.getType(),ModuleCRM.getType(),Sequence.getType()});
        collateTypePanel.add(new JLabel("    Data type  "));
        collateTypePanel.add(collateTypeComboBox); 
        
        headlineTextfield=new JTextField("");
        headlineTextfield.setColumns(16);
        headlinePanel.add(new JLabel("    Optional title  "));
        headlinePanel.add(headlineTextfield);
        String title=(String)parameters.getParameter(Operation_collate.OPTIONAL_TITLE);
        if (title!=null) headlineTextfield.setText(title);

        sourceTargetPanel.add(targetPanel);
        sourceTargetPanel.add(collateTypePanel);
        sourceTargetPanel.add(headlinePanel);

        initializeWithData();
        collateTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeCollateType((String)collateTypeComboBox.getSelectedItem());
            }
        });   
    }    
    
    private void initColumnsPanel() {
       columnsPanel=new JPanel();
       BoxLayout layout=new BoxLayout(columnsPanel, BoxLayout.Y_AXIS);
       columnsPanel.setLayout(layout); 
    }

    private void initializeWithData() {
        Class collateType=(Class)parameters.getParameter(Operation_collate.COLLATE_DATA_TYPE);
        String[][] sourceData=(String[][])parameters.getParameter(Operation_collate.SOURCE_DATA);
        if (collateType!=null && sourceData!=null) { // this is an existing protocol line which is being edited
            collateTypeComboBox.setSelectedItem(engine.getTypeNameForDataClass(collateType));
            for (String[] row:sourceData) {
                addNewColumn(collateType,row[0],row[1],row[2]);
            }
        } else if (parameters.getSourceDataName()!=null) { // initialized from infused sources (Analyses choosen in GUI)
            String[] sources=parameters.getSourceDataName().split("\\s*,\\s*");
            collateType=null;
            for (String name:sources) {
                Class sourceType=null;
                Class sourceClass=getClassForDataItem(name);
                if (sourceClass!=null && NumericMap.class.isAssignableFrom(sourceClass)) {
                    sourceType=NumericMap.getDataTypeForMapType(sourceClass);
                } else if (sourceClass!=null && TextMap.class.isAssignableFrom(sourceClass)) {
                    sourceType=TextMap.getDataTypeForMapType(sourceClass);
                } else {
                    Analysis analysis=getAnalysisOrProxyForName(name);
                    sourceType=(analysis!=null)?analysis.getCollateType():null;
                }
                if (sourceType!=null) {
                   if (collateType==null) {
                       collateType=sourceType;
                       collateTypeComboBox.setSelectedItem(engine.getTypeNameForDataClass(collateType));
                   } else if (sourceType!=collateType) continue;
                   addNewColumn(sourceType, name, null, null);
                }
            }
        } else {
            collateTypeComboBox.setSelectedIndex(0);
            changeCollateType((String)collateTypeComboBox.getSelectedItem());
        }
    }



    private void initAddColumnButtonPanel() {
       addColumnButtonPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
       addColumnButton=new JButton("Add Column");
       addColumnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               Class type=getCollateType();
               addNewColumn(type,null,null,null);
            }
       });
       addColumnButtonPanel.add(helpButton);       
       addColumnButtonPanel.add(new JLabel("   "));           
       addColumnButtonPanel.add(addColumnButton);       
    } 
    
    private void addNewColumn(Class type, String sourceName, String propertyName, String columnName) {
       columnsPanel.add(new SingleColumnPanel(type,sourceName,propertyName,columnName));
       columnsPanel.revalidate();
       columnsPanel.repaint();       
    }
        
    private void removeColumn(SingleColumnPanel column) {
       columnsPanel.remove(column);
       columnsPanel.revalidate();       
    }

    private void moveColumnUp(SingleColumnPanel column) {
       int index=0;
       Component[] components=columnsPanel.getComponents();
       if (components.length==1) return;
       for (int i=0;i<components.length;i++) {
           if (components[i]==column) {index=i;break;}
       }
       if (index==0) return; // already at the top
       columnsPanel.remove(column);
       columnsPanel.add(column, index-1);
       columnsPanel.revalidate();
    }

    private void moveColumnDown(SingleColumnPanel column) {
       int index=0;
       Component[] components=columnsPanel.getComponents();
       if (components.length==1) return;
       for (int i=0;i<components.length;i++) {
           if (components[i]==column) {index=i;break;}
       }
       if (index==components.length-1) return; // already at the bottom
       columnsPanel.remove(column);
       columnsPanel.add(column, index+1);
       columnsPanel.revalidate();
    }

    private Class getCollateType() {
         String type=(String)collateTypeComboBox.getSelectedItem();
         return engine.getDataClassForTypeName(type);
    }
    private void changeCollateType(String newType) {
       Class type=engine.getDataClassForTypeName(newType);
       columnsPanel.removeAll();
       addNewColumn(type,null,null,null); // add one column just to get things started...    
    }

    @Override
    protected void setParameters() {        
        String targetName=targetDataTextfield.getText();
        if (targetName!=null) targetName.trim();     
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);  
        parameters.setParameter(OperationTask.SOURCE_NAME, null);
        parameters.setParameter(Operation_collate.COLLATE_DATA_TYPE, getCollateType());
        String title=headlineTextfield.getText();
        if (title!=null) title.trim();
        if (title.isEmpty()) title=null;
        parameters.setParameter(Operation_collate.OPTIONAL_TITLE, title);

        Component[] columns=columnsPanel.getComponents();
        String[][] collatedColumns=new String[columns.length][];
        for (int i=0;i<columns.length;i++) {
           collatedColumns[i]=((SingleColumnPanel)columns[i]).getColumnInfo();
        }
        parameters.setParameter(Operation_collate.SOURCE_DATA, collatedColumns);
        parameters.addAffectedDataObject(targetName, CollatedAnalysis.class);  
    }    



    @Override
    protected String checkForErrors() {
       HashSet<String> columnNames=new HashSet<String>();
       Component[] columns=columnsPanel.getComponents();
       if (columns.length==0) return "No columns added";
       for (int i=0;i<columns.length;i++) {
          SingleColumnPanel singlecolumn=(SingleColumnPanel)columns[i];
          String[] column=singlecolumn.getColumnInfo();
          if (column[0].isEmpty()) return "Missing name of source in row "+(i+1);
          if (column[1]!=null && column[1].isEmpty()) return "Missing name of property in row "+(i+1);
          String columnName=(column[2]!=null)?column[2]:column[1];
          if (columnName==null) columnName=column[0]; // use name of data object as last resort
          if (columnNames.contains(columnName)) return "The column name '"+columnName+"' at row "+(i+1)+" is already in use by a previous column";
          if (columnName.contains("\"")) return "Column names are not allowed to contain double-quotes (at row "+(i+1)+")";
          columnNames.add(columnName);
        }
        String optionalTitle=headlineTextfield.getText();
        if (optionalTitle==null) optionalTitle="";
        optionalTitle=optionalTitle.trim();
        if (optionalTitle.contains("\"")) return "The optional title is not allowed to contain double-quotes";     
        return null; // no errors to report
    }

    private DefaultComboBoxModel getColumnSourceComboboxModel(Class type, String selectedSource) {
        DefaultComboBoxModel sourcemodel=getDataCandidates(new Class[]{Analysis.class,NumericMap.class,TextMap.class});
        int i=0;
        while (i<sourcemodel.getSize()) { // remove non-compatible sources
            String sourcename=(String)sourcemodel.getElementAt(i);
            Class collateType=getCollateTypeFromObjectOrProxy(sourcename);
            if (collateType==null || collateType!=type) sourcemodel.removeElementAt(i);
            else i++;
        }
        sourcemodel.addElement(engine.getTypeNameForDataClass(type)); // Add Motif/Module/Sequence as possible source
        int selectedIndex=0;
        if (selectedSource!=null) {
           for (i=0;i<sourcemodel.getSize();i++) {
              String source=(String)sourcemodel.getElementAt(i);
              if (selectedSource.equals(source)) {selectedIndex=i;break;}
           }
        } 
        sourcemodel.setSelectedItem(sourcemodel.getElementAt(selectedIndex));
        return sourcemodel;
    }
    
    private DefaultComboBoxModel getPropertyComboboxModel(String sourceName, String selectedProperty) {
        DefaultComboBoxModel propertymodel=null;  
        if (sourceName==null) {
            propertymodel=new DefaultComboBoxModel();
        } else if (sourceName.equals(Motif.getType())) {
            propertymodel=new DefaultComboBoxModel(motifproperties);
        } else if (sourceName.equals(ModuleCRM.getType())) {
            propertymodel=new DefaultComboBoxModel(moduleproperties);
        } else if (sourceName.equals(Sequence.getType())) {
            propertymodel=new DefaultComboBoxModel(sequenceproperties);
        } else { // the source should be the name of an analysis or DataMap
            Analysis analysis=getAnalysisOrProxyForName(sourceName);
            if (analysis!=null) propertymodel=new DefaultComboBoxModel(analysis.getColumnsExportedForCollation());
            else propertymodel=new DefaultComboBoxModel(); // this default applies to DataMaps also
        }
        if (selectedProperty!=null && propertymodel!=null) {
            propertymodel.setSelectedItem(selectedProperty);
        }
        return propertymodel;
    }    

    /* Note that this method can return a superclass (I think...) */
    private Class getTypeForSource(String sourceName) {
         if (sourceName==null) {
            return null;
        } else if (sourceName.equals(Motif.getType())) {
            return Motif.class;
        } else if (sourceName.equals(ModuleCRM.getType())) {
            return ModuleCRM.class;
        } else if (sourceName.equals(Sequence.getType())) {
            return Sequence.class;
        } else {
            return getClassForDataItem(sourceName);
        }
    }

    private Analysis getAnalysisOrProxyForName(String sourceName) {
        Data analysis=engine.getDataItem(sourceName);
        if (analysis instanceof Analysis) { // The analysis exists so I will use it directly
            return (Analysis)analysis;
        } else { // derive properties from proxy Analysis of same type
            Class analysisclass=getClassForDataItem(sourceName);
            Analysis proxy=engine.getAnalysisForClass(analysisclass);
            return proxy;
        }
    }

    /** Given the name of a source data object (analysis or numeric map) this method returns its collate type */
    private Class getCollateTypeFromObjectOrProxy(String sourceName) {
        Data data=engine.getDataItem(sourceName);
        if (data instanceof Analysis) { // The object exists so I will use it directly
            return ((Analysis)data).getCollateType();
        } else if (data instanceof DataMap) { // The object exists so I will use it directly
            return ((DataMap)data).getMembersClass();
        } else { // derive properties from proxy Analysis of same type or proxy DataMap
            Class proxyclass=getClassForDataItem(sourceName);
            if (proxyclass==null) return null;
            if (Analysis.class.isAssignableFrom(proxyclass)) {
                Analysis proxy=engine.getAnalysisForClass(proxyclass);
                return proxy.getCollateType();
            } else if (DataMap.class.isAssignableFrom(proxyclass)) {
                try {
                   Object proxy=proxyclass.newInstance();
                   return ((DataMap)proxy).getMembersClass();
                } catch (Exception e) {return null;}
            } else return null;
        }
    }


    /** This panel represents one column in the collated analysis (but it takes up one row in the dialog :P) */
    private class SingleColumnPanel extends JPanel {
         JComboBox columnSourceComboBox;
         JComboBox columnPropertyComboBox;
         JTextField newColumnNameTextfield;
         JButton moveUpButton;
         JButton moveDownButton;
         JButton removeColumnButton;
       
         public SingleColumnPanel(Class collateType, String sourceName, final String property, String columnName) {     
             this.setLayout(new FlowLayout(FlowLayout.LEFT));
             this.add(new JLabel("      Source"));
             columnSourceComboBox=new JComboBox(getColumnSourceComboboxModel(collateType,sourceName));
             if (sourceName==null) sourceName=(String)columnSourceComboBox.getSelectedItem();
             this.add(columnSourceComboBox);
             this.add(new JLabel("      Property"));
             columnPropertyComboBox=new JComboBox(getPropertyComboboxModel(sourceName,property));
             if (sourceName!=null && !sourceName.isEmpty()) {
                 Class sourceClass=getTypeForSource(sourceName);
                 if (sourceClass!=null && DataMap.class.isAssignableFrom(sourceClass)) columnPropertyComboBox.setEnabled(false);
             }
             this.add(columnPropertyComboBox);
             this.add(new JLabel("      Column name"));
             newColumnNameTextfield=new JTextField(8);
             if (columnName!=null) newColumnNameTextfield.setText(columnName);
             this.add(newColumnNameTextfield);
             Dimension dim=new Dimension(26,22);
             moveUpButton=new JButton(new MiscIcons((MiscIcons.UP_TRIANGLE)));
             moveUpButton.setPreferredSize(dim);
             moveUpButton.setToolTipText("Move this column up");
             moveUpButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveColumnUp(SingleColumnPanel.this);
                }
             });
             this.add(moveUpButton);
             moveDownButton=new JButton(new MiscIcons((MiscIcons.DOWN_TRIANGLE)));
             moveDownButton.setPreferredSize(dim);
             moveDownButton.setToolTipText("Move this column down");
             moveDownButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveColumnDown(SingleColumnPanel.this);
                }
             });
             this.add(moveDownButton);
             removeColumnButton=new JButton(new MiscIcons((MiscIcons.XCROSS_ICON)));
             removeColumnButton.setPreferredSize(dim);
             removeColumnButton.setToolTipText("Remove this column");
             removeColumnButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeColumn(SingleColumnPanel.this);
                }
             });
             this.add(removeColumnButton);
             columnSourceComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newsource=(String)columnSourceComboBox.getSelectedItem();
                    columnPropertyComboBox.setModel(getPropertyComboboxModel(newsource, null));
                    Class sourceClass=getTypeForSource(newsource);
                    if (sourceClass!=null && DataMap.class.isAssignableFrom(sourceClass)) columnPropertyComboBox.setEnabled(false);
                    else columnPropertyComboBox.setEnabled(true);
                }
             });
             columnSourceComboBox.setEditable(true);
             columnPropertyComboBox.setEditable(true);
         }
         
         public String[] getColumnInfo() {
             String source=(String)columnSourceComboBox.getSelectedItem();
             if (source==null) source="";
             String property=null; 
             if (columnPropertyComboBox.isEnabled()) { // a disabled combobox means that the source is a DataMap
                 property=(String)columnPropertyComboBox.getSelectedItem();
                 if (property==null) property=""; // property==null signals that the source is (probably) a DataMap
                 else property=property.trim();
             }
             String newname=(String)newColumnNameTextfield.getText();
             if (newname!=null) newname=newname.trim();
             if (newname.isEmpty()) newname=null;
             return new String[]{source.trim(),property,newname};
         }

    }
}
