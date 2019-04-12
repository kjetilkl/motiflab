/*
 
 
 */

package motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Condition_position;
import motiflab.engine.operations.Operation_extend;
 /*
 * @author kjetikl
 */


public class StandardOperationParser_extend extends StandardOperationParser {

    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String extendOperator=(String)task.getParameter(Operation_extend.EXTEND_OPERATOR);
        String extendUpstreamOperator=(String)task.getParameter(Operation_extend.EXTEND_UPSTREAM_OPERATOR);
        String extendDownstreamOperator=(String)task.getParameter(Operation_extend.EXTEND_DOWNSTREAM_OPERATOR);
        Object extendExpressionObject=task.getParameter(Operation_extend.EXTEND_EXPRESSION);
        Object extendUpstreamExpressionObject=task.getParameter(Operation_extend.EXTEND_UPSTREAM_EXPRESSION);
        Object extendDownstreamExpressionObject=task.getParameter(Operation_extend.EXTEND_DOWNSTREAM_EXPRESSION);
        
        String extendExpression="";
        String extendUpstreamExpression="";
        String extendDownstreamExpression="";
        
        if (extendExpressionObject!=null) {
            if (extendExpressionObject instanceof Condition_position) extendExpression=getCommandString_condition((Condition_position)extendExpressionObject);
            else extendExpression=extendExpressionObject.toString();
        }
        if (extendUpstreamExpressionObject!=null) {
            if (extendUpstreamExpressionObject instanceof Condition_position) extendUpstreamExpression=getCommandString_condition((Condition_position)extendUpstreamExpressionObject);
            else extendUpstreamExpression=extendUpstreamExpressionObject.toString();
        }
        if (extendDownstreamExpressionObject!=null) {
            if (extendDownstreamExpressionObject instanceof Condition_position) extendDownstreamExpression=getCommandString_condition((Condition_position)extendDownstreamExpressionObject);
            else extendDownstreamExpression=extendDownstreamExpressionObject.toString();
        }
        
        String msg="";
        String parstring="";
        if (extendOperator!=null) parstring = " "+extendOperator+" "+extendExpression;
        else {
            if (extendUpstreamOperator!=null) parstring = " upstream "+extendUpstreamOperator+" "+extendUpstreamExpression;
            if (extendDownstreamOperator!=null) parstring+= ((extendUpstreamOperator!=null)?",":"")+" downstream "+extendDownstreamOperator+" "+extendDownstreamExpression;
        }
        if (!sourceName.equalsIgnoreCase(targetName)) msg=targetName+" = ";
        msg+="extend "+sourceName;
        msg+=parstring;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);        
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="extend";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String whereString=null;
           String extendOperator=null;
           String extendUpstreamOperator=null;
           String extendDownstreamOperator=null;
           String extendExpression=null;
           String extendUpstreamExpression=null;
           String extendDownstreamExpression=null;
           String restCommand=command;
           String[] splitOn=restCommand.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           restCommand=splitOn[0];
           String[] splitOnWhere=restCommand.split(" where ");
           if (splitOnWhere.length==2 && !splitOnWhere[1].isEmpty()) whereString=splitOnWhere[1].trim();       
           else if (splitOnWhere.length>2) throw new ParseError("Too many where-clauses");
           restCommand=splitOnWhere[0];

           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?"+operationName+" ([a-zA-Z_0-9-]+)? (upstream|downstream)( (by|until|while))?( (.+?))?\\s*,\\s*(upstream|downstream)( (by|until|while))?( (.+))?");
           Matcher matcher=pattern.matcher(restCommand);
           if (matcher.find()) { // first try matching 2 directions (both upstream and downstream)
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               String firstDirection=matcher.group(4);
               String secondDirection=matcher.group(9);
               if (firstDirection.equals(secondDirection)) throw new ParseError(firstDirection+" extension specified twice");
               String firstOperator=matcher.group(6);
               String secondOperator=matcher.group(11);
               if (firstOperator==null)  throw new ParseError("Missing operator (by,while,until) for "+firstDirection+" extension");  
               if (secondOperator==null)  throw new ParseError("Missing operator (by,while,until) for "+secondDirection+" extension");  
               String firstExpression=matcher.group(8);
               String secondExpression=matcher.group(13);
               if (firstExpression==null)  throw new ParseError("Missing operand for "+firstDirection+" extension");  
               if (secondExpression==null)  throw new ParseError("Missing operand for "+secondDirection+" extension");  
               if (firstDirection.equals("upstream")) {
                   extendUpstreamOperator=firstOperator;
                   extendUpstreamExpression=firstExpression.trim();
                   extendDownstreamOperator=secondOperator;
                   extendDownstreamExpression=secondExpression.trim();                   
               } else { // firstDirection==downstream
                   extendUpstreamOperator=secondOperator;
                   extendUpstreamExpression=secondExpression.trim();
                   extendDownstreamOperator=firstOperator;
                   extendDownstreamExpression=firstExpression.trim();                     
               }
           } else { // if 2-directions-REGEX failed try 0-or-1 direction
               pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?"+operationName+" ([a-zA-Z_0-9-]+)?( (upstream|downstream))?( (by|until|while))?( (.+))?");
               matcher=pattern.matcher(restCommand);
               if (matcher.find()) {
                   //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
                   targetName=matcher.group(2);
                   sourceName=matcher.group(3);
                   String direction=matcher.group(5);
                   String operator=matcher.group(7);
                   String expression=matcher.group(9);
                   if (operator==null)  throw new ParseError("Missing operator (by,while,until) for extension");  
                   if (expression==null)  throw new ParseError("Missing operand for extension");  
                   if (direction==null) {
                       extendOperator=operator;
                       extendExpression=expression.trim();                  
                   } else { // direction specified
                       if (direction.equals("upstream")) {
                           extendUpstreamOperator=operator;
                           extendUpstreamExpression=expression.trim();                             
                       } else {
                           extendDownstreamOperator=operator;
                           extendDownstreamExpression=expression.trim();                                                        
                       }                 
                   }                                        
               } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           } 
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (whereString!=null) parseWhereRegionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=RegionDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object has wrong type!");
           lookup.register(targetName, RegionDataset.class);
           task.addAffectedDataObject(targetName, RegionDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);

           if (extendOperator!=null && !extendOperator.isEmpty()) {
               task.setParameter(Operation_extend.EXTEND_OPERATOR,extendOperator);
               if (extendOperator.equals("by")) {
                  task.setParameter(Operation_extend.EXTEND_EXPRESSION,removeBPsuffix(extendExpression)); 
               } else {
                   Condition_position condition=parsePossiblyCompoundPositionCondition(extendExpression);
                   task.setParameter(Operation_extend.EXTEND_EXPRESSION,condition);
               }
           }
           if (extendUpstreamOperator!=null && !extendUpstreamOperator.isEmpty()) {
               task.setParameter(Operation_extend.EXTEND_UPSTREAM_OPERATOR,extendUpstreamOperator);
               if (extendUpstreamOperator.equals("by")) {
                  task.setParameter(Operation_extend.EXTEND_UPSTREAM_EXPRESSION,removeBPsuffix(extendUpstreamExpression)); 
               } else {
                   Condition_position condition=parsePossiblyCompoundPositionCondition(extendUpstreamExpression);
                   task.setParameter(Operation_extend.EXTEND_UPSTREAM_EXPRESSION,condition);
               }
           }
           if (extendDownstreamOperator!=null && !extendDownstreamOperator.isEmpty()) {
               task.setParameter(Operation_extend.EXTEND_DOWNSTREAM_OPERATOR,extendDownstreamOperator);
               if (extendDownstreamOperator.equals("by")) {
                  task.setParameter(Operation_extend.EXTEND_DOWNSTREAM_EXPRESSION,removeBPsuffix(extendDownstreamExpression)); 
               } else {
                   Condition_position condition=parsePossiblyCompoundPositionCondition(extendDownstreamExpression);
                   task.setParameter(Operation_extend.EXTEND_DOWNSTREAM_EXPRESSION,condition);
               }
           }                     
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);

           return task;        
    }
    
    private String removeBPsuffix(String string) {
        if (string.endsWith(" bp") && string.length()>3) return string.substring(0,string.lastIndexOf(" bp"));
        else return string;
    }

}
