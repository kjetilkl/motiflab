/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;

/**
 *
 * @author Kjetil
 */
public abstract class DataPartition extends Data implements DataGroup {
    static final long serialVersionUID = 1274342290242839476L;
    
    protected String datasetName;
    protected HashMap<String,String> storage; // the key is a member name and the value is a cluster name
    
    private String[] constructor=null;

    /**
     * Specifies a new name for this Partition
     *
     * @param name the name for this Partition
     */
    public void setName(String name) {
        this.datasetName=name;
    }

    @Override
    public void rename(String name) {
        setName(name);
    }

   /**
    * Returns the name of this Partition
    *
    * @return dataset name
    */
    @Override
    public String getName() {
        return datasetName;
    }

    @Override
    public Object getValue() {return this;} // should maybe change later

    /**
     * Returns a list with the names of all clusters (alphabetically ordered)
     * @return
     */
    public ArrayList<String> getClusterNames() {
        ArrayList<String> list=new ArrayList<String>();
        for (String value:storage.values()) {
            if (!list.contains(value)) list.add(value);
        }
        Collections.sort(list, MotifLabEngine.getNaturalSortOrderComparator(true));
        return list;
    } 

    /**
     * Returns a list of all members that have been assigned to clusters
     * @return
     */
    public ArrayList<String> getAllAssignedMembers() {
        ArrayList<String> list=new ArrayList<String>(storage.size());
        for (String s:storage.keySet()) list.add(s);
        return list;
    }

    /**
     * Returns true if this partition has a cluster with the given name
     */
    public boolean containsCluster(String clusterName) {
        return storage.values().contains(clusterName);
    }

    /**
     * Returns the number of clusters in this Partition
     * @return
     */
    public int getNumberOfClusters() {
        ArrayList<String> list=new ArrayList<String>();
        for (String value:storage.values()) {
            if (!list.contains(value)) list.add(value);
        }
        return list.size();
    }


    /**
     * Returns the size of the given cluster
     * @return
     */
    public int getClusterSize(String clustername) {
        int count=0;
        for (String value:storage.values()) {
            if (value.equals(clustername)) count++;
        }
        return count;
    }

    /**
     * Returns the names of all the members in the cluster with the given name
     * @param clusterName
     * @return A list of names (in random order)
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllMembersInCluster(String clustername) {
        ArrayList<String> list=new ArrayList<String>();
        for (String memberName:storage.keySet()) {
            if (storage.get(memberName).equals(clustername)) list.add(memberName);
        }
        return list;
    }

    /** Returns the clusters as a map where each key is the name of a cluster 
     *  and the value is a Set with the names of all members of that cluster
     */
    public HashMap<String,HashSet<String>> getClusters() {
        HashMap<String,HashSet<String>> clusters=new HashMap<String, HashSet<String>>();
        for (String clusterName:getClusterNames()) {
            ArrayList<String> members=getAllMembersInCluster(clusterName);
            HashSet<String> cluster=new HashSet<String>(members);
            clusters.put(clusterName, cluster);
        }
        return clusters;
    }
    /**
     * Returns true if the cluster with the given name contains a member with the given name
     */
    public boolean contains(String clusterName, String memberName) {
        if (memberName==null || clusterName==null) return false;
        if (!storage.containsKey(memberName)) return false;
        return storage.get(memberName).equals(clusterName);
    }

    /**
     * Returns true if this partition contains the given member.
     * (I.e. the given member has been assigned to a cluster)
     * @param memberName
     * @return
     */
    public boolean contains(String memberName) {
        if (memberName==null) return false;
        return storage.containsKey(memberName);
    }

    /**
     * Returns the names of all the data items of the member type that has not 
     * been assigned to any clusters
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllUnassigned(MotifLabEngine engine) {
        ArrayList<String> list=new ArrayList<String>();
        ArrayList<Data> alldata=engine.getAllDataItemsOfType(getMembersClass());
        for (Data data:alldata) {
            String dataname=data.getName();
            if (!storage.containsKey(dataname)) list.add(dataname);
        }
        return list;
    }    
    
    /**
     * Returns the number of objects that are members of this Partition
     * (only counting the ones that are assigned to clusters)
     */
    public int size() {
        return storage.size();
    }

    /**
     * 
     * @param item
     * @param clusterName
     * @throws ExecutionError An error is thrown if the added item is not of the correct type (see getMembersClass)
     *                        or the clusterName is inValid
     */
    public abstract void addItem(Data item, String clusterName) throws ExecutionError;   
    
    /**
     * Changes the name of the cluster to the newname
     * @param oldname
     * @param newname
     */
    public void renameCluster(String oldname, String newname) {
        for (String memberName:storage.keySet()) {
            if (storage.get(memberName).equals(oldname)) storage.put(memberName, newname);
        }
        notifyListenersOfDataUpdate();
    }

    @Override
    public String output() {
        StringBuilder string=new StringBuilder();
        for (String cluster:getClusterNames()) {
            ArrayList<String> members=getAllMembersInCluster(cluster);
            Collections.sort(members, MotifLabEngine.getNaturalSortOrderComparator(true));
            for (String memberName:members) {
                string.append(memberName);
                string.append("=");
                string.append(cluster);
                string.append("\n");
            }           
        }
        return string.toString();
    }

  /** Returns TRUE if the given name is a valid clustername (consisting of only letters, numbers and underscore */
    public static boolean isValidClusterName(String clustername) {
        if (clustername==null || clustername.isEmpty()) return false;
        return (clustername.matches("[a-zA-z_0-9]+"));
    }
    
    
    @Override
    public String getConstructorString(String constructorPrefix) {
        if (constructor==null || constructor.length<2) return null;
        if (constructor[0].equals(constructorPrefix)) return constructor[1];
        else return null;
    }
    
    @Override
    public String getFullConstructorString() {
        if (constructor==null || constructor.length<2 || constructor[0]==null || constructor[1]==null) return "";
        else return constructor[0]+constructor[1];
    }  
    
    @Override
    public void setConstructorString(String constructorPrefix, String parameters) {
        if (constructorPrefix==null || constructorPrefix.isEmpty() || parameters==null || parameters.isEmpty()) constructor=null;
        else constructor=new String[]{constructorPrefix,parameters};
    }   

    @Override
    public boolean hasConstructorString() {
        return (constructor!=null && constructor.length==2 && constructor[0]!=null && constructor[1]!=null);
    }      
    
    @Override
    public boolean hasConstructorString(String constructorPrefix) {
        if (constructor==null || constructor.length<2 || constructor[0]==null) return false;
        return constructor[0].equals(constructorPrefix);
    }
    
    @Override
    public void clearConstructorString() {
        constructor=null;
    }      
    
    @Override
    public String getConstructorTypePrefix() {
       if (constructor==null || constructor.length<2) return null;
       else return constructor[0];
    }
    
    @Override   
    public void cloneConstructor(DataGroup other) {
        String otherPrefix=other.getConstructorTypePrefix();
        String otherParameter=other.getConstructorString(otherPrefix);
        setConstructorString(otherPrefix, otherParameter);
    }
    
    @Override
    public boolean hasSameConstructor(DataGroup other) {
        String thisPrefix=this.getConstructorTypePrefix();
        String otherPrefix=other.getConstructorTypePrefix();
        if (thisPrefix==null && otherPrefix==null) return true;        
        if (! (otherPrefix!=null && thisPrefix!=null && thisPrefix.equals(otherPrefix))) return false;
        // here the prefixes are equal (and not NULL)
        String thisParameter=this.getConstructorString(thisPrefix);         
        String otherParameter=other.getConstructorString(otherPrefix);   
        return (thisParameter!=null && otherParameter!=null && thisParameter.equals(otherParameter));
    }   
    
    
    /**
     * Returns TRUE if this partition contains the same entries assigned to the same clusters 
     * as the other partition
     * @param data
     * @return 
     */
    public final boolean containsSameMappings(DataPartition other) {
        if (other==null || !(other.getClass()==this.getClass())) return false;
        if (size()!=other.size()) return false;
        for (String name:storage.keySet()) {
            String thisvalue=storage.get(name);
            String othervalue=other.storage.get(name);
            if (!thisvalue.equals(othervalue)) return false;
        }
        return true;
    }    

}
