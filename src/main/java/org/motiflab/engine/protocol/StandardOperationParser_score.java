/*
 
 
 */

package org.motiflab.engine.protocol;


import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_score;
import org.motiflab.engine.operations.Operation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.operations.Condition;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_score extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
         
        String motifName=(String)task.getParameter(Operation_score.MOTIFNAME); 
        String rawOrLogodds=(String)task.getParameter(Operation_score.RAW_OR_LOGLIKELIHOOD);
        String scoreString=(String)task.getParameter(Operation_score.SCORE);
        String strand=(String)task.getParameter(Operation_score.STRAND);    
        String backgroundModel=(String)task.getParameter(Operation_score.BACKGROUNDMODEL);   
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        StringBuilder msg=new StringBuilder();
        if (!sourceName.equalsIgnoreCase(targetName))  {
            msg.append(targetName);
            msg.append(" = ");
        }
        msg.append("score ");
        msg.append(sourceName);
        msg.append(" with ");
        msg.append(motifName);
        msg.append(" using ");
        msg.append(scoreString);
        msg.append(" ");
        msg.append(rawOrLogodds);
        msg.append(" scores");
        if (backgroundModel!=null) {
           msg.append(" against "); 
           msg.append(backgroundModel);
        }
        msg.append(" on ");
        msg.append(strand);
        msg.append(" strand");
        if (strand.equals(Operation_score.STRAND_BOTH)) msg.append("s");
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) {
            msg.append(" where "); 
            msg.append(getCommandString_condition(condition));
        }      
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) {
            msg.append(" in collection ");
            msg.append(subset);
        }
        return msg.toString();
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="score";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String motifName=null;
           String scoreString=null;
           String rawOrLogOddsString=null;
           String strand=null;
           String backgroundModel=null;
           String whereString=null;          
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           String scorePattern=Operation_score.SCORE_ABSOLUTE+"|"+Operation_score.SCORE_RELATIVE;
           String raworlogoddsPattern=Operation_score.RAW+"|"+Operation_score.LOGLIKELIHOOD;
           //String strandPattern=Operation_score.STRAND_SINGLE+"|"+Operation_score.STRAND_BOTH+"|"+Operation_score.STRAND_DIRECT+"|"+Operation_score.STRAND_REVERSE+"|"+Operation_score.STRAND_RELATIVE+"|"+Operation_score.STRAND_OPPOSITE;
           
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?score ([a-zA-Z_0-9-]+)?( with (\\S+))?( using)?(( ("+scorePattern+"))?( ("+raworlogoddsPattern+"))? scores)?( against ([a-zA-Z_0-9-]+))?( on (\\S+) strands?)?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               motifName=matcher.group(5);
               scoreString=matcher.group(9);
               rawOrLogOddsString=matcher.group(11);
               backgroundModel=matcher.group(13);
               strand=matcher.group(15);
               whereString=matcher.group(17);
               String unknown = matcher.group(18);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));              
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for score operation");
           if (motifName==null || motifName.isEmpty()) throw new ParseError("Missing motif specification");
           if (scoreString==null || scoreString.isEmpty()) scoreString=Operation_score.SCORE_ABSOLUTE;
           if (rawOrLogOddsString==null || rawOrLogOddsString.isEmpty()) rawOrLogOddsString=Operation_score.RAW;
           if (strand==null || strand.isEmpty()) strand=Operation_score.STRAND_BOTH;
           if (!(   strand.equals(Operation_score.STRAND_SINGLE)
                 || strand.equals(Operation_score.STRAND_BOTH)
                 || strand.equals(Operation_score.STRAND_DIRECT)
                 || strand.equals(Operation_score.STRAND_REVERSE)
                 || strand.equals(Operation_score.STRAND_RELATIVE)
                 || strand.equals(Operation_score.STRAND_OPPOSITE)
                   )) throw new ParseError("Unrecognized strand: "+strand);
           if (whereString!=null && !motifName.isEmpty()) parseWherePositionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=DNASequenceDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object has wrong type!");
           lookup.register(targetName, NumericDataset.class);
           task.addAffectedDataObject(sourceName, NumericDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_score.MOTIFNAME, motifName);
           task.setParameter(Operation_score.SCORE, scoreString);
           task.setParameter(Operation_score.RAW_OR_LOGLIKELIHOOD, rawOrLogOddsString);
           task.setParameter(Operation_score.STRAND, strand);
           if (backgroundModel!=null && !backgroundModel.isEmpty()) task.setParameter(Operation_score.BACKGROUNDMODEL, backgroundModel);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }


}