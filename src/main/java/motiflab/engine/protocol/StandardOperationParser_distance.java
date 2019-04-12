/*
 
 
 */

package motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.operations.Operation_distance;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_distance extends StandardOperationParser {

    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String anchorPoint=(String)task.getParameter(Operation_distance.ANCHOR_POINT);
        String relativeAnchorPoint=(String)task.getParameter(Operation_distance.RELATIVE_ANCHOR_POINT);
        if (anchorPoint==null || anchorPoint.isEmpty()) anchorPoint="transcription start site";  
        String msg=targetName+" = ";
        String direction=(String)task.getParameter(Operation_distance.DIRECTION);
        if (direction==null || direction.equals("both")) direction="";
        else direction+=" ";
        msg+="distance "+direction+"from "+anchorPoint;
        if (relativeAnchorPoint!=null && !relativeAnchorPoint.isEmpty()) msg+=" relative to "+relativeAnchorPoint;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="distance";
           String direction=null;
           String targetName=null;
           String anchorPoint=null;
           String relativeAnchorPoint=null;
           String sequenceCollection=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?distance\\s*(upstream|downstream)? from (transcription start site|transcription end site|sequence upstream end|sequence downstream end|[a-zA-Z_0-9\\-]+)( relative to (transcription start site|transcription end site|sequence upstream end|sequence downstream end|chromosome start))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               direction=matcher.group(3);
               anchorPoint=matcher.group(4);
               relativeAnchorPoint = matcher.group(6);
               String unknown = matcher.group(7);
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (targetName==null || targetName.isEmpty()) targetName="distanceTrack";           
           if (anchorPoint==null) throw new ParseError("Missing anchor point for operation '"+operationName+"'");
           if (relativeAnchorPoint!=null && relativeAnchorPoint.trim().isEmpty()) relativeAnchorPoint=null;
           
           lookup.register(targetName, NumericDataset.class);
           task.addAffectedDataObject(targetName, NumericDataset.class);

           if (direction==null || direction.isEmpty()) direction=Operation_distance.BOTH;
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
          // task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           task.setParameter(Operation_distance.DIRECTION, direction);           
           task.setParameter(Operation_distance.ANCHOR_POINT, anchorPoint);           
           task.setParameter(Operation_distance.RELATIVE_ANCHOR_POINT, relativeAnchorPoint);           
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }
}
