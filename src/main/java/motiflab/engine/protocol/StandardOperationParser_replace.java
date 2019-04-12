/*
 
 
 */

package motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DataMap;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.TextVariable;
import motiflab.engine.operations.Operation_replace;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_replace extends StandardOperationParser {

//    Pattern regexPattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace \"(.*?)\" with \"(.*?)\" in ([a-zA-Z_0-9-]+)( property \"(.+?)\")?( where (.+))?(\\s*\\S.*)?");
//    Pattern variablePattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace ([a-zA-Z_0-9-]+) with ([a-zA-Z_0-9-]+) in ([a-zA-Z_0-9-]+)( property \"(.+?)\")?( where (.+))?(\\s*\\S.*)?");
//    Pattern macroPattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace macros in ([a-zA-Z_0-9-]+)(\\s*\\S.*)?");   
//    Pattern insertPattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace (beginning|start|end|ending) with \"(.*?)\" in ([a-zA-Z_0-9-]+)(\\s*\\S.*)?");
 
    // note: this is the regex for a quoted string (which could contain escaped quotes):  \"((?:\\\\\\\\|\\\\\\w|\\\\\"|[^\"\\\\])*?)\"
    Pattern regexPattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace \"((?:\\\\\\\\|\\\\\\w|\\\\\"|[^\"\\\\])*?)\" with \"((?:\\\\\\\\|\\\\\\w|\\\\\"|[^\"\\\\])*?)\" in ([a-zA-Z_0-9-]+)( property \"(.+?)\")?( where (.+))?(\\s*\\S.*)?");
    Pattern variablePattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace ([a-zA-Z_0-9-]+) with ([a-zA-Z_0-9-]+) in ([a-zA-Z_0-9-]+)( property \"(.+?)\")?( where (.+))?(\\s*\\S.*)?");
    Pattern mapPattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace ([a-zA-Z_0-9-]+) in ([a-zA-Z_0-9-]+)( property \"(.+?)\")?( where (.+))?(\\s*\\S.*)?");
    Pattern macroPattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace macros in ([a-zA-Z_0-9-]+)(\\s*\\S.*)?");   
    Pattern insertPattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?replace (beginning|start|end|ending) with \"((?:\\\\\\\\|\\\\\\w|\\\\\"|[^\"\\\\])*?)\" in ([a-zA-Z_0-9-]+)(\\s*\\S.*)?");
    
    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();

        String searchExpression=(String)task.getParameter(Operation_replace.SEARCH_PATTERN);
        String replaceExpression=(String)task.getParameter(Operation_replace.REPLACE_PATTERN);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        String propertyName=(String)task.getParameter(Operation_replace.REGION_PROPERTY);
        Boolean useMacros=(Boolean)task.getParameter(Operation_replace.EXPAND_MACROS);
        if (useMacros==null) useMacros=Boolean.FALSE;
        Boolean insertBefore=(Boolean)task.getParameter(Operation_replace.INSERT_BEFORE);
        if (insertBefore==null) insertBefore=Boolean.FALSE;
        Boolean insertAfter=(Boolean)task.getParameter(Operation_replace.INSERT_AFTER);
        if (insertAfter==null) insertAfter=Boolean.FALSE;  
        Boolean expressionsFromTextVariable=(Boolean)task.getParameter(Operation_replace.EXPRESSIONS_FROM_TEXTVARIABLE);
        if (expressionsFromTextVariable==null) expressionsFromTextVariable=Boolean.FALSE;    
        Boolean expressionsFromMap=(Boolean)task.getParameter(Operation_replace.EXPRESSIONS_FROM_MAP);
        if (expressionsFromMap==null) expressionsFromMap=Boolean.FALSE;          
        
        StringBuilder msg=new StringBuilder();
        if (!sourceName.equalsIgnoreCase(targetName))  msg.append(targetName+" = ");
        msg.append("replace ");
        if (useMacros) {
           msg.append("macros in "); 
        } else if (insertBefore || insertAfter) {
            msg.append((insertBefore)?"beginning":"end");
            msg.append(" with \"");
            msg.append(MotifLabEngine.escapeQuotedString(replaceExpression));
            msg.append("\" in ");  
        } else if (expressionsFromTextVariable) { //
            msg.append(searchExpression);
            msg.append(" with ");
            msg.append(replaceExpression);
            msg.append(" in ");            
        } else if (expressionsFromMap) { //
            msg.append(searchExpression);
            msg.append(" in ");          
        } else  { //
            searchExpression="\""+MotifLabEngine.escapeQuotedString(searchExpression)+"\"";
            replaceExpression="\""+MotifLabEngine.escapeQuotedString(replaceExpression)+"\"";          
            msg.append(searchExpression);
            msg.append(" with ");
            msg.append(replaceExpression);
            msg.append(" in ");            
        }
        msg.append(sourceName);
        if (!useMacros && propertyName!=null && !propertyName.equalsIgnoreCase("type")) {
            msg.append(" property \"");
            msg.append(propertyName);
            msg.append("\"");
        }        
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg.append(" where "+getCommandString_condition(condition));
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg.append(" in collection "+subset);
        return msg.toString();
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="replace";
           String sourceName=null;
           String targetName=null;
           String propertyName=null;
           String sequenceCollectionName=null;
           String searchExpression=null;
           String replaceExpression=null;
           String whereString=null;  
           boolean usemacros=false;
           boolean insertbefore=false;
           boolean insertafter=false;        
           boolean expressionsFromTextVariable=false;
           boolean expressionsFromMap=false;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollectionName=splitOn[1].trim();
           if (sequenceCollectionName!=null) {
             String[] splitOn2=sequenceCollectionName.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=macroPattern;
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);               
               String unknown=matcher.group(4);
               usemacros=true;
               if (unknown!=null && !unknown.trim().isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown); 
           } else {
               pattern=regexPattern;
               matcher=pattern.matcher(splitOn[0]);
               if (matcher.find()) {
                   targetName=matcher.group(2);
                   searchExpression=matcher.group(3);
                   replaceExpression=matcher.group(4);
                   sourceName=matcher.group(5);  
                   propertyName=matcher.group(7);                   
                   whereString=matcher.group(9);
                   String unknown = matcher.group(10);
                   //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                   if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);                  
               } else {
                   pattern=variablePattern;
                   matcher=pattern.matcher(splitOn[0]);                   
                   if (matcher.find()) {
                       targetName=matcher.group(2);
                       searchExpression=matcher.group(3);
                       replaceExpression=matcher.group(4);
                       sourceName=matcher.group(5);  
                       propertyName=matcher.group(7);                   
                       whereString=matcher.group(9);
                       String unknown = matcher.group(10);
                       //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                       if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);   
                       expressionsFromTextVariable=true;
                   } else {
                       pattern=mapPattern;
                       matcher=pattern.matcher(splitOn[0]);                   
                       if (matcher.find()) {
                           targetName=matcher.group(2);
                           searchExpression=matcher.group(3);
                           replaceExpression=null;
                           sourceName=matcher.group(4);  
                           propertyName=matcher.group(6);                   
                           whereString=matcher.group(8);
                           String unknown = matcher.group(9);
                           //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                           if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);  
                           expressionsFromMap=true;
                       } else {
                           pattern=insertPattern;
                           matcher=pattern.matcher(splitOn[0]);
                           if (matcher.find()) {
                               targetName=matcher.group(2);
                               searchExpression=matcher.group(3);
                               replaceExpression=matcher.group(4);
                               sourceName=matcher.group(5);  
                               String unknown = matcher.group(6);
                               if (searchExpression.equalsIgnoreCase("beginning") || searchExpression.equalsIgnoreCase("start")) insertbefore=true;
                               else insertafter=true;
                               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
                           } else throw new ParseError("Unable to parse "+operationName+" command: "+command+"  (remember to place search and replace expressions in double quotes unless they are Text Variables)");               
                       }   
                   }   
               }
           }
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (!usemacros && (searchExpression==null || searchExpression.isEmpty())) throw new ParseError("Missing search expression");
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           if (propertyName!=null && propertyName.isEmpty()) propertyName=null;
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           if (whereString!=null && !whereString.isEmpty()) {
               parseWhereRegionCondition(whereString, task);
           }
           if (propertyName!=null && sourceclass!=RegionDataset.class) throw new ParseError(engine.getTypeNameForDataClass(sourceclass)+" '"+sourceName+"' can not have a property named '"+propertyName+"'");
           if (expressionsFromTextVariable) {
               Class typeSearch=lookup.getClassFor(searchExpression);
               Class typeReplace=lookup.getClassFor(replaceExpression);
               if (typeSearch==null) throw new ParseError("Unknown data object: "+searchExpression+".  Remember to place search-expression in double quotes unless it refers to a Text Variable or Map");
               if (typeSearch!=TextVariable.class) throw new ParseError("'"+searchExpression+"' is not a Text Variable.  Remember to place search-expression in double quotes unless it refers to a Text Variable or Map");
               if (typeReplace==null) throw new ParseError("Unknown data object: "+replaceExpression+".  Remember to place replace-expression in double quotes unless it refers to a Text Variable or Map");
               if (typeReplace!=TextVariable.class) throw new ParseError("'"+replaceExpression+"' is not a Text Variable.  Remember to place replace-expression in double quotes unless it refers to a Text Variable or Map");
           } if (expressionsFromMap) {
               Class typeSearch=lookup.getClassFor(searchExpression);
               if (typeSearch==null) throw new ParseError("Unknown data object: "+searchExpression+".  Remember to place search-expression in double quotes unless it refers to a Text Variable or Map");
               if (!DataMap.class.isAssignableFrom(typeSearch)) throw new ParseError("'"+searchExpression+"' is not a Map.  Remember to place search-expression in double quotes unless it refers to a Text Variable or Map");
           } else {
               searchExpression=MotifLabEngine.unescapeQuotedString(searchExpression);
               replaceExpression=MotifLabEngine.unescapeQuotedString(replaceExpression);
           }            
           
           lookup.register(targetName, sourceclass);
           task.addAffectedDataObject(targetName, sourceclass);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           task.setParameter(Operation_replace.SEARCH_PATTERN, searchExpression);
           task.setParameter(Operation_replace.REPLACE_PATTERN, replaceExpression);
           task.setParameter(Operation_replace.REGION_PROPERTY, propertyName);      
           if (usemacros) task.setParameter(Operation_replace.EXPAND_MACROS, Boolean.TRUE); else task.removeParameter(Operation_replace.EXPAND_MACROS);
           if (insertbefore) task.setParameter(Operation_replace.INSERT_BEFORE, Boolean.TRUE); else task.removeParameter(Operation_replace.INSERT_BEFORE);
           if (insertafter)  task.setParameter(Operation_replace.INSERT_AFTER, Boolean.TRUE); else task.removeParameter(Operation_replace.INSERT_AFTER);      
           if (expressionsFromTextVariable) task.setParameter(Operation_replace.EXPRESSIONS_FROM_TEXTVARIABLE, Boolean.TRUE); else task.removeParameter(Operation_replace.EXPRESSIONS_FROM_TEXTVARIABLE); // this is used to differentiate between search/replace pairs based on TextVariables versus literal strings (both are provded as strings)           
           if (expressionsFromMap)  task.setParameter(Operation_replace.EXPRESSIONS_FROM_MAP, Boolean.TRUE); else task.removeParameter(Operation_replace.EXPRESSIONS_FROM_MAP); //           
           if (sequenceCollectionName!=null && !sequenceCollectionName.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollectionName);
           return task;
    }

}
