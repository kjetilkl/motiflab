/*
 
 
 */

package motiflab.engine.protocol;

import java.util.List;
import javax.swing.text.Document;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.ProtocolTask;

/**
 *
 * @author kjetikl
 */
public abstract class Protocol {
    protected MotifLabEngine engine;
    
    public Protocol(MotifLabEngine engine) {
        this.engine=engine;
    }
    
    /**
     * Returns a name for this protocol script. The name can for instance be used as filename
     * (by adding a suffix and a path)
     * @return
     */
    public abstract String getName();
    
    /**
     * Sets a name for this protocol script. The name can for instance be used as filename
     * (by adding a suffix and a path)
     * @param A name to use for this protocol script
     */
    public abstract void setName(String name);
    
    /**
     * Returns a DataTypeTable which can be used to register and query information
     * regarding the types of data objects mentioned on the protocol
     * @return
     */
    public abstract DataTypeTable getDataTypeLookupTable();
    
    
    /**
     * Returns the macros known to this protocol in their current defined state
     * (which can vary depending on whether or not the protocol has been parsed)
     * @return 
     */
    public abstract List<String[]> getMacros();
    
    /** 
     * Returns a Documents object containing the protocol script formated in some language
     * The type of Document returned may be dependent on the type of Protocol
     */
    public abstract Document getDocument();
    
    /**
     * This function returns a textual representation of a single operation (with arguments)
     * or other executable task in the language of this Protocol
     * 
     * @return A string that can be included in protocol scripts
     */
    public abstract String getCommandString(ExecutableTask task);
        
    
    /**
     * Inserts an operation-command into the protocol at the specified caret position
     * @param task
     * @param pos
     */
    public abstract boolean insertOperationAt(ExecutableTask task, int pos);
    
    /**
     * Appends an operation-command to the end of the protocol 
     * @param task
     */
    public abstract boolean appendOperation(ExecutableTask task);

    /**
     * Registers an operationParser for the specified operation
     * @param operationName
     * @param opparser
     */
    public abstract void registerOperationParser(String operationName, OperationParser opparser);
    
    /**
     * Registers a parser for parsing display settings
     * @param vsparser
     */
    public abstract void registerDisplaySettingsParser(DisplaySettingsParser vsparser);
    
    /**
     * Registers an Parameters parser to be used for parsing parameters settings 
     * for instance for external programs or data formats
     * @param parametersParser
     */
    public abstract void registerParametersParser(ParametersParser parametersParser);
    
    /**
     * Returns the operationParser registered for the specified operation
     * @param operationName
     * @param opparser
     */
    public abstract OperationParser getOperationParser(String operationName);
    
    /**
     * Returns the registered ParametersParser
     */
    public abstract ParametersParser getParametersParser();

    /**
     * Returns the registered DisplaySettingsParser
     */
    public abstract DisplaySettingsParser getDisplaySettingsParser();
    
    /** 
     * Parses the protocol and returns a task which can be executed by the engine
     * If any errors are encountered while parsing, the parsing is aborted and
     * a ParseError is thrown instead
     */
    public abstract ProtocolTask parse() throws ParseError;
    
    
    /** 
     * Parses the specified 'range' of the protocol and returns a task which can be executed by the engine
     * If any errors are encountered while parsing, the parsing is aborted and
     * a ParseError is thrown instead. A ParseError is also thrown if the start or end parameters
     * are 'out of range' with respect to the protocol.
     * @param start The number of the first command to be included in the ProtocolTask
     * @param end The number of the last command to be included in the ProtocolTask
     */
    public abstract ProtocolTask parse(int start, int end) throws ParseError;
    
       
    /** 
     * Tries to parse the n'th command in the protocol (first line is 1) and returns an OperationTask
     * object containing the results of the parsing. If any errors are encountered 
     * while parsing, the parsing is aborted and a ParseError is thrown instead
     * If the nth command is "empty" or otherwise not possible to execute a NULL value is returned 
     * Note that the "validity" of the n'th line must be viewed in the context of the previous lines.
     * This is important to take into considerations where a data item's type might change in the
     * course of a protocol script execution (for instance through "convert" operations). 
     * Thus, when parsing, not only the n'th line should be parsed, but the entire protocol up to the 
     * n'th line in order to validate correctly the types of all data items.
     * However, if this command is called multiple times for successive lines this constant reparsing
     * of all previous lines is redundant. If the "clearTypeTable" flag is set to TRUE, the
     * method should not assume that correct registration of data types in previous lines have been
     * performed. Hence, in this case any implementation of this method should start by clearing the
     * current DataTypeTable and reparsing all previous lines. However, if the flag is set to FALSE one
     * can assume that the DataTypeTable is in correct state at the time the method is called and the
     * previous lines need not be parsed. 
     * 
     * @param n The number of the command to be parsed (equals the line number for standard protocols)
     * @param clearTypeTable See above for explanation
     */
    public abstract ExecutableTask parseCommand(int n, boolean clearTypeTable) throws ParseError;
    
    
    /**
     * This method parses all the commands (or lines) in a protocol script, up to and including the
     * specified commandNumber, and updates the DataTypeTable to reflect the type state at the given command (or line)
     * NOTE: Calling this method will update the internal DataTypeTable of the Protocol (which is returned)
     * @param commandNumber The number of the command to be parsed (equals the line number for standard protocols)
     */
    public abstract DataTypeTable getDataTypeStateAtCommand(int commandNumber);
    
    
    /**
     * This method returns true if the Protocol script has no contents (including comments)
     */
    public abstract boolean isEmpty();
    
}
