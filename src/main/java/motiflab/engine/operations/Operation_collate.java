/*
 
 
 */

package motiflab.engine.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.task.OperationTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.*;
import motiflab.engine.data.analysis.Analysis;
import motiflab.engine.data.analysis.AnnotatedValue;
import motiflab.engine.data.analysis.CollatedAnalysis;

/**
 *
 * @author kjetikl
 */
public class Operation_collate extends Operation {
    public static final String SOURCE_DATA="sourceData"; // references a String[][] with information about which columns to include from which analyses
    public static final String COLLATE_DATA_TYPE="collateType"; // the type of data Motif/Module/Sequence
    public static final String OPTIONAL_TITLE="optionalTitle"; //
    private static final String name="collate";
    private static final String description="Creates a new analysis object by combining data columns from multiple sources";
    private Class[] datasourcePreferences=new Class[]{Analysis.class, NumericMap.class, TextMap.class};

    @Override
    public String getOperationGroup() {
        return "Combine";
    }

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
    public boolean canHaveMultipleInput() {
        return true; 
    }    
    
  
    
    @Override
    public boolean execute(OperationTask task) throws Exception {
        String[][] entries=(String[][])task.getParameter(SOURCE_DATA); // this could now potentially be a comma-separated list!
        if (entries.length==0) throw new ExecutionError("Missing source objects",task.getLineNumber());
        Class collateType=(Class)task.getParameter(COLLATE_DATA_TYPE);
        if (collateType==null) throw new ExecutionError("Unable to determine data type for collated analysis");
        CollatedAnalysis targetData=new CollatedAnalysis(task.getTargetDataName());
        targetData.setCollateType(collateType);
        String optionalTitle=(String)task.getParameter(OPTIONAL_TITLE);
        if (optionalTitle!=null) targetData.setOptionalTitle(optionalTitle);
        ArrayList<String[]> resolveProperties=new ArrayList<String[]>(); // lists direct "Basic data type properties" (not Analysis or Map objects) that should be included
        for (int i=0;i<entries.length;i++) {
           String propertysource=entries[i][0];
           String columnname=entries[i][1];
           String newcolumnName=entries[i][2]; 
           if (isBasicClassProperty(propertysource)) { // property source is a Data type like Motif, ModuleCRM or Sequence
               Class propertysourcetype=engine.getDataClassForTypeName(propertysource);
               if (collateType!=propertysourcetype) throw new ExecutionError("Properties from "+propertysource.toLowerCase()+"s can not be combined with the type of data found in the previous columns ("+engine.getTypeNameForDataClass(collateType)+")",task.getLineNumber());
                // now add the specified property of the data type the collated object!
               if (newcolumnName==null) newcolumnName=columnname;
               if (!columnname.trim().isEmpty()) { 
                   Class columnType=getPropertyClass(columnname, propertysourcetype);
                   if (columnType==null) throw new ExecutionError("Data objects of type '"+propertysource+"' do not have a property named '"+columnname+"'");
                   if (ArrayList.class.isAssignableFrom(columnType)) columnType=String.class; // we will flatten lists into simple strings!
                   HashMap<String,Object> columnData=new HashMap<String,Object>(); // this will be filled in later, when we know which entries (rows) are included         
                   targetData.addColumn(newcolumnName, columnType, columnData, new String[]{propertysource,columnname});          
                   resolveProperties.add(new String[]{columnname,newcolumnName});              
               } else {
                  // I will allow "emtpy" column names to create a Motif, ModuleCRM or Sequence collated analysis with no columns 
               }
           } else { //  property source should be an Analysis object or Numeric Map
               Data data=engine.getDataItem(propertysource);
               if (data==null) throw new ExecutionError("Unknown data object '"+propertysource+"'",task.getLineNumber());
               if (data instanceof Analysis) {
                   Analysis sourceAnalysis=(Analysis)data;
                   Class analysisCollationType=sourceAnalysis.getCollateType();
                   if (analysisCollationType==null) throw new ExecutionError("'"+propertysource+"' can not be collated",task.getLineNumber());
                   if (collateType!=analysisCollationType) throw new ExecutionError("'"+propertysource+"' contains data of a type ("+engine.getTypeNameForDataClass(analysisCollationType)+") which is different from the data in the previous columns ("+engine.getTypeNameForDataClass(collateType)+")",task.getLineNumber());
                   // now add the specified column from the analysis to the collated object!
                   if (newcolumnName==null) newcolumnName=columnname;
                   String[] exportedColumns=null;
                   String[] exportedColumnNames=null;
                   if (columnname.equals("*")) { // A wildcare indicates that all exported columns from the analysis should be added
                       exportedColumns=sourceAnalysis.getColumnsExportedForCollation();
                       exportedColumnNames=new String[exportedColumns.length];
                       for (int n=0;n<exportedColumnNames.length;n++) {
                           exportedColumnNames[n]=newcolumnName.replace("*", exportedColumns[n]);  // replace wildcards in newcolumnName with the column name from the source 
                       }
                   } else { // just add a single named column from the analysis
                      exportedColumns=new String[]{columnname}; 
                      exportedColumnNames=new String[]{newcolumnName};
                   }
                   // now add 1 or more columns to the new collated analysis object
                   for (int n=0;n<exportedColumns.length;n++) {
                      Class columnType=sourceAnalysis.getColumnType(exportedColumns[n]);
                       if (columnType==null) throw new ExecutionError("Analysis '"+propertysource+"' does not have a column named '"+exportedColumns[n]+"'");
                       HashMap<String,Object> columnData=sourceAnalysis.getColumnData(exportedColumns[n]);
                       if (columnType==Double.class && isColumnReallyInteger(columnData.values())) {
                          columnType=Integer.class; // convert the column for easy presentation if all values are integers anyway
                          convertToIntegers(columnData);
                       }
                       targetData.addColumn(exportedColumnNames[n], columnType, columnData, new String[]{propertysource,exportedColumns[n]});
                   }
                                                        
               } else if (data instanceof NumericMap) {
                   Class mapType=((NumericMap)data).getMembersClass();
                   if (collateType!=mapType) throw new ExecutionError("'"+propertysource+"' contains data of a type ("+engine.getTypeNameForDataClass(mapType)+") which is different from the data in the previous columns ("+engine.getTypeNameForDataClass(collateType)+")",task.getLineNumber());
                   if (newcolumnName==null) newcolumnName=data.getName();
                   HashMap<String,Object> columnData=((NumericMap)data).getColumnData();
                   Class columnType=Double.class; 
                   if (isColumnReallyInteger(columnData.values())) {
                      columnType=Integer.class; // convert the column for easy presentation if all values are integers anyway
                      convertToIntegers(columnData);
                   }                   
                   targetData.addColumn(newcolumnName, columnType, columnData, new String[]{propertysource,"values"});
               } else if (data instanceof TextMap) {
                   Class mapType=((TextMap)data).getMembersClass();
                   if (collateType!=mapType) throw new ExecutionError("'"+propertysource+"' contains data of a type ("+engine.getTypeNameForDataClass(mapType)+") which is different from the data in the previous columns ("+engine.getTypeNameForDataClass(collateType)+")",task.getLineNumber());
                   if (newcolumnName==null) newcolumnName=data.getName();
                   HashMap<String,Object> columnData=((TextMap)data).getColumnData();
                   Class columnType=String.class;                   
                   targetData.addColumn(newcolumnName, columnType, columnData, new String[]{propertysource,"values"});
               } else throw new ExecutionError("Data object '"+propertysource+"' is not an Analysis object or a Numeric Map",task.getLineNumber());
           }
           task.checkExecutionLock(); // checks to see if this task should suspend execution
           if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
           task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+entries.length+")");
           task.setProgress(i, entries.length); //       
        }
        if (!resolveProperties.isEmpty()) {
            HashSet<String> rows=targetData.getEntries();
            for (String[] dataproperty:resolveProperties) {
                String propertyname=dataproperty[0];
                String columnname=dataproperty[1];                     
                for (String row:rows) {
                    Object value=getPropertyValue(row,propertyname,collateType);
                    targetData.setColumnData(columnname, row, value);
                }
                // check the column if it contains all integers
                if (targetData.getColumnType(columnname)==Double.class) {
                    HashMap<String,Object> columndata=targetData.getColumnData(columnname);
                    if (isColumnReallyInteger(columndata.values())) {
                        convertToIntegers(columndata);
                        targetData.setCollateType(Integer.class);
                    }
                }
            }
        }
        try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }
    
    private boolean isBasicClassProperty(String propertysource) {
        return  (  
                  propertysource.equals(engine.getTypeNameForDataClass(motiflab.engine.data.Motif.class))
               || propertysource.equals(engine.getTypeNameForDataClass(motiflab.engine.data.ModuleCRM.class))
               || propertysource.equals(engine.getTypeNameForDataClass(motiflab.engine.data.Sequence.class))
        );        
    }
    
    private Class getPropertyClass(String propertyName, Class collateType) throws ExecutionError {
             if (collateType==Motif.class) return Motif.getPropertyClass(propertyName,engine);
        else if (collateType==ModuleCRM.class) return ModuleCRM.getPropertyClass(propertyName,engine);
        else if (collateType==Sequence.class) return Sequence.getPropertyClass(propertyName,engine);
        else return null; // this should never happen
    }    
    
    private Object getPropertyValue(String dataname, String propertyName, Class collateType) throws ExecutionError {
             if (collateType==Motif.class) return getPropertyFromMotif(dataname,propertyName);
        else if (collateType==ModuleCRM.class) return getPropertyFromModule(dataname,propertyName);
        else if (collateType==Sequence.class) return getPropertyFromSequence(dataname,propertyName);
        else return null; // this should never happen
    }
    
    private Object getPropertyFromMotif(String motifname, String property) throws ExecutionError {
        Data data=engine.getDataItem(motifname);
        if (!(data instanceof Motif)) throw new ExecutionError("'"+motifname+"' is not a Motif");
        Object value=((Motif)data).getPropertyValue(property, engine);
        if (value instanceof ArrayList) value=MotifLabEngine.splice((ArrayList)value, ","); // flatten lists
        return value;
    }
    private Object getPropertyFromModule(String modulename, String property) throws ExecutionError {
        Data data=engine.getDataItem(modulename);
        if (!(data instanceof ModuleCRM)) throw new ExecutionError("'"+modulename+"' is not a Module");
        Object value=((ModuleCRM)data).getPropertyValue(property,engine);
        if (value instanceof ArrayList) value=MotifLabEngine.splice((ArrayList)value, ","); // flatten lists
        return value;
    }
    private Object getPropertyFromSequence(String sequencename, String property) throws ExecutionError {
        Data data=engine.getDataItem(sequencename);
        if (!(data instanceof Sequence)) throw new ExecutionError("'"+sequencename+"' is not a Sequence");
        Object value=((Sequence)data).getPropertyValue(property,engine);
        if (value instanceof ArrayList) value=MotifLabEngine.splice((ArrayList)value, ","); // flatten lists
        return value;
    }    
    
    private boolean isColumnReallyInteger(Collection<Object> values) {
       if (values==null || values.isEmpty()) return false; // unable to determine if they are integers
       for (Object value:values) {
           if (value instanceof AnnotatedValue) value=((AnnotatedValue)value).getValue();
           if (!(value instanceof Number)) return false;
           if (value instanceof Double) {
               double doublevalue=(Double)value;
               if (Double.isNaN(doublevalue)) return false;
               int intvalue=((Double)value).intValue();  
               if (intvalue!=doublevalue) return false;
           } else return false;
       }
       return true;
    }   
    
    /** Converts the values in this column from Doubles to Integers  
     *  The method does not perform any checks, so make sure to test the column
     *  first with the isColumnReallyInteger() method above
     */
    private void convertToIntegers(HashMap<String,Object> column) {
       ArrayList<String> keys=new ArrayList<String>(column.keySet());
       for (String key:keys) {
           Object value=column.get(key);
           if (value instanceof AnnotatedValue) {
               Double oldvalue=(Double)((AnnotatedValue)value).getValue();
               ((AnnotatedValue)value).setValue(new Integer(oldvalue.intValue()));
           } else if (value instanceof Double) {
               column.put(key, new Integer(((Double)value).intValue()));
           }
       }
    }      
    
}
    