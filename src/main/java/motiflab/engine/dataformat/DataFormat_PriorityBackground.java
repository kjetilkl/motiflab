/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Iterator;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.BackgroundModel;
import motiflab.engine.data.OutputData;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_PriorityBackground extends DataFormat {
    private String name="PriorityBackground";
    private Class[] supportedTypes=new Class[]{BackgroundModel.class};

        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return data instanceof BackgroundModel;
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return dataclass.equals(BackgroundModel.class);
    }

    
    @Override
    public boolean canParseInput(Data data) {
        return data instanceof BackgroundModel;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return dataclass.equals(BackgroundModel.class);
    }    
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "pbg";
    } 
    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new BackgroundModel("Background");
        if (input.isEmpty()) throw new ParseError("Empty input document");
        if (input.size()<=1) throw new ParseError("No background model data in input document");
        Iterator iter=input.iterator();
        while (iter.hasNext()) { // remove blank lines
            String next=(String)iter.next();
            if (next.trim().isEmpty()) iter.remove();
        }
        double[] values=new double[input.size()];
        for (int i=0;i<input.size();i++) {
            String string=input.get(i);
            try {
                values[i]=Double.parseDouble(string);
            } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numerical value: "+string);}
        }       
        BackgroundModel model=BackgroundModel.convertPlainFormatToBackground(values);
        model.rename(target.getName()); // the new model returned from the conversion above is called "temporary"
        target.importData(model);
        return target;
    }


    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        BackgroundModel model=(BackgroundModel)dataobject;
        double[] frequencies=BackgroundModel.convertBackgroundToPlainFormat(model);
        StringBuilder outputString=new StringBuilder();
        for (int i=0;i<frequencies.length;i++) {
            outputString.append(frequencies[i]);
            outputString.append("\n");
        }
        outputobject.append(outputString.toString(),getName());
        setProgress(100);
        return outputobject;   
    }

}
