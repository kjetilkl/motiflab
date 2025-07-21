/*
 
 
 */

package org.motiflab.engine.dataformat;


import java.util.ArrayList;
import java.util.Iterator;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.ExpressionProfile;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleNumericMap;
import org.motiflab.engine.data.ModulePartition;
import org.motiflab.engine.data.ModuleTextMap;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.MotifNumericMap;
import org.motiflab.engine.data.MotifPartition;
import org.motiflab.engine.data.MotifTextMap;
import org.motiflab.engine.data.NumericVariable;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.SequenceNumericMap;
import org.motiflab.engine.data.SequencePartition;
import org.motiflab.engine.data.SequenceTextMap;
import org.motiflab.engine.data.TextVariable;
import org.motiflab.engine.protocol.ParseError;


/**
 * A Simple 'default' DataFormat that can format any Data object in its 'standard plain format'
 * simply by calling the data-objects output() method
 * @author kjetikl
 */
public class DataFormat_Plain extends DataFormat {
    private String name="Plain";
    private static String SKIP_COMMENTS="Skip comments";
    private Class[] supportedTypes=new Class[]{Data.class};
        
    public DataFormat_Plain() {
        addOptionalParameter(SKIP_COMMENTS, "", null,"If this parameter is non-empty, lines starting with the given prefix are ignored when parsed.");
        setParameterFilter(SKIP_COMMENTS,"input");       
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return true;
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return true;
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return (   data instanceof SequenceCollection 
                || data instanceof SequencePartition 
                || data instanceof MotifCollection 
                || data instanceof MotifPartition                 
                || data instanceof ModuleCollection 
                || data instanceof ModulePartition                 
                || data instanceof TextVariable 
                || data instanceof NumericVariable 
                || data instanceof SequenceNumericMap 
                || data instanceof MotifNumericMap 
                || data instanceof ModuleNumericMap
                || data instanceof SequenceTextMap 
                || data instanceof MotifTextMap 
                || data instanceof ModuleTextMap                 
                || data instanceof ExpressionProfile
                || data instanceof OutputData
         );
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (   dataclass.equals(SequenceCollection.class) 
                || dataclass.equals(SequencePartition.class) 
                || dataclass.equals(MotifCollection.class) 
                || dataclass.equals(MotifPartition.class)      
                || dataclass.equals(ModuleCollection.class)                 
                || dataclass.equals(ModulePartition.class) 
                || dataclass.equals(TextVariable.class) 
                || dataclass.equals(NumericVariable.class) 
                || dataclass.equals(SequenceNumericMap.class) 
                || dataclass.equals(MotifNumericMap.class) 
                || dataclass.equals(ModuleNumericMap.class) 
                || dataclass.equals(SequenceTextMap.class) 
                || dataclass.equals(MotifTextMap.class) 
                || dataclass.equals(ModuleTextMap.class)                 
                || dataclass.equals(ExpressionProfile.class)
                || dataclass.equals(OutputData.class)
         );
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
        String text=dataobject.output().trim();
        outputobject.append(text+"\n",getName());
        return outputobject;
    }    
           
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {        
        String commentPrefix="";
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              commentPrefix=(String)settings.getResolvedParameter(SKIP_COMMENTS,defaults,engine);          
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
            commentPrefix=(String)getDefaultValueForParameter(SKIP_COMMENTS);
        }     
        if (commentPrefix!=null && !commentPrefix.isEmpty()) {// remove comment lines
            Iterator<String> iter=input.iterator();
            while(iter.hasNext()) {
                String line=iter.next();
                if (line!=null && line.startsWith(commentPrefix)) iter.remove();
            }
        }      
        target.inputFromPlain(input, engine);
        return target;
    }
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
         throw new ParseError("Unable to parse input to DataSegment in Plain format)");
    }
   
}

        
       
        
        
