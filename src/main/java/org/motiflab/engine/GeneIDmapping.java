/*
 
 
 */

package org.motiflab.engine;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author kjetikl
 */
    /** Just a "struct" to return information in a more formalized way than an Object array */
public class GeneIDmapping implements Serializable {
        private static final long serialVersionUID = -2489796539046550293L;
    
        public String geneID;
        public String geneName;
        public String chromosome;
        public int TSS;
        public int TES;
        public int strand;
        public ArrayList<String> GOterms;
        
        public GeneIDmapping(String geneID, String geneName, String chromosome, int TSS, int TES, int strand) {
            this.geneID=geneID;
            this.geneName=(geneName==null || geneName.isEmpty())?geneID:geneName;
            this.chromosome=chromosome;
            this.TSS=TSS; // actual TSS (relative to direction). Same as "start" if genes with direct orientation or "end" for genes with reverse orientation
            this.TES=TES;
            this.strand=strand;
            this.GOterms=null;
        }
        
        @Override
        public String toString() {
            return geneID+"."+geneName+"   chr"+chromosome+":"+((TSS<TES)?TSS:TES)+"-"+((TSS<TES)?TES:TSS)+" "+((strand<0)?"Reverse":"Direct");
        }
        
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeneIDmapping other = (GeneIDmapping) obj;
        if ((this.geneID == null) ? (other.geneID != null) : !this.geneID.equals(other.geneID)) {
            return false;
        }
        if ((this.geneName == null) ? (other.geneName != null) : !this.geneName.equals(other.geneName)) {
            return false;
        }
        if ((this.chromosome == null) ? (other.chromosome != null) : !this.chromosome.equals(other.chromosome)) {
            return false;
        }
        if (this.TSS != other.TSS) {
            return false;
        }
        if (this.TES != other.TES) {
            return false;
        }
        if (this.strand != other.strand) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this.geneID != null ? this.geneID.hashCode() : 0);
        hash = 89 * hash + (this.geneName != null ? this.geneName.hashCode() : 0);
        hash = 89 * hash + (this.chromosome != null ? this.chromosome.hashCode() : 0);
        hash = 89 * hash + this.TSS;
        hash = 89 * hash + this.TES;
        hash = 89 * hash + this.strand;
        return hash;
    }
        
    /** Adds the given GO term to this mapping (the term can be a comma-separated list) */
    public void addGOterm(String term) {
        if (GOterms==null) GOterms=new ArrayList<>();
        if (term.contains(",")) {
           String[] parts=term.trim().split("\\s*,\\s*");
           for (String part:parts) {
               addSingleGOterm(part);
           }
        } else {
            addSingleGOterm(term);
        }      
    } 
    
    private void addSingleGOterm(String term) {
        if (term==null || term.isEmpty() || !term.startsWith("GO:")) return;
        if (!GOterms.contains(term)) GOterms.add(term);
    }
    
}
