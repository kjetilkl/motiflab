/*
 
 
 */

package org.motiflab.gui;

import org.motiflab.engine.data.Data;

/**
 *
 * @author kjetikl
 */
public interface VisualizationSettingsListener {  
        public static final int REORDERED=1;
        public static final int UPDATED=2;
        public static final int ADDED=3;
        public static final int REMOVED=4;
        public static final int SCALE_CHANGED=5;
        public static final int EXPANSION_CHANGED=6;
        public static final int VISIBILITY_CHANGED=7;   
        public static final int TRACK_HEIGHT_CHANGED=8;   
        public static final int FORCE_MAJURE=0; // not sure what is happening but we cannot assume that anything is OK anymore            
 
    
        /** 
         * Notifies listeners that an update has been made that could
         * affect the order that datatracks are visualized in within sequences
         * or the height of these tracks.
         */
        public void trackReorderEvent(int type, Data affected);
        
        /** 
         * Notifies listeners that an update has been made that could
         * affect the layout of the sequences.
         * This includes adding or removing datasets or sequences,
         * but not reordering of sequences (which is notified with sequencesReordered()
         */
        public void sequencesLayoutEvent(int type, Data affected);
        
        /** 
         * Notifies listeners that one or more sequences has been moved to a new place,
         * thus affecting the order in which the sequences should be displayed.
         * @param oldposition If a single sequence is moved, this is the index of the old position. If multiple sequences have been moved, this value should be NULL
         * @param newposition If a single sequence is moved, this is the index of the new position. If multiple sequences have been moved, this value should be NULL
         */
        public void sequencesReordered(Integer oldposition, Integer newposition);        
        
        /** 
         * Notifies listeners that the size of the sequence window has been changed.
         * All viewports should be updated accordingly to fit the new window
         */
        public void sequenceWindowSizeChangedEvent(int newsize, int oldsize);
        
        /** 
         * Notifies listeners that the size of the sequence margin has been changed
         */
        public void sequenceMarginSizeChangedEvent(int newsize);
        
        /** 
         * Notifies listeners that the name of a dataitem has changed
         */
        public void dataRenamedEvent(String oldname, String newname);
        

        
        
        
}
