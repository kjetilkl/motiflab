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
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.operations.Operation_rank;
import org.motiflab.gui.MiscIcons;

/**
 *
 * @author kjetikl
 */
public class OperationDialog_rank extends OperationDialog {

    private JPanel sourceTargetPanel;
    private JPanel addColumnButtonPanel;
    private JPanel columnsPanel;
    private JButton addColumnButton;
    private JComboBox rankTypeComboBox;
    private String[] sequenceproperties;
    private String[] motifproperties;
    private String[] moduleproperties;


    public OperationDialog_rank(JFrame parent) {
        super(parent);
    }

    public OperationDialog_rank() {
        super();
    }

    @Override
    public void initComponents() {
        super.initComponents();
        this.setLayout(new BorderLayout()); // this will replace the current layout

        String[] standardProps=Motif.getNumericStandardProperties(true);
        String[] userProps=Motif.getNumericUserDefinedProperties(engine);
        motifproperties=new String[standardProps.length+userProps.length];
        System.arraycopy(standardProps, 0, motifproperties, 0, standardProps.length);
        System.arraycopy(userProps, 0, motifproperties, standardProps.length, userProps.length);
        Arrays.sort(motifproperties);
        moduleproperties=ModuleCRM.getNumericProperties(engine);
        sequenceproperties=Sequence.getNumericProperties(engine);
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
        JPanel rankTypePanel=new JPanel(new FlowLayout(FlowLayout.CENTER));

        String targetName=parameters.getTargetDataName();
        if (targetName==null || targetName.isEmpty()) targetName=gui.getGenericDataitemName("rankOrder",getDataTypeTable());
        targetDataTextfield=new JTextField(targetName);
        targetDataTextfield.setColumns(16);
        targetPanel.add(new JLabel("Store results in  "));
        targetPanel.add(targetDataTextfield);

        rankTypeComboBox=new JComboBox(new String[]{Motif.getType(),ModuleCRM.getType(),Sequence.getType()});
        rankTypePanel.add(new JLabel("    Data type  "));
        rankTypePanel.add(rankTypeComboBox);

        sourceTargetPanel.add(targetPanel);
        sourceTargetPanel.add(rankTypePanel);


        initializeWithData();
        rankTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeRankType((String)rankTypeComboBox.getSelectedItem());
            }
        });
    }

    private void initColumnsPanel() {
       columnsPanel=new JPanel();
       BoxLayout layout=new BoxLayout(columnsPanel, BoxLayout.Y_AXIS);
       columnsPanel.setLayout(layout);
    }

    private void initializeWithData() {
        Class rankType=(Class)parameters.getParameter(Operation_rank.TARGET_DATA_TYPE);
        String[][] sourceData=(String[][])parameters.getParameter(Operation_rank.SOURCE_DATA);
        if (rankType!=null && sourceData!=null) { // this is an existing protocol line which is being edited
            rankTypeComboBox.setSelectedItem(engine.getTypeNameForDataClass(rankType));
            for (String[] row:sourceData) {
                addNewColumn(rankType,row[0],row[1],row[2],row[3]);
            }
        } else if (parameters.getSourceDataName()!=null) { // initialized from infused sources (Analyses choosen in GUI)
            String[] sources=parameters.getSourceDataName().split("\\s*,\\s*");
            rankType=null;
            for (String name:sources) {
                Class sourceType=null;
                Class sourceClass=getClassForDataItem(name);
                if (sourceClass!=null && NumericMap.class.isAssignableFrom(sourceClass)) {
                    sourceType=NumericMap.getDataTypeForMapType(sourceClass);
                } else {
                    Analysis analysis=getAnalysisOrProxyForName(name);
                    sourceType=(analysis!=null)?analysis.getCollateType():null;
                }
                if (sourceType!=null) {
                   if (rankType==null) {
                       rankType=sourceType;
                       rankTypeComboBox.setSelectedItem(engine.getTypeNameForDataClass(rankType));
                   } else if (sourceType!=rankType) continue;
                   addNewColumn(sourceType, name, null, "1",null);
                }
            }
        } else {
            rankTypeComboBox.setSelectedIndex(0);
            changeRankType((String)rankTypeComboBox.getSelectedItem());
        }
    }



    private void initAddColumnButtonPanel() {
       addColumnButtonPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
       addColumnButton=new JButton("Add Property");
       addColumnButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               Class type=getRankType();
               addNewColumn(type,null,null,"1",null);
            }
       });
       addColumnButtonPanel.add(helpButton);       
       addColumnButtonPanel.add(new JLabel("   "));       
       addColumnButtonPanel.add(addColumnButton);
    }

    private void addNewColumn(Class type, String sourceName, String propertyName, String weightString, String direction) {
       columnsPanel.add(new SingleColumnPanel(type,sourceName,propertyName,weightString,direction));
       columnsPanel.revalidate();
       columnsPanel.repaint();
    }

    private void removeColumn(SingleColumnPanel column) {
       columnsPanel.remove(column);
       columnsPanel.revalidate();
    }

    private Class getRankType() {
         String type=(String)rankTypeComboBox.getSelectedItem();
         return engine.getDataClassForTypeName(type);
    }
    private void changeRankType(String newType) {
       Class type=engine.getDataClassForTypeName(newType);
       columnsPanel.removeAll();
       addNewColumn(type,null,null,"1",null); // add one column just to get things started...
    }

    @Override
    protected void setParameters() {
        String targetName=targetDataTextfield.getText();
        if (targetName!=null) targetName.trim();
        parameters.setParameter(OperationTask.TARGET_NAME, targetName);
        parameters.setParameter(OperationTask.SOURCE_NAME, null);
        Class rankType=getRankType();
        Class targetMapType=NumericMap.getMapTypeForDataType(rankType);
        parameters.setParameter(Operation_rank.TARGET_DATA_TYPE, rankType);

        Component[] columns=columnsPanel.getComponents();
        String[][] rankColumns=new String[columns.length][];
        for (int i=0;i<columns.length;i++) {
           rankColumns[i]=((SingleColumnPanel)columns[i]).getColumnInfo();
        }
        parameters.setParameter(Operation_rank.SOURCE_DATA, rankColumns);
        parameters.addAffectedDataObject(targetName, targetMapType);
    }



    @Override
    protected String checkForErrors() {
       Component[] columns=columnsPanel.getComponents();
       if (columns.length==0) return "No columns added";
       for (int i=0;i<columns.length;i++) {
          SingleColumnPanel singlecolumn=(SingleColumnPanel)columns[i];
          String[] column=singlecolumn.getColumnInfo();
          if (column[0].isEmpty()) return "Missing name of source in row "+(i+1);
          if (column[1]!=null && column[1].isEmpty()) return "Missing name of property in row "+(i+1);
          //String weightString=column[2];
        }
        return null; // no errors to report
    }

    private DefaultComboBoxModel getColumnSourceComboboxModel(Class type, String selectedSource) {
        DefaultComboBoxModel sourcemodel=getDataCandidates(new Class[]{Analysis.class,NumericMap.class});
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
        } else { // the source should be the name of an analysis or NumericMap
            Analysis analysis=getAnalysisOrProxyForName(sourceName);
            if (analysis!=null) propertymodel=new DefaultComboBoxModel(analysis.getColumnsExportedForCollation());
            else propertymodel=new DefaultComboBoxModel(); // this default applies to NumericMaps also
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
        } else if (data instanceof NumericMap) { // The object exists so I will use it directly
            return ((NumericMap)data).getMembersClass();
        } else { // derive properties from proxy Analysis of same type or proxy NumericMap
            Class proxyclass=getClassForDataItem(sourceName);
            if (proxyclass==null) return null;
            if (Analysis.class.isAssignableFrom(proxyclass)) {
                Analysis proxy=engine.getAnalysisForClass(proxyclass);
                return proxy.getCollateType();
            } else if (NumericMap.class.isAssignableFrom(proxyclass)) {
                try {
                   Object proxy=proxyclass.newInstance();
                   return ((NumericMap)proxy).getMembersClass();
                } catch (Exception e) {return null;}
            } else return null;
        }
    }


    /** This panel represents settings for one column/property */
    private class SingleColumnPanel extends JPanel {
         JComboBox columnSourceComboBox;
         JComboBox columnPropertyComboBox;
         JComboBox directionComboBox;
         JTextField weightTextfield;
         JButton removeColumnButton;

         public SingleColumnPanel(Class collateType, String sourceName, final String property, String weightString, String direction) {
             this.setLayout(new FlowLayout(FlowLayout.LEFT));
             this.add(new JLabel("      Source"));
             columnSourceComboBox=new JComboBox(getColumnSourceComboboxModel(collateType,sourceName));
             if (sourceName==null) sourceName=(String)columnSourceComboBox.getSelectedItem();
             this.add(columnSourceComboBox);
             this.add(new JLabel("      Property"));
             columnPropertyComboBox=new JComboBox(getPropertyComboboxModel(sourceName,property));
             if (sourceName!=null && !sourceName.isEmpty()) {
                 Class sourceClass=getTypeForSource(sourceName);
                 if (sourceClass!=null && NumericMap.class.isAssignableFrom(sourceClass)) columnPropertyComboBox.setEnabled(false);
             }
             this.add(columnPropertyComboBox);
             this.add(new JLabel("      Direction"));
             directionComboBox=new JComboBox(new String[]{"Ascending","Descending"});
             if (direction!=null && direction.equalsIgnoreCase("descending")) directionComboBox.setSelectedItem("Descending");
             else directionComboBox.setSelectedItem("Ascending");
             this.add(directionComboBox);
             this.add(new JLabel("      weight"));
             weightTextfield=new JTextField(8);
             if (weightString!=null) weightTextfield.setText(weightString);
             else weightTextfield.setText("1");
             this.add(weightTextfield);
             Dimension dim=new Dimension(26,22);
             removeColumnButton=new JButton(new MiscIcons((MiscIcons.XCROSS_ICON)));
             removeColumnButton.setPreferredSize(dim);
             removeColumnButton.setToolTipText("Remove this entry");
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
                    if (sourceClass!=null && NumericMap.class.isAssignableFrom(sourceClass)) columnPropertyComboBox.setEnabled(false);
                    else columnPropertyComboBox.setEnabled(true);
                }
             });
             columnSourceComboBox.setEditable(true);
             columnPropertyComboBox.setEditable(true);
         }

         /** Returns settings for a single column */
         public String[] getColumnInfo() {
             String source=(String)columnSourceComboBox.getSelectedItem();
             if (source==null) source="";
             String property=null;
             if (columnPropertyComboBox.isEnabled()) { // a disabled combobox means that the source is a NumericMap
                 property=(String)columnPropertyComboBox.getSelectedItem();
                 if (property==null) property=""; // property==null signals that the source is (probably) a NumericMap
                 else property=property.trim();
             }
             String weightString=(String)weightTextfield.getText();
             if (weightString!=null) weightString=weightString.trim();
             if (weightString.isEmpty()) weightString=null;
             try {
                 double value=Double.parseDouble(weightString);
                 if (value==1) weightString=null; // i.e. no explicit weight (1.0 is the default)
             } catch (NumberFormatException e) {}
             String direction=(String)directionComboBox.getSelectedItem();
             return new String[]{source.trim(),property,weightString,direction.toLowerCase()};
         }

    }
}
