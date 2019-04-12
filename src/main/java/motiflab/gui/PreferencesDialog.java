/*
 * PreferencesDialog.java
 *
 * Created on 12. juni 2009, 12:56
 */

package motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import org.jdesktop.application.Action;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import motiflab.engine.ExecutionError;
import motiflab.engine.ImportantNotification;
import motiflab.engine.datasource.DataLoader;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.SystemError;

/**
 *
 * @author  kjetikl
 */
public class PreferencesDialog extends javax.swing.JDialog {
     VisualizationSettings settings;
     private Preferences guiPreferences;
     private Preferences enginePreferences;
     private MotifLabGUI gui;
     
     private JComboBox<String> fontChooserBox;
     private JSpinner fontSizeSpinner;
     private JCheckBox antialiasCheckbox;
     private JPanel protocolColorsPanel;
     
     
    /** Creates new form PreferencesDialog */
    public PreferencesDialog(MotifLabGUI gui) {
        super(gui.getFrame(), true);
        this.gui=gui;
        this.settings=gui.getVisualizationSettings();
        
        enginePreferences=Preferences.userNodeForPackage(MotifLabEngine.class);
        guiPreferences=Preferences.userNodeForPackage(MotifLabGUI.class);
        initComponents();
        numericTracksValueCombobox.setModel(new DefaultComboBoxModel(new String[]{"Extreme value","Average value","Center value"})); // NOTE: The order of these options must correspond to the numbers of the "static final int" fields from DataTrackVisualizer_Numeric.java
        antialiasModeCombobox.setModel(new DefaultComboBoxModel(MotifLabGUI.getAntialiasModes()));
        setupProtocolEditorPanel();
        String[] installedStylesheets=getInstalledStylesheets();
        htmlSettingsStylesheetCombobox.setModel(new DefaultComboBoxModel(installedStylesheets));
        notificationsLevelCombobox.setModel(new DefaultComboBoxModel(ImportantNotification.getMessageLevels()));
        int windowsize=settings.getSequenceWindowSize();
        int labelsize=settings.getSequenceLabelFixedWidth();
        sequenceWindowSizeSelector.setValue(new Integer(windowsize));
        sequenceLabelSizeSelector.setValue(new Integer(labelsize));
//        antialias_checkbox.setSelected(settings.useTextAntialiasing());

        // From now on I will permanently turn ON antialiasing of text/motifs and not allow users to change this setting
        // antialiasSequencelogos_checkbox.setSelected(settings.useMotifAntialiasing()); // I will use the text antialias setting also for motifs
//        antialias_checkbox.setVisible(false);
//        antialias_label.setVisible(false);
//        antialias_vacantSpaceLabel.setVisible(false);
//        antialias_checkbox.setSelected(true);
        
        boolean useFeatureDataCache=enginePreferences.getBoolean(MotifLabEngine.PREFERENCES_USE_CACHE_FEATUREDATA, true);
        boolean useGeneIDMappingCache=enginePreferences.getBoolean(MotifLabEngine.PREFERENCES_USE_CACHE_GENE_ID_MAPPING, true);
        boolean autoCorrectMotifNames=enginePreferences.getBoolean(MotifLabEngine.PREFERENCES_AUTO_CORRECT_SEQUENCE_NAMES, true);
        int networkTimeout=enginePreferences.getInt(MotifLabEngine.PREFERENCES_NETWORK_TIMEOUT, 25000);
        int maxSequenceLength=enginePreferences.getInt(MotifLabEngine.MAX_SEQUENCE_LENGTH, 0); // 0 means "no limit"
        if (maxSequenceLength==Integer.MAX_VALUE || maxSequenceLength<0) maxSequenceLength=0;
        int maxConcurrentDownloads=enginePreferences.getInt(MotifLabEngine.MAX_CONCURRENT_DOWNLOADS, 10);
        int concurrentThreads=enginePreferences.getInt(MotifLabEngine.CONCURRENT_THREADS, 8); 
        
        
        boolean skipPosition0=guiPreferences.getBoolean(MotifLabGUI.PREFERENCES_SKIP_POSITION_0, true);      
        boolean promptBeforeDiscard=guiPreferences.getBoolean(MotifLabGUI.PREFERENCES_PROMPT_BEFORE_DISCARD, false);
        String autoSaveSession=guiPreferences.get(MotifLabGUI.AUTOSAVE_SESSION_ON_EXIT, "Never");
        int notificationsLevel=guiPreferences.getInt(MotifLabGUI.PREFERENCES_NOTIFICATIONS_MINIMUM_LEVEL,1);
        if (notificationsLevel<1) notificationsLevel=1;
        if (notificationsLevel>notificationsLevelCombobox.getModel().getSize()) notificationsLevel=notificationsLevelCombobox.getModel().getSize();
        boolean scaleLabelsToFit=settings.getScaleSequenceLabelsToFit();      
        int numericTrackDisplayValueSetting=guiPreferences.getInt(VisualizationSettings.NUMERIC_TRACK_DISPLAY_VALUE, 0);
        int numericTrackSamplingCutoff=guiPreferences.getInt(VisualizationSettings.NUMERIC_TRACK_SAMPLING_CUTOFF,100);
        int numericTrackSamplingNumber=guiPreferences.getInt(VisualizationSettings.NUMERIC_TRACK_SAMPLING_NUMBER,20);
        String antialiasMode=guiPreferences.get(MotifLabGUI.PREFERENCES_ANTIALIAS_MODE, "On");
        cacheFeatureDataCheckbox.setSelected(useFeatureDataCache);
        cacheGeneIDCheckbox.setSelected(useGeneIDMappingCache);
        promptBeforeDiscardingCheckbox.setSelected(promptBeforeDiscard);
        autoSaveSessionCombobox.setSelectedItem(autoSaveSession);
        networkTimeoutSpinner.setValue(networkTimeout);
        maxSequenceLengthSpinner.setValue(maxSequenceLength); 
        notificationsLevelCombobox.setSelectedIndex(notificationsLevel-1);
        antialiasModeCombobox.setSelectedItem(antialiasMode);
        scaleToFitLabelCheckbox.setSelected(scaleLabelsToFit);
        sequenceLabelSizeSelector.setEnabled(!scaleLabelsToFit);
        numericTrackSamplingCutoffCombobox.setValue(numericTrackSamplingCutoff);
        numericTrackSamplingNumberCombobox.setValue(numericTrackSamplingNumber);
        numericTracksValueCombobox.setSelectedIndex(numericTrackDisplayValueSetting);
        autoCorrectSequenceNamesCheckbox.setSelected(autoCorrectMotifNames);
        maxConcurrentDownloadsSpinner.setModel(new javax.swing.SpinnerNumberModel(maxConcurrentDownloads, 1, DataLoader.getUpperLimitOnConcurrentDownloads(), 1));       
        concurrentThreadsSpinner.setModel(new javax.swing.SpinnerNumberModel(concurrentThreads, 1, 1000, 1));       
        if (skipPosition0) TSSpositionCombobox.setSelectedItem("+1"); else TSSpositionCombobox.setSelectedItem("+0");
        this.getRootPane().setDefaultButton(OKButton);
        Color bgcolor=settings.getVisualizationPanelBackgroundColor(); 
        backgroundcolorbutton.setBackground(bgcolor);
        backgroundcolorbutton.setOpaque(true);
        String javascript=settings.getJavascriptSetting();
        String css=settings.getCSSSetting();
        String stylesheet=settings.getStylesheetSetting();
        htmlSettingsJavascriptCombobox.setSelectedItem(javascript);
        htmlSettingsCSSCombobox.setSelectedItem(css);
        htmlSettingsStylesheetCombobox.setSelectedItem(stylesheet);
        stylesheetBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseStylesheets();
            }
        });
        pack();
    }

    private void setupProtocolEditorPanel() {
        protocolEditorInternalPanel.setLayout(new BorderLayout());
        JPanel topPanel=new JPanel(new FlowLayout(FlowLayout.LEADING));
        String fontName=guiPreferences.get(ProtocolEditor.FONT_NAME, Font.MONOSPACED);
        int fontSize=guiPreferences.getInt(ProtocolEditor.FONT_SIZE, 12);
        boolean antialias=guiPreferences.getBoolean(ProtocolEditor.ANTIALIAS, false);        
        fontChooserBox=new JComboBox<String>(new String[]{Font.MONOSPACED,Font.SERIF,Font.SANS_SERIF});
        fontChooserBox.setSelectedItem(fontName);
        fontSizeSpinner=new JSpinner(new SpinnerNumberModel(fontSize, 6, 100, 1));
        antialiasCheckbox=new JCheckBox("  Antialias", antialias);
        antialiasCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        topPanel.add(new JLabel("Font"));
        topPanel.add(fontChooserBox);
        topPanel.add(new JLabel("  Size "));
        topPanel.add(fontSizeSpinner);   
        topPanel.add(antialiasCheckbox);
        Object[][] protocolColorClasses=gui.getProtocolEditor().getColorSettingsClasses();
        protocolColorsPanel=new JPanel(new java.awt.GridLayout(0, 3, 20, 16));      
        for (Object[] colorclass:protocolColorClasses) {
            String classname=(String)colorclass[0];
            Color currentcolor=(Color)colorclass[1];
            //protocolEditorInternalPanel.add(new JLabel(classname));
            JButton coloreditbutton=new JButton(classname);
            coloreditbutton.setBackground(currentcolor);
            coloreditbutton.setForeground((VisualizationSettings.isDark(currentcolor))?Color.WHITE:Color.BLACK);
            coloreditbutton.setOpaque(false);
            coloreditbutton.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
            coloreditbutton.setToolTipText("Select new color for "+classname);
            coloreditbutton.addActionListener(new ColorButtonListener(coloreditbutton));
            protocolColorsPanel.add(coloreditbutton);
        }
        protocolColorsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(12, 0, 0, 0), 
                BorderFactory.createTitledBorder("Colors"))
                );
        protocolEditorInternalPanel.add(topPanel,BorderLayout.NORTH);        
        protocolEditorInternalPanel.add(protocolColorsPanel,BorderLayout.CENTER);
    }



    private void browseStylesheets() {
        final JFileChooser fc = gui.getFileChooser(null);// new JFileChooser(gui.getLastUsedDirectory());
        FileNameExtensionFilter filter=new FileNameExtensionFilter("Cascading Style Sheet (*.css)", "css");
        fc.addChoosableFileFilter(filter);
        fc.setFileFilter(filter);
        fc.setDialogTitle("Import Stylesheet");  
        int returnValue=fc.showOpenDialog(this);
        if (returnValue!=JFileChooser.APPROVE_OPTION) return; // user cancelled
        File file=fc.getSelectedFile();
        htmlSettingsStylesheetCombobox.setSelectedItem(file.getAbsolutePath()); 
        gui.setLastUsedDirectory(file.getParentFile());        
    }
    
    
    /**
     * This method (which is called when the user clicks the "Apply" or "OK" buttons
     * will set the preferences according to the selections in the dialog
     * @return 
     */
    public boolean applyChanges() {
        int windowsize=((Integer)sequenceWindowSizeSelector.getValue()).intValue();
        int labelsize=((Integer)sequenceLabelSizeSelector.getValue()).intValue();
        boolean useAntialias=true; // antialias_checkbox.isSelected();
        boolean useMotifAntialias=true; // antialias_checkbox.isSelected();     // In the future I will use one combined setting for antialiasing
        boolean scaleLabelsToFit=scaleToFitLabelCheckbox.isSelected(); // In the future I will use one combined setting for antialiasing
        int numericTrackSamplingCutoff=((Integer)numericTrackSamplingCutoffCombobox.getValue()).intValue();
        int numericTrackSamplingNumber=((Integer)numericTrackSamplingNumberCombobox.getValue()).intValue();        
        int numericTrackValueSetting=numericTracksValueCombobox.getSelectedIndex();
        int notificationsLevel=notificationsLevelCombobox.getSelectedIndex()+1;
        String antialiasMode=(String)antialiasModeCombobox.getSelectedItem();
        guiPreferences.putInt(MotifLabGUI.PREFERENCES_NOTIFICATIONS_MINIMUM_LEVEL, notificationsLevel);        
        settings.setSequenceLabelFixedWidth(labelsize);
        guiPreferences.putInt(VisualizationSettings.SEQUENCE_LABEL_FIXED_WIDTH, labelsize);
        settings.setScaleSequenceLabelsToFit(scaleLabelsToFit); // this is persistant in VisualizationSettings
        guiPreferences.putBoolean(VisualizationSettings.SCALE_SEQUENCE_LABELS_TO_FIT, scaleLabelsToFit);        
        settings.setUseTextAntialiasing(useAntialias);
        guiPreferences.putBoolean(VisualizationSettings.ANTIALIAS_TEXT, useAntialias);        
        settings.setUseMotifAntialiasing(useMotifAntialias);
        guiPreferences.putBoolean(VisualizationSettings.ANTIALIAS_MOTIFS, useMotifAntialias);        
        guiPreferences.putInt(VisualizationSettings.SEQUENCE_WINDOW_SIZE, windowsize);
        if (windowsize!=settings.getSequenceWindowSize()) {
            settings.setSequenceWindowSize(windowsize);
            //settings.getGUI().getVisualizationPanel().realignSequences();
        }            
        guiPreferences.putInt(VisualizationSettings.NUMERIC_TRACK_SAMPLING_CUTOFF, numericTrackSamplingCutoff);
        DataTrackVisualizer_Numeric.sampling_length_cutoff=numericTrackSamplingCutoff;
        guiPreferences.putInt(VisualizationSettings.NUMERIC_TRACK_SAMPLING_NUMBER, numericTrackSamplingNumber);
        DataTrackVisualizer_Numeric.sampling_size=numericTrackSamplingNumber;
        guiPreferences.putInt(VisualizationSettings.NUMERIC_TRACK_DISPLAY_VALUE, numericTrackValueSetting);
        DataTrackVisualizer_Numeric.display_value_setting=numericTrackValueSetting;
        guiPreferences.put(MotifLabGUI.PREFERENCES_ANTIALIAS_MODE, antialiasMode);
        settings.getGUI().setAntialiasMode(antialiasMode);
        
        enginePreferences.putBoolean(MotifLabEngine.PREFERENCES_USE_CACHE_FEATUREDATA, cacheFeatureDataCheckbox.isSelected());
        settings.getEngine().getDataLoader().setUseCache(cacheFeatureDataCheckbox.isSelected());
        enginePreferences.putBoolean(MotifLabEngine.PREFERENCES_USE_CACHE_GENE_ID_MAPPING, cacheGeneIDCheckbox.isSelected());        
        settings.getEngine().getGeneIDResolver().setUseCache(cacheGeneIDCheckbox.isSelected());
        enginePreferences.putBoolean(MotifLabEngine.PREFERENCES_AUTO_CORRECT_SEQUENCE_NAMES, autoCorrectSequenceNamesCheckbox.isSelected());        
        settings.getEngine().setAutoCorrectSequenceNames(autoCorrectSequenceNamesCheckbox.isSelected());
        
        enginePreferences.putInt(MotifLabEngine.PREFERENCES_NETWORK_TIMEOUT, (Integer)networkTimeoutSpinner.getValue());
        settings.getEngine().setNetworkTimeout((Integer)networkTimeoutSpinner.getValue());
        int maxSeqLength=(Integer)maxSequenceLengthSpinner.getValue();
        if (maxSeqLength==Integer.MAX_VALUE || maxSeqLength<0) maxSeqLength=0;
        enginePreferences.putInt(MotifLabEngine.MAX_SEQUENCE_LENGTH,maxSeqLength);
        settings.getEngine().setMaxSequenceLength(maxSeqLength);
        boolean skip0=(((String)TSSpositionCombobox.getSelectedItem()).equals("+1"));
        guiPreferences.putBoolean(MotifLabGUI.PREFERENCES_SKIP_POSITION_0, skip0);        
        gui.setSkipPosition0(skip0);
                       
        int oldMaxConcurrentDownloads=enginePreferences.getInt(MotifLabEngine.MAX_CONCURRENT_DOWNLOADS, 10);        
        int newMaxConcurrentDownloads=(Integer)maxConcurrentDownloadsSpinner.getValue();
        if (newMaxConcurrentDownloads!=oldMaxConcurrentDownloads) {
           try {
               // if (!gui.getScheduler().isIdle()) throw new SystemError("The 'Maximum Concurrent Downloads' setting can not be changed while tasks are executing");
               settings.getEngine().getDataLoader().setConcurrentDownloads(newMaxConcurrentDownloads);
               enginePreferences.putInt(MotifLabEngine.MAX_CONCURRENT_DOWNLOADS, (Integer)newMaxConcurrentDownloads);               
           } catch (SystemError e) {
               JOptionPane.showMessageDialog(PreferencesDialog.this, e.getMessage(), "Preferences Error", JOptionPane.ERROR_MESSAGE);
               return false;
           }
        }
        int newConcurrentThreads=(Integer)concurrentThreadsSpinner.getValue(); 
        settings.getEngine().setConcurrentThreads(newConcurrentThreads);
        enginePreferences.putInt(MotifLabEngine.CONCURRENT_THREADS, (Integer)newConcurrentThreads);        
        guiPreferences.putBoolean(MotifLabGUI.PREFERENCES_PROMPT_BEFORE_DISCARD, promptBeforeDiscardingCheckbox.isSelected());        
        gui.setPromptBeforeDiscard(promptBeforeDiscardingCheckbox.isSelected());
        guiPreferences.put(MotifLabGUI.AUTOSAVE_SESSION_ON_EXIT, (String)autoSaveSessionCombobox.getSelectedItem());        
        gui.setAutoSaveSessionSetting((String)autoSaveSessionCombobox.getSelectedItem());        

        String javascript=(String)htmlSettingsJavascriptCombobox.getSelectedItem();
        settings.setJavascriptSetting(javascript);
        guiPreferences.put(VisualizationSettings.JAVASCRIPT, javascript);
        String css=(String)htmlSettingsCSSCombobox.getSelectedItem();
        settings.setCSSSetting(css);
        guiPreferences.put(VisualizationSettings.CSS, css);
        String stylesheet=(String)htmlSettingsStylesheetCombobox.getSelectedItem();
        settings.setStylesheetSetting(stylesheet);
        guiPreferences.put(VisualizationSettings.STYLESHEET, stylesheet);

        Color newColor=backgroundcolorbutton.getBackground();
        setColorSetting(guiPreferences, VisualizationSettings.MAINPANEL_BACKGROUND, newColor);
        settings.setVisualizationPanelBackgroundColor(new Color(newColor.getRed(),newColor.getGreen(),newColor.getBlue()));
       
        String protocolEditorFontName=(String)fontChooserBox.getSelectedItem();
        int protocolEditorFontSize=(Integer)fontSizeSpinner.getValue();
        boolean protocolEditorAntialias=antialiasCheckbox.isSelected();
        guiPreferences.put(ProtocolEditor.FONT_NAME,protocolEditorFontName);
        guiPreferences.putInt(ProtocolEditor.FONT_SIZE, protocolEditorFontSize);
        guiPreferences.putBoolean(ProtocolEditor.ANTIALIAS,protocolEditorAntialias);
        gui.getProtocolEditor().setFont(protocolEditorFontName,protocolEditorFontSize);
        gui.getProtocolEditor().setAntialias(protocolEditorAntialias);
        gui.getProtocolEditor().updateProtocolRendering();
        applyProtocolColors();
        gui.redraw();        
        return true;
    }
    
    /**
     * This method can be used from anywhere to set a preference
     */
    public static void setOption(String settingname, Object value, MotifLabEngine engine) throws ExecutionError {
        if (value==null || settingname==null) return;
        Object[] settings=getOptionSettings(settingname);       
        if (settings==null) throw new ExecutionError("Unrecognized option: "+settingname);
        boolean isProtocolColor=settingname.startsWith("ProtocolColor:");
        if (isProtocolColor) settingname=settingname.substring("ProtocolColor:".length());
        Class packageNode=(Class)settings[0];
        Class expectedType=(Class)settings[1];
        Object useValue=checkAndReturnValue(value,expectedType);
        // just some hacks
        if (settingname.equals(VisualizationSettings.NUMERIC_TRACK_DISPLAY_VALUE) && value instanceof String) {
            if (value.toString().toLowerCase().startsWith("extreme")) useValue=new Integer(0);
            else if (value.toString().toLowerCase().startsWith("average")) useValue=new Integer(1);
            else if (value.toString().toLowerCase().startsWith("cent") || value.toString().toLowerCase().startsWith("middle")) useValue=new Integer(2);
        }
        if (useValue==null) throw new ExecutionError("Option '"+settingname+"' can not be set to '"+value+"'. (Expected "+expectedType.getSimpleName().toLowerCase()+" value not "+value.getClass().getSimpleName().toLowerCase()+").");
        
        // first store the new preferences
        Preferences preferences=Preferences.userNodeForPackage(packageNode);
             if (useValue instanceof Integer) preferences.putInt(settingname, (Integer)useValue);
        else if (useValue instanceof Double) preferences.putDouble(settingname, (Double)useValue);
        else if (useValue instanceof Boolean) preferences.putBoolean(settingname, (Boolean)useValue);
        else if (useValue instanceof String) preferences.put(settingname, (String)useValue);
        else if (useValue instanceof Color) setColorSetting(preferences, settingname, (Color)useValue);
        
        // then update the "live" value   
        if (engine==null) return;
        MotifLabGUI gui=null;
        if (engine.getClient() instanceof MotifLabGUI) gui=(MotifLabGUI)engine.getClient();
        
        if (settingname.equals(MotifLabEngine.PREFERENCES_USE_CACHE_FEATUREDATA)) engine.getDataLoader().setUseCache((Boolean)useValue);
        if (settingname.equals(MotifLabEngine.PREFERENCES_USE_CACHE_GENE_ID_MAPPING)) engine.getGeneIDResolver().setUseCache((Boolean)useValue);
        if (settingname.equals(MotifLabEngine.PREFERENCES_AUTO_CORRECT_SEQUENCE_NAMES)) engine.setAutoCorrectSequenceNames((Boolean)useValue);
        if (settingname.equals(MotifLabEngine.PREFERENCES_NETWORK_TIMEOUT)) engine.setNetworkTimeout((Integer)useValue);
        if (settingname.equals(MotifLabEngine.MAX_SEQUENCE_LENGTH)) engine.setMaxSequenceLength((Integer)useValue);
        if (settingname.equals(MotifLabEngine.CONCURRENT_THREADS)) engine.setConcurrentThreads((Integer)useValue);    
        if (settingname.equals(MotifLabEngine.MAX_CONCURRENT_DOWNLOADS)) try {
               engine.getDataLoader().setConcurrentDownloads((Integer)useValue);
           } catch (SystemError e) {
               engine.logMessage(e.getMessage());
           }

        if (gui==null) return;       
        if (settingname.equals(MotifLabGUI.PREFERENCES_SKIP_POSITION_0)) gui.setSkipPosition0((Boolean)useValue);
        if (settingname.equals(MotifLabGUI.PREFERENCES_PROMPT_BEFORE_DISCARD)) gui.setPromptBeforeDiscard((Boolean)useValue);
        if (settingname.equals(MotifLabGUI.PREFERENCES_ANTIALIAS_MODE)) gui.setAntialiasMode((String)useValue);
        if (settingname.equals(MotifLabGUI.AUTOSAVE_SESSION_ON_EXIT)) gui.setAutoSaveSessionSetting((String)useValue);
        // if (settingname.equals(MotifLabGUI.PREFERENCES_NOTIFICATIONS_MINIMUM_LEVEL)) - The GUI just queries the preferences so there is no need to set anything
        
        if (settingname.equals(VisualizationSettings.NUMERIC_TRACK_DISPLAY_VALUE)) DataTrackVisualizer_Numeric.display_value_setting=(Integer)useValue;
        if (settingname.equals(VisualizationSettings.NUMERIC_TRACK_SAMPLING_CUTOFF)) DataTrackVisualizer_Numeric.sampling_length_cutoff=(Integer)useValue;
        if (settingname.equals(VisualizationSettings.NUMERIC_TRACK_SAMPLING_NUMBER)) DataTrackVisualizer_Numeric.sampling_size=(Integer)useValue;
        if (settingname.equals(VisualizationSettings.SEQUENCE_LABEL_FIXED_WIDTH)) gui.getVisualizationSettings().setSequenceLabelFixedWidth((Integer)useValue);
        if (settingname.equals(VisualizationSettings.ANTIALIAS_TEXT)) gui.getVisualizationSettings().setUseTextAntialiasing((Boolean)useValue);
        if (settingname.equals(VisualizationSettings.SCALE_SEQUENCE_LABELS_TO_FIT)) gui.getVisualizationSettings().setScaleSequenceLabelsToFit((Boolean)useValue);
        if (settingname.equals(VisualizationSettings.MAINPANEL_BACKGROUND)) gui.getVisualizationSettings().setVisualizationPanelBackgroundColor((Color)useValue);
        if (settingname.equals(VisualizationSettings.JAVASCRIPT)) gui.getVisualizationSettings().setJavascriptSetting((String)useValue);
        if (settingname.equals(VisualizationSettings.CSS)) gui.getVisualizationSettings().setCSSSetting((String)useValue);
        if (settingname.equals(VisualizationSettings.STYLESHEET)) gui.getVisualizationSettings().setStylesheetSetting((String)useValue);   
        if (settingname.equals(VisualizationSettings.SEQUENCE_WINDOW_SIZE)) {
            gui.getVisualizationSettings().setSequenceWindowSize((Integer)useValue);
            gui.getVisualizationPanel().realignSequences();
        }     

        if (settingname.equals(ProtocolEditor.FONT_NAME)) {gui.getProtocolEditor().setFont((String)useValue);gui.getProtocolEditor().updateProtocolRendering();}
        if (settingname.equals(ProtocolEditor.FONT_SIZE)) {gui.getProtocolEditor().setFontSize((Integer)useValue);gui.getProtocolEditor().updateProtocolRendering();}
        if (settingname.equals(ProtocolEditor.ANTIALIAS)) gui.getProtocolEditor().setAntialias((Boolean)useValue);
        if (isProtocolColor) gui.getProtocolEditor().setupProtocolColors();     
        gui.redraw();
    }
    
    /** If the given value can be coerced into a value of the expected class
     *  the new value will be returned, else NULL will be returned
     */
    private static Object checkAndReturnValue(Object value, Class expected) {
        if (value==null) return null;
        if (value instanceof Integer && expected==Integer.class) return value;
        if (value instanceof Integer && expected==Double.class) return new Double(((Integer)value).doubleValue());
        if (value instanceof Double && expected==Double.class) return value;
        if (value instanceof Double && expected==Integer.class) return new Integer(((Double)value).intValue());
        if (value instanceof Boolean && expected==Boolean.class) return value;
        if (value instanceof Color && expected==Color.class) return value;
        if (expected==String.class) return value.toString();
        return null;
    }

    private void applyProtocolColors() {
        Preferences protocolPreferences=Preferences.userNodeForPackage(ProtocolEditor.class);
        for (Component comp:protocolColorsPanel.getComponents()) {
            if (comp instanceof JButton) {
                String classname=((JButton)comp).getText();
                Color classcolor=((JButton)comp).getBackground();
                setColorSetting(protocolPreferences, classname, classcolor);
            }
        }
        gui.getProtocolEditor().setupProtocolColors();
    }
    
    /**
     * Given the name of a valid setting, this method will return an Object array
     * containing the class that is associated with the Preferences object
     * and a class denoting the expected type of the value
     * @param name
     * @return 
     */
    public static Object[] getOptionSettings(String name) {
        if (name.equals(MotifLabEngine.PREFERENCES_USE_CACHE_FEATUREDATA)) return new Object[]{MotifLabEngine.class,Boolean.class};
        if (name.equals(MotifLabEngine.PREFERENCES_USE_CACHE_GENE_ID_MAPPING)) return new Object[]{MotifLabEngine.class,Boolean.class};
        if (name.equals(MotifLabEngine.PREFERENCES_AUTO_CORRECT_SEQUENCE_NAMES)) return new Object[]{MotifLabEngine.class,Boolean.class};
        if (name.equals(MotifLabEngine.PREFERENCES_NETWORK_TIMEOUT)) return new Object[]{MotifLabEngine.class,Integer.class};
        if (name.equals(MotifLabEngine.MAX_SEQUENCE_LENGTH)) return new Object[]{MotifLabEngine.class,Integer.class};
        if (name.equals(MotifLabEngine.MAX_CONCURRENT_DOWNLOADS)) return new Object[]{MotifLabEngine.class,Integer.class};
        if (name.equals(MotifLabEngine.CONCURRENT_THREADS)) return new Object[]{MotifLabEngine.class,Integer.class};
            
        if (name.equals(MotifLabGUI.PREFERENCES_SKIP_POSITION_0)) return new Object[]{MotifLabGUI.class,Boolean.class};
        if (name.equals(MotifLabGUI.PREFERENCES_PROMPT_BEFORE_DISCARD)) return new Object[]{MotifLabGUI.class,Boolean.class};
        if (name.equals(MotifLabGUI.PREFERENCES_ANTIALIAS_MODE)) return new Object[]{MotifLabGUI.class,String.class};
        if (name.equals(MotifLabGUI.AUTOSAVE_SESSION_ON_EXIT)) return new Object[]{MotifLabGUI.class,String.class};
        if (name.equals(MotifLabGUI.PREFERENCES_NOTIFICATIONS_MINIMUM_LEVEL)) return new Object[]{MotifLabGUI.class,Integer.class};
        
        if (name.equals(VisualizationSettings.NUMERIC_TRACK_DISPLAY_VALUE)) return new Object[]{MotifLabGUI.class,Integer.class};
        if (name.equals(VisualizationSettings.NUMERIC_TRACK_SAMPLING_CUTOFF)) return new Object[]{MotifLabGUI.class,Integer.class};
        if (name.equals(VisualizationSettings.NUMERIC_TRACK_SAMPLING_NUMBER)) return new Object[]{MotifLabGUI.class,Integer.class};
        if (name.equals(VisualizationSettings.SEQUENCE_LABEL_FIXED_WIDTH)) return new Object[]{MotifLabGUI.class,Integer.class};
        if (name.equals(VisualizationSettings.SEQUENCE_WINDOW_SIZE)) return new Object[]{MotifLabGUI.class,Integer.class};
        if (name.equals(VisualizationSettings.ANTIALIAS_TEXT)) return new Object[]{MotifLabGUI.class,Boolean.class};
        if (name.equals(VisualizationSettings.SCALE_SEQUENCE_LABELS_TO_FIT)) return new Object[]{MotifLabGUI.class,Boolean.class};
        if (name.equals(VisualizationSettings.MAINPANEL_BACKGROUND)) return new Object[]{MotifLabGUI.class,Color.class};                 
        if (name.equals(VisualizationSettings.JAVASCRIPT)) return new Object[]{MotifLabGUI.class,String.class};
        if (name.equals(VisualizationSettings.CSS)) return new Object[]{MotifLabGUI.class,String.class};
        if (name.equals(VisualizationSettings.STYLESHEET)) return new Object[]{MotifLabGUI.class,String.class};
        
        if (name.equals(ProtocolEditor.FONT_NAME)) return new Object[]{MotifLabGUI.class,String.class};
        if (name.equals(ProtocolEditor.FONT_SIZE)) return new Object[]{MotifLabGUI.class,Integer.class};
        if (name.equals(ProtocolEditor.ANTIALIAS)) return new Object[]{MotifLabGUI.class,Boolean.class};
       
        if (name.startsWith("ProtocolColor:")) return new Object[]{ProtocolEditor.class,Color.class};
        
        return null; // unknown setting
            
    }

    /** Restores the visualization settings that can be set in Preferences to the chosen values (in the preferences dialog) 
     *  This method is used on "session restore", to change the settings back to the values currently chosen in Preferences
     *  rather than the settings saved with the session
     */
    public static void restoreVisualizationSettings(MotifLabGUI gui) {
        VisualizationSettings settings=gui.getVisualizationSettings();
        Preferences guiPreferences=Preferences.userNodeForPackage(MotifLabGUI.class);
        int windowsize=guiPreferences.getInt(VisualizationSettings.SEQUENCE_WINDOW_SIZE, settings.getSequenceWindowSize());
        if (windowsize!=settings.getSequenceWindowSize()) {
            settings.setSequenceWindowSize(windowsize);
            settings.getGUI().getVisualizationPanel().realignSequences();
        }   
        settings.setSequenceLabelFixedWidth(guiPreferences.getInt(VisualizationSettings.SEQUENCE_LABEL_FIXED_WIDTH, settings.getSequenceLabelFixedWidth()));
        settings.setScaleSequenceLabelsToFit(guiPreferences.getBoolean(VisualizationSettings.SCALE_SEQUENCE_LABELS_TO_FIT, settings.getScaleSequenceLabelsToFit()));
        settings.setUseTextAntialiasing(guiPreferences.getBoolean(VisualizationSettings.ANTIALIAS_TEXT, settings.useTextAntialiasing()));
        settings.setUseMotifAntialiasing(guiPreferences.getBoolean(VisualizationSettings.ANTIALIAS_MOTIFS, settings.useMotifAntialiasing()));
        Color bgcolor=settings.getVisualizationPanelBackgroundColor();
        settings.setVisualizationPanelBackgroundColor(getColorSetting(guiPreferences,VisualizationSettings.MAINPANEL_BACKGROUND,bgcolor));
        settings.setJavascriptSetting(guiPreferences.get(VisualizationSettings.JAVASCRIPT, settings.getJavascriptSetting()));
        settings.setCSSSetting(guiPreferences.get(VisualizationSettings.CSS, settings.getCSSSetting()));
        settings.setStylesheetSetting(guiPreferences.get(VisualizationSettings.STYLESHEET, settings.getStylesheetSetting()));
    }

    public static Color getColorSetting(Preferences pref, String settingName, Color defaultcolor) {
        int red=pref.getInt(settingName+".R", defaultcolor.getRed()); // colors can not be stored directly in preferences, so they must be split into integer components
        int green=pref.getInt(settingName+".G", defaultcolor.getGreen());
        int blue=pref.getInt(settingName+".B", defaultcolor.getBlue());
        return new Color(red,green,blue);
    }

    public static void setColorSetting(Preferences pref, String settingName, Color c) {
        pref.putInt(settingName+".R",c.getRed()); // colors can not be stored directly in preferences, so they must be split into integer components
        pref.putInt(settingName+".G",c.getGreen());
        pref.putInt(settingName+".B",c.getBlue());
    }


    /** Returns a list of installed stylesheets. Each name is enclosed in brackets */
    private String[] getInstalledStylesheets() {
        File workdir=new File(gui.getEngine().getMotifLabDirectory());
        if (workdir.isDirectory()) {
            File[] list = workdir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".css");
                }
            });
            String[] names=new String[list.length];
            for (int i=0;i<list.length;i++) {
                String filename=list[i].getName();
                filename=filename.substring(0,filename.lastIndexOf('.'));
                names[i]="["+filename+"]";
            }
            return names;
        } else return new String[0];
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonsPanel = new javax.swing.JPanel();
        OKButton = new javax.swing.JButton();
        applyButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        tabbedPane = new javax.swing.JTabbedPane();
        generalSettingsPanel = new javax.swing.JPanel();
        generalSettingsInternalPanel = new javax.swing.JPanel();
        concurrentThreadsLabel = new javax.swing.JLabel();
        concurrentThreadsSpinner = new javax.swing.JSpinner();
        maxConcurrentDownloadsLabel = new javax.swing.JLabel();
        maxConcurrentDownloadsSpinner = new javax.swing.JSpinner();
        networkTimeoutLabel = new javax.swing.JLabel();
        networkTimeoutSpinner = new javax.swing.JSpinner();
        sequenceLengthLabel = new javax.swing.JLabel();
        maxSequenceLengthSpinner = new javax.swing.JSpinner();
        autoCorrectSequenceNamesLabel = new javax.swing.JLabel();
        autoCorrectSequenceNamesCheckbox = new javax.swing.JCheckBox();
        guiSettingsPanel = new javax.swing.JPanel();
        guiSettingsInternalPanel = new javax.swing.JPanel();
        autoSaveSessionLabel = new javax.swing.JLabel();
        autoSaveSessionCombobox = new javax.swing.JComboBox();
        notificationsLabel = new javax.swing.JLabel();
        notificationsLevelCombobox = new javax.swing.JComboBox();
        antialiasModeLabel = new javax.swing.JLabel();
        antialiasModeCombobox = new javax.swing.JComboBox();
        TSSpositionLabel = new javax.swing.JLabel();
        TSSpositionCombobox = new javax.swing.JComboBox();
        promptSaveLabel = new javax.swing.JLabel();
        promptBeforeDiscardingCheckbox = new javax.swing.JCheckBox();
        visualizationSettingsPanel = new javax.swing.JPanel();
        vizualizationSettingsInternalPanel = new javax.swing.JPanel();
        sequenceWindowSizeLabel = new javax.swing.JLabel();
        sequenceWindowSizeSelector = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        sequenceLabelSizeLabel = new javax.swing.JLabel();
        sequenceLabelSizeSelector = new javax.swing.JSpinner();
        scaleToFitLabelCheckbox = new javax.swing.JCheckBox();
        numericTracksLabel = new javax.swing.JLabel();
        numericTracksValueCombobox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        numericTrackSamplingCutoffCombobox = new javax.swing.JSpinner();
        numericTrackSamplingNumberCombobox = new javax.swing.JSpinner();
        backgroundcolorlabel = new javax.swing.JLabel();
        backgroundcolorbutton = new javax.swing.JButton();
        resetbgcolorbutton = new javax.swing.JButton();
        cacheSettingsPanel = new javax.swing.JPanel();
        cacheSettingsInternalPanel = new javax.swing.JPanel();
        cacheFeatureDataLabel = new javax.swing.JLabel();
        cacheFeatureDataCheckbox = new javax.swing.JCheckBox();
        clearFeatureDataCacheButton = new javax.swing.JButton();
        cacheGeneIdDataLabel = new javax.swing.JLabel();
        cacheGeneIDCheckbox = new javax.swing.JCheckBox();
        clearGeneIDCacheButton = new javax.swing.JButton();
        protocolEditorPanel = new javax.swing.JPanel();
        protocolEditorInternalPanel = new javax.swing.JPanel();
        htmlSettingsPanel = new javax.swing.JPanel();
        htmlSettingsInternalPanel = new javax.swing.JPanel();
        htmlSettingsJavascriptLabel = new javax.swing.JLabel();
        htmlSettingsJavascriptCombobox = new javax.swing.JComboBox();
        htmlSettingsEmptyLabel1 = new javax.swing.JLabel();
        htmlSettingsCSSLabel = new javax.swing.JLabel();
        htmlSettingsCSSCombobox = new javax.swing.JComboBox();
        htmlSettingsEmptyLabel2 = new javax.swing.JLabel();
        htmlSettingsStylesheetLabel = new javax.swing.JLabel();
        htmlSettingsStylesheetCombobox = new javax.swing.JComboBox();
        stylesheetBrowseButton = new javax.swing.JButton();
        otherSettingsPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        editStartupScriptButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        viewDisplaySettingsButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getResourceMap(PreferencesDialog.class);
        setTitle(resourceMap.getString("Form.title")); // NOI18N
        setName("Form"); // NOI18N

        buttonsPanel.setMaximumSize(new java.awt.Dimension(32767, 40));
        buttonsPanel.setMinimumSize(new java.awt.Dimension(100, 40));
        buttonsPanel.setName("buttonsPanel"); // NOI18N
        buttonsPanel.setPreferredSize(new java.awt.Dimension(100, 36));
        buttonsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 5));

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(motiflab.gui.MotifLabApp.class).getContext().getActionMap(PreferencesDialog.class, this);
        OKButton.setAction(actionMap.get("OKButtonPressed")); // NOI18N
        OKButton.setText(resourceMap.getString("OKButton.text")); // NOI18N
        OKButton.setMaximumSize(new java.awt.Dimension(75, 27));
        OKButton.setMinimumSize(new java.awt.Dimension(75, 27));
        OKButton.setName("OKButton"); // NOI18N
        OKButton.setOpaque(false);
        OKButton.setPreferredSize(new java.awt.Dimension(75, 27));
        buttonsPanel.add(OKButton);

        applyButton.setAction(actionMap.get("applyButtonPressed")); // NOI18N
        applyButton.setText(resourceMap.getString("applyButton.text")); // NOI18N
        applyButton.setMaximumSize(new java.awt.Dimension(75, 27));
        applyButton.setMinimumSize(new java.awt.Dimension(75, 27));
        applyButton.setName("applyButton"); // NOI18N
        applyButton.setPreferredSize(new java.awt.Dimension(75, 27));
        buttonsPanel.add(applyButton);

        cancelButton.setAction(actionMap.get("cancelButtonPressed")); // NOI18N
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 27));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 27));
        cancelButton.setName("cancelButton"); // NOI18N
        cancelButton.setPreferredSize(new java.awt.Dimension(75, 27));
        buttonsPanel.add(cancelButton);

        getContentPane().add(buttonsPanel, java.awt.BorderLayout.PAGE_END);

        tabbedPane.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        tabbedPane.setName("tabbedPane"); // NOI18N

        generalSettingsPanel.setName("generalSettingsPanel"); // NOI18N
        generalSettingsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        generalSettingsInternalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 10, 10));
        generalSettingsInternalPanel.setName("generalSettingsInternalPanel"); // NOI18N
        generalSettingsInternalPanel.setLayout(new java.awt.GridLayout(6, 2, 20, 16));

        concurrentThreadsLabel.setText(resourceMap.getString("concurrentThreadsLabel.text")); // NOI18N
        concurrentThreadsLabel.setName("concurrentThreadsLabel"); // NOI18N
        generalSettingsInternalPanel.add(concurrentThreadsLabel);

        concurrentThreadsSpinner.setModel(new javax.swing.SpinnerNumberModel(4, 1, 1000, 1));
        concurrentThreadsSpinner.setName("concurrentThreadsSpinner"); // NOI18N
        concurrentThreadsSpinner.setRequestFocusEnabled(false);
        generalSettingsInternalPanel.add(concurrentThreadsSpinner);

        maxConcurrentDownloadsLabel.setText(resourceMap.getString("maxConcurrentDownloadsLabel.text")); // NOI18N
        maxConcurrentDownloadsLabel.setName("maxConcurrentDownloadsLabel"); // NOI18N
        generalSettingsInternalPanel.add(maxConcurrentDownloadsLabel);

        maxConcurrentDownloadsSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 20, 1));
        maxConcurrentDownloadsSpinner.setToolTipText(resourceMap.getString("maxConcurrentDownloadsSpinner.toolTipText")); // NOI18N
        maxConcurrentDownloadsSpinner.setName("maxConcurrentDownloadsSpinner"); // NOI18N
        generalSettingsInternalPanel.add(maxConcurrentDownloadsSpinner);

        networkTimeoutLabel.setText(resourceMap.getString("networkTimeoutLabel.text")); // NOI18N
        networkTimeoutLabel.setName("networkTimeoutLabel"); // NOI18N
        generalSettingsInternalPanel.add(networkTimeoutLabel);

        networkTimeoutSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(25000), Integer.valueOf(0), null, Integer.valueOf(100)));
        networkTimeoutSpinner.setName("networkTimeoutSpinner"); // NOI18N
        generalSettingsInternalPanel.add(networkTimeoutSpinner);

        sequenceLengthLabel.setText(resourceMap.getString("sequenceLengthLabel.text")); // NOI18N
        sequenceLengthLabel.setName("sequenceLengthLabel"); // NOI18N
        generalSettingsInternalPanel.add(sequenceLengthLabel);

        maxSequenceLengthSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        maxSequenceLengthSpinner.setToolTipText(resourceMap.getString("maxSequenceLengthSpinner.toolTipText")); // NOI18N
        maxSequenceLengthSpinner.setName("maxSequenceLengthSpinner"); // NOI18N
        generalSettingsInternalPanel.add(maxSequenceLengthSpinner);

        autoCorrectSequenceNamesLabel.setText(resourceMap.getString("autoCorrectSequenceNamesLabel.text")); // NOI18N
        autoCorrectSequenceNamesLabel.setName("autoCorrectSequenceNamesLabel"); // NOI18N
        generalSettingsInternalPanel.add(autoCorrectSequenceNamesLabel);

        autoCorrectSequenceNamesCheckbox.setText(resourceMap.getString("autoCorrectSequenceNamesCheckbox.text")); // NOI18N
        autoCorrectSequenceNamesCheckbox.setName("autoCorrectSequenceNamesCheckbox"); // NOI18N
        generalSettingsInternalPanel.add(autoCorrectSequenceNamesCheckbox);

        generalSettingsPanel.add(generalSettingsInternalPanel);

        tabbedPane.addTab(resourceMap.getString("generalSettingsPanel.TabConstraints.tabTitle"), generalSettingsPanel); // NOI18N

        guiSettingsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 10, 10));
        guiSettingsPanel.setName("guiSettingsPanel"); // NOI18N
        guiSettingsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        guiSettingsInternalPanel.setName("guiSettingsInternalPanel"); // NOI18N
        guiSettingsInternalPanel.setLayout(new java.awt.GridLayout(5, 2, 20, 16));

        autoSaveSessionLabel.setText(resourceMap.getString("autoSaveSessionLabel.text")); // NOI18N
        autoSaveSessionLabel.setName("autoSaveSessionLabel"); // NOI18N
        guiSettingsInternalPanel.add(autoSaveSessionLabel);

        autoSaveSessionCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Never", "Always", "Ask", "Ask for File" }));
        autoSaveSessionCombobox.setToolTipText(resourceMap.getString("autoSaveSessionCombobox.toolTipText")); // NOI18N
        autoSaveSessionCombobox.setName("autoSaveSessionCombobox"); // NOI18N
        guiSettingsInternalPanel.add(autoSaveSessionCombobox);

        notificationsLabel.setText(resourceMap.getString("notificationsLabel.text")); // NOI18N
        notificationsLabel.setName("notificationsLabel"); // NOI18N
        guiSettingsInternalPanel.add(notificationsLabel);

        notificationsLevelCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        notificationsLevelCombobox.setToolTipText(resourceMap.getString("notificationsLevelCombobox.toolTipText")); // NOI18N
        notificationsLevelCombobox.setName("notificationsLevelCombobox"); // NOI18N
        guiSettingsInternalPanel.add(notificationsLevelCombobox);

        antialiasModeLabel.setText(resourceMap.getString("antialiasModeLabel.text")); // NOI18N
        antialiasModeLabel.setName("antialiasModeLabel"); // NOI18N
        guiSettingsInternalPanel.add(antialiasModeLabel);

        antialiasModeCombobox.setName("antialiasModeCombobox"); // NOI18N
        guiSettingsInternalPanel.add(antialiasModeCombobox);

        TSSpositionLabel.setText(resourceMap.getString("TSSpositionLabel.text")); // NOI18N
        TSSpositionLabel.setName("TSSpositionLabel"); // NOI18N
        guiSettingsInternalPanel.add(TSSpositionLabel);

        TSSpositionCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "+0", "+1" }));
        TSSpositionCombobox.setToolTipText(resourceMap.getString("TSSpositionCombobox.toolTipText")); // NOI18N
        TSSpositionCombobox.setName("TSSpositionCombobox"); // NOI18N
        guiSettingsInternalPanel.add(TSSpositionCombobox);

        promptSaveLabel.setText(resourceMap.getString("promptSaveLabel.text")); // NOI18N
        promptSaveLabel.setName("promptSaveLabel"); // NOI18N
        guiSettingsInternalPanel.add(promptSaveLabel);

        promptBeforeDiscardingCheckbox.setText(resourceMap.getString("promptBeforeDiscardingCheckbox.text")); // NOI18N
        promptBeforeDiscardingCheckbox.setName("promptBeforeDiscardingCheckbox"); // NOI18N
        guiSettingsInternalPanel.add(promptBeforeDiscardingCheckbox);

        guiSettingsPanel.add(guiSettingsInternalPanel);

        tabbedPane.addTab(resourceMap.getString("guiSettingsPanel.TabConstraints.tabTitle"), guiSettingsPanel); // NOI18N

        visualizationSettingsPanel.setName("visualizationSettingsPanel"); // NOI18N
        visualizationSettingsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        vizualizationSettingsInternalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 10, 10));
        vizualizationSettingsInternalPanel.setName("vizualizationSettingsInternalPanel"); // NOI18N
        vizualizationSettingsInternalPanel.setLayout(new java.awt.GridLayout(6, 3, 20, 16));

        sequenceWindowSizeLabel.setText(resourceMap.getString("sequenceWindowSizeLabel.text")); // NOI18N
        sequenceWindowSizeLabel.setName("sequenceWindowSizeLabel"); // NOI18N
        vizualizationSettingsInternalPanel.add(sequenceWindowSizeLabel);

        sequenceWindowSizeSelector.setName("sequenceWindowSizeSelector"); // NOI18N
        vizualizationSettingsInternalPanel.add(sequenceWindowSizeSelector);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N
        vizualizationSettingsInternalPanel.add(jLabel1);

        sequenceLabelSizeLabel.setText(resourceMap.getString("sequenceLabelSizeLabel.text")); // NOI18N
        sequenceLabelSizeLabel.setName("sequenceLabelSizeLabel"); // NOI18N
        vizualizationSettingsInternalPanel.add(sequenceLabelSizeLabel);

        sequenceLabelSizeSelector.setName("sequenceLabelSizeSelector"); // NOI18N
        vizualizationSettingsInternalPanel.add(sequenceLabelSizeSelector);

        scaleToFitLabelCheckbox.setSelected(true);
        scaleToFitLabelCheckbox.setText(resourceMap.getString("scaleToFitLabelCheckbox.text")); // NOI18N
        scaleToFitLabelCheckbox.setName("scaleToFitLabelCheckbox"); // NOI18N
        scaleToFitLabelCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scaleToFitComboboxAction(evt);
            }
        });
        vizualizationSettingsInternalPanel.add(scaleToFitLabelCheckbox);

        numericTracksLabel.setText(resourceMap.getString("numericTracksLabel.text")); // NOI18N
        numericTracksLabel.setName("numericTracksLabel"); // NOI18N
        vizualizationSettingsInternalPanel.add(numericTracksLabel);

        numericTracksValueCombobox.setToolTipText(resourceMap.getString("numericTracksValueCombobox.toolTipText")); // NOI18N
        numericTracksValueCombobox.setName("numericTracksValueCombobox"); // NOI18N
        vizualizationSettingsInternalPanel.add(numericTracksValueCombobox);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        vizualizationSettingsInternalPanel.add(jLabel4);

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        vizualizationSettingsInternalPanel.add(jLabel3);

        numericTrackSamplingCutoffCombobox.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(100), Integer.valueOf(2), null, Integer.valueOf(1)));
        numericTrackSamplingCutoffCombobox.setName("numericTrackSamplingCutoffCombobox"); // NOI18N
        vizualizationSettingsInternalPanel.add(numericTrackSamplingCutoffCombobox);

        numericTrackSamplingNumberCombobox.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(20), Integer.valueOf(1), null, Integer.valueOf(1)));
        numericTrackSamplingNumberCombobox.setName("numericTrackSamplingNumberCombobox"); // NOI18N
        vizualizationSettingsInternalPanel.add(numericTrackSamplingNumberCombobox);

        backgroundcolorlabel.setText(resourceMap.getString("backgroundcolorlabel.text")); // NOI18N
        backgroundcolorlabel.setName("backgroundcolorlabel"); // NOI18N
        vizualizationSettingsInternalPanel.add(backgroundcolorlabel);

        backgroundcolorbutton.setText(resourceMap.getString("backgroundcolorbutton.text")); // NOI18N
        backgroundcolorbutton.setName("backgroundcolorbutton"); // NOI18N
        backgroundcolorbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backgroundcolorbuttonpressed(evt);
            }
        });
        vizualizationSettingsInternalPanel.add(backgroundcolorbutton);

        resetbgcolorbutton.setText(resourceMap.getString("resetbgcolorbutton.text")); // NOI18N
        resetbgcolorbutton.setName("resetbgcolorbutton"); // NOI18N
        resetbgcolorbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetbgcolorbuttonclicked(evt);
            }
        });
        vizualizationSettingsInternalPanel.add(resetbgcolorbutton);

        visualizationSettingsPanel.add(vizualizationSettingsInternalPanel);

        tabbedPane.addTab(resourceMap.getString("visualizationSettingsPanel.TabConstraints.tabTitle"), visualizationSettingsPanel); // NOI18N

        cacheSettingsPanel.setName("cacheSettingsPanel"); // NOI18N
        cacheSettingsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        cacheSettingsInternalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 10, 10));
        cacheSettingsInternalPanel.setName("cacheSettingsInternalPanel"); // NOI18N
        cacheSettingsInternalPanel.setLayout(new java.awt.GridLayout(2, 3, 20, 16));

        cacheFeatureDataLabel.setText(resourceMap.getString("cacheFeatureDataLabel.text")); // NOI18N
        cacheFeatureDataLabel.setName("cacheFeatureDataLabel"); // NOI18N
        cacheSettingsInternalPanel.add(cacheFeatureDataLabel);

        cacheFeatureDataCheckbox.setText(resourceMap.getString("cacheFeatureDataCheckbox.text")); // NOI18N
        cacheFeatureDataCheckbox.setName("cacheFeatureDataCheckbox"); // NOI18N
        cacheSettingsInternalPanel.add(cacheFeatureDataCheckbox);

        clearFeatureDataCacheButton.setAction(actionMap.get("clearFeatureData")); // NOI18N
        clearFeatureDataCacheButton.setToolTipText(null);
        clearFeatureDataCacheButton.setMaximumSize(new java.awt.Dimension(39, 23));
        clearFeatureDataCacheButton.setMinimumSize(new java.awt.Dimension(39, 23));
        clearFeatureDataCacheButton.setName("clearFeatureDataCacheButton"); // NOI18N
        clearFeatureDataCacheButton.setPreferredSize(new java.awt.Dimension(39, 23));
        cacheSettingsInternalPanel.add(clearFeatureDataCacheButton);

        cacheGeneIdDataLabel.setText(resourceMap.getString("cacheGeneIdDataLabel.text")); // NOI18N
        cacheGeneIdDataLabel.setName("cacheGeneIdDataLabel"); // NOI18N
        cacheSettingsInternalPanel.add(cacheGeneIdDataLabel);

        cacheGeneIDCheckbox.setText(resourceMap.getString("cacheGeneIDCheckbox.text")); // NOI18N
        cacheGeneIDCheckbox.setName("cacheGeneIDCheckbox"); // NOI18N
        cacheSettingsInternalPanel.add(cacheGeneIDCheckbox);

        clearGeneIDCacheButton.setAction(actionMap.get("clearGeneIDMappings")); // NOI18N
        clearGeneIDCacheButton.setToolTipText(null);
        clearGeneIDCacheButton.setName("clearGeneIDCacheButton"); // NOI18N
        cacheSettingsInternalPanel.add(clearGeneIDCacheButton);

        cacheSettingsPanel.add(cacheSettingsInternalPanel);

        tabbedPane.addTab(resourceMap.getString("cacheSettingsPanel.TabConstraints.tabTitle"), cacheSettingsPanel); // NOI18N

        protocolEditorPanel.setName("protocolEditorPanel"); // NOI18N

        protocolEditorInternalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 10, 10));
        protocolEditorInternalPanel.setName("protocolEditorInternalPanel"); // NOI18N

        javax.swing.GroupLayout protocolEditorInternalPanelLayout = new javax.swing.GroupLayout(protocolEditorInternalPanel);
        protocolEditorInternalPanel.setLayout(protocolEditorInternalPanelLayout);
        protocolEditorInternalPanelLayout.setHorizontalGroup(
            protocolEditorInternalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        protocolEditorInternalPanelLayout.setVerticalGroup(
            protocolEditorInternalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        protocolEditorPanel.add(protocolEditorInternalPanel);

        tabbedPane.addTab(resourceMap.getString("protocolEditorPanel.TabConstraints.tabTitle"), protocolEditorPanel); // NOI18N

        htmlSettingsPanel.setName("htmlSettingsPanel"); // NOI18N
        htmlSettingsPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        htmlSettingsInternalPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 10, 10, 10));
        htmlSettingsInternalPanel.setName("htmlSettingsInternalPanel"); // NOI18N
        htmlSettingsInternalPanel.setLayout(new java.awt.GridLayout(3, 3, 20, 16));

        htmlSettingsJavascriptLabel.setText(resourceMap.getString("htmlSettingsJavascriptLabel.text")); // NOI18N
        htmlSettingsJavascriptLabel.setName("htmlSettingsJavascriptLabel"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsJavascriptLabel);

        htmlSettingsJavascriptCombobox.setModel(new javax.swing.DefaultComboBoxModel(VisualizationSettings.HTML_SETTINGS));
        htmlSettingsJavascriptCombobox.setToolTipText(resourceMap.getString("htmlSettingsJavascriptCombobox.toolTipText")); // NOI18N
        htmlSettingsJavascriptCombobox.setName("htmlSettingsJavascriptCombobox"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsJavascriptCombobox);

        htmlSettingsEmptyLabel1.setText(resourceMap.getString("htmlSettingsEmptyLabel1.text")); // NOI18N
        htmlSettingsEmptyLabel1.setName("htmlSettingsEmptyLabel1"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsEmptyLabel1);

        htmlSettingsCSSLabel.setText(resourceMap.getString("htmlSettingsCSSLabel.text")); // NOI18N
        htmlSettingsCSSLabel.setName("htmlSettingsCSSLabel"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsCSSLabel);

        htmlSettingsCSSCombobox.setModel(new javax.swing.DefaultComboBoxModel(VisualizationSettings.HTML_SETTINGS));
        htmlSettingsCSSCombobox.setToolTipText(resourceMap.getString("htmlSettingsCSSCombobox.toolTipText")); // NOI18N
        htmlSettingsCSSCombobox.setName("htmlSettingsCSSCombobox"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsCSSCombobox);

        htmlSettingsEmptyLabel2.setText(resourceMap.getString("htmlSettingsEmptyLabel2.text")); // NOI18N
        htmlSettingsEmptyLabel2.setName("htmlSettingsEmptyLabel2"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsEmptyLabel2);

        htmlSettingsStylesheetLabel.setText(resourceMap.getString("htmlSettingsStylesheetLabel.text")); // NOI18N
        htmlSettingsStylesheetLabel.setName("htmlSettingsStylesheetLabel"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsStylesheetLabel);

        htmlSettingsStylesheetCombobox.setEditable(true);
        htmlSettingsStylesheetCombobox.setName("htmlSettingsStylesheetCombobox"); // NOI18N
        htmlSettingsInternalPanel.add(htmlSettingsStylesheetCombobox);

        stylesheetBrowseButton.setText(resourceMap.getString("stylesheetBrowseButton.text")); // NOI18N
        stylesheetBrowseButton.setName("stylesheetBrowseButton"); // NOI18N
        htmlSettingsInternalPanel.add(stylesheetBrowseButton);

        htmlSettingsPanel.add(htmlSettingsInternalPanel);

        tabbedPane.addTab(resourceMap.getString("htmlSettingsPanel.TabConstraints.tabTitle"), htmlSettingsPanel); // NOI18N

        otherSettingsPanel.setName("otherSettingsPanel"); // NOI18N
        otherSettingsPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel7.setName("jPanel7"); // NOI18N
        jPanel1.add(jPanel7);

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 4));
        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        editStartupScriptButton.setAction(actionMap.get("editStartupScript")); // NOI18N
        editStartupScriptButton.setText(resourceMap.getString("editStartupScriptButton.text")); // NOI18N
        editStartupScriptButton.setName("editStartupScriptButton"); // NOI18N
        jPanel4.add(editStartupScriptButton);

        jPanel1.add(jPanel4);

        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 4));
        jPanel5.setMinimumSize(null);
        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setPreferredSize(null);
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        viewDisplaySettingsButton.setAction(actionMap.get("showDisplaySettingsDialog")); // NOI18N
        viewDisplaySettingsButton.setText(resourceMap.getString("viewDisplaySettingsButton.text")); // NOI18N
        viewDisplaySettingsButton.setToolTipText(resourceMap.getString("viewDisplaySettingsButton.toolTipText")); // NOI18N
        viewDisplaySettingsButton.setName("viewDisplaySettingsButton"); // NOI18N
        jPanel5.add(viewDisplaySettingsButton);

        jPanel1.add(jPanel5);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 4));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButton1.setAction(actionMap.get("activateTransfacPRO")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N
        jPanel2.add(jButton1);

        jPanel1.add(jPanel2);

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 4));
        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButton2.setAction(actionMap.get("showSystemDirectory")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jPanel6.add(jButton2);

        jPanel1.add(jPanel6);

        jPanel3.setName("jPanel3"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 499, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 59, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel3);

        filler1.setName("filler1"); // NOI18N
        jPanel1.add(filler1);

        otherSettingsPanel.add(jPanel1, java.awt.BorderLayout.CENTER);

        tabbedPane.addTab(resourceMap.getString("otherSettingsPanel.TabConstraints.tabTitle"), otherSettingsPanel); // NOI18N

        getContentPane().add(tabbedPane, java.awt.BorderLayout.PAGE_START);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void scaleToFitComboboxAction(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleToFitComboboxAction
// TODO add your handling code here:
       sequenceLabelSizeSelector.setEnabled(!scaleToFitLabelCheckbox.isSelected());//GEN-LAST:event_scaleToFitComboboxAction
}

private void backgroundcolorbuttonpressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backgroundcolorbuttonpressed
// TODO add your handling code here:
      Color color=JColorChooser.showDialog(settings.getGUI().getFrame(),"Select new background color",backgroundcolorbutton.getBackground());//GEN-LAST:event_backgroundcolorbuttonpressed
      if (color!=null) backgroundcolorbutton.setBackground(color);
}

private void resetbgcolorbuttonclicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetbgcolorbuttonclicked
// TODO add your handling code here:
    backgroundcolorbutton.setBackground(new javax.swing.JPanel().getBackground());//GEN-LAST:event_resetbgcolorbuttonclicked
}


    @Action
    public void cancelButtonPressed() {
       setVisible(false);
    }

    @Action
    public void applyButtonPressed() {
       applyChanges();
    }

    @Action
    public void OKButtonPressed() {
       boolean ok=applyChanges();
       if (ok) setVisible(false);
    }

    @Action
    public void clearFeatureData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        settings.getGUI().logMessage("Clearing feature data");        
        settings.getEngine().getDataLoader().clearCache();
        setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void clearGeneIDMappings() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        settings.getGUI().logMessage("Clearing gene mappings");        
        settings.getEngine().getGeneIDResolver().clearCache();
        setCursor(Cursor.getDefaultCursor());
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton OKButton;
    private javax.swing.JComboBox TSSpositionCombobox;
    private javax.swing.JLabel TSSpositionLabel;
    private javax.swing.JComboBox antialiasModeCombobox;
    private javax.swing.JLabel antialiasModeLabel;
    private javax.swing.JButton applyButton;
    private javax.swing.JCheckBox autoCorrectSequenceNamesCheckbox;
    private javax.swing.JLabel autoCorrectSequenceNamesLabel;
    private javax.swing.JComboBox autoSaveSessionCombobox;
    private javax.swing.JLabel autoSaveSessionLabel;
    private javax.swing.JButton backgroundcolorbutton;
    private javax.swing.JLabel backgroundcolorlabel;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JCheckBox cacheFeatureDataCheckbox;
    private javax.swing.JLabel cacheFeatureDataLabel;
    private javax.swing.JCheckBox cacheGeneIDCheckbox;
    private javax.swing.JLabel cacheGeneIdDataLabel;
    private javax.swing.JPanel cacheSettingsInternalPanel;
    private javax.swing.JPanel cacheSettingsPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton clearFeatureDataCacheButton;
    private javax.swing.JButton clearGeneIDCacheButton;
    private javax.swing.JLabel concurrentThreadsLabel;
    private javax.swing.JSpinner concurrentThreadsSpinner;
    private javax.swing.JButton editStartupScriptButton;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JPanel generalSettingsInternalPanel;
    private javax.swing.JPanel generalSettingsPanel;
    private javax.swing.JPanel guiSettingsInternalPanel;
    private javax.swing.JPanel guiSettingsPanel;
    private javax.swing.JComboBox htmlSettingsCSSCombobox;
    private javax.swing.JLabel htmlSettingsCSSLabel;
    private javax.swing.JLabel htmlSettingsEmptyLabel1;
    private javax.swing.JLabel htmlSettingsEmptyLabel2;
    private javax.swing.JPanel htmlSettingsInternalPanel;
    private javax.swing.JComboBox htmlSettingsJavascriptCombobox;
    private javax.swing.JLabel htmlSettingsJavascriptLabel;
    private javax.swing.JPanel htmlSettingsPanel;
    private javax.swing.JComboBox htmlSettingsStylesheetCombobox;
    private javax.swing.JLabel htmlSettingsStylesheetLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JLabel maxConcurrentDownloadsLabel;
    private javax.swing.JSpinner maxConcurrentDownloadsSpinner;
    private javax.swing.JSpinner maxSequenceLengthSpinner;
    private javax.swing.JLabel networkTimeoutLabel;
    private javax.swing.JSpinner networkTimeoutSpinner;
    private javax.swing.JLabel notificationsLabel;
    private javax.swing.JComboBox notificationsLevelCombobox;
    private javax.swing.JSpinner numericTrackSamplingCutoffCombobox;
    private javax.swing.JSpinner numericTrackSamplingNumberCombobox;
    private javax.swing.JLabel numericTracksLabel;
    private javax.swing.JComboBox numericTracksValueCombobox;
    private javax.swing.JPanel otherSettingsPanel;
    private javax.swing.JCheckBox promptBeforeDiscardingCheckbox;
    private javax.swing.JLabel promptSaveLabel;
    private javax.swing.JPanel protocolEditorInternalPanel;
    private javax.swing.JPanel protocolEditorPanel;
    private javax.swing.JButton resetbgcolorbutton;
    private javax.swing.JCheckBox scaleToFitLabelCheckbox;
    private javax.swing.JLabel sequenceLabelSizeLabel;
    private javax.swing.JSpinner sequenceLabelSizeSelector;
    private javax.swing.JLabel sequenceLengthLabel;
    private javax.swing.JLabel sequenceWindowSizeLabel;
    private javax.swing.JSpinner sequenceWindowSizeSelector;
    private javax.swing.JButton stylesheetBrowseButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JButton viewDisplaySettingsButton;
    private javax.swing.JPanel visualizationSettingsPanel;
    private javax.swing.JPanel vizualizationSettingsInternalPanel;
    // End of variables declaration//GEN-END:variables


    private class ColorButtonListener implements ActionListener {
        JButton button;
        public ColorButtonListener(JButton button) {
            this.button=button;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Color color=JColorChooser.showDialog(PreferencesDialog.this,"Select new color for "+button.getText(),button.getBackground());
            if (color!=null) {
                button.setBackground(color);
                button.setForeground((VisualizationSettings.isDark(color))?Color.WHITE:Color.BLACK);
            }
        }


    }

    @Action
    public void activateTransfacPRO() {
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ActivateTransfacProDialog dialog=new ActivateTransfacProDialog(gui);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=gui.getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        gui.getFrame().setCursor(Cursor.getDefaultCursor());
    }

    @Action
    public void editStartupScript() {
        String filename=gui.getEngine().getMotifLabDirectory()+File.separator+"startup.config";
        File file=new File(filename);
        if (!file.exists()) { // try installing it once more
            try {
                InputStream stream=this.getClass().getResourceAsStream("/motiflab/engine/resources/startup.config");
                gui.getEngine().installConfigFileFromStream("startup.config",stream);
            } catch (Exception e) {e.printStackTrace(System.err);} // don't bother with the error.            
        }
        if (file.exists()) {
            gui.getProtocolEditor().openProtocolFile(file);
        } else {
            JOptionPane.showMessageDialog(this, "Unable to open \"startup.config\"", "Error", JOptionPane.ERROR_MESSAGE);
        }        
    }
    
    @Action
    public void showDisplaySettingsDialog() {
        gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ConfigureDisplaySettingsDialog dialog=new ConfigureDisplaySettingsDialog(gui);
        int height=dialog.getHeight();
        int width=dialog.getWidth();
        java.awt.Dimension size=gui.getFrame().getSize();
        dialog.setLocation((int)((size.width-width)/2),(int)((size.height-height)/2));
        dialog.setVisible(true);
        dialog.dispose();
        gui.getFrame().setCursor(Cursor.getDefaultCursor());         
    }

    @Action
    public void showSystemDirectory() {
        String path=gui.getEngine().getMotifLabDirectory();
        Desktop desktop=java.awt.Desktop.getDesktop();
        String error=null;
        if (desktop!=null && Desktop.isDesktopSupported()) {
            try {
                desktop.browse(getFileURI(path));  
            } catch (IOException e) {
                error="Unable to launch external file browser:\n"+e.getMessage();      
            }
        } else error="Unable to launch external file browser";  
        if (error!=null) {
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // generate uri according to the filePath
    private static URI getFileURI(String filePath) {
        URI uri = null;
        filePath = filePath.trim();
        if (filePath.indexOf("http") == 0 || filePath.indexOf("\\") == 0) {
            if (filePath.indexOf("\\") == 0){
                filePath = "file:" + filePath;
                filePath = filePath.replaceAll("#", "%23");
            }
            try {
                filePath = filePath.replaceAll(" ", "%20");
                URL url = new URL(filePath);
                uri = url.toURI();
            } catch (Exception ex) {} 
        } else {
            File file = new File(filePath);
            uri = file.toURI();
        }
        return uri;
    }    
    
}
