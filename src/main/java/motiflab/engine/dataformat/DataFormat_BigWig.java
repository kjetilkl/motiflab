/*
 
 
 */

package motiflab.engine.dataformat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericSequenceData;
import org.broad.igv.bbfile.BBFileHeader;
import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.WigItem;

/**
 * This Dataformat parses BigWig files.
 * It makes use of the BigWig API found here:  http://code.google.com/p/bigwig/
 * which is available under the GNU Lesser GPL license
 * 
 * @author kjetikl
 */
public class DataFormat_BigWig extends DataFormat {
    private String name="BigWig";

    private Class[] supportedTypes=new Class[]{NumericSequenceData.class, NumericDataset.class};

    public DataFormat_BigWig() {
        addOptionalParameter("CHR prefix", Boolean.TRUE,  new Boolean[]{ Boolean.TRUE, Boolean.FALSE},"Select this option if the chromosomes in the file are prefixed with 'chr' (e.g. chromosome 12 is named 'chr12' rather than just '12'");                  
       // addAdvancedParameter("Coordinate system", "BED", new String[]{"BED","GFF"},"In the default BED coordinate-system the chromosome starts at position 0 and the end-coordinate is exclusive, whereas in the GFF-system the chromosome starts at 1 and the end-coordinate is inclusive.");     
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
        return (data instanceof NumericSequenceData || data instanceof NumericDataset);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(NumericSequenceData.class) || dataclass.equals(NumericDataset.class));
    }

    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "bw";
    }
    @Override
    public String[] getSuffixAlternatives() {return new String[]{"bw","bigwig"};}

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());      
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("System Error: Inappropriate use of parseInput(ArrayList<String>...) method in BigWig format");

    }

    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        throw new ParseError("System Error: Inappropriate use of parseInput(ArrayList<String>...) method in BigWig format");
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
        if (target==null) target=new NumericDataset("temporary");
        if (target instanceof NumericDataset) { // The NumericDataset might not contain NumericSequenceData objects for all Sequences. Add them if they are missing!
            NumericDataset dataset=(NumericDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new NumericSequenceData((Sequence)seq,0));
            }
        }
        boolean addChr=false;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             addChr=(Boolean)settings.getResolvedParameter("CHR prefix",defaults,engine);                                     
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           addChr=(Boolean)getDefaultValueForParameter("CHR prefix");                        
        }         
        //open big file       
        BBFileReader reader=null;
        try {
            reader=new BBFileReader(filename);
        } catch (IOException e) {
            throw new ParseError(e.getMessage());
        }
        //get the big header
        BBFileHeader bbFileHdr = reader.getBBFileHeader();
        if(!bbFileHdr.isHeaderOK()) {
            throw new ParseError("Bad header for: "+filename);
        }
        if(!(bbFileHdr.isBigWig())){
            throw new ParseError("Not a valid BigWig file: "+filename);
        }        

        if (target instanceof NumericSequenceData) {
            NumericSequenceData targetSequence=(NumericSequenceData)target;
            String chromosome=targetSequence.getChromosome(); 
            int start=targetSequence.getRegionStart();
            int end=targetSequence.getRegionEnd(); 
            parseSequenceSegment((NumericSequenceData)target,chromosome, start, end, addChr, reader,task);
        } else if (target instanceof NumericDataset) { // add values to all applicable sequences (those that cover the genomic region)
            int size=((NumericDataset)target).getNumberofSequences();
            int i=0;
            ArrayList<FeatureSequenceData> sequences=((NumericDataset)target).getAllSequences();
            for (FeatureSequenceData seq:sequences) {
                String chromosome=seq.getChromosome(); 
                int start=seq.getRegionStart();
                int end=seq.getRegionEnd(); 
                parseSequenceSegment((NumericSequenceData)seq, chromosome, start, end, addChr, reader,task);
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
            parseSequenceSegment((DataSegment)target, chromosome, start, end, addChr, reader,task);
        } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-numeric track as target for BigWig dataformat: "+target.getClass().getSimpleName());
        
        try {reader.getBBFis().close();} catch (Exception e) {}
        
        return target;
    }

    private void parseSequenceSegment(Object target, String chromosome, int start, int end, boolean addChr, BBFileReader reader, ExecutableTask task) throws ParseError {
       start--; // subtract 1 because the BED file is 0-indexed
       Set<String> set=new HashSet<String>(reader.getChromosomeNames());
       
       String originalChromosome=chromosome;       
       boolean chromosomeFound = set.contains(chromosome);
              
       if (!chromosomeFound && addChr) { // try different versions of chr-prefix. Search until chromosome is found
           String chromosomeWithoutPrefix=chromosome;
           if (chromosomeWithoutPrefix.toLowerCase().startsWith("chr")) chromosomeWithoutPrefix=chromosomeWithoutPrefix.substring(3); // strip prefix if it is already there
           String[] prefixes=new String[]{"chr","","Chr","CHR"}; // try different versions of the prefix and also without
           for (String prefix:prefixes) {
               chromosome=prefix+chromosomeWithoutPrefix;
               if (set.contains(chromosome)) {
                   chromosomeFound=true;
                   break;
               }
           }   
       }         
           
       if(chromosomeFound){
           BigWigIterator iter=null;
           iter=reader.getBigWigIterator(chromosome,start,chromosome,end,true);
           boolean warned=false; // only give warning once
           while(iter.hasNext()) {
                WigItem f=iter.next();
                if (f==null) {
                    if (!warned) {engine.logMessage("Warning: iterator in BigWig parser returned NULL");warned=true;}
                    continue;                                       
                }        
                Object[] map=new Object[]{f.getChromosome(),f.getStartBase()+1,f.getEndBase(),(double)f.getWigValue()};
                addRegionToTarget(target,map);
            }               
       } else task.setStatusMessage("BigWig file '"+reader.getBBFilePath()+"' doesn't contain chromosome: "+originalChromosome);        
             
    }
    
    /**
     * 
     * @param target This would be a NumericSequenceData object or DataSegment
     * @param map
     * @throws ParseError 
     */
    private void addRegionToTarget(Object target, Object[] map) throws ParseError {
        String chromosome=(String)map[0];
        int start=(Integer)map[1]; // these should now be regular genomic coordinates (GFF-coordinates)
        int end=(Integer)map[2]; // these should now be regular genomic coordinates(GFF-coordinates)
        double value=(Double)map[3];
        if (target instanceof NumericSequenceData) {
            for (int i=start;i<=end;i++) {
                ((NumericSequenceData)target).setValueAtGenomicPosition(i, value);
            }
        }
        else if (target instanceof DataSegment) {
            int targetStart=((DataSegment)target).getSegmentStart();
            start-=targetStart; // convert to relative coordinates
            end-=targetStart;            
            ((DataSegment)target).addNumericValue(start,end,value);
        } else throw new ParseError("Target object neither NumericSequenceData nor DataSegment in DataFormat_BedGraph.addRegionToTarget():"+target.toString());
    }

}





