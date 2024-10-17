/*


 */

package motiflab.engine.protocol;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.Motif;
import motiflab.engine.data.NumericMap;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.analysis.Analysis;
import motiflab.engine.operations.Operation_rank;


/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_rank extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String targetName=task.getTargetDataName();
        String[][] entries=(String[][])task.getParameter(Operation_rank.SOURCE_DATA);
        StringBuilder line=new StringBuilder();
        line.append(targetName);
        line.append(" = rank ");
        for (int i=0;i<entries.length;i++) {
            String[] entry=entries[i];
            if (i>0) line.append(", ");
            if (entry[3]!=null) {
                line.append(entry[3]);
                line.append(" ");
            }
            if (entry[1]!=null) {
                line.append("\"");
                line.append(entry[1]);
                line.append("\" from ");
            }
            line.append(entry[0]);
            if (entry[2]!=null){
               line.append(" with weight=");
               line.append(entry[2]);
            }
        }
        return line.toString();
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="rank";
           String targetName=null;
           String rankPattern=null;

           Pattern entrypattern=Pattern.compile("(ascending\\s+|descending\\s+)?(\"(.+?)\"\\s+from\\s+)?([a-zA-Z_0-9-]+)(\\s+with\\s+weight\\s*=\\s*(\\S+))?(\\s*\\S.*)?");
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?rank (.+)");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               rankPattern=matcher.group(3);
                //for (int i=0;i<=matcher.groupCount();i++) System.err.println("Group["+i+"]=>"+matcher.group(i));
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing required target data object for rank operation");
           ArrayList<String> columnsList=MotifLabEngine.splitOnComma(rankPattern.trim());
           String[][] list=new String[columnsList.size()][4];
           for (int i=0;i<columnsList.size();i++) {
               String entry=columnsList.get(i).trim();
               if (entry.isEmpty()) throw new ParseError("Empty command part encountered");
               Matcher entrymatcher=entrypattern.matcher(entry);
               if (entrymatcher.find()) {
                   //for (int j=0;j<=matcher.groupCount();j++) System.err.println("#1:Group["+j+"]=>"+matcher.group(j));
                   if (entrymatcher.group(7)!=null && !entrymatcher.group(7).trim().isEmpty()) throw new ParseError("Parse error near '"+entrymatcher.group(7)+"'");
                   list[i][0]=entrymatcher.group(4); // analysis or Numeric Map (or Data type (e.g. Motif)?)
                   list[i][1]=entrymatcher.group(3); // property (column)
                   list[i][2]=entrymatcher.group(6); // weight
                   list[i][3]=entrymatcher.group(1); // ascending or descending
                   if (list[i][3]!=null) list[i][3]=list[i][3].trim();
                   if (list[i][2]!=null && ((String)list[i][2]).trim().isEmpty()) list[i][2]=null; // no weight specified (default to 1)
                   if (entrymatcher.group(2)==null || entrymatcher.group(2).isEmpty()) list[i][1]=null; // no property specified. The data source should be a Numeric Map
               } else throw new ParseError("Unable to parse entry: "+entry);

           }
           list=stripEmptyLines(list);
           Class targetType=checkSourceObjects(list,lookup); // checks the list of source analyses and throws ParseErrors if appropriate
           Class targetMapType=NumericMap.getMapTypeForDataType(targetType);
           if (targetMapType==null) throw new ParseError("Unable to determine correct data type for target");
           checkWeightStrings(list,lookup); // check that the weight strings are all valid numbers or names of Numeric Variables
           lookup.register(targetName, targetMapType);
           task.addAffectedDataObject(targetName,targetMapType);
           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(Operation_rank.SOURCE_DATA, list);
           task.setParameter(Operation_rank.TARGET_DATA_TYPE, targetType);
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
        Class rankType=null;
        for (int i=0;i<list.length;i++) {
           String sourceName=list[i][0];
           if (sourceName.equals(engine.getTypeNameForDataClass(Motif.class))) {
                if (rankType==null) rankType=Motif.class;
                else if (rankType!=Motif.class) throw new ParseError("Motif properties can not be compared to previous properties ("+engine.getTypeNameForDataClass(rankType)+")");
           } else if (sourceName.equals(engine.getTypeNameForDataClass(ModuleCRM.class))) {
                if (rankType==null) rankType=ModuleCRM.class;
                else if (rankType!=ModuleCRM.class) throw new ParseError("Module properties can not be compared to previous properties ("+engine.getTypeNameForDataClass(rankType)+")");
           } else if (sourceName.equals(engine.getTypeNameForDataClass(Sequence.class))) {
                if (rankType==null) rankType=Sequence.class;
                else if (rankType!=Sequence.class) throw new ParseError("Sequence properties can not be compared to previous properties ("+engine.getTypeNameForDataClass(rankType)+")");
           } else {
                Class sourceType=null; // the collated type of this source
                Data source=engine.getDataItem(sourceName);
                if (source instanceof Analysis) { // The analysis exists so I will use it directly
                     sourceType=((Analysis)source).getCollateType();
                } else if (source instanceof NumericMap) { //
                     sourceType=((NumericMap)source).getMembersClass();
                } else { // The data object does not exist but we can find the class and then derive the collate type from proxy Analysis or NumericMap of same type
                    Class sourceclass=lookup.getClassFor(sourceName);
                    if (sourceclass==null) throw new ParseError("Unrecognized source object: "+sourceName);
                    if (Analysis.class.isAssignableFrom(sourceclass)) {
                        Analysis proxy=engine.getAnalysisForClass(sourceclass);
                        if (proxy!=null) sourceType=proxy.getCollateType();
                    } else if (NumericMap.class.isAssignableFrom(sourceclass)) {
                        try {
                          sourceType=((NumericMap)sourceclass.newInstance()).getMembersClass();
                        } catch (Exception e) {throw new ParseError(e.getMessage());}
                    } else throw new ParseError("'"+sourceName+"' is not an Analysis object or Numeric Map");
                }
                if (sourceType==null) throw new ParseError("Unable to determine target data type");
                if (rankType==null) rankType=sourceType;
                else if (rankType!=sourceType) {
                    String colname=(list[i][1]!=null && !list[i][1].isEmpty())?("'"+list[i][1]+"' from "):"";
                    throw new ParseError(colname+"'"+sourceName+"' ("+engine.getTypeNameForDataClass(sourceType)+") can not be combined with with previous properties ("+engine.getTypeNameForDataClass(rankType)+")");
                }
          }
        }
        return rankType;
    }

    private void checkWeightStrings(String[][] list, DataTypeTable lookup) throws ParseError {
        for (int i=0;i<list.length;i++) {
           String weightString=list[i][2];
           if (weightString==null) continue;
           try {
               Double.parseDouble(weightString);
           } catch (NumberFormatException e) { // weight is not a number. Check if it is a NumericVariable
               Class weightDataClass=lookup.getClassFor(weightString);
               if (weightDataClass==null || weightDataClass!=NumericVariable.class) throw new ParseError("The weight '"+weightString+"' is not a valid numeric value or Numeric Variable");
           }
        }
    }

}
