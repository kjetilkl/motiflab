/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_ChIPMunk extends DataFormat {
    private String name="ChIPMunk";

    private static final String SITES="sites";
    private static final String MOTIFS="motifs";
    private static final String PARAMETER_STRAND_ORIENTATION="Strand orientation";    
    
    
    
    private Class[] supportedTypes=new Class[]{RegionDataset.class,RegionSequenceData.class}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes

    
    public DataFormat_ChIPMunk() {
        addParameter("datatype", "sites", new String[]{"sites","motifs"},"specifies whether to parse and return motifs or binding sites");  
        //addParameter("Sequences", null, new Class[]{SequenceCollection.class},"This is needed to specify the order of the sequences in the ChIPMunk output");
        addParameter(PARAMETER_STRAND_ORIENTATION, "Direct", new String[]{"Relative","Direct"},null);   
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
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in ChIPMunk format (functionality not implemented)");
    }    
    
  
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       String returntype=MOTIFS;
       String orientationString="Direct";
       SequenceCollection sequenceCollection=null;
       if (input.isEmpty()) throw new ParseError("Unable to parse empty results file");
       if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             returntype=(String)settings.getResolvedParameter("datatype",defaults,engine);   
             orientationString=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);               
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           returntype=(String)getDefaultValueForParameter("datatype");
           orientationString=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);           
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
           int motifsize=0;
           boolean A_OK=false,C_OK=false,G_OK=false,T_OK=false;
           String[] matrixlines=new String[4]; // 0=A,1=C,2=G,3=T   
           
           double[][] matrix=null; // first is position, second is base (4)          
           for (String line:input) {
               if (line.startsWith("A|")) {
                   matrixlines[0]=line.substring(2);
                   A_OK=true;
               } else if (line.startsWith("C|")) {
                   matrixlines[1]=line.substring(2);
                   C_OK=true;
               } else if (line.startsWith("G|")) {
                   matrixlines[2]=line.substring(2);
                   G_OK=true;
               } else if (line.startsWith("T|")) {
                   matrixlines[3]=line.substring(2);
                   T_OK=true;
               } else 
               if (A_OK && C_OK && G_OK && T_OK) break; // found all matrix lines
           }
           if (!A_OK) throw new ParseError("Missing matrix line for base 'A'");
           if (!C_OK) throw new ParseError("Missing matrix line for base 'C'");
           if (!G_OK) throw new ParseError("Missing matrix line for base 'G'");
           if (!T_OK) throw new ParseError("Missing matrix line for base 'T'");
           for (int i=0;i<4;i++) {
              String[] split=matrixlines[i].split("\\s+");
              if (matrix==null) {
                  motifsize=split.length;
                  matrix=new double[motifsize][4];
              } else if (split.length!=motifsize) throw new ParseError("Expected "+motifsize+" columns but "+split.length+" found for: "+matrixlines[i]);
              for (int j=0;j<motifsize;j++) {
                  try {
                      double value=Double.parseDouble(split[j]);
                      matrix[j][i]=value;
                  } catch (NumberFormatException e) {
                      throw new ParseError("Unable to parse expected numeric matrix entry: "+split[j]);
                  }
              }
           }           
           // ChIPMunk only finds one motif (at a time)
           Motif newMotif=new Motif("motif1");
           newMotif.setMatrix(matrix);
           collection.addMotifToPayload(newMotif);           
           return collection;
       } else if (returntype.equalsIgnoreCase(SITES)) {
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
          // now parse the lines
          Pattern siteline=Pattern.compile("WORD\\|(\\d+)\\s+(\\d+)\\s+(\\w+)\\s+(\\S+)\\s+(direct|revcomp)\\s+(\\S+)");
          for (int i=0;i<input.size();i++) {
               int linenumber=i+1;
               String line=input.get(i);
               if (line.startsWith("ERRR|java.lang.RuntimeException:")) {
                   line=line.substring("ERRR|java.lang.RuntimeException:".length());
                   throw new ParseError(line);
               }
               if (line.startsWith("WORD|")) {
                   Matcher matcher=siteline.matcher(line);  
                   if (matcher.matches()) {
                       String seqString=matcher.group(1);
                       String startPos=matcher.group(2);
                       String bindingpattern=matcher.group(3);
                       String scoreString=matcher.group(4);
                       String somenumber=matcher.group(6);
                       int seqIndex=-1;
                       int start=0;
                       double score=0;
                       double somevalue=0;
                       int orientation=(matcher.group(5).equals("direct"))?Region.DIRECT:Region.REVERSE;
                       try {
                           seqIndex=Integer.parseInt(seqString);
                       } catch (NumberFormatException e) {throw new ParseError("Unable to parse sequence index: "+seqString, linenumber);}
                       try {
                           start=Integer.parseInt(startPos);
                       } catch (NumberFormatException e) {throw new ParseError("Unable to parse start position for TFBS: "+startPos, linenumber);}
                       try {
                           score=Double.parseDouble(scoreString);
                       } catch (NumberFormatException e) {throw new ParseError("Unable to parse score value: "+scoreString, linenumber);}
                       try {
                           somevalue=Double.parseDouble(somenumber);
                       } catch (NumberFormatException e) {throw new ParseError("Unable to parse last value on line: "+somenumber, linenumber);}
                       int motifsize=bindingpattern.length();
                       String sequenceName=sequenceCollection.getSequenceNameByIndex(seqIndex);
                       if (sequenceName==null) throw new ParseError("Unable to figure out which sequence has index number "+seqIndex, linenumber);
                       RegionSequenceData regionsequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceName);
                       int startPosition=start; // offset from start of sequence
                       int endPosition=start+motifsize-1;                         
                       if (orientationString.equalsIgnoreCase("Relative") && regionsequence.getStrandOrientation()==Sequence.REVERSE) {                     
                           orientation=orientation*(-1); // flip orientation
                           int sequenceSize=regionsequence.getSize();
                           endPosition=sequenceSize-(start+1);
                           startPosition=endPosition-motifsize+1;
                       }  
                       Region newsite=new Region(regionsequence, startPosition, endPosition, "motif1", score, orientation);
                       if (orientation==Region.REVERSE) bindingpattern=MotifLabEngine.reverseSequence(bindingpattern);
                       newsite.setProperty("sequence", bindingpattern); //
                       regionsequence.addRegion(newsite);                       
                 } else throw new ParseError("Unable to parse line for binding site: "+line, linenumber);
              }                        
           }
           return regiondataset;
       } else throw new ParseError("Unknown returntype: "+returntype);
    }             
    
}