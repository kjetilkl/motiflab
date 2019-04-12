/*
 
 
 */

package motiflab.engine.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.operations.Condition;
import motiflab.engine.operations.Operation;
import motiflab.engine.operations.Operation_search;

/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_search extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();         
        String searchExpressionString=(String)task.getParameter(Operation_search.SEARCH_EXPRESSION);
        boolean searchRepeat=false;
        if (searchExpressionString.equals(Operation_search.SEARCH_REPEAT)) {
            searchRepeat=true;
            Integer minHalfsiteLength=(Integer)task.getParameter(Operation_search.MIN_HALFSITE_LENGTH);
            Integer maxHalfsiteLength=(Integer)task.getParameter(Operation_search.MAX_HALFSITE_LENGTH);
            Integer minGapLength=(Integer)task.getParameter(Operation_search.MIN_GAP_LENGTH);
            Integer maxGapLength=(Integer)task.getParameter(Operation_search.MAX_GAP_LENGTH);
            String direction=(String)task.getParameter(Operation_search.SEARCH_REPEAT_DIRECTION);
            String reportSite=(String)task.getParameter(Operation_search.REPORT_SITE);
            searchExpressionString=direction+" repeats {halfsite=["+minHalfsiteLength.intValue()+","+maxHalfsiteLength.intValue()+"], gap=["+minGapLength.intValue()+","+maxGapLength.intValue()+"], report="+reportSite+"}";
        }
        Integer mismatches=(Integer)task.getParameter(Operation_search.MISMATCHES);        
        String searchStrand=(searchRepeat)?null:(String)task.getParameter(Operation_search.SEARCH_STRAND);
        String subset=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);        
        String msg="";
        if (sourceName!=null && targetName!=null && !sourceName.equalsIgnoreCase(targetName))  msg=targetName+" = ";
        msg+="search "+sourceName+" for "+searchExpressionString;
        if (searchStrand!=null) msg+=" on "+searchStrand;
        if (mismatches!=null && mismatches.intValue()>0) {
            if (mismatches.intValue()==1) msg+=" with 1 mismatch";
            else msg+=" with "+mismatches.intValue()+" mismatches";
        }
        Condition condition=(Condition)task.getParameter("where");
        if (condition!=null) msg+=" where "+ getCommandString_condition(condition);
        if (subset!=null && !subset.equals(engine.getDefaultSequenceCollectionName())) msg+=" in collection "+subset;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="search";
           String sourceName=null;
           String targetName=null;
           String sequenceCollection=null;
           String searchExpressionString=null;
           String whereString=null;
           String strandString=null;
           String mismatchNumber=null;
           String minHalfsiteNumber=null;
           String maxHalfsiteNumber=null;
           String minGapNumber=null;
           String maxGapNumber=null;
           String repeatDirection=null;
           String reportSiteString=null;
           String[] splitOn=command.split(" in collection ");
           if (splitOn.length==2 && !splitOn[1].isEmpty()) sequenceCollection=splitOn[1].trim();       
           if (sequenceCollection!=null) {
             String[] splitOn2=sequenceCollection.split(" ");
             if (splitOn2.length>1) throw new ParseError("Unrecognized clause (or wrong order): "+splitOn2[1]);
           }
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?search ([a-zA-Z_0-9-]+)? for (direct|inverted) repeats\\s*\\{\\s*halfsite\\s*=\\s*\\[\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\]\\s*,\\s*gap\\s*=\\s*\\[\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\]\\s*,\\s*report\\s*=\\s*(\\w+)\\s*\\}( with (\\w+) mismatch(?:es)?)?( where (.+))?(\\s*\\S.*)?");

           Matcher matcher=pattern.matcher(splitOn[0]);
           if (matcher.find()) {
               targetName=matcher.group(2);
               sourceName=matcher.group(3);
               searchExpressionString=Operation_search.SEARCH_REPEAT;
               repeatDirection=matcher.group(4);
               minHalfsiteNumber=matcher.group(5);
               maxHalfsiteNumber=matcher.group(6);
               minGapNumber=matcher.group(7);
               maxGapNumber=matcher.group(8);
               reportSiteString=matcher.group(9);
               mismatchNumber=matcher.group(11);
               whereString=matcher.group(13);
               String unknown = matcher.group(14);
               if (reportSiteString!=null && !(reportSiteString.equals(Operation_search.REPORT_SITE_FULL) || reportSiteString.equals(Operation_search.REPORT_SITE_HALFSITE))) throw new ParseError("The value for 'report' should be either '"+Operation_search.REPORT_SITE_FULL+"' or '"+Operation_search.REPORT_SITE_HALFSITE+"'");
               // for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               if (unknown!=null && !unknown.isEmpty()) throw new ParseError("In search for repeats: unrecognized clause (or wrong order): "+unknown);
            } else { // search for non-palindromes
               pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?search ([a-zA-Z_0-9-]+)?( for (\\S+))?( on (\\w+ strands?))?( with (\\w+) mismatch(?:es)?)?( where (.+))?(\\s*\\S.*)?");               
               matcher=pattern.matcher(splitOn[0]);
               if (matcher.find()) {
                   targetName=matcher.group(2);
                   sourceName=matcher.group(3);
                   searchExpressionString=matcher.group(5);
                   strandString=matcher.group(7);
                   mismatchNumber=matcher.group(9);
                   whereString=matcher.group(11);
                   String unknown = matcher.group(12);
                   if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);
                   //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
               } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           }
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName==null || sourceName.isEmpty()) throw new ParseError("Missing source data for "+operationName+" operation");
           if (searchExpressionString==null || searchExpressionString.isEmpty()) throw new ParseError("Empty search expression");
           if (whereString!=null && !searchExpressionString.isEmpty()) parseWherePositionCondition(whereString,task);
           if (targetName==null || targetName.isEmpty()) targetName=sourceName;           

           Class sourceclass=lookup.getClassFor(sourceName);
           if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
           if (!operation.canUseAsSource(sourceclass)) throw new ParseError("'"+sourceName+"' is of a type not supported by operation '"+operationName+"'");
           lookup.register(targetName, RegionDataset.class);
           task.addAffectedDataObject(sourceName, RegionDataset.class);
           if (strandString==null) strandString=Operation_search.STRAND_BOTH;
           if (!(strandString.equals(Operation_search.STRAND_BOTH) 
               || strandString.equals(Operation_search.STRAND_GENE) 
               || strandString.equals(Operation_search.STRAND_OPPOSITE)
               || strandString.equals(Operation_search.STRAND_DIRECT) 
               || strandString.equals(Operation_search.STRAND_REVERSE))) throw new ParseError("Unrecognized strand specification: '"+strandString+"'. Permitted values = "+Operation_search.STRAND_BOTH+"|"+Operation_search.STRAND_GENE+"|"+Operation_search.STRAND_OPPOSITE+"|"+Operation_search.STRAND_DIRECT+"|"+Operation_search.STRAND_REVERSE);
           if (mismatchNumber!=null) {
               try {
                   int mismathces=Integer.parseInt(mismatchNumber);
                   task.setParameter(Operation_search.MISMATCHES, new Integer(mismathces));
               } catch (NumberFormatException nfe) {throw new ParseError("Number of mismatches must be a constant integer number");}
           }
           int minhalfsiteLength=0;
           if (minHalfsiteNumber!=null) {
               try {
                   minhalfsiteLength=Integer.parseInt(minHalfsiteNumber);
                   if (minhalfsiteLength<3) throw new ParseError("Minimum halfsite size must be a constant integer number (minimum 3)");
                   task.setParameter(Operation_search.MIN_HALFSITE_LENGTH, new Integer(minhalfsiteLength));
               } catch (NumberFormatException nfe) {throw new ParseError("Minimum halfsite size must be a constant integer number (minimum 3)");}
           }
           if (maxHalfsiteNumber!=null) {
               try {
                   int maxhalfsiteLength=Integer.parseInt(maxHalfsiteNumber);
                   task.setParameter(Operation_search.MAX_HALFSITE_LENGTH, new Integer(maxhalfsiteLength));
                   if (maxhalfsiteLength<minhalfsiteLength) throw new ParseError("Maximum halfsite size must be equal to or greater than minimum halfsite size");
               } catch (NumberFormatException nfe) {throw new ParseError("Maximum halfsite size must be a constant integer number");}
           }
           int minGap=0;
           if (minGapNumber!=null) {
               try {
                   minGap=Integer.parseInt(minGapNumber);
                   task.setParameter(Operation_search.MIN_GAP_LENGTH, new Integer(minGap));
                   if (minhalfsiteLength<0) throw new ParseError("Minimum halfsite size must be a constant integer number (minimum 0)");
               } catch (NumberFormatException nfe) {throw new ParseError("Minimum gap size must be a constant integer number (minimum 0)");}
           }
           if (maxGapNumber!=null) {
               try {
                   int maxGap=Integer.parseInt(maxGapNumber);
                   task.setParameter(Operation_search.MAX_GAP_LENGTH, new Integer(maxGap));
                   if (maxGap<minGap) throw new ParseError("Maximum gap size must be equal to or greater than minimum gap size");
               } catch (NumberFormatException nfe) {throw new ParseError("Maximum gap size must be a constant integer number");}
           }
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);           
           task.setParameter(Operation_search.SEARCH_EXPRESSION, searchExpressionString);
           task.setParameter(Operation_search.SEARCH_STRAND, strandString);
           task.setParameter(Operation_search.SEARCH_REPEAT_DIRECTION, repeatDirection);
           task.setParameter(Operation_search.REPORT_SITE, reportSiteString);
           if (sequenceCollection!=null && !sequenceCollection.isEmpty()) task.setParameter(OperationTask.SEQUENCE_COLLECTION_NAME, sequenceCollection);
           return task;
    }

}
