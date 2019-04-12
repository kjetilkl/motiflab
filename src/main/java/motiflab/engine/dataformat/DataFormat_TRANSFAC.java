package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.List;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_TRANSFAC extends DataFormat {
    private String name="TRANSFAC";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_TRANSFAC() {
        //addOptionalParameter("Format","Default", new String[]{"Default","Frequencies"},"Default: output matrices in the same format as used in the table.<br>Frequencies: output matrices as frequencies (convert from default format if necessary)");
        addParameter("OutputACCasID", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE}, "Output", false, true);
        setParameterFilter("OutputACCasID","output"); 
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
        return "dat";
    }

    @Override
    public String[] getSuffixAlternatives() {return new String[]{"dat","txt"};}        
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }       
        setProgress(5);
        boolean outputAccAsID=false;
        if (settings!=null) {
        try{
             Parameter[] defaults=getParameters();
             outputAccAsID=(Boolean)settings.getResolvedParameter("OutputACCasID",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           outputAccAsID=(Boolean)getDefaultValueForParameter("OutputACCasID");
        }
        StringBuilder outputString=new StringBuilder();
        outputString.append("VV  TRANSFAC MATRIX TABLE\nXX\n//\n");
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, outputString, outputAccAsID);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%20==0) Thread.yield();
            }
        } else if (dataobject instanceof Motif){
                outputMotif((Motif)dataobject, outputString, outputAccAsID);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    /** outputformats a single motif */
    private void outputMotif(Motif motif, StringBuilder outputString, boolean outputAccAsID) {
            //double[][] matrix=(format.equals("Frequencies"))?motif.getMatrixAsFrequencyMatrix():motif.getMatrix();
            boolean isCountMatrix=motif.getMatrixType()==Motif.COUNT_MATRIX;
            double[][] matrix=motif.getMatrix();
            if (matrix==null) return;
            outputString.append("AC  ");
            outputString.append(motif.getName());
            outputString.append("\nXX\n");
            if (outputAccAsID) {
                outputString.append("ID  ");
                outputString.append(motif.getName());
                outputString.append("\nXX\n");               
            } else if (motif.getShortName()!=null && !motif.getShortName().isEmpty()) {
                outputString.append("ID  ");
                outputString.append(motif.getShortName());
                outputString.append("\nXX\n");               
            }
            if (motif.getShortName()!=null && !motif.getShortName().isEmpty()) {
                outputString.append("NA  ");
                outputString.append(Motif.cleanUpMotifShortName(motif.getShortName(),false));
                outputString.append("\nXX\n");
            }
            if (motif.getLongName()!=null && !motif.getLongName().isEmpty()) {
                outputString.append("DE  ");
                outputString.append(motif.getLongName());
                outputString.append("\nXX\n");
            }
            // horisontal orientation
            int length=motif.getLength();
            String consensus=motif.getConsensusMotif();
            outputString.append("P0      A      C      G      T\n");
            for (int i=0;i<length;i++) {
                int spos=i+1;
                String pos=(spos<10)?("0"+spos):(""+spos);
                outputString.append(pos);
                outputString.append(" ");
                outputString.append((isCountMatrix)?formatFixedWidthInteger((int)matrix[i][0],6):formatFixedWidthDouble(matrix[i][0],6,2));
                outputString.append(" ");
                outputString.append((isCountMatrix)?formatFixedWidthInteger((int)matrix[i][1],6):formatFixedWidthDouble(matrix[i][1],6,2));
                outputString.append(" ");
                outputString.append((isCountMatrix)?formatFixedWidthInteger((int)matrix[i][2],6):formatFixedWidthDouble(matrix[i][2],6,2));
                outputString.append(" ");
                outputString.append((isCountMatrix)?formatFixedWidthInteger((int)matrix[i][3],6):formatFixedWidthDouble(matrix[i][3],6,2));
                outputString.append("      ");
                outputString.append(Character.toUpperCase(consensus.charAt(i)));
                outputString.append("\n");
            }
            outputString.append("XX\n//\n");
    }



    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_TRANSFAC.parseInput(ArrayList<String> input, Data target)");
             if (target instanceof MotifCollection) return parseMotifCollection(input, (MotifCollection)target, task);
        else if (target instanceof Motif) {
            Motif motif=parseMotif(input, (Motif)target);
            if (motif!=null) return motif;
            else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
        }
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }


    private Motif parseMotif(List<String> input, Motif target) throws ParseError {
       int unknowncount=0;     
       String motifID=null;
       String motifShortName=null;
       String motifLongName=null;
       String alternativeName=null;
       int quality=6;
       boolean inMatrixSection=false;
       if (target==null) target=new Motif("unknown");
       ArrayList<String> matrixlines=new ArrayList<String>();
       ArrayList<String> organismlines=new ArrayList<String>();
       ArrayList<String> factorlines=new ArrayList<String>();
       for (String line:input) {
           line=line.trim();
           if (line.isEmpty() || line.startsWith("//")) continue;
           else if (line.startsWith("XX")) {
               if (inMatrixSection) inMatrixSection=false;
               continue;
           }
           else if (line.startsWith("AC")) {
               String[] split=line.split("\\s+",2);
               if (split.length<2) throw new ParseError("Unable to parse line:\n"+line);
               motifID=split[1];
           } else if (line.startsWith("ID")) {
               String[] split=line.split("\\s+",2);
               if (split.length<2) throw new ParseError("Unable to parse line:\n"+line);
               motifShortName=split[1];
           } else if (line.startsWith("DE")) {
               String[] split=line.split("\\s+",2);
               if (split.length<2) throw new ParseError("Unable to parse line:\n"+line);
               motifLongName=split[1];
           } else if (line.startsWith("NA")) {
               String[] split=line.split("\\s+",2);
               if (split.length<2) throw new ParseError("Unable to parse line:\n"+line);
               alternativeName=split[1];
           } else if (line.startsWith("P0") || line.startsWith("PO")) {
               inMatrixSection=true;
           } else if (line.startsWith("BF") && line.contains(";")) {
             String[] elements=line.substring(2).trim().split(";\\s+");
             for (int g=0;g<elements.length;g++) {
                 if (elements[g].startsWith("Species: ")) {
                     String species=elements[g].substring("Species: ".length());
                     if (species.endsWith(".")) species=species.substring(0,species.length()-1);
                     if (species.contains(",")) {
                         String[] names=species.split(",\\s*");
                         species=names[0]+" ("+names[1]+")";
                     }
                     if (!organismlines.contains(species)) organismlines.add(species);
                 } else if (!elements[g].matches("T\\d\\d\\d\\d\\d")) { // do not add TRANSFAC TF IDs (only TF names)
                     String factor=elements[g];
                     if (!factorlines.contains(factor)) factorlines.add(factor);                                             
                 }
             }
           } else if (inMatrixSection) {
              matrixlines.add(line);
           }
       }
       if (motifID==null && motifShortName==null && alternativeName==null && matrixlines.isEmpty()) return null; // there is nothing here
       if (motifID==null) {
           if (alternativeName!=null) motifID=alternativeName;
           else {unknowncount++;motifID="unknown"+unknowncount;}
       }
       if (matrixlines.isEmpty()) throw new ParseError("Missing matrix specification for motif '"+motifID+"'");
       motifID=Motif.cleanUpMotifID(motifID);
       double[][] matrix=null;
       matrix=parseMatrix(matrixlines,motifID);
       target.setMatrix(matrix);
       target.setConsensusMotif(Motif.getConsensusForMatrix(matrix));
       target.setName(motifID);
       if (motifShortName!=null) motifShortName=fixShortName(motifShortName);
       target.setShortName(motifShortName);
       target.setLongName(motifLongName);
       target.setQuality(quality);
       //try {target.setClassification(classification);} catch (ExecutionError e) {throw new ParseError(e.getMessage());}
       if (!factorlines.isEmpty()) {     
           target.setBindingFactors(arrayToString(factorlines));       
       }
        if (!organismlines.isEmpty()) {
           target.setOrganisms(arrayToString(organismlines));
       }
       
       target.setICcontent(Motif.calculateInformationContent(matrix, false));
       return target;
    }

    private String arrayToString(ArrayList<String> list) {
        StringBuilder string=new StringBuilder();
        for (int i=0;i<list.size();i++) {
            string.append(list.get(i));
            if (i<list.size()-1) string.append(",");
        }
        return string.toString();
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
        int count=0;
        boolean firstEncountered=false;
        for (int i=0;i<size;i++) {
            String line=input.get(i).trim();
            if (line.startsWith("//") || (line.startsWith("AC") && !firstEncountered)) {
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
                    if (motif!=null) target.addMotifToPayload(motif);
                    first=i;
                    last=0;
                }
                firstEncountered=true;
            }
        }
        if (first>=0) {
            Motif motif=parseMotif(input.subList(first, size), null);
            if (motif!=null) target.addMotifToPayload(motif);
        }
        return target;
    }




    /** */
    private double[][] parseMatrix(ArrayList<String> list, String motifname) throws ParseError {
        int size=list.size();
        boolean startsAtZero=false; // allows numbering rows starting at 00 rather than 01.
        double[][] matrix=new double [size][4];
        for (String line:list) {
            String[] elements=line.split("\\s+");
            if (elements.length<5 || elements.length>6) throw new ParseError("Unable to parse matrix line (expected 5 or 6 columns):\n"+line);
            int pos=-1;
            if (elements[0].equals("00")) startsAtZero=true;
            try {pos=Integer.parseInt(elements[0]);}
            catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric matrix position for motif '"+motifname+"': "+line);}
            if (!startsAtZero) pos--;
            if (pos<0 || pos>=size) throw new ParseError("Matrix position "+pos+" 'out of bounds' for motif '"+motifname+"':  "+elements[0]);
            try {matrix[pos][0]=Double.parseDouble(elements[1]);}
            catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for base=A in matrix for '"+motifname+"': "+line);}
            try {matrix[pos][1]=Double.parseDouble(elements[2]);}
            catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for base=C in matrix for '"+motifname+"': "+line);}
            try {matrix[pos][2]=Double.parseDouble(elements[3]);}
            catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for base=G in matrix for '"+motifname+"': "+line);}
            try {matrix[pos][3]=Double.parseDouble(elements[4]);}
            catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for base=T in matrix for '"+motifname+"': "+line);}
        }
        return matrix;
    }

}
