/*
 
 
 */

package org.motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.operations.Condition;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.operations.Operation_statistic;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_statistic extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
         
        String statisticFunction=(String)task.getParameter(Operation_statistic.STATISTIC_FUNCTION);   
        String propertyName=(String)task.getParameter(Operation_statistic.REGION_DATASET_PROPERTY);       
        String strand=(String)task.getParameter(Operation_statistic.STRAND);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (propertyName!=null && !propertyName.isEmpty()) statisticFunction+=(" "+propertyName);
        String msg="";
        if (!sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="statistic \""+statisticFunction+"\" in "+sourceName;
        if (strand!=null) msg+=" on "+strand;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    /** This method returns almost the same string as getCommandString() except without the 'X = statistic' at the beginning */
    public String getStatisticCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String statisticFunction=(String)task.getParameter(Operation_statistic.STATISTIC_FUNCTION);
        String propertyName=(String)task.getParameter(Operation_statistic.REGION_DATASET_PROPERTY);          
        String strand=(String)task.getParameter(Operation_statistic.STRAND);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (propertyName!=null && !propertyName.isEmpty()) statisticFunction+=(" "+propertyName);
        String msg="\""+statisticFunction+"\" in "+sourceName;
        if (strand!=null) msg+=" on "+strand;
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+getCommandString_condition(condition);
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="statistic";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String statisticFunction=null;
           String propertyString=null;
           String strandString=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?statistic\\s+\"(\\S.+?)\" in ([a-zA-Z_0-9-]+)?( on (\\S+ strand))?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               statisticFunction=matcher.group(3).trim();
               sourceName=matcher.group(4);
               strandString=matcher.group(6);
               whereString=matcher.group(8);
               String unknown = matcher.group(9);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");

           if (statisticFunction==null || statisticFunction.isEmpty()) throw new ParseError("Missing statistic function specification");
           if (sourceclass==RegionDataset.class) {
               String[] pair=Operation_statistic.getStatisticAndPropertyFromInput(statisticFunction);
               if (pair!=null) {
                   statisticFunction=pair[0];
                   propertyString=pair[1].trim();
                   if (propertyString.isEmpty()) throw new ParseError("Missing required property name");
               }           
           }
           if (!Operation_statistic.isKnownFunction(statisticFunction)) throw new ParseError("Unrecognized statistic function '"+statisticFunction+"'");
           if (strandString!=null && !strandString.isEmpty() && !Operation_statistic.isKnownStrand(strandString)) throw new ParseError("Unrecognized strand '"+strandString+"'");
           if (targetName==null || targetName.isEmpty()) throw new ParseError("SYSTEM ERROR: Missing required target data object for '"+operationName+"'"); 

           if (whereString!=null) {
               if (sourceclass==NumericDataset.class || sourceclass==DNASequenceDataset.class) parseWherePositionCondition(whereString,task);
               else if (sourceclass==RegionDataset.class) parseWhereRegionCondition(whereString,task);
           }
           if (sourceclass!=DNASequenceDataset.class) strandString=null; // this is not needed

           lookup.register(targetName, SequenceNumericMap.class);
           task.addAffectedDataObject(targetName, SequenceNumericMap.class);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_statistic.STATISTIC_FUNCTION,statisticFunction);
           if (strandString!=null && !strandString.isEmpty()) task.setParameter(Operation_statistic.STRAND,strandString);
           else task.removeParameter(Operation_statistic.STRAND);
           if (propertyString!=null && !propertyString.isEmpty()) task.setParameter(Operation_statistic.REGION_DATASET_PROPERTY,propertyString);
           else task.removeParameter(Operation_statistic.REGION_DATASET_PROPERTY);           
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           else task.removeParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
           return task;
    }


    /** Parses statistical function to be used by e.g. SequenceCollection creation */
    public OperationTask parseInternal(String command) throws ParseError {
           String operationName="statistic";
           String sourceName=null;
           String sequenceCollection=null;
           String statisticFunction=null;
           String propertyString=null;
           String strandString=null;
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^\"(\\S.+?)\" in ([a-zA-Z_0-9-]+)?( on (\\S+ strand))?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               statisticFunction=matcher.group(1).trim();
               sourceName=matcher.group(2);
               strandString=matcher.group(4);
               whereString=matcher.group(6);
               String unknown = matcher.group(7);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (strandString!=null && !strandString.isEmpty() && !Operation_statistic.isKnownStrand(strandString)) throw new ParseError("Unrecognized strand '"+strandString+"'");
           Data sourceData=engine.getDataItem(sourceName);
           if (sourceData==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceData)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");

           if (!(sourceData instanceof DNASequenceDataset)) strandString=null; // not needed
           if (statisticFunction==null || statisticFunction.isEmpty()) throw new ParseError("Missing statistic function specification");
           if (sourceData instanceof RegionDataset) {
               String[] pair=Operation_statistic.getStatisticAndPropertyFromInput(statisticFunction);
               if (pair!=null) {
                   statisticFunction=pair[0];
                   propertyString=pair[1].trim();
                   if (propertyString.isEmpty()) throw new ParseError("Missing required property name");
               }           
           }
           if (!Operation_statistic.isKnownFunction(statisticFunction)) throw new ParseError("Unrecognized statistic function '"+statisticFunction+"'");        
           if (whereString!=null) {
               if (sourceData instanceof NumericDataset || sourceData instanceof DNASequenceDataset) parseWherePositionCondition(whereString,task);
               else if (sourceData instanceof RegionDataset) parseWhereRegionCondition(whereString,task);
           }
           
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_statistic.STATISTIC_FUNCTION,statisticFunction);
           if (strandString!=null && !strandString.isEmpty()) task.setParameter(Operation_statistic.STRAND,strandString);
           else task.removeParameter(Operation_statistic.STRAND);
           if (propertyString!=null && !propertyString.isEmpty()) task.setParameter(Operation_statistic.REGION_DATASET_PROPERTY,propertyString);
           else task.removeParameter(Operation_statistic.REGION_DATASET_PROPERTY);              
           task.setParameter(OperationTask.TARGET_NAME, "temp");
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           else task.removeParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
           return task;
    }

}
