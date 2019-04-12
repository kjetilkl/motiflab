/*
 
 
 */

package motiflab.engine.protocol;

/**
 *
 * @author kjetikl
 */
public class ParseError extends Exception {
    private int linenumber=0;
    
    /**
     * Creates a new instance of <code>ParseError</code> without detail message.
     */
    public ParseError() {
    }


    /**
     * Constructs an instance of <code>ParseError</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ParseError(String msg) {
        super(msg);
    }
    /**
     * Constructs an instance of <code>ParseError</code> with the specified detail message
     * occurring at the given linenumber
     * @param msg the detail message.
     */
    public ParseError(String msg, int linenumber) {
        super(msg);
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
