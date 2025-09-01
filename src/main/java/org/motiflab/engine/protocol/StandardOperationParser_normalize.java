/*
 
 
 */

package org.motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.operations.Condition;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Operation_normalize;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_normalize extends StandardOperationParser{


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String propertyName=(String)task.getParameter(Operation_normalize.PROPERTY_NAME);        
        String msg="";         
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="normalize "+sourceName;
        if (propertyName!=null) {
            msg+=("["+propertyName+"]");
        }          
        String mode=(String)task.getParameter(Operation_normalize.MODE);
        if (mode==null) mode=Operation_normalize.SUM_TO_ONE;
        if (mode.equals(Operation_normalize.SUM_TO_ONE)) {
            msg+=" sequence sum to one";            
        } else {
            String oldMinString=(String)task.getParameter(Operation_normalize.OLD_MIN);        
            String oldMaxString=(String)task.getParameter(Operation_normalize.OLD_MAX);        
            String newMinString=(String)task.getParameter(Operation_normalize.NEW_MIN);        
            String newMaxString=(String)task.getParameter(Operation_normalize.NEW_MAX);        

            String parstring="from range ["+oldMinString+","+oldMaxString+"] to range ["+newMinString+","+newMaxString+"]";
            msg+=" "+parstring;
            Condition condition=(Condition)task.getParameter("where");
            if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        }
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="normalize";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String rangeString=null;
           String oldMinString=null;
           String oldMaxString=null;
           String newMinString=null;
           String newMaxString=null;
           String propertyName=null;
           String whereString=null;
           String mode=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?normalize (([a-zA-Z_0-9-]+)(\\[(.+?)\\]|\\s+property\\s+\"(.+?)\")?)?( sequence)? sum to one(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {      
               mode=Operation_normalize.SUM_TO_ONE;               
               targetName=matcher.group(2);
               sourceName=matcher.group(4);
               propertyName=matcher.group(6);
               if (propertyName==null) propertyName=matcher.group(7); // either way is fine 
               if (propertyName!=null) {
                   propertyName=propertyName.trim();
                   if (propertyName.startsWith("\"") || propertyName.startsWith("\'")) propertyName=propertyName.substring(1);
                   if (propertyName.endsWith("\"") || propertyName.endsWith("\'")) propertyName=propertyName.substring(0,propertyName.length()-1);                  
               }
               String unknown = matcher.group(9);                                       
               // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);  
           } else {
               pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?normalize (([a-zA-Z_0-9-]+)(\\[(.+?)\\]|\\s+property\\s+\"(.+?)\")?)?( from range\\s*\\[\\s*([a-zA-Z_0-9-\\.]+)\\s*,\\s*([a-zA-Z_0-9-\\.]+)\\s*\\] to range\\s*\\[\\s*([a-zA-Z_0-9-\\.]+)\\s*,\\s*([a-zA-Z_0-9-\\.]+)\\s*\\])?( where (.+))?(\\s*\\S.*)?");
               matcher=pattern.matcher(splitOn[0]);
               if (matcher.find()) {
                   mode=Operation_normalize.NORMALIZE_TO_RANGE;                   
                   targetName=matcher.group(2);
                   sourceName=matcher.group(4);
                   propertyName=matcher.group(6);
                   if (propertyName==null) propertyName=matcher.group(7); // either way is fine 
                   if (propertyName!=null) {
                       propertyName=propertyName.trim();
                       if (propertyName.startsWith("\"") || propertyName.startsWith("\'")) propertyName=propertyName.substring(1);
                       if (propertyName.endsWith("\"") || propertyName.endsWith("\'")) propertyName=propertyName.substring(0,propertyName.length()-1);                  
                   }                                     
                   rangeString=matcher.group(8);
                   oldMinString=matcher.group(9);
                   oldMaxString=matcher.group(10);
                   newMinString=matcher.group(11);
                   newMaxString=matcher.group(12);
                   whereString=matcher.group(14);
                   String unknown = matcher.group(15);               
                   // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));                   
                   if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
               }        
               else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           }
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (mode.equals(Operation_normalize.NORMALIZE_TO_RANGE) && (rangeString==null || rangeString.isEmpty())) throw new ParseError("Missing correct range specification");
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;           

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           
           if (whereString!=null && !whereString.isEmpty()) {
               if (sourceclass==NumericDataset.class) parseWherePositionCondition(whereString,task);
               else if (sourceclass==RegionDataset.class) parseWhereRegionCondition(whereString,task);
           }
           if ((propertyName!=null && !propertyName.isEmpty()) && !(sourceclass==RegionDataset.class)) throw new ParseError(engine.getTypeNameForDataClass(sourceclass)+" '"+sourceName+"' can not have a property named '"+propertyName+"'");
           
           if (sourceclass==NumericDataset.class) {
               lookup.register(targetName, NumericDataset.class);
               task.addAffectedDataObject(targetName, NumericDataset.class);
           }  else if (sourceclass==RegionDataset.class) {
               lookup.register(targetName, RegionDataset.class);
               task.addAffectedDataObject(targetName, RegionDataset.class);
           }
           if (mode==null) mode=Operation_normalize.SUM_TO_ONE;
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_normalize.MODE, mode);
           task.setParameter(Operation_normalize.OLD_MIN, oldMinString);
           task.setParameter(Operation_normalize.OLD_MAX, oldMaxString);
           task.setParameter(Operation_normalize.NEW_MIN, newMinString);
           task.setParameter(Operation_normalize.NEW_MAX, newMaxString);
           task.setParameter(OperationTask.TARGET_NAME, targetName); 
           task.setParameter(Operation_normalize.PROPERTY_NAME, propertyName);            
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }
}
