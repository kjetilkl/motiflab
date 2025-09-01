/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.protocol.ParseError;
/**
 *
 * @author kjetikl
 */
public class DataFormat_Excel extends DataFormat {
    public static final String EXCEL="Excel";
    private Class[] supportedTypes=new Class[]{Analysis.class};

    public DataFormat_Excel() {
//        addOptionalParameter("Embed images", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, all images referenced in the HTML document will be replaced by local copies. If the document references images with relative paths they should be embedded!");
//        setParameterFilter("Embed images","input");
    }
        
    @Override
    public String getName() {
        return EXCEL;
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
        return "xlsx";
    }

    @Override
    public boolean isAppendable() {
        return false;
    }
    
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!(dataobject instanceof Analysis)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+EXCEL+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+EXCEL+" format");
        }        
        setProgress(5);
        outputobject.setBinary(true);
        ((Analysis)dataobject).formatExcel(outputobject, engine, settings, task, this);
        task.setProgress(95);
        return outputobject;
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Inappropriate use of data format '"+getName()+"' to parse data");
    }
    
    
    
}