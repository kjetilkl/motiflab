/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.HashMap;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.ExpressionProfile;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class DataFormat_ExpressionProfile extends DataFormat {
    private String name="ExpressionProfile";
    private Class[] supportedTypes=new Class[]{ExpressionProfile.class};
    private static final String HEADER_NONE="None";
    private static final String HEADER_COLUMN_NAMES="Column names";
    private static final String HEADER_HASH_COLUMN_NAMES="#Column names";
    


    public DataFormat_ExpressionProfile() {
        addOptionalParameter("Sequence name delimiter","Tab", new String[]{"Space","Tab","Comma","Semicolon","Equals","Colon"},"The character that separates the name of the sequence from the expression values");
        addOptionalParameter("Condition delimiter","Tab", new String[]{"Space","Tab","Comma","Semicolon","Colon"},"The character that separates expression values for different conditions");
        addOptionalParameter("Header",HEADER_NONE, new String[]{HEADER_NONE,HEADER_COLUMN_NAMES,HEADER_HASH_COLUMN_NAMES},"Output an optional header in the first line");
        //setParameterFilter("Header","output");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof ExpressionProfile );
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(ExpressionProfile.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof ExpressionProfile);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(ExpressionProfile.class));
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "csv";
    }

    @Override
    public String[] getSuffixAlternatives() {return new String[]{"csv","tsv"};}
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        String sequencenamedelimiter;
        String delimiter;
        String header;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             sequencenamedelimiter=(String)settings.getResolvedParameter("Sequence name delimiter",defaults,engine);
             delimiter=(String)settings.getResolvedParameter("Condition delimiter",defaults,engine);
             header=(String)settings.getResolvedParameter("Header",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           sequencenamedelimiter=(String)getDefaultValueForParameter("Sequence name delimiter");
           delimiter=(String)getDefaultValueForParameter("Condition delimiter");
           header=(String)getDefaultValueForParameter("Header");
        }
        if (sequencenamedelimiter==null || sequencenamedelimiter.equals("Tab")) sequencenamedelimiter="\t";
        else if (sequencenamedelimiter.equals("Space")) sequencenamedelimiter=" ";
        else if (sequencenamedelimiter.equals("Comma")) sequencenamedelimiter=",";
        else if (sequencenamedelimiter.equals("Colon")) sequencenamedelimiter=":";
        else if (sequencenamedelimiter.equals("Semicolon")) sequencenamedelimiter=";";
        else if (sequencenamedelimiter.equals("Equals")) sequencenamedelimiter="=";
        if (delimiter==null || delimiter.equals("Tab")) delimiter="\t";
        else if (delimiter.equals("Space")) delimiter=" ";
        else if (delimiter.equals("Comma")) delimiter=",";
        else if (delimiter.equals("Colon")) delimiter=":";
        else if (delimiter.equals("Semicolon")) delimiter=";";
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

        ExpressionProfile expressionprofile=(ExpressionProfile)dataobject;
        int i=0;
        int size=sequenceCollection.getNumberofSequences();
        ArrayList<Sequence> sequences=sequenceCollection.getAllSequencesInDefaultOrder(engine);
        StringBuilder outputString=new StringBuilder();
        if (header.equals(HEADER_COLUMN_NAMES) || header.equals(HEADER_HASH_COLUMN_NAMES)) {
            if (header.equals(HEADER_HASH_COLUMN_NAMES)) outputString.append("#");
            outputString.append("Sequence");
            int count=expressionprofile.getNumberOfConditions();
            for (int j=0;j<count;j++) {
                outputString.append((j==0)?sequencenamedelimiter:delimiter); 
                outputString.append(expressionprofile.getHeader(j));
            }
            outputString.append("\n");
        }
        for (Sequence sequence:sequences) { // for each sequence
              String sequenceName=sequence.getName();
              outputSequence(sequenceName, expressionprofile, sequencenamedelimiter, delimiter,outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    private void outputSequence(String sequenceName, ExpressionProfile expressionprofile, String sequenceNameDelimiter, String delimiter, StringBuilder outputString) {
        ArrayList<Double> profile=expressionprofile.getExpressionProfileForSequence(sequenceName);
        if (profile==null) {
            int records=expressionprofile.getNumberOfConditions();
            profile=new ArrayList<Double>(records);
            for (int i=0;i<records;i++) profile.add(new Double(0));
        }
        outputString.append(sequenceName);
        outputString.append(sequenceNameDelimiter);
        if (profile.isEmpty()) outputString.append("\n");
        else if (profile.size()==1) {
            outputString.append(profile.get(0));
            outputString.append("\n");
        }
        else {
            for (int i=0;i<profile.size()-1;i++) {
               outputString.append(profile.get(i));
               outputString.append(delimiter);
            }
            outputString.append(profile.get(profile.size()-1));
            outputString.append("\n");
        }
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) {
            target=new ExpressionProfile("temporary");
        }
        String sequencenamedelimiter;
        String delimiter;
        String header;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             sequencenamedelimiter=(String)settings.getResolvedParameter("Sequence name delimiter",defaults,engine);
             delimiter=(String)settings.getResolvedParameter("Condition delimiter",defaults,engine);
             header=(String)settings.getResolvedParameter("Header",defaults,engine);
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
           sequencenamedelimiter=(String)getDefaultValueForParameter("Sequence name delimiter");
           delimiter=(String)getDefaultValueForParameter("Condition delimiter");
           header=(String)getDefaultValueForParameter("Header");
        }
        if (sequencenamedelimiter==null || sequencenamedelimiter.equals("Tab")) sequencenamedelimiter="\t";
        else if (sequencenamedelimiter.equals("Space")) sequencenamedelimiter=" ";
        else if (sequencenamedelimiter.equals("Comma")) sequencenamedelimiter=",";
        else if (sequencenamedelimiter.equals("Colon")) sequencenamedelimiter=":";
        else if (sequencenamedelimiter.equals("Semicolon")) sequencenamedelimiter=";";
        if (delimiter==null || delimiter.equals("Tab")) delimiter="\t";
        else if (delimiter.equals("Space")) delimiter=" ";
        else if (delimiter.equals("Comma")) delimiter=",";
        else if (delimiter.equals("Colon")) delimiter=":";
        else if (delimiter.equals("Semicolon")) delimiter=";";
        HashMap<String, ArrayList<Double>> profiles=new HashMap<String, ArrayList<Double>>();
        int expected=-1;
        String headerline=null;
        if (!header.equals(HEADER_NONE) && !input.isEmpty()) {
            headerline=input.remove(0).trim();
            if (headerline.startsWith("#")) headerline=headerline.substring(1);
            int index=headerline.indexOf(sequencenamedelimiter);
            if (index<0) throw new ParseError("Sequence name delimiter not found in line:\nline");
            String rest=headerline.substring(index+1);
            String[] records=rest.split(delimiter);
            expected=records.length;
            HashMap<Integer,String> headernames=new HashMap<Integer,String>();
            for (int j=0;j<expected;j++) {
               headernames.put(j, records[j]); 
            }
            ((ExpressionProfile)target).setHeaders(headernames);
        }
        for (String line:input) {
            if (line.startsWith("#")) continue; // skip comment lines
            line=line.trim();
            int index=line.indexOf(sequencenamedelimiter);
            if (index<0) throw new ParseError("Sequence name delimiter not found in line:\nline");
            if (index==line.length()-1) throw new ParseError("Missing expression records for sequence");
            String sequenceName=line.substring(0,index).trim();  
            sequenceName=convertIllegalSequenceNamesIfNecessary(sequenceName, false);
            String error=engine.checkSequenceNameValidity(sequenceName, false);
            if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+sequenceName+"' : "+error);                              
                                
            String rest=line.substring(index+1);
            String[] records=rest.split(delimiter);
            if (expected<0) expected=records.length;
            else if (expected!=records.length) throw new ParseError("Sequence '"+sequenceName+"' does not have the expected number of condition entries (expected "+expected+" found "+records.length+")");
            ArrayList<Double> seqprofile=new ArrayList<Double>(expected);
             for (String record:records) {
                 try {
                     double value=Double.parseDouble(record);
                     seqprofile.add(new Double(value));
                 } catch (NumberFormatException e) {
                     throw new ParseError("Unable to parse expected numeric entry for sequence '"+sequenceName+"': "+record);
                 }
             }
             profiles.put(sequenceName,seqprofile);
        }
        ((ExpressionProfile)target).setValue(profiles);
        return target;
    }



}





