/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
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
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.SequenceCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_GTF extends DataFormat {
    private String name="GTF";
        
    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};
    
    public DataFormat_GTF() { 
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
        return (data instanceof RegionSequenceData || data instanceof RegionDataset);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class));
    }    
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "gtf";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
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
            outputString=outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, task, engine);
        } else if (dataobject instanceof RegionSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((RegionSequenceData)dataobject, task, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }    
    
    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence, task, outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    } 
    
    
    /** output-formats a single sequence */
    private void outputSequence(RegionSequenceData sequence, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
        ArrayList<Region> regionList=sequence.getAllRegions();
        //String sequenceName=sequence.getName();
        String chromosome=sequence.getChromosome();
        String featureName=sequence.getParent().getName();
        int count=0;
        for (Region region:regionList) {
           count++;
           if (count%200==0) {
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              Thread.yield();
           }                
           String strand=".";
           int start=region.getGenomicStart();
           int end=region.getGenomicEnd();    
           String type=region.getType();
                if (region.getOrientation()==Region.DIRECT)  strand="+";
           else if (region.getOrientation()==Region.REVERSE) strand="-"; 
           String attributes="gene_id \""+type+"\"; transcript_id \""+type+"\";";
           String line="chr"+chromosome+"\t"+featureName+"\tCDS\t"+start+"\t"+end+"\t"+region.getScore()+"\t"+strand+"\t.\t"+attributes;               
           outputString.append(line+"\n");
        }
    }
    

    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
         return (Data)parseInputToTarget(input, target, task);         
    }
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        return (DataSegment)parseInputToTarget(input, target, task);
    }
    
    /** The following method is a common substitute for the above 2 methods */
    private Object parseInputToTarget(ArrayList<String> input, Object target, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) target=new RegionDataset("temporary");
        if (target instanceof RegionDataset) { // The RegionDataset might not contain RegionSequenceData objects for all Sequences. Add them if they are missing!
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));     
            }            
        }
        int count=0;
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
               Thread.yield();
            }     
            if (line.startsWith("#ERROR:")) throw new ParseError(line.substring("#ERROR:".length()).trim(), count);            
            if (line.startsWith("#") || line.isEmpty()) continue; // GTF comment line
            HashMap<String,Object> map=parseSingleLineInStandardFormat(line, count);
            // check that start<=end in the map. Else assume reverse strand
            if (map.get("START") instanceof Integer && map.get("END") instanceof Integer) {
                int start=(Integer)map.get("START");
                int end=(Integer)map.get("END");
                if (start>end) {
                    int swap=start;
                    start=end;
                    end=swap;
                    map.put("START", start);
                    map.put("END", end);
                    map.put("STRAND", "-");
                }
            }            
            String regionchromosome=(String)map.get("CHROMOSOME");
            int regionstart=(Integer)map.get("START");
            int regionend=(Integer)map.get("END");
            RegionSequenceData targetSequence=null;
            if (target instanceof RegionSequenceData) {             
               targetSequence=(RegionSequenceData)target;
               if (!targetSequence.getChromosome().equals(regionchromosome)) continue;
               if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
               addRegionToTarget(targetSequence,map);
            } else if (target instanceof RegionDataset) { // add region to all applicable sequences              
                ArrayList<FeatureSequenceData> sequences=((RegionDataset)target).getAllSequences();
                for (FeatureSequenceData seq:sequences) {
                    targetSequence=(RegionSequenceData)seq;
                    if (!targetSequence.getChromosome().equals(regionchromosome)) continue;
                    if (regionstart>targetSequence.getRegionEnd() || regionend<targetSequence.getRegionStart()) continue;
                    addRegionToTarget(targetSequence,map);                    
                }
            } else if (target instanceof DataSegment) {
                if (!((DataSegment)target).getChromosome().equals(regionchromosome)) continue; 
                if (regionstart>((DataSegment)target).getSegmentEnd() || regionend<((DataSegment)target).getSegmentStart()) continue;               
                addRegionToTarget(target,map);
            } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-Region data as target for GTF dataformat: "+target.getClass().getSimpleName());
        }
        return target;      
    }
    
    
    private void addRegionToTarget(Object target, HashMap<String,Object> map) throws ParseError {                
        int start=0, end=0; // these are offset relative to the start of the parent sequence or segment
        int targetStart=0;
        if (target instanceof RegionSequenceData) {
            targetStart=((RegionSequenceData)target).getRegionStart();
        } else if (target instanceof DataSegment) {
            targetStart=((DataSegment)target).getSegmentStart();                   
        } else throw new ParseError("Target object neither RegionSequenceData nor DataSegment in DataFormat_GTF.addRegionToTarget():"+target.toString());
        Object startValue=map.get("START");
        if (startValue instanceof Integer) start=(Integer)startValue;
        Object endValue=map.get("END");
        if (endValue instanceof Integer) end=(Integer)endValue;
        double score=0;
        Object scoreValue=map.get("SCORE");
        if (scoreValue instanceof Double) score=(Double)scoreValue;
        String type=(String)map.get("TYPE"); if (type==null) type="unknown_type";
        String annotatedOrientation=(String)map.get("STRAND"); // the orientation in the GTF file              
        int orientation=Sequence.DIRECT;                    
        start-=targetStart;
        end-=targetStart;
             if (annotatedOrientation.equals("+")) orientation=Region.DIRECT; 
        else if (annotatedOrientation.equals("-")) orientation=Region.REVERSE; 
        else orientation=Region.INDETERMINED;   

        RegionSequenceData parentSequence=null;
        if (target instanceof RegionSequenceData) parentSequence=(RegionSequenceData)target;                        
        Region newRegion=new Region(parentSequence, start, end, type, score, orientation);
        for (String property:map.keySet()) {
            if (property.equalsIgnoreCase("CHROMOSOME") || 
                property.equalsIgnoreCase("FEATURE") || 
                property.equalsIgnoreCase("SOURCE") || 
                property.equalsIgnoreCase("START") || 
                property.equalsIgnoreCase("END") || 
                property.equalsIgnoreCase("SCORE") || 
                property.equalsIgnoreCase("TYPE") || 
                property.equalsIgnoreCase("STRAND")) continue;
            else {
                newRegion.setProperty(property, map.get(property));
            }                    
        }
        // System.err.println("Add region: "+newRegion.toString());
        if (target instanceof RegionSequenceData) ((RegionSequenceData)target).addRegion(newRegion);   
        else if (target instanceof DataSegment) ((DataSegment)target).addRegion(newRegion);
    }
       
    
    /** parses a single line in a GTF-file and returns a HashMap with the different properties (with values as strings!) according to the capturing groups in the formatString */
    private HashMap<String,Object> parseSingleLineInStandardFormat(String line, int lineNumber) throws ParseError {
        HashMap<String,Object> result=new HashMap<String,Object>();
        String[] fields=line.split("\t");
        if (fields.length!=9) throw new ParseError("Expected at 9 fields per line in GTF-format. Got "+fields.length+":\n"+line, lineNumber);
        String chromosome=fields[0];
        if (chromosome.startsWith("chr")) chromosome=chromosome.substring(3);
        int underscorepos=fields[1].indexOf('_');
        if (underscorepos>=0 && underscorepos<fields[1].length()-1) fields[1]=fields[1].substring(underscorepos+1);
        result.put("CHROMOSOME",chromosome);
        result.put("SOURCE",fields[1]); // this is correct
        result.put("FEATURE",fields[2]);
        try {
            result.put("START",Integer.parseInt(fields[3]));        
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+e.getMessage(), lineNumber);}
        try {
            result.put("END",Integer.parseInt(fields[4]));        
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+e.getMessage(), lineNumber);}
        try {
            result.put("SCORE",Double.parseDouble(fields[5]));        
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+e.getMessage(), lineNumber);}
        
        result.put("STRAND",fields[6]);
        String[] attributes=fields[8].split(";\\s*");
        for (String attribute:attributes) {
            attribute=attribute.trim();
            String[] pair=attribute.split(" ",2);
            if (pair.length!=2) throw new ParseError("Attribute not in recognized '<key> <value>' format: "+attribute, lineNumber); 
            String key=pair[0].trim();
            String value=pair[1].trim();
            if (value.startsWith("\"")) value=value.substring(1);
            if (value.endsWith("\"")) value=value.substring(0,value.length()-1);
            if (key.equals("gene_id")) result.put("TYPE",value);
            //result.put(key,value);
        }           
        
        return result;
    }


}

        
       
        
        
