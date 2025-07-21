
package org.motiflab.engine.util;

import java.util.HashMap;

/**
 *
 * @author kjetikl
 */
public class CodonUsage {
    private static final HashMap<String,String> codons=new HashMap<String,String>();
    private static final HashMap<String,String> abbrmap=new HashMap<String,String>(); 
    
    static {
        codons.put("ATT","I");
        codons.put("ATC","I");
        codons.put("ATA","I");
        codons.put("CTT","L");
        codons.put("CTC","L");
        codons.put("CTA","L");
        codons.put("CTG","L");
        codons.put("TTA","L");
        codons.put("TTG","L");
        codons.put("GTT","V");
        codons.put("GTC","V");
        codons.put("GTA","V");
        codons.put("GTG","V");
        codons.put("TTT","F");
        codons.put("TTC","F");
        codons.put("ATG","M");
        codons.put("TGT","C");
        codons.put("TGC","C");
        codons.put("GCT","A");
        codons.put("GCC","A");
        codons.put("GCA","A");
        codons.put("GCG","A");
        codons.put("GGT","G");
        codons.put("GGC","G");
        codons.put("GGA","G");
        codons.put("GGG","G");
        codons.put("CCT","P");
        codons.put("CCC","P");
        codons.put("CCA","P");
        codons.put("CCG","P");
        codons.put("ACT","T");
        codons.put("ACC","T");
        codons.put("ACA","T");
        codons.put("ACG","T");
        codons.put("TCT","S");
        codons.put("TCC","S");
        codons.put("TCA","S");
        codons.put("TCG","S");
        codons.put("AGT","S");
        codons.put("AGC","S");
        codons.put("TAT","Y");
        codons.put("TAC","Y");
        codons.put("TGG","W");
        codons.put("CAA","Q");
        codons.put("CAG","Q");
        codons.put("AAT","N");
        codons.put("AAC","N");
        codons.put("CAT","H");
        codons.put("CAC","H");
        codons.put("GAA","E");
        codons.put("GAG","E");
        codons.put("GAT","D");
        codons.put("GAC","D");
        codons.put("AAA","K");
        codons.put("AAG","K");
        codons.put("CGT","R");
        codons.put("CGC","R");
        codons.put("CGA","R");
        codons.put("CGG","R");
        codons.put("AGA","R");
        codons.put("AGG","R");
        codons.put("TAA","Stop");
        codons.put("TAG","Stop");
        codons.put("TGA","Stop");

        abbrmap.put("A","Alanine");
        abbrmap.put("C","Cysteine");
        abbrmap.put("D","Aspartic acid");
        abbrmap.put("E","Glutamic acid");
        abbrmap.put("F","Phenylalanine");
        abbrmap.put("G","Glycine");
        abbrmap.put("H","Histidine");
        abbrmap.put("I","Isoleucine");
        abbrmap.put("K","Lysine");
        abbrmap.put("L","Leucine");
        abbrmap.put("M","Methionine");
        abbrmap.put("N","Asparagine");
        abbrmap.put("P","Proline");
        abbrmap.put("Q","Glutamine");
        abbrmap.put("R","Arginine");
        abbrmap.put("S","Serine");
        abbrmap.put("T","Threonine");
        abbrmap.put("V","Valine");
        abbrmap.put("W","Tryptophan");
        abbrmap.put("Y","Tyrosine");
    }
    
    
    /** Given a 3bp DNA triplet sequence, this method will return
     *  the abbreviated name of the corresponding amino acid according
     *  to the standard genetic code. The stop codons TAA,TAG,TGA will 
     *  return the keyword "Stop".
     * 
     */
    public static String getAminoAcidForCodon(String codon) {
        if (codon==null || codon.length()!=3) return null;
        String aminoAcid=codons.get(codon.toUpperCase());
        return aminoAcid;
    }
    
    /**
     * Given a one-letter abbreviation for an amino acid,
     * this method will return its full name
     * @param code
     * @return 
     */
    public static String getAminoAcidName(String code) {
        return abbrmap.get(code);
    }    
}
