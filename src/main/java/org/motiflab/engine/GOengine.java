/*
 * The GOengine provides functionality to handle gene ontology terms
 * including looking up descriptions for terms
 */
package org.motiflab.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author kjetikl
 */
public class GOengine {

    // abbreviations of "domains"
    public static final String MOLECULAR_FUNCTION_ABBREVIATION="MF";
    public static final String BIOLOGICAL_PROCESS_ABBREVIATION="BP";
    public static final String CELLULAR_COMPONENT_ABBREVIATION="CC";
    
    public static final String MOLECULAR_FUNCTION="Molecular function";
    public static final String BIOLOGICAL_PROCESS="Biological process";
    public static final String CELLULAR_COMPONENT="Cellular component";    
    
    
    private HashMap<String,String[]> cache=new HashMap<>(); // maps GO accessions to descriptions. The keys are 7-digit numbers. The values String[]{domain, description}
    private MotifLabEngine engine=null;
      
      
    public GOengine(MotifLabEngine engine) {
        this.engine=engine;
        // check if the GO annotations file has been installed locally, and if not: install it!
        String GOfilepath=engine.getMotifLabDirectory()+File.separator+"GO_table.config";        
        File file=new File(GOfilepath);
        if (!file.exists()) { // install from resource if file is not found
            try {
                engine.installBundledResource("/org/motiflab/engine/resources/GO_table.config", "GO_table.config");
            } catch (SystemError e) {
                engine.logMessage("WARNING: Unable to install Gene Ontology annotation file");
            }         
        }        
        
    }
    
    /** Checks if the provided string is a valid GO-term */
    public static boolean checkGOterm(String value) {
       if (value==null || value.trim().isEmpty()) return true;
       if (value.startsWith("GO:") || value.startsWith("go:")) value=value.substring(3);
       int termvalue=0;
       try {
           termvalue=Integer.parseInt(value);
       } catch (Exception e) {return false;}
       if (termvalue<0 || termvalue>9999999) return false;
       return true;
    }            
            
      
    /** Given a valid GO term string this method will return the GO accession number as an integer
     *  @returns the GO accession number as an integer or -1 if the string does not represent a valid GO term
     */
    public static int getGOtermAccessionNumber(String goterm) {
        if (goterm.startsWith("GO:") || goterm.startsWith("go:")) goterm=goterm.substring(3);
        try {
            int value=Integer.parseInt(goterm);
            if (value<0 || value>9999999) return -1;
        } catch (NumberFormatException e) {
            return -1;
        }      
        return -1;
    }
    
    
    /** Given a GO accession number in the form of an integer, this method will return 
     *  a GO term in the form of a string with 7 digits possibly prefixed with "GO:". 
     *  If the number is not a valid accession (positive number, max 7 digits) the method will return NULL
     */
    public static String getGOterm(int accession, boolean prefix) {
        if (accession<0 || accession>9999999) return null;
        String sevendigits=String.format("%07d", accession);
        if (prefix) return "GO:"+sevendigits; else return sevendigits;      
    }
    
    /** Strips the prefix "GO:" (or "go:") from the beginning of the provided GO term if present */
    public static String stripGOprefix(String goterm) {
       if (goterm.startsWith("GO:") || goterm.startsWith("go:")) return goterm.substring(3);
       else return goterm;
    }    
        
    
    /** Returns the GO description (or "name") for the given GO term */
    public String getGOdescription(String goterm) {
        goterm=stripGOprefix(goterm);
        String[] info=cache.get(goterm);
        if (info==null) { // not cached. Retreive from source and cache it
            getAndCacheSingleTerm(goterm);
            info=cache.get(goterm); // it should now hopefully have been cached            
        } 
        if (info!=null) return info[1];
        else return ""; // default to emtpy value
    }
    
    /** Returns the GO description (or "name") for the given GO term represented as an accession number */
    public String getGOdescription(int accession) {
        String goTerm=getGOterm(accession,false);
        return getGOdescription(goTerm);        
    }
    
    /** Returns the GO domain for the given GO term */
    public String getGOdomain(String goterm) {
        goterm=stripGOprefix(goterm);
        String[] info=cache.get(goterm);
        if (info==null) { // not cached. Retreive from source and cache it
            getAndCacheSingleTerm(goterm);
            info=cache.get(goterm); // it should now hopefully have been cached            
        } 
        if (info!=null) return info[0];
        else return ""; // default to emtpy value
    }
    
    public String[] getGOdescriptionAndDomain(String goterm) {
        goterm=stripGOprefix(goterm);
        String[] info=cache.get(goterm);
        if (info==null) { // not cached. Retreive from source and cache it
            getAndCacheSingleTerm(goterm);
            info=cache.get(goterm); // it should now hopefully have been cached            
        } 
        if (info!=null) return info;
        else return new String[]{"",""}; // default to emtpy value
    }    
    
    /** Returns the GO domain for the given GO term represented as an accession number */
    public String getGOdomain(int accession) {
        String goTerm=getGOterm(accession,false);
        return getGOdomain(goTerm);        
    }      
    
    
    
    /** Finds information about a single GO term from the primary source and puts this information into the cache */
    private void getAndCacheSingleTerm(String goTerm) {
        HashSet<String> singleTerm=new HashSet<>(1);
        singleTerm.add(goTerm);
        cache.putAll(getGOtermsFromSource(singleTerm));      
    }
    
    public static String getFullDomain(String domainAbbreviation) {
        if (domainAbbreviation.equalsIgnoreCase(MOLECULAR_FUNCTION_ABBREVIATION)) return MOLECULAR_FUNCTION;
        else if (domainAbbreviation.equalsIgnoreCase(BIOLOGICAL_PROCESS_ABBREVIATION)) return BIOLOGICAL_PROCESS;
        else if (domainAbbreviation.equalsIgnoreCase(CELLULAR_COMPONENT_ABBREVIATION)) return CELLULAR_COMPONENT;
        else return "";
    }
    
    public static String getAbbreviatedDomain(String domain) {
        if (domain.equalsIgnoreCase(MOLECULAR_FUNCTION)) return MOLECULAR_FUNCTION_ABBREVIATION;
        else if (domain.equalsIgnoreCase(BIOLOGICAL_PROCESS)) return BIOLOGICAL_PROCESS_ABBREVIATION;
        else if (domain.equalsIgnoreCase(CELLULAR_COMPONENT)) return CELLULAR_COMPONENT_ABBREVIATION;
        else return "";
    }    
    

    /** Fetches and returns descriptions for the given GO terms in the form of a map where the keys are 7-digit numbers
     *  Unknown GO terms will have empty values ("") in the map
     *  @param terms The terms to fetch. These should be in the form of 7-digit strings (without prefix)
     */
    public HashMap<String,String> getGOdescriptions(Collection<String> terms) {
         HashMap<String,String> result=new HashMap<>();
         HashSet<String> uncached=new HashSet<>();
         for (String term:terms) {
             String[] info=cache.get(term);
             if (info!=null) result.put(term, info[1]); 
             else uncached.add(term);
         }
         if (!uncached.isEmpty()) { // not in cache? try once more to get the information from original source
             cache.putAll(getGOtermsFromSource(uncached)); // 
             for (String term:uncached) {
                 String[] info=cache.get(term);
                 if (info!=null) result.put(term, info[1]); 
                 else result.put(term, ""); // default to empty value when no information is obtainable 
             }             
         }
         return result;
    }
    
    /** Fetches and returns domains for the given GO terms in the form of a map where the keys are 7-digit numbers
     *  Unknown terms will have empty values ("") in the map 
     *  @param terms The terms to fetch. These should be in the form of 7-digit strings (without prefix)
     */
    public HashMap<String,String> getGOdomains(Collection<String> terms) {
         HashMap<String,String> result=new HashMap<>();
         HashSet<String> uncached=new HashSet<>();
         for (String term:terms) {
             String[] info=cache.get(term);
             if (info!=null) result.put(term, info[0]); 
             else uncached.add(term);
         }
         if (!uncached.isEmpty()) { // not in cache? try once more to get the information from original source
             cache.putAll(getGOtermsFromSource(uncached)); // 
             for (String term:uncached) {
                 String[] info=cache.get(term);
                 if (info!=null) result.put(term, info[0]); 
                 else result.put(term, ""); // default to empty value when no information is obtainable 
             }             
         }
         return result;      
    }    
    
    /** Fetches and returns GO information for the given GO terms in the form of a map where the keys are 7-digit numbers
     *  and the values are String[] pairs where the first element is a "domain" code and the second element is a description (name) of the GO term
     *  Unknown terms will have empty values ("") for both elements in the pair
     *  @param terms The terms to fetch.
     */
    private HashMap<String,String[]> getGOtermsFromSource(Collection<String> terms) {
        HashSet<String> lookup;
        if (terms instanceof HashSet) lookup=(HashSet<String>)terms;
        else {
            lookup=new HashSet<>();
            for (String term:terms) {
                lookup.add(stripGOprefix(term));
            }             
        } // add all terms to this set for easy lookup
        HashMap<String,String[]> map=new HashMap<>(); //
        int size=lookup.size();
        String GOfilepath=engine.getMotifLabDirectory()+File.separator+"GO_table.config";
        File file=new File(GOfilepath);
        if (!file.exists()) { // install from resource if file is not found
            engine.logMessage("WARNING: Unable to find Gene Ontology annotation file");
            return map;                                  
        }
        FileInputStream inputStream=null;
        BufferedReader dataReader=null;
        try {
            inputStream=new FileInputStream(file); 
            dataReader = new BufferedReader(new InputStreamReader(inputStream));
            String line=null;
            while ((line=dataReader.readLine())!=null){               
               String[] fields=line.split("\t");
               if (fields.length>=2 && lookup.contains(fields[0])) {
                   map.put(fields[0], new String[]{fields[1],fields[2]}); 
                   if (map.size()==size) break; // we have found all that we need
               }
            }
        } catch (Exception e) {
           engine.logMessage("An error happened while reading the system's Gene Ontology file");
        }
        finally {
            try {if (dataReader!=null) dataReader.close();} catch(Exception e) {}
            try {if (inputStream!=null) inputStream.close();} catch(Exception e) {}
        }            
        return map;       
    }
    
    
}
