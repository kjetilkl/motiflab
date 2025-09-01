/*
 
 
 */

package org.motiflab.engine.operations;

import org.motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericConstant;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.NumericSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.TextVariable;
/**
 *
 * @author kjetikl
 */
public class Condition_position extends Condition {
    private boolean isResolved=false;
    
    public Condition_position() {
        super();
    }
    
    public Condition_position(HashMap<String,Object> initialValues) {
        super(initialValues);
    }      
    
    @Override
    public void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError {
        Data operandA=null;
        Data operandB=null;
        Data operandB2=null;
        String operandAstring=getOperandAString();
        if (operandAstring!=null) {                     
           String operandBstring=getOperandBString();
           String operandB2string=getOperandB2String();
           operandA=engine.getDataItem(operandAstring);                 
           if (operandA==null || !(operandA instanceof FeatureDataset)) throw new ExecutionError("First operand '"+operandAstring+"' in where-clause is not a recognized Feature Dataset",task.getLineNumber());
           if (operandBstring!=null) {
              if (operandBstring.startsWith("\"") && operandBstring.endsWith("\"")) {
                if (operandBstring.length()<3) throw new ExecutionError("Empty string in condition",task.getLineNumber());
                operandBstring=operandBstring.substring(1, operandBstring.length()-1); 
                operandB=new TextVariable(operandBstring, operandBstring);
              } else {
                  operandB=engine.getDataItem(operandBstring);
                  if (operandB==null) {
                        try {
                           double value=Double.parseDouble(operandBstring);
                           operandB=new NumericConstant(operandBstring, (double)value);
                        } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandBstring+"' neither data nor numeric constant",task.getLineNumber());}
                  } // end operandB==null   
              }
              if (operandA instanceof RegionDataset && !(operandB instanceof RegionDataset)) throw new ExecutionError("Second operand '"+operandBstring+"' in condition should match first operand (region data)",task.getLineNumber());
              if (operandA instanceof NumericDataset && !(operandB instanceof NumericDataset || operandB instanceof NumericVariable || operandB instanceof SequenceNumericMap || operandB instanceof NumericConstant)) throw new ExecutionError("Second operand '"+operandBstring+"' in condition should match first operand (numerical)",task.getLineNumber());
              if (operandA instanceof DNASequenceDataset && !(operandB instanceof DNASequenceDataset || operandB instanceof TextVariable)) throw new ExecutionError("Second operand '"+operandBstring+"' in condition should match first operand (dna base)",task.getLineNumber());
           } // end operandBstring!=null
           if (operandB2string!=null) {
              operandB2=engine.getDataItem(operandB2string);
              if (operandB2==null) {
                    try {
                       double value=Double.parseDouble(operandB2string);
                       operandB2=new NumericConstant(operandB2string, (double)value);
                    } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandB2string+"' neither data nor numeric constant",task.getLineNumber());}
              } // end operandB2==null
              if (!(operandB2 instanceof NumericDataset || operandB2 instanceof NumericVariable || operandB2 instanceof SequenceNumericMap || operandB2 instanceof NumericConstant)) throw new ExecutionError("Operand '"+operandB2string+"' should be of numerical type",task.getLineNumber());
           } // end operandB2string!=null
        } // end whereString!=null        

        setOperandAData(operandA);
        setOperandBData(operandB);
        if (operandB2!=null) setOperandB2Data(operandB2);
        //if (operandA!=null) System.err.println("OperandA resolved: "+operandA.getName()+" ["+operandA.getTypeDescription()+"]");
        //if (operandB!=null) System.err.println("OperandB resolved: "+operandB.getName()+" ["+operandB.getTypeDescription()+"]");
        isResolved=true;
    }    

    /**
     * Returns true if this Condition holds at the specified genomic position
     * @param sequencename
     * @param pos the Genomic position! where this condition should be tested
     * @param task
     * @return
     * @throws java.lang.Exception
     */
    protected boolean isConditionSatisfied(String sequencename, int pos, OperationTask task) throws Exception {
        if (!isResolved) {
            task.getEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_position is not 'resolved' before use"));
        }
        String whereString=getOperandAString();
        if (whereString==null) return true; // no condition is set, so the transform should be applied everywhere
        Data operandA=getOperandAData();
        Data operandB=getOperandBData();
        Data operandB2=getOperandB2Data();
        String comparator=getComparator();
        boolean whereNot=false;
        Boolean whereNotBoolean=negateAll();
        if (whereNotBoolean!=null) whereNot=whereNotBoolean.booleanValue();         
        boolean returnValue=true;
        if (operandA instanceof RegionDataset && operandB==null) {// --- single Region comparison
            RegionSequenceData a=(RegionSequenceData)((RegionDataset)operandA).getSequenceByName(sequencename);         
            if (!comparator.equals("inside")) {throw new Exception("Sloppy programming error: comparator is not 'inside' for single region comparison");}
            returnValue=(a.getNumberOfRegionsAtGenomicPosition(pos)>0);                                  
        } // --------------------------------------------------------
        else if (operandA instanceof RegionDataset && operandB!=null && operandB instanceof RegionDataset) { // --- compare Region to Region
            RegionSequenceData a=(RegionSequenceData)((RegionDataset)operandA).getSequenceByName(sequencename);         
            RegionSequenceData b=(RegionSequenceData)((RegionDataset)operandB).getSequenceByName(sequencename);         

            if (comparator.equals("bases overlap")) { 
                   returnValue=(a.getNumberOfRegionsAtGenomicPosition(pos)>0 && b.getNumberOfRegionsAtGenomicPosition(pos)>0);
            } else if (comparator.equals("bases not overlap")) { 
                   returnValue=(a.getNumberOfRegionsAtGenomicPosition(pos)>0 && b.getNumberOfRegionsAtGenomicPosition(pos)==0);
            } else if (comparator.equals("regions overlap")) { 
                   returnValue=false;
                   ArrayList<Region> regionsAtpos=a.getValueAtGenomicPosition(pos);
                   for (Region reg:regionsAtpos) {
                       if (b.getNumberOfRegionsOverlappingGenomicInterval(reg.getGenomicStart(), reg.getGenomicEnd())>0) {returnValue=true;break;}
                   }
            } else if (comparator.equals("regions not overlap")) { 
                   returnValue=false;
                   ArrayList<Region> regionsAtpos=a.getValueAtGenomicPosition(pos);
                   if (regionsAtpos.size()>0) {
                     returnValue=true;
                     for (Region reg:regionsAtpos) {
                       if (b.getNumberOfRegionsOverlappingGenomicInterval(reg.getGenomicStart(), reg.getGenomicEnd())>0) {returnValue=false;break;}
                     }
                   }
            } else throw new ExecutionError("comparator '"+comparator+"' not applicable here",task.getLineNumber());                            
         } // --------------------------------------------------------
         else if (operandA instanceof NumericDataset) { // 
            NumericSequenceData seqdata=(NumericSequenceData)((NumericDataset)operandA).getSequenceByName(sequencename);
            double valueA=seqdata.getValueAtGenomicPosition(pos).doubleValue();
            double valueB=0;
            double valueB2=0;
            if (operandB instanceof NumericDataset) {
                 NumericSequenceData seqdataB=(NumericSequenceData)((NumericDataset)operandB).getSequenceByName(sequencename);
                 valueB=seqdataB.getValueAtGenomicPosition(pos).doubleValue();
            }
            else if (operandB instanceof NumericSequenceData) valueB=((NumericSequenceData)operandB).getValueAtGenomicPosition(pos).doubleValue();
            else if (operandB instanceof NumericConstant) valueB=((NumericConstant)operandB).getValue().doubleValue();
            else if (operandB instanceof SequenceNumericMap) valueB=((SequenceNumericMap)operandB).getValue(sequencename).doubleValue();
            else if (operandB instanceof NumericVariable) valueB=((NumericVariable)operandB).getValue().doubleValue();

            if (operandB2 instanceof NumericDataset) {
                 NumericSequenceData seqdataB2=(NumericSequenceData)((NumericDataset)operandB2).getSequenceByName(sequencename);
                 valueB2=seqdataB2.getValueAtGenomicPosition(pos).doubleValue();
            }
            else if (operandB2 instanceof NumericSequenceData) valueB2=((NumericSequenceData)operandB2).getValueAtGenomicPosition(pos).doubleValue();
            else if (operandB2 instanceof NumericConstant) valueB2=((NumericConstant)operandB2).getValue().doubleValue();
            else if (operandB2 instanceof SequenceNumericMap) valueB2=((SequenceNumericMap)operandB2).getValue(sequencename).doubleValue();
            else if (operandB2 instanceof NumericVariable) valueB2=((NumericVariable)operandB2).getValue().doubleValue();

            if (comparator.equals("="))  returnValue=(valueA==valueB);
            else if (comparator.equals(">=")) returnValue=(valueA>=valueB);
            else if (comparator.equals(">"))  returnValue=(valueA>valueB);
            else if (comparator.equals("<"))  returnValue=(valueA<valueB);
            else if (comparator.equals("<=")) returnValue=(valueA<=valueB);
            else if (comparator.equals("<>")) returnValue=(valueA!=valueB);
            else if (comparator.equals("in")) returnValue=(valueA>=valueB && valueA<=valueB2);
            //System.err.println("At "+sequencename+"["+pos+"]=> "+valueA+""+comparator+""+valueB+"  => "+returnValue);
        } // --------------------------------------------------------
         else if (operandA instanceof DNASequenceDataset) { // 
            boolean useRelativeStrand=false; // only really needed when comparing to literal characters (i.e. TextVariables)
            if (comparator.endsWith(" relative strand")) {useRelativeStrand=true; comparator=comparator.substring(0,comparator.indexOf(" relative strand"));}
            DNASequenceData seqdata=(DNASequenceData)((DNASequenceDataset)operandA).getSequenceByName(sequencename);
            Character valueA=seqdata.getValueAtGenomicPosition(pos);
            Character valueB=null;            
            if (operandB instanceof DNASequenceDataset) {
                 DNASequenceData seqdataB=(DNASequenceData)((DNASequenceDataset)operandB).getSequenceByName(sequencename);
                 valueB=seqdataB.getValueAtGenomicPosition(pos);
            }
            else if (operandB instanceof TextVariable) {
                String string=((TextVariable)operandB).getFirstValue();
                if (string==null || string.isEmpty()) throw new ExecutionError("Empty text variable in comparison ("+operandB.getName()+")",task.getLineNumber());                     
                valueB=string.charAt(0); // use first character
                if (useRelativeStrand && seqdata.getStrandOrientation()==Sequence.REVERSE) valueA=MotifLabEngine.reverseBase(valueA);               
            }
            
            if (comparator.equals("is lowercase"))  returnValue=Character.isLowerCase(valueA);
            else if (comparator.equals("is uppercase")) returnValue=Character.isUpperCase(valueA);
            else if (comparator.equals("case-sensitive equals"))  returnValue=(valueA!=null && valueB!=null && valueA.charValue()==valueB.charValue());
            else if (comparator.equals("equals"))  returnValue=(valueA!=null && valueB!=null && Character.toUpperCase(valueA)==Character.toUpperCase(valueB));
            else if (comparator.equals("has same case as"))  returnValue=(valueA!=null && valueB!=null && ( (Character.isLowerCase(valueA) && Character.isLowerCase(valueB)) || (Character.isUpperCase(valueA) && Character.isUpperCase(valueB)) ));
            else if (comparator.equals("matches")) {
                if (valueA==null || valueB==null) returnValue=false; 
                else {
                  char charA=Character.toUpperCase(valueA);
                  char charB=Character.toUpperCase(valueB);
                  switch (charA) {
                      case 'A': returnValue=(charB=='A' || charB=='R' || charB=='M' || charB=='W' || charB=='D' || charB=='H' || charB=='V' || charB=='N');break;
                      case 'C': returnValue=(charB=='C' || charB=='Y' || charB=='M' || charB=='S' || charB=='B' || charB=='H' || charB=='V' || charB=='N');break;
                      case 'G': returnValue=(charB=='G' || charB=='R' || charB=='K' || charB=='S' || charB=='B' || charB=='D' || charB=='V' || charB=='N');break;
                      case 'T': returnValue=(charB=='T' || charB=='Y' || charB=='K' || charB=='W' || charB=='B' || charB=='D' || charB=='H' || charB=='N');break;
                      default:  returnValue=(charA==charB);
                  }
                }
            }
        } // --------------------------------------------------------                 
        else System.err.println("SYSTEM-ERROR in Condition_position: OperandA="+operandA);
        
        if (whereNot) return !returnValue;
        else return returnValue;
    } 
    
    @SuppressWarnings("unchecked")    
    @Override
    public Condition_position clone() {
        Condition_position newdata=new Condition_position((HashMap<String, Object>)storage.clone());
        return newdata;
    }   
    
    @Override
    public void importCondition(Condition other) throws ClassCastException {
        if (other==null) throw new ClassCastException("Unable to import from NULL condition");
        if (!other.getClass().equals(this.getClass())) throw new ClassCastException("Unable to import from condition of different class. Expected '"+this.getClass()+"' but got '"+other.getClass()+"'.");
        isResolved=((Condition_position)other).isResolved;
        storage=(HashMap<String, Object>)((Condition_position)other).storage.clone();
    }      
}
