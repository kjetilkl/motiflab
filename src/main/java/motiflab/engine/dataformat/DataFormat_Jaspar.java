package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.List;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.ParameterSettings;
import motiflab.engine.Parameter;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_Jaspar extends DataFormat {
    private String name="Jaspar";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_Jaspar() {
        addOptionalParameter("Format","Default", new String[]{"Default","Frequencies"},"Default: output matrices in the same format as used in the table.<br>Frequencies: output matrices as frequencies (convert from default format if necessary)");
        addOptionalParameter("Header","ID", new String[]{"ID","ID Name"},"Format of header");
        setParameterFilter("Format","output");
        setParameterFilter("Header","output");        
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
        String format="Default";
        String header="ID";
        setProgress(5);
        if (settings!=null) {
        try{
             Parameter[] defaults=getParameters();
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
             header=(String)settings.getResolvedParameter("Header",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           format=(String)getDefaultValueForParameter("Format");
           header=(String)getDefaultValueForParameter("Header");
        }
        StringBuilder outputString=new StringBuilder();
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, header, format, outputString);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%20==0) Thread.yield();
            }
        } else if (dataobject instanceof Motif){
                outputMotif((Motif)dataobject, header, format, outputString);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    /** outputformats a single motif */
    private void outputMotif(Motif motif, String header, String format, StringBuilder outputString) {
            double[][] matrix=(format.equals("Frequencies"))?motif.getMatrixAsFrequencyMatrix():motif.getMatrix();
            if (matrix==null) return;
            boolean useCountMatrix=(format.equals("Default") && motif.getMatrixType()==Motif.COUNT_MATRIX);
            double max=Motif.getMaxValueInMatrix(matrix);
            int decimals=1;
            if (max>=1000) decimals=4;
            else if (max>=100) decimals=3;
            else if (max>=10) decimals=2;
            outputString.append(">");
            if (header.equals("ID")) outputString.append(motif.getName());
            else if (header.equals("ID Name")){
                String shortname=motif.getShortName();
                String presentationName=motif.getName();
                if (shortname!=null && !shortname.isEmpty()) {
                    presentationName+=" "+shortname;
                }
                outputString.append(presentationName);
            }

            outputString.append("\n");
            // horisontal orientation
            int length=motif.getLength();
            char[] bases=new char[]{'A','C','G','T'};
            for (int base=0;base<4;base++) {
                outputString.append(bases[base]);
                outputString.append("  [");
                for (int i=0;i<length;i++) { // A
                   if (useCountMatrix) outputString.append(formatFixedWidthInteger((int)matrix[i][base],decimals));
                   else outputString.append(formatFixedWidthDouble(matrix[i][base],decimals+6,5));
                   if (i<length-1) outputString.append(" ");
                   else outputString.append(" ]\n");
                }
            }
            outputString.append("\n");
    }



    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_Jaspar.parseInput(ArrayList<String> input, Data target)");
             if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, task);
        else if (target instanceof Motif) return parseMotif(input, (Motif)target);
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }


    private Motif parseMotif(List<String> input, Motif target) throws ParseError {
       String delimiter="\\s+";        
       String motifID="unknown";
       String motifShortName=null;
       String motifLongName=null;
       int quality=6;
       String classification=null;
       String factors=null;
       String organisms=null;
       if (target==null) target=new Motif("unknown");
       String[] matrixlines=new String[4];
       for (String line:input) {
           line=line.trim();
           if (line.isEmpty()) continue;
           else if (line.startsWith(">")) {
               String namestring=line.substring(1);
               String[] split=namestring.split("\\s+",2);
               if (split.length==2) {motifShortName=split[1];}
               motifID=split[0];
               motifID=motifID.replaceFirst("\\.\\d+", ""); // remove version info
           } else if (line.startsWith("#")) {
               continue;
           } else if (line.startsWith("A")) {
               matrixlines[0]=line.replaceFirst("A\\s+\\[\\s*","");
               matrixlines[0]=matrixlines[0].replaceFirst("\\s*\\]\\s*","");
           } else if (line.startsWith("C")) {
               matrixlines[1]=line.replaceFirst("C\\s+\\[\\s*","");
               matrixlines[1]=matrixlines[1].replaceFirst("\\s*\\]\\s*","");
           } else if (line.startsWith("G")) {
               matrixlines[2]=line.replaceFirst("G\\s+\\[\\s*","");
               matrixlines[2]=matrixlines[2].replaceFirst("\\s*\\]\\s*","");
           } else if (line.startsWith("T")) {
               matrixlines[3]=line.replaceFirst("T\\s+\\[\\s*","");
               matrixlines[3]=matrixlines[3].replaceFirst("\\s*\\]\\s*","");
           }
       }
       double[][] matrix=null;
       matrix=parseHorizontalMatrix(matrixlines, motifID, delimiter);
       target.setMatrix(matrix);
       target.setConsensusMotif(Motif.getConsensusForMatrix(matrix));
       target.setName(motifID);
       if (motifShortName!=null) motifShortName=fixShortName(motifShortName);
       target.setShortName(motifShortName);
       target.setLongName(motifLongName);
       target.setQuality(quality);
       try {target.setClassification(classification);} catch (ExecutionError e) {throw new ParseError(e.getMessage());}
       target.setBindingFactors(factors);
       target.setOrganisms(organisms);
       target.setICcontent(Motif.calculateInformationContent(matrix, false));
       return target;
    }

    private String fixShortName(String shortname) {
        String result=shortname.replaceAll(":+", "-");
        result=result.replaceAll(";+", "-");
        result=result.replaceAll(",+", "-");
        return result;
    }

    private MotifCollection parseMotifCollection(List<String> input, MotifCollection target, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new MotifCollection("MotifCollection");
        int first=-1; int last=0; int size=input.size();
        if (size<1) return target; // throw new ParseError("Empty input for MotifCollection");
        String headerline=input.get(0);
        if (!(headerline.startsWith("#") || headerline.startsWith(">"))) throw new ParseError("Unrecognized header for Jaspar motif format: "+headerline);
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
                if (first<0) {
                    first=i;
                } else {
                    last=i;
                    Motif motif=parseMotif(input.subList(first, last), null); // note that the 'last' line is exclusive in the sublist
                    target.addMotifToPayload(motif);
                    first=i;
                    last=0;
                }
            }
        }
        if (first>=0) {
            Motif motif=parseMotif(input.subList(first, size), null);
            target.addMotifToPayload(motif);
        }
        return target;
    }




    /** */
    private double[][] parseHorizontalMatrix(String[] list, String motifID, String delimiter) throws ParseError {
        if (list[0]==null) throw new ParseError("Missing row for 'A' in matrix for: "+motifID);
        if (list[1]==null) throw new ParseError("Missing row for 'C' in matrix for: "+motifID);
        if (list[2]==null) throw new ParseError("Missing row for 'G' in matrix for: "+motifID);
        if (list[3]==null) throw new ParseError("Missing row for 'T' in matrix for: "+motifID);
        String[] splitA=list[0].split(delimiter);
        String[] splitC=list[1].split(delimiter);
        String[] splitG=list[2].split(delimiter);
        String[] splitT=list[3].split(delimiter);
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





