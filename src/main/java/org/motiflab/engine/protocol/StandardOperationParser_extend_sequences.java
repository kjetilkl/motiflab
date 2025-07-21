/*
 
 
 */

package org.motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_extend_sequences;
 /*
 * @author kjetikl
 */


public class StandardOperationParser_extend_sequences extends StandardOperationParser {

    @Override
    public String getCommandString(OperationTask task) {
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);                        
        String extendExpression=(String)task.getParameter(Operation_extend_sequences.EXTEND_EXPRESSION);
        String extendUpstreamExpression=(String)task.getParameter(Operation_extend_sequences.EXTEND_UPSTREAM_EXPRESSION);
        String extendDownstreamExpression=(String)task.getParameter(Operation_extend_sequences.EXTEND_DOWNSTREAM_EXPRESSION);
        Object orientation=task.getParameter(Operation_extend_sequences.USE_RELATIVE_ORIENTATION);
        boolean useRelativeOrientation=(orientation instanceof Boolean)?((Boolean)orientation):false;

        String msg="extend_sequences";
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in "+subset;

        if (extendExpression!=null || (extendUpstreamExpression!=null && extendDownstreamExpression!=null && extendUpstreamExpression.equals(extendDownstreamExpression))) {
            String expression=(extendExpression!=null)?extendExpression:extendUpstreamExpression;
            msg+=" by "+expression;
            if (!expression.endsWith(" bp")) msg+=" bp";
        } else {
            if (extendUpstreamExpression!=null) msg+= " by "+extendUpstreamExpression+" bp "+((useRelativeOrientation)?"upstream":"before start");
            if (extendUpstreamExpression!=null && extendDownstreamExpression!=null) msg+=" and";
            if (extendDownstreamExpression!=null) msg+= " by "+extendDownstreamExpression+" bp "+((useRelativeOrientation)?"downstream":"after end");
        }
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="extend_sequences";
           String sequenceCollection=null;
           String extendExpression=null;
           String extendUpstreamExpression=null;
           String extendDownstreamExpression=null;
           boolean useRelativeOrientation=false;

           Pattern pattern=Pattern.compile("^"+operationName+"( (in )?([a-zA-Z_0-9-]+))? by ([a-zA-Z_0-9-]+)( bp)? (upstream|downstream|before start|after end)( and)? by ([a-zA-Z_0-9-]+)( bp)? (upstream|downstream|before start|after end)(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) { // first test if both directions are specified
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("#1: Group["+i+"]=>"+matcher.group(i));
               sequenceCollection=matcher.group(3);
               String expression1=matcher.group(4);
               String direction1=matcher.group(6);
               String expression2=matcher.group(8);
               String direction2=matcher.group(10);
               String unknown=matcher.group(11);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               if ( (direction1.equals("upstream") || direction1.equals("downstream")) && (direction2.equals("before start") || direction2.equals("after end")) ) throw new ParseError("Mixing relative and absolute directions is not allowed");
               if ( (direction2.equals("upstream") || direction2.equals("downstream")) && (direction1.equals("before start") || direction1.equals("after end")) ) throw new ParseError("Mixing relative and absolute directions is not allowed");
               if (direction1.equals(direction2)) throw new ParseError("Direction '"+direction1+"' is specified twice");
               useRelativeOrientation=(direction1.equals("upstream") || direction2.equals("upstream"));
               if (direction1.equals("upstream") || direction1.equals("before start")) { // direction1 is "upstream" and direction2 is "downstream" 
                   extendUpstreamExpression=expression1;
                   extendDownstreamExpression=expression2;
               } else { // direction1 is "downstream" and direction2 is "upstream" 
                   extendUpstreamExpression=expression2;
                   extendDownstreamExpression=expression1;                   
               }
           } else { // only one direction or no directionality?
               pattern=Pattern.compile("^"+operationName+"( (in )?([a-zA-Z_0-9-]+))? by ([a-zA-Z_0-9-]+)( bp)?( (upstream|downstream|before start|after end))?(\\s*\\S.*)?");
               matcher=pattern.matcher(command);
               if (matcher.find()) {
                   //for (int i=0;i<=matcher.groupCount();i++) System.err.println("#2: Group["+i+"]=>"+matcher.group(i));
                   sequenceCollection=matcher.group(3);
                   String expression=matcher.group(4);
                   String direction=matcher.group(7);
                   String unknown=matcher.group(8);
                   if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
                   if (expression==null)  throw new ParseError("Missing operand for specifying number of bp to extend");  
                   if (direction==null || direction.trim().isEmpty()) {
                       extendExpression=expression.trim();
                       extendUpstreamExpression=null;
                       extendDownstreamExpression=null;
                       useRelativeOrientation=false; // this is not really needed
                   } else { // direction is specified
                       if (direction.equals("upstream")) {
                           extendUpstreamExpression=expression.trim();
                           extendDownstreamExpression=null;
                           extendExpression=null;
                           useRelativeOrientation=true;
                       } else if (direction.equals("before start")) {
                           extendUpstreamExpression=expression.trim();
                           extendDownstreamExpression=null;
                           extendExpression=null;
                           useRelativeOrientation=false;
                       } else if (direction.equals("downstream")) {
                           extendDownstreamExpression=expression.trim();
                           extendUpstreamExpression=null;
                           extendExpression=null;
                           useRelativeOrientation=true;
                       } else if (direction.equals("after end")) {
                           extendDownstreamExpression=expression.trim();
                           extendUpstreamExpression=null;
                           extendExpression=null;
                           useRelativeOrientation=false;
                       } else throw new ParseError("Unrecognized direction: "+direction);                
                   }                                        
               } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           } 
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");

           if (sequenceCollection!=null) {
               Class sourceclass=lookup.getClassFor(sequenceCollection);
               if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sequenceCollection);
               if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sequenceCollection+"' is not a Sequence Collection");              
           }

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);

           if (extendExpression!=null) extendExpression=removeBPsuffix(extendExpression);
           if (extendUpstreamExpression!=null) extendUpstreamExpression=removeBPsuffix(extendUpstreamExpression);
           if (extendDownstreamExpression!=null) extendDownstreamExpression=removeBPsuffix(extendDownstreamExpression);
           
           if (extendUpstreamExpression!=null && extendDownstreamExpression!=null && extendUpstreamExpression.equals(extendDownstreamExpression)) {
               // same expression in both directions, just use a common expression
               extendExpression=extendUpstreamExpression;
               extendUpstreamExpression=null;
               extendDownstreamExpression=null;
               useRelativeOrientation=false;
           }
           task.setParameter(Operation_extend_sequences.EXTEND_EXPRESSION,extendExpression);                    
           task.setParameter(Operation_extend_sequences.EXTEND_UPSTREAM_EXPRESSION,extendUpstreamExpression);                    
           task.setParameter(Operation_extend_sequences.EXTEND_DOWNSTREAM_EXPRESSION,extendDownstreamExpression);                    
           task.setParameter(Operation_extend_sequences.USE_RELATIVE_ORIENTATION,useRelativeOrientation);                    
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           task.setTurnOffGUInotifications(true); // this will also update the visualization properly afterwards
           return task;        
    }
    
    private String removeBPsuffix(String string) {
        if (string.endsWith(" bp") && string.length()>3) return string.substring(0,string.lastIndexOf(" bp"));
        else return string;
    }

}
