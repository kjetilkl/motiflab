/*
 
 
 */

package motiflab.gui;

import motiflab.engine.data.*;
import motiflab.engine.DataListener;
import javax.swing.DefaultListModel;
import motiflab.engine.MotifLabEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import javax.swing.SwingUtilities;
import motiflab.engine.data.analysis.Analysis;


/**
 * This class implements a list model that backs up the lists of available
 * datasets (sequences and features) displayed in the left panels of the editor
 * 
 * The list model reflects the data that is currently available through the engine
 * and keeps its own ordering for visualization purposes
 * 
 * @author kjetikl
 */
public class DataObjectsPanelListModel extends DefaultListModel implements DataListener {
    MotifLabEngine engine=null;
    Class classfilter[]=null;
    private SortOrderComparator sorter=new SortOrderComparator();
    private boolean groupbytype=true; // used when new features are added
    private boolean sortalphabetically=true; // used when new features are added     

    
    public DataObjectsPanelListModel(MotifLabEngine engine, Class[] classfilter) {
        this.engine=engine;
        this.classfilter=classfilter;
        engine.addDataListener(this);
        // add all applicable dataitems currently in store (if any)
        for (int i=0;i<classfilter.length;i++) {
              ArrayList<Data> list=engine.getAllDataItemsOfType(classfilter[i]);
              for (Data item:list) {
                  if (item==engine.getDefaultSequenceCollection() && engine.getDefaultSequenceCollection().isEmpty()) continue;
                  dataAdded(item);
              }
        }
    }
    
    public MotifLabEngine getEngine() {return engine;}

    public void addElementToList(Data data) {
        if (groupbytype || sortalphabetically) {
            int index=getSortedIndexForData(data);
            add(index, data);
        } else addElement(data); // add to end of list
    }

    /**
     * This method is called by the engine when a new data item has been added to the pool
     * @param data
     */
    @Override
    public final void dataAdded(final Data data) {        
        if (!isAccepted(data)) return;
        if (MotifLabEngine.isTemporary(data)) return; // do not show temporary data items
        // if (indexOf(data)>=0) return; // data item exists in list?      
        Runnable runner=new Runnable() {
            public void run() {
                if (indexOf(data)<0) addElementToList(data);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);                
    }
    
    /**
     * This method is called by the engine when a data item has been removed from the pool
     * @param data
     */
    @Override
    public void dataRemoved(final Data data) {
        if (!isAccepted(data)) return;      
        Runnable runner=new Runnable() {
            public void run() {
                removeElement(data);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);                 
    }
    
    /**
     * This method is called by the engine when a data item has been updated
     * @param data
     */
    @Override
    public void dataUpdated(final Data data) {
        if (!isAccepted(data)) return;
        Runnable runner=new Runnable() {
            public void run() {
                int index=indexOf(data);
                fireContentsChanged(this, index, index);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);                
    }

    @Override
    public void dataAddedToSet(final Data parent, Data child) {
        if (parent!=engine.getDefaultSequenceCollection()) return;
        //if (indexOf(parent)>=0) return; // DefaultSequenceCollection item exists in list
        Runnable runner=new Runnable() {
            public void run() {
                if (indexOf(parent)<0) addElementToList(parent); // add default sequence collection to the list if sequences have been added to it (and it was previously empty)
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);                
    }
    
    @Override
    public void dataRemovedFromSet(final Data parent, Data child) {
        if (parent!=engine.getDefaultSequenceCollection()) return;
        if (engine.getDefaultSequenceCollection().isEmpty()) { // do not show the default sequence collection anymore if it is empty
            Runnable runner=new Runnable() {
                public void run() {
                    removeElement(parent);
                }
            };
            if (SwingUtilities.isEventDispatchThread()) runner.run();
            else SwingUtilities.invokeLater(runner);             
        }        
    }
    
    @Override    
    public void dataUpdate(Data oldvalue, Data newvalue) {}     
    
//    /**
//     * Relocates the specified dataset to the new index in the list
//     * @param data The dataset to be relocated
//     * @param index The new position index that the data should be relocated to
//     */
//    public void relocate(final Data data,final int index) {
//        Runnable runner=new Runnable() {
//            public void run() {
//                removeElement(data);
//                add(index, data);
//           }
//        };
//        if (SwingUtilities.isEventDispatchThread()) runner.run();
//        else SwingUtilities.invokeLater(runner);
//    }
    
    /**
     * This method searches through the dataitems in the list to see if any
     * has a name that matches the given name. If found the data item will be
     * return, if not the method will return null.
     * @param name The name of the data item to be retrieved
     * @return Data The data item with the given name (if found) else null
     */
    public Data getDataByName(String name) {    
        for (Enumeration e=elements();e.hasMoreElements();) {
            Data data=(Data)e.nextElement();
            if (data.getName().equals(name)) return data;
        }
        return null;
        
    }

    /** Returns the index that the given data item should be placed at in the list if sorting is used */
    private int getSortedIndexForData(Data data) { // the argument is the data object to be inserted
        for (int i=0;i<size();i++) {
            Data other=(Data)get(i);
            int compare=sorter.compare(data, other);
            if (compare<0) return i; // add before this element
        }
        return size(); // add to the end
    }
    
    /**
     * This method returns TRUE if the supplied Data argument
     * is an instance of one of the classes accepted by this model
     */
    private boolean isAccepted(Data data) {
        for (int i=0;i<classfilter.length;i++) {
            if (classfilter[i].isInstance(data)) return true;
        }
        return false;
    }
    
    public boolean shouldSortAlphabetically() {
        return sortalphabetically;
    }
    public boolean shouldGroupByType() {
        return groupbytype;
    }
    public void setSortAlphabetically(boolean flag) {
        sortalphabetically=flag;
        if (sortalphabetically) updateSortOrder(); // 'activate' grouping      
    }
    public void setGroupByType(boolean flag) {
        groupbytype=flag;
        if (groupbytype) updateSortOrder(); // 'activate' grouping              
    }      

    private class SortOrderComparator implements Comparator<Data> {
        private Class[] order=new Class[]{SequenceCollection.class,SequencePartition.class, BackgroundModel.class, ExpressionProfile.class, NumericVariable.class, SequenceTextMap.class, MotifTextMap.class,ModuleTextMap.class,SequenceNumericMap.class, MotifNumericMap.class, ModuleNumericMap.class, TextVariable.class, PriorsGenerator.class, Analysis.class};
        @Override
        public int compare(Data data1, Data data2) { // these are two datanames
            Class data1class=data1.getClass();
            Class data2class=data2.getClass();
            boolean sameType=(data1 instanceof Analysis && data2 instanceof Analysis) || data1class.equals(data2class);
            if (groupbytype && !sameType) { // different types of data objects. Sort according to "order-list" for types
                int order1=getIndexForClass(data1class);
                int order2=getIndexForClass(data2class);
                return Double.compare(order1,order2); // must use Double since Integer does not have compare-method ?!
            } else { // compare names
                if (sortalphabetically) return MotifLabEngine.compareNaturalOrder(data1.getName(),data2.getName());
                else return 0; //
            }
        }
        /** Returns the index in the order-list for the given type, or Integer.MAX_VALUE if the class is not in the list */
        private int getIndexForClass(Class type) {
            for (int i=0;i<order.length;i++) if (order[i].equals(type)) return i;
            return Integer.MAX_VALUE;
        }                    
    }
    
    /** This method should be called when the settings for sorting/grouping changes to a more strict condition or when when a data object is renamed */
    public void updateSortOrder() {
        if (!(groupbytype || sortalphabetically)) return; // really no need to sort
        final DataObjectsPanelListModel lm=this;
        Runnable runner=new Runnable() {
            public void run() {        
                int size=lm.getSize();
                ArrayList<Data> list=new ArrayList<Data>();
                for (int i=0;i<size;i++) list.add((Data)lm.get(i));
                lm.clear();
                Collections.sort(list, sorter);
                for (int i=0;i<size;i++) lm.addElement(list.get(i));
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);               
    }
}
