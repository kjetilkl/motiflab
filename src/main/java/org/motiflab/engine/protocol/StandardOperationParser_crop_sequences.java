/*
 
 
 */

package org.motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_crop_sequences;
 /*
 * @author kjetikl
 */


public class StandardOperationParser_crop_sequences extends StandardOperationParser {

    @Override
    public String getCommandString(OperationTask task) {
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);                        
        String cropToRegions=(String)task.getParameter(Operation_crop_sequences.CROP_TO_REGIONS);
        String cropExpression=(String)task.getParameter(Operation_crop_sequences.CROP_EXPRESSION);
        String cropUpstreamExpression=(String)task.getParameter(Operation_crop_sequences.CROP_UPSTREAM_EXPRESSION);
        String cropDownstreamExpression=(String)task.getParameter(Operation_crop_sequences.CROP_DOWNSTREAM_EXPRESSION);
        Object orientation=task.getParameter(Operation_crop_sequences.USE_RELATIVE_ORIENTATION);
        boolean useRelativeOrientation=(orientation instanceof Boolean)?((Boolean)orientation):false;

        String msg="crop_sequences";
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in "+subset;
        
        if (cropToRegions!=null) {
          msg+=" to "+cropToRegions;  
        } else {
            if (cropExpression!=null || (cropUpstreamExpression!=null && cropDownstreamExpression!=null && cropUpstreamExpression.equals(cropDownstreamExpression))) {
                String expression=(cropExpression!=null)?cropExpression:cropUpstreamExpression;
                msg+=" by "+expression;
                if (!expression.endsWith(" bp")) msg+=" bp";
            } else {
                if (cropUpstreamExpression!=null) msg+= " by "+cropUpstreamExpression+" bp from "+((useRelativeOrientation)?"upstream end":"start");
                if (cropUpstreamExpression!=null && cropDownstreamExpression!=null) msg+=" and";
                if (cropDownstreamExpression!=null) msg+= " by "+cropDownstreamExpression+" bp from "+((useRelativeOrientation)?"downstream end":"end");
            }
        }
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="crop_sequences";
           String sequenceCollection=null;
           String cropExpression=null;
           String cropUpstreamExpression=null;
           String cropDownstreamExpression=null;
           String cropToRegionsExpression=null;
           boolean useRelativeOrientation=false;

           Pattern pattern=Pattern.compile("^"+operationName+"( (in )?([a-zA-Z_0-9-]+))? to ([a-zA-Z_0-9-]+)(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) { // first test if sequences should be cropped to regions in a track         
               sequenceCollection=matcher.group(3);
               cropToRegionsExpression=matcher.group(4);
               String unknown=matcher.group(5);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order):"+unknown);         
           } else { // cropping an explicit number of bases
               pattern=Pattern.compile("^"+operationName+"( (in )?([a-zA-Z_0-9-]+))? by ([a-zA-Z_0-9-]+)( bp)? from (upstream end|downstream end|start|end)( and)? by ([a-zA-Z_0-9-]+)( bp)? from (upstream end|downstream end|start|end)(\\s*\\S.*)?");
               matcher=pattern.matcher(command);
               if (matcher.find()) { // check if both directions are specified
                   //for (int i=0;i<=matcher.groupCount();i++) System.err.println("#1: Group["+i+"]=>"+matcher.group(i));
                   sequenceCollection=matcher.group(3);
                   String expression1=matcher.group(4);
                   String direction1=matcher.group(6);
                   String expression2=matcher.group(8);
                   String direction2=matcher.group(10);
                   String unknown=matcher.group(11);
                   if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order):"+unknown);
                   if ( (direction1.equals("upstream end") || direction1.equals("downstream end")) && (direction2.equals("start") || direction2.equals("end")) ) throw new ParseError("Mixing relative and absolute directions is not allowed");
                   if ( (direction2.equals("upstream end") || direction2.equals("downstream end")) && (direction1.equals("start") || direction1.equals("end")) ) throw new ParseError("Mixing relative and absolute directions is not allowed");
                   if (direction1.equals(direction2)) throw new ParseError("Direction '"+direction1+"' is specified twice");
                   useRelativeOrientation=(direction1.equals("upstream end") || direction2.equals("upstream end"));
                   if (direction1.equals("upstream end") || direction1.equals("start")) { // direction1 is "upstream end" and direction2 is "downstream end" 
                       cropUpstreamExpression=expression1;
                       cropDownstreamExpression=expression2;
                   } else { // direction1 is "downstream" and direction2 is "upstream" 
                       cropUpstreamExpression=expression2;
                       cropDownstreamExpression=expression1;                   
                   }
               } else { // only one direction or no directionality?
                   pattern=Pattern.compile("^"+operationName+"( (in )?([a-zA-Z_0-9-]+))? by ([a-zA-Z_0-9-]+)( bp)?( from (upstream end|downstream end|start|end))?(\\s*\\S.*)?");
                   matcher=pattern.matcher(command);
                   if (matcher.find()) {
                       //for (int i=0;i<=matcher.groupCount();i++) System.err.println("#2: Group["+i+"]=>"+matcher.group(i));
                       sequenceCollection=matcher.group(3);
                       String expression=matcher.group(4);
                       String direction=matcher.group(7);
                       String unknown=matcher.group(8);
                       if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
                       if (expression==null)  throw new ParseError("Missing operand for specifying number of bp to crop");  
                       if (direction==null || direction.trim().isEmpty()) {
                           cropExpression=expression.trim();
                           cropUpstreamExpression=null;
                           cropDownstreamExpression=null;
                           useRelativeOrientation=false; // this is not really needed
                       } else { // direction is specified
                           if (direction.equals("upstream end")) {
                               cropUpstreamExpression=expression.trim();
                               cropDownstreamExpression=null;
                               cropExpression=null;
                               useRelativeOrientation=true;
                           } else if (direction.equals("start")) {
                               cropUpstreamExpression=expression.trim();
                               cropDownstreamExpression=null;
                               cropExpression=null;
                               useRelativeOrientation=false;
                           } else if (direction.equals("downstream end")) {
                               cropDownstreamExpression=expression.trim();
                               cropUpstreamExpression=null;
                               cropExpression=null;
                               useRelativeOrientation=true;
                           } else if (direction.equals("end")) {
                               cropDownstreamExpression=expression.trim();
                               cropUpstreamExpression=null;
                               cropExpression=null;
                               useRelativeOrientation=false;
                           } else throw new ParseError("Unrecognized direction: "+direction);                
                       }                                        
                   } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
               } 
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

           if (cropExpression!=null) cropExpression=removeBPsuffix(cropExpression);
           if (cropUpstreamExpression!=null) cropUpstreamExpression=removeBPsuffix(cropUpstreamExpression);
           if (cropDownstreamExpression!=null) cropDownstreamExpression=removeBPsuffix(cropDownstreamExpression);
           
           if (cropUpstreamExpression!=null && cropDownstreamExpression!=null && cropUpstreamExpression.equals(cropDownstreamExpression)) {
               // same expression in both directions, just use a common expression
               cropExpression=cropUpstreamExpression;
               cropUpstreamExpression=null;
               cropDownstreamExpression=null;
               useRelativeOrientation=false;
           }
           task.setParameter(Operation_crop_sequences.CROP_TO_REGIONS,cropToRegionsExpression);                    
           task.setParameter(Operation_crop_sequences.CROP_EXPRESSION,cropExpression);                    
           task.setParameter(Operation_crop_sequences.CROP_UPSTREAM_EXPRESSION,cropUpstreamExpression);                    
           task.setParameter(Operation_crop_sequences.CROP_DOWNSTREAM_EXPRESSION,cropDownstreamExpression);                    
           task.setParameter(Operation_crop_sequences.USE_RELATIVE_ORIENTATION,useRelativeOrientation);                    
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           task.setTurnOffGUInotifications(true); // this will also update the visualization properly afterwards
           return task;        
    }
    
    private String removeBPsuffix(String string) {
        if (string.endsWith(" bp") && string.length()>3) return string.substring(0,string.lastIndexOf(" bp"));
        else return string;
    }

}
