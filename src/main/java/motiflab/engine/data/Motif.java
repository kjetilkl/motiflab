/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.SystemError;
import motiflab.engine.protocol.ParseError;

/**
 * This class represents Transcription Factor Binding Motifs 
 * (usually as PWMs or consensus strings)
 * 
 * @author kjetikl
 */
public class Motif extends Data implements Comparable, BasicDataType {    
    public static final int COUNT_MATRIX=0;
    public static final int FREQUENCY_MATRIX=1;
    public static final int LOG_ODDS_MATRIX=2;
    public static final int UNNORMALIZED_FREQUENCY_MATRIX=3;
    public static final String FULL="Full";
    public static final String HALFSITE="Halfsite";
    public static final String DIMER="Dimer";
    public static final String OLIGOMER="Oligomer";

    private static final String[] partStrings=new String[]{FULL,HALFSITE,DIMER,OLIGOMER}; // the order of these should correponds to the integer constants above!
    private static final String[] reservedproperties=new String[]{"short","shortname","short name","long","longname","long name","name","names","ID","consensus","IC","IC-content","GC-content","GO","class","classification","organisms","partners","interactions","alternatives","expression","quality","factors","size","part","halfsite","matrix","A","C","G","T","W"};
    private static final String typedescription="Motif";
    
    private static transient HashMap<String,Class> userdefinedpropertyClasses=null; // contains a lookup-table defining the class-type of each user-defined property    

    private String name=null; // usually accession. This is the name or ID used when referencing this Motif
    private String shortname=null; //
    private String longname=null; //
    private String consensus=null;
    private String part=FULL; // this supersedes the old HALFSITE property
    private double IC_content=0;
    private double GC_content=0;
    private String organisms=null; // this could be a comma-separated list
    private int quality=6; // 6 is "unknown"
    private String bindingfactors=null; // this could be a comma-separated list
    private String classification=null;
    private String description=null;
    private int matrixtype=COUNT_MATRIX;
    private double[][] matrix; // x rows, 4 columns
    private ArrayList<String> interactionpartners=null; // A list which stores the names (Motif IDs) of known interaction partners
    private ArrayList<String> knownduplicates=null; // A list which stores the names (Motif IDs) of known duplicate motif models
    private int[] GOterms=null; // The GO terms associated with this motif stored as numbers rather than strings
    private HashMap<String,Object> properties=null; // A list which stores key-value pairs for user-defined properties. These keys are always in UPPERCASE!
    private ArrayList<String> tissueExpression=null; // as of v2.0 this property will replace the deprecated "tissues" list and the two lookup tables below

    @Deprecated
    private ArrayList<Short> tissues=null; // DEPRECATED: A list which stores the IDs of tissues the factors for this motif is expressed in. Use the static tissueNameTable to get the name of a tissue    
    @Deprecated
    private static ArrayList<String> tissueNameTable=new ArrayList<String>(); // tissues are stored as numbers rather than Strings to save space (since a limited number of tissues appear all over)
    @Deprecated
    private static HashMap<String,Short> tissueIDTable=new HashMap<String,Short>(); // tissues are stored as numbers rather than Strings to save space (since a limited number of tissues appear all over)    
   
    



    /**
     * Constructs a new Motif object with the given name
     * 
     * @param name

     */
    public Motif(String name){
         setName(name);
    }  

    @Override
    public String getName() {
        return name;
    }
    
    public String getShortName() {return shortname;}
    public void setShortName(String motifName) {this.shortname=motifName;}
    
    public String getCleanShortName() {
        if (shortname==null) return "";
        else return Motif.cleanUpMotifShortName(shortname, true);
    }
    
    public String getLongName() {return longname;}
    public void setLongName(String motifName) {this.longname=motifName;}
    
    public String getPresentationName() {
        if (shortname==null || shortname.isEmpty()) return name;
        else if (shortname.equals(name)) return name; // no need to repeat it...
        else return name+" - "+shortname; // previous versus did not flank the hyphen with spaces
    }
    
    /**
     * Sets a free-text description for this motif
     * @param description 
     */
    public void setDescription(String description) {
        this.description=description;
    }
    
    public String getDescription() {
        return (description!=null)?description:"";
    }
    
    public ArrayList<String> getAllTFNames() {
        String[] list=(bindingfactors!=null)?bindingfactors.split("\\s*,\\s*"):null;
        ArrayList<String> result=new ArrayList<String>((list!=null)?(list.length+3):3);
        if (list!=null) result.addAll(Arrays.asList(list));
        if (longname!=null) result.add(0, longname);
        if (shortname!=null) {
            result.add(0, getCleanShortName());
            result.add(0, shortname);
        }
        return result;
    }
        
    /**
     * Returns the consensus motif string for this Motif.
     * If the consensus property has been set explicitly, this string will be returned.
     * Else, the consensus will be derived from the matrix
     * @return 
     */
    public String getConsensusMotif() {
        if (consensus==null && matrix!=null) {
            consensus=getConsensusForMatrix(matrix);
        }    
        return consensus;
    }
    
    /**
     * Sets the "consensus" string property for this motif.
     * Note that this will not update the matrix property of the motif,
     * which means that the consensus string can be different from the matrix
     * @param consensus 
     * @see setConsensusMotifAndUpdatePWM
     */
    public void setConsensusMotif(String consensus) {this.consensus=consensus;}
    
    /**
     * Sets the consensus string property for this motif and also sets
     * the matrix property of the motif to a new matrix derived from the
     * new consensus
     * @param newconsensus 
     */
    public void setConsensusMotifAndUpdatePWM(String newconsensus) {
        setConsensusMotif(newconsensus);
        double[][] newmatrix=getMatrixFromConsensus(newconsensus);
        setMatrix(normalizeMatrix(newmatrix));   
    }
    
    /**
     * Returns a "count matrix" derived from the IUPAC consensus string.
     * A non-generate base letter (A,C,G,T) will give a count of 12 for that letter,
     * A double-generate letter (e.g. S,M,Y) will give a count of 6 for the two base 
     * letters covered and a triple-degenerate letter (e.g. B,C) will give a count of 4 for the three covered bases. 
     * The letter N will give a count of 3 for all four bases.
     * The matrix can later be normalized with the "normalizeMatrix" function to convert it to a frequency matrix
     * @param consensus
     * @return a new motif matrix
     * @see getPWMfromSites
     */
    public static double[][] getMatrixFromConsensus(String consensus) {
        double[][] newmatrix=new double[consensus.length()][4];
        for (int i=0;i<consensus.length();i++) {
            char base=Character.toUpperCase(consensus.charAt(i));
                 if (base=='A') {newmatrix[i][0]=12.0;newmatrix[i][1]=0.0;newmatrix[i][2]=0.0;newmatrix[i][3]=0.0;}
            else if (base=='C') {newmatrix[i][0]=0.0;newmatrix[i][1]=12.0;newmatrix[i][2]=0.0;newmatrix[i][3]=0.0;}
            else if (base=='G') {newmatrix[i][0]=0.0;newmatrix[i][1]=0.0;newmatrix[i][2]=12.0;newmatrix[i][3]=0.0;}
            else if (base=='T') {newmatrix[i][0]=0.0;newmatrix[i][1]=0.0;newmatrix[i][2]=0.0;newmatrix[i][3]=12.0;}
            else if (base=='R') {newmatrix[i][0]=6.0;newmatrix[i][1]=0.0;newmatrix[i][2]=6.0;newmatrix[i][3]=0.0;}
            else if (base=='Y') {newmatrix[i][0]=0.0;newmatrix[i][1]=6.0;newmatrix[i][2]=0.0;newmatrix[i][3]=6.0;}
            else if (base=='M') {newmatrix[i][0]=6.0;newmatrix[i][1]=6.0;newmatrix[i][2]=0.0;newmatrix[i][3]=0.0;}
            else if (base=='K') {newmatrix[i][0]=0.0;newmatrix[i][1]=0.0;newmatrix[i][2]=6.0;newmatrix[i][3]=6.0;}
            else if (base=='W') {newmatrix[i][0]=6.0;newmatrix[i][1]=0.0;newmatrix[i][2]=0.0;newmatrix[i][3]=6.0;}
            else if (base=='S') {newmatrix[i][0]=0.0;newmatrix[i][1]=6.0;newmatrix[i][2]=6.0;newmatrix[i][3]=0.0;}
            else if (base=='B') {newmatrix[i][0]=0.0;newmatrix[i][1]=4.0;newmatrix[i][2]=4.0;newmatrix[i][3]=4.0;}
            else if (base=='D') {newmatrix[i][0]=4.0;newmatrix[i][1]=0.0;newmatrix[i][2]=4.0;newmatrix[i][3]=4.0;}
            else if (base=='H') {newmatrix[i][0]=4.0;newmatrix[i][1]=4.0;newmatrix[i][2]=0.0;newmatrix[i][3]=4.0;}
            else if (base=='V') {newmatrix[i][0]=4.0;newmatrix[i][1]=4.0;newmatrix[i][2]=4.0;newmatrix[i][3]=0.0;}
            else {newmatrix[i][0]=3.0;newmatrix[i][1]=3.0;newmatrix[i][2]=3.0;newmatrix[i][3]=3.0;}
        }     
        return newmatrix;
    }
    
    public int getMatrixType() {return matrixtype;}
    
    public String getMatrixTypeAsString() {
        if (matrixtype==COUNT_MATRIX) return "count";
        else if (matrixtype==LOG_ODDS_MATRIX) return "weight";
        else if (matrixtype==FREQUENCY_MATRIX) return "frequency";
        else return "frequency";
    }
    
    public void setMatrixType(int type) {matrixtype=type;}
    
    /** This will returned the stored matrix 'as is'
     * The values in the matrix could either be raw frequency counts, normalized counts or log-odds values
     */
    public double[][] getMatrix() {return matrix;}

    /**
     * Sets the matrix property for this motif and also updates the "consensus" property
     * to be a consensus string derived from the new matrix.
     * @param newmatrix 
     */
    public void setMatrix(double[][] newmatrix) {
        int type=findProbableMatrixType(newmatrix);  
        setMatrixType(type);        
        matrix=newmatrix;
        double[][] frequencymatrix=matrix; 
        if (type==LOG_ODDS_MATRIX) frequencymatrix=getMatrixAsFrequencyMatrix(); // its easier this way
        IC_content=calculateInformationContent(frequencymatrix, false);
        GC_content=calculateGCContent(frequencymatrix);
        consensus=getConsensusForMatrix(frequencymatrix);   
    }
           
    public String getOrganisms() {return organisms;}
    public void setOrganisms(String organisms) {this.organisms=organisms;}
    public void setOrganisms(List<String> organisms) {this.organisms=MotifLabEngine.splice(organisms,",");}    
    
    public int getQuality() {return quality;}
    public void setQuality(int quality) {this.quality=quality;}
    
    public double getICcontent() {
        if (IC_content==0 && matrix!=null) {
            IC_content=calculateInformationContent(matrix, false);
        }
        return IC_content;
    }
    
    /** Returns an array containing the IC-content for each column (or position) in the motif
     *  @param reverse If TRUE the returned IC-content array will be reversed 
     *  @param normalize If TRUE the IC-content will be normalized to [0,1] by dividing by 2
     */
    public double[] getICcontentForColumns(boolean reverse, boolean normalize) {
        if (matrix==null) return null;
        int upper=matrix.length-1;
        double ic[]=new double[matrix.length];
        for (int i=0;i<matrix.length;i++) {
            ic[i]=calculateColumnIC(matrix[(reverse)?(upper-i):i][0], matrix[(reverse)?(upper-i):i][1], matrix[(reverse)?(upper-i):i][2], matrix[(reverse)?(upper-i):i][3], false);
            if (normalize) ic[i]=ic[i]/2;
        }
        return ic;        
    }  
    
    /** Returns the IC-content for the specified column (position) in the motif
     */
    public double getICcontentForColumn(int column) {
        if (matrix==null || column<0 || column>=matrix.length) return 0;
        return calculateColumnIC(matrix[column][0], matrix[column][1], matrix[column][2], matrix[column][3], false);      
    }      

    public void setICcontent(double ic) {this.IC_content=ic;}
    
    /** Returns the start end end coordinates for the motif's 'core region', which is defined here
     *  as either the middle region of the motif counted from the leftmost base with IC>=1 to the rightmost base with IC>=1,
     *  (i.e. the motif discarding flanks where bases have IC<1) with the core region having at least a size of 3bp,
     *  or as the 5 contiguous bases with highest total IC-content, or the whole motif if the size of the motif
     *  is equal to or less than 5 bp.
     */
    public int[] getCoreRegion() {
        int length=getLength();
        double[] IC=getICcontentForColumns(false,false);
        int[] core=new int[]{-1,-1};       
        for (int i=0;i<length;i++) {
           if (IC[i]>=1) {
               if (core[0]<0) core[0]=i; // first position woth IC>=1
               core[1]=i; // so far the last position with IC>=1
           }
        }
        if (core[0]>=0 && core[1]-core[0]>=2) { // The core size must be at least 3 bp long (sic)
            return core;
        }
        if (length<=5) return new int[]{0,length-1}; // the motif is max 5bp long but has no real core, so we use the whole motif as core     
        // if not returned yet, find 5 consecutive bases with highest total IC
        double[] ICsum=new double[length];
        for (int i=0;i<=length-5;i++) {
            for (int j=0;j<5;j++) {
               ICsum[i]+=IC[i+j];
            }
        }
        int maxpos=0;
        for (int i=1;i<length;i++) {
            if (ICsum[i]>=ICsum[maxpos]) maxpos=i;
        }  
        return new int[]{maxpos,maxpos+5-1};                
    }
    
    public String getBindingFactors() {return bindingfactors;}
    
    public void setBindingFactors(String factors) {this.bindingfactors=factors;}
    public void setBindingFactors(List<String> factors) {this.bindingfactors=MotifLabEngine.splice(factors,",");}
    
    public String getClassification() {return classification;}

    public void setClassification(String classification) throws ExecutionError {
        if (classification!=null && classification.matches("[^0-9\\.]")) throw new ExecutionError("Illegal motif class: '"+classification+"' for "+getPresentationName());
        if (classification!=null) {
            classification=MotifClassification.trimToLevel(classification, 6);
            if (!MotifClassification.isKnownClassString(classification)) throw new ExecutionError("Unknown motif class: '"+classification+"' for "+getPresentationName());
        }
        this.classification=classification;
    }
    
    // public boolean isHalfsite() {return part==HALFSITE;}
    public String getPart() {
        if (part==null) return FULL;
        else return part;
    }
    
    public void setPart(String part) {
        if (part!=null) { // check that part is a valid value else set to FULL
            for (int i=0;i<partStrings.length;i++) {
                if (part.equalsIgnoreCase(partStrings[i])) {this.part=partStrings[i];return;}
            }
        }
        this.part=FULL;
    }

    /** Returns TRUE if the argument is a valid value for the 'part' property of motifs */
    public static boolean isValidPart(String part) {
        if (part!=null) {
            for (int i=0;i<partStrings.length;i++) {
                if (part.equalsIgnoreCase(partStrings[i])) return true;
            }
        }
        return false;
    }

    public double getGCcontent() {
        if (GC_content==0 && matrix!=null) {
            GC_content=calculateGCContent(matrix);
        }
        return GC_content;
    }

    /** Returns the length of the motif in base pairs */
    public int getLength() {
        if (matrix==null) return 0;
        else return matrix.length;
    }
    
    /** Returns the number of sequences the PWM is based on.
     *  This is derived by summing up the A,C,G and T counts in the first position
     *  (If the PWM is a normalized frequency matrix, this will return a support of 1)
     */
    public int getSupport() {
        if (matrix==null || matrix.length==0) return 0;
        else {
            return (int)(matrix[0][0]+matrix[0][1]+matrix[0][2]+matrix[0][3]);
        }
    }    


    /**
     * Returns the names of known interaction partners for this motif
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getInteractionPartnerNames() {
        if (interactionpartners==null) return new ArrayList<String>(0);
        else return (ArrayList<String>)interactionpartners.clone();
    }

    /**
     * Returns TRUE if a motif with the given name (ID) is a known interaction partner for this Motif
     * @param motifname
     * @return
     */
    public boolean isKnownInteractionPartner(String motifname) {
        if (interactionpartners==null || motifname==null || motifname.isEmpty()) return false;
        return interactionpartners.contains(motifname);
    }

   /**
     * Returns the names of known duplicate motif models for this motif
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getKnownDuplicatesNames() {
        if (knownduplicates==null) return new ArrayList<String>(0);
        else return (ArrayList<String>)knownduplicates.clone();
    }

    public boolean hasDuplicates() {
        return (knownduplicates!=null && !knownduplicates.isEmpty());
    }
    
    /**
     * Sets the names (Motif IDs) of known interaction partners for this motif
     */
    public void setInteractionPartnerNames(String[] names) {
        if (names==null || names.length==0) interactionpartners=null;
        else {
            interactionpartners=new ArrayList<String>(names.length);
            interactionpartners.addAll(Arrays.asList(names));
        }
    }

   /**
     * Sets the names (Motif IDs) of known alternatives/duplicates for this motif
     */
    public void setKnownDuplicatesNames(String[] names) {
        if (names==null || names.length==0) knownduplicates=null;
        else {
            knownduplicates=new ArrayList<String>(names.length);
            knownduplicates.addAll(Arrays.asList(names));
        }
    }
       
   /**
     * Returns a  list of tissues that factors for this motifs are expressed in
     */
    public ArrayList<String> getTissueExpressionAsStringArray() {
        if (tissueExpression!=null && !tissueExpression.isEmpty()) return tissueExpression;
        // legacy case (Deprecated)
        if (tissues==null || tissues.isEmpty() || tissueNameTable==null || tissueNameTable.isEmpty()) return new ArrayList<String>();
        else {
           ArrayList<String>list=new ArrayList<String>(tissues.size());
           for (Short id:tissues) {
               if (id>=0 && id<tissueNameTable.size()) list.add(tissueNameTable.get(id));
           }
           return list;
        }
    }

   /**
     * Returns a comma-separated list of tissues that factors for this motifs are expressed in
     * (or an empty string)
     */
    public String getTissueExpressionAsString() {
        if (tissueExpression!=null && !tissueExpression.isEmpty()) return MotifLabEngine.splice(tissueExpression, ",");
        // legacy case (Deprecated)
        if (tissues==null || tissues.isEmpty() || tissueNameTable==null || tissueNameTable.isEmpty()) return "";
        StringBuilder builder=new StringBuilder();
        for (int i=0;i<tissues.size();i++) {
            if (i>0) builder.append(",");
            short index=tissues.get(i);
            if (index>=0 && index<tissueNameTable.size()) builder.append(tissueNameTable.get(tissues.get(i)));
        }
        return builder.toString();
    }

   /**
     * Sets the names of tissues that factors for this motifs are expressed in
    * (Note: the argument array will be sorted when calling this method)
     */
    public void setTissueExpression(String[] names) {
        if (names==null || names.length==0) tissueExpression=null;
        else {
            if (tissueExpression==null) tissueExpression=new ArrayList<>(names.length);
            tissueExpression.clear();
            tissueExpression.addAll(Arrays.asList(names));            
        }        
        // legacy case (Deprecated)           
//        if (names==null || names.length==0) tissues=null;
//        else {
//            Arrays.sort(names);
//            tissues=new ArrayList<Short>(names.length);
//            for (String n:names ) tissues.add(Motif.getTissueIndexForName(n));
//        }
    }
    
    public void setTissueExpression(ArrayList<String> names) {
        if (names==null || names.isEmpty()) tissueExpression=null;
        else {
            if (tissueExpression==null) tissueExpression=new ArrayList<>(names.size());
            tissueExpression.clear();
            tissueExpression.addAll(names);
        }       
        // legacy case (Deprecated)        
//        if (names==null || names.isEmpty()) tissues=null;
//        else {
//            String[] list=new String[names.size()];
//            list=names.toArray(list);
//            setTissueExpression(list);
//        }
    }
    
    public void setGOterms(int[] terms) {
        if (terms.length==0) this.GOterms=null;
        else this.GOterms=MotifLabEngine.removeDuplicates(terms);   
        
    }
    
    /** Sets the GO terms of this motif to the provided strings (duplicates are ignored)
     *  Throws ParseError if one of the GO-terms is invalid.
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
    
    
    /** Returns TRUE if the given TFname could refer to this motif.
     *  I.e. if the name matches either this motif's ID, short name,
     *  "clean name", long name or name of any associated TF.
     */
    public boolean matchesTF(String TFname) {
        if (TFname.equalsIgnoreCase(name)) return true;
        if (TFname.equalsIgnoreCase(shortname)) return true;
        if (TFname.equalsIgnoreCase(longname)) return true;
        if (TFname.equalsIgnoreCase(getCleanShortName())) return true;
        if (bindingfactors!=null) {
            String[] TFs=bindingfactors.split("\\s*,\\s*");
            for (String TF:TFs) {
                if (TFname.equalsIgnoreCase(TF)) return true;
            }            
        }
        return false;
    }

    /*
     * Compares the given sequence against the matrix of this motif and returns
     * a match score between 0 (worst possible match) and 1 (best possible match) 
     * reflecting the degree of similarity between this motif and the sequence.
     * Only the direct orientation will be considered.
     * If the sequence has a different size from this motif, a value of 0 will
     * automatically be returned
     */
    public double calculatePWMmatchScore(String sequence) {
         double[][] pfm=getMatrixAsFrequencyMatrix();
         if (sequence.isEmpty() || pfm==null || pfm.length!=sequence.length()) return 0;
         double rawscorevalue=0;
         double minscore=0;
         double maxscore=0;
         for (int i=0;i<sequence.length();i++) { // 
           char base=sequence.charAt(i);
                if (base=='A' || base=='a') rawscorevalue+=pfm[i][0];
           else if (base=='C' || base=='c') rawscorevalue+=pfm[i][1];
           else if (base=='G' || base=='g') rawscorevalue+=pfm[i][2];
           else if (base=='T' || base=='t') rawscorevalue+=pfm[i][3];
           minscore+=MotifLabEngine.getMinimumValue(pfm[i]);
           maxscore+=MotifLabEngine.getMaximumValue(pfm[i]);          
         }   
         if (maxscore==minscore) return 1.0; // if maxscore and minscore are equal, it means that there is no variation in the motif (all N's?) and every sequence will score max
         return (rawscorevalue-minscore)/(maxscore-minscore); // returns the relative score by comparing against the minimum and maximum scores possible
     }   
    
    @Override
    public String[] getResultVariables() {
        String[] standard=getAllStandardProperties(true);
        // To add user-defined properties we need access to the engine as a parameter...
//        Set<String> motifprops=getUserDefinedProperties();
//        String[] all=new String[standard.length+((motifprops!=null)?motifprops.size():0)];
//        System.arraycopy(standard, 0, all, 0, standard.length);
//        int i=0;
//        if (motifprops!=null) {
//            Iterator<String> it=motifprops.iterator();
//            while (it.hasNext()) {               
//                all[standard.length+i]=it.next();
//                i++;
//            }
//        }
        String[] special=new String[]{"reverse motif","inverse motif","shuffle motif","flank motif: AAA,TTT","combine motif: othermotif,distance","trim motif: X,Y","trim motif flanks: IC-cutoff", "round motif"};
        String[] all=new String[standard.length+special.length];
        System.arraycopy(standard, 0, all, 0, standard.length);
        System.arraycopy(special, 0, all, standard.length, special.length);
        return all;        
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename.equals("log weight matrix")) {
            Motif copy=this.clone();
            copy.setMatrix(copy.getMatrixAsLogMatrix());
            return copy;            
        }
        if (!hasResult(variablename)) throw new ExecutionError("'" + getName() + "' does not have a result for '" + variablename + "'");
        else if (variablename.equalsIgnoreCase("Alternatives")) {
            MotifCollection col=new MotifCollection("result");
            if (knownduplicates!=null) col.addMotifNames(knownduplicates);       
            return col;
        } else if (variablename.equalsIgnoreCase("Interactions")) {
            MotifCollection col=new MotifCollection("result");
            if (interactionpartners!=null) col.addMotifNames(interactionpartners);  
            return col;
        } else if (variablename.equalsIgnoreCase("reverse motif") || variablename.equalsIgnoreCase("complement motif")) {
            return Motif.parseMotifParameters("reverse:"+getName(), "result", engine);
        } else if (variablename.equalsIgnoreCase("inverse motif")) {
            return Motif.parseMotifParameters("inverse:"+getName(), "result", engine);
        } else if (variablename.equalsIgnoreCase("shuffle motif")) {
            return Motif.parseMotifParameters("shuffle:"+getName(), "result", engine);
        } else if (variablename.equalsIgnoreCase("round motif")) {
            return Motif.parseMotifParameters("round:"+getName(), "result", engine);
        } else if (variablename.toLowerCase().startsWith("flank motif:")) {
            String[] parts=variablename.substring("flank motif:".length()).split("\\s*,\\s*");
            if (parts.length!=2) throw new ExecutionError("Format of 'flank motif' function should be \"flank motif:prefix,suffix\"");            
            return Motif.parseMotifParameters("flank:"+getName()+","+parts[0].trim()+","+parts[1].trim(), "result", engine);
        } else if (variablename.toLowerCase().startsWith("combine motif:")) {
            String[] parts=variablename.substring("combine motif:".length()).split("\\s*,\\s*");
            if (parts.length==1) return Motif.parseMotifParameters("combine:"+getName()+","+parts[0].trim(), "result", engine);          
            else if (parts.length==2) return Motif.parseMotifParameters("combine:"+getName()+","+parts[0].trim()+","+parts[1].trim(), "result", engine);
            else throw new ExecutionError("Format of 'combine motif' function should be \"combine motif:othermotif [,distance]\"");            
        } else if (variablename.toLowerCase().startsWith("trim motif:")) {
            String[] parts=variablename.substring("trim motif:".length()).split("\\s*,\\s*");
            if (parts.length!=2) throw new ExecutionError("Format of 'trim motif' function should be \"trim motif:columnsToTrimFromBeginning,columnsToTrimFromEnd\"");
            int columnsFromStart=0;
            int columnsFromEnd=0;
            try {
                columnsFromStart=Integer.parseInt(parts[0].trim()); 
                columnsFromEnd=Integer.parseInt(parts[1].trim()); 
            } catch (NumberFormatException e) {
                throw new ExecutionError("Format of 'trim motif' function should be \"trim motif:columnsToTrimFromBeginning,columnsToTrimFromEnd\"");
            }            
            return Motif.parseMotifParameters("trim:"+getName()+","+columnsFromStart+","+columnsFromEnd, "result", engine);
        } else if (variablename.toLowerCase().startsWith("trim motif flanks:")) {
            String ICcutoffString=variablename.substring("trim motif flanks:".length()).trim();
            double ICcutoff=0;
            try {
                ICcutoff=Double.parseDouble(ICcutoffString); 
            } catch (NumberFormatException e) {
                throw new ExecutionError("Format of 'trim motif flanks' function should be \"trim motif flanks:IC-cutoff\"");
            }                       
            return Motif.parseMotifParameters("trim flanks:"+getName()+","+ICcutoff, "result", engine);
        } else {
            Object value=getPropertyValue(variablename, engine);
            if (value instanceof Integer) return new NumericVariable("result", (Integer)value); 
            else if (value instanceof Double) return new NumericVariable("result", (Double)value);
            else if (value instanceof String) return new TextVariable("result", (String)value);
            else if (value instanceof ArrayList) return new TextVariable("result", (ArrayList<String>)value);
            throw new ExecutionError("'" + getName() + "' can not return result for '" + variablename + "'");
        } 
    }

    @Override
    public Class getResultType(String variablename) {
        if (variablename.equalsIgnoreCase("log weight matrix")) return Motif.class;
        if (   variablename.equalsIgnoreCase("reverse motif")
            || variablename.equalsIgnoreCase("complement motif")
            || variablename.equalsIgnoreCase("inverse motif")
            || variablename.equalsIgnoreCase("shuffle motif")
            || variablename.equalsIgnoreCase("round motif")
            || variablename.toLowerCase().startsWith("combine motif:")                
            || variablename.toLowerCase().startsWith("flank motif:")
            || variablename.toLowerCase().startsWith("trim motif:")
            || variablename.toLowerCase().startsWith("trim motif flanks:")
        ) return Motif.class;
        if (!hasResult(variablename)) {
            return null;
        } else if (variablename.equalsIgnoreCase("Alternatives") || variablename.equalsIgnoreCase("Interactions")) {
            return MotifCollection.class;
        } else {
            Class type=getPropertyClass(variablename, null);
            if (type==Integer.class || type==Double.class) return NumericVariable.class;
            else if (type!=null) return TextVariable.class;
            else return null; // Could be user-defined I cannot handle these yet.
        }
    }

    @Override
    public boolean hasResult(String variablename) {
        if (   variablename.equalsIgnoreCase("reverse motif")
            || variablename.equalsIgnoreCase("complement motif")
            || variablename.equalsIgnoreCase("inverse motif")
            || variablename.equalsIgnoreCase("shuffle motif")
            || variablename.equalsIgnoreCase("round motif")
            || variablename.toLowerCase().startsWith("combine motif:")                
            || variablename.toLowerCase().startsWith("flank motif:")
            || variablename.toLowerCase().startsWith("trim motif:")
            || variablename.toLowerCase().startsWith("trim motif flanks:")
        ) return true;      
        for (String s:getResultVariables()) {
            if (s.equalsIgnoreCase(variablename)) return true;
        }
        return false;
    }     


    @Override
    public Object getValue() {return this;} // should maybe change later
    
    @Override
    public String getValueAsParameterString() {
        StringBuilder parameter=new StringBuilder();
        if (shortname!=null && !shortname.isEmpty()) {parameter.append("SHORTNAME:");parameter.append(shortname);parameter.append(";");}
        if (longname!=null && !longname.isEmpty()) {parameter.append("LONGNAME:");parameter.append(longname);parameter.append(";");}
        //if (consensus!=null && !consensus.isEmpty()) parameter.append("CONSENSUS:");parameter.append(consensus);parameter.append(";");
        if (matrix!=null) {parameter.append(getMatrixAsString());}       
        if (classification!=null) {parameter.append("CLASS:");parameter.append(classification);parameter.append(";");}
        if (part!=null && !part.equals(FULL)) {parameter.append("PART:");parameter.append(part);parameter.append(";");}
        if (quality<6) {parameter.append("QUALITY:");parameter.append(quality);parameter.append(";");}
        if (organisms!=null && !organisms.isEmpty()) {parameter.append("ORGANISMS:");parameter.append(organisms);parameter.append(";");}
        // if (tissues!=null && !tissues.isEmpty()) {parameter.append("EXPRESSION:");parameter.append(getTissueExpressionAsString());parameter.append(";");}
        if (tissueExpression!=null && !tissueExpression.isEmpty()) {parameter.append("EXPRESSION:");parameter.append(getTissueExpressionAsString());parameter.append(";");}
        if (bindingfactors!=null && !bindingfactors.isEmpty()) {parameter.append("FACTORS:");parameter.append(bindingfactors);parameter.append(";");}
        if (interactionpartners!=null && !interactionpartners.isEmpty()) {parameter.append("PARTNERS:");parameter.append(MotifLabEngine.splice(interactionpartners,","));parameter.append(";");}
        if (knownduplicates!=null && !knownduplicates.isEmpty()) {parameter.append("ALTERNATIVES:");parameter.append(MotifLabEngine.splice(knownduplicates,","));parameter.append(";");}
        if (GOterms!=null) {parameter.append("GO-TERMS:");parameter.append(MotifLabEngine.splice(getGOtermsWithoutPrefix(),","));parameter.append(";");}
        if (description!=null) {parameter.append("DESCRIPTION:");parameter.append(escapePropertyText(description, true));parameter.append(";");}
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

    /** Returns the stored value for the specified base in the given position (starting at 0) 
     * (If the position is outside the matrix bounds or the base is unknown a value of 0 is returned);
     * This will return the value as stored in the matrix itself, which could be an unnormalized base count value,
     * a base frequency or a log-odds value.
     */
    public double getMatrixValue(int pos, char base) {
        if (matrix==null || pos<0 || pos>=matrix.length) return 0;
        switch (base) {
            case 'A': return matrix[pos][0];
            case 'a': return matrix[pos][0];
            case 'C': return matrix[pos][1];
            case 'c': return matrix[pos][1];
            case 'G': return matrix[pos][2];
            case 'g': return matrix[pos][2];
            case 'T': return matrix[pos][3];
            case 't': return matrix[pos][3];
        }
        return 0;
    }
    
    /** Returns the value for the specified base in the given position (starting at 0) 
     * (If the position is outside the matrix bounds or the base is unknown a value of 0 is returned);
     * This will return the value as stored in the matrix itself
     * Note that unknown/masked bases (other than A,C,G,T) will return the value 0
     */
    public double getBaseFrequency(int pos, char base) {
        if (matrix==null || pos<0 || pos>=matrix.length) return 0;
        double value = 0;
        switch (base) {
            case 'A': value = matrix[pos][0]; break;
            case 'a': value = matrix[pos][0]; break;
            case 'C': value = matrix[pos][1]; break;
            case 'c': value = matrix[pos][1]; break;
            case 'G': value = matrix[pos][2]; break;
            case 'g': value = matrix[pos][2]; break;
            case 'T': value = matrix[pos][3]; break;
            case 't': value = matrix[pos][3]; break;
            default : return 0;
        }
        if (matrixtype==FREQUENCY_MATRIX) return value;
        else if (matrixtype==COUNT_MATRIX || matrixtype==UNNORMALIZED_FREQUENCY_MATRIX) {
            double total=matrix[pos][0]+matrix[pos][1]+matrix[pos][2]+matrix[pos][3];
            return value/total;
        } else if (matrixtype==LOG_ODDS_MATRIX) {
            double fraction=(double)Math.exp(value);
            return fraction*0.25f; // using uniform background frequencies
        } else return value;
    }  
    
    /**
     * Sets the name of the motif (same as ID)
     * 
     * @param a name (ID) for this motif 
     */
    public final void setName(String name) {
        this.name=name;
    }
    
    @Override
    public void rename(String newname) {
        setName(newname);
    } 
   
    private String getMatrixAsString() {
        String result="A:";
        for (int i=0;i<matrix.length;i++) {
            if (i<matrix.length-1) result+=matrix[i][0]+",";
            else result+=matrix[i][0]+";";
        }
        result+="C:";
        for (int i=0;i<matrix.length;i++) {
            if (i<matrix.length-1) result+=matrix[i][1]+",";
            else result+=matrix[i][1]+";";
        }
        result+="G:";
        for (int i=0;i<matrix.length;i++) {
            if (i<matrix.length-1) result+=matrix[i][2]+",";
            else result+=matrix[i][2]+";";
        }
        result+="T:";
        for (int i=0;i<matrix.length;i++) {
            if (i<matrix.length-1) result+=matrix[i][3]+",";
            else result+=matrix[i][3]+";";
        }
        return result;
    }
       
    
    /** Calculates the IC content of a count matrix
     * @param matrix A count matrix where the first index is the position and the second is the base (A=0,C=1,G=2,T=3)
     * @param a boolean flag that can be used to correct the calculation for small sample biases
     * @return the IC (information content) of the matrix
     */
    public static double calculateInformationContent(double[][] targetmatrix, boolean correctForSmallSampleBias) {
        if (targetmatrix==null) return 0;
        double[][] matrix=targetmatrix;
        if (findProbableMatrixType(targetmatrix)==LOG_ODDS_MATRIX) matrix=getMatrixAsFrequencyMatrix(targetmatrix);          
        double ic=0;
        for (int i=0;i<matrix.length;i++) {
            ic+=calculateColumnIC(matrix[i][0], matrix[i][1], matrix[i][2], matrix[i][3], correctForSmallSampleBias);
        }
        return ic;
    }
   
    
    /** Calculates the information content in one position (for non-log-odds matrices) */
    public static double calculateColumnIC(double a_count, double c_count, double g_count, double t_count, boolean correctForSmallSampleBias) {
       double total= a_count + c_count + g_count + t_count;
       double Pa=a_count/total;
       double Pc=c_count/total;
       double Pg=g_count/total;
       double Pt=t_count/total;
       double entropyA=(Pa>0)?Pa*log2(Pa):0;
       double entropyC=(Pc>0)?Pc*log2(Pc):0;
       double entropyG=(Pg>0)?Pg*log2(Pg):0;
       double entropyT=(Pt>0)?Pt*log2(Pt):0;
       double HsL=-(entropyA+entropyC+entropyG+entropyT);
       if (correctForSmallSampleBias) {
           double errorterm=3f/(2f*(double)Math.log(2)*total); // approximation. Found this in an appendix to a paper by Thomas D. Schneider                       
           return 2-errorterm-HsL;
       }
       else return 2-HsL;
    }
    
    /** Calculates the information content in one position of a matrix (for non-log-odds matrices) */
    public static double calculateColumnIC(double[][] matrix, int column, boolean correctForSmallSampleBias) {
       if (matrix==null || column<0 || column>=matrix.length) return 0;
       return calculateColumnIC(matrix[column][0], matrix[column][1], matrix[column][2], matrix[column][3], correctForSmallSampleBias);
    }    
    
    /** Return the Log2-value of the input*/
    private static double log2(double value) {
        return (double)(Math.log(value)/Math.log(2));
    }
   
    public static double calculateGCContent(double[][] targetmatrix) {
        if (targetmatrix==null) return 0;
        double[][] matrix=targetmatrix;
        if (findProbableMatrixType(targetmatrix)==LOG_ODDS_MATRIX) matrix=getMatrixAsFrequencyMatrix(targetmatrix);          
        double GC=0;
        for (int i=0;i<matrix.length;i++) {
            double posGC=(double)(matrix[i][1]+matrix[i][2])/(double)(matrix[i][0]+matrix[i][1]+matrix[i][2]+matrix[i][3]);
            GC+=posGC;
        }
        return GC/(double)matrix.length;
    }

    /** 
     * Returns a consensus string for the given matrix
     * The consensus is determined according the the rules described in Transfac
     * 1) If a single base occurs in at least 50% of the sequences in a given position, that base is returned for that position
     * 2) A double-degenerate base (W,S,R,Y,M,K) is returned if two bases together occurs in at least 75% of the sequences and rule 1 does not apply
     * 3) A triple-degenerate base (B,D,H,V) is returned if none of the previous rules apply and one base is missing from all sequences in this position
     * 4) An 'N' is returned if no other rules apply
     * @return a string with the consensus for the matrix
     */
    public static String getConsensusForMatrix(double[][] targetmatrix) {
       if (targetmatrix==null) return null;
       double[][] matrix=targetmatrix;
       if (findProbableMatrixType(targetmatrix)==LOG_ODDS_MATRIX) matrix=getMatrixAsFrequencyMatrix(targetmatrix);          
       String consensus="";
       for (int i=0;i<matrix.length;i++) { 
          double a=matrix[i][0];
          double c=matrix[i][1];
          double g=matrix[i][2];
          double t=matrix[i][3];
          consensus+=getConsensusBase(a, c, g, t);
       }       
      return consensus;
    }
    
    /** Returns the IUPAC consensus base letter that best describes the given base distribution */
    public static String getConsensusBase(double a, double c, double g, double t) {
       double total=a+c+g+t;       
       if (total==0) return "n";
       String consensusletter="n";       
            if (a/total>=0.5 && a>=2*c && a>=2*g && a>=2*t) consensusletter="A"; 
       else if (c/total>=0.5 && c>=2*a && c>=2*g && c>=2*t) consensusletter="C"; 
       else if (g/total>=0.5 && g>=2*a && g>=2*c && g>=2*t) consensusletter="G"; 
       else if (t/total>=0.5 && t>=2*a && t>=2*c && t>=2*g) consensusletter="T"; 
       else if ((a+c)/total>=0.75) consensusletter="m"; 
       else if ((a+g)/total>=0.75) consensusletter="r"; 
       else if ((a+t)/total>=0.75) consensusletter="w"; 
       else if ((c+g)/total>=0.75) consensusletter="s"; 
       else if ((c+t)/total>=0.75) consensusletter="y"; 
       else if ((g+t)/total>=0.75) consensusletter="k"; 
       else if (a==0) consensusletter="b"; 
       else if (c==0) consensusletter="d"; 
       else if (g==0) consensusletter="h"; 
       else if (t==0) consensusletter="v";    
       return consensusletter;
    }
    
    /** 
     * Returns a frequency matrix based on a set of aligned binding sites of equal length
     */
    public static double[][] getMatrixForAlignedBindingSites(ArrayList<String> strings) throws ParseError {
       if (strings==null || strings.isEmpty()) throw new ParseError("No oligos found");
       int length=strings.get(0).length();
       for (int i=1;i<strings.size();i++) {
           if (strings.get(i).length()!=length) throw new ParseError("All binding sites do not have same lengths");
       }
       double[][] matrix=new double[length][4];
       for (int i=0;i<strings.size();i++) {
           String string=strings.get(i);
           for (int j=0;j<length;j++) {
               char base=string.charAt(j);
               if (base=='A' || base=='a') matrix[j][0]++;
               else if (base=='C' || base=='c') matrix[j][1]++;
               else if (base=='G' || base=='g') matrix[j][2]++;
               else if (base=='T' || base=='t') matrix[j][3]++;
               else throw new ParseError("Unrecognized base: "+base);
           }
       }  
       // normalize to frequency
       double numseq=(double)strings.size();
       for (int j=0;j<length;j++) {
           matrix[j][0]= matrix[j][0]/numseq;
           matrix[j][1]= matrix[j][1]/numseq;
           matrix[j][2]= matrix[j][2]/numseq;
           matrix[j][3]= matrix[j][3]/numseq;
       }      
       return matrix;
    }


    
    @Override
    public void importData(Data source) {
        this.name=((Motif)source).name;  
        this.consensus=((Motif)source).consensus;
        this.shortname=((Motif)source).shortname;
        this.longname=((Motif)source).longname;
        this.description=((Motif)source).description;
        this.matrixtype=((Motif)source).matrixtype;
        this.part=((Motif)source).part;
        this.IC_content=((Motif)source).IC_content;
        this.GC_content=((Motif)source).GC_content;
        this.organisms=((Motif)source).organisms;
        this.quality=((Motif)source).quality;
        this.bindingfactors=((Motif)source).bindingfactors;
        this.classification=((Motif)source).classification;
        this.interactionpartners=((Motif)source).interactionpartners;
        this.knownduplicates=((Motif)source).knownduplicates;
        // this.tissues=((Motif)source).tissues;
        this.tissueExpression=((Motif)source).tissueExpression;
        double[][] sourcematrix=((Motif)source).matrix;
        //this.matrix=(double[][])sourcematrix.clone();
        this.matrix=sourcematrix;
        this.properties=((Motif)source).properties;
        this.GOterms=((Motif)source).GOterms;
        //notifyListenersOfDataUpdate(); 
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Motif clone() {
        Motif motif=new Motif(name);
        motif.consensus=this.consensus;
        motif.shortname=this.shortname;
        motif.longname=this.longname;
        motif.description=this.description;
        motif.matrixtype=this.matrixtype;
        motif.part=this.part;
        motif.IC_content=this.IC_content;
        motif.GC_content=this.GC_content;
        motif.quality=this.quality;
        motif.bindingfactors=this.bindingfactors;
        motif.classification=this.classification;
        motif.matrix=(this.matrix==null)?null:(double[][])this.matrix.clone();
        motif.organisms=this.organisms;
        motif.interactionpartners=(this.interactionpartners==null)?null:(ArrayList<String>)this.interactionpartners.clone();
        motif.knownduplicates=(this.knownduplicates==null)?null:(ArrayList<String>)this.knownduplicates.clone();
        //motif.tissues=(this.tissues==null)?null:(ArrayList<Short>)this.tissues.clone(); // Deprecated. Do not include!
        motif.tissueExpression=(this.tissueExpression==null)?null:(ArrayList<String>)this.tissueExpression.clone();
        motif.GOterms=(this.GOterms==null)?null:(int[])this.GOterms.clone();
        motif.properties=cloneProperties();//properties.clone();
        return motif;
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
     * Returns true if this Motif equals the other given Motif
     * @param other
     * @return
     */
    @Override
    public boolean containsSameData(Data othermotif) {
        if (othermotif==null || !(othermotif instanceof Motif)) return false;
        Motif other=(Motif)othermotif;
        if ((other.shortname==null && this.shortname!=null) || (other.shortname!=null && this.shortname==null) || (other.shortname!=null && this.shortname!=null && !other.shortname.equals(this.shortname))) return false;
        if ((other.longname==null && this.longname!=null) || (other.longname!=null && this.longname==null) ||  (other.longname!=null && this.longname!=null && !other.longname.equals(this.longname))) return false;
        if ((other.description==null && this.description!=null) || (other.description!=null && this.description==null) ||  (other.description!=null && this.description!=null && !other.description.equals(this.description))) return false;
        if ((other.consensus==null && this.consensus!=null) || (other.consensus!=null && this.consensus==null) ||  (other.consensus!=null && this.consensus!=null && !other.consensus.equals(this.consensus))) return false;
        if ((other.part==null && this.part!=null) || (other.part!=null && this.part==null) ||  (other.part!=null && this.part!=null && !other.part.equals(this.part))) return false;
        if (other.quality!=this.quality) return false;
        if ((other.bindingfactors==null && this.bindingfactors!=null) || (other.bindingfactors!=null && this.bindingfactors==null) ||  (other.bindingfactors!=null && this.bindingfactors!=null && !other.bindingfactors.equals(this.bindingfactors))) return false;
        if ((other.classification==null && this.classification!=null) || (other.classification!=null && this.classification==null) ||  (other.classification!=null && this.classification!=null && !other.classification.equals(this.classification))) return false;
        if ((other.organisms==null && this.organisms!=null) || (other.organisms!=null && this.organisms==null) ||  (other.organisms!=null && this.organisms!=null && !other.organisms.equals(this.organisms))) return false;
        if ((other.knownduplicates==null && this.knownduplicates!=null) || (other.knownduplicates!=null && this.knownduplicates==null) ||  (other.knownduplicates!=null && this.knownduplicates!=null && !listcompare(this.knownduplicates,other.knownduplicates))) return false;
        if ((other.interactionpartners==null && this.interactionpartners!=null) || (other.interactionpartners!=null && this.interactionpartners==null) ||  (other.interactionpartners!=null && this.interactionpartners!=null && !listcompare(this.interactionpartners,other.interactionpartners))) return false;
        // if ((other.tissues==null && this.tissues!=null) || (other.tissues!=null && this.tissues==null) ||  (other.tissues!=null && this.tissues!=null && !listcompare(this.tissues,other.tissues))) return false;
        if ((other.tissueExpression==null && this.tissueExpression!=null) || (other.tissueExpression!=null && this.tissueExpression==null) ||  (other.tissueExpression!=null && this.tissueExpression!=null && !listcompare(this.tissueExpression,other.tissueExpression))) return false;
        if ((other.GOterms==null && this.GOterms!=null) || (other.GOterms!=null && this.GOterms==null) ||  (other.GOterms!=null && this.GOterms!=null && !MotifLabEngine.listcompare(this.GOterms,other.GOterms))) return false;
        if ((other.properties==null && this.properties!=null) || (other.properties!=null && this.properties==null) ||  (other.properties!=null && this.properties!=null && !this.properties.equals(other.properties))) return false;
        if (other.matrixtype!=this.matrixtype) return false; // needed in order to avoid follow-up errors from old bug
        if (!matrixCompare(other.matrix, this.matrix)) return false;
        return true;
    }
    
    /** Returns TRUE if the two double[][] arrays are equal */
    private boolean matrixCompare(double[][] first, double[][] second) {
        if (first==null && second==null) return true;
        if (first!=null && second==null) return false;
        if (first==null && second!=null) return false;
        if (first.length!=second.length) return false;
        for (int i=0;i<first.length;i++) {
            if (first[i].length!=second[i].length) return false;
            for (int j=0;j<first[i].length;j++) {
                if (first[i][j]!=second[i][j]) return false;
            }
        }
        return true;
    }
    
    public static String getType() {return typedescription;}    

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}

    @Override
    public int compareTo(Object other) {
        if (!(other instanceof Motif)) return -1;
        return getPresentationName().compareTo(((Motif)other).getPresentationName());
    }

    /** Compares two lists and returns TRUE if the lists contain the same elements (irrespective of order)
     *  Note that the order of elements in the two lists will be modified, so use clones if you want to
     *  keep the original order!
     */
    @SuppressWarnings("unchecked")
    private boolean listcompare(ArrayList<? extends Comparable> list1, ArrayList<? extends Comparable>  list2) {
        if (list1.size()!=list2.size()) return false;
        java.util.Collections.sort(list1);
        java.util.Collections.sort(list2);
        return list1.equals(list2);
    }
    
    /**
     * Returns the tissue ID used in stead of String names when referring to tissues
     * (Note that tissue IDs can change from session to session, so always use proper
     * String names for "cross-session references"
     */
    @Deprecated
    private static short getTissueIndexForName(String name) {
        if (tissueIDTable==null) tissueIDTable=new HashMap<>();
        if (tissueIDTable.containsKey(name)) return tissueIDTable.get(name);
        else {
            short newID=(short)tissueNameTable.size();
            tissueNameTable.add(name);
            tissueIDTable.put(name, newID);
            return newID;
        }
    }

    /**
     * Tries to figure out the most probable type of matrix this is based on the values in the matrix
     * The rules are as follows:
     *      If all matrix values are positive integers it is assumed that the matrix is a count matrix.
     *      else, If all numbers are between 0 and 1 (inclusive) and the sum is ~1.0 it is assumed that the matrix is a frequency matrix
     *      else, if any bases are negative it is assumed that the matrix is a log-odds matrix
     *      else, it is assumed that the matrix is an unnormalized frequency matrix
     * @return a number specifying the assumed type of matrix (or -1 if matrix is NULL)
     */
    public static int findProbableMatrixType(double[][] targetmatrix) {
        if (targetmatrix==null) return -1;
        boolean allIntegers=true;
        for (int i=0;i<targetmatrix.length;i++) { 
            for (int j=0;j<4;j++) {
                double value=targetmatrix[i][j];
                if (value<0 || (double)((int)value)!=value) {allIntegers=false;break;} 
            }  
        }
        if (allIntegers) return COUNT_MATRIX;
        boolean allBetween0and1=true;
        boolean sumToOne=true;        
        for (int i=0;i<targetmatrix.length;i++) {
            double sum=0;
            for (int j=0;j<4;j++) {
                double value=targetmatrix[i][j];
                sum+=value;
                if (value<0 || value>1) {allBetween0and1=false;break;} 
            }  
            if (sum<0.999999 || sum>1.00001) sumToOne=false; // allow slight deviation from 1.0 to account for rounding problems
        }    
        if (allBetween0and1 && sumToOne) return FREQUENCY_MATRIX;
        for (int i=0;i<targetmatrix.length;i++) { 
            for (int j=0;j<4;j++) {
                double value=targetmatrix[i][j];
                if (value<0) return LOG_ODDS_MATRIX;
            }  
        }            
        return UNNORMALIZED_FREQUENCY_MATRIX;
    }
    
    /**
     * Returns this matrix as a count matrix with integer values
     * Note that if the original matrix was not a count matrix,
     * or if the sum-count for each position is not the same,
     * a new count matrix will be derived based on a normalized
     * frequency matrix, and the precision of the counts will
     * be determined by the given decimals (between 1 and 6)
     * @return 
     */
    public int[][] getMatrixAsCountMatrix(int decimals) {        
        if (decimals<1) decimals=1; if (decimals>6) decimals=6;
        if (matrix==null || matrix.length==0) return new int[0][4];
        int[][] result=new int[matrix.length][4];
        if (matrixtype==COUNT_MATRIX) {
            int sum=(int)matrix[0][0]+(int)matrix[0][1]+(int)matrix[0][2]+(int)matrix[0][3];   
            boolean samesum=true;
            // check and verify that the matrix is consistent (that all columns sum to the same value)
            for (int i=0;i<matrix.length;i++) {
                int colsum=(int)matrix[i][0]+(int)matrix[i][1]+(int)matrix[i][2]+(int)matrix[i][3];
                if (colsum!=sum) {samesum=false;break;}
                result[i][0]=(int)matrix[i][0];
                result[i][1]=(int)matrix[i][1];
                result[i][2]=(int)matrix[i][2];
                result[i][3]=(int)matrix[i][3];
            }   
            if (samesum) return result; // if it is consistent just return the original count matrix           
        }
        // derive counts from normalized matrix
        double[][] pfm=getMatrixAsFrequencyMatrix();
        int sum=1; // I will do the sum manually just to make sure the precision is absolute
             if (decimals==1) {sum=1;}
        else if (decimals==2) {sum=10;}
        else if (decimals==3) {sum=100;}
        else if (decimals==4) {sum=1000;}
        else if (decimals==5) {sum=10000;}
        else if (decimals==6) {sum=100000;}
        for (int i=0;i<pfm.length;i++) {
            if (sum==1) { // just use the one with the highest frequency
                double max=MotifLabEngine.getMaximumValue(pfm[i]);   
                int index=MotifLabEngine.getIndexOfFirstMatch(pfm[i],max);
                for (int j=0;j<4;j++) result[i][j]=((j==index)?1:0);
            } else {
                int[] counts=new int[4];
                counts[0]=(int)Math.round(pfm[i][0]*sum);
                counts[1]=(int)Math.round(pfm[i][1]*sum);
                counts[2]=(int)Math.round(pfm[i][2]*sum);
                counts[3]=(int)Math.round(pfm[i][3]*sum);
                int totalSum=counts[0]+counts[1]+counts[2]+counts[3];           
                while (totalSum!=sum) { // adjust so that the counts are correct
                    if (totalSum>sum) { // subtract one count from the one with the largest count
                        int index=MotifLabEngine.getFirstIndexOfLargestValue(counts);
                        counts[index]--;
                    } else if (totalSum<sum) { // add one count to the one with the largest count
                        int index=MotifLabEngine.getFirstIndexOfLargestValue(counts);
                        counts[index]++;
                    }
                    totalSum=counts[0]+counts[1]+counts[2]+counts[3];                  
                }
                result[i][0]=counts[0];
                result[i][1]=counts[1];
                result[i][2]=counts[2];
                result[i][3]=counts[3];
            }
        }
        return result;
    }
    
    
    
    /** Returns a normalized frequency matrix for this 
     *  The returned matrix must not be modified. If the original matrix for
     *  this motif is already a FREQUENCY_MATRIX then the original matrix is returned,
     *  if not a new matrix with converted values are returned!
     */
    public double[][] getMatrixAsFrequencyMatrix() {
        if (matrixtype==FREQUENCY_MATRIX) return getMatrix(); // already a normalized frequency matrix
        else if (matrixtype==LOG_ODDS_MATRIX) {
            double[][] result=copyMatrix(getMatrix());
            for (int i=0;i<result.length;i++) {
              for (int j=0;j<4;j++) {
                  result[i][j]=(double)Math.exp(result[i][j])*0.25f;
              }
              double total=result[i][0]+result[i][1]+result[i][2]+result[i][3];
              result[i][0]=result[i][0]/total;
              result[i][1]=result[i][1]/total;
              result[i][2]=result[i][2]/total;
              result[i][3]=result[i][3]/total;               
            } 
            return result;
        } else { // It's either a count matrix or an un-normalized frequency matrix, so we just need to normalize
            double[][] result=copyMatrix(getMatrix());
            for (int i=0;i<result.length;i++) {
                double total=result[i][0]+result[i][1]+result[i][2]+result[i][3];
                result[i][0]=result[i][0]/total;
                result[i][1]=result[i][1]/total;
                result[i][2]=result[i][2]/total;
                result[i][3]=result[i][3]/total;
            }
            return result;
        }
    }
    
    /** Returns this matrix as a log-transformed weight matrix
     *  If this is already log transformed, the original is returned.
     *  if not a new matrix with converted values are returned!
     */
    public double[][] getMatrixAsLogMatrix() {
        if (matrixtype==LOG_ODDS_MATRIX) return getMatrix(); // already a log matrix
        else { // It's either a count matrix or a frequency matrix. Normalize first, then log transform it
            double[][] result=getMatrixAsFrequencyMatrixWithPseudo(0.005); // add a small zero to avoid log(0)
            for (int i=0;i<result.length;i++) {
                result[i][0]=Math.log(result[i][0]/0.25);
                result[i][1]=Math.log(result[i][1]/0.25);
                result[i][2]=Math.log(result[i][2]/0.25);
                result[i][3]=Math.log(result[i][3]/0.25);
            }
            return result;
        }
    }    
    
    /** Returns a normalized frequency matrix for the given matrix
     *  The returned matrix must not be modified. If the original matrix for
     *  this motif is FREQUENCY_MATRIX then the original matrix is returned,
     *  if not a new matrix with converted values are returned!
     */
    public static double[][] getMatrixAsFrequencyMatrix(double[][] matrix) {
        int type=findProbableMatrixType(matrix);        
        if (type==FREQUENCY_MATRIX) return matrix;
        else if (type==LOG_ODDS_MATRIX) {
            double[][] result=copyMatrix(matrix);
            for (int i=0;i<result.length;i++) {
              for (int j=0;j<4;j++) {
                  result[i][j]=(double)Math.exp(result[i][j])*0.25f;
              }
              double total=result[i][0]+result[i][1]+result[i][2]+result[i][3];
              result[i][0]=result[i][0]/total;
              result[i][1]=result[i][1]/total;
              result[i][2]=result[i][2]/total;
              result[i][3]=result[i][3]/total;              
            } 
            return result;
        } else { // just need to normalize
            double[][] result=copyMatrix(matrix);
            for (int i=0;i<result.length;i++) {
                double total=result[i][0]+result[i][1]+result[i][2]+result[i][3];
                result[i][0]=result[i][0]/total;
                result[i][1]=result[i][1]/total;
                result[i][2]=result[i][2]/total;
                result[i][3]=result[i][3]/total;
            }
            return result;
        }
    }    

    /** Returns frequency matrix with pseudo frequencies added
     *  First, usual normalized frequency matrix is obtained then the pseudo-value
     *  is added to all entries (so that each column sums to 1.0+pseudo*4;
     */
    public double[][] getMatrixAsFrequencyMatrixWithPseudo(double pseudo) {
        double[][] result=copyMatrix(getMatrix());
        if (matrixtype==FREQUENCY_MATRIX) {}
        else if (matrixtype==LOG_ODDS_MATRIX) {
            for (int i=0;i<result.length;i++) {
              for (int j=0;j<4;j++) {
                  result[i][j]=(double)Math.exp(result[i][j])*0.25f;
              }
              double total=result[i][0]+result[i][1]+result[i][2]+result[i][3];
              result[i][0]=result[i][0]/total;
              result[i][1]=result[i][1]/total;
              result[i][2]=result[i][2]/total;
              result[i][3]=result[i][3]/total;                
            }
        } else { // count matrix or something similar. just need to normalize
            for (int i=0;i<result.length;i++) {
                double total=result[i][0]+result[i][1]+result[i][2]+result[i][3];
                result[i][0]=result[i][0]/total;
                result[i][1]=result[i][1]/total;
                result[i][2]=result[i][2]/total;
                result[i][3]=result[i][3]/total;
            }
        }
        // add pseudo
        for (int i=0;i<result.length;i++) {
            result[i][0]+=pseudo;
            result[i][1]+=pseudo;
            result[i][2]+=pseudo;
            result[i][3]+=pseudo;
        }
        // renormalize and return
        result=normalizeMatrix(result);
        return result;
    }

    /** Given a matrix, this method will normalize it to a frequency matrix with each column summing to 1.0 */
    public static double[][] normalizeMatrix(double[][] matrix) {
      double[][] normalized=new double[matrix.length][4];
      for (int i=0;i<matrix.length;i++) {
        double total=matrix[i][0]+matrix[i][1]+matrix[i][2]+matrix[i][3];
        normalized[i][0]=matrix[i][0]/total;
        normalized[i][1]=matrix[i][1]/total;
        normalized[i][2]=matrix[i][2]/total;
        normalized[i][3]=matrix[i][3]/total;
      }
      return normalized;
    }
    
    /** Given a matrix, this method will normalize it to a frequency matrix with each column summing to 1.0 */
    public static Double[][] normalizeMatrix(Double[][] matrix) {
      Double[][] normalized=new Double[matrix.length][4];
      for (int i=0;i<matrix.length;i++) {
        double total=matrix[i][0]+matrix[i][1]+matrix[i][2]+matrix[i][3];
        normalized[i][0]=matrix[i][0]/total;
        normalized[i][1]=matrix[i][1]/total;
        normalized[i][2]=matrix[i][2]/total;
        normalized[i][3]=matrix[i][3]/total;
      }
      return normalized;
    }    
    


    /** Given a matrix as double array, this method will return the reverse complement of the source matrix*/
    public static double[][] reverseComplementMatrix(double[][] source) {
        if (source==null) return null;
        double[][] result=new double[source.length][4];
        for (int i=0;i<source.length;i++) {
            int revpos=(source.length-1)-i;
            result[revpos][0]=source[i][3]; //
            result[revpos][1]=source[i][2]; //
            result[revpos][2]=source[i][1]; //
            result[revpos][3]=source[i][0]; //
        }
        return result;
    }
    
    /** Given a matrix as double array, this method will return the 'inverse' of the source matrix
      * The inverse is not the same as the reverse complement. Rather, the inverse is the mirror image
     *  of the matrix as read from the end to the start. E.g.: the inverse of TTACTG is GTCATT
     */
    public static double[][] inverseMatrix(double[][] source) {
        if (source==null) return null;
        double[][] result=new double[source.length][4];
        for (int i=0;i<source.length;i++) {
            int revpos=(source.length-1)-i;
            result[revpos][0]=source[i][0]; //
            result[revpos][1]=source[i][1]; //
            result[revpos][2]=source[i][2]; //
            result[revpos][3]=source[i][3]; //
        }
        return result;
    }    

    /** Given a matrix as double array, this method will return a copy of the source matrix*/
    public static double[][] copyMatrix(double[][] source) {
        double[][] result=new double[source.length][4];
        for (int i=0;i<source.length;i++) {
            result[i][0]=source[i][0]; //
            result[i][1]=source[i][1]; //
            result[i][2]=source[i][2]; //
            result[i][3]=source[i][3]; //
        }
        return result;
    }
    
    /** Given a matrix as double array, this method will return
     *  a new matrix where the position "columns" (1x4) have been shuffled
     */
    public static double[][] shuffleMatrix(double[][] source) {
        if (source==null) return null;
        double[][] result=copyMatrix(source);    
        int index;
        Random random = new Random();
        for (int i=result.length-1; i>0; i--) {
            index = random.nextInt(i + 1);
            swapColumns(result,index,i);
        }             
        return result;
    }    
    
    /** Given a matrix as double array, this method will return
     *  a new matrix where every value has been rounded to the nearest integer
     */
    public static double[][] roundMatrix(double[][] source) {
        if (source==null) return null;
        double[][] result=copyMatrix(source);    
        for (int i=0;i<result.length;i++) {
            result[i][0]=Math.round(source[i][0]);
            result[i][1]=Math.round(source[i][1]);
            result[i][2]=Math.round(source[i][2]);
            result[i][3]=Math.round(source[i][3]);
        }
        return result;
    }       
    
    /** Given a matrix as double array, this method will return
     *  a new matrix where the "begin" first columns and "end" last columns
     *  have been removed.
     */
    public static double[][] trimMatrix(double[][] source, int begin, int end) {
        if (source==null) return null;       
        if (begin<0) begin=0;
        if (end<0) end=0; 
        int resultcolumns=source.length-(begin+end);
        if (resultcolumns<=0) return new double[0][4];
        double[][] result=new double[resultcolumns][4];
        for (int i=0;i<resultcolumns;i++) {
            result[i][0]=source[begin+i][0]; //
            result[i][1]=source[begin+i][1]; //
            result[i][2]=source[begin+i][2]; //
            result[i][3]=source[begin+i][3]; //
        }
        return result;
    }     
    
    /**
     * swaps the "columns" M[j][?] and M[i][?] in the double array
     */
    private static void swapColumns(double[][] array, int i, int j) {
        for (int x=0;x<array[0].length;x++) {
            double swap=array[i][x];
            array[i][x]=array[j][x];
            array[j][x]=swap;
        }      
    }
    
    /**
     * Takes two matrices as input (of sizes NxR and MxR) and returns a new matrix
     * by appending the second matrix to the first (new size is (N+M)xR). 
     * A NULL value will be returned if the two matrices do not have the same number of elements
     * along the second index. If any of the inputs are NULL or empty, the other value will be returned
     * unaltered.
     * @param first
     * @param second
     * @return 
     */
    public static double[][] spliceMatrices(double[][] first, double[][] second) {
        if (first==null || first.length==0) return second;
        if (second==null || second.length==0) return first;
        if (first.length==0 && second.length==0) return first;
        if (first[0].length!=second[0].length) return null;
        int bases=first[0].length;
        double[][] result=new double[first.length+second.length][bases];
        for (int i=0;i<first.length;i++) {
            System.arraycopy(first[i], 0, result[i], 0, bases);
        }  
        for (int i=0;i<second.length;i++) {
            System.arraycopy(second[i], 0, result[i+first.length], 0, bases);
        }                
        return result;
    }
    
    /** Given a matrix as double array, this method will return a copy of the source matrix*/
    public void debugMatrix() {      
        if (matrix==null || matrix.length==0) {
            System.err.println("Matrix is empty");
            return;
        }
        for (int i=0;i<matrix.length;i++) {
            System.err.print(matrix[i][0]);
            System.err.print(",");            
            System.err.print(matrix[i][1]);
            System.err.print(",");            
            System.err.print(matrix[i][2]);
            System.err.print(",");            
            System.err.println(matrix[i][3]);
        }
    }    

    public static double getMaxValueInMatrix(double[][] source) {
        double max=-Double.MAX_VALUE;
        for (int i=0;i<source.length;i++) {
            for (int j=0;j<source[i].length;j++) {
                if (source[i][j]>max) max=source[i][j];
            }
        }
        return max;
    }

    public double getMaxValueInMatrix() {
        if (matrix==null) return 0;
        double max=-Double.MAX_VALUE;
        for (int i=0;i<matrix.length;i++) {
            for (int j=0;j<matrix[i].length;j++) {
                if (matrix[i][j]>max) max=matrix[i][j];
            }
        }
        return max;
    }

    /** If the matrix is a count matrix this method will return the number of
      * sequences used to derive the matrix (based on the total count in the first position)
     *  For other matrix type, a value of 0 is returned
     */
    public int getNumberOfSequences() {
        if (matrixtype!=COUNT_MATRIX || matrix==null) return 0;
        double count=0;
        for (int j=0;j<matrix[0].length;j++) {
            count+=matrix[0][j];
        }
        return (int)count;
    }

    /**
     * If the given text contains newlines, tabs, backslashes
     * or (optionally) semicolons, these will be escaped
     * @param text
     * @return 
     */
    public static String escapePropertyText(String text, boolean escapeSemiColon) {
        if (text==null) return text;
        text=text.replace("\\", "\\\\"); // escape literal \ characters in the text as \\
        text=text.replace("\n", "\\n");    // escape newlines as \n
        text=text.replace("\t", "\\t");    // escape tabs as \t
        if (escapeSemiColon) text=text.replace(";", "\\;");    // escape semicolons
        return text;
    }
    
    /**
     * If the given text contains escaped newlines, tabs, backslashes
     * or (optionally) semicolons, these will be unescaped
     * @param text
     * @return 
     */
    public static String unescapePropertyText(String text, boolean escapeSemiColon) {
        if (text==null) return text;
        text=text.replace("\\\\", "\b");  // unescape \ character (convert \\ to "bell" temporarily to avoid subsequent problems with the single \ being interpreted as an escape character)        
        text=text.replace("\\n", "\n");   // unescape newlines
        text=text.replace("\\t", "\t");   // unescape tabs  
        if (escapeSemiColon) text=text.replace("\\;", ";"); // unescape semicolons     
        text=text.replace("\b", "\\"); // convert the temporary bell back to \
        return text;
    }    
    
    /** Creates a new motif instance initialized from a parameter-string */
    public static Motif parseMotifParameters(String text, String targetName, MotifLabEngine engine) throws ExecutionError { 
        if (MotifLabEngine.startsWithIgnoreCase(text,"complement:")) text="reverse:"+text.substring("complement:".length()); // convert "complement:" prefix to "reverse:"
        if (MotifLabEngine.startsWithIgnoreCase(text,"reverse:") || MotifLabEngine.startsWithIgnoreCase(text,"inverse:")) { // create motif based on reverse complement or inverse of existing motif
            String originalMotifName=text.substring(8); // works for both reverse and inverse since they have the same length=8
            Object motif=engine.getDataItem(originalMotifName);
            if (motif==null) throw new ExecutionError("Unknown motif: "+originalMotifName);
            if (!(motif instanceof Motif)) throw new ExecutionError("'"+originalMotifName+"' is not a motif");
            Motif motifCopy=((Motif)motif).clone();
            motifCopy.setName(targetName);
            double[][] matrix;
            if (MotifLabEngine.startsWithIgnoreCase(text,"reverse:")) matrix=Motif.reverseComplementMatrix(motifCopy.getMatrix());
            else matrix=Motif.inverseMatrix(motifCopy.getMatrix());
            motifCopy.setMatrix(matrix);
            return motifCopy;
        } else if (MotifLabEngine.startsWithIgnoreCase(text,"flank:")) {
            String[] parts=text.substring("flank:".length()).split("\\s*,\\s*");
            if (parts.length!=3) throw new ExecutionError("Format of 'flank' function should be \"flank:motifName,prefix,suffix\"");
            String originalMotifName=parts[0];
            String motifPrefix=parts[1];
            String motifSuffix=parts[2];
            // strip quotes
            if (motifPrefix.endsWith("'") || motifPrefix.endsWith("\"")) motifPrefix=motifPrefix.substring(0, motifPrefix.length()-1);
            if (motifPrefix.startsWith("'") || motifPrefix.startsWith("\"")) motifPrefix=motifPrefix.substring(1);
            if (motifSuffix.endsWith("'") || motifSuffix.endsWith("\"")) motifSuffix=motifSuffix.substring(0, motifSuffix.length()-1);
            if (motifSuffix.startsWith("'") || motifSuffix.startsWith("\"")) motifSuffix=motifSuffix.substring(1);
            Object motif=engine.getDataItem(originalMotifName);
            if (motif==null) throw new ExecutionError("Unknown motif: "+originalMotifName);
            if (!(motif instanceof Motif)) throw new ExecutionError("'"+originalMotifName+"' is not a motif");
            Motif motifCopy=((Motif)motif).clone();
            motifCopy.setName(targetName);
            double[][] matrix=motifCopy.getMatrixAsFrequencyMatrix();            
            if (!(motifPrefix.isEmpty() || motifPrefix.equals("*") || motifPrefix.equals("0"))) {
                 double[][] prefixMatrix=getMatrixFromConsensus(motifPrefix);
                 matrix=spliceMatrices(normalizeMatrix(prefixMatrix), matrix);
            }
            if (!(motifSuffix.isEmpty() || motifSuffix.equals("*") || motifSuffix.equals("0"))) {
                 double[][] suffixMatrix=getMatrixFromConsensus(motifSuffix);
                 matrix=spliceMatrices(matrix, normalizeMatrix(suffixMatrix));
            }                      
            motifCopy.setMatrix(matrix);
            return motifCopy;            
        } else if (MotifLabEngine.startsWithIgnoreCase(text,"shuffle:")) {
            String originalMotifName=text.substring("shuffle:".length()); 
            Object motif=engine.getDataItem(originalMotifName);
            if (motif==null) throw new ExecutionError("Unknown motif: "+originalMotifName);
            if (!(motif instanceof Motif)) throw new ExecutionError("'"+originalMotifName+"' is not a motif");
            Motif motifCopy=((Motif)motif).clone();
            motifCopy.setName(targetName);
            double[][] matrix;
            matrix=Motif.shuffleMatrix(motifCopy.getMatrix());
            motifCopy.setMatrix(matrix);
            return motifCopy;          
        } else if (MotifLabEngine.startsWithIgnoreCase(text,"round:")) {
            String originalMotifName=text.substring("round:".length());  
            Object motif=engine.getDataItem(originalMotifName);
            if (motif==null) throw new ExecutionError("Unknown motif: "+originalMotifName);
            if (!(motif instanceof Motif)) throw new ExecutionError("'"+originalMotifName+"' is not a motif");
            Motif motifCopy=((Motif)motif).clone();
            motifCopy.setName(targetName);
            double[][] matrix;
            matrix=Motif.roundMatrix(motifCopy.getMatrix());
            motifCopy.setMatrix(matrix);
            return motifCopy;          
        } else if (MotifLabEngine.startsWithIgnoreCase(text,"combine:")) {
            String[] parts=text.substring("combine:".length()).split("\\s*,\\s*");
            if (parts.length<2 || parts.length>3) throw new ExecutionError("Format of 'combine' function should be \"combine:motifName1,motifName2 [,distance]\"");
            String motifName1=parts[0];
            String motifName2=parts[1];
            int distance=0;
            if (parts.length==3) {
                try {distance=Integer.parseInt(parts[2]);} 
                catch (NumberFormatException e) {throw new ExecutionError("Format of 'combine' function should be \"combine:motifName1,motifName2 [,distance]\"");}
            }
            Object motif1=engine.getDataItem(motifName1);
            if (motif1==null) throw new ExecutionError("Unknown motif: "+motifName1);
            if (!(motif1 instanceof Motif)) throw new ExecutionError("'"+motifName1+"' is not a motif. Unable to combine.");
            Object motif2=engine.getDataItem(motifName2);
            if (motif2==null) throw new ExecutionError("Unknown motif: "+motifName2);
            if (!(motif2 instanceof Motif)) throw new ExecutionError("'"+motifName2+"' is not a motif. Unable to combine.");
            double[][] motifMatrix1=((Motif)motif1).getMatrixAsFrequencyMatrix();
            double[][] motifMatrix2=((Motif)motif2).getMatrixAsFrequencyMatrix();
            Motif targetMotif=new Motif(targetName);
            double[][] newMatrix=null;
            int newlength=0;    
            if (distance>=0) { // no overlap between first and second matrix
                newlength=motifMatrix1.length+distance+motifMatrix2.length;
                newMatrix=new double[newlength][4];
                for (int i=0;i<newlength;i++) {
                    if (i<motifMatrix1.length) { // within first matrix
                        System.arraycopy(motifMatrix1[i], 0, newMatrix[i], 0, 4); // copy column from first matrix
                    } else if (i>=motifMatrix1.length+distance) { // within second matrix
                        System.arraycopy(motifMatrix2[i-(motifMatrix1.length+distance)], 0, newMatrix[i], 0, 4); // copy column from second matrix
                    } else { // within spacer region
                        newMatrix[i][0]=0.25; newMatrix[i][1]=0.25; newMatrix[i][2]=0.25; newMatrix[i][3]=0.25; // default to uniform distribution
                    }
                }
                
            } else { // potential overlap
                throw new ExecutionError("Negative distance is not supported yet...");
                //
                // ====>  NOTE::  Use weighted average when combining columns from two matrices  (IC-weighted)  <=====
                //
            }
            targetMotif.setMatrix(newMatrix);            
            return targetMotif;  
        } else if (MotifLabEngine.startsWithIgnoreCase(text,"trim:")) {
            String[] parts=text.substring("trim:".length()).split("\\s*,\\s*");
            if (parts.length!=3) throw new ExecutionError("Format of 'trim' function should be \"trim:motifName,columnsToTrimFromBeginning,columnsToTrimFromEnd\"");
            String originalMotifName=parts[0];
            int columnsFromStart=0;
            int columnsFromEnd=0;
            try {
              columnsFromStart=Integer.parseInt(parts[1]); 
              columnsFromEnd=Integer.parseInt(parts[2]); 
            } catch (NumberFormatException e) {
                throw new ExecutionError("Format of 'trim' function should be \"trim:motifName,columnsToTrimFromBeginning,columnsToTrimFromEnd\"");
            }
            Object motif=engine.getDataItem(originalMotifName);
            if (motif==null) throw new ExecutionError("Unknown motif: "+originalMotifName);
            if (!(motif instanceof Motif)) throw new ExecutionError("'"+originalMotifName+"' is not a motif");
            Motif motifCopy=((Motif)motif).clone();
            motifCopy.setName(targetName);
            double[][] matrix=motifCopy.getMatrix();            
            matrix=trimMatrix(matrix,columnsFromStart,columnsFromEnd); 
            motifCopy.setMatrix(matrix);
            return motifCopy;  
        } else if (MotifLabEngine.startsWithIgnoreCase(text,"trim flanks:")) {
            String[] parts=text.substring("trim flanks:".length()).split("\\s*,\\s*");
            if (parts.length!=2) throw new ExecutionError("Format of 'trim flanks' function should be \"trim flanks:motifName,IC-cutoff\"");
            String originalMotifName=parts[0];
            double ICcutoff=0;
            try {
               ICcutoff=Double.parseDouble(parts[1]); 
            } catch (NumberFormatException e) {
                throw new ExecutionError("Format of 'trim flanks' function should be \"trim flanks:motifName,IC-cutoff\"");
            }
            Object motif=engine.getDataItem(originalMotifName);
            if (motif==null) throw new ExecutionError("Unknown motif: "+originalMotifName);
            if (!(motif instanceof Motif)) throw new ExecutionError("'"+originalMotifName+"' is not a motif");
            Motif motifCopy=((Motif)motif).clone();
            motifCopy.setName(targetName);
            double[][] freqmatrix=motifCopy.getMatrixAsFrequencyMatrix();  
            int columnsFromStart=0;            
            int columnsFromEnd=0;
            int size=(freqmatrix==null)?0:freqmatrix.length;
            for (int i=0;i<size;i++) {
               double IC=calculateColumnIC(freqmatrix, i, false);
               if (IC<ICcutoff) columnsFromStart++;
               else break;
            }
            for (int i=size-1;i>=0;i--) {
               double IC=calculateColumnIC(freqmatrix, i, false);
               if (IC<ICcutoff) columnsFromEnd++;
               else break;
            }            
            double[][] matrix=trimMatrix(motifCopy.getMatrix(),columnsFromStart,columnsFromEnd); 
            motifCopy.setMatrix(matrix);
            return motifCopy;  
        }
        // A new motif is created based on a list of explicit properties
        Motif motif=new Motif(targetName);
        double[][] matrix=null;
        String consensus=null;
        boolean hasMatrix=false;        
        boolean[] gotAll=new boolean[]{false,false,false,false};
        String[] segments=text.split("\\s*(?<!\\\\);\\s*"); // matches ; except when escaped as \;
        for (String segment:segments) {
            if (segment.trim().isEmpty()) continue;
            if (segment.startsWith("SHORTNAME:") && segment.length()>"SHORTNAME:".length()) {
                motif.setShortName(segment.substring("SHORTNAME:".length()).trim());
            } else if (segment.startsWith("LONGNAME:") && segment.length()>"LONGNAME:".length()) {
                motif.setLongName(segment.substring("LONGNAME:".length()).trim());
            } else if (segment.startsWith("CONSENSUS:") && segment.length()>"CONSENSUS:".length()) {
                consensus=segment.substring("CONSENSUS:".length()).trim();
                motif.setConsensusMotif(consensus);
            } else if (segment.startsWith("CLASS:") && segment.length()>"CLASS:".length()) {
                motif.setClassification(segment.substring("CLASS:".length()).trim());
            } else if (segment.startsWith("ORGANISMS:") && segment.length()>"ORGANISMS:".length()) {
                motif.setOrganisms(segment.substring("ORGANISMS:".length()).trim());
            } else if (segment.startsWith("PARTNERS:") && segment.length()>"PARTNERS:".length()) {
                String partnersString=segment.substring("PARTNERS:".length()).trim();
                String[] partners=partnersString.split("\\s*,\\s*");
                motif.setInteractionPartnerNames(partners);
            } else if (segment.startsWith("ALTERNATIVES:") && segment.length()>"ALTERNATIVES:".length()) {
                String alternativesString=segment.substring("ALTERNATIVES:".length()).trim();
                String[] alternatives=alternativesString.split("\\s*,\\s*");
                motif.setKnownDuplicatesNames(alternatives);
            } else if (segment.startsWith("EXPRESSION:") && segment.length()>"EXPRESSION:".length()) {
                String expressionString=segment.substring("EXPRESSION:".length()).trim();
                String[] tissueexpression=expressionString.split("\\s*,\\s*");
                motif.setTissueExpression(tissueexpression);
            } else if (segment.startsWith("QUALITY:") && segment.length()>"QUALITY:".length()) {
                try {
                    motif.setQuality(Integer.parseInt(segment.substring("QUALITY:".length())));
                } catch(Exception e) {throw new ExecutionError("Unable to parse expected numeric value for "+segment);}
            } else if (segment.startsWith("FACTORS:") && segment.length()>"FACTORS:".length()) {
                motif.setBindingFactors(segment.substring("FACTORS:".length()).trim());
            } else if (segment.startsWith("DESCRIPTION:") && segment.length()>"DESCRIPTION:".length()) {
                String description=segment.substring("DESCRIPTION:".length()).trim();
                description=unescapePropertyText(description,true);
                motif.setDescription(description);
            } else if (segment.startsWith("PART:") && segment.length()>"PART:".length()) {
                motif.setPart(segment.substring("PART:".length()).trim());
            } else if (segment.startsWith("HALFSITE")) { // included for backwards compatibility
                motif.setPart(HALFSITE);
            } else if (segment.startsWith("GO-TERMS:")) { //
                String goString=segment.substring("GO-TERMS:".length()).trim();
                String[] GO=goString.split("\\s*,\\s*");
                try {
                    motif.setGOterms(GO);
                } catch (ParseError p) {
                    throw new ExecutionError(p.getMessage(),p);
                }
            } else if ((segment.startsWith("A:") || segment.startsWith("C:") || segment.startsWith("G:") || segment.startsWith("T:"))  && segment.length()>2) {
                hasMatrix=true;
                String[] values=segment.substring(2).split("\\s*,\\s*");
                int index=0;
                     if (segment.startsWith("C")) index=1;
                else if (segment.startsWith("G")) index=2;
                else if (segment.startsWith("T")) index=3;
                if (matrix==null) matrix=new double[values.length][4];
                else if (matrix.length!=values.length) throw new ExecutionError("Expected "+matrix.length+" values for base '"+segment.substring(0, 1)+"'. Got "+values.length);
                for (int i=0;i<values.length;i++) {
                     try {
                        matrix[i][index]=Double.parseDouble(values[i]);
                    } catch (Exception e) {throw new ExecutionError("Unable to parse expected numeric value for base '"+segment.substring(0, 1)+"': "+values[i]);}
                }
                gotAll[index]=true;
            } else { // user-defined properties
                int colonindex=segment.indexOf(':');
                if (colonindex<=0 || colonindex>=segment.length()-1)  throw new ExecutionError("Unable to parse motif parameter. Not a key-value pair: "+segment); // throws exception if string contains no colon or if the colon is the first or last character
                String key=segment.substring(0, colonindex);
                if (!Motif.isValidUserDefinedPropertyKey(key)) throw new ExecutionError("Not a valid name for a property: "+key);
                String valuestring=segment.substring(colonindex+1);
                Object value=getObjectForPropertyValueString(valuestring);
                motif.setUserDefinedPropertyValue(key,value); //                   
            }
        }
        if (hasMatrix) {
            if (!gotAll[0]) throw new ExecutionError("Missing matrix values for base A");
            if (!gotAll[1]) throw new ExecutionError("Missing matrix values for base C");
            if (!gotAll[2]) throw new ExecutionError("Missing matrix values for base G");
            if (!gotAll[3]) throw new ExecutionError("Missing matrix values for base T");
            motif.setMatrix(matrix);
        } else if (consensus!=null) {
            motif.setConsensusMotifAndUpdatePWM(consensus);
        } else throw new ExecutionError("Missing binding motif information");
        return motif;
    }

    /**
     * Returns a matrix parsed from a string with four fields delimited by semicolon.
     * The fields should be a comma-separated list of numeric values.
     * Each field can optionally start with a base indicator on the form "N:" (where N is either A,B,G or T. Note capital letters)
     * If no such indicator is present it is assumed that the fields are (in order): A,C,G and T.
     * @return 
     */
    public static double[][] parseMatrixFromString(String string) throws ParseError {
        double[][] matrix=null;
        boolean[] gotAll=new boolean[]{false,false,false,false};        
        String[] segments=string.trim().split("\\s*;\\s*");
        int counter=0;
        for (String segment:segments) {
            int index=0; // the base index
            if ((segment.startsWith("A:") || segment.startsWith("C:") || segment.startsWith("G:") || segment.startsWith("T:")) && segment.length()>2) {
                     if (segment.startsWith("A")) index=0;
                else if (segment.startsWith("C")) index=1;
                else if (segment.startsWith("G")) index=2;
                else if (segment.startsWith("T")) index=3;
                segment=segment.substring(2);
            } else index=counter;
            String[] values=segment.split("\\s*,\\s*");
            if (matrix==null) matrix=new double[values.length][4];
            else if (matrix.length!=values.length) throw new ParseError("Expected "+matrix.length+" values for base '"+segment.substring(0, 1)+"'. Got "+values.length);
            for (int i=0;i<values.length;i++) {
                 try {
                    matrix[i][index]=Double.parseDouble(values[i]);
                } catch (Exception e) {throw new ParseError("Unable to parse expected numeric value for base '"+segment.substring(0, 1)+"': "+values[i]);}
            }
            gotAll[index]=true;
            counter++;
        }
        if (!gotAll[0]) throw new ParseError("Missing matrix values for base A");
        if (!gotAll[1]) throw new ParseError("Missing matrix values for base C");
        if (!gotAll[2]) throw new ParseError("Missing matrix values for base G");
        if (!gotAll[3]) throw new ParseError("Missing matrix values for base T");
        return matrix;
    }
    
    /** Returns a list of names for all standard Motif properties such as "short name" and "Classification".
     *  (not including ID)
     *  Note that the names of properties are case-sensitive.
     *  @param includeDerived If TRUE, also derived properties such as IC-content, GC-content, size etc. which can not be set explicitly will be included in the list 
     */    
    public static String[] getAllStandardProperties(boolean includeDerived) {
        if (includeDerived) return new String[]{"Short name","Clean short name","Long name","Names","Consensus","Size","IC-content","GC-content","GO","Factors","Classification","Class name","Quality","Part","Alternatives","Interactions","Organisms","Expression","Description","Support","Matrix type"};     
        else return new String[]{"Short name","Long name","Consensus","GO","Factors","Classification","Quality","Part","Alternatives","Interactions","Organisms","Expression","Description"};     
    }

    public static boolean isStandardMotifProperty(String propertyname) {
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
            Class propclass=Motif.getPropertyClass(prop,null);
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
        ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
        for (Data motif:motifs) {
            Set<String> motifprops=((Motif)motif).getUserDefinedProperties();
            if (motifprops!=null) propertyNamesSet.addAll(motifprops);
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
            Class propclass=Motif.getPropertyClass(prop,engine);
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
    
    /** Returns a value for the motif property with the given name 
     *  @return an Object representing the value, this can be a 
     *  String, Boolean, Double, Integer, ArrayList etc. depending on the property
     *  @throws ExecutionError If the property is not recognized 
     */
    @Override
    public Object getPropertyValue(String propertyName,MotifLabEngine engine) throws ExecutionError {
             if (propertyName.equalsIgnoreCase("ID")) return getName();
        else if (propertyName.equalsIgnoreCase("short name") || propertyName.equalsIgnoreCase("shortname") || propertyName.equalsIgnoreCase("name")) return getShortName();
        else if (propertyName.equalsIgnoreCase("clean short name") || propertyName.equalsIgnoreCase("clean shortname") || propertyName.equalsIgnoreCase("clean name")) return getCleanShortName();
        else if (propertyName.equalsIgnoreCase("long name") || propertyName.equalsIgnoreCase("longname")) return getLongName();
        else if (propertyName.equalsIgnoreCase("names") || propertyName.equalsIgnoreCase("all names")) return getAllTFNames();
        else if (propertyName.equalsIgnoreCase("presentation name") || propertyName.equalsIgnoreCase("presentationname")) return getPresentationName();
        else if (propertyName.equalsIgnoreCase("consensus")) return getConsensusMotif();
        else if (propertyName.equalsIgnoreCase("IC-content") || propertyName.equalsIgnoreCase("IC") || propertyName.equalsIgnoreCase("Information content")) return new Double(getICcontent());
        else if (propertyName.equalsIgnoreCase("GC-content") || propertyName.equalsIgnoreCase("GC")) return new Double(getGCcontent());
        else if (propertyName.equalsIgnoreCase("GO") || propertyName.equalsIgnoreCase("gene ontology")) return getGOterms();       
        else if (propertyName.equalsIgnoreCase("size") || propertyName.equalsIgnoreCase("length") || propertyName.equalsIgnoreCase("width")) return new Integer(getLength());
        else if (propertyName.equalsIgnoreCase("support")) return new Integer(getSupport());
        else if (propertyName.equalsIgnoreCase("factors")) return MotifLabEngine.splitOnCommaSimple(getBindingFactors());
        else if (propertyName.equalsIgnoreCase("classification") || propertyName.equalsIgnoreCase("class")) return getClassification();
        else if (propertyName.equalsIgnoreCase("class name") || propertyName.equalsIgnoreCase("classname")) return MotifClassification.getNameForClass(getClassification());
        else if (propertyName.equalsIgnoreCase("quality")) return new Integer(getQuality());
        else if (propertyName.equalsIgnoreCase("part")) return getPart();
        else if (propertyName.equalsIgnoreCase("alternatives")) return getKnownDuplicatesNames();
        else if (propertyName.equalsIgnoreCase("interactions")) return getInteractionPartnerNames();
        else if (propertyName.equalsIgnoreCase("organisms") || propertyName.equalsIgnoreCase("organism")) return MotifLabEngine.splitOnCommaSimple(getOrganisms());
        else if (propertyName.equalsIgnoreCase("expression")) return getTissueExpressionAsStringArray();
        else if (propertyName.equalsIgnoreCase("matrix")) return getMatrixAsString();
        else if (propertyName.equalsIgnoreCase("matrix type")) return getMatrixTypeAsString();        
        else if (propertyName.equalsIgnoreCase("description")) return getDescription();        
        else {
            if (Motif.getPropertyClass(propertyName,engine)==null) throw new ExecutionError("Unknown motif property: "+propertyName);
            if (properties!=null && properties.containsKey(propertyName)) return properties.get(propertyName);
            else return null; // user-defined property exists, but this motif does not have a value for it
        }                     
    }
    
    /**
     * Assigns the given property the given value.
     * @param propertyName
     * @param value
     * @return FALSE if the property represents a derived value that can not be explicitly set, else TRUE
     * @throws ExecutionError if the given value is not of the appropriate type
     */
    @Override
    public boolean setPropertyValue(String propertyName, Object value) throws ExecutionError {
        try {
                 if (propertyName.equalsIgnoreCase("ID")) return false;
            else if (propertyName.equalsIgnoreCase("short name") || propertyName.equalsIgnoreCase("shortname") || propertyName.equalsIgnoreCase("name")) setShortName((String)MotifLabEngine.convertToType(value,String.class));
            else if (propertyName.equalsIgnoreCase("clean short name") || propertyName.equalsIgnoreCase("clean shortname") || propertyName.equalsIgnoreCase("clean name")) return false;
            else if (propertyName.equalsIgnoreCase("long name") || propertyName.equalsIgnoreCase("longname")) setLongName((String)MotifLabEngine.convertToType(value,String.class));
            else if (propertyName.equalsIgnoreCase("names") || propertyName.equalsIgnoreCase("all names")) return false;
            else if (propertyName.equalsIgnoreCase("presentation name") || propertyName.equalsIgnoreCase("presentationname")) return false;
            else if (propertyName.equalsIgnoreCase("consensus")) setConsensusMotifAndUpdatePWM((String)MotifLabEngine.convertToType(value,String.class));
            else if (propertyName.equalsIgnoreCase("IC-content") || propertyName.equalsIgnoreCase("IC") || propertyName.equalsIgnoreCase("Information content")) return false;
            else if (propertyName.equalsIgnoreCase("GC-content") || propertyName.equalsIgnoreCase("GC")) return false;
            else if (propertyName.equalsIgnoreCase("description")) setDescription((String)MotifLabEngine.convertToType(value,String.class));             
            else if (propertyName.equalsIgnoreCase("size") || propertyName.equalsIgnoreCase("length") || propertyName.equalsIgnoreCase("width")) return false;
            else if (propertyName.equalsIgnoreCase("support")) return false;
            else if (propertyName.equalsIgnoreCase("matrix type")) return false;            
            else if (propertyName.equalsIgnoreCase("classification") || propertyName.equalsIgnoreCase("class")) setClassification((String)MotifLabEngine.convertToType(value,String.class));
            else if (propertyName.equalsIgnoreCase("classname") || propertyName.equalsIgnoreCase("class name")) return false;
            else if (propertyName.equalsIgnoreCase("quality")) setQuality((Integer)MotifLabEngine.convertToType(value,Integer.class));
            else if (propertyName.equalsIgnoreCase("part")) setPart((String)MotifLabEngine.convertToType(value,String.class));
            else if (propertyName.equalsIgnoreCase("alternatives")) {
                if (value instanceof String) setKnownDuplicatesNames(((String)value).split("\\s*,\\s*"));
                else if (value instanceof String[]) setKnownDuplicatesNames((String[])value);
                else if (value instanceof ArrayList) knownduplicates=(ArrayList<String>)value;
                else throw new ClassCastException();
            }
            else if (propertyName.equalsIgnoreCase("interactions")) {
                if (value instanceof String) setInteractionPartnerNames(((String)value).split("\\s*,\\s*"));
                else if (value instanceof String[]) setInteractionPartnerNames((String[])value);
                else if (value instanceof ArrayList) interactionpartners=(ArrayList<String>)value;
                else throw new ClassCastException();                
            }
            else if (propertyName.equalsIgnoreCase("expression")) {
                if (value instanceof String) setTissueExpression(((String)value).split("\\s*,\\s*"));
                else if (value instanceof String[]) setTissueExpression((String[])value);
                else if (value instanceof ArrayList) setTissueExpression((ArrayList<String>)value);
                else throw new ClassCastException();                 
            }
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
            else if (propertyName.equalsIgnoreCase("organisms") || propertyName.equalsIgnoreCase("organism")) {
                if (value instanceof String) setOrganisms((String)value);
                else if (value instanceof List) setOrganisms((List)value);
                else throw new ClassCastException();                 
            }
            else if (propertyName.equalsIgnoreCase("factors")) {
                if (value instanceof String) setBindingFactors((String)value);
                else if (value instanceof List) setBindingFactors((List)value);
                else throw new ClassCastException(); 
            }                   
            else if (propertyName.equalsIgnoreCase("matrix")) {
                if (value instanceof double[][]) setMatrix((double[][])value);
                else if (value instanceof String) {
                    double[][] newmatrix=parseMatrixFromString((String)value);
                    setMatrix(newmatrix);
                } else throw new ClassCastException(); 
            }                       
            else setUserDefinedPropertyValue(propertyName, value);          
        } catch(Exception e) {     
            if (e instanceof ParseError) throw new ExecutionError(e.getMessage());
            throw new ExecutionError("Unable to set property '"+propertyName+"' to value '"+((value!=null)?value.toString():"")+"'");
        }
        return true;
    }    
     
    /**
     * Returns the type-class for the given property (standard or user-defined) 
     * or NULL if the property is not recognized
     * @param propertyName
     * @param engine
     * @return 
     */
    public static Class getPropertyClass(String propertyName,MotifLabEngine engine) {
             if (propertyName.equalsIgnoreCase("ID")) return String.class;
        else if (propertyName.equalsIgnoreCase("short name") || propertyName.equalsIgnoreCase("shortname") || propertyName.equalsIgnoreCase("name")) return String.class;
        else if (propertyName.equalsIgnoreCase("clean short name") || propertyName.equalsIgnoreCase("clean shortname") || propertyName.equalsIgnoreCase("clean name")) return String.class;
        else if (propertyName.equalsIgnoreCase("long name") || propertyName.equalsIgnoreCase("longname")) return String.class;
        else if (propertyName.equalsIgnoreCase("names") || propertyName.equalsIgnoreCase("all names")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("presentation name") || propertyName.equalsIgnoreCase("presentationname")) return String.class;
        else if (propertyName.equalsIgnoreCase("consensus")) return String.class;
        else if (propertyName.equalsIgnoreCase("IC-content") || propertyName.equalsIgnoreCase("IC") || propertyName.equalsIgnoreCase("Information content")) return Double.class;
        else if (propertyName.equalsIgnoreCase("GC-content") || propertyName.equalsIgnoreCase("GC")) return Double.class;
        else if (propertyName.equalsIgnoreCase("GO") || propertyName.equalsIgnoreCase("gene ontology")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("size") || propertyName.equalsIgnoreCase("length") || propertyName.equalsIgnoreCase("width")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("support")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("factors")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("classification") || propertyName.equalsIgnoreCase("class")) return String.class;
        else if (propertyName.equalsIgnoreCase("classname") || propertyName.equalsIgnoreCase("class name")) return String.class;
        else if (propertyName.equalsIgnoreCase("quality")) return Integer.class;
        else if (propertyName.equalsIgnoreCase("part")) return String.class;
        else if (propertyName.equalsIgnoreCase("alternatives")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("interactions")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("organisms") || propertyName.equalsIgnoreCase("organism")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("expression")) return ArrayList.class;
        else if (propertyName.equalsIgnoreCase("matrix")) return String.class;
        else if (propertyName.equalsIgnoreCase("matrix type")) return String.class;
        else if (propertyName.equalsIgnoreCase("description")) return String.class;        
        else return Motif.getClassForUserDefinedProperty(propertyName, engine);                  
    }    
    
    /**
     * Returns TRUE if there is a numeric property with the given name
     * and FALSE if there is no such property or if it is not numeric
     * @param propertyName
     * @param engine
     * @return 
     */
    public static boolean isNumericProperty(String propertyName, MotifLabEngine engine) {
        Class type=Motif.getPropertyClass(propertyName, engine);
        if (type==null) return false;
        return (type==Integer.class || type==Double.class);
    }

    
    /** Returns a property CLASS for the user-defined motif-property with the given name
     *  or NULL if no property with the given name has been defined for any Motifs.
     *  This method works by dynamically querying all registered motifs to see
     *  what user-defined properties they have and what classes they are.
     *  Note that individual Motifs might have different Object (types) stored for the
     *  same property. e.g. One Motif might have a value of "7" for the property "ID" 
     *  which is stored as a Double while a different Motif might have the value "H8" 
     *  for the same "ID" property which is stored as a a String. 
     *  This method will return the 'least common class denominator'. 
     *  Which is to say that if all the values (across all motifs) for the same property
     *  are the same, this class will be returned else String.class will be returned
     *  @param propertyname (case-sensitive)
     */
    public static Class getClassForUserDefinedProperty(String propertyName, MotifLabEngine engine) {
        if (userdefinedpropertyClasses!=null && userdefinedpropertyClasses.containsKey(propertyName)) return userdefinedpropertyClasses.get(propertyName); // cached entries
        Class type=getClassForUserDefinedPropertyDynamically(propertyName, engine);
        if (type!=null) Motif.setUserDefinedPropertyClass(propertyName,type,true); // cache the class in this lookup-table
        return type;       
    }    
    
    private static Class getClassForUserDefinedPropertyDynamically(String propertyName, MotifLabEngine engine) {
        if (engine==null) return null;
        // try to determine the type dynamically by querying all Motifs
        Class firstclass=null;
        ArrayList<Data> motifs=engine.getAllDataItemsOfType(Motif.class);
        for (Data motif:motifs) {
            Object value=((Motif)motif).getUserDefinedPropertyValue(propertyName);
            if (value==null) continue;
            else if (firstclass==null) firstclass=value.getClass();
            else if (firstclass.equals(Integer.class) && value.getClass().equals(Double.class)) firstclass=Double.class; // return Double if values are a mix of Integers and Doubles
            else if (firstclass.equals(Double.class) && value.getClass().equals(Integer.class)) continue; // return Double if values are a mix of Integers and Doubles
            else if (firstclass.equals(ArrayList.class) || value.getClass().equals(ArrayList.class)) return ArrayList.class; // return ArrayList if one of the values is an ArrayList
            else if (!value.getClass().equals(firstclass)) return String.class; // Not compatible
        }
        return firstclass;         
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
             if (!(value instanceof Double || value instanceof Integer || value instanceof Boolean || value instanceof List || value instanceof String)) value=value.toString(); // convert to String just in case
             properties.put(propertyName, value);
             Motif.setUserDefinedPropertyClass(propertyName, value.getClass(), false);
         }
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
    public static void updateUserdefinedPropertiesLookupTable(Motif dataitem, MotifLabEngine engine) {
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
    
    /** Given a name for motif (not an ID but for instance a Transfac name like V$MMM_Q5 
     *  This method will strip any "X$" like prefixes and any Transfac like suffixes
     *  (i.e. suffixes like "_C", "_Q4" or "_X" (where X is any number)
     *  @param replaceNonAlphanumeric If true, any non-alphanumeric characters will be replaced by underscores
     */
    public static String cleanUpMotifShortName(String name, boolean replaceNonAlphanumeric) {
       name=name.replaceAll("^\\w\\$", ""); // remove V$ and similar prefixes
       name=name.replaceAll("_\\d\\d$", "");  // remove TRANSFAC numbering
       name=name.replaceAll("_Q\\d$", "");  // remove TRANSFAC quality
       name=name.replaceAll("_C$", "");  // remove TRANSFAC consensus
       name=name.replaceAll("_B$", "");  // not quite sure what this is but I will remove it nonetheless       
       if (replaceNonAlphanumeric) name=name.replaceAll("\\W+", "_"); // replace non-valid characters with _
       name=name.replaceAll("^_+", "");  // remove leading underscores    
       name=name.replaceAll("_+$", "");  // remove trailing underscores    
       return name;
    }
    
    /** Given an ID for a motif, this method will replace any non-word characters with underscore
     */
    public static String cleanUpMotifID(String name) {
       name=name.replaceAll("\\W", "_"); // 
       return name;
    }    
        
    /**
     * Creates a new PWM matrix from a set of binding sequences of equal length
     * If only one sequence is provided it is allowed to contain IUPAC degenerate symbols
     * @param sequences
     * @return
     * @throws SystemError 
     */
    public static double[][] getPWMfromSites(String[] sequences) throws SystemError {
        // check if the multiple sequences have been entered
        double[][] matrix=null;
        if (sequences==null || sequences.length==0) return new double[0][4];
        if (sequences.length>1) { // multiple aligned sequences. 
            int consensuslength=sequences[0].length();
            matrix=new double[consensuslength][4];
            // initialize the matrix with zeros! 
            for (int i=0;i<consensuslength;i++) {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
            for (int i=0;i<sequences.length;i++) {
                 if (sequences[i].length()!=consensuslength) throw new SystemError("Aligned sequences are not of equal length");                  
                 for (int j=0;j<sequences[i].length();j++) {
                    char base=Character.toUpperCase(sequences[i].charAt(j));
                         if (base=='A') {matrix[j][0]+=1f;}
                    else if (base=='C') {matrix[j][1]+=1f;}
                    else if (base=='G') {matrix[j][2]+=1f;}
                    else if (base=='T' || base=='U') {matrix[j][3]+=1f;}
                    else throw new SystemError("Unrecognized DNA base '"+sequences[i].charAt(j)+"'");
                 }                 
            }
        } else { // single consensus sequence
            String newconsensus=sequences[0];
            matrix=new double[newconsensus.length()][4];
            for (int i=0;i<newconsensus.length();i++) {
                char base=Character.toUpperCase(newconsensus.charAt(i));
                     if (base=='A') {matrix[i][0]=12.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
                else if (base=='C') {matrix[i][0]=0.0;matrix[i][1]=12.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
                else if (base=='G') {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=12.0;matrix[i][3]=0.0;}
                else if (base=='T' || base=='U') {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=12.0;}
                else if (base=='R') {matrix[i][0]=6.0;matrix[i][1]=0.0;matrix[i][2]=6.0;matrix[i][3]=0.0;}
                else if (base=='Y') {matrix[i][0]=0.0;matrix[i][1]=6.0;matrix[i][2]=0.0;matrix[i][3]=6.0;}
                else if (base=='M') {matrix[i][0]=6.0;matrix[i][1]=6.0;matrix[i][2]=0.0;matrix[i][3]=0.0;}
                else if (base=='K') {matrix[i][0]=0.0;matrix[i][1]=0.0;matrix[i][2]=6.0;matrix[i][3]=6.0;}
                else if (base=='W') {matrix[i][0]=6.0;matrix[i][1]=0.0;matrix[i][2]=0.0;matrix[i][3]=6.0;}
                else if (base=='S') {matrix[i][0]=0.0;matrix[i][1]=6.0;matrix[i][2]=6.0;matrix[i][3]=0.0;}
                else if (base=='B') {matrix[i][0]=0.0;matrix[i][1]=4.0;matrix[i][2]=4.0;matrix[i][3]=4.0;}
                else if (base=='D') {matrix[i][0]=4.0;matrix[i][1]=0.0;matrix[i][2]=4.0;matrix[i][3]=4.0;}
                else if (base=='H') {matrix[i][0]=4.0;matrix[i][1]=4.0;matrix[i][2]=0.0;matrix[i][3]=4.0;}
                else if (base=='V') {matrix[i][0]=4.0;matrix[i][1]=4.0;matrix[i][2]=4.0;matrix[i][3]=0.0;}
                else if (base=='N') {matrix[i][0]=3.0;matrix[i][1]=3.0;matrix[i][2]=3.0;matrix[i][3]=3.0;}
                else throw new SystemError("Unrecognized IUPAC DNA base '"+newconsensus.charAt(i)+"'");
            }
        }
        matrix=Motif.normalizeMatrix(matrix);
        return matrix;
    }    
    
    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=2; // this is an internal version number for serialization of objects of this type. Version 2 replaced the "tissues<Short>" list (plus two LUTs) with the "tissueExpression<String>" list
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();             
             // at this point I would have liked to convert the old "tisse" list to the new "tissueExpression" list,
             // but, as it turns out, serializing and restoring motifs that have tissue expression properties did not work at all in the previous version either :-S
             // (Why was this not discovered in testing?)
         } if (currentinternalversion==2) {
             in.defaultReadObject();
         } else if (currentinternalversion>2) throw new ClassNotFoundException("Newer version");
    }
    
}
