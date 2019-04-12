/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.operations.Operation_new;
import motiflab.engine.protocol.ParseError;


/**
 * SequencePartitions are used to divide all known sequences into distinct groups or clusters.
 * Each cluster can be given explicit names, but defaults to "Cluster1", "Cluster2" .. etc
 * Although most get/set methods work directly with Sequence
 * objects rather than Sequence names (Strings), the internal mechanism of the SequencePartition 
 * itself revolves around a plain list of names for the Sequences in the partition. 
 * The methods that returns Sequences objects or lists do this by dynamically obtaining these Sequences
 * from the Engine (on-the-fly) based on the internal list of names
 * 
 * @author kjetikl
 */
public class SequencePartition extends DataPartition implements SequenceGroup {
    private static String typedescription="Sequence Partition"; 
    protected String fromMap=null;
    protected String fromProperty=null;
    protected String fromList=null;

    public static String[] propertyNames=new String[]{"organism","genome build","strand orientation","chromosome"}; // properties recognized by fromProperty prefix


    @Override
    public Class getMembersClass() {
        return Sequence.class;
    }

    /**
     * Constructs a new initially "empty" Sequence partition with the given name
     * 
     * @param datasetName A name for this dataset
     */
   public SequencePartition(String datasetName) {
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
                ArrayList<String> sequences=getAllSequenceNamesInCluster(clustername);
                int clustersize=sequences.size();
                for (int i=0;i<clustersize;i++) {
                    if (i<clustersize-1) {
                        string.append(sequences.get(i));
                        string.append(",");
                    }
                    else string.append(sequences.get(i));
                }
                string.append("=");
                string.append(clustername);
                if (c<clusters.size()) string.append(";");
            }
            return string.toString();
        }
    }


    /**
     * Returns the name of the cluster the sequence is a member of
     * or NULL if the sequence is not member of any clusters in this partition
     */ 
    public String getClusterForSequence(Sequence sequence) {
        return storage.get(sequence.getName());
    }
    
    /**
     * Returns the name of the cluster the sequence is a member of
     * or NULL if the sequence is not member of any clusters in this partition
     */ 
    public String getClusterForSequence(String sequenceName) {
        return storage.get(sequenceName);
    }
    

    
    /**
     * Returns the number of Sequences in the given cluster
     * @return
     */    
    public int getNumberOfSequencesInCluster(String clustername) {
        int count=0;
        for (String value:storage.values()) {
            if (value.equals(clustername)) count++;
        }
        return count;
    }
    
    /**
     * Returns the names of all the Sequences in the cluster with the given name
     * @param clusterName
     * @return A list of Sequence names (in random order)
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllSequenceNamesInCluster(String clustername) {
        ArrayList<String> list=new ArrayList<String>();
        for (String seqName:storage.keySet()) {
            if (storage.get(seqName).equals(clustername)) list.add(seqName);
        }
        return list;
    }    
    
    /**
     * Returns all the Sequence objects in the cluster with the given name
     * (if they are currently registered with the engine)
     * @param clusterName
     * @param engine
     * @return A list of Sequence objects (in random order)
     */
    public ArrayList<Sequence> getAllSequencesInCluster(String clustername, MotifLabEngine engine) {

        ArrayList<Sequence> list=new ArrayList<Sequence>();
        for (String seqName:storage.keySet()) {
            if (storage.get(seqName).equals(clustername)) {
                Data item=engine.getDataItem(seqName); 
                if (item!=null && item instanceof Sequence) list.add((Sequence)item);                
            }
        }
        return list;
    }

    /**
     * Returns all the Sequences objects that have been assigned to clusters
     * (if they are currently registered with the engine)
     * @param engine
     * @return A list of Sequence objects (in random order)
     */
    public ArrayList<Sequence> getAllSequencesInClusters(MotifLabEngine engine) {
        ArrayList<Sequence> list=new ArrayList<Sequence>(storage.size());
        for (String clustername:getClusterNames()) {
            for (String seqName:storage.keySet()) {
                if (storage.get(seqName).equals(clustername)) {
                    Data item=engine.getDataItem(seqName);
                    if (item!=null && item instanceof Sequence) list.add((Sequence)item);
                }
            }
        }
        return list;
    }
    
    /**
     * Returns all the names of sequences that have been assigned to clusters
     * (if they are currently registered with the engine)
     * @param engine
     * @return A list of Sequence objects (in random order)
     */
    public ArrayList<String> getAllNamesOfSequencesInClusters() {
        ArrayList<String> list=new ArrayList<String>(storage.size());
        list.addAll(storage.keySet());
        return list;
    }    

    /**
     * Returns all the sequences in the cluster with the given name as a SequenceCollection
     * (if they are currently registered with the engine)
     * @param clusterName (this can not be null)
     * @param engine
     * @return A SequenceCollection containing the sequences in the cluster
     */
    public SequenceCollection getClusterAsSequenceCollection(String clustername, MotifLabEngine engine) {
        SequenceCollection collection=new SequenceCollection((clustername==null)?"EMPTY":clustername);
        for (String seqName:storage.keySet()) {
            if (storage.get(seqName).equals(clustername)) {
                Data item=engine.getDataItem(seqName); 
                if (item!=null && item instanceof Sequence) collection.addSequence((Sequence)item);                
            }
        }
        return collection;
    }        

    /** 
     * Returns true if cluster with the given name contains the sequence
     */
    public boolean contains(String clusterName, Sequence sequence) {
        if (sequence==null || clusterName==null) return false;
        if (!storage.containsKey(sequence.getName())) return false;        
        return storage.get(sequence.getName()).equals(clusterName);
    }
    
    
    /** 
     * Returns true this partition contains the given sequence
     * @param sequence
     * @return
     */
    public boolean contains(Sequence sequence) {
        if (sequence==null) return false;
        return storage.containsKey(sequence.getName());
    }
    

       
    /**
     * Returns true if this SequencePartition is the same as the other Partition
     * (and is based on the same map-criteria)
     * @param data
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof SequencePartition)) return false;
        SequencePartition other=(SequencePartition)data;
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
     * Adds a new Sequence object to the given cluster.
    *  If the clusterName is NULL the sequence will be removed if already present
     * 
     * @param sequence The Sequence to be added
     * @param clusterName The name of the target cluster
     */
    public void addSequence(Sequence sequence, String clusterName) {
        if (clusterName==null) {removeSequence(sequence); return;}        
        storage.put(sequence.getName(),clusterName); // add to local storage
        notifyListenersOfDataAddition(sequence);
    }
    
    @Override
    public void addItem(Data item, String clusterName) throws ExecutionError {
        if (!isValidClusterName(clusterName)) throw new ExecutionError("Invalid cluster name: "+clusterName);          
        if (item instanceof Sequence) addSequence((Sequence)item,clusterName);
        else throw new ExecutionError("Only Sequences can be added to a Sequence Partition. Tried to add a "+item.getDynamicType());
    }     
    
   /**
     * Adds a new Sequence object to a new cluster (with default name)
     * 
     * @param sequence The Sequence to be added
     */
    public void addSequence(Sequence sequence) {
        int nextCluster=getNumberOfClusters()+1;        
        addSequence(sequence,"Cluster"+nextCluster); 
    }
    
    
    
   /**
     * Moves a Sequence object into a new cluster. Empty clusters are 'removed'
     * If the clusterName is NULL the sequence will be removed if already present
     * 
     * @param sequence The Sequence to be moved
    *  @param clusterName the name of the new cluster
     */
    public void moveSequence(Sequence sequence, String clusterName) {
        if (clusterName==null) {removeSequence(sequence); return;}
        storage.put(sequence.getName(),clusterName);
        notifyListenersOfDataUpdate();
    }
    
   /**
     * Removes a Sequence object from this partition. Empty clusters are 'removed'
     * 
     * @param sequence The Sequence to be removed
     */
    public void removeSequence(Sequence sequence) {
        String res=storage.remove(sequence.getName()); // remove from local storage
        if (res!=null) notifyListenersOfDataRemoval(sequence);
    }
    

    @Override
    public void clearAll(MotifLabEngine engine) {
        Iterator iterator=storage.keySet().iterator();
        while (iterator.hasNext()) {
            String name=(String)iterator.next();
            iterator.remove();
            Data item=null;
            if (engine!=null) item=engine.getDataItem(name);
            if (item!=null && item instanceof Sequence) notifyListenersOfDataRemoval((Sequence)item);
        }
        this.fromMap=null;
        this.fromList=null;
        this.fromProperty=null;
        this.clearConstructorString();
    }
    
    @Override
    public void importData(Data source) throws ClassCastException {
        if (source==this) return; // no need to import, the source and target are the same
        SequencePartition datasource=(SequencePartition)source;
        this.datasetName=datasource.datasetName;
        this.fromMap=datasource.fromMap;
        this.fromList=datasource.fromList;
        this.fromProperty=datasource.fromProperty;
        this.cloneConstructor(datasource);
        storage.clear();
        for (String seqName:datasource.storage.keySet()) {
            storage.put(seqName,datasource.storage.get(seqName));
        }
    }
    
    @Override
    public SequencePartition clone() {
        SequencePartition newpartition= new SequencePartition(datasetName);
        for (String seqName:storage.keySet()) {
            newpartition.storage.put(seqName,storage.get(seqName));
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
        int sequences=size();
        return typedescription+" : "+sequences+" sequence"+((sequences==1)?"":"s")+" in "+clusters+" cluster"+((clusters==1)?"":"s");
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
                String sequenceName=matcher.group(1);
                String clustername=matcher.group(3);
                if (!SequencePartition.isValidClusterName(clustername)) throw new ParseError("Invalid cluster name: '"+clustername+"'");
                Data data=engine.getDataItem(sequenceName);
                if (data==null) throw new ParseError("No such sequence: "+line);
                else if (!(data instanceof Sequence)) throw new ParseError("'"+line+"' is not a Sequence object");
                else addSequence((Sequence)data, clustername);

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
            SequenceTextMap result=new SequenceTextMap("map","");
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
        } else return getClusterAsSequenceCollection(variablename, engine);
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
            return SequenceTextMap.class;
        } else {
            return SequenceCollection.class; // all other exported values are SequenceCollection
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


    /** If the clusters in this partition are based on which Sequences satisfy a condition in a SequenceNumericMap, this method will return a string describing the settings used for initialization */
    public String getFromMapString() {
        return getConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    public void setFromMapString(String mapString) {
        setConstructorString(Operation_new.FROM_MAP_PREFIX,mapString);
    }    
    /** Returns TRUE if  the clusters in this partition are based on which Sequences satisfy a condition in a SequenceNumericMap */
    public boolean isFromMap() {
        return hasConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    /** If this partition is based on a (non-resolved) list of references, this method will return a string describing the settings used for initialization */
    public String getFromListString() {
        return getConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** Sets a string used for initialization of this partition (which includes references to sequences, collections, and partition clusters) */
    public void setFromListString(String liststring) {
        setConstructorString(Operation_new.FROM_LIST_PREFIX,liststring);
    }
    /** Returns TRUE if this partition is based a (non-resolved) list of references (which could include references to collections and partition-clusters) */
    public boolean isFromList() {
        return hasConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** If the clusters in this partition are based a defined sequence property such as organism, genome build or strand orientation, this method will return a string describing the settings used for initialization */
    public String getFromPropertyString() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }
    public void setFromPropertyString(String propertyString) {
        setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,propertyString);
    }
    /** Returns TRUE if  the clusters in this partition are based a defined sequence property such as organism, genome build or strand orientation */
    public boolean isFromProperty() {
        return hasConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }

     /**
     * Adds a new cluster to this partition containing sequences that satisfy a condition in a SequenceNumericMap
     *
     * @param the name of the new cluster
     * @param map The SequenceNumericMap used as basis for the condition
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in )
     * @param firstOperand A number used for comparisons (specified as a string which can be either a literal number or the name of a data object)
     * @param secondOperand A second number used as upper limit if operator is "in" (specified as a string which can be either a literal number or the name of a data object)
     * @return A configuration-String describing the cluster assignment
     */
   public String addClusterFromMap(String clustername, SequenceNumericMap map, String operator, String firstOperand, String secondOperand, MotifLabEngine engine) throws ExecutionError {
       Object firstOperandData=null;
       Object secondOperandData=null;
       if (firstOperand==null || firstOperand.isEmpty()) throw new ExecutionError("Missing numeric operand for comparison");       
       firstOperandData=engine.getNumericDataForString(firstOperand);
       if (firstOperandData==null) throw new ExecutionError("'"+firstOperand+"' is not a numeric constant or known numeric data object");
       if ((firstOperandData instanceof Data) && !(firstOperandData instanceof NumericConstant || firstOperandData instanceof NumericVariable || firstOperandData instanceof SequenceNumericMap)) throw new ExecutionError("'"+firstOperand+"' is of a type not applicable in this context");
       if (operator.equals("in")) {
           if (secondOperand==null) throw new ExecutionError("Missing upper limit for numeric range");
           secondOperandData=engine.getNumericDataForString(secondOperand);
           if (secondOperandData==null) throw new ExecutionError("'"+secondOperand+"' is not a numeric constant or known numeric data object");
           if ((secondOperandData instanceof Data) && !(secondOperandData instanceof NumericConstant || secondOperandData instanceof NumericVariable || secondOperandData instanceof SequenceNumericMap)) throw new ExecutionError("'"+secondOperand+"' is of a type not applicable in this context");
       }         
       // configure
       String config="";
       if (operator.equals("in")) {
          config=map.getName()+" in ["+firstOperand+","+secondOperand+"]:"+clustername; // sets the 'config' string
       } else {
          config=map.getName()+""+operator+""+firstOperand+":"+clustername; // sets the 'config' string
       }
       ArrayList<Data> allSequences=engine.getAllDataItemsOfType(Sequence.class);
       for (Data sequence:allSequences) {
           String sequenceName=sequence.getName();
           if (sequenceSatisfiesCondition(sequenceName, map, operator, firstOperandData, secondOperandData)) storage.put(sequenceName,clustername);
       }
       return config;
   }

   /** Returns TRUE if the sequence with the given name satisfies the condition in the map */
   private boolean sequenceSatisfiesCondition(String sequenceName, SequenceNumericMap map, String operator, Object firstOperandData, Object secondOperandData) {
       double firstOperand=0;
       double secondOperand=0;
            if (firstOperandData instanceof Integer) firstOperand=((Integer)firstOperandData).intValue();
       else if (firstOperandData instanceof Double) firstOperand=((Double)firstOperandData).doubleValue();
       else if (firstOperandData instanceof NumericVariable) firstOperand=((NumericVariable)firstOperandData).getValue();
       else if (firstOperandData instanceof NumericConstant) firstOperand=((NumericConstant)firstOperandData).getValue();
       else if (firstOperandData instanceof SequenceNumericMap) firstOperand=((SequenceNumericMap)firstOperandData).getValue(sequenceName);
            if (secondOperandData instanceof Integer) secondOperand=((Integer)secondOperandData).intValue();
       else if (secondOperandData instanceof Double) secondOperand=((Double)secondOperandData).doubleValue();
       else if (secondOperandData instanceof NumericVariable) secondOperand=((NumericVariable)secondOperandData).getValue();
       else if (secondOperandData instanceof NumericConstant) secondOperand=((NumericConstant)secondOperandData).getValue();
       else if (secondOperandData instanceof SequenceNumericMap) secondOperand=((SequenceNumericMap)secondOperandData).getValue(sequenceName);
      
       double sequenceValue=map.getValue(sequenceName);
            if (operator.equals("="))  return (sequenceValue==firstOperand);
       else if (operator.equals(">=")) return (sequenceValue>=firstOperand);
       else if (operator.equals(">"))  return (sequenceValue>firstOperand);
       else if (operator.equals("<=")) return (sequenceValue<=firstOperand);
       else if (operator.equals("<"))  return (sequenceValue<firstOperand);
       else if (operator.equals("<>")) return (sequenceValue!=firstOperand);
       else if (operator.equals("in")) return (sequenceValue>=firstOperand && sequenceValue<=secondOperand);
       else return false;
   }

/**
 * Parses a configuration string for Sequence Partition cluster creation from a Numeric map
 * and returns an Object list containing parsed elements:
 * [0] Name of SequenceNumericMap (String)
 * [1] Operator (String)
 * [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [4] cluster name (String)
 * @param configString. Format example: "SequenceNumericMap >= 0.4 : clustername" or "SequenceNumericMap in [0,10]:clustername"
 * @return
 */
   public static String[] parseMapConfigurationString(String configstring) throws ParseError {
       Pattern pattern=Pattern.compile("\\s*([a-zA-Z_0-9]+)\\s*(>=|<=|<>|<|=|>|in)\\s*\\[?\\s*([\\w\\d\\.\\-]+)(\\s*,\\s*([\\w\\d\\.\\-]+))?\\s*\\]?\\s*:\\s*([a-zA-Z_0-9]+)");
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
       } else throw new ParseError("Unable to parse 'Map' parameter for new Sequence Partition");
       return new String[]{mapName,operator,firstOperand,secondOperand,targetCluster};
   }

/**
 * Creates and returns a new SequencePartition based on a parameter string
 * @param text The parameter string to be parsed
 * @param targetName The new name of the SequencePartition (only used if a new partition is created)
 * @param notfound If this parameter is NULL the method will throw an ExecutionError if any entries in the parameter list is not found (the first error will be reported)
 *                 If this parameter is an (empty) ArrayList, the method will be run in 'silent-mode' and not throw exceptions upon encountering errors.
 *                 The ArrayList will be filled with the names that could not be properly resolved (with the reason in parenthesis),
 *                 the entries that could not be properly resolved will be ignored
 * @param engine
 * @return
 * @throws ExecutionError
 * @throws InterruptedException
 */
public static SequencePartition parseSequencePartitionParameters(String text, String targetName, ArrayList<String> notfound, MotifLabEngine engine) throws ExecutionError, InterruptedException {
    boolean silentMode=false;
    if (notfound!=null) {silentMode=true;notfound.clear();}
    SequencePartition partition=new SequencePartition(targetName);
    if (text.startsWith(Operation_new.FROM_PROPERTY_PREFIX)) {
        String property=text.substring(Operation_new.FROM_PROPERTY_PREFIX.length()).trim();
        ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
        for (Data seq:sequences) {
            String clustername=null;
            if (property.equalsIgnoreCase("organism")) {
                int organism=((Sequence)seq).getOrganism();
                clustername=Organism.getCommonName(organism);
            } else if (property.equalsIgnoreCase("build") || property.equalsIgnoreCase("genome build")) {
                clustername=((Sequence)seq).getGenomeBuild();
            } else if (property.equalsIgnoreCase("chromosome")) {
                clustername=((Sequence)seq).getChromosome();
            } else if (property.equalsIgnoreCase("strand") || property.equalsIgnoreCase("orientation") || property.equalsIgnoreCase("strand orientation")) {
                int strand=((Sequence)seq).getStrandOrientation();
                if (strand==Sequence.DIRECT) clustername="direct";
                else if (strand==Sequence.REVERSE) clustername="reverse";
                else clustername="undetermined";
            } else throw new ExecutionError("Unknown sequence property: "+property);
            addToSequencePartitionCluster(partition, seq, clustername, engine);
        }
        partition.setFromPropertyString(property);
        return partition;
    } else if(text.startsWith(Operation_new.FROM_MAP_PREFIX)) {
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
               if (!(numericMap instanceof SequenceNumericMap)) throw new ExecutionError("'"+mapName+"' is not a Sequence Numeric Map");
               partition.addClusterFromMap(targetCluster, (SequenceNumericMap)numericMap, operator, firstOperand, secondOperand, engine);
           }
           partition.setFromMapString(configstring);
           return partition;
     } else {
            boolean keepConfig=false; // set this flag to true if the original config-string should be retained
            if (text.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               text=text.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
            }
 //          ArrayList<String> list=MotifLabEngine.splitOnComma(text); // is this necessary or is it sufficient to use text.split(",")
            String[] list=text.split("\\s*;\\s*");
            for (String lineentry:list) {
               if (lineentry.trim().isEmpty()) continue;
               Data dataobject=null;
               String[] pair=lineentry.split("\\s*=\\s*");
               if (pair.length!=2) {
                   if (silentMode) {notfound.add(lineentry+" : Not a 'sequence = cluster' pair");continue;} else throw new ExecutionError("Not a 'sequence = cluster' pair: "+lineentry);
               }
               String leftSide=pair[0].trim();
               String targetCluster=pair[1].trim();   
               
               if (leftSide.isEmpty()) {
                   if (silentMode) {notfound.add(lineentry+" : Missing sequence name");continue;} else throw new ExecutionError("Missing sequence name "+lineentry);
               }
               if (targetCluster.isEmpty()) {
                   if (silentMode) {notfound.add(lineentry+" : Missing cluster name");continue;} else throw new ExecutionError("Missing cluster name "+lineentry);
               }
               if (!SequencePartition.isValidClusterName(targetCluster)) {
                   if (silentMode) {notfound.add(targetCluster+" : Invalid cluster name");continue;} else throw new ExecutionError("Invalid cluster name: "+targetCluster);
               }
               String[] entries=leftSide.split("\\s*,\\s*");
               for (String entry:entries) {
                   if (entry.contains("->")) { // entry refers to a cluster within a Sequence Partition
                       String[] elements=entry.split("->");
                       if (elements.length!=2) {
                           if (silentMode) {notfound.add(entry+" : Syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                       }
                       String entrypartition=elements[0];
                       String cluster=elements[1];
                       dataobject=engine.getDataItem(entrypartition);
                            if (dataobject==null) {if (silentMode) {notfound.add(entrypartition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+entrypartition);}
                       else if (!(dataobject instanceof SequencePartition)) {if (silentMode) {notfound.add(entrypartition+" : Not a Sequence Partition"); continue;} else throw new ExecutionError("Data item '"+entrypartition+"' is not a Sequence Partition");}
                       else if (!((SequencePartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Sequence Partition '"+entrypartition+"' does not contain a cluster with the name '"+cluster+"'");}
                       else dataobject=((SequencePartition)dataobject).getClusterAsSequenceCollection(cluster, engine);
                   } else if (entry.contains(":")) { // a range of sequences
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
                           ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpressionInNumericRange(range1[0], range1[2], start, end, Sequence.class);
                           for (Data object:regexmatches) {
                               addToSequencePartitionCluster(partition,object,targetCluster,engine);
                           }
                           continue; 
                   } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                       if (entry.contains(".")) entry=entry.replace(".", "\\."); // escape dot (since it is allowed in sequence names)
                       if (entry.contains("-")) entry=entry.replace("-", "\\-"); // escape - (since it is allowed in sequence names)
                       if (entry.contains("+")) entry=entry.replace("+", "\\+"); // escape + (since it is allowed in sequence names)   
                       if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                       ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, Sequence.class);
                       for (Data object:regexmatches) {
                           addToSequencePartitionCluster(partition,object,targetCluster,engine);
                       }
                       continue; 
                   } else {
                       dataobject=engine.getDataItem(entry);
                   }

                        if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
                   else if (dataobject instanceof SequencePartition) {if (silentMode) notfound.add(entry+" : Missing cluster for Sequence Partition)"); else throw new ExecutionError("Missing specification of cluster for Sequence Partition '"+entry+"'. (use format: Partition.Cluster)");}
                   else if (!(dataobject instanceof Sequence || dataobject instanceof SequenceCollection)) {if (silentMode) notfound.add(entry+" : Not a Sequence or Sequence Collection)"); else throw new ExecutionError("Data item '"+entry+"' is not a Sequence or Sequence Collection");}
                   else {
                       addToSequencePartitionCluster(partition,dataobject,targetCluster,engine);
                   }
              } // end: for each sequence entry on the left side of = sign
           } // end: for each line
           if (keepConfig) partition.setFromListString(text); // Store original config-string in data object
           return partition;
        }
    }

    /** Adds a single Sequence or Sequence collection (other) to a target cluster in a SequencePartition */
    private static void addToSequencePartitionCluster(SequencePartition target, Object other, String clusterName, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Sequence) {
            target.addSequence((Sequence)other, clusterName);
        } else if (other instanceof SequenceCollection) {
            for (Sequence seq:((SequenceCollection)other).getAllSequences(engine)) {
                target.addSequence(seq, clusterName);
            }
        } else {
            System.err.println("SYSTEM ERROR: In SequencePartition.addToSequencePartitionCluster. Parameter is neither Sequence nor Sequence Collection but rather: "+other.getClass().getSimpleName());
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
         
         // migrate legacy "constructor" fields to new common constructor                
              if (this.fromMap!=null) this.setConstructorString(Operation_new.FROM_MAP_PREFIX, this.fromMap);
         else if (this.fromList!=null) this.setConstructorString(Operation_new.FROM_LIST_PREFIX, this.fromList);           
         else if (this.fromProperty!=null) this.setConstructorString(Operation_new.FROM_PROPERTY_PREFIX, this.fromProperty); 

         // clear legacy fields 
         this.fromMap=null; 
         this.fromProperty=null; 
         this.fromList=null;                
    }

}

