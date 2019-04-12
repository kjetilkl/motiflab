/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.List;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_INCLUSive_Motif extends DataFormat {
    private String name="INCLUSive_Motif_Model";
    
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};

    
    public DataFormat_INCLUSive_Motif() {
 
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
        return "mtrx";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings setting, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);
        StringBuilder outputString=new StringBuilder("#INCLUSive Motif Model v1.0\n#\n");
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif,outputString);
                //task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%30==0) Thread.yield();                                
            } 
        } else if (dataobject instanceof Motif){
               outputMotif((Motif)dataobject,outputString);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }    
    
    
    /** outputformats a single motif */
    private void outputMotif(Motif motif,StringBuilder outputString) {
            double[][] matrix=motif.getMatrix();
            if (matrix==null) return;
            outputString.append("#ID = ");outputString.append(motif.getName());outputString.append("\n");
            outputString.append("#W = ");outputString.append(motif.getLength());outputString.append("\n");
            outputString.append("#Consensus = ");outputString.append(motif.getConsensusMotif());outputString.append("\n");
            for (double[] row:matrix) {
              outputString.append(row[0]);outputString.append("\t");outputString.append(row[1]);outputString.append("\t");outputString.append(row[2]);outputString.append("\t");outputString.append(row[3]);outputString.append("\n");
            }
            outputString.append("\n");
    }

      

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_INCLUSive_Motif.parseInput(ArrayList<String> input, Data target)");
             if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, task);
        else if (target instanceof Motif) return parseMotif(input, (Motif)target);
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }

    private Motif parseMotif(List<String> input, Motif target) throws ParseError {
       int expectedsize=0;
       String consensus="";
       String id="";
       String motifname="";
       if (target==null) target=new Motif("temporary");
       ArrayList<String>matrixlines=new ArrayList<String>();
       for (String line:input) {
           line=line.trim();
           if (line.isEmpty()) continue;
           else if (line.startsWith("#ID")) {
               String[] split=line.split("\\s*=\\s*");
               if (split.length==2) {
                   String[] namesplit=split[1].split("-",2);
                   if (namesplit.length==2) {id=namesplit[0];motifname=namesplit[1];}
                   else id=namesplit[0];
               }
           } else if (line.startsWith("#W")) {
               String[] split=line.split("\\s*=\\s*");
               if (split.length==2) {try {expectedsize=Integer.parseInt(split[1]);} catch(Exception e){}}               
           } else if (line.startsWith("#Consensus") || line.startsWith("#Conensus")) {
               String[] split=line.split("\\s*=\\s*");
               if (split.length==2) {consensus=split[1];}
           } else if (line.startsWith("#")) {
               continue;
           } else matrixlines.add(line);
       }
       if (expectedsize>0 && matrixlines.size()!=expectedsize) throw new ParseError("Expected "+expectedsize+" matrix rows in Motif input. Got "+matrixlines.size());
       double[][] matrix=parseValueLines(matrixlines);
       target.setConsensusMotif(consensus);
       target.setName(id);
       target.setShortName(motifname);
       target.setMatrix(matrix);
       return target;
    } 
                        
    
    private MotifCollection parseMotifCollection(List<String> input, MotifCollection target, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new MotifCollection("MotifCollection");
        int first=0; int last=0; int size=input.size();
        if (size<1) return target; // throw new ParseError("Empty input for MotifCollection");
        if (!input.get(0).startsWith("#INCLUSive Motif Model")) throw new ParseError("Unrecognized header for INCLUSive Motif format: "+input.get(0));
        int count=0;
        for (int i=1;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith("#ID")) {
                count++;
                if (count%30==0) {
                  if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                  if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
                  Thread.yield();
                }                 
                if (first==0) {
                    first=i;
                } else {
                    last=i;
                    Motif motif=parseMotif(input.subList(first, last), null);
                    target.addMotifToPayload(motif);
                    first=i;
                    last=0;
                }
            }
        }
        if (first>0) {
            Motif motif=parseMotif(input.subList(first, size), null);
            target.addMotifToPayload(motif);
        }
        return target;
    }
            
            
    
    /** */
    private double[][] parseValueLines(List<String> list) throws ParseError {
        double[][] result=new double[list.size()][4];
        for (int i=0;i<list.size();i++) {
            String line=list.get(i);
            String[] split=line.split("\\s+");
            if (split.length!=4) throw new ParseError("Expected 4 columns in data matrix. Got "+split.length);
            for (int j=0;j<4;j++) {
                try {
                    result[i][j]=Double.parseDouble(split[j]);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value = " +split[j]);}
            }
        }
        return result;
    }    
    
}

        
       
        
        
