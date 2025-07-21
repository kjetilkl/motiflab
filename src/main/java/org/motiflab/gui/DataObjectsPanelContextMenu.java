/*
 
 
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JList;
import java.awt.event.ActionListener;
import javax.swing.DefaultListModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import javax.swing.JMenu;
import javax.swing.JSeparator;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.PriorsGenerator;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.operations.Operation_analyze;



/**
 *
 * @author kjetikl
 */
public class DataObjectsPanelContextMenu extends JPopupMenu  {
    private static String EDIT_DATA="Edit";
    private static String DISPLAY_DATA="Display";
    private static String RENAME_DATA="Rename";
    private static String DELETE_DATA="Delete";

    private static String OPERATION_SUBMENU_HEADER="Perform Operation";
    private static String COLOR_SUBMENU_HEADER="Set Label Color";
    private static String COLOR_CLUSTER_SUBMENU_HEADER="Set Label Color From Clusters";
    private static String ORDER_BY_CLUSTER="Group Sequences By Clusters";
    private static String SHOW_SEQUENCES="Show Sequences";
    private static String SHOW_ONLY_SEQUENCES="Show Only Sequences";    
    private static String HIDE_SEQUENCES="Hide Sequences";
    private static String MARK_SEQUENCES="Mark Sequences";   
    private static String REMOVE_MARKS="Remove Marks";    
    
    private MotifLabEngine engine;
    private Data dataitem;
    private Data[] datalist;
    private MotifLabGUI gui;
    private JMenu operationsMenu;
    private DataObjectsPanel datapanel;
  
    public static DataObjectsPanelContextMenu getInstance(JList list, int index, MotifLabGUI gui, DataObjectsPanel datapanel) {
         if (index==-1) return null;
         DefaultListModel model=(DefaultListModel)list.getModel();
         Object[] values=list.getSelectedValues();
         if (values==null || values.length<=1) return new DataObjectsPanelContextMenu((Data)model.getElementAt(index),gui, datapanel);
         else {
             Data[] datalist=new Data[values.length];
             for (int i=0;i<datalist.length;i++) datalist[i]=(Data)values[i];
             return new DataObjectsPanelContextMenu(datalist,gui,datapanel);
         }                   
    }
    
    /** Returns a menu which can be used for a single data item */
    private DataObjectsPanelContextMenu(Data data, MotifLabGUI peGUI, DataObjectsPanel datapanel) {
        this.gui=peGUI;
        this.engine=gui.getEngine();
        this.dataitem=data;
        this.datalist=null;
        this.datapanel=datapanel;
        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        OperationsMenuListener menulistener=new OperationsMenuListener();
        operationsMenu=gui.getOperationContextMenu(OPERATION_SUBMENU_HEADER, dataitem, menulistener);
        operationsMenu.setEnabled(operationsMenu.getItemCount()>0); 
        this.add(operationsMenu);
        this.add(new JSeparator());
        JMenuItem editDataItem=new JMenuItem();
        if (data instanceof Analysis || data instanceof PriorsGenerator || dataitem==engine.getDefaultSequenceCollection()) editDataItem.setText(DISPLAY_DATA+" \""+dataitem.getName()+"\"");
        else editDataItem.setText(EDIT_DATA+" \""+dataitem.getName()+"\"");
        editDataItem.addActionListener(menuItemListener);
        this.add(editDataItem);
        if (dataitem instanceof SequenceCollection) {
            JMenuItem showitem=new JMenuItem(SHOW_SEQUENCES);
            showitem.addActionListener(menuItemListener);           
            this.add(showitem);  
            JMenuItem showonlyitem=new JMenuItem(SHOW_ONLY_SEQUENCES);
            showonlyitem.addActionListener(menuItemListener);           
            this.add(showonlyitem);                  
            JMenuItem hideitem=new JMenuItem(HIDE_SEQUENCES);
            hideitem.addActionListener(menuItemListener);              
            this.add(hideitem);
            JMenuItem markitem=new JMenuItem(MARK_SEQUENCES);
            markitem.addActionListener(menuItemListener);              
            this.add(markitem); 
            JMenuItem removemarksitem=new JMenuItem(REMOVE_MARKS);
            removemarksitem.addActionListener(menuItemListener);              
            this.add(removemarksitem);                
            ColorMenuListener colormenulistener=new ColorMenuListener() {
                public void newColorSelected(Color color) {
                    if (color!=null) setSequenceLabelColors(gui.getVisualizationSettings(), (SequenceCollection)dataitem, color, true);
                }
            };
            ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER, colormenulistener,gui.getFrame());
            this.add(colorMenu); 
            this.add(new JSeparator());
        }
        if (dataitem instanceof SequencePartition) {
            JMenuItem clusterColorItem=new JMenuItem(COLOR_CLUSTER_SUBMENU_HEADER);
            clusterColorItem.addActionListener(menuItemListener);    
            this.add(clusterColorItem);   
            JMenuItem orderByClustersItem=new JMenuItem(ORDER_BY_CLUSTER);
            orderByClustersItem.addActionListener(menuItemListener);    
            this.add(orderByClustersItem);  
            this.add(new JSeparator());
        } 
        JMenuItem renameDataItem=new JMenuItem(RENAME_DATA+" \""+dataitem.getName()+"\"");
        JMenuItem deleteDataItem=new JMenuItem(DELETE_DATA+" \""+dataitem.getName()+"\"");            
        renameDataItem.addActionListener(menuItemListener);
        deleteDataItem.addActionListener(menuItemListener);            
        this.add(renameDataItem);
        this.add(deleteDataItem);
        if (!gui.getScheduler().isIdle()) {
            //deleteDataItem.setEnabled(false); // on second thought I want to be able to do this           
            renameDataItem.setEnabled(false);
        }       
        if (dataitem==engine.getDefaultSequenceCollection()) {
            deleteDataItem.setEnabled(false);
            renameDataItem.setEnabled(false);
        }
     }
    
     /** Returns a menu which can be used for multiple selected data item */
     private DataObjectsPanelContextMenu(Data[] datalist, MotifLabGUI peGUI, DataObjectsPanel datapanel) {
        this.gui=peGUI;
        this.engine=gui.getEngine();
        this.dataitem=null;
        this.datalist=datalist;
        this.datapanel=datapanel;
        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        OperationsMenuListener menulistener=new OperationsMenuListener();
        operationsMenu=gui.getOperationContextMenu(OPERATION_SUBMENU_HEADER, datalist, menulistener);
        operationsMenu.setEnabled(operationsMenu.getItemCount()>0); 
        this.add(operationsMenu);
        this.add(new JSeparator());

        if (listContainsOnly(datalist,SequenceCollection.class)) {
            JMenuItem showitem=new JMenuItem(SHOW_SEQUENCES);
            showitem.addActionListener(menuItemListener);           
            this.add(showitem);  
            JMenuItem showonlyitem=new JMenuItem(SHOW_ONLY_SEQUENCES);
            showonlyitem.addActionListener(menuItemListener);           
            this.add(showonlyitem);                  
            JMenuItem hideitem=new JMenuItem(HIDE_SEQUENCES);
            hideitem.addActionListener(menuItemListener);              
            this.add(hideitem);
            JMenuItem markitem=new JMenuItem(MARK_SEQUENCES);
            markitem.addActionListener(menuItemListener);              
            this.add(markitem); 
            JMenuItem removemarksitem=new JMenuItem(REMOVE_MARKS);
            removemarksitem.addActionListener(menuItemListener);              
            this.add(removemarksitem);   
            ColorMenuListener colormenulistener=new ColorMenuListener() {
                public void newColorSelected(Color color) {
                    if (color!=null) setSequenceLabelColorsOnAll(gui.getVisualizationSettings(), DataObjectsPanelContextMenu.this.datalist, color);
                }
            };
            ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER, colormenulistener,gui.getFrame());
            this.add(colorMenu);
            this.add(new JSeparator());
        }  
        JMenuItem deleteDataItem=new JMenuItem(DELETE_DATA);            
        deleteDataItem.addActionListener(menuItemListener);   
        this.add(deleteDataItem);        
     }       

       /** Returns true if all of the data items in the list are of the given type (or subclass thereof) */
       private boolean listContainsOnly(Data[] items, Class type) {
           for (Data item:items) {
               if (!type.isAssignableFrom(item.getClass())) return false;
           }
           return true;
       }       
       
     
      /**
       * An inner class that listens to popup-menu events related to the operations submenu and notifies the gui events
       */
        @SuppressWarnings("unchecked")
        private class OperationsMenuListener implements ActionListener {
           public void actionPerformed(ActionEvent e) {
                Operation operation=engine.getOperation(e.getActionCommand());
                if (operation!=null) {
                    OperationTask parameters=new OperationTask(operation.getName());
                    parameters.setParameter(OperationTask.OPERATION, operation);
                    if (dataitem!=null) {
                        if (operation.canUseAsSource(dataitem)) {
                            parameters.setParameter(OperationTask.SOURCE, dataitem);
                            parameters.setParameter(OperationTask.SOURCE_NAME, dataitem.getName());
                        } else if (operation.canUseAsSourceProxy(dataitem)) {
                            operation.assignToProxy(dataitem, parameters);
                        }
                    }
                    else if (datalist!=null && datalist.length>0) {
                       if (operation.canUseAsSource(datalist)) {                        
                            String names=datalist[0].getName();
                            for (int i=1;i<datalist.length;i++) {
                                names+=","+datalist[i].getName();
                            }
                            parameters.setParameter(OperationTask.SOURCE_NAME, names);
                        } else if (operation.canUseAnyAsSourceProxy(datalist)) {
                            operation.assignToProxy(datalist, parameters);
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
                    if (dataitem!=null) { // single parameter
                        addProxySourceToAnalysis(dataitem,analysis,parametersettings);
                    }
                    else if (datalist!=null && datalist.length>0) { // multiple parameters
                        for (int i=0;i<datalist.length;i++) {
                            Data source=datalist[i];
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
              // String dataitemName=dataitem.getName();
              // gui.debugMessage("Selected "+e.getActionCommand()+" on "+dataitemName);

               if (e.getActionCommand().startsWith(RENAME_DATA)) {
                    if (dataitem==null) return;
                    RenameDataDialog dialog=new RenameDataDialog(gui.getFrame(), gui, dataitem.getName());
                    dialog.setLocation(100, 250);    
                    dialog.setVisible(true);
                    dialog.dispose();
               } else if (e.getActionCommand().startsWith(DELETE_DATA)) {
                    if (dataitem!=null && dataitem!=engine.getDefaultSequenceCollection()) gui.deleteDataItem(dataitem.getName());
                    else if (datalist!=null) {
                        String[] names=new String[datalist.length];
                        for (int i=0;i<datalist.length;i++) {
                            if (datalist[i]!=engine.getDefaultSequenceCollection()) names[i]=datalist[i].getName();
                        }
                        gui.deleteDataItems(names);                
                    }  
               } else if (e.getActionCommand().startsWith(EDIT_DATA) || e.getActionCommand().startsWith(DISPLAY_DATA)) {
                    if (dataitem!=null) datapanel.showPrompt(dataitem);
                    else if (datalist!=null && datalist.length>=1) datapanel.showPrompt(datalist[0]);
               } else if (e.getActionCommand().equals(COLOR_CLUSTER_SUBMENU_HEADER)) {
                    if (dataitem instanceof SequencePartition) setClusterColors(gui.getVisualizationSettings(),(SequencePartition)dataitem);
               } else if (e.getActionCommand().equals(ORDER_BY_CLUSTER)) {
                    if (dataitem instanceof SequencePartition) orderByClusters(gui.getVisualizationSettings(),(SequencePartition)dataitem);
               } else if (e.getActionCommand().equals(SHOW_SEQUENCES)) {
                    if (dataitem instanceof SequenceCollection) datapanel.setSequenceVisibility(gui.getVisualizationSettings(), (SequenceCollection)dataitem, true);
                    else if (datalist!=null)  datapanel.setSequenceVisibilityOnAll(gui.getVisualizationSettings(), datalist, true);
               } else if (e.getActionCommand().equals(SHOW_ONLY_SEQUENCES)) {
                    datapanel.setSequenceVisibility(gui.getVisualizationSettings(), engine.getDefaultSequenceCollection(), false);
                    if (dataitem instanceof SequenceCollection) datapanel.setSequenceVisibility(gui.getVisualizationSettings(), (SequenceCollection)dataitem, true);
                    else if (datalist!=null)  datapanel.setSequenceVisibilityOnAll(gui.getVisualizationSettings(), datalist, true);
               } else if (e.getActionCommand().equals(HIDE_SEQUENCES)) {
                    if (dataitem instanceof SequenceCollection) datapanel.setSequenceVisibility(gui.getVisualizationSettings(), (SequenceCollection)dataitem, false);
                    else if (datalist!=null)  datapanel.setSequenceVisibilityOnAll(gui.getVisualizationSettings(), datalist, false);
               } else if (e.getActionCommand().equals(MARK_SEQUENCES)) {
                    if (dataitem instanceof SequenceCollection) datapanel.setSequenceMark(gui.getVisualizationSettings(), (SequenceCollection)dataitem, true, true);
                    else if (datalist!=null)  datapanel.setSequenceMarkOnAll(gui.getVisualizationSettings(), datalist, true);                
               } else if (e.getActionCommand().equals(REMOVE_MARKS)) {
                    if (dataitem instanceof SequenceCollection) datapanel.setSequenceMark(gui.getVisualizationSettings(), (SequenceCollection)dataitem, false, true);
                    else if (datalist!=null)  datapanel.setSequenceMarkOnAll(gui.getVisualizationSettings(), datalist, false);                   
               }
           }            
        }
        
   
        /** Sets the label color of all sequences in the collection to the given color */
        private void setSequenceLabelColors(VisualizationSettings settings, SequenceCollection collection, Color color, boolean redraw) {
            for (String sequence:collection.getAllSequenceNames()) {
                settings.setSequenceLabelColor(sequence,color);
            }
            if (redraw) gui.redraw();
        }
        
        private void setSequenceLabelColorsOnAll(VisualizationSettings settings, Data[] data, Color color) {
            for (Data entry:data) {
                if (entry instanceof SequenceCollection) setSequenceLabelColors(settings, (SequenceCollection)entry, color,false);
            }           
            gui.redraw();
        }        
              
        /** Sets the label color of all sequences based on colors for clusters in the partition */
        private void setClusterColors(VisualizationSettings settings, SequencePartition partition) {
            for (String sequence:engine.getDefaultSequenceCollection().getAllSequenceNames()) {
                String clusterName=partition.getClusterForSequence(sequence);
                Color color=(clusterName!=null)?settings.getClusterColor(clusterName):Color.BLACK;
                settings.setSequenceLabelColor(sequence,color);
            }
            gui.redraw();
        }
        
        /** Orders the sequences according to the clusters in the partition */
        private void orderByClusters(VisualizationSettings settings, SequencePartition partition) {
            ArrayList<String> list = engine.getDefaultSequenceCollection().getAllSequenceNames();
            Collections.sort(list, new SortOrderComparator(partition));
            engine.getDefaultSequenceCollection().setSequenceOrder(list);
            gui.redraw();
        }
        
        private class SortOrderComparator implements Comparator<String> {
            private SequencePartition partition=null;
            public SortOrderComparator(SequencePartition partition) {this.partition=partition;}
            @Override
            public int compare(String seq1, String seq2) { // these are two motifnames
                String cluster1=partition.getClusterForSequence(seq1);
                String cluster2=partition.getClusterForSequence(seq2);
                if (cluster1==null && cluster2==null) return 0;
                else if (cluster1==null && cluster2!=null) return 1;
                else if (cluster1!=null && cluster2==null) return -1;
                else return cluster1.compareTo(cluster2);
            }    
        }          
                            
}
