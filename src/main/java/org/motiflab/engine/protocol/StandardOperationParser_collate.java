/*
 
 
 */

package org.motiflab.engine.protocol;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataMap;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.data.analysis.CollatedAnalysis;
import org.motiflab.engine.operations.Operation_collate;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_collate extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String[][] entries=(String[][])task.getParameter(Operation_collate.SOURCE_DATA);                   
        String optionalTitle=(String)task.getParameter(Operation_collate.OPTIONAL_TITLE);                   
        StringBuilder line=new StringBuilder();
        line.append(targetName);
        line.append(" = collate ");
        for (int i=0;i<entries.length;i++) {
            String[] entry=entries[i];
            if (i>0) line.append(", ");
            if (entry[1]!=null) {
                line.append("\"");
                line.append(entry[1]);
                line.append("\" from ");
            }
            line.append(entry[0]);
            if (entry[2]!=null){
               line.append(" as \""); 
               line.append(entry[2]);
               line.append("\"");               
            }              
        }
        if (optionalTitle!=null) {
            if (entries!=null && entries.length>0) line.append(", ");
            line.append("title=\"");
            line.append(optionalTitle);
            line.append("\"");
        }
        return line.toString();
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="collate";
           String targetName=null;
           String collatePattern=null;
           String optionalTitle=null;

           Pattern entrypattern=Pattern.compile("(\"(.+?)\"\\s+from\\s+)?([a-zA-Z_0-9-]+)(\\s+as\\s+\"(.+?)\")?(\\s*\\S.*)?");
           Pattern titlepattern=Pattern.compile("title\\s*=\\s*\"(.+?)\"(\\s*\\S.*)?");            
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?collate (.+)");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               collatePattern=matcher.group(3);
                //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing required target data object for collate operation");
           ArrayList<String> collateList=MotifLabEngine.splitOnCharacter(collatePattern.trim(),',','"','"');                                 
           String[][] list=new String[collateList.size()][3];
           for (int i=0;i<collateList.size();i++) {
               String entry=collateList.get(i).trim();
               if (entry.isEmpty()) throw new ParseError("Empty command part encountered");
               Matcher entrymatcher=titlepattern.matcher(entry); // try title first to avoid problems!
               if (entrymatcher.find()) {
                    if (entrymatcher.group(2)!=null && !entrymatcher.group(2).trim().isEmpty()) throw new ParseError("Parse error near '"+entrymatcher.group(2)+"' #1");
                    optionalTitle=entrymatcher.group(1).trim();
               } else {
                   entrymatcher=entrypattern.matcher(entry);
                   if (entrymatcher.find()) {
                       //for (int j=0;j<=matcher.groupCount();j++) System.err.println("#1:Group["+j+"]=>"+matcher.group(j));
                       if (entrymatcher.group(6)!=null && !entrymatcher.group(6).trim().isEmpty()) throw new ParseError("Parse error near '"+entrymatcher.group(6)+"' #2");
                       list[i][0]=entrymatcher.group(3); // analysis or Data type (e.g. Motif) or Numeric Map
                       list[i][1]=entrymatcher.group(2); // property (column)
                       list[i][2]=entrymatcher.group(5); // new column name
                       if (list[i][2]!=null && ((String)list[i][2]).trim().isEmpty()) list[i][2]=null;
                       if (entrymatcher.group(1)==null || entrymatcher.group(1).isEmpty()) list[i][1]=null; // no property specified. The data source should be a Numeric Map
                   } else throw new ParseError("Unable to parse entry: "+entry);
               }                           
           }    
           list=stripEmptyLines(list);
           Class collateType=checkSourceObjects(list,lookup); // checks the list of source analyses and throws ParseErrors if appropriate
           lookup.register(targetName, CollatedAnalysis.class);
           lookup.registerCollatedType(targetName, collateType);
           task.addAffectedDataObject(targetName, CollatedAnalysis.class);
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);          
           task.setParameter(OperationTask.TARGET_NAME, targetName);    
           task.setParameter(Operation_collate.SOURCE_DATA, list); 
           task.setParameter(Operation_collate.COLLATE_DATA_TYPE, collateType);
           if (optionalTitle!=null) task.setParameter(Operation_collate.OPTIONAL_TITLE, optionalTitle);
           return task;
    }
    
    private String[][] stripEmptyLines(String[][] list) {
        int count=0;
        for (int i=0;i<list.length;i++) { // count non-empty lines
           if (list[i][0]!=null) count++;
        }
        if (count==list.length) return list; // no empty-lines. Return original
        String[][] newlist=new String[count][];
        int newindex=0;
        for (int i=0;i<list.length;i++) { // assign non-empty lines to new array
           if (list[i][0]!=null) {
               newlist[newindex]=list[i];
               newindex++;
           }
        }
        return newlist;
    }
    
    private Class checkSourceObjects(String[][] list, DataTypeTable lookup) throws ParseError {
        Class collateType=null;
        for (int i=0;i<list.length;i++) {
           String sourceName=list[i][0];
           if (sourceName.equals(engine.getTypeNameForDataClass(Motif.class))) {
                if (collateType==null) collateType=Motif.class;
                else if (collateType!=Motif.class) throw new ParseError("Motif properties can not be collated together with previous columns ("+engine.getTypeNameForDataClass(collateType)+")");
           } else if (sourceName.equals(engine.getTypeNameForDataClass(ModuleCRM.class))) {
                if (collateType==null) collateType=ModuleCRM.class;
                else if (collateType!=ModuleCRM.class) throw new ParseError("Module properties can not be collated together with previous columns ("+engine.getTypeNameForDataClass(collateType)+")");
           } else if (sourceName.equals(engine.getTypeNameForDataClass(Sequence.class))) {
                if (collateType==null) collateType=Sequence.class;
                else if (collateType!=Sequence.class) throw new ParseError("Sequence properties can not be collated together with previous columns ("+engine.getTypeNameForDataClass(collateType)+")");
           } else {
                Class sourceType=null; // the collated type of this source
                Data source=engine.getDataItem(sourceName);
                if (source instanceof Analysis) { // The analysis exists so I will use it directly
                     sourceType=((Analysis)source).getCollateType();
                } else if (source instanceof DataMap) { //
                     sourceType=((DataMap)source).getMembersClass();
                } else { // The data object does not exist but we can find the class and then derive the collate type from proxy Analysis or DataMap of same type
                    Class sourceclass=lookup.getClassFor(sourceName);
                    if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName); 
                    if (Analysis.class.isAssignableFrom(sourceclass)) {
                        Analysis proxy=engine.getAnalysisForClass(sourceclass);
                        if (proxy!=null) sourceType=proxy.getCollateType();    
                        if (sourceType==null) sourceType=lookup.getCollatedTypeFor(sourceName); // maybe the collation type has been registered specifically
                    } else if (DataMap.class.isAssignableFrom(sourceclass)) {
                        try {
                          sourceType=((DataMap)sourceclass.newInstance()).getMembersClass();
                        } catch (Exception e) {throw new ParseError(e.getMessage());}
                    } else throw new ParseError("'"+sourceName+"' is not an Analysis object or Map");
                }               
                if (sourceType==null) throw new ParseError("Unable to determine data type for collated analysis"); 
                if (collateType==null) collateType=sourceType;
                else if (collateType!=sourceType) {
                    String colname=(list[i][1]!=null && !list[i][1].isEmpty())?("'"+list[i][1]+"' from "):"";
                    throw new ParseError(colname+"'"+sourceName+"' ("+engine.getTypeNameForDataClass(sourceType)+") can not be collated together with previous columns ("+engine.getTypeNameForDataClass(collateType)+")");
                }
          }
        }
        return collateType;
    }

}
