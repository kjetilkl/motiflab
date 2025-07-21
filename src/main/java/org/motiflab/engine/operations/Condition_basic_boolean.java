
package org.motiflab.engine.operations;

import java.util.ArrayList;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.task.ConditionalTask;

/**
 * Implements a compound region condition consisting of two clauses connected by either AND or OR
 *
 * @author Kjetil Klepper
 */
public class Condition_basic_boolean extends Condition_basic implements Condition_boolean {

    private boolean isResolved=false;
    private int type=AND;
    private ArrayList<Condition_basic> conditions=null;



    public Condition_basic_boolean(int type) {
        super();
        this.type=type;
        if (conditions==null) conditions=new ArrayList<Condition_basic>();        
    }
    
    public Condition_basic_boolean(Condition_basic firstCondition, Condition_basic secondCondition, int type) {
        super();
        this.conditions=new ArrayList<Condition_basic>(2);
        this.conditions.add(firstCondition);
        this.conditions.add(secondCondition);
        this.type=type;
    }
    
    public Condition_basic_boolean(ArrayList<Condition_basic> conditions, int type) {
        super();
        this.conditions=conditions;
        this.type=type;
    }    

    @Override
    public void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError {
        if (conditions!=null) {
            for (Condition_basic condition:conditions) {
                if (condition!=null) condition.resolve(engine, task);
            }
        }
        isResolved=true;
    }
    
    public void resolve(ConditionalTask task) throws ExecutionError {
        if (conditions!=null) {
            for (Condition_basic condition:conditions) {
                if (condition!=null) condition.resolve(task);
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
        return Condition_basic.class;
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
    public ArrayList<Condition_basic> getConditions() {
        return conditions;
    }
    
    public void addCondition(Condition_basic newcondition) {
        conditions.add(newcondition);
    } 
    
    @Override
    public void addCondition(Condition newcondition) {
      if (newcondition instanceof Condition_basic) conditions.add((Condition_basic)newcondition);
    }  
    
    @Override
    public void addCondition(Condition newcondition, int index) {
      if (newcondition instanceof Condition_basic) conditions.add(index,(Condition_basic)newcondition);
    }     
    
    @Override
    public void removeCondition(int index) {
        conditions.remove(index);
    }
    
    public void replaceCondition(int index, Condition_basic newcondition) {
       conditions.set(index, newcondition);
    }
    
    @Override
    public void replaceCondition(int index, Condition newcondition) {
       if (newcondition instanceof Condition_basic) conditions.set(index, (Condition_basic)newcondition);
    }    

    /**
     * Returns true if this Condition holds
     * @param task
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public boolean isConditionSatisfied(ConditionalTask task) throws Exception {
        if (!isResolved) {
            resolve(task); // task.getMotifLabEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_basic_boolean is not 'resolved' before use"));
        }   
        if (conditions==null) return true; // I guess this is OK since it could be taken to mean no restrictions
        boolean negateCondition=false;
        Boolean whereNotBoolean=negateAll(); //
        if (whereNotBoolean!=null) negateCondition=whereNotBoolean.booleanValue();
        if (!negateCondition) { // normal conditions
            if (type==AND) {
                for (Condition_basic condition:conditions) {
                    if (condition!=null && !condition.isConditionSatisfied(task)) return false;
                }
                return true; // all conditions are satisfied
            } else if (type==OR) {
                for (Condition_basic condition:conditions) {
                    if (condition!=null && condition.isConditionSatisfied(task)) return true;
                }
                return false; // no conditions are satisfied
            } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: the 'type' parameter to boolean condition should be 0=AND, or 1=OR but got: "+type);          
        } else {// negate compound: apply De Morgan's law
             if (type==AND) { // => not X1 or not X2 or not...
                for (Condition_basic condition:conditions) {
                    if (condition!=null && !condition.isConditionSatisfied(task)) return true;
                }
                return false; // no conditions are satisfied
            } else if (type==OR) {// not X1 and not X2 and not...
                for (Condition_basic condition:conditions) {
                    if (condition!=null && condition.isConditionSatisfied(task)) return false;
                }
                return true; // no conditions are satisfied
            } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: the 'type' parameter to boolean condition should be 0=AND, or 1=OR but got: "+type);         
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public Condition_basic_boolean clone() {
        ArrayList<Condition_basic> conditionlist=null;
        if (conditions!=null) {
            conditionlist=new ArrayList<Condition_basic>(conditions.size());
            for (Condition_basic condition:conditions) {
                if (condition==null) conditionlist.add(null);
                else conditionlist.add(condition.clone());
            }
        }
        Condition_basic_boolean thisclone=new Condition_basic_boolean(conditionlist, type);
        return thisclone;
    }
    
    @Override
    public void importCondition(Condition other) throws ClassCastException {
        if (other==null) throw new ClassCastException("Unable to import from NULL condition");
        if (!other.getClass().equals(this.getClass())) throw new ClassCastException("Unable to import from condition of different class. Expected '"+this.getClass()+"' but got '"+other.getClass()+"'.");
        isResolved=((Condition_basic_boolean)other).isResolved;
        type=((Condition_basic_boolean)other).type;
        conditions=((Condition_basic_boolean)other).conditions;
    }     
}
