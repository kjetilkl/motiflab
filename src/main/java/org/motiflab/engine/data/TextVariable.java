/*
 
 
 */

package org.motiflab.engine.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.protocol.ParseError;

/**
 * TextVariable is a general-purpose String-based variable which can either
 * be interpreted as a free-text document or as a collection of independent Strings
 * corresponding do the lines in the document. 
 * The data itself is stored as an ordered list of Strings.
 * 
 * @author kjetikl
 */
public class TextVariable extends Data {
    private static String typedescription="Text Variable"; 
    protected String datasetName;
    protected ArrayList<String> storage; //
    
    /**
     * Constructs a new initially "empty" TextVariable with the given name
     * 
     * @param datasetName A name for this dataset
     */
   public TextVariable(String datasetName) {
       this.datasetName=datasetName;
       storage=new ArrayList<String>(20);
   }

    /**
     * Constructs a new TextVariable with the given name initialized with a single String value
     * @param datasetName A name for this dataset
     */
   public TextVariable(String datasetName, String value) {
       this.datasetName=datasetName;
       ArrayList<String> list=new ArrayList<String>(1);     
       list.add(value);
       setValue(list); // this will ensure that TABs are unescaped properly
   }   
   
    /**
     * Constructs a new TextVariable with the given name initialized with the given set of Strings
     * @param datasetName A name for this dataset
     * @param values A list of string which will be the contents of this TextVariable 
     */
   public TextVariable(String datasetName, ArrayList<String> values) {
       this.datasetName=datasetName;
       setValue(values);
   }
   
    /**
     * Constructs a new TextVariable with the given name initialized with the given set of Strings
     * @param datasetName A name for this dataset
     * @param values A list of string which will be the contents of this TextVariable     * 
     */
   public TextVariable(String datasetName, String[] values) {
       this.datasetName=datasetName;
       setValue(values);
   }
   
    /**
     * Specifies a new name for this TextVariable
     * @param name the name for this TextVariable
     */
    public void setName(String name) {
        this.datasetName=name;
    }
    
    @Override
    public void rename(String name) {
        setName(name);
    }   
    
   /**
    * Returns the name of this TextVariable
    * @return name
    */
    @Override
    public String getName() {
        return datasetName;
    }
    
    @Override
    public Object getValue() {return this;} // should maybe change later
    
    /**
     * Returns the value of this TextVariable as a single string.
     * If the variable has multiple text lines these will be separated
     * with the provided separator string, but the lines will not be escaped further
     * (and lines could contain the separator themselves)
     * @param separator
     * @return 
     */
    public String getValueAsString(String separator) {
        return MotifLabEngine.splice(storage, separator);
    }    
  
    /**
     * Returns the first non-empty line found in this Text Variable
     * or null if no such lines exists
     * @return 
     */
    public String getFirstValue() {
        for (String string:storage) {
            if (!string.trim().isEmpty()) return string.trim();
        }
        return null;
    }
    
    
    @Override
    public String getValueAsParameterString() {
        StringBuilder string=new StringBuilder();
        for (int i=0;i<storage.size();i++) {
            String line=storage.get(i);
            line=MotifLabEngine.escapeQuotedString(line); // escape special characters such as quotes, tabs, newlines and backslashes          
            if (i>0) string.append(",");
            string.append("\"");
            string.append(line);
            string.append("\"");
        }
        return string.toString();
    } 
      
    /** Replaces the contents of this TextVariable with the given list of Strings */
    public void setValue(ArrayList<String> newvalue) {
        storage=newvalue;
        for (int i=0;i<storage.size();i++) { // check for escaped tabs
            String line=storage.get(i);
            if (line.contains("\\")) storage.set(i, MotifLabEngine.unescapeQuotedString(line));
        }
        notifyListenersOfDataUpdate();
    } 
    
    /** Replaces the contents of this TextVariable with the given list of Strings */
    public void setValue(String[] newvalue) {
        storage.clear();
        for (String value:newvalue) {
            if (value.contains("\\")) value=MotifLabEngine.unescapeQuotedString(value);
            storage.add(value);
        }
        notifyListenersOfDataUpdate();
    } 
    
    /** Replaces the contents of this TextVariable with the given single String */
    public void setValue(String newvalue) {
        storage.clear();
        if (newvalue.contains("\\")) newvalue=MotifLabEngine.unescapeQuotedString(newvalue);
        storage.add(newvalue);     
        notifyListenersOfDataUpdate();
    }     
    
    
    /**
     * Returns all the Strings (lines) in this TextVariable
     * 
     * @return A list of String names
     */
    public ArrayList<String> getAllStrings() {
        return storage;
    }    
    
    public String getString(int index) {
        if (storage==null || index>=storage.size()) return "";
        return storage.get(index);
    }      
           

    /** 
     * Returns true if the given string equals any string (line) in this TextVariable (case-sensitive)
     */
    public boolean contains(String string) {
        for (String stored:storage) {
            if (stored.equals(string)) return true;
        }
        return false;
    }
    
    /** 
     * Returns true if the given string is found within any string (line) in this TextVariable
     */
    public boolean containsSubstring(String string, boolean casesensitive) {
        if (!casesensitive) string=string.toLowerCase();
        for (String stored:storage) {
            if ((casesensitive && stored.contains(string)) || (!casesensitive && stored.toLowerCase().contains(string))) return true;
        }
        return false;
    }    
    
    /** 
     * Returns true if the given string matches any string (line) in this TextVariable
     * @string a string to compare against the strings/lines in this TextVariable.
     *         Note that the argument string should NOT be a regex but this TextVariable
     *         can contain regexes!
     */
    public boolean matches(String string) {
        for (String stored:storage) { // the stored strings can be regexes!
            stored="(?i)"+stored; // case insensitive match 
            if (string.matches(stored)) return true;
        }
        return false;
    }    
    
    /**
     * Returns true if this TextVariable has the exact same contents as the argument 
     * Note that the strings/lines must be in the same order also.
     * @param data
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof TextVariable)) return false;
        TextVariable collection=(TextVariable)data;
        if (size()!=collection.size()) return false;
        for (int i=0;i<storage.size();i++) {
            String thisline=storage.get(i);
            String otherline=collection.storage.get(i);
            if (!thisline.equals(otherline)) return false;
        }
        return true;
    }
    
    /**
     * Returns true if this TextVariable represents the same collection
     * of strings as the other TextVariable. I.e. they must have the same
     * content but the lines do not have to appear in the same order
     * (which is a requirement for the method containsSameData(other))
     * @param data
     * @return
     */
    public boolean containsSameEntries(TextVariable data) {
        if (data==null || !(data instanceof TextVariable)) return false;
        TextVariable collection=(TextVariable)data;
        if (size()!=collection.size()) return false;
        for (int i=0;i<storage.size();i++) {
            String thisline=storage.get(i);
            if (!collection.contains(thisline)) return false;
        }
        return true;
    }    
       
    /** Returns TRUE if the contents of this TextVariable when treated as a set is a subset of the contents in the other TextVariable (or the same) */
    public boolean isSubsetOf(TextVariable other) {
        if (!this.getClass().equals(other.getClass())) return false;
        if (size()>other.size()) return false; // this set is larger so it cannot be a subset!
        for (String name:storage) {
            if (!other.contains(name)) return false;
        }
        return true;       
    }
    
    /** Returns TRUE if the contents of this TextVariable when treated as a set is proper subset of the contents in the other TextVariable
     *  i.e. all the entries in this TextVariable are also present in the other TextVariable 
     *  but the other TextVariable also contains additional entries not in this TextVariable
     */
    public boolean isProperSubsetOf(TextVariable other) {
        return (isSubsetOf(other) && other.size()>this.size());
    }    
    
    /** Returns TRUE if the contents of this TextVariable when treated as a set is a superset of the contents in the other TextVariable (or the same) */
    public boolean isSupersetOf(TextVariable other) {
        return other.isSubsetOf(this);
    }  
    
    /** Returns TRUE if the contents of this TextVariable when treated as a set is proper superset of the contents in the other TextVariable
     *  i.e. all the entries in the other TextVariable are also in this TextVariable 
     *  but the this TextVariable also contains additional entries not in the other TextVariable
     */
    public boolean isProperSupersetOf(TextVariable other) {
        return other.isProperSubsetOf(this);
    }       
    
    
  /** Appends a new line to the end of this Text Variable
    * @param line The String to be appended
    * @return TRUE if the line was added
    */
    public boolean append(String line) {
        return storage.add(line); // add to local storage
        //notifyListenersOfDataAddition(string);
    }
 
  /** Appends a new line to the beginning of this Text Variable
    * @param line The String to be appended
    */    
    public void appendFirst(String line) {
        storage.add(0,line); // add to local storage
        //notifyListenersOfDataAddition(string);
    }    
    
   /** Appends additional lines to the end of this Text Variable
     * @param lines The Strings to be appended
     * @return TRUE if the lines were added
     */
    public boolean append(ArrayList<String> lines) {
        return storage.addAll(lines); // add to local storage
    }    
    
    /** Removes all lines matching the target line from this Text Variable */
    public boolean removeAll(String target) {
       boolean removed=false;
        Iterator<String> iterator=storage.iterator();
        while (iterator.hasNext()) {
            String line=iterator.next();
            if (line.equals(target)) {iterator.remove();removed=true;}
        }  
        return removed;
    }
    
   /**
     * Removes all Strings from this dataset
     */
    public void clearAll() {
        storage.clear();
    }
    
    
    /**
     * Returns the number of Strings (lines) present in this TextVariable
     * 
     * @return number of strings
     */
    public int getNumberofStrings() {
        return storage.size();
    }
    
    /**
     * Returns the number of Strings (lines) in this TextVariable (same as getNumberofStrings)
     * 
     * @return number of Strings
     */
    public int size() {
        return storage.size();
    }
    
    

    
    /**
     * This method takes all Strings in the specified TextVariable and adds them
     * to this one unless they are already present.
     * @param dataset The TextVariable to be merged into this TextVariable
     */
    public void merge(TextVariable other) {
        int size=other.size();
        for (int i=0;i<size;i++) {
            String line=other.storage.get(i);
            if (!contains(line)) storage.add(line); // I use this directly instead of calling append to limit the number of notifications sent 
        }              
        notifyListenersOfDataAddition(other);
    }
    
    @Override
    public void importData(Data source) throws ClassCastException {
        TextVariable datasource=(TextVariable)source;
        this.datasetName=datasource.datasetName;
        storage.clear();
        for (String string:datasource.storage) {
            storage.add(string);
        }
        //notifyListenersOfDataUpdate(); 
    }
    
    @Override
    public TextVariable clone() {
        TextVariable newcollection= new TextVariable(datasetName);
        int size=size();
        for (int i=0;i<size;i++) {
            String string=storage.get(i);
            newcollection.storage.add(string); // I use this directly instead of calling append() to limit the number of notifications sent 
        }  
        return newcollection;
    }
       
    

    public static String getType() {return typedescription;}
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription+" : "+storage.size()+" line"+((storage.size()!=1)?"s":"");}

    @Override
    public String output() {
        StringBuilder string=new StringBuilder();
        for (int i=0;i<storage.size();i++) {
            String line=storage.get(i);
            string.append(MotifLabEngine.escapeQuotedString(line)); // replace TABs with \t and other stuff
            string.append("\n");
        }
        return string.toString();
    }
    
    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        String[] result=new String[input.size()];
        setValue(input.toArray(result));
    }
    
    
    
   @Override
    public String[] getResultVariables() {
        return new String[]{"size","unique","duplicates","transpose","union:<Text Variable>","append:<Text Variable>","subtract:<Text Variable>","intersect:<Text Variable>","xor:<Text Variable>","reverse","sorted","columns:<list>","lines containing:<text>","lines not containing:<text>","lines matching:<regex>","lines not matching:<regex>"}; //
    }

    @Override
    public boolean hasResult(String variablename) {
        if (variablename.startsWith("columns:")) return true;
        else if (variablename.startsWith("lines matching:") || variablename.startsWith("lines not matching:")) return true;
        else if (variablename.startsWith("lines containing:") || variablename.startsWith("lines not containing:")) return true;
        else if (variablename.startsWith("union:") || variablename.startsWith("append:") || variablename.startsWith("xor:")) return true;
        else if (variablename.startsWith("subtract:") || variablename.startsWith("intersect:")) return true;
        else return super.hasResult(variablename); // this will check if the variable is in the list returned by getResultVariables()
    }    
    
    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
         if (variablename.equals("size")) {
             return new NumericVariable("temp",storage.size());
         } else if (variablename.equals("sorted") || variablename.equals("sort")) {
             ArrayList<String> lines=new ArrayList<String>();
             if (!storage.isEmpty()) {
                 lines=(ArrayList)storage.clone();
                 Collections.sort(lines, MotifLabEngine.getNaturalSortOrderComparator(true));
             }
             TextVariable result = new TextVariable("temp", lines);
             return result;
         } else if (variablename.equals("reverse") || variablename.equals("reversed")) {
             ArrayList<String> lines=new ArrayList<String>();
             if (!storage.isEmpty()) {
                 for (int i=storage.size()-1;i>=0;i--) {
                     lines.add(storage.get(i));
                 }
             }
             TextVariable result = new TextVariable("temp", lines);
             return result;
         } else if (variablename.equals("transpose") || variablename.equals("transposed")) {
             ArrayList<String> lines=new ArrayList<String>();
             ArrayList<String[]> table=new ArrayList<String[]>();
             int newcolumns=storage.size(); // old rows
             int newrows=0;
             if (!storage.isEmpty()) {
                 for (String line:storage) {
                     if (line.endsWith("\t")) line=line.substring(0,line.length()-1);
                     String[] parts=line.split("\t",-1);
                     table.add(parts);
                     if (parts.length>newrows) newrows=parts.length;
                 }
                 for (int i=0;i<newrows;i++) {       
                     StringBuilder builder=new StringBuilder();
                     for (int j=0;j<newcolumns;j++) {
                        if (j>0) builder.append("\t");
                        String[] column=table.get(j);
                        if (i<column.length) builder.append(column[i]);
                     }
                     lines.add(builder.toString());
                 }                             
             }
             TextVariable result = new TextVariable("temp", lines);
             return result;
         } else if (variablename.startsWith("append:")) {
             int pos=variablename.indexOf(":")+1;
             String dataname=(pos>=variablename.length())?"":variablename.substring(pos);             
             TextVariable other=getOtherTextVariable(dataname);
             TextVariable result = new TextVariable("temp", (ArrayList<String>)storage.clone());           
             for (String line:other.getAllStrings()) {
                 result.append(line);
             }                                       
             return result;
         } else if (variablename.startsWith("union:")) {
             int pos=variablename.indexOf(":")+1;
             String dataname=(pos>=variablename.length())?"":variablename.substring(pos);             
             TextVariable other=getOtherTextVariable(dataname);
             TextVariable result = new TextVariable("temp", (ArrayList<String>)storage.clone());           
             for (String line:other.getAllStrings()) {
                 if (!result.contains(line)) result.append(line);
             }                                       
             return result;
         } else if (variablename.startsWith("subtract:")) {
             int pos=variablename.indexOf(":")+1;
             String dataname=(pos>=variablename.length())?"":variablename.substring(pos);             
             TextVariable other=getOtherTextVariable(dataname);
             TextVariable result = new TextVariable("temp", (ArrayList<String>)storage.clone());           
             for (String line:other.getAllStrings()) {
                 result.removeAll(line);
             }                                       
             return result;
         } else if (variablename.startsWith("intersect:") || variablename.startsWith("intersection:")) {
             int pos=variablename.indexOf(":")+1;
             String dataname=(pos>=variablename.length())?"":variablename.substring(pos);             
             TextVariable other=getOtherTextVariable(dataname);
             ArrayList<String> newstorage=(ArrayList<String>)storage.clone();
             Iterator<String> iterator=newstorage.iterator();
             while (iterator.hasNext()) {
                 String line=iterator.next();
                 if (!other.contains(line)) iterator.remove();
             }
             TextVariable result = new TextVariable("temp", newstorage);                                     
             return result;
         } else if (variablename.startsWith("xor:")) {
             int pos=variablename.indexOf(":")+1;
             String dataname=(pos>=variablename.length())?"":variablename.substring(pos);             
             TextVariable other=getOtherTextVariable(dataname);
             ArrayList<String> newstorage=(ArrayList<String>)storage.clone();
             HashSet<String> toRemove=new HashSet<String>();
             ArrayList<String> toAdd=new ArrayList<String>();
             for (String line:other.getAllStrings()) {
                 if (newstorage.contains(line)) toRemove.add(line);
                 else toAdd.add(line);
             }
             Iterator<String> iterator=newstorage.iterator();
             while (iterator.hasNext()) {
                 String line=iterator.next();
                 if (toRemove.contains(line)) iterator.remove();
             }             
             newstorage.addAll(toAdd);
             TextVariable result = new TextVariable("temp", newstorage);                                     
             return result;
         } else if (variablename.equals("unique")) {             
             ArrayList<String> newstorage=new ArrayList<String>(storage.size());
             HashSet<String> encountered=new HashSet<String>();
             for (String line:storage) {
                 if (!encountered.contains(line)) {
                     newstorage.add(line);
                     encountered.add(line);
                 }
             }
             TextVariable result = new TextVariable("temp", newstorage);                                     
             return result;
         } else if (variablename.equals("duplicate") || variablename.equals("duplicates")) {             
             ArrayList<String> newstorage=new ArrayList<String>(storage.size());
             HashSet<String> encountered=new HashSet<String>();
             for (String line:storage) {
                 if (encountered.contains(line) && !newstorage.contains(line)) { // only add duplicates once
                     newstorage.add(line);
                 }
                 encountered.add(line);                 
             }
             TextVariable result = new TextVariable("temp", newstorage);                                     
             return result;
         } else if (variablename.startsWith("lines matching:") || variablename.startsWith("lines not matching:") ) {
             boolean invert=variablename.startsWith("lines not matching:");
             int pos=variablename.indexOf(":")+1;
             if (pos>=variablename.length()) throw new ExecutionError("Missing search expression for row extraction");
             String expression=variablename.substring(pos);
             ArrayList<String> matching=getRowsMatching(expression, invert);
             TextVariable result = new TextVariable("temp", matching);
             return result;
         } else if (variablename.startsWith("lines containing:") || variablename.startsWith("lines not containing:") ) {
             boolean invert=variablename.startsWith("lines not containing:");
             int pos=variablename.indexOf(":")+1;
             if (pos>=variablename.length()) throw new ExecutionError("Missing search expression for row extraction");
             String expression=variablename.substring(pos);
             ArrayList<String> matching=getRowsContaining(expression, invert);
             TextVariable result = new TextVariable("temp", matching);
             return result;
         } else if (variablename.startsWith("columns:")) {
             int pos=variablename.indexOf(":")+1;
             if (pos>=variablename.length()) throw new ExecutionError("Missing specification of which columns to extract");  
             String expression=variablename.substring(pos);
             if (expression.equals("<list>")) throw new ExecutionError("Missing specification of which columns to extract. You should replace <list> with a comma-separated list of column indices.");
             ArrayList<Object> list=parseColumnsSpecification(expression);
             ArrayList<String> newlines=new ArrayList<>();
             if (storage!=null && !storage.isEmpty()) {
                  for (String line:storage) {
                      String[] columns=line.split("\t");
                      StringBuilder newline=new StringBuilder();
                      for (Object c:list) {
                          if (c instanceof String) { // insert new column
                              newline.append((String)c);
                              newline.append("\t");
                          } else if (c instanceof Integer) {
                              int col=((Integer)c); //
                              if (col<0) col=columns.length+col; // col is negative!
                              if (col<0 || col>=columns.length) continue;
                              if (col<columns.length) {
                                  newline.append(columns[col]);
                                  newline.append("\t");                                  
                              }
                          } else if (c instanceof int[]) {
                              int start=((int[])c)[0];
                              int end=((int[])c)[1];
                              if (start<0) start=columns.length+start; // start is negative!
                              if (end<0) end=columns.length+end;
                              if (start<0 || end<0 || start>=columns.length || end>=columns.length) continue; // "index out of bounds". Just skip this
                              if (start>end) {// reverse order
                                  for (int i=start;i>=end;i--) {newline.append(columns[i]);newline.append("\t");}
                              } else { // normal order
                                  for (int i=start;i<=end;i++) {newline.append(columns[i]);newline.append("\t");} 
                              }
                          }
                      }
                      if (newline.length()>0 && newline.charAt(newline.length()-1)=='\t') newline.deleteCharAt(newline.length()-1); // remove the last tab
                      newlines.add(newline.toString());
                  }   
             }             
             TextVariable result = new TextVariable("temp", newlines);
             return result;
         } 
         throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    private TextVariable getOtherTextVariable(String name) throws ExecutionError {
        if (name.isEmpty() || name.equals("<Text Variable>")) throw new ExecutionError("Missing name of other Text Variable");
        Data data=MotifLabEngine.getEngine().getDataItem(name);
        if (data==null) throw new ExecutionError("Unknown data item: "+name);
        if (!(data instanceof TextVariable)) throw new ExecutionError("'"+name+"' is not a Text Variable");
        return (TextVariable)data;
    }
    
    @Override
    public Class getResultType(String variablename) {
       if (variablename.equals("size")) return NumericVariable.class;
       else return TextVariable.class; // all other exported values are manipulations of the text itself
    }    

    private ArrayList<String> getRowsMatching(String regex, boolean invert) {
        ArrayList<String> result=new ArrayList<>();
        if (storage==null || storage.isEmpty()) return result;
        for (String line:storage) {
            boolean isMatch=line.matches(regex);
            if ((isMatch && !invert) || (!isMatch && invert)) result.add(line);
        }
        return result;
    }
    private ArrayList<String> getRowsContaining(String text, boolean invert) {
        ArrayList<String> result=new ArrayList<>();
        if (storage==null || storage.isEmpty()) return result;
        for (String line:storage) {
            boolean isMatch=line.contains(text);
            if ((isMatch && !invert) || (!isMatch && invert)) result.add(line);
        }
        return result;
    }
    
    /** Parses the "column:" expression  */
    private ArrayList<Object> parseColumnsSpecification(String parameter) throws ExecutionError {
        if (parameter==null || parameter.trim().isEmpty()) return null;
        ArrayList<Object> list=new ArrayList<Object>();
        String[] parts=parameter.trim().split("\\s*,\\s*");
        for (String part:parts) {
            if (part.startsWith("'") && part.endsWith("'")) { // explicit string
                list.add(part.substring(1,part.length()-1)); // strip quotes
            }
            else if (part.indexOf(':')>=0) { // column range
               int start=1;
               int end=0;             
               String[] range=part.split(":");
               if (range.length!=2) throw new ExecutionError("Unable to parse column range: "+part);              
               start=parseColumnNumber(range[0]);
               end=parseColumnNumber(range[1]);                            
               list.add(new int[]{start,end}); // positive indices now start from the beginning and negative from the end
            } else { // single column. This could be a column index or a piece of text to insert in a new column      
                 int column=parseColumnNumber(part);
                 list.add(new Integer(column));
            }
        }
        return list;
    }  
    
    private int parseColumnNumber(String string) throws ExecutionError {
       int column=0;
       if (string.equals("end")) column=0;
       else if (string.startsWith("end-")) column=Integer.parseInt(string.substring("end".length())); // include minus sign
       else {
           try {column=Integer.parseInt(string);} catch (NumberFormatException e) {throw new ExecutionError("Column indices must be positive numbers (starting at 1). Found: "+string);}
           if (column<1) throw new ExecutionError("Column indices must be positive numbers (starting at 1). Found: "+string);
       }        
       return column-1;
    }
       
    
    
    public static TextVariable createTextVariableFromParameterString(String targetName, String text, MotifLabEngine engine) throws ExecutionError {
        if (text.toLowerCase().startsWith("columns:")) { // create table with columns from other Text Variables
            text=text.substring("columns:".length()+1);
            if (text.isEmpty()) throw new ExecutionError("Empty list of columns for Text Variable");
            String[] names=text.trim().split("\\s*,\\s*");
            TextVariable[] inputs=new TextVariable[names.length];
            int i=0; int size=0;
            for (String name:names) {
                Data data=engine.getDataItem(name);
                if (data==null) throw new ExecutionError("Unknown data object: "+name);
                if (!(data instanceof TextVariable)) throw new ExecutionError("'"+name+"' is not a Text Variable");
                inputs[i]=(TextVariable)data;
                if (inputs[i].size()>size) size=inputs[i].size();
                i++;
            }
            ArrayList<String> lines=new ArrayList<String>();
            for (int lineNumber=0;lineNumber<size;lineNumber++) {
                StringBuilder builder=new StringBuilder();
                for (int j=0;j<inputs.length;j++) {
                    if (j>0) builder.append("\t");
                    builder.append(inputs[j].getString(lineNumber));
                }
                lines.add(builder.toString());
            }
            return new TextVariable(targetName,lines);           
        } else { // regular list of strings       
            ArrayList<String> values=parseTextVariableParameters(text);
            return new TextVariable(targetName,values);       
        }
    }
    
    private static ArrayList<String> parseTextVariableParameters(String text) throws ExecutionError {
        if (text.trim().isEmpty()) return new ArrayList<String>(0);
        try {
            ArrayList<String> list=MotifLabEngine.splitQuotedStringListOnComma(text);
            return list;
        } catch (ParseError e) {
            throw new ExecutionError("Text Variable error:\n"+e.getMessage());
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