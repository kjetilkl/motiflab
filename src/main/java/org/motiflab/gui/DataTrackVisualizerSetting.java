/*
 * A small "struct" class for defining DataTrackVisualizer settings
 * (i.e. specific settings that applies to one type of DTV)
 */
package org.motiflab.gui;

import java.util.ArrayList;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.RegionDataset;

/**
 *
 * @author kjetikl
 */
public class DataTrackVisualizerSetting implements Cloneable {
    
    // These numbers are used as bit-flags so they must be powers of 2!
    public static final int ALL=0;
    public static final int MOTIF_TRACK=1;
    public static final int MODULE_TRACK=2;
    public static final int NESTED_TRACK=4;
    public static final int MOTIF_OR_MODULE=MOTIF_TRACK+MODULE_TRACK;    
    public static final int NESTED_OR_MODULE=MODULE_TRACK+NESTED_TRACK;
    
    public static final boolean MAJOR_SETTING=false;
    public static final boolean MINOR_SETTING=true;
    
    public static final boolean SEPARATOR=true; // this is just used as a flag   
    
    private String settingName;
    private String visualizationSettingName;    
    private ArrayList<Object[]> options; // Each element is a pair with key (String) and value (Object)
    private boolean minor=false;
    private int scope=0; // which types of tracks the setting applies to (use static final integers above)
    private int keyStroke=0;
    private boolean useFlag=false;
    private boolean dialog=false;
    private boolean multipleTracks=true; // 
    private String docString=null;
    private boolean is_separator=false; // if TRUE, this setting is just a "mock" setting to insert a separator in the menu

    /**
     * Creates a new DataTrack Visualization Setting with a few attributes. More attributes, including setting options can be added later.
     * @param name The name of the setting (as it will appear in menus)
     * @param visualizationSettingName The name used for this setting when storing it in VisualizationSettings (it should be unique there)
     * @param isMinor Minor features will be grouped together in a submenu whereas major features will appear as their own entries in the top-menu
     * @param scope The types of tracks this setting applies to. Use either ALL or an OR-combination or types, e.g. MOTIF_TRACK|MODULE_TRACK
     * @param shortCutKey an optional key stroke used in the FeaturesPanel to cycle through the options (These are defined in the KeyEvent class)
     */
    public DataTrackVisualizerSetting(String name, String visualizationSettingName, boolean isMinor, int scope, int shortCutKey) {
        this.settingName=name;
        this.visualizationSettingName=visualizationSettingName;
        this.minor=isMinor;
        this.scope=scope;
        this.keyStroke=shortCutKey;
    }
    
    /**
     * Creates a special "separator" setting that will be inserted as a JSeparator in the context menu (or just skipped)
     * @param isMinor Minor features will be grouped together in a submenu whereas major features will appear as their own entries in the top-menu
     * @param scope The types of tracks this setting applies to. Use either ALL or an OR-combination or types, e.g. MOTIF_TRACK|MODULE_TRACK
     */
    public DataTrackVisualizerSetting(boolean isSeparator, boolean isMinor, int scope) {
        this.settingName="-------";
        this.minor=isMinor;
        this.scope=scope;
        this.is_separator=true; // 
    }    
    
    
    public String name() {
        return settingName;
    }
    
    public String getVisualizationSettingName() {
        return visualizationSettingName;
    }
    
    /** Returns a list of allowed options for this setting. 
     *  Each entry is an Object[2] pair where the first element is the option value name (String) 
     *  and the second element is the value
     *  @return a list of allowed options
     */
    public ArrayList<Object[]> getOptions() {
        if (options==null) return new ArrayList<>();
        return options;
    }
    
    /**
     * Minor settings should be grouped together in a context submenu named "Graph Settings" while Major settings should have their own entries
     * at the top level in the context menu
     * @return 
     */
    public boolean isMinorSetting() {
        return minor;
    }
    
    /**
     * @return If TRUE then this setting should use a popup dialog rather than a list of options
     */
    public boolean useDialog() {
        return dialog;
    }
    
    public int shortCutKey() {
        return keyStroke;
    }
    
    /** 
     * @return A documentation string that can be be used as e.g. a tooltip when hovering over the setting in a menu. Can return NULL
     */
    public String getDocumentationString() {
        return docString;
    }  
    
    /** 
     * @param helpString A documentation string that can be be used as e.g. a tooltip when hovering over the setting in a menu
     */
    public void setDocumentationString(String helpString) {
        docString=helpString;
    }       
    
    /** Returns a 'scope' value for this setting. This value holds bit flags */
    public int getScope() {
        return scope;
    }
    
    public boolean appliesToTrack(FeatureDataset track) {
        if (scope==0) return true; // this setting applies to all
        if (track instanceof RegionDataset) {
            if ((scope & MOTIF_TRACK)>0  && ((RegionDataset)track).isMotifTrack()) return true;
            if ((scope & MODULE_TRACK)>0 && ((RegionDataset)track).isModuleTrack()) return true;
            if ((scope & NESTED_TRACK)>0 && ((RegionDataset)track).isNestedTrack()) return true;            
        }
        return false;
    }  
    
    public boolean appliesToTracks(FeatureDataset[] tracks) {
        if (tracks==null || tracks.length==0) return false;        
        if (!multipleTracks && tracks.length!=1) return false; // this setting cannot be applied to multiple tracks at the same time        
        if (scope==0) return true; // this setting applies to all
        for (FeatureDataset track:tracks) {
            if (!appliesToTrack(track)) return false;
        }
        return true;
    }       
    
    /** If this is a boolean setting with only two options, they can either be specified as separate options in a submenu beneath the option (default)
     *  or by using a check mark behind the setting name itself to indicate selected status (True/False). 
     *  @param useFlag Specifies whether or not to render this option as a "flag" (This should only be TRUE for boolean settings)
     */
    public void setBooleanOption(boolean useFlag) {
        this.useFlag=useFlag;
    }
    
    public boolean isFlagOption() {
        return useFlag;
    }
    
    public boolean isSeparator() {
        return is_separator;
    }
    
   /**
    * Specifies whether this setting should display a popup-dialog rather than a list of options
    * @param useDialog If this if TRUE the DTV should return a dialog for this setting with the method dtv.getSettingDialog(SettingName)
    */
    public void setUseDialog(boolean useDialog) {
        this.dialog=useDialog;
    }    
    
   /**
    * Specifies whether this setting should display a popup-dialog rather than a list of options
    * @param allow If this if TRUE then this setting can be applied to multiple selected tracks at the same time. 
    *              If FALSE, the setting can only be applied to a single track
    */
    public void setAllowMultipleTracks(boolean allow) {
        this.multipleTracks=allow;
    }   
    
    /**
     * @return TRUE if this setting can be applied to multiple tracks at the same time
     */
    public boolean allowMultipleTracks() {
        return multipleTracks;
    }      
    
    /** Adds a new option
     *  @param name The name (and value) of the option. If the option already exists it will replace the old option
     */
    public void addOption(String name) {
        if (options==null) options=new ArrayList<>();
        int index=getOptionIndex(name);
        if (index>=0) options.set(index, new Object[]{name,name});
        else options.add(new Object[]{name,name});
    }  
    
    /** Adds a new option that maps to a specific value
     *  @param name The name of the option. If the option already exists it will replace the old option
     *  @param value The corresponding value to be used when this option is selected
     */    
    public void addOption(String name, Object value) {
        if (!(value instanceof Number || value instanceof Boolean || value instanceof String)) throw new IllegalArgumentException("Only Strings, Numbers and Booleans can be used as option values");
        if (options==null) options=new ArrayList<>();
        int index=getOptionIndex(name);     
        if (index>=0) options.set(index, new Object[]{name,value});
        else options.add(new Object[]{name,value});        
    }
    
    /** Adds a new option. If an option already exists with the same name it will replace the old option 
     *  @param option This should be an Object[2] where the first element is a name (String) and the second is the value (Object)
     */    
    public void addOption(Object[] option) {
        if (option==null || option.length!=2) throw new IllegalArgumentException("The option must be an Object[2] consisting of a name and a value");
        Object value=option[1];
        if (!(value instanceof Number || value instanceof Boolean || value instanceof String)) throw new IllegalArgumentException("Only Strings, Numbers and Booleans can be used as option values");        
        if (options==null) options=new ArrayList<>();
        int index=getOptionIndex((String)option[0]);           
        if (index>=0) options.set(index, option);
        else options.add(option);
    }    
    
    private int getOptionIndex(String name) {
        if (options==null || options.isEmpty()) return -1;
        for (int i=0;i<options.size();i++) {
            Object[] option=options.get(i);
            if (option[0]!=null && option[0].equals(name)) return i;
        }
        return -1;
    }
    
    /**
     * Merges options from another setting into this one
     * If the option already exists it will replace the current option
     * @param other 
     */
    public void mergeOptions(DataTrackVisualizerSetting other) {
        ArrayList<Object[]> otheroptions=other.getOptions();
        if (otheroptions==null || otheroptions.isEmpty()) return;
        for (Object[] opt:otheroptions) {
           addOption(opt); 
        }
    }
    
    public Object getValueForOptionName(String optionName) {
        if (options==null || options.isEmpty()) return null;
        for (int i=0;i<options.size();i++) {
            Object[] element=options.get(i);
            String option=(String)element[0]; // this is the option name
            if (option!=null && option.equals(optionName)) return element[1];      
        }
        return null; // option name not recognized    
    }
    
    public Object getNameForOptionValue(Object optionValue) {
        if (options==null || options.isEmpty()) return null;
        for (int i=0;i<options.size();i++) {
            Object[] element=options.get(i);
            Object option=(String)element[1]; // this is the option value
            if (option!=null && option.equals(optionValue)) return element[0];      
        }
        return null; // option value not recognized    
    }    
        
    /** Returns the default option value (i.e. first one) */
    public Object getDefaultOptionValue() {
        if (options==null || options.isEmpty()) return null;
        return options.get(0)[1];
    }
    
    /** Given the current option value for this setting.
     *  This method will return the next (or previous) option value in the list
     *  If the value is not recognized, the first allowed value in the list will be returned
     *  @param current the value of the currently select option
     *  @param previous If TRUE, return the previous value (before the current) instead of the next (after current)
     *  @return The value of the next or previous option
     */
    public Object getNextOptionValue(Object current, boolean previous) {
        if (options==null || options.isEmpty()) return null;
        if (current==null) return options.get(0)[1]; // Default is current value is not recognized
        int offset=(previous)?-1:1;
        for (int i=0;i<options.size();i++) {
            Object optionValue=options.get(i)[1]; // this is the option value
            if (optionValue!=null && optionValue.equals(current)) return getOptionValueNumber(i+offset);            
        }
        return options.get(0)[1]; // Default is current value is not recognized
    }
    
    /** Returns the option value at the specified index (starting at 0). 
      * If the index is greater than or equal to the number of elements, the modulo operator is used to get an index within bounds
      * If the index is negative then the last option value is returned
      * If there are no options, a NULL value is returned
     */
    private Object getOptionValueNumber(int index) {
         if (options==null || options.isEmpty()) return null;     
         if (index>=options.size()) index=index%options.size();
         if (index<0) index=options.size()-1;
         return options.get(index)[1];           
    }

    @Override
    public String toString() {
        String string="DTVsetting["+settingName+"]("+visualizationSettingName+"){"+scope+"}:";
        if (options==null) string+=" No options ";
        else {
           string+=" Options={";
           for (Object[] pair:options) string+=(pair[0]+"=>"+pair[1]+",");  
           string+="}";
        }
        return string;
    }
    
    @Override
    public DataTrackVisualizerSetting clone() {
        DataTrackVisualizerSetting newclone=new DataTrackVisualizerSetting(settingName, visualizationSettingName, minor, scope, keyStroke);
        newclone.useFlag=this.useFlag;
        newclone.dialog=this.dialog;
        newclone.multipleTracks=this.multipleTracks; 
        newclone.docString=this.docString;     
        if (this.options==null) newclone.options=null;
        else {
            newclone.options=new ArrayList<>(this.options.size());
            for (Object[] option:options) {
                newclone.options.add(new Object[]{option[0],option[1]});
            }
        }
        return newclone;
    }
}
