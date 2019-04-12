/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.analysis.Analysis;
/**
 *
 * @author kjetikl
 */
public class DataFormat_RawData extends DataFormat {
    public static final String RAWDATA="RawData";

    private Class[] supportedTypes=new Class[]{Analysis.class};

    public DataFormat_RawData() {

    }
        
    @Override
    public String getName() {
        return RAWDATA;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof Analysis);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (Analysis.class.isAssignableFrom(dataclass));
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
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!(dataobject instanceof Analysis)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+RAWDATA+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+RAWDATA+" format");
        }        
        setProgress(5);
        outputobject.setShowAsHTML(false);
        ((Analysis)dataobject).formatRaw(outputobject, engine, settings, task, this);
        return outputobject;
    }    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Inappropriate use of data format '"+getName()+"' to parse data");
    }    
    
    /** Orients a textstring vertically (for use in table headers) by inserting linebreaks
     *  between every letter in the original string
     */
    public static String orientStringVertically(String text) {
        StringBuilder buffer=new StringBuilder(text.length()*5);
        for (int i=0;i<text.length();i++) {
            buffer.append(text.charAt(i));
            buffer.append("<br>");
        }
        return buffer.toString();
    }

}

        
       
        
        
