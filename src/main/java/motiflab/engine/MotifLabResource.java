/*
 * 
 */
package motiflab.engine;

/**
 * MotifLabResources are objects that can be registered with the engine with its "name" and an optional "type".
 * Other classes can then ask the engine to provide them with a named resource or all resources of specific type.
 * This makes it easier to dynamically add new components to the system, such as new Tools, Analyses or DataSources added by plugins.
 * For instance, instead of using a hard-coded list of options for a menu, the menu can be populated with registered resources of the relevant type.
 * @author kjetikl
 */
public interface MotifLabResource {
    
    /**
     * @return 
     * Returns the name of this resource. This should be unique across all resources within the same "namespace"
     * If a new resource is registered with the same name (and type name) as an existing resource,
     * the newly registered resource will replace the older one
     * 
     */
    public String getResourceName();
    
    /**
     * @return 
     * Returns the class type of this resource
     * 
     */    
    public Class getResourceClass();
    
    /**
     * Returns the "type name" of this resource. This optional property is used
     * to implement "namespaces" for the resources. If a resource has a type name
     * if will be filed under "typename|name" in the resource-registry. 
     * (if not they will just be filed under "name")
     * This allows you to have multiple resources with the same name, as long as they
     * have different "type names".
     * @return A "type name" for this resource (or NULL)

     */    
    public String getResourceTypeName();    
    
    /** 
     * @return
     * Optionally returns an icon representing this resource (could be NULL) 
     */
     public javax.swing.Icon getResourceIcon();
    
     /**
      * A factory method that returns an object for this resource. 
      * This can be a new object or a shared singleton object depending on the resource 
      * @return 
      */
     public Object getResourceInstance();
}
