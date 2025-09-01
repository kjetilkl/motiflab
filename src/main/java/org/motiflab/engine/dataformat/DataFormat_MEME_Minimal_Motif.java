package org.motiflab.engine.dataformat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_MEME_Minimal_Motif extends DataFormat {
    private String name="MEME_Minimal_Motif";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_MEME_Minimal_Motif() {

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
        return "meme-io";
    }
    
    @Override
    public String[] getSuffixAlternatives() {
        return new String[]{"meme-io","meme"};
    }    

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);

        StringBuilder outputString=new StringBuilder();
        outputString.append("MEME version 4\n\n");
        outputString.append("ALPHABET= ACGT\n\n");
        outputString.append("strands: + -\n\n");
        outputString.append("Background letter frequencies (from uniform background):\n");
        outputString.append("A 0.25 C 0.25 G 0.25 T 0.25\n\n");

        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, outputString);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%20==0) Thread.yield();
            }
        } else if (dataobject instanceof Motif){
                outputMotif((Motif)dataobject,outputString);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    /** outputformats a single motif */
    private void outputMotif(Motif motif, StringBuilder outputString) {
            double[][] matrix=motif.getMatrixAsFrequencyMatrix();
            if (matrix==null) return;
            outputString.append("MOTIF ");
            outputString.append(motif.getName());
            String shortName=motif.getShortName();
            if (shortName!=null && !shortName.equals(motif.getName())) {
               outputString.append(" ");
               outputString.append(shortName);
            }
            outputString.append("\n");
            outputString.append("letter-probability matrix: alength= 4");
            outputString.append(" w= ");
            outputString.append(motif.getLength());
            outputString.append(" nsites= ");
            int nsites=motif.getNumberOfSequences();
            if (nsites<=0) nsites=100; // this value must be positive (>0)
            outputString.append(nsites);
            outputString.append(" E= 0\n");
            DecimalFormat formatter=DataFormat.getDecimalFormatter(6);
            for (double[] row:matrix) {
                outputString.append(" ");
                outputString.append(formatter.format(row[0]));
                outputString.append("  ");
                outputString.append(formatter.format(row[1]));
                outputString.append("  ");
                outputString.append(formatter.format(row[2]));
                outputString.append("  ");
                outputString.append(formatter.format(row[3]));
                outputString.append("\n");
            }
            outputString.append("\n");
    }



    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_MEME_Minimal_Motif.parseInput(ArrayList<String> input, Data target)");
             if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, task);
        else if (target instanceof Motif) return parseMotif(input, (Motif)target);
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }


    private Motif parseMotif(List<String> input, Motif target) throws ParseError {
       String motifID="unknown";
       String motifName=null;     
       int expectedLines=0;
       if (target==null) target=new Motif("unknown");
       ArrayList<String[]>matrixlines=new ArrayList<String[]>();
       for (String line:input) {
           line=line.trim();
           if (line.isEmpty()) continue;
           else if (line.startsWith("MOTIF ")) {
               String[] split=line.split("\\s+");
               if (split.length>=2) {motifID=split[1];}
               if (split.length>=3) {motifName=split[2];}
               //else {motifName=motifID;}               
           } else if (line.startsWith("URL")) {
               continue;
           } else if (line.startsWith("log-odds matrix") || line.startsWith("letter-probability matrix")) {
               int wpos=line.indexOf(" w=");
               if (wpos>0) {
                   String wline=line.substring(wpos+3).trim();
                   int nextspace=wline.indexOf(' ');
                   if (nextspace>0) {
                       wline=wline.substring(0, nextspace);
                       try {
                         expectedLines=Integer.parseInt(wline);
                       } catch (NumberFormatException e) {}
                   }
               }
           } else {
               String[] split=line.split("\\s+");               
               if (split.length==4) matrixlines.add(split);
           }
       }
       if (expectedLines>0 && expectedLines!=matrixlines.size()) throw new ParseError("Expected "+expectedLines+" matrix lines for motif '"+motifID+"' but found "+matrixlines.size());
       double[][] matrix=parseVerticalMatrix(matrixlines, motifID);
       target.setName(motifID);
       target.setMatrix(matrix);
       target.setShortName(motifName);
       target.setLongName(null);
       target.setQuality(6);
       return target;
    }


    private MotifCollection parseMotifCollection(List<String> input, MotifCollection target, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new MotifCollection("MotifCollection");
        int first=-1; int last=0; int size=input.size();
        if (size<1) return target; // throw new ParseError("Empty input for MotifCollection");
        int count=0;
        for (int i=0;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith("MOTIF ")) {
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
    private double[][] parseVerticalMatrix(List<String[]> list, String motifID) throws ParseError {
        if (list.isEmpty()) throw new ParseError("Missing matrix specification values for matrix: "+motifID);
        double[][] result=new double[list.size()][4];
        for (int i=0;i<list.size();i++) {
            String[] split=list.get(i);
            if (split.length!=4) throw new ParseError("Expected 4 columns in data matrix '"+motifID+"'. Got "+split.length);
            for (int j=0;j<4;j++) {
                try {
                    result[i][j]=Double.parseDouble(split[j]);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value in matrix for '"+motifID+"':" +split[j]);}
            }
        }
        return result;
    }


}





