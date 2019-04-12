/*
 
 
 */

package motiflab.engine;

import motiflab.engine.data.Data;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements a data storage facility that is used by the engine
 * to store all the data objects (feature datasets and other data objects) 
 * that have been registered with the engine
 * 
 * The data storage is decoupled from the MotifLabEngine class itself so
 * that it can be easily substituted at a later time to provide more sophisticated
 * storage if needed (such as support for caching/memory swapping). 
 * However, at present the data is just stored in a HashMap
 * 
 * @author kjetikl
 */
public class DataStorage {
    private volatile HashMap<String,Data> storage;

    /**
     * Returns a new instance of DataStorage
     */
    public DataStorage() {
        storage=new HashMap<String,Data>();
    }
            
    /**
     * Returns a data item from storage
     * @param key The name of the data to be retrieved
     */
    protected Data getDataItem(String key) {
      synchronized(storage){
        return storage.get(key);
      }
    }
    
    /**
     * Puts a new data item into storage
     * @param dataitem The data to be stored
     */ 
    protected void storeDataItem(Data dataitem) {
      synchronized(storage){
        storage.put(dataitem.getName(),dataitem);
      }
    }
    
    /**
     * Removes the dataitem corresponding to the given key from storage and returns it 
     * @param dataitem The specified dataitem if found, else null
     */ 
    protected Data removeDataItem(String key) {
      synchronized(storage){
          return storage.remove(key);
      }
    }
    
     /**
     * Renames the dataitem corresponding to the given key in storage 
     * @param oldname The specified dataitem if found, else null
     * @param newname The specified dataitem if found, else null
     * @return the Data item removed (if any) or null if no item with that name was found 
     */ 
    protected Data renameDataItem(String oldname, String newname) {
        Data item=null;
        synchronized(storage){
           item=storage.remove(oldname);
           if (item!=null) storage.put(newname, item);
        }
        return item;
    }
    
     /**
     * Returns an ArrayList containing all Data of the specified classtype
     * currently available in storage
     * 
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList of Data objects
     */
    protected ArrayList<Data> getAllDataItemsOfType(Class classtype) {
        ArrayList<Data> datalist=new ArrayList<Data>();
        synchronized(storage){
            for (Data element : storage.values()) {
               if (classtype.isInstance(element)) {
                   datalist.add(element);
               }
            }
        }
        return datalist;
    }

    /**
     * Returns an ArrayList containing all Data of the specified classtype
     * currently available in storage whose names match the given regular expression string
     * 
     * @param expression A regular expression string that data items should match
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList of Data objects
     */
    protected ArrayList<Data> getAllDataItemsOfTypeMatchingExpression(String expression, Class classtype) {
        ArrayList<Data> datalist=new ArrayList<Data>();
        synchronized(storage){
            for (Data element : storage.values()) {
               if (classtype.isInstance(element) && element.getName().matches(expression)) {
                   datalist.add(element);
               }
            }
        }
        return datalist;
    }
    
    /**
     * Returns an ArrayList containing all Data of the specified classtype
     * currently available in storage whose names contain numbers and matches 
     * a specific name expression string consisting of an optional prefix followed
     * by the number and an optional suffix. However, only data items whose number
     * component falls within a given range will be returned.
     * For example if the prefix is "sequence" the suffix is "" and the start and end
     * parameters are 100 and 130 respectively, all data items with names starting with 
     * the prefix "sequence" and followed by a number between 100 and 130 will be returned
     * The prefix and suffix strings could be regular expressions but should not contain capturing groups.
     * 
     * @param prefix A prefix (optionally empty) that the names of target data items should contain 
     * @param suffix A suffix (optionally empty) that the names of target data items should contain 
     * @param start The min value in the numeric range of data items to return
     * @param end The max value in the numeric range of data items to return
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList of Data objects
     */
    protected ArrayList<Data> getAllDataItemsOfTypeMatchingExpressionInNumericRange(String prefix, String suffix, int start, int end, Class classtype) {
        if (prefix==null) prefix="";
        if (suffix==null) suffix="";
        String expression=prefix+"(\\d+)"+suffix;
        Pattern pattern=Pattern.compile(expression);
        ArrayList<Data> datalist=new ArrayList<Data>();
        synchronized(storage){
            for (Data element : storage.values()) {
               if (classtype.isInstance(element) && element.getName().matches(expression)) {
                   Matcher matcher = pattern.matcher(element.getName());
                   if (matcher.matches()) { 
                       String numberstring=matcher.group(1);
                       try {
                           int number=Integer.parseInt(numberstring);
                           if (number>=start && number<=end) datalist.add(element);
                       } catch (NumberFormatException nfe) {} 
                   }
               }                                
            }
        }
        return datalist;
    }    


     /**
     * Returns an ArrayList containing names of all Data objects of the
     * specified classtype currently available in storage
     *
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList containing names of Data objects
     */
    protected ArrayList<String> getNamesForAllDataItemsOfType(Class classtype) {
        ArrayList<String> nameslist=new ArrayList<String>();
        synchronized(storage)  {
            for (Data element : storage.values()) {
               if (classtype.isInstance(element)) {
                   nameslist.add(element.getName());
               }
            }
        }
        return nameslist;
    }

     /**
     * Returns an ArrayList containing names of all Data objects of the
     * specified classtypes currently available in storage
     *
     * @param classtype the class of data to return (subclass of Data)
     * @return An ArrayList containing names of Data objects
     */
    protected ArrayList<String> getNamesForAllDataItemsOfTypes(Class[] classtypes) {
        ArrayList<String> nameslist=new ArrayList<String>();
        synchronized(storage)  {
            for (Data element : storage.values()) {
               for (Class classtype : classtypes) {
                   if (classtype.isInstance(element)) {nameslist.add(element.getName());break;}             
               }
            }
        }
        return nameslist;
    }

     /**
     * Returns TRUE if any Data object of the specified classtype is 
     * currently available in storage
     * 
     * @param classtype the class of data to return (subclass of Data)
     * @return 
     */
    protected boolean hasDataItemsOfType(Class classtype) {
        synchronized(storage){
            for (Data element : storage.values()) {
               if (classtype.isInstance(element)) {
                   return true;
               }
            }
        }
        return false;
    }
    
     /**
     * Returns the number of Data objects of the specified classtype currently available in storage
     * 
     * @param classtype the class of data to return (subclass of Data)
     * @return 
     */
    protected int countDataItemsOfType(Class classtype) {
        int counter=0;
        synchronized(storage){
            for (Data element : storage.values()) {
               if (classtype.isInstance(element)) {
                   counter++;
               }
            }
        }
        return counter;
    }
    
    
    
    /**
     * Returns the number of Data items currently stored
     * @return
     */
    public int getSize() {
        return storage.size();
    }
}
