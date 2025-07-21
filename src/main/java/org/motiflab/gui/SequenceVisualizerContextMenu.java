/*
 
 
 */

package org.motiflab.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSeparator;
import javax.swing.JMenu;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.operations.Operation_analyze;


/**
 * This class represents the context menu shown when right-clicking inside a sequence window.
 * It contains options for the particular track (including applicable operations and show/hide),
 * the particular sequence, selection window options, and more general options (set colors).
 * If the track is a region track with a selected region, the menu also contains options for this region (edit/delete)
 * 
 * @author kjetikl
 */
public class SequenceVisualizerContextMenu extends JPopupMenu  {
    // Please note that all these labels should be distinct (even in submenus!)
    private static String LABEL_ZOOM="Zoom";
    private static String LABEL_ORIENTATION="Orientation";
    private static String LABEL_ORIENTATION_FLIP_POINT="Flip Around Point";
    private static String LABEL_ALIGNMENT="Alignment";
    private static String LABEL_RULER="Ruler";
    private static String LABEL_RULER_TSS="Origo At Gene TSS";
    private static String LABEL_RULER_TES="Origo At Gene TES";    
    private static String LABEL_RULER_UPSTREAM_END="Origo At Upstream End";
    private static String LABEL_RULER_DOWNSTREAM_END="Origo At Downstream End";
    private static String LABEL_RULER_CHROMOSOME="Origo At Chromosome Start";
    private static String LABEL_RULER_FIXED="Fixed Ruler";
    private static String LABEL_RULER_NONE="No Ruler";
    private static String LABEL_REORDER="Reorder Sequences";
    private static String OPERATION_SUBMENU_HEADER="Perform Operation";
    private static String HIDE_TRACK="Hide Track";
    private static String EXPAND_TRACK="Expand Track";
    private static String CONTRACT_TRACK="Contract Track";
    private static String EDIT_REGION="Edit Region";
    private static String DELETE_REGION="Delete Region";
    private static String SET_BORDER_COLOR="Set Track Border Color";   
    private static String SET_BOX_COLOR="Set Bounding Box Color";   
    private static String SET_CURSOR_FLANKS="Set Cursor Flanks"; 
    private static Font headerfont=new Font(Font.SANS_SERIF,Font.BOLD,12);

    //private static FeatureSequenceData sequenceData;
    private static MotifLabGUI gui;
    private static int position=-1;
    private static String sequenceName=null;
    private static String trackName=null;
    private static FeatureDataset dataset=null;
    private static SequenceVisualizerContextMenu thismenu=null;
    private static JMenuItem header=null;
    private static JCheckBoxMenuItem menuitemRulerTSS;
    private static JCheckBoxMenuItem menuitemRulerTES;
    private static JCheckBoxMenuItem menuitemRulerLeftEnd;
    private static JCheckBoxMenuItem menuitemRulerRightEnd;
    private static JCheckBoxMenuItem menuitemRulerChromosome;
    private static JCheckBoxMenuItem menuitemRulerFixed;
    private static JCheckBoxMenuItem menuitemRulerNone;
    private static JCheckBoxMenuItem menuitemAlignNone;
    private static JCheckBoxMenuItem menuitemAlignLeft;
    private static JCheckBoxMenuItem menuitemAlignRight;
    private static JCheckBoxMenuItem menuitemAlignTSS;
    private static JMenuItem moveUpMenuItem=null;
    private static JMenuItem moveDownMenuItem=null;
    private static JMenuItem moveToTopMenuItem=null;
    private static JMenuItem moveToBottomMenuItem=null;    
    private static JMenuItem setViewportMenuItem=null;
    private static JMenuItem hideSequenceMenuItem=null;
    private static JMenuItem hideOtherSequencesMenuItem=null;
    private static JMenuItem selectAllMenuItem=null;
    private static JMenuItem inverSelectionMenuItem=null;
    private static SequenceVisualizer sequenceVisualizer;
    private static ActionMap actionMap;
    private static ActionMap visualizationPanelActionMap;
    private static Region currentRegion=null;
    
    private static ExternalDBLinkMenu externalDBmenu=null;
    
    public static SequenceVisualizerContextMenu getInstance(SequenceVisualizer sequencevisualizer, String featuretrackName, int screenPosition, MotifLabGUI GUI) {
         position=screenPosition;
         sequenceVisualizer=sequencevisualizer;
         gui=GUI;
         sequenceName=sequenceVisualizer.getSequenceName();
         trackName=featuretrackName;
         Data data=gui.getEngine().getDataItem(trackName);
         if (data!=null && data instanceof FeatureDataset) dataset=(FeatureDataset)data; else dataset=null;
         actionMap=sequenceVisualizer.getActionMap();
         DataTrackVisualizer trackviz=sequenceVisualizer.getTrackVisualizer(trackName);
         if (trackviz instanceof DataTrackVisualizer_Region) {
           currentRegion=((DataTrackVisualizer_Region)trackviz).getCurrentRegion();
         } else currentRegion=null;
         VisualizationPanel parent=sequencevisualizer.getVisualizationPanel();
         if (parent!=null) visualizationPanelActionMap=parent.getActionMap();
         thismenu=new SequenceVisualizerContextMenu();
         String currentRuler=gui.getVisualizationSettings().getRuler(null);
              if (currentRuler.equals(VisualizationSettings.RULER_TSS)) menuitemRulerTSS.setSelected(true);
         else if (currentRuler.equals(VisualizationSettings.RULER_TES)) menuitemRulerTES.setSelected(true);
         else if (currentRuler.equals(VisualizationSettings.RULER_UPSTREAM_END)) menuitemRulerLeftEnd.setSelected(true);
         else if (currentRuler.equals(VisualizationSettings.RULER_DOWNSTREAM_END)) menuitemRulerRightEnd.setSelected(true);
         else if (currentRuler.equals(VisualizationSettings.RULER_CHROMOSOME)) menuitemRulerChromosome.setSelected(true);
         else if (currentRuler.equals(VisualizationSettings.RULER_FIXED)) menuitemRulerFixed.setSelected(true);
         else if (currentRuler.equals(VisualizationSettings.RULER_NONE)) menuitemRulerNone.setSelected(true);
         String currentAlignment=gui.getVisualizationSettings().getSequenceAlignment(sequenceName);
              if (currentAlignment.equals(VisualizationSettings.ALIGNMENT_NONE)) menuitemAlignNone.setSelected(true);
         else if (currentAlignment.equals(VisualizationSettings.ALIGNMENT_LEFT)) menuitemAlignLeft.setSelected(true);
         else if (currentAlignment.equals(VisualizationSettings.ALIGNMENT_RIGHT)) menuitemAlignRight.setSelected(true);
         else if (currentAlignment.equals(VisualizationSettings.ALIGNMENT_TSS)) menuitemAlignTSS.setSelected(true);           
         return thismenu;
    }
    
    
        private SequenceVisualizerContextMenu() {
            MenuItemListener menuItemListener=new MenuItemListener();
            JMenuItem menuitem;
            
            header=new JMenuItem((dataset!=null)?(sequenceName+" : "+trackName):sequenceName);
            header.setEnabled(false);
            header.setFont(headerfont);
            this.add(header);
            this.add(new JSeparator());

            if (dataset!=null) {
                OperationsMenuListener menulistener=new OperationsMenuListener();
                JMenu operationsMenu=gui.getOperationContextMenu(OPERATION_SUBMENU_HEADER, dataset, menulistener);
                operationsMenu.setEnabled(operationsMenu.getItemCount()>0); 
                this.add(operationsMenu);
                if (currentRegion!=null) {
                    menuitem=new JMenuItem(EDIT_REGION);
                    menuitem.addActionListener(menuItemListener);
                    this.add(menuitem);
                    menuitem=new JMenuItem(DELETE_REGION);
                    menuitem.addActionListener(menuItemListener);
                    this.add(menuitem);
                    // database menu
                    externalDBmenu=new ExternalDBLinkMenu(currentRegion.getType(), trackName, gui);
                    if (externalDBmenu.isEnabled()) this.add(externalDBmenu);                    
                }
                this.add(new JSeparator());
                menuitem=new JMenuItem(HIDE_TRACK);
                menuitem.addActionListener(menuItemListener);
                this.add(menuitem);
                if (dataset instanceof RegionDataset) {
                    String expandString=gui.getVisualizationSettings().isExpanded(trackName)?CONTRACT_TRACK:EXPAND_TRACK;
                    menuitem=new JMenuItem(expandString);
                    menuitem.addActionListener(menuItemListener);
                    this.add(menuitem);
                }
                this.add(new JSeparator());
            }
                 
            JMenu zoomMenu=new JMenu(LABEL_ZOOM);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ZOOM_IN));
                menuitem.setIcon(null);
                zoomMenu.add(menuitem);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ZOOM_OUT));
                menuitem.setIcon(null);
                zoomMenu.add(menuitem);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ZOOM_RESET));
                menuitem.setIcon(null);
                zoomMenu.add(menuitem);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ZOOM_TO_FIT));
                menuitem.setIcon(null);
                zoomMenu.add(menuitem);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ZOOM_TO_CUSTOM_LEVEL));
                menuitem.setIcon(null);
                zoomMenu.add(menuitem);

            JMenu orientationMenu=new JMenu(LABEL_ORIENTATION);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ORIENTATION_DIRECT));
                menuitem.setIcon(null);
                orientationMenu.add(menuitem);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ORIENTATION_REVERSE));
                menuitem.setIcon(null);
                orientationMenu.add(menuitem);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ORIENTATION_RELATIVE));
                menuitem.setIcon(null);
                orientationMenu.add(menuitem);
                menuitem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_ORIENTATION_FLIP));
                menuitem.setIcon(null);
                orientationMenu.add(menuitem);
                menuitem=new JMenuItem(LABEL_ORIENTATION_FLIP_POINT);
                menuitem.addActionListener(menuItemListener);
                orientationMenu.add(menuitem);

            ButtonGroup alignmentgroup=new ButtonGroup();
            JMenu alignmentMenu=new JMenu(LABEL_ALIGNMENT);

            menuitemAlignLeft=new JCheckBoxMenuItem(visualizationPanelActionMap.get(SequenceVisualizer.ACTION_ALIGN_LEFT));
                menuitemAlignLeft.setIcon(null);
                alignmentMenu.add(menuitemAlignLeft);
                alignmentgroup.add(menuitemAlignLeft);
            menuitemAlignRight=new JCheckBoxMenuItem(visualizationPanelActionMap.get(SequenceVisualizer.ACTION_ALIGN_RIGHT));
                menuitemAlignRight.setIcon(null);
                alignmentMenu.add(menuitemAlignRight);
                alignmentgroup.add(menuitemAlignRight);
            menuitemAlignTSS=new JCheckBoxMenuItem(SequenceVisualizer.ACTION_ALIGN_TSS_HERE);
                menuitemAlignTSS.addActionListener(menuItemListener);
                alignmentMenu.add(menuitemAlignTSS);
                alignmentgroup.add(menuitemAlignTSS);
            menuitemAlignNone=new JCheckBoxMenuItem(visualizationPanelActionMap.get(SequenceVisualizer.ACTION_ALIGN_NONE));
                menuitemAlignNone.setIcon(null);
                alignmentMenu.add(menuitemAlignNone);
                alignmentgroup.add(menuitemAlignNone);
                
            ButtonGroup rulergroup=new ButtonGroup();
            JMenu rulerMenu=new JMenu(LABEL_RULER);
             menuitemRulerTSS=new JCheckBoxMenuItem(LABEL_RULER_TSS);
                menuitemRulerTSS.addActionListener(menuItemListener);
                rulerMenu.add(menuitemRulerTSS);
                rulergroup.add(menuitemRulerTSS);
             menuitemRulerTES=new JCheckBoxMenuItem(LABEL_RULER_TES);
                menuitemRulerTES.addActionListener(menuItemListener);
                rulerMenu.add(menuitemRulerTES);
                rulergroup.add(menuitemRulerTES);                
             menuitemRulerChromosome=new JCheckBoxMenuItem(LABEL_RULER_CHROMOSOME);
                menuitemRulerChromosome.addActionListener(menuItemListener);
                rulerMenu.add(menuitemRulerChromosome);
                rulergroup.add(menuitemRulerChromosome);
            menuitemRulerLeftEnd=new JCheckBoxMenuItem(LABEL_RULER_UPSTREAM_END);
                menuitemRulerLeftEnd.addActionListener(menuItemListener);
                rulerMenu.add(menuitemRulerLeftEnd);
                rulergroup.add(menuitemRulerLeftEnd);
             menuitemRulerRightEnd=new JCheckBoxMenuItem(LABEL_RULER_DOWNSTREAM_END);
                menuitemRulerRightEnd.addActionListener(menuItemListener);
                rulerMenu.add(menuitemRulerRightEnd);
                rulergroup.add(menuitemRulerRightEnd);
             menuitemRulerFixed=new JCheckBoxMenuItem(LABEL_RULER_FIXED);
                menuitemRulerFixed.addActionListener(menuItemListener);
                rulerMenu.add(menuitemRulerFixed);
                rulergroup.add(menuitemRulerFixed);
             menuitemRulerNone=new JCheckBoxMenuItem(LABEL_RULER_NONE);
                menuitemRulerNone.addActionListener(menuItemListener);
                rulerMenu.add(menuitemRulerNone);
                rulergroup.add(menuitemRulerNone);

            JMenu rearrangeMenu=new JMenu(LABEL_REORDER);
                moveUpMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_REORDER_MOVE_UP));
                moveUpMenuItem.setIcon(null);
                rearrangeMenu.add(moveUpMenuItem);
                moveDownMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_REORDER_MOVE_DOWN));
                moveDownMenuItem.setIcon(null);
                rearrangeMenu.add(moveDownMenuItem);
                moveToTopMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_REORDER_MOVE_TO_TOP));
                moveToTopMenuItem.setIcon(null);
                rearrangeMenu.add(moveToTopMenuItem);
                moveToBottomMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_REORDER_MOVE_TO_BOTTOM));
                moveToBottomMenuItem.setIcon(null);
                rearrangeMenu.add(moveToBottomMenuItem);                
                        
            setViewportMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_SET_VIEWPORT));
            setViewportMenuItem.setIcon(null);
            hideSequenceMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_HIDE_SEQUENCE));
            hideSequenceMenuItem.setIcon(null);
            hideOtherSequencesMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_HIDE_OTHER_SEQUENCES));
            hideOtherSequencesMenuItem.setIcon(null);

            selectAllMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_SELECT_ALL));
            selectAllMenuItem.setIcon(null);
            inverSelectionMenuItem=new JMenuItem(actionMap.get(SequenceVisualizer.ACTION_INVERT_SELECTION));
            inverSelectionMenuItem.setIcon(null);          
            
            this.add(hideSequenceMenuItem);
            this.add(hideOtherSequencesMenuItem);           
            this.add(selectAllMenuItem);
            this.add(inverSelectionMenuItem);
            
            this.add(setViewportMenuItem);
            this.add(zoomMenu);
            this.add(orientationMenu);
            this.add(alignmentMenu);
            this.add(rulerMenu);
            this.add(rearrangeMenu);
            
            ColorMenuListener colormenulistener=new ColorMenuListener() {
                  @Override
                  public void newColorSelected(Color color) {
                    if (color==null) return;
                    else SequenceVisualizer.setTrackBorderColor(color);   
                    gui.redraw();
                  }
            }; 
            ColorMenuListener boxcolormenulistener=new ColorMenuListener() {
                  @Override
                  public void newColorSelected(Color color) {
                    if (color==null) return;
                    else SequenceVisualizer.setBoundingBoxColor(color);
                    gui.redraw();
                  }
            };              
            ArrayList<Object[]> extraColors=new ArrayList<Object[]>();
            extraColors.add(new Object[]{"TRANSPARENT", new Color(0,0,0,0)});
            ColorMenu colorMenu=new ColorMenu(SET_BORDER_COLOR,colormenulistener, gui.getFrame(), extraColors);              
            this.add(colorMenu);
            ColorMenu boxcolorMenu=new ColorMenu(SET_BOX_COLOR,boxcolormenulistener, gui.getFrame(), extraColors);              
            this.add(boxcolorMenu);            
            
            JMenuItem cursorFlanksItem=new JMenuItem(SET_CURSOR_FLANKS);
            cursorFlanksItem.addActionListener(menuItemListener);
            this.add(cursorFlanksItem);            
        }
        

        
        
        /** Returns true if this popup menu is currently being displayed */
        public static boolean isPopupShowing() {
            if (thismenu!=null && thismenu.isVisible()) return true;
            else return false;
        }

         /**
         * An inner class that listens and responds to popup-menu items that are not wired to Actions
         */       
        private class MenuItemListener implements ActionListener {
           @Override
           public void actionPerformed(ActionEvent e) {
               //gui.debugMessage("Selected "+e.getActionCommand());                   
                       if (e.getActionCommand().equals(LABEL_ORIENTATION_FLIP_POINT)) {
                       gui.getVisualizationSettings().flipSequenceAroundFixedPoint(sequenceName,position);
                } else if (e.getActionCommand().equals(SequenceVisualizer.ACTION_ALIGN_TSS_HERE)) {
                       VisualizationPanel panel=sequenceVisualizer.getVisualizationPanel();
                       if (panel!=null) panel.alignAllSequencesToTSSHere(position);
                } else if (e.getActionCommand().equals(LABEL_RULER_TSS)) {
                       gui.getVisualizationSettings().setRuler(null,VisualizationSettings.RULER_TSS);
                } else if (e.getActionCommand().equals(LABEL_RULER_TES)) {
                       gui.getVisualizationSettings().setRuler(null,VisualizationSettings.RULER_TES);
                } else if (e.getActionCommand().equals(LABEL_RULER_UPSTREAM_END)) {
                       gui.getVisualizationSettings().setRuler(null,VisualizationSettings.RULER_UPSTREAM_END);
                } else if (e.getActionCommand().equals(LABEL_RULER_DOWNSTREAM_END)) {
                       gui.getVisualizationSettings().setRuler(null,VisualizationSettings.RULER_DOWNSTREAM_END);
                } else if (e.getActionCommand().equals(LABEL_RULER_CHROMOSOME)) {
                       gui.getVisualizationSettings().setRuler(null,VisualizationSettings.RULER_CHROMOSOME);
                } else if (e.getActionCommand().equals(LABEL_RULER_FIXED)) {
                       gui.getVisualizationSettings().setRuler(null,VisualizationSettings.RULER_FIXED);
                } else if (e.getActionCommand().equals(LABEL_RULER_NONE)) {
                       gui.getVisualizationSettings().setRuler(null,VisualizationSettings.RULER_NONE);
                } else if (e.getActionCommand().equals(HIDE_TRACK)) {
                       gui.getVisualizationSettings().setTrackVisible(trackName, false);
                } else if (e.getActionCommand().equals(EXPAND_TRACK)) {
                       gui.getVisualizationSettings().setExpanded(trackName, true);
                } else if (e.getActionCommand().equals(CONTRACT_TRACK)) {
                       gui.getVisualizationSettings().setExpanded(trackName, false);
                } else if (e.getActionCommand().equals(EDIT_REGION)) {
                       sequenceVisualizer.editRegion(currentRegion);
                } else if (e.getActionCommand().equals(DELETE_REGION)) {
                       gui.updatePartialDataItem(trackName, sequenceName, currentRegion, null);
                } else if (e.getActionCommand().equals(SET_CURSOR_FLANKS)) {
                       setCursorFlanks();
                } 
           }            
        }
  
        /**
         * An inner class that listens to popup-menu events related to the operations submenu and notifies the gui events
         */
        @SuppressWarnings("unchecked")
        private class OperationsMenuListener implements ActionListener {
           public void actionPerformed(ActionEvent e) {
                MotifLabEngine engine=gui.getEngine();
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
                    if (dataset!=null) { 
                        String[] proxys=analysis.getSourceProxyParameters();
                        String proxyparameter=null;
                        for (String proxy:proxys) {
                            Parameter proxysource=analysis.getParameterWithName(proxy);
                            Class proxytype=proxysource.getType();
                            if (proxytype.isAssignableFrom(dataset.getClass())) {proxyparameter=proxy;break;} // assign to first applicable parameter
                        }
                        if (proxyparameter!=null) parametersettings.setParameter(proxyparameter, dataset.getName());
                    }
                    parameters.setParameter(Operation_analyze.PARAMETERS, parametersettings);
                    gui.launchOperationEditor(parameters);                   
                }
           }            
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
        
        private void setCursorFlanks() {
           int flanks=SequenceVisualizer.getCursorFlanks();
           String newString = JOptionPane.showInputDialog("Specify cursor flank distance (in bp)", ""+flanks);
           if (newString==null) return;
           try {
               int newFlanks=Integer.parseInt(newString.trim());
               SequenceVisualizer.setCursorFlanks(newFlanks);
               repaint();
           } catch (Exception e) {}
        }          
        
}