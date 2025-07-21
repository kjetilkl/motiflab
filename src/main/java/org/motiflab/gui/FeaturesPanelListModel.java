/*
 
 
 */

package org.motiflab.gui;

import java.util.Arrays;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.DataListener;
import javax.swing.DefaultListModel;
import org.motiflab.engine.MotifLabEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import javax.swing.SwingUtilities;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.FeatureDataset;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.engine.data.RegionDataset;


/**
 * This class implements a list model that backs up the lists of available
 * datasets (sequences and features) displayed in the left panels of the editor
 * 
 * The listmodel reflects the data that is currently available through the engine
 * and keeps its own ordering for visualization purposes
 * 
 * @author kjetikl
 */
public class FeaturesPanelListModel extends DefaultListModel implements DataListener {
    MotifLabEngine engine=null;
    Class classfilter=null;
    VisualizationSettings settings=null;
    private boolean groupbytype=false; // used when new features are added
    private boolean sortalphabetically=false; // used when new features are added 
    private SortOrderComparator sorter=new SortOrderComparator();
    
    public FeaturesPanelListModel(MotifLabEngine engine, Class classfilter, VisualizationSettings settings) {
        this.engine=engine;
        this.classfilter=classfilter;
        this.settings=settings;
        engine.addDataListener(this);
        // add all applicable dataitems currently in store (if any)
        ArrayList<Data> list=engine.getAllDataItemsOfType(classfilter);
        for (Data item:list) {dataAdded(item);}
    }
    
    public MotifLabEngine getEngine() {return engine;}

    public void addElementToList(Data data) {
        if (groupbytype || sortalphabetically) {
            int index=getSortedIndexForData(data);
            add(index, data);
        } else addElement(data); // add to end of list
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
     * This method is called by the engine when a new data item has been added to the pool
     * @param data
     */
    @Override
    public final void dataAdded(final Data data) {
        if (!classfilter.isInstance(data)) return;
        if (MotifLabEngine.isTemporary(data)) return; // do not show temporary data items
        final FeaturesPanelListModel panel=this;
        Runnable runner=new Runnable() {
            public void run() {
                if (indexOf(data)==-1) { // the track is not present already
                    int index=settings.getTrackOrderPosition(data.getName(),panel);
                    if (index<0) addElementToList(data); // not in the master list. Just add the data to the end
                    else add(index, data); // add at the position specified by the master list
                    settings.setupMasterTrackorderList(panel);
                }
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
        if (!classfilter.isInstance(data)) return;
        Runnable runner=new Runnable() {
            public void run() {
                boolean removed=removeElement(data);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);        
    }
    
    /**
     * This method is called by the engine when a data item has been updated
     * @param data
     */
    public void dataUpdated(final Data data) {
        if (!classfilter.isInstance(data)) return;
        Runnable runner=new Runnable() {
            public void run() {
                int index=indexOf(data);
                fireContentsChanged(this, index, index);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);   
    }
    
    public void dataUpdate(Data oldvalue, Data newvalue) {
        // check for data type conversions
    }    

    
    // The following two methods complete the DataListener interface, but we are not really interested in these events here
    public void dataAddedToSet(Data parent, Data child) {}
    public void dataRemovedFromSet(Data parent, Data child) {}
    
    /**
     * Relocates the specified dataset to the new index in the list
     * @param data The dataset to be relocated
     * @param index The new position index that the data should be relocated to
     * @param doSetup if TRUE the relocation will trigger a setup of the MasterTrackOrder list in VisualizationSettings
     *        Note that this setup should always be performed, however, the flag can be set to false if multiple relocations
     *        should be performed right after each other. If the flag is set to FALSE, it is necessary to manually call
     *        setupMasterTrackorderList() after all relocations have been performed
     */
    public void relocate(final Data data, final int index, final boolean doSetup) {
        final FeaturesPanelListModel panel=this;
        Runnable runner=new Runnable() {
            public void run() {
                removeElement(data);
                add(index, data);
                if (doSetup) settings.setupMasterTrackorderList(panel);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);   
    }


    public void setupMasterTrackorderList() {
        final FeaturesPanelListModel panel=this;
        if (SwingUtilities.isEventDispatchThread()) {
            settings.setupMasterTrackorderList(panel);
            return;
        } else {
            Runnable runner=new Runnable() {
                public void run() {
                    settings.setupMasterTrackorderList(panel);
                }
            };
            SwingUtilities.invokeLater(runner);
        }
    }
    
    
    /** Rearranges the tracks according to the order in the given list
     *  @param tracklist list of track names in correct order
     *         If a name in the list is not a track it will be skipped
     *         Track names missing from the tracklist will be sorted at the end
     */
    public void orderByTrackList(final String[] tracklist, final boolean updateMasterList) {
        HashSet<String> included=new HashSet<String>(tracklist.length);
        included.addAll(Arrays.asList(tracklist)); // keeps track of which names are explicitly included in the provided track order list
        final ArrayList<Data> additional=new ArrayList<Data>(this.size());        
        for (int i=0;i<this.getSize();i++) {
            Data feature=(Data)this.getElementAt(i);
            String name=feature.getName();         
            if (!included.contains(name)) additional.add(feature); // these are added at the bottom in the order listed currently in the panel
        }
        final FeaturesPanelListModel listmodel=this;     
        Runnable runner=new Runnable() {
            public void run() {
                listmodel.clear();
                for (String name:tracklist) {
                    Data feature=engine.getDataItem(name);
                    if (feature instanceof FeatureDataset) addElement(feature);            
                }
                // add the rest to the end
                for (Data feature:additional) {
                    if (feature!=null && engine.dataExists(feature.getName(), feature.getClass())) addElement(feature); 
                }
                // This method could potentially experience race conditions if data has been added to the engine but not yet been updated in the panel, so we check if there are additional feature datasets in the engine that are not in the panel so we have to add them at the bottom
                ArrayList<Data> alltracks=engine.getAllDataItemsOfType(FeatureDataset.class);
                for (Data feature:alltracks) {
                    if (feature.isTemporary()) continue;
                    if (!listmodel.contains(feature)) addElement(feature);
                }
                if (updateMasterList) settings.setupMasterTrackorderList(listmodel);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runner.run();
        else SwingUtilities.invokeLater(runner);          
    }    
    
    
    /**
     * This method searches through the data items in the list to see if any
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
    
    public boolean shouldSortAlphabetically() {
        return sortalphabetically;
    }
    public boolean shouldGroupByType() {
        return groupbytype;
    }
    public void setSortAlphabetically(boolean flag) {
        sortalphabetically=flag;
        if (sortalphabetically) {
            updateSortOrder();
        }
    }
    public void setGroupByType(boolean flag) {
        groupbytype=flag;
        if (groupbytype) {
            updateSortOrder();
        }        
    }    
        
    /** This method should be called when the settings for sorting/grouping changes to a more strict condition or when when a data object is renamed */
    public void updateSortOrder() {
        if (!(groupbytype || sortalphabetically)) return; // really no need to sort    
        int size=this.getSize();
        ArrayList<Data> list=new ArrayList<Data>();
        for (int i=0;i<size;i++) list.add((Data)this.get(i));
        Collections.sort(list, sorter);
        String[] tracknames=new String[size];         
        for (int i=0;i<size;i++) tracknames[i]=list.get(i).getName();
        orderByTrackList(tracknames,true);            
    } 
    
    private class SortOrderComparator implements Comparator<Data> {
        private Class[] order=new Class[]{DNASequenceDataset.class,NumericDataset.class,RegionDataset.class};
        @Override
        public int compare(Data data1, Data data2) { // these are two datanames
            Class data1class=data1.getClass();
            Class data2class=data2.getClass();
            boolean sameType=data1class.equals(data2class);
            if (groupbytype && !sameType) { // different types of data objects. Sort according to "order-list" for types
                int order1=getIndexForClass(data1class);
                int order2=getIndexForClass(data2class);
                return Double.compare(order1,order2); // must use Double since Integer does not have compare-method ?!
            } else { // compare names
                if (sortalphabetically) return MotifLabEngine.compareNaturalOrder(data1.getName(),data2.getName());
                else return 0; // keep current sorting
            }
        }
        /** Returns the index in the order-list for the given type, or Integer.MAX_VALUE if the class is not in the list */
        private int getIndexForClass(Class type) {
            for (int i=0;i<order.length;i++) if (order[i].equals(type)) return i;
            return Integer.MAX_VALUE;
        }                    
    }    

}
