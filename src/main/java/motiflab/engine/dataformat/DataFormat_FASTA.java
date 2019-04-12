/*
 
 
 */

package motiflab.engine.dataformat;


import java.util.ArrayList;
import java.util.List;
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
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.util.SequenceSorter;
import motiflab.gui.SortSequencesDialog;


/**
 *
 * @author kjetikl
 */
public class DataFormat_FASTA extends DataFormat implements DNASequenceParser {
    private String name="FASTA";
    private Class[] supportedTypes=new Class[]{DNASequenceData.class, DNASequenceDataset.class};

    private static final String PARAMETER_HEADER="Header";
    private static final String PARAMETER_STRAND_ORIENTATION="Strand orientation";
    private static final String PARAMETER_COLUMN_WIDTH="Column width";
    private static final String HEADER_SEQUENCENAME_ONLY="Sequence name";
    private static final String HEADER_NAME_LOCATION_STRAND="Name|Location|Strand";
    private static final String HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD="Name|Location|Strand|organism:build";
    private static final String HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD_GENELOCATION="Name|Location|Strand|organism:build|genename:TSS-TES";
    private static final String NO_HEADER="No header";
    private static final String EXTRA_NEWLINE="Extra space"; // do not change the name of this parameter without also changing the name in XML-files for MDscan and BioProspector. Those programs have problems with extra empty lines
    private static final String SORT_SEQUENCES_ASCENDING="Sort ascending"; // can be used to sort sequences in output according to values in a numeric map
    private static final String SORT_SEQUENCES_DESCENDING="Sort descending"; // can be used to sort sequences in output according to values in a numeric map
    private static final String CONVERT_URACIL="Convert uracil"; // can be used to sort sequences in output according to values in a numeric map
    public static final String ADD_SEQUENCES_FROM_FILE="addSequencesFromFile"; // this flag is used to indicate that the sequence objects themselves should be based on the data in the fasta file
    public static final String START_INDEX="startIndex"; // this can be used to define a range of sequences to read a subset from file (starting at index 0)
    public static final String END_INDEX="endIndex"; // this can be used to define a range of sequences to read a subset from file (starting at index 0)
    
    
    public DataFormat_FASTA() {
        addParameter(PARAMETER_STRAND_ORIENTATION, "Relative", new String[]{"Relative","Direct","Reverse"},null);
        addOptionalParameter(PARAMETER_HEADER, HEADER_SEQUENCENAME_ONLY, new String[]{HEADER_SEQUENCENAME_ONLY,HEADER_NAME_LOCATION_STRAND,HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD,HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD_GENELOCATION,NO_HEADER},null);
        addOptionalParameter(PARAMETER_COLUMN_WIDTH, new Integer(60), new Integer[]{0,Integer.MAX_VALUE},"<html>The number of bases to display on each line<br>Use the special value 0 to print the whole sequence on one line</html>");
        addOptionalParameter(EXTRA_NEWLINE, Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Add an extra empty line after each sequence to separate them visually");
        addOptionalParameter(CONVERT_URACIL, Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Convert uracils (U) encountered in the file to thymins (T)");
        addParameter(SORT_SEQUENCES_ASCENDING, null, new Class[]{SequenceNumericMap.class},"Can be used to sort sequences in output according to values in a numeric map",false,true);
        addParameter(SORT_SEQUENCES_DESCENDING, null, new Class[]{SequenceNumericMap.class},"Can be used to sort sequences in output according to values in a numeric map",false,true);

        addParameter(START_INDEX, new Integer(-1), new Integer[]{Integer.MIN_VALUE,Integer.MAX_VALUE},"Specifies the index of the first sequence to return when reading sequences from a file (first sequence has index 0, negative values signal that all sequences should be returned)",false,true);
        addParameter(END_INDEX, new Integer(-1), new Integer[]{Integer.MIN_VALUE,Integer.MAX_VALUE},"Specifies the index of the last sequence to return when reading sequences from a file (first sequence has index 0, negative values signal that all sequences should be returned)",false,true);
      
        setParameterFilter(PARAMETER_COLUMN_WIDTH,"output");
        setParameterFilter(PARAMETER_HEADER,"output");        
        setParameterFilter(EXTRA_NEWLINE,"output");  
        setParameterFilter(CONVERT_URACIL,"input");         
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
        return (data instanceof DNASequenceData || data instanceof DNASequenceDataset || data instanceof SequenceCollection);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(DNASequenceData.class) || dataclass.equals(DNASequenceDataset.class) || dataclass.equals(SequenceCollection.class));
    }

    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "fas";
    }
    
    @Override
    public String[] getSuffixAlternatives() {
        return new String[]{"fas","fasta","fa"};
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        Integer columns;
        String orientation;
        String headerformat;
        boolean extraNewlines=false;
        SequenceNumericMap sortAscending=null;
        SequenceNumericMap sortDescending=null; 
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              columns=(Integer)settings.getResolvedParameter(PARAMETER_COLUMN_WIDTH,defaults,engine); 
              orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);          
              headerformat=(String)settings.getResolvedParameter(PARAMETER_HEADER,defaults,engine);   
              extraNewlines=(Boolean)settings.getResolvedParameter(EXTRA_NEWLINE,defaults,engine);  
              sortAscending=(SequenceNumericMap)settings.getResolvedParameter(SORT_SEQUENCES_ASCENDING,defaults,engine);  
              sortDescending=(SequenceNumericMap)settings.getResolvedParameter(SORT_SEQUENCES_DESCENDING,defaults,engine);                
           } catch (ExecutionError e) {
              throw e;
           } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
           }
        } else {
            columns=(Integer)getDefaultValueForParameter(PARAMETER_COLUMN_WIDTH);
            orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
            headerformat=(String)getDefaultValueForParameter(PARAMETER_HEADER);
            extraNewlines=(Boolean)getDefaultValueForParameter(EXTRA_NEWLINE);
            sortAscending=(SequenceNumericMap)getDefaultValueForParameter(SORT_SEQUENCES_ASCENDING);            
            sortDescending=(SequenceNumericMap)getDefaultValueForParameter(SORT_SEQUENCES_DESCENDING);            
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
            outputString=outputMultipleSequences((DNASequenceDataset)dataobject, sequenceCollection, columns, orientation, headerformat, extraNewlines, task, engine, (sortAscending!=null)?sortAscending:sortDescending,(sortAscending!=null));
        } else if (dataobject instanceof DNASequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((DNASequenceData)dataobject, columns, orientation, headerformat,false,engine,builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.trim()+"\n",getName());
        setProgress(100);
        return outputobject;
    }    
    
    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(DNASequenceDataset sourcedata, SequenceCollection collection, Integer columns, String orientation, String headerformat, boolean extraNewline, ExecutableTask task, MotifLabEngine engine, SequenceNumericMap sortMap, boolean ascending) throws InterruptedException, ExecutionError {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=null;
        if (sortMap!=null) {
           String mapname=sortMap.getName();
           collection=collection.clone(); // just to be sure
           SequenceSorter.checkParameters(SortSequencesDialog.SORT_BY_NUMERIC_MAP, mapname, null, engine);
           SequenceSorter.sortBy(collection, SortSequencesDialog.SORT_BY_NUMERIC_MAP, ascending, mapname, null, engine.getClient());
           sequences=collection.getAllSequences(engine);
        }
        if (sequences==null) sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              DNASequenceData sourceSequence=(DNASequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence,columns, orientation,headerformat,extraNewline, engine,outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }
    
    
    
    
    /** outputformats a single sequence */
    private void outputSequence(DNASequenceData data, Integer columns, String orientation, String headerformat, boolean extraNewline, MotifLabEngine engine, StringBuilder outputString) {
        String sequence;
        String shownOrientation;
        if (columns==null) columns=(Integer)getDefaultValueForParameter(PARAMETER_COLUMN_WIDTH);
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
        boolean includeHeader=true;
        
        String header=">"+data.getName()+"\n";
             if (headerformat.equals(HEADER_SEQUENCENAME_ONLY)) header=">"+data.getName()+"\n";
        else if (headerformat.equals(HEADER_NAME_LOCATION_STRAND)) header=">"+data.getName()+"|"+data.getRegionAsString()+"|"+shownOrientation+"\n";
        else if (headerformat.equals(HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD)) {
            Sequence s=(Sequence)engine.getDataItem(data.getSequenceName());                
            header=">"+data.getName()+"|"+data.getRegionAsString()+"|"+shownOrientation+"|"+s.getOrganism()+":"+s.getGenomeBuild()+"\n";
        } else if (headerformat.equals(HEADER_NAME_LOCATION_STRAND_ORGANISMBUILD_GENELOCATION)) {
            Sequence s=(Sequence)engine.getDataItem(data.getSequenceName());                
            header=">"+data.getName()+"|"+data.getRegionAsString()+"|"+shownOrientation+"|"+s.getOrganism()+":"+s.getGenomeBuild()+"|"+s.getGeneName()+":"+s.getTSS()+"-"+s.getTES()+"\n";
        } else if (headerformat.equals(NO_HEADER)) {
            includeHeader=false;
        }

        if (columns==null || columns.intValue()==0) {
            if (includeHeader) outputString.append(header);
            outputString.append(sequence);
            outputString.append("\n");
        } else {
            if (includeHeader) outputString.append(header);
            int col=columns.intValue();
            int pos=0;
            while (pos+col<=sequence.length()) {
                outputString.append(sequence.substring(pos, pos+col));
                outputString.append("\n");
                pos+=col;
            }
            if (pos<sequence.length()) {
              outputString.append(sequence.substring(pos, sequence.length()));
              outputString.append("\n");
            }
        }
        if (extraNewline) outputString.append("\n");
    }

        
    
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target instanceof SequenceCollection) return parseInputAsSequenceCollection(input, (SequenceCollection)target, settings, task);
        if (target==null) {
            target=new DNASequenceDataset("temporary");
        }
        setupofDNADataset((DNASequenceDataset)target);
        if (input.size()<1) return target; // throw new ParseError("Empty input");
        String headerline=input.get(0);
        if (!(headerline.startsWith(">"))) throw new ParseError("Unrecognized header for FASTA format: "+headerline);       
        String orientation;
        boolean convertUracil=false;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              orientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);
              convertUracil=(Boolean)settings.getResolvedParameter(CONVERT_URACIL,defaults,engine);  
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
            orientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);
            convertUracil=(Boolean)getDefaultValueForParameter(CONVERT_URACIL);
        }
        if (target instanceof DNASequenceData) target=parseSingleSequenceInput(input, (DNASequenceData)target, orientation, convertUracil);
        else if (target instanceof DNASequenceDataset) target=parseMultipleSequenceInput(input, (DNASequenceDataset)target, orientation, convertUracil, task);
        else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-DNASequenceData passed as parameter to parseInput in DataFormat_FASTA");
        return target;
    }
    
    
    private DNASequenceData parseSingleSequenceInput(ArrayList<String> input, DNASequenceData target, String useorientation, boolean convertUracil) throws ParseError, InterruptedException {
        StringBuilder buffer=new StringBuilder();
        int first=-1; // index of the header line of the target sequence
        int last=input.size(); // index of the header line of the first sequence after the target (or the last line if the target sequence is the last in the file)    
        int i=-1;
        for (String line:input) {
            i++;            
            if (line.startsWith(">")) {
                String targetName=getSequenceNameFromHeader(line);               
                if (targetName==null) throw new ParseError("Unable to extract sequence name from header: "+line);                
                String error=engine.checkSequenceNameValidity(targetName, false);
                if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+targetName+"' : "+error);
                if (targetName.equals(target.getSequenceName())) first=i; // this is the index of the header line
                else {
                    if (first<0) continue; // sequence not found yet
                    else {last=i;break;} // found sequence after target sequence
                }
            }
        }
        if (first<0) return target; // sequence not found in file
        List<String> sub=input.subList(first+1, last); // add +1 since 'first' referse to the header line
        for (String string:sub) buffer.append(string.trim());
        //System.err.println(target.getSequenceName()+": length="+target.getSize()+"    complete.length()="+complete.length());
        if (buffer.length()!=target.getSize()) throw new ParseError("Length of FASTA sequence for "+target.getSequenceName()+" ("+buffer.length()+" bp) does not match expected length ("+target.getSize()+" bp)");
        if (useorientation.equals("Reverse") || ((useorientation.equals("Relative")||useorientation.equals("From Sequence")||useorientation.equals("From Gene")) && target.getStrandOrientation()==Sequence.REVERSE)) buffer=MotifLabEngine.reverseSequence(buffer);
        for (i=0;i<buffer.length();i++) {
            if (convertUracil) {
               if (buffer.charAt(i)=='U') target.setValueAtRelativePosition(i, 'T'); 
               else if (buffer.charAt(i)=='u') target.setValueAtRelativePosition(i, 't');   
               else target.setValueAtRelativePosition(i, buffer.charAt(i));
            } 
            else target.setValueAtRelativePosition(i, buffer.charAt(i));
        }
        return target;
    }
    

    private DNASequenceDataset parseMultipleSequenceInput(ArrayList<String> input, DNASequenceDataset target, String useorientation, boolean convertUracil, ExecutableTask task) throws ParseError, InterruptedException {
        // first determine on which line each new sequence starts       
        int first=-1; int last=0; int size=input.size();
        ArrayList<int[]> startandstop=new ArrayList<int[]>();
        for (int i=0;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith(">")) {
                if (first<0) {
                    first=i;
                } else {
                    last=i;
                    startandstop.add(new int[]{first,last});
                    first=i;
                    last=0;
                }
            }
        }
        if (first>=0) {
            startandstop.add(new int[]{first,size});
        }
        if (first<0) return target; // sequence not found in file
        int lines=0;
        int datasetsize=target.getNumberofSequences();
        for (int[] pair:startandstop) {
            lines++;
            if (datasetsize>0 && task!=null) {
                int count=(lines<datasetsize)?lines:(datasetsize-1);
                task.setProgress(count, datasetsize);
                task.setStatusMessage("Loading data for sequences ("+count+"/"+datasetsize+")");
            }
            if (lines%30==0) { // yield every 30 sequences (arbitrary choice)
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
            }   
            int start=pair[0]; int end=pair[1];
            if (end<=start+1) continue; // no sequence?
            String sequencename=getSequenceNameFromHeader(input.get(start));
            if (sequencename==null) throw new ParseError("Unable to extract sequence name from header: "+input.get(start));
            String error=engine.checkSequenceNameValidity(sequencename, false);
            if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+sequencename+"' : "+error);                    
            DNASequenceData seq=(DNASequenceData)target.getSequenceByName(sequencename);
            if (seq==null) continue; // unknown sequence
            StringBuilder buffer=new StringBuilder();
            for (int j=start+1;j<end;j++) buffer.append(input.get(j).trim());
            if (buffer.length()!=seq.getSize()) throw new ParseError("Length of FASTA sequence for "+seq.getSequenceName()+" ("+buffer.length()+" bp) does not match expected length ("+seq.getSize()+" bp)");
            if (useorientation.equals("Reverse") || ((useorientation.equals("Relative")||useorientation.equals("From Sequence")||useorientation.equals("From Gene")) && seq.getStrandOrientation()==Sequence.REVERSE)) buffer=MotifLabEngine.reverseSequence(buffer);
            for (int  i=0;i<buffer.length();i++) {
                if (convertUracil) {
                   if (buffer.charAt(i)=='U') seq.setValueAtRelativePosition(i, 'T'); 
                   else if (buffer.charAt(i)=='u') seq.setValueAtRelativePosition(i, 't');   
                   else seq.setValueAtRelativePosition(i, buffer.charAt(i));
                } 
                else seq.setValueAtRelativePosition(i, buffer.charAt(i));
            }            
        }
        return target;
    }    

    private SequenceCollection parseInputAsSequenceCollection(ArrayList<String> input, SequenceCollection target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
         DNASequenceDataset dnaset=parseDNASequenceDataset(input, "temp", settings);
         return engine.extractSequencesFromFeatureDataset(dnaset, target);
    }
    /** 
     * This will parse and return a DNASequenceDataset without any existing Sequence objects to match the read data against            
     */
    public DNASequenceDataset parseDNASequenceDataset(ArrayList<String> input, String datasetname, ParameterSettings settings) throws ParseError, InterruptedException {
        if (input.size()<1) throw new ParseError("Empty input document");
        String headerline=input.get(0);
        if (!(headerline.startsWith(">"))) throw new ParseError("Unrecognized header for FASTA format: "+headerline);       
        String useorientation;
        boolean convertUracil=false;
        int startIndex=-1;
        int endIndex=-1;
        if (settings!=null) {
           try {
              Parameter[] defaults=getParameters();
              useorientation=(String)settings.getResolvedParameter(PARAMETER_STRAND_ORIENTATION,defaults,engine);          
              convertUracil=(Boolean)settings.getResolvedParameter(CONVERT_URACIL,defaults,engine); 
              startIndex=(Integer)settings.getResolvedParameter(START_INDEX,defaults,engine); 
              endIndex=(Integer)settings.getResolvedParameter(END_INDEX,defaults,engine); 
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
            useorientation=(String)getDefaultValueForParameter(PARAMETER_STRAND_ORIENTATION);            
            convertUracil=(Boolean)getDefaultValueForParameter(CONVERT_URACIL);
        }
        DNASequenceDataset target=new DNASequenceDataset(datasetname);
        int first=-1; int last=0; int size=input.size();
        ArrayList<int[]> startandstop=new ArrayList<int[]>();
        for (int i=0;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith(">")) {
                if (first<0) {
                    first=i;
                } else {
                    last=i;
                    startandstop.add(new int[]{first,last});
                    first=i;
                    last=0;
                }
            }
        }
        if (first>=0) {
            startandstop.add(new int[]{first,size});
        }
        int counter=0;
        if (first<0) return target; // sequence not found in file
        if (startIndex<0) startIndex=0;        
        if (endIndex<0 || endIndex<startIndex || endIndex>=startandstop.size()) endIndex=startandstop.size()-1;       
        for (int index=startIndex;index<=endIndex;index++) {
            int[] pair=startandstop.get(index);
            String genename=null;
            int start=pair[0]; int end=pair[1];
            if (end<=start+1) continue; // no sequence?
            counter++;
            Object[] info=getSequenceInfoFromHeader(input.get(start));
            String sequencename=(String)info[0];
            if (sequencename.indexOf(' ')>0) {
                sequencename=sequencename.substring(0,sequencename.indexOf(' '));
            }
            //if (sequencename==null) throw new ParseError("Unable to extract sequence name from header: "+input.get(start));
            if (sequencename==null) {
                sequencename="Sequence"+counter;
                genename=(info[7]!=null)?(String)info[7]:sequencename;
            }
            else {
                genename=(info[7]!=null)?(String)info[7]:sequencename;
                sequencename=convertIllegalSequenceNamesIfNecessary(sequencename, false);
                String error=engine.checkSequenceNameValidity(sequencename, false);
                if (error!=null) throw new ParseError("Invalid name for sequence '"+sequencename+"' : "+error);
            }
            if (target.getSequenceByName(sequencename)!=null) throw new ParseError("The file contains multiple sequences named '"+sequencename+"'");
            int expectedlength=0;
            if (info[2]!=null && info[3]!=null) {
                int seqend=(Integer)info[3];
                int seqstart=(Integer)info[2];
                expectedlength=seqend-seqstart+1;
            }
            int sequenceOrientation=Sequence.DIRECT;
            if  (info[4]!=null) sequenceOrientation=(Integer)info[4];
            StringBuilder complete=new StringBuilder();
            for (int j=start+1;j<end;j++) complete.append(input.get(j).trim());
            if (expectedlength>0 && expectedlength!=complete.length()) throw new ParseError("Length of FASTA sequence for "+sequencename+" ("+complete.length()+" bp) does not match expected length ("+expectedlength+" bp)");
            if (useorientation.equals("Reverse") || ((useorientation.equals("Relative")||useorientation.equals("From Sequence")||useorientation.equals("From Gene")) && sequenceOrientation==Sequence.REVERSE)) complete=MotifLabEngine.reverseSequence(complete);
            char[] buffer=new char[complete.length()];
            for (int i=0;i<complete.length();i++) {
                if (convertUracil) {
                    if (complete.charAt(i)=='U') buffer[i]='T';
                    else if (complete.charAt(i)=='u') buffer[i]='t';
                    else buffer[i]=complete.charAt(i);
                }
                else buffer[i]=complete.charAt(i);
            } 
            int startPos=1;
            int endPos=complete.length();
            String chromosome="?";
            int orientation=Sequence.DIRECT;
            
            if (info[1]!=null) chromosome=(String)info[1]; 
            if (info[2]!=null) startPos=(Integer)info[2]; 
            if (info[2]!=null && info[3]!=null) endPos=(Integer)info[3]; 
            if (info[4]!=null) orientation=(Integer)info[4]; 
            DNASequenceData seq=new DNASequenceData(sequencename, chromosome, startPos, endPos, buffer);
            // the "temporary" properties below will be passed over to the corresponding Sequence object (when that is created later on)
            if (info[5]!=null) seq.setTemporaryOrganism((Integer)info[5]);
            if (info[6]!=null) seq.setTemporaryBuild((String)info[6]);           
            if (info[8]!=null) seq.setTemporaryTSS((Integer)info[8]);
            if (info[9]!=null) seq.setTemporaryTES((Integer)info[9]);  
            seq.setTemporaryOrientation(orientation);
            seq.setTemporaryGeneName(genename);            
            target.addSequence(seq);            
        }
        return target;
    }        
    
    /**
     * Returns the sequence name from the header (first 'word' after the > sign)
     * If the sequence name contains illegal characters it will be auto-corrected
     * if this setting is turned on in the engine.
     * @param header
     * @return 
     */
    private String getSequenceNameFromHeader(String header) {
        Pattern pattern=Pattern.compile(">\\s*([a-zA-Z_0-9.+-]+)");
        Matcher matcher=pattern.matcher(header);
        if (matcher.find()) { 
            String sequenceName=matcher.group(1);
            if (sequenceName.indexOf('|')>=0) sequenceName=sequenceName.substring(0,sequenceName.indexOf('|')).trim();
            if (sequenceName.isEmpty()) return null;
            sequenceName=convertIllegalSequenceNamesIfNecessary(sequenceName, false);     
            return sequenceName;
        } else return null;
    }    
    
    /**
     * Returns an array with 10 objects containing information obtained from the header in a fasta file.
     * The elements are 
     * [0] Sequence name (String)
     * [1] Chromosome name (String)
     * [2] Sequence Start Position (Integer)
     * [3] Sequence End Position (Integer)
     * [4] Sequence orientation (Integer)
     * [5] Organism Taxonomy ID (Integer) can be null
     * [6] Genome Build (String) can be null
     * [7] Genome name (String) can be null 
     * [8] TSS(Integer) can be null 
     * [9] TES (Integer) can be null 
     * If no information is obtained on a subject the corresponding entry in the array will be null
     * @param header This should start with the > sign
     * @return
     */
    private Object[] getSequenceInfoFromHeader(String header) throws ParseError {
        Object[] result=new Object[]{null,null,null,null,null,null,null,null,null,null}; // to be filled with sequencename,chromosome,start,end,orientation(int) if known
        if (!header.startsWith(">")) return result;
        String[] fields=header.substring(1).split("\\|");
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
        if (fields.length>=5) {
            Pattern pattern=Pattern.compile("(.+?):(\\S+)-(\\S+)");
            Matcher matcher=pattern.matcher(fields[4].trim());
            if (matcher.find()) {
                if (matcher.group(1)!=null && !matcher.group(1).isEmpty()) result[7]=matcher.group(1);
                if (matcher.group(2)!=null && !matcher.group(2).isEmpty()) {
                    try {
                        int start=Integer.parseInt(matcher.group(2));
                        result[8]=new Integer(start);
                    } catch (NumberFormatException e) {}
                }
                if (matcher.group(3)!=null && !matcher.group(3).isEmpty()) {
                    try {
                        int end=Integer.parseInt(matcher.group(3));
                        result[9]=new Integer(end);
                    } catch (NumberFormatException e) {}
                }
            } else throw new ParseError("Expected information about gene location in the format 'genename:start-end', but got:"+fields[4]);       
        }        
        return result;
    }   
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        StringBuilder complete=new StringBuilder();
        int lines=0;
        for (String line:input) {
            lines++;
            if (line.startsWith("#ERROR:")) {
                throw new ParseError(line.substring("#ERROR:".length()).trim());
            }
            if (line.startsWith(">")) continue;
            else complete.append(line);
            if (lines%1000==0) { // yield every 1000 lines (arbitrary choice)
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
            }
        }
        if (complete.length()!=target.getSize()) throw new ParseError("Length of input sequence ("+complete.length()+" bp) does not match expected length from buffer ("+target.getSize()+" bp)");
        String completeString=complete.toString().toUpperCase();
        char[] data=completeString.toCharArray();
        target.setSegmentData(data);
        return target;
    }
    
    
    /** adds missing sequences to the dataset */
    private void setupofDNADataset(DNASequenceDataset dataset) {
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data seq:sequences) {
            if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new DNASequenceData((Sequence)seq,'N'));     
        }          
    }   
}

        
       
        
        
