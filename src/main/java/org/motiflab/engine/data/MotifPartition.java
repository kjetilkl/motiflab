/*


 */

package org.motiflab.engine.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.ParseError;


/**
 * MotifPartitions are used to divide all known motifs into distinct groups or clusters.
 * Each cluster can be given explicit names, but defaults to "Cluster1", "Cluster2" .. etc
 * Although most get/set methods work directly with Motifs
 * objects rather than Motif names (Strings), the internal mechanism of the MotifPartition
 * itself revolves around a plain list of names for the Motifs in the partition.
 * The methods that returns Motifs objects or lists do this by dynamically obtaining these Motifs
 * from the Engine (on-the-fly) based on the internal list of names
 *
 * @author kjetikl
 */
public class MotifPartition extends DataPartition {
    private static String typedescription="Motif Partition";
    protected String fromMap=null;
    protected String fromProperty=null;
    protected String fromList=null;

    public static String[] propertyNames=new String[]{"Alternatives","Class_1_level","Class_2_levels","Class_3_levels","Class_4_levels","Class_5_levels","Class_6_levels"}; // properties recognized by fromProperty prefix


    @Override
    public Class getMembersClass() {
        return Motif.class;
    }

    /**
     * Constructs a new initially "empty" Motif partition with the given name
     *
     * @param datasetName A name for this dataset
     */
   public MotifPartition(String datasetName) {
       this.datasetName=datasetName;
       storage=new HashMap<String,String>(20);
   }


    @Override
    public String getValueAsParameterString() {
        if (hasConstructorString()) return getFullConstructorString();
        else {
            StringBuilder string=new StringBuilder();
            ArrayList<String> clusters=getClusterNames();
            int c=0;
            for (String clustername:clusters) {
                c++;
                ArrayList<String> motifs=getAllMotifNamesInCluster(clustername);
                int clustersize=motifs.size();
                for (int i=0;i<clustersize;i++) {
                    if (i<clustersize-1) {
                        string.append(motifs.get(i));
                        string.append(",");
                    }
                    else string.append(motifs.get(i));
                }
                string.append("=");
                string.append(clustername);
                if (c<clusters.size()) string.append(";");
            }
            return string.toString();
        }
    }




    /**
     * Returns the name of the cluster the motif is a member of
     * or NULL if the motif is not member of any clusters in this partition
     */
    public String getClusterForMotif(Motif motif) {
        return storage.get(motif.getName());
    }

    /**
     * Returns the name of the cluster the motif is a member of
     * or NULL if the motif is not member of any clusters in this partition
     */
    public String getClusterForMotif(String motifName) {
        return storage.get(motifName);
    }

    /**
     * Returns the number of Motifs in the given cluster
     * @return
     */
    public int getNumberOfMotifsInCluster(String clustername) {
        int count=0;
        for (String value:storage.values()) {
            if (value.equals(clustername)) count++;
        }
        return count;
    }

    /**
     * Returns the names of all the Motifs in the cluster with the given name
     * @param clusterName
     * @return A list of Motif names (in random order)
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllMotifNamesInCluster(String clustername) {
        ArrayList<String> list=new ArrayList<String>();
        for (String motifName:storage.keySet()) {
            if (storage.get(motifName).equals(clustername)) list.add(motifName);
        }
        return list;
    }

    /**
     * Returns all the Motif objects in the cluster with the given name
     * (if they are currently registered with the engine)
     * @param clusterName
     * @param engine
     * @return A list of Motif objects (in random order)
     */
    public ArrayList<Motif> getAllMotifsInCluster(String clustername, MotifLabEngine engine) {

        ArrayList<Motif> list=new ArrayList<Motif>();
        for (String motifName:storage.keySet()) {
            if (storage.get(motifName).equals(clustername)) {
                Data item=engine.getDataItem(motifName);
                if (item!=null && item instanceof Motif) list.add((Motif)item);
            }
        }
        return list;
    }

    /**
     * Returns all the motifs in the cluster with the given name as a MotifCollection
     * (if they are currently registered with the engine)
     * @param clusterName (this can not be null)
     * @param engine
     * @return A MotifCollection containing the motifs in the cluster
     */
    public MotifCollection getClusterAsMotifCollection(String clustername, MotifLabEngine engine) {
        MotifCollection collection=new MotifCollection((clustername==null)?"EMPTY":clustername);
        for (String motifName:storage.keySet()) {
            if (storage.get(motifName).equals(clustername)) {
                Data item=engine.getDataItem(motifName);
                if (item!=null && item instanceof Motif) collection.addMotif((Motif)item);
            }
        }
        return collection;
    }

    /**
     * Returns true if cluster with the given name contains the motif
     */
    public boolean contains(String clusterName, Motif motif) {
        if (motif==null || clusterName==null) return false;
        if (!storage.containsKey(motif.getName())) return false;
        return storage.get(motif.getName()).equals(clusterName);
    }


    /**
     * Returns true this partition contains the given motif
     * @param motif
     * @return
     */
    public boolean contains(Motif motif) {
        if (motif==null) return false;
        return storage.containsKey(motif.getName());
    }



    /**
     * Returns true if this MotifPartition is the same as the other Partition
     * (or is based on the same map-criteria)
     * @param data
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof MotifPartition)) return false;
        MotifPartition other=(MotifPartition)data;
        if (!this.hasSameConstructor(other)) return false;
        if (size()!=other.size()) return false;
        for (String name:storage.keySet()) {
            String thisvalue=storage.get(name);
            String othervalue=other.storage.get(name);
            if (!thisvalue.equals(othervalue)) return false;
        }
        return true;
    }


   /**
     * Adds a new Motif object to the given cluster.
    *  If the clusterName is NULL the motif will be removed if already present
     *
     * @param motif The Motif to be added
     * @param clusterName The name of the target cluster
     */
    public void addMotif(Motif motif, String clusterName) {
        if (clusterName==null) {removeMotif(motif); return;}
        storage.put(motif.getName(),clusterName); // add to local storage
        notifyListenersOfDataAddition(motif);
    }

    @Override
    public void addItem(Data item, String clusterName) throws ExecutionError {
        if (!isValidClusterName(clusterName)) throw new ExecutionError("Invalid cluster name: "+clusterName);       
        if (item instanceof Motif) addMotif((Motif)item,clusterName);
        else throw new ExecutionError("Only Motifs can be added to a Motif Partition. Tried to add a "+item.getDynamicType());
    }     

   /**
     * Adds a new Motif object to a new cluster (with default name)
     *
     * @param motif The Motif to be added
     */
    public void addMotif(Motif motif) {
        int nextCluster=getNumberOfClusters()+1;
        addMotif(motif,"Cluster"+nextCluster);
    }



   /**
     * Moves a Motif object into a new cluster. Empty clusters are 'removed'
     * If the clusterName is NULL the motif will be removed if already present
     *
     * @param motif The Motif to be moved
    *  @param clusterName the name of the new cluster
     */
    public void moveMotif(Motif motif, String clusterName) {
        if (clusterName==null) {removeMotif(motif); return;}
        storage.put(motif.getName(),clusterName);
        notifyListenersOfDataUpdate();
    }


   /**
     * Removes a Motif object from this partition. Empty clusters are 'removed'
     *
     * @param motif The Motif to be removed
     */
    public void removeMotif(Motif motif) {
        String res=storage.remove(motif.getName()); // remove from local storage
        if (res!=null) notifyListenersOfDataRemoval(motif);
    }


    @Override
    public void clearAll(MotifLabEngine engine) {
        Iterator iterator=storage.keySet().iterator();
        while (iterator.hasNext()) {
            String name=(String)iterator.next();
            iterator.remove();
            Data item=null;
            if (engine!=null) item=engine.getDataItem(name);
            if (item!=null && item instanceof Motif) notifyListenersOfDataRemoval((Motif)item);
        }
        this.fromMap=null;
        this.fromList=null;
        this.fromProperty=null;
        this.clearConstructorString();
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        if (source==this) return; // no need to import, the source and target are the same
        MotifPartition datasource=(MotifPartition)source;
        this.datasetName=datasource.datasetName;
        this.fromMap=datasource.fromMap;
        this.fromList=datasource.fromList;
        this.fromProperty=datasource.fromProperty;
        this.cloneConstructor(datasource);
        storage.clear();
        for (String motifName:datasource.storage.keySet()) {
            storage.put(motifName,datasource.storage.get(motifName));
        }
    }

    @Override
    public MotifPartition clone() {
        MotifPartition newpartition= new MotifPartition(datasetName);
        for (String motifName:storage.keySet()) {
            newpartition.storage.put(motifName,storage.get(motifName));
        }
        newpartition.fromMap=this.fromMap;
        newpartition.fromList=this.fromList;
        newpartition.fromProperty=this.fromProperty;
        newpartition.cloneConstructor(this);
        return newpartition;
    }



    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }
    
    @Override
    public String getTypeDescription() {
        int clusters=getNumberOfClusters();
        int motifs=size();
        return typedescription+" : "+motifs+" motif"+((motifs==1)?"":"s")+" in "+clusters+" cluster"+((clusters==1)?"":"s");
    }


    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("(\\S+)(\\s*=\\s*|\\t)(\\S+)");
        storage.clear();
        this.fromMap=null;
        this.fromList=null;
        this.fromProperty=null;
        this.clearConstructorString();
        for (String line:input) {
            line=line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            java.util.regex.Matcher matcher=pattern.matcher(line);
            if (matcher.matches()) {
                String motifName=matcher.group(1);
                String clustername=matcher.group(3);
                if (!MotifPartition.isValidClusterName(clustername)) throw new ParseError("Invalid cluster name: '"+clustername+"'");
                Data data=engine.getDataItem(motifName);
                if (data==null) throw new ParseError("No such motif: "+line);
                else if (!(data instanceof Motif)) throw new ParseError("'"+line+"' is not a Motif object");
                else addMotif((Motif)data, clustername);

            } // end: matcher.matches()
        } // end: for each input line
    } // end: inputFromPlain


    public void debug() {
        System.err.println(output());
    }


    @Override
    public String[] getResultVariables() {
        ArrayList<String>val=getClusterNames();
        val.add("cluster map");
        val.add("cluster names");
        val.add("cluster sizes");
        val.add("number of clusters");
        val.add("size");
        String[] list=new String[val.size()];
        return val.toArray(list);
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (!hasResult(variablename)) throw new ExecutionError("'" + getName() + "' does not have a result for '" + variablename + "'");
        else if (variablename.equalsIgnoreCase("cluster map")) {
            MotifTextMap result=new MotifTextMap("map","");
            for (String key:storage.keySet()) {
                String value=storage.get(key);
                result.setValue(key, value);
            }
            return result;
        } else if (variablename.equalsIgnoreCase("cluster names")) {
            TextVariable result=new TextVariable("names");
            result.append(getClusterNames());
            return result;
        } else if (variablename.equalsIgnoreCase("cluster sizes")) {
            TextVariable result=new TextVariable("sizes");
            for (String cluster:getClusterNames()) {
                int size=getClusterSize(cluster);
                result.append(cluster+"\t"+size);
            }
            return result;           
        } else if (variablename.equalsIgnoreCase("number of clusters")) {
            NumericVariable result=new NumericVariable("clusters",getNumberOfClusters());
            return result;           
        } else if (variablename.equalsIgnoreCase("size")) {
            NumericVariable result=new NumericVariable("size",size());
            return result;           
        } else return getClusterAsMotifCollection(variablename, engine);
    }

    @Override
    public Class getResultType(String variablename) {
        if (!hasResult(variablename)) {
            return null;
        } else if (variablename.equalsIgnoreCase("number of clusters") || variablename.equalsIgnoreCase("size") ) {
            return NumericVariable.class;
        } else if (variablename.equalsIgnoreCase("cluster names") || variablename.equals("cluster sizes")) {
            return TextVariable.class;
        } else if (variablename.equalsIgnoreCase("cluster map")) {
            return MotifTextMap.class;
        } else {
            return MotifCollection.class; // all other exported values are MotifCollections
        }
    }

    @Override
    public boolean hasResult(String variablename) {
        if (   variablename.equalsIgnoreCase("cluster map")
            || variablename.equalsIgnoreCase("cluster names")
            || variablename.equals("cluster sizes")
            || variablename.equals("number of clusters")
            || variablename.equals("size")
        ) return true;
        return storage.containsValue(variablename);
    }    
    

    /** If the clusters in this partition are based on which Motifs satisfy a condition in a MotifNumericMap, this method will return a string describing the settings used for initialization */
    public String getFromMapString() {
        return getConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    
    public void setFromMapString(String fromMapString) {
        setConstructorString(Operation_new.FROM_MAP_PREFIX,fromMapString);
    }    
    /** Returns TRUE if  the clusters in this partition are based on which Motifs satisfy a condition in a MotifNumericMap */
    public boolean isFromMap() {
        return hasConstructorString(Operation_new.FROM_MAP_PREFIX);
    }

    /** If the clusters in this partition are based a defined motif property such as organism, genome build or strand orientation, this method will return a string describing the settings used for initialization */
    public String getFromPropertyString() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }

    public void setFromPropertyString(String propertyString) {
        setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,propertyString);
    }    
    
    /** Returns TRUE if  the clusters in this partition are based a defined motif property such as organism, genome build or strand orientation */
    public boolean isFromProperty() {
        return hasConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }
    /** If this partition is based on a (non-resolved) list of references, this method will return a string describing the settings used for initialization */
    public String getFromListString() {
        return getConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** Sets a string used for initialization of this partition (which includes references to motifs, collections, and partition clusters) */
    public void setFromListString(String liststring) {
        setConstructorString(Operation_new.FROM_LIST_PREFIX,liststring);
    }
    /** Returns TRUE if this partition is based a (non-resolved) list of references (which could include references to collections and partition-clusters) */
    public boolean isFromList() {
        return hasConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
     /**
     * Adds a new cluster to this partition containing motifs that satisfy a condition in a MotifNumericMap
     *
     * @param the name of the new cluster
     * @param map The MotifNumericMap used as basis for the condition
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in )
     * @param firstOperand A number used for comparisons (specified as a string which can be either a literal number or the name of a data object)
     * @param secondOperand A second number used as upper limit if operator is "in" (specified as a string which can be either a literal number or the name of a data object)
     * @return A configuration-String describing the cluster assignment
     */
   public String addClusterFromMap(String clustername, MotifNumericMap map, String operator, String firstOperand, String secondOperand, MotifLabEngine engine) throws ExecutionError{
       Object firstOperandData=null;
       Object secondOperandData=null;
       if (firstOperand==null || firstOperand.isEmpty()) throw new ExecutionError("Missing numeric operand for comparison");       
       firstOperandData=engine.getNumericDataForString(firstOperand);
       if (firstOperandData==null) throw new ExecutionError("'"+firstOperand+"' is not a numeric constant or known numeric data object");
       if ((firstOperandData instanceof Data) && !(firstOperandData instanceof NumericConstant || firstOperandData instanceof NumericVariable || firstOperandData instanceof MotifNumericMap)) throw new ExecutionError("'"+firstOperand+"' is of a type not applicable in this context");
       if (operator.equals("in")) {
           if (secondOperand==null) throw new ExecutionError("Missing upper limit for numeric range");
           secondOperandData=engine.getNumericDataForString(secondOperand);
           if (secondOperandData==null) throw new ExecutionError("'"+secondOperand+"' is not a numeric constant or known numeric data object");
           if ((secondOperandData instanceof Data) && !(secondOperandData instanceof NumericConstant || secondOperandData instanceof NumericVariable || secondOperandData instanceof MotifNumericMap)) throw new ExecutionError("'"+secondOperand+"' is of a type not applicable in this context");
       }         
       // configure
       String config="";
       if (operator.equals("in")) {
          config=map.getName()+" in ["+firstOperand+","+secondOperand+"]:"+clustername; // sets the 'config' string
       } else {
          config=map.getName()+""+operator+""+firstOperand+":"+clustername; // sets the 'config' string
       }
       ArrayList<Data> allMotifs=engine.getAllDataItemsOfType(Motif.class);
       for (Data motif:allMotifs) {
           String motifName=motif.getName();
           if (motifSatisfiesCondition(motifName, map, operator, firstOperandData, secondOperandData)) storage.put(motifName,clustername);
       }
       return config;
   }

   /** Returns TRUE if the motif with the given name satisfies the condition in the map */
   private boolean motifSatisfiesCondition(String motifName, MotifNumericMap map, String operator, Object firstOperandData, Object secondOperandData) {
       double firstOperand=0;
       double secondOperand=0;
            if (firstOperandData instanceof Integer) firstOperand=((Integer)firstOperandData).intValue();
       else if (firstOperandData instanceof Double) firstOperand=((Double)firstOperandData).doubleValue();
       else if (firstOperandData instanceof NumericVariable) firstOperand=((NumericVariable)firstOperandData).getValue();
       else if (firstOperandData instanceof NumericConstant) firstOperand=((NumericConstant)firstOperandData).getValue();
       else if (firstOperandData instanceof MotifNumericMap) firstOperand=((MotifNumericMap)firstOperandData).getValue(motifName);
            if (secondOperandData instanceof Integer) secondOperand=((Integer)secondOperandData).intValue();
       else if (secondOperandData instanceof Double) secondOperand=((Double)secondOperandData).doubleValue();
       else if (secondOperandData instanceof NumericVariable) secondOperand=((NumericVariable)secondOperandData).getValue();
       else if (secondOperandData instanceof NumericConstant) secondOperand=((NumericConstant)secondOperandData).getValue();
       else if (secondOperandData instanceof MotifNumericMap) secondOperand=((MotifNumericMap)secondOperandData).getValue(motifName);
           
       double motifValue=map.getValue(motifName);
            if (operator.equals("="))  return (motifValue==firstOperand);
       else if (operator.equals(">=")) return (motifValue>=firstOperand);
       else if (operator.equals(">"))  return (motifValue>firstOperand);
       else if (operator.equals("<=")) return (motifValue<=firstOperand);
       else if (operator.equals("<"))  return (motifValue<firstOperand);
       else if (operator.equals("<>")) return (motifValue!=firstOperand);
       else if (operator.equals("in")) return (motifValue>=firstOperand && motifValue<=secondOperand);
       else return false;
   }

/**
 * Parses a configuration string for Motif Partition cluster creation from a Numeric map
 * and returns an Object list containing parsed elements:
 * [0] Name of MotifNumericMap (String)
 * [1] Operator (String)
 * [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [4] cluster name (String)
 * @param configString. Format example: "MotifNumericMap >= 0.4 : clustername" or "MotifNumericMap in [0,10]:clustername"
 * @return
 */
   public static String[] parseMapConfigurationString(String configstring) throws ParseError {
       Pattern pattern=Pattern.compile("\\s*([a-zA-Z_0-9]+)\\s*(>=|>|=|<=|<>|<|in)\\s*\\[?\\s*([\\w\\d\\.\\-]+)(\\s*,\\s*([\\w\\d\\.\\-]+))?\\s*\\]?\\s*:\\s*([a-zA-Z_0-9]+)");
       Matcher matcher=pattern.matcher(configstring);
       String mapName="";
       String operator="";
       String targetCluster="";
       String firstOperand=null;
       String secondOperand=null;
       if (matcher.find()) {
           mapName=matcher.group(1);
           operator=matcher.group(2);
           targetCluster=matcher.group(6);   
           firstOperand=matcher.group(3);
           if (matcher.group(5)!=null && !matcher.group(5).isEmpty()) { // second operand               
              secondOperand=matcher.group(5);
           }
           if (operator.equals("in") && secondOperand==null) throw new ParseError("Missing upper limit for numeric range");
       } else throw new ParseError("Unable to parse 'Map' parameter for new Motif Partition");
       return new String[]{mapName,operator,firstOperand,secondOperand,targetCluster};
   }

/**
 * Creates and returns a new MotifPartition based on a parameter string
 * @param text The parameter string to be parsed
 * @param targetName The new name of the MotifPartition (only used if a new partition is created)
 * @param notfound If this parameter is NULL the method will throw an ExecutionError if any entries in the parameter list is not found (the first error will be reported)
 *                 If this parameter is an (empty) ArrayList, the method will be run in 'silent-mode' and not throw exceptions upon encountering errors.
 *                 The ArrayList will be filled with the names that could not be properly resolved (with the reason in parenthesis),
 *                 the entries that could not be properly resolved will be ignored
 * @param engine
 * @return
 * @throws ExecutionError
 * @throws InterruptedException
 */
public static MotifPartition parseMotifPartitionParameters(String text, String targetName, ArrayList<String> notfound, MotifLabEngine engine) throws ExecutionError, InterruptedException {
    boolean silentMode=false;
    if (notfound!=null) {silentMode=true;notfound.clear();}
    if (text.startsWith(Operation_new.FROM_PROPERTY_PREFIX)) {
        MotifPartition partition=null;
        String property=text.substring(Operation_new.FROM_PROPERTY_PREFIX.length()).trim();
        if (property.toLowerCase().startsWith("class_")) {
            partition=new MotifPartition(targetName);            
            ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
            for (Data motif:motifs) {
                String clustername=null;
                if (property.equalsIgnoreCase("Class_1_level")) {
                    String classification=((Motif)motif).getClassification();
                    if (classification==null) clustername=MotifClassification.UNKNOWN_CLASS_LABEL;
                    else clustername = MotifClassification.trimToLevel(classification,1);
                } else if (property.equalsIgnoreCase("Class_2_levels")) {
                    String classification=((Motif)motif).getClassification();
                    if (classification==null) clustername=MotifClassification.UNKNOWN_CLASS_LABEL;
                    else {
                        int levels=MotifClassification.getClassLevel(classification);
                        if (levels==1) clustername=classification+".x";
                        else clustername = MotifClassification.trimToLevel(classification, 2);
                    }
                } else if (property.equalsIgnoreCase("Class_3_levels")) {
                    String classification=((Motif)motif).getClassification();
                    if (classification==null) clustername=MotifClassification.UNKNOWN_CLASS_LABEL;
                    else {
                        int levels=MotifClassification.getClassLevel(classification);
                        if (levels==1) clustername=classification+".x.x";
                        else if (levels == 2) clustername=classification+".x";
                        else clustername = MotifClassification.trimToLevel(classification, 3);
                    }
                } else if (property.equalsIgnoreCase("Class_4_levels")) {
                    String classification=((Motif)motif).getClassification();
                    if (classification==null) clustername=MotifClassification.UNKNOWN_CLASS_LABEL;
                    else {
                        int levels=MotifClassification.getClassLevel(classification);
                        if (levels==1) clustername=clustername+".x.x.x";
                        else if (levels == 2) clustername=classification+".x.x";
                        else if (levels == 3) clustername=classification+".x";
                        else clustername = MotifClassification.trimToLevel(classification, 4);
                    }
                } else if (property.equalsIgnoreCase("Class_5_levels")) {
                    String classification=((Motif)motif).getClassification();
                    if (classification==null) clustername=MotifClassification.UNKNOWN_CLASS_LABEL;
                    else {
                        int levels=MotifClassification.getClassLevel(classification);
                        if (levels==1) clustername=clustername+".x.x.x.x";
                        else if (levels == 2) clustername=classification+".x.x.x";
                        else if (levels == 3) clustername=classification+".x.x";
                        else if (levels == 4) clustername=classification+".x";
                        else clustername = MotifClassification.trimToLevel(classification, 5);
                    }
                } else if (property.equalsIgnoreCase("Class_6_levels")) {
                    String classification=((Motif)motif).getClassification();
                    if (classification==null) clustername=MotifClassification.UNKNOWN_CLASS_LABEL;
                    else {
                        int levels=MotifClassification.getClassLevel(classification);
                        if (levels==1) clustername=clustername+".x.x.x.x.x";
                        else if (levels == 2) clustername=classification+".x.x.x.x";
                        else if (levels == 3) clustername=classification+".x.x.x";
                        else if (levels == 4) clustername=classification+".x.x";
                        else if (levels == 5) clustername=classification+".x";                        
                        else clustername = MotifClassification.trimToLevel(classification, 6);
                    }
                } else throw new ExecutionError("Unknown motif property: "+property);
                clustername=clustername.replace('.', '_'); // clusternames should not contain dots
                clustername=clustername.replace(' ','_'); // clusternames should not contain spaces
                addToMotifPartitionCluster(partition, motif, clustername, engine);
            }
        } else if (property.equalsIgnoreCase("Alternatives")) {
               partition=createPartitionBasedOnAlternatives(targetName,null,engine); // base the partition on all existing motifs. Hence the 'null' value.
        } else throw new ExecutionError("Unknown motif property: "+property);
        partition.setFromPropertyString(property);
        return partition;
    } else if(text.startsWith(Operation_new.FROM_MAP_PREFIX)) {
           MotifPartition partition=new MotifPartition(targetName);
           String configstring=text.substring(Operation_new.FROM_MAP_PREFIX.length());
           String[] mapelements=configstring.split("\\s*;\\s*"); // use semi-colon since comma is used in ranges. e.g. "Map in [0,1]"
           for (String mapstring:mapelements) { // for each map condition
               String mapName="";
               String operator="";
               String targetCluster="";
               String firstOperand=null;
               String secondOperand=null;
               try {
                   String[] parseElements=parseMapConfigurationString(mapstring);
                   mapName=(String)parseElements[0];
                   operator=(String)parseElements[1];
                   firstOperand=(String)parseElements[2];
                   if (parseElements[3]!=null) secondOperand=(String)parseElements[3];
                   targetCluster=(String)parseElements[4];
               } catch (ParseError e) {
                   throw new ExecutionError(e.getMessage());
               }
               Data numericMap=engine.getDataItem(mapName);
               if (numericMap==null) throw new ExecutionError("Unknown data object '"+mapName+"'");
               if (!(numericMap instanceof MotifNumericMap)) throw new ExecutionError("'"+mapName+"' is not a Motif Numeric Map");
               partition.addClusterFromMap(targetCluster, (MotifNumericMap)numericMap, operator, firstOperand, secondOperand, engine);
           }
           partition.setFromMapString(configstring);
           return partition;
     } else {
            MotifPartition partition=new MotifPartition(targetName);
            boolean keepConfig=false; // set this flag to true if the original config-string should be retained
            if (text.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               text=text.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
            }
            String[] list=text.split("\\s*;\\s*");
            for (String lineentry:list) {
               if (lineentry.trim().isEmpty()) continue;
               Data dataobject=null;
               String[] pair=lineentry.split("\\s*=\\s*");
               if (pair.length!=2) {
                   if (silentMode) {notfound.add(lineentry+" : Not a 'motif = cluster' pair");continue;} else throw new ExecutionError("Not a 'motif = cluster' pair: "+lineentry);
               }
               String leftSide=pair[0].trim();
               if (leftSide.isEmpty()) {
                   if (silentMode) {notfound.add(lineentry+" : Missing motif name");continue;} else throw new ExecutionError("Missing motif name "+lineentry);
               }
               String targetCluster=pair[1].trim();
               if (targetCluster.isEmpty()) {
                   if (silentMode) {notfound.add(lineentry+" : Missing cluster name");continue;} else throw new ExecutionError("Missing cluster name "+lineentry);
               }
               if (!MotifPartition.isValidClusterName(targetCluster)) {
                   if (silentMode) {notfound.add(targetCluster+" : Invalid cluster name");continue;} else throw new ExecutionError("Invalid cluster name: "+targetCluster);
               }
               String[] entries=leftSide.split("\\s*,\\s*");
               for (String entry:entries) {
                   if (entry.contains("->")) { // entry refers to a cluster within a Motif Partition
                       String[] elements=entry.split("->");
                       if (elements.length!=2) {
                           if (silentMode) {notfound.add(entry+" : Syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                       }
                       String entrypartition=elements[0];
                       String cluster=elements[1];
                       dataobject=engine.getDataItem(entrypartition);
                            if (dataobject==null) {if (silentMode) {notfound.add(entrypartition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+entrypartition);}
                       else if (!(dataobject instanceof MotifPartition)) {if (silentMode) {notfound.add(entrypartition+" : Not a Motif Partition"); continue;} else throw new ExecutionError("Data item '"+entrypartition+"' is not a Motif Partition");}
                       else if (!((MotifPartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Motif Partition '"+entrypartition+"' does not contain a cluster with the name '"+cluster+"'");}
                       else dataobject=((MotifPartition)dataobject).getClusterAsMotifCollection(cluster, engine);
                   } else if (entry.contains(":")) { // a range of motifs
                           String[] rangeElements=entry.split(":");
                           if (rangeElements[0]==null || rangeElements[0].isEmpty() || rangeElements[1]==null || rangeElements[1].isEmpty()) {if (silentMode) {notfound.add(entry+" : Problem with range specification"); continue;} else throw new ExecutionError("Problem with range specification: "+entry);}
                           String[] range1=MotifLabEngine.splitOnNumericPart(rangeElements[0]);
                           String[] range2=MotifLabEngine.splitOnNumericPart(rangeElements[1]);
                           if (   range1==null || range2==null
                               || range1[0].isEmpty() && !range2[0].isEmpty()
                               || range2[0].isEmpty() && !range1[0].isEmpty()
                               || range1[2].isEmpty() && !range2[2].isEmpty()
                               || range2[2].isEmpty() && !range1[2].isEmpty()
                               || !range1[0].equals(range2[0])
                               || !range1[2].equals(range2[2])
                           ) {if (silentMode) {notfound.add(entry+" : Problem with range specification"); continue;} else throw new ExecutionError("Problem with range specification: "+entry);}                         
                           int start=0;
                           int end=0;
                           try {
                              start=Integer.parseInt(range1[1]);  
                              end=Integer.parseInt(range2[1]);  
                           } catch(NumberFormatException nf) {
                              if (silentMode) {notfound.add(entry+" : Problem with range specification"); continue;} else throw new ExecutionError("Problem with range specification: "+entry); 
                           }
                           ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpressionInNumericRange(range1[0], range1[2], start, end, Motif.class);
                           for (Data object:regexmatches) {
                               addToMotifPartitionCluster(partition,object,targetCluster,engine);
                           }
                           continue; 
                   } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                       if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                       ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, Motif.class);
                       for (Data object:regexmatches) {
                           addToMotifPartitionCluster(partition,object,targetCluster,engine);
                       }
                       continue;
                   } else {
                       dataobject=engine.getDataItem(entry);
                   }

                        if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
                   else if (dataobject instanceof MotifPartition) {if (silentMode) notfound.add(entry+" : Missing cluster for Motif Partition)"); else throw new ExecutionError("Missing specification of cluster for Motif Partition '"+entry+"'. (use format: Partition.Cluster)");}
                   else if (!(dataobject instanceof Motif || dataobject instanceof MotifCollection)) {if (silentMode) notfound.add(entry+" : Not a Motif or Motif Collection)"); else throw new ExecutionError("Data item '"+entry+"' is not a Motif or Motif Collection");}
                   else {
                       addToMotifPartitionCluster(partition,dataobject,targetCluster,engine);
                   }
              } // end: for each motif entry on the left side of = sign
           } // end: for each line
           if (keepConfig) partition.setFromListString(text); // Store original config-string in data object
           return partition;
        }
    }

    /** Adds a single Motif or Motif collection (other) to a target cluster in a MotifPartition */
    private static void addToMotifPartitionCluster(MotifPartition target, Object other, String clusterName, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Motif) {
            target.addMotif((Motif)other, clusterName);
        } else if (other instanceof MotifCollection) {
            for (Motif motif:((MotifCollection)other).getAllMotifs(engine)) {
                target.addMotif(motif, clusterName);
            }
        } else {
            System.err.println("SYSTEM ERROR: In MotifPartition.addToMotifPartitionCluster. Parameter is neither Motif nor Motif Collection but rather: "+other.getClass().getSimpleName());
        }
    }
    
    
    public static MotifPartition createPartitionWithIndividualClusters(String targetName, MotifCollection motifcollection, MotifLabEngine engine) {
        MotifPartition target=new MotifPartition(targetName);
        ArrayList<Motif> motifs=null;
        if (motifcollection!=null) {
            motifs=motifcollection.getAllMotifs(engine);
        } else {
           motifs=new ArrayList<Motif>();
           for (Data data:engine.getAllDataItemsOfType(Motif.class)) motifs.add((Motif)data); 
        }
        for (Motif motif:motifs) {    
            String clusterName=motif.getShortName();
            if (clusterName==null) clusterName=motif.getName();
            clusterName=Motif.cleanUpMotifShortName(clusterName, true);
            target.addMotif(motif, clusterName);
        }   
        return target;
    }
    
    
    /** Creates a MotifPartition with clusters based on the equivalence classes 
     *  that stems from considering a motif to be equivalent with its known 
     *  alternatives (duplicate motifs)
     *  @param targetName A new name for this partition
     *  @param motifcollection If this is provided, only motifs in this collection will be considered.
     *         If this parameter is null, all currently existing motifs will be included.
     */
    public static MotifPartition createPartitionBasedOnAlternatives(String targetName, MotifCollection motifcollection, MotifLabEngine engine) {
        MotifPartition target=new MotifPartition(targetName);
        ArrayList<String> singletons=new ArrayList<String>(); // a list of those motifs that have no alternatives
        ArrayList<HashSet<String>> clusters=new ArrayList<HashSet<String>>();
        ArrayList<Motif> motifs=null;
        HashSet<String> motifnamesfilter=null;
        if (motifcollection!=null) {
            motifs=motifcollection.getAllMotifs(engine);
            motifnamesfilter=new HashSet<String>(); 
            for (Motif motif:motifs) motifnamesfilter.add(motif.getName());
        } else {
           motifs=new ArrayList<Motif>();
           for (Data data:engine.getAllDataItemsOfType(Motif.class)) motifs.add((Motif)data); 
        }
        // go through each motif and create "clusters" containing the motif itself and its alternatives within the MotifCollection       
        
        for (Motif motif:motifs) {
            if (!motif.hasDuplicates()) singletons.add(motif.getName());
            else {
               ArrayList<String> alternatives=motif.getKnownDuplicatesNames();              
               HashSet<String> cluster=new HashSet<String>(alternatives.size()+1);
               cluster.add(motif.getName());
               cluster.addAll(alternatives);
               if (motifnamesfilter!=null) filterMotifNames(cluster,motifnamesfilter); // consider only the motifs in given collection
               clusters.add(cluster);
            }
        }
        // now go through the clusters and merge those that overlap until no more mergers can be made
        int mergers=1; // the 1 is just to start things off
        int[] mergedown=new int[clusters.size()];
        for (int i=0;i<mergedown.length;i++) mergedown[i]=i; // the number is the smallest numbered cluster that a cluster should be merged with. We are always "merging down" (i.e. cluster 5 merges into cluster 3, not the other way around)       
        while (mergers>0) {
            mergers=0;
            for (int i=0;i<clusters.size()-1;i++) {
              for (int j=i+1;j<clusters.size();j++) {
                 HashSet<String> cluster1=clusters.get(i);
                 HashSet<String> cluster2=clusters.get(j);
                 if (cluster1==null || cluster2==null) continue; // some of these have already been merged
                 if (setsOverlap(cluster1, cluster2)) {
                     mergedown[j]=mergedown[i]; // this will be translative. E.g. if cluster 5 should be merged with cluster 3 and cluster 7 with cluster 5, then mergedown[5]:=3 and later mergedown[7]:=mergedown[5]==3 so cluster 7 will end up being merged with 3
                     mergers++;
                 }
              }               
            }
            // now do the merging
            for (int i=0;i<clusters.size();i++) {
               if (mergedown[i]<i) { // this cluster should be merged down... 
                  HashSet<String> cluster=clusters.get(i);
                  if (cluster==null) continue; // already merged
                  HashSet<String> targetcluster=clusters.get(mergedown[i]);
                  targetcluster.addAll(cluster);
                  clusters.set(i,null); // remove the cluster which has been merged down
               }
            }
        } // end while still merging
        // now the clusters and singletons lists contain our partitions
        HashSet<String> usedNames=new HashSet<String>();
        for (HashSet<String> cluster:clusters) {
            if (cluster==null) continue; // this has been merged down
            String clusterName=getUsableClusterName(cluster,usedNames,engine);
            for (String motifname:cluster) {
               Data motif=engine.getDataItem(motifname);
               if (motif instanceof Motif) target.addMotif((Motif)motif, clusterName);
            }
            usedNames.add(clusterName);
        }
        for (String motifname:singletons) {
            Data motif=engine.getDataItem(motifname);
            if (motif instanceof Motif) {
                String clusterName=((Motif)motif).getShortName();
                if (clusterName==null || clusterName.isEmpty()) clusterName=motifname;
                if (clusterName.matches("^\\w\\$.+")) clusterName=clusterName.substring(2); // remove Transfac prefix
                clusterName=getUsableClusterName(clusterName,usedNames,engine); 
                target.addMotif((Motif)motif, clusterName);
                usedNames.add(clusterName);
            }
        }
        return target;
    }
    
    /** Removes all Strings from the target Set which is not in the filter
     *  The method alters the target parameter
     */
    private static void filterMotifNames(HashSet<String> target, HashSet<String> filter) {
        Iterator<String> iterator=target.iterator();
        while (iterator.hasNext()) {
            String value=iterator.next();
            if (!filter.contains(value)) iterator.remove();
        }
    }
    
    private static boolean setsOverlap(HashSet<String> set1,HashSet<String> set2) {
        for (String string:set1) {
            if (set2.contains(string)) return true;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private static String getUsableClusterName(Object names, HashSet<String> usednames, MotifLabEngine engine) {       
        String candidatename=(names instanceof HashSet)?getClusterCommonName((HashSet<String>)names, engine):(String)names;
        candidatename=Motif.cleanUpMotifShortName(candidatename, true); // strip Transfac prefixes and suffixes
        if (!usednames.contains(candidatename)) return candidatename;   
        while (usednames.contains(candidatename)) { // the name was taken... try adding incremental numbers
           candidatename=incrementName(candidatename);
        }
        return candidatename;
    }  
     
    private static String getClusterCommonName(HashSet<String> motifnames, MotifLabEngine engine) {
        // this method should really look into which TFs that bind to the given motifs but for now I will just return the short name of the motif with the shortest name
        String name=null;
        for (String motifname:motifnames) {
            Data motif=engine.getDataItem(motifname);
            if (motif instanceof Motif) {
                String shortname=((Motif)motif).getShortName();
                if (shortname!=null && !shortname.isEmpty()) {
                    if (shortname.matches("^\\w\\$.+")) shortname=shortname.substring(2);
                    if (name==null) name=shortname;
                    else if (shortname.length()<name.length()) name=shortname; // return shortest name
                }
            }           
        }
        return (name!=null)?name:"ERROR";
    }

    private static String incrementName(String candidatename) {
        if (candidatename.matches("^.+_\\d+$")) { // determine counter from suffix
            int index=candidatename.lastIndexOf('_');
            String prefix=candidatename.substring(0,index);
            String suffix=candidatename.substring(index+1);
            try {
                int counter=Integer.parseInt(suffix);
                counter++;
                candidatename=prefix+"_"+counter;
            } catch (Exception e) {} // there should be no exception!
        } else candidatename=candidatename+"_2";  
        return candidatename;
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
         
         // migrate legacy "constructor" fields to new common constructor
              if (this.fromMap!=null) this.setConstructorString(Operation_new.FROM_MAP_PREFIX, this.fromMap);
         else if (this.fromProperty!=null) this.setConstructorString(Operation_new.FROM_PROPERTY_PREFIX, this.fromProperty);
         else if (this.fromList!=null) this.setConstructorString(Operation_new.FROM_LIST_PREFIX, this.fromList);

         // clear legacy fields
         this.fromMap=null; 
         this.fromProperty=null; 
         this.fromList=null;         
         
    }

    private class StringLengthComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if (o2==null) return -1;
            else if (o1==null) return 1;
            else {
                int length1=o1.length();
                int length2=o2.length();
                if (length1<length2) return -1;
                else if (length1>length2) return 1;
                else return 0;
            }
        }
        
    }
}

