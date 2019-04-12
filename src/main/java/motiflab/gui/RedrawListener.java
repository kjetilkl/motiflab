package motiflab.gui;

/**
 * RedrawListeners can register with the GUI to get notifications
 * of forced redraw in response to changes in visualization settings etc.
 * This is mostly applicable to dialogs such as MotifBrowser or 
 * PositionalDistributionViewer that need to be updated to be consistent with
 * the rest of the GUI.
 * @author kjetikl
 */
public interface RedrawListener {
    
    public void redrawEvent();
}
