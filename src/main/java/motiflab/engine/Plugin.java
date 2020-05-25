/*
 * A plugin for MotifLab is simply a regular ZIP-file with the suffix ".mlp" (MotifLab Plugin), altough the default ".zip" suffix can also be used.
 * During plugin installation, this ZIP-file will be unpacked in a hidden directory created specifically for the plugin.
 *
 * The ZIP-file must contain at least one JAR-file in the top-level directory, and ONE of the classes in the JARs should implement this plugin interface.
 * All the class-files in all JARs in the top-level directory will be loaded by a class-loader. If the plugin has many dependencies on other JARs,
 * these can optionally be placed in the subdirectory "lib" to make things a bit tidier (these will also be loaded). 
 * 
 * In addition to the JAR-file(s), the top-level directory of the plugin MUST contain a configuration file named "plugin.conf" (or "plugin.config" or "plugin.ini").
 * This file should be in INI format, i.e. it should contain metadata information in key=value pairs. All lines that do not start with a regular letter
 * will be ignored (and can be used for comments, section headers, etc.), but the rest will be included in the plugin's metadata information.
 * The only required plugin attribute in the configuration file is "name", and the plugin will be registered under this name in MotifLab. 
 * Note that this interface also has a method called getPluginName() which should return the same name!
 *
 * Other recognized plugin properties that can be set in the configuration file are "version", "description" and "type" (e.g. Tool, Analysis, Operation, DTV (Data Track Visualizer).
 * These properties will be included in the table of the "Plugins" dialog that can be accessed from the Configure menu in the GUI. Other recommended information are "author" (name) and "contact" (email).
 * If a "documentation" property is specified, it will enable the "Help" option for the plugin. The documentation property should be a URL pointing to an on-line documentation page for the plugin.
 * Alternatively, if no "documentation" property is specified, but the plugin ZIP-fip contains a subdirectory called "docs" with an "index.html" file, this HTML file will be used as documentation.
 *  
 * Once installed in MotifLab, the metadata information supplied in the config file can be obtained from MotifLabEngine via the method engine.getPluginMetaData(pluginName)
 * (this will be returned as a HashMap). Specific attributes can alternatively be obtained with the convenience method engine.getPluginProperty(pluginName, propertyName). 
 * The plugin itself (i.e. the class implementing this interface) is stored under the special property name "_plugin", or it can alternatively be obtained with engine.getPlugin(pluginName).
 *  
 * The plugin ZIP-file can also contain other resources that the plugin needs, such as e.g. images. These can most easily be obtained as resources using a syntax such as this:
 * ImageIcon pluginIcon=new ImageIcon(YourPluginClass.class.getResource("filename.png"));
 * 
 * If you need to get access directly to the plugin's directory, you can obtain the path via the metadata property "pluginDirectory" (which is set automatically by MotifLab).
 *
 * During startup, the engine will go through an initialize all the plugins by calling their initializePlugin(engine) method. The plugins should take this time to register themselves as resources with the engine (if necessary).
 * Afterwards, the plugins' initializePluginFromClient(client) method will be called. If the plugins need to register themselves with the client also (or exclusively), they should do so at this point.
 * For instance, if the plugin is a tool and would like to add itself to the "Tools" menu in the GUI, they should do this during this method, but be sure to first check that the client is actually the correct one (e.g. an instance of MotifLabGUI).
 * When MotifLab exits, it will call the shutdownPlugin() method. If the plugin has created some outstanding resources that should be cleaned up, it can do so at this time. 
 * Also, if the plugin has some settings that it would like to persist for future sessions, this will be the last available time to make those arrangements.
 * If the user chooses to uninstall the plugin, the uninstallPlugin() method will be called. At the time of calling, the plugin has already been removed from the "plugin master list" in MotifLab and can no longer be accessed.
 * However, if the plugin has registered itself as a resource of some kind with the engine, or has added itself to the client (e.g. has created a new menu item for itself in the GUI), the plugin itself is responsible for cleaning up after itself
 * and reversing these changes. After the "uninstallPlugin" method returns, the plugin's directory will be deleted from disc and the plugin will not be loaded the next time MotifLab starts up.
 * 
 */
package motiflab.engine;


/**
 * This interface must be implemented by all MotifLab plugins
 * 
 * @author kjetikl
 */
public interface Plugin {
    
    /**
     * Returns a name for the plugin
     * @return 
     */
    public String getPluginName();
     
    /**
     * This is called immediately after the Plugin is instantiated to initialize the plugin.
     * The plugin should also use this method to register itself as a resource with the engine 
     * (along with additional dependencies that might also require registration).
     * Note that the MotifLab client might not be available when this method is called,
     * so the plugin should not use this method for client-specific initialization.
     * That should rather be done in the @initializeFromClient method which is called afterwards.
     * 
     * @param engine
     * @throws ExecutionError Signals a non-critical initialization error
     *                        If the plugin throws an ExecutionError the message will
     *                        be displayed to the user but the plugin can still be used by MotifLab
     * @throws SystemError Signals a critical initialization error
     *                     If the plugin throws a SystemError the plugin will not be
     *                     registered with the engine for this MotifLab session
     *               
     */
    public void initializePlugin(MotifLabEngine engine) throws ExecutionError, SystemError;
    
    /**
     * This method can be called by the client to present itself to the plugin so that it can perform
     * additional client-specific initialization, such as e.g. adding itself to a menu in the GUI.
     * This method would be called some time after the main @initialize method is called by the engine
     * (for example from within client.initializeClient()).
     * @param client
     * @throws ExecutionError
     * @throws SystemError 
     */
    public void initializePluginFromClient(MotifLabClient client) throws ExecutionError, SystemError; 
    
    /**
     * This method is called when the user has requested that the plugin should be uninstalled.
     * Use this method to deregister with the engine and client. Note that the engine itself
     * is responsible for not loading the plugin on next startup, so the plugin only has to remove
     * itself from the current running session.
     * @param engine 
     */
    public void uninstallPlugin(MotifLabEngine engine);
    
    
    /** This is called by the engine when MotifLab is shutting down and allows the plugin to perform necessary cleanup
     *  or make arrangements for persistence 
     */
    public void shutdownPlugin();
    
    

}
