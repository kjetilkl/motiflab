/*
 
 
 */

package org.motiflab.engine.task;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabClient;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.operations.Operation_output;
import org.motiflab.engine.protocol.Protocol;
import org.motiflab.engine.util.SequenceSorter;
import org.motiflab.gui.MotifLabGUI;
import org.motiflab.gui.PreferencesDialog;
import org.motiflab.gui.SequenceVisualizer;
import org.motiflab.gui.SortSequencesDialog;
import org.motiflab.gui.VisualizationSettings;
import org.motiflab.gui.VisualizationSettingsListener;

/**
 *
 * @author Kjetil
 */
public class DisplaySettingTask extends ExecutableTask {

    private String settingName;
    private String targets; // original string
    private String[] targetNames; // resolved names
    private Object value;
    private boolean force=false;
    private ClearDataTask clearTask=null; // the DisplaySettingTask can function as a wrapper around this one


    public DisplaySettingTask(String settingName, String targets, Object value, boolean force) {
        super("Display setting");
        this.settingName=settingName;
        this.targets=targets;
        this.value=value;
        this.force=force;        
    }
    
    /**
     * Returns the name of the display setting (the "command")
     * @return 
     */
    public String getSettingName() {
        return settingName;
    }
    
    /**
     * Returns the string denoting the targets for this task (this can be a list)
     * @return 
     */    
    public String getTargets() {
        return targets;
    }
          
    public Object getValue() {
        return value;
    }
    
    public boolean shouldForce() {
        return force;
    }

    @Override
    public String getCommandString(Protocol protocol) { // this is currently not used, since these tasks are not recorded to protocol
        String prefix=(force)?"!":"$";
        if (value==null) return prefix+settingName+"("+targets+")";
        else return prefix+settingName+"("+targets+")="+getValueString(value);
    }

    private String getValueString(Object value) {
        if (value instanceof Color) {
            int red=((Color)value).getRed();
            int green=((Color)value).getGreen();
            int blue=((Color)value).getBlue();
            return red+","+green+","+blue;
        } else return value.toString();
    }
    
    
    
    @Override
    public void run() throws InterruptedException, Exception {
        setProgress(1);
        setStatus(RUNNING);
        //if (undoMonitor!=null) undoMonitor.register();
          try {
            execute();
          } catch (InterruptedException e) { // task aborted by the user
              //if (undoMonitor!=null) undoMonitor.deregister(false);
              setStatus(ABORTED);
              throw e;
          } catch (Exception e) { // other errors
              //e.printStackTrace(System.err);
              int lineNumber=getLineNumber();
              if (lineNumber>0) getMotifLabClient().logMessage("Display settings error in line "+getLineNumber()+": "+e.getMessage());
              else getMotifLabClient().logMessage("Display settings error: "+e.getMessage());           
//              setStatus(ERROR); // do not stop execution. Just ignore the error and move on
//              throw e;
          }

        setProgress(100);
        //if (undoMonitor!=null) undoMonitor.deregister(true);
        setStatus(DONE);
        setStatusMessage(null);
    }


    private void execute() throws InterruptedException, ExecutionError {
        VisualizationSettings settings=getMotifLabClient().getVisualizationSettings();
        if (settings==null) {
            getMotifLabClient().logMessage("WARNING: Unable to execute Display Setting task because the VisualizationSettings object is missing ",5);
            return;
        }
        resolveNames();
        //System.err.println("DisplaySetting: "+settingName+"("+targets+"|"+targetNames+")="+value);
        if (settingName.equalsIgnoreCase("visible")) { // I'm just setting "everything" to the new value without checking the type
            settings.setTrackVisible(targetNames, getBoolean(value));
            settings.setSequenceVisible(targetNames, getBoolean(value));
            settings.setRegionTypeVisible(targetNames, getBoolean(value),true);            
        } 
        else if (settingName.equalsIgnoreCase("expanded")) {
            settings.setExpanded(targetNames, getBoolean(value));
        } 
        else if (settingName.equalsIgnoreCase("gradient") || settingName.equalsIgnoreCase("gradientfill")) {
            if (value instanceof Boolean) {
               int newsetting=((Boolean)value)?1:0;
               settings.setGradientFillRegions(targetNames, newsetting);
            } else if (value instanceof Integer) {
               int val=((Integer)value).intValue();
               if (val<0 || val>=2) val=0;
               settings.setGradientFillRegions(targetNames, val);                
            } else {
               String valueAsString=value.toString().toLowerCase();
               if (valueAsString.contains("vertical")) settings.setGradientFillRegions(targetNames, 1);
               else if (valueAsString.contains("horisontal") || valueAsString.contains("horizontal")) settings.setGradientFillRegions(targetNames, 2);
               else settings.setGradientFillRegions(targetNames, 0);
            }
        } 
        else if (settingName.equalsIgnoreCase("motifTrack") || settingName.equalsIgnoreCase("moduleTrack")) {
            boolean ismotiftrack=settingName.equalsIgnoreCase("motifTrack");
            MotifLabEngine engine=settings.getEngine();
            for (String trackName:targetNames) {
                Data dataset=engine.getDataItem(trackName);
                if (dataset instanceof RegionDataset) {
                    if (ismotiftrack) ((RegionDataset)dataset).setMotifTrack(getBoolean(value));
                    else ((RegionDataset)dataset).setModuleTrack(getBoolean(value));
                }
            }
        } 
        else if (settingName.equalsIgnoreCase("showscore")) {
            settings.setVisualizeRegionScore(targetNames, getBoolean(value));
        } 
        else if (settingName.equalsIgnoreCase("showorientation") || settingName.equalsIgnoreCase("showstrand")) {
            settings.setVisualizeRegionStrand(targetNames, getBoolean(value));
        } 
        else if (settingName.equalsIgnoreCase("color") || settingName.equalsIgnoreCase("fgcolor") || settingName.equalsIgnoreCase("foreground")) {
               Color color=(value instanceof Color)?((Color)value):Color.BLACK;
               settings.setForeGroundColor(targetNames, color);
        } 
        else if (settingName.equalsIgnoreCase("bgcolor") || settingName.equalsIgnoreCase("background")) {
               Color color=(value instanceof Color)?((Color)value):Color.BLACK;
               settings.setBackGroundColor(targetNames, color);
        } 
        else if (settingName.equalsIgnoreCase("secondary") || settingName.equalsIgnoreCase("secondarycolor")) {
               Color color=(value instanceof Color)?((Color)value):Color.BLACK;
               settings.setSecondaryColor(targetNames, color);
        } 
        else if (settingName.equalsIgnoreCase("baseline") || settingName.equalsIgnoreCase("baselinecolor")) {
               Color color=(value instanceof Color)?((Color)value):Color.BLACK;
               settings.setBaselineColor(targetNames, color);
        } 
        else if (settingName.equalsIgnoreCase("canvas") || settingName.equalsIgnoreCase("canvascolor")) {
               Color color=(value instanceof Color)?((Color)value):Color.WHITE;
               settings.setVisualizationPanelBackgroundColor(color);
        } 
        else if (settingName.equalsIgnoreCase("regioncolor") || settingName.equalsIgnoreCase("motifcolor") || settingName.equalsIgnoreCase("modulecolor")) {
               Color color=(value instanceof Color)?((Color)value):Color.BLACK;
               settings.setFeatureColor(targetNames, color, true);
        } 
        else if (settingName.equalsIgnoreCase("regionvisible") || settingName.equalsIgnoreCase("motifvisible") || settingName.equalsIgnoreCase("modulevisible")) {
               settings.setFeatureVisible(targetNames, getBoolean(value),true);
        } 
        else if (settingName.equalsIgnoreCase("label") || settingName.equalsIgnoreCase("labelcolor")) {
               Color color=(value instanceof Color)?((Color)value):Color.BLACK;
               settings.setSequenceLabelColor(targetNames, color);
        } 
        else if (settingName.equalsIgnoreCase("height") || settingName.equalsIgnoreCase("trackheight")) {
               int height=(value instanceof Integer)?((Integer)value).intValue():16;
               settings.setDataTrackHeight(targetNames, height, true);
        } 
        else if (settingName.equalsIgnoreCase("regionheight")) {
               int height=(value instanceof Integer)?((Integer)value).intValue():16;
               settings.setExpandedRegionHeight(targetNames, height, true);
        } 
        else if (settingName.equalsIgnoreCase("rowspacing")) {
               int spacing=(value instanceof Integer)?((Integer)value).intValue():2;
               settings.setRowSpacing(targetNames, spacing, true);
        }         
        else if (settingName.equalsIgnoreCase("margin")) {
               int margin=(value instanceof Integer)?((Integer)value).intValue():16;
               settings.setSequenceMargin(margin);
        } 
        else if (settingName.equalsIgnoreCase("motifvisible") || settingName.equalsIgnoreCase("modulevisible")) {
               settings.setFeatureVisible(targetNames, getBoolean(value),true);
        } 
        else if (settingName.equalsIgnoreCase("regionvisible")) {
               settings.setRegionTypeVisible(targetNames, getBoolean(value),true);
        } 
        else if (settingName.equalsIgnoreCase("sequencevisible")) {
               settings.setSequenceVisible(targetNames, getBoolean(value));
        } 
        else if (settingName.equalsIgnoreCase("modulefillcolor")) {
               if (value instanceof Color) settings.setModuleFillColor((Color)value);
               else if (value instanceof String && ((String)value).equalsIgnoreCase("none")) settings.setModuleFillColor(VisualizationSettings.COLOR_MODULE_BY_TYPE);
               else settings.setModuleFillColor(null);
        } 
        else if (settingName.equalsIgnoreCase("moduleoutlinecolor")) {
               if (value instanceof Color) settings.setModuleOutlineColor((Color)value);
               else if (value instanceof String && ((String)value).equalsIgnoreCase("none")) settings.setModuleOutlineColor(VisualizationSettings.COLOR_MODULE_BY_TYPE);
               else settings.setModuleOutlineColor(null);
        } 
        else if (settingName.equalsIgnoreCase("trackbordercolor")) {
               if (value instanceof Color) {
                   SequenceVisualizer.setTrackBorderColor((Color)value);
                   settings.redraw();
               }
        }         
        else if (settingName.equalsIgnoreCase("order") || settingName.equalsIgnoreCase("trackorder")) {
               settings.setTrackOrder(targetNames);
        } 
        else if (settingName.equalsIgnoreCase("multicolor")) {
               settings.setUseMultiColoredRegions(targetNames, getBoolean(value));
        } 
        else if (settingName.equalsIgnoreCase("regionborder")) {
            int border=0;
            if (value instanceof Color) border=2; // black
            else if (value instanceof Integer) border=((Integer)value).intValue();
            else if (value instanceof String) {
                if (((String)value).toLowerCase().startsWith("dark")) border=1;
                else if (((String)value).equalsIgnoreCase("black")) border=2;               
            }
            if (border<0 || border>2) border=0;
            settings.setDrawRegionBorders(targetNames, border);
        }         
        else if (settingName.equalsIgnoreCase("graph") || settingName.equalsIgnoreCase("graphtype")) {
               if (value instanceof String) {
                   String graphtype=((String)value);
                        if (graphtype.equalsIgnoreCase("graph") || graphtype.equalsIgnoreCase("filled graph")) graphtype="Graph (filled)";
                   else if (graphtype.equalsIgnoreCase("line graph") || graphtype.equalsIgnoreCase("line")) graphtype="Graph (line)";
                   else if (graphtype.equalsIgnoreCase("outlined graph") || graphtype.equalsIgnoreCase("outline")  || graphtype.equalsIgnoreCase("outlined")) graphtype="Graph (outlined)";
                   else if (graphtype.equalsIgnoreCase("one-color heatmap") || graphtype.equalsIgnoreCase("one color heatmap") || graphtype.equalsIgnoreCase("heatmap") || graphtype.equalsIgnoreCase("gradient")) graphtype="Heatmap (1 color)";
                   else if (graphtype.equalsIgnoreCase("two-color heatmap") || graphtype.equalsIgnoreCase("two color heatmap"))  graphtype="Heatmap (2 colors)";
                   else if (graphtype.equalsIgnoreCase("rainbow heatmap") || graphtype.equalsIgnoreCase("rainbow")) graphtype="Heatmap (rainbow)";
                   // Should I check if this graphtype exists and is allowed?
                   settings.setGraphType(targetNames, graphtype);
               } else throw new ExecutionError("Illegal assignment value '"+value+"' for  for display setting '"+settingName+"'");
        }
        else if (settingName.equalsIgnoreCase("range")) { // sets the minimum and maximum (and baseline) values of Numeric Datasets explicitly
            double[] range=parseRangeValues((String)value); // return value==null means "to fit". The length of range is either 2 or 3. Throws ExecutionError if unable to parse
            for (String target:targetNames) {
                Data data=getMotifLabEngine().getDataItem(target);
                if (data==null) getMotifLabClient().logMessage("Unknown data object: "+target);
                else if (!(data instanceof NumericDataset)) getMotifLabClient().logMessage("Not a Numeric Dataset: "+target);
                else {
                    NumericDataset dataset=(NumericDataset)data;
                    if (range==null) { // scale to fit
                        double[] minmax = dataset.getMinMaxValuesFromData();
                        setNewRangeValues(dataset, minmax[0], minmax[1], dataset.getBaselineValue());                          
                    } else {
                        double baseline=(range.length==3)?range[2]:dataset.getBaselineValue(); 
                        setNewRangeValues(dataset, range[0], range[1], baseline);                        
                    }
                }
            }
            settings.redraw();
        }         
        else if (settingName.equalsIgnoreCase("scale")) {
            if (value instanceof String) settings.zoomToFitSequence(targetNames, true);
            else if (value instanceof Double) settings.zoomToLevel(targetNames, (Double)value, true);
        } 
        else if (settingName.equalsIgnoreCase("orientation")) {
            if (((String)value).equalsIgnoreCase("DIRECT")) {
                for (String targetName:targetNames) {
                    settings.setSequenceOrientation(targetName, VisualizationSettings.DIRECT, false);
                }
                //settings.redraw();
            } else if (((String)value).equalsIgnoreCase("REVERSE")) {
                 for (String targetName:targetNames) {
                    settings.setSequenceOrientation(targetName, VisualizationSettings.REVERSE, false);
                }
                //settings.redraw();               
            } else if (((String)value).equalsIgnoreCase("RELATIVE") || ((String)value).equalsIgnoreCase("FROM SEQUENCE") ) {
                MotifLabEngine engine=getMotifLabClient().getEngine();
                for (String targetName:targetNames) {
                    Data seq=engine.getDataItem(targetName);
                    if (seq instanceof Sequence) {
                       int orientation=VisualizationSettings.DIRECT;
                       if (((Sequence)seq).getStrandOrientation()==Sequence.REVERSE) orientation=VisualizationSettings.REVERSE;
                       settings.setSequenceOrientation(targetName, orientation, false);  
                    }                
                }
                //settings.redraw();               
            } else if (((String)value).equalsIgnoreCase("OPPOSITE")) {
                MotifLabEngine engine=getMotifLabClient().getEngine();
                for (String targetName:targetNames) {
                    Data seq=engine.getDataItem(targetName);
                    if (seq instanceof Sequence) {
                       int orientation=VisualizationSettings.DIRECT;
                       if (((Sequence)seq).getStrandOrientation()==Sequence.DIRECT) orientation=VisualizationSettings.REVERSE;
                       settings.setSequenceOrientation(targetName, orientation, false);  
                    }                
                }
                //settings.redraw();                  
            } else throw new ExecutionError("Unknown orientation '"+value+"'");
        } 
        else if (settingName.equalsIgnoreCase("alignment")) {
            if (((String)value).equalsIgnoreCase("LEFT"))  settings.setSequenceAlignment(targetNames, VisualizationSettings.ALIGNMENT_LEFT);
            else if (((String)value).equalsIgnoreCase("RIGHT"))  settings.setSequenceAlignment(targetNames, VisualizationSettings.ALIGNMENT_RIGHT);
            else if (((String)value).equalsIgnoreCase("TSS"))  settings.setSequenceAlignment(targetNames, VisualizationSettings.ALIGNMENT_TSS);
            else if (((String)value).equalsIgnoreCase("NONE"))  settings.setSequenceAlignment(targetNames, VisualizationSettings.ALIGNMENT_NONE);
            else throw new ExecutionError("Unknown alignment specification '"+value+"'");
        } 
        else if (settingName.equalsIgnoreCase("updates")) {
            settings.enableVisualizationSettingsNotifications(getBoolean(value));
        } 
        else if (settingName.equalsIgnoreCase("setup") || settingName.equalsIgnoreCase("refresh")) {
            settings.notifyListenersOfSequenceLayoutUpdate(VisualizationSettingsListener.REMOVED, null);
        } 
        else if (settingName.equalsIgnoreCase("saveSession")) {
            try {
               getMotifLabClient().saveSession(targets);
            } catch (Exception e) {
                throw new ExecutionError("An error occurred while saving session: "+e.getMessage());
            }
        } 
        else if (settingName.equalsIgnoreCase("restoreSession")) {
            try {
               getMotifLabClient().restoreSession(targets);
            } catch (Exception e) {
                throw new ExecutionError("An error occurred while restoring session: "+e.getMessage());
            }
        } 
        else if (settingName.equalsIgnoreCase("saveOutput")) {           
            Data data=getMotifLabClient().getEngine().getDataItem(targets);
            if (data instanceof OutputData) {
                try {
                    getMotifLabClient().logMessage("Saving '"+targets+"' to file: "+(String)value);
                   ((OutputData)data).saveToFile((String)value,getMotifLabClient().getEngine()); // 
                } catch (IOException e) {
                   throw new ExecutionError("An error occurred while saving output: "+e.getMessage()); 
                }
            } else throw new ExecutionError("'"+targets+"' is not an OutputData object");
        } 
        else if (settingName.equalsIgnoreCase("import")) {
            try {
               settings.importSettingsFromFile(targets);
            } catch (Exception e) {
               if (!force) throw new ExecutionError("An error occurred while importing settings: "+e.getMessage()); 
            }
        } 
        else if (settingName.equalsIgnoreCase("setting")) {
            settings.storeSetting(targets, value);
            //settings.redraw();// just in case
        } 
        else if (settingName.equalsIgnoreCase("option")) {
            PreferencesDialog.setOption(targets, value, getMotifLabClient().getEngine());
        } 
        else if (settingName.equalsIgnoreCase("sort")) {
            String mode=targetNames[0];
            String dataobjectName=targetNames[1];
            String groupByName=targetNames[2];
            SequenceSorter.checkParameters(mode, dataobjectName, groupByName, getMotifLabClient().getEngine()); // throws ExecutionError if there are problems with any parameters
            SortSequencesDialog.sortBy(mode, (Boolean)value, dataobjectName, groupByName, getMotifLabClient());
        } 
        else if (settingName.equalsIgnoreCase("display")) {
            MotifLabClient client=getMotifLabClient();
            if (client instanceof MotifLabGUI) {
                MotifLabEngine engine=client.getEngine();
                for (String target:targetNames) {
                    Data item=engine.getDataItem(target);
                    if (item==null) continue;
                    displayDataItem(item, (MotifLabGUI)client);
                } 
            }
        } 
        else if (settingName.equalsIgnoreCase("clear")) {        
           MotifLabClient client=getMotifLabClient();
           if (client!=null) {
               clearTask=new ClearDataTask(client.getEngine(),targets);
               clearTask.execute();
           }          
        } 
        else if (settingName.equalsIgnoreCase("log")) {
           String text=targets;
           MotifLabClient client=getMotifLabClient();          
           Operation_output output=(Operation_output)client.getEngine().getOperation("output");
           text=Operation_output.processDirectOutput(text, true);
           if (output!=null) {
               text=output.resolveNamedReferences(text);
           }
           client.getEngine().logMessage(text);
        }     
        else if (settingName.equalsIgnoreCase("message")) {
           String text=targets;
           String type=(value!=null)?value.toString():null;
           if (type!=null && (type.isEmpty() || type.equalsIgnoreCase("plain"))) type=null;
           MotifLabClient client=getMotifLabClient();          
           Operation_output output=(Operation_output)client.getEngine().getOperation("output");
           text=Operation_output.processDirectOutput(text, true);
           if (output!=null) {
               text=output.resolveNamedReferences(text);
           }
           if (client instanceof MotifLabGUI) {
               int messageType=JOptionPane.PLAIN_MESSAGE;
               if (type!=null && type.equalsIgnoreCase("error")) messageType=JOptionPane.ERROR_MESSAGE;
               if (type!=null && type.equalsIgnoreCase("warning")) messageType=JOptionPane.WARNING_MESSAGE;
               if (type!=null && type.equalsIgnoreCase("question")) messageType=JOptionPane.QUESTION_MESSAGE;
               if (type!=null && type.equalsIgnoreCase("information")) messageType=JOptionPane.INFORMATION_MESSAGE;
               ((MotifLabGUI)client).alert(text, messageType);
           } else {
              if (type!=null) text=type+" : "+text;
              client.getEngine().logMessage(text);
           }
        }          
        else if (settingName.equalsIgnoreCase("dump")) {         
           MotifLabClient client=getMotifLabClient();
           if (client==null) throw new ExecutionError("Error: unable to make contact with client..."); 
           if (targets!=null && !targets.isEmpty()) {
               Object currentvalue=settings.getSetting(targets); 
               if (currentvalue instanceof Color) currentvalue=VisualizationSettings.convertColorToHTMLrepresentation((Color)currentvalue);
               if (currentvalue==null) client.logMessage("[DUMP] "+targets+" = ");
               else client.logMessage("[DUMP] "+targets+" = "+currentvalue.toString());
           } else {
               ArrayList<String> keys=settings.getAllKeys(true);
               client.logMessage("[DUMP]");
               for (String key:keys) {
                   Object currentvalue=settings.getSetting(key); 
                   if (currentvalue instanceof Color) currentvalue=VisualizationSettings.convertColorToHTMLrepresentation((Color)currentvalue);
                   if (currentvalue==null) client.logMessage(" - "+key+" = ");
                   else client.logMessage(" - "+key+" = "+currentvalue.toString());                  
               }
           } 
        } 
        else if (settingName.equalsIgnoreCase("macro")) {   
           String macroname=targets;
           String macrodefinition=(String)value;
           MotifLabEngine engine=getMotifLabClient().getEngine();
           if (engine.isMacroDefined(macroname) && !force) return; // do not replace existing macro unless forced to
           if (macrodefinition.contains(macroname)) throw new ExecutionError("Self-referencing macros are not allowed");
           engine.addMacro(macroname, macrodefinition);
        }
        else if (settingName.equalsIgnoreCase("pause")) {
            if (value instanceof Integer) {
               Thread.sleep((Integer)value); // NB: The InterruptedException is not caught here since that exception is also used to signal protocol abortion and catching it would interfere with that mechanism!
            }                      
        }
        else if (settingName.equalsIgnoreCase("stop")) {
            throw new InterruptedException("Execution stopped");                    
        }        
        else throw new ExecutionError("Unknown display setting '"+settingName+"'");    
        settings.redraw();
    }
       
    private boolean getBoolean(Object value) {
        if (value==null) return true;
        else if (value instanceof Boolean) return (Boolean)value;
        else if (value instanceof String && (((String)value).equalsIgnoreCase("ON") || ((String)value).equalsIgnoreCase("YES"))) return true;
        else if (value instanceof String && (((String)value).equalsIgnoreCase("OFF") || ((String)value).equalsIgnoreCase("NO"))) return false;      
        else return Boolean.parseBoolean(value.toString());        
    }
    
    @Override
    public void purgeReferences() {
        settingName=null;
        targetNames=null;
        targets=null;
        value=null;
    }
    
    /** This method splits the target string into separate names (if it is a comma separated list)
     *  checks the data types of the individual names and resolves references to collections or wildcards
     * @throws InterruptedException
     * @throws Exception 
     */
    private void resolveNames() throws InterruptedException, ExecutionError {
             if (isMotifSetting(settingName)) targetNames=parseDataNames(settingName, targets, Motif.class);
        else if (isModuleSetting(settingName)) targetNames=parseDataNames(settingName, targets, ModuleCRM.class);
        else if (isSequenceSetting(settingName)) targetNames=parseDataNames(settingName, targets, Sequence.class);
        else if (settingName.equalsIgnoreCase("setting") || settingName.equalsIgnoreCase("import") || settingName.equalsIgnoreCase("option") || settingName.equalsIgnoreCase("clear") || settingName.equalsIgnoreCase("macro") || settingName.equalsIgnoreCase("dump") || settingName.equalsIgnoreCase("log") || settingName.equalsIgnoreCase("message")) targetNames=null; // not needed
        else if (settingName.equalsIgnoreCase("saveSession") || settingName.equalsIgnoreCase("restoreSession") || settingName.equalsIgnoreCase("saveOutput")) targetNames=null; // not needed
        else if (settingName.equalsIgnoreCase("regionvisible") || settingName.equalsIgnoreCase("regioncolor")) targetNames=parseDataNames(settingName, targets, null); // these are not region tracks but individual region types (like motifs but more general). Hence the NULL parameter
        else if (settingName.equalsIgnoreCase("graph") || settingName.equalsIgnoreCase("graphtype")) targetNames=parseDataNames(settingName, targets, NumericDataset.class);
        else if (settingName.equalsIgnoreCase("range")) targetNames=parseDataNames(settingName, targets, NumericDataset.class);
        else if (settingName.equalsIgnoreCase("multicolor")) targetNames=parseDataNames(settingName, targets, RegionDataset.class);
        else if (settingName.equalsIgnoreCase("regionborder")) targetNames=parseDataNames(settingName, targets, RegionDataset.class);        
        else if (settingName.equalsIgnoreCase("expand") || settingName.equalsIgnoreCase("contract") || settingName.equalsIgnoreCase("expanded")) targetNames=parseDataNames(settingName, targets, RegionDataset.class);
        else if (settingName.equalsIgnoreCase("showScore") || settingName.equalsIgnoreCase("showOrientation") || settingName.equalsIgnoreCase("showStrand")) targetNames=parseDataNames(settingName, targets, RegionDataset.class);
        else if (settingName.equalsIgnoreCase("motifTrack") || settingName.equalsIgnoreCase("moduleTrack")) targetNames=parseDataNames(settingName, targets, RegionDataset.class);
        else if (settingName.equalsIgnoreCase("display")) targetNames=parseDataNames(settingName, targets);
        else if (settingName.equalsIgnoreCase("sort")) targetNames=parseSortParameters(targets);      
        else targetNames=parseDataNames(settingName, targets, FeatureDataset.class);
    }
    
    
    private boolean isMotifSetting(String settingName) {
      return (settingName.equalsIgnoreCase("motifcolor")                     
           || settingName.equalsIgnoreCase("motifvisible")                                                                                                                               
        );           
    } 
    private boolean isModuleSetting(String settingName) {
      return (settingName.equalsIgnoreCase("modulecolor")                     
           || settingName.equalsIgnoreCase("modulevisible")                                                                 
        );           
    }    
    private boolean isSequenceSetting(String settingName) {
      return (settingName.equalsIgnoreCase("label") 
           || settingName.equalsIgnoreCase("labelcolor")    
           || settingName.equalsIgnoreCase("sequencevisible")   
           || settingName.equalsIgnoreCase("scale")
           || settingName.equalsIgnoreCase("orientation")              
        );           
    }   
      
    @SuppressWarnings("unchecked")
    /** 
     * Given a comma-separated string with data object names, this method will
     * split the string into a String[]. The string can contain a wildcard (*)
     * and the returned list will then include names of all known data objects 
     * of the specified typeclass that match the search string
     * 
     * @throws ExecutionError
     */    
    private String[] parseDataNames(String settingName, String datanames, Class typeclass) throws ExecutionError {
        MotifLabEngine engine=getMotifLabClient().getEngine();
        ArrayList<String> list=null;        
        if (datanames.contains("*") && !datanames.endsWith(":*")) { // using wildcard
            if (typeclass==null) throw new ExecutionError("Wildcards can not be used for display setting '"+settingName+"'");
            list=engine.getNamesForAllDataItemsOfType(typeclass); 
            if (!datanames.equals("*")) { //
                if (datanames.endsWith("*")) {
                    String prefix=datanames.substring(0,datanames.indexOf('*'));
                    list=engine.filterNamesWithoutPrefix(list,prefix);
                } else if (datanames.startsWith("*")) {
                    String suffix=datanames.substring(1);
                    list=engine.filterNamesWithoutSuffix(list,suffix);
                }
            }
        } else { // no general data name wildcards (but it could include region-type wildcards on the form "regiontrack:*")  
            if (typeclass!=null) {            
                String[] splitstring=datanames.trim().split("\\s*,\\s*");
                list=new ArrayList<String>(splitstring.length);  
                for (String name:splitstring) { // check the list
                    if (name.isEmpty()) continue;                   
                    if (force) {list.add(name);continue;}
                    Data data=engine.getDataItem(name);
                    if (data==null) throw new ExecutionError("Unknown data item '"+name+"' for display setting '"+settingName+"'");
                    if (data instanceof DataCollection) {
                        Class membersclass=((DataCollection)data).getMembersClass();
                        if (!membersclass.equals(typeclass)) throw new ExecutionError("'"+name+"' is a Collection of a type which can not be used for display setting '"+settingName+"'");
                        list.addAll(((DataCollection)data).getValues());
                    } else if (!typeclass.isAssignableFrom(data.getClass())) throw new ExecutionError("Display setting '"+settingName+"' can not be applied to '"+name+"'");
                    else list.add(name);
                }
            } else { // no type class
                if (datanames.contains(":") && datanames.contains("*") && datanames.indexOf('*')>datanames.indexOf(':')) {
                   String datasetname=datanames.substring(0,datanames.indexOf(":"));
                   Data data=engine.getDataItem(datasetname);
                   if (data==null) throw new ExecutionError("Unknown data item '"+datasetname+"' for display setting '"+settingName+"'");
                   else if (!(data instanceof RegionDataset)) throw new ExecutionError("Dataset must refer to a Region Dataset when using \"<dataset>:* \" option. However,  '"+datasetname+"' is a "+data.getDynamicType());
                   else {
                       String namestring=datanames.substring(datanames.indexOf(":"));
                       HashSet<String> alltypes=((RegionDataset)data).getAllRegionTypes();
                       if (namestring.equals("*")) {
                          // keep alltypes as is
                       } else if (namestring.endsWith("*")) {
                          String prefix=namestring.substring(0, namestring.indexOf('*'));
                          Iterator<String> iterator=alltypes.iterator();
                          while (iterator.hasNext()) {
                              String string=iterator.next();
                              if (!string.startsWith(prefix)) iterator.remove();
                          }
                       } else if (namestring.startsWith("*")) {
                          String suffix=namestring.substring(1);
                          Iterator<String> iterator=alltypes.iterator();
                          while (iterator.hasNext()) {
                              String string=iterator.next();
                              if (!string.endsWith(suffix)) iterator.remove();
                          }
                       } else {
                          boolean addback=alltypes.contains(namestring);
                          alltypes.clear();
                          if (addback) alltypes.add(namestring);
                       }
                       String[] names=new String[alltypes.size()];
                       names=alltypes.toArray(names);
                       return names;
                   }
                } else return datanames.trim().split("\\s*,\\s*");
            }
        }
        String[] datalist=new String[list.size()];
        datalist=list.toArray(datalist);           
        return datalist;
    }    
    
    @SuppressWarnings("unchecked")
    /** 
     * Given a comma-separated string with data object names, this method will
     * split the string into a String[] and return it after checking that every
     * name corresponds to a known data item
     * @throws ExecutionError if name checking is forced and some of the data items do not exist
     */
    private String[] parseDataNames(String settingName, String datanames) throws ExecutionError {
        String[] splitstring=datanames.trim().split("\\s*,\\s*");
        if (!force) { // check the names
            MotifLabEngine engine=getMotifLabClient().getEngine();
            for (String name:splitstring) {
                Data data=engine.getDataItem(name);
                if (data==null) throw new ExecutionError("Unknown data item '"+name+"' for display setting '"+settingName+"'");
            }
        }
        return splitstring;
    }    

    @SuppressWarnings("unchecked")
    /**
     * Parses a parameter for the sort display settings and returns a String[] with three entries
     * [0] sort mode 
     * [1] sort mode parameter (could be null)
     * [2] group by data object name
     * @throws ExecutionError If the expression is unparsable or if any required values are missing
     */
    private String[] parseSortParameters(String string) throws ExecutionError {
        String[] returnValue=new String[]{null,null,null};
        string=string.trim();
        String[] splitstring=string.trim().split("\\s*,\\s*");
        if (splitstring==null || splitstring.length==0) throw new ExecutionError("Unable to parse sort mode");
        if (splitstring.length>=2) { // has group by option
           if (splitstring[1].startsWith("group by ")) splitstring[1]=splitstring[1].substring("group by ".length());
           else if (splitstring[1].startsWith("group by:")) splitstring[1]=splitstring[1].substring("group by:".length());
           returnValue[2]=splitstring[1].trim();
           if (returnValue[2].isEmpty()) returnValue[2]=null;
        } 
        if (splitstring[0].contains(":")) {
            String[] splitfirst=splitstring[0].trim().split("\\s*:\\s*");
            returnValue[0]=splitfirst[0].trim();
            returnValue[1]=splitfirst[1].trim();
            if (returnValue[1].isEmpty()) returnValue[1]=null;
        } else returnValue[0]=splitstring[0].trim();
        if (returnValue[0].isEmpty()) throw new ExecutionError("Missing sort mode");
        if (!SortSequencesDialog.isValidSortOption(returnValue[0]))  throw new ExecutionError("Not a valid sort mode '"+returnValue[0]+"'");
        if (SortSequencesDialog.requiresSortObject(returnValue[0]) && returnValue[1]==null)  throw new ExecutionError("Sort mode '"+returnValue[0]+"' requires additional parameter in the format '"+returnValue[0]+":<dataobject>'");       
        return returnValue;
    }    
      
    private void displayDataItem(final Data target, final MotifLabGUI gui) {
         if (gui.getMotifsPanel().holdsType(target)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gui.getMotifsPanel().showPrompt(target,true,false);
                }
            });                             
        } else if (gui.getDataObjectsPanel().holdsType(target)) {
             SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gui.getDataObjectsPanel().showPrompt(target);
                }
            });                            
        }        
    }
    

    private double[] parseRangeValues(String value) throws ExecutionError {
        if (value.toLowerCase().equals("scale to fit") || value.toLowerCase().equals("to fit")) return null;
        String[] parts=value.trim().split("\\s*,\\s*");
        if (parts.length<2 || parts.length>3) throw new ExecutionError("Range value should have the format: \"min, max [,baseline]\" or \"scale to fit\"");
        double[] result=new double[parts.length];
        for (int i=0;i<parts.length;i++) {
            try {
                result[i]=Double.parseDouble(parts[i]);
            } catch (NumberFormatException e) {
                throw new ExecutionError("Unable to parse list of expected numeric values: "+value);
            }
        }
        return result;       
    }
    
    private void setNewRangeValues(NumericDataset data, double newmin, double newmax, double baseline) {
        if (newmax > newmin) {
            data.setBaselineValue(baseline);
            data.setMinAllowedValue(newmin);
            data.setMaxAllowedValue(newmax);
            data.notifyListenersOfDataUpdate();
            double actualNewMin = data.getMinAllowedValue();
            double actualNewMax = data.getMaxAllowedValue();
            String msg = "Visualized range for " + data.getName() + " updated to [" + actualNewMin + "," + actualNewMax + "] ";
            if (actualNewMin != newmin || actualNewMax != newmax) {
                msg += "   (the range was expanded to include the baseline value)";
            }
            getMotifLabClient().logMessage(msg);
        } else getMotifLabClient().logMessage("The new maximum value should be larger than the minimum value");
    }
    
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[DisplaySettingTask] ===== "+getTaskName()+" =====  (Line: "+getLineNumber()+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);
        if (verbosity>=3) {
            org.motiflab.engine.protocol.StandardProtocol protocol=new org.motiflab.engine.protocol.StandardProtocol(MotifLabEngine.getEngine());
            MotifLabEngine.debugOutput(" Command: "+getCommandString(protocol),indentLevel);
        }
        if (verbosity>1) MotifLabEngine.debugOutput("-------------------------------------------[End DisplaySettingTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }       
    
}
