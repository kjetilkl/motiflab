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

import motiflab.engine.task.OperationTask;
import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.Parameter;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Module;
import motiflab.engine.data.ModuleCollection;
import motiflab.engine.data.Motif;
import motiflab.engine.data.SequenceCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_ModuleSearcher extends DataFormat {
    private String name="ModuleSearcher";
    private static final String SITES="sites";
    private static final String MODULES="modules";


    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    public DataFormat_ModuleSearcher() {
       addParameter("datatype", "sites", new String[]{"sites","modules"},"specifies whether to parse and return modules or binding sites");
       addParameter("module model", "General", new String[]{"General","Specific"},"specifies whether the module models created should be general or specific to the sites used as basis");

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
        if (!(dataobject instanceof RegionDataset || dataobject instanceof RegionSequenceData)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);

        String outputString="";
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
            outputString=outputMultipleSequences((RegionDataset)dataobject, sequenceCollection, task, engine);
        } else if (dataobject instanceof RegionSequenceData){
            StringBuilder builder=new StringBuilder();
            outputSequence((RegionSequenceData)dataobject,task, builder);
            outputString=builder.toString();
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString,getName());
        setProgress(100);
        return outputobject;
    }


   /** Outputs multiple sequences (i.e. a whole FeatureDataset)
     *  The sequences are output in the order determined by the DefaultSequenceCollection (as shown in the GUI)
     */
    private String outputMultipleSequences(RegionDataset sourcedata, SequenceCollection collection, ExecutableTask task, MotifLabEngine engine) throws InterruptedException {
        StringBuilder outputString=new StringBuilder();
        int size=collection.getNumberofSequences();
        int i=0;
        ArrayList<Sequence> sequences=collection.getAllSequencesInDefaultOrder(engine);
        for (Sequence sequence:sequences) { // for each sequence
              String sequenceName=sequence.getName();
              RegionSequenceData sourceSequence=(RegionSequenceData)sourcedata.getSequenceByName(sequenceName);
              outputSequence(sourceSequence, task, outputString);
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%3==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        return outputString.toString();
    }


    /** outputformats a single sequence */
    private void outputSequence(RegionSequenceData sequence, ExecutableTask task, StringBuilder outputString) throws InterruptedException {
        ArrayList<Region> regionList=sequence.getAllRegions();
        String sequenceName=sequence.getName();
        String featureName=sequence.getParent().getName();
        int count=0;
        for (Region region:regionList) {
           count++;
           if (count%200==0) {
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              Thread.yield();
           }
           int start=region.getGenomicStart();
           int end=region.getGenomicEnd();
           String strand=".";
                if (region.getOrientation()==Region.INDETERMINED) strand=".";
           else if (region.getOrientation()==Region.DIRECT) strand="+";
           else if (region.getOrientation()==Region.REVERSE) strand="-";

           String type=region.getType();
           if (type==null) type="unknown";
           type="\""+type+"\";";

           outputString.append(sequenceName);
           outputString.append("\t");
           outputString.append(featureName);
           outputString.append("\tmisc_feature\t");
           outputString.append(start);
           outputString.append("\t");
           outputString.append(end);
           outputString.append("\t");
           outputString.append(region.getScore());
           outputString.append("\t");
           outputString.append(strand);
           outputString.append("\t.\t");
           outputString.append("id ");
           outputString.append(type);
           outputString.append("\n");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        String returntype=MODULES;
        String modeltype="General";
        //if (input.isEmpty()) throw new ParseError("Unable to parse empty results file");
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             returntype=(String)settings.getResolvedParameter("datatype",defaults,engine);
             modeltype=(String)settings.getResolvedParameter("module model",defaults,engine);
          }
          catch (ExecutionError e) {throw new ParseError("An error occurred while resolving parser parameters: "+e.getMessage());}
          catch (Exception ex) {throw new ParseError("An error occurred while resolving parser parameters: "+ex.getMessage());}
        } else {
           returntype=(String)getDefaultValueForParameter("datatype");
           modeltype=(String)getDefaultValueForParameter("module model");
        }
        engine.logMessage("Selected '"+modeltype+"' model for modules in ModuleSearcher. (Note that only the 'General' model is currently supported)");

        // The following map stores lists of all singlemotif sites belonging to the same module in the same sequence 
        // the key to the hash is a concatenation of sequencename and moduleID: "Sequencename\tModuleID"
        HashMap<String, ArrayList<HashMap<String,Object>>> motifsites=new HashMap<String,ArrayList<HashMap<String,Object>>>();
        
        // This map stores lists of motifs appearing in modules. Key is moduleID
        HashMap<String,ArrayList<String>> modulemotifs=new HashMap<String,ArrayList<String>>();
        
        // store Module objects
        HashMap<String,Module> modules=new HashMap<String,Module>();
        
        int count=0;
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               Thread.yield();
            }
            if (line.startsWith("#") || line.isEmpty()) continue; // GFF comment line
            HashMap<String,Object> singleMotifSiteMap=parseSingleLineInStandardFormat(line);
            String sequenceName=(String)singleMotifSiteMap.get("SEQUENCENAME");
            String moduleName=(String)singleMotifSiteMap.get("MODULE");
            String motifName=(String)singleMotifSiteMap.get("MOTIF");
            if (!modules.containsKey(moduleName)) modules.put(moduleName, new Module(moduleName));
            String key=sequenceName+"\t"+moduleName;
            if (!motifsites.containsKey(key)) {
                ArrayList<HashMap<String,Object>> list=new ArrayList<HashMap<String,Object>>();
                motifsites.put(key, list);
            }
            ArrayList<HashMap<String,Object>> list=motifsites.get(key);
            list.add(singleMotifSiteMap);
            if (!modulemotifs.containsKey(moduleName)) {
                modulemotifs.put(moduleName, new ArrayList<String>());
            }
            ArrayList<String> mmlist=modulemotifs.get(moduleName);
            if (!mmlist.contains(motifName)) mmlist.add(motifName);
        }
        // Now, go through each site and determine the appearance of the modules: fill in order, distances and motif orientations
        // ** AS A FIRST ATTEMPT I WILL JUST USE A SIMPLE DEFINITION OF AN UNORDERED MODULE WITH NO CONSTRAINTS ***
        // (This will be called the "general" model. If will try to implement the "specific" model later if time permits...)

        for (Module module:modules.values()) { // update the modules by including modulemotifs!
            for (String motifname:modulemotifs.get(module.getName())) { // currently these appear in no particular order
                Data motifdata=engine.getDataItem(motifname);
                if (!(motifdata instanceof Motif)) motifdata=null;
                String modulemotifname=motifname;
                if (motifdata!=null && ((Motif)motifdata).getShortName()!=null) {
                    modulemotifname=Motif.cleanUpMotifShortName(((Motif)motifdata).getShortName(),true);
                }
                module.addModuleMotif(modulemotifname, (Motif)motifdata, Module.INDETERMINED);
            }
        }
        //
        if (returntype.equals(MODULES)) {
           if (target==null || !(target instanceof ModuleCollection)) target=new ModuleCollection("temporary");
           for (Module module:modules.values()) {
               ((ModuleCollection)target).addModuleToPayload(module); // Add to payload, it will be "revived" later by the module-discovery operation
           }
        } else { // return sites
             if (target==null || !(target instanceof RegionDataset)) target=new RegionDataset("temporary");
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) {
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));
                RegionSequenceData targetSequence=(RegionSequenceData)dataset.getSequenceByName(seq.getName());
                for (Module module:modules.values()) {
                    ArrayList<HashMap<String,Object>> singlesites=motifsites.get(seq.getName()+"\t"+module.getName());
                    if (singlesites==null || singlesites.isEmpty()) continue;
                    int modulestart=Integer.MAX_VALUE;
                    int moduleend=Integer.MIN_VALUE;
                    double score=0;
                    Region moduleRegion=new Region(targetSequence, modulestart, moduleend, module.getName(), 0, Module.INDETERMINED); // this is just a template for now. The values will be updated later!
                    for (HashMap<String,Object> sitemap:singlesites) {
                        Region siteRegion=createModuleMotifRegion(targetSequence,sitemap);
                        if (siteRegion==null) continue; // if outside sequence, but this should not happen
                        score+=siteRegion.getScore();
                        if (siteRegion.getRelativeStart()<modulestart) modulestart=siteRegion.getRelativeStart();
                        if (siteRegion.getRelativeEnd()>moduleend) moduleend=siteRegion.getRelativeEnd();
                        String modulemotifname=getCandidateModuleMotifNameFromMotifName(siteRegion.getType());
                        Object motifregions=moduleRegion.getProperty(modulemotifname);
                        if (motifregions==null) { // no regions registered for this motif
                            moduleRegion.setProperty(modulemotifname, siteRegion);
                        } else if (motifregions instanceof Region) { // this motif has one registered region before
                            ArrayList<Region> multipleregions=new ArrayList<Region>();
                            multipleregions.add((Region)motifregions);
                            multipleregions.add(siteRegion);
                        } else if (motifregions instanceof ArrayList) { // this region has multiple registered regions from before
                            ((ArrayList)motifregions).add(siteRegion);
                        }
                    }
                    moduleRegion.setRelativeStart(modulestart);
                    moduleRegion.setRelativeEnd(moduleend);
                    moduleRegion.setScore(score);
                    targetSequence.addRegion(moduleRegion);
                }
            }
        }
        return target;
    }


    private String getCandidateModuleMotifNameFromMotifName(String name) {
        Data motifdata=engine.getDataItem(name);
        if (motifdata instanceof Motif) {
            String modulemotifname=((Motif)motifdata).getShortName();
            if (modulemotifname==null || modulemotifname.isEmpty()) return name;
            else return Motif.cleanUpMotifShortName(modulemotifname,true);
        } else return name;
    }

    private Region createModuleMotifRegion(RegionSequenceData target, HashMap<String,Object> map) {
        int start=0, end=0; // these are offset relative to the start of the parent sequence
        int targetStart=target.getRegionStart();
        int targetSize=target.getSize();
        Object startValue=map.get("START");
        if (startValue instanceof Integer) start=(Integer)startValue;
        Object endValue=map.get("END");
        if (endValue instanceof Integer) end=(Integer)endValue;
        double score=0;
        Object scoreValue=map.get("SCORE");
        if (scoreValue instanceof Double) score=(Double)scoreValue;
        String type=(String)map.get("MOTIF");
        if (type==null) type="unknown_type";
        String annotatedOrientation=(String)map.get("STRAND"); // the orientation in the GFF file
        int orientation=Sequence.DIRECT;
             if (annotatedOrientation.equals("+")) orientation=Region.DIRECT;
        else if (annotatedOrientation.equals("-")) orientation=Region.REVERSE;
        else orientation=Region.INDETERMINED;
        // offset the start and end positions so that they are relative to target sequence
        start-=targetStart;
        end-=targetStart;
        if (end<0 || start>=targetSize) return null; // region is outside sequence
        Region newRegion=new Region(target, start, end, type, score, orientation);
        return newRegion;
    }


    /** parses a single line in a GFF-file and returns a HashMap with the different properties (with values as strings!) according to the capturing groups in the formatString */
    private HashMap<String,Object> parseSingleLineInStandardFormat(String line) throws ParseError {
        HashMap<String,Object> result=new HashMap<String,Object>();
        String[] fields=line.split("\t");
        if (fields.length!=9) throw new ParseError("Expected 9 fields per line in ModuleSearcher-format. Got "+fields.length+":\n"+line);
        //System.err.println("Parsed standard: "+line+" =>"+fields[0]);
        result.put("SEQUENCENAME",fields[0]);
        result.put("FEATURE",fields[1]);
        result.put("SOURCE",fields[1]); // this is correct
        try {
            result.put("START",Integer.parseInt(fields[3]));
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for START: "+e.getMessage());}
        try {
            result.put("END",Integer.parseInt(fields[4]));
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for END: "+e.getMessage());}
        try {
            result.put("SCORE",Double.parseDouble(fields[5]));
        } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numerical value for SCORE: "+e.getMessage());}

        result.put("STRAND",fields[6]);
        String module="unknown";
        String motif="unknown";
        Pattern pattern=Pattern.compile("id \"(Mod\\d+)(\\S+)\";");
        Matcher matcher=pattern.matcher(fields[8]);
        if (matcher.matches()) {
            module=matcher.group(1);
            motif=matcher.group(2);
        }
        result.put("MODULE",module);
        result.put("MOTIF",motif);
        return result;
    }


}





