/*
 
 
 */

package motiflab.engine.dataformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Region;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.Motif;
import motiflab.engine.data.SequenceCollection;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
/**
 *
 * @author kjetikl
 */
public class DataFormat_CisML extends DataFormat {
    private String name="CisML";
    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE_ORIENTATION="Relative";
    private static final String OPPOSITE="Opposite";
    
    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};
    
    public DataFormat_CisML() { 
        addParameter("Relative-offset", new Integer(1), null,"Start relative positions at 0 or 1 (or any other coordinate)");
        addParameter("Orientation", RELATIVE_ORIENTATION,new String[]{DIRECT,REVERSE,RELATIVE_ORIENTATION,OPPOSITE},"Orientation of relative coordinates");        
    }
        
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof RegionSequenceData || data instanceof RegionDataset);
    }
    
    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class));
    }
    
    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof RegionSequenceData || data instanceof RegionDataset);
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(RegionSequenceData.class) || dataclass.equals(RegionDataset.class));
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
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        int relativeoffset=1;
        String orientation=RELATIVE_ORIENTATION;        
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             relativeoffset=(Integer)settings.getResolvedParameter("Relative-offset",defaults,engine);
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           relativeoffset=(Integer)getDefaultValueForParameter("Relative-offset");
           orientation=(String)getDefaultValueForParameter("Orientation");
        }        
        StringBuilder builder=new StringBuilder();
        SequenceCollection sequenceCollection=null;
        ArrayList<String> allMotifs=new ArrayList<String>();
        if (dataobject instanceof RegionDataset) {
            if (task instanceof OperationTask) {
                String subsetName=(String)((OperationTask)task).getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
                if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
                Data seqcol=engine.getDataItem(subsetName);
                if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
                if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
                sequenceCollection=(SequenceCollection)seqcol;
            }
            if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection();
            HashSet<String> present=getPresentMotifs((RegionDataset)dataobject, sequenceCollection);
            allMotifs.addAll(present);
        } else if (dataobject instanceof RegionSequenceData){
            HashSet<String> present=getPresentMotifs((RegionSequenceData)dataobject);
            allMotifs.addAll(present);
            
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        builder.append("<?xml version=\"1.0\"?>\n");
        builder.append("<cis-element-search");
        builder.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");        
        builder.append(" xsi:schemaLocation=\"http://zlab.bu.edu/schema/cisml cisml.xsd\"");
        builder.append(" xmlns=\"http://zlab.bu.edu/schema/cisml\"");
        builder.append(">\n");
        builder.append("\t<program-name>");
        builder.append(dataobject.getName()); // name of feature?
        builder.append("</program-name>\n");
        builder.append("\t<parameters>\n");
        builder.append("\t</parameters>\n");        
        Collections.sort(allMotifs);
        int totalmotifs=allMotifs.size();
        int i=0;
        for (String motifID:allMotifs) {
                 if (dataobject instanceof RegionDataset) outputMotifsInDataset(motifID, (RegionDataset)dataobject, sequenceCollection, relativeoffset, orientation, task, builder, i, totalmotifs);
            else if (dataobject instanceof RegionSequenceData) outputMotifsInSequence(motifID, (RegionSequenceData)dataobject, relativeoffset, orientation, task, builder, i, totalmotifs);
            i++;
        }   
        builder.append("</cis-element-search>\n");
        outputobject.append(builder.toString(),getName());
        setProgress(100);
        return outputobject;
    }    
    
    /** Returns the Motif IDs (types) of the motifs (or regions) that are present in the given dataset
     *  in the sequences in the collection
     */
    private HashSet<String> getPresentMotifs(RegionDataset dataset, SequenceCollection collection) {        
        HashSet<String> result=new HashSet<String>();
        for (String sequencename:collection.getAllSequenceNames()) {
            RegionSequenceData sequence=(RegionSequenceData)dataset.getSequenceByName(sequencename);
            result.addAll(getPresentMotifs(sequence));
        }
        return result;
    }
    /** Returns the Motif IDs (types) of the motifs (or regions) that are present in the given sequence */
    private HashSet<String> getPresentMotifs(RegionSequenceData sequence) {
        HashSet<String> result=new HashSet<String>();
        for (Region r:sequence.getAllRegions()) {
            result.add(r.getType());            
        }
        return result;
    }    
    
   
    
    /** outputformats hits for a single motif (pattern) in a multiple sequences */
    private void outputMotifsInDataset(String motifID, RegionDataset dataset, SequenceCollection collection, int relativeoffset, String orientationString, ExecutableTask task, StringBuilder outputString, int motifnumber, int totalmotifs) throws InterruptedException {
        String motifname=motifID;
        Data data=engine.getDataItem(motifID);
        if (data instanceof Motif) motifname=((Motif)data).getShortName();
        outputString.append("\t<pattern accession=\"");
        outputString.append(motifID);
        outputString.append("\" name=\"");
        outputString.append(motifname);
        outputString.append("\">\n");

        int size=collection.getNumberofSequences();
        int i=size*motifnumber;
        int progressSize=size*totalmotifs;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              RegionSequenceData sourceSequence=(RegionSequenceData)dataset.getSequenceByName(sequenceName);
              outputMotif(motifID, sourceSequence, relativeoffset, orientationString, outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%50==0) Thread.yield();
              setProgress(i+1,progressSize);
              i++;
        }  
        outputString.append("\t</pattern>\n");
    }   
    
    /** outputformats hits for a single motif (pattern) in a single sequence */
    private void outputMotifsInSequence(String motifID, RegionSequenceData sourceSequence, int relativeoffset, String orientationString, ExecutableTask task, StringBuilder outputString, int motifnumber, int totalmotifs) throws InterruptedException {
        String motifname=motifID;
        Data data=engine.getDataItem(motifID);
        if (data instanceof Motif) motifname=((Motif)data).getShortName();
        outputString.append("\t<pattern accession=\"");
        outputString.append(motifID);
        outputString.append("\" name=\"");
        outputString.append(motifname);
        outputString.append("\">\n");
        outputMotif(motifID, sourceSequence, relativeoffset, orientationString, outputString);
        if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
        if (motifnumber%20==0) Thread.yield();
        setProgress(motifnumber,totalmotifs);
        outputString.append("\t</pattern>\n");
    }      
    
    
   /** outputformats hits for a single motif (pattern) in a single sequence */
    private void outputMotif(String motifID, RegionSequenceData sequence, int relativeoffset, String orientationString, StringBuilder outputString) throws InterruptedException {
         for (Region region:sequence.getAllRegions(motifID)) { //    
              outputString.append("\t\t<scanned-sequence accession=\"");
              outputString.append(sequence.getName());
              outputString.append("\" name=\"");
              outputString.append(sequence.getRegionAsString());
              outputString.append("\" length=\"");
              outputString.append(sequence.getSize());
              outputString.append("\">\n");
              String clusterID=null;
              outputRegion(region, relativeoffset, orientationString, clusterID, outputString);
              outputString.append("\t\t</scanned-sequence>\n");
        }              
    }
    
    /**
     * 
     * @param region
     * @param relativeoffset
     * @param orientationString A string specifying the chosen orientation (E.g. Direct, Reverse, Relative or Opposite)
     * @param clusterID
     * @param outputString 
     */
    public void outputRegion(Region region, int relativeoffset, String orientationString, String clusterID, StringBuilder outputString) {
        RegionSequenceData sequence=region.getParent();
        int shownStrand=Sequence.DIRECT; // this will resolve to the orientation that the 'sequence' is output in
            if (orientationString.equals(DIRECT)) shownStrand=Sequence.DIRECT;
        else if (orientationString.equals(REVERSE)) shownStrand=Sequence.REVERSE;
        else if (orientationString.equals(RELATIVE_ORIENTATION) || orientationString.equals("From Sequence") || orientationString.equals("From Gene")) {
           shownStrand=sequence.getStrandOrientation();
        } else if (orientationString.equals(OPPOSITE)) {
           shownStrand=sequence.getStrandOrientation();
           if (shownStrand==Sequence.DIRECT) shownStrand=Sequence.REVERSE;
           else shownStrand=Sequence.DIRECT;
        }      
        int start=-1;
        int end=-1;
        int strand=Region.DIRECT; // this will be the strand of the region relative to shown sequence orientation
        if (shownStrand==Sequence.DIRECT) {
           start=region.getRelativeStart()+relativeoffset;
           end=region.getRelativeEnd()+relativeoffset;                        
        } else { // relative offset and reverse orientation
           start=(sequence.getSize()-1)-region.getRelativeEnd()+relativeoffset;
           end=(sequence.getSize()-1)-region.getRelativeStart()+relativeoffset;                  
        }               
             if (region.getOrientation()==Region.INDETERMINED) strand=Region.DIRECT;
        else if (region.getOrientation()==shownStrand) strand=Region.DIRECT;
        else strand=Region.REVERSE; // region on opposite strand compared to sequence orientation
        if (strand==Region.REVERSE) { // swap coordinates for reverse orientation
            int temp=start;
            start=end;
            end=temp;
        }          
        outputString.append("\t\t\t<matched-element start=\"");
        outputString.append(start);
        outputString.append("\" end=\"");
        outputString.append(end);
        outputString.append("\" score=\"");
        outputString.append(region.getScore());        
        outputString.append("\"");
        if (clusterID!=null) {
            outputString.append(" clusterid=\"");
            outputString.append(clusterID);        
            outputString.append("\"");            
        }
        outputString.append(">\n");   
        String sequencestring=region.getSequence();
        if (sequencestring!=null) {
            outputString.append("\t\t\t\t<sequence>");
            if (strand==Region.DIRECT) outputString.append(sequencestring);
            else outputString.append(MotifLabEngine.reverseSequence(sequencestring));      
            outputString.append("</sequence>\n");
        }
        outputString.append("\t\t\t</matched-element>\n");
    }
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
         return (Data)parseInputToTarget(input, target, settings, task);         
    }
    
    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        return (DataSegment)parseInputToTarget(input, target, settings, task);
    }
    
    /** The following method is a common substitute for the above 2 methods */
    private Object parseInputToTarget(ArrayList<String> input, Object target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_XMS.parseInput(ArrayList<String> input, Data target)");
        StringBuilder builder=new StringBuilder();
        if (input==null) input=new ArrayList<String>();
        for (String s:input) builder.append(s);
        byte[] bytes=null;
        try {
            bytes=builder.toString().getBytes("UTF-8");
        } catch (Exception e) {throw new ParseError("Unsupported encoding:"+e.getMessage());}
        InputStream stream = new ByteArrayInputStream(bytes);

        String orientation="";
        int relativeoffset=0;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();              
             relativeoffset=(Integer)settings.getResolvedParameter("Relative-offset",defaults,engine);
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
          } 
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           relativeoffset=(Integer)getDefaultValueForParameter("Relative-offset");
           orientation=(String)getDefaultValueForParameter("Orientation");
        } 
        if (target==null) target=new RegionDataset("temporary");
        if (target instanceof RegionDataset) { // The RegionDataset might not contain RegionSequenceData objects for all Sequences. Add them if they are missing!
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));     
            }            
        }
        if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                       
        parseXML(stream, target, relativeoffset, orientation, task);        
        return target;      
    }
    
    
   private void addRegionToTarget(Object target, HashMap<String,Object> map, int relativeoffset, String orientationString) throws ParseError {                
        int start=0, end=0; // these are offset relative to the start of the parent sequence
        int targetOrientation=Sequence.DIRECT;
        int targetStart=0;
        int targetSize=0;
        RegionSequenceData parentSequence=null;
        if (target instanceof RegionSequenceData) {
            parentSequence=((RegionSequenceData)target);                    
            targetOrientation=parentSequence.getStrandOrientation();
            targetStart=parentSequence.getRegionStart();
            targetSize=parentSequence.getSize();
        } if (target instanceof RegionDataset) {
            String sequenceName=(String)map.get("SEQUENCENAME");
            parentSequence=(RegionSequenceData)((RegionDataset)target).getSequenceByName(sequenceName);
            targetOrientation=parentSequence.getStrandOrientation();
            targetStart=parentSequence.getRegionStart();
            targetSize=parentSequence.getSize();
        } else if (target instanceof DataSegment) {
            targetStart=((DataSegment)target).getSegmentStart();
            targetSize=((DataSegment)target).getSize();
        } else throw new ParseError("Target object neither RegionSequenceData nor DataSegment in DataFormat_GFF.addRegionToTarget():"+target.toString());
        Object startValue=map.get("START");
        if (startValue instanceof Integer) start=(Integer)startValue;
        Object endValue=map.get("END");
        if (endValue instanceof Integer) end=(Integer)endValue;
        double score=0;
        Object scoreValue=map.get("SCORE");
        if (scoreValue instanceof Double) score=(Double)scoreValue;
        String type=(String)map.get("TYPE");
        if (type==null) type="unknown_type";
        String annotatedOrientation=(String)map.get("STRAND"); // the orientation of the site in the CisML file
        int annotatedStrand=Sequence.DIRECT; // the strand corresponding to + in the input
             if (orientationString.equals(DIRECT)) annotatedStrand=Sequence.DIRECT;
        else if (orientationString.equals(REVERSE)) annotatedStrand=Sequence.REVERSE;
        else if (orientationString.equals(RELATIVE_ORIENTATION) || orientationString.equals("From Gene")) {
           annotatedStrand=targetOrientation;
        } else if (orientationString.equals(OPPOSITE)) {
           annotatedStrand=targetOrientation;
           if (annotatedStrand==Sequence.DIRECT) annotatedStrand=Sequence.REVERSE;
           else annotatedStrand=Sequence.DIRECT;
        }
        int orientation=Region.DIRECT; // orientation of region (relative to genomic orientation)    
        if (annotatedOrientation==null) annotatedOrientation="+";
        // relative offsets. And orientation could be DIRECT,REVERSE,FROM GENE or OPPOSITE
        if (annotatedStrand==Sequence.DIRECT) {
           start-=relativeoffset;
           end-=relativeoffset;
                if (annotatedOrientation.equals(".")) orientation=Region.INDETERMINED;
           else if (annotatedOrientation.equals("+")) orientation=Region.DIRECT;
           else orientation=Region.REVERSE;
        } else { // relative offset and reverse orientation
           int annotatedStart=start;
           int annotatedEnd=end;
           start=(targetSize-1)-annotatedEnd+relativeoffset;
           end=(targetSize-1)-annotatedStart+relativeoffset;
                if (annotatedOrientation.equals(".")) orientation=Region.INDETERMINED;
           else if (annotatedOrientation.equals("+")) orientation=Region.REVERSE;
           else orientation=Region.DIRECT;
        }      
        String sequenceString=(String)map.get("SEQUENCE");
        if (sequenceString!=null && orientation!=annotatedStrand) {
            map.put("SEQUENCE", MotifLabEngine.reverseSequence(sequenceString));
        }
        if (end<0 || start>=targetSize) return; // region is outside sequence                       
        Region newRegion=new Region(parentSequence, start, end, type, score, orientation);
        for (String property:map.keySet()) {
            if (property.equalsIgnoreCase("SEQUENCENAME") || 
                property.equalsIgnoreCase("FEATURE") || 
                property.equalsIgnoreCase("SOURCE") || 
                property.equalsIgnoreCase("START") || 
                property.equalsIgnoreCase("END") || 
                property.equalsIgnoreCase("SCORE") || 
                property.equalsIgnoreCase("TYPE") ||                         
                property.equalsIgnoreCase("STRAND")) continue;
            else {
                newRegion.setProperty(property, map.get(property));
            }                    
        }
        //System.err.println("Add to ["+target.toString()+"]  offset="+offsetString+"  relative="+relativeoffset+"  orientionString="+orientationString+":  region="+newRegion.toString());
        if (target instanceof RegionSequenceData) ((RegionSequenceData)target).addRegion(newRegion);   
        else if (target instanceof RegionDataset) parentSequence.addRegion(newRegion);   
        else if (target instanceof DataSegment) ((DataSegment)target).addRegion(newRegion);
    }
       
    

    private void parseXML(InputStream inputstream, Object target, int relativeoffset, String orientationString, ExecutableTask task) throws ParseError, InterruptedException {
        DocumentBuilder builder;
         try {
            String currentMotif=null;
            String currentSequenceName=null;            
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(inputstream);
            NodeList topnodes = doc.getElementsByTagName("cis-element-search");
            if (topnodes.getLength()!=1) throw new ParseError("Expected 1 <cis-element-search> in CisML file. Found "+topnodes.getLength());
            Element cisElementSearchNode = (Element)topnodes.item(0);
            NodeList patternNodes = cisElementSearchNode.getElementsByTagName("pattern");
            for (int i=0;i<patternNodes.getLength();i++) {
                 if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                 if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                 task.setProgress(i, patternNodes.getLength());
                 Element patternNode = (Element)patternNodes.item(i);
                 currentMotif=patternNode.getAttribute("accession");
                 NodeList sequenceNodes = patternNode.getElementsByTagName("scanned-sequence");
                 for (int j=0; j<sequenceNodes.getLength();j++) {
                     Element sequenceNode = (Element)sequenceNodes.item(j);
                     currentSequenceName=sequenceNode.getAttribute("accession");
                     currentSequenceName=convertIllegalSequenceNamesIfNecessary(currentSequenceName, false);
                     String error=engine.checkSequenceNameValidity(currentSequenceName, false);
                     if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+currentSequenceName+"' : "+error);                                            
                     NodeList matchNodes = sequenceNode.getElementsByTagName("matched-element");
                     for (int k=0;k<matchNodes.getLength();k++) {
                         String sequenceString=null;
                         Element matchNode = (Element)matchNodes.item(k);
                         NodeList sequenceStringNodes = matchNode.getElementsByTagName("sequence");
                         if (sequenceStringNodes.getLength()>0) {
                             sequenceString=((Element)sequenceStringNodes.item(0)).getTextContent();                           
                         }                         
                         String startString=matchNode.getAttribute("start");
                         String endString=matchNode.getAttribute("end");
                         String scoreString=matchNode.getAttribute("score");
                         int start=0;
                         int end=0;
                         double score=0;
                         try {
                             start=Integer.parseInt(startString);
                         } catch(NumberFormatException nfe) {throw new ParseError("Unable to parse expected integer value for 'start': "+startString);}
                         try {
                             end=Integer.parseInt(endString);
                         } catch(NumberFormatException nfe) {throw new ParseError("Unable to parse expected integer value for 'end': "+endString);}
                         try {
                             score=Double.parseDouble(scoreString);
                         } catch(NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value for 'score': "+scoreString);}
                         String orientation="+";
                         if (end<start) {
                             int temp=start;
                             start=end;
                             end=temp;
                             orientation="-";
                         }
                         HashMap<String,Object> map=new HashMap<String,Object>();
                         map.put("TYPE",currentMotif);
                         map.put("SEQUENCENAME",currentSequenceName);
                         map.put("START",start);
                         map.put("END",end);
                         map.put("SCORE",score);
                         map.put("STRAND",orientation);
                         map.put("SEQUENCE",sequenceString);
                         addRegionToTarget(target, map, relativeoffset, orientationString);
                     } // end for each TFBS match
                 } // end for each sequence
            } // end for each pattern (motif)
         }
         catch (ParseError e) {throw e;}
         catch (ParserConfigurationException e) {throw new ParseError("Unable to instantiate XML Document Builder");}
         catch (SAXException e) {throw new ParseError("SAXException: "+e.getMessage());}
         catch (IOException e) {throw new ParseError("I/O-error:"+e.getMessage());}
         catch (Exception e) {throw new ParseError(e.getClass().getSimpleName()+":"+e.getMessage());}
    }
}

        
       
        
        
