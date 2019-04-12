/*
 * This interface should be implemented by Plugins that allow themselves to be configured
 */
package motiflab.engine;

import java.awt.Dialog;
import javax.swing.JDialog;

/**
 *
 * @author kjetikl
 */
public interface ConfigurablePlugin {
    
    
    /**
     * This method can return a dialog that can be used to configure the plugin.
     * The dialog should be modal and be responsible for saving its own settings
     * @return 
     */
    public JDialog getPluginConfigurationDialog(Dialog owner);
    
    
        
    /**
     * Returns a list of Parameters that can be used to configure this plugin
     * @return 
     */
    public Parameter[] getPluginParameters();
    
    /**
     * Returns the current parameter settings for this plugin
     * @return 
     */
    public ParameterSettings getPluginParameterSettings();
    
    /**
     * Sets
     * @param settings
     * @throws ExecutionError 
     */
    public void setPluginParameterSettings(ParameterSettings settings) throws ExecutionError;
    
    /**
     * Returns the value for the plugin parameter with the given name
     * @param parameterName
     * @return
     * @throws ExecutionError If the parameter is not recognized
     */
    public Object getPluginParameterValue(String parameterName) throws ExecutionError;
    
    /**
     * Sets a value for the parameter with the given name.
     * The implementing method should be lenient with respect to the format of input values.
     * For example, it should be possible to set the value of a numeric parameter both by providing a Numeric object as value
     * or by providing a String containing a valid number (which must then be parsed by the implementing method).
     * @param parameterName
     * @param parameterValue
     * @throws ExecutionError If the parameter is not recognized or it could not be assigned the provided value
     */
    public void setPluginParameterValue(String parameterName, Object parameterValue) throws ExecutionError;    
    
}
