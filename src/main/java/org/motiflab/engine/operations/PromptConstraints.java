
package org.motiflab.engine.operations;

import java.util.Arrays;
import java.util.HashMap;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.protocol.ParseError;

/**
 * This class represents constraints that can be applied to the "prompt" operation to control
 * which values are allowed to be selected (e.g. numeric range or specific options) 
 * and how the selections are presented to the user (suggested widget to use)
 * @author kjetikl
 */
public class PromptConstraints {
    public static final String ALLOWED_VALUES="allowed_values";
    public static final String MIN_VALUE="min_value";
    public static final String MAX_VALUE="max_value";
    public static final String STEP_VALUE="step_value"; 
    public static final String WIDGET="widget"; // which type of widget to use     
    
    public static final String MENU="menu";
    public static final String LIST="list";
    public static final String TEXTBOX="textbox";
    public static final String SLIDER="slider";
    public static final String SPINNER="spinner";    
        
    private Class type=null;
    private HashMap<String,Object> map=null;    
    
    
   /** 
     * 
     * @param data A data object or a class type (subtype of Data)
     * @param constraints 
     */
    public PromptConstraints(Object data, String constraints) throws ParseError {
         if (data instanceof Data) {
             type=((Data)data).getClass();
         } else if (data instanceof Class) {
             type=(Class)data;
         } else type=null;
         map=new HashMap<>();
         if (type!=null) parseConstraints(constraints);
    }
    
    
    public Class getType() {
        return type;
    }
    
    public Object getValue(String key) {
        return map.get(key);
    }
    
    public void setValue(String key, Object value) {
        map.put(key, value);
    }
    
    /**
     * Returns a string on the form "[min:max]" reporting the allowed value range
     * or NULL if neither min nor max is specified
     * @return 
     */
    public String getNumericRangeAsString() {
         Object minValue=map.get(MIN_VALUE);
         Object maxValue=map.get(MAX_VALUE);   
         if (minValue==null && maxValue==null) return null; // no range is specified
         if (minValue instanceof Double || maxValue instanceof Double) { // if one of the limits are missing, default for that limit
              if (minValue==null) minValue=-Double.MAX_VALUE;
              if (maxValue==null) maxValue=Double.MAX_VALUE;           
         } else { // assume integers
             if (minValue==null) minValue=Integer.MIN_VALUE;
             if (maxValue==null) maxValue=Integer.MAX_VALUE;
         }
         return "["+minValue+":"+maxValue+"]";
    }
    
    public Object[] getAllowedValues() {
        return (Object[])map.get(ALLOWED_VALUES);
    }
    
    /** Returns NULL if the value provided as argument to this method is allowed according to the constraints set forth by this object. 
     *  If the argument value is in violation if the constraints, a single String object is returned containing an explanation for why the value is not allowed
     */
    public String isValueAllowed(Object value, MotifLabEngine engine) {
        if (type==NumericVariable.class) {   
            if (!(value instanceof Number)) return "Value is not a number";
            if (map.get(ALLOWED_VALUES) instanceof Integer[]) {
                Integer[] list=(Integer[])map.get(ALLOWED_VALUES);
                int intvalue=((Number)value).intValue();
                if (value instanceof Integer) {}
                else if ((double)intvalue!=((Number)value).doubleValue()) return "Value must be an integer"; // not an integer
                for (int allowed:list) {if (intvalue==allowed) return null;}
                return "Value is not allowed";
            } else if (map.get(ALLOWED_VALUES) instanceof Double[]) {
                 double dvalue=((Number)value).doubleValue();
                 Double[] list=(Double[])map.get(ALLOWED_VALUES);
                 for (double allowed:list) {if (dvalue==allowed) return null;}
                 return "Value is not allowed";             
            } else if (map.get(ALLOWED_VALUES) instanceof Object[]) { // list of numbers that could also contain references to Numeric Variables. Treat all numbers as doubles
                 double dvalue=((Number)value).doubleValue();
                 Object[] list=(Object[])map.get(ALLOWED_VALUES);
                 for (Object allowed:list) {
                     Double thisValue=null;
                     if (allowed instanceof Number) thisValue=((Number)allowed).doubleValue();
                     else if (allowed instanceof String) thisValue=getValueFromString((String)allowed, engine); 
                     if (thisValue!=null && dvalue==thisValue.doubleValue()) return null;
                 }
                 return "Value is not allowed";             
            } else { // range values
                 Object minValue=map.get(MIN_VALUE);
                 Object maxValue=map.get(MAX_VALUE);   
                 Object stepValue=map.get(STEP_VALUE); // this is not checked .... It could be difficult unless the values are integers           
                 double dvalue=((Number)value).doubleValue();
                 if (minValue instanceof String) minValue=getValueFromString((String)minValue, engine); // reference to Numeric Variable?
                 if (maxValue instanceof String) maxValue=getValueFromString((String)maxValue, engine); // reference to Numeric Variable?
                 if (minValue instanceof Number && dvalue<((Number)minValue).doubleValue()) return "Value is smaller than minimum ("+((Number)minValue)+")";
                 if (maxValue instanceof Number && dvalue>((Number)maxValue).doubleValue()) return "Value is greater than maximum ("+((Number)maxValue)+")";
                 return null;
            }
        } else if (type==TextVariable.class) {
            if (value instanceof String) {
                Object allowed=map.get(ALLOWED_VALUES);
                if (allowed instanceof String[]) {
                    if (((String[])allowed).length==0) return null; // all values are allowed
                    if (((String[])allowed).length==1 && engine.getDataItem(((String[])allowed)[0], TextVariable.class)!=null) { // this is a reference to a text variable
                        TextVariable temp=(TextVariable)engine.getDataItem(((String[])allowed)[0], TextVariable.class);
                        if (temp!=null) {
                            for (String string:temp.getAllStrings()) { // check against explicit list of values
                                if (string.equals((String)value)) return null;
                            }  
                            return "Value is not allowed";
                        }
                    }
                    for (String string:(String[])allowed) { // check against explicit list of values
                        if (string.equals((String)value)) return null;
                    }
                    return "Value is not allowed";
                } else return null; // no constraints
            } else return "Value is not of correct type";
        } else return null; // no other types can have constraints (at least not for now)           
    }
    
    /** Returns a Double if the provided string is either a Numeric Variable or a literal double, 
      * else returns NULL
      */
    private Double getValueFromString(String dataname, MotifLabEngine engine) {
        NumericVariable var=(NumericVariable)engine.getDataItem(dataname,NumericVariable.class);
        if (var!=null) return var.getValue();
        try {
            double value=Double.parseDouble(dataname);
            return value;
        } catch (Exception e) {}
        return null;
    }
    
    /**
     * Returns a representation of this PromptConstraints as a String that can be incorporated into command lines (can be empty or NULL)
     * @return 
     */
    public String getConstraintString() {
        String widget="";
        if (map.containsKey(WIDGET)) {
            String preferredWidget=(String)map.get(WIDGET);
                 if (preferredWidget.equalsIgnoreCase(MENU)) widget="M";
            else if (preferredWidget.equalsIgnoreCase(LIST)) widget="L";
            else if (preferredWidget.equalsIgnoreCase(TEXTBOX)) widget="T";
            else if (preferredWidget.equalsIgnoreCase(SLIDER)) widget="S";
            else if (preferredWidget.equalsIgnoreCase(SPINNER)) widget="R";                 
        }
        if (type==NumericVariable.class) {            
            if (map.get(ALLOWED_VALUES) instanceof Integer[]) {
                Integer[] list=(Integer[])map.get(ALLOWED_VALUES);
                return "{"+MotifLabEngine.splice(Arrays.asList(list),",")+"}"+widget;
            } else if (map.get(ALLOWED_VALUES) instanceof Double[]) {
                 Double[] list=(Double[])map.get(ALLOWED_VALUES);
                 return "{"+MotifLabEngine.splice(Arrays.asList(list),",")+"}"+widget;                
            } else if (map.get(ALLOWED_VALUES) instanceof Object[]) {
                 Object[] list=(Object[])map.get(ALLOWED_VALUES);
                 return "{"+MotifLabEngine.splice(Arrays.asList(list),",")+"}"+widget;                
            } else { 
              Object minValue=map.get(MIN_VALUE);
              Object maxValue=map.get(MAX_VALUE);   
              Object stepValue=map.get(STEP_VALUE);            
              if (minValue==null && maxValue==null && stepValue==null) return null;
              if (stepValue!=null) return "["+((minValue!=null)?minValue:"*")+":"+((maxValue!=null)?maxValue:"*")+":"+((stepValue!=null)?stepValue:"*")+"]"+widget;
              else return "["+((minValue!=null)?minValue:"*")+":"+((maxValue!=null)?maxValue:"*")+"]"+widget;
            }
        } else if (type==TextVariable.class) {
            Object allowed=map.get(ALLOWED_VALUES);
            if (allowed instanceof String[]) return "{\""+MotifLabEngine.splice((String[])allowed, "\",\"")+"\"}"+widget; // quotes are always added
            else return null;
        } else return null; // no other types can have constraints (at least not for now)       
    }    
    
    /**
     * Parses a command string containing constraints information and configures this object accordingly
     * @param constraints
     * @throws ParseError 
     */
    private void parseConstraints(String constraints) throws ParseError {
        if (constraints==null || constraints.isEmpty()) return;
        if (constraints.length()>2 && (constraints.charAt(constraints.length()-2)==']' || (constraints.charAt(constraints.length()-2)=='}'))) {
            char hint=constraints.charAt(constraints.length()-1);
                 if (hint=='M' || hint=='m') map.put(WIDGET, MENU);
            else if (hint=='L' || hint=='l') map.put(WIDGET, LIST);
            else if (hint=='T' || hint=='t') map.put(WIDGET, TEXTBOX);
            else if (hint=='S' || hint=='s') map.put(WIDGET, SLIDER);
            else if (hint=='R' || hint=='r') map.put(WIDGET, SPINNER);                 
            constraints=constraints.substring(0,constraints.length()-1);
        }
        if (type==NumericVariable.class) {            
            if (constraints.startsWith("[") && constraints.endsWith("]") && constraints.contains(":")) {  
                constraints=constraints.substring(1,constraints.length()-1);
                parseAndSetLimits(constraints);                 
            } else if (constraints.startsWith("{") && constraints.endsWith("}")) {
                constraints=constraints.substring(1,constraints.length()-1);
                if (constraints.matches(".*[a-zA-Z].*")) map.put(ALLOWED_VALUES, parseMixedList(constraints)); // List contains letters which would signal possible references to Numeric Variables (treat these as doubles)
                else if (constraints.contains(".")) map.put(ALLOWED_VALUES, parseDoubleList(constraints)); // A decimal sign (dot) signals double values 
                else map.put(ALLOWED_VALUES, parseIntegerList(constraints));                
            } else throw new ParseError("Constraints for Numeric Variables should be on the format: [min:max] , [min:max:step] or {comma-separated list of allowed values}");           
        } else if (type==TextVariable.class) {
            if (constraints.startsWith("{") && constraints.endsWith("}")) {
                constraints=constraints.substring(1,constraints.length()-1);
                if (constraints.isEmpty() || constraints.equals("*")) map.put(ALLOWED_VALUES, new String[0]); // this means "no restrictions" except that the value can only be a single string (not multi-line)
                else map.put(ALLOWED_VALUES, MotifLabEngine.splitOnCommaToArray(constraints));
            } else throw new ParseError("Constraints for Text Variables should be on the format: {comma-separated list of allowed values}");
        } else throw new ParseError("Constraints can only be applied to Numeric or Text Variables");   
    }
    
    
    private Integer[] parseIntegerList(String list) throws ParseError {
        String[] parts=list.trim().split("\\s*,\\s*");
        Integer[] values=new Integer[parts.length];
        for (int i=0;i<parts.length;i++) {
            try {values[i]=Integer.parseInt(parts[i]);}
            catch (NumberFormatException e) {throw new ParseError("Not a valid integer: "+parts[i]);}
        }
        return values;
    }
    
    private Double[] parseDoubleList(String list) throws ParseError {
        String[] parts=list.trim().split("\\s*,\\s*");
        Double[] values=new Double[parts.length];
        for (int i=0;i<parts.length;i++) {
            try {values[i]=Double.parseDouble(parts[i]);}
            catch (NumberFormatException e) {throw new ParseError("Not a valid numeric value: "+parts[i]);}
        }
        return values;
    }  
    
    private Object[] parseMixedList(String list) throws ParseError {
        String[] parts=list.trim().split("\\s*,\\s*");
        Object[] values=new Object[parts.length];
        for (int i=0;i<parts.length;i++) {
            try {values[i]=Double.parseDouble(parts[i]);} // insert parsed double value
            catch (NumberFormatException e) {
                values[i]=parts[i]; // insert string directly
            }
        }
        return values;
    }     
    
    private void parseAndSetLimits(String string) throws ParseError {
        boolean integer=true;
        String minString=null;
        String maxString=null;
        String stepString=null;
        String[] parts=string.trim().split("\\s*:\\s*");
        if (string.contains(".")) integer=false; // dot is decimal sign. If any of the numbers are double (or references) then all should be treated as such, else all should be treated as integers
        if (parts.length<2 || parts.length>3) throw new ParseError("Min and max values should be defined on the format:  [min:max] or [min:max:step]");
        minString=parts[0];
        maxString=parts[1];
        Double minValue=null;
        if (parts.length==3) stepString=parts[2];
        if (minString.isEmpty() || minString.equals("*")) map.remove(MIN_VALUE); // min value not set explicitly
        else {
            try {
                Double value=Double.parseDouble(minString); 
                if (integer) map.put(MIN_VALUE, new Integer(value.intValue())); else map.put(MIN_VALUE,value);
                minValue=value;
            } catch (NumberFormatException e) {
                // This could be a reference to a Numeric Variable. I will allow it "as is" for now and check back when needed
                map.put(MIN_VALUE, minString);
                // throw new ParseError("Not a valid numeric value: "+minString);
            }           
        }
        if (maxString.isEmpty() || maxString.equals("*")) map.remove(MAX_VALUE); // max value not set explicitly
        else {
            try {
                Double value=Double.parseDouble(maxString);
                if (integer) map.put(MAX_VALUE, new Integer(value.intValue())); else map.put(MAX_VALUE,value);
                if (minValue!=null && minValue>value) throw new ParseError("The maximum value must be larger than the minimum value");
            } catch (NumberFormatException e) {
                // This could be a reference to a Numeric Variable. I will allow it "as is" for now and check back when needed    
                map.put(MAX_VALUE, maxString);
                // throw new ParseError("Not a valid numeric value: "+maxString);
            }           
        }         
        if (stepString==null || stepString.isEmpty() || stepString.equals("*")) map.remove(STEP_VALUE); //
        else {
            try {
                Double value=Double.parseDouble(stepString);
                if (integer) map.put(STEP_VALUE, new Integer(value.intValue())); else map.put(STEP_VALUE,value);
                if (value<=0) throw new ParseError("The step value must be larger than zero");            
            } catch (NumberFormatException e) {
                // This could be a reference to a Numeric Variable. I will allow it "as is" for now and check back when needed    
                map.put(STEP_VALUE, stepString);
                // throw new ParseError("Not a valid numeric value: "+stepString);
            }           
        }                  
    }     
    
    /** Output all the contraints to STDERR (for debugging) */
    private void dump() {
        System.err.println("PromptConstraints: "+type);
        if (map!=null) {
            for (String key:map.keySet()) {
                System.err.println(key+" =>  "+map.get(key)); 
            }
        }
    }
}
