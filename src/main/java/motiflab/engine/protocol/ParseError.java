/*
 
 
 */

package motiflab.engine.protocol;

/**
 *
 * @author kjetikl
 */
public class ParseError extends Exception {
    private int linenumber = 0; // line number starts at 1, so a value of 0 means "undefined"
    
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
     * @param linenumber The line where the error occured (starting at 1)
     */
    public ParseError(String msg, int linenumber) {
        super(msg);
        this.linenumber=linenumber;
    }
    
    
    /**
     * Gets the linenumber this error occurred on (starting a 1)
     * @param linenumber
     */
    public int getLineNumber() {
        return linenumber;
    }
    
    /**
     * Sets the linenumber this error occurred on (starting a 1)
     * @param linenumber
     */
    public void setLineNumber(int linenumber) {
        this.linenumber=linenumber;
    }
}
