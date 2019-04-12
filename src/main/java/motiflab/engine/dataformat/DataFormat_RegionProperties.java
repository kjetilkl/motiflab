/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Region;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Parameter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.protocol.ParseError;

/**
 * A custom data format for Region Datasets where the user explicitly specifies the output format using field codes that are substituted for property values
 * @author kjetikl
 */
public class DataFormat_RegionProperties extends DataFormat {
    private String name="Region_Properties";

    private static final Pattern fieldcode=Pattern.compile("\\{(.+?)\\}");
    
    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    
    public DataFormat_RegionProperties() {
        addParameter("Layout", "One region per line", new String[]{"One sequence per line","One region per line"},"<html><ul><li><b>One sequence per line</b>: All regions from the same sequence is output on a single line which consists of the specified<br>sequence information followed by the sequence delimiter and then followed by a list of regions (separated by the region delimiter).</li><li><b>One region per line</b>: Each region is output on a separate line which consists of the specified sequence information<br>followed by the sequence delimiter and then the region information</li></ul></html>");
        addOptionalParameter("Sequence format", "{sequencename}", null,"<html>This parameter specifies the information to output for each sequence.<br>In additional to literal text, the format string can contain <i>property codes</i> surrounded by braces, e.g. <tt>{CHROMOSOME}</tt>.<br>These property codes will be replaced by the corresponding property values of the sequence in the output.<br>Some standard property codes include: SEQUENCENAME, START, END and STRAND (standard properties are case-insensitive)<br>See the on-line documentation for names of additional standard properties.<br>Property codes can also refer to user-defined properties, but these will be case-sensitive.<br>Use <tt><b>\\t</b></tt> to insert tabs and <tt><b>\\n</b></tt> to insert newlines.<br>If you leave the field empty it will take on the default value, but you can set it to <tt>*</tt> (single asterisk) to signal that the field should not be output at all.<html>");        
        addOptionalParameter("Region format", "{type}\\t{sequence:chromosome string}\\t{genomic start}\\t{genomic end}\\t{score}\\t{orientation sign}", null,"<html>This parameter specifies the information to output for each region.<br>In additional to literal text, the format string can contain <i>property codes</i> surrounded by braces, e.g. <tt>{TYPE}</tt>.<br>These property codes will be replaced by the corresponding property values of the region in the output.<br>Some standard property codes include: START, END, SCORE and STRAND (standard properties are case-insensitive)<br>See the on-line documentation for names of additional standard properties.<br>You can prefix the property code with \"sequence:\" to refer to properties of the parent sequence (e.g. <tt>{sequence:chromosome}</tt>)<br>or, if the region <i>type</i> is a motif, you can prefix with \"motif:\" to refer to properties of the associated motif (e.g. <tt>{motif:long name}</tt>).<br>Property codes can also refer to user-defined properties, but these will be case-sensitive.<br>Use <tt><b>\\t</b></tt> to insert tabs and <tt><b>\\n</b></tt> to insert newlines.<br>If you leave the field empty it will take on the default value, but you can set it to <tt>*</tt> (single asterisk) to signal that the field should not be output at all.<html>");        
        addOptionalParameter("Sequence delimiter", "\\t", null,"<html>The delimiter text which separates the sequence information from the region information. The default is a TAB (<tt><b>\\t</b></tt>).<br>If you leave the field empty it will take on the default value, but you can set it to <tt>*</tt> (single asterisk) to signal that the field should not be output at all.</html>");
        addOptionalParameter("Region delimiter", "\\t", null,"<html>The delimiter text which separates the information of different regions.<br>This only applies when multiple regions are output to the same line. The default is a TAB (<tt><b>\\t</b></tt>).<br>If you leave the field empty it will take on the default value, but you can set it to <tt>*</tt> (single asterisk) to signal that the field should not be output at all.</html>");
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof RegionSequenceData || data instanceof RegionDataset);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class));
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
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);        
        String sequenceFormat,sequenceDelimiter, regionFormat, regionDelimiter;
        String layout;
        boolean oneSequencePerLine=false;
        
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             sequenceFormat=(String)settings.getResolvedParameter("Sequence format",defaults,engine); 
             sequenceDelimiter=(String)settings.getResolvedParameter("Sequence delimiter",defaults,engine);
             regionFormat=(String)settings.getResolvedParameter("Region format",defaults,engine);
             regionDelimiter=(String)settings.getResolvedParameter("Region delimiter",defaults,engine);             
             layout=(String)settings.getResolvedParameter("Layout",defaults,engine);         
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           sequenceFormat=(String)getDefaultValueForParameter("Sequence format");
           sequenceDelimiter=(String)getDefaultValueForParameter("Sequence delimiter");
           regionFormat=(String)getDefaultValueForParameter("Region format");
           regionDelimiter=(String)getDefaultValueForParameter("Region delimiter");
           layout=(String)getDefaultValueForParameter("Layout");
        }
        oneSequencePerLine=(layout.equalsIgnoreCase("one sequence per line"));
        
        if (sequenceFormat==null    || sequenceFormat.equals("*"))    sequenceFormat="";
        if (regionFormat==null      || regionFormat.equals("*"))      regionFormat="";
        if (sequenceDelimiter==null || sequenceDelimiter.equals("*")) sequenceDelimiter="";
        if (regionDelimiter==null   || regionDelimiter.equals("*"))   regionDelimiter="";
        
        sequenceFormat=unescapeStuff(sequenceFormat);
        sequenceDelimiter=unescapeStuff(sequenceDelimiter);
        regionFormat=unescapeStuff(regionFormat);
        regionDelimiter=unescapeStuff(regionDelimiter);
        
        String outputString="";
        if (dataobject instanceof RegionDataset) {
            SequenceCollection sequenceCollection=null;
            if (task instanceof OperationTask) {
                String subsetName=(String)((OperationTask)task).getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
                if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
                Data seqcol=engine.getDataItem(subsetName);
                if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
                if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
                sequenceCollection=(SequenceCollection)seqcol;
            }
            if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection();                   
            outputString=outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, sequenceFormat, sequenceDelimiter, regionFormat, regionDelimiter, oneSequencePerLine, task, engine);
        } else if (dataobject instanceof RegionSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((RegionSequenceData)dataobject, sequenceFormat, sequenceDelimiter, regionFormat, regionDelimiter, oneSequencePerLine, task, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }    

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, String sequenceFormat, String sequenceDelimiter, String regionFormat, String regionDelimiter, boolean oneSequencePerLine, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence, sequenceFormat, sequenceDelimiter, regionFormat, regionDelimiter, oneSequencePerLine, task, outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }    
    
    
    /** output-formats a single sequence */
    private void outputSequence(RegionSequenceData regionsequence,  String sequenceFormat, String sequenceDelimiter, String regionFormat, String regionDelimiter, boolean oneSequencePerLine, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
        int count=0;
        Sequence sequence=(Sequence)MotifLabEngine.getEngine().getDataItem(regionsequence.getSequenceName(), Sequence.class);
        String sequenceString="ERROR:unknown sequence '"+regionsequence.getSequenceName()+"'";
        if (sequence!=null) sequenceString=getSequenceString(sequence, sequenceFormat);
        if (!sequenceString.isEmpty()) sequenceString+=sequenceDelimiter;
        
        for (Region region:regionsequence.getAllRegions()) {
           if (count%200==0) {
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              Thread.yield();
           }
           if (count==0 || !oneSequencePerLine) outputString.append(sequenceString);
           if (count>0 && oneSequencePerLine) outputString.append(regionDelimiter);
           outputRegion(region, regionFormat, outputString);               
           if (!oneSequencePerLine) outputString.append("\n");
           count++;
        }
        if (oneSequencePerLine) outputString.append("\n");
    }


    private void outputRegion(Region region, String formatString, StringBuilder outputString) {
       String line=formatString;
       // replace field codes with actual properties
       HashMap<String,String> fields=new HashMap<String, String>();
       Matcher matcher=fieldcode.matcher(line);  
       while (matcher.find()) {
           String code=matcher.group(1);
           Object value=region.getProperty(code);
           if (value!=null) fields.put(code,value.toString());
           else fields.put(code,""); 
       }
       for (String key:fields.keySet()) {
           line=line.replace("{"+key+"}", fields.get(key));
       }     
       outputString.append(line);
    }
    
    private String getSequenceString(Sequence sequence, String formatString) {
       String line=formatString;
       // replace field codes with actual properties
       HashMap<String,String> fields=new HashMap<String, String>();
       Matcher matcher=fieldcode.matcher(line);  
       while (matcher.find()) {
           String code=matcher.group(1);
           Object value=null;
           try {value=sequence.getPropertyValue(code,MotifLabEngine.getEngine());} catch (ExecutionError e) {}
           if (value!=null) fields.put(code,value.toString());
           else fields.put(code,""); 
       }
       for (String key:fields.keySet()) {
           line=line.replace("{"+key+"}", fields.get(key));
       }     
       return line;
    }    


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Unable to parse input in "+name+" format");
    }    
    
    private String unescapeStuff(String string) {
        string=string.replace("\\\\", "\\"); // escaped \
        string=string.replace("\\t", "\t"); // escaped TAB
        string=string.replace("\\n", "\n"); // escaped newline 
        string=string.replace("\\s", " "); // escaped space         
        return string;
    }
    
}

        
       
        
        
