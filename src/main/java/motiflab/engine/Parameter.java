/*
 
 
 */

package motiflab.engine;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class represents the specification of a parameter, describing its name, its type, default value
 * and allowed values. A description of the parameter and its implication can also be provided
 * The class only represents a "template" for a parameter. The value itself is not stored in objects of 
 * this class.
 * Object of this class can be used to "export" information about the set of Parameters available for 
 * for instance DataFormats or external programs. 
 * @author kjetikl
 */
public class Parameter {
    
    private HashMap<String,Object> attributes=new HashMap<String, Object>();
    
    
    public Parameter(String name, Class type, Object defaultValue, Object allowedValues, String description) {
        attributes.put("name", name);
        attributes.put("type", type);
        attributes.put("defaultValue", defaultValue);
        attributes.put("allowedValues", allowedValues);
        attributes.put("description", description);       
    }
    
    public Parameter(String name, Class type, Object defaultValue, Object allowedValues, String description, boolean required, boolean hidden) {
        attributes.put("name", name);
        attributes.put("type", type);
        attributes.put("defaultValue", defaultValue);
        attributes.put("allowedValues", allowedValues);
        attributes.put("description", description); 
        attributes.put("required", required); 
        attributes.put("hidden", hidden);         
    }
    
    public Parameter(String name, Class type, Object defaultValue, Object allowedValues, String description, boolean required, boolean hidden, boolean advanced) {
        attributes.put("name", name);
        attributes.put("type", type);
        attributes.put("defaultValue", defaultValue);
        attributes.put("allowedValues", allowedValues);
        attributes.put("description", description); 
        attributes.put("required", required); 
        attributes.put("hidden", hidden);  
        attributes.put("advanced", advanced);
    }    
    
    public Parameter(String name, Class type, Object defaultValue, Object allowedValues, String description, boolean required, boolean hidden, boolean advanced, ArrayList<ParameterCondition> conditions) {
        attributes.put("name", name);
        attributes.put("type", type);
        attributes.put("defaultValue", defaultValue);
        attributes.put("allowedValues", allowedValues);
        attributes.put("description", description); 
        attributes.put("required", required); 
        attributes.put("hidden", hidden);  
        attributes.put("advanced", advanced);
        attributes.put("conditions",conditions);
    }       
    
    public String getName() {return (String)attributes.get("name");}
    public Class getType() {return (Class)attributes.get("type");}
    public Object getDefaultValue() {return attributes.get("defaultValue");}
    public Object getAllowedValues() {return attributes.get("allowedValues");}
    public String getDescription() {return (String)attributes.get("description");}
    
    public String getFilterType() {return (String)attributes.get("filterType");}    
    public void setFilterType(String type) {attributes.put("filterType",type);} 
    
    /** Returns a list of conditions that could affect this parameter (its visibility or value)
     *  Conditions are (usually?) stored with its target parameter and not with the parameter being monitored
     */
    public ArrayList<ParameterCondition> getConditions() {
        if (attributes.containsKey("conditions")) return (ArrayList<ParameterCondition>)attributes.get("conditions"); 
        else return null;
    }   
    
    public void addCondition(ParameterCondition condition) {
        if (!hasConditions()) setConditions(new ArrayList<ParameterCondition>());
        ((ArrayList<ParameterCondition>)attributes.get("conditions")).add(condition);
    }     
    
    public void setConditions(ArrayList<ParameterCondition> conditions) {
        attributes.put("conditions",conditions);
    }     
    
    
    /** Returns TRUE if the parameter is NOT required to have an explicitly set value (default values will be used instead if necessary) */
    public boolean isOptional() {return !isRequired();}
    
    /** Returns TRUE if the parameter is required to have an explicitly set value */
    public boolean isRequired() {
        Object required=attributes.get("required");
        if (required instanceof Boolean) return (Boolean)required;
        else return true; // all parameters are required unless specifically tagged as optional
    }
    
    /** Returns TRUE if the parameter should be hidden. Hidden options are never shown in GUI panels and can not be set by the user (only the system or config files) */
    public boolean isHidden() {
        Object required=attributes.get("hidden");
        if (required instanceof Boolean) return (Boolean)required;
        else return false;      
    }
    
    /** Returns TRUE if the parameter is considered to represent an 'advanced' setting. GUI's can choose whether or not to show advanced parameters or leave them hidden to use default values */
    public boolean isAdvanced() {
        Object required=attributes.get("advanced");
        if (required instanceof Boolean) return (Boolean)required;
        else return false;        
    } 
    
    /** Returns TRUE if the parameter should never be included in a protocol
     *  (this usually means that the parameter only has a role in GUI panels where it controls other parameters through conditional actions)
     */
    public boolean excludeFromProtocol() {
        Object exclude=attributes.get("excludeFromProtocol");
        if (exclude instanceof Boolean) return (Boolean)exclude;
        else return false;        
    }     

    /** Sets the "excludeFromProtocol" attribute of this parameter
     *  If set to TRUE, the parameter will never be included in protocol scripts.
     *  This setting usually applies to parameters that only have a role in GUI panels where they control other parameters through conditional actions
     */
    public void setExcludeFromProtocol(boolean exclude) {
        setAttribute("excludeFromProtocol", exclude);
    }      
    
    /** Returns TRUE if the parameter has associated conditions */
    public boolean hasConditions() {
        Object conditions=attributes.get("conditions");
        if (conditions==null || !(conditions instanceof ArrayList)) return false;
        if (((ArrayList)conditions).isEmpty()) return false;
        else return true;   
    }     
    
    public Object getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }
    
    public void setAttribute(String attributeName, Object value) {
        attributes.put(attributeName, value);
    }    
    
    public boolean hasAttributeValue(String attributeName, Object value) {
        Object attributeValue=attributes.get(attributeName);
        if (attributeValue==null && value==null) return true;
        else if ((attributeValue!=null && value==null) || (attributeValue==null && value!=null)) return false;
        else return attributeValue.equals(value);
    }        
}
