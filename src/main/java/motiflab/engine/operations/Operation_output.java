/*
 
 
 */

package motiflab.engine.operations;

import motiflab.engine.task.OperationTask;
import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.ProgressListener;
import motiflab.engine.data.analysis.Analysis;
import motiflab.engine.data.*;
import motiflab.engine.dataformat.DataFormat;

/**
 *
 * @author kjetikl
 */
public class Operation_output extends Operation {
    public static final String OUTPUT_FORMAT="format";  
    public static final String DIRECT_OUTPUT="directOutput";  
    public static final String DIRECT_OUTPUT_REFERENCES="directOutputReferences";      
    public static final String OUTPUT_FORMAT_PARAMETERS="formatParameters";  
    private static final String name="output";
    private static final String description="Outputs data items to text files in selected data formats";
    private Class[] datasourcePreferences=new Class[]{FeatureDataset.class,FeatureSequenceData.class,Motif.class,MotifCollection.class,MotifPartition.class,Module.class,ModuleCollection.class,ModulePartition.class,BackgroundModel.class,MotifTextMap.class,MotifNumericMap.class,ModuleTextMap.class,ModuleNumericMap.class,NumericVariable.class, SequenceTextMap.class, SequenceNumericMap.class,Sequence.class,SequenceCollection.class,SequencePartition.class,ExpressionProfile.class,TextVariable.class,Analysis.class, OutputData.class};
    

    
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
    public boolean execute(final OperationTask task) throws Exception {
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();        
        if (targetName==null || targetName.isEmpty() || targetName.equals(sourceName)) targetName=engine.getDefaultOutputObjectName();
        String outputformatName=(String)task.getParameter(OUTPUT_FORMAT);      
        String directOutputString=(String)task.getParameter(DIRECT_OUTPUT);
        Data sourceData=engine.getDataItem(sourceName); 
        Data targetData=engine.getDataItem(targetName);
        task.setParameter(OperationTask.ENGINE, engine);
        OutputData doubleBuffer;
        if (targetData==null) doubleBuffer=new OutputData(targetName);            
        else {
            if (!(targetData instanceof OutputData)) throw new ExecutionError("Incompatible assignment: "+targetData.getName()+" already exists and is not an output object",task.getLineNumber());
            else {
                if (((OutputData)targetData).isHTMLformatted()) throw new ExecutionError("The selected output document '"+((OutputData)targetData).getName()+"' is HTML formatted and can not be appended to.\nPlease select a different target name");
                if (((OutputData)targetData).isBinary()) throw new ExecutionError("The selected output document '"+((OutputData)targetData).getName()+"' contains contents that can not be appended to.\nPlease select a different target name");
                doubleBuffer=(OutputData)targetData.clone();
            }
        }
        if (sourceData==null && directOutputString!=null) {
            String directOutputReferences=(String)task.getParameter(DIRECT_OUTPUT_REFERENCES);
            directOutput(directOutputString,directOutputReferences, doubleBuffer, task);
            return true;
        }
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError("Operation 'output' can not work on '"+sourceName+"'",task.getLineNumber());
       
        DataFormat formatter=null;
        if (outputformatName==null || outputformatName.isEmpty()) formatter=engine.getDefaultDataFormat(sourceData);
        else formatter=engine.getDataFormat(outputformatName);
        // if (formatter==null) throw new ExecutionError("Unknown output format '"+outputformatName+"'",task.getLineNumber());
        if (formatter!=null && !formatter.canFormatOutput(sourceData)) throw new ExecutionError("Unsupported output format '"+outputformatName+"' for data object '"+sourceName+"'",task.getLineNumber());
        ParameterSettings settings=(ParameterSettings)task.getParameter(Operation_output.OUTPUT_FORMAT_PARAMETERS);
        ProgressListener listener=new ProgressListener() {
            public void processProgressEvent(Object source, int progress) {
                task.setProgress(progress);
            }
        };     
        task.setStatusMessage("Executing operation: output");
        if (formatter!=null) {
            if (!formatter.isAppendable() && doubleBuffer.getDocument().getLength()>0) throw new ExecutionError("Text in format '"+formatter.getName()+"' can not be appended to existing output.\nPlease select a different target name");
            try {                 
               formatter.addProgressListener(listener);
               formatter.format(sourceData, doubleBuffer, settings, task);
            }
            catch (ExecutionError ee) {ee.setLineNumber(task.getLineNumber());throw ee;}
            catch (Exception e) {throw e;}
            finally {formatter.removeProgressListener(listener);}
        }
        else doubleBuffer.append(sourceData.output()+"\n","text");    
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {engine.updateDataItem(doubleBuffer);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
        task.setProgress(100);
        return true;
    }
    
    private void directOutput(String text, String references, OutputData targetData, OperationTask task) throws Exception {
        if (references!=null) {
           String[] referencelist=references.split("\\s*,\\s*");
           for (int i=0;i<referencelist.length;i++) {
              String ref=referencelist[i];
              text=text.replace("{"+(i+1)+"}", "{"+ref+"}");  // replace numbered references with named references instead. These will be resolved next :-)
              text=text.replace("{|"+(i+1)+"|}", "{|"+ref+"|}");              
           }           
        }  
        // check for "named references" and resolve if found
        text=resolveNamedReferences(text);
        
        String currentFormat=targetData.getDataFormat();
        targetData.append(text,currentFormat);
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {engine.updateDataItem(targetData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
        task.setProgress(100);        
    }
    
    public String resolveNamedReferences(String text) throws ExecutionError {
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
    }
    
    
    public static String processDirectOutput(String text, boolean fromSafeForm) {
        if (fromSafeForm) { // convert from safe protocol form to internal form (replacing e.g. \t with TABs, \n with newlines, \" with quotes and \\ with single backslash)
            text=MotifLabEngine.unescapeQuotedString(text);              
        } else { // convert from actual string to a string by escaping special characters so that the string can be safely enclosed in double quotes.
            text=MotifLabEngine.escapeQuotedString(text);       
        }
        return text;
    }
}

