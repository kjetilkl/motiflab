
package motiflab.engine.operations;

import java.util.ArrayList;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.Region;

/**
 * Implements a compound region condition consisting of two clauses connected by either AND or OR
 *
 * @author Kjetil Klepper
 */
public class Condition_region_boolean extends Condition_region implements Condition_boolean {

    private boolean isResolved=false;
    private int type=AND;
    private ArrayList<Condition_region> conditions=null;



    public Condition_region_boolean(int type) {
        super();
        this.type=type;
        if (conditions==null) conditions=new ArrayList<Condition_region>();        
    }
    
    public Condition_region_boolean(Condition_region firstCondition, Condition_region secondCondition, int type) {
        super();
        this.conditions=new ArrayList<Condition_region>(2);
        this.conditions.add(firstCondition);
        this.conditions.add(secondCondition);
        this.type=type;
    }
    
    public Condition_region_boolean(ArrayList<Condition_region> conditions, int type) {
        super();
        this.conditions=conditions;
        this.type=type;
    }    

    @Override
    public void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError {
        if (conditions!=null) {
            for (Condition_region condition:conditions) {
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
        return Condition_region.class;
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
    public ArrayList<Condition_region> getConditions() {
        return conditions;
    }
    
    public void addCondition(Condition_region newcondition) {
        conditions.add(newcondition);
    } 
    
    @Override
    public void addCondition(Condition newcondition) {
      if (newcondition instanceof Condition_region) conditions.add((Condition_region)newcondition);
    }  
    
    @Override
    public void addCondition(Condition newcondition, int index) {
      if (newcondition instanceof Condition_region) conditions.add(index,(Condition_region)newcondition);
    }     
    
    @Override
    public void removeCondition(int index) {
        conditions.remove(index);
    }
    
    public void replaceCondition(int index, Condition_region newcondition) {
       conditions.set(index, newcondition);
    }
    
    @Override
    public void replaceCondition(int index, Condition newcondition) {
       if (newcondition instanceof Condition_region) conditions.set(index, (Condition_region)newcondition);
    }    

    /**
     * Returns true if this Condition holds for the specified region
     * @param sequencename
     * @param region The region that this condition should be evaluation for
     * @param task
     * @return
     * @throws java.lang.Exception
     */
    @Override
    public boolean isConditionSatisfied(String sequencename, Region region, OperationTask task) throws Exception {
        if (!isResolved) {
            task.getEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_region_boolean is not 'resolved' before use"));
        }   
        if (conditions==null) return true; // I guess this is OK since it could be taken to mean no restrictions
        boolean negateCondition=false;
        Boolean whereNotBoolean=negateAll(); //
        if (whereNotBoolean!=null) negateCondition=whereNotBoolean.booleanValue();
        if (!negateCondition) { // normal conditions
            if (type==AND) {
                for (Condition_region condition:conditions) {
                    if (condition!=null && !condition.isConditionSatisfied(sequencename, region, task)) return false;
                }
                return true; // all conditions are satisfied
            } else if (type==OR) {
                for (Condition_region condition:conditions) {
                    if (condition!=null && condition.isConditionSatisfied(sequencename, region, task)) return true;
                }
                return false; // no conditions are satisfied
            } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: the 'type' parameter to boolean condition should be 0=AND, or 1=OR but got: "+type);          
        } else {// negate compound: apply De Morgan's law
             if (type==AND) { // => not X1 or not X2 or not...
                for (Condition_region condition:conditions) {
                    if (condition!=null && !condition.isConditionSatisfied(sequencename, region, task)) return true;
                }
                return false; // no conditions are satisfied
            } else if (type==OR) {// not X1 and not X2 and not...
                for (Condition_region condition:conditions) {
                    if (condition!=null && condition.isConditionSatisfied(sequencename, region, task)) return false;
                }
                return true; // no conditions are satisfied
            } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: the 'type' parameter to boolean condition should be 0=AND, or 1=OR but got: "+type);         
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public Condition_region_boolean clone() {
        ArrayList<Condition_region> conditionlist=null;
        if (conditions!=null) {
            conditionlist=new ArrayList<Condition_region>(conditions.size());
            for (Condition_region condition:conditions) {
                if (condition==null) conditionlist.add(null);
                else conditionlist.add(condition.clone());
            }
        }
        Condition_region_boolean thisclone=new Condition_region_boolean(conditionlist, type);
        return thisclone;
    }
    
    @Override
    public void importCondition(Condition other) throws ClassCastException {
        if (other==null) throw new ClassCastException("Unable to import from NULL condition");
        if (!other.getClass().equals(this.getClass())) throw new ClassCastException("Unable to import from condition of different class. Expected '"+this.getClass()+"' but got '"+other.getClass()+"'.");
        isResolved=((Condition_region_boolean)other).isResolved;
        type=((Condition_region_boolean)other).type;
        conditions=((Condition_region_boolean)other).conditions;
    }     
}
