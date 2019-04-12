
package motiflab.engine.operations;

import java.util.ArrayList;

/**
 * A boolean condition is a compound condition consisting of two condition-clauses connected by other AND or OR
 * The result of the condition is thus either (X AND Y) or (X OR Y) depending on the particular boolean operator.
 * 
 * This class represents the top-level interface which applies to both position and region compound (boolean) conditions
 *
 * @author Kjetil Klepper
 */
public interface Condition_boolean {
    public static int AND=0;
    public static int OR=1;

    public int getOperatorType();
    
    public void setOperatorType(int type);    

    public ArrayList<? extends Condition> getConditions();
    
    public void addCondition(Condition condition); 
    
    public void addCondition(Condition condition, int index);    
    
    public void removeCondition(int index);
    
    public void replaceCondition(int index, Condition newcondition);  
    
    /** The number of sub-conditions in the compound */
    public int size();
    
    /** Returns the type of conditions being combined (e.g. Condition_position or Condition_region) */
    public Class getConditionType();

}
