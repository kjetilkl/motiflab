/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.SequenceCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_GFF extends DataFormat {
    private String name="GFF";
    private static final int RELATIVE=0;
    private static final int GENOMIC=1;
    private static final int RELATIVE_TSS=2;    
    private static final String DIRECT="Direct";
    private static final String REVERSE="Reverse";
    private static final String RELATIVE_ORIENTATION="Relative";
    private static final String OPPOSITE="Opposite";
    private static final Pattern fieldcode=Pattern.compile("\\{(.+?)\\}");
    
    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};
    //private String GFFformatString="{SEQUENCENAME}\t{FEATURE}\tmisc_feature\t{START}\t{END}\t{SCORE}\t{STRAND}\t.\t{ATTRIBUTES}";
    
    public DataFormat_GFF() {
        addParameter("Position", "Genomic", new String[]{"Genomic","Relative","TSS-Relative"},"Specifies whether the coordinate positions [start-end] for a region should be given relative to the start of the chromosome ('Genomic'), relative to the upstream start of the sequence ('Relative') or relative to the transcription start site associated with the sequence ('TSS-Relative')");
        addParameter("Relative-offset", new Integer(1), null,"If the 'Position' setting is set to 'Relative', this offset-value specifies what position the first base in the sequence should start at (common choices are 0 or 1). If the 'Position' setting is 'TSS-Relative', a value of 0 here will place the TSS at position '+0' whereas any other value will place the TSS at position '+1' (and the coordinate-system will then skip 0 and go directly from -1 to +1)");
        addParameter("Orientation", RELATIVE_ORIENTATION,new String[]{DIRECT,REVERSE,RELATIVE_ORIENTATION,OPPOSITE},"Orientation of relative coordinates (only applicable if the 'Position' setting is 'Relative')");        
        addOptionalParameter("Format", "", null,"<html>Here you can specify a different format to use rather than the standard GFF fields.<br>In additional to literal text, the format string can contain <i>field</i>-codes surrounded by braces, e.g. <tt>{TYPE}</tt>.<br>These field codes will be replaced by the corresponding property value of the region.<br>Standard recognized field codes include: SEQUENCENAME,START,END,TYPE,SCORE,STRAND and ATTRIBUTES.<br>Other field codes can be used to refer to user-defined properties.<br>Example: to output a comma-separated list with the type of the region plus start and end coordinates in the sequence<br>,use the following format string: \"<tt>{TYPE},{START},{END}</tt>\"<br>Use <tt>\\t</tt> to insert tabs and <tt>\\n</tt> to insert newlines<html>");
        addOptionalParameter("Include module motifs", Boolean.FALSE,  new Boolean[]{ Boolean.TRUE, Boolean.FALSE},"If selected, the constituent single TF binding sites making up a cis-regulatory module will also be included for each module region");
        addOptionalParameter("Skip header lines", new Integer(0), null,"If the GFF-file starts with header lines that are not preceeded by '#' you can specify the number of lines to skip here");
        addOptionalParameter("Sort1", "Position", new String[]{"Position","Type","Score"},null);
        addOptionalParameter("Sort2", "Type", new String[]{"Position","Type","Score"},null);
        addOptionalParameter("Sort3", "Score", new String[]{"Position","Type","Score"},null);         
        
        setParameterFilter("Sort1","output");
        setParameterFilter("Sort2","output");
        setParameterFilter("Sort3","output");
        setParameterFilter("Skip header lines","input");        
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
        return "gff";
    }
 
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }        
        setProgress(5);
        String sort1,sort2,sort3,offsetString,formatString,orientation;
        int relativeoffset=1;
        boolean includeModuleMotifs=false;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             sort1=(String)settings.getResolvedParameter("Sort1",defaults,engine); 
             sort2=(String)settings.getResolvedParameter("Sort2",defaults,engine);
             sort3=(String)settings.getResolvedParameter("Sort3",defaults,engine);
             offsetString=(String)settings.getResolvedParameter("Position",defaults,engine);
             relativeoffset=(Integer)settings.getResolvedParameter("Relative-offset",defaults,engine);
             formatString=(String)settings.getResolvedParameter("Format",defaults,engine);
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
             includeModuleMotifs=(Boolean)settings.getResolvedParameter("Include module motifs",defaults,engine);           
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
           formatString=(String)getDefaultValueForParameter("Format");
           orientation=(String)getDefaultValueForParameter("Orientation");
           includeModuleMotifs=(Boolean)getDefaultValueForParameter("Include module motifs");
        }
        if (formatString!=null && formatString.trim().isEmpty()) formatString=null;
        if (formatString!=null) { // replace escape characters
            formatString=formatString.replace("\\\\", "\\"); // escaped \
            formatString=formatString.replace("\\t", "\t"); // escaped TAB
            formatString=formatString.replace("\\n", "\n"); // escaped newline           
        }
        int offset=GENOMIC;
        if (offsetString!=null && offsetString.equalsIgnoreCase("Relative")) offset=RELATIVE;
        else if (offsetString!=null && offsetString.equalsIgnoreCase("TSS-Relative")) offset=RELATIVE_TSS;
        String outputString="";
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
            outputString=outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, regionComparator,offset,relativeoffset,orientation,formatString,includeModuleMotifs, task, engine);
        } else if (dataobject instanceof RegionSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((RegionSequenceData)dataobject,regionComparator,offset,relativeoffset,orientation,formatString,includeModuleMotifs,null, task, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }    

    /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, RegionComparator regionComparator, int offset,int relativeoffset, String orientationString, String formatString, boolean includeModuleMotifs, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();        
        int size=collection.getNumberofSequences();
        int i=0;
        int[] modulecounter=new int[]{0}; // this is an array in order to be able to pass it as a reference and not a copy
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence   
              String sequenceName=sequence.getName();
              RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence,regionComparator,offset,relativeoffset,orientationString,formatString,includeModuleMotifs,modulecounter, task, outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }      
        return outputString.toString();
    }    
    
    
    /** output-formats a single sequence */
    private void outputSequence(RegionSequenceData sequence, RegionComparator regionComparator, int offset,int relativeoffset, String orientationString, String formatString, boolean includeModuleMotifs, int[] modulecounter, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
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

            int count=0;
            for (Region region:regionList) {
               count++;
               if (count%200==0) {
                  if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                  if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
                  Thread.yield();
               }
               if (includeModuleMotifs && sequence.isModuleTrack()) {
                  modulecounter[0]++; // this is an array with a single entry instead of a normal integer to be able to pass it to methods by reference and not by copy
                  String moduleID="Mod"+modulecounter[0];
                  outputRegion(region, sequence, shownStrand, offset, relativeoffset, formatString, moduleID, null, "module", outputString);
                  HashMap<String, Object> motifsites=region.getNestedRegionProperties(true);
                  for (String modulemotifname:motifsites.keySet()) {
                      Object entry=motifsites.get(modulemotifname);
                      if (entry instanceof Region) outputRegion((Region)entry, sequence, shownStrand, offset, relativeoffset, formatString, moduleID, modulemotifname, "motif", outputString);
                      else if (entry instanceof ArrayList) {
                         for (Object reg:(ArrayList)entry) {
                             outputRegion((Region)reg, sequence, shownStrand, offset, relativeoffset, formatString, moduleID, modulemotifname, "motif", outputString);
                         }
                      }
                  } // for each module motif group
               } else {
                 outputRegion(region, sequence, shownStrand, offset, relativeoffset, formatString, null, null,"misc_feature",outputString);
               }
            }
    }


    private void outputRegion(Region region, RegionSequenceData sequence, int shownStrand, int offset, int relativeoffset, String formatString, String moduleID, String moduleMotifName, String featureType, StringBuilder outputString) {
       String sequenceName=sequence.getName();
       String featureName=sequence.getParent().getName();
       String source=sequence.getDataSource();
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

       String attributes="";
       String[] properties=region.getAllPropertyNames();
       for (int i=0;i<properties.length;i++) {
           Object val=region.getProperty(properties[i]);
           if (val instanceof Region || val instanceof ArrayList) continue; // these are nested Regions. We don't want to include those!
           if (val==null) attributes+=properties[i]+"=null;";
           else attributes+=properties[i]+"=\""+val.toString()+"\";";
       }
       String regionsequence=region.getSequence();
       if (regionsequence!=null) attributes+="sequence=\""+regionsequence+"\";";
       if (moduleID!=null) attributes=("module_identifier=\""+moduleID+"\";")+attributes; // add moduleID to start of attributes string
       if (moduleMotifName!=null) attributes=("module_motif=\""+moduleMotifName+"\";")+attributes; // add moduleMotifName to start of attributes string
       String line=formatString;
       if (line!=null) {
           line=line.replace("{SEQUENCENAME}", sequenceName);
           line=line.replace("{SOURCE}", source);
           line=line.replace("{FEATURE}", featureName);
           line=line.replace("{START}", ""+start);
           line=line.replace("{END}", ""+end);
           line=line.replace("{SCORE}", ""+region.getScore());
           line=line.replace("{STRAND}", strand);
           line=line.replace("{TYPE}", region.getType());
           line=line.replace("{ATTRIBUTES}", attributes);      
           // now see if there are any other field codes in use
           if (line.indexOf('{')>=0 && line.indexOf('}')>=0) {
               HashMap<String,String> fields=new HashMap<String, String>();
               Matcher matcher=fieldcode.matcher(line);  
               while (matcher.find()) {
                   String code=matcher.group(1);
                   Object value=region.getProperty(code);
                   if (value!=null) fields.put(code,value.toString());
                   else fields.put(code,""); 
               }
               for (String key:fields.keySet()) {
                   line=line.replace("{"+key+"}", fields.get(key));
               }
           }
       } else { // default format
           String type=region.getType();
           if (type==null) type=""; else type="type=\""+type+"\";";
           line=sequenceName+"\t"+featureName+"\t"+featureType+"\t"+start+"\t"+end+"\t"+region.getScore()+"\t"+strand+"\t.\t"+type+attributes;
       }
       outputString.append(line);
       outputString.append("\n");
    }

    /**
     * Compares two Regions according to specified sorting order
     */
    private class RegionComparator implements Comparator<org.motiflab.engine.data.Region> {
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
        String formatString="";
        String offsetString="";
        String orientation="";
        int relativeoffset=0;
        boolean includeModuleMotifs=true;
        int skipheaderlines=0;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             formatString=(String)settings.getResolvedParameter("Format",defaults,engine); 
             offsetString=(String)settings.getResolvedParameter("Position",defaults,engine);
             relativeoffset=(Integer)settings.getResolvedParameter("Relative-offset",defaults,engine);
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine);
             includeModuleMotifs=(Boolean)settings.getResolvedParameter("Include module motifs", defaults, engine);
             skipheaderlines=(Integer)settings.getResolvedParameter("Skip header lines",defaults,engine);           
          } 
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           formatString=(String)getDefaultValueForParameter("Format");
           offsetString=(String)getDefaultValueForParameter("Position");
           relativeoffset=(Integer)getDefaultValueForParameter("Relative-offset");
           orientation=(String)getDefaultValueForParameter("Orientation");
           includeModuleMotifs=(Boolean)getDefaultValueForParameter("Include module motifs");
           skipheaderlines=(Integer)getDefaultValueForParameter("Skip header lines");
        }
        if (formatString!=null && (formatString.trim().isEmpty())) formatString=null;
        if (formatString!=null) { // replace escape characters
            formatString=formatString.replace("\\\\", "\\"); // escaped \
            formatString=formatString.replace("\\t", "\t"); // escaped TAB
            formatString=formatString.replace("\\n", "\n"); // escaped newline           
        }        
        if (target==null) target=new RegionDataset("temporary");
        if (target instanceof RegionDataset) { // The RegionDataset might not contain RegionSequenceData objects for all Sequences. Add them if they are missing!
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));     
            }            
        }
        int count=0;
        HashMap<String,Region> modules=(includeModuleMotifs)?new HashMap<String,Region>():null;
        for (String line:input) { // parsing each line in succession
            count++;
            if (skipheaderlines>0) {skipheaderlines--;continue;}
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                
               Thread.yield();
            }            
            if (line.startsWith("#") || line.isEmpty()) continue; // GFF comment line
            HashMap<String,Object> map=null;
            if (formatString!=null) map=parseSingleLineFromPattern(line,formatString, count);
            else map=parseSingleLineInStandardFormat(line, count);
            // check that start<=end in the map. Else assume reverse strand
            if (map.get("START") instanceof Integer && map.get("END") instanceof Integer) {
                int start=(Integer)map.get("START");
                int end=(Integer)map.get("END");
                if (start>end) {
                    int swap=start;
                    start=end;
                    end=swap;
                    map.put("START", start);
                    map.put("END", end);
                    map.put("STRAND", "-");
                }
            }
            String motiformodule=(String)map.remove("_FEATURETYPE_"); // this property should not be passed on to the new Region
            String moduleID=(String)map.remove("module_identifier");  // this property should not be passed on to the new Region
            String sequenceName=map.get("SEQUENCENAME").toString(); // this can return an Integer if the sequence name is just a number! Hence "toString" instead of a cast
            sequenceName=convertIllegalSequenceNamesIfNecessary(sequenceName, false);
            String modulemotifname=(String)map.get("module_motif");
            RegionSequenceData targetSequence=null;
            // is this region a modulemotif site? If so, add it to its parent module site
            if (includeModuleMotifs && motiformodule!=null && motiformodule.equalsIgnoreCase("motif")) {
                if (moduleID==null || moduleID.isEmpty()) throw new ParseError("Missing 'module_identifier' attribute for motif site", count);
                if (modulemotifname==null || modulemotifname.isEmpty()) throw new ParseError("Missing 'module_motif' attribute for motif site", count);
                Region moduleregion=modules.get(moduleID);
                if (moduleregion!=null) addRegionToTarget(moduleregion, map, offsetString, relativeoffset, orientation);
                // else throw new ParseError("Unrecognized module_identifier: " + moduleID + ". (note that module sites must be listed before their constituent motif sites");
                continue;
            }
            Region lastadded=null;
            if (target instanceof RegionSequenceData) {
               targetSequence=(RegionSequenceData)target;
               if (targetSequence==null || !targetSequence.getName().equals(sequenceName)) continue; // the sequence mentioned in the file does not correspond to any known sequence
               lastadded=addRegionToTarget(targetSequence,map,offsetString,relativeoffset,orientation);
            } else if (target instanceof RegionDataset) {
                targetSequence=(RegionSequenceData)((RegionDataset)target).getSequenceByName(sequenceName);
                if (targetSequence==null) continue; // the sequence mentioned in the file does not correspond to any known sequence
                lastadded=addRegionToTarget(targetSequence,map,offsetString,relativeoffset,orientation);
            } else if (target instanceof DataSegment) {
                lastadded=addRegionToTarget(target,map,offsetString,relativeoffset,orientation);
            } else throw new ParseError("SLOPPY PROGRAMMING ERROR: non-Region data as target for GFF dataformat: "+target.getClass().getSimpleName());
            // is the new region a module site or modulemotif site?
            if (includeModuleMotifs && motiformodule!=null && motiformodule.equalsIgnoreCase("module")) {
                if (moduleID==null || moduleID.isEmpty()) throw new ParseError("Missing 'module_identifier' attribute for module site");
                else if (lastadded!=null) modules.put(moduleID, lastadded);
            }
        }
        return target;      
    }
    
    /** Creates a new region based on the data in the given map and adds it to the target.
     *  Also returns a reference to it
     */
    private Region addRegionToTarget(Object target, HashMap<String,Object> map, String offsetString, int relativeoffset, String orientationString) throws ParseError {
        int start=0, end=0; // these are offset relative to the start of the parent sequence
        int targetOrientation=Sequence.DIRECT;
        int targetStart=0;
        int targetSize=0;
        RegionSequenceData targetSequence=null;
        if (target instanceof RegionSequenceData) {
            targetOrientation=((RegionSequenceData)target).getStrandOrientation();
            targetStart=((RegionSequenceData)target).getRegionStart();
            targetSize=((RegionSequenceData)target).getSize();
            targetSequence=(RegionSequenceData)target;
        } else if (target instanceof DataSegment) {
            targetStart=((DataSegment)target).getSegmentStart();
            targetSize=((DataSegment)target).getSize();
        } else if (target instanceof Region) {
            targetSequence=((Region)target).getParent();
            targetOrientation=((RegionSequenceData)targetSequence).getStrandOrientation();
            targetStart=((RegionSequenceData)targetSequence).getRegionStart();
            targetSize=((RegionSequenceData)targetSequence).getSize();
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
        String annotatedOrientation=(String)map.get("STRAND"); // the orientation in the GFF file
        int orientation=Sequence.DIRECT;    
        if (annotatedOrientation==null) annotatedOrientation="+";
        if (offsetString.equalsIgnoreCase("Genomic")) { // always use "DIRECT" orientation for genomic positions
            start-=targetStart; // convert genomic coordinates to sequence-relative
            end-=targetStart;   // convert genomic coordinates to sequence-relative
                 if (annotatedOrientation.equals("+")) orientation=Region.DIRECT;
            else if (annotatedOrientation.equals("-")) orientation=Region.REVERSE;
            else orientation=Region.INDETERMINED;
        } else if (offsetString.equalsIgnoreCase("TSS-Relative") && targetSequence!=null && targetSequence.getTSS()!=null) {
            // orientation is relative to target sequence => targetOrientation
            if (relativeoffset>0) { // Skipping position 0. TSS starts at +1
                if (start>=1) start--;
                if (end>=1) end--;
            }
            if (targetOrientation==Sequence.REVERSE) {
                int offset=targetSequence.getTSS()-targetStart;  
                start-=offset;
                end-=offset;  
                int annotatedStart=start;
                int annotatedEnd=end;
                start=-annotatedEnd;
                end=-annotatedStart;
            } else { // Direct strand
                int offset=targetSequence.getTSS()-targetStart;
                start+=offset;
                end+=offset;                
            }
                 if (annotatedOrientation.equals(".")) orientation=Region.INDETERMINED;
            else if (annotatedOrientation.equals("+")) orientation=(targetOrientation==Sequence.DIRECT)?Region.DIRECT:Region.REVERSE;
            else orientation=(targetOrientation==Sequence.DIRECT)?Region.REVERSE:Region.DIRECT;            
        } else { // relative offsets. And orientation could be DIRECT,REVERSE,FROM GENE or OPPOSITE
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
        }               
        if (end<0 || start>=targetSize) return null; // region is outside sequence (note that these are relative coordinates)
        RegionSequenceData parentSequence=null;
        if (target instanceof RegionSequenceData) parentSequence=(RegionSequenceData)target;
        else if (target instanceof Region) parentSequence=((Region)target).getParent();
        Region newRegion=new Region(parentSequence, start, end, type, score, orientation);
        for (String property:map.keySet()) {
            if (property.equalsIgnoreCase("SEQUENCENAME") || 
                property.equalsIgnoreCase("FEATURE") || 
                property.equalsIgnoreCase("SOURCE") || 
                property.equalsIgnoreCase("START") || 
                property.equalsIgnoreCase("END") || 
                property.equalsIgnoreCase("SCORE") || 
                property.equalsIgnoreCase("TYPE") ||
                property.equalsIgnoreCase("STRAND") ||
                property.equalsIgnoreCase("module_motif")) continue;
            else if (property.equalsIgnoreCase("SEQUENCE")) {
                Object sequence=map.get(property);
                if (sequence instanceof String && !((String)sequence).trim().isEmpty()) newRegion.setSequence((String)sequence);
            } else {
                newRegion.setProperty(property, map.get(property));
            }
        }
        //System.err.println("Add to ["+target.toString()+"]  offset="+offsetString+"  relative="+relativeoffset+"  orientionString="+orientationString+":  region="+newRegion.toString());
             if (target instanceof RegionSequenceData) ((RegionSequenceData)target).addRegion(newRegion);   
        else if (target instanceof DataSegment) ((DataSegment)target).addRegion(newRegion);
        else if (target instanceof Region) { // add motif to module
            String modulemotifname=(String)map.get("module_motif");
            ((Region)target).addNestedRegion(modulemotifname,newRegion,true);
        }
        return newRegion;
    }
    
    /** This method will convert a value string to Integer, Double, Boolean or String depending on its contents */
    private Object getMotifPropertyValue(String valuestring) {
        if (valuestring==null || valuestring.trim().isEmpty()) return null;
        Object value=Motif.getObjectForPropertyValueString(valuestring);
        if (value instanceof ArrayList) return valuestring;
        return value;
    }
    
    /** parses a single line in a GFF-file and returns a HashMap with the different properties (with values as strings!) according to the capturing groups in the formatString */
    private HashMap<String,Object> parseSingleLineInStandardFormat(String line, int lineNumber) throws ParseError {
        HashMap<String,Object> result=new HashMap<String,Object>();
        String[] fields=line.split("\t");
        if (fields.length<8) throw new ParseError("Expected at least 8 fields per line in GFF-format. Got "+fields.length+":\n"+line, lineNumber);
        // engine.logMessage("Parsed standard: "+line+" =>"+fields[0]);
        result.put("SEQUENCENAME",fields[0]);
        result.put("FEATURE",fields[1]);
        result.put("_FEATURETYPE_",fields[2]);
        result.put("SOURCE",fields[1]); // this is correct
        try {
            result.put("START",Integer.parseInt(fields[3]));        
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+e.getMessage(), lineNumber);}
        try {
            result.put("END",Integer.parseInt(fields[4]));        
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+e.getMessage(), lineNumber);}
        try {
            result.put("SCORE",Double.parseDouble(fields[5]));        
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+e.getMessage(), lineNumber);}
        
        result.put("STRAND",fields[6]);
        if (fields.length>=9) {
            String[] attributes=fields[8].split(";");
            for (String attribute:attributes) {
                String[] pair=attribute.split("=",2);
                if (pair.length!=2) throw new ParseError("Attribute not in recognized 'key=value' format: "+attribute, lineNumber); 
                String key=pair[0].trim();
                if (key.equalsIgnoreCase("type")) key="TYPE";
                String value=pair[1].trim();
                if (value.startsWith("\"") && value.endsWith("\"")) value=value.substring(1, value.length()-1);
                result.put(key,getMotifPropertyValue(value));
            }           
        }
        return result;
    }

    /** parses a single line in a GFF-file and returns a HashMap with the different properties according to the capturing groups in the formatString */
    private HashMap<String,Object> parseSingleLineFromPattern(String line, String formatString, int lineNumber) throws ParseError {
        HashMap<String,Object> result=new HashMap<String,Object>();
        //engine.logMessage("Parsed pattern: "+line);
        Pattern pattern=Pattern.compile("\\{(.+?)\\}"); // previously: compile("\\{([A-Z]+)\\}")
        Matcher matcher=pattern.matcher(formatString);
        ArrayList<String> fields=new ArrayList<String>(); // these are the names inside braces (in order)
        while (matcher.find()) {  
             String field=matcher.group(1);
             fields.add(field);
        }
        formatString=formatString.replaceAll("\\{(.+?)\\}", "([^\t]+)"); // this replaces each fieldcode with an expression that reads "anything but TAB"
        pattern=Pattern.compile(formatString);
        matcher=pattern.matcher(line);
        if (matcher.find()) {
           for (int i=1;i<=matcher.groupCount();i++) {
               String field=fields.get(i-1);
               String matchString=matcher.group(i);
               Object value;
               if (field.equals("START") || field.equals("END")) {
                   try {value=Integer.parseInt(matchString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for "+field+": "+e.getMessage(), lineNumber);}
               } else if (field.equals("SCORE")) {
                   try {value=Double.parseDouble(matchString);} catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for "+field+": "+e.getMessage(), lineNumber);}           
               } else value=getMotifPropertyValue(matchString);
               result.put(field,value);
               //engine.logMessage("  "+field+" => "+value.toString());
           } 
        } else throw new ParseError("Unable to parse GFF line: "+line, lineNumber);

        return result;
    }

}

        
       
        
        
