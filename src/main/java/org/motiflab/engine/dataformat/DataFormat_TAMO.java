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
public class DataFormat_TAMO extends DataFormat {
    private String name="TAMO";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_TAMO() {

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
        return "tamo";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_TAMO.parseInput(ArrayList<String> input, Data target)");
             if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, task);
        else if (target instanceof Motif) return parseMotif(input, (Motif)target);
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }


    private Motif parseMotif(List<String> input, Motif target) throws ParseError {
       String motifID="unknown";
       String shortname=null;
       String lineA=null,lineC=null,lineG=null,lineT=null;      
       if (target==null) target=new Motif("unknown");
       for (String line:input) {           
           if (line.startsWith("Log-odds matrix for Motif")) {
               line=line.substring("Log-odds matrix for Motif".length()).trim();
               String[] split=line.split("\\s+");
               try {
                  int number=Integer.parseInt(split[0]);
                  motifID="Motif"+number;
               } catch(Exception e) {}
           } else if (line.startsWith("Source:")) {
               shortname=line.substring("Source:".length()).trim();
           } else if (line.startsWith("#A")) {
               lineA=line.substring(2).trim();
           } else if (line.startsWith("#C")) {
               lineC=line.substring(2).trim();
           } else if (line.startsWith("#G")) {
               lineG=line.substring(2).trim();
           } else if (line.startsWith("#T")) {
               lineT=line.substring(2).trim();
           }             
       }
       if (lineA==null) throw new ParseError("Missing matrix line starting with '#A' for motif '"+motifID+"'");
       if (lineC==null) throw new ParseError("Missing matrix line starting with '#C' for motif '"+motifID+"'");
       if (lineG==null) throw new ParseError("Missing matrix line starting with '#G' for motif '"+motifID+"'");
       if (lineT==null) throw new ParseError("Missing matrix line starting with '#T' for motif '"+motifID+"'");
       double[][] matrix=parseHorizontalMatrix(lineA,lineC,lineG,lineT, motifID, "\\s+");
       target.setName(motifID);
       target.setShortName(shortname);       
       target.setMatrix(matrix);
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
            if (line.startsWith("Log-odds matrix for Motif")) {
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
    private double[][] parseHorizontalMatrix(String lineA, String lineC, String lineG, String lineT, String motifID, String delimiter) throws ParseError {
        String[] splitA=lineA.split(delimiter);
        String[] splitC=lineC.split(delimiter);
        String[] splitG=lineG.split(delimiter);
        String[] splitT=lineT.split(delimiter);
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





