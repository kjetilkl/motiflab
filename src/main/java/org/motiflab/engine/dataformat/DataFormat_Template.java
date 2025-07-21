/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.TextVariable;

/**
 *
 * @author kjetikl
 */
public class DataFormat_Template extends DataFormat {
    private String name="Template";

    private Class[] supportedTypes=new Class[]{TextVariable.class};
    
    public DataFormat_Template() {      

    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof TextVariable);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(TextVariable.class));
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
    public boolean isAppendable() {
        return true;
    }
    
   @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(1);    
        if (dataobject instanceof TextVariable) {
            ArrayList<String> lines=((TextVariable)dataobject).getAllStrings();
            int size=lines.size();
            int i=0;
            for (String line:lines) {
                outputobject.append(resolveReferences(line), null);
                outputobject.append("\n", null);
                setProgress(i+1, size);
                i++;
                if (i%100==0) {
                    if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                   
                    Thread.yield();
                }
            }
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        setProgress(100);        
        return outputobject;
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       throw new ParseError("Unable to parse input with data format Template");
    }
     
    private String resolveReferences(String text) throws ExecutionError {
        if (text.indexOf('{')>=0) { // there 
            int start=0;
            ArrayList<String> namedReferences=new ArrayList<String>();
            int startpos=text.indexOf('{',start);
            int endpos=text.indexOf('}',startpos);     
            while (startpos>=0 && endpos>startpos) {
                String ref=text.substring(startpos+1,endpos);
                start=endpos+1;
                if (!namedReferences.contains(ref)) namedReferences.add(ref);
                startpos=text.indexOf('{',start);
                endpos=text.indexOf('}',startpos);                   
            }
            for (String ref:namedReferences) {
                try {
                    String resolvedRef=engine.resolveDataReferences(ref, "\n");
                    if (resolvedRef!=null) text=text.replace("{"+ref+"}", resolvedRef);  
                } catch (Exception e) {
                    // do not report this error. Just let it slide and leave the original placeholder untouched
                }
            }
        }
        return text;
    } // end: resolveReferences
      
}

        
       
        
        
