/*
 
 
 */

package org.motiflab.engine.dataformat;

import java.util.ArrayList;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleMotif;
import org.motiflab.engine.data.MotifCollection;
/**
 *
 * @author kjetikl
 */
public class DataFormat_ClusterBuster extends DataFormat {
    private String name="ClusterBuster";
    private static final String SITES="sites";
    private static final String MODULES="modules";


    private Class[] supportedTypes=new Class[]{RegionSequenceData.class, RegionDataset.class};

    public DataFormat_ClusterBuster() {
       addParameter("datatype", "sites", new String[]{"sites","modules"},"specifies whether to parse and return modules or binding sites");

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
        return "cb";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
         throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format. Functionality not implemented");
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        String returntype=MODULES;
        //if (input.isEmpty()) throw new ParseError("Unable to parse empty results file");
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
        // setup target data
        if (returntype.equals(MODULES)) {
           if (target==null || !(target instanceof ModuleCollection)) target=new ModuleCollection("temporary");
        } else { // return sites
            if (target==null || !(target instanceof RegionDataset)) target=new RegionDataset("temporary");
            RegionDataset dataset=(RegionDataset)target;
            ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
            for (Data seq:sequences) { // add sequences that are missing
                if (dataset.getSequenceByName(seq.getName())==null) dataset.addSequence(new RegionSequenceData((Sequence)seq));
            }
        }

        String sequencename=null;
        int totalclusters=0;
        int clustersinsequence=0;
        double currentClusterScore=0;
        int currentClusterStart=0;
        int currentClusterEnd=0;
        boolean singlemotifsread=false;
        ArrayList<Region> singlemotifs=null;
        int count=0; // just to count input lines
        for (String line:input) { // parsing each line in succession
            count++;
            if (count%200==0) {
               if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
               if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
               Thread.yield();
            }
            if (line.startsWith(">")) {
                int pos=line.indexOf(" ");
                if (pos<0) pos=line.length()-1; // this should not happen, but just in case
                sequencename=line.substring(1,pos);
                singlemotifs=new ArrayList<Region>();
                clustersinsequence=0;
            } else if (line.startsWith("Location:")) {
                clustersinsequence++;
                totalclusters++;
                singlemotifsread=false;
                String[] elements=line.split(" ");
                if (elements.length!=4) throw new ParseError("Unable to parse location-entry in ClusterBuster-format: '"+line+"'", count);
                try {
                  currentClusterStart=Integer.parseInt(elements[1]);    
                  currentClusterStart--; // the positions are 1-indexed
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric start position in ClusterBuster-format: '"+line+"'", count);}
                try {
                  currentClusterEnd=Integer.parseInt(elements[3]);
                  currentClusterEnd--; // the positions are 1-indexed
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric end position in ClusterBuster-format: '"+line+"'", count);}
            } else if (line.startsWith("Score:")) {
                String[] elements=line.split(" ");
                if (elements.length!=2) throw new ParseError("Unable to parse score-entry in ClusterBuster-format: '"+line+"'");
                try {
                  currentClusterScore=Double.parseDouble(elements[1]);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric score-value in ClusterBuster-format: '"+line+"'", count);}
            } else if (line.indexOf('\t')>=0) { // Single motif line (only these contains tabulated data)
                singlemotifsread=true;
                String[] elements=line.split("\t");
                if (elements.length!=6) throw new ParseError("Expected 6 columns of data for single motifs in ClusterBuster-format: '"+line+"'", count);
                String motifname=elements[0];
                int motifstart=0;
                int motifend=0;
                double motifscore=0;
                try {
                  motifstart=Integer.parseInt(elements[1]);
                  motifstart--; // the positions are 1-indexed
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric start position for motif in ClusterBuster-format: '"+line+"'", count);}
                try {
                  motifend=Integer.parseInt(elements[2]);
                  motifend--;  // the positions are 1-indexed
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric end position for motif in ClusterBuster-format: '"+line+"'", count);}
                try {
                  motifscore=Double.parseDouble(elements[4]);
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected numeric score for motif in ClusterBuster-format: '"+line+"'", count);}
                int orientation=(elements[3].equals("-"))?ModuleCRM.REVERSE:ModuleCRM.DIRECT;
                Region motifregion=new Region(null, motifstart, motifend, motifname, motifscore, orientation);
                singlemotifs.add(motifregion);
            } else if (line.trim().isEmpty()) { // this means end of section
                if (singlemotifsread) { // end of cluster -> register this cluster as module!
                    registerCluster(target, sequencename, clustersinsequence, currentClusterStart, currentClusterEnd, currentClusterScore, singlemotifs);
                }
                singlemotifsread=false;
            } else if (line.matches("^[acgtACGTnN]+$")) { // this is the "module" string
               singlemotifsread=true; // just in case there are no single motif reports
            }
        } // end for each line
        return target;
    }

    /** Adds a cluster to the target Data object either as ModuleCRM or as a site */
    @SuppressWarnings("unchecked")
    private void registerCluster(Data target, String sequencename, int clusterIndex, int start, int end, double score, ArrayList<Region> singlemotifs) {
        String moduleName=sequencename+"_"+clusterIndex;
        //engine.logMessage("Register cluster "+moduleName+" to "+target.getTypeDescription()+"  location="+start+"-"+end+", score="+score+"  motifs="+singlemotifs.size());
        if (target instanceof ModuleCollection) { // register ModuleCRM
            ModuleCRM cisRegModule=new ModuleCRM(moduleName);
            cisRegModule.setMaxLength(end-start+1);
            cisRegModule.setOrdered(false);
            if (singlemotifs!=null) {
                for (Region motif:singlemotifs) {
                    String motifname=motif.getType();
                    ModuleMotif mm=cisRegModule.getModuleMotif(motifname);
                    if (mm==null) { // register new module motif for this motif
                        MotifCollection collection=new MotifCollection(moduleName+"_"+motifname);
                        collection.addMotifName(motifname);
                        cisRegModule.addModuleMotif(motifname, collection, ModuleCRM.INDETERMINED);
                    }
                }
            }
            ((ModuleCollection)target).addModuleToPayload(cisRegModule);
        } else { // register ModuleCRM site
            RegionSequenceData regionsequence=(RegionSequenceData)((RegionDataset)target).getSequenceByName(sequencename);
            Region moduleRegion=new Region(regionsequence, start, end, moduleName, score, ModuleCRM.DIRECT);
            if (singlemotifs!=null) {
                for (Region motif:singlemotifs) {
                    motif.setParent(regionsequence);
                    Object motifregions=moduleRegion.getProperty(motif.getType());
                    if (motifregions==null) { // no regions registered for this motif
                        moduleRegion.setProperty(motif.getType(), motif);
                    } else if (motifregions instanceof Region) { // this motif has one registered region before
                        ArrayList<Region> multipleregions=new ArrayList<Region>();
                        multipleregions.add((Region)motifregions);
                        multipleregions.add(motif);
                    } else if (motifregions instanceof ArrayList) { // this region has multiple registered regions from before
                        ((ArrayList)motifregions).add(motif);
                    }
                }
            }
            regionsequence.addRegion(moduleRegion);
        }
    }


}





