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
 * ModulePartitions are used to divide all known modules into distinct groups or clusters.
 * Each cluster can be given explicit names, but defaults to "Cluster1", "Cluster2" .. etc
 * Allthough most get/set methods work directly with Module
 * objects rather than Module names (Strings), the internal mechanism of the ModulePartition
 * itself revolves around a plain list of names for the Modules in the partition.
 * The methods that returns Modules objects or lists do this by dynamically obtaining these Modules
 * from the Engine (on-the-fly) based on the internal list of names
 *
 * @author kjetikl
 */
public class ModulePartition extends DataPartition {
    private static String typedescription="Module Partition";
    protected String fromMap=null;
    protected String fromList=null;


    @Override
    public Class getMembersClass() {
        return ModuleCRM.class;
    }

    /**
     * Constructs a new initially "empty" ModuleCRM partition with the given name
     *
     * @param datasetName A name for this dataset
     */
   public ModulePartition(String datasetName) {
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
                ArrayList<String> modules=getAllModuleNamesInCluster(clustername);
                int clustersize=modules.size();
                for (int i=0;i<clustersize;i++) {
                    if (i<clustersize-1) {
                        string.append(modules.get(i));
                        string.append(",");
                    }
                    else string.append(modules.get(i));
                }
                string.append("=");
                string.append(clustername);
                if (c<clusters.size()) string.append(";");
            }
            return string.toString();
        }
    }




    /**
     * Returns the name of the cluster the module is a member of
     * or NULL if the module is not member of any clusters in this partition
     */
    public String getClusterForModule(ModuleCRM cisRegModule) {
        return storage.get(cisRegModule.getName());
    }

    /**
     * Returns the name of the cluster the module is a member of
     * or NULL if the module is not member of any clusters in this partition
     */
    public String getClusterForModule(String moduleName) {
        return storage.get(moduleName);
    }

    /**
     * Returns the number of Modules in the given cluster
     * @return
     */
    public int getNumberOfModulesInCluster(String clustername) {
        int count=0;
        for (String value:storage.values()) {
            if (value.equals(clustername)) count++;
        }
        return count;
    }

    /**
     * Returns the names of all the Modules in the cluster with the given name
     * @param clusterName
     * @return A list of ModuleCRM names (in random order)
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllModuleNamesInCluster(String clustername) {
        ArrayList<String> list=new ArrayList<String>();
        for (String moduleName:storage.keySet()) {
            if (storage.get(moduleName).equals(clustername)) list.add(moduleName);
        }
        return list;
    }

    /**
     * Returns all the ModuleCRM objects in the cluster with the given name
 (if they are currently registered with the engine)
     * @param clusterName
     * @param engine
     * @return A list of ModuleCRM objects (in random order)
     */
    public ArrayList<ModuleCRM> getAllModulesInCluster(String clustername, MotifLabEngine engine) {

        ArrayList<ModuleCRM> list=new ArrayList<ModuleCRM>();
        for (String moduleName:storage.keySet()) {
            if (storage.get(moduleName).equals(clustername)) {
                Data item=engine.getDataItem(moduleName);
                if (item!=null && item instanceof ModuleCRM) list.add((ModuleCRM)item);
            }
        }
        return list;
    }

    /**
     * Returns all the modules in the cluster with the given name as a ModuleCollection
     * (if they are currently registered with the engine)
     * @param clusterName (this can not be null)
     * @param engine
     * @return A ModuleCollection containing the modules in the cluster
     */
    public ModuleCollection getClusterAsModuleCollection(String clustername, MotifLabEngine engine) {
        ModuleCollection collection=new ModuleCollection((clustername==null)?"EMPTY":clustername);
        for (String moduleName:storage.keySet()) {
            if (storage.get(moduleName).equals(clustername)) {
                Data item=engine.getDataItem(moduleName);
                if (item!=null && item instanceof ModuleCRM) collection.addModule((ModuleCRM)item);
            }
        }
        return collection;
    }

    /**
     * Returns true if cluster with the given name contains the module
     */
    public boolean contains(String clusterName, ModuleCRM cisRegModule) {
        if (cisRegModule==null || clusterName==null) return false;
        if (!storage.containsKey(cisRegModule.getName())) return false;
        return storage.get(cisRegModule.getName()).equals(clusterName);
    }


    /**
     * Returns true this partition contains the given module
     * @param module
     * @return
     */
    public boolean contains(ModuleCRM cisRegModule) {
        if (cisRegModule==null) return false;
        return storage.containsKey(cisRegModule.getName());
    }



    /**
     * Returns true if this ModulePartition is the same as the other Partition
     * (or is based on the same map-criteria)
     * @param data
     * @return
     */
    @Override
    public boolean containsSameData(Data data) {
        if (data==null || !(data instanceof ModulePartition)) return false;
        ModulePartition other=(ModulePartition)data;
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
     * Adds a new Module object to the given cluster.
    *  If the clusterName is NULL the module will be removed if already present
     *
     * @param module The ModuleCRM to be added
     * @param clusterName The name of the target cluster
     */
    public void addModule(ModuleCRM cisRegModule, String clusterName) {
        if (clusterName==null) {removeModule(cisRegModule); return;}
        storage.put(cisRegModule.getName(),clusterName); // add to local storage
        notifyListenersOfDataAddition(cisRegModule);
    }

    @Override
    public void addItem(Data item, String clusterName) throws ExecutionError {
        if (!isValidClusterName(clusterName)) throw new ExecutionError("Invalid cluster name: "+clusterName);
        if (item instanceof ModuleCRM) addModule((ModuleCRM)item,clusterName);
        else throw new ExecutionError("Only Modules can be added to a Module Partition. Tried to add a "+item.getDynamicType());
    }    

   /**
     * Adds a new ModuleCRM object to a new cluster (with default name)
     *
     * @param module The ModuleCRM to be added
     */
    public void addModule(ModuleCRM cisRegModule) {
        int nextCluster=getNumberOfClusters()+1;
        addModule(cisRegModule,"Cluster"+nextCluster);
    }



   /**
     * Moves a Module object into a new cluster. Empty clusters are 'removed'
     * If the clusterName is NULL the module will be removed if already present
     *
     * @param module The ModuleCRM to be moved
    *  @param clusterName the name of the new cluster
     */
    public void moveModule(ModuleCRM cisRegModule, String clusterName) {
        if (clusterName==null) {removeModule(cisRegModule); return;}
        storage.put(cisRegModule.getName(),clusterName);
        notifyListenersOfDataUpdate();
    }


   /**
     * Removes a Module object from this partition. Empty clusters are 'removed'
     *
     * @param module The ModuleCRM to be removed
     */
    public void removeModule(ModuleCRM cisRegModule) {
        String res=storage.remove(cisRegModule.getName()); // remove from local storage
        if (res!=null) notifyListenersOfDataRemoval(cisRegModule);
    }


    @Override
    public void clearAll(MotifLabEngine engine) {
        Iterator iterator=storage.keySet().iterator();
        while (iterator.hasNext()) {
            String name=(String)iterator.next();
            iterator.remove();
            Data item=null;
            if (engine!=null) item=engine.getDataItem(name);
            if (item!=null && item instanceof ModuleCRM) notifyListenersOfDataRemoval((ModuleCRM)item);
        }
        this.fromMap=null;
        this.fromList=null;
        this.clearConstructorString();
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        if (source==this) return; // no need to import, the source and target are the same
        ModulePartition datasource=(ModulePartition)source;
        this.datasetName=datasource.datasetName;
        this.fromMap=datasource.fromMap;
        this.fromList=datasource.fromList;
        this.cloneConstructor(datasource);
        storage.clear();
        for (String moduleName:datasource.storage.keySet()) {
            storage.put(moduleName,datasource.storage.get(moduleName));
        }
    }

    @Override
    public ModulePartition clone() {
        ModulePartition newpartition= new ModulePartition(datasetName);
        for (String moduleName:storage.keySet()) {
            newpartition.storage.put(moduleName,storage.get(moduleName));
        }
        newpartition.fromMap=this.fromMap;
        newpartition.fromList=this.fromList;
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
        int modules=size();
        return typedescription+" : "+modules+" module"+((modules==1)?"":"s")+" in "+clusters+" cluster"+((clusters==1)?"":"s");
    }


    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("(\\S+)(\\s*=\\s*|\\t)(\\S+)");
        storage.clear();
        this.fromMap=null;
        this.fromList=null;
        this.clearConstructorString();
        for (String line:input) {
            line=line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            java.util.regex.Matcher matcher=pattern.matcher(line);
            if (matcher.matches()) {
                String moduleName=matcher.group(1);
                String clustername=matcher.group(3);
                if (!ModulePartition.isValidClusterName(clustername)) throw new ParseError("Invalid cluster name: '"+clustername+"'");
                Data data=engine.getDataItem(moduleName);
                if (data==null) throw new ParseError("No such module: "+line);
                else if (!(data instanceof ModuleCRM)) throw new ParseError("'"+line+"' is not a Module object");
                else addModule((ModuleCRM)data, clustername);

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
            ModuleTextMap result=new ModuleTextMap("map","");
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
        } else return getClusterAsModuleCollection(variablename, engine);
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
            return ModuleTextMap.class;
        } else {
            return ModuleCollection.class; // all other exported values are ModuleCollections
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

    /** If the clusters in this partition are based on which Modules satisfy a condition in a ModuleNumericMap, this method will return a string describing the settings used for initialization */
    public String getFromMapString() {
        return getConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    /** Returns TRUE if  the clusters in this partition are based on which Modules satisfy a condition in a ModuleNumericMap */
    public boolean isFromMap() {
        return hasConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    
    public void setFromMapString(String mapstring) {
        setConstructorString(Operation_new.FROM_MAP_PREFIX,mapstring);
    }    
    
    /** If this partition is based on a (non-resolved) list of references, this method will return a string describing the settings used for initialization */
    public String getFromListString() {
        return getConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** Sets a string used for initialization of this partition (which includes references to modules, collections, and partition clusters) */
    public void setFromListString(String liststring) {
        setConstructorString(Operation_new.FROM_LIST_PREFIX,liststring);
    }
    /** Returns TRUE if this partition is based a (non-resolved) list of references (which could include references to collections and partition-clusters) */
    public boolean isFromList() {
        return hasConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
     /**
     * Adds a new cluster to this partition containing modules that satisfy a condition in a ModuleNumericMap
     *
     * @param the name of the new cluster
     * @param map The ModuleNumericMap used as basis for the condition
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in )
     * @param firstOperand A number used for comparisons (specified as a string which can be either a literal number or the name of a data object)
     * @param secondOperand A second number used as upper limit if operator is "in" (specified as a string which can be either a literal number or the name of a data object)
     * @return A configuration-String describing the cluster assignment
     */
   public String addClusterFromMap(String clustername, ModuleNumericMap map, String operator, String firstOperand, String secondOperand, MotifLabEngine engine) throws ExecutionError{
       Object firstOperandData=null;
       Object secondOperandData=null;
       if (firstOperand==null || firstOperand.isEmpty()) throw new ExecutionError("Missing numeric operand for comparison");
       firstOperandData=engine.getNumericDataForString(firstOperand);
       if (firstOperandData==null) throw new ExecutionError("'"+firstOperand+"' is not a numeric constant or known numeric data object");
       if ((firstOperandData instanceof Data) && !(firstOperandData instanceof NumericConstant || firstOperandData instanceof NumericVariable || firstOperandData instanceof ModuleNumericMap)) throw new ExecutionError("'"+firstOperand+"' is of a type not applicable in this context");
       if (operator.equals("in")) {
           if (secondOperand==null) throw new ExecutionError("Missing upper limit for numeric range");
           secondOperandData=engine.getNumericDataForString(secondOperand);
           if (secondOperandData==null) throw new ExecutionError("'"+secondOperand+"' is not a numeric constant or known numeric data object");
           if ((secondOperandData instanceof Data) && !(secondOperandData instanceof NumericConstant || secondOperandData instanceof NumericVariable || secondOperandData instanceof ModuleNumericMap)) throw new ExecutionError("'"+secondOperand+"' is of a type not applicable in this context");
       }
       // configure
       String config="";
       if (operator.equals("in")) {
          config=map.getName()+" in ["+firstOperand+","+secondOperand+"]:"+clustername; // sets the 'config' string
       } else {
          config=map.getName()+""+operator+""+firstOperand+":"+clustername; // sets the 'config' string
       }
       ArrayList<Data> allModules=engine.getAllDataItemsOfType(ModuleCRM.class);
       for (Data cisRegModule:allModules) {
           String moduleName=cisRegModule.getName();
           if (moduleSatisfiesCondition(moduleName, map, operator, firstOperandData, secondOperandData)) storage.put(moduleName,clustername);
       }
       return config;
   }

   /** Returns TRUE if the module with the given name satisfies the condition in the map */
   private boolean moduleSatisfiesCondition(String moduleName, ModuleNumericMap map, String operator, Object firstOperandData, Object secondOperandData) {
       double firstOperand=0;
       double secondOperand=0;
            if (firstOperandData instanceof Integer) firstOperand=((Integer)firstOperandData).intValue();
       else if (firstOperandData instanceof Double) firstOperand=((Double)firstOperandData).doubleValue();
       else if (firstOperandData instanceof NumericVariable) firstOperand=((NumericVariable)firstOperandData).getValue();
       else if (firstOperandData instanceof NumericConstant) firstOperand=((NumericConstant)firstOperandData).getValue();
       else if (firstOperandData instanceof ModuleNumericMap) firstOperand=((ModuleNumericMap)firstOperandData).getValue(moduleName);
            if (secondOperandData instanceof Integer) secondOperand=((Integer)secondOperandData).intValue();
       else if (secondOperandData instanceof Double) secondOperand=((Double)secondOperandData).doubleValue();
       else if (secondOperandData instanceof NumericVariable) secondOperand=((NumericVariable)secondOperandData).getValue();
       else if (secondOperandData instanceof NumericConstant) secondOperand=((NumericConstant)secondOperandData).getValue();
       else if (secondOperandData instanceof ModuleNumericMap) secondOperand=((ModuleNumericMap)secondOperandData).getValue(moduleName);

       double moduleValue=map.getValue(moduleName);
            if (operator.equals("="))  return (moduleValue==firstOperand);
       else if (operator.equals(">=")) return (moduleValue>=firstOperand);
       else if (operator.equals(">"))  return (moduleValue>firstOperand);
       else if (operator.equals("<=")) return (moduleValue<=firstOperand);
       else if (operator.equals("<"))  return (moduleValue<firstOperand);
       else if (operator.equals("<>")) return (moduleValue!=firstOperand);
       else if (operator.equals("in")) return (moduleValue>=firstOperand && moduleValue<=secondOperand);
       else return false;
   }

/**
 * Parses a configuration string for ModuleCRM Partition cluster creation from a Numeric map
 and returns an Object list containing parsed elements:
 [0] Name of ModuleNumericMap (String)
 [1] Operator (String)
 [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 [4] cluster name (String)
 * @param configString. Format example: "ModuleNumericMap >= 0.4 : clustername" or "ModuleNumericMap in [0,10]:clustername"
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
       } else throw new ParseError("Unable to parse 'Map' parameter for new Module Partition");
       return new String[]{mapName,operator,firstOperand,secondOperand,targetCluster};
   }

/**
 * Creates and returns a new ModulePartition based on a parameter string
 * @param text The parameter string to be parsed
 * @param targetName The new name of the ModulePartition (only used if a new partition is created)
 * @param notfound If this parameter is NULL the method will throw an ExecutionError if any entries in the parameter list is not found (the first error will be reported)
 *                 If this parameter is an (empty) ArrayList, the method will be run in 'silent-mode' and not throw exceptions upon encountering errors.
 *                 The ArrayList will be filled with the names that could not be properly resolved (with the reason in parenthesis),
 *                 the entries that could not be properly resolved will be ignored
 * @param engine
 * @return
 * @throws ExecutionError
 * @throws InterruptedException
 */
public static ModulePartition parseModulePartitionParameters(String text, String targetName, ArrayList<String> notfound, MotifLabEngine engine) throws ExecutionError, InterruptedException {
    boolean silentMode=false;
    if (notfound!=null) {silentMode=true;notfound.clear();}
    ModulePartition partition=new ModulePartition(targetName);
    if (text.startsWith(Operation_new.FROM_MAP_PREFIX)) {
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
               if (!(numericMap instanceof ModuleNumericMap)) throw new ExecutionError("'"+mapName+"' is not a Module Numeric Map");
               partition.addClusterFromMap(targetCluster, (ModuleNumericMap)numericMap, operator, firstOperand, secondOperand, engine);
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
                   if (silentMode) {notfound.add(lineentry+" : Not a 'module = cluster' pair");continue;} else throw new ExecutionError("Not a 'module = cluster' pair: "+lineentry);
               }
               String leftSide=pair[0].trim();
               if (leftSide.isEmpty()) {
                   if (silentMode) {notfound.add(lineentry+" : Missing module name");continue;} else throw new ExecutionError("Missing module name "+lineentry);
               }
               String targetCluster=pair[1].trim();
               if (targetCluster.isEmpty()) {
                   if (silentMode) {notfound.add(lineentry+" : Missing cluster name");continue;} else throw new ExecutionError("Missing cluster name "+lineentry);
               }
               if (!ModulePartition.isValidClusterName(targetCluster)) {
                   if (silentMode) {notfound.add(targetCluster+" : Invalid cluster name");continue;} else throw new ExecutionError("Invalid cluster name: "+targetCluster);
               }
               String[] entries=leftSide.split("\\s*,\\s*");
               for (String entry:entries) {
                   if (entry.contains("->")) { // entry refers to a cluster within a ModuleCRM Partition
                       String[] elements=entry.split("->");
                       if (elements.length!=2) {
                           if (silentMode) {notfound.add(entry+" : Syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                       }
                       String entrypartition=elements[0];
                       String cluster=elements[1];
                       dataobject=engine.getDataItem(entrypartition);
                            if (dataobject==null) {if (silentMode) {notfound.add(entrypartition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+entrypartition);}
                       else if (!(dataobject instanceof ModulePartition)) {if (silentMode) {notfound.add(entrypartition+" : Not a Module Partition"); continue;} else throw new ExecutionError("Data item '"+entrypartition+"' is not a Module Partition");}
                       else if (!((ModulePartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Module Partition '"+entrypartition+"' does not contain a cluster with the name '"+cluster+"'");}
                       else dataobject=((ModulePartition)dataobject).getClusterAsModuleCollection(cluster, engine);
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
                           ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpressionInNumericRange(range1[0], range1[2], start, end, ModuleCRM.class);
                           for (Data object:regexmatches) {
                               addToModulePartitionCluster(partition,object,targetCluster,engine);
                           }
                           continue; 
                   } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                       if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                       ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, ModuleCRM.class);
                       for (Data object:regexmatches) {
                           addToModulePartitionCluster(partition,object,targetCluster,engine);
                       }
                       continue;
                   } else {
                       dataobject=engine.getDataItem(entry);
                   }

                        if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
                   else if (dataobject instanceof ModulePartition) {if (silentMode) notfound.add(entry+" : Missing cluster for Module Partition)"); else throw new ExecutionError("Missing specification of cluster for Module Partition '"+entry+"'. (use format: Partition.Cluster)");}
                   else if (!(dataobject instanceof ModuleCRM || dataobject instanceof ModuleCollection)) {if (silentMode) notfound.add(entry+" : Not a Module or Module Collection)"); else throw new ExecutionError("Data item '"+entry+"' is not a Module or Module Collection");}
                   else {
                       addToModulePartitionCluster(partition,dataobject,targetCluster,engine);
                   }
              } // end: for each module entry on the left side of = sign
           } // end: for each line
           if (keepConfig) partition.setFromListString(text); // Store original config-string in data object 
           return partition;
        }
    }

    /** Adds a single ModuleCRM or ModuleCRM collection (other) to a target cluster in a ModulePartition */
    private static void addToModulePartitionCluster(ModulePartition target, Object other, String clusterName, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof ModuleCRM) {
            target.addModule((ModuleCRM)other, clusterName);
        } else if (other instanceof ModuleCollection) {
            for (ModuleCRM cisRegModule:((ModuleCollection)other).getAllModules(engine)) {
                target.addModule(cisRegModule, clusterName);
            }
        } else {
            System.err.println("SYSTEM ERROR: In ModulePartition.addToModulePartitionCluster. Parameter is neither Module nor Module Collection but rather: "+other.getClass().getSimpleName());
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

         // clear legacy fields
         this.fromMap=null; 
         this.fromList=null;                
    }

}

