 /*
 
 
 */

package org.motiflab.engine.dataformat;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
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
public class DataFormat_MEME extends DataFormat {
    private String name="MEME";
    private static final int RELATIVE=0;
    private static final int GENOMIC=1;
    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE_ORIENTATION="Relative";
    private static final String OPPOSITE="Opposite";
    private static final String SITES="sites";
    private static final String MOTIFS="motifs";
    
    
    
    private Class[] supportedTypes=new Class[]{}; // Since we can not format with this DataFormat (only parse) we don't want to "expose" it to the user as an option. Hence we list no classes

    
    public DataFormat_MEME() {
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
        return "xml";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in MEME format (functionality not implemented)");
    }    
    
  
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       String returntype=MOTIFS;
       if (input.isEmpty()) throw new ParseError("Unable to parse empty results file");
       StringBuilder stringbuilder=new StringBuilder();
       for (String line:input) stringbuilder.append(line);
       String memeresults=stringbuilder.toString();
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
           try {
             DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             Document doc = builder.parse(new InputSource(new StringReader(memeresults)));
             NodeList motifs = doc.getElementsByTagName("motif");             
             for (int i=0;i<motifs.getLength();i++) {                
                Element motif = (Element) motifs.item(i);
                String motifID=motif.getAttribute("id");
                NodeList probabilitiesList = motif.getElementsByTagName("probabilities");
                if (probabilitiesList.getLength()==0) throw new ParseError("Unable to find probabilities matrix for motif '"+motifID+"'");
                Element probabilities = (Element) probabilitiesList.item(0);
                NodeList matrixList = probabilities.getElementsByTagName("alphabet_matrix");
                if (matrixList.getLength()==0) throw new ParseError("Unable to find alphabet matrix for motif '"+motifID+"'");
                Element matrix = (Element) matrixList.item(0);            
                NodeList bases = matrix.getElementsByTagName("alphabet_array");
                int motifwidth=bases.getLength();
                double[][] pwm=new double[motifwidth][4];
                for (int j=0;j<motifwidth;j++) {
                    Element base = (Element) bases.item(j);    
                    NodeList letters = base.getElementsByTagName("value");
                    for (int k=0;k<4;k++) {
                       Element lettervalue = (Element) letters.item(k);
                       String letterID=lettervalue.getAttribute("letter_id");
                       letterID=letterID.substring(letterID.length()-1);
                       try {
                           double value=Double.parseDouble(lettervalue.getTextContent());
                                if (letterID.equals("A")) pwm[j][0]=value;
                           else if (letterID.equals("C")) pwm[j][1]=value;
                           else if (letterID.equals("G")) pwm[j][2]=value;
                           else if (letterID.equals("T")) pwm[j][3]=value;
                       } catch (NumberFormatException e) {
                           throw new ParseError("Unable to parse expected numeric value: '"+lettervalue.getTextContent()+"'");
                       }
                    } // end letters A,C,G,T            
                } // end base position
                Motif newmotif=new Motif(motifID);
                newmotif.setMatrix(pwm);
                collection.addMotifToPayload(newmotif);         
             } // end for each motif
             return collection;
            } catch (Exception e) {
              // StackTraceElement[] list=e.getStackTrace(); for (StackTraceElement el:list) engine.logMessage(el.toString());
              throw new ParseError("["+e.getClass().toString()+"] "+e.getMessage());
              //throw new ParseError("["+e.getClass().toString()+"]");
            }  
       } else if (returntype.equalsIgnoreCase(SITES)) {
          RegionDataset regiondataset;
          if (target!=null && target instanceof RegionDataset) regiondataset=(RegionDataset)target;
          else regiondataset=new RegionDataset("sites");
           // add missing RegionSequenceData objects
          ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
          for (Data seq:sequences) {
                if (regiondataset.getSequenceByName(seq.getName())==null) regiondataset.addSequence(new RegionSequenceData((Sequence)seq));     
          }
          try {
             DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
             Document doc = builder.parse(new InputSource(new StringReader(memeresults)));
             // 
             HashMap<String,String> sequenceIDmap=new HashMap<String,String>();
             NodeList trainingset = doc.getElementsByTagName("training_set");
             if (trainingset!=null && trainingset.getLength()>0) {
                Element trainingsetElement = (Element) trainingset.item(0);
                NodeList sequenceList = trainingsetElement.getElementsByTagName("sequence");
                for (int j=0;j<sequenceList.getLength();j++) { // for each sequence
                    Element sequenceElement = (Element)sequenceList.item(j);
                    String sequenceID=sequenceElement.getAttribute("id");
                    String sequenceName=sequenceElement.getAttribute("name");
                    sequenceIDmap.put(sequenceID,sequenceName);
                }
             }
             // 
             NodeList motifs = doc.getElementsByTagName("motif");
             for (int i=0;i<motifs.getLength();i++) {                
                Element motif = (Element) motifs.item(i);
                String motifID=motif.getAttribute("id");
                NodeList sitesList = motif.getElementsByTagName("contributing_site");
                for (int j=0;j<sitesList.getLength();j++) { // for each site
                    Element bindingsite = (Element) sitesList.item(j);    
                    String sequenceID=bindingsite.getAttribute("sequence_id");
                    String bindingpattern="";
                    String strandstring=bindingsite.getAttribute("strand");
                    int orientation=Sequence.DIRECT;
                    if (strandstring.equals("minus")) orientation=Sequence.REVERSE;
                    double score=1.0f;                    
                    int position=0;
                    try {
                        position=Integer.parseInt(bindingsite.getAttribute("position"));
                    } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric value for site position: '"+bindingsite.getAttribute("position")+"'");}
                    NodeList letters = bindingsite.getElementsByTagName("letter_ref");
                    for (int k=0;k<letters.getLength();k++) {
                       Element lettervalue = (Element) letters.item(k);
                       String letterID=lettervalue.getAttribute("letter_id");
                       letterID=letterID.substring(letterID.length()-1);
                       bindingpattern+=letterID;
                       
                    } // end letters A,C,G,T  
                    RegionSequenceData sequence=null;
                    if (sequenceID.startsWith("sequence_")) {
                        String sequenceName=sequenceIDmap.get(sequenceID);
                        if (sequenceName!=null) sequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceName);
                    } else sequence=(RegionSequenceData)regiondataset.getSequenceByName(sequenceID);
                    if (sequence==null) engine.logMessage("Unknown sequence '"+sequenceID+"' referenced in MEME results file");
                    if (sequence==null) continue; // is this the best option?
                    Region newsite=new Region(sequence, position, position+bindingpattern.length()-1, motifID, score, orientation);
                    newsite.setProperty("sequence", bindingpattern);
                    sequence.addRegion(newsite);
                } // end base position       
             } // end for each motif
             return regiondataset;
          } catch (Exception e) {
              //StackTraceElement[] list=e.getStackTrace(); for (StackTraceElement el:list) engine.logMessage(el.toString());
              throw new ParseError("["+e.getClass().toString()+"] "+e.getMessage());
          }            
       } else throw new ParseError("Unknown returntype: "+returntype);
    }
            
            
    
    
}

        
       
        
        
