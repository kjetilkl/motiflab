/*
 
 
 */

package motiflab.engine;

/**
 *
 * @author kjetikl
 */
public interface ProgressListener {

    /** Notifies listeners that some progress has occurred. 
     * @param source The object reporting the progress
     * @param progress A percentage integer value specifying the progress (the value should be between 0 and 100)
     */
    public void processProgressEvent(Object source, int progress);
}
