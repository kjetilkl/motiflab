/*
 
 
 */

package org.motiflab.engine.protocol;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DataCollection;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.operations.Condition;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.operations.ArithmeticOperation;


/**
 * This class represents common code for parsers of the operations: increase,decrease,weight,divide
 * since the command-strings for these operations are basically the same (except for the name of
 * the operation itself, but that is obtained by the getOperationName() method which is overriden
 * in the subclasses for these individual operations).
 * @author kjetikl
 */
public abstract class StandardArithmeticsOperationParser extends StandardOperationParser {

    public abstract String getOperationName();

    /** This method should return the preposition which is associated
     *  with this operation in common language terms. E.g. increase BY or set TO
     */
    public abstract String getPreposition();

    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();

        String operandDatastring=(String)task.getParameter(ArithmeticOperation.OPERAND_STRING);
        String regionOperator=(String)task.getParameter(ArithmeticOperation.REGION_OPERATOR);
        String subset=(String)task.getParameter(ArithmeticOperation.DATA_COLLECTION_NAME);
        String propertyName=(String)task.getParameter(ArithmeticOperation.PROPERTY_NAME);
        StringBuilder msg=new StringBuilder();
        if (!sourceName.equalsIgnoreCase(targetName))   {
            msg.append(targetName);
            msg.append(" = ");
        }
        msg.append(getOperationName());
        msg.append(' ');
        msg.append(sourceName);
        if (propertyName!=null) {
            msg.append('[');
            msg.append(propertyName);
            msg.append(']');
        }
        msg.append(' ');
        msg.append(getPreposition());
        msg.append(' ');
        if (regionOperator!=null && !regionOperator.isEmpty()) {
          msg.append(regionOperator);
          msg.append(' ');
        }
        msg.append(operandDatastring);
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
           String operationName=getOperationName();
           String preposition=getPreposition();
           String sourceName=null;
           String targetName=null;
           String dataCollectionName=null;
           String regionoperator=null;
           String propertyName=null;
           String operandValueString=null; // the value by which the source data objecte should be weighted/divided/increased/decreased
           String whereString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) dataCollectionName=splitOn[1].trim();
           if (dataCollectionName!=null) {
             String[] splitOn2=dataCollectionName.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           // note: I have excluded "weighted min/minimum", "weighted max/maximum" and "weighted median" from this list even though the operation supports it, since I am not sure if it makes any sense to include them          
           String operators="min|minimum|max|maximum|average|median|sum|weighted average|weighted sum|startValue|endValue|relativeStartValue|relativeEndValue|regionStartValue|regionEndValue|centerValue";
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?"+operationName+" (([a-zA-Z_0-9-]+)(\\[(.+?)\\]|\\s+property\\s+\"(.+?)\")?)?( "+preposition+"( ("+operators+"))? (\".+?\"|region\\[.+?\\]|\\S+))?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(4);
               propertyName=matcher.group(6);
               if (propertyName==null) propertyName=matcher.group(7); // either way is fine 
               if (propertyName!=null) {
                   propertyName=propertyName.trim();
                   if (propertyName.startsWith("\"") || propertyName.startsWith("\'")) propertyName=propertyName.substring(1);
                   if (propertyName.endsWith("\"") || propertyName.endsWith("\'")) propertyName=propertyName.substring(0,propertyName.length()-1);                  
               }
               regionoperator=matcher.group(10);
               operandValueString=matcher.group(11);
               whereString=matcher.group(13);
               String unknown = matcher.group(14);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (operandValueString==null || operandValueString.isEmpty()) throw new ParseError("Missing operand value for "+operationName+" operation");
           if (operandValueString.startsWith("\"") && !operandValueString.endsWith("\"")) throw new ParseError("Unclosed quote starting with '"+operandValueString+"'");
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           Class operandclass=lookup.getClassFor(operandValueString);
           if (sourceclass==RegionDataset.class && operandclass==NumericDataset.class) {
               if (regionoperator==null || regionoperator.isEmpty()) throw new ParseError("Missing operator which specifies what value from numeric dataset to use in relation to region");
           } else if (regionoperator!=null && !regionoperator.isEmpty()) throw new ParseError("Operator '"+regionoperator+"' not applicable in this context");
           if (whereString!=null && !operandValueString.isEmpty()) {
               if (sourceclass==RegionDataset.class) parseWhereRegionCondition(whereString, task);
               else if (sourceclass==NumericDataset.class) parseWherePositionCondition(whereString,task);
           }
           if ((propertyName!=null && !propertyName.isEmpty()) && !(sourceclass==RegionDataset.class || sourceclass==Motif.class || sourceclass==ModuleCRM.class || sourceclass==Sequence.class || DataCollection.class.isAssignableFrom(sourceclass))) throw new ParseError(engine.getTypeNameForDataClass(sourceclass)+" '"+sourceName+"' can not have a property named '"+propertyName+"'");
           if (sourceclass!=RegionDataset.class && operandValueString.startsWith("region[")) throw new ParseError(engine.getTypeNameForDataClass(sourceclass)+" '"+sourceName+"' can not be modified by '"+operandValueString+"'");
           
           lookup.register(targetName, sourceclass);
           task.addAffectedDataObject(targetName, sourceclass);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(ArithmeticOperation.OPERAND_STRING, operandValueString);
           task.setParameter(ArithmeticOperation.REGION_OPERATOR, regionoperator);
           task.setParameter(ArithmeticOperation.PROPERTY_NAME, propertyName);
           if (dataCollectionName!=null && !dataCollectionName.isEmpty()) task.setParameter(ArithmeticOperation.DATA_COLLECTION_NAME, dataCollectionName);
           return task;
    }

}
