/*
 
 
 */

package org.motiflab.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericVariable;

/**
 * This class implements a set of values for parameters
 * The values are initially set as strings representing the value in question
 * (for instance the string "10.3" to represent the floating point number 10.3 
 * or the string "X" which refers to the name of a Numerical Variable)
 * 
 * When the method resolve() is called, the values for all parameters are replaced
 * with values of their proper type after parsing the initial strings and, 
 * if necessary, resolving any references to variables.
 * 
 * @author kjetikl
 */
public class ParameterSettings implements Cloneable, Serializable {
    private HashMap<String,Object> storage;  // String is the name of the parameter, Object is the value
    
    public ParameterSettings() {
        storage=new HashMap<String,Object>();
    } 
    
    /**
     * Sets the value of the specified parameter
     * @param key
     * @param value
     */
    public void setParameter(String parameterName, Object value) {
        if (value==null) storage.remove(parameterName);
        else storage.put(parameterName, value);
    } 

    /**
     * Gets the value of the specified parameter as a String
     * @param key
     */    
    public String getParameterAsString(String parameterName, Parameter[] parameterFormats) {
        Object value=storage.get(parameterName);
        if (value==null) value=getDefaultValueForParameter(parameterName, parameterFormats);
        if (value==null) return null;
        else if (value instanceof String && ((String)value).isEmpty()) return null; // ?
        else return value.toString();
    }  
    
    /**
     * Returns TRUE if the value of the given parameter is the default value.
     * This will happen either if the parameter has no explicitly assigned value
     * (the method hasAssignedValueForParameter() returns FALSE for the parameter)
     * or if the parameter has an assigned value but the String representation
     * of that value is the same as the String representation of the default value
     * @param parameterName
     * @param parameterFormats
     */    
    public boolean usesDefaultValue(String parameterName, Parameter[] parameterFormats) {
        if (!hasAssignedValueForParameter(parameterName)) return true;
        String stringValue=getParameterAsString(parameterName, parameterFormats);
        String stringDefaultValue=null;
        Object defaultValue=getDefaultValueForParameter(parameterName, parameterFormats);
        if (defaultValue==null) stringDefaultValue=null;
        else if (defaultValue instanceof String && ((String)defaultValue).isEmpty()) stringDefaultValue=null; // ?
        else stringDefaultValue=defaultValue.toString();
        if (stringValue==null && stringDefaultValue==null) return true;
        if (stringValue!=null && stringDefaultValue!=null && stringValue.equals(stringDefaultValue)) return true;
        return false;
    }      
    
    /**
     * Gets the resolved value of the specified parameter. If there is no explicit value
     * set for this parameter, the parameters default value (if any) is returned
     * @param key
     */    
    public Object getResolvedParameter(String parameterName, Parameter[] parameterFormats, MotifLabEngine engine) throws ExecutionError {
        Object value=storage.get(parameterName);
        if (value==null) return getDefaultValueForParameter(parameterName, parameterFormats);
        if (!(value instanceof String)) return value; // already "resolved"
        Class type=getTypeForParameter(parameterName, parameterFormats);
        String valueAsString=(String)value;
        if (valueAsString.trim().isEmpty()) return getDefaultValueForParameter(parameterName, parameterFormats);
        if (type==null) {
            return null;
        } else if (type.equals(String.class)) {
            return (String)value;
        } else if (type.equals(Integer.class)) {
            try {
                int intval=Integer.parseInt(valueAsString);
                return new Integer(intval);
            } catch (NumberFormatException ne) {
                Data dataitem=engine.getDataItem(valueAsString);
                if (dataitem==null) throw new ExecutionError("Unknown data item: "+valueAsString);
                else if (dataitem instanceof NumericVariable) return new Integer((int)((NumericVariable)dataitem).getValue().doubleValue());
                else throw new ExecutionError("Value for parameter '"+parameterName+"' is not a valid integer or Numeric Variable");
            }
        } else if (type.equals(Double.class)) {
            try {
                double doubleval=Double.parseDouble(valueAsString);
                return new Double(doubleval);
            } catch (NumberFormatException ne) {
                Data dataitem=engine.getDataItem(valueAsString);
                if (dataitem==null) throw new ExecutionError("Unknown data item: "+valueAsString);
                else if (dataitem instanceof NumericVariable) return new Double(((NumericVariable)dataitem).getValue().doubleValue());
                else throw new ExecutionError("Value for parameter '"+parameterName+"' is not a valid number or Numeric Variable");
            }            
        } else if (type.equals(Boolean.class)) {
            if (valueAsString.equalsIgnoreCase("TRUE") || valueAsString.equalsIgnoreCase("YES")) return Boolean.TRUE;
            else return Boolean.FALSE;
        } else if (NumericMap.class.isAssignableFrom(type)) { // if the type class is Numeric map, create a wrapper map if the actual value is a constant number or numeric variable
            if (valueAsString.isEmpty()) return null;  
            Object data=engine.getNumericDataForString(valueAsString);
            if (data==null) throw new ExecutionError("Value for parameter '"+parameterName+"' is not a valid Numeric Map, Numeric Variable or constant number");
            if (data instanceof NumericMap && !type.equals(NumericMap.class) && !data.getClass().equals(type)) throw new ExecutionError("Value for parameter '"+parameterName+"' is not a Numeric Map of the correct type ("+engine.getTypeNameForDataClass(type)+")");
            if (data instanceof NumericMap) return data;
            if (NumericMap.class.equals(type)) throw new ExecutionError("Unable to create generic Numeric Map from scalar value");
            double defaultvalue=0;
            String newmapname="Map";
            if (data instanceof NumericVariable) {defaultvalue=((NumericVariable)data).getValue();newmapname+="_"+((Data)data).getName();}
            else if (data instanceof Double) {defaultvalue=((Double)data).doubleValue();newmapname+="_"+defaultvalue;}           
            Data newmap=engine.createDataObject(type,newmapname);
            ((NumericMap)newmap).setDefaultValue(defaultvalue);
            return newmap;
        } else  { // some data type probably
            if (valueAsString.isEmpty()) return null;
            Data dataitem=engine.getDataItem(valueAsString);
            if (dataitem==null) throw new ExecutionError("Unknown data item: "+valueAsString);
            else if (type.isInstance(dataitem)) return dataitem;
            else throw new ExecutionError("Value for parameter '"+parameterName+"' is not of correct type");
        } 
    }   

    /**
     * Returns an object representing the default value for the parameter with the given name,
     * or null if no there is no such parameter
     * @param parameterName
     * @param parameterFormats
     * @return
     */
    public Object getDefaultValueForParameter(String parameterName, Parameter[] parameterFormats) {
        Object def=null;
        if (parameterFormats==null) return null;
        for(Parameter item:parameterFormats) {
            if (item.getName().equals(parameterName)) {def=item.getDefaultValue();break;}
        }
        return def;
    }  
    
    /**
     * Returns an object representing the type for the parameter with the given name,
     * or null if no there is no such parameter. The object should either be of type "Class"
     * or "Class[]"
     * @param parameterName
     * @param parameterFormats
     * @return
     */
    public Class getTypeForParameter(String parameterName, Parameter[] parameterFormats) {
        Class type=null;
        if (parameterFormats==null) return null;
        for(Parameter item:parameterFormats) {
            if (item.getName().equals(parameterName)) {type=item.getType();break;}
        }
        return type;
    }    

    /**
     * Returns TRUE if a parameter with the given name is present among the list
     * of parameters and this parameter is a HIDDEN parameter or FALSE if it is
     * not hidden or not among the listed parameters
     * @param parameterName
     * @param parameterFormats
     * @return
     */
    public boolean isHidden(String parameterName, Parameter[] parameterFormats) {
        if (parameterFormats==null) return false;
        for(Parameter item:parameterFormats) {
            if (item.getName().equals(parameterName)) {return item.isHidden();}
        }
        return false;
    }

    /**
     * Returns TRUE if a parameter with the given name is present among the list
     * of parameters and this parameter is an ADVANCED parameter or FALSE if it is
     * not advanced or not among the listed parameters
     * @param parameterName
     * @param parameterFormats
     * @return
     */
    public boolean isAdvanced(String parameterName, Parameter[] parameterFormats) {
        if (parameterFormats==null) return false;
        for(Parameter item:parameterFormats) {
            if (item.getName().equals(parameterName)) {return item.isAdvanced();}
        }
        return false;
    }

    /**
     * Returns a list with the names of all (specified) parameters
     * @return
     */
    public String[] getParameters() {
        Set<String>keys=storage.keySet();
        String[]list=new String[keys.size()];
        return storage.keySet().toArray(list);        
    }
    
    /**
     * Returns true if the given parameter has an explicitly assigned value
     * @param parameter
     * @return 
     */
    public boolean hasAssignedValueForParameter(String parameter) {
        return storage.containsKey(parameter);       
    }    
    

    /** Clears all parameter key,value-pairs stored in this object */
    public void clear() {
        storage.clear();
    }

    /** Dumps the contents of this settings object to STDERR*/
    public void debug(int indentation) {
        Set<String>keys=storage.keySet();
        for (String key:keys) {
            MotifLabEngine.debugOutput("  * "+key+" => "+storage.get(key),indentation);
        }
    }


    @Override
    public Object clone()  {
        ParameterSettings newclone=new ParameterSettings();
        Set<String>keys=storage.keySet();
        for (String key:keys) {
            newclone.storage.put(key, storage.get(key));
        }
        return newclone;
    }

    
    /** Applies all the conditions registered with the provided parameters to this set of parameter values
     *  These conditions could possibly result in changes in some parameter values based on the values of others.
     *  Note that the ParametersPanel GUI widget will initialize the settings in the panel based on the conditions by itself.
     *  This method should only be used when settings are used in a non-GUI setting (e.g. when executing a protocol)
     */
    public void applyConditions(Parameter[] parameterFormats) {
        MotifLabEngine engine=MotifLabEngine.getEngine();
        ArrayList<ParameterCondition> conditions=new ArrayList<ParameterCondition>();
        HashMap<String,Parameter> parametersMap=new HashMap<>();
        HashMap<String,ArrayList<ParameterCondition>> parameterConditions=new HashMap<>(); // for each parameter, this will contain the list of conditions monitoring that parameter      
        for (Parameter par:parameterFormats) {
            parametersMap.put(par.getName(), par);
            if (par.hasConditions()) {
                for (ParameterCondition condition:par.getConditions()) {
                    String monitored=condition.getMonitoredParameter();
                    if (!parameterConditions.containsKey(monitored)) parameterConditions.put(monitored, new ArrayList<ParameterCondition>()); // create new conditions list for the monitoried parameter
                    parameterConditions.get(monitored).add(condition);
                    conditions.add(condition);
                }
            }        
        }
        if (conditions.isEmpty()) return; // no conditions to apply
        // while list of conditions is not empty: go through all conditions in turn
        // if (condition is satisfied) => perform action and remove it from the list. This condition has now been processed to completion
        // if (condition is not satisfied) => it could perhaps be satisfied later depending on other actions. Keep it in for now.
        // at the end of each iteration through the list: if no conditions were satisfied => break and finish (since no other conditions can be satisfied from now on)
        boolean finished=false;
        while (!finished) {
            ArrayList<ParameterCondition> newConditions=new ArrayList<>(); // conditions for parameters updated in response to the previous actions
            for (ParameterCondition condition:conditions) {
                String monitored=condition.getMonitoredParameter();
                Object[] pair=getValueAndClassForParameter(parametersMap.get(monitored),engine);
                Object value=pair[0];
                Class valueClass=(Class)pair[1];
                boolean isSatisfied=condition.isSatisfied(value, valueClass, engine);
                String affectedParameterName=triggerAction(condition,isSatisfied);
                if (affectedParameterName!=null) { // the action has potentially updated the value of this parameter
                   Object affectedConditions=parameterConditions.get(affectedParameterName);
                   if (affectedConditions instanceof ArrayList && !((ArrayList)affectedConditions).isEmpty()) newConditions.addAll((ArrayList<ParameterCondition>)affectedConditions);                   
                }
           } 
           if (newConditions.isEmpty()) finished=true; // no more actions can be triggered. We are done!
           else conditions=newConditions;
        }   
    }
    
    private Object[] getValueAndClassForParameter(Parameter parameter, MotifLabEngine engine) {
        String parameterName=parameter.getName();
        Class parameterclass=parameter.getType();    
        Object value=storage.get(parameterName);
        Class selectedClass=null;
        if (Data.class.isAssignableFrom(parameterclass)) { // parameter refers to a data object
            if (value instanceof String && ((String)value).isEmpty()) value=null;
            else {
               selectedClass=engine.getClassForDataItem((String)value);
               // if selectedClass is null then no known data item was selected
            }
        } else if (parameterclass==Boolean.class) {
            selectedClass=parameterclass;
        } else if (parameterclass==Double.class) {
            if (value instanceof String) { // this could be the name of a Numeric Variable or a literal number written as text
                if (((String)value).isEmpty()) value=null;
                else {
                   selectedClass=engine.getClassForDataItem((String)value);
                   if (selectedClass==null) { // not a data object. Check if it is a number
                       try {
                           double number=Double.parseDouble((String)value);
                           value=new Double(number);
                           selectedClass=Double.class;
                       } catch (NumberFormatException e) {}
                   }
                } // non-empty String                   
            } else selectedClass=parameterclass;
        } else if (parameterclass==Integer.class) {
            if (value instanceof String) { // this could be the name of a Numeric Variable or a literal number written as text
                if (((String)value).isEmpty()) value=null;
                else {
                   selectedClass=engine.getClassForDataItem((String)value);
                   if (selectedClass==null) { // not a data object. Check if it is a number
                       try {
                           int number=Integer.parseInt((String)value);
                           value=new Integer(number);
                           selectedClass=Integer.class;
                       } catch (NumberFormatException e) {}
                   }
                } // non-empty String                   
            } else selectedClass=parameterclass;
        } else if (parameterclass==String.class) {
            if (value instanceof String && ((String)value).isEmpty()) value=null;
            selectedClass=parameterclass;
        } 
        return new Object[]{value,selectedClass};
    }     
    
    /** If the condition action (possibly) changes the value of a parameter, 
     *  the name of this parameter will be returned, else NULL
     */
    private String triggerAction(ParameterCondition condition, boolean satisfied) {
       String affected=null;
       Object[] action=condition.getAction(satisfied);
       if (action!=null) {              
           String affectedParameterName=(String)action[0];
           String actionCommand=(String)action[1];
           Object newValue=action[2];
           if (actionCommand.equalsIgnoreCase("setValue")) {
               storage.put(affectedParameterName,newValue); // this is a string here but will eventually be resolved to proper type
               affected=affectedParameterName;
           } else if (actionCommand.equalsIgnoreCase("setToValueOf")) {
               Object resultValue=storage.get((String)newValue);
               storage.put(affectedParameterName,resultValue);
               affected=affectedParameterName;
           }
       } 
       return affected;
    }

    
    private static final long serialVersionUID = 8857516165055182045L;
  
}
