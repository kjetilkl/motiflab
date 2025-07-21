/*
 
 
 */

package org.motiflab.gui.prompt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.gui.ColorMenu;
import org.motiflab.gui.ColorMenuListener;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.SimpleDataPanelIcon;
import org.motiflab.gui.SingleMotifTooltip;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author Kjetil
 */
public class Prompt_Module extends Prompt {
    private ModuleCRM data;
    
    private JTabbedPane tabbedpane;    
    private JPanel mainPanel;    
    private JPanel moduleStructurePanel;
    private GOEditorPanel GOPanel;     
    private JPanel userPropertiesPanel;        
    private JPanel addremoveMotifButtonsPanel;
    private JPanel motifControlsPanel;
    private JPanel distanceControlsPanel;    
    private JPanel controlsPanel;
    private JPanel globalPropertiesPanel;
    private JPanel motifsPanel;
    private JSpinner maxLengthSpinner;
    private JCheckBox preserveOrderCheckbox;
    private JCheckBox useMaxLengthCheckbox;
    private JButton addMotifButton;
    private JButton removeMotifButton;
    private VisualizationSettings settings;
    private JScrollPane moduleLogoScrollpane;
    private JPanel selectedPanel=null;
    private static Color selectedColor=new Color(220,220,255);
    
    private JTextField motifnamefield;
    private JTextField minDistanceField;
    private JTextField maxDistanceField;
    private JButton motifColorButton;
    private JButton selectMotifsButton;
    private JToggleButton directOrientationButton;
    private JToggleButton reverseOrientationButton;
    private JToggleButton anyOrientationButton;
    private ButtonGroup orientationgroup;
    private JLabel motifnamelabel;
    private JLabel mindistlabel;
    private JLabel maxdistlabel;
    private JPopupMenu selectMotifColorMenu;
    
    private JTable userproptable;   
    private JButton addPropertyButton;    
    private JLabel errorLabel;    
    
    
    
    public Prompt_Module(MotifLabGUI gui, String prompt, ModuleCRM dataitem) {
        this(gui,prompt,dataitem,true);
    }

    public Prompt_Module(MotifLabGUI gui, String prompt, ModuleCRM dataitem, boolean modal) {
        super(gui,prompt, modal);
        this.settings=gui.getVisualizationSettings();
        if (dataitem!=null)  {
            data=dataitem;
            setExistingDataItem(dataitem);
        }
        else {
            data=new ModuleCRM(gui.getGenericDataitemName(ModuleCRM.class, null));
            data.setOrdered(true);
        }
        setDataItemName(data.getName());
        setDataItemNameLabelText("Module ID  ");
        setTitle("Module");
        SimpleDataPanelIcon moduleicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
        moduleicon.setForegroundColor(settings.getFeatureColor(data.getName()));
        setDataItemColorIcon(moduleicon);
        moduleStructurePanel=new JPanel();
        moduleStructurePanel.setLayout(new BorderLayout());
        globalPropertiesPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        useMaxLengthCheckbox=new JCheckBox("Max span (bp)");
        useMaxLengthCheckbox.setHorizontalTextPosition(SwingConstants.RIGHT);
        globalPropertiesPanel.add(useMaxLengthCheckbox);
        maxLengthSpinner=new JSpinner(new SpinnerNumberModel(data.getMaxLength(), 0, 100000, 1));
        preserveOrderCheckbox=new JCheckBox("Motifs must appear in order",data.isOrdered());
        preserveOrderCheckbox.setHorizontalTextPosition(SwingConstants.RIGHT);
        preserveOrderCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (preserveOrderCheckbox.isSelected()) {
                   updateSelectedPanel();
                } else {
                   maxDistanceField.setText(""); maxDistanceField.setEnabled(false);
                   minDistanceField.setText(""); minDistanceField.setEnabled(false);
                   mindistlabel.setEnabled(false);
                   maxdistlabel.setEnabled(false);                     
                }
            }
        });
        globalPropertiesPanel.add(maxLengthSpinner);
        globalPropertiesPanel.add(new JLabel("       "));
        globalPropertiesPanel.add(preserveOrderCheckbox);
        motifsPanel=new JPanel() {
            public int getHeight() {
                return moduleLogoScrollpane.getHeight()-8;
            }
        };
        initMotifsPanel();
        moduleLogoScrollpane=new JScrollPane(motifsPanel);
        moduleLogoScrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        moduleLogoScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        addremoveMotifButtonsPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addMotifButton=new JButton("Add motif");
        addMotifButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addMotif();
            }
        });
        removeMotifButton=new JButton("Remove motif");
        removeMotifButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeMotif();
            }
        });
        addremoveMotifButtonsPanel.add(new JLabel("      "));
        addremoveMotifButtonsPanel.add(addMotifButton);
        addremoveMotifButtonsPanel.add(removeMotifButton); 
        if (isDataEditable()) {
            globalPropertiesPanel.add(addremoveMotifButtonsPanel);
        }
        motifControlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
        motifnamelabel=new JLabel("Name ");
        motifControlsPanel.add(motifnamelabel);
        motifnamefield=new JTextField(16);
        motifnamefield.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedPanel instanceof MotifRepresentationPanel) {
                    String newname=motifnamefield.getText();
                    if (newname!=null) newname=newname.trim();
                    if (newname.equals(((MotifRepresentationPanel)selectedPanel).getMotifName())) return; // no change in the name
                    boolean ok=true;
                    if (newname!=null && !newname.isEmpty() && !isNameInUse(newname, ((MotifRepresentationPanel)selectedPanel))) {
                       ok=((MotifRepresentationPanel)selectedPanel).setMotifName(newname.trim());    
                    } else ok=false;
                    if (!ok) JOptionPane.showMessageDialog(selectedPanel, "The name is illegal or already in use", "Error", JOptionPane.ERROR_MESSAGE);
                    motifsPanel.revalidate();
                }
            }
        });        
        motifControlsPanel.add(motifnamefield);
        motifControlsPanel.add(new JLabel("    "));
        selectMotifsButton=new JButton("Select Motifs");
        selectMotifsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedPanel instanceof MotifRepresentationPanel) {
                    ((MotifRepresentationPanel)selectedPanel).promptMotifs();
                }
            }
        });
        motifControlsPanel.add(selectMotifsButton);
        motifControlsPanel.add(new JLabel("    "));        
        motifColorButton=new JButton("Color");
        motifControlsPanel.add(motifColorButton);
        final ColorMenuListener selectcolorlistener=new ColorMenuListener() {
            @Override
            public void newColorSelected(Color color) {
                if (color!=null && selectedPanel instanceof MotifRepresentationPanel){
                    settings.setFeatureColor(data.getName()+"."+((MotifRepresentationPanel)selectedPanel).getMotifName(), color, true);
                    ((MotifRepresentationPanel)selectedPanel).repaint();
                    updateSelectedPanel();
                }                   
            }
        };
        ColorMenu temp=new ColorMenu("Select Motif Color", selectcolorlistener, gui.getFrame());
        selectMotifColorMenu=temp.wrapInPopup();
        motifColorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectMotifColorMenu.show(motifColorButton, 10, 10);
            }
        });
        motifControlsPanel.add(new JLabel("    "));
        directOrientationButton=new JToggleButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage(Prompt_Module.class.getResource("resources/directArrow.gif"))));
        reverseOrientationButton=new JToggleButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage(Prompt_Module.class.getResource("resources/reverseArrow.gif"))));
        anyOrientationButton=new JToggleButton(new ImageIcon(Toolkit.getDefaultToolkit().getImage(Prompt_Module.class.getResource("resources/anyArrow.gif"))));
        directOrientationButton.setToolTipText("Direct orientation");
        reverseOrientationButton.setToolTipText("Reverse orientation");
        anyOrientationButton.setToolTipText("Any orientation");
        
        ActionListener orientationActionListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedPanel instanceof MotifRepresentationPanel) {
                         if (e.getSource()==directOrientationButton) ((MotifRepresentationPanel)selectedPanel).setMotifOrientation(ModuleCRM.DIRECT);
                    else if (e.getSource()==reverseOrientationButton) ((MotifRepresentationPanel)selectedPanel).setMotifOrientation(ModuleCRM.REVERSE);
                    else if (e.getSource()==anyOrientationButton) ((MotifRepresentationPanel)selectedPanel).setMotifOrientation(ModuleCRM.INDETERMINED);
                    motifsPanel.repaint();                            
                }
            }
        };
        directOrientationButton.addActionListener(orientationActionListener);
        reverseOrientationButton.addActionListener(orientationActionListener);
        anyOrientationButton.addActionListener(orientationActionListener);
        motifControlsPanel.add(directOrientationButton);
        motifControlsPanel.add(reverseOrientationButton);
        motifControlsPanel.add(anyOrientationButton);
        orientationgroup=new ButtonGroup();
        orientationgroup.add(directOrientationButton);
        orientationgroup.add(reverseOrientationButton);
        orientationgroup.add(anyOrientationButton);
        
        distanceControlsPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));        
        minDistanceField = new JTextField(6);
        maxDistanceField = new JTextField(6);
        mindistlabel=new JLabel("Min distance ");
        maxdistlabel=new JLabel("   Max distance ");
        distanceControlsPanel.add(mindistlabel);
        distanceControlsPanel.add(minDistanceField);
        distanceControlsPanel.add(maxdistlabel);
        distanceControlsPanel.add(maxDistanceField);
        ActionListener constraintsActionListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commitDistanceConstraints();
            }
        };        
        FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                commitDistanceConstraints();
            }          
        };
        minDistanceField.addActionListener(constraintsActionListener);
        maxDistanceField.addActionListener(constraintsActionListener);  
        minDistanceField.addFocusListener(focusAdapter);
        maxDistanceField.addFocusListener(focusAdapter);
        controlsPanel=new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.add(globalPropertiesPanel);
        controlsPanel.add(motifControlsPanel);
        controlsPanel.add(distanceControlsPanel);        
               
        moduleStructurePanel.add(moduleLogoScrollpane, BorderLayout.CENTER);
        moduleStructurePanel.add(controlsPanel, BorderLayout.SOUTH);
        
        
        
        controlsPanel.setVisible(isDataEditable());
        addremoveMotifButtonsPanel.setVisible(isDataEditable());        
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        tabbedpane = new JTabbedPane();        
        mainPanel.add(tabbedpane);
        setupUserDefinedPropertiesPanel();      
        setupGOPanel();         
              
        tabbedpane.addTab("Module", moduleStructurePanel);
        tabbedpane.addTab("GO", GOPanel);
        tabbedpane.addTab("Properties", userPropertiesPanel);
        Border tabsBorder=BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),BorderFactory.createEmptyBorder(8,14,8,14));        
        moduleStructurePanel.setBorder(tabsBorder);
        GOPanel.setBorder(tabsBorder);        
        userPropertiesPanel.setBorder(tabsBorder);
        
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(Prompt_Motif.class);                
        errorLabel=new JLabel();
        errorLabel.setFont(resourceMap.getFont("errorLabel.font")); // NOI18N
        errorLabel.setForeground(resourceMap.getColor("errorLabel.foreground")); // NOI18N
        errorLabel.setText("   "); // NOI18N
        errorLabel.setName("errorLabel"); // NOI18N        
        mainPanel.add(errorLabel,BorderLayout.SOUTH);            
        setMainPanel(mainPanel);
        
        
        Dimension d=new Dimension(660,370);
        this.setMinimumSize(d);
        pack();
        // check if the dialog window fits. If not resize it
        Dimension dim=getSize();
        Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
        if (dim.height>screen.height-100) {
            setSize(screen.height-100,dim.width);
        }
        if (motifsPanel.getComponentCount()==0) removeMotifButton.setEnabled(false);
        useMaxLengthCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (useMaxLengthCheckbox.isSelected()) maxLengthSpinner.setEnabled(true);
                else {
                    maxLengthSpinner.setEnabled(false);                  
                }
            }
        });
        preserveOrderCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                motifsPanel.repaint(); // shows or hides distance constraints
            }
        });
        if (data.getMaxLength()==0) {
            useMaxLengthCheckbox.setSelected(false);
            maxLengthSpinner.setEnabled(false);
        }
        else {
            useMaxLengthCheckbox.setSelected(true);
            maxLengthSpinner.setEnabled(true);
        }
        if (dataitem!=null) focusOKButton();
        selectPanel(null);       
    }

    private void setupGOPanel() {
        GOPanel=new GOEditorPanel(engine,  data.getGOterms());
    }    
    
    private void setupUserDefinedPropertiesPanel() {
        userPropertiesPanel=new JPanel(new FlowLayout(FlowLayout.CENTER));
        String[] allProperties=ModuleCRM.getAllUserDefinedProperties(engine);
        Object[][] propdata=new Object[allProperties.length][3]; // 3 columns: key and value and classtype
        for (int i=0;i<allProperties.length;i++) {
            //Object value=data.getUserDefinedPropertyValue(allProperties[i]);
            Class propclass=ModuleCRM.getClassForUserDefinedProperty(allProperties[i],engine);
            propdata[i][0]=allProperties[i]; // key
            propdata[i][1]=data.getUserDefinedPropertyValueAsType(allProperties[i],String.class); // convert to string for display purposes
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
                    if (stringvalue!=null && !stringvalue.toString().trim().isEmpty()) value=ModuleCRM.getObjectForPropertyValueString(stringvalue.toString());
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
        internalpropspanel.setMaximumSize(dim);
        userPropertiesPanel.add(internalpropspanel);
        userproptable.getRowSorter().toggleSortOrder(0);
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
        addremoveMotifButtonsPanel.setVisible(editable);
        motifControlsPanel.setVisible(editable);
        distanceControlsPanel.setVisible(editable);
        preserveOrderCheckbox.setEnabled(editable);
        maxLengthSpinner.setEnabled(editable);
        useMaxLengthCheckbox.setEnabled(editable);
        if (editable && useMaxLengthCheckbox.isSelected()) maxLengthSpinner.setEnabled(true);
        else maxLengthSpinner.setEnabled(false);
        addPropertyButton.setVisible(editable);
        GOPanel.setEditable(editable);         
    }

    @Override
    public Data getData() {
        return data;
    }
    
    @Override
    public void setData(Data newdata) {
       if (newdata instanceof ModuleCRM) data=(ModuleCRM)newdata; 
    }     

    private void commitDistanceConstraints() {
        if (selectedPanel instanceof DistancePanel) {
            String minString=minDistanceField.getText();
            String maxString=maxDistanceField.getText();
            if (minString==null) minString="";
            if (maxString==null) maxString="";
            int min=Integer.MIN_VALUE;
            int max=Integer.MAX_VALUE;
            try {
                min=Integer.parseInt(minString);
            } catch (NumberFormatException nfe) {}
            try {
                max=Integer.parseInt(maxString);
            } catch (NumberFormatException nfe) {}
            if (min==Integer.MIN_VALUE && max==Integer.MAX_VALUE) ((DistancePanel)selectedPanel).clearDistanceConstraint();
            else ((DistancePanel)selectedPanel).setDistanceConstraint(min, max);
            updateSelectedPanel(); 
            motifsPanel.repaint();
        }        
    }
    
    /** */
    private void initMotifsPanel() {
         BoxLayout boxlayout=new BoxLayout(motifsPanel, BoxLayout.X_AXIS);
         motifsPanel.setLayout(boxlayout);
         motifsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
         motifsPanel.setBackground(Color.WHITE);
         motifsPanel.setMinimumSize(new Dimension(120,90));
         motifsPanel.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
         motifsPanel.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {}
                @Override
                public void mousePressed(MouseEvent e) {
                    selectPanel(null);              
                }
                @Override
                public void mouseReleased(MouseEvent e) {}
                @Override
                public void mouseEntered(MouseEvent e) {}
                @Override
                public void mouseExited(MouseEvent e) {}
            });         

         int numberofmotifs=data.size();
         for (int i=0;i<numberofmotifs;i++) {
             String previousmotif=(i==0)?null:data.getSingleMotifName(i-1);
             String motifname=data.getSingleMotifName(i);
             int orientation=data.getMotifOrientation(i);
             MotifCollection motifcollection=data.getMotifAsCollection(i);
             int[] distance=(i==0)?null:data.getDistance(previousmotif, motifname);
             if (i>0) {
                 boolean isConstrained=(distance!=null);
                 motifsPanel.add(new DistancePanel((distance!=null)?distance[0]:0, (distance!=null)?distance[1]:0,isConstrained));
             }
             motifsPanel.add(new MotifRepresentationPanel(motifname, orientation, motifcollection));
         }
    }


    private void addMotif() {
        int count=motifsPanel.getComponentCount();
        count=(int)((count+1)/2); // counts motif panels but excludes distance panels
        count++;
        String newmotifname="Motif"+count;
        while (isMotifNameInUse(newmotifname)) {
            count++;
            newmotifname="Motif"+count;
        }
        MotifRepresentationPanel newmotifpanel=new MotifRepresentationPanel(newmotifname, ModuleCRM.INDETERMINED, new MotifCollection(newmotifname+"Col"));
        if (motifsPanel.getComponentCount()>0) {
            DistancePanel distancepanel=new DistancePanel(0,0,false);
            motifsPanel.add(distancepanel);
        }
        motifsPanel.add(newmotifpanel);
        motifsPanel.validate();
        moduleLogoScrollpane.validate();
    }

    private void removeMotif() {
        if (selectedPanel instanceof MotifRepresentationPanel) {
            int selectedIndex=-1;
            for (int i=0;i<motifsPanel.getComponentCount();i++) {
                if (motifsPanel.getComponent(i)==selectedPanel) {selectedIndex=i;break;}
            }
            if (selectedIndex<0) return;
            if (selectedIndex==0) { // removing first motif
                if (motifsPanel.getComponentCount()>1) motifsPanel.remove(1); // more than one motif? remove first distance constraint also
                motifsPanel.remove(0);
            } else { // not first motif. 
                motifsPanel.remove(selectedIndex); // remove motif
                motifsPanel.remove(selectedIndex-1); // remove distance constraint before it
            }       
            motifsPanel.validate();
            moduleLogoScrollpane.validate();
            motifsPanel.repaint();
            selectPanel(null);
        }
    }
    
    /** Returns TRUE if the specified name is used by a module motif */
    private boolean isMotifNameInUse(String name) {
        for (int i=0;i<motifsPanel.getComponentCount();i++) {
            Component panel=motifsPanel.getComponent(i);
            if (panel instanceof MotifRepresentationPanel) {
               if (((MotifRepresentationPanel)panel).getMotifName().equals(name)) return true;                 
            }
        }
        return false;
    }

    @Override
    public boolean onOKPressed() {
        // check that everything is ok an update the dataitem based on the new settings
        for (int i=0;i<motifsPanel.getComponentCount();i++) {
            JPanel p=(JPanel)motifsPanel.getComponent(i);
            if (p instanceof MotifRepresentationPanel) {
                if ( ((MotifRepresentationPanel)p).getMotifsAsCollection().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No single motif models specified for '"+((MotifRepresentationPanel)p).getMotifName()+"'", "Missing motifs", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        // Everything OK with the data
        data.clear(); // start from scratch. It is probably easier
        data.setOrdered(preserveOrderCheckbox.isSelected());
        if (useMaxLengthCheckbox.isSelected()) data.setMaxLength((Integer)maxLengthSpinner.getValue());
        else data.setMaxLength(0);
        for (int i=0;i<motifsPanel.getComponentCount();i++) {
            JPanel p=(JPanel)motifsPanel.getComponent(i);
            if (p instanceof MotifRepresentationPanel) {
                MotifRepresentationPanel motifpanel=(MotifRepresentationPanel)p;
                data.addModuleMotif(motifpanel.getMotifName(), motifpanel.getMotifsAsCollection(), motifpanel.getMotifOrientation());
            }
        }
        if (preserveOrderCheckbox.isSelected() || motifsPanel.getComponentCount()<=3) {// distance constraints really only apply if the order is important
            for (int i=0;i<motifsPanel.getComponentCount();i++) {
                JPanel p=(JPanel)motifsPanel.getComponent(i);
                if (p instanceof DistancePanel) {
                    DistancePanel distancepanel=(DistancePanel)p;
                    int[] limits=distancepanel.getDistance();
                    int lowermotif=(int)(i/2);
                    if (limits!=null) try {
                        data.addDistanceConstraint(lowermotif, lowermotif+1, limits[0], limits[1]);
                    } catch (Exception e) {System.err.println("SYSTEM ERROR: Prompt_Module: adding distance constraint:"+e.getMessage());}
                }
            }
        }
        // parse user-defined properties
        int rows=userproptable.getRowCount();
        for (int i=0;i<rows;i++) {
           Object key=userproptable.getValueAt(i, 0);
           if (key==null || (key instanceof String && ((String)key).trim().isEmpty())) continue;
           if (!ModuleCRM.isValidUserDefinedPropertyKey(key.toString())) {
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
           value=ModuleCRM.getObjectForPropertyValueString(value.toString());
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
        String newName=getDataItemName();
        if (!data.getName().equals(newName)) data.rename(newName);
        return true;
    }

    private void reportError(String msg) {
        if (msg==null) msg="NULL";
        errorLabel.setText(msg);
    }    
    
    private String cleanUpMotifName(String name) {
         String newname=name.trim().replaceAll("^\\w\\$", ""); // remove V$, I$, P$ etc at the beginning of Transfac names
         newname=newname.replaceAll("\\W", "_"); // replace non-word characters with underscore
         newname=newname.replaceAll("__+", "_"); // replace runs of underscores with single underscore
         while (newname.startsWith("_")) newname=newname.substring(1); // remove prefixing underscores
         while (newname.endsWith("_")) newname=newname.substring(0,newname.length()-1); // remove trailing underscores
         return newname;
    }

    /** Used to set the currently selected panel (MotifRepresentationPanel or DistancePanel) 
     *  and updated controls accordingly
     */
    private void selectPanel(JPanel panel) {
       selectedPanel=panel;     
       motifnamefield.setText(""); motifnamefield.setEnabled(false);
       maxDistanceField.setText(""); maxDistanceField.setEnabled(false);
       minDistanceField.setText(""); minDistanceField.setEnabled(false);
       mindistlabel.setEnabled(false);
       maxdistlabel.setEnabled(false);       
       orientationgroup.clearSelection();
       directOrientationButton.setEnabled(false);
       reverseOrientationButton.setEnabled(false);
       anyOrientationButton.setEnabled(false);
       motifColorButton.setEnabled(false);
       selectMotifsButton.setEnabled(false);  
       motifnamelabel.setEnabled(false);
       removeMotifButton.setEnabled(false);
       if (panel instanceof DistancePanel && preserveOrderCheckbox.isSelected()) {
           minDistanceField.setEnabled(true);   
           maxDistanceField.setEnabled(true);  
           mindistlabel.setEnabled(true);
           maxdistlabel.setEnabled(true);
           int[] constraint=((DistancePanel)panel).getDistance();
           if (constraint!=null) {
               if (constraint[0]==Integer.MIN_VALUE) minDistanceField.setText(""); 
               else minDistanceField.setText(""+constraint[0]);
               if (constraint[1]==Integer.MAX_VALUE) maxDistanceField.setText(""); 
               else maxDistanceField.setText(""+constraint[1]);
           }
           motifColorButton.setBackground(null);
           motifColorButton.setForeground(null);        
       } else if (panel instanceof MotifRepresentationPanel) {
           removeMotifButton.setEnabled(true);
           motifnamelabel.setEnabled(true);
           directOrientationButton.setEnabled(true);
           reverseOrientationButton.setEnabled(true);
           anyOrientationButton.setEnabled(true);
           motifnamefield.setEnabled(true);
           motifColorButton.setEnabled(true);
           selectMotifsButton.setEnabled(true);
           String motifname=((MotifRepresentationPanel)panel).getMotifName();
           motifnamefield.setText(motifname);
           int orientation=((MotifRepresentationPanel)panel).getMotifOrientation();
           if (orientation==ModuleCRM.DIRECT) directOrientationButton.setSelected(true);
           else if (orientation==ModuleCRM.REVERSE) reverseOrientationButton.setSelected(true);
           else anyOrientationButton.setSelected(true);
           Color moduleColor=settings.getFeatureColor(data.getName()+"."+motifname);  
           motifColorButton.setBackground(moduleColor);
           if (VisualizationSettings.isDark(moduleColor)) motifColorButton.setForeground(Color.WHITE);
           else motifColorButton.setForeground(Color.BLACK);
       } else {
           motifColorButton.setBackground(null);
           motifColorButton.setForeground(null); 
       }
       motifsPanel.repaint();
    }
    
    private void updateSelectedPanel() {
       selectPanel(selectedPanel); // just set it selected once more to update 
    }
    
    /**
     * This panel paints motifs a single box
     */
    private class MotifRepresentationPanel extends JPanel {
        private String motifname="?";
        private int orientation;
        private MotifCollection motifs; //
        private boolean mouseEntered=false;
        private SingleMotifTooltip tooltip=new SingleMotifTooltip(settings);
        private int width=40;

        public MotifRepresentationPanel(String motifname, int orientation, MotifCollection motifs) {
            this.motifname=motifname;
            this.orientation=orientation;
            this.motifs=motifs;
            int w=this.getFontMetrics(getFont()).stringWidth(getMotifLabel());
            if (w+10>width) width=w+10;
            this.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!isDataEditable()) return;
                    if (selectedPanel!=MotifRepresentationPanel.this) selectPanel(MotifRepresentationPanel.this);                                  
                    if (e.getClickCount()==2) promptMotifs();
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isDataEditable()) return;
                    if (selectedPanel!=MotifRepresentationPanel.this) selectPanel(MotifRepresentationPanel.this);              
                    if (e.isPopupTrigger() && e.getClickCount()==1) showMotifContextMenu(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!isDataEditable()) return;
                    if (selectedPanel!=MotifRepresentationPanel.this) selectPanel(MotifRepresentationPanel.this);              
                    if (e.isPopupTrigger() && e.getClickCount()==1) showMotifContextMenu(e);
                }
                @Override
                public void mouseEntered(MouseEvent e) {mouseEntered=true;repaint();}
                @Override
                public void mouseExited(MouseEvent e) {mouseEntered=false;repaint();}
            });
            updateTooltipText();
        }

        public int getWidth() {return width;}
        public int getHeight() {return motifsPanel.getHeight();}
        public Dimension getSize() {return new Dimension(width,motifsPanel.getHeight());}
        public Dimension getPreferredSize() {return new Dimension(width,motifsPanel.getHeight());}        
        public Dimension getMaximumSize() {return new Dimension(width,motifsPanel.getHeight());}        
        
        @Override
        public JToolTip createToolTip() {
            return tooltip;
        }

        public String getMotifName() {
            return motifname;
        }

        public boolean setMotifName(String newname) {
            Color oldcolor=settings.getFeatureColor(data.getName()+"."+motifname); // do not change color just because we changed name, so 
            newname=cleanUpMotifName(newname);
            if (newname.isEmpty()) return false; // the name (after cleanup) is no longer valid
            while (isMotifNameInUse(newname))
            if (isMotifNameInUse(newname)) { // come up with a new name
                int counter=2;
                while (isMotifNameInUse(newname+"_"+counter)) counter++;
                newname=newname+"_"+counter;
            } 
            motifname=newname;
            settings.setFeatureColor(data.getName()+"."+motifname,oldcolor,false);       
            updateTooltipText();
            updateSelectedPanel();
            repaint();
            return true;
        }


        public void setMotifOrientation(int neworientation) {
            orientation=neworientation;
            repaint();
        }
        public int getMotifOrientation() {
            return orientation;
        }

        public MotifCollection getMotifsAsCollection() {
            return motifs;
        }


        private void showMotifContextMenu(MouseEvent e) {
            JPopupMenu menu=new JPopupMenu();
            JMenuItem item1=new JMenuItem("Select motifs");
            JMenu item2=new JMenu("Set orientation");
            JMenuItem item3=new JMenuItem("Direct");
            JMenuItem item4=new JMenuItem("Reverse");
            JMenuItem item5=new JMenuItem("Any");
            JMenuItem item6=new JMenuItem("Rename");
            item2.add(item3);
            item2.add(item4);
            item2.add(item5);
            MotifContextMenuListener listener=new MotifContextMenuListener(this);
            item1.addActionListener(listener);
            item3.addActionListener(listener);
            item4.addActionListener(listener);
            item5.addActionListener(listener);
            item6.addActionListener(listener);
            menu.add(item6);
            menu.add(item1);
            menu.add(item2);
            final ColorMenuListener colormenulistener=new ColorMenuListener() {
                @Override
                public void newColorSelected(Color color) {
                    if (color!=null){
                        gui.getVisualizationSettings().setFeatureColor(data.getName()+"."+MotifRepresentationPanel.this.getMotifName(), color, true);
                        MotifRepresentationPanel.this.repaint();
                        updateSelectedPanel();
                    }                   
                }
            };
            ColorMenu colorMenu=new ColorMenu("Set color",colormenulistener,gui.getFrame());
            menu.add(colorMenu);

            menu.show(e.getComponent(), e.getX(),e.getY());
        }


        public void promptMotifs() {
            Prompt_MotifCollection prompt=new Prompt_MotifCollection(gui, null, motifs.clone(),true, "Select motifs", true);
            prompt.setHeaderVisible(false);
            prompt.setLocation(gui.getFrame().getWidth()/2-prompt.getWidth()/2, gui.getFrame().getHeight()/2-prompt.getHeight()/2);
            prompt.setVisible(true);
            if (prompt.isOKPressed()) {
                MotifCollection newcol=(MotifCollection)prompt.getData();
                if (newcol.isEmpty()) JOptionPane.showMessageDialog(this, "You must select at least one motif","Empty collection error",JOptionPane.ERROR_MESSAGE);
                if (!newcol.containsSameData(motifs)) { // collections has been changed
                    motifs=newcol;
                    if (motifs.size()==1 && motifname.startsWith("Motif")) { // 
                        Motif motif=motifs.getMotifByIndex(0, engine);
                        String shortname=motif.getShortName();
                        if (shortname!=null && !shortname.isEmpty()) setMotifName(Motif.cleanUpMotifShortName(shortname,true));
                        Color color=settings.getFeatureColor(motif.getName());
                        settings.setFeatureColor(data.getName()+"."+getMotifName(), color, false); // use the same color as the single motif
                    }
                }
            }
            prompt.dispose();
            updateTooltipText();
            updateSelectedPanel();
            repaint();
        }

        public final void updateTooltipText() {          
            if (motifs.isEmpty()) setToolTipText("No motifs selected");
            else {
              StringBuilder builder=new StringBuilder("#M#");
              builder.append(motifname);
              builder.append(":");
              int i=0;
              for (String m:motifs.getAllMotifNames()) {
                if (i>0) builder.append(",");
                builder.append(m);
                i++;
              }
              setToolTipText(builder.toString());
            }

        }


        @Override
        public void paintComponent(Graphics g) {
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            int height=getHeight();
            int yoffset=(int)(height/2.0)-20;
            if (yoffset<30) yoffset=30;
            if (selectedPanel==this) g.setColor(selectedColor);       
            else g.setColor(Color.WHITE);  
            g.fillRect(0, 0, width, height);            
            Color moduleColor=settings.getFeatureColor(data.getName()+"."+motifname);
            g.setColor(moduleColor);
            g.fillRect(0, yoffset, width-1, 20);
            if (mouseEntered) g.setColor(Color.RED); else g.setColor(Color.BLACK);
            g.drawRect(0, yoffset, width-1, 20);
            // draw arrow
            g.setColor(Color.BLACK);
            if (orientation!=ModuleCRM.INDETERMINED) { // remove this condition to paint <-> arrows for INDETERMINED orientations
                g.fillRect(2, yoffset-10, width-4, 5);
                if (orientation==ModuleCRM.DIRECT) g.setColor(Color.GREEN);
                else if (orientation==ModuleCRM.REVERSE) g.setColor(Color.RED);
                else g.setColor(Color.YELLOW);
                g.fillRect(3, yoffset-9, width-6, 3);
            }
            if (orientation==ModuleCRM.DIRECT) { // if (orientation==ModuleCRM.DIRECT || orientation==ModuleCRM.INDETERMINED)
                g.setColor(Color.BLACK);
                g.drawLine(width-1, yoffset-8, width-1, yoffset-8);
                g.drawLine(width-2, yoffset-9, width-2, yoffset-7);
                g.drawLine(width-3, yoffset-10, width-3, yoffset-6);
                g.drawLine(width-4, yoffset-11, width-4, yoffset-5);
                g.drawLine(width-5, yoffset-12, width-5, yoffset-4);
                g.drawLine(width-6, yoffset-13, width-6, yoffset-3);
                if (orientation==ModuleCRM.DIRECT) g.setColor(Color.GREEN); else g.setColor(Color.YELLOW);
                g.drawLine(width-2, yoffset-8, width-2, yoffset-8);
                g.drawLine(width-3, yoffset-9, width-3, yoffset-7);
                g.drawLine(width-4, yoffset-10, width-4, yoffset-6);
                g.drawLine(width-5, yoffset-11, width-5, yoffset-5);
                g.drawLine(width-6, yoffset-9, width-6, yoffset-7);
            }
            if (orientation==ModuleCRM.REVERSE) { // if (orientation==ModuleCRM.REVERSE || orientation==ModuleCRM.INDETERMINED)
                g.setColor(Color.BLACK);
                g.drawLine(0, yoffset-8, 0, yoffset-8);
                g.drawLine(1, yoffset-9, 1, yoffset-7);
                g.drawLine(2, yoffset-10, 2, yoffset-6);
                g.drawLine(3, yoffset-11, 3, yoffset-5);
                g.drawLine(4, yoffset-12, 4, yoffset-4);
                g.drawLine(5, yoffset-13, 5, yoffset-3);
                if (orientation==ModuleCRM.REVERSE) g.setColor(Color.RED); else g.setColor(Color.YELLOW);
                g.drawLine(1, yoffset-8, 1, yoffset-8);
                g.drawLine(2, yoffset-9, 2, yoffset-7);
                g.drawLine(3, yoffset-10, 3, yoffset-6);
                g.drawLine(4, yoffset-11, 4, yoffset-5);
                g.drawLine(5, yoffset-9, 5, yoffset-7);
            }
            if (isDark(moduleColor)) g.setColor(Color.WHITE); else g.setColor(Color.BLACK);
            String usename=getMotifLabel();
            int stringwidth=g.getFontMetrics().stringWidth(usename);
            if (stringwidth>width-10 || (stringwidth+10<width && width>40)) {
                int newwidth=stringwidth+10;
                if (newwidth<40) newwidth=40;
                width=newwidth;
                motifsPanel.revalidate();
                return;
            }
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawString(usename, (int)((width-stringwidth)/2f),yoffset+14);
            //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        private String getMotifLabel() {
            if (motifs.isEmpty()) return "* "+motifname+" *";
            else return motifname;
        }
    }



    /**
     * This panel paints distance constraints between motifs
     */
    private class DistancePanel extends JPanel {
        private int min=0;
        private int max=0;
        private boolean distanceIsConstrained=false;
        private boolean mouseEntered=false;
        private int width=50;

        public DistancePanel(int min, int max, boolean distanceIsConstrained) {
            this.min=min;
            this.max=max;
            this.distanceIsConstrained=distanceIsConstrained;
            int w=this.getFontMetrics(getFont()).stringWidth(getConstraintAsString());
            if (w+20>width) width=w+20;
            this.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!isDataEditable()) return;
                    if (selectedPanel!=DistancePanel.this) selectPanel(DistancePanel.this);
                    if (e.getClickCount()==2 && preserveOrderCheckbox.isSelected()) promptDistance();
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isDataEditable()) return;
                   if (selectedPanel!=DistancePanel.this) selectPanel(DistancePanel.this);                              
                   if (e.isPopupTrigger() && e.getClickCount()==1 && preserveOrderCheckbox.isSelected()) showDistanceContextMenu(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                   if (!isDataEditable()) return;
                   if (selectedPanel!=DistancePanel.this) selectPanel(DistancePanel.this);
                   if (e.isPopupTrigger() && e.getClickCount()==1 && preserveOrderCheckbox.isSelected()) showDistanceContextMenu(e);
                }
                @Override
                public void mouseEntered(MouseEvent e) {mouseEntered=true;repaint();}
                @Override
                public void mouseExited(MouseEvent e) {mouseEntered=false;repaint();}
            });
        }

        public int getWidth() {return width;}
        public int getHeight() {return motifsPanel.getHeight();}
        public Dimension getSize() {return new Dimension(width,motifsPanel.getHeight());}
        public Dimension getPreferredSize() {return new Dimension(width,motifsPanel.getHeight());}
        public Dimension getMaximumSize() {return new Dimension(width,motifsPanel.getHeight());}         
        
        public int[] getDistance() {
            if (!distanceIsConstrained) return null;
            else return new int[]{min,max};
        }

        private void showDistanceContextMenu(MouseEvent e) {
            JPopupMenu menu=new JPopupMenu();
            JMenuItem item1=new JMenuItem("Set distance constraint");
            JMenuItem item2=new JMenuItem("Remove distance constraint");
            DistanceContextMenuListener listener=new DistanceContextMenuListener(this);
            item1.addActionListener(listener);
            item2.addActionListener(listener);
            menu.add(item1);
            menu.add(item2);
            menu.show(e.getComponent(), e.getX(),e.getY());
        }


        public void promptDistance() {
            String initial="";
            if (distanceIsConstrained) {
                if (min==Integer.MIN_VALUE) initial+="*,"; else initial+=min+",";
                if (max==Integer.MAX_VALUE) initial+="*"; else initial+=""+max;
            }
            String value=JOptionPane.showInputDialog(this, "<html>Enter new min and max distance values<br>separated by comma (* = Unlimited)</html>",initial);
            if (value!=null) {
                if (value.trim().isEmpty()) {distanceIsConstrained=false;}
                else {
                    String[] elements=value.split("\\s*,\\s*");
                    if (elements.length==2) {
                       int newmin=-1;
                       int newmax=-1;
                       try {
                           if (elements[0].equals("*")) newmin=Integer.MIN_VALUE;
                           else newmin=Integer.parseInt(elements[0]);
                       } catch (NumberFormatException e) {return;}
                       try {
                           if (elements[1].equals("*")) newmax=Integer.MAX_VALUE;
                           else newmax=Integer.parseInt(elements[1]);
                       } catch (NumberFormatException e) {return;}
                       if (newmax<newmin) return;
                       min=newmin;
                       max=newmax;
                    } else return;
                    distanceIsConstrained=true;
                    preserveOrderCheckbox.setSelected(true);
                }
                repaint();
            }
            updateSelectedPanel();            
        }

        public void setDistanceConstraint(int newmin, int newmax) {
             min=newmin;
             max=newmax;
             distanceIsConstrained=true;           
        }
        
        public void clearDistanceConstraint() {
             min=0;
             max=0;
             distanceIsConstrained=false;
             repaint();
             updateSelectedPanel();
        }

        @Override
        public void paintComponent(Graphics g) {
            int height=getHeight();            
            int yoffset=(int)(height/2.0)-20;
            if (yoffset<30) yoffset=30;
            if (selectedPanel==this) g.setColor(selectedColor);       
            else g.setColor(Color.WHITE); 
            g.fillRect(0, 0, width, height);
            if (!preserveOrderCheckbox.isSelected()) return;
            if (mouseEntered) g.setColor(Color.RED); else g.setColor(Color.BLACK);
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawLine(0, yoffset+10, width/2, yoffset);
            g.drawLine(width/2, yoffset, width, yoffset+10);
            String string=getConstraintAsString();
            int stringwidth=g.getFontMetrics().stringWidth(string);
            if (stringwidth>width-20 || (stringwidth+20<width && width>50)) {
                int newwidth=stringwidth+20;
                if (newwidth<50) newwidth=50;
                width=newwidth;
                motifsPanel.revalidate();
            } else g.drawString(string, (int)((width-stringwidth)/2f), yoffset-8);
        }

        private String getConstraintAsString() {
            if (distanceIsConstrained) {
                String string ="[ ";
                if (min==Integer.MIN_VALUE) string+="* , ";
                else string+=(min+" , ");
                if (max==Integer.MAX_VALUE) string+="* ]";
                else string+=(max+" ]");
                return string;
            } else return "";
        }
    } // end class DistancePanel

    private boolean isDark(Color color) {
        int red=color.getRed();
        int green=color.getGreen();
        int blue=color.getBlue();
        int threshold=130;
        if ((((red*299) + (green*587) + (blue*114)) / 1000f)>threshold) return false;
        else return true;
    }

    private boolean isNameInUse(String name, MotifRepresentationPanel sourcepanel) {
         if (Region.isReservedProperty(name.toLowerCase())) return true;                 
         for (int i=0;i<motifsPanel.getComponentCount();i++) {
            JPanel p=(JPanel)motifsPanel.getComponent(i);
            if (p instanceof MotifRepresentationPanel && p!=sourcepanel) {
                String panelname=((MotifRepresentationPanel)p).getMotifName();
                if (panelname.equals(name) || panelname.equals(cleanUpMotifName(name))) return true;
            }
        }
        return false;
    }

    private class MotifContextMenuListener implements ActionListener {
        MotifRepresentationPanel panel;
        public MotifContextMenuListener(MotifRepresentationPanel panel) {this.panel=panel;}
        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd=e.getActionCommand();
            if (cmd.equals("Select motifs")) panel.promptMotifs();
            else if (cmd.equals("Rename")) {
                String oldName=panel.getMotifName();
                String newname=JOptionPane.showInputDialog(panel, "Enter new name", oldName);
                if (newname==null) return;
                newname=newname.trim();
                if (oldName!=null && newname.equals(oldName)) return; // the name was not changed
                boolean ok=true;
                if (newname!=null && !newname.trim().isEmpty() && !isNameInUse(newname, panel)) {
                    ok=panel.setMotifName(newname.trim());
                } else ok=false;
                if (!ok) JOptionPane.showMessageDialog(panel, "The name is illegal or already in use", "Error", JOptionPane.ERROR_MESSAGE);
            } else if (cmd.equals("Direct")) {
                panel.setMotifOrientation(ModuleCRM.DIRECT);
            } else if (cmd.equals("Reverse")) {
                panel.setMotifOrientation(ModuleCRM.REVERSE);
            } else if (cmd.equals("Any")) {
                panel.setMotifOrientation(ModuleCRM.INDETERMINED);
            }
            updateSelectedPanel();
        }
    }

    private class DistanceContextMenuListener implements ActionListener {
        DistancePanel panel;
        public DistanceContextMenuListener(DistancePanel panel) {this.panel=panel;}
        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd=e.getActionCommand();
            if (cmd.equals("Set distance constraint")) panel.promptDistance();
            else if (cmd.equals("Remove distance constraint")) {
                panel.clearDistanceConstraint();
            }
        }
    }



}
