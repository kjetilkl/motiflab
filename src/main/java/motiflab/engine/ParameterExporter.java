/*
 
 
 */

package motiflab.engine;

/**
 *
 * @author kjetikl
 */
public interface ParameterExporter {
    /** Returns a list of all parameters exported */
    public Parameter[] getParameters();
    
    
    /** Returns a default value for the parameter with the given name */
    public Object getDefaultValueForParameter(String parameterName);
    
    
    /** Returns a reference to the parameter with the given name
     * or NULL if no such parameter exists
     */
    public Parameter getParameterWithName(String parameterName);
}
