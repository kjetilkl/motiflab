/*
 
 
 */

package motiflab.engine.task;

import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.FeatureDataset;
import motiflab.engine.data.Sequence;
import motiflab.engine.protocol.Protocol;

/**
 *
 * @author kjetikl
 */
public class AddSequencesTask extends ExecutableTask {
    private static final int FORMAT_MANUAL_ENTRY=1;
    private static final int FORMAT_SEQUENCES=2;
    
    private MotifLabEngine engine;
    private int format=0;
    private Object[][] sequenceData=null;
    private Sequence[] sequences=null;
    private String filename=null;
    private String dataformatname=null;
    private ParameterSettings dataformatsettings=null;

    /**
     * Creates a new AddSequencesTask object instantiated with the given data
     * originating from manual entry of sequence and gene data. In the data object
     * the number of rows (1st dimension) is the number of sequences while the
     * columns (2nd dimension) specifies
     *      1) Sequence name (String)
     *      2) Organism (Integer) NCBI taxonomy ID
     *      3) Genome build
     *      4) Chromosome (String)
     *      5) Sequence start (Integer)
     *      6) Sequence end (Integer)
     *      7) Gene name (String)
     *      8) Gene start (Integer)
     *      9) Gene end (Integer)
     *      10) Gene orientation (Integer)
     * 
     * NOTE: This method is kept for historic purposes and is deprecated. 
     * It is better to use: AddSequencesTask(MotifLabEngine engine, Sequence[] sequences) 
     * 
     * @param engine
     * @param data
     */
    @Deprecated
    public AddSequencesTask(MotifLabEngine engine, Object[][] sequenceSpecificationData) {
        super("Add Sequences");
        setTurnOffGUInotifications(true); // this will speed up the GUI initialization by just updating the GUI once after all sequences have been added
        this.engine=engine;
        this.format=FORMAT_MANUAL_ENTRY;
        this.sequenceData=sequenceSpecificationData;
        for (int i=0;i<sequenceSpecificationData.length;i++) {
            String seqName=(String)sequenceSpecificationData[i][0];
            addAffectedDataObject(seqName, Sequence.class);
        }
    }
    
    
    /**
     * Creates a new AddSequencesTask object instantiated with the given data
     * @param engine
     * @param data
     */    
    public AddSequencesTask(MotifLabEngine engine, Sequence[] sequences) {
        super("Add Sequences");
        setTurnOffGUInotifications(true);
        this.engine=engine;
        this.format=FORMAT_SEQUENCES;
        this.sequences=sequences;
        for (Sequence seq:sequences) {
            addAffectedDataObject(seq.getName(), Sequence.class);
        }
    }

    /** Sets a filename (if these sequences were loaded from file) */
    public void setFilename(String filename) {
        this.filename=filename;
    }

   /** Sets the dataformat used if these sequences were loaded from file */
    public void setDataFormat(String format) {
        this.dataformatname=format;
    }

   /** Sets the dataformat settings used if these sequences were loaded from file */
    public void setDataFormatSettings(ParameterSettings settings) {
        this.dataformatsettings=settings;
    }

   /** Returns the name (path) of the file these sequences were loaded from
    *  or NULL if the sequences were not obtained from a file
    */
    public String getFilename() {
        return filename;
    }

   /** Returns the name of the dataformat used if these sequences were loaded from file */
    public String getDataFormat() {
        return dataformatname;
    }

   /** Returns the dataformat settings used if these sequences were loaded from file */
    public ParameterSettings getDataFormatSettings() {
        return dataformatsettings;
    }

    @Override
    public void purgeReferences() {
        sequenceData=null;
        sequences=null;
        filename=null;
        dataformatname=null;
        dataformatsettings=null;
    }

    /**
     * Executes the operation associated with this task
     * Implements the Runnable interface
     */
    @Override
    public void run() throws InterruptedException, Exception {
        //System.err.println("Executing AddSequencesTask");//debug();
        boolean done=false;
        setStatus(RUNNING);  
        setProgress(5);
        if (engine.hasDataItemsOfType(FeatureDataset.class)) {
            setStatus(ERROR);
            throw new ExecutionError("New sequences can not be added after feature data has been loaded");            
        }          
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
              if (shouldRetry(e)) {
                  setRetrying(true);
              } else {
                  if (undoMonitor!=null) undoMonitor.deregister(false);
                  setStatus(ERROR);
                  throw e;
              }
          }            
        }
        setProgress(100);
        if (undoMonitor!=null) undoMonitor.deregister(true);        
        setStatus(DONE);
        setStatusMessage(null);
    }

    
    private boolean execute() throws Exception {
        int numberOfSequences=(format==FORMAT_SEQUENCES)?sequences.length:sequenceData.length;
        for (int i=0;i<numberOfSequences;i++) { // for each sequence 
            String sequenceName=(format==FORMAT_SEQUENCES)?((Sequence)sequences[i]).getSequenceName():(String)sequenceData[i][0];
            Sequence sequence=(format==FORMAT_SEQUENCES)?sequences[i]:getSequenceFromData(i);
            if (sequence==null) throw new ExecutionError("Unable to instantiate sequence '"+sequenceName+"'");
            setStatusMessage("Adding sequence '"+sequenceName+"' ("+(i+1)+"/"+numberOfSequences+")");
            setProgress(i+1, numberOfSequences);
            if (i%10==0) Thread.yield();
            checkExecutionLock(); // checks to see if this task should suspend execution
            if (Thread.interrupted() || getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            if (engine.dataExists(sequenceName, Sequence.class)) engine.logMessage("Warning: Adding new sequence with the same name as an existing sequence ("+sequenceName+"). The new sequence will replace the old");
            engine.storeDataItem(sequence);
            //System.err.println("Adding sequence '"+sequenceName+"' ("+(i+1)+"/"+numberOfSequences+")");
        }   
        return true;
    }
    
    @Deprecated
    private Sequence getSequenceFromData(int index) {
        return new Sequence((String)sequenceData[index][0], ((Integer)sequenceData[index][1]).intValue(), (String)sequenceData[index][2], (String)sequenceData[index][3], ((Integer)sequenceData[index][4]).intValue(), ((Integer)sequenceData[index][5]).intValue(), (String)sequenceData[index][6], ((Integer)sequenceData[index][7]), ((Integer)sequenceData[index][8]), ((Integer)sequenceData[index][9]).intValue());
    }
    

    @Override
    public String getCommandString(Protocol protocol) {
       if (protocol==null) return null;
       return protocol.getCommandString(this);
    }
    
    public int getNumberofSequencesToAdd() {
        if (format==FORMAT_SEQUENCES) {
            if (sequences==null) return 0;
            else return sequences.length;
        } else if (format==FORMAT_MANUAL_ENTRY) {
            if (sequenceData==null) return 0;
            else return sequenceData.length;
        }
        return 0;
    }


    public Sequence getSequence(int index) {
         if (format==FORMAT_SEQUENCES) {
            if (sequences==null) return null;
            else return sequences[index];
        } else if (format==FORMAT_MANUAL_ENTRY) {
            if (sequenceData==null) return null;
            else return getSequenceFromData(index);
        }  else return null;
    }
    
    /**
     * Returns a shallow clone of this AddSequencesTask object
     * @return 
     */
    @Override
    public AddSequencesTask clone() {
        AddSequencesTask copy=null;
        if (sequences!=null) copy=new AddSequencesTask(engine, sequences);
        else if (sequenceData!=null) copy=new AddSequencesTask(engine, sequenceData);
        copy.format=format;
        copy.filename=filename;
        copy.dataformatsettings=dataformatsettings;  
        copy.dataformatname=dataformatname;   
        return copy;
    }
    
    @Override
    public void debug(int verbosity, int indentLevel) {
        MotifLabEngine.debugOutput("[AddSequencesTask] ===== "+getTaskName()+" =====  (Line: "+getLineNumber()+")",indentLevel);
        if (verbosity>=2) MotifLabEngine.debugOutput(" Status: "+getStatus()+",  Status Message: "+getStatusMessage(),indentLevel);
        if (verbosity>=3) {
            motiflab.engine.protocol.StandardProtocol protocol=new motiflab.engine.protocol.StandardProtocol(MotifLabEngine.getEngine());
            MotifLabEngine.debugOutput(" Command: "+getCommandString(protocol),indentLevel);
        }
        if (verbosity>1) MotifLabEngine.debugOutput("-------------------------------------------[End AddSequencesTask]\n",indentLevel); // if verbosity==1 then output is a one-liner anyway
    }     
}
