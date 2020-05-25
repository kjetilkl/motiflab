/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import motiflab.engine.data.Data;
import motiflab.engine.data.Sequence;


/**
 *
 * @author kjetikl
 */
public class SequenceLabelContextMenu extends JPopupMenu  {
    
    private static final String DISPLAY="Display";
    private static final String SET_COLOR="Set Label Color";
    private static final String ALIGN_MENU="Align Labels";
    private static final String ALIGN_FRONT_TOP="Top";
    private static final String ALIGN_FRONT_MIDDLE="Middle";
    private static final String ALIGN_BEHIND="Behind";    
    private static final String MARK_SEQUENCE="Mark Sequence";    
    private static final String REMOVE_MARK="Remove Mark";    
    private static final String REMOVE_ALL_MARKS="Clear All Marks";    
    private static final String ADD_TO_COLLECTION="Add To Collection";    
    private static final String HIDE_SEQUENCE="Hide Sequence";    
    private static final String SHOW_ONLY_THIS_SEQUENCE="Show Only This Sequence";    


    //private static FeatureSequenceData sequenceData;
    private static MotifLabGUI gui;
    private static String sequenceName=null;
    private static SequenceLabelContextMenu thismenu=null;
    private static SequenceVisualizer sequenceVisualizer;

    
    public static SequenceLabelContextMenu getInstance(SequenceVisualizer sequencevisualizer, MotifLabGUI GUI) {
         sequenceVisualizer=sequencevisualizer;
         gui=GUI;
         sequenceName=sequenceVisualizer.getSequenceName();
         thismenu=new SequenceLabelContextMenu();         
         return thismenu;
    }
    
    
        private SequenceLabelContextMenu() {
            MenuItemListener menuItemListener=new MenuItemListener();

            ColorMenuListener colormenulistener=new ColorMenuListener() {
                  @Override
                  public void newColorSelected(Color color) {
                    if (color==null) return;
                    gui.getVisualizationSettings().setSequenceLabelColor(sequenceName,color);   
                    sequenceVisualizer.repaint();
                  }
            };           
            ColorMenu colorMenu=new ColorMenu(SET_COLOR,colormenulistener,gui.getFrame());   
            
            int alignment=gui.getVisualizationSettings().getSequenceLabelAlignment();            
            JMenu alignMenu=new JMenu(ALIGN_MENU);
            JCheckBoxMenuItem alignTopMenuItem=new JCheckBoxMenuItem(ALIGN_FRONT_TOP, alignment==SwingConstants.TOP);
            JCheckBoxMenuItem alignMiddleMenuItem=new JCheckBoxMenuItem(ALIGN_FRONT_MIDDLE, alignment==SwingConstants.CENTER);
            //JCheckBoxMenuItem alignBehindMenuItem=new JCheckBoxMenuItem(ALIGN_BEHIND, alignment==SwingConstants.TRAILING);            
            
            JMenuItem hideSequenceMenuItem=new JMenuItem(HIDE_SEQUENCE);
            JMenuItem hideOthersMenuItem=new JMenuItem(SHOW_ONLY_THIS_SEQUENCE);
            
            boolean marked=gui.getVisualizationSettings().isSequenceMarked(sequenceName);
            JMenuItem markMenuItem=new JMenuItem((marked)?REMOVE_MARK:MARK_SEQUENCE);
            JMenuItem clearMarksMenuItem=new JMenuItem(REMOVE_ALL_MARKS);
            
            alignTopMenuItem.addActionListener(menuItemListener);            
            alignMiddleMenuItem.addActionListener(menuItemListener);    
            hideOthersMenuItem.addActionListener(menuItemListener);    
            hideSequenceMenuItem.addActionListener(menuItemListener);    
            markMenuItem.addActionListener(menuItemListener);    
            clearMarksMenuItem.addActionListener(menuItemListener);    
            
            alignMenu.add(alignTopMenuItem);
            alignMenu.add(alignMiddleMenuItem);
            //alignMenu.add(alignBehindMenuItem); // functionality for this option has not been implemented           
            
            JMenuItem displayMenuItem=new JMenuItem(DISPLAY+" "+sequenceName);
            displayMenuItem.addActionListener(menuItemListener);    
            
            this.add(displayMenuItem);
            this.add(new JSeparator());
            this.add(hideSequenceMenuItem);
            this.add(hideOthersMenuItem);
            this.add(markMenuItem);
            this.add(clearMarksMenuItem);     
            this.add(colorMenu);
            this.add(alignMenu);         
            this.add(new ExternalDBLinkMenu(sequenceName, gui));
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
                       if (e.getActionCommand().equals(ALIGN_FRONT_TOP)) {
                       gui.getVisualizationSettings().setSequenceLabelAlignment(SwingConstants.TOP);
                } else if (e.getActionCommand().equals(ALIGN_FRONT_MIDDLE)) {
                       gui.getVisualizationSettings().setSequenceLabelAlignment(SwingConstants.CENTER);
                } else if (e.getActionCommand().equals(ALIGN_BEHIND)) {
                       gui.getVisualizationSettings().setSequenceLabelAlignment(SwingConstants.TRAILING);
                } else if (e.getActionCommand().equals(MARK_SEQUENCE)) {
                       gui.getVisualizationSettings().setSequenceMarked(sequenceName, true);
                       sequenceVisualizer.repaint();
                } else if (e.getActionCommand().equals(REMOVE_MARK)) {
                       gui.getVisualizationSettings().setSequenceMarked(sequenceName, false);
                       sequenceVisualizer.repaint();
                } else if (e.getActionCommand().equals(REMOVE_ALL_MARKS)) {
                       gui.getVisualizationSettings().clearSequenceMarks();
                       gui.redraw();
                } else if (e.getActionCommand().equals(HIDE_SEQUENCE)) {
                       gui.getVisualizationSettings().setSequenceVisible(sequenceName,false);
                       //gui.redraw();
                } else if (e.getActionCommand().equals(SHOW_ONLY_THIS_SEQUENCE)) {
                       setVisibilityOnAllSequences(false);
                       VisualizationSettings settings=gui.getVisualizationSettings();
                       settings.setSequenceVisible(sequenceName, true);
                } else if (e.getActionCommand().startsWith(DISPLAY)) {
                       Data data=gui.getEngine().getDataItem(sequenceName);
                       if (data instanceof Sequence) gui.showPrompt(data, true, true);
                } 
           }            
        }
        
       private void setVisibilityOnAllSequences(boolean show) {
           VisualizationSettings settings=gui.getVisualizationSettings();
           ArrayList<Data> allSequences=gui.getEngine().getAllDataItemsOfType(Sequence.class);
           String[] sequenceNames=new String[allSequences.size()];
           for (int i=0;i<sequenceNames.length;i++) sequenceNames[i]=allSequences.get(i).getName();
           settings.setSequenceVisible(sequenceNames,show);
       }
   
  

        
}