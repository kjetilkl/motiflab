package org.motiflab.engine.data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @deprecated Legacy stub for deserialization only. Use ModuleCRM instead.
 *             The original Module class was renamed to avoid confusion with modules in Java 9+
 */
@Deprecated
public class Module implements Serializable {
    private String name=null; // usually accession. This is the name or ID used when referencing this ModuleCRM
    private ArrayList<ModuleMotif> singleMotifs;
    private ArrayList<int[]> distance; // space constraints int[] should have length 4.  2 first elements are single motif references (index from "singleMotifs"), the two next is min and max distance between the two single motifs.
    private int maxLength=0; // the maximum length span of the whole module. Applicable if maxLength>0;
    private boolean ordered=false; // is the order of motifs within the module important?
    private HashMap<String,Object> properties=null; // A list which stores key-value pairs for user-defined properties. These keys are always in UPPERCASE!
    private int[] GOterms=null; // The GO terms associated with this motif stored as numbers rather than strings  

    public String getName() { return name; }
    public ArrayList getSingleMotifs() { return singleMotifs; }
    public ArrayList<int[]> getDistance() { return distance; }
    public int getMaxLength() { return maxLength; }
    public boolean isOrdered() { return ordered; }
    public HashMap getProperties() { return properties; }
    public int[] getGOterms() { return GOterms; }
    
    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }
}