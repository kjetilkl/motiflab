/*
 
 
 */

package motiflab.engine.protocol;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.DisplaySettingTask;
import motiflab.gui.VisualizationSettings;


/**
 *
 * @author Kjetil
 */
public class StandardDisplaySettingsParser extends DisplaySettingsParser {

    public StandardDisplaySettingsParser(StandardProtocol protocol) {
        setProtocol(protocol);
    }

    @Override
    public DisplaySettingTask parse(String command) throws ParseError {
           Pattern pattern=Pattern.compile("(@|!|\\$)(\\S+?)\\(\\s*(.*?)\\s*\\)\\s*(=\\s*(\\S.*))?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               String prefix=matcher.group(1);
               String settingName=matcher.group(2);
               String target=(matcher.group(3)==null)?"":matcher.group(3).trim();
               String valueString=(matcher.group(5)==null)?"":matcher.group(5).trim();
               Object value=null;
               if (settingName.equalsIgnoreCase("sleep")||settingName.equalsIgnoreCase("wait")) settingName="pause"; // pause, wait and sleep are synonyms
               if (settingName.equalsIgnoreCase("end")) settingName="stop"; // end and stop are synonyms
               if (isBooleanSetting(settingName)) {
                    value=parseBoolean(settingName, valueString);
               } else if (isColorSetting(settingName))  {
                    value=parseColor(valueString);   
               } else if (isIntegerSetting(settingName))  {
                    value=parseInteger(settingName, valueString);
               } else if (isDoubleSetting(settingName))  {
                    value=parseDouble(settingName, valueString);
               } else if (settingName.toLowerCase().startsWith("show")) {
                    if (settingName.equalsIgnoreCase("show")) settingName="visible";
                    else if (settingName.equalsIgnoreCase("showmotif")) settingName="motifvisible";
                    else if (settingName.equalsIgnoreCase("showmodule")) settingName="modulevisible";
                    else if (settingName.equalsIgnoreCase("showsequence")) settingName="sequencevisible";
                    else if (settingName.equalsIgnoreCase("showregion")) settingName="regionvisible";
                    else throw new ParseError("Unrecognized display setting '"+settingName+"'");
                    if (!valueString.isEmpty()) throw new ParseError("No assignment value allowed for display setting '"+settingName+"'");                   
                    value=Boolean.TRUE;
               } else if (settingName.toLowerCase().startsWith("hide")) {
                    if (settingName.equalsIgnoreCase("hide")) settingName="visible";
                    else if (settingName.equalsIgnoreCase("hidemotif")) settingName="motifvisible";
                    else if (settingName.equalsIgnoreCase("hidemodule")) settingName="modulevisible";
                    else if (settingName.equalsIgnoreCase("hidesequence")) settingName="sequencevisible";
                    else if (settingName.equalsIgnoreCase("hideregion")) settingName="regionvisible";
                    else throw new ParseError("Unrecognized display setting '"+settingName+"'");
                    if (!valueString.isEmpty()) throw new ParseError("No assignment value allowed for display setting '"+settingName+"'");                    
                    value=Boolean.FALSE;
               } else if (settingName.equalsIgnoreCase("expand")) {
                    if (!valueString.isEmpty()) throw new ParseError("No assignment value allowed for display setting 'expand'");
                    settingName="expanded";
                    value=Boolean.TRUE;
               } else if (settingName.equalsIgnoreCase("contract")) {
                    if (!valueString.isEmpty()) throw new ParseError("No assignment value allowed for display setting 'contract'");
                    settingName="expanded";
                    value=Boolean.FALSE;
               } else if (settingName.equalsIgnoreCase("display")) {
                    if (!valueString.isEmpty()) throw new ParseError("No assignment value allowed for display setting '"+settingName+"'");
                    value=valueString;
               } else if (settingName.equalsIgnoreCase("order") || settingName.equalsIgnoreCase("trackorder")) {
                    if (!valueString.isEmpty()) throw new ParseError("No assignment value allowed for display setting '"+settingName+"'");
                    value=valueString;
               } else if (settingName.equalsIgnoreCase("multicolor") || settingName.equalsIgnoreCase("graph") || settingName.equalsIgnoreCase("graphtype")) {
                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                    value=valueString;
               } else if (settingName.equalsIgnoreCase("modulefillcolor") || settingName.equalsIgnoreCase("moduleoutlinecolor")) {
                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                    if (valueString.equalsIgnoreCase("none") || valueString.equalsIgnoreCase("type")) value=valueString;
                    else value=parseColor(valueString);
               } else if (settingName.equalsIgnoreCase("trackbordercolor")) {
                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                    value=parseColor(valueString);
               } else if (settingName.equalsIgnoreCase("regionborder")) {
                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                    value=parseUnknown(valueString);
               } else if (settingName.equalsIgnoreCase("scale")) {
                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                    if (valueString.endsWith("%")) {
                        value=parseDouble(settingName,valueString.substring(0,valueString.length()-1));
                    } else if (valueString.equalsIgnoreCase("ToFit")) value=valueString;
                    else throw new ParseError("Assignment value for '"+settingName+"' must either be \"ToFit\" or a number followed by %");
               } else if (settingName.equalsIgnoreCase("orientation")) {
                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                    if (valueString.equalsIgnoreCase("direct") || valueString.equalsIgnoreCase("reverse") || valueString.equalsIgnoreCase("relative") || valueString.equalsIgnoreCase("from sequence") || valueString.equalsIgnoreCase("opposite") ) value=valueString;
                    else throw new ParseError("Assignment value for '"+settingName+"' must be one of: DIRECT, REVERSE, RELATIVE (or FROM SEQUENCE) or OPPOSITE");
//               } else if (settingName.equalsIgnoreCase("alignment")) {
//                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
//                    if (valueString.equalsIgnoreCase("left") || valueString.equalsIgnoreCase("right") || valueString.equalsIgnoreCase("tss") || valueString.equalsIgnoreCase("none")) value=valueString;
//                    else throw new ParseError("Assignment value for '"+settingName+"' must be one of: LEFT, RIGHT, TSS or NONE");
               } else if (settingName.equalsIgnoreCase("gradient") || settingName.equalsIgnoreCase("gradientfill")) { 
                    if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                    value=parseUnknown(valueString);
               } else if (settingName.equalsIgnoreCase("setup") || settingName.equalsIgnoreCase("refresh")) { 
                   target=""; value="";
               } else if (settingName.equalsIgnoreCase("saveSession")) {
                   target=stripQuotes(target);             
                   if (target.isEmpty()) throw new ParseError("Filename required for '"+settingName+"'");
                   value="";
               } else if (settingName.equalsIgnoreCase("restoreSession")) {
                   target=stripQuotes(target);                                     
                   if (target.isEmpty()) throw new ParseError("Filename required for '"+settingName+"'");
                   value="";
               } else if (settingName.equalsIgnoreCase("saveOutput")) {
                   target=stripQuotes(target);                   
                   if (target.isEmpty()) throw new ParseError("Filename required for '"+settingName+"'");
                   if (target.contains(",")) throw new ParseError("It is only possible to save one OutputData object at a time with '"+settingName+"'");                 
                   valueString=stripQuotes(valueString);                  
                   if (valueString.isEmpty()) throw new ParseError("Filename required for '"+settingName+"'");
                   value=valueString;
               } else if (settingName.equalsIgnoreCase("import")) {
                   target=stripQuotes(target);                                  
                   if (target.isEmpty()) throw new ParseError("Filename required for '"+settingName+"'");
                   value="";
               } else if (settingName.equalsIgnoreCase("sort")) {
                   valueString=valueString.toLowerCase();                                    
                   if (valueString.isEmpty()) throw new ParseError("Sort direction must be specified. Either 'ascending' or 'descending' (asc/desc).");
                   if (!(valueString.startsWith("asc") || valueString.startsWith("desc"))) throw new ParseError("Sort direction should be either 'ascending' or 'descending' (asc/desc)");
                   value=(valueString.startsWith("asc")?Boolean.TRUE:Boolean.FALSE);
               } else if (settingName.equalsIgnoreCase("setting") || settingName.equalsIgnoreCase("option")) {
                   target=stripQuotes(target);                                      
                   if (target.isEmpty()) throw new ParseError("Missing name of setting");
                   value=parseUnknown(valueString);
               } else if (settingName.equalsIgnoreCase("clear")) {
                   target=stripQuotes(target);                                       
                   if (target.isEmpty()) throw new ParseError("Missing specification for 'clear'");
                   value="";
               } else if (settingName.equalsIgnoreCase("log")) {
                   target=stripQuotes(target);                                                          
                   value="";
               } else if (settingName.equalsIgnoreCase("message")) {
                   target=stripQuotes(target);                                                          
                   value=valueString;
               } else if (settingName.equalsIgnoreCase("dump")) { 
                   target=stripQuotes(target);                                       
                   value=null;
               } else if (settingName.equalsIgnoreCase("macro")) { 
                   target=stripQuotes(target);                                    
                   if (target.isEmpty()) throw new ParseError("Missing name of macro");    
                   // if (valueString.isEmpty()) throw new ParseError("Missing macro definition"); // I will allow empty macros
                   value=valueString;
                   if (valueString.contains(target)) throw new ParseError("Self-referencing macros are not allowed");
               } else if (settingName.equalsIgnoreCase("range")) {  
                   if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
                   value=valueString;
               } else if (settingName.equalsIgnoreCase("stop")) {  

               } else throw new ParseError("Unrecognized display setting '"+settingName+"'");

               if (target.isEmpty() && requiresTarget(settingName)) throw new ParseError("Missing target for display setting '"+settingName+"'");
               return new DisplaySettingTask(settingName, target, value, prefix.equals("!"));
           } else throw new ParseError("Unable to parse display setting. Expected format is <setting>(target)=<value>, but got: "+command);
    }
    
    /** Strips enclosing single or double quotes */
    private String stripQuotes(String input) {
       if (input.startsWith("\"") || input.startsWith("'")) input=input.substring(1);
       if (input.endsWith("\"") || input.endsWith("'")) input=input.substring(0,input.length()-1);
       return input;
    }

    /** Returns TRUE if the setting requires target data objects*/
    private boolean requiresTarget(String settingName) { 
        // NOTE: since most settings require targets, I will instead list those that do NOT require targets and then invert the result
        return !(  settingName.equalsIgnoreCase("margin") 
                || settingName.equalsIgnoreCase("updates")
                || settingName.equalsIgnoreCase("setup")
                || settingName.equalsIgnoreCase("refresh")
                || settingName.equalsIgnoreCase("canvas")
                || settingName.equalsIgnoreCase("canvascolor") 
                || settingName.equalsIgnoreCase("trackbordercolor") 
                || settingName.equalsIgnoreCase("dump")        
                || settingName.equalsIgnoreCase("pause")   
                || settingName.equalsIgnoreCase("stop")                 
        ); // Note that return value is inverted!
    }
    
    private boolean isBooleanSetting(String settingName) {
      return (settingName.equalsIgnoreCase("visible")
           || settingName.equalsIgnoreCase("regionvisible")
           || settingName.equalsIgnoreCase("motifvisible")
           || settingName.equalsIgnoreCase("modulevisible")
           || settingName.equalsIgnoreCase("regionvisible")
           || settingName.equalsIgnoreCase("sequencevisible")              
           || settingName.equalsIgnoreCase("expanded")
           || settingName.equalsIgnoreCase("showorientation")
           || settingName.equalsIgnoreCase("showstrand")              
           || settingName.equalsIgnoreCase("showscore")
           || settingName.equalsIgnoreCase("multicolor")              
           || settingName.equalsIgnoreCase("updates")     
           || settingName.equalsIgnoreCase("motifTrack") 
           || settingName.equalsIgnoreCase("moduleTrack")
           || settingName.equalsIgnoreCase("nestedTrack") || settingName.equalsIgnoreCase("linkedTrack")              
       );            
    }
    
    private boolean isColorSetting(String settingName) {
      return (settingName.equalsIgnoreCase("color") 
           || settingName.equalsIgnoreCase("foreground") 
           || settingName.equalsIgnoreCase("fgcolor") 
           || settingName.equalsIgnoreCase("background") 
           || settingName.equalsIgnoreCase("bgcolor") 
           || settingName.equalsIgnoreCase("secondary") 
           || settingName.equalsIgnoreCase("secondarycolor") 
           || settingName.equalsIgnoreCase("baseline") 
           || settingName.equalsIgnoreCase("baselinecolor")   
           || settingName.equalsIgnoreCase("motifcolor") 
           || settingName.equalsIgnoreCase("modulecolor")                        
           || settingName.equalsIgnoreCase("regioncolor")                        
           || settingName.equalsIgnoreCase("label")                        
           || settingName.equalsIgnoreCase("labelcolor")
           || settingName.equalsIgnoreCase("canvas")
           || settingName.equalsIgnoreCase("canvascolor")  
           || settingName.equalsIgnoreCase("trackbordercolor")               
        );           
    }  
    
    private boolean isIntegerSetting(String settingName) {
      return (settingName.equalsIgnoreCase("height") 
           || settingName.equalsIgnoreCase("trackheight") 
           || settingName.equalsIgnoreCase("regionheight") 
           || settingName.equalsIgnoreCase("rowspacing")               
           || settingName.equalsIgnoreCase("margin") 
           || settingName.equalsIgnoreCase("pause")               
        );           
    }

    private boolean isDoubleSetting(String settingName) {
      return false; // no one yet
    }


    private Boolean parseBoolean(String settingName, String valueString) throws ParseError {
             if (valueString.equalsIgnoreCase("TRUE") || valueString.equalsIgnoreCase("YES") || valueString.equalsIgnoreCase("ON")) return Boolean.TRUE;
       else if (valueString.equalsIgnoreCase("FALSE") || valueString.equalsIgnoreCase("NO") || valueString.equalsIgnoreCase("OFF")) return Boolean.FALSE;
       else if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'. Allowed values are YES/TRUE/ON or NO/FALSE/OFF");
       else throw new ParseError("Unrecognized assignment value for display setting '"+settingName+"'. Allowed values are YES/TRUE/ON or NO/FALSE/OFF");     
    } 
    
    private Integer parseInteger(String settingName, String valueString) throws ParseError {
        if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
        try {
            int val=Integer.parseInt(valueString);
            return new Integer(val);
        } catch (NumberFormatException e) {
            throw new ParseError("Unable to parse expected integer value for display setting '"+settingName+"': "+valueString);
        }          
    }
    private Double parseDouble(String settingName, String valueString) throws ParseError {
        if (valueString.isEmpty()) throw new ParseError("Missing assignment value for display setting '"+settingName+"'");
        try {
            double val=Double.parseDouble(valueString);
            return new Double(val);
        } catch (NumberFormatException e) {
            throw new ParseError("Unable to parse expected numeric value for display setting '"+settingName+"': "+valueString);
        }
    }

    public static Color parseColor(String colorstring) throws ParseError {
        return VisualizationSettings.parseColor(colorstring);
    }

    /** Tries to parse the given valueString and returns a Boolean if
     *  the value is boolean, an Integer if the value is integer, a Double
     *  if the value is a double (with decimal point) a Color if the value
     *  is a color or else returns the same string value.
     *  If the valueString is enclosed in quotes they will be stripped.
     */
    public static Object parseUnknown(String valueString) {
       if (valueString.startsWith("\"") || valueString.startsWith("'")) valueString=valueString.substring(1);
       if (valueString.endsWith("\"") || valueString.endsWith("'")) valueString=valueString.substring(0,valueString.length()-1);
       if (valueString.equalsIgnoreCase("TRUE") || valueString.equalsIgnoreCase("YES") || valueString.equalsIgnoreCase("ON")) return Boolean.TRUE;
       else if (valueString.equalsIgnoreCase("FALSE") || valueString.equalsIgnoreCase("NO") || valueString.equalsIgnoreCase("OFF")) return Boolean.FALSE;
       else if (valueString.contains(".")) { // possibly a Double
           try {
              double value=Double.parseDouble(valueString);
              return new Double(value);
           } catch (NumberFormatException e) {} // do nothing just proceed
       }
       try { // integer perhaps?
          int value=Integer.parseInt(valueString);
          return new Integer(value);
       } catch (NumberFormatException e) {} // do nothing just proceed
        try { // a color?
          Color color=parseColor(valueString);
          return color;
       } catch (ParseError e) {} // do nothing just proceed
       return valueString;
    }

}
