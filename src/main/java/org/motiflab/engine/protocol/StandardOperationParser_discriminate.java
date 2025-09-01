/*
 
 
 */

package org.motiflab.engine.protocol;


import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_discriminate;
import org.motiflab.engine.operations.Operation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.operations.Condition;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_discriminate extends StandardOperationParser {

    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
         
        String positiveSet=(String)task.getParameter(Operation_discriminate.POSITIVE_SET); 
        String negativeSet=(String)task.getParameter(Operation_discriminate.NEGATIVE_SET);
        String dnaDataset=(String)task.getParameter(Operation_discriminate.DNA_SEQUENCE);
        String wordsize=(String)task.getParameter(Operation_discriminate.WORD_SIZE);    
        String orientation=(String)task.getParameter(Operation_discriminate.ORIENTATION);
        String anchor=(String)task.getParameter(Operation_discriminate.ANCHOR);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        StringBuilder msg=new StringBuilder();
        if (!sourceName.equalsIgnoreCase(targetName))  {
            msg.append(targetName);
            msg.append(" = ");
        }
        msg.append("discriminate ");
        msg.append(sourceName);
        msg.append(" in ");
        msg.append(positiveSet);
        msg.append(" from ");
        msg.append(negativeSet);
        msg.append(" based on words of size ");
        msg.append(wordsize);
        msg.append(" in ");
        msg.append(dnaDataset);
        if (orientation!=null) {
            msg.append(" on ");
            msg.append(orientation);
            msg.append(" strand");
            if (orientation.equals(Operation_discriminate.ORIENTATION_BOTH)) msg.append("s");
        }
        if (anchor!=null) {
            msg.append(" with anchor at ");
            msg.append(anchor);
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
           String operationName="discriminate";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String positiveSetName=null;
           String negativeSetName=null;
           String dnaSequenceDataset=null;
           String wordSizeString=null;
           String orientationString=null;
           String anchorString=null;
           String whereString=null;          
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           String strandPattern=Operation_discriminate.ORIENTATION_BOTH+"|"+Operation_discriminate.ORIENTATION_RELATIVE+"|"+Operation_discriminate.ORIENTATION_DIRECT;
           String anchorPattern=Operation_discriminate.ANCHOR_START+"|"+Operation_discriminate.ANCHOR_RELATIVE_START;

           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?discriminate ([a-zA-Z_0-9-]+)? in ([a-zA-Z_0-9-]+) from ([a-zA-Z_0-9-]+) based on words of size ([a-zA-Z_0-9-]+) in ([a-zA-Z_0-9-]+)( on ("+strandPattern+") strands?)?( with anchor at ("+anchorPattern+"))?( where (.+))?(\\s*\\S.*)?");
           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               positiveSetName=matcher.group(4);
               negativeSetName=matcher.group(5);
               wordSizeString=matcher.group(6);
               dnaSequenceDataset=matcher.group(7);
               orientationString=matcher.group(9);
               anchorString=matcher.group(11);
               whereString=matcher.group(13);
               String unknown = matcher.group(14);
               //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));              
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for score operation");
           if (positiveSetName==null || positiveSetName.isEmpty()) throw new ParseError("Missing name of sequence collection containing positive sequences");
           if (negativeSetName==null || negativeSetName.isEmpty()) throw new ParseError("Missing name of sequence collection containing negative sequences");
           if (wordSizeString==null || wordSizeString.isEmpty()) throw new ParseError("Missing specification of word size");
           if (orientationString==null || orientationString.isEmpty()) orientationString=null;
           if (anchorString==null || anchorString.isEmpty()) anchorString=null;
           if (whereString!=null && !sourceName.isEmpty()) parseWherePositionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;
           
           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           //Class oldclass=lookup.getClassFor(targetName);
           //if (oldclass!=null && oldclass!=DNASequenceDataset.class) throw new ParseError("Unable to output to "+targetName+". Target data object has wrong type!");
           lookup.register(targetName, NumericDataset.class);
           task.addAffectedDataObject(sourceName, NumericDataset.class);
           Class positiveSetClass=lookup.getClassFor(positiveSetName);
           if (positiveSetClass==null) throw new ParseError("Unrecognized data object: "+positiveSetName);
           if (positiveSetClass!=SequenceCollection.class) throw new ParseError("'"+positiveSetName+"' is not a Sequence Collection");
           Class negativeSetClass=lookup.getClassFor(negativeSetName);
           if (negativeSetClass==null) throw new ParseError("Unrecognized data object: "+negativeSetName);
           if (negativeSetClass!=SequenceCollection.class) throw new ParseError("'"+negativeSetName+"' is not a Sequence Collection");
           Class dnaDatasetClass=lookup.getClassFor(dnaSequenceDataset);
           if (dnaDatasetClass==null) throw new ParseError("Unrecognized data object: "+dnaSequenceDataset);
           if (dnaDatasetClass!=DNASequenceDataset.class) throw new ParseError("'"+dnaSequenceDataset+"' is not a DNA Sequence Dataset");
           Class wordSizeClass=lookup.getClassFor(wordSizeString);
           if (wordSizeClass==null) {
               try {
                   Integer.parseInt(wordSizeString);
               } catch (NumberFormatException e) {
                   throw new ParseError("The word size '"+wordSizeString+"' should be either an integer constant or a Numeric Variable");
               }
           } else if (wordSizeClass!=NumericVariable.class) throw new ParseError("The word size '"+wordSizeString+"' should be either an integer constant or a Numeric Variable");
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(Operation_discriminate.POSITIVE_SET, positiveSetName);
           task.setParameter(Operation_discriminate.NEGATIVE_SET, negativeSetName);
           task.setParameter(Operation_discriminate.WORD_SIZE, wordSizeString);
           task.setParameter(Operation_discriminate.DNA_SEQUENCE, dnaSequenceDataset);           
           if (orientationString!=null) task.setParameter(Operation_discriminate.ORIENTATION, orientationString);
           if (anchorString!=null) task.setParameter(Operation_discriminate.ANCHOR, anchorString);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }


}