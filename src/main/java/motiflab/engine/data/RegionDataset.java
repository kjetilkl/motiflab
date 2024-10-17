/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.task.OperationTask;


/**
 *
 * @author kjetikl
 */
public class RegionDataset extends FeatureDataset implements Cloneable {
    private static String typedescription="Region Dataset";
    private double maxScore=0; // this is mainly used by visualization to scale height of regions. The "smallest score" in the eyes of the GUI is always 0 since it does not explicitly draw negative values.
    private boolean ismotiftrack=false; // these flags should not be true at the same time
    private boolean ismoduletrack=false;
    private boolean isnestedtrack=false; // if the track contains "Regions within Regions" that are not modules 


  
    public RegionDataset(String datasetName) {
        super(datasetName);
    }
    
    @Override
    public void setupDefaultDataset(ArrayList<Sequence> sequences) {
        for (Sequence seq:sequences) {
            if (getSequenceByName(seq.getName())==null) {               
                RegionSequenceData seqdata=new RegionSequenceData(seq);
                addSequence(seqdata);
            }
        }
    }       
    
   /**
     * Returns the maximum score value for this dataset
     * @return the maximum score value
     */       
    public Double getMaxScoreValue() {
        return maxScore;
    }

   /**
     * Sets the maximum score value for this dataset
     * @param max The maximum score value for this dataset
     */
    public void setMaxScoreValue(double max) {
        this.maxScore=max;
    } 

    /** Updates the max score value for this dataset based on the data for the current sequences. */
    public double updateMaxScoreValueFromData() {
        double max=-Double.MAX_VALUE;
        ArrayList<FeatureSequenceData> sequences=getAllSequences();
        for (FeatureSequenceData seq:sequences) {
            double seqmax=((RegionSequenceData)seq).updateMaxScoreValueFromData(); //
            if (seqmax>max) max=seqmax;
        }
        setMaxScoreValue(max);
        return max;
    }

    /**
     * Returns the actual min and max values for a given numeric region property
     * in this dataset based on a search through all the sequences.
     * @propery The numeric region property to get values for. Defaults to "score" if null.
     */
    public double[] getMinMaxValuesFromData(String property) {
        double min=Double.MAX_VALUE;
        double max=-Double.MAX_VALUE;
        ArrayList<FeatureSequenceData> sequences=getAllSequences();
        for (FeatureSequenceData seq:sequences) {
            double[] seqMinMax=((RegionSequenceData)seq).getMinMaxFromData(property);
            if (seqMinMax[0]<min) min=seqMinMax[0];
            if (seqMinMax[1]>max) max=seqMinMax[1];            
        }
        return new double[]{min,max};
    }


    /**
     * Specifies whether this RegionDataset represents a "motif track" where the region typenames
     * corresponds to motifs registered with the engine
     * @param ismotiftrack
     */
    public void setMotifTrack(boolean ismotiftrack) {
        this.ismotiftrack=ismotiftrack;
        if (ismotiftrack) {
            ismoduletrack=false;
            isnestedtrack=false;
        }
    }

    /** Returns TRUE if this RegionDataset contains motifs (or transcription factors binding sites),
     *  in which case the typenames of the regions should match the names of motifs registered with the engine
     */
    public boolean isMotifTrack() {
        return ismotiftrack;
    }


    /**
     * Specifies whether this RegionDataset represents a "module track" where the region typenames
     * corresponds to modules registered with the engine
     * @param ismoduletrack
     */
    public void setModuleTrack(boolean ismoduletrack) {
        this.ismoduletrack=ismoduletrack;
        if (ismoduletrack) {
            ismotiftrack=false;
            isnestedtrack=false;
        }
    }

    /** Returns TRUE if this RegionDataset contains modules (or composite TF binding sites)
     *  in which case the typenames of the regions should match the names of modules registered with the engine
     */
    public boolean isModuleTrack() {
        return ismoduletrack;
    }
    
    /**
     * Specifies whether this RegionDataset contains "nested regions"
     * (which are sort of like modules except that they are not associated with module objects)
     * @param isnestedtrack
     */
    public void setNestedTrack(boolean isnestedtrack) {
        this.isnestedtrack=isnestedtrack;
        if (isnestedtrack) {
            ismotiftrack=false;
            ismoduletrack=false;
        }
    }

    /** Returns TRUE if this RegionDataset contains "nested regions" (that are not modules)
      */
    public boolean isNestedTrack() {
        return isnestedtrack;
    }    

    
    /** 
     * Returns a deep copy of this object 
     */
    @Override
    public RegionDataset clone() {
        RegionDataset newdataset=new RegionDataset(datasetName); // creates a new dataset with the same name and empty storage
        newdataset.datasetDescription=this.datasetDescription;
        int size=getNumberofSequences();
        newdataset.maxScore=this.maxScore;
        newdataset.ismotiftrack=this.ismotiftrack;
        newdataset.ismoduletrack=this.ismoduletrack;
        newdataset.isnestedtrack=this.isnestedtrack;
        for (int i=0;i<size;i++) {
            RegionSequenceData seq=(RegionSequenceData)getSequenceByIndex(i);
            if (seq==null) throw new ConcurrentModificationException("Sequence was deleted before the dataset could be cloned"); // not sure what else to do
            newdataset.addSequence((RegionSequenceData)seq.clone());
        }
        return newdataset;
    }  
    
   @Override
    public void importData(Data source) throws ClassCastException {
        super.importData(source); // this will import the individual sequences
        this.maxScore=((RegionDataset)source).maxScore;
        this.ismotiftrack=((RegionDataset)source).ismotiftrack;
        this.ismoduletrack=((RegionDataset)source).ismoduletrack; 
        this.isnestedtrack=((RegionDataset)source).isnestedtrack; 
        //notifyListenersOfDataUpdate(); 
    }   
   
    /**
    * Returns a set of all region types present in this Region Dataset
    * @return 
    */
    public HashSet<String> getAllRegionTypes() {
        HashSet<String> alltypes=new HashSet<String>();
        for (int i=0;i<getNumberofSequences();i++) {
            RegionSequenceData seq=(RegionSequenceData)getSequenceByIndex(i);
            alltypes.addAll(seq.getRegionTypes());
        }
        return alltypes;
    }
    
    /**
     * clears all regions in this dataset
     * 
     */      
    public void clearRegions() {
        for (int i=0;i<getNumberofSequences();i++) {
            RegionSequenceData seq=(RegionSequenceData)getSequenceByIndex(i);
            seq.clearRegions();
        }
    }
   
    public int getNumberOfRegions() {
        int sum=0;
        for (int i=0;i<getNumberofSequences();i++) {
            RegionSequenceData seq=(RegionSequenceData)getSequenceByIndex(i);
            sum+=seq.getNumberOfRegions();
        }
        return sum;
    }    

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof RegionDataset)) return false;
        RegionDataset other=(RegionDataset)data;
        if (this.ismotiftrack!=other.ismotiftrack) return false;
        if (this.ismoduletrack!=other.ismotiftrack) return false;
        if (this.isnestedtrack!=other.isnestedtrack) return false;
        for (FeatureSequenceData sequence:getAllSequences()) {
            if (!sequence.containsSameData(other.getSequenceByName(sequence.getName()))) return false;
        }
        return false;
    }

    /**
     * Returns a copy of this dataset where the regions have been 'flattened',
     * i.e. overlapping regions (or regions lying back-to-back) are merged into a single contiguous region.
     * The new regions do not retain their type, score or orientation properties. Also, regions that
     * lie partially outside the sequence boundaries are cropped so that the start or end of the region
     * coincides with the start or end of the sequence.
     * This method should preferably only be used to create temporary objects used in calculations where
     * it is necessary to know which part of a sequence are covered by regions, but where regions should not
     * overlap each other
     * @return
     */
    public RegionDataset flatten() {
        RegionDataset flattened=this.clone();
        int size=getNumberofSequences();
        for (int i=0;i<size;i++) {
            RegionSequenceData seq=(RegionSequenceData)flattened.getSequenceByIndex(i);
            seq.flattenRegions();
        }
        return flattened;
    }


    /**
     * Returns a HashMap describing the names and class types of all user-defined properties that are defined for regions in this sequence.
     * If the type class of the value for a given property is the same across all regions, this class will be used as the type.
     * However, if the classes of the values varies, a "greatest common" class will be determined based on the following simple rule:
     * If all values are variations of Numbers (e.g. Integer or Doubles) the class will be Number, else the returned class will be String (since all values can be converted into Strings with toString()).
     * @param map If provided, the properties will be added to this map. Properties already existing in this map. If map is NULL a new map will be returned
     * @return 
     */
    public HashMap<String,Class> getUserDefinedProperties(HashMap<String,Class> map) {
        if (map==null) map=new HashMap<String,Class>();
        ArrayList<FeatureSequenceData> sequences=getAllSequences();
        for (FeatureSequenceData seq:sequences) {
            ((RegionSequenceData)seq).getUserDefinedProperties(map);
        }           
        return map;
    }


    @Override
    public String[] getResultVariables() {
        if (isModuleTrack()) return new String[]{"TFBS","start","end","relativeStart","relativeEnd","regionStart","regionEnd","edges","center","types","counts","counts of X"};
        else if (isNestedTrack()) return new String[]{"nested regions","nested numbered regions","top level regions","start","end","relativeStart","relativeEnd","regionStart","regionEnd","edges","center","types","counts","counts of X"};
        else return new String[]{"start","end","relativeStart","relativeEnd","regionStart","regionEnd","edges","center","types","counts","counts of X"};
    }
    
    @Override
    public boolean hasResult(String variablename) {
        if (variablename.startsWith("counts of ")) return true;
        else return super.hasResult(variablename);
    }    

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (getResultType(variablename)==null) throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
        if (isModuleTrack() && variablename.equals("TFBS")) {
             RegionDataset newdata=new RegionDataset("Result");
             for (FeatureSequenceData seq:getAllSequences()) {
                 newdata.addSequence( ((RegionSequenceData)seq).getNestedRegions() );
             }
             newdata.updateMaxScoreValueFromData();
             newdata.setMotifTrack(true);
             return newdata;
        } else if (isNestedTrack() && (variablename.equals("nested regions") || variablename.equals("nested numbered regions") || variablename.equals("numbered nested regions")))  {
             RegionDataset newdata=new RegionDataset("Result");
             for (FeatureSequenceData seq:getAllSequences()) {
                 if (variablename.contains("numbered")) newdata.addSequence( ((RegionSequenceData)seq).getNumberedNestedRegions() );
                 else newdata.addSequence( ((RegionSequenceData)seq).getNestedRegions() );
             }
             newdata.updateMaxScoreValueFromData();
             return newdata;
        } else if (isNestedTrack() && variablename.equals("top level regions")) {
             RegionDataset newdata=new RegionDataset("Result");
             for (FeatureSequenceData seq:getAllSequences()) {
                 newdata.addSequence( ((RegionSequenceData)seq).getTopLevelRegions());
             }
             newdata.updateMaxScoreValueFromData();
             return newdata;
        } else if (variablename.equals("types")) {
             HashSet<String> types=new HashSet<String>();
             for (FeatureSequenceData seq:getAllSequences()) {
                 types.addAll(((RegionSequenceData)seq).getRegionTypes());
             }
             TextVariable typesCollection=new TextVariable("types");
             ArrayList<String> sortedList=new ArrayList<String>(types.size());
             sortedList.addAll(types);
             Collections.sort(sortedList);
             for (String string:sortedList) typesCollection.append(string);
             return typesCollection;
        } else if (variablename.startsWith("counts")) {
             String collection=null;
             ArrayList<String> types=null;
             if (variablename.startsWith("counts of")) collection=variablename.substring("counts of".length()).trim();
             if (collection!=null) {
                if (collection.startsWith("\"") && collection.endsWith("\"")) {
                   collection=collection.substring(1,collection.length()-1);
                   types=new ArrayList<String>(1);
                   types.add(collection);
                } else {
                    Data data=engine.getDataItem(collection);
                    if (data==null) throw new ExecutionError("Unknown data object: "+collection);
                    if (data instanceof MotifCollection) types=((MotifCollection)data).getAllMotifNames();
                    else if (data instanceof ModuleCollection) types=((ModuleCollection)data).getAllModuleNames();
                    else if (data instanceof TextVariable) types=((TextVariable)data).getAllStrings();
                    else if (data instanceof Motif || data instanceof ModuleCRM) {
                       types=new ArrayList<String>(1);
                       types.add(collection); // this is the name of the motif or module
                    }
                    else throw new ExecutionError("Expected a Motif, Module, Collection, TextVariable or literal string in quotes. '"+collection+"' is a "+data.getDynamicType());
                }
             } else {
                 HashSet<String> typesset=new HashSet<String>();
                 for (FeatureSequenceData seq:getAllSequences()) {
                    typesset.addAll(((RegionSequenceData)seq).getRegionTypes());
                 }
                 types=new ArrayList<String>(typesset.size());
                 types.addAll(typesset);
             }          
             Collections.sort(types);
             HashMap<Integer, String> headers=new HashMap<Integer, String>(types.size());
             for (int i=0;i<types.size();i++) {
                 headers.put(i,types.get(i));                
             }
             HashMap<String,ArrayList<Double>> values=new HashMap<String, ArrayList<Double>>();
             for (FeatureSequenceData seq:getAllSequences()) {
                String seqName=seq.getName();
                ArrayList<Double> counts=new ArrayList<Double>(types.size());
                HashMap<String,Integer> countmap=((RegionSequenceData)seq).countAllRegions();
                for (int i=0;i<types.size();i++) {
                   String type=types.get(i);
                   int c=(countmap.containsKey(type))?countmap.get(type):0;
                   counts.add(new Double(c));
                }
                values.put(seqName, counts);
             }            
             ExpressionProfile table=new ExpressionProfile("Result",values,headers);        
             return table;
        } else {            
             RegionDataset newdata=new RegionDataset("Result");
             for (FeatureSequenceData seq:getAllSequences()) {
                 newdata.addSequence( ((RegionSequenceData)seq).extractRegionsAsPositions(variablename));
             }
             newdata.updateMaxScoreValueFromData();
             newdata.setMotifTrack(false);
             newdata.setModuleTrack(false);
             return newdata;       
        }
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else if (variablename.equals("types")) return TextVariable.class;       
       else if (variablename.equals("counts") || variablename.startsWith("counts of ")) return ExpressionProfile.class;
       else return RegionDataset.class; // all other exports are region datasets
    }
        
    public static RegionDataset createRegionDatasetFromParameter(String parameter, String targetName, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        RegionDataset newDataItem=new RegionDataset(targetName);
        SequenceCollection allSequences = engine.getDefaultSequenceCollection();
        ArrayList<Sequence> seqlist=allSequences.getAllSequences(engine);
        int size=seqlist.size();
        int i=0;        
        for (Sequence sequence:seqlist) {
            i++;
            RegionSequenceData seq=new RegionSequenceData(sequence.getName(), sequence.getGeneName(), sequence.getChromosome(), sequence.getRegionStart(), sequence.getRegionEnd(), sequence.getTSS(), sequence.getTES(), sequence.getStrandOrientation());
            // set some default values also...
            ((RegionDataset)newDataItem).addSequence(seq);
            if (task!=null) {
                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();                
                task.setProgress(i, size);
            }              
        }         
        return newDataItem;
    }
    
    
    /** Updates all regions so that their "parent" property points to the correct RegionSequenceData object
     *  This method was introduced to fix a legacy bug which is still present in some older saved sessions.
     */    
    public void fixParentChildRelationships() {
        for (FeatureSequenceData seq:getAllSequences()) {
            ((RegionSequenceData)seq).fixParentChildRelationships(this);
        }
    }
    
    
    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }    

}
