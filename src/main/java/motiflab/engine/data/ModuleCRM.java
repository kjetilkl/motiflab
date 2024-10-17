/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.ParseError;


/**
 * This class represents cis-Regulatory Modules (aka "composite motifs")
 * Note that the class only represents a module "template" in the same
 * way that the Motif class represents a template for a binding site.
 * Actual occurrences of modules within sequences are represented with Region objects
 *
 * @author kjetikl
 */
public class ModuleCRM extends Data implements BasicDataType {
    public static final int DIRECT=1;
    public static final int REVERSE=-1;
    public static final int INDETERMINED=0;

    private static String typedescription="Module";
    private String name=null; // usually accession. This is the name or ID used when referencing this ModuleCRM
    private ArrayList<ModuleMotif> singleMotifs;
    private ArrayList<int[]> distance; // space constraints int[] should have length 4.  2 first elements are single motif references (index from "singleMotifs"), the two next is min and max distance between the two single motifs.
    private int maxLength=0; // the maximum length span of the whole module. Applicable if maxLength>0;
    private boolean ordered=false; // is the order of motifs within the module important?
    private HashMap<String,Object> properties=null; // A list which stores key-value pairs for user-defined properties. These keys are always in UPPERCASE!
    private int[] GOterms=null; // The GO terms associated with this motif stored as numbers rather than strings  
    private static final String[] reservedproperties=new String[]{"ID","Cardinality","Consensus","Motifs","Ordered","Oriented","Max length","Max IC", "Min IC","maxlength","GO", "gene ontology","ordered","unordered","distance"};

    private static transient HashMap<String,Class> userdefinedpropertyClasses=null; // contains a lookup-table defining the class-type of each user-defined property
     /**
     * Constructs a new Module object with the given name
     *
     * @param name
     */
    public ModuleCRM(String name){
         this.name=name;
         singleMotifs=new ArrayList<ModuleMotif>();
         distance=new ArrayList<int[]>();
    }

    @Override
    public String getName() {
        return name;
    }
    
    public String getNamePlusSingleMotifNames() {
        return name+":"+getSingleMotifNamesAsString(",");        
    }    

    /**
     * Sets the name of the module
     *
     * @param a name for this module
     */
    public void setName(String name) {
        this.name=name;
    }

    @Override
    public void rename(String newname) {
        setName(newname);
    }    
    
    /**
     * Returns the number of single motifs in this module
     */
    public int size() {
        return singleMotifs.size();
    }

    /**
     * Returns the number of single motifs in this module
     */
    public int getCardinality() {
        return singleMotifs.size();
    }

    /**
     * Returns the maximum length (span) allowed for this module type
     * If this value is 0 it means that the module can have any length
     * or the maximum length is rather determined by distance constraints
     * specified for each pair of consecutive motifs
     * @see getDistance
     * @return Maximum length of module or 0 if this is not set specifically
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Sets the maximum length (span) allowed for this module type
     * @param max
     */
    public void setMaxLength(int max) {
        maxLength=max;
    }

    /**
     * Set to TRUE if the order of motifs should be preserved in the module
     * or FALSE if the order of motifs is not important
     */
    public void setOrdered(boolean isordered) {
        ordered=isordered;
    }

    /**
     * Returns TRUE if the order of motifs should be preserved in the module
     * or FALSE if the order of motifs is not important
     */
    public boolean isOrdered() {
        return ordered;
    }
    
    /**
     * Returns TRUE if any of the motifs have specifically assigned orientations
     */
    public boolean isOriented() {
        for (ModuleMotif mm:singleMotifs) {
            if (mm.getOrientation()!=ModuleCRM.INDETERMINED) return true;
        }
        return false;
    }    
    
    @Override
    public Object getValue() {return this;} // should maybe change later

    @Override
    public String getValueAsParameterString() {
        StringBuilder parameter=new StringBuilder();
        parameter.append("CARDINALITY:"+size()+";");
        if (maxLength>0) parameter.append("MAXLENGTH:"+maxLength+";");
        parameter.append((ordered)?"ORDERED;":"UNORDERED;");
        for (ModuleMotif mm:singleMotifs) {
             parameter.append(mm.getValueAsParameterString()+";");
        }
        for (int[] constraint:distance) {
             String min=(constraint[2]==Integer.MIN_VALUE)?"UNLIMITED":""+constraint[2];
             String max=(constraint[3]==Integer.MAX_VALUE)?"UNLIMITED":""+constraint[3];
             String motif1=getSingleMotifName(constraint[0]);
             String motif2=getSingleMotifName(constraint[1]);
             parameter.append("DISTANCE("+motif1+","+motif2+","+min+","+max+");");
        }
        if (GOterms!=null) {parameter.append("GO-TERMS:");parameter.append(MotifLabEngine.splice(getGOtermsWithoutPrefix(),","));parameter.append(";");}           
        if (properties!=null) {
            for (String key:properties.keySet()) {
                String value=(String)getUserDefinedPropertyValueAsType(key,String.class);
                if (value!=null) {
                    parameter.append(key);
                    parameter.append(":");
                    parameter.append(value);
                    parameter.append(";");
                }
            }
        }
        return parameter.toString();
    }

    /** Returns a text string which conveys the structure/composition of this module (sort of like "consensus" for Motifs)*/
    public String getModuleLogo() {
        StringBuilder string=new StringBuilder();
        if (ordered) string.append("< "); else string.append("{ ");
        for (int i=0;i<getCardinality();i++) {
            if (i>0) { // append separator between motifs
                int[] constraint=getDistance(i-1, i);
                if (constraint==null) string.append(" , ");
                else {
                   string.append(" -[");
                   if (constraint[0]==Integer.MIN_VALUE) string.append("*"); else string.append(constraint[0]);
                   string.append(",");
                   if (constraint[1]==Integer.MAX_VALUE) string.append("*"); else string.append(constraint[1]);
                   string.append("]- ");
                }
            }
            ModuleMotif mm=singleMotifs.get(i);
            string.append(mm.getRepresentativeName());
            int orientation=mm.getOrientation();
            if (orientation==ModuleCRM.DIRECT) string.append("(+)");
            else if (orientation==ModuleCRM.REVERSE) string.append("(-)");
        }
        if (ordered) string.append(" >"); else string.append(" }");
        if (maxLength>0) {
            string.append("[");
            string.append(maxLength);
            string.append("]");
        }
        return string.toString();
    }

    /**
     * Returns a list of representative names for the constituent single motifs
     * of this module. The length of the list equals the cardinality (size) of the module
     */
    public ArrayList<String> getSingleMotifNames() {
        ArrayList<String> names = new ArrayList<String>();
        for (ModuleMotif motif:singleMotifs) {
            names.add(motif.getRepresentativeName());
        }
        return names;
    }

    /**
     * Returns a comma-separated list of representative names for the constituent single motifs
     * of this module. The length of the list equals the cardinality (size) of the module
     */
    public String getSingleMotifNamesAsString(String separator) {
        if (singleMotifs.isEmpty()) return "";
        StringBuilder builder=new StringBuilder();
        builder.append(singleMotifs.get(0).getRepresentativeName());
        for (int i=1;i<singleMotifs.size();i++) {
            builder.append(separator);
            builder.append(singleMotifs.get(i).getRepresentativeName());
        }
        return builder.toString();
    }

    /** 
     * Returns a representative name for the motif with the given index (starting at 0)
     * Note that this name could, but does not need to, refer to a single motif
     */
    public String getSingleMotifName(int index) {
        if (index<0 || index>=singleMotifs.size()) return null;
        else {
            ModuleMotif motif=singleMotifs.get(index);
            return motif.getRepresentativeName();
        }
    }

    /**
     * Returns a ModuleMotif representing the constituent single motif with the given index (starting at 0)
     */
    public ModuleMotif getSingleMotif(int index) {
        if (index<0 || index>=singleMotifs.size()) return null;
        else return singleMotifs.get(index);
    }
    /**
     * Returns the index of the motif with the given representative name
     * or -1 if no such motif is found
     */
    public int getIndexForMotifName(String motifname) {
        for (int i=0;i<singleMotifs.size();i++) {
            ModuleMotif motif=singleMotifs.get(i);
            if (motif.getRepresentativeName().equals(motifname)) return i;
        }
        return -1;
    }

    /**
     * Returns TRUE if one of the modulemotifs in this module has the given representative name
     * @param motifname
     * @return 
     */
    public boolean hasMotif(String motifname) {
        for (int i=0;i<singleMotifs.size();i++) {
            ModuleMotif motif=singleMotifs.get(i);
            if (motif.getRepresentativeName().equals(motifname)) return true;
        }       
        return false;
    }
    
    /**
     * Returns a comma-separated list of IDs for all motifs that can be a constituent single motifs
     * of this module. The length of the list is equal to or greater than the cardinality (size) of the module
     */
    public String getSingleMotifIDsAsString() {
        if (singleMotifs.isEmpty()) return "";
        StringBuilder builder=new StringBuilder();
        int count=0;
        for (int i=0;i<singleMotifs.size();i++) {
            for (String id:singleMotifs.get(i).getAllMotifIDs()) {
                if (count>0) builder.append(",");
                builder.append(id);
                count++;
            }          
        }
        return builder.toString();
    } 
    
    /**
     * Returns a comma-separated list of short names for all motifs that can be a constituent single motifs
     * of this module. The length of the list is equal to or greater than the cardinality (size) of the module
     */
    public String getSingleMotifShortNamesAsString(MotifLabEngine engine) {
        if (singleMotifs.isEmpty()) return "";
        StringBuilder builder=new StringBuilder();
        int count=0;
        for (int i=0;i<singleMotifs.size();i++) {
            for (String id:singleMotifs.get(i).getAllMotifShortNames(engine)) {
                if (count>0) builder.append(",");
                builder.append(id);
                count++;
            }          
        }
        return builder.toString();
    }     
    
    /** Returns a list of the ModuleMotifs */
    @SuppressWarnings("unchecked")
    public ArrayList<ModuleMotif> getModuleMotifs() {
        return (ArrayList<ModuleMotif>)singleMotifs.clone();
    }


    /**
     * Returns TRUE if the motif with the given name is a candidate
     * for the module single motif at the given index (starting at 0)
     */
    public boolean isMotifCandidate(int index, String motifname) {
        if (index<0 || index>=singleMotifs.size()) return false;
        else {
            ModuleMotif motif=singleMotifs.get(index);
            return motif.getMotifAsCollection().contains(motifname);
        }
    }

    /**
     * Returns TRUE if the motif with the given name and orientation is a candidate
     * for the module single motif at the given index (starting at 0)
     */
    public boolean isMotifCandidate(int index,String motifname,int orientation) {
        if (index<0 || index>=singleMotifs.size()) return false;
        else {
            ModuleMotif motif=singleMotifs.get(index);
            if (!motif.getMotifAsCollection().contains(motifname)) return false;
            if (motif.getOrientation()==ModuleCRM.INDETERMINED) return true;
            else return motif.getOrientation()==orientation;
        }
    }

    /** Returns TRUE if this module contains motifs with opposing orientations */
    public boolean motifsHaveOpposingOrientations() {
        boolean hasDirect=false;
        boolean hasReverse=false;
        for (ModuleMotif mm:singleMotifs) {
            if (mm.getOrientation()==ModuleCRM.DIRECT) hasDirect=true;
            else if (mm.getOrientation()==ModuleCRM.REVERSE) hasReverse=true;
            if (hasDirect && hasReverse) return true;
        }
        return false;
    }

    /**
     * Adds an additional motif to this module. The motif is represented by a MotifCollections of equivalent motif representations
     * @param modulemotifname
     * @param motifs
     * @param orientation
     */
    public void addModuleMotif(String modulemotifname, MotifCollection motifs, int orientation) {
        ModuleMotif modulemotif=new ModuleMotif(modulemotifname, motifs, orientation);
        singleMotifs.add(modulemotif);
    }
    
    /**
     * Adds an additional motif to this module. The motif is represented by a Collection containing names of equivalent motif representations
     * @param modulemotifname
     * @param motifs
     * @param orientation
     */
    public void addModuleMotif(String modulemotifname, Collection<String> motifs, int orientation) {
        ModuleMotif modulemotif=new ModuleMotif(modulemotifname, motifs, orientation);
        singleMotifs.add(modulemotif);
    }    

    /**
     * Adds an additional motif to this module (the motif is a single motif)
     * @param modulemotifname
     * @param motifs
     * @param orientation
     */
    public void addModuleMotif(String modulemotifname, Motif  motif, int orientation) {
        ModuleMotif modulemotif=new ModuleMotif(modulemotifname, motif, orientation);
        singleMotifs.add(modulemotif);
    }

    /**
     * Adds a distance constraint between 2 motifs referenced by index (starting at 0)
     * @param motif1 index of motif 1 (starting at 0)
     * @param motif2 index of motif 2 (starting at 0)
     * @param mindistance
     * @param maxdistance
     */
    public void addDistanceConstraint(int motif1, int motif2, int mindistance, int maxdistance) throws ExecutionError {
        if (motif1>=singleMotifs.size()) throw new ExecutionError("Motif reference #1 out of bounds (0 indexed!) for module '"+name+"'");
        if (motif2>=singleMotifs.size()) throw new ExecutionError("Motif reference #2 out of bounds (0 indexed!) for module '"+name+"'");
        if (motif1>=motif2) throw new ExecutionError("Motif reference #1 must be before reference #2 for module '"+name+"'");
        if (maxdistance<mindistance) throw new ExecutionError("Maximum distance ("+maxdistance+") must be equal to or larger than minimum distance ("+mindistance+") for module '"+name+"'");
        distance.add(new int[]{motif1,motif2,mindistance,maxdistance});
    }

   /**
     * Adds a distance constraining between 2 motifs referenced by name
     * @param motifname1
     * @param motifname2
     * @param mindistance
     * @param maxdistance
     */
    public void addDistanceConstraint(String motifname1, String motifname2, int mindistance, int maxdistance) throws ExecutionError {
        int motif1=getIndexForMotifName(motifname1);
        if (motif1<0) throw new ExecutionError("No such single motif '"+motifname1+"' for module '"+name+"'");
        int motif2=getIndexForMotifName(motifname2);
        if (motif2<0) throw new ExecutionError("No such single motif '"+motifname2+"' for module '"+name+"'");
        if (motif1>=motif2) throw new ExecutionError("Motif reference #1 must be before reference #2 for module '"+name+"'");
        if (maxdistance<mindistance) throw new ExecutionError("Maximum distance ("+maxdistance+") must be equal to or larger than minimum distance ("+mindistance+") for module '"+name+"'");
        distance.add(new int[]{motif1,motif2,mindistance,maxdistance});
    }

     /**
     * Adds a distance constraining between 2 motifs references by index (starting at 0)
     * @param constraint The first 2 values should reference two motifs, the last two values are min and max distance respectively

     */
    public void addDistanceConstraint(int[] constraint) throws ExecutionError  {
        if (constraint.length!=4) throw new ExecutionError("Length of distance constraint array for module must be 4");
        if (constraint[0]>=singleMotifs.size()) throw new ExecutionError("Motif reference #1 out of bounds (0 indexed!) for module '"+name+"'");
        if (constraint[1]>=singleMotifs.size()) throw new ExecutionError("Motif reference #2 out of bounds (0 indexed!) for module '"+name+"'");
        if (constraint[0]>=constraint[1]) throw new ExecutionError("Motif reference #1 must be smaller than reference #2 for module '"+name+"'");
        if (constraint[3]<constraint[2]) throw new ExecutionError("Maximum distance ("+constraint[3]+") must be equal to or larger than minimum distance ("+constraint[2]+") for module '"+name+"'");
        distance.add(constraint.clone());
    }

    /**
     * Returns a list of distance (distance) constrains set between pairs of motifs in the module
     * each int[] array in the list represents a constrains between a motif pair.
     * In the int[] the first 2 elements are single motif references (modulemotif indices)
     * while the 3rd and 4th elements are the min and max distance respectively;
     * @return
     */
    public ArrayList<int[]> getDistanceConstraints() {
        return distance;
    }

    /** Returns the module motif corresponding to the given representative name */
    public ModuleMotif getModuleMotif(String representativeMotifName) {
        for (ModuleMotif mm:singleMotifs) {
            if (mm.getRepresentativeName().equals(representativeMotifName)) return mm;
        }
        return null;
    }


    /**
     * Returns the motif with the given representative name as a Motif Collection of equivalent motifs
     * @return
     */
    public MotifCollection getMotifAsCollection(String representativeMotifName) {
        ModuleMotif mm=getModuleMotif(representativeMotifName);
        if (mm==null) return null;
        else return mm.getMotifAsCollection();
    }

    /**
     * Returns the motif at the given (zero indexed) position as a Motif Collection of equivalent motifs
     * @return
     */
    public MotifCollection getMotifAsCollection(int index) {
        if (index<0 || index>=singleMotifs.size()) return null;
        ModuleMotif mm=singleMotifs.get(index);
        return mm.getMotifAsCollection();
    }

    /**
     * Returns the orientation of the motif at the given (zero indexed) position
     * @return
     */
    public int getMotifOrientation(int index) {
        if (index<0 || index>=singleMotifs.size()) return 0;
        ModuleMotif mm=singleMotifs.get(index);
        return mm.getOrientation();
    }

    /**
     * Returns an array containing min and max allowed distance between the two motifs with the given names
     * or NULL if no distance constraint is set for the two motifs
     * @param motif1
     * @param motif2
     * @return
     */
    public int[] getDistance(String motif1, String motif2) {
        int index1=getIndexForMotifName(motif1);
        int index2=getIndexForMotifName(motif2);
        for (int[] array:distance) {
            if (array[0]==index1 && array[1]==index2) return new int[]{array[2],array[3]};
        }
        return null;
    }
    /**
     * Returns an array containing min and max allowed distance between the two motifs with the given indices
     * or NULL if no distance constraint is set for the two motifs
     * @param motif1
     * @param motif2
     * @return
     */
    public int[] getDistance(int index1, int index2) {
        for (int[] array:distance) {
            if (array[0]==index1 && array[1]==index2) return new int[]{array[2],array[3]};
        }
        return null;
    }

    /**
     * Returns the orientation of the motif with the given representative name
     * @return
     */
    public int getMotifOrientation(String representativeMotifName) {
        ModuleMotif mm=getModuleMotif(representativeMotifName);
        if (mm==null) return 0;
        else return mm.getOrientation();
    }

    /**
     * Returns the motif with the given representative name as either a single Motif or a Motif Collection of equivalent motifs
     * or NULL if no such motif exists in the module
     * @param engine
     * @return
     */
    public Data getMotif(String representativeMotifName, MotifLabEngine engine) {
        ModuleMotif mm=getModuleMotif(representativeMotifName);
        if (mm==null) return null;
        else return mm.getMotifs(engine);
    }

    /**
     * Clears all properties, motifs and distance constraints for this module
     */
    public void clear() {
        maxLength=0;
        ordered=false;
        singleMotifs.clear();
        distance.clear();
    }


    @Override
    public void importData(Data source) {
          ModuleCRM other=((ModuleCRM)source);
          this.name=other.name;
          this.maxLength=other.maxLength;
          this.ordered=other.ordered;
          this.singleMotifs=new ArrayList<ModuleMotif>();
          for (ModuleMotif mm:other.singleMotifs) {
             this.singleMotifs.add((ModuleMotif)mm.clone());
          }
          this.distance=new ArrayList<int[]>();
          for (int[] constraint:other.distance) {
             this.distance.add(constraint.clone());
          }
          this.GOterms=((ModuleCRM)source).GOterms;
          this.properties=((ModuleCRM)source).properties;          
    }

    @Override
    public ModuleCRM clone() {
        ModuleCRM newmodule=new ModuleCRM(this.name);
        newmodule.maxLength=this.maxLength;
        newmodule.ordered=this.ordered;
        newmodule.singleMotifs=new ArrayList<ModuleMotif>();
        for (ModuleMotif mm:this.singleMotifs) {
             newmodule.singleMotifs.add((ModuleMotif)mm.clone());
        }
        newmodule.distance=new ArrayList<int[]>();
        for (int[] constraint:this.distance) {
             newmodule.distance.add(constraint.clone());
        }
        newmodule.GOterms=(this.GOterms==null)?null:(int[])this.GOterms.clone(); 
        newmodule.properties=cloneProperties();//properties.clone();        
        return newmodule;
    }
    
    /** Tries to make a clone of the user-defined properties map 
     *  It assumes that all map values are either Strings, Numbers (i.e. immutable objects)
     *  or ArrayLists (but arrayLists are not allowed anyway
     */
    private HashMap<String,Object> cloneProperties() {
        if (this.properties==null) return null;
        HashMap<String,Object> clonedMap=new HashMap<String,Object>(properties.size());
        for (String key:properties.keySet()) {
            Object value=properties.get(key);  
            if (value instanceof ArrayList) clonedMap.put(key,((ArrayList)value).clone());
            else clonedMap.put(key, value);
        }
        return clonedMap;
    }    

    /**
     * Returns true if this ModuleCRM equals the other given ModuleCRM
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof ModuleCRM)) return false;
        if (this.maxLength!=((ModuleCRM)other).maxLength) return false;
        if (this.ordered!=((ModuleCRM)other).ordered) return false;
        if (!this.distance.equals(((ModuleCRM)other).distance)) return false;
        if (!containsSameModuleMotifs(((ModuleCRM)other))) return false;
        if ((((ModuleCRM)other).GOterms==null && this.GOterms!=null) || (((ModuleCRM)other).GOterms!=null && this.GOterms==null) ||  (((ModuleCRM)other).GOterms!=null && this.GOterms!=null && !MotifLabEngine.listcompare(this.GOterms,((ModuleCRM)other).GOterms))) return false;     
        if ((((ModuleCRM)other).properties==null && this.properties!=null) || (((ModuleCRM)other).properties!=null && this.properties==null) ||  (((ModuleCRM)other).properties!=null && this.properties!=null && !this.properties.equals(((ModuleCRM)other).properties))) return false;
    
        return true;
    }

    private boolean containsSameModuleMotifs(ModuleCRM other) {
        if (this.singleMotifs==null && other.singleMotifs==null) return true;
        if (this.singleMotifs!=null && other.singleMotifs==null) return false;
        if (this.singleMotifs==null && other.singleMotifs!=null) return false;                
        if (this.singleMotifs.size()!=other.singleMotifs.size()) return false;
        return this.singleMotifs.equals(other.singleMotifs);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.singleMotifs != null ? this.singleMotifs.hashCode() : 0);
        hash = 19 * hash + (this.distance != null ? this.distance.hashCode() : 0);
        hash = 19 * hash + this.maxLength;
        hash = 19 * hash + (this.ordered ? 1 : 0);
        hash = 19 * hash + (this.GOterms != null ? this.GOterms.hashCode() : 0);
        return hash;
    }


    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    public static ModuleCRM createModuleFromParameterString(String name, String parameterString, MotifLabEngine engine) throws ExecutionError {
        ModuleCRM cisRegModule=new ModuleCRM(name);
        String[] elements=parameterString.split("\\s*;\\s*");
        if (!elements[0].startsWith("CARDINALITY:") || elements[0].length()<=12) throw new ExecutionError("CARDINALITY property should be first parameter for module '"+name+"'");
        String cardinalityString=elements[0].substring(12);
        int cardinality=0;
        int found=0;
        try {
            cardinality=Integer.parseInt(cardinalityString);
        } catch (NumberFormatException nfe) {
            throw new ExecutionError("Unable to parse cardinality (integer) for module '"+name+"' => "+cardinalityString);
        }
        Pattern motifpattern=Pattern.compile("MOTIF\\((.+?)\\)\\[(.)\\]\\{(.+?)\\}");
        for (int i=1;i<elements.length;i++) {
            if (elements[i].startsWith("MAXLENGTH:")) {
                String maxlengthstring=elements[i].substring(10);
                 try {
                    cisRegModule.setMaxLength(Integer.parseInt(maxlengthstring));
                } catch (NumberFormatException nfe) {
                    throw new ExecutionError("Unable to parse maximum length (integer) for module '"+name+"' => "+maxlengthstring);
                }
            } else if (elements[i].equals("ORDERED")) {
                 cisRegModule.setOrdered(true);
            } else if (elements[i].equals("UNORDERED")) {
                 cisRegModule.setOrdered(false);
            } else if (elements[i].startsWith("MOTIF")) {
                Matcher matcher=motifpattern.matcher(elements[i]);
                if (matcher.matches()) {
                    String singlemotifname=matcher.group(1);
                    int orientation=INDETERMINED;
                    if (matcher.group(2).equals("+")) orientation=DIRECT;
                    else if (matcher.group(2).equals("-")) orientation=REVERSE;
                    String[] motifnames=matcher.group(3).split("\\s*,\\s*");
                    MotifCollection collection=new MotifCollection("ModuleMotifs");
                    for (String motifname:motifnames) {
                        Data item=engine.getDataItem(motifname);
                        if (item==null) throw new ExecutionError("Unrecognized data item '"+motifname+"' as Motif reference in module '"+name+"'");
                        else if (!(item instanceof Motif)) throw new ExecutionError("'"+motifname+"' referenced in module '"+name+"' is not a Motif");
                        else collection.addMotif((Motif)item);
                    }
                    cisRegModule.addModuleMotif(singlemotifname, collection, orientation);
                    found++;
                } else throw new ExecutionError("Unable to parse parameters for MOTIF in module '"+name+"'. Syntax should be MOTIF(name)[+|-|.]{comma-separated list of motifs}");
            } else if (elements[i].startsWith("DISTANCE(")) {
                int close=elements[i].indexOf(")");
                if (close<0) throw new ExecutionError("Missing end parenthesis for DISTANCE parameter in module '"+name+"'");
                String contents=elements[i].substring(9, close);
                String[] numbers=contents.trim().split("\\s*,\\s*");
                if (numbers.length!=4) throw new ExecutionError("Expected 4 entries for DISTANCE parameter in module '"+name+"'. Found "+numbers.length+".\n => "+contents);
                int[] values=new int[2];
                for (int j=2;j<numbers.length;j++) {
                    if (numbers[j].equals("UNLIMITED") || numbers[j].equals("*")) {
                        //if (j!=3) throw new ExecutionError("Only last value can be 'UNLIMITED' (or *) for DISTANCE parameter in module '"+name+"'");
                        if (j==2) values[0]=Integer.MIN_VALUE; //
                        else values[1]=Integer.MAX_VALUE;
                    }
                    else try {
                        values[j-2]=Integer.parseInt(numbers[j]);
                    } catch (NumberFormatException nfe) {
                        throw new ExecutionError("Unable to parse expected numeric value for DISTANCE parameter in module '"+name+"' => "+numbers[j]);
                    }
                }
                cisRegModule.addDistanceConstraint(numbers[0],numbers[1],values[0],values[1]);
            } else if (elements[i].startsWith("GO-TERMS:")) { //
                String goString=elements[i].substring("GO-TERMS:".length()).trim();
                String[] GO=goString.split("\\s*,\\s*");
                try {
                    cisRegModule.setGOterms(GO);
                } catch (ParseError p) {
                    throw new ExecutionError(p.getMessage(),p);
                }
            } else { // user-defined properties
                String segment=elements[i];
                int colonindex=segment.indexOf(':');
                if (colonindex<=0 || colonindex>=segment.length()-1)  throw new ExecutionError("Unable to parse module parameter. Not a key-value pair: "+segment); // throws exception if string contains no colon or if the colon is the first or last character
                String key=segment.substring(0, colonindex);
                if (!ModuleCRM.isValidUserDefinedPropertyKey(key)) throw new ExecutionError("Not a valid name for a property: "+key);
                String valuestring=segment.substring(colonindex+1);
                Object value=getObjectForPropertyValueString(valuestring);
                cisRegModule.setUserDefinedPropertyValue(key,value); //                   
            }            
        }
        if (found!=cardinality) throw new ExecutionError("Expected "+cardinality+" motifs for module '"+name+"'. Found "+found);
        return cisRegModule;
    }


    /** Returns the names of all properties that can be obtained from ModuleCRM objects */
    public static String[] getProperties(MotifLabEngine engine) {
        return new String[] {"ID","Cardinality","Consensus","Motifs","Ordered","Oriented","Max length","Max IC", "Min IC"};
    }
    
   /** Returns a list of names for all standard Motif properties such as "short name" and "Classification".
     *  (not including ID)
     *  Note that the names of properties are case-sensitive.
     *  @param includeDerived If TRUE, also derived properties such as IC-content, GC-content, size etc. which can not be set explicitly will be included in the list 
     */    
    public static String[] getAllStandardProperties(boolean includeDerived) {
        if (includeDerived) return new String[]{"ID","Cardinality","Consensus","Motifs","Ordered","Oriented","Max length","Max IC", "Min IC","GO"};
        else return new String[]{"ID","Motifs","Ordered","Max length","GO"};
    }

    public static boolean isStandardModuleProperty(String propertyname) {
        if (propertyname==null || propertyname.isEmpty()) return false;
        String[] props=getAllStandardProperties(true);
        for (String prop:props) {
            if (propertyname.equalsIgnoreCase(prop)) return true;
        }
        return false;
    }
        
    /**
     * Returns a list of all motif properties, both standard and user-defined
     * @param includeDerived
     * @param engine
     * @return 
     */
    public static String[] getAllProperties(boolean includeDerived, MotifLabEngine engine) {
        String[] standard=getAllStandardProperties(includeDerived);
        String[] userdefined=getAllUserDefinedProperties(engine);
        String[] all=new String[standard.length+userdefined.length];
        System.arraycopy(standard, 0, all, 0, standard.length);
        System.arraycopy(userdefined, 0, all, standard.length, userdefined.length);
        return all;
    }    
    
    public static String[] getAllEditableProperties(MotifLabEngine engine) {
        String[] editable=getAllStandardProperties(false);  // this should be coordinates with setPropertyValue()
        String[] userdefined=getAllUserDefinedProperties(engine);
        String[] all=new String[editable.length+userdefined.length];
        System.arraycopy(editable, 0, all, 0, editable.length);
        System.arraycopy(userdefined, 0, all, editable.length, userdefined.length);
        return all;
    }      
    
    /** Returns a list of names for all standard Motif properties that have numeric values such as "size" and "IC-content".
     *  Note that the names of properties are case-sensitive.
     *  @param includeDerived If TRUE, also derived properties such as IC-content, GC-content, size etc. which can not be set explicitly will be included in the list
     */
    public static String[] getNumericStandardProperties(boolean includeDerived) {
        ArrayList<String> numericProps=new ArrayList<String>();
        String[] props=getAllStandardProperties(includeDerived);
        for (String prop:props) {
            Class propclass=ModuleCRM.getPropertyClass(prop,null);
            if (propclass!=null && Number.class.isAssignableFrom(propclass)) numericProps.add(prop);
        }
        String[] result=new String[numericProps.size()];
        return numericProps.toArray(result);
     }
    
    /** Returns a list of names for all user-defined properties across all Motifs.
     *  Note that the names of properties are case-sensitive.
     *  This method works by dynamically querying all registered motifs to see
     *  what user-defined properties they have
     */
    public static String[] getAllUserDefinedProperties(MotifLabEngine engine) {
        HashSet<String> propertyNamesSet=new HashSet<String>();
        String[] propertynames=new String[propertyNamesSet.size()];
        ArrayList<Data> modules=engine.getAllDataItemsOfType(ModuleCRM.class);
        for (Data cisRegModule:modules) {
            Set<String> moduleprops=((ModuleCRM)cisRegModule).getUserDefinedProperties();
            if (moduleprops!=null) propertyNamesSet.addAll(moduleprops);
        }
        return propertyNamesSet.toArray(propertynames);
    }
    
    /** Returns a list of names for all user-defined properties with numeric values across all Motifs.
     *  Note that the names of properties are case-sensitive.
     */
    public static String[] getNumericUserDefinedProperties(MotifLabEngine engine) {
        ArrayList<String> numericProps=new ArrayList<String>();
        String[] props=getAllUserDefinedProperties(engine);
        for (String prop:props) {
            Class propclass=ModuleCRM.getPropertyClass(prop,engine);
            if (propclass!=null && Number.class.isAssignableFrom(propclass)) numericProps.add(prop);
        }
        String[] result=new String[numericProps.size()];
        return numericProps.toArray(result);
     }
    
    public static String[] getAllNumericProperties(boolean includeDerived, MotifLabEngine engine) {
        String[] standard=getNumericStandardProperties(includeDerived);
        String[] userdefined=getNumericUserDefinedProperties(engine);
        String[] all=new String[standard.length+userdefined.length];
        System.arraycopy(standard, 0, all, 0, standard.length);
        System.arraycopy(userdefined, 0, all, standard.length, userdefined.length);
        return all;
    }
        
    
    
    /**
     * Returns the type-class for the given property or NULL if the property is not recognized
     * @param propertyName
     * @return 
     */
    public static Class getPropertyClass(String propertyName, MotifLabEngine engine) {
             if (propertyName.equalsIgnoreCase("ID")) return String.class;
        else if (propertyName.equalsIgnoreCase("Cardinality") || propertyName.equalsIgnoreCase("Size")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("Consensus")) return String.class;
        //else if (propertyName.equalsIgnoreCase("Elements")) return String.class;
        else if (propertyName.equalsIgnoreCase("Max length")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("Motifs")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("Ordered")) return Boolean.class;
        else if (propertyName.equalsIgnoreCase("Oriented")) return Boolean.class;
        else if (propertyName.equalsIgnoreCase("Max IC")) return Double.class;
        else if (propertyName.equalsIgnoreCase("Min IC")) return Double.class;
        else return ModuleCRM.getClassForUserDefinedProperty(propertyName, engine);                    
    }      

    /** Returns the names of all numeric properties that can be obtained from ModuleCRM objects */
    public static String[] getNumericProperties(MotifLabEngine engine) {
        ArrayList<String> numericProps=new ArrayList<String>();
        String[] props=ModuleCRM.getProperties(engine);
        for (String prop:props) {
            Class propclass=ModuleCRM.getPropertyClass(prop,engine);
            if (propclass!=null && Number.class.isAssignableFrom(propclass)) numericProps.add(prop);
        }
        String[] result=new String[numericProps.size()];
        return numericProps.toArray(result);
    }
    
    
    /**
     * Returns TRUE if there is a numeric property with the given name
     * and FALSE if there is no such property or if it is not numeric
     * @param propertyName
     * @param engine
     * @return 
     */
    public static boolean isNumericProperty(String propertyName, MotifLabEngine engine) {
        Class type=ModuleCRM.getPropertyClass(propertyName, engine);
        if (type==null) return false;
        return (type==Integer.class || type==Double.class);
    }    
    
 
    /** Returns a value for the module property with the given name 
     *  @return an Object representing the value, this can be a 
     *  String, Boolean, Double, Integer depending on the property
     *  @throws ExecutionError If the property is not recognized 
     */
    @Override
    public Object getPropertyValue(String propertyName, MotifLabEngine engine) throws ExecutionError {
             if (propertyName.equalsIgnoreCase("ID")) return getName();
        else if (propertyName.equalsIgnoreCase("cardinality") || propertyName.equalsIgnoreCase("size")) return getCardinality();
        else if (propertyName.equalsIgnoreCase("Consensus")) return getModuleLogo();
        //else if (propertyName.equalsIgnoreCase("Elements")) return getSingleMotifNamesAsString(":");
        else if (propertyName.equalsIgnoreCase("Max length")) return getMaxLength();        
        else if (propertyName.equalsIgnoreCase("Motifs")) return getSingleMotifNames();
        else if (propertyName.equalsIgnoreCase("Ordered")) return isOrdered();
        else if (propertyName.equalsIgnoreCase("Oriented")) return isOriented();     
        else if (propertyName.equalsIgnoreCase("Max IC")) return getMaxICcontent(engine);     
        else if (propertyName.equalsIgnoreCase("Min IC")) return getMinICcontent(engine);     
        else if (propertyName.equalsIgnoreCase("GO") || propertyName.equalsIgnoreCase("gene ontology")) return getGOterms();          
        else throw new ExecutionError("Unknown module property: "+propertyName);     
    }
    
    @Override
    public boolean setPropertyValue(String propertyName, Object value) throws ExecutionError {
        try {
                 if (propertyName.equalsIgnoreCase("ID")) return false;          
            else if (propertyName.equalsIgnoreCase("Ordered")) setOrdered((Boolean)MotifLabEngine.convertToType(value,Boolean.class));
            else if (propertyName.equalsIgnoreCase("Max length")) setMaxLength((Integer)MotifLabEngine.convertToType(value,Integer.class));         
            else if (propertyName.equalsIgnoreCase("GO") || propertyName.equalsIgnoreCase("gene ontology")) {
                if (value instanceof String) setGOterms(((String)value).trim().split("\\s*,\\s*"));
                else if (value instanceof String[]) setGOterms((String[])value);
                else if (value instanceof ArrayList) {
                    String[] list=new String[((ArrayList<String>)value).size()];
                    list=((ArrayList<String>)value).toArray(list);
                    setGOterms(list);
                }
                else throw new ClassCastException();                 
            }                                    
            else setUserDefinedPropertyValue(propertyName, value);          
        } catch(Exception e) {    
            if (e instanceof ParseError) throw new ExecutionError(e.getMessage());
            throw new ExecutionError("Unable to set property '"+propertyName+"' to value '"+((value!=null)?value.toString():"")+"'");
        }
        return true;     
    }
    

    /** Returns a set of names for user-defined properties that are explicitly 
     *  defined for this Motif object or NULL if no such properties are defined
     */
    public Set<String> getUserDefinedProperties() {
        if (properties!=null) return properties.keySet();
        else return null;
    }
    
    /**
     * Returns the value of a user-defined property for this motif, or NULL if no property with the given
     * name has been explicitly defined for this motif
     * @param propertyName (case-sensitive)
     * @return
     */
    public Object getUserDefinedPropertyValue(String propertyName) {
         if (properties!=null) return properties.get(propertyName);
         else return null;
    }
    
    /** Returns the specified user-defined property as an object of the given class
     *  if the property is defined for this motif and can be "converted" into an object
     *  of the given class, or NULL if the property is not defined or can not be converted
     *  All properties can be converted to String.class if defined
     */
    public Object getUserDefinedPropertyValueAsType(String propertyName, Class type) {
        if (properties==null) return null;
        Object value=properties.get(propertyName);
        if (value==null || (value instanceof String && ((String)value).trim().isEmpty())) return null;
        if (value.getClass().equals(type)) return value; // no conversion necessary
        if (type.equals(ArrayList.class)) { // convert to list
            ArrayList<String> list=new ArrayList<String>(1);
            list.add(value.toString());
            return list;
        }
        if (type.equals(Double.class)) {
            if (value instanceof Integer) return new Double(((Integer)value));
            else return null;
        }
        if (type.equals(String.class)) {
            if (value instanceof ArrayList) {
                return MotifLabEngine.splice((ArrayList)value,",");
            } else return value.toString(); 
        } 
        return null; // no conversion possible (apparently)
    }
 
    /**
     * Set the value of a user-defined property for this motif
     * @param propertyName (case-sensitive)
     * @param value should be a Double, Integer, Boolean,  String or ArrayList (if NULL the property will be removed)
     * @return
     */
    public void setUserDefinedPropertyValue(String propertyName, Object value) {
         if (properties==null) properties=new HashMap<String, Object>();
         if (value==null || (value instanceof String && ((String)value).trim().isEmpty())) properties.remove(propertyName); // remove binding if it is empty
         else {
             if (!(value instanceof Double || value instanceof Integer || value instanceof Boolean || value instanceof List || value instanceof String)) value=value.toString(); // convert 'unknown' types to String just in case
             properties.put(propertyName, value);
             ModuleCRM.setUserDefinedPropertyClass(propertyName, value.getClass(), false);
         }
    }    
    
    public static Class getClassForUserDefinedProperty(String propertyName, MotifLabEngine engine) {
        if (userdefinedpropertyClasses!=null && userdefinedpropertyClasses.containsKey(propertyName)) return userdefinedpropertyClasses.get(propertyName); // cached entries
        Class type=getClassForUserDefinedPropertyDynamically(propertyName, engine);
        if (type!=null) ModuleCRM.setUserDefinedPropertyClass(propertyName,type,true); // cache the class in this lookup-table
        return type;       
    }    
    
    private static Class getClassForUserDefinedPropertyDynamically(String propertyName, MotifLabEngine engine) {
        if (engine==null) return null;
        // try to determine the type dynamically by querying all Modules
        Class firstclass=null;
        ArrayList<Data> modules=engine.getAllDataItemsOfType(ModuleCRM.class);
        for (Data cisRegModule:modules) {
            Object value=((ModuleCRM)cisRegModule).getUserDefinedPropertyValue(propertyName);
            if (value==null) continue;
            else if (firstclass==null) firstclass=value.getClass();
            else if (firstclass.equals(Integer.class) && value.getClass().equals(Double.class)) firstclass=Double.class; // return Double if values are a mix of Integers and Doubles
            else if (firstclass.equals(Double.class) && value.getClass().equals(Integer.class)) continue; // return Double if values are a mix of Integers and Doubles
            else if (firstclass.equals(ArrayList.class) || value.getClass().equals(ArrayList.class)) return ArrayList.class; // return ArrayList if one of the values is an ArrayList
            else if (!value.getClass().equals(firstclass)) return String.class; // Not compatible
        }
        return firstclass;         
    }    
    
    /**
     * Sets the class of the user-defined property in the lookup-table
     * @param propertyName
     * @param type
     * @param replaceCurrent 
     * @return Returns the new class for this property (this could be different from the argument type)
     */
    public static Class setUserDefinedPropertyClass(String propertyName, Class type, boolean replaceCurrent) {
        if (userdefinedpropertyClasses==null) userdefinedpropertyClasses=new HashMap<String, Class>();
        if (type!=null && !(type==Double.class || type==Integer.class || type==Boolean.class || List.class.isAssignableFrom(type) || type==String.class)) type=String.class; // just in case
        if (replaceCurrent || !userdefinedpropertyClasses.containsKey(propertyName)) userdefinedpropertyClasses.put(propertyName, type);
        else if (userdefinedpropertyClasses.get(propertyName)!=type) { // current class is different from new class. Try to coerce
              Class oldclass=userdefinedpropertyClasses.get(propertyName);
              if ((oldclass==Double.class && type==Integer.class) || (type==Double.class && oldclass==Integer.class)) userdefinedpropertyClasses.put(propertyName, Double.class);
              else if (List.class.isAssignableFrom(oldclass) || (type!=null && List.class.isAssignableFrom(type))) userdefinedpropertyClasses.put(propertyName, ArrayList.class);
              else userdefinedpropertyClasses.put(propertyName, String.class);
        }
        return userdefinedpropertyClasses.get(propertyName);
    }    
    
    
    /**
     * Removes all "cached" class-mappings for user-defined properties
     */
    public static void clearUserDefinedPropertyClassesLookupTable() {
        if (userdefinedpropertyClasses!=null) userdefinedpropertyClasses.clear();
    }

    /** updates the lookup table for all userdefined properties defined for this dataitem
     *  This could perhaps be more efficient?
     */
    public static void updateUserdefinedPropertiesLookupTable(ModuleCRM dataitem, MotifLabEngine engine) {
        Set<String> props=dataitem.getUserDefinedProperties();
        if (props==null || props.isEmpty()) return;
        for (String prop:props) {
            Class commontype=getClassForUserDefinedPropertyDynamically(prop, engine);
            setUserDefinedPropertyClass(prop,commontype,true);
        }
    }
    
    /**
     * Removes the value of a user-defined property from this motif
     * @param propertyName (case-sensitive)
     * @return
     */
    public void removeUserDefinedPropertyValue(String propertyName) {
         if (properties!=null) properties.remove(propertyName);
    }
    
    /** Returns an object corresponding to the valuestring.
     *  If the string is NULL or empty a value of NULL will be returned
     *  If the string can be parsed as an integer an Integer will be returned
     *  else If the string can be parsed as a double a Double will be returned
     *  If the string equals TRUE, FALSE, YES or NO (case-insensitive) a Boolean will be returned
     *  If the string contains commas the string will be split and returned as an ArrayList
     *  else a String will be returned (same as input)
     */
    public static Object getObjectForPropertyValueString(String valuestring) {
             if (valuestring==null || valuestring.trim().isEmpty()) return null;
        else if (valuestring.equalsIgnoreCase("TRUE") || valuestring.equalsIgnoreCase("YES")) return Boolean.TRUE;
        else if (valuestring.equalsIgnoreCase("FALSE") || valuestring.equalsIgnoreCase("NO")) return Boolean.FALSE;
        else if (valuestring.contains(",")) {
           String[] splitstring=valuestring.split("\\s*,\\s*");
           ArrayList<String> splitarray=new ArrayList<String>(splitstring.length);
           for (String s:splitstring) {
               if (!s.isEmpty()) splitarray.add(s);
           }
           return splitarray;
        }
        else {
            try {
                int integerValue=Integer.parseInt(valuestring);
                return new Integer(integerValue);

            } catch (NumberFormatException e) {
                  try {
                    double doublevalue=Double.parseDouble(valuestring);
                    return new Double(doublevalue);
                } catch(NumberFormatException ex) {
                    return valuestring; // default: return the same string
                }
            }
        }
    }    
    

    /** Returns TRUE if the given key can be used for user-defined properties */
    public static boolean isValidUserDefinedPropertyKey(String key) {
        if (key==null || key.trim().isEmpty()) return false;
        //if (engine.isReservedWord(key)) return false;
        if (!key.matches("^[a-zA-Z0-9_\\-\\s]+$")) return false; // spaces, underscores and hyphens are allowed
        for (String reserved:reservedproperties) {
            if (key.equalsIgnoreCase(reserved)) return false;
        }
        return true;
    }    
    
    /** Returns the highest possible IC-content of all modulemotifs in this module (gaps and orientations not considered)
     *  If several a modulemotif is represented by several single-motifs the one with highest IC
     *  will be selected as the representative
     */
    public double getMaxICcontent(MotifLabEngine engine) {
        double IC=0;
        for (ModuleMotif mm:singleMotifs) {
            IC+=mm.getHighestICcontent(engine);
        }      
        return IC;
    }
    
    /** Returns the lowest possible IC-content of all modulemotifs in this module (gaps and orientations not considered)
     *  If several a modulemotif is represented by several single-motifs the one with lowest IC
     *  will be selected as the representative
     */
    public double getMinICcontent(MotifLabEngine engine) {
        double IC=0;
        for (ModuleMotif mm:singleMotifs) {
            IC+=mm.getLowestICcontent(engine);
        }      
        return IC;
    }    

   
    public void setGOterms(int[] terms) {
        if (terms.length==0) this.GOterms=null;
        else this.GOterms=MotifLabEngine.removeDuplicates(terms);       
    }
    
    /** Sets the GO terms of this motif to the provided strings. 
     *  Throws ParseError if one of the GO-terms is invalid 
     *  Note that the GO-terms list should not contain duplicates
     */
    public void setGOterms(String[] terms) throws ParseError {
        if (terms==null || terms.length==0) {this.GOterms=null; return;}
        for (int i=0;i<terms.length;i++) { // preprocess
            if (terms[i]==null || terms[i].trim().isEmpty()) continue;
            if (terms[i].startsWith("GO:") || terms[i].startsWith("go:")) terms[i]=terms[i].substring(3);
        }
        terms=MotifLabEngine.flattenCommaSeparatedSublists(terms);
        terms=MotifLabEngine.removeDuplicates(terms,true);         
        this.GOterms=new int[terms.length];   
        for (int i=0;i<terms.length;i++) {
            String term=terms[i];          
            try {
                int value=Integer.parseInt(term);
                if (value<0 || value>9999999) throw new NumberFormatException(); // GO terms have 7 digits
                this.GOterms[i]=value;
            } catch (NumberFormatException e) {
                throw new ParseError("Not a valid GO term: "+term);
            }
        }
    }    
    
    /** Returns a list of GO terms associated with this motif on the form "GO:nnnnnnn" */
    public ArrayList<String> getGOterms() {
        if (this.GOterms==null) return new ArrayList<String>(0);
        ArrayList<String> terms=new ArrayList<String>(this.GOterms.length);
        for (int i=0;i<GOterms.length;i++) {
            int value=GOterms[i];
            terms.add("GO:"+String.format("%07d", value));
        }
        return terms;
    }
    
    /** Returns a list of GO terms associated with this motif. This is similar to the
        getGOterms method except that the returned only consist of 7-digit codes that
        do not start with the "GO:" prefix 
     */
    public ArrayList<String> getGOtermsWithoutPrefix() {
        if (this.GOterms==null) return new ArrayList<String>(0);
        ArrayList<String> terms=new ArrayList<String>(this.GOterms.length);
        for (int i=0;i<GOterms.length;i++) {
            int value=GOterms[i];
            terms.add(String.format("%07d", value));
        }
        return terms;
    }    
    
    public boolean hasGOterm(String term) {
        if (this.GOterms==null || this.GOterms.length==0) return false;
        if (term.startsWith("GO:") || term.startsWith("go:")) term=term.substring(3); // optional prefix
        try {
            int value=Integer.parseInt(term);
            if (value<=0 || value>9999999) return false;
            for (int t:this.GOterms) {
                if (t==value) return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }       
    }
    
    public boolean hasGOterm(int term) {
        if (this.GOterms==null || this.GOterms.length==0) return false;
        for (int t:this.GOterms) {
            if (t==term) return true;
        }
        return false;
    }  
    
    public boolean hasAnyGOterm(String[] terms) {
        for (String term:terms) {
            if (hasGOterm(term)) return true;
        }
        return false;
    }
        
        
    
    
    // ------------ Serialization ---------
    private static final long serialVersionUID = 2L;

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
