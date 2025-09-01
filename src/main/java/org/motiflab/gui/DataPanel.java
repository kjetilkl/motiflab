package org.motiflab.gui;

import javax.swing.JPanel;
import org.motiflab.engine.data.Data;

/**
 * An abstract superclass for FeaturesPanel, MotifsPanel and DataObjectsPanel
 * @author kjetikl
 */
public abstract class DataPanel extends JPanel implements Searchable {
    
    public abstract void clearSelection();
    
    public abstract void setHeaderVisible(boolean visible);
    
    /** Returns TRUE if the Panel can hold data items of the given type */
    public abstract boolean holdsType(String type);
    
    /** Returns TRUE if the Panel can hold the given data item */
    public abstract boolean holdsType(Data data);   
    
    /** Registers a PanelListener that can respond to events in this panel */
    public abstract void addDataPanelListener(PanelListener listener);
    
    /** Removes a registered PanelListener from this panel */
    public abstract void removeDataPanelListener(PanelListener listener);    
    
}
