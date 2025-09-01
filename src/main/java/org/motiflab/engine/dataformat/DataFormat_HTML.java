/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.analysis.Analysis;
import org.motiflab.engine.datasource.DataRepositoryFile;
import org.apache.commons.io.FileUtils;
/**
 *
 * @author kjetikl
 */
public class DataFormat_HTML extends DataFormat {
    public static final String HTML="HTML";
    private Class[] supportedTypes=new Class[]{Analysis.class, TextVariable.class};

    public DataFormat_HTML() {
        addOptionalParameter("Embed images", Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, all images referenced in the HTML document will be replaced by local copies. NOTE: If the document references images using relative paths they must be embedded!");
        setParameterFilter("Embed images","input");
    }
        
    @Override
    public String getName() {
        return HTML;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof Analysis || data instanceof TextVariable);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (Analysis.class.isAssignableFrom(dataclass) || TextVariable.class.equals(dataclass));
    }
    
    @Override
    public boolean canParseInput(Data data) {
        if (data instanceof OutputData) return true;
        return false;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        if (dataclass.equals(OutputData.class)) return true;      
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
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+HTML+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+HTML+" format");
        }        
        setProgress(5);
        outputobject.setShowAsHTML(true);
        if (dataobject instanceof Analysis) ((Analysis)dataobject).formatHTML(outputobject, engine, settings, task, this);
        else if (dataobject instanceof TextVariable) outputobject.append(((TextVariable)dataobject).getAllStrings(), HTML);
        return outputobject;
    }    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new OutputData("temp");
        else if (!(target instanceof OutputData)) throw new ParseError("Unable to parse input to target data of type '"+target.getTypeDescription()+"' using DataFormat_HTML");
        boolean embedImages=false;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             embedImages=(Boolean)settings.getResolvedParameter("Embed images",defaults,engine);         
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
           embedImages=(Boolean)getDefaultValueForParameter("Embed images");
        }
        // first just copy the contents of the input into the HTML document
        int count=0;
        StringBuffer buffer=new StringBuffer();
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               Thread.yield();
            }  
            buffer.append(line);
            buffer.append("\n"); // this is necessary since the newline is sometimes used as a space between words
            if (count%20==0 && task!=null) task.setProgress(count, input.size()*2); // go up to 50% in progress here            
        }
        // now see if there are any images that need embedding   
        HashMap<String,String> imagemap=new HashMap<String, String>();        
        if (embedImages) {
          // first determine which images are present and create an internal copy of each one
          // this is done because the same image can be referenced several times, but we only need one copy of each image
          StringBuffer newBuffer = new StringBuffer(); 
          try {                 
             int imageCount=0;
             Pattern pattern = Pattern.compile("(<\\s*img .*?src\\s*=\\s*\")(.+?)(\".*?>)", Pattern.DOTALL);
             Matcher  matcher = pattern.matcher(buffer);
             while (matcher.find()) {
                 imageCount++;                 
                 String url=matcher.group(2);
                 if (imagemap.containsKey(url)) continue; // this has already been created
                 String imagesuffix=url.substring(url.lastIndexOf('.')+1); // this may break if URL does not contain a dot
                 File dependentfile=((OutputData)target).createDependentBinaryFile(engine, imagesuffix);   
                 imagemap.put(url, "file:///"+dependentfile.getAbsolutePath()); // local temp-file
                 // now read the image file and store it in the dependency file
                 boolean hasProtocol=false;
                 try {
                     URL extUrl=new URL(url);
                     String protocol=extUrl.getProtocol();
                     hasProtocol=(protocol!=null && !protocol.isEmpty());
                 } catch (MalformedURLException mfux) {}
                 if (!hasProtocol) { // the URL is a relative reference
                     Object sourceFile=((OutputData)target).getSourceFile();                     
                     if (sourceFile==null) throw new ParseError("Unable to resolve relative reference in HTML document");
                     if (sourceFile instanceof String) sourceFile=engine.getFile((String)sourceFile);
                     if (sourceFile instanceof File && !(sourceFile instanceof DataRepositoryFile)) { // regular file. cna be copied normally                     
                         File imageFile=new File(((File)sourceFile).getParent(), url);
                         FileUtils.copyFile(imageFile, dependentfile); // not sure if this will work for repository files    
                     } else if (sourceFile instanceof DataRepositoryFile) {                    
                         File imageFile=MotifLabEngine.getFile(((DataRepositoryFile)sourceFile).getParentFile(), url);
                         MotifLabEngine.copyFile(imageFile, dependentfile); // not sure if this will work for repository files    
                     } else if (sourceFile instanceof URL) {
                         URL newURL=new URL((URL)sourceFile,url);
                         FileUtils.copyURLToFile(newURL, dependentfile);
                     }                     
                 }
                 else FileUtils.copyURLToFile(new URL(url), dependentfile);
             }    
          } catch (Exception e) {
              e.printStackTrace(System.err);
              throw new ParseError(e.getClass().getSimpleName()+":"+e.getMessage());
          }
          
          // now go through the document again and replace original image references with new internal references
          Pattern pattern = Pattern.compile("(<\\s*img .*?src\\s*=\\s*\")(.+?)(\".*?>)", Pattern.DOTALL);
          Matcher  matcher = pattern.matcher(buffer);
          while (matcher.find()) {              
             String url=matcher.group(2);
             String dependendFilename=imagemap.get(url);      
             // replace the external URL with the new internal url
             String newURL=matcher.group(1)+dependendFilename+matcher.group(3);
             newURL=newURL.replace("\\", "\\\\" ); // this is needed for the appendReplacement to work
             matcher.appendReplacement(newBuffer, newURL); // appendReplacement(buffer, newURL);
          }
          matcher.appendTail(newBuffer);  
          buffer=newBuffer;
        }
        ((OutputData)target).append(buffer.toString(), HTML);
        ((OutputData)target).setShowAsHTML(true);
        return target;        
    }    
    
    
    /** Orients a text string vertically (for use in table headers) by inserting linebreaks
     *  between every letter in the original string
     */
    public static String orientStringVertically(String text) {
        StringBuilder buffer=new StringBuilder(text.length()*5);
        for (int i=0;i<text.length();i++) {
            buffer.append(text.charAt(i));
            buffer.append("<br />");
        }
        return buffer.toString();
    }

}

        
       
        
        
