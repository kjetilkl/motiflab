/*
 
 
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.FeatureDataset;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.DefaultListModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JSeparator;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.operations.Operation_analyze;



/**
 *
 * @author kjetikl
 */
public class FeaturesPanelContextMenu extends JPopupMenu  {
    private static String SHOW_DATATRACK="Show Track"; 
    private static String SHOW_ONLY_DATATRACK="Show Only"; 
    private static String HIDE_DATATRACK="Hide Track";
    private static String EXPAND_DATATRACK="Expand Track";
    private static String CONTRACT_DATATRACK="Contract Track";
    private static String GROUP_DATATRACK="Group with Track Above";
    private static String UNGROUP_DATATRACK="Ungroup Track";    
    private static String SHOW_TRACK_IN_REGION_BROWSER="Open in Region Browser";         
    private static String COLOR_BY_TYPE="Color By Type";
    private static String SINGLE_COLOR="Use Single Color";
    private static String RENAME_DATA ="Rename";
    private static String DELETE_DATA ="Delete";
    private static String CONVERT_TO_MOTIF_TRACK="Convert to Motif Track";
    private static String CONVERT_TO_MODULE_TRACK="Convert to Module Track";
    private static String CONVERT_TO_NESTED_TRACK="Treat as Nested Track";
    private static String DISPLAY_SETTINGS_DIALOG="Change Display Settings";
    private static String SET_COLOR="Set Track Color";
    private static String SET_SECONDARY_COLOR="Set Secondary Color";    
    private static String SET_BACKGROUND_COLOR="Set Background Color"; 
    private static String SET_BASELINE_COLOR="Set Baseline Color";    
    private static String SET_OTHER_COLORS="Other Colors";
    private static String SET_RANGE ="Set Range";
    private static String OPERATION_SUBMENU_HEADER="Perform Operation";

    
    
    private MotifLabEngine engine;
    private FeatureDataset dataset;
    private FeatureDataset[] datasets;
    private MotifLabGUI gui;
    private JMenu operationsMenu;
    private JList list=null;
    
    public static FeaturesPanelContextMenu getInstance(JList list, int index, MotifLabGUI gui) {
         if (index==-1) return null;
         DefaultListModel model=(DefaultListModel)list.getModel();
         Object[] values=list.getSelectedValues();
         if (values==null || values.length<=1) return new FeaturesPanelContextMenu(list,(FeatureDataset)model.getElementAt(index),gui);
         else {
             FeatureDataset[] newsets=new FeatureDataset[values.length];
             for (int i=0;i<newsets.length;i++) newsets[i]=(FeatureDataset)values[i];
             return new FeaturesPanelContextMenu(list,newsets,gui);
         }        
    }

    /** Creates a context menu for a single selected dataset */
    private FeaturesPanelContextMenu(final JList list, final FeatureDataset dataset, final MotifLabGUI gui) {
            this.engine=gui.getEngine();
            this.dataset=dataset;
            this.datasets=null;
            this.gui=gui;
            this.list=list;
            String datasetName=dataset.getName();
            VisualizationSettings settings=gui.getVisualizationSettings();
            DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
            ArrayList<Operation> operationslist=engine.getAllOperations();
            Collections.sort(operationslist, new NameComparator());
            OperationsMenuListener menulistener=new OperationsMenuListener();
            operationsMenu=gui.getOperationContextMenu(OPERATION_SUBMENU_HEADER, dataset, menulistener);            
            operationsMenu.setEnabled(operationsMenu.getItemCount()>0);
            this.add(operationsMenu);
            this.add(new JSeparator());

            JMenuItem displayoptions=new JMenuItem(DISPLAY_SETTINGS_DIALOG);
            JMenuItem renameDataItem=new JMenuItem(RENAME_DATA+" \""+datasetName+"\"");
            JMenuItem deleteDataItem=new JMenuItem(DELETE_DATA+" \""+datasetName+"\"");

            String hideorshowString=(settings.isTrackVisible(datasetName))?HIDE_DATATRACK:SHOW_DATATRACK;
            JMenuItem showorhide=new JMenuItem(hideorshowString);
            JMenuItem showonly=new JMenuItem(SHOW_ONLY_DATATRACK+" This Track");
            displayoptions.addActionListener(menuItemListener);
            showorhide.addActionListener(menuItemListener);
            showonly.addActionListener(menuItemListener);            
            renameDataItem.addActionListener(menuItemListener);
            deleteDataItem.addActionListener(menuItemListener);
            if (!gui.getScheduler().isIdle()) {
                deleteDataItem.setEnabled(false);
                renameDataItem.setEnabled(false);
            }
          
            this.add(showorhide);
            this.add(showonly);    
            
            String groupString=(settings.isGroupedTrack(datasetName))?UNGROUP_DATATRACK:GROUP_DATATRACK;
            JMenuItem groupDataItem=new JMenuItem(groupString);
            groupDataItem.addActionListener(menuItemListener);
            this.add(groupDataItem);
                
            if (dataset instanceof RegionDataset) {
                String expandString=(settings.isExpanded(datasetName))?CONTRACT_DATATRACK:EXPAND_DATATRACK;
                JMenuItem expandDataItem=new JMenuItem(expandString);
                expandDataItem.addActionListener(menuItemListener);
                this.add(expandDataItem);
                
                JMenuItem regionBrowserItem=new JMenuItem(SHOW_TRACK_IN_REGION_BROWSER);
                regionBrowserItem.addActionListener(menuItemListener);
                this.add(regionBrowserItem);                
                
                boolean colorByType=settings.useMultiColoredRegions(datasetName);
                String colorByTypeString=(colorByType)?SINGLE_COLOR:COLOR_BY_TYPE;
                JMenuItem colorByTypeDataItem=new JMenuItem(colorByTypeString);
                colorByTypeDataItem.addActionListener(menuItemListener);
                this.add(colorByTypeDataItem);
            } 

            /* This is the new Graph type menu based on names (Strings) rather than numbers */
            ArrayList<String> graphTypes=gui.getRegisteredDataTrackVisualizers(dataset.getClass());
            if (graphTypes!=null && graphTypes.size()>1) {
               JMenu graphmenu=new JMenu("Graph type");
               String currentgraph=settings.getGraphType(datasetName);
               for (String type:graphTypes) {
                   JCheckBoxMenuItem graphMenuItem=new JCheckBoxMenuItem(type,type.equals(currentgraph));
                   graphMenuItem.setActionCommand("graph:"+type); // since graph names can be anything, we add a prefix to avoid confusion
                   graphMenuItem.addActionListener(menuItemListener);
                   graphmenu.add(graphMenuItem);
               }
               this.add(graphmenu);
            }            
            
            if (dataset instanceof NumericDataset || dataset instanceof RegionDataset || dataset instanceof DNASequenceDataset) {
                ColorMenuListener colormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        else gui.getVisualizationSettings().setForeGroundColor(dataset.getName(), color);
                     }
                };
                ColorMenu colorMenu=new ColorMenu(SET_COLOR, settings.getForeGroundColor(datasetName), colormenulistener,gui.getFrame());                
                this.add(colorMenu);
            }            

            if (dataset instanceof NumericDataset || dataset instanceof RegionDataset || dataset instanceof DNASequenceDataset) {
                JMenu otherColorsMenu=new JMenu(SET_OTHER_COLORS);

                ColorMenuListener bgcolormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        else gui.getVisualizationSettings().setBackGroundColor(dataset.getName(), color);
                     }
                };
                ColorMenu bgColorMenu=new ColorMenu(SET_BACKGROUND_COLOR,settings.getBackGroundColor(datasetName),bgcolormenulistener,gui.getFrame());                
                otherColorsMenu.add(bgColorMenu);
           
                ColorMenuListener basecolormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        else gui.getVisualizationSettings().setBaselineColor(dataset.getName(), color);
                     }
                };
                ColorMenu basecolorMenu=new ColorMenu(SET_BASELINE_COLOR,settings.getBaselineColor(datasetName),basecolormenulistener,gui.getFrame());                
                otherColorsMenu.add(basecolorMenu);
                
                ColorMenuListener secondarycolormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        else gui.getVisualizationSettings().setSecondaryColor(dataset.getName(), color);
                     }
                };
                ColorMenu secondaryColorMenu=new ColorMenu(SET_SECONDARY_COLOR,settings.getSecondaryColor(datasetName),secondarycolormenulistener,gui.getFrame());                
                otherColorsMenu.add(secondaryColorMenu);
                
                this.add(otherColorsMenu);
            }  
            
            // Add menus for graph type specific settings
            String graphType=gui.getVisualizationSettings().getGraphType(datasetName);
            Object dtv=gui.getEngine().getResource(graphType, "DTV");
            if (dtv instanceof DataTrackVisualizer) {
               ArrayList<DataTrackVisualizerSetting> graphSettings=((DataTrackVisualizer)dtv).getGraphTypeSettings();
               if (graphSettings!=null) {
                   ArrayList<DataTrackVisualizerSetting> minorSettings=new ArrayList<>();
                   for (DataTrackVisualizerSetting graphSetting:graphSettings) {
                       if (!graphSetting.appliesToTrack(dataset)) continue;
                       if (graphSetting.isMinorSetting()) {minorSettings.add(graphSetting);continue;}
                       if (graphSetting.isSeparator()) {
                            this.add(new JSeparator());
                       } else {
                            Object currentValue=settings.getSetting(datasetName+"."+graphSetting.getVisualizationSettingName());
                            if (currentValue==null) currentValue=graphSetting.getDefaultOptionValue();
                            JMenuItem settingMenuItem = createMenuEntryForSetting(graphType, graphSetting, currentValue, menuItemListener);
                            this.add(settingMenuItem);     
                       }
                   }
                   if (!minorSettings.isEmpty()) {
                       JMenuItem minorSettingsMenu = new JMenu("Graph Settings");
                       for (DataTrackVisualizerSetting graphSetting:minorSettings) {
                            if (graphSetting.isSeparator()) {
                                minorSettingsMenu.add(new JSeparator());
                            } else {                           
                                Object currentValue=settings.getSetting(datasetName+"."+graphSetting.getVisualizationSettingName());
                                if (currentValue==null) currentValue=graphSetting.getDefaultOptionValue();
                                JMenuItem settingMenuItem = createMenuEntryForSetting(graphType, graphSetting, currentValue, menuItemListener);
                                minorSettingsMenu.add(settingMenuItem);         
                            }
                       }  
                       this.add(minorSettingsMenu); 
                   }
               }
            }
                        
            this.add(displayoptions);
            
            if (dataset instanceof NumericDataset) {           
                JMenuItem setRangeItem=new JMenuItem(SET_RANGE); 
                setRangeItem.addActionListener(menuItemListener);
                this.add(setRangeItem);
            }
    
            if (dataset instanceof RegionDataset && !((RegionDataset)dataset).isMotifTrack() && !((RegionDataset)dataset).isModuleTrack()) {
                this.add(new JSeparator());
                JMenuItem convertToMotifTrackItem=new JMenuItem(CONVERT_TO_MOTIF_TRACK);
                convertToMotifTrackItem.addActionListener(menuItemListener);
                this.add(convertToMotifTrackItem);
                JMenuItem convertToModuleTrackItem=new JMenuItem(CONVERT_TO_MODULE_TRACK);
                convertToModuleTrackItem.addActionListener(menuItemListener);
                this.add(convertToModuleTrackItem);
                
//                JMenuItem convertToNestedTrackItem=new JMenuItem(CONVERT_TO_NESTED_TRACK);
//                convertToNestedTrackItem.addActionListener(menuItemListener);
//                this.add(convertToNestedTrackItem);               
            }
            this.add(new JSeparator()); 
            JMenu sortmenu=new JMenu("Sort sequences by");
            this.add(sortmenu);
            ActionListener sortMenuListener=new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SortSequencesDialog.sortBy(e.getActionCommand(), false, dataset.getName(), null, gui);
                }
            };          
            if (dataset instanceof RegionDataset) {     
                JMenuItem regcount=new JMenuItem(SortSequencesDialog.SORT_BY_REGION_COUNT);
                JMenuItem regcoverage=new JMenuItem(SortSequencesDialog.SORT_BY_REGION_COVERAGE);
                JMenuItem regscores=new JMenuItem(SortSequencesDialog.SORT_BY_REGION_SCORES_SUM);
                JMenuItem visregcount=new JMenuItem(SortSequencesDialog.SORT_BY_VISIBLE_REGION_COUNT);
                JMenuItem visregcoverage=new JMenuItem(SortSequencesDialog.SORT_BY_VISIBLE_REGION_COVERAGE);
                JMenuItem visregsum=new JMenuItem(SortSequencesDialog.SORT_BY_VISIBLE_REGION_SCORES_SUM);
                regcount.addActionListener(sortMenuListener);
                regcoverage.addActionListener(sortMenuListener);
                regscores.addActionListener(sortMenuListener);
                visregcount.addActionListener(sortMenuListener);
                visregcoverage.addActionListener(sortMenuListener);
                visregsum.addActionListener(sortMenuListener);
                sortmenu.add(regcount);
                sortmenu.add(regcoverage);
                sortmenu.add(regscores);
                sortmenu.add(visregcount);
                sortmenu.add(visregcoverage);
                sortmenu.add(visregsum);
            } else if (dataset instanceof NumericDataset) {
                JMenuItem tracksum=new JMenuItem(SortSequencesDialog.SORT_BY_NUMERIC_TRACK_SUM);     
                tracksum.addActionListener(sortMenuListener);        
                sortmenu.add(tracksum);                 
            } else if (dataset instanceof DNASequenceDataset) {
                JMenuItem gccontent=new JMenuItem(SortSequencesDialog.SORT_BY_GC_CONTENT);     
                gccontent.addActionListener(sortMenuListener);        
                sortmenu.add(gccontent);                
            }   
            
            this.add(new JSeparator());            
            this.add(renameDataItem);
            this.add(deleteDataItem);            
        }
        

    /** Creates a context-menu for multiple selected datasets */
    private FeaturesPanelContextMenu(final JList list, final FeatureDataset[] datasets, final MotifLabGUI gui) {
            this.engine=gui.getEngine();
            this.datasets=datasets;
            this.dataset=null;
            this.gui=gui;
            this.list=list;
            int countRegionDatasets=0;
            int countNumericDatasets=0;
            int countDNASequenceDatasets=0;
            int countGrouped=0;
            for (FeatureDataset data:datasets) {
                     if (data instanceof RegionDataset) countRegionDatasets++;
                else if (data instanceof NumericDataset) countNumericDatasets++;
                else if (data instanceof DNASequenceDataset) countDNASequenceDatasets++;
                if (gui.getVisualizationSettings().isGroupedTrack(data.getName())) countGrouped++;
            }
            DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
            ArrayList<Operation> operationslist=engine.getAllOperations();
            Collections.sort(operationslist, new NameComparator());

            JMenuItem deleteDataItems=new JMenuItem(DELETE_DATA);
            JMenuItem showDataitems=new JMenuItem(SHOW_DATATRACK+"s");
            JMenuItem showOnlyDataitems=new JMenuItem(SHOW_ONLY_DATATRACK+" These Tracks");            
            JMenuItem hideDataitems=new JMenuItem(HIDE_DATATRACK+"s");

            showDataitems.addActionListener(menuItemListener);
            showOnlyDataitems.addActionListener(menuItemListener);            
            hideDataitems.addActionListener(menuItemListener);
            deleteDataItems.addActionListener(menuItemListener);
            if (!gui.getScheduler().isIdle()) {
                deleteDataItems.setEnabled(false);
            }

            OperationsMenuListener menulistener=new OperationsMenuListener();
            operationsMenu=gui.getOperationContextMenu(OPERATION_SUBMENU_HEADER, datasets, menulistener);
            operationsMenu.setEnabled(operationsMenu.getItemCount()>0);  
            
            this.add(operationsMenu);                   
            this.add(new JSeparator());   
            this.add(showDataitems);
            this.add(showOnlyDataitems);            
            this.add(hideDataitems);
            
            JMenuItem groupDataitems=new JMenuItem(GROUP_DATATRACK);
            JMenuItem ungroupDataitems=new JMenuItem(UNGROUP_DATATRACK+"s");
            groupDataitems.addActionListener(menuItemListener);
            ungroupDataitems.addActionListener(menuItemListener);
            if (countGrouped<datasets.length) this.add(groupDataitems);
            if (countGrouped>0) this.add(ungroupDataitems);            
        
            if (countRegionDatasets==datasets.length && this.list!=null) { // only RegionDatasets selected
                JMenuItem expandDataitems=new JMenuItem(EXPAND_DATATRACK+"s");
                JMenuItem contractDataitems=new JMenuItem(CONTRACT_DATATRACK+"s");
                expandDataitems.addActionListener(menuItemListener);
                contractDataitems.addActionListener(menuItemListener);
                this.add(expandDataitems);
                this.add(contractDataitems);
                
                JMenuItem colorByType=new JMenuItem(COLOR_BY_TYPE);
                colorByType.addActionListener(menuItemListener);
                JMenuItem singleColor=new JMenuItem(SINGLE_COLOR);                
                singleColor.addActionListener(menuItemListener);
                this.add(singleColor); 
                this.add(colorByType);                 
            }
            
            Class commonClass=null;
            if (countNumericDatasets==datasets.length && this.list!=null) commonClass=NumericDataset.class;
            if (countRegionDatasets==datasets.length && this.list!=null) commonClass=RegionDataset.class;
            if (countDNASequenceDatasets==datasets.length && this.list!=null) commonClass=DNASequenceDataset.class;
                       
            if (commonClass!=null) { // all selected datasets are of the same type
                ArrayList<String> graphTypes=gui.getRegisteredDataTrackVisualizers(commonClass);               
                if (graphTypes!=null && graphTypes.size()>1) {
                   JMenu graphmenu=new JMenu("Graph type");
                   String currentgraph=getCommonGraphTypeSettings(datasets); // this can return an empty string (which is OK)
                   for (String type:graphTypes) {
                       JCheckBoxMenuItem graphMenuItem=new JCheckBoxMenuItem(type,type.equals(currentgraph));
                       graphMenuItem.addActionListener(menuItemListener);
                       graphMenuItem.setActionCommand("graph:"+type); // since graph names can be anything, we add a prefix to avoid confusion                       
                       graphmenu.add(graphMenuItem);
                   }
                   this.add(graphmenu);
                }                                
            }
            
            if (countNumericDatasets+countRegionDatasets+countDNASequenceDatasets==datasets.length) {  // only NumericDatasets and RegionDatasets selected (and DNA sets)
                ColorMenuListener colormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setForeGroundColor(names, color);
                     }
                };
                ColorMenu colorMenu=new ColorMenu(SET_COLOR,colormenulistener,gui.getFrame());                
                this.add(colorMenu);
            } 
            
            if (countNumericDatasets+countRegionDatasets+countDNASequenceDatasets==datasets.length) {  // only NumericDatasets and RegionDatasets selected (and DNA sets)
                JMenu otherColors=new JMenu(SET_OTHER_COLORS);              
               
                ColorMenuListener bgcolormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setBackGroundColor(names, color);
                     }
                };
                ColorMenu bgcolorMenu=new ColorMenu(SET_BACKGROUND_COLOR,bgcolormenulistener,gui.getFrame());                
                otherColors.add(bgcolorMenu);

                ColorMenuListener basecolormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setBaselineColor(names, color);
                     }
                };
                ColorMenu basecolorMenu=new ColorMenu(SET_BASELINE_COLOR,basecolormenulistener,gui.getFrame());                
                otherColors.add(basecolorMenu);

                ColorMenuListener secondarycolormenulistener=new ColorMenuListener() {
                      @Override
                      public void newColorSelected(Color color) {
                        if (color==null) return;
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setSecondaryColor(names, color);
                     }
                };
                ColorMenu secondaryColorMenu=new ColorMenu(SET_SECONDARY_COLOR,secondarycolormenulistener,gui.getFrame());                
                otherColors.add(secondaryColorMenu);                  
                
                this.add(otherColors);
            } 
            
            // Add menus for graph type specific settings
            String graphType=getCommonGraphTypeSettings(datasets);
            Object dtv=(graphType!=null)?gui.getEngine().getResource(graphType, "DTV"):null;
            if (dtv instanceof DataTrackVisualizer) {
               ArrayList<DataTrackVisualizerSetting> graphSettings=((DataTrackVisualizer)dtv).getGraphTypeSettings();
               if (graphSettings!=null) {
                   ArrayList<DataTrackVisualizerSetting> minorSettings=new ArrayList<>();
                   for (DataTrackVisualizerSetting graphSetting:graphSettings) {
                       if (!graphSetting.appliesToTracks(datasets)) continue;
                       if (graphSetting.isMinorSetting()) {minorSettings.add(graphSetting);continue;}                       
                       Object currentValue=getCommonSettingValue(datasets,graphSetting.getVisualizationSettingName());
                       JMenuItem settingMenuItem = createMenuEntryForSetting(graphType, graphSetting, currentValue, menuItemListener);
                       this.add(settingMenuItem);                       
                   }
                   if (!minorSettings.isEmpty()) {
                       JMenuItem minorSettingsMenu = new JMenu("Graph Settings");
                       for (DataTrackVisualizerSetting graphSetting:minorSettings) {
                            Object currentValue=getCommonSettingValue(datasets,graphSetting.getVisualizationSettingName());
                            JMenuItem settingMenuItem = createMenuEntryForSetting(graphType, graphSetting, currentValue, menuItemListener);
                            minorSettingsMenu.add(settingMenuItem);                       
                       }  
                       this.add(minorSettingsMenu); 
                   }
               }
            }            
            
//            if (countRegionDatasets==datasets.length && this.list!=null) { // only RegionDatasets selected            
//
//            }            
            
            if (countNumericDatasets==datasets.length) { // only NumericDatasets selected
               JMenuItem setRangeItem=new JMenuItem(SET_RANGE); 
               setRangeItem.addActionListener(menuItemListener);
               this.add(setRangeItem);
            }       
                
            this.add(new JSeparator());            
            this.add(deleteDataItems);                                
        }    
    
        private String getCommonGraphTypeSettings(FeatureDataset[] datasets) {
            String common=null;
            VisualizationSettings settings=gui.getVisualizationSettings();
            for (FeatureDataset data:datasets) {
                String graph=settings.getGraphType(data.getName());
                if (common==null) common=graph;
                else if (!graph.equals(common)) return ""; // not similar
            }
            return common;
        }                   
        
        private Object getCommonSettingValue(FeatureDataset[] datasets, String settingName) {
            Object commonValue=null;
            VisualizationSettings settings=gui.getVisualizationSettings();
            for (FeatureDataset data:datasets) {
                Object thisValue=settings.getSetting(data.getName()+"."+settingName);
                if (commonValue==null) commonValue=thisValue;
                else if (!commonValue.equals(thisValue)) return null; // not similar
            }
            return commonValue;
        }          

        
        private JMenuItem createMenuEntryForSetting(String graphType, DataTrackVisualizerSetting setting, Object currentValue, ActionListener menuItemListener) {
            JMenuItem menuitem;
            if (setting.useDialog()) {                                
                menuitem = new JMenuItem(setting.name()+"...");
                menuitem.setActionCommand(graphType+"::"+setting.name()); // this special syntax is recognized by the menuitemlistener.
                menuitem.addActionListener(menuItemListener);             
            } else if (setting.isFlagOption()) {
                menuitem = new JCheckBoxMenuItem(setting.name());
                boolean currentStatus=(currentValue instanceof Boolean)?((Boolean)currentValue):false;
                menuitem.setSelected(currentStatus);
                menuitem.setActionCommand(setting.getVisualizationSettingName()+"=>"+(!currentStatus)); // this special syntax is recognized by the menuitemlistener.
                menuitem.addActionListener(menuItemListener);                             
            } else {
                JMenu submenu = new JMenu(setting.name());
                for (Object[] option:setting.getOptions()) {
                    String optionName=(String)option[0];
                    Object optionValue=option[1];
                    JCheckBoxMenuItem optionMenuItem=new JCheckBoxMenuItem(optionName,optionValue.equals(currentValue));
                    optionMenuItem.setActionCommand(setting.getVisualizationSettingName()+"=>"+optionValue); // this special syntax is recognized by the menuitemlistener
                    optionMenuItem.addActionListener(menuItemListener);
                    submenu.add(optionMenuItem);
                }
                menuitem=submenu;               
            }
            if (setting.shortCutKey()>0) {
                
                if (menuitem instanceof JMenu) {
                    char firstChar=KeyEvent.getKeyText(setting.shortCutKey()).charAt(0);
                    menuitem.setMnemonic(firstChar);
                }
                else {
                    KeyStroke accelerator = KeyStroke.getKeyStroke(setting.shortCutKey(), 0);
                    menuitem.setAccelerator(accelerator);
                }
            }
            String docString=setting.getDocumentationString();
            if (docString!=null) menuitem.setToolTipText(docString);
            else menuitem.setToolTipText(setting.name());
            return menuitem;              
        }
        
        /**
         * An inner class that listens to popup-menu events related to the operations submenu and notifies the gui events
         */
        private class OperationsMenuListener implements ActionListener {
           public void actionPerformed(ActionEvent e) {
                Operation operation=engine.getOperation(e.getActionCommand());
                if (operation!=null) {
                    OperationTask parameters=new OperationTask(operation.getName());
                    parameters.setParameter(OperationTask.OPERATION, operation);
                    if (dataset!=null) {
                        if (operation.canUseAsSource(dataset)) {
                            parameters.setParameter(OperationTask.SOURCE, dataset);
                            parameters.setParameter(OperationTask.SOURCE_NAME, dataset.getName());
                        } else if (operation.canUseAsSourceProxy(dataset)) {
                            operation.assignToProxy(dataset, parameters);
                        }
                    }
                    else if (datasets!=null && datasets.length>0) {
                        if (operation.canUseAsSource(datasets)) {
                            String names=datasets[0].getName();
                            for (int i=1;i<datasets.length;i++) {
                                names+=","+datasets[i].getName();
                            }
                            parameters.setParameter(OperationTask.SOURCE_NAME, names);
                        } else if (operation.canUseAnyAsSourceProxy(datasets)) {
                            operation.assignToProxy(datasets, parameters);
                        }
                    }
                    gui.launchOperationEditor(parameters);
                } else if (engine.getAnalysis(e.getActionCommand())!=null) { // operation is an analysis
                    operation=engine.getOperation("analyze"); 
                    OperationTask parameters=new OperationTask(operation.getName());
                    String analysisName=e.getActionCommand();
                    Analysis analysis=engine.getAnalysis(analysisName);                    
                    parameters.setParameter(OperationTask.OPERATION, operation);
                    parameters.setParameter(Operation_analyze.ANALYSIS, analysisName);
                    ParameterSettings parametersettings=new ParameterSettings();
                    // infuse analysis' parametersettings with proxy sources
                    if (dataset!=null) { // single parameter
                        addProxySourceToAnalysis(dataset,analysis,parametersettings);
                    }
                    else if (datasets!=null && datasets.length>0) { // multiple parameters
                        for (int i=0;i<datasets.length;i++) {
                            FeatureDataset source=datasets[i];
                            addProxySourceToAnalysis(source,analysis,parametersettings);
                        }                      
                    }
                    parameters.setParameter(Operation_analyze.PARAMETERS, parametersettings);
                    gui.launchOperationEditor(parameters);                   
                }
           }            
        }
        
        @SuppressWarnings("unchecked")
        private void addProxySourceToAnalysis(Data source, Analysis analysis, ParameterSettings settings) {
            String[] proxys=analysis.getSourceProxyParameters();
            String proxyparameter=null;
            for (String proxy:proxys) {
                Parameter proxysource=analysis.getParameterWithName(proxy);
                Class proxytype=proxysource.getType();
                if (proxytype.isAssignableFrom(source.getClass()) && !settings.hasAssignedValueForParameter(proxy)) {proxyparameter=proxy;break;} // assign to first applicable parameter which is not already taken
            }
            if (proxyparameter!=null) settings.setParameter(proxyparameter, source.getName());
        }
         /**
         * An inner class that listens to popup-menu events NOT related to the operations submenu and notifies the gui events
         */       
        private class DisplayMenuItemListener implements ActionListener {
           public void actionPerformed(ActionEvent e) {            
               //gui.debugMessage("Selected "+e.getActionCommand()+" on "+datasetName);
                if (e.getActionCommand().startsWith(SHOW_DATATRACK)) {
                    if (dataset!=null) gui.getVisualizationSettings().setTrackVisible(dataset.getName(), true);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setTrackVisible(names, true);
                    }
                } else if (e.getActionCommand().startsWith(SHOW_ONLY_DATATRACK)) {
                    if (dataset!=null) {
                        String[] names=new String[]{dataset.getName()};
                        showOnlyTheseTracks(names);
                    } else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        showOnlyTheseTracks(names);
                    }
                } else if (e.getActionCommand().startsWith(HIDE_DATATRACK)) {
                    if (dataset!=null) gui.getVisualizationSettings().setTrackVisible(dataset.getName(), false);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setTrackVisible(names, false);
                    }
                } else if (e.getActionCommand().startsWith(EXPAND_DATATRACK)) {
                    if (dataset!=null) gui.getVisualizationSettings().setExpanded(dataset.getName(), true);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setExpanded(names, true);
                    }
                } else if (e.getActionCommand().startsWith(CONTRACT_DATATRACK)) {
                    if (dataset!=null) gui.getVisualizationSettings().setExpanded(dataset.getName(), false);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setExpanded(names, false);
                    }
                } else if (e.getActionCommand().startsWith(GROUP_DATATRACK)) {
                    if (dataset!=null) gui.getVisualizationSettings().setGroupedTrack(dataset.getName(), true);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setGroupedTracks(names, true);
                    }
                } else if (e.getActionCommand().startsWith(UNGROUP_DATATRACK)) {
                    if (dataset!=null) gui.getVisualizationSettings().setGroupedTrack(dataset.getName(), false);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setGroupedTracks(names, false);
                    }
                } else if (e.getActionCommand().startsWith(SHOW_TRACK_IN_REGION_BROWSER)) {
                    if (dataset instanceof RegionDataset) gui.showRegionBrowser((RegionDataset)dataset);
                } else if (e.getActionCommand().startsWith(COLOR_BY_TYPE) || e.getActionCommand().startsWith(SINGLE_COLOR)) {
                    boolean useMultiColor=(e.getActionCommand().startsWith(COLOR_BY_TYPE));
                    if (dataset!=null) gui.getVisualizationSettings().setUseMultiColoredRegions(dataset.getName(), useMultiColor);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();                        
                        gui.getVisualizationSettings().setUseMultiColoredRegions(names, useMultiColor); //gui.getVisualizationSettings().setGraphType(names, VisualizationSettings.REGION_MULTICOLOR_GRAPH);
                    }
                } else if (e.getActionCommand().startsWith(RENAME_DATA)) {
                    RenameDataDialog dialog=new RenameDataDialog(gui.getFrame(), gui, dataset.getName());
                    dialog.setLocation(100, 250);    
                    dialog.setVisible(true);
                    dialog.dispose();
                } else if (e.getActionCommand().startsWith(DELETE_DATA)) {
                    if (dataset!=null) gui.deleteDataItem(dataset.getName());
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.deleteDataItems(names);                
                    }                      
                } else if (e.getActionCommand().equals(CONVERT_TO_MOTIF_TRACK)) {
                   gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                   convertTracksToMotifTracks();
                   gui.getFrame().setCursor(Cursor.getDefaultCursor());
                } else if (e.getActionCommand().equals(CONVERT_TO_MODULE_TRACK)) {
                   gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                   convertTracksToModuleTracks();
                   gui.getFrame().setCursor(Cursor.getDefaultCursor());
                } else if (e.getActionCommand().equals(CONVERT_TO_NESTED_TRACK)) {
                   gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                   convertTracksToNestedTracks();
                   gui.getFrame().setCursor(Cursor.getDefaultCursor());
                } else if (e.getActionCommand().equals(SET_RANGE)) {
                    SetRangeDialog dialog;
                    if (dataset!=null) dialog=new SetRangeDialog(gui, (NumericDataset)dataset);
                    else {
                        NumericDataset[] data=new NumericDataset[datasets.length];
                        for (int i=0;i<datasets.length;i++) data[i]=(NumericDataset)datasets[i];
                        dialog=new SetRangeDialog(gui, data);
                    }
                    dialog.setLocation(100, 250);    
                    dialog.setVisible(true);
                    dialog.dispose();
                } else if (e.getActionCommand().equals(DISPLAY_SETTINGS_DIALOG)) {
                    DatasetDisplaySettingsDialog dialog=new DatasetDisplaySettingsDialog(gui.getFrame(), dataset, gui.getVisualizationSettings());
                    dialog.setLocation(100, 250);    
                    dialog.setVisible(true);
                    dialog.dispose();
                } else if (e.getActionCommand().startsWith("graph:")) {
                    String graphType=e.getActionCommand().substring("graph:".length());
                    if (dataset!=null) gui.getVisualizationSettings().setGraphType(dataset.getName(), graphType);
                    else if (datasets!=null) {
                        String[] names=new String[datasets.length];
                        for (int i=0;i<datasets.length;i++) names[i]=datasets[i].getName();
                        gui.getVisualizationSettings().setGraphType(names, graphType);
                    }
                } else if (e.getActionCommand().contains("::")) {
                    String[] parts=e.getActionCommand().split("::");
                    String graphType=parts[0];                       
                    String settingName=parts[1];                 
                    Object target=(dataset!=null)?dataset:datasets;                    
                    Object dtv=gui.getEngine().getResource(graphType,"DTV");
                    if (dtv instanceof DataTrackVisualizer) {
                        JDialog dialog=((DataTrackVisualizer)dtv).getGraphTypeSettingDialog(settingName, target, gui.getVisualizationSettings());
                        if (dialog!=null) {
                           dialog.setLocation(gui.getFrame().getWidth()/2-dialog.getWidth()/2, gui.getFrame().getHeight()/2-dialog.getHeight()/2);
                           dialog.setVisible(true);
                           dialog.dispose();
                        } else gui.errorMessage("ERROR: Missing dialog for Graph Type '"+graphType+"' setting '"+settingName +"'", 0);
                    }
                } else if (e.getActionCommand().contains("=>")) {
                    String[] parts=e.getActionCommand().split("=>");
                    String settingName=parts[0];
                    String settingValueAsString=parts[1];
                    Object settingValue=MotifLabEngine.getBasicValueForStringAsObject(settingValueAsString);
                    if (dataset!=null) gui.getVisualizationSettings().storeSetting(dataset.getName()+"."+settingName, settingValue);
                    else if (datasets!=null) {
                        for (int i=0;i<datasets.length;i++) {
                            gui.getVisualizationSettings().storeSetting(datasets[i].getName()+"."+settingName, settingValue);
                        }                       
                    }
                    gui.redraw();
                } 
           }            
        }
        
     private void setVisibilityOnAll(boolean show) {
         FeaturesPanelListModel model=(FeaturesPanelListModel)list.getModel();     
         int size=model.size();
         String[] names=new String[size];
         for (int i=0;i<size;i++) {
            names[i]=((FeatureDataset)model.getElementAt(i)).getName();
         }
         gui.getVisualizationSettings().setTrackVisible(names, show);
     }

     private void showOnlyTheseTracks(String[] names) {
        setVisibilityOnAll(false);
        gui.getVisualizationSettings().setTrackVisible(names, true);     
     }        

        
        /**
         * Just an inner class to sort available operations alphabetically in the popup menu
         */
        private class NameComparator implements Comparator<Operation> {        
            public int compare(Operation o1, Operation o2) {
                return o1.getName().compareTo(o2.getName());
            }
            public boolean equals(Operation o1, Operation o2) {
                return o1.getName().equals(o2.getName());
            }           
        }
                
                     
        private void convertTracksToMotifTracks() {
            HashMap<String,String> namemap=new HashMap<String,String>();
            ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
            for (Data motif:motifs) {
                namemap.put(((Motif)motif).getShortName(), motif.getName()); // note that there might be a 1-to-many mapping between shortname and motif ID but I ignore this here and just overwrite previous entries
            }
            DNASequenceDataset dna=null;
            int listsize=list.getModel().getSize();
            for (int i=0;i<listsize;i++) { // try to locate a DNASequenceDataset in the features panel list
                Object element=list.getModel().getElementAt(i);
                if (element instanceof DNASequenceDataset) {dna=(DNASequenceDataset)element;break;}
            }
            if (dataset!=null) convertSingleTrackToMotifTrack((RegionDataset)dataset,namemap,dna);
            else if (datasets!=null) {
                for (int i=0;i<datasets.length;i++) convertSingleTrackToMotifTrack((RegionDataset)datasets[i],namemap,dna);              
            }             
        }
        
        private void convertSingleTrackToMotifTrack(RegionDataset dataset, HashMap<String,String>namemap, DNASequenceDataset dna) {
              if (dataset.isMotifTrack()) return; // track is already a motif track
              boolean ok=engine.convertRegionTrackToMotifTrack(dataset, namemap, dna, true);
              if (!ok) {
                  JOptionPane.showMessageDialog(gui.getFrame(), "No known motifs found in "+dataset.getName()+".", "Conversion error", JOptionPane.ERROR_MESSAGE);
              } else {
                 String datasetName=dataset.getName();
                 gui.getVisualizationSettings().setUseMultiColoredRegions(datasetName, true);
                 gui.getVisualizationSettings().setVisualizeRegionStrand(datasetName,true);
                 gui.getVisualizationSettings().setVisualizeRegionScore(datasetName,true);   
                 gui.getVisualizationSettings().setDataTrackHeight(datasetName, VisualizationSettings.DEFAULT_TRACK_HEIGHT_MOTIF, true);
              }
        }

        private void convertTracksToModuleTracks() {
            DNASequenceDataset dna=null;
            int listsize=list.getModel().getSize();
            for (int i=0;i<listsize;i++) { // try to locate a DNASequenceDataset in the features panel list
                Object element=list.getModel().getElementAt(i);
                if (element instanceof DNASequenceDataset) {dna=(DNASequenceDataset)element;break;}
            }
            if (dataset!=null) convertSingleTrackToModuleTrack((RegionDataset)dataset,dna);
            else if (datasets!=null) {
                for (int i=0;i<datasets.length;i++) convertSingleTrackToModuleTrack((RegionDataset)datasets[i],dna);
            }
        }

        private void convertSingleTrackToModuleTrack(RegionDataset dataset, DNASequenceDataset dna) {
              if (dataset.isModuleTrack()) return; // track is already a module track
              boolean ok=engine.convertRegionTrackToModuleTrack(dataset, dna, true);
              if (!ok) {
                  JOptionPane.showMessageDialog(gui.getFrame(), "No known modules found in "+dataset.getName()+".", "Conversion error", JOptionPane.ERROR_MESSAGE);
              } else {
                 String datasetName=dataset.getName();
                 gui.getVisualizationSettings().setUseMultiColoredRegions(datasetName, true);
                 gui.getVisualizationSettings().setDataTrackHeight(datasetName, VisualizationSettings.DEFAULT_TRACK_HEIGHT_MODULE, true);
              }
        }
        
        private void convertTracksToNestedTracks() {
            if (dataset!=null) convertSingleTrackToNestedTrack((RegionDataset)dataset);
            else if (datasets!=null) {
                for (int i=0;i<datasets.length;i++) convertSingleTrackToNestedTrack((RegionDataset)datasets[i]);
            }
        }
        
        private void convertSingleTrackToNestedTrack(RegionDataset dataset) {
              if (dataset.isNestedTrack()) return; // track is already a nested track
              dataset.setNestedTrack(true);
//              String datasetName=dataset.getName();
//              gui.getVisualizationSettings().setUseMultiColoredRegions(datasetName, true);
//              gui.getVisualizationSettings().setDataTrackHeight(datasetName, VisualizationSettings.DEFAULT_TRACK_HEIGHT_MODULE, true);             
        }        
}
