/*
 
 
 */

package motiflab.engine.dataformat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.MotifNumericMap;
import motiflab.engine.data.OutputData;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_TransfacProfile extends DataFormat {
    private String name="TransfacProfile";
    private Class[] supportedTypes=new Class[]{MotifNumericMap.class};


    public DataFormat_TransfacProfile() {
        addOptionalParameter("Core thresholds", null, new Class[]{MotifNumericMap.class},"Operational map containing core threshold values");
        addOptionalParameter("Motif collection", null, new Class[]{MotifCollection.class},"Use entries from the given collection");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return false; // return false instead of "(data instanceof MotifNumericMap)" to 'hide' the format 
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return false; // return false (MotifNumericMap.class.equals(dataclass));
    }

    @Override
    public boolean canParseInput(Data data) {
        return false;
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return false;
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "txt";
    }

    @Override
    public String[] getSuffixAlternatives() {return new String[]{"txt","csv","tsv"};}
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!(dataobject instanceof MotifNumericMap)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        MotifCollection collection;
        MotifNumericMap corethresholds;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             corethresholds=(MotifNumericMap)settings.getResolvedParameter("Core thresholds",defaults,engine);
             collection=(MotifCollection)settings.getResolvedParameter("Motif collection",defaults,engine);             
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
            corethresholds=(MotifNumericMap)getDefaultValueForParameter("Core thresholds");
            collection=(MotifCollection)getDefaultValueForParameter("Motif collection");         
        }

        MotifNumericMap matrixthresholds=(MotifNumericMap)dataobject;
        if (corethresholds==null) corethresholds=matrixthresholds; // use same if not set
        StringBuilder outputString=new StringBuilder();
        ArrayList<String> keys=(collection!=null)?collection.getAllMotifNames():engine.getNamesForAllDataItemsOfType(Motif.class);
        //Collections.sort(keys);
        int size=keys.size();
        int i=0;
        // output 4 line header
        outputString.append("Profile for searching with MATCH within MotifLab\n");
        outputString.append("temp.prf\n");
        outputString.append(" MIN_LENGTH 300\n");  // I don't know what this means but it must be included
        outputString.append("0.0\n"); // I don't know what this means but it must be included
        DecimalFormat formatter=DataFormat.getDecimalFormatter(3);
        for (String key:keys) { // for each entry
              outputString.append(" 1.000 ");
              outputString.append(formatter.format(corethresholds.getValue(key)));
              outputString.append(" ");              
              outputString.append(formatter.format(matrixthresholds.getValue(key)));
              outputString.append(" ");                
              outputString.append(key);
              outputString.append(" ");
              outputString.append(key);
              outputString.append("\n");
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%100==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        outputString.append("//\n"); // End of profile marker
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }




    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("Unable to parse input with DatFormat_TransfacProfile");
    }



}
