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
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_SeSiMCMC extends DataFormat {
    private String name="SeSiMCMC";

    private static final String SITES="sites";
    private static final String MOTIFS="motifs";




    private Class[] supportedTypes=new Class[]{}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes


    public DataFormat_SeSiMCMC() {
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
        return "txt";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in SeSiMCMC format (functionality not implemented)");
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

       Pattern siteline=Pattern.compile("^([a-zA-Z_0-9.+-]+)\\s+(\\d+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S)\\s+([acgtACGT]+)\\s+([acgtACGT]+).*");
       String motifname="SeSiMCMCmotit"; // it only returns one motif (I think)
       if (returntype.equalsIgnoreCase(MOTIFS)) {
           //System.err.println("SeSiMCMC: Parsing for MOTIFS");
           MotifCollection collection;
           if (target!=null && target instanceof MotifCollection) collection=(MotifCollection)target;
           else collection=new MotifCollection("MotifCollection");
           ArrayList<String> sitepatterns=new ArrayList<String>();

           for (int inputpos=0;inputpos<input.size();inputpos++) {
              String line=input.get(inputpos);
              if (line.startsWith("#")) continue;
              Matcher matcher=siteline.matcher(line);
              if (matcher.matches()) {
                  if (!line.endsWith("*")) sitepatterns.add(matcher.group(7)); // lines ending with * are sites that were not used to make the motif model
              }

          } // end for each line
          if (!sitepatterns.isEmpty()) {
              // it only returns one motif (I think), so make a motif for that
              Motif newMotif=new Motif(motifname);
              double[][] matrix=Motif.getMatrixForAlignedBindingSites(sitepatterns);
              newMotif.setMatrix(matrix);
              collection.addMotifToPayload(newMotif);
          }
          return collection;
       } else if (returntype.equalsIgnoreCase(SITES)) {
           //System.err.println("SeSiMCMC: Parsing for SITES");
           RegionDataset regiondataset;
           if (target!=null && target instanceof RegionDataset) regiondataset=(RegionDataset)target;
           else regiondataset=new RegionDataset("sites");
           // add missing RegionSequenceData objects
           ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
           for (Data seq:sequences) {
                if (regiondataset.getSequenceByName(seq.getName())==null) regiondataset.addSequence(new RegionSequenceData((Sequence)seq));
           }
           for (int inputpos=0;inputpos<input.size();inputpos++) {
              String line=input.get(inputpos);
              if (line.startsWith("#")) continue;
              Matcher matcher=siteline.matcher(line);
              if (matcher.matches()) {
                  String sequenceName=matcher.group(1);
                  int start=0;
                  int end;
                  try {
                      start=Integer.parseInt(matcher.group(2));
                  } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value. "+matcher.group(2), inputpos+1);}
                  try {
                      int length=Integer.parseInt(matcher.group(3));
                      end=start+length-1;
                  } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value. "+matcher.group(3), inputpos+1);}
                  double score=0;
                  try {
                      score=Double.parseDouble(matcher.group(4));
                  } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value. "+matcher.group(4), inputpos+1);}
                  String bindingpattern=matcher.group(7);
                  String strandString=matcher.group(5);
                  int orientation=Region.DIRECT;
                  if (strandString.equals(">")) orientation=Region.DIRECT;
                  else if (strandString.equals("<")) orientation=Region.REVERSE;
                  else throw new ParseError("Unknown strand for motif: "+strandString, inputpos+1);
                  RegionSequenceData sequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceName);
                  if (sequence==null) {throw new ParseError("Unknown sequence: "+sequenceName, inputpos+1);}
                  Region newsite=new Region(sequence, start, end, motifname, score, orientation);
                  newsite.setProperty("sequence", bindingpattern); //
                  sequence.addRegion(newsite);
              } 

          } // end for each line
          return regiondataset;

        } else throw new ParseError("Unknown returntype: "+returntype);
     }





}




