/*
 
 
 */

package motiflab.engine.protocol;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Condition_basic;
import motiflab.engine.operations.Condition_basic_boolean;
import motiflab.engine.operations.Condition_region;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Condition_boolean;
import motiflab.engine.operations.Condition_position_boolean;
import motiflab.engine.operations.Condition_region_boolean;
import motiflab.engine.operations.Condition_within;

/**
 *
 * @author kjetikl
 */
public abstract class StandardOperationParser extends OperationParser {
    
    public String getCommandString_condition(Condition condition) {
        if (condition==null) return "";
             if (condition instanceof Condition_boolean) return getCommandString_BooleanCondition((Condition_boolean)condition,null).toString();
        else if (condition instanceof Condition_position) return getCommandString_condition((Condition_position)condition);
        else if (condition instanceof Condition_region) return getCommandString_condition((Condition_region)condition);
        else if (condition instanceof Condition_within) return getCommandString_condition((Condition_within)condition);
        else if (condition instanceof Condition_basic) return getCommandString_condition((Condition_basic)condition);        
        else return "<unknown condition type="+condition.getClass().toString()+">";
    }

    private StringBuilder getCommandString_BooleanCondition(Condition_boolean condition, StringBuilder builder) { //
        if (builder==null) builder=new StringBuilder();
        ArrayList<? extends Condition> list=condition.getConditions();
        int type=condition.getOperatorType();
        if (list!=null && !list.isEmpty()) {
            for (int i=0;i<list.size();i++) {
                Condition cond=list.get(i);
                if (cond instanceof Condition_boolean) {
                   builder.append("(");
                   getCommandString_BooleanCondition((Condition_boolean)cond,builder);
                   builder.append(")");
                } else { // normal condition
                   builder.append(getCommandString_condition(cond));
                }
                if (i<list.size()-1) builder.append((type==Condition_boolean.AND)?" and ":" or ");
            }
        }
        return builder;
    }


//    private StringBuilder getCommandString_BooleanConditionRegion(Condition_region_boolean condition, StringBuilder builder) { //
//        if (builder==null) builder=new StringBuilder();
//        ArrayList<Condition_region> list=condition.getConditions();
//        int type=condition.getOperatorType();
//        if (list!=null && !list.isEmpty()) {
//            for (int i=0;i<list.size();i++) {
//                Condition cond=list.get(i);
//                if (cond instanceof Condition_region_boolean) {
//                   builder.append("(");
//                   getCommandString_BooleanConditionRegion((Condition_region_boolean)cond,builder);
//                   builder.append(")");
//                } else { // normal condition
//                   builder.append(getCommandString_condition(cond));
//                }
//                if (i<list.size()-1) builder.append((type==Condition_boolean.AND)?" and ":" or ");
//            }
//        }
//        return builder;
//    }

    private String getCommandString_condition(Condition_position condition) { // 
        String operandAstring=condition.getOperandAString();
        if (operandAstring==null) return "";
        boolean whereNot=condition.negateAll();
        String comparator=condition.getComparator();  
        String operandBstring=condition.getOperandBString();
        String operandB2string=condition.getOperandB2String();
        String notString=(whereNot) ? "not " : "";
        String msg=notString;
        if (comparator!=null && comparator.equals("inside")) msg+="inside "+operandAstring;
        else if (comparator!=null && (comparator.equals("is uppercase") || comparator.equals("is lowercase"))) msg+=operandAstring+" "+comparator;        
        else if (comparator!=null && comparator.equals("in")) msg+=operandAstring+" "+comparator+" "+operandBstring+" to "+operandB2string;
        else msg+=operandAstring+" "+comparator+" "+operandBstring;
        return msg;
    }
    
    private String getCommandString_condition(Condition_region condition) { // 
        String compareProperty=condition.getCompareProperty();
        if (compareProperty==null) return "";
        boolean whereNot=condition.negateAll();
        String comparator=condition.getComparator();  
        String operandAstring=condition.getOperandAString();               
        String operandBstring=condition.getOperandBString();
        String typeRestrict=condition.getOtherRegionTypeRestriction();
        Class userPropertyType=condition.getUserDefinedPropertyType();
        String notString=(whereNot) ? "not " : "";
        String msg=notString;
             if (compareProperty.equals("type")) msg+="region's type "+comparator+" "+operandBstring;
        else if (userPropertyType==String.class) msg+="region's text property \""+compareProperty+"\" "+comparator+" "+operandBstring;
        else if (userPropertyType==Boolean.class) msg+="region's boolean property \""+compareProperty+"\" "+comparator+" "+operandBstring;
        else if (userPropertyType==Number.class) msg+="region's numeric property \""+compareProperty+"\" "+comparator+" "+operandBstring;
        else if (compareProperty.equals("score") || compareProperty.equals("length")) msg+="region's "+compareProperty+" "+comparator+" "+operandBstring;        
        else if (compareProperty.equals("overlaps")) msg+="region overlaps "+((typeRestrict!=null)?typeRestrict+" ":"")+operandBstring;
        else if (compareProperty.equals("inside")) msg+="region inside "+((typeRestrict!=null)?typeRestrict+" ":"")+operandBstring;
        else if (compareProperty.equals("covers")) msg+="region covers "+((typeRestrict!=null)?typeRestrict+" ":"")+operandBstring;
        else if (compareProperty.equals("present in")) msg+="region present in "+operandBstring;
        else if (compareProperty.equals("similar in")) msg+="region similar in "+operandBstring;
        else if (compareProperty.startsWith("distance to")) msg+="region's "+compareProperty+" "+((typeRestrict!=null)?typeRestrict+" ":"")+operandAstring+" "+comparator+" "+operandBstring;
        else if (operandAstring!=null) msg+="region's "+compareProperty+" "+operandAstring+" "+comparator+" "+operandBstring;
        if (comparator!=null && comparator.equals("in")) msg+=" to "+condition.getOperandB2String();
        return msg;
    }    
    
    private String getCommandString_condition(Condition_within condition) { // 
        String operandAstring=condition.getOperandAString();
        if (operandAstring==null) return "";
        //boolean whereNot=condition.negateAll();
        String msg=" within ["+operandAstring+"]";
        return msg;
    }    
    
    private String getCommandString_condition(Condition_basic condition) { // 
        boolean whereNot=condition.negateAll();
        String comparator=condition.getComparator();  
        String operandAstring=condition.getOperandAString();               
        String operandBstring=condition.getOperandBString();
        String notString=(whereNot) ? "not " : "";
        String msg=notString;
        msg+=operandAstring+" "+comparator+" "+operandBstring;
        if (comparator!=null && comparator.equals("in")) msg+=" to "+condition.getOperandB2String();
        return msg;
    }     
    
    /**
     * Parses a compound condition and returns a list of objects that can either be strings or other lists
     * This method should return a list with an odd number of entries, where every odd entry is either a single string
     * which corresponds to a single condition (e.g. single where-clause) or a new nested list which corresponds to a new nested clause.
     * Every even entry is the name of a boolean operator which connects the clauses, i.e. either "AND" or "OR"
     * @param text
     * @return
     */
    private ArrayList<Object> parseCompoundCondition(String text) throws ParseError {
        if (text.startsWith("(") && text.endsWith(")")) text=stripExcessiveParentheses(text);
        int i=0;
        int lastSegmentStart=0;
        boolean lastPosWasBoundary=false;
        ArrayList<Object> segments=new ArrayList<Object>();
        while (i<text.length()) {
            char current=text.charAt(i);
            if (current=='(') { // move forward to closing parenthesis
                i=findClosingParenthesis(text,i+1);
                if (i<0) throw new ParseError("Unclosed parenthesis");
                lastPosWasBoundary=true;
            } else if (current=='\"') {
                i=findClosingQuote(text,i+1);// move forward to matching closing quote
                if (i<0) throw new ParseError("Unclosed quote");
            } else if ((current=='a' || current=='A' || current=='o' || current=='O') && lastPosWasBoundary) { // this could be the start of an 'AND' or 'OR'
               if (matchesOperatorAtPos(text,"and",i) || matchesOperatorAtPos(text,"or",i)) {
                    // add previous segment -> check if it is within parenthesis?
                    String segment=text.substring(lastSegmentStart,i-1).trim();
                    if (segment.startsWith("(") && segment.endsWith(")")) {
                       ArrayList<Object> list2=parseCompoundCondition(segment.substring(1,segment.length()-1));
                       if (list2.size()==1) segments.add(list2.get(0)); // it is just one segment so lift it up
                       else segments.add(list2); // add nested compound
                    } else segments.add(segment);
                   // add operator
                   if (current=='a' || current=='A') {i+=2;segments.add("AND");}
                   if (current=='o' || current=='O') {i+=1;segments.add("OR");}
                   lastSegmentStart=i+1;
               } 
               lastPosWasBoundary=false;
            } else if (current==' ') {
               lastPosWasBoundary=true;
            } else { // just a normal letter
               lastPosWasBoundary=false;
            }
            i++;
        }
        // Add final segment
        String segment=text.substring(lastSegmentStart,text.length()).trim();
        if (segment.startsWith("(") && segment.endsWith(")")) {
           ArrayList<Object> list2=parseCompoundCondition(segment.substring(1,segment.length()-1));
           if (list2.size()==1) segments.add(list2.get(0)); // it is just one segment so lift it up
           else segments.add(list2); // add nested compound
        } else segments.add(segment);
        return segments;
    }

    /** Returns true if the text has a possible boolean operator at the given position (followed by space or parenthesis) */
    private boolean matchesOperatorAtPos(String text, String target, int pos) {
        if (target.equals("and")) {
            if (pos+4>text.length()) return false; // not room
            String sub=text.substring(pos, pos+4);
            return (sub.equalsIgnoreCase("and ") || sub.equals("and("));
        } else if (target.equals("or")) {
             if (pos+3>text.length()) return false; // not room
            String sub=text.substring(pos, pos+3);
            return (sub.equalsIgnoreCase("or ") || sub.equals("or("));
        } else return false;
    }

    /** Returns the position of the closing parenthesis */
    private int findClosingParenthesis(String text, int pos) {
        int level=0;
        int i=pos;
        while (i<text.length()) {
            char current=text.charAt(i);
            if (current=='\"') {
               int close=findClosingQuote(text, i+1);
               if (close<0) return -1;
               else i=close;
            } else if (current=='(') {// another nesting
                level++;
            } else if (current==')') {
               if (level==0) return i;
               else level--;
            }
            i++;
        }
        return -1; // no matching parenthesis found
    }

    private int findClosingQuote(String text, int pos) {
        int i=pos;
        while (i<text.length()) {
            char current=text.charAt(i);
            if (current=='\"') {
                if (text.charAt(i-1)!='\\') return i; // check first if the quote was escaped: \"
            }
            i++;
        }
        return -1; // no closing quote found
    }
    
    /** Removes enclosing parentheses if they span the whole text */
    private String stripExcessiveParentheses(String text) {
        text=text.trim();
        if (!text.startsWith("(")) return text;
        int closepos=findClosingParenthesis(text,1);
        if (closepos==text.length()-1) { // the parenthesis in the first position matches one in the last position
            text=text.substring(1,text.length()-1).trim();
            if (text.startsWith("(") && text.endsWith(")")) return stripExcessiveParentheses(text); // see if there are more
        }
        return text;
    }

    /** Takes as input a tree where each odd element is the name of an operator (AND/OR) and each even element
     *  is either a single condition or a new compound condition. There can be a mix of operators on the same level
     *  e.g. "X and Y or Z" . This method restructures the tree so that the elements on each level are all connected
     *  by the same operator. I.e. if the original level contained both AND and OR, that level is divided into two new
     *  levels, first by the OR operator (which has lower precedence) and then by the AND operator. In the new tree the
     *  first element on each level is the name of the operator (AND/OR) and the subsequent elements are either single
     *  conditions or compounds (new trees).
     */
    private void restructureConditionTree(ArrayList<Object> tree) throws ParseError {
        // first process the levels recursively
        for (int i=0;i<tree.size();i+=2) { // only process even nodes because the odd ones are operators
            Object element=tree.get(i);
            if (element instanceof ArrayList) restructureConditionTree((ArrayList<Object>)element);
        }
        // now check if this level has mixed operators
        String operator=(String)tree.get(1);
        for (int i=3;i<tree.size();i+=2) { // only process odd nodes
            String element=(String)tree.get(i);
            if (!element.equals(operator)) {operator="MIXED";break;}
        }
        if (operator.equals("MIXED")) { // restructure this node on two levels
            ArrayList<Object> newlevel=new ArrayList<Object>(tree.size());
            newlevel.add("OR");    
            int start=0;
            int end=posOfNextOr(tree, start);
            while (end>0) { // there should be at least one OR
                if (end-1==start) { // just a single element between ORs
                   newlevel.add(tree.get(start)); 
                } else {
                   ArrayList<Object> sublevel=new ArrayList<Object>((end-start)/2+1);
                   sublevel.add("AND");
                   for (int j=start;j<end;j+=2) sublevel.add(tree.get(j));
                   newlevel.add(sublevel);
                }
                start=end+1;
                end=posOfNextOr(tree, start);                
            }
            // add last bit
            if (start==tree.size()-1) { // just one element left
               newlevel.add(tree.get(start));  
            } else {
               ArrayList<Object> sublevel=new ArrayList<Object>((tree.size()-start)/2+1);
               sublevel.add("AND");
               for (int j=start;j<tree.size();j+=2) sublevel.add(tree.get(j));
               newlevel.add(sublevel);               
            }
            tree.clear();
            tree.addAll(newlevel);
        } else { // restructure on same level by removing duplicate operators
            // remove operator nodes (old odd)
            for (int i=tree.size()-2;i>0;i-=2) { // only process odd nodes
                tree.remove(i);
            }          
            // add operator to front of list
            tree.add(0, operator);
        }
    }    
    
    /** Returns the position of the first "OR" found in the list from the position 'pos' and onwards or -1 if no OR is found */
    private int posOfNextOr(ArrayList<Object> tree, int pos) {
        for (int i=pos;i<tree.size();i++) {
            Object o=tree.get(i);
            if (o instanceof String && ((String)o).equals("OR")) return i;
        }
        return -1;
    }
    
    private Condition_position_boolean convertToPositionCondition(ArrayList<Object> tree) throws ParseError {
        String operator=(String)tree.get(0);
        Condition_position_boolean condition=new Condition_position_boolean((operator.equals("AND")?Condition_boolean.AND:Condition_boolean.OR));
        for (int i=1;i<tree.size();i++) {
            Object element=tree.get(i);
            if (element instanceof ArrayList) {
                Condition_position_boolean subcondition=convertToPositionCondition((ArrayList)element);
                condition.addCondition(subcondition);               
            } else {
                Condition_position subcondition=parsePositionCondition((String)element);
                condition.addCondition(subcondition);
            }
        }
        return condition;
    }
    
    private Condition_region_boolean convertToRegionCondition(ArrayList<Object> tree) throws ParseError {
        String operator=(String)tree.get(0);
        Condition_region_boolean condition=new Condition_region_boolean((operator.equals("AND")?Condition_boolean.AND:Condition_boolean.OR));
          for (int i=1;i<tree.size();i++) {
            Object element=tree.get(i);
            if (element instanceof ArrayList) {
                Condition_region_boolean subcondition=convertToRegionCondition((ArrayList)element);
                condition.addCondition(subcondition);               
            } else {
                Condition_region subcondition=parseRegionCondition((String)element);
                condition.addCondition(subcondition);
            }
        }      
        return condition;
    }    
    
    /** Parses and sets the where-condition parameter in the OperationTask */
    protected void parseWherePositionCondition(String text, OperationTask task) throws ParseError {
       Condition_position condition=parsePossiblyCompoundPositionCondition(text);
       task.setParameter("where", condition);       
    }
    
     /** Parses a position condition (which could be a compound condition) and returns a condition object */  
    protected Condition_position parsePossiblyCompoundPositionCondition(String text) throws ParseError {
        text=text.trim(); // just in case
        if (text.matches(".+(\\s|\\)\\s*)(AND|and|OR|or)(\\s|\\s*\\().+")) { // Is this a compound condition? Matches e.g. " AND (" or ")or("
           ArrayList<Object> tree=parseCompoundCondition(text);
           if (tree.size()<3) throw new ParseError("Unable to parse condition:"+text);
           restructureConditionTree(tree);
           Condition_position_boolean condition=convertToPositionCondition(tree);
           return condition;          
        } else {
           if (text.startsWith("(") && text.endsWith(")")) text=stripExcessiveParentheses(text);
           Condition_position condition=parsePositionCondition(text);
           return condition;
        }
    }    
  
    /** 
     * Parses the given text string containing a (single) position condition clause 
     * and returns a Condition_position object to represent the condition
     */
    protected Condition_position parsePositionCondition(String text) throws ParseError {
          text=text.trim();
          if (text.isEmpty()) throw new ParseError("Empty condition");
          Boolean whereNot;
          String comparator="";
          String operandA=null;
          String operandB=null;
          String operandB2=null;
          Pattern pattern=Pattern.compile("^(not )?(inside) ([a-zA-Z_0-9-]+)");
          Matcher matcher=pattern.matcher(text);
          if (matcher.find()) {
               if (matcher.group(1)!=null && matcher.group(1).startsWith("not")) whereNot=Boolean.TRUE;else whereNot=Boolean.FALSE;
               comparator=matcher.group(2);
               operandA=matcher.group(3);
               // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
          } else {
              pattern=Pattern.compile("^(not )?([a-zA-Z_0-9-]+)\\s*(bases overlap|bases not overlap|regions overlap|regions not overlap|equals relative strand|equals|case-sensitive equals relative strand|case-sensitive equals|matches relative strand|matches|is uppercase|is lowercase|has same case as|<=|>=|<>|>|=|<|in)\\s*(\"?[a-zA-Z_0-9-\\.\\-]+\"?)?(\\s+to\\s+([a-zA-Z_0-9-\\.\\-]+))?");
              matcher=pattern.matcher(text);
              if (matcher.find()) {
                  if (matcher.group(1)!=null && matcher.group(1).startsWith("not")) whereNot=Boolean.TRUE;else whereNot=Boolean.FALSE;
                  operandA=matcher.group(2);
                  comparator=matcher.group(3);
                  operandB=matcher.group(4);
                  operandB2=matcher.group(6);
                  if (!(comparator.equals("is uppercase") || comparator.equals("is lowercase")) && (operandB==null || operandB.isEmpty())) throw new ParseError("Missing second operand (or illegal value) for '"+comparator+"' comparison");
                  // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
              } else throw new ParseError("Unable to parse position-condition: '"+text+"'");          
          }
          // System.err.println("whereNot="+whereNot+", comparator="+comparator+",  opA="+operandA+", opB="+operandB);
          Condition_position condition=new Condition_position();         
          condition.setNegateAll(whereNot);
          condition.setComparator(comparator);
          condition.setOperandAString(operandA);
          condition.setOperandBString(operandB); 
          if (comparator!=null && comparator.equals("in")) {
              if (operandB2!=null && !operandB2.isEmpty()) condition.setOperandB2String(operandB2);
              else throw new ParseError("Missing upper bound (or illegal value) for numerical range in condition");
          } 
          return condition;
    }
    
    
    /** Parses and sets the where-condition parameter in the OperationTask */
    protected void parseWhereRegionCondition(String text, OperationTask task) throws ParseError {       
       Condition_region condition=parsePossiblyCompoundRegionCondition(text);
       task.setParameter("where", condition);
        
    }
    
    /** Parses a region condition (which could be a compound condition) and returns a condition object */
    protected Condition_region parsePossiblyCompoundRegionCondition(String text) throws ParseError {
        text=text.trim(); // just in case
        if (text.matches(".+(\\s|\\)\\s*)(AND|and|OR|or)(\\s|\\s*\\().+")) { // Is this a compound condition? Matches e.g. " AND (" or ")or("
           ArrayList<Object> tree=parseCompoundCondition(text);
           if (tree.size()<3) throw new ParseError("Unable to parse condition:"+text);
           restructureConditionTree(tree);
           Condition_region_boolean condition=convertToRegionCondition(tree);
           return condition;
        } else {
           if (text.startsWith("(") && text.endsWith(")")) text=stripExcessiveParentheses(text);            
           Condition_region condition=parseRegionCondition(text);
           return condition;
        }
    }    
    
    /** 
     * Parses the given text string containing a (single) region condition clause 
     * and returns a Condition_region object to represent the condition
     */    
    protected Condition_region parseRegionCondition(String text) throws ParseError {
          text=text.trim();
          if (text.isEmpty()) throw new ParseError("Empty condition");
          Boolean whereNot;
          String compareProperty=null;
          String comparator=""; // this will cause problems since it is not always checked for NULL
          String operandA=null;
          String operandB=null;
          String operandB2=null;
          String typeRestrict=null;
          Class userpropertyType=null; // used to flag the property type if user-defined properties are referenced. The type could be String, Number or Boolean
          Pattern pattern=Pattern.compile("^(not )?region'?s?'? (?:type|(text|boolean) property\\s+\"(.+?)\") (equals|matches in|matches|is in|in set) (\\S+)");
          Matcher matcher=pattern.matcher(text);
          if (matcher.find()) { // region type condition
               if (matcher.group(1)!=null && matcher.group(1).startsWith("not")) whereNot=Boolean.TRUE;else whereNot=Boolean.FALSE;
               comparator=matcher.group(4);
               operandB=matcher.group(5);
               String userproperty=matcher.group(3);
               if (userproperty!=null) {
                   compareProperty=userproperty.trim();
                   String userpropertyTypeString=matcher.group(2);
                   userpropertyType=(userpropertyTypeString.equals("text"))?String.class:Boolean.class;
               } else {
                   compareProperty="type";
               }               
               if (comparator.equals("in set")) comparator="is in"; // for backwards compatibility
               if (comparator.equals("is in") || comparator.equals("matches in")) { // the operand should be a TextVariable, TextMap or Collection
                   if (!operandB.matches("^[a-zA-Z_0-9-]+$")) throw new ParseError("Data item name '"+operandB+"' contains illegal characters");
               } else { // comparator is 'equals' or 'matches'
                   // Now allows equals or matches with TextVariable or TextMap. Hence, the line below is commented out
                   // if (!(operandB.startsWith("\"") && operandB.endsWith("\""))) throw new ParseError("Literal text expression '"+operandB+"' should be enclosed in double quotation marks");         
               }
               // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
          } else {
              //pattern=Pattern.compile("^(not )?region'?s?'? (score|length|numeric property\\s+\"(.+?)\")\\s*(>=|<=|<>|<|=|>|in)\\s*([a-zA-Z_0-9-\\.\\-]+)(\\s+to\\s+([a-zA-Z_0-9-\\.\\-]+))?");
              pattern=Pattern.compile("^(not )?region'?s?'? (score|length|numeric property\\s+\"(.+?)\")\\s*(>=|<=|<>|<|=|>|in)\\s*((?:numeric )?property \".+?\"|[a-zA-Z_0-9-\\.\\-]+)(\\s+to\\s+((?:numeric )?property \".+?\"|[a-zA-Z_0-9-\\.\\-]+))?");
              matcher=pattern.matcher(text);
              if (matcher.find()) { // region score or length condition
                  if (matcher.group(1)!=null && matcher.group(1).startsWith("not")) whereNot=Boolean.TRUE;else whereNot=Boolean.FALSE;
                  compareProperty=matcher.group(2);
                  String userproperty=matcher.group(3);
                  if (userproperty!=null) {
                      compareProperty=userproperty.trim();
                      userpropertyType=Number.class;
                  }                   
                  comparator=matcher.group(4);
                  operandB=matcher.group(5);
                  operandB2=matcher.group(7);
                  // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
              } else {
                  pattern=Pattern.compile("^(not )?region'?s?'? (overlaps|inside|covers|present in|similar in)( type-equal| type-matching)? ([a-zA-Z_0-9-\\.\\-]+)");
                  matcher=pattern.matcher(text);
                  if (matcher.find()) { // region overlaps/inside condition
                      if (matcher.group(1)!=null && matcher.group(1).startsWith("not")) whereNot=Boolean.TRUE;else whereNot=Boolean.FALSE;
                      compareProperty=matcher.group(2);
                      typeRestrict=matcher.group(3);
                      if (typeRestrict!=null) typeRestrict=typeRestrict.trim();
                      if (typeRestrict!=null && typeRestrict.isEmpty()) typeRestrict=null;
                      operandB=matcher.group(4);
                  // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                  } else {
                     // note: I have excluded "weighted min/minimum", "weighted max/maximum" and "weighted median" from this list even though the condition supports it, since I am not sure if it makes any sense to include them
                     //pattern=Pattern.compile("^(not )?region'?s?'? (distance to closest|distance to any|avg|average|min|max|median|minimum|maximum|sum|weighted avg|weighted average|weighted sum|startValue|endValue|relativeStartValue|relativeEndValue|regionStartValue|regionEndValue|centerValue)( type-equal| type-matching)? (interaction partner|[a-zA-Z_0-9-\\.\\-]+)\\s*(<=|=|<>|>=|>|<|in)\\s*([a-zA-Z_0-9-\\.\\-]+)(\\s+to\\s+([a-zA-Z_0-9-\\.\\-]+))?");
                     pattern=Pattern.compile("^(not )?region'?s?'? (distance to closest|distance to any|avg|average|min|max|median|minimum|maximum|sum|weighted avg|weighted average|weighted sum|startValue|endValue|relativeStartValue|relativeEndValue|regionStartValue|regionEndValue|centerValue)( type-equal| type-matching)? (interaction partner|[a-zA-Z_0-9-\\.\\-]+)\\s*(<=|=|<>|>=|>|<|in)\\s*((?:numeric )?property \".+?\"|[a-zA-Z_0-9-\\.\\-]+)(\\s+to\\s+((?:numeric )?property \".+?\"|[a-zA-Z_0-9-\\.\\-]+))?");
                     matcher=pattern.matcher(text);
                     if (matcher.find()) { // position-condition
                        if (matcher.group(1)!=null && matcher.group(1).startsWith("not")) whereNot=Boolean.TRUE;else whereNot=Boolean.FALSE;
                        compareProperty=matcher.group(2);
                        typeRestrict=matcher.group(3);
                        if (typeRestrict!=null) typeRestrict=typeRestrict.trim();
                        if (typeRestrict!=null && typeRestrict.isEmpty()) typeRestrict=null;
                        operandA=matcher.group(4);
                        comparator=matcher.group(5);
                        operandB=matcher.group(6);
                        operandB2=matcher.group(8);
                        if (typeRestrict!=null && !(compareProperty.startsWith("distance to"))) throw new ParseError("Misplaced restriction '"+typeRestrict+"' in region-condition");
                  // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                  } else throw new ParseError("Unable to parse region-condition: '"+text+"'");    
              } // not region overlap condition          
            } // not region score|length condition
          } // not region type condition
          //System.err.println("whereNot="+whereNot+", comparator="+comparator+",  opA="+operandA+", opB="+operandB);

          Condition_region condition=new Condition_region();         
          condition.setNegateAll(whereNot);
          condition.setComparator(comparator);
          condition.setOperandAString(operandA);
          condition.setOperandBString(operandB);
          condition.setOtherRegionTypeRestriction(typeRestrict);
          condition.setUserDefinedPropertyType(userpropertyType);
          if (comparator!=null && comparator.equals("in")) {
              if (operandB2!=null && !operandB2.isEmpty()) condition.setOperandB2String(operandB2);  
              else throw new ParseError("Missing upper bound for numerical range in condition");   
          }
          condition.setCompareProperty(compareProperty);        
          return condition;
    }

     /** Parses and sets the within-condition parameter in the OperationTask */
    public void parseWithinCondition(String text, OperationTask task) throws ParseError {
          Condition_within condition=parseWithinCondition(text);
          task.setParameter("within", condition);
    }
    
    /** 
     * Parses the given text string containing a within-condition clause and returns a Condition_within object
     * to represent the condition.
     * @param text A comma-separated list of sequence segments that satisfies the condition. 
     *             Each sequence segment must be on the format "sequencename:start-end" (where start and end are genomic coordinates)
     */
    protected Condition_within parseWithinCondition(String text) throws ParseError {
          text=text.trim();
          if (text.isEmpty()) throw new ParseError("Empty within condition");
          //Boolean whereNot;
          Pattern pattern=Pattern.compile("\\s*[a-zA-Z_0-9-]+:\\d+-\\d\\s*");
          String[] ranges=text.split(",");
          for (String range:ranges) {
             Matcher matcher=pattern.matcher(range);
             if (!matcher.find()) throw new ParseError("Unable to parse within-condition range: '"+text+"'");              
          }          
          //System.err.println("whereNot="+whereNot+", comparator="+comparator+",  opA="+operandA+", opB="+operandB);
          Condition_within condition=new Condition_within();         
          condition.setOperandAString(text);     
          return condition;
    }   
    
    
    /** Parses and returns a basic condition (which can be a compound) */
    public Condition_basic parseBasicCondition(String text) throws ParseError {              
       Condition_basic condition=parsePossiblyCompoundBasicCondition(text);
       return condition;
       // task.setParameter("where", condition);      
    }
    
    /** Parses a basic condition (which could be a compound condition) and returns a condition object */
    private Condition_basic parsePossiblyCompoundBasicCondition(String text) throws ParseError {
        text=text.trim(); // just in case
        if (text.matches(".+(\\s|\\)\\s*)(AND|and|OR|or)(\\s|\\s*\\().+")) { // Is this a compound condition? Matches e.g. " AND (" or ")or("
           ArrayList<Object> tree=parseCompoundCondition(text);
           if (tree.size()<3) throw new ParseError("Unable to parse condition:"+text);
           restructureConditionTree(tree);
           Condition_basic_boolean condition=convertToBasicCondition(tree);
           return condition;
        } else {
           if (text.startsWith("(") && text.endsWith(")")) text=stripExcessiveParentheses(text);            
           Condition_basic condition=parseSingleBasicCondition(text);
           return condition;
        }
    }    
    
    /** 
     * Parses the given text string containing a (single) basic condition clause 
     * and returns a Condition_basic object to represent the condition
     */    
    private Condition_basic parseSingleBasicCondition(String text) throws ParseError {
          text=text.trim();
          if (text.isEmpty()) throw new ParseError("Empty condition");
          Boolean whereNot;
          String comparator=""; // this will cause problems since it is not always checked for NULL
          String operandA=null;
          String operandB=null;
          String operandB2=null;
          Pattern pattern=Pattern.compile("^(not\\s+)?([a-zA-Z_0-9-\\.\\-]+)\\s*(>=|<=|<>|<|=|>|in|equals|matches in|matches|is in|in set)\\s*(\\S+|\".+?\")(\\s+to\\s+(\\S+))?");
          Matcher matcher=pattern.matcher(text);
          if (matcher.find()) { // 
               if (matcher.group(1)!=null && matcher.group(1).startsWith("not")) whereNot=Boolean.TRUE;else whereNot=Boolean.FALSE;
               operandA=matcher.group(2);               
               comparator=matcher.group(3);               
               operandB=matcher.group(4);
               operandB2=matcher.group(6);               
               if (comparator.equals("in set")) comparator="is in"; // for backwards compatibility
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
          } else throw new ParseError("Unable to parse condition: '"+text+"'");    
          Condition_basic condition=new Condition_basic();         
          condition.setNegateAll(whereNot);
          condition.setComparator(comparator);
          condition.setOperandAString(operandA);
          condition.setOperandBString(operandB);;
          if (comparator!=null && comparator.equals("in")) {
              if (operandB2!=null && !operandB2.isEmpty()) condition.setOperandB2String(operandB2);  
              else throw new ParseError("Missing upper bound for numerical range in condition");   
          }      
          return condition;
    }    
    
    private Condition_basic_boolean convertToBasicCondition(ArrayList<Object> tree) throws ParseError {
        String operator=(String)tree.get(0);
        Condition_basic_boolean condition=new Condition_basic_boolean((operator.equals("AND")?Condition_boolean.AND:Condition_boolean.OR));
          for (int i=1;i<tree.size();i++) {
            Object element=tree.get(i);
            if (element instanceof ArrayList) {
                Condition_basic_boolean subcondition=convertToBasicCondition((ArrayList)element);
                condition.addCondition(subcondition);               
            } else {
                Condition_basic subcondition=parseSingleBasicCondition((String)element);
                condition.addCondition(subcondition);
            }
        }      
        return condition;
    }      
    
}
