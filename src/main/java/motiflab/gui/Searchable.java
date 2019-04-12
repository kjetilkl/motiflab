/*
 
 
 */

package motiflab.gui;

/**
 * This interface should be implemented by GUI components that supports FIND-functionality (and perhaps also REPLACE)
 * @author Kjetil
 */
public interface Searchable {

    /** */
    public boolean find(String searchstring);

    public boolean isSearchCaseSensitive();

    public void setSearchIsCaseSensitive(boolean flag);

    /** Returns TRUE if the component also supports REPLACE functionality  */
    public boolean supportsReplace();

    /** This method is called when searchAndReplace is initiated (usually by CTRL+R or from the Edit-menu)  */
    public void searchAndReplace();

   /** Replaces the currently found instance of the searchstring with the replacestring
    *  If the target searchstring is not the current selection the method will normally do nothing
    * @return TRUE if the searchstring was replaced
    */
    public boolean replaceCurrent(String searchstring, String replacestring);

  /** Replaces all instances of the searchstring with the replacestring
   *  @return Number of instances found and replaced
    */
    public int replaceAll(String searchstring, String replacestring);
    
    /**
     * Returns the text that is currently selected in the component as a String
     * or NULL if no text is currently selected
     * @return 
     */
    public String getSelectedTextForSearch();

}
