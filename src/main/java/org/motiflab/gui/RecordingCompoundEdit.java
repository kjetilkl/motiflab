/*
 
 
 */

package org.motiflab.gui;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CompoundEdit;

/**
 * This class represents objects that listen to undo events and add them to an
 * internal queue. All events that have occurred can then be undone as a single event.
 * The RecordingCompoundEdit has to be registered as an UndoableEditListener somewhere,
 * for instance as a "forward recipient" for events sent to the GUIUndoManager.
 * Note that the end() method must be called on this object after all events
 * have been recorded and before undo() is called, otherwise undo() will throw a 
 * CannotUndoException
 * 
 * @author kjetikl
 */
public class RecordingCompoundEdit extends CompoundEdit implements UndoableEditListener  {
    
        private String presentationname=null;

        public RecordingCompoundEdit() {
            super();
        }
        
        public RecordingCompoundEdit(String presentationname) {
            super();
            this.presentationname=presentationname;
        }

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
             this.addEdit(e.getEdit());
        }
        @Override
        public String getPresentationName() {
            if (presentationname==null) return super.getPresentationName();
            else return presentationname;
        }

        @Override
        public String getUndoPresentationName() { // default super implementation is to use presentationname of last edit in the queue
            if (presentationname==null) return super.getUndoPresentationName();
            else return "Undo "+presentationname;
        }

        @Override
        public String getRedoPresentationName() { // default super implementation is to use presentationname of last edit in the queue
             if (presentationname==null) return super.getRedoPresentationName();
             else return "Redo "+presentationname;
        }

        @Override
        public boolean isSignificant() {
            return true;
        }



 }