/*
 
 
 */

package motiflab.engine.protocol;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;

/**
 * This top class represents parsers for (external) parameter settings
 * 
 * Each subclass is responsible for parsing external parameter setting
 * in one particular protocol language (eg. standard protocol language or XML)
 * 
 * 
 * @author kjetikl
 */
public abstract class ParametersParser {
    
    protected MotifLabEngine engine=null;
    protected Protocol protocol;
    
    public ParametersParser(MotifLabEngine engine) {
        this.engine=engine;
    }
    
    public ParametersParser(){}
    
    /** 
     * Sets the engine for this ParametersParser. This method should always be called 
     * directly after instantiation of new parser with a zero-parameters constructor
     */
    public void setEngine(MotifLabEngine engine) {
        this.engine=engine;
    }
    
    /** 
     * Sets a reference back to the "parent" protocol which ParametersParser might
     * need to query the Protocol for information 
     */
    public void setProtocol(Protocol protocol) {
        this.protocol=protocol;
    }
    
    /**
     * The method parses a string containing a set of parameter settings in the
     * given protocol language. It returns a ParameterSettings object where the 
     * appropriate parameters have been set according to the  specification in 
     * the command string
     * @param command A portion of text containing parameter settings
     * @return
     */
    public abstract ParameterSettings parse(String command,  Parameter[] parameterFormats) throws ParseError; 

    /**
     * The method returns a command string for the given operation task
     * (with parameter settings). Different subclass implementations can return
     * command strings in different formats (standard protocol language, XML etc.)
     * @param parameters A list of Parameter definitions
     * @param settings A ParameterSettings object containing current values for the parameters
     * @param filter An optional string specifying whether only "input" or only "output" parameters should be included (a null value will include both)     * 
     * @return A string containing the parameters and their value settings according to the protocol specification
     */
    public abstract String getCommandString(Parameter[] parameters, ParameterSettings settings, String filter); 

    public String getCommandString(Parameter[] parameters, ParameterSettings settings) {
        return getCommandString(parameters, settings, null);
    }
}
