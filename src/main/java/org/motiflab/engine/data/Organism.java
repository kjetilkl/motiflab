package org.motiflab.engine.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.protocol.ParseError;

/**
 * This class basically just provides a predefined lookup-table and associated methods
 * to convert between taxonomy identifier numbers and names (common and latin) for different
 * organisms. If references to organisms are stored as integer (taxonomy IDs) this class can
 * be used to lookup corresponding names
 * 
 * @author Kjetil Klepper
 */
public class Organism {
    public final static int HUMAN=9606;
    public final static int MOUSE=10090;
    public final static int RAT=10116;
    public final static int FRUITFLY=7227;
    public final static int ZEBRAFISH=7955;
    public final static int ARABIDOPSIS=3702;
    public final static int FROG=8364; // Xenopus laevis tropicalis
    public final static int MAIZE=4577;
    public final static int COW=9913;
    public final static int CHICKEN=9031;
    public final static int YEAST=4932;
    public final static int RABBIT=9986;
    public final static int UNKNOWN=0;    
    
    /*
     * the predefined Object array contains info on all organisms currently "supported"
     * Each row in the array contains the following information about a single organism:
     * [0] => NCBI taxonomy ID for that organism
     * [1] => A common english name
     * [2] => The latin name
     * [3] => A 2D-String-array listing supported genome builds (or null if no builds are supported) 
     *        Each entry (first dimension) represents one genome build with the entries within
     *        this dimension representing alternative names for the same build (default should be first)
     * [4] => A String a string specifying the 'clade': mammal, vertebrate, insect or other) 
     */
    private static Object[][] predefined={
        {new Integer(HUMAN),"Human","Homo sapiens",new String[][]{{"hg18","NCBI36","NCBI_36"},{"hg19","GRCh37","GRCh_37"}},"mammal"},
        {new Integer(MOUSE),"Mouse","Mus musculus",new String[][]{{"mm9","NCBI37","NCBI_37"},{"mm8","NCBI36","NCBI_36"},{"mm7","NCBI35","NCBI_35"}},"mammal"},
        {new Integer(RAT),"Rat","Rattus norvegicus",new String[][]{{"rn4","RGSC3.4","RGSC_3.4","Baylor 3.4"}},"mammal"},
        {new Integer(FRUITFLY),"Fruit fly","Drosophila melanogaster",new String[][]{{"dm3","BDGP R5","BDGP_R5"}},"insect"},
        {new Integer(ZEBRAFISH),"Zebrafish","Danio rerio",new String[][]{{"danRer6","Zv8","Zv_8"},{"danRer5","Zv7","Zv_7"}},"vertebrate"},
     //   {new Integer(ARABIDOPSIS),"Thale cress","Arabidopsis thaliana",null,"other"},       
        {new Integer(FROG),"Frog","Xenopus tropicalis",new String[][]{{"xenTro2","JGI 4.1","JGI_4.1"}},"vertebrate"},       
     //   {new Integer(MAIZE),"Maize","Zea mays",null,"other"},       
        {new Integer(COW),"Cow","Bos taurus",new String[][]{{"bosTau4","Baylor 4.0","Baylor_4.0"}},"mammal"},      
        {new Integer(CHICKEN),"Chicken","Gallus gallus",new String[][]{{"galGal3","WUGSC2.1","WUGSC_2.1"}},"vertebrate"},      
        {new Integer(YEAST),"Yeast","Saccharomyces cerevisiae",new String[][]{{"sacCer2","SGD","SGD/S288C"}},"other"},
        {new Integer(RABBIT),"Rabbit","Oryctolagus cuniculus",new String[][]{{"oryCun2"}},"vertebrate"},
    };
       
   
    /** This method can be called to initialize the Organism table with other 
     * organisms than the default settings
     */
    public static void initializeFromFile(File file) throws IOException, SystemError {
        ArrayList<Object[]> entries=new ArrayList<Object[]>();
        BufferedReader inputStream=null;
        try {
            inputStream=new BufferedReader(new FileReader(file));
            String line;
            while((line=inputStream.readLine())!=null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue; // comments and empty lines
                String[] fields=line.split("\\t");
                if (fields.length<5) throw new SystemError("Expected 5 fields per line in Organisms definition file, but got "+fields.length+":\n"+line);
                int taxonomyID=0;
                if (fields[0].trim().isEmpty()) throw new SystemError("Missing taxonomy ID for organism in Organisms definition file");
                try {
                  taxonomyID=Integer.parseInt(fields[0]);
                } catch (NumberFormatException nfe) {throw new SystemError("Expected integer taxonomy ID in first column in Organisms definition file, but got '"+fields[0]+"'");}
                String commonname=fields[1].trim();
                String latinname=fields[2].trim();
                String cladename=fields[4].trim();
                if (commonname.isEmpty()) throw new SystemError("Missing common name for organism with taxonomy ID: "+taxonomyID);
                if (latinname.isEmpty()) throw new SystemError("Missing latin name for organism with taxonomy ID: "+taxonomyID);
                if (cladename.isEmpty()) throw new SystemError("Missing clade for organism with taxonomy ID: "+taxonomyID);
                // if (!cladename.equals("mammal") && !cladename.equals("insect") && !cladename.equals("vertebrate") && !cladename.equals("other")) throw new SystemError("Unknown clade '"+cladename+"' for organism with taxonomy ID "+taxonomyID);
                String[] builds=fields[3].trim().split("\\s*,\\s*");
                String[][] buildsStructure=null;
                if (!fields[3].trim().isEmpty()) {
                    buildsStructure=new String[builds.length][];
                    for (int i=0;i<builds.length;i++) {
                       String[] alternatives=builds[i].trim().split("\\s*\\|\\s*");  
                       buildsStructure[i]=alternatives;
                    }
                }
                Object[] organism=new Object[]{new Integer(taxonomyID),commonname,latinname,buildsStructure,cladename};
                entries.add(organism);
            }
            inputStream.close();
        } catch (IOException e) {
            throw e;
        } catch (Exception ee) {
            if (ee instanceof SystemError) throw ee;
            else throw new SystemError(ee.getClass()+":"+ee.getMessage());
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {}
        }
        predefined=new Object[entries.size()][5];
        for (int i=0;i<entries.size();i++) {
            predefined[i]=entries.get(i);
        }
    }    
    
    /** 
     * This method will replace the current configuration with a new one.
     * The content of the new configuration is first checked for errors.
     * If errors are found a ParseError will be thrown for the first error
     * encountered, else the existing configuration will be replaced and 
     * saved to the provided file. Note that if a file is not specified,
     * only the error checking is performed but the configuration will 
     * not be replaced
     */
    public static void replaceandSave(ArrayList<String[]> newconfiguration, File file) throws ParseError, IOException {
        ArrayList<Object[]> entries=new ArrayList<Object[]>();
        for (int i=0;i<newconfiguration.size();i++) {
        String[] fields=newconfiguration.get(i);
            if (fields.length<5) throw new ParseError("Expected 5 columns for Organism configuration, but got "+fields.length);
            int taxonomyID=0;
            if (fields[0].trim().isEmpty()) throw new ParseError("Missing taxonomy ID for entry number "+(i+1));
            try {
              taxonomyID=Integer.parseInt(fields[0]);
            } catch (NumberFormatException nfe) {throw new ParseError("Expected integer taxonomy ID in first column, found '"+fields[0]+"'");}
            String commonname=fields[1].trim();
            String latinname=fields[2].trim();
            String cladename=fields[4].trim();
            if (commonname.isEmpty()) throw new ParseError("Missing common name for organism with taxonomy ID: "+taxonomyID);
            if (latinname.isEmpty()) throw new ParseError("Missing latin name for organism with taxonomy ID: "+taxonomyID);
            if (cladename.isEmpty()) throw new ParseError("Missing clade for organism with taxonomy ID: "+taxonomyID);
            // if (!cladename.equals("mammal") && !cladename.equals("insect") && !cladename.equals("vertebrate") && !cladename.equals("other")) throw new ParseError("Unknown clade '"+cladename+"' for organism with taxonomy ID "+taxonomyID);
            String[] builds=fields[3].trim().split("\\s*,\\s*");
            String[][] buildsStructure=null;
            if (!fields[3].trim().isEmpty()) {
                buildsStructure=new String[builds.length][];
                for (int j=0;j<builds.length;j++) {
                   String[] alternatives=builds[j].trim().split("\\s*\\|\\s*");  
                   buildsStructure[j]=alternatives;
                }
            }
            Object[] organism=new Object[]{new Integer(taxonomyID),commonname,latinname,buildsStructure,cladename};
            entries.add(organism);
        }

        if (file!=null) {
            predefined=new Object[entries.size()][5];
            for (int i=0;i<entries.size();i++) {
                predefined[i]=entries.get(i);
            }           
            writeToFile(file);
        }
    }

    
    /**
     * Outputs the current configuration to the given file
     * @param file
     * @throws IOException
     */
    public static void writeToFile(File file) throws IOException {
        BufferedWriter outputStream=null;
        try {
            outputStream=new BufferedWriter(new FileWriter(file));
            outputStream.write("# This file contains information on all organisms currently supported by the system.");
            outputStream.newLine();
            outputStream.write("# Each row contains the following information about a single organism in tab-separated fields:");
            outputStream.newLine();
            outputStream.write("# 1) NCBI taxonomy ID for the organism");
            outputStream.newLine();
            outputStream.write("# 2) A common english name");
            outputStream.newLine();
            outputStream.write("# 3) The latin name");
            outputStream.newLine();
            outputStream.write("# 4) A comma-separated list of supported genome builds for this organism. The first will be the default build. The list can be empty if no builds are supported). Each genome build can list multiple alternative names separated by vertical bars '|'. The default name should be listed first in each group");
            outputStream.newLine();
            outputStream.write("# 5) A string specifying the clade: 'mammal', 'vertebrate', 'insect' or 'other' (corresponding to UCSC usage)");
            outputStream.newLine();
            outputStream.newLine();
            for (Object[] entry:predefined) {
                outputStream.write(""+entry[0]);
                outputStream.write("\t");
                outputStream.write(""+entry[1]);
                outputStream.write("\t");
                outputStream.write(""+entry[2]);
                outputStream.write("\t");
                String[][] builds=(String[][])entry[3];
                if (builds!=null && builds.length>0) {
                    for (int i=0;i<builds.length;i++) {
                        for (int j=0;j<builds[i].length;j++) {
                            outputStream.write(builds[i][j]);
                            if (j<builds[i].length-1) outputStream.write("|");
                        }                    
                        if (i<builds.length-1) outputStream.write(",");
                    }
                }
                outputStream.write("\t");
                outputStream.write(""+entry[4]);
                outputStream.newLine();
            }
            outputStream.close();
        } catch (IOException e) {
            throw e;
        } finally {
            try {if (outputStream!=null) outputStream.close();} catch (IOException ioe) {}
        }
    }

    /**
     * Returns the NCBI taxonomy ID for the organism with the specified name (common name or latin name)
     * (if it is known to the system, or else 0)
     */
    public static int getTaxonomyID(String name) {
        if (name==null) return 0;
        for (Object[] values:predefined) { // first try exact matches
            if (((String)values[1]).equalsIgnoreCase(name)) return ((Integer)values[0]).intValue();
            if (((String)values[2]).equalsIgnoreCase(name)) return ((Integer)values[0]).intValue();
        }
        // ... Previous versions also allowed for partial matches to the name, but that turned out to be to risky (e.g. "pig" would match "Guinea pig")             
        return 0;
    }
        
    /**
     * Returns the common name for the organism associated with the given taxonomy ID
     * If the taxonomy ID is not known to the system the value "NCBI:[ID]" will be returned instead
     * unless ID==0, in which case the value "UNKNOWN" will be returned
     * @param id
     * @return
     */
    public static String getCommonName(int id) {
        if (id==0) return "UNKNOWN";
        for (Object[] values:predefined) {
            if (((Integer)values[0]).intValue()==id) return (String)values[1];
        }
        return "NCBI:"+id; 
    }
    
    /**
     * Returns the latin name for the organism associated with the given taxonomy ID
     * (if it is known to the system, or null)
     * @param id
     * @return
     */
    public static String getLatinName(int id) {
        for (Object[] values:predefined) {
            if (((Integer)values[0]).intValue()==id) return (String)values[2];
        }
        return "NCBI:"+id;  
    }
    
    /**
     * Returns a string containing both the common and latin name for the organism 
     * associated with the given taxonomy ID (if it is known to the system, or null)
     * The format is "common (latin)", e.g. "Human (Homo sapiens)"
     * 
     * @param id
     * @return
     */
    public static String getCombinedName(int id) {
        for (Object[] values:predefined) {
            if (((Integer)values[0]).intValue()==id) return (String)values[1]+" ("+(String)values[2]+")";
        }
        return "NCBI:"+id; 
    }
    
    /**
     * Returns a String array containing the common names of all organisms known to the engine
     * @return
     */
    public static String[] getSupportedOrganisms() {
        String[] results=new String[predefined.length];
        for (int i=0;i<predefined.length;i++) {
            results[i]=(String)predefined[i][1];   
        }
        return results;
    }
    
    /**
     * Returns an Integer array containing the organism IDs of all organisms known to the engine
     * @return
     */
    public static Integer[] getSupportedOrganismIDs() {
        Integer[] results=new Integer[predefined.length];
        for (int i=0;i<predefined.length;i++) {
            results[i]=(Integer)predefined[i][0];   
        }
        return results;
    }
    
    public static boolean isSupportedOrganismID(int id) {
        for (int i=0;i<predefined.length;i++) {
            if (id==(int)(predefined[i][0])) return true;   
        }       
        return false;
    }
    
    /**
     * Returns TRUE if the given organism has known genome builds, else FALSE
     * @return
     */    
    public static boolean hasSupportedGenomeBuilds(int organism) {
        for (Object[] values:predefined) { 
            if (((Integer)values[0])==organism) {
                String[][] builds=(String[][])values[3];
                return (builds!=null && builds.length>0);
            }
        } 
        return false;
    }
    
    /**
     * Returns a String array containing the supported genome builds (using default names)
     * for the given organism or null if no genome builds are known
     * @return
     */    
    public static String[] getSupportedGenomeBuilds(int organism) {
        for (Object[] values:predefined) { 
            if (((Integer)values[0])==organism) {
                String[][] builds=(String[][])values[3];
                if (builds==null) return null;
                String[] defaults=new String[builds.length];
                for (int i=0;i<builds.length;i++) defaults[i]=builds[i][0];
                return defaults;
            }
        } 
        return null;
    }
       
    public static String getSupportedGenomeBuildsAsString(int organism) {
        for (Object[] values:predefined) { 
            if (((Integer)values[0])==organism) {
                String[][] builds=(String[][])values[3];
                if (builds==null || builds.length==0) return "";
                else {
                    StringBuilder builder=new StringBuilder();
                    for (int i=0;i<builds.length;i++) {
                        for (int j=0;j<builds[i].length;j++) {
                            builder.append(builds[i][j]);
                            if (j<builds[i].length-1) builder.append("|");
                        }                    
                        if (i<builds.length-1) builder.append(",");
                    }
                    return builder.toString();
                }
            }
        } 
        return "";
    }    


    /**
     * Returns true if the given genome build is supported (by any organism)
     * @param genomebuild A default name for a genome build (this will not be matched to alternative names!)
     */    
    public static boolean isGenomeBuildSupported(String genomebuild) {
        for (Object[] values:predefined) { 
            if (values[3]==null) continue;
            else {
                String[][] builds=(String[][])values[3];
                for (int i=0;i<builds.length;i++) {
                   if (builds[i][0].equals(genomebuild)) return true; 
                }
            }
        } 
        return false;
    }   
    
    /**
     * Returns true if the given organism supports the given genome build
     * @param genomebuild A default name for a genome build (this will not be matched to alternative names!)
     */    
    public static boolean isGenomeBuildSupported(int organism, String genomebuild) {
        if (genomebuild==null) return false;
        for (Object[] values:predefined) { 
            if (((Integer)values[0])==organism) {
                String[][] builds=(String[][])values[3];
                if (builds==null) return false;
                for (int i=0;i<builds.length;i++) {
                    if (builds[i][0].equals(genomebuild)) return true;
                }
            }
        } 
        return false;
    }     
    
    /**
     * Returns the organism ID (taxonomy) corresponding to the genomebuild if the build is supported by the system. 
     * If the build is unknown the value 0 is returned
     * @param genomebuild The identifier for the genomebuild. Both default and alternative identifiers can be used
     */    
    public static int getOrganismForGenomeBuild(String genomebuild) {
        if (genomebuild==null) return 0;
        for (Object[] values:predefined) { 
            if (values[3]==null) continue;
            else {
                String[][] builds=(String[][])values[3];
                for (int i=0;i<builds.length;i++) {
                    for (int j=0;j<builds[i].length;j++) {
                       if (builds[i][j].equals(genomebuild)) return (Integer)values[0]; 
                    }                    
                }
            }
        } 
        return 0;
    }    
    
    /**
     * Returns the default buildname for the given alternative build name.
     * E.g. the alternative name 'NCBI36' could return the default name 'hg18'
     * or NULL if the build name is not recognized
     * @param genomebuild An alternative identifier for the genomebuild
     * @param a taxonomy number for the organism, or use 0 to search all
     */    
    public static String getDefaultBuildName(String genomebuild, int organism) {
        if (genomebuild==null) return null;
        for (Object[] values:predefined) { 
            if (organism!=0 && ((Integer)values[0]).intValue()!=organism) continue;
            if (values[3]==null) continue;
            else {
                String[][] builds=(String[][])values[3];
                for (int i=0;i<builds.length;i++) {
                    for (int j=0;j<builds[i].length;j++) {
                       if (builds[i][j].equals(genomebuild)) return builds[i][0]; // return first in list 
                    }                    
                }
            }
        } 
        return null;
    }  
    
    /**
     * Returns the Clade for the organism with the specified taxonomy ID
     * Returns the value "Unknown" if either the organism is unknown or 
     * the clade for the organism is unknown
     */    
    public static String getCladeForOrganism(int organism) {
        for (Object[] values:predefined) { 
            if ((Integer)values[0]==organism) {
                 String clade=(String)values[4];
                 if (clade==null) return "Unknown";
                 else return clade;
            }
        }
        return "Unknown";
    }    
    
    
    
    /** Lists information about all registered organisms to STDERR */
    public static void debug() {
        for (Object[] organism:predefined) {
            System.err.println(organism[1]+" ("+organism[2]+") => "+organism[0]+" ["+organism[4]+"]");
            String[][] builds=(String[][])organism[3];
            if (builds!=null) {
                for (int i=0;i<builds.length;i++) {
                    System.err.print("  >");
                    for (int j=0;j<builds[i].length;j++) {
                        System.err.print(builds[i][j]+"  ");
                    }
                    System.err.println("");
                }
            }
            System.err.println("\n------------");
        }
    }

}
