/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.ParseError;

/**
 * This class represents gene expression profiles for a set of sequences.
 * The expression profile consists of a number of conditions (numbered from 0 to N-1)
 * and each sequence has an expression value (double) corresponding to that condition.
 * The data structure is stored as a HashMap where the sequence name is the key.
 * The value from this HashMap is an ArrayList of Doubles which stores the values
 * of each of the N conditions.
 * @author kjetikl
 */
public class ExpressionProfile extends Data {
    private String name;
    private HashMap<String,ArrayList<Double>> profiles=new HashMap<String,ArrayList<Double>>(); // key is sequencename, value is list of condition-values
    private HashMap<Integer,String> headers=null; // optional headers for the conditions (instead of just referring to them by index numbers)
    private static String typedescription="Expression Profile";

    /**
     * Creates a new ExpressionProfile with 0 conditions
     * @param name
     */
    public ExpressionProfile(String name) {
        this.name=name;
        profiles=new HashMap<String,ArrayList<Double>>();
    }

    /**
     * Creates a new ExpressionProfile with 0 conditions based on the list of sequence names provided
     * @param name
     */
    public ExpressionProfile(String name, ArrayList<String> sequenceNames) {
        this.name=name;
        profiles=new HashMap<String,ArrayList<Double>>(sequenceNames.size());
        for (String sequenceName:sequenceNames) {
            profiles.put(sequenceName, new ArrayList<Double>());
        }
    }
    /**
     * Creates new ExpressionProfile based on the given values. The values parameter
     * should contain expressions for all known sequences and the number of recorded
     * conditions should be the same for all sequences (no checks are performed here)
     * @param name
     * @param values
     */
    public ExpressionProfile(String name, HashMap<String,ArrayList<Double>> values) {
        this.name=name;
        this.profiles=values;       
    }

    /**
     * Creates new ExpressionProfile based on the given values. The values parameter
     * should contain expressions for all known sequences and the number of recorded
     * conditions should be the same for all sequences (no checks are performed here)
     * @param name
     * @param values
     * @param conditionHeaders
     */
    public ExpressionProfile(String name, HashMap<String,ArrayList<Double>> values, HashMap<Integer,String> conditionHeaders) {
        this.name=name;
        this.profiles=values;
        this.headers=conditionHeaders;
    }

    /**
     * Creates a new ExpressionProfile which is based on a subset of columns from an existing profile given as input
     * @param name
     * @param original
     * @param columns A list of which columns to include in the new profile (zero-based)
     */
    public ExpressionProfile(String name, ExpressionProfile original, ArrayList<Integer> columns) throws ExecutionError {
        this.name=name;
        Set<String> keys=original.profiles.keySet();
        profiles=new HashMap<String,ArrayList<Double>>(keys.size());
        int numColumns=columns.size();
        for (String sequenceName:keys) {
            ArrayList<Double> newvalues=new ArrayList<Double>(numColumns);
            for (int i=0;i<numColumns;i++) {
                int oldcolumn=columns.get(i);
                Double value=original.getValue(sequenceName, oldcolumn);
                newvalues.add(value);                
            }           
            profiles.put(sequenceName, newvalues);
        }        
        // set the headers
        if (original.headers!=null) {
            for (int i=0;i<numColumns;i++) {
                this.setHeader(i, original.getHeader(columns.get(i)));
            }
        }
    }    
    
    @Override
    public String getName() {return name;}

    @Override
    public void rename(String newname) {
        this.name=newname;
    }

    /**
     * Returns the number of conditions recorded in this expression profile
     * @return
     */
    public int getNumberOfConditions() {
        if (profiles==null || profiles.isEmpty()) return 0;
        else {
            Collection<ArrayList<Double>> list=profiles.values();
            for (ArrayList<Double> el:list) {
                return el.size(); // short-cuts after returning first size
            }
        }
        return 0;
    }

    @Override
    public Object getValue() {return this;}

    /** Returns a header to use for the condition with the given index 
      * If no header name is explicitly set, the string will be based on the
      * condition index (plus 1) (i.e. conditionNumber=0 will result in "1" 
      * if not other header is set).  
      *  @param conditionNumber 
      */
    public String getHeader(int conditionNumber) {
        if (headers==null) return ""+(conditionNumber+1);
        String header=headers.get(conditionNumber);
        if (header!=null) return header;
        else return ""+(conditionNumber+1);
    }
    
    /**
     * Sets a header to use for the condition with the given index
     * @param conditionNumber
     * @param headerString
     * @trows ExecutionError If the given header name is already in use by a different condition column (or headername is empty)
     * 
     */
    public final void setHeader(int conditionNumber, String headername) throws ExecutionError {
        if (headers==null) headers=new HashMap<Integer, String>();
        if (headername==null || headername.trim().isEmpty()) throw new ExecutionError("Missing header name");
        if (headers.containsValue(headername)) throw new ExecutionError("Header "+headername+" is already in use");
        headers.put(conditionNumber,headername);
    }   
    
     /**
     * Sets headers for conditions 
     * @param newHeaders The values must all be distinct (this is not checked)
     */   
    public void setHeaders(HashMap<Integer,String> newHeaders) {
        headers=newHeaders;
    }       

    /**
     * Returns the internal condition index for the condition with the given name (or external index)
     * @param variablename
     * @return the internal condition index or -1 if no corresponding condition exists
     */
    public int getConditionIndex(String variablename) {
        if (headers!=null && headers.containsValue(variablename)) {
            for (int i:headers.keySet()) {
                String header=headers.get(i);
                if (header!=null && header.equals(variablename)) return i;
            }
        }
        try {
           int conditionIndex=Integer.parseInt(variablename);
           conditionIndex--; // external indices are 1-based but internal are 0-based
           int numExp=getNumberOfConditions();
           if (conditionIndex<0 || conditionIndex>=numExp) return -1;
           return conditionIndex;
        } catch (NumberFormatException e) {return -1;}
    }    
    
    @Override
    public String getValueAsParameterString() {
        StringBuilder string=new StringBuilder();
        if (headers!=null) {
            for (Integer index:headers.keySet()) {
                string.append("header[");
                string.append(index+1); // +1 because external indices are 1-based while internal are 0-based
                string.append("]=");
                string.append(headers.get(index));
                string.append(";");
            }
        }
        for (String seq:profiles.keySet()) {
            ArrayList<Double> seqval=profiles.get(seq);
            if (seqval!=null && !seqval.isEmpty()) {
                string.append(seq);
                string.append("=");
                for (int i=0;i<seqval.size()-1;i++) {
                    string.append(seqval.get(i));
                    string.append(",");
                }
                string.append(seqval.get(seqval.size()-1));
                string.append(";");
            }
        }      
        return string.toString();
    }

    /** Returns an array containing the minimum and maximum values found in this dataset
     *  Or NULL if no conditions have been recorded
     */
    public double[] getValuesRange() {
        if (getNumberOfConditions()==0) return null;
        double min=Double.MAX_VALUE;
        double max=-Double.MAX_VALUE;
        for (String seq:profiles.keySet()) {
            ArrayList<Double> seqval=profiles.get(seq);
            if (seqval!=null && !seqval.isEmpty()) {
                for (int i=0;i<seqval.size();i++) {
                    double value=seqval.get(i);
                    if (value>max) max=value;
                    if (value<min) min=value;
                }
            }
        }
        return new double[]{min,max};
    }


    @Override
    public String output() {
        StringBuilder string=new StringBuilder();
        for (String seq:profiles.keySet()) {
            ArrayList<Double> seqval=profiles.get(seq);
            if (seqval!=null && !seqval.isEmpty()) {
                string.append(seq);
                string.append("=");
                for (int i=0;i<seqval.size()-1;i++) {
                    string.append(seqval.get(i));
                    string.append(",");
                }
                string.append(seqval.get(seqval.size()-1));
                string.append("\n");
            }
        }
        return string.toString();
    }

    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        clear();     
        HashMap<String,ArrayList<Double>> newprofile=new HashMap<String,ArrayList<Double>>();
        for (String line:input) {
            line=line.trim();
            int expected=-1;
            if (line.startsWith("#") || line.isEmpty()) continue;
            String[] lineSplit=line.split("\\s*=\\s*");
            if (lineSplit.length!=2) throw new ParseError("Expected line with format: SequenceName = comma-separated list of values. Found: "+line);
            String sequenceName=lineSplit[0].trim();
            if (!engine.dataExists(sequenceName, Sequence.class)) continue;
            String[] elements=lineSplit[1].split("\\s*,\\s*");
            if (expected<0) expected=elements.length;
            else if (elements.length!=expected) throw new ParseError("Expected "+expected+" value(s) for sequence '"+sequenceName+"' but found "+elements.length);                 
            ArrayList<Double> values=new ArrayList<Double>(elements.length);
            for (String element:elements) {
                try {
                   double value=Double.parseDouble(element);
                   values.add(value);
                } catch (NumberFormatException e) {
                    throw new ParseError("Unable to parse expected numeric value: "+element);
                }          
            }
            newprofile.put(sequenceName, values);
        } // end: for each input line
        setValue(newprofile);
    } // end: inputFromPlain
    
    /**
     * Sets an expression value for the given sequence and conditionNumber
     * Note that the condition with the given index must already exists for this to work.
     *
     * @param sequencename
     * @param conditionNumber
     * @param expressionvalue
     * @return TRUE if the expression value was properly updated or false if no condition with the given number exists
     */
    public boolean setValue(String sequencename, int conditionNumber, double expressionvalue) {
         ArrayList<Double> seqval=profiles.get(sequencename);
         if (seqval==null || seqval.isEmpty() || conditionNumber<0 || conditionNumber>=seqval.size()) return false;
         seqval.set(conditionNumber, expressionvalue);
         return true;
    }

    /** Adds a sequence with the given name to this Expression profile (if it does not already exist). Condition records will be set to 0 */
    public void addSequence(String sequencename) {
        if (profiles!=null && profiles.containsKey(sequencename)) return; // already present
        if (profiles==null) profiles=new HashMap<String, ArrayList<Double>>();
        int exp=getNumberOfConditions();
        ArrayList<Double> values=new ArrayList<Double>();
        for (int i=0;i<exp;i++) values.add(new Double(0));
        profiles.put(sequencename, values);
    }
    
    /** Removes the sequence with the given name from this Expression profile */
    public void removeSequence(String sequencename) {
        if (profiles==null || (profiles!=null && !profiles.containsKey(sequencename))) return; // no such sequence
        profiles.remove(sequencename);
    }    


    /** Sets the expression data for this object based on the provided values.
     * The values parameter should contain expressions for all known sequences and the number of recorded
     * conditions should be the same for all sequences (no checks are performed here)
     * @param sequencename
     */
    public void setValue(HashMap<String,ArrayList<Double>> profiles) {
         this.profiles=profiles;
    }



    /** Removes all condition records (but keeps the sequences) */
    public void clearCondition() {
         for (String seq:profiles.keySet()) {
            ArrayList<Double> seqval=profiles.get(seq);
            seqval.clear();
         }
         headers=null;
    }

    /** Removes all condition records and sequence names */
    public void clear() {
         profiles.clear();
         headers=null;
    }

    /** Adds a new condition at the end of the expression records for all sequences (with initial expression values = 0) */
    public void addCondition() {
         for (String seq:profiles.keySet()) {
            ArrayList<Double> seqval=profiles.get(seq);
            seqval.add(new Double(0));
         }
    }

    /**
     * Returns the expression value for the given sequence and conditionNumber
     * or NULL if the required condition (or sequence) has no records
     */
    public Double getValue(String sequencename, int conditionNumber) {
        ArrayList<Double> seqval=profiles.get(sequencename);
        if (seqval==null || seqval.isEmpty() || conditionNumber<0 || conditionNumber>=seqval.size()) return null;
        else return seqval.get(conditionNumber);
    }

    
    /** Returns the maximum value over all conditions for the given sequence
     *  or NaN if no values are found for this sequence
     */
    public double getMaxValue(String sequencename) {
        ArrayList<Double> list=getExpressionProfileForSequence(sequencename);
        if (list==null || list.isEmpty()) return Double.NaN;
        double max=-Double.MAX_VALUE;
        for (Double val:list) {
            if (val!=null && val>max) max=val;
        }
        return max;
    }
    
    /** Returns the minimum value over all conditions for the given sequence
     *  or NaN if no values are found for this sequence
     */
    public double getMinValue(String sequencename) {
        ArrayList<Double> list=getExpressionProfileForSequence(sequencename);
        if (list==null || list.isEmpty()) return Double.NaN;
        double min=Double.MAX_VALUE;
        for (Double val:list) {
            if (val!=null && val<min) min=val;
        }
        return min;
    }  
    
    /** Returns the maximum value across all sequences and conditions
      * or NaN if the ExpressionProfile contains no values
      */
    public double getMaxValue() {
        if (profiles==null || profiles.isEmpty() || getNumberOfConditions()==0) return Double.NaN;
        double max=-Double.MAX_VALUE;
        for (String seqname:profiles.keySet()) {
            double seqmax=getMaxValue(seqname);
            if (seqmax>max) max=seqmax;
        }
        return max;
    }
    
    /** Returns the minimum value across all sequences and conditions
      * or NaN if the ExpressionProfile contains no values
      */
    public double getMinValue() {
        if (profiles==null || profiles.isEmpty() || getNumberOfConditions()==0) return Double.NaN;
        double min=Double.MAX_VALUE;
        for (String seqname:profiles.keySet()) {
            double seqmin=getMinValue(seqname);
            if (seqmin<min) min=seqmin;
        }
        return min;
    }    
    
    /**
     * Returns the expressions values for all conditions for the sequence with the given name
     * Note that the returned object is not a clone
     */
    public ArrayList<Double>getExpressionProfileForSequence(String sequencename) {
        if (profiles==null) return null;
        else return profiles.get(sequencename);
    }

    /** Returns the full data structure for this Expression profile.
     *  Note that the original structure is returned, not a copy!
     */
    public HashMap<String,ArrayList<Double>> getValues() {return profiles;}

    @Override
    public void importData(Data source) throws ClassCastException {
        ExpressionProfile datasource=(ExpressionProfile)source;
        this.name=datasource.name;
        this.profiles=datasource.profiles;
        this.headers=datasource.headers;
        //notifyListenersOfDataUpdate();
    }

    /**
     * Returns true if this ExpressionProfile equals the other given ExpressionProfile
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof ExpressionProfile)) return false;
        ExpressionProfile otherprofile=(ExpressionProfile)other;
        if (profiles==null && otherprofile.profiles==null) return true;
        if (profiles!=null && otherprofile.profiles==null) return false;
        if (profiles==null && otherprofile.profiles!=null) return false;
        if (headers!=null && otherprofile.headers==null) return false;
        if (headers==null && otherprofile.headers!=null) return false;        
        for (String key:profiles.keySet()) {
            ArrayList<Double> thisseqval=profiles.get(key);
            ArrayList<Double> otherseqval=otherprofile.profiles.get(key);
            if (otherseqval==null || otherseqval.size()!=thisseqval.size()) return false;
            else {
                for (int i=0;i<thisseqval.size();i++) {
                    if (!thisseqval.get(i).equals(otherseqval.get(i))) return false;
                }
            }
        }
        if (headers!=null && otherprofile.headers!=null && !headers.equals(otherprofile.headers)) return false;     
        return true;
    }


    @Override
    @SuppressWarnings("unchecked")
    public ExpressionProfile clone() {
        HashMap<String,ArrayList<Double>> newvalues=null;
        if (profiles!=null) {
            newvalues=new HashMap<String,ArrayList<Double>>();
            for (String key:profiles.keySet()) {
                ArrayList<Double> seqval=profiles.get(key);
                ArrayList<Double> newseqval=new ArrayList<Double>(seqval.size());
                for (Double val:seqval) newseqval.add(val);
                newvalues.put(key, newseqval);
            }
        }      
        ExpressionProfile newdata=new ExpressionProfile(name, newvalues);
        if (headers!=null) newdata.headers=(HashMap<Integer,String>)headers.clone();
        else newdata.headers=null;
        return newdata;
    }

    @Override
    public String[] getResultVariables() {
        int numExp=getNumberOfConditions();
        String[] result=new String[numExp+2];
        result[0]="column names";
        for (int i=0;i<numExp;i++) result[i+1]="column:"+getHeader(i);
        result[result.length-1]="subprofile:1-"+numExp;
        return result;
    }

    @Override
    public boolean hasResult(String variablename) {
        if (variablename.equals("column names") || variablename.startsWith("subprofile:")) return true;
        else if (variablename.startsWith("column:")) variablename=variablename.substring("column:".length());
        int expIndex=getConditionIndex(variablename);
        return (expIndex>=0);
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename.equals("column names")) {
            ArrayList<String> values=new ArrayList<String>();
            int size=getNumberOfConditions();
            for (int i=0;i<size;i++) values.add(getHeader(i));
            return new TextVariable("temp",values);
        }        
        if (variablename.startsWith("subprofile:")) {
            ArrayList<Integer> columns=parseValueColumns(variablename.substring("subprofile:".length()));
            return new ExpressionProfile("temp", this, columns);
        }
        if (variablename.startsWith("column:")) variablename=variablename.substring("column:".length());               
        int conditionIndex=getConditionIndex(variablename);
        if (conditionIndex<0) throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");   
        SequenceNumericMap map=new SequenceNumericMap("temp",0);
        for (String seq:profiles.keySet()) {
           ArrayList<Double> list=profiles.get(seq);
           double value=list.get(conditionIndex); 
           map.setValue(seq, value);
        }
        return map;  
    }

    @Override
    public Class getResultType(String variablename) {
       if (variablename.equals("column names")) return TextVariable.class;
       if (variablename.startsWith("subprofile:")) return ExpressionProfile.class;
       if (!hasResult(variablename)) return null;
       else return SequenceNumericMap.class; // all exported values are SequenceNumericMap (columns)
    }

    /** Parses the "value columns" parameter for the "subprofile" result type. The indices returned are zero-indexes (original index minus one) */
    private ArrayList<Integer> parseValueColumns(String parameter) throws ExecutionError {
        if (parameter==null || parameter.trim().isEmpty()) return null;
        ArrayList<Integer> list=new ArrayList<Integer>();
        String[] parts=parameter.trim().split("\\s*,\\s*");
        for (String part:parts) {
            if (part.indexOf('-')>=0 || part.indexOf(':')>=0) { // range
               String[] range=part.split("-|:"); // split on - or :
               if (range.length==1) throw new ExecutionError("Column indices must be greater than or equal to 1: "+part);
               int start=getConditionIndex(range[0]);
               int end=getConditionIndex(range[1]);
               if (start<0) throw new ExecutionError("Not a valid start column: "+range[0]);
               if (end<0) throw new ExecutionError("Not a valid end column: "+range[1]);
               if (start>end) { // reverse order
                   for (int i=start;i>=end;i--) {
                       list.add(new Integer(i));  
                   }
               } else { // normal order
                   for (int i=start;i<=end;i++) {
                       list.add(new Integer(i));  
                   }
               }
            } else {        
               int column=getConditionIndex(part);                 
               if (column<0) throw new ExecutionError("Not a valid column: "+part);
               list.add(new Integer(column));  
            }
        }
        return list;
    }    
    
    

    public static String getType() {return typedescription;}
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }
    
    @Override
    public String getTypeDescription() {return typedescription+" : "+getNumberOfConditions()+" conditions";}

    public void debug() {
        System.err.println("ExpressionProfile["+getName()+"]:  #sequences="+profiles.size()+"   #conditions="+getNumberOfConditions());
        System.err.println(output());
    }

    public static ExpressionProfile createExpressionProfileFromParameterString(String name, String parameterString, MotifLabEngine engine) throws ExecutionError {
        if (parameterString==null || parameterString.trim().isEmpty()) return new ExpressionProfile(name);
        if (parameterString.startsWith(Operation_new.FROM_MAP_PREFIX)) {
            parameterString=parameterString.substring(Operation_new.FROM_MAP_PREFIX.length());
            String[] mapNames=parameterString.split("\\s*,\\s*");
            ArrayList<SequenceNumericMap> maps=new ArrayList<>(mapNames.length);
            HashMap<Integer,String> expheaders=new HashMap<Integer,String>();
            int i=0;
            for (String mapName:mapNames) {
                Object map=engine.getDataItem(mapName);
                if (map==null) throw new ExecutionError("Unknown data object: "+mapName);
                if (!(map instanceof SequenceNumericMap)) throw new ExecutionError("Data object '"+mapName+"' is not a Sequence Numeric Map");
                if (maps.contains((SequenceNumericMap)map)) throw new ExecutionError("The map '"+mapName+"' can not be added twice");
                maps.add((SequenceNumericMap)map); 
                expheaders.put(new Integer(i), mapName);
                i++;
            }
            ArrayList<String> sequenceNames=engine.getDefaultSequenceCollection().getAllSequenceNames();
            HashMap<String,ArrayList<Double>> profile=new HashMap<>();
            for (String sequence:sequenceNames) {
               ArrayList<Double> values=new ArrayList<>(maps.size());
               for (SequenceNumericMap map:maps) {
                   values.add(map.getValue(sequence));                   
               }
               profile.put(sequence, values);
            }           
            ExpressionProfile expProfile=new ExpressionProfile(name, profile, expheaders);
            return expProfile;            
        } else { // "manual entry" format       
            String[] seqs=parameterString.split("\\s*;\\s*");
            int expectedRecords=-1;
            HashMap<String,ArrayList<Double>> profile=new HashMap<String,ArrayList<Double>>(seqs.length);
            HashMap<Integer,String> expheaders=null;
            for (String seq:seqs) {
                 String[] nameExpressionSplit=seq.split("\\s*=\\s*");
                 if (nameExpressionSplit.length==1) throw new ExecutionError("Unable to parse argument for Expression Profile");
                 String sequenceName=nameExpressionSplit[0];
                 if (sequenceName.startsWith("header[") && sequenceName.endsWith("]")) { // this is a header entry
                     String conditionIndex=sequenceName.substring("header[".length(),sequenceName.length()-1);
                     if (conditionIndex.trim().isEmpty()) throw new ExecutionError("Missing condition index for header statement: "+seq);
                     try {
                        int expIndex=Integer.parseInt(conditionIndex);
                        expIndex--; // external indices are 1-based but internal are 0-based
                        if (expIndex<0) throw new ExecutionError("Condition index must be greater than 0, found '"+conditionIndex+"'");
                        String expHeader=nameExpressionSplit[1];
                        if (expHeader.trim().isEmpty()) throw new ExecutionError("Missing header for condition '"+conditionIndex+"'");
                        if (expHeader.equals("sequence")) throw new ExecutionError("Illegal condition header '"+expHeader+"'");
                        if (expheaders==null) expheaders=new HashMap<Integer,String>();
                        expheaders.put(expIndex,expHeader);
                     } catch (NumberFormatException e) {
                         throw new ExecutionError("Unable to parse expected numeric value for condition index '"+conditionIndex+"'");
                     }
                     continue;
                 }
                 // entry refers to a regular sequence
                 Data dataitem=engine.getDataItem(sequenceName);
                 if (dataitem==null) throw new ExecutionError("No sequence exists with the name '"+sequenceName+"'");
                 else if (!(dataitem instanceof Sequence)) throw new ExecutionError("Data object '"+sequenceName+"' is not a Sequence");
                 String[] recordsSplit=nameExpressionSplit[1].split("\\s*,\\s*");
                 if (expectedRecords<0) expectedRecords=recordsSplit.length;
                 else if (expectedRecords!=recordsSplit.length) throw new ExecutionError("Sequence '"+sequenceName+"' does not have the expected number of condition entries (expected "+expectedRecords+" found "+recordsSplit.length+")");
                 ArrayList<Double> seqprofile=new ArrayList<Double>(expectedRecords);
                 for (String record:recordsSplit) {
                     try {
                         double value=Double.parseDouble(record);
                         seqprofile.add(new Double(value));
                     } catch (NumberFormatException e) {
                         throw new ExecutionError("Unable to parse expected numeric entry for sequence '"+sequenceName+"': "+record);
                     }
                 }
                 profile.put(sequenceName,seqprofile);
            }
            // check that all the sequences are present and add default values (0) for those that are missing            
            if (expectedRecords<=0 && expheaders!=null) { // no sequences have been added => determine number of columns from headers
                int index=-1;
                for (Integer num:expheaders.keySet()) {
                    if (num>index) index=num;
                }
                index++; // since indexes are 0-based
                expectedRecords=index;
            }
            if (expectedRecords>0) {
                for (String seqName:engine.getNamesForAllDataItemsOfType(Sequence.class)) {
                    if (!profile.containsKey(seqName)) {
                       ArrayList<Double> seqprofile=new ArrayList<Double>(expectedRecords);
                       for (int i=0;i<expectedRecords;i++) seqprofile.add(new Double(0));
                       profile.put(seqName,seqprofile);
                    }
                }
            }
            return new ExpressionProfile(name, profile, expheaders);
        }
    }


    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }

}
