/*
 * The ParameterCondition class can be used to connect conditions to 
 * Parameters that monitor and react to settings of other parameters
 * by either showing/hiding this parameter in the ParametersPanel or 
 * by changing its value
 */
package motiflab.engine;

/**
 *
 * @author kjetikl
 */
public class ParameterCondition {
    public static final int VALUE=0;
    public static final int SELECTED=1;
    public static final int TYPE=2;
    public static final int UPDATED=3;    
     
    private String parameterName=null; // this is a reference to the target parameter associated with condition. (this is the parameter that will be affected by an event in some other parameter (or the same))
    private String monitoredParameterName=null; // this is the parameter that the condition monitors in order to respond to changes
    private String ifClause=null; // the condition that should be satisfied by the monitored parameter in order to trigger a repons in the monitoring parameter
    private String thenClause=null; // the action to perform if the condition is met
    private String elseClause=null; // the action to perform if the condition is not met
    private int type=SELECTED;
    private String[] allowedValues=null;
    private boolean negation=false; // set to true if to negate condition (i.e. "ifNot" instead of "if")    
     
    /**
     * 
     * @param parameterName The parameter that will be affected by the then- or else-clause if this is not explicitly specific in those strings (with the syntax "targetParameter:action")
     * @param monitoredParameter The parameter whose value is evaluated by the if-clause expression. This will default to the target parameter if null or empty
     * @param ifClause. The clause to satisfy, this could be "selected", "updated", "value=<allowed values>" or "type=<allowed types>"
     * @param not set to TRUE if the condition should be negated
     * @param thenClause The action to be taken if the if-clause expression is satisfied. Allowed actions are: "show", "hide", "setValue=<somevalue>" or "setToValueOf=<parameter>"
     * @param elseClause The action to be taken if the if-clause expression is NOT satisfied. Allowed actions are: "show", "hide", "setValue=<somevalue>" or "setToValueOf=<parameter>"
     * @throws SystemError 
     */
    public ParameterCondition(String parameterName, String monitoredParameter, String ifClause, boolean not, String thenClause, String elseClause) throws SystemError {
         this.parameterName=parameterName;
         if (parameterName==null || parameterName.isEmpty()) throw new SystemError("Missing name of associated parameter in condition");         
         this.monitoredParameterName=(monitoredParameter!=null && !monitoredParameter.isEmpty())?monitoredParameter:parameterName;
         this.negation=not;
         this.ifClause=ifClause;
         this.thenClause=thenClause;
         this.elseClause=elseClause;
         if (ifClause==null || ifClause.isEmpty()) throw new SystemError("Missing if-clause for condition");         
         if (ifClause.toLowerCase().equals("selected")) {
             type=SELECTED;
             allowedValues=null;
         } else if (ifClause.toLowerCase().startsWith("update")) {
             type=UPDATED;
             allowedValues=null;
         } else {
             String[] splitParts=ifClause.trim().split("\\s*=\\s*");
             String typePart=splitParts[0].toLowerCase();
             if (typePart.equals("value")) type=VALUE;
             else if (typePart.equals("type")) type=TYPE;
             else throw new SystemError("Unrecognized if-condition: "+ifClause);
             if (splitParts.length!=2) throw new SystemError("If-condition should be on the format: "+typePart+"=<value>");
             allowedValues=getAllowedValuesFromString(ifClause);
         }
         if (thenClause==null || thenClause.isEmpty()) throw new SystemError("Missing then-clause for condition");         
                          
    }
    
    /**
     * Parses a given string an returns a set of allowed values from this.
     * The string should be on the form: [target:]<type>=<values> where values
     * can be a pipe-separated list
     * @param string
     * @return 
     */
    private String[] getAllowedValuesFromString(String string) {
        int colonpos=string.indexOf(':');
        int equalspos=string.indexOf('=',colonpos);   
        if (equalspos<0 || equalspos+1==string.length()) return new String[0]; // no allowed values
        string=string.substring(equalspos+1);
        return string.split("\\s*\\|\\s*");        
    }
    
    /**
     * 
     * @param targetValue the 'value' of the target parameter (either the name of a data object or a basic value)
     * @param targetClass the actual class of the data object
     * @return 
     */
    public boolean isSatisfied(Object targetValue, Class targetClass, MotifLabEngine engine) {
        boolean satisfied=false;
        if (type==SELECTED) {
            if (targetClass==Boolean.class) satisfied=(Boolean)targetValue;
            else satisfied=(targetValue!=null); // Not a boolean. Check if something is selected (not a blank value)
        } else if (type==UPDATED) {
            satisfied=true;
        } else if (type==TYPE && allowedValues!=null && allowedValues.length>0) {
           String datatype=engine.getTypeNameForDataOrBasicClass(targetClass);
           if (datatype!=null) {
               datatype=datatype.toLowerCase();
               if (datatype.indexOf(' ')>0) datatype=datatype.replace(" ", ""); // remove spaces
               for (String allowed:allowedValues) {
                   if (datatype.equals(allowed.toLowerCase())) {satisfied=true;break;}
               }
           }
        } else if (type==VALUE && allowedValues!=null && allowedValues.length>0) { // this can only be for "direct values" (constants or data names) since values of data items are not resolved dynamically      
            if (targetValue==null) satisfied=false;
            else if (targetClass==Boolean.class) satisfied=compareBoolean(targetValue, allowedValues);
            else if (targetClass!=null && Number.class.isAssignableFrom(targetClass)) satisfied=compareNumeric(targetValue, allowedValues);
            else satisfied=compareStrings(targetValue, allowedValues); 
        } 
        return (negation)?(!satisfied):satisfied;
    }
    
    private boolean compareNumeric(Object value, String[] allowed) {
        double val=0;
        try {
            val= Double.parseDouble(value.toString());         
        } catch (NumberFormatException e) { // the value is not a number, but perhaps the name of a data object
            return compareStrings(value, allowed);
        }
        for (String a:allowed) {
            try {
               double allowedVal=Double.parseDouble(a); 
               if (val==allowedVal) return true;
            } catch (NumberFormatException e) {} // allowed value is not a number, just ignore it
        }      
        return false;
    }
    
    
    private boolean compareBoolean(Object value, String[] allowed) {
        if (value instanceof Boolean) value=value.toString().toLowerCase();
        for (String a:allowed) {
            if (a.equalsIgnoreCase((String)value)) return true;
        }  
        return false;
    }
    private boolean compareStrings(Object value, String[] allowed) {
        String val=value.toString();
        for (String a:allowed) {
            if (a.equals(val)) return true;
        }
        return false;
    }    
    
    /**
     * Based on a given then- or else-clause this parses the string
     * and returns an Object array with 3 values.
     * [0] Name of affected parameter (String)
     * [1] Action command (String)
     * [2] Value (this could be NULL and the type will depend on the action command)
     * @param clause
     * @return 
     */
    private Object[] getAction(String clause) {
        if (clause==null) return null;
        if (clause.contains("=")) {
            String affected=parameterName;
            String[] parts=clause.split("\\s*=\\s*",2);
            String action=parts[0].trim();
            if (action.contains(":")) {
                String[] actionparts=action.split("\\s*:\\s*");
                affected=actionparts[0];
                if (actionparts.length>=2) action=actionparts[1];
            }             
            String value=(parts.length==2)?parts[1]:""; // allow empty values. E.g. "setValue="           
            if (action.equalsIgnoreCase("setValue") || action.equalsIgnoreCase("setToValue")) return new Object[]{affected,"setValue",value};            
            else if (action.equalsIgnoreCase("setToValueOf")) return new Object[]{affected,"setToValueOf",value};            
        } else {
            String affected=parameterName;
            String action=clause;
            if (clause.contains(":")) {
                String[] parts=clause.split("\\s*:\\s*");
                affected=parts[0];
                action=parts[1];
            } 
            if (action.equalsIgnoreCase("show")) return new Object[]{affected,"visibility",Boolean.TRUE};
            else if (action.equalsIgnoreCase("hide")) return new Object[]{affected,"visibility",Boolean.FALSE};
            else return null;                      
        }
        return null; // something was not right
    }
    
    public Object[] getThenAction() {
        return getAction(thenClause);
    }
    
    public Object[] getElseAction() {
        if (elseClause!=null) return getAction(elseClause);
        else return null; 
    }    
    
    /**
     * Returns the "Then" action if the argument is TRUE 
     * or the "Else" action if the argument is FALSE
     * @param conditionSatisfied
     * @return 
     */
    public Object[] getAction(boolean conditionSatisfied) {
        return (conditionSatisfied)?getThenAction():getElseAction();        
    }
    
    
    /** Returns the name of the parameter that this condition applies to */
    public String getParameter() {
        return parameterName;
    }      
    
    /** Returns the name of the parameter that this condition monitors (this could be the same as the associated parameter) */
    public String getMonitoredParameter() {
        return monitoredParameterName;
    }    
    
    public boolean isMonitoringSelf() {
        return monitoredParameterName.equals(parameterName);
    }
    
    /** Returns the 'if' part of the condition as a string */
    public String getIfClause() {
        return ifClause;
    }
    
    /** Returns the 'then' part of the condition as a string */
    public String getThenClause() {
        return thenClause;
    } 
    
    /** Returns the 'else' part of the condition as a string */
    public String getElseClause() {
        return elseClause;
    }       
    
    /** Returns true if the this condition should be negated (i.e. "ifNot" instead of "if")  */
    public boolean isConditionNegated() {
        return negation;
    }
    
    @Override
    public String toString() {
        return parameterName+" monitors "+monitoredParameterName+": if"+((negation)?"Not":"")+"=\""+ifClause+"\" then=\""+thenClause+"\""+((elseClause!=null)?(" else=\""+elseClause+"\""):""); 
    }
    
}
