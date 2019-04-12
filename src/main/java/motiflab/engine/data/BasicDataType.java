
package motiflab.engine.data;

import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;

/**
 *
 * @author kjetikl
 */
public interface BasicDataType {
    
    public Object getPropertyValue(String propertyName, MotifLabEngine engine) throws ExecutionError;
    
    public boolean setPropertyValue(String propertyName, Object value) throws ExecutionError;
    
    
    
}
