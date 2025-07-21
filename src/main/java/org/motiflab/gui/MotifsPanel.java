
package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.jdesktop.application.ResourceMap;

import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.DataPartition;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleMotif;
import org.motiflab.engine.data.ModulePartition;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifClassification;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.gui.prompt.Prompt;
import org.motiflab.gui.prompt.Prompt_Module;
import org.motiflab.gui.prompt.Prompt_ModuleCollection;
import org.motiflab.gui.prompt.Prompt_ModulePartition;
import org.motiflab.gui.prompt.Prompt_Motif;
import org.motiflab.gui.prompt.Prompt_MotifCollection;
import org.motiflab.gui.prompt.Prompt_MotifPartition;


/**
 * This class encapsulates the MotifCollection panel
 * 
 * @author kjetikl
 */
public class MotifsPanel extends DataPanel {
        public static final String SHOW_MOTIFS="Motifs";
        public static final String SHOW_MOTIF_COLLECTIONS="Motif Collections";
        public static final String SHOW_MOTIF_PARTITIONS="Motif Partitions";
        public static final String SHOW_MODULES="Modules";
        public static final String SHOW_MODULE_COLLECTIONS="Module Collections";
        public static final String SHOW_MODULE_PARTITIONS="Module Partitions";
        private static final String GROUP_BY_CLASS_1="Group by class: 1 level";
        private static final String GROUP_BY_CLASS_2="Group by class: 2 levels";
        private static final String GROUP_BY_CLASS_3="Group by class: 3 levels";
        private static final String GROUP_BY_CLASS_4="Group by class: 4 levels";
        private static final String GROUP_BY_CLASS_5="Group by class: 5 levels";
        private static final String GROUP_BY_CLASS_6="Group by class: 6 levels";        
        private static final String GROUP_NONE="No Grouping";
        //private JLabel motifCollectionPanelLabel;
        private JScrollPane motifsPanelScrollPane;
        private JTree motifsTree;
        private JButton addButton;
        private JButton groupButton;
        private addDataPopupMenu addPopupMenu;
        private groupPopupMenu groupPopupMenu;
        private MotifLabGUI gui;
        private CheckBoxTreeCellRenderer checkBoxTreeCellRenderer;
        private MotifsPanelTreeModel model=null;
        private SingleMotifTooltip tooltip=null;        
        VisualizationSettings settings=null;
        private TreeListener treelistener;
        private JComboBox selectContentCombobox;
        private MiscIcons groupButtonIcon;
        private JPanel labelpanel;
        private ArrayList<PanelListener> dataPanelListeners=null;
        

        
  public MotifsPanel(MotifLabGUI gui) {
      this.gui=gui;      
      this.settings=gui.getVisualizationSettings();      
      CheckBoxTreeNode.setVisualizationSettings(settings); // static property
      treelistener=new TreeListener();
      initComponents();
      motifsTree.setRootVisible(false); // NOTE: hiding the root causes NullPointerExceptions and GUI crash in Nimbus when removing tree nodes (I don't know why and have not been able to create a workaround yet)
      tooltip=new SingleMotifTooltip(settings);
      ToolTipManager.sharedInstance().registerComponent(motifsTree); // this is necessary for trees
      //gui.getEngine().addDataListener(this);
  } 

  @Override
  public boolean holdsType(String type) {
      return (   type.equals(Motif.getType())
              || type.equals(MotifCollection.getType())
              || type.equals(MotifPartition.getType())
              || type.equals(ModuleCRM.getType())
              || type.equals(ModuleCollection.getType())
              || type.equals(ModulePartition.getType()));
  }
  
  @Override
  public boolean holdsType(Data data) {
      return (   data instanceof Motif
              || data instanceof MotifCollection
              || data instanceof MotifPartition
              || data instanceof ModuleCRM
              || data instanceof ModuleCollection
              || data instanceof ModulePartition);
  }  

  public JTree getTree() {
      return motifsTree;
  }  
  
  @Override
  public void clearSelection() {
      motifsTree.clearSelection();
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
        ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(MotifsPanel.class);

//        motifCollectionPanelLabel=new JLabel();
        setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        setName("MotifCollectionPanel"); // NOI18N
        setLayout(new BorderLayout());

//        motifCollectionPanelLabel.setText(resourceMap.getString("motifCollectionLabel.text")); // NOI18N
//        motifCollectionPanelLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 3, 1, 1));
//        motifCollectionPanelLabel.setName("Motif Collections"); // NOI18N
        //Font buttonsFont=new Font(Font.SANS_SERIF,Font.PLAIN,12);
        addPopupMenu=new addDataPopupMenu();
        addButton=new JButton(new MiscIcons(MiscIcons.ADD_DATA));
        addButton.setName("addButton");
        addButton.setToolTipText(resourceMap.getString("addButton.toolTipText")); // NOI18N
        addButton.setIcon(new MiscIcons(MiscIcons.PLUS_ICON));
        //addButton.setFont(buttonsFont);
        Dimension dim=new Dimension(36,22);
        addButton.setMaximumSize(dim);
        addButton.setPreferredSize(dim);
        addButton.setMinimumSize(dim);
        addButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                addDataObject(evt);
            }
        });
        groupPopupMenu=new groupPopupMenu();
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
        //JPanel internallabel=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        labelpanel.setLayout(new BorderLayout());
        //internallabel.add(motifCollectionPanelLabel);
        //internallabel.add(new JLabel("   "));
        //internallabel.add(groupButton);
        //labelpanel.add(internallabel,BorderLayout.CENTER);
        //labelpanel.add(motifCollectionPanelLabel,BorderLayout.CENTER);
        selectContentCombobox=new JComboBox(new String[]{SHOW_MOTIFS,SHOW_MOTIF_COLLECTIONS,SHOW_MOTIF_PARTITIONS,SHOW_MODULES,SHOW_MODULE_COLLECTIONS,SHOW_MODULE_PARTITIONS});
        labelpanel.add(selectContentCombobox,BorderLayout.CENTER);
        labelpanel.add(buttonspanel,BorderLayout.EAST);        
        add(labelpanel, BorderLayout.NORTH);
        
        motifsPanelScrollPane = new JScrollPane();
        motifsPanelScrollPane.setName("MotifCollectionScrollPane"); // NOI18N

        motifsTree=new JTree() {
            @Override
            public JToolTip createToolTip() {
                return tooltip;
            }              
        };       
        motifsTree.setBackground(java.awt.Color.WHITE); // NOI18N
        motifsTree.setDragEnabled(false);
        // motifsTree.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        motifsTree.setName("motifCollectionList"); // NOI18N
        motifsTree.setEditable(false);
        motifsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        motifsTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_V) {
                    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
                    if (rows==null) {notifyKeyListeners(e);return;}
                    Arrays.sort(rows);
                    int checkedNodes=0;
                    for (int row:rows) {
                        CheckBoxTreeNode node=(CheckBoxTreeNode)motifsTree.getPathForRow(row).getLastPathComponent();
                        if (node.isChecked()) checkedNodes++;
                    }
                    boolean doCheck=true;
                    if (checkedNodes==rows.length) doCheck=false;
                    setVisibilityOnSelectedRows(doCheck);
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown() && !e.isControlDown()) {
                    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
                    if (rows==null) {notifyKeyListeners(e);return;}
                    Arrays.sort(rows);
                    setVisibilityOnSelectedRows(e.getKeyCode()==KeyEvent.VK_S);
                } else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown() && !e.isControlDown()) {
                    setVisibilityOnAll(e.getKeyCode()==KeyEvent.VK_S);
                } else if (e.getKeyCode()==KeyEvent.VK_O && !e.isControlDown()) {
                    showOnlySelectedRows();
                } else if (e.getKeyCode()==KeyEvent.VK_DELETE) {
                    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
                    if (rows==null) {notifyKeyListeners(e);return;} // this can happen!                   
                    ArrayList<String> deletionList=new ArrayList<String>();
                    for (int row:rows) {
                        CheckBoxTreeNode node=(CheckBoxTreeNode)motifsTree.getPathForRow(row).getLastPathComponent();
                        Object object=node.getUserObject();                        
                        if (object instanceof DataCollection || object instanceof DataPartition) { // do not allow deletion of single Motifs or Modules from the engine, only collections and partitions
                            deletionList.add(((Data)object).getName());
                        }                          
                    } // end for each selected row
                    String[] names=new String[deletionList.size()];
                    if (!deletionList.isEmpty()) gui.deleteDataItems(deletionList.toArray(names)); 
                } else if (e.getKeyCode()==KeyEvent.VK_P && !e.isControlDown()) {
                    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
                    if (rows==null) {notifyKeyListeners(e);return;} // this can happen!                   
                    for (int row:rows) {
                        CheckBoxTreeNode node=(CheckBoxTreeNode)motifsTree.getPathForRow(row).getLastPathComponent();
                        Object object=node.getUserObject();                        
                        if (object instanceof Data) { // 
                            String parameterString=((Data)object).getValueAsParameterString();
                            gui.logMessage(parameterString);
                        }                          
                    } // end for each selected row
                } else if (e.getKeyCode()==KeyEvent.VK_I) {
                    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
                    if (rows==null) {notifyKeyListeners(e);return;} // this can happen!                   
                    for (int row:rows) {
                        CheckBoxTreeNode node=(CheckBoxTreeNode)motifsTree.getPathForRow(row).getLastPathComponent();
                        Object object=node.getUserObject();                        
                        if (object instanceof Motif) { // do not allow deletion of single Motifs or Modules from the engine, only collections and partitions
                            int size=((Motif)object).getLength();
                            String IC="";
                            for (int i=0;i<size;i++) {
                               double ic=((Motif)object).getICcontentForColumn(i);
                               if (i>0) IC+=",";
                               IC+=""+ic;
                            }
                            gui.logMessage(IC);                            
                        }                          
                    } // end for each selected row
                } 
                notifyKeyListeners(e);
            } // end key pressed 
        });
        checkBoxTreeCellRenderer=new CheckBoxTreeCellRenderer();        
        motifsTree.setCellRenderer(checkBoxTreeCellRenderer);
        motifsPanelScrollPane.setViewportView(motifsTree);
        add(motifsPanelScrollPane, java.awt.BorderLayout.CENTER);
        motifsTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Object node=null;
                int x=e.getX();
                int y=e.getY();
                int row = motifsTree.getRowForLocation(x, y);
                if (row<0) return;
                if (!motifsTree.isRowSelected(row)) {
                    motifsTree.setSelectionRow(row);
                }
                Rectangle rect = motifsTree.getRowBounds(row);
	        if (rect == null) return;	  
                TreePath path=motifsTree.getPathForRow(row);
                if (path!=null) node=path.getLastPathComponent();
                if (node!=null && node instanceof CheckBoxTreeNode) {
                   if (checkBoxTreeCellRenderer.isOnHotspot(x - rect.x, y - rect.y)) { // pressed show/hide button
                       CheckBoxTreeNode checknode=(CheckBoxTreeNode)node;
                       checknode.setChecked(!checknode.isChecked()); // reverse Check
                       if (!checknode.isChecked()) { // uncheck parents if necessary
                          Object[] nodepath=path.getPath();
                          for (Object parentnode:nodepath) {
                              Object userObject=((CheckBoxTreeNode)parentnode).getUserObject();
                              if (userObject instanceof ModuleCRM || userObject instanceof ModuleMotif) break;
                              ((CheckBoxTreeNode)parentnode).setChecked(false);
                          }
                       }
                       if (!checknode.isLeaf()) {
                           Object userObject=checknode.getUserObject();
                           if (userObject==null || !(userObject instanceof ModuleCRM)) setCheckedRecursively(checknode,checknode.isChecked());
                       }
                       gui.redraw();
                       motifsTree.repaint();
	            } else { // pressed the label
                       Object val=((CheckBoxTreeNode)node).getUserObject();
                       mousePressedInTree(e,val);                  
                    }                   
                }
                //
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                int row = motifsTree.getRowForLocation(e.getX(), e.getY());
                if (row<0) return;
                if (!motifsTree.isRowSelected(row)) motifsTree.setSelectionRow(row);
                mouseReleasedInTree(e); 
                //motifsTree.repaint();
            }            
        });       
        motifsTree.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                gui.setSearchTargetComponent(MotifsPanel.this);
            }

        });
        selectContentCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selected=(String)selectContentCombobox.getSelectedItem();
                groupButton.setEnabled(selected.equals(SHOW_MOTIFS));
                groupButtonIcon.setForegroundColor((selected.equals(SHOW_MOTIFS))?Color.BLACK:Color.GRAY);
                     if (selected.equals(SHOW_MOTIFS)) setGroupingByMotif();
                else if (selected.equals(SHOW_MOTIF_COLLECTIONS)) setGroupingByMotifCollection();
                else if (selected.equals(SHOW_MOTIF_PARTITIONS)) setGroupingByMotifPartition();
                else if (selected.equals(SHOW_MODULES)) setGroupingByModules();
                else if (selected.equals(SHOW_MODULE_COLLECTIONS)) setGroupingByModuleCollection();
                else if (selected.equals(SHOW_MODULE_PARTITIONS)) setGroupingByModulePartition();
            }
        });
        selectContentCombobox.setSelectedItem(SHOW_MOTIFS);

  }
  
    private void notifyKeyListeners(KeyEvent e) {
        if (dataPanelListeners==null) return;
        Data[] selectedData=getSelectedDataElements();
        for (PanelListener listener:dataPanelListeners) {
            listener.keyPressed(this, selectedData, e);
        }
    }  
    
    /**
     * Returns an array with all the selected data elements in this panel (the contents could be heterogeneous)
     * @return 
     */
    public Data[] getSelectedDataElements() {
        int[] selection=motifsTree.getSelectionRows();
        if (selection==null || selection.length==0) return new Data[0];
        int countOK=0;
        for (int selectionIndex=0;selectionIndex<selection.length;selectionIndex++) {
            CheckBoxTreeNode treenode=(CheckBoxTreeNode)motifsTree.getPathForRow(selection[selectionIndex]).getLastPathComponent();
            Object userObject=treenode.getUserObject();
            if (userObject instanceof Data) countOK++;       
        }
        if (countOK==0) return new Data[0];
        Data[] dataitems=new Data[countOK];
        int dataindex=0;
        for (int selectionIndex=0;selectionIndex<selection.length;selectionIndex++) {
            CheckBoxTreeNode treenode=(CheckBoxTreeNode)motifsTree.getPathForRow(selection[selectionIndex]).getLastPathComponent();
            Object userObject=treenode.getUserObject();
            if (userObject instanceof Data) {
                dataitems[dataindex]=(Data)userObject;
                dataindex++;
            }               
        }               
        return dataitems;
    }    

  /**
   * Returns the combobox used to select what types of data items to show in this panel
   */
  public JComboBox getContentCombobox() {
     return selectContentCombobox;       
  }  
  
  /**
   * Shows the selected panel (for motifs, modules, collections or partitions)
   * @param type Use of the string constants in this class beginning with SHOW_
   */
  public void showPanel(String type) {
     selectContentCombobox.setSelectedItem(type);       
  }
  

  
/** Sets the check-status of all children of the specified node (if any) to the specified check value.
 * Note that the check-status of the specified node itself is not updated!
 */  
private void setCheckedRecursively(CheckBoxTreeNode node, boolean check) {
    int size=node.getChildCount();
    for (int i=0;i<size;i++) {
      CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(i);
      child.setChecked(check);
      if (!child.isLeaf()) setCheckedRecursively(child,check);
    }
}


/** Sets the processed-status of all children of the specified node (if any) to the specified value.
 * The processed status of the node itself is also set!
 */  
private void setProcessedRecursively(CheckBoxTreeNode node, boolean processed) {
    node.setProcessed(processed);
    int size=node.getChildCount();
    for (int i=0;i<size;i++) {
      CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(i);
      child.setProcessed(processed);
      if (!child.isLeaf()) setProcessedRecursively(child,processed);
    }
}


private void mousePressedInTree(java.awt.event.MouseEvent evt, Object selectedData) {                                             
    if (evt.getButton()==MouseEvent.BUTTON1 && evt.getClickCount()==2) {
         if (selectedData instanceof Data) showPrompt((Data)selectedData, true, false);
    }
    if (evt.isPopupTrigger()) {
        JPopupMenu popup=MotifsPanelContextMenu.getInstance(motifsTree,gui,this);
        if (dataPanelListeners!=null && popup!=null) {
            Data[] selected=getSelectedDataElements();
            for (PanelListener datapanelListener:dataPanelListeners) {
                datapanelListener.showContextMenu((MotifsPanel)this, selected, popup);
            }
        }
        if (popup!=null) popup.show(evt.getComponent(), evt.getX(),evt.getY());
    }  
}                                            

private void mouseReleasedInTree(java.awt.event.MouseEvent evt) {                                              
    if (evt.isPopupTrigger()) {
        JPopupMenu popup=MotifsPanelContextMenu.getInstance(motifsTree,gui,this);
        if (dataPanelListeners!=null && popup!=null) {
            Data[] selected=getSelectedDataElements();
            for (PanelListener datapanelListener:dataPanelListeners) {
                datapanelListener.showContextMenu((MotifsPanel)this, selected, popup);
            }
        }         
        if (popup!=null) popup.show(evt.getComponent(), evt.getX(),evt.getY());       
    }
}                                             

private int lastPrompt_X=-1, lastPrompt_Y=-1;

/** Shows a prompt to edit the selected data object */
public void showPrompt(final Data selectedData, boolean editable, boolean modal) {
    //gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
     Prompt prompt=null;
     if (selectedData instanceof Motif) {
          prompt=new Prompt_Motif(gui, null, (Motif)selectedData.clone(),settings, modal);        
     } else if (selectedData instanceof MotifCollection) {
          prompt=new Prompt_MotifCollection(gui, null, (MotifCollection)selectedData.clone(), modal);
     } else if (selectedData instanceof MotifPartition) {
          prompt=new Prompt_MotifPartition(gui, null, (MotifPartition)selectedData.clone(), modal);
     } else if (selectedData instanceof ModuleCRM) {
          prompt=new Prompt_Module(gui, null, (ModuleCRM)selectedData.clone(), modal);
     } else if (selectedData instanceof ModuleCollection) {
          prompt=new Prompt_ModuleCollection(gui, null, (ModuleCollection)selectedData.clone(), modal);
     } else if (selectedData instanceof ModulePartition) {
          prompt=new Prompt_ModulePartition(gui, null, (ModulePartition)selectedData.clone(), modal);
     } else {gui.logMessage("Unknown data type "+selectedData.getTypeDescription()); return;}
    
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
     prompt.setDataEditable(editable);
     if (editable) {
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
     }
     prompt.setVisible(true);    
}


/** */
public void setColorOnSelectedRows(Color color) {
    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
    if (rows==null) return;    
    Arrays.sort(rows);
    HashSet<String> affectedClasses=new HashSet<String>();
    for (int row:rows) {
        CheckBoxTreeNode node=(CheckBoxTreeNode)motifsTree.getPathForRow(row).getLastPathComponent();
        if (node.isProcessed()) continue;
        if (node.isLeaf()) { // let us say that classes or groups can be leaf nodes too...
            Object data=node.getUserObject();
            if (data instanceof Motif) {
                settings.setFeatureColor(data.toString(), color, false);
                if (!(model instanceof MotifsPanelTreeModel_GroupByClass) && data instanceof Motif) {
                    String motifClassification=((Motif)data).getClassification();
                    if (motifClassification==null) motifClassification=MotifClassification.UNKNOWN_CLASS_LABEL;
                    affectedClasses.add(motifClassification);
                }
            } else if (data instanceof ModuleCRM) {
                settings.setFeatureColor(data.toString(), color, false);
            } else {
                if (model instanceof MotifsPanelTreeModel_GroupByClass) affectedClasses.add(node.getName());
            }                     
        } // end node is leaf
        else if (node.getUserObject() instanceof ModuleCRM) {
            settings.setFeatureColor(((ModuleCRM)node.getUserObject()).getName(), color, false);
        }
        else if (node.getUserObject() instanceof ModuleMotif) {
            CheckBoxTreeNode modulenode=(CheckBoxTreeNode)node.getParent();
            ModuleCRM cisRegModule=(ModuleCRM)modulenode.getUserObject();
            settings.setFeatureColor(cisRegModule.getName()+"."+((ModuleMotif)node.getUserObject()).getRepresentativeName(), color, false);
        }
        else { // internal node (class or group (MotifCollection/ModuleCollection) )
            String nodeName=node.getName();
            settings.setClassLabelColor(nodeName,color);
            TreeNode parent=node.getParent();
            if (parent instanceof CheckBoxTreeNode && !((CheckBoxTreeNode)parent).isProcessed()) clearParentColors(node);
            if (model instanceof MotifsPanelTreeModel_GroupByClass) {
                affectedClasses.add(nodeName);
                setColorsRecursively(node, color,null);
            }
            else setColorsRecursively(node, color,affectedClasses);
            setProcessedRecursively(node, true);
        } 
    }   
    if (model instanceof MotifsPanelTreeModel_GroupByClass) setSubClassColors(affectedClasses,color);
    else clearSuperClassColors(affectedClasses);
    gui.redraw();
    setProcessedRecursively((CheckBoxTreeNode)motifsTree.getModel().getRoot(),false); // clear processed flags
}

private void setSubClassColors(HashSet<String> affected, Color newColor) {
    for (String classname:affected) {
        ArrayList<String> list=MotifClassification.getAllSubLevels(classname);
        for (String c:list) {
            settings.setClassLabelColor(c,newColor);
        }
    }
}
private void clearSuperClassColors(HashSet<String> affected) {
    for (String classname:affected) {
        if (classname.equals(MotifClassification.UNKNOWN_CLASS_LABEL)) {
            settings.setClassLabelColor(MotifClassification.UNKNOWN_CLASS_LABEL,null);
        } else {
            ArrayList<String> list=MotifClassification.getAllSuperLevels(classname);
            for (String c:list) {
                settings.setClassLabelColor(c,null);
            }
        }
    }
}

private void clearParentColors(CheckBoxTreeNode node) {
    TreeNode[] pathtoroot=node.getPath();
    for (int i=pathtoroot.length-2;i>0;i--) {
        if (pathtoroot[i] instanceof CheckBoxTreeNode) {
            CheckBoxTreeNode parent=(CheckBoxTreeNode)pathtoroot[i];
            if (parent.isProcessed()) break;
            else {
                settings.setClassLabelColor(parent.getName(),null);
                parent.setProcessed(true);
            }
        }
    }    
}

/** 
 * Sets the color of all motifs below the specified parent node to the given color
 */  
private void setColorsRecursively(CheckBoxTreeNode node, Color color, HashSet<String> labels) {
    int size=node.getChildCount();
    for (int i=0;i<size;i++) {
      CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(i);
      if (!child.isLeaf()) {
          settings.setClassLabelColor(child.getName(),color);
          setColorsRecursively(child,color, labels);
      }
      else {
          Object data=child.getUserObject();
          if (data instanceof Motif) {
              settings.setFeatureColor(data.toString(), color, false);
              if (labels!=null) {
                  String motifClass=((Motif)data).getClassification();
                  if (motifClass==null) motifClass=MotifClassification.UNKNOWN_CLASS_LABEL;
                  labels.add(motifClass);
              }
          } else if (data instanceof ModuleCRM) {
              settings.setFeatureColor(data.toString(), color, false);
          }
      }
    }
}

public void showOnlySelectedRows() {
    setVisibilityRecursively((CheckBoxTreeNode)motifsTree.getModel().getRoot(),false);
    setVisibilityOnSelectedRows(true);
}


    
public void setVisibilityOnSelectedRows(boolean show) {
    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
    if (rows==null) return;
    Arrays.sort(rows);
    for (int row:rows) {
        CheckBoxTreeNode node=(CheckBoxTreeNode)motifsTree.getPathForRow(row).getLastPathComponent();
        if (node.isProcessed()) continue;
        if (node.isLeaf()) {
            Object data=node.getUserObject();
            if (data instanceof Motif || data instanceof ModuleCRM) {settings.setRegionTypeVisible(data.toString(), show, false);}
        }
        else if (node.getUserObject() instanceof ModuleCRM) {
            node.setChecked(show);
            settings.setRegionTypeVisible(node.getUserObject().toString(), show, false);
        }
        else if (node.getUserObject() instanceof ModuleMotif) {
            node.setChecked(show);
            CheckBoxTreeNode modulenode=(CheckBoxTreeNode)node.getParent();
            ModuleCRM cisRegModule=(ModuleCRM)modulenode.getUserObject();
            settings.setRegionTypeVisible(cisRegModule.toString()+"."+((ModuleMotif)node.getUserObject()).getRepresentativeName(), show, false);
            setVisibilityRecursively(node, show);
            setProcessedRecursively(node, true);
        }
        else {
            node.setChecked(show);
            setVisibilityRecursively(node, show);
            setProcessedRecursively(node, true);
        } 
    }                    
    setProcessedRecursively((CheckBoxTreeNode)motifsTree.getModel().getRoot(),false); // clear processed flags
    gui.redraw();
}


/** 
 * Sets the visibility of all motifs below the specified parent node to the given boolean value
 */  
private void setVisibilityRecursively(CheckBoxTreeNode node, boolean show) {
    int size=node.getChildCount();
    for (int i=0;i<size;i++) {
      CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(i);
      if (!child.isLeaf()) {child.setChecked(show);setVisibilityRecursively(child,show);}
      else {
          Object data=child.getUserObject();
          if (data instanceof Motif || data instanceof ModuleCRM) {settings.setRegionTypeVisible(data.toString(), show, false);}
      }
    }
}

/** Sets the visibility on all nodes in the panel recursively from the root node*/
public void setVisibilityOnAll(boolean show) {
    setVisibilityRecursively((CheckBoxTreeNode)motifsTree.getModel().getRoot(),show);
    gui.redraw();
}

/** Selected rows that are not leafs will be expanded (as will their children) */
public void expandSelectedRows() {
    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
    if (rows==null) return;
    Arrays.sort(rows);
    for (int i=rows.length-1;i>=0;i--) {
        int row=rows[i];  
        TreePath path = motifsTree.getPathForRow(row);  
        expandPathRecursively(path);  
    }                    
    this.repaint();
}

/** Selected rows that are not leafs will be collapsed (as will their children) */
public void collapseSelectedRows() {
    int[] rows=motifsTree.getSelectionModel().getSelectionRows();
    if (rows==null) return;
    Arrays.sort(rows);
    for (int i=rows.length-1;i>=0;i--) {
        int row=rows[i];  
        TreePath path = motifsTree.getPathForRow(row);  
        collapsePathRecursively(path); 
    }                    
    this.repaint();
}

/** Selected rows that are not leafs will be expanded (as will their children) */
public void expandAllRows() {
    int rows=motifsTree.getRowCount();//  
    if (rows==0) return;
    for (int row=rows-1;row>=0;row--) {
        TreePath path = motifsTree.getPathForRow(row);  
        expandPathRecursively(path);  
    }                    
    this.repaint();
}

/** All rows that are not leafs will be collapsed (as will their children) */
public void collapseAllRows() {
    int rows=motifsTree.getRowCount();//  
    if (rows==0) return;
    for (int row=rows-1;row>=0;row--) {
        TreePath path = motifsTree.getPathForRow(row);  
        collapsePathRecursively(path); 
    }                    
    this.repaint();
}

private void expandPathRecursively(TreePath path) {
    motifsTree.expandPath(path);
    CheckBoxTreeNode node=(CheckBoxTreeNode)path.getLastPathComponent();
    if (!node.isLeaf()) {
        for (int i=0;i<node.getChildCount();i++) {
            CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(i);
            expandPathRecursively(path.pathByAddingChild(child));
        }
    }
}

private void collapsePathRecursively(TreePath path) {    
    CheckBoxTreeNode node=(CheckBoxTreeNode)path.getLastPathComponent();
    if (!node.isLeaf()) {
        for (int i=0;i<node.getChildCount();i++) {
            CheckBoxTreeNode child=(CheckBoxTreeNode)node.getChildAt(i);
            collapsePathRecursively(path.pathByAddingChild(child));
        }
        motifsTree.collapsePath(path);
    }   
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
 
 /**
  * This method is called when the "group motifs" button (next to the panel label) is pressed
  */
 private void setGrouping(java.awt.event.MouseEvent evt) {
    Component invoker=evt.getComponent();
    int x=evt.getX();
    int y=evt.getY();
    groupPopupMenu.updateMenu();
    groupPopupMenu.checkColorByClassMenuItem(settings.shouldColorMotifsByClass());
    groupPopupMenu.show(invoker, x,y);
 }

 private void setGroupingByMotif() {
    int levels=groupPopupMenu.getGroupLevels();
    if (levels==0) setGroupingNone();
    else setGroupingByClass(levels);
 }
 
 private void setGroupingNone() {
     if (model!=null) model.removeTreeModelListener(treelistener);
     model=new MotifsPanelTreeModel_NoGroup(gui.getEngine(),this);
     model.addTreeModelListener(treelistener);
     motifsTree.setModel(model);
     //motifCollectionPanelLabel.setText("Motifs");

 }

 private void setGroupingByMotifCollection() {
     if (model!=null) model.removeTreeModelListener(treelistener);
     model=new MotifsPanelTreeModel_GroupByCollection(gui.getEngine(),this);
     model.addTreeModelListener(treelistener);
     motifsTree.setModel(model);
     //motifCollectionPanelLabel.setText("Motif Collections");
 }

  private void setGroupingByMotifPartition() {
     if (model!=null) model.removeTreeModelListener(treelistener);
     model=new MotifsPanelTreeModel_GroupByPartition(gui.getEngine(),this);
     model.addTreeModelListener(treelistener);
     motifsTree.setModel(model);
     //motifCollectionPanelLabel.setText("Motif Collections");
 }

 private void setGroupingByClass(int levels) {
     if (model!=null) model.removeTreeModelListener(treelistener);
     model=new MotifsPanelTreeModel_GroupByClass(gui.getEngine(),levels,this);
     model.addTreeModelListener(treelistener);
     motifsTree.setModel(model);
     //motifCollectionPanelLabel.setText("Motifs by class");
 }

 private void setGroupingByModules() {
     if (model!=null) model.removeTreeModelListener(treelistener);
     model=new MotifsPanelTreeModel_GroupByModules(gui.getEngine(),this);
     model.addTreeModelListener(treelistener);
     motifsTree.setModel(model);
     //motifCollectionPanelLabel.setText("Modules");
 }
 private void setGroupingByModuleCollection() {
     if (model!=null) model.removeTreeModelListener(treelistener);
     model=new MotifsPanelTreeModel_GroupByModuleCollection(gui.getEngine(),this);
     model.addTreeModelListener(treelistener);
     motifsTree.setModel(model);
     //motifCollectionPanelLabel.setText("ModuleCRM Collections");
 }
 private void setGroupingByModulePartition() {
     if (model!=null) model.removeTreeModelListener(treelistener);
     model=new MotifsPanelTreeModel_GroupByModulePartition(gui.getEngine(),this);
     model.addTreeModelListener(treelistener);
     motifsTree.setModel(model);
     //motifCollectionPanelLabel.setText("ModuleCRM Collections");
 }
 


 
// // ---- data listener interface ----
//    @Override public void dataAdded(Data data) {}
//    @Override public void dataAddedToSet(Data parentDataset, Data child) {}
//    @Override public void dataRemoved(Data data) {}
//    @Override public void dataRemovedFromSet(Data parentDataset, Data child) {}
//    @Override public void dataUpdated(Data data) {}
//    @Override public void dataUpdate(Data oldvalue, Data newvalue) {}

    /** Removes all data from this panel */
    public void clearAll(boolean justmodules) {        
        if (model!=null) {
            if (justmodules) {
               if (model instanceof MotifsPanelTreeModel_GroupByModules || model instanceof MotifsPanelTreeModel_GroupByModuleCollection || model instanceof MotifsPanelTreeModel_GroupByModulePartition) model.clearAll(); 
            } else model.clearAll();  
        }
    }


 // ----- Searchable interface -----
    String previoussearchString="";
    @Override
    public boolean find(String searchstring) {
         CheckBoxTreeNode rootNode=(CheckBoxTreeNode)model.getRoot();
         if (rootNode==null || rootNode.isLeaf()) return false;
         TreePath selectedpath=motifsTree.getSelectionPath();
         CheckBoxTreeNode searchFromNode=null;
         if (selectedpath!=null && previoussearchString.equalsIgnoreCase(searchstring)) {            
             searchFromNode=(CheckBoxTreeNode)selectedpath.getLastPathComponent();
         }
         previoussearchString=searchstring;
         boolean[] passedStartNode=new boolean[]{searchFromNode==null};
         CheckBoxTreeNode matchingnode=findNextRecursively(rootNode, searchFromNode, searchstring, passedStartNode);
         if (matchingnode!=null) {
             String name=getSearchNameFromNode(matchingnode);
             gui.statusMessage("["+selectContentCombobox.getSelectedItem().toString()+"] Searching for '" + searchstring + "'. Found "+name); //  
             TreePath path=new TreePath(matchingnode.getPath());
             motifsTree.setSelectionPath(path);
             motifsTree.scrollPathToVisible(path);
             return true;
         } else {
             gui.statusMessage("["+selectContentCombobox.getSelectedItem().toString()+"] Searching for '" + searchstring + "'. No more matches found.");
             previoussearchString=""; // this will reset the search and wrap around
             return false;
         }
    }

    private String getSearchNameFromNode(CheckBoxTreeNode node) {
            Object valueAsObject=node.getUserObject();
            String nodetext;
            if (valueAsObject instanceof String) {
               String stringValue=(String)valueAsObject;
               if (stringValue.matches("^[0-9\\.]+")) { // node represents a motif class
                  nodetext=stringValue+":"+MotifClassification.getNameForClass(stringValue);
               } else nodetext=stringValue;
            } else if (valueAsObject instanceof ModuleMotif) {
                  nodetext=((ModuleMotif)valueAsObject).getRepresentativeName();
            } else if (valueAsObject instanceof Motif) {
                  nodetext=((Motif)valueAsObject).getPresentationName();
            } else if (valueAsObject instanceof Data) {
                  nodetext=((Data)valueAsObject).getName();
            } else if (valueAsObject!=null) {
                  nodetext=valueAsObject.toString();
            } else nodetext="";     
            return nodetext;
    }
    
    /** Searches the children of the given parent node (and their children recursively) to see if any of them matches the searchstring
     *  @return the first node encountered (after searchFromNode) that matches the searchstring (or null)
     */
    private CheckBoxTreeNode findNextRecursively(CheckBoxTreeNode parent, CheckBoxTreeNode searchFromNode, String searchstring, boolean[] passedStartNode) {
        int size=parent.getChildCount();
        for (int i=0;i<size;i++) {
            CheckBoxTreeNode node=(CheckBoxTreeNode)parent.getChildAt(i);
            String nodetext=getSearchNameFromNode(node);
            if (nodetext.matches("(?i).*"+searchstring+".*")) {
                if (passedStartNode[0]) return node;
            }
            if (node==searchFromNode) passedStartNode[0]=true;
            if (!node.isLeaf()) {
               CheckBoxTreeNode found=findNextRecursively(node,searchFromNode,searchstring,passedStartNode);
               if (found!=null) return found;
            }
        }
        return null;
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
 
 


 
 
 // ------------------------------------------- INNER CLASSES ---------------------------------

 
 private class addDataPopupMenu extends JPopupMenu {
     public addDataPopupMenu() {
         JMenuItem addMotif=new JMenuItem(Motif.getType());
         JMenuItem addMotifCollection=new JMenuItem(MotifCollection.getType());
         JMenuItem addMotifPartition=new JMenuItem(MotifPartition.getType());
         JMenuItem addModule=new JMenuItem(ModuleCRM.getType());
         JMenuItem addModuleCollection=new JMenuItem(ModuleCollection.getType());
         JMenuItem addModulePartition=new JMenuItem(ModulePartition.getType());
         addDataPopupMenuListener listener=new addDataPopupMenuListener();
         addMotif.addActionListener(listener);
         addMotifCollection.addActionListener(listener);
         addMotifPartition.addActionListener(listener);
         addModule.addActionListener(listener);
         addModuleCollection.addActionListener(listener);
         addModulePartition.addActionListener(listener);
         add(addMotif);
         add(addMotifCollection);
         add(addMotifPartition);
         add(new JSeparator());
         add(addModule);
         add(addModuleCollection);
         add(addModulePartition);
     }
 }
 
 private class addDataPopupMenuListener implements ActionListener {
     @Override
     public void actionPerformed(ActionEvent e) {
         gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         Prompt prompt=null;
         String type=e.getActionCommand();
              if (type.equals(MotifCollection.getType())) prompt=new Prompt_MotifCollection(gui, null, null);
         else if (type.equals(Motif.getType())) prompt=new Prompt_Motif(gui, null, null,settings);
         else if (type.equals(MotifPartition.getType())) prompt=new Prompt_MotifPartition(gui, null, null);
         else if (type.equals(ModuleCRM.getType())) prompt=new Prompt_Module(gui, null, null);
         else if (type.equals(ModuleCollection.getType())) prompt=new Prompt_ModuleCollection(gui, null, null);
         else if (type.equals(ModulePartition.getType())) prompt=new Prompt_ModulePartition(gui, null, null);
         prompt.setLocation(gui.getFrame().getWidth()/2-prompt.getWidth()/2, gui.getFrame().getHeight()/2-prompt.getHeight()/2);
         prompt.setVisible(true);
         gui.getFrame().setCursor(Cursor.getDefaultCursor());
         if (prompt.isOKPressed()) {
            Data variable=prompt.getData();
            boolean silent=true;// silent=prompt.isSilentOK();
            if (variable==null) return;
            OperationTask task=new OperationTask("new "+type);
            task.setParameter(OperationTask.OPERATION_NAME, "new");
            task.setParameter(Operation_new.DATA_TYPE, type);
            task.setParameter(OperationTask.TARGET_NAME,variable.getName());
            task.setParameter(OperationTask.ENGINE,gui.getEngine());
            task.setParameter(Operation_new.PARAMETERS, variable.getValueAsParameterString());
            if (silent) task.setParameter("_SHOW_RESULTS",Boolean.FALSE);
            if (prompt.isDataImportedFromFile()) {
                prompt.setImportFromFileSettingsInTask(task); // sets FILENAME, DATA_FORMAT and DATA_FORMAT_SETTINGS parameters
            }
            variable.setAdditionalOperationNewTaskParameters(task);
            Class datatype=gui.getEngine().getDataClassForTypeName(type);
            task.addAffectedDataObject(variable.getName(), datatype);
            gui.launchOperationTask(task,gui.isRecording());
            // change panel to the type of the newly added data item, so users will see that it has been added
            setContentType(type);
         }
    } 
}
 /** Sets the layout of the panel to show data items of the given type
  *  @param type A string specifying the data item type. Use the static getType() method in data class, eg MotifCollection.getType() or ModuleCRM.getType() 
  */
 public void setContentType(String type) {
          if (type.equals(MotifCollection.getType())) selectContentCombobox.setSelectedItem(SHOW_MOTIF_COLLECTIONS);
     else if (type.equals(Motif.getType())) selectContentCombobox.setSelectedItem(SHOW_MOTIFS);
     else if (type.equals(MotifPartition.getType())) selectContentCombobox.setSelectedItem(SHOW_MOTIF_PARTITIONS);
     else if (type.equals(ModuleCRM.getType())) selectContentCombobox.setSelectedItem(SHOW_MODULES);
     else if (type.equals(ModuleCollection.getType())) selectContentCombobox.setSelectedItem(SHOW_MODULE_COLLECTIONS);
     else if (type.equals(ModulePartition.getType())) selectContentCombobox.setSelectedItem(SHOW_MODULE_PARTITIONS);
 }

 private class groupPopupMenu extends JPopupMenu implements ActionListener {
     ButtonGroup buttonsgroup;
     JMenuItem noGroupsItem;
     JMenuItem level1Item;
     JMenuItem level2Item;
     JMenuItem level3Item;
     JMenuItem level4Item;
     JMenuItem level5Item;
     JMenuItem level6Item;     
     JMenuItem colorByClassItem;
     JMenu colorByPartitionMenu;
     ColorByPartitionListener colorByPartitionListener;

     public groupPopupMenu() {
         noGroupsItem=new JCheckBoxMenuItem(GROUP_NONE,true);
         level1Item=new JCheckBoxMenuItem(GROUP_BY_CLASS_1);
         level2Item=new JCheckBoxMenuItem(GROUP_BY_CLASS_2);
         level3Item=new JCheckBoxMenuItem(GROUP_BY_CLASS_3);
         level4Item=new JCheckBoxMenuItem(GROUP_BY_CLASS_4);
         level5Item=new JCheckBoxMenuItem(GROUP_BY_CLASS_5);
         level6Item=new JCheckBoxMenuItem(GROUP_BY_CLASS_6);         
         colorByClassItem=new JCheckBoxMenuItem("Color by class");
         colorByPartitionMenu=new JMenu("Color by Partition");
         colorByPartitionListener=new ColorByPartitionListener();      
         noGroupsItem.addActionListener(this);
         level1Item.addActionListener(this);
         level2Item.addActionListener(this);
         level3Item.addActionListener(this);
         level4Item.addActionListener(this);
         level5Item.addActionListener(this);
         level6Item.addActionListener(this);         
         colorByClassItem.addActionListener(this);
         add(noGroupsItem);
         add(level1Item);
         add(level2Item);
         add(level3Item);
         add(level4Item);
         add(level5Item);
         add(level6Item);         
         add(new JSeparator());
         add(colorByClassItem);
         add(colorByPartitionMenu); 
         buttonsgroup=new ButtonGroup();
         buttonsgroup.add(noGroupsItem);
         buttonsgroup.add(level1Item);      
         buttonsgroup.add(level2Item);         
         buttonsgroup.add(level3Item);         
         buttonsgroup.add(level4Item);   
         buttonsgroup.add(level5Item);         
         buttonsgroup.add(level6Item);          
     }
     
     public int getGroupLevels() {
              if (level1Item.isSelected()) return 1;
         else if (level2Item.isSelected()) return 2;
         else if (level3Item.isSelected()) return 3;
         else if (level4Item.isSelected()) return 4;
         else if (level5Item.isSelected()) return 5;
         else if (level6Item.isSelected()) return 6;         
         else return 0;
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd=e.getActionCommand();
        if (cmd.equals("Color by class")) {
            settings.setColorMotifsByClass(!settings.shouldColorMotifsByClass()); // going from not color by class to do color by class -> set colors again!
            motifsTree.repaint();
        } else {
            setGroupingByMotif(); // this will also select correct grouping based on the selection
        }
    }


    public void checkColorByClassMenuItem(boolean flag) {colorByClassItem.setSelected(flag);}
    
    private class ColorByPartitionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String partitionName=e.getActionCommand();
            Data partition=gui.getEngine().getDataItem(partitionName);
            if (partition instanceof MotifPartition) {
               ArrayList<String> clusters=((MotifPartition)partition).getClusterNames();
               for (String cluster:clusters) {
                   Color color=settings.getClusterColor(cluster);
                   ArrayList<String> list=((MotifPartition)partition).getAllMotifNamesInCluster(cluster);
                   String[] stringAsArray=new String[list.size()];
                   stringAsArray=list.toArray(stringAsArray);
                   settings.setFeatureColor(stringAsArray, color, false);
               }  
               settings.setColorMotifsByClass(false);
               checkColorByClassMenuItem(false);
               gui.redraw();
            }
        }  
    }    
    
    public void updateMenu() {
       colorByPartitionMenu.removeAll();
       MotifLabEngine engine=gui.getEngine();
       for (String partitionName:engine.getNamesForAllDataItemsOfType(MotifPartition.class)) {
           JMenuItem partitionItem=new JMenuItem(partitionName);   
           partitionItem.addActionListener(colorByPartitionListener);
           colorByPartitionMenu.add(partitionItem);
       }
       colorByPartitionMenu.setEnabled(colorByPartitionMenu.getItemCount()>0);          
    }
 }
 

 
private class CheckBoxTreeCellRenderer extends JPanel implements TreeCellRenderer {
   DefaultTreeCellRenderer tcr;
   JCheckBox button = new JCheckBox();
   JLabel label = new JLabel();
   SimpleDataPanelIcon unselectedIcon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
   SimpleDataPanelIcon selectedicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
   
   public boolean isOnHotspot(int x, int y) {
     return (button.getBounds().contains(x, y));
   }

   public CheckBoxTreeCellRenderer() {
     super();
     tcr=new DefaultTreeCellRenderer();
     label.setOpaque(false);
     button.setOpaque(false);
     setOpaque(false);
     Dimension d=new Dimension(18,18);
     button.setMinimumSize(d);
     button.setPreferredSize(d);
     button.setMaximumSize(d);      
     //setLayout(new FlowLayout(FlowLayout.LEADING,0,0));
     setLayout(new FlowLayout(FlowLayout.LEADING,0,0));
     add(button,BorderLayout.WEST);    
     add(label,BorderLayout.CENTER);
     unselectedIcon.setForegroundColor(java.awt.Color.WHITE);
     unselectedIcon.setBorderColor(java.awt.Color.LIGHT_GRAY);
     selectedicon.set3D(true);
   }
 
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded,boolean leaf, int row,boolean hasFocus) {
        if (isSelected) label.setForeground(tcr.getTextSelectionColor());
        else label.setForeground(tcr.getTextNonSelectionColor());
        button.setIcon(null);
        button.setSelectedIcon(null);   
        label.setForeground(Color.BLACK);
        
        if (value instanceof CheckBoxTreeNode) button.setSelected(((CheckBoxTreeNode)value).isChecked());
        boolean doColorByClass=settings.shouldColorMotifsByClass();
        Object valueAsObject=((DefaultMutableTreeNode)value).getUserObject();
        if (valueAsObject instanceof String && model instanceof MotifsPanelTreeModel_GroupByClass) {
           String stringValue=(String)valueAsObject;
           if (stringValue.matches("^[0-9\\.]+")) { // node represents a motif class               
               if (doColorByClass) {
                  Color labelColor=settings.getClassLabelColor(stringValue);
                  if (labelColor!=null) {
                      selectedicon.setForegroundColor(labelColor);
                      button.setSelectedIcon(selectedicon); 
                      button.setIcon(unselectedIcon);      
                  }
               }
               stringValue=stringValue+":"+MotifClassification.getNameForClass(stringValue);
           } else if (stringValue.equals(MotifClassification.UNKNOWN_CLASS_LABEL) && doColorByClass) {
                  Color labelColor=settings.getClassLabelColor(stringValue);
                  if (labelColor!=null) {
                      selectedicon.setForegroundColor(labelColor);
                      button.setSelectedIcon(selectedicon);                 
                      button.setIcon(unselectedIcon);  
                  }
           }
           setToolTipText("Transcription factor class");
           label.setText(stringValue);
           return this; 
        } else if (valueAsObject instanceof String) { // a String that should not be rendered as a class
           if (model instanceof MotifsPanelTreeModel_GroupByPartition || model instanceof MotifsPanelTreeModel_GroupByModulePartition) {
               CheckBoxTreeNode partitionnode=(CheckBoxTreeNode)((CheckBoxTreeNode)value).getParent();
               if (partitionnode!=null) {
                   Object obj=((CheckBoxTreeNode)partitionnode).getUserObject();
                   if (obj instanceof DataPartition && ((DataPartition)obj).containsCluster((String)valueAsObject)) {
                      int size=((DataPartition)obj).getClusterSize((String)valueAsObject); 
                      setToolTipText((String)valueAsObject+"   [ Cluster : "+size+" ]");
                   } else setToolTipText("?"); 
               } else setToolTipText(null);
           } else setToolTipText(null);
           label.setText((String)valueAsObject);
           return this;
        } else if (valueAsObject instanceof ModuleMotif) {
            CheckBoxTreeNode modulenode=(CheckBoxTreeNode)((CheckBoxTreeNode)value).getParent();
            ModuleCRM cisRegModule=(ModuleCRM)((CheckBoxTreeNode)modulenode).getUserObject();
            String modulemotifname=((ModuleMotif)valueAsObject).getRepresentativeName();
            Color modulemotifcolor=settings.getFeatureColor(cisRegModule.getName()+"."+modulemotifname);
            selectedicon.setForegroundColor(modulemotifcolor);
            button.setSelectedIcon(selectedicon);
            button.setIcon(unselectedIcon);
            setToolTipText("#MM#"+cisRegModule.getName()+":"+modulemotifname);
            label.setText(modulemotifname);
            return this;
        }

        Data data=null;
        if (valueAsObject instanceof Data) data=(Data)valueAsObject;
	String stringValue=null;
        if (data instanceof Motif) stringValue=((Motif)data).getPresentationName();
        else if (data instanceof ModuleCRM) stringValue=((ModuleCRM)data).getName();
        else stringValue=valueAsObject.toString();
        label.setText(stringValue);
        this.revalidate();
        
        if (data instanceof Motif) {
            String name=data.getName();
            //icon.setForegroundColor(settings.getFeatureColor(data.getName()));
            selectedicon.setForegroundColor(settings.getFeatureColor(name));
            button.setSelectedIcon(selectedicon);
            button.setIcon(unselectedIcon);
            setToolTipText(name); // use only the motif name here, since this will be used as a key to look up the motif itself          
            if (!settings.isRegionTypeVisible(name)) label.setForeground(Color.LIGHT_GRAY);
        } else if (data instanceof ModuleCRM) {
            String name=data.getName();
            selectedicon.setForegroundColor(settings.getFeatureColor(name));
            button.setSelectedIcon(selectedicon);
            button.setIcon(unselectedIcon);
            setToolTipText(name); //
            if (!settings.isRegionTypeVisible(name)) label.setForeground(Color.LIGHT_GRAY);
        } else if (data!=null) {
            setToolTipText(data.getName()+"   [ "+data.getTypeDescription()+" ]");
        } else {
            System.err.println("Got NULL data in TreeNodeRenderer");
            setToolTipText("ERROR: Nothing here...");    
        }      
        setComponentOrientation(tree.getComponentOrientation());	    
	//selected = isSelected;
        return this;
    }
    
}

private class TreeListener implements TreeModelListener {
    @Override public void treeNodesChanged(TreeModelEvent e) {}
    @Override public void treeNodesRemoved(TreeModelEvent e) {}
    @Override public void treeStructureChanged(TreeModelEvent e) {}
    @Override public void treeNodesInserted(TreeModelEvent e) {motifsTree.expandPath(e.getTreePath());}
}

}
