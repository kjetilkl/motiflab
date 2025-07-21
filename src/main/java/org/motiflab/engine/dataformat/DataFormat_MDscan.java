/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_MDscan extends DataFormat {
    private String name="MDscan";

    private static final String SITES="sites";
    private static final String MOTIFS="motifs";
    
    
    
    private Class[] supportedTypes=new Class[]{}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes

    
    public DataFormat_MDscan() {
        addParameter("datatype", "sites", new String[]{"sites","motifs"},"specifies whether to parse and return motifs or binding sites");  
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
        return "mdscan";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in MDscan format (functionality not implemented)");
    }    
    
  
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       String returntype=MOTIFS;
       if (input.isEmpty()) throw new ParseError("Unable to parse empty results file");
       if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             returntype=(String)settings.getResolvedParameter("datatype",defaults,engine);
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           returntype=(String)getDefaultValueForParameter("datatype");
        }       
       if (returntype.equalsIgnoreCase(MOTIFS)) {
           MotifCollection collection;
           if (target!=null && target instanceof MotifCollection) collection=(MotifCollection)target;
           else collection=new MotifCollection("MotifCollection");
           int motifnumber=0;
           int motifsize=0;
           int found=0;
           double[][] matrix=null; // first is position, second is base (4)
           Pattern motifheader=Pattern.compile("Motif (\\d+): Wid (\\d+); Score ([0-9\\.]+); Sites (\\d+); Con (\\w+); RCon (\\w+)");
           Pattern matrixline=Pattern.compile("(\\d+)\\s+([0-9\\.]+)\\s+([0-9\\.]+)\\s+([0-9\\.]+)\\s+([0-9\\.]+)\\s+\\w\\s+\\w\\s+\\w\\s+\\w");
           int lineNumber=0;
           try {
               for (String line:input) {
                   lineNumber++;
                   if (line.startsWith("Motif")) {
                       Matcher matcher=motifheader.matcher(line);  
                       if (matcher.matches()) {
                           if (motifnumber>0) { // register previous motif                               
                               if (motifsize!=found) throw new ParseError("Expected "+motifsize+" matrix lines for motif#"+motifnumber+" but found only "+found, lineNumber);
                               Motif newMotif=new Motif("motif_"+motifnumber);
                               newMotif.setMatrix(matrix);
                               collection.addMotifToPayload(newMotif);
                               found=0;
                           }
                           motifnumber=Integer.parseInt(matcher.group(1));
                           motifsize=Integer.parseInt(matcher.group(2));
                           matrix=new double[motifsize][4];
                       }
                   } else {
                       Matcher matcher=matrixline.matcher(line);  
                       if (matcher.matches()) {
                           int pos=Integer.parseInt(matcher.group(1));
                           pos--; // output is 1-indexed so we substract 1 to get it 0-indexed
                           matrix[pos][0]=Double.parseDouble(matcher.group(2)); // A
                           matrix[pos][1]=Double.parseDouble(matcher.group(3)); // C
                           matrix[pos][2]=Double.parseDouble(matcher.group(4)); // G
                           matrix[pos][3]=Double.parseDouble(matcher.group(5)); // T                           
                           found++;
                       }
                   }               
               }
           } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+e.getMessage(), lineNumber);}
           // add last motif
           if (motifsize!=found) throw new ParseError("Expected "+motifsize+" matrix lines for motif#"+motifnumber+" but found only "+found);
           else { // register previous motif
               Motif newMotif=new Motif("motif_"+motifnumber);
               newMotif.setMatrix(matrix);
               collection.addMotifToPayload(newMotif);
           }
           return collection;
       } else if (returntype.equalsIgnoreCase(SITES)) {
          RegionDataset regiondataset;
          if (target!=null && target instanceof RegionDataset) regiondataset=(RegionDataset)target;
          else regiondataset=new RegionDataset("sites");
           // add missing RegionSequenceData objects
          ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
          for (Data seq:sequences) {
                if (regiondataset.getSequenceByName(seq.getName())==null) regiondataset.addSequence(new RegionSequenceData((Sequence)seq));     
          }                               
           int motifnumber=0;
           int motifsize=0;
           int sites=0;
           int found=0;
           Pattern motifheader=Pattern.compile("Motif (\\d+): Wid (\\d+); Score ([0-9\\.]+); Sites (\\d+); Con (\\w+); RCon (\\w+)");
           Pattern siteline=Pattern.compile(">(\\S+)\\s+Len\\s+\\d+\\s+Site\\s+\\S+\\s+([rf])\\s+(\\d+)");
           int lineNumber=0;
           try {
               for (int i=0;i<input.size()-1;i++) {
                   String line=input.get(i);
                   lineNumber++;
                   if (line.startsWith("Motif")) {
                       Matcher matcher=motifheader.matcher(line);  
                       if (matcher.matches()) {
                           if (motifnumber>0 && sites!=found) throw new ParseError("Expected "+sites+" sites for motif#"+motifnumber+" but found only "+found, lineNumber);
                           motifnumber=Integer.parseInt(matcher.group(1));
                           motifsize=Integer.parseInt(matcher.group(2));
                           sites=Integer.parseInt(matcher.group(4));
                           found=0;
                       } else throw new ParseError("Unable to parse line according to expectations:\n"+line, lineNumber);
                   } else if (line.startsWith(">")) {
                       Matcher matcher=siteline.matcher(line);  
                       if (matcher.matches()) {
                           found++;
                           String sequenceName=matcher.group(1);
                           String orientationString=matcher.group(2);
                           int orientation=(orientationString.equals("f"))?Region.DIRECT:Region.REVERSE;
                           int position=Integer.parseInt(matcher.group(3));       
                           position--; // position in output is 1-indexed so we substract 1 to get it 0-indexed
                           RegionSequenceData sequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceName);
                           if (sequence==null) continue; // is this the best option? or should I report error?
                           Region newsite=new Region(sequence, position, position+motifsize-1, "motif_"+motifnumber, 1f, orientation);
                           i++;
                           String bindingpattern=input.get(i).trim();
                           if (orientation==Region.REVERSE) bindingpattern=MotifLabEngine.reverseSequence(bindingpattern);
                           newsite.setProperty("sequence", bindingpattern); //
                           sequence.addRegion(newsite);                           
                       } else throw new ParseError("Unable to parse line according to expectations:\n"+line, lineNumber);
                   }               
               }
           } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value: "+e.getMessage(), lineNumber);}

          if (sites!=found) throw new ParseError("Expected "+sites+" sites for motif#"+motifnumber+" but found only "+found);
          return regiondataset;
       } else throw new ParseError("Unknown returntype: "+returntype);
    }
            
            
    
    
}
