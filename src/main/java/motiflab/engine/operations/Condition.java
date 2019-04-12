/*
 
 
 */

package motiflab.engine.operations;


import motiflab.engine.task.OperationTask;
import java.util.HashMap;
import java.util.Set;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.Data;


/**
 * This class functions as a "wrapper" for where-clauses (and similar clauses such as while or until)
 * that determine whether an operation should be applied to a base or region based on some user-defined conditions  
 *
 * The contents of Conditions can not be output to strings in this class (for instance for inclusion in Protocol scripts)
 * since that would be protocol-language specific. Instead, use methods like for instance
 * StandardOperationParser.getCommandString_condition(condition) to get a protocol-language specific formatting of a Condition
 * @author kjetikl
 */
public abstract class Condition implements Cloneable {
    protected HashMap<String,Object> storage;

    
    public Condition() {
        storage=new HashMap<String,Object>();
    }
    
    public Condition(HashMap<String,Object> initialValues) {
        storage=initialValues;
    }

    /**
     * Sets a specific parameter to be used for execution of an Operation
     * @param key
     * @param value
     */
    public void setParameter(String key, Object value) {
        storage.put(key, value);
    } 

    /**
     * Gets a specific parameter to be used for execution of an Operation
     * @param key
     */    
    public Object getParameter(String key) {
        return storage.get(key);
    }
    
        
    /**
     * This method is used to resolve textual references to constants or data items
     * to actual data objects (i.e. The Condition goes through all textual (String) representations
     * of all parameters and creates/fetches Data objects to represent their values)
     * @param engine
     */
    public abstract void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError;
    
    
    /**
     * Removes all resolved references to data-objects
     */
    public void unlink() {
        Set<String> keys=storage.keySet();
        for(String key:keys) {
            if (storage.get(key) instanceof Data) keys.remove(key);
        }
    } 

    /*
     * NOTE: OperandA is the first operand on the left side of the comparator
     *       and OperandB is the operand on the right side which operandA is compared with
     *       If the second operand represents a numeric value range, OperandB will hold the
     *       lower bound and OperandB2 will hold the upper bound
     */
   
    public String getOperandAString() {return (String)getParameter("whereOperandA");}
    public Data getOperandAData() {return (Data)getParameter("operandAdata");}
    public String getOperandBString() {return (String)getParameter("whereOperandB");}
    public Data getOperandBData() {return (Data)getParameter("operandBdata");}
    public String getOperandB2String() {return (String)getParameter("whereOperandB2");}
    public Data getOperandB2Data() {return (Data)getParameter("operandB2data");}
    public String getComparator() {return (String)getParameter("whereComparator");}
    public Boolean negateAll() {return (Boolean)getParameter("whereNot");}
    
    public void setOperandAString(String operandAString) {setParameter("whereOperandA", operandAString);}
    public void setOperandAData(Data operandAData) {setParameter("operandAdata", operandAData);}
    public void setOperandBString(String operandBString) {setParameter("whereOperandB", operandBString);}
    public void setOperandBData(Data operandBData) {setParameter("operandBdata", operandBData);}
    public void setOperandB2String(String operandB2String) {setParameter("whereOperandB2", operandB2String);}
    public void setOperandB2Data(Data operandB2Data) {setParameter("operandB2data", operandB2Data);}
    public void setComparator(String comparator) {setParameter("whereComparator", comparator);}
    public void setNegateAll(Boolean negate) {setParameter("whereNot", negate);}

    @Override
    public abstract Condition clone();    

    /** Replaces the contents of this condition with the contents of the argument
     *  which must be of the same type
     */
    public abstract void importCondition(Condition condition) throws ClassCastException;      
    
    /**
     * Dumps the settings in this Condition object to System.err
     */
    public void debug() {
        System.err.println("--Condition--");
        Set<String> keys=storage.keySet();
        for(String key:keys) {
            Object value=storage.get(key);
            if (value!=null)  System.err.println("   "+key+" = '"+value.toString()+"'");
        }
    } 

}
