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
public class DataFormat_AlignAce extends DataFormat {
    private String name="AlignAce";

    private static final String SITES="sites";
    private static final String MOTIFS="motifs";




    private Class[] supportedTypes=new Class[]{}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes


    public DataFormat_AlignAce() {
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
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in AlignAce format (functionality not implemented)");
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
           //System.err.println("AlignAce: Parsing for MOTIFS");
           MotifCollection collection;
           if (target!=null && target instanceof MotifCollection) collection=(MotifCollection)target;
           else collection=new MotifCollection("MotifCollection");
           String motifname=null;
           ArrayList<String> sitepatterns=null;
           Pattern motifheader=Pattern.compile("^Motif (\\d+)");
           Pattern siteline=Pattern.compile("^([acgtACGT]+)\\t(\\d+)\\t(\\d+)\\t(\\d)");
           
           for (int inputpos=0;inputpos<input.size();inputpos++) {
             String line=input.get(inputpos);
             if (line.startsWith("MAP Score:")) {
                 if (motifname==null) throw new ParseError("End of motif (MAP Score) encountered before Motif number was specififed");
                 if (sitepatterns==null || sitepatterns.isEmpty()) throw new ParseError("No sites found for motif: "+motifname);
                 Motif newMotif=new Motif(motifname);
                 double[][] matrix=Motif.getMatrixForAlignedBindingSites(sitepatterns);
                 newMotif.setMatrix(matrix);
                 collection.addMotifToPayload(newMotif);
                 motifname=null;
                 sitepatterns=null;
             } else {
                Matcher matcher;
                matcher=motifheader.matcher(line);
                if (matcher.matches()) {
                     motifname="Motif"+matcher.group(1);
                     sitepatterns=new ArrayList<String>();
                } else {
                    matcher=siteline.matcher(line);
                    if (matcher.matches()) {
                        if (sitepatterns!=null) sitepatterns.add(matcher.group(1));
                        else throw new ParseError("TFBS line encountered before Motif number was specified");
                    }
                 }
              }
           } // end for each line
           return collection;
       } else if (returntype.equalsIgnoreCase(SITES)) {
           //System.err.println("AlignAce: Parsing for SITES");
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
           Pattern sequenceLookupPattern=Pattern.compile("^#(\\d+)\\t(\\w+)");
           Pattern motifheader=Pattern.compile("^Motif (\\d+)");
           Pattern siteline=Pattern.compile("^([acgtACGT]+)\\t(\\d+)\\t(\\d+)\\t(\\d)");

           for (int inputpos=0;inputpos<input.size();inputpos++) {
             String line=input.get(inputpos);
             if (line.startsWith("MAP Score:")) {
                 motifname=null;
             } else {
                Matcher matcher;
                matcher=sequenceLookupPattern.matcher(line);
                if (matcher.matches()) {
                   String seqNumber=matcher.group(1);
                   String seqName=matcher.group(2);
                   sequenceNameLookup.put(seqNumber,seqName);
                } else {
                    matcher=motifheader.matcher(line);
                    if (matcher.matches()) {
                         motifname="Motif"+matcher.group(1);
                    } else {
                        matcher=siteline.matcher(line);
                        if (matcher.matches()) {
                            if (motifname==null) throw new ParseError("TFBS line encountered before Motif number was specified");
                            String bindingpattern=matcher.group(1);
                            String seqNumber=matcher.group(2);
                            String positonString=matcher.group(3);
                            String strandString=matcher.group(4);
                            int position=0;
                            try {
                                position=Integer.parseInt(positonString);
                            } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value: "+nfe.getMessage());}
                            if (!sequenceNameLookup.containsKey(seqNumber)) {throw new ParseError("Missing sequence name for sequence number: #"+seqNumber);}
                            String sequenceName=sequenceNameLookup.get(seqNumber);
                            int orientation=(strandString.equals("0"))?Region.REVERSE:Region.DIRECT;
                            RegionSequenceData sequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceName);
                            if (sequence==null) {throw new ParseError("Unknown sequence: "+sequenceName);}
                            Region newsite=new Region(sequence, position, position+bindingpattern.length()-1, motifname, 1.0, orientation);
                            newsite.setProperty("sequence", bindingpattern); //
                            sequence.addRegion(newsite);
                        }
                     } // end match siteline
                } // end: not sequence lookup line 'else'
             } // end: not MAP Score 'else'


           } // end for each line
           return regiondataset;
        } else throw new ParseError("Unknown returntype: "+returntype);
    }





}




