/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;

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

import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.OperationTask;
import motiflab.engine.SystemError;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.SequenceNumericMap;

/**
 *
 * @author kjetikl
 */
public class DataFormat_DRIMust extends DataFormat {
    private String name="DRIMust";

    private static final String SITES="sites";
    private static final String MOTIFS="motifs";
    
    
    
    private Class[] supportedTypes=new Class[]{RegionDataset.class,RegionSequenceData.class}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes

    
    public DataFormat_DRIMust() {
        addParameter("datatype", "sites", new String[]{"sites","motifs"},"specifies whether to parse and return motifs or binding sites");  
        addParameter("order", null, new Class[]{SequenceNumericMap.class},"This is needed to specify the order of the sequences in the DRIMust output");
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
        return (data instanceof RegionSequenceData || data instanceof RegionDataset); // The format can parse MotifCollections also but we do not want to announce that!
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class)); // The format can parse MotifCollections also but we do not want to announce that!
    }
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "log";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in DRIMust format (functionality not implemented)");
    }    
    
  
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       String returntype=MOTIFS;
       SequenceCollection sequenceCollection=null;
       SequenceNumericMap order=null;
       if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             returntype=(String)settings.getResolvedParameter("datatype",defaults,engine);   
             order=(SequenceNumericMap)settings.getResolvedParameter("order",defaults,engine);               
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           returntype=(String)getDefaultValueForParameter("datatype");
           order=(SequenceNumericMap)getDefaultValueForParameter("order");
       }
       if (task instanceof OperationTask) {
            String subsetName=(String)((OperationTask)task).getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
            if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
            Data seqcol=engine.getDataItem(subsetName);
            if (seqcol==null) throw new ParseError("No such collection: '"+subsetName+"'",task.getLineNumber());
            if (!(seqcol instanceof SequenceCollection)) throw new ParseError(subsetName+" is not a sequence collection",task.getLineNumber());
            sequenceCollection=(SequenceCollection)seqcol;
       }
       if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection();       
       // 
       if (returntype.equalsIgnoreCase(MOTIFS)) {
           MotifCollection collection;
           if (target!=null && target instanceof MotifCollection) collection=(MotifCollection)target;
           else collection=new MotifCollection("MotifCollection");
           int count=0;
           if (input.isEmpty()) return collection; // no motifs found
           ArrayList<String> sites=null;
           for (String line:input) {
               if (line.matches("mHG score = N.+")) { // marks start of motif
                    if (sites!=null) throw new ParseError("Unexpected motif header found");
                    else sites=new ArrayList<String>();
               }
               if (line.matches("^[AGCT]+$")) {
                   if (sites==null) throw new ParseError("Motif header not found");
                   else sites.add(line);
               }                      
               if (line.matches("motif\\s+mHG_score\\s+corrected_score.+")) { // Marks end of motif 
                   count++;
                   Motif newMotif=new Motif("motif"+count);
                   try {
                      String[] sequences=new String[sites.size()];
                      sequences=sites.toArray(sequences);
                      double[][] matrix=Motif.getPWMfromSites(sequences);
                      newMotif.setMatrix(matrix);
                   } catch (SystemError e) {
                       throw new ParseError(e.getMessage());
                   }
                   collection.addMotifToPayload(newMotif);
                   sites=null;
               }
           }
           if (sites!=null) throw new ParseError("Motif parsing not complete");
           return collection;
       } else if (returntype.equalsIgnoreCase(SITES)) {
          if (order==null) throw new ParseError("Missing 'order' parameter for DRIMust data format");
           //if (sequenceCollection==null) throw new ParseError("The order of the sequences in the output must be specified using a Sequence Collection");
          if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection(); // this is just temporary
          RegionDataset regiondataset;          
          if (target!=null && target instanceof RegionDataset) regiondataset=(RegionDataset)target;
          else regiondataset=new RegionDataset("sites");
           // add missing RegionSequenceData objects to set up the return dataset
          ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
          for (Data seq:sequences) {
              if (regiondataset.getSequenceByName(seq.getName())==null) regiondataset.addSequence(new RegionSequenceData((Sequence)seq));     
          }
          ArrayList<String> sequencesInOrder=sequenceCollection.getAllSequenceNames();
          order.sortAccordingToMap(sequencesInOrder);         
          ArrayList<RegionSequenceData> sequenceList=new ArrayList<RegionSequenceData>(); // this must contain the sequences in the order used by DRIMust!
          for (String seqName:sequencesInOrder) {
              RegionSequenceData seq=(RegionSequenceData)regiondataset.getSequenceByName(seqName);
              sequenceList.add(seq);
          }
          if (input.isEmpty()) {
              engine.logMessage("DRIMust did not find any motifs");
              return regiondataset;
          } // no sites found
          // now parse the lines
          Pattern siteline=Pattern.compile("([ACGT]+)\\s+(\\d+)\\s+(.)\\s+(\\d+)\\s+(\\d+)(\\s+(in_target_set|not_in_target_set))?");
          int count=1;
          String motiftype="motif"+count;
          for (int i=0;i<input.size();i++) {
               String line=input.get(i);
               if (line.startsWith("******")) { // separator between motifs
                   count++;
                   motiftype="motif"+count;
               } else {
                   Matcher matcher=siteline.matcher(line);  
                   if (matcher.matches()) {
                       String inSet=matcher.group(7);
                       if (inSet!=null && inSet.equals("not_in_target_set")) continue; // skip those that are not marked as "in_target_set" (these were not included in the PWM)
                       String bindingpattern=matcher.group(1);
                       int sequenceIndex=getIntegerNumber(matcher.group(2)); // 
                       String strandString=matcher.group(3);
                       int strand=Region.INDETERMINED;
                       if (strandString.equals("+")) strand=Region.DIRECT;
                       else if (strandString.equals("-")) strand=Region.REVERSE;
                       int startPos=getIntegerNumber(matcher.group(4)); // this is 0-offset
                       int endPos=getIntegerNumber(matcher.group(5)); // this is 0-offset
                       
                       if (strand==Region.REVERSE) bindingpattern=MotifLabEngine.reverseSequence(bindingpattern);
                       RegionSequenceData regionsequence=sequenceList.get(sequenceIndex);
                       Region newsite=new Region(regionsequence, startPos, endPos, motiftype, 1.0, strand);
                       newsite.setProperty("sequence", bindingpattern); //
                       regionsequence.addRegion(newsite);                        
                   }
               }                       
          }
          return regiondataset;
       } else throw new ParseError("Unknown returntype: "+returntype);
    }             
    
    
    private int getIntegerNumber(String string) throws ParseError{
        try {
           return Integer.parseInt(string); 
        } catch (NumberFormatException e) {
            throw new ParseError("Unable to parse expected integer number: "+string);
        }
    }
}