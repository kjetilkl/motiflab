/*
 
 
 */

package motiflab.engine.operations;
import motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ExecutionError;
import motiflab.engine.data.Data;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.ModuleNumericMap;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifNumericMap;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericConstant;
import motiflab.engine.data.NumericMap;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Region;
import motiflab.engine.data.TextVariable;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceTextMap;
import motiflab.engine.data.TextMap;

/**
 *
 * @author kjetikl
 */
public class Condition_region extends Condition {
    private boolean isResolved=false;
    
    public Condition_region() {
        super();
    }
    public Condition_region(HashMap<String,Object> initialValues) {
        super(initialValues);
    }   
    
    public String getCompareProperty() {return (String)getParameter("compareProperty");}
    public void setCompareProperty(String compareProperty) {setParameter("compareProperty",compareProperty);}
    
    public String getOtherRegionTypeRestriction() {return (String)getParameter("otherRegionTypeRestriction");}
    public void setOtherRegionTypeRestriction(String restriction) {setParameter("otherRegionTypeRestriction",restriction);}

    public Class getUserDefinedPropertyType() {return (Class)getParameter("userDefinedPropertyType");}
    public void setUserDefinedPropertyType(Class type) {setParameter("userDefinedPropertyType",type);}

    
    @Override
    public void resolve(MotifLabEngine engine, OperationTask task) throws ExecutionError {
        Data operandA=null;
        Data operandB=null;
        Data operandB2=null;
        String operandAstring=getOperandAString();               
        String operandBstring=getOperandBString();
        String operandB2string=getOperandB2String();
        String compareProperty=getCompareProperty();
        Class userDefinedPropertyType=getUserDefinedPropertyType();        
        if (operandBstring==null) throw new ExecutionError("Missing operand in condition",task.getLineNumber());
        if (compareProperty==null) throw new ExecutionError("Unable to decide type of condition",task.getLineNumber());                     
        else if (compareProperty.equals("") && userDefinedPropertyType==null) {} // nothing to resolve?
        else if (compareProperty.equals("type") || userDefinedPropertyType==String.class || userDefinedPropertyType==Boolean.class) {
            if (compareProperty.isEmpty()) throw new ExecutionError("Missing name of property");   
            if (operandBstring.startsWith("\"") && operandBstring.endsWith("\"")) { // literal string
                 operandBstring=operandBstring.substring(1, operandBstring.length()-1); // removes quotes
                 //if (operandBstring.isEmpty()) throw new ExecutionError("Empty text expression in comparison",task.getLineNumber());      
                 operandB=new TextVariable("noname", operandBstring); // enclose literal expression in a TextVariable
             } else {
                operandB=engine.getDataItem(operandBstring);
                if (operandB==null) throw new ExecutionError("Unrecognized token '"+operandBstring+"' is not a data object",task.getLineNumber());      
                else if (!(operandB instanceof DataCollection || operandB instanceof TextMap || operandB instanceof TextVariable)) throw new ExecutionError("Operand '"+operandBstring+"' is not a valid Collection, Text Map or Text Variable",task.getLineNumber());      
             }
        } else if (compareProperty.equals("score") || compareProperty.equals("length") || userDefinedPropertyType==Number.class) {
              if (compareProperty.isEmpty()) throw new ExecutionError("Missing name of property");  
              operandB=engine.getDataItem(operandBstring);
              if (operandB==null) {
                  if (operandBstring.contains("property \"")) operandB=new RegionDataset("dummy"); // The new RegionDataset is just an indicator for later
                  else try {
                     double value=Double.parseDouble(operandBstring);
                     operandB=new NumericConstant(operandBstring, (double)value);
                  } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandBstring+"' neither data nor numeric constant",task.getLineNumber());}
              } // end operandB==null               
              if (!(operandB instanceof NumericVariable || operandB instanceof NumericMap || operandB instanceof NumericConstant || operandB instanceof RegionDataset)) throw new ExecutionError("Operand '"+operandBstring+"' should be of numerical type",task.getLineNumber());
              if (operandB2string!=null) {
                  operandB2=engine.getDataItem(operandB2string);
                  if (operandB2==null) {
                      if (operandB2string.contains("property \"")) operandB2=new RegionDataset("dummy"); // The new RegionDataset is just an indicator for later
                      else try {
                         double value=Double.parseDouble(operandB2string);
                         operandB2=new NumericConstant(operandB2string, (double)value);
                      } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandB2string+"' neither data nor numeric constant",task.getLineNumber());}
                  } // end operandB2==null               
                  if (!(operandB2 instanceof NumericVariable || operandB2 instanceof NumericMap || operandB2 instanceof NumericConstant || operandB2 instanceof RegionDataset)) throw new ExecutionError("Operand '"+operandB2string+"' should be of numerical type",task.getLineNumber());
              }
        } else if (compareProperty.startsWith("distance to")) {
              if (!operandAstring.equals("interaction partner")) {
                  operandA=engine.getDataItem(operandAstring);
                  if (!(operandA instanceof RegionDataset)) throw new ExecutionError("Operand '"+operandAstring+"' in distance condition should be a Region Dataset",task.getLineNumber());
              }
              operandB=engine.getDataItem(operandBstring);
              if (operandB==null) {
                  if (operandBstring.contains("property \"")) operandB=new RegionDataset("dummy"); // The new RegionDataset is just an indicator for later
                  else try {
                     double value=Double.parseDouble(operandBstring);
                     operandB=new NumericConstant(operandBstring, value);
                  } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandBstring+"' neither data nor numeric constant",task.getLineNumber());}
              } // end operandB==null
              if (!(operandB instanceof NumericVariable || operandB instanceof NumericMap || operandB instanceof NumericConstant || operandB instanceof RegionDataset)) throw new ExecutionError("Operand '"+operandBstring+"' should be of numerical type",task.getLineNumber());
              if (operandB2string!=null) {
                  operandB2=engine.getDataItem(operandB2string);
                  if (operandB2==null) {
                      if (operandB2string.contains("property \"")) operandB2=new RegionDataset("dummy"); // The new RegionDataset is just an indicator for later
                      else try {
                         double value=Double.parseDouble(operandB2string);
                         operandB2=new NumericConstant(operandB2string, (double)value);
                      } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandB2string+"' neither data nor numeric constant",task.getLineNumber());}
                  } // end operandB2==null               
                  if (!(operandB2 instanceof NumericVariable || operandB2 instanceof NumericMap || operandB2 instanceof NumericConstant || operandB2 instanceof RegionDataset)) throw new ExecutionError("Operand '"+operandB2string+"' should be of numerical type",task.getLineNumber());
              }
        } else if (compareProperty.equals("overlaps") || compareProperty.equals("inside") || compareProperty.equals("covers") || compareProperty.equals("present in") || compareProperty.equals("similar in")) {
              operandB=engine.getDataItem(operandBstring);
              if (operandB==null) throw new ExecutionError("Unrecognized token '"+operandBstring+"' is not a data object",task.getLineNumber());              
              if (!(operandB instanceof RegionDataset)) throw new ExecutionError("Second operand '"+operandBstring+"' in "+compareProperty+"-comparison should be a Region Dataset",task.getLineNumber());
        } else if (operandAstring!=null) { // compare numeric: avg|min|max|median|etc....
             operandA=engine.getDataItem(operandAstring);
             if (!(operandA instanceof NumericDataset)) throw new ExecutionError("Operand '"+operandAstring+"' in condition should be a Numeric Dataset",task.getLineNumber());            
              operandB=engine.getDataItem(operandBstring);
              if (operandB==null) {
                  if (operandBstring.contains("property \"")) operandB=new RegionDataset("dummy"); // The new RegionDataset is just an indicator for later
                  else try {
                     double value=Double.parseDouble(operandBstring);
                     operandB=new NumericConstant(operandBstring, value);
                  } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandBstring+"' neither data nor numeric constant",task.getLineNumber());}
              } // end operandB==null               
              if (!(operandB instanceof NumericVariable || operandB instanceof NumericMap || operandB instanceof NumericConstant || operandB instanceof RegionDataset)) throw new ExecutionError("Operand '"+operandBstring+"' should be of numerical type",task.getLineNumber());            
              if (operandB2string!=null) {
                  operandB2=engine.getDataItem(operandB2string);
                  if (operandB2==null) {
                      if (operandB2string.contains("property \"")) operandB2=new RegionDataset("dummy"); // The new RegionDataset is just an indicator for later
                      else try {
                         double value=Double.parseDouble(operandB2string);
                         operandB2=new NumericConstant(operandB2string, (double)value);
                      } catch (Exception e) {throw new ExecutionError("Unrecognized token '"+operandB2string+"' neither data nor numeric constant",task.getLineNumber());}
                  } // end operandB2==null               
                  if (!(operandB2 instanceof NumericVariable || operandB2 instanceof NumericMap || operandB2 instanceof NumericConstant || operandB2 instanceof RegionDataset)) throw new ExecutionError("Operand '"+operandB2string+"' should be of numerical type",task.getLineNumber());
             }
        }
        setOperandAData(operandA);
        setOperandBData(operandB); 
        setOperandB2Data(operandB2);
        isResolved=true;
    }  

    
    
    /**
     * Returns true if this Condition holds at the specified region
     * @param sequencename
     * @param region The region that this condition should be evaluation for
     * @param task
     * @return
     * @throws java.lang.Exception
     */
    public boolean isConditionSatisfied(String sequencename, Region region, OperationTask task) throws Exception {
        if (!isResolved) {
            task.getEngine().reportError(new ExecutionError("SLOPPY PROGRAMMING ERROR: Condition_region is not 'resolved' before use"));
        }
        String compareProperty=getCompareProperty();
        if (compareProperty==null) return true; // no condition is set, so the transform should be applied everywhere
        Data operandA=getOperandAData(); // the first operand A (as in: A < B)
        Data operandB=getOperandBData();  // the second operand B (as in: A < B) 
        Data operandB2=getOperandB2Data(); // upper limit of the second operand if this is a range  (as in: A in range [B,B2])
        String comparator=getComparator();
        boolean whereNot=false;
        Boolean whereNotBoolean=negateAll();
        if (whereNotBoolean!=null) whereNot=whereNotBoolean.booleanValue();         
        boolean returnValue=true;
        String otherRegionTypeRestriction=getOtherRegionTypeRestriction();
        Class userDefinedPropertyType=getUserDefinedPropertyType();          
        if (compareProperty.equals("type") || userDefinedPropertyType==String.class || userDefinedPropertyType==Boolean.class) { // compare standard or user-defined String or Boolean values
              String propertyValue="";
              if (compareProperty.equals("type")) propertyValue=region.getType();
              else {
                  Object val=region.getProperty(compareProperty);
                  //if (val==null) return false; // if the region does not have this property. always return false! (even if 'not' operator is used in condition)
                  if (val==null) {
                      val=(userDefinedPropertyType==Boolean.class)?Boolean.FALSE:""; // default to boolean false or empty string if the regions does not have a defined value for the property
                  } // Old strategy: if the region does not have this property. always return false! (even if 'not' operator is used in condition)
                  propertyValue=val.toString(); 
              }
              if (operandB instanceof TextVariable) {
                  if (comparator.equals("equals") || comparator.equals("is in") || comparator.equals("in set")) returnValue=((TextVariable)operandB).contains(propertyValue);
                  else if (comparator.equals("matches") || comparator.equals("matches in")) returnValue=((TextVariable)operandB).matches(propertyValue);                          
              } else {
                  if (operandB instanceof DataCollection) returnValue=((DataCollection)operandB).contains(propertyValue); // collections cannot contain regexes, so "matches" does not really make sense in this case
                  else if (operandB instanceof TextMap) {
                      String mapValue=null;
                      if (operandB instanceof SequenceTextMap) mapValue=((SequenceTextMap)operandB).getValue(sequencename);
                      else mapValue=((TextMap)operandB).getValue(region.getType());
                      if (comparator.equals("equals")) returnValue=mapValue.equalsIgnoreCase(propertyValue);
                      else if (comparator.equals("is in") || comparator.equals("in set")) { // if the map value itself is a set (comma-separated list)
                          if (mapValue.contains(",")) {
                              String[] parts=mapValue.split(",");
                              boolean isMatch=false;
                              for (String part:parts) {
                                  if (part.equalsIgnoreCase(propertyValue)) {isMatch=true;break;}
                              }
                              returnValue=isMatch;
                          } else returnValue=mapValue.equalsIgnoreCase(propertyValue);
                      } else if (comparator.equals("matches")) {
                          returnValue=(propertyValue.matches(mapValue));
                      } else if (comparator.equals("matches in")) { // if the map value itself is a set (comma-separated list)
                          if (mapValue.contains(",")) {
                              String[] parts=mapValue.split(",");
                              boolean isMatch=false;
                              for (String part:parts) {
                                  if (propertyValue.matches(part)) {isMatch=true;break;}
                              }
                              returnValue=isMatch;
                          } else returnValue=propertyValue.matches(mapValue);                          
                      } else returnValue=false;
                  }                  
              }
        } else if (compareProperty.equals("score") || compareProperty.equals("length") || userDefinedPropertyType==Number.class) { // compare standard or user-defined Numeric values
              double valueA=0;
              double valueB=0;
              double valueB2=0;
              if (userDefinedPropertyType==Number.class) {
                  valueA=getNumericProperty(region,compareProperty); // non-numeric and undefined properties will return a value of 0
              }
              else if (compareProperty.equals("score")) valueA=region.getScore();
              else valueA=region.getLength(); // this is the only valid case left since we check for three cases in the if-statement above
                   if (operandB instanceof SequenceNumericMap) valueB=((SequenceNumericMap)operandB).getValue(sequencename);
              else if (operandB instanceof MotifNumericMap) valueB=((MotifNumericMap)operandB).getValue(region.getType());
              else if (operandB instanceof ModuleNumericMap) valueB=((ModuleNumericMap)operandB).getValue(region.getType());
              else if (operandB instanceof NumericVariable) valueB=((NumericVariable)operandB).getValue();
              else if (operandB instanceof NumericConstant) valueB=((NumericConstant)operandB).getValue();
              else if (operandB instanceof RegionDataset)   valueB=getNumericProperty(region, extractPropertyName(getOperandBString())); // the 'RegionDataset' here is just an indicator                   
              if (operandB2!=null) {
                       if (operandB2 instanceof SequenceNumericMap) valueB2=((SequenceNumericMap)operandB2).getValue(sequencename);
                  else if (operandB2 instanceof MotifNumericMap) valueB2=((MotifNumericMap)operandB2).getValue(region.getType());
                  else if (operandB2 instanceof ModuleNumericMap) valueB2=((ModuleNumericMap)operandB2).getValue(region.getType());
                  else if (operandB2 instanceof NumericVariable) valueB2=((NumericVariable)operandB2).getValue();
                  else if (operandB2 instanceof NumericConstant) valueB2=((NumericConstant)operandB2).getValue();
                  else if (operandB2 instanceof RegionDataset)   valueB2=getNumericProperty(region, extractPropertyName(getOperandB2String())); // the 'RegionDataset' here is just an indicator                    
              }
              returnValue=compareNumerical(valueA, valueB, valueB2, comparator);              
        } else if (compareProperty.equals("overlaps")) {
              RegionSequenceData otherSequence=(RegionSequenceData)((RegionDataset)operandB).getSequenceByName(sequencename); // the track we are comparing with
              String typeRestriction=(otherRegionTypeRestriction!=null)?region.getType():null;
              boolean exact=(otherRegionTypeRestriction!=null && !otherRegionTypeRestriction.equalsIgnoreCase("type-matching")); //
              returnValue=otherSequence.hasRegionsOverlappingGenomicInterval(region.getGenomicStart(), region.getGenomicEnd(), typeRestriction, exact);
        } else if (compareProperty.equals("inside")) {
              RegionSequenceData otherSequence=(RegionSequenceData)((RegionDataset)operandB).getSequenceByName(sequencename); // the track we are comparing with
              String typeRestriction=(otherRegionTypeRestriction!=null)?region.getType():null;
              boolean exact=(otherRegionTypeRestriction!=null && !otherRegionTypeRestriction.equalsIgnoreCase("type-matching")); //
              returnValue=otherSequence.hasRegionsSpanningGenomicInterval(region.getGenomicStart(), region.getGenomicEnd(), typeRestriction, exact);
        } else if (compareProperty.equals("covers")) {
              RegionSequenceData otherSequence=(RegionSequenceData)((RegionDataset)operandB).getSequenceByName(sequencename); // the track we are comparing with
              String typeRestriction=(otherRegionTypeRestriction!=null)?region.getType():null;
              boolean exact=(otherRegionTypeRestriction!=null && !otherRegionTypeRestriction.equalsIgnoreCase("type-matching")); //
              returnValue=otherSequence.hasRegionsWithinGenomicInterval(region.getGenomicStart(), region.getGenomicEnd(), typeRestriction, exact);
        } else if (compareProperty.equals("present in")) {
              RegionSequenceData otherSequence=(RegionSequenceData)((RegionDataset)operandB).getSequenceByName(sequencename); // the track we are comparing with
              returnValue=otherSequence.containsRegion(region);
        } else if (compareProperty.equals("similar in")) {
              RegionSequenceData otherSequence=(RegionSequenceData)((RegionDataset)operandB).getSequenceByName(sequencename); // the track we are comparing with
              returnValue=otherSequence.hasSimilarRegion(region,false);
        } else if (compareProperty.startsWith("distance to")) {
              double valueA=0; // this will hold the distance from the closest region to the target
              double valueB=0; // this is the second operand (the value to which the distance held in valueA is compared)
              double valueB2=0; // this is the upper bound if the second operand is a range
              RegionSequenceData otherSequence=null;
              String typeRestriction=(otherRegionTypeRestriction!=null)?region.getType():null;
              boolean exactType=(otherRegionTypeRestriction!=null && !otherRegionTypeRestriction.equalsIgnoreCase("type-matching")); //
              boolean restrictToInteractionPartners=false;
              if (getOperandAString().equals("interaction partner")) {
                  restrictToInteractionPartners=true;
                  otherSequence=region.getParent();
              } else {
                  otherSequence=(RegionSequenceData)((RegionDataset)operandA).getSequenceByName(sequencename);
              }        
                   if (operandB instanceof SequenceNumericMap) valueB=((SequenceNumericMap)operandB).getValue(sequencename);
              else if (operandB instanceof MotifNumericMap) valueB=((MotifNumericMap)operandB).getValue(region.getType());
              else if (operandB instanceof ModuleNumericMap) valueB=((ModuleNumericMap)operandB).getValue(region.getType());
              else if (operandB instanceof NumericVariable) valueB=((NumericVariable)operandB).getValue();
              else if (operandB instanceof NumericConstant) valueB=((NumericConstant)operandB).getValue();
              else if (operandB instanceof RegionDataset)   valueB=getNumericProperty(region, extractPropertyName(getOperandBString())); // the 'RegionDataset' here is just an indicator                   
              if (operandB2!=null) {
                     if (operandB2 instanceof SequenceNumericMap) valueB2=((SequenceNumericMap)operandB2).getValue(sequencename);
                else if (operandB2 instanceof MotifNumericMap) valueB2=((MotifNumericMap)operandB2).getValue(region.getType());
                else if (operandB2 instanceof ModuleNumericMap) valueB2=((ModuleNumericMap)operandB2).getValue(region.getType());
                else if (operandB2 instanceof NumericVariable) valueB2=((NumericVariable)operandB2).getValue();
                else if (operandB2 instanceof NumericConstant) valueB2=((NumericConstant)operandB2).getValue();
                else if (operandB2 instanceof RegionDataset)   valueB2=getNumericProperty(region, extractPropertyName(getOperandB2String())); // the 'RegionDataset' here is just an indicator                    
              }
              if (compareProperty.equals("distance to closest")) {
                  valueA=findShortestDistance(region,otherSequence,restrictToInteractionPartners,typeRestriction,exactType,task.getEngine());
                  returnValue=compareNumerical(valueA, valueB, valueB2, comparator);
              } else { // distance to any
                  int[] range=getApplicableDistanceRange((int)valueB, (int)valueB2, comparator);
                  boolean otherregions=otherRegionsWithinDistance(region, otherSequence, range, restrictToInteractionPartners, typeRestriction, exactType, task.getEngine());
                  boolean negate=range.length>2;
                  if (negate) returnValue=!otherregions; else returnValue=otherregions;
              }
        } else if (operandA!=null) { // compare numeric avg|min|max|median|etc...
              Double valueA=null;
              double valueB=0;
              double valueB2=0;
              NumericSequenceData otherSequence=(NumericSequenceData)((NumericDataset)operandA).getSequenceByName(sequencename); // the track we are comparing with
                   if (compareProperty.equals("startValue")) valueA=otherSequence.getValueAtRelativePosition(region.getRelativeStart());
              else if (compareProperty.equals("endValue")) valueA = otherSequence.getValueAtRelativePosition(region.getRelativeEnd());
              else if (compareProperty.equals("relativeStartValue")) valueA=otherSequence.getValueAtRelativePosition((otherSequence.getStrandOrientation()==Sequence.DIRECT)?region.getRelativeStart():region.getRelativeEnd());
              else if (compareProperty.equals("relativeEndValue")) valueA=otherSequence.getValueAtRelativePosition((otherSequence.getStrandOrientation()==Sequence.DIRECT)?region.getRelativeEnd():region.getRelativeStart());
              else if (compareProperty.equals("regionStartValue")) valueA=otherSequence.getValueAtRelativePosition((region.getOrientation()!=Region.REVERSE)?region.getRelativeStart():region.getRelativeEnd());
              else if (compareProperty.equals("regionEndValue")) valueA=otherSequence.getValueAtRelativePosition((region.getOrientation()!=Region.REVERSE)?region.getRelativeEnd():region.getRelativeStart());
              else if (compareProperty.equals("centerValue")) valueA=otherSequence.getValueAtRelativePosition((int)((region.getRelativeStart()+region.getRelativeEnd())/2.0));
              else if (compareProperty.equals("avg") || compareProperty.equals("average")) valueA=otherSequence.getAverageValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (compareProperty.equals("min") || compareProperty.equals("minimum")) valueA=otherSequence.getMinValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (compareProperty.equals("max") || compareProperty.equals("maximum")) valueA=otherSequence.getMaxValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (compareProperty.equals("sum")) valueA=otherSequence.getSumValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (compareProperty.equals("median")) valueA=otherSequence.getMedianValueInInterval(region.getRelativeStart(), region.getRelativeEnd());
              else if (compareProperty.equals("weighted avg") || compareProperty.equals("weighted average")) valueA=getWeightedValue(compareProperty,region,otherSequence,task.getEngine());
              else if (compareProperty.equals("weighted sum")) valueA=getWeightedValue(compareProperty,region,otherSequence,task.getEngine());
              else if (compareProperty.equals("weighted median")) valueA=getWeightedValue(compareProperty,region,otherSequence,task.getEngine());
              else if (compareProperty.equals("weighted min") || compareProperty.equals("weighted minimum")) valueA=getWeightedValue(compareProperty,region,otherSequence,task.getEngine());
              else if (compareProperty.equals("weighted max") || compareProperty.equals("weighted maximum")) valueA=getWeightedValue(compareProperty,region,otherSequence,task.getEngine());

                   if (operandB instanceof SequenceNumericMap) valueB=((SequenceNumericMap)operandB).getValue(sequencename);
              else if (operandB instanceof MotifNumericMap)  valueB=((MotifNumericMap)operandB).getValue(region.getType());
              else if (operandB instanceof ModuleNumericMap) valueB=((ModuleNumericMap)operandB).getValue(region.getType());
              else if (operandB instanceof NumericVariable)  valueB=((NumericVariable)operandB).getValue();
              else if (operandB instanceof NumericConstant)  valueB=((NumericConstant)operandB).getValue();
              else if (operandB instanceof RegionDataset)    valueB=getNumericProperty(region, extractPropertyName(getOperandBString())); // the 'RegionDataset' here is just an indicator
              if (operandB2!=null) {
                     if (operandB2 instanceof SequenceNumericMap) valueB2=((SequenceNumericMap)operandB2).getValue(sequencename);
                else if (operandB2 instanceof MotifNumericMap)  valueB2=((MotifNumericMap)operandB2).getValue(region.getType());
                else if (operandB2 instanceof ModuleNumericMap) valueB2=((ModuleNumericMap)operandB2).getValue(region.getType());
                else if (operandB2 instanceof NumericVariable)  valueB2=((NumericVariable)operandB2).getValue();
                else if (operandB2 instanceof NumericConstant)  valueB2=((NumericConstant)operandB2).getValue();
                else if (operandB2 instanceof RegionDataset)    valueB2=getNumericProperty(region, extractPropertyName(getOperandB2String())); // the 'RegionDataset' here is just an indicator                    
              }
              returnValue=(valueA==null)?false:compareNumerical(valueA, valueB, valueB2, comparator);
        }
                
        if (whereNot) return !returnValue;
        else return returnValue;
    } 

    private double getNumericProperty(Region region, String propertyName) {
        Object val=region.getProperty(propertyName);
        if (val instanceof Number) return ((Number)val).doubleValue();
        else return 0; // is this OK?  Treat NULL and non-numeric values as 0        
    }

    private String extractPropertyName(String propertyName) {
        int pos=propertyName.indexOf("property");
        if (pos>=0) {
            propertyName=propertyName.substring(pos+8); // "property".length() = 8
        }
        return MotifLabEngine.stripQuotes(propertyName.trim());   
    }    
    
    private double getWeightedValue(String compareProperty, Region region, NumericSequenceData numericSequence, MotifLabEngine engine) {
        String type=region.getType();
        int regionStart=region.getRelativeStart();
        int regionEnd=region.getRelativeEnd();
        Data motif=(type==null)?null:engine.getDataItem(type);
        if (motif instanceof Motif) {
           double[] weights=((Motif)motif).getICcontentForColumns(region.getOrientation()==Region.REVERSE, true);
           if (weights!=null) {
                    if (compareProperty.equals("weighted average") || compareProperty.equals("weighted avg")) return numericSequence.getWeightedAverageValueInInterval(regionStart, regionEnd, weights);
               else if (compareProperty.equals("weighted sum")) return numericSequence.getWeightedSumValueInInterval(regionStart, regionEnd, weights); 
               else if (compareProperty.equals("weighted min") || compareProperty.equals("weighted minimum")) return numericSequence.getWeightedMinValueInInterval(regionStart, regionEnd, weights);
               else if (compareProperty.equals("weighted max") || compareProperty.equals("weighted maximum")) return numericSequence.getWeightedMaxValueInInterval(regionStart, regionEnd, weights);
               else if (compareProperty.equals("weighted median")) return numericSequence.getWeightedMedianValueInInterval(regionStart, regionEnd, weights);
           }
        }
        // If we have not returned yet something wrong has happened. Use non-weighted defaults
             if (compareProperty.equals("weighted average") || compareProperty.equals("weighted avg")) return numericSequence.getAverageValueInInterval(regionStart, regionEnd);
        else if (compareProperty.equals("weighted sum")) return numericSequence.getSumValueInInterval(regionStart, regionEnd);     
        else if (compareProperty.equals("weighted min") || compareProperty.equals("weighted minimum")) return numericSequence.getMinValueInInterval(regionStart, regionEnd);
        else if (compareProperty.equals("weighted max") || compareProperty.equals("weighted maximum")) return numericSequence.getMaxValueInInterval(regionStart, regionEnd);
        else if (compareProperty.equals("weighted median")) return numericSequence.getMedianValueInInterval(regionStart, regionEnd);
        else return 0;
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

    /**
     * Returns a distance range based on a distance limit (or range) and a comparator
     * @param operand
     * @param comparator
     * @return distance range (consisting of 2 positive values) + an optional negation flag
     */
    private int[] getApplicableDistanceRange(int operand, int operand2, String comparator) {
            int[] range=null;
                 if (comparator.equals("="))  range=new int[]{operand,operand};
            else if (comparator.equals(">=")) range=new int[]{operand,Integer.MAX_VALUE};
            else if (comparator.equals(">"))  range=new int[]{operand+1,Integer.MAX_VALUE};
            else if (comparator.equals("<"))  range=new int[]{0,operand-1};
            else if (comparator.equals("<=")) range=new int[]{0,operand};
            else if (comparator.equals("<>")) range=new int[]{operand,operand,1}; // the 1 here is a flag for negation
            else if (comparator.equals("in")) range=new int[]{operand,operand2}; 
            return range;
    }

    
    /** Returns the distance between the given region (first argument) and the
     *  region in the (second argument) track that lays closest to the first region.
     *  (If the first region is a member of the second list it will be skipped!)
     */
    private int findShortestDistance(Region region, RegionSequenceData regionTrack, boolean mustbeinteractionpartner, String restrictType, boolean exactType, MotifLabEngine engine) {
        int distance=Integer.MAX_VALUE;
        int startRegion=region.getGenomicStart();
        int endRegion=region.getGenomicEnd();
        Motif motif=null;
        if (mustbeinteractionpartner) {
             Object data=engine.getDataItem(region.getType());
             if (data!=null && data instanceof Motif) motif=(Motif)data;
             else return distance; // this region is not a motif
        }
        for (Region other:regionTrack.getOriginalRegions()) { // these other regions are sorted by position (on direct strand)
               if (region.isIdenticalTo(other)) continue; // do not compare to same region (it is probably the same anyway...)
               if (mustbeinteractionpartner) {
                   if (!motif.isKnownInteractionPartner(other.getType())) continue; // this region is not an interaction partner
               }
               if (restrictType!=null) {
                   if (exactType && !restrictType.equals(other.getType())) continue;
                   if (!exactType && !(restrictType.contains(other.getType()) || other.getType().contains(restrictType))) continue;
               }
               int startOther=other.getGenomicStart();
               int endOther=other.getGenomicEnd();
               //System.err.print("compare region ["+startRegion+"-"+endRegion+"] to other ["+startOther+"-"+endOther+"] => ");
               if (endOther<startRegion) { // other region lies before
                   int newdist=startRegion-endOther-1;
                   //System.err.println("other before    olddist="+distance+"  newdist="+newdist);
                   if (newdist<distance) distance=newdist; // update shortest distance as long as new distances keep getting smaller
               } else if (endRegion<startOther) { // other region lies after
                   int newdist=startOther-endRegion-1;
                   //System.err.println("other after     olddist="+distance+"  newdist="+newdist);
                   if (newdist<distance) distance=newdist; // update shortest distance as long as new distances keep getting smaller
                   else return distance; // if they are not getting any smaller they will keep getting larger, so just return the smallest seen so far
               } else { // region overlaps (return -1)
                   //System.err.println("overlap");
                   return -1;
               }
        }
        return distance;
    }


    /** Returns TRUE if there exists any other region (or interaction partner) within a given distance range (relative distances applied to both sides)
     *  from the target region.
     *  @param mustbeinteractionpartner If TRUE only known interaction partners for the target region motif is considered eligible
     *  @param restrictType if non-null The other regions must be of a type which matches the type in this String
     *  @param exactType if a restrictType String is given and exactType==true, the type of the region must be identical to the restrictType in order to be eligible
     *               if exactType==false, it is enough that the region type is contained within the restrictType string
     */
    private boolean otherRegionsWithinDistance(Region region, RegionSequenceData regionTrack, int[] range, boolean mustbeinteractionpartner, String restrictType, boolean exactType, MotifLabEngine engine) {
        int startRegion=region.getGenomicStart();
        int endRegion=region.getGenomicEnd();
        int minRange=range[0];
        int maxRange=range[1];
        Motif motif=null;
        if (mustbeinteractionpartner) {
             Object data=engine.getDataItem(region.getType());
             if (data!=null && data instanceof Motif) motif=(Motif)data;
             else return false; // this region is not a motif
        }
        int startRange=(maxRange==Integer.MAX_VALUE)?regionTrack.getRegionStart():startRegion-(maxRange+1);
        int endRange=(maxRange==Integer.MAX_VALUE)?regionTrack.getRegionEnd():endRegion+maxRange+1;
        if (startRange<regionTrack.getRegionStart() || startRange>regionTrack.getRegionEnd()) startRange=regionTrack.getRegionStart();
        if (endRange>regionTrack.getRegionEnd() || endRange<regionTrack.getRegionStart()) endRange=regionTrack.getRegionEnd(); // endRange<0 indicates overflow error
        ArrayList<Region> otherRegions=regionTrack.getRegionsOverlappingGenomicInterval(startRange, endRange);
        // System.err.println(regionTrack.getName()+"   Region:["+startRegion+","+endRegion+"]    interval:["+startRange+","+endRange+"]   regions="+otherRegions.size());
        for (Region other:otherRegions) { // these other regions are sorted by position (on direct strand)
               if (region.isIdenticalTo(other)) continue; // do not compare to same region (it is probably the same anyway...)
               if (mustbeinteractionpartner) {
                   if (!motif.isKnownInteractionPartner(other.getType())) continue; // this region is not an interaction partner
               }
               if (restrictType!=null) {
                   if (exactType && !restrictType.equals(other.getType())) continue;
                   if (!exactType && !(restrictType.contains(other.getType()) || other.getType().contains(restrictType))) continue;
               }
               int startOther=other.getGenomicStart();
               int endOther=other.getGenomicEnd();
               if (endOther<startRegion) { // other region lies before
                   int newdist=startRegion-endOther-1;
                   if (newdist>=minRange && newdist<=maxRange) return true;
               } else if (endRegion<startOther) { // other region lies after
                   int newdist=startOther-endRegion-1;
                  if (newdist>=minRange && newdist<=maxRange) return true;
               } else { // region overlaps with other
                   if (minRange<0) return true;
               }
        }
        return false;
    }

    
    @SuppressWarnings("unchecked")  
    @Override
    public Condition_region clone() {
        Condition_region newdata=new Condition_region((HashMap<String, Object>)storage.clone());
        return newdata;
    }     
    
    @Override
    public void importCondition(Condition other) throws ClassCastException {
        if (other==null) throw new ClassCastException("Unable to import from NULL condition");
        if (!other.getClass().equals(this.getClass())) throw new ClassCastException("Unable to import from condition of different class. Expected '"+this.getClass()+"' but got '"+other.getClass()+"'.");
        isResolved=((Condition_region)other).isResolved;
        storage=(HashMap<String, Object>)((Condition_region)other).storage.clone();
    }      
}
