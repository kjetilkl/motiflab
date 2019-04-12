/*
 
 
 */

package motiflab.gui;

import javax.swing.event.ListDataEvent;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataListener;
import motiflab.engine.ExtendedDataListener;
import motiflab.engine.MotifLabClient;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.StandardDisplaySettingsParser;



/**
 * This class is a placeholder for all settings applicable to sequence visualization,
 * and keeps information such as the color, type and height of all datatracks and the 
 * order of tracks (and whether they should be displayed at all). 
 * Relevant visualizers use this class to obtain the appropriate settings, which 
 * can be adjusted by the user in the GUI.
 * 
 * VisualizationSettings should be a "singleton" object within the GUI, and to
 * obtain the object one should use the getInstance() method rather than calling
 * the constructor directly (it is private anyway). 
 *
 * To set or get data one could use the getSetting() or storeSetting() methods respectively.
 * Alternatively, the methods getPersistentSetting() and storePersistentSetting() can be used
 * for settings that should be kept the same for later invokations of the application 
 * (these values are stored locally in the users home directory).
 * 
 * Many properties are provided with convenience accessor methods that should
 * preferably be used when available. For instance, in stead of using getSetting("height")
 * or storeSetting("height") directly, one should use getHeight() or setHeight(). 
 * These convenience methods take care of remembering the specific settings names (HashMap keys)
 * and also do conversions to appropriate data types. In addition, the methods can
 * supply sensible defaults for uninitialized settings and take care of any other
 * updates that should be made for consistency.
 * 
 * 
 * @author kjetikl
 */
public class VisualizationSettings implements ListDataListener, ExtendedDataListener, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String SEQUENCE_WINDOW_SIZE="SequenceWindowSize";
    public static final String SEQUENCE_SIZE="SequenceSize";
    private static final int DEFAULT_SEQUENCE_WINDOW_SIZE=700;
    public static final String TOTAL_TRACKS_HEIGHT="totalTracksHeight";
    public static final String VIEWPORT_START="viewportStart";
    public static final String MAINPANEL_BACKGROUND="mainpanelBackground";
    public static final String FOREGROUND_COLOR="foregroundColor";
    public static final String BACKGROUND_COLOR="backgroundColor";
    public static final String SECONDARY_COLOR="secondaryColor";
    public static final String BASELINE_COLOR="baselineColor";
    public static final String COLORGRADIENT="colorgradient";
    public static final String SECONDARY_COLORGRADIENT="secondarycolorgradient";
    public static final String BASE_COLOR="baseColor";
    public static final String SEQUENCE_ORIGO="sequenceOrigo";
    public static final String TRACK_HEIGHT="trackHeight";
    public static final String EXPANDED_REGIONS_HEIGHT="expandedRegionsHeight";    
    public static final String EXPANDED_REGIONS_ROW_SPACING="expandedRegionsRowSpacing"; 
    private static final int DEFAULT_TRACK_HEIGHT_DNA=10;
    private static final int DEFAULT_TRACK_HEIGHT_NUMERIC=20;
    private static final int DEFAULT_TRACK_HEIGHT_REGION=6;
    public static final int DEFAULT_TRACK_HEIGHT_MOTIF=24;
    public static final int DEFAULT_TRACK_HEIGHT_MODULE=16;
    public static final int DEFAULT_EXPANDED_REGIONS_HEIGHT=3;
    public static final int DEFAULT_EXPANDED_REGIONS_ROW_SPACING=2;
    public static final String SEQUENCE_MARGIN="sequenceMargin";
    private static final int DEFAULT_SEQUENCE_MARGIN=20;
    public static final String SEQUENCE_LABEL_FIXED_WIDTH="sequenceLabelFixedWidth";
    public static final String SEQUENCE_LABEL_WIDTH="sequenceLabelWidth";
    public static final String SEQUENCE_LABEL_COLOR="sequenceLabelColor";
    public static final String SCALE_SEQUENCE_LABELS_TO_FIT="scaleSequenceLabelsToFit";
    public static final String TRACK_LABEL_STYLE="datatrackNameStyle";
    public static final String TRACK_LABEL_SIZE="datatrackNameFontSize";
    public static final String TRACK_LABEL_ALIGNMENT="datatrackNameAlignment"; 
    public static final String USE_MULTI_COLORED_REGIONS="useMultiColoredRegions";
    
    private static final int DEFAULT_SEQUENCE_LABEL_WIDTH=80;
    public static final String CLUSTER_COLOR="clusterColor";
    public static final String ZOOM_LEVEL="zoomLevel";
    private static final double DEFAULT_ZOOM_LEVEL=100.0;
    public static final String VISIBLE="visible";
    public static final String SEQUENCE_VISIBLE="sequence."+VISIBLE;
    public static final String TRACK_VISIBLE="track."+VISIBLE;    
    public static final String GRAPH_TYPE="graphType"; // this is the old deprecated property which stores graph types as integer codes (static constants)
    public static final String GRAPH_NAME="graphName"; // this is the new property that supercedes the one above and stores graph types as string names
    public static final String CONNECTOR_TYPE="connectorType";    
    
    /* The following graph types used to be public, but graph types are now encoded by name (String) and not by magic numbers */
    private static final int BAR_GRAPH=0;
    private static final int GRADIENT_GRAPH=1;
    private static final int REGION_GRAPH=2;
    private static final int REGION_MULTICOLOR_GRAPH=3;
    private static final int GRADIENT_REGION_GRAPH=4; // This has not been used. I think it was supposed to visualize score using "grayscale" coloring with darker colors for higher scoring regions
    private static final int DNA_GRAPH=5;
    private static final int LINE_GRAPH=6;
    private static final int RAINBOW_HEATMAP_GRAPH=7;
    private static final int TWO_COLOR_HEATMAP_GRAPH=8;
    private static final int OUTLINED_GRAPH=9;
    private static final int GRADIENT_BAR_GRAPH=10;
    private static final int NUMERIC_DNA_GRAPH=11;    
    
    public static final int CONNECTOR_BOXED=0;    
    public static final int CONNECTOR_STRAIGHT_LINE=1; 
    public static final int CONNECTOR_ANGLED=2;    
    public static final int CONNECTOR_CURVED=3;    
    public static final int CONNECTOR_FILLED_CURVE=4;    
    
    public static final String SEQUENCE_ORIENTATION="sequenceOrientation";
    public static final int DIRECT=1;
    public static final int REVERSE=-1;
    public static final String CONSTRAINED="constrained";
    public static final String SCROLL_LOCK="scroll lock";
    public static final String SEQUENCE_WITH_SELECTION="sequenceWithSelection";
    public static final String RULER="Ruler";
    public static final String RULER_TSS="Origo At Gene TSS";
    public static final String RULER_TES="Origo At Gene TES";    
    public static final String RULER_UPSTREAM_END="Origo At Upstream End";
    public static final String RULER_DOWNSTREAM_END="Origo At Downstream End";
    public static final String RULER_CHROMOSOME="Origo At Chromosome Start";
    public static final String RULER_FIXED="Fixed Ruler";
    public static final String RULER_NONE="No Ruler";
    public static final String SEQUENCE_ALIGNMENT="sequenceAlignment";
    public static final String SEQUENCE_DEFAULT_ALIGNMENT="sequenceAlignment";
    public static final String ALIGNMENT_LEFT="Align Left";
    public static final String ALIGNMENT_RIGHT="Align Right";
    public static final String ALIGNMENT_NONE="None";
    public static final String ALIGNMENT_TSS="Align TSS";
    public static final String ANTIALIAS_TEXT="Antialias text";
    public static final String ANTIALIAS_MOTIFS="Antialias motifs";
    public static final String VISUALIZE_REGION_STRAND="visualizeRegionStrand";
    public static final String VISUALIZE_REGION_SCORE="visualizeRegionScore";
    public static final String VISUALIZE_NESTED_REGIONS="visualizeNestedRegions";    
    public static final String RENDER_UPSIDE_DOWN="renderUpsideDown";   
    public static final String MULTICOLOR_PALETTE="color";
    public static final String CLASS_LABEL_COLOR="classLabelColor";
    public static final String COLOR_MOTIFS_BY_CLASS="colorByClass";
    public static final String MODULE_OUTLINE_COLOR="moduleOutlineColor";
    public static final String MODULE_FILL_COLOR="moduleFillColor";
    public static final String COLOR_MODULE_BY_TYPE="useFeatureColor";
    public static final String VARIABLE_HEIGHT_TRACK="variableHeightTrack";
    public static final String REGION_TRACK_EXPANDED="regionTrackExpanded";
    public static final String SHOW_MOTIF_LOGO_IN_CLOSEUP_VIEW="showMotifLogoInCloseupView";
    public static final String SHOW_TRACK_NAMES="showDataTrackNames";
    public static final String CLOSEUP_VIEW_ENABLED="closeupViewEnabled";
    public static final String BASE_CURSOR_COLOR="basecursorcolor";
    public static final String GRADIENT_FILL_REGIONS="gradientfillregions";
    public static final String DRAW_REGION_BORDERS="drawregionborders";    
    public static final String DROPSHADOW="dropshadow";
    public static final String SEQUENCE_LABEL_ALIGNMENT="sequenceLabelAlignment";
    public static final String MARKED_SEQUENCES="markedSequences";
    public static final String EXPRESSION_COLORGRADIENT_UPREGULATED="expressionColorGradientUp";
    public static final String EXPRESSION_COLORGRADIENT_DOWNREGULATED="expressionColorGradientDown";
    public static final String HTML_SETTING_NONE="None";
    public static final String HTML_SETTING_NEW_FILE="New File";
    public static final String HTML_SETTING_SHARED_FILE="Shared File";
    public static final String HTML_SETTING_EMBED="Embed";
    public static final String HTML_SETTING_LINK="Link";
    public static final String JAVASCRIPT="Javascript";
    public static final String CSS="CSS";
    public static final String STYLESHEET="stylesheet";
    public static final String SYSTEM_COLOR="systemColor";
    public static final String SYSTEM_SETTING="systemSetting";   
    public static final String MACROS="macroDefinitions";           
    public static final String SCALE_RANGE_BY_INDIVIDUAL_SEQUENCES="scaleRangeByIndividualSequences";     
    public static final String GROUP_TRACK="displayAsGroupTrack";         
    
    public static final String NUMERIC_TRACK_SAMPLING_CUTOFF="numericTrackSamplingCutoff";
    public static final String NUMERIC_TRACK_SAMPLING_NUMBER="numericTrackSamplingNumber";
    public static final String NUMERIC_TRACK_DISPLAY_VALUE="numericTrackDisplayValue";
   
    public static final double CLOSE_UP_ZOOM_LIMIT=1000; // >=1000% zoom is closeup limit
    public static final String[] HTML_SETTINGS=new String[]{HTML_SETTING_NONE,HTML_SETTING_NEW_FILE,HTML_SETTING_SHARED_FILE,HTML_SETTING_EMBED,HTML_SETTING_LINK};

    public static final int DATATRACK_BORDER_SIZE=1; // NEVER CHANGE THIS (unless you fix all the other code that just assumes the border width is 1.0)
    public static final int DATATRACK_MIN_HEIGHT=2; // 
    public static final int DATATRACK_MAX_HEIGHT=200; // 
    public static final int EXPANDED_REGION_MAX_HEIGHT=150; // 
    public static final int EXPANDED_REGION_MAX_ROW_SPACING=100;
    public static final int SEQUENCE_MARGIN_MIN_HEIGHT=0; //
    public static final int SEQUENCE_MARGIN_MAX_HEIGHT=150; // 
    
    private static Font dnaFont = new Font(Font.SANS_SERIF, Font.BOLD, 10); 
    private static Font defaultGraphFont=new Font(Font.DIALOG,Font.PLAIN,12);    
    
    private transient ColorGradient rainbowgradient=null;

    /** Standard MotifLab palette colors */
    public static final Color BLACK = Color.BLACK;
    public static final Color WHITE = Color.WHITE;
    public static final Color RED = Color.RED;
    public static final Color ORANGE = new Color(255,204,0);
    public static final Color YELLOW = Color.YELLOW;
    public static final Color LIGHT_GREEN = new Color(138,255,150);
    public static final Color GREEN = Color.GREEN;
    public static final Color DARK_GREEN = new Color(0,168,0);      
    public static final Color CYAN = Color.CYAN;
    public static final Color LIGHT_BLUE = new Color(56,163,255);
    public static final Color BLUE = Color.BLUE;
    public static final Color VIOLET = new Color(153,51,255);
    public static final Color MAGENTA = Color.MAGENTA;
    public static final Color PINK = new Color(255, 150, 150);
    public static final Color LIGHT_BROWN = new Color(220,112,40);
    public static final Color DARK_BROWN = new Color(153,51,0);
    public static final Color GRAY = new Color(164,164,164);
    public static final Color LIGHT_GRAY = new Color(220,220,220);
    public static final Color DARK_GRAY = new Color(96,96,96);    

    
    private static Color getColorForMotifClass(String classname) {
         return MotifClassification.getColorForMotifClass(classname);       
    }
    

    // Below are the default color palettes in order
    private Color[] bgColorList=new Color[]{Color.white,Color.white,Color.white, Color.white,Color.white,  Color.white,Color.white,Color.white,Color.white,Color.white,Color.white,Color.white,Color.white,Color.white};
    private Color[] fgColorList=new Color[]{Color.black,Color.red,  Color.green, Color.blue, Color.magenta,Color.cyan, Color.orange, new Color(128,0,0), new Color(0,128,0), new Color(0,0,128), new Color(128,0,128), new Color(0,128,128), new Color(128,128,0), new Color(128,64,0)};
    private double[] zoomChoices=new double[]{1,2,5,10,25,50,75,100,200,500,800,1000,2000,5000,10000}; // These are shown in the zoom drop-down box. NOTE: The values must be in increasing order!!!
    private double[] zoomChoicesFiner=new double[]{0.0005,0.001,0.002,0.005,0.01,0.02,0.05,0.1,0.2,0.5,1,2,5,10,20,30,40,50,60,70,80,90,100,125,150,175,200,250,300,350,400,450,500,600,700,800,900,1000,1100,1200,1300,1400,1500,1600,1700,1800,1900,2000,2500,3000,3500,4000,4500,5000,6000,7000,8000,9000,10000}; // these values must be in increasing order!!!
    
    private transient MotifLabGUI gui;
    private transient MotifLabClient client;
    private int colorpointer=0;
    private int clustercolorpointer=0;
    private HashMap<String,Object> settings;
    private static VisualizationSettings instance=null;
    private ArrayList<String> datatrackorder=null; // a list of the currently visible datatracks in order. This should be a subset of the "masterlist" below (with same sort order)
    private ArrayList<String> datatrackorderMasterList=null; // the names of all datatracks that have been visualized (both shown or hidden). Note that this may contain names for datasets that have since been deleted
    private final transient ArrayList<VisualizationSettingsListener> listeners=new ArrayList<VisualizationSettingsListener>();
    private ArrayList<SelectionWindow> selections=new ArrayList<SelectionWindow>(); // this array stores selection windows
    private boolean enableNotifications=true;
    private boolean featurePanelListeningEnabled=true;
    /**
     * Returns a singleton instance of VisualizationSettings
     * Use this method instead of the regular constructor
     * @param client This is installed the first time getInstance is called
     *               and has no effect on subsequent calls!
     */
    public static VisualizationSettings getInstance(MotifLabClient client) {
        if (instance==null) instance=new VisualizationSettings(client);
        return instance;
    }
    
    /**
     * Creates a new instance of VisualizationSettings
     * Some settings are attempted read from persistent storage (local preference files)
     * @param gui
     */
    private VisualizationSettings(MotifLabClient client) {
        setClient(client);
        this.settings=new HashMap<String,Object>();
        this.datatrackorderMasterList=new ArrayList<String>();
        if (getPersistentSetting(SEQUENCE_WINDOW_SIZE)==null) {storePersistentSetting(SEQUENCE_WINDOW_SIZE,new Integer(DEFAULT_SEQUENCE_WINDOW_SIZE));}
        setDefaultBaseColors(); // loads or otherwise initializes a palette of default colors for visualizing DNA bases
    }
    
    private void setClient(MotifLabClient client) {
        this.client=client;
        if (client instanceof MotifLabGUI) this.gui=(MotifLabGUI)client; else this.gui=null;
        if (client!=null) client.getEngine().addDataListener(this);
    }
  
    public void redraw() {
        if (gui!=null) gui.redraw();
    }
    
    /**
     * Returns a (possibly sorted) list of all registered keys in the VisualizationSettings lookup-table
     * @param sort
     * @return 
     */
    public ArrayList<String> getAllKeys(boolean sort) {
        if (settings==null) return new ArrayList<String>(0);
        ArrayList<String> list=new ArrayList<String>(settings.keySet());
        if (sort) Collections.sort(list);
        return list;
    }
    
    /**
     * Returns a (possibly sorted) list of all registered keys in the VisualizationSettings lookup-table
     * that are associated with the specified setting. E.g. if the setting is "color" it will return
     * all names X that has stored a setting with the key "X.color"
     * @param setting The name of the setting to search for
     * @param sort
     * @return The key string minus the ".setting" part
     */
    public ArrayList<String> getKeysForSetting(String setting, boolean sort) {
        setting="."+setting;
        ArrayList<String> list=new ArrayList<String>();          
        if (settings==null) return list; 
        for (String key:settings.keySet()) {
            int i=key.indexOf(setting);
            if (i>0) list.add(key.substring(0,i));                         
        }
        if (sort) Collections.sort(list);
        return list;
    }    
    
    /**
     * Returns a visualization setting corresponding to the given key (if it exists)
     * Settings are usually stored in the format "datasetname.propertyname" (e.g. Repeats.foregroundColor)
     * 
     * @param key The name of the visualization setting to be retrieved
     * @return An object representing the specified visualization setting
     */
    public Object getSetting(String key) {
        return settings.get(key);
    }
    
    
    /**
     * Returns the setting corresponding to the given key (if it exists)
     * but only if the type of the setting corresponds to the type of the defaultValue
     * If the setting does not exist, or if the current value of the setting is 
     * of a different type than the defaultValue, the defaultValue will be returned
     * instead
     * @param setting
     * @param defaultValue (this can not be null)
     * @return A value with the same class as the defaultValue
     */
    public Object getSettingAsType(String key, Object defaultValue) {
        Object value=getSetting(key);
        if (value==null) return defaultValue;
        if (value instanceof Double && defaultValue instanceof Integer) return ((Double)value).intValue(); // return an int if that is expected
        if (value instanceof Integer && defaultValue instanceof Double) return ((Integer)value).doubleValue(); // return a double if that is expected
        if (value.getClass()!=defaultValue.getClass()) return defaultValue;
        return value;
    }    
    
    /**
     * Stores a visualization setting (as an object) under a given name 
     * @param key The name of this setting
     * @param value The object representing the settings value
     */
    public void storeSetting(String key, Object value) {
        settings.put(key, value);
    }

    /**
     * Removes the key-value binding for the given setting
     * @param key The name of this setting
     */
    public void clearSetting(String key) {
        settings.remove(key);
    }
    
    /**
     * Returns a visualization setting corresponding to the given key (if it exists)
     * The method works by first invoking getSetting() to see if a value currently
     * exists for this setting in working memory. If no value can be found at this
     * point, the method will look for a persistent value which has previously been
     * stored in the users home-directory from an earlier invocation of the application.
     * @param key The name of this setting
     * @param value The object representing the settings value
     */
    public final Object getPersistentSetting(String key) {
        Object value=getSetting(key);
        if (value!=null) return value;
        try {
            String filename=client.getEngine().getMotifLabDirectory()+File.separator+key+".viz";
            value=client.getEngine().loadSerializedObject(filename);
            storeSetting(key,value);
            return value;
        } catch (Exception e) {
            //System.err.println("SYSTEM ERROR: Unable to restore persistent setting for '"+key+"': "+e.toString());
            return null;
        }        
    }
      

    /**
     * Stores a visualization setting (an object) under a given name in a persistent way 
     * (so that the setting can restored when the MotifLab is invoked again at later time)
     * @param key The name of this setting
     * @param value The object representing the settings value
     */
    public final void storePersistentSetting(String key, Object value) {
        storeSetting(key, value); // store in normal settings first
        String filename=client.getEngine().getMotifLabDirectory()+File.separator+key+".viz";
        try {
            client.getEngine().saveSerializedObject(value,filename);
        } catch (Exception e) {System.err.println("SYSTEM ERROR: Unable to store persistent setting for '"+key+"': "+e.toString());}        
    }
      
    /**
     * Returns TRUE if there is a persistent setting stored for the given key
     * @param key
     * @return
     */
    public final boolean hasPersistentSetting(String key) {
        String filename=client.getEngine().getMotifLabDirectory()+File.separator+key+".viz";
        java.io.File file=new java.io.File(filename);
        return file.exists();
    }


    /** Return true if the visualizationSettings contain an explicitly
     *  set value for the given setting
     *  Use the static final Strings in this class to refer to settings
     */
    public boolean hasSetting(String settingname) {
        return (getSetting(settingname)!=null);
    }


    /** Return true if the visualizationSettings contain an explicitly 
     *  set value for the given dataset and setting 
     *  Use the static final Strings in this class to refer to settings
     */
    public boolean hasSetting(String settingname, String datasetname) {
        return (getSetting(datasetname+"."+settingname)!=null);
    }

    /**
     * This method renames all settings currently associated with the data item of the given name
     * Note: this renaming only works for non-persistent settings
     * Also: I have a feeling that this is not thread-safe! (but renaming should only be done when no other activities are performed)
     */
    public void renameDataItem(String oldname, String newname) {
        Set<String> keys=settings.keySet();
        ArrayList<String> oldkeys=new ArrayList<String>();
        HashMap<String, Object> newvalues=new HashMap<String, Object>();
        for (String key:keys) {
            if (key.equals(oldname)) {
                Object value=getSetting(key);
                oldkeys.add(key);
                newvalues.put(newname, value);
            }
            else if (key.startsWith(oldname+".")) {
                Object value=getSetting(key);
                oldkeys.add(key);
                String newkey=key.replaceFirst(oldname+".", newname+".");
                newvalues.put(newkey, value);
            }
            else if (key.endsWith("."+oldname)) {
                Object value=getSetting(key);
                oldkeys.add(key);
                String newkey=key.replace("."+oldname, "."+newname);
                newvalues.put(newkey, value);
            }
            else if (key.contains("."+oldname+".")) {
                Object value=getSetting(key);
                oldkeys.add(key);
                String newkey=key.replaceAll("."+oldname+".", "."+newname+".");
                newvalues.put(newkey, value);
            }
        }
        for (String k:oldkeys) settings.remove(k);
        Set<String> newkeys=newvalues.keySet();
        for (String k:newkeys) settings.put(k,newvalues.get(k));
        // Check if the data item is a feature data object in the order list and master order list
        if (datatrackorder!=null) {
            for (int i=0;i<datatrackorder.size();i++) {
                if (datatrackorder.get(i).equals(oldname)) datatrackorder.set(i, newname);                          
            }
        }
        if (datatrackorderMasterList!=null) {
            for (int i=0;i<datatrackorderMasterList.size();i++) {
                if (datatrackorderMasterList.get(i).equals(oldname)) datatrackorderMasterList.set(i, newname);                          
            }
        }              
        // notifyListenersOfDataRenameEvent(oldname,newname);
    }
    
    /**
     * Copies all settings associated with an existing data object.
     * Note that this method could fail (in which case nothing will be updated)
     * 
     * @param oldfeature The name of the existing data object
     * @param newfeature The name of the new data object that should have the same settings
     * @param if replace is TRUE, all values will be copied and possibly replace existing ones. if FALSE, values will only be copied if the target does not already have a value for that setting 
     * @return true If it worked
     */
    public boolean copySettings(String olddata, String newdata, boolean replace) {
        ArrayList<String> keys=getSettingsKeys();
        if (keys==null) return false;
        for (String key:keys) {
            Object value=getSetting(key);          
            if (key.equals(olddata)) {
                if (replace || !hasSetting(newdata)) settings.put(newdata, value);
            }
            else if (key.startsWith(olddata+".")) {
                String newkey=key.replaceFirst(olddata+".", newdata+".");
                if (replace || !hasSetting(newkey)) settings.put(newkey, value);
            }
            else if (key.endsWith("."+olddata)) {
                String newkey=key.replace("."+olddata, "."+newdata);
                if (replace || !hasSetting(newkey)) settings.put(newkey, value);
            }
            else if (key.contains("."+olddata+".")) {
                String newkey=key.replaceAll("."+olddata+".", "."+newdata+".");
                if (replace || !hasSetting(newkey)) settings.put(newkey, value);
            }
        }
        return true;
    }
    
    /** Returns all the keys of all settings 
     *  NOTE: This is not thread safe. If the settings are updated while getting the keys, it will just return NULL!
     */
    private ArrayList<String> getSettingsKeys() {
        ArrayList<String> keys=new ArrayList<String>();
        try {
            keys.addAll(settings.keySet());
        } catch (ConcurrentModificationException cme) {return null;}
        return keys;
    }
    
    /**
     * Returns a reference to the GUI itself
     * @return the GUI
     */
    public MotifLabGUI getGUI() {
        return gui;
    }
    
    /** Returns a reference to the underlying instance of MotifLabEngine used by the GUI
     * @return the engine
     */
    public MotifLabEngine getEngine() {
        return client.getEngine();
    }
    
    
  /** Returns a new "unique" foreground/background color combination.
     * Tracks that do not have specified colors (yet) can 
     * let this function provide them with unique (to some extent) colors.
     * Each new call to this method returns a new fg/bg set of different colors
     * (at least up to a certain point, after which the colors are recycled)
     * This function is "synchronized" with getNewColorGradient() to make sure that unique colors are 
     * returned for each color request whether the user asks for gradients or fg/bg-color sets.
   * 
     * @return an array with two Color entries, the first is a foreground color and the second a background color
     */
    private Color[] getNewColorSet() {
        Color[] colors=new Color[]{fgColorList[colorpointer], bgColorList[colorpointer]};
        colorpointer++;
        if (colorpointer>=fgColorList.length) colorpointer=0; // cycle through the colors
        return colors;
    }
     
    public Font getDNAFont() {
        Object fontNameObject=getSetting("system.dnaFont");
        if (fontNameObject==null || !(fontNameObject instanceof String)) return dnaFont;        
        Font font=getSystemFont("system.dnaFont");
        if (font!=null) return font;
        else return dnaFont;
    }
    
    /**
     * A convenience method to fetch the foreground color for a dataset
     * If no color is currently specified for this dataset, a new set of colors
     * will be provided (for both foreground and background)
     * @param datasetname The name of the dataset
     * @return The foreground color for this dataset 
     */
    public Color getForeGroundColor(String datasetname) {
        Object value=getSetting(datasetname+"."+FOREGROUND_COLOR);       
        if (value!=null) return (Color)value;
        else { // assign a new color
            Data dataitem=client.getEngine().getDataItem(datasetname);           
            Color[] set=(dataitem instanceof DNASequenceDataset)?(new Color[]{Color.BLACK,Color.WHITE}):getNewColorSet(); // use Black&White colors for DNA tracks and generated colors for other tracks
            if (!hasSetting(datasetname+"."+BACKGROUND_COLOR)) storeSetting(datasetname+"."+BACKGROUND_COLOR,set[1]); // if background is already assigned, don't replace it           
            storeSetting(datasetname+"."+FOREGROUND_COLOR,set[0]);
            storeSetting(datasetname+"."+COLORGRADIENT,null); // invalidate gradients
            return set[0];
        }
    }
    
    /**
     * A convenience method to set the foreground color for a dataset
     * @param datasetname The name of the dataset
     * @param color The foreground color for this dataset
     */
    public void setForeGroundColor(String datasetname, Color color) {
        if (datasetname==null) return;               
        storeSetting(datasetname+"."+FOREGROUND_COLOR,color);
        storeSetting(datasetname+"."+COLORGRADIENT,null); // invalidate gradients
        redraw();
    }
    
    /**
     * A convenience method to set the foreground color for multiple datasets
     * @param datasetnames The names of the datasets
     * @param color The foreground color for these datasets
     */
    public void setForeGroundColor(String datasetnames[], Color color) {
        for (String datasetname:datasetnames) {                       
            storeSetting(datasetname+"."+FOREGROUND_COLOR,color);
            storeSetting(datasetname+"."+COLORGRADIENT,null); // invalidate gradients
        }
        redraw();
    }
    
    
     /**
     * A convenience method to fetch the secondary color for a dataset
     * If no secondary color is currently specified for this dataset, a new color will be provided
     * @param datasetname The name of the dataset
     * @return The secondary color for this dataset 
     */
    public Color getSecondaryColor(String datasetname) {
        Object value=getSetting(datasetname+"."+SECONDARY_COLOR);
        if (value!=null) return (Color)value;
        else {
            Color foreground=getForeGroundColor(datasetname);
            Color secondary;
                 if (foreground.equals(Color.RED)) secondary=Color.GREEN;
            else if (foreground.equals(Color.GREEN)) secondary=Color.RED;
            else if (foreground.equals(Color.BLUE)) secondary=Color.YELLOW;
            else if (foreground.equals(Color.YELLOW)) secondary=Color.BLUE;
            else secondary=new Color(255-foreground.getRed(),255-foreground.getGreen(),255-foreground.getBlue());
            setSecondaryColor(datasetname,secondary);
            return secondary;
        }
    }
    
    /**
     * A convenience method to set the secondary color for a dataset
     * @param datasetname The name of the dataset
     * @param color The secondary color for this dataset
     */
    public void setSecondaryColor(String datasetname, Color color) {
        storeSetting(datasetname+"."+SECONDARY_COLOR,color);
        storeSetting(datasetname+"."+SECONDARY_COLORGRADIENT,null); // invalidate gradients
        redraw();
    }
    
    /**
     * A convenience method to set the secondary color for multiple datasets
     * @param datasetnames The names of the datasets
     * @param color The secondary color for these datasets
     */
    public void setSecondaryColor(String datasetnames[], Color color) {
        for (String datasetname:datasetnames) {
            storeSetting(datasetname+"."+SECONDARY_COLOR,color);
            storeSetting(datasetname+"."+SECONDARY_COLORGRADIENT,null); // invalidate gradients
        }
        redraw();
    }   
    
    
     /**
     * A convenience method to fetch the baseline color for a dataset
     * If no baseline color is currently specified for this dataset, a new color will be provided
     * @param datasetname The name of the dataset
     * @return The baseline color for this dataset 
     */
    public Color getBaselineColor(String datasetname) {
        Object value=getSetting(datasetname+"."+BASELINE_COLOR);
        if (value!=null) return (Color)value;
        else {
            setBaselineColor(datasetname,Color.lightGray);
            return Color.lightGray;
        }
    }
    
    /**
     * A convenience method to set the baseline color for a dataset
     * @param datasetname The name of the dataset
     * @param color The baseline color for this dataset
     */
    public void setBaselineColor(String datasetname, Color color) {
        storeSetting(datasetname+"."+BASELINE_COLOR,color);
        redraw();
    }
    
    /**
     * A convenience method to set the baseline color for multiple datasets
     * @param datasetnames The names of the datasets
     * @param color The baseline color for these datasets
     */
    public void setBaselineColor(String datasetnames[], Color color) {
        for (String datasetname:datasetnames) {
            storeSetting(datasetname+"."+BASELINE_COLOR,color);
        }
        redraw();
    }   
    
    /**
     * A convenience method to fetch the background color for a dataset
     * If no color is currently specified for this dataset, a new set of colors
     * will be provided 
     * @param datasetname The name of the dataset
     * @return The background color for this dataset 
     */
    public Color getBackGroundColor(String datasetname) {        
        Object value=getSetting(datasetname+"."+BACKGROUND_COLOR);
        if (value!=null) return (Color)value;
        else {
            Data dataitem=client.getEngine().getDataItem(datasetname);           
            Color[] set=(dataitem instanceof DNASequenceDataset)?(new Color[]{Color.BLACK,Color.WHITE}):getNewColorSet(); // use Black&White colors for DNA tracks and generated colors for other tracks
            setBackGroundColor(datasetname,set[1]);   
            return set[1];
        }
    }
    
    /**
     * A convenience method to set the background color for a dataset
     * @param datasetname The name of the dataset
     * @param background The background color for this dataset
     */
    public void setBackGroundColor(String datasetname, Color background) {
        if (datasetname==null) return;     
        storeSetting(datasetname+"."+BACKGROUND_COLOR,background);
        storeSetting(datasetname+"."+COLORGRADIENT,null); // invalidate gradients
        storeSetting(datasetname+"."+SECONDARY_COLORGRADIENT,null); // invalidate gradients
        redraw();
    }

    /**
     * A convenience method to set the background color for multiple datasets
     * @param datasetnames The names of the datasets
     * @param color The background color for these datasets
     */
    public void setBackGroundColor(String datasetnames[], Color color) {
        if (datasetnames==null) return;         
        for (String datasetname:datasetnames) { 
            storeSetting(datasetname+"."+BACKGROUND_COLOR,color);
            storeSetting(datasetname+"."+COLORGRADIENT,null); // invalidate gradients
            storeSetting(datasetname+"."+SECONDARY_COLORGRADIENT,null); // invalidate gradients
        }
        redraw();
    }
       
    /**
     * A convenience method to fetch the color gradient for a dataset
     * @param datasetname The name of the dataset
     * @return A color gradient for this dataset (based on shades between foreground and background)
     */
    public ColorGradient getColorGradient(String datasetname) {
        Object value=getSetting(datasetname+"."+COLORGRADIENT);
        if (value!=null) return (ColorGradient)value; 
        else {
            Color fgcolor=getForeGroundColor(datasetname);
            Color bgcolor=getBackGroundColor(datasetname);
            //ColorGradient gradient=new ColorGradient(fgcolor, bgcolor); // this returns a "solid" gradient (deprecated in this context)
            ColorGradient gradient=new ColorGradient(fgcolor); // this returns a gradient based on different levels of transparency (alpha channel)          
            storeSetting(datasetname+"."+COLORGRADIENT,gradient);
            return gradient;
        }    
    }
    
    /**
     * A convenience method to fetch the secondary color gradient for a dataset
     * @param datasetname The name of the dataset
     * @return A color gradient for this dataset (based on shades between secondary color and background)
     */
    public ColorGradient getSecondaryGradient(String datasetname) {
        Object value=getSetting(datasetname+"."+SECONDARY_COLORGRADIENT);
        if (value!=null) return (ColorGradient)value; 
        else {
            Color secondarycolor=getSecondaryColor(datasetname);
            Color bgcolor=getBackGroundColor(datasetname);
            //ColorGradient gradient=new ColorGradient(secondarycolor, bgcolor); // this returns a "solid" gradient (deprecated in this context) 
            ColorGradient gradient=new ColorGradient(secondarycolor); // this returns a gradient based on different levels of transparency (alpha channel) 
            storeSetting(datasetname+"."+SECONDARY_COLORGRADIENT,gradient);
            return gradient;
        }    
    }    
    
    /** Returns a rainbow color gradient with colors from blue to red */
    public ColorGradient getRainbowGradient() {
        if (rainbowgradient==null) rainbowgradient=new ColorGradient();
        return rainbowgradient;
    }
    
    /**
     * Given a foreground and background color corresponding to a color pair
     * from the predefined list of standard colors, this function will return
     * the next color pair in the list (if the supplied pair is not a standard
     * color pair, the first preset color pair will be returned).
     * @param foreground
     * @param background
     * @return Color[] a list containing the next foreground and background color pair (size=2);
     */
    public Color[] getNextPresetColorPair(Color foreground,Color background) {
        int size=bgColorList.length;
        if (foreground==null || background==null) return new Color[]{fgColorList[0],bgColorList[0]};
        int index=getIndexForColorPair(foreground, background);  
        if (index<0)  return new Color[]{fgColorList[0],bgColorList[0]};
        if (index==size-1) index=0; else index++;
        return new Color[]{fgColorList[index],bgColorList[index]};
    }
    
    /**
     * Given a foreground and background color corresponding to a color pair
     * from the predefined list of standard colors, this function will return
     * the previous color pair in the list (if the supplied pair is not a standard
     * color pair, the first preset color pair will be returned).
     * @param foreground
     * @param background
     * @return Color[] a list containing the next foreground and background color pair (size=2);
     */
    public Color[] getPreviousPresetColorPair(Color foreground,Color background) {
        int size=bgColorList.length;
        if (foreground==null || background==null) return new Color[]{fgColorList[0],bgColorList[0]};
        int index=getIndexForColorPair(foreground, background);   
        if (index<0)  return new Color[]{fgColorList[0],bgColorList[0]};
        if (index==0) index=size-1; else index--;
        return new Color[]{fgColorList[index],bgColorList[index]};
    }
    
    /** 
     * Returns the preset index corresponding to the given color pair 
     * or -1 if there is no such color pair
     */
    private int getIndexForColorPair(Color foreground,Color background) {
       int size=bgColorList.length;
       for (int i=0;i<size;i++) {
            if (foreground.equals(fgColorList[i]) && background.equals(bgColorList[i])) return i;
       }   
       return -1;
    }
    
     /**
     * A convenience method to fetch the "Scale numerical range by individual sequences" property of the dataset.
     * If this method return TRUE, each sequence should be shown with an individual range that is fitted to the range of values in that sequence.
     * If this method returns FALSE, all sequences should shown with the same range
     * @param datasetname The name of the dataset
     * @return  
     */
    public boolean scaleShownNumericalRangeByIndividualSequence(String datasetname) {
        Object value=getSetting(datasetname+"."+SCALE_RANGE_BY_INDIVIDUAL_SEQUENCES);
        if (value instanceof Boolean) return (Boolean)value;
        else {
            return false;
        }
    }
    
    /**
     * Sets the "Scale numerical range by individual sequences" flag.
     * If this is set (TRUE), each sequence will be shown with an individual range that is fitted to the range of values in that sequence.
     * If not set (FALSE), all sequences will shown with the same range
     * @param datasetname The name of the dataset
     * @param individual Whether or not sequences should be shown with individual ranges
     */
    public void setScaleShownNumericalRangeByIndividualSequence(String datasetname, boolean individual) {
        if (!individual) clearSetting(datasetname+"."+SCALE_RANGE_BY_INDIVIDUAL_SEQUENCES); // only store positive values
        else storeSetting(datasetname+"."+SCALE_RANGE_BY_INDIVIDUAL_SEQUENCES,individual);
        redraw();
    }    
    
    
     /**
     * A convenience method to get the color to be used for the specified base when visualizing DNA sequences
     * @param base The letter representing the base (A,C,G,T or X)
     * @return color The color to be used for this base
     */
    public Color getBaseColor(char base) {
        switch (base) {
            case 'A': break;
            case 'C': break;
            case 'G': break;
            case 'T': break;
            case 'a': base='A';break;
            case 'g': base='G';break;
            case 'c': base='C';break;
            case 't': base='T';break;
            default : base='X';
        }
        Object color=getPersistentSetting(BASE_COLOR+"."+base);
        if (color!=null) return (Color)color;
        else return Color.GRAY;
    }

    /** Returns the base colors for A,C,G and T in that order */
    public Color[] getBaseColors() {
        return new Color[]{getBaseColor('A'),getBaseColor('C'),getBaseColor('G'),getBaseColor('T')};
    }
    
   /**
     * A convenience method to determine whether the color to be used for the specified base when visualizing DNA sequences
     * can be considered to be "dark"
     * @param base The letter representing the base (A,C,G,T or X)
     */
    public boolean isBaseColorDark(char base) {
        switch (base) {
            case 'A': break;
            case 'C': break;
            case 'G': break;
            case 'T': break;
            case 'a': base='A';break;
            case 'g': base='G';break;
            case 'c': base='C';break;
            case 't': base='T';break;
            default : base='X';
        }
        Object color=getPersistentSetting(BASE_COLOR+"."+base);
        if (color!=null) return isDark((Color)color);
        else return false;
    }

   /**
     * A convenience method to determine whether the color to be used for the specified base when visualizing DNA sequences
     * can be considered to be "dark"
     * @param base The letter representing the base (A,C,G,T or X)
     */
    public boolean isBaseColorVeryDark(char base) {
        switch (base) {
            case 'A': break;
            case 'C': break;
            case 'G': break;
            case 'T': break;
            case 'a': base='A';break;
            case 'g': base='G';break;
            case 'c': base='C';break;
            case 't': base='T';break;
            default : base='X';
        }
        Object color=getPersistentSetting(BASE_COLOR+"."+base);
        if (color!=null) return isVeryDark((Color)color);
        else return false;
    }

    /**
     * A convenience method to set the color for specified base when visualizing DNA sequences
     * @param base The letter representing the base (A,C,G,T or X)
     * @param color The color to be used for this base
     */
    public void setBaseColor(char base, Color color) {
        storePersistentSetting(BASE_COLOR+"."+base,color);
        redraw();
    }

    /**
     * Sets a palette of default colors for visualizing DNA bases (if no settings are stored before)
     */
    private void setDefaultBaseColors() {
        if (getPersistentSetting(BASE_COLOR+".A")==null) storePersistentSetting(BASE_COLOR+".A", new Color(0,230,0)); // slightly darker green
        if (getPersistentSetting(BASE_COLOR+".C")==null) storePersistentSetting(BASE_COLOR+".C", Color.blue);
        if (getPersistentSetting(BASE_COLOR+".G")==null) storePersistentSetting(BASE_COLOR+".G", new Color(255,205,0)); // orange-yellow
        if (getPersistentSetting(BASE_COLOR+".T")==null) storePersistentSetting(BASE_COLOR+".T", Color.red);
        if (getPersistentSetting(BASE_COLOR+".X")==null) storePersistentSetting(BASE_COLOR+".X", Color.black);       
    }
   
    /**
     * Returns a color to use for the given feature. If not color is currently
     * assigned to this feature a new color will be selected from a predefined palette.
     * @param feature The name of the feature
     * @return A color to use for this feature
     */
    private int paletteindex=0;

    /** Resets the palette index counter for feature colors */
    public void resetPaletteIndex() {paletteindex=0;}

    public Color getFeatureColor(String feature) {
        Object value=getSetting(feature+"."+MULTICOLOR_PALETTE);
        if (value!=null) return (Color)value;
        else {
            Color featureColor;
            if (shouldColorMotifsByClass() && client.getEngine().dataExists(feature, Motif.class)) {
                    String motifclass=((Motif)client.getEngine().getDataItem(feature)).getClassification();
                    if (motifclass!=null) {
                        String[] path=MotifClassification.getClassPath(motifclass);
                        if (path.length>2) motifclass=path[1];
                    } else motifclass=MotifClassification.UNKNOWN_CLASS_LABEL;
                    featureColor=getColorForMotifClass(motifclass);
            } else {
                featureColor=getColorFromIndex(paletteindex);
                paletteindex++;
            }
            storeSetting(feature+"."+MULTICOLOR_PALETTE,featureColor);
            return featureColor;
        }
    }



    /**
     * Sets a color to use for the given feature.
     * @param feature The name of the feature
     * @param newcolor A color to use for this feature
     */
    public void setFeatureColor(String feature, Color newcolor, boolean doRedraw) {
         if (feature==null) return;
         storeSetting(feature+"."+MULTICOLOR_PALETTE,newcolor);
         if (doRedraw) redraw();
    }

    /**
     * Sets a color to use for the given features.
     * @param features The names of the features
     * @param newcolor A color to use for these features
     */
    public void setFeatureColor(String[] features, Color newcolor, boolean doRedraw) {
        if (features==null) return; 
        for (String feature:features) {
            if (feature!=null) storeSetting(feature+"."+MULTICOLOR_PALETTE,newcolor);
        }
        if (doRedraw) redraw();
    }

    /**
     * Returns true if the feature currently has an assigned color
     * @param feature The name of the feature
     */
    public boolean hasFeatureColor(String feature) {
         Object value=getSetting(feature+"."+MULTICOLOR_PALETTE);
         return (value!=null);
    }


     /**
     * Sets a color to use for the outlines of modules in module tracks
     * @param color The argument could either be a Color object representing a
     *              specific color to use, or a string (constant COLOR_MODULE_BY_TYPE)
     *              which means the color to use should be based on the type of module
     *              or NULL which means the outline should not be drawn
     *
     */
    public void setModuleOutlineColor(Object color) {
        if (color==null || color instanceof Color) storePersistentSetting(MODULE_OUTLINE_COLOR,color);
        else storePersistentSetting(MODULE_OUTLINE_COLOR,COLOR_MODULE_BY_TYPE);
    }

     /**
     * Specifies the color to use when drawing outlines for modules
     * @return  The return value could either be a Color object representing a
     *              specific color to use, or a string (constant COLOR_MODULE_BY_TYPE)
     *              which means the color to use should be based on the type of module
     *              or NULL which means the outline should not be drawn
     */
    public Object getModuleOutlineColor() {
         if (!settings.containsKey(MODULE_OUTLINE_COLOR)) {
             if (!hasPersistentSetting(MODULE_OUTLINE_COLOR)) {
                 setModuleOutlineColor(COLOR_MODULE_BY_TYPE); // this will also store the value in 'settings' so that it is available next time
                 return COLOR_MODULE_BY_TYPE;
             } else {
                return getPersistentSetting(MODULE_OUTLINE_COLOR); // this will also store the value in 'settings' so that it is available next time
             } 
         }
         Object value=getSetting(MODULE_OUTLINE_COLOR);
         return value;
    }

     /**
     * Sets a color to use when filling intra-module (background) space for modules
     * @param color The argument could either be a Color object representing a
     *              specific color to use, or a string (constant COLOR_MODULE_BY_TYPE)
     *              which means the color to use should be based on the type of module
     *              or NULL which means the module background should not be drawn
     *
     */
    public void setModuleFillColor(Object color) {
        if (color==null || color instanceof Color) storePersistentSetting(MODULE_FILL_COLOR,color);
        else storePersistentSetting(MODULE_FILL_COLOR,COLOR_MODULE_BY_TYPE);
    }

     /**
     * Specifies the color to use when filling intra-module (background) space for modules
     * @return  The return value could either be a Color object representing a
     *              specific color to use, or a string (constant COLOR_MODULE_BY_TYPE)
     *              which means the color to use should be based on the type of module
     *              or NULL which means the outline should not be drawn
     */
    public Object getModuleFillColor() {
         if (!settings.containsKey(MODULE_FILL_COLOR)) {
             if (!hasPersistentSetting(MODULE_FILL_COLOR)) {
                 setModuleFillColor(COLOR_MODULE_BY_TYPE); // this will also store the value in 'settings' so that it is available next time
                 return COLOR_MODULE_BY_TYPE;
             } else {
                return getPersistentSetting(MODULE_FILL_COLOR); // this will also store the value in 'settings' so that it is available next time
             }                   
         }
         Object value=getSetting(MODULE_FILL_COLOR);
         return value;
    }

 /** Goes through all motifs registered with engine and sets the color of each motif to a predefined class color 
  *  It also updates the label colors for all registered classes
  */
 private void updateMotifColorsByClass() {
    ArrayList<Data> motifs=client.getEngine().getAllDataItemsOfType(Motif.class);
    for (Data data:motifs) {
        String motifclass=((Motif)data).getClassification();
        if (motifclass!=null) {
            String[] path=MotifClassification.getClassPath(motifclass);
            if (path.length>2) motifclass=path[1];
        } else motifclass=MotifClassification.UNKNOWN_CLASS_LABEL;
        Color newcolor=getColorForMotifClass(motifclass);
        setFeatureColor(data.getName(), newcolor, false);
    }
    String[] classlabels=MotifClassification.getClassStrings();
    for (String classlabel:classlabels) {
      String lookupkey=classlabel;
      String[] path=MotifClassification.getClassPath(classlabel);
      if (path.length>2) lookupkey=path[1];
      setClassLabelColor(classlabel, getColorForMotifClass(lookupkey));
    }
    setClassLabelColor(MotifClassification.UNKNOWN_CLASS_LABEL, getColorForMotifClass(MotifClassification.UNKNOWN_CLASS_LABEL));
    redraw();
 }

 /** Goes through all motifs registered with engine and 'resets' the color for each motif
  */
 private void clearMotifColorsByClass() {
    ArrayList<Data> motifs=client.getEngine().getAllDataItemsOfType(Motif.class);
    Collections.sort(motifs, new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    resetPaletteIndex();
    for (Data data:motifs) {
        Color featureColor=getColorFromIndex(paletteindex);
        storeSetting(data.getName()+"."+MULTICOLOR_PALETTE,featureColor);
        paletteindex++;
    }
    redraw();
 }

    /**
     * A convenience method to set the visibility of a region type feature
     * This differs from other 'setVisible' methods in that it will clear the current setting
     * if show==TRUE (and thus isVisible(X) will later return the default TRUE value)
     * and only store the setting if show==FALSE
     * @param featureName The name of the feature
     * @param show True if the feature should be shown and false if it should be hidden
     * @param redraw set to True if the gui should be automatically redrawn afterwards
     */
    public void setRegionTypeVisible(String regionType, boolean show, boolean redraw) {
        if (regionType==null) return;  
        if (show) clearSetting(regionType+"."+VISIBLE); // No stored setting means 'show' (to save memory space)
        else storeSetting(regionType+"."+VISIBLE, show); // only return if hidden
        if (redraw) redraw();
    } 
    
    public void setRegionTypeVisible(String[] regionTypes, boolean show, boolean redraw) {
        if (regionTypes==null) return;  
        for (String regionType:regionTypes) {
            if (regionType==null) continue;
            if (show) clearSetting(regionType+"."+VISIBLE); // No stored setting means 'show' (to save memory space)
            else storeSetting(regionType+"."+VISIBLE, show); // only return if hidden
        }
        if (redraw) {redraw();}
         
    }    
     
    /**
     * A convenience method to decide whether or not the specified region type should be drawn at all
     * @param regiontype The type name of the region
     * @return a boolean value which is true if the region type should be displayed and false if it should be hidden
     */
    public boolean isRegionTypeVisible(String regiontype) {
        Object value=getSetting(regiontype+"."+VISIBLE);
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else {
            return true;       
        }
    }    
    
    /**
     * A convenience method to set the visibility of a feature (such as motifs)
     * @param featureNames The names of the features
     * @param show True if the feature should be shown and false if it should be hidden
     * @param redraw set to True if the gui should be automatically redrawn afterwards
     */
    public void setFeatureVisible(String[] featureNames, boolean show, boolean redraw) {
        if (featureNames==null) return;  
        for (String featureName:featureNames) {  
           if (featureName!=null) storeSetting(featureName+"."+VISIBLE, show);
        }
        if (redraw) redraw();           
    }    
    
    /**
     * Sets a color to use for the given class label. 
     * @param classlabel The name of the classlabel
     * @param newcolor A color to use for this classlabel
     */
    public void setClassLabelColor(String classlabel, Color newcolor) {
        if (classlabel==null) return;           
        storeSetting(CLASS_LABEL_COLOR+"."+classlabel,newcolor);
    }
    
    /**
     * Returns a color to use for the given class label. 
     * Note that the returned value can be NULL. If no color is assigned for the label, it will not be assigned one automatically
     * @param classlabel The name of the classlabel
     */
    public Color getClassLabelColor(String classlabel) {
        return (Color)getSetting(CLASS_LABEL_COLOR+"."+classlabel);
    }
  
    /**
     * Specifies whether motifs should be assigned colors based on their classification
     * and not by random assignment.
     * If this setting was previously FALSE but is set to TRUE, all motifs will have their color updated
     * @param newsetting
     */
    public void setColorMotifsByClass(boolean newsetting) {
        boolean oldSetting=shouldColorMotifsByClass();
        storeSetting(COLOR_MOTIFS_BY_CLASS, newsetting);
        if (oldSetting==false && newsetting==true) updateMotifColorsByClass();
        else if (oldSetting==true && newsetting==false) clearMotifColorsByClass();
    }

    /**
     * Returns TRUE if the 'color motifs by class' setting is currently in effect
     * @return
     */
    public boolean shouldColorMotifsByClass() {
        Object value=getSetting(COLOR_MOTIFS_BY_CLASS);
        if (value!=null) return (Boolean)value;
        else {
            storeSetting(COLOR_MOTIFS_BY_CLASS,Boolean.FALSE);
            return Boolean.FALSE;
        }
    }

    /**
     * Sets a color to use for the label for the given sequence 
     * @param sequencename The name of the sequence
     * @param newcolor A color to use for this sequencelabel
     */
    public void setSequenceLabelColor(String sequencename, Color newcolor) {
        if (sequencename==null) return;
        storeSetting(SEQUENCE_LABEL_COLOR+"."+sequencename,newcolor);
    }

    /**
     * Sets a color to use for the label for the given sequences
     * @param sequencenames The names of the sequences
     * @param newcolor A color to use for these sequencelabels
     */
    public void setSequenceLabelColor(String[] sequencenames, Color newcolor) {
        if (sequencenames==null || sequencenames.length==0) return;
        for (String sequencename:sequencenames) {
           if (sequencename!=null) storeSetting(SEQUENCE_LABEL_COLOR+"."+sequencename,newcolor);
        }
    }
    /**
     * Returns a color to use for the label for the given sequence
     * If no explicit color has been set the return value will default to BLACK
     * @param classlabel The name of the classlabel
     */
    public Color getSequenceLabelColor(String sequencename) {
        Color color=(Color)getSetting(SEQUENCE_LABEL_COLOR+"."+sequencename);
        if (color==null) return Color.BLACK;
        else return color;
    }
    
    /**
     * Sets the alignment strategy to use for sequence labels with respect to the feature tracks stack
     * @param alignment An integer code specifying alignment based on SwingConstants (e.g. SwingConstants.TOP)
     */
    public void setSequenceLabelAlignment(int alignment) {
         storePersistentSetting(SEQUENCE_LABEL_ALIGNMENT, alignment);
         redraw();
    }
    
    /**
     * Returns the alignment strategy to use for sequence labels with respect to the feature tracks stack
     * The returned value is as integer code specifying alignment based on SwingConstants (e.g. SwingConstants.TOP)
     */
    public int getSequenceLabelAlignment() {
        Integer align=(Integer)getPersistentSetting(SEQUENCE_LABEL_ALIGNMENT);
        if (align==null) {
            setSequenceLabelAlignment(SwingConstants.TOP);
            return SwingConstants.TOP;
        } else return align.intValue();
    }    
    
    /**
     * Sets a color to use for the cluster with the given name
     * @param clustername The name of the cluster
     * @param newcolor A color to use for this cluster
     */
    public void setClusterColor(String clustername, Color newcolor) {
         if (clustername==null) return;         
         storeSetting(CLUSTER_COLOR+"."+clustername,newcolor);
    }
    
    /**
     * Returns a color to use for the cluster with the given name
     * If no explicit color has been set a new color will be assigned
     * @param clustername The name of the cluster
     */
    public Color getClusterColor(String clustername) {
        Color color=(Color)getSetting(CLUSTER_COLOR+"."+clustername);
        if (color==null) {
            clustercolorpointer++;
            color=fgColorList[clustercolorpointer%fgColorList.length];
            setClusterColor(clustername, color);
        }
        return color;
    }
    
    
    
    /**
     * Sets a background color to use for the main visualization panel
     * @param newcolor A color to use
     */
    public void setVisualizationPanelBackgroundColor(Color newcolor) {
         storePersistentSetting(MAINPANEL_BACKGROUND,newcolor);
         if (gui!=null) gui.getVisualizationPanel().getMainVisualizationPanel().setBackground(newcolor);
         redraw();
    }
    
    /**
     * Returns a background color to use for the main visualization panel
     */
    public Color getVisualizationPanelBackgroundColor() {
        Color color=(Color)getPersistentSetting(MAINPANEL_BACKGROUND);
        if (color!=null) {
            return color;
        } else {
            color=new javax.swing.JPanel().getBackground();
            storePersistentSetting(MAINPANEL_BACKGROUND,color);
            return color;
        }
    }

    /**
     * Returns the color to use when drawing the base cursor
     * @return
     */
    public Color getBaseCursorColor() {
        Color color=(Color)getPersistentSetting(BASE_CURSOR_COLOR);
        if (color!=null) {
            return color;
        } else {
            Color basecursorcolor=Color.MAGENTA; // slightly transparent
            storePersistentSetting(BASE_CURSOR_COLOR, basecursorcolor);
            return basecursorcolor;
        }
    }

    /**
     * Sets the color to use when drawing the base cursor
     */
    public void setBaseCursorColor(Color basecolor) {
         storePersistentSetting(BASE_CURSOR_COLOR,basecolor);
    }
    
    /**
     * Gets the sequence margin, which is the number of extra pixels between sequences
     * in the visualize panel
     */
    public int getSequenceMargin() {
        Object value=getPersistentSetting(SEQUENCE_MARGIN);
        if (value!=null) {
            return ((Integer)value).intValue();
        } else {
            storePersistentSetting(SEQUENCE_MARGIN,new Integer(DEFAULT_SEQUENCE_MARGIN));
            return DEFAULT_SEQUENCE_MARGIN;
        }
    }
    
    /**
     * Sets the sequence margin, which is the number of extra pixels between sequences
     * in the visualize panel
     * @param margin The new margin
     */
    public void setSequenceMargin(int margin) {
            if (margin<SEQUENCE_MARGIN_MIN_HEIGHT) margin=SEQUENCE_MARGIN_MIN_HEIGHT;
            else if (margin>SEQUENCE_MARGIN_MAX_HEIGHT) margin=SEQUENCE_MARGIN_MAX_HEIGHT;
            storePersistentSetting(SEQUENCE_MARGIN,new Integer(margin));
            //rearrangeDataTracks();
            notifyListenersOfSequenceMarginChange(margin);
            redraw();
    }
    
    /**
     * A convenience method to fetch the height that should be used when drawing
     * the datatrack (excluding borders)
     * @param datasetname The name of the dataset
     * @return The height of the dataset track
     */
    public int getDataTrackHeight(String datasetname) {
        Object value=getSetting(datasetname+"."+TRACK_HEIGHT);
        if (value!=null) return ((Integer)value).intValue(); 
        else {
            int newtrackHeight=6;
            Data dataset=client.getEngine().getDataItem(datasetname);
                 if (dataset instanceof DNASequenceDataset) newtrackHeight=DEFAULT_TRACK_HEIGHT_DNA;
            else if (dataset instanceof NumericDataset) newtrackHeight=DEFAULT_TRACK_HEIGHT_NUMERIC;
            else if (dataset instanceof RegionDataset && ((RegionDataset)dataset).isMotifTrack()) newtrackHeight=DEFAULT_TRACK_HEIGHT_MOTIF;
            else if (dataset instanceof RegionDataset && ((RegionDataset)dataset).isModuleTrack()) newtrackHeight=DEFAULT_TRACK_HEIGHT_MODULE;
            else if (dataset instanceof RegionDataset) newtrackHeight=DEFAULT_TRACK_HEIGHT_REGION;
            storeSetting(datasetname+"."+TRACK_HEIGHT,new Integer(newtrackHeight));
            return newtrackHeight;
        }
    }

     /**
     * A convenience method to set the height that should be used when drawing
     * the dataset (excluding borders)
     * @param datasetname The name of the dataset
     * @param height The height of the dataset track
     * @param redraw Set this to TRUE if visualization should be updated right away
     */
    public void setDataTrackHeight(String datasetname, int height, boolean redraw) {
        if (datasetname==null) return;       
        storeSetting(datasetname+"."+TRACK_HEIGHT,new Integer(height));
        if (redraw) {
            notifyListenersOfTrackOrderRearrangement(VisualizationSettingsListener.UPDATED,null);
            redraw();
        }
    }
    
    public void setDataTrackHeight(String[] datasetnames, int height, boolean redraw) {          
        if (datasetnames==null) return;       
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+TRACK_HEIGHT,new Integer(height));
        }       
        if (redraw) {
            //calculateTotalTracksHeight(); // recalculate total tracks height
            notifyListenersOfTrackOrderRearrangement(VisualizationSettingsListener.UPDATED,null);
            redraw();
        }
    }    
 

    /**
     * A convenience method to fetch the height that should be used for regions
     * when drawing in expanded mode
     * @param datasetname The name of the dataset
     * @return The height of the regions
     */
    public int getExpandedRegionHeight(String datasetname) {     
        Object value=getSetting(datasetname+"."+EXPANDED_REGIONS_HEIGHT);  
        if (value!=null) return ((Integer)value).intValue(); 
        else {
            int newtrackHeight=DEFAULT_EXPANDED_REGIONS_HEIGHT;
            storeSetting(datasetname+"."+EXPANDED_REGIONS_HEIGHT,new Integer(newtrackHeight));
            return newtrackHeight;
        }
    }

     /**
     * A convenience method to set the height that should be used for regions
     * when drawing in expanded mode
     * @param datasetname The name of the dataset
     * @param height The height of the regions
     * @param redraw Set this to TRUE if visualization should be updated right away
     */
    public void setExpandedRegionHeight(String datasetname, int height, boolean redraw) {
        if (datasetname==null) return;
        storeSetting(datasetname+"."+EXPANDED_REGIONS_HEIGHT,new Integer(height));
        if (redraw) {
            //calculateTotalTracksHeight(); // recalculate total tracks height
            notifyListenersOfTrackOrderRearrangement(VisualizationSettingsListener.UPDATED,null);
            redraw();
        }
    }
    
    public void setExpandedRegionHeight(String[] datasetnames, int height, boolean redraw) {   
        if (datasetnames==null) return;
        for (String datasetname:datasetnames) {        
            if (datasetname!=null) storeSetting(datasetname+"."+EXPANDED_REGIONS_HEIGHT,new Integer(height));
        }       
        if (redraw) {
            //calculateTotalTracksHeight(); // recalculate total tracks height
            notifyListenersOfTrackOrderRearrangement(VisualizationSettingsListener.UPDATED,null);
            redraw();
        }
    }     
    
    
    /**
     * A convenience method to fetch the spacing between rows of regions
     * when drawing in expanded mode
     * @param datasetname The name of the dataset
     * @return The row spacing
     */
    public int getRowSpacing(String datasetname) {
        Object value=getSetting(datasetname+"."+EXPANDED_REGIONS_ROW_SPACING);
        if (value!=null) return ((Integer)value).intValue(); 
        else {
            storeSetting(datasetname+"."+EXPANDED_REGIONS_ROW_SPACING, DEFAULT_EXPANDED_REGIONS_ROW_SPACING);
            return DEFAULT_EXPANDED_REGIONS_ROW_SPACING;
        }
    }

     /**
     * A convenience method to set the spacing between rows of regions
     * when drawing in expanded mode
     * @param datasetname The name of the dataset
     * @param spacing The new row spacing
     * @param redraw Set this to TRUE if visualization should be updated right away
     */
    public void setRowSpacing(String datasetname, int spacing, boolean redraw) {
        if (datasetname==null) return;
        storeSetting(datasetname+"."+EXPANDED_REGIONS_ROW_SPACING,new Integer(spacing));
        if (redraw) {
            //calculateTotalTracksHeight(); // recalculate total tracks height
            notifyListenersOfTrackOrderRearrangement(VisualizationSettingsListener.UPDATED,null);
            redraw();
        }
    }
    
    public void setRowSpacing(String[] datasetnames, int spacing, boolean redraw) {   
        if (datasetnames==null) return;
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+EXPANDED_REGIONS_ROW_SPACING,new Integer(spacing));
        }       
        if (redraw) {
            //calculateTotalTracksHeight(); // recalculate total tracks height
            notifyListenersOfTrackOrderRearrangement(VisualizationSettingsListener.UPDATED,null);
            redraw();
        }
    }  
    
    
    /**
     * A convenience method to decide the type of graph that should be used when drawing a datatrack 
     * @param datasetname The name of the dataset
     * @return The graph type for this dataset
     */
    public String getGraphType(String datasetname) {
        // Note that when it is needed to update other properties also besides GRAPH_TYPE, 
        // the storeSetting() method is used rather than the normal convenience methods to avoid invoking an immediate redraw()
        Object value=getSetting(datasetname+"."+GRAPH_NAME); // The Graph type propery is now called GRAPH_NAME (String), but previously it was called GRAPH_TYPE (Integer)
        if (value instanceof String) {return (String)value;}
        else {
            // check if a numeric setting exists (old deprecated property)
            value=getSetting(datasetname+"."+GRAPH_TYPE);
            if (value instanceof Integer) {
                String newSetting=getGraphTypeFromNumber((Integer)value);
                if (((Integer)value)==REGION_MULTICOLOR_GRAPH) storeSetting(datasetname+"."+USE_MULTI_COLORED_REGIONS,true);
                if (!newSetting.isEmpty()) {
                  storeSetting(datasetname+"."+GRAPH_NAME,newSetting);
                  return newSetting;
                }
            } else if (value instanceof String) { // for a brief moment the old GRAPH_TYPE was a String property. This is to remain backwards compatible with that forgotten era
                String newSetting=(String)value;
                if (!newSetting.isEmpty()) {
                  storeSetting(datasetname+"."+GRAPH_NAME,newSetting);
                  return newSetting;           
                }
            }
            // no applicable setting exist. Create and set a default to return
            Data data=client.getEngine().getDataItem(datasetname);
            if (data==null) return "";
            String graphtype="";
                 if (data instanceof DNASequenceDataset) graphtype="DNA";
            else if (data instanceof NumericDataset) graphtype="Graph (filled)";
            else if (data instanceof RegionDataset && (((RegionDataset)data).isMotifTrack() || ((RegionDataset)data).isModuleTrack())) {
                graphtype="Region";
                if (!hasSetting(datasetname+"."+USE_MULTI_COLORED_REGIONS)) storeSetting(datasetname+"."+USE_MULTI_COLORED_REGIONS,true);
                if (!hasSetting(datasetname+"."+GRADIENT_FILL_REGIONS)) storeSetting(datasetname+"."+GRADIENT_FILL_REGIONS, 2);
            }
            else if (data instanceof RegionDataset) graphtype="Region";
            storeSetting(datasetname+"."+GRAPH_NAME,graphtype);
            return graphtype;
        }
    }
    
    
    /**
     * A convenience method to set the type of graph that should be used when drawing a datatrack
     * @param datasetname The name of the dataset
     * @param graphType The name of the graph type to use for this datatrack
     */
    public void setGraphType(String datasetname, String graphType) {
        if (datasetname==null) return;   
        storeSetting(datasetname+"."+GRAPH_NAME,graphType); // "type" used to be new Integer(graphType)
        notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.UPDATED,null);
        //redraw();
    }  
    
    /**
     * A convenience method to set the type of graph that should be used when drawing a datatrack
     * Unlike the "setGraphType" method, this sets the graph types but does not trigger an refresh of the GUI
     * @param datasetname The name of the dataset
     * @param graphType The name of the graph type to use for this datatrack
     */
    public void setGraphTypeSilently(String datasetname, String graphType) {
        if (datasetname==null) return;   
        storeSetting(datasetname+"."+GRAPH_NAME,graphType); // "type" used to be new Integer(graphType)
    }             
    
    /**
     * A convenience method to set the type of graph that should be used when drawing datatracks
     * @param datasetnames The names of the datasets
     * @param graphType The name of the graph type to use for these datatracks
     */
    public void setGraphType(String[] datasetnames, String graphType) {
         if (datasetnames==null) return;
         for (String datasetname:datasetnames) {        
            if (datasetname!=null) storeSetting(datasetname+"."+GRAPH_NAME, graphType); // graphType used to be new Integer(graphType)
         }
         notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.UPDATED,null);
         //redraw();
    }       
    
    /**
     * In older versions of MotifLab the graph type was a numeric property (integer).
     * This method tries to convert integer old values to new string names. 
     * @param graphType
     * @return 
     */
    public String getGraphTypeFromNumber(int graphType) {
        switch (graphType) { // these are hardcoded because they are based on an older deprecated version
            case BAR_GRAPH: return "Graph (filled)";
            case GRADIENT_GRAPH: return "Heatmap (1 color)";
            case REGION_GRAPH: return "Region";
            case REGION_MULTICOLOR_GRAPH: return "Region";
            case GRADIENT_REGION_GRAPH: return "Region (gradient)"; // this has never been used
            case DNA_GRAPH: return "DNA";
            case LINE_GRAPH: return "Graph (line)";
            case RAINBOW_HEATMAP_GRAPH: return "Heatmap (rainbow)";
            case TWO_COLOR_HEATMAP_GRAPH: return "Heatmap (2 colors)";
            case OUTLINED_GRAPH: return "Graph (outlined)";
            case GRADIENT_BAR_GRAPH: return "Graph (gradient)";
            case NUMERIC_DNA_GRAPH: return "DNA Graph"; 
        }
        return "";
    }
    
    /**
     * Determines whether this region track should be drawn with regions in different colors (TRUE) or just a single color (FALSE)
     * @param datasetname The name of the dataset
     */
    public boolean useMultiColoredRegions(String datasetname) {
        Object value=getSetting(datasetname+"."+USE_MULTI_COLORED_REGIONS); 
        if (value instanceof Boolean) return (Boolean)value;
        else return false;
    }  
    
    /**
     * Specifies whether regions in a region track should be drawn in different colors depending on type
     * @param datasetname The name of the dataset
     * @param multicolor TRUE if regions should be drawn in different colors depending on type and FALSE if just a single color should be used
     */
    public void setUseMultiColoredRegions(String datasetname, boolean multicolor) {
        storeSetting(datasetname+"."+USE_MULTI_COLORED_REGIONS,multicolor); // 
        redraw();        
    }       
    
    /**
     * Specifies whether regions in a region track should be drawn in different colors depending on type
     * @param datasetnames The names of the datasets
     * @param multicolor TRUE if regions should be drawn in different colors depending on type and FALSE if just a single color should be used
     */
    public void setUseMultiColoredRegions(String[] datasetnames, boolean multicolor) {
         if (datasetnames==null) return;
         for (String datasetname:datasetnames) {        
            if (datasetname!=null) storeSetting(datasetname+"."+USE_MULTI_COLORED_REGIONS,multicolor); // 
         }
         redraw();
    }     

    
    /**
     * A convenience method to decide the type of connector that should be used
     * when drawing nested regions
     * @param datasetname The name of the dataset
     * @return The connector type for this dataset
     */
    public String getConnectorType(String datasetname) {
        Object value=getSetting(datasetname+"."+CONNECTOR_TYPE);
        if (value instanceof String) return (String)value;
        else if (value instanceof Integer ) { // legacy setting
            switch((Integer)value) {
                case CONNECTOR_STRAIGHT_LINE:return "Straight Line";
                case CONNECTOR_ANGLED:return "Angled Line"; 
                case CONNECTOR_CURVED:return "Curved Line";
                case CONNECTOR_FILLED_CURVE:return "Ribbon";
                default: return "Bounding Box";
            }
        }
        else {
            String connector="Bounding Box";
            storeSetting(datasetname+"."+CONNECTOR_TYPE,connector);
            return connector;
        }
    }
    
    /**
     * A convenience method to set the type of connector that should be used when 
     * drawing nested regions
     * @param datasetname The name of the dataset
     * @param connectorType The type of connector to use for this datatrack
     */
    public void setConnectorType(String datasetname, String connectorType) {
        if (datasetname==null) return;     
        storeSetting(datasetname+"."+CONNECTOR_TYPE,connectorType);
        redraw();
    }
    
    /**
     * A convenience method to set the type of connector that should be used when 
     * drawing nested regions
     * @param datasetnames The names of the datasets
     * @param connectorType The type of connector to use for these datatracks
     */
    public void setConnectorType(String[] datasetnames, int connectorType) {
         if (datasetnames==null) return; 
         for (String datasetname:datasetnames) {        
            if (datasetname!=null) storeSetting(datasetname+"."+CONNECTOR_TYPE,connectorType);
         }
         redraw();
    }     
    

    /**
     * Determines whether the Region data track with the given name should be visualized in "expanded view" with overlapping
     * regions drawn beneath each other
     * @param datasetname The name of the dataset
     * @return
     */
    public boolean isExpanded(String datasetname) {
        Object value=getSetting(datasetname+"."+REGION_TRACK_EXPANDED);
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else return false;
    }
    
    /**
     * Sets whether the Region data track with the given name should be visualized in "expanded view" with overlapping
     * regions drawn beneath each other
     * @param datasetname
     * @param isExpanded
     */
    public void setExpanded(String datasetname, boolean isExpanded) {
         if (datasetname==null) return;
         storeSetting(datasetname+"."+REGION_TRACK_EXPANDED, isExpanded);
         notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.EXPANSION_CHANGED, null);
         redraw();
    }


    /**
     * A convenience method to set the "expanded" setting for several tracks at once
     * Region tracks drawn in "expanded view" will draw overlapping regions beneath each other
     * @param datasetnames
     * @param isExpanded
     */
    public void setExpanded(String[] datasetnames, boolean isExpanded) {
         if (datasetnames==null) return;         
         for (String datasetname:datasetnames) {
             if (datasetname!=null) storeSetting(datasetname+"."+REGION_TRACK_EXPANDED, isExpanded);
         }
         notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.EXPANSION_CHANGED, null);
         redraw();
    }

    /**
     * Determines whether nested regions should be visualized or not
     * @param datasetname The name of the dataset
     */
    public boolean visualizeNestedRegions(String datasetname) {      
        Object value=getSetting(datasetname+"."+VISUALIZE_NESTED_REGIONS);
        if (value!=null && value instanceof Boolean) { // for backwards compatibility
            return ((Boolean)value).booleanValue();
        }
        else return true;
    }

    /**
     * Sets whether nested regions should be visualized or not for the given dataset
     * @param datasetname
     * @param show
     */
    public void setVisualizeNestedRegions(String datasetname, boolean show) {
         if (datasetname==null) return; 
         storeSetting(datasetname+"."+VISUALIZE_NESTED_REGIONS, show);
         redraw();
    }


    /**
     * Determines whether the Regions in the Region track with the given name should be drawn
     * using a gradient fill rather than a solid color
     * @param datasetname The name of the dataset
     * @return 0=flat fill, 1=vertical gradient, 2=horizontal gradient
     */
    public int useGradientFill(String datasetname) {      
        Object value=getSetting(datasetname+"."+GRADIENT_FILL_REGIONS);         
        if (value!=null && value instanceof Integer) {          
            return ((Integer)value).intValue();
        } else if (value!=null && value instanceof Boolean) { // for backwards compatibility
            if (((Boolean)value).booleanValue()) return 1;
            else return 0;
        }
        else return 1;
    }

    /**
     * Sets whether the Regions in the Region track with the given name should be drawn
     * using a gradient fill rather than a solid color
     * @param datasetname
     * @param gradientFill 0=flat fill, 1=vertical gradient, 2=horizontal gradient
     */
    public void setGradientFillRegions(String datasetname, int gradientFill) {
         if (datasetname==null) return; 
         gradientFill=gradientFill%3;         // just in case   
         if (gradientFill<0) gradientFill=0;  // just in case 
         storeSetting(datasetname+"."+GRADIENT_FILL_REGIONS, gradientFill);
         redraw();
    }


    /**
     * Sets whether the Regions in the Region tracks with the given names should be drawn
     * using a gradient fill rather than a solid color
     * @param datasetnames
     * @param gradientFill 0=flat fill, 1=vertical gradient, 2=horizontal gradient
     */
    public void setGradientFillRegions(String[] datasetnames, int gradientFill) {
         if (datasetnames==null) return;        
         gradientFill=gradientFill%3; // just in case
         for (String datasetname:datasetnames) {
             if (datasetname!=null) storeSetting(datasetname+"."+GRADIENT_FILL_REGIONS, gradientFill);
         }
         redraw();
    }
    
    /**
     * Determines whether the Regions in the Region track with the given name should be drawn with borders
     * @param datasetname The name of the dataset
     * @return 0=no borders, 1=border in darker shade of the region color, 2=black border
     */
    public int drawRegionBorders(String datasetname) {      
        Object value=getSetting(datasetname+"."+DRAW_REGION_BORDERS);
        if (value!=null && value instanceof Integer) {
            return ((Integer)value).intValue();
        } return 0;
    }

    /**
     * Sets whether the Regions in the Region track with the given name should be drawn
     * with borders around them
     * @param datasetname
     * @param border 0=no borders, 1=border in darker shade of the region color, 2=black border
     */
    public void setDrawRegionBorders(String datasetname, int border) {
         if (datasetname==null) return; 
         border=border%3; // just in case. Only the values 0, 1 and 2 are permitted!
         storeSetting(datasetname+"."+DRAW_REGION_BORDERS, border);
         redraw();
    }


    /**
     * Sets whether the Regions in the Region tracks with the given names should be drawn
     * with borders around them
     * @param datasetnames
     * @param border 0=no borders, 1=border in darker shade of the region color, 2=black border
     */
    public void setDrawRegionBorders(String[] datasetnames, int border) {
         if (datasetnames==null) return;        
         border=border%3; // just in case. Only the values 0, 1 and 2 are permitted!
         for (String datasetname:datasetnames) {
             if (datasetname!=null) storeSetting(datasetname+"."+DRAW_REGION_BORDERS, border);
         }
         redraw();
    }    
    

     /**
     * Determines whether Regions shown in closeup expanded view should include the sequence logo of motifs
     * @param datasetname The name of the dataset
     * @return
     */
    public boolean showMotifLogoInCloseupView() {
        Object value=getPersistentSetting(SHOW_MOTIF_LOGO_IN_CLOSEUP_VIEW);
        if (value!=null) return ((Boolean)value).booleanValue();
        else {
            Boolean defaultValue=Boolean.TRUE;
            storePersistentSetting(SHOW_MOTIF_LOGO_IN_CLOSEUP_VIEW,defaultValue);
            return defaultValue.booleanValue();
        }
    }

    /**
     * Sets whether Regions shown in closeup expanded view should include the sequence logo of motifs
     * @param show
     */
    public void setShowMotifLogoInCloseupView(boolean show) {
         storePersistentSetting(SHOW_MOTIF_LOGO_IN_CLOSEUP_VIEW, show);
         notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.EXPANSION_CHANGED, null);
         redraw();
    }

    /**
     * Determines whether "close up view" of expanded region datasets is enabled
     * @param show
     */
    public boolean isCloseupViewEnabled() {
        Object value=getPersistentSetting(CLOSEUP_VIEW_ENABLED);
        if (value!=null) return ((Boolean)value).booleanValue();
        else {
            Boolean defaultValue=Boolean.TRUE;
            storePersistentSetting(CLOSEUP_VIEW_ENABLED,defaultValue);
            return defaultValue.booleanValue();
        }
    }

    /**
     * Sets whether "close up view" of expanded region datasets should be enabled
     * @param show
     */
    public void setCloseupViewEnabled(boolean enabled) {
         storePersistentSetting(CLOSEUP_VIEW_ENABLED, enabled);
         notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.EXPANSION_CHANGED, null);
         redraw();
    }

 
    

    /**
     * A convenience method to decide whether or not the specified datatrack should be drawn at all
     * @param datasetname The name of the feature dataset
     * @return a boolean value which is true if the track should be displayed and false if it should be hidden
     */
    public boolean isTrackVisible(String datasetname) {
        Object value=getSetting(datasetname+"."+TRACK_VISIBLE);
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else {
            return true;       
        }
    }
    
    /**
     * A convenience method to set the visibility for a datatrack or full
     * @param datasetname The name of the dataset
     * @param boolean True if the datatrack should be shown and false if it should be hidden
     */
    public void setTrackVisible(String datasetname, boolean show) {
        if (datasetname==null) return;         
        storeSetting(datasetname+"."+TRACK_VISIBLE, show);
        rearrangeDataTracks();
        redraw();
    }

    /**
     * A convenience method to set the visibility for several datatracks at once
     * @param datasetnames An array containing the names of the datasets
     * @param boolean True if the datatracks should be shown and false if they should be hidden
     */
    public void setTrackVisible(String[] datasetnames, boolean show) {
        if (datasetnames==null) return;         
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+TRACK_VISIBLE, show);
        }
        rearrangeDataTracks();
        redraw();
    }

    /**
     * A convenience method to decide whether or not the specified sequence should be drawn at all
     * @param sequencename The name of the sequence
     * @return a boolean value which is true if the sequence should be displayed and false if it should be hidden
     */
    public boolean isSequenceVisible(String sequencename) {
        Object value=getSetting(sequencename+"."+SEQUENCE_VISIBLE);
        if (value!=null) return ((Boolean)value).booleanValue();
        else {
            storeSetting(sequencename+"."+SEQUENCE_VISIBLE, true);
            return true;
        }
    }

    /**
     * A convenience method to set the visibility for sequence
     * @param sequencename The name of the sequence
     * @param boolean True if the sequence should be shown and false if it should be hidden
     */
    public void setSequenceVisible(String sequencename, boolean show) {
        if (sequencename==null) return;    
        storeSetting(sequencename+"."+SEQUENCE_VISIBLE, show);
        notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.VISIBILITY_CHANGED, null);
        redraw();
    }
    
    /**
     * A convenience method to set the visibility for sequence
     * @param sequencename The name of the sequence
     * @param boolean True if the sequence should be shown and false if it should be hidden
     * @param update if TRUE, layout listeners will be updated and the GUI redrawn.
     *               if FALSE, only the new setting will be stored, but the GUI will not be updated
     *               (it has to be updated manually with a call to notifySequenceVisibilityUpdated() )
     */
    public void setSequenceVisible(String sequencename, boolean show, boolean update) {
        if (sequencename==null) return;    
        storeSetting(sequencename+"."+SEQUENCE_VISIBLE, show);
        if (update) {
            notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.VISIBILITY_CHANGED, null);
            redraw();
        }
    }    
    
    public void notifySequenceVisibilityUpdated() {
        notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.VISIBILITY_CHANGED, null);
        redraw();       
    }

    /**
     * A convenience method to set the visibility for several sequences at once
     * @param sequencenames An array containing the names of the sequences
     * @param boolean True if the sequences should be shown and false if they should be hidden
     */
    public void setSequenceVisible(String[] sequencenames, boolean show) {
        if (sequencenames==null) return;
        for (String sequencename:sequencenames) {
            if (sequencename!=null) storeSetting(sequencename+"."+SEQUENCE_VISIBLE, show);         
        }
        notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.VISIBILITY_CHANGED, null);
        redraw();
    }

    
    /** Returns the color to use to represent upregulation (or 'above baseline' values) */
    public Color getExpressionColorUpregulated() {
        Object value=getPersistentSetting(EXPRESSION_COLORGRADIENT_UPREGULATED);
        if (value!=null) {
            return ((ColorGradient)value).getForegroundColor();
        } else {
            setExpressionColorUpregulated(Color.RED);
            return Color.RED;
        }
    }
    
    /** Returns the color to use to represent downregulation (or 'below baseline' values) */
    public Color getExpressionColorDownregulated() {
        Object value=getPersistentSetting(EXPRESSION_COLORGRADIENT_DOWNREGULATED);
        if (value!=null) {
            return ((ColorGradient)value).getForegroundColor();
        } else {
            setExpressionColorUpregulated(Color.GREEN);
            return Color.GREEN;
        }
    }  
    
    /** Returns the color to use for background in expression profiles (or 'no change' values) */
    public Color getExpressionColorBackground() {
        Object value=getPersistentSetting(EXPRESSION_COLORGRADIENT_UPREGULATED);
        if (value!=null) {
            return ((ColorGradient)value).getBackgroundColor();
        } else {
            return Color.BLACK; // default. Do not update anything else
        }
    }  
    
    /** Returns the colorgradient to use to represent upregulation. 
     *  The first color in this gradient will be the background (no change) color and the last
     *  will be the color for maximum upregulation
     */
    public ColorGradient getExpressionColorUpregulatedGradient() {
        Object value=getPersistentSetting(EXPRESSION_COLORGRADIENT_UPREGULATED);
        if (value!=null) {
            return ((ColorGradient)value);
        } else {
            setExpressionColorUpregulated(Color.RED);
            return (ColorGradient)getPersistentSetting(EXPRESSION_COLORGRADIENT_UPREGULATED);
        }
    }   
    /** Returns the colorgradient to use to represent downregulation. 
     *  The first color in this gradient will be the background (no change) color and the last
     *  will be the color for maximum downregulation
     */
    public ColorGradient getExpressionColorDownregulatedGradient() {
        Object value=getPersistentSetting(EXPRESSION_COLORGRADIENT_DOWNREGULATED);
        if (value!=null) {
            return ((ColorGradient)value);
        } else {
            setExpressionColorDownregulated(Color.GREEN);
            return (ColorGradient)getPersistentSetting(EXPRESSION_COLORGRADIENT_DOWNREGULATED);
        }
    }     
    
    /** Sets the color to use to represent upregulation (or 'above baseline' values). Default is RED */
    public void setExpressionColorUpregulated(Color color) {
        Color bg=getExpressionColorBackground();
        ColorGradient upgradient=new ColorGradient(color, bg, 255); // 255+1 colors
        storePersistentSetting(EXPRESSION_COLORGRADIENT_UPREGULATED, upgradient);
    }
    /** Sets the color to use to represent downregulation (or 'below baseline' values). Default is GREEN */
    public void setExpressionColorDownregulated(Color color) {
        Color bg=getExpressionColorBackground();
        ColorGradient downgradient=new ColorGradient(color, bg, 255); // 255+1 colors
        storePersistentSetting(EXPRESSION_COLORGRADIENT_DOWNREGULATED, downgradient);
    }
    /** Sets the color to use for background in expression profiles (or 'no change' values). Default is BLACK */
    public void setExpressionColorBackground(Color color) {
        Color up=getExpressionColorUpregulated();
        Color down=getExpressionColorDownregulated();        
        ColorGradient upgradient=new ColorGradient(up, color, 255); // 255+1 colors
        ColorGradient downgradient=new ColorGradient(down, color, 255); // 255+1 colors
        storePersistentSetting(EXPRESSION_COLORGRADIENT_UPREGULATED, upgradient);
        storePersistentSetting(EXPRESSION_COLORGRADIENT_DOWNREGULATED, downgradient);        
    }    
    /** Sets the colors to use to represent up and downregulation */
    public void setExpressionColors(Color up, Color down, Color background) {      
        ColorGradient upgradient=new ColorGradient(up, background, 255); // 255+1 colors
        ColorGradient downgradient=new ColorGradient(down, background, 255); // 255+1 colors
        storePersistentSetting(EXPRESSION_COLORGRADIENT_UPREGULATED, upgradient);
        storePersistentSetting(EXPRESSION_COLORGRADIENT_DOWNREGULATED, downgradient);        
    }        

    
    /**
     * Specifies that a subset of a sequence is marked as selected (this could for instance be a result of the user
     * selecting a region with the selection tool or zoom tool)
     * @param sequenceName The name of the sequence (format="datasetName.sequenceName")
     * @param start The genomic coordinate of the selection start
     * @param end The genomic coordinate of the selection end
     * @param clear If this is TRUE then all currently registered selection windows are cleared before registering the new selection
     * @param mergeoverlapping If this is TRUE then the current selection windows will be updated so
     *        that no two (or more) windows overlap (or are directly back-to-back) but rather are represented 
     *        by a single window that spans all the previous windows (and the newly submitted)
     */
    public void addSelectionWindow(String sequenceName, int start, int end, boolean clear, boolean mergeoverlapping) {
        if (clear) selections.clear();
        if (mergeoverlapping && !clear) {
            // remove windows that overlap and update bounds for newly submitted window
            Iterator itr = selections.iterator();
            while (itr.hasNext()) {
              SelectionWindow window = (SelectionWindow) itr.next();
              if (!window.sequenceName.equals(sequenceName)) continue;
              if (!(window.end<start-1 || end<window.start-1)) { // not separate
                itr.remove();
                if (window.start<start) start=window.start;
                if (window.end>end) end=window.end;                    
              }
            }                
        } 
        selections.add(new SelectionWindow(sequenceName,start,end));            
        redraw(); //
    }
    
    public void addSelectionWindow(String sequenceName, int start, int end) {
        selections.add(new SelectionWindow(sequenceName,start,end));            
    }    

    /**
     * Subtracts a selected window from the current selection.
     * If the window overlaps any currently selected segments then these will be cropped or split up
     * as necessary so that the specified segment will no longer be selected.
     * @param sequenceName The name of the sequence (format="datasetName.sequenceName")
     * @param start The genomic coordinate of the selection start
     * @param end The genomic coordinate of the selection end
      */
    public void subtractSelectionWindow(String sequenceName, int start, int end) {
        if (selections.isEmpty()) return;
        ArrayList<SelectionWindow> newselections=new ArrayList<SelectionWindow>();
        Iterator itr = selections.iterator();
        while (itr.hasNext()) {
              SelectionWindow window = (SelectionWindow) itr.next();
              if (!window.sequenceName.equals(sequenceName)) continue;
              if (start<=window.start && end>=window.end) { // the old window is completely covered by new selection: remove it
                  itr.remove();
              }
              else if (start>window.start && end<window.end) { // new selection lies within this window with at least 1 bp margin on either side
                int oldend=window.end;
                window.end=start-1; // update end of old window
                newselections.add(new SelectionWindow(sequenceName,end+1,oldend));
              } else if (start<=window.start && end>=window.start) { // overlaps on left end: crop old window
                window.start=end+1;
              } else if (end>=window.end && start<=window.end) { // overlaps on right end: crop old window
                window.end=start-1;
              }
        }
        if (!newselections.isEmpty()) selections.addAll(newselections);
        redraw(); //
    }
  
    /** Removes all sequence selection windows */
    public void clearSelectionWindows() {
        selections.clear();
        redraw(); //
    }
    
    /** Removes all sequence selection windows in the given sequence */
    public void clearSelectionWindows(String sequenceName) {
        if (selections.isEmpty()) return;
        Iterator itr = selections.iterator();
        while (itr.hasNext()) {
             SelectionWindow window = (SelectionWindow) itr.next();
             if (window.sequenceName.equals(sequenceName)) itr.remove();
        }
    }  
      
    
    /**
     * Returns current set of sequence selections windows (in arbitrary order)
     * or NULL if no selection windows are registered
     * Note that these might not be "live" updated while a user drags to select,
     * but are usually updated immediately after the user has finished with a selection
     */
    public ArrayList<SelectionWindow> getSelectionWindows() {
        if (selections.isEmpty()) return null;
        return selections;
    }
    
    /**
     * Returns current set of sequence selections windows for the given sequence
     * or NULL if no selection windows are registered
     * Note that these might not be "live" updated while a user drags to select,
     * but are usually updated immediately after the user has finished with a selection
     */
    public ArrayList<SelectionWindow> getSelectionWindows(String sequenceName) {
        ArrayList<SelectionWindow> sequenceSelections=new ArrayList<SelectionWindow>();
        if (selections.isEmpty()) return sequenceSelections;
        Iterator itr = selections.iterator();
        while (itr.hasNext()) {
              SelectionWindow window = (SelectionWindow) itr.next();
              if (window.sequenceName.equals(sequenceName)) sequenceSelections.add(window);
        }
        return sequenceSelections;
    }    
    
    /**
     * Returns the number of current active selection windows
     */
    public int getSelectionWindowCount() {
         return selections.size();
    }
    
    /**
     * Returns TRUE if the specified sequence has an active selection window
     */
    public boolean hasSelectionWindow(String sequenceName) {
         for (SelectionWindow window:selections) {
            if (window.sequenceName.equals(sequenceName)) return true;
         }
         return false;
    }

    /**
     * Returns the total number of bases currently selected in the given sequence.
     * The number is calculated from the current selection windows plus an additional
     * windows that can either be added or subtracted from the current selections
     * @param sequenceName
     * @param start
     * @param end
     * @param subtract
     * @param sequenceName
     * @param clear
     * @param mergeoverlapping
     * @return 
     */
    public int countSelectedBasesInSequence(String sequenceName, int start, int end, boolean subtract, boolean clear, boolean mergeoverlapping) {
        if (subtract) {
            if (selections.isEmpty()) return 0;
            int count=0;
            Iterator itr = selections.iterator();
            while (itr.hasNext()) {
                  SelectionWindow window = (SelectionWindow) itr.next();
                  if (!window.sequenceName.equals(sequenceName)) continue;
                  if (start<=window.start && end>=window.end) continue;  // the old window is completely covered by new subtraction selection: don't count it
                  if (start>window.start && end<window.end) { // new selection lies within this window with at least 1 bp margin on either side
                    int oldsize=window.end-window.start+1;
                    int subtractsize=end-start+1;
                    count+=(oldsize-subtractsize);
                  } else if (start<=window.start && end>=window.start) { // overlaps on left end: crop old window
                    int newsize=window.end-(end+1)+1;
                    count+=newsize;
                  } else if (end>=window.end && start<=window.end) { // overlaps on right end: crop old window
                    int newsize=(start-1)-window.start+1;
                    count+=newsize;                    
                  } else count+=(window.end-window.start+1); // no overlap with this window
            }
            return count;
        } else {
            if (clear) return (end-start+1); // just return new selection
            int count=0;        
            int overlap=0;
            if (mergeoverlapping && !clear) {
                Iterator itr = selections.iterator();
                while (itr.hasNext()) {
                   SelectionWindow window = (SelectionWindow) itr.next();
                   if (!window.sequenceName.equals(sequenceName)) continue;
                   int thisoverlap=0;                   
                   count+=(window.end-window.start+1); // count full window size
                   if (start<=window.start && end>=window.end) thisoverlap=(window.end-window.start+1);  // the old window is completely covered by new selection
                   else if (start>window.start && end<window.end) { // new selection lies within this window with at least 1 bp margin on either side
                      thisoverlap=(end-start+1);
                   } else if (start<=window.start && end>=window.start) { // overlaps on left end
                       thisoverlap=(end-window.start+1); // updates end of old selection window  
                   } else if (end>=window.end && start<=window.end) { // overlaps on right end
                       thisoverlap=(window.end-start+1); // updates start of old selection window              
                   }
                   overlap+=thisoverlap;
                 }                            
            }
            count+=(end-start+1); // add current selection window
            count-=overlap; // but remove overlap
            return count;            
        }
    }
    
    /**
     * Sets the name of the sequence which has a currently selected subregion
     * Since only one sequence can have a current subregion selection at the time, this will "clear" any selections in other sequences
     * @param sequenceID The name of the sequence (format="datasetName.sequenceName")
     */
    public void setSequenceWithSelection(String sequenceID) {
        storeSetting(SEQUENCE_WITH_SELECTION,sequenceID);
        redraw(); //
    }
  
    
    /**
     * Returns the name of the sequence which has a currently selected subregion
     * (selected by pressing the button somewhere inside a sequence and dragging to mark a region)
     * @return The name of the sequence (format="datasetName.sequenceName") which currently has a selected region, or null 
     */
    public String getSequenceWithSelection() {
        return (String)getSetting(SEQUENCE_WITH_SELECTION);
    }    
      
    /** sets or clears the 'mark' for the selected sequence */
    @SuppressWarnings("unchecked")
    public void setSequenceMarked(String sequenceName, boolean marked) {
        if (sequenceName==null) return;
        if (!hasSetting(MARKED_SEQUENCES)) storeSetting(MARKED_SEQUENCES, new HashSet<String>());
        HashSet<String> markedSequences=(HashSet<String>)getSetting(MARKED_SEQUENCES);
        if (marked) markedSequences.add(sequenceName);
        else markedSequences.remove(sequenceName);     
    }
    
    /** sets or clears the 'mark' for the selected sequences
     *  The marks on other sequences will remain as is
     */
    @SuppressWarnings("unchecked")
    public void setSequenceMarked(String[] sequenceNames, boolean marked) {
        if (sequenceNames==null) return;
        if (!hasSetting(MARKED_SEQUENCES)) storeSetting(MARKED_SEQUENCES, new HashSet<String>());
        HashSet<String> markedSequences=(HashSet<String>)getSetting(MARKED_SEQUENCES);
        HashSet<String> set=new HashSet<String>(sequenceNames.length);
        set.addAll(Arrays.asList(sequenceNames));            
        if (marked) markedSequences.addAll(set);
        else markedSequences.removeAll(set);     
    }    
    
    /** Returns true if the user has selected this sequence to be 'marked' in the GUI */
    @SuppressWarnings("unchecked")
    public boolean isSequenceMarked(String sequenceName) {
        if (!hasSetting(MARKED_SEQUENCES)) return false;
        HashSet<String> markedSequences=(HashSet<String>)getSetting(MARKED_SEQUENCES);
        return markedSequences.contains(sequenceName);  
    }
    
    /** Clear all 'marks' from all sequences */
    @SuppressWarnings("unchecked")
    public void clearSequenceMarks() {
        if (!hasSetting(MARKED_SEQUENCES)) return;
        HashSet<String> markedSequences=(HashSet<String>)getSetting(MARKED_SEQUENCES);
        markedSequences.clear();
    }    
    
    /** Returns all the names of all sequences that are currently 'marked' */
    @SuppressWarnings("unchecked")
    public HashSet<String> getMarkedSequences() {
        if (!hasSetting(MARKED_SEQUENCES)) return new HashSet<String>(0);
        else return (HashSet<String>)getSetting(MARKED_SEQUENCES);      
    }

    /** Sets the sequences in the set as 'marked' (and those not in the set as unmarked) 
     *  This will replace all current marks!
     */
    @SuppressWarnings("unchecked")
    public void setMarkedSequences(HashSet<String> sequences) {
        storeSetting(MARKED_SEQUENCES, sequences);    
    }   
        
    /**
     * Sets the specified sequences as marked, but also keeps all currently
     * marked sequences
     * @param sequences 
     */
    @SuppressWarnings("unchecked")
    public void addMarkedSequences(HashSet<String> sequences) {
        if (!hasSetting(MARKED_SEQUENCES)) setMarkedSequences(sequences);
        else ((HashSet<String>)getSetting(MARKED_SEQUENCES)).addAll(sequences);   
    } 
     
    @SuppressWarnings("unchecked")
    public void addMarkedSequences(String[] sequences) {
        HashSet<String> added=new HashSet<String>(sequences.length);
        added.addAll(Arrays.asList(sequences));
        addMarkedSequences(added);
    }  
    
     /**
     * Removes the 'mark' from the specified sequences
     * @param sequences 
     */   
    @SuppressWarnings("unchecked")
    public void clearMarkedSequences(HashSet<String> sequences) {
        if (!hasSetting(MARKED_SEQUENCES)) return;   
        ((HashSet<String>)getSetting(MARKED_SEQUENCES)).removeAll(sequences);   
    }   
    
     /**
     * Removes the 'mark' from the specified sequences
     * @param sequences 
     */   
    @SuppressWarnings("unchecked")
    public void clearMarkedSequences(String[] sequences) {
        HashSet<String> set=new HashSet<String>(sequences.length);
        set.addAll(Arrays.asList(sequences));        
        if (!hasSetting(MARKED_SEQUENCES)) return;   
        ((HashSet<String>)getSetting(MARKED_SEQUENCES)).removeAll(set);   
    }      
    
    
    /**
     * This method returns a list of the datatracks that should be visualized in the correct order
     * This list is based on the order of datasets in the "features" panel. 
     * Note that "hidden" datatracks are not included in this list! 
     * @return a list containing the datasets in order
     */
    public ArrayList<String> getDatatrackOrder() {
        if (datatrackorder==null) rearrangeDataTracks();
        return datatrackorder;
    }
    
    
    /**
     * Recalculates and stores the order of visible "feature" datatracks
     * This method should be called whenever the order or visibility of datatracks
     * is updated (such as when the user rearranges the order of datasets in the
     * "features" panel or selects hide or show on a datatrack).
     */
    public void rearrangeDataTracks() {
        if (gui==null) {rearrangeDataTracksForNonVisualClient();return;}
        FeaturesPanelListModel listmodel=gui.getFeaturesPanelListModel();
        if (listmodel==null) return;
        int size=listmodel.getSize();
        ArrayList<String> list=new ArrayList<String>(size);
        for (int i=0;i<size;i++) {
            FeatureDataset dataset=(FeatureDataset)listmodel.elementAt(i);
            if (dataset==null) continue; // this happens?!
            String trackname=dataset.getName();
            //  if (isTrackVisible(trackname)) list.add(trackname);
            if (isTrackVisible(trackname) && !isGroupedTrack(trackname)) list.add(trackname);
        }
        datatrackorder=list;       
        //gui.debugMessage("rearrangeDataTracks");
        notifyListenersOfTrackOrderRearrangement(VisualizationSettingsListener.UPDATED,null);
    }
    
    /** This updates the datatracks order for non-visual clients based on the masterlist */
    private void rearrangeDataTracksForNonVisualClient() {
        int size=datatrackorderMasterList.size();
        ArrayList<String> list=new ArrayList<String>(size);
        for (int i=0;i<size;i++) {
            String trackname=datatrackorderMasterList.get(i);
            // if (isTrackVisible(trackname)) list.add(trackname);
            if (isTrackVisible(trackname) && !isGroupedTrack(trackname)) list.add(trackname);            
        }
        datatrackorder=list;           
    }
    
    /**
     * Returns TRUE if the height of the given feature track is determined by content and
     * can vary between sequences. Each sequence visualizer (and track) is responsible
     * for updating the height-setting for its variable height tracks so that they always
     * have the correct value
     * @return
     */
    public boolean hasVariableTrackHeight(String datasetname) {
        Object value=getSetting(datasetname+"."+VARIABLE_HEIGHT_TRACK);
        if (value!=null) return ((Boolean)value).booleanValue();
        else return false;
    }

    /**
     * Sets the "variable track height" property for a dataset (feature)
     */
    public void setVariableTrackHeight(String datasetname, boolean value) {
        if (value==true) storeSetting(datasetname+"."+VARIABLE_HEIGHT_TRACK, Boolean.TRUE);
        else clearSetting(datasetname+"."+VARIABLE_HEIGHT_TRACK);
        // This should not notify any listeners or trigger updates/redraws! (That would require rewriting other parts of the code also)
    }

    
    /**
     * A convenience method to get the width of the window for dataset visualization (excluding borders)
     * @return The width of the sequence window
     */
    public int getSequenceWindowSize() {
        Object value=getPersistentSetting(SEQUENCE_WINDOW_SIZE);
        if (value!=null) {
            return ((Integer)value).intValue();
        } else {
            storePersistentSetting(SEQUENCE_WINDOW_SIZE,new Integer(DEFAULT_SEQUENCE_WINDOW_SIZE));
            return DEFAULT_SEQUENCE_WINDOW_SIZE;
        }
    }
    
    /**
     * A convenience method to set the width of the window for dataset visualization
     * @param width The new width of the sequence window
     */
    public void setSequenceWindowSize(int width) {
        int oldWidth=getSequenceWindowSize();
        storePersistentSetting(SEQUENCE_WINDOW_SIZE,new Integer(width));
        //rearrangeDataTracks();
        notifyListenersOfSequenceWindowSizeChange(width,oldWidth);
        redraw();
    }
    
    
    /**
     * This method returns the position of the start (lowest coordinate) of the view port in genomic coordinates. 
     * Note that the start of the viewport can (possibly) lie outside the sequence itself
     * (The datatrack visualizer should decide how to handle this)
     * @param sequenceName The name of the sequence itself 
     * @return The start of the viewport (genomic coordinates, direct strand)
     */
    public int getSequenceViewPortStart(String sequenceName) {
        Object value=getSetting(sequenceName+"."+VIEWPORT_START);
        if (value!=null) {
            return ((Integer)value).intValue();      
        } else {
            if (client==null) return -1; // this should function as a flag
            Sequence sequence=(Sequence)client.getEngine().getDataItem(sequenceName);
            if (sequence==null) return -1; // this should function as a flag
            int start=sequence.getRegionStart();
            storeSetting(sequenceName+"."+VIEWPORT_START, start);
            return start;
        }
    }
    
    /**
     * This method sets the position of the start (lowest coordinate) of the view port in genomic coordinates
     * Unless the sequence in question is "constrained", the start of the viewport can be set outside the 
     * region of the sequence itself. However, attempting to do this on a constrained sequence will result in
     * automatic repositioning according to the current settings.
     * Note also that a call to this function will not result in automatic update/redraw of sequences (for performance reasons)
     * 
     * @param sequenceName The name of the sequence itself 
     * @param start A position specifying the start of the viewport in genomic coordinates (direct strand)
     * */
    public void setSequenceViewPortStart(String sequenceName,int start) {
        if (isSequenceConstrained(sequenceName)) {
            double scale=getScale(sequenceName);
            Sequence sequence=(Sequence)getEngine().getDataItem(sequenceName);
            if (sequence==null) return; // this has happened :S
            int sequenceSize=sequence.getSize();  
            int sequenceStart=sequence.getRegionStart();                  
            int sequenceEnd=sequence.getRegionEnd();  
            int end=start+(int)Math.ceil(getSequenceWindowSize()/scale)-1; // new end if VPstart is set to start
            boolean canFitInWindow=(sequenceSize<=getSequenceWindowSize()/scale);
            if (canFitInWindow) {
                if (start>sequenceStart) start=sequenceStart;
                else if (end<sequenceEnd) start=sequenceEnd-(int)Math.ceil(getSequenceWindowSize()/scale)+1;
            } else { // sequence is at current scale too large to fit within window
                if (start<sequenceStart) start=sequenceStart;
                else if (end>sequenceEnd) start=sequenceEnd-(int)Math.ceil(getSequenceWindowSize()/scale)+1;
            }
        }
        storeSetting(sequenceName+"."+VIEWPORT_START,new Integer(start));
        //redraw(); // calling redraw will update all sequences (which is a bit heavy when only one is changed)
    }
 
    /**
     * This method sets the position of the end (highest coordinate) of the view port in genomic coordinates
     * Note that the start of the viewport can be set outside the region of the sequence itself
     * (The datatrack visualizer should decide how to handle this)
     * Note also that a call to this function will not results in automatic update/redraw of sequences
     * (for performance reasons)
     * 
     * @param sequenceName The name of the sequence itself 
     * @param end A position specifying the end of the viewport in genomic coordinates (direct strand)
     * */
    public void setSequenceViewPortEnd(String sequenceName,int end) {
        // We do not set VP-end directly, since the VP-end is always calculated on the fly with respect to start and scale.
        // So instead we calculate and set a new VP-start
        int start=end-(int)Math.ceil(getSequenceWindowSize()/getScale(sequenceName))+1;                
        storeSetting(sequenceName+"."+VIEWPORT_START,new Integer(start));
        //redraw(); // calling redraw will update all sequences (which is a bit heavy when only one is changed)
    }
 
    
    
    
    
    /**
     * This method returns the position of the end (highest coordinate) of the view port in genomic coordinates
     * Note that the start of the viewport can (possibly) lie outside the sequence itself
     * (The datatrack visualizer should decide how to handle this)
     * @param sequenceName The name of the sequence itself 
     * @return The start of the viewport (genomic coordinate)
     */
    public int getSequenceViewPortEnd(String sequenceName) {
        // this value is inferred from viewPortStart in combination with scale and windowSize
        // note that the casting to (double) below is quite necessary, or else you will have problems once in a blue moon
        return getSequenceViewPortStart(sequenceName)+(int)Math.ceil((double)getSequenceWindowSize()/(double)getScale(sequenceName))-1;
    }
    

    
    /**
     * This method sets a new viewport to use for visualization of the given sequence (in genomic coordinates)
     * Note that the range of the viewport can be set outside the region of the underlying sequence
     * ViewPort should be specified relative to direct strand so that "start" is less than or equal to "end"
     * 
     * @param sequenceName The name of the sequence itself 
     * @param start A position specifying the start of the viewport (in genomic coordinates)
     * @param end A position specifying the end of the viewport (in genomic coordinates)
     * */
    public void setSequenceViewPort(String sequenceName,int start, int end) {
        storeSetting(sequenceName+"."+VIEWPORT_START,new Integer(start));
        // you must recalculate zoomFactor and set that also!
        double width=end-start+1;
        double scale=getSequenceWindowSize()/width;
        setSequenceZoomLevel(sequenceName,scale*100.0); // this will trigger redraw also!
    }
    
    
    /**
     * This method sets a new viewport to use for visualization of a sequence so that
     * the given genomic coordinate aligns with the given screen position (=coordinate within a component)
     * The zoom-level will be kept as is, only the translation will be affected.
     * 
     * @param sequenceName The name of the sequence itself 
     * @param genomic A genomic coordinate
     * @param screen A screen position (offset within component)
     * @param alignment If alignment==ALIGNMENT_LEFT  the viewport will be set so that the genomic coordinate is the first within the screen pixel (eg. for very small scales)
     *                  If alignment==ALIGNMENT_RIGHT the viewport will be set so that the genomic coordinate is the last within the screen pixel (eg. for very small scales)
     *                  else the viewport will be set so that the genomic coordinate is in the middle of the pixel (round to the left)
     * */
    public void alignSequenceViewPort(String sequenceName,int genomic, int screen, String alignment) {
        int viewPortStart=getSequenceViewPortStart(sequenceName);
        double scale=getScale(sequenceName);
        int[] genomicScreen=getGenomicCoordinateFromScreenOffset(sequenceName, screen);
        int currentlyUnderScreenPixel;
        if (alignment.equals(ALIGNMENT_LEFT)) currentlyUnderScreenPixel=genomicScreen[0];
        else if (alignment.equals(ALIGNMENT_RIGHT)) currentlyUnderScreenPixel=genomicScreen[1];
        else if (alignment.equals(ALIGNMENT_TSS)) currentlyUnderScreenPixel=genomicScreen[0];
        else currentlyUnderScreenPixel=(int)((genomicScreen[0]+genomicScreen[1])/2);
        int screendiff=genomic-currentlyUnderScreenPixel; // number of genomic coordinates from start of window to screen pixel
        double genomicdiff=screendiff;//scale;
        int newVPstart=(int)(viewPortStart+genomicdiff);
        //gui.debugMessage(sequenceName+": genomic="+genomic+"  screen="+screen+"  scale="+scale+"  currentlyUnderScreenPixel="+currentlyUnderScreenPixel+" ["+genomicScreen[0]+","+genomicScreen[1]+"]   screendiff="+screendiff+"  genomicdiff="+genomicdiff+"  oldVPstart="+viewPortStart+"  newVPstart="+newVPstart+" alignment="+alignment);
        //storeSetting(sequenceName+"."+VIEWPORT_START,new Integer(newVPstart));
        setSequenceViewPortStart(sequenceName,newVPstart);
        //redraw(); // calling redraw will update all sequences (which is a bit heavy when only one is changed)
    }
    
    
    
    /**
     * Returns the zoom level (percentage value) for the specified sequence.
     * If this sequence has han individually specified zoom level, this value will be returned.
     * If not, the global zoom level will be used instead. 
     * A zoom percentage of 100 means that there is a 1-to-1 correspondence between bases and pixels 
     * (i.e. one base per pixel). A zoom value of 200 means one base takes up 2 pixels, while zoom=50 
     * means that the values of 2 neighboring bases should be represented by 1 pixel.
     * @param sequenceName The name of the sequence itself 
     * @return The zoom percentage 
     */
    public double getSequenceZoomLevel(String sequenceName) {
        Object value=getSetting(sequenceName+"."+ZOOM_LEVEL);
        if (value!=null) {
            return ((Double)value).doubleValue();
        } else {
            return getGlobalZoomLevel();
        }
    }
    
    /**
     * Sets the zoom level (percentage value) for the specified sequence.
     * A zoom percentage of 100 means that there is a 1-to-1 correspondence between bases and pixels 
     * (i.e. one base per pixel). A zoom value of 200 means one base takes up 2 pixels, while zoom=50 
     * means that the values of 2 neighboring bases should be represented by 1 pixel.
     * 
     * @param sequenceName The name of the sequence itself 
     * @param zoompercentage
     */
    public void setSequenceZoomLevel(String sequenceName, double zoompercentage) {
        if (sequenceName==null) return;         
        double oldvalue=getSequenceZoomLevel(sequenceName);
        storeSetting(sequenceName+"."+ZOOM_LEVEL,new Double(zoompercentage));
        if ((oldvalue>=CLOSE_UP_ZOOM_LIMIT && zoompercentage<CLOSE_UP_ZOOM_LIMIT) || (oldvalue<CLOSE_UP_ZOOM_LIMIT && zoompercentage>=CLOSE_UP_ZOOM_LIMIT)) notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
}

    /**
     * Sets the zoom level (percentage value) for the specified sequence to the next zoom level in 
     * the preset list which is greater than the current zoom level. If the current alignment setting
     * for this sequence is something other than NONE the ViewPort will be repositioned according to
     * alignment settings
     * 
     * @param sequenceName The name of the sequence itself
     * @param useFineScale If TRUE the zoom-steps will be chosen based on a finer scale
     */
    public void zoomInOnSequence(String sequenceName, boolean useFineScale) {
        double zoomLevel=getSequenceZoomLevel(sequenceName);
        int index;
        double[] choices=(useFineScale)?zoomChoicesFiner:zoomChoices;
        for (index=0;index<choices.length-1;index++) {
           if (choices[index]>zoomLevel) {break;}
        }
        zoomLevel=choices[index]; // new zoom level
        zoomToLevel(sequenceName,zoomLevel,true);
    }

    
     /**
     * Sets the zoom level (percentage value) for the specified sequence to the next zoom level in 
     * the preset list which is smaller than the current zoom level. If the current alignment setting
     * for this sequence is something other than NONE the ViewPort will be repositioned according to
     * alignment settings
     * 
     * @param sequenceName The name of the sequence itself
     * @param useFineScale If TRUE the zoom-steps will be chosen based on a finer scale
     */
    public void zoomOutOnSequence(String sequenceName, boolean useFineScale) {
        double zoomLevel=getSequenceZoomLevel(sequenceName);
        int index;
        double[] choices=(useFineScale)?zoomChoicesFiner:zoomChoices;
        for (index=choices.length-1;index>0;index--) {
           if (choices[index]<zoomLevel) {break;}
        }
        zoomLevel=choices[index]; // new zoom level
        zoomToLevel(sequenceName,zoomLevel,true);
    }

     /**
     * Sets the zoom level (percentage value) for the specified sequence to the specified level.
     * If the current alignment setting for this sequence is something other than NONE
     * the ViewPort will be repositioned according to alignment settings
     * 
     * @param sequenceName The name of the sequence itself 
      */
    public void zoomToLevel(String sequenceName, double zoomLevel, boolean redraw) {
          int fixedScreenPoint=1;
          int genomicAnchor=1;
          boolean cheatonrightalign=false;
          String alignment=getSequenceAlignment(sequenceName);
          Sequence datasequence=(Sequence)getEngine().getDataItem(sequenceName);
          int sequenceStart=datasequence.getRegionStart();
          int sequenceEnd=datasequence.getRegionEnd();
          int shownOrientation=getSequenceOrientation(sequenceName);
          if (alignment.equals(ALIGNMENT_RIGHT))  {
              if ((shownOrientation==DIRECT && getSequenceViewPortEnd(sequenceName)==sequenceEnd) || (shownOrientation==REVERSE && getSequenceViewPortStart(sequenceName)==sequenceStart) ) cheatonrightalign=true;
          }
          double scale=zoomLevel/100.0;
          if (alignment.equals(ALIGNMENT_TSS)) {
                 Integer TSSint=datasequence.getTSS();
                 if (TSSint!=null) genomicAnchor=TSSint.intValue();
                 else genomicAnchor=(datasequence.getStrandOrientation()==VisualizationSettings.DIRECT)?sequenceEnd:sequenceStart;                   
                 int[] range=getScreenCoordinateFromGenomic(sequenceName, genomicAnchor);
                 fixedScreenPoint=(int)(Math.floor((double)range[0]/scale)*scale);   
          } else if (alignment.equals(ALIGNMENT_LEFT)) {
                 genomicAnchor=(shownOrientation==VisualizationSettings.DIRECT)?sequenceStart:sequenceEnd;                   
                 int[] range=getScreenCoordinateFromGenomic(sequenceName, genomicAnchor);
                 fixedScreenPoint=(int)(Math.floor((double)range[0]/scale)*scale);     
                 //gui.debugMessage("ZoomOut "+sequenceName+" scale="+scale+", screen-range=["+range[0]+","+range[1]+"]  range0="+range[0]+" corrected to "+fixedScreenPoint);                 
          } else if (alignment.equals(ALIGNMENT_RIGHT)) {
                 genomicAnchor=(shownOrientation==VisualizationSettings.REVERSE)?sequenceStart:sequenceEnd;                   
                 int[] range=getScreenCoordinateFromGenomic(sequenceName, genomicAnchor);
                 fixedScreenPoint=(int)(Math.floor((double)range[1]/scale)*scale);                              
                 //gui.debugMessage("ZoomOut "+sequenceName+" scale="+scale+", screen-range=["+range[0]+","+range[1]+"]  range1="+range[1]+" corrected to "+fixedScreenPoint);                 
                 int overflow=(int)(Math.ceil(getSequenceWindowSize()/scale)-Math.floor(getSequenceWindowSize()/scale));
                 //gui.debugMessage("Overflow="+overflow);
                 fixedScreenPoint+=overflow;
          } else { // align to center point in window
                 fixedScreenPoint=(int)(getSequenceWindowSize()/2.0);
                 int[] gc=getGenomicCoordinateFromScreenOffset(sequenceName, fixedScreenPoint);
                 //genomicAnchor=(alignedToGene)?gc[0]:gc[1];                  
                 genomicAnchor=(int)((gc[0]+gc[1]))/2;                  
          }
          setSequenceZoomLevel(sequenceName,zoomLevel);
          if (cheatonrightalign) {
               if (shownOrientation==VisualizationSettings.DIRECT) setSequenceViewPortEnd(sequenceName, sequenceEnd);                
               else setSequenceViewPortStart(sequenceName, sequenceStart);
          } else {
            if (shownOrientation==REVERSE && alignment.equals(ALIGNMENT_RIGHT)) alignment=ALIGNMENT_LEFT;
            else if (shownOrientation==REVERSE && alignment.equals(ALIGNMENT_LEFT)) alignment=ALIGNMENT_RIGHT;
            alignSequenceViewPort(sequenceName, genomicAnchor, fixedScreenPoint,alignment); 
          }
          if (redraw) redraw();
    }    
    
    public void zoomToLevel(String[] sequenceNames,double zoomLevel, boolean redraw) {
        for (String sequenceName:sequenceNames) {
            if (sequenceName!=null) zoomToLevel(sequenceName, zoomLevel, false);
        }
        if (redraw) redraw();
    }

    /**
     * Sets the zoom level (percentage value) for the specified sequence to the next zoom level in 
     * the preset list which is greater than the current zoom level and adjusts the viewPort so that
     * the genomic position under the given screen coordinate will remain fixed in the same position
     * after scaling (unless constraints on the sequence prohibits this. In which case the sequence 
     * will be repositioned automatically once more to conform with the constraints) 
     * 
     * @param sequenceName The name of the sequence itself 
     * @param x the screen coordinate which should be fixed (first position=1)
     * @param useFineScale If TRUE the zoom-steps will be chosen based on a finer scale       *
     */   
    public void zoomInOnSequence(String sequenceName,int x, boolean useFineScale) {
        int gc[]=getGenomicCoordinateFromScreenOffset(sequenceName, x); // store genomic coordinate under cursor
        zoomInOnSequence(sequenceName,useFineScale); // zoom
        int gcoord=(int)((gc[0]+gc[1])/2);
        alignSequenceViewPort(sequenceName, gcoord, x, ALIGNMENT_NONE); // translate so that stored genomic coordinate aligns with cursor
        redraw();
    }
    
    /**
     * Sets the zoom level (percentage value) for the specified sequence to the next zoom level in 
     * the preset list which is smaller than the current zoom level and adjusts the viewPort so that
     * the genomic coordinate under the given screen coordinate will remain fixed in the same position 
     * after scaling (unless constraints on the sequence prohibits this. In which case the sequence 
     * will be repositioned automatically once more to conform with the constraints) 
     * 
     * @param sequenceName The name of the sequence itself 
     * @param x the screen coordinate which should be fixed (first position=1)
     * @param useFineScale If TRUE the zoom-steps will be chosen based on a finer scale       *
     */
    public void zoomOutOnSequence(String sequenceName,int x, boolean useFineScale) {
        int gc[]=getGenomicCoordinateFromScreenOffset(sequenceName, x); // store genomic coordinate under cursor
        zoomOutOnSequence(sequenceName, useFineScale); // zoom
        int gcoord=(int)((gc[0]+gc[1])/2);
        alignSequenceViewPort(sequenceName, gcoord, x, ALIGNMENT_NONE); // translate so that stored genomic coordinate aligns with cursor
        redraw();
    }


    /**
     * Sets the zoom level and repositions the sequence so that the full sequence 
     * fits exactly within the display window
     *
     * @param sequenceName The name of the sequence itself 
     */
    public void zoomToFitSequence(String sequenceName, boolean redraw) {
        double width=getSequenceWindowSize();
        Sequence sequence=(Sequence)getEngine().getDataItem(sequenceName);
        int start=sequence.getRegionStart();
        double sequenceLength=sequence.getSize();
        double scale=width/sequenceLength;
        setSequenceZoomLevel(sequenceName,scale*100.0);
        setSequenceViewPortStart(sequenceName, start);
        if (redraw) redraw();
    }
    
    public void zoomToFitSequence(String[] sequenceNames, boolean redraw) {
        if (sequenceNames==null) return;
        for (String sequenceName:sequenceNames) {
            if (sequenceName!=null) zoomToFitSequence(sequenceName,false);
        }
        if (redraw) redraw();
    }
    
     /**
     * Returns the global zoom level (percentage value) for all sequences. 
     * A zoom percentage of 100 means that there is a 1-to-1 correspondence between bases and pixels 
     * (i.e. one base per pixel). A zoom value of 200 means one base takes up 2 pixels, while zoom=50 
     * means that the values of 2 neighboring bases should be represented by 1 pixel.
     * @return The zoom percentage 
     */
    public double getGlobalZoomLevel() {
        Object value=getSetting(ZOOM_LEVEL);
        if (value!=null) {
            return ((Double)value).doubleValue();
        } else {
            storeSetting(ZOOM_LEVEL,new Double(DEFAULT_ZOOM_LEVEL));
            return DEFAULT_ZOOM_LEVEL;
        }
    }
    
    /**
     * Sets the global zoom level (percentage value) for all sequences. 
     * (However, individual sequences can choose to override this setting using setSequenceZoomLevel() )
     * A zoom percentage of 100 means that there is a 1-to-1 correspondence between bases and pixels 
     * (i.e. one base per pixel). A zoom value of 200 means one base takes up 2 pixels, while zoom=50 
     * means that the values of 2 neighboring bases should be represented by 1 pixel.
     * @param zoompercentage
     */
    public void setGlobalZoomLevel(double zoompercentage) {
            storeSetting(ZOOM_LEVEL,new Double(zoompercentage));
            notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.SCALE_CHANGED, null);
            redraw();
    }
    

    /**
     * Returns the scale level to be used for visualization of the given sequence.
     * Scale is related to zoomlevel in that the zoomlevel=scale*100
     * (Thus zoom=100 equals scale=1.0, zoom=25 equals scale=0.25 etc.)
     * @return scale level
     */
    public double getScale(String sequenceName){
        double zoomlevel=getSequenceZoomLevel(sequenceName);
        return zoomlevel/100.0;
    }
    
    /**
     * Returns the strand orientation to use for visualization of the specified sequence.
     * (The orientation to use for visualization can be selected by the user independent of
     * the original orientation of the sequence relative to its target gene).
     * This class provides two static constants which can be used to determine and compare orientation: DIRECT and REVERSE
     * @param sequenceName The name of the sequence itself 
     * @return An integer specifying orientation (DIRECT or REVERSE)
     */
    public int getSequenceOrientation(String sequenceName) {
        Object value=getSetting(sequenceName+"."+SEQUENCE_ORIENTATION);
        if (value!=null) {
            return ((Integer)value).intValue();
        } else {
            Sequence sequence=((Sequence)client.getEngine().getDataItem(sequenceName));
            if (sequence==null) return Sequence.DIRECT; // this should not happen, but alas it can
            int orientation=sequence.getStrandOrientation();
            storeSetting(sequenceName+"."+SEQUENCE_ORIENTATION,new Integer(orientation));
            return orientation;
        }
    }
    
    /**
     * Sets the strand orientation to use for visualization of the specified sequence.
     * (The orientation to use for visualization can be selected by the user independent of
     * the original orientation of the sequence relative to its target gene).
     * This class provides two static constants which can be used to determine and compare orientation: DIRECT and REVERSE

     * @param sequenceName The name of the sequence itself 
     * @param orientation An integer specifying orientation (DIRECT or REVERSE)
     */
    public void setSequenceOrientation(String sequenceName, int orientation) {
        storeSetting(sequenceName+"."+SEQUENCE_ORIENTATION,new Integer(orientation));
        redraw();
    }
    public void setSequenceOrientation(String sequenceName, int orientation, boolean redraw) {
        storeSetting(sequenceName+"."+SEQUENCE_ORIENTATION,new Integer(orientation));
        if (redraw) redraw();
    }   
    
    /**
     * Sets the zoom level (percentage value) for the specified sequence to the next zoom level in 
     * the preset list which is greater than the current zoom level and adjusts the viewPort so that
     * the genomic position under the given screen coordinate will remain fixed in the same position
     * after scaling
     * 
     * @param sequenceName The name of the sequence itself 
     * @param x The screen coordinate which should be fixed (first position=1)
     */  
    public void flipSequenceAroundFixedPoint(String sequenceName, int x) {
        int gc[]=getGenomicCoordinateFromScreenOffset(sequenceName, x); // store genomic coordinate under cursor
        int neworientation=(getSequenceOrientation(sequenceName)==DIRECT)?REVERSE:DIRECT;       
        storeSetting(sequenceName+"."+SEQUENCE_ORIENTATION,new Integer(neworientation));
        int gcoord=(int)((gc[0]+gc[1])/2);
        alignSequenceViewPort(sequenceName, gcoord, x, ALIGNMENT_NONE); // translate so that stored genomic coordinate aligns with cursor       
        redraw();
    }
                               
    
    /**
     * Returns the sequence alignment to use for visualization of the specified sequence.
     * This class provides two static constants which can be used to determine and compare alignments: LEFT and RIGHT
     * 
     * @param sequenceName The name of the sequence itself 
     * @return A String specifying alignment (LEFT or RIGHT)
     */
    public String getSequenceAlignment(String sequenceName) {
        Object value=getSetting(sequenceName+"."+SEQUENCE_ALIGNMENT);
        if (value!=null) {
            return (String)value;
        } else {// 
            String alignment=getDefaultSequenceAlignment();       
            storeSetting(sequenceName+"."+SEQUENCE_ALIGNMENT,alignment);
            return alignment;
        }
    }
    
    /**
     * Sets the sequence alignment to use for visualization of the specified sequence.
     * This class provides static constants which can be used to determine and compare alignments
     * (Note that calling this method does not results in any viewport updates to align sequences,
     * it only stores the settings for future references.)
     * If alignment is set to TSS the sequence will automatically be released from constraints
     * via call to setSequenceConstrained(...,...,false)
     * 
     * @param sequenceName The name of the sequence itself 
     * @param alignment A String specifying alignment (LEFT, RIGHT, TSS or NONE)
     */
    public void setSequenceAlignment(String sequenceName, String alignment) {
        storeSetting(sequenceName+"."+SEQUENCE_ALIGNMENT,alignment);
        if (alignment.equals(ALIGNMENT_TSS)) setSequenceConstrained(sequenceName, false);
    }    
    
    public void setSequenceAlignment(String[] sequenceNames, String alignment) {
        if (sequenceNames==null) return;
        for (String sequenceName:sequenceNames) {
            if (sequenceName!=null) setSequenceAlignment(sequenceName,alignment);
        }
    }     
    
    /**
     * Returns the default alignment to be used if nothing else is selected by the user
     * The default setting is persistent and can be set with setDefaultSequenceAlignment
     * @return The default alignment
     */
    public String getDefaultSequenceAlignment() {
        Object value=getPersistentSetting(SEQUENCE_DEFAULT_ALIGNMENT);
        if (value!=null) {
            return (String)value;
        } else {// if nothing else is specified align right. I think that is an OK default
            String alignment=ALIGNMENT_RIGHT;       
            storePersistentSetting(SEQUENCE_DEFAULT_ALIGNMENT,alignment);
            return alignment;
        }        
    }
    
    /**
     * Sets the default alignment to be used if nothing else is selected by the user
     * This setting is persistent
     * @param alignment The new default alignment
     */
    public void setDefaultSequenceAlignment(String alignment) {     
           storePersistentSetting(SEQUENCE_DEFAULT_ALIGNMENT,alignment);
    }

    /**
     * Gets the position of origo for this sequence in genomic coordinates
     * 
     * @param sequenceName The name of the sequence itself 
     * @return The position of origo in genomic coordinates
     */
    @Deprecated
    public int getSequenceOrigo(String sequenceName) {
        Object value=getSetting(sequenceName+"."+SEQUENCE_ORIGO);
        if (value!=null) {
            return ((Integer)value).intValue();
        } else {
            int origo=1; //
            storeSetting(sequenceName+"."+SEQUENCE_ORIGO,new Integer(origo));
            return origo;
        }
    }
    
    /**
     * Sets the position of origo for this sequence in genomic coordinates
     * 
     * @param sequenceName The name of the sequence itself 
     * @param origo The new position of origo (in genomic coordinates)
     */
    @Deprecated
    public void setSequenceOrigo(String sequenceName, int origo) {
            storeSetting(sequenceName+"."+RULER,new Integer(origo));
            redraw();
    }
    

    public String getJavascriptSetting() {
        Object value=getPersistentSetting(JAVASCRIPT);
        if (value!=null) {
            return (String)value;
        } else {//
            storePersistentSetting(JAVASCRIPT,HTML_SETTING_SHARED_FILE);
            return HTML_SETTING_SHARED_FILE;
        }
    }
    public void setJavascriptSetting(String value) {
             if (value.equalsIgnoreCase(HTML_SETTING_NONE)) value=HTML_SETTING_NONE;
        else if (value.equalsIgnoreCase(HTML_SETTING_NEW_FILE)) value=HTML_SETTING_NEW_FILE;
        else if (value.equalsIgnoreCase(HTML_SETTING_SHARED_FILE)) value=HTML_SETTING_SHARED_FILE;
        else if (value.equalsIgnoreCase(HTML_SETTING_EMBED)) value=HTML_SETTING_EMBED;
        else if (value.equalsIgnoreCase(HTML_SETTING_LINK)) value=HTML_SETTING_LINK;
        else value=HTML_SETTING_SHARED_FILE; // default
        storePersistentSetting(JAVASCRIPT, value);
    }
    public String getCSSSetting() {
        Object value=getPersistentSetting(CSS);
        if (value!=null) {
            return (String)value;
        } else {
            storePersistentSetting(CSS,HTML_SETTING_SHARED_FILE);
            return HTML_SETTING_SHARED_FILE;
        }
    }
    public void setCSSSetting(String value) {
             if (value.equalsIgnoreCase(HTML_SETTING_NONE)) value=HTML_SETTING_NONE;
        else if (value.equalsIgnoreCase(HTML_SETTING_NEW_FILE)) value=HTML_SETTING_NEW_FILE;
        else if (value.equalsIgnoreCase(HTML_SETTING_SHARED_FILE)) value=HTML_SETTING_SHARED_FILE;
        else if (value.equalsIgnoreCase(HTML_SETTING_EMBED)) value=HTML_SETTING_EMBED;
        else if (value.equalsIgnoreCase(HTML_SETTING_LINK)) value=HTML_SETTING_LINK;
        else value=HTML_SETTING_SHARED_FILE; // default
        storePersistentSetting(CSS, value);
    }
    public String getStylesheetSetting() {
        Object value=getPersistentSetting(STYLESHEET);
        if (value!=null) {
            return (String)value;
        } else {
            storePersistentSetting(STYLESHEET,"[default]");
            return "[default]";
        }        
    }
    public void setStylesheetSetting(String value) {
        storePersistentSetting("stylesheet", value);
    }

    
    /**
     * Returns a string that describes the type of ruler to be used for the specified sequence
     * @param sequenceName The name of the sequence itself 
     * @return A string specifying the type of ruler to use
     */
    public String getRuler(String sequenceName) {
        String key=null;
        Object value=null;
        if (sequenceName!=null) {
            key=sequenceName+"."+RULER;
            value=getSetting(key);
        } 
        if (value==null) {
            key=RULER;
            value=getSetting(key);
        }
        if (value!=null) {
            return (String)value;
        } else {
            String ruler=RULER_TSS; // default to TSS-ruler
            storeSetting(key,ruler);
            return ruler;
        }
    }

    
    /**
     * Sets the constraint on the specified sequence 
     * If set to TRUE the sequence should be "constrained" meaning:
     * 1) if the zoom-level is such that the entire sequence can fit within the window, 
     *    no part of the sequence should be allowed to be moved outside the window 
     * 2) if the sequence is larger than the window, the sequence should not be allowed 
     *    to move such that it no longer fills the entire window.
     * 
     * @param sequenceName The name of the sequence itself 
     * @param constrained TRUE if the sequence should be constrained or FALSE if it should be allowed to move freely
     */
    public void setSequenceConstrained(String sequenceName, boolean constrained) {
            storeSetting(sequenceName+"."+CONSTRAINED, constrained);
    }
    
    /**
     * Returns TRUE if the specified sequence is "contrained", which means that:
     * 1) if the zoom-level is such that the entire sequence can fit within the window, 
     *    no part of the sequence should be allowed to be moved outside the window 
     * 2) if the sequence is larger than the window, the sequence should not be allowed 
     *    to move such that it no longer fills the entire window.
     * 
     * @param sequenceName The name of the sequence itself 
     */
    public boolean isSequenceConstrained(String sequenceName) {
        Object value=getSetting(sequenceName+"."+CONSTRAINED);
        if (value!=null) {
            return ((Boolean)value).booleanValue();
        } else {    
            storeSetting(sequenceName+"."+CONSTRAINED,Boolean.TRUE);
            return true;
        }
    }

    /**
     * Sets the scroll lock on the specified sequence 
     * If set to TRUE the sequence is not allowed to autoscroll when mouse cursor is dragged outside sequence window
     * 
     * @param sequenceName The name of the sequence itself 
     * @param lock TRUE if the sequence should not be allowed to autoscroll
     */
    public void setSequenceScrollLock(String sequenceName, boolean lock) {
            storeSetting(sequenceName+"."+SCROLL_LOCK, lock);
    }
    
    /**
     * Returns TRUE if the specified sequence should not be allowed to autoscroll when mouse cursor is dragged outside sequence window
     * 
     * @param sequenceName The name of the sequence itself 
     */
    public boolean isSequenceScrollLocked(String sequenceName) {
        Object value=getSetting(sequenceName+"."+SCROLL_LOCK);
        if (value!=null) {
            return ((Boolean)value).booleanValue();
        } else {    
            storeSetting(sequenceName+"."+SCROLL_LOCK,Boolean.TRUE);
            return true;
        }
    }


    /**
     * Sets the type of ruler to be used for this sequence
     * @param sequenceName The name of the sequence itself 
     * @param ruler A string specifying the type of ruler to use
     */
    public void setRuler(String sequenceName, String ruler) {
            String key=RULER;
            if (sequenceName!=null) key=sequenceName+"."+RULER;
            storeSetting(key,ruler);
            redraw();
    }

    /**
     * Returns the width to use for the sequence labels in the visualization panel
     * Depending on the current preferences settings, this could either be a fixed width
     * or be based on the width of the longest sequence name
     * @return The label width
     */
    public int getSequenceLabelWidth() {    
        if (getScaleSequenceLabelsToFit()) {
            Object value=getSetting(SEQUENCE_LABEL_WIDTH);
            if (value!=null) {
                return ((Integer)value).intValue();
            } else {
                return getSequenceLabelFixedWidth();
            }             
        } else return getSequenceLabelFixedWidth();
       
    }

    /**
     * Returns a fixed width to use for the sequence labels in the visualization panel
     * @return The label width
     */
    public int getSequenceLabelFixedWidth() {       
        Object value=getPersistentSetting(SEQUENCE_LABEL_FIXED_WIDTH);
        if (value!=null) {
            return ((Integer)value).intValue();
        } else {
            storePersistentSetting(SEQUENCE_LABEL_FIXED_WIDTH,new Integer(DEFAULT_SEQUENCE_LABEL_WIDTH));
            return DEFAULT_SEQUENCE_LABEL_WIDTH;
        }        
    }
    
    /**
     * Sets the maximum fixed width to use for the sequence labels in the visualization panel
     * @param width
     */
    public void setSequenceLabelFixedWidth(int width) {
            storeSetting(SEQUENCE_LABEL_FIXED_WIDTH,new Integer(width));
            rearrangeDataTracks();
            redraw();
    }
    
    /**
     * Sets the width of the widest label currently in use
     * @param width
     */
    public void setSequenceLabelMaxWidth(int width) {
            boolean scaletofit=getScaleSequenceLabelsToFit();
            int oldsetting=-1;
            if (scaletofit) {
            Object value=getSetting(SEQUENCE_LABEL_WIDTH);
                if (value!=null) {
                    oldsetting=((Integer)value).intValue();
                } else {
                    oldsetting=getSequenceLabelFixedWidth();
                }
            }
            storeSetting(SEQUENCE_LABEL_WIDTH,new Integer(width));
            if (scaletofit && oldsetting<width) {
                rearrangeDataTracks();
                redraw();
            }
    }
    
    
    /**
     * Returns a value that specifies whether the sequence labels should be scaled 
     * to the width of the longest sequence name (true) or use a fixed width (false)
     */
    public boolean getScaleSequenceLabelsToFit() {       
        Object value=getPersistentSetting(SCALE_SEQUENCE_LABELS_TO_FIT);
        if (value!=null) {
            return ((Boolean)value).booleanValue();
        } else {
            storePersistentSetting(SCALE_SEQUENCE_LABELS_TO_FIT,Boolean.TRUE);
            return Boolean.TRUE;
        }        
    }
    
    /**
     * Specifies whether the sequence labels should be scaled to the width
     * of the longest sequence name (true) or use a fixed width (false)
     * @param doScale
     */
    public void setScaleSequenceLabelsToFit(boolean doScale) {
            storePersistentSetting(SCALE_SEQUENCE_LABELS_TO_FIT, doScale);
            rearrangeDataTracks();
            redraw();
    }
    
    
    /**
     * Gets the antialiasing setting for text. If this is set to true, small text 
     * in the visualization panel (such as coordinates for the ruler and control panel)
     * will be drawn with an antialiased font
     */
    public boolean useTextAntialiasing() {       
        Object value=getPersistentSetting(ANTIALIAS_TEXT);
        if (value!=null) {
            return ((Boolean)value).booleanValue();
        } else {
            storePersistentSetting(ANTIALIAS_TEXT,Boolean.TRUE);
            return Boolean.TRUE;
        }        
    }
    
    /**
     * Sets the antialiasing setting for text. If this is set to true, small text 
     * in the visualization panel (such as coordinates for the ruler and control panel)
     * will be drawn with an antialiased font
     * @param antialias
     */
    public void setUseTextAntialiasing(boolean antialias) {
            storePersistentSetting(ANTIALIAS_TEXT, antialias);
            redraw();
    }
    
    /**
     * Gets the antialiasing setting for motif logos. If this is set to true,
     * the base letters in the motif sequence logos will be drawn with an antialiased font
     */
    public boolean useMotifAntialiasing() {       
        Object value=getPersistentSetting(ANTIALIAS_MOTIFS);
        if (value!=null) {
            return ((Boolean)value).booleanValue();
        } else {
            storePersistentSetting(ANTIALIAS_MOTIFS,Boolean.TRUE);
            return Boolean.TRUE;
        }        
    }
    
    /**
     * Sets the antialiasing setting for motif logos. If this is set to true,
     * the base letters in the motif sequence logos will be drawn with an antialiased font     
     * @param antialias
     */
    public void setUseMotifAntialiasing(boolean antialias) {
            storePersistentSetting(ANTIALIAS_MOTIFS, antialias);
            redraw();
    }
    
    
    
    
    /**
     * A convenience method to decide whether or not the score of regions should be visualized
     * @param datasetname The name of the dataset
     * @return a boolean value which is true if the the score of regions should be visualized
     */
    public boolean shouldVisualizeRegionScore(String datasetname) {
        Object value=getSetting(datasetname+"."+VISUALIZE_REGION_SCORE);
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else {
            boolean defaultvalue=false;
            Data dataitem=client.getEngine().getDataItem(datasetname);
            if (dataitem instanceof RegionDataset && ((RegionDataset)dataitem).isMotifTrack()) defaultvalue=true;
            storeSetting(datasetname+"."+VISUALIZE_REGION_SCORE, defaultvalue);
            return defaultvalue;
        }
    }
    
    /**
     * A convenience method to select whether or not the score of regions should be visualized
     * by setting the height of regions relative to the score
     * @param datasetname The name of the dataset
     * @param boolean True if region score should be visualized
     */
    public void setVisualizeRegionScore(String datasetname, boolean show) {
        if (datasetname==null) return;
        storeSetting(datasetname+"."+VISUALIZE_REGION_SCORE, show);
        redraw();
    }   
    
    /**
     * A convenience method to select whether or not the score of regions should be visualized
     * by setting the height of regions relative to the score
     * @param datasetnames The names of the datasets
     * @param boolean True if region score should be visualized
     */
    public void setVisualizeRegionScore(String[] datasetnames, boolean show) {
        if (datasetnames==null) return;
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+VISUALIZE_REGION_SCORE, show);
        }
        redraw();
    }       

    /**
     * A convenience method to decide whether or not the strand of regions should be visualized
     * @param datasetname The name of the dataset
     * @return a boolean value which is true if the the strand of regions should be visualized
     */
    public boolean shouldVisualizeRegionStrand(String datasetname) {
        Object value=getSetting(datasetname+"."+VISUALIZE_REGION_STRAND);
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else {
            boolean defaultvalue=false;
            Data dataitem=client.getEngine().getDataItem(datasetname);
            if (dataitem instanceof RegionDataset && ((RegionDataset)dataitem).isMotifTrack()) defaultvalue=true;
            storeSetting(datasetname+"."+VISUALIZE_REGION_STRAND, defaultvalue);
            return defaultvalue;
        }
    }
    
    /**
     * A convenience method to select whether or not the strand of regions should be visualized
     * by drawing regions with the same relative orientation as the one currently shown above a
     * middle baseline and those with opposite orientation below this baselin
     * @param datasetname The name of the dataset
     * @param boolean True if region strand orientation should be visualized
     */
    public void setVisualizeRegionStrand(String datasetname, boolean show) {
        if (datasetname==null) return;        
        storeSetting(datasetname+"."+VISUALIZE_REGION_STRAND, show);
        redraw();
    }  
    
    /**
     * A convenience method to select whether or not the strand of regions should be visualized
     * by drawing regions with the same relative orientation as the one currently shown above a
     * middle baseline and those with opposite orientation below this baselin
     * @param datasetnames The names of the datasets
     * @param boolean True if region strand orientation should be visualized
     */
    public void setVisualizeRegionStrand(String[] datasetnames, boolean show) {    
        if (datasetnames==null) return;        
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+VISUALIZE_REGION_STRAND, show);
        }            
        redraw();
    }  
    
    /**
     * A convenience method to decide whether or not the region track
     * should be drawn "upside down" when shouldVisualizeRegionStrand() returns false
     * @param datasetname The name of the dataset
     * @return a boolean value which is true if the region track should be rendered upside down
     */
    public boolean renderUpsideDown(String datasetname) {
        Object value=getSetting(datasetname+"."+RENDER_UPSIDE_DOWN);
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else return false;
    }  
    
    /**
     * A convenience method to select whether or not the region track should 
     * be rendered "upside down" (when strand orientation is not shown)
     * @param datasetname The name of the dataset
     * @param boolean True if the region track should be rendered "upside down"
     */
    public void setRenderUpsideDown(String datasetname, boolean upsideDown) {
        if (datasetname==null) return;
        storeSetting(datasetname+"."+RENDER_UPSIDE_DOWN, upsideDown);
        redraw();
    }   
    
    /**
     * A convenience method to select whether or not the region tracks should 
     * be rendered "upside down" (when strand orientation is not shown)
     * @param datasetnames The names of the datasets
     * @param boolean True if the region tracks should be rendered "upside down"
     */
    public void setRenderUpsideDown(String[] datasetnames, boolean upsideDown) {
        if (datasetnames==null) return;
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+RENDER_UPSIDE_DOWN, upsideDown);
        }
        redraw();
    }    
    
    
    /**
     * A convenience method to decide whether or not regions should be drawn with drop-shadows
     * @param datasetname The name of the dataset
     * @return a boolean value which is true if drop-shadows should be drawn
     */
    public boolean useDropShadows(String datasetname) {
        Object value=getSetting(datasetname+"."+DROPSHADOW);
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else return false;
    }    

    /**
     * A convenience method to set whether or not regions should be drawn with drop-shadows
     * @param datasetname The name of the dataset
     * @param useshadows True if drop-shadows should be drawn
     */
    public void setUseDropShadows(String datasetname, boolean useshadows) {
        if (datasetname==null) return;        
        storeSetting(datasetname+"."+DROPSHADOW, useshadows);
        redraw();
    }  
    
    /**
     * A convenience method to set whether or not regions should be drawn with drop-shadows
     * @param datasetnames The names of the datasets
     * @param useshadows True if drop-shadows should be drawn
     */
    public void setUseDropShadows(String[] datasetnames, boolean useshadows) {    
        if (datasetnames==null) return;        
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+DROPSHADOW, useshadows);
        }            
        redraw();
    }    
    
    /**
     * A convenience method to set whether or not regions should be drawn with drop-shadows
     * @param datasetname The name of the dataset
     * @param settingName The name of the graph type setting
     * @param value The value for the setting
     */
    public void setGraphTypeSetting(String datasetname, String settingName, Object value) {
        if (datasetname==null) return;        
        storeSetting(datasetname+"."+settingName, value);
        redraw();
    }  
    
    /**
     * A convenience method to set whether or not regions should be drawn with drop-shadows
     * @param datasetnames The names of the datasets
     * @param settingName The name of the graph type setting
     * @param value The value for the setting
     */
    public void setGraphTypeSetting(String[] datasetnames, String settingName, Object value) {
        if (datasetnames==null) return;        
        for (String datasetname:datasetnames) {
            if (datasetname!=null) storeSetting(datasetname+"."+settingName, value);
        }            
        redraw();
    }   
    
    /** Returns a list (or double[]) containing some preset zoom-level choices
     *  @return a double[] with preset zoom levels
     */
    public double[] getZoomChoices() {
        return zoomChoices;
    }
    
  
    /**
     * Marks the named track as being "grouped", meaning that it should not be drawn on its own but rather on top of its parent
     * @param datasetname
     * @param isGrouped 
     */
    public void setGroupedTrack(String datasetname, boolean isGrouped) {
        if (datasetname==null) return;       
        storeSetting(datasetname+"."+GROUP_TRACK, isGrouped);
        rearrangeDataTracks();
        redraw();        
    }
    
    /**
     * Marks the named tracks as being "grouped", meaning that they should not be drawn on their own but rather on top of their parents
     * @param datasetnames
     * @param isGrouped 
     */
    public void setGroupedTracks(String[] datasetname, boolean isGrouped) {
        if (datasetname==null) return;   
        for (String name:datasetname) {
            storeSetting(name+"."+GROUP_TRACK, isGrouped);
        }
        rearrangeDataTracks();
        redraw();        
    }    
    
    /**
     * Returns TRUE if the named track is grouped, i.e. is a subordinate part of a group track
     * The "parent" (or "primary") track in the group is not marked as being "grouped" 
     */    
    public boolean isGroupedTrack(String datasetname) {
        Object value=getSetting(datasetname+"."+GROUP_TRACK);        
        if (value!=null) return ((Boolean)value).booleanValue(); 
        else return false;        
    }    
    
    /**
     * Returns TRUE if the named track should be drawn as a "grouped track" together with its dependent children.
     * Parent tracks are not marked as such, but they should be drawn as grouped tracks iff all the following conditions are true:
     *   1) The track itself is not marked as belonging to a group (i.e. it is not a "child" track)
     *   2) The track is not hidden
     *   3) The track has one (ore more) non-hidden tracks marked as "grouped" following it in the master track order list
     * In other words, a track should not be drawn as a parent group track if: it is itself grouped, or it is hidden, or all its children are hidden
     */    
    public boolean isGroupTrackParent(String datasetname) {
        if (isGroupedTrack(datasetname) || !isTrackVisible(datasetname)) return false;
        for (int i=0;i<datatrackorderMasterList.size()-1;i++) { // note that the loop only goes to size-1, since the dataset cannot be a parent if it is the last in the list 
            if (datatrackorderMasterList.get(i).equals(datasetname)) { // We have found the target track, now check the ones that follow it in the list
                for (int j=i+1;j<datatrackorderMasterList.size();j++) {
                   String nextTrack=datatrackorderMasterList.get(j); 
                   if (!gui.getEngine().dataExists(nextTrack, FeatureDataset.class)) continue; // the master list can contain names of datasets that have since been deleted (so they will be put back where they were in case of UNDO). Ignore these here.
                   if (!isGroupedTrack(nextTrack)) return false;  // the next track in the list is not a grouped track, which means we did not find any viable children for the target track.
                   if (isTrackVisible(nextTrack)) return true; // // we have a visible grouped child! The target track must now be considred a responsible parent.F
                }
                return false; 
            }
        }       
        return false;
    }    
    
    /**
     * If the named dataset is a "responsible parent track" (i.e. a track that should draw both itself and at least one grouped child track),
     * this method will return a list of the names of the children (dependent tracks). If the dataset is not a responsible parent, the function will return null.
     * @param datasetname
     * @return 
     */
    public ArrayList<String> getTrackGroup(String datasetname) {
        if (isGroupedTrack(datasetname) || !isTrackVisible(datasetname)) return null;
        for (int i=0;i<datatrackorderMasterList.size()-1;i++) { // note that the loop only goes to size-1, since the dataset cannot be a parent if it is the last in the list 
            if (datatrackorderMasterList.get(i).equals(datasetname)) { // We have found the target track, now check the ones that follow it in the list
                ArrayList<String> children=new ArrayList<>(datatrackorderMasterList.size()-i); // this size should be enough
                for (int j=i+1;j<datatrackorderMasterList.size();j++) {
                   String nextTrack=datatrackorderMasterList.get(j); 
                   if (!gui.getEngine().dataExists(nextTrack, FeatureDataset.class)) continue; // the master list can contain names of datasets that have since been deleted (so they will be put back where they were in case of UNDO). Ignore these here.
                   if (!isGroupedTrack(nextTrack)) break; // 
                   if (!isTrackVisible(nextTrack)) continue; // ignore hidden tracks also. If all the children are hidden the parent will have no responsibilities for them
                   children.add(nextTrack); // This track must now be groupd and visible, so we add it to the list
                }
                return (children.isEmpty())?null:children; 
            }
        }       
        return null;       
    }
    
    /** Returns the name of the parent track for the given track,
     *  or NULL if the given dataset is not a grouped track or has no viable parent
     */
    public String getParentTrack(String datasetname) {
        if (!isGroupedTrack(datasetname)) return null;
        String parent=null; // this will be updated as we go down the list. The last valid value encountered before finding the target dataset will be the parent
        for (int i=0;i<datatrackorderMasterList.size()-1;i++) { // note that the loop only goes to size-1, since the dataset cannot be a parent if it is the last in the list            
            String nextTrack=datatrackorderMasterList.get(i);
            if (nextTrack.equals(datasetname)) return parent;
            if (!gui.getEngine().dataExists(nextTrack, FeatureDataset.class)) continue; // the master list can contain names of datasets that have since been deleted (so they will be put back where they were in case of UNDO). Ignore these here.
            if (!isGroupedTrack(nextTrack)) parent=nextTrack;
        }       
        return parent;         
    }
    
    /**
     * given a "screen coordinate" from a mouse event within a component, this function will calculate and return
     * the corresponding genomic coordinate (or range if zoom is less than 100.0)  
     * The function returns an int[] with two numbers. If scale is 1.0 or more, these two numbers will be 
     * the same and reflect the genomic coordinate corresponding to the screen coordinate given 
     * (orientation considered). Note that this function assumes the component is drawn without border (so the sequence itself starts at x=0),
     * so if borders are used (and they normally are!!) you must offset the screen X parameter accordingly first
     * 
     * @param sequenceName The name of the sequence itself 
     * @param x the offset within the component window (first window pixel should be 0 so remember to exclude border!)
     * @return range of genomic coordinates corresponding to screen coordinates 
     */
    public int[] getGenomicCoordinateFromScreenOffset(String sequenceName, int x) {
        int viewPortStart=getSequenceViewPortStart(sequenceName);
        int viewPortEnd=getSequenceViewPortEnd(sequenceName);
        double scale=getScale(sequenceName);
        int orientation=getSequenceOrientation(sequenceName);
        int firstoffset=(int)Math.floor(x/scale);
        int lastoffset=(int)Math.ceil((x+1)/scale)-1;
        int first=0,last=0;
        if (orientation==Sequence.DIRECT) {first=viewPortStart+firstoffset;last=viewPortStart+lastoffset;}
        else {first=viewPortEnd-lastoffset;last=viewPortEnd-firstoffset;}
        return new int[]{first,last};
      
    } 
    
    /**
     * given a "genomic coordinate" from a mouse event, this function will calculate and return
     * the corresponding "screen coordinate" (offset within component) or range if zoom is more than 100.0  
     * The function returns an int[] with two numbers. If scale is 1.0 or less, these two numbers will (probably) be
     * the same and reflect the screen coordinate corresponding to the genomic coordinate given 
     * (orientation considered). This function does not care how the component is drawn (optimized or not)
     * and will return the coordinate in "screen pixels". Note that this function assumes that component is drawn without border,
     * so if borders are used (and they normally are!!), the coordinate must be offset accordingly!
     * 
     * @param sequenceName The name of the sequence itself 
     * @param x the genomic coordinate
     * @return range of genomic coordinates corresponding to screen coordinates 
     */
    public int[] getScreenCoordinateFromGenomic(String sequenceName, int x) {
        int viewPortStart=getSequenceViewPortStart(sequenceName);
        int viewPortEnd=getSequenceViewPortEnd(sequenceName);
        double scale=getScale(sequenceName);
        int orientation=getSequenceOrientation(sequenceName);
            int first=0,last=0;
            if (orientation==Sequence.DIRECT) {
                first=(int)Math.floor((x-viewPortStart)*scale);
                last=(int)Math.ceil((x+1-viewPortStart)*scale)-1;
            }
            else {
                first=(int)Math.floor((viewPortEnd-x)*scale);
                last=(int)Math.ceil((viewPortEnd-x+1)*scale)-1;
            }
            return new int[]{first,last};   
    } 
    
    /**
     * Given two "genomic coordinates" this function will calculate and return the "outer" screen coordinates
     * Note that this function assumes that component is drawn without border,
     * so if borders are used (and they normally are!!), the coordinates must be offset accordingly!
     * @param sequenceName
     * @param x
     * @param y
     * @return 
     */
    public int[] getScreenCoordinatesFromGenomic(String sequenceName, int x, int y) {
        int[] first=getScreenCoordinateFromGenomic(sequenceName,x);
        int[] second=getScreenCoordinateFromGenomic(sequenceName,y);
        int min=first[0];
        if (first[1]<min) min=first[1];
        if (second[0]<min) min=second[0];
        if (second[1]<min) min=second[1];
        int max=first[0];
        if (first[1]>max) max=first[1];
        if (second[0]>max) max=second[0];
        if (second[1]>max) max=second[1];  
        return new int[]{min,max};
    }
        
    /** Sets the order of the tracks manually according to the given list 
     *  @param trackorder The names of the tracks in the right order
     *         Existing tracks from the masterlist not explicitly listed in the parameter will be sorted at the end
     */
    public void setTrackOrder(String[] trackorder) {
        if (trackorder==null) return;
        // update master list first by creating a new list with the new sort order and then swapping  
        ArrayList<String> newlist=new ArrayList<String>(trackorder.length);
        newlist.addAll(Arrays.asList(trackorder));
        for (String trackname:datatrackorderMasterList) {
            if (!newlist.contains(trackname)) newlist.add(trackname);
        }
        datatrackorderMasterList=newlist;
        // now update the current (visible) tracklist
        if (gui!=null) gui.getFeaturesPanelListModel().orderByTrackList(trackorder,true);
        else rearrangeDataTracksForNonVisualClient();       
    }
    
    /** This is called from FeaturesPanelListModel when the order of tracks changes in response to user actions (reordering) */
    public void setupMasterTrackorderList(FeaturesPanelListModel model) {
        ArrayList<String> neworder=new ArrayList<String>();
        for (int i=0;i<model.size();i++) { // populate with the current list
            Data data=(Data)model.getElementAt(i);
            if (data!=null) neworder.add(data.getName());
        }
        // now check if there are any more left in the master list and incorporate those also
        for (int i=0;i<datatrackorderMasterList.size();i++) {
            String name=datatrackorderMasterList.get(i);
            if (neworder.contains(name)) continue;
            if (i==0) { // first element in masterlist is not in new list. Place it on top!
                neworder.add(0, name);
            } else {
                String previous=datatrackorderMasterList.get(i-1);
                int newindex=neworder.indexOf(previous);
                neworder.add(newindex+1, name);
            }
        }
        datatrackorderMasterList=neworder;
    }
    
    /** 
     * Returns the correct position in the track order for the dataset with the given name
     * If a dataset with the given name has never been encountered before, the value -1 will be returned
     */
    public int getTrackOrderPosition(String name, FeaturesPanelListModel model) {
        if (!datatrackorderMasterList.contains(name)) return -1;
        else {
            int i=datatrackorderMasterList.indexOf(name);
            if (i==0) return 0;           
            int newindex=-1;
            while (newindex<0 && i>0){
               String previous=datatrackorderMasterList.get(i-1);
               newindex=getPositionInModel(previous,model);
               i--;
            }            
             return newindex+1; 
        }    
    }
    
  /**
   * Given an list containing names of feature dataset, this method will return these names 
   * (or a subset of them) according to the order they are listed in the 'MasterList' used by 
   * non-visual clients
   * @param list A list of names of feature datasets
   * @param skiphidden If TRUE only currently visible feature datasets will be included in the resulting list
   * @param aggregated
   * @return A list of names of feature datasets in sorted order
   */
  public ArrayList<String> sortFeaturesAccordingToOrderInMasterList(ArrayList<String> list, boolean skiphidden, boolean aggregated) {
      ArrayList<String> result=new ArrayList<String>(list.size());
      for (int i=0;i<datatrackorderMasterList.size();i++) {
          String name=datatrackorderMasterList.get(i);
          if (name!=null) {
              if (skiphidden && !isTrackVisible(name)) continue;
              if (list.contains(name) && !result.contains(name)) result.add(name);
              else if (aggregated) {
                  String prefix=getAggregatePrefix(name);
                  if (prefix!=null && skiphidden && !isTrackVisible(prefix)) continue;
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
    
    private int getPositionInModel(String name, FeaturesPanelListModel model) {
        int newindex=-1;
        for (int x=0;x<model.size();x++) {
           if (((Data)model.get(x)).getName().equals(name)) {newindex=x;break;}
        } 
        return newindex;
    }
    
    /**
     * Clears all non-persistent settings
     * Note that this could potentially create problems since the GUI does not respond properly
     */
    public void clearAllSettings() {
        settings.clear();
        redraw();
    }       
     /**
     * This method goes through all settings and clears those that contain
     * the specified key string as a substring of their key.
     * (Eg: if subkey="zoom" then settings with keys such as "zoom", "sequencename.zoom" 
     * and "kazoom" will be removed.)
     * @param subkey
     */
    public void clearAllSettingsFor(String subkey) {
        Set<String> keys=settings.keySet();
        ArrayList<String> toDelete=new ArrayList<String>();
        for (String key:keys) {
            if (key.indexOf(subkey)>0) toDelete.add(key);
        }
        for (String key:toDelete) {
            settings.remove(key);
        }
    }   
    
    public void debug() { // prints all settings to log.         
        ArrayList<String> keys=new ArrayList<String>(settings.keySet());
        Collections.sort(keys);
        for (String k:keys) {
            Object val=settings.get(k);
            if (val==null) debugOutput("Viz:  "+k+" => NULL");
            else debugOutput("VisualizationSettings:  "+k+" => "+val.toString());
        }
        debugOutput("=== MasterList ===");
        for (String track:datatrackorderMasterList) {
            String hidden="  ", grouped="";
            if (!gui.getEngine().dataExists(track, FeatureDataset.class)) hidden="  [DELETED]"; 
            else {
                if (!isTrackVisible(track)) hidden="  [hidden]";
                if (isGroupedTrack(track)) grouped="  [grouped] => parent: "+getParentTrack(track);
                else if (isGroupTrackParent(track)) grouped="  [group parent] => "+getTrackGroup(track);
            }
            debugOutput("   "+track+hidden+grouped);
        }     
        debugOutput("-------------");
    }

    private void debugOutput(String msg) {
        if (client!=null) client.logMessage(msg);
        else System.err.println(msg);
    }
    
    /* Listener callback method that the engine calls when Data is added (or datasets have sequences added to them) */
    @Override
    public void dataAdded(Data data) {
        //gui.debugMessage("Data Added    data="+data.getName()+" class="+data.getClass().toString());
        if (data instanceof FeatureDataset || data instanceof Sequence) notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.ADDED,data);
    }
    
    /* Listener callback method that the engine calls when Data objects representing datasets have items added to them */
    @Override
    public void dataAddedToSet(Data parent, Data child) {
        //gui.debugMessage("Data AddedToSet   parent="+parent.getName()+" class="+parent.getClass().toString()+",  child="+child.getName()+" class="+child.getClass().toString());
        //if (parent instanceof FeatureDataset || getEngine().isDefaultSequenceCollection(parent)) notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.ADDED,parent);
    }

    /* Listener callback method that the engine calls when Data is removed (or datasets have sequences removed from them) */
    @Override
    public void dataRemoved(Data data) {
        if (data instanceof FeatureDataset || data instanceof Sequence) notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REMOVED,data);
    }
    
    /* Listener callback method that the engine calls when Data objects representing datasets have items reomved from them */
    @Override
    public void dataRemovedFromSet(Data parent, Data child) {
        //gui.debugMessage("Data AddedToSet parent="+parent.getName()+" class="+parent.getClass().toString()+",  child="+child.getName()+" class="+child.getClass().toString());
        if (parent instanceof FeatureDataset || getEngine().isDefaultSequenceCollection(parent)) notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REMOVED,parent);
    }

    /* Listener callback method that the engine calls when a Data item has been updated */
    @Override
    public void dataUpdated(Data data) {
        //gui.debugMessage("Data updated "+data.getName()+" class="+data.getClass().toString());
        if (data instanceof FeatureDataset || getEngine().isDefaultSequenceCollection(data)) notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.UPDATED,data);
    }
    
    /* Listener callback method that the engine calls just prior to data update */
    @Override
    public void dataUpdate(Data oldvalue, Data newvalue) {
        // this event is not important for this class
    }

    /* Listener callback method that the engine calls when a Data item has been updated */
    @Override
    public void dataOrderChanged(Data data, Integer oldpos, Integer newpos) {
        //gui.debugMessage("Data updated "+data.getName()+" class="+data.getClass().toString());
        if (getEngine().isDefaultSequenceCollection(data)) {
            notifyListenersOfSequenceReordering(oldpos,newpos);
        }
    }    
    
    
    
    
    
    /** Listener interface method called by DatasetPanels when contents has changed */
    @Override
    public void contentsChanged(ListDataEvent e) {
        
    }

    /** Listener interface method called by the FeaturesPanel when order of items is changed (specifically when a new item is inserted) */
    @Override
    public void intervalAdded(ListDataEvent e) {
        if (gui==null || !featurePanelListeningEnabled) return;
        FeaturesPanelListModel listmodel=(FeaturesPanelListModel)e.getSource();
        if (listmodel==gui.getFeaturesPanelListModel()) rearrangeDataTracks(); // is the event coming from the "Features" panel
        
    }

    /** listener interface method called by the FeaturesPanel when order of items is changed (specifically when an item is removed from the list) */
    @Override
    public void intervalRemoved(ListDataEvent e) {
        if (gui==null || !featurePanelListeningEnabled) return;
        FeaturesPanelListModel listmodel=(FeaturesPanelListModel)e.getSource();
        if (listmodel==gui.getFeaturesPanelListModel()) rearrangeDataTracks(); // is the event coming from the "Features" panel
    }
    
    /**
     * Registers a new VisualizationSettingsListener that will be notified when important settings changes
     * @param listener
     */
    public void addVisualizationSettingsListener(VisualizationSettingsListener listener) {        
        if (listeners.contains(listener)) return;
        synchronized(listeners) {
            // Note that this strategy of adding listeners represents a very simple sorting procedure
            // which ensures that SequenceVisualizers is listed before VisualizationPanel
            // and will thus receive notifications before the VisualizationPanel (which can be essential for correct updates)
            if (listener instanceof VisualizationPanel) listeners.add(listener);
            else listeners.add(0,listener);
        }
    }
    
    
    /** This method functions as a sort of 'master switch' to turn on or off VisualizationSettingsListener notifications */
    public void enableVisualizationSettingsNotifications(boolean enable) {
        enableNotifications=enable;
    }

    public boolean isNotificationsEnabled() {
        return enableNotifications;
    }
    
    /** This method can be used to specify whether or not VisualizationSettings should respond to events in the FeaturePanel (add/remove/reorder) */
    public void enableFeaturePanelListening(boolean enable) {
        featurePanelListeningEnabled=enable;
    }

    /**
     * Removes the specified VisualizationSettingsListener
     * @param listener
     */
    public void removeVisualizationSettingsListener(VisualizationSettingsListener listener) {
        synchronized(listeners) {
          listeners.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public void notifyListenersOfTrackOrderRearrangement(int type, Data affected) {
        //Since the original list of listeners might change as the result of a callback method invokation, it is necessary to synchronize and clone the list to avoid ConcurrentModificationExceptions     
        if (!enableNotifications) return;
        ArrayList<VisualizationSettingsListener> listenerList;
        synchronized(listeners) {   
            listenerList=(ArrayList<VisualizationSettingsListener>)listeners.clone();
        }
        for (Object listener:listenerList) {
            ((VisualizationSettingsListener)listener).trackReorderEvent(type, affected);
        }   
//        javax.swing.JComponent panel=gui.getVisualizationPanel();
//        if (panel!=null) {
//            RepaintManager.currentManager(panel).markCompletelyDirty(panel);
//            panel.revalidate();
//        }
        redraw();
    }

    /**
     * Notifies listeners that an event has occurred that could influence the layout of the sequence beneath each other
     * For instance hiding/showing individual sequences or a resize of a single sequence (but not sequence reordering)
     * @param type
     * @param affected
     */
    @SuppressWarnings("unchecked")
    public void notifyListenersOfSequenceLayoutUpdate(int type, Data affected) {
        //Since the original list of listeners might change as the result of a callback method invokation, it is necessary to synchronize and clone the list to avoid ConcurrentModificationExceptions
        // if (!enableNotifications && type!=VisualizationSettingsListener.REMOVED) return; // remove notifications should always be passed on!
        if (enableNotifications==false && !(type==VisualizationSettingsListener.REMOVED && (affected instanceof FeatureDataset || affected instanceof Sequence))) return; // Notifications that featuredata has been removed should always be passed on or else we might have problems with cached visualizers. Not sure if this approach is the best solution to this...
        ArrayList<VisualizationSettingsListener> listenerList;
        synchronized(listeners) {   
            listenerList=(ArrayList<VisualizationSettingsListener>)listeners.clone();
        }
        for (Object listener:listenerList) {
            ((VisualizationSettingsListener)listener).sequencesLayoutEvent(type, affected);
        }   
        redraw();
    }
    
    /**
     * Notifies listeners that the order of one or more sequences have been changed 
     * @param type
     * @param affected
     */
    @SuppressWarnings("unchecked")
    public void notifyListenersOfSequenceReordering(Integer oldposition, Integer newposition) {
        //Since the original list of listeners might change as the result of a callback method invokation, it is necessary to synchronize and clone the list to avoid ConcurrentModificationExceptions
        // if (!enableNotifications && type!=VisualizationSettingsListener.REMOVED) return; // remove notifications should always be passed on!
        if (!enableNotifications) return; // remove notifications should always be passed on!
        ArrayList<VisualizationSettingsListener> listenerList;
        synchronized(listeners) {   
            listenerList=(ArrayList<VisualizationSettingsListener>)listeners.clone();
        }
        for (Object listener:listenerList) {
            ((VisualizationSettingsListener)listener).sequencesReordered(oldposition, newposition);
        }   
        redraw();
    }    
   
    public void notifyListenersOfSequenceWindowSizeChange(int newsize, int oldsize) {
        if (!enableNotifications) return;
        for (Object listener:listeners) {
            ((VisualizationSettingsListener)listener).sequenceWindowSizeChangedEvent(newsize, oldsize);
        }
    }
    
    public void notifyListenersOfSequenceMarginChange(int newsize) {
        if (!enableNotifications) return;
        for (Object listener:listeners) {
            ((VisualizationSettingsListener)listener).sequenceMarginSizeChangedEvent(newsize);
        }
    }
    
    public void notifyListenersOfDataRenameEvent(String oldname,String newname) {
        if (!enableNotifications) return;
        for (Object listener:listeners) {
            ((VisualizationSettingsListener)listener).dataRenamedEvent(oldname,newname);
        }
    }
       
    /**
     * Returns a color to use for certain styled elements, for instance to highlight significant 
     * cells in a table. If no color is previously specified for that class, the color
     * BLACK will be returned
     * @param classname 
     */
    public Color getSystemColor(String element) {
        Object value=getSetting(SYSTEM_COLOR+"."+element);
        if (value instanceof Color) return (Color)value;
        else { // default system colors
            Color assignedcolor=Color.BLACK;
                 if (element.equals("verysignificant")) assignedcolor=new Color(255,0,0);
            else if (element.equals("significant")) assignedcolor=new Color(255,136,136);
            else if (element.equals("onlyintarget")) assignedcolor=new Color(255,0,0);
            else if (element.equals("overrepintarget")) assignedcolor=new Color(255,136,136);
            else if (element.equals("intarget")) assignedcolor=new Color(255,68,68);
            else if (element.equals("samerate")) assignedcolor=new Color(255,255,0);
            else if (element.equals("onlyincontrol")) assignedcolor=new Color(0,255,0);
            else if (element.equals("overrepincontrol")) assignedcolor=new Color(136,255,136);
            else if (element.equals("incontrol")) assignedcolor=new Color(68,255,68);
            else if (element.equals("notpresent")) assignedcolor=new Color(255,255,255);
            else if (element.equals("inside")) assignedcolor=Color.RED;
            else if (element.equals("outside")) assignedcolor=Color.BLUE;
            else if (element.equals("Sensitivity")) assignedcolor=Color.RED;
            else if (element.equals("Specificity")) assignedcolor=Color.GREEN;
            else if (element.equals("Positive Predictive Value")) assignedcolor=Color.BLUE;
            else if (element.equals("Negative Predictive Value")) assignedcolor=Color.YELLOW;
            else if (element.equals("Performance Coefficient")) assignedcolor=Color.MAGENTA;
            else if (element.equals("Average Site Performance")) assignedcolor=Color.CYAN;
            else if (element.equals("F-measure")) assignedcolor=Color.ORANGE;
            else if (element.equals("Accuracy")) assignedcolor=Color.LIGHT_GRAY;
            else if (element.equals("Correlation Coefficient")) assignedcolor=Color.BLACK;
            else if (element.equals("Sensitivity (site)")) assignedcolor=new Color(255,140,140);
            else if (element.equals("Positive Predictive Value (site)")) assignedcolor=new Color(96,173,255);       
            else if (element.equals("Average Site Performance (site)")) assignedcolor=new Color(0,150,150); 
            else if (element.equals("Performance Coefficient (site)")) assignedcolor=new Color(153,51,255);
            else if (element.equals("F-measure (site)")) assignedcolor=new Color(180,90,30);
            else if (element.equals("color1")) assignedcolor=Color.RED;
            else if (element.equals("color2")) assignedcolor=Color.BLUE;
            else if (element.equals("color3")) assignedcolor=Color.GREEN;
            else if (element.equals("color4")) assignedcolor=Color.MAGENTA;
            else if (element.equals("color5")) assignedcolor=Color.CYAN;
            else if (element.equals("color6")) assignedcolor=Color.YELLOW;
            else if (element.equals("histogram")) assignedcolor=new Color(100,100,255);
            else if (element.equals("histogramSelected")) assignedcolor=new Color(200,200,255);
            //else if (element.equals("PDV_histogram")) assignedcolor=Color.BLUE;
            //else if (element.equals("interactions_level1")) assignedcolor=Color.BLUE;             
            storeSetting(SYSTEM_COLOR+"."+element, assignedcolor);
            return assignedcolor;
        }
    }
        
    /**
     * Returns a color to use for certain styled elements, for instance to highlight significant 
     * cells in a table. The color is returned as a 6-digit Hex-string (e.g. '#FF0000' for red)
     * that can be incorporated into HTML code
     */
    public String getSystemColorAsHTML(String classname) {
        return convertColorToHTMLrepresentation(getSystemColor(classname));
    }    
    
    
    /**
     * Returns a system font (if this has been configured in the settings)
     * @param setting
     * @return 
     */
    public Font getSystemFont(String setting) {
        Object fontNameObject=getSetting(setting);
        if (fontNameObject==null || !(fontNameObject instanceof String)) {
            fontNameObject=defaultGraphFont.getFontName();
            //return defaultGraphFont;
        }
        String fontName=(String)fontNameObject;
        int fontSize=12;
        int fontStyle=Font.PLAIN;
        Object fontSizeObject=getSetting(setting+"Size");
        Object fontStyleObject=getSetting(setting+"Style");
        if (fontName.contains(",")) { // font could be specified as a triplet "fontname,size,style"
            String[] parts=fontName.split("\\s*,\\s*");
            if (parts.length==3) {
                fontName=parts[0];
                fontStyleObject=parts[2];
                try {
                    int value=Integer.parseInt(parts[1]);
                    fontSizeObject=new Integer(value);
                } catch (NumberFormatException e) {}
            }
        }
        if (fontStyleObject instanceof String) {
                 if (((String)fontStyleObject).equalsIgnoreCase("plain")) fontStyle=Font.PLAIN;
            else if (((String)fontStyleObject).equalsIgnoreCase("bold")) fontStyle=Font.BOLD;
            else if (((String)fontStyleObject).equalsIgnoreCase("italic")) fontStyle=Font.ITALIC;
            else if (((String)fontStyleObject).equalsIgnoreCase("italics")) fontStyle=Font.ITALIC;
            else if (((String)fontStyleObject).equalsIgnoreCase("bolditalic")) fontStyle=(Font.BOLD | Font.ITALIC);
            else if (((String)fontStyleObject).equalsIgnoreCase("italicbold")) fontStyle=(Font.BOLD | Font.ITALIC);
            else if (((String)fontStyleObject).equalsIgnoreCase("bolditalics")) fontStyle=(Font.BOLD | Font.ITALIC);
            else if (((String)fontStyleObject).equalsIgnoreCase("italicsbold")) fontStyle=(Font.BOLD | Font.ITALIC);
            else fontStyle=Font.PLAIN;
        } 
        if (fontSizeObject instanceof Integer) {
            fontSize=((Integer)fontSizeObject).intValue();
            if (fontSize<3) fontSize=3;
            if (fontSize>200) fontSize=200;
        }
        Font font=new Font(fontName, fontStyle, fontSize);
        return font;        
    }
    
    
    /**
     * Adds a new macro definition to the current set of macros
     */
    public void addMacro(String macroname, String macrodefinition) {
        if (!hasSetting(MACROS)) {
            storeSetting(MACROS, new HashMap<String, String>()); 
        }
        HashMap<String, String> macros=(HashMap<String, String>)getSetting(MACROS);
        macros.put(macroname, macrodefinition);
    }
    
    /**
     * Replaces the current set of macros with a new set
     */    
    public void setMacros(HashMap<String, String> macros) {
        storeSetting(MACROS, macros);
    }    
    
    /**
     * Returns the set of currently defined macros
     */    
    public HashMap<String,String> getMacros() {
         return (HashMap<String,String>)getSetting(MACROS);
    }
    
    /**
     * Deletes all currently defined macros
     */
    public void clearMacros() {
         clearSetting(MACROS);
    } 
    
    public boolean isMacroDefined(String macroname) {
       if (!hasSetting(MACROS)) return false;
       return getMacros().containsKey(macroname);      
    }
    

    
// --------------------------------------------------------------------------------------------
   //private double[] huemap=new double[]{0, 0.1f, 0.15f, 0.3f, 0.48f, 0.56f, 0.65f, 0.76f, 0.82f, 0.9f}; // size=7                               

    private float[] huemap=new float[]{0, 0.15f, 0.65f, 0.3f, 0.48f, 0.76f, 0.09f, 0.56f, 0.82f}; // red, yellow, blue, green, cyan, violet, orange, (blue, pink, 
    private float[] valmap=new float[]{
           1,    1,    1,    1,    1,  
        0.75f, 0.75f, 0.75f, 0.75f, 0.75f,
        0.45f, 0.45f, 0.45f, 0.45f, 0.45f,        
        0.88f, 0.88f, 0.88f, 0.88f, 0.88f,
        0.60f, 0.60f, 0.60f, 0.60f, 0.60f
    }; 
    private float[] satmap=new float[]{
           1, 
        0.75f, 
        0.50f, 
        0.85f, 
        0.65f 
    }; 
            
    /** 
     * Given an integer number, this method returns a "unique" color (among a palette of 250)
     * corresponding to the index
     */
    public Color getColorFromIndex(int i) {
        i=i%250; // only 250 different colors
        int valline=i/huemap.length;
        int valindex=valline%valmap.length;
        float brightness=valmap[valindex];
        int hueindex=(i%huemap.length);
        float hue=huemap[hueindex];

        int satindex=valline%satmap.length;
        float saturation=satmap[satindex];
        return new Color(Color.HSBtoRGB(hue, saturation, brightness));
    }    
    
    /** Imports VisualizationSettings from another object */
    public void importSettings(VisualizationSettings other) {        
         boolean enableCloseUp=isCloseupViewEnabled();  // this should not be restored from session, so I make note of the current value here to restore it later
         boolean showLogo=showMotifLogoInCloseupView(); // this should not be restored from session
         String javascript=getJavascriptSetting();
         String css=getCSSSetting();
         String stylesheet=getStylesheetSetting();
         if (other!=null) { // 'other' should not be NULL, but sometimes it happens?!
             this.colorpointer=other.colorpointer;
             this.clustercolorpointer=other.clustercolorpointer;
             this.settings=other.settings; // import all session settings
             this.datatrackorder=other.datatrackorder;
             this.datatrackorderMasterList=other.datatrackorderMasterList;
             this.selections=(other.selections!=null)?other.selections:new ArrayList<SelectionWindow>();
         }
         setCloseupViewEnabled(enableCloseUp);
         setShowMotifLogoInCloseupView(showLogo);
         setJavascriptSetting(javascript);
         setCSSSetting(css);
         setStylesheetSetting(stylesheet);
    }
    
    /**
     * Reads settings from file and adds them to the current settings
     * Each line in the file should be on the format: setting = value
     * The value will be parsed and converted to the proper type if it 
     * represents a boolean, a number or a color
     * @param file 
     */
    public void importSettingsFromFile(String filenameOrURL) throws Exception {  
        Object input=null;        
        if (filenameOrURL.startsWith("http://") || filenameOrURL.startsWith("https://") || filenameOrURL.startsWith("ftp://")|| filenameOrURL.startsWith("ftps://")) {
            input=new java.net.URL(filenameOrURL); 
        } else {
            input=client.getEngine().getFile(filenameOrURL);                
        }                 
        BufferedReader reader=null;
        try {
            InputStream stream=MotifLabEngine.getInputStreamForDataSource(input);
            reader=new BufferedReader(new InputStreamReader(stream));
            String line;
            while((line=reader.readLine())!=null) {
                line=line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts=line.split("\\s*=\\s*");
                if (parts.length==2) {
                   Object value=StandardDisplaySettingsParser.parseUnknown(parts[1]);
                   storeSetting(parts[0], value);
                } 
            }
        } catch (IOException e) { 
            throw e;
        } finally {
            try {
                if (reader!=null) reader.close();
            } catch (IOException ioe) {}
        }        
    }    
    
    /** Sessions saved in format prior to v4 only supported "solid" gradients, whereas those in v4+ supports transparent gradients
     *  When importing a session in format prior to v4, this method should be called to update the gradients in order to convert them to the new transparent form
     */
    public void updateGradientsFromOlderSession() {
        ArrayList<String> keys=new ArrayList<String>(settings.keySet());
        for (String key:keys) {
            Object value=settings.get(key);
            if (value instanceof ColorGradient && (key.contains(".colorgradient") || key.contains(".secondarycolorgradient"))) {
                Color targetColor=((ColorGradient)value).getForegroundColor();
                ColorGradient newgradient=new ColorGradient(targetColor);
                settings.put(key,newgradient);
            }
        }
    }
       
    /**
     * Returns a hex string representing the given color. E.g. "#FFFF00" for yellow
     * @param color
     * @return
     */
    public static String convertColorToHTMLrepresentation(Color color) {
        String red=Integer.toHexString(color.getRed());
        String green=Integer.toHexString(color.getGreen());
        String blue=Integer.toHexString(color.getBlue());
        if (red.length()==1) red="0"+red;
        if (green.length()==1) green="0"+green;
        if (blue.length()==1) blue="0"+blue;
        return ("#"+red+green+blue).toUpperCase();
    }
    
    /**
     * parses a color in hex string representation (E.g. "#FFFF00" for yellow)
     * and returns the color or null if the string could not be parsed (no notice is given)
     * @param color
     * @return
     */    
    public static Color convertHTMLrepresentationToColor(String colorstring) {
         if (colorstring.startsWith("#")) {
            if (colorstring.length()!=7) return null;
            if (!colorstring.substring(1).matches("[A-Fa-f0-9]+")) return null;
            try {
                int red=Integer.parseInt(colorstring.substring(1, 3), 16); // parse in in radix=16. This can parse hex-numbers!
                int green=Integer.parseInt(colorstring.substring(3, 5), 16); // parse in in radix=16. This can parse hex-numbers!
                int blue=Integer.parseInt(colorstring.substring(5, 7), 16); // parse in in radix=16. This can parse hex-numbers!
                if (red<0 || red>255) return null;
                if (green<0 || green>255) return null;
                if (blue<0 || blue>255) return null;
                return new Color(red, green, blue);
            } catch (NumberFormatException e) {
                return null;
            }
        } else return null;        
    }
    
    /** Returns TRUE if the given color can be said to be "dark"
     *  Text that should be rendered on top of dark backgrounds should be colored WHITE
     *  rather than BLACK
     */
    public static boolean isDark(Color c) {
        int brightness=(int) Math.sqrt(
          c.getRed() * c.getRed() * .241 +
          c.getGreen() * c.getGreen() * .691 +
          c.getBlue() * c.getBlue() * .068);
        return (brightness<140);
    }

    public static boolean isVeryDark(Color c) {
        return (
          c.getRed()<30 &&
          c.getGreen()<30 &&
          c.getBlue()<30
        );
    }
    
    public static Color darker(Color color, double factor) {
        return new Color(Math.max((int)(color.getRed()  *factor), 0),
                         Math.max((int)(color.getGreen()*factor), 0),
                         Math.max((int)(color.getBlue() *factor), 0),
                         color.getAlpha());      
    }

   /** This method implements a different way to return a brighter color than the on inside the Color class
     * (since that does not work for "pure" colors where some channels are 0)
     * @param color
     * @param factor a scaling factor between 0+ and 1.0. Lower numbers lead to more brighter colors.
     */
   public static Color brighter(Color color, float factor) {
      int red=color.getRed();
      int green=color.getGreen();
      int blue=color.getBlue();
      if (red==0 && green==0 && blue==0) return Color.DARK_GRAY; // just to get things started
      if (red==green && red==blue) return color.brighter(); // use this implementation to avoid strange hues for grayscale colors
      float[] hsb=Color.RGBtoHSB(red,green,blue,null);
      if (hsb[2]<1.0) { // increase brightness
          hsb[2]=hsb[2]/factor; // this will increase the value
          if (hsb[2]>1.0) hsb[2]=1.0f;
      } else if (hsb[1]>0) { // if maximum brightness, lower saturation
          hsb[1]=hsb[1]*factor; // 
          if (hsb[1]<0) hsb[1]=0f;    
      }
      return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
   }
   
   public static Color brighter(Color color) {
       return brighter(color, 0.7f);
   }

   public static Color parseColor(String colorstring) throws ParseError {
             if (colorstring.equalsIgnoreCase("BLACK")) return BLACK;
        else if (colorstring.equalsIgnoreCase("RED")) return RED;
        else if (colorstring.equalsIgnoreCase("ORANGE")) return ORANGE;
        else if (colorstring.equalsIgnoreCase("YELLOW")) return YELLOW;
        else if (colorstring.equalsIgnoreCase("LIGHT GREEN")) return LIGHT_GREEN;
        else if (colorstring.equalsIgnoreCase("GREEN")) return GREEN;
        else if (colorstring.equalsIgnoreCase("DARK GREEN")) return DARK_GREEN;      
        else if (colorstring.equalsIgnoreCase("CYAN")) return CYAN;
        else if (colorstring.equalsIgnoreCase("LIGHT BLUE")) return LIGHT_BLUE;
        else if (colorstring.equalsIgnoreCase("BLUE")) return BLUE;
        else if (colorstring.equalsIgnoreCase("VIOLET")) return VIOLET;
        else if (colorstring.equalsIgnoreCase("MAGENTA")) return MAGENTA;
        else if (colorstring.equalsIgnoreCase("PINK")) return PINK;
        else if (colorstring.equalsIgnoreCase("LIGHT BROWN")) return LIGHT_BROWN;
        else if (colorstring.equalsIgnoreCase("DARK BROWN")) return DARK_BROWN;
        else if (colorstring.equalsIgnoreCase("GRAY") || colorstring.equalsIgnoreCase("GREY")) return GRAY;
        else if (colorstring.equalsIgnoreCase("LIGHT GRAY") || colorstring.equalsIgnoreCase("LIGHT GREY")) return LIGHT_GRAY;
        else if (colorstring.equalsIgnoreCase("DARK GRAY") || colorstring.equalsIgnoreCase("DARK GREY")) return DARK_GRAY;
        else if (colorstring.equalsIgnoreCase("WHITE")) return WHITE;
        else if (colorstring.startsWith("#")) {
            if (!(colorstring.length()==7 || colorstring.length()==9)) throw new ParseError("Color value in HEX should consist of a # followed by 6 digits");
            if (!colorstring.substring(1).matches("[A-Fa-f0-9]+")) throw new ParseError("'"+colorstring+"' is not a recognized 6 digit hexadecimal number");
            try {
                int red=Integer.parseInt(colorstring.substring(1, 3), 16); // parse in in radix=16. This can parse hex-numbers!
                int green=Integer.parseInt(colorstring.substring(3, 5), 16); // parse in in radix=16. This can parse hex-numbers!
                int blue=Integer.parseInt(colorstring.substring(5, 7), 16); // parse in in radix=16. This can parse hex-numbers!
                if (red<0 || red>255) throw new ParseError("Color value for RED component should be in the range x00-xFF");
                if (green<0 || green>255) throw new ParseError("Color value for GREEN component should be in the range x00-xFF");
                if (blue<0 || blue>255) throw new ParseError("Color value for BLUE component should be in the range x00-xFF");
                if (colorstring.length()==9) {
                    int alpha=Integer.parseInt(colorstring.substring(7, 9), 16); // parse in in radix=16. This can parse hex-numbers!
                    if (alpha<0 || alpha>255) throw new ParseError("Color value for ALPHA component should be in the range x00-xFF");
                    return new Color(red, green, blue, alpha);
                }
                else return new Color(red, green, blue);
            } catch (NumberFormatException e) {
                 throw new ParseError("'"+colorstring+"' is not a recognized 6 digit hexadecimal number");
            }
        }
        else if (colorstring.indexOf(',')>0) {
          String[] components=colorstring.split("\\s*,\\s*");
          if (!(components.length==3 || components.length==4)) throw new ParseError("Color should be specified by 3 comma-separated integers (RED,GREEN,BLUE) in the range 0-255");
          int red=0;
          int green=0;
          int blue=0;
          try {red=Integer.parseInt(components[0]);} catch (NumberFormatException e) {throw new ParseError("Color should be specified by 3 comma-separated integers (RED,GREEN,BLUE) in the range 0-255");}
          try {green=Integer.parseInt(components[1]);} catch (NumberFormatException e) {throw new ParseError("Color should be specified by 3 comma-separated integers (RED,GREEN,BLUE) in the range 0-255");}
          try {blue=Integer.parseInt(components[2]);} catch (NumberFormatException e) {throw new ParseError("Color should be specified by 3 comma-separated integers (RED,GREEN,BLUE) in the range 0-255");}
          if (red<0 || red>255) throw new ParseError("Color value for RED component should be in the range 0-255");
          if (green<0 || green>255) throw new ParseError("Color value for GREEN component should be in the range 0-255");
          if (blue<0 || blue>255) throw new ParseError("Color value for BLUE component should be in the range 0-255");
          if (components.length==4) {
              try {
                  int alpha=Integer.parseInt(components[3]); //
                  if (alpha<0 || alpha>255) throw new ParseError("Color value for ALPHA component should be in the range 0-255");
                  return new Color(red, green, blue, alpha);
              } catch (NumberFormatException e) {throw new ParseError("Color should be specified by 3 comma-separated integers (RED,GREEN,BLUE) in the range 0-255");}
          }        
          else return new Color(red, green, blue);
        } 
        else if (colorstring.indexOf(':')>0) { // HSV type color
          String[] components=colorstring.split("\\s*:\\s*");
          if (components.length!=3) throw new ParseError("Color should be specified by 3 colon-separated numbers (HUE,SATURATION,BRIGHTNESS) between 0.0 and 1.0");
          float hue=0;
          float saturation=0;
          float brightness=0;
          try {hue=Float.parseFloat(components[0]);} catch (NumberFormatException e) {throw new ParseError("Color should be specified by 3 colon-separated numbers (HUE,SATURATION,BRIGHTNESS) between 0.0 and 1.0");}
          try {saturation=Float.parseFloat(components[1]);} catch (NumberFormatException e) {throw new ParseError("Color should be specified by 3 colon-separated numbers (HUE,SATURATION,BRIGHTNESS) between 0.0 and 1.0");}
          try {brightness=Float.parseFloat(components[2]);} catch (NumberFormatException e) {throw new ParseError("Color should be specified by 3 colon-separated numbers (HUE,SATURATION,BRIGHTNESS) between 0.0 and 1.0");}
          if (hue<0) hue=0f; if (hue>1.0) hue=hue%1.0f; // cycle
          if (saturation<0) saturation=0f; if (saturation>1.0) saturation=1.0f;
          if (brightness<0) brightness=0f; if (brightness>1.0) brightness=1.0f;
          return new Color(Color.HSBtoRGB(hue, saturation, brightness));
        }      
        else throw new ParseError("Not a recognized color: "+colorstring);
   }
   
}
