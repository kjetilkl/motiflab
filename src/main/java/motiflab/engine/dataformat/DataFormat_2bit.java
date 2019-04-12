/*
 
 
 */

package motiflab.engine.dataformat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.DNASequenceData;

/**
 * This Dataformat parses DNA files in 2bit format.
 * 
 * @author kjetikl
 */
public class DataFormat_2bit extends DataFormat {
    private String name="2bit";

    private Class[] supportedTypes=new Class[]{DNASequenceData.class, DNASequenceDataset.class};

    public DataFormat_2bit() {
        addOptionalParameter("CHR prefix", Boolean.TRUE,  new Boolean[]{ Boolean.TRUE, Boolean.FALSE},"Select this option if the chromosomes in the file are prefixed with 'chr' (e.g. chromosome 12 is named 'chr12' rather than just '12'");          
        addAdvancedParameter("Keep masks", Boolean.FALSE,  new Boolean[]{ Boolean.TRUE, Boolean.FALSE},"If selected, lowercase letters in the DNA sequence will be kept as is. If not selected, all bases will be in uppercase.");      
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return false;
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return false;
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof DNASequenceData || data instanceof DNASequenceDataset);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(DNASequenceData.class) || dataclass.equals(DNASequenceDataset.class));
    }

    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "2bit";
    }


    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());      
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("System Error: Inappropriate use of parseInput(ArrayList<String>...) method in 2bit format");

    }

    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("System Error: Inappropriate use of parseInput(ArrayList<String>...) method in 2bit format");
    }
    
    @Override
    public boolean canOnlyParseDirectlyFromLocalFile() {
        return true;
    }
    
    @Override
    public DataSegment parseInput(String filename, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        Object result=parseInputToTarget(filename, target, settings, task); 
        return (DataSegment)result;
    }  
    
    @Override
    public Data parseInput(String filename, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        Object result=parseInputToTarget(filename, target, settings, task); 
        return (Data)result;
    }      

    /** The following method is a common substitute for the above 2 methods */
    private Object parseInputToTarget(String filename, Object target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) target=new DNASequenceDataset("temporary");
        if (target instanceof DNASequenceDataset) { // The String might not contain DNASequenceData objects for all Sequences. Add them if they are missing!
            DNASequenceDataset dataset=(DNASequenceDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new DNASequenceData((Sequence)seq,null)); // do not set the buffers yet!
            }
        }
        boolean keepMasks=false;
        boolean addChr=false;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             keepMasks=(Boolean)settings.getResolvedParameter("Keep masks",defaults,engine);
             addChr=(Boolean)settings.getResolvedParameter("CHR prefix",defaults,engine);             
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
             keepMasks=(Boolean)getDefaultValueForParameter("Keep masks");
             addChr=(Boolean)getDefaultValueForParameter("CHR prefix");             
        }         
        //open big file       
        TwoBitParser filereader=null;
        File file=engine.getFile(filename);
        try {
            filereader=new TwoBitParser(file);
        } catch (Exception e) {
            throw new ParseError(e.getMessage());
        }
        try {
            if (target instanceof DNASequenceData) {
                DNASequenceData targetSequence=(DNASequenceData)target;
                String chromosome=targetSequence.getChromosome(); 
                int start=targetSequence.getRegionStart();
                int end=targetSequence.getRegionEnd(); 
                parseSequenceSegment((DNASequenceData)target,chromosome, start, end, addChr, keepMasks, filereader,task);
            } else if (target instanceof DNASequenceDataset) { // add values to all applicable sequences (those that cover the genomic region)
                int size=((DNASequenceDataset)target).getNumberofSequences();
                int i=0;
                ArrayList<FeatureSequenceData> sequences=((DNASequenceDataset)target).getAllSequences();
                for (FeatureSequenceData seq:sequences) {
                    String chromosome=seq.getChromosome(); 
                    int start=seq.getRegionStart();
                    int end=seq.getRegionEnd(); 
                    parseSequenceSegment((DNASequenceData)seq, chromosome, start, end, addChr, keepMasks, filereader, task);
                    if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                    if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                    if (i%3==0) Thread.yield();
                    setProgress(i+1,size);
                    i++;                
                }
            } else if (target instanceof DataSegment) {
                String chromosome=((DataSegment)target).getChromosome(); 
                int start=((DataSegment)target).getSegmentStart();
                int end=((DataSegment)target).getSegmentEnd();
                parseSequenceSegment((DataSegment)target, chromosome, start, end, addChr, keepMasks, filereader,task);
            } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-DNA track as target for 2bit dataformat: "+target.getClass().getSimpleName());
        } catch (ParseError p) {
            throw p;
        } finally {
           try {filereader.close();} catch(IOException e){}
           try {filereader.closeParser();} catch(Exception e){}            
        }  
        return target;
    }

    private void parseSequenceSegment(Object target, String chromosome, int start, int end, boolean addChr, boolean keepMasks, TwoBitParser reader, ExecutableTask task) throws ParseError {
        if (!(target instanceof DNASequenceData || target instanceof DataSegment)) throw new ParseError("System Error: Target is neither DNASequenceData nor DataSegment");
        
        if (addChr) {
           String originalChromosome=chromosome;
           String chromosomeWithoutPrefix=chromosome;
           if (chromosomeWithoutPrefix.toLowerCase().startsWith("chr")) chromosomeWithoutPrefix=chromosomeWithoutPrefix.substring(3); // strip prefix if it is already there
           String[] prefixes=new String[]{"chr","","Chr","CHR"}; // try different versions of the prefix and also without
           int prefixIndex=0;
           boolean found=false;
           while (!found) {
               chromosome=prefixes[prefixIndex]+chromosomeWithoutPrefix;
               try {
                  char[] buffer=reader.loadFragmentAsBuffer(chromosome, start, end, keepMasks);
                  if (target instanceof DNASequenceData) {
                      ((DNASequenceData)target).setSequenceData(buffer);
                  } else if (target instanceof DataSegment) {
                      ((DataSegment)target).setSegmentData(buffer);
                  } 
                  found=true; // no need to try other prefixes
               } catch (Exception e) {
                   if (e.getMessage().contains("was not found")) {
                      if (prefixIndex==prefixes.length-1) throw new ParseError("Sequence ["+originalChromosome+"] (and variations thereof) was not found in 2bit file");
                      else prefixIndex++; // try with next prefix
                   }
               }                
           }
       }       
       else { // no prefix trouble
           try {
              char[] buffer=reader.loadFragmentAsBuffer(chromosome, start, end, keepMasks);
              if (target instanceof DNASequenceData) {
                  ((DNASequenceData)target).setSequenceData(buffer);
              } else if (target instanceof DataSegment) {
                  ((DataSegment)target).setSegmentData(buffer);
              } 
           } catch (Exception e) {
               throw new ParseError(e.getMessage());
           } 
       } // end: else no prefix
    }
    

}





