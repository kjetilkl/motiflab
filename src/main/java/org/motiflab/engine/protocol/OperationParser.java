/*
 
 
 */

package org.motiflab.engine.protocol;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.MotifLabEngine;

/**
 * This top class represents parsers for single operations. 
 * Objects of this class parses language constructs that specify
 * the parameters for a specific operation and returns an instance
 * of that operation.
 * 
 * Each subclass is responsible for parsing one type of operation, 
 * and the language to be parsed (eg. standard protocol language or XML)
 * is also determined in the subclasses. 
 * 
 * The operations object returned should be independent of the language
 * used to specify it (that is, the Operation is a language-independent 
 * abstraction of an operation on some data under some conditions)
 * 
 * @author kjetikl
 */
public abstract class OperationParser {
    
    protected MotifLabEngine engine=null;
    protected Protocol protocol;
    
    public OperationParser(MotifLabEngine engine) {
        this.engine=engine;
    }
    
    public OperationParser(){}
    
    /** 
     * Sets the engine for this operation. This method should always be called 
     * directly after instantiation of new parser with a zero-parameters constructor
     */
    public void setEngine(MotifLabEngine engine) {
        this.engine=engine;
    }

    /** 
     * Sets a reference back to the "parent" protocol which OperationParsers might
     * need to query the Protocol for information (for instance regarding the 
     * type of different data objects)
     */
    public void setProtocol(Protocol protocol) {
        this.protocol=protocol;
    }
    
    /**
     * The method parses a string containing the definition and settings for an
     * applied operation in the protocol language. It returns an OperationTask
     * object where the appropriate parameters have been set according to 
     * specification in the command string
     * @param text A portion of text 
     * @return
     */
    public abstract OperationTask parse(String command) throws ParseError; 

    /**
     * The method returns a command string for the given operation task
     * (with parameter settings). Different subclass implementations can return
     * command strings in different formats (standard protocol language, XML etc.)
     * @param operationtask
     * @return
     */
    public abstract String getCommandString(OperationTask operationtask); 
}
