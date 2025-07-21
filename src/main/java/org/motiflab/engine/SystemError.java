/*
 
 
 */

package org.motiflab.engine;

/**
 *
 * @author kjetikl
 */
public class SystemError extends Exception {

    /**
     * Creates a new instance of <code>SystemError</code> without detail message.
     */
    public SystemError() {
    }


    /**
     * Constructs an instance of <code>SystemError</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SystemError(String msg) {
        super(msg);
    }
}
