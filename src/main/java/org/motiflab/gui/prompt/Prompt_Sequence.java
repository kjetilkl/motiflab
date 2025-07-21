package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Organism;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.gui.MiscIcons;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.SimpleDataPanelIcon;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class Prompt_Sequence extends Prompt {

    private javax.swing.JLabel errorLabel;
    private javax.swing.JTextField organismTextField;
    private javax.swing.JTextField buildTextField;    
    private javax.swing.JTextField chromosomeTextField;
    private javax.swing.JTextField startPositionTextField;
    private javax.swing.JTextField endPositionTextField;
    private javax.swing.JTextField geneTSSpositionTextField;
    private javax.swing.JTextField geneTESpositionTextField;
    private javax.swing.JTextField TSSrelativeStartTextField;
    private javax.swing.JTextField TSSrelativeEndTextField;
    private javax.swing.JTextField TESrelativeStartTextField;
    private javax.swing.JTextField TESrelativeEndTextField;   
    private javax.swing.JTextField geneTextField;


    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel standardPropertiesPanel; 
    private javax.swing.JPanel userPropertiesPanel;  
    private javax.swing.JTabbedPane tabbedpane;  
    
    private JTable userproptable;
    private GOEditorPanel GOPanel;
    private JButton addPropertyButton;    
    private VisualizationSettings settings=null;
    private MiscIcons directStrandIcon=null;
    private MiscIcons reverseStrandIcon=null;
    
     
    private Sequence data;
    
    public Prompt_Sequence(MotifLabGUI gui, String prompt, Sequence dataitem, VisualizationSettings settings) {
        this(gui,prompt,dataitem,settings,true);
    }    
    
    public Prompt_Sequence(MotifLabGUI gui, String prompt, Sequence dataitem, VisualizationSettings settings, boolean modal) {
        super(gui,prompt, modal);
        this.settings=settings;
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else data=null;
        disableNameEdit();
        setDataItemName(data.getName());
        setDataItemNameLabelText("Sequence ");
        SimpleDataPanelIcon coloricon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
        coloricon.setForegroundColor(settings.getSequenceLabelColor(data.getName()));
        setDataItemColorIcon(coloricon);
        directStrandIcon = new MiscIcons(MiscIcons.RIGHT_ARROW_LONG);
        reverseStrandIcon = new MiscIcons(MiscIcons.LEFT_ARROW_LONG);
        directStrandIcon.setForegroundColor(new Color(0,168,0));
        reverseStrandIcon.setForegroundColor(Color.RED);
        setTitle("Sequence");
        
        mainPanel=new JPanel();
        initComponents();
        setMainPanel(mainPanel);
        pack();  
        if (dataitem!=null) focusOKButton();
    }
 
    
    @Override
    public Data getData() {
        return data;
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof Sequence) data=(Sequence)newdata; 
    }     

    @Override
    public boolean onOKPressed() {  
        // parse user-defined properties
        int rows=userproptable.getRowCount();
        for (int i=0;i<rows;i++) {
           Object key=userproptable.getValueAt(i, 0);
           if (key==null || (key instanceof String && ((String)key).trim().isEmpty())) continue;
           if (!Sequence.isValidUserDefinedPropertyKey(key.toString())) {
               tabbedpane.setSelectedComponent(userPropertiesPanel);
               userproptable.setRowSelectionInterval(i, i);
               userproptable.scrollRectToVisible(userproptable.getCellRect(i,0,true));
               reportError("Not a valid property name: '"+key+"'");
               return false;
           }
           Object value=userproptable.getValueAt(i, 1);
           if (value==null) value=""; //
           if (value instanceof String && ((String)value).contains(";")) {
               tabbedpane.setSelectedComponent(userPropertiesPanel);
               userproptable.setRowSelectionInterval(i, i);
               userproptable.scrollRectToVisible(userproptable.getCellRect(i,0,true));
               reportError("Value for property '"+key+"' contains illegal character ';'");
               return false;               
           }
           value=Sequence.getObjectForPropertyValueString(value.toString());
           data.setUserDefinedPropertyValue(key.toString(), value);
        }
        // parse GO terms
        String[] termsAsArray=GOPanel.getGOterms();
        try {
           data.setGOterms(termsAsArray);
        } catch (ParseError p) {
           reportError(p.getMessage());
           return false;
        }
        // update gene name 
        String newGeneName=geneTextField.getText();
        if (newGeneName!=null) newGeneName=newGeneName.trim();
        if (!data.getGeneName().equals(newGeneName)) data.setGeneName(newGeneName); 
        // update genome build
        String newGenomeBuild=buildTextField.getText();
        if (newGenomeBuild!=null) newGenomeBuild=newGenomeBuild.trim();
        if (!data.getGenomeBuild().equals(newGenomeBuild)) {
            int organism=Organism.getOrganismForGenomeBuild(newGenomeBuild);
            if (organism>0) { // check that the genome build is actually recognized by MotifLab before updating
               data.setOrganism(organism); 
               data.setGenomeBuild(newGenomeBuild);
            }
        }   
        // update TSS
        String TSSstring=geneTSSpositionTextField.getText();
        if (TSSstring==null || TSSstring.trim().isEmpty()) data.setTSS(null);
        else try {
            int newTSS=(int)Double.parseDouble(TSSstring);
            if (data.getTSS()==null || data.getTSS()!=newTSS) data.setTSS(newTSS);
        } catch (NumberFormatException e) {}   
        // update TES
        String TESstring=geneTESpositionTextField.getText();
        if (TESstring==null || TESstring.trim().isEmpty()) data.setTES(null);
        else try {
            int newTES=(int)Double.parseDouble(TESstring);
            if (data.getTES()==null || data.getTES()!=newTES) data.setTES(newTES);
        } catch (NumberFormatException e) {}          
        
        return true;
    } // end "OK pressed"


    
    private void reportError(String msg) {
        if (msg==null) msg="NULL";
        errorLabel.setText(msg);
    }
    
    private void initComponents() {
        Border tabsBorder=BorderFactory.createRaisedBevelBorder();
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(Prompt_Sequence.class);
        
        setupStandardPropertiesPanel();
        standardPropertiesPanel.setBorder(tabsBorder);        
        
        setupUserDefinedPropertiesPanel();
        userPropertiesPanel.setBorder(tabsBorder);
        
        setupGOPanel();
        GOPanel.setBorder(tabsBorder);    
        
        errorLabel = new javax.swing.JLabel();
        errorLabel.setFont(resourceMap.getFont("errorLabel.font")); // NOI18N
        errorLabel.setForeground(resourceMap.getColor("errorLabel.foreground")); // NOI18N
        errorLabel.setText("   "); // NOI18N
        errorLabel.setName("errorLabel"); // NOI18N        

        tabbedpane = new javax.swing.JTabbedPane();               
        tabbedpane.addTab("Sequence", standardPropertiesPanel);
        tabbedpane.addTab("GO", GOPanel);
        tabbedpane.addTab("Properties", userPropertiesPanel);
        mainPanel.add(tabbedpane,BorderLayout.CENTER);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        JPanel errorPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        errorPanel.add(errorLabel);
        getControlsPanel().add(errorPanel,BorderLayout.CENTER);
    }
    
    private void setupStandardPropertiesPanel() {
       standardPropertiesPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));        
       if (data==null) return;
       JPanel internalPanel = new JPanel(new GridBagLayout());
       GridBagConstraints constraints=new GridBagConstraints();
       constraints.anchor = java.awt.GridBagConstraints.WEST;

       JPanel locationPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
       JPanel locationRelativetoTSSPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
       JPanel locationRelativetoTESPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
       JPanel organismBuildPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
       JPanel genePanel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
       chromosomeTextField=new JTextField(3); chromosomeTextField.setText(data.getChromosome());
       startPositionTextField=new JTextField(8); startPositionTextField.setText(""+data.getRegionStart());
       endPositionTextField=new JTextField(8); endPositionTextField.setText(""+data.getRegionEnd());       
       boolean direcOrientation=(data.getStrandOrientation()!=Sequence.REVERSE);
       
       locationPanel.add(new JLabel("chr "));
       locationPanel.add(chromosomeTextField);
       locationPanel.add(new JLabel("  :  "));
       locationPanel.add(startPositionTextField);
       JLabel strandLabel=new JLabel((direcOrientation)?directStrandIcon:reverseStrandIcon);
       strandLabel.setToolTipText((direcOrientation)?"Direct strand":"Reverse strand");
       locationPanel.add(strandLabel);
       locationPanel.add(endPositionTextField);
       locationPanel.add(new JLabel("   ("+data.getSize()+" bp)"));
       
       int organism=data.getOrganism();
       organismTextField=new JTextField(18);organismTextField.setText(Organism.getCommonName(organism)+" ("+Organism.getLatinName(organism) +")");
       buildTextField=new JTextField(6); buildTextField.setText(data.getGenomeBuild());
       geneTextField=new JTextField(12); if (data.getGeneName()!=null) geneTextField.setText(data.getGeneName());
       
       geneTSSpositionTextField=new JTextField(8); if (data.getTSS()!=null) geneTSSpositionTextField.setText(""+data.getTSS());
       geneTESpositionTextField=new JTextField(8); if (data.getTES()!=null) geneTESpositionTextField.setText(""+data.getTES());
       TSSrelativeStartTextField=new JTextField(8);
       TSSrelativeEndTextField=new JTextField(8);
       TESrelativeStartTextField=new JTextField(8);
       TESrelativeEndTextField=new JTextField(8);
       locationRelativetoTSSPanel.add(new JLabel("Relative to TSS: "));
       locationRelativetoTSSPanel.add(TSSrelativeStartTextField);
       locationRelativetoTSSPanel.add(new JLabel(" to "));
       locationRelativetoTSSPanel.add(TSSrelativeEndTextField);
       locationRelativetoTESPanel.add(new JLabel("Relative to TES: "));
       locationRelativetoTESPanel.add(TESrelativeStartTextField);
       locationRelativetoTESPanel.add(new JLabel(" to "));
       locationRelativetoTESPanel.add(TESrelativeEndTextField);       
              
//       JCheckBox skipZeroCheckBox=new JCheckBox("Skip 0",gui.skipPosition0());
//       skipZeroCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
//       skipZeroCheckBox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                setRelativeCoordinates(((JCheckBox)e.getSource()).isSelected());
//            }
//        });
//       locationRelativetoTSSPanel.add(skipZeroCheckBox);
       
       setRelativeCoordinates(gui.skipPosition0());
       
       organismBuildPanel.add(organismTextField);
       organismBuildPanel.add(new JLabel("    Genome build: "));
       organismBuildPanel.add(buildTextField);
       
       genePanel.add(geneTextField);
       genePanel.add(new JLabel("    TSS: "));
       genePanel.add(geneTSSpositionTextField);      
       genePanel.add(new JLabel(" TES: "));
       genePanel.add(geneTESpositionTextField);       
       
       constraints.insets = new java.awt.Insets(16, 8, 12, 0);           
       constraints.gridy = 0; constraints.gridx = 0;
       internalPanel.add(new JLabel("Location:"),constraints);       
       constraints.gridy = 0; constraints.gridx = 1;
       internalPanel.add(locationPanel,constraints);       
       
       constraints.insets = new java.awt.Insets(8, 8, 12, 0);      
    
       constraints.gridy = 1; constraints.gridx = 1;
       internalPanel.add(locationRelativetoTSSPanel,constraints);       
       constraints.gridy = 2; constraints.gridx = 1;
       internalPanel.add(locationRelativetoTESPanel,constraints);  
       
       constraints.insets = new java.awt.Insets(30, 8, 12, 0);
       
       constraints.gridy = 3; constraints.gridx = 0;
       internalPanel.add(new JLabel("Gene:"),constraints);       
       constraints.gridy = 3; constraints.gridx = 1;
       internalPanel.add(genePanel,constraints);         
       
       constraints.insets = new java.awt.Insets(8, 8, 12, 0);
       
       constraints.gridy = 4; constraints.gridx = 0;
       internalPanel.add(new JLabel("Organism:"),constraints);       
       constraints.gridy = 4; constraints.gridx = 1;
       internalPanel.add(organismBuildPanel,constraints);  
                       
       standardPropertiesPanel.add(internalPanel);
       
       organismTextField.setEditable(false);
       buildTextField.setEditable(true);    
       chromosomeTextField.setEditable(false);
       startPositionTextField.setEditable(false);
       endPositionTextField.setEditable(false);
       geneTSSpositionTextField.setEditable(true);
       geneTESpositionTextField.setEditable(true);
       TSSrelativeStartTextField.setEditable(false);
       TSSrelativeEndTextField.setEditable(false);
       TESrelativeStartTextField.setEditable(false);
       TESrelativeEndTextField.setEditable(false);   
       geneTextField.setEditable(true);
    }

    private void setRelativeCoordinates(boolean skipZero) {
       if (data.getTSS()!=null && data.getTES()!=null) {
           boolean direcOrientation=(data.getStrandOrientation()!=Sequence.REVERSE);
           String tss1=""+getPositionRelativeToAnchor(data.getRegionStart(),data.getTSS(),skipZero);
           String tss2=""+getPositionRelativeToAnchor(data.getRegionEnd(),data.getTSS(),skipZero);
           String tes1=""+getPositionRelativeToAnchor(data.getRegionStart(),data.getTES(),skipZero);
           String tes2=""+getPositionRelativeToAnchor(data.getRegionEnd(),data.getTES(),skipZero);
           TSSrelativeStartTextField.setText((direcOrientation)?tss1:tss2);
           TSSrelativeEndTextField.setText((direcOrientation)?tss2:tss1);
           TESrelativeStartTextField.setText((direcOrientation)?tes1:tes2);
           TESrelativeEndTextField.setText((direcOrientation)?tes2:tes1);
       }        
    }
    
    private void setupUserDefinedPropertiesPanel() {
        userPropertiesPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        String[] allProperties=Sequence.getAllUserDefinedProperties(engine);
        Object[][] propdata=new Object[allProperties.length][3]; // 3 columns: key and value and classtype
        for (int i=0;i<allProperties.length;i++) {
            //Object value=data.getUserDefinedPropertyValue(allProperties[i]);
            Class propclass=Sequence.getClassForUserDefinedProperty(allProperties[i],engine);
            propdata[i][0]=allProperties[i]; // key
            propdata[i][1]=(data!=null)?data.getUserDefinedPropertyValueAsType(allProperties[i],String.class):null; // convert to string for display purposes
            propdata[i][2]=propclass;          
        }
        final DefaultTableModel userproptablemodel=new DefaultTableModel(propdata, new String[]{"Property","Value","Type"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column<2; // type column is not editable
            }            
        };
        userproptable=new JTable(userproptablemodel);
        userproptable.setAutoCreateRowSorter(true);
        userproptable.getTableHeader().setReorderingAllowed(false);
        userproptable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // this will enable unfinished edits to be commited when table looses focus
        userproptable.setDefaultRenderer(Object.class, new UserDefinedPropertiesRenderer());
        userproptable.setCellSelectionEnabled(true);
        userproptablemodel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType()==TableModelEvent.UPDATE && e.getColumn()==1) { // update value column
                    Object stringvalue=userproptablemodel.getValueAt(e.getFirstRow(), 1);
                    Object value=null;
                    if (stringvalue!=null && !stringvalue.toString().trim().isEmpty()) value=Sequence.getObjectForPropertyValueString(stringvalue.toString());
                    if (value!=null) userproptablemodel.setValueAt(value.getClass(),e.getFirstRow(),2);
                    else userproptablemodel.setValueAt(null, e.getFirstRow(), 2);
                    userproptable.repaint();
                }
            }
        });
        JPanel internalpropspanel=new JPanel(new BorderLayout());
        JPanel propscontrolspanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        addPropertyButton=new JButton("Add Property");
        addPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               String[] newRowValues=new String[userproptable.getColumnCount()];
               for (int j=0;j<newRowValues.length;j++) newRowValues[j]="";
               newRowValues[newRowValues.length-1]=null;
               userproptablemodel.addRow(newRowValues);
               int newrow=getFirstEmptyRowInTable(userproptable);
               if (newrow>=0) {
                   boolean canedit=userproptable.editCellAt(newrow, 0);
                   if (canedit) {
                       userproptable.changeSelection(newrow, 0, false, false);
                       userproptable.requestFocus();
                   }
               }
            }
        });
        if (isDataEditable()) propscontrolspanel.add(addPropertyButton);
        internalpropspanel.add(new JScrollPane(userproptable),BorderLayout.CENTER);
        internalpropspanel.add(propscontrolspanel,BorderLayout.SOUTH);
        internalpropspanel.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        Dimension dim=new Dimension(500,290);
        internalpropspanel.setMinimumSize(dim);
        internalpropspanel.setPreferredSize(dim);
        //internalpropspanel.setMaximumSize(dim);
        userPropertiesPanel.add(internalpropspanel);
        userproptable.getRowSorter().toggleSortOrder(0);
    }
    
    private void setupGOPanel() {
        GOPanel=new GOEditorPanel(engine,  data.getGOterms());
    }

    private int getPositionRelativeToAnchor(int genomicCoordinate, int anchorPosition, boolean skipZero) {
        int coordinate=0;
        if (data.getStrandOrientation()==Sequence.DIRECT) {
            coordinate=genomicCoordinate-anchorPosition;
        } else {
            coordinate=anchorPosition-genomicCoordinate;
        }
        if (skipZero) {
           if (coordinate>=0) coordinate++;
        }
        return coordinate;
    }    
    
    private int getFirstEmptyRowInTable(JTable table) {
        for (int i=0;i<table.getRowCount();i++) {
            boolean empty=true;
            for (int j=0;j<table.getColumnCount();j++) {
                Object val=table.getValueAt(i, j);
                if (val!=null && !val.toString().isEmpty()) {empty=false;break;}
            }
            if (empty) return i;
        }
        return -1;
    }
    
    private class UserDefinedPropertiesRenderer extends DefaultTableCellRenderer {
         public UserDefinedPropertiesRenderer() {
             super();
             //this.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Object propKey=table.getValueAt(row, 0);
            Object propValue=table.getValueAt(row, 1);
            boolean keyDefined=(propKey!=null && !propKey.toString().trim().isEmpty());
            boolean valueDefined=(propValue!=null && !propValue.toString().trim().isEmpty());
            if (column==0) { // key column
                if (keyDefined) {
                    if (valueDefined) this.setForeground(Color.BLACK);
                    else this.setForeground(Color.LIGHT_GRAY);
                } else { // not key is given
                    if (valueDefined) {
                        this.setText("*** Missing property name ***");
                        this.setForeground(Color.RED);
                    }
                }
                setToolTipText((keyDefined)?propKey.toString():"*** Missing property name ***");
            } else if (column==1) { // value column
                if (valueDefined) {
                    String valuestring=value.toString();
                    if (valuestring.contains(",")) {
                        String split=valuestring.replaceAll("\\s*,\\s*", "<br>");   
                        setToolTipText("<html>"+split+"</html>");  
                    } else setToolTipText(valuestring);
                } else setToolTipText("");
            } else { // type column
                Object classTypeObject=table.getValueAt(row, 2);
                if (classTypeObject==null || !valueDefined || !keyDefined) this.setText("");
                if (classTypeObject instanceof Class) {
                    Class classType = (Class)classTypeObject;
                    if (classType.equals(ArrayList.class)) this.setText("List");
                    else if (Number.class.isAssignableFrom(classType)) this.setText("Numeric");
                    else if (classType.equals(String.class)) this.setText("Text");
                    else this.setText(classType.getSimpleName());
                } else this.setText("");
            }
            return this;
        }
    }
    
    
  
    
 
    @Override
    public void setDataEditable(boolean editable) {
        super.setDataEditable(editable);
        addPropertyButton.setVisible(editable);
        GOPanel.setEditable(editable);     
    }  
 
}
