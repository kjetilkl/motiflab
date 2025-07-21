/*
 
 
 */

package org.motiflab.engine.data;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.operations.Operation_statistic;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.protocol.StandardOperationParser_statistic;
import org.motiflab.engine.task.ExecutableTask;


/**
 * SequenceCollections can be used to refer to subsets of all the Sequence objects that
 * are registered in the Engine. Although most get/set methods work directly with Sequence
 * objects rather than Sequence names (Strings), the internal mechanism of the SequenceCollection 
 * itself revolves around a plain list of names for the Sequences in the collection. 
 * The methods that returns Sequences objects or lists do this by dynamically obtaining these Sequences
 * from the Engine (on-the-fly) based on the internal list of names
 * 
 * @author kjetikl
 */
public class SequenceCollection extends DataCollection implements SequenceGroup {
    private static String typedescription="Sequence Collection"; 
    protected String datasetName;
    protected ArrayList<String> storage; // this is a list of Sequence names
    private ArrayList<Sequence> payload=null; // this is used to temporarily store Sequences that should be registered with the engine
    private String fromMap=null; // a configuration string if this collection is based on a map. Format example: "<mapname>,value>=0" or "mapname,value in [0,1]"
    private String fromList=null; // a configuration string if this collection is based on a (non-resolved) list of references
    private String fromStatistic=null; // a configuration string if this collection is based on a statistical function

    /**
     * Constructs a new initially "empty" Sequence collection with the given name
     * 
     * @param datasetName A name for this dataset
     */
   public SequenceCollection(String datasetName) {
       this.datasetName=datasetName;
       storage=new ArrayList<String>(20);
   }
   
//    /**
//     * Constructs a new Sequence collection with the given name and sequences
//     * The provided list should contain valid sequence names
//     * 
//     * @param datasetName A name for this dataset
//     */
//   public SequenceCollection(String datasetName, ArrayList<String> sequenceNames) {
//       this.datasetName=datasetName;
//       storage=(ArrayList<String>)sequenceNames.clone();
//   }
//   
   
    /**
     * Specifies a new name for this dataset
     * 
     * @param name the name for this dataset
     */
    public void setName(String name) {
        this.datasetName=name;
    }
    
    @Override
    public void rename(String name) {
        setName(name);
    }   
    
    @Override
    public String getName() {
        return datasetName;
    }
    
    @Override
    public Object getValue() {return this;} // should maybe change later
  
    @Override
    public String getValueAsParameterString() {
        if (hasConstructorString()) return getFullConstructorString();
        else {
            StringBuilder string=new StringBuilder();
            for (int i=0;i<storage.size();i++) {
                if (i==storage.size()-1) string.append(storage.get(i));
                else {
                    string.append(storage.get(i));
                    string.append(',');
                }
            }
            return string.toString();
        }
    }  

    /**
     * Returns the names of all the Sequences in this dataset
     *
     * @return A list of Sequence names (note that the order might be 'random')
     */
    @Override
    public ArrayList<String> getValues() {
        return storage;
    }
    
    @Override
    public String[] getResultVariables() {
        return new String[]{"size","random X","random X%","sequence:size","sequence:<property>"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (!hasResult(variablename)) throw new ExecutionError("'" + getName() + "' does not have a result for '" + variablename + "'");
        else if (variablename.equalsIgnoreCase("size")) {
            NumericVariable result=new NumericVariable("size",size());
            return result;           
        } else if (variablename.startsWith("random")) {
            String[] parts=variablename.split("\\s+",2);
            if (parts.length==1) return new SequenceCollection("randomCollection");
            String configString=Operation_new.RANDOM_PREFIX+parts[1]+" from "+getName();
            try {
              SequenceCollection randomCollection=SequenceCollection.parseSequenceCollectionParameters(configString, "randomCollection", null, engine, null);
              return randomCollection;
            } catch (Exception e) {
                if (e instanceof ExecutionError) throw (ExecutionError)e;
                else return null;
            }  
        } else if (variablename.startsWith("sequence:")) {
            String propertyName=variablename.substring("sequence:".length());
            if (propertyName.equals("<property>")) throw new ExecutionError("You must replace <property> with the actual name of a sequence property");
            Class propclass=Sequence.getPropertyClass(propertyName, engine);
            if (propclass==null) throw new ExecutionError("'"+propertyName+"' is not a recognized sequence property");
            DataMap map=null;
            if (Number.class.isAssignableFrom(propclass)) map=new SequenceNumericMap("map_"+propertyName, 0);
            else map=new SequenceTextMap("map_"+propertyName, "");
            for (String sequencename:storage) {
                Sequence sequence=(Sequence)engine.getDataItem(sequencename, Sequence.class);
                if (sequence==null) continue;
                Object value=sequence.getPropertyValue(propertyName, engine);
                if (value instanceof Number && map instanceof SequenceNumericMap) ((SequenceNumericMap)map).setValue(sequencename, ((Number)value).doubleValue());
                else if (value!=null && map instanceof SequenceTextMap) {
                    if (value instanceof List) ((SequenceTextMap)map).setValue(sequencename,MotifLabEngine.splice((List)value, ","));
                    else ((SequenceTextMap)map).setValue(sequencename,value.toString());
                }
            }
            return map;
        } else return null;
    }

    @Override
    public Class getResultType(String variablename) {
        if (!hasResult(variablename)) {
            return null;
        } else if (variablename.equalsIgnoreCase("size") ) {
            return NumericVariable.class;
        } else if (variablename.startsWith("random") ) {
            return SequenceCollection.class;            
        } else if (variablename.startsWith("sequence:") ) {
            String propertyName=variablename.substring("sequence:".length());
            if (propertyName.equals("<property>")) return SequenceTextMap.class; // just in case
            Class propclass=Sequence.getPropertyClass(propertyName, MotifLabEngine.getEngine());
            if (propclass==null) return SequenceTextMap.class; // just in case
            else if (Number.class.isAssignableFrom(propclass)) return SequenceNumericMap.class;
            else return SequenceTextMap.class;            
        } else return null;
    }

    @Override
    public boolean hasResult(String variablename) {
        if ( variablename.equalsIgnoreCase("size")) return true;
        else if ( variablename.startsWith("random")) return true;        
        else if ( variablename.startsWith("sequence:")) return true;        
        return false;
    }  
    
    
    
    @Override
    public Class getMembersClass() {
        return Sequence.class;
    }    

    /**
     * Returns the names of all the Sequences in this dataset
     * The order of the sequences is regarded as the "default sort order"
     * and reflects the way the sequences are sorted in the GUI.
     * @return A list of Sequence names
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getAllSequenceNames() {
        return (ArrayList<String>)storage.clone();
    }    
    
    /**
     * Returns all the Sequence objects in this dataset
     * (if they are currently registered with the engine)
     * in the order they were registered in this collection
     * @return A list of Sequence objects (note that the order might be 'random')
     */
    public ArrayList<Sequence> getAllSequences(MotifLabEngine engine) {
        ArrayList<Sequence> list = new ArrayList<Sequence>(storage.size());
        for (String name:storage) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Sequence) list.add((Sequence)item);
        }
        return list;
    }    
    
    /**
     * Returns all the Sequence objects in this dataset
     * (if they are currently registered with the engine)
     * in the same order as they are listed in the default sequence collection
     * @return A list of Sequence objects
     */
    public ArrayList<Sequence> getAllSequencesInDefaultOrder(MotifLabEngine engine) {
        ArrayList<Sequence> list = new ArrayList<Sequence>(storage.size());
        ArrayList<String> allnames=engine.getDefaultSequenceCollection().getAllSequenceNames();
        for (String name:allnames) {
            if (!storage.contains(name)) continue;
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Sequence) list.add((Sequence)item);
        }
        return list;
    }    
    

             
    /**
     * Returns the Sequence corresponding to the given name.
     * 
     * @param name The name of the sequence
     * @return the specified sequence (if found) or null
     */
    public Sequence getSequenceByName(String name, MotifLabEngine engine) {
        for (String sequencename : storage) {
            if (sequencename.equals(name)) {
                Data item=engine.getDataItem(name); 
                if (item!=null && item instanceof Sequence) return (Sequence)item;
            }
        }
        return null;
    }
    
    /**
     * Returns the name of the Sequence corresponding to the given index.
     * This method could be used if you want to iterate through all sequences
     * in a dataset. 
     * 
     * @param index The index of the sequence
     * @return the name of the Sequence at the specified index (if exists) or null
     */
    public String getSequenceNameByIndex(int index) {
        if (index<0 || index>=storage.size()) return null;
        return storage.get(index);
    }
    /**
     * Returns the Sequence corresponding to the given index.
     * This method could be used if you want to iterate through all sequences
     * in a dataset. 
     * 
     * @param index The index of the sequence
     * @return the specified sequence (if exists) or null
     */
    public Sequence getSequenceByIndex(int index, MotifLabEngine engine) {
        if (index<0 || index>=storage.size()) return null;
        String name=storage.get(index);
        Sequence sequence=null;
        if (name!=null) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Sequence) return (Sequence)item;            
        }
        return sequence;
    }
    
    /**
     * Returns the index (order within the dataset) of the sequence with the given name 
     * If no sequence with the given name exists within the dataset the value -1 is returned.
     * 
     * @param name The name of the sequence
     * @return index of the sequence (between 0 and size-1) or -1
     */
    public int getIndexForSequence(String name) {
        for (int i=0;i<storage.size();i++) {
            String sequenceName=storage.get(i);
            if (sequenceName.equals(name)) return i;
        }
        return -1;
    }
    
    /** 
     * Returns true if the specified Sequence is in this collection 
     */
    public boolean contains(Sequence sequence) {
        if (sequence==null) return false;
        String sequenceName=sequence.getName();
        return contains(sequenceName);
    }
    
    @Override
    public boolean contains(String sequenceName) {
        for (String name:storage) {
            if (name.equals(sequenceName)) return true;
        }
        return false;
    }
    
    /**
     * Returns true if this SequenceCollection objects contains the same sequences
     * as the given collection (or is based on the same map-criteria)
     * @param collection
     * @return
     */
    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof SequenceCollection)) return false;
        SequenceCollection collection =(SequenceCollection)other;
        if (!this.hasSameConstructor(collection)) return false;
        if (size()!=collection.size()) return false;
        for (String name:storage) {
            if (!collection.contains(name)) return false;
        }
        return true;
    }
    
   /**
     * Adds a new Sequence object to the dataset.
     * 
     * @param sequence The Sequence to be added
     */
    public void addSequence(Sequence sequence) {
        if (!storage.contains(sequence.getName())) {
            storage.add(sequence.getName()); // add to local storage
            notifyListenersOfDataAddition(sequence);
        }
    }
    
   /**
     * Adds the name of a sequence to this collection (if it is not already present)
     * Note that this method is not the preferred way of adding sequences.
     * It is requested that one addSequence(Sequence sequence) instead since that method
     * will also notify any listeners that the collection has been updated
     * @param name The name of the sequence to be added
     */
    public void addSequenceName(String name) {
        if (!storage.contains(name)) storage.add(name); //
    }
    
   /**
     * Adds the names of sequences to this collection (if they are not already present)
     * Note that this method is not the preferred way of adding sequences.
     * It is requested that one uses addSequence(Sequence sequence) instead since that method
     * will also notify any listeners that the collection has been updated
     * @param names The names of the sequences to be added
     */
    public void addSequenceNames(ArrayList<String> names) {
        for (String name:names) {
          if (!storage.contains(name)) storage.add(name);      
        }
    }       
    
   /**
     * Removes a Sequence object from the dataset.
     * 
     * @param sequence The Sequence to be removed
     */
    public void removeSequence(Sequence sequence) {
        storage.remove(sequence.getName()); // remove from local storage
        notifyListenersOfDataRemoval(sequence);
    }
    
    
    @Override
    public void clearAll(MotifLabEngine engine) {
        String[] list=new String[storage.size()];
        for (int i=0;i<list.length;i++) {list[i]=storage.get(i);}
        for (String name:list) {
            storage.remove(name);
            Data item=null;
            if (engine!=null) item=engine.getDataItem(name);
            if (item!=null && item instanceof Sequence) notifyListenersOfDataRemoval((Sequence)item);
        }
        this.fromMap=null;
        this.fromList=null;
        this.fromStatistic=null;
        this.clearConstructorString();
    }
    
    
    /**
     * Reorders the list of sequences in the collection by moving the sequence
     * at the specified current position to the new position. 
     * 
     * @param oldposition Position of the sequence to be moved
     * @param newposition New position to move sequence to
     */
    public void reorderSequences(int currentposition, int newposition) {
        String temp=storage.remove(currentposition);
        storage.add(newposition,temp);
        notifyListenersOfDataReorder(currentposition,newposition);
    }   
    
    /** Sets the order of sequences to the provided list. 
     *  The list must contain the same sequences as the original collection
     */
    public void setSequenceOrder(String[] order) {
         storage.clear();
         storage.addAll(Arrays.asList(order));
         notifyListenersOfDataReorder(null,null);
    }
    
    /** Sets the order of sequences to the provided list. 
     *  The list must contain the same sequences as the original collection
     */
    public void setSequenceOrder(ArrayList<String> order) {
         storage.clear();
         for(String s:order) storage.add(s);
         notifyListenersOfDataReorder(null,null);
    }
    
    /**
     * Returns the number of sequences in this dataset
     * 
     * @return number of sequences
     */
    public int getNumberofSequences() {
        return storage.size();
    }
    
    /**
     * Returns the number of sequences in this dataset (same as getNumberofSequence)
     * 
     * @return number of sequences
     */
    @Override
    public int size() {
        return storage.size();
    }
    
    /** Returns TRUE is this Sequence Collection has no sequences*/
    @Override
    public boolean isEmpty() {
        return storage.isEmpty();
    }
    

    
    /**
     * This method takes all sequences in the specified dataset and adds them
     * to this dataset
     * @param dataset The dataset to be incorporated into this dataset
     */
    public void merge(SequenceCollection dataset) {
        int size=dataset.getNumberofSequences();
        for (int i=0;i<size;i++) {
            String data=dataset.getSequenceNameByIndex(i);
            if (!contains(data)) storage.add(data); // I use this directly instead of calling addSequence() to limit the number of notifications sent 
        }              
        notifyListenersOfDataAddition(dataset);
    }
    
    @Override
    public void importData(Data source) throws ClassCastException {
        if (source==this) return; // no need to import, the source and target are the same
        SequenceCollection datasource=(SequenceCollection)source;
        this.datasetName=datasource.datasetName;
        this.fromMap=datasource.fromMap;
        this.fromList=datasource.fromList;
        this.fromStatistic=datasource.fromStatistic;
        this.cloneConstructor(datasource);
        storage.clear();
        for (String seqName:datasource.storage) {
            storage.add(seqName);
        }
    }
    
    @Override
    public SequenceCollection clone() {
        SequenceCollection newcollection= new SequenceCollection(datasetName);
        int size=getNumberofSequences();
        for (int i=0;i<size;i++) {
            String seqName=getSequenceNameByIndex(i);
            newcollection.storage.add(seqName); // I use this directly instead of calling addSequence() to limit the number of notifications sent 
        }
        newcollection.fromMap=this.fromMap;
        newcollection.fromList=this.fromList;
        newcollection.fromStatistic=this.fromStatistic;
        newcollection.cloneConstructor(this);
        return newcollection;
    }
       
    

    public static String getType() {return typedescription;}
    
    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription+" : "+storage.size();}


    /** Sets a string used for initialization of this collection based on which Sequences satisfy a condition in a SequenceNumericMap */
    public void setFromMapString(String fromMapString) {
        setConstructorString(Operation_new.FROM_MAP_PREFIX, fromMapString);      
    }   
    /** If this collection is based on which Sequences satisfy a condition in a SequenceNumericMap, this method will return a string describing the settings used for initialization */
    public String getFromMapString() {
        return getConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    /** Returns TRUE if this SequenceCollection is based on which Sequences satisfy a condition in a SequenceNumericMap */
    public boolean isFromMap() {
        return hasConstructorString(Operation_new.FROM_MAP_PREFIX);
    }
    
    /** If this collection is based on a (non-resolved) list of references, this method will return a string describing the settings used for initialization */
    public String getFromListString() {
        return getConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** Sets a string used for initialization of this collection (which includes references to sequences, collections, and partition clusters) */
    public void setFromListString(String liststring) {
        setConstructorString(Operation_new.FROM_LIST_PREFIX, liststring);        
    }
    /** Returns TRUE if this collection is based a (non-resolved) list of references (which could include references to collections and partition-clusters) */
    public boolean isFromList() {
        return hasConstructorString(Operation_new.FROM_LIST_PREFIX);
    }
    /** If this collection is based on a property condition, this method will return a string describing the settings used for initialization */
    public String getFromPropertyString() {
        return getConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }
    
    /** Returns TRUE if this SequenceCollection is based on a property condition */
    public boolean isFromProperty() {
        return hasConstructorString(Operation_new.FROM_PROPERTY_PREFIX);
    }    
    
    public void setFromPropertyString(String fromPropertyString) {
        setConstructorString(Operation_new.FROM_PROPERTY_PREFIX,fromPropertyString);      
    }
    
    /** Sets a string used for initialization of this collection based on a statistical condition */
    public void setFromStatisticString(String statisticString) {
        setConstructorString(Operation_new.FROM_STATISTIC_PREFIX, statisticString);         
    }
    /** If this collection is based on which Sequences satisfy a statistical condition, this method will return a string describing the settings used for initialization */
    public String getFromStatisticString() {
        return getConstructorString(Operation_new.FROM_STATISTIC_PREFIX);
    }
    /** Returns TRUE if this SequenceCollection is based on which Sequences satisfy a statistical condition */
    public boolean isFromStatistic() {
        return hasConstructorString(Operation_new.FROM_STATISTIC_PREFIX);
    }
    
    /**
     * Returns an unsorted list containing taxonomy IDs for all organisms that 
     * are represented in this Sequence Collection
     */
    public int[] getOrganisms(MotifLabEngine engine) {
        HashSet<Integer> organisms=new HashSet<Integer>();
        for (String name:storage) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Sequence) {
                int id=((Sequence)item).getOrganism();
                organisms.add(new Integer(id));
            }
        }
        int[] results=new int[organisms.size()];
        int i=0;
        for (Integer id:organisms) {
            results[i]=id.intValue();
            i++;
        }
        return results;
    }

    /**
     * Returns an unsorted list containing information about distinct genome builds
     * that are present among the sequences in this Sequence Collection
     */
    public String[] getGenomeBuilds(MotifLabEngine engine) {
        HashSet<String> organisms=new HashSet<String>();
        for (String name:storage) {
            Data item=engine.getDataItem(name); 
            if (item!=null && item instanceof Sequence) {
                String build=((Sequence)item).getGenomeBuild();
                organisms.add(build);
            }
        }
        String[] results=new String[organisms.size()];
        int i=0;
        for (String string:organisms) {
            results[i]=string;
            i++;
        }
        return results;
    }

    
    
    @Override
    public String output() {
        StringBuilder string=new StringBuilder();
        for (int i=0;i<storage.size();i++) {
            string.append(storage.get(i));
            string.append("\n");
        }
        return string.toString();
    }

    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        clearAll(engine);
        for (String line:input) {
            line=line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            Data data=engine.getDataItem(line);
            if (data==null) engine.logMessage("No such sequence: "+line); // should this throw ParseError?
            else if (!(data instanceof Sequence)) engine.logMessage("'"+line+"' is not a Sequence object"); // should this throw ParseError?
            else addSequence((Sequence)data);
        }
    }

    public void addSequenceToPayload(Sequence sequence) {
        if (payload==null) payload=new ArrayList<Sequence>();
        payload.add(sequence);
    }

    @Override
    public void initializeFromPayload(MotifLabEngine engine) throws ExecutionError {
        if (payload==null) return;
        for (Sequence seq:payload) {        
            engine.updateDataItem(seq);
            this.addSequence(seq);
        }
        payload=null; // clear payload after initialization!
    }

    @Override
    public boolean hasPayload() {
        if (payload==null) return false;
        else return !payload.isEmpty();
    }


    /**
     * Initialize this collection based on which sequences satisfy a condition in a SequenceNumericMap
     *
     * @param map The SequenceNumericMap used as basis for the condition
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in, not in )
     * @param firstOperand A string representing a number used for comparisons. This could be a literal numeric constant or the name of a data object
     * @param secondOperand A string representing a second number used as upper limit if operator is "in". This could be a literal numeric constant or the name of a data object
     */
   public void createCollectionFromMap(SequenceNumericMap map, String operator, String firstOperand, String secondOperand, MotifLabEngine engine) throws ExecutionError {
       Object firstOperandData=null;
       Object secondOperandData=null;
       if (firstOperand==null || firstOperand.isEmpty()) throw new ExecutionError("Missing numeric operand for comparison");
       firstOperandData=engine.getNumericDataForString(firstOperand);
       if (firstOperandData==null) throw new ExecutionError("'"+firstOperand+"' is not a numeric constant or known numeric data object");
       if ((firstOperandData instanceof Data) && !(firstOperandData instanceof NumericConstant || firstOperandData instanceof NumericVariable || firstOperandData instanceof SequenceNumericMap)) throw new ExecutionError("'"+firstOperand+"' is of a type not applicable in this context");
       if (operator.equals("in") || operator.equals("not in")) {
           if (secondOperand==null) throw new ExecutionError("Missing upper limit for numeric range");
           secondOperandData=engine.getNumericDataForString(secondOperand);
           if (secondOperandData==null) throw new ExecutionError("'"+secondOperand+"' is not a numeric constant or known numeric data object");
           if ((secondOperandData instanceof Data) && !(secondOperandData instanceof NumericConstant || secondOperandData instanceof NumericVariable || secondOperandData instanceof SequenceNumericMap)) throw new ExecutionError("'"+secondOperand+"' is of a type not applicable in this context");
       }     
       storage.clear();
       // configure
       String fromMapString=null;
       if (operator.equals("in") || operator.equals("not in")) {
          fromMapString=map.getName()+" "+operator+" ["+firstOperand+","+secondOperand+"]"; // sets the 'config' string
       } else {
          fromMapString=map.getName()+""+operator+""+firstOperand; // sets the 'config' string
       }      
       setFromMapString(fromMapString);
       ArrayList<Data> allSequences=engine.getAllDataItemsOfType(Sequence.class);
       for (Data sequence:allSequences) {
           String sequenceName=sequence.getName();
           if (sequenceSatisfiesCondition(sequenceName, map, operator, firstOperandData, secondOperandData)) storage.add(sequenceName);
       }
   }

    /**
     * Initialize this collection based on which sequences satisfy a statistical condition
     *
     * @param statisticalString A string describing the statistical function. The "statistic" operation will be used to create a SequenceNumericMap based on this function
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in, not in )
     * @param firstOperand A string representing a number used for comparisons. This could be a literal numeric constant or the name of a data object
     * @param secondOperand A string representing a second number used as upper limit if operator is "in". This could be a literal numeric constant or the name of a data object
     */
   public void createCollectionFromStatistic(String statisticalString, String operator, String firstOperand, String secondOperand, MotifLabEngine engine, final OperationTask monitorTask) throws ExecutionError {
       Object firstOperandData=null;
       Object secondOperandData=null;
       if (firstOperand==null || firstOperand.isEmpty()) throw new ExecutionError("Missing numeric operand for comparison");
       firstOperandData=engine.getNumericDataForString(firstOperand);
       if (firstOperandData==null) throw new ExecutionError("'"+firstOperand+"' is not a numeric constant or known numeric data object");
       if ((firstOperandData instanceof Data) && !(firstOperandData instanceof NumericConstant || firstOperandData instanceof NumericVariable || firstOperandData instanceof SequenceNumericMap)) throw new ExecutionError("'"+firstOperand+"' is of a type not applicable in this context");
       if (operator.equals("in") || operator.equals("not in")) {
           if (secondOperand==null) throw new ExecutionError("Missing upper limit for numeric range");
           secondOperandData=engine.getNumericDataForString(secondOperand);
           if (secondOperandData==null) throw new ExecutionError("'"+secondOperand+"' is not a numeric constant or known numeric data object");
           if ((secondOperandData instanceof Data) && !(secondOperandData instanceof NumericConstant || secondOperandData instanceof NumericVariable || secondOperandData instanceof SequenceNumericMap)) throw new ExecutionError("'"+secondOperand+"' is of a type not applicable in this context");
       }
       storage.clear();
       // configure
       String fromStatisticString=null;
       if (operator.equals("in") || operator.equals("not in")) {
          fromStatisticString="("+statisticalString+")"+" "+operator+" ["+firstOperand+","+secondOperand+"]"; // sets the 'config' string
       } else {
          fromStatisticString="("+statisticalString+")"+operator+""+firstOperand; // sets the 'config' string
       }
       setFromStatisticString(fromStatisticString);
       Operation_statistic statop=(Operation_statistic)engine.getOperation("statistic");
       StandardOperationParser_statistic parser=new StandardOperationParser_statistic();
       parser.setEngine(engine);
       OperationTask task=null;
       try {
           task=parser.parseInternal(statisticalString);
       } catch (ParseError e) {throw new ExecutionError("Unable to parse statistic argument: "+e.getMessage(),e);}
       task.setParameter(OperationTask.ENGINE, engine);
       task.setParameter(OperationTask.OPERATION, statop);
       task.setParameter(Operation_statistic.INITIATE_ALL_TO_ZERO, Boolean.TRUE);
       task.setParameter(Operation_statistic.FOR_INTERNAL_USE, Boolean.TRUE);
       if (monitorTask!=null) {
            final OperationTask innerTask=task;
            innerTask.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(ExecutableTask.PROGRESS)) {
                    Object value=evt.getNewValue();
                    if (value instanceof Integer) monitorTask.setProgress((Integer)value);
                    if (innerTask.isAborted()) monitorTask.setStatus(ExecutableTask.ABORTED);                    
                }
            }
        });
       }       
       try {
           statop.execute(task);
       } catch (Exception e) {throw new ExecutionError("An exception occurred while evaluating statistical function", e);}
       SequenceNumericMap map=(SequenceNumericMap)task.getParameter(Operation_statistic.RESULT);
       ArrayList<Data> allSequences=engine.getAllDataItemsOfType(Sequence.class);
       for (Data sequence:allSequences) {
           String sequenceName=sequence.getName();
           if (sequenceSatisfiesCondition(sequenceName, map, operator, firstOperandData, secondOperandData)) storage.add(sequenceName);
       }
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
       else if (operator.equals("not in")) return (!(sequenceValue>=firstOperand && sequenceValue<=secondOperand));
       else return false;
   }

/**
 * Parses a configuration string for Sequence Collection creation from a Numeric map
 * and returns an String[]  containing parsed elements:
 * [0] Name of SequenceNumericMap 
 * [1] Operator
 * [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 * @param configString. Format example: "SequenceNumericMap >= 0.4" or "SequenceNumericMap in [0,10]" or "SequenceNumericMap <> NumericVariable1"
 * @return
 */
   public static String[] parseMapConfigurationString(String configstring, MotifLabEngine engine) throws ParseError {
       Pattern pattern=Pattern.compile("\\s*([a-zA-Z_0-9]+)\\s*(>=|>|=|<=|<>|<| in | not in )\\s*\\[?\\s*([\\w\\d\\.\\-]+)(\\s*,\\s*([\\w\\d\\.\\-]+))?\\s*\\]?");
       Matcher matcher=pattern.matcher(configstring);
       String mapName=null;
       String operator=null;
       String firstOperand=null;
       String secondOperand=null;
       if (matcher.find()) {
           mapName=matcher.group(1);
           operator=matcher.group(2).trim();
           firstOperand=matcher.group(3);
           if (matcher.group(5)!=null && !matcher.group(5).isEmpty()) { // second operand
               secondOperand=matcher.group(5);
           }
           if ((operator.equals("in") || operator.equals("not in")) && secondOperand==null) throw new ParseError("Missing upper limit for numeric range");
       } else throw new ParseError("Unable to parse 'Map' parameter for new Sequence Collection");
       return new String[]{mapName,operator,firstOperand,secondOperand};
   }

/**
 * Parses a configuration string for Sequence Collection creation from a Property
 * and returns an Object[] containing parsed elements:
 * [0] Name of Property 
 * [1] Operator
 * [2] List of operands, as a String[]
 * @param configString. Format example: "chromosome = X" or "length in 1000,1500"
 * @return
 */
   public static Object[] parsePropertyConfigurationString(String configstring, MotifLabEngine engine) throws ParseError {
       Pattern pattern=Pattern.compile("\\s*(\\S.*?)\\s*( not in | in | not equals | equals | not matches | matches |>=|<=|<>|>|=|<)(.+)");
       Matcher matcher=pattern.matcher(configstring);
       String propertyName=null;
       String operator=null;
       String[] operands=null;
       if (matcher.find()) {
           propertyName=matcher.group(1).trim();
           operator=matcher.group(2).trim();
           String rest=matcher.group(3);
           if (rest==null || rest.trim().isEmpty()) throw new ParseError("Missing value for 'Property' parameter");
           operands=MotifLabEngine.splitOnCommaToArray(rest); // rest.trim().split("\\s*,\\s*");         
       } else throw new ParseError("Unable to parse 'Property' parameter for new Sequence Collection");
       if (propertyName.startsWith("\"") && propertyName.endsWith("\"")) propertyName=propertyName.substring(1,propertyName.length()-1);
       for (int i=0;i<operands.length;i++) { // remove quotes
           if (operands[i].startsWith("\"") && operands[i].endsWith("\"")) operands[i]=operands[i].substring(1,operands[i].length()-1);
       }
       return new Object[]{propertyName,operator,operands};
   }      
   
/**
 * Parses a configuration string for Sequence Collection creation from a statistical condition
 * and returns an String[]  containing parsed elements:
 * [0] A String describing the statistical function (can be parsed with StandardOperationParser_statistic if the prefix "x = statistic " is added in front of the string)
 * [1] Operator
 * [2] First operand (this could be a literal numeric constant or the name of a data object (or another string))
 * [3] Second operand (this could be a literal numeric constant or the name of a data object (or another string))
 * @param configString. Format example: "(..some statistic...) >= 0.4" or "(..some statistic...) in [0,10]" or "(..some statistic...) <> NumericVariable1"
 * @return
 */
   public static String[] parseStatisticConfigurationString(String configstring, MotifLabEngine engine) throws ParseError {
       configstring=configstring.trim();
       if (!configstring.startsWith("(")) throw new ParseError("The statistical function should be enclosed in parenthesis");
       int openP=1;
       String statisticalString=null;
       for (int i=1;i<configstring.length();i++) {
          if (configstring.charAt(i)=='(') openP++;
          else if (configstring.charAt(i)==')') {
              openP--;
              if (openP==0) {
                  statisticalString=configstring.substring(1,i);
                  if (i+1<configstring.length()) configstring=configstring.substring(i+1);
                  else configstring="";
                  break;
              }
          }
       }
       if (statisticalString==null) throw new ParseError("Unable to parse statistical function (possibly caused by unmatched parentheses)");
       Pattern pattern=Pattern.compile("\\s*(>=|>|=|<=|<>|<| in | not in )\\s*\\[?\\s*([\\w\\d\\.\\-]+)(\\s*,\\s*([\\w\\d\\.\\-]+))?\\s*\\]?");
       Matcher matcher=pattern.matcher(configstring);
       String operator=null;
       String firstOperand=null;
       String secondOperand=null;
       if (matcher.find()) {
           operator=matcher.group(1);
           firstOperand=matcher.group(2);
           if (matcher.group(4)!=null && !matcher.group(4).isEmpty()) { // second operand
               secondOperand=matcher.group(4);
           }
           if ((operator.equals("in") || operator.equals("not in")) && secondOperand==null) throw new ParseError("Missing upper limit for numeric range");
       } else throw new ParseError("Unable to parse 'Statistic' parameter for new Sequence Collection");
       return new String[]{statisticalString,operator,firstOperand,secondOperand};
   }

/**
 * Creates and returns a new SequenceCollection based on a parameter string
 * @param text The parameter string to be parsed
 * @param targetName The new name of the SequenceCollection (only used if a new collection is created)
 * @param notfound If this parameter is NULL the method will throw an ExecutionError if any entries in the parameter list is not found (the first error will be reported)
 *                 If this parameter is an (empty) ArrayList, the method will be run in 'silent-mode' and not throw exceptions upon encountering errors.
 *                 The ArrayList will be filled with the names that could not be properly resolved (with the reason in parenthesis),
 *                 and the returned collection will only contain those sequences that could successfully be resolved.
 * @param engine
 * @return
 * @throws ExecutionError
 * @throws InterruptedException
 */
public static SequenceCollection parseSequenceCollectionParameters(String text, String targetName, ArrayList<String> notfound, MotifLabEngine engine, OperationTask monitorTask) throws ExecutionError, InterruptedException {
    boolean silentMode=false;
    if (notfound!=null) {silentMode=true;notfound.clear();}
    SequenceCollection collection=new SequenceCollection(targetName);

    if (text.startsWith(Operation_new.FROM_MAP_PREFIX)) {
           String configstring=text.substring(Operation_new.FROM_MAP_PREFIX.length());
           String mapName="";
           String operator="";
           String firstOperand=null; 
           String secondOperand=null; 
           try {
               String[] parseElements=parseMapConfigurationString(configstring,engine);
               mapName=(String)parseElements[0];
               operator=(String)parseElements[1];
               firstOperand=(String)parseElements[2];
               if (parseElements[3]!=null) secondOperand=(String)parseElements[3];
           } catch (ParseError e) {
               throw new ExecutionError(e.getMessage());
           }
           Data numericMap=engine.getDataItem(mapName);
           if (numericMap==null) throw new ExecutionError("Unknown data object '"+mapName+"'");
           if (!(numericMap instanceof SequenceNumericMap)) throw new ExecutionError("'"+mapName+"' is not a Sequence Numeric Map");
           collection.createCollectionFromMap((SequenceNumericMap)numericMap, operator, firstOperand, secondOperand, engine);
           return collection;
        } else if (text.startsWith(Operation_new.FROM_PROPERTY_PREFIX)) {
           String configstring=text.substring(Operation_new.FROM_PROPERTY_PREFIX.length());
           String propertyName="";
           String operator="";
           String[] operands=null;
           try {
               Object[] parseElements=parsePropertyConfigurationString(configstring,engine);
               propertyName=(String)parseElements[0];
               operator=(String)parseElements[1];
               operands=(String[])parseElements[2];
           } catch (ParseError e) {
               throw new ExecutionError(e.getMessage());
           }
           collection.createCollectionFromProperty(propertyName, operator, operands, engine);
           return collection;
        } else if (text.startsWith(Operation_new.FROM_STATISTIC_PREFIX)) {
           String configstring=text.substring(Operation_new.FROM_STATISTIC_PREFIX.length());
           String statisticString="";
           String operator="";
           String firstOperand=null;
           String secondOperand=null;
           try {
               String[] parseElements=parseStatisticConfigurationString(configstring,engine);
               statisticString=(String)parseElements[0];
               operator=(String)parseElements[1];
               firstOperand=(String)parseElements[2];
               if (parseElements[3]!=null) secondOperand=(String)parseElements[3];
           } catch (ParseError e) {
               throw new ExecutionError(e.getMessage());
           }
           collection.createCollectionFromStatistic(statisticString, operator, firstOperand, secondOperand, engine, monitorTask);
           return collection;
        } else if (text.startsWith(Operation_new.RANDOM_PREFIX)) {
            String configstring=text.substring(Operation_new.RANDOM_PREFIX.length()).trim();
            String[] parts=configstring.split("\\s+from\\s+|\\*s,\\s*");                        
            if (parts[0].isEmpty()) throw new ExecutionError("Missing size specification for random collection");
            double number=0;
            boolean percentage=false;
            if (parts[0].endsWith("%")) {parts[0]=parts[0].substring(0,parts[0].length()-1).trim();percentage=true;}
            Data data=engine.getDataItem(parts[0]);
            if (data!=null) {
                if (data instanceof NumericVariable) number=((NumericVariable)data).getValue();
                else throw new ExecutionError("'"+parts[0]+"' is not a valid integer number or Numeric Variable");
            } else {
                try {
                    number=Double.parseDouble(parts[0]);
                } catch (NumberFormatException e) {throw new ExecutionError("'"+parts[0]+"' is not a valid literal number or Numeric Variable");}
            }
            ArrayList<String> names=null;
            if (parts.length>1) {
                Data sourceCollection=engine.getDataItem(parts[1]);
                if (sourceCollection instanceof SequenceCollection) names=((SequenceCollection)sourceCollection).getAllSequenceNames();
                else throw new ExecutionError("'"+parts[1]+"' is not a Sequence Collection");
            } else {
                names=engine.getNamesForAllDataItemsOfType(Sequence.class);
            }
            if (percentage) number=number*(double)names.size()/100.0;
            number=Math.round(number);            
            if (number<names.size()) {
                Collections.shuffle(names);
                for (int i=0;i<number;i++) collection.addSequenceName(names.get(i));
            } else {
                collection.addSequenceNames(names);
            }
            return collection;
        } else {   
           boolean keepConfig=false; // set this flag to true if the original config-string should be retained
           if (text.startsWith(Operation_new.FROM_LIST_PREFIX)) {
               text=text.substring(Operation_new.FROM_LIST_PREFIX.length());
               keepConfig=true;
           }
           String[] list=text.split("\\s*,\\s*");
           for (String entry:list) {
               int mode=Operation_new.UNION;
               if (entry.equals("+") || entry.equals("-") || entry.equals("&") || entry.equals("!")) if (silentMode) {notfound.add("Operator '"+entry+"' must be immediately followed by identifier (no space inbetween)");continue;} else throw new ExecutionError("Operator '"+entry+"' must be immediately followed by identifier (no space inbetween)");               
               if (entry.startsWith("+") && entry.length()>1) {mode=Operation_new.UNION;entry=entry.substring(1);}
               if (entry.startsWith("-") && entry.length()>1) {mode=Operation_new.SUBTRACTION;entry=entry.substring(1);}
               if (entry.startsWith("&") && entry.length()>1) {mode=Operation_new.INTERSECTION;entry=entry.substring(1);}
               if (entry.startsWith("!") && entry.length()>1) {mode=Operation_new.COMPLEMENT;entry=entry.substring(1);}
               Data dataobject=null;
               if (entry.trim().isEmpty()) continue;
               if (entry.contains("->")) { // entry refers to a cluster within a Sequence Partition
                   String[] elements=entry.split("->");
                   if (elements.length!=2) {
                       if (silentMode) {notfound.add(entry+" : syntax error");continue;} else throw new ExecutionError("Syntax error: "+entry);
                   }
                   String partition=elements[0];
                   String cluster=elements[1];
                   dataobject=engine.getDataItem(partition);
                        if (dataobject==null) {if (silentMode) {notfound.add(partition+" : Unknown data item"); continue;} else throw new ExecutionError("Unknown data item: "+partition);}
                   else if (!(dataobject instanceof SequencePartition)) {if (silentMode) {notfound.add(partition+" : Not a Sequence Partition"); continue;} else throw new ExecutionError("Data item '"+partition+"' is not a Sequence Partition");}
                   else if (!((SequencePartition)dataobject).containsCluster(cluster)) {if (silentMode) {notfound.add(entry+" : No such cluster"); continue;} else throw new ExecutionError("The Sequence Partition '"+partition+"' does not contain a cluster with the name '"+cluster+"'");}
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
                   SequenceCollection tempCollection=new SequenceCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addSequence((Sequence)object);
                   }
                   combineWithCollection(collection,tempCollection,mode,engine);
                   continue; 
               } else if (entry.matches(".*\\W.*")) { // contains non-word characters (not letter,number or underscore)
                   if (entry.contains(".")) entry=entry.replace(".", "\\."); // escape dot (since it is allowed in sequence names)
                   if (entry.contains("-")) entry=entry.replace("-", "\\-"); // escape - (since it is allowed in sequence names)
                   if (entry.contains("+")) entry=entry.replace("+", "\\+"); // escape + (since it is allowed in sequence names)         
                   if (entry.contains("*")) entry=entry.replace("*", ".*"); // convert wildcard * to proper regex
                   ArrayList<Data> regexmatches=engine.getAllDataItemsOfTypeMatchingExpression(entry, Sequence.class);
                   SequenceCollection tempCollection=new SequenceCollection("_temp");
                   for (Data object:regexmatches) {
                       tempCollection.addSequence((Sequence)object);
                   }
                   combineWithCollection(collection,tempCollection,mode,engine);
                   continue; 
               } else { // entry refers one sequence or a sequence collection
                   dataobject=engine.getDataItem(entry);
               }
               
                    if (dataobject==null) {if (silentMode) notfound.add(entry+" : Unknown data item"); else throw new ExecutionError("Unknown data item: "+entry);}
               else if (dataobject instanceof SequencePartition) {if (silentMode) notfound.add(entry+" : Missing cluster for Sequence Partition"); else throw new ExecutionError("Missing specification of cluster for Sequence Partition '"+entry+"'. (use format: Partition.Cluster)");}
               else if (!(dataobject instanceof Sequence || dataobject instanceof SequenceCollection)) {if (silentMode) notfound.add(entry+" : Not a Sequence or Sequence Collection"); else throw new ExecutionError("Data item '"+entry+"' is not a Sequence or Sequence Collection");}
               else combineWithCollection(collection,dataobject,mode,engine);
           }
           if (keepConfig) collection.setFromListString(text); // Store original config-string in data object
           return collection;
        }
    }

    private static void combineWithCollection(SequenceCollection col, Data dataobject, int mode, MotifLabEngine engine) {
            if (mode==Operation_new.SUBTRACTION) subtractFromSequenceCollection(col,dataobject,engine);
       else if (mode==Operation_new.INTERSECTION) intersectWithSequenceCollection(col,dataobject,engine);
       else if (mode==Operation_new.COMPLEMENT) addComplementToSequenceCollection(col,dataobject,engine);
       else addToSequenceCollection(col,dataobject,engine);
    }


    /** Adds a single Sequence or Sequence collection (other) to a target collection */
    private static void addToSequenceCollection(SequenceCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Sequence) {
            target.addSequence((Sequence)other);
        } else if (other instanceof SequenceCollection) {
            for (Sequence seq:((SequenceCollection)other).getAllSequences(engine)) {
                target.addSequence(seq);
            }
        } else {
            System.err.println("SYSTEM ERROR: In SequenceCollection.addToSequenceCollection. Parameter is neither Sequence nor Sequence Collection but rather: "+other.getClass().getSimpleName());
        }
    }

    /** Removes all sequences from the target collection which is not present in the other Sequence collection (or single sequence) */
    private static void intersectWithSequenceCollection(SequenceCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Sequence) {
            if (target.contains((Sequence)other)) {
                target.clearAll(engine);
                target.addSequence((Sequence)other);
            } else target.clearAll(engine);
        } else if (other instanceof SequenceCollection) {
            for (Sequence seq:target.getAllSequences(engine)) {
                if (!((SequenceCollection)other).contains(seq)) target.removeSequence(seq);
            }
        } else {
            System.err.println("SYSTEM ERROR: In SequenceCollection.intersectWithSequenceCollection. Parameter is neither Sequence nor Sequence Collection but rather: "+other.getClass().getSimpleName());
        }

    }
     /** Subtracts a single Sequence or Sequence collection (other) from a target collection */
    private static void subtractFromSequenceCollection(SequenceCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Sequence) {
            target.removeSequence((Sequence)other);
        } else if (other instanceof SequenceCollection) {
            for (Sequence seq:((SequenceCollection)other).getAllSequences(engine)) {
                target.removeSequence(seq);
            }
        } else {
            System.err.println("SYSTEM ERROR: In SequenceCollection.subtractFromSequenceCollection. Parameter is neither Sequence nor Sequence Collection but rather: "+other.getClass().getSimpleName());
        }
    }

 /** Adds the complement of a single Sequence or Sequence Collection (other) to a target collection */
    private static void addComplementToSequenceCollection(SequenceCollection target, Object other, MotifLabEngine engine) {
        if (other==null) return;
        if (other instanceof Sequence) {
            for (Data sequence:engine.getAllDataItemsOfType(Sequence.class)) {
                if (sequence!=other) target.addSequence((Sequence)sequence);
            }
        } else if (other instanceof SequenceCollection) {
            for (Data sequence:engine.getAllDataItemsOfType(Sequence.class)) {
                if (!((SequenceCollection)other).contains((Sequence)sequence)) target.addSequence((Sequence)sequence);
            }
        } else {
            System.err.println("SYSTEM ERROR: In SequenceCollection.addComplementToMotifCollection. Parameter is neither Sequence nor Sequence Collection but rather: "+other.getClass().getSimpleName());
        }
    }

    
     /**
     * Initialize this collection based on which sequences that satisfy a condition with respect to a specified property
     *
     * @param property The name of the property 
     * @param operator A string describing the condition operator (should be one of the following: =, >, =, >, <, <=, <>, in, not in, equals, not equals, matches, not matches )
     * @param operands A list of possible values for the property
      */
   public void createCollectionFromProperty(String property, String operator, String[] operands, MotifLabEngine engine) throws ExecutionError {
       Class typeclass=Sequence.getPropertyClass(property,engine);
       if (typeclass==null) throw new ExecutionError("Unknown sequence property: "+property);
       if (operands==null || operands.length==0) throw new ExecutionError("Missing property value");       
       storage.clear();
       Object[] resolvedoperands=null;
       if (Number.class.isAssignableFrom(typeclass)) { // either a single operand or two for ranges
           resolvedoperands=new Object[operands.length]; // resolve all operands, irrespective of the operator used
           for (int i=0;i<operands.length;i++) {
               resolvedoperands[i]=engine.getNumericDataForString(operands[i]);     
               if (resolvedoperands[i]==null) throw new ExecutionError("Property value '"+operands[i]+"' is not a numeric constant or applicable numeric data object");           
               if (resolvedoperands[i] instanceof NumericMap && !(resolvedoperands[i] instanceof SequenceNumericMap)) throw new ExecutionError("Property value '"+operands[i]+"' is of a type not applicable in this context");               
           }
           if (operator.equals("in") || operator.equals("not in")) {
               if (operands.length!=2) throw new ExecutionError("The operator '"+operator+"' requires exactly two operand values (for lower and upper range limits) when applied to numeric properties");
           } else if (!operator.equals("=") && operands.length!=1) throw new ExecutionError("The operator '"+operator+"' requires a single operand value when applied to numeric properties");
       } else if (typeclass.equals(Boolean.class)) {   
           if (!(operator.equals("=") || operator.equals("<>"))) throw new ExecutionError("The operator should be either '=' or '<>' for boolean properties");
           if (operands.length!=1 || getBooleanValue(operands[0])==null) throw new ExecutionError("The property value should be a single YES/NO or TRUE/FALSE value for boolean properties");          
           resolvedoperands=new Object[]{getBooleanValue(operands[0])};
       } else if (typeclass.equals(String.class) || typeclass.equals(ArrayList.class)) {
           if (!(operator.equals("equals") || operator.equals("not equals") || operator.equals("matches")|| operator.equals("not matches")|| operator.equals("in")|| operator.equals("not in"))) throw new ExecutionError("The operator should be either '(not) equals', '(not) matches' or '(not) in' for text based properties");
           if (operator.equals("in") || operator.equals("not in")) { // the operands should be names of Text Variables
               resolvedoperands=new Object[operands.length];
               for (int i=0;i<operands.length;i++) {
                   String operand=operands[i];
                   Data data=engine.getDataItem(operand);
                   if (data instanceof TextVariable) resolvedoperands[i]=data;
                   else throw new ExecutionError("When using the '"+operator+"' operator with text based properties, the property values should refer to Text Variables");
               }
           } else {
               resolvedoperands=operands;
               for (int i=0;i<resolvedoperands.length;i++) {
                   resolvedoperands[i]=MotifLabEngine.addQuotes((String)resolvedoperands[i]); // add double quotes around strings
               } 
           } // use literal string values
       } else throw new ExecutionError("SLOPPY PROGRAMMING ERROR: class '"+typeclass+"' not recognized by SequenceCollection.createCollectionFromProperty()");
       // configure 
       String spliced=MotifLabEngine.splice(operands, ",");
       StringBuilder builder=new StringBuilder();
       builder.append(property);
       if (operator.matches("[\\w\\s]+")) builder.append(" "+operator+" ");
       else builder.append(operator);
       builder.append(spliced);
       setFromPropertyString(builder.toString());// sets the 'config' string

       ArrayList<Data> allSequences=engine.getAllDataItemsOfType(Sequence.class);
       for (Data sequence:allSequences) {
           Object value=null;
           String sequenceName=sequence.getName();
           try {
              value=((Sequence)sequence).getPropertyValue(property,engine); 
              if (value==null) continue; // do not include sequences which do not have a value for the property (no matter which operator is used)
           } catch (Exception e) {continue;}
           boolean satisfies=false;
           if (Boolean.class.equals(typeclass)) satisfies=sequenceSatisfiesBooleanPropertyCondition(value,operator,(Boolean)resolvedoperands[0]);
           else if (Number.class.isAssignableFrom(typeclass)) satisfies=sequenceSatisfiesNumericPropertyCondition(value,sequenceName,operator,resolvedoperands);
           else satisfies=sequenceSatisfiesStringPropertyCondition(value,operator,resolvedoperands);
           if (satisfies) storage.add(sequenceName);
       }   
   }    
   
   private double resolveNumericValue(Object value, String sequencename) throws ExecutionError{
            if (value instanceof Integer) return ((Integer)value).doubleValue();
       else if (value instanceof Double) return ((Double)value).doubleValue();
       else if (value instanceof NumericVariable) return ((NumericVariable)value).getValue();
       else if (value instanceof SequenceNumericMap) return ((SequenceNumericMap)value).getValue(sequencename);
       else if (value instanceof Data) throw new ExecutionError("Data object '"+value+"' can not be used in this context");
       else throw new ExecutionError("'"+value+"' is not a numeric constant or known data object");
   }
   
   private boolean sequenceSatisfiesBooleanPropertyCondition(Object sequencePropertyValue, String operator, boolean targetValue) {
       if (sequencePropertyValue instanceof Boolean) {
          if (operator.equals("=")) return ((Boolean)sequencePropertyValue)==targetValue;
          else return ((Boolean)sequencePropertyValue)!=targetValue;
       } else return false;
   }   
   
   private boolean sequenceSatisfiesNumericPropertyCondition(Object sequencePropertyValue, String sequencename, String operator, Object[] operands) throws ExecutionError {
       double sequencevalue=0;
            if (sequencePropertyValue instanceof Integer) sequencevalue=((Integer)sequencePropertyValue).doubleValue();
       else if (sequencePropertyValue instanceof Double)  sequencevalue=((Double)sequencePropertyValue).doubleValue();
       else return false;
       double firstOperandValue=resolveNumericValue(operands[0],sequencename);
       double secondOperandValue=(operands.length>1 && operands[1]!=null)?resolveNumericValue(operands[1],sequencename):0;
            if (operator.equals("=") && operands.length==1) return sequencevalue==firstOperandValue;
       else if (operator.equals("=")) return sequenceNumericValueInList(sequencevalue,operands,sequencename);            
       else if (operator.equals("<")) return sequencevalue<firstOperandValue;
       else if (operator.equals("<=")) return sequencevalue<=firstOperandValue;
       else if (operator.equals(">")) return sequencevalue>firstOperandValue;
       else if (operator.equals(">=")) return sequencevalue>=firstOperandValue;
       else if (operator.equals("<>")) return sequencevalue!=firstOperandValue;
       else if (operator.equals("in")) return (sequencevalue>=firstOperandValue && sequencevalue<=secondOperandValue);
       else if (operator.equals("not in")) return !(sequencevalue>=firstOperandValue && sequencevalue<=secondOperandValue);
       else throw new ExecutionError("Unknown operator for numeric comparison '"+operator+"'");
   } 
   
   private boolean sequenceNumericValueInList(double sequenceValue, Object[] operands, String sequencename) throws ExecutionError {
       for (Object operand:operands) {
           double operandValue=(operand!=null)?resolveNumericValue(operand,sequencename):0;
           if (sequenceValue==operandValue) return true;
       }
       return false;
   }
   
   @SuppressWarnings("unchecked")
   private boolean sequenceSatisfiesStringPropertyCondition(Object sequencePropertyValue, String operator, Object[] operands) throws ExecutionError {
       if (sequencePropertyValue instanceof String) {
           String value=(String)sequencePropertyValue;
           boolean isMatch=(operator.equals("matches") || operator.equals("not matches"))?stringPropertyMatches(value,operands):stringPropertyEquals(value, operands);
           if (operator.startsWith("not")) return !isMatch;
           else return isMatch; // 
       } else if (sequencePropertyValue instanceof ArrayList) {
           boolean isMatch=false;
           for (String value:(ArrayList<String>)sequencePropertyValue) {
              isMatch=(operator.equals("matches") || operator.equals("not matches"))?stringPropertyMatches(value,operands):stringPropertyEquals(value, operands);
              if (isMatch) break;
           }
           if (operator.startsWith("not")) return !isMatch;
           else return isMatch; //         
       } else return false;
   }    
   
   /** Returns TRUE if the sequence value is found among the operand values else false */
   private boolean stringPropertyMatches(String sequencevalue, Object[] operands) {
        for (Object operand:operands) {
           if (operand instanceof String) {
               String target=( ((String)operand).startsWith("\"") && ((String)operand).endsWith("\"") && ((String)operand).length()>2)?((String)operand).substring(1,((String)operand).length()-1):(String)operand;
               if (sequencevalue.matches("(?i)"+target)) return true; // case insensitive match         
           }                
           else if (operand instanceof TextVariable && ((TextVariable)operand).matches(sequencevalue)) return true;
        }
        return false;
   }
   /** Returns TRUE if the sequence value is found among the operand values else false */
   private boolean stringPropertyEquals(String sequencevalue, Object[] operands) {
        for (Object operand:operands) {
           if (operand instanceof String) {
               String target=( ((String)operand).startsWith("\"") && ((String)operand).endsWith("\"") && ((String)operand).length()>2)?((String)operand).substring(1,((String)operand).length()-1):(String)operand;
               if (sequencevalue.equalsIgnoreCase(target)) return true;         
           } 
           else if (operand instanceof TextVariable && ((TextVariable)operand).contains(sequencevalue)) return true;
        }
        return false;
   }   
   
   private Boolean getBooleanValue(String string) {
            if (string.equalsIgnoreCase("TRUE") || string.equalsIgnoreCase("YES")) return Boolean.TRUE;
       else if (string.equalsIgnoreCase("FALSE") || string.equalsIgnoreCase("NO")) return Boolean.FALSE;
       else return null;
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
         else if (this.fromStatistic!=null) this.setConstructorString(Operation_new.FROM_STATISTIC_PREFIX, this.fromStatistic);                
 
         // clear legacy fields
         this.fromStatistic=null;
         this.fromMap=null; 
         this.fromList=null;                
    }

}

