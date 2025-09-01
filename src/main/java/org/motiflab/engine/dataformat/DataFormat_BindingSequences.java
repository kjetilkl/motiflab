package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.List;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_BindingSequences extends DataFormat {
    private String name="BindingSequences";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_BindingSequences() {
        addOptionalParameter("Binding sequence property", "Binding sequences", null,"<html>This parameter should name the user-defined motif property holding the list of binding sequences for the motif</html>");     
        addOptionalParameter("Header","ID", new String[]{"ID","ID Name"},"Format of header");
        addOptionalParameter("Separate headers",Boolean.FALSE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"If selected, each sequence will be output with a separate header on the form \"ID-N [name]\" (where N is an incremental counter");
        addOptionalParameter("When missing","Output consensus", new String[]{"Generate random","Output consensus"},"If the motif has no information about the binding sites, you can either output a single consensus sequence or generate a set of random binding sequences with approximately the same base frequencies as the motif matrix");        
        addAdvancedParameter("Random sequences precision", new Integer(3), new Integer[]{1,6},"For sequences generated from frequency matrices, the value N of this parameter determines the number of sequences that will be generated (=10^(N-1))");
        setParameterFilter("Header","output");           
        setParameterFilter("When missing","output");       
        setParameterFilter("Random sequences precision","output");           
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
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
        setProgress(5);
        String BSproperty=null;
        String header="ID";
        boolean generateRandom=false;
        boolean separateHeaders=false;
        int precision=3;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             BSproperty=(String)settings.getResolvedParameter("Binding sequence property",defaults,engine);         
             header=(String)settings.getResolvedParameter("Header",defaults,engine);
             separateHeaders=(Boolean)settings.getResolvedParameter("Separate headers",defaults,engine);
             String missing=(String)settings.getResolvedParameter("When missing",defaults,engine);
             generateRandom=missing.equalsIgnoreCase("Generate random");
             precision=(Integer)settings.getResolvedParameter("Random sequences precision",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
             BSproperty=(String)getDefaultValueForParameter("Binding sequence property");           
             header=(String)getDefaultValueForParameter("Header");  
             separateHeaders=(Boolean)getDefaultValueForParameter("Separate headers");  
             String missing=(String)getDefaultValueForParameter("When missing");
             generateRandom=missing.equalsIgnoreCase("Generate random");     
             precision=(Integer)getDefaultValueForParameter("Random sequences precision");
        }
        StringBuilder outputString=new StringBuilder();
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                if (i>0) outputString.append("\n");
                outputMotif(motif, outputString, BSproperty, header, separateHeaders, generateRandom, precision);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            }
        } else if (dataobject instanceof Motif){
            outputMotif((Motif)dataobject, outputString, BSproperty, header, separateHeaders, generateRandom, precision);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }

    

    /** output formats a single motif */
    protected void outputMotif(Motif motif, StringBuilder outputString, String property, String header, boolean separateHeaders, boolean generateRandom, int precision) throws ExecutionError {
        if (property==null || property.isEmpty()) { // property not listed. output consensus motif
           outputString.append(">"+getHeader(motif, header, 0) +"\n");
           outputString.append(motif.getConsensusMotif()+"\n");
           return;
        }
        try {
           Object value=motif.getPropertyValue(property,engine); // this throws ExecutionError if property is not recognized                    
           if (value instanceof ArrayList) {
               if (separateHeaders) {
                   int counter=0;
                   for (String bsite:(ArrayList<String>)value) {
                       counter++;
                       outputString.append(">"+getHeader(motif, header, counter) +"\n");
                       outputString.append(bsite+"\n");                       
                   }
               }
               else {
                   outputString.append(">"+getHeader(motif, header, 0) +"\n");
                   outputString.append(MotifLabEngine.splice((ArrayList<String>)value, "\n")+"\n");
               }
           }
           else if (value!=null) { // binding sites provided as a string separated by | or , (or both)
               String valueAsString=value.toString();
               if (valueAsString.isEmpty()) {
                   outputString.append(">"+getHeader(motif, header, 0) +"\n");
                   outputString.append(motif.getConsensusMotif()+"\n");
               }
               else if (separateHeaders) {
                   valueAsString=valueAsString.replace('|', ',');                   
                   String[] sites=valueAsString.split("\\s*,\\s*");
                   int counter=0;
                   for (String bsite:sites) {
                       counter++;
                       outputString.append(">"+getHeader(motif, header, counter) +"\n");
                       outputString.append(bsite+"\n");                           
                       
                   }
               } 
               else {
                   valueAsString=valueAsString.replace('|', '\n');                   
                   valueAsString=valueAsString.replace(',', '\n');                   
                   outputString.append(">"+getHeader(motif, header, 0) +"\n");
                   outputString.append(valueAsString+"\n");                  
               }
           } else { // no value for property, output Consensus-string instead
             if (generateRandom) outputRandomMotifs(motif, outputString, precision, header, separateHeaders);
             else {
                 outputString.append(">"+getHeader(motif, header, 0) +"\n");
                 outputString.append(motif.getConsensusMotif()+"\n");
             }  
           }      
        } catch (Exception e) { // not a recognized property, output Consensus-string instead
            if (generateRandom) outputRandomMotifs(motif, outputString, precision, header, separateHeaders);
            else {
                outputString.append(">"+getHeader(motif, header,0) +"\n");
                outputString.append(motif.getConsensusMotif()+"\n");
            }
        }
    }
    
    private void outputRandomMotifs(Motif motif, StringBuilder outputString, int precision, String header, boolean separateHeaders) throws ExecutionError {
        int[][] countMatrix=motif.getMatrixAsCountMatrix(precision);
        if (countMatrix==null || countMatrix.length==0) return;
        int sites=countMatrix[0][0]+countMatrix[0][1]+countMatrix[0][2]+countMatrix[0][3];      
        int columns=countMatrix.length;
        // check and verify that the matrix is consistent (that all columns sum to the same value)
        for (int i=0;i<columns;i++) {
            int colsum=countMatrix[i][0]+countMatrix[i][1]+countMatrix[i][2]+countMatrix[i][3]; 
            if (colsum!=sites) throw new ExecutionError("System Error:DataFormat BindingSequences: Inconsistent count matrix. Sum of column #"+(i+1)+" ("+colsum+") does match expected sum ("+sites+")");
        }        
        if (!separateHeaders) outputString.append(">"+getHeader(motif, header, 0) +"\n");
        // now generate the sequences
        for (int j=0;j<sites;j++) { 
            if (separateHeaders) outputString.append(">"+getHeader(motif, header, j+1) +"\n");
            for (int i=0;i<columns;i++) {
                     if (countMatrix[i][0]>0) {outputString.append("A");countMatrix[i][0]--;}
                else if (countMatrix[i][1]>0) {outputString.append("C");countMatrix[i][1]--;}
                else if (countMatrix[i][2]>0) {outputString.append("G");countMatrix[i][2]--;}
                else if (countMatrix[i][3]>0) {outputString.append("T");countMatrix[i][3]--;}
                else throw new ExecutionError("System Error: Ran out of available bases while generating binding sequence "+(j+1)+" of "+sites);
            }
            outputString.append("\n");
        }
    }
    

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_Jaspar.parseInput(ArrayList<String> input, Data target)");
        String BSproperty=null;
        boolean separateHeaders=false;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             BSproperty=(String)settings.getResolvedParameter("Binding sequence property",defaults,engine);   
             separateHeaders=(Boolean)settings.getResolvedParameter("Separate headers",defaults,engine);
          } catch (Exception ex) {
              throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());
          }
        } else {
             BSproperty=(String)getDefaultValueForParameter("Binding sequence property");  
             separateHeaders=(Boolean)getDefaultValueForParameter("Separate headers");               
        }
        if (BSproperty!=null && BSproperty.trim().isEmpty()) BSproperty=null;
        if (BSproperty!=null && Motif.isStandardMotifProperty(BSproperty)) throw new ParseError("Property '"+BSproperty+"' is reserved and can not be used for binding sequences");
        if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, BSproperty, separateHeaders, task);
        else if (target instanceof Motif) return parseMotif(input, (Motif)target, BSproperty, separateHeaders);
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }


    private Motif parseMotif(List<String> input, Motif target, String property, boolean separateHeaders) throws ParseError {   
       String motifID="unknown";
       String motifShortName=null;
       boolean headerprocessed=false;
       ArrayList<String> sequencesList=new ArrayList<String>();
       if (target==null) target=new Motif("unknown");
       int linenumber=0;
       for (String line:input) {
           linenumber++;
           line=line.trim();
           if (line.isEmpty()) continue;
           else if (line.startsWith("#")) continue;
           else if (line.startsWith(">")) {
               if (headerprocessed) continue; // just skip this extra header
               String namestring=line.substring(1);
               String[] split=(separateHeaders)?namestring.split("\\s+",2):namestring.split("\\W+",2);
               if (split.length==2) {motifShortName=split[1];}
               motifID=split[0];
               if (separateHeaders && motifID.contains("-")) motifID=motifID.substring(0, motifID.indexOf('-'));
               headerprocessed=true;
           } else if (line.matches("[ACGTacgtRrYyMmKkWwSsBbDdHhVvNn]+")) {
               sequencesList.add(line);
           } else throw new ParseError("Unrecognized line: "+line,linenumber);
       }
       if (sequencesList.isEmpty()) throw new ParseError("No binding sequences found for motif: "+motifID);
       String[] sequences=new String[sequencesList.size()];
       sequences=sequencesList.toArray(sequences);
       try {
          double[][] matrix=Motif.getPWMfromSites(sequences);
          target.setMatrix(matrix);
          target.setConsensusMotif(Motif.getConsensusForMatrix(matrix));
          target.setICcontent(Motif.calculateInformationContent(matrix, false));
       } catch (SystemError e) {
          throw new ParseError(e.getMessage()+". (Motif: "+motifID+")"); 
       }
              
       target.setName(motifID);
       if (motifShortName!=null) motifShortName=fixShortName(motifShortName);
       target.setShortName(motifShortName);       
       if (property!=null) {
           try {
               String BSString=MotifLabEngine.splice(sequencesList, "|"); // since "list" properties are Sets and not really lists, use "|" to separate sequences instead of "," to allow duplicate identical sequences
               target.setPropertyValue(property, BSString);
           } catch (Exception e) {throw new ParseError(e.getMessage());}
       }
       return target;
    }

    private String fixShortName(String shortname) {
        String result=shortname.replaceAll(":+", "-");
        result=result.replaceAll(";+", "-");
        result=result.replaceAll(",+", "-");
        return result;
    }

    private MotifCollection parseMotifCollection(List<String> input, MotifCollection target, String property, boolean separateHeaders, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new MotifCollection("MotifCollection");
        int first=-1; int last=0; int size=input.size();
        if (size<1) return target; // throw new ParseError("Empty input for MotifCollection");
        String headerline=input.get(0);
        if (!(headerline.startsWith("#") || headerline.startsWith(">"))) throw new ParseError("Unrecognized header for BindingSequences motif format: "+headerline);
        int count=0;
        String lastMotifID="";
        for (int i=0;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith(">")) {  
                count++;                    
                if (count%30==0) {
                   if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                   if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                   Thread.yield();
                }
                boolean cutHere=false;
                if (separateHeaders && line.indexOf('-')>0) {
                   String newMotifID=line.substring(1, line.indexOf('-'));
                   cutHere=(!newMotifID.equals(lastMotifID)); // make new separation if the motifID on this line is different from the previous
                   lastMotifID=newMotifID;
                } else cutHere=true;            
                if (cutHere) {
                    if (first<0) {
                        first=i;
                    } else {
                        last=i;
                        Motif motif=parseMotif(input.subList(first, last), null, property, separateHeaders); // note that the 'last' line is exclusive in the sublist
                        target.addMotifToPayload(motif);
                        first=i;
                        last=0;
                    }
                }
            }
        }
        if (first>=0) {
            Motif motif=parseMotif(input.subList(first, size), null, property, separateHeaders);
            target.addMotifToPayload(motif);
        }
        return target;
    }

    private String getHeader(Motif motif, String header, int counter) {
        if (header.equals("ID Name")){
            String shortname=motif.getShortName();
            String presentationName=motif.getName();
            if (counter>0) presentationName+=("-"+counter);
            if (shortname!=null && !shortname.isEmpty()) {
                presentationName+=" "+shortname;
            }
            return presentationName;
        } else {
            if (counter>0) return motif.getName()+"-"+counter;
            else return motif.getName();            
        }   
    }

}





