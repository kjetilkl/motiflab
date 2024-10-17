/*
 
 
 */

package motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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

import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.Parameter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.ModuleCRM;
import motiflab.engine.data.Motif;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericMap;
import motiflab.engine.data.NumericSequenceData;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.TextVariable;

/**
 *
 * @author kjetikl
 */
public class DataFormat_EvidenceGFF extends DataFormat {
    private String name="EvidenceGFF";
    private static final int RELATIVE=0;
    private static final int GENOMIC=1;
    private static final int RELATIVE_TSS=2;    
    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE_ORIENTATION="Relative";
    private static final String OPPOSITE="Opposite";
    
    private static final String FORMAT_TABBED="Tabbed fields";
    private static final String FORMAT_KEY_VALUE_LIST="key=value;";
    
    private static final String BOOLEAN_YES_NO="Yes/No";
    private static final String BOOLEAN_TRUE_FALSE="True/False";
    private static final String BOOLEAN_YES_NO_ABBR="Y/N";
    private static final String BOOLEAN_TRUE_FALSE_ABBR="T/F";
    private static final String BOOLEAN_0_1="0/1";

    private static final int BOOLEAN_FORMAT_YES_NO=0;
    private static final int BOOLEAN_FORMAT_YES_NO_ABBR=1;
    private static final int BOOLEAN_FORMAT_TRUE_FALSE=2;
    private static final int BOOLEAN_FORMAT_TRUE_FALSE_ABBR=3;
    private static final int BOOLEAN_FORMAT_0_1=4;
    
    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};
    
    
    public DataFormat_EvidenceGFF() {
        addParameter("Position", "Genomic", new String[]{"Genomic","Relative","TSS-Relative"},"Specifies whether the coordinate positions [start-end] for a region should be given relative to the start of the chromosome ('Genomic'), relative to the upstream start of the sequence ('Relative') or relative to the transcription start site associated with the sequence ('TSS-Relative')");
        addParameter("Relative-offset", new Integer(1), null,"If the 'Position' setting is set to 'Relative', this offset-value specifies what position the first base in the sequence should start at (common choices are 0 or 1). If the 'Position' setting is 'TSS-Relative', a value of 0 here will place the TSS at position '+0' whereas any other value will place the TSS at position '+1' (and the coordinate-system will then skip 0 and go directly from -1 to +1)");
        addParameter("Orientation", RELATIVE_ORIENTATION,new String[]{DIRECT,REVERSE,RELATIVE_ORIENTATION,OPPOSITE},"Orientation of relative coordinates (only applicable if the 'Position' setting is 'Relative')");  
        addOptionalParameter("Evidence", "", null,"The 'evidence' parameter should be a comma-separated list of key-value pairs specifying additional information that should be output for each region. See the MotifLab user manual for a complete list of recognized evidence codes.");   
        addOptionalParameter("Evidence format", FORMAT_TABBED, new String[]{FORMAT_TABBED,FORMAT_KEY_VALUE_LIST},"Specifies how the 'evidence' should be output for each region. Options are to output each evidence value in a column of its own or to output all evidences in a single column in key=value pairs (separated by semi-colons)"); 
        addOptionalParameter("Boolean format", BOOLEAN_YES_NO, new String[]{BOOLEAN_YES_NO,BOOLEAN_YES_NO_ABBR,BOOLEAN_TRUE_FALSE,BOOLEAN_TRUE_FALSE_ABBR,BOOLEAN_0_1},"Specifies how boolean values should be formatted in the output.");
        addOptionalParameter("Include header", Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Output a single header line (starting with #) at the beginning of the file. The header contains a specification of all the fields included in the file."); 
        addOptionalParameter("Skip standard fields", Boolean.FALSE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"If selected, the standard GFF fields will not be output, only the evidence fields."); 
        addOptionalParameter("Sort1", "Position", new String[]{"Position","Type","Score"},null);
        addOptionalParameter("Sort2", "Type", new String[]{"Position","Type","Score"},null);
        addOptionalParameter("Sort3", "Score", new String[]{"Position","Type","Score"},null);  
           
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
        return false;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return false;
    }
    
    
    
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "gff";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        String sort1,sort2,sort3,offsetString,evidenceString,orientation,format, booleanformatString;
        boolean header=false;
        boolean skipStandard=false;
        int relativeoffset=1;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             sort1=(String)settings.getResolvedParameter("Sort1",defaults,engine); 
             sort2=(String)settings.getResolvedParameter("Sort2",defaults,engine);
             sort3=(String)settings.getResolvedParameter("Sort3",defaults,engine);
             offsetString=(String)settings.getResolvedParameter("Position",defaults,engine);
             relativeoffset=(Integer)settings.getResolvedParameter("Relative-offset",defaults,engine);
             evidenceString=(String)settings.getResolvedParameter("Evidence",defaults,engine);
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
             format=(String)settings.getResolvedParameter("Evidence format",defaults,engine);
             booleanformatString=(String)settings.getResolvedParameter("Boolean format",defaults,engine);
             header=(Boolean)settings.getResolvedParameter("Include header",defaults,engine);
             skipStandard=(Boolean)settings.getResolvedParameter("Skip standard fields",defaults,engine);             
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           sort1=(String)getDefaultValueForParameter("Sort1");
           sort2=(String)getDefaultValueForParameter("Sort2");
           sort3=(String)getDefaultValueForParameter("Sort3");
           offsetString=(String)getDefaultValueForParameter("Position");
           relativeoffset=(Integer)getDefaultValueForParameter("Relative-offset");
           evidenceString=(String)getDefaultValueForParameter("Evidence");
           orientation=(String)getDefaultValueForParameter("Orientation");
           format=(String)getDefaultValueForParameter("Evidence format");  
           booleanformatString=(String)getDefaultValueForParameter("Boolean format");  
           header=(Boolean)getDefaultValueForParameter("Include header");  
           skipStandard=(Boolean)getDefaultValueForParameter("Skip standard fields");            
        }
        int booleanformat=BOOLEAN_FORMAT_YES_NO;
             if (booleanformatString.equals(BOOLEAN_TRUE_FALSE)) booleanformat=BOOLEAN_FORMAT_TRUE_FALSE;
        else if (booleanformatString.equals(BOOLEAN_TRUE_FALSE_ABBR)) booleanformat=BOOLEAN_FORMAT_TRUE_FALSE_ABBR;
        else if (booleanformatString.equals(BOOLEAN_YES_NO)) booleanformat=BOOLEAN_FORMAT_YES_NO;
        else if (booleanformatString.equals(BOOLEAN_YES_NO_ABBR)) booleanformat=BOOLEAN_FORMAT_YES_NO_ABBR;
        else if (booleanformatString.equals(BOOLEAN_0_1)) booleanformat=BOOLEAN_FORMAT_0_1;
        if (evidenceString!=null && evidenceString.trim().isEmpty()) evidenceString=null;
        int offset=GENOMIC;
        if (offsetString!=null && offsetString.equalsIgnoreCase("Relative")) offset=RELATIVE;
        else if (offsetString!=null && offsetString.equalsIgnoreCase("TSS-Relative")) offset=RELATIVE_TSS;
        String outputString="";
        Object[][] evidence=null;
        
        // parse and validate the evidenceString
        if (evidenceString!=null && !evidenceString.isEmpty()) {
            String[] splitEvidence=evidenceString.trim().split("\\s*(;|,)\\s*");
            evidence=new Object[splitEvidence.length][]; // A list of entries where the first field [x][0] if trackname (or property) and the other [x][1] is track format            
            int index=0;
            for (String entry:splitEvidence) {             
               String[] entrySplit=entry.split("\\s*(=|:)\\s*");
               if (entrySplit.length!=2) throw new ExecutionError("Evidence entry not in expected format (track=format): "+entry);
               String trackName=entrySplit[0];
               String trackFormatString=entrySplit[1];
               if (trackName.equalsIgnoreCase("region") || trackName.equalsIgnoreCase("motif") || trackName.equalsIgnoreCase("module") || trackName.equalsIgnoreCase("sequence")) {
                   evidence[index]=new Object[]{trackName,trackFormatString};  
               } else if (trackName.equalsIgnoreCase("text")) {
                   evidence[index]=new Object[]{trackName,entrySplit[1]};  
               } else { // target is a data track
                   Data track=engine.getDataItem(trackName);
                   if (track instanceof RegionDataset) {
                     String operator=null;  // is|count|list|percentage|percent    or 'distance to'
                     String condition=null; // inside|covering|overlapping|within   or any|<Collection>|<TextVariable>|
                     if (trackFormatString.startsWith("distance to")) { // handle this as a special case?
                         operator="distance to";
                         condition=entrySplit[1].substring(operator.length());
                         condition=condition.trim(); // this should be "any", "closest" or "interaction partner" or the name of a Collection or TextVariable
                         Boolean allowOverlap=true;
                         if (condition.startsWith("non-overlapping")) {
                             allowOverlap=false;                             
                             condition=condition.substring("non-overlapping".length());
                             condition=condition.trim(); //                                                      
                         } else if (condition.endsWith("non-overlapping")) {
                             allowOverlap=false;                             
                             condition=condition.substring(0,(condition.length()-"non-overlapping".length()));
                             condition=condition.trim(); //                                                      
                         }
                         if (condition.isEmpty()) throw new ExecutionError("Missing specification for 'distance to'. Recognized values are 'any' (or 'closest'), 'interaction partner' (or 'interacting') or the name of a Collection or TextVariable");
                         evidence[index]=new Object[]{trackName,condition,operator,allowOverlap};
                     } else {
                         String extra=null;
                         String rangeString=null;
                         Object rangeObject=null; // could be an Integer or a Numeric Map or Numeric Variable
                         String withAdditional=null;  
                         if (trackFormatString.contains(" with ")) {
                             int pos=trackFormatString.lastIndexOf(" with ");
                             withAdditional=trackFormatString.substring(pos+5).trim();
                             trackFormatString=trackFormatString.substring(0,pos).trim();
                         }
                         Pattern pattern=Pattern.compile("^(is|count|list|percentage|percent)(\\s+.+?)?\\s+(present|inside|covering|overlapping|within(\\s+\\S+)?)");
                         Matcher matcher=pattern.matcher(trackFormatString);
                         if (matcher.find()) {  
                           operator=matcher.group(1);
                           extra=matcher.group(2);
                           condition=matcher.group(3);
                           if (condition.indexOf(' ')>=0) condition=condition.substring(0,condition.indexOf(' '));
                           rangeString=matcher.group(4);                           
                           if (rangeString!=null) rangeString=rangeString.trim();
                         } else throw new ExecutionError("Unrecognized evidence field format for region track: "+trackFormatString);                    
                         boolean interacting=(extra!=null && (extra.contains("interacting") || extra.contains("interaction partner")));
                         boolean removeoverlapping=(extra!=null && extra.contains("non-overlapping"));
                         boolean percentageAlloverlapping=(extra!=null && extra.trim().equals("all"));
                         if (condition.equals("within")) {
                             if (rangeString==null || rangeString.isEmpty()) throw new ExecutionError("Missing range for 'within distance' format for region track: "+trackName); 
                             Data data = engine.getDataItem(rangeString);                 
                             if (data==null) {
                                 try {
                                   int value=Integer.parseInt(rangeString);
                                   rangeObject=new Integer(value);
                                 } catch (NumberFormatException e) {throw new ExecutionError("Not a valid number or numeric data object: "+rangeString);}
                             } else if (data instanceof NumericMap || data instanceof NumericVariable) {
                               rangeObject=data;
                             } else throw new ExecutionError("Not a valid number or numeric data object: "+rangeString);                        
                         }
                         evidence[index]=new Object[]{trackName,condition,operator,rangeObject,removeoverlapping,interacting,withAdditional,percentageAlloverlapping};                                              
                     }
                   } else if (track instanceof NumericDataset || track instanceof DNASequenceDataset) {
                       evidence[index]=new Object[]{trackName,trackFormatString};     
                   } else throw new ExecutionError("Evidence field '"+trackName+"' does not refer to a known data track");                               
               }
               index++;
            }
        }
        // output header
        if (header) {
          outputobject.append("#",getName());
          if (!skipStandard) outputobject.append("Sequence\tFeature\tType\tStart\tEnd\tScore\tStrand\tFrame",getName());
          if (evidenceString!=null) {             
              if (format.equals(FORMAT_KEY_VALUE_LIST)) {
                  if (skipStandard) outputobject.append("Evidence",getName()); // skip leading TAB if standard fields were omitted
                  else outputobject.append("\tEvidence",getName());
              } else if (format.equals(FORMAT_TABBED)) {
                  String[] splitEvidence=evidenceString.trim().split("\\s*(;|,)\\s*");
                  int index=0;
                  for (String entry:splitEvidence) {
                      String[] entrySplit=entry.split("\\s*(=|:)\\s*");
                      if (index>0 || !skipStandard) outputobject.append("\t",getName());
                      if (entrySplit[0].equals("text")) {
                          outputobject.append("\"",getName());
                          outputobject.append(entrySplit[1],getName());
                          outputobject.append("\"",getName());
                      } else {
                          outputobject.append(entrySplit[0],getName());
                          outputobject.append(" (",getName());
                          outputobject.append(entrySplit[1],getName());
                          outputobject.append(")",getName());
                      }
                      index++;
                  }
              }
          }
          outputobject.append("\n",getName()); 
        }
        //
        RegionComparator regionComparator=new RegionComparator(sort1, sort2, sort3);
        if (dataobject instanceof RegionDataset) {
            SequenceCollection sequenceCollection=null;
            if (task instanceof OperationTask) {
                String subsetName=(String)((OperationTask)task).getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
                if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
                Data seqcol=engine.getDataItem(subsetName);
                if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
                if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
                sequenceCollection=(SequenceCollection)seqcol;
            }
            if (sequenceCollection==null) sequenceCollection=engine.getDefaultSequenceCollection();                   
            outputString=outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, regionComparator, offset, relativeoffset, orientation, evidence, format, booleanformat, task, engine, skipStandard);
        } else if (dataobject instanceof RegionSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((RegionSequenceData)dataobject, regionComparator, offset, relativeoffset, orientation, evidence, format, booleanformat, task, builder, skipStandard);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }    

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, RegionComparator regionComparator, int offset,int relativeoffset, String orientationString, Object[][] evidence, String format, int booleanformat, ExecutableTask task, MotifLabEngine engine, boolean skipStandard) throws ExecutionError,InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence, regionComparator, offset, relativeoffset, orientationString, evidence, format, booleanformat, task, outputString, skipStandard);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }    
    
    
    /** output-formats a single sequence */
    private void outputSequence(RegionSequenceData sequence, RegionComparator regionComparator, int offset, int relativeoffset, String orientationString, Object[][] evidence, String format, int booleanformat, ExecutableTask task, StringBuilder outputString, boolean skipStandard) throws ExecutionError,InterruptedException {
            ArrayList<Region> regionList=sequence.getAllRegions();
            int shownStrand=Sequence.DIRECT;
                if (orientationString.equals(DIRECT)) shownStrand=Sequence.DIRECT;
            else if (orientationString.equals(REVERSE)) shownStrand=Sequence.REVERSE;
            else if (orientationString.equals(RELATIVE_ORIENTATION) || orientationString.equals("From Sequence") || orientationString.equals("From Gene")) {
               shownStrand=sequence.getStrandOrientation();
            } else if (orientationString.equals(OPPOSITE)) {
               shownStrand=sequence.getStrandOrientation();
               if (shownStrand==Sequence.DIRECT) shownStrand=Sequence.REVERSE;
               else shownStrand=Sequence.DIRECT;
            }
            regionComparator.setOrientation(shownStrand);
            Collections.sort(regionList, regionComparator);
            String sequenceName=sequence.getName();
            String featureName=sequence.getParent().getName();
            String source=sequence.getDataSource();
            int count=0;
            for (Region region:regionList) {
               count++;
               if (count%200==0) {
                  if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                  if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
                  Thread.yield();
               }
               int start=-1;
               int end=-1;
               String strand=".";
               if (offset==GENOMIC) {
                   start=region.getGenomicStart();
                   end=region.getGenomicEnd();
                        if (region.getOrientation()==Region.INDETERMINED) strand=".";
                   else if (region.getOrientation()==Region.DIRECT) strand="+";
                   else if (region.getOrientation()==Region.REVERSE) strand="-";
               } else if (offset==RELATIVE_TSS && sequence.getTSS()!=null) { // TSS-relative offset
                   Integer TSS=sequence.getTSS();
                   int seqOrientation=sequence.getStrandOrientation();
                   start=region.getGenomicStart();
                   end=region.getGenomicEnd();           

                   start=(seqOrientation==Sequence.DIRECT)?start-TSS:TSS-start;
                   end=(seqOrientation==Sequence.DIRECT)?end-TSS:TSS-end;
                   if (seqOrientation==Sequence.REVERSE) {int swap=start; start=end; end=swap;}
                   if (relativeoffset>0) { // this is used as a signal to "skip 0"
                      if (start>=0) start++; // because there is no "0" position
                      if (end>=0) end++; // because there is no "0" position
                   }           
                        if (region.getOrientation()==Region.INDETERMINED) strand=".";
                   else if (region.getOrientation()==seqOrientation) strand="+";
                   else strand="-";
               } else { // position relative to start of sequence
                   if (shownStrand==Sequence.DIRECT) {
                       start=region.getRelativeStart()+relativeoffset;
                       end=region.getRelativeEnd()+relativeoffset;
                   } else { // relative offset and reverse orientation
                       start=(sequence.getSize()-1)-region.getRelativeEnd()+relativeoffset;
                       end=(sequence.getSize()-1)-region.getRelativeStart()+relativeoffset;
                   }           
                        if (region.getOrientation()==Region.INDETERMINED) strand=".";
                   else if (region.getOrientation()==shownStrand) strand="+";
                   else strand="-";                     
               }

//               String attributes="";
//               String[] properties=region.getAllPropertyNames();
//               for (int i=0;i<properties.length;i++) {
//                   Object val=region.getProperty(properties[i]);
//                   if (val==null) attributes+=properties[i]+"=null;";
//                   else attributes+=properties[i]+"=\""+val.toString()+"\";";
//               }
               // output line
               String type=region.getType();
               if (type==null) type="UNKNOWN";  
               if (!skipStandard) {
                   outputString.append(sequenceName);
                   outputString.append("\t");
                   outputString.append(featureName);
                   outputString.append("\t");
                   outputString.append(type);
                   outputString.append("\t");
                   outputString.append(start);
                   outputString.append("\t");
                   outputString.append(end);
                   outputString.append("\t");
                   outputString.append(region.getScore());
                   outputString.append("\t");
                   outputString.append(strand);
                   outputString.append("\t");
                   outputString.append(".");
               }
               boolean tabbed=format.equals(FORMAT_TABBED);
               if (evidence!=null) {
                   int index=0;
                   if (!tabbed && !skipStandard) outputString.append("\t");
                   for (Object[] entry:evidence) {                    
                      String value=getValueForEvidence(region, sequenceName, entry, booleanformat);
                      if (tabbed) {
                          if (index>0 || !skipStandard) outputString.append("\t");                         
                          outputString.append(value);                        
                      } else { // key=value format
                          String key=getKeyForEvidence(entry);                    
                          outputString.append(key);
                          outputString.append("=");
                          outputString.append(value);
                          outputString.append(";");                           
                      }
                      index++;
                   }
               }                           
               outputString.append("\n");
            }
    }
    
    private String getKeyForEvidence(Object[] evidenceEntry) {
        String evidenceName=(String)evidenceEntry[0];
        if (evidenceName.equals("region") || evidenceName.equals("motif") || evidenceName.equals("sequence")) {
            String property=(String)evidenceEntry[1]; 
            return evidenceName+"."+property;
        } else return evidenceName;
    }
    
    /** Returns the value for an evidence field as a string */
    private String getValueForEvidence(Region region, String sequenceName, Object[] evidenceEntry, int booleanformat) throws ExecutionError {
        String evidenceName=(String)evidenceEntry[0];
        if (evidenceName.equals("region")) {
            String property=(String)evidenceEntry[1]; 
            if (property.equals("orientationsymbol") || property.equals("orientationstring")) {
                return region.getOrientationAsString();
            }
            Object propertyvalue=region.getProperty(property);         
            return getValueAsString(propertyvalue);
        } else if (evidenceName.equals("motif")) {
            String type=region.getType();
            if (type==null || type.isEmpty()) return "";
            Data motif=engine.getDataItem(type);
            if (!(motif instanceof Motif)) return "N/A";           
            String property=(String)evidenceEntry[1];
                 if (property.equals("id")) return type;
            else if (property.equals("short name")) return ((Motif)motif).getShortName();
            else if (property.equals("long name")) return ((Motif)motif).getLongName();
            else if (property.equals("consensus")) return ((Motif)motif).getConsensusMotif();
            else if (property.equals("classification")) return ((Motif)motif).getClassification();
            else if (property.equals("factors")) return ((Motif)motif).getBindingFactors();
            try {
                Object propertyValue=((Motif)motif).getPropertyValue(property, engine);
                return getValueAsString(propertyValue);
            } catch (ExecutionError e) {return "";}
        } else if (evidenceName.equals("module")) {
            String type=region.getType();
            if (type==null || type.isEmpty()) return "";
            Data cisRegModule=engine.getDataItem(type);
            if (!(cisRegModule instanceof ModuleCRM)) return "N/A";           
            String property=(String)evidenceEntry[1];
            try {
                Object propertyValue=((ModuleCRM)cisRegModule).getPropertyValue(property, engine);
                return getValueAsString(propertyValue);
            } catch (ExecutionError e) {return "";}
        } else if (evidenceName.equals("sequence")) {
            Data sequence=engine.getDataItem(sequenceName);
            if (!(sequence instanceof Sequence)) return "N/A";           
            String property=(String)evidenceEntry[1];
                 if (property.equals("name")) return ((Sequence)sequence).getName();
            else if (property.equals("gene name") || property.equals("genename")) return ((Sequence)sequence).getGeneName();
            else if (property.equals("taxonomy")) return ""+((Sequence)sequence).getOrganism();
            else if (property.equals("species") || property.equals("organism")) return ((Sequence)sequence).getSpeciesName();
            else if (property.equals("latin species") || property.equals("latin organism")) return ((Sequence)sequence).getSpeciesLatinName();
            else if (property.equals("build")) return ((Sequence)sequence).getGenomeBuild();
            else if (property.equals("start")) return ""+((Sequence)sequence).getRegionStart();
            else if (property.equals("end")) return ""+((Sequence)sequence).getRegionEnd();
            else if (property.equals("chromosome")) return ""+((Sequence)sequence).getChromosome();
            else if (property.equals("chr")) {
                String chr=((Sequence)sequence).getChromosome();
                if (!chr.startsWith("chr")) chr="chr"+chr;
                return chr;
            }
            else if (property.equals("orientation")) {
                int strand=((Sequence)sequence).getStrandOrientation();
                if (strand==Sequence.DIRECT) return "+";
                else if (strand==Sequence.REVERSE) return "-";
                else return ".";
            } 
            try {
                Object propertyValue=((Sequence)sequence).getPropertyValue(property, engine);
                return getValueAsString(propertyValue);
            } catch (ExecutionError e) {return "";}
        } else if (evidenceName.equals("text")) {         
            String property=(String)evidenceEntry[1];
            return property;
        } else { // track based evidence       
            Data track=engine.getDataItem(evidenceName);
            int regionOrientation=region.getOrientation();        
            if (track instanceof NumericDataset) {
                int regionStart=region.getRelativeStart();
                int regionEnd=region.getRelativeEnd();
                String trackFormat=((String)evidenceEntry[1]).toLowerCase();
                NumericSequenceData numericSequence=(NumericSequenceData)((NumericDataset)track).getSequenceByName(sequenceName);
                int sequenceOrientation=numericSequence.getStrandOrientation();
                     if (trackFormat.equals("average") || trackFormat.equals("avg")) return ""+numericSequence.getAverageValueInInterval(regionStart, regionEnd);
                else if (trackFormat.equals("min") || trackFormat.equals("minimum")) return ""+numericSequence.getMinValueInInterval(regionStart, regionEnd);
                else if (trackFormat.equals("max") || trackFormat.equals("maximum")) return ""+numericSequence.getMaxValueInInterval(regionStart, regionEnd);
                else if (trackFormat.equals("median")) return ""+numericSequence.getMedianValueInInterval(regionStart, regionEnd);
                else if (trackFormat.equals("sum")) return ""+numericSequence.getSumValueInInterval(regionStart, regionEnd);
                else if (trackFormat.equals("startvalue")) return ""+numericSequence.getValueAtRelativePosition(regionStart);
                else if (trackFormat.equals("endvalue")) return ""+numericSequence.getValueAtRelativePosition(regionEnd);
                else if (trackFormat.equals("relativestartvalue")) return ""+numericSequence.getValueAtRelativePosition((sequenceOrientation==Sequence.DIRECT)?regionStart:regionEnd);
                else if (trackFormat.equals("relativeendvalue")) return ""+numericSequence.getValueAtRelativePosition((sequenceOrientation==Sequence.DIRECT)?regionEnd:regionStart);
                else if (trackFormat.equals("regionstartvalue")) return ""+numericSequence.getValueAtRelativePosition((regionOrientation!=Sequence.REVERSE)?regionStart:regionEnd);
                else if (trackFormat.equals("regionendvalue")) return ""+numericSequence.getValueAtRelativePosition((regionOrientation!=Sequence.REVERSE)?regionEnd:regionStart);
                else if (trackFormat.equals("centervalue")) return ""+numericSequence.getValueAtRelativePosition((int)((regionStart+regionEnd)/2.0));
                else if (trackFormat.equals("weighted average") || trackFormat.equals("weighted avg")) return getWeightedValue(trackFormat, region, numericSequence);
                else if (trackFormat.equals("weighted min") || trackFormat.equals("weighted minimum")) return getWeightedValue(trackFormat, region, numericSequence);
                else if (trackFormat.equals("weighted max") || trackFormat.equals("weighted maximum")) return getWeightedValue(trackFormat, region, numericSequence);
                else if (trackFormat.equals("weighted median")) return getWeightedValue(trackFormat, region, numericSequence);
                else if (trackFormat.equals("weighted sum")) return getWeightedValue(trackFormat, region, numericSequence);
                
                else throw new ExecutionError("Unrecognized evidence field format for numeric track: "+trackFormat);                   
                     
            } else if (track instanceof DNASequenceDataset) {
                int regionStart=region.getRelativeStart();
                int regionEnd=region.getRelativeEnd();
                String trackFormat=(String)evidenceEntry[1];
                DNASequenceData dnaSequence=(DNASequenceData)((DNASequenceDataset)track).getSequenceByName(sequenceName);
                     if (trackFormat.equals("direct")) return new String((char[])dnaSequence.getValueInInterval(regionStart, regionEnd));
                else if (trackFormat.equals("reverse")) return new String( MotifLabEngine.reverseSequence( (char[])dnaSequence.getValueInInterval(regionStart, regionEnd)) );
                else if (trackFormat.equals("relative")) {
                    char[] sequence=(char[])dnaSequence.getValueInInterval(regionStart, regionEnd);
                    int sequenceOrientation=dnaSequence.getStrandOrientation();
                    if (sequenceOrientation==Sequence.REVERSE)  return new String( MotifLabEngine.reverseSequence(sequence));
                    else return new String(sequence);                  
                } else throw new ExecutionError("Unrecognized evidence field format for dna track: "+trackFormat);

            } else if (track instanceof RegionDataset) {
                RegionSequenceData regionSequence=(RegionSequenceData)((RegionDataset)track).getSequenceByName(sequenceName); // this is the other track
                String condition=(String)evidenceEntry[1];
                String operator=(String)evidenceEntry[2];
                if (operator.equals("distance to")) { // handle this operator as a special case
                    Boolean allowOverlap=(Boolean)evidenceEntry[3];
                    Object limitation=null;
                    if (condition.equals("interaction partner") || condition.equals("interacting")) limitation=condition;
                    else if (!(condition.equals("any") || condition.equals("closest"))) {
                        Data dataitem=engine.getDataItem(condition);
                        if (dataitem==null) throw new ExecutionError("Unknown data object: "+condition);
                        if (!(dataitem instanceof DataCollection || dataitem instanceof TextVariable)) throw new ExecutionError("'"+condition+"' should be a Collection or TextVariable or 'any|closest|interaction partner'");
                        limitation=dataitem;
                    }
                    Integer distance=getDistanceTo(regionSequence,region,allowOverlap,limitation);
                    if (distance==null) return "N/A";
                    else return distance.toString();
                } // end of special case "distance to".
                int regionStart=region.getGenomicStart(); // this is the target region
                int regionEnd=region.getGenomicEnd();                
                Object rangeObject=evidenceEntry[3];
                boolean removeoverlapping=(Boolean)evidenceEntry[4];
                boolean considerOnlyInteracting=(Boolean)evidenceEntry[5]; 
                String withAdditional=(String)evidenceEntry[6];
                boolean percentageAllOverlapping=(Boolean)evidenceEntry[7];
                boolean includeScores=(withAdditional!=null && (withAdditional.equals("scores") || withAdditional.equals("scores and distances")));
                boolean includeDistances=(withAdditional!=null && (withAdditional.equals("distances") || withAdditional.equals("scores and distances")));
                if (includeScores || includeDistances) withAdditional=null; // already taken care of
                boolean sametrack=(regionSequence==region.getParent());
                Data motif=engine.getDataItem(region.getType());
                boolean isMotif=(regionSequence.isMotifTrack() && motif instanceof Motif);
                ArrayList<Region> list=null;
                     if (condition.equals("present"))     list=regionSequence.getSameRegion(region);
                else if (condition.equals("inside"))      list=regionSequence.getRegionsWithinGenomicInterval(regionStart, regionEnd);
                else if (condition.equals("covering"))    list=regionSequence.getRegionsSpanningGenomicInterval(regionStart, regionEnd);
                else if (condition.equals("overlapping")) list=regionSequence.getRegionsOverlappingGenomicInterval(regionStart, regionEnd);
                else if (condition.equals("within")) {
                    int range=0;
                         if (rangeObject instanceof Integer) range=(Integer)rangeObject;
                    else if (rangeObject instanceof NumericVariable) range=((NumericVariable)rangeObject).getValue().intValue();
                    else if (rangeObject instanceof SequenceNumericMap) range=((SequenceNumericMap)rangeObject).getValue(sequenceName).intValue();
                    else if (rangeObject instanceof NumericMap) { // other Numeric Maps (e.g. Motif or ModuleCRM maps)
                       String type=region.getType();
                       if (type==null) range=((NumericMap)rangeObject).getValue().intValue();
                       else range=((NumericMap)rangeObject).getValue(type).intValue();
                    }  
                    list=regionSequence.getRegionsOverlappingGenomicInterval(regionStart-range, regionEnd+range);
                } else throw new ExecutionError("unknown preposition:"+condition);
                // remove the query region from the list of regions recovered if they are from the same track (so that the query region is not considered)
                if (sametrack) {
                   list.remove(region);                     
                } 
                // remove overlapping regions (if this option is specified)
                if (removeoverlapping) { 
                   Iterator<Region> iter=list.iterator();
                   while (iter.hasNext()) {
                       Region other=iter.next();
                       if (regionsAreOverlapping(region,other)) iter.remove();
                   }                    
                }
                // remove non-interacting motif region (if this option is specified)
                if (considerOnlyInteracting && isMotif) { 
                   Iterator<Region> iter=list.iterator();
                   while (iter.hasNext()) {
                       Region other=iter.next();
                       if (!((Motif)motif).isKnownInteractionPartner(other.getType())) iter.remove();
                   }
                }
                // return evidence value as string
                     if (operator.equals("is")) return getBooleanString(list.size()>0, booleanformat);
                else if (operator.equals("count")) return ""+list.size();
                else if (operator.equals("list")) return getRegionListAsString(region,list,includeScores,includeDistances, withAdditional);  
                else if (operator.equals("percent") || operator.equals("percentage")) {
                    if (!condition.equals("overlapping")) throw new ExecutionError("The '"+operator+"' operator should only be used in the context: '"+operator+" "+condition+"'");
                    return getPercentageOverlap(region,list,percentageAllOverlapping);
                }
                else throw new ExecutionError("Unknown operator for region track: "+operator);
            } else throw new ExecutionError("Unknown track: "+evidenceName);      
        }
    }
    
    /** Returns the largest percentage overlap between the target region and the regions in the list (or a comma-separated list of percentages) */
    private String getPercentageOverlap(Region region, ArrayList<Region> list, boolean includeAllOverlapping) {
        if (list==null || list.isEmpty()) return "0"; // no overlap
        ArrayList<Double> allpercentages=(includeAllOverlapping)?new  ArrayList<Double>(list.size()):null;
        double greatestOverlap=0;
        int start=region.getRelativeStart();
        int end=region.getRelativeEnd();
        double length=end-start+1;
        for (Region other:list) { // Note that all the 'other' regions will be overlapping with the target at this point!
            double current=0;
            int otherStart=other.getRelativeStart();
            int otherEnd=other.getRelativeEnd();
            if (otherStart<=start && otherEnd>=end) {
                if (includeAllOverlapping) current=1.0;
                else return "1.0"; //  full overlap. Return early since this can not be improved
            } else if (start<otherStart && end<=otherEnd) { // region starts before other (overlaps upstream end of other)
                double overlap=end-otherStart+1;
                current=overlap/length;
            } else if (start>=otherStart && end>otherEnd) { // region ends after other (overlaps downstream end of other)
                double overlap=otherEnd-start+1;
                current=overlap/length;               
            } else if (start<otherStart && end>otherEnd) { // other region fully within current
                double overlap=otherEnd-otherStart+1;
                current=overlap/length; 
            } 
            if (current>greatestOverlap) greatestOverlap=current;
            if (includeAllOverlapping) allpercentages.add(current);            
        }        
        return (includeAllOverlapping)?MotifLabEngine.splice(allpercentages, ",") :(""+greatestOverlap);
    }
    
    private String getWeightedValue(String trackFormat, Region region, NumericSequenceData numericSequence) {
        String type=region.getType();
        int regionStart=region.getRelativeStart();
        int regionEnd=region.getRelativeEnd();
        Data motif=(type==null)?null:engine.getDataItem(type);
        if (motif instanceof Motif) {
           double[] weights=((Motif)motif).getICcontentForColumns(region.getOrientation()==Region.REVERSE, true);
           if (weights!=null) {
                    if (trackFormat.equals("weighted average") || trackFormat.equals("weighted avg")) return ""+numericSequence.getWeightedAverageValueInInterval(regionStart, regionEnd, weights);
               else if (trackFormat.equals("weighted min") || trackFormat.equals("weighted minimum")) return ""+numericSequence.getWeightedMinValueInInterval(regionStart, regionEnd, weights);
               else if (trackFormat.equals("weighted max") || trackFormat.equals("weighted maximum")) return ""+numericSequence.getWeightedMaxValueInInterval(regionStart, regionEnd, weights);
               else if (trackFormat.equals("weighted median")) return ""+numericSequence.getWeightedMedianValueInInterval(regionStart, regionEnd, weights);
               else if (trackFormat.equals("weighted sum")) return ""+numericSequence.getWeightedSumValueInInterval(regionStart, regionEnd, weights);
           }
        }
        // If we have not returned yet something wrong has happened. Use non-weighted defaults
             if (trackFormat.equals("weighted average") || trackFormat.equals("weighted avg")) return ""+numericSequence.getAverageValueInInterval(regionStart, regionEnd);
        else if (trackFormat.equals("weighted min") || trackFormat.equals("weighted minimum")) return ""+numericSequence.getMinValueInInterval(regionStart, regionEnd);
        else if (trackFormat.equals("weighted max") || trackFormat.equals("weighted maximum")) return ""+numericSequence.getMaxValueInInterval(regionStart, regionEnd);
        else if (trackFormat.equals("weighted median")) return ""+numericSequence.getMedianValueInInterval(regionStart, regionEnd);
        else if (trackFormat.equals("weighted sum")) return ""+numericSequence.getSumValueInInterval(regionStart, regionEnd);     
        else return "ERROR";
    }
    
    
    private boolean regionsAreOverlapping(Region region1, Region region2) {
        if (region1.getRelativeEnd()<region2.getRelativeStart() || region2.getRelativeEnd()<region1.getRelativeStart()) return false;
        else return true;
    }
    
    private String getRegionListAsString(Region region, ArrayList<Region> list, boolean includeScores, boolean includeDistance, String additional) {
        StringBuilder builder=new StringBuilder();
        boolean motifProperty=false;
        boolean moduleProperty=false;
        String property=null;
        if (additional!=null) {
            if (additional.startsWith("motif ")) {
                property=additional.substring("motif ".length());
                motifProperty=true;
            } else if (additional.startsWith("module ")) {
                 property=additional.substring("module ".length());
                 moduleProperty=true;
            } else if (additional.startsWith("region ")) {
                property=additional.substring("region ".length());
            } else property=additional;
        }
        
        for (Region listentry:list) {
            String type=listentry.getType();
            if (type==null || type.isEmpty()) type="UNKNOWN";
            if (builder.length()>0) builder.append(",");
            builder.append(type);
            if (includeScores) {
                builder.append("(");
                double score=listentry.getScore();
                if (score==(int)score) builder.append((int)listentry.getScore());
                else builder.append(listentry.getScore());
                builder.append(")");                
            }
            if (includeDistance) {
                builder.append("[");
                builder.append(getShortestDistance(region,listentry));
                builder.append("]");                
            }       
            if (property!=null) {
                String valueAsString="";
                if (motifProperty) {
                    Motif motif=(Motif)engine.getDataItem(type, Motif.class);
                    if (motif==null) valueAsString="N/A";
                    else {
                        try {
                            Object propValue=motif.getPropertyValue(property, engine);
                            valueAsString=getValueAsString(propValue);
                        } catch (ExecutionError e) {}                       
                    }
                } else if (moduleProperty) {
                    ModuleCRM cisRegModule=(ModuleCRM)engine.getDataItem(type, ModuleCRM.class);
                    if (cisRegModule==null) valueAsString="N/A";
                    else {
                        try {
                            Object propValue=cisRegModule.getPropertyValue(property, engine);
                            valueAsString=getValueAsString(propValue);
                        } catch (ExecutionError e) {}                       
                    }                    
                } else {
                    Object propValue=listentry.getProperty(property);
                    valueAsString=getValueAsString(propValue);
                }
                builder.append("(");
                builder.append(valueAsString);
                builder.append(")");                  
            }
        }
        return builder.toString();        
    }
    
    private Integer getDistanceTo(RegionSequenceData targetSequence, Region sourceRegion, boolean allowOverlap, Object typelist) {
        int closest=Integer.MAX_VALUE;
        int last=Integer.MAX_VALUE;
        ArrayList<Region> regions=targetSequence.getOriginalRegions();
        if (typelist!=null) regions=filterBasedOnCondition(regions,sourceRegion, typelist); // remove regions that should not be considered 
        for (Region other:regions) {
            if (other==sourceRegion) continue;
            int distance=getShortestDistance(sourceRegion,other);
            if (distance<0 && !allowOverlap) continue;
            if (distance<closest) closest=distance;
            if (distance>last) break;
            last=distance;
        }
        if (closest==Integer.MAX_VALUE) return null;
        else return closest;
    }
    
    private ArrayList<Region> filterBasedOnCondition(ArrayList<Region> regions, Region source, Object typelist) {
       ArrayList<Region> newlist=new ArrayList<Region>();
       if (typelist instanceof String) { // String=="interaction partner" (or "interacting"). The String type is just used as a code 
           Motif motif=(Motif)engine.getDataItem(source.getType(),Motif.class);
           if (motif==null) return newlist;
           for (Region r:regions) {
               if (motif.isKnownInteractionPartner(r.getType())) newlist.add(r);
           }           
       } else if (typelist instanceof DataCollection) {
           for (Region r:regions) {
               if (((DataCollection)typelist).contains(r.getType())) newlist.add(r);
           }                    
       } else if (typelist instanceof TextVariable) {
           for (Region r:regions) {
               if (((TextVariable)typelist).contains(r.getType())) newlist.add(r);
           }            
       }
       return newlist;       
    }
    
    private int getShortestDistance(Region region1, Region region2) {
        int r1s=region1.getRelativeStart();
        int r2s=region2.getRelativeStart();
        int r1e=region1.getRelativeEnd();
        int r2e=region2.getRelativeEnd();        
             if (r2s>r1e) return (r2s-r1e)-1; // R2 follows R1 non-overlapping. Subtract 1 to count bases between the two regions
        else if (r1s>r2e) return (r1s-r2e)-1; // R1 follows R2 non-overlapping. Subtract 1 to count bases between the two regions
        else { // overlap
            return -1; // this is just an indicator
        } 
    }
    
    private String getBooleanString(boolean value,int format) {
            if (format==BOOLEAN_FORMAT_TRUE_FALSE) return (value)?"True":"False";
       else if (format==BOOLEAN_FORMAT_TRUE_FALSE_ABBR) return (value)?"T":"F";
       else if (format==BOOLEAN_FORMAT_YES_NO) return (value)?"Yes":"No";
       else if (format==BOOLEAN_FORMAT_YES_NO_ABBR) return (value)?"Y":"N";
       else if (format==BOOLEAN_FORMAT_0_1) return (value)?"0":"1";
       else return Boolean.toString(value);
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Unable to parse input in "+name+" format");
    }
    
           
    
    /**
     * Compares two Regions according to specified sorting order
     */
    private class RegionComparator implements Comparator<motiflab.engine.data.Region> {
        String sort1;
        String sort2;
        String sort3;
        int orientation=Sequence.DIRECT;
        
        public RegionComparator(String sort1,String sort2,String sort3) {
            this.sort1=sort1;
            this.sort2=sort2;
            this.sort3=sort3;
            orientation=Sequence.DIRECT;
        }
        
        public void setOrientation(int orientation) {
            this.orientation=orientation;
        }
        
        @Override
        public int compare(Region region1, Region region2) {
            int result=0;
            result=compareProperty(region1, region2, sort1);
            if (result!=0) return result;
            result=compareProperty(region1, region2, sort2);
            if (result!=0) return result;
            result=compareProperty(region1, region2, sort3);
            return result;
        }
        
        
        private int compareProperty(Region region1,Region region2, String property) {
            if (property.equals("Position")) {
              if (orientation==Sequence.DIRECT) {
                  if (region1.isLocatedPriorTo(region2)) return -1;
                  else if (region2.isLocatedPriorTo(region1)) return 1;
                  else return 0;                  
              } else {
                  if (region1.isLocatedPriorToOnReverseStrand(region2)) return -1;
                  else if (region2.isLocatedPriorToOnReverseStrand(region1)) return 1;
                  else return 0;                  
              }
            } else if (property.equals("Type")) {
                return region1.getType().compareTo(region2.getType());
            } else if (property.equals("Score")) {
                double score1=region1.getScore();
                double score2=region2.getScore();
                if (score1>score2) return -1;
                else if (score2>score1) return 1;
                else return 0;
            } 
            return 0;
        }
        
    }
    
  
    private String getValueAsString(Object value) {
        if (value==null) return "";
        else if (value instanceof ArrayList) return MotifLabEngine.splice((ArrayList)value, ",");
        else if (value instanceof String[]) return MotifLabEngine.splice((String[])value, ",");                
        else return value.toString();        
    }
}

        
       
        
        
