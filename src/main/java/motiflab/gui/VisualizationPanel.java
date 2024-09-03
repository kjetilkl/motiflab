/*
 * VisualizationPanel.java
 *
 * Created on 7. oktober 2008, 14:43
 */

package motiflab.gui;



import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.print.PageFormat;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import javax.swing.Action;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentListener;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.RepaintManager;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Data;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.Region;
import sun.swing.PrintingStatus;
import sun.swing.SwingUtilities2;

/**
 * This class represents the main panel shown in the "Visualization" tab in the GUI.
 * It is responsible for visualizing all the sequences and datatracks (through division of labour
 * to classes such as SequenceVisualizer which draws all chosen tracks for a single sequence).
 * 
 * @author  kjetikl
 */
public class VisualizationPanel extends javax.swing.JScrollPane implements VisualizationSettingsListener, MouseWheelListener, Printable, Searchable {
    private Action zoomInAction;
    private Action zoomOutAction;
    private Action zoomResetAction;
    private Action zoomToFitAction;
    private Action zoomToCustomAction;
    private Action moveLeftAction;
    private Action moveToLeftEndAction;
    private Action moveRightAction;
    private Action moveToRightEndAction;
    private Action flipOrientationAction;
    private Action directOrientationAction;
    private Action reverseOrientationAction;
    private Action fromGeneOrientationAction;
    private Action alignRightAction;
    private Action alignLeftAction;
    private Action alignNoneAction;
    private Action alignTSSHereAction;
    private Action alignTSSRightAction;
    private Action constrainOnAction;
    private Action constrainOffAction;
    private Action scrollLockOnAction;
    private Action scrollLockOffAction;
    private Action selectAllAction;
    private Action invertSelectionAction;    

    public static final String ACTION_SCROLL_LOCK_ON = "Turn On Scroll Lock";
    public static final String ACTION_SCROLL_LOCK_OFF = "Turn Off Scroll Lock";
    public static final String ACTION_CONSTRAIN_ON = "Turn On Movement Constraint";
    public static final String ACTION_CONSTRAIN_OFF = "Turn Off Movement Constraint";
    public static final String ACTION_DELETE_REGION="Delete Region";
    public static final String ACTION_EDIT_REGION="Edit Region";
    
    
    private VisualizationSettings settings;
    private JPanel mainVisualizationPanel; // an inner panel that will be placed inside a JScrollPanel. This is where all the stuff goes!
    private ArrayList<SequenceVisualizer> sequenceVisualizers; // this list contains all SequenceVisualizer for all current sequences (visible or hidden)
    private HashSet<String> dirtySequenceVisualizers=new HashSet<>(); // if a sequence is included here, the SequenceVisualizer for this sequence must be created anew 
    private ComponentListener resizeListener; 
    private ArrayList<PanelDecorator> panelDecorators=null;
    private ArrayList<PanelListener> panelListeners=null;
    private boolean isDirty=false; // if this flag is TRUE, a call to setupMainVisualizationPanel() must be performed before painting the component
    
    
    /** 
     * Creates new VisualizationPanel object
     * @param settings A reference to the singleton VisualizationSettings object
     */
    public VisualizationPanel(VisualizationSettings settings) {
        this.settings=settings;
        mainVisualizationPanel=new JPanel() {
            @Override
            public void paint(Graphics g) {  
                if (isDirty) {
                    setupMainVisualizationPanel();
                    isDirty=false;
                }
                super.paint(g);
                if (panelDecorators!=null && !panelDecorators.isEmpty()) {
                    for (PanelDecorator decorator:panelDecorators) {
                        if (decorator.getTargetClass()==VisualizationPanel.class && decorator.isActive()) decorator.draw((Graphics2D)g, mainVisualizationPanel);
                    }
                }
            }             
        };
        mainVisualizationPanel.setLayout(new BoxLayout(mainVisualizationPanel, BoxLayout.Y_AXIS));
        mainVisualizationPanel.setBackground(settings.getVisualizationPanelBackgroundColor());
        mainVisualizationPanel.setOpaque(true);    
        settings.addVisualizationSettingsListener(this);
        this.setViewportView(mainVisualizationPanel);
        sequenceVisualizers=new ArrayList<SequenceVisualizer>(0);
        setupMainVisualizationPanel(); 
        setupActions();
        this.setFocusCycleRoot(true);
        this.setWheelScrollingEnabled(false); // the wheel will be used for zooming when CTRL is down. So we handle mousewheelscrolling manually!
        this.addMouseWheelListener(this);        
        // uncomment next lines to allow width of sequencewindow to scale with MotifLab window (experimental feature)        
//        resizeListener=new ComponentAdapter() {
//            int oldWidth=0;     
//            int storedseqwindowsize=VisualizationPanel.this.settings.getSequenceWindowSize();
//            @Override
//            public void componentResized(ComponentEvent e) {
//                int newWidth=getWidth();                
//                if (newWidth!=oldWidth) {
//                    int resize=newWidth-oldWidth; // positive if new width is larger, negative if new width is smaller
//                    int seqwindowsize=VisualizationPanel.this.settings.getSequenceWindowSize();
//                    if (storedseqwindowsize<100) {
//                        storedseqwindowsize+=resize;
//                        if (storedseqwindowsize>=100) seqwindowsize=storedseqwindowsize;
//                        else seqwindowsize=100;
//                    } else {
//                       seqwindowsize+=resize;
//                       storedseqwindowsize=seqwindowsize;
//                       if (seqwindowsize<100) {seqwindowsize=100;}
//                    }
//                    oldWidth=newWidth;
//                    VisualizationPanel.this.settings.setSequenceWindowSize(seqwindowsize);
//                }
//            }       
//        };
//        this.addComponentListener(resizeListener);

        RedispatchingMouseAdapter mouseEventDispatcher=new RedispatchingMouseAdapter();
        mainVisualizationPanel.addMouseListener(mouseEventDispatcher);
        mainVisualizationPanel.addMouseMotionListener(mouseEventDispatcher);      
        
    }
    
    public JPanel getMainVisualizationPanel() {
        return mainVisualizationPanel;
    }
  
    public final void addPanelDecorator(PanelDecorator decorator) {
        // make a new list and swap with the original to avoid interfering with others that might be iterating through the current list at this point
        ArrayList<PanelDecorator> newlist=new ArrayList<>((panelDecorators==null)?1:panelDecorators.size()+1);
        if (panelDecorators!=null && !panelDecorators.isEmpty()) newlist.addAll(panelDecorators);
        newlist.add(decorator);
        panelDecorators=newlist;
    }
    
    public final void removePanelDecorator(PanelDecorator decorator) {
        if (panelDecorators==null || panelDecorators.isEmpty()) return;
        if (panelDecorators.size()==1 && panelDecorators.contains(decorator)) {panelDecorators=null;return;} // last decorator removed. Just drop whole list
        // make a new list and swap with the original to avoid interfering with others that might be iterating through the current list at this point
        ArrayList<PanelDecorator> newlist=new ArrayList<>(panelDecorators.size());
        newlist.addAll(panelDecorators);
        newlist.remove(decorator);
        panelDecorators=newlist;
    }    
    
    /** Returns the current list of registered PanelDecorators (not a copy!)
     *  Note: Do not modify this list! Use addPanelDecorator() and removePanelDecorator() to modify it
     */
    public final List<PanelDecorator> getPanelDecorators() {
        return panelDecorators;
    }
    
    
    /** Adds a panel listener that can respond to key pressed events and context menu events on sequences (and their sequence labels) */
    public void addPanelListener(PanelListener listener) {
        if (panelListeners==null) panelListeners=new ArrayList<PanelListener>();
        panelListeners.add(listener);    
    }

    public void removePanelListener(PanelListener listener) {
        if (panelListeners!=null) panelListeners.remove(listener);
    }      
    
    /** Notify panel listeners of a Key pressed event */
    public void notifyPanelListeners(Object panelsource, Data[] data, KeyEvent e) {
        if (panelListeners!=null) {
            for (PanelListener listener:panelListeners) {
                listener.keyPressed(panelsource, data, e);
            }
        }
    }
    
    /** Notify panel listeners of a show context menu event */
    public void notifyPanelListeners(Object panelsource, Data[] data, JPopupMenu popup) {
        if (panelListeners!=null) {
            for (PanelListener listener:panelListeners) {
                listener.showContextMenu(panelsource, data, popup);
            }
        }
    }    
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // This method is either called "directly" or by mousewheel-listening trackvisualizers in SequenceVisualizer which report back here
        if (!(e.isControlDown() || e.isShiftDown())) {
          if (e.getScrollType()==MouseWheelEvent.WHEEL_UNIT_SCROLL) {
              JViewport vp=VisualizationPanel.this.getViewport();
              int units=e.getUnitsToScroll()*5; // scale by a factor to make it a little faster
              Point p=vp.getViewPosition();
              p.y+=units;
              p.y = Math.max(0, p.y);
              p.y = Math.min((vp.getView().getHeight()-vp.getHeight()), p.y);
              vp.setViewPosition(p);
          }
        }
    }

    private boolean blockMouseEvents=false;
    
    /**
     * If set to TRUE, children of this component (e.g. SequenceVisualizers) 
     * should not respond to mouse events but just pass them on
     * @param blocked 
     */
    public void setMouseEventsBlocked(boolean blocked) {
        blockMouseEvents=blocked;
    }    
    
    /** This returns TRUE if children of this component (e.g. SequenceVisualizers) 
     * should not respond to mouse events but just pass them on
     */
    public boolean isMouseEventsBlocked() {
        return blockMouseEvents;
    }       
    
    @Override
    public String toString() {return "VizPanel";}

    private void setupActions() {
        zoomInAction=new AbstractAction(SequenceVisualizer.ACTION_ZOOM_IN, new InfoPanelIcon(InfoPanelIcon.ZOOM_IN_ICON)) {
            public void actionPerformed(ActionEvent e) {zoomInOnAllSequences();} 
        };
        zoomOutAction=new AbstractAction(SequenceVisualizer.ACTION_ZOOM_OUT, new InfoPanelIcon(InfoPanelIcon.ZOOM_OUT_ICON)) {
            public void actionPerformed(ActionEvent e) {zoomOutOnAllSequences();} 
        };
        zoomResetAction=new AbstractAction(SequenceVisualizer.ACTION_ZOOM_RESET, new InfoPanelIcon(InfoPanelIcon.ZOOM_RESET_ICON)) {
            public void actionPerformed(ActionEvent e) {zoomResetOnAllSequences();} 
        };
        zoomToFitAction=new AbstractAction(SequenceVisualizer.ACTION_ZOOM_TO_FIT, new InfoPanelIcon(InfoPanelIcon.ZOOM_TO_FIT_ICON)) {
            public void actionPerformed(ActionEvent e) {zoomToFitOnAllSequences();} 
        };
        zoomToCustomAction=new AbstractAction(SequenceVisualizer.ACTION_ZOOM_TO_CUSTOM_LEVEL, new InfoPanelIcon(InfoPanelIcon.ZOOM_ICON)) {
            public void actionPerformed(ActionEvent e) {zoomToCustomOnAllSequences();} 
        };
        
        moveLeftAction=new AbstractAction(SequenceVisualizer.ACTION_MOVE_LEFT, new InfoPanelIcon(InfoPanelIcon.LEFT_ICON)) {
            public void actionPerformed(ActionEvent e) {moveAllSequencesLeft(SequenceVisualizer.moveIncrement);} 
        };
        moveRightAction=new AbstractAction(SequenceVisualizer.ACTION_MOVE_RIGHT, new InfoPanelIcon(InfoPanelIcon.RIGHT_ICON)) {
            public void actionPerformed(ActionEvent e) {moveAllSequencesRight(SequenceVisualizer.moveIncrement);} 
        };
        moveToLeftEndAction=new AbstractAction(SequenceVisualizer.ACTION_MOVE_TO_LEFT_END, new InfoPanelIcon(InfoPanelIcon.LEFT_END_ICON)) {
            public void actionPerformed(ActionEvent e) {moveAllSequencesToLeftEnd();} 
        };
        moveToRightEndAction=new AbstractAction(SequenceVisualizer.ACTION_MOVE_TO_RIGHT_END, new InfoPanelIcon(InfoPanelIcon.RIGHT_END_ICON)) {
            public void actionPerformed(ActionEvent e) {moveAllSequencesToRightEnd();} 
        };
        
        flipOrientationAction=new AbstractAction(SequenceVisualizer.ACTION_ORIENTATION_FLIP, new InfoPanelIcon(InfoPanelIcon.FLIP_ICON)) {
            public void actionPerformed(ActionEvent e) {flipOrientationOnAllSequences();} 
        };
        directOrientationAction=new AbstractAction(SequenceVisualizer.ACTION_ORIENTATION_DIRECT, null) {
             public void actionPerformed(ActionEvent e) {showDirectStrandOnAllSequences();} 
        };
        reverseOrientationAction=new AbstractAction(SequenceVisualizer.ACTION_ORIENTATION_REVERSE, null) {
            public void actionPerformed(ActionEvent e) {showReverseStrandOnAllSequences();} 
        };
        fromGeneOrientationAction=new AbstractAction(SequenceVisualizer.ACTION_ORIENTATION_RELATIVE, null) {
             public void actionPerformed(ActionEvent e) {showStrandFromGeneOnAllSequences();} 
        };

        alignLeftAction=new AbstractAction(SequenceVisualizer.ACTION_ALIGN_LEFT, new InfoPanelIcon(InfoPanelIcon.ALIGN_LEFT_ICON)) {
            public void actionPerformed(ActionEvent e) {alignAllSequencesLeft();} 
        };
        alignRightAction=new AbstractAction(SequenceVisualizer.ACTION_ALIGN_RIGHT, new InfoPanelIcon(InfoPanelIcon.ALIGN_RIGHT_ICON)) {
             public void actionPerformed(ActionEvent e) {alignAllSequencesRight();} 
        };
        alignTSSRightAction=new AbstractAction(SequenceVisualizer.ACTION_ALIGN_TSS_RIGHT, new InfoPanelIcon(InfoPanelIcon.ALIGN_TSS_ICON)) {
             public void actionPerformed(ActionEvent e) {alignAllSequencesToTSSRight();} 
        };
        alignNoneAction=new AbstractAction(SequenceVisualizer.ACTION_ALIGN_NONE, new InfoPanelIcon(InfoPanelIcon.ALIGN_NONE_ICON)) {
            public void actionPerformed(ActionEvent e) {alignAllSequencesNone();} 
        };
              
        constrainOnAction=new AbstractAction(ACTION_CONSTRAIN_ON, new InfoPanelIcon(InfoPanelIcon.CONSTRAIN_ICON)) { 
            public void actionPerformed(ActionEvent e) { setConstrainOnAllSequences(true);} 
        };
        constrainOffAction=new AbstractAction(ACTION_CONSTRAIN_OFF, new InfoPanelIcon(InfoPanelIcon.CONSTRAIN_ICON)) { 
            public void actionPerformed(ActionEvent e) { setConstrainOnAllSequences(false);} 
        };
        scrollLockOnAction=new AbstractAction(ACTION_SCROLL_LOCK_ON, new InfoPanelIcon(InfoPanelIcon.LOCK_ICON)) { 
            public void actionPerformed(ActionEvent e) { setScrollLockOnAllSequences(true);} 
        };
        scrollLockOffAction=new AbstractAction(ACTION_SCROLL_LOCK_OFF, new InfoPanelIcon(InfoPanelIcon.LOCK_ICON)) {
            public void actionPerformed(ActionEvent e) { setScrollLockOnAllSequences(false);}
        };
        selectAllAction=new AbstractAction(SequenceVisualizer.ACTION_SELECT_ALL,null) {
            public void actionPerformed(ActionEvent e) { selectAll();}
        }; 
        invertSelectionAction=new AbstractAction(SequenceVisualizer.ACTION_INVERT_SELECTION,null) {
            public void actionPerformed(ActionEvent e) { invertSelections();}
        };        
            
        InputMap inputmap=this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionmap=this.getActionMap();
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_IN);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_IN);
        actionmap.put(SequenceVisualizer.ACTION_ZOOM_IN, zoomInAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_OUT);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_OUT);
        actionmap.put(SequenceVisualizer.ACTION_ZOOM_OUT, zoomOutAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_RESET);
        actionmap.put(SequenceVisualizer.ACTION_ZOOM_RESET, zoomResetAction);
          
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_TO_FIT);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_TO_FIT);
        actionmap.put(SequenceVisualizer.ACTION_ZOOM_TO_FIT, zoomToFitAction);
                        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ZOOM_TO_CUSTOM_LEVEL);
        actionmap.put(SequenceVisualizer.ACTION_ZOOM_TO_CUSTOM_LEVEL, zoomToCustomAction);
        
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_MOVE_LEFT);
        actionmap.put(SequenceVisualizer.ACTION_MOVE_LEFT, moveLeftAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_MOVE_RIGHT);
        actionmap.put(SequenceVisualizer.ACTION_MOVE_RIGHT, moveRightAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK), SequenceVisualizer.ACTION_MOVE_TO_LEFT_END);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), SequenceVisualizer.ACTION_MOVE_TO_LEFT_END);
        actionmap.put(SequenceVisualizer.ACTION_MOVE_TO_LEFT_END, moveToLeftEndAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK), SequenceVisualizer.ACTION_MOVE_TO_RIGHT_END);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), SequenceVisualizer.ACTION_MOVE_TO_RIGHT_END);
        actionmap.put(SequenceVisualizer.ACTION_MOVE_TO_RIGHT_END, moveToRightEndAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ORIENTATION_FLIP);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ORIENTATION_FLIP);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ORIENTATION_FLIP);
        actionmap.put(SequenceVisualizer.ACTION_ORIENTATION_FLIP, flipOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ORIENTATION_DIRECT);
        actionmap.put(SequenceVisualizer.ACTION_ORIENTATION_DIRECT, directOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ORIENTATION_REVERSE);
        actionmap.put(SequenceVisualizer.ACTION_ORIENTATION_REVERSE, reverseOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_ORIENTATION_RELATIVE);
        actionmap.put(SequenceVisualizer.ACTION_ORIENTATION_RELATIVE, fromGeneOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.ALT_DOWN_MASK), ACTION_CONSTRAIN_ON);
        actionmap.put(ACTION_CONSTRAIN_ON, constrainOnAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.ALT_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK), ACTION_CONSTRAIN_OFF);
        actionmap.put(ACTION_CONSTRAIN_OFF, constrainOffAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK), ACTION_SCROLL_LOCK_ON);
        actionmap.put(ACTION_SCROLL_LOCK_ON, scrollLockOnAction); 
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK), ACTION_SCROLL_LOCK_OFF);
        actionmap.put(ACTION_SCROLL_LOCK_OFF, scrollLockOffAction);  
        
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), SequenceVisualizer.ACTION_ALIGN_LEFT);        
        actionmap.put(SequenceVisualizer.ACTION_ALIGN_LEFT, alignLeftAction);  
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), SequenceVisualizer.ACTION_ALIGN_RIGHT); 
        actionmap.put(SequenceVisualizer.ACTION_ALIGN_RIGHT, alignRightAction);  

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), SequenceVisualizer.ACTION_ALIGN_NONE); 
        actionmap.put(SequenceVisualizer.ACTION_ALIGN_NONE, alignNoneAction);  

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), SequenceVisualizer.ACTION_ALIGN_TSS_RIGHT);         
        actionmap.put(SequenceVisualizer.ACTION_ALIGN_TSS_RIGHT, alignTSSRightAction);  
        
        actionmap.put(SequenceVisualizer.ACTION_ALIGN_TSS_HERE, alignTSSHereAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), ACTION_DELETE_REGION);
        actionmap.put(ACTION_DELETE_REGION, new AbstractAction() {public void actionPerformed(ActionEvent e) {deleteCurrentRegion();} });

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), ACTION_EDIT_REGION);
        actionmap.put(ACTION_EDIT_REGION, new AbstractAction() {public void actionPerformed(ActionEvent e) {editCurrentRegion();} });

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_SELECT_ALL);
        actionmap.put(SequenceVisualizer.ACTION_SELECT_ALL, selectAllAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK), SequenceVisualizer.ACTION_INVERT_SELECTION);
        actionmap.put(SequenceVisualizer.ACTION_INVERT_SELECTION, invertSelectionAction);
 
    }  

    /** This method can be used to enable or disable all actions registered in this VisualizationPanel */
    public void setEnabledOnAllActions(boolean enabled) {
        ActionMap actionmap=this.getActionMap();
        Object[] keys=actionmap.keys();
        for (Object key:keys) actionmap.get(key).setEnabled(enabled);
    }



    /** Removes the currently selected region (if there is one)
     *  This method will find the region identified by the current mouse pointer in any track in any sequence
     */
    private void deleteCurrentRegion() {
        Region current=getCurrentRegion();
        if (current!=null) {           
            FeatureSequenceData seq=current.getParent();
            if (seq==null) return;                     
            FeatureDataset dataset=seq.getParent();
            if (dataset!=null) {
                settings.getGUI().updatePartialDataItem(dataset.getName(),seq.getName(), current, null);
                settings.getGUI().logMessage("Deleted region = "+current.getType()+"  [ "+current.getChromosomalLocationAsString()+" ("+current.getOrientationAsString()+") ] in "+seq.getName());   
            }
        }
    }

    /** Edits the currently selected region (if there is one)
     *  This method will find the region identified by the current mouse pointer in any track in any sequence
     */
    private void editCurrentRegion() {
        Region current=getCurrentRegion();
        if (current!=null) {
             FeatureSequenceData seq=current.getParent();
             if (seq==null) return;
             SequenceVisualizer viz=getSequenceVisualizer(seq.getSequenceName());
             if (viz!=null) viz.editRegion(current);
        }
    }

    /** Returns the currently selected region
     *  This method will find the region identified by the current mouse pointer in any track in any sequence
     *  @return The region which is currently pointed at by the mouse or NULL if no such Region is found
     */
    public Region getCurrentRegion() {
        Point point=MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(point, mainVisualizationPanel);
        Component comp=mainVisualizationPanel.getComponentAt(point);
        if (comp instanceof SequenceVisualizer) {
           Point relative=SwingUtilities.convertPoint(mainVisualizationPanel, point, comp);
           DataTrackVisualizer track=((SequenceVisualizer)comp).getTrackVisualizer(relative);
           if (track!=null) {
              if (track instanceof DataTrackVisualizer_Region) {
                  Region reg=((DataTrackVisualizer_Region)track).getCurrentRegion();
                  return reg;
              }
           }
        } 
        return null;
    }
  

    /** Returns the currently selected FeatureSequenceData (sequence within a featuredataset)
     *  This method will find the track identified by the current mouse pointer in any track in any sequence
     *  and return the corresponding FeatureSequenceData object
     *  @return The FeatureSequenceData of the track which is currently pointed at by the mouse or NULL if no such track is found
     */
    public FeatureSequenceData getCurrentFeatureSequenceData() {
        Point point=MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(point, mainVisualizationPanel);
        Component comp=mainVisualizationPanel.getComponentAt(point);
        if (comp instanceof SequenceVisualizer) {
           Point relative=SwingUtilities.convertPoint(mainVisualizationPanel, point, comp);
           DataTrackVisualizer track=((SequenceVisualizer)comp).getTrackVisualizer(relative);
           if (track!=null) {
              return track.sequencedata;
           }
        }
        return null;
    }
    
    public DataTrackVisualizer getTrackVisualizer(String FeatureDatasetName, String sequenceName) {
        SequenceVisualizer seqviz=getSequenceVisualizer(sequenceName);
        if (seqviz==null) return null;
        DataTrackVisualizer trackviz=seqviz.getTrackVisualizer(FeatureDatasetName);
        return trackviz;
    }

    /**
     * Sets up the main visualization panel. 
     * This should be called every time a new Feature Dataset (or a single sequence) 
     * is added, removed or reordered in order to update the visualization
     */
    @SuppressWarnings("unchecked")
    private void setupMainVisualizationPanel() {
        // settings.getGUI().logMessage("setupMainVisualizationPanel()");
        SequenceVisualizer focusedSequence=null;
        ArrayList<SequenceVisualizer> tempList=(ArrayList<SequenceVisualizer>)sequenceVisualizers.clone(); // make a copy of the current list of SequenceVisualizer
        for (SequenceVisualizer viz:tempList) if (viz.hasFocus()) {focusedSequence=viz;break;} // remember which sequence that currently has focus
        sequenceVisualizers=new ArrayList<SequenceVisualizer>();
        mainVisualizationPanel.removeAll();
        int maxlabelwidth=0;
        MotifLabEngine engine=settings.getEngine();
        String defaultAlignment=settings.getDefaultSequenceAlignment(); 
        SequenceCollection dataset=engine.getDefaultSequenceCollection();        
        int size=dataset.getNumberofSequences();       
        for (int i=0;i<size;i++) {
                Sequence sequence=(Sequence)dataset.getSequenceByIndex(i,engine);
                if (sequence==null) continue;
                String sequenceName=sequence.getName();
                SequenceVisualizer viz=(isSequenceVisualizerDirty(sequenceName))?null:getSequenceVisualizerFromList(sequenceName,tempList);
                if (viz==null) {
                    viz=new SequenceVisualizer(sequenceName,settings);                
                    viz.alignSequence(defaultAlignment,false);
                    setSequenceVisualizerDirty(sequenceName,false); // remove dirty flag from this SequenceVisualizer
                } 
                int labelwidth=viz.getRenderedLabelWidth();
                if (labelwidth>maxlabelwidth) maxlabelwidth=labelwidth;
                sequenceVisualizers.add(viz);
                mainVisualizationPanel.add(viz);
                viz.setVisible(settings.isSequenceVisible(sequenceName));
                if (viz==focusedSequence) viz.requestFocusInWindow();    
                viz.setDirty(); //
        }
        settings.setSequenceLabelMaxWidth(maxlabelwidth);
        settings.rearrangeDataTracks();
        mainVisualizationPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
    }
    
    private void setSequenceVisualizerDirty(String sequencename, boolean dirty) {
        if (dirty) dirtySequenceVisualizers.add(sequencename);
        else dirtySequenceVisualizers.remove(sequencename);
    }
    
    private boolean isSequenceVisualizerDirty(String sequencename) {
        return dirtySequenceVisualizers.contains(sequencename);
    }
    
    /**
     * Updates the order of all SequenceVisualizers so that they reflect the order provided in the default sequence collection
     * It does not update the SequenceVisualizers themselves
     */
    private void updateSequenceOrder() {  
        SequenceVisualizer focusedSequence=null;
        HashMap<String,SequenceVisualizer> map=new HashMap<String,SequenceVisualizer>();
        for (SequenceVisualizer viz:sequenceVisualizers) {
            if (viz.hasFocus()) focusedSequence=viz;
            map.put(viz.getSequenceName(),viz);
        }
        mainVisualizationPanel.removeAll();
        MotifLabEngine engine=settings.getEngine();
        SequenceCollection dataset=engine.getDefaultSequenceCollection();        
        int size=dataset.getNumberofSequences();       
        for (int i=0;i<size;i++) {
            String sequenceName=dataset.getSequenceNameByIndex(i);
            SequenceVisualizer viz=map.get(sequenceName);
            if (viz==null) {setupMainVisualizationPanel();return;} // this could happen only if this method is called out of order. Abort and perform regular setup
            mainVisualizationPanel.add(viz);
            // viz.setVisible(settings.isSequenceVisible(sequenceName));
            if (viz==focusedSequence) viz.requestFocusInWindow();                
        }
    }    
    
    
    /**
     * Reorders a single SequenceVisualizer
     * @param index
     * @param newindex 
     */
    public void reorderSequences(int index, int newindex) {
        if (newindex<0 || newindex>mainVisualizationPanel.getComponentCount()) return; // do not move across edge
        SequenceVisualizer seqviz=(SequenceVisualizer)mainVisualizationPanel.getComponent(index);
        mainVisualizationPanel.remove(index);
        mainVisualizationPanel.add(seqviz, newindex);
        mainVisualizationPanel.revalidate();
    }
    
    /** Goes through each sequence and updates its setVisible() flag based on the current VisualizationSettings */
    private void updateSequenceVisibility() {
        for (SequenceVisualizer seqviz:sequenceVisualizers) {
            seqviz.setVisible(settings.isSequenceVisible(seqviz.getSequenceName()));
        }      
    }



    /** 
     * Searches a given list of SequenceVisualizers for one that corresponds to the given name
     * If found this SequenceVisualizer is returned else NULL is returned
     */
    public SequenceVisualizer getSequenceVisualizerFromList(String sequenceName, ArrayList<SequenceVisualizer> list) {
        for (SequenceVisualizer viz:list) {
            if (viz.getSequenceName().equals(sequenceName)) return viz;
        }
        return null;
    }
    
    /** Returns a list of all SequenceVisualizers currently in use */
    public ArrayList<SequenceVisualizer> getSequenceVisualizers() {
        return sequenceVisualizers;
    }
    
    /** Returns the SequenceVisualizer corresponding to the given sequence or NULL of no such visualizer exists 
     * @param sequenceName
     * @return
     */
    public SequenceVisualizer getSequenceVisualizer(String sequenceName) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            if (viz.getSequenceName().equals(sequenceName)) return viz;
        }
        return null;
    }
    
    /** Removes all DataTrackVisualizers currently employed by each of the SequenceVisualizers, so that they will have to be created anew when needed */
    public void clearCachedVisualizers() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.clearCachedVisualizers();
         }       
    }
    
    
    public void repackRegions(FeatureDataset data) {
        settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.UPDATED, data);
    }
    
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the datatrack order is rearranged
    */
    @Override
    public void trackReorderEvent(int type, Data affected) {
        // not important here
    }
    
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the name of a dataitem is changed (by the user)
    */
    @Override
    public void dataRenamedEvent(String oldname,String newname) {
        // this event is not important here
    }
    
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the size of the sequence window is changed
    */
    @Override
    public void sequenceWindowSizeChangedEvent(int newsize, int oldsize) {
        // this event is not important here
    }
    
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the size of the sequence margin is changed
    */
    @Override
    public void sequenceMarginSizeChangedEvent(int newsize) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.setSequenceMarginWithoutRepaint(newsize);
        }
        mainVisualizationPanel.repaint();
    }
    
      
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the order of sequences or sequence sets is rearranged (or when the visibility is changed)
    */
    @Override
    public void sequencesLayoutEvent(final int type, final Data affected) {
        //
        //  Do not perform setupMainVisualizationPanel() unless this is absolutely necessary!!!!!!!!
        // 
        if (type==VisualizationSettingsListener.SCALE_CHANGED) return; // 
        //settings.getGUI().logMessage("VizPanel:sequencesLayoutEvent ("+type+") => "+affected);
        if (SwingUtilities.isEventDispatchThread()) {
               if (type==VisualizationSettingsListener.VISIBILITY_CHANGED) updateSequenceVisibility();
               else if (type==VisualizationSettingsListener.REORDERED && affected==settings.getGUI().getEngine().getDefaultSequenceCollection()) {
                   setupMainVisualizationPanel(); // sequences have been totally resorted
               } else if (affected instanceof Sequence && (type==VisualizationSettingsListener.REMOVED || type==VisualizationSettingsListener.UPDATED)) {
                   setSequenceVisualizerDirty(affected.getName(), true); // The sequence has changed. Discard the currently employed SeqViz so it will not be reused
               } else setupMainVisualizationPanel();
               validate();  
               mainVisualizationPanel.repaint();
        } else {
           Runnable runner = new Runnable() {
               public void run() {
                   if (type==VisualizationSettingsListener.VISIBILITY_CHANGED) updateSequenceVisibility();
                   else if (type==VisualizationSettingsListener.REORDERED && affected==settings.getGUI().getEngine().getDefaultSequenceCollection()) {
                       setupMainVisualizationPanel(); // sequences have been totally resorted
                   } else if (affected instanceof Sequence && (type==VisualizationSettingsListener.REMOVED || type==VisualizationSettingsListener.UPDATED)) {
                       setSequenceVisualizerDirty(affected.getName(), true); // The sequence has changed. Discard the currently employed SeqViz so it will not be reused
                   } else setupMainVisualizationPanel();
                   validate();
                   mainVisualizationPanel.repaint();
               }
           };
           SwingUtilities.invokeLater(runner);
        }        
    }
    
    @Override
    public void sequencesReordered(final Integer oldposition, final Integer newposition) {      
        if (oldposition!=null) { // a single sequence has changed place. This is handled by a direct call instead
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
               updateSequenceOrder();
               validate();  
               mainVisualizationPanel.repaint();
        } else {
           Runnable runner = new Runnable() {
               public void run() {
                   updateSequenceOrder(); 
                   validate(); 
                   mainVisualizationPanel.repaint();
               }
           };
           SwingUtilities.invokeLater(runner);
        }
    }    

    public void condensedModeChanged(boolean newmode) {
       if (SwingUtilities.isEventDispatchThread()) {
               setupMainVisualizationPanel();
               validate();
               mainVisualizationPanel.repaint();               
       } else {
           Runnable runner = new Runnable() {
               public void run() {
                   setupMainVisualizationPanel();
                   validate();
                   mainVisualizationPanel.repaint();
               }
           };
           SwingUtilities.invokeLater(runner);
      }          
    }
           
    
    /** 
     * Moves all sequences to the left. 
     */
    public void moveAllSequencesLeft(double fraction) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.moveSequenceLeft(fraction,false);
        }
        repaint();
    }   
    
    /** 
     * Moves all sequences to the right. 
     */
    public void moveAllSequencesRight(double fraction) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.moveSequenceRight(fraction,false);
        }
        repaint();
    }   
    
    /** 
     * Moves all sequences to the left the specified number of bases. 
     */
    public void moveAllSequencesPositionsLeft(int bases) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.moveSequencePositionsLeft(bases,false);
        }
        repaint();
    }   
    
    /** 
     * Moves all sequences to the right the specified number of bases. 
     */
    public void moveAllSequencesPositionsRight(int bases) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.moveSequencePositionsRight(bases,false);
        }
        repaint();
    }     
        
    /** 
     * Moves all sequences to the left end. 
     */
    public void moveAllSequencesToLeftEnd() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.moveSequenceToLeftEnd(false);
        }
        repaint();        

    }   
    
    /** 
     * Moves all sequences to the right end. 
     */
    public void moveAllSequencesToRightEnd() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.moveSequenceToRightEnd(false);
        }
        repaint();
    }
    
    /** 
     * Zooms in on all sequences while conforming to current alignment settings
     */
    public void zoomInOnAllSequences() {
        settings.enableVisualizationSettingsNotifications(false);
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.zoomInOnSequence(false);
        }
        settings.enableVisualizationSettingsNotifications(true);
        settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
        //repaint();
    }  
    
    /** 
     * Zooms out on all sequences while conforming to current alignment settings
     */
    public void zoomOutOnAllSequences() {
        settings.enableVisualizationSettingsNotifications(false);
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.zoomOutOnSequence(false);
        }
        settings.enableVisualizationSettingsNotifications(true);
        settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
        //repaint();
    }  
    
    /** 
     * Resets zoom level on all sequences while conforming to current alignment settings
     */
    public void zoomResetOnAllSequences() {
        settings.enableVisualizationSettingsNotifications(false);
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.zoomResetOnSequence(false);
        }
        settings.enableVisualizationSettingsNotifications(true);
        settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
        //repaint();

    }
    
    /** 
     * Sets individual zoomlevels on all sequences so that all fit within their respective sequence windows
     */
    public void zoomToFitOnAllSequences() {
        settings.enableVisualizationSettingsNotifications(false);
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.zoomToFitSequence(false);
        }
        settings.enableVisualizationSettingsNotifications(true);
        settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
        //repaint();
    }
    
   /**
    * Calling this method triggers a popup dialog that asks the user to enter a new zoom level
    * If the user enters a valid number, the zoom level of all sequences is set to that value
    * (However, if this number is greater than the largest possible zoom, it will be rounded down
    * to the largest possible zoom-level);
    */  
    public void zoomToCustomOnAllSequences() {
         String newZoomString=(String)JOptionPane.showInputDialog(settings.getGUI().getFrame(),"Enter new zoom level","Zoom to custom level",JOptionPane.PLAIN_MESSAGE,null,null,100.0);
         double zoomlevel=0;
         try {
             zoomlevel=Double.parseDouble(newZoomString);
         } catch (Exception e) {return;}

         if (zoomlevel>settings.getSequenceWindowSize()*100) zoomlevel=settings.getSequenceWindowSize()*100;
         settings.enableVisualizationSettingsNotifications(false);
         for (SequenceVisualizer viz:sequenceVisualizers) {
           viz.zoomToLevel(zoomlevel,false);
         }
         settings.enableVisualizationSettingsNotifications(true);
         settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
        //repaint();
    }
   
    /** 
     * Sets sets zoom level on all sequences to specified value (without asking the user to specify it)
     */
    public void zoomToLevelOnAllSequences(double zoomlevel) {       
        settings.enableVisualizationSettingsNotifications(false);
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.zoomToLevel(zoomlevel,false);
        }
        settings.enableVisualizationSettingsNotifications(true);
        settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
        //repaint();     
    }    
    
    /** 
     * Flips the current orientation of all sequences
     */
    public void flipOrientationOnAllSequences() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.flipSequenceStrand(false);
        }
        repaint();
    }
    
    /** 
     * Sets the orientation of all sequences to display direct strand
     */
    public void showDirectStrandOnAllSequences() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.showSequenceDirectStrand(false);
        }
        repaint();
    }
    
    /** 
     * Sets the orientation of all sequences to display reverse strand
     */
    public void showReverseStrandOnAllSequences() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.showSequenceReverseStrand(false);
        }
        repaint();
    }
    
    /** 
     * Sets the orientation of all sequences so they correspond the the respective genes
     */
    public void showStrandFromGeneOnAllSequences() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.showSequenceStrandFromGene(false);
        }
        repaint();
    }
    
    
    /** 
     * Sets the alignment of all sequences to LEFT and moves them to the left end. 
     */
    public void alignAllSequencesLeft() {
        for (SequenceVisualizer viz:sequenceVisualizers) {           
            viz.alignSequenceLeft(false);
        }
        settings.setDefaultSequenceAlignment(VisualizationSettings.ALIGNMENT_LEFT); // this will ensure that new sequences added later also will have the same alignment
        repaint();
    }


    /** 
     * Sets the alignment of all sequences to RIGHT and moves them to the right end. 
     */
    public void alignAllSequencesRight() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.alignSequenceRight(false);
        }
        settings.setDefaultSequenceAlignment(VisualizationSettings.ALIGNMENT_RIGHT);
        repaint();
    }
    
    /** 
     * Sets the alignment of all sequences to NONE
     */
    public void alignAllSequencesNone() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.alignSequenceNone(false);
        }
        settings.setDefaultSequenceAlignment(VisualizationSettings.ALIGNMENT_NONE);                
        repaint();
    }
    
    /** 
     * Sets the alignment of all sequences to TSS and moves them so that the TSS of
     * all sequences (or upstream end) is placed at the specified screen coordinate
     * @param x The screen x-coordinate where TSS should be placed
     */
    public void alignAllSequencesToTSSHere(int x) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.alignSequenceTSSatPoint(x,false);
        }
        settings.setDefaultSequenceAlignment(VisualizationSettings.ALIGNMENT_TSS); 
        repaint();
    }    

    /** 
     * Sets the alignment of all sequences to TSS and moves them so that the TSS of
     * all sequences (or upstream end) is placed at the right end of the sequence window
     */
    public void alignAllSequencesToTSSRight() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.alignSequenceTSSRight(false);
        }
        settings.setDefaultSequenceAlignment(VisualizationSettings.ALIGNMENT_TSS);        
        repaint();
    }    

    /** 
     * realigns the sequences using the currently selected alignment for each sequence
     */
    public void realignSequences() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            String name=viz.getName();
            String currentAlignment=settings.getSequenceAlignment(name);
                 if (currentAlignment.equals(VisualizationSettings.ALIGNMENT_LEFT)) viz.alignSequenceLeft(false);
            else if (currentAlignment.equals(VisualizationSettings.ALIGNMENT_RIGHT)) viz.alignSequenceRight(false);
            else if (currentAlignment.equals(VisualizationSettings.ALIGNMENT_TSS)) viz.alignSequenceTSSRight(false);
        }      
        repaint();
    }    

    
     /** 
     * Sets the movement constraint on all sequences to the specified value
     * @param constrain TRUE if movement should be constrained
     */
    public void setConstrainOnAllSequences(boolean contrain) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.constrainSequence(contrain,false);
        }
        repaint();
    }
    
    /** 
     * Sets the scroll lock setting on all sequences
     * @param lock TRUE if sequences should not be allowed to autoscroll
     */
    public void setScrollLockOnAllSequences(boolean lock) {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.scrollLockSequence(lock,false);
        }
        repaint();
    }
    /** 
     * Adds selection windows covering the full sequence to all sequences
     */
    public void selectAll() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.selectAll(false);
        }
        mainVisualizationPanel.repaint();
    }
    /** 
     * Inverts the selection windows in all sequences
     */
    public void invertSelections() {
        for (SequenceVisualizer viz:sequenceVisualizers) {
            viz.invertSelection(false);
        }
        mainVisualizationPanel.repaint();
    }    

    /** Scrolls the sequence window so that the named sequence is shown on top of the screen (if the sequence in question is currently visible and it is practically possible) */
    public void goToSequence(String sequenceName) {
         for (int index=0;index<sequenceVisualizers.size();index++) {             
             SequenceVisualizer seqviz=sequenceVisualizers.get(index);
             String seqName=seqviz.getSequenceName();
             if (!settings.isSequenceVisible(seqName)) continue;           
             if (seqName.equals(sequenceName)) {
                 this.getViewport().repaint(); // for some magical and illogical reason, this line is needed to prevent flickering when the viewport repositions
                 this.getViewport().setViewPosition(seqviz.getLocation());
                 seqviz.requestFocusInWindow();
             }
         }
    }
      
    @Override
    public boolean find(String searchstring) {
         int currentfocus=0;
         for (int i=0;i<sequenceVisualizers.size();i++) {
             if (sequenceVisualizers.get(i).hasFocus()) currentfocus=i;
         }
         for (int i=1;i<=sequenceVisualizers.size();i++) {             
             int index=(currentfocus+i)%sequenceVisualizers.size(); // scroll through all SeqViz starting at the next sequence. Wrap if necessary
             SequenceVisualizer seqviz=sequenceVisualizers.get(index);
             String seqName=seqviz.getSequenceName();
             if (!settings.isSequenceVisible(seqName)) continue;           
             if (seqName.matches("(?i).*"+searchstring+".*")) {
                 settings.getGUI().statusMessage("[Visualization] Searching for '"+searchstring+"' within sequences. Found '"+seqName+"'.");
                 this.getViewport().repaint(); // for some magical and illogical reason, this line is needed to prevent flickering when the viewport repositions
                 this.getViewport().setViewPosition(seqviz.getLocation());
                 seqviz.requestFocusInWindow();
                 return true;
             } else { // try searching the gene name of the sequence
                 Sequence seq=(Sequence)settings.getEngine().getDataItem(seqName, Sequence.class);
                 if (seq!=null) {
                     String geneName=seq.getGeneName();
                     if (geneName.matches("(?i).*"+searchstring+".*")) {
                         settings.getGUI().statusMessage("[Visualization] Searching for '"+searchstring+"' within sequences. Found '"+seqName+":"+geneName+"'.");
                         this.getViewport().repaint(); // for some magical and illogical reason, this line is needed to prevent flickering when the viewport repositions
                         this.getViewport().setViewPosition(seqviz.getLocation());
                         seqviz.requestFocusInWindow();
                         return true;
                     }                      
                 }
             }
         }
         settings.getGUI().statusMessage("[Visualization] Searching for '"+searchstring+"' within sequences. No matches found.");
         return false;
    }


    @Override
    public boolean supportsReplace() {
        return false;
    }

    @Override
    public void searchAndReplace() {
        // this is not supported
    }

    @Override
    public boolean replaceCurrent(String searchstring, String replacestring) {
        return false;
    }

    @Override
    public int replaceAll(String searchstring, String replacestring) {
        return 0;
    }

    @Override
    public boolean isSearchCaseSensitive() {
        return false;
    }

    @Override
    public void setSearchIsCaseSensitive(boolean flag) {
        // not supported
    }

    @Override
    public String getSelectedTextForSearch() {
       return null;
    }      

        
    

    /** This method has been stolen from Swing */
    public boolean print(final MessageFormat headerFormat, final MessageFormat footerFormat, final boolean showPrintDialog,final PrintService service,final PrintRequestAttributeSet attributes, final boolean interactive) throws PrinterException {
        final PrinterJob job = PrinterJob.getPrinterJob();
        final Printable printable;
        final PrintingStatus printingStatus;
        final boolean isHeadless = GraphicsEnvironment.isHeadless();
        final boolean isEventDispatchThread = SwingUtilities.isEventDispatchThread();
        if (interactive && ! isHeadless) {
            printingStatus = PrintingStatus.createPrintingStatus(this, job);
            printable = this;
        } else {
            printingStatus = null; 
            printable = this;
        }

        if (service != null) {
            job.setPrintService(service);
        }

        job.setPrintable(printable);

        final PrintRequestAttributeSet attr = (attributes == null) 
            ? new HashPrintRequestAttributeSet() 
            : attributes;        

        if (showPrintDialog && ! isHeadless && ! job.printDialog(attr)) {
            return false;
        }

        /*
         * there are three cases for printing:
         * 1. print non interactively (! interactive || isHeadless)
         * 2. print interactively off EDT
         * 3. print interactively on EDT
         * 
         * 1 and 2 prints on the current thread (3 prints on another thread)
         * 2 and 3 deal with PrintingStatusDialog
         */
        final Callable<Object> doPrint = 
            new Callable<Object>() {
                public Object call() throws Exception {
                    try {
                        job.print(attr);
                    } finally {
                        if (printingStatus != null) {
                            printingStatus.dispose();
                        }
                    }
                    return null;
                }
            };

        final FutureTask<Object> futurePrinting = 
            new FutureTask<Object>(doPrint);

        final Runnable runnablePrinting = 
            new Runnable() {
                public void run() {
                    //disable component
                    boolean wasEnabled = false;
                    if (isEventDispatchThread) {
                        if (isEnabled()) {
                            wasEnabled = true;
                            setEnabled(false);
                        }
                    } else {
                        try {
                            wasEnabled = SwingUtilities2.submit(
                                new Callable<Boolean>() {
                                    public Boolean call() throws Exception {
                                        boolean rv = isEnabled();
                                        if (rv) {
                                            setEnabled(false);
                                        } 
                                        return rv;
                                    }
                                }).get();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (ExecutionException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof Error) {
                                throw (Error) cause;
                            } 
                            if (cause instanceof RuntimeException) {
                                throw (RuntimeException) cause;
                            } 
                            throw new AssertionError(cause);
                        }
                    }

                    futurePrinting.run();

                    //enable component
                    if (wasEnabled) {
                        if (isEventDispatchThread) {
                            setEnabled(true);
                        } else {
                            try {
                                SwingUtilities2.submit(
                                    new Runnable() {
                                        public void run() {
                                            setEnabled(true);
                                        }
                                    }, null).get();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } catch (ExecutionException e) {
                                Throwable cause = e.getCause();
                                if (cause instanceof Error) {
                                    throw (Error) cause;
                                } 
                                if (cause instanceof RuntimeException) {
                                    throw (RuntimeException) cause;
                                } 
                                throw new AssertionError(cause);
                            }
                        }
                    }
                }
            };
        
        if (! interactive || isHeadless) {
            runnablePrinting.run();
        } else {
            if (isEventDispatchThread) {
                (new Thread(runnablePrinting)).start();
                printingStatus.showModal(true);
            } else {
                printingStatus.showModal(false);
                runnablePrinting.run();
            }
        }
        
        //the printing is done successfully or otherwise. 
        //dialog is hidden if needed.
        try {
            futurePrinting.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PrinterAbortException) {
                if (printingStatus != null
                    && printingStatus.isAborted()) {
                    return false;
                } else {
                    throw (PrinterAbortException) cause;
                }
            } else if (cause instanceof PrinterException) {
                throw (PrinterException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new AssertionError(cause);
            }
        }
        return true;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
          return(NO_SUCH_PAGE);
        } else {
          Graphics2D g2d = (Graphics2D)graphics;
          RepaintManager currentManager = RepaintManager.currentManager(mainVisualizationPanel);
          currentManager.setDoubleBufferingEnabled(false);          
          g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
          int height=mainVisualizationPanel.getHeight();
          int width=getImageWidth();
            // scale to fill the page
          double dw = pageFormat.getImageableWidth();
          double dh = pageFormat.getImageableHeight();
          double xScale = dw/(double)width;
          double yScale = dh/(double)height;
          double scale = Math.min(xScale,yScale);
          g2d.scale(scale, scale);        
          mainVisualizationPanel.printAll(g2d);
          currentManager.setDoubleBufferingEnabled(true);
          return(PAGE_EXISTS);
        }
    }
    
    private int getImageWidth() {
        int width=settings.getSequenceLabelWidth()+settings.getSequenceWindowSize()+2*VisualizationSettings.DATATRACK_BORDER_SIZE;
        Font trackLabelsFont=SequenceVisualizer.getTrackLabelsFont();
        int maxtracklabelwidth=0;
        ArrayList<String> trackorder=settings.getDatatrackOrder();
        for (String trackname:trackorder) {                 
           if (!settings.isTrackVisible(trackname)) continue;
           int tracklabelwidth=mainVisualizationPanel.getFontMetrics(trackLabelsFont).stringWidth(trackname);
           if (tracklabelwidth>maxtracklabelwidth) maxtracklabelwidth=tracklabelwidth;
        }
        width+=maxtracklabelwidth+16; // 16 is just a padding value
        return width;    
    }
    
private class RedispatchingMouseAdapter implements MouseListener, MouseWheelListener, MouseMotionListener {

    public void mouseClicked(MouseEvent e) {
        redispatchToParent(e);
    }

    public void mousePressed(MouseEvent e) {
        redispatchToParent(e);
    }

    public void mouseReleased(MouseEvent e) {
        redispatchToParent(e);
    }

    public void mouseEntered(MouseEvent e) {
        redispatchToParent(e);
    }

    public void mouseExited(MouseEvent e) {
        redispatchToParent(e);
    }

    public void mouseWheelMoved(MouseWheelEvent e){
        redispatchToParent(e);
    }

    public void mouseDragged(MouseEvent e){
        redispatchToParent(e);
    }

    public void mouseMoved(MouseEvent e) {
        redispatchToParent(e);
    }

    private void redispatchToParent(MouseEvent e){
        Component source = (Component) e.getSource();
        MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, VisualizationPanel.this);
        VisualizationPanel.this.dispatchEvent(parentEvent);
    }
}    
    
}    
    
    