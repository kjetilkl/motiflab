/*
 
 
 */

package motiflab.engine;

import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.operations.PromptConstraints;
import motiflab.gui.VisualizationSettings;

/**
 * 
 * 
 * @author kjetikl
 */
public interface MotifLabClient extends MessageListener {

    /**
     * This method can be used to ask the client to provide a value for the specified data item
     * The value returned should be an altered copy of the original (a double buffer) not the 
     * original itself!  
     * @param item The data item (NOT null!)
     * @param message An optional message that the client can pass on to the user
     * @return
     */
    public Data promptValue(Data item, String message, PromptConstraints contstraint) throws ExecutionError;

    
    /**
     * This function should return true if an task should attempt
     * to retry executions that have failed because of unforseen errors
     * (such as network timeouts) or false if the task should abort the
     * attempt and throw an Exception instead.
     * @param The task that encountered the error
     * @param e The Exception which caused the execution to fail
     * @return true if an attempt should be made at re-executing the task 
     * or false if the task should be aborted by throwing an exception
     */
    public boolean shouldRetry(ExecutableTask task, Exception e);
    
    /**
     * This function should return true if a task should rollback
     * all changes made during its execution upon encountering an error
     * or false if the operation should just abort without performing
     * cleanup.
     * @return true if rollback should be performed
     */
    public boolean shouldRollback();
    
    
    /**
     * This method is called by the engine if an uncaught exception happens
     * The client is asked to deal with the problem. If the problem is non-fatal
     * and the client is able to recover, a TRUE value should be returned. 
     * If the client is not able to recover, necessary clean-up should be performed
     * before returning FALSE. After which the engine will shut down (exit). 
     * 
     * @param e
     */
    public boolean handleUncaughtException(java.lang.Throwable e);
    
    /**
     * This method is called automatically from the engine to initialize the client 
     * after it has been installed!
     */
    public void initializeClient(MotifLabEngine engine);
    
    /**
     * This is called by the engine if the engine thinks it is time to exit
     * (for instance after catching an exception which can not be handled)
     */
    public void shutdown();

    /**
     * Returns a VisualizationSettings object used by this client to store
     * information about visualization settings (such as colors for tracks
     * and graphs etc).
     * @return
     */
    public VisualizationSettings getVisualizationSettings();

    /**
     * Saves the current session to a file
     * @param filename 
     */
    public void saveSession(String filename) throws Exception;

    /**
     * Restores a session from file
     * @param filename 
     */
    public void restoreSession(String filename) throws Exception;

    public MotifLabEngine getEngine();
    
    
    /** This method is used whenever multiple mappings are returned for a single gene ID
     * (I am not sure if this is possible but I will account for it anyway). A dialog is
     * displayed so that the user can select the mapping wanted (or even several mappings, or none)
     */
    public ArrayList<GeneIDmapping> selectCorrectMappings(ArrayList<GeneIDmapping> list, String id);

    /**
     * This method will force the client to display a prompt (or multiple prompts) asking the user to enter values
     * for a specific set of parameters.
     * The client should not ask for values for hidden parameters, but it is up
     * to the client to decide whether it wants to prompt for values of advanced parameters.
     * The client is not required to respect parameter-conditions, so such conditions should not be used.
     * The client is also not required to validate the input as this should be the responsibility of the calling method,
     * If the calling method is not satisfied with the user's input it can try again.
     * 
     * @param parameters The set of parameters whose values will be prompted for
     * @param defaultsettings An optional set of default values for the parameters
     * @param message An explanatory message that will be displayed together with the prompt (at the beginning)
     * @param title A text that the client can optionally use as a title for the prompt dialog
     * @return The values the user entered for the parameters. Note: this can be NULL if the user decides to cancel the prompt 
     */
    public ParameterSettings promptForValues(Parameter[] parameters, ParameterSettings defaultsettings, String message, String title);
    
}
