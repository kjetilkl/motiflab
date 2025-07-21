/*
 
 
 */

package org.motiflab.engine.operations;

import org.motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ExecutionError;
import org.motiflab.gui.SelectionWindow;
/**
 *
 * @author kjetikl
 */
public class Condition_within extends Condition {
    private boolean isResolved=false;
    private boolean isSet=false; // is this condition set at all?
    
    public Condition_within() {
        super();
    }
    
    public Condition_within(HashMap<String,Object> initialValues) {
        super(initialValues);
    }      
    
    /** Creates a new Condition_within based on a list of selection windows */
    public Condition_within(ArrayList<SelectionWindow>list) {
        super();
        if (list!=null && list.size()>0) {
            StringBuilder builder=new StringBuilder(list.get(0).toString());
            for (int i=1;i<list.size();i++) {
                builder.append(",");
                builder.append(list.get(i).toString());
            }
            setOperandAString(builder.toString());
        }
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    public void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError {
        String operandAstring=getOperandAString();
        if (operandAstring!=null) {
             isSet=true;
             String[] regions=operandAstring.split(",");
             for (String region:regions) {
                 region=region.trim();
                 int colonpos=region.indexOf(':');
                 int dashpos=region.indexOf('-');
                 if (colonpos<=0 || dashpos<=0 || colonpos>dashpos || colonpos==region.length()-1 || dashpos==region.length()-1 || dashpos==colonpos+1) throw new ExecutionError("Sequence subrange should be on the format 'seqname:startpos-endpos'");
                 try {
                     String seqname=region.substring(0, colonpos);
                     String startposstring=region.substring(colonpos+1,dashpos);
                     String endposstring=region.substring(dashpos+1,region.length());
                     int startpos=Integer.parseInt(startposstring);
                     int endpos=Integer.parseInt(endposstring);
                     if (getParameter(seqname)==null) setParameter(seqname, new ArrayList<int[]>());
                     ArrayList<int[]>ranges=(ArrayList<int[]>)getParameter(seqname);
                     ranges.add(new int[]{startpos,endpos});
                 } catch (Exception e) {
                     throw new ExecutionError("Sequence window should be on the format 'seqname:startpos-endpos'");
                 }
             }
        } // end operandAstring!=null         
        isResolved=true;
    }    

    /** Returns true if this sequence has a selected windows in which the operation should be applied 
     *  If sequence subranges ("within conditions") are in use but this sequence does not contain a selection range
     *  then the entire sequence can be skipped when transforming a dataset 
     */
    public boolean existsSelectionWithinSequence(String sequenceName, OperationTask task) {
        if (!isResolved) {
            task.getEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_within is not 'resolved' before use"));
        }
        return getParameter(sequenceName)!=null;
    }
    
    @Override
    public String toString() {
        return getOperandAString();
    }
    
    /**
     * Returns true if this Condition holds at the specified genomic position
     * @param sequencename
     * @param pos the Genomic position! where this condition should be tested
     * @param task
     * @return
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    protected boolean isConditionSatisfied(String sequencename, int pos, OperationTask task) throws Exception {
        if (!isResolved) {
            task.getEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_within is not 'resolved' before use"));
        }
        if (!isSet) return true; // no condition is set, so the transform should be applied everywhere
        boolean whereNot=false;
        Boolean whereNotBoolean=negateAll();
        if (whereNotBoolean!=null) whereNot=whereNotBoolean.booleanValue();         
        boolean returnValue=false;
        ArrayList<int[]>ranges=(ArrayList<int[]>)getParameter(sequencename);
        if (ranges!=null) {
            for (int[] range:ranges) {
                if (pos>=range[0] && pos<=range[1]) {returnValue=true;break;} // is position within this window
            }
        }
        if (whereNot) return !returnValue;
        else return returnValue;
    } 
    
   /**
     * Similar to the method above but rather than seeing if a single position lies within a window
     * this checks whether the given region (from start to end) overlaps with a window
     */
    @SuppressWarnings("unchecked")
    protected boolean isConditionSatisfied(String sequencename, int start, int end, OperationTask task) throws Exception {
        if (!isResolved) {
            task.getEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_within is not 'resolved' before use"));
        }
        if (!isSet) return true; // no condition is set, so the transform should be applied everywhere
        boolean whereNot=false;
        Boolean whereNotBoolean=negateAll();
        if (whereNotBoolean!=null) whereNot=whereNotBoolean.booleanValue();         
        boolean returnValue=false;
        ArrayList<int[]>ranges=(ArrayList<int[]>)getParameter(sequencename);
        for (int[] range:ranges) {
            if (!(start>range[1] || end<range[0])) {returnValue=true;break;} // does region overlap this window
        }
        if (whereNot) return !returnValue;
        else return returnValue;
    } 
    
    
    
    @SuppressWarnings("unchecked")    
    @Override
    public Condition_within clone() {
        Condition_within newdata=new Condition_within((HashMap<String, Object>)storage.clone());
        return newdata;
    } 
    
    @Override
    public void importCondition(Condition other) throws ClassCastException {
        if (other==null) throw new ClassCastException("Unable to import from NULL condition");
        if (!other.getClass().equals(this.getClass())) throw new ClassCastException("Unable to import from condition of different class. Expected '"+this.getClass()+"' but got '"+other.getClass()+"'.");
        isResolved=((Condition_within)other).isResolved;
        isSet=((Condition_within)other).isSet;
        storage=(HashMap<String, Object>)((Condition_within)other).storage.clone();
    }     
}
