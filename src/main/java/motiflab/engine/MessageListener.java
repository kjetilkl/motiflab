/*
 
 
 */

package motiflab.engine;

/**
 * All message passing eminating from the engine goes through this interface
 * 
 * @author kjetikl
 */
public interface MessageListener {
    
    /**
     * This method is called when the engine wants to notify potential listeners
     * about an error that has occurred during execution
     * 
     * @param msg A message providing additional human-readable information about the error
     * @param errortype A number specifying the type of error that has occurred (not actively used)
     * 
     */
    public void errorMessage(String msg, int errortype);
    
    
    /**
     * This method is called when the engine wants to notify potential listeners
     * about its status in a short and concise way. Status messages should essentially
     * be "one-liners" that could fit in a status bar
     * 
     * @param msg The data itself
     */
    public void statusMessage(String msg);
    

    /**
     * This method is called when the engine wants to notify potential listeners
     * about what it is doing and non of the other methods are appropriate to use.
     * 
     * @param msg The data itself
     */    
    public void logMessage(String msg);
    
    /**
     * This method is called when the engine wants to notify potential listeners
     * about what it is doing and non of the other methods are appropriate to use.
     * 
     * @param msg The data itself
     * @param level a number indicating the importance level of the message
     *              Numbers in the range 0-9 are trivial/incremental progress and status reports (e.g. completed X% of the task)
     *              Numbers in the range 10-19 are normal progress/status reports (e.g. a certain task is initiated)
     *              Numbers in the range 20-29 are regular messages
     *              Numbers above 30 are important messages (e.g. critical errors)
     */    
    public void logMessage(String msg, int level);    
    

    /**
     * This method can be used to notify clients of some form of progress
     * @param progress a number between 0 and 100. Numbers outside this range indicate that progress reporting has ended (because of abortion or because the process is finished).
     * However, the number MAX_INT indicate that the progress is "indeterminate"
     */
    public void progressReportMessage(int progress);
    
}
