/*
 
 
 */

package motiflab.engine.task;

import java.util.ArrayList;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.FeatureSequenceData;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.RegionDataset;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.protocol.Protocol;
import motiflab.gui.MotifLabGUI;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class CropTask extends ExecutableTask {
    
    private MotifLabEngine engine;
    private Object cropStartObject=0;
    private Object cropEndObject=0;
    private String orientation=null;
    private RegionDataset cropToRegions=null;

    public CropTask(MotifLabEngine engine, RegionDataset cropToRegions, Object cropStart, Object cropEnd, String orientation) {
        super("Crop sequences");
        this.engine=engine;
        this.cropToRegions=cropToRegions;
        this.cropStartObject=cropStart;
        this.cropEndObject=cropEnd;
        this.orientation=orientation;
    }


    @Override
    public void run() throws InterruptedException, Exception {
        boolean done=false;
        setProgress(1);
        setStatus(RUNNING);
        if (undoMonitor!=null) undoMonitor.register();
        while (!done){
          try {
            execute();
            done=true;
          } catch (InterruptedException e) { // task aborted by the user
              if (undoMonitor!=null) undoMonitor.deregister(false);
              setStatus(ABORTED);
              throw e;
          } catch (Exception e) { // other errors
              if (undoMonitor!=null) undoMonitor.deregister(false);
              setStatus(ERROR);
              throw e;
          }
        }
        setProgress(100);
        if (undoMonitor!=null) undoMonitor.deregister(true);
        setStatus(DONE);
        setStatusMessage(null);
    }

    private void execute() throws InterruptedException, ExecutionError {
        try {           
                ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
                ArrayList<Sequence> clonedsequences=new ArrayList<Sequence>(sequences.size());
                ArrayList<Data> features=engine.getAllDataItemsOfType(FeatureDataset.class);
                ArrayList<FeatureDataset> clonedfeatures=new ArrayList<FeatureDataset>(features.size());
                for (int i=0;i<features.size();i++) { // clone the feature dataset. The clones are registered with the UndoManager to represent to original sequences                    
                    FeatureDataset feature=(FeatureDataset)features.get(i);                   
                    FeatureDataset clonedfeature=(FeatureDataset)feature.clone();
                    clonedfeatures.add(clonedfeature);
                }
                for (int i=0;i<sequences.size();i++) {
                   clonedsequences.add((Sequence)sequences.get(i).clone()); 
                }
                int cropped=0;
                int i=0;
                for (Data data:clonedsequences) {  
                    setProgress(i,sequences.size()*2); // *2 is so that this part will end at 50%
                    i++;
                    Sequence sequence=(Sequence)data;
                    int[] newGenomicRange=getNewGenomicCoordinates(sequence);
                    if (newGenomicRange==null) {
                        engine.logMessage("Unable to crop "+sequence.getName()+" (new sequence will be empty)");                        
                        continue;
                    }
                    if (newGenomicRange[0]==sequence.getRegionStart() && newGenomicRange[1]==sequence.getRegionEnd()) continue; // no cropping
                    int[] newRelativeRange=new int[]{newGenomicRange[0]-sequence.getRegionStart(),newGenomicRange[1]-sequence.getRegionStart()};
                    // update sequences
                    //gui.logMessage(sequence.getName()+"  genomic["+newGenomicRange[0]+"-"+newGenomicRange[1]+"]   relative["+newRelativeRange[0]+"-"+newRelativeRange[1]+"]  size="+(newRelativeRange[1]-newRelativeRange[0]+1));
                    if (newRelativeRange[0]>newRelativeRange[1]) {
                        engine.logMessage("Unable to crop "+sequence.getName()+" (new length is negative)");                        
                        continue;                        
                    }               
                    sequence.setRegionStart(newGenomicRange[0]);
                    sequence.setRegionEnd(newGenomicRange[1]);
                    cropped++;
                    for (Data feature:clonedfeatures) { // update features for this sequence
                        FeatureSequenceData featureseq=((FeatureDataset)feature).getSequenceByName(sequence.getName());
                        try {
                            featureseq.cropTo(newRelativeRange[0], newRelativeRange[1]);
                        } catch (Exception e) {
                            engine.logMessage(sequence.getName()+":"+feature.getName()+" => "+e.getMessage()+",  ["+newRelativeRange[0]+"-"+newRelativeRange[1]+"]");
                        }
                        if (feature instanceof NumericDataset) ((NumericDataset)feature).updateAllowedMinMaxValuesFromData();
                        else if (feature instanceof RegionDataset) ((RegionDataset)feature).updateMaxScoreValueFromData();
                    }                                                     
                }    
                int totaldata=sequences.size()+features.size();
                int counter=0;
                if (cropped>0) {
                    for (i=0;i<clonedsequences.size();i++) {
                        if (i%100==0) {
                            checkExecutionLock(); // checks to see if this task should suspend execution
                            if (Thread.interrupted() || getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            Thread.yield();
                            setProgress(50+(int)((double)counter/(double)totaldata*50.0));
                        }
                        engine.updateDataItem(clonedsequences.get(i));
                        counter++;
                    }  
                    for (i=0;i<clonedfeatures.size();i++) {
                        if (i%2==0) {
                            checkExecutionLock(); // checks to see if this task should suspend execution
                            if (Thread.interrupted() || getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
                            Thread.yield();
                            setProgress(50+(int)((double)counter/(double)totaldata*50.0));
                        }
                        engine.updateDataItem(clonedfeatures.get(i));
                        counter++;                    
                    }      
                } // end: if (cropped>0)
        }  catch (ClassCastException c) {
            throw new ExecutionError(c.getMessage());
        } catch (ExecutionError e) {
            throw e;
        }
    }

    @Override
    public String getCommandString(Protocol protocol) {
        return "#crop";
    }

    @Override
    public void purgeReferences() {

    }

    private int[] getNewGenomicCoordinates(Sequence sequence) {
        int[] result=new int[]{sequence.getRegionStart(),sequence.getRegionEnd()};
        //gui.logMessage(sequence.getName()+":pre:"+result[0]+"-"+result[1]);
        if (cropToRegions!=null) {
            RegionSequenceData seqTrack=(RegionSequenceData)cropToRegions.getSequenceByName(sequence.getName());
            int[] minmax=seqTrack.getMinMaxPositionsOfAllRegions();
            if (minmax!=null) {
                result[0]=sequence.getRegionStart()+minmax[0];
                result[1]=sequence.getRegionStart()+minmax[1];
                //engine.logMessage(sequence.getName()+"  min="+minmax[0]+"  max="+minmax[1]);            
            } else return null;
            if (result[0]<sequence.getRegionStart()) result[0]=sequence.getRegionStart(); // it is only allowed to make sequences smaller, not longer 
            if (result[1]>sequence.getRegionEnd()) result[1]=sequence.getRegionEnd(); // it is only allowed to make sequences smaller, not longer   
            return result;
        } else {
            int cropStart=0;
            int cropEnd=0;
            if (cropStartObject instanceof SequenceNumericMap) cropStart=((SequenceNumericMap)cropStartObject).getValue(sequence.getSequenceName()).intValue();
            else if (cropStartObject instanceof NumericVariable) cropStart=((NumericVariable)cropStartObject).getValue().intValue();
            else if (cropStartObject instanceof Number) cropStart=((Number)cropStartObject).intValue();
            if (cropEndObject instanceof SequenceNumericMap) cropEnd=((SequenceNumericMap)cropEndObject).getValue(sequence.getSequenceName()).intValue();
            else if (cropEndObject instanceof NumericVariable) cropEnd=((NumericVariable)cropEndObject).getValue().intValue();
            else if (cropEndObject instanceof Number) cropEnd=((Number)cropEndObject).intValue();
            
            if (cropStart+cropEnd>sequence.getSize()) {                
                return null;
            } // cropping too much... do nothing
            boolean relative=orientation.equals("Relative orientation");
            if (relative && sequence.getStrandOrientation()==Sequence.REVERSE) {
               result[0]=result[0]+cropEnd;
               result[1]=result[1]-cropStart;                
            } else {
               result[0]=result[0]+cropStart;
               result[1]=result[1]-cropEnd;
            }
            //gui.logMessage(sequence.getName()+":post:"+result[0]+"-"+result[1]);
            return result;
        }
    }    
    
    @Override
    public boolean shouldBlockGUI() {
        return true;
    }
    
    @Override
    public boolean turnOffGUInotifications() {
        return true;
    }    
    
    // -- The two methods below are actually not used anywhere since I don't override
    //    shouldRunPreprocessing() and shouldRunPostprocessing to return TRUE.
    //    But it seems that the visualization works just fine anyway
    
    @Override
    public void guiClientPreprocess(){
        ArrayList<String> names=engine.getNamesForAllDataItemsOfType(Sequence.class);
        String[] nameslist=new String[names.size()];
        nameslist=names.toArray(nameslist);      
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();                 
        settings.setSequenceVisible(nameslist, false);         
    }
    
    @Override
    public void guiClientPostprocess(){
        ArrayList<String> names=engine.getNamesForAllDataItemsOfType(Sequence.class);
        String[] nameslist=new String[names.size()];
        nameslist=names.toArray(nameslist);      
        ((MotifLabGUI)engine.getClient()).getVisualizationPanel().clearCachedVisualizers();
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();                 
        settings.setSequenceVisible(nameslist, true);          
    }     
    
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[CropTask] ===== "+getTaskName()+" =====  (Line: "+getLineNumber()+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);
        if (verbosity>=3) {
            motiflab.engine.protocol.StandardProtocol protocol=new motiflab.engine.protocol.StandardProtocol(MotifLabEngine.getEngine());
            MotifLabEngine.debugOutput(" Command: "+getCommandString(protocol),indentLevel);
        }
        if (verbosity>1) MotifLabEngine.debugOutput("-------------------------------------------[End CropTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }     
    
}
