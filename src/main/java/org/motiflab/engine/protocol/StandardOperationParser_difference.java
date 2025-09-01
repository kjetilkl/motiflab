/*
 
 
 */

package org.motiflab.engine.protocol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.operations.Operation;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModulePartition;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.operations.Operation_difference;
/**
 *
 * @author kjetikl
 */
public class StandardOperationParser_difference extends StandardOperationParser {


    @Override
    public String getCommandString(OperationTask task) {
        String sourceName1=task.getSourceDataName();
        String sourceName2=(String)task.getParameter(Operation_difference.COMPARE_AGAINST_DATA);
        String options=(String)task.getParameter(Operation_difference.REGION_DATASET_OPTIONS);
        String propertyName=(String)task.getParameter(Operation_difference.REGION_DATASET_TARGET_PROPERTY);     
        String targetName=task.getTargetDataName();
        String msg=targetName+" = difference";
        if (propertyName!=null) msg+=" in \""+propertyName+"\"";
        msg+=" between "+sourceName1+" and "+sourceName2;
        if (options!=null && !options.isEmpty()) msg+=" "+options;
        return msg;
    }

    @Override
    public OperationTask parse(String command) throws ParseError {
           DataTypeTable lookup=protocol.getDataTypeLookupTable();
           String operationName="difference";
           String sourceName1=null;
           String sourceName2=null;           
           String targetName=null;
           String propertyName=null;
           String options=null;
           Pattern pattern=Pattern.compile("^(([a-zA-Z_0-9-]+)\\s*=\\s*)?difference(\\s+in\\s+\"(.+?)\")?\\s+between\\s+([a-zA-Z_0-9-]+)\\s+and\\s+([a-zA-Z_0-9-]+)(\\s+(\\S.+))?");
           Matcher matcher=pattern.matcher(command);
           if (matcher.find()) {
               targetName=matcher.group(2);
               propertyName=matcher.group(4);               
               sourceName1=matcher.group(5);
               sourceName2=matcher.group(6);
               options=matcher.group(8);
               if (options!=null) options=options.trim();
               // if (unknown!=null && !unknown.isEmpty()) throw new ParseError("Unrecognized clause (or wrong order): "+unknown);               
           } else throw new ParseError("Unable to parse "+operationName+" command: "+command);
           OperationTask task=new OperationTask(operationName);
           Operation operation=engine.getOperation(operationName);
           if (operation==null) throw new ParseError("SYSTEM ERROR: Missing operation '"+operationName+"'");
           if (sourceName1==null || sourceName1.isEmpty()) throw new ParseError("Missing source data for extract operation");           
           if (sourceName2==null || sourceName2.isEmpty()) throw new ParseError("Missing source data for extract operation");           
           if (targetName==null || targetName.isEmpty()) throw new ParseError("Missing required target data name for difference operation");           
           Class sourceclass1=lookup.getClassFor(sourceName1);
           if (sourceclass1==null) throw new ParseError("Unrecognized data object: "+sourceName1);
           Class sourceclass2=lookup.getClassFor(sourceName2);
           if (sourceclass2==null) throw new ParseError("Unrecognized data object: "+sourceName2);
           if (!operation.canUseAsSource(sourceclass1)) throw new ParseError("'"+sourceName1+"' is of a type not supported by operation '"+operationName+"'");
           if (sourceclass2!=sourceclass1) throw new ParseError("The two data object to compare must be of the same type!");
        
           if (options!=null) {
               if (sourceclass1!=RegionDataset.class) throw new ParseError("Additional options can only be used for Region Dataset comparisons");
               boolean recognizedOption=false;
               for (String known:Operation_difference.getRegionDatasetOptions()) {
                   if (options.equalsIgnoreCase(known)) {recognizedOption=true;break;}
               }
               if (!recognizedOption) throw new ParseError("Unrecognized option for Region Dataset comparison: "+options);
           }
           Class targetclass;
                if (sourceclass1==NumericDataset.class) targetclass=NumericDataset.class;
           else if (sourceclass1==DNASequenceDataset.class) targetclass=NumericDataset.class;
           else if (sourceclass1==RegionDataset.class) targetclass=RegionDataset.class;
           else if (NumericMap.class.isAssignableFrom(sourceclass1)) targetclass=sourceclass1;
           else if (sourceclass1==MotifCollection.class) targetclass=MotifPartition.class;
           else if (sourceclass1==ModuleCollection.class) targetclass=ModulePartition.class;
           else if (sourceclass1==SequenceCollection.class) targetclass=SequencePartition.class;
           else throw new ParseError("'"+sourceName1+"' is of a type not supported by operation '"+operationName+"'");
           
           if (propertyName!=null && sourceclass1!=RegionDataset.class) throw new ParseError("Differences in properties can only be determined for Region Datasets");
                
           lookup.register(targetName, targetclass);
           task.addAffectedDataObject(targetName, targetclass);

           task.setParameter(OperationTask.OPERATION, operation);
           task.setParameter(OperationTask.OPERATION_NAME, operationName);
           task.setParameter(OperationTask.SOURCE_NAME, sourceName1);
           task.setParameter(Operation_difference.COMPARE_AGAINST_DATA, sourceName2); 
           if (options!=null) task.setParameter(Operation_difference.REGION_DATASET_OPTIONS, options);             
           if (propertyName!=null) task.setParameter(Operation_difference.REGION_DATASET_TARGET_PROPERTY, propertyName);             
           task.setParameter(OperationTask.TARGET_NAME, targetName);
           task.setParameter(command, task);
           return task;
    }

}


