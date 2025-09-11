package org.motiflab.gui;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPopupMenu;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;
import org.motiflab.engine.Graph;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.Region;
        

/**
 * Each SequenceVisualizer is responsible for visualizing ONE sequence (with multiple tracks)
 * in addition to a ruler and a sequence label
 * @author kjetikl
 */
public class SequenceVisualizer extends JPanel implements VisualizationSettingsListener, MouseInputListener, MouseWheelListener, FocusListener {
    public static final String ACTION_SET_VIEWPORT="Set Viewport";
    public static final String ACTION_MOVE_TO_LEFT_END="Move Viewport To Left End";
    public static final String ACTION_MOVE_TO_RIGHT_END="Move Viewport To Right End";
    public static final String ACTION_MOVE_LEFT="Move Viewport Left";
    public static final String ACTION_MOVE_RIGHT="Move Viewport Right";
    public static final String ACTION_ZOOM_IN="Zoom In";
    public static final String ACTION_ZOOM_OUT="Zoom Out";
    public static final String ACTION_ZOOM_RESET="Reset Zoom";
    public static final String ACTION_ZOOM_TO_FIT="Zoom To Fit";
    public static final String ACTION_ZOOM_TO_CUSTOM_LEVEL="Zoom To Custom Level";
    public static final String ACTION_ALIGN_LEFT="Align Left";
    public static final String ACTION_ALIGN_RIGHT="Align Right";
    public static final String ACTION_ALIGN_TSS_RIGHT="Align TSS Right";
    public static final String ACTION_ALIGN_TSS_HERE="Align TSS Here";
    public static final String ACTION_ALIGN_NONE="No Alignment";
    public static final String ACTION_ORIENTATION_FLIP="Flip Orientation";
    public static final String ACTION_ORIENTATION_DIRECT="Direct Strand";
    public static final String ACTION_ORIENTATION_REVERSE="Reverse Strand";
    public static final String ACTION_ORIENTATION_RELATIVE="Relative Strand";
    public static final String ACTION_REORDER_MOVE_UP="Move Sequence Up";
    public static final String ACTION_REORDER_MOVE_DOWN="Move Sequence Down";
    public static final String ACTION_REORDER_MOVE_TO_TOP="Move Sequence To Top";
    public static final String ACTION_REORDER_MOVE_TO_BOTTOM="Move Sequence To Bottom";    
    public static final String ACTION_RULER_TSS="Origo At Gene TSS";
    public static final String ACTION_RULER_UPSTREAM_END="Origo At Upstream End";
    public static final String ACTION_RULER_DOWNSTREAM_END="Origo At Downstream End";
    public static final String ACTION_RULER_CHROMOSOME="Origo At Chromosome Start";
    public static final String ACTION_RULER_FIXED="Fixed Ruler";
    public static final String ACTION_RULER_NONE="No Ruler";
    public static final String ACTION_CONSTRAIN="Constrain Movement";
    public static final String ACTION_SCROLL_LOCK="Scroll Lock";
    public static final String ACTION_HIDE_SEQUENCE="Hide This Sequence";
    public static final String ACTION_HIDE_OTHER_SEQUENCES="Show Only This Sequence";
    public static final String ACTION_HIDE_ALL_SEQUENCES="Hide All Sequences";
    public static final String ACTION_SHOW_ALL_SEQUENCES="Show All Sequences";
    public static final String ACTION_SELECT_ALL="Select All";
    public static final String ACTION_INVERT_SELECTION="Invert Selection";  
    public static final String ACTION_COPY_SEQUENCE="Copy Sequence";     

    private static final String LABEL_ZOOM="Zoom";
    private static final String LABEL_ORIENTATION="Orientation";
    private static final String LABEL_ALIGNMENT="Alignment";
    private static final String LABEL_RULER="Ruler";
    private static final String LABEL_REORDER="Reorder Sequences";
        
    private static final InfoPanelIcon ZOOM_IN_ICON=new InfoPanelIcon(InfoPanelIcon.ZOOM_IN_ICON);
    private static final InfoPanelIcon ZOOM_OUT_ICON=new InfoPanelIcon(InfoPanelIcon.ZOOM_OUT_ICON);
    private static final InfoPanelIcon ZOOM_RESET_ICON=new InfoPanelIcon(InfoPanelIcon.ZOOM_RESET_ICON);
    private static final InfoPanelIcon ZOOM_TO_FIT_ICON=new InfoPanelIcon(InfoPanelIcon.ZOOM_TO_FIT_ICON);
    private static final InfoPanelIcon ZOOM_ICON=new InfoPanelIcon(InfoPanelIcon.ZOOM_ICON);   
    private static final InfoPanelIcon LEFT_ICON=new InfoPanelIcon(InfoPanelIcon.LEFT_ICON);
    private static final InfoPanelIcon RIGHT_ICON=new InfoPanelIcon(InfoPanelIcon.RIGHT_ICON);
    private static final InfoPanelIcon LEFT_END_ICON=new InfoPanelIcon(InfoPanelIcon.LEFT_END_ICON);
    private static final InfoPanelIcon RIGHT_END_ICON=new InfoPanelIcon(InfoPanelIcon.RIGHT_END_ICON);      
    private static final InfoPanelIcon FLIP_ICON=new InfoPanelIcon(InfoPanelIcon.FLIP_ICON);
    private static final InfoPanelIcon ALIGN_LEFT_ICON=new InfoPanelIcon(InfoPanelIcon.ALIGN_LEFT_ICON); 
    private static final InfoPanelIcon ALIGN_RIGHT_ICON=new InfoPanelIcon(InfoPanelIcon.ALIGN_RIGHT_ICON); 
    private static final InfoPanelIcon ALIGN_TSS_ICON=new InfoPanelIcon(InfoPanelIcon.ALIGN_TSS_ICON);
    private static final InfoPanelIcon ALIGN_NONE_ICON=new InfoPanelIcon(InfoPanelIcon.ALIGN_NONE_ICON);
    private static final InfoPanelIcon CONSTRAIN_ICON= new InfoPanelIcon(InfoPanelIcon.CONSTRAIN_ICON); 
    private static final InfoPanelIcon LOCK_ICON=new InfoPanelIcon(InfoPanelIcon.LOCK_ICON);    

    private Action setViewportAction;
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
    private Action constrainAction;
    private Action scrollLockAction;
    private Action moveSequenceUpAction;
    private Action moveSequenceDownAction;
    private Action moveSequenceToTopAction;
    private Action moveSequenceToBottomAction;    
    private Action hideSequenceAction;
    private Action hideOtherSequencesAction; 
    private Action hideAllSequencesAction;    
    private Action showAllSequencesAction;    
    private Action selectAllAction; 
    private Action invertSelectionAction;   
    private Action copySequenceAction;    
    
    private int labelLeftMargin=12;
    private int labelRightMargin=10;
    private int margin=20;
    private int rulerHeight=24; // The size of the ruler = 24
    private String sequenceName;
    private Sequence datasequence;
    private VisualizationSettings settings;
    private SequenceLabel namelabel; // The label that displays the sequence name (left)
    private JPanel tracksPanel; // a panel that holds all DataTrackVisualizers and the ruler (center)
    private InfoPanel infoPanel; // Displays additional info such as current scale and orientation (right)
    private Ruler ruler; // Displays the ruler
    private HashMap<String,DataTrackVisualizer> allTrackvisualizers;
    private SelectionPane selectionPane;
    private MotifLabGUI gui;
    private int mouseButtonPressed=MouseEvent.NOBUTTON; // remember which mouseButton was pressed while dragging
    private int grabAnchor=-1; // the screen position at which a grab&drag-gesture (aka "move") is commenced. 
    private int storedViewPortStart=-1; // stores viewPortStart during grab&drag (aka "move")
    private static Cursor zoomInCursor=null;
    private static Cursor zoomOutCursor=null;
    private static Cursor handCursor=null;
    private static Cursor grabCursor=null;
    private static boolean stillDragging=false; // this must be static since you can only drag in one sequence at a time, and the others must know that!
    private static int bordersize=VisualizationSettings.DATATRACK_BORDER_SIZE;
    protected static double moveIncrement=0.02; // fraction of windowsize that should be used when moving (scrolling) sequences viewports
    private boolean visualizeBaseCursor=false; // draw colored rectangle around the base under the mouse pointer
    private static boolean visualizeBaseCursorOptionSelected=true;
    private static Font trackLabelsFont=new Font(Font.SERIF,Font.PLAIN,12);
    private static boolean condensedmode=false; 
    private static final int absoluteMinHeight=10;
    private static Color borderColor=Color.BLACK;
    private static final Color darkgreen = new Color(0,196,0);
    private static final Color translucent = new Color(0,0,0,20); // used for overlaying region between cursorflanks
    private static TrackBorder trackBorder = new TrackBorder(borderColor,bordersize);
    private static Color boundingBoxColor = borderColor;
    private String wheeltrack=null;
    private static int cursorFlanks=0;
    private int tracksTop=0;
    private int tracksBottom=0;
    private String lastSelectedTrack=null; // name of the track that where the last mouse-click occurred
    
    private boolean isInitialized=false; // This flag is raised after the SequenceVisualizer has been properly set up the first time
    private boolean isDirty=false; // This flag should be raised whenever the SequenceVisualizer needs to be updated
                                   // the update does not have to happen right away, but it must be done before the sequence is displayed

    public static boolean highlighCurrentRegion=true;
           
    
    static { // load static images and register some data track visualizers
        Toolkit toolkit=Toolkit.getDefaultToolkit();        
        Image ZoomIncursorimage=toolkit.getImage(SequenceVisualizer.class.getResource("resources/icons/zoom_in_cursor_32x32.png"));
        Image ZoomOutcursorimage=toolkit.getImage(SequenceVisualizer.class.getResource("resources/icons/zoom_out_cursor_32x32.png"));
        Image HandCursorimage=toolkit.getImage(SequenceVisualizer.class.getResource("resources/icons/hand_cursor_32x32.png"));
        Image DragCursorimage=toolkit.getImage(SequenceVisualizer.class.getResource("resources/icons/hand_cursor_grab_32x32.png"));
        zoomInCursor=toolkit.createCustomCursor(ZoomIncursorimage, new Point(5,5),"zoom");
        zoomOutCursor=toolkit.createCustomCursor(ZoomOutcursorimage, new Point(5,5),"zoom");
        handCursor=toolkit.createCustomCursor(HandCursorimage, new Point(7,6),"hand");
        grabCursor=toolkit.createCustomCursor(DragCursorimage, new Point(7,6),"grab");    
    }
    
    /**
     * Constructs a new SequenceVisualizer
     * @param sequenceName The name of the sequence that should be visualized
     * @param settings A reference to the shared VisualizationSettings object
     */
    public SequenceVisualizer(String sequenceName, VisualizationSettings settings) {
       this.sequenceName=sequenceName;
       this.settings=settings;
       this.gui=settings.getGUI();
       this.setAlignmentX(LEFT_ALIGNMENT);
       this.setFocusable(true);
       this.setRequestFocusEnabled(true);
       this.datasequence=(Sequence)gui.getEngine().getDataItem(sequenceName);         
       initialize(); // could this be delayed until the sequence is actually visualized to speed up "startup" (lazy initialization)
    }

    
    private void initialize() {      
      setupActions();
      addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                getVisualizationPanel().notifyPanelListeners(SequenceVisualizer.this, new Data[]{datasequence}, e);               
            }     
      });      
      allTrackvisualizers=new HashMap<String,DataTrackVisualizer>();
      namelabel=new SequenceLabel(sequenceName);      
      infoPanel=new InfoPanel();

      margin=settings.getSequenceMargin();
      namelabel.setBorder(BorderFactory.createEmptyBorder(0, labelLeftMargin, 0, 0));
      tracksPanel=new JPanel();
      tracksPanel.setLayout(null);
      tracksPanel.setOpaque(false);      
      ruler=new Ruler(rulerHeight);

      setOpaque(false);

      setLayout(null); // allows components to be laid out on top of each other
      add(namelabel);
      add(tracksPanel);
      add(infoPanel); 
      add(ruler);
      
      selectionPane=new SelectionPane();
      selectionPane.setBounds(tracksPanel.getBounds()); // selectionPane should be same size as tracksPanel
      selectionPane.clearSelection();

      settings.addVisualizationSettingsListener(this);
      arrangeDataTracks();

      int fullWidth=namelabel.getWidth()+tracksPanel.getWidth()+infoPanel.getWidth();
      ruler.setBounds(0,0,fullWidth,ruler.getHeight());        
            
      addFocusListener(this);
      isInitialized=true;
           
    }
    
    /* Returns an instance of a DataTrackVisualizer with that name and type. If the name is not recognized, a default visualizer for that type will be returned */
    public DataTrackVisualizer getRegisteredDataTrackVisualizer(String name, Class dtvClasstype) throws NullPointerException {
        Object dtv = gui.getEngine().getResource(name,"DTV");
        if (dtv!=null && dtvClasstype.isAssignableFrom(dtv.getClass())) return (DataTrackVisualizer)dtv; // The returned resource is a DTV of the requested type. Everything is OK!       
        // Either name is not registered or the requested type does not match, or something else went wrong. Return a default DTV for the type
             if (dtvClasstype==DataTrackVisualizer_Sequence.class) return new DataTrackVisualizer_Sequence_DNA();
        else if (dtvClasstype==DataTrackVisualizer_Numeric.class) return new DataTrackVisualizer_Numeric_BarGraph();
        else if (dtvClasstype==DataTrackVisualizer_Region.class) return new DataTrackVisualizer_Region_Default();
        else throw new NullPointerException("Unknown data track visualizer type");            
    }    
    
    
    public boolean isVisualizerDirty() {
        return isDirty || !isInitialized;
    }
    
    @Override
    public void paint(Graphics g) {  // this is the main paint() method that draws the complete SequenceVisualizer.      
        if (!isInitialized) initialize(); // lazy initialization for performance reasons
        if (isDirty) {   
            arrangeDataTracks(); // this will call setDirty() if not already dirty
        }        
        List<PanelDecorator> decorators=gui.getVisualizationPanel().getPanelDecorators();
           
        // this makes sure children (data tracks) are painted and paintComponent() is called 
        super.paint(g);                     
        
        // draw registered decorators on top of all the other stuff
        if (decorators!=null && !decorators.isEmpty()) {
             for (PanelDecorator decorator:decorators) {
                if (decorator.getTargetClass()==SequenceVisualizer.class && decorator.isActive()) decorator.draw((Graphics2D)g, SequenceVisualizer.this);
            }                    
        }

        // draw a bounding box around the tracks
        g.setColor(boundingBoxColor);
        Rectangle2D rect=tracksPanel.getBounds();
        g.drawRect((int)rect.getX(),tracksTop,tracksPanel.getWidth()-1,tracksBottom-tracksTop);  
        
    }
    
    /** returns the bounding box around the tracks themselves (including the border) but excluding everything around the tracks (like sequence name label, rulers and margins) */
    public Rectangle getTrackPanelBounds() {
        Rectangle2D rect=tracksPanel.getBounds();        
        return new Rectangle((int)rect.getX(), tracksTop, tracksPanel.getWidth(), tracksBottom-tracksTop+1);
    }

    @Override
    public String toString() {
        return "SeqViz:"+sequenceName;
    }
    
    public JPanel getTracksPanel() {
        return tracksPanel;
    }
    public JPanel getInfoPanel() {
        return infoPanel;
    }
    public JLabel getSequenceLabel() {
        return namelabel;
    }
    
    
    /** Specifies whether the sequence should be drawn in condensed mode
     *  In condensed form, only the tracks and the label (without arrow) is drawn
     *  The ruler, control panel, and sequence label arrow are left out
     */
    public static void setCondensedMode(boolean usecondensedmode) {
        condensedmode=usecondensedmode;
    }
    
    /** Returns TRUE if visualization should be carried out in condensed mode
     */
    public static boolean getCondensedMode() {
        return condensedmode;
    }
    
    public static void setCursorFlanks(int flanks) {
        if (flanks>=0) cursorFlanks=flanks;
        else cursorFlanks=0;
    }
    
    public static int getCursorFlanks() {
        return cursorFlanks;
    }    
    
    /** Returns the width that should be used for the sequence labels. This could be a fixed value or
     *  be derived from the longest label
     */
    public int getLabelWidth() {
        return settings.getSequenceLabelWidth();
    }
    
    /** Returns the rendered width of the sequence name associated with this visualizer
     */
    public int getRenderedLabelWidth() {
        // if ("no lanel in front") return 20; else return namelabel.getWidth();
        return namelabel.getWidth();
    }
    
    /** Returns the font that should be used for rendering track labels. These are only visible when saving visualization as an image */
    public static Font getTrackLabelsFont() {
        return trackLabelsFont;
    }

    /** The contents of this visualizer has been changed and must be recalculated and repainted as soon as possible */
    public void setDirty() {  
        isDirty=true; // this will call arrangeDataTracks the next time the component is redrawn      
        repaint();
    }

    
    /**
     * This method should be called whenever an event considering the number of, order of, or visibility of
     * or height of datatracks makes it necessary to rearrange the DataTrackVisualizers in the tracksPanel 
     */
    private void arrangeDataTracks() {   
        // gui.logMessage("arrangeDataTracks");
        tracksPanel.removeAll();
        tracksPanel.add(selectionPane);
        ArrayList<String> trackorder=settings.getDatatrackOrder();
        int current_y=0;
        if (!condensedmode) current_y=rulerHeight; 
        tracksTop=current_y;
        for (String trackname:trackorder) {
            DataTrackVisualizer trackvisualizer=getTrackVisualizer(trackname);          
            if (trackvisualizer==null) continue;
            trackvisualizer.refreshSettings(); // just in case            
            tracksPanel.add(trackvisualizer);
            trackvisualizer.setBounds(0, current_y,trackvisualizer.getWidth(),trackvisualizer.getHeight());
            current_y+=trackvisualizer.getHeight()-1;  
            
       }
       int tracksPanelWidth=settings.getSequenceWindowSize()+2*bordersize;
       tracksBottom=current_y;
       int height=(infoPanel.getHeight()>current_y+1)?infoPanel.getHeight():current_y+1;
       height+=margin;
       int nameLabelWidth=getLabelWidth();
       selectionPane.setBounds(0,0,tracksPanelWidth,height); // selectionPane should be same size as tracksPanel       
       namelabel.setBounds(0, 0,nameLabelWidth,height);
       tracksPanel.setBounds(nameLabelWidth, 0, tracksPanelWidth, height);
       infoPanel.setBounds(nameLabelWidth+tracksPanelWidth, 0, infoPanel.getWidth(), infoPanel.getHeight());
       RepaintManager.currentManager(this).markCompletelyDirty(this);    
       isDirty=false;       
       revalidate();     
    }


    /**
     * Returns the DataTrackVisualizer for the track with the given name (or null if something really bad happens)
     * It the DataTrackVisualizer does not currently exist, it will be created and initialized before returning it
     * @param trackName
     * @return
     */    
    public DataTrackVisualizer getTrackVisualizer(String trackname) {
        DataTrackVisualizer trackvisualizer=allTrackvisualizers.get(trackname);
        if (trackvisualizer==null) trackvisualizer=addNewDataTrack(trackname); // this track has not been created before
        if (!isCorrectTrackVisualizer(trackname, trackvisualizer)) { // visualizer does not match current VisualizationSettings, perhaps because the graph type is unknown. Use a default DTV and update the settings
            trackvisualizer=addNewDataTrack(trackname); // 
            String graphName=(trackvisualizer!=null)?trackvisualizer.getGraphTypeName():null;
            if (graphName!=null) settings.setGraphTypeSilently(trackname, graphName); // this will not trigger refresh
        }
        return trackvisualizer;
    }    

     /**
     * Returns the DataTrackVisualizer at the specified relative coordinate point
     * within this SequenceVisualizer (or null) if the point is not relative to
     * a DataTrackVisualizer
     * @param trackName
     * @return
     */
    public DataTrackVisualizer getTrackVisualizer(Point point) {
         if (!tracksPanel.getBounds().contains(point)) return null;
         Point relative=SwingUtilities.convertPoint(this, point, tracksPanel);
         for (java.awt.Component comp:tracksPanel.getComponents()) {
             if (comp instanceof DataTrackVisualizer) {
                 if (comp.getBounds().contains(relative)) return (DataTrackVisualizer)comp;
             }
         }
         return null;
    }
    




    /**
     * Instantiates the correct track visualizer to use for the given track and adds it to the panel.
     * @param trackName
     * @return
     */
    private DataTrackVisualizer addNewDataTrack(String trackName) {
         DataTrackVisualizer trackvisualizer=null;
         Data dataset=settings.getEngine().getDataItem(trackName);
         if (dataset==null || !(dataset instanceof FeatureDataset)) return null; // this could happen because of "unsynchronized" deletions
         Object graphObject=settings.getGraphType(trackName); // this used to return INT in older versions
         String graphType=(graphObject instanceof String)?(String)graphObject:"*"; //  
         Class dtvType=null;
              if (dataset instanceof DNASequenceDataset) dtvType = DataTrackVisualizer_Sequence.class;
         else if (dataset instanceof NumericDataset)     dtvType = DataTrackVisualizer_Numeric.class;
         else if (dataset instanceof RegionDataset)      dtvType = DataTrackVisualizer_Region.class;
         trackvisualizer = getRegisteredDataTrackVisualizer(graphType, dtvType);
         if (trackvisualizer==null) return null; // This should not really happen as long as you have default DTVs for each type.
         trackvisualizer.initialize(sequenceName, trackName, settings);     
         trackvisualizer.setBorder(trackBorder); // Note: changing the size of the border can have serious unexpected results and is not recommended
         trackvisualizer.addMouseListener(this);
         trackvisualizer.addMouseMotionListener(this);
         trackvisualizer.addMouseWheelListener(this); 
         allTrackvisualizers.put(trackName, trackvisualizer);
         return trackvisualizer;
    }
    
  
    /** Check if the given visualizer is correct for the track with the given name */
    private boolean isCorrectTrackVisualizer(String trackName, DataTrackVisualizer visualizer) {
         Data dataset=settings.getEngine().getDataItem(trackName);
         if (dataset==null || !(dataset instanceof FeatureDataset)) return false;
         if (    (dataset instanceof DNASequenceDataset && visualizer instanceof DataTrackVisualizer_Sequence)
              || (dataset instanceof NumericDataset && visualizer instanceof DataTrackVisualizer_Numeric)
              || (dataset instanceof RegionDataset && visualizer instanceof DataTrackVisualizer_Region)
         ) {
             /** Check here that the graphtype recorded in VisualizationSettings matches the current DTV */
             String graphType=settings.getGraphType(trackName);
             if (graphType==null || !graphType.equals(visualizer.getGraphTypeName())) return false;
             return true;
         }
         else return false;
    }

   @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(),getHeight());
    }
   
   @Override
    public Dimension getMinimumSize() {
        return new Dimension(getWidth(),getHeight());
    }

   @Override
    public Dimension getMaximumSize() {
        return new Dimension(getWidth(),getHeight());
    }
   
   @Override
    public Dimension getSize() {
        return new Dimension(getWidth(),getHeight());
    }
   
   @Override
    public int getHeight() {
        int height=getTotalTracksHeight();
        if (!condensedmode) {
            height=height+ruler.getHeight(); //add the size of a ruler 
        }
        if (height<infoPanel.getHeight()) height=infoPanel.getHeight();
        if (height<absoluteMinHeight) height=absoluteMinHeight;
        height+=margin; // margin between the sequences
        return height;
    }

   @Override
    public int getWidth() {
        int width=getLabelWidth()+settings.getSequenceWindowSize()+(2*bordersize);
        if (isPaintingForPrint()) width+=infoPanel.getTrackLabelsWidth();
        else width+=infoPanel.getWidth();
        return width;
    }   
   
   /** Returns the name of the sequence that this SequenceVisualizer displays */
   public String getSequenceName() {
       return sequenceName;
   }

    /**
     * This method returns the total height (in pixels) of all visible (and non-grouped)
     * datatracks (including a 1px collapsed border!) for this sequence
     * @return the total height of the tracks
     */
    public int getTotalTracksHeight() {
        int height=0;
        FeaturesPanelListModel listmodel=gui.getFeaturesPanelListModel();
        if (listmodel!=null) {
          for (int i=0;i<listmodel.getSize();i++) {
            FeatureDataset dataset=(FeatureDataset)listmodel.elementAt(i);
            if (dataset==null) continue; // dataset has been removed?
            String datasetname=dataset.getName();
            if (settings.isTrackVisible(datasetname) && !settings.isGroupedTrack(datasetname)) {
                if (settings.hasVariableTrackHeight(datasetname) || settings.isExpanded(datasetname)) { // not sure if these are two different groups or I should drop one of them 
                    DataTrackVisualizer viz=allTrackvisualizers.get(datasetname);
                    if (viz!=null) height += (viz.getTrackHeight() + 1);
                }
                else height+=(settings.getDataTrackHeight(datasetname)+1);
            }
          }
        }
        height+=1; // additional border
        return height;
    }

    
    /** Removes all DataTrackVisualizers currently employed by this SequenceVisualizer so that they will have to be created anew when needed */
    public void clearCachedVisualizers() {
        allTrackvisualizers.clear();
    }
    
    /** Removes the DataTrackVisualizers currently employed by this SequenceVisualizer to render the given feature track. The DataTrackVisualzier will have to be created anew when needed */   
    public void clearCachedVisualizer(String featurename) {
        allTrackvisualizers.remove(featurename);
    }    

   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the datatrack order is rearranged (or some tracks are hidden)
    */
    @Override
    public void trackReorderEvent(int type, Data affected) {
        setDirty();
    }
    
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the size of the sequence window is changed
    */
    @Override
    public void sequenceWindowSizeChangedEvent(int size, int oldsize) {
        int vpstart=settings.getSequenceViewPortStart(sequenceName); // vpstart is kept the same
        int oldVPend=vpstart+(int)Math.ceil((double)oldsize/(double)settings.getScale(sequenceName))-1; // vpend is dynamically calculated based on the size and scale. The scale is still the same (so far) but the size has changed.
        arrangeDataTracks();  
        selectionPane.setBounds(tracksPanel.getBounds());
        int fullWidth=namelabel.getWidth()+tracksPanel.getWidth()+infoPanel.getWidth();
        ruler.setBounds(0,0,fullWidth,ruler.getHeight());    
        settings.setSequenceViewPort(sequenceName, vpstart, oldVPend);
    }   
    
    
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the name of a dataitem is changed (by the user)
    */
    @Override
    public void dataRenamedEvent(String oldname, String newname) {
        DataTrackVisualizer visualizer = allTrackvisualizers.get(oldname);
        if (visualizer!=null) {
            visualizer.rename(newname);
            allTrackvisualizers.remove(oldname);
            allTrackvisualizers.put(newname, visualizer);
        }
        // arrangeDataTracks();        
    }    
    
   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the order of sequences or sequence sets is rearranged. 
    */
    @Override
    public void sequencesLayoutEvent(int type, Data affected) {
        // System.err.println("SeqViz["+sequenceName+"]:sequencesLayoutEvent ("+type+") => "+affected);
        if (type==VisualizationSettingsListener.SCALE_CHANGED) {
            setDirty(); // this might have to be revalidated
        } else if (type==VisualizationSettingsListener.REMOVED) {
            // remove cached visualizer for this track         
            if (affected instanceof FeatureDataset) allTrackvisualizers.remove(affected.getName()); // clear current visualizer so it will not be reused
            else if (affected instanceof Sequence && affected.getName().equals(sequenceName)) settings.removeVisualizationSettingsListener(this); // this sequence has been removed          
            setDirty(); // revalidate();
        } else if (type==VisualizationSettingsListener.UPDATED) {
            if (affected instanceof FeatureDataset) {
                DataTrackVisualizer viz=allTrackvisualizers.get(affected.getName());
                if (viz!=null) viz.dataUpdated();
                //RepaintManager.currentManager(this).markCompletelyDirty(this);                
                setDirty(); // revalidate();
            }
        } else if (type==VisualizationSettingsListener.REORDERED && affected==gui.getEngine().getDefaultSequenceCollection()) {
            // this is probably caused by UNDO/REDO?
            clearCachedVisualizers();  // previously: for (DataTrackVisualizer viz:allTrackvisualizers.values()) {viz.dataUpdated();} // .. but this was not synchronized properly
            setDirty(); // revalidate();
        } else if (type==VisualizationSettingsListener.FORCE_MAJURE) { // it is unclear what has happened but everything should be redone
            clearCachedVisualizers();  
            setDirty(); 
            revalidate();            
        }
        // No need to respond to other events such as VisualizationSettingsListener.VISIBILITY_CHANGED
    }
    
    @Override
    public void sequencesReordered(Integer oldposition, Integer newposition) {
        // not used here
    }

   /**
    * This callback method is called by the VisualizationSettings object whenever
    * the size of the sequence margin is changed
    */
    @Override
    public void sequenceMarginSizeChangedEvent(int newsize) {
         // we don't use this callback method. Instead we allow the VisualizationPanel to call setSequenceMarginWithoutRepaint() on every child
    }

        
    
    public void setSequenceMarginWithoutRepaint(int newsize) {
        margin=newsize;
        setDirty(); // invalidate();
    }
    
    public static void setTrackBorderColor(Color newColor) {
        borderColor=newColor;
        trackBorder.setBorderColor(borderColor);          
    }
    
    public static Color getTrackBorderColor() {
        return borderColor;        
    }    
    
    public static void setBoundingBoxColor(Color newColor) {
        boundingBoxColor=newColor;         
    }    
        
    public static Color getBoundingBoxColor() {
        return boundingBoxColor;       
    }      
           
    /** Adds a selection window which covers all of this sequence */
    public void selectAll(boolean redraw) {
        selectionPane.selectAll();
        if (redraw) gui.redraw();
    }  
    
    /**
     * Inverts the current selection windows in this sequence
     */
    public void invertSelection(boolean redraw) {
        selectionPane.invertSelection();
        if (redraw) gui.redraw();
    }      
    
    /**
     * Displays an input dialog which allows the user to enter new viewport coordinates
     * 
     */
    public void setViewPort() {
       int start=settings.getSequenceViewPortStart(sequenceName);
       int end=settings.getSequenceViewPortEnd(sequenceName);
       String posString=start+"-"+end;
       String newString = JOptionPane.showInputDialog("Enter new viewport coordinates for "+sequenceName, posString);              
       if (newString==null) return;
       boolean TSSrelative=false; // To Do: Allow specification of relative (or TSS/TES-relative) coordinates as an alternative to just genomic coordinates
       boolean TESrelative=false;
       boolean startRelative=false;
       String[] split;
       if (newString.startsWith("TSS:")) {newString=newString.substring(4);TSSrelative=true;}
       if (newString.startsWith("TES:")) {newString=newString.substring(4);TESrelative=true;}
       if (newString.startsWith(":")) {newString=newString.substring(1);startRelative=true;}       
       int colonpos=newString.indexOf(':');
       if (colonpos>=0 && newString.length()>colonpos+1) newString=newString.substring(newString.indexOf(':')+1); // discard chromosome (e.g. "chrN:start-end")
       if (newString.indexOf(',')>=0) {
          split=newString.split("\\s*,\\s*");   
       } else {
          split=newString.split("\\s*-\\s*"); // this can only be used if coordinates are positive
       }
       if (split.length!=2) return;
       try {
           start=Integer.parseInt(split[0]);
           end=Integer.parseInt(split[1]);
           if (TSSrelative) {
               if (datasequence.getTSS()==null) return;
               boolean skip0=gui.skipPosition0();
               start=datasequence.getGenomicPositionFromAnchoredRelativePosition(start,datasequence.getTSS(),skip0,true);
               end=datasequence.getGenomicPositionFromAnchoredRelativePosition(end,datasequence.getTSS(),skip0,true);
           } else if (TESrelative) {
               if (datasequence.getTES()==null) return;
               boolean skip0=gui.skipPosition0();   // the GUI has a setting for skipping 0 at TSS but not at TES. But we treat them as the same...
               start=datasequence.getGenomicPositionFromAnchoredRelativePosition(start,datasequence.getTES(),skip0,true);
               end=datasequence.getGenomicPositionFromAnchoredRelativePosition(end,datasequence.getTES(),skip0,true);
           } else if (startRelative) {
               start=datasequence.getGenomicPositionFromRelative(start-1,true);
               end=datasequence.getGenomicPositionFromRelative(end-1,true);
           }                                              
           if (end<start) {int swap=end;end=start;start=swap;}
           settings.setSequenceViewPort(sequenceName, start, end);
           repaint();
       } catch (Exception e) {} // just ignore
    }  
    

    
    /** Moves the sequence viewport left by a fraction of the sequence window (minimum 1 base) 
     * @param fraction Specifies the fraction of window length that sequence should be moved.
     *        if fraction==0 the sequence viewport will be moved 1 base
     *        if fraction is less than 0 a default fraction (currently 0.05) will be used
     * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not
     */
    public void moveSequenceLeft(double fraction, boolean repaint) {
         if (fraction<0) fraction=moveIncrement;      
         int increment=(int)((int)(settings.getSequenceWindowSize()/settings.getScale(sequenceName))*fraction);
         if (increment==0) increment++;
         if (settings.getSequenceOrientation(sequenceName)==VisualizationSettings.REVERSE) increment=-increment;
         int currentVPstart=settings.getSequenceViewPortStart(sequenceName);
         int newVPstart=currentVPstart-increment;
         settings.setSequenceViewPortStart(sequenceName, newVPstart);
         if (repaint) repaint();          
    }
    
    /** Moves the sequence viewport right by a fraction of the sequence window (minimum 1 base)
     * @param fraction Specifies the fraction of window length that sequence should be moved.
     *        if fraction==0 the sequence viewport will be moved 1 base   
     *        if fraction is less than 0 a default fraction (currently 0.05) will be used
     * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not
     */
    public void moveSequenceRight(double fraction, boolean repaint) {
         if (fraction<0) fraction=moveIncrement;
         int increment=(int)((int)(settings.getSequenceWindowSize()/settings.getScale(sequenceName))*fraction);
         if (increment==0) increment++;
         if (settings.getSequenceOrientation(sequenceName)==VisualizationSettings.REVERSE) increment=-increment;
         int currentVPstart=settings.getSequenceViewPortStart(sequenceName);
         int newVPstart=currentVPstart+increment;
         settings.setSequenceViewPortStart(sequenceName, newVPstart); 
         if (repaint) repaint();         
    }    
    
    /** Moves the sequence viewport left by a number of base positions
     * @param increment The number of bases to move
     * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not
     */
    public void moveSequencePositionsLeft(int increment, boolean repaint) {
         if (settings.getSequenceOrientation(sequenceName)==VisualizationSettings.REVERSE) increment=-increment;
         int currentVPstart=settings.getSequenceViewPortStart(sequenceName);
         int newVPstart=currentVPstart-increment;
         settings.setSequenceViewPortStart(sequenceName, newVPstart);
         if (repaint) repaint();          
    }
    
    /** Moves the sequence viewport right by a number of base positions
     * @param increment The number of bases to move
     * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not
     */
    public void moveSequencePositionsRight(int increment, boolean repaint) {
         if (settings.getSequenceOrientation(sequenceName)==VisualizationSettings.REVERSE) increment=-increment;
         int currentVPstart=settings.getSequenceViewPortStart(sequenceName);
         int newVPstart=currentVPstart+increment;
         settings.setSequenceViewPortStart(sequenceName, newVPstart); 
         if (repaint) repaint();         
    }      
    
    /** Moves the sequence viewport to the left end of the sequence
     * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not    
     */
    public void moveSequenceToLeftEnd(boolean repaint) {
         int orientation=settings.getSequenceOrientation(sequenceName);
         if (orientation==VisualizationSettings.DIRECT) settings.setSequenceViewPortStart(sequenceName, datasequence.getRegionStart());
         else settings.setSequenceViewPortEnd(sequenceName, datasequence.getRegionEnd());      
         if (repaint) repaint();         
    }    
    
   /** Moves the sequence viewport to the right end of the sequence
    * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not  
    */
    public void moveSequenceToRightEnd(boolean repaint) {
         int orientation=settings.getSequenceOrientation(sequenceName);
         if (orientation==VisualizationSettings.DIRECT) settings.setSequenceViewPortEnd(sequenceName, datasequence.getRegionEnd());                
         else settings.setSequenceViewPortStart(sequenceName, datasequence.getRegionStart());
         if (repaint) repaint();        
    }  
    
   /** Zooms in on the sequence while conforming to current alignment settings
    * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not  
    */
    public void zoomInOnSequence(boolean repaint) {
         settings.zoomInOnSequence(sequenceName,false);
         if (repaint) repaint();        
    }  
    
   /** Zooms out on the sequence while conforming to current alignment settings
    * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not  
    */
    public void zoomOutOnSequence(boolean repaint) {
         settings.zoomOutOnSequence(sequenceName,false);
         if (repaint) repaint();        
    }  
    
   /** Resets zoom on the sequence while conforming to current alignment settings
    * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not  
    */    
    public void zoomResetOnSequence(boolean repaint) {
         settings.zoomToLevel(sequenceName,100.0, repaint);
         if (repaint) repaint();        
    }  
    
   /** Sets the zoom level on the sequence while conforming to current alignment settings
    * 
    * @param zoomlevel The new zoom level
    * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not  
    */    
    public void zoomToLevel(double zoomlevel,boolean repaint) {
         settings.zoomToLevel(sequenceName,zoomlevel,repaint);
         if (repaint) repaint();        
    }  
    
   /** Sets the zoom level on the sequence so the full sequence fits within the sequence window
    * 
    * @param repaint If TRUE the sequenceVisualizer will be repainted immediately if FALSE it will not  
    */    
    public void zoomToFitSequence(boolean repaint) {
         settings.zoomToFitSequence(sequenceName, repaint);
         if (repaint) repaint();        
    }  
    

   /**
    * Calling this method triggers a popup dialog that asks the user to enter a new zoom level
    * If the user enters a valid number, the zoom level of the sequence is set to that value
    * (However, if this number is greater than the largest possible zoom, it will be rounded down
    * to the largest possible zoom-level);
    */    
    public void zoomToCustomLevel() {
         String newZoomString=(String)JOptionPane.showInputDialog(gui.getFrame(),"Enter new zoom level","Zoom "+sequenceName+" to custom level",JOptionPane.PLAIN_MESSAGE,null,null,settings.getSequenceZoomLevel(sequenceName));
         try {
             double zoomlevel=Double.parseDouble(newZoomString);
             if (zoomlevel>settings.getSequenceWindowSize()*100) zoomlevel=settings.getSequenceWindowSize()*100;
             zoomToLevel(zoomlevel,true);
         } catch (Exception e) {}              
    }  
    
    
    
 
   /** Reorders sequences in the dataset by moving the sequence associated with this SequenceVisualizer one place up */
    public void reorderSequencesMoveUp() {
        SequenceCollection dataset = settings.getEngine().getDefaultSequenceCollection();
        int orderWithinDataset=dataset.getIndexForSequence(sequenceName);
        int newposition=orderWithinDataset;
        while (newposition>0) {
            newposition--;
            if (settings.isSequenceVisible(dataset.getSequenceNameByIndex(newposition))) break; // if the sequence above is hidden, keep on moving up
        }
        dataset.reorderSequences(orderWithinDataset, newposition);    
        getVisualizationPanel().reorderSequences(orderWithinDataset,newposition);
        requestFocusInWindow();
    }    
 
   /** Reorders sequences in the dataset by moving the sequence associated with this SequenceVisualizer one place down among the currently visible sequences */
    public void reorderSequencesMoveDown() {
        SequenceCollection dataset = settings.getEngine().getDefaultSequenceCollection();
        int orderWithinDataset=dataset.getIndexForSequence(sequenceName);
        int newposition=orderWithinDataset;        
        while (newposition<dataset.getNumberofSequences()-1) {
            newposition++;
            if (settings.isSequenceVisible(dataset.getSequenceNameByIndex(newposition))) break; // if the sequence above is hidden, keep on moving up            
        }         
        dataset.reorderSequences(orderWithinDataset, newposition);
        getVisualizationPanel().reorderSequences(orderWithinDataset,newposition);        
        requestFocusInWindow();
    }    
    
    public void reorderSequencesMoveToTop() {
        SequenceCollection dataset = settings.getEngine().getDefaultSequenceCollection();
        int orderWithinDataset=dataset.getIndexForSequence(sequenceName);
        int newposition=0;          
        dataset.reorderSequences(orderWithinDataset, newposition);
        getVisualizationPanel().reorderSequences(orderWithinDataset,newposition);        
        requestFocusInWindow();         
    }
    
    public void reorderSequencesMoveToBottom() {
        SequenceCollection dataset = settings.getEngine().getDefaultSequenceCollection();
        int orderWithinDataset=dataset.getIndexForSequence(sequenceName);
        int newposition=dataset.getNumberofSequences()-1;          
        dataset.reorderSequences(orderWithinDataset, newposition);
        getVisualizationPanel().reorderSequences(orderWithinDataset,newposition);        
        requestFocusInWindow();        
    }    
 
    /** Flips the sequence display if necessary so that the DIRECT strand is displayed */
    public void showSequenceDirectStrand(boolean repaint) {
       settings.setSequenceOrientation(sequenceName,VisualizationSettings.DIRECT);
       if (repaint) repaint();  
    }
    
    /** Flips the sequence display if necessary so that the REVERSE strand is displayed */
    public void showSequenceReverseStrand(boolean repaint) {
       settings.setSequenceOrientation(sequenceName,VisualizationSettings.REVERSE);
       if (repaint) repaint();  
    }
    
    /** Flips the sequence display if necessary so that the strand corresponding to the orientation of the gene is display */
    public void showSequenceStrandFromGene(boolean repaint) {
        int geneOrientation=((Sequence)settings.getEngine().getDataItem(sequenceName)).getStrandOrientation();
        settings.setSequenceOrientation(sequenceName,geneOrientation);
        if (repaint) repaint();   
    }
    
    /** Flips the sequence so that the complementary strand is shown (relative to the strand shown at present) */
    public void flipSequenceStrand(boolean repaint) {
       int newOrientation=(settings.getSequenceOrientation(sequenceName)==VisualizationSettings.DIRECT)?VisualizationSettings.REVERSE:VisualizationSettings.DIRECT;
       settings.setSequenceOrientation(sequenceName,newOrientation);
       if (repaint) repaint();  
    }
        
    /** Sets the constrain setting for this sequence
     *
     * @param constrain TRUE if the movement of the sequence should be constrained else FALSE 
     */
    public void constrainSequence(boolean constrain, boolean repaint) {
       settings.setSequenceConstrained(sequenceName,constrain); 
       if (repaint) repaint(); 
    }
        
    /** Sets the scroll lock setting for this sequence
     *
     * @param lock TRUE if the sequence should not be allowed to autoscroll 
     */    
    public void scrollLockSequence(boolean lock, boolean repaint) {
       settings.setSequenceScrollLock(sequenceName,lock);
       if (repaint) repaint(); 
    }
   
    /** Sets the alignment of this sequence to LEFT */
    public void alignSequenceLeft(boolean repaint) {
       settings.setSequenceAlignment(sequenceName,VisualizationSettings.ALIGNMENT_LEFT);            
       moveSequenceToLeftEnd(false);
       if (repaint) repaint();  
    }
    
    /** Sets the alignment of this sequence to RIGHT */
    public void alignSequenceRight(boolean repaint) {
       settings.setSequenceAlignment(sequenceName,VisualizationSettings.ALIGNMENT_RIGHT);            
       moveSequenceToRightEnd(false);
       if (repaint) repaint();  
    }
    
    /** Sets the alignment of this sequence to TSS and align TSS at right end of viewport */
    public void alignSequenceTSSRight(boolean repaint) {
        int x=settings.getSequenceWindowSize();
        int TSS=0;
        Integer TSSint=datasequence.getTSS();
        if (TSSint!=null) TSS=TSSint.intValue();
        else {
                if (datasequence.getStrandOrientation()==VisualizationSettings.DIRECT) TSS=datasequence.getRegionEnd();
                else TSS=datasequence.getRegionStart();
        }
        settings.setSequenceAlignment(sequenceName,VisualizationSettings.ALIGNMENT_TSS);
        settings.alignSequenceViewPort(sequenceName, TSS, x,VisualizationSettings.ALIGNMENT_TSS);
        if (repaint) repaint();  
    }
    
    /** Sets the alignment of this sequence to TSS and align TSS at the specified screen coordinate */
    public void alignSequenceTSSatPoint(int x, boolean repaint) {
        int TSS=0;
        Integer TSSint=datasequence.getTSS();
        if (TSSint!=null) TSS=TSSint.intValue();
        else {
                if (datasequence.getStrandOrientation()==VisualizationSettings.DIRECT) TSS=datasequence.getRegionEnd();
                else TSS=datasequence.getRegionStart();
        }
        settings.setSequenceAlignment(sequenceName,VisualizationSettings.ALIGNMENT_TSS);
        settings.alignSequenceViewPort(sequenceName, TSS, x,VisualizationSettings.ALIGNMENT_TSS);
        if (repaint) repaint();  
    }
    
    /** Sets the alignment of this sequence to NONE */
    public void alignSequenceNone(boolean repaint) {
       settings.setSequenceAlignment(sequenceName,VisualizationSettings.ALIGNMENT_NONE);
       if (repaint) repaint();  
    }
    
    
    /** Aligns the sequence to the specified alignment by calling the appropritate alignSequenceX function
     * @param alignment A string specifying the new alignment (see constants in VisualizationSettings)
     */
    public void alignSequence(String alignment, boolean repaint) {
             if (alignment.equals(VisualizationSettings.ALIGNMENT_LEFT)) alignSequenceLeft(repaint);
        else if (alignment.equals(VisualizationSettings.ALIGNMENT_RIGHT)) alignSequenceRight(repaint);
        else if (alignment.equals(VisualizationSettings.ALIGNMENT_NONE)) alignSequenceNone(repaint);
        else if (alignment.equals(VisualizationSettings.ALIGNMENT_TSS)) alignSequenceTSSRight(repaint);
    }
    
    /** Returns the parent VisualizationPanel (if found, else NULL) */
    public VisualizationPanel getVisualizationPanel() {
        return gui.getVisualizationPanel();    
    }
    
    /** Sets a new position for the base cursor
     *  This position usually follows the mouse but you can use this method to update
     *  the position when there is no mouse movement
     */
    public void setCurrentBasePosition(int genomicpos) {
        selectionPane.setBaseCursorPosition(genomicpos);
    }

    /** This method selects whether or not a base cursor should be drawn around the current base
     *  A base cursor is a colored rectangle drawn (for all tracks) around the base which is under the current mouse position
     */
    public static void setVisualizeBaseCursor(boolean flag) {
        visualizeBaseCursorOptionSelected=flag;
    }

    /** Determines whether or not a base cursor should be drawn around the current base
     *  A base cursor is a colored rectangle drawn (for all tracks) around the base which is under the current mouse position
     */
    public static boolean getVisualizeBaseCursor() {
        return visualizeBaseCursorOptionSelected;
    }


    // ****************** mouse event listeners. These implement selection, move and zoom ********************
    

    
    @Override
    public void mouseClicked(MouseEvent e) {
       if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}
       stillDragging=false;
       if (e.isPopupTrigger()) return;
       if (gui.getSelectedTool().equals(MotifLabGUI.ZOOM_TOOL)) {
                if (e.getButton()==MouseEvent.BUTTON1 && !e.isShiftDown()) settings.zoomInOnSequence(sequenceName, e.getX()-bordersize,false);
           else if (e.getButton()==MouseEvent.BUTTON1 && e.isShiftDown()) settings.zoomOutOnSequence(sequenceName, e.getX()-bordersize,false);
           else if (e.getButton()==MouseEvent.BUTTON2 && !e.isPopupTrigger()) settings.zoomOutOnSequence(sequenceName, e.getX()-bordersize,false);
       }
       gui.setSearchTargetComponent(null); // this will redirect the "find" function to the currently open tab in the main panel
       if (e.getComponent() instanceof DataTrackVisualizer) {
            lastSelectedTrack=((DataTrackVisualizer)e.getComponent()).featureName;
       }
       redispatchMouseEvent(e); // send it upwards in case they need it
    }

    @Override
    public void mouseEntered(MouseEvent e) {
       SequenceVisualizer.this.requestFocusInWindow();  
       if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}       
       visualizeBaseCursor=true; 
       if (stillDragging) {
           if (gui.getSelectedTool().equals(MotifLabGUI.MOVE_TOOL)) setMouseCursor(grabCursor);
       } else { // mouse pointer has entered a new sequence while still dragging on a different sequence
                 if (gui.getSelectedTool().equals(MotifLabGUI.SELECTION_TOOL)) setMouseCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            else if (gui.getSelectedTool().equals(MotifLabGUI.MOVE_TOOL)) setMouseCursor(handCursor);
            else if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) setMouseCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            else if (gui.getSelectedTool().equals(MotifLabGUI.ZOOM_TOOL)) {
                if (e.isShiftDown()) setMouseCursor(zoomOutCursor); 
                else setMouseCursor(zoomInCursor); 
            }  
        }
        redispatchMouseEvent(e); // send it upwards in case they need it
    }

    @Override
    public void mouseExited(MouseEvent e) {
       if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}       
       visualizeBaseCursor=false;
       if (stillDragging) {
           if (gui.getSelectedTool().equals(MotifLabGUI.MOVE_TOOL)) setMouseCursor(grabCursor);;
       } else {
           setMouseCursor(Cursor.getDefaultCursor());
           repaint();
       }
       redispatchMouseEvent(e); // send it upwards in case they need it
    }

    @Override
    public void mousePressed(MouseEvent e) {
       SequenceVisualizer.this.requestFocusInWindow();
       if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}       
       if (mouseButtonPressed==MouseEvent.NOBUTTON) mouseButtonPressed=e.getButton();
       if (e.isPopupTrigger()) {showContextMenu(e);return;}
       if (e.getButton()==MouseEvent.BUTTON3) {return;}
       if (gui.getSelectedTool().equals(MotifLabGUI.SELECTION_TOOL) || gui.getSelectedTool().equals(MotifLabGUI.ZOOM_TOOL)) {
          selectionPane.clearSelection(); // clear any selection.
          if (!(e.isAltDown() || e.isShiftDown()) && gui.getSelectedTool().equals(MotifLabGUI.SELECTION_TOOL)) settings.clearSelectionWindows();
          selectionPane.setFirstAnchor(e.getX()-bordersize, e.isShiftDown() && gui.getSelectedTool().equals(MotifLabGUI.SELECTION_TOOL)); //  We don't know yet if the user will drag to select, but just in case we remember the position
       } else if (gui.getSelectedTool().equals(MotifLabGUI.MOVE_TOOL)) {
           setMouseCursor(grabCursor);
           //setGrabAnchor(e.getX()-bordersize); // the screen coordinate (which will be converted to genomic when grabAnchor is set)
           grabAnchor=(e.getX()-bordersize); // the screen coordinate (which will be converted to genomic when grabAnchor is set)
           storedViewPortStart=settings.getSequenceViewPortStart(sequenceName);
       } else if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
           settings.clearSelectionWindows();
       } 
       redispatchMouseEvent(e); // send it upwards in case they need it
    }

    @Override
    public void mouseReleased(MouseEvent e) {
       mouseButtonPressed=MouseEvent.NOBUTTON;
       stillDragging=false;
       if (e.isPopupTrigger()) {showContextMenu(e);return;}       
       gui.statusMessage("");
       if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}       

       if (gui.getSelectedTool().equals(MotifLabGUI.MOVE_TOOL)) setMouseCursor(handCursor);
       else if (gui.getSelectedTool().equals(MotifLabGUI.ZOOM_TOOL)) {
            if (settings.getSequenceWithSelection()!=null) {
              //gui.debugMessage("Zooming in on "+sequenceName);
              int start=selectionPane.getSelectionStartPosition();
              int end=selectionPane.getSelectionEndPosition();
              settings.setSequenceViewPort(sequenceName,start,end);
              selectionPane.clearSelection(); 
            }
       } else if (gui.getSelectedTool().equals(MotifLabGUI.SELECTION_TOOL)) {
            if (settings.getSequenceWithSelection()!=null) {
              int start=selectionPane.getSelectionStartPosition();
              int end=selectionPane.getSelectionEndPosition();
              if (e.isShiftDown()) settings.subtractSelectionWindow(sequenceName,start,end);
              else settings.addSelectionWindow(sequenceName,start,end,!e.isAltDown(),true);
              selectionPane.clearSelection(); 
            }
       }     
       redispatchMouseEvent(e); // send it upwards in case they need it
    }

    @Override
    public void mouseDragged(MouseEvent e) {
       if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}         
       stillDragging=true;
       if (mouseButtonPressed!=MouseEvent.BUTTON1) {stillDragging=false;return;}      
       String tool=gui.getSelectedTool();
       int orientation=settings.getSequenceOrientation(sequenceName);
       if (tool.equals(MotifLabGUI.SELECTION_TOOL) || tool.equals(MotifLabGUI.ZOOM_TOOL)) {
           int pos=e.getX()-bordersize;
           selectionPane.setSecondAnchor(pos, e.isShiftDown() && tool.equals(MotifLabGUI.SELECTION_TOOL)); //
           selectionPane.setCurrentPosition(pos,e.isShiftDown() && tool.equals(MotifLabGUI.SELECTION_TOOL));
           int start=selectionPane.getSelectionStartPosition();
           int end=selectionPane.getSelectionEndPosition();
           int selectedbases=settings.countSelectedBasesInSequence(sequenceName, start, end, e.isShiftDown(), !e.isAltDown(), true);
           String statusmsg;
           if (end-start==0) statusmsg="Selected 1 base [chr"+datasequence.getChromosome()+":"+start+"]. Total = "+MotifLabEngine.groupDigitsInNumber(selectedbases)+" bp";
           else statusmsg="Selected "+(MotifLabEngine.groupDigitsInNumber(end-start+1))+" bases  [chr"+datasequence.getChromosome()+":"+start+"-"+end+"]. Total = "+MotifLabEngine.groupDigitsInNumber(selectedbases)+" bp";
           gui.statusMessage(statusmsg);
           //setToolTipText((end-start==0)?"1 bp":((end-start+1)+" bp"));  // this did not work...                     
       }
       else if (tool.equals(MotifLabGUI.MOVE_TOOL)) {
           int moved=(int)(((e.getX()-bordersize)-grabAnchor)/settings.getScale(sequenceName));
           int newposition=storedViewPortStart-orientation*moved;
           settings.setSequenceViewPortStart(sequenceName,newposition);
           repaint();  
       }
       redispatchMouseEvent(e); // send it upwards in case they need it
       
    }

    @Override
    public void mouseMoved(MouseEvent e) {
       if (wheeltrack!=null) wheeltrack=null; // clear this property
       if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}       
       if (gui.getSelectedTool().equals(MotifLabGUI.MOVE_TOOL)) setMouseCursor(handCursor);
       selectionPane.setCurrentPosition(e.getX()-bordersize, e.isShiftDown() && gui.getSelectedTool().equals(MotifLabGUI.SELECTION_TOOL)); //
       redispatchMouseEvent(e); // send it upwards in case they need it
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (getVisualizationPanel().isMouseEventsBlocked()) {redispatchMouseEvent(e);return;}        
        if (e.isControlDown() || ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0)) { // change zoom level if CONTROL or middle mouse button is pressed
            if (e.getWheelRotation()<0) settings.zoomInOnSequence(sequenceName, e.getX()-bordersize,true);
            else settings.zoomOutOnSequence(sequenceName, e.getX() - bordersize,true);
            int[] genomic=getGenomicCoordinateFromScreenOffset(e.getX()-bordersize);
            setCurrentBasePosition((genomic[0]+genomic[1])/2); //
        } else if (e.isShiftDown()) { // change track height
            if (e.getComponent() instanceof DataTrackVisualizer) {
                String trackname=wheeltrack; // the wheeltrack name is used so that the trackname is only readjusted when the mouse itself moves, not because the mouse points to a different track because of resizing 
                if (trackname==null) {
                    trackname=((DataTrackVisualizer)e.getComponent()).getFeatureName();
                    wheeltrack=trackname;
                }
                if (e.getComponent() instanceof DataTrackVisualizer_Region && ((DataTrackVisualizer_Region)e.getComponent()).isExpanded()) {
                    int currentRegionHeight=settings.getExpandedRegionHeight(trackname);
                    int newheight=currentRegionHeight+e.getWheelRotation();
                    if (newheight>VisualizationSettings.EXPANDED_REGION_MAX_HEIGHT) newheight=VisualizationSettings.EXPANDED_REGION_MAX_HEIGHT;
                    else if (newheight<1) newheight=1;
                    settings.setExpandedRegionHeight(trackname, newheight,true);                    
                } else {
                    int currentTrackHeight=settings.getDataTrackHeight(trackname);
                    int newheight=currentTrackHeight+e.getWheelRotation();
                    if (newheight>VisualizationSettings.DATATRACK_MAX_HEIGHT) newheight=VisualizationSettings.DATATRACK_MAX_HEIGHT;
                    else if (newheight<VisualizationSettings.DATATRACK_MIN_HEIGHT) newheight=VisualizationSettings.DATATRACK_MIN_HEIGHT;
                    settings.setDataTrackHeight(trackname, newheight,true);
                }
            }
        } else {
            getVisualizationPanel().mouseWheelMoved(e); // forward event to parent (to move JScrollPane)
            return;           
        }
    }
  

    @Override
    public void focusGained(FocusEvent e) {
        namelabel.repaint(); // this ensures that the focus marker is painted in the namelabel when the sequence gains focus
    }
    
    @Override
    public void focusLost(FocusEvent e) {
        namelabel.repaint(); // this ensures that the focus marker is removed from the namelabel when the sequence looses focus
    }
      
    
    private void redispatchMouseEvent(MouseEvent e) {
        Component source = (Component)e.getSource();
        Component parent=getVisualizationPanel().getMainVisualizationPanel();
        MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, parent);
        parent.dispatchEvent(parentEvent);
    }    
    
    // ****************** end of listeners ********************
    
   
    
    public int[] getGenomicCoordinateFromScreenOffset(int x) {
        return settings.getGenomicCoordinateFromScreenOffset(sequenceName,x);   
    }
    
    public int[] getScreenCoordinateFromGenomic(int x) {
        return settings.getScreenCoordinateFromGenomic(sequenceName,x);       
    } 

    
    /** 
     * Sets the mouse cursor both in this panel and its parent 
     * This is done so that a "dragging" cursor can be displayed even if the mouse moves outside
     * the bounds of a single SequenceVisualizer 
     * 
     * THIS DOES NOT WORK PROPERLY (I THINK) !
     */
    private void setMouseCursor(Cursor newcursor) {
        setCursor(newcursor);
        //if (stillDragging) getParent().setCursor(newcursor);
        //else getParent().setCursor(Cursor.getDefaultCursor());
    }
    
    /** 
     * shows the Context Menu
     * 
     */
    private void showContextMenu(MouseEvent e) {
        String trackName=null;
        if (e.getComponent() instanceof DataTrackVisualizer) {
            trackName=((DataTrackVisualizer)e.getComponent()).featureName;
        }
        JPopupMenu popup=SequenceVisualizerContextMenu.getInstance(this, trackName, e.getX()-bordersize, gui);
        if (popup!=null) {
            getVisualizationPanel().notifyPanelListeners((SequenceVisualizer)this, new Data[]{datasequence}, popup);
            popup.show(e.getComponent(), e.getX(),e.getY());
        }
    }


    
    private void setupActions() {
        setViewportAction=new AbstractAction(ACTION_SET_VIEWPORT) { public void actionPerformed(ActionEvent e) {setViewPort();} };
        selectAllAction=new AbstractAction(ACTION_SELECT_ALL) { public void actionPerformed(ActionEvent e) {selectAll(true);} };
        invertSelectionAction=new AbstractAction(ACTION_INVERT_SELECTION) { public void actionPerformed(ActionEvent e) {invertSelection(true);} };

        zoomInAction=new AbstractAction(ACTION_ZOOM_IN, ZOOM_IN_ICON) {public void actionPerformed(ActionEvent e) {zoomInOnSequence(true);} };
        zoomOutAction=new AbstractAction(ACTION_ZOOM_OUT, ZOOM_OUT_ICON) { @Override public void actionPerformed(ActionEvent e) {zoomOutOnSequence(true);} };
        zoomResetAction=new AbstractAction(ACTION_ZOOM_RESET, ZOOM_RESET_ICON) { public void actionPerformed(ActionEvent e) {zoomResetOnSequence(true);} };
        zoomToFitAction=new AbstractAction(ACTION_ZOOM_TO_FIT, ZOOM_TO_FIT_ICON) { public void actionPerformed(ActionEvent e) {zoomToFitSequence(true);} };
        zoomToCustomAction=new AbstractAction(ACTION_ZOOM_TO_CUSTOM_LEVEL, ZOOM_ICON) { public void actionPerformed(ActionEvent e) {zoomToCustomLevel();} };
        
        moveLeftAction=new AbstractAction(ACTION_MOVE_LEFT, LEFT_ICON) { public void actionPerformed(ActionEvent e) {moveSequenceLeft(moveIncrement,true);} };
        moveRightAction=new AbstractAction(ACTION_MOVE_RIGHT, RIGHT_ICON) { public void actionPerformed(ActionEvent e) {moveSequenceRight(moveIncrement,true);} };
        moveToLeftEndAction=new AbstractAction(ACTION_MOVE_TO_LEFT_END, LEFT_END_ICON) { public void actionPerformed(ActionEvent e) {moveSequenceToLeftEnd(true);} };
        moveToRightEndAction=new AbstractAction(ACTION_MOVE_TO_RIGHT_END, RIGHT_END_ICON) { public void actionPerformed(ActionEvent e) {moveSequenceToRightEnd(true);} };
        
        flipOrientationAction=new AbstractAction(ACTION_ORIENTATION_FLIP, FLIP_ICON) { public void actionPerformed(ActionEvent e) {flipSequenceStrand(true);} };
        directOrientationAction=new AbstractAction(ACTION_ORIENTATION_DIRECT, null) { public void actionPerformed(ActionEvent e) {showSequenceDirectStrand(true);} };
        reverseOrientationAction=new AbstractAction(ACTION_ORIENTATION_REVERSE, null) { public void actionPerformed(ActionEvent e) {showSequenceReverseStrand(true);} };
        fromGeneOrientationAction=new AbstractAction(ACTION_ORIENTATION_RELATIVE, null) { public void actionPerformed(ActionEvent e) {showSequenceStrandFromGene(true);} };

        alignLeftAction=new AbstractAction(ACTION_ALIGN_LEFT, ALIGN_LEFT_ICON) { public void actionPerformed(ActionEvent e) {VisualizationPanel parent=getVisualizationPanel(); if (parent!=null) parent.alignAllSequencesLeft(); }};
        alignRightAction=new AbstractAction(ACTION_ALIGN_RIGHT, ALIGN_RIGHT_ICON) { public void actionPerformed(ActionEvent e) {VisualizationPanel parent=getVisualizationPanel(); if (parent!=null) parent.alignAllSequencesRight(); }};
        alignTSSRightAction=new AbstractAction(ACTION_ALIGN_TSS_RIGHT, ALIGN_TSS_ICON) { public void actionPerformed(ActionEvent e) {VisualizationPanel parent=getVisualizationPanel(); if (parent!=null) parent.alignAllSequencesToTSSRight(); }};
        alignNoneAction=new AbstractAction(ACTION_ALIGN_NONE, ALIGN_NONE_ICON) { public void actionPerformed(ActionEvent e) {VisualizationPanel parent=getVisualizationPanel(); if (parent!=null) parent.alignAllSequencesNone(); }};
        
        copySequenceAction=new AbstractAction(ACTION_COPY_SEQUENCE, null) { public void actionPerformed(ActionEvent e) {copySequenceToClipboard();}};
       
        constrainAction=new AbstractAction(ACTION_CONSTRAIN, CONSTRAIN_ICON) { 
            public void actionPerformed(ActionEvent e) {
                boolean current=settings.isSequenceConstrained(sequenceName); 
                constrainSequence(!current,true);
            } 
        };
        scrollLockAction=new AbstractAction(ACTION_SCROLL_LOCK, LOCK_ICON){ 
            public void actionPerformed(ActionEvent e) {
                boolean current=settings.isSequenceScrollLocked(sequenceName);                 
                scrollLockSequence(!current,true);
            } 
        };        
        moveSequenceUpAction=new AbstractAction(ACTION_REORDER_MOVE_UP, null){ 
            public void actionPerformed(ActionEvent e) {reorderSequencesMoveUp();}
            @Override 
            public boolean isEnabled () {
               SequenceCollection dataset = settings.getEngine().getDefaultSequenceCollection();
               if (dataset==null) return false;
               return (dataset.getIndexForSequence(sequenceName)>0);
            };
        };

        moveSequenceDownAction=new AbstractAction(ACTION_REORDER_MOVE_DOWN, null){ 
            public void actionPerformed(ActionEvent e) {reorderSequencesMoveDown();}          
            @Override 
            public boolean isEnabled () {
               SequenceCollection dataset = settings.getEngine().getDefaultSequenceCollection();
               if (dataset==null) return false;
               return (dataset.getIndexForSequence(sequenceName)<dataset.getNumberofSequences()-1);
            };
        };
        
        moveSequenceToTopAction=new AbstractAction(ACTION_REORDER_MOVE_TO_TOP, null){ 
            public void actionPerformed(ActionEvent e) {reorderSequencesMoveToTop();}
        };

        moveSequenceToBottomAction=new AbstractAction(ACTION_REORDER_MOVE_TO_BOTTOM, null){ 
            public void actionPerformed(ActionEvent e) {reorderSequencesMoveToBottom();}
        };       

        hideSequenceAction=new AbstractAction(ACTION_HIDE_SEQUENCE, null){
            public void actionPerformed(ActionEvent e) {
                transferFocus();
                settings.setSequenceVisible(sequenceName, false);
            }
        };
        
        hideOtherSequencesAction=new AbstractAction(ACTION_HIDE_OTHER_SEQUENCES, null){
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> seqList=settings.getEngine().getDefaultSequenceCollection().getAllSequenceNames();
                seqList.remove(sequenceName);
                String[] seqAsArray=new String[seqList.size()];
                settings.setSequenceVisible(seqList.toArray(seqAsArray), false);
            }
        };
        hideAllSequencesAction=new AbstractAction(ACTION_HIDE_ALL_SEQUENCES, null){
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> seqList=settings.getEngine().getDefaultSequenceCollection().getAllSequenceNames();
                String[] seqAsArray=new String[seqList.size()];
                settings.setSequenceVisible(seqList.toArray(seqAsArray), false);
            }
        };     
        showAllSequencesAction=new AbstractAction(ACTION_SHOW_ALL_SEQUENCES, null){
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> seqList=settings.getEngine().getDefaultSequenceCollection().getAllSequenceNames();
                String[] seqAsArray=new String[seqList.size()];
                settings.setSequenceVisible(seqList.toArray(seqAsArray), true);
            }
        };         
        
        InputMap inputmap=this.getInputMap();
        ActionMap actionmap=this.getActionMap();       
        
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), ACTION_ZOOM_IN);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), ACTION_ZOOM_IN);
        actionmap.put(ACTION_ZOOM_IN, zoomInAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0), ACTION_SET_VIEWPORT);
        actionmap.put(ACTION_SET_VIEWPORT, setViewportAction);       
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), ACTION_ZOOM_OUT);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), ACTION_ZOOM_OUT);
        actionmap.put(ACTION_ZOOM_OUT, zoomOutAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACTION_ZOOM_RESET);
        actionmap.put(ACTION_ZOOM_RESET, zoomResetAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, 0), ACTION_ZOOM_TO_FIT);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, 0), ACTION_ZOOM_TO_FIT);
        actionmap.put(ACTION_ZOOM_TO_FIT, zoomToFitAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0), ACTION_ZOOM_TO_CUSTOM_LEVEL);
        actionmap.put(ACTION_ZOOM_TO_CUSTOM_LEVEL, zoomToCustomAction);
                                      
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), ACTION_MOVE_LEFT);
        actionmap.put(ACTION_MOVE_LEFT, moveLeftAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), ACTION_MOVE_RIGHT);
        actionmap.put(ACTION_MOVE_RIGHT, moveRightAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK), ACTION_MOVE_TO_LEFT_END);
        actionmap.put(ACTION_MOVE_TO_LEFT_END, moveToLeftEndAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK), ACTION_MOVE_TO_RIGHT_END);
        actionmap.put(ACTION_MOVE_TO_RIGHT_END, moveToRightEndAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0), ACTION_ORIENTATION_FLIP);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, 0), ACTION_ORIENTATION_FLIP);
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), ACTION_ORIENTATION_FLIP);
        actionmap.put(ACTION_ORIENTATION_FLIP, flipOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), ACTION_ORIENTATION_DIRECT);
        actionmap.put(ACTION_ORIENTATION_DIRECT, directOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), ACTION_ORIENTATION_REVERSE);
        actionmap.put(ACTION_ORIENTATION_REVERSE, reverseOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), ACTION_ORIENTATION_RELATIVE);
        actionmap.put(ACTION_ORIENTATION_RELATIVE, fromGeneOrientationAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,0), ACTION_CONSTRAIN);
        actionmap.put(ACTION_CONSTRAIN, constrainAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK), ACTION_SHOW_ALL_SEQUENCES);
        actionmap.put(ACTION_SHOW_ALL_SEQUENCES, showAllSequencesAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), ACTION_HIDE_SEQUENCE);
        actionmap.put(ACTION_HIDE_SEQUENCE, hideSequenceAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.SHIFT_DOWN_MASK), ACTION_HIDE_ALL_SEQUENCES);
        actionmap.put(ACTION_HIDE_ALL_SEQUENCES, hideAllSequencesAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0), ACTION_HIDE_OTHER_SEQUENCES);
        actionmap.put(ACTION_HIDE_OTHER_SEQUENCES, hideOtherSequencesAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), ACTION_REORDER_MOVE_UP);
        actionmap.put(ACTION_REORDER_MOVE_UP, moveSequenceUpAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), ACTION_REORDER_MOVE_DOWN);
        actionmap.put(ACTION_REORDER_MOVE_DOWN, moveSequenceDownAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK), ACTION_REORDER_MOVE_TO_TOP);
        actionmap.put(ACTION_REORDER_MOVE_TO_TOP, moveSequenceToTopAction);

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK+InputEvent.SHIFT_DOWN_MASK), ACTION_REORDER_MOVE_TO_BOTTOM);
        actionmap.put(ACTION_REORDER_MOVE_TO_BOTTOM, moveSequenceToBottomAction);        

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), ACTION_SELECT_ALL);
        actionmap.put(ACTION_SELECT_ALL, selectAllAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0), ACTION_INVERT_SELECTION);
        actionmap.put(ACTION_INVERT_SELECTION, invertSelectionAction);
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Shift Focus Next");
        actionmap.put("Shift Focus Next", new AbstractAction() {public void actionPerformed(ActionEvent e) {transferFocus();} });

        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Shift Focus Previous");
        actionmap.put("Shift Focus Previous", new AbstractAction() {public void actionPerformed(ActionEvent e) {transferFocusBackward();} });
     
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "Mark Sequence");
        actionmap.put("Mark Sequence", new AbstractAction() {public void actionPerformed(ActionEvent e) {markSequence();} });
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0), "Next Region");
        actionmap.put("Next Region", new AbstractAction() {public void actionPerformed(ActionEvent e) {goToNextRegion();} });
        
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0), "Previous Region");
        actionmap.put("Previous Region", new AbstractAction() {public void actionPerformed(ActionEvent e) {goToPreviousRegion();} });        
             
        inputmap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), ACTION_COPY_SEQUENCE);
        actionmap.put(ACTION_COPY_SEQUENCE, copySequenceAction);

    }
    
    /** This method can be used to enable or disable all actions registered in this SequenceVisualizer */
    public void setEnabledOnAllActions(boolean enabled) {
        ActionMap actionmap=this.getActionMap();
        Object[] keys=actionmap.keys();
        for (Object key:keys) actionmap.get(key).setEnabled(enabled);
    }

    /** Shows a prompt dialog which lets the user edit the properties of the argument region
     *  and commits the update via the GUI
     */
    public void editRegion(Region editregion) {
         boolean doupdate=false;
         FeatureSequenceData seq=editregion.getParent();
         if (seq==null) return;
         FeatureDataset dataset=seq.getParent();
         if (dataset==null) return;
         Region originalclone=editregion.clone();
         Region updatedRegion=editregion.clone();
         EditRegionPropertiesDialog propertiesdialog=new EditRegionPropertiesDialog(updatedRegion, gui);
         propertiesdialog.setLocation(gui.getFrame().getWidth()/2-propertiesdialog.getWidth()/2, gui.getFrame().getHeight()/2-propertiesdialog.getHeight()/2);
         propertiesdialog.setVisible(true);
         if (propertiesdialog.okPressed()) doupdate=true;
         propertiesdialog.dispose();
         if (originalclone.isIdenticalTo(updatedRegion)) doupdate=false;
         if (doupdate) {
            gui.updatePartialDataItem(dataset.getName(),seq.getSequenceName(),originalclone,updatedRegion);
            ((DataTrackVisualizer_Region)getTrackVisualizer(dataset.getName())).setCurrentRegion(null); // invalidate current selected region to avaoid problems
         }
         SequenceVisualizer.this.requestFocusInWindow();
    }
    
    /** toggles the "mark" property of the selected region
     *  and commits the update via the GUI
     */
    public void toggleRegionMark(Region region) {
         FeatureSequenceData seq=region.getParent();
         if (seq==null) return;
         FeatureDataset dataset=seq.getParent();
         if (dataset==null) return;
         // Region originalclone=region.clone();
         Region updatedRegion=region.clone();
         Boolean mark=(Boolean)region.getPropertyAsType("mark",Boolean.class);
         if (Boolean.TRUE==mark) mark=Boolean.FALSE; else mark=Boolean.TRUE;
         if (mark) updatedRegion.setProperty("mark", mark);
         else updatedRegion.setProperty("mark", null); // this will remove the property
         gui.updatePartialDataItem(dataset.getName(),seq.getSequenceName(),region,updatedRegion);
         ((DataTrackVisualizer_Region)getTrackVisualizer(dataset.getName())).setCurrentRegion(null); // invalidate current selected region to avaoid problems         
         SequenceVisualizer.this.requestFocusInWindow();
    }    
    
    private void markSequence() {
        if (settings.isSequenceMarked(sequenceName)) settings.setSequenceMarked(sequenceName, false);  
        else settings.setSequenceMarked(sequenceName, true);
        SequenceVisualizer.this.repaint();
    }
      
    private void goToNextRegion() {
        if (lastSelectedTrack!=null) {
            DataTrackVisualizer viz=getTrackVisualizer(lastSelectedTrack);
            if (viz instanceof DataTrackVisualizer_Region) {
                Region region = ((DataTrackVisualizer_Region)viz).goToNextRegion();
                if (region!=null) repositionToRegion(region);
            }
        }
    }
    
    private void goToPreviousRegion() {
        if (lastSelectedTrack!=null) {
            DataTrackVisualizer viz=getTrackVisualizer(lastSelectedTrack);
             if (viz instanceof DataTrackVisualizer_Region) {
                 Region region = ((DataTrackVisualizer_Region)viz).goToPreviousRegion();
                 if (region!=null) repositionToRegion(region);
             }
        }
    } 
    
    /*
    * Move the viewport so that the region is (at least partly) inside the viewport
    */
    private void repositionToRegion(Region region) {
        int vpStart = settings.getSequenceViewPortStart(sequenceName);
        int vpEnd = settings.getSequenceViewPortEnd(sequenceName);
        int regionStart = region.getGenomicStart();
        int regionEnd = region.getGenomicEnd();
        int vpSize = vpEnd-vpStart+1;
        int regionSize = regionEnd-regionStart+1;           
        if (regionSize<vpSize) { // center the region in the viewport
            int regionCenter = (int)(regionStart + (regionSize/2.0));
            vpStart = (int)(regionCenter - (vpSize/2.0));
            settings.setSequenceViewPort(sequenceName, vpStart, vpStart+vpSize-1);
        } else {
            settings.setSequenceViewPort(sequenceName, regionStart, regionStart+vpSize-1);
        } 
        settings.redraw();
    }
    
    /**
     * Finds the first DNA sequence track and copies the DNA sequence(s) from the selected segments to the clipboard (in the orientation shown).
     * If multiple selections are made, the sequences will be added separated by newlines
     */
    private void copySequenceToClipboard() { //    
        ArrayList<SelectionWindow>selections=settings.getSelectionWindows(sequenceName);
        if (selections==null || selections.isEmpty()) return; 
        DNASequenceDataset dnadataset=gui.getFeaturesPanel().getReferenceDNAtrack(null);
        if (dnadataset==null) return;
        DNASequenceData sequence=(DNASequenceData)dnadataset.getSequenceByName(sequenceName); 
        StringBuilder builder=new StringBuilder();
        int index=0;
        for (SelectionWindow window:selections) {
            int length=window.end-window.start+1;
            char[] dnasequence=new char[length];
            for (int i=0;i<length;i++) {
                dnasequence[i]=sequence.getValueAtGenomicPosition(window.start+i);   
            }
            if (settings.getSequenceOrientation(sequenceName)==Sequence.REVERSE) dnasequence=MotifLabEngine.reverseSequence(dnasequence);
            if (index>0) builder.append("\n");
            builder.append(dnasequence);
            index++;                  
        }
        String content=builder.toString();
        gui.logMessage("DNA sequence copied to clipboard: "+(selections.size()>1?"\n":"")+content, 8);         
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content),null);        
    }    
    
// =========================================  INNER CLASSES BELOW THIS LINE  ==================================    
    
    
    
    
    /**
     * A class used to highlight selected areas of sequences.
     * Highlighting is accomplished by overlaying a transparent rectangle over the selected parts of the sequence. 
     * The currently selected area is specified with two "anchors". The user presses the mouse button 
     * at some chosen position to set the first anchor and then drags the mouse to mark a region. 
     * The second anchor is set to the position where the user releases the mouse button. 
     * Positions are always given in genomic coordinates (within a chromosome) 
     * The coordinate of the second anchor can be either larger or smaller than the first depending on
     * which direction the user drags the mouse in, however, the "start" and "end" positions of the selected
     * area are given so that selectionStart is lower or equal to than selectionEnd. 
     *
     */
    private class SelectionPane extends JPanel {
        private AlphaComposite composite;
        private AlphaComposite erasingComposite;
        private int anchor1=-1;   // position of first placed mark! (genomic coordinate)
        private int anchor2=-1;   // position of second placed mark! (genomic coordinate)
        private int currentPosition=0;
        private int firstAnchorPos=0; // screen coordinate of first placed mark
        private boolean isErasing=false;
        
        public SelectionPane() {
            setFocusable(false);
            setOpaque(false);
            composite=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f);
            erasingComposite=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.2f);
        }
        
        /** This method clears the selection in this sequence (and all others!)*/
        public void clearSelection() {            
            if (settings.getSelectionWindows()==null) setVisible(false);
            settings.setSequenceWithSelection(null);
            anchor1=-1; //
            anchor2=-1;
        }
        
        /** Sets the selected region directly. start and end should be genomic coordinates */
        public void setSelectedRegion(int start,int end) {
            setVisible(true);
            settings.setSequenceWithSelection(sequenceName);
            anchor1=start;
            anchor2=end;
        }
        
        /** Selects the whole sequence window */
        public void selectAll() {        
            setVisible(true);
            settings.clearSelectionWindows(sequenceName);
            settings.addSelectionWindow(sequenceName, datasequence.getRegionStart(), datasequence.getRegionEnd());
        }
        
        /** Selects the whole sequence window */
        public void invertSelection() {        
            ArrayList<SelectionWindow> windows=settings.getSelectionWindows(sequenceName);
            if (windows==null || windows.isEmpty()) {
                selectAll();
                return;
            }
            settings.clearSelectionWindows(sequenceName);
            int start=datasequence.getRegionStart();
            int end=datasequence.getRegionEnd();
            Collections.sort(windows, new Comparator<SelectionWindow>() {
                @Override
                public int compare(SelectionWindow o1, SelectionWindow o2) {
                    if (o1.start<o2.start) return -1;
                    else if (o2.start<o1.start) return 1;
                    else return 0;
                }            
            });
            int count=0;
            if (windows.get(0).start>start) {settings.addSelectionWindow(sequenceName, start, windows.get(0).start-1);count++;}
            for (int i=0;i<windows.size()-1;i++) { // go through all but the last window
                int wend=windows.get(i).end;
                int wstart=windows.get(i+1).start;
                settings.addSelectionWindow(sequenceName, wend+1, wstart-1);
                count++;
            }
            if (windows.get(windows.size()-1).end<end) {settings.addSelectionWindow(sequenceName, windows.get(windows.size()-1).end+1, end);count++;}
            setVisible(count>0);
        }        
        
        /** Sets the first anchor to a genomic coordinate based on the relative position within the window given by the argument
         * (specifically, if single screen pixel represents multiple sequence bases, the first anchor should be set to the first (leftmost) of these)
         * However, note that the first anchor might change later depending on the position of the second anchor relative to the first (when 1 pixel represents multiple bases)
         * @param pos Position of anchor within the window (first position within window is pos=0)
         */
        public void setFirstAnchor(int pos, boolean erase) {
            isErasing=erase;
            int largest=settings.getSequenceWindowSize();
            if (pos<0) pos=0;
            if (pos>=largest) pos=largest;
            firstAnchorPos=pos;
            anchor1=getGenomicCoordinateFromScreenOffset(pos)[0]; // this might change later depending on relative position of anchor2
        }

        /** Sets the second anchor to a genomic coordinate based on the relative position within the window given by the argument
         *  If scale is such that 1 pixel represents several bases, the anchors are selected in such a way as to maximize the span of the selection.
         *  I.e. if first anchor lies to the left of anchor right (Direct orientation) then the leftmost base in firstAnchorPos and the rightmost base in second position (this pos) is used.
         *  However, if the second anchor is to the left, the rightmost base of the first anchor and the leftmost of the second is used.
         * @param pos Position of anchor within the window (first position within window is pos=0)
         */        
        public void setSecondAnchor(int pos, boolean erase) {
            isErasing=erase;
            setVisible(true);
            int largest=settings.getSequenceWindowSize()-1;
            if (pos<0) pos=0;
            if (pos>=largest) pos=largest;
            int[] firstAnchorGenomic=getGenomicCoordinateFromScreenOffset(firstAnchorPos);
            int[] currentPosGenomic=getGenomicCoordinateFromScreenOffset(pos);
            if (firstAnchorGenomic[0]<=currentPosGenomic[0]) {
                anchor1=firstAnchorGenomic[0];
                anchor2=currentPosGenomic[1];
            } else {
                anchor1=firstAnchorGenomic[1];
                anchor2=currentPosGenomic[0];
            }
            int VPstart=settings.getSequenceViewPortStart(sequenceName);
            int VPend=settings.getSequenceViewPortEnd(sequenceName);
            if (anchor1<VPstart) anchor1=VPstart; // just in case
            else if (anchor1>VPend) anchor1=VPend; // just in case
            if (anchor2<VPstart) anchor2=VPstart; // just in case
            else if (anchor2>VPend) anchor2=VPend; // just in case
            String selectednow=settings.getSequenceWithSelection();
            if (selectednow==null || !selectednow.equals(sequenceName)) settings.setSequenceWithSelection(sequenceName); // now there is no doubt that this sequence is selected
            repaint();
        }

        /**
         * Updates the current (genomic) position as the mouse is moved/dragged
         * @param pos (relative position within sequence window!)
         */
        public void setCurrentPosition(int pos, boolean erase) {
            isErasing=erase;
            setVisible(true);
            int largest=settings.getSequenceWindowSize()-1;
            if (pos<0) pos=0;
            if (pos>=largest) pos=largest;
            int[] firstAnchorGenomic=getGenomicCoordinateFromScreenOffset(firstAnchorPos);
            int[] currentPosGenomic=getGenomicCoordinateFromScreenOffset(pos);
            if (firstAnchorGenomic[0]<=currentPosGenomic[0]) {
                anchor1=firstAnchorGenomic[0];
                currentPosition=currentPosGenomic[1];
            } else {
                anchor1=firstAnchorGenomic[1];
                currentPosition=currentPosGenomic[0];
            }
            repaint();
        }

        /** Sets the genomic position of the base cursor */
        public void setBaseCursorPosition(int pos) {
            currentPosition=pos;
        }

        /** Returns the start position of the selection (smallest of the two anchors) if this sequence currently has a selection, else -1*/
        public int getSelectionStartPosition() {
            String selectedSequence=settings.getSequenceWithSelection();
            if (selectedSequence==null || !selectedSequence.equals(sequenceName)) return -1; 
            return (anchor1<anchor2)?anchor1:anchor2;
        }
        
        /** Returns the end position of the selection (largest of the two anchors) if this sequence currently has a selection, else -1*/
        public int getSelectionEndPosition() {
            String selectedSequence=settings.getSequenceWithSelection();
            if (selectedSequence==null || !selectedSequence.equals(sequenceName)) return -1; 
            return (anchor1>anchor2)?anchor1:anchor2;
        }
        
             
        /**
         * Paints the selection area using a translucent rectangle
         * @param g
         */
        @Override
        public void paintComponent(Graphics g) {
            String selectedSequence=settings.getSequenceWithSelection(); // the name of the sequence which is currently undergoing a selection
            boolean thisSequenceIsUnderSelection=(selectedSequence!=null && selectedSequence.equals(sequenceName));               
            ArrayList<SelectionWindow>selections=settings.getSelectionWindows();
            boolean paintCursorForThisSequence=(visualizeBaseCursorOptionSelected && visualizeBaseCursor && !gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL) && !isPaintingForPrint());
            if (selections==null && !thisSequenceIsUnderSelection && !paintCursorForThisSequence) return; // nothing to draw...
            int rulerHeight=(condensedmode)?bordersize:ruler.getHeight();
            int height=getTotalTracksHeight();
            int orientation=settings.getSequenceOrientation(sequenceName); 
            Graphics2D g2=(Graphics2D) g;
            Composite oldComposite=g2.getComposite();
            int currentSelectionStartPosition=0; // position relative to component
            int currentSelectionEndPosition=0;
            if (thisSequenceIsUnderSelection) {
                int genomicAnchor1=(anchor1<anchor2)?anchor1:anchor2; // smallest genomic anchor
                int genomicAnchor2=(anchor1>anchor2)?anchor1:anchor2; // largest genomic anchor
                if (orientation==VisualizationSettings.DIRECT) {
                    currentSelectionStartPosition=getScreenCoordinateFromGenomic(genomicAnchor1)[0];
                    currentSelectionEndPosition=getScreenCoordinateFromGenomic(genomicAnchor2)[1];
                } else {
                    currentSelectionStartPosition=getScreenCoordinateFromGenomic(genomicAnchor2)[0];                
                    currentSelectionEndPosition=getScreenCoordinateFromGenomic(genomicAnchor1)[1];
                }
                currentSelectionStartPosition+=bordersize; currentSelectionEndPosition+=bordersize; 
            }
            // first draw submitted selections
            if (selections!=null) {
                for (SelectionWindow window:selections) {
                    if (window.sequenceName.equals(sequenceName)) {
                        int startPosition=0; // position relative to component
                        int endPosition=0; // position relative to component. 
                        if (orientation==VisualizationSettings.DIRECT) {
                            startPosition=getScreenCoordinateFromGenomic(window.start)[0];
                            endPosition=getScreenCoordinateFromGenomic(window.end)[1];
                        } else {
                            startPosition=getScreenCoordinateFromGenomic(window.end)[0];                
                            endPosition=getScreenCoordinateFromGenomic(window.start)[1];
                        }
                        startPosition+=bordersize; endPosition+=bordersize;
                        if (!isErasing || currentSelectionEndPosition<startPosition || currentSelectionStartPosition>endPosition) { // draw full selection window
                            g.setColor(Color.orange);
                            g.drawLine(startPosition, rulerHeight, startPosition, rulerHeight+height-3); // bounding box
                            g.drawLine(endPosition, rulerHeight, endPosition, rulerHeight+height-3); // bounding box
                            g2.setComposite(composite);
                            g2.setColor(Color.yellow);
                            g2.fillRect(startPosition, rulerHeight, endPosition-startPosition+1, height-2);
                        } else if (isErasing && currentSelectionStartPosition<=startPosition && currentSelectionEndPosition<endPosition) { // erasing left end of window
                            startPosition=currentSelectionEndPosition+1;
                            g.setColor(Color.orange);
                            //g.drawLine(startPosition, rulerHeight, startPosition, rulerHeight+height-3); // bounding box
                            g.drawLine(endPosition, rulerHeight, endPosition, rulerHeight+height-3); // bounding box
                            g2.setComposite(composite);
                            g2.setColor(Color.yellow);
                            g2.fillRect(startPosition, rulerHeight, endPosition-startPosition+1, height-2);
                        } else if (isErasing && currentSelectionStartPosition>startPosition && currentSelectionEndPosition>=endPosition) { // erasing right end of window
                            endPosition=currentSelectionStartPosition-1;
                            g.setColor(Color.orange);
                            g.drawLine(startPosition, rulerHeight, startPosition, rulerHeight+height-3); // bounding box
                            //g.drawLine(endPosition, rulerHeight, endPosition, rulerHeight+height-3); // bounding box
                            g2.setComposite(composite);
                            g2.setColor(Color.yellow);
                            g2.fillRect(startPosition, rulerHeight, endPosition-startPosition+1, height-2);
                        } else if (isErasing && currentSelectionStartPosition>startPosition && currentSelectionEndPosition<endPosition) { // erasing middle window -> split in 2
                            g.setColor(Color.orange);
                            g.drawLine(startPosition, rulerHeight, startPosition, rulerHeight+height-3); // bounding box
                            //g.drawLine(currentSelectionStartPosition-1, rulerHeight, currentSelectionStartPosition-1, rulerHeight+height-3); // bounding box
                            //g.drawLine(currentSelectionEndPosition+1, rulerHeight, currentSelectionEndPosition+1, rulerHeight+height-3); // bounding box
                            g.drawLine(endPosition, rulerHeight, endPosition, rulerHeight+height-3); // bounding box
                            g2.setComposite(composite);
                            g2.setColor(Color.yellow);
                            g2.fillRect(startPosition, rulerHeight, currentSelectionStartPosition-startPosition, height-2);
                            g2.fillRect(currentSelectionEndPosition+1, rulerHeight, endPosition-currentSelectionEndPosition, height-2);
                        }
                    }
                }
            }
            // now draw currently updating selection
            if (thisSequenceIsUnderSelection) {
                //g.setColor((isErasing)?Color.red:Color.orange);
                g.setColor(Color.orange);
                g.drawLine(currentSelectionStartPosition, rulerHeight, currentSelectionStartPosition, rulerHeight+height-3); // bounding box
                g.drawLine(currentSelectionEndPosition, rulerHeight, currentSelectionEndPosition, rulerHeight+height-3); // bounding box
                g.setColor(Color.black);
                g.drawLine(currentSelectionStartPosition, rulerHeight-5, currentSelectionStartPosition, rulerHeight-1); // ticks
                g.drawLine(currentSelectionEndPosition, rulerHeight-5, currentSelectionEndPosition, rulerHeight-1); // ticks                 
                if (isErasing) g2.setComposite(erasingComposite); else g2.setComposite(composite);
                g2.setColor(Color.yellow);
                g2.fillRect(currentSelectionStartPosition, rulerHeight, currentSelectionEndPosition-currentSelectionStartPosition+1, height-2);
                //gui.debugMessage(sequenceName+": Selected "+genomicAnchor1+"-"+genomicAnchor2+"  which corresponds to "+startPosition+"-"+endPosition);
                
            }
            if (paintCursorForThisSequence) paintBaseCursor(g);
            g2.setComposite(oldComposite);
            // draw a 'border' at left and right ends of sequence window (or else the selection windows might paint over the track-borders)
            g.setColor(borderColor);
            int rightend=this.getWidth()-1;
            g.drawLine(0, rulerHeight, 0, rulerHeight+height-3); // 
            g.drawLine(rightend, rulerHeight, rightend, rulerHeight+height-3); //
        }
    
//        /** paints the base cursor (usually a pink line or rectangle at the position of the mouse) */
        private void paintBaseCursor(Graphics g){
            int[] pos=getScreenCoordinateFromGenomic(currentPosition);
            int startPosition=pos[0]; // position relative to component
            int endPosition=pos[1]; // position relative to component. 
            int rulerHeight=(condensedmode)?bordersize:ruler.getHeight();
            if (settings.getScale(sequenceName)<=0.33) endPosition=startPosition; // just draw a simple 1px line for small scales
            startPosition+=bordersize; endPosition+=bordersize; 
            int height=getTotalTracksHeight();
            Color color=settings.getBaseCursorColor();
            color=new Color(color.getRed(),color.getGreen(),color.getBlue(),128);            
            boolean filled=true;
            if (filled && endPosition>startPosition) {
                g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),40));
                g.fillRect(startPosition, rulerHeight, endPosition-startPosition+1, height-2); // bounding box
            }
            g.setColor(color);
            g.drawLine(startPosition, rulerHeight, startPosition, rulerHeight+height-3); // bounding box
            if (endPosition!=startPosition) g.drawLine(endPosition, rulerHeight, endPosition, rulerHeight+height-3); // bounding box            

            if (cursorFlanks>0) {

                int shownStrand=settings.getSequenceOrientation(sequenceName);
                int leftGenomicPosition=(shownStrand==Sequence.DIRECT)?(currentPosition-cursorFlanks):(currentPosition+cursorFlanks);
                int rightGenomicPosition=(shownStrand==Sequence.DIRECT)?(currentPosition+cursorFlanks):(currentPosition-cursorFlanks);
                int[] posLeft=getScreenCoordinateFromGenomic(leftGenomicPosition);
                int[] posRight=getScreenCoordinateFromGenomic(rightGenomicPosition);
                int left=(posLeft[0]<=posLeft[1])?posLeft[0]:posLeft[1];
                int right=(posRight[1]>=posRight[0])?posRight[1]:posRight[0];
                // Draw translucent overlay to highlight the region between cursor flanks
                g.setColor(translucent); 
                g.fillRect(left, rulerHeight,right-left+1,height-2); 
                // Draw edges of flankin region in dashed lines   
                g.setColor(Color.LIGHT_GRAY);
                Stroke def=((Graphics2D)g).getStroke();
                ((Graphics2D)g).setStroke(Graph.DASHED_STROKE);
                if (left>=0 && left!=startPosition && left!=endPosition) { // draw left edge of cursor flanks                  
                    g.drawLine(left, rulerHeight, left, rulerHeight+height-3);             
                }
                if (right<=tracksPanel.getWidth() && right!=startPosition && right!=endPosition) {  // draw right edge of cursor flanks   
                    g.drawLine(right, rulerHeight, right, rulerHeight+height-3);             
                }  
                ((Graphics2D)g).setStroke(def);               
            }           
        }        
    }
    
    /**
     * This private class renders the name of the sequence on the left side of the sequence window
     */
    private class SequenceLabel extends JLabel implements MouseInputListener {
        //private int arrowheadsize=4;
        private int minimumHeight=rulerHeight+16;
        private Dimension minSize=new Dimension(minimumHeight,minimumHeight);
                
        public SequenceLabel(String seqname) {
            super(seqname);
            setFocusable(false);
            setVerticalAlignment(JLabel.TOP);       
            addMouseListener(this);
            addMouseMotionListener(this);
            Dimension labelsize=new Dimension(getWidth(),minimumHeight);
            setMinimumSize(labelsize);
            setPreferredSize(labelsize);        
            addKeyListener(new KeyAdapter() {
                 @Override
                 public void keyPressed(KeyEvent e) {
                     getVisualizationPanel().notifyPanelListeners(SequenceLabel.this, new Data[]{datasequence}, e);               
                 }     
            });             
        }
    
        public int getMinimumHeight() {
            return minimumHeight;
        }
        
        @Override
        public final int getWidth() {
            int labelwidth=getFontMetrics(getFont()).stringWidth(sequenceName);
            if (labelwidth<20) labelwidth=20;
            return labelwidth+(labelLeftMargin+labelRightMargin);
        }
        
        @Override
        public Dimension getMinimumSize() {
          return minSize;
        }
        
        @Override
        public void paintComponent(Graphics g) { // paint SequenceLabel
            int align=settings.getSequenceLabelAlignment();
            if (align==SwingConstants.TRAILING) return; // the label should not be painted here        
            java.awt.Font font=getFont();
            java.awt.Rectangle bounds=font.getStringBounds(sequenceName, ((Graphics2D)g).getFontRenderContext()).getBounds();
            int sequenceNameWidth=bounds.width; 
            if (sequenceNameWidth<20) sequenceNameWidth=20; // just to have a minimum
            int labelwidth=getWidth()-(labelLeftMargin+10); // 10 is just a "right margin"
            int arrowWidth=(sequenceNameWidth<labelwidth)?sequenceNameWidth:labelwidth;
            int y=0;
                 if (align==SwingConstants.TOP) y=((condensedmode)?1:rulerHeight)+9;//+bounds.height/2;
            else if (align==SwingConstants.CENTER) { // "middle" alignment
                // this code has been updated so that in condensed mode, the middle of the tracks block align with the middle of the label, and in non-condensed mode, the arrow points to the middle of the tracks block
                int min=((condensedmode)?2:rulerHeight)+8;
                int blockheight=getTotalTracksHeight();  
                int labelheight=(condensedmode)?5:16;// this size works with the standard font size for labels
                if (condensedmode) y=2+(blockheight+labelheight)/2;
                else y=rulerHeight+(blockheight-labelheight)/2;
                if (y<min)y=min;
            } else y=((condensedmode)?1:rulerHeight)+9;//+bounds.height/2;  
            // paint mark
            if (settings.isSequenceMarked(sequenceName)) {
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.RED);
                g.fillOval(labelLeftMargin-8, y-8, 6, 6);
                g.setColor(Color.BLACK);
                g.drawOval(labelLeftMargin-8, y-8, 6, 6);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);         
            }                 
            // paint focus wedge
            if (SequenceVisualizer.this.hasFocus() && !isPaintingForPrint()) { 
                int[] xcoords=new int[]{labelLeftMargin-8,labelLeftMargin-6,labelLeftMargin-3,labelLeftMargin-3,labelLeftMargin-6,labelLeftMargin-8};
                int[] ycoords=new int[]{y-9,y-9,y-6,y-5,y-2,y-2};
                g.setColor(Color.YELLOW);   
                g.fillPolygon(xcoords, ycoords, xcoords.length);
                g.setColor(Color.BLACK); // outline                
                g.drawPolygon(xcoords,ycoords, xcoords.length);                
            }
            g.setFont(font);
            if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, MotifLabGUI.ANTIALIAS_MODE);            
            Color color=settings.getSequenceLabelColor(getText());
            g.setColor(color);
            g.drawString(getText(),labelLeftMargin,y);
            if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);            
            if (condensedmode) return;
            y+=bounds.height/2;
            arrowWidth-=3;
            int arrowLeftMargin=labelLeftMargin+1; // just to move the arrow 1px to the right (I forgot the border)
            int strand=datasequence.getStrandOrientation();
            boolean fromGeneOrientation=(strand==settings.getSequenceOrientation(sequenceName));
            int[] xcoords;
            int[] ycoords=new int[]{y-2,y-2,y-5,y,y+5,y+2,y+2};
            if (strand==VisualizationSettings.DIRECT) {
                xcoords=new int[]{arrowLeftMargin,arrowLeftMargin+arrowWidth-5,arrowLeftMargin+arrowWidth-5,arrowLeftMargin+arrowWidth,arrowLeftMargin+arrowWidth-5,arrowLeftMargin+arrowWidth-5,arrowLeftMargin};
            } else {
                xcoords=new int[]{arrowLeftMargin+arrowWidth,arrowLeftMargin+5,arrowLeftMargin+5,arrowLeftMargin,arrowLeftMargin+5,arrowLeftMargin+5,arrowLeftMargin+arrowWidth};                
            }          
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);              
            if (fromGeneOrientation) {g.setColor(Color.green);} else {g.setColor(Color.red);}
            g.fillPolygon(xcoords, ycoords, xcoords.length);            
            g.setColor(Color.black);
            g.drawPolygon(xcoords, ycoords, xcoords.length);
        }
        
        /** 
         * shows the Context Menu
         */
        private void showLabelContextMenu(MouseEvent e) {
            JPopupMenu popup=SequenceLabelContextMenu.getInstance(SequenceVisualizer.this, gui);
            if (popup!=null) {
                getVisualizationPanel().notifyPanelListeners((SequenceLabel)this, new Data[]{datasequence}, popup);
                popup.show(e.getComponent(), e.getX(),e.getY());
                SequenceVisualizer.this.repaint(); // in case some properties have been updated
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            SequenceVisualizer.this.requestFocusInWindow();
            gui.setSearchTargetComponent(null);            
            if (!getVisualizationPanel().isMouseEventsBlocked()) {
                if (e.getClickCount()==2) gui.showPrompt(datasequence, true, true);
            }
            redispatchMouseEvent(e);  
        }

        @Override
        public void mouseEntered(MouseEvent e) {                            
            setToolTipText(datasequence.getToolTip(gui.getEngine()));      
            SequenceVisualizer.this.requestFocusInWindow();
        }

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {
           if (!getVisualizationPanel().isMouseEventsBlocked()) {
              if (e.isPopupTrigger()) showLabelContextMenu(e);         
           }
           redispatchMouseEvent(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
           if (!getVisualizationPanel().isMouseEventsBlocked()) {
              if (e.isPopupTrigger()) showLabelContextMenu(e);         
           }
           redispatchMouseEvent(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            redispatchMouseEvent(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            redispatchMouseEvent(e);
        }
        
        
    }

    
    private class Ruler extends JPanel {
        private int rulerHeight;
        private int xoffset; // offset from the left where the ruler is to be placed (since some text might be rendered outside "bounds")
        private int minorTickHeight=2;
        private int majorTickHeight=5;
        private int tssTickHeight=12;
        private int tssTickWidth=14;
        private int rulerFontSize=9;
        private Font rulerFont=new Font(Font.SANS_SERIF,Font.PLAIN,rulerFontSize);
        private int[] allowedMinorTickIntervals={1,2,5,10,20,25,50,100,200,250,300,400,500,800,1000,1500,2000,3000,4000,5000,10000,15000,20000,25000,30000,40000,50000,100000,200000,250000,300000,400000,500000,1000000,2000000}; // Allowed values for minor tick intervals (in number of genomic bases). 
        private int[] TSSLabelPosition={0,0}; // start,end
        private int[] TESLabelPosition={0,0}; // start,end
        
        public Ruler(int rulerHeight) {
            super();
            setFocusable(false);
            this.rulerHeight=rulerHeight;
        }
        
        @Override 
        public int getHeight() {
            return rulerHeight+1;
        }
                
        /** given a minor tick interval candidate value, this will round off to closest allowed interval */
        private int getMinorTickIntervalToUse(double tickinterval) {
            for (int i=allowedMinorTickIntervals.length-1;i>=0;i--) {
                if (tickinterval>=allowedMinorTickIntervals[i]) return allowedMinorTickIntervals[i];
            }
            return 1;
        }
        
        /** checks if the given interval overlaps with the last registered TSS arrow and label */
        private boolean overlapsTSS(int start, int end) {
            if (end<TSSLabelPosition[0] || start>TSSLabelPosition[1]) return false;
            else return true;
        }
        
        /** checks if the given interval overlaps with the last registered TES arrow and label */
        private boolean overlapsTES(int start, int end) {
            if (end<TESLabelPosition[0] || start>TESLabelPosition[1]) return false;
            else return true;
        }
        
        
       /**
        * Paints the ruler
        * @param g
        */
        @Override
        public void paintComponent(Graphics g) { // paint ruler
            if (condensedmode) return;
            int vizOrientation=settings.getSequenceOrientation(sequenceName);
            int geneOrientation=datasequence.getStrandOrientation();
            xoffset=getLabelWidth();
            Integer TSS=datasequence.getTSS();
            Integer TES=datasequence.getTES();
            int width=settings.getSequenceWindowSize();
            String genename=datasequence.getGeneName();
            if (genename==null) genename=datasequence.getSequenceName();
            String rulerType=settings.getRuler(null);
            double scale=settings.getScale(sequenceName);
            int minorTickSpacing=10; // approximately 10 pixels between minor ticks as a general guideline
            double minorTickIntervalPrecise=minorTickSpacing/scale; // how many genomic bases pr minor tick if a minor tick is 10 pixels?
            int minorTickInterval=getMinorTickIntervalToUse(minorTickIntervalPrecise); // genomic bases pr minor tick - ajusted to a "sensible" number (from a preset list)
            int majorTickInterval=minorTickInterval*10;
            //gui.debugMessage(sequenceName+": Minor interval="+minorTickInterval+" candidate("+minorTickIntervalPrecise+")  major="+majorTickInterval);
            g.setColor(Color.black);
            g.setFont(rulerFont);
            g.drawLine(xoffset,rulerHeight,xoffset+width,rulerHeight); // draws horizontal line the width of the window
            //int genomicAnchor=(vizOrientation==VisualizationSettings.DIRECT)?datasequence.getRegionStart():datasequence.getRegionEnd();
            int VPStart=settings.getSequenceViewPortStart(sequenceName);
            int VPEnd=settings.getSequenceViewPortEnd(sequenceName);
            if (VPEnd-VPStart+1<=10) {minorTickInterval=1;majorTickInterval=1;}
//            Rectangle2D r=g.getFontMetrics().getStringBounds(genename, g);
//            boolean labelcrash=false;
//            if (TSS!=null && TES!=null) { // don't draw TES label if the TSS and TSS labels will crash
//                int TSSlabelwidth=(int)r.getWidth()+16+5; // 16 is width of the arrow, 5 is a margin
//                int genespan=(TES>TSS)?TES-TSS:TSS-TES;
//                genespan=(int)(genespan*scale);
//                if (TSSlabelwidth*2>genespan) labelcrash=true;
//            }
            drawTSS(g,vizOrientation,geneOrientation,genename, TSS);  
            drawTES(g,vizOrientation,geneOrientation,genename, TES);  

            if (rulerType.equals(VisualizationSettings.RULER_NONE)) {
                return;                 
            } else if (rulerType.equals(VisualizationSettings.RULER_FIXED)) {
                drawFixedRuler(g, minorTickInterval, majorTickInterval, scale, width);           
            } else if (rulerType.equals(VisualizationSettings.RULER_CHROMOSOME)) {
                 drawGenomicRuler(g, vizOrientation, VPStart, VPEnd, minorTickInterval, majorTickInterval, (double)scale, width);
            } else if (rulerType.equals(VisualizationSettings.RULER_TSS)) {
                 drawTSSRuler(g, vizOrientation, geneOrientation, VPStart, VPEnd, minorTickInterval, majorTickInterval, (double)scale, width, TSS);         
            } else if (rulerType.equals(VisualizationSettings.RULER_TES)) {
                 drawTSSRuler(g, vizOrientation, geneOrientation, VPStart, VPEnd, minorTickInterval, majorTickInterval, (double)scale, width, TES);         
            } else if (rulerType.equals(VisualizationSettings.RULER_UPSTREAM_END)) {
                 Integer origo=(geneOrientation==VisualizationSettings.DIRECT)?datasequence.getRegionStart():datasequence.getRegionEnd();
                 drawTSSRuler(g, vizOrientation, geneOrientation, VPStart, VPEnd, minorTickInterval, majorTickInterval, (double)scale, width, origo);         
            } else if (rulerType.equals(VisualizationSettings.RULER_DOWNSTREAM_END)) {
                 Integer origo=(geneOrientation==VisualizationSettings.DIRECT)?datasequence.getRegionEnd():datasequence.getRegionStart();
                 drawTSSRuler(g, vizOrientation, geneOrientation, VPStart, VPEnd, minorTickInterval, majorTickInterval, (double)scale, width, origo);              
            } else {
              // I don't know ...
            } 
 
        
        } // end paintComponent
        
        /** 
         * Draws a ruler at appropriate scale whose origo is always at the left end of the sequence window 
         */
        private void drawFixedRuler(Graphics g, int minorTickInterval, int majorTickInterval, double scale, int width) {
                int useXoffset=xoffset+1;
                double i=minorTickInterval*scale; 
                while ((int)i<=width) {
                    int pos=useXoffset+(int)(i-scale); // "i-scale" is done to reposition the tick of base 1 to the start of the window (so that counting starts at 1 not 0)
                    g.drawLine(pos,rulerHeight-minorTickHeight,pos,rulerHeight);
                    i+=minorTickInterval*scale;
                }
                i=majorTickInterval*scale;
                while ((int)i<=width) {
                    int pos=useXoffset+(int)(i-scale); // "i-scale" is done to reposition tick 1 base (=scale pixels) to the left. So that counting starts at 1 not 0
                    //String tickLabel=""+((int)(i/scale));
                    String tickLabel=""+(int)Math.round(i/scale);
                    g.drawLine(pos,rulerHeight-majorTickHeight,pos,rulerHeight);
                    int tickLabelWidth=(int)rulerFont.getStringBounds(tickLabel, ((Graphics2D)g).getFontRenderContext()).getWidth();
                    if (!overlapsTSS(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2)) && !overlapsTES(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2))) drawString(g,tickLabel,pos-(tickLabelWidth/2),rulerHeight-(majorTickHeight+2));
                    i+=majorTickInterval*scale;
                }
                // now draw an extra major tick at position "1"
                g.drawLine(useXoffset,rulerHeight-majorTickHeight,useXoffset,rulerHeight);
                String tickLabel="1";
                int tickLabelWidth=(int)rulerFont.getStringBounds(tickLabel, ((Graphics2D)g).getFontRenderContext()).getWidth();
                if (!overlapsTSS(useXoffset-(tickLabelWidth/2),useXoffset+(tickLabelWidth/2)) && !overlapsTES(useXoffset-(tickLabelWidth/2),useXoffset+(tickLabelWidth/2))) drawString(g,tickLabel,useXoffset-(tickLabelWidth/2),rulerHeight-(majorTickHeight+2));
        }
        
        /** 
         * Draws a ruler at appropriate scale whose origo is at the beginning of the chromosome 
         */
        private void drawGenomicRuler(Graphics g, int vizOrientation, int VPStart, int VPEnd, int minorTickInterval, int majorTickInterval, double scale, int width) {
            if (vizOrientation==VisualizationSettings.DIRECT) {
                 int leftEnd=VPStart;
                 int offsetMinor=minorTickInterval-(int)Math.IEEEremainder(leftEnd,minorTickInterval); // offset in bases
                 int offsetMajor=majorTickInterval-(int)Math.IEEEremainder(leftEnd,majorTickInterval); // offset in bases
                 if (offsetMinor>=minorTickInterval) offsetMinor-=minorTickInterval;
                 if (offsetMajor>=majorTickInterval) offsetMajor-=majorTickInterval;      
                 if (minorTickInterval==1) offsetMinor=0; // ad hoc override
                 if (majorTickInterval==1) offsetMajor=0; // ad hoc override
                 double i=0; 
                 while (i+(offsetMinor*scale)<width) {
                    int pos=(int)(i+xoffset+(offsetMinor*scale)+1);
                    g.drawLine(pos,rulerHeight-minorTickHeight,pos,rulerHeight);
                    i+=minorTickInterval*scale;
                 }
                 i=0; 
                 while (i+(offsetMajor*scale)<width) {
                    int pos=(int)(i+xoffset+(offsetMajor*scale)+1);
                    String tickLabel=""+(int)Math.round(leftEnd+offsetMajor+(i/scale));
                    g.drawLine(pos,rulerHeight-majorTickHeight,pos,rulerHeight);
                    int tickLabelWidth=(int)rulerFont.getStringBounds(tickLabel, ((Graphics2D)g).getFontRenderContext()).getWidth();
                    if (!overlapsTSS(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2)) && !overlapsTES(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2))) drawString(g,tickLabel,pos-(tickLabelWidth/2),rulerHeight-(majorTickHeight+2));
                    i+=majorTickInterval*scale;
                 }
            } else { // sequence displayed on reverse strand
                 boolean placeTickAtRightEndOfBase=true;
                 int leftEnd=VPEnd;
                 int offsetMinor=(int)Math.IEEEremainder(leftEnd,minorTickInterval); // offset in bases
                 int offsetMajor=(int)Math.IEEEremainder(leftEnd,majorTickInterval); // offset in bases
                 if (offsetMinor<0) offsetMinor+=minorTickInterval;
                 if (offsetMajor<0) offsetMajor+=majorTickInterval;               
                 if (minorTickInterval==1) offsetMinor=0; // ad hoc override
                 if (majorTickInterval==1) offsetMajor=0; // ad hoc override
                 //gui.debugMessage(sequenceName+"  left="+leftEnd+"  minorTick="+minorTickInterval+"  MinorOffset="+offsetMinor+"  majorTick="+majorTickInterval+" MajorOffset="+offsetMajor);
                 double i=0; 
                 double smallvalue=1E-8;
                 double rightedge=(placeTickAtRightEndOfBase)?(scale-smallvalue):0; // use this to place tick at right end of base
                 while (i+(offsetMinor*scale)+rightedge<width) {
                    int pos=(int)(i+xoffset+(offsetMinor*scale)+rightedge+1);
                    g.drawLine(pos,rulerHeight-minorTickHeight,pos,rulerHeight);
                    i+=minorTickInterval*scale;
                 }
                 i=0; 
                 while (i+(offsetMajor*scale)+rightedge<width) {
                    int pos=(int)(i+xoffset+(offsetMajor*scale)+rightedge+1);                    
                    String tickLabel=""+(int)Math.round(leftEnd-(offsetMajor+(i/scale)));
                    g.drawLine(pos,rulerHeight-majorTickHeight,pos,rulerHeight);
                    int tickLabelWidth=(int)rulerFont.getStringBounds(tickLabel, ((Graphics2D)g).getFontRenderContext()).getWidth();
                    if (!overlapsTSS(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2)) && !overlapsTES(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2))) drawString(g,tickLabel,pos-(tickLabelWidth/2),rulerHeight-(majorTickHeight+2));
                    i+=majorTickInterval*scale;
                 }              
            }         
        }
 
        /** 
         * Draws a ruler at appropriate scale whose origo is at located at the TSS of the gene (or a similar anchor point, or at the upstream end if no TSS is specified)
         */
        private void drawTSSRuler(Graphics g, int vizOrientation, int geneOrientation, int VPStart, int VPEnd, int minorTickInterval, int majorTickInterval, double scale, int width, Integer TSSint) {
                 boolean skip0=gui.skipPosition0(); // if this flag is set, the ruler will skip directly from -1 to +1 without going through 0
                 int TSS=0;
                 if (TSSint!=null) TSS=TSSint.intValue();                
                 //else TSS=(geneOrientation==VisualizationSettings.DIRECT)?datasequence.getRegionEnd():datasequence.getRegionStart(); // if TSS is missing, use EDN of sequence as origo                      
                 else TSS=(geneOrientation==VisualizationSettings.DIRECT)?datasequence.getRegionStart():datasequence.getRegionEnd();   // if TSS is missing, use START of sequence as origo                      
                 // NOTE: the two lines above starting with 'else' (one is commented out) deals with how
                 // to position the origo when the TSS of the sequence is not specified
                 // the first line places origo at the end of the sequence (as though the sequence is UPSTREAM of an unspecified gene)
                 // the second line places origo at the start of the sequence
                 int leftEnd=0;
                 int rightEnd=0;
                 boolean orientedFromGene=(geneOrientation==vizOrientation);
                      if (geneOrientation==VisualizationSettings.DIRECT && vizOrientation==VisualizationSettings.DIRECT) {leftEnd=VPStart-TSS;rightEnd=VPEnd-TSS;}
                 else if (geneOrientation==VisualizationSettings.DIRECT && vizOrientation==VisualizationSettings.REVERSE) {leftEnd=VPEnd-TSS;rightEnd=VPStart-TSS;}
                 else if (geneOrientation==VisualizationSettings.REVERSE && vizOrientation==VisualizationSettings.DIRECT) {leftEnd=TSS-VPStart;rightEnd=TSS-VPEnd;}
                 else if (geneOrientation==VisualizationSettings.REVERSE && vizOrientation==VisualizationSettings.REVERSE) {leftEnd=TSS-VPEnd;rightEnd=TSS-VPStart;}
                 if (skip0 && leftEnd>=0) leftEnd++;   // because ruler jumps from -1 to +1 without going through 0
                 if (skip0 && rightEnd>=0) rightEnd++; // because ruler jumps from -1 to +1 without going through 0                 
                 boolean TSSwithinWindow=((leftEnd<=0 && rightEnd>=0) || (leftEnd>=0 && rightEnd<=0));
                 //gui.debugMessage(sequenceName+"[gene="+geneOrientation+"] viz=["+vizOrientation+"] TSS="+TSS+"  leftEnd="+leftEnd+"  rightEnd="+rightEnd);       
            
            if (orientedFromGene) { // ruler values increases from left to right
                 int offsetMinor=minorTickInterval-(int)Math.IEEEremainder(leftEnd,minorTickInterval); // offset in bases
                 int offsetMajor=majorTickInterval-(int)Math.IEEEremainder(leftEnd,majorTickInterval); // offset in bases
                 if (offsetMinor>=minorTickInterval) offsetMinor-=minorTickInterval;
                 if (offsetMajor>=majorTickInterval) offsetMajor-=majorTickInterval;      
                 if (minorTickInterval==1) offsetMinor=0; // ad hoc override
                 if (majorTickInterval==1) offsetMajor=0; // ad hoc override              
                 //gui.debugMessage(sequenceName+"  left="+leftEnd+"  minorTick="+minorTickInterval+"  MinorOffset="+offsetMinor+"  majorTick="+majorTickInterval+" MajorOffset="+offsetMajor);
                 double i=0; 
                 double skip0Offset=0;
                 double extend=(skip0 && TSSwithinWindow)?scale:0; // if we skip 0, positive ticks are offset to the left. The "extend" value extends window to the right so that all ticks are 
                 while (i+(offsetMinor*scale)<width+extend) {
                    if (vizOrientation==VisualizationSettings.DIRECT) {
                        if (skip0 && settings.getSequenceViewPortStart(sequenceName)<TSS  && (leftEnd+offsetMinor+(i/scale)>0)) skip0Offset=scale;else skip0Offset=0;
                    } else {// vizOrientation==REVERSE
                        if (skip0 && settings.getSequenceViewPortEnd(sequenceName)>TSS && (leftEnd+offsetMinor+(i/scale)>0)) skip0Offset=scale;else skip0Offset=0;                        
                    }
                    int pos=(int)(i+xoffset+(offsetMinor*scale)+1-skip0Offset);
                    if (pos<=xoffset) {i+=minorTickInterval*scale;continue;}
                    g.drawLine(pos,rulerHeight-minorTickHeight,pos,rulerHeight);
                    i+=minorTickInterval*scale;
                 }
                 i=0; 
                 while (i+(offsetMajor*scale)<width+extend) {
                    if (vizOrientation==VisualizationSettings.DIRECT) {
                        if (skip0 && settings.getSequenceViewPortStart(sequenceName)<TSS  && (leftEnd+offsetMajor+(i/scale)>0)) skip0Offset=scale;else skip0Offset=0;
                    } else { // vizOrientation==REVERSE
                        if (skip0 && settings.getSequenceViewPortEnd(sequenceName)>TSS && (leftEnd+offsetMajor+(i/scale)>0)) skip0Offset=scale;else skip0Offset=0;                        
                    }
                    int pos=(int)(i+xoffset+(offsetMajor*scale)+1-skip0Offset);
                    if (pos<=xoffset) {i+=majorTickInterval*scale;continue;}
                    int coordinate=(int)Math.round(leftEnd+offsetMajor+(i/scale));
                    boolean skipTick=(skip0 && coordinate==0);
                    if (!skipTick) {
                        String tickLabel=""+coordinate;
                        g.drawLine(pos,rulerHeight-majorTickHeight,pos,rulerHeight);
                        int tickLabelWidth=(int)rulerFont.getStringBounds(tickLabel, ((Graphics2D)g).getFontRenderContext()).getWidth();
                        if (!overlapsTSS(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2)) && !overlapsTES(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2))) drawString(g,tickLabel,pos-(tickLabelWidth/2),rulerHeight-(majorTickHeight+2));
                    }                    
                    i+=majorTickInterval*scale;
                 }
                 
            } else { // sequence displayed on strand opposite of gene. Ruler values increase right to left
                 boolean placeTickAtRightEndOfBase=true;
                 int offsetMinor=(int)Math.IEEEremainder(leftEnd,minorTickInterval); // offset in bases
                 int offsetMajor=(int)Math.IEEEremainder(leftEnd,majorTickInterval); // offset in bases
                 if (offsetMinor<0) offsetMinor+=minorTickInterval;
                 if (offsetMajor<0) offsetMajor+=majorTickInterval;               
                 if (minorTickInterval==1) offsetMinor=0; // ad hoc override
                 if (majorTickInterval==1) offsetMajor=0; // ad hoc override
                 //gui.debugMessage(sequenceName+"  left="+leftEnd+"  minorTick="+minorTickInterval+"  MinorOffset="+offsetMinor+"  majorTick="+majorTickInterval+" MajorOffset="+offsetMajor);
                 double i=0;
                 double skip0Offset=0;
                 double extend=(skip0 && TSSwithinWindow)?scale:0;
                 double smallvalue=1E-8;
                 double rightedge=(placeTickAtRightEndOfBase)?(scale-smallvalue):0; // use this to place tick at right end of base
                 while (i+(offsetMinor*scale)+rightedge<width+extend) {
                    if (vizOrientation==VisualizationSettings.DIRECT) {
                        if (skip0 && settings.getSequenceViewPortStart(sequenceName)<=TSS  && (leftEnd-(offsetMinor+i/scale)<=0)) skip0Offset=scale;else skip0Offset=0;
                    } else { // vizOrientation==REVERSE
                        if (skip0 && settings.getSequenceViewPortEnd(sequenceName)>=TSS && (leftEnd-(offsetMinor+i/scale)<=0)) skip0Offset=scale;else skip0Offset=0;                        
                    }
                    int pos=(int)(i+xoffset+(offsetMinor*scale)+rightedge+1-skip0Offset);
                    g.drawLine(pos,rulerHeight-minorTickHeight,pos,rulerHeight);
                    i+=minorTickInterval*scale;
                 }                 
                 i=0;  
                 while (i+(offsetMajor*scale)+rightedge<width+extend) {
                    if (vizOrientation==VisualizationSettings.DIRECT) {
                        if (skip0 && settings.getSequenceViewPortStart(sequenceName)<=TSS  && (leftEnd-(offsetMajor+i/scale)<=0)) skip0Offset=scale;else skip0Offset=0;
                    } else { // vizOrientation==REVERSE
                        if (skip0 && settings.getSequenceViewPortEnd(sequenceName)>=TSS && (leftEnd-(offsetMajor+i/scale)<=0)) skip0Offset=scale;else skip0Offset=0;                        
                    }
                    int pos=(int)(i+xoffset+(offsetMajor*scale)+rightedge+1-skip0Offset);
                    int coordinate=(int)Math.round(leftEnd-(offsetMajor+(i/scale)));
                    boolean skipTick=(skip0 && coordinate==0);
                    if (!skipTick) {
                        String tickLabel=""+coordinate;
                        g.drawLine(pos,rulerHeight-majorTickHeight,pos,rulerHeight);
                        int tickLabelWidth=(int)rulerFont.getStringBounds(tickLabel, ((Graphics2D)g).getFontRenderContext()).getWidth();
                        if (!overlapsTSS(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2)) && !overlapsTES(pos-(tickLabelWidth/2),pos+(tickLabelWidth/2))) drawString(g,tickLabel,pos-(tickLabelWidth/2),rulerHeight-(majorTickHeight+2));
                    }
                    i+=majorTickInterval*scale;
                 }                    
                 
            }        
                         
        }      
        
        /** 
         * Draws a TSS mark consisting of a directed arrow and gene name at the appropriate place 
         */
        private void drawTSS(Graphics g, int vizOrientation, int geneOrientation, String genename, Integer TSS) {
            if (TSS==null) {TSSLabelPosition[0]=Integer.MIN_VALUE;TSSLabelPosition[1]=Integer.MIN_VALUE;return;} // "invalidate" the TSS label position and return immediately without drawing arrow
            int[] TSSrange=settings.getScreenCoordinateFromGenomic(sequenceName, TSS);   
            int tsscord=(vizOrientation==geneOrientation)?TSSrange[0]:TSSrange[1]; // if vizOrient==geneOrient gene extends to the right on screen
            tsscord+=bordersize;
            if (tsscord>0 && tsscord<=settings.getSequenceWindowSize()) {
                int y=rulerHeight-tssTickHeight;
                int textWidth=(int)rulerFont.getStringBounds(genename, ((Graphics2D)g).getFontRenderContext()).getWidth();
                if (vizOrientation==geneOrientation) { // gene extends to the right
                   TSSLabelPosition[0]=xoffset+tsscord-5;  // the constants just gives some additional margin
                   TSSLabelPosition[1]=TSSLabelPosition[0]+tssTickWidth+12+textWidth;   // the constants just gives some additional margin
                   g.drawLine(xoffset+tsscord,rulerHeight-tssTickHeight,xoffset+tsscord,rulerHeight);  
                   g.drawLine(xoffset+tsscord,y,xoffset+tsscord+tssTickWidth,y);  
                   g.drawLine(xoffset+tsscord+tssTickWidth-1,y-1,xoffset+tsscord+tssTickWidth-1,y+1);
                   g.drawLine(xoffset+tsscord+tssTickWidth-2,y-2,xoffset+tsscord+tssTickWidth-2,y+2);
                   g.drawLine(xoffset+tsscord+tssTickWidth-3,y-3,xoffset+tsscord+tssTickWidth-3,y+3);
                   //g.drawLine(xoffset+tsscord+tssTickWidth-4,y-4,xoffset+tsscord+tssTickWidth-4,y+4);
                   g.fillPolygon(TSSrange, TSSrange, UPDATED);
                   drawString(g,genename, xoffset+tsscord+tssTickWidth+3, y+4);
                 
                } else { // gene extends to the left
                   TSSLabelPosition[0]=xoffset+tsscord-(tssTickWidth+textWidth+7); // the constants just gives some additional margin
                   TSSLabelPosition[1]=xoffset+tsscord+5; // the constants just gives some additional margin
                   g.drawLine(xoffset+tsscord,rulerHeight-tssTickHeight,xoffset+tsscord,rulerHeight);  
                   g.drawLine(xoffset+tsscord-tssTickWidth,y,xoffset+tsscord,y);                      
                   g.drawLine(xoffset+tsscord-tssTickWidth+1,y-1,xoffset+tsscord-tssTickWidth+1,y+1);
                   g.drawLine(xoffset+tsscord-tssTickWidth+2,y-2,xoffset+tsscord-tssTickWidth+2,y+2);
                   g.drawLine(xoffset+tsscord-tssTickWidth+3,y-3,xoffset+tsscord-tssTickWidth+3,y+3);
                   //g.drawLine(xoffset+tsscord-tssTickWidth+4,y-4,xoffset+tsscord-tssTickWidth+4,y+4);
                   drawString(g,genename, xoffset+tsscord-(tssTickWidth+textWidth+3), y+4);
                }
            } else { // TSS outside window. Clear position
                TSSLabelPosition[0]=Integer.MIN_VALUE;
                TSSLabelPosition[1]=Integer.MIN_VALUE;
            }
        } // end draw TSS
    
        
        /** 
         * Draws a TES mark consisting of a directed arrow and gene name at the appropriate place 
         */
        private void drawTES(Graphics g, int vizOrientation, int geneOrientation, String genename, Integer TES) {
            if (TES==null) {TESLabelPosition[0]=Integer.MIN_VALUE;TESLabelPosition[1]=Integer.MIN_VALUE;return;}
            int[] TESrange=settings.getScreenCoordinateFromGenomic(sequenceName, TES);   

            // NOTE: The next 2 codelines beginning with "int tescord=" are almost identical except for a == versus != in the condition
            // The first (commented out here) will draw the TES mark at the start of the base (lefthand side for direct strand)
            // while the second will draw the TES mark at the end of the base (righthand side for direct strand)
            
            //int tescord=(vizOrientation==geneOrientation)?TESrange[0]:TESrange[1]; // if vizOrient==geneOrient gene extends to the right on screen
            int tescord=(vizOrientation!=geneOrientation)?TESrange[0]:TESrange[1]; // if vizOrient==geneOrient gene extends to the right on screen
            
            tescord+=bordersize;            
            if (tescord>0 && tescord<=settings.getSequenceWindowSize()) {
                int y=rulerHeight-tssTickHeight;
                int textWidth=(int)rulerFont.getStringBounds(genename, ((Graphics2D)g).getFontRenderContext()).getWidth();
                if (vizOrientation==geneOrientation) { // gene extends to the right
                   TESLabelPosition[0]=xoffset+tescord-(tssTickWidth+textWidth+7); // the constants just gives some additional margin
                   TESLabelPosition[1]=xoffset+tescord+5; // the constants just gives some additional margin
                   boolean crashWithTSS=!(TSSLabelPosition[1]<TESLabelPosition[0] || TSSLabelPosition[0]>TESLabelPosition[1]);
                   boolean drawSmallerArrow=crashWithTSS;
                   if (crashWithTSS) { // check if arrows crash or just the labels
                       if (TSSLabelPosition[1]>TESLabelPosition[0] && TESLabelPosition[1]-TSSLabelPosition[1]>20) drawSmallerArrow=false; // draw regular TES arrow but drop TES label
                   } 
                   if (drawSmallerArrow) { // draw smaller arrow without label
                       y=y+6;
                       g.drawLine(xoffset+tescord,y,xoffset+tescord,rulerHeight);  // vertical
                       g.drawLine(xoffset+tescord-tssTickWidth+3,y,xoffset+tescord,y);  // horisontal                    
                       g.drawLine(xoffset+tescord-tssTickWidth+1,y-1,xoffset+tescord-tssTickWidth+1,y+1); // smallest 
                       g.drawLine(xoffset+tescord-tssTickWidth+3,y-2,xoffset+tescord-tssTickWidth+3,y+2);                   
                   } else {
                       g.drawLine(xoffset+tescord,y,xoffset+tescord,rulerHeight);  
                       g.drawLine(xoffset+tescord-tssTickWidth+3,y,xoffset+tescord,y);                      
                       g.drawLine(xoffset+tescord-tssTickWidth+1,y-2,xoffset+tescord-tssTickWidth+1,y+2);
                       g.drawLine(xoffset+tescord-tssTickWidth+3,y-3,xoffset+tescord-tssTickWidth+3,y+3);
                       if (!crashWithTSS) drawString(g,genename, xoffset+tescord-(tssTickWidth+textWidth+3), y+4);
                   }
                } else { // gene extends to the left
                   TESLabelPosition[0]=xoffset+tescord-5;  // the constants just gives some additional margin
                   TESLabelPosition[1]=TESLabelPosition[0]+tssTickWidth+12+textWidth;   // the constants just gives some additional margin
                   boolean crashWithTSS=!(TSSLabelPosition[1]<TESLabelPosition[0] || TSSLabelPosition[0]>TESLabelPosition[1]);
                   boolean drawSmallerArrow=crashWithTSS;
                   if (crashWithTSS) { // check if arrows crash or just the labels
                        if (TSSLabelPosition[0]<TESLabelPosition[1] && TSSLabelPosition[1]-TESLabelPosition[1]>20) drawSmallerArrow=false; // draw regular TES arrow but drop TES label
                   } 
                   if (drawSmallerArrow) { // draw smaller arrow without label
                       y=y+6;
                       g.drawLine(xoffset+tescord,y,xoffset+tescord,rulerHeight);  
                       g.drawLine(xoffset+tescord,y,xoffset+tescord+tssTickWidth-3,y);  
                       g.drawLine(xoffset+tescord+tssTickWidth-1,y-1,xoffset+tescord+tssTickWidth-1,y+1);
                       g.drawLine(xoffset+tescord+tssTickWidth-3,y-2,xoffset+tescord+tssTickWidth-3,y+2);                                        
                   } else {
                       g.drawLine(xoffset+tescord,y,xoffset+tescord,rulerHeight);  
                       g.drawLine(xoffset+tescord,y,xoffset+tescord+tssTickWidth-3,y);  
                       g.drawLine(xoffset+tescord+tssTickWidth-1,y-2,xoffset+tescord+tssTickWidth-1,y+2);
                       g.drawLine(xoffset+tescord+tssTickWidth-3,y-3,xoffset+tescord+tssTickWidth-3,y+3);
                       if (!crashWithTSS) drawString(g,genename, xoffset+tescord+tssTickWidth+3, y+4); // don't draw label if it crashes with TSS label
                   }
                }
            } else { // TES outside window. Clear position
                TESLabelPosition[0]=Integer.MIN_VALUE;
                TESLabelPosition[1]=Integer.MIN_VALUE;
            }
        } // end draw TES
        

        /** Draws strings in aliased mode (if selected) */
        private void drawString(Graphics g, String string, int x, int y) {
            if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, MotifLabGUI.ANTIALIAS_MODE);
            g.drawString(string, x, y);
            if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        
               
    } // end inner class Ruler

    
    /**
     * 
     * This private class is responsible for the Information/Control panel located 
     * to the right of the sequence window
     */
     private class InfoPanel extends JPanel {
        private int fontSize=9;
        private Font font=new Font(Font.SANS_SERIF,Font.PLAIN,fontSize); // previously Font.SERIF in older versions
        private int buttonspanelheight=50;
        private Dimension size=new Dimension(150,rulerHeight+buttonspanelheight); // 60
        private InfoPanelButton moveLeftEndButton,moveLeftButton,moveRightEndButton,moveRightButton;
        private InfoPanelButton zoomInButton,zoomResetButton,zoomOutButton;
        private InfoPanelButton flipButton, constrainButton;// lockButton;
        private InfoPanelButton alignLeftButton,alignRightButton,alignTSSButton,alignNoneButton;
        private ViewPortMiniature viewPortMiniature;
        public static final int boxlength=102; // this length includes 1px border on every side
        public static final int boxheight=8; // this height includes 1px border on every side
        private int xoffset=16;
        private int yoffsetArrow=rulerHeight-7;
        private int yoffsetViewBox=rulerHeight+0;
        private int yoffsetViewPort=rulerHeight+19;
        private int yoffsetZoomLevel=rulerHeight+31;
        private int yoffsetButtons=rulerHeight+34;
        private String viewPortString="";
        
        public InfoPanel() {
            super();           
            setOpaque(false);
            setLayout(null);
            setFocusable(false); 
            moveLeftEndButton=new InfoPanelButton(moveToLeftEndAction);
            moveLeftButton=new InfoPanelButton(moveLeftAction);
            moveRightButton=new InfoPanelButton(moveRightAction);
            moveRightEndButton=new InfoPanelButton(moveToRightEndAction);
            zoomOutButton=new InfoPanelButton(zoomOutAction);
            zoomResetButton=new InfoPanelButton(zoomResetAction);
            zoomInButton=new InfoPanelButton(zoomInAction);
            constrainButton=new InfoPanelButton(constrainAction);
            //lockButton=new InfoPanelButton(scrollLockAction);
            flipButton=new InfoPanelButton(flipOrientationAction);
            alignLeftButton=new InfoPanelButton(alignLeftAction);
            alignRightButton=new InfoPanelButton(alignRightAction);
            alignTSSButton=new InfoPanelButton(alignTSSRightAction);
            alignNoneButton=new InfoPanelButton(alignNoneAction);
            add(moveLeftEndButton);
            add(moveLeftButton);
            add(moveRightButton);
            add(moveRightEndButton);            
            add(zoomOutButton);
            add(zoomResetButton);
            add(zoomInButton);
            add(constrainButton);
            //add(lockButton);
            add(flipButton);
            add(alignLeftButton);
            add(alignRightButton);
            add(alignTSSButton);
            add(alignNoneButton);
            alignLeftButton.setBounds(xoffset+boxlength-32, yoffsetZoomLevel-7, 8, 8);
            alignRightButton.setBounds(xoffset+boxlength-24, yoffsetZoomLevel-7, 8, 8);
            alignTSSButton.setBounds(xoffset+boxlength-16, yoffsetZoomLevel-7, 8, 8);
            alignNoneButton.setBounds(xoffset+boxlength-7, yoffsetZoomLevel-7, 8, 8);            
            moveLeftEndButton.setBounds(xoffset, yoffsetButtons, 8, 8);
            moveLeftButton.setBounds(xoffset+8, yoffsetButtons, 6, 8);
            moveRightButton.setBounds(xoffset+14, yoffsetButtons, 6, 8);
            moveRightEndButton.setBounds(xoffset+20, yoffsetButtons, 8, 8);            
            zoomOutButton.setBounds(xoffset+36, yoffsetButtons, 8, 8);
            zoomResetButton.setBounds(xoffset+44, yoffsetButtons, 8, 8);
            zoomInButton.setBounds(xoffset+52, yoffsetButtons, 8, 8);
            flipButton.setBounds(xoffset+69, yoffsetButtons, 14, 8);
            constrainButton.setBounds(xoffset+boxlength-14, yoffsetButtons, 16, 8);
            //lockButton.setBounds(xoffset+boxlength-12, yoffsetButtons, 14, 8);
            viewPortMiniature=new ViewPortMiniature();
            add(viewPortMiniature);
            viewPortMiniature.setBounds(xoffset,yoffsetViewBox-8,10,10);            
        } 
        
        @Override 
        public int getHeight() {
            if (condensedmode) return boxheight;
            int tracksheight=getTotalTracksHeight();
            if (tracksheight<buttonspanelheight) tracksheight=buttonspanelheight; // to fit the buttons
            return tracksheight+rulerHeight;
        }
                
        @Override 
        public int getWidth() {
            if (isPaintingForPrint()) return getTrackLabelsWidth();
            else return size.width; 
        }
                 
        public int getTrackLabelsWidth() {
            Font trackLabelsFont=SequenceVisualizer.getTrackLabelsFont();
            int maxtracklabelwidth=0;
            ArrayList<String> trackorder=settings.getDatatrackOrder();
            for (String trackname:trackorder) {                 
               if (!settings.isTrackVisible(trackname)) continue;
               int tracklabelwidth=getFontMetrics(trackLabelsFont).stringWidth(trackname);
               if (tracklabelwidth>maxtracklabelwidth) maxtracklabelwidth=tracklabelwidth;
            }
            return maxtracklabelwidth;
        }
        
        @Override
        public Dimension getPreferredSize() { return size; }
        
        @Override
        public Dimension getMinimumSize() {return size;}
        
        @Override
        public Dimension getMaximumSize() {return size;}
     
        
        @Override
        public void paint(Graphics g) {
            // This might not be good programming: when we "SaveAsImage" we don't want to include the infopanel. "SaveAsImage" is done in the background (off the EDT), hence this check                       
             if (isPaintingForPrint()) paintTrackLabels((Graphics2D)g);
             else super.paint(g);
        }
        
        /** paints track labels to the right of the tracks instead of painting the usual infoPanel */
        private void paintTrackLabels(Graphics2D g) {
             g.setColor(java.awt.Color.BLACK);
             int ypos=rulerHeight;
             g.setFont(trackLabelsFont);
             g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, MotifLabGUI.ANTIALIAS_MODE);             
             ArrayList<String> trackorder=settings.getDatatrackOrder();
             FontMetrics metrics=getFontMetrics(trackLabelsFont);
             int fontheight=metrics.getAscent();
             AffineTransform saveAt=g.getTransform();
             for (String trackname:trackorder) {                 
                 if (!settings.isTrackVisible(trackname)) continue;
                 int trackHeight=settings.getDataTrackHeight(trackname);
                 if (settings.hasVariableTrackHeight(trackname)) {
                    DataTrackVisualizer viz=allTrackvisualizers.get(trackname);
                    if (viz!=null) trackHeight = (viz.getTrackHeight());
                 }                 
                 if (fontheight>trackHeight) {
                     g.translate(0, ypos+trackHeight);
                     double scale=(double)trackHeight/(double)(fontheight-3); // the -3 is just to avoid scaling too small...
                     g.scale(1,scale);
                 } else {
                     g.translate(0, ypos+(trackHeight+fontheight)/2);
                 }
                 ypos+=trackHeight+bordersize;
                 g.drawString(trackname, 10, 0);                 
                 g.setTransform(saveAt);
             }
             g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);    
        }
        
        @Override
        public void paintComponent(Graphics g) {  // paint infopanel
            int orientation=settings.getSequenceOrientation(sequenceName);
            int viewPortStart=settings.getSequenceViewPortStart(sequenceName);
            int viewPortEnd=settings.getSequenceViewPortEnd(sequenceName);
            int sequenceStart=datasequence.getRegionStart();
            int sequenceEnd=datasequence.getRegionEnd();
            int sequenceLength=sequenceEnd-sequenceStart+1;
            double scale=settings.getScale(sequenceName);
            g.setColor(Color.black);
            
            // draw Arrow
            if (!condensedmode) {
                g.drawRect(xoffset,yoffsetArrow,boxlength-14,1); // arrowline
                if (orientation==VisualizationSettings.DIRECT) { // arrowhead
                   g.drawLine(xoffset+boxlength-15,yoffsetArrow-1,xoffset+boxlength-15,yoffsetArrow+2);
                   g.drawLine(xoffset+boxlength-16,yoffsetArrow-2,xoffset+boxlength-16,yoffsetArrow+3);
                   g.drawLine(xoffset+boxlength-17,yoffsetArrow-3,xoffset+boxlength-17,yoffsetArrow+4);
                } else {
                   g.drawLine(xoffset+1,yoffsetArrow-1,xoffset+1,yoffsetArrow+2);
                   g.drawLine(xoffset+2,yoffsetArrow-2,xoffset+2,yoffsetArrow+3);
                   g.drawLine(xoffset+3,yoffsetArrow-3,xoffset+3,yoffsetArrow+4);
                }

                // draw + or - after arrow
                g.drawRect(xoffset+boxlength-7,yoffsetArrow,7,1);
                if (orientation==VisualizationSettings.DIRECT) {
                   g.drawRect(xoffset+boxlength-4,yoffsetArrow-3,1,7);
                }
            } else { // draw small + or - after viewport miniature
                int strand=datasequence.getStrandOrientation();
                if (strand==settings.getSequenceOrientation(sequenceName)) g.setColor(darkgreen); else g.setColor(Color.red);
                g.drawRect(xoffset+boxlength+4,yoffsetViewBox-rulerHeight+3,7,1); // draw horizontal line to make -
                if (orientation==VisualizationSettings.DIRECT) { // draw vertical line do make +
                   g.drawRect(xoffset+boxlength+7,(yoffsetViewBox-rulerHeight),1,7);
                }
                g.setColor(Color.BLACK);
            }
               
            // draw viewPortMiniatureBox (without the inner box which is handled by the class ViewPortMiniature)
            if (condensedmode) yoffsetViewBox-=rulerHeight; // not drawing ruler in condensed mode so don't offset
            g.drawRect(xoffset,yoffsetViewBox,boxlength,boxheight-1); // border
            g.setColor(Color.GRAY);
            g.fillRect(xoffset+1,yoffsetViewBox+1,boxlength-1,boxheight-2);
                        
            if (sequenceStart>=viewPortStart && sequenceEnd<=viewPortEnd) { // sequence fully within ViewPort
                viewPortMiniature.setBounds(xoffset,yoffsetViewBox,boxlength+1,boxheight);  
                viewPortMiniature.setBackground(Color.WHITE);
            } else if ((sequenceEnd<viewPortStart && orientation==VisualizationSettings.DIRECT)||(sequenceStart>viewPortEnd && orientation==VisualizationSettings.REVERSE)) { // Sequence outside viewport. Draw left arrow.               
                g.setColor(Color.BLACK);
                g.drawLine(xoffset-2, yoffsetViewBox, xoffset-2, yoffsetViewBox+boxheight-1);
                g.drawLine(xoffset-3, yoffsetViewBox+1, xoffset-3, yoffsetViewBox+boxheight-2);
                g.drawLine(xoffset-4, yoffsetViewBox+2, xoffset-4, yoffsetViewBox+boxheight-3);
                g.drawLine(xoffset-5, yoffsetViewBox+3, xoffset-5, yoffsetViewBox+boxheight-4);
                viewPortMiniature.setBounds(xoffset,yoffsetViewBox,0,0);  
            } else if ((sequenceStart>viewPortEnd && orientation==VisualizationSettings.DIRECT)||(sequenceEnd<viewPortStart && orientation==VisualizationSettings.REVERSE)) { // Sequence outside viewport. Draw right arrow.
                g.setColor(Color.BLACK);
                g.drawLine(xoffset+boxlength+2, yoffsetViewBox, xoffset+boxlength+2, yoffsetViewBox+boxheight-1);
                g.drawLine(xoffset+boxlength+3, yoffsetViewBox+1, xoffset+boxlength+3, yoffsetViewBox+boxheight-2);
                g.drawLine(xoffset+boxlength+4, yoffsetViewBox+2, xoffset+boxlength+4, yoffsetViewBox+boxheight-3);
                g.drawLine(xoffset+boxlength+5, yoffsetViewBox+3, xoffset+boxlength+5, yoffsetViewBox+boxheight-4);
                viewPortMiniature.setBounds(xoffset,yoffsetViewBox,0,0);  
            } else {
                int boxwidth=boxlength-2; // subtract borders to get inner box size
                double basesPerBoxPixel=sequenceLength/(double)boxwidth; // number of sequence bases represented by each pixel in the full box                
                int viewboxStart=0,viewboxEnd=0,viewboxWidth=0; // marks region within sequence that is shown in ViewPort
                if (orientation==VisualizationSettings.DIRECT) {
                    int basesOutsideLeft=(sequenceStart<viewPortStart)?viewPortStart-sequenceStart:0;
                    int basesOutsideRight=(sequenceEnd>viewPortEnd)?(sequenceEnd-viewPortEnd):0;
                    int pixelsOutLeft=(int)Math.ceil(basesOutsideLeft/basesPerBoxPixel); // rounds up to nearest pixel
                    int pixelsOutRight=(int)Math.ceil(basesOutsideRight/basesPerBoxPixel);
                    viewboxStart=(pixelsOutLeft==1)?pixelsOutLeft+1:pixelsOutLeft; // this is so that some gray line will always be displayed to the left of the yellow box when applicable
                    viewboxEnd=(pixelsOutRight==1)?boxlength-(pixelsOutRight+1):boxlength-pixelsOutRight;
                    viewboxWidth=viewboxEnd-viewboxStart+1;              
                } else {
                    int basesOutsideRight=(sequenceStart<viewPortStart)?viewPortStart-sequenceStart:0;
                    int basesOutsideLeft=(sequenceEnd>viewPortEnd)?(sequenceEnd-viewPortEnd):0;
                    int pixelsOutLeft=(int)Math.ceil(basesOutsideLeft/basesPerBoxPixel); // rounds up to nearest pixel
                    int pixelsOutRight=(int)Math.ceil(basesOutsideRight/basesPerBoxPixel);
                    viewboxStart=(pixelsOutLeft==1)?pixelsOutLeft+1:pixelsOutLeft;
                    viewboxEnd=(pixelsOutRight==1)?boxlength-(pixelsOutRight+1):boxlength-pixelsOutRight;
                    viewboxWidth=viewboxEnd-viewboxStart+1;   
                }
                viewPortMiniature.setBackground(Color.YELLOW);
                viewPortMiniature.setBounds(xoffset+viewboxStart,yoffsetViewBox,viewboxWidth,boxheight);  
            }
            if (condensedmode) yoffsetViewBox+=rulerHeight; // reset offset
            if (!condensedmode) {
                //draw ViewPort coordinates
                g.setColor(Color.black);
                if (orientation==VisualizationSettings.DIRECT) {viewPortString=getChromosomeString()+MotifLabEngine.groupDigitsInNumber(viewPortStart)+"\u2192"+MotifLabEngine.groupDigitsInNumber(viewPortEnd);}
                else {viewPortString=getChromosomeString()+MotifLabEngine.groupDigitsInNumber(viewPortEnd)+"\u2190"+MotifLabEngine.groupDigitsInNumber(viewPortStart);}
                g.setFont(font);
                if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, MotifLabGUI.ANTIALIAS_MODE);
                g.drawString(viewPortString, xoffset, yoffsetViewPort);
                int viewPortStringWidth=getFontMetrics(getFont()).stringWidth(viewPortString);
                if (viewPortStringWidth<150) viewPortStringWidth=150;
                size.width=viewPortStringWidth;
                 //draw Zoom level
                String zoomString;
                if (scale>=0.01) { // zoom level above 1%. Round down to nearest integer
                    zoomString=(int)(scale*100.0)+"%";
                } else if (scale<0.01 && scale>=0.001) { // zoom level between 0.1 and 0.9%
                    zoomString=Graph.formatNumber(scale, true, 1, 1);
                } else if (scale<0.001 && scale>=0.0001) { // zoom level between 0.01 and 0.09%
                    zoomString=Graph.formatNumber(scale, true, 2, 2);
                } else if (scale<0.0001 && scale>=0.00001) { // zoom level between 0.001 and 0.009%
                    zoomString=Graph.formatNumber(scale, true, 3, 3);
                } else if (scale<0.00001 && scale>=0.000001) { // zoom level between 0.0001 and 0.0009%
                    zoomString=Graph.formatNumber(scale, true, 4, 4);
                } else { // zoom level below 0.0001%
                    zoomString="0%";
                }
                int zoomStringSize=(int)font.getStringBounds(zoomString, ((Graphics2D)g).getFontRenderContext()).getWidth();
                g.drawString("Zoom:", xoffset, yoffsetZoomLevel);
                g.drawString(zoomString, xoffset+63-zoomStringSize, yoffsetZoomLevel);
                if (settings.useTextAntialiasing()) ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }
        } // end paint infoPanel

        private String getChromosomeString() {
            String chr=datasequence.getChromosome();
            if (chr.equals("?")) return "";
            else return "chr"+chr+":";
        }
        
     private class InfoPanelButton extends JButton {
          Color selectedColor=Color.WHITE;
         
          public InfoPanelButton(Action action) { 
              super(action);
              setBorderPainted(false);
              setOpaque(false);
              setBorder(null);
              setRolloverEnabled(false);
              setFocusPainted(false);     
              setFocusable(false);
              setToolTipText((String)action.getValue(Action.NAME));
          }
          
          @Override
          public boolean isVisible() {
              return (!condensedmode);
          }

          @Override
          public void paintComponent(Graphics g){ // paint InfoPanelButton
               InfoPanelIcon icon=(InfoPanelIcon)getIcon();
               if (icon.getIconType()==InfoPanelIcon.CONSTRAIN_ICON) {
                   if (settings.isSequenceConstrained(sequenceName)) icon.setForegroundColor(selectedColor);
                   else icon.setForegroundColor(Color.BLACK);
               } else if(icon.getIconType()==InfoPanelIcon.LOCK_ICON) {
                   if (settings.isSequenceScrollLocked(sequenceName)) icon.setForegroundColor(selectedColor);
                   else icon.setForegroundColor(Color.BLACK);
               } else if(icon.getIconType()==InfoPanelIcon.ALIGN_LEFT_ICON) {
                   if (settings.getSequenceAlignment(sequenceName).equals(VisualizationSettings.ALIGNMENT_LEFT)) icon.setForegroundColor(selectedColor);
                   else icon.setForegroundColor(Color.BLACK);
               } else if(icon.getIconType()==InfoPanelIcon.ALIGN_RIGHT_ICON) {
                   if (settings.getSequenceAlignment(sequenceName).equals(VisualizationSettings.ALIGNMENT_RIGHT)) icon.setForegroundColor(selectedColor);
                   else icon.setForegroundColor(Color.BLACK);
               } else if(icon.getIconType()==InfoPanelIcon.ALIGN_NONE_ICON) {
                   if (settings.getSequenceAlignment(sequenceName).equals(VisualizationSettings.ALIGNMENT_NONE)) icon.setForegroundColor(selectedColor);
                   else icon.setForegroundColor(Color.BLACK);
               } else if(icon.getIconType()==InfoPanelIcon.ALIGN_TSS_ICON) {
                   if (settings.getSequenceAlignment(sequenceName).equals(VisualizationSettings.ALIGNMENT_TSS)) icon.setForegroundColor(selectedColor);
                   else icon.setForegroundColor(Color.BLACK);
               } 
               icon.paintIcon(null, g, 0, 0);
          }

     } // End class  InfoPanel.InfoPanelButton      
   
     /** 
      * This inner class is responsible for drawing the miniature representation
      * of the viewPort in relation to the sequence and for handling any mouse events
      * on that control
      */
     private class ViewPortMiniature extends JPanel implements MouseInputListener {
            private int dragX=0;
            private int storedViewPortStart=0;
            
            public ViewPortMiniature() {   
              addMouseListener(this);
              addMouseMotionListener(this);
              setOpaque(true);  
              setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }

            @Override public void mouseClicked(MouseEvent e) {}  // unneeded interface implementation
            @Override public void mouseEntered(MouseEvent e) {}  // unneeded interface implementation
            @Override public void mouseExited(MouseEvent e) {}   // unneeded interface implementation
            @Override public void mouseReleased(MouseEvent e) {} // unneeded interface implementation
            @Override public void mouseMoved(MouseEvent e) {}    // unneeded interface implementation
            
            @Override 
            public void mousePressed(MouseEvent e) {
                dragX=e.getXOnScreen(); // must use screen coordinates because the miniature can reposition unexpectedly! (which results in "flickering")
                storedViewPortStart=settings.getSequenceViewPortStart(sequenceName);
            }
            
            @Override 
            public void mouseDragged(MouseEvent e) { //
                int pixelDiff=e.getXOnScreen()-dragX; // must use screen coordinates because the miniature can reposition unexpectedly!
                double boxwidth=InfoPanel.boxlength-2;// 1px border on each side
                int sequenceLength=datasequence.getSize();
                double basesPerBoxPixel=sequenceLength/boxwidth;
                int genomicDiff=(int)Math.round(pixelDiff*basesPerBoxPixel);
                int orientation=settings.getSequenceOrientation(sequenceName);
                int newVPstart=storedViewPortStart+orientation*genomicDiff;             
                settings.setSequenceViewPortStart(sequenceName, newVPstart);
                SequenceVisualizer.this.repaint();
            }


          

     } // End class  InfoPanel.ViewPortMiniature      
   
     
   } // End class InfoPanel
     

} // End class SequenceVisualizer
