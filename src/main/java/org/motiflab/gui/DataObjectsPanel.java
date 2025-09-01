
package org.motiflab.gui;

import org.motiflab.engine.data.analysis.Analysis;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import org.jdesktop.application.ResourceMap;
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.ModuleTextMap;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.MotifTextMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.PriorsGenerator;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.data.SequenceTextMap;
import org.motiflab.engine.data.TextVariable;

import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.gui.prompt.Prompt;
import org.motiflab.gui.prompt.Prompt_BackgroundModel;
import org.motiflab.gui.prompt.Prompt_ExpressionProfile;
import org.motiflab.gui.prompt.Prompt_ModuleNumericMap;
import org.motiflab.gui.prompt.Prompt_ModuleTextMap;
import org.motiflab.gui.prompt.Prompt_MotifNumericMap;
import org.motiflab.gui.prompt.Prompt_MotifTextMap;
import org.motiflab.gui.prompt.Prompt_NumericVariable;
import org.motiflab.gui.prompt.Prompt_PriorsGenerator;
import org.motiflab.gui.prompt.Prompt_SequenceCollection;
import org.motiflab.gui.prompt.Prompt_SequenceNumericMap;
import org.motiflab.gui.prompt.Prompt_SequencePartition;
import org.motiflab.gui.prompt.Prompt_SequenceTextMap;
import org.motiflab.gui.prompt.Prompt_TextVariable;


/**
 * This class encapsulates the DataObjects (non-feature data) dataset panel
 * 
 * @author kjetikl
 */
public class DataObjectsPanel extends DataPanel {
    private JPanel labelpanel;
    private JLabel dataObjectsPanelLabel;
    private JScrollPane dataListScrollPane;
    private JList dataList;
    private JButton groupButton;
    private MiscIcons groupButtonIcon;    
    private groupPopupMenu groupPopupMenu;     
    private JButton addButton;
    private addDataPopupMenu addPopupMenu;
    private MotifLabGUI gui;
    private ArrayList<PanelListener> dataPanelListeners=null;
    
    private Class[] filter=new Class[]{
        SequenceCollection.class,
        SequencePartition.class,
        NumericVariable.class,
        SequenceTextMap.class,
        SequenceNumericMap.class,
        BackgroundModel.class,
        MotifTextMap.class,
        MotifNumericMap.class,
        ModuleTextMap.class,
        ModuleNumericMap.class,
        TextVariable.class,
        Analysis.class, 
        ExpressionProfile.class, 
        PriorsGenerator.class
    }; // list of which class types this panel accepts
    
  public DataObjectsPanel(MotifLabGUI gui) {
      this.gui=gui;
      initComponents();
  } 
  
  @Override
  public boolean holdsType(String type) {
      return (   type.equals(SequenceCollection.getType())
              || type.equals(SequencePartition.getType())
              || type.equals(NumericVariable.getType())
              || type.equals(BackgroundModel.getType())
              || type.equals(SequenceNumericMap.getType())              
              || type.equals(MotifNumericMap.getType())
              || type.equals(ModuleNumericMap.getType())
              || type.equals(SequenceTextMap.getType())              
              || type.equals(MotifTextMap.getType())
              || type.equals(ModuleTextMap.getType())              
              || type.equals(TextVariable.getType())
              || type.equals(Analysis.getType())
              || type.equals(PriorsGenerator.getType())
              || type.equals(ExpressionProfile.getType()));
  }  
  
  @Override
  public boolean holdsType(Data data) {
      return (   data instanceof SequenceCollection
              || data instanceof SequencePartition
              || data instanceof SequenceNumericMap              
              || data instanceof MotifNumericMap
              || data instanceof ModuleNumericMap
              || data instanceof SequenceTextMap              
              || data instanceof MotifTextMap
              || data instanceof ModuleTextMap              
              || data instanceof NumericVariable              
              || data instanceof TextVariable
              || data instanceof BackgroundModel              
              || data instanceof Analysis
              || data instanceof PriorsGenerator
              || data instanceof ExpressionProfile
              );
  }     
  
  public JList getDataObjectsList() {
      return dataList;
  }  

  @Override
  public void clearSelection() {
      dataList.clearSelection();
  }

  @Override
  public void setHeaderVisible(boolean visible) {
     labelpanel.setVisible(visible);    
     revalidate();
     repaint();        
  }  
  
  @Override
  public void addDataPanelListener(PanelListener listener) {
      if (dataPanelListeners==null) dataPanelListeners=new ArrayList<PanelListener>();
      dataPanelListeners.add(listener);
  }

  @Override
  public void removeDataPanelListener(PanelListener listener) {
      if (dataPanelListeners!=null) dataPanelListeners.remove(listener);
  }  
  
  private void initComponents() {
        ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(DataObjectsPanel.class);

        dataObjectsPanelLabel=new JLabel();
        setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        setName("DataObjectsPanel"); // NOI18N
        setLayout(new BorderLayout());

        dataObjectsPanelLabel.setText(resourceMap.getString("DataObjectsLabel.text")); // NOI18N
        dataObjectsPanelLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 3, 1, 1));
        dataObjectsPanelLabel.setName("Data objects"); // NOI18N
        addPopupMenu=new addDataPopupMenu();
        addButton=new JButton(new MiscIcons(MiscIcons.ADD_DATA));
        addButton.setName("addButton");
        addButton.setToolTipText(resourceMap.getString("addButton.toolTipText")); // NOI18N
        addButton.setIcon(new MiscIcons(MiscIcons.PLUS_ICON));
        //addButton.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
        Dimension dim=new Dimension(36,22);
        addButton.setMaximumSize(dim);
        addButton.setPreferredSize(dim);
        addButton.setMinimumSize(dim);
        addButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                addDataObject(evt);
            }
        });
        
        groupButtonIcon=new MiscIcons(MiscIcons.TREE_GROUPS);
        groupButton=new JButton(groupButtonIcon); // 
        groupButton.setName("groupButton");
        groupButton.setToolTipText(resourceMap.getString("groupButton.toolTipText")); // NOI18N
        //groupButton.setFont(buttonsFont);
        Dimension dim2=new Dimension(36,22); //
        groupButton.setMaximumSize(dim2);
        groupButton.setPreferredSize(dim2);
        groupButton.setMinimumSize(dim2);
        groupButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (groupButton.isEnabled()) setGrouping(evt);
            }
        });
        JPanel buttonspanel=new JPanel();
        buttonspanel.setLayout(new BoxLayout(buttonspanel, BoxLayout.X_AXIS));
        buttonspanel.add(groupButton);
        buttonspanel.add(addButton);         
        labelpanel=new JPanel();
        labelpanel.setLayout(new BorderLayout());
        labelpanel.add(dataObjectsPanelLabel,BorderLayout.CENTER);
        labelpanel.add(buttonspanel,BorderLayout.EAST);        
        add(labelpanel, java.awt.BorderLayout.PAGE_START);
        
        dataListScrollPane = new JScrollPane();
        dataListScrollPane.setName("DataListScrollPane"); // NOI18N

        dataList=new JList();
        removePeskyKeyListener();
        dataList.setBackground(resourceMap.getColor("dataList.background")); // NOI18N
        dataList.setModel(new DataObjectsPanelListModel(gui.getEngine(),filter));
        dataList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dataList.setCellRenderer(new DataPanelCellRenderer(gui.getVisualizationSettings()));
        dataList.setDragEnabled(true);
        dataList.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        dataList.setName("dataList"); // NOI18N

        dataList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                mousePressedInDataObjectsPanel(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                mouseReleasedInDataObjectsPanel(evt);
            }
        });
        dataList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown() && !e.isControlDown()) {
                    boolean show=(e.getKeyCode()==KeyEvent.VK_S);
                    setSequenceVisibility(gui.getVisualizationSettings(),gui.getEngine().getDefaultSequenceCollection(),show); 
                    notifyKeyListeners(e);
                    return;
                } 
                int[] selected=dataList.getSelectedIndices();
                if (selected.length==0) {notifyKeyListeners(e);return;} // this is not interesting to us here, but we will notify other listeners before returning
                if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown() && !e.isControlDown()) {
                    DataObjectsPanelListModel model=(DataObjectsPanelListModel)dataList.getModel();
                    Data[] selecteddata=new Data[selected.length];
                    for (int i=0;i<selected.length;i++) {
                        selecteddata[i]=(Data)model.getElementAt(selected[i]);
                    }
                    boolean show=(e.getKeyCode()==KeyEvent.VK_S);
                    setSequenceVisibilityOnAll(gui.getVisualizationSettings(),selecteddata,show);                
                }                
                else if (e.getKeyCode()==KeyEvent.VK_O && !e.isControlDown()) {
                    DataObjectsPanelListModel model=(DataObjectsPanelListModel)dataList.getModel();
                    Data[] selecteddata=new Data[selected.length];
                    boolean atleastonesequencecollection=false;
                    for (int i=0;i<selected.length;i++) {
                        selecteddata[i]=(Data)model.getElementAt(selected[i]);
                        if (selecteddata[i] instanceof SequenceCollection) atleastonesequencecollection=true;
                    }
                    if (atleastonesequencecollection) {
                        setSequenceVisibility(gui.getVisualizationSettings(),gui.getEngine().getDefaultSequenceCollection(),false);                                     
                        setSequenceVisibilityOnAll(gui.getVisualizationSettings(),selecteddata,true);   
                    }
                }                               
                else if (e.getKeyCode()==KeyEvent.VK_DELETE) {
                    DataObjectsPanelListModel model=(DataObjectsPanelListModel)dataList.getModel();
                    String[] datanames=new String[selected.length];
                    for (int i=0;i<selected.length;i++) {
                        Data selectedData=(Data)model.getElementAt(selected[i]);
                        if (selectedData!=gui.getEngine().getDefaultSequenceCollection()) datanames[i]=selectedData.getName();
                    }
                    gui.deleteDataItems(datanames);  
                } 
                else if (e.getKeyCode()==KeyEvent.VK_P && !e.isControlDown()) {
                    Data data=(Data)dataList.getSelectedValue();
                    String parameterString=data.getValueAsParameterString();
                    gui.logMessage(parameterString);
                }
                else if (e.getKeyCode()==KeyEvent.VK_M && !e.isControlDown()) {
                    DataObjectsPanelListModel model=(DataObjectsPanelListModel)dataList.getModel();
                    Data[] selecteddata=new Data[selected.length];
                    for (int i=0;i<selected.length;i++) {
                        selecteddata[i]=(Data)model.getElementAt(selected[i]);
                    }
                    boolean mark=(!e.isShiftDown());
                    setSequenceMarkOnAll(gui.getVisualizationSettings(),selecteddata,mark);   
                } 
                else if (e.getKeyCode()==KeyEvent.VK_B) { // bookmark favorites
                    Favorites favorites=gui.getFavorites();
                    DataObjectsPanelListModel model=(DataObjectsPanelListModel)dataList.getModel();
                    for (int i=0;i<selected.length;i++) {
                        Data data=(Data)model.getElementAt(selected[i]);
                        String parameterString=data.getValueAsParameterString();
                        if (parameterString!=null && !parameterString.equals("N/A")) {
                            if (favorites.hasFavorite(data.getName())) gui.logMessage("Error: An entry with the same name ("+data.getName()+") already exists in Favorites");
                            else { 
                                favorites.addFavorite(data, gui.getEngine());
                                gui.logMessage("Adding '"+data.getName()+"' to Favorites");                                
                            }
                        } else gui.logMessage("Error: Unable to add '"+data.getName()+"' to Favorites");
                    } 
                } 
                // Notify other listeners also
                notifyKeyListeners(e);
            }
        });
        dataList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                gui.setSearchTargetComponent(DataObjectsPanel.this);
            }

        });
        // dataList.setTransferHandler(new FeaturesPanelTransferHandler(dataList,gui)); <-- THIS SHOULD BE UPDATED!!!
        //dataList.getModel().addListDataListener(gui.getVisualizationSettings());
        dataListScrollPane.setViewportView(dataList);
        add(dataListScrollPane, java.awt.BorderLayout.CENTER);
        groupPopupMenu=new groupPopupMenu();        
  }

        private void notifyKeyListeners(KeyEvent e) {
            if (dataPanelListeners==null) return;
            Data[] selectedData=getSelectedDataElements();
            for (PanelListener listener:dataPanelListeners) {
                listener.keyPressed(this, selectedData, e);
            }
        }
  
        /** Returns the data elements currently selected in this panel */
        public Data[] getSelectedDataElements() {
            Data[] selectedData=null;
            int[] selected=dataList.getSelectedIndices();
            if (selected.length==0) selectedData=new Data[0];
            else {
                DataObjectsPanelListModel model=(DataObjectsPanelListModel)dataList.getModel();
                selectedData=new Data[selected.length];
                for (int i=0;i<selected.length;i++) {
                    selectedData[i]=(Data)model.getElementAt(selected[i]);
                } 
            } 
            return selectedData;
        }
        
  
        /** Removes all data objects from this panel */
        public void clearAll() {
          ((DataObjectsPanelListModel)dataList.getModel()).clear();
        }
  
        /** sets the visibility status of all sequences in the collection to the selected boolean value */
        public void setSequenceVisibility(VisualizationSettings settings, SequenceCollection collection, boolean visibility) {
            String[] sequenceNames=new String[collection.size()];
            sequenceNames=collection.getAllSequenceNames().toArray(sequenceNames);
            settings.setSequenceVisible(sequenceNames, visibility);
        }
        
        public void setSequenceVisibilityOnAll(VisualizationSettings settings, Data[] data, boolean visibility) {
            for (Data entry:data) {
                if (entry instanceof SequenceCollection) setSequenceVisibility(settings, (SequenceCollection)entry, visibility);
            }          
        }
              
        /** Marks/removes marks from all sequences in the given collection */
        public void setSequenceMark(VisualizationSettings settings, SequenceCollection collection, boolean mark, boolean redraw) {
            for (String sequence:collection.getAllSequenceNames()) {
                settings.setSequenceMarked(sequence, mark);     
            }           
            if (redraw) gui.redraw();
        }  
        
        public void setSequenceMarkOnAll(VisualizationSettings settings, Data[] data, boolean mark) {
            for (Data entry:data) {
                if (entry instanceof SequenceCollection) setSequenceMark(settings, (SequenceCollection)entry, mark, false);
            }          
            gui.redraw();
        }   
    
  /** This method will remove the automatically installed key-listener 
   *  which changes the currently selected item to the first item in the list 
   *  beginning with the letter for the key the user pressed. This functionality
   *  might be helpful to some users, but since it is more likely to crash with
   *  the functionality of MotifLab's own shortcut keys it is better to disable it.
   *  (Note that the usual navigation keys, up/down/space etc, will still function
   *  normally after this listener is removed).
   */
  private void removePeskyKeyListener() {
     KeyListener peskyListener=null;
     dataList.removeKeyListener(null);
     for (KeyListener kl:dataList.getKeyListeners()) {
        if (kl.toString().contains("BasicListUI")) {peskyListener=kl;break;} // This is the pesky listener
     }
     if (peskyListener!=null) dataList.removeKeyListener(peskyListener);       
  }    

 /**
  * This method is called when the "group motifs" button (next to the panel label) is pressed
  */
 private void setGrouping(java.awt.event.MouseEvent evt) {
    Component invoker=evt.getComponent();
    int x=evt.getX();
    int y=evt.getY();
    groupPopupMenu.show(invoker, x,y);
 }   
  
private void mousePressedInDataObjectsPanel(java.awt.event.MouseEvent evt) {                                             
    int x=evt.getX(); 
    int y=evt.getY();
    JList list=(JList)evt.getComponent();    
    int index=getElementIndexAtPointInList(list,x,y);
    if (index==-1) {list.clearSelection();return;} 
    if (evt.getButton()==java.awt.event.MouseEvent.BUTTON1 && evt.getClickCount()==2) {
         //gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         javax.swing.DefaultListModel model=(javax.swing.DefaultListModel)list.getModel();
         Data selectedData=(Data)model.getElementAt(index);
         showPrompt(selectedData);
    }
    if (evt.isPopupTrigger()) {
        if (!list.isSelectedIndex(index)) list.setSelectedIndex(index);
        JPopupMenu popup=DataObjectsPanelContextMenu.getInstance(list,index,gui,this);
        if (dataPanelListeners!=null && popup!=null) {
            Data[] selected=getSelectedDataElements();
            for (PanelListener datapanelListener:dataPanelListeners) {
                datapanelListener.showContextMenu((DataObjectsPanel)this, selected, popup);
            }
        }         
        if (popup!=null) popup.show(evt.getComponent(), x,y);
    }  
}                                            

private void mouseReleasedInDataObjectsPanel(java.awt.event.MouseEvent evt) {                                              
    int x=evt.getX(); 
    int y=evt.getY();
    JList list=(JList)evt.getComponent();
    int index=getElementIndexAtPointInList(list,x,y);
    if (index==-1) {list.clearSelection();return;} 
    if (evt.isPopupTrigger()) {
        if (!list.isSelectedIndex(index)) list.setSelectedIndex(index);
        JPopupMenu popup=DataObjectsPanelContextMenu.getInstance((JList)evt.getComponent(),index,gui,this);
        if (dataPanelListeners!=null && popup!=null) {
            Data[] selected=getSelectedDataElements();
            for (PanelListener datapanelListener:dataPanelListeners) {
                datapanelListener.showContextMenu((DataObjectsPanel)this, selected, popup);
            }
        }         
        if (popup!=null) popup.show(evt.getComponent(), x,y);
    }
}                                             

private int lastPrompt_X=-1, lastPrompt_Y=-1;

public void showPrompt(final Data selectedData) {
    showPrompt(selectedData,true,false);
}
public void showPrompt(final Data selectedData, boolean editable, boolean modal) {
      //gui.debugMessage("Doubled-clicked on "+selectedData.getName());
      Prompt prompt=null;
      if (selectedData instanceof SequenceCollection) {
          prompt=new Prompt_SequenceCollection(gui, null, (SequenceCollection)selectedData.clone(), modal);
      } else if (selectedData instanceof SequencePartition) {
          prompt=new Prompt_SequencePartition(gui, null, (SequencePartition)selectedData.clone(), modal);
      } else if (selectedData instanceof NumericVariable) {
          prompt=new Prompt_NumericVariable(gui, null, (NumericVariable)selectedData.clone(), modal);
      } else if (selectedData instanceof SequenceNumericMap) {
          prompt=new Prompt_SequenceNumericMap(gui, null, (SequenceNumericMap)selectedData.clone(), modal);
      } else if (selectedData instanceof MotifNumericMap) {
          prompt=new Prompt_MotifNumericMap(gui, null, (MotifNumericMap)selectedData.clone(), modal);
      } else if (selectedData instanceof ModuleNumericMap) {
          prompt=new Prompt_ModuleNumericMap(gui, null, (ModuleNumericMap)selectedData.clone(), modal);
      } else if (selectedData instanceof SequenceTextMap) {
          prompt=new Prompt_SequenceTextMap(gui, null, (SequenceTextMap)selectedData.clone(), modal);
      } else if (selectedData instanceof MotifTextMap) {
          prompt=new Prompt_MotifTextMap(gui, null, (MotifTextMap)selectedData.clone(), modal);
      } else if (selectedData instanceof ModuleTextMap) {
          prompt=new Prompt_ModuleTextMap(gui, null, (ModuleTextMap)selectedData.clone(), modal);
      } else if (selectedData instanceof BackgroundModel) {
          prompt=new Prompt_BackgroundModel(gui, null, (BackgroundModel)selectedData.clone(), modal);
      } else if (selectedData instanceof TextVariable) {
          prompt=new Prompt_TextVariable(gui, null, (TextVariable)selectedData.clone(), modal);
      } else if (selectedData instanceof ExpressionProfile) {
          prompt=new Prompt_ExpressionProfile(gui, null, (ExpressionProfile)selectedData.clone(), modal);
      } else if (selectedData instanceof PriorsGenerator) {
          prompt=new Prompt_PriorsGenerator(gui, null, (PriorsGenerator)selectedData.clone(), modal);
      } else if (selectedData instanceof Analysis) {
          prompt=((Analysis)selectedData).getPrompt(gui,false);
      }

      if (selectedData instanceof Analysis) prompt.setDataEditable(false);
      else prompt.setDataEditable(editable);
      //prompt.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      int promptX=gui.getFrame().getWidth()/2-prompt.getWidth()/2;
      int promptY=gui.getFrame().getHeight()/2-prompt.getHeight()/2;
      if (promptX==lastPrompt_X && promptY==lastPrompt_Y) { // select a different location
          promptX+=16;
          promptY+=16;
      }
      prompt.setLocation(promptX, promptY);
      lastPrompt_X=promptX;
      lastPrompt_Y=promptY;
      prompt.disableNameEdit();
      prompt.setCallbackOnOKPressed(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Prompt prompt=(Prompt)e.getSource(); 
          if (prompt.isOKPressed()) {
             Data data=prompt.getData();
             prompt.dispose();
             if (data!=null && !data.containsSameData(selectedData)) gui.updateDataItem(data);
          }   
        }
      });
      prompt.setVisible(true);
      prompt.requestFocus();
      //gui.getFrame().setCursor(Cursor.getDefaultCursor());    
}

 /**
  * Given a reference to a JList and a point (x,y), this function returns the index 
  * of the element corresponding to that point (between 0 and model.getSize()-1).
  * If the coordinate does not correspond to an element in the list, the method returns the value -1;
  * @param list The JList (fitted with a DefaultListModel)
  * @param x
  * @param y
  * @return
  */
 private int getElementIndexAtPointInList(JList list, int x, int y) {
         int index=list.locationToIndex(new java.awt.Point(x,y));
         if (index>=0) {
            javax.swing.DefaultListModel model=(javax.swing.DefaultListModel)list.getModel();
            java.awt.Rectangle bounds = list.getCellBounds(0, model.getSize()-1);
            if (y>bounds.getHeight() || y<0 || x>bounds.getWidth() || x<0) index=-1; // 
         }
         return index;
 }

 /**
  * This method is called when the "add data object" button (next to the panel label) is pressed
  */
 private void addDataObject(java.awt.event.MouseEvent evt) {
    Component invoker=evt.getComponent();
    int x=evt.getX();
    int y=evt.getY();
    addPopupMenu.show(invoker, x,y);
 }
 

 private class addDataPopupMenu extends JPopupMenu {
     public addDataPopupMenu() {
         addDataPopupMenuListener listener=new addDataPopupMenuListener();
         addTypeToMenu(BackgroundModel.getType(),listener); 
         addTypeToMenu(ExpressionProfile.getType(),listener);
         addTypeToMenu(MotifTextMap.getType(),listener);          
         addTypeToMenu(MotifNumericMap.getType(),listener);   
         addTypeToMenu(ModuleTextMap.getType(),listener);           
         addTypeToMenu(ModuleNumericMap.getType(),listener);
         addTypeToMenu(NumericVariable.getType(),listener);
         addTypeToMenu(PriorsGenerator.getType(),listener);
         addTypeToMenu(SequenceCollection.getType(),listener);
         addTypeToMenu(SequencePartition.getType(),listener);
         addTypeToMenu(SequenceTextMap.getType(),listener);
         addTypeToMenu(SequenceNumericMap.getType(),listener);         
         addTypeToMenu(TextVariable.getType(),listener);
     }
     
     private void addTypeToMenu(String type, addDataPopupMenuListener listener) {
         JMenuItem item=new JMenuItem(type);
         // Show type icons for each data type
//         SimpleDataPanelIcon typeicon=new SimpleDataPanelIcon(20, 20, SimpleDataPanelIcon.getIconTypeForDataType(type), gui.getVisualizationSettings());
//         typeicon.drawBorder(true);
//         typeicon.setBackgroundColor(java.awt.Color.WHITE);
//         item.setIcon(typeicon);
         item.addActionListener(listener);
         add(item);
     }
 }
 


 private class addDataPopupMenuListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
         gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         Prompt prompt=null;
         String type=e.getActionCommand();
              if (type.equals(SequenceCollection.getType())) prompt=new Prompt_SequenceCollection(gui, null, null);
         else if (type.equals(SequencePartition.getType())) prompt=new Prompt_SequencePartition(gui, null, null);
         else if (type.equals(NumericVariable.getType())) prompt=new Prompt_NumericVariable(gui, null, null);
         else if (type.equals(BackgroundModel.getType())) prompt=new Prompt_BackgroundModel(gui, null, null);
         else if (type.equals(SequenceNumericMap.getType())) prompt=new Prompt_SequenceNumericMap(gui, null, null);
         else if (type.equals(MotifNumericMap.getType())) prompt=new Prompt_MotifNumericMap(gui, null, null);
         else if (type.equals(ModuleNumericMap.getType())) prompt=new Prompt_ModuleNumericMap(gui, null, null);
         else if (type.equals(SequenceTextMap.getType())) prompt=new Prompt_SequenceTextMap(gui, null, null);
         else if (type.equals(MotifTextMap.getType())) prompt=new Prompt_MotifTextMap(gui, null, null);
         else if (type.equals(ModuleTextMap.getType())) prompt=new Prompt_ModuleTextMap(gui, null, null);
         else if (type.equals(TextVariable.getType())) prompt=new Prompt_TextVariable(gui, null, null);
         else if (type.equals(ExpressionProfile.getType())) prompt=new Prompt_ExpressionProfile(gui, null, null);
         else if (type.equals(PriorsGenerator.getType())) prompt=new Prompt_PriorsGenerator(gui, null, null);
         prompt.setLocation(gui.getFrame().getWidth()/2-prompt.getWidth()/2, gui.getFrame().getHeight()/2-prompt.getHeight()/2);
         prompt.setVisible(true);
         gui.getFrame().setCursor(Cursor.getDefaultCursor());
         if (prompt.isOKPressed()) {
            Data variable=prompt.getData();    
            boolean silent=true; // silent=prompt.isSilentOK();
            if (variable==null) {prompt.dispose();return;}
            OperationTask task=new OperationTask("new "+type);
            task.setParameter(Operation_new.DATA_TYPE, type);
            task.setParameter(OperationTask.OPERATION_NAME, "new");
            task.setParameter(OperationTask.ENGINE,gui.getEngine());
            task.setParameter(OperationTask.TARGET_NAME,variable.getName());
            String parameterString=variable.getValueAsParameterString();
            task.setParameter(Operation_new.PARAMETERS, parameterString);
            if (silent) task.setParameter("_SHOW_RESULTS", Boolean.FALSE);
            if (prompt.isDataImportedFromFile()) {
                prompt.setImportFromFileSettingsInTask(task); // sets FILENAME, DATA_FORMAT and DATA_FORMAT_SETTINGS parameters
            }
            variable.setAdditionalOperationNewTaskParameters(task);
            Class typeclass=gui.getEngine().getDataClassForTypeName(type);
            task.addAffectedDataObject(variable.getName(), typeclass);
            prompt.dispose();
            gui.launchOperationTask(task,gui.isRecording());
         } else {prompt.dispose();}
    } 
}
 
// ----- Searchable interface -----
    String previoussearchString="";
    @Override
    public boolean find(String searchstring) {
         DataObjectsPanelListModel model=(DataObjectsPanelListModel)dataList.getModel();
         if (model.getSize()==0) return false;
         int selected=(previoussearchString.equalsIgnoreCase(searchstring))?dataList.getSelectedIndex():-1;
         previoussearchString=searchstring;
         int found=-1;
         int start=(selected<0)?0:selected+1;
         for (int i=start;i<model.getSize();i++) {
              String name=null;
              Object el=model.getElementAt(i);
              if (el instanceof Data) name=((Data)el).getName();
              else if (el instanceof String) name=(String)el;
              if (name!=null && name.matches("(?i).*"+searchstring+".*")) {
                  gui.statusMessage("[Data Objects] Searching for '"+searchstring+"'. Found '"+name+"'.");                 
                  found=i;
                  break;
              }
         }
         if (found>=0) {
             //gui.statusMessage(""); // just to clear the status
             dataList.setSelectedIndex(found);
             dataList.ensureIndexIsVisible(found);
             return true;
         } else {
             gui.statusMessage("[Data Objects] Searching for '"+searchstring+"'. No more matches found.");
             previoussearchString=""; // this will reset the search and wrap around
             return false;
         }
    }


    @Override
    public boolean isSearchCaseSensitive() {
        return false;
    }

    @Override
    public int replaceAll(String searchstring, String replacestring) {
        return 0;
    }

    @Override
    public boolean replaceCurrent(String searchstring, String replacestring) {
        return false;
    }

    @Override
    public void searchAndReplace() {
       // Not implemented since supportReplace returns false
    }

    @Override
    public void setSearchIsCaseSensitive(boolean flag) {
        // not needed here?
    }

    @Override
    public boolean supportsReplace() {
       return false;
    }

    @Override
    public String getSelectedTextForSearch() {
       return null;
    }  
 
    
    private class groupPopupMenu extends JPopupMenu implements ActionListener {

         JMenuItem groupByType;
         JMenuItem sortAlphabetically;

         public groupPopupMenu() {
             groupByType=new JCheckBoxMenuItem("Group By Type",((DataObjectsPanelListModel)dataList.getModel()).shouldGroupByType());
             sortAlphabetically=new JCheckBoxMenuItem("Sort Alphabetically",((DataObjectsPanelListModel)dataList.getModel()).shouldSortAlphabetically());
             groupByType.addActionListener(this);
             sortAlphabetically.addActionListener(this);
             add(groupByType);
             add(sortAlphabetically);

         }

         @Override
         public void actionPerformed(ActionEvent e) {
            String cmd=e.getActionCommand();
            DataObjectsPanelListModel listmodel=((DataObjectsPanelListModel)dataList.getModel());
            if (cmd.equals("Sort Alphabetically")) {
                boolean current=listmodel.shouldSortAlphabetically();
                listmodel.setSortAlphabetically(!current);              
            } else if (cmd.equals("Group By Type")) {
                boolean current=listmodel.shouldGroupByType();
                listmodel.setGroupByType(!current);
            }
         }
    } // end class groupPopupMenu     
    
}
