/*
 
 
 */

package motiflab.engine.operations;
import java.util.HashMap;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.Data;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.DataMap;
import motiflab.engine.data.DataPartition;
import motiflab.engine.data.NumericMap;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.TextVariable;
import motiflab.engine.data.TextMap;
import motiflab.engine.task.ConditionalTask;
import motiflab.engine.task.OperationTask;

/**
 *
 * @author kjetikl
 */
public class Condition_basic extends Condition {
    private boolean isResolved=false;
    
    public Condition_basic() {
        super();
    }
    public Condition_basic(HashMap<String,Object> initialValues) {
        super(initialValues);
    }

    @Override
    public void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError {
       isResolved=false; // don't use this! use the one below instead 
    }
        
    public void resolve(ConditionalTask task) throws ExecutionError {
        MotifLabEngine engine=task.getMotifLabEngine();
        Data operandA=null;
        Data operandB=null;
        Data operandB2=null;
        String operandAstring=getOperandAString();               
        String operandBstring=getOperandBString();
        String operandB2string=getOperandB2String();
     
        if (operandAstring==null || operandBstring==null || operandAstring.isEmpty() ||  operandBstring.isEmpty()) throw new ExecutionError("Missing operand(s) in condition",task.getLineNumber());
        
        // resolve first operand (operandA). This must be a Numeric Variable or Text Variable
        operandA=engine.getDataItem(operandAstring);
        if (operandA==null) throw new ExecutionError("Unrecognized token '"+operandAstring+"' is not a data object",task.getLineNumber()); 
        
        // resolve second operand (operandB). This can be a literal string (enclosed in double quotes), literal number or the name of a data object
        if (operandBstring.startsWith("\"") && operandBstring.endsWith("\"")) { // literal string
            operandBstring=operandBstring.substring(1, operandBstring.length()-1); // removes quotes  
            operandB=new TextVariable("noname", operandBstring); // enclose literal expression in a TextVariable
       } else {
           operandB=engine.getDataItem(operandBstring);
           if (operandB==null) { // operandB is not a literal text, nor a data object so it should be a literal number
              try {
                  double value=Double.parseDouble(operandBstring);
                  operandB=new NumericVariable(operandBstring, (double)value);
              } catch (Exception e) {throw new ExecutionError("'"+operandBstring+"' must be a text string (in double quotes), a number or a data object",task.getLineNumber());}                 
           }
        } 
        
        // check that the two operands are comparable
        if (!(operandA instanceof NumericVariable || operandA instanceof TextVariable)) { // NumericVariables and TextVariables can be compared to lots of things. Other types must match
            if (!(operandA.getClass().equals(operandB.getClass()))) throw new ExecutionError("The two operands are not comparable. They must be of the same type unless the first operand is a Numeric Variable or Text Variable",task.getLineNumber());
        }
                
       // resolve third operand (operandB2). This must be a literal number or the name of a Numeric Variable
        if (operandB2string!=null && !operandB2string.isEmpty()) {
            operandB2=engine.getDataItem(operandB2string);
            if (operandB2!=null) { // operandB2 is a data object. Check that it is a NumericVariable
                if (!(operandB2 instanceof NumericVariable || operandB2 instanceof NumericMap)) throw new ExecutionError("'"+operandB2string+"' must be a number or Numeric Variable (or Map)",task.getLineNumber());
            } else { // operandB2 is a not a data object. Then it should be a literal number
                  try {
                     double value=Double.parseDouble(operandB2string);
                     operandB2=new NumericVariable(operandB2string, (double)value);
                  } catch (Exception e) {throw new ExecutionError("'"+operandB2string+"' must be a number or Numeric Variable",task.getLineNumber());}                
            }
        }
        setOperandAData(operandA);
        setOperandBData(operandB); 
        setOperandB2Data(operandB2);
        isResolved=true;
    }  

    
    
    /**
     * Returns true if this Condition is satisfied
     * @param task
     * @return
     * @throws java.lang.Exception
     */
    public boolean isConditionSatisfied(ConditionalTask task) throws Exception {
        if (!isResolved) resolve(task);

        Data operandA=getOperandAData(); // the first operand A (as in: A < B)
        Data operandB=getOperandBData();  // the second operand B (as in: A < B) 
        Data operandB2=getOperandB2Data(); // upper limit of the second operand if this is a range  (as in: A in range [B,B2])
        String comparator=getComparator();
        boolean whereNot=false;
        Boolean whereNotBoolean=negateAll();
        if (whereNotBoolean!=null) whereNot=whereNotBoolean.booleanValue();         
        boolean returnValue=true;
         
        if (operandA instanceof TextVariable) { // compare text 
              if (operandB instanceof TextMap) operandB=new TextVariable(operandB.getName(),((TextMap)operandB).getValue()); // Convert TextMap to TextVariable for further comparison. Just use default value from the map.
            
              if (operandB instanceof TextVariable) { // compare two texts
                       if (comparator.equals("equals")) returnValue=((TextVariable)operandA).containsSameData((TextVariable)operandB); // must be exactly the same (not just same set)
                  else if (comparator.equals("=")) returnValue=((TextVariable)operandA).containsSameEntries((TextVariable)operandB); // same set (contains same text but not in same order)
                  else if (comparator.equals("<=") || comparator.equals("is in") || comparator.equals("in set")) returnValue=((TextVariable)operandA).isSubsetOf((TextVariable)operandB);
                  else if (comparator.equals("<")) returnValue=((TextVariable)operandA).isProperSubsetOf((TextVariable)operandB);                  
                  else if (comparator.equals(">=")) returnValue=((TextVariable)operandA).isSupersetOf((TextVariable)operandB);                  
                  else if (comparator.equals(">")) returnValue=((TextVariable)operandA).isProperSupersetOf((TextVariable)operandB);                                  
                  else if (comparator.equals("matches") || comparator.equals("matches in")) returnValue=textCollectionMatchesText((TextVariable)operandB,(TextVariable)operandA);
                  else throw new ExecutionError("The comparator '"+comparator+"' cannot be used in this context",task.getLineNumber());
              } else if (operandB instanceof DataCollection) { // compare text (as collection versus data collection). Convert collection to textvariable first
                  TextVariable text1=(TextVariable)operandA;
                  TextVariable text2=new TextVariable("temp",((DataCollection)operandB).getValues());
                       if (comparator.equals("equals") || comparator.equals("=")) returnValue=text1.containsSameEntries(text2); //
                  else if (comparator.equals("<=") || comparator.equals("is in") || comparator.equals("in set")) returnValue=text1.isSubsetOf(text2);
                  else if (comparator.equals("<")) returnValue=text1.isProperSubsetOf(text2);                  
                  else if (comparator.equals(">=")) returnValue=text1.isSupersetOf(text2);                  
                  else if (comparator.equals(">")) returnValue=text1.isProperSupersetOf(text2);  
                  else throw new ExecutionError("The comparator '"+comparator+"' cannot be used in this context",task.getLineNumber());
              } else throw new ExecutionError("The Text Variable '"+getOperandAString()+"' cannot be compared against "+getOperandBString(),task.getLineNumber());
        } else if (operandA instanceof NumericVariable) { // compare numeric values
              double valueA=0;
              double valueB=0;
              double valueB2=0;
//                   if (operandA instanceof NumericMap) valueA=((NumericMap)operandA).getValue(); // use default value
//              else if (operandA instanceof NumericVariable) valueA=((NumericVariable)operandA).getValue();              
              valueA=((NumericVariable)operandA).getValue();                   
                   
              if (operandB==null) throw new ExecutionError("SystemError: missing second operand in condition comparison",task.getLineNumber());
                   if (operandB instanceof NumericMap) valueB=((NumericMap)operandB).getValue();  // use default value
              else if (operandB instanceof NumericVariable) valueB=((NumericVariable)operandB).getValue();
                   
              if (operandB2!=null) {
                     if (operandB2 instanceof NumericMap) valueB2=((NumericMap)operandB2).getValue();
                else if (operandB2 instanceof NumericVariable) valueB2=((NumericVariable)operandB2).getValue();
              }
              returnValue=compareNumerical(valueA, valueB, valueB2, comparator);              
        } else if (operandA instanceof DataCollection && operandB instanceof DataCollection) {
              if (comparator.equals("equals") || comparator.equals("=")) returnValue=((DataCollection)operandA).containsSameEntries((DataCollection)operandB);
              else if (comparator.equals("<>")) returnValue=!((DataCollection)operandA).containsSameEntries((DataCollection)operandB);
              else if (comparator.equals("<=") || comparator.equals("is in") || comparator.equals("in set")) returnValue=((DataCollection)operandA).isSubsetOf((DataCollection)operandB);
              else if (comparator.equals("<")) returnValue=((DataCollection)operandA).isProperSubsetOf((DataCollection)operandB);
              else if (comparator.equals(">=")) returnValue=((DataCollection)operandA).isSupersetOf((DataCollection)operandB);              
              else if (comparator.equals(">")) returnValue=((DataCollection)operandA).isProperSupersetOf((DataCollection)operandB);
              else if (comparator.equals("overlaps")) returnValue=((DataCollection)operandA).overlaps((DataCollection)operandB);              
              else throw new ExecutionError("The comparator '"+comparator+"' cannot be used for comparing collections",task.getLineNumber());            
        } else if (operandA instanceof DataMap && operandB instanceof DataMap) {
              if (comparator.equals("equals") || comparator.equals("=")) returnValue=((DataMap)operandA).containsSameMappings((DataMap)operandB);
              else if (comparator.equals("<>")) returnValue=!((DataMap)operandA).containsSameMappings((DataMap)operandB);
              else throw new ExecutionError("The comparator '"+comparator+"' cannot be used for comparing maps",task.getLineNumber());                    
        } else if (operandA instanceof DataPartition && operandB instanceof DataPartition) {
              if (comparator.equals("equals") || comparator.equals("=")) returnValue=((DataPartition)operandA).containsSameMappings((DataPartition)operandB);
              else if (comparator.equals("<>")) returnValue=!((DataPartition)operandA).containsSameMappings((DataPartition)operandB);
              else throw new ExecutionError("The comparator '"+comparator+"' cannot be used for comparing partitions",task.getLineNumber());                         
        } else if (operandA.getClass().equals(operandB.getClass())) {
              if (comparator.equals("equals") || comparator.equals("=")) returnValue=operandA.containsSameData(operandB);
              else if (comparator.equals("<>")) returnValue=!(operandA.containsSameData(operandB));
              else throw new ExecutionError("The comparator '"+comparator+"' cannot be used for comparing data objects of type "+operandA.getDynamicType(),task.getLineNumber());              
        } else throw new ExecutionError("SystemError: unable to decide how to perform condition comparison",task.getLineNumber());
                
        if (whereNot) return !returnValue;
        else return returnValue;
    } 
 
   
    
    /** 
     * Returns true if the text in the first TextVariable (collection) matches the text in the target TextVariable.
     * If the target TextVariable contains multiple values, they are checked one by one and all must have matches in the first collection.
     */
    private boolean textCollectionMatchesText(TextVariable collection, TextVariable targetText) {
        if (targetText.size()==1) return collection.matches(targetText.getFirstValue());
        for (String entry:targetText.getAllStrings()) {
            if (!collection.matches(entry)) return false;
        }
        return true;
    }      
    
    /** Compares two numerical values with the specified comparator (the second value could be a range)  */
    private boolean compareNumerical(double valueA, double valueB, double valueB2, String comparator) {
        boolean returnValue=false;
             if (comparator.equals("="))  returnValue=(valueA==valueB);
        else if (comparator.equals(">=")) returnValue=(valueA>=valueB);
        else if (comparator.equals(">"))  returnValue=(valueA>valueB);
        else if (comparator.equals("<"))  returnValue=(valueA<valueB);
        else if (comparator.equals("<=")) returnValue=(valueA<=valueB);
        else if (comparator.equals("<>")) returnValue=(valueA!=valueB);  
        else if (comparator.equals("in")) returnValue=(valueA>=valueB && valueA<=valueB2);
        return returnValue;
    }

  
    
    @SuppressWarnings("unchecked")  
    @Override
    public Condition_basic clone() {
        Condition_basic newdata=new Condition_basic((HashMap<String, Object>)storage.clone());
        return newdata;
    }     
    
    @Override
    public void importCondition(Condition other) throws ClassCastException {
        if (other==null) throw new ClassCastException("Unable to import from NULL condition");
        if (!other.getClass().equals(this.getClass())) throw new ClassCastException("Unable to import from condition of different class. Expected '"+this.getClass()+"' but got '"+other.getClass()+"'.");
        isResolved=((Condition_basic)other).isResolved;
        storage=(HashMap<String, Object>)((Condition_basic)other).storage.clone();
    }      
}
