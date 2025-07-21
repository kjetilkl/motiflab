/*
 
 
 */

package org.motiflab.engine.operations;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.FeatureSequenceData;
import org.motiflab.engine.data.ModuleCRM;
import org.motiflab.engine.data.ModuleCollection;
import org.motiflab.engine.data.ModuleMotif;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.external.ExternalProgram;
import org.motiflab.external.ModuleDiscovery;
import org.motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class Operation_moduleDiscovery extends Operation {
    private static final String name="moduleDiscovery";
    private static final String description="Performs de novo module discovery with a chosen algorithm";
    public static final String ALGORITHM="algorithm";
    public static final String PARAMETERS="parameters";
    public static final String MODULECOLLECTION="ModuleCollection";
    public static final String MODULEPREFIX="Modules prefix";
    public static final String ADDITIONAL_RESULTS="Additional results";    
    private Class[] datasourcePreferences=new Class[]{RegionDataset.class, DNASequenceDataset.class};

    @Override
    public String getOperationGroup() {
        return "Module";
    }

    @Override
    public Class[] getDataSourcePreferences() {
        return datasourcePreferences;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {return name;}

    public void resolveParameters(OperationTask task, ModuleDiscovery program) throws Exception {
        ParameterSettings parameterSettings=(ParameterSettings)task.getParameter(Operation_moduleDiscovery.PARAMETERS);
        if (parameterSettings==null) throw new ExecutionError("Missing parameters for module discovery algorithm");
        Parameter[] arguments=program.getParameters();
        parameterSettings.applyConditions(arguments); // triggers actions that might change some parameter settings depending on the values of others          
        for (int i=0;i<arguments.length;i++) {
            String parameterName=arguments[i].getName();
            Object value=parameterSettings.getResolvedParameter(parameterName, program.getParameters(), engine);
            if (arguments[i].isRequired() && (value==null || value.toString().isEmpty())) throw new ExecutionError("Missing value for required parameter '"+parameterName+"'");
            task.setParameter(parameterName,value);
        }
        String subsetName=(String)task.getParameter(OperationTask.SEQUENCE_COLLECTION_NAME);
        if (subsetName==null || subsetName.isEmpty()) subsetName=engine.getDefaultSequenceCollectionName();
        Data seqcol=engine.getDataItem(subsetName);
        if (seqcol==null) throw new ExecutionError("No such collection: '"+subsetName+"'",task.getLineNumber());
        if (!(seqcol instanceof SequenceCollection)) throw new ExecutionError(subsetName+" is not a sequence collection",task.getLineNumber());
        SequenceCollection sequenceCollection=(SequenceCollection)seqcol;
        task.setParameter(OperationTask.SEQUENCE_COLLECTION, sequenceCollection);
        if (sequenceCollection.isEmpty()) throw new ExecutionError("Attempting to perform module discovery on empty Sequence Collection '"+sequenceCollection.getName()+"'",task.getLineNumber());
        //else if (sequenceCollection.size()==1) task.getEngine().logMessage("WARNING: Attempting to perform module discovery on Sequence Collection '"+sequenceCollection.getName()+"' containing only 1 sequence");
    }



    @Override
    public boolean execute(OperationTask task) throws Exception {
        if (engine.getDefaultSequenceCollection().isEmpty()) throw new ExecutionError("No sequences are selected");
        String algorithmName=(String)task.getParameter(Operation_moduleDiscovery.ALGORITHM);
        Object object=engine.getExternalProgram(algorithmName);
        if (object==null) throw new ExecutionError("Unknown algorithm "+algorithmName);
        if (!(object instanceof ModuleDiscovery)) throw new ExecutionError(algorithmName+" is not a module discovery algorithm");
        ModuleDiscovery program = (ModuleDiscovery)object;
        task.setStatusMessage("Executing operation: "+task.getOperationName());
        task.setProgress(5);
        String sourceName=task.getSourceDataName();
        String targetName=task.getTargetDataName();
        if ((targetName==null || targetName.isEmpty()) && program.returnsSiteResults())  throw new ExecutionError("Missing name for target data object",task.getLineNumber());
        String moduleCollectionName=(String)task.getParameter(Operation_moduleDiscovery.MODULECOLLECTION);
        if ((moduleCollectionName==null || moduleCollectionName.isEmpty()) && program.returnsModuleResults())  throw new ExecutionError("Missing name for returned module collection",task.getLineNumber());
        if (sourceName==null || sourceName.isEmpty()) throw new ExecutionError("Missing name for source data object",task.getLineNumber()); 
        Data sourceData=engine.getDataItem(sourceName);
        if (sourceData==null) throw new ExecutionError("Unknown data object '"+sourceName+"'",task.getLineNumber());
        if (!canUseAsSource(sourceData)) throw new ExecutionError("Operation 'moduleDiscovery' can not work on '"+sourceName+"'",task.getLineNumber());
        resolveParameters(task,program);
        RegionDataset targetData = performModuleDiscovery(program, (FeatureDataset)sourceData, targetName, moduleCollectionName, task);
        if (targetData!=null) {
            targetData.updateMaxScoreValueFromData();
            targetData.setModuleTrack(true);
        }
        ModuleCollection modulesData=(ModuleCollection)task.getParameter("Modules");
        task.setProgress(99);
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        try {
            if (targetData!=null) engine.updateDataItem(targetData);
            if (modulesData!=null) engine.updateDataItem(modulesData);
        } catch (ClassCastException ce) {
            throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());
        }        
        String additionalResults=(String)task.getParameter(Operation_moduleDiscovery.ADDITIONAL_RESULTS);
        // check if additional results objects need to be handled
        if (additionalResults!=null && !additionalResults.isEmpty()) {
            String[] resultName=additionalResults.split("\\s*,\\s*");
            ArrayList<Parameter> additional=program.getAdditionalResultsParameters();
            if (resultName.length!=additional.size()) throw new ExecutionError("Specified number of result parameters ("+resultName.length+") does not match expected number ("+additional.size()+")");
            for (int i=0;i<resultName.length;i++) {
                String internalName=additional.get(i).getName(); 
                String externalName=resultName[i].trim();
                if (externalName.isEmpty()) throw new ExecutionError("Missing name for result data object");
                Data resultData=(Data)task.getParameter(internalName);
                resultData.rename(externalName);
                try {engine.updateDataItem(resultData);} catch (ClassCastException ce) {throw new ExecutionError("Incompatible assignment: "+ce.getMessage(),task.getLineNumber());}
            }
        }
        task.setProgress(100);        
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();        
        return true;
    }

    private RegionDataset performModuleDiscovery(ModuleDiscovery program, FeatureDataset sourceData, String targetName,String moduleCollectionName, OperationTask task) throws Exception {
        task.setParameter(ExternalProgram.SOURCES,new Data[]{sourceData});
        program.execute(task);
        String moduleprefix=(String)task.getParameter(MODULEPREFIX);
        if (moduleprefix==null) moduleprefix="MOD";
        ModuleCollection modulesData=(ModuleCollection)task.getParameter("Modules");
        RegionDataset targetData=(RegionDataset)task.getParameter("Result");
        if (modulesData==null && program.returnsModuleResults()) throw new ExecutionError("No module collection returned");        
        if (targetData==null && program.returnsSiteResults()) throw new ExecutionError("No module track returned");          
        if (modulesData!=null) {
            modulesData.setName(moduleCollectionName);            
            int numberofmodulesfound=modulesData.getNumberofModulesInPayload();
            String[] newnames=task.getEngine().getNextAvailableDataNames(moduleprefix, 5, numberofmodulesfound);
            HashMap<String,String> newnamesmap=new HashMap<String,String>(numberofmodulesfound);
            for (int i=0;i<numberofmodulesfound;i++) {            
                String oldname=modulesData.renameModuleInPayload(i, newnames[i]);
                newnamesmap.put(oldname,newnames[i]);
            }
            // Some module discovery programs can find their own single motifs and return these
            // as part of the payload in the ModuleCollection
            int numberofmotifsfound=modulesData.getNumberofMotifsInPayload();
            if (numberofmotifsfound>0) {
                String motifsprefix=moduleprefix+"Motif";
                String[] newmotifnames=task.getEngine().getNextAvailableDataNames(motifsprefix, 5, numberofmotifsfound);
                for (int i=0;i<numberofmotifsfound;i++) {
                   String oldname=modulesData.renameMotifInPayload(i, newmotifnames[i]);
                   newnamesmap.put(oldname,newmotifnames[i]);
                }
                modulesData.renameSingleMotifsInPayloadModules(newnamesmap); // renames the motifs in the modules to avoid potential conflicts if the same program is run twice
            }
            if (targetData!=null) annotateModules(modulesData, targetData, sourceData, newnamesmap, numberofmotifsfound>0);    
        } else if (targetData!=null) {
            ArrayList<String> types=new ArrayList<String>(targetData.getAllRegionTypes());
            int numberofmodulesfound=types.size();
            String[] newnames=task.getEngine().getNextAvailableDataNames(moduleprefix, 5, numberofmodulesfound);
            HashMap<String,String> newnamesmap=new HashMap<String,String>(numberofmodulesfound);
            for (int i=0;i<numberofmodulesfound;i++) {
                String oldname=types.get(i);
                newnamesmap.put(oldname,newnames[i]);
            }                  
            annotateModules(modulesData, targetData, sourceData, newnamesmap, false);            
        }
        if (targetData!=null) targetData.setName(targetName);  
        return targetData;
    }


    /** Renames the regions (and modulemotif property of the region) if necessary
     *  to correspond with the renamed modules/motifs and also selects consistent
     *  colors for the same modulemotifs between modules
     */
    private void annotateModules(ModuleCollection modules, RegionDataset targetData, FeatureDataset sourceData, HashMap<String,String> newnames, boolean renameSingleMotifs) {
        ArrayList<FeatureSequenceData> list=targetData.getAllSequences();
        for (FeatureSequenceData sequenceData:list) {
            RegionSequenceData regionSequenceData=(RegionSequenceData)sequenceData;
            String sequenceName=regionSequenceData.getName();
            ArrayList<Region> regions=regionSequenceData.getAllRegions();
            int sequenceOrientation=sequenceData.getStrandOrientation();
            for (Region moduleregion:regions) {
                String oldtype=moduleregion.getType();
                String newtype=newnames.get(oldtype);
                if (newtype==null) newtype=oldtype; // this should not happen
                moduleregion.setType(newtype);
                if (renameSingleMotifs) { // rename references to single motifs in the module regions
                    ModuleCRM cisRegModule=modules.getModuleFromPayload(newtype);
                    if (cisRegModule!=null) {
                        for (ModuleMotif mm:cisRegModule.getModuleMotifs()) {
                            String modulemotifname=mm.getRepresentativeName();
                            String oldmotifname=(String)moduleregion.getProperty(modulemotifname);
                            if (oldmotifname!=null) { // region has this motif
                                String newmotifname=newnames.get(oldmotifname);
                                if (newmotifname==null) moduleregion.setProperty(modulemotifname, newmotifname);
                            }
                        } // end for each ModuleMotif
                    }
                } // end rename single motifs
                for (Region motifregion:moduleregion.getNestedRegions(false)) {
                    if (motifregion.getProperty("sequence")==null) { // try to set the sequence property if it isn't set already
                       if (sourceData instanceof DNASequenceDataset) {
                           DNASequenceData dnaSeq=(DNASequenceData)sourceData.getSequenceByName(sequenceName);
                           int start=motifregion.getGenomicStart();
                           int end=motifregion.getGenomicEnd();
                           int orientation=motifregion.getOrientation();
                           char[] site=(char[])dnaSeq.getValueInGenomicInterval(start, end);
                           if (site!=null) {
                              if (orientation==Region.DIRECT) motifregion.setProperty("sequence", new String(site));
                              else motifregion.setProperty("sequence", new String(MotifLabEngine.reverseSequence(site)));
                           }
                       } else if (sourceData instanceof RegionDataset) {
                            RegionSequenceData sourceSeq=(RegionSequenceData)sourceData.getSequenceByName(sequenceName);
                            ArrayList<Region> similarregions=sourceSeq.getSimilarRegion(motifregion);
                            if (!similarregions.isEmpty()) {
                                motifregion.setProperty("sequence", similarregions.get(0).getProperty("sequence")); // use first in list (there should probably not be more than one anyway)
                            }
                       }
                    }
                } // end for each modulemotif region
            } // end for each module region
        } // end for each sequence
        // now assign colors

        if (modules!=null && modules.hasPayload()) {
            HashMap<String,Color> assignedColors=new HashMap<String, Color>();        
            for (Data data:modules.getPayload()) {
                if (data instanceof ModuleCRM) {
                    ModuleCRM cisRegModule=(ModuleCRM)data;
                    String modulename=cisRegModule.getName();
                    for (ModuleMotif mm:cisRegModule.getModuleMotifs()) {
                        String modulemotifname=mm.getRepresentativeName();
                        String originalMMname=getModuleMotifNamePrefix(modulemotifname); // this is an attempt to give similar modulemotifs (that have been fitted with "_2" etc because the names have to be unique) the same color
                        assignModuleMotifColor(modulename,originalMMname,modulemotifname,assignedColors);
                    }
                }
            }
        }
    }

    private String getModuleMotifNamePrefix(String name) {
        if (name.matches("^.+_\\d+$")) { // name ends with _X where X is a number
            return name.substring(name.lastIndexOf('_'));
        } else return name;
    }
    
    private void assignModuleMotifColor(String modulename, String originalMMname, String modulemotifname, HashMap<String,Color> assignedColors) {
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();
        String featureName=modulename+"."+modulemotifname;
        Color assigned=assignedColors.get(originalMMname); // use same color for same modulemotifs between modules
        if (assigned==null) {          
            if (settings.hasFeatureColor(originalMMname)) { // possibly a motif
               assigned=settings.getFeatureColor(originalMMname);                  
            } else {
               assigned=settings.getFeatureColor(featureName); // let the VizSettings choose a new color for the modulemotif within the module                  
            }
            assignedColors.put(originalMMname, assigned);
        } 
        settings.setFeatureColor(featureName, assigned, false);
    }

}