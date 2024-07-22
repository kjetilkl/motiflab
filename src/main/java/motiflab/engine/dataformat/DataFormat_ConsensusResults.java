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
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.SequenceCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_ConsensusResults extends DataFormat {
    private String name="ConsensusResults";

    private static final String SITES="sites";
    private static final String MOTIFS="motifs";
    
    
    
    private Class[] supportedTypes=new Class[]{RegionDataset.class,RegionSequenceData.class}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes

    
    public DataFormat_ConsensusResults() {
        addParameter("datatype", "sites", new String[]{"sites","motifs"},"specifies whether to parse and return motifs or binding sites");  
        addParameter("Sequences", null, new Class[]{SequenceCollection.class},"This is needed to specify the order of the sequences in the ConsensusResults output");
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
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in ConsensusResults format (functionality not implemented)");
    }    
    
  
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       String returntype=MOTIFS;
       SequenceCollection sequenceCollection=null;
       if (input.isEmpty()) throw new ParseError("Unable to parse empty results file");
       if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             returntype=(String)settings.getResolvedParameter("datatype",defaults,engine);              
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           returntype=(String)getDefaultValueForParameter("datatype");
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
           int motifcount=0; int motifsOK=0;
           boolean A_OK=false,C_OK=false,G_OK=false,T_OK=false;
           String[] matrixlines=new String[4]; // 0=A,1=C,2=G,3=T   
           boolean processing=false;         
           for (String line:input) {
               if (line.startsWith("THE LIST OF MATRICES FROM FINAL CYCLE")) {processing=true; continue;}
               if (!processing) continue;
               if (line.startsWith("MATRIX ")) {
                   motifcount++;                   
                   A_OK=false;C_OK=false;G_OK=false;T_OK=false;
                   matrixlines=new String[4];
               }
               else if (line.startsWith("A |")) {
                   matrixlines[0]=line.substring(3);
                   A_OK=true;
               } else if (line.startsWith("C |")) {
                   matrixlines[1]=line.substring(3);
                   C_OK=true;
               } else if (line.startsWith("G |")) {
                   matrixlines[2]=line.substring(3);
                   G_OK=true;
               } else if (line.startsWith("T |")) {
                   matrixlines[3]=line.substring(3);
                   T_OK=true;
               }  
               if (A_OK && C_OK && G_OK && T_OK) { // found all matrix lines. Now process them
                   int motifsize=0;
                   double[][] matrix=null; // first is position, second is base (4) 
                   for (int i=0;i<4;i++) {
                      String[] split=matrixlines[i].trim().split("\\s+");
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
                   Motif newMotif=new Motif("motif"+motifcount);
                   newMotif.setMatrix(matrix);
                   collection.addMotifToPayload(newMotif);   
                   A_OK=false;C_OK=false;G_OK=false;T_OK=false;
                   motifsOK++;
               } // end found all matrix lines 
           }
           if (motifcount!=motifsOK) throw new ParseError("Missing matrix information about (at least) one of the motifs");        
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
          boolean processing=false;
          int motifcount=0;
          String motifname="";
          Pattern siteline=Pattern.compile("^\\s*(\\d+)\\|(\\d+)\\s+:\\s+(-)?(\\d+)/(\\d+)\\s+(\\w+)");
          for (int i=0;i<input.size();i++) {
               int lineNumber=i+1;
               String line=input.get(i);               
               if (line.startsWith("THE LIST OF MATRICES FROM FINAL CYCLE")) {processing=true;continue;}
               if (!processing) continue;
                   if (line.startsWith("MATRIX ")) {
                       motifcount++;     
                       motifname="motif"+motifcount;
                   }               
                   Matcher matcher=siteline.matcher(line);  
                   if (matcher.matches()) {
                       String noIdea1=matcher.group(1);
                       String noIdea2=matcher.group(2);
                       String strand=matcher.group(3);
                       String sequenceNumber=matcher.group(4);
                       String startPos=matcher.group(5);                      
                       String bindingpattern=matcher.group(6);
                       int seqIndex=-1;
                       int start=0;
                       double score=1;
                       int orientation=(strand!=null && strand.equals("-"))?Region.REVERSE:Region.DIRECT;
                       try {
                           seqIndex=Integer.parseInt(sequenceNumber);
                           seqIndex--; // because MotifLab uses zero-indexing and Consensus uses one-indexing
                       } catch (NumberFormatException e) {throw new ParseError("Unable to parse sequence index: "+sequenceNumber, lineNumber);}
                       try {
                           start=Integer.parseInt(startPos);
                       } catch (NumberFormatException e) {throw new ParseError("Unable to parse start position for TFBS: "+startPos, lineNumber);}
//                       try {
//                           score=Double.parseDouble(scoreString);
//                       } catch (NumberFormatException e) {throw new ParseError("Unable to parse score value: "+scoreString);}
                       int motifsize=bindingpattern.length();
                       String sequenceName=sequenceCollection.getSequenceNameByIndex(seqIndex);
                       if (sequenceName==null) throw new ParseError("Unable to figure out which sequence has index number "+seqIndex, lineNumber);
                       RegionSequenceData regionsequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceName);
                       Region newsite=new Region(regionsequence, start, start+motifsize-1, motifname, score, orientation);
                       if (orientation==Region.REVERSE) bindingpattern=MotifLabEngine.reverseSequence(bindingpattern);
                       newsite.setProperty("sequence", bindingpattern); //
                       regionsequence.addRegion(newsite);   
                 } // else throw new ParseError("Unable to parse line for binding site: "+line);
                   else System.err.println(line);
                                      
           }
           return regiondataset;
       } else throw new ParseError("Unknown returntype: "+returntype);
    }             
    
}