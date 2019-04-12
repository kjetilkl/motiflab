/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import motiflab.engine.task.ExecutableTask;

import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputDataDependency;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.TextVariable;

/**
 *
 * @author kjetikl
 */
public class DataFormat_TemplateHTML extends DataFormat {
    private String name="TemplateHTML";
    private static final String HTML=DataFormat_HTML.HTML;

    private Class[] supportedTypes=new Class[]{TextVariable.class};
    
    public DataFormat_TemplateHTML() {      

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
        return "html";
    }

    @Override
    public boolean isAppendable() {
        return false;
    }
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       throw new ParseError("Unable to parse input with data format TemplateHTML");
    }
         
    
   @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(1);    
        if (dataobject instanceof TextVariable) {
            boolean hasHTMLheader=((TextVariable)dataobject).containsSubstring("<html>", false);
            if (!hasHTMLheader) engine.createHTMLheader("MotifLab", null, null, true, true, true, outputobject);
            ArrayList<String> lines=((TextVariable)dataobject).getAllStrings();
            int size=lines.size();
            int i=0;
            HashMap<OutputDataDependency,OutputDataDependency> processedDependencies=new HashMap<>(); // in case the references point to OutputData objects that contain dependencies that have to be carried over to this new OutputData object
            for (String line:lines) {
                outputobject.append(resolveReferences(line, outputobject, processedDependencies), HTML);
                setProgress(i+1, size);
                i++;
                if (i%100==0) {
                    if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                   
                    Thread.yield();
                }
            }
            if (!hasHTMLheader) {
                outputobject.append("</body>\n",HTML);
                outputobject.append("</html>\n",HTML);                
            }
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.setShowAsHTML(true);
        setProgress(100);        
        return outputobject;
    }


    private String resolveReferences(String text, OutputData outputobject, HashMap<OutputDataDependency,OutputDataDependency> processedDependencies) throws ExecutionError {
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
            for (String ref:namedReferences) { // note that <ref> can contain options also (preceded by colons)
                try {
                    if (ref.contains("->image#")) { // references a particular image within an OutputData object. Replace the reference with the filepath of the image
                        String[] parts=ref.split("->image#");
                        if (parts.length<2) continue;                       
                        OutputData object=(OutputData)engine.getDataItem(parts[0], OutputData.class);
                        int number=0;
                        try {number=Integer.parseInt(parts[1]);} catch (NumberFormatException e) {}   
                        if (number<=0 || object==null || object.getNumberOfDependencies()<number) continue; // no such dependency here...  
                        ArrayList<OutputDataDependency> imageDependencies=object.getDependencies(new String[]{"css","js"},false); // we skip any HTML related dependencies that might be present but return all others (even if they might not be images).
                        if (imageDependencies.size()<number) continue; // no such image dependency here
                        OutputDataDependency dependency=imageDependencies.get(number-1); // we use 1-indexing in the Template but they are 0-indexed internally
                        if (dependency==null) continue;
                        carryOverDependency(outputobject, dependency, processedDependencies);                                         
                        dependency=processedDependencies.get(dependency); // the new dependency (or old if shared)
                        String resolvedRef="file:///"+dependency.getInternalPathName(); //
                        if (resolvedRef!=null) text=text.replace("{"+ref+"}", resolvedRef); // this replaces ALL instances of {<ref>} with the resolved ref (image filepath)
                    } 
                    else {
                        String resolvedRef=engine.resolveDataReferences(ref, "<br>");
                        if (resolvedRef!=null) text=text.replace("{"+ref+"}", resolvedRef); // this replaces ALL instances of {<ref>} with the resolved ref 
                        // check if the reference is to an OutputData object. If so, all of its dependencies must be carried over to the new OutputData object
                        String dataname=(ref.contains(":"))?(ref.substring(ref.indexOf(":"))):ref;
                        OutputData sourceoutputobject=(OutputData)engine.getDataItem(dataname, OutputData.class);
                        if (sourceoutputobject!=null && sourceoutputobject.hasDependencies()) { 
                            for (OutputDataDependency dependency:sourceoutputobject.getDependencies()) {
                                carryOverDependency(outputobject, dependency, processedDependencies);                                                      
                                // The resolved String will contain the filenames of the original dependency so we must change this to the filename used by the new copy (unless it is shared)
                                if (!dependency.isShared()) { 
                                    OutputDataDependency copyDep=processedDependencies.get(dependency);
                                    String oldInternalPath=dependency.getInternalPathName();
                                    String newInternalPath=copyDep.getInternalPathName();
                                    text=text.replace(oldInternalPath, newInternalPath);                                    
                                }                             
                            }
                        }   
                    }
                } catch (Exception e) {
                    // do not report this error. Just let it slide and leave the original placeholder untouched
                }
            }
        }
        return text;
    } // end: resolveReferences
    
    private void carryOverDependency(OutputData outputobject, OutputDataDependency dependency, HashMap<OutputDataDependency,OutputDataDependency> processedDependencies) {
        if (processedDependencies.containsKey(dependency)) return; // already carried over
        if (dependency.isShared()) {
            outputobject.addSharedDependency(dependency); // no need to make an explicit copy, just add it
            processedDependencies.put(dependency,dependency);
        } else {
            OutputDataDependency copyDep=outputobject.copyDependency(engine, dependency); // copy the dependency into the new object
            processedDependencies.put(dependency,copyDep); // note that the copy will not have the same filename as the original
        }           
    }
      
}

        
       
        
        
