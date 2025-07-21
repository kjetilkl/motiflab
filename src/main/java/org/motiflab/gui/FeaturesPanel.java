
package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.ListModel;
import org.jdesktop.application.ResourceMap;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.SequenceCollection;


/**
 * This class encapsulates the Features dataset panel
 * 
 * @author kjetikl
 */
public class FeaturesPanel extends DataPanel {
    private JPanel labelpanel;
    private JLabel featuresPanelLabel;
    private JScrollPane featuresetListScrollPane;
    private JList featuresetList;
    private JButton addButton;
    private JButton groupButton;
    private MiscIcons groupButtonIcon;    
    private groupPopupMenu groupPopupMenu; 
    private MotifLabGUI gui;
    private VisualizationSettings visualizationSettings;
    private Class filter=org.motiflab.engine.data.FeatureDataset.class; // 
    private ArrayList<PanelListener> dataPanelListeners=null;

    
        
  public FeaturesPanel(MotifLabGUI gui) {
      this.gui=gui;
      visualizationSettings=gui.getVisualizationSettings();
      initComponents();      
  } 
  
  @Override
  public boolean holdsType(String type) {
      return (   type.equals(DNASequenceDataset.getType())
              || type.equals(NumericDataset.getType())
              || type.equals(RegionDataset.getType()));
  }   
  
  @Override
  public boolean holdsType(Data data) {
      return (   data instanceof DNASequenceDataset
              || data instanceof NumericDataset
              || data instanceof RegionDataset
              );
  }    
  
  public JList getFeaturesList() {
      return featuresetList;
  }  

  @Override
  public void clearSelection() {
      featuresetList.clearSelection();
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
  
  /**
   * Given an list containing names of feature dataset, this method will return these names 
   * (or a subset of them) according to the order they appear in the FeaturesPanel
   * @param list A list of names of feature datasets
   * @param skiphidden If TRUE only currently visible feature datasets will be included in the resulting list
   * @param aggregated
   * @return A list of names of feature datasets in sorted order
   */
  public ArrayList<String> sortFeaturesAccordingToPanelOrder(ArrayList<String> list, boolean skiphidden, boolean aggregated) {
      ArrayList<String> result=new ArrayList<String>(list.size());
      ListModel model=featuresetList.getModel();
      for (int i=0;i<model.getSize();i++) { // for each track in the panel
          String name=null;
          Object el=model.getElementAt(i);
          if (el instanceof Data) name=((Data)el).getName();
          else if (el instanceof String) name=(String)el;
          if (name!=null) {
              if (skiphidden && !visualizationSettings.isTrackVisible(name)) continue;
              if (list.contains(name)) result.add(name);
              else if (aggregated) {
                  String prefix=getAggregatePrefix(name);
                  if (prefix!=null && list.contains(prefix) && !(result.contains(prefix))) result.add(prefix);
              }
          }
      }
      return result;
  }
  
  private String getAggregatePrefix(String string) {
        int i=string.lastIndexOf('_');
        if (i>0 && i<string.length()-1) {
            String suffix=string.substring(i+1);
            try {
                Integer.parseInt(suffix);
                String prefix=string.substring(0, i);
                if (prefix.isEmpty()) return null;
                else return prefix;
            } catch (Exception e) {return null;}
        } else return null;     
  }
  
  private void initComponents() {
        ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.motiflab.gui.MotifLabApp.class).getContext().getResourceMap(FeaturesPanel.class);

        featuresPanelLabel=new JLabel();
        setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        setName("FeaturesPanel"); // NOI18N
        setLayout(new BorderLayout());

        featuresPanelLabel.setText(resourceMap.getString("FeaturesLabel.text")); // NOI18N
        featuresPanelLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 3, 1, 1));
        featuresPanelLabel.setName("FeaturesLabel"); // NOI18N
        
        addButton=new JButton(new MiscIcons(MiscIcons.ADD_DATA));
        addButton.setName("addButton");
        addButton.setToolTipText(resourceMap.getString("addButton.toolTipText")); //
        addButton.setIcon(new MiscIcons(MiscIcons.PLUS_ICON));
        //addButton.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
        Dimension dim=new Dimension(36,22);
        addButton.setMaximumSize(dim);
        addButton.setPreferredSize(dim);
        addButton.setMinimumSize(dim);
        addButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                addFeatureDataset();
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
        labelpanel.add(featuresPanelLabel,BorderLayout.CENTER);
        labelpanel.add(buttonspanel,BorderLayout.EAST);         
        add(labelpanel, java.awt.BorderLayout.PAGE_START);
        
        featuresetListScrollPane = new JScrollPane();
        featuresetListScrollPane.setName("FeaturesetListScrollPane"); // NOI18N

        featuresetList=new JList();        
        removePeskyKeyListener();
        featuresetList.setBackground(resourceMap.getColor("featuresetList.background")); // NOI18N
        featuresetList.setModel(new FeaturesPanelListModel(gui.getEngine(),filter,visualizationSettings));
        featuresetList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        featuresetList.setCellRenderer(new DataPanelCellRenderer(gui.getVisualizationSettings()));
        featuresetList.setDragEnabled(true);
        featuresetList.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        featuresetList.setName("featuresetList"); // NOI18N
        featuresetList.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                mouseWheelMovedInFeaturesPanel(evt);
            }
        });
        featuresetList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                mousePressedInFeaturesPanel(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                mouseReleasedInFeaturesPanel(evt);
            }
        });
        featuresetList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { 
                if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && e.isShiftDown() && !e.isControlDown()) {
                    boolean show=(e.getKeyCode()==KeyEvent.VK_S);
                    setVisibilityOnAll(show);  
                    notifyKeyListeners(e);
                    return;
                }                 
                int[] selected=featuresetList.getSelectedIndices();
                if (selected.length==0) {notifyKeyListeners(e);return;} // no tracks selected, but notify key listeners anyway
                if (e.getKeyCode()==KeyEvent.VK_SPACE || e.getKeyCode()==KeyEvent.VK_V) {
                    if (selected.length==1) toggleVisibility((FeatureDataset)featuresetList.getSelectedValue());
                    else {
                        int visible=0;
                        FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                        String[] names=new String[selected.length];
                        for (int i=0;i<selected.length;i++) {
                            names[i]=((FeatureDataset)model.getElementAt(selected[i])).getName();
                            if (visualizationSettings.isTrackVisible(names[i])) visible++;
                        }
                        boolean show=true;
                        if (visible==selected.length) show=false; // all selected datatracks are currently visible so hide all
                        visualizationSettings.setTrackVisible(names, show);
                    }
                }
                else if ((e.getKeyCode()==KeyEvent.VK_S || e.getKeyCode()==KeyEvent.VK_H) && !e.isShiftDown() && !e.isControlDown()) {
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    String[] names=new String[selected.length];
                    for (int i=0;i<selected.length;i++) {
                        names[i]=((FeatureDataset)model.getElementAt(selected[i])).getName();
                    }
                    boolean show=(e.getKeyCode()==KeyEvent.VK_S);
                    visualizationSettings.setTrackVisible(names, show);                  
                }                
                else if (e.getKeyCode()==KeyEvent.VK_O && !e.isControlDown()) {
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    String[] names=new String[selected.length];
                    for (int i=0;i<selected.length;i++) {
                        names[i]=((FeatureDataset)model.getElementAt(selected[i])).getName();
                    }
                    showOnlyTheseTracks(names);                  
                }                               
                else if (e.getKeyCode()==KeyEvent.VK_DELETE) {
                    String[] names=new String[selected.length];
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int i=0;i<selected.length;i++) {
                        names[i]=((FeatureDataset)model.getElementAt(selected[i])).getName();
                    }
                    gui.deleteDataItems(names); // queues and undoable event
                }
                else if (e.getKeyCode()==KeyEvent.VK_PLUS || e.getKeyCode()==KeyEvent.VK_ADD) {
                    MouseWheelEvent mwe=new MouseWheelEvent(getFeaturesList(), e.getID(), e.getWhen(), e.getModifiers(), 0, 0, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1);
                    mouseWheelMovedInFeaturesPanel(mwe);
                }                
                else if (e.getKeyCode()==KeyEvent.VK_MINUS || e.getKeyCode()==KeyEvent.VK_SUBTRACT) {
                    MouseWheelEvent mwe=new MouseWheelEvent(getFeaturesList(), e.getID(), e.getWhen(), e.getModifiers(), 0, 0, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1);
                    mouseWheelMovedInFeaturesPanel(mwe);
                }
                else if (e.getKeyCode()==KeyEvent.VK_M) { // toggle multicolor mode ("color by type") for region datasets
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset) {
                               boolean currentValue=visualizationSettings.useMultiColoredRegions(dataset.getName());
                               visualizationSettings.setUseMultiColoredRegions(dataset.getName(),!currentValue);
                          }
                    }
                }
                else if (e.getKeyCode()==KeyEvent.VK_C) {
                    cycleColor(1);
                }
                else if (e.getKeyCode()==KeyEvent.VK_A) {
                    cycleAlpha();
                }                
                else if (e.getKeyCode()==KeyEvent.VK_D) { // toggle show strand (orientation)
                    boolean showStrand=false; // if all selected Region tracks are currently showing strand then keep showStrand=false else set showStrand=true;
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset && !visualizationSettings.shouldVisualizeRegionStrand(dataset.getName())) {showStrand=true;break;}
                    }
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset) visualizationSettings.setVisualizeRegionStrand(dataset.getName(),showStrand);
                    }
                }
                else if (e.getKeyCode()==KeyEvent.VK_R) { // toggle show score
                    boolean showScore=false; // if all selected Region tracks are currently showing score then keep showScore=false else set showScore=true;
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset && !visualizationSettings.shouldVisualizeRegionScore(dataset.getName())) {showScore=true;break;}
                    }
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset) visualizationSettings.setVisualizeRegionScore(dataset.getName(),showScore);
                    }
                }
                else if (e.getKeyCode()==KeyEvent.VK_G) { // cycle through graph types for numeric datasets
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    boolean isShiftDown=e.isShiftDown();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          String currentgraph=visualizationSettings.getGraphType(dataset.getName());
                          String nextgraph=gui.getNextGraphType(currentgraph, isShiftDown); // this should return the name of the next graph type (or previous is SHIFT is down)
                          if (nextgraph!=null) visualizationSettings.setGraphType(dataset.getName(),nextgraph);
                    }
                }             
                else if (e.getKeyCode()==KeyEvent.VK_X || e.getKeyCode()==KeyEvent.VK_E) { // toggle expansion
                    boolean currentSetting=getCommonExpansionSettings(selected);
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset) {
                             visualizationSettings.setExpanded(dataset.getName(),!currentSetting); // "toggle"
                          }
                    }
                }
                else if (e.getKeyCode()==KeyEvent.VK_N) { // toggle visualize nested regions
                    boolean currentSetting=getCommonNestedSettings(selected);
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset) {
                             visualizationSettings.setVisualizeNestedRegions(dataset.getName(),!currentSetting); // "toggle"
                          }
                    }
                }                           
                else if (e.getKeyCode()==KeyEvent.VK_P && !e.isControlDown()) { // output value as parameter string
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                        FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                        String parameterString=dataset.getValueAsParameterString();
                        gui.logMessage(parameterString);
                    }
                }
                else if (e.getKeyCode()==KeyEvent.VK_Q) { // repack if expanded
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset) {
                              gui.getVisualizationPanel().repackRegions(dataset);
                          }
                    }
                } 
                else if (e.getKeyChar()=='(' || e.getKeyChar()==')') { // render region tracks "upsideDown" or normally
                    boolean renderUpsideDown=(e.getKeyChar()==')');
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    for (int index:selected) {
                          FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                          if (dataset instanceof RegionDataset) {
                              visualizationSettings.setRenderUpsideDown(dataset.getName(),renderUpsideDown);
                          }
                    }
                } 
                else if (e.getKeyCode()==KeyEvent.VK_U) { // sort sequences by feature (descending unless SHIFT is pressed also)
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();                    
                    FeatureDataset dataset=((FeatureDataset)model.getElementAt(selected[0]));
                    boolean ascending=e.isShiftDown();
                    if (dataset instanceof RegionDataset) {
                       SortSequencesDialog.sortBy(SortSequencesDialog.SORT_BY_VISIBLE_REGION_COUNT, ascending, dataset.getName(), null, gui); 
                    } else if (dataset instanceof NumericDataset) {
                       SortSequencesDialog.sortBy(SortSequencesDialog.SORT_BY_NUMERIC_TRACK_SUM, ascending, dataset.getName(), null, gui); 
                    } else if (dataset instanceof DNASequenceDataset) {
                       SortSequencesDialog.sortBy(SortSequencesDialog.SORT_BY_GC_CONTENT, ascending, dataset.getName(), null, gui); 
                    }                                                                     
                } 
                else if (e.getKeyCode()==KeyEvent.VK_T && !e.isShiftDown()) { // show only sequences with visible regions in selected track
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();                    
                    FeatureDataset dataset=((FeatureDataset)model.getElementAt(selected[0]));
                    if (dataset instanceof RegionDataset) {
                       SequenceCollection collection=gui.getEngine().getDefaultSequenceCollection();
                       String[] allSequenceNames=new String[collection.size()];
                       allSequenceNames=collection.getAllSequenceNames().toArray(allSequenceNames);                        
                       String[] sequences=getNamesOfSequencesWithVisibleRegions((RegionDataset)dataset);
                       visualizationSettings.setSequenceVisible(allSequenceNames,false);
                       visualizationSettings.setSequenceVisible(sequences, true);
                    }                                                                     
                } 
                else if (e.getKeyCode()==KeyEvent.VK_RIGHT || e.getKeyCode()==KeyEvent.VK_LEFT) { // GROUP (right array) or UNGROUP (left arrow) tracks
                    boolean group=(e.getKeyCode()==KeyEvent.VK_RIGHT); //
                    FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                    String[] names=new String[selected.length];
                    for (int i=0;i<selected.length;i++) {
                        names[i]=((FeatureDataset)model.getElementAt(selected[i])).getName();
                    }    
                    visualizationSettings.setGroupedTracks(names, group);
                } 
                else if (e.getKeyCode()==KeyEvent.VK_T && e.isShiftDown() && e.isControlDown()) { // SHIFT+CONTROL+T is used to test stuff during development
                    // --- Toggle optimization cutoff for region tracks ---                    
                    // if (DataTrackVisualizer_Region.optimize_cutoff==Integer.MAX_VALUE) DataTrackVisualizer_Region.optimize_cutoff=1000;
                    // else DataTrackVisualizer_Region.optimize_cutoff=Integer.MAX_VALUE;           
                    // visualizationSettings.debug();
                } 
                else { // check if there might be some keys registered for the common graph type
                     checkGraphTypeSpecificKeyEvents(selected, e.getKeyCode(), e.isShiftDown());                                      
                }
                // else gui.logMessage("Key["+e.getKeyCode()+"]="+e.toString()); 
                notifyKeyListeners(e);
            }
        });
        featuresetList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                gui.setSearchTargetComponent(FeaturesPanel.this);
            }

        });
        featuresetList.setTransferHandler(new FeaturesPanelTransferHandler(featuresetList,gui));
        featuresetList.getModel().addListDataListener(gui.getVisualizationSettings());
        featuresetListScrollPane.setViewportView(featuresetList);        
        add(featuresetListScrollPane, java.awt.BorderLayout.CENTER);
        groupPopupMenu=new groupPopupMenu();        
  }
  
    private void checkGraphTypeSpecificKeyEvents(int[] selected, int keyCode, boolean shiftDown) {
        String commonGraphType=getCommonGraphType(selected);             
        if (commonGraphType!=null) {
            Object graphType=gui.getEngine().getResource(commonGraphType, "DTV");
            if (graphType instanceof DataTrackVisualizer) {
                ArrayList<DataTrackVisualizerSetting> graphTypeSettings=((DataTrackVisualizer)graphType).getGraphTypeSettings();
                if (graphTypeSettings!=null) {
                    for (DataTrackVisualizerSetting setting:graphTypeSettings) {
                        if (keyCode==setting.shortCutKey()) { // this keyboard shortcut is registered with the DTV
                            String vizSettingName=setting.getVisualizationSettingName();
                            if (selected.length==1 || setting.allowMultipleTracks()) {
                                FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
                                if (setting.useDialog()) {
                                    FeatureDataset[] selectedDatasets=getSelectedDatasets(selected, model);
                                    JDialog dialog=((DataTrackVisualizer)graphType).getGraphTypeSettingDialog(setting.name(), selectedDatasets, visualizationSettings);
                                    if (dialog!=null) {
                                       dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);                                        
                                       dialog.setVisible(true);
                                       dialog.dispose();
                                    } else gui.errorMessage("GRAPH SETTING ERROR: Missing dialog for graph type '"+commonGraphType+"' setting '"+setting.name() +"'", 0);                                                                           
                                } else {
                                    Object commonValue=getCommonSetting(vizSettingName, selected);
                                    Object nextValue=setting.getNextOptionValue(commonValue, shiftDown);                                   
                                    for (int index:selected) {
                                       FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
                                       if (dataset instanceof RegionDataset || dataset instanceof DNASequenceDataset) {                         
                                           visualizationSettings.storeSetting(dataset.getName()+"."+vizSettingName,nextValue);
                                       }
                                    }                                       
                                }
                                gui.redraw();
                            }
                        }
                    }
                }                
            }
        }        
    } 
    
    private FeatureDataset[] getSelectedDatasets(int[] selected, FeaturesPanelListModel model) {
        if (selected==null || selected.length==0) return new FeatureDataset[0];
        FeatureDataset[] selectedDatasets=new FeatureDataset[selected.length];
        for (int i=0;i<selected.length;i++) {
            FeatureDataset dataset=((FeatureDataset)model.getElementAt(i)); 
            selectedDatasets[i]=dataset;
        }
        return selectedDatasets;
    }
  
    private void notifyKeyListeners(KeyEvent e) {
        if (dataPanelListeners==null) return;
        Data[] selectedData=getSelectedDataElements();
        for (PanelListener listener:dataPanelListeners) {
            listener.keyPressed(this, selectedData, e);
        }
    }
    
    public Data[] getSelectedDataElements() {
        Data[] selectedData=null;
        int[] selected=featuresetList.getSelectedIndices();
        if (selected.length==0) selectedData=new Data[0];
        else {
            FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
            selectedData=new Data[selected.length];
            for (int i=0;i<selected.length;i++) {
                selectedData[i]=(Data)model.getElementAt(selected[i]);
            } 
        }   
        return selectedData;
    }
  
    /** Returns the first Data item in the list that has the specific class type */
    public Data getFirstItemOfType(Class type) {
        Data result=null;
        int listsize=featuresetList.getModel().getSize();
        for (int i=0;i<listsize;i++) { // try to locate a DNASequenceDataset in the features panel list
            Object element=featuresetList.getModel().getElementAt(i);
            if (type.isAssignableFrom(element.getClass())) {result=(Data)element;break;}
        }   
        return result;
    }
    
   /** Returns a DNA track to be used as general reference for comparison with other. 
     * This could be either the first DNA track in the panel or the first DNA track found above another specified track 
     * @param aboveTrack If a track name is provided, the first DNA track found above this track will be returned (or first track if no DNA track is above this)
     */
    public DNASequenceDataset getReferenceDNAtrack(String aboveTrack) {
        DNASequenceDataset result=null;
        boolean aboveTrackFound=false;
        int listsize=featuresetList.getModel().getSize();
        for (int i=0;i<listsize;i++) { // try to locate a DNASequenceDataset in the features panel list
            Object element=featuresetList.getModel().getElementAt(i);
            if (element instanceof DNASequenceDataset) {
                result=(DNASequenceDataset)element;
                if (aboveTrack==null || aboveTrackFound) break;
            }
            if (((Data)element).getName().equals(aboveTrack)) aboveTrackFound=true;
            if (aboveTrackFound && result!=null) break; // 
        }   
        return result;       
    }    

    private boolean getCommonExpansionSettings(int[] selected) {
        int common=-1;
        FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
        for (int index:selected) {
           FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
           if (dataset instanceof RegionDataset) {
              boolean currentSetting=visualizationSettings.isExpanded(dataset.getName());
              if (common==-1) common=(currentSetting)?1:0;
              else if ((currentSetting && common==0) || (!currentSetting && common==1)) return false;
           }
        }
        return (common==1);
    }
    private boolean getCommonNestedSettings(int[] selected) {
        int common=-1;
        FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
        for (int index:selected) {
           FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
           if (dataset instanceof RegionDataset) {
              boolean currentSetting=visualizationSettings.visualizeNestedRegions(dataset.getName());
              if (common==-1) common=(currentSetting)?1:0;
              else if ((currentSetting && common==0) || (!currentSetting && common==1)) return true;
           }
        }
        return (common==1);
    }     
  
    /** Checks the selected entries in the panel and returns the common value that the selected entries have for the given setting
     *  Returns NULL if the entries do not have the same value
     */
    private Object getCommonSetting(String settingName, int[] selected) {
        if (selected==null || selected.length==0) return null;
        FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
        if (selected.length==1) {
            FeatureDataset dataset=((FeatureDataset)model.getElementAt(selected[0]));
            return visualizationSettings.getSetting(dataset.getName()+"."+settingName);
        }
        Object common=null;
        for (int index:selected) {
           FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
           Object currentSetting=visualizationSettings.getSetting(dataset.getName()+"."+settingName);
           if (common==null) common=currentSetting;
           else if (!common.equals(currentSetting)) return null;           
        }
        return common;
    }  
    
    /** Checks the selected entries in the panel to see if they use the same graph type (DataTrackVisualizer)
     *  @return the name of the common graph type used by all the selected tracks, or else null
     */
    private String getCommonGraphType(int[] selected) {
        if (selected==null || selected.length==0) return null;
        FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
        if (selected.length==1) {
            FeatureDataset dataset=((FeatureDataset)model.getElementAt(selected[0]));
            return visualizationSettings.getGraphType(dataset.getName());
        }
        String commonGraphType=null;
        for (int index:selected) {
           FeatureDataset dataset=((FeatureDataset)model.getElementAt(index));
           String currentGraphType=visualizationSettings.getGraphType(dataset.getName());
           if (commonGraphType==null) commonGraphType=currentGraphType;
           else if (!commonGraphType.equals(currentGraphType)) return null;           
        }
        return commonGraphType;
    }     
    
    
  /** Removes all Feature datasets from this panel */
  public void clearAll() {
      ((FeaturesPanelListModel)featuresetList.getModel()).clear();
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
     featuresetList.removeKeyListener(null);
     for (KeyListener kl:featuresetList.getKeyListeners()) {
        if (kl.toString().contains("BasicListUI")) {peskyListener=kl;break;} // This is the pesky listener
     }
     if (peskyListener!=null) featuresetList.removeKeyListener(peskyListener);       
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
  
private void mousePressedInFeaturesPanel(java.awt.event.MouseEvent evt) {                                             
    int x=evt.getX(); 
    int y=evt.getY();
    JList list=(JList)evt.getComponent();    
    int index=getElementIndexAtPointInList(list,x,y);
    if (index==-1) {list.clearSelection();return;}     
    if (evt.isPopupTrigger()) {
        if (!list.isSelectedIndex(index)) list.setSelectedIndex(index);        
        JPopupMenu popup=FeaturesPanelContextMenu.getInstance(list,index,gui);
        if (dataPanelListeners!=null && popup!=null) {
            Data[] selected=getSelectedDataElements();
            for (PanelListener datapanelListener:dataPanelListeners) {
                datapanelListener.showContextMenu((FeaturesPanel)this, selected, popup);
            }
        }         
        if (popup!=null) popup.show(evt.getComponent(), x,y);
    }  
}                                            

private void mouseReleasedInFeaturesPanel(java.awt.event.MouseEvent evt) {                                              
    int x=evt.getX(); 
    int y=evt.getY();
    JList list=(JList)evt.getComponent();
    int index=getElementIndexAtPointInList(list,x,y);
    if (index==-1) {list.clearSelection();return;} 
    if (evt.isPopupTrigger()) {
        if (!list.isSelectedIndex(index)) list.setSelectedIndex(index);        
        JPopupMenu popup=FeaturesPanelContextMenu.getInstance((JList)evt.getComponent(),index,gui);
        if (dataPanelListeners!=null && popup!=null) {
            Data[] selected=getSelectedDataElements();
            for (PanelListener datapanelListener:dataPanelListeners) {
                datapanelListener.showContextMenu((FeaturesPanel)this, selected, popup);
            }
        }          
        if (popup!=null) popup.show(evt.getComponent(), x,y);
    }
}

private void mouseWheelMovedInFeaturesPanel(MouseWheelEvent evt) {
    int modifiers=evt.getModifiersEx();
    if ((modifiers & (MouseWheelEvent.SHIFT_DOWN_MASK | MouseWheelEvent.CTRL_DOWN_MASK))==MouseWheelEvent.SHIFT_DOWN_MASK) { // SHIFT modifier -> adjust row spacing for regions in expanded mode
       int wheelupdate=evt.getWheelRotation();
       int[] selected=featuresetList.getSelectedIndices();
       if (selected.length==0) return;
       FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
       for (int i=0;i<selected.length;i++) {
          String datasetname=((FeatureDataset)model.getElementAt(selected[i])).getName();
          if (model.getElementAt(selected[i]) instanceof RegionDataset && visualizationSettings.isExpanded(datasetname)) { // track is showing expanded regions. Change row spacing instead of track height
              int currentRowSpacing=visualizationSettings.getRowSpacing(datasetname);
              int newRowSpacing=currentRowSpacing+wheelupdate;
              if (newRowSpacing>VisualizationSettings.EXPANDED_REGION_MAX_ROW_SPACING) newRowSpacing=VisualizationSettings.EXPANDED_REGION_MAX_ROW_SPACING;
              else if (newRowSpacing<0) newRowSpacing=0; // It could be possible to have negative spacing and thus vertically overlapping regions, but that might be a bit hassle.
              visualizationSettings.setRowSpacing(datasetname, newRowSpacing,(i==selected.length-1));              
          } 
       }
    } else if ((modifiers & (MouseWheelEvent.SHIFT_DOWN_MASK | MouseWheelEvent.CTRL_DOWN_MASK))==MouseWheelEvent.CTRL_DOWN_MASK) { // CTRL modifier -> adjust sequence margin
        int margin=visualizationSettings.getSequenceMargin();
        int newmargin=margin+evt.getWheelRotation();
        visualizationSettings.setSequenceMargin(newmargin);       
    }    
    else { // no keyboard modifiers -> adjust track height (or row height in expanded mode)       
       int wheelupdate=evt.getWheelRotation();
       int[] selected=featuresetList.getSelectedIndices();
       if (selected.length==0) return;
       FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
       for (int i=0;i<selected.length;i++) {
          String datasetname=((FeatureDataset)model.getElementAt(selected[i])).getName();
          if (model.getElementAt(selected[i]) instanceof RegionDataset && visualizationSettings.isExpanded(datasetname)) { // track is showing expanded regions. Change region height instead of track height
              int currentRegionHeight=visualizationSettings.getExpandedRegionHeight(datasetname);
              int newheight=currentRegionHeight+wheelupdate;
              if (newheight>VisualizationSettings.EXPANDED_REGION_MAX_HEIGHT) newheight=VisualizationSettings.EXPANDED_REGION_MAX_HEIGHT;
              else if (newheight<1) newheight=1;
              visualizationSettings.setExpandedRegionHeight(datasetname, newheight,(i==selected.length-1));              
          } else {        
              int currentTrackHeight=visualizationSettings.getDataTrackHeight(datasetname);
              int newheight=currentTrackHeight+wheelupdate;
              if (newheight>VisualizationSettings.DATATRACK_MAX_HEIGHT) newheight=VisualizationSettings.DATATRACK_MAX_HEIGHT;
              else if (newheight<VisualizationSettings.DATATRACK_MIN_HEIGHT) newheight=VisualizationSettings.DATATRACK_MIN_HEIGHT;
              visualizationSettings.setDataTrackHeight(datasetname, newheight,(i==selected.length-1));
          }
       }       
    }
  }

  /**
   * Changes the color of the currently selected tracks
   * @param dir direction of cycling (positive or negative)
   */
  private void cycleColor(int dir) {
      int[] selected=featuresetList.getSelectedIndices();
      if (selected.length==0) return;
      for (int index:selected) {
          FeatureDataset dataset=(FeatureDataset)featuresetList.getModel().getElementAt(index);  // only apply to first selected
          String datasetname=dataset.getName();
          Color fg=visualizationSettings.getForeGroundColor(datasetname);
          Color bg=visualizationSettings.getBackGroundColor(datasetname);
          Color[] newColors;
          if (dir>=0) newColors=visualizationSettings.getNextPresetColorPair(fg, bg);
          else newColors=visualizationSettings.getPreviousPresetColorPair(fg, bg);
          visualizationSettings.setForeGroundColor(datasetname, newColors[0]);
          visualizationSettings.setBackGroundColor(datasetname, newColors[1]);
      }
  }
  
  /**
   * Changes the color alpha value (transparency) of the currently selected tracks
   */
  private void cycleAlpha() {
      int[] selected=featuresetList.getSelectedIndices();
      if (selected.length==0) return;
      FeatureDataset dataset=(FeatureDataset)featuresetList.getModel().getElementAt(selected[0]);
      String datasetname=dataset.getName();      
      int alpha = visualizationSettings.getForeGroundAlpha(datasetname);
           if (alpha>=255) alpha=25;
      else if (alpha>=192) alpha=255;
      else if (alpha>=128) alpha=192;
      else if (alpha>=64) alpha=128;
      else if (alpha>=25) alpha=64;      
      else alpha=100;
      String[] datasetnames = new String[selected.length];
      int index=0;
      for (int i:selected) {
          dataset=(FeatureDataset)featuresetList.getModel().getElementAt(i);
          datasetname=dataset.getName();
          datasetnames[index]=datasetname;
          index++;          
      }
      visualizationSettings.setForeGroundAlpha(datasetnames, alpha);
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
  * This method is called when the "add feature dataset" button (next to the panel label) is pressed
  */
 private void addFeatureDataset() {
     gui.showDatatrackDialog();
 }
 
 /**
  * This method toggles the visibility of a selected feature datatrack
  * (The method is called when the user presses the spacebar while a feature dataset is selected and has focus)
  */
 private void toggleVisibility(FeatureDataset dataset) {
     String datasetname=dataset.getName();
     boolean visible=visualizationSettings.isTrackVisible(datasetname);
     visualizationSettings.setTrackVisible(datasetname, !visible);
 }
 
 protected void setVisibilityOnAll(boolean show) {
     FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();     
     int size=model.size();
     String[] names=new String[size];
     for (int i=0;i<size;i++) {
        names[i]=((FeatureDataset)model.getElementAt(i)).getName();
     }
     visualizationSettings.setTrackVisible(names, show);
 }
 
 protected void showOnlyTheseTracks(String[] names) {
    setVisibilityOnAll(false);
    visualizationSettings.setTrackVisible(names, true);     
 }
 
 private String[] getNamesOfSequencesWithVisibleRegions(RegionDataset dataset) {
    ArrayList<String> sequencesWithRegions=new ArrayList<String>(dataset.getNumberofSequences());
    RegionVisualizationFilter regionfilter=gui.getRegionVisualizationFilter();
    for (FeatureSequenceData seq:dataset.getAllSequences()) {
        for (Region region:((RegionSequenceData)seq).getOriginalRegions()) {
            if (visualizationSettings.isRegionTypeVisible(region.getType()) && (regionfilter==null || regionfilter.shouldVisualizeRegion(region))) {
                sequencesWithRegions.add(seq.getSequenceName());
                break; // we only require a single region to be visible in order to show the sequence
            }
        }
    }
    String[] array=new String[sequencesWithRegions.size()];
    return sequencesWithRegions.toArray(array);   
 }
 
 // ----- Searchable interface -----
    String previoussearchString="";
    @Override
    public boolean find(String searchstring) {
         FeaturesPanelListModel model=(FeaturesPanelListModel)featuresetList.getModel();
         if (model.getSize()==0) return false;
         int selected=(previoussearchString.equalsIgnoreCase(searchstring))?featuresetList.getSelectedIndex():-1;
         previoussearchString=searchstring;
         int found=-1;
         int start=(selected<0)?0:selected+1;
         for (int i=start;i<model.getSize();i++) {
              String name=null;
              Object el=model.getElementAt(i);
              if (el instanceof Data) name=((Data)el).getName();
              else if (el instanceof String) name=(String)el;
              if (name!=null && name.matches("(?i).*"+searchstring+".*")) {
                  gui.statusMessage("[Features] Searching for '"+searchstring+"'. Found '"+name+"'.");                  
                  found=i;
                  break;
              }
         }
         if (found>=0) {
             //gui.statusMessage(""); // just to clear the status
             featuresetList.setSelectedIndex(found);
             featuresetList.ensureIndexIsVisible(found);
             return true;
         } else {
             gui.statusMessage("[Features] Searching for '" + searchstring + "'. No more matches found.");
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
             groupByType=new JCheckBoxMenuItem("Group By Type",((FeaturesPanelListModel)featuresetList.getModel()).shouldGroupByType());
             sortAlphabetically=new JCheckBoxMenuItem("Sort Alphabetically",((FeaturesPanelListModel)featuresetList.getModel()).shouldSortAlphabetically());
             groupByType.addActionListener(this);
             sortAlphabetically.addActionListener(this);
             add(groupByType);
             add(sortAlphabetically);

         }

         @Override
         public void actionPerformed(ActionEvent e) {
            String cmd=e.getActionCommand();
            FeaturesPanelListModel listmodel=((FeaturesPanelListModel)featuresetList.getModel());
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