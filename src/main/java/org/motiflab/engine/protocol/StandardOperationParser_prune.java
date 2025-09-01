/*
 
 
 */

package org.motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Condition;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_prune;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_prune extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        StringBuilder msg=new StringBuilder();
        if (!sourceName.equalsIgnoreCase(targetName)) {
            msg.append(targetName);
            msg.append(" = ");
        }
        msg.append("prune ");
        msg.append(sourceName);

        msg.append(" remove \"");
        String pruneString=(String)task.getParameter(Operation_prune.PRUNE);
        msg.append(pruneString);
        msg.append("\"");
        String partitionName=(String)task.getParameter(Operation_prune.MOTIFPARTITION);
        boolean usePartition=(pruneString.equals(Operation_prune.PRUNE_ALTERNATIVES) || pruneString.equals(Operation_prune.PRUNE_ALTERNATIVES_NAIVE));
        if (usePartition && partitionName!=null) {
            msg.append(" from ");
            msg.append(partitionName);
        }
        String keepString=(String)task.getParameter(Operation_prune.KEEP);
        if (keepString!=null && !keepString.isEmpty()) {
            msg.append(" keep \"");
            msg.append(keepString);
            msg.append("\"");
        }
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
           String operationName="prune";
           String sourceName=null;
           String targetName=null;
           String pruneOption=null;
           String motifpartition=null;
           String keepOption=null;
           String sequenceCollection=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?prune ([a-zA-Z_0-9-]+) remove \"(.+?)\"( from ([a-zA-Z_0-9-]+))?( keep \"(.+?)\")?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               pruneOption=matcher.group(4);
               motifpartition=matcher.group(6);
               keepOption=matcher.group(8);
               whereString=matcher.group(10);
               String unknown = matcher.group(11);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for prune operation");
           if (whereString!=null) parseWhereRegionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           if (keepOption!=null && keepOption.isEmpty()) keepOption=null;
           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           String[] pruneOptions=Operation_prune.getPruneOptions();
           if (!isIn(pruneOption, pruneOptions)) throw new ParseError("Unrecognized 'remove' option: " +pruneOption);
           if (keepOption!=null) {
              String[] keepOptions=Operation_prune.getKeepOptions(pruneOption);
              if (!isIn(keepOption, keepOptions)) throw new ParseError("Unrecognized 'keep' option for 'remove "+pruneOption+"': " +keepOption);
           }
           
           lookup.register(targetName, RegionDataset.class);
           task.addAffectedDataObject(targetName, RegionDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_prune.PRUNE, pruneOption);
           task.setParameter(Operation_prune.KEEP, keepOption);
           boolean usePartition=(pruneOption.equals(Operation_prune.PRUNE_ALTERNATIVES) || pruneOption.equals(Operation_prune.PRUNE_ALTERNATIVES_NAIVE));
           task.setParameter(Operation_prune.MOTIFPARTITION, (usePartition)?motifpartition:null);
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }

    private boolean isIn(String target, String[] options) {
        for (String option:options) {
            if (target.equals(option)) return true;
        }
        return false;
    }

}


