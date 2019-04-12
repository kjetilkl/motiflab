/*
 
 
 */

package motiflab.engine;

/**
 * ExecutionError exceptions represents things that go wrong during execution of a script
 * (that is not a result of sloppy programming on my part, such as null pointer exceptions 
 * or array index out of bounds)
 * @author kjetikl
 */
public class ExecutionError extends Exception {
    private int linenumber=0;

    /**
     * Creates a new instance of <code>ExecutionError</code> without detail message.
     */
    public ExecutionError() {
    }


    /**
     * Constructs an instance of <code>ExecutionError</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ExecutionError(String msg) {
        super(msg);
    }
    /**
     * Constructs an instance of <code>ExecutionError</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ExecutionError(String msg, Throwable originalCause) {
        super(msg,originalCause);
    }
    
    /**
     * Constructs an instance of <code>ExecutionError</code> with the specified detail message
     * occurring at the given linenumber
     * @param msg the detail message.
     */
    public ExecutionError(String msg, int linenumber) {
        super(msg);
        this.linenumber=linenumber;
    }
    
    /**
     * Constructs an instance of <code>ExecutionError</code> with the specified detail message
     * occurring at the given linenumber
     * @param msg the detail message.
     */
    public ExecutionError(String msg, int linenumber, Exception originalCause) {
        super(msg,originalCause);
        this.linenumber=linenumber;
    }
    
    /**
     * Gets the linenumber this error occurred on
     * @param linenumber
     */
    public int getLineNumber() {
        return linenumber;
    }
    
    /**
     * Sets the linenumber this error occurred on
     * @param linenumber
     */
    public void setLineNumber(int linenumber) {
        this.linenumber=linenumber;
    }
    

    
    
}
