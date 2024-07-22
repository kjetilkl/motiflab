/*
 */
package motiflab.engine;

import javax.swing.Icon;

/**
 * A simple class that implements the MotifLabResource interface
 * and can be used to wrap other resource objects for registration
 * with the MotifLabEngine
 * 
 * @author kjetikl
 */
public class MotifLabResourceWrapper implements MotifLabResource {

    private String resourcename=null;
    private String resourcetypename=null;
    private Class resourceclass=null;
    private Icon resourceicon=null;
    private Object resourceinstance=null;
    
    /**
     * 
     * @param name The name of the resource (required)
     * @param typename The "type name" of the resource (optional)
     * @param type The class type of the resource (required)
     * @param icon An icon to represent the resource (optional)
     * @param instance The object representing the resource (optional). If this is provided, the getResourceInstance() method will return this object (singleton resource).
     *                 If not provided (null), the getResourceInstance() method will return a new object of the type class (new resource object every time the method is called).
     *                 If neither of these two strategies are adequate, this wrapper class should not be used for the resource.
     */
    public MotifLabResourceWrapper(String name, String typename, Class type, Icon icon, Object instance) {
        resourcename=name;
        resourcetypename=typename;
        resourceclass=type;
        resourceicon=icon;
        resourceinstance=instance;
    }
    
    
    @Override
    public String getResourceName() {
        return resourcename;
    }

    @Override
    public Class getResourceClass() {
       return resourceclass;
    }

    @Override
    public String getResourceTypeName() {
        return resourcetypename;
    }

    @Override
    public Icon getResourceIcon() {
        return resourceicon;
    }

    @Override
    public Object getResourceInstance() {
        if (resourceinstance!=null) return resourceinstance;
        try {
           return resourceclass.newInstance();
        }
        catch (Exception e) {
           System.err.println(e);
           return null;       
        }
    }
    
    
    
    
}
