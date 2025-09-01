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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.tree.TreeNode;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleMotif;
import org.motiflab.engine.data.ModulePartition;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifClassification;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.operations.Operation_analyze;



/**
*
* @author kjetikl
*/
public class MotifsPanelContextMenu extends JPopupMenu  {
private final static String EDIT_DATA="Edit";
private final static String RENAME_DATA="Rename";
private final static String DELETE_DATA="Delete";
private final static String SHOW="Show";
private final static String HIDE="Hide";
private final static String SHOW_ONLY_SELECTED="Show Only Selected";   
private final static String SHOW_ALL="Show All";  
private final static String HIDE_ALL="Hide All";      
private final static String COLOR_SUBMENU_HEADER="Set Color";
private final static String COLOR_BY_CLUSTERS="Set Motif Colors from Clusters";
private final static String COMPARE_TO_KNOWN_MOTIFS="Compare to Other Motifs";
private final static String SAVE_MOTIF_LOGO="Save Motif Logo";  
private final static String OPERATION_SUBMENU_HEADER="Perform Operation";
private final static String SAVE_AS_PREDEFINED="Save As Predefined";
private final static String EXPAND_NODE="Expand";
private final static String COLLAPSE_NODE="Collapse";
private final static String EXPAND_ALL_NODES="Expand All";
private final static String COLLAPSE_ALL_NODES="Collapse All";


private MotifLabEngine engine;
private JTree tree;
private MotifsPanel panel;
private MotifLabGUI gui;
private JMenu operationsMenu;
private Data dataitem=null;
private Data[] dataitems=null;
private CheckBoxTreeNode node;

public static MotifsPanelContextMenu getInstance(JTree tree, MotifLabGUI gui, MotifsPanel panel) {
     if (tree.isSelectionEmpty()) return null;
     return new MotifsPanelContextMenu(tree,gui,panel);
}

    private MotifsPanelContextMenu(JTree tree, MotifLabGUI gui, MotifsPanel motifspanel) {
        this.engine=gui.getEngine();
        this.tree=tree;            
        this.gui=gui;
        this.panel=motifspanel;
        int[] selection=tree.getSelectionRows();
        Object sources="";
        node=null;
        int nodetypes=0; // 1 means only leaf nodes are selected, 2 means only internal nodes and 3 is a mix
        if (selection!=null) {
            if (selection.length==1) {
                node=(CheckBoxTreeNode)tree.getPathForRow(selection[0]).getLastPathComponent();
                Object userObject=node.getUserObject();
                if (userObject instanceof Data) dataitem=(Data)userObject;
                if (dataitem!=null) sources=dataitem;
                if (node.isLeaf()) nodetypes=1; else nodetypes=2;
            } else { // multiple selections
                int countOK=0;
                for (int selectionIndex=0;selectionIndex<selection.length;selectionIndex++) {
                    CheckBoxTreeNode treenode=(CheckBoxTreeNode)tree.getPathForRow(selection[selectionIndex]).getLastPathComponent();
                    Object userObject=treenode.getUserObject();
                    if (userObject instanceof Data) countOK++;       
                    if (treenode.isLeaf()) {
                        if (nodetypes==0 || nodetypes==1) nodetypes=1;
                        else nodetypes=3;
                    } else {
                        if (nodetypes==0 || nodetypes==2) nodetypes=2;
                        else nodetypes=3;                      
                    }
                }
                dataitems=new Data[countOK];
                int dataindex=0;
                for (int selectionIndex=0;selectionIndex<selection.length;selectionIndex++) {
                    CheckBoxTreeNode treenode=(CheckBoxTreeNode)tree.getPathForRow(selection[selectionIndex]).getLastPathComponent();
                    Object userObject=treenode.getUserObject();
                    if (userObject instanceof Data) {
                        dataitems[dataindex]=(Data)userObject;
                        dataindex++;
                    }               
                }               
                if (countOK>0) sources=dataitems;
            }
        }  
        OperationsMenuListener menulistener=new OperationsMenuListener();
        operationsMenu=gui.getOperationContextMenu(OPERATION_SUBMENU_HEADER, sources, menulistener); // the empty string is used to signal "unrecognized source"
        operationsMenu.setEnabled(operationsMenu.getItemCount()>0); 
        this.add(operationsMenu);
        this.add(new JSeparator());
        
        DisplayMenuItemListener menuItemListener=new DisplayMenuItemListener();
        JMenuItem showItem=new JMenuItem(SHOW);
        JMenuItem showOnlySelectedItem=new JMenuItem(SHOW_ONLY_SELECTED);   
        JMenuItem showAllItem=new JMenuItem(SHOW_ALL);               
        JMenuItem hideItem=new JMenuItem(HIDE);
        JMenuItem hideAllItem=new JMenuItem(HIDE_ALL);

        showItem.addActionListener(menuItemListener);
        showOnlySelectedItem.addActionListener(menuItemListener);
        showAllItem.addActionListener(menuItemListener);
        hideItem.addActionListener(menuItemListener);
        hideAllItem.addActionListener(menuItemListener);            
        ColorMenuListener colormenulistener=new ColorMenuListener() {
              public void newColorSelected(Color color) {
                if (color==null) return;
                     if (dataitem instanceof Motif) {setColorForMotif((Motif)dataitem,color);}
                else if (dataitem instanceof ModuleCRM) {setColorForModule((ModuleCRM)dataitem,color);}
                else if (node!=null && node.getUserObject() instanceof ModuleMotif) {setColorForModuleMotif(node,color);}
                else {panel.setColorOnSelectedRows(color);} // multiple rows selected
             }
        };
        ColorMenu colorMenu=new ColorMenu(COLOR_SUBMENU_HEADER,colormenulistener,panel);

        if (dataitem!=null) {
            JMenuItem editDataItem=new JMenuItem(EDIT_DATA+" \""+dataitem.getName()+"\"");
            editDataItem.addActionListener(menuItemListener);
            this.add(editDataItem);
            if (!gui.getScheduler().isIdle()) editDataItem.setEnabled(false);
        }

        this.add(showItem);
        this.add(showOnlySelectedItem); 
        this.add(showAllItem);            
        this.add(hideItem);   
        this.add(hideAllItem);  

        if (nodetypes>=2) {
            JMenuItem expandItem=new JMenuItem(EXPAND_NODE);
            expandItem.addActionListener(menuItemListener);
            this.add(expandItem);
            JMenuItem expandAllItem=new JMenuItem(EXPAND_ALL_NODES);
            expandAllItem.addActionListener(menuItemListener);
            this.add(expandAllItem);            
            JMenuItem contractItem=new JMenuItem(COLLAPSE_NODE);
            contractItem.addActionListener(menuItemListener);
            this.add(contractItem);   
            JMenuItem contractAllItem=new JMenuItem(COLLAPSE_ALL_NODES);
            contractAllItem.addActionListener(menuItemListener);
            this.add(contractAllItem);              
        }           
        
        this.add(colorMenu);
        
        if (dataitem instanceof MotifPartition) {
            JMenuItem colorByClusters=new JMenuItem(COLOR_BY_CLUSTERS);
            colorByClusters.addActionListener(menuItemListener);
            this.add(colorByClusters);
        }          
                    
        if (dataitem instanceof Motif) {
            JMenuItem compareMotifItem=new JMenuItem(COMPARE_TO_KNOWN_MOTIFS);
            compareMotifItem.addActionListener(menuItemListener);
            this.add(compareMotifItem); 
            JMenuItem saveLogoItem=new JMenuItem(SAVE_MOTIF_LOGO);
            saveLogoItem.addActionListener(menuItemListener);
            this.add(saveLogoItem);     
            String id=((Motif)dataitem).getName();
            ExternalDBLinkMenu dbmenu=new ExternalDBLinkMenu(id, gui);
            this.add(dbmenu);// if (dbmenu.isEnabled()) this.add(dbmenu);            
        }
        if (dataitem instanceof MotifCollection || dataitem instanceof ModuleCollection) {
            JMenuItem saveAsPredefinedItem=new JMenuItem(SAVE_AS_PREDEFINED);
            saveAsPredefinedItem.addActionListener(menuItemListener);
            this.add(saveAsPredefinedItem);
        }                     
        if (dataitem instanceof MotifCollection || dataitem instanceof MotifPartition || dataitem instanceof ModuleCollection || dataitem instanceof ModulePartition) {
            JMenuItem renameDataItem=new JMenuItem(RENAME_DATA+" \""+dataitem.getName()+"\"");
            JMenuItem deleteDataItem=new JMenuItem(DELETE_DATA+" \""+dataitem.getName()+"\"");
            renameDataItem.addActionListener(menuItemListener);
            deleteDataItem.addActionListener(menuItemListener);
            this.add(new JSeparator());
            this.add(renameDataItem);
            this.add(deleteDataItem);
            if (!gui.getScheduler().isIdle()) {
                deleteDataItem.setEnabled(false);
                renameDataItem.setEnabled(false);
            }
        }    
        
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
                    else if (dataitems!=null && dataitems.length>0) {
                        if (operation.canUseAsSource(dataitems)) {
                            String names=dataitems[0].getName();
                            for (int i=1;i<dataitems.length;i++) {
                                names+=","+dataitems[i].getName();
                            }
                            parameters.setParameter(OperationTask.SOURCE_NAME, names);
                        } else if (operation.canUseAnyAsSourceProxy(dataitems)) {
                            operation.assignToProxy(dataitems, parameters);
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
                    else if (dataitems!=null && dataitems.length>0) { // multiple parameters
                        for (int i=0;i<dataitems.length;i++) {
                            Data source=dataitems[i];
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
                if (proxysource==null) continue; // to avoid some bug
                Class proxytype=proxysource.getType();
                if (proxytype.isAssignableFrom(source.getClass()) && !settings.hasAssignedValueForParameter(proxy)) {proxyparameter=proxy;break;} // assign to first applicable parameter which is not already taken
            }
            if (proxyparameter!=null) settings.setParameter(proxyparameter, source.getName());
        }      
      
     /**
     * An inner class that listens to popup-menu events NOT related to the operations submenu and notifies the gui events
     */       
    private class DisplayMenuItemListener implements ActionListener {
       @Override
       public void actionPerformed(ActionEvent e) {
           if (e.getActionCommand().startsWith(RENAME_DATA)) {
                RenameDataDialog dialog=new RenameDataDialog(gui.getFrame(), gui, dataitem.getName());
                dialog.setLocation(100, 250);    
                dialog.setVisible(true);
                dialog.dispose();
           } else if (e.getActionCommand().startsWith(DELETE_DATA)) {
                gui.deleteDataItem(dataitem.getName());
           } else if (e.getActionCommand().startsWith(EDIT_DATA)) {
                panel.showPrompt(dataitem, true, false);
           } else if (e.getActionCommand().equals(SHOW_ONLY_SELECTED)) {
                panel.showOnlySelectedRows();
           } else if (e.getActionCommand().equals(SHOW_ALL)) {
                panel.setVisibilityOnAll(true);
           } else if (e.getActionCommand().equals(HIDE_ALL)) {
                panel.setVisibilityOnAll(false);
           } else if (e.getActionCommand().equals(SHOW)) {
                if (dataitem instanceof Motif || dataitem instanceof ModuleCRM) {gui.getVisualizationSettings().setRegionTypeVisible(dataitem.getName(),true,true);}
                else {panel.setVisibilityOnSelectedRows(true);}
           } else if (e.getActionCommand().equals(HIDE)) {
                if (dataitem instanceof Motif || dataitem instanceof ModuleCRM) {gui.getVisualizationSettings().setRegionTypeVisible(dataitem.getName(),false,true);}
                else {panel.setVisibilityOnSelectedRows(false);}
           } else if (e.getActionCommand().equals(EXPAND_NODE)) {
                panel.expandSelectedRows();
           } else if (e.getActionCommand().equals(COLLAPSE_NODE)) {
                panel.collapseSelectedRows();
           } else if (e.getActionCommand().equals(EXPAND_ALL_NODES)) {
                panel.expandAllRows();
           } else if (e.getActionCommand().equals(COLLAPSE_ALL_NODES)) {
                panel.collapseAllRows();
           } else if (e.getActionCommand().equals(COMPARE_TO_KNOWN_MOTIFS)) {
                MotifComparisonDialog motifcomparisonPanel=new MotifComparisonDialog(gui, (Motif)dataitem, false);
                motifcomparisonPanel.setLocation(gui.getFrame().getWidth()/2-motifcomparisonPanel.getWidth()/2, gui.getFrame().getHeight()/2-motifcomparisonPanel.getHeight()/2);
                motifcomparisonPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                motifcomparisonPanel.setVisible(true);
           } else if (e.getActionCommand().equals(SAVE_MOTIF_LOGO)) {
                SaveMotifLogoImageDialog saveLogoPanel=new SaveMotifLogoImageDialog(gui, (Motif)dataitem, false);
                saveLogoPanel.setLocation(gui.getFrame().getWidth()/2-saveLogoPanel.getWidth()/2, gui.getFrame().getHeight()/2-saveLogoPanel.getHeight()/2);
                saveLogoPanel.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                saveLogoPanel.setVisible(true);
           } else if (e.getActionCommand().equals(SAVE_AS_PREDEFINED)) {
                      if (dataitem instanceof MotifCollection)  saveAsPredefinedMotifCollection();
                 else if (dataitem instanceof ModuleCollection) saveAsPredefinedModuleCollection();
           } else if (e.getActionCommand().equals(COLOR_BY_CLUSTERS)) {
                if (dataitem instanceof MotifPartition) {
                   ArrayList<String> clusters=((MotifPartition)dataitem).getClusterNames();
                   for (String cluster:clusters) {
                       Color color=gui.getVisualizationSettings().getClusterColor(cluster);
                       ArrayList<String> list=((MotifPartition)dataitem).getAllMotifNamesInCluster(cluster);
                       String[] stringAsArray=new String[list.size()];
                       stringAsArray=list.toArray(stringAsArray);
                       gui.getVisualizationSettings().setFeatureColor(stringAsArray, color, false);
                   }  
                   gui.getVisualizationSettings().setColorMotifsByClass(false);
                   gui.redraw();
                }
           }
       }            
    }

    private void saveAsPredefinedMotifCollection() {
       String collectionName=null;
       if (dataitem instanceof MotifCollection) collectionName=((MotifCollection)dataitem).getPredefinedCollectionName();
       if (collectionName==null || collectionName.isEmpty()) collectionName=dataitem.getName();
       boolean ok=false;
       String msg="Please enter a name for the collection";
       while (!ok) {
           collectionName=(String)JOptionPane.showInputDialog(gui.getFrame(), msg, "Save collection as predefined", JOptionPane.QUESTION_MESSAGE, null, null, collectionName);
           if (collectionName==null || collectionName.trim().isEmpty()) {
               return;
           }
           String test=collectionName.replaceAll("\\s", "_");
           String nameerror=gui.getEngine().checkNameValidity(test, false);
           if (nameerror==null) ok=true;
           else msg=nameerror;
       }
       java.util.Set<String> existingCollections=engine.getPredefinedMotifCollections();
       if (existingCollections.contains(collectionName)) {
           int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "There is already a predefined collection with this name.\nDo you want to replace it?","Save collection as predefined",JOptionPane.YES_NO_CANCEL_OPTION);
           if (choice!=JOptionPane.YES_OPTION) return;
       }
       ((MotifCollection)dataitem).setPredefinedCollectionName(collectionName);
       try {
            engine.registerPredefinedMotifCollection((MotifCollection) dataitem); // this will also set the 'predefined collection name' in the dataitem
            JOptionPane.showMessageDialog(gui.getFrame(), "Collection saved OK", "Save collection as predefined", JOptionPane.INFORMATION_MESSAGE);
       } catch (Exception ex) {
            String errmsg="An error occurred while saving motif collection:\n"+ex.toString();
            JOptionPane.showMessageDialog(gui.getFrame(), errmsg, "Save error", JOptionPane.ERROR_MESSAGE);
       }
    }
    
    private void saveAsPredefinedModuleCollection() {
       String collectionName=null;
       if (dataitem instanceof ModuleCollection) collectionName=((ModuleCollection)dataitem).getPredefinedCollectionName();
       if (collectionName==null || collectionName.isEmpty()) collectionName=dataitem.getName();
       boolean ok=false;
       String msg="Please enter a name for the collection";
       while (!ok) {
           collectionName=(String)JOptionPane.showInputDialog(gui.getFrame(), msg, "Save collection as predefined", JOptionPane.QUESTION_MESSAGE, null, null, collectionName);
           if (collectionName==null || collectionName.trim().isEmpty()) {
               return;
           }
           String test=collectionName.replaceAll("\\s", "_");
           String nameerror=gui.getEngine().checkNameValidity(test, false);
           if (nameerror==null) ok=true;
           else msg=nameerror;
       }
       java.util.Set<String> existingCollections=engine.getPredefinedModuleCollections();
       if (existingCollections.contains(collectionName)) {
           int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "There is already a predefined collection with this name.\nDo you want to replace it?","Save collection as predefined",JOptionPane.YES_NO_CANCEL_OPTION);
           if (choice!=JOptionPane.YES_OPTION) return;
       }
       int choice=JOptionPane.showConfirmDialog(gui.getFrame(), "Would you like to include the single motif models when saving?\n(If they are not included, the motifs must be imported separately)","Save collection as predefined",JOptionPane.YES_NO_OPTION);
       boolean includeMotifModels=(choice==JOptionPane.YES_OPTION);
       ((ModuleCollection)dataitem).setPredefinedCollectionName(collectionName);
       try {
            engine.registerPredefinedModuleCollection((ModuleCollection) dataitem, includeMotifModels); // this will also set the 'predefined collection name' in the dataitem
            JOptionPane.showMessageDialog(gui.getFrame(), "Collection saved OK", "Save collection as predefined", JOptionPane.INFORMATION_MESSAGE);
       } catch (Exception ex) {
            String errmsg="An error occurred while saving module collection:\n"+ex.toString();
            JOptionPane.showMessageDialog(gui.getFrame(), errmsg, "Save error", JOptionPane.ERROR_MESSAGE);
       }
    }    


    private void setColorForModule(ModuleCRM cisRegModule, Color newColor) {
        VisualizationSettings settings=gui.getVisualizationSettings();
        settings.setFeatureColor(cisRegModule.getName(), newColor, true);
    }

    private void setColorForModuleMotif(CheckBoxTreeNode node, Color newColor) {
        VisualizationSettings settings=gui.getVisualizationSettings();
        ModuleMotif modulemotif=(ModuleMotif)node.getUserObject();
        CheckBoxTreeNode modulenode=(CheckBoxTreeNode)node.getParent();
        ModuleCRM cisRegModule=(ModuleCRM)modulenode.getUserObject();
        settings.setFeatureColor(cisRegModule.getName()+"."+modulemotif.getRepresentativeName(), newColor, true);
    }

    private void setColorForMotif(Motif motif, Color newColor) {
        VisualizationSettings settings=gui.getVisualizationSettings();
        settings.setFeatureColor(motif.getName(), newColor, true);
        if (node!=null) { // clear parent nodes colors
            TreeNode[] pathtoroot=node.getPath();
            for (int i=pathtoroot.length-2;i>0;i--) {
                if (pathtoroot[i] instanceof CheckBoxTreeNode) {
                    CheckBoxTreeNode parent=(CheckBoxTreeNode)pathtoroot[i];
                    settings.setClassLabelColor(parent.getName(),null);                     
                }
            } 
        }
         // clear also colors from parent class labels
        String motifclass=((Motif)motif).getClassification();
        if (motifclass==null) {
            settings.setClassLabelColor(MotifClassification.UNKNOWN_CLASS_LABEL, null);
        } else {
            String[] path=MotifClassification.getClassPath(motifclass);
            for (int i=0;i<path.length;i++) settings.setClassLabelColor(path[i], null);
        }
    }


}
