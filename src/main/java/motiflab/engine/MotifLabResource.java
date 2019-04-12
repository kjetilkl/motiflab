/*
 * 
 */
package motiflab.engine;

/**
 *
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
