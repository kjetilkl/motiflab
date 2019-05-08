/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Region;
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
public class DataFormat_Weeder extends DataFormat {
    private String name="Weeder";

    private static final String SITES="sites";
    private static final String MOTIFS="motifs";
    private static final String EXACT_STRING="From exact pattern";
    private static final String BEST_OCCURRENCES="From best sites";
    private static final String ALL_OCCURRENCES="From all sites";
    
    
    
    private Class[] supportedTypes=new Class[]{}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes

    
    public DataFormat_Weeder() {
        addParameter("datatype", "sites", new String[]{"sites","motifs"},"specifies whether to parse and return motifs or binding sites");  
        addParameter("motifmodel", EXACT_STRING, new String[]{EXACT_STRING,BEST_OCCURRENCES,ALL_OCCURRENCES},"specifies how to build the matrices");  
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
        return false; // we do not want to announce that this dataformat can parse anything. This way it will not show up as a selectable format in menus
        // return (data instanceof RegionSequenceData || data instanceof RegionDataset); // The format can parse MotifCollections also but we do not want to announce that!
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return false; // we do not want to announce that this dataformat can parse anything. This way it will not show up as a selectable format in menus
        // return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class)); // The format can parse MotifCollections also but we do not want to announce that!
    }
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "wee";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in Weeder format (functionality not implemented)");
    }    
    
  
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       String returntype=MOTIFS;
       String modeltype=EXACT_STRING;
       if (input.isEmpty()) throw new ParseError("Unable to parse empty results file");
       if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             returntype=(String)settings.getResolvedParameter("datatype",defaults,engine);
             modeltype=(String)settings.getResolvedParameter("motifmodel",defaults,engine);
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           returntype=(String)getDefaultValueForParameter("datatype");
           modeltype=(String)getDefaultValueForParameter("motifmodel");
        }   
       boolean contentOK=checkContentContainsAdvice(input.get(input.size()-1));
       if (!contentOK && returntype.equalsIgnoreCase(SITES)) engine.logMessage("WARNING: Weeder Adviser gave no advice. Returning only motifs (exact strings) but no sites!");
       if (returntype.equalsIgnoreCase(MOTIFS)) {
           //System.err.println("Weeder: Parsing for MOTIFS");
           MotifCollection collection;
           if (target!=null && target instanceof MotifCollection) collection=(MotifCollection)target;
           else collection=new MotifCollection("MotifCollection");
           String motifname=null;
           int motifsize=-1;
           int found=-1;
           double[][] matrix=null; // first is position, second is base (4)
           Pattern motifheader=(contentOK)?Pattern.compile("^([AaCcGgTt]+)"):Pattern.compile("(\\d+)\\)\\s+([AaCcGgTt]+)\\s+\\d.+");
           Pattern matrixline=Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
           try {
              for (int inputpos=0;inputpos<input.size();inputpos++) {
                 String line=input.get(inputpos);
                 Matcher matcher;
                 if (contentOK) {
                    matcher=motifheader.matcher(line);
                    if (matcher.matches()) {
                         motifname=matcher.group(1);
                         motifsize=motifname.length();
                         found=0;
                         matrix=new double[motifsize][4];
                         //System.err.println("Parsing motifs. Found motif="+motifname+"  size=motifsize");
                         if (contentOK) inputpos++; // skip next line, it will just be the same motif in reverse direction
                    } else {
                        matcher=matrixline.matcher(line);
                        if (matcher.matches()) {
                             found++;
                             //System.err.println("Found matrixline["+found+"/"+motifsize+"]=> A="+matcher.group(2)+", C="+matcher.group(3)+", G="+matcher.group(4)+", T="+matcher.group(5)+", A="+matcher.group(6)+", C="+matcher.group(7)+", G="+matcher.group(8)+", T="+matcher.group(9));
                             int pos=Integer.parseInt(matcher.group(1));
                             pos--; // subtract 1 to get zero indexed
                             if (modeltype.equals(ALL_OCCURRENCES)) {
                                   matrix[pos][0]=Double.parseDouble(matcher.group(2)); // A all
                                   matrix[pos][1]=Double.parseDouble(matcher.group(3)); // C all
                                   matrix[pos][2]=Double.parseDouble(matcher.group(4)); // G all
                                   matrix[pos][3]=Double.parseDouble(matcher.group(5)); // T all                                   
                             } else if (modeltype.equals(BEST_OCCURRENCES)) {
                                   matrix[pos][0]=Double.parseDouble(matcher.group(6)); // A best
                                   matrix[pos][1]=Double.parseDouble(matcher.group(7)); // C best
                                   matrix[pos][2]=Double.parseDouble(matcher.group(8)); // G best
                                   matrix[pos][3]=Double.parseDouble(matcher.group(9)); // T best                                  
                             } 
                             if (found==motifsize) {
                                Motif newMotif=new Motif(motifname);
                                if (modeltype.equals(EXACT_STRING)) newMotif.setMatrix(getMatrixFromExactString(motifname));
                                else newMotif.setMatrix(matrix);
                                collection.addMotifToPayload(newMotif);  
                                found=-1; motifsize=-1;
                             }
                        }                        
                    } 
                 }  // end parse single line contentOK=true
                 else {
                    matcher=motifheader.matcher(line);
                    if (matcher.matches()) {      
                         motifname=matcher.group(2);
                         motifsize=motifname.length();
                         found=motifsize;
                         Motif newMotif=new Motif(motifname);
                         newMotif.setMatrix(getMatrixFromExactString(motifname));
                         collection.addMotifToPayload(newMotif);                           
                    }           
                 }// end parse single line contentOK=false
               } // end for each line
               if (found!=motifsize) throw new ParseError("Reached end of Weeder results file unexpectedly");
           } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+e.getMessage());}
           return collection;
       } else if (returntype.equalsIgnoreCase(SITES)) {
           //System.err.println("Weeder: Parsing for SITES");
           RegionDataset regiondataset;
           if (target!=null && target instanceof RegionDataset) regiondataset=(RegionDataset)target;
           else regiondataset=new RegionDataset("sites");
           // add missing RegionSequenceData objects
           ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
           for (Data seq:sequences) {
                if (regiondataset.getSequenceByName(seq.getName())==null) regiondataset.addSequence(new RegionSequenceData((Sequence)seq));     
           }                               
           String motifname=null;
           HashMap<String,String> sequenceNameLookup = new HashMap<String,String>();
           Pattern sequenceLookupPattern=Pattern.compile("^Sequence\\s+(\\d+)\\s+:\\s>([a-zA-Z_0-9.+-]+)");
           Pattern motifheader=Pattern.compile("^([AaCcGgTt]+)");
           Pattern siteline=Pattern.compile("^\\s*(\\d+)\\s+([\\+\\-])\\s+\\[?([AaCcGgTt]+)\\]?\\s+(\\d+)\\s+\\(([\\d\\.]+)\\)\\s*");
           int lineNumber=0;
           try {
              for (int inputpos=0;inputpos<input.size();inputpos++) {
                    lineNumber++;
                    String line=input.get(inputpos);
                    Matcher matcher;
                    matcher=motifheader.matcher(line);
                    if (matcher.matches()) {
                        //System.err.println("Parsing sites. Found motif="+motifname); 
                        motifname=matcher.group(1);
                        if (contentOK) inputpos++; // skip next line, it will just be the same motif in reverse direction
                    } else {
                        matcher=sequenceLookupPattern.matcher(line);
                        if (matcher.matches()) {
                            sequenceNameLookup.put(matcher.group(1), matcher.group(2));
                        }                            
                        else {
                            matcher=siteline.matcher(line);
                            if (matcher.matches()) {                                 
                                 String sequenceName=sequenceNameLookup.get(matcher.group(1));
                                 if (sequenceName==null) throw new ParseError("No name found for sequence #"+matcher.group(1), lineNumber);
                                 String orientationString=matcher.group(2);
                                 int orientation=(orientationString.equals("+"))?Region.DIRECT:Region.REVERSE;
                                 String bindingpattern=matcher.group(3);       
                                 int position=Integer.parseInt(matcher.group(4));       
                                 double score=Double.parseDouble(matcher.group(5));       
                                 position--; // position in output is 1-indexed so we substract 1 to get it 0-indexed
                                 RegionSequenceData sequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceName);
                                 //System.err.println("Parsing sites. Found siteline["+sequence+"]=>"+line); 
                                 if (sequence==null) continue; // is this the best option? or should I report error?
                                 Region newsite=new Region(sequence, position, position+motifname.length()-1, motifname, score, orientation);
                                 newsite.setProperty("sequence", bindingpattern); //
                                 sequence.addRegion(newsite);                                   
                            }
                        }                      
                    } // end parse single line
               } // end for each line
           } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+e.getMessage(), lineNumber);}
          return regiondataset;
       } else throw new ParseError("Unknown returntype: "+returntype);
    }         
            
/** Returns true if advice was given and false if no advice was given */       
private boolean checkContentContainsAdvice(String line) {
    return !line.contains("Sorry! No advice on this one");
}       


private double[][] getMatrixFromExactString(String consensus) {
    int size=consensus.length();
    double[][] matrix=new double[size][4];
    for (int i=0;i<size;i++) {
        char base=Character.toUpperCase(consensus.charAt(i));
             if (base=='A') matrix[i][0]=100.0;
        else if (base=='C') matrix[i][1]=100.0;
        else if (base=='G') matrix[i][2]=100.0;
        else if (base=='T') matrix[i][3]=100.0;       
    }
    return matrix;
}

private void debug(double[][] matrix) {
    char[] bases=new char[]{'A','C','G','T'};
    for (int j=0;j<bases.length;j++) {
        System.err.print(bases[j]+" => [");
        for (int i=0;i<matrix.length;i++) {
            System.err.print(matrix[i][j]);
            if (i<matrix.length-1) System.err.print(", ");
        }
        System.err.println("]");
    }
    
}

}
