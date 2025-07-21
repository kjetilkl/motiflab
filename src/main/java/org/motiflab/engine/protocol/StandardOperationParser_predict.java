/*
 
 
 */

package org.motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.NumericDataset;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_predict extends StandardOperationParser {



    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String priorsGeneratorName=task.getSourceDataName();
        String msg=targetName+" = predict with "+priorsGeneratorName;
//        if (parameters!=null && !parameters.isEmpty()) msg+=" {"+parameters+"}";
//        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }


    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="predict";
           String priorsGeneratorName=null;
           String targetName=null;
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?predict with ([a-zA-Z_0-9-]+)(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               targetName=matcher.group(2);
               priorsGeneratorName=matcher.group(3);
               String unknown=matcher.group(4);
               if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing target dataset for operation 'predict'");
               if (priorsGeneratorName==null || priorsGeneratorName.isEmpty()) throw new ParseError("Missing Priors Generator specification for operation 'predict'");
               if (unknown!=null && !unknown.isEmpty()) {
                   throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }
           } else throw new ParseError("Unable to parse command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           Class sourceclass=lookup.getClassFor(priorsGeneratorName);
           if (sourceclass==null) throw new ParseError("Unrecognized data object: "+priorsGeneratorName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+priorsGeneratorName+"' is not a Priors Generator");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=RegionDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object exists and is not a Region Dataset!");
           lookup.register(targetName, NumericDataset.class);
           task.addAffectedDataObject(targetName, NumericDataset.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.SOURCE_NAME, priorsGeneratorName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);

           return task;
    }



}
