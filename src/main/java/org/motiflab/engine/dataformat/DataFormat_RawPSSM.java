package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.List;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_RawPSSM extends DataFormat {
    private String name="RawPSSM";
    public static final String VERTICAL="Vertical";
    public static final String HORIZONTAL="Horizontal";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};

    
    public DataFormat_RawPSSM() {
        addOptionalParameter("Format","Default", new String[]{"Default","Frequencies"},"Default: output matrices in the same format as used in the table.<br>Frequencies: output matrices as frequencies (convert from default format if necessary)");
        addOptionalParameter("Orientation",VERTICAL, new String[]{VERTICAL,HORIZONTAL},"<html>Vertical: matrix has N rows and 4 columns<br>Horizontal: matrix has 4 rows and N columns</html>");
        addOptionalParameter("Delimiter","Tab", new String[]{"Space","Tab","Comma","Semicolon"},"The character used to separate columns");
        addOptionalParameter("Header","ID", new String[]{"ID","ID-Name","ID Name"},"Which properties of a motif to include in the header");
        setParameterFilter("Header","output");
        setParameterFilter("Format","output");         
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
        return "pssm";
    }
 
    @Override
    public String[] getSuffixAlternatives() {return new String[]{"pssm","txt"};}       
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        String orientation;
        String delimiter;
        String header="ID";
        String format="Default";
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine); 
             delimiter=(String)settings.getResolvedParameter("Delimiter",defaults,engine);
             header=(String)settings.getResolvedParameter("Header",defaults,engine);
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           orientation=(String)getDefaultValueForParameter("Orientation");
           delimiter=(String)getDefaultValueForParameter("Delimiter");
           header=(String)getDefaultValueForParameter("Header");
           format=(String)getDefaultValueForParameter("Format");
        }        
        if (delimiter==null || delimiter.equals("Space")) delimiter=" ";
        else if (delimiter.equals("Tab")) delimiter="\t";
        else if (delimiter.equals("Comma")) delimiter=",";
        else if (delimiter.equals("Semicolon")) delimiter=";";
        StringBuilder outputString=new StringBuilder();
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, orientation, delimiter,outputString, header, format);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%20==0) Thread.yield();                                 
            } 
        } else if (dataobject instanceof Motif){
                outputMotif((Motif)dataobject,orientation,delimiter,outputString, header, format);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }    
    
    
    /** outputformats a single motif */
    private void outputMotif(Motif motif, String orientation, String delimiter, StringBuilder outputString, String header, String format) {
            double[][] matrix=(format.equals("Frequencies"))?motif.getMatrixAsFrequencyMatrix():motif.getMatrix();
            if (matrix==null) return;
            outputString.append(">");
            if (header.equals("ID")) outputString.append(motif.getName());
            else {
                String shortname=motif.getShortName();
                String presentationName=motif.getName();
                if (shortname!=null && !shortname.isEmpty()) {
                    if (header.equalsIgnoreCase("ID-Name")) presentationName+="-"+shortname;
                    else if (header.equalsIgnoreCase("ID Name")) presentationName+=" "+shortname;
                }
                outputString.append(presentationName);
            }
            
            outputString.append("\n");
            if (orientation.equals(VERTICAL)) { // Vertical orientation
                for (double[] row:matrix) {
                    if (row[0]==(int)row[0]) outputString.append((int)row[0]); else outputString.append(row[0]);
                    outputString.append(delimiter);
                    if (row[1]==(int)row[1]) outputString.append((int)row[1]); else outputString.append(row[1]);
                    outputString.append(delimiter);
                    if (row[2]==(int)row[2]) outputString.append((int)row[2]); else outputString.append(row[2]);
                    outputString.append(delimiter);
                    if (row[3]==(int)row[3]) outputString.append((int)row[3]); else outputString.append(row[3]);
                    outputString.append("\n");
                }
            } else { // Horizontal orientation
                int length=motif.getLength();
                for (int base=0;base<4;base++) {
                    for (int i=0;i<length;i++) { // 
                       if (matrix[i][base]==(int)matrix[i][base]) outputString.append((int)matrix[i][base]); else outputString.append(matrix[i][base]);                       
                       outputString.append((i<length-1)?delimiter:"\n");
                       
                    }   
                }         
            }
            outputString.append("\n");
    }

      

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_RawPSSM.parseInput(ArrayList<String> input, Data target)");
        String orientation=VERTICAL;
        String delimiter="\\s+";
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine); 
             delimiter=(String)settings.getResolvedParameter("Delimiter",defaults,engine); 
          } catch (ExecutionError e) {
             throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());
          } catch (Exception ex) {
              throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());
          }
        } else {
           orientation=(String)getDefaultValueForParameter("Orientation");
           delimiter=(String)getDefaultValueForParameter("Delimiter");
        }
        if (delimiter==null || delimiter.equals("Space") || delimiter.equals("Tab")) delimiter="\\s+";
        else if (delimiter.equals("Comma")) delimiter="\\s*,\\s*";
        else if (delimiter.equals("Semicolon")) delimiter="\\s*;\\s*";        
             if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, orientation, delimiter, task);
        else if (target instanceof Motif) return parseMotif(input, (Motif)target, orientation, delimiter);
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }
    

    private Motif parseMotif(List<String> input, Motif target, String orientation, String delimiter) throws ParseError {
       String consensus=null;
       String motifID="unknown";
       String motifShortName=null;
       String motifLongName=null;
       int quality=6;
       String classification=null;
       String factors=null;
       String organisms=null;
       if (target==null) target=new Motif("unknown");
       ArrayList<String>matrixlines=new ArrayList<String>();
       for (String line:input) {
           line=line.trim();
           if (line.isEmpty()) continue;
           else if (line.startsWith(">")) {
               String namestring=line.substring(1);         
               String[] split=namestring.split("-|\\s+",2);
               if (split.length==2) {motifShortName=split[1];}
               motifID=split[0];
           } else if (line.startsWith("#")) { // this is considered a comment
               continue;
           } else matrixlines.add(line);
       }       
       double[][] matrix=null;       
       if (orientation.equals(VERTICAL)) matrix=parseVerticalMatrix(matrixlines, motifID, delimiter);
       else matrix=parseHorizontalMatrix(matrixlines, motifID, delimiter);
       target.setMatrix(matrix);
       if (consensus!=null) target.setConsensusMotif(consensus);
       else target.setConsensusMotif(Motif.getConsensusForMatrix(matrix));
       target.setName(motifID);
       target.setShortName(motifShortName);
       target.setLongName(motifLongName);
       target.setQuality(quality);
       try {target.setClassification(classification);} catch (ExecutionError e) {throw new ParseError(e.getMessage());}
       target.setBindingFactors(factors);
       target.setOrganisms(organisms);
       target.setICcontent(Motif.calculateInformationContent(matrix, false));
       return target;
    } 
                        
    
    private MotifCollection parseMotifCollection(List<String> input, MotifCollection target, String orientation, String delimiter, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new MotifCollection("MotifCollection");
        int first=-1; int last=0; int size=input.size();
        if (size<1) return target; // throw new ParseError("Empty input for MotifCollection");
        String headerline=input.get(0);
        if (!(headerline.startsWith("#") || headerline.startsWith(">"))) throw new ParseError("Unrecognized header for RawPSSM motif format: "+headerline);
        int count=0;
        for (int i=0;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith(">")) {
                count++;
                if (count%30==0) {
                  if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                  if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
                  Thread.yield();
                }                 
                if (first<0) { // this is the first encountered motif
                    first=i;
                } else {
                    last=i;
                    Motif motif=parseMotif(input.subList(first, last), null, orientation, delimiter); // note that the 'last' line is exclusive in the sublist
                    target.addMotifToPayload(motif);
                    first=i;
                    last=0;
                }
            }
        }
        if (first>=0) {
            Motif motif=parseMotif(input.subList(first, size), null, orientation, delimiter);
            target.addMotifToPayload(motif);
        }
        return target;
    }
            
            
    
    /** */
    private double[][] parseVerticalMatrix(List<String> list, String motifID, String delimiter) throws ParseError {
        if (list.isEmpty()) throw new ParseError("Missing matrix specification values for matrix: "+motifID);
        double[][] result=new double[list.size()][4];
        for (int i=0;i<list.size();i++) {
            String line=list.get(i);
            String[] split=line.split(delimiter);
            if (split.length!=4) throw new ParseError("Expected 4 columns in data matrix '"+motifID+"'. Got "+split.length);
            for (int j=0;j<4;j++) {
                try {
                    result[i][j]=Double.parseDouble(split[j]);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value in matrix for '"+motifID+"':" +split[j]);}
            }
        }
        return result;
    }  
    
    /** */
    private double[][] parseHorizontalMatrix(List<String> list, String motifID, String delimiter) throws ParseError {
        if (list.isEmpty()) throw new ParseError("Missing matrix specification values for matrix: "+motifID);
        if (list.size()!=4) throw new ParseError("Expected 4 rows of values for horizontal matrix orientation for: "+motifID);
        String[] splitA=list.get(0).split(delimiter);
        String[] splitC=list.get(1).split(delimiter);
        String[] splitG=list.get(2).split(delimiter);
        String[] splitT=list.get(3).split(delimiter);
        if (splitT.length!=splitA.length || splitG.length!=splitA.length || splitC.length!=splitA.length) throw new ParseError("Unequal number of values in rows for matrix "+motifID+": A="+splitA.length+", C="+splitC.length+", G="+splitG.length+", T="+splitT.length);
        double[][] result=new double[splitA.length][4];
        for (int i=0;i<splitA.length;i++) {
                try { result[i][0]=Double.parseDouble(splitA[i]);} 
                catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value in matrix for '"+motifID+"' = " +splitA[i]);}
                try { result[i][1]=Double.parseDouble(splitC[i]);} 
                catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value in matrix for '"+motifID+"' = " +splitC[i]);}
                try { result[i][2]=Double.parseDouble(splitG[i]);} 
                catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value in matrix for '"+motifID+"' = " +splitG[i]);}
                try { result[i][3]=Double.parseDouble(splitT[i]);} 
                catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value in matrix for '"+motifID+"' = " +splitT[i]);}
        }
        return result;
    }    
    
}
