/*
 
 
 */

package motiflab.engine.operations;

import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.Data;

/**
 *
 * @author kjetikl
 */
public class Operation_extract extends Operation {
    private static final String name="extract";
    private static final String description="Extracts a specific value from a data object";
    private Class[] datasourcePreferences=new Class[]{Data.class};

    public static final String RESULT_VARIABLE_NAME="resultvariablename"; 
    public static final String RESULT_VARIABLE_TYPE="resultvariabletype"; 

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {return name;}    
    
    @Override
    public boolean execute(OperationTask task) throws Exception {            
        String targetDataName=task.getTargetDataName();
        if (targetDataName==null || targetDataName.isEmpty()) throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        String sourceDataName=task.getSourceDataName();
        if (sourceDataName==null || sourceDataName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber());         
        Data sourceData=engine.getDataItem(sourceDataName);
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceDataName+"'",task.getLineNumber());
        String variableName=(String)task.getParameter(RESULT_VARIABLE_NAME);
        Class variableType=(Class)task.getParameter(RESULT_VARIABLE_TYPE);
        Data targetData=sourceData.getResult(variableName,engine);
        if (!variableType.isAssignableFrom(targetData.getClass())) throw new ExecutionError("Result variable '"+variableName+"' is of type '"+targetData.getDynamicType()+"' not '"+engine.getTypeNameForDataClass(variableType)+"'");
        targetData.rename(targetDataName);
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
}
