/*
 
 
 */

package motiflab.engine.dataformat;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Parameter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.Organism;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.protocol.ParseError;


/**
 *
 * @author kjetikl
 */
public class DataFormat_Consensus extends DataFormat {
    private String name="Consensus";
    private Class[] supportedTypes=new Class[]{DNASequenceData.class, DNASequenceDataset.class};

    private static final String PARAMETER_HEADER="Header";
    private static final String PARAMETER_STRAND_ORIENTATION="Strand orientation";
    private static final String HEADER_SEQUENCENAME_ONLY="Sequence name";
    private static final String HEADER_NAME_LOCATION_STRAND="Name|Location|Strand";
    private static final String HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD="Name|Location|Strand|organism:build";
    private static final String EXTRA_NEWLINE="Extra space"; // do not change the name of this parameter without also changing the name in XML-files for MDscan and BioProspector. Those programs have problems with extra empty lines
    //public static final String ADD_SEQUENCES_FROM_FILE="addSequencesFromFile"; // this flag is used to indicate that the sequence objects themselves should be based on the data in the Consensus file
    
    
    public DataFormat_Consensus() {
        addParameter(PARAMETER_STRAND_ORIENTATION, "Relative", new String[]{"Relative","Direct","Reverse"},null);
        addOptionalParameter(PARAMETER_HEADER, HEADER_SEQUENCENAME_ONLY, new String[]{HEADER_SEQUENCENAME_ONLY,HEADER_NAME_LOCATION_STRAND,HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD},null);
        addOptionalParameter(EXTRA_NEWLINE, Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Add an extra empty line after each sequence to separate them visually");
        setParameterFilter(PARAMETER_HEADER,"output");        
        setParameterFilter(EXTRA_NEWLINE,"output");        
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof DNASequenceData || data instanceof DNASequenceDataset);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(DNASequenceData.class) || dataclass.equals(DNASequenceDataset.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof DNASequenceData || data instanceof DNASequenceDataset); // || data instanceof SequenceCollection);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(DNASequenceData.class) || dataclass.equals(DNASequenceDataset.class)); // || dataclass.equals(SequenceCollection.class));
    }

    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "con";
    }
    @Override
    public String[] getSuffixAlternatives() {return new String[]{"con","consensus","txt"};}

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        String orientation;
        String headerformat;
        boolean extraNewlines=false;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);          
              headerformat=(String)settings.getResolvedParameter(PARAMETER_HEADER,defaults,engine);   
              extraNewlines=(Boolean)settings.getResolvedParameter(EXTRA_NEWLINE,defaults,engine);   
           } catch (ExecutionError e) {
              throw e;
           } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
           }
        } else {
            orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
            headerformat=(String)getDefaultValueForParameter(PARAMETER_HEADER);
            extraNewlines=(Boolean)getDefaultValueForParameter(EXTRA_NEWLINE);
        }
        String outputString="";
        if (dataobject instanceof DNASequenceDataset) {
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
            outputString=outputMultipleSequences((DNASequenceDataset)dataobject, sequenceCollection, orientation, headerformat, extraNewlines, task, engine);
        } else if (dataobject instanceof DNASequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((DNASequenceData)dataobject, orientation, headerformat,false,engine,builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.trim()+"\n",getName());
        setProgress(100);
        return outputobject;
    }    
    
    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(DNASequenceDataset sourcedata, SequenceCollection collection, String orientation, String headerformat, boolean extraNewline, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              DNASequenceData sourceSequence=(DNASequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence, orientation,headerformat,extraNewline, engine,outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }
    
    
    
    
    /** outputformats a single sequence */
    private void outputSequence(DNASequenceData data, String orientation, String headerformat, boolean extraNewline, MotifLabEngine engine, StringBuilder outputString) {
        String sequence;
        String shownOrientation;
        if (orientation==null) orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
        if (headerformat==null) headerformat=(String)getDefaultValueForParameter(PARAMETER_HEADER);
        if (orientation.equals("Reverse") || ((orientation.equals("Relative")||orientation.equals("From Sequence")||orientation.equals("From Gene")) && data.getStrandOrientation()==Sequence.REVERSE)) {
            sequence=MotifLabEngine.reverseSequence(data.getSequenceAsString());
            shownOrientation="Reverse strand";
        }
        else {
            sequence=data.getSequenceAsString();
            shownOrientation="Direct strand";
        }
        String header=data.getName();
        if (headerformat.equals(HEADER_NAME_LOCATION_STRAND)) header=data.getName()+"|"+data.getRegionAsString()+"|"+shownOrientation;
        else if (headerformat.equals(HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD)) {
            Sequence s=(Sequence)engine.getDataItem(data.getSequenceName());                
            header=data.getName()+"|"+data.getRegionAsString()+"|"+shownOrientation+"|"+s.getOrganism()+":"+s.getGenomeBuild();
        }

        outputString.append(header);
        outputString.append(" ");
        outputString.append("\\");                
        outputString.append(sequence);
        outputString.append("\\");                 
        outputString.append("\n");

        if (extraNewline) outputString.append("\n");
    }

        
    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        //if (target instanceof SequenceCollection) return parseInputAsSequenceCollection(input, (SequenceCollection)target, settings, task);
        if (target==null) {
            target=new DNASequenceDataset("temporary");
        }
        setupofDNADataset((DNASequenceDataset)target);
        if (input.isEmpty()) return target; // throw new ParseError("Empty input");
        String orientation;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);          
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
            orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
        }
        if (target instanceof DNASequenceData) target=parseSingleSequenceInput(input, (DNASequenceData)target, orientation);
        else if (target instanceof DNASequenceDataset) target=parseMultipleSequenceInput(input, (DNASequenceDataset)target, orientation, task);
        else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-DNASequenceData passed as parameter to parseInput in DataFormat_Consensus");
        return target;
    }
    
    
    private DNASequenceData parseSingleSequenceInput(ArrayList<String> input, DNASequenceData target, String useorientation) throws ParseError, InterruptedException {
        String line=input.get(0);

        String[] parts=getHeaderAndSequence(line);
        if (parts==null) throw new ParseError("Unable to parse line: "+line);
        String header=parts[0];
        String buffer=parts[1];
        Object[] headerparts=getSequenceInfoFromHeader(header);
        if (headerparts==null) throw new ParseError("Unable to parse header: "+header);   
        String targetName=(String)headerparts[0];      
        if (targetName==null) throw new ParseError("Unable to extract sequence name from header: "+line);
        if (targetName.equals(target.getSequenceName())) {
            if (buffer.length()!=target.getSize()) throw new ParseError("Length of Consensus sequence for "+target.getSequenceName()+" ("+buffer.length()+" bp) does not match expected length ("+target.getSize()+" bp)");
            if (useorientation.equals("Reverse") || ((useorientation.equals("Relative")||useorientation.equals("From Sequence")||useorientation.equals("From Gene")) && target.getStrandOrientation()==Sequence.REVERSE)) buffer=MotifLabEngine.reverseSequence(buffer);
            for (int i=0;i<buffer.length();i++) {
                target.setValueAtRelativePosition(i, buffer.charAt(i));
            }           
        }
        return target;
    }
    

    private DNASequenceDataset parseMultipleSequenceInput(ArrayList<String> input, DNASequenceDataset target, String useorientation, ExecutableTask task) throws ParseError, InterruptedException {
        int lines=0;
        for (String line:input) {
            line=line.trim();
            if (line.isEmpty()) continue;            
            lines++;
            if (lines%3==0) { // yield every 3 sequences (arbitrary choice)
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
            }   
            String[] parts=getHeaderAndSequence(line);
            if (parts==null) throw new ParseError("Unable to parse line: "+line);
            String header=parts[0];
            String buffer=parts[1];           
            Object[] headerparts=getSequenceInfoFromHeader(header);
            if (headerparts==null) throw new ParseError("Unable to parse header: "+header);   
            String targetName=(String)headerparts[0];      
            if (targetName==null) throw new ParseError("Unable to extract sequence name from header: "+line);
            DNASequenceData targetseq=(DNASequenceData)target.getSequenceByName(targetName);
            if (targetseq==null) continue; // unknown sequence            
            if (buffer.length()!=targetseq.getSize()) throw new ParseError("Length of Consensus sequence for '"+targetName+"' ("+buffer.length()+" bp) does not match expected length ("+targetseq.getSize()+" bp)");
            if (useorientation.equals("Reverse") || ((useorientation.equals("Relative")||useorientation.equals("From Sequence")||useorientation.equals("From Gene")) && targetseq.getStrandOrientation()==Sequence.REVERSE)) buffer=MotifLabEngine.reverseSequence(buffer);
            for (int i=0;i<buffer.length();i++) {
                targetseq.setValueAtRelativePosition(i, buffer.charAt(i));
            }                                 
        }
        return target;
    }    

//    private SequenceCollection parseInputAsSequenceCollection(ArrayList<String> input, SequenceCollection target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
//         DNASequenceDataset dnaset=parseNewSequenceDatasetFromFASTA(input, "temp", settings, false);
//         return engine.extractSequencesFromFeatureDataset(dnaset, target);
//    }
//    /** 
//     * This will parse and return a DNASequenceDataset without any existing Sequence objects to match the read data against
//     * @param rename If TRUE the method will rename any sequences which does not have legal names.
//     *               Illegal character in the sequencename will be replaced by underscores, e.g. "YMR173W-A" will be renamed to "YMR173W_A"
//     */
//    public DNASequenceDataset parseNewSequenceDatasetFromFASTA(ArrayList<String> input, String datasetname, ParameterSettings settings, boolean rename) throws ParseError, InterruptedException {
//        if (input.size()<1) throw new ParseError("Empty input document");
//        String headerline=input.get(0);
//        if (!(headerline.startsWith(">"))) throw new ParseError("Unrecognized header for FASTA format: "+headerline);       
//        String useorientation;
//        if (settings!=null) {
//           try {
//              Parameter[] defaults=getParameters();
//              useorientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);          
//           } catch (Exception ex) {
//              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
//           }
//        } else {
//            useorientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
//        }
//        DNASequenceDataset target=new DNASequenceDataset(datasetname);
//        int first=-1; int last=0; int size=input.size();
//        ArrayList<int[]> startandstop=new ArrayList<int[]>();
//        for (int i=0;i<size;i++) {
//            String line=input.get(i).trim();
//            if (line.startsWith(">")) {
//                if (first<0) {
//                    first=i;
//                } else {
//                    last=i;
//                    startandstop.add(new int[]{first,last});
//                    first=i;
//                    last=0;
//                }
//            }
//        }
//        if (first>=0) {
//            startandstop.add(new int[]{first,size});
//        }
//        int counter=0;
//        if (first<0) return target; // sequence not found in file
//        for (int[] pair:startandstop) {
//            int start=pair[0]; int end=pair[1];
//            if (end<=start+1) continue; // no sequence?
//            counter++;
//            Object[] info=getSequenceInfoFromHeader(input.get(start));
//            String sequencename=(String)info[0];
//            if (sequencename.indexOf(' ')>0) {
//                sequencename=sequencename.substring(0,sequencename.indexOf(' '));
//            }
//            //if (sequencename==null) throw new ParseError("Unable to extract sequence name from header: "+input.get(start));
//            if (sequencename==null) sequencename="Sequence"+counter;
//            else {
//                if (sequencename.matches(".*[^a-zA-Z_0-9].*") && rename) { // sequencename contains illegal characters
//                    if (engine.autoCorrectSequenceNames()) {
//                        String newsequencename=MotifLabEngine.convertToLegalName(sequencename);
//                        engine.logMessage("NOTE: sequence '"+sequencename+"' was renamed to '"+newsequencename+"'");
//                        sequencename=newsequencename;                        
//                    } else throw new ParseError("The sequence name '"+sequencename+"' contains illegal characters. Note that it is possible to turn on auto-correct from the Config->Options menu.");
//                }
//                String error=engine.checkSequenceNameValidity(sequencename, false);
//                if (error!=null) throw new ParseError("Invalid name for sequence '"+sequencename+"' : "+error);
//            }
//            if (target.getSequenceByName(sequencename)!=null) throw new ParseError("The file contains multiple sequences named '"+sequencename+"'");
//            int expectedlength=0;
//            if (info[2]!=null && info[3]!=null) {
//                int seqend=(Integer)info[3];
//                int seqstart=(Integer)info[2];
//                expectedlength=seqend-seqstart+1;
//            }
//            int sequenceOrientation=Sequence.DIRECT;
//            if  (info[4]!=null) sequenceOrientation=(Integer)info[4];
//            StringBuilder complete=new StringBuilder();
//            for (int j=start+1;j<end;j++) complete.append(input.get(j).trim());
//            if (expectedlength>0 && expectedlength!=complete.length()) throw new ParseError("Length of Consensus sequence for "+sequencename+" ("+complete.length()+" bp) does not match expected length ("+expectedlength+" bp)");
//            if (useorientation.equals("Reverse") || ((useorientation.equals("Relative")||useorientation.equals("From Sequence")||useorientation.equals("From Gene")) && sequenceOrientation==Sequence.REVERSE)) complete=MotifLabEngine.reverseSequence(complete);
//            char[] buffer=new char[complete.length()];
//            for (int  i=0;i<complete.length();i++) {
//                buffer[i]=complete.charAt(i);
//            } 
//            int startPos=1;
//            int endPos=complete.length();
//            String chromosome="?";
//            int orientation=Sequence.DIRECT;
//            
//            if (info[1]!=null) chromosome=(String)info[1]; 
//            if (info[2]!=null) startPos=(Integer)info[2]; 
//            if (info[2]!=null && info[3]!=null) endPos=(Integer)info[3]; 
//            if (info[4]!=null) orientation=(Integer)info[4]; 
//            DNASequenceData seq=new DNASequenceData(sequencename, sequencename, chromosome, startPos, endPos, null, null, orientation, buffer);
//            if (info[5]!=null) seq.setTemporaryOrganism((Integer)info[5]);
//            if (info[6]!=null) seq.setTemporaryBuild((String)info[6]);
//            target.addSequence(seq);            
//        }
//        return target;
//    }        
    
    private String[] getHeaderAndSequence(String inputstring) {
        Pattern pattern=Pattern.compile("^(.*?\\S)\\s+\\\\(\\w+)\\\\");
        Matcher matcher=pattern.matcher(inputstring);
        if (matcher.find()) { 
            String sequenceName=matcher.group(1);
            String sequence=matcher.group(2);
            return new String[]{sequenceName,sequence};
        } else return null;
    }    
    
    /**
     * Returns an array with 5 objects containing information obtained from the header in a Consensus file.
     * The elements are 
     * 1) Sequence name (String)
     * 2) Chromosome name (String)
     * 3) Sequence Start Position (Integer)
     * 4) Sequence End Position (Integer)
     * 5) Sequence orientation (Integer)
     * 6) Organism Taxonomy ID (Integer) can be null
     * 7) Genome Build (String) can be null
     * If no information is obtained on a subject the corresponding entry in the array will be null
     * @param header
     * @return
     */
    private Object[] getSequenceInfoFromHeader(String header) throws ParseError {
        Object[] result=new Object[]{null,null,null,null,null,null,null}; // to be filled with sequencename,chromosome,start,end,orientation(int) if known
        String[] fields=header.split("\\|");
        result[0]=fields[0].trim(); // name is first entry
        if (fields.length>=2) {
            Pattern pattern=Pattern.compile("(chr)?(\\w+):(\\d+)-(\\d+)");
            Matcher matcher=pattern.matcher(fields[1].trim());
            if (matcher.find()) {
                if (matcher.group(2)!=null && !matcher.group(2).isEmpty()) result[1]=matcher.group(2);
                if (matcher.group(3)!=null && !matcher.group(3).isEmpty()) {
                    try {
                        int start=Integer.parseInt(matcher.group(3));
                        result[2]=new Integer(start);
                    } catch (NumberFormatException e) {}
                }
                if (matcher.group(4)!=null && !matcher.group(4).isEmpty()) {
                    try {
                        int end=Integer.parseInt(matcher.group(4));
                        result[3]=new Integer(end);
                    } catch (NumberFormatException e) {}
                }
            } else throw new ParseError("Expected information about sequence location in the format 'chrX:start-end', but got:"+fields[1]);
        }
        if (fields.length>=3) {
            String strand=fields[2].trim().toLowerCase();
            if (strand.startsWith("direct") || strand.startsWith("+") || strand.startsWith("1")) result[4]=new Integer(Sequence.DIRECT);
            else if (strand.startsWith("reverse") || strand.startsWith("-")) result[4]=new Integer(Sequence.REVERSE);
            // parse strand in fields[2]
        }
        if (fields.length>=4) {
           String[] el=fields[3].split("\\s*:\\s*");
           if (el.length==1) {
               el=new String[]{null,el[0]};
           }
           if (el[0]==null) { // just build?
               if (Organism.isGenomeBuildSupported(el[1])) {
                   int organism=Organism.getOrganismForGenomeBuild(el[1]);
                   result[5]=new Integer(organism);
               }
           } else {
               try {
                 int organism=Integer.parseInt(el[0]);
                 result[5]=new Integer(organism);
               } catch (NumberFormatException e) {
                 int organism=Organism.getTaxonomyID(el[0]);
                 if (organism!=0) result[5]=new Integer(organism);  
               }
           }
           result[6]=el[1];
        }
        return result;
    }   
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
         throw new ParseError("Unable to parse input to DataSegment in Consensus format)");
    }
    
    
    /** adds missing sequences to the dataset */
    private void setupofDNADataset(DNASequenceDataset dataset) {
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data seq:sequences) {
            if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new DNASequenceData((Sequence)seq,'N'));     
        }          
    }   
}

        
       
        
        
