/*


 */

package org.motiflab.engine.operations;

import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.data.analysis.AnnotatedValue;

/**
 *
 * @author kjetikl
 */
public class Operation_rank extends Operation {
    public static final String SOURCE_DATA="sourceData"; // references a String[][] with information about which columns to include from which analyses/maps
    public static final String TARGET_DATA_TYPE="targetType"; // the type of data Motif/Module/Sequence
    private static final String name="rank";
    private static final String description="Creates a new Numeric Map where the values correspond to the rank order of the elements in another Numeric Map or column from an analysis. The rank can also be based on a linear combination of several properties.";
    private Class[] datasourcePreferences=new Class[]{Analysis.class, NumericMap.class};

    @Override
    public String getOperationGroup() {
        return "Derive";
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
        String[][] entries=(String[][])task.getParameter(SOURCE_DATA); //
        if (entries==null || entries.length==0) throw new ExecutionError("Missing source objects",task.getLineNumber());
        double[] weights=new double[entries.length];
        for (int i=0;i<weights.length;i++) weights[i]=1.0;
        NumericMap[] rankmaps=new NumericMap[entries.length];
        Class targetType=(Class)task.getParameter(TARGET_DATA_TYPE);
        if (targetType==null) throw new ExecutionError("Unable to determine target data type for the rank operation");
        NumericMap targetData=NumericMap.createMapForType(task.getTargetDataName(),targetType,0); // change default later
        if (targetData==null) throw new ExecutionError("SYSTEM ERROR: Unable to create result data object for rank operation");
        for (int i=0;i<entries.length;i++) {
           String propertysource=entries[i][0];
           String columnname=entries[i][1];
           String weightString=entries[i][2];
           String sortDirection=entries[i][3];
           if (sortDirection.equalsIgnoreCase("descending")) sortDirection="rank descending";
           else sortDirection="rank ascending";
           if (weightString!=null) {  // determine weight
               try {
                   double weight=Double.parseDouble(weightString);
                   weights[i]=weight;
               } catch (NumberFormatException e) {
                   Data weightdata=engine.getDataItem(weightString);
                   if (weightdata==null) throw new ExecutionError("The weight '"+weightString+"' is not a valid numeric value or a Numeric Variable");
                   if (weightdata instanceof NumericVariable) {
                       double weight=((NumericVariable)weightdata).getValue();
                       weights[i]=weight;
                   } else throw new ExecutionError("The weight '"+weightString+"' is not a valid numeric value or a Numeric Variable");
               }
           }
           if (weights[i]<0) throw new ExecutionError("The weight '"+weightString+"' is a negative value");
           //
           if (   propertysource.equals(engine.getTypeNameForDataClass(org.motiflab.engine.data.Motif.class))
               || propertysource.equals(engine.getTypeNameForDataClass(org.motiflab.engine.data.ModuleCRM.class))
               || propertysource.equals(engine.getTypeNameForDataClass(org.motiflab.engine.data.Sequence.class))
           ) { // property source is a Data type like Motif, ModuleCRM or Sequence
               Class propertysourcetype=engine.getDataClassForTypeName(propertysource);
               if (targetType!=propertysourcetype) throw new ExecutionError("Properties from "+propertysource.toLowerCase()+"s can not be combined with the type of data found in the previous entries ("+engine.getTypeNameForDataClass(targetType)+")",task.getLineNumber());
                // now add the specified property of the data type the collated object!
               Class columnType=getPropertyClass(columnname, propertysourcetype);
               if (columnType==null) throw new ExecutionError("Data objects of type '"+propertysource+"' do not have a property named '"+columnname+"'");
               if (!Number.class.isAssignableFrom(columnType)) throw new ExecutionError("The "+propertysource.toLowerCase()+" property  '"+columnname+"' is not numeric");
               HashMap<String,Object> columnData=resolveProperties(targetData, columnname, targetType);
               NumericMap map=createNumericMapFromHashMap(targetType, columnData, sortDirection.equals("rank ascending"),propertysource+"->"+columnname);
               rankmaps[i]=(NumericMap)map.getResult(sortDirection,engine);
               if (rankmaps[i]==null) throw new ExecutionError("Unable to determine rank order for '"+propertysource+"->"+columnname+"'");
           } else { //  property source should be an Analysis object or Numeric Map
               Data data=engine.getDataItem(propertysource);
               if (data==null) throw new ExecutionError("Unknown data object '"+propertysource+"'",task.getLineNumber());
               if (data instanceof Analysis) {
                   Analysis sourceAnalysis=(Analysis)data;
                   Class analysisCollationType=sourceAnalysis.getCollateType();
                   if (analysisCollationType==null) throw new ExecutionError("'"+propertysource+"' can not be used by the rank operation",task.getLineNumber());
                   if (targetType!=analysisCollationType) throw new ExecutionError("'"+propertysource+"' contains data of a type ("+engine.getTypeNameForDataClass(analysisCollationType)+") which is different from the data in the previous entries ("+engine.getTypeNameForDataClass(targetType)+")",task.getLineNumber());
                   // now add the specified column from the analysis to the collated object!
                   Class columnType=sourceAnalysis.getColumnType(columnname);
                   if (columnType==null) throw new ExecutionError("Analysis '"+propertysource+"' does not have a column named '"+columnname+"'");
                   HashMap<String,Object> columnData=sourceAnalysis.getColumnData(columnname);
                   NumericMap map=createNumericMapFromHashMap(targetType, columnData, sortDirection.equals("rank ascending"),propertysource+"->"+columnname);
                   rankmaps[i]=(NumericMap)map.getResult(sortDirection,engine);
                   if (rankmaps[i]==null) throw new ExecutionError("Unable to determine rank order for '"+propertysource+"->"+columnname+"'");
               } else if (data instanceof NumericMap) {
                   Class mapType=((NumericMap)data).getMembersClass();
                   if (targetType!=mapType) throw new ExecutionError("'"+propertysource+"' contains data of a type ("+engine.getTypeNameForDataClass(mapType)+") which is different from the data in the previous entries ("+engine.getTypeNameForDataClass(targetType)+")",task.getLineNumber());
                   HashMap<String,Object> columnData=((NumericMap)data).getColumnData();
                   NumericMap map=createNumericMapFromHashMap(targetType, columnData,sortDirection.equals("rank ascending"),propertysource);
                   rankmaps[i]=(NumericMap)map.getResult(sortDirection,engine);
                   if (rankmaps[i]==null) throw new ExecutionError("Unable to determine rank order for '"+propertysource+"'");
               } else throw new ExecutionError("Data object '"+propertysource+"' is not an Analysis object or a Numeric Map",task.getLineNumber());
           }
           task.checkExecutionLock(); // checks to see if this task should suspend execution
           if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
           task.setStatusMessage("Executing operation: "+task.getOperationName()+" ("+(i+1)+"/"+entries.length+")");
           task.setProgress(i, entries.length); //
        }
        double sumWeights=0;
        boolean equalweights=true;
        double firstWeight=weights[0];
        for (int i=1;i<weights.length;i++) {
            if (weights[i]!=firstWeight) {equalweights=false;break;}
        }
        if (equalweights) { // handle equal weights as special to avoid potential precision/rounding errors
            ArrayList<String> keys=targetData.getAllKeys(engine);
            for (String key:keys) {
                double rankSum=0;
                for (int i=0;i<rankmaps.length;i++) {
                    NumericMap rankmap=rankmaps[i];
                    rankSum+=rankmap.getValue(key);
                }
                targetData.setValue(key, rankSum);
            }
        } else {
            for (int i=0;i<weights.length;i++) sumWeights+=weights[i];
            for (int i=0;i<weights.length;i++) weights[i]=weights[i]/sumWeights; // normalize weights
            ArrayList<String> keys=targetData.getAllKeys(engine);
            for (String key:keys) {
                double weightedRankSum=0;
                for (int i=0;i<rankmaps.length;i++) {
                    NumericMap rankmap=rankmaps[i];
                    weightedRankSum+=weights[i]*rankmap.getValue(key);
                }
                targetData.setValue(key, weightedRankSum);
            }
        }
        targetData=(NumericMap)targetData.getResult("rank ascending", engine);
        targetData.rename(task.getTargetDataName());
        try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment:"+ce.getMessage(),task.getLineNumber());}
        return true;
    }


    private HashMap<String,Object> resolveProperties(NumericMap targetData, String property, Class targetType) throws ExecutionError {
        HashMap<String,Object> result=new HashMap<String, Object>();
        ArrayList<String> keys=targetData.getAllKeys(engine);
        for (String key:keys) {
            Object value=getPropertyValue(key, property, targetType);
            result.put(key, value);
        }
        return result;
    }

    private NumericMap createNumericMapFromHashMap(Class targetType, HashMap<String,Object> map, boolean ascending, String propertyName) throws ExecutionError {
        double minValue=Double.MAX_VALUE;
        double maxValue=-Double.MAX_VALUE;

        NumericMap result=NumericMap.createMapForType("temp", targetType, 0); // the default value will be set later
        for (String key:map.keySet()) {
            Object valueObject=map.get(key);
            if (valueObject==null) continue;
            if (valueObject instanceof AnnotatedValue) valueObject=((AnnotatedValue)valueObject).getValue();
            if (valueObject instanceof Number) {
                double value=((Number)valueObject).doubleValue();
                if (value<minValue) minValue=value;
                if (value>maxValue) maxValue=value;
                result.setValue(key,value);
            } else throw new ExecutionError("The value of property '"+propertyName+"' for entry '"+key+"' is not numeric");
        }
        double defaultValue=0;
        if (ascending) { // find highest value and set defaultValue to a value larger than that
           defaultValue=maxValue+1;
        } else { // find lowest value and set defaultValue to a value smaller than that
           defaultValue=minValue-1;
        }
        result.setDefaultValue(defaultValue);
        return result;
    }

    private Class getPropertyClass(String propertyName, Class targetType) throws ExecutionError {
             if (targetType==Motif.class) return Motif.getPropertyClass(propertyName,engine);
        else if (targetType==ModuleCRM.class) return ModuleCRM.getPropertyClass(propertyName,engine);
        else if (targetType==Sequence.class) return Sequence.getPropertyClass(propertyName,engine);
        else return null; // this should never happen
    }

    private Object getPropertyValue(String dataname, String propertyName, Class targetType) throws ExecutionError {
             if (targetType==Motif.class) return getPropertyFromMotif(dataname,propertyName);
        else if (targetType==ModuleCRM.class) return getPropertyFromModule(dataname,propertyName);
        else if (targetType==Sequence.class) return getPropertyFromSequence(dataname,propertyName);
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

}
