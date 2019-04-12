
package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.OperationTask;

/**
 * Implements a compound position condition consisting of two clauses connected by either AND or OR
 *
 * @author Kjetil Klepper
 */
public class Condition_position_boolean extends Condition_position implements Condition_boolean {

    private boolean isResolved=false;
    private int type=AND;
    private ArrayList<Condition_position> conditions=null;


    public Condition_position_boolean(int type) {
        super();
        this.type=type;
        if (conditions==null) conditions=new ArrayList<Condition_position>();        
    }
    
    public Condition_position_boolean(Condition_position firstCondition, Condition_position secondCondition, int type) {
        super();
        this.conditions=new ArrayList<Condition_position>(2);
        this.conditions.add(firstCondition);
        this.conditions.add(secondCondition);
        this.type=type;
    }
    
    public Condition_position_boolean(ArrayList<Condition_position> conditions, int type) {
        super();
        this.conditions=conditions;
        this.type=type;
    }    

    @Override
    public void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError {
        if (conditions!=null) {
            for (Condition_position condition:conditions) {
                if (condition!=null) condition.resolve(engine, task);
            }
        }
        isResolved=true;
    }

    @Override
    public int size() {
        if (conditions==null) return 0;
        else return (conditions.size());
    }
    
    @Override
    public Class getConditionType() {
        return Condition_position.class;
    }
    
    @Override
    public int getOperatorType() {
        return type;
    }
    
    @Override
    public void setOperatorType(int type) {
        this.type=type;
    }  
    
    @Override
    public ArrayList<Condition_position> getConditions() {
        return conditions;
    }
    
    public void addCondition(Condition_position newcondition) {
        conditions.add(newcondition);
    } 
    
    @Override
    public void addCondition(Condition newcondition) {
        if (newcondition instanceof Condition_position) conditions.add((Condition_position)newcondition);
    }     
    
    @Override
    public void addCondition(Condition newcondition, int index) {
        if (newcondition instanceof Condition_position) conditions.add(index,(Condition_position)newcondition);
    }       
   
    @Override
    public void removeCondition(int index) {
        conditions.remove(index);
    }
    
    public void replaceCondition(int index, Condition_position newcondition) {
        conditions.set(index, newcondition);
    }  
    
    @Override
    public void replaceCondition(int index, Condition newcondition) {
        if (newcondition instanceof Condition_position) conditions.set(index, (Condition_position)newcondition);
    }     

    /**
     * Returns true if this Condition holds for the specified position
     * @param sequencename
     * @param position The position that this condition should be evaluation for
     * @param task
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public boolean isConditionSatisfied(String sequencename, int position, OperationTask task) throws Exception {
        if (!isResolved) {
            task.getEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_position_boolean is not 'resolved' before use"));
        }   
        if (conditions==null) return true; // I guess this is OK since it could be taken to mean no restrictions
        boolean negateCondition=false;
        Boolean whereNotBoolean=negateAll(); //
        if (whereNotBoolean!=null) negateCondition=whereNotBoolean.booleanValue();
        if (!negateCondition) { // normal conditions
            if (type==AND) {
                for (Condition_position condition:conditions) {
                    if (condition!=null && !condition.isConditionSatisfied(sequencename, position, task)) return false;
                }
                return true; // all conditions are satisfied
            } else if (type==OR) {
                for (Condition_position condition:conditions) {
                    if (condition!=null && condition.isConditionSatisfied(sequencename, position, task)) return true;
                }
                return false; // no conditions are satisfied
            } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: the 'type' parameter to boolean condition should be 0=AND, or 1=OR but got: "+type);          
        } else {// negate compound: apply De Morgan's law
             if (type==AND) { // => not X1 or not X2 or not...
                for (Condition_position condition:conditions) {
                    if (condition!=null && !condition.isConditionSatisfied(sequencename, position, task)) return true;
                }
                return false; // no conditions are satisfied
            } else if (type==OR) {// not X1 and not X2 and not...
                for (Condition_position condition:conditions) {
                    if (condition!=null && condition.isConditionSatisfied(sequencename, position, task)) return false;
                }
                return true; // no conditions are satisfied
            } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: the 'type' parameter to boolean condition should be 0=AND, or 1=OR but got: "+type);         
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public Condition_position_boolean clone() {
        ArrayList<Condition_position> conditionlist=null;
        if (conditions!=null) {
            conditionlist=new ArrayList<Condition_position>(conditions.size());
            for (Condition_position condition:conditions) {
                if (condition==null) conditionlist.add(null);
                else conditionlist.add(condition.clone());
            }
        }
        Condition_position_boolean thisclone=new Condition_position_boolean(conditionlist, type);
        return thisclone;
    }
    
    @Override
    public void importCondition(Condition other) throws ClassCastException {
        if (other==null) throw new ClassCastException("Unable to import from NULL condition");
        if (!other.getClass().equals(this.getClass())) throw new ClassCastException("Unable to import from condition of different class. Expected '"+this.getClass()+"' but got '"+other.getClass()+"'.");
        isResolved=((Condition_position_boolean)other).isResolved;
        type=((Condition_position_boolean)other).type;
        conditions=((Condition_position_boolean)other).conditions;
    }    
}
