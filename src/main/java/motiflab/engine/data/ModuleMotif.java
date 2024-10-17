/*
 
 
 */

package motiflab.engine.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import motiflab.engine.MotifLabEngine;

/**
 * This class represents constituent single Motifs of a ModuleCRM (CRM)
 Each constituent motif can be represented by several "equivalent" Motif models (usually different models for the same TF)
 Note that ModuleMotif is not a subclass of Data and can thus not be stored directly in the engine.
 * @author Kjetil Klepper
 */
public class ModuleMotif implements Serializable, Cloneable {
    MotifCollection equivalenceClass;
    int orientation;
    String representativeName; // a representative name for this motif equivalence class
    
    /**
     * Creates a new ModuleMotif based on the given collection of motif models
     * @param name
     * @param motifs
     * @param orientation
     */
    public ModuleMotif(String name, MotifCollection motifs, int orientation) {
        this.representativeName=name;
        this.equivalenceClass=motifs;
        this.orientation=orientation;
    }

    /**
     * Creates a new ModuleMotif based on a single motif model
     * @param name
     * @param singlemotif
     * @param orientation
     */
    public ModuleMotif(String name, Motif singlemotif, int orientation) {
        this.representativeName=name;
        this.equivalenceClass=new MotifCollection(name+"Col");
        if (singlemotif!=null) this.equivalenceClass.addMotif(singlemotif);
        this.orientation=orientation;
    }

    public ModuleMotif(String name, Collection<String> motifnames, int orientation) {
        this.representativeName=name;
        this.equivalenceClass=new MotifCollection(name+"Col");
        if (motifnames!=null) this.equivalenceClass.addMotifNames(motifnames);
        this.orientation=orientation;
    }    
    
    /**
     * Returns a representative name for this motif (usually the name of the TF which
     * corresponds to this constituent motif, or a generic name like "Motif3"
     * @return
     */
    public String getRepresentativeName() {
        return representativeName;
    }

    @Override
    public String toString() {
        return representativeName;
    }

    /** Returns the orientation constraint imposed on this ModuleMotif
  This could be ModuleCRM.DIRECT, ModuleCRM.REVERSE or ModuleCRM.INDETERMINED (if no specific constraing is set)
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * Returns this constituent motif as either a single Motif or a Motif Collection
     * of "equivalent" motifs (depending on the number of motif models in the equivalence cluster)
     * @param engine
     * @return
     */
    public Data getMotifs(MotifLabEngine engine) {
        if (equivalenceClass==null || equivalenceClass.isEmpty()) return null;
        else if (equivalenceClass.size()>1) return equivalenceClass.clone();
        else return equivalenceClass.getMotifByIndex(0, engine);
    }

    /**
     * Returns this motif as a Motif Collection of equivalent motifs
     * (or NULL if no motifs are selected for this ModuleMotif)
     * @return
     */
    public MotifCollection getMotifAsCollection() {
        if (equivalenceClass==null || equivalenceClass.isEmpty()) return null;
        else return equivalenceClass.clone();
    }
    
    /**
     * Returns a list of Motif IDs for all equivalent motifs that make
     * up this ModuleMotif
     * @return
     */
    public ArrayList<String> getAllMotifIDs() {
        if (equivalenceClass==null || equivalenceClass.isEmpty()) return new ArrayList<String>();
        else return equivalenceClass.getAllMotifNames();
    }   
    
    /**
     * Returns a list of Motif IDs for all equivalent motifs that make
     * up this ModuleMotif
     * @return
     */
    public ArrayList<String> getAllMotifShortNames(MotifLabEngine engine) {
        if (equivalenceClass==null || equivalenceClass.isEmpty()) return new ArrayList<String>();
        else return equivalenceClass.getAllMotifShortNames(engine);
    }      

    /**
     * Returns the first Motif in the collection of equivalent motifs for this ModuleMotif
     * (or NULL if no motifs are selected for this ModuleMotif)
     * @return
     */
    public Motif getFirstMotifInCollection(MotifLabEngine engine) {
        if (equivalenceClass==null || equivalenceClass.isEmpty()) return null;
        else return equivalenceClass.getMotifByIndex(0, engine);
    }

    /** Renames all references to a single motif in the equivalence class 
     *  of motifs represented by this ModuleMotif
     */
    public void renameSingleMotifReference(String oldname, String newname) {
        if (equivalenceClass==null || equivalenceClass.isEmpty()) return;
        equivalenceClass.renameMotifReference(oldname, newname);
    }

    /** Renames all references to single motifs in the equivalence class
     *  of motifs represented by this ModuleMotif based on the given mapping
     *  of names from old names to new names
     */
    public void renameSingleMotifReferences(HashMap<String, String> namesmapping) {
        if (equivalenceClass==null || equivalenceClass.isEmpty()) return;
        equivalenceClass.renameMotifReferences(namesmapping);
    }

    @Override
    public ModuleMotif clone() {
        ModuleMotif newmodulemotif=new ModuleMotif(representativeName,this.equivalenceClass.clone(),orientation);
        return newmodulemotif;
    }

    public void importData(ModuleMotif source) {
        this.representativeName=source.representativeName;
        this.orientation=source.orientation;
        this.equivalenceClass=source.equivalenceClass.clone();
    }


    @Override
    public boolean equals(Object other) {
        if (other==null || !(other instanceof ModuleMotif)) return false;
        if (!this.representativeName.equals(((ModuleMotif)other).representativeName)) return false;
        if (this.orientation!=((ModuleMotif)other).orientation) return false;
        if (!this.equivalenceClass.containsSameEntries(((ModuleMotif)other).equivalenceClass)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.equivalenceClass != null ? this.equivalenceClass.hashCode() : 0);
        hash = 29 * hash + this.orientation;
        hash = 29 * hash + (this.representativeName != null ? this.representativeName.hashCode() : 0);
        return hash;
    }

    public String getValueAsParameterString() {
        String orientationString;
        if (orientation==ModuleCRM.DIRECT) orientationString="+";
        else if (orientation==ModuleCRM.REVERSE) orientationString="-";
        else orientationString=".";
        String parameter="MOTIF("+representativeName+")["+orientationString+"]{";
        ArrayList<String> motifNames=equivalenceClass.getAllMotifNames();
        int size=motifNames.size();
        for (int i=0;i<size;i++) {
            if (i==size-1) parameter+=motifNames.get(i)+"}";
            else parameter+=motifNames.get(i)+",";
        }
        return parameter;
    }
    
    /** Returns the IC-content of the motif with the highest IC-content in this ModuleMotif*/
    public double getHighestICcontent(MotifLabEngine engine) {
         if (equivalenceClass==null || equivalenceClass.isEmpty()) return 0;
        else if (equivalenceClass.size()==1) { // single motif
            Motif motif=equivalenceClass.getMotifByIndex(0, engine);
            return motif.getICcontent();
        } else {
            double highIC=0;
            for (Motif m:equivalenceClass.getAllMotifs(engine)) {
                double thisIC=m.getICcontent();
                if (thisIC>highIC) highIC=thisIC;
            }
            return highIC;      
        }
    }
    
    /** Returns the IC-content of the motif with the lowest IC-content in this ModuleMotif*/
    public double getLowestICcontent(MotifLabEngine engine) {
         if (equivalenceClass==null || equivalenceClass.isEmpty()) return 0;
        else if (equivalenceClass.size()==1) { // single motif
            Motif motif=equivalenceClass.getMotifByIndex(0, engine);
            return motif.getICcontent();
        } else {
            double lowIC=Double.MAX_VALUE;
            for (Motif m:equivalenceClass.getAllMotifs(engine)) {
                double thisIC=m.getICcontent();
                if (thisIC<lowIC) lowIC=thisIC;
            }
            return lowIC;      
        }
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
    }
}